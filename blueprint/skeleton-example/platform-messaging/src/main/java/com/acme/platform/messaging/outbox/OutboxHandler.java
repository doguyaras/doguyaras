package com.acme.platform.messaging.outbox;

/**
 * Bir lane'in (kind) isini yapar: EVENT -> exchange publish + confirm, COMMAND -> queue, HTTP -> client.
 * Transaction DISINDA cagrilir. Hedef idempotent olmak zorundadir: claim_token cift teslimi engellemez (Bolum 11.2).
 * Kalici hata icin {@link PermanentFailureException}; diger her exception gecici sayilir ve backoff ile yeniden denenir.
 */
@FunctionalInterface
public interface OutboxHandler {
    void handle(OutboxEvent event) throws Exception;
}
