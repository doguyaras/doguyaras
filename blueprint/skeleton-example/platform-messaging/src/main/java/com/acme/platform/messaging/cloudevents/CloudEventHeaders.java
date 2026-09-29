package com.acme.platform.messaging.cloudevents;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * CloudEvents 1.0 attribute'larinin AMQP 0-9-1 mesaj header'larina eslenmesi (referans Bolum 12.2). AMQP 0-9-1 icin
 * resmi CloudEvents binding'i yoktur; attribute'lar "ce-" onekiyle header'a yazilir ve bu esleme YALNIZ burada yasar:
 * uretici (outbox handler) ve tuketici (listener) ayni sabitleri kullanir, header adi iki yerde yazilmaz.
 *
 * Transport'a bagimli tip (Spring AMQP Message) kullanilmaz; duz Map ile calisir ki platform-messaging AMQP'siz
 * (Kafka, HTTP) tasiyicilarda da ayni eslemeyi tasiyabilsin.
 */
public final class CloudEventHeaders {

    public static final String ID = "ce-id";
    public static final String TYPE = "ce-type";
    public static final String SOURCE = "ce-source";
    public static final String SUBJECT = "ce-subject";
    public static final String TIME = "ce-time";
    public static final String SPEC_VERSION = "ce-specversion";
    public static final String SPEC_VERSION_VALUE = "1.0";
    /** W3C Trace Context extension'lari: outbox headers JSON'undan oldugu gibi kopyalanir (span uretici tarafinda acildi). */
    public static final String TRACEPARENT = "traceparent";
    public static final String TRACESTATE = "tracestate";

    private CloudEventHeaders() { }

    /**
     * Uretici tarafi: outbox satirindan header kumesi. traceparent/tracestate outbox headers'inda varsa tasinir;
     * yoksa header yazilmaz (bos string yazmak tuketicide gecersiz trace baglami uretir).
     */
    public static Map<String, Object> of(UUID id, String type, String source, UUID subject, Instant time,
                                         Map<String, ?> outboxHeaders) {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put(SPEC_VERSION, SPEC_VERSION_VALUE);
        h.put(ID, id.toString());
        h.put(TYPE, type);
        h.put(SOURCE, source);
        if (subject != null) h.put(SUBJECT, subject.toString());
        h.put(TIME, time.toString());
        copyIfText(outboxHeaders, TRACEPARENT, h);
        copyIfText(outboxHeaders, TRACESTATE, h);
        return h;
    }

    /** Tuketici tarafi: header yoksa veya UUID degilse bos doner; karar (zehirli mesaj) cagirana aittir. */
    public static Optional<UUID> id(Map<String, Object> headers) {
        Object v = headers == null ? null : headers.get(ID);
        if (v == null) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(v.toString()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> text(Map<String, Object> headers, String name) {
        Object v = headers == null ? null : headers.get(name);
        return v == null ? Optional.empty() : Optional.of(v.toString());
    }

    private static void copyIfText(Map<String, ?> from, String key, Map<String, Object> to) {
        if (from == null) return;
        Object v = from.get(key);
        if (v instanceof String s && !s.isBlank()) to.put(key, s);
    }
}
