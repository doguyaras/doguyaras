package com.acme.contract.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sozlesme ornegi icin kucuk controller: kalicilik bu modulun konusu degil (bellek ici map). Onemli olan, OpenAPI'nin
 * bu imzalardan uretilmesi: metod adi operationId olur (getOrder / createOrder) ve istemci kodu bu adlarla uretilir;
 * metod adini degistirmek de istemci tarafinda API kirilmasidir.
 */
@RestController
@RequestMapping(path = "/v1/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderController {

    private final Map<UUID, OrderResponse> orders = new ConcurrentHashMap<>();
    private final Clock clock;

    public OrderController(Clock clock) { this.clock = clock; }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable("id") UUID id) {
        OrderResponse order = orders.get(id);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        // Kisisel veri donen uc: ara cache'lerde tutulmaz (referans 6.7)
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).body(order);
    }

    // @ResponseStatus yalniz dokumantasyon icindir (springdoc 201'i buradan okur); gercek status ResponseEntity'den
    // gelir. Olmazsa spec "200" der, runtime 201 doner: uretilen istemci yanlis kodu bekler.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        OrderResponse created = new OrderResponse(UUID.randomUUID(), req.customerId(), req.sku(), req.quantity(),
                OrderStatus.PENDING, clock.instant(), req.note());
        orders.put(created.id(), created);
        return ResponseEntity.created(URI.create("/v1/orders/" + created.id()))
                .cacheControl(CacheControl.noStore().cachePrivate()).body(created);
    }
}
