package com.acme.order.config;

import com.acme.platform.messaging.outbox.OutboxRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class WebConfig {

    /** Test edilebilir zaman kaynagi: servisler Instant.now() yerine bu Clock'u kullanir. */
    @Bean
    public Clock clock() { return Clock.systemUTC(); }

    /** Generic outbox (platform-messaging); servis kendi poller'ini/tablosunu yazmaz. */
    @Bean
    public OutboxRepository outboxRepository(NamedParameterJdbcTemplate jdbc) { return new OutboxRepository(jdbc, "order"); }
}
