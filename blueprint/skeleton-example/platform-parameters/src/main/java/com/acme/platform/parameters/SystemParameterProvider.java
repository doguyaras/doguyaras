package com.acme.platform.parameters;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servis basina tek parametre saglayici (referans Bolum 14, 4.7 "control plane"). Kaynak bir control plane'dir;
 * dustugunde data plane durmaz: bounded staleness.
 *
 * group(g):        cacheTtl icinde bellek; sonra kaynak; kaynak yoksa son bilinen (bellek, soguk acilista disk)
 *                  maxStaleness(g) suresince; asilinca SECURITY_CRITICAL 503, NORMAL son bilinenle devam.
 *                  parameter_staleness_seconds{group} her okumada yayinlanir; esik alarmi ops'un isidir.
 * freshGroup:      her zaman kaynak; hata = 503, fallback YOK (kullanici girdisini dogrulayan yazma akislari).
 * freshGroupSince: kaynak + revizyon alt siniri (clamp); eski revizyon = 503.
 * groupAt:         gecmis anin degeri, dogrudan kaynak.
 *
 * Parametre TX ve lock disinda okunur; worker her turda yeniden okur. Deger ve key adi loglanmaz.
 */
public final class SystemParameterProvider {
    private static final Logger log = LoggerFactory.getLogger(SystemParameterProvider.class);
    public static final String STALENESS_METRIC = "parameter_staleness_seconds";

    private final ParameterSource source;
    private final DiskSnapshotStore snapshots;
    private final ParameterProperties props;
    private final Clock clock;
    private final MeterRegistry meters;
    private final ConcurrentHashMap<String, ParameterSnapshot> memory = new ConcurrentHashMap<>();
    /** Gauge durumlari guclu referansla tutulur; Micrometer zayif referans kullanir, aksi halde metrik NaN'a duser. */
    private final ConcurrentHashMap<String, AtomicLong> staleness = new ConcurrentHashMap<>();

    public SystemParameterProvider(ParameterSource source, DiskSnapshotStore snapshots, ParameterProperties props,
                                   Clock clock, MeterRegistry meters) {
        this.source = source;
        this.snapshots = snapshots;
        this.props = props;
        this.clock = clock;
        this.meters = meters;
    }

    public ParameterValues group(String group) {
        Instant now = clock.instant();
        ParameterSnapshot cached = memory.get(group);
        if (cached != null && Duration.between(cached.fetchedAt(), now).compareTo(props.cacheTtl()) < 0) {
            publishStaleness(group, cached, now);
            return new ParameterValues(cached.group());
        }
        ParameterGroupDto dto;
        try {
            dto = source.fetch(group);
        } catch (ParameterSourceException e) {
            return serveLastKnown(group, cached, now, e);
        }
        ParameterSnapshot fresh = remember(dto, now);
        publishStaleness(group, fresh, now);
        return new ParameterValues(dto);
    }

    public ParameterValues freshGroup(String group) {
        ParameterGroupDto dto;
        try {
            dto = source.fetch(group);
        } catch (ParameterSourceException e) {
            throw new ParameterUnavailableException(group, "fresh okuma, fallback yok", e);
        }
        remember(dto, clock.instant());
        return new ParameterValues(dto);
    }

    public ParameterValues freshGroupSince(String group, long minRevision) {
        ParameterGroupDto dto;
        try {
            dto = source.fetchSince(group, minRevision);
        } catch (ParameterSourceException e) {
            throw new ParameterUnavailableException(group, "since okuma, fallback yok", e);
        }
        if (dto.revision() < minRevision) throw new ParameterRevisionStaleException(group, minRevision, dto.revision());
        remember(dto, clock.instant());
        return new ParameterValues(dto);
    }

    /** Gecmis anin degeri: cache'lenmez, snapshot'a yazilmaz (guncel deger degildir). */
    public ParameterValues groupAt(String group, Instant at) {
        try {
            return new ParameterValues(source.fetchAt(group, at));
        } catch (ParameterSourceException e) {
            throw new ParameterUnavailableException(group, "at okuma, fallback yok", e);
        }
    }

    /** Bellekteki son basarili okumanin yasi; health indicator ve testler icin. */
    public Optional<Duration> stalenessOf(String group) {
        ParameterSnapshot s = memory.get(group);
        return s == null ? Optional.empty() : Optional.of(Duration.between(s.fetchedAt(), clock.instant()));
    }

    private ParameterValues serveLastKnown(String group, ParameterSnapshot cached, Instant now, ParameterSourceException cause) {
        ParameterSnapshot last = cached;
        if (last == null) {
            // soguk acilis: bellek bos, diskteki son snapshot ile ayaga kalk
            last = snapshots.read(group).orElse(null);
            if (last != null) memory.putIfAbsent(group, last);
        }
        if (last == null) throw new ParameterUnavailableException(group, "son bilinen deger yok", cause);
        Duration age = publishStaleness(group, last, now);
        Duration limit = props.maxStaleness(group);
        if (age.compareTo(limit) > 0 && last.group().criticality() == Criticality.SECURITY_CRITICAL) {
            throw new ParameterUnavailableException(group, "guvenlik-kritik grup maxStaleness'i asti", cause);
        }
        if (age.compareTo(limit) > 0) {
            log.warn("parametre grubu {} maxStaleness'i asti ({} sn), son bilinen degerle devam", group, age.toSeconds());
        }
        return new ParameterValues(last.group());
    }

    private ParameterSnapshot remember(ParameterGroupDto dto, Instant now) {
        ParameterSnapshot snapshot = new ParameterSnapshot(dto, now);
        memory.put(dto.group(), snapshot);
        try {
            snapshots.write(snapshot);
        } catch (RuntimeException e) {
            // disk yalniz soguk acilis yedegidir; yazilamamasi guncel degeri servis etmeyi engellemez
            log.warn("parametre snapshot'i diske yazilamadi: {}", dto.group(), e);
        }
        return snapshot;
    }

    private Duration publishStaleness(String group, ParameterSnapshot snapshot, Instant now) {
        Duration age = Duration.between(snapshot.fetchedAt(), now);
        staleness.computeIfAbsent(group, g -> meters.gauge(STALENESS_METRIC, Tags.of("group", g), new AtomicLong()))
                .set(age.toSeconds());
        return age;
    }
}
