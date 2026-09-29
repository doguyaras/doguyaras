package com.acme.platform.security.web;

import com.acme.platform.security.jwt.ServiceIdentity;
import com.acme.platform.security.jwt.ServiceJwtVerifier;
import com.acme.platform.security.jwt.ServiceTokenInvalidException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bolum 9.4: /internal/** icin servis JWT dogrulama + allowlist. Sira: path normalize (400) -> header (401) ->
 * token (401) -> first-match allowlist (403) -> attribute'lar. /internal disindaki istekler dokunulmadan gecer.
 * Red mesajlari tek tiptir; gercek sebep yalniz DEBUG loguna gider.
 */
public class ServiceJwtVerificationFilter extends OncePerRequestFilter {

    public static final String SERVICE_AUTH_HEADER = "X-Service-Auth";
    private static final String BEARER = "Bearer ";
    private static final Logger log = LoggerFactory.getLogger(ServiceJwtVerificationFilter.class);

    private final ServiceJwtVerifier verifier;
    private final InternalAccessPolicy policy;

    public ServiceJwtVerificationFilter(ServiceJwtVerifier verifier, InternalAccessPolicy policy) {
        this.verifier = verifier;
        this.policy = policy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        InternalPathNormalizer.Result path = InternalPathNormalizer.inspect(rawPath(request));
        switch (path.kind()) {
            case PUBLIC -> { chain.doFilter(request, response); return; }
            case INVALID -> {
                log.debug("internal path rejected: {}", request.getRequestURI());
                new ErrorResponse(ErrorResponse.INTERNAL_PATH_INVALID, "Invalid internal path").write(response, HttpStatus.BAD_REQUEST);
                return;
            }
            case INTERNAL -> { /* devam */ }
        }
        String token = extractToken(request);
        if (token == null) {
            new ErrorResponse(ErrorResponse.SERVICE_TOKEN_INVALID, "Service token missing").write(response, HttpStatus.UNAUTHORIZED);
            return;
        }
        ServiceIdentity identity;
        try {
            identity = verifier.verify(token);
        } catch (ServiceTokenInvalidException e) {
            log.debug("service token rejected on {}: {}", path.path(), e.getMessage());
            new ErrorResponse(ErrorResponse.SERVICE_TOKEN_INVALID, "Service token invalid").write(response, HttpStatus.UNAUTHORIZED);
            return;
        }
        if (!policy.allows(path.path(), identity.actor())) {
            log.info("internal access denied: actor={} path={}", identity.actor(), path.path());
            new ErrorResponse(ErrorResponse.INTERNAL_ACCESS_DENIED, "Internal access denied").write(response, HttpStatus.FORBIDDEN);
            return;
        }
        request.setAttribute(ServiceRequestAttributes.CALLER_SERVICE, identity.actor());
        request.setAttribute(ServiceRequestAttributes.ACCOUNT_ID, identity.accountId());
        request.setAttribute(ServiceRequestAttributes.BACKGROUND, identity.background());
        try {
            chain.doFilter(request, response);
        } finally {
            request.removeAttribute(ServiceRequestAttributes.CALLER_SERVICE);
            request.removeAttribute(ServiceRequestAttributes.ACCOUNT_ID);
            request.removeAttribute(ServiceRequestAttributes.BACKGROUND);
        }
    }

    /** Ham (decode edilmemis) path; context path cikarilir ki normalizer uygulama koku goreli calissin. */
    private static String rawPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (uri == null) return "";
        return context != null && !context.isEmpty() && uri.startsWith(context) ? uri.substring(context.length()) : uri;
    }

    private static String extractToken(HttpServletRequest request) {
        String header = request.getHeader(SERVICE_AUTH_HEADER);
        if (header != null && !header.isBlank()) return header.trim();
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.regionMatches(true, 0, BEARER, 0, BEARER.length()) && auth.length() > BEARER.length()) {
            return auth.substring(BEARER.length()).trim();
        }
        return null;
    }
}
