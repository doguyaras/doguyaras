package com.acme.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.platform.messaging.readmodel.ReadModelApplier;
import com.acme.platform.messaging.readmodel.ReadModelFreshness;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

/**
 * Kanit seviyesi 1: Micrometer'daki noktali adlar Prometheus'ta referansin (Bolum 4.6) adlariyla gorunur:
 * readmodel_gap_total{source} ve readmodel_lag_seconds{source}. Alarm kurallari bu adlara yazilir.
 */
class ReadModelMetricNamesTest {

    @Test
    void metricNamesRenderAsInReference() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Counter.builder(ReadModelApplier.GAP_METRIC).tag("source", "user").register(registry).increment();
        Gauge.builder(ReadModelFreshness.LAG_METRIC, () -> 4.5).tag("source", "auth").register(registry);
        String scrape = registry.scrape();
        assertThat(scrape).contains("readmodel_gap_total{source=\"user\"} 1.0");
        assertThat(scrape).contains("readmodel_lag_seconds{source=\"auth\"} 4.5");
    }
}
