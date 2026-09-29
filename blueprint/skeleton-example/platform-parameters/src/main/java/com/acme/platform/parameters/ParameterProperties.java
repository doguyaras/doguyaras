package com.acme.platform.parameters;

import java.time.Duration;
import java.util.Map;

/**
 * Baslangic ayarlari (referans Bolum 1.4 sinifi): cacheTtl 5 sn instance cache; maxStaleness grup basina
 * (orn. hak limitleri 10 dk, yas/uygunluk kurallari 1 sa), tanimsiz gruplar icin defaultMaxStaleness.
 * Bunlar is parametresi degildir; yml'de durur.
 */
public record ParameterProperties(Duration cacheTtl, Duration defaultMaxStaleness, Map<String, Duration> maxStalenessByGroup) {
    public ParameterProperties {
        if (cacheTtl == null || cacheTtl.isNegative()) throw new IllegalArgumentException("cacheTtl");
        if (defaultMaxStaleness == null || defaultMaxStaleness.isNegative()) throw new IllegalArgumentException("defaultMaxStaleness");
        maxStalenessByGroup = maxStalenessByGroup == null ? Map.of() : Map.copyOf(maxStalenessByGroup);
    }

    public static ParameterProperties defaults() {
        return new ParameterProperties(Duration.ofSeconds(5), Duration.ofMinutes(10), Map.of());
    }

    public Duration maxStaleness(String group) {
        return maxStalenessByGroup.getOrDefault(group, defaultMaxStaleness);
    }
}
