package com.acme.platform.messaging.readmodel;

import java.time.Instant;
import java.util.UUID;

/**
 * DEGISIKLIK (delta) olayi: op tek bir degisikliktir (bir engel ekleme/kaldirma); onceki durumu kapsamaz.
 * aggregateSeq aggregate basina monoton siradir: hicbir olay atlanamaz, bosluk = eksik olay. streamSeq tuketim
 * konumu icindir.
 */
public record DeltaEvent<D>(long streamSeq, UUID aggregateId, long aggregateSeq, Instant eventTime, D op) { }
