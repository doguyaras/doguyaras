package com.acme.runtime.order.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Katilimci client'i (clients.subscription.*). Timeout'lar ACIK yazilir: varsayilan (sonsuz/uzun) read timeout
 * circuit breaker ve bulkhead'i anlamsizlastirir (Bolum 4.7 "once her hop'ta sert timeout").
 */
@Validated
@ConfigurationProperties("clients.subscription")
public record SubscriptionClientProperties(
        @NotBlank String baseUrl,
        @DefaultValue("subscription-api") String audience,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {}
