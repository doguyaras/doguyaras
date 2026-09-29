package com.acme.order.service;

import java.util.UUID;

/** Servis katmaninin okuma modeli; controller bunu surume gore DTO'ya esler (entity controller'a cikmaz). */
public record OrderSummary(UUID id, String sku, int quantity, String status) {}
