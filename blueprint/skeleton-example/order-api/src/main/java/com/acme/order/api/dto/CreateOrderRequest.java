package com.acme.order.api.dto;
import jakarta.validation.constraints.NotBlank;
public record CreateOrderRequest(@NotBlank String productCode, int quantity) {}
