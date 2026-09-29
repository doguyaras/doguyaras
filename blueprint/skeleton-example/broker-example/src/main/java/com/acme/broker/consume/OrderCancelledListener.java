package com.acme.broker.consume;

import com.acme.platform.messaging.inbox.InboxProcessor;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * order.order.cancelled tuketicisi (referans Bolum 11.3, 12.3). Manuel ack; karar tablosu (RabbitMQ 4.3.0'da dogrulandi):
 *
 *   sonuc                 broker'a giden               broker davranisi
 *   commit basarili   ->  basic.ack                    mesaj biter (ack YALNIZ commit'ten sonra)
 *   duplicate         ->  basic.ack                    inbox satiri vardi, is yapilmadi
 *   bilinmeyen tip    ->  basic.ack                    ileri uyumluluk: loglanir, yok sayilir (12.2)
 *   gecici hata       ->  basic.reject requeue=true    delivery_failed=true: x-delivery-count++ ve QQ native gecikmeli
 *                                                      retry (min*count); delivery-limit asilinca at-least-once DLQ
 *   zehirli mesaj     ->  basic.reject requeue=false   aninda DLQ, deneme yok
 *
 * NEDEN basic.reject, basic.nack degil: 4.3.0'da basic.nack requeue=true "return" komutudur (delivery_failed=false):
 * sayac artmaz, gecikme uygulanmaz, delivery-limit hic dolmaz -> gecikmesiz sonsuz requeue (12.3 "Kacin"). basic.reject
 * requeue=true ise "modify{delivery_failed=true}" olarak islenir; gecikmeli retry ve limit yalniz bu yolda calisir.
 *
 * Container MANUAL modda oldugu icin defaultRequeueRejected listener'in KENDI verdigi reject'leri etkilemez; yalniz
 * listener'dan kacan (siniflandirilmamis) exception'lar icin container'in yedek kuralidir ve false birakilir.
 */
public class OrderCancelledListener implements ChannelAwareMessageListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelledListener.class);

    public static final String HANDLER = "OrderCancelledNotificationHandler";   // inbox dedup kapsami
    public static final String EVENT_TYPE = "order.order.cancelled";

    private final InboxProcessor inbox;
    private final OrderCancelledEffect effect;
    private final JsonMapper json;
    private final DeliveryObserver observer;
    private final Clock clock;

    public OrderCancelledListener(InboxProcessor inbox, OrderCancelledEffect effect, JsonMapper json,
                                  DeliveryObserver observer, Clock clock) {
        this.inbox = inbox;
        this.effect = effect;
        this.json = json;
        this.observer = observer;
        this.clock = clock;
    }

    @Override
    public void onMessage(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        IncomingEvent event;
        try {
            event = IncomingEvent.decode(message, json);
        } catch (MalformedEventException e) {
            log.error("Poison message rejected to DLQ: code=CONSUMER_POISON messageId={} reason={}",
                    message.getMessageProperties().getMessageId(), e.getMessage());
            channel.basicReject(tag, false);                                    // yeniden teslim anlamsiz
            observe(null, DeliveryObserver.Outcome.POISON, 0, message.getMessageProperties().isRedelivered(), e);
            return;
        }
        if (!EVENT_TYPE.equals(event.type())) {                                 // binding order.order.* baska tipleri de getirir
            log.info("Ignoring unknown event type: type={} eventId={}", event.type(), event.id());
            channel.basicAck(tag, false);
            observe(event.id(), DeliveryObserver.Outcome.IGNORED_TYPE, event.deliveryCount(), event.redelivered(), null);
            return;
        }
        InboxProcessor.Outcome outcome;
        try {
            outcome = inbox.process(HANDLER, event.id(), () -> effect.apply(event));   // inbox satiri + is, tek TX
        } catch (MalformedEventException e) {                                   // payload bu handler icin gecersiz: TX geri alindi
            log.error("Poison payload rejected to DLQ: code=CONSUMER_POISON eventId={} reason={}", event.id(), e.getMessage());
            channel.basicReject(tag, false);
            observe(event.id(), DeliveryObserver.Outcome.POISON, event.deliveryCount(), event.redelivered(), e);
            return;
        } catch (RuntimeException e) {                                          // DB/is hatasi: gecici
            log.warn("Transient failure; broker will retry with delay: eventId={} deliveryCount={} exceptionType={}",
                    event.id(), event.deliveryCount(), e.getClass().getSimpleName());
            channel.basicReject(tag, true);                                     // delivery_failed=true -> gecikmeli retry
            observe(event.id(), DeliveryObserver.Outcome.TRANSIENT_FAILURE, event.deliveryCount(), event.redelivered(), e);
            return;
        }
        channel.basicAck(tag, false);                                           // commit'ten SONRA
        observe(event.id(), outcome == InboxProcessor.Outcome.APPLIED
                ? DeliveryObserver.Outcome.APPLIED : DeliveryObserver.Outcome.DUPLICATE,
                event.deliveryCount(), event.redelivered(), null);
    }

    private void observe(UUID id, DeliveryObserver.Outcome outcome, int deliveryCount, Boolean redelivered, Exception e) {
        observer.observe(new DeliveryObserver.Attempt(id, outcome, deliveryCount, Boolean.TRUE.equals(redelivered),
                clock.instant(), e == null ? null : e.getClass().getSimpleName()));
    }
}
