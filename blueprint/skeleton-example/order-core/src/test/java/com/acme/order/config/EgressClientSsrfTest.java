package com.acme.order.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.client.FilteredHostException;
import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

/**
 * SSRF kaniti (referans Bolum 9.11): InetAddressFilter.externalAddresses() bean'i auto-configured RestClient'a
 * uygulanir; loopback'e (IP ile ve DNS'ten cozulen "localhost" ile) giden istek FilteredHostException ile kesilir
 * ve sunucuya HIC ulasmaz. Filtresiz duz RestClient ayni istegi basarir: engelleyen sey filtredir.
 * Sunucu gercek bir HTTP sunucusudur (com.sun.net.httpserver, 127.0.0.1, rastgele port).
 */
@SpringBootTest(classes = EgressClientConfig.class)
@ImportAutoConfiguration({HttpClientAutoConfiguration.class, ImperativeHttpClientAutoConfiguration.class,
        RestClientAutoConfiguration.class})
class EgressClientSsrfTest {

    static HttpServer server;
    static int port;
    static final AtomicInteger hits = new AtomicInteger();

    @Autowired @Qualifier("egressRestClient") RestClient egress;
    // ObjectProvider: bean silinirse context yine kalkar ve loopback testleri DAVRANISLA (istek sunucuya ulasir) kirilir.
    @Autowired ObjectProvider<InetAddressFilter> filterProvider;

    @BeforeAll
    static void startLocalServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/ping", exchange -> {
            hits.incrementAndGet();
            byte[] body = "pong".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterAll
    static void stopLocalServer() { if (server != null) server.stop(0); }

    @BeforeEach
    void resetHits() { hits.set(0); }

    @Test
    void loopbackIp_isBlockedByFilter_beforeAnyConnection() {
        Throwable t = catchThrowable(() -> egress.get().uri("http://127.0.0.1:" + port + "/ping").retrieve().body(String.class));
        assertThat(t).isNotNull();
        assertThat(rootCause(t)).isInstanceOf(FilteredHostException.class);
        assertThat(((FilteredHostException) rootCause(t)).getHost()).isEqualTo("127.0.0.1");
        assertThat(hits).as("sunucuya ulasan istek").hasValue(0);
    }

    @Test
    void hostnameResolvingToPrivateAddress_isBlocked_dnsRebindingSafe() {
        Throwable t = catchThrowable(() -> egress.get().uri("http://localhost:" + port + "/ping").retrieve().body(String.class));
        assertThat(t).isNotNull();
        assertThat(rootCause(t)).isInstanceOf(FilteredHostException.class);
        assertThat(((FilteredHostException) rootCause(t)).getHost()).isEqualTo("localhost");
        assertThat(hits).hasValue(0);
    }

    @Test
    void plainRestClientWithoutFilter_reachesTheSameServer() {
        String body = RestClient.create().get().uri("http://127.0.0.1:" + port + "/ping").retrieve().body(String.class);
        assertThat(body).isEqualTo("pong");
        assertThat(hits).hasValue(1);
    }

    @Test
    void filterClassifiesPrivateMetadataAndPublicAddresses() throws Exception {
        InetAddressFilter filter = filterProvider.getObject();
        // Literal IP: DNS sorgusu yok.
        for (String internal : new String[]{"10.0.0.1", "172.16.0.1", "192.168.1.1", "127.0.0.1", "169.254.169.254", "::1", "fd00::1"}) {
            assertThat(filter.matches(InetAddress.getByName(internal))).as(internal).isFalse();
        }
        for (String external : new String[]{"8.8.8.8", "1.1.1.1", "2606:4700:4700::1111"}) {
            assertThat(filter.matches(InetAddress.getByName(external))).as(external).isTrue();
        }
    }

    static Throwable rootCause(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) r = r.getCause();
        return r;
    }
}
