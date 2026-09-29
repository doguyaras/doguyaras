package com.acme.runtime.subscription.chaos;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * YALNIZ TEST: hata enjeksiyonu ayarlari ve gozlem sayaclari. runtime.chaos.enabled=false iken bean yoktur.
 *
 * slowMillis      is mantigi + commit bittikten SONRA yanit bu kadar geciktirilir (commit olmus ama yanit gec)
 * dropAfterCommit is mantigi commit olur, yanit yarim yazilip baglanti kapatilir (yanit kayboldu)
 * down            is mantigina hic girilmeden 503 (commit yok)
 * operations      bos = tum islemler; aksi halde yalniz listelenenler (consume, confirm, compensate, get)
 */
public class ChaosState {

    public record Settings(Long slowMillis, Boolean dropAfterCommit, Boolean down, Set<String> operations) {
        public static final Settings NONE = new Settings(0L, false, false, Set.of());
        public Settings {                                                // JSON'da verilmeyen alan = etkisiz
            slowMillis = slowMillis == null ? 0L : slowMillis;
            dropAfterCommit = Boolean.TRUE.equals(dropAfterCommit);
            down = Boolean.TRUE.equals(down);
            operations = operations == null ? Set.of() : Set.copyOf(operations);
        }
        boolean appliesTo(String operation) { return operations.isEmpty() || operations.contains(operation); }
    }

    private volatile Settings settings = Settings.NONE;
    final AtomicInteger inFlight = new AtomicInteger();
    final AtomicInteger maxInFlight = new AtomicInteger();
    final Map<String, String> tracesByPath = new ConcurrentHashMap<>();

    public Settings settings() { return settings; }
    public void set(Settings s) { settings = s == null ? Settings.NONE : s; }

    public void resetStats() { maxInFlight.set(inFlight.get()); tracesByPath.clear(); }

    public Map<String, Object> stats() { return Map.of("inFlight", inFlight.get(), "maxInFlight", maxInFlight.get()); }

    void enter() {
        int now = inFlight.incrementAndGet();
        maxInFlight.accumulateAndGet(now, Math::max);
    }

    void exit() { inFlight.decrementAndGet(); }
}
