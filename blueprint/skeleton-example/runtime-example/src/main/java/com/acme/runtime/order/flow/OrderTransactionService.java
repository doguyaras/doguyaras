package com.acme.runtime.order.flow;

import com.acme.platform.messaging.saga.LocalSagaStore;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain yazimi + saga success() AYNI local TX'te, ayri bir bean uzerinden (referans Bolum 11.4 adim 3). success()
 * compare-and-set kaybederse (recovery iptal etti) SagaCancelledException firlar ve siparis satiri rollback olur.
 */
@Service
public class OrderTransactionService {

    private final NamedParameterJdbcTemplate jdbc;
    private final LocalSagaStore sagaStore;
    private final Clock clock;

    public OrderTransactionService(NamedParameterJdbcTemplate jdbc, LocalSagaStore sagaStore, Clock clock) {
        this.jdbc = jdbc;
        this.sagaStore = sagaStore;
        this.clock = clock;
    }

    @Transactional
    public UUID createOrder(UUID sagaId, UUID accountId, UUID operationKey, UUID resourceId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO "order".order_item (id, account_id, resource_id, operation_key, created_at)
                VALUES (:id, :a, :r, :k, :now)""",
                new MapSqlParameterSource().addValue("id", id).addValue("a", accountId).addValue("r", resourceId)
                        .addValue("k", operationKey).addValue("now", Timestamp.from(clock.instant())));
        sagaStore.success(sagaId, "ORDER:" + id);
        return id;
    }
}
