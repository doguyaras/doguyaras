package com.acme.platform.parameters;

/**
 * Grubun bounded-staleness sinifi (referans Bolum 14): katalogda tanimlanir, tuketici kodunda degil.
 * SECURITY_CRITICAL: maxStaleness asilinca 503 (PARAMETER_UNAVAILABLE). NORMAL: son bilinen degerle devam.
 */
public enum Criticality { SECURITY_CRITICAL, NORMAL }
