package com.acme.broker.consume;

import com.acme.platform.messaging.cloudevents.CloudEventHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Broker'dan gelen mesajin cozulmus hali. Cozme HATASI kalici hatadir (zehirli mesaj): yeniden teslim ayni sonucu verir,
 * bu yuzden MalformedEventException DLQ'ya gider, retry'a degil. Bilinmeyen alanlar yok sayilir (Bolum 12.2).
 *
 * @param deliveryCount broker'in x-delivery-count'u (ilk teslimde header yok -> 0); gozlem/metrik icindir
 */
public record IncomingEvent(UUID id, String type, String source, JsonNode data, int deliveryCount, boolean redelivered,
                            long deliveryTag) {

    public static IncomingEvent decode(Message message, JsonMapper json) {
        MessageProperties p = message.getMessageProperties();
        Map<String, Object> headers = p.getHeaders();
        UUID id = CloudEventHeaders.id(headers)
                .or(() -> parseUuid(p.getMessageId()))
                .orElseThrow(() -> new MalformedEventException("missing or invalid ce-id/messageId"));
        String type = CloudEventHeaders.text(headers, CloudEventHeaders.TYPE)
                .orElseThrow(() -> new MalformedEventException("missing ce-type"));
        String source = CloudEventHeaders.text(headers, CloudEventHeaders.SOURCE).orElse("");
        JsonNode data;
        try {
            data = json.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
        } catch (JacksonException e) {
            throw new MalformedEventException("payload is not JSON");
        }
        if (data == null || !data.isObject()) throw new MalformedEventException("payload is not a JSON object");
        Object count = headers.get("x-delivery-count");
        int deliveryCount = count instanceof Number n ? n.intValue() : 0;
        return new IncomingEvent(id, type, source, data, deliveryCount, Boolean.TRUE.equals(p.isRedelivered()),
                p.getDeliveryTag());
    }

    private static java.util.Optional<UUID> parseUuid(String s) {
        if (s == null) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }
}
