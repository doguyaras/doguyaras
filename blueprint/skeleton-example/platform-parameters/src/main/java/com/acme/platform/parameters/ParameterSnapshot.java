package com.acme.platform.parameters;

import java.time.Instant;

/** Son basarili fetch: grup + ne zaman alindigi. Staleness = now - fetchedAt; bellekte ve diskte ayni sekil. */
public record ParameterSnapshot(ParameterGroupDto group, Instant fetchedAt) {
    public ParameterSnapshot {
        if (group == null || fetchedAt == null) throw new IllegalArgumentException("snapshot eksik");
    }
}
