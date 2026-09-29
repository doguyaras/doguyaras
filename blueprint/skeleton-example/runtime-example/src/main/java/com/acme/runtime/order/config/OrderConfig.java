package com.acme.runtime.order.config;

import com.acme.platform.messaging.saga.LocalSagaStore;
import com.acme.platform.messaging.saga.SagaRecoveryWorker;
import com.acme.platform.security.client.ServiceJwtClientInterceptor;
import com.acme.platform.security.jwt.ServiceJwtSigner;
import com.acme.platform.security.jwt.ServiceJwtVerifier;
import com.acme.runtime.order.client.SubscriptionParticipantClient;
import com.acme.runtime.order.flow.OrderCreationFlow;
import com.acme.runtime.order.web.GatewayIdentityFilter;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.Set;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class OrderConfig {

    /** Resilience4j instance adi = hedef servis (order-app.yml: resilience4j.*.instances.subscription). */
    public static final String SUBSCRIPTION_TARGET = "subscription";

    @Bean
    public Clock clock() { return Clock.systemUTC(); }

    @Bean
    public LocalSagaStore localSagaStore(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager tm,
                                         OperationConsistencyProperties props, Clock clock) {
        return new LocalSagaStore(jdbc, tm, "order", props.toSagaProperties(), clock);
    }

    /**
     * Boot'un RestClient.Builder'i kullanilir: observation (W3C traceparent) customizer'i yalniz bu builder'da vardir
     * (Bolum 8.6). RestClient.create()/builder() ile kurulan client trace baglamini TASIMAZ.
     */
    @Bean
    public SubscriptionParticipantClient subscriptionParticipantClient(RestClient.Builder builder, SubscriptionClientProperties props,
                                                                       ServiceJwtSigner signer, CircuitBreakerRegistry circuitBreakers,
                                                                       BulkheadRegistry bulkheads) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(props.connectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(props.readTimeout());
        RestClient rest = builder
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .requestInterceptor(new ServiceJwtClientInterceptor(signer, props.audience(), SubscriptionParticipantClient.subjectContext()))
                .build();
        return new SubscriptionParticipantClient(rest, circuitBreakers.circuitBreaker(SUBSCRIPTION_TARGET),
                bulkheads.bulkhead(SUBSCRIPTION_TARGET), signer.serviceName());
    }

    @Bean
    public SagaRecoveryWorker sagaRecoveryWorker(LocalSagaStore store, SubscriptionParticipantClient participant,
                                                 OperationConsistencyProperties props, Clock clock) {
        return new SagaRecoveryWorker(store, participant, OrderCreationFlow.CALLER, props.toSagaProperties(), clock);
    }

    /** Public uclar yalniz gateway'in servis JWT'siyle (act=gateway, sub=hesap) cagrilabilir. */
    @Bean
    public FilterRegistrationBean<GatewayIdentityFilter> gatewayIdentityFilter(ServiceJwtVerifier verifier) {
        var reg = new FilterRegistrationBean<>(new GatewayIdentityFilter(verifier, Set.of("gateway")));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 11);                  // /internal filtresiyle ayni katman
        reg.addUrlPatterns("/v1/*");
        return reg;
    }
}
