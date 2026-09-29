package com.acme.broker.consume;

import java.time.Instant;
import java.util.UUID;

/**
 * Listener'in her teslim icin verdigi kararin gozlemi: uretimde metrik/log (consumer_outcome_total{outcome}), testte
 * kanit (deneme sayisi, x-delivery-count, teslimler arasi sure). Listener kararini bu arayuze BAGLAMAZ; yalniz bildirir.
 */
@FunctionalInterface
public interface DeliveryObserver {

    enum Outcome { APPLIED, DUPLICATE, IGNORED_TYPE, TRANSIENT_FAILURE, POISON }

    /** @param eventId zehirli mesajda cozulemeyebilir (null) */
    record Attempt(UUID eventId, Outcome outcome, int deliveryCount, boolean redelivered, Instant at, String errorType) {}

    void observe(Attempt attempt);

    static DeliveryObserver none() { return a -> { }; }
}
