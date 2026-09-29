package com.acme.modulith;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Sekil A (Bolum 1.1): tek deploy birimi, icinde Spring Modulith modulleri.
 * Bu paketin dogrudan alt paketleri (order, inventory) birer uygulama moduludur; bir modulun
 * {@code internal} alt paketi baska modulden gorulemez ({@code ApplicationModules.verify()} kirar).
 */
@SpringBootApplication
// @ApplicationModuleListener @Async tasir: bu olmadan dinleyici yayinlayan thread'de, commit sonrasi
// senkron calisir ve yavas bir modul siparis cevabini geciktirir.
@EnableAsync
public class ModulithApp {

    public static void main(String[] args) {
        SpringApplication.run(ModulithApp.class, args);
    }

    // Zaman her yerde enjekte edilen Clock'tan okunur: event registry de (publication_date, completion_date,
    // "su kadar eski" yeniden gonderim) ayni Clock bean'ini kullanir, bu yuzden testte deterministiktir.
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
