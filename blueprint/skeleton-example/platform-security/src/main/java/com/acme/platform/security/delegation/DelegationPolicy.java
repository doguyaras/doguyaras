package com.acme.platform.security.delegation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bolum 9.2.1 delegasyon matrisi: (aktor, islem) -> kullanici baglami kurali. Servis kimligi (allowlist) kullanici
 * adina islem yetkisi DEGILDIR; bu politika ucuncu kontrolu yapar:
 *  REQUIRED  -> token'da sub olmali VE path'teki {accountId} ile ayni olmali (baskasinin hesabi adina islem yok)
 *  FORBIDDEN -> arka plan isi; sub tasiyan token bu islemi yapamaz
 *  OPTIONAL  -> her ikisi de olur (ornek: bildirim komutlari)
 * Kural yoksa DENY: "her kullanici adina her sey" satiri yoktur.
 */
public final class DelegationPolicy {

    public enum UserContext { REQUIRED, FORBIDDEN, OPTIONAL }

    public record Rule(String actor, String operation, UserContext userContext) {
        public Rule {
            if (actor == null || actor.isBlank()) throw new IllegalArgumentException("delegation rule without actor");
            if (operation == null || operation.isBlank()) throw new IllegalArgumentException("delegation rule without operation");
            if (userContext == null) throw new IllegalArgumentException("delegation rule " + actor + "/" + operation + " without user-context");
        }
    }

    public record Decision(boolean allowed, String reason) {
        static Decision allow() { return new Decision(true, null); }
        static Decision deny(String reason) { return new Decision(false, reason); }
    }

    private final List<Rule> rules;

    public DelegationPolicy(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<Rule> rules() { return rules; }

    public Optional<Rule> find(String actor, String operation) {
        return rules.stream().filter(r -> r.actor().equals(actor) && r.operation().equals(operation)).findFirst();
    }

    /**
     * @param tokenAccount token'daki sub (arka plan isinde null)
     * @param pathAccount  path'teki {accountId} (ucta yoksa null)
     */
    public Decision decide(String actor, String operation, UUID tokenAccount, UUID pathAccount) {
        Optional<Rule> rule = find(actor, operation);
        if (rule.isEmpty()) return Decision.deny("no delegation rule for actor " + actor + " on " + operation);
        return switch (rule.get().userContext()) {
            case REQUIRED -> {
                if (tokenAccount == null) yield Decision.deny("operation requires user context, token is background");
                if (pathAccount == null) yield Decision.deny("operation requires user context but path carries no accountId");
                if (!tokenAccount.equals(pathAccount)) yield Decision.deny("token subject does not own the path account");
                yield Decision.allow();
            }
            case FORBIDDEN -> tokenAccount == null ? Decision.allow()
                    : Decision.deny("background-only operation called with user context");
            case OPTIONAL -> Decision.allow();
        };
    }
}
