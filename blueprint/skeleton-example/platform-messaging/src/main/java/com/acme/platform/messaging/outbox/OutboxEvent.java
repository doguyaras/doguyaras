package com.acme.platform.messaging.outbox;

import java.time.Instant;
import java.util.UUID;

/** outbox_event satiri (referans Bolum 11.2). Payload/headers JSON metin olarak tasinir; JPA yok, JDBC. */
public record OutboxEvent(
        UUID id,
        String kind,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload,
        String headers,
        String status,
        int priority,
        DeadPolicy deadPolicy,
        int retryCount,
        Instant nextRetryAt,
        Instant lockedUntil,
        UUID claimToken,
        String lastErrorCode,
        Instant createdAt) {

    public enum DeadPolicy { DEAD_ON_PERMANENT, NEVER_DEAD }

    public boolean isNeverDead() { return deadPolicy == DeadPolicy.NEVER_DEAD; }
}
