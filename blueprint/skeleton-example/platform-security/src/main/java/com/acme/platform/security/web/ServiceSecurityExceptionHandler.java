package com.acme.platform.security.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** ServiceSecurityException -> {code, message}. Servisin kendi GlobalServiceExceptionHandler'i ile yan yana calisir. */
@RestControllerAdvice
public class ServiceSecurityExceptionHandler {

    @ExceptionHandler(ServiceSecurityException.class)
    public ResponseEntity<ErrorResponse> handle(ServiceSecurityException e) {
        return ResponseEntity.status(e.status()).header("Cache-Control", "no-store").body(e.toResponse());
    }
}
