package com.acme.broker.publish;

import com.acme.platform.messaging.cloudevents.CloudEventHeaders;
import com.acme.platform.messaging.outbox.OutboxEvent;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * outbox_event satiri -> AMQP mesaji. Govde payload JSON'unun kendisidir (CloudEvents "data"), attribute'lar ce-* header'i;
 * messageId = outbox id (tuketici inbox anahtari), delivery mode PERSISTENT (broker yeniden basladiginda kaybolmaz;
 * quorum queue zaten diske yazar ama exchange'de/DLX yolunda kalici olmayan mesaj dusebilir).
 */
public final class CloudEventMessageFactory {

    private final JsonMapper json;
    private final String source;

    /** @param source CloudEvents "source" (orn. "urn:acme:order"); servis basina sabittir. */
    public CloudEventMessageFactory(JsonMapper json, String source) {
        this.json = json;
        this.source = source;
    }

    public Message toMessage(OutboxEvent e) {
        MessageProperties p = new MessageProperties();
        p.setMessageId(e.id().toString());
        p.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        p.setContentEncoding(StandardCharsets.UTF_8.name());
        p.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        p.setTimestamp(java.util.Date.from(e.createdAt()));
        CloudEventHeaders.of(e.id(), e.eventType(), source, e.aggregateId(), e.createdAt(), outboxHeaders(e.headers()))
                .forEach(p::setHeader);
        return new Message(e.payload().getBytes(StandardCharsets.UTF_8), p);
    }

    /** headers JSONB bozuksa (yazici hatasi) trace baglami olmadan yayinlanir; olay kaybolmaz. */
    private Map<String, ?> outboxHeaders(String headersJson) {
        if (headersJson == null || headersJson.isBlank()) return Map.of();
        try {
            return json.readValue(headersJson, Map.class);
        } catch (JacksonException ex) {
            return Map.of();
        }
    }
}
