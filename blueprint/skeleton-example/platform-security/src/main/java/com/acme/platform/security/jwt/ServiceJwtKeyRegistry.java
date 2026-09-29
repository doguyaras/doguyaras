package com.acme.platform.security.jwt;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.JSONObjectUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * iss -> (kid -> public JWK) kaydi (referans Bolum 9.2 "iss -> JWKS eslemesi"). Anahtar YALNIZ kayitli oldugu
 * issuer icin gecerlidir: order-service'in anahtariyla imzalanmis bir token iss=gateway diyemez. Rotasyon icin ayni
 * issuer altinda birden fazla kid ayni anda aktif olabilir; eski anahtar TTL sonunda remove ile dusurulur, restart yok.
 *
 * JSON bicimi (jwks-path dosyasi): {"gateway": {"keys":[...]}, "order-service": {"keys":[...]}}
 */
public final class ServiceJwtKeyRegistry {

    private final Map<String, Map<String, OctetKeyPair>> keysByIssuer = new ConcurrentHashMap<>();

    /** Public parcayi kaydeder; yanlislikla private JWK verilse bile "d" alani dusurulur. */
    public void register(String issuer, OctetKeyPair jwk) {
        Objects.requireNonNull(issuer, "issuer");
        Ed25519Keys.requireEd25519(jwk);
        if (jwk.getKeyID() == null || jwk.getKeyID().isBlank()) throw new IllegalArgumentException("JWK without kid");
        keysByIssuer.computeIfAbsent(issuer, i -> new ConcurrentHashMap<>()).put(jwk.getKeyID(), jwk.toPublicJWK());
    }

    public boolean remove(String issuer, String kid) {
        Map<String, OctetKeyPair> keys = keysByIssuer.get(issuer);
        return keys != null && keys.remove(kid) != null;
    }

    public Optional<OctetKeyPair> find(String issuer, String kid) {
        if (issuer == null || kid == null) return Optional.empty();
        Map<String, OctetKeyPair> keys = keysByIssuer.get(issuer);
        return keys == null ? Optional.empty() : Optional.ofNullable(keys.get(kid));
    }

    public Set<String> issuers() { return Collections.unmodifiableSet(keysByIssuer.keySet()); }

    /** Tek bir issuer'in RFC 7517 JWKS temsili (public anahtarlar). */
    public String toJwksJson(String issuer) {
        Map<String, OctetKeyPair> keys = keysByIssuer.getOrDefault(issuer, Map.of());
        return new JWKSet(new ArrayList<JWK>(keys.values())).toString(true);
    }

    /** Tum kaydin dosya bicimi: iss -> JWKS. */
    public String toJson() {
        Map<String, Object> out = new LinkedHashMap<>();
        keysByIssuer.forEach((iss, keys) -> out.put(iss, new JWKSet(new ArrayList<JWK>(keys.values())).toJSONObject(true)));
        return JSONObjectUtils.toJSONString(out);
    }

    public static ServiceJwtKeyRegistry fromJson(String json) {
        ServiceJwtKeyRegistry registry = new ServiceJwtKeyRegistry();
        try {
            Map<String, Object> root = JSONObjectUtils.parse(json);
            for (String issuer : root.keySet()) {
                JWKSet set = JWKSet.parse(JSONObjectUtils.getJSONObject(root, issuer));
                List<JWK> keys = set.getKeys();
                for (JWK jwk : keys) {
                    if (!(jwk instanceof OctetKeyPair okp)) {
                        throw new IllegalArgumentException("Issuer " + issuer + " key " + jwk.getKeyID() + " is not an OKP key");
                    }
                    registry.register(issuer, okp);
                }
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid service JWKS document", e);
        }
        return registry;
    }
}
