package com.acme.order.service;
import com.acme.order.api.dto.OrderStatusResponse;
import java.util.UUID;
public interface OrderStatusService {
    OrderStatusResponse status(UUID accountId, UUID orderId);
    void markShipped(UUID accountId, UUID orderId);
}
