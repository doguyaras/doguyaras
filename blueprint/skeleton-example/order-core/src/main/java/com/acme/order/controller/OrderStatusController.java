package com.acme.order.controller;

import com.acme.order.api.dto.OrderStatusResponse;
import com.acme.order.security.CurrentAccount;
import com.acme.order.service.OrderStatusService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kullanicinin kendi siparisinin durumu. Hesap kimligi yalniz dogrulanmis baglamdan gelir; sahiplik serviste kontrol edilir. */
@RestController
@RequestMapping("/v1/orders")
public class OrderStatusController {

    private final OrderStatusService statusService;

    public OrderStatusController(OrderStatusService statusService) { this.statusService = statusService; }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> status(@CurrentAccount UUID accountId, @PathVariable("orderId") UUID orderId) {
        OrderStatusResponse body = statusService.status(accountId, orderId);
        return ResponseEntity.ok(Map.of("data", body));
    }
}
