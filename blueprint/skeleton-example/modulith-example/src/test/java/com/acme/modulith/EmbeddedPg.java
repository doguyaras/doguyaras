package com.acme.modulith;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * JVM basina tek gercek PostgreSQL 18 sureci (zonky gomulu ikili; Docker gerekmez).
 * DDL: Modulith'in KENDI event_publication semasi (jar'daki v2 dosyasi, kopyasi degil) + modul semalari.
 */
public final class EmbeddedPg {

    private static final EmbeddedPostgres PG = start();

    private EmbeddedPg() {
    }

    public static String jdbcUrl() {
        return PG.getJdbcUrl("postgres", "postgres");
    }

    /** Spring'in havuzundan bagimsiz baglanti kaynagi: "baska bir oturumdan gorunuyor mu" sorulari icin. */
    public static DataSource independentDataSource() {
        return PG.getPostgresDatabase();
    }

    private static EmbeddedPostgres start() {
        try {
            EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
            try (Connection c = pg.getPostgresDatabase().getConnection()) {
                ScriptUtils.executeSqlScript(c, new ClassPathResource(
                        "org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql"));
                ScriptUtils.executeSqlScript(c, new ClassPathResource("db/modulith-example-schema.sql"));
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    pg.close();
                } catch (IOException ignored) {
                    // JVM kapanirken temizlik; hata raporlanacak yer yok
                }
            }));
            return pg;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
