package com.acme.order.api.dto;
import java.time.Instant;
import java.util.UUID;
/** Siparis durumu (istemciye donen DTO; entity disari acilmaz). */
public record OrderStatusResponse(UUID orderId, String status, Instant updatedAt) {}
