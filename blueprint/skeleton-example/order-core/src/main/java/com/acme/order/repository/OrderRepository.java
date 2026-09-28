package com.acme.order.repository;
import com.acme.order.entity.Order;
import java.util.Optional; import java.util.UUID;
import org.springframework.stereotype.Repository;
@Repository
public class OrderRepository { public Optional<Order> findById(UUID id) { return Optional.empty(); } }
