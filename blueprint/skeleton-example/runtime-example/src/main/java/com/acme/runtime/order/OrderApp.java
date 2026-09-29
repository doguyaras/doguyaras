package com.acme.runtime.order;

import java.util.Arrays;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Saga koordinatoru (referans Bolum 11.4): POST /v1/orders istek yolunda begin -> uzak consume -> domain + success;
 * recovery worker @Scheduled ile confirm/compensate eder. Katilimciya giden tek client HTTP + servis JWT +
 * circuit breaker + bulkhead ile korunur (Bolum 4.7, 6.8).
 *
 * Config dosyasi order-app.yml (ayni modulde SubscriptionApp de var; application.yml paylasilmaz).
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class OrderApp {

    public static final String CONFIG_NAME = "order-app";

    public static ConfigurableApplicationContext start(String... args) {
        return new SpringApplicationBuilder(OrderApp.class).run(withConfigName(args));
    }

    public static void main(String[] args) { start(args); }

    static String[] withConfigName(String[] args) {
        return Stream.concat(Stream.of("--spring.config.name=" + CONFIG_NAME), Arrays.stream(args)).toArray(String[]::new);
    }
}
