package com.acme.modulith.order;

import java.time.Instant;
import java.util.UUID;

/**
 * Order modulunun disa acik olayi (modulun kok paketinde = API). Diger moduller yalniz bunu gorur,
 * {@code order.internal} icindekileri degil. Registry bu kaydi JSON olarak event_publication'a yazar;
 * yeniden gonderimde JSON'dan geri okunur, bu yuzden yalniz basit, kararli alanlar tasir (PII yok).
 */
public record OrderPlaced(UUID orderId, String sku, int quantity, Instant placedAt) {
}
