package com.acme.runtime.subscription.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Tek zaman kaynagi: JWT dogrulama (exp/nbf) ve kayit zaman damgalari ayni Clock'u kullanir. */
@Configuration(proxyBeanMethods = false)
public class SubscriptionConfig {

    @Bean
    public Clock clock() { return Clock.systemUTC(); }
}
