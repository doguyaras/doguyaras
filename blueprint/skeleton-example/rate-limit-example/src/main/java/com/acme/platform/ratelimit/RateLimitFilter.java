package com.acme.platform.ratelimit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.function.Function;

/**
 * Tek scope icin servlet filtresi. Ozne cozumleyici null donerse (ornegin {@code *-account} scope'u anonim
 * istekte) bu scope o istege uygulanmaz; IP scope'u ayri bir filtre olarak zaten calisir.
 *
 * <p>Red yaniti DispatcherServlet'ten once olusur; bu yuzden controller hata formatiyla ayni zarf burada
 * yazilir (Bolum 6.2, 7.6). 429 ve 503'te {@code Retry-After} her zaman vardir: istemci/SDK geri cekilir,
 * bekleme suresini tahmin etmez.
 */
public final class RateLimitFilter implements Filter {

    private final RedisFixedWindowRateLimiter limiter;
    private final String scope;
    private final Function<HttpServletRequest, String> subjectResolver;
    private final Clock clock;

    public RateLimitFilter(RedisFixedWindowRateLimiter limiter, String scope,
                           Function<HttpServletRequest, String> subjectResolver, Clock clock) {
        this.limiter = limiter; this.scope = scope; this.subjectResolver = subjectResolver; this.clock = clock;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String subject = subjectResolver.apply(request);
        if (subject == null) { chain.doFilter(req, res); return; }
        RateLimitDecision decision = limiter.tryAcquire(scope, subject);
        if (decision.allowed()) { chain.doFilter(req, res); return; }
        writeRejection(request, (HttpServletResponse) res, decision, clock);
    }

    /**
     * 429/503 + Retry-After + standart hata zarfi. Filtre disinda (ornegin OTP servisinde elle kontrol) da
     * kullanilir ki iki farkli red formati olusmasin.
     */
    public static void writeRejection(HttpServletRequest request, HttpServletResponse response,
                                      RateLimitDecision decision, Clock clock) throws IOException {
        RateLimitErrorCode code = decision.errorCode();
        if (code == null) throw new IllegalArgumentException("izin verilen karar reddedilemez: " + decision);
        response.setStatus(code.getHttpStatus().value());
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // details yalniz istemciye gosterilebilir anahtar=deger; ozne (IP/telefon) asla yazilmaz.
        String body = "{\"ok\":false,\"data\":null,\"error\":{\"code\":" + code.getCode()
                + ",\"message\":" + json(code.getMessage())
                + ",\"service\":" + json(code.getService())
                + ",\"path\":" + json(request.getRequestURI())
                + ",\"timestamp\":" + clock.millis()
                + ",\"traceId\":null"
                + ",\"details\":[" + json("retryAfterSeconds=" + decision.retryAfterSeconds()) + "]}}";
        response.getWriter().write(body);
    }

    private static String json(String s) {
        StringBuilder b = new StringBuilder(s.length() + 2).append('"');
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                default -> {
                    if (ch < 0x20) b.append(String.format("\\u%04x", (int) ch)); else b.append(ch);
                }
            }
        }
        return b.append('"').toString();
    }
}
