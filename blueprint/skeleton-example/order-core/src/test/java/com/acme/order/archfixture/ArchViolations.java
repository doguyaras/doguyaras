package com.acme.order.archfixture;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * KASITLI IHLALLER: yalniz TransactionBoundaryRulesTest'in negatif testleri bu paketi ice aktarir.
 * Kurallar bu siniflari yakalayamazsa negatif test kirmizi olur (yesil kural bir sey kanitlamaz).
 */
public final class ArchViolations {

    private ArchViolations() { }

    /** Outbox yazicisi MANDATORY degil: domain TX'i yokken kendi TX'ini acar, olay domain'den bagimsiz commit olur. */
    public static class BadOutboxWriter {
        @Transactional(propagation = Propagation.REQUIRED)
        public void append(Object event) { }
    }

    /** Sinif duzeyinde MANDATORY: metot anotasyonsuz ama sinif anotasyonu gecerli (kural bunu ihlal SAYMAMALI). */
    @Transactional(propagation = Propagation.MANDATORY)
    public static class ClassLevelMandatoryOutboxWriter {
        public void append(Object event) { }
    }

    /** Uzak cagri yapan client. */
    public static class InventoryClient {
        public int stock(String sku) { return 0; }
    }

    /** TX tutulurken uzak HTTP: baglanti ve satir kilidi uzak cagri suresince tutulur (Bolum 4.3). */
    public static class BadService {
        private final InventoryClient inventory = new InventoryClient();

        @Transactional
        public int cancel(String sku) { return inventory.stock(sku); }
    }

    /** Sinif duzeyinde @Transactional + client cagrisi: metot anotasyonsuz oldugu icin naif kural kacirir. */
    @Transactional
    public static class BadClassLevelService {
        private final InventoryClient inventory = new InventoryClient();

        public int cancel(String sku) { return inventory.stock(sku); }
    }
}
