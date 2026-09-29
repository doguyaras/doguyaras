package com.acme.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.platform.security.jwt.Ed25519Keys;
import com.acme.platform.security.jwt.ServiceJwtKeyRegistry;
import com.acme.platform.security.jwt.ServiceJwtSigner;
import com.acme.platform.security.jwt.ServiceJwtVerifier;
import com.acme.platform.security.support.MutableClock;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetKeyPair;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** kid kaydi: iki aktif anahtar, kaldirma, JWKS JSON'u (public-only) ve dosya bicimi round-trip. */
class ServiceJwtKeyRegistryTest {

    final ServiceJwtKeyRegistry registry = new ServiceJwtKeyRegistry();
    final OctetKeyPair k1 = Ed25519Keys.generate("gw-1");
    final OctetKeyPair k2 = Ed25519Keys.generate("gw-2");

    @Test
    void twoActiveKeysThenRemoval() {
        registry.register("gateway", k1);
        registry.register("gateway", k2);
        assertThat(registry.find("gateway", "gw-1")).isPresent();
        assertThat(registry.find("gateway", "gw-2")).isPresent();
        assertThat(registry.find("order-service", "gw-1")).as("kid issuer'a baglidir").isEmpty();
        assertThat(registry.remove("gateway", "gw-1")).isTrue();
        assertThat(registry.remove("gateway", "gw-1")).isFalse();
        assertThat(registry.find("gateway", "gw-1")).isEmpty();
        assertThat(registry.find("gateway", "gw-2")).isPresent();
    }

    @Test
    void jwksJsonIsPublicOnly_andParseable() throws Exception {
        registry.register("gateway", k1);                                    // private JWK verilse bile
        String jwks = registry.toJwksJson("gateway");
        assertThat(jwks).doesNotContain("\"d\"").contains("\"kid\":\"gw-1\"").contains("\"crv\":\"Ed25519\"");
        JWKSet parsed = JWKSet.parse(jwks);
        assertThat(parsed.getKeyByKeyId("gw-1")).isNotNull();
        assertThat(parsed.getKeyByKeyId("gw-1").isPrivate()).isFalse();
        assertThat(registry.find("gateway", "gw-1").orElseThrow().isPrivate()).isFalse();
    }

    @Test
    void fileFormatRoundTrip_verifiesTokensSignedWithOriginalPrivateKey() {
        registry.register("gateway", k1);
        registry.register("order-service", k2);
        String json = registry.toJson();
        assertThat(json).doesNotContain("\"d\"");

        ServiceJwtKeyRegistry loaded = ServiceJwtKeyRegistry.fromJson(json);
        assertThat(loaded.issuers()).containsExactlyInAnyOrder("gateway", "order-service");
        MutableClock clock = new MutableClock();
        String token = new ServiceJwtSigner(k2, "order-service", clock).mint("subscription-api", null);
        var verifier = new ServiceJwtVerifier(loaded, Set.of("gateway", "order-service"), "subscription-api", clock);
        assertThat(verifier.verify(token).actor()).isEqualTo("order-service");
    }

    @Test
    void rejectsKeyWithoutKid_andNonEd25519() {
        OctetKeyPair noKid = new OctetKeyPair.Builder(k1.toPublicJWK()).keyID(null).build();
        assertThatThrownBy(() -> registry.register("gateway", noKid)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServiceJwtKeyRegistry.fromJson("{\"gateway\": {\"keys\": [{\"kty\":\"oct\",\"k\":\"AAAA\",\"kid\":\"x\"}]}}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void jdkKeyConversion_roundTripsThroughJwkBytes() throws Exception {
        // JWK -> JDK -> imza -> dogrulama: x/d kodlamasi RFC 8032 ile uyumlu
        var sig = java.security.Signature.getInstance("Ed25519");
        sig.initSign(Ed25519Keys.toPrivateKey(k1));
        sig.update("payload".getBytes());
        byte[] s = sig.sign();
        var ver = java.security.Signature.getInstance("Ed25519");
        ver.initVerify(Ed25519Keys.toPublicKey(k1.toPublicJWK()));
        ver.update("payload".getBytes());
        assertThat(ver.verify(s)).isTrue();
        // 200 rastgele anahtar: y'nin onde sifir/isaret byte'i olan kodlamalari da dogru cevrilir
        for (int i = 0; i < 200; i++) {
            OctetKeyPair k = Ed25519Keys.generate("k" + i);
            assertThat(k.getDecodedX()).hasSize(32);
            var s2 = java.security.Signature.getInstance("Ed25519");
            s2.initSign(Ed25519Keys.toPrivateKey(k));
            s2.update(new byte[] {(byte) i});
            var v2 = java.security.Signature.getInstance("Ed25519");
            v2.initVerify(Ed25519Keys.toPublicKey(k.toPublicJWK()));   // yalniz x'ten kurulan public anahtar
            v2.update(new byte[] {(byte) i});
            assertThat(v2.verify(s2.sign())).as("key %d", i).isTrue();
        }
    }
}
