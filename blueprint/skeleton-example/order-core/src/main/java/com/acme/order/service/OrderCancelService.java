package com.acme.order.service;
import java.util.UUID;
public interface OrderCancelService {
    void cancel(UUID accountId, UUID orderId, String reason, String phone);
}
