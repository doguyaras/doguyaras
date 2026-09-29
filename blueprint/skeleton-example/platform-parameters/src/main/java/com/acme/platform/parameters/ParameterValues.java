package com.acme.platform.parameters;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Tek revizyondan tipli okuma. Fail-closed: key yoksa PARAMETER_NOT_DEFINED, deger tipe uymuyorsa
 * PARAMETER_VALUE_INVALID. Kod ici default deger YOKTUR; "yoksa su olsun" ihtiyaci katalogda karsilanir.
 * revision() kalici sonuclarla birlikte yazilir (hangi kuralla karar verildigi sonradan izlenebilir).
 */
public final class ParameterValues {
    private final ParameterGroupDto dto;

    ParameterValues(ParameterGroupDto dto) { this.dto = dto; }

    public String group() { return dto.group(); }
    public long revision() { return dto.revision(); }
    public Criticality criticality() { return dto.criticality(); }
    public Set<String> keys() { return dto.values().keySet(); }
    public boolean has(String key) { return dto.values().containsKey(key); }

    /** INTEGER: JSON tam sayi (int araligi). Ondalik, metin, null gecersizdir. */
    public int integer(String key) {
        JsonNode n = required(key);
        if (!n.isIntegralNumber() || !n.canConvertToInt()) {
            throw new ParameterValueInvalidException(dto.group(), key, ParameterType.INTEGER, "tam sayi degil");
        }
        return n.intValue();
    }

    /** DURATION: her zaman saniye cinsinden negatif olmayan tam sayi (referans Bolum 14 tip kurali). */
    public Duration duration(String key) {
        JsonNode n = required(key);
        if (!n.isIntegralNumber() || !n.canConvertToLong() || n.longValue() < 0) {
            throw new ParameterValueInvalidException(dto.group(), key, ParameterType.DURATION, "negatif olmayan saniye degil");
        }
        return Duration.ofSeconds(n.longValue());
    }

    /** OPTION_LIST: {code, labels{tr,en}, order, active} dizisi; order'a gore sirali doner, code tekil olmali. */
    public List<ParameterOption> optionList(String key) {
        JsonNode n = required(key);
        if (!n.isArray()) throw invalidOption(key, "dizi degil");
        List<ParameterOption> out = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        for (JsonNode item : n) {
            if (!item.isObject()) throw invalidOption(key, "eleman nesne degil");
            JsonNode code = item.get("code");
            JsonNode labels = item.get("labels");
            JsonNode order = item.get("order");
            JsonNode active = item.get("active");
            if (code == null || !code.isString() || code.stringValue().isBlank()
                    || labels == null || !labels.isObject()
                    || order == null || !order.isIntegralNumber() || !order.canConvertToInt()
                    || active == null || !active.isBoolean()) {
                throw invalidOption(key, "eleman sekli bozuk");
            }
            if (!codes.add(code.stringValue())) throw invalidOption(key, "code tekrarli");
            Map<String, String> labelMap = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> e : labels.properties()) {
                if (!e.getValue().isString()) throw invalidOption(key, "label metin degil");
                labelMap.put(e.getKey(), e.getValue().stringValue());
            }
            out.add(new ParameterOption(code.stringValue(), Map.copyOf(labelMap), order.intValue(), active.booleanValue()));
        }
        out.sort(Comparator.comparingInt(ParameterOption::order));
        return List.copyOf(out);
    }

    /** Beklenen tipe gore dogrular; acilis kontrolu tum key'leri bununla gecer. */
    public void validate(String key, ParameterType type) {
        switch (type) {
            case INTEGER -> integer(key);
            case DURATION -> duration(key);
            case OPTION_LIST -> optionList(key);
        }
    }

    private JsonNode required(String key) {
        JsonNode n = dto.values().get(key);
        if (n == null || n.isNull() || n.isMissingNode()) throw new ParameterNotDefinedException(dto.group(), key);
        return n;
    }

    private ParameterValueInvalidException invalidOption(String key, String reason) {
        return new ParameterValueInvalidException(dto.group(), key, ParameterType.OPTION_LIST, reason);
    }
}
