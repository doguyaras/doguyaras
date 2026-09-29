package com.acme.platform.security.config;

import com.acme.platform.security.delegation.DelegationPolicy;
import com.acme.platform.security.web.InternalAccessPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Bolum 9.5 yml semasi. internal-access SIRALI listedir (first-match); yml'de yazim sirasi = degerlendirme sirasi.
 *
 * <pre>
 * service-jwt:
 *   audience: subscription-api
 *   service-name: subscription-service
 *   private-key-path: /run/secrets/subscription-service-signing-key   # JWK (OKP, d dahil)
 *   jwks-path: /run/config/service-jwks.json                          # {"iss": {"keys":[...]}}
 *   known-issuers: [gateway, order-service]
 *   internal-access:
 *     - path: "/internal/subscription/accounts/*&#47;operations/*&#47;consume"
 *       allowed-actors: [order-service]
 *   delegation:
 *     - { actor: order-service, operation: subscription.consume, user-context: REQUIRED }
 * </pre>
 */
@Validated
@ConfigurationProperties("service-jwt")
public record ServiceJwtProperties(
        @NotBlank String audience,
        @NotBlank String serviceName,
        String privateKeyPath,
        String jwksPath,
        @DefaultValue Set<String> knownIssuers,
        @DefaultValue("60") @Min(1) @Max(300) int ttlSeconds,
        @DefaultValue("30") @Min(0) @Max(120) int clockSkewSeconds,
        @DefaultValue @Valid List<InternalAccessRule> internalAccess,
        @DefaultValue @Valid List<DelegationRule> delegation) {

    public record InternalAccessRule(@NotBlank String path, @NotEmpty List<String> allowedActors) {
        public InternalAccessPolicy.Rule toRule() { return new InternalAccessPolicy.Rule(path, new HashSet<>(allowedActors)); }
    }

    public record DelegationRule(@NotBlank String actor, @NotBlank String operation, @NotNull DelegationPolicy.UserContext userContext) {
        public DelegationPolicy.Rule toRule() { return new DelegationPolicy.Rule(actor, operation, userContext); }
    }

    public InternalAccessPolicy internalAccessPolicy() {
        return new InternalAccessPolicy(internalAccess.stream().map(InternalAccessRule::toRule).toList());
    }

    public DelegationPolicy delegationPolicy() {
        return new DelegationPolicy(delegation.stream().map(DelegationRule::toRule).toList());
    }
}
