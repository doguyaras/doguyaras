package com.acme.order.worker;

import com.acme.order.repository.OrderRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Worker tarafi dayaniklilik (referans Bolum 4.7: retry yalniz worker'da, butceyle, idempotent islemde).
 * Proxy tabanlidir: metotlar public ve self-invocation yoktur (ResilienceConfig @EnableResilientMethods).
 */
@Component
public class OrderMaintenanceWorker {

    private static final Logger log = LoggerFactory.getLogger(OrderMaintenanceWorker.class);

    private final OrderRepository repo;

    public OrderMaintenanceWorker(OrderRepository repo) { this.repo = repo; }

    /**
     * Gecici DB hatalari (serialization_failure 40001, deadlock 40P01 -> TransientDataAccessException) toplam
     * 3 denemeye kadar tekrarlanir; kalici hatalar (DataIntegrityViolation vb.) hemen yukari gider.
     * Framework 7 @Retryable'da sayac maxRetries'tir (ilk deneme haric); maxAttempts diye bir nitelik yoktur.
     */
    @Retryable(includes = TransientDataAccessException.class, maxRetries = 2,
            delayString = "${order.worker.retry-delay:100ms}", multiplier = 2.0)
    public int markCancelled(UUID orderId) {
        int updated = repo.updateStatus(orderId, "CANCELLED");
        log.info("Order maintenance: operation=MARK_CANCELLED outcome={} orderId={}", updated == 1 ? "SUCCESS" : "NOOP", orderId);
        return updated;
    }

    /** Projeksiyon rebuild'i deterministik olmali: ayni anda tek kosucu (semaphore bulkhead, bekleyenler kuyrukta). */
    @ConcurrencyLimit(1)
    public int rebuildProjection() {
        int rows = repo.findAll().size();
        log.info("Order maintenance: operation=REBUILD_PROJECTION outcome=SUCCESS rows={}", rows);
        return rows;
    }
}
