package com.acme.broker.stream;

import com.acme.platform.messaging.cloudevents.CloudEventHeaders;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

/**
 * Replay/analitik tuketicisi (referans Bolum 12.4): stream'i AMQP 0-9-1 uzerinden x-stream-offset ile okur. Okuma
 * yikici degildir; ack yalniz istemci tarafinda kredi (prefetch) acar, mesaj stream'de kalir. Ayni offset'ten ikinci
 * okuyucu ayni mesajlari alir. spring-rabbit-stream (RabbitMQ stream protokolu, 5552) ayni isi daha yuksek hizla yapar;
 * classpath'te olmadigi icin burada 0-9-1 yolu kullanilmistir — iki yol da broker'in resmi destekledigi yollardir.
 *
 * Streams manuel ack + prefetch zorunlu kilar (prefetch'siz consume broker tarafindan reddedilir).
 */
public class StreamReplayReader {

    /** Okunan bir kayit: stream offset'i broker'in verdigi mutlak konumdur (x-stream-offset header'i). */
    public record Entry(long offset, String eventId, String type, String body) {}

    private final ConnectionFactory connectionFactory;
    private final String stream;
    private final int prefetch;

    public StreamReplayReader(ConnectionFactory connectionFactory, String stream, int prefetch) {
        this.connectionFactory = connectionFactory;
        this.stream = stream;
        this.prefetch = prefetch;
    }

    /**
     * @param offset "first" | "last" | "next" | Long (mutlak offset) | timestamp; broker sozlesmesi
     * @param expected bu kadar kayit gelince durur (bounded okuma; sinirsiz akis icin container kullanilir)
     */
    public List<Entry> read(Object offset, int expected, Duration timeout) throws IOException, InterruptedException {
        List<Entry> out = new ArrayList<>();
        CountDownLatch done = new CountDownLatch(expected);
        try (Connection conn = connectionFactory.createConnection(); Channel ch = conn.createChannel(false)) {
            ch.basicQos(prefetch);
            String tag = ch.basicConsume(stream, false, Map.of("x-stream-offset", offset), new DefaultConsumer(ch) {
                @Override
                public void handleDelivery(String consumerTag, Envelope env, AMQP.BasicProperties props, byte[] body)
                        throws IOException {
                    Map<String, Object> headers = props.getHeaders() == null ? Map.of() : props.getHeaders();
                    Object off = headers.get("x-stream-offset");
                    synchronized (out) {
                        if (out.size() < expected) {
                            out.add(new Entry(off instanceof Number n ? n.longValue() : -1L,
                                    String.valueOf(headers.get(CloudEventHeaders.ID)),
                                    String.valueOf(headers.get(CloudEventHeaders.TYPE)),
                                    new String(body, java.nio.charset.StandardCharsets.UTF_8)));
                        }
                    }
                    ch.basicAck(env.getDeliveryTag(), false);                    // kredi acar; stream'den silmez
                    done.countDown();
                }
            });
            done.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
            ch.basicCancel(tag);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IOException(e);
        }
        synchronized (out) { return List.copyOf(out); }
    }
}
