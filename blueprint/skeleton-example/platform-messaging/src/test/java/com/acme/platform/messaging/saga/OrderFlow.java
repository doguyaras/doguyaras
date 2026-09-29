package com.acme.platform.messaging.saga;

import com.acme.platform.messaging.saga.LocalSagaStore.BeginResult;
import com.acme.platform.messaging.saga.SagaParticipant.ParticipantUnavailableException;
import com.acme.platform.messaging.saga.SagaParticipant.State;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Koordinator tarafindaki istek akisi (referans Bolum 11.4 adim 1-3): begin (ayri TX) -> consume (TX disi) ->
 * domain yazimi + success() (ayni TX). Test icin "cokme noktalari" enjekte edilebilir.
 */
public class OrderFlow {

    public enum Kind { OK, REPLAY, IN_PROGRESS, CANCELLED, REJECTED, UPSTREAM_UNAVAILABLE, DOMAIN_CONFLICT }
    public record Result(Kind kind, String detail, UUID sagaId) {}
    public enum CrashPoint { NONE, AFTER_BEGIN, AFTER_CONSUME }
    public static class SimulatedCrash extends RuntimeException { SimulatedCrash(String p) { super("crash at " + p); } }

    static final String CALLER = "order-service", SCOPE = "ORDER_CREATE", STEP = "quota";
    public static final String DDL = """
            CREATE TABLE "order".order_item (id UUID PRIMARY KEY, account_id UUID NOT NULL, resource_id UUID NOT NULL UNIQUE,
                                             operation_key UUID NOT NULL);
            """;

    private final LocalSagaStore store;
    private final SagaParticipant participant;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public volatile CrashPoint crashAt = CrashPoint.NONE;
    public volatile Runnable beforeSuccess = () -> {};          // yaris testleri icin kanca
    public volatile Runnable afterBegin = () -> {};

    public OrderFlow(LocalSagaStore store, SagaParticipant participant, NamedParameterJdbcTemplate jdbc, TransactionTemplate tx) {
        this.store = store; this.participant = participant; this.jdbc = jdbc; this.tx = tx;
    }

    public Result createOrder(UUID account, UUID operationKey, UUID resourceId) {
        BeginResult b = store.begin(account, SCOPE, operationKey, STEP);
        UUID sagaId = b.saga().id();
        if (!b.created()) {
            return switch (b.saga().status()) {                                   // idempotent replay
                case SUCCEEDED, CONFIRMED -> new Result(Kind.REPLAY, b.saga().result(), sagaId);
                case COMPENSATED, MANUAL_REVIEW -> new Result(Kind.CANCELLED, "OPERATION_CANCELLED", sagaId);
                default -> new Result(Kind.IN_PROGRESS, "OPERATION_IN_PROGRESS", sagaId);
            };
        }
        afterBegin.run();
        if (crashAt == CrashPoint.AFTER_BEGIN) throw new SimulatedCrash("AFTER_BEGIN");

        State state;
        try {
            state = participant.consume(CALLER, account, operationKey, "ORDER_QUOTA", 1);   // TX disi, ayni operationKey
        } catch (ParticipantUnavailableException e) {
            return new Result(Kind.UPSTREAM_UNAVAILABLE, "UPSTREAM_UNAVAILABLE", sagaId);  // saga STARTED kalir; deadline -> recovery
        }
        if (state != State.APPLIED) {
            store.fail(sagaId);                                                   // REJECTED/CANCELLED: telafi (no-op/tombstone)
            return new Result(Kind.REJECTED, "QUOTA_" + state, sagaId);
        }
        if (crashAt == CrashPoint.AFTER_CONSUME) throw new SimulatedCrash("AFTER_CONSUME");

        try {
            return tx.execute(st -> {                                              // domain yazimi + success() AYNI TX
                UUID id = UUID.randomUUID();
                jdbc.update("INSERT INTO \"order\".order_item (id, account_id, resource_id, operation_key) VALUES (:id, :a, :r, :k)",
                        Map.of("id", id, "a", account, "r", resourceId, "k", operationKey));
                beforeSuccess.run();
                store.success(sagaId, "ORDER:" + id);                              // CAS; recovery iptal ettiyse exception -> rollback
                return new Result(Kind.OK, "ORDER:" + id, sagaId);
            });
        } catch (SagaCancelledException e) {
            return new Result(Kind.CANCELLED, "OPERATION_CANCELLED", sagaId);
        } catch (DuplicateKeyException e) {
            store.fail(sagaId);                                                   // domain uniqueness reddetti -> telafi
            return new Result(Kind.DOMAIN_CONFLICT, "RESOURCE_ALREADY_ORDERED", sagaId);
        }
    }

    public int orderCount(UUID account) {
        return jdbc.queryForObject("SELECT count(*) FROM \"order\".order_item WHERE account_id = :a", Map.of("a", account), Integer.class);
    }
}
