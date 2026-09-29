/**
 * Inventory modulu. Yalniz order'in API'sine (kok paketi: OrderPlaced) bagimli olabilir;
 * order.internal'a erisim ve baska modullere bag verify() ile yakalanir.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Inventory", allowedDependencies = "order")
package com.acme.modulith.inventory;
