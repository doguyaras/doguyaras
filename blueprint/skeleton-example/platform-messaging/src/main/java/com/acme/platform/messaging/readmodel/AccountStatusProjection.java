package com.acme.platform.messaging.readmodel;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * SNAPSHOT projeksiyon ornegi (sahibi auth, olay account.status.changed). Olay tam durumu tasir; bu yuzden UPSERT
 * yalniz EXCLUDED.source_revision > mevcut ise yazar: gec gelen eski karar yeni karari ezmez, tekrar teslim tek etkidir.
 */
public class AccountStatusProjection implements SnapshotProjection<AccountStatusProjection.AccountStatus> {

    public record AccountStatus(UUID accountId, boolean active, boolean legalOk, long sourceRevision, Instant sourceTime) { }

    public record Row(UUID accountId, boolean active, boolean legalOk, long sourceRevision, Instant sourceTime, Instant appliedAt) { }

    private final NamedParameterJdbcTemplate jdbc;
    private final String table;

    public AccountStatusProjection(NamedParameterJdbcTemplate jdbc, String schema) {
        this.jdbc = jdbc;
        this.table = "\"" + schema + "\".rm_account_status";
    }

    @Override
    public int upsert(AccountStatus s, Instant appliedAt) {
        return jdbc.update("""
                INSERT INTO %s (account_id, active, legal_ok, source_revision, source_time, applied_at)
                VALUES (:id, :active, :legal, :rev, :srcTime, :applied)
                ON CONFLICT (account_id) DO UPDATE SET active = EXCLUDED.active, legal_ok = EXCLUDED.legal_ok,
                    source_revision = EXCLUDED.source_revision, source_time = EXCLUDED.source_time, applied_at = EXCLUDED.applied_at
                WHERE EXCLUDED.source_revision > %s.source_revision""".formatted(table, table),
                new MapSqlParameterSource().addValue("id", s.accountId()).addValue("active", s.active())
                        .addValue("legal", s.legalOk()).addValue("rev", s.sourceRevision())
                        .addValue("srcTime", Timestamp.from(s.sourceTime())).addValue("applied", Timestamp.from(appliedAt)));
    }

    @Override
    public void clear() {
        jdbc.getJdbcTemplate().execute("TRUNCATE " + table);                    // PostgreSQL'de TRUNCATE transactional'dir
    }

    public List<Row> findAll() {
        return jdbc.query("SELECT * FROM %s ORDER BY account_id".formatted(table), (rs, i) -> new Row(
                rs.getObject("account_id", UUID.class), rs.getBoolean("active"), rs.getBoolean("legal_ok"),
                rs.getLong("source_revision"), rs.getTimestamp("source_time").toInstant(), rs.getTimestamp("applied_at").toInstant()));
    }

    public boolean exists(UUID accountId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM %s WHERE account_id = :id)".formatted(table),
                Map.of("id", accountId), Boolean.class));
    }
}
