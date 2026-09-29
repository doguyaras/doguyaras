package com.acme.platform.messaging.readmodel;

import java.time.Instant;

/**
 * TAM DURUM (snapshot) olayi: state onceki durumu butunuyle kapsar, bu yuzden kucuk revizyonu atlamak guvenlidir.
 * streamSeq kaynak akisindaki konumdur (tuketim konumu icin); revizyon state icindedir ve yalniz kendi aggregate'i
 * icinde karsilastirilir.
 */
public record SnapshotEvent<S>(long streamSeq, Instant eventTime, S state) { }
