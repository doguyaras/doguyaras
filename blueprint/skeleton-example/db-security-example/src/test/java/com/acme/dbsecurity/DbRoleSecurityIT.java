package com.acme.dbsecurity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import javax.sql.DataSource;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * DAVRANISSAL dogrulama (referans Bolum 10.1, 10.2, 10.3, 10.5, 10.6; kanit seviyesi 2 ve 3): rol ayrimi, default
 * privilege, rol bazli zaman asimlari, RLS + SET LOCAL, append-only audit, UUIDv7, Flyway baseline proseduru ve
 * PgBouncer transaction mode - gercek PostgreSQL 18 (gomulu) ve gercek pgbouncer sureci uzerinde.
 * Metot yorumlarindaki #n gorev senaryo numarasidir.
 *
 * Zaman: buradaki sureler uygulama mantigi degil, sunucu tarafi ayarlardir (statement/lock/idle timeout); bu yuzden
 * enjekte edilen Clock yerine gercek zamanli, sinirli beklemeler kullanilir (awaitInfra).
 */
class DbRoleSecurityIT {

    static final String SCHEMA = "order";
    static final String DB = "postgres";
    static final String MIGRATE_PW = "migrate-s3cret";
    static final String APP_PW = "app-s3cret";
    static final UUID ACCOUNT_A = UUID.fromString("00000000-0000-7000-8000-00000000000a");
    static final UUID ACCOUNT_B = UUID.fromString("00000000-0000-7000-8000-00000000000b");
    static final String COUNT_ITEMS = "SELECT count(*) FROM \"order\".order_item";

    static EmbeddedPostgres pg;
    static ServiceRoleBootstrap.Spec spec;
    static String jdbcUrl;
    static DataSource superDs;
    static DataSource migrateDs;
    static DataSource appDs;
    static JdbcTemplate su;
    static JdbcTemplate migrate;
    static JdbcTemplate app;

    @BeforeAll
    static void startDbAndBootstrapRoles() throws Exception {
        pg = EmbeddedPostgres.builder().start();
        superDs = pg.getPostgresDatabase();
        su = new JdbcTemplate(superDs);
        int version = su.queryForObject("SHOW server_version_num", Integer.class);
        assertThat(version).as("uuidv7() ve testlerin hedefi PostgreSQL 18").isGreaterThanOrEqualTo(180000);
        jdbcUrl = "jdbc:postgresql://127.0.0.1:" + pg.getPort() + "/" + DB;

        // Altyapi migration'i (superuser, bir kez). Test icin kisa zaman asimlari; script ayni.
        spec = ServiceRoleBootstrap.Spec.defaults(SCHEMA, MIGRATE_PW, APP_PW)
                .withTimeouts(Duration.ofSeconds(2), Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(10));
        ServiceRoleBootstrap.apply(superDs, spec);
        requirePasswordAuthOverTcp();

        // Flyway migration rolu ile (uygulama datasource'undan ayri kimlik)
        MigrateResult result = new SchemaMigrator(jdbcUrl, spec.migrateRole(), MIGRATE_PW, SCHEMA, "classpath:db/migration").migrate();
        assertThat(result.migrationsExecuted).isEqualTo(3);

        migrateDs = dataSource(spec.migrateRole(), MIGRATE_PW);
        appDs = dataSource(spec.appRole(), APP_PW);
        migrate = new JdbcTemplate(migrateDs);
        app = new JdbcTemplate(appDs);
    }

    @AfterAll
    static void stopDb() throws IOException { if (pg != null) pg.close(); }

    @BeforeEach
    void clean() {
        // superuser TRUNCATE: satir trigger'lari calismaz; testler arasi temiz tablo
        su.execute("TRUNCATE \"order\".order_item, \"order\".audit_log, \"order\".order_number");
    }

    // ---------- kurulum yardimcilari ----------

    /**
     * Zonky initdb'yi -A trust ile kosar: sifre hic kontrol edilmez, rol ayrimi kanitlanmis olmaz. Uretimdeki gibi:
     * superuser haric TCP'de SCRAM. pg_hba yeniden yazilir ve pg_reload_conf ile yuklenir (asenkron SIGHUP).
     */
    static void requirePasswordAuthOverTcp() throws Exception {
        Path hba = Path.of(su.queryForObject("SHOW hba_file", String.class));
        Files.writeString(hba, """
                local   all   all                      trust
                host    all   postgres  127.0.0.1/32   trust
                host    all   postgres  ::1/128        trust
                host    all   all       127.0.0.1/32   scram-sha-256
                host    all   all       ::1/128        scram-sha-256
                """);
        su.queryForObject("SELECT pg_reload_conf()", Boolean.class);
        awaitInfra(Duration.ofSeconds(5), () -> "28P01".equals(sqlState(
                () -> dataSource(spec.appRole(), "yanlis-sifre").getConnection().close())));
    }

    static DataSource dataSource(String user, String password) {
        PGSimpleDataSource ds = new PGSimpleDataSource();     // havuzsuz: sonlandirilan baglanti testleri net kalsin
        ds.setUrl(jdbcUrl);
        ds.setUser(user);
        ds.setPassword(password);
        return ds;
    }

    interface SqlAction { void run() throws Exception; }

    /** Basarili ise null, aksi halde zincirdeki ilk SQLException'in SQLState'i. */
    static String sqlState(SqlAction action) {
        try {
            action.run();
            return null;
        } catch (Throwable t) {
            return rootSql(t).getSQLState();
        }
    }

    static SQLException rootSql(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) if (c instanceof SQLException se) return se;
        throw new AssertionError("SQLException beklendi", t);
    }

    /** Gercek zamanli altyapi icin sinirli bekleme; kosul saglanmazsa test basarisiz (sessiz gecis yok). */
    static void awaitInfra(Duration max, BooleanSupplier condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(max);
        while (!condition.getAsBoolean()) {
            if (Instant.now().isAfter(deadline)) throw new AssertionError("kosul " + max + " icinde saglanmadi");
            Thread.sleep(50);
        }
    }

    static UUID insertAsOwner(UUID account, String resource) {
        return UUID.fromString(migrate.queryForObject(
                "INSERT INTO \"order\".order_item (account_id, resource) VALUES (?, ?) RETURNING id::text",
                String.class, account, resource));
    }

    static void setLocalAccount(Connection conn, UUID account) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT set_config('app.account_id', ?, true)")) {
            ps.setString(1, account.toString());
            ps.executeQuery();
        }
    }

    static int count(Connection conn) throws SQLException {
        try (var st = conn.createStatement(); var rs = st.executeQuery(COUNT_ITEMS)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    static int count(JdbcTemplate jdbc) { return jdbc.queryForObject(COUNT_ITEMS, Integer.class); }

    // ---------- senaryolar ----------

    @Test // kurulum kaniti: sifre gercekten kontrol ediliyor (trust degil), dogru sifre ile giris var
    void passwordAuthIsEnforcedOverTcp() {
        assertThat(sqlState(() -> dataSource(spec.appRole(), "yanlis").getConnection().close())).isEqualTo("28P01");
        assertThat(sqlState(() -> dataSource(spec.migrateRole(), "yanlis").getConnection().close())).isEqualTo("28P01");
        assertThat(app.queryForObject("SELECT current_user", String.class)).isEqualTo(spec.appRole());
    }

    @Test // #1: uygulama rolu kendi satirlarinda DML yapar; sequence'te USAGE default privilege ile gelir
    void appRoleDmlWorksOnOwnRowsAndSequences() {
        AccountScope scope = new AccountScope(appDs);
        UUID id = UUID.fromString(scope.inAccount(ACCOUNT_A, j -> j.queryForObject(
                "INSERT INTO \"order\".order_item (account_id, resource) VALUES (?, ?) RETURNING id::text",
                String.class, ACCOUNT_A, "r1")));
        String resource = scope.inAccount(ACCOUNT_A, j -> j.queryForObject(
                "SELECT resource FROM \"order\".order_item WHERE id = ?", String.class, id));
        assertThat(resource).isEqualTo("r1");
        Integer updated = scope.inAccount(ACCOUNT_A, j -> j.update(
                "UPDATE \"order\".order_item SET resource = 'r2' WHERE id = ?", id));
        assertThat(updated).isEqualTo(1);
        Integer deleted = scope.inAccount(ACCOUNT_A, j -> j.update(
                "DELETE FROM \"order\".order_item WHERE id = ?", id));
        assertThat(deleted).isEqualTo(1);

        assertThat(app.queryForObject("SELECT nextval('\"order\".order_number_seq')", Long.class)).isPositive();
        app.update("INSERT INTO \"order\".order_number (order_id) VALUES (?)", UUID.randomUUID());
        assertThat(app.queryForObject("SELECT count(*) FROM \"order\".order_number", Integer.class)).isEqualTo(1);
    }

    @Test // #2: uygulama rolu DDL yapamaz; RLS/trigger/policy'yi kaldiramaz (42501)
    void appRoleCannotRunDdl() {
        assertThat(sqlState(() -> app.execute("CREATE TABLE \"order\".rogue (id INT)"))).isEqualTo("42501");
        assertThat(sqlState(() -> app.execute("CREATE TABLE public.rogue (id INT)"))).isEqualTo("42501");
        assertThat(sqlState(() -> app.execute("ALTER TABLE \"order\".order_item ADD COLUMN rogue INT"))).isEqualTo("42501");
        assertThat(sqlState(() -> app.execute("ALTER TABLE \"order\".order_item DISABLE ROW LEVEL SECURITY"))).isEqualTo("42501");
        assertThat(sqlState(() -> app.execute("DROP POLICY order_item_account_isolation ON \"order\".order_item"))).isEqualTo("42501");
        assertThat(sqlState(() -> app.execute("DROP TRIGGER trg_audit_log_append_only ON \"order\".audit_log"))).isEqualTo("42501");
        assertThat(sqlState(() -> app.execute("DROP TABLE \"order\".order_item"))).isEqualTo("42501");
        assertThat(sqlState(() -> app.execute("TRUNCATE \"order\".order_item"))).isEqualTo("42501");
        // GRANT sahibi olmayan rolde hata degil WARNING ("no privileges were granted") verir; kanit: etkisi yok
        app.execute("GRANT ALL ON \"order\".order_item TO PUBLIC");
        assertThat(su.queryForObject("SELECT count(*) FROM information_schema.role_table_grants "
                + "WHERE table_schema = 'order' AND table_name = 'order_item' AND grantee = 'PUBLIC'", Integer.class)).isZero();
    }

    @Test // #2b: public semasinda USAGE yok; tablo bazli GRANT verilse bile sema kapisi kapali (REVOKE ... FROM PUBLIC)
    void appRoleCannotUsePublicSchemaEvenWithTableGrant() {
        // PG15+ public'te CREATE'i zaten kapatir; USAGE ise varsayilan olarak PUBLIC'e aciktir. Bu test REVOKE'u kanitlar.
        su.execute("CREATE TABLE public.shared_probe (id INT)");
        try {
            su.execute("GRANT SELECT ON public.shared_probe TO " + spec.appRole());
            assertThat(sqlState(() -> app.queryForObject("SELECT count(*) FROM public.shared_probe", Integer.class)))
                    .isEqualTo("42501");
            assertThat(su.queryForObject("SELECT has_schema_privilege(?, 'public', 'USAGE')", Boolean.class,
                    spec.appRole())).isFalse();
        } finally {
            su.execute("DROP TABLE public.shared_probe");
        }
    }

    @Test // #2c: flyway_schema_history migration rolunun tablosu; default privilege'in verdigi DML afterMigrate ile geri alinir
    void appRoleCannotTamperWithFlywayHistory() {
        String history = "\"order\".flyway_schema_history";
        assertThat(sqlState(() -> app.queryForObject("SELECT count(*) FROM " + history, Integer.class))).isEqualTo("42501");
        assertThat(sqlState(() -> app.update("UPDATE " + history + " SET success = false"))).isEqualTo("42501");
        assertThat(sqlState(() -> app.update("DELETE FROM " + history + " WHERE version = '3'"))).isEqualTo("42501");
        assertThat(sqlState(() -> app.update("INSERT INTO " + history + " (installed_rank, version, description, type, "
                + "script, installed_by, execution_time, success) VALUES (99, '99', 'x', 'SQL', 'x', 'x', 0, true)")))
                .isEqualTo("42501");
        assertThat(appGrantsOn("flyway_schema_history")).isEmpty();
        assertThat(appGrantsOn("order_item")).containsExactlyInAnyOrder("SELECT", "INSERT", "UPDATE", "DELETE");

        // Elle yapilan drift: bir sonraki migrate() (bekleyen migration olmasa bile) yetkiyi yine geri alir
        su.execute("GRANT ALL ON " + history + " TO " + spec.appRole());
        assertThat(appGrantsOn("flyway_schema_history")).isNotEmpty();
        MigrateResult again = new SchemaMigrator(jdbcUrl, spec.migrateRole(), MIGRATE_PW, SCHEMA, "classpath:db/migration").migrate();
        assertThat(again.migrationsExecuted).isZero();
        assertThat(appGrantsOn("flyway_schema_history")).isEmpty();
        assertThat(su.queryForList("SELECT version FROM " + history + " ORDER BY installed_rank", String.class))
                .as("callback history'ye satir yazmaz").containsExactly("1", "2", "3");
    }

    static List<String> appGrantsOn(String table) {
        return su.queryForList("SELECT privilege_type FROM information_schema.role_table_grants WHERE table_schema = 'order' "
                + "AND table_name = ? AND grantee = ?", String.class, table, spec.appRole());
    }

    @Test // #3: audit_log append-only: uygulama INSERT eder, UPDATE/DELETE 42501; tablo sahibi bile trigger'a takilir
    void auditLogIsAppendOnly() {
        app.update("INSERT INTO \"order\".audit_log (actor, action) VALUES ('svc', 'ORDER_CREATED')");
        assertThat(sqlState(() -> app.update("UPDATE \"order\".audit_log SET action = 'x'"))).isEqualTo("42501");
        assertThat(sqlState(() -> app.update("DELETE FROM \"order\".audit_log"))).isEqualTo("42501");

        // ikinci hat: migration rolu tablo sahibi ve yetkili, ama trigger reddeder
        assertThatThrownBy(() -> migrate.update("UPDATE \"order\".audit_log SET action = 'x'"))
                .satisfies(t -> {
                    assertThat(rootSql(t).getSQLState()).isEqualTo("P0001");
                    assertThat(rootSql(t).getMessage()).contains("append-only: UPDATE rejected");
                });
        assertThat(sqlState(() -> migrate.update("DELETE FROM \"order\".audit_log"))).isEqualTo("P0001");
        assertThat(su.queryForObject("SELECT count(*) FROM \"order\".audit_log", Integer.class)).isEqualTo(1);
    }

    @Test // #4: statement_timeout rol ayari: kacak sorgu ~2 sn'de 57014 ile iptal; migration rolunde timeout yok
    void statementTimeoutCancelsRunawayQuery() {
        assertThat(app.queryForObject("SHOW statement_timeout", String.class)).isEqualTo("2s");
        long t0 = System.nanoTime();
        assertThat(sqlState(() -> app.execute("SELECT pg_sleep(4)"))).isEqualTo("57014");
        Duration elapsed = Duration.ofNanos(System.nanoTime() - t0);
        assertThat(elapsed).isBetween(Duration.ofMillis(1500), Duration.ofMillis(3500));
        assertThat(migrate.queryForObject("SHOW statement_timeout", String.class)).isEqualTo("0");
        assertThat(migrate.queryForObject("SHOW lock_timeout", String.class)).isEqualTo("10s");
    }

    @Test // #5: lock_timeout: baska TX'in kilitledigi satirda UPDATE ~1 sn'de 55P03 (kuyruk birikmez)
    void lockTimeoutFailsFastOnLockedRow() throws Exception {
        UUID id = insertAsOwner(ACCOUNT_A, "locked");
        try (Connection holder = migrateDs.getConnection()) {
            holder.setAutoCommit(false);
            try (var ps = holder.prepareStatement("SELECT id FROM \"order\".order_item WHERE id = ? FOR UPDATE")) {
                ps.setObject(1, id);
                ps.executeQuery();
            }
            try (Connection appConn = appDs.getConnection()) {
                appConn.setAutoCommit(false);
                setLocalAccount(appConn, ACCOUNT_A);
                long t0 = System.nanoTime();
                String state = sqlState(() -> {
                    try (var ps = appConn.prepareStatement("UPDATE \"order\".order_item SET resource = 'x' WHERE id = ?")) {
                        ps.setObject(1, id);
                        ps.setQueryTimeout(5);   // lock_timeout yoksa sonsuza kadar bekler: test asmasin, 57014 ile FAIL etsin
                        ps.executeUpdate();
                    }
                });
                Duration elapsed = Duration.ofNanos(System.nanoTime() - t0);
                assertThat(state).as("lock_timeout rol ayari 55P03 vermeli (57014 = surucu iptali, ayar yok)").isEqualTo("55P03");
                assertThat(elapsed).isBetween(Duration.ofMillis(800), Duration.ofMillis(2500));
                appConn.rollback();
            }
            holder.rollback();
        }
    }

    @Test // #6: idle_in_transaction_session_timeout: acik unutulan TX ~2 sn sonra sunucu tarafindan sonlandirilir
    void idleInTransactionSessionIsTerminated() throws Exception {
        Connection conn = appDs.getConnection();
        try {
            conn.setAutoCommit(false);
            int pid;
            try (var st = conn.createStatement(); var rs = st.executeQuery("SELECT pg_backend_pid()")) {
                rs.next();
                pid = rs.getInt(1);
            }
            long t0 = System.nanoTime();
            // Uygulama baglantisina dokunulmaz (her ifade sayaci sifirlar); backend'in kaybolmasi disaridan izlenir.
            awaitInfra(Duration.ofSeconds(8), () -> su.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity WHERE pid = ?", Integer.class, pid) == 0);
            Duration elapsed = Duration.ofNanos(System.nanoTime() - t0);
            assertThat(elapsed).as("2 sn'den once sonlanmamali").isGreaterThanOrEqualTo(Duration.ofMillis(1500));

            String state = sqlState(() -> { try (var st = conn.createStatement()) { st.execute("SELECT 1"); } });
            assertThat(state).isIn("25P03", "57P01", "08006", "08003");
        } finally {
            try { conn.close(); } catch (SQLException alreadyDead) { /* sunucu kapatti */ }
        }
    }

    @Test // #7: RLS hesaplari ayirir; baglam SET LOCAL ile; duz SET oturuma sizar; uygulama rolu BYPASSRLS degil
    void rowLevelSecurityIsolatesAccountsAndPlainSetLeaks() throws Exception {
        insertAsOwner(ACCOUNT_A, "a1");
        insertAsOwner(ACCOUNT_A, "a2");
        insertAsOwner(ACCOUNT_B, "b1");
        assertThat(count(migrate)).as("tablo sahibi (migration/backfill) RLS'i atlar").isEqualTo(3);

        AccountScope scope = new AccountScope(appDs);
        Integer seenByA = scope.inAccount(ACCOUNT_A, j -> count(j));
        Integer seenByB = scope.inAccount(ACCOUNT_B, j -> count(j));
        assertThat(seenByA).isEqualTo(2);
        assertThat(seenByB).isEqualTo(1);
        assertThat(count(app)).as("baglamsiz = kapali").isZero();
        // Havuz simulasyonu (tek baglanti tekrar tekrar verilir): AccountScope'un baglami TX ile bitmeli, oturumda kalmamali.
        // Havuzsuz DataSource'ta SET ile SET LOCAL ayirt edilemez; bu kontrol M5 mutasyonunu (is_local=false) yakalar.
        SingleConnectionDataSource reused = new SingleConnectionDataSource(jdbcUrl, spec.appRole(), APP_PW, true);
        try {
            Integer inScope = new AccountScope(reused).inAccount(ACCOUNT_A, j -> count(j));
            assertThat(inScope).isEqualTo(2);
            assertThat(count(new JdbcTemplate(reused))).as("havuzdan geri gelen baglantida baglam kalmadi").isZero();
        } finally {
            reused.destroy();
        }
        // USING ifadesi WITH CHECK olarak da uygulanir: A baglaminda B'ye satir yazilamaz
        assertThat(sqlState(() -> scope.inAccount(ACCOUNT_A, j -> j.update(
                "INSERT INTO \"order\".order_item (account_id, resource) VALUES (?, 'x')", ACCOUNT_B)))).isEqualTo("42501");

        try (Connection conn = appDs.getConnection()) {
            conn.setAutoCommit(false);
            // Dogru: SET LOCAL TX ile biter; ayni baglantidaki sonraki TX hicbir sey gormez
            setLocalAccount(conn, ACCOUNT_A);
            assertThat(count(conn)).isEqualTo(2);
            conn.commit();
            assertThat(count(conn)).isZero();
            // TX bitince deger NULL degil '' olur: policy'deki NULLIF bu yuzden var
            try (var st = conn.createStatement(); var rs = st.executeQuery("SELECT current_setting('app.account_id', true)")) {
                rs.next();
                assertThat(rs.getString(1)).isEmpty();
            }
            conn.commit();

            // SIZINTI: duz SET oturuma yazar; commit sonrasi ayni baglantidaki her sonraki TX A'yi gorur.
            // Havuzda bu baglanti baska istege gider -> baska hesabin verisi. Bu yuzden SET LOCAL zorunlu.
            try (var st = conn.createStatement()) { st.execute("SET app.account_id = '" + ACCOUNT_A + "'"); }
            conn.commit();
            assertThat(count(conn)).isEqualTo(2);
            conn.commit();
            assertThat(count(conn)).as("sizinti: sonraki TX de goruyor").isEqualTo(2);
            conn.commit();
            try (var st = conn.createStatement()) { st.execute("RESET app.account_id"); }
            conn.commit();
            assertThat(count(conn)).isZero();
            conn.commit();
        }

        Map<String, Object> flags = su.queryForMap(
                "SELECT rolbypassrls, rolsuper FROM pg_roles WHERE rolname = ?", spec.appRole());
        assertThat(flags).containsEntry("rolbypassrls", false).containsEntry("rolsuper", false);
    }

    @Test // #8: uuidv7(): surum 7; ayni oturumda 100 uretim kesin artan (uuid_extract_timestamp azalmaz, metin sirasi artar)
    void uuidv7IdsAreVersion7AndMonotonicWithinSession() throws Exception {
        List<String> ids = new ArrayList<>();
        try (Connection conn = appDs.getConnection(); var st = conn.createStatement()) {
            for (int i = 0; i < 100; i++) {
                try (var rs = st.executeQuery("SELECT uuidv7()::text")) {
                    rs.next();
                    ids.add(rs.getString(1));
                }
            }
        }
        long previousMs = Long.MIN_VALUE;
        for (int i = 0; i < ids.size(); i++) {
            Map<String, Object> row = su.queryForMap("SELECT uuid_extract_version(?::uuid) AS v, "
                    + "(extract(epoch FROM uuid_extract_timestamp(?::uuid)) * 1000)::bigint AS ts_ms", ids.get(i), ids.get(i));
            assertThat(((Number) row.get("v")).intValue()).isEqualTo(7);
            long ms = ((Number) row.get("ts_ms")).longValue();
            assertThat(ms).as("v7 zaman damgasi azalmaz").isGreaterThanOrEqualTo(previousMs);
            assertThat(Math.abs(ms - System.currentTimeMillis())).as("gercek saat").isLessThan(60_000);
            if (i > 0) assertThat(ids.get(i).compareTo(ids.get(i - 1))).as("id %d > id %d", i, i - 1).isPositive();
            previousMs = ms;
        }
        // Tablo default'u ile: id sirasi = ekleme sirasi = created_at sirasi (keyset created_at DESC, id DESC uyumu)
        List<UUID> inserted = new ArrayList<>();
        for (int i = 0; i < 20; i++) inserted.add(insertAsOwner(ACCOUNT_A, "r" + i));
        List<String> byId = migrate.queryForList("SELECT id::text FROM \"order\".order_item ORDER BY id", String.class);
        List<String> byTime = migrate.queryForList("SELECT id::text FROM \"order\".order_item ORDER BY created_at, id", String.class);
        assertThat(byId).containsExactlyElementsOf(inserted.stream().map(UUID::toString).toList());
        assertThat(byTime).isEqualTo(byId);
    }

    @Test // #9: Flyway guvenlik agi: bos olmayan yonetilmeyen sema baseline-on-migrate=false ile reddedilir; tek seferlik baseline sonra gecer
    void flywayRefusesNonEmptyUnmanagedSchemaUntilExplicitBaseline() {
        su.execute("CREATE SCHEMA legacy AUTHORIZATION " + spec.migrateRole());
        migrate.execute("CREATE TABLE legacy.legacy_thing (id INT PRIMARY KEY)");   // "mevcut" sistem: V1'e denk
        SchemaMigrator legacy = new SchemaMigrator(jdbcUrl, spec.migrateRole(), MIGRATE_PW, "legacy", "classpath:db/legacy-migration");

        assertThatThrownBy(legacy::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("non-empty")
                .hasMessageContaining("baseline");
        assertThat(historyTableExists()).isFalse();
        assertThat(noteColumnExists()).isFalse();

        legacy.baselineOnce("1");
        MigrateResult result = legacy.migrate();
        assertThat(result.migrationsExecuted).as("yalniz V2 uygulanir; V1 baseline altinda").isEqualTo(1);
        assertThat(noteColumnExists()).isTrue();
        List<Map<String, Object>> history = su.queryForList(
                "SELECT version, type, success FROM legacy.flyway_schema_history ORDER BY installed_rank");
        assertThat(history).extracting(r -> r.get("version") + ":" + r.get("type") + ":" + r.get("success"))
                .containsExactly("1:BASELINE:true", "2:SQL:true");
    }

    static boolean historyTableExists() {
        return su.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'legacy' "
                + "AND table_name = 'flyway_schema_history'", Integer.class) == 1;
    }

    static boolean noteColumnExists() {
        return su.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema = 'legacy' "
                + "AND table_name = 'legacy_thing' AND column_name = 'note'", Integer.class) == 1;
    }

    // ---------- PgBouncer (seviye 3: JDBC -> pgbouncer sureci -> PostgreSQL) ----------

    @Test // #10a: transaction mode + max_prepared_statements=200: sunucu tarafi prepared statement'lar iki istemcide 60 TX boyunca sorunsuz
    void pgBouncerTracksServerSidePreparedStatementsInTransactionMode() throws Exception {
        try (PgBouncerProcess pgb = PgBouncerProcess.start(pg.getPort(), DB, spec.appRole(), APP_PW,
                new PgBouncerProcess.Config(200, 5))) {
            List<String> anomalies = runAlternatingPreparedTransactions(pgb, 30);
            assertThat(anomalies).as("pgbouncer log:\n" + pgb.log()).isEmpty();
        }
    }

    @Test // #10b: max_prepared_statements=0 (1.24.1 oncesinde VARSAYILAN; buradaki pgbouncer 1.22): ayni yuk bozulur
    void pgBouncerWithoutPreparedStatementTrackingBreaksNamedStatements() throws Exception {
        try (PgBouncerProcess pgb = PgBouncerProcess.start(pg.getPort(), DB, spec.appRole(), APP_PW,
                new PgBouncerProcess.Config(0, 5))) {
            List<String> anomalies = runAlternatingPreparedTransactions(pgb, 30);
            // Gozlem (1.22.0): once 42P05 "S_1 already exists", sonra 26000 ve 08P01 bind uyusmazligi; ayrinti aciklamada
            assertThat(anomalies).as("izleme kapaliyken hata beklenir; log:\n" + pgb.log()).isNotEmpty();
            assertThat(anomalies.getFirst()).as("max_prepared_statements=0 gozlemi: %s", anomalies)
                    .containsAnyOf("42P05", "26000", "TAG-MISMATCH");
        }
    }

    @Test // #10c: PgBouncer arkasinda SET LOCAL sonraki TX'e sizmaz; duz SET ise BASKA ISTEMCIYE sizar (tek server baglantisi)
    void pgBouncerSetLocalIsTransactionScopedButPlainSetLeaksAcrossClients() throws Exception {
        insertAsOwner(ACCOUNT_A, "a1");
        insertAsOwner(ACCOUNT_A, "a2");
        try (PgBouncerProcess pgb = PgBouncerProcess.start(pg.getPort(), DB, spec.appRole(), APP_PW,
                new PgBouncerProcess.Config(200, 1));
             Connection c1 = DriverManager.getConnection(pgb.jdbcUrl(DB), spec.appRole(), APP_PW);
             Connection c2 = DriverManager.getConnection(pgb.jdbcUrl(DB), spec.appRole(), APP_PW)) {
            c1.setAutoCommit(false);
            setLocalAccount(c1, ACCOUNT_A);
            assertThat(count(c1)).isEqualTo(2);
            c1.commit();
            assertThat(count(c1)).as("SET LOCAL sonraki TX'e sizmadi").isZero();
            c1.commit();
            assertThat(count(c2)).as("diger istemci de temiz").isZero();

            // Duz SET: c1'in oturum ayari pgbouncer'in tek server baglantisina yazilir; c2 ayni server baglantisini alir.
            try (var st = c1.createStatement()) { st.execute("SET app.account_id = '" + ACCOUNT_A + "'"); }
            c1.commit();
            assertThat(count(c2)).as("baska istemci A'nin verisini gordu (transaction mode + SET)").isEqualTo(2);
        }
    }

    /**
     * Iki istemci, FARKLI SQL, ayni surucu statement adi (S_1). pgbouncer prepared statement'lari izlemezse:
     * "already exists" (42P05), "does not exist" (26000) veya en tehlikelisi: baska istemcinin plani ile sessizce
     * yanlis sonuc (TAG-MISMATCH). Basarili durumda liste bos doner.
     */
    static List<String> runAlternatingPreparedTransactions(PgBouncerProcess pgb, int rounds) throws SQLException {
        String url = pgb.jdbcUrl(DB) + "?prepareThreshold=1";     // ilk calistirmadan itibaren sunucu tarafi prepared
        List<String> anomalies = new ArrayList<>();
        try (Connection a = DriverManager.getConnection(url, spec.appRole(), APP_PW);
             Connection b = DriverManager.getConnection(url, spec.appRole(), APP_PW)) {
            a.setAutoCommit(false);
            b.setAutoCommit(false);
            for (int round = 0; round < rounds; round++) {
                preparedTx(a, "A", round, anomalies);
                preparedTx(b, "B", round, anomalies);
            }
        }
        return anomalies;
    }

    static void preparedTx(Connection conn, String tag, int round, List<String> anomalies) {
        String sql = "SELECT '" + tag + "' AS tag, count(*) FROM \"order\".order_item WHERE account_id = ?";
        try {
            try (var ps = conn.prepareStatement(sql)) {
                ps.setObject(1, ACCOUNT_A);
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    if (!tag.equals(rs.getString(1))) {
                        anomalies.add("round " + round + " " + tag + " TAG-MISMATCH got " + rs.getString(1));
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            anomalies.add("round " + round + " " + tag + " " + e.getSQLState() + " " + e.getMessage());
            try { conn.rollback(); } catch (SQLException ignored) { /* baglanti zaten bozuk olabilir */ }
        }
    }
}
