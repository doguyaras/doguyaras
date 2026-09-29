package com.acme.contract.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Istek DTO'su. Zorunluluk bean validation ile ifade edilir; springdoc ayni anotasyonlardan OpenAPI "required"
 * listesini uretir, yani sozlesme ile dogrulama tek kaynaktan gelir (elle yazilan spec kayar).
 * Yeni alan eklenecekse OPSIYONEL eklenir: zorunlu yeni alan eski istemciyi kirar (openapi-diff INCOMPATIBLE).
 */
public record CreateOrderRequest(
        @NotNull UUID customerId,
        @NotBlank @Size(max = 64) String sku,
        @NotNull @Min(1) @Max(1000) Integer quantity,
        @Size(max = 500) String note) {}
