package com.acme.platform.messaging.readmodel;

import java.util.UUID;

/**
 * Delta akisinda sira boslugu: beklenen seq gelmedi. Tuketici DURUR ve kaynakla uzlasir (rebuild veya since ucu);
 * bosluktan sonraki olaylar uygulanmaz, aksi halde projeksiyon sessizce yanlis olur.
 */
public class GapDetectedException extends RuntimeException {

    private final String source;
    private final UUID aggregateId;
    private final long expectedSeq;
    private final long actualSeq;

    public GapDetectedException(String source, UUID aggregateId, long expectedSeq, long actualSeq) {
        super("read-model gap: source=%s aggregate=%s expected=%d actual=%d".formatted(source, aggregateId, expectedSeq, actualSeq));
        this.source = source;
        this.aggregateId = aggregateId;
        this.expectedSeq = expectedSeq;
        this.actualSeq = actualSeq;
    }

    public String source() { return source; }
    public UUID aggregateId() { return aggregateId; }
    public long expectedSeq() { return expectedSeq; }
    public long actualSeq() { return actualSeq; }
}
