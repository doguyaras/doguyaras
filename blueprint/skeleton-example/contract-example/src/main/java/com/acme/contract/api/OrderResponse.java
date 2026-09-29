package com.acme.contract.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Yanit DTO'su. Her zaman dolu alanlar REQUIRED isaretlenir: uretilen istemci (dart-dio, typescript-fetch) bunlari
 * null olamaz tipe cevirir. Bir yanit alanini kaldirmak ya da yeniden adlandirmak istemcinin okudugu alani yok eder;
 * bu yuzden kiricidir ve yeni versiyon (/v2) ister (referans Bolum 20 API versiyonlama).
 */
public record OrderResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID customerId,
        @Schema(requiredMode = RequiredMode.REQUIRED) String sku,
        @Schema(requiredMode = RequiredMode.REQUIRED) int quantity,
        @Schema(requiredMode = RequiredMode.REQUIRED) OrderStatus status,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt,
        String note) {}
