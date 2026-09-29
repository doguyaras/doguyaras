package com.acme.platform.messaging.saga;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Local saga store (referans Bolum 11.4). Tek adim, tek katilimci; domain'den bagimsiz (step adi parametre).
 * Zaman kaynagi disaridan verilen Clock (uretimde DB now() ile ayni saat; testte deterministik).
 *
 * Akis: begin() AYRI TX -> consume (TX disi) -> domain yazimi + success() AYNI TX (compare-and-set) ->
 * recovery worker: claim -> prepare -> confirm/compensate -> complete (lock_token eslesmesi).
 */
public class LocalSagaStore {

    public enum SagaStatus { STARTED, SUCCEEDED, CONFIRMED, CANCEL_REQUESTED, COMPENSATED, MANUAL_REVIEW }
    public enum StepStatus { PENDING, RETRY, RUNNING, DONE, MANUAL_REVIEW }
    public enum Action { CONFIRM, COMPENSATE }

    public record Saga(UUID id, UUID accountId, String scope, UUID operationKey, SagaStatus status, String result,
                       Instant createdAt, Instant updatedAt) {
        public boolean isTerminal() {
            return status == SagaStatus.CONFIRMED || status == SagaStatus.COMPENSATED || status == SagaStatus.MANUAL_REVIEW;
        }
    }

    public record Step(UUID id, UUID sagaId, String stepName, Action nextAction, StepStatus status, int attempt,
                       Instant nextAttemptAt, UUID lockToken, Instant lockedUntil, String lastErrorCode) {}

    public record BeginResult(Saga saga, boolean created) {}

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate requiresNew;
    private final String sagaTable;
    private final String stepTable;
    private final SagaProperties props;
    private final Clock clock;

    public LocalSagaStore(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager tm, String schema,
                          SagaProperties props, Clock clock) {
        this.jdbc = jdbc;
        this.requiresNew = new TransactionTemplate(tm);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.sagaTable = "\"" + schema + "\".saga";
        this.stepTable = "\"" + schema + "\".saga_steps";
        this.props = props;
        this.clock = clock;
    }

    // ---------- istek yolu ----------

    /**
     * Ayri TX'te niyet kaydi. Yeni kayitta adim COMPENSATE/PENDING ve next_attempt_at = now + deadline: surec cokerse
     * deadline dolunca otomatik telafi baslar. Kayit zaten varsa mevcut saga (replay/IN_PROGRESS karari cagirana ait).
     */
    public BeginResult begin(UUID accountId, String scope, UUID operationKey, String stepName) {
        Objects.requireNonNull(accountId, "accountId"); Objects.requireNonNull(operationKey, "operationKey");
        if (scope == null || scope.isBlank()) throw new IllegalArgumentException("scope");
        return requiresNew.execute(st -> {
            Instant now = clock.instant();
            UUID id = UUID.randomUUID();
            int inserted = jdbc.update("""
                    INSERT INTO %s (id, account_id, scope, operation_key, status, created_at, updated_at)
                    VALUES (:id, :account, :scope, :key, 'STARTED', :now, :now)
                    ON CONFLICT (account_id, scope, operation_key) DO NOTHING""".formatted(sagaTable),
                    params().addValue("id", id).addValue("account", accountId).addValue("scope", scope)
                            .addValue("key", operationKey).addValue("now", ts(now)));
            if (inserted == 1) {
                jdbc.update("""
                        INSERT INTO %s (id, saga_id, step_name, next_action, status, attempt, next_attempt_at, created_at, updated_at)
                        VALUES (:id, :saga, :name, 'COMPENSATE', 'PENDING', 0, :next, :now, :now)""".formatted(stepTable),
                        params().addValue("id", UUID.randomUUID()).addValue("saga", id).addValue("name", stepName)
                                .addValue("next", ts(now.plusSeconds(props.deadlineSeconds()))).addValue("now", ts(now)));
                return new BeginResult(new Saga(id, accountId, scope, operationKey, SagaStatus.STARTED, null, now, now), true);
            }
            return new BeginResult(find(accountId, scope, operationKey).orElseThrow(), false);
        });
    }

    /**
     * Domain TX'i ICINDE cagrilir (ayni TX; aktif TX yoksa hata). Compare-and-set STARTED -> SUCCEEDED: recovery araya girip
     * iptal ettiyse (CANCEL_REQUESTED) SagaCancelledException -> domain yazimi rollback olur. Adim CONFIRM'e cevrilir ve
     * hemen denenir. RUNNING adima dokunulmaz: recovery prepare() SUCCEEDED'i gorup CONFIRM'e uzlastirir.
     */
    public void success(UUID sagaId, String result) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("success() must run inside the domain transaction");
        }
        Instant now = clock.instant();
        int n = jdbc.update("UPDATE %s SET status = 'SUCCEEDED', result = :r, updated_at = :now WHERE id = :id AND status = 'STARTED'"
                .formatted(sagaTable), params().addValue("r", result).addValue("now", ts(now)).addValue("id", sagaId));
        if (n == 0) throw new SagaCancelledException(sagaId);
        jdbc.update("""
                UPDATE %s SET next_action = 'CONFIRM', status = 'PENDING', next_attempt_at = :now, updated_at = :now
                WHERE saga_id = :id AND status IN ('PENDING','RETRY')""".formatted(stepTable),
                params().addValue("now", ts(now)).addValue("id", sagaId));
    }

    /** Istek yolu basarisiz (REJECTED, domain hatasi): ayri TX'te iptal iste; recovery hemen telafi eder. */
    public void fail(UUID sagaId) {
        requiresNew.executeWithoutResult(st -> {
            Instant now = clock.instant();
            jdbc.update("UPDATE %s SET status = 'CANCEL_REQUESTED', updated_at = :now WHERE id = :id AND status = 'STARTED'"
                    .formatted(sagaTable), params().addValue("now", ts(now)).addValue("id", sagaId));
            jdbc.update("""
                    UPDATE %s SET next_action = 'COMPENSATE', next_attempt_at = :now, updated_at = :now
                    WHERE saga_id = :id AND status IN ('PENDING','RETRY')""".formatted(stepTable),
                    params().addValue("now", ts(now)).addValue("id", sagaId));
        });
    }

    // ---------- recovery worker ----------

    /** SKIP LOCKED + lock_token + lease. Kirasi dolan RUNNING adim (worker cokmesi) yeniden claim edilir. */
    public List<Step> claimSteps(UUID lockToken, Instant now, Instant lockedUntil, int limit) {
        return jdbc.query("""
                WITH c AS (
                    SELECT s.id FROM %1$s s
                    WHERE (s.status IN ('PENDING','RETRY') AND s.next_attempt_at <= :now)
                       OR (s.status = 'RUNNING' AND s.locked_until <= :now)
                    ORDER BY s.next_attempt_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT :limit)
                UPDATE %1$s s SET status = 'RUNNING', lock_token = :token, locked_until = :until, updated_at = :now
                FROM c WHERE s.id = c.id
                RETURNING s.*""".formatted(stepTable),
                params().addValue("now", ts(now)).addValue("limit", limit).addValue("token", lockToken)
                        .addValue("until", ts(lockedUntil)), STEP);
    }

    /**
     * Sahiplik + durum uzlastirmasi (FOR UPDATE). STARTED saga'yi CANCEL_REQUESTED'a ceker (istek yolu success() CAS'i
     * bunu gorur ve rollback olur). SUCCEEDED -> CONFIRM, CANCEL_REQUESTED -> COMPENSATE. Terminal saga -> adim DONE.
     * Kira elden gitmisse (token eslesmez) bos doner.
     */
    public Optional<Action> prepare(Step step, UUID lockToken) {
        return requiresNew.execute(st -> {
            Instant now = clock.instant();
            Saga saga = DataAccessUtils.singleResult(jdbc.query("SELECT * FROM %s WHERE id = :id FOR UPDATE".formatted(sagaTable),
                    Map.of("id", step.sagaId()), SAGA));
            Step owned = DataAccessUtils.singleResult(jdbc.query(
                    "SELECT * FROM %s WHERE id = :id AND lock_token = :token AND status = 'RUNNING'".formatted(stepTable),
                    params().addValue("id", step.id()).addValue("token", lockToken), STEP));
            if (saga == null || owned == null) return Optional.<Action>empty();
            Action action;
            switch (saga.status()) {
                case STARTED -> {
                    jdbc.update("UPDATE %s SET status = 'CANCEL_REQUESTED', updated_at = :now WHERE id = :id".formatted(sagaTable),
                            params().addValue("now", ts(now)).addValue("id", saga.id()));
                    action = Action.COMPENSATE;
                }
                case CANCEL_REQUESTED -> action = Action.COMPENSATE;
                case SUCCEEDED -> action = Action.CONFIRM;
                default -> {                                             // terminal: adim kapatilir
                    jdbc.update("UPDATE %s SET status = 'DONE', lock_token = NULL, locked_until = NULL, updated_at = :now WHERE id = :id AND lock_token = :token"
                            .formatted(stepTable), params().addValue("now", ts(now)).addValue("id", step.id()).addValue("token", lockToken));
                    return Optional.<Action>empty();
                }
            }
            jdbc.update("UPDATE %s SET next_action = :a, updated_at = :now WHERE id = :id AND lock_token = :token".formatted(stepTable),
                    params().addValue("a", action.name()).addValue("now", ts(now)).addValue("id", step.id()).addValue("token", lockToken));
            return Optional.of(action);
        });
    }

    /** Yalniz kira sahibi tamamlar. 0 satir = kira elden gitmis; sonuc yazilmaz. */
    public boolean complete(Step step, UUID lockToken, Action action) {
        return Boolean.TRUE.equals(requiresNew.execute(st -> {
            Instant now = clock.instant();
            int n = jdbc.update("UPDATE %s SET status = 'DONE', lock_token = NULL, locked_until = NULL, updated_at = :now WHERE id = :id AND lock_token = :token"
                    .formatted(stepTable), params().addValue("now", ts(now)).addValue("id", step.id()).addValue("token", lockToken));
            if (n == 0) return false;
            String target = action == Action.CONFIRM ? "CONFIRMED" : "COMPENSATED";
            jdbc.update("UPDATE %s SET status = :s, updated_at = :now WHERE id = :id AND status IN ('SUCCEEDED','CANCEL_REQUESTED')"
                    .formatted(sagaTable), params().addValue("s", target).addValue("now", ts(now)).addValue("id", step.sagaId()));
            return true;
        }));
    }

    /** Gecici hata: RETRY + backoff min(maxBackoff, 2^n) sn; kira birakilir. */
    public boolean retry(Step step, UUID lockToken, String errorCode) {
        Instant now = clock.instant();
        int attempt = step.attempt() + 1;
        long backoff = Math.min(props.maxBackoffSeconds(), 1L << Math.min(attempt, 20));
        return jdbc.update("""
                UPDATE %s SET status = 'RETRY', attempt = :attempt, next_attempt_at = :next, lock_token = NULL, locked_until = NULL,
                       last_error_code = :err, updated_at = :now
                WHERE id = :id AND lock_token = :token""".formatted(stepTable),
                params().addValue("attempt", attempt).addValue("next", ts(now.plusSeconds(backoff))).addValue("err", errorCode)
                        .addValue("now", ts(now)).addValue("id", step.id()).addValue("token", lockToken)) == 1;
    }

    /** Celiski: insan karari gerekir. MANUAL_REVIEW cleanup'ta silinmez. */
    public boolean manualReview(Step step, UUID lockToken, String reasonCode) {
        return Boolean.TRUE.equals(requiresNew.execute(st -> {
            Instant now = clock.instant();
            int n = jdbc.update("""
                    UPDATE %s SET status = 'MANUAL_REVIEW', lock_token = NULL, locked_until = NULL, last_error_code = :err, updated_at = :now
                    WHERE id = :id AND lock_token = :token""".formatted(stepTable),
                    params().addValue("err", reasonCode).addValue("now", ts(now)).addValue("id", step.id()).addValue("token", lockToken));
            if (n == 0) return false;
            jdbc.update("UPDATE %s SET status = 'MANUAL_REVIEW', updated_at = :now WHERE id = :id".formatted(sagaTable),
                    params().addValue("now", ts(now)).addValue("id", step.sagaId()));
            return true;
        }));
    }

    // ---------- monitor / cleanup ----------

    /** warnAfter'dan eski cozulmemis saga sayisi (ERROR log + saga_unresolved_total metrigi icin). */
    public int countUnresolved() {
        Instant threshold = clock.instant().minus(Duration.ofMinutes(props.warnAfterMinutes()));
        return jdbc.queryForObject("""
                SELECT count(*) FROM %s WHERE status NOT IN ('CONFIRMED','COMPENSATED','MANUAL_REVIEW') AND created_at < :t"""
                .formatted(sagaTable), Map.of("t", ts(threshold)), Integer.class);
    }

    /** Yalniz terminal (CONFIRMED/COMPENSATED) kayitlar retention sonrasi silinir; MANUAL_REVIEW asla. Adimlar CASCADE. */
    public int cleanup() {
        Instant threshold = clock.instant().minus(Duration.ofDays(props.retentionDays()));
        return jdbc.update("DELETE FROM %s WHERE status IN ('CONFIRMED','COMPENSATED') AND updated_at < :t".formatted(sagaTable),
                Map.of("t", ts(threshold)));
    }

    // ---------- sorgular ----------

    public Optional<Saga> find(UUID accountId, String scope, UUID operationKey) {
        return Optional.ofNullable(DataAccessUtils.singleResult(jdbc.query(
                "SELECT * FROM %s WHERE account_id = :a AND scope = :s AND operation_key = :k".formatted(sagaTable),
                params().addValue("a", accountId).addValue("s", scope).addValue("k", operationKey), SAGA)));
    }

    public Optional<Saga> findById(UUID id) {
        return Optional.ofNullable(DataAccessUtils.singleResult(jdbc.query(
                "SELECT * FROM %s WHERE id = :id".formatted(sagaTable), Map.of("id", id), SAGA)));
    }

    public List<Step> stepsOf(UUID sagaId) {
        return jdbc.query("SELECT * FROM %s WHERE saga_id = :id ORDER BY created_at".formatted(stepTable), Map.of("id", sagaId), STEP);
    }

    public int countSagas() { return jdbc.queryForObject("SELECT count(*) FROM " + sagaTable, Map.of(), Integer.class); }
    public int countSteps() { return jdbc.queryForObject("SELECT count(*) FROM " + stepTable, Map.of(), Integer.class); }

    private static MapSqlParameterSource params() { return new MapSqlParameterSource(); }
    private static Timestamp ts(Instant i) { return Timestamp.from(i); }
    private static Instant inst(Timestamp t) { return t == null ? null : t.toInstant(); }

    static final RowMapper<Saga> SAGA = (ResultSet rs, int i) -> new Saga(
            rs.getObject("id", UUID.class), rs.getObject("account_id", UUID.class), rs.getString("scope"),
            rs.getObject("operation_key", UUID.class), SagaStatus.valueOf(rs.getString("status")), rs.getString("result"),
            inst(rs.getTimestamp("created_at")), inst(rs.getTimestamp("updated_at")));

    static final RowMapper<Step> STEP = (ResultSet rs, int i) -> new Step(
            rs.getObject("id", UUID.class), rs.getObject("saga_id", UUID.class), rs.getString("step_name"),
            Action.valueOf(rs.getString("next_action")), StepStatus.valueOf(rs.getString("status")), rs.getInt("attempt"),
            inst(rs.getTimestamp("next_attempt_at")), rs.getObject("lock_token", UUID.class),
            inst(rs.getTimestamp("locked_until")), rs.getString("last_error_code"));

}
