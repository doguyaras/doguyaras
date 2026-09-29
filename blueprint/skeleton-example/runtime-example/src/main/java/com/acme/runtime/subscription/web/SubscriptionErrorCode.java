package com.acme.runtime.subscription.web;

import com.acme.platform.core.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Katilimci hata kodlari. HTTP durumu sozlesmenin parcasidir: koordinatorun client'i govdeyi okumaz, yalniz durumu
 * cevirir (403 -> ParticipantForbidden, 409 -> ParticipantConflict, 5xx/timeout -> ParticipantUnavailable; Bolum 6.8).
 */
public enum SubscriptionErrorCode implements ErrorCode {
    OPERATION_TYPE_NOT_ALLOWED(12001, "Actor is not allowed for this operation type.", HttpStatus.FORBIDDEN),
    OPERATION_STATE_CONFLICT(12002, "Operation state does not allow this action.", HttpStatus.CONFLICT),
    OPERATION_NOT_FOUND(12003, "Operation not found.", HttpStatus.NOT_FOUND),
    PARTICIPANT_UNAVAILABLE(12004, "Participant temporarily unavailable.", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    SubscriptionErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code; this.message = message; this.httpStatus = httpStatus;
    }

    @Override public int getCode() { return code; }
    @Override public String getMessage() { return message; }
    @Override public String getService() { return "subscription"; }
    @Override public HttpStatus getHttpStatus() { return httpStatus; }
}
