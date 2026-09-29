package com.acme.order.web;

/** Yanit zarfi: hatalar HER ZAMAN zarflidir; basari yanitlari bu iskelette ham doner (referans Bolum 6.2 karari). */
public record ApiResponse<T>(boolean ok, T data, ErrorResponse error) {
    public static ApiResponse<Void> error(ErrorResponse error) { return new ApiResponse<>(false, null, error); }
}
