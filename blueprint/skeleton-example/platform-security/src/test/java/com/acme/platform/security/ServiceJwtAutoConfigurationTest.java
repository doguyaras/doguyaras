package com.acme.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.platform.security.jwt.Ed25519Keys;
import com.acme.platform.security.jwt.ServiceJwtKeyRegistry;
import com.acme.platform.security.jwt.ServiceJwtSigner;
import com.acme.platform.security.jwt.ServiceJwtVerifier;
import com.acme.platform.security.support.MutableClock;
import com.acme.platform.security.support.SubscriptionInternalController;
import com.acme.platform.security.web.ErrorResponse;
import com.acme.platform.security.web.ServiceJwtVerificationFilter;
import com.nimbusds.jose.jwk.OctetKeyPair;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Kutuphane olarak kablolama: servis yalniz yml + anahtar dosyalari verir; filtre, interceptor, resolver, advice ve
 * signer auto-configuration'dan gelir. Anahtar dosyalari Bolum 9.5'teki bicimde (private JWK, iss -> JWKS).
 */
@SpringBootTest(properties = {
        "service-jwt.audience=subscription-api",
        "service-jwt.service-name=subscription-service",
        "service-jwt.ttl-seconds=45",
        "service-jwt.internal-access[0].path=/internal/subscription/accounts/*/operations/*/consume",
        "service-jwt.internal-access[0].allowed-actors=order-service",
        "service-jwt.internal-access[1].path=/internal/subscription/reconcile",
        "service-jwt.internal-access[1].allowed-actors=subscription-worker",
        "service-jwt.delegation[0].actor=order-service",
        "service-jwt.delegation[0].operation=subscription.consume",
        "service-jwt.delegation[0].user-context=REQUIRED",
        "service-jwt.delegation[1].actor=subscription-worker",
        "service-jwt.delegation[1].operation=subscription.reconcile",
        "service-jwt.delegation[1].user-context=FORBIDDEN"})
@AutoConfigureMockMvc
class ServiceJwtAutoConfigurationTest {

    static final MutableClock CLOCK = new MutableClock();
    static final OctetKeyPair ORDER_KEY = Ed25519Keys.generate("order-1");
    static final OctetKeyPair WORKER_KEY = Ed25519Keys.generate("worker-1");
    static final OctetKeyPair OWN_KEY = Ed25519Keys.generate("subscription-1");

    @DynamicPropertySource
    static void keyFiles(DynamicPropertyRegistry registry) throws IOException {
        Path dir = Files.createTempDirectory("service-jwt");
        ServiceJwtKeyRegistry jwks = new ServiceJwtKeyRegistry();
        jwks.register("order-service", ORDER_KEY);
        jwks.register("subscription-worker", WORKER_KEY);
        Path jwksFile = Files.writeString(dir.resolve("service-jwks.json"), jwks.toJson());
        Path privateFile = Files.writeString(dir.resolve("signing-key.json"), OWN_KEY.toJSONString());
        registry.add("service-jwt.jwks-path", jwksFile::toString);
        registry.add("service-jwt.private-key-path", privateFile::toString);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(SubscriptionInternalController.class)
    static class App {
        @Bean Clock clock() { return CLOCK; }                              // ConditionalOnMissingBean: uygulama saati kazanir
    }

    @Autowired MockMvc mvc;
    @Autowired ServiceJwtSigner ownSigner;
    @Autowired ServiceJwtVerifier verifier;
    @Autowired ServiceJwtKeyRegistry keyRegistry;
    @Autowired SubscriptionInternalController controller;

    final ServiceJwtSigner order = new ServiceJwtSigner(ORDER_KEY, "order-service", CLOCK);
    final ServiceJwtSigner worker = new ServiceJwtSigner(WORKER_KEY, "subscription-worker", CLOCK);

    @Test
    void filterInterceptorAndResolver_areWiredFromProperties() throws Exception {
        UUID account = UUID.randomUUID();
        String url = "/internal/subscription/accounts/" + account + "/operations/op/consume";
        mvc.perform(post(url)).andExpect(status().isUnauthorized());
        mvc.perform(post(url).header(ServiceJwtVerificationFilter.SERVICE_AUTH_HEADER, worker.mint("subscription-api", account)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(ErrorResponse.INTERNAL_ACCESS_DENIED));
        mvc.perform(post(url).header(ServiceJwtVerificationFilter.SERVICE_AUTH_HEADER, order.mint("subscription-api", null)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(ErrorResponse.DELEGATION_DENIED));
        mvc.perform(post(url).header(ServiceJwtVerificationFilter.SERVICE_AUTH_HEADER, order.mint("subscription-api", account)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caller").value("order-service"))
                .andExpect(jsonPath("$.account").value(account.toString()));
        mvc.perform(post("/internal/subscription/reconcile")
                        .header(ServiceJwtVerificationFilter.SERVICE_AUTH_HEADER, worker.mint("subscription-api", null)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.background").value(true));
        mvc.perform(get("/v1/ping")).andExpect(status().isOk());
        mvc.perform(get(java.net.URI.create("/internal/..%2Fv1/ping"))).andExpect(status().isBadRequest());
    }

    @Test
    void requireOperationOutsideInternal_isStillEnforced_failClosed() throws Exception {
        // filtre /internal disina dokunmaz -> kimlik yok; interceptor yalniz /internal/** icin kayitli olsaydi 200 donerdi
        int before = controller.hits.get();
        mvc.perform(post("/v1/misplaced/reconcile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorResponse.SERVICE_TOKEN_INVALID));
        assertThat(controller.hits).hasValue(before);
    }

    @Test
    void signerBean_usesPrivateKeyFileAndConfiguredTtl() throws Exception {
        assertThat(ownSigner.serviceName()).isEqualTo("subscription-service");
        assertThat(ownSigner.kid()).isEqualTo("subscription-1");
        String token = ownSigner.mint("notification-api", null);
        var claims = com.nimbusds.jwt.SignedJWT.parse(token).getJWTClaimsSet();
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(CLOCK.instant().plus(Duration.ofSeconds(45)));
        // kendi anahtarimiz JWKS'te yok: kendi token'imizi kendimiz dogrulayamayiz (aud zaten baska)
        assertThat(keyRegistry.issuers()).containsExactlyInAnyOrder("order-service", "subscription-worker");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> verifier.verify(token))
                .as("uygulamanin verifier bean'i kendi (kayitsiz kid, baska aud) token'ini reddeder")
                .isInstanceOf(RuntimeException.class);
        assertThat(Set.copyOf(keyRegistry.issuers())).doesNotContain("subscription-service");
    }
}
