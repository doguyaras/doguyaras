package com.acme.dbsecurity;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.BaselineResult;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * Flyway'i MIGRATION rolu ile programatik kosar (referans Bolum 10.2). Spring Boot'ta karsiligi
 * spring.flyway.user/password (uygulama datasource'undan ayri), schemas, clean-disabled=true.
 *
 * baseline-on-migrate BILEREK ACILMAZ: bu bayrak, migration'i bos olmayan ama Flyway yonetiminde olmayan bir
 * semaya (yanlis DB, yanlis sema) uygulamayi onleyen kontrolu kaldirir. Mevcut bir semayi yonetime alma
 * ayri ve tek seferlik bir islemdir: {@link #baselineOnce(String)}.
 */
public final class SchemaMigrator {

    private final FluentConfiguration config;

    public SchemaMigrator(String jdbcUrl, String migrateUser, String migratePassword, String schema, String... locations) {
        this.config = Flyway.configure()
                .dataSource(jdbcUrl, migrateUser, migratePassword)
                .schemas(schema)
                .locations(locations)
                .cleanDisabled(true)          // uretimde clean asla; test profili bile acmaz
                .baselineOnMigrate(false);    // varsayilan zaten false; niyet acikca yazilir
    }

    /** Bekleyen migration'lari uygular; bos olmayan yonetilmeyen semada FlywayException firlatir. */
    public MigrateResult migrate() {
        return config.load().migrate();
    }

    /**
     * Tek seferlik, belgelenmis prosedur: mevcut semadaki durumun hangi V surumune denk geldigi elle belirlenir,
     * history tablosu o surumle acilir; sonraki migrate() yalniz daha buyuk surumleri uygular.
     * Config'te surekli acik bir bayrak degil, operator kararidir.
     */
    public BaselineResult baselineOnce(String baselineVersion) {
        return Flyway.configure()
                .configuration(config)
                .baselineVersion(baselineVersion)
                .baselineDescription("mevcut sema Flyway yonetimine alindi")
                .load()
                .baseline();
    }
}
