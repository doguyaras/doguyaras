package com.acme.platform.messaging.outbox;

/** Yeniden denemenin anlamsiz oldugu hata (kalici 4xx; 401/403/408/429 haric). DEAD karari dead_policy'ye gore verilir. */
public class PermanentFailureException extends RuntimeException {
    public PermanentFailureException(String message) { super(message); }
}
