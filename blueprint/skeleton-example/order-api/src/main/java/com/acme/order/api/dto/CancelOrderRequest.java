package com.acme.order.api.dto;
import java.util.UUID;
/** Siparis iptal istegi. */
public record CancelOrderRequest(UUID accountId, String reason, String phone) {}
