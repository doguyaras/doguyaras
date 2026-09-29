package com.acme.runtime.subscription.web;

import com.acme.platform.core.ServiceException;
import com.acme.platform.messaging.saga.SagaParticipant.State;
import com.acme.platform.security.delegation.RequireOperation;
import com.acme.platform.security.web.ServiceRequestAttributes;
import com.acme.runtime.subscription.quota.QuotaOperationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Katilimci sozlesmesi (referans Bolum 11.4): consume / GET / confirm / compensate. Uc guvence birlikte calisir
 * (Bolum 9.2.1): filtre token'i dogrular ve allowlist'i uygular, DelegationInterceptor @RequireOperation ile sub ==
 * {accountId} sartini arar (arka plan token'i reddedilir), servis aktor -> islem tipi haritasini uygular.
 * Cagiran kimligi govdeden/header'dan degil, dogrulanmis act claim'inden okunur.
 */
@RestController
@RequestMapping("/internal/subscription/accounts/{accountId}/operations/{operationKey}")
public class InternalSubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(InternalSubscriptionController.class);

    public record ConsumeRequest(@NotBlank String operationType, @Min(1) @Max(100) int amount) {}
    public record OperationResponse(String state) {}

    private final QuotaOperationService service;

    public InternalSubscriptionController(QuotaOperationService service) { this.service = service; }

    @PostMapping("/consume")
    @RequireOperation("subscription.consume")
    public OperationResponse consume(@PathVariable UUID accountId, @PathVariable UUID operationKey,
                                     @Valid @RequestBody ConsumeRequest body, HttpServletRequest request) {
        State state = service.consume(caller(request), accountId, operationKey, body.operationType(), body.amount());
        log.info("quota consume: operationKey={} state={}", operationKey, state);
        return new OperationResponse(state.name());
    }

    @GetMapping
    @RequireOperation("subscription.read")
    public OperationResponse get(@PathVariable UUID accountId, @PathVariable UUID operationKey, HttpServletRequest request) {
        return service.get(caller(request), accountId, operationKey).map(s -> new OperationResponse(s.name()))
                .orElseThrow(() -> new ServiceException(SubscriptionErrorCode.OPERATION_NOT_FOUND));
    }

    @PostMapping("/confirm")
    @RequireOperation("subscription.confirm")
    public OperationResponse confirm(@PathVariable UUID accountId, @PathVariable UUID operationKey, HttpServletRequest request) {
        return new OperationResponse(service.confirm(caller(request), accountId, operationKey).name());
    }

    @PostMapping("/compensate")
    @RequireOperation("subscription.compensate")
    public OperationResponse compensate(@PathVariable UUID accountId, @PathVariable UUID operationKey, HttpServletRequest request) {
        return new OperationResponse(service.compensate(caller(request), accountId, operationKey).name());
    }

    /** Filtre bu attribute'u yazmadan istek buraya ulasamaz; yine de yoksa fail-closed. */
    private static String caller(HttpServletRequest request) {
        return ServiceRequestAttributes.callerService(request).orElseThrow(() -> new IllegalStateException("caller identity missing"));
    }
}
