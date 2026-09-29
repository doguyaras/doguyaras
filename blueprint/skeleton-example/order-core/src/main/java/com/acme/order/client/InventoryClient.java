package com.acme.order.client;

import java.util.UUID;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Envanter servisi client'i. */
@Component
public class InventoryClient {

    private final RestClient client = RestClient.create("http://inventory:8086");

    @Retryable(maxRetries = 3)
    public void release(UUID orderId) {
        client.post().uri("/internal/inventory/release/{id}", orderId).retrieve().toBodilessEntity();
    }

    public int stock(UUID orderId) {
        Integer body = client.get().uri("/internal/inventory/stock/{id}", orderId).retrieve().body(Integer.class);
        return body == null ? 0 : body;
    }
}
