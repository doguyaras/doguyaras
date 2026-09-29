package com.acme.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.acme.order.config.ApiVersioningConfig;
import com.acme.order.controller.OrderController;
import com.acme.order.exception.CommonErrorCode;
import com.acme.order.exception.ErrorCode;
import com.acme.order.exception.OrderServiceException;
import com.acme.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global handler eslemesi (referans Bolum 7.3) ve gizlilik (Bolum 8.4): her hata tek zarf + X-Trace-Id;
 * reddedilen deger / exception metni ne yanitta ne log'da. Log dogrulamasi ROOT logger'a takilan ListAppender
 * ile yapilir: framework'un kendi WARN satirlari da (orn. resolved-exception) denetime girer.
 *
 * Kanit seviyesi 1 (MockMvc, @WebMvcTest dilimi). application.yml'deki API-Version zorunlulugu gecerlidir;
 * bu yuzden her istek header'i tasir (eksik header senaryosu ApiVersioningTest'tedir).
 */
@WebMvcTest(controllers = OrderController.class)
@Import({ApiVersioningConfig.class, GlobalServiceExceptionHandlerTest.FixedClock.class,
        GlobalServiceExceptionHandlerTest.ValidationProbeController.class})
class GlobalServiceExceptionHandlerTest {

    static final Instant NOW = Instant.parse("2026-09-29T10:00:00Z");
    static final String ACCOUNT_ID = "5f1c2a3b-7d8e-4f90-a1b2-c3d4e5f60718";
    static final String PHONE_MARKER = "+905551234567";
    static final String TOKEN_MARKER = "SECRET-TOKEN-MARKER-9f8e7d6c";
    static final String VALUE_MARKER = "rejected-value-marker-31337";

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {
        @Bean Clock clock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }

    /** @Valid + ayirt edici reddedilen deger icin sonda: order-api DTO'su (sku/quantity) bu senaryoyu uretemez. */
    @RestController
    @RequestMapping("/v1/probe")
    static class ValidationProbeController {
        record ProbeBody(@Pattern(regexp = "[A-Z0-9-]{3,20}") String code) {}
        @PostMapping
        ResponseEntity<Void> post(@Valid @RequestBody ProbeBody body) { return ResponseEntity.noContent().build(); }
    }

    @Autowired MockMvc mvc;
    @MockitoBean OrderService service;

    Logger root;
    ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
    }

    @AfterEach
    void detachAppender() { root.detachAppender(appender); appender.stop(); }

    // ---------- senaryolar ----------

    @Test
    void validationFailure_returns400ValidationWithoutRejectedValue() throws Exception {
        MvcResult r = mvc.perform(versioned(post("/v1/probe")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + VALUE_MARKER + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.VALIDATION.getCode()))
                .andExpect(jsonPath("$.error.service").value("validation"))
                .andExpect(jsonPath("$.error.details[0]").value(org.hamcrest.Matchers.startsWith("code=")))
                .andReturn();
        assertEnvelope(r);
        assertThat(r.getResponse().getContentAsString()).doesNotContain(VALUE_MARKER);
        assertThat(renderedLogs()).anyMatch(m -> m.contains("code=VALIDATION") && m.contains("status=400"))
                .noneMatch(m -> m.contains(VALUE_MARKER));
        assertNoMarkerAnywhereInLogs(VALUE_MARKER);
    }

    @Test
    void malformedJson_returns400NotReadableWithoutPayloadText() throws Exception {
        MvcResult r = mvc.perform(versioned(post("/v1/orders")).header("X-Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1\",\"quantity\":\"" + VALUE_MARKER + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.REQUEST_NOT_READABLE.getCode()))
                .andReturn();
        assertEnvelope(r);
        assertThat(r.getResponse().getContentAsString()).doesNotContain(VALUE_MARKER).doesNotContain("JsonParseException")
                .doesNotContain("Cannot");
        assertNoMarkerAnywhereInLogs(VALUE_MARKER);
    }

    @Test
    void unknownPath_returns404NotFoundInsteadOf500() throws Exception {
        MvcResult r = mvc.perform(versioned(get("/v1/nothing-here")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.NOT_FOUND.getCode()))
                .andReturn();
        assertEnvelope(r);
    }

    @Test
    void wrongMethod_returns405WithAllowedMethods() throws Exception {
        MvcResult r = mvc.perform(versioned(delete("/v1/orders")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.METHOD_NOT_ALLOWED.getCode()))
                .andExpect(jsonPath("$.error.details[0]").value("allowed=POST"))
                .andReturn();
        assertEnvelope(r);
    }

    @Test
    void unsupportedMediaType_returns415() throws Exception {
        MvcResult r = mvc.perform(versioned(post("/v1/orders")).header("X-Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.TEXT_PLAIN).content("sku=SKU-1"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode()))
                .andReturn();
        assertEnvelope(r);
    }

    @Test
    void serviceException_returnsItsOwnStatusAndCode_andLogsSafeReasonOnly() throws Exception {
        UUID orderId = UUID.randomUUID();
        doThrow(new OrderServiceException(ErrorCode.ORDER_NOT_CANCELLABLE, "STATUS_SHIPPED", "BUSINESS_RULE",
                List.of("retryAfterSeconds=120")))
                .when(service).cancel(UUID.fromString(ACCOUNT_ID), orderId);

        MvcResult r = mvc.perform(versioned(post("/v1/orders/{id}/cancel", orderId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(11002))
                .andExpect(jsonPath("$.error.service").value("order"))
                .andExpect(jsonPath("$.error.message").value("Order cannot be cancelled."))
                .andExpect(jsonPath("$.error.details[0]").value("retryAfterSeconds=120"))
                .andReturn();
        assertEnvelope(r);
        assertThat(r.getResponse().getContentAsString()).doesNotContain("STATUS_SHIPPED"); // safeLogReason yanita cikmaz
        assertThat(renderedLogs()).anyMatch(m -> m.contains("code=ORDER_NOT_CANCELLABLE") && m.contains("reason=STATUS_SHIPPED")
                && m.contains("category=BUSINESS_RULE") && m.contains("status=409"));
    }

    @Test
    void unexpectedRuntimeException_returns500GenericMessage_andLogsSanitizedSummaryWithoutThrowable() throws Exception {
        when(service.create(any(), any(), any()))
                .thenThrow(new IllegalStateException("boom token=" + TOKEN_MARKER + " phone=" + PHONE_MARKER));

        MvcResult r = mvc.perform(versioned(post("/v1/orders")).header("X-Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"SKU-1\",\"quantity\":1}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.INTERNAL_ERROR.getCode()))
                .andExpect(jsonPath("$.error.service").value("system"))
                .andExpect(jsonPath("$.error.message").value("Unexpected error."))
                .andExpect(jsonPath("$.error.details").isEmpty())
                .andReturn();
        assertEnvelope(r);
        String body = r.getResponse().getContentAsString();
        assertThat(body).doesNotContain("boom").doesNotContain("IllegalStateException").doesNotContain(TOKEN_MARKER);

        assertThat(renderedLogs()).anyMatch(m -> m.contains("code=INTERNAL_ERROR") && m.contains("exceptionType=IllegalStateException")
                && m.contains("token=[REDACTED]") && m.contains("+90********67"));
        assertNoMarkerAnywhereInLogs(TOKEN_MARKER);
        assertNoMarkerAnywhereInLogs(PHONE_MARKER);
        // Ham throwable log olayina eklenmez: stack trace exception mesajini (PII) tasirdi.
        assertThat(appender.list).filteredOn(e -> e.getFormattedMessage().contains("code=INTERNAL_ERROR"))
                .allMatch(e -> e.getThrowableProxy() == null);
    }

    @Test
    void headerTypeMismatch_returns400WithoutRejectedValue() throws Exception {
        MvcResult r = mvc.perform(versioned(post("/v1/orders")).header("X-Idempotency-Key", VALUE_MARKER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"SKU-1\",\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.TYPE_MISMATCH.getCode()))
                .andReturn();
        assertEnvelope(r);
        assertThat(r.getResponse().getContentAsString()).doesNotContain(VALUE_MARKER);
        assertNoMarkerAnywhereInLogs(VALUE_MARKER);
    }

    @Test
    void missingRequiredHeader_returns400NamingTheHeaderOnly() throws Exception {
        MvcResult r = mvc.perform(versioned(post("/v1/orders"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"SKU-1\",\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.MISSING_PARAMETER.getCode()))
                .andExpect(jsonPath("$.error.details[0]").value("header=X-Idempotency-Key"))
                .andReturn();
        assertEnvelope(r);
    }

    @Test
    void missingAccountContext_returns400BeforeReachingService() throws Exception {
        MvcResult r = mvc.perform(post("/v1/orders/{id}/cancel", UUID.randomUUID()).header("API-Version", "1.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.MISSING_PARAMETER.getCode()))
                .andReturn();
        assertEnvelope(r);
        org.mockito.Mockito.verifyNoInteractions(service);
    }

    // ---------- yardimcilar ----------

    static MockHttpServletRequestBuilder versioned(MockHttpServletRequestBuilder b) {
        return b.header("API-Version", "1.0").requestAttr(CurrentAccountArgumentResolver.ACCOUNT_ID_ATTRIBUTE, ACCOUNT_ID);
    }

    /** Her hata yanitinda: X-Trace-Id header'i, zarf alanlari, traceId = header, timestamp = Clock. */
    static void assertEnvelope(MvcResult r) throws Exception {
        String traceId = r.getResponse().getHeader(GlobalServiceExceptionHandler.TRACE_ID_HEADER);
        assertThat(traceId).as("X-Trace-Id header").isNotBlank();
        assertThat(r.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        String body = r.getResponse().getContentAsString();
        assertThat(body).contains("\"ok\":false").contains("\"traceId\":\"" + traceId + "\"")
                .contains("\"timestamp\":" + NOW.toEpochMilli()).contains("\"path\":\"" + r.getRequest().getRequestURI() + "\"");
        assertThat(body).doesNotContain(ACCOUNT_ID); // hesap id'si hata zarfina sizmaz
    }

    List<String> renderedLogs() { return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList(); }

    /** Bolum 8.5: rendered mesaj, argumanlar, MDC ve exception zincirinde isaret yok. */
    void assertNoMarkerAnywhereInLogs(String marker) {
        for (ILoggingEvent e : appender.list) {
            assertThat(e.getFormattedMessage()).as("formatted message").doesNotContain(marker);
            if (e.getArgumentArray() != null) {
                assertThat(Stream.of(e.getArgumentArray()).map(String::valueOf).toList()).noneMatch(a -> a.contains(marker));
            }
            assertThat(e.getMDCPropertyMap().values()).noneMatch(v -> v.contains(marker));
            for (var p = e.getThrowableProxy(); p != null; p = p.getCause()) {
                assertThat(String.valueOf(p.getMessage())).as("throwable message").doesNotContain(marker);
            }
        }
    }
}
