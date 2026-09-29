/**
 * Order modulu. Hicbir module bagimli olamaz: bos allowedDependencies "hepsine izin" degil,
 * "hicbirine izin yok" demektir (varsayilan OPEN_TOKEN'dir). Order -> inventory bagi eklenirse
 * verify() kirar; akis yonu olay ile tersine (inventory order'i dinler) kalir.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Order", allowedDependencies = {})
package com.acme.modulith.order;
