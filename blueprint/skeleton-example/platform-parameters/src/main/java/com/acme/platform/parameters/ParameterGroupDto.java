package com.acme.platform.parameters;

import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Yonetim servisinin /internal/parameters/groups/{group} yaniti: bir grubun TUM key'leri tek revizyonda.
 * Birlikte anlamli key'ler ayni revizyondan okunur; kalici sonuclara bu revizyon snapshot olarak yazilir.
 * Degerler ham JSON tasinir; tip dogrulamasi okuma aninda ParameterValues'ta yapilir (fail-closed).
 */
public record ParameterGroupDto(String group, long revision, Map<String, JsonNode> values, Criticality criticality) {
    public ParameterGroupDto {
        if (group == null || group.isBlank()) throw new IllegalArgumentException("group bos olamaz");
        if (criticality == null) throw new IllegalArgumentException("criticality bos olamaz: " + group);
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
