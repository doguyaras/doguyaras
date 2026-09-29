package com.acme.platform.messaging.readmodel;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Tuketim konumu (rm_consumer_position) ve delta sira takibi (rm_delta_position). Konum GREATEST ile ilerler:
 * snapshot olaylari sirasiz gelebilir, konum hicbir zaman geri gitmez ve son durum teslim sirasindan bagimsiz olur.
 */
public class ConsumerPositionStore {

    public record Position(String source, long lastSeq, Instant lastEventTime, Instant updatedAt) { }

    private final NamedParameterJdbcTemplate jdbc;
    private final String position;
    private final String deltaPosition;

    public ConsumerPositionStore(NamedParameterJdbcTemplate jdbc, String schema) {
        this.jdbc = jdbc;
        this.position = "\"" + schema + "\".rm_consumer_position";
        this.deltaPosition = "\"" + schema + "\".rm_delta_position";
    }

    public void advance(String source, long streamSeq, Instant eventTime, Instant now) {
        jdbc.update("""
                INSERT INTO %s (source, last_seq, last_event_time, updated_at) VALUES (:s, :seq, :t, :now)
                ON CONFLICT (source) DO UPDATE SET
                    last_seq = GREATEST(%s.last_seq, EXCLUDED.last_seq),
                    last_event_time = GREATEST(%s.last_event_time, EXCLUDED.last_event_time),
                    updated_at = EXCLUDED.updated_at""".formatted(position, position, position),
                Map.of("s", source, "seq", streamSeq, "t", Timestamp.from(eventTime), "now", Timestamp.from(now)));
    }

    public Optional<Position> find(String source) {
        return jdbc.query("SELECT source, last_seq, last_event_time, updated_at FROM %s WHERE source = :s".formatted(position),
                Map.of("s", source), (rs, i) -> new Position(rs.getString("source"), rs.getLong("last_seq"),
                        rs.getTimestamp("last_event_time").toInstant(), rs.getTimestamp("updated_at").toInstant()))
                .stream().findFirst();
    }

    /**
     * Aggregate'in son uygulanan delta sirasi; satir yoksa 0 (ilk olay seq=1). Satir FOR UPDATE ile kilitlenir:
     * ayni aggregate'e iki tuketici ayni anda gelirse sira kontrolu serilesir (kontrol-et-sonra-yaz yarisini kapatir).
     */
    public long lockDeltaSeq(String source, UUID aggregateId) {
        jdbc.update("INSERT INTO %s (source, aggregate_id, last_seq) VALUES (:s, :a, 0) ON CONFLICT DO NOTHING".formatted(deltaPosition),
                Map.of("s", source, "a", aggregateId));
        return jdbc.queryForObject("SELECT last_seq FROM %s WHERE source = :s AND aggregate_id = :a FOR UPDATE".formatted(deltaPosition),
                Map.of("s", source, "a", aggregateId), Long.class);
    }

    public void setDeltaSeq(String source, UUID aggregateId, long seq) {
        jdbc.update("UPDATE %s SET last_seq = :seq WHERE source = :s AND aggregate_id = :a".formatted(deltaPosition),
                Map.of("s", source, "a", aggregateId, "seq", seq));
    }

    /** Rebuild: kaynagin konumu ve delta siralari sifirlanir; export yeniden uygulanirken konum yeniden kurulur. */
    public void reset(String source) {
        jdbc.update("DELETE FROM %s WHERE source = :s".formatted(deltaPosition), Map.of("s", source));
        jdbc.update("DELETE FROM %s WHERE source = :s".formatted(position), Map.of("s", source));
    }
}
