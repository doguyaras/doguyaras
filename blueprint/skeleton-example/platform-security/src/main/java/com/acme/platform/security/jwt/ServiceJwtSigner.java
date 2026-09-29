package com.acme.platform.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Servis JWT ureticisi (referans Bolum 9.2). Her servis KENDI Ed25519 private anahtariyla imzalar; iss = imzalayan
 * servisin adi. Token cok kisa omurludur (varsayilan 60 sn): calinsa bile pencere dardir ve replay deposu gerekmez.
 * typ="service+jwt" ile user/admin JWT'lerinden ayrisir (RFC 8725 3.11: yuzeyler birbirinin yerine gecmez).
 */
public final class ServiceJwtSigner {

    public static final JOSEObjectType SERVICE_JWT_TYPE = new JOSEObjectType("service+jwt");
    public static final String ACTOR_CLAIM = "act";
    public static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    private final OctetKeyPair key;
    private final JdkEd25519Signer jwsSigner;
    private final String serviceName;
    private final Duration ttl;
    private final Clock clock;

    public ServiceJwtSigner(OctetKeyPair privateKey, String serviceName, Duration ttl, Clock clock) {
        if (privateKey.getKeyID() == null || privateKey.getKeyID().isBlank()) {
            throw new IllegalArgumentException("Signing key must carry a kid (rotation depends on it)");
        }
        if (ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("Service JWT ttl must be within (0, 5m]: " + ttl);
        }
        this.key = privateKey;
        this.jwsSigner = new JdkEd25519Signer(privateKey);
        this.serviceName = Objects.requireNonNull(serviceName, "serviceName");
        this.ttl = ttl;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ServiceJwtSigner(OctetKeyPair privateKey, String serviceName, Clock clock) {
        this(privateKey, serviceName, DEFAULT_TTL, clock);
    }

    public String serviceName() { return serviceName; }
    public String kid() { return key.getKeyID(); }
    /** JWKS'e konacak public parca; private "d" alani asla disari cikmaz. */
    public OctetKeyPair publicJwk() { return key.toPublicJWK(); }

    /**
     * @param audience hedef servisin aud'u
     * @param subject  kullanici istegi baglaminda calisan cagri icin hesap kimligi; arka plan isi icin null
     */
    public String mint(String audience, UUID subject) {
        Instant now = clock.instant();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(serviceName)
                .audience(List.of(Objects.requireNonNull(audience, "audience")))
                .claim(ACTOR_CLAIM, serviceName)                        // act == iss: dogrulayan bunu sart kosar
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)))
                .jwtID(UUID.randomUUID().toString());
        if (subject != null) claims.subject(subject.toString());
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA).type(SERVICE_JWT_TYPE).keyID(key.getKeyID()).build();
        SignedJWT jwt = new SignedJWT(header, claims.build());
        try {
            jwt.sign(jwsSigner);
        } catch (JOSEException e) {
            throw new IllegalStateException("Service JWT signing failed", e);
        }
        return jwt.serialize();
    }
}
