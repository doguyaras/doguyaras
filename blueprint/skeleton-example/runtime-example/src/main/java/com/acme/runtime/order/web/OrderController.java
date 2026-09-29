package com.acme.runtime.order.web;

import com.acme.platform.core.ErrorCode;
import com.acme.platform.security.web.CurrentAccount;
import com.acme.runtime.order.exception.OrderRuntimeErrorCode;
import com.acme.runtime.order.flow.OrderCreationFlow;
import com.acme.runtime.order.flow.OrderCreationFlow.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /v1/orders. Hesap @CurrentAccount ile gateway token'inin sub'indan gelir (govdeden asla). Yanit {kind, detail,
 * sagaId}; hata durumlarinda ayrica Bolum 6.2 zarfi (ok=false, error{code,message,service}).
 */
@RestController
public class OrderController {

    public record CreateOrderRequest(@NotNull UUID resourceId) {}

    private final OrderCreationFlow flow;

    public OrderController(OrderCreationFlow flow) { this.flow = flow; }

    @PostMapping("/v1/orders")
    public ResponseEntity<Map<String, Object>> create(@CurrentAccount UUID accountId,
                                                      @RequestHeader("X-Idempotency-Key") UUID operationKey,
                                                      @Valid @RequestBody CreateOrderRequest request) {
        Result r = flow.createOrder(accountId, operationKey, request.resourceId());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", r.kind() == OrderCreationFlow.Kind.OK || r.kind() == OrderCreationFlow.Kind.REPLAY);
        body.put("kind", r.kind().name());
        body.put("detail", r.detail());
        body.put("sagaId", r.sagaId());
        ErrorCode error = switch (r.kind()) {
            case OK, REPLAY -> null;
            case IN_PROGRESS -> OrderRuntimeErrorCode.OPERATION_IN_PROGRESS;
            case CANCELLED -> OrderRuntimeErrorCode.OPERATION_CANCELLED;
            case REJECTED -> OrderRuntimeErrorCode.QUOTA_REJECTED;
            case UPSTREAM_UNAVAILABLE -> OrderRuntimeErrorCode.UPSTREAM_UNAVAILABLE;
            case UPSTREAM_REJECTED -> OrderRuntimeErrorCode.UPSTREAM_REJECTED;
            case DOMAIN_CONFLICT -> OrderRuntimeErrorCode.RESOURCE_ALREADY_ORDERED;
        };
        if (error == null) {
            return ResponseEntity.status(r.kind() == OrderCreationFlow.Kind.OK ? HttpStatus.CREATED : HttpStatus.OK).body(body);
        }
        body.put("error", Map.of("code", error.getCode(), "message", error.getMessage(), "service", error.getService()));
        return ResponseEntity.status(error.getHttpStatus()).body(body);
    }
}
