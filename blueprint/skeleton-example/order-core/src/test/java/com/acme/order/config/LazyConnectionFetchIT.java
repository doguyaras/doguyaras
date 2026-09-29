package com.acme.order.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Boot 4.1 spring.datasource.connection-fetch=lazy kaniti (referans Bolum 2.1) - gercek PostgreSQL 18 (gomulu),
 * kanit seviyesi 2: auto-configured DataSource LazyConnectionDataSourceProxy ile sarilir; getConnection() ve
 * transaction ayarlari (setAutoCommit vb.) havuzdan baglanti ALMAZ; fiziksel baglanti ilk Statement'ta alinir.
 * Karsilastirma ayni havuz uzerinden: sarilmamis Hikari getConnection() aninda aktif sayaci artirir.
 */
// Property test'te VERILMEZ: uretim application.yml'deki spring.datasource.connection-fetch=lazy kanitlanir
// (yml'den silinirse bu test kirilir).
@SpringBootTest(classes = LazyConnectionFetchIT.Config.class)
class LazyConnectionFetchIT {

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(DataSourceAutoConfiguration.class)
    static class Config {}

    static EmbeddedPostgres pg;

    @DynamicPropertySource
    static void embeddedPostgres(DynamicPropertyRegistry registry) throws IOException {
        pg = EmbeddedPostgres.builder().start();
        registry.add("spring.datasource.url", () -> pg.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void stopDb() throws IOException { if (pg != null) pg.close(); }

    @Autowired DataSource dataSource;

    @Test
    void dataSourceBeanIsLazyProxyAroundHikari() throws Exception {
        assertThat(dataSource).isInstanceOf(LazyConnectionDataSourceProxy.class);
        assertThat(((LazyConnectionDataSourceProxy) dataSource).getTargetDataSource()).isInstanceOf(HikariDataSource.class);
        assertThat(dataSource.unwrap(HikariDataSource.class)).isNotNull();
    }

    @Test
    void getConnectionAndTransactionSetup_doNotBorrowFromPool_untilFirstStatement() throws Exception {
        HikariDataSource hikari = dataSource.unwrap(HikariDataSource.class);

        try (Connection lazy = dataSource.getConnection()) {
            // Proxy dondu ama havuzdan odunc alinmis baglanti yok (havuz henuz baslamamis olabilir: pool == null).
            assertThat(activeConnections(hikari)).as("getConnection() sonrasi aktif").isZero();
            lazy.setAutoCommit(false);
            lazy.setReadOnly(true);
            assertThat(lazy.getAutoCommit()).isFalse();
            assertThat(activeConnections(hikari)).as("transaction ayarlari sonrasi aktif").isZero();

            try (Statement st = lazy.createStatement(); ResultSet rs = st.executeQuery("SELECT version()")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).startsWith("PostgreSQL 18");
                assertThat(activeConnections(hikari)).as("ilk statement sonrasi aktif").isEqualTo(1);
            }
            assertThat(lazy.getAutoCommit()).isFalse(); // ayarlar fiziksel baglantiya uygulandi
            lazy.rollback();
        }
        assertThat(activeConnections(hikari)).as("close sonrasi aktif").isZero();
    }

    @Test
    void unwrappedHikari_isEager_forContrast() throws Exception {
        HikariDataSource hikari = dataSource.unwrap(HikariDataSource.class);
        try (Connection eager = hikari.getConnection()) {
            assertThat(eager.isValid(1)).isTrue();
            assertThat(activeConnections(hikari)).as("Hikari getConnection() aninda aktif").isEqualTo(1);
        }
        assertThat(activeConnections(hikari)).isZero();
    }

    /** Havuz ilk fiziksel baglantida baslar; baslamamissa odunc alinan baglanti da yoktur. */
    static int activeConnections(HikariDataSource hikari) {
        HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
        return pool == null ? 0 : pool.getActiveConnections();
    }
}
