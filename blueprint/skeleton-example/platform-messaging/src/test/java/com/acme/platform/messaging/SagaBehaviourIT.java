package com.acme.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.platform.messaging.saga.FlakyParticipant;
import com.acme.platform.messaging.saga.LocalSagaStore;
import com.acme.platform.messaging.saga.LocalSagaStore.Action;
import com.acme.platform.messaging.saga.LocalSagaStore.SagaStatus;
import com.acme.platform.messaging.saga.LocalSagaStore.Step;
import com.acme.platform.messaging.saga.LocalSagaStore.StepStatus;
import com.acme.platform.messaging.saga.OrderFlow;
import com.acme.platform.messaging.saga.OrderFlow.CrashPoint;
import com.acme.platform.messaging.saga.OrderFlow.Kind;
import com.acme.platform.messaging.saga.OrderFlow.Result;
import com.acme.platform.messaging.saga.QuotaParticipant;
import com.acme.platform.messaging.saga.SagaParticipant;
import com.acme.platform.messaging.saga.SagaParticipant.State;
import com.acme.platform.messaging.saga.SagaProperties;
import com.acme.platform.messaging.saga.SagaRecoveryWorker;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DAVRANISSAL dogrulama (referans Bolum 11.4-11.5, 19.6 kanit seviyesi 2): local saga guvenceleri gercek PostgreSQL
 * uzerinde. Katilimci in-process (HTTP/JWT katmani bu testin KAPSAMI DISINDA; belirsizlikler FlakyParticipant ile
 * enjekte edilir). Metot basliklarindaki # numaralari operation-consistency skill'i verification.md matrisidir.
 */
class SagaBehaviourIT {

    static EmbeddedPostgres pg;
    static DataSource ds;
    static NamedParameterJdbcTemplate jdbc;
    static JdbcTransactionManager tm;
    static TransactionTemplate tx;

    static final class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2026-09-29T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    MutableClock clock;
    SagaProperties props;
    LocalSagaStore store;
    QuotaParticipant quota;
    FlakyParticipant participant;
    SagaRecoveryWorker worker;
    OrderFlow flow;
    UUID account;

    @BeforeAll
    static void startDb() throws Exception {
        pg = EmbeddedPostgres.builder().start();
        ds = pg.getPostgresDatabase();
        jdbc = new NamedParameterJdbcTemplate(ds);
        tm = new JdbcTransactionManager(ds);
        tx = new TransactionTemplate(tm);
        String ddl = new String(SagaBehaviourIT.class.getResourceAsStream("/db/platform/saga_coordinator.sql").readAllBytes(),
                StandardCharsets.UTF_8).replace("${schema}", "\"order\"");
        jdbc.getJdbcTemplate().execute("CREATE SCHEMA \"order\"");
        jdbc.getJdbcTemplate().execute("CREATE SCHEMA subscription");
        try (var conn = ds.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8)));
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(OrderFlow.DDL.getBytes(StandardCharsets.UTF_8)));
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(QuotaParticipant.DDL.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @AfterAll
    static void stopDb() throws Exception { if (pg != null) pg.close(); }

    @BeforeEach
    void setUp() {
        jdbc.getJdbcTemplate().execute("TRUNCATE \"order\".saga, \"order\".saga_steps, \"order\".order_item, subscription.quota, subscription.operation");
        clock = new MutableClock();
        props = SagaProperties.defaults();                       // deadline 15 sn, lease 60 sn, backoff min(300, 2^n), uyari 15 dk, retention 30 gun
        store = new LocalSagaStore(jdbc, tm, "order", props, clock);
        quota = new QuotaParticipant(jdbc, tx);
        participant = new FlakyParticipant(quota);
        worker = new SagaRecoveryWorker(store, participant, "order-service", props, clock);
        flow = new OrderFlow(store, participant, jdbc, tx);
        account = UUID.randomUUID();
        quota.grant(account, 5);
    }

    // ---------- yardimcilar ----------

    SagaStatus sagaStatus(UUID sagaId) { return store.findById(sagaId).orElseThrow().status(); }
    Step step(UUID sagaId) { return store.stepsOf(sagaId).get(0); }
    State opState(UUID key) { return quota.get("order-service", account, key).orElse(null); }
    void pastDeadline() { clock.advance(Duration.ofSeconds(props.deadlineSeconds() + 1)); }
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) { } }

    // ---------- senaryolar ----------

    @Test // #1 normal basari: saga CONFIRMED, hak tuketildi, domain yazildi
    void normalSuccess() {
        Result r = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());
        assertThat(r.kind()).isEqualTo(Kind.OK);
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.SUCCEEDED);
        assertThat(step(r.sagaId()).nextAction()).isEqualTo(Action.CONFIRM);
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.CONFIRMED);
        assertThat(step(r.sagaId()).status()).isEqualTo(StepStatus.DONE);
        assertThat(quota.remaining(account)).isEqualTo(4);
        assertThat(flow.orderCount(account)).isEqualTo(1);
        assertThat(worker.runOnce().claimed()).isZero();
    }

    @Test // #2 ayni key ile replay: ayni sonuc, ikinci consume yok (confirm oncesi ve sonrasi)
    void replayWithSameKey() {
        UUID key = UUID.randomUUID(), resource = UUID.randomUUID();
        Result first = flow.createOrder(account, key, resource);
        Result beforeConfirm = flow.createOrder(account, key, resource);
        worker.runOnce();
        Result afterConfirm = flow.createOrder(account, key, resource);
        assertThat(beforeConfirm.kind()).isEqualTo(Kind.REPLAY);
        assertThat(afterConfirm.kind()).isEqualTo(Kind.REPLAY);
        assertThat(afterConfirm.detail()).isEqualTo(first.detail());
        assertThat(quota.consumeApplied.get()).isEqualTo(1);
        assertThat(flow.orderCount(account)).isEqualTo(1);
        assertThat(store.countSagas()).isEqualTo(1);
    }

    @Test // #3 ayni key farkli body: ilk istek kazanir (fingerprint tutulmaz, belgelenir)
    void sameKeyDifferentBodyFirstWins() {
        UUID key = UUID.randomUUID();
        Result first = flow.createOrder(account, key, UUID.randomUUID());
        Result second = flow.createOrder(account, key, UUID.randomUUID());
        assertThat(second.kind()).isEqualTo(Kind.REPLAY);
        assertThat(second.detail()).isEqualTo(first.detail());
        assertThat(flow.orderCount(account)).isEqualTo(1);
    }

    @Test // #4 eszamanli ayni key: tek saga, tek consume, tek siparis
    void concurrentSameKeyProducesSingleSaga() throws Exception {
        UUID key = UUID.randomUUID(), resource = UUID.randomUUID();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        List<Future<Result>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) futures.add(pool.submit(() -> { barrier.await(); return flow.createOrder(account, key, resource); }));
        List<Kind> kinds = new ArrayList<>();
        for (Future<Result> f : futures) kinds.add(f.get(30, TimeUnit.SECONDS).kind());
        pool.shutdownNow();
        assertThat(kinds).filteredOn(k -> k == Kind.OK).hasSize(1);
        assertThat(kinds).allMatch(k -> k == Kind.OK || k == Kind.REPLAY || k == Kind.IN_PROGRESS);
        assertThat(quota.consumeApplied.get()).isEqualTo(1);
        assertThat(store.countSagas()).isEqualTo(1);
        assertThat(flow.orderCount(account)).isEqualTo(1);
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(quota.remaining(account)).isEqualTo(4);
    }

    @Test // #5 farkli key ayni kaynak: domain uniqueness reddeder, saga telafi edilir (iade)
    void differentKeySameResourceIsCompensated() {
        UUID resource = UUID.randomUUID();
        Result first = flow.createOrder(account, UUID.randomUUID(), resource);
        Result second = flow.createOrder(account, UUID.randomUUID(), resource);
        assertThat(first.kind()).isEqualTo(Kind.OK);
        assertThat(second.kind()).isEqualTo(Kind.DOMAIN_CONFLICT);
        assertThat(sagaStatus(second.sagaId())).isEqualTo(SagaStatus.CANCEL_REQUESTED);
        assertThat(quota.remaining(account)).isEqualTo(3);                 // ikinci consume uygulandi, henuz iade yok
        SagaRecoveryWorker.RunResult r = worker.runOnce();                  // hem confirm (1.) hem compensate (2.)
        assertThat(r.done()).isEqualTo(2);
        assertThat(sagaStatus(first.sagaId())).isEqualTo(SagaStatus.CONFIRMED);
        assertThat(sagaStatus(second.sagaId())).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(quota.remaining(account)).isEqualTo(4);
        assertThat(quota.refunds.get()).isEqualTo(1);
        assertThat(flow.orderCount(account)).isEqualTo(1);
    }

    @Test // #6 ayni UUID farkli hesap: ayri saga'lar; #18 yanlis aktor: yetki reddi, kayit yok
    void sameKeyDifferentAccountIsSeparate_andWrongActorIsForbidden() {
        UUID key = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        quota.grant(other, 5);
        assertThat(flow.createOrder(account, key, UUID.randomUUID()).kind()).isEqualTo(Kind.OK);
        assertThat(flow.createOrder(other, key, UUID.randomUUID()).kind()).isEqualTo(Kind.OK);
        assertThat(store.countSagas()).isEqualTo(2);

        UUID k2 = UUID.randomUUID();
        assertThatThrownBy(() -> quota.consume("chat-service", account, k2, "ORDER_QUOTA", 1))
                .isInstanceOf(SagaParticipant.ParticipantForbiddenException.class);
        assertThat(quota.get("chat-service", account, k2)).isEmpty();
        assertThat(quota.remaining(account)).isEqualTo(4);
    }

    @Test // #7 begin sonrasi, consume oncesi cokme: deadline -> otomatik telafi (tombstone); #11 gec gelen consume uygulanmaz
    void crashAfterBeginIsCompensatedAfterDeadline_andLateConsumeHitsTombstone() {
        UUID key = UUID.randomUUID();
        flow.crashAt = CrashPoint.AFTER_BEGIN;
        assertThatThrownBy(() -> flow.createOrder(account, key, UUID.randomUUID())).isInstanceOf(OrderFlow.SimulatedCrash.class);
        UUID sagaId = store.find(account, "ORDER_CREATE", key).orElseThrow().id();
        assertThat(sagaStatus(sagaId)).isEqualTo(SagaStatus.STARTED);
        assertThat(worker.runOnce().claimed()).isZero();                    // deadline dolmadan dokunulmaz
        pastDeadline();
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(sagaStatus(sagaId)).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(opState(key)).isEqualTo(State.CANCELLED);                // tombstone
        // gec gelen consume (ornegin agda takilmis istek): uygulanmaz, hak dusmez
        assertThat(quota.consume("order-service", account, key, "ORDER_QUOTA", 1)).isEqualTo(State.CANCELLED);
        assertThat(quota.remaining(account)).isEqualTo(5);
        assertThat(quota.consumeApplied.get()).isZero();
        // istemci ayni key ile tekrar denerse: OPERATION_CANCELLED (yeni niyet = yeni key)
        flow.crashAt = CrashPoint.NONE;
        assertThat(flow.createOrder(account, key, UUID.randomUUID()).kind()).isEqualTo(Kind.CANCELLED);
    }

    @Test // #8 katilimci commit etti, yanit kayboldu: istek 503; retry IN_PROGRESS; deadline sonrasi GET/compensate -> iade
    void participantCommittedButResponseLost() {
        UUID key = UUID.randomUUID();
        participant.loseResponseNext("consume");
        Result r = flow.createOrder(account, key, UUID.randomUUID());
        assertThat(r.kind()).isEqualTo(Kind.UPSTREAM_UNAVAILABLE);
        assertThat(quota.remaining(account)).isEqualTo(4);                  // katilimcida uygulandi
        assertThat(flow.createOrder(account, key, UUID.randomUUID()).kind()).isEqualTo(Kind.IN_PROGRESS);
        pastDeadline();
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(opState(key)).isEqualTo(State.COMPENSATED);
        assertThat(quota.remaining(account)).isEqualTo(5);
        assertThat(flow.orderCount(account)).isZero();
        assertThat(flow.createOrder(account, key, UUID.randomUUID()).kind()).isEqualTo(Kind.CANCELLED);
    }

    @Test // #8b istek katilimciya hic ulasmadi (timeout): saga STARTED; recovery tombstone ile kapatir
    void participantUnreachableIsCompensatedWithTombstone() {
        UUID key = UUID.randomUUID();
        participant.failNext("consume");
        Result r = flow.createOrder(account, key, UUID.randomUUID());
        assertThat(r.kind()).isEqualTo(Kind.UPSTREAM_UNAVAILABLE);
        assertThat(quota.remaining(account)).isEqualTo(5);
        pastDeadline();
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(opState(key)).isEqualTo(State.CANCELLED);
    }

    @Test // #9 consume commit + domain rollback (cokme): recovery compensate -> iade
    void consumeCommittedThenDomainNeverWritten() {
        UUID key = UUID.randomUUID();
        flow.crashAt = CrashPoint.AFTER_CONSUME;
        assertThatThrownBy(() -> flow.createOrder(account, key, UUID.randomUUID())).isInstanceOf(OrderFlow.SimulatedCrash.class);
        assertThat(quota.remaining(account)).isEqualTo(4);
        assertThat(flow.orderCount(account)).isZero();
        pastDeadline();
        assertThat(worker.runOnce().done()).isEqualTo(1);
        UUID sagaId = store.find(account, "ORDER_CREATE", key).orElseThrow().id();
        assertThat(sagaStatus(sagaId)).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(quota.remaining(account)).isEqualTo(5);
        assertThat(quota.refunds.get()).isEqualTo(1);
    }

    @Test // #10 domain commit + confirm oncesi cokme; #20 restart: yeni instance'lar in-flight saga'yi kurtarir
    void domainCommittedThenCrashBeforeConfirm_recoveredAfterRestart() {
        Result r = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());
        assertThat(r.kind()).isEqualTo(Kind.OK);
        // "restart": eski nesneler atilir; yalniz DB'deki durum kalir
        LocalSagaStore freshStore = new LocalSagaStore(jdbc, tm, "order", props, clock);
        SagaRecoveryWorker freshWorker = new SagaRecoveryWorker(freshStore, new QuotaParticipant(jdbc, tx), "order-service", props, clock);
        assertThat(freshWorker.runOnce().done()).isEqualTo(1);
        assertThat(freshStore.findById(r.sagaId()).orElseThrow().status()).isEqualTo(SagaStatus.CONFIRMED);
        assertThat(opState(store.findById(r.sagaId()).orElseThrow().operationKey())).isEqualTo(State.CONFIRMED);
        assertThat(quota.remaining(account)).isEqualTo(4);
        assertThat(flow.orderCount(account)).isEqualTo(1);
    }

    @Test // #12 confirm timeout: retry + backoff; belirsizlikte GET (yanit kayboldu ama katilimci confirm etti -> retry yok)
    void confirmTimeoutIsRetried_andLostResponseIsResolvedByGet() {
        Result r = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());
        participant.failNext("confirm").failNext("get");                    // katilimci tamamen erisilemez
        SagaRecoveryWorker.RunResult first = worker.runOnce();
        assertThat(first.retried()).isEqualTo(1);
        Step s = step(r.sagaId());
        assertThat(s.status()).isEqualTo(StepStatus.RETRY);
        assertThat(s.attempt()).isEqualTo(1);
        assertThat(s.lastErrorCode()).isEqualTo("ParticipantUnavailableException");
        assertThat(s.nextAttemptAt()).isEqualTo(clock.instant().plusSeconds(2));   // 2^1
        assertThat(worker.runOnce().claimed()).isZero();                    // backoff dolmadan alinmaz
        clock.advance(Duration.ofSeconds(3));
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.CONFIRMED);

        // yanit kayboldu: katilimci confirm etti; worker GET ile CONFIRMED gorur ve retry yapmadan tamamlar
        Result r2 = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());
        participant.loseResponseNext("confirm");
        SagaRecoveryWorker.RunResult res = worker.runOnce();
        assertThat(res.done()).isEqualTo(1);
        assertThat(res.retried()).isZero();
        assertThat(sagaStatus(r2.sagaId())).isEqualTo(SagaStatus.CONFIRMED);
    }

    @Test // #13 eszamanli confirm ve compensate (katilimci): tek sonuc; celiski MANUAL_REVIEW; cift iade yok
    void concurrentConfirmAndCompensateYieldSingleOutcome() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 10; i++) {
                UUID key = UUID.randomUUID();
                assertThat(quota.consume("order-service", account, key, "ORDER_QUOTA", 1)).isEqualTo(State.APPLIED);
                int before = quota.remaining(account);
                CyclicBarrier barrier = new CyclicBarrier(2);
                Future<Object> c = pool.submit(() -> { barrier.await(); try { return quota.confirm("order-service", account, key); } catch (RuntimeException e) { return e; } });
                Future<Object> k = pool.submit(() -> { barrier.await(); try { return quota.compensate("order-service", account, key); } catch (RuntimeException e) { return e; } });
                Object confirmRes = c.get(30, TimeUnit.SECONDS), compRes = k.get(30, TimeUnit.SECONDS);
                State finalState = opState(key);
                if (finalState == State.MANUAL_REVIEW) {                        // confirm once kazandi: iade yok, insan karari
                    assertThat(confirmRes).isEqualTo(State.CONFIRMED);
                    assertThat(compRes).isEqualTo(State.MANUAL_REVIEW);
                    assertThat(quota.remaining(account)).isEqualTo(before);
                } else {                                                        // compensate once kazandi: iade; confirm celiski
                    assertThat(finalState).isEqualTo(State.COMPENSATED);
                    assertThat(compRes).isEqualTo(State.COMPENSATED);
                    assertThat(confirmRes).isInstanceOf(SagaParticipant.ParticipantConflictException.class);
                    assertThat(quota.remaining(account)).isEqualTo(before + 1);
                }
                quota.grant(account, 5);                                        // sonraki tur icin sifirla
            }
        } finally { pool.shutdownNow(); }
        assertThat(quota.refunds.get()).isLessThanOrEqualTo(10);
    }

    @Test // #14 iki worker + expired lease: eski worker complete edemez
    void expiredLeaseIsTakenOverAndOldWorkerCannotComplete() {
        Result r = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());
        UUID tokenA = UUID.randomUUID();
        List<Step> byA = store.claimSteps(tokenA, clock.instant(), clock.instant().plusSeconds(props.leaseSeconds()), 10);
        assertThat(byA).hasSize(1);                                          // A claim etti ve "coktu"
        clock.advance(Duration.ofSeconds(props.leaseSeconds() - 1));
        assertThat(worker.runOnce().claimed()).isZero();
        clock.advance(Duration.ofSeconds(2));
        assertThat(worker.runOnce().done()).isEqualTo(1);                    // B devraldi
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.CONFIRMED);
        assertThat(store.complete(byA.get(0), tokenA, Action.CONFIRM)).isFalse();   // A geri geldi: token eslesmez
        assertThat(store.prepare(byA.get(0), tokenA)).isEmpty();
        assertThat(step(r.sagaId()).status()).isEqualTo(StepStatus.DONE);
    }

    @Test // #15a recovery once iptal etti: istek yolunun success() CAS'i kaybeder, domain yazimi rollback olur
    void recoveryCancelBeforeSuccessRollsBackDomainWrite() {
        UUID key = UUID.randomUUID();
        flow.beforeSuccess = () -> {                                          // domain TX acikken recovery BASKA thread'de kosar
            pastDeadline();
            ExecutorService other = Executors.newSingleThreadExecutor();
            try { assertThat(other.submit(worker::runOnce).get(30, TimeUnit.SECONDS).done()).isEqualTo(1); }
            catch (Exception e) { throw new IllegalStateException(e); }
            finally { other.shutdownNow(); }
        };
        Result r = flow.createOrder(account, key, UUID.randomUUID());
        assertThat(r.kind()).isEqualTo(Kind.CANCELLED);
        assertThat(flow.orderCount(account)).isZero();                        // rollback
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(quota.remaining(account)).isEqualTo(5);                    // iade edildi
        assertThat(opState(key)).isEqualTo(State.COMPENSATED);
    }

    @Test // #15b gercek yaris: istek ile recovery paralel; her turda tutarli uc sonuctan biri, asla yarim durum
    void requestSuccessVersusRecoveryCancelRaceIsAlwaysConsistent() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        int confirmed = 0, compensated = 0;
        try {
            for (int i = 0; i < 20; i++) {
                UUID key = UUID.randomUUID();
                CountDownLatch begun = new CountDownLatch(1);
                // Iki tarafa da degisen gecikme: bazen istek, bazen recovery once davranir; her iki dal da gorulmeli
                final long requestJitter = (i % 4) * 3L, workerDelay = ((i + 2) % 5) * 4L;
                flow.beforeSuccess = () -> sleep(requestJitter);
                flow.afterBegin = () -> { pastDeadline(); begun.countDown(); };   // begin commit oldu; deadline gecmis sayilir; worker baslayabilir
                Future<Result> req = pool.submit(() -> flow.createOrder(account, key, UUID.randomUUID()));
                begun.await();
                Future<SagaRecoveryWorker.RunResult> rec = pool.submit(() -> { sleep(workerDelay); return worker.runOnce(); });
                Result r = req.get(30, TimeUnit.SECONDS);
                rec.get(30, TimeUnit.SECONDS);
                for (int n = 0; n < 3; n++) worker.runOnce();                   // kalan adimi kapat
                SagaStatus status = sagaStatus(r.sagaId());
                int orders = flow.orderCount(account), remaining = quota.remaining(account);
                if (status == SagaStatus.CONFIRMED) {
                    confirmed++;
                    assertThat(r.kind()).isEqualTo(Kind.OK);
                    assertThat(orders).isEqualTo(1);
                    assertThat(remaining).isEqualTo(4);
                } else {
                    compensated++;
                    assertThat(status).isEqualTo(SagaStatus.COMPENSATED);
                    assertThat(r.kind()).isIn(Kind.CANCELLED, Kind.REJECTED);
                    assertThat(orders).isZero();
                    assertThat(remaining).isEqualTo(5);
                }
                jdbc.getJdbcTemplate().execute("TRUNCATE \"order\".order_item");
                quota.grant(account, 5);
            }
        } finally { pool.shutdownNow(); flow.afterBegin = () -> {}; }
        assertThat(confirmed + compensated).isEqualTo(20);
        System.out.println("RACE_OUTCOMES confirmed=" + confirmed + " compensated=" + compensated);
    }

    @Test // #16 tekrarlanan compensate: idempotent, tek iade
    void repeatedCompensateRefundsOnce() {
        UUID key = UUID.randomUUID();
        assertThat(quota.consume("order-service", account, key, "ORDER_QUOTA", 1)).isEqualTo(State.APPLIED);
        assertThat(quota.compensate("order-service", account, key)).isEqualTo(State.COMPENSATED);
        assertThat(quota.compensate("order-service", account, key)).isEqualTo(State.COMPENSATED);
        assertThat(quota.remaining(account)).isEqualTo(5);
        assertThat(quota.refunds.get()).isEqualTo(1);
        // confirm sonrasi compensate: iade yok, MANUAL_REVIEW
        UUID k2 = UUID.randomUUID();
        quota.consume("order-service", account, k2, "ORDER_QUOTA", 1);
        quota.confirm("order-service", account, k2);
        assertThat(quota.compensate("order-service", account, k2)).isEqualTo(State.MANUAL_REVIEW);
        assertThat(quota.remaining(account)).isEqualTo(4);
    }

    @Test // #17 eksik/bozuk key: saga acilmaz (HTTP 400 binding'i MVC testinin isi; burada store seviyesi)
    void missingKeyOrScopeIsRejectedBeforeAnySideEffect() {
        assertThatThrownBy(() -> store.begin(account, "ORDER_CREATE", null, "quota")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> store.begin(account, " ", UUID.randomUUID(), "quota")).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.countSagas()).isZero();
        assertThatThrownBy(() -> store.success(UUID.randomUUID(), "x")).isInstanceOf(IllegalStateException.class);  // TX disinda success yok
    }

    @Test // #19 monitor: 15 dk'dan eski cozulmemis kayit sayilir; cleanup: terminal 30 gun sonra silinir, MANUAL_REVIEW asla
    void monitorCountsUnresolved_andCleanupKeepsManualReview() {
        Result ok = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());     // -> CONFIRMED
        worker.runOnce();
        Result manual = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID()); // katilimci disaridan compensate edilmis -> confirm celiskisi
        quota.compensate("order-service", account, store.findById(manual.sagaId()).orElseThrow().operationKey());
        assertThat(worker.runOnce().manualReview()).isEqualTo(1);
        assertThat(sagaStatus(manual.sagaId())).isEqualTo(SagaStatus.MANUAL_REVIEW);
        flow.crashAt = CrashPoint.AFTER_BEGIN;
        UUID stuckKey = UUID.randomUUID();
        assertThatThrownBy(() -> flow.createOrder(account, stuckKey, UUID.randomUUID())).isInstanceOf(OrderFlow.SimulatedCrash.class);

        assertThat(store.countUnresolved()).isZero();                         // henuz 15 dk gecmedi
        clock.advance(Duration.ofMinutes(16));
        assertThat(store.countUnresolved()).isEqualTo(1);                     // yalniz STARTED; MANUAL_REVIEW cozulmus sayilir (insan)
        assertThat(store.cleanup()).isZero();                                 // 30 gun gecmedi
        clock.advance(Duration.ofDays(31));
        assertThat(store.cleanup()).isEqualTo(1);                             // yalniz CONFIRMED
        assertThat(store.findById(ok.sagaId())).isEmpty();
        assertThat(store.findById(manual.sagaId())).isPresent();
        assertThat(store.countSagas()).isEqualTo(2);
        assertThat(store.countSteps()).isEqualTo(2);                          // CASCADE
    }
}
