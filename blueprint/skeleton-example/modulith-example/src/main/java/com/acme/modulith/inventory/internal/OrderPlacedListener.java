package com.acme.modulith.inventory.internal;

import java.time.Clock;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.acme.modulith.order.OrderPlaced;

/**
 * OrderPlaced -> stok rezervasyonu.
 *
 * <p>{@code @ApplicationModuleListener} = @Async + @Transactional(REQUIRES_NEW) + @TransactionalEventListener
 * (AFTER_COMMIT). Registry teslimati "tamamlandi" olarak dinleyicinin transaction'i commit OLDUKTAN SONRA,
 * ayri bir yazimla isaretler. Arada surec olurse etki kalici ama kayit tamamlanmamis olur ve olay
 * yeniden teslim edilir (restart'ta ya da resubmit ile). Yani teslim en-az-bir-kez'dir: dinleyici
 * idempotent OLMAK ZORUNDA (Bolum 11.3 ile ayni kural, in-process olsa bile).
 */
@Component
class OrderPlacedListener {

    private final StockRepository stock;
    private final Clock clock;

    OrderPlacedListener(StockRepository stock, Clock clock) {
        this.stock = stock;
        this.clock = clock;
    }

    @ApplicationModuleListener
    public void on(OrderPlaced event) {
        // Dedup kaydi etkiyle AYNI transaction'da: ya ikisi birlikte commit olur ya hicbiri.
        if (!stock.recordReservationOnce(event.orderId(), event.sku(), event.quantity(), clock.instant())) {
            return; // bu siparis zaten rezerve edildi (tekrar teslim)
        }
        if (!stock.decrement(event.sku(), event.quantity())) {
            // Istisna transaction'i (dedup satiri dahil) geri alir ve publication tamamlanmamis kalir;
            // stok tanimlaninca resubmit ayni olayi yeniden teslim eder.
            throw new StockNotProvisionedException(event.sku());
        }
    }

    static final class StockNotProvisionedException extends RuntimeException {
        StockNotProvisionedException(String sku) {
            super("stock not provisioned for sku " + sku);
        }
    }
}
