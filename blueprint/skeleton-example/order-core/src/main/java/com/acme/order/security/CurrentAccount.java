package com.acme.order.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Dogrulanmis hesap kimligi: servis JWT'sinin sub claim'inden gelen request attribute'undan cozumlenir (referans Bolum 6.5). */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentAccount {}
