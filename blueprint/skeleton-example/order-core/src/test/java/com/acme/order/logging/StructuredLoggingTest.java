package com.acme.order.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Structured logging kaniti (referans Bolum 8.1): logging.structured.format.console=ecs ile konsol satirlari
 * JSON'dur ve ECS alanlarini tasir (@timestamp, log.level, message, ecs.version). Uretimde bu property
 * config/order.yml'dedir; test acikca verir.
 *
 * TUZAK: Logback LoggingSystem JVM'de bir kez baslatilir; ayni surefire JVM'inde onceki bir Spring testi
 * duz metin formatla baslattiysa property'miz sessizce yok sayilir. Bu yuzden context yuklenmeden once
 * cleanUp() ile yeniden baslatma zorlanir; sonrasinda da geri alinir ki siradaki testler kendi ayarini kursun.
 */
@SpringBootTest(classes = StructuredLoggingTest.Empty.class, properties = "logging.structured.format.console=ecs")
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingTest {

    @Configuration(proxyBeanMethods = false)
    static class Empty {}

    @BeforeAll
    static void forceLoggingReinitialization() { LoggingSystem.get(StructuredLoggingTest.class.getClassLoader()).cleanUp(); }

    @AfterAll
    static void releaseLoggingSystem() { LoggingSystem.get(StructuredLoggingTest.class.getClassLoader()).cleanUp(); }

    @Test
    void consoleLinesAreEcsJson(CapturedOutput output) {
        String marker = "ecs-probe-" + UUID.randomUUID();
        LoggerFactory.getLogger("com.acme.order.EcsProbe").info("Structured probe: outcome=SUCCESS marker={}", marker);

        String line = output.getOut().lines().filter(l -> l.contains(marker)).findFirst()
                .orElseThrow(() -> new AssertionError("probe satiri konsolda yok; cikti:\n" + output.getOut()));
        assertThat(line).startsWith("{").endsWith("}");

        JsonNode json = JsonMapper.builder().build().readTree(line);
        assertThat(json.get("@timestamp").asString()).as("@timestamp").isNotBlank();
        assertThat(json.at("/log/level").asString()).as("log.level").isEqualTo("INFO");
        assertThat(json.at("/log/logger").asString()).isEqualTo("com.acme.order.EcsProbe");
        assertThat(json.get("message").asString()).contains("outcome=SUCCESS").contains(marker);
        assertThat(json.at("/ecs/version").asString()).as("ecs.version").isNotBlank();
        assertThat(json.at("/process/pid").isNumber()).isTrue();
    }
}
