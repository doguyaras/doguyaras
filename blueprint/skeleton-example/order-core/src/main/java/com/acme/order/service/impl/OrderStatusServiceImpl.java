package com.acme.order.service.impl;

import com.acme.order.api.dto.OrderStatusResponse;
import com.acme.order.exception.ErrorCode;
import com.acme.order.service.OrderStatusService;
import com.acme.platform.core.ServiceException;
import com.acme.platform.messaging.outbox.OutboxEvent;
import com.acme.platform.messaging.outbox.OutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siparis durumu: sahiplik kontrolu sorguda (account_id = ?), bulunamayan ve baskasinin siparisi ayni sonucu verir
 * (varlik oracle'i yok). Durum degisikligi ile outbox satiri ayni transaction'da yazilir (referans Bolum 11.2).
 */
@Service
public class OrderStatusServiceImpl implements OrderStatusService {

    private final JdbcTemplate jdbc;
    private final OutboxRepository outbox;
    private final Clock clock;

    public OrderStatusServiceImpl(JdbcTemplate jdbc, OutboxRepository outbox, Clock clock) {
        this.jdbc = jdbc; this.outbox = outbox; this.clock = clock;
    }

    @Override
    public OrderStatusResponse status(UUID accountId, UUID orderId) {
        List<OrderStatusResponse> rows = jdbc.query(
                "SELECT id, status, created_at FROM \"order\".order_item WHERE id = ? AND account_id = ?",
                (rs, i) -> new OrderStatusResponse(rs.getObject("id", UUID.class), rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()),
                orderId, accountId);
        if (rows.isEmpty()) throw new ServiceException(ErrorCode.ORDER_NOT_FOUND);
        return rows.get(0);
    }

    @Override
    @Transactional
    public void markShipped(UUID accountId, UUID orderId) {
        int n = jdbc.update("UPDATE \"order\".order_item SET status = 'SHIPPED' WHERE id = ? AND account_id = ? AND status = 'CREATED'",
                orderId, accountId);
        if (n == 0) throw new ServiceException(ErrorCode.ORDER_NOT_CANCELLABLE);
        Instant now = clock.instant();
        outbox.append(new OutboxEvent(UUID.randomUUID(), "EVENT", "order", orderId, "order.order.shipped",
                "{\"orderId\":\"" + orderId + "\",\"revision\":1}", "{}", "PENDING", 0,
                OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT, 0, now, null, null, null, now));
    }
}
