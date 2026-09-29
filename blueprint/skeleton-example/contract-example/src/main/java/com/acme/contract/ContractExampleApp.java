package com.acme.contract;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// Spec basligi/surumu istemci paketinin adini ve surumunu belirler; springdoc'un "OpenAPI definition / v0"
// varsayilani birlestirilmis <proje>-api.yaml'da servisleri ayirt edilemez kilar (referans Bolum 20).
@OpenAPIDefinition(info = @Info(title = "contract-example", version = "v1"))
@SpringBootApplication
public class ContractExampleApp {
    public static void main(String[] args) { SpringApplication.run(ContractExampleApp.class, args); }

    /** Saat enjekte edilir: createdAt testlerde sabit, yanit ornekleri deterministik. */
    @Bean
    Clock clock() { return Clock.systemUTC(); }
}
