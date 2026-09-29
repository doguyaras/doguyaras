package com.acme.platform.security.support;

import com.acme.platform.security.delegation.DelegationInterceptor;
import com.acme.platform.security.delegation.DelegationPolicy;
import com.acme.platform.security.delegation.DelegationPolicy.UserContext;
import com.acme.platform.security.jwt.Ed25519Keys;
import com.acme.platform.security.jwt.ServiceJwtKeyRegistry;
import com.acme.platform.security.jwt.ServiceJwtSigner;
import com.acme.platform.security.jwt.ServiceJwtVerifier;
import com.acme.platform.security.web.CurrentAccountArgumentResolver;
import com.acme.platform.security.web.InternalAccessPolicy;
import com.acme.platform.security.web.InternalAccessPolicy.Rule;
import com.acme.platform.security.web.ServiceJwtVerificationFilter;
import com.acme.platform.security.web.ServiceSecurityExceptionHandler;
import com.nimbusds.jose.jwk.OctetKeyPair;
import java.util.List;
import java.util.Set;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * GERCEK filtre + interceptor + resolver + advice ile standalone MockMvc (Bolum 23.5 deseni). Dort servis, her biri
 * kendi Ed25519 anahtariyla; rogue anahtar hicbir issuer altinda kayitli degil.
 */
public final class SecurityFixture {

    public static final String AUDIENCE = "subscription-api";
    public static final String ORDER = "order-service";
    public static final String WORKER = "subscription-worker";
    public static final String BACKOFFICE = "backoffice-service";
    public static final String GATEWAY = "gateway";
    public static final Set<String> KNOWN_ISSUERS = Set.of(ORDER, WORKER, BACKOFFICE, GATEWAY);

    public final MutableClock clock = new MutableClock();
    public final OctetKeyPair orderKey = Ed25519Keys.generate("order-1");
    public final OctetKeyPair workerKey = Ed25519Keys.generate("worker-1");
    public final OctetKeyPair backofficeKey = Ed25519Keys.generate("backoffice-1");
    public final OctetKeyPair gatewayKey = Ed25519Keys.generate("gw-1");
    public final OctetKeyPair rogueKey = Ed25519Keys.generate("rogue-1");
    public final ServiceJwtKeyRegistry registry = new ServiceJwtKeyRegistry();
    public final ServiceJwtSigner order = new ServiceJwtSigner(orderKey, ORDER, clock);
    public final ServiceJwtSigner worker = new ServiceJwtSigner(workerKey, WORKER, clock);
    public final ServiceJwtSigner backoffice = new ServiceJwtSigner(backofficeKey, BACKOFFICE, clock);
    public final ServiceJwtSigner gateway = new ServiceJwtSigner(gatewayKey, GATEWAY, clock);
    public final SubscriptionInternalController controller = new SubscriptionInternalController();

    public SecurityFixture() {
        registry.register(ORDER, orderKey);
        registry.register(WORKER, workerKey);
        registry.register(BACKOFFICE, backofficeKey);
        registry.register(GATEWAY, gatewayKey);
    }

    /** Dar kurallar once, catch-all sonda (Bolum 9.5). */
    public static List<Rule> defaultRules() {
        return List.of(
                new Rule("/internal/subscription/accounts/*/operations/*/consume", Set.of(ORDER)),
                new Rule("/internal/subscription/reconcile", Set.of(WORKER, BACKOFFICE)),
                new Rule("/internal/subscription/accounts/*/balance", Set.of(ORDER)),
                new Rule("/internal/subscription/**", Set.of(BACKOFFICE, GATEWAY)));
    }

    public static List<DelegationPolicy.Rule> defaultDelegation() {
        return List.of(
                new DelegationPolicy.Rule(ORDER, "subscription.consume", UserContext.REQUIRED),
                new DelegationPolicy.Rule(WORKER, "subscription.reconcile", UserContext.FORBIDDEN),
                new DelegationPolicy.Rule(BACKOFFICE, "subscription.profile", UserContext.OPTIONAL));
    }

    public ServiceJwtVerifier verifier() {
        return new ServiceJwtVerifier(registry, KNOWN_ISSUERS, AUDIENCE, clock);
    }

    public MockMvc mockMvc() { return mockMvc(defaultRules()); }

    public MockMvc mockMvc(List<Rule> rules) {
        return MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new ServiceJwtVerificationFilter(verifier(), new InternalAccessPolicy(rules)))
                .addInterceptors(new DelegationInterceptor(new DelegationPolicy(defaultDelegation())))
                .setCustomArgumentResolvers(new CurrentAccountArgumentResolver())
                .setControllerAdvice(new ServiceSecurityExceptionHandler())
                .build();
    }
}
