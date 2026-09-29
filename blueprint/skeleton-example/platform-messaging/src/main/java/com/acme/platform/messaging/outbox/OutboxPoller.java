package com.acme.platform.messaging.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic outbox poller (referans Bolum 23.4). Her lane (kind) icin ayri dongu: yavas bir HTTP hedefi event yayinini
 * bekletmez. Satirlar SKIP LOCKED + kira ile claim edilir, uzak is transaction disindadir. Hedefler idempotent oldugu
 * icin tekrar islenme zararsizdir; claim_token yalniz poller'in kendi yazma yarisini cozer.
 *
 * Zamanlama (Spring @Scheduled) bu sinifin disinda baglanir; boylece testte poll(kind) dogrudan ve deterministik
 * bir Clock ile cagrilir.
 */
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxRepository repository;
    private final Map<String, OutboxHandler> handlersByKind;
    private final OutboxProperties props;
    private final Clock clock;

    public OutboxPoller(OutboxRepository repository, Map<String, OutboxHandler> handlersByKind,
                        OutboxProperties props, Clock clock) {
        this.repository = repository;
        this.handlersByKind = handlersByKind;
        this.props = props;
        this.clock = clock;
    }

    public record PollResult(int claimed, int applied, int failed, int dead, int deferred) {}

    public PollResult poll(String kind) {
        Instant claimedAt = clock.instant();
        Instant leaseEnd = claimedAt.plusSeconds(props.leaseSeconds());
        UUID claimToken = UUID.randomUUID();
        List<OutboxEvent> entries = repository.claim(kind, claimedAt, leaseEnd, claimToken, props.batchSize());
        if (entries.isEmpty()) return new PollResult(0, 0, 0, 0, 0);          // bos turlar loglanmaz

        OutboxHandler handler = handlersByKind.get(kind);
        if (handler == null) throw new IllegalStateException("No outbox handler for lane " + kind);
        Instant workDeadline = leaseEnd.minusSeconds(props.leaseSafetySeconds());
        int applied = 0, failed = 0, deferred = 0, dead = 0;
        for (OutboxEvent entry : entries) {
            if (clock.instant().isAfter(workDeadline)) { deferred++; continue; }   // kira dolunca baska instance alir
            try {
                handler.handle(entry);                                             // TX disinda
                repository.deleteProcessed(entry.id(), claimToken);
                applied++;
            } catch (PermanentFailureException e) {
                if (entry.isNeverDead()) { markFailure(entry, claimToken, e); failed++; }   // guvenlik yan etkisi DEAD olmaz
                else {
                    repository.release(entry.id(), claimToken, "DEAD", entry.retryCount(), clock.instant(),
                            e.getClass().getSimpleName());
                    dead++;
                }
            } catch (Exception e) {
                markFailure(entry, claimToken, e);
                failed++;
            }
        }
        log.info("Outbox batch finished: lane={} claimed={} applied={} failed={} dead={} deferred={}",
                kind, entries.size(), applied, failed, dead, deferred);
        return new PollResult(entries.size(), applied, failed, dead, deferred);
    }

    private void markFailure(OutboxEvent entry, UUID claimToken, Exception e) {
        int retries = entry.retryCount() + 1;
        String errorType = e.getClass().getSimpleName();                           // exception mesaji degil
        repository.release(entry.id(), claimToken, "PENDING", retries,
                clock.instant().plusSeconds(backoffSeconds(retries, props.maxBackoffSeconds())), errorType);
        if (retries % props.stuckAlertEvery() == 0) {
            log.error("Outbox entry still failing: code=OUTBOX_STUCK lane={} eventId={} retry={} exceptionType={}",
                    entry.kind(), entry.id(), retries, errorType);
        } else {
            log.warn("Outbox entry failed; retry scheduled: lane={} eventId={} retry={} exceptionType={}",
                    entry.kind(), entry.id(), retries, errorType);
        }
    }

    // min(maxBackoff, 30 * 2^n) sn — baslangic ayari
    static long backoffSeconds(int retries, long maxBackoff) {
        return Math.min(maxBackoff, 30L * (1L << Math.min(Math.max(retries, 0), 20)));
    }
}
