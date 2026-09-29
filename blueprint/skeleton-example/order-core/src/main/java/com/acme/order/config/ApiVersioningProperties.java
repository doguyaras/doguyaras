package com.acme.order.config;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kaldirilacak API surumleri (referans Bolum 20): Deprecation (RFC 9745, @epoch), Sunset (RFC 8594) ve
 * Link rel="deprecation" header'lari bu listeden uretilir. Sunset sonrasi surum kapatilir (410).
 */
@ConfigurationProperties("api")
public record ApiVersioningProperties(List<Deprecation> deprecations) {
    public ApiVersioningProperties { deprecations = deprecations == null ? List.of() : List.copyOf(deprecations); }
    public record Deprecation(String version, Instant deprecatedAt, Instant sunsetAt, URI link) {}
}
