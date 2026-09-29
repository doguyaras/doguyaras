package com.acme.platform.ratelimit;

/**
 * Tek bir kontrolun sonucu. {@code retryAfterSeconds} yalniz red durumunda anlamlidir (429/503 Retry-After).
 * FAIL_OPEN "izin verildi" sayilir ama {@code degraded()} ile ayirt edilir: cagiran isterse loglar, sayac yoktur.
 */
public record RateLimitDecision(Outcome outcome, long remaining, long retryAfterSeconds) {

    public enum Outcome {
        /** Redis sayaci limit icinde. */
        ALLOWED,
        /** Redis sayaci limiti asti: 429 + Retry-After. */
        EXCEEDED,
        /** Redis'e ulasilamadi, scope fail-open: istek gecer, metrik artar. */
        FAIL_OPEN,
        /** Redis'e ulasilamadi, scope fail-closed: 503 RATE_LIMIT_UNAVAILABLE. */
        UNAVAILABLE,
        /** Scope icin kural yok: config hatasi, 503 (fail-closed). */
        RULE_MISSING
    }

    /** Store'a ulasilamadiginda 503 icin onerilen bekleme; kisa tutulur ki toparlaninca istemci hemen doner. */
    static final long UNAVAILABLE_RETRY_AFTER_SECONDS = 1;

    public boolean allowed() { return outcome == Outcome.ALLOWED || outcome == Outcome.FAIL_OPEN; }

    public boolean degraded() { return outcome == Outcome.FAIL_OPEN; }

    /** Red icin hata kodu; izin verilen kararlarda null. */
    public RateLimitErrorCode errorCode() {
        return switch (outcome) {
            case ALLOWED, FAIL_OPEN -> null;
            case EXCEEDED -> RateLimitErrorCode.RATE_LIMIT_EXCEEDED;
            case UNAVAILABLE -> RateLimitErrorCode.RATE_LIMIT_UNAVAILABLE;
            case RULE_MISSING -> RateLimitErrorCode.RATE_LIMIT_RULE_MISSING;
        };
    }

    static RateLimitDecision failOpen() { return new RateLimitDecision(Outcome.FAIL_OPEN, -1, 0); }

    static RateLimitDecision unavailable() {
        return new RateLimitDecision(Outcome.UNAVAILABLE, 0, UNAVAILABLE_RETRY_AFTER_SECONDS);
    }

    static RateLimitDecision ruleMissing() {
        return new RateLimitDecision(Outcome.RULE_MISSING, 0, UNAVAILABLE_RETRY_AFTER_SECONDS);
    }
}
