package com.acme.order.api.event;
/** notification servisine "SMS gonder" talimati (order servisi domain.events uzerinden yayinlar). */
public record NotificationSendCommand(String targetService, String channel, String phone, String text) {}
