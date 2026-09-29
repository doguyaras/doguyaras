package com.acme.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Spring Framework 7 yerlesik dayaniklilik: @Retryable ve @ConcurrencyLimit (semaphore bulkhead) icin
 * proxy post-processor'lari (referans Bolum 4.7). Retry senkron HTTP yolunda kullanilmaz; worker'larda,
 * idempotent ve gecici hatalarda kullanilir.
 */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
public class ResilienceConfig {}
