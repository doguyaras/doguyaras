package com.acme.platform.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.platform.ratelimit.RateLimitDecision.Outcome;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * DAVRANISSAL dogrulama (referans Bolum 9.6, 22; kanit seviyesi 2): gercek redis-server 7.0 sureci uzerinde
 * fixed-window limiter. Pencere zamani Redis TTL'idir (sunucu saati); JVM saati yalniz hata zarfindaki
 * timestamp icindir ve deterministik Clock ile verilir.
 */
class RateLimiterBehaviourIT {

    static final RateLimitRules RULES = RateLimitRules.of(
            new RateLimitRule("otp-send-phone", 5, 60, FailPolicy.CLOSED),
            new RateLimitRule("search-ip", 5, 60, FailPolicy.OPEN),
            new RateLimitRule("login-ip", 5, 60, FailPolicy.CLOSED),
            new RateLimitRule("login-account", 5, 60, FailPolicy.CLOSED),
            new RateLimitRule("burst-ip", 10, 60, FailPolicy.CLOSED),
            new RateLimitRule("tick-ip", 2, 1, FailPolicy.OPEN));

    static final Duration COMMAND_TIMEOUT = Duration.ofMillis(200);
    static final Clock FIXED = Clock.fixed(Instant.parse("2026-09-29T10:00:00Z"), ZoneOffset.UTC);

    static RedisServerProcess redis;
    static RedisClient adminClient;
    static StatefulRedisConnection<String, String> adminConn;
    static RedisCommands<String, String> admin;

    final List<RateLimitRedis> opened = new ArrayList<>();
    PrometheusMeterRegistry registry;
    RedisFixedWindowRateLimiter limiter;

    @BeforeAll
    static void startRedis() throws Exception {
        redis = RedisServerProcess.startOnFreePort();
        adminClient = RedisClient.create(uri());
        adminConn = adminClient.connect();
        admin = adminConn.sync();
    }

    @AfterAll
    static void stopRedis() throws Exception {
        if (adminConn != null) adminConn.close();
        if (adminClient != null) adminClient.shutdown();
        if (redis != null) redis.stop();
    }

    @BeforeEach
    void setUp() {
        admin.flushall();
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        limiter = newLimiter();
    }

    @AfterEach
    void closeConnections() { opened.forEach(RateLimitRedis::close); }

    static RedisURI uri() { return RedisURI.create("redis://127.0.0.1:" + redis.port); }

    /** Her cagri ayri Lettuce baglantisi = ayri uygulama replikasi. */
    RedisFixedWindowRateLimiter newLimiter() {
        RateLimitRedis conn = RateLimitRedis.connect(uri(), COMMAND_TIMEOUT);
        opened.add(conn);
        return new RedisFixedWindowRateLimiter(conn.connection(), RULES, registry);
    }

    // --- 1. limit 5/60: 5 izin, 6. red + Retry-After <= 60 --------------------------------------------------

    @Test
    void fiveAllowedThenSixthDeniedWithRetryAfterWithinWindow() {
        String phone = "+905551112233";
        for (int i = 1; i <= 5; i++) {
            RateLimitDecision d = limiter.tryAcquire("otp-send-phone", phone);
            assertThat(d.outcome()).as("istek %d", i).isEqualTo(Outcome.ALLOWED);
            assertThat(d.remaining()).isEqualTo(5 - i);
        }
        RateLimitDecision sixth = limiter.tryAcquire("otp-send-phone", phone);
        assertThat(sixth.allowed()).isFalse();
        assertThat(sixth.outcome()).isEqualTo(Outcome.EXCEEDED);
        assertThat(sixth.errorCode()).isEqualTo(RateLimitErrorCode.RATE_LIMIT_EXCEEDED);
        assertThat(sixth.errorCode().getHttpStatus().value()).isEqualTo(429);
        assertThat(sixth.retryAfterSeconds()).isBetween(1L, 60L);
        // Red de sayilir ama pencereyi uzatmaz (fixed window): TTL 60 sn'yi asmaz.
        assertThat(limiter.tryAcquire("otp-send-phone", phone).allowed()).isFalse();
        long pttl = admin.pttl(RedisFixedWindowRateLimiter.key("otp-send-phone", phone));
        assertThat(pttl).isBetween(1L, 60_000L);
        // Baska ozne etkilenmez.
        assertThat(limiter.tryAcquire("otp-send-phone", "+905559998877").outcome()).isEqualTo(Outcome.ALLOWED);
    }

    // --- 2. pencere dolumu: 1 sn'lik pencere, sinirli yoklama ile tekrar izin --------------------------------

    @Test
    void windowExpiresAndSubjectIsAllowedAgain() throws Exception {
        String ip = "198.51.100.23";
        assertThat(limiter.tryAcquire("tick-ip", ip).allowed()).isTrue();
        assertThat(limiter.tryAcquire("tick-ip", ip).allowed()).isTrue();
        RateLimitDecision denied = limiter.tryAcquire("tick-ip", ip);
        assertThat(denied.outcome()).isEqualTo(Outcome.EXCEEDED);
        assertThat(denied.retryAfterSeconds()).isEqualTo(1);

        long start = System.nanoTime();
        RateLimitDecision d = pollUntil(() -> limiter.tryAcquire("tick-ip", ip), x -> x.outcome() == Outcome.ALLOWED,
                Duration.ofSeconds(5));
        assertThat(d.outcome()).as("1 sn'lik pencere 5 sn icinde dolmali (TTL'siz key = sonsuz kilit)")
                .isEqualTo(Outcome.ALLOWED);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
        // Yeni pencere: sayac 1'den basladi.
        assertThat(d.remaining()).isEqualTo(1);
    }

    /** INCR ve EXPIRE ayri yapan (atomik olmayan) bir surumun cokmesi TTL'siz sayac birakir; script onu iyilestirir. */
    @Test
    void ttlLessCounterLeftByNonAtomicWriterIsHealed() {
        String ip = "198.51.100.77";
        String key = RedisFixedWindowRateLimiter.key("search-ip", ip);
        admin.set(key, "40");                                   // INCR oldu, EXPIRE'dan once surec oldu
        assertThat(admin.pttl(key)).isEqualTo(-1L);
        RateLimitDecision d = limiter.tryAcquire("search-ip", ip);
        assertThat(d.outcome()).isEqualTo(Outcome.EXCEEDED);
        assertThat(d.retryAfterSeconds()).isBetween(1L, 60L);
        assertThat(admin.pttl(key)).as("sonsuz kilit kalmamali").isBetween(1L, 60_000L);
    }

    // --- 3. key hash: ham ozne Redis'e yazilmaz ------------------------------------------------------------

    @Test
    void rawSubjectNeverAppearsInRedisKeys() throws Exception {
        String phone = "+905551112233";
        String ip = "203.0.113.7";
        limiter.tryAcquire("otp-send-phone", phone);
        limiter.tryAcquire("login-ip", ip);
        limiter.tryAcquire("login-account", ip);                // ayni ozne, farkli scope

        List<String> keys = scanAll();
        assertThat(keys).hasSize(3);
        for (String k : keys) {
            assertThat(k).matches("rl:[a-z0-9-]+:[0-9a-f]{64}");
            assertThat(k).doesNotContain(phone).doesNotContain("5551112233").doesNotContain(ip).doesNotContain("203.0");
            assertThat(admin.get(k)).isEqualTo("1");            // deger yalniz sayac
        }
        // Beklenen hash testte bagimsiz hesaplanir: sha256(scope \0 ozne)
        assertThat(keys).containsExactlyInAnyOrder(
                "rl:otp-send-phone:" + sha256("otp-send-phone\0" + phone),
                "rl:login-ip:" + sha256("login-ip\0" + ip),
                "rl:login-account:" + sha256("login-account\0" + ip));
        // Scope hash'e katildigi icin ayni IP iki scope'ta iliskilendirilemez.
        assertThat(keys.stream().map(k -> k.substring(k.lastIndexOf(':') + 1)).distinct()).hasSize(3);
    }

    // --- 4. atomiklik: 50 thread, 5 replika, limit 10 -> tam 10 izin ------------------------------------------

    @Test
    void fiftyConcurrentRequestsAcrossReplicasAllowExactlyTheLimit() throws Exception {
        List<RedisFixedWindowRateLimiter> replicas = List.of(limiter, newLimiter(), newLimiter(), newLimiter(), newLimiter());
        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<RateLimitDecision>> results = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            RedisFixedWindowRateLimiter r = replicas.get(i % replicas.size());
            results.add(pool.submit(() -> { go.await(); return r.tryAcquire("burst-ip", "192.0.2.10"); }));
        }
        go.countDown();
        Map<Outcome, AtomicInteger> byOutcome = new ConcurrentHashMap<>();
        for (Future<RateLimitDecision> f : results) {
            byOutcome.computeIfAbsent(f.get(10, TimeUnit.SECONDS).outcome(), o -> new AtomicInteger()).incrementAndGet();
        }
        pool.shutdownNow();
        assertThat(byOutcome.get(Outcome.ALLOWED).get()).isEqualTo(10);
        assertThat(byOutcome.get(Outcome.EXCEEDED).get()).isEqualTo(40);
        assertThat(byOutcome).containsOnlyKeys(Outcome.ALLOWED, Outcome.EXCEEDED);
        assertThat(admin.get(RedisFixedWindowRateLimiter.key("burst-ip", "192.0.2.10"))).isEqualTo("50");
    }

    /** Bolum 22: sayac JVM'de degil; iki replika ayni limiti paylasir. */
    @Test
    void replicasShareOneCounter() {
        RedisFixedWindowRateLimiter replicaB = newLimiter();
        for (int i = 0; i < 3; i++) assertThat(limiter.tryAcquire("login-account", "acc-42").allowed()).isTrue();
        assertThat(replicaB.tryAcquire("login-account", "acc-42").remaining()).isEqualTo(1);
        assertThat(replicaB.tryAcquire("login-account", "acc-42").allowed()).isTrue();
        assertThat(replicaB.tryAcquire("login-account", "acc-42").outcome()).isEqualTo(Outcome.EXCEEDED);
        assertThat(limiter.tryAcquire("login-account", "acc-42").outcome()).isEqualTo(Outcome.EXCEEDED);
    }

    // --- 5. scope'lar bagimsiz -----------------------------------------------------------------------------

    @Test
    void scopesAreIndependent() {
        String subject = "acc-7";
        for (int i = 0; i < 5; i++) limiter.tryAcquire("login-ip", subject);
        assertThat(limiter.tryAcquire("login-ip", subject).outcome()).isEqualTo(Outcome.EXCEEDED);
        for (int i = 1; i <= 5; i++) {
            RateLimitDecision d = limiter.tryAcquire("login-account", subject);
            assertThat(d.outcome()).as("login-account istek %d", i).isEqualTo(Outcome.ALLOWED);
            assertThat(d.remaining()).isEqualTo(5 - i);
        }
        assertThat(limiter.tryAcquire("search-ip", subject).remaining()).isEqualTo(4);
    }

    @Test
    void unknownScopeIsFailClosedAndWritesNothing() {
        RateLimitDecision d = limiter.tryAcquire("upload-ip", "198.51.100.1");
        assertThat(d.outcome()).isEqualTo(Outcome.RULE_MISSING);
        assertThat(d.allowed()).isFalse();
        assertThat(d.errorCode().getHttpStatus().value()).isEqualTo(503);
        assertThat(registry.get("rate_limit.rule_missing").counter().count()).isEqualTo(1.0);
        assertThat(scanAll()).isEmpty();
    }

    // --- 6. Lua bir kez yuklenir, sonra yalniz EVALSHA -------------------------------------------------------

    @Test
    void scriptIsLoadedOnceAndInvokedByShaOnly() throws Exception {
        admin.scriptFlush();
        admin.configResetstat();
        RedisFixedWindowRateLimiter fresh = newLimiter();       // acilista SCRIPT LOAD
        for (int i = 0; i < 20; i++) fresh.tryAcquire("search-ip", "198.51.100." + i);

        String expectedSha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                .digest(RedisFixedWindowRateLimiter.SCRIPT.getBytes(StandardCharsets.UTF_8)));
        assertThat(admin.scriptExists(expectedSha)).containsExactly(true);
        Map<String, Long> stats = commandCalls();
        assertThat(stats).containsEntry("script|load", 1L).containsEntry("evalsha", 20L);
        assertThat(stats).doesNotContainKey("eval");
    }

    // --- 7. fail politikalari: Redis olur -> OPEN gecer + metrik, CLOSED 503; Redis doner -> ikisi de calisir --

    @Test
    void redisDownAppliesPerScopeFailPolicyAndRecoversAfterRestart() throws Exception {
        RateLimitFilter otpFilter = new RateLimitFilter(limiter, "otp-send-phone", r -> r.getParameter("phone"), FIXED);
        assertThat(limiter.tryAcquire("search-ip", "203.0.113.50").outcome()).isEqualTo(Outcome.ALLOWED);
        assertThat(limiter.tryAcquire("otp-send-phone", "+905550000000").outcome()).isEqualTo(Outcome.ALLOWED);

        redis.stop();
        try {
            // OPEN: istek gecer, beklemeden (komut kuyruga alinmaz), metrik artar.
            for (int i = 0; i < 3; i++) {
                long t0 = System.nanoTime();
                RateLimitDecision d = limiter.tryAcquire("search-ip", "203.0.113.50");
                assertThat(Duration.ofNanos(System.nanoTime() - t0)).isLessThan(Duration.ofSeconds(1));
                assertThat(d.outcome()).isEqualTo(Outcome.FAIL_OPEN);
                assertThat(d.allowed()).isTrue();
                assertThat(d.degraded()).isTrue();
            }
            assertThat(registry.get("rate_limit.fail_open").tag("scope", "search-ip").counter().count()).isEqualTo(3.0);
            assertThat(registry.scrape()).contains("rate_limit_fail_open_total{scope=\"search-ip\"} 3.0");

            // CLOSED: hic gorulmemis ozneler dahil HER istek reddedilir (JVM'de "yedek sayac" yok).
            // Kopukluk artik biliniyor: komut kuyrukta timeout'u beklemeden (REJECT_COMMANDS) aninda reddedilir;
            // kuyruklayan istemci her istekte tam komut timeout'u kadar thread bloklardi.
            for (int i = 0; i < 6; i++) {
                long t0 = System.nanoTime();
                RateLimitDecision d = limiter.tryAcquire("otp-send-phone", "+90555000010" + i);
                assertThat(Duration.ofNanos(System.nanoTime() - t0)).as("kopukken bekleme olmamali")
                        .isLessThan(COMMAND_TIMEOUT.dividedBy(2));
                assertThat(d.outcome()).isEqualTo(Outcome.UNAVAILABLE);
                assertThat(d.allowed()).isFalse();
                assertThat(d.errorCode()).isEqualTo(RateLimitErrorCode.RATE_LIMIT_UNAVAILABLE);
                assertThat(d.errorCode().getHttpStatus().value()).isEqualTo(503);
                assertThat(d.retryAfterSeconds()).isEqualTo(1);
            }
            assertThat(registry.get("rate_limit.fail_closed").tag("scope", "otp-send-phone").counter().count())
                    .isEqualTo(6.0);
            assertThat(registry.get("rate_limit.fail_open").tag("scope", "otp-send-phone").counter().count())
                    .isZero();
            assertThat(registry.get("rate_limit.fail_open").tag("scope", "search-ip").counter().count()).isEqualTo(3.0);

            // Filtre: 503 + Retry-After + standart zarf, istek zincire gitmez.
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/otp/send");
            req.setParameter("phone", "+905551234567");
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            otpFilter.doFilter(req, res, chain);
            assertThat(res.getStatus()).isEqualTo(503);
            assertThat(res.getHeader("Retry-After")).isEqualTo("1");
            assertThat(res.getContentAsString()).contains("\"code\":90102").doesNotContain("5551234567");
            assertThat(chain.getRequest()).isNull();
        } finally {
            redis.start();                                      // ayni port, bos veri + bos script cache
        }

        // Lettuce yeniden baglanir; ilk EVALSHA NOSCRIPT alir, script yeniden yuklenir.
        RateLimitDecision open = pollUntil(() -> limiter.tryAcquire("search-ip", "203.0.113.51"),
                d -> d.outcome() == Outcome.ALLOWED, Duration.ofSeconds(10));
        assertThat(open.outcome()).isEqualTo(Outcome.ALLOWED);
        assertThat(open.degraded()).isFalse();
        for (int i = 1; i <= 5; i++) {
            assertThat(limiter.tryAcquire("otp-send-phone", "+905550000099").outcome()).as("istek %d", i)
                    .isEqualTo(Outcome.ALLOWED);
        }
        assertThat(limiter.tryAcquire("otp-send-phone", "+905550000099").outcome()).isEqualTo(Outcome.EXCEEDED);
        assertThat(admin.scriptExists(RedisFixedWindowRateLimiter.SCRIPT_SHA)).containsExactly(true);
        double openAfter = registry.get("rate_limit.fail_open").tag("scope", "search-ip").counter().count();
        limiter.tryAcquire("search-ip", "203.0.113.52");
        assertThat(registry.get("rate_limit.fail_open").tag("scope", "search-ip").counter().count()).isEqualTo(openAfter);
    }

    /** Asili (yanit vermeyen) Redis: komut timeout'u dolunca politika uygulanir, istek thread'i bloklanmaz. */
    @Test
    void hangingRedisHitsCommandTimeoutThenPolicyApplies() throws Exception {
        admin.clientPause(1500);                                // tum istemcilerin komutlari 1.5 sn bekletilir
        long t0 = System.nanoTime();
        RateLimitDecision open = limiter.tryAcquire("search-ip", "203.0.113.90");
        RateLimitDecision closed = limiter.tryAcquire("otp-send-phone", "+905550000090");
        Duration took = Duration.ofNanos(System.nanoTime() - t0);
        assertThat(open.outcome()).isEqualTo(Outcome.FAIL_OPEN);
        assertThat(closed.outcome()).isEqualTo(Outcome.UNAVAILABLE);
        assertThat(took).as("iki cagri ~2x200 ms timeout ile donmeli, pause suresi (1.5 sn) kadar degil")
                .isLessThan(Duration.ofMillis(1200));
        RateLimitDecision back = pollUntil(() -> limiter.tryAcquire("search-ip", "203.0.113.91"),
                d -> d.outcome() == Outcome.ALLOWED, Duration.ofSeconds(10));
        assertThat(back.outcome()).isEqualTo(Outcome.ALLOWED);
    }

    // --- 8. 429 filtresi / Retry-After ---------------------------------------------------------------------

    @Test
    void filterAnswers429WithRetryAfterAndEnvelopeWithoutSubject() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(limiter, "login-ip", r -> r.getRemoteAddr(), FIXED);
        for (int i = 0; i < 5; i++) {
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(request("203.0.113.200"), new MockHttpServletResponse(), chain);
            assertThat(chain.getRequest()).as("istek %d zincire gitmeli", i + 1).isNotNull();
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("203.0.113.200"), res, chain);
        assertThat(chain.getRequest()).isNull();
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(res.getHeader("Retry-After"))).isBetween(1L, 60L);
        assertThat(res.getContentType()).startsWith("application/json");
        assertThat(res.getContentAsString())
                .contains("\"ok\":false", "\"code\":90100", "\"service\":\"security\"", "\"path\":\"/auth/login\"",
                        "\"timestamp\":" + FIXED.millis(), "retryAfterSeconds=" + res.getHeader("Retry-After"))
                .doesNotContain("203.0.113.200");
        // Baska IP etkilenmez.
        MockFilterChain other = new MockFilterChain();
        filter.doFilter(request("203.0.113.201"), new MockHttpServletResponse(), other);
        assertThat(other.getRequest()).isNotNull();
    }

    // --- yardimcilar ---------------------------------------------------------------------------------------

    static MockHttpServletRequest request(String ip) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/auth/login");
        r.setRemoteAddr(ip);
        return r;
    }

    /** Gercek zamanli altyapi (Redis TTL, TCP yeniden baglanma) icin sinirli yoklama; son sonucu dondurur. */
    static <T> T pollUntil(Supplier<T> action, java.util.function.Predicate<T> done, Duration max) throws InterruptedException {
        long deadline = System.nanoTime() + max.toNanos();
        T last = action.get();
        while (!done.test(last) && System.nanoTime() < deadline) {
            Thread.sleep(50);
            last = action.get();
        }
        return last;
    }

    static List<String> scanAll() {
        List<String> keys = new ArrayList<>();
        KeyScanCursor<String> c = admin.scan(ScanArgs.Builder.matches("*").limit(1000));
        keys.addAll(c.getKeys());
        while (!c.isFinished()) {
            c = admin.scan(ScanCursor.of(c.getCursor()), ScanArgs.Builder.matches("*").limit(1000));
            keys.addAll(c.getKeys());
        }
        return keys;
    }

    /** INFO commandstats -> komut adi (alt komut dahil, ornegin script|load) -> calls. */
    static Map<String, Long> commandCalls() {
        Map<String, Long> m = new ConcurrentHashMap<>();
        for (String line : admin.info("commandstats").split("\r?\n")) {
            if (!line.startsWith("cmdstat_")) continue;
            String name = line.substring("cmdstat_".length(), line.indexOf(':'));
            String calls = line.substring(line.indexOf("calls=") + 6, line.indexOf(',', line.indexOf("calls=")));
            m.put(name, Long.parseLong(calls));
        }
        return m;
    }

    static String sha256(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    }
}
