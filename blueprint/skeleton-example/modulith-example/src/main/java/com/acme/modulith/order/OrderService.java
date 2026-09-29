package com.acme.modulith.order;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acme.modulith.order.internal.OrderRepository;

/**
 * Siparis yazar ve {@link OrderPlaced} yayinlar. Inventory'yi DOGRUDAN cagirmaz: moduller arasi bag
 * yalniz olaydir, boylece inventory ileride ayri servise cikarilirsa order degismez (Bolum 1.1 Sekil C).
 */
@Service
public class OrderService {

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderService(OrderRepository orders, ApplicationEventPublisher events, Clock clock) {
        this.orders = orders;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Siparis satiri ve event_publication satiri AYNI transaction'da yazilir (in-process outbox):
     * ikisi birlikte commit olur ya da ikisi birlikte geri alinir. Transaction yoksa
     * {@code @ApplicationModuleListener} (bir @TransactionalEventListener) olayi hic almaz; bu yuzden
     * {@code @Transactional} burada sart, "tercih" degil.
     */
    @Transactional
    public UUID placeOrder(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        UUID orderId = UUID.randomUUID();
        Instant now = clock.instant();
        orders.insert(orderId, sku, quantity, now);
        events.publishEvent(new OrderPlaced(orderId, sku, quantity, now));
        return orderId;
    }

    public boolean exists(UUID orderId) {
        return orders.exists(orderId);
    }
}
