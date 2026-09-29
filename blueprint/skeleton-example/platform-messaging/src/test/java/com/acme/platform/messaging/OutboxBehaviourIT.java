package com.acme.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.platform.messaging.inbox.InboxProcessor;
import com.acme.platform.messaging.outbox.OutboxEvent;
import com.acme.platform.messaging.outbox.OutboxHandler;
import com.acme.platform.messaging.outbox.OutboxPoller;
import com.acme.platform.messaging.outbox.OutboxProperties;
import com.acme.platform.messaging.outbox.OutboxRepository;
import com.acme.platform.messaging.outbox.PermanentFailureException;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DAVRANISSAL dogrulama (referans Bolum 11.5 senaryo matrisi, 19.6 kanit seviyesi 2): gercek PostgreSQL uzerinde
 * outbox/inbox guvenceleri. Docker gerektirmez (gomulu PG binary'si); CI'da Testcontainers ile de kosturulabilir.
 * Her test metodu bir senaryo numarasina karsilik gelir (operation-consistency skill'i verification.md).
 */
class OutboxBehaviourIT {

    static final String SCHEMA = "order";
    static EmbeddedPostgres pg;
    static DataSource ds;
    static NamedParameterJdbcTemplate jdbc;
    static TransactionTemplate tx;

    OutboxRepository outbox;
    MutableClock clock;

    /** Deterministik zaman: kira dolumu ve backoff testleri gercek zaman beklemeden calisir. */
    static final class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2026-09-29T10:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    @BeforeAll
    static void startDb() throws Exception {
        pg = EmbeddedPostgres.builder().start();
        ds = pg.getPostgresDatabase();
        jdbc = new NamedParameterJdbcTemplate(ds);
        tx = new TransactionTemplate(new JdbcTransactionManager(ds));
        String ddl = new String(OutboxBehaviourIT.class.getResourceAsStream("/db/platform/outbox_inbox.sql")
                .readAllBytes(), StandardCharsets.UTF_8).replace("${schema}", "\"" + SCHEMA + "\"");
        JdbcTemplate plain = jdbc.getJdbcTemplate();
        plain.execute("CREATE SCHEMA \"" + SCHEMA + "\"");
        try (var conn = ds.getConnection()) {                       // yorum ve ';' guvenli script calistirma
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8)));
        }
        plain.execute("CREATE TABLE \"order\".effect (event_id UUID NOT NULL, handler TEXT NOT NULL)");
        plain.execute("""
                CREATE TABLE "order".rm_account_status (account_id UUID PRIMARY KEY, active BOOLEAN NOT NULL,
                    source_revision BIGINT NOT NULL)""");
    }

    @AfterAll
    static void stopDb() throws IOException { if (pg != null) pg.close(); }

    @BeforeEach
    void clean() {
        JdbcTemplate plain = jdbc.getJdbcTemplate();
        plain.execute("TRUNCATE \"order\".outbox_event, \"order\".inbox_event, \"order\".effect, \"order\".rm_account_status");
        outbox = new OutboxRepository(jdbc, SCHEMA);
        clock = new MutableClock();
    }

    // ---------- yardimcilar ----------

    OutboxEvent event(String kind, UUID aggregate, Instant createdAt, int priority, OutboxEvent.DeadPolicy policy) {
        return new OutboxEvent(UUID.randomUUID(), kind, "order", aggregate, "order.order.created",
                "{\"orderId\":\"" + aggregate + "\"}", "{}", "PENDING", priority, policy, 0, createdAt, null, null,
                null, createdAt);
    }

    OutboxEvent event(String kind, UUID aggregate) {
        return event(kind, aggregate, clock.instant(), 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
    }

    /** Domain TX'i taklidi: outbox yazicisi MANDATORY oldugu icin TransactionTemplate icinde cagrilir. */
    void appendInTx(OutboxEvent... events) {
        tx.executeWithoutResult(s -> { for (OutboxEvent e : events) outbox.append(e); });
    }

    OutboxPoller poller(Map<String, OutboxHandler> handlers, OutboxProperties props) {
        return new OutboxPoller(outbox, handlers, props, clock);
    }

    OutboxPoller poller(Map<String, OutboxHandler> handlers) { return poller(handlers, OutboxProperties.defaults()); }

    int rows() { return jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM \"order\".outbox_event", Integer.class); }

    int effects() { return jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM \"order\".effect", Integer.class); }

    void recordEffect(UUID eventId, String handler) {
        jdbc.update("INSERT INTO \"order\".effect (event_id, handler) VALUES (:e, :h)", Map.of("e", eventId, "h", handler));
    }

    // ---------- senaryolar ----------

    @Test // #21: outbox satiri domain TX ile rollback olur; MANDATORY: TX disinda yazilamaz
    void outboxRowRollsBackWithDomainTransaction() {
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            outbox.append(event("EVENT", UUID.randomUUID()));
            throw new IllegalStateException("domain hatasi");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(rows()).isZero();

        // MANDATORY: aktif TX yoksa yazma reddedilir (proxy'siz cagrida anotasyon uygulanmaz; bu yuzden
        // referansta yazici bir Spring bean'idir. Burada TX disinda append'in satir yazdigini gormek yerine
        // kuralin proxy ile zorlandigi Boot testinde dogrulanir — bu testin kapsami disi, BLOCKED degil, ayri seviye).
    }

    @Test // #22: tekrar teslim tek etki (inbox dedup, handler kapsaminda)
    void duplicateDeliveryHasSingleEffect() {
        InboxProcessor inbox = new InboxProcessor(jdbc, tx, SCHEMA);
        UUID eventId = UUID.randomUUID();
        assertThat(inbox.process("ReadModelHandler", eventId, () -> recordEffect(eventId, "ReadModelHandler")))
                .isEqualTo(InboxProcessor.Outcome.APPLIED);
        assertThat(inbox.process("ReadModelHandler", eventId, () -> recordEffect(eventId, "ReadModelHandler")))
                .isEqualTo(InboxProcessor.Outcome.DUPLICATE);
        // ayni olay farkli handler'da ayri islenir (dedup kapsami handler)
        assertThat(inbox.process("NotificationHandler", eventId, () -> recordEffect(eventId, "NotificationHandler")))
                .isEqualTo(InboxProcessor.Outcome.APPLIED);
        assertThat(effects()).isEqualTo(2);
    }

    @Test // #25: handler ortasinda exception -> inbox satiri YOK, etki YOK; yeniden teslimde is yapilir
    void inboxRowAndWorkAreAtomic() {
        InboxProcessor inbox = new InboxProcessor(jdbc, tx, SCHEMA);
        UUID eventId = UUID.randomUUID();
        assertThatThrownBy(() -> inbox.process("H", eventId, () -> {
            recordEffect(eventId, "H");
            throw new IllegalStateException("is ortasinda cokme");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(effects()).isZero();
        Integer inboxRows = jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM \"order\".inbox_event", Integer.class);
        assertThat(inboxRows).isZero();

        assertThat(inbox.process("H", eventId, () -> recordEffect(eventId, "H"))).isEqualTo(InboxProcessor.Outcome.APPLIED);
        assertThat(effects()).isEqualTo(1);
    }

    @Test // #22 (poller tarafi) + coklu instance: iki poller paralel, 300 satir, her satir TAM BIR kez islenir
    void twoPollersNeverProcessTheSameRow() throws Exception {
        for (int i = 0; i < 300; i++) appendInTx(event("EVENT", UUID.randomUUID()));
        ConcurrentHashMap<UUID, AtomicInteger> seen = new ConcurrentHashMap<>();
        OutboxHandler handler = e -> seen.computeIfAbsent(e.id(), k -> new AtomicInteger()).incrementAndGet();
        OutboxPoller a = poller(Map.of("EVENT", handler)), b = poller(Map.of("EVENT", handler));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Integer> fa = pool.submit(() -> drain(a)), fb = pool.submit(() -> drain(b));
        int total = fa.get(60, TimeUnit.SECONDS) + fb.get(60, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(total).isEqualTo(300);
        assertThat(seen).hasSize(300);
        assertThat(seen.values()).allMatch(c -> c.get() == 1);
        assertThat(rows()).isZero();
    }

    @Test // SKIP LOCKED: baska bir TX'in kilitledigi satir beklenmez, atlanir (instance'lar birbirini bloke etmez)
    void lockedRowIsSkippedNotWaitedFor() throws Exception {
        OutboxEvent locked = event("EVENT", UUID.randomUUID()), free = event("EVENT", UUID.randomUUID());
        appendInTx(locked, free);
        CountDownLatch held = new CountDownLatch(1), release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> holder = pool.submit(() -> tx.executeWithoutResult(s -> {      // acik TX, satir kilitli
            jdbc.queryForList("SELECT id FROM \"order\".outbox_event WHERE id = :id FOR UPDATE", Map.of("id", locked.id()));
            held.countDown();
            try { release.await(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
        }));
        try {
            assertThat(held.await(10, TimeUnit.SECONDS)).isTrue();
            Future<List<OutboxEvent>> claim = pool.submit(() ->
                    outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120), UUID.randomUUID(), 10));
            List<OutboxEvent> got = claim.get(5, TimeUnit.SECONDS);               // SKIP LOCKED yoksa burada bloke olur
            assertThat(got).extracting(OutboxEvent::id).containsExactly(free.id());
        } finally {
            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
            pool.shutdownNow();
        }
    }

    int drain(OutboxPoller p) {
        int applied = 0;
        OutboxPoller.PollResult r;
        do { r = p.poll("EVENT"); applied += r.applied(); } while (r.claimed() > 0);
        return applied;
    }

    @Test // #27: ayni aggregate'in sirali iki satiri tek worker'da ve sirayla; ilk satir basarisizsa ikincisi bekler
    void orderedRowsOfSameAggregateStayOrdered() {
        UUID agg = UUID.randomUUID();
        Instant t0 = clock.instant();
        OutboxEvent first = event("EVENT", agg, t0, 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
        OutboxEvent second = event("EVENT", agg, t0.plusMillis(1), 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
        appendInTx(first, second);

        // A ilk satiri claim eder (handler cagirmadan): ikinci satir baska worker'a verilmez
        List<OutboxEvent> claimedByA = outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120),
                UUID.randomUUID(), 10);
        assertThat(claimedByA).extracting(OutboxEvent::id).containsExactly(first.id());
        List<OutboxEvent> claimedByB = outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120),
                UUID.randomUUID(), 10);
        assertThat(claimedByB).isEmpty();

        // A ilk satirda basarisiz olur -> PENDING + backoff; ikinci satir hala bloke (sira korunur)
        outbox.release(first.id(), claimedByA.get(0).claimToken(), "PENDING", 1, clock.instant().plusSeconds(60), "IOException");
        assertThat(outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120), UUID.randomUUID(), 10)).isEmpty();

        // backoff gecince once ilk, sonra ikinci islenir
        clock.advance(Duration.ofSeconds(61));
        List<UUID> order = new java.util.ArrayList<>();
        OutboxPoller p = poller(Map.of("EVENT", e -> order.add(e.id())));
        assertThat(p.poll("EVENT").applied()).isEqualTo(1);     // yalniz first (second NOT EXISTS ile bekler)
        assertThat(p.poll("EVENT").applied()).isEqualTo(1);     // simdi second
        assertThat(order).containsExactly(first.id(), second.id());
    }

    @Test // #32: surec olur, kira dolar, ikinci instance devralir; geri gelen eski worker yazamaz
    void expiredLeaseIsTakenOverAndLateWorkerCannotWrite() {
        OutboxEvent e = event("EVENT", UUID.randomUUID());
        appendInTx(e);
        UUID tokenA = UUID.randomUUID();
        List<OutboxEvent> byA = outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120), tokenA, 10);
        assertThat(byA).hasSize(1);
        // A "coker": handler'i bitirmez. Kira dolmadan B alamaz.
        clock.advance(Duration.ofSeconds(119));
        assertThat(outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120), UUID.randomUUID(), 10)).isEmpty();
        // Kira dolar: B devralir (henuz bitirmedi: satir PUBLISHING, token B)
        clock.advance(Duration.ofSeconds(2));
        UUID tokenB = UUID.randomUUID();
        assertThat(outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120), tokenB, 10)).hasSize(1);
        // A geri gelir (isi bitirdigini sanir): eski token ile ne silebilir ne PENDING'e cekebilir; B'nin claim'i bozulmaz
        assertThat(outbox.deleteProcessed(e.id(), tokenA)).isZero();
        assertThat(outbox.release(e.id(), tokenA, "PENDING", 1, clock.instant(), "x")).isZero();
        OutboxEvent row = outbox.findAll().get(0);
        assertThat(row.status()).isEqualTo("PUBLISHING");
        assertThat(row.claimToken()).isEqualTo(tokenB);
        // B bitirir
        assertThat(outbox.deleteProcessed(e.id(), tokenB)).isEqualTo(1);
        assertThat(rows()).isZero();
    }

    @Test // #28: bir lane'de takili hedef diger lane'i bekletmez
    void stuckHttpLaneDoesNotBlockEventLane() throws Exception {
        appendInTx(event("HTTP", UUID.randomUUID()), event("EVENT", UUID.randomUUID()));
        CountDownLatch httpBlocked = new CountDownLatch(1), release = new CountDownLatch(1);
        OutboxHandler http = e -> { httpBlocked.countDown(); release.await(30, TimeUnit.SECONDS); };
        AtomicInteger events = new AtomicInteger();
        OutboxPoller p = poller(Map.of("HTTP", http, "EVENT", e -> events.incrementAndGet()));

        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<OutboxPoller.PollResult> httpRun = pool.submit(() -> p.poll("HTTP"));
        assertThat(httpBlocked.await(10, TimeUnit.SECONDS)).isTrue();          // HTTP lane'i sagliyicida takili
        assertThat(p.poll("EVENT").applied()).isEqualTo(1);                   // EVENT lane'i beklemeden yayinladi
        assertThat(events.get()).isEqualTo(1);
        release.countDown();
        assertThat(httpRun.get(10, TimeUnit.SECONDS).applied()).isEqualTo(1);
        pool.shutdownNow();
    }

    @Test // gecici hata -> backoff ile yeniden deneme -> basari; deneme sayisi ve hata kodu (mesaj degil) kaydedilir
    void transientFailureIsRetriedWithBackoff() {
        OutboxEvent e = event("EVENT", UUID.randomUUID());
        appendInTx(e);
        AtomicInteger calls = new AtomicInteger();
        OutboxPoller p = poller(Map.of("EVENT", ev -> {
            if (calls.incrementAndGet() == 1) throw new java.io.IOException("broker unreachable: secret-host");
        }));
        OutboxPoller.PollResult r1 = p.poll("EVENT");
        assertThat(r1.failed()).isEqualTo(1);
        OutboxEvent after = outbox.findAll().get(0);
        assertThat(after.status()).isEqualTo("PENDING");
        assertThat(after.retryCount()).isEqualTo(1);
        assertThat(after.lastErrorCode()).isEqualTo("IOException");            // mesaj/host yok
        assertThat(after.nextRetryAt()).isEqualTo(clock.instant().plusSeconds(60));   // 30*2^1
        assertThat(after.claimToken()).isNull();

        assertThat(p.poll("EVENT").claimed()).isZero();                       // backoff dolmadan alinmaz
        clock.advance(Duration.ofSeconds(61));
        assertThat(p.poll("EVENT").applied()).isEqualTo(1);
        assertThat(rows()).isZero();
    }

    @Test // DEAD politikasi is turune gore: kalici hata -> DEAD; guvenlik yan etkisi (NEVER_DEAD) -> yeniden dener
    void deadPolicyDependsOnJobType() {
        OutboxEvent normal = event("COMMAND", UUID.randomUUID(), clock.instant(), 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
        OutboxEvent security = event("COMMAND", UUID.randomUUID(), clock.instant(), 10, OutboxEvent.DeadPolicy.NEVER_DEAD);
        appendInTx(normal, security);
        OutboxPoller p = poller(Map.of("COMMAND", e -> { throw new PermanentFailureException("422"); }));
        OutboxPoller.PollResult r = p.poll("COMMAND");
        assertThat(r.dead()).isEqualTo(1);
        assertThat(r.failed()).isEqualTo(1);
        Map<UUID, String> status = new java.util.HashMap<>();
        outbox.findAll().forEach(e -> status.put(e.id(), e.status()));
        assertThat(status.get(normal.id())).isEqualTo("DEAD");
        assertThat(status.get(security.id())).isEqualTo("PENDING");
    }

    @Test // oncelik: yuksek priority once claim edilir (guvenlik karari toplu isi beklemez)
    void higherPriorityIsClaimedFirst() {
        Instant t = clock.instant();   // ucu de gecmiste (next_retry_at <= now); urgent en yeni ama once alinmali
        OutboxEvent bulk1 = event("COMMAND", UUID.randomUUID(), t.minusMillis(3), 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
        OutboxEvent bulk2 = event("COMMAND", UUID.randomUUID(), t.minusMillis(2), 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
        OutboxEvent urgent = event("COMMAND", UUID.randomUUID(), t.minusMillis(1), 10, OutboxEvent.DeadPolicy.NEVER_DEAD);
        appendInTx(bulk1, bulk2, urgent);
        List<OutboxEvent> claimed = outbox.claim("COMMAND", clock.instant(), clock.instant().plusSeconds(120), UUID.randomUUID(), 1);
        assertThat(claimed).extracting(OutboxEvent::id).containsExactly(urgent.id());
    }

    @Test // #29: eski karar (kucuk source_revision) yeni karari ezmez — read-model UPSERT kurali
    void staleDecisionDoesNotOverrideNewerOne() {
        UUID account = UUID.randomUUID();
        String upsert = """
                INSERT INTO "order".rm_account_status (account_id, active, source_revision) VALUES (:a, :active, :rev)
                ON CONFLICT (account_id) DO UPDATE SET active = EXCLUDED.active, source_revision = EXCLUDED.source_revision
                WHERE EXCLUDED.source_revision > "order".rm_account_status.source_revision""";
        jdbc.update(upsert, Map.of("a", account, "active", false, "rev", 2L));   // yeni karar: engellendi
        int changed = jdbc.update(upsert, Map.of("a", account, "active", true, "rev", 1L));   // gec gelen eski karar
        assertThat(changed).isZero();
        Boolean active = jdbc.queryForObject("SELECT active FROM \"order\".rm_account_status WHERE account_id = :a",
                Map.of("a", account), Boolean.class);
        assertThat(active).isFalse();
        // ayni revizyon tekrar (duplicate) da degistirmez
        assertThat(jdbc.update(upsert, Map.of("a", account, "active", true, "rev", 2L))).isZero();
    }

    @Test // kira guvenlik payi: sure dolmak uzereyken yeni satira baslanmaz (deferred), deneme sayilmaz
    void rowsAreDeferredWhenLeaseSafetyWindowIsReached() {
        appendInTx(event("EVENT", UUID.randomUUID()), event("EVENT", UUID.randomUUID()));
        OutboxPoller p = poller(Map.of("EVENT", e -> clock.advance(Duration.ofSeconds(100))),  // ilk is 100 sn surer
                new OutboxProperties(50, 120, 30, 600, 10));
        OutboxPoller.PollResult r = p.poll("EVENT");
        assertThat(r.applied()).isEqualTo(1);
        assertThat(r.deferred()).isEqualTo(1);
        OutboxEvent left = outbox.findAll().get(0);
        assertThat(left.retryCount()).isZero();                                    // deferred deneme degildir
        assertThat(left.status()).isEqualTo("PUBLISHING");                         // kira dolunca baska instance alir
        clock.advance(Duration.ofSeconds(30));
        assertThat(poller(Map.of("EVENT", e -> {})).poll("EVENT").applied()).isEqualTo(1);
    }
}
