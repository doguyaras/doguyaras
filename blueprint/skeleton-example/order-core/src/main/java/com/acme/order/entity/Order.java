package com.acme.order.entity;
import java.util.UUID;
/** Iskelet entity'si (gercek projede JPA @Entity + @Table(schema = "order")). Entity api paketine cikmaz. */
public class Order {
    private final UUID id; private final String sku; private final int quantity; private final String status;
    public Order(UUID id, String sku, int quantity, String status) { this.id = id; this.sku = sku; this.quantity = quantity; this.status = status; }
    public UUID getId() { return id; } public String getSku() { return sku; }
    public int getQuantity() { return quantity; } public String getStatus() { return status; }
}
