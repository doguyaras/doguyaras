package com.acme.order.service.impl;

import com.acme.order.api.event.NotificationSendCommand;
import com.acme.order.api.event.OrderCancelledEvent;
import com.acme.order.client.InventoryClient;
import com.acme.order.exception.ErrorCode;
import com.acme.order.readmodel.AccountStandingReader;
import com.acme.order.service.OrderCancelService;
import com.acme.platform.core.ServiceException;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCancelServiceImpl implements OrderCancelService {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AccountStandingReader standingReader;

    @Override
    @Transactional
    public void cancel(UUID accountId, UUID orderId, String reason, String phone) {
        if (!standingReader.isActive(accountId)) {
            throw new ServiceException(ErrorCode.ACCOUNT_INACTIVE);
        }
        String status = jdbc.queryForObject("SELECT status FROM \"order\".order_item WHERE id = ?", String.class, orderId);
        if ("CANCELLED".equals(status)) {
            throw new ServiceException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        int stock = inventoryClient.stock(orderId);
        inventoryClient.release(orderId);
        jdbc.update("UPDATE \"order\".order_item SET status = 'CANCELLED', reason = ? WHERE id = ?", reason, orderId);
        try {
            rabbitTemplate.convertAndSend("domain.events", "order.order.cancelled",
                    new OrderCancelledEvent(orderId, accountId, phone, reason, Instant.now()));
            rabbitTemplate.convertAndSend("domain.events", "notification.send",
                    new NotificationSendCommand("notification", "SMS", phone, "Siparisiniz iptal edildi (stok: " + stock + ")"));
        } catch (Exception e) {
            log.error("publish failed: " + e.getMessage());
        }
        audit(accountId, orderId, reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void audit(UUID accountId, UUID orderId, String reason) {
        jdbc.update("INSERT INTO \"order\".order_cancel_log (order_id, account_id, reason) VALUES (?, ?, ?)", orderId, accountId, reason);
    }
}
