package com.acme.platform.security.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Filtrenin dogrulanmis kimlikten yazdigi request attribute'lari. Controller'lar header'i degil bu attribute'lari
 * okur (Bolum 23.5: "kimlik yalniz dogrulanmis baglamdan gelir"). x.accountId gateway tarafindan String olarak da
 * yazilabildiginden okuma tarafi iki tipi de kabul eder.
 */
public final class ServiceRequestAttributes {

    public static final String CALLER_SERVICE = "x.callerService";
    public static final String ACCOUNT_ID = "x.accountId";
    public static final String BACKGROUND = "x.background";

    private ServiceRequestAttributes() {}

    public static Optional<String> callerService(HttpServletRequest request) {
        Object v = request.getAttribute(CALLER_SERVICE);
        return v instanceof String s && !s.isBlank() ? Optional.of(s) : Optional.empty();
    }

    /** @throws IllegalArgumentException attribute var ama UUID degil (yanlis kablolama; sessizce yutulmaz) */
    public static Optional<UUID> accountId(HttpServletRequest request) {
        Object v = request.getAttribute(ACCOUNT_ID);
        if (v == null) return Optional.empty();
        if (v instanceof UUID id) return Optional.of(id);
        if (v instanceof String s) return s.isBlank() ? Optional.empty() : Optional.of(UUID.fromString(s));
        throw new IllegalArgumentException(ACCOUNT_ID + " attribute has unsupported type " + v.getClass().getName());
    }

    public static boolean background(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(BACKGROUND));
    }

    /** Mevcut thread'in istegi icindeki hesap; RestClient interceptor'u sub'i asagiya boyle tasir. */
    public static Optional<UUID> currentAccountId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return accountId(attrs.getRequest());
        }
        return Optional.empty();
    }
}
