package com.acme.platform.parameters;

/** freshGroupSince: kaynak istenen revizyondan eski bir grup dondurdu; eski revizyonla kirpma yapilmaz (503). */
public class ParameterRevisionStaleException extends ParameterException {
    public static final String CODE = "PARAMETER_REVISION_STALE";

    public ParameterRevisionStaleException(String group, long required, long actual) {
        super(CODE, 503, "parametre revizyonu eski: " + group + " istenen>=" + required + " gelen=" + actual, null);
    }
}
