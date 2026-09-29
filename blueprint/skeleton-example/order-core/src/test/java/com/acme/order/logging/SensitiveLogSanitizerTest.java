package com.acme.order.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Sanitizer birim kurallari (referans Bolum 8.4 tablosu). */
class SensitiveLogSanitizerTest {

    @Test
    void sanitize_removesLineBreaks_redactsSecretFields_masksPhoneAndEmail_truncates() {
        String raw = "POST /hook\r\nAuthorization: Bearer abc.def token=SECRET-1 password: p4ss, otp=123456 "
                + "to=+905551234567 mail=jane.doe@example.com key=" + "A".repeat(40);
        String s = SensitiveLogSanitizer.sanitize(raw);
        assertThat(s).doesNotContain("\r").doesNotContain("\n")
                .contains("token=[REDACTED]").contains("password: [REDACTED]").contains("otp=[REDACTED]")
                .doesNotContain("SECRET-1").doesNotContain("p4ss").doesNotContain("123456")
                .contains("+90********67").doesNotContain("905551234567")
                .contains("j***@e***.com").doesNotContain("jane.doe")
                .doesNotContain("A".repeat(40));
        // 240 karakterde kesme (bosluklu metin; kesintisiz 32+ alfanumerik dizi zaten opak token sayilip redakte edilir)
        assertThat(SensitiveLogSanitizer.sanitize("word ".repeat(100))).hasSize(SensitiveLogSanitizer.MAX_LENGTH + 3).endsWith("...");
        assertThat(SensitiveLogSanitizer.sanitize("x".repeat(500))).isEqualTo("[REDACTED]");
        assertThat(SensitiveLogSanitizer.sanitize(null)).isEmpty();
    }

    @Test
    void maskPhone_keepsCountryCodeAndLastTwoDigits() {
        assertThat(SensitiveLogSanitizer.maskPhone("+905551234567")).isEqualTo("+90********67");
        assertThat(SensitiveLogSanitizer.maskPhone("05551234567")).isEqualTo("0********67");
        assertThat(SensitiveLogSanitizer.maskPhone("123")).isEqualTo("***");
    }

    @Test
    void maskEmail_keepsFirstCharAndTld() {
        assertThat(SensitiveLogSanitizer.maskEmail("jane.doe@example.com")).isEqualTo("j***@e***.com");
        assertThat(SensitiveLogSanitizer.maskEmail("bogus")).isEqualTo("***");
    }

    @Test
    void tokenFingerprint_isShortStableSha256_notTheToken() {
        String fp = SensitiveLogSanitizer.tokenFingerprint("SECRET-TOKEN");
        assertThat(fp).startsWith("sha256:").hasSize("sha256:".length() + 12).doesNotContain("SECRET");
        assertThat(SensitiveLogSanitizer.tokenFingerprint("SECRET-TOKEN")).isEqualTo(fp);
        assertThat(SensitiveLogSanitizer.tokenFingerprint("OTHER")).isNotEqualTo(fp);
    }

    @Test
    void safeExceptionSummary_usesRootCauseTypeAndSanitizedMessage() {
        var root = new IllegalStateException("insert failed: Key (phone)=(+905551234567) token=SECRET-9");
        var wrapped = new RuntimeException("outer", new RuntimeException("middle", root));
        String s = SensitiveLogSanitizer.safeExceptionSummary(wrapped);
        assertThat(s).startsWith("IllegalStateException: ").contains("+90********67").contains("token=[REDACTED]")
                .doesNotContain("905551234567").doesNotContain("SECRET-9").doesNotContain("outer");
        assertThat(SensitiveLogSanitizer.safeExceptionSummary(new IllegalArgumentException())).isEqualTo("IllegalArgumentException");
    }
}
