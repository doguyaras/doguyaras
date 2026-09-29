package com.acme.order.service;
import com.acme.order.api.dto.CreateOrderRequest;
import java.util.UUID;
public interface OrderService {
    UUID create(UUID accountId, UUID idempotencyKey, CreateOrderRequest req);
    void cancel(UUID accountId, UUID orderId);
    OrderSummary summary(UUID accountId, UUID orderId);
}
