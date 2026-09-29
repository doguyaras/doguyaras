package com.acme.platform.security.jwt;

/**
 * Token reddi. Mesaj yalniz log icindir (hangi kontrolun dustugu); istemciye tek tip "SERVICE_TOKEN_INVALID" doner
 * ki saldirgan hangi adimi gectigini ogrenemesin.
 */
public final class ServiceTokenInvalidException extends RuntimeException {
    public ServiceTokenInvalidException(String reason) { super(reason); }
    public ServiceTokenInvalidException(String reason, Throwable cause) { super(reason, cause); }
}
