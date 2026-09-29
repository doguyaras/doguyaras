package com.acme.platform.ratelimit;

/**
 * Redis'e ulasilamadiginda scope basina karar (referans Bolum 9.6 fail tablosu).
 * OPEN: is yuzeyleri (arama, listeleme) - istek gecer, metrik + alarm. CLOSED: guvenlik yuzeyleri (OTP, login,
 * refresh) - 503, cunku limitsiz gecis kaba kuvvet/SMS pumping kapisini acar. Varsayilan yoktur; her scope
 * karari acikca yazar (README'deki tek tablo).
 */
public enum FailPolicy { OPEN, CLOSED }
