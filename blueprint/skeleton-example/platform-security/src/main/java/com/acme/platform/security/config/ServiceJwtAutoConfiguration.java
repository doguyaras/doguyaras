package com.acme.platform.security.config;

import com.acme.platform.security.delegation.DelegationInterceptor;
import com.acme.platform.security.delegation.DelegationPolicy;
import com.acme.platform.security.jwt.ServiceJwtKeyRegistry;
import com.acme.platform.security.jwt.ServiceJwtSigner;
import com.acme.platform.security.jwt.ServiceJwtVerifier;
import com.acme.platform.security.web.CurrentAccountArgumentResolver;
import com.acme.platform.security.web.InternalAccessPolicy;
import com.acme.platform.security.web.ServiceJwtVerificationFilter;
import com.acme.platform.security.web.ServiceSecurityExceptionHandler;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetKeyPair;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * service-jwt.audience tanimliysa devreye girer. Servis yalniz yml yazar: filtre, interceptor, resolver ve advice
 * buradan gelir. Anahtar dosyalari (Bolum 15.3 config tree) JWK bicimindedir; jwks-path yoksa bos registry
 * verilir ve uygulama (veya test) anahtarlari programatik kaydeder.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty("service-jwt.audience")
@EnableConfigurationProperties(ServiceJwtProperties.class)
public class ServiceJwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock serviceJwtClock() { return Clock.systemUTC(); }

    @Bean
    @ConditionalOnMissingBean
    public ServiceJwtKeyRegistry serviceJwtKeyRegistry(ServiceJwtProperties props) {
        if (props.jwksPath() == null || props.jwksPath().isBlank()) return new ServiceJwtKeyRegistry();
        return ServiceJwtKeyRegistry.fromJson(read(Path.of(props.jwksPath())));
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("service-jwt.private-key-path")
    public ServiceJwtSigner serviceJwtSigner(ServiceJwtProperties props, Clock clock) {
        try {
            JWK jwk = JWK.parse(read(Path.of(props.privateKeyPath())));
            if (!(jwk instanceof OctetKeyPair okp)) throw new IllegalStateException("private-key-path is not an OKP JWK");
            return new ServiceJwtSigner(okp, props.serviceName(), Duration.ofSeconds(props.ttlSeconds()), clock);
        } catch (ParseException e) {
            throw new IllegalStateException("private-key-path is not a JWK", e);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceJwtVerifier serviceJwtVerifier(ServiceJwtProperties props, ServiceJwtKeyRegistry registry, Clock clock) {
        // Bilinen imzalayicilar: yml'deki liste + JWKS dosyasinda anahtari olanlar
        Set<String> issuers = new HashSet<>(props.knownIssuers());
        issuers.addAll(registry.issuers());
        return new ServiceJwtVerifier(registry, issuers, props.audience(), Duration.ofSeconds(props.clockSkewSeconds()), clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public InternalAccessPolicy internalAccessPolicy(ServiceJwtProperties props) { return props.internalAccessPolicy(); }

    @Bean
    @ConditionalOnMissingBean
    public DelegationPolicy delegationPolicy(ServiceJwtProperties props) { return props.delegationPolicy(); }

    @Bean
    public FilterRegistrationBean<ServiceJwtVerificationFilter> serviceJwtVerificationFilter(ServiceJwtVerifier verifier,
                                                                                            InternalAccessPolicy policy) {
        var reg = new FilterRegistrationBean<>(new ServiceJwtVerificationFilter(verifier, policy));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);                     // loglama/tracing filtrelerinden sonra, is mantigindan once
        reg.addUrlPatterns("/*");                                          // /internal karari filtrenin icinde (normalize sonrasi)
        return reg;
    }

    @Bean
    public ServiceSecurityExceptionHandler serviceSecurityExceptionHandler() { return new ServiceSecurityExceptionHandler(); }

    @Bean
    public WebMvcConfigurer serviceJwtWebMvcConfigurer(DelegationPolicy delegationPolicy) {
        return new WebMvcConfigurer() {
            @Override public void addInterceptors(InterceptorRegistry registry) {
                // tum path'ler: @RequireOperation /internal disinda bir metoda konursa sessizce atlanmasin
                // (anotasyonsuz handler'da interceptor zaten no-op; kimlik yoksa fail-closed 401)
                registry.addInterceptor(new DelegationInterceptor(delegationPolicy));
            }
            @Override public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new CurrentAccountArgumentResolver());
            }
        };
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read service JWT key material at " + path, e);
        }
    }
}
