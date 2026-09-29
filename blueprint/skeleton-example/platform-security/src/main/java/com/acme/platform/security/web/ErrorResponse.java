package com.acme.platform.security.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Filtre/interceptor hatalari icin kucuk zarf. Filtre DispatcherServlet'ten once calistigi icin controller advice'a
 * ulasamaz; JSON'u kendisi yazar. Token icerigi, kid, claim ya da red sebebi ASLA bu mesaja girmez.
 */
public record ErrorResponse(String code, String message) {

    public static final String SERVICE_TOKEN_INVALID = "SERVICE_TOKEN_INVALID";
    public static final String INTERNAL_ACCESS_DENIED = "INTERNAL_ACCESS_DENIED";
    public static final String INTERNAL_PATH_INVALID = "INTERNAL_PATH_INVALID";
    public static final String DELEGATION_DENIED = "DELEGATION_DENIED";
    public static final String ACCOUNT_CONTEXT_REQUIRED = "ACCOUNT_CONTEXT_REQUIRED";

    public String toJson() {
        return "{\"code\":\"" + escape(code) + "\",\"message\":\"" + escape(message) + "\"}";
    }

    public void write(HttpServletResponse response, HttpStatus status) throws IOException {
        response.resetBuffer();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        if (status == HttpStatus.UNAUTHORIZED) response.setHeader("WWW-Authenticate", "Bearer realm=\"service\"");
        response.getWriter().write(toJson());
        response.flushBuffer();
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.toString();
    }
}
