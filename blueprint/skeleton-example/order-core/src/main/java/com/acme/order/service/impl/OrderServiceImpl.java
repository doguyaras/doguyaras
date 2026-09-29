package com.acme.order.service.impl;

import com.acme.order.api.dto.CreateOrderRequest;
import com.acme.order.entity.Order;
import com.acme.order.exception.ErrorCode;
import com.acme.order.exception.OrderServiceException;
import com.acme.order.logging.SensitiveLogSanitizer;
import com.acme.order.repository.OrderRepository;
import com.acme.order.service.OrderService;
import com.acme.order.service.OrderSummary;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Log sozlesmesi (referans Bolum 8.2/8.4): satirlar allowlist alanlarla kurulur (code=, reason=, outcome=,
 * operation=, orderId=). accountId kisisel veriyle eslenebilir oldugu icin log'a yazilmaz; istemciden gelen
 * serbest metin (sku) yalniz SensitiveLogSanitizer'dan gecerek yazilir.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    static final int MAX_QUANTITY = 1000;

    private final OrderRepository repo;

    public OrderServiceImpl(OrderRepository repo) { this.repo = repo; }

    @Override
    public UUID create(UUID accountId, UUID idempotencyKey, CreateOrderRequest req) {
        if (req.quantity() < 1 || req.quantity() > MAX_QUANTITY) {
            // sku ham yazilmaz: CR/LF ile sahte log satiri ve icine yapistirilmis telefon/e-posta/token olabilir.
            throw reject(ErrorCode.ORDER_QUANTITY_INVALID, "QUANTITY_OUT_OF_RANGE",
                    "sku=" + SensitiveLogSanitizer.sanitize(req.sku()));
        }
        UUID id = UUID.randomUUID(); // gercek projede UUIDv7 (Bolum 10.3)
        repo.save(new Order(id, req.sku(), req.quantity(), "CREATED"));
        log.info("Order created: operation=ORDER_CREATE outcome=SUCCESS orderId={}", id);
        return id;
    }

    @Override
    public void cancel(UUID accountId, UUID orderId) {
        Order order = repo.findById(orderId)
                .orElseThrow(() -> reject(ErrorCode.ORDER_NOT_FOUND, "NOT_FOUND", "orderId=" + orderId));
        if (!"CREATED".equals(order.getStatus())) {
            // status sabit bir durum degeridir (serbest metin degil); sebep olarak loglanabilir.
            throw reject(ErrorCode.ORDER_NOT_CANCELLABLE, "STATUS_" + order.getStatus(), "orderId=" + orderId);
        }
        repo.updateStatus(orderId, "CANCELLED");
        log.info("Order cancelled: operation=ORDER_CANCEL outcome=SUCCESS orderId={}", orderId);
    }

    @Override
    public OrderSummary summary(UUID accountId, UUID orderId) {
        Order o = repo.findById(orderId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND, "NOT_FOUND"));
        return new OrderSummary(o.getId(), o.getSku(), o.getQuantity(), o.getStatus());
    }

    /** Tek red satiri: "Order rejected: code=X reason=Y outcome=REJECTED <guvenli baglam>"; sonra tipli istisna. */
    private static OrderServiceException reject(ErrorCode code, String reason, String safeContext) {
        log.warn("Order rejected: code={} reason={} outcome=REJECTED {}", code, reason, safeContext);
        return new OrderServiceException(code, reason);
    }
}
