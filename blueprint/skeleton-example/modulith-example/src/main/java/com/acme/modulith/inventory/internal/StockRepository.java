package com.acme.modulith.inventory.internal;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.OptionalInt;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Inventory'nin ic deposu (inventory semasi). Modul disindan erisim yasak: order bu sinifa
 * dokunursa ApplicationModules.verify() "non-exposed type" ihlali verir.
 */
@Repository
public class StockRepository {

    private final JdbcClient jdbc;

    public StockRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Rezervasyonu siparis basina BIR kez kaydeder. PK (order_id) + ON CONFLICT DO NOTHING tekrar
     * teslimde false doner; dinleyici o zaman stok dusmez. Ayni siparisin iki teslimi es zamanliysa
     * ikinci INSERT birincinin commit'ini bekler, sonra 0 satir doner (check-then-act yarisi yok).
     */
    public boolean recordReservationOnce(UUID orderId, String sku, int quantity, Instant reservedAt) {
        return jdbc.sql("""
                INSERT INTO inventory.reservation (order_id, sku, quantity, reserved_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (order_id) DO NOTHING
                """)
                .params(orderId, sku, quantity, Timestamp.from(reservedAt))
                .update() == 1;
    }

    /** Stok satiri yoksa false (tanimlanmamis urun). Yetersiz stok CHECK (available >= 0) ile reddedilir. */
    public boolean decrement(String sku, int quantity) {
        return jdbc.sql("UPDATE inventory.stock SET available = available - ? WHERE sku = ?")
                .params(quantity, sku)
                .update() == 1;
    }

    public void upsertStock(String sku, int available) {
        jdbc.sql("""
                INSERT INTO inventory.stock (sku, available) VALUES (?, ?)
                ON CONFLICT (sku) DO UPDATE SET available = EXCLUDED.available
                """)
                .params(sku, available)
                .update();
    }

    public OptionalInt available(String sku) {
        return jdbc.sql("SELECT available FROM inventory.stock WHERE sku = ?")
                .param(sku).query(Integer.class).optional()
                .map(OptionalInt::of).orElseGet(OptionalInt::empty);
    }

    public int reservationCount(UUID orderId) {
        return jdbc.sql("SELECT count(*) FROM inventory.reservation WHERE order_id = ?")
                .param(orderId).query(Integer.class).single();
    }
}
