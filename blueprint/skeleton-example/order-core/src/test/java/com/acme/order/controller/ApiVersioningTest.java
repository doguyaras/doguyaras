package com.acme.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.order.config.ApiVersioningConfig;
import com.acme.order.config.ClockConfig;
import com.acme.order.exception.CommonErrorCode;
import com.acme.order.service.OrderService;
import com.acme.order.service.OrderSummary;
import com.acme.order.web.CurrentAccountArgumentResolver;
import com.acme.order.web.GlobalServiceExceptionHandler;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Spring Framework 7 API versiyonlama (referans Bolum 20): ayni path'te @GetMapping(version="1.0") ve "1.1";
 * cozumleme Boot property'leriyle (spring.mvc.apiversion.use.header=API-Version, required, supported - application.yml).
 * Kaldirilacak 1.0 yaniti Deprecation (RFC 9745, @epoch), Sunset (RFC 8594, HTTP-date) ve Link rel="deprecation"
 * tasir; 1.1 tasimaz. Eksik/desteklenmeyen surum 400 API_VERSION_INVALID zarfidir.
 */
@WebMvcTest(controllers = OrderController.class)
@Import({ClockConfig.class, ApiVersioningConfig.class})
class ApiVersioningTest {

    // application.yml api.deprecations[0] ile birebir
    static final Instant DEPRECATED_AT = Instant.parse("2026-09-01T00:00:00Z");
    static final Instant SUNSET_AT = Instant.parse("2027-03-01T00:00:00Z");
    static final String LINK = "https://docs.acme.example/order/api/deprecations#v1-0";

    @Autowired MockMvc mvc;
    @MockitoBean OrderService service;

    final UUID accountId = UUID.fromString("aaaaaaaa-0000-4000-8000-00000000a001");
    final UUID orderId = UUID.fromString("cccccccc-0000-4000-8000-00000000c003");

    @BeforeEach
    void stubService() {
        when(service.summary(accountId, orderId)).thenReturn(new OrderSummary(orderId, "SKU-42", 3, "CREATED"));
    }

    @Test
    void headerVersion11_selectsNewHandler_withoutDeprecationHeaders() throws Exception {
        mvc.perform(summary().header("API-Version", "1.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.line.sku").value("SKU-42"))
                .andExpect(jsonPath("$.line.quantity").value(3))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"));
    }

    @Test
    void headerVersion10_selectsDeprecatedHandler_withDeprecationSunsetAndLinkHeaders() throws Exception {
        MvcResult r = mvc.perform(summary().header("API-Version", "1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.line").doesNotExist())
                // RFC 9745: Deprecation = structured-field date (@epoch-seconds)
                .andExpect(header().string("Deprecation", "@" + DEPRECATED_AT.getEpochSecond()))
                // RFC 8594: Sunset = HTTP-date (IMF-fixdate)
                .andExpect(header().string("Sunset", DateTimeFormatter.RFC_1123_DATE_TIME.format(SUNSET_AT.atZone(ZoneOffset.UTC))))
                .andReturn();
        assertThat(r.getResponse().getHeaders("Link"))
                .anyMatch(l -> l.contains("<" + LINK + ">") && l.contains("rel=\"deprecation\""));
    }

    @Test
    void patchLevelRequest_isNotImplicitlySupported_returns400() throws Exception {
        // supported listesi (application.yml) birebir esler: 1.0.7, 1.0 ile ayni degildir -> desteklenmeyen surum.
        // Patch surumleri kabul edilecekse listeye eklenir ya da detect-supported ile mapping'lerden toplanir.
        mvc.perform(summary().header("API-Version", "1.0.7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.API_VERSION_INVALID.getCode()));
    }

    @Test
    void missingVersion_returns400ApiVersionInvalidEnvelope() throws Exception {
        mvc.perform(summary())
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(GlobalServiceExceptionHandler.TRACE_ID_HEADER))
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.API_VERSION_INVALID.getCode()));
    }

    @Test
    void unsupportedVersion_returns400ApiVersionInvalidEnvelope() throws Exception {
        mvc.perform(summary().header("API-Version", "9.9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.API_VERSION_INVALID.getCode()));
    }

    @Test
    void unparsableVersion_returns400NotFiveHundred() throws Exception {
        mvc.perform(summary().header("API-Version", "latest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(CommonErrorCode.API_VERSION_INVALID.getCode()));
    }

    MockHttpServletRequestBuilder summary() {
        return get("/v1/orders/{id}/summary", orderId)
                .requestAttr(CurrentAccountArgumentResolver.ACCOUNT_ID_ATTRIBUTE, accountId.toString());
    }
}
