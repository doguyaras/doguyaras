package com.acme.platform.parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * DAVRANISSAL dogrulama (referans Bolum 14 bounded staleness, 4.7 control plane; kanit seviyesi 1: sahte kaynak,
 * deterministik saat, GERCEK gecici dizin). Her test bir okuma kuralini kanitlar; kaynak cagri sayilariyla
 * "kaynaga gitti / gitmedi" iddialari dogrudan olculur.
 */
class SystemParameterProviderTest {

    static final String LIMITS = "order-limits";          // guvenlik-kritik: maxStaleness 10 dk
    static final String LABELS = "order-labels";          // normal: default maxStaleness 1 sa
    static final Duration TTL = Duration.ofSeconds(5);
    static final Duration LIMITS_MAX = Duration.ofMinutes(10);
    static final Duration DEFAULT_MAX = Duration.ofHours(1);

    @TempDir Path dir;
    final JsonMapper mapper = JsonMapper.builder().build();
    final MutableClock clock = new MutableClock();
    SimpleMeterRegistry meters;
    DiskSnapshotStore disk;
    ParameterProperties props;
    ScriptedSource limits;
    ScriptedSource labels;

    @BeforeEach
    void setUp() {
        meters = new SimpleMeterRegistry();
        disk = new DiskSnapshotStore(dir, mapper);
        props = new ParameterProperties(TTL, DEFAULT_MAX, Map.of(LIMITS, LIMITS_MAX));
        limits = new ScriptedSource(7, values("""
                {"order.max_items": 25, "order.cancel_window": 900,
                 "order.channels": [
                   {"code":"WEB","labels":{"tr":"Web","en":"Web"},"order":2,"active":true},
                   {"code":"APP","labels":{"tr":"Uygulama","en":"App"},"order":1,"active":false}]}
                """), Criticality.SECURITY_CRITICAL);
        labels = new ScriptedSource(3, values("""
                {"label.max_length": 40}
                """), Criticality.NORMAL);
    }

    SystemParameterProvider provider(ParameterSource source) {
        return new SystemParameterProvider(source, disk, props, clock, meters);
    }

    Map<String, JsonNode> values(String json) {
        JsonNode node = mapper.readTree(json);
        Map<String, JsonNode> out = new java.util.LinkedHashMap<>();
        node.properties().forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    double staleness(String group) {
        Gauge g = meters.find(SystemParameterProvider.STALENESS_METRIC).tag("group", group).gauge();
        assertThat(g).as("parameter_staleness_seconds{group=%s} yayinlanmali", group).isNotNull();
        return g.value();
    }

    // --- 1. soguk acilis: kaynak KAPALI, diskte snapshot var -> snapshot servis edilir, staleness raporlanir

    @Test
    void coldStartWithSourceDownServesDiskSnapshotAndReportsStaleness() {
        Instant fetchedAt = clock.now.minus(Duration.ofSeconds(90));
        disk.write(new ParameterSnapshot(new ParameterGroupDto(LIMITS, 5, limits.fetch(LIMITS).values(),
                Criticality.SECURITY_CRITICAL), fetchedAt));
        limits.fetchCalls.set(0);
        limits.down();

        SystemParameterProvider fresh = provider(limits);            // yeni instance: bellek bos
        ParameterValues v = fresh.group(LIMITS);

        assertThat(v.revision()).isEqualTo(5);
        assertThat(v.integer("order.max_items")).isEqualTo(25);
        assertThat(limits.fetchCalls.get()).as("kaynak denendi").isEqualTo(1);
        assertThat(staleness(LIMITS)).isEqualTo(90.0);
        assertThat(fresh.stalenessOf(LIMITS)).contains(Duration.ofSeconds(90));
    }

    @Test
    void coldStartWithSourceDownAndNoSnapshotAnywhereIsUnavailableEvenForNormalGroup() {
        labels.down();
        assertThatThrownBy(() -> provider(labels).group(LABELS))
                .isInstanceOf(ParameterUnavailableException.class)
                .satisfies(e -> assertThat(((ParameterException) e).httpStatus()).isEqualTo(503));
    }

    // --- 2. kaynak dusmus, maxStaleness icinde -> son bilinen deger

    @Test
    void sourceDownWithinMaxStalenessServesLastKnownValue() {
        SystemParameterProvider p = provider(limits);
        assertThat(p.group(LIMITS).revision()).isEqualTo(7);
        limits.down();
        clock.advance(LIMITS_MAX.minusSeconds(1));                  // 9 dk 59 sn: sinirin icinde

        ParameterValues v = p.group(LIMITS);

        assertThat(v.revision()).isEqualTo(7);
        assertThat(v.integer("order.max_items")).isEqualTo(25);
        assertThat(limits.fetchCalls.get()).as("ttl gectigi icin kaynak yeniden denendi").isEqualTo(2);
        assertThat(staleness(LIMITS)).isEqualTo(LIMITS_MAX.minusSeconds(1).toSeconds());
    }

    // --- 3. maxStaleness asildi, SECURITY_CRITICAL -> 503 PARAMETER_UNAVAILABLE

    @Test
    void beyondMaxStalenessSecurityCriticalGroupThrowsUnavailable() {
        SystemParameterProvider p = provider(limits);
        p.group(LIMITS);
        limits.down();
        clock.advance(LIMITS_MAX.plusSeconds(1));

        assertThatThrownBy(() -> p.group(LIMITS))
                .isInstanceOf(ParameterUnavailableException.class)
                .satisfies(e -> {
                    assertThat(((ParameterException) e).code()).isEqualTo("PARAMETER_UNAVAILABLE");
                    assertThat(((ParameterException) e).httpStatus()).isEqualTo(503);
                });
        assertThat(staleness(LIMITS)).isGreaterThan(LIMITS_MAX.toSeconds());
    }

    @Test
    void perGroupMaxStalenessOverridesDefaultExactlyAtBoundary() {
        SystemParameterProvider p = provider(limits);
        p.group(LIMITS);
        limits.down();
        clock.advance(LIMITS_MAX);                                   // tam sinir: hala izinli (<=)
        assertThat(p.group(LIMITS).revision()).isEqualTo(7);
        clock.advance(Duration.ofSeconds(1));                        // sinir + 1 sn: kritik grup 503
        assertThatThrownBy(() -> p.group(LIMITS)).isInstanceOf(ParameterUnavailableException.class);
    }

    // --- 4. maxStaleness asildi, NORMAL -> son bilinen deger + staleness metrigi siniri asar

    @Test
    void beyondMaxStalenessNormalGroupServesLastKnownAndKeepsPublishingStaleness() {
        SystemParameterProvider p = provider(labels);
        p.group(LABELS);
        labels.down();
        clock.advance(DEFAULT_MAX.plus(Duration.ofMinutes(30)));

        ParameterValues v = p.group(LABELS);

        assertThat(v.integer("label.max_length")).isEqualTo(40);
        assertThat(v.revision()).isEqualTo(3);
        assertThat(staleness(LABELS)).isGreaterThan(DEFAULT_MAX.toSeconds());
        clock.advance(Duration.ofMinutes(5));
        p.group(LABELS);
        assertThat(staleness(LABELS)).as("metrik her okumada guncellenir").isEqualTo(DEFAULT_MAX.toSeconds() + 35 * 60);
    }

    // --- 5. freshGroup: kaynak dusmus -> exception, bellekte veri olsa bile fallback YOK

    @Test
    void freshGroupWithSourceDownThrowsEvenWhenMemoryHasData() {
        SystemParameterProvider p = provider(limits);
        p.group(LIMITS);                                             // bellek dolu, taze
        limits.down();

        assertThatThrownBy(() -> p.freshGroup(LIMITS))
                .isInstanceOf(ParameterUnavailableException.class)
                .satisfies(e -> assertThat(((ParameterException) e).httpStatus()).isEqualTo(503));
        assertThat(limits.fetchCalls.get()).as("fresh her zaman kaynaga gider").isEqualTo(2);
        assertThat(p.group(LIMITS).revision()).as("group() yolu etkilenmez (ttl icinde bellek)").isEqualTo(7);
    }

    @Test
    void freshGroupAlwaysHitsSourceAndRefreshesMemoryAndDisk() {
        SystemParameterProvider p = provider(limits);
        p.group(LIMITS);
        limits.revision(8);
        ParameterValues v = p.freshGroup(LIMITS);                    // ttl icinde olsa da kaynak
        assertThat(v.revision()).isEqualTo(8);
        assertThat(limits.fetchCalls.get()).isEqualTo(2);
        assertThat(p.group(LIMITS).revision()).as("bellek guncellendi").isEqualTo(8);
        assertThat(disk.read(LIMITS)).map(s -> s.group().revision()).contains(8L);
    }

    // --- 6. freshGroupSince: istenen revizyon kaynaktakinden yeni -> stale exception

    @Test
    void freshGroupSinceThrowsWhenSourceRevisionIsOlderThanRequired() {
        SystemParameterProvider p = provider(limits);
        assertThatThrownBy(() -> p.freshGroupSince(LIMITS, 8))
                .isInstanceOf(ParameterRevisionStaleException.class)
                .satisfies(e -> assertThat(((ParameterException) e).code()).isEqualTo("PARAMETER_REVISION_STALE"));
        assertThat(p.freshGroupSince(LIMITS, 7).revision()).isEqualTo(7);
        assertThat(limits.sinceCalls.get()).isEqualTo(2);
        limits.down();
        assertThatThrownBy(() -> p.freshGroupSince(LIMITS, 7)).isInstanceOf(ParameterUnavailableException.class);
    }

    @Test
    void groupAtDelegatesToSourceWithoutCaching() {
        SystemParameterProvider p = provider(limits);
        assertThat(p.groupAt(LIMITS, clock.now.minus(Duration.ofDays(1))).revision()).isEqualTo(7);
        assertThat(limits.atCalls.get()).isEqualTo(1);
        assertThat(p.stalenessOf(LIMITS)).as("gecmis okuma bellege girmez").isEmpty();
        assertThat(disk.read(LIMITS)).isEmpty();
    }

    // --- 7. key yok -> PARAMETER_NOT_DEFINED, default yok

    @Test
    void missingKeyFailsClosedWithParameterNotDefined() {
        ParameterValues v = provider(limits).group(LIMITS);
        for (var read : List.<Runnable>of(
                () -> v.integer("order.min_items"),
                () -> v.duration("order.min_items"),
                () -> v.optionList("order.min_items"))) {
            assertThatThrownBy(read::run)
                    .isInstanceOf(ParameterNotDefinedException.class)
                    .satisfies(e -> assertThat(((ParameterException) e).code()).isEqualTo("PARAMETER_NOT_DEFINED"));
        }
        assertThat(v.has("order.min_items")).isFalse();
    }

    @Test
    void explicitNullValueCountsAsNotDefined() {
        limits.values(values("{\"order.max_items\": null}"));
        assertThatThrownBy(() -> provider(limits).group(LIMITS).integer("order.max_items"))
                .isInstanceOf(ParameterNotDefinedException.class);
    }

    // --- 8. tip uyusmazligi -> PARAMETER_VALUE_INVALID

    @Test
    void wrongTypeFailsClosedWithParameterValueInvalid() {
        limits.values(values("""
                {"as_text": "25", "as_decimal": 2.5, "negative": -1, "too_big": 99999999999,
                 "opts_not_array": {"code":"X"}, "opts_bad_item": [{"code":"X","labels":{"tr":"x"},"order":"1","active":true}],
                 "opts_dup": [{"code":"X","labels":{},"order":1,"active":true},{"code":"X","labels":{},"order":2,"active":true}]}
                """));
        ParameterValues v = provider(limits).group(LIMITS);
        for (var read : List.<Runnable>of(
                () -> v.integer("as_text"),
                () -> v.integer("as_decimal"),
                () -> v.integer("too_big"),
                () -> v.duration("as_text"),
                () -> v.duration("negative"),
                () -> v.optionList("opts_not_array"),
                () -> v.optionList("opts_bad_item"),
                () -> v.optionList("opts_dup"))) {
            assertThatThrownBy(read::run)
                    .isInstanceOf(ParameterValueInvalidException.class)
                    .satisfies(e -> assertThat(((ParameterException) e).code()).isEqualTo("PARAMETER_VALUE_INVALID"));
        }
    }

    @Test
    void typedAccessorsReturnCatalogTypes() {
        ParameterValues v = provider(limits).group(LIMITS);
        assertThat(v.integer("order.max_items")).isEqualTo(25);
        assertThat(v.duration("order.cancel_window")).isEqualTo(Duration.ofMinutes(15));   // saniye -> Duration
        List<ParameterOption> opts = v.optionList("order.channels");
        assertThat(opts).extracting(ParameterOption::code).containsExactly("APP", "WEB");   // order'a gore
        assertThat(opts.get(0).active()).isFalse();
        assertThat(opts.get(1).labels()).containsEntry("tr", "Web");
    }

    // --- 9. cacheTtl: 5 sn icinde kaynak bir kez, sonra yeniden

    @Test
    void cacheTtlIsRespected() {
        SystemParameterProvider p = provider(limits);
        p.group(LIMITS);
        clock.advance(Duration.ofMillis(4_999));
        p.group(LIMITS);
        p.group(LIMITS);
        assertThat(limits.fetchCalls.get()).as("5 sn icinde tek cagri").isEqualTo(1);
        limits.revision(9);
        clock.advance(Duration.ofMillis(1));                         // tam 5 sn: ttl doldu
        assertThat(p.group(LIMITS).revision()).isEqualTo(9);
        assertThat(limits.fetchCalls.get()).isEqualTo(2);
        assertThat(staleness(LIMITS)).isEqualTo(0.0);
    }

    // --- 10. disk snapshot basarida yazilir ve yeni provider instance'inda hayatta kalir

    @Test
    void diskSnapshotIsWrittenOnSuccessAndSurvivesNewProviderInstance() throws Exception {
        provider(limits).group(LIMITS);
        Path file = dir.resolve(LIMITS + ".json");
        assertThat(file).exists();
        assertThat(Files.readString(file)).contains("\"revision\":7").contains("order.max_items");

        clock.advance(Duration.ofMinutes(3));
        limits.down();
        meters = new SimpleMeterRegistry();                          // yeni process = yeni registry, yeni store nesnesi
        SystemParameterProvider restarted = new SystemParameterProvider(limits, new DiskSnapshotStore(dir, mapper),
                props, clock, meters);
        ParameterValues v = restarted.group(LIMITS);
        assertThat(v.revision()).isEqualTo(7);
        assertThat(v.integer("order.max_items")).isEqualTo(25);
        assertThat(staleness(LIMITS)).isEqualTo(180.0);
        clock.advance(Duration.ofMinutes(8));                        // 11 dk: kritik grup artik 503
        assertThatThrownBy(() -> restarted.group(LIMITS)).isInstanceOf(ParameterUnavailableException.class);
    }

    @Test
    void diskSnapshotIsRefreshedOnEverySuccessfulFetch() {
        SystemParameterProvider p = provider(limits);
        p.group(LIMITS);
        limits.revision(8);
        clock.advance(TTL);
        p.group(LIMITS);
        assertThat(disk.read(LIMITS)).map(s -> s.group().revision()).contains(8L);
        assertThat(disk.read(LIMITS)).map(ParameterSnapshot::fetchedAt).contains(clock.now);
    }

    // --- 11. acilis kontrolu: eksik key -> fail fast

    @Test
    void startupCheckFailsFastOnMissingKey() {
        StartupParameterCheck check = new StartupParameterCheck(provider(limits));
        check.verify(List.of(
                new RequiredParameter(LIMITS, "order.max_items", ParameterType.INTEGER),
                new RequiredParameter(LIMITS, "order.cancel_window", ParameterType.DURATION),
                new RequiredParameter(LIMITS, "order.channels", ParameterType.OPTION_LIST)));

        assertThatThrownBy(() -> check.verify(List.of(
                new RequiredParameter(LIMITS, "order.max_items", ParameterType.INTEGER),
                new RequiredParameter(LIMITS, "order.min_items", ParameterType.INTEGER),
                new RequiredParameter(LIMITS, "order.max_weight", ParameterType.INTEGER))))
                .isInstanceOf(ParameterNotDefinedException.class)
                .hasMessageContaining("order.min_items").hasMessageContaining("order.max_weight");
    }

    @Test
    void startupCheckFailsFastOnWrongTypeAndWorksFromDiskWhenSourceIsDown() {
        StartupParameterCheck check = new StartupParameterCheck(provider(limits));
        assertThatThrownBy(() -> check.verify(List.of(new RequiredParameter(LIMITS, "order.channels", ParameterType.INTEGER))))
                .isInstanceOf(ParameterValueInvalidException.class);

        limits.down();                                                // control plane kapali, disk snapshot yeterli
        new StartupParameterCheck(provider(limits))
                .verify(List.of(new RequiredParameter(LIMITS, "order.max_items", ParameterType.INTEGER)));
    }

    // --- 12. revizyon disari verilir: kalici sonuca deger + revizyon snapshot'i yazilabilir

    @Test
    void revisionIsExposedForPersistingWithResults() {
        SystemParameterProvider p = provider(limits);
        ParameterValues v = p.group(LIMITS);
        assertThat(v.revision()).isEqualTo(7);
        assertThat(v.group()).isEqualTo(LIMITS);
        assertThat(v.criticality()).isEqualTo(Criticality.SECURITY_CRITICAL);
        limits.revision(12);
        assertThat(p.freshGroup(LIMITS).revision()).isEqualTo(12);
        assertThat(p.freshGroupSince(LIMITS, 12).revision()).isEqualTo(12);
    }
}
