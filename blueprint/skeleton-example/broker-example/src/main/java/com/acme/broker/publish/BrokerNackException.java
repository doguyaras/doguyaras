package com.acme.broker.publish;

import java.util.UUID;

/** Broker mesaji kalici olarak kabul etmedi (NACK). Gecici sayilir: poller backoff ile yeniden dener; SimpleName last_error_code olur. */
public class BrokerNackException extends RuntimeException {
    public BrokerNackException(UUID eventId, String reason) {
        super("Broker NACK for event " + eventId + ": " + reason);
    }
}
