package com.acme.broker.publish;

import java.util.UUID;

/**
 * mandatory yayin hicbir kuyruga yonlenmedi (basic.return). Kalici DEGIL gecici sayilir: tuketici binding'i henuz
 * deploy edilmemis olabilir ("once consumer deploy", Bolum 12.6); satir PENDING kalir ve binding gelince yayinlanir.
 */
public class UnroutableEventException extends RuntimeException {
    public UnroutableEventException(UUID eventId, String routingKey) {
        super("Event " + eventId + " unroutable with routing key " + routingKey);
    }
}
