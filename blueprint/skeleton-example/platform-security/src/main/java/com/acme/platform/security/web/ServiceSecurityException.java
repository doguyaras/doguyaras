package com.acme.platform.security.web;

import org.springframework.http.HttpStatus;

/** Interceptor ve argument resolver katmanindan atilan, ServiceSecurityExceptionHandler'in zarfa cevirdigi hata. */
public final class ServiceSecurityException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ServiceSecurityException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public ErrorResponse toResponse() { return new ErrorResponse(code, getMessage()); }
}
