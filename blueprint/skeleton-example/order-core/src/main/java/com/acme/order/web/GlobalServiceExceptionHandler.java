package com.acme.order.web;

import com.acme.order.exception.CommonErrorCode;
import com.acme.order.exception.OrderServiceException;
import com.acme.order.logging.SensitiveLogSanitizer;
import com.acme.platform.core.ErrorCode;
import com.acme.platform.core.ServiceException;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global handler (referans Bolum 7.3). ResponseEntityExceptionHandler'dan turer: standart MVC hatalarinin tamami
 * (400/404/405/406/413/415, API version 400) taban sinifta tek noktadan handleExceptionInternal'a duser; burada
 * kod eslemesi ve tek zarf uretilir. Boylece yeni bir MVC hata tipi 500'e dusmez.
 *
 * Gizlilik: yanita ve log'a reddedilen deger, exception metni ve body ASLA yazilmaz; 500'de yalniz
 * exceptionType + sanitize edilmis ozet loglanir (ham throwable log olayina eklenmez, cunku mesaji PII tasiyabilir).
 */
@RestControllerAdvice
public class GlobalServiceExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final Logger log = LoggerFactory.getLogger(GlobalServiceExceptionHandler.class);

    private final Clock clock;

    public GlobalServiceExceptionHandler(Clock clock) { this.clock = clock; }

    /** Is hatalari: status/kod exception'dan; safeLogReason yalniz log'a, details yalniz yanita. */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Object> handleServiceException(ServiceException ex, WebRequest request) {
        ErrorCode code = ex.getErrorCode();
        List<String> details = ex instanceof OrderServiceException o ? o.getDetails() : List.of();
        String reason = ex instanceof OrderServiceException o ? o.getSafeLogReason() : null;
        String category = ex instanceof OrderServiceException o ? o.getSafeLogCategory() : null;
        String traceId = currentTraceId();
        HttpStatus status = code.getHttpStatus();
        if (status.is5xxServerError()) {
            log.error("Request failed: code={} status={} reason={} category={} exceptionType={} traceId={}",
                    code, status.value(), reason, category, ex.getClass().getSimpleName(), traceId);
        } else {
            log.warn("Request rejected: code={} status={} reason={} category={} traceId={}",
                    code, status.value(), reason, category, traceId);
        }
        return envelope(status, code, details, traceId, request);
    }

    /** @Validated metot parametreleri (ConstraintViolation) taban sinifta yoktur; acikca eslenir. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + "=" + v.getMessage()) // alan adi + kural mesaji; deger yok
                .sorted().toList();
        String traceId = currentTraceId();
        log.warn("Request rejected: code={} status=400 exceptionType={} traceId={}",
                CommonErrorCode.VALIDATION, ex.getClass().getSimpleName(), traceId);
        return envelope(HttpStatus.BAD_REQUEST, CommonErrorCode.VALIDATION, details, traceId, request);
    }

    /** Beklenmeyen her sey: 500 + genel mesaj; log'a exceptionType ve sanitize ozet (Bolum 7.3 son satir). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        String traceId = currentTraceId();
        log.error("Request failed: code={} status=500 exceptionType={} summary={} traceId={}",
                CommonErrorCode.INTERNAL_ERROR, ex.getClass().getSimpleName(),
                SensitiveLogSanitizer.safeExceptionSummary(ex), traceId);
        return envelope(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.INTERNAL_ERROR, List.of(), traceId, request);
    }

    /**
     * ResponseEntityExceptionHandler'in TUM handleXxx metotlari buraya gelir (status taban sinifin karari).
     * Eslenmeyen yeni bir tip 4xx ise VALIDATION, 5xx ise INTERNAL_ERROR olur; hicbiri ham metin tasimaz.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        String traceId = currentTraceId();
        if (status.is5xxServerError()) {
            log.error("Request failed: code={} status={} exceptionType={} summary={} traceId={}",
                    CommonErrorCode.INTERNAL_ERROR, status.value(), ex.getClass().getSimpleName(),
                    SensitiveLogSanitizer.safeExceptionSummary(ex), traceId);
            return envelope(status, CommonErrorCode.INTERNAL_ERROR, List.of(), traceId, request);
        }
        Mapping m = map(ex);
        log.warn("Request rejected: code={} status={} exceptionType={} traceId={}",
                m.code(), status.value(), ex.getClass().getSimpleName(), traceId);
        return envelope(status, m.code(), m.details(), traceId, request);
    }

    private record Mapping(CommonErrorCode code, List<String> details) {}

    private static Mapping map(Exception ex) {
        return switch (ex) {
            case MethodArgumentNotValidException e -> new Mapping(CommonErrorCode.VALIDATION, fieldDetails(e));
            case HandlerMethodValidationException e -> new Mapping(CommonErrorCode.VALIDATION, parameterDetails(e));
            case HttpMessageNotReadableException e -> new Mapping(CommonErrorCode.REQUEST_NOT_READABLE, List.of());
            case MethodArgumentTypeMismatchException e ->
                    new Mapping(CommonErrorCode.TYPE_MISMATCH, List.of("parameter=" + e.getName()));
            case TypeMismatchException e -> new Mapping(CommonErrorCode.TYPE_MISMATCH, List.of());
            case MissingServletRequestParameterException e ->
                    new Mapping(CommonErrorCode.MISSING_PARAMETER, List.of("parameter=" + e.getParameterName()));
            case MissingRequestHeaderException e ->
                    new Mapping(CommonErrorCode.MISSING_PARAMETER, List.of("header=" + e.getHeaderName()));
            case ServletRequestBindingException e -> new Mapping(CommonErrorCode.MISSING_PARAMETER, List.of());
            case NoResourceFoundException e -> new Mapping(CommonErrorCode.NOT_FOUND, List.of());
            case NoHandlerFoundException e -> new Mapping(CommonErrorCode.NOT_FOUND, List.of());
            case HttpRequestMethodNotSupportedException e -> new Mapping(CommonErrorCode.METHOD_NOT_ALLOWED,
                    e.getSupportedMethods() == null ? List.of() : List.of("allowed=" + String.join(",", e.getSupportedMethods())));
            case HttpMediaTypeNotSupportedException e -> new Mapping(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE,
                    List.of("supported=" + MediaType.toString(e.getSupportedMediaTypes())));
            case HttpMediaTypeNotAcceptableException e -> new Mapping(CommonErrorCode.NOT_ACCEPTABLE, List.of());
            case MaxUploadSizeExceededException e -> new Mapping(CommonErrorCode.PAYLOAD_TOO_LARGE, List.of());
            case MissingApiVersionException e -> new Mapping(CommonErrorCode.API_VERSION_INVALID, List.of());
            case InvalidApiVersionException e -> new Mapping(CommonErrorCode.API_VERSION_INVALID, List.of());
            default -> new Mapping(CommonErrorCode.VALIDATION, List.of());
        };
    }

    /** Alan adi + kural mesaji (orn. sku=must not be blank). Reddedilen deger bilincli olarak alinmaz. */
    private static List<String> fieldDetails(MethodArgumentNotValidException e) {
        List<String> details = new ArrayList<>();
        e.getBindingResult().getFieldErrors().forEach(f -> details.add(f.getField() + "=" + f.getDefaultMessage()));
        e.getBindingResult().getGlobalErrors().forEach(g -> details.add(g.getObjectName() + "=" + g.getDefaultMessage()));
        return details;
    }

    private static List<String> parameterDetails(HandlerMethodValidationException e) {
        List<String> details = new ArrayList<>();
        e.getParameterValidationResults().forEach(r -> {
            String name = r.getMethodParameter().getParameterName();
            String message = r.getResolvableErrors().isEmpty() ? "invalid" : r.getResolvableErrors().get(0).getDefaultMessage();
            details.add((name != null ? name : "arg" + r.getMethodParameter().getParameterIndex()) + "=" + message);
        });
        return details;
    }

    private ResponseEntity<Object> envelope(HttpStatusCode status, ErrorCode code, List<String> details,
                                            String traceId, WebRequest request) {
        String path = request instanceof ServletWebRequest s ? s.getRequest().getRequestURI() : null;
        ErrorResponse error = new ErrorResponse(code.getCode(), code.getMessage(), code.getService(), path,
                clock.millis(), traceId, details);
        return ResponseEntity.status(status)
                .header(TRACE_ID_HEADER, traceId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(error));
    }

    /** traceId aktif span'den (Micrometer Tracing MDC'ye koyar); tracing yoksa yanit ve log ayni uretilmis id'yi tasir. */
    static String currentTraceId() {
        String fromSpan = MDC.get("traceId");
        return fromSpan != null && !fromSpan.isBlank() ? fromSpan : UUID.randomUUID().toString().replace("-", "");
    }
}
