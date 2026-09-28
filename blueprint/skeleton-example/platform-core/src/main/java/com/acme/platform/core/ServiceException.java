package com.acme.platform.core;
public class ServiceException extends RuntimeException {
    private final ErrorCode errorCode;
    public ServiceException(ErrorCode c) { super(c.getMessage()); this.errorCode = c; }
    public ErrorCode getErrorCode() { return errorCode; }
}
