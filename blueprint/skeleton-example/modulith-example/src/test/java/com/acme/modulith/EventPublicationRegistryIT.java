package com.acme.modulith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import com.acme.modulith.inventory.InventoryService;
import com.acme.modulith.order.OrderPlaced;
import com.acme.modulith.order.OrderService;

/**
 * Seviye 2 (gercek PostgreSQL 18): Modulith event publication registry = in-process outbox (Bolum 1.1, 11.2).
 * Dinleyici gercekten asenkron (@EnableAsync) calisir; beklemeler, gercek arka plan thread'inin DB'ye
 * yazdigi sonucu sinirli sure yoklar (Awaitility), uygulama mantigini sleep ile "beklemez".
 */
@SpringBootTest(classes = {ModulithApp.class, EventPublicationRegistryIT.ClockConfig.class})
class EventPublicationRegistryIT {

    private static final Duration WAIT = Duration.ofSeconds(20);
    private static final Instant T0 = Instant.parse("2026-03-01T09:00:00Z");

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(T0);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", EmbeddedPg::jdbcUrl);
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "");
    }

    @Autowired OrderService orders;
    @Autowired InventoryService inventory;
    @Autowired IncompleteEventPublications incomplete;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired MutableClock clock;

    private final DataSource otherSession = EmbeddedPg.independentDataSource();

    @Test
    void runsAgainstRealPostgres18WithModulithsOwnSchema() {
        assertThat(jdbc.queryForObject("SELECT current_setting('server_version_num')::int", Integer.class))
                .isBetween(180000, 189999);
        assertThat(jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'event_publication'
                """, String.class))
                .contains("status", "completion_attempts", "last_resubmission_date"); // v2 semasi
    }

    // --- 3a: siparis satiri ve publication satiri AYNI transaction'da -------------------------------------

    @Test
    void orderRowAndPublicationRowAreWrittenInTheSameTransaction() throws Exception {
        String sku = sku();
        inventory.provision(sku, 10);
        AtomicReference<UUID> id = new AtomicReference<>();

        tx.executeWithoutResult(status -> {
            id.set(orders.placeOrder(sku, 2));
            // Transaction'in kendi baglantisi: ikisi de yazilmis
            assertThat(countOrdersInTx(id.get())).isEqualTo(1);
            assertThat(countPublicationsInTx(id.get())).isEqualTo(1);
            // Bagimsiz bir oturum: commit oncesi IKISI DE gorunmez (ayri commit olsalardi biri gorunurdu)
            assertThat(countOrders(otherSession, id.get())).isZero();
            assertThat(countPublications(otherSession, id.get())).isZero();
        });

        // Commit sonrasi ikisi birlikte gorunur
        assertThat(countOrders(otherSession, id.get())).isEqualTo(1);
        assertThat(countPublications(otherSession, id.get())).isEqualTo(1);
        awaitStatus(id.get(), "COMPLETED");
        assertThat(inventory.availableStock(sku)).hasValue(8);
    }

    // --- 3b: dinleyici bloke iken publication kayitli ve tamamlanmamis; bitince COMPLETED -----------------

    @Test
    void publicationIsPersistedWhileListenerIsBlockedAndCompletedAfterwards() throws Exception {
        String sku = sku();
        inventory.provision(sku, 10);

        UUID id;
        // Dinleyiciyi gercek bir satir kilidiyle durdur: stok satirini baska oturumda FOR UPDATE tut
        try (Connection blocker = otherSession.getConnection()) {
            blocker.setAutoCommit(false);
            try (PreparedStatement ps = blocker.prepareStatement("SELECT available FROM inventory.stock WHERE sku = ? FOR UPDATE")) {
                ps.setString(1, sku);
                ps.executeQuery().close();
            }

            id = orders.placeOrder(sku, 3);

            // Dinleyici gercekten basladi ve kilitte bekliyor (henuz baslamamis olmasiyla karistirilmasin)
            await().atMost(WAIT).until(() -> listenerWaitingOnLock());

            Map<String, Object> pub = publication(id);
            assertThat(orders.exists(id)).isTrue();
            assertThat(pub.get("completion_date")).isNull();
            assertThat(pub.get("status")).isEqualTo("PROCESSING");
            assertThat((String) pub.get("listener_id")).contains("OrderPlacedListener");
            assertThat(((Timestamp) pub.get("publication_date")).toInstant()).isEqualTo(clock.instant());
            assertThat(inventory.availableStock(sku)).hasValue(10);

            blocker.rollback(); // kilidi birak
        }

        awaitStatus(id, "COMPLETED");
        Map<String, Object> done = publication(id);
        assertThat(done.get("completion_date")).isNotNull();
        assertThat(((Timestamp) done.get("completion_date")).toInstant()).isEqualTo(clock.instant());
        assertThat(inventory.availableStock(sku)).hasValue(7);
        assertThat(inventory.reservationCount(id)).isEqualTo(1);
    }

    // --- 3c: dinleyici firlatir -> tamamlanmamis kalir; resubmit (yas esigi Clock ile) -> tamamlanir ---------

    @Test
    void failingListenerLeavesPublicationIncompleteAndResubmissionCompletesIt() {
        String sku = sku(); // stok TANIMSIZ: dinleyici StockNotProvisionedException firlatir
        UUID id = orders.placeOrder(sku, 4);

        awaitStatus(id, "FAILED");
        Map<String, Object> failed = publication(id);
        assertThat(failed.get("completion_date")).isNull();
        assertThat(inventory.reservationCount(id)).isZero(); // dedup satiri da geri alindi

        inventory.provision(sku, 10); // eksik stok sonradan tanimlandi

        // Yas esigi registry'nin Clock'u ile hesaplanir: 1 dk sonra "5 dk'dan eski" kaydi SECMEZ.
        // (Diger testler bitmeden once kendi publication'larini COMPLETED'e getirir; filtresiz cagri guvenli.)
        clock.advance(Duration.ofMinutes(1));
        incomplete.resubmitIncompletePublicationsOlderThan(Duration.ofMinutes(5));
        assertThat(publication(id).get("status")).isEqualTo("FAILED");
        assertThat(inventory.availableStock(sku)).hasValue(10);

        // 6 dk sonra secer ve yeniden teslim eder
        clock.advance(Duration.ofMinutes(5));
        incomplete.resubmitIncompletePublicationsOlderThan(Duration.ofMinutes(5));

        awaitStatus(id, "COMPLETED");
        assertThat(inventory.availableStock(sku)).hasValue(6);
        assertThat(inventory.reservationCount(id)).isEqualTo(1);
    }

    // --- 3c': KUTUPHANE KUSURU (Modulith 2.1.1 JDBC) sabitlenir: ResubmissionOptions.withMinAge FAILED'da etkisiz ---

    /**
     * {@code resubmitIncompletePublications(ResubmissionOptions)} yalniz FAILED kayitlari okur ve SQL'i
     * {@code WHERE STATUS = 'FAILED' OR (STATUS IS NULL AND COMPLETION_DATE IS NULL) AND PUBLICATION_DATE < ?}
     * olarak kurar: AND, OR'dan once baglar, yas esigi FAILED dalina UYGULANMAZ. Sonuc: 1 dakikalik FAILED
     * kayit "en az 5 dk" secenegine ragmen hemen yeniden teslim edilir (backoff yok). Bu test kusuru sabitler;
     * surum yukseltmesinde duzelirse kirilir ve referans notu guncellenir. Yasa gore geri alma icin
     * {@code resubmitIncompletePublicationsOlderThan(Duration)} kullanilir (dogru parantezli sorgu, test 3c).
     */
    @Test
    void knownDefect_minAgeIsIgnoredForFailedPublicationsInModulith211Jdbc() {
        String sku = sku();
        UUID id = orders.placeOrder(sku, 1);
        awaitStatus(id, "FAILED");
        Instant publishedAt = ((Timestamp) publication(id).get("publication_date")).toInstant();
        inventory.provision(sku, 5);

        clock.advance(Duration.ofMinutes(1));
        assertThat(Duration.between(publishedAt, clock.instant())).isLessThan(Duration.ofMinutes(5));
        incomplete.resubmitIncompletePublications(ResubmissionOptions.defaults()
                .withMinAge(Duration.ofMinutes(5)).withFilter(forOrder(id)));

        // Beklenen (dokumante edilen) davranis: secilmemeli. Gercek: yeniden teslim edilip tamamlaniyor.
        awaitStatus(id, "COMPLETED");
        assertThat(inventory.availableStock(sku)).hasValue(4);
    }

    // --- 3d: etki commit oldu ama "tamamlandi" yazilamadi -> yeniden teslim cift rezervasyon YAPMAZ --------

    @Test
    void redeliveryAfterLostCompletionDoesNotDoubleReserve() {
        String sku = sku();
        inventory.provision(sku, 10);
        // Surec cokusu simulasyonu: dinleyicinin transaction'i commit olduktan SONRA gelen "tamamlandi"
        // yazimini DB'de sessizce yut (Modulith bunu ayri yazimla yapar; arada cokus = ayni durum).
        jdbc.execute("CREATE TABLE IF NOT EXISTS lost_completion (publication_id UUID, at TIMESTAMPTZ DEFAULT now())");
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION swallow_completion() RETURNS trigger AS $$
                BEGIN
                  IF NEW.completion_date IS NOT NULL AND OLD.completion_date IS NULL THEN
                    INSERT INTO lost_completion (publication_id) VALUES (OLD.id);
                    RETURN NULL;
                  END IF;
                  RETURN NEW;
                END $$ LANGUAGE plpgsql""");
        jdbc.execute("CREATE TRIGGER swallow_completion BEFORE UPDATE ON event_publication "
                + "FOR EACH ROW EXECUTE FUNCTION swallow_completion()");
        UUID id;
        UUID publicationId;
        try {
            id = orders.placeOrder(sku, 2);
            publicationId = (UUID) publication(id).get("id");
            await().atMost(WAIT).until(() -> jdbc.queryForObject(
                    "SELECT count(*) FROM lost_completion WHERE publication_id = ?", Long.class, publicationId) == 1L);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS swallow_completion ON event_publication");
        }

        // Etki kalici, publication tamamlanmamis: tam "at-least-once" penceresi
        assertThat(inventory.availableStock(sku)).hasValue(8);
        assertThat(inventory.reservationCount(id)).isEqualTo(1);
        assertThat(publication(id).get("completion_date")).isNull();

        clock.advance(Duration.ofMinutes(10));
        incomplete.resubmitIncompletePublications(forOrder(id));

        awaitStatus(id, "COMPLETED");
        // Ikinci teslim dedup'a takildi: stok bir kez dustu, rezervasyon tek
        assertThat(inventory.availableStock(sku)).hasValue(8);
        assertThat(inventory.reservationCount(id)).isEqualTo(1);
        assertThat((Integer) publication(id).get("completion_attempts")).isGreaterThanOrEqualTo(2);
    }

    // --- 3e: publish SONRASI istisna -> ne siparis ne publication (birlikte geri alinir) ------------------

    @Test
    void exceptionAfterPublishRollsBackOrderAndPublicationTogether() throws Exception {
        String sku = sku();
        inventory.provision(sku, 10);
        AtomicReference<UUID> id = new AtomicReference<>();

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            id.set(orders.placeOrder(sku, 1));
            assertThat(countPublicationsInTx(id.get())).isEqualTo(1); // publish gercekten oldu
            throw new IllegalStateException("downstream step failed after publish");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(countOrders(otherSession, id.get())).isZero();
        assertThat(countPublications(otherSession, id.get())).isZero();
        // AFTER_COMMIT dinleyici hic tetiklenmez: stok ve rezervasyon dokunulmamis
        assertThat(inventory.availableStock(sku)).hasValue(10);
        assertThat(inventory.reservationCount(id.get())).isZero();
    }

    // --- yardimcilar ------------------------------------------------------------------------------------

    private static String sku() {
        return "SKU-" + UUID.randomUUID();
    }

    private static Predicate<EventPublication> forOrder(UUID id) {
        return p -> p.getEvent() instanceof OrderPlaced e && e.orderId().equals(id);
    }

    private void awaitStatus(UUID orderId, String status) {
        await().atMost(WAIT).until(() -> status.equals(publication(orderId).get("status")));
    }

    private Map<String, Object> publication(UUID orderId) {
        return jdbc.queryForMap("SELECT * FROM event_publication WHERE serialized_event LIKE ?", "%" + orderId + "%");
    }

    private boolean listenerWaitingOnLock() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM pg_stat_activity
                WHERE wait_event_type = 'Lock' AND query LIKE 'UPDATE inventory.stock%'
                """, Long.class) > 0;
    }

    private long countOrdersInTx(UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM ordering.orders WHERE id = ?", Long.class, id);
    }

    private long countPublicationsInTx(UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM event_publication WHERE serialized_event LIKE ?",
                Long.class, "%" + id + "%");
    }

    private static long countOrders(DataSource ds, UUID id) {
        return count(ds, "SELECT count(*) FROM ordering.orders WHERE id = ?", id);
    }

    private static long countPublications(DataSource ds, UUID id) {
        return count(ds, "SELECT count(*) FROM event_publication WHERE serialized_event LIKE ?", "%" + id + "%");
    }

    /** Spring'in transaction'ina KATILMAYAN ayri bir oturumdan (autocommit) sayim. */
    private static long count(DataSource ds, String sql, Object arg) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, arg);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
