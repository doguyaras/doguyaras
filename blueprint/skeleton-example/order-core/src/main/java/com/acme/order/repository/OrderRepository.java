package com.acme.order.repository;
import com.acme.order.entity.Order;
import java.util.List; import java.util.Optional; import java.util.UUID;
import org.springframework.stereotype.Repository;
/**
 * Iskelet repository'si (gercek projede Spring Data JPA / JdbcClient). Metot sozlesmeleri gercek olanlarla aynidir:
 * updateStatus etkilenen satir sayisini doner (idempotent; worker retry'i buna dayanir).
 */
@Repository
public class OrderRepository {
    public Optional<Order> findById(UUID id) { return Optional.empty(); }
    public Order save(Order order) { return order; }
    public int updateStatus(UUID id, String status) { return 0; }
    public List<Order> findAll() { return List.of(); }
}
