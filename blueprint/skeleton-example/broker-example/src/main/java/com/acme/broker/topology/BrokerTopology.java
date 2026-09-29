package com.acme.broker.topology;

import java.util.List;
import java.util.Map;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;

/**
 * Tuketici tarafinin sahip oldugu topoloji (referans Bolum 12.1, 12.3, 12.4): domain event'leri topic exchange'e gider,
 * her tuketici amac basina KENDI quorum queue'sunu, DLQ'sunu ve binding'ini bildirir ("once consumer deploy").
 *
 * RabbitMQ 4.3.0 uzerinde dogrulanmis kararlar:
 * - Gecikmeli retry QQ'nun kendi ozelligi: x-delayed-retry-type=failed + x-delayed-retry-min/max KUYRUK ARGUMANI olarak
 *   verilir. 4.3.0'da ayni anahtarlar policy olarak KABUL EDILMEZ ("delayed-retry-* are not recognised policy settings":
 *   rabbit_policies'te validator kayitli degil) — bu yuzden policy degil argument. Gecikme lineer: min * delivery_count,
 *   max ile sinirli.
 * - "failed" tipi yalniz delivery_failed=true ile geri verilen mesaji geciktirir; AMQP 0-9-1'de bunu basic.reject
 *   requeue=true uretir (basic.nack requeue=true DEGIL — o sayacsiz, gecikmesiz aninda requeue'dur). Listener'a bakiniz.
 * - at-least-once dead-letter stratejisi overflow=reject-publish ister; DLX'e giden mesaj DLQ'ya kadar QQ'da tutulur.
 * - delivery-limit=3: ilk teslim + 3 yeniden teslim; 4. basarisizlikta (x-delivery-count 3 > limit) DLQ.
 * - Stream ayni exchange'e "#" ile baglanir: analitik/replay tuketicileri queue'yu etkilemeden bastan okur (12.4).
 */
public final class BrokerTopology {

    /** Test/ornek isim oneki: paylasilan broker'da baska calismalarla cakismaz; uretimde "domain.events" vb. */
    public static final String PREFIX = "bvt.";

    public static final String DOMAIN_EVENTS_EXCHANGE = PREFIX + "domain.events";
    public static final String DLX = PREFIX + "dlx";
    public static final String ORDER_CANCELLED_QUEUE = PREFIX + "notification.order-cancelled.queue";
    public static final String ORDER_CANCELLED_DLQ = PREFIX + "notification.order-cancelled.dlq";
    public static final String ORDER_CANCELLED_BINDING_KEY = "order.order.*";
    public static final String DOMAIN_EVENTS_STREAM = PREFIX + "domain.events.stream";

    /** Baslangic ayarlari (Bolum 1.4): olcumle degisir. */
    public static final int DELIVERY_LIMIT = 3;
    public static final int DELAYED_RETRY_MIN_MS = 1000;
    public static final int DELAYED_RETRY_MAX_MS = 5000;
    public static final String STREAM_MAX_AGE = "1D";

    private BrokerTopology() { }

    public static TopicExchange domainEventsExchange() { return new TopicExchange(DOMAIN_EVENTS_EXCHANGE, true, false); }

    public static DirectExchange deadLetterExchange() { return new DirectExchange(DLX, true, false); }

    /** Quorum queue: delivery-limit + at-least-once DLX + native gecikmeli retry (hepsi kuyruk argumani). */
    public static Queue orderCancelledQueue() {
        return QueueBuilder.durable(ORDER_CANCELLED_QUEUE)
                .quorum()
                .deliveryLimit(DELIVERY_LIMIT)
                .withArgument("x-dead-letter-strategy", "at-least-once")
                .overflow(QueueBuilder.Overflow.rejectPublish)              // at-least-once DLX'in on kosulu
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(ORDER_CANCELLED_DLQ)                  // direct DLX -> DLQ adi
                .withArgument("x-delayed-retry-type", "failed")
                .withArgument("x-delayed-retry-min", DELAYED_RETRY_MIN_MS)
                .withArgument("x-delayed-retry-max", DELAYED_RETRY_MAX_MS)
                .build();
    }

    public static Queue orderCancelledDlq() { return QueueBuilder.durable(ORDER_CANCELLED_DLQ).quorum().build(); }

    /** Stream: append-only, broker'da offset, retention max-age; TTL/oncelik/DLX yok (12.4). */
    public static Queue domainEventsStream() {
        return QueueBuilder.durable(DOMAIN_EVENTS_STREAM).stream().withArgument("x-max-age", STREAM_MAX_AGE).build();
    }

    public static Binding orderCancelledBinding() {
        return BindingBuilder.bind(orderCancelledQueue()).to(domainEventsExchange()).with(ORDER_CANCELLED_BINDING_KEY);
    }

    public static Binding dlqBinding() {
        return BindingBuilder.bind(orderCancelledDlq()).to(deadLetterExchange()).with(ORDER_CANCELLED_DLQ);
    }

    public static Binding streamBinding() {
        return BindingBuilder.bind(domainEventsStream()).to(domainEventsExchange()).with("#");
    }

    /** Boot'ta bean olarak verilir (RabbitAdmin otomatik bildirir); burada declare(admin) ile elle. */
    public static Declarables declarables() {
        return new Declarables(List.of(domainEventsExchange(), deadLetterExchange(), orderCancelledQueue(),
                orderCancelledDlq(), domainEventsStream(), orderCancelledBinding(), dlqBinding(), streamBinding()));
    }

    public static void declare(AmqpAdmin admin) {
        for (Declarable d : declarables().getDeclarables()) {
            if (d instanceof Exchange e) admin.declareExchange(e);
            else if (d instanceof Queue q) admin.declareQueue(q);
            else if (d instanceof Binding b) admin.declareBinding(b);
        }
    }

    /** Silme sirasi: binding'ler kuyrukla gider; kuyruklar ve exchange'ler ayri silinir (paylasilan broker temizligi). */
    public static void delete(AmqpAdmin admin) {
        for (String q : List.of(ORDER_CANCELLED_QUEUE, ORDER_CANCELLED_DLQ, DOMAIN_EVENTS_STREAM)) admin.deleteQueue(q);
        for (String x : List.of(DOMAIN_EVENTS_EXCHANGE, DLX)) admin.deleteExchange(x);
    }

    /** Broker'in kabul ettigi argumanlar; management API'de queue.arguments ile karsilastirmak icin. */
    public static Map<String, Object> orderCancelledQueueArguments() { return orderCancelledQueue().getArguments(); }
}
