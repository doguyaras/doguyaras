package com.acme.runtime.order.web;

import com.acme.platform.security.jwt.ServiceIdentity;
import com.acme.platform.security.jwt.ServiceJwtVerifier;
import com.acme.platform.security.jwt.ServiceTokenInvalidException;
import com.acme.platform.security.web.ErrorResponse;
import com.acme.platform.security.web.ServiceJwtVerificationFilter;
import com.acme.platform.security.web.ServiceRequestAttributes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Public uclar (/v1/**) icin kimlik (referans Bolum 6.1 tablo): gateway kullanici JWT'sini dogrular ve servise
 * act=gateway, sub=kullanici tasiyan servis JWT'si gecirir. Bu filtre yalniz izinli aktorleri (gateway) kabul eder
 * ("public path'ler de aktore kisitlanabilir", Bolum 9.5) ve sub'i x.accountId attribute'una yazar; @CurrentAccount
 * ve ServiceJwtClientInterceptor ayni attribute'u okur. sub'siz token public ucta anlamsizdir -> 401.
 */
public class GatewayIdentityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayIdentityFilter.class);

    private final ServiceJwtVerifier verifier;
    private final Set<String> allowedActors;

    public GatewayIdentityFilter(ServiceJwtVerifier verifier, Set<String> allowedActors) {
        this.verifier = verifier;
        this.allowedActors = Set.copyOf(allowedActors);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader(ServiceJwtVerificationFilter.SERVICE_AUTH_HEADER);
        if (token == null || token.isBlank()) {
            new ErrorResponse(ErrorResponse.SERVICE_TOKEN_INVALID, "Service token missing").write(response, HttpStatus.UNAUTHORIZED);
            return;
        }
        ServiceIdentity identity;
        try {
            identity = verifier.verify(token.trim());
        } catch (ServiceTokenInvalidException e) {
            log.debug("public request token rejected: {}", e.getMessage());
            new ErrorResponse(ErrorResponse.SERVICE_TOKEN_INVALID, "Service token invalid").write(response, HttpStatus.UNAUTHORIZED);
            return;
        }
        if (!allowedActors.contains(identity.actor())) {
            log.info("public access denied: actor={} path={}", identity.actor(), request.getRequestURI());
            new ErrorResponse(ErrorResponse.INTERNAL_ACCESS_DENIED, "Access denied").write(response, HttpStatus.FORBIDDEN);
            return;
        }
        if (identity.background()) {
            new ErrorResponse(ErrorResponse.ACCOUNT_CONTEXT_REQUIRED, "Account context required").write(response, HttpStatus.UNAUTHORIZED);
            return;
        }
        request.setAttribute(ServiceRequestAttributes.CALLER_SERVICE, identity.actor());
        request.setAttribute(ServiceRequestAttributes.ACCOUNT_ID, identity.accountId());
        try {
            chain.doFilter(request, response);
        } finally {
            request.removeAttribute(ServiceRequestAttributes.CALLER_SERVICE);
            request.removeAttribute(ServiceRequestAttributes.ACCOUNT_ID);
        }
    }
}
