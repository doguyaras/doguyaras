package com.acme.order.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Zaman tek kaynaktan: testler sabit/ilerletilebilir Clock verir, uretim UTC sistem saati. */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {
    @Bean
    Clock clock() { return Clock.systemUTC(); }
}
