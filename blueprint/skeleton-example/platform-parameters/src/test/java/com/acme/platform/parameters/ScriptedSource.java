package com.acme.platform.parameters;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.JsonNode;

/**
 * Senaryolanabilir sahte kaynak: saglikli (revizyon N ile doner) / kapali (ParameterSourceException). Cagri sayar,
 * boylece cacheTtl ("5 sn icinde bir kez") ve "fresh her zaman kaynaga gider" iddialari sayiyla kanitlanir.
 */
final class ScriptedSource implements ParameterSource {
    private volatile boolean down;
    private volatile long revision;
    private volatile Map<String, JsonNode> values;
    private volatile Criticality criticality;
    final AtomicInteger fetchCalls = new AtomicInteger();
    final AtomicInteger sinceCalls = new AtomicInteger();
    final AtomicInteger atCalls = new AtomicInteger();

    ScriptedSource(long revision, Map<String, JsonNode> values, Criticality criticality) {
        this.revision = revision;
        this.values = values;
        this.criticality = criticality;
    }

    ScriptedSource down() { down = true; return this; }
    ScriptedSource up() { down = false; return this; }
    ScriptedSource revision(long r) { revision = r; return this; }
    ScriptedSource values(Map<String, JsonNode> v) { values = v; return this; }

    @Override public ParameterGroupDto fetch(String group) {
        fetchCalls.incrementAndGet();
        return respond(group);
    }

    @Override public ParameterGroupDto fetchSince(String group, long minRevision) {
        sinceCalls.incrementAndGet();
        return respond(group);       // gercek uc da mevcut revizyonu doner; alt sinir kontrolu tuketicidedir
    }

    @Override public ParameterGroupDto fetchAt(String group, Instant at) {
        atCalls.incrementAndGet();
        return respond(group);
    }

    private ParameterGroupDto respond(String group) {
        if (down) throw new ParameterSourceException("baglanti reddedildi: " + group);
        return new ParameterGroupDto(group, revision, values, criticality);
    }
}
