package com.acme.platform.parameters;

/**
 * Parametre okuma hatalarinin ortak tabani: sabit bir hata kodu ve HTTP semantigi tasir. Servisler bunu kendi
 * GlobalServiceExceptionHandler'inda ProblemDetail'e cevirir. Mesajlarda deger ve ham yanit bulunmaz (referans
 * Bolum 14: loglara deger yazilmaz); grup ve key adi deploy hatasini teshis icin gerekli oldugundan yer alir.
 */
public abstract class ParameterException extends RuntimeException {
    private final String code;
    private final int httpStatus;

    protected ParameterException(String code, int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code() { return code; }
    public int httpStatus() { return httpStatus; }
}
