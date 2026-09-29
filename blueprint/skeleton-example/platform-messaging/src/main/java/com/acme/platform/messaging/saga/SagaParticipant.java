package com.acme.platform.messaging.saga;

import java.util.Optional;
import java.util.UUID;

/**
 * Katilimci sozlesmesi (referans Bolum 11.4): gercekte /internal/<kaynak>/operations/{operationKey}/{consume,confirm,compensate}
 * ve GET; burada HTTP client'in arkasindaki arayuz. Tum islemler idempotent: ayni key ile tekrar cagri ayni sonucu dondurur.
 *
 * <pre>
 * consume    kayit yok    -> APPLIED / REJECTED
 * consume    kayit var    -> replay (mevcut durum; CANCELLED tombstone ise uygulanmaz)
 * confirm    APPLIED      -> CONFIRMED
 * compensate kayit yok    -> CANCELLED tombstone (gec gelen consume uygulanmaz)
 * compensate APPLIED      -> iade -> COMPENSATED
 * compensate CONFIRMED    -> MANUAL_REVIEW (iade yok)
 * </pre>
 */
public interface SagaParticipant {

    enum State { APPLIED, REJECTED, CONFIRMED, CANCELLED, COMPENSATED, MANUAL_REVIEW }

    State consume(String callerService, UUID accountId, UUID operationKey, String operationType, int amount);

    Optional<State> get(String callerService, UUID accountId, UUID operationKey);

    State confirm(String callerService, UUID accountId, UUID operationKey);

    State compensate(String callerService, UUID accountId, UUID operationKey);

    /** Timeout, 5xx, baglanti hatasi: sonuc BELIRSIZ; koordinator GET ile sorar, sonra retry/backoff. */
    class ParticipantUnavailableException extends RuntimeException {
        public ParticipantUnavailableException(String message) { super(message); }
    }

    /** Katilimci istegi anlamsiz buldu (409): durumlar celisiyor; koordinator MANUAL_REVIEW'a alir. */
    class ParticipantConflictException extends RuntimeException {
        public ParticipantConflictException(String message) { super(message); }
    }

    /** Aktor bu islem tipi icin yetkili degil (403); saga acilmaz / adim MANUAL_REVIEW. */
    class ParticipantForbiddenException extends RuntimeException {
        public ParticipantForbiddenException(String message) { super(message); }
    }
}
