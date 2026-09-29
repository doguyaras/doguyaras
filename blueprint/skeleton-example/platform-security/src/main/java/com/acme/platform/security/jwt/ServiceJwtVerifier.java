package com.acme.platform.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Servis JWT dogrulayici (referans Bolum 9.4 adim 3 ve 5). Kontrol sirasi sabittir ve her biri fail-closed'dur:
 * alg=EdDSA (HS256 ve none reddedilir) -> typ=service+jwt -> iss bilinen imzalayici -> kid o issuer altinda kayitli
 * -> imza -> aud == bu servis -> exp/nbf (30 sn tolerans) -> act == iss -> sub UUID.
 * Zaman Clock'tan gelir: sure testleri gercek zaman beklemeden calisir.
 */
public final class ServiceJwtVerifier {

    public static final Duration DEFAULT_CLOCK_SKEW = Duration.ofSeconds(30);

    private final ServiceJwtKeyRegistry registry;
    private final Set<String> knownIssuers;
    private final String audience;
    private final Duration clockSkew;
    private final Clock clock;

    public ServiceJwtVerifier(ServiceJwtKeyRegistry registry, Set<String> knownIssuers, String audience,
                              Duration clockSkew, Clock clock) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.knownIssuers = Set.copyOf(knownIssuers);
        this.audience = Objects.requireNonNull(audience, "audience");
        this.clockSkew = Objects.requireNonNull(clockSkew, "clockSkew");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public ServiceJwtVerifier(ServiceJwtKeyRegistry registry, Set<String> knownIssuers, String audience, Clock clock) {
        this(registry, knownIssuers, audience, DEFAULT_CLOCK_SKEW, clock);
    }

    public ServiceIdentity verify(String token) {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token);                                    // alg=none (PlainJWT) burada duser
        } catch (ParseException e) {
            throw new ServiceTokenInvalidException("token is not a signed JWT", e);
        }
        JWSHeader header = jwt.getHeader();
        if (!JWSAlgorithm.EdDSA.equals(header.getAlgorithm())) {
            throw new ServiceTokenInvalidException("alg must be EdDSA, got " + header.getAlgorithm());
        }
        if (!ServiceJwtSigner.SERVICE_JWT_TYPE.equals(header.getType())) {
            throw new ServiceTokenInvalidException("typ must be service+jwt");
        }
        JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new ServiceTokenInvalidException("claims are not parseable", e);
        }
        String issuer = claims.getIssuer();
        if (issuer == null || !knownIssuers.contains(issuer)) {
            throw new ServiceTokenInvalidException("unknown issuer");
        }
        OctetKeyPair key = registry.find(issuer, header.getKeyID())
                .orElseThrow(() -> new ServiceTokenInvalidException("kid not registered for issuer"));
        try {
            if (!jwt.verify(new JdkEd25519Verifier(key))) throw new ServiceTokenInvalidException("signature invalid");
        } catch (JOSEException e) {
            throw new ServiceTokenInvalidException("signature verification error", e);
        }
        if (!List.of(audience).equals(claims.getAudience())) {              // tam esitlik: coklu aud kabul edilmez
            throw new ServiceTokenInvalidException("audience mismatch");
        }
        Instant now = clock.instant();
        Date exp = claims.getExpirationTime();
        if (exp == null || !now.isBefore(exp.toInstant().plus(clockSkew))) {
            throw new ServiceTokenInvalidException("token expired");
        }
        Date nbf = claims.getNotBeforeTime();
        if (nbf != null && now.isBefore(nbf.toInstant().minus(clockSkew))) {
            throw new ServiceTokenInvalidException("token not yet valid");
        }
        String actor;
        try {
            actor = claims.getStringClaim(ServiceJwtSigner.ACTOR_CLAIM);
        } catch (ParseException e) {
            throw new ServiceTokenInvalidException("act claim malformed", e);
        }
        if (actor == null) actor = issuer;
        else if (!actor.equals(issuer)) {
            // Bolum 9.4 adim 5: act == iss. Aksi halde herhangi bir servis kendi anahtariyla baskasinin adini soyler.
            throw new ServiceTokenInvalidException("act does not match iss");
        }
        UUID accountId = null;
        if (claims.getSubject() != null) {
            try {
                accountId = UUID.fromString(claims.getSubject());
            } catch (IllegalArgumentException e) {
                throw new ServiceTokenInvalidException("sub is not a UUID");
            }
        }
        return new ServiceIdentity(actor, issuer, accountId, claims.getJWTID());
    }
}
