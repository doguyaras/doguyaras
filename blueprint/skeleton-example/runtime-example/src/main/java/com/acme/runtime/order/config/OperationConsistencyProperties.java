package com.acme.runtime.order.config;

import com.acme.platform.messaging.saga.SagaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Bolum 11.4 "Isletim degerleri" (operation-consistency.*); pollMillis recovery worker'in @Scheduled araligidir. */
@ConfigurationProperties("operation-consistency")
public record OperationConsistencyProperties(
        @DefaultValue("15") long deadlineSeconds,
        @DefaultValue("60") long leaseSeconds,
        @DefaultValue("300") long maxBackoffSeconds,
        @DefaultValue("15") long warnAfterMinutes,
        @DefaultValue("30") long retentionDays,
        @DefaultValue("50") int batchSize,
        @DefaultValue("5000") long pollMillis) {

    public SagaProperties toSagaProperties() {
        return new SagaProperties(deadlineSeconds, leaseSeconds, maxBackoffSeconds, warnAfterMinutes, retentionDays, batchSize);
    }
}
