package com.acme.platform.parameters;

/** Servisin bagimli oldugu bir key: acilis kontrolu bu listeden gecer. Servisin SystemParameterKey enum'undan uretilir. */
public record RequiredParameter(String group, String key, ParameterType type) {}
