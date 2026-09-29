package com.acme.platform.security.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.web.util.UriUtils;

/**
 * Bolum 9.4 adim 0: path once decode + normalize edilir, allowlist NORMALIZE EDILMIS path uzerinde eslestirilir.
 * /internal alanina dokunan her istekte cift kodlama, '.'/'..' segmenti, '//', '\' ve NUL gecersizdir (400) ve
 * public kurallara ASLA geri dusmez: "/internal/..%2Fv1/ping" ne /internal/** ne de /v1/ping olarak degerlendirilir.
 * /internal disindaki istekler dokunulmadan gecer (Tomcat zaten kendi kanonlastirmasini yapar).
 * "/internal'a dokunuyor mu?" sorusu, Spring MVC'nin YONLENDIRME icin gordugu forma gore sorulur: Tomcat ve
 * Spring ';...' path parametrelerini eslestirmeden once atar, yani "/internal;x/admin" handler tarafinda
 * "/internal/admin" olur. Siniflandirma bu parametreleri atmadan yapilsaydi istek PUBLIC sayilip token'siz
 * gecerdi. Buyuk/kucuk harf farki da ("/INTERNAL/..") case-insensitive eslestirme acilan uygulamalar icin
 * internal sayilir ve kanonik olmadigi icin reddedilir.
 */
public final class InternalPathNormalizer {

    public enum Kind { PUBLIC, INTERNAL, INVALID }

    public record Result(Kind kind, String path) {
        static final Result PUBLIC = new Result(Kind.PUBLIC, null);
        static final Result INVALID = new Result(Kind.INVALID, null);
    }

    private static final String INTERNAL_PREFIX = "/internal";
    private static final Pattern PATH_PARAMS = Pattern.compile(";[^/]*");

    private InternalPathNormalizer() {}

    /** @param rawPath context path'i cikarilmis, HENUZ decode edilmemis request URI */
    public static Result inspect(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) return Result.PUBLIC;
        boolean rawInternal = routesToInternal(rawPath);
        String decoded;
        try {
            decoded = UriUtils.decode(rawPath, StandardCharsets.UTF_8);       // yalniz %XX; '+' path'te literaldir
        } catch (IllegalArgumentException e) {
            return rawInternal ? Result.INVALID : Result.PUBLIC;               // bozuk kodlama internal'a ulasamaz
        }
        String normalized = normalize(decoded);
        boolean touchesInternal = rawInternal || routesToInternal(decoded) || routesToInternal(normalized)
                || routesToInternal(normalize(stripPathParams(decoded)));     // "/v1/..;/internal/x" gibi
        if (!touchesInternal) return Result.PUBLIC;
        if (normalized == null) return Result.INVALID;
        if (decoded.indexOf('%') >= 0 || decoded.indexOf('\\') >= 0 || decoded.indexOf('\0') >= 0
                || decoded.contains("//") || decoded.indexOf(';') >= 0) {
            return Result.INVALID;                                             // cift kodlama, ters bolu, NUL, bos segment, matrix param
        }
        if (rawPath.toUpperCase().contains("%2F")) return Result.INVALID;     // kodlanmis '/': segment yapisini degistirir
        if (!normalized.equals(decoded)) return Result.INVALID;                // '.' veya '..' segmenti vardi
        if (!isInternal(normalized)) return Result.INVALID;
        return new Result(Kind.INTERNAL, normalized);
    }

    /** Path parametreleri atilmis ve harf buyuklugu yok sayilmis haliyle /internal alanina yonlenebilir mi? */
    private static boolean routesToInternal(String path) {
        return path != null && isInternal(stripPathParams(path).toLowerCase(Locale.ROOT));
    }

    private static String stripPathParams(String path) {
        return PATH_PARAMS.matcher(path).replaceAll("");
    }

    private static boolean isInternal(String path) {
        return path.equals(INTERNAL_PREFIX) || path.startsWith(INTERNAL_PREFIX + "/");
    }

    private static String normalize(String decodedPath) {
        try {
            String p = new URI(null, null, decodedPath, null).normalize().getPath();
            return p == null || p.startsWith("/..") ? null : p;               // kokun ustune cikan path gecersiz
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
