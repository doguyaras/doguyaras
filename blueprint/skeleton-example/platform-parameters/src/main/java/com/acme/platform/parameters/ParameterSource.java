package com.acme.platform.parameters;

import java.time.Instant;

/**
 * Uzak parametre kaynagi (yonetim servisinin internal uclari). Servis basina tek implementasyon
 * (BackofficeParameterClient). Erisilemezlik, timeout, 5xx gibi tum tasima hatalari ParameterSourceException olarak
 * gelir; fallback karari bu arayuzde degil SystemParameterProvider'da verilir.
 */
public interface ParameterSource {
    /** GET /internal/parameters/groups/{group} */
    ParameterGroupDto fetch(String group);
    /** GET /internal/parameters/groups/{group}/since/{revision}: en az bu revizyon beklenir. */
    ParameterGroupDto fetchSince(String group, long revision);
    /** GET /internal/parameters/groups/{group}/at?at=<instant>: gecmis bir anin degeri. */
    ParameterGroupDto fetchAt(String group, Instant at);
}
