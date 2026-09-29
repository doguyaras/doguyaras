package com.acme.platform.messaging.saga;

import java.util.UUID;

/**
 * success() compare-and-set kaybetti: recovery araya girip saga'yi iptal etti (CANCEL_REQUESTED/COMPENSATED).
 * Domain TX'inde firlatilir; domain yazimi rollback olur (referans Bolum 11.4 adim 3).
 */
public class SagaCancelledException extends RuntimeException {
    private final UUID sagaId;
    public SagaCancelledException(UUID sagaId) { super("saga cancelled by recovery"); this.sagaId = sagaId; }
    public UUID sagaId() { return sagaId; }
}
