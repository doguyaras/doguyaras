package com.acme.order.config;

import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * SSRF korumasi (referans Bolum 9.11). InetAddressFilter bean'i Boot 4.1 HttpClientAutoConfiguration tarafindan
 * HttpClientSettings'e alinir ve auto-configured her RestClient/WebClient'a uygulanir: hedef host DNS'ten
 * cozuldukten SONRA ic/ozel adrese (10/8, 172.16/12, 192.168/16, 127/8, link-local, NAT64) giden istek
 * FilteredHostException ile kesilir; DNS rebinding'e karsi da bu yuzden etkilidir.
 *
 * Dikkat: bean globaldir; ic servis client'lari (spring.http.serviceclient.*) da ayni filtreyi alir. Ic ag
 * adreslerine giden servis client'lari icin filtre uygulanmamis ayri bir ClientHttpRequestFactory gerekir.
 * Timeout/redirect ayarlari spring.http.clients.* altindadir (application.yml).
 */
@Configuration(proxyBeanMethods = false)
public class EgressClientConfig {

    /** Yalniz genel internet adresleri: ic ag, loopback, link-local (169.254.169.254 metadata) reddedilir. */
    @Bean
    InetAddressFilter egressAddressFilter() { return InetAddressFilter.externalAddresses(); }

    /** Kullanici kaynakli URL'lere (webhook, onizleme) giden client; auto-configured Builder filtreyi tasir. */
    @Bean
    RestClient egressRestClient(RestClient.Builder builder) { return builder.build(); }
}
