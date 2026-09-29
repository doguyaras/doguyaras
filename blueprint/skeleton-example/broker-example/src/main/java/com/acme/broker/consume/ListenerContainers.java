package com.acme.broker.consume;

import com.acme.broker.topology.BrokerTopology;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

/**
 * Tuketici container'i (referans Bolum 12.3 Consumer). Boot'ta ayni ayarlar spring.rabbitmq.listener.simple.* ile
 * container factory'ye verilir; burada seviye-3 test icin elle kurulur. Ayarlarin nedeni:
 * - MANUAL: ack yalniz inbox TX commit'inden sonra (11.3). AUTO modda container listener donunce ack'ler; listener
 *   commit'ten sonra donuyorsa o da gecerlidir ama "ack once" hatasi derleyici/okuyucu tarafindan gorulmez.
 * - prefetch 10: Spring varsayilani 250 (bir instance kuyrugu bosaltir, digerleri bos kalir; yavas is birikir).
 * - defaultRequeueRejected=false: listener'dan kacan siniflandirilmamis exception sonsuz requeue uretmesin (Spring
 *   varsayilani true). MANUAL modda container yalniz AmqpRejectAndDontRequeueException(rejectManual) icin devreye girer.
 * - recoveryInterval: broker dusunce yeniden baglanma araligi (uretim 5000 ms; test 500 ms).
 */
public final class ListenerContainers {

    public static final int PREFETCH = 10;

    private ListenerContainers() { }

    public static SimpleMessageListenerContainer orderCancelled(ConnectionFactory cf, ChannelAwareMessageListener listener,
                                                                long recoveryIntervalMs) {
        return on(cf, BrokerTopology.ORDER_CANCELLED_QUEUE, listener, recoveryIntervalMs);
    }

    public static SimpleMessageListenerContainer on(ConnectionFactory cf, String queue, ChannelAwareMessageListener listener,
                                                    long recoveryIntervalMs) {
        SimpleMessageListenerContainer c = new SimpleMessageListenerContainer(cf);
        c.setQueueNames(queue);
        c.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        c.setPrefetchCount(PREFETCH);
        c.setDefaultRequeueRejected(false);
        c.setConcurrentConsumers(1);
        c.setRecoveryInterval(recoveryIntervalMs);
        c.setMissingQueuesFatal(false);                 // broker gecici olarak dusukken container kendini kapatmasin
        c.setMessageListener(listener);
        return c;
    }
}
