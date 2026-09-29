package com.acme.platform.security.support;

import com.acme.platform.security.jwt.JdkEd25519Signer;
import com.acme.platform.security.jwt.ServiceJwtSigner;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Uretim imzalayicisinin ASLA uretmeyecegi token'lari (yanlis alg, typ, nbf, act != iss...) kurmak icin
 * test tarafi kurucu. Saldirganin elindeki serbestligi temsil eder.
 */
public final class RawTokens {

    private RawTokens() {}

    public static final class Builder {
        private JWSAlgorithm alg = JWSAlgorithm.EdDSA;
        private JOSEObjectType typ = ServiceJwtSigner.SERVICE_JWT_TYPE;
        private String kid;
        private String iss;
        private String act;
        private boolean actSet;
        private String aud;
        private String sub;
        private Instant iat;
        private Instant exp;
        private Instant nbf;
        private String jti = UUID.randomUUID().toString();

        public Builder alg(JWSAlgorithm v) { alg = v; return this; }
        public Builder typ(String v) { typ = v == null ? null : new JOSEObjectType(v); return this; }
        public Builder kid(String v) { kid = v; return this; }
        public Builder iss(String v) { iss = v; if (!actSet) act = v; return this; }
        public Builder act(String v) { act = v; actSet = true; return this; }
        public Builder aud(String v) { aud = v; return this; }
        public Builder sub(UUID v) { sub = v == null ? null : v.toString(); return this; }
        public Builder sub(String v) { sub = v; return this; }
        public Builder iat(Instant v) { iat = v; return this; }
        public Builder exp(Instant v) { exp = v; return this; }
        public Builder nbf(Instant v) { nbf = v; return this; }

        public JWTClaimsSet claims() {
            JWTClaimsSet.Builder c = new JWTClaimsSet.Builder().issuer(iss).jwtID(jti);
            if (aud != null) c.audience(List.of(aud));
            if (act != null) c.claim(ServiceJwtSigner.ACTOR_CLAIM, act);
            if (sub != null) c.subject(sub);
            if (iat != null) c.issueTime(Date.from(iat));
            if (exp != null) c.expirationTime(Date.from(exp));
            if (nbf != null) c.notBeforeTime(Date.from(nbf));
            return c.build();
        }

        public JWSHeader header() {
            JWSHeader.Builder h = new JWSHeader.Builder(alg).keyID(kid);
            if (typ != null) h.type(typ);
            return h.build();
        }

        /** Ed25519 ile imzala (gecerli anahtar, gecersiz icerik senaryolari). */
        public String signEd25519(OctetKeyPair privateKey) {
            return sign(new JdkEd25519Signer(privateKey));
        }

        /** HS256 "alg confusion": secret olarak public anahtarin ham byte'lari (klasik saldiri). */
        public String signHs256(byte[] secret) {
            try {
                alg = JWSAlgorithm.HS256;
                return sign(new MACSigner(secret));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /** alg=none: imza kismi bos. Nimbus PlainHeader kid tasiyamadigi icin elle serilestirilir. */
        public String unsecured() {
            String header = "{\"alg\":\"none\",\"typ\":\"" + typ.getType() + "\",\"kid\":\"" + kid + "\"}";
            return Base64URL.encode(header.getBytes(StandardCharsets.UTF_8)) + "."
                    + Base64URL.encode(claims().toString().getBytes(StandardCharsets.UTF_8)) + ".";
        }

        private String sign(JWSSigner signer) {
            try {
                SignedJWT jwt = new SignedJWT(header(), claims());
                jwt.sign(signer);
                return jwt.serialize();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static Builder token() { return new Builder(); }
}
