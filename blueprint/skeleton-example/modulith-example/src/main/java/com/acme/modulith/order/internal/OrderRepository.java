package com.acme.modulith.order.internal;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Order modulunun ic deposu. Tablo order modulunun semasindadir (ordering); baska modul bu sinifa
 * ya da tabloya erisemez. Public olmasi yalniz order'in kendi paketleri arasi erisim icindir.
 */
@Repository
public class OrderRepository {

    private final JdbcClient jdbc;

    public OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID orderId, String sku, int quantity, Instant placedAt) {
        jdbc.sql("INSERT INTO ordering.orders (id, sku, quantity, placed_at) VALUES (?, ?, ?, ?)")
                .params(orderId, sku, quantity, Timestamp.from(placedAt))
                .update();
    }

    public boolean exists(UUID orderId) {
        return jdbc.sql("SELECT count(*) FROM ordering.orders WHERE id = ?")
                .param(orderId).query(Long.class).single() == 1L;
    }
}
