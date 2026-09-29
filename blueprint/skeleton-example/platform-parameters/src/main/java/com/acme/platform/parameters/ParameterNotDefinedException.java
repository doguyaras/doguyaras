package com.acme.platform.parameters;

import java.util.List;

/** Key katalogda yok: deploy hatasi (kod ici default yasak). Acilis kontrolunde ve ilk okumada fail-closed. */
public class ParameterNotDefinedException extends ParameterException {
    public static final String CODE = "PARAMETER_NOT_DEFINED";

    public ParameterNotDefinedException(String group, String key) {
        super(CODE, 500, "parametre tanimsiz: " + group + "/" + key, null);
    }

    public ParameterNotDefinedException(List<String> missingKeys) {
        super(CODE, 500, "parametreler tanimsiz: " + missingKeys, null);
    }
}
