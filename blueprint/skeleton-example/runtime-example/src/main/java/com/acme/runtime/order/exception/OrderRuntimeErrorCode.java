package com.acme.runtime.order.exception;

import com.acme.platform.core.ErrorCode;
import org.springframework.http.HttpStatus;

/** POST /v1/orders hata kodlari (order blogu 11000-11999; order-core'daki 110xx ile cakismasin diye 111xx). */
public enum OrderRuntimeErrorCode implements ErrorCode {
    UPSTREAM_UNAVAILABLE(11101, "Upstream service unavailable.", HttpStatus.SERVICE_UNAVAILABLE),
    UPSTREAM_REJECTED(11102, "Upstream service rejected the call.", HttpStatus.BAD_GATEWAY),
    OPERATION_IN_PROGRESS(11103, "Operation is in progress.", HttpStatus.CONFLICT),
    OPERATION_CANCELLED(11104, "Operation was cancelled.", HttpStatus.CONFLICT),
    QUOTA_REJECTED(11105, "Quota is not sufficient.", HttpStatus.UNPROCESSABLE_CONTENT),
    RESOURCE_ALREADY_ORDERED(11106, "Resource is already ordered.", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    OrderRuntimeErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code; this.message = message; this.httpStatus = httpStatus;
    }

    @Override public int getCode() { return code; }
    @Override public String getMessage() { return message; }
    @Override public String getService() { return "order"; }
    @Override public HttpStatus getHttpStatus() { return httpStatus; }
}
