package com.acme.platform.messaging.readmodel;

/** Bir read-model olayinin sonucu: uygulandi ya da (eski/esit revizyon, tekrar teslim) yok sayildi. Bosluk sonuc degil, istisnadir. */
public enum Outcome { APPLIED, IGNORED }
