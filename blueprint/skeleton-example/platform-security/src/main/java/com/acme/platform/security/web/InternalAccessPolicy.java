package com.acme.platform.security.web;

import java.util.List;
import java.util.Set;
import org.springframework.util.AntPathMatcher;

/**
 * Bolum 9.5 internal-access allowlist: FIRST-MATCH. Path'e uyan ILK kural karar verir; sonraki kurallara bakilmaz.
 * Bu yuzden dar kurallar catch-all'dan once yazilir. Hicbir kural uymazsa DEFAULT-DENY (Bolum 9.4 adim 7).
 */
public final class InternalAccessPolicy {

    public record Rule(String path, Set<String> allowedActors) {
        public Rule {
            if (path == null || path.isBlank()) throw new IllegalArgumentException("internal-access rule without path");
            if (allowedActors == null || allowedActors.isEmpty()) {
                throw new IllegalArgumentException("internal-access rule " + path + " without allowed-actors");
            }
            allowedActors = Set.copyOf(allowedActors);
        }
    }

    private final List<Rule> rules;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public InternalAccessPolicy(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<Rule> rules() { return rules; }

    /** @param normalizedPath InternalPathNormalizer'dan gecmis path; ham URI ile CAGRILMAZ */
    public boolean allows(String normalizedPath, String actor) {
        for (Rule rule : rules) {
            if (matcher.match(rule.path(), normalizedPath)) return rule.allowedActors().contains(actor);
        }
        return false;
    }
}
