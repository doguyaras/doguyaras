package com.acme.platform.messaging.saga;

import com.acme.platform.messaging.saga.LocalSagaStore.Action;
import com.acme.platform.messaging.saga.LocalSagaStore.Saga;
import com.acme.platform.messaging.saga.LocalSagaStore.Step;
import com.acme.platform.messaging.saga.SagaParticipant.ParticipantConflictException;
import com.acme.platform.messaging.saga.SagaParticipant.ParticipantForbiddenException;
import com.acme.platform.messaging.saga.SagaParticipant.ParticipantUnavailableException;
import com.acme.platform.messaging.saga.SagaParticipant.State;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recovery worker (referans Bolum 11.4 adim 4). Her instance'ta calisir; adimlar SKIP LOCKED + lock_token + lease ile
 * claim edilir. Uzak cagri TX disinda. Hata veya belirsiz sonucta once GET ile katilimcinin durumu sorulur; celiski
 * varsa MANUAL_REVIEW. Zamanlama (@Scheduled, poll 5 sn) disarida baglanir; runOnce() deterministik test edilir.
 */
public class SagaRecoveryWorker {

    private static final Logger log = LoggerFactory.getLogger(SagaRecoveryWorker.class);

    public record RunResult(int claimed, int done, int retried, int manualReview, int skipped) {}

    private final LocalSagaStore store;
    private final SagaParticipant participant;
    private final String callerService;
    private final SagaProperties props;
    private final Clock clock;

    public SagaRecoveryWorker(LocalSagaStore store, SagaParticipant participant, String callerService,
                              SagaProperties props, Clock clock) {
        this.store = store;
        this.participant = participant;
        this.callerService = callerService;
        this.props = props;
        this.clock = clock;
    }

    public RunResult runOnce() {
        UUID token = UUID.randomUUID();
        Instant now = clock.instant();
        List<Step> steps = store.claimSteps(token, now, now.plusSeconds(props.leaseSeconds()), props.batchSize());
        int done = 0, retried = 0, manual = 0, skipped = 0;
        for (Step step : steps) {
            switch (process(step, token)) {
                case DONE -> done++;
                case RETRIED -> retried++;
                case MANUAL_REVIEW -> manual++;
                case SKIPPED -> skipped++;
            }
        }
        if (!steps.isEmpty()) {
            log.info("Saga recovery batch finished: claimed={} done={} retried={} manualReview={} skipped={}",
                    steps.size(), done, retried, manual, skipped);
        }
        return new RunResult(steps.size(), done, retried, manual, skipped);
    }

    enum Outcome { DONE, RETRIED, MANUAL_REVIEW, SKIPPED }

    Outcome process(Step step, UUID token) {
        Optional<Action> prepared = store.prepare(step, token);
        if (prepared.isEmpty()) return Outcome.SKIPPED;                 // kira elden gitti veya saga zaten terminal
        Action action = prepared.get();
        Saga saga = store.findById(step.sagaId()).orElse(null);
        if (saga == null) return Outcome.SKIPPED;
        try {
            State state = action == Action.CONFIRM
                    ? participant.confirm(callerService, saga.accountId(), saga.operationKey())
                    : participant.compensate(callerService, saga.accountId(), saga.operationKey());
            return settle(step, token, action, state);
        } catch (ParticipantUnavailableException e) {
            // Belirsiz sonuc: once GET ile sor (istek katilimcida commit olmus olabilir)
            try {
                Optional<State> remote = participant.get(callerService, saga.accountId(), saga.operationKey());
                if (remote.isPresent()) return settle(step, token, action, remote.get());
            } catch (ParticipantUnavailableException ignored) { /* asagida retry */ }
            boolean ok = store.retry(step, token, e.getClass().getSimpleName());
            log.warn("Saga step deferred; retry scheduled: sagaId={} step={} attempt={} exceptionType={}",
                    saga.id(), step.stepName(), step.attempt() + 1, e.getClass().getSimpleName());
            return ok ? Outcome.RETRIED : Outcome.SKIPPED;
        } catch (ParticipantConflictException | ParticipantForbiddenException e) {
            return manual(step, token, saga, e.getClass().getSimpleName());
        }
    }

    /** Katilimcinin dondurdugu durum niyetle tutarli mi? Tutarsizsa insan karari. */
    private Outcome settle(Step step, UUID token, Action action, State state) {
        boolean consistent = switch (action) {
            case CONFIRM -> state == State.CONFIRMED;
            case COMPENSATE -> state == State.COMPENSATED || state == State.CANCELLED || state == State.REJECTED;
        };
        if (!consistent) {
            Saga saga = store.findById(step.sagaId()).orElse(null);
            return manual(step, token, saga, "STATE_" + state.name());
        }
        // Belirsizlikten sonra GET APPLIED dondurduyse is henuz yapilmamis demektir -> retry
        return store.complete(step, token, action) ? Outcome.DONE : Outcome.SKIPPED;
    }

    private Outcome manual(Step step, UUID token, Saga saga, String reason) {
        boolean ok = store.manualReview(step, token, reason);
        log.error("Saga needs manual review: code=SAGA_MANUAL_REVIEW sagaId={} step={} reason={}",
                saga == null ? null : saga.id(), step.stepName(), reason);
        return ok ? Outcome.MANUAL_REVIEW : Outcome.SKIPPED;
    }
}
