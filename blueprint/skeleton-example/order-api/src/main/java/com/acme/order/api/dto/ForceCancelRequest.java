package com.acme.order.api.dto;
import java.util.UUID;
/** Backoffice'in zorla iptal istegi. */
public record ForceCancelRequest(UUID accountId, String reason) {}
