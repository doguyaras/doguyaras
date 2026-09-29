package com.acme.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.platform.security.jwt.Ed25519Keys;
import com.acme.platform.security.jwt.ServiceJwtKeyRegistry;
import com.acme.platform.security.jwt.ServiceJwtSigner;
import com.acme.runtime.order.OrderApp;
import com.acme.runtime.subscription.SubscriptionApp;
import com.nimbusds.jose.jwk.OctetKeyPair;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * KANIT SEVIYESI 3 (referans Bolum 11.5, 19.6): iki ayri Spring Boot uygulamasi (OrderApp koordinator,
 * SubscriptionApp katilimci) rastgele portlarda, aralarinda GERCEK HTTP + servis JWT (Ed25519, anahtar dosyalari) +
 * Resilience4j; tek gomulu PostgreSQL 18'de iki ayri schema. Test gateway rolundedir (act=gateway, sub=hesap).
 * Hata enjeksiyonu katilimcinin test-only /internal/test/chaos ucu ile yapilir (allowlist: yalniz "test" aktoru).
 *
 * Zaman: uygulamalar gercek saatle calisir (timeout/circuit/recovery gercek zamanda olculur); asenkron sonuclar
 * sinirli poll ile beklenir (en fazla 15 sn), uygulama mantigi icin sabit sleep yoktur.
 */
class OrderSubscriptionRuntimeIT {

    static final Duration MAX_WAIT = Duration.ofSeconds(15);
    static final String SUBSCRIPTION_AUD = "subscription-api";
    static final String ORDER_AUD = "order-api";
    static final JsonMapper JSON = new JsonMapper();

    static EmbeddedPostgres pg;
    static NamedParameterJdbcTemplate db;
    static Path secrets;
    static String jdbcUrl;
    static ServiceJwtSigner gateway, order, chat, tester;
    static ConfigurableApplicationContext subscriptionApp, orderApp;
    static String subscriptionBase, orderBase;
    static HttpClient http;

    record Resp(int status, Map<String, Object> body, Map<String, List<String>> headers, long elapsedMs) {
        String str(String field) { Object v = body.get(field); return v == null ? null : v.toString(); }
        @SuppressWarnings("unchecked")
        Object errorField(String field) { return ((Map<String, Object>) body.get("error")).get(field); }
        String header(String name) { return headers.getOrDefault(name.toLowerCase(), List.of()).stream().findFirst().orElse(null); }
    }

    // ---------------------------------------------------------------- altyapi

    @BeforeAll
    static void startInfrastructure() throws Exception {
        pg = EmbeddedPostgres.builder().start();
        DataSource ds = pg.getPostgresDatabase();
        db = new NamedParameterJdbcTemplate(ds);
        assertThat(db.getJdbcTemplate().queryForObject("SHOW server_version_num", Integer.class)).isGreaterThanOrEqualTo(180000);
        jdbcUrl = pg.getJdbcUrl("postgres", "postgres");
        migrate(ds);

        // Her imzalayicinin kendi Ed25519 anahtari (Bolum 9.2); dogrulayicilar ortak JWKS dosyasini okur
        Clock clock = Clock.systemUTC();
        OctetKeyPair gatewayKey = Ed25519Keys.generate("gw-1"), orderKey = Ed25519Keys.generate("order-1"),
                chatKey = Ed25519Keys.generate("chat-1"), testKey = Ed25519Keys.generate("test-1");
        gateway = new ServiceJwtSigner(gatewayKey, "gateway", clock);
        order = new ServiceJwtSigner(orderKey, "order-service", clock);
        chat = new ServiceJwtSigner(chatKey, "chat-service", clock);
        tester = new ServiceJwtSigner(testKey, "test", clock);
        ServiceJwtKeyRegistry registry = new ServiceJwtKeyRegistry();
        registry.register("gateway", gatewayKey);
        registry.register("order-service", orderKey);
        registry.register("chat-service", chatKey);
        registry.register("test", testKey);
        secrets = Files.createTempDirectory("runtime-secrets");
        Files.writeString(secrets.resolve("service-jwks.json"), registry.toJson());
        Files.writeString(secrets.resolve("order-service-signing-key"), orderKey.toJSONString());

        http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(2)).build();
        subscriptionApp = SubscriptionApp.start(
                "--server.port=0",
                "--spring.datasource.url=" + jdbcUrl, "--spring.datasource.username=postgres",
                "--service-jwt.jwks-path=" + secrets.resolve("service-jwks.json"),
                "--runtime.chaos.enabled=true");
        subscriptionBase = "http://localhost:" + subscriptionApp.getEnvironment().getProperty("local.server.port");
        startOrderApp();
    }

    static void startOrderApp() {
        orderApp = OrderApp.start(
                "--server.port=0",
                "--spring.datasource.url=" + jdbcUrl, "--spring.datasource.username=postgres",
                "--service-jwt.jwks-path=" + secrets.resolve("service-jwks.json"),
                "--service-jwt.private-key-path=" + secrets.resolve("order-service-signing-key"),
                "--clients.subscription.base-url=" + subscriptionBase,
                // deadline/backoff test icin kisaltildi; timeout, circuit ve bulkhead degerleri order-app.yml'deki gibi
                "--operation-consistency.deadline-seconds=3",
                "--operation-consistency.lease-seconds=5",
                "--operation-consistency.max-backoff-seconds=2",
                "--operation-consistency.poll-millis=500");
        orderBase = "http://localhost:" + orderApp.getEnvironment().getProperty("local.server.port");
    }

    /** Migration yerine gecen DDL: saga tablolari platform-messaging'den, domain tablolari bu modulden. */
    static void migrate(DataSource ds) throws Exception {
        db.getJdbcTemplate().execute("CREATE SCHEMA \"order\"");
        db.getJdbcTemplate().execute("CREATE SCHEMA subscription");
        String saga = resource("/db/platform/saga_coordinator.sql").replace("${schema}", "\"order\"");
        try (var conn = ds.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(saga.getBytes(StandardCharsets.UTF_8)));
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(resource("/db/order/V1__order_item.sql").getBytes(StandardCharsets.UTF_8)));
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(resource("/db/subscription/V1__quota.sql").getBytes(StandardCharsets.UTF_8)));
        }
    }

    static String resource(String path) throws Exception {
        try (var in = OrderSubscriptionRuntimeIT.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @AfterAll
    static void stopInfrastructure() throws Exception {
        if (orderApp != null) orderApp.close();
        if (subscriptionApp != null) subscriptionApp.close();
        if (pg != null) pg.close();
    }

    @BeforeEach
    void resetFaults() throws Exception {
        clearChaos();
        circuitBreaker().reset();                                       // onceki testin circuit durumu tasinmaz
        chaosCall("POST", "/internal/test/chaos/stats/reset", null);
    }

    /** Test izolasyonu: arka planda kalan saga'lar terminal olana kadar bekle; hicbiri MANUAL_REVIEW'a dusmemeli. */
    @AfterEach
    void drain() throws Exception {
        clearChaos();
        await("all sagas terminal", () -> count("SELECT count(*) FROM \"order\".saga WHERE status NOT IN ('CONFIRMED','COMPENSATED','MANUAL_REVIEW')") == 0);
        assertThat(count("SELECT count(*) FROM \"order\".saga WHERE status = 'MANUAL_REVIEW'")).as("manual review sagas").isZero();
    }

    // ---------------------------------------------------------------- senaryolar

    @Test // 1: mutlu yol + W3C trace yayilimi (gateway -> order -> subscription)
    void happyPathConfirmsAndPropagatesTrace() throws Exception {
        UUID account = newAccount(5), key = UUID.randomUUID();
        String traceId = hex(16), parentId = hex(8);
        Resp r = postOrder(account, key, UUID.randomUUID(), "00-" + traceId + "-" + parentId + "-01");

        assertThat(r.status()).isEqualTo(201);
        assertThat(r.str("kind")).isEqualTo("OK");
        assertThat(r.str("detail")).startsWith("ORDER:");
        UUID sagaId = UUID.fromString(r.str("sagaId"));
        await("saga CONFIRMED", () -> "CONFIRMED".equals(sagaStatus(sagaId)));
        assertThat(opStatus(account, key)).isEqualTo("CONFIRMED");
        assertThat(remaining(account)).isEqualTo(4);
        assertThat(orderCount(account)).isEqualTo(1);

        // Katilimcinin ALDIGI traceparent: ayni trace-id, OrderApp'in client span'i yeni parent-id
        String received = receivedTraceparent(account, key, "consume");
        assertThat(received).matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}");
        assertThat(received.substring(3, 35)).isEqualTo(traceId);
        assertThat(received.substring(36, 52)).isNotEqualTo(parentId);
        // Worker'in confirm cagrisi da (arka plan, istek baglami yok) W3C header tasir
        assertThat(receivedTraceparent(account, key, "confirm")).matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}");
    }

    @Test // 2: yavas katilimci: read-timeout butcesi korunur, commit olmus consume recovery ile telafi edilir
    void slowParticipantTimesOutAndRecoveryCompensates() throws Exception {
        UUID account = newAccount(5), key = UUID.randomUUID();
        chaos("{\"slowMillis\":3000,\"operations\":[\"consume\"]}");

        Resp r = postOrder(account, key, UUID.randomUUID(), null);
        assertThat(r.status()).isEqualTo(503);
        assertThat(r.str("kind")).isEqualTo("UPSTREAM_UNAVAILABLE");
        assertThat(r.errorField("code")).isEqualTo(11101);
        assertThat(r.errorField("service")).isEqualTo("order");
        assertThat(r.elapsedMs()).as("request bounded by read timeout 1500 ms").isBetween(1400L, 2499L);
        UUID sagaId = UUID.fromString(r.str("sagaId"));
        assertThat(sagaStatus(sagaId)).isEqualTo("STARTED");
        // Istemci vazgecti ama katilimci commit etti: belirsiz sonuc gercek
        assertThat(opStatus(account, key)).isEqualTo("APPLIED");
        assertThat(remaining(account)).isEqualTo(4);

        clearChaos();
        await("saga COMPENSATED after deadline", () -> "COMPENSATED".equals(sagaStatus(sagaId)));
        assertThat(opStatus(account, key)).isEqualTo("COMPENSATED");
        assertThat(remaining(account)).as("quota restored exactly once").isEqualTo(5);
        assertThat(orderCount(account)).isZero();
    }

    @Test // 2b: yanit commit SONRASI kayboluyor (baglanti kopuyor): worker GET ile uzlasir, iade tek sefer
    void lostResponsesAreReconciledThroughGet() throws Exception {
        UUID account = newAccount(5), key = UUID.randomUUID();
        // consume VE compensate commit olur ama yanitlari hic ulasmaz; GET saglikli
        chaos("{\"dropAfterCommit\":true,\"operations\":[\"consume\",\"compensate\"]}");

        Resp r = postOrder(account, key, UUID.randomUUID(), null);
        assertThat(r.status()).isEqualTo(503);
        assertThat(r.str("kind")).isEqualTo("UPSTREAM_UNAVAILABLE");
        assertThat(r.elapsedMs()).as("connection drop is detected without waiting for read timeout").isLessThan(1400L);
        UUID sagaId = UUID.fromString(r.str("sagaId"));
        assertThat(opStatus(account, key)).isEqualTo("APPLIED");

        // chaos ACIK kalir: compensate yaniti her seferinde kaybolur; saga yalniz GET uzlasmasiyla terminal olabilir
        await("saga COMPENSATED via GET reconciliation", () -> "COMPENSATED".equals(sagaStatus(sagaId)));
        assertThat(opStatus(account, key)).isEqualTo("COMPENSATED");
        assertThat(remaining(account)).as("refund applied exactly once").isEqualTo(5);
    }

    @Test // 3: circuit breaker: pencere dolunca hizli red, bekleme + saglikli katilimci -> half-open -> closed
    void circuitBreakerOpensAndRecovers() throws Exception {
        UUID account = newAccount(50);
        chaos("{\"slowMillis\":3000}");
        List<Long> elapsed = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Resp r = postOrder(account, UUID.randomUUID(), UUID.randomUUID(), null);
            assertThat(r.str("kind")).as("request %d", i + 1).isEqualTo("UPSTREAM_UNAVAILABLE");
            assertThat(r.status()).isEqualTo(503);
            elapsed.add(r.elapsedMs());
        }
        assertThat(elapsed.get(0)).as("first call waits for read timeout").isGreaterThanOrEqualTo(1400L);
        assertThat(elapsed.subList(4, 6)).as("circuit OPEN: no thread blocked").allSatisfy(ms -> assertThat(ms).isLessThan(200L));
        assertThat(circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);

        clearChaos();
        // wait-duration (2 sn) dolana kadar istekler hizli reddedilir; sonra half-open deneme cagrilari gecer
        await("order succeeds again (half-open probe)", () -> "OK".equals(postOrderQuiet(account).str("kind")));
        await("circuit CLOSED", () -> circuitBreaker().getState() == CircuitBreaker.State.CLOSED
                || "OK".equals(postOrderQuiet(account).str("kind")) && circuitBreaker().getState() == CircuitBreaker.State.CLOSED);
        assertThat(postOrderQuiet(account).str("kind")).isEqualTo("OK");
    }

    @Test // 4: katilimci 503: hizli UPSTREAM_UNAVAILABLE, worker backoff ile dener, duzelince tombstone ile telafi
    void participantDownRetriesWithBackoffThenCompensates() throws Exception {
        UUID account = newAccount(5), key = UUID.randomUUID();
        chaos("{\"down\":true}");

        Resp r = postOrder(account, key, UUID.randomUUID(), null);
        assertThat(r.status()).isEqualTo(503);
        assertThat(r.str("kind")).isEqualTo("UPSTREAM_UNAVAILABLE");
        assertThat(r.errorField("code")).isEqualTo(11101);
        assertThat(r.elapsedMs()).isLessThan(500L);
        UUID sagaId = UUID.fromString(r.str("sagaId"));

        await("worker retried twice with backoff", () -> {
            Map<String, Object> step = step(sagaId);
            return ((Number) step.get("attempt")).intValue() >= 2 && "RETRY".equals(step.get("status"));
        });
        Map<String, Object> step = step(sagaId);
        assertThat(step.get("last_error_code")).isEqualTo("ParticipantUnavailableException");
        assertThat(((Number) step.get("backoff_ms")).longValue()).as("backoff = min(max, 2^n) s").isEqualTo(2000L);
        assertThat(sagaStatus(sagaId)).isEqualTo("CANCEL_REQUESTED");
        assertThat(opStatus(account, key)).as("down: nothing committed on participant").isNull();

        clearChaos();
        await("saga COMPENSATED", () -> "COMPENSATED".equals(sagaStatus(sagaId)));
        assertThat(opStatus(account, key)).as("tombstone blocks a late consume").isEqualTo("CANCELLED");
        assertThat(remaining(account)).isEqualTo(5);
        assertThat(count("SELECT count(*) FROM subscription.operation WHERE account_id = '" + account + "' AND amount > 0")).isZero();
    }

    @Test // 5: allowlist (yanlis aktor) ve delegasyon (arka plan token'i / baska hesap) katilimcida reddedilir
    void wrongActorAndBackgroundTokenAreRejected() throws Exception {
        UUID account = newAccount(5), key = UUID.randomUUID();
        String path = "/internal/subscription/accounts/" + account + "/operations/" + key + "/consume";
        String body = "{\"operationType\":\"ORDER_QUOTA\",\"amount\":1}";

        Resp wrongActor = call("POST", subscriptionBase + path, chat.mint(SUBSCRIPTION_AUD, account), body, null);
        assertThat(wrongActor.status()).isEqualTo(403);
        assertThat(wrongActor.str("code")).isEqualTo("INTERNAL_ACCESS_DENIED");

        Resp background = call("POST", subscriptionBase + path, order.mint(SUBSCRIPTION_AUD, null), body, null);
        assertThat(background.status()).isEqualTo(403);
        assertThat(background.str("code")).isEqualTo("DELEGATION_DENIED");

        Resp otherAccount = call("POST", subscriptionBase + path, order.mint(SUBSCRIPTION_AUD, UUID.randomUUID()), body, null);
        assertThat(otherAccount.status()).isEqualTo(403);
        assertThat(otherAccount.str("code")).isEqualTo("DELEGATION_DENIED");

        assertThat(opStatus(account, key)).as("no operation row after rejected calls").isNull();
        assertThat(remaining(account)).isEqualTo(5);

        // Pozitif kontrol: dogru aktor + sub == path hesabi; katilimci aldigi traceparent'i yanitta yansitir
        String tp = "00-" + hex(16) + "-" + hex(8) + "-01";
        Resp ok = call("POST", subscriptionBase + path, order.mint(SUBSCRIPTION_AUD, account), body, tp);
        assertThat(ok.status()).isEqualTo(200);
        assertThat(ok.str("state")).isEqualTo("APPLIED");
        assertThat(ok.header("X-Received-Traceparent")).isEqualTo(tp);
        assertThat(opStatus(account, key)).isEqualTo("APPLIED");
    }

    @Test // 6: bulkhead: katilimcida en fazla 8 esanli cagri, fazlasi hemen reddedilir, thread havuzu tukenmez
    void bulkheadCapsConcurrentCallsAndRejectsFast() throws Exception {
        UUID account = newAccount(50);
        chaos("{\"slowMillis\":1000,\"operations\":[\"consume\"]}");
        int n = 20;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Resp>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < n; i++) {
                futures.add(pool.submit(() -> { go.await(); return postOrder(account, UUID.randomUUID(), UUID.randomUUID(), null); }));
            }
            long start = System.nanoTime();
            go.countDown();
            List<Resp> results = new ArrayList<>();
            for (Future<Resp> f : futures) results.add(f.get(5, TimeUnit.SECONDS));
            long wallMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            long ok = results.stream().filter(r -> "OK".equals(r.str("kind"))).count();
            List<Resp> rejected = results.stream().filter(r -> "UPSTREAM_UNAVAILABLE".equals(r.str("kind"))).toList();
            assertThat(ok + rejected.size()).isEqualTo(n);
            assertThat(ok).isEqualTo(8);
            assertThat(rejected).hasSize(12).allSatisfy(r -> {
                assertThat(r.status()).isEqualTo(503);
                assertThat(r.elapsedMs()).as("bulkhead full -> immediate").isLessThan(700L);
            });
            assertThat(wallMs).as("all return within read timeout + margin").isLessThan(2500L);
            Map<String, Object> stats = chaosCall("GET", "/internal/test/chaos/stats", null).body();
            assertThat(((Number) stats.get("maxInFlight")).intValue()).as("max concurrent calls at participant").isEqualTo(8);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test // 7: SUCCEEDED ama confirm edilmemis saga, OrderApp yeniden baslayinca ayni DB'den confirm edilir
    void restartedCoordinatorConfirmsPendingSaga() throws Exception {
        UUID account = newAccount(5), key = UUID.randomUUID();
        chaos("{\"down\":true,\"operations\":[\"confirm\",\"get\"]}");

        Resp r = postOrder(account, key, UUID.randomUUID(), null);
        assertThat(r.str("kind")).isEqualTo("OK");
        UUID sagaId = UUID.fromString(r.str("sagaId"));
        await("confirm attempted and deferred", () -> ((Number) step(sagaId).get("attempt")).intValue() >= 1);

        orderApp.close();                                               // koordinator sureci durur
        assertThat(sagaStatus(sagaId)).isEqualTo("SUCCEEDED");
        assertThat(opStatus(account, key)).isEqualTo("APPLIED");

        clearChaos();
        startOrderApp();                                                // yeni surec, ayni DB
        await("saga CONFIRMED after restart", () -> "CONFIRMED".equals(sagaStatus(sagaId)));
        assertThat(opStatus(account, key)).isEqualTo("CONFIRMED");
        assertThat(remaining(account)).isEqualTo(4);
        assertThat(orderCount(account)).isEqualTo(1);
    }

    // ---------------------------------------------------------------- yardimcilar

    static UUID newAccount(int quota) {
        UUID account = UUID.randomUUID();
        db.update("INSERT INTO subscription.quota (account_id, remaining) VALUES (:a, :r)", Map.of("a", account, "r", quota));
        return account;
    }

    static Resp postOrder(UUID account, UUID key, UUID resource, String traceparent) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(orderBase + "/v1/orders"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-Service-Auth", gateway.mint(ORDER_AUD, account))
                .header("X-Idempotency-Key", key.toString())
                .POST(HttpRequest.BodyPublishers.ofString("{\"resourceId\":\"" + resource + "\"}"));
        if (traceparent != null) b.header("traceparent", traceparent);
        return send(b.build());
    }

    static Resp postOrderQuiet(UUID account) {
        try {
            return postOrder(account, UUID.randomUUID(), UUID.randomUUID(), null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static Resp call(String method, String url, String token, String body, String traceparent) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").header("X-Service-Auth", token)
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (traceparent != null) b.header("traceparent", traceparent);
        return send(b.build());
    }

    @SuppressWarnings("unchecked")
    static Resp send(HttpRequest request) throws Exception {
        long start = System.nanoTime();
        HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        Map<String, Object> body = res.body() == null || res.body().isBlank() ? Map.of() : JSON.readValue(res.body(), Map.class);
        return new Resp(res.statusCode(), body, res.headers().map(), ms);
    }

    static Resp chaosCall(String method, String path, String body) throws Exception {
        Resp r = call(method, subscriptionBase + path, tester.mint(SUBSCRIPTION_AUD, null), body, null);
        assertThat(r.status()).as("chaos endpoint %s %s", method, path).isEqualTo(200);
        return r;
    }

    static void chaos(String settingsJson) throws Exception { chaosCall("PUT", "/internal/test/chaos", settingsJson); }

    static void clearChaos() throws Exception { chaosCall("DELETE", "/internal/test/chaos", null); }

    static String receivedTraceparent(UUID account, UUID key, String operation) throws Exception {
        String path = "/internal/subscription/accounts/" + account + "/operations/" + key + "/" + operation;
        return chaosCall("GET", "/internal/test/chaos/traces?path=" + path, null).str("traceparent");
    }

    static CircuitBreaker circuitBreaker() {
        return orderApp.getBean(CircuitBreakerRegistry.class).circuitBreaker("subscription");
    }

    static String sagaStatus(UUID sagaId) {
        return DataAccessUtils.singleResult(db.queryForList("SELECT status FROM \"order\".saga WHERE id = :id", Map.of("id", sagaId), String.class));
    }

    static Map<String, Object> step(UUID sagaId) {
        return db.queryForMap("""
                SELECT status, attempt, last_error_code,
                       (EXTRACT(EPOCH FROM (next_attempt_at - updated_at)) * 1000)::bigint AS backoff_ms
                FROM "order".saga_steps WHERE saga_id = :id""", Map.of("id", sagaId));
    }

    static String opStatus(UUID account, UUID key) {
        return DataAccessUtils.singleResult(db.queryForList(
                "SELECT status FROM subscription.operation WHERE caller_service = 'order-service' AND account_id = :a AND operation_key = :k",
                Map.of("a", account, "k", key), String.class));
    }

    static int remaining(UUID account) {
        return db.queryForObject("SELECT remaining FROM subscription.quota WHERE account_id = :a", Map.of("a", account), Integer.class);
    }

    static int orderCount(UUID account) {
        return db.queryForObject("SELECT count(*) FROM \"order\".order_item WHERE account_id = :a", Map.of("a", account), Integer.class);
    }

    static int count(String sql) { return db.getJdbcTemplate().queryForObject(sql, Integer.class); }

    /** Sinirli poll: gercek zamanli altyapi (HTTP, scheduler) icin; en fazla MAX_WAIT. */
    static void await(String what, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + MAX_WAIT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            TimeUnit.MILLISECONDS.sleep(100);
        }
        assertThat(condition.getAsBoolean()).as("timed out after %s waiting for: %s", MAX_WAIT, what).isTrue();
    }

    static String hex(int bytes) {
        byte[] b = new byte[bytes];
        ThreadLocalRandom.current().nextBytes(b);
        b[0] |= 1;                                                       // tamami sifir olmayan id (W3C: gecersiz)
        return HexFormat.of().formatHex(b);
    }
}
