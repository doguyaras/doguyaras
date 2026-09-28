package com.acme.platform.core;
import org.springframework.http.HttpStatus;
public interface ErrorCode { int getCode(); String getMessage(); String getService(); HttpStatus getHttpStatus(); }
