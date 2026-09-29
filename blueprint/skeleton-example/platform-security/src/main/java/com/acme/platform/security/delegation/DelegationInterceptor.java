package com.acme.platform.security.delegation;

import com.acme.platform.security.web.ErrorResponse;
import com.acme.platform.security.web.ServiceRequestAttributes;
import com.acme.platform.security.web.ServiceSecurityException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * @RequireOperation tasiyan handler'lar icin delegasyon denetimi (Bolum 9.2.1). Filtre kimligi dogrulamis ve
 * allowlist'i gecmis olsa da, kullanici baglami kurali burada uygulanir. Kimlik attribute'u yoksa (uc /internal
 * disinda kalmis, filtre baglanmamis) fail-closed 401.
 */
public class DelegationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DelegationInterceptor.class);

    private final DelegationPolicy policy;

    public DelegationInterceptor(DelegationPolicy policy) {
        this.policy = policy;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;
        RequireOperation op = method.getMethodAnnotation(RequireOperation.class);
        if (op == null) return true;
        String actor = ServiceRequestAttributes.callerService(request).orElseThrow(() ->
                new ServiceSecurityException(HttpStatus.UNAUTHORIZED, ErrorResponse.SERVICE_TOKEN_INVALID,
                        "Service identity required"));
        UUID tokenAccount = ServiceRequestAttributes.accountId(request).orElse(null);
        UUID pathAccount = pathAccount(request, op.accountPathVariable());
        DelegationPolicy.Decision decision = policy.decide(actor, op.value(), tokenAccount, pathAccount);
        if (!decision.allowed()) {
            log.info("delegation denied: actor={} operation={} reason={}", actor, op.value(), decision.reason());
            throw new ServiceSecurityException(HttpStatus.FORBIDDEN, ErrorResponse.DELEGATION_DENIED, "Delegation denied");
        }
        return true;
    }

    /** Path degiskeni yoksa null; UUID degilse null (REQUIRED kural bunu zaten reddeder: fail-closed). */
    private static UUID pathAccount(HttpServletRequest request, String variable) {
        Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(vars instanceof Map<?, ?> map)) return null;
        Object raw = map.get(variable);
        if (!(raw instanceof String s)) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
