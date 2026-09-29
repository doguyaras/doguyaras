package com.acme.order.web;

import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @CurrentAccount UUID cozumleyicisi. Kaynak yalniz guvenlik filtresinin koydugu request attribute'udur;
 * attribute yoksa istek servise ULASMADAN 400 ile reddedilir (filtre yanlis sirada/eksik demektir).
 */
public class CurrentAccountArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String ACCOUNT_ID_ATTRIBUTE = "x.accountId";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAccount.class) && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav, NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) throws ServletRequestBindingException {
        Object value = request.getAttribute(ACCOUNT_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (value instanceof UUID uuid) return uuid;
        if (value instanceof String s && !s.isBlank()) {
            try { return UUID.fromString(s); } catch (IllegalArgumentException ignored) { /* asagida reddedilir */ }
        }
        // Mesaj sabittir: attribute degeri (ne olursa olsun) yanita veya log'a tasinmaz.
        throw new ServletRequestBindingException("Authenticated account context is missing");
    }
}
