package com.acme.modulith.inventory;

import java.util.OptionalInt;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acme.modulith.inventory.internal.StockRepository;

/** Inventory modulunun disa acik yuzu: stok tanimlama ve okuma. Rezervasyon yalniz olayla olur. */
@Service
public class InventoryService {

    private final StockRepository stock;

    public InventoryService(StockRepository stock) {
        this.stock = stock;
    }

    @Transactional
    public void provision(String sku, int available) {
        stock.upsertStock(sku, available);
    }

    public OptionalInt availableStock(String sku) {
        return stock.available(sku);
    }

    public int reservationCount(UUID orderId) {
        return stock.reservationCount(orderId);
    }
}
