package com.acme.order.controller;
import com.acme.order.api.dto.CreateOrderRequest;
import com.acme.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/v1/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<UUID> create(@RequestHeader("X-Idempotency-Key") UUID key, @Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(service.create(UUID.randomUUID(), key, req));
    }
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable("orderId") UUID orderId) { service.cancel(UUID.randomUUID(), orderId); return ResponseEntity.noContent().build(); }
}
