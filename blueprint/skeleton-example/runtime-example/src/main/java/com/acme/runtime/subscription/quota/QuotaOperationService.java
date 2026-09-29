package com.acme.runtime.subscription.quota;

import com.acme.platform.core.ServiceException;
import com.acme.platform.messaging.saga.SagaParticipant.State;
import com.acme.runtime.subscription.web.SubscriptionErrorCode;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Katilimci is mantigi (referans Bolum 11.4 tablo). Tum islemler idempotent ve ayni anahtar icin advisory lock ile
 * siralanir; kota satiri FOR UPDATE ile kilitlenir. caller her zaman dogrulanmis JWT act claim'idir (controller verir).
 *
 * Iki katmanli yetki: allowlist + delegasyon (HTTP katmani) ve burada "aktor -> izinli islem tipi" haritasi.
 */
@Service
public class QuotaOperationService {

    static final Map<String, Set<String>> ALLOWED_OPERATIONS = Map.of("order-service", Set.of("ORDER_QUOTA"));

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    public QuotaOperationService(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager tm, Clock clock) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(tm);
        this.clock = clock;
    }

    public State consume(String caller, UUID account, UUID key, String opType, int amount) {
        if (!ALLOWED_OPERATIONS.getOrDefault(caller, Set.of()).contains(opType)) {
            throw new ServiceException(SubscriptionErrorCode.OPERATION_TYPE_NOT_ALLOWED);
        }
        return tx.execute(st -> {
            lock(caller, account, key);
            State existing = status(caller, account, key);
            if (existing != null) return existing;                       // replay; CANCELLED tombstone dahil (uygulanmaz)
            Integer remaining = DataAccessUtils.singleResult(jdbc.queryForList(
                    "SELECT remaining FROM subscription.quota WHERE account_id = :a FOR UPDATE", Map.of("a", account), Integer.class));
            State result = remaining != null && remaining >= amount ? State.APPLIED : State.REJECTED;
            if (result == State.APPLIED) {
                jdbc.update("UPDATE subscription.quota SET remaining = remaining - :n WHERE account_id = :a", Map.of("n", amount, "a", account));
            }
            insert(caller, account, key, opType, result, amount);
            return result;
        });
    }

    public Optional<State> get(String caller, UUID account, UUID key) {
        return Optional.ofNullable(status(caller, account, key));
    }

    public State confirm(String caller, UUID account, UUID key) {
        return tx.execute(st -> {
            lock(caller, account, key);
            State s = status(caller, account, key);
            if (s == State.APPLIED) { setStatus(caller, account, key, State.CONFIRMED); return State.CONFIRMED; }
            if (s == State.CONFIRMED) return State.CONFIRMED;                                   // replay
            throw new ServiceException(SubscriptionErrorCode.OPERATION_STATE_CONFLICT);        // yok/iptal/iade: celiski
        });
    }

    public State compensate(String caller, UUID account, UUID key) {
        return tx.execute(st -> {
            lock(caller, account, key);
            State s = status(caller, account, key);
            if (s == null) {                                                // tombstone: gec gelen consume uygulanmaz
                insert(caller, account, key, "TOMBSTONE", State.CANCELLED, 0);
                return State.CANCELLED;
            }
            return switch (s) {
                case APPLIED -> {
                    // iade tek sefer: refunded_at IS NULL kosulu tekrar gelen compensate'i no-op yapar
                    int n = jdbc.update("""
                            UPDATE subscription.operation SET status = 'COMPENSATED', refunded_at = :now, updated_at = :now
                            WHERE caller_service = :c AND account_id = :a AND operation_key = :k AND refunded_at IS NULL""",
                            keyParams(caller, account, key).addValue("now", now()));
                    if (n == 1) {
                        jdbc.update("""
                                UPDATE subscription.quota q SET remaining = q.remaining + o.amount FROM subscription.operation o
                                WHERE q.account_id = o.account_id AND o.caller_service = :c AND o.account_id = :a AND o.operation_key = :k""",
                                keyParams(caller, account, key));
                    }
                    yield State.COMPENSATED;
                }
                case REJECTED -> State.CANCELLED;                           // hic uygulanmamis: telafi no-op
                case COMPENSATED, CANCELLED, MANUAL_REVIEW -> s;            // replay
                case CONFIRMED -> { setStatus(caller, account, key, State.MANUAL_REVIEW); yield State.MANUAL_REVIEW; }
            };
        });
    }

    private void lock(String caller, UUID account, UUID key) {
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtext(:k))", Map.of("k", caller + ":" + account + ":" + key), Object.class);
    }

    private State status(String caller, UUID account, UUID key) {
        String s = DataAccessUtils.singleResult(jdbc.queryForList("""
                SELECT status FROM subscription.operation WHERE caller_service = :c AND account_id = :a AND operation_key = :k""",
                keyParams(caller, account, key), String.class));
        return s == null ? null : State.valueOf(s);
    }

    private void insert(String caller, UUID account, UUID key, String opType, State status, int amount) {
        jdbc.update("""
                INSERT INTO subscription.operation (caller_service, account_id, operation_key, op_type, status, amount, created_at, updated_at)
                VALUES (:c, :a, :k, :t, :s, :n, :now, :now)""",
                keyParams(caller, account, key).addValue("t", opType).addValue("s", status.name()).addValue("n", amount)
                        .addValue("now", now()));
    }

    private void setStatus(String caller, UUID account, UUID key, State status) {
        jdbc.update("""
                UPDATE subscription.operation SET status = :s, updated_at = :now
                WHERE caller_service = :c AND account_id = :a AND operation_key = :k""",
                keyParams(caller, account, key).addValue("s", status.name()).addValue("now", now()));
    }

    private static MapSqlParameterSource keyParams(String caller, UUID account, UUID key) {
        return new MapSqlParameterSource().addValue("c", caller).addValue("a", account).addValue("k", key);
    }

    private Timestamp now() { return Timestamp.from(clock.instant()); }
}
