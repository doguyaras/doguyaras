package com.acme.broker.publish;

import com.acme.platform.messaging.outbox.OutboxEvent;
import com.acme.platform.messaging.outbox.OutboxHandler;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * EVENT lane handler'i (referans Bolum 12.3 Producer, 23.4): outbox satirini topic exchange'e routing key = event_type ile
 * yayinlar ve broker ACK'ini bekler. Uc basarisizlik da exception'dir, poller satiri PENDING + backoff'a ceker:
 * - NACK: broker kalici yazamadi (disk/quorum sorunu);
 * - RETURNED: mandatory + publisher returns ile "hicbir kuyruga yonlenmedi" (binding yok / tuketici henuz deploy edilmedi).
 *   ACK yine gelir (broker mesaji aldi ama attı); returned kontrolu olmadan olay SESSIZCE kaybolur;
 * - TIMEOUT: confirm sinirli sure beklenir (Bolum 12.3: orn. 5 sn); broker yanit vermiyorsa satir yeniden denenir.
 *
 * Tekrar yayin (kira dolumu, timeout sonrasi aslinda ulasmis mesaj) zararsizdir: tuketici inbox ile dedup yapar (11.3).
 * Confirm callback'i (metrik/log) template uzerinde ayrica kayitli olabilir; bu sinif CorrelationData future'ini kullanir.
 */
public class OutboxEventPublisher implements OutboxHandler {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final RabbitTemplate template;          // publisher-confirm-type=correlated, publisher-returns, mandatory
    private final CloudEventMessageFactory messages;
    private final String exchange;
    private final Duration confirmTimeout;

    public OutboxEventPublisher(RabbitTemplate template, CloudEventMessageFactory messages, String exchange,
                                Duration confirmTimeout) {
        this.template = template;
        this.messages = messages;
        this.exchange = exchange;
        this.confirmTimeout = confirmTimeout;
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        Message message = messages.toMessage(event);
        CorrelationData correlation = new CorrelationData(event.id().toString());
        template.send(exchange, event.eventType(), message, correlation);     // TX disinda; poller cagirir

        CorrelationData.Confirm confirm;
        try {
            confirm = correlation.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new ConfirmTimeoutException(event.id());
        } catch (ExecutionException e) {
            throw new BrokerNackException(event.id(), String.valueOf(e.getCause()));
        }
        ReturnedMessage returned = correlation.getReturned();                   // return, ack'ten ONCE gelir
        if (returned != null) {
            log.warn("Event unroutable; will retry: eventId={} routingKey={} replyCode={}",
                    event.id(), returned.getRoutingKey(), returned.getReplyCode());
            throw new UnroutableEventException(event.id(), returned.getRoutingKey());
        }
        if (!confirm.ack()) throw new BrokerNackException(event.id(), confirm.reason());
    }
}
