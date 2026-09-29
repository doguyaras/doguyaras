package com.acme.order.web;

import java.util.List;

/**
 * Tek hata zarfi (referans Bolum 6.2). details yalniz istemciye gosterilebilir k=v tasir; reddedilen deger,
 * exception metni veya log'a ozel sebep (safeLogReason) buraya ASLA konmaz (Bolum 7.4).
 */
public record ErrorResponse(int code, String message, String service, String path, long timestamp, String traceId,
                            List<String> details) {}
