package com.acme.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

/**
 * Local yml ile deploy config'i arasindaki drift'i yakalar:
 *  1) Guvenlik ve baglanti key'leri (internal-access, rate-limit scope'lari, client base-url'leri)
 *     application-local.yml ve config/<svc>.yml'de AYNI kumeyi olusturmali.
 *  2) config/*.yml icinde kullanilan her ${ENV_VAR} placeholder'i deploy env sablonunda tanimli olmali.
 *  3) Secret key'lerinde literal fallback (${X:deger}) bulunmamali.
 *
 * Yollar modul kokune goredir; deploy sablonu repo kokundedir (../../deploy/prod.env.example).
 * Referans: mikroservis-mimari-referans.md Bolum 15.2, 19.5.
 */
class ConfigDriftTest {

    private static final Path LOCAL = Path.of("src/main/resources/application-local.yml");
    private static final Path SERVICE = Path.of("src/main/resources/config/order.yml");
    private static final Path ENV_TEMPLATE = Path.of("../../deploy/prod.env.example");

    /** Iki dosyada da birebir ayni olmasi gereken key prefix'leri. */
    private static final Set<String> MIRRORED_PREFIXES = Set.of(
            "service-jwt.internal-access",
            "rate-limit.rules",
            "spring.http.serviceclient",
            "services.");

    /** Fallback yasak olan secret key parcalari. */
    private static final Pattern SECRET_KEY = Pattern.compile("(secret|password|pass|token|key|credential)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z0-9_]+)(:[^}]*)?}");

    @Test
    void mirroredKeysAreIdenticalBetweenLocalAndDeployConfig() {
        Properties local = load(LOCAL);
        Properties service = load(SERVICE);
        for (String prefix : MIRRORED_PREFIXES) {
            Set<String> l = keysWithPrefix(local, prefix);
            Set<String> s = keysWithPrefix(service, prefix);
            assertThat(l).as("prefix '%s': application-local.yml ↔ config/order.yml key kumesi", prefix)
                    .containsExactlyInAnyOrderElementsOf(s);
        }
    }

    @Test
    void everyPlaceholderInServiceConfigExistsInDeployEnvTemplate() throws IOException {
        Set<String> declared = Files.readAllLines(ENV_TEMPLATE).stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("#") && l.contains("="))
                .map(l -> l.substring(0, l.indexOf('=')).trim())
                .collect(Collectors.toSet());
        Set<String> used = placeholders(Files.readString(SERVICE));
        // /run/secrets ile gelen degerler env degil, config tree'dir; onlar env sablonunda aranmaz.
        used.removeIf(v -> v.startsWith("SECRET_"));
        assertThat(declared).as("deploy env sablonunda eksik degiskenler").containsAll(used);
    }

    @Test
    void secretKeysHaveNoLiteralFallback() {
        Properties service = load(SERVICE);
        Set<String> offenders = new TreeSet<>();
        for (String key : service.stringPropertyNames()) {
            if (!SECRET_KEY.matcher(key).find()) continue;
            Matcher m = PLACEHOLDER.matcher(service.getProperty(key));
            while (m.find()) {
                if (m.group(2) != null) offenders.add(key + " = " + m.group());
            }
        }
        assertThat(offenders).as("secret key'lerinde literal fallback yasak (fail-fast)").isEmpty();
    }

    private static Properties load(Path p) {
        YamlPropertiesFactoryBean f = new YamlPropertiesFactoryBean();
        f.setResources(new FileSystemResource(p));
        Properties props = f.getObject();
        assertThat(props).as("yml okunamadi: %s", p).isNotNull();
        return props;
    }

    private static Set<String> keysWithPrefix(Properties props, String prefix) {
        return props.stringPropertyNames().stream()
                .filter(k -> k.startsWith(prefix))
                // internal-access[0].path gibi listeler icin sirayi degil kumeyi karsilastir
                .map(k -> k + "=" + props.getProperty(k).replaceAll("\\s+", ""))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) out.add(m.group(1));
        return out;
    }
}
