package com.acme.order.controller;

import com.acme.order.api.dto.ForceCancelRequest;
import com.acme.order.service.OrderCancelService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Backoffice ve destek ekibi icin zorla iptal (internal). */
@RestController
@RequestMapping("/internal/orders")
public class InternalOrderController {

    private final OrderCancelService cancelService;

    public InternalOrderController(OrderCancelService cancelService) { this.cancelService = cancelService; }

    @PostMapping("/{orderId}/force-cancel")
    public ResponseEntity<Void> forceCancel(@PathVariable("orderId") UUID orderId, @RequestBody ForceCancelRequest request) {
        cancelService.cancel(request.accountId(), orderId, request.reason(), null);
        return ResponseEntity.noContent().build();
    }
}
