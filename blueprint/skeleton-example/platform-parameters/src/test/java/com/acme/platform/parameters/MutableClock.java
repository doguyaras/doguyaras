package com.acme.platform.parameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Deterministik zaman: cacheTtl ve maxStaleness testleri gercek zaman beklemeden calisir. */
final class MutableClock extends Clock {
    volatile Instant now = Instant.parse("2026-09-29T10:00:00Z");
    @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return now; }
    void advance(Duration d) { now = now.plus(d); }
}
