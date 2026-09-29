package com.acme.platform.security;

import static com.acme.platform.security.support.SecurityFixture.AUDIENCE;
import static com.acme.platform.security.support.SecurityFixture.BACKOFFICE;
import static com.acme.platform.security.support.SecurityFixture.ORDER;
import static com.acme.platform.security.support.SecurityFixture.WORKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.platform.security.jwt.ServiceJwtSigner;
import com.acme.platform.security.support.RawTokens;
import com.acme.platform.security.support.SecurityFixture;
import com.acme.platform.security.web.ErrorResponse;
import com.acme.platform.security.web.ServiceJwtVerificationFilter;
import com.nimbusds.jose.JWSAlgorithm;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Bolum 9.4 filtre zinciri ve 9.2.1 delegasyon kurallari, GERCEK filtre + interceptor + resolver uzerinden
 * (kanit seviyesi 1, MockMvc). Her test bir saldiri/senaryo satiridir; hits sayaci reddedilen istegin controller'a
 * ulasmadigini kanitlar.
 */
class ServiceJwtVerificationFilterTest {

    static final String HDR = ServiceJwtVerificationFilter.SERVICE_AUTH_HEADER;
    final UUID accountA = UUID.randomUUID();
    final UUID accountB = UUID.randomUUID();
    SecurityFixture fx;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        fx = new SecurityFixture();
        mvc = fx.mockMvc();
    }

    MockHttpServletRequestBuilder consume(UUID pathAccount) {
        return post("/internal/subscription/accounts/{a}/operations/{k}/consume", pathAccount, "op-1")
                .contentType(MediaType.APPLICATION_JSON);
    }

    Instant now() { return fx.clock.instant(); }

    // ---- kimlik + allowlist ------------------------------------------------------------------------------------

    @Test
    void allowedActorWithOwnSubject_reaches200_andAttributesAreSet() throws Exception {
        mvc.perform(consume(accountA).header(HDR, fx.order.mint(AUDIENCE, accountA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caller").value(ORDER))
                .andExpect(jsonPath("$.account").value(accountA.toString()))
                .andExpect(jsonPath("$.accountAttr").value(accountA.toString()))
                .andExpect(jsonPath("$.pathAccount").value(accountA.toString()))
                .andExpect(jsonPath("$.background").value(false));
        assertThat(fx.controller.hits).hasValue(1);
    }

    @Test
    void authorizationBearerHeader_isAcceptedAsCarrier() throws Exception {
        mvc.perform(consume(accountA).header(HttpHeaders.AUTHORIZATION, "Bearer " + fx.order.mint(AUDIENCE, accountA)))
                .andExpect(status().isOk());
    }

    @Test
    void knownActorNotInRule_is403_INTERNAL_ACCESS_DENIED() throws Exception {
        // backoffice bilinen bir imzalayici; consume kurali yalniz order-service'e acik
        mvc.perform(consume(accountA).header(HDR, fx.backoffice.mint(AUDIENCE, accountA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorResponse.INTERNAL_ACCESS_DENIED));
        assertThat(fx.controller.hits).hasValue(0);
    }

    @Test
    void pathWithoutAnyRule_isDefaultDeny403() throws Exception {
        mvc.perform(get("/internal/admin/keys").header(HDR, fx.gateway.mint(AUDIENCE, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorResponse.INTERNAL_ACCESS_DENIED));
        assertThat(fx.controller.hits).hasValue(0);
    }

    @Test
    void missingHeader_is401() throws Exception {
        mvc.perform(consume(accountA))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer realm=\"service\""))
                .andExpect(jsonPath("$.code").value(ErrorResponse.SERVICE_TOKEN_INVALID));
        assertThat(fx.controller.hits).hasValue(0);
    }

    @Test
    void publicPath_isUntouched_noTokenNeeded_noAttributes() throws Exception {
        mvc.perform(get("/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pong").value(true))
                .andExpect(jsonPath("$.caller").doesNotExist())
                .andExpect(jsonPath("$.background").value(false));
        // gecersiz bir token bile public path'i etkilemez
        mvc.perform(get("/v1/ping").header(HDR, "garbage")).andExpect(status().isOk());
        assertThat(fx.controller.hits).hasValue(2);
    }

    // ---- token dogrulama adimlari (hepsi 401 SERVICE_TOKEN_INVALID, mesaj sebebi sizdirmaz) -------------------

    void assert401(String token) throws Exception {
        mvc.perform(consume(accountA).header(HDR, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorResponse.SERVICE_TOKEN_INVALID))
                .andExpect(jsonPath("$.message").value("Service token invalid"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("kid"))));
        assertThat(fx.controller.hits).hasValue(0);
    }

    RawTokens.Builder validShape() {
        return RawTokens.token().kid("order-1").iss(ORDER).aud(AUDIENCE).sub(accountA)
                .iat(now()).exp(now().plusSeconds(60));
    }

    @Test
    void wrongAudience_is401() throws Exception {
        assert401(fx.order.mint("order-api", accountA));
    }

    @Test
    void multiAudienceContainingOurs_isStillRejected() throws Exception {
        String token = validShape().signEd25519(fx.orderKey);
        // ayni claim'lerle ama aud=[ours, other]: tam esitlik sarti
        var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder(validShape().claims())
                .audience(java.util.List.of(AUDIENCE, "other-api")).build();
        var jwt = new com.nimbusds.jwt.SignedJWT(validShape().header(), claims);
        jwt.sign(new com.acme.platform.security.jwt.JdkEd25519Signer(fx.orderKey));
        assertThat(token).isNotEqualTo(jwt.serialize());
        assert401(jwt.serialize());
    }

    @Test
    void expired_is401_butWithinSkewStillAccepted() throws Exception {
        String token = fx.order.mint(AUDIENCE, accountA);               // exp = now + 60s
        fx.clock.advance(Duration.ofSeconds(60 + 29));                   // 30 sn tolerans icinde
        mvc.perform(consume(accountA).header(HDR, token)).andExpect(status().isOk());
        assertThat(fx.controller.hits).hasValue(1);
        fx.controller.hits.set(0);
        fx.clock.advance(Duration.ofSeconds(2));                         // 60 + 31 sn: tolerans disinda
        assert401(token);
    }

    @Test
    void notBeforeInFuture_is401_butWithinSkewAccepted() throws Exception {
        assert401(validShape().nbf(now().plusSeconds(31)).signEd25519(fx.orderKey));
        mvc.perform(consume(accountA).header(HDR, validShape().nbf(now().plusSeconds(29)).signEd25519(fx.orderKey)))
                .andExpect(status().isOk());
    }

    @Test
    void hs256SignedWithPublicKeyBytes_algConfusion_is401() throws Exception {
        // saldirgan public anahtari (JWKS'ten okunabilir) HMAC secret'i olarak kullanir
        byte[] publicKeyBytes = fx.orderKey.toPublicJWK().getDecodedX();
        assert401(validShape().signHs256(publicKeyBytes));
    }

    @Test
    void algNone_is401() throws Exception {
        assert401(validShape().unsecured());
    }

    @Test
    void unknownKid_is401() throws Exception {
        assert401(validShape().kid("rogue-1").signEd25519(fx.rogueKey));
    }

    @Test
    void unknownIssuer_is401() throws Exception {
        assert401(validShape().iss("evil-service").signEd25519(fx.orderKey));
    }

    @Test
    void issuerImpersonation_keyOfAnotherIssuer_is401() throws Exception {
        // backoffice kendi anahtariyla (kid backoffice-1) iss=order-service diyor: kid o issuer altinda kayitli degil
        assert401(validShape().kid("backoffice-1").iss(ORDER).signEd25519(fx.backofficeKey));
    }

    @Test
    void actDifferentFromIss_is401() throws Exception {
        // gercek saldiri: backoffice kendi gecerli anahtariyla act=order-service soyleyip consume kuralini gecmeye calisir
        assert401(validShape().kid("backoffice-1").iss(BACKOFFICE).act(ORDER).signEd25519(fx.backofficeKey));
        assert401(validShape().act(BACKOFFICE).signEd25519(fx.orderKey));
    }

    @Test
    void wrongTyp_is401() throws Exception {
        assert401(validShape().typ("JWT").signEd25519(fx.orderKey));
        assert401(validShape().typ(null).signEd25519(fx.orderKey));
    }

    @Test
    void tamperedPayload_is401() throws Exception {
        String token = fx.order.mint(AUDIENCE, accountA);
        String[] parts = token.split("\\.");
        String forged = com.nimbusds.jose.util.Base64URL.encode(
                new String(com.nimbusds.jose.util.Base64URL.from(parts[1]).decode(), java.nio.charset.StandardCharsets.UTF_8)
                        .replace(accountA.toString(), accountB.toString())).toString();
        assert401(parts[0] + "." + forged + "." + parts[2]);
    }

    @Test
    void garbageToken_is401() throws Exception {
        assert401("not.a.jwt");
        assert401("eyJhbGciOiJFZERTQSJ9.e30.");                            // bos imza
        mvc.perform(consume(accountA).header(HDR, ""))                     // bos header = eksik token
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorResponse.SERVICE_TOKEN_INVALID));
    }

    // ---- path normalize (Bolum 9.4 adim 0): 400, public kurallara geri dusmez --------------------------------

    void assertPathInvalid(String rawUri) throws Exception {
        String token = fx.gateway.mint(AUDIENCE, null);                  // gecerli token bile kurtarmaz
        mvc.perform(get(URI.create(rawUri)).header(HDR, token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorResponse.INTERNAL_PATH_INVALID));
        assertThat(fx.controller.hits).as(rawUri).hasValue(0);
    }

    @Test
    void encodedTraversalOutOfInternal_isRejected_notMatchedAsPublic() throws Exception {
        assertPathInvalid("/internal/..%2Fv1/ping");
    }

    @Test
    void encodedDotSegmentsInsideInternal_areRejected() throws Exception {
        assertPathInvalid("/internal/subscription/%2e%2e/admin/keys");
        assertPathInvalid("/internal/subscription/%2E%2E/admin/keys");
        assertPathInvalid("/internal/./subscription/reconcile");
    }

    @Test
    void doubleEncoding_isRejected() throws Exception {
        assertPathInvalid("/internal/%252e%252e/v1/ping");
        assertPathInvalid("/internal/subscription/%252Fadmin");
    }

    @Test
    void backslashEmptySegmentAndMatrixParam_areRejected() throws Exception {
        assertPathInvalid("/internal/subscription/%5C..%5Cadmin/keys");
        assertPathInvalid("/internal//subscription/reconcile");
        assertPathInvalid("/internal/subscription/..;/admin/keys");
    }

    @Test
    void traversalFromPublicIntoInternal_isRejected() throws Exception {
        assertPathInvalid("/v1/..%2Finternal/subscription/reconcile");
        assertPathInvalid("/v1/../internal/subscription/reconcile");
    }

    @Test
    void encodedButHarmlessInternalPath_isNormalizedBeforeAllowlist() throws Exception {
        // %61 = 'a': allowlist ham URI ile degil, decode edilmis path ile eslesir (Bolum 9.4 kural)
        String raw = "/internal/subscription/%61ccounts/" + accountA + "/balance";
        mvc.perform(get(URI.create(raw)).header(HDR, fx.order.mint(AUDIENCE, accountA))).andExpect(status().isOk());
    }

    // ---- delegasyon (Bolum 9.2.1) ------------------------------------------------------------------------------

    @Test
    void requiredUserContext_withBackgroundToken_is403_DELEGATION_DENIED() throws Exception {
        mvc.perform(consume(accountA).header(HDR, fx.order.mint(AUDIENCE, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorResponse.DELEGATION_DENIED));
        assertThat(fx.controller.hits).hasValue(0);
    }

    @Test
    void requiredUserContext_subjectDiffersFromPathAccount_is403() throws Exception {
        mvc.perform(consume(accountB).header(HDR, fx.order.mint(AUDIENCE, accountA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorResponse.DELEGATION_DENIED));
        assertThat(fx.controller.hits).hasValue(0);
    }

    @Test
    void forbiddenUserContext_backgroundOk_subjectPresent403() throws Exception {
        mvc.perform(post("/internal/subscription/reconcile").header(HDR, fx.worker.mint(AUDIENCE, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caller").value(WORKER))
                .andExpect(jsonPath("$.background").value(true))
                .andExpect(jsonPath("$.accountAttr").doesNotExist());
        mvc.perform(post("/internal/subscription/reconcile").header(HDR, fx.worker.mint(AUDIENCE, accountA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorResponse.DELEGATION_DENIED));
        assertThat(fx.controller.hits).hasValue(1);
    }

    @Test
    void actorAllowlistedButWithoutDelegationRule_is403() throws Exception {
        // reconcile allowlist'i backoffice'e acik ama delegasyon matrisinde satiri yok
        mvc.perform(post("/internal/subscription/reconcile").header(HDR, fx.backoffice.mint(AUDIENCE, null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorResponse.DELEGATION_DENIED));
    }

    @Test
    void currentAccountRequired_withBackgroundTokenOnOptionalOperation_is401() throws Exception {
        String url = "/internal/subscription/accounts/" + accountA + "/profile";
        mvc.perform(post(url).header(HDR, fx.backoffice.mint(AUDIENCE, null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorResponse.ACCOUNT_CONTEXT_REQUIRED));
        mvc.perform(post(url).header(HDR, fx.backoffice.mint(AUDIENCE, accountB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value(accountB.toString()));
        assertThat(fx.controller.hits).hasValue(1);
    }

    // ---- rotasyon (Bolum 9.2) ----------------------------------------------------------------------------------

    @Test
    void keyRotation_newKidAfterRegister_oldKidUntilRemoved() throws Exception {
        var newKey = com.acme.platform.security.jwt.Ed25519Keys.generate("order-2");
        var newSigner = new ServiceJwtSigner(newKey, ORDER, fx.clock);

        assert401(newSigner.mint(AUDIENCE, accountA));                    // henuz yayinlanmadi
        fx.registry.register(ORDER, newKey.toPublicJWK());
        mvc.perform(consume(accountA).header(HDR, newSigner.mint(AUDIENCE, accountA))).andExpect(status().isOk());
        mvc.perform(consume(accountA).header(HDR, fx.order.mint(AUDIENCE, accountA))).andExpect(status().isOk()); // eski hala gecerli

        assertThat(fx.registry.remove(ORDER, "order-1")).isTrue();
        fx.controller.hits.set(0);
        assert401(fx.order.mint(AUDIENCE, accountA));                    // eski anahtar dusuruldu
        mvc.perform(consume(accountA).header(HDR, newSigner.mint(AUDIENCE, accountA))).andExpect(status().isOk());
    }

    @Test
    void signerAlwaysUsesEdDSA_andTokenCarriesRequiredClaims() throws Exception {
        String token = fx.order.mint(AUDIENCE, accountA);
        var jwt = com.nimbusds.jwt.SignedJWT.parse(token);
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.EdDSA);
        assertThat(jwt.getHeader().getType()).isEqualTo(ServiceJwtSigner.SERVICE_JWT_TYPE);
        assertThat(jwt.getHeader().getKeyID()).isEqualTo("order-1");
        var c = jwt.getJWTClaimsSet();
        assertThat(c.getIssuer()).isEqualTo(ORDER);
        assertThat(c.getAudience()).containsExactly(AUDIENCE);
        assertThat(c.getStringClaim("act")).isEqualTo(ORDER);
        assertThat(c.getSubject()).isEqualTo(accountA.toString());
        assertThat(c.getJWTID()).isNotBlank();
        assertThat(c.getExpirationTime().toInstant()).isEqualTo(now().plusSeconds(60));
        assertThat(c.getIssueTime().toInstant()).isEqualTo(now());
        assertThat(fx.order.mint(AUDIENCE, null)).isNotEqualTo(token);   // jti her seferinde farkli
    }
}
