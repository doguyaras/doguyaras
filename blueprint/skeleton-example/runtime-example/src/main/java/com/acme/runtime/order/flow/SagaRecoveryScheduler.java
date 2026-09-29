package com.acme.runtime.order.flow;

import com.acme.platform.messaging.saga.SagaRecoveryWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Recovery worker'in zamanlamasi (referans Bolum 11.4 "poll"). Her instance calistirir; esgudum claim'deki
 * SKIP LOCKED + lease ile saglanir, leader election gerekmez. Bir turdaki hata sonraki turu durdurmaz.
 */
@Component
public class SagaRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SagaRecoveryScheduler.class);

    private final SagaRecoveryWorker worker;

    public SagaRecoveryScheduler(SagaRecoveryWorker worker) { this.worker = worker; }

    @Scheduled(fixedDelayString = "${operation-consistency.poll-millis:5000}")
    public void poll() {
        try {
            worker.runOnce();
        } catch (RuntimeException e) {
            log.error("saga recovery run failed: exceptionType={}", e.getClass().getSimpleName(), e);
        }
    }
}
