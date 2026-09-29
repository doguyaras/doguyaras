package com.acme.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.order.api.dto.CreateOrderRequest;
import com.acme.order.exception.ErrorCode;
import com.acme.order.exception.OrderServiceException;
import com.acme.order.service.OrderService;
import com.acme.order.web.CurrentAccountArgumentResolver;
import com.acme.order.web.GlobalServiceExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.accept.HeaderApiVersionResolver;
import org.springframework.web.accept.SemanticApiVersionParser;

/**
 * Controller binding sablonu (referans Bolum 23.5): binding HTTP katmani uzerinden dogrulanir; kimlik yalniz
 * dogrulanmis baglamdan (request attribute x.accountId) gelir, path/query/header/body'den asla.
 * Standalone kurulum: Spring context yok; yalniz controller + resolver + advice + surum stratejisi.
 */
class OrderControllerTest {

    private final OrderService orderService = mock(OrderService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new OrderController(orderService))
            .setCustomArgumentResolvers(new CurrentAccountArgumentResolver())
            .setControllerAdvice(new GlobalServiceExceptionHandler(Clock.fixed(Instant.parse("2026-09-29T10:00:00Z"), ZoneOffset.UTC)))
            // TUZAK: controller'da @GetMapping(version=...) varsa standalone kurulum strateji olmadan "Invalid mapping"
            // ile cokar; uretimle ayni cozumleme (API-Version header'i, zorunlu) burada elle verilir.
            .setApiVersionStrategy(new DefaultApiVersionStrategy(List.of(new HeaderApiVersionResolver("API-Version")),
                    new SemanticApiVersionParser(), true, null, true, null, null))
            .build();

    // Ayirt edici sentetik degerler: yanlis kaynaktan (path/query/body) alinan kimlik hemen goze carpar.
    private final UUID accountId = UUID.fromString("aaaaaaaa-0000-4000-8000-00000000a001");
    private final UUID spoofedAccountId = UUID.fromString("bbbbbbbb-0000-4000-8000-00000000b002");
    private final UUID orderId = UUID.fromString("cccccccc-0000-4000-8000-00000000c003");
    private final UUID idempotencyKey = UUID.fromString("dddddddd-0000-4000-8000-00000000d004");

    @Test
    void cancel_bindsPathTargetAndAuthenticatedAccount() throws Exception {
        mockMvc.perform(authenticated(post("/v1/orders/{orderId}/cancel", orderId)))
                .andExpect(status().isNoContent());
        verify(orderService).cancel(accountId, orderId);
    }

    @Test
    void cancel_whenPathIsNotUuid_rejectsBeforeReachingService() throws Exception {
        mockMvc.perform(authenticated(post("/v1/orders/{orderId}/cancel", "not-a-uuid")))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(GlobalServiceExceptionHandler.TRACE_ID_HEADER))
                // path alani istek URI'sini tasir (Bolum 6.2); reddedilen deger mesaj/details'e sizmaz, yalniz parametre adi
                .andExpect(jsonPath("$.error.message").value(Matchers.not(Matchers.containsString("not-a-uuid"))))
                .andExpect(jsonPath("$.error.details").value(Matchers.contains("parameter=orderId")));
        verifyNoInteractions(orderService);
    }

    @Test
    void cancel_whenAuthenticatedAccountIsMissing_doesNotReachService() throws Exception {
        mockMvc.perform(versioned(post("/v1/orders/{orderId}/cancel", orderId)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    @Test
    void cancel_whenNotCancellable_returnsErrorEnvelope() throws Exception {
        doThrow(new OrderServiceException(ErrorCode.ORDER_NOT_CANCELLABLE)).when(orderService).cancel(accountId, orderId);
        mockMvc.perform(authenticated(post("/v1/orders/{orderId}/cancel", orderId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.error.code").value(11002))
                .andExpect(jsonPath("$.error.service").value("order"));
    }

    @Test
    void create_whenBodyIsInvalid_serviceIsNeverCalled() throws Exception {
        mockMvc.perform(authenticated(post("/v1/orders"))
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"\",\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0]").value("sku=must not be blank"));
        verifyNoInteractions(orderService);
    }

    @Test
    void create_takesAccountFromAuthenticatedContext_neverFromRequestData() throws Exception {
        // Ayni istekte sahte kimlik her kanaldan gonderilir: query, header ve body. Hicbiri baglanmaz.
        mockMvc.perform(authenticated(post("/v1/orders"))
                        .queryParam("accountId", spoofedAccountId.toString())
                        .header("X-Account-Id", spoofedAccountId.toString())
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-1\",\"quantity\":2,\"accountId\":\"" + spoofedAccountId + "\"}"))
                .andExpect(status().isOk());
        verify(orderService).create(eq(accountId), eq(idempotencyKey), eq(new CreateOrderRequest("SKU-1", 2)));
        verify(orderService, never()).create(eq(spoofedAccountId), any(), any());
    }

    /** Surum header'i (uretimde zorunlu) - kimlik baglami YOK. */
    static MockHttpServletRequestBuilder versioned(MockHttpServletRequestBuilder b) { return b.header("API-Version", "1.0"); }

    /** Guvenlik filtresinin koydugu baglam: attribute x.accountId (String; JWT sub claim'i). */
    MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder b) {
        return versioned(b).requestAttr(CurrentAccountArgumentResolver.ACCOUNT_ID_ATTRIBUTE, accountId.toString());
    }
}
