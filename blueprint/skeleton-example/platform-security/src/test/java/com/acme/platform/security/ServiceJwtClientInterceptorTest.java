package com.acme.platform.security;

import static com.acme.platform.security.support.SecurityFixture.AUDIENCE;
import static com.acme.platform.security.support.SecurityFixture.ORDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.platform.security.client.ServiceJwtClientInterceptor;
import com.acme.platform.security.jwt.ServiceIdentity;
import com.acme.platform.security.support.SecurityFixture;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Istemci tarafi: RestClient interceptor'u her istekte taze token basar; token gercek filtre tarafindan dogrulanir
 * (RestClient -> MockMvc -> filtre -> controller). sub, holder'dan gelir; holder bos ise arka plan token'i.
 */
class ServiceJwtClientInterceptorTest {

    static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};

    final SecurityFixture fx = new SecurityFixture();
    final MockMvc mvc = fx.mockMvc();
    final AtomicReference<UUID> holder = new AtomicReference<>();
    final RestClient client = RestClient.builder()
            .requestFactory(new MockMvcClientHttpRequestFactory(mvc))
            .requestInterceptor(new ServiceJwtClientInterceptor(fx.order, AUDIENCE, () -> Optional.ofNullable(holder.get())))
            .build();

    @Test
    void attachesVerifiableToken_withPropagatedSubject_freshPerRequest() {
        UUID account = UUID.randomUUID();
        holder.set(account);
        Map<String, Object> first = client.post()
                .uri("/internal/subscription/accounts/{a}/operations/{k}/consume", account, "op-1")
                .retrieve().body(MAP);
        Map<String, Object> second = client.post()
                .uri("/internal/subscription/accounts/{a}/operations/{k}/consume", account, "op-1")
                .retrieve().body(MAP);

        assertThat(first).containsEntry("caller", ORDER).containsEntry("account", account.toString());
        ServiceIdentity id1 = fx.verifier().verify((String) first.get("token"));   // sunucunun gordugu token dogrulanabilir
        ServiceIdentity id2 = fx.verifier().verify((String) second.get("token"));
        assertThat(id1.actor()).isEqualTo(ORDER);
        assertThat(id1.accountId()).isEqualTo(account);
        assertThat(id1.tokenId()).isNotEqualTo(id2.tokenId());                    // istek basina taze jti
    }

    @Test
    void emptyHolder_sendsBackgroundToken_whichUserOperationRejects() {
        holder.set(null);
        assertThatThrownBy(() -> client.post()
                .uri("/internal/subscription/accounts/{a}/operations/{k}/consume", UUID.randomUUID(), "op-1")
                .retrieve().body(MAP))
                .isInstanceOf(HttpClientErrorException.Forbidden.class)
                .hasMessageContaining("DELEGATION_DENIED");
        assertThat(fx.controller.hits).hasValue(0);
    }

    @Test
    void wrongTargetAudienceConfiguredOnClient_isRejectedByServer() {
        RestClient misconfigured = RestClient.builder()
                .requestFactory(new MockMvcClientHttpRequestFactory(mvc))
                .requestInterceptor(new ServiceJwtClientInterceptor(fx.order, "payment-api", Optional::empty))
                .build();
        assertThatThrownBy(() -> misconfigured.post().uri("/internal/subscription/reconcile").retrieve().body(MAP))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class)
                .hasMessageContaining("SERVICE_TOKEN_INVALID");
    }
}
