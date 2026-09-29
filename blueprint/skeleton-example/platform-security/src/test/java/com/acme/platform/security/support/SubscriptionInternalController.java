package com.acme.platform.security.support;

import com.acme.platform.security.delegation.RequireOperation;
import com.acme.platform.security.web.CurrentAccount;
import com.acme.platform.security.web.ServiceJwtVerificationFilter;
import com.acme.platform.security.web.ServiceRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sahte subscription servisi uclari. Yanit, filtrenin yazdigi attribute'lari ve gelen header'i geri yansitir ki
 * test "controller'a ne ulasti"yi dogrulayabilsin. hits sayaci: reddedilen istek handler'a ulasmamali.
 */
@RestController
public class SubscriptionInternalController {

    public final AtomicInteger hits = new AtomicInteger();

    @PostMapping("/internal/subscription/accounts/{accountId}/operations/{key}/consume")
    @RequireOperation("subscription.consume")
    public Map<String, Object> consume(@PathVariable UUID accountId, @PathVariable String key,
                                       @CurrentAccount UUID account, HttpServletRequest request) {
        hits.incrementAndGet();
        Map<String, Object> body = echo(request);
        body.put("pathAccount", accountId.toString());
        body.put("account", account.toString());
        body.put("key", key);
        return body;
    }

    @PostMapping("/internal/subscription/reconcile")
    @RequireOperation("subscription.reconcile")
    public Map<String, Object> reconcile(HttpServletRequest request) {
        hits.incrementAndGet();
        return echo(request);
    }

    /** OPTIONAL delegasyon ama controller hesap ister: @CurrentAccount 401 senaryosu. */
    @PostMapping("/internal/subscription/accounts/{accountId}/profile")
    @RequireOperation("subscription.profile")
    public Map<String, Object> profile(@PathVariable UUID accountId, @CurrentAccount UUID account, HttpServletRequest request) {
        hits.incrementAndGet();
        Map<String, Object> body = echo(request);
        body.put("account", account.toString());
        return body;
    }

    /** Delegasyon kurali olmayan salt-okur uc: allowlist siralama senaryolari icin. */
    @GetMapping("/internal/subscription/accounts/{accountId}/balance")
    public Map<String, Object> balance(@PathVariable UUID accountId, HttpServletRequest request) {
        hits.incrementAndGet();
        return echo(request);
    }

    @GetMapping("/internal/admin/keys")
    public Map<String, Object> adminKeys(HttpServletRequest request) {
        hits.incrementAndGet();
        return echo(request);
    }

    /** /internal DISINDA ama @RequireOperation tasiyan uc: interceptor tum path'lere kayitli olmali (fail-closed). */
    @PostMapping("/v1/misplaced/reconcile")
    @RequireOperation("subscription.reconcile")
    public Map<String, Object> misplacedReconcile(HttpServletRequest request) {
        hits.incrementAndGet();
        return echo(request);
    }

    @GetMapping("/v1/ping")
    public Map<String, Object> ping(HttpServletRequest request) {
        hits.incrementAndGet();
        Map<String, Object> body = echo(request);
        body.put("pong", true);
        return body;
    }

    private static Map<String, Object> echo(HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("caller", ServiceRequestAttributes.callerService(request).orElse(null));
        body.put("accountAttr", ServiceRequestAttributes.accountId(request).map(UUID::toString).orElse(null));
        body.put("background", ServiceRequestAttributes.background(request));
        body.put("token", request.getHeader(ServiceJwtVerificationFilter.SERVICE_AUTH_HEADER));
        return body;
    }
}
