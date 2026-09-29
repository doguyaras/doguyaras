package com.acme.platform.messaging.saga;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hata enjeksiyonu: gercek HTTP katmaninin uretecegi belirsizlikleri taklit eder.
 * FAIL: istek katilimciya hic ulasmadi (timeout/baglanti). LOSE_RESPONSE: katilimci commit etti, yanit kayboldu.
 */
public class FlakyParticipant implements SagaParticipant {

    enum Fault { FAIL, LOSE_RESPONSE }

    private final SagaParticipant delegate;
    private final Map<String, Deque<Fault>> faults = new ConcurrentHashMap<>();

    public FlakyParticipant(SagaParticipant delegate) { this.delegate = delegate; }

    public FlakyParticipant failNext(String method) { faults.computeIfAbsent(method, k -> new ArrayDeque<>()).add(Fault.FAIL); return this; }
    public FlakyParticipant loseResponseNext(String method) { faults.computeIfAbsent(method, k -> new ArrayDeque<>()).add(Fault.LOSE_RESPONSE); return this; }

    private <T> T call(String method, java.util.function.Supplier<T> real) {
        Deque<Fault> q = faults.get(method);
        Fault f = q == null ? null : q.poll();
        if (f == Fault.FAIL) throw new ParticipantUnavailableException(method + " timeout");
        T result = real.get();
        if (f == Fault.LOSE_RESPONSE) throw new ParticipantUnavailableException(method + " response lost");
        return result;
    }

    @Override public State consume(String c, UUID a, UUID k, String t, int n) { return call("consume", () -> delegate.consume(c, a, k, t, n)); }
    @Override public Optional<State> get(String c, UUID a, UUID k) { return call("get", () -> delegate.get(c, a, k)); }
    @Override public State confirm(String c, UUID a, UUID k) { return call("confirm", () -> delegate.confirm(c, a, k)); }
    @Override public State compensate(String c, UUID a, UUID k) { return call("compensate", () -> delegate.compensate(c, a, k)); }
}
