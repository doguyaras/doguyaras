package com.acme.order.exception;

import org.springframework.http.HttpStatus;

/**
 * Ortak (servis bagimsiz) hata kodlari: validation blogu 90000-90099, system blogu 99998-99999 (referans Bolum 7.2).
 * Global handler standart MVC hatalarini bu kodlara esler (Bolum 7.3). Bu enum kavramsal olarak ortak kutuphaneye
 * (platform-core) aittir; bu sprintte modul siniri disina cikilmadigi icin order-core'da tanimlidir ve tasinirken
 * kodlar/servis adlari degismez (ErrorCodeUniquenessTest bloklari korur).
 */
public enum CommonErrorCode implements com.acme.platform.core.ErrorCode {
    // --- validation: bean validation, bind, malformed body, type mismatch, eksik parametre/header (90000-90009)
    VALIDATION(90000, "validation", "Request validation failed.", HttpStatus.BAD_REQUEST),
    REQUEST_NOT_READABLE(90001, "validation", "Request body could not be read.", HttpStatus.BAD_REQUEST),
    TYPE_MISMATCH(90002, "validation", "Request parameter has an invalid type.", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER(90003, "validation", "Required request parameter is missing.", HttpStatus.BAD_REQUEST),
    // --- istek sekli / route (90010-90019)
    NOT_FOUND(90010, "validation", "Resource not found.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(90011, "validation", "HTTP method not allowed.", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(90012, "validation", "Unsupported media type.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    NOT_ACCEPTABLE(90013, "validation", "Requested media type is not acceptable.", HttpStatus.NOT_ACCEPTABLE),
    PAYLOAD_TOO_LARGE(90014, "validation", "Request payload is too large.", HttpStatus.CONTENT_TOO_LARGE),
    // --- API versiyonlama (90020-90029): eksik/desteklenmeyen API-Version (referans Bolum 20)
    API_VERSION_INVALID(90020, "validation", "API version is missing or not supported.", HttpStatus.BAD_REQUEST),
    // --- system
    UPSTREAM_ERROR(99998, "system", "Upstream service failed.", HttpStatus.BAD_GATEWAY),
    INTERNAL_ERROR(99999, "system", "Unexpected error.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code; private final String service; private final String message; private final HttpStatus httpStatus;
    CommonErrorCode(int code, String service, String message, HttpStatus httpStatus) {
        this.code = code; this.service = service; this.message = message; this.httpStatus = httpStatus;
    }
    public int getCode() { return code; } public String getMessage() { return message; }
    public String getService() { return service; } public HttpStatus getHttpStatus() { return httpStatus; }
}
