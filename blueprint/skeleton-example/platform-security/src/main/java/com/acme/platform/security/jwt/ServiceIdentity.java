package com.acme.platform.security.jwt;

import java.util.UUID;

/**
 * Dogrulanmis servis kimligi. actor = act claim'i (yoksa iss); accountId = sub (arka plan isinde null).
 * background = sub yok: kullanici-yetkisi isteyen islemler bu token'i kabul etmez (Bolum 9.2.1).
 */
public record ServiceIdentity(String actor, String issuer, UUID accountId, String tokenId) {
    public boolean background() { return accountId == null; }
}
