package com.acme.platform.messaging.readmodel;

import java.time.Duration;

/**
 * Karar kurali (referans Bolum 4.6): her karar icin "en fazla ne kadar eski bilgiyle verilebilir" ayri cevaplanir
 * (engel karari <= 30 sn, aktiflik bayragi <= 5 dk). Satir yoksa ya da konum maxLag'i asiyorsa FAIL_CLOSED:
 * cagiran kaynaga sorar veya reddeder; read-model karar verdirir ama kaynak degildir.
 */
public class StalenessGuard {

    public enum Decision { ALLOW, FAIL_CLOSED }

    private final ReadModelFreshness freshness;

    public StalenessGuard(ReadModelFreshness freshness) {
        this.freshness = freshness;
    }

    public Decision decide(String source, Duration maxLag, boolean rowPresent) {
        if (!rowPresent) return Decision.FAIL_CLOSED;                           // varsayilan: satir yok = bilinmiyor
        return freshness.lag(source)
                .filter(lag -> lag.compareTo(maxLag) <= 0)
                .map(lag -> Decision.ALLOW)
                .orElse(Decision.FAIL_CLOSED);                                  // konum yok veya eski
    }
}
