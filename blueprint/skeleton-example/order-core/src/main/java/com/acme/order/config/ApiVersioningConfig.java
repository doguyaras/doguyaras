package com.acme.order.config;

import java.time.ZoneOffset;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.ApiVersionDeprecationHandler;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;

/**
 * Surum cozumleme Boot property'leriyle (spring.mvc.apiversion.use.header vb.); Boot bu bean'i
 * ObjectProvider<ApiVersionDeprecationHandler> uzerinden otomatik baglar (WebMvcAutoConfiguration).
 *
 * Dikkat: spring.mvc.apiversion.required=true, strateji tanimli her DispatcherServlet route'una uygulanir
 * (surumsuz handler'lar ve ayni porttaki actuator dahil; yalniz ERROR dispatch muaf). Bu nedenle actuator ayri
 * management portunda calisir ya da required=false + default surum secilir.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ApiVersioningProperties.class)
public class ApiVersioningConfig {

    @Bean
    ApiVersionDeprecationHandler apiVersionDeprecationHandler(ApiVersioningProperties props) {
        StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler();
        for (ApiVersioningProperties.Deprecation d : props.deprecations()) {
            StandardApiVersionDeprecationHandler.VersionSpec spec = handler.configureVersion(d.version())
                    .setDeprecationDate(d.deprecatedAt().atZone(ZoneOffset.UTC))
                    .setSunsetDate(d.sunsetAt().atZone(ZoneOffset.UTC));
            if (d.link() != null) spec.setDeprecationLink(d.link());
        }
        return handler;
    }
}
