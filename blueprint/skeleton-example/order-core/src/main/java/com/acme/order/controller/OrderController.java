package com.acme.order.controller;

import com.acme.order.api.dto.CreateOrderRequest;
import com.acme.order.service.OrderService;
import com.acme.order.service.OrderSummary;
import com.acme.order.web.CurrentAccount;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST. Major surum path'te (/v1), minor surum Spring Framework 7 API versiyonlama ile
 * (API-Version header'i; referans Bolum 20). Kimlik yalniz @CurrentAccount'tan gelir (Bolum 6.5).
 */
@RestController
@RequestMapping("/v1/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<UUID> create(@CurrentAccount UUID accountId,
                                       @RequestHeader("X-Idempotency-Key") UUID idempotencyKey,
                                       @Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(service.create(accountId, idempotencyKey, req));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancel(@CurrentAccount UUID accountId, @PathVariable("orderId") UUID orderId) {
        service.cancel(accountId, orderId);
        return ResponseEntity.noContent().build();
    }

    /** 1.0: kaldirilacak surum; Deprecation/Sunset/Link header'larini ApiVersioningConfig ekler. */
    @GetMapping(path = "/{orderId}/summary", version = "1.0")
    public OrderSummaryV1 summaryV1(@CurrentAccount UUID accountId, @PathVariable("orderId") UUID orderId) {
        OrderSummary s = service.summary(accountId, orderId);
        return new OrderSummaryV1(s.id(), s.status());
    }

    /** 1.1: kirici degisiklik (alan ekleme degil, sema degisimi) -> ayni path, yeni surum. */
    @GetMapping(path = "/{orderId}/summary", version = "1.1")
    public OrderSummaryV1_1 summaryV1_1(@CurrentAccount UUID accountId, @PathVariable("orderId") UUID orderId) {
        OrderSummary s = service.summary(accountId, orderId);
        return new OrderSummaryV1_1(s.id(), new OrderSummaryV1_1.Line(s.sku(), s.quantity()), s.status());
    }

    // Yanit DTO'lari gercek projede order-api/dto'da yasar; iskelette api modulu degistirilmedigi icin buradadir.
    public record OrderSummaryV1(UUID id, String status) {}
    public record OrderSummaryV1_1(UUID id, Line line, String status) { public record Line(String sku, int quantity) {} }
}
