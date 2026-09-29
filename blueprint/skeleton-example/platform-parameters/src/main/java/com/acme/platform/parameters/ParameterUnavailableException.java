package com.acme.platform.parameters;

/**
 * 503: kaynak erisilemez ve fallback'e izin yok (freshGroup) ya da guvenlik-kritik grup maxStaleness'i asti
 * ya da hicbir yerde (bellek/disk) son bilinen deger yok.
 */
public class ParameterUnavailableException extends ParameterException {
    public static final String CODE = "PARAMETER_UNAVAILABLE";

    public ParameterUnavailableException(String group, String reason, Throwable cause) {
        super(CODE, 503, "parametre grubu erisilemez: " + group + " (" + reason + ")", cause);
    }
}
