package com.acme.broker.consume;

/**
 * Handler'in is degisikligi: inbox TX'i ICINDE cagrilir (read-model UPSERT, domain yazimi, dis etki icin outbox satiri).
 * Exception atarsa inbox satiri da geri alinir ve mesaj yeniden gelir (Bolum 11.3). Dis sisteme dogrudan gitmez.
 */
@FunctionalInterface
public interface OrderCancelledEffect {
    void apply(IncomingEvent event);
}
