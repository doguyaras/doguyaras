package com.acme.platform.messaging.inbox;

import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Idempotent consumer (referans Bolum 11.3). Zorunlu guvence: inbox satiri ve is degisikligi AYNI transaction'da;
 * satir varsa (duplicate) is yapilmadan cikilir; is exception atarsa satir da geri alinir ve mesaj yeniden gelir.
 * Broker ack bu metodun DONUSUNDEN sonra (commit sonrasi) yapilir; AUTO ack ile bu guvence bozulur.
 * Dedup kapsami handler adidir: ayni olayi iki handler ayri ayri isler.
 */
public class InboxProcessor {

    public enum Outcome { APPLIED, DUPLICATE }

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final String table;

    public InboxProcessor(NamedParameterJdbcTemplate jdbc, TransactionTemplate tx, String schema) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.table = "\"" + schema + "\".inbox_event";
    }

    public Outcome process(String handler, UUID eventId, Runnable work) {
        return tx.execute(status -> {
            int inserted = jdbc.update(
                    "INSERT INTO %s (handler, event_id) VALUES (:h, :e) ON CONFLICT DO NOTHING".formatted(table),
                    Map.of("h", handler, "e", eventId));
            if (inserted == 0) return Outcome.DUPLICATE;      // daha once islendi; hicbir sey yapmadan cik (ack)
            work.run();                                         // is degisikligi ayni TX'te; exception -> rollback (satir dahil)
            return Outcome.APPLIED;
        });
    }
}
