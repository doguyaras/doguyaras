package com.acme.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.acme.order.api.dto.CreateOrderRequest;
import com.acme.order.entity.Order;
import com.acme.order.exception.OrderServiceException;
import com.acme.order.repository.OrderRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Log privacy sablonu (referans Bolum 23.6, 8.5): guvenli alan (outcome=) VAR; sentetik hassas isaretler
 * rendered mesajda, argumanlarda, MDC'de ve exception'da YOK. Appender her testten sonra ayrilir, seviye geri yuklenir.
 */
class OrderServiceLogPrivacyTest {

    static final String PHONE_MARKER = "+905551234567";
    static final String EMAIL_MARKER = "jane.doe@example.com";
    static final String TOKEN_MARKER = "SECRET-TOKEN-MARKER-4e5f6a7b";

    private final OrderRepository repo = mock(OrderRepository.class);
    private final OrderServiceImpl service = new OrderServiceImpl(repo);
    private final UUID accountId = UUID.fromString("aaaaaaaa-0000-4000-8000-00000000a001");

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(OrderServiceImpl.class);
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        logger.setLevel(null);
    }

    @Test
    void create_whenRejected_logsCodeReasonOutcome_withoutSensitiveInputOrAccountId() {
        // sku istemciden gelen serbest metindir: CR/LF (log injection), telefon, e-posta ve token tasiyor olabilir.
        String sku = "SKU-1\r\nfake=line " + PHONE_MARKER + " " + EMAIL_MARKER + " token=" + TOKEN_MARKER;

        assertThatThrownBy(() -> service.create(accountId, UUID.randomUUID(), new CreateOrderRequest(sku, 0)))
                .isInstanceOf(OrderServiceException.class);

        assertThat(rendered())
                .anyMatch(m -> m.contains("Order rejected:") && m.contains("code=ORDER_QUANTITY_INVALID")
                        && m.contains("reason=QUANTITY_OUT_OF_RANGE") && m.contains("outcome=REJECTED"))
                .noneMatch(m -> m.contains("\n") || m.contains("\r"));
        // Maskeli/redakte halleri var, ham degerler yok:
        assertThat(rendered()).anyMatch(m -> m.contains("+90********67") && m.contains("j***@e***.com") && m.contains("token=[REDACTED]"));
        assertNoMarkerAnywhere(PHONE_MARKER);
        assertNoMarkerAnywhere(EMAIL_MARKER);
        assertNoMarkerAnywhere(TOKEN_MARKER);
        assertNoMarkerAnywhere(accountId.toString());
    }

    @Test
    void create_whenSucceeds_logsOutcomeSuccessWithoutAccountId() {
        UUID id = service.create(accountId, UUID.randomUUID(), new CreateOrderRequest("SKU-1", 2));

        assertThat(rendered()).anyMatch(m -> m.contains("operation=ORDER_CREATE") && m.contains("outcome=SUCCESS")
                && m.contains("orderId=" + id));
        assertNoMarkerAnywhere(accountId.toString());
    }

    @Test
    void cancel_whenNotFound_logsRejectionBeforeThrowing_withoutAccountId() {
        UUID orderId = UUID.randomUUID();
        when(repo.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(accountId, orderId)).isInstanceOf(OrderServiceException.class);

        assertThat(rendered()).anyMatch(m -> m.contains("code=ORDER_NOT_FOUND") && m.contains("outcome=REJECTED")
                && m.contains("orderId=" + orderId));
        assertNoMarkerAnywhere(accountId.toString());
    }

    @Test
    void cancel_whenNotCancellable_logsStatusAsReason() {
        UUID orderId = UUID.randomUUID();
        when(repo.findById(orderId)).thenReturn(Optional.of(new Order(orderId, "SKU-1", 1, "SHIPPED")));

        assertThatThrownBy(() -> service.cancel(accountId, orderId)).isInstanceOf(OrderServiceException.class);

        assertThat(rendered()).anyMatch(m -> m.contains("code=ORDER_NOT_CANCELLABLE") && m.contains("reason=STATUS_SHIPPED"));
    }

    private Stream<String> rendered() { return appender.list.stream().map(ILoggingEvent::getFormattedMessage); }

    private void assertNoMarkerAnywhere(String marker) {
        for (ILoggingEvent e : appender.list) {
            assertThat(e.getFormattedMessage()).doesNotContain(marker);
            if (e.getArgumentArray() != null) {
                assertThat(Stream.of(e.getArgumentArray()).map(String::valueOf)).noneMatch(a -> a.contains(marker));
            }
            assertThat(e.getMDCPropertyMap().values()).noneMatch(v -> v.contains(marker));
            for (var p = e.getThrowableProxy(); p != null; p = p.getCause()) {
                assertThat(String.valueOf(p.getMessage())).doesNotContain(marker);
            }
        }
    }
}
