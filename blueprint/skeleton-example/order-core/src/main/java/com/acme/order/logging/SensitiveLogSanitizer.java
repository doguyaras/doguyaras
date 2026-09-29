package com.acme.order.logging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Log satirina girecek serbest metin icin tek kapi (referans Bolum 8.4). Kural: log satiri allowlist'teki
 * alanlardan (code=, reason=, outcome=, exceptionType=, teknik id) kurulur; kullanicidan/saglayicidan gelen
 * her metin (SKU, exception mesaji, URL) yalniz bu sinif uzerinden gecerek yazilir. Parametreli loglama
 * sanitize yerine gecmez: {} icine giren deger de ham metindir.
 *
 * Kavramsal olarak ortak kutuphaneye (platform-core) aittir; bu sprintte modul siniri disina cikilmadi.
 */
public final class SensitiveLogSanitizer {

    static final int MAX_LENGTH = 240;
    private static final String REDACTED = "[REDACTED]";

    /** k=v veya JSON icindeki gizli alan adlari; deger sonraki ayiraca kadar redakte edilir. */
    private static final Pattern SECRET_FIELD = Pattern.compile(
            "(?i)\\b(token|access_token|refresh_token|password|passwd|pwd|secret|otp|ciphertext|authorization|api[_-]?key|bearer)\\b"
                    + "(\\s*[=:]\\s*\"?)([^\\s,;&\"}\\]]+)");
    /** Uzun base64/hex bloklari (JWT, sifreli icerik, anahtar) - 32+ karakter. */
    private static final Pattern LONG_OPAQUE = Pattern.compile("[A-Za-z0-9+/_\\-]{32,}={0,2}");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    /** +90 555 123 45 67, 05551234567, (555) 123-4567 gibi 10-15 haneli diziler. */
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\s().-]{8,18}\\d");
    private static final Pattern DIGITS = Pattern.compile("\\D");

    private SensitiveLogSanitizer() {}

    /** CR/LF (log injection), gizli alanlar, e-posta, telefon, uzun opak bloklar; 240 karakterde keser. */
    public static String sanitize(String raw) {
        if (raw == null) return "";
        String s = raw.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');
        s = SECRET_FIELD.matcher(s).replaceAll(m -> m.group(1) + m.group(2) + REDACTED);
        s = EMAIL.matcher(s).replaceAll(m -> maskEmail(m.group()));
        s = PHONE.matcher(s).replaceAll(m -> maskPhone(m.group()));
        s = LONG_OPAQUE.matcher(s).replaceAll(REDACTED);
        return s.length() > MAX_LENGTH ? s.substring(0, MAX_LENGTH) + "..." : s;
    }

    /** Ulke kodu/ilk 2 hane + son 2 hane kalir: +905551234567 -> +90*******67. */
    public static String maskPhone(String phone) {
        if (phone == null) return "";
        String digits = DIGITS.matcher(phone).replaceAll("");
        if (digits.length() < 6) return "***";
        int keepHead = phone.startsWith("+") ? 2 : 1;
        String head = (phone.startsWith("+") ? "+" : "") + digits.substring(0, keepHead);
        return head + "*".repeat(digits.length() - keepHead - 2) + digits.substring(digits.length() - 2);
    }

    /** jane.doe@example.com -> j***@e***.com (alan adinin TLD'si kalir). */
    public static String maskEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at < 1) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        int dot = domain.lastIndexOf('.');
        String tld = dot > 0 ? domain.substring(dot) : "";
        return local.charAt(0) + "***@" + (domain.isEmpty() ? "" : domain.charAt(0)) + "***" + tld;
    }

    /** Token'i loglamadan iliskilendirmek icin kisa sha256 parmak izi (12 hex). */
    public static String tokenFingerprint(String token) {
        if (token == null || token.isEmpty()) return "";
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Root-cause tipi + sanitize edilmis mesaj. Ham exception mesaji (PG DETAIL satiri, HTTP istemci URL'si,
     * saglayici yaniti) sik sik PII/secret tasir; log'a yalniz bu ozet yazilir, throwable'in kendisi eklenmez.
     */
    public static String safeExceptionSummary(Throwable t) {
        if (t == null) return "";
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String msg = root.getMessage() == null ? "" : ": " + sanitize(root.getMessage());
        return root.getClass().getSimpleName() + msg;
    }
}
