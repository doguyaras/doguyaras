package com.acme.contract;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Mevcut spec'ten programatik varyant turetir (kod degistirmeden "su DTO degisikligi olsaydi" sorusu). Her metod
 * yalniz components.schemas altindaki tek bir semayi degistirir; DTO her iki operasyonda da $ref ile kullanildigindan
 * fark ilgili tum uclarda gorunur.
 */
final class SpecVariants {

    private SpecVariants() {}

    static ObjectNode schema(ObjectNode spec, String name) {
        JsonNode node = spec.get("components").get("schemas").get(name);
        if (!(node instanceof ObjectNode schema)) throw new IllegalArgumentException("sema yok: " + name);
        return schema;
    }

    static ObjectNode properties(ObjectNode spec, String schemaName) {
        return (ObjectNode) schema(spec, schemaName).get("properties");
    }

    static void removeResponseProperty(ObjectNode spec, String schemaName, String property) {
        requirePresent(spec, schemaName, property);
        properties(spec, schemaName).remove(property);
        removeFromRequired(schema(spec, schemaName), property);
    }

    /** Yeniden adlandirma = eski adi kaldir + yeni adi ayni tanim ve ayni zorunlulukla ekle. */
    static void renameProperty(ObjectNode spec, String schemaName, String from, String to) {
        requirePresent(spec, schemaName, from);
        ObjectNode schema = schema(spec, schemaName);
        JsonNode definition = properties(spec, schemaName).remove(from);
        properties(spec, schemaName).set(to, definition);
        if (removeFromRequired(schema, from)) required(schema).add(to);
    }

    static void addOptionalProperty(ObjectNode spec, String schemaName, String property, String type) {
        // Var olan alanin ustune yazmak sessizce "degisiklik yok" varyanti uretir; test yanlis sebeple gecer/kalir
        if (properties(spec, schemaName).has(property))
            throw new IllegalArgumentException(schemaName + "." + property + " zaten var; varyant anlamsiz olur");
        properties(spec, schemaName).putObject(property).put("type", type);
    }

    static void addRequiredProperty(ObjectNode spec, String schemaName, String property, String type) {
        addOptionalProperty(spec, schemaName, property, type);
        required(schema(spec, schemaName)).add(property);
    }

    static void makeRequired(ObjectNode spec, String schemaName, String property) {
        requirePresent(spec, schemaName, property);
        ArrayNode required = required(schema(spec, schemaName));
        for (JsonNode existing : required)
            if (property.equals(existing.asString()))
                throw new IllegalArgumentException(schemaName + "." + property + " zaten zorunlu; varyant anlamsiz olur");
        required.add(property);
    }

    static void addEnumValue(ObjectNode spec, String schemaName, String property, String value) {
        ((ArrayNode) properties(spec, schemaName).get(property).get("enum")).add(value);
    }

    private static void requirePresent(ObjectNode spec, String schemaName, String property) {
        if (!properties(spec, schemaName).has(property))
            throw new IllegalArgumentException(schemaName + "." + property + " yok; varyant anlamsiz olur");
    }

    private static ArrayNode required(ObjectNode schema) {
        return schema.has("required") ? (ArrayNode) schema.get("required") : schema.putArray("required");
    }

    private static boolean removeFromRequired(ObjectNode schema, String property) {
        if (!schema.has("required")) return false;
        ArrayNode required = (ArrayNode) schema.get("required");
        for (int i = 0; i < required.size(); i++) {
            if (property.equals(required.get(i).asString())) {
                required.remove(i);
                return true;
            }
        }
        return false;
    }
}
