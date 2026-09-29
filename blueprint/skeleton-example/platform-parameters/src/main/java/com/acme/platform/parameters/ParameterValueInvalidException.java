package com.acme.platform.parameters;

/** Deger var ama beklenen tipe/sinira uymuyor. Deger mesaja yazilmaz; yalniz grup, key ve beklenen tip. */
public class ParameterValueInvalidException extends ParameterException {
    public static final String CODE = "PARAMETER_VALUE_INVALID";

    public ParameterValueInvalidException(String group, String key, ParameterType expected, String reason) {
        super(CODE, 500, "parametre degeri gecersiz: " + group + "/" + key + " beklenen " + expected + " (" + reason + ")", null);
    }
}
