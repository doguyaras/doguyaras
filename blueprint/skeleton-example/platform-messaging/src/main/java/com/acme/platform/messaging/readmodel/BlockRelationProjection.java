package com.acme.platform.messaging.readmodel;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * DELTA projeksiyon ornegi (sahibi user, olaylar user.block.created / user.block.removed). Her olay tek bir
 * degisikliktir; onceki durumu kapsamaz. Bu yuzden hicbir olay atlanamaz: sira kontrolu ReadModelApplier'dadir,
 * bu sinif yalniz dogrulanmis olayi uygular.
 */
public class BlockRelationProjection implements DeltaProjection<BlockRelationProjection.BlockOp> {

    public enum Kind { CREATED, REMOVED }

    public record BlockOp(UUID blockedId, Kind kind) { }

    public record Row(UUID blockerId, UUID blockedId, long sourceSeq) { }

    private final NamedParameterJdbcTemplate jdbc;
    private final String table;

    public BlockRelationProjection(NamedParameterJdbcTemplate jdbc, String schema) {
        this.jdbc = jdbc;
        this.table = "\"" + schema + "\".rm_block_relation";
    }

    @Override
    public void apply(DeltaEvent<BlockOp> e) {
        Map<String, Object> p = Map.of("blocker", e.aggregateId(), "blocked", e.op().blockedId(), "seq", e.aggregateSeq());
        switch (e.op().kind()) {
            case CREATED -> jdbc.update("""
                    INSERT INTO %s (blocker_id, blocked_id, source_seq) VALUES (:blocker, :blocked, :seq)
                    ON CONFLICT (blocker_id, blocked_id) DO UPDATE SET source_seq = EXCLUDED.source_seq""".formatted(table), p);
            case REMOVED -> jdbc.update("DELETE FROM %s WHERE blocker_id = :blocker AND blocked_id = :blocked".formatted(table), p);
        }
    }

    @Override
    public void clear() {
        jdbc.getJdbcTemplate().execute("TRUNCATE " + table);
    }

    public List<Row> findByBlocker(UUID blockerId) {
        return jdbc.query("SELECT * FROM %s WHERE blocker_id = :b ORDER BY blocked_id".formatted(table), Map.of("b", blockerId),
                (rs, i) -> new Row(rs.getObject("blocker_id", UUID.class), rs.getObject("blocked_id", UUID.class), rs.getLong("source_seq")));
    }
}
