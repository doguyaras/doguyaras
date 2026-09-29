package com.acme.platform.messaging.saga;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Katilimci ornegi (referans Bolum 11.4 "Katilimci sozlesmesi"): abonelik/kota servisinin kendi semasinda.
 * Gercekte HTTP arkasindadir; burada in-process. Tekillik UNIQUE(caller_service, account_id, operation_key);
 * caller_service JWT act claim'inden gelir (burada parametre). Iki katmanli yetki: allowlist (HTTP filtresi, burada yok)
 * + kod ici aktor -> izinli islem tipi haritasi. Kilit: advisory lock (islem anahtari) + FOR UPDATE (kota satiri).
 */
public class QuotaParticipant implements SagaParticipant {

    static final Map<String, Set<String>> ALLOWED_OPERATIONS = Map.of("order-service", Set.of("ORDER_QUOTA"));

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public final AtomicInteger consumeApplied = new AtomicInteger(), refunds = new AtomicInteger();

    public QuotaParticipant(NamedParameterJdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    public static final String DDL = """
            CREATE TABLE subscription.quota (account_id UUID PRIMARY KEY, remaining INT NOT NULL CHECK (remaining >= 0));
            CREATE TABLE subscription.operation (
              caller_service TEXT NOT NULL, account_id UUID NOT NULL, operation_key UUID NOT NULL,
              op_type TEXT NOT NULL, status TEXT NOT NULL, amount INT NOT NULL,
              refunded_at TIMESTAMPTZ,
              PRIMARY KEY (caller_service, account_id, operation_key));
            """;

    public void grant(UUID accountId, int remaining) {
        jdbc.update("INSERT INTO subscription.quota (account_id, remaining) VALUES (:a, :r) ON CONFLICT (account_id) DO UPDATE SET remaining = :r",
                new MapSqlParameterSource().addValue("a", accountId).addValue("r", remaining));
    }

    public int remaining(UUID accountId) {
        return jdbc.queryForObject("SELECT remaining FROM subscription.quota WHERE account_id = :a", Map.of("a", accountId), Integer.class);
    }

    @Override
    public State consume(String caller, UUID account, UUID key, String opType, int amount) {
        if (!ALLOWED_OPERATIONS.getOrDefault(caller, Set.of()).contains(opType)) {
            throw new ParticipantForbiddenException("actor not allowed for operation type");   // kod ici aktor -> islem tipi
        }
        return tx.execute(st -> {
            lock(caller, account, key);
            State existing = status(caller, account, key);
            if (existing != null) return existing;                     // replay (CANCELLED tombstone dahil: uygulanmaz)
            Integer remaining = DataAccessUtils.singleResult(jdbc.queryForList(
                    "SELECT remaining FROM subscription.quota WHERE account_id = :a FOR UPDATE", Map.of("a", account), Integer.class));
            State result;
            if (remaining != null && remaining >= amount) {
                jdbc.update("UPDATE subscription.quota SET remaining = remaining - :n WHERE account_id = :a",
                        Map.of("n", amount, "a", account));
                result = State.APPLIED;
                consumeApplied.incrementAndGet();
            } else {
                result = State.REJECTED;
            }
            insert(caller, account, key, opType, result, amount);
            return result;
        });
    }

    @Override
    public Optional<State> get(String caller, UUID account, UUID key) { return Optional.ofNullable(status(caller, account, key)); }

    @Override
    public State confirm(String caller, UUID account, UUID key) {
        return tx.execute(st -> {
            lock(caller, account, key);
            State s = status(caller, account, key);
            if (s == null) throw new ParticipantConflictException("confirm without consume");
            return switch (s) {
                case APPLIED -> { setStatus(caller, account, key, State.CONFIRMED); yield State.CONFIRMED; }
                case CONFIRMED -> State.CONFIRMED;                                       // replay
                default -> throw new ParticipantConflictException("confirm on " + s);   // CANCELLED/COMPENSATED/REJECTED
            };
        });
    }

    @Override
    public State compensate(String caller, UUID account, UUID key) {
        return tx.execute(st -> {
            lock(caller, account, key);
            State s = status(caller, account, key);
            if (s == null) {                                                            // gec gelen consume uygulanmasin
                insert(caller, account, key, "TOMBSTONE", State.CANCELLED, 0);
                return State.CANCELLED;
            }
            return switch (s) {
                case APPLIED -> {                                                       // iade; cift iade refunded_at ile engellenir
                    int n = jdbc.update("""
                            UPDATE subscription.operation SET status = 'COMPENSATED', refunded_at = now()
                            WHERE caller_service = :c AND account_id = :a AND operation_key = :k AND refunded_at IS NULL""",
                            Map.of("c", caller, "a", account, "k", key));
                    if (n == 1) {
                        Integer amount = jdbc.queryForObject("SELECT amount FROM subscription.operation WHERE caller_service = :c AND account_id = :a AND operation_key = :k",
                                Map.of("c", caller, "a", account, "k", key), Integer.class);
                        jdbc.update("UPDATE subscription.quota SET remaining = remaining + :n WHERE account_id = :a", Map.of("n", amount, "a", account));
                        refunds.incrementAndGet();
                    }
                    yield State.COMPENSATED;
                }
                case COMPENSATED, CANCELLED, REJECTED -> s == State.REJECTED ? State.CANCELLED : s;   // replay / no-op
                case CONFIRMED -> { setStatus(caller, account, key, State.MANUAL_REVIEW); yield State.MANUAL_REVIEW; }  // iade yok
                case MANUAL_REVIEW -> State.MANUAL_REVIEW;
            };
        });
    }

    private void lock(String caller, UUID account, UUID key) {
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtext(:k))", Map.of("k", caller + ":" + account + ":" + key), Object.class);
    }

    private State status(String caller, UUID account, UUID key) {
        String s = DataAccessUtils.singleResult(jdbc.queryForList(
                "SELECT status FROM subscription.operation WHERE caller_service = :c AND account_id = :a AND operation_key = :k",
                Map.of("c", caller, "a", account, "k", key), String.class));
        return s == null ? null : State.valueOf(s);
    }

    private void insert(String caller, UUID account, UUID key, String opType, State status, int amount) {
        jdbc.update("INSERT INTO subscription.operation (caller_service, account_id, operation_key, op_type, status, amount) VALUES (:c, :a, :k, :t, :s, :n)",
                new MapSqlParameterSource().addValue("c", caller).addValue("a", account).addValue("k", key)
                        .addValue("t", opType).addValue("s", status.name()).addValue("n", amount));
    }

    private void setStatus(String caller, UUID account, UUID key, State status) {
        jdbc.update("UPDATE subscription.operation SET status = :s WHERE caller_service = :c AND account_id = :a AND operation_key = :k",
                Map.of("s", status.name(), "c", caller, "a", account, "k", key));
    }
}
