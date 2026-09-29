package com.acme.platform.messaging.readmodel;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.List;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Read-model tuketici sozlesmesi (referans Bolum 4.6). Projeksiyon degisikligi ve tuketim konumu AYNI transaction'da
 * yazilir; disaridan bir TX (inbox, Bolum 11.3) icinde cagrilirsa ona katilir (REQUIRED): dis TX rollback olursa
 * ne projeksiyon ne konum degisir. Snapshot: kucuk/esit revizyon yok sayilir. Delta: seq == last + 1 zorunlu;
 * bosluk => sayac + istisna, HICBIR SEY uygulanmaz; tekrar (seq <= last) => yok sayilir.
 */
public class ReadModelApplier {

    /** Micrometer adi; Prometheus'ta readmodel_gap_total{source} olarak gorunur. */
    public static final String GAP_METRIC = "readmodel.gap";

    private final TransactionTemplate tx;
    private final ConsumerPositionStore positions;
    private final Clock clock;
    private final MeterRegistry meters;

    public ReadModelApplier(TransactionTemplate tx, ConsumerPositionStore positions, Clock clock, MeterRegistry meters) {
        this.tx = tx;
        this.positions = positions;
        this.clock = clock;
        this.meters = meters;
    }

    public <S> Outcome applySnapshot(String source, SnapshotProjection<S> projection, SnapshotEvent<S> event) {
        return tx.execute(status -> {
            int changed = projection.upsert(event.state(), clock.instant());   // revizyon korumasi UPSERT'in icinde
            positions.advance(source, event.streamSeq(), event.eventTime(), clock.instant());  // yok sayilsa da tuketildi
            return changed > 0 ? Outcome.APPLIED : Outcome.IGNORED;
        });
    }

    public <D> Outcome applyDelta(String source, DeltaProjection<D> projection, DeltaEvent<D> event) {
        return tx.execute(status -> {
            long last = positions.lockDeltaSeq(source, event.aggregateId());
            if (event.aggregateSeq() <= last) {                                 // tekrar teslim: tek etki
                positions.advance(source, event.streamSeq(), event.eventTime(), clock.instant());
                return Outcome.IGNORED;
            }
            if (event.aggregateSeq() != last + 1) {                             // bosluk: dur + uzlastir; kontrol ONCE
                gaps(source).increment();
                throw new GapDetectedException(source, event.aggregateId(), last + 1, event.aggregateSeq());
            }
            projection.apply(event);                                            // yalniz sira dogrulandiktan sonra
            positions.setDeltaSeq(source, event.aggregateId(), event.aggregateSeq());
            positions.advance(source, event.streamSeq(), event.eventTime(), clock.instant());
            return Outcome.APPLIED;
        });
    }

    /**
     * Sahibin export'undan (veya stream replay'inden) sifirdan kurma: projeksiyon ve konum silinir, export sirayla
     * uygulanir. Ayni export iki kez => ayni satirlar (deterministik); export sirasi da sonucu degistirmez (revizyon korumasi).
     */
    public <S> void rebuildFromExport(String source, SnapshotProjection<S> projection, List<SnapshotEvent<S>> export) {
        tx.executeWithoutResult(status -> {
            projection.clear();
            positions.reset(source);
            for (SnapshotEvent<S> e : export) {
                projection.upsert(e.state(), clock.instant());
                positions.advance(source, e.streamSeq(), e.eventTime(), clock.instant());
            }
        });
    }

    private Counter gaps(String source) {
        return Counter.builder(GAP_METRIC).tag("source", source)
                .description("read-model delta akisinda tespit edilen sira boslugu; alarm esigi > 0").register(meters);
    }
}
