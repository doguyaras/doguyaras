package com.acme.runtime.subscription;

import java.util.Arrays;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Katilimci servis (abonelik/kota). Saga koordinatoru OrderApp'in /internal cagrilarini servis JWT + allowlist +
 * delegasyon matrisiyle kabul eder (referans Bolum 9.2.1, 9.5, 11.4 "Katilimci sozlesmesi").
 *
 * Ayni modulde OrderApp de oldugu icin config dosyasi adi ayridir (subscription-app.yml): iki uygulama birbirinin
 * application.yml'ini okumaz. Paket taramasi yalniz com.acme.runtime.subscription altini kapsar.
 */
@SpringBootApplication
public class SubscriptionApp {

    public static final String CONFIG_NAME = "subscription-app";

    public static ConfigurableApplicationContext start(String... args) {
        return new SpringApplicationBuilder(SubscriptionApp.class).run(withConfigName(args));
    }

    public static void main(String[] args) { start(args); }

    static String[] withConfigName(String[] args) {
        return Stream.concat(Stream.of("--spring.config.name=" + CONFIG_NAME), Arrays.stream(args)).toArray(String[]::new);
    }
}
