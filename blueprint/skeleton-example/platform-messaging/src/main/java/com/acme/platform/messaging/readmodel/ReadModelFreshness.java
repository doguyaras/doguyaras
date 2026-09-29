package com.acme.platform.messaging.readmodel;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Tazelik TUKETIM KONUMUNDAN olculur: now - rm_consumer_position.last_event_time. Projeksiyon satirinin applied_at'i
 * kullanilmaz; bir hesabin durumu bir ay degismemis olabilir, o eski satir gecikme degildir. Konum yoksa gecikme
 * sonsuzdur (hic tuketilmemis kaynak = bilinmeyen durum, alarm).
 */
public class ReadModelFreshness {

    /** Micrometer adi; Prometheus'ta readmodel_lag_seconds{source}. */
    public static final String LAG_METRIC = "readmodel.lag.seconds";

    private final ConsumerPositionStore positions;
    private final Clock clock;
    private final MeterRegistry meters;

    public ReadModelFreshness(ConsumerPositionStore positions, Clock clock, MeterRegistry meters) {
        this.positions = positions;
        this.clock = clock;
        this.meters = meters;
    }

    /** Her scrape'te konum satiri okunur (PK lookup); kaynak sayisi kucuktur, bu maliyet kabul edilir. */
    public void registerGauge(String source) {
        Gauge.builder(LAG_METRIC, () -> lagSeconds(source)).tag("source", source)
                .description("tuketim konumundan olculen read-model gecikmesi (saniye)").register(meters);
    }

    public Optional<Duration> lag(String source) {
        Instant now = clock.instant();
        return positions.find(source)
                .map(p -> Duration.between(p.lastEventTime(), now))
                .map(d -> d.isNegative() ? Duration.ZERO : d);                  // saat kaymasi gecikme sayilmaz
    }

    double lagSeconds(String source) {
        return lag(source).map(d -> d.toMillis() / 1000.0).orElse(Double.POSITIVE_INFINITY);
    }
}
