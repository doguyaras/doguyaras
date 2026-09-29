package com.acme.order.exception;
import org.springframework.http.HttpStatus;
/** order servisinin kod blogu: 11000-11999 (referans Bolum 7.2; blok tablosu README'de). */
public enum ErrorCode implements com.acme.platform.core.ErrorCode {
    ORDER_NOT_FOUND(11001, "Order not found.", HttpStatus.NOT_FOUND),
    ORDER_NOT_CANCELLABLE(11002, "Order cannot be cancelled.", HttpStatus.CONFLICT),
    // Is kurali reddi: istek sekli dogru (400 degil) ama icerik kabul edilemez -> 422
    ORDER_QUANTITY_INVALID(11003, "Order quantity is out of range.", HttpStatus.UNPROCESSABLE_CONTENT);
    private final int code; private final String message; private final HttpStatus httpStatus;
    ErrorCode(int c, String m, HttpStatus s) { code = c; message = m; httpStatus = s; }
    public int getCode() { return code; } public String getMessage() { return message; }
    public String getService() { return "order"; } public HttpStatus getHttpStatus() { return httpStatus; }
}
