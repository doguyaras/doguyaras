package com.acme.platform.security.delegation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Internal controller metodunu delegasyon matrisindeki bir isleme baglar (orn. "subscription.consume").
 * DelegationInterceptor cagirani, sub'i ve {accountId} path degiskenini bu isleme gore denetler.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireOperation {
    String value();
    /** sub ile karsilastirilacak path degiskeninin adi. */
    String accountPathVariable() default "accountId";
}
