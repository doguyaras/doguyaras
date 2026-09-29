package com.acme.broker.consume;

/** Zehirli mesaj: cozulemeyen govde/header. Yeniden teslim anlamsiz -> requeue=false ile dogrudan DLQ (Bolum 12.3). */
public class MalformedEventException extends RuntimeException {
    public MalformedEventException(String message) { super(message); }
}
