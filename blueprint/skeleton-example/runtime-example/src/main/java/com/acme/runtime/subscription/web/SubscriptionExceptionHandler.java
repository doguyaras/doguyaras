package com.acme.runtime.subscription.web;

import com.acme.platform.core.ErrorCode;
import com.acme.platform.core.ServiceException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** ServiceException -> zarfli hata (Bolum 6.2 bicimi: {ok:false, error:{code, message, service}}). */
@RestControllerAdvice
public class SubscriptionExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Map<String, Object>> handle(ServiceException e) {
        return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(envelope(e.getErrorCode()));
    }

    public static Map<String, Object> envelope(ErrorCode code) {
        return Map.of("ok", false, "error", Map.of("code", code.getCode(), "message", code.getMessage(), "service", code.getService()));
    }
}
