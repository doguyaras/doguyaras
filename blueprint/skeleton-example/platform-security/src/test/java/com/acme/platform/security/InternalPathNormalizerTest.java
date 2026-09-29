package com.acme.platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.platform.security.web.InternalPathNormalizer;
import com.acme.platform.security.web.InternalPathNormalizer.Kind;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Bolum 9.4 adim 0 tablosu: hangi ham path INTERNAL / PUBLIC / INVALID sayilir. */
class InternalPathNormalizerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/internal/..%2Fv1/ping", "/internal/../v1/ping", "/internal/subscription/%2e%2e/admin",
            "/internal/subscription/%2E%2E/admin", "/internal/%252e%252e/v1/ping", "/internal/./x",
            "/internal/x/.", "/internal/x/..", "/internal//x", "/internal/x%5C..%5Cy", "/internal/x%00y",
            "/internal/x;jsessionid=1", "/v1/../internal/x", "/v1/%2e%2e/internal/x", "/internal/%zz",
            "/internal/x%2Fy", "/internal/.."})
    void invalid(String raw) {
        assertThat(InternalPathNormalizer.inspect(raw).kind()).as(raw).isEqualTo(Kind.INVALID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v1/ping", "/", "/actuator/health", "/internalx/y", "/v1/internal/x", "/v1/..%2Fping", ""})
    void publicPassThrough(String raw) {
        assertThat(InternalPathNormalizer.inspect(raw).kind()).as(raw).isEqualTo(Kind.PUBLIC);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/internal", "/internal/x", "/internal/subscription/accounts/1/balance", "/internal/%61bc",
            "/internal/a+b", "/internal/a%20b"})
    void internalNormalized(String raw) {
        var r = InternalPathNormalizer.inspect(raw);
        assertThat(r.kind()).as(raw).isEqualTo(Kind.INTERNAL);
        assertThat(r.path()).doesNotContain("%");
    }

    @org.junit.jupiter.api.Test
    void decodedFormIsWhatAllowlistSees() {
        assertThat(InternalPathNormalizer.inspect("/internal/%61bc").path()).isEqualTo("/internal/abc");
        assertThat(InternalPathNormalizer.inspect("/internal/a+b").path()).as("'+' path'te literal").isEqualTo("/internal/a+b");
        assertThat(InternalPathNormalizer.inspect("/internal/a%20b").path()).isEqualTo("/internal/a b");
    }
}
