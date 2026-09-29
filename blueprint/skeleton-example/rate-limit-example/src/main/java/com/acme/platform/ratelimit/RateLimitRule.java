package com.acme.platform.ratelimit;

import java.util.regex.Pattern;

/**
 * Bir scope'un kurali: pencere basina en fazla {@code limit} istek, pencere {@code windowSeconds} saniye.
 * Scope adi anahtar tipini de tasir ({@code <aksiyon>-ip}, {@code <aksiyon>-account} ...), boylece ayni ozne
 * farkli scope'larda bagimsiz sayilir. Scope adi Redis key'ine ham girer; bu yuzden karakter kumesi dardir.
 */
public record RateLimitRule(String scope, int limit, int windowSeconds, FailPolicy failPolicy) {

    static final Pattern SCOPE = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    public RateLimitRule {
        if (scope == null || !SCOPE.matcher(scope).matches()) {
            throw new IllegalArgumentException("scope kebab-case olmali: " + scope);
        }
        if (limit < 1) throw new IllegalArgumentException("limit >= 1 olmali: " + scope);
        if (windowSeconds < 1) throw new IllegalArgumentException("window-seconds >= 1 olmali: " + scope);
        if (failPolicy == null) throw new IllegalArgumentException("fail-policy acikca secilmeli (OPEN|CLOSED): " + scope);
    }
}
