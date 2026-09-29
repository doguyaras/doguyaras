package com.acme.broker.publish;

import java.util.UUID;

/** Broker confirm'i sinirli surede gelmedi; mesaj ulasmis OLABILIR. Yeniden yayin guvenlidir (tuketici inbox dedup). */
public class ConfirmTimeoutException extends RuntimeException {
    public ConfirmTimeoutException(UUID eventId) {
        super("No publisher confirm for event " + eventId + " within timeout");
    }
}
