package com.acme.order.api.event;
import java.time.Instant;
import java.util.UUID;
/** order.order.cancelled olayi. */
public record OrderCancelledEvent(UUID orderId, UUID accountId, String phone, String reason, Instant cancelledAt) {}
