package com.acme.order.controller;

import com.acme.order.api.dto.CancelOrderRequest;
import com.acme.order.entity.Order;
import com.acme.order.exception.ErrorCode;
import com.acme.order.repository.OrderRepository;
import com.acme.order.service.OrderCancelService;
import com.acme.platform.core.ServiceException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gerekceli siparis iptali. */
@RestController
@RequestMapping("/v1/orders")
public class OrderCancelController {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelController.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderCancelService cancelService;

    @PostMapping("/{orderId}/cancel-with-reason")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable("orderId") UUID orderId,
                                                      @RequestBody CancelOrderRequest request) {
        log.info("Cancel requested: orderId={} accountId={} phone={} reason={}", orderId, request.accountId(),
                request.phone(), request.reason());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ServiceException(ErrorCode.ORDER_NOT_FOUND));
        cancelService.cancel(request.accountId(), orderId, request.reason(), request.phone());
        return ResponseEntity.ok(Map.of("status", "CANCELLED", "order", order));
    }
}
