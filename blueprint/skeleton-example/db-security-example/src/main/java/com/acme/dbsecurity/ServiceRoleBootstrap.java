package com.acme.dbsecurity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Altyapi migration'i (referans Bolum 10.1): servis basina iki rol (migration + uygulama), sema sahipligi,
 * default privilege ve rol bazli zaman asimlari. Superuser/DBA baglantisiyla BIR KEZ kosar; Flyway'in disindadir
 * cunku Flyway migration rolu ile calisir ve kendi rolunu yaratamaz. SQL kaynagi: db/infra/service_roles.sql.
 */
public final class ServiceRoleBootstrap {

    /** PostgreSQL kimligi olarak guvenle gomulebilecek adlar; tirnaksiz kullanildigi icin sikidir. */
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

    private ServiceRoleBootstrap() { }

    /**
     * Rol ve zaman asimi parametreleri. Varsayilanlar referans Bolum 10.1'deki baslangic degerleridir
     * (10s / 3s / 60s / migration lock 10s); testler daha kisa surelerle ayni script'i kosar.
     */
    public record Spec(String schema, String migrateRole, String migratePassword, String appRole, String appPassword,
                       Duration statementTimeout, Duration lockTimeout, Duration idleInTransactionTimeout,
                       Duration migrateLockTimeout) {

        public Spec {
            for (String id : new String[] {schema, migrateRole, appRole}) {
                if (id == null || !IDENTIFIER.matcher(id).matches()) {
                    throw new IllegalArgumentException("gecersiz kimlik: " + id);
                }
            }
            if (migrateRole.equals(appRole)) throw new IllegalArgumentException("migration ve uygulama rolu ayni olamaz");
        }

        /** Konvansiyon: sema "order" icin roller svc_order_migrate ve svc_order. */
        public static Spec defaults(String schema, String migratePassword, String appPassword) {
            return new Spec(schema, "svc_" + schema + "_migrate", migratePassword, "svc_" + schema, appPassword,
                    Duration.ofSeconds(10), Duration.ofSeconds(3), Duration.ofSeconds(60), Duration.ofSeconds(10));
        }

        public Spec withTimeouts(Duration statement, Duration lock, Duration idleInTransaction, Duration migrateLock) {
            return new Spec(schema, migrateRole, migratePassword, appRole, appPassword, statement, lock,
                    idleInTransaction, migrateLock);
        }
    }

    /** Script'i superuser baglantisiyla kosar. Idempotent DEGILDIR: rol zaten varsa hata verir (drift gizlenmez). */
    public static void apply(DataSource superuser, Spec spec) {
        String sql = render(spec);
        try (var conn = superuser.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
        } catch (SQLException e) {
            throw new IllegalStateException("rol bootstrap basarisiz", e);
        }
    }

    /** Placeholder'lari doldurur; sifreler tek tirnak icinde oldugu icin ' -> '' ile kacirilir. */
    static String render(Spec spec) {
        String template;
        try (var in = ServiceRoleBootstrap.class.getResourceAsStream("/db/infra/service_roles.sql")) {
            if (in == null) throw new IllegalStateException("db/infra/service_roles.sql bulunamadi");
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> values = Map.of(
                "schema", spec.schema(),
                "migrate_role", spec.migrateRole(),
                "migrate_password", spec.migratePassword().replace("'", "''"),
                "app_role", spec.appRole(),
                "app_password", spec.appPassword().replace("'", "''"),
                "statement_timeout", millis(spec.statementTimeout()),
                "lock_timeout", millis(spec.lockTimeout()),
                "idle_in_transaction_timeout", millis(spec.idleInTransactionTimeout()),
                "migrate_lock_timeout", millis(spec.migrateLockTimeout()));
        String sql = template;
        for (var e : values.entrySet()) sql = sql.replace("${" + e.getKey() + "}", e.getValue());
        if (sql.contains("${")) throw new IllegalStateException("doldurulmamis placeholder kaldi");
        return sql;
    }

    /** PostgreSQL sure literali: '2000ms' her surumde belirsizlik olmadan okunur. */
    private static String millis(Duration d) { return d.toMillis() + "ms"; }
}
