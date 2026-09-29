package com.acme.broker;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.broker.consume.DeliveryObserver;
import com.acme.broker.consume.DeliveryObserver.Attempt;
import com.acme.broker.consume.DeliveryObserver.Outcome;
import com.acme.broker.consume.ListenerContainers;
import com.acme.broker.consume.OrderCancelledEffect;
import com.acme.broker.consume.OrderCancelledListener;
import com.acme.broker.publish.CloudEventMessageFactory;
import com.acme.broker.publish.OutboxEventPublisher;
import com.acme.broker.stream.StreamReplayReader;
import com.acme.broker.support.ManagementApi;
import com.acme.broker.support.RabbitCtl;
import com.acme.broker.topology.BrokerTopology;
import com.acme.platform.messaging.cloudevents.CloudEventHeaders;
import com.acme.platform.messaging.inbox.InboxProcessor;
import com.acme.platform.messaging.outbox.OutboxEvent;
import com.acme.platform.messaging.outbox.OutboxPoller;
import com.acme.platform.messaging.outbox.OutboxProperties;
import com.acme.platform.messaging.outbox.OutboxRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * SEVIYE 3 kanit (referans Bolum 19.6): gercek RabbitMQ 4.3 + gercek PostgreSQL (gomulu 18.x). Uretici ("order" semasi,
 * outbox) -> topic exchange -> quorum queue -> tuketici ("notification" semasi, inbox + etki). Senaryolar a-j gorev
 * tanimindaki harflerle; k-l ek kanit. Zaman: poller icin deterministik MutableClock (backoff beklenmez), tuketici icin
 * sistem saati (broker gecikmeleri duvar saatiyle olculur).
 *
 * Broker ve rabbitmqctl yoksa test ACIK mesajla basarisiz olur; sessiz atlama yoktur (0 test = basarisiz kurali).
 */
class BrokerBehaviourIT {

    static final String PRODUCER_SCHEMA = "order";
    static final String CONSUMER_SCHEMA = "notification";
    static final String SOURCE = "urn:acme:order";
    static final String CONSUMER_TIMEOUT_POLICY = "bvt-consumer-timeout";
    static final Duration BOUND = Duration.ofSeconds(20);

    static EmbeddedPostgres pg;
    static DataSource ds;
    static NamedParameterJdbcTemplate jdbc;
    static JdbcTemplate plain;
    static TransactionTemplate tx;
    static CachingConnectionFactory cf;
    static RabbitAdmin admin;
    static RabbitTemplate template;
    static ManagementApi mgmt;
    static JsonMapper json = JsonMapper.builder().build();
    static final AtomicInteger confirmAcks = new AtomicInteger();
    static final AtomicInteger confirmNacks = new AtomicInteger();

    OutboxRepository outbox;
    InboxProcessor inbox;
    MutableClock clock;
    OutboxEventPublisher publisher;
    final List<Attempt> attempts = new CopyOnWriteArrayList<>();
    SimpleMessageListenerContainer container;

    /** Deterministik poller zamani: backoff (60 sn) gercek beklemeden gecilir. */
    static final class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2026-09-29T10:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    // ---------- kurulum ----------

    @BeforeAll
    static void start() throws Exception {
        pg = EmbeddedPostgres.builder().start();
        ds = pg.getPostgresDatabase();
        jdbc = new NamedParameterJdbcTemplate(ds);
        plain = jdbc.getJdbcTemplate();
        tx = new TransactionTemplate(new JdbcTransactionManager(ds));
        String platformDdl = resource("/db/platform/outbox_inbox.sql");
        String consumerDdl = resource("/db/broker/notification.sql");
        for (String schema : List.of(PRODUCER_SCHEMA, CONSUMER_SCHEMA)) {
            plain.execute("CREATE SCHEMA \"" + schema + "\"");
            runScript(platformDdl.replace("${schema}", "\"" + schema + "\""));
        }
        runScript(consumerDdl.replace("${schema}", "\"" + CONSUMER_SCHEMA + "\""));

        cf = new CachingConnectionFactory("127.0.0.1", 5672);
        cf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);   // 12.3 Producer
        cf.setPublisherReturns(true);
        try {
            cf.createConnection().close();
        } catch (RuntimeException e) {
            throw new IllegalStateException("RabbitMQ not reachable at 127.0.0.1:5672; this level-3 IT needs a real broker", e);
        }
        template = new RabbitTemplate(cf);
        template.setMandatory(true);
        template.setConfirmCallback((cd, ack, cause) -> (ack ? confirmAcks : confirmNacks).incrementAndGet());
        template.setReturnsCallback(r -> { });                                     // CorrelationData.returned zaten dolar
        admin = new RabbitAdmin(cf);
        BrokerTopology.delete(admin);                                                // onceki kosudan kalinti
        BrokerTopology.declare(admin);
        mgmt = new ManagementApi("http://127.0.0.1:15672", "guest", "guest", "/");
        assertThat(mgmt.alive()).as("management API").isTrue();
    }

    @AfterAll
    static void stop() throws IOException {
        try {
            if (admin != null) { admin.deleteQueue(BrokerTopology.ORDER_CANCELLED_QUEUE); BrokerTopology.delete(admin); }
        } finally {
            if (cf != null) cf.destroy();
            if (pg != null) pg.close();
        }
    }

    @BeforeEach
    void reset() throws Exception {
        plain.execute("TRUNCATE \"order\".outbox_event, \"order\".inbox_event, \"notification\".outbox_event, "
                + "\"notification\".inbox_event, \"notification\".order_cancelled_effect");
        admin.purgeQueue(BrokerTopology.ORDER_CANCELLED_QUEUE);
        admin.purgeQueue(BrokerTopology.ORDER_CANCELLED_DLQ);
        // management istatistigi 5 sn'ye kadar gecikir: her test dogrulanmis SIFIR derinlikten baslar (bayat sayim yok)
        assertThat(await(Duration.ofSeconds(15), () -> mgmtDepth(BrokerTopology.ORDER_CANCELLED_QUEUE) == 0
                && mgmtDepth(BrokerTopology.ORDER_CANCELLED_DLQ) == 0)).as("queues empty at start").isTrue();
        admin.deleteQueue(BrokerTopology.DOMAIN_EVENTS_STREAM);                      // stream purge edilemez: yeniden kur
        admin.declareQueue(BrokerTopology.domainEventsStream());
        admin.declareBinding(BrokerTopology.streamBinding());
        attempts.clear();
        confirmAcks.set(0);
        confirmNacks.set(0);
        clock = new MutableClock();
        outbox = new OutboxRepository(jdbc, PRODUCER_SCHEMA);
        inbox = new InboxProcessor(jdbc, tx, CONSUMER_SCHEMA);
        publisher = new OutboxEventPublisher(template, new CloudEventMessageFactory(json, SOURCE),
                BrokerTopology.DOMAIN_EVENTS_EXCHANGE, Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (container != null) { container.stop(); container = null; }
        mgmt.deletePolicy(CONSUMER_TIMEOUT_POLICY);
    }

    // ---------- yardimcilar ----------

    static String resource(String path) throws IOException {
        return new String(BrokerBehaviourIT.class.getResourceAsStream(path).readAllBytes(), StandardCharsets.UTF_8);
    }

    static void runScript(String ddl) throws Exception {
        try (var conn = ds.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8)));
        }
    }

    OutboxEvent cancelledEvent(UUID orderId, Instant createdAt) {
        return new OutboxEvent(UUID.randomUUID(), "EVENT", "order", orderId, OrderCancelledListener.EVENT_TYPE,
                "{\"orderId\":\"" + orderId + "\",\"reason\":\"customer_request\"}",
                "{\"traceparent\":\"00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01\"}",
                "PENDING", 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT, 0, createdAt, null, null, null, createdAt);
    }

    OutboxEvent cancelledEvent() { return cancelledEvent(UUID.randomUUID(), clock.instant()); }

    void appendInTx(OutboxEvent... events) { tx.executeWithoutResult(s -> { for (OutboxEvent e : events) outbox.append(e); }); }

    OutboxPoller poller() { return new OutboxPoller(outbox, Map.of("EVENT", publisher), OutboxProperties.defaults(), clock); }

    /** Gercek is etkisi: tuketici semasina satir (inbox TX'i icinde). */
    final OrderCancelledEffect realEffect = e -> jdbc.update("""
            INSERT INTO "notification".order_cancelled_effect (event_id, order_id, reason, applied_at)
            VALUES (:e, :o, :r, :t)""",
            Map.of("e", e.id(), "o", UUID.fromString(e.data().get("orderId").asString()),
                    "r", e.data().get("reason").asString(), "t", java.sql.Timestamp.from(Instant.now())));

    void startConsumer(OrderCancelledEffect effect) {
        OrderCancelledListener listener = new OrderCancelledListener(inbox, effect, json, attempts::add, Clock.systemUTC());
        container = ListenerContainers.orderCancelled(cf, listener, 500);
        container.start();
    }

    int effects() { return plain.queryForObject("SELECT count(*) FROM \"notification\".order_cancelled_effect", Integer.class); }

    int effectsOf(UUID eventId) {
        return jdbc.queryForObject("SELECT count(*) FROM \"notification\".order_cancelled_effect WHERE event_id = :e",
                Map.of("e", eventId), Integer.class);
    }

    int inboxRows() { return plain.queryForObject("SELECT count(*) FROM \"notification\".inbox_event", Integer.class); }

    int outboxRows() { return plain.queryForObject("SELECT count(*) FROM \"order\".outbox_event", Integer.class); }

    /** Bounded polling: kosul saglanana kadar en fazla BOUND bekler; saglanmazsa false (assert cagirana ait). */
    static boolean await(Duration max, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + max.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(50);
        }
        return condition.getAsBoolean();
    }

    Message rawMessage(UUID id, String type, String body) {
        MessageProperties p = new MessageProperties();
        p.setMessageId(id.toString());
        p.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        p.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        CloudEventHeaders.of(id, type, SOURCE, null, Instant.now(), Map.of()).forEach(p::setHeader);
        return new Message(body.getBytes(StandardCharsets.UTF_8), p);
    }

    long gapMs(Attempt a, Attempt b) { return Duration.between(a.at(), b.at()).toMillis(); }

    // ---------- senaryolar ----------

    @Test // a: outbox -> broker -> tuketici uctan uca; 20 satir tam bir kez; confirm 20; outbox bos
    void a_outboxToConsumerEndToEndExactlyOnce() throws Exception {
        startConsumer(realEffect);
        OutboxEvent[] events = new OutboxEvent[20];
        for (int i = 0; i < 20; i++) events[i] = cancelledEvent(UUID.randomUUID(), clock.instant().minusMillis(100 - i));  // gecmiste, sirali
        appendInTx(events);

        OutboxPoller.PollResult r = poller().poll("EVENT");
        assertThat(r.applied()).isEqualTo(20);
        assertThat(r.failed()).isZero();
        assertThat(confirmAcks.get()).isEqualTo(20);                                   // publisher confirm callback
        assertThat(confirmNacks.get()).isZero();
        assertThat(outboxRows()).isZero();

        assertThat(await(BOUND, () -> effects() == 20)).as("20 effects within bound").isTrue();
        assertThat(inboxRows()).isEqualTo(20);
        for (OutboxEvent e : events) assertThat(effectsOf(e.id())).isEqualTo(1);
        assertThat(attempts).hasSize(20).allMatch(a -> a.outcome() == Outcome.APPLIED);
        // trace baglami tasindi (ce-* eslemesi platform-messaging'de tek yerde)
        assertThat(admin.getQueueInfo(BrokerTopology.ORDER_CANCELLED_QUEUE).getMessageCount()).isZero();
    }

    @Test // b: binding'i olmayan routing key -> basic.return -> handler exception -> satir PENDING, retry_count 1
    void b_unroutableEventStaysPendingWithRetry() throws Exception {
        admin.removeBinding(BrokerTopology.streamBinding());                          // "#" stream her seyi yakalar
        try {
            UUID orderId = UUID.randomUUID();
            OutboxEvent e = new OutboxEvent(UUID.randomUUID(), "EVENT", "order", orderId, "order.payment.failed",
                    "{\"orderId\":\"" + orderId + "\"}", "{}", "PENDING", 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT, 0,
                    clock.instant(), null, null, null, clock.instant());
            appendInTx(e);

            OutboxPoller.PollResult r = poller().poll("EVENT");
            assertThat(r.failed()).isEqualTo(1);
            assertThat(r.applied()).isZero();
            OutboxEvent row = outbox.findAll().get(0);
            assertThat(row.status()).isEqualTo("PENDING");
            assertThat(row.retryCount()).isEqualTo(1);
            assertThat(row.lastErrorCode()).isEqualTo("UnroutableEventException");
            assertThat(row.nextRetryAt()).isEqualTo(clock.instant().plusSeconds(60));
            assertThat(confirmAcks.get()).as("broker yine ACK verir; kayip returned kontrolu ile yakalanir").isEqualTo(1);
        } finally {
            admin.declareBinding(BrokerTopology.streamBinding());
        }
    }

    @Test // c: native gecikmeli retry: ilk 2 teslimde gecici hata; yeniden teslimler >= min gecikmeli, 3. basarili, tek etki
    void c_transientFailureIsRetriedByBrokerWithDelay() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        startConsumer(e -> {
            if (calls.incrementAndGet() <= 2) throw new IllegalStateException("simulated transient failure");
            realEffect.apply(e);
        });
        OutboxEvent e = cancelledEvent();
        appendInTx(e);
        assertThat(poller().poll("EVENT").applied()).isEqualTo(1);

        assertThat(await(BOUND, () -> attempts.stream().anyMatch(a -> a.outcome() == Outcome.APPLIED))).isTrue();
        assertThat(attempts).hasSize(3);
        assertThat(attempts.get(0).outcome()).isEqualTo(Outcome.TRANSIENT_FAILURE);
        assertThat(attempts.get(0).deliveryCount()).isZero();
        assertThat(attempts.get(0).redelivered()).isFalse();
        assertThat(attempts.get(1).outcome()).isEqualTo(Outcome.TRANSIENT_FAILURE);
        assertThat(attempts.get(1).deliveryCount()).isEqualTo(1);                       // x-delivery-count
        assertThat(attempts.get(1).redelivered()).isTrue();
        assertThat(attempts.get(2).outcome()).isEqualTo(Outcome.APPLIED);
        assertThat(attempts.get(2).deliveryCount()).isEqualTo(2);
        // gecikme lineer: min * delivery_count (1000, 2000 ms); tolerans %10 (zamanlayici/aginin payi)
        assertThat(gapMs(attempts.get(0), attempts.get(1))).isGreaterThanOrEqualTo(900);
        assertThat(gapMs(attempts.get(1), attempts.get(2))).isGreaterThanOrEqualTo(1800);
        assertThat(effectsOf(e.id())).isEqualTo(1);
        assertThat(inboxRows()).isEqualTo(1);
        assertThat(attempts.get(0).errorType()).isEqualTo("IllegalStateException");
    }

    @Test // d: her teslimde hata -> delivery-limit (3) sonra at-least-once DLQ; ana kuyruk bos; etki yok
    void d_deliveryLimitSendsMessageToDlq() throws Exception {
        startConsumer(e -> { throw new IllegalStateException("always failing"); });
        OutboxEvent e = cancelledEvent();
        appendInTx(e);
        assertThat(poller().poll("EVENT").applied()).isEqualTo(1);

        await(BOUND, () -> attempts.size() >= 1 + BrokerTopology.DELIVERY_LIMIT);
        assertThat(await(BOUND, () -> mgmtDepth(BrokerTopology.ORDER_CANCELLED_DLQ) == 1))
                .as("DLQ depth 1 (attempts seen: " + attempts.size() + ")").isTrue();
        assertThat(mgmtDepth(BrokerTopology.ORDER_CANCELLED_QUEUE)).isZero();
        assertThat(admin.getQueueInfo(BrokerTopology.ORDER_CANCELLED_QUEUE).getMessageCount()).isZero();
        assertThat(attempts).as("ilk + 3 yeniden teslim; fazlasi sicak requeue dongusudur").hasSize(1 + BrokerTopology.DELIVERY_LIMIT);
        assertThat(attempts).extracting(Attempt::deliveryCount).containsExactly(0, 1, 2, 3);
        assertThat(attempts).allMatch(a -> a.outcome() == Outcome.TRANSIENT_FAILURE);
        assertThat(effects()).isZero();
        assertThat(inboxRows()).isZero();
    }

    @Test // e: zehirli mesaj (JSON degil) -> requeue=false -> aninda DLQ; tek deneme
    void e_poisonMessageGoesToDlqImmediately() throws Exception {
        startConsumer(realEffect);
        template.send(BrokerTopology.DOMAIN_EVENTS_EXCHANGE, OrderCancelledListener.EVENT_TYPE,
                rawMessage(UUID.randomUUID(), OrderCancelledListener.EVENT_TYPE, "this is not json"));

        assertThat(await(BOUND, () -> attempts.size() == 1)).isTrue();
        assertThat(await(BOUND, () -> mgmtDepth(BrokerTopology.ORDER_CANCELLED_DLQ) == 1)).as("DLQ depth 1").isTrue();
        assertThat(attempts).as("deneme yok: DLQ'ya giderken yeniden teslim olmadi").hasSize(1);
        assertThat(attempts.get(0).outcome()).isEqualTo(Outcome.POISON);
        assertThat(attempts.get(0).errorType()).isEqualTo("MalformedEventException");
        assertThat(mgmtDepth(BrokerTopology.ORDER_CANCELLED_QUEUE)).isZero();
        assertThat(effects()).isZero();
        assertThat(inboxRows()).isZero();
    }

    @Test // f: ack commit'ten sonra: inbox satiri yazildiktan sonra is patlar -> hicbir sey commit olmaz; yeniden teslim basarili
    void f_ackOnlyAfterCommit() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        startConsumer(e -> {
            realEffect.apply(e);                                                           // etki yazildi (henuz commit yok)
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("crash after inbox insert");
        });
        OutboxEvent e = cancelledEvent();
        appendInTx(e);
        assertThat(poller().poll("EVENT").applied()).isEqualTo(1);

        assertThat(await(BOUND, () -> attempts.stream().anyMatch(a -> a.outcome() == Outcome.APPLIED))).isTrue();
        assertThat(attempts).extracting(Attempt::outcome).containsExactly(Outcome.TRANSIENT_FAILURE, Outcome.APPLIED);
        assertThat(attempts.get(1).redelivered()).isTrue();
        assertThat(gapMs(attempts.get(0), attempts.get(1))).isGreaterThanOrEqualTo(900);  // gecikmeli retry
        assertThat(effectsOf(e.id())).isEqualTo(1);                                       // ilk denemenin etkisi geri alindi
        assertThat(inboxRows()).isEqualTo(1);
    }

    @Test // g: ayni olay (ayni ce-id) iki kez yayinlanir -> tuketici bir kez uygular (inbox dedup), ikisi de ack
    void g_duplicateDeliveryIsAppliedOnce() throws Exception {
        startConsumer(realEffect);
        OutboxEvent e = cancelledEvent();
        publisher.handle(e);
        publisher.handle(e);                                                                // kira dolumu / timeout sonrasi tekrar yayin

        assertThat(await(BOUND, () -> attempts.size() == 2)).isTrue();
        assertThat(attempts).extracting(Attempt::outcome).containsExactly(Outcome.APPLIED, Outcome.DUPLICATE);
        assertThat(effectsOf(e.id())).isEqualTo(1);
        assertThat(inboxRows()).isEqualTo(1);
        assertThat(admin.getQueueInfo(BrokerTopology.ORDER_CANCELLED_QUEUE).getMessageCount()).isZero();
    }

    @Test // h: broker dusuk (stop_app): poller exception sizdirmaz, satir PENDING + retry + hata kodu; start_app sonrasi yayin ve tuketim
    void h_brokerDownThenRecovers() throws Exception {
        RabbitCtl ctl = new RabbitCtl();
        assertThat(ctl.available()).as("rabbitmqctl bulunamadi: " + ctl.describe()).isTrue();
        startConsumer(realEffect);
        OutboxEvent e = cancelledEvent();
        appendInTx(e);
        try {
            ctl.stopApp();
            OutboxPoller.PollResult r = poller().poll("EVENT");                          // exception yok
            assertThat(r.failed()).isEqualTo(1);
            OutboxEvent row = outbox.findAll().get(0);
            assertThat(row.status()).isEqualTo("PENDING");
            assertThat(row.retryCount()).isGreaterThanOrEqualTo(1);
            // kod, mesaj/host degil. Durdurma aninda baglanti kurulamiyorsa AmqpConnectException, kurulu baglanti
            // kapanirken AmqpIOException gelir (CI: docker exec rabbitmqctl ile gozlendi); ikisi de gecici broker hatasidir.
            assertThat(row.lastErrorCode()).isIn("AmqpConnectException", "AmqpIOException");
        } finally {
            ctl.startApp();
        }
        assertThat(await(Duration.ofSeconds(60), () -> mgmt.alive())).as("broker back").isTrue();
        assertThat(await(Duration.ofSeconds(30), () -> connectable())).as("AMQP back").isTrue();

        clock.advance(Duration.ofSeconds(61));                                              // backoff doldu
        assertThat(await(BOUND, () -> {
            clock.advance(Duration.ofSeconds(61));
            return poller().poll("EVENT").applied() == 1;
        })).as("republished after recovery").isTrue();
        assertThat(outboxRows()).isZero();
        assertThat(await(BOUND, () -> effectsOf(e.id()) == 1)).as("consumer recovered and applied").isTrue();
    }

    @Test // i: stream replay: 5 olay; "first"ten okuyan 5'ini sirayla alir; ikinci okuyucu offset 0'dan aynisini alir
    void i_streamReplayFromFirstOffset() throws Exception {
        OutboxEvent[] events = new OutboxEvent[5];
        for (int i = 0; i < 5; i++) events[i] = cancelledEvent(UUID.randomUUID(), clock.instant().minusMillis(100 - i));
        appendInTx(events);
        assertThat(poller().poll("EVENT").applied()).isEqualTo(5);
        List<String> published = java.util.Arrays.stream(events).map(ev -> ev.id().toString()).toList();

        StreamReplayReader reader = new StreamReplayReader(cf, BrokerTopology.DOMAIN_EVENTS_STREAM, 50);
        List<StreamReplayReader.Entry> first = reader.read("first", 5, Duration.ofSeconds(10));
        assertThat(first).extracting(StreamReplayReader.Entry::eventId).containsExactlyElementsOf(published);
        assertThat(first).extracting(StreamReplayReader.Entry::offset).containsExactly(0L, 1L, 2L, 3L, 4L);
        assertThat(first).allMatch(en -> en.type().equals(OrderCancelledListener.EVENT_TYPE));

        List<StreamReplayReader.Entry> replay = reader.read(0L, 5, Duration.ofSeconds(10));   // yikici degil
        assertThat(replay).extracting(StreamReplayReader.Entry::eventId).containsExactlyElementsOf(published);
        // quorum queue tuketicisinden bagimsiz: kuyrukta 5 mesaj hala bekliyor (consumer baslatilmadi)
        assertThat(admin.getQueueInfo(BrokerTopology.ORDER_CANCELLED_QUEUE).getMessageCount()).isEqualTo(5);
    }

    @Test // j: consumer-timeout policy (5 sn): ack'lenmeyen mesaj broker tarafindan geri alinir ve yeniden teslim edilir
    void j_consumerTimeoutReleasesUnackedMessage() throws Exception {
        int put = mgmt.putPolicy(CONSUMER_TIMEOUT_POLICY, "^" + BrokerTopology.ORDER_CANCELLED_QUEUE.replace(".", "\\.") + "$",
                Map.of("consumer-timeout", 5000), "quorum_queues");
        assertThat(put).isIn(201, 204);
        List<Instant> deliveries = new CopyOnWriteArrayList<>();
        List<Boolean> redelivered = new CopyOnWriteArrayList<>();
        CountDownLatch acked = new CountDownLatch(1);
        ChannelAwareMessageListener holding = (message, channel) -> {
            deliveries.add(Instant.now());
            redelivered.add(Boolean.TRUE.equals(message.getMessageProperties().isRedelivered()));
            if (deliveries.size() == 1) {
                new CountDownLatch(1).await(6, TimeUnit.SECONDS);                        // ack'siz tutar (yavas tuketici)
                return;                                                                  // ack yok; broker zaten geri aldi
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            acked.countDown();
        };
        container = ListenerContainers.on(cf, BrokerTopology.ORDER_CANCELLED_QUEUE, holding, 500);
        container.start();                                                                 // policy consume aninda okunur
        template.send(BrokerTopology.DOMAIN_EVENTS_EXCHANGE, OrderCancelledListener.EVENT_TYPE,
                rawMessage(UUID.randomUUID(), OrderCancelledListener.EVENT_TYPE, "{\"orderId\":\"x\"}"));

        assertThat(acked.await(BOUND.toSeconds(), TimeUnit.SECONDS)).as("redelivered after consumer timeout").isTrue();
        assertThat(deliveries).hasSize(2);
        assertThat(redelivered).containsExactly(false, true);
        long gap = Duration.between(deliveries.get(0), deliveries.get(1)).toMillis();
        assertThat(gap).isGreaterThanOrEqualTo(4500);                                       // >= timeout (tolerans %10)
        assertThat(await(BOUND, () -> mgmtDepth(BrokerTopology.ORDER_CANCELLED_QUEUE) == 0)).isTrue();
    }

    @Test // k: gecikmeli retry KUYRUK ARGUMANI ile her 4.3.x'te etkin; policy yolu SURUME BAGLI (4.3.0 reddeder, 4.3.6 kabul eder)
    void k_delayedRetryIsEffectiveViaQueueArgumentsPolicyDependsOnVersion() throws Exception {
        JsonNode q = mgmt.queue(BrokerTopology.ORDER_CANCELLED_QUEUE);
        JsonNode args = q.get("arguments");
        assertThat(args.get("x-queue-type").asString()).isEqualTo("quorum");
        assertThat(args.get("x-delayed-retry-type").asString()).isEqualTo("failed");
        assertThat(args.get("x-delayed-retry-min").asInt()).isEqualTo(BrokerTopology.DELAYED_RETRY_MIN_MS);
        assertThat(args.get("x-delayed-retry-max").asInt()).isEqualTo(BrokerTopology.DELAYED_RETRY_MAX_MS);
        assertThat(args.get("x-delivery-limit").asInt()).isEqualTo(BrokerTopology.DELIVERY_LIMIT);
        assertThat(args.get("x-dead-letter-strategy").asString()).isEqualTo("at-least-once");
        assertThat(args.get("x-overflow").asString()).isEqualTo("reject-publish");

        String version = mgmt.brokerVersion();
        String probeQueue = "bvt.policy-probe.queue";
        String policy = "bvt-delayed-retry-probe";
        var resp = mgmt.putPolicyRaw(policy, "^" + probeQueue.replace(".", "\\.") + "$",
                Map.of("delayed-retry-type", "failed", "delayed-retry-min", 1000, "delayed-retry-max", 5000), "quorum_queues");
        try {
            if ("4.3.0".equals(version)) {
                // 4.3.0: validator kayitli degil -> 400 "not recognised policy settings" (yerel broker'da gozlendi)
                assertThat(resp.statusCode()).as("policy route on " + version + ": " + resp.body()).isEqualTo(400);
                assertThat(resp.body()).contains("not recognised policy settings");
                return;
            }
            // Sonraki 4.3.x (CI: 4.3.6): policy kabul edilir. 201/204 yetmez; ARGUMANSIZ bir QQ'da policy'nin gecikmeyi
            // gercekten uyguladigi olculur (reject requeue=true -> ikinci teslim >= 900 ms sonra).
            assertThat(resp.statusCode()).as("policy route on " + version + ": " + resp.body()).isIn(201, 204);
            assertThat(measureRedeliveryGapMs(probeQueue)).as("policy-only delayed retry on " + version).isGreaterThanOrEqualTo(900);
        } finally {
            mgmt.deletePolicy(policy);
        }
    }

    /** Argumansiz quorum queue: 1 mesaj, ilk teslimde basicReject(requeue=true), ikinci teslime kadar gecen sure. */
    private long measureRedeliveryGapMs(String queue) throws Exception {
        try (var conn = cf.createConnection(); var ch = conn.createChannel(false)) {
            ch.queueDelete(queue);
            ch.queueDeclare(queue, true, false, false, Map.of("x-queue-type", "quorum"));
            try {
                // policy uygulanana kadar bekle (effective_policy_definition)
                assertThat(await(Duration.ofSeconds(15), () -> {
                    try {
                        JsonNode def = mgmt.queue(queue).get("effective_policy_definition");
                        return def != null && def.has("delayed-retry-type");
                    } catch (Exception ex) { return false; }
                })).as("policy applied to " + queue).isTrue();
                ch.basicQos(1);
                List<Instant> seen = new CopyOnWriteArrayList<>();
                CountDownLatch two = new CountDownLatch(2);
                ch.basicConsume(queue, false, (tag, d) -> {
                    seen.add(Instant.now());
                    two.countDown();
                    if (seen.size() == 1) ch.basicReject(d.getEnvelope().getDeliveryTag(), true);
                    else ch.basicAck(d.getEnvelope().getDeliveryTag(), false);
                }, tag -> { });
                ch.basicPublish("", queue, null, "probe".getBytes(StandardCharsets.UTF_8));
                assertThat(two.await(20, TimeUnit.SECONDS)).as("second delivery").isTrue();
                return Duration.between(seen.get(0), seen.get(1)).toMillis();
            } finally {
                ch.queueDelete(queue);
            }
        }
    }

    @Test // l: bilinmeyen tip (binding order.order.* ile gelen order.order.created) yok sayilir ve ack'lenir (12.2)
    void l_unknownEventTypeIsIgnoredAndAcked() throws Exception {
        startConsumer(realEffect);
        UUID id = UUID.randomUUID();
        template.send(BrokerTopology.DOMAIN_EVENTS_EXCHANGE, "order.order.created",
                rawMessage(id, "order.order.created", "{\"orderId\":\"" + UUID.randomUUID() + "\"}"));
        assertThat(await(BOUND, () -> attempts.size() == 1)).isTrue();
        assertThat(attempts.get(0).outcome()).isEqualTo(Outcome.IGNORED_TYPE);
        assertThat(attempts.get(0).eventId()).isEqualTo(id);
        assertThat(await(BOUND, () -> admin.getQueueInfo(BrokerTopology.ORDER_CANCELLED_QUEUE).getMessageCount() == 0)).isTrue();
        assertThat(effects()).isZero();
        assertThat(inboxRows()).isZero();
        assertThat(mgmtDepth(BrokerTopology.ORDER_CANCELLED_DLQ)).isZero();
    }

    int mgmtDepth(String queue) {
        try {
            return mgmt.depth(queue);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    boolean connectable() {
        try {
            cf.resetConnection();
            cf.createConnection().close();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
