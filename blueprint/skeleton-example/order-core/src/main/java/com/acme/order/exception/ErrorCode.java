package com.acme.order.exception;
import org.springframework.http.HttpStatus;
public enum ErrorCode implements com.acme.platform.core.ErrorCode {
    ORDER_NOT_FOUND(11001, "Order not found.", HttpStatus.NOT_FOUND),
    ORDER_NOT_CANCELLABLE(11002, "Order cannot be cancelled.", HttpStatus.CONFLICT),
    ACCOUNT_INACTIVE(11003, "Account is not active.", HttpStatus.FORBIDDEN),
    ORDER_CANCEL_FAILED(11001, "Order cancel failed.", HttpStatus.INTERNAL_SERVER_ERROR);
    private final int code; private final String message; private final HttpStatus httpStatus;
    ErrorCode(int c, String m, HttpStatus s) { code = c; message = m; httpStatus = s; }
    public int getCode() { return code; } public String getMessage() { return message; }
    public String getService() { return "order"; } public HttpStatus getHttpStatus() { return httpStatus; }
}
