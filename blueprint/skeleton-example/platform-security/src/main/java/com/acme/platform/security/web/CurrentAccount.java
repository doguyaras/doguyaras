package com.acme.platform.security.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller parametresine dogrulanmis hesap kimligini (x.accountId) baglar. Header'dan degil, filtrenin/gateway'in
 * yazdigi attribute'tan okunur. required=true iken attribute yoksa 401 ACCOUNT_CONTEXT_REQUIRED.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentAccount {
    boolean required() default true;
}
