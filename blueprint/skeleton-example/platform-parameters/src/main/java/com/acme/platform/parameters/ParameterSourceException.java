package com.acme.platform.parameters;

/** Kaynak erisilemedi (baglanti, timeout, 5xx). Provider bunu bounded-staleness kurallariyla ele alir. */
public class ParameterSourceException extends RuntimeException {
    public ParameterSourceException(String message) { super(message); }
    public ParameterSourceException(String message, Throwable cause) { super(message, cause); }
}
