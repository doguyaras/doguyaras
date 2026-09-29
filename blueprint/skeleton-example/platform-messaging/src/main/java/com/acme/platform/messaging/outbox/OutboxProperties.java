package com.acme.platform.messaging.outbox;

/**
 * Poller baslangic ayarlari (referans Bolum 1.4: "baslangic ayari" sinifi; yuk testiyle degisir).
 * batch 50, lease 120 sn, guvenlik payi 30 sn, backoff min(600, 30*2^n), STUCK alarmi her 10 denemede.
 */
public record OutboxProperties(int batchSize, long leaseSeconds, long leaseSafetySeconds,
                               long maxBackoffSeconds, int stuckAlertEvery) {
    public static OutboxProperties defaults() { return new OutboxProperties(50, 120, 30, 600, 10); }
}
