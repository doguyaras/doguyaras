package com.acme.platform.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/** Kanit seviyesi 1: {@code rate-limit.rules.<scope>.*} baglamasi ve kural dogrulamasi. */
class RateLimitRulesTest {

    static Binder binder(Map<String, String> props) { return new Binder(new MapConfigurationPropertySource(props)); }

    @Test
    void bindsKebabCaseRulesWithExplicitFailPolicy() {
        RateLimitRules rules = RateLimitRules.bind(binder(Map.of(
                "rate-limit.rules.otp-send-phone.limit", "5",
                "rate-limit.rules.otp-send-phone.window-seconds", "3600",
                "rate-limit.rules.otp-send-phone.fail-policy", "closed",
                "rate-limit.rules.search-ip.limit", "120",
                "rate-limit.rules.search-ip.window-seconds", "60",
                "rate-limit.rules.search-ip.fail-policy", "OPEN")));
        assertThat(rules.find("otp-send-phone")).contains(new RateLimitRule("otp-send-phone", 5, 3600, FailPolicy.CLOSED));
        assertThat(rules.find("search-ip")).contains(new RateLimitRule("search-ip", 120, 60, FailPolicy.OPEN));
        assertThat(rules.find("upload-ip")).isEmpty();
    }

    @Test
    void missingFailPolicyIsRejectedAtStartup() {
        // Referansin config ornegi fail-policy icermez; varsayilan secmek ya limitsiz ya hep-503 demektir.
        assertThatThrownBy(() -> RateLimitRules.bind(binder(Map.of(
                "rate-limit.rules.order-create-account.limit", "60",
                "rate-limit.rules.order-create-account.window-seconds", "60"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fail-policy");
    }

    @Test
    void invalidRulesAreRejected() {
        assertThatThrownBy(() -> new RateLimitRule("OTP:send", 5, 60, FailPolicy.CLOSED))
                .hasMessageContaining("kebab-case");
        assertThatThrownBy(() -> new RateLimitRule("otp-send", 0, 60, FailPolicy.CLOSED)).hasMessageContaining("limit");
        assertThatThrownBy(() -> new RateLimitRule("otp-send", 5, 0, FailPolicy.CLOSED)).hasMessageContaining("window");
        assertThatThrownBy(() -> RateLimitRules.of(new RateLimitRule("a", 1, 1, FailPolicy.OPEN),
                new RateLimitRule("a", 2, 2, FailPolicy.OPEN))).hasMessageContaining("iki kez");
        assertThatThrownBy(() -> RateLimitRules.bind(binder(Map.of("rate-limit.rules.a.limit", "x"))))
                .isInstanceOf(BindException.class);
    }
}
