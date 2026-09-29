package com.acme.order.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kimlik baglami: dogrulanmis service JWT'nin sub claim'i (request attribute x.accountId) -> UUID.
 * Hesap kimligi ASLA path/query/body'den alinmaz (IDOR; referans Bolum 6.5).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentAccount {}
