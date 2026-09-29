package com.acme.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.platform.security.config.ServiceJwtProperties;
import com.acme.platform.security.delegation.DelegationPolicy.UserContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/** Bolum 9.5 yml anahtarlari birebir baglanir: service-jwt.internal-access[i].path / allowed-actors, delegation[i].user-context. */
class ServiceJwtPropertiesBindingTest {

    @Test
    void bindsOrderedRulesFromRelaxedKeys() {
        var source = new MapConfigurationPropertySource(Map.ofEntries(
                Map.entry("service-jwt.audience", "subscription-api"),
                Map.entry("service-jwt.service-name", "subscription-service"),
                Map.entry("service-jwt.known-issuers[0]", "gateway"),
                Map.entry("service-jwt.known-issuers[1]", "order-service"),
                Map.entry("service-jwt.internal-access[0].path", "/internal/subscription/accounts/*/operations/*/consume"),
                Map.entry("service-jwt.internal-access[0].allowed-actors[0]", "order-service"),
                Map.entry("service-jwt.internal-access[1].path", "/internal/subscription/**"),
                Map.entry("service-jwt.internal-access[1].allowed-actors", "backoffice-service,gateway"),
                Map.entry("service-jwt.delegation[0].actor", "order-service"),
                Map.entry("service-jwt.delegation[0].operation", "subscription.consume"),
                Map.entry("service-jwt.delegation[0].user-context", "REQUIRED")));
        ServiceJwtProperties props = new Binder(source).bind("service-jwt", Bindable.of(ServiceJwtProperties.class)).get();

        assertThat(props.ttlSeconds()).isEqualTo(60);
        assertThat(props.clockSkewSeconds()).isEqualTo(30);
        assertThat(props.knownIssuers()).containsExactlyInAnyOrder("gateway", "order-service");
        assertThat(props.internalAccess()).hasSize(2);
        assertThat(props.internalAccess().get(0).path()).endsWith("/consume");           // sira korunur
        assertThat(props.internalAccess().get(1).allowedActors()).containsExactly("backoffice-service", "gateway");
        assertThat(props.delegation().get(0).userContext()).isEqualTo(UserContext.REQUIRED);

        var policy = props.internalAccessPolicy();
        assertThat(policy.allows("/internal/subscription/accounts/a/operations/k/consume", "order-service")).isTrue();
        assertThat(policy.allows("/internal/subscription/accounts/a/operations/k/consume", "backoffice-service")).isFalse();
        assertThat(props.delegationPolicy().decide("order-service", "subscription.consume", null, null).allowed()).isFalse();
    }
}
