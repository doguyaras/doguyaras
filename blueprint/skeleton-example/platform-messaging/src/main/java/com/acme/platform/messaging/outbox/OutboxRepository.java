package com.acme.platform.messaging.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generic outbox erisimi (referans Bolum 23.3). Sema adi parametre: her servis kendi semasindaki tabloyu kullanir.
 * JDBC ile yazilmistir; JPA versiyonu ayni sorgulari @Query(nativeQuery=true) ile tasir.
 */
public class OutboxRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final String table;

    public OutboxRepository(NamedParameterJdbcTemplate jdbc, String schema) {
        this.jdbc = jdbc;
        this.table = "\"" + schema + "\".outbox_event";
    }

    /**
     * Domain TX'i icinde cagrilir (Propagation.MANDATORY): outbox satiri domain yazimiyla birlikte commit/rollback olur.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(OutboxEvent e) {
        jdbc.update("""
                INSERT INTO %s (id, kind, aggregate_type, aggregate_id, event_type, payload, headers, priority, dead_policy,
                                next_retry_at, created_at)
                VALUES (:id, :kind, :aggType, :aggId, :eventType, CAST(:payload AS jsonb), CAST(:headers AS jsonb),
                        :priority, :deadPolicy, :nextRetryAt, :createdAt)
                """.formatted(table),
                new MapSqlParameterSource()
                        .addValue("id", e.id()).addValue("kind", e.kind())
                        .addValue("aggType", e.aggregateType()).addValue("aggId", e.aggregateId())
                        .addValue("eventType", e.eventType()).addValue("payload", e.payload())
                        .addValue("headers", e.headers() == null ? "{}" : e.headers())
                        .addValue("priority", e.priority()).addValue("deadPolicy", e.deadPolicy().name())
                        .addValue("nextRetryAt", Timestamp.from(e.nextRetryAt() == null ? e.createdAt() : e.nextRetryAt()))
                        .addValue("createdAt", Timestamp.from(e.createdAt())));
    }

    /**
     * Birden fazla instance ayni satiri alamaz: SKIP LOCKED kilitli satirlari atlar, locked_until kira suresidir.
     * Kirasi dolan PUBLISHING satiri (instance cokmesi) yeniden claim edilir. Lane = kind; oncelik yuksek olan once.
     * Sira: ayni aggregate_id'nin daha eski, henuz bitmemis (PENDING/PUBLISHING) satiri varsa bu satir atlanir
     * (NOT EXISTS) — sira uretici tarafinda korunur; ilk satir basarisiz olursa ardillari kendiliginden bekler.
     */
    public List<OutboxEvent> claim(String kind, Instant now, Instant lockedUntil, UUID claimToken, int limit) {
        return jdbc.query("""
                WITH candidates AS (
                    SELECT o.id FROM %1$s o
                    WHERE o.kind = :kind
                      AND (o.status = 'PENDING' OR (o.status = 'PUBLISHING' AND o.locked_until <= :now))
                      AND o.next_retry_at <= :now
                      AND NOT EXISTS (SELECT 1 FROM %1$s p
                                      WHERE p.aggregate_id = o.aggregate_id
                                        AND (p.created_at, p.id) < (o.created_at, o.id)
                                        AND p.status IN ('PENDING','PUBLISHING'))
                    ORDER BY o.priority DESC, o.created_at, o.id
                    FOR UPDATE OF o SKIP LOCKED
                    LIMIT :limit)
                UPDATE %1$s o
                SET status = 'PUBLISHING', locked_until = :lockedUntil, claim_token = :claimToken
                FROM candidates WHERE o.id = candidates.id
                RETURNING o.*
                """.formatted(table),
                Map.of("kind", kind, "now", Timestamp.from(now), "lockedUntil", Timestamp.from(lockedUntil),
                        "claimToken", claimToken, "limit", limit),
                MAPPER);
    }

    /** Sonucu yalniz claim sahibi yazar; kirasi elinden alinmis eski worker satiri ezemez. */
    public int deleteProcessed(UUID id, UUID claimToken) {
        return jdbc.update("DELETE FROM %s WHERE id = :id AND claim_token = :token".formatted(table),
                Map.of("id", id, "token", claimToken));
    }

    /** PENDING (retry) veya DEAD'e birak; yine yalniz claim sahibi. */
    public int release(UUID id, UUID claimToken, String status, int retryCount, Instant nextRetryAt, String errorCode) {
        return jdbc.update("""
                UPDATE %s SET status = :status, retry_count = :retry, next_retry_at = :next,
                       locked_until = NULL, claim_token = NULL, last_error_code = :err
                WHERE id = :id AND claim_token = :token
                """.formatted(table),
                new MapSqlParameterSource().addValue("status", status).addValue("retry", retryCount)
                        .addValue("next", Timestamp.from(nextRetryAt)).addValue("err", errorCode)
                        .addValue("id", id).addValue("token", claimToken));
    }

    public List<OutboxEvent> findAll() {
        return jdbc.query("SELECT * FROM %s ORDER BY created_at, id".formatted(table), Map.of(), MAPPER);
    }

    static final RowMapper<OutboxEvent> MAPPER = new RowMapper<>() {
        @Override public OutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new OutboxEvent(
                    rs.getObject("id", UUID.class), rs.getString("kind"), rs.getString("aggregate_type"),
                    rs.getObject("aggregate_id", UUID.class), rs.getString("event_type"), rs.getString("payload"),
                    rs.getString("headers"), rs.getString("status"), rs.getShort("priority"),
                    OutboxEvent.DeadPolicy.valueOf(rs.getString("dead_policy")), rs.getInt("retry_count"),
                    instant(rs.getTimestamp("next_retry_at")), instant(rs.getTimestamp("locked_until")),
                    rs.getObject("claim_token", UUID.class), rs.getString("last_error_code"),
                    instant(rs.getTimestamp("created_at")));
        }
        private Instant instant(Timestamp t) { return t == null ? null : t.toInstant(); }
    };
}
