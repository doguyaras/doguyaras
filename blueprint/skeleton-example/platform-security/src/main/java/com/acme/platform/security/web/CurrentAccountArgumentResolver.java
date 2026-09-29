package com.acme.platform.security.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** @CurrentAccount UUID parametresini x.accountId attribute'undan cozer (Bolum 23.5 binding testi bunu dogrular). */
public class CurrentAccountArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAccount.class) && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav, NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        UUID accountId = request == null ? null : ServiceRequestAttributes.accountId(request).orElse(null);
        CurrentAccount ann = parameter.getParameterAnnotation(CurrentAccount.class);
        if (accountId == null && (ann == null || ann.required())) {
            throw new ServiceSecurityException(HttpStatus.UNAUTHORIZED, ErrorResponse.ACCOUNT_CONTEXT_REQUIRED,
                    "Account context required");
        }
        return accountId;
    }
}
