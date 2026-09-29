package com.acme.platform.security.jwt;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;

/**
 * Ed25519 anahtarlarini JWK (OKP) ile JDK anahtar tipleri arasinda cevirir. Nimbus'un kendi Ed25519Signer'i
 * opsiyonel Google Tink bagimliligi ister; JDK 15+ EdDSA'yi yerli destekledigi icin ek kutuphane tasimiyoruz
 * (daha kucuk saldiri yuzeyi, daha az CVE takibi).
 *
 * RFC 8032 kodlamasi: public anahtar = y koordinatinin 32 byte little-endian hali, en ust bit x'in tekligi;
 * private anahtar = 32 byte tohum. Nimbus OKP "x"/"d" alanlari bu byte dizilerini base64url tasir.
 */
public final class Ed25519Keys {

    private static final String ALGORITHM = "Ed25519";
    private static final int KEY_BYTES = 32;

    private Ed25519Keys() {}

    /** Yeni imzalama cifti; kid ile isaretlenir. Uretim ortaminda anahtar dosyadan gelir, bu metod test/bootstrap icindir. */
    public static OctetKeyPair generate(String kid) {
        try {
            KeyPair pair = KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair();
            EdECPublicKey pub = (EdECPublicKey) pair.getPublic();
            EdECPrivateKey priv = (EdECPrivateKey) pair.getPrivate();
            byte[] seed = priv.getBytes().orElseThrow(() -> new IllegalStateException("Ed25519 private key is not extractable"));
            return new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(encodePoint(pub.getPoint())))
                    .d(Base64URL.encode(seed)).keyID(kid).keyUse(KeyUse.SIGNATURE).build();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Ed25519 key generation failed", e);
        }
    }

    public static PublicKey toPublicKey(OctetKeyPair jwk) {
        requireEd25519(jwk);
        byte[] raw = jwk.getDecodedX();
        if (raw.length != KEY_BYTES) throw new IllegalArgumentException("Ed25519 public key must be 32 bytes");
        byte[] le = raw.clone();
        boolean xOdd = (le[KEY_BYTES - 1] & 0x80) != 0;
        le[KEY_BYTES - 1] &= 0x7F;
        BigInteger y = new BigInteger(1, reverse(le));
        try {
            return KeyFactory.getInstance(ALGORITHM)
                    .generatePublic(new EdECPublicKeySpec(NamedParameterSpec.ED25519, new EdECPoint(xOdd, y)));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid Ed25519 public key", e);
        }
    }

    public static PrivateKey toPrivateKey(OctetKeyPair jwk) {
        requireEd25519(jwk);
        if (!jwk.isPrivate()) throw new IllegalArgumentException("JWK " + jwk.getKeyID() + " has no private part");
        try {
            return KeyFactory.getInstance(ALGORITHM)
                    .generatePrivate(new EdECPrivateKeySpec(NamedParameterSpec.ED25519, jwk.getDecodedD()));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid Ed25519 private key", e);
        }
    }

    static void requireEd25519(OctetKeyPair jwk) {
        if (!Curve.Ed25519.equals(jwk.getCurve())) {
            throw new IllegalArgumentException("Only Ed25519 keys are supported, got " + jwk.getCurve());
        }
    }

    private static byte[] encodePoint(EdECPoint point) {
        byte[] be = point.getY().toByteArray();                    // big-endian, isaret byte'i olabilir
        byte[] le = new byte[KEY_BYTES];
        for (int i = 0; i < KEY_BYTES && i < be.length; i++) le[i] = be[be.length - 1 - i];
        if (point.isXOdd()) le[KEY_BYTES - 1] |= (byte) 0x80;
        return le;
    }

    private static byte[] reverse(byte[] in) {
        byte[] out = Arrays.copyOf(in, in.length);
        for (int i = 0; i < out.length / 2; i++) { byte t = out[i]; out[i] = out[out.length - 1 - i]; out[out.length - 1 - i] = t; }
        return out;
    }
}
