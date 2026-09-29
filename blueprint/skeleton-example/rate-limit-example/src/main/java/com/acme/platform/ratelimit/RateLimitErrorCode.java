package com.acme.platform.ratelimit;

import com.acme.platform.core.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Ortak "security" blogu (90100-90199, referans Bolum 7.2): rate limit asildi / kural yok / store yok.
 * Mesajlar istemciye doner; Redis hata metni veya ozne asla buraya girmez.
 */
public enum RateLimitErrorCode implements ErrorCode {
    RATE_LIMIT_EXCEEDED(90100, "Too many requests.", HttpStatus.TOO_MANY_REQUESTS),
    /** Config hatasi: kural tanimsiz scope fail-closed'dur, uretime cikmadan yakalanmalidir. */
    RATE_LIMIT_RULE_MISSING(90101, "Rate limit rule is not configured.", HttpStatus.SERVICE_UNAVAILABLE),
    /** Redis'e ulasilamadi ve scope fail-closed: 503, istemci Retry-After ile tekrar dener. */
    RATE_LIMIT_UNAVAILABLE(90102, "Rate limit store is unavailable.", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code; private final String message; private final HttpStatus httpStatus;
    RateLimitErrorCode(int c, String m, HttpStatus s) { code = c; message = m; httpStatus = s; }
    public int getCode() { return code; } public String getMessage() { return message; }
    public String getService() { return "security"; } public HttpStatus getHttpStatus() { return httpStatus; }
}
