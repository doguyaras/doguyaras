package com.acme.platform.messaging.readmodel;

import java.time.Instant;

/** Kaynak basina bir snapshot projeksiyonu. upsert revizyon korumali UPSERT'tir: 0 satir = eski/esit revizyon, yok sayildi. */
public interface SnapshotProjection<S> {

    int upsert(S state, Instant appliedAt);

    /** Rebuild icin: projeksiyon sifirlanir, export sirayla yeniden uygulanir. */
    void clear();
}
