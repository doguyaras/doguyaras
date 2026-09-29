package com.acme.platform.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Set;

/**
 * JDK yerli EdDSA ile dogrulayan Nimbus JWSVerifier. Header'daki alg EdDSA degilse imzaya hic bakmadan reddeder:
 * "alg confusion" (public anahtari HMAC secret'i gibi kullanma) bu katmanda da kapali kalir.
 */
public final class JdkEd25519Verifier implements JWSVerifier {

    private final PublicKey publicKey;
    private final JCAContext jcaContext = new JCAContext();

    public JdkEd25519Verifier(OctetKeyPair jwk) {
        this.publicKey = Ed25519Keys.toPublicKey(jwk);
    }

    @Override
    public boolean verify(JWSHeader header, byte[] signingInput, Base64URL signature) throws JOSEException {
        if (!JWSAlgorithm.EdDSA.equals(header.getAlgorithm())) return false;
        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(publicKey);
            sig.update(signingInput);
            return sig.verify(signature.decode());
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;                                              // bozuk imza = gecersiz, istisna degil
        }
    }

    @Override public Set<JWSAlgorithm> supportedJWSAlgorithms() { return Set.of(JWSAlgorithm.EdDSA); }
    @Override public JCAContext getJCAContext() { return jcaContext; }
}
