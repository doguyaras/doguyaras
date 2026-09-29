package com.acme.platform.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Set;

/** JDK yerli EdDSA ile imzalayan Nimbus JWSSigner (Tink gerektirmez). Yalniz alg=EdDSA header'i imzalar. */
public final class JdkEd25519Signer implements JWSSigner {

    private final PrivateKey privateKey;
    private final JCAContext jcaContext = new JCAContext();

    public JdkEd25519Signer(OctetKeyPair jwk) {
        this.privateKey = Ed25519Keys.toPrivateKey(jwk);
    }

    @Override
    public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
        if (!JWSAlgorithm.EdDSA.equals(header.getAlgorithm())) {
            throw new JOSEException("Unsupported JWS algorithm " + header.getAlgorithm() + ", expected EdDSA");
        }
        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initSign(privateKey);
            sig.update(signingInput);
            return Base64URL.encode(sig.sign());
        } catch (GeneralSecurityException e) {
            throw new JOSEException("Ed25519 signing failed", e);
        }
    }

    @Override public Set<JWSAlgorithm> supportedJWSAlgorithms() { return Set.of(JWSAlgorithm.EdDSA); }
    @Override public JCAContext getJCAContext() { return jcaContext; }
}
