package com.acme.runtime.subscription.chaos;

import com.acme.runtime.subscription.web.SubscriptionErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * YALNIZ TEST: /internal/subscription/** cagrilarina hata enjekte eder ve gozlemler (esanli istek sayisi, alinan
 * traceparent). Servis JWT filtresinden SONRA calisir: yalniz kimligi dogrulanmis cagrilar sayilir/geciktirilir.
 * Gecikme is mantigi (ve TX commit) bittikten sonra, yanit tamponda tutulurken uygulanir: istemci read-timeout ile
 * vazgectiginde katilimcida commit olmus bir islem kalir (Bolum 11.4 "belirsiz sonuc").
 */
public class ChaosFilter extends OncePerRequestFilter {

    public static final String TRACE_ECHO_HEADER = "X-Received-Traceparent";
    private static final String PREFIX = "/internal/subscription/";

    private final ChaosState state;

    public ChaosFilter(ChaosState state) { this.state = state; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceparent = request.getHeader("traceparent");
        if (traceparent != null) {                                       // katilimcinin GORDUGU trace baglami
            response.setHeader(TRACE_ECHO_HEADER, traceparent);
            state.tracesByPath.put(request.getRequestURI(), traceparent);
        }
        ChaosState.Settings s = state.settings();
        boolean applies = s.appliesTo(operation(request));
        state.enter();
        try {
            if (applies && s.down()) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"ok\":false,\"error\":{\"code\":" + SubscriptionErrorCode.PARTICIPANT_UNAVAILABLE.getCode()
                        + ",\"message\":\"" + SubscriptionErrorCode.PARTICIPANT_UNAVAILABLE.getMessage() + "\",\"service\":\"subscription\"}}");
                return;
            }
            if (!applies || (s.slowMillis() <= 0 && !s.dropAfterCommit())) {
                chain.doFilter(request, response);
                return;
            }
            ContentCachingResponseWrapper buffered = new ContentCachingResponseWrapper(response);
            chain.doFilter(request, buffered);                           // is mantigi + commit burada tamamlandi
            if (s.dropAfterCommit()) {
                dropConnection(response);
                return;
            }
            pause(s.slowMillis());
            buffered.copyBodyToResponse();
        } finally {
            state.exit();
        }
    }

    /** Content-Length'ten kisa govde + Connection: close: istemci govdeyi okurken EOF alir (yanit kayboldu). */
    private static void dropConnection(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Connection", "close");
        response.setContentLength(1024);
        response.getOutputStream().write("{\"state\":".getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }

    private static void pause(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);                         // enjekte edilen gecikme (gercek zaman)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static String operation(HttpServletRequest request) {
        if ("GET".equals(request.getMethod())) return "get";
        String uri = request.getRequestURI();
        return uri.substring(uri.lastIndexOf('/') + 1);
    }
}
