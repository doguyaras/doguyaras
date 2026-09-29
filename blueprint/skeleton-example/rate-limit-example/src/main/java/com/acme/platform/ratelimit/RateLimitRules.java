package com.acme.platform.ratelimit;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;

/** Scope -> kural kaydi. Degismez; kural olmayan scope icin karar limiter'da fail-closed'dur (config hatasi). */
public final class RateLimitRules {

    private final Map<String, RateLimitRule> byScope;

    private RateLimitRules(Map<String, RateLimitRule> byScope) { this.byScope = Map.copyOf(byScope); }

    public static RateLimitRules of(RateLimitRule... rules) { return of(List.of(rules)); }

    public static RateLimitRules of(Collection<RateLimitRule> rules) {
        Map<String, RateLimitRule> m = new LinkedHashMap<>();
        for (RateLimitRule r : rules) {
            if (m.putIfAbsent(r.scope(), r) != null) throw new IllegalArgumentException("scope iki kez tanimli: " + r.scope());
        }
        return new RateLimitRules(m);
    }

    /** {@code rate-limit.rules.<scope>.{limit, window-seconds, fail-policy}} baglama sekli. */
    public record RuleProperties(Integer limit, Integer windowSeconds, FailPolicy failPolicy) {}

    /**
     * {@code rate-limit.rules.<scope>.*} baglar. Eksik alan acilista patlar (limit/window/fail-policy icin
     * varsayilan yoktur): yanlis/eksik kural uretimde "limitsiz" ya da "hep 503" olarak degil, deploy'da gorunur.
     */
    public static RateLimitRules bind(Binder binder) {
        Map<String, RuleProperties> raw = binder
                .bind("rate-limit.rules", Bindable.mapOf(String.class, RuleProperties.class))
                .orElse(Map.of());
        return of(raw.entrySet().stream().map(e -> {
            RuleProperties p = e.getValue();
            if (p.limit() == null || p.windowSeconds() == null) {
                throw new IllegalArgumentException("limit ve window-seconds zorunlu: " + e.getKey());
            }
            return new RateLimitRule(e.getKey(), p.limit(), p.windowSeconds(), p.failPolicy());
        }).toList());
    }

    public Optional<RateLimitRule> find(String scope) { return Optional.ofNullable(byScope.get(scope)); }

    public Collection<RateLimitRule> all() { return byScope.values(); }
}
