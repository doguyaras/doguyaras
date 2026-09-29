package com.acme.platform.security.client;

import com.acme.platform.security.jwt.ServiceJwtSigner;
import com.acme.platform.security.web.ServiceJwtVerificationFilter;
import com.acme.platform.security.web.ServiceRequestAttributes;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * RestClient interceptor'u: her istek icin TAZE token basar (cache yok; TTL zaten 60 sn ve imza ucuz), hedef aud
 * sabittir (bir RestClient = bir hedef servis), act = kendi adimiz, sub = holder'dan gelen mevcut hesap.
 * Varsayilan holder mevcut HTTP istegindeki x.accountId'dir: servis yalniz kendi akisinda gordugu sub'i aktarir
 * (Bolum 9.2.1). Arka plan isleri bos holder ile calisir ve sub tasimaz.
 */
public class ServiceJwtClientInterceptor implements ClientHttpRequestInterceptor {

    private final ServiceJwtSigner signer;
    private final String targetAudience;
    private final Supplier<Optional<UUID>> accountContext;

    public ServiceJwtClientInterceptor(ServiceJwtSigner signer, String targetAudience, Supplier<Optional<UUID>> accountContext) {
        this.signer = Objects.requireNonNull(signer, "signer");
        this.targetAudience = Objects.requireNonNull(targetAudience, "targetAudience");
        this.accountContext = Objects.requireNonNull(accountContext, "accountContext");
    }

    /** Mevcut istegin hesabini aktaran varsayilan kablolama. */
    public ServiceJwtClientInterceptor(ServiceJwtSigner signer, String targetAudience) {
        this(signer, targetAudience, ServiceRequestAttributes::currentAccountId);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        UUID subject = accountContext.get().orElse(null);
        request.getHeaders().set(ServiceJwtVerificationFilter.SERVICE_AUTH_HEADER, signer.mint(targetAudience, subject));
        return execution.execute(request, body);
    }
}
