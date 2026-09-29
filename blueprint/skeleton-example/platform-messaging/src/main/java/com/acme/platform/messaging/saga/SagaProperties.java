package com.acme.platform.messaging.saga;

/**
 * Baslangic ayarlari (referans Bolum 11.4 "Isletim degerleri"; Bolum 1.4: olcumle degisir).
 * Config key'leri: operation-consistency.{deadline-seconds,lease-seconds,max-backoff-seconds,warn-after-minutes,retention-days,batch-size}
 */
public record SagaProperties(long deadlineSeconds, long leaseSeconds, long maxBackoffSeconds,
                             long warnAfterMinutes, long retentionDays, int batchSize) {
    public static SagaProperties defaults() { return new SagaProperties(15, 60, 300, 15, 30, 50); }
}
