package com.acme.modulith.order;

import java.util.OptionalInt;

import com.acme.modulith.inventory.internal.StockRepository;

/**
 * KASITLI SINIR IHLALI (yalniz test kaynaginda): order modulunun paketinde duran bir sinif, inventory'nin
 * ic sinifina (inventory.internal.StockRepository) dogrudan bagimli. Uretim kodunda olsaydi
 * ApplicationModules.of(ModulithApp.class).verify() kirmaliydi; ModuleBoundaryViolationTest bunu kanitlar.
 * Uygulama context'ine girmez (bean degil).
 */
class LeakyOrderStockPeek {

    private final StockRepository stock;

    LeakyOrderStockPeek(StockRepository stock) {
        this.stock = stock;
    }

    OptionalInt peek(String sku) {
        return stock.available(sku);
    }
}
