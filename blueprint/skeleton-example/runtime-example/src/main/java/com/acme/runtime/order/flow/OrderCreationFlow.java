package com.acme.runtime.order.flow;

import com.acme.platform.messaging.saga.LocalSagaStore;
import com.acme.platform.messaging.saga.LocalSagaStore.BeginResult;
import com.acme.platform.messaging.saga.SagaCancelledException;
import com.acme.platform.messaging.saga.SagaParticipant;
import com.acme.platform.messaging.saga.SagaParticipant.ParticipantConflictException;
import com.acme.platform.messaging.saga.SagaParticipant.ParticipantForbiddenException;
import com.acme.platform.messaging.saga.SagaParticipant.ParticipantUnavailableException;
import com.acme.platform.messaging.saga.SagaParticipant.State;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Istek yolu (referans Bolum 11.4 adim 1-3): begin (ayri TX) -> consume (TX DISI, ayni operationKey) -> domain +
 * success() (ayni TX). Uzak cagri belirsiz bittiyse (timeout, 5xx, circuit acik) saga STARTED birakilir: deadline
 * dolunca recovery worker GET ile uzlasir ve telafi eder. Istek yolunda retry yok (Bolum 4.7).
 */
@Service
public class OrderCreationFlow {

    public static final String CALLER = "order-service";
    public static final String SCOPE = "ORDER_CREATE";
    public static final String STEP = "quota";
    public static final String OPERATION_TYPE = "ORDER_QUOTA";

    public enum Kind { OK, REPLAY, IN_PROGRESS, CANCELLED, REJECTED, UPSTREAM_UNAVAILABLE, UPSTREAM_REJECTED, DOMAIN_CONFLICT }

    public record Result(Kind kind, String detail, UUID sagaId) {}

    private final LocalSagaStore store;
    private final SagaParticipant participant;
    private final OrderTransactionService transactions;

    public OrderCreationFlow(LocalSagaStore store, SagaParticipant participant, OrderTransactionService transactions) {
        this.store = store;
        this.participant = participant;
        this.transactions = transactions;
    }

    public Result createOrder(UUID accountId, UUID operationKey, UUID resourceId) {
        BeginResult begin = store.begin(accountId, SCOPE, operationKey, STEP);
        UUID sagaId = begin.saga().id();
        if (!begin.created()) {                                           // ayni key: idempotent replay
            return switch (begin.saga().status()) {
                case SUCCEEDED, CONFIRMED -> new Result(Kind.REPLAY, begin.saga().result(), sagaId);
                case COMPENSATED, MANUAL_REVIEW -> new Result(Kind.CANCELLED, "OPERATION_CANCELLED", sagaId);
                default -> new Result(Kind.IN_PROGRESS, "OPERATION_IN_PROGRESS", sagaId);
            };
        }
        State state;
        try {
            state = participant.consume(CALLER, accountId, operationKey, OPERATION_TYPE, 1);
        } catch (ParticipantUnavailableException e) {
            return new Result(Kind.UPSTREAM_UNAVAILABLE, "UPSTREAM_UNAVAILABLE", sagaId);   // STARTED kalir; deadline -> recovery
        } catch (ParticipantForbiddenException | ParticipantConflictException e) {
            store.fail(sagaId);                                           // katilimci reddetti: telafi (no-op/tombstone)
            return new Result(Kind.UPSTREAM_REJECTED, "UPSTREAM_REJECTED", sagaId);
        }
        if (state != State.APPLIED) {
            store.fail(sagaId);
            return new Result(Kind.REJECTED, "QUOTA_" + state, sagaId);
        }
        try {
            UUID orderId = transactions.createOrder(sagaId, accountId, operationKey, resourceId);
            return new Result(Kind.OK, "ORDER:" + orderId, sagaId);
        } catch (SagaCancelledException e) {
            return new Result(Kind.CANCELLED, "OPERATION_CANCELLED", sagaId);
        } catch (DuplicateKeyException e) {
            store.fail(sagaId);                                           // domain uniqueness reddetti -> telafi
            return new Result(Kind.DOMAIN_CONFLICT, "RESOURCE_ALREADY_ORDERED", sagaId);
        }
    }
}
