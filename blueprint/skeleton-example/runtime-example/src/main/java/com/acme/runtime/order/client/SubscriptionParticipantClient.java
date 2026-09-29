package com.acme.runtime.order.client;

import com.acme.platform.messaging.saga.SagaParticipant;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * SagaParticipant'in HTTP implementasyonu (referans Bolum 6.8, 11.4). Istek yolu (consume) ve recovery worker
 * (confirm/compensate/get) AYNI client'i, dolayisiyla ayni circuit breaker ve bulkhead'i kullanir: hedef basina tek
 * koruma. Sira CircuitBreaker(Bulkhead(http)): circuit acikken bulkhead izni bile alinmaz, thread bloke olmaz.
 *
 * Hata cevirisi (govde okunmaz/loglanmaz): 401/403 -> Forbidden, 409 ve diger 4xx -> Conflict, 5xx / IO / timeout /
 * circuit acik / bulkhead dolu -> Unavailable (sonuc belirsiz; koordinator GET ile sorar). Retry YOK (Bolum 4.7):
 * tekrar deneme saga worker'inda backoff ile yapilir.
 *
 * sub: servis JWT'sindeki sub her cagrida saga'nin hesabidir. Worker arka planda calissa da sub'i kendi akisinda
 * (saga kaydinda) gordugu hesaptan aktarir; katilimcinin delegasyon kurali REQUIRED oldugu icin bu zorunludur.
 */
public class SubscriptionParticipantClient implements SagaParticipant {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionParticipantClient.class);
    private static final String BASE = "/internal/subscription/accounts/{accountId}/operations/{operationKey}";
    private static final ThreadLocal<UUID> SUBJECT = new ThreadLocal<>();

    record OperationResponse(String state) {}

    private final RestClient rest;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final String serviceName;

    /**
     * @param rest RestClient: base-url, timeout'lu request factory ve ServiceJwtClientInterceptor(subjectContext()) ile
     *             kurulmus olmali (OrderConfig).
     */
    public SubscriptionParticipantClient(RestClient rest, CircuitBreaker circuitBreaker, Bulkhead bulkhead, String serviceName) {
        this.rest = Objects.requireNonNull(rest);
        this.circuitBreaker = Objects.requireNonNull(circuitBreaker);
        this.bulkhead = Objects.requireNonNull(bulkhead);
        this.serviceName = Objects.requireNonNull(serviceName);
    }

    /** ServiceJwtClientInterceptor'un sub kaynagi: o anki cagrinin hesabi (yalniz bu client'in cagri suresince dolu). */
    public static Supplier<Optional<UUID>> subjectContext() { return () -> Optional.ofNullable(SUBJECT.get()); }

    @Override
    public State consume(String callerService, UUID accountId, UUID operationKey, String operationType, int amount) {
        requireSelf(callerService);
        return call("consume", accountId, () -> state(rest.post().uri(BASE + "/consume", accountId, operationKey)
                .body(Map.of("operationType", operationType, "amount", amount))
                .retrieve().body(OperationResponse.class)));
    }

    @Override
    public Optional<State> get(String callerService, UUID accountId, UUID operationKey) {
        requireSelf(callerService);
        return call("get", accountId, () -> {
            try {
                return Optional.of(state(rest.get().uri(BASE, accountId, operationKey).retrieve().body(OperationResponse.class)));
            } catch (HttpClientErrorException.NotFound e) {
                return Optional.<State>empty();                             // kayit yok: consume hic uygulanmadi
            }
        });
    }

    @Override
    public State confirm(String callerService, UUID accountId, UUID operationKey) {
        requireSelf(callerService);
        return call("confirm", accountId, () -> state(rest.post().uri(BASE + "/confirm", accountId, operationKey)
                .retrieve().body(OperationResponse.class)));
    }

    @Override
    public State compensate(String callerService, UUID accountId, UUID operationKey) {
        requireSelf(callerService);
        return call("compensate", accountId, () -> state(rest.post().uri(BASE + "/compensate", accountId, operationKey)
                .retrieve().body(OperationResponse.class)));
    }

    private <T> T call(String operation, UUID accountId, Supplier<T> http) {
        Supplier<T> translated = () -> translate(operation, accountId, http);
        Supplier<T> guarded = CircuitBreaker.decorateSupplier(circuitBreaker, Bulkhead.decorateSupplier(bulkhead, translated));
        try {
            return guarded.get();
        } catch (CallNotPermittedException e) {
            log.warn("participant call short-circuited: target=subscription operation={} circuitState=OPEN", operation);
            throw new ParticipantUnavailableException("circuit open");
        } catch (BulkheadFullException e) {
            log.warn("participant call rejected: target=subscription operation={} reason=BULKHEAD_FULL", operation);
            throw new ParticipantUnavailableException("bulkhead full");
        }
    }

    /** Circuit breaker cevrilmis istisnayi gorur: yalniz ParticipantUnavailableException hata sayilir (order-app.yml). */
    private <T> T translate(String operation, UUID accountId, Supplier<T> http) {
        SUBJECT.set(accountId);
        try {
            return http.get();
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            log.warn("participant call failed: target=subscription operation={} status={}", operation, status.value());
            if (status.is5xxServerError() || status.value() == 429) throw new ParticipantUnavailableException("upstream " + status.value());
            if (status.value() == 401 || status.value() == 403) throw new ParticipantForbiddenException("upstream " + status.value());
            throw new ParticipantConflictException("upstream " + status.value());
        } catch (RestClientException e) {
            // ResourceAccessException (connect/read timeout, baglanti koptu) ve yarim govde: sonuc belirsiz
            log.warn("participant call failed: target=subscription operation={} exceptionType={}", operation,
                    e.getClass().getSimpleName());
            throw new ParticipantUnavailableException("upstream io: " + e.getClass().getSimpleName());
        } finally {
            SUBJECT.remove();
        }
    }

    private static State state(OperationResponse body) {
        if (body == null || body.state() == null) throw new ParticipantUnavailableException("empty participant response");
        return State.valueOf(body.state());
    }

    /** act claim'i imzalayan anahtardan gelir; parametre yalniz tutarlilik kontrolu icin. */
    private void requireSelf(String callerService) {
        if (!serviceName.equals(callerService)) {
            throw new IllegalArgumentException("client signs as " + serviceName + ", not " + callerService);
        }
    }
}
