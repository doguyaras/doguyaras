package com.acme.platform.security.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Deterministik zaman: exp/nbf/skew testleri gercek zaman beklemeden calisir (platform-messaging ile ayni desen). */
public final class MutableClock extends Clock {
    private volatile Instant now = Instant.parse("2026-09-29T10:00:00Z");
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
    @Override public Instant instant() { return now; }
    public void advance(Duration d) { now = now.plus(d); }
    public void set(Instant instant) { now = instant; }
}
