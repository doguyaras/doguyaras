package com.acme.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.platform.messaging.readmodel.AccountStatusProjection;
import com.acme.platform.messaging.readmodel.AccountStatusProjection.AccountStatus;
import com.acme.platform.messaging.readmodel.AccountStatusProjection.Row;
import com.acme.platform.messaging.readmodel.BlockRelationProjection;
import com.acme.platform.messaging.readmodel.BlockRelationProjection.BlockOp;
import com.acme.platform.messaging.readmodel.BlockRelationProjection.Kind;
import com.acme.platform.messaging.readmodel.ConsumerPositionStore;
import com.acme.platform.messaging.readmodel.ConsumerPositionStore.Position;
import com.acme.platform.messaging.readmodel.DeltaEvent;
import com.acme.platform.messaging.readmodel.DeltaProjection;
import com.acme.platform.messaging.readmodel.GapDetectedException;
import com.acme.platform.messaging.readmodel.Outcome;
import com.acme.platform.messaging.readmodel.ReadModelApplier;
import com.acme.platform.messaging.readmodel.ReadModelFreshness;
import com.acme.platform.messaging.readmodel.SnapshotEvent;
import com.acme.platform.messaging.readmodel.StalenessGuard;
import com.acme.platform.messaging.readmodel.StalenessGuard.Decision;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DAVRANISSAL dogrulama (referans Bolum 4.6 "Dogrulama" satiri; kanit seviyesi 2): gercek PostgreSQL uzerinde
 * read-model tuketici sozlesmesi. Snapshot: kucuk/esit revizyon yok sayilir, sirasiz teslim ayni sonuca yakinsar.
 * Delta: bosluk uygulamayi durdurur (sayac + istisna, hicbir etki), tekrar tek etki. Tazelik konumdan olculur.
 * Rebuild deterministik. Inbox TX'i icinde cagri atomiktir.
 */
class ReadModelBehaviourIT {

    static final String SCHEMA = "order";
    static final String AUTH = "auth";
    static final String USER = "user";
    static EmbeddedPostgres pg;
    static DataSource ds;
    static NamedParameterJdbcTemplate jdbc;
    static TransactionTemplate tx;

    MutableClock clock;
    SimpleMeterRegistry meters;
    ConsumerPositionStore positions;
    ReadModelApplier applier;
    ReadModelFreshness freshness;
    StalenessGuard guard;
    AccountStatusProjection accounts;
    BlockRelationProjection blocks;

    /** Deterministik zaman: gecikme ve applied_at hesaplari gercek zaman beklemeden calisir. */
    static final class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2026-09-29T10:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    @BeforeAll
    static void startDb() throws Exception {
        pg = EmbeddedPostgres.builder().start();
        ds = pg.getPostgresDatabase();
        jdbc = new NamedParameterJdbcTemplate(ds);
        tx = new TransactionTemplate(new JdbcTransactionManager(ds));
        String ddl = new String(ReadModelBehaviourIT.class.getResourceAsStream("/db/platform/readmodel.sql")
                .readAllBytes(), StandardCharsets.UTF_8).replace("${schema}", "\"" + SCHEMA + "\"");
        jdbc.getJdbcTemplate().execute("CREATE SCHEMA \"" + SCHEMA + "\"");
        try (var conn = ds.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @AfterAll
    static void stopDb() throws IOException { if (pg != null) pg.close(); }

    @BeforeEach
    void clean() {
        jdbc.getJdbcTemplate().execute("TRUNCATE \"order\".rm_consumer_position, \"order\".rm_delta_position, "
                + "\"order\".rm_account_status, \"order\".rm_block_relation");
        clock = new MutableClock();
        meters = new SimpleMeterRegistry();
        positions = new ConsumerPositionStore(jdbc, SCHEMA);
        applier = new ReadModelApplier(tx, positions, clock, meters);
        freshness = new ReadModelFreshness(positions, clock, meters);
        guard = new StalenessGuard(freshness);
        accounts = new AccountStatusProjection(jdbc, SCHEMA);
        blocks = new BlockRelationProjection(jdbc, SCHEMA);
    }

    // ---------- yardimcilar ----------

    SnapshotEvent<AccountStatus> snapshot(long streamSeq, UUID account, boolean active, long revision) {
        Instant t = clock.instant().minusSeconds(60).plusSeconds(revision);   // kaynak zamani revizyonla artar
        return new SnapshotEvent<>(streamSeq, t, new AccountStatus(account, active, true, revision, t));
    }

    DeltaEvent<BlockOp> block(long streamSeq, UUID blocker, long seq, UUID blocked, Kind kind) {
        return new DeltaEvent<>(streamSeq, blocker, seq, clock.instant().minusSeconds(10), new BlockOp(blocked, kind));
    }

    Outcome applyAuth(SnapshotEvent<AccountStatus> e) { return applier.applySnapshot(AUTH, accounts, e); }

    Outcome applyUser(DeltaEvent<BlockOp> e) { return applier.applyDelta(USER, blocks, e); }

    Row row(UUID account) {
        return accounts.findAll().stream().filter(r -> r.accountId().equals(account)).findFirst().orElseThrow();
    }

    double gapCount(String source) {
        var c = meters.find(ReadModelApplier.GAP_METRIC).tag("source", source).counter();
        return c == null ? 0 : c.count();
    }

    int count(String table) {
        return jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM \"order\"." + table, Integer.class);
    }

    /** Sira kontrolunun projeksiyona DOKUNMADAN yapildigini gormek icin: apply cagri sayisini sayan sarmalayici. */
    static final class CountingDelta implements DeltaProjection<BlockOp> {
        final DeltaProjection<BlockOp> inner;
        final AtomicInteger applied = new AtomicInteger();
        CountingDelta(DeltaProjection<BlockOp> inner) { this.inner = inner; }
        @Override public void apply(DeltaEvent<BlockOp> e) { applied.incrementAndGet(); inner.apply(e); }
        @Override public void clear() { inner.clear(); }
    }

    // ---------- snapshot (tam durum) ----------

    @Test // kucuk revizyon yok sayilir: gec gelen eski karar yeni karari ezmez
    void snapshotOlderRevisionIsIgnored() {
        UUID a = UUID.randomUUID();
        assertThat(applyAuth(snapshot(1, a, false, 2))).isEqualTo(Outcome.APPLIED);   // rev 2: engellendi
        assertThat(applyAuth(snapshot(2, a, true, 1))).isEqualTo(Outcome.IGNORED);    // rev 1 gec geldi
        assertThat(row(a).active()).isFalse();
        assertThat(row(a).sourceRevision()).isEqualTo(2);
    }

    @Test // esit revizyon = tekrar teslim: tek etki, satir degismez (applied_at bile)
    void snapshotEqualRevisionIsIgnoredAsDuplicate() {
        UUID a = UUID.randomUUID();
        assertThat(applyAuth(snapshot(1, a, false, 5))).isEqualTo(Outcome.APPLIED);
        Row before = row(a);
        clock.advance(Duration.ofMinutes(1));
        assertThat(applyAuth(snapshot(1, a, true, 5))).isEqualTo(Outcome.IGNORED);
        assertThat(row(a)).isEqualTo(before);
    }

    @Test // yeni revizyon uygulanir; applied_at tuketicinin saati, source_time kaynagin
    void snapshotNewerRevisionIsApplied() {
        UUID a = UUID.randomUUID();
        assertThat(applyAuth(snapshot(1, a, true, 1))).isEqualTo(Outcome.APPLIED);
        clock.advance(Duration.ofMinutes(5));
        SnapshotEvent<AccountStatus> rev7 = snapshot(2, a, false, 7);
        assertThat(applyAuth(rev7)).isEqualTo(Outcome.APPLIED);
        Row r = row(a);
        assertThat(r.active()).isFalse();
        assertThat(r.sourceRevision()).isEqualTo(7);
        assertThat(r.sourceTime()).isEqualTo(rev7.state().sourceTime());
        assertThat(r.appliedAt()).isEqualTo(clock.instant());
        assertThat(count("rm_account_status")).isEqualTo(1);
    }

    @Test // kaynaklar arasi revizyon/konum karsilastirilmaz: auth rev 100 iken user seq 20 birbirine karismaz
    void perSourceRevisionsAndPositionsAreIndependent() {
        UUID account = UUID.randomUUID();   // ayni id hem auth'un hesabi hem user'in blocker'i
        assertThat(applyAuth(snapshot(100, account, true, 100))).isEqualTo(Outcome.APPLIED);
        // user kaynagi ayni aggregate icin kendi sirasindan (1'den) baslar: auth'un 100'u onu "tekrar" yapmaz
        assertThat(applyUser(block(20, account, 1, UUID.randomUUID(), Kind.CREATED))).isEqualTo(Outcome.APPLIED);
        Position auth = positions.find(AUTH).orElseThrow(), user = positions.find(USER).orElseThrow();
        assertThat(auth.lastSeq()).isEqualTo(100);
        assertThat(user.lastSeq()).isEqualTo(20);
        // auth'ta rev 101 gelince user'in konumu degismez; user'da seq 2 gelince auth'un konumu degismez
        assertThat(applyAuth(snapshot(101, account, false, 101))).isEqualTo(Outcome.APPLIED);
        assertThat(positions.find(USER).orElseThrow().lastSeq()).isEqualTo(20);
        assertThat(applyUser(block(21, account, 2, UUID.randomUUID(), Kind.CREATED))).isEqualTo(Outcome.APPLIED);
        assertThat(positions.find(AUTH).orElseThrow().lastSeq()).isEqualTo(101);
        assertThat(blocks.findByBlocker(account)).hasSize(2);
    }

    @Test // snapshot olaylari SIRASIZ gelse de son durum ayni: revizyon korumasi + GREATEST konum
    void shuffledSnapshotDeliveryConvergesToSameState() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        List<SnapshotEvent<AccountStatus>> ordered = new ArrayList<>();
        long seq = 0;
        for (long rev = 1; rev <= 4; rev++)
            for (UUID id : ids) ordered.add(snapshot(++seq, id, rev % 2 == 0, rev));
        for (SnapshotEvent<AccountStatus> e : ordered) applyAuth(e);
        List<Row> expected = accounts.findAll();
        Position expectedPos = positions.find(AUTH).orElseThrow();

        for (int round = 1; round <= 3; round++) {
            clean();
            List<SnapshotEvent<AccountStatus>> shuffled = new ArrayList<>(ordered);
            Collections.shuffle(shuffled, new Random(round));                  // tekrarlanabilir karistirma
            assertThat(shuffled).isNotEqualTo(ordered);
            for (SnapshotEvent<AccountStatus> e : shuffled) applyAuth(e);
            assertThat(accounts.findAll()).isEqualTo(expected);
            assertThat(accounts.findAll()).allSatisfy(r -> { assertThat(r.sourceRevision()).isEqualTo(4); assertThat(r.active()).isTrue(); });
            Position p = positions.find(AUTH).orElseThrow();
            assertThat(p.lastSeq()).isEqualTo(expectedPos.lastSeq());          // konum geri gitmez: max(seq)
            assertThat(p.lastEventTime()).isEqualTo(expectedPos.lastEventTime());
        }
    }

    // ---------- delta (degisiklik) ----------

    @Test // sirali delta uygulanir; ekleme/kaldirma sirasiyla; aggregate sirasi ve kaynak konumu ilerler
    void deltaInOrderIsApplied() {
        UUID blocker = UUID.randomUUID(), x = UUID.randomUUID(), y = UUID.randomUUID();
        assertThat(applyUser(block(1, blocker, 1, x, Kind.CREATED))).isEqualTo(Outcome.APPLIED);
        assertThat(applyUser(block(2, blocker, 2, y, Kind.CREATED))).isEqualTo(Outcome.APPLIED);
        assertThat(applyUser(block(3, blocker, 3, x, Kind.REMOVED))).isEqualTo(Outcome.APPLIED);
        assertThat(blocks.findByBlocker(blocker)).extracting(BlockRelationProjection.Row::blockedId).containsExactly(y);
        assertThat(positions.find(USER).orElseThrow().lastSeq()).isEqualTo(3);
        assertThat(gapCount(USER)).isZero();
    }

    @Test // bosluk (3'ten sonra 5): istisna, sayac +1, projeksiyon DOKUNULMAMIS, konum degismemis; 4 gelince devam
    void deltaGapStopsApplyingAndIsCounted() {
        UUID blocker = UUID.randomUUID(), z = UUID.randomUUID();
        for (long s = 1; s <= 3; s++) applyUser(block(s, blocker, s, UUID.randomUUID(), Kind.CREATED));
        Position before = positions.find(USER).orElseThrow();
        CountingDelta counting = new CountingDelta(blocks);

        assertThatThrownBy(() -> applier.applyDelta(USER, counting, block(5, blocker, 5, z, Kind.CREATED)))
                .isInstanceOf(GapDetectedException.class)
                .satisfies(ex -> {
                    GapDetectedException g = (GapDetectedException) ex;
                    assertThat(g.expectedSeq()).isEqualTo(4);
                    assertThat(g.actualSeq()).isEqualTo(5);
                    assertThat(g.source()).isEqualTo(USER);
                });
        assertThat(gapCount(USER)).isEqualTo(1.0);
        assertThat(counting.applied.get()).isZero();                          // sira kontrolu projeksiyondan ONCE
        assertThat(blocks.findByBlocker(blocker)).hasSize(3);
        assertThat(blocks.findByBlocker(blocker)).extracting(BlockRelationProjection.Row::blockedId).doesNotContain(z);
        assertThat(positions.find(USER).orElseThrow()).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT last_seq FROM \"order\".rm_delta_position WHERE source = :s AND aggregate_id = :a",
                Map.of("s", USER, "a", blocker), Long.class)).isEqualTo(3);

        // ilk olay da 1 olmak zorunda: yeni aggregate icin 2 ile baslamak bosluktur
        assertThatThrownBy(() -> applyUser(block(6, UUID.randomUUID(), 2, z, Kind.CREATED))).isInstanceOf(GapDetectedException.class);
        assertThat(gapCount(USER)).isEqualTo(2.0);

        // uzlastirma: eksik 4 gelir, sonra 5 uygulanir
        assertThat(applyUser(block(4, blocker, 4, UUID.randomUUID(), Kind.CREATED))).isEqualTo(Outcome.APPLIED);
        assertThat(applyUser(block(5, blocker, 5, z, Kind.CREATED))).isEqualTo(Outcome.APPLIED);
        assertThat(blocks.findByBlocker(blocker)).hasSize(5);
    }

    @Test // tekrar teslim (seq <= last) yok sayilir, tek etki; kaldirilmis iliski geri gelmez
    void deltaDuplicateIsIgnored() {
        UUID blocker = UUID.randomUUID(), x = UUID.randomUUID();
        assertThat(applyUser(block(1, blocker, 1, x, Kind.CREATED))).isEqualTo(Outcome.APPLIED);
        assertThat(applyUser(block(2, blocker, 2, x, Kind.REMOVED))).isEqualTo(Outcome.APPLIED);
        assertThat(applyUser(block(1, blocker, 1, x, Kind.CREATED))).isEqualTo(Outcome.IGNORED);   // eski olay tekrar
        assertThat(applyUser(block(2, blocker, 2, x, Kind.REMOVED))).isEqualTo(Outcome.IGNORED);   // son olay tekrar
        assertThat(blocks.findByBlocker(blocker)).isEmpty();
        assertThat(gapCount(USER)).isZero();
        assertThat(positions.find(USER).orElseThrow().lastSeq()).isEqualTo(2);
    }

    @Test // delta SIRALI olmak zorunda: snapshot'in aksine karisik teslim yakinsamaz, koruma durdurur
    void deltaEventsMustBeOrderedUnlikeSnapshots() {
        UUID blocker = UUID.randomUUID();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        DeltaEvent<BlockOp> e1 = block(1, blocker, 1, a, Kind.CREATED), e2 = block(2, blocker, 2, b, Kind.CREATED),
                e3 = block(3, blocker, 3, c, Kind.CREATED);
        List<DeltaEvent<BlockOp>> shuffled = List.of(e1, e3, e2);            // sabit permutasyon: 3, 2'den once gelir
        List<Outcome> outcomes = new ArrayList<>();
        int gaps = 0;
        for (DeltaEvent<BlockOp> e : shuffled) {
            try { outcomes.add(applyUser(e)); } catch (GapDetectedException ex) { gaps++; }
        }
        assertThat(gaps).isEqualTo(1);
        assertThat(outcomes).containsExactly(Outcome.APPLIED, Outcome.APPLIED);
        assertThat(blocks.findByBlocker(blocker)).extracting(BlockRelationProjection.Row::blockedId).containsExactlyInAnyOrder(a, b);
        assertThat(gapCount(USER)).isEqualTo(1.0);
        // uzlastirma: 3 yeniden teslim edilince tamamlanir
        assertThat(applyUser(e3)).isEqualTo(Outcome.APPLIED);
        assertThat(blocks.findByBlocker(blocker)).hasSize(3);
    }

    // ---------- tazelik ----------

    @Test // gecikme KONUMDAN olculur: satir 30 gun eski ama konum taze -> ALLOW; gauge konumu yansitir
    void lagIsMeasuredFromPositionNotFromRowAge() {
        UUID a = UUID.randomUUID();
        applyAuth(snapshot(1, a, true, 1));                                   // satir simdi yazildi
        clock.advance(Duration.ofDays(30));                                   // hesap 30 gundur degismedi
        jdbc.update("UPDATE \"order\".rm_consumer_position SET last_event_time = :t, updated_at = :t WHERE source = :s",
                Map.of("t", Timestamp.from(clock.instant().minusSeconds(5)), "s", AUTH));   // ama akis tuketiliyor
        freshness.registerGauge(AUTH);

        assertThat(row(a).appliedAt()).isBefore(clock.instant().minus(Duration.ofDays(29)));
        assertThat(freshness.lag(AUTH)).contains(Duration.ofSeconds(5));
        assertThat(meters.get(ReadModelFreshness.LAG_METRIC).tag("source", AUTH).gauge().value()).isEqualTo(5.0);
        assertThat(guard.decide(AUTH, Duration.ofSeconds(30), accounts.exists(a))).isEqualTo(Decision.ALLOW);
    }

    @Test // konum maxLag'i asarsa FAIL_CLOSED (satir taze olsa bile): karar kaynaga sorulur veya reddedilir
    void stalePositionFailsClosed() {
        UUID a = UUID.randomUUID();
        applyAuth(snapshot(1, a, true, 1));
        assertThat(freshness.lag(AUTH)).contains(Duration.ofSeconds(59));    // snapshot(): kaynak zamani now-60s+rev
        assertThat(guard.decide(AUTH, Duration.ofMinutes(2), true)).isEqualTo(Decision.ALLOW);
        clock.advance(Duration.ofMinutes(10));                                // tuketici 10 dk geride kaldi
        freshness.registerGauge(AUTH);
        assertThat(meters.get(ReadModelFreshness.LAG_METRIC).tag("source", AUTH).gauge().value()).isGreaterThan(600.0);
        assertThat(guard.decide(AUTH, Duration.ofSeconds(30), true)).isEqualTo(Decision.FAIL_CLOSED);
        assertThat(guard.decide(AUTH, Duration.ofMinutes(15), true)).isEqualTo(Decision.ALLOW);  // tolerans karara gore
    }

    @Test // satir yoksa varsayilan FAIL_CLOSED; hic tuketilmemis kaynak (konum yok) da FAIL_CLOSED ve gauge sonsuz
    void missingRowOrPositionFailsClosed() {
        applyAuth(snapshot(1, UUID.randomUUID(), true, 1));                   // konum taze
        assertThat(guard.decide(AUTH, Duration.ofMinutes(5), false)).isEqualTo(Decision.FAIL_CLOSED);
        freshness.registerGauge(USER);                                        // user hic tuketilmedi
        assertThat(freshness.lag(USER)).isEmpty();
        assertThat(meters.get(ReadModelFreshness.LAG_METRIC).tag("source", USER).gauge().value()).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(guard.decide(USER, Duration.ofMinutes(5), true)).isEqualTo(Decision.FAIL_CLOSED);
    }

    // ---------- rebuild ----------

    @Test // export'tan yeniden kurma deterministik: iki kez -> ayni satirlar; sira farki ve eski/silinmis satir sonucu degistirmez
    void rebuildFromExportIsDeterministic() {
        UUID stale = UUID.randomUUID();
        applyAuth(snapshot(1, stale, true, 9));                               // export'ta olmayan eski satir: silinmeli
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        List<SnapshotEvent<AccountStatus>> export = new ArrayList<>();
        long seq = 10;
        for (UUID id : ids) { export.add(snapshot(++seq, id, false, 3)); export.add(snapshot(++seq, id, true, 5)); }

        applier.rebuildFromExport(AUTH, accounts, export);
        List<Row> first = accounts.findAll();
        Position pos1 = positions.find(AUTH).orElseThrow();
        assertThat(first).hasSize(4);
        assertThat(first).allSatisfy(r -> { assertThat(r.sourceRevision()).isEqualTo(5); assertThat(r.active()).isTrue(); });
        assertThat(accounts.exists(stale)).isFalse();

        List<SnapshotEvent<AccountStatus>> reversed = new ArrayList<>(export);
        reversed.sort(Comparator.comparingLong(SnapshotEvent<AccountStatus>::streamSeq).reversed());
        applier.rebuildFromExport(AUTH, accounts, reversed);
        assertThat(accounts.findAll()).isEqualTo(first);
        assertThat(positions.find(AUTH).orElseThrow()).isEqualTo(pos1);
    }

    // ---------- atomiklik ----------

    @Test // inbox TX'i icinde: is sonra basarisiz olursa ne projeksiyon ne konum ne delta sirasi degisir
    void applyInsideFailingTransactionLeavesNothingBehind() {
        UUID account = UUID.randomUUID(), blocker = UUID.randomUUID();
        applyUser(block(1, blocker, 1, UUID.randomUUID(), Kind.CREATED));    // onceden var olan durum
        Position userBefore = positions.find(USER).orElseThrow();

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            assertThat(applyAuth(snapshot(1, account, true, 1))).isEqualTo(Outcome.APPLIED);
            assertThat(applyUser(block(2, blocker, 2, UUID.randomUUID(), Kind.CREATED))).isEqualTo(Outcome.APPLIED);
            assertThat(accounts.exists(account)).isTrue();                    // TX icinde gorunur
            throw new IllegalStateException("inbox isi ortasinda cokme");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(accounts.exists(account)).isFalse();
        assertThat(positions.find(AUTH)).isEmpty();
        assertThat(blocks.findByBlocker(blocker)).hasSize(1);
        assertThat(positions.find(USER).orElseThrow()).isEqualTo(userBefore);
        assertThat(jdbc.queryForObject("SELECT last_seq FROM \"order\".rm_delta_position WHERE source = :s AND aggregate_id = :a",
                Map.of("s", USER, "a", blocker), Long.class)).isEqualTo(1);
        // yeniden teslimde is yapilir
        assertThat(applyUser(block(2, blocker, 2, UUID.randomUUID(), Kind.CREATED))).isEqualTo(Outcome.APPLIED);
    }
}
