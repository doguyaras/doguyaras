package com.acme.platform.parameters;

import java.util.Map;

/** OPTION_LIST elemani (referans Bolum 14): code, labels.tr/en, order, active. */
public record ParameterOption(String code, Map<String, String> labels, int order, boolean active) {}
