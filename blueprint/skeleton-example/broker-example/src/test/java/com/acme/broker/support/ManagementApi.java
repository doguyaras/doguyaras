package com.acme.broker.support;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * RabbitMQ management HTTP API (test kaniti): kuyruk derinligi, kuyruk bilgisi, policy yonetimi. Uretimde policy'ler
 * deploy/IaC ile verilir; testte ayni API ile kurulur/kaldirilir. Istatistik alanlari (messages) 5 sn'ye kadar gecikebilir;
 * cagiran bounded polling yapar.
 */
public final class ManagementApi {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final JsonMapper json = JsonMapper.builder().build();
    private final String base;
    private final String auth;
    private final String vhost;

    public ManagementApi(String base, String user, String password, String vhost) {
        this.base = base;
        this.auth = "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.vhost = URLEncoder.encode(vhost, StandardCharsets.UTF_8);
    }

    public JsonNode queue(String name) throws IOException, InterruptedException {
        HttpResponse<String> r = send(HttpRequest.newBuilder(URI.create(base + "/api/queues/" + vhost + "/" + enc(name)))
                .header("Authorization", auth).GET());
        if (r.statusCode() != 200) throw new IOException("GET queue " + name + " -> " + r.statusCode() + " " + r.body());
        return json.readTree(r.body());
    }

    /** "messages" toplam (ready + unacked); alan henuz yoksa (yeni kuyruk) 0. */
    public int depth(String name) throws IOException, InterruptedException {
        JsonNode q = queue(name);
        return q.has("messages") ? q.get("messages").asInt() : 0;
    }

    public int putPolicy(String name, String pattern, Map<String, Object> definition, String applyTo)
            throws IOException, InterruptedException {
        String body = json.writeValueAsString(Map.of("pattern", pattern, "definition", definition, "apply-to", applyTo));
        HttpResponse<String> r = send(HttpRequest.newBuilder(URI.create(base + "/api/policies/" + vhost + "/" + enc(name)))
                .header("Authorization", auth).header("content-type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body)));
        return r.statusCode();
    }

    /** Hata govdesini de dondurur (400 nedeni kanittir). */
    public HttpResponse<String> putPolicyRaw(String name, String pattern, Map<String, Object> definition, String applyTo)
            throws IOException, InterruptedException {
        String body = json.writeValueAsString(Map.of("pattern", pattern, "definition", definition, "apply-to", applyTo));
        return send(HttpRequest.newBuilder(URI.create(base + "/api/policies/" + vhost + "/" + enc(name)))
                .header("Authorization", auth).header("content-type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body)));
    }

    public int deletePolicy(String name) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(URI.create(base + "/api/policies/" + vhost + "/" + enc(name)))
                .header("Authorization", auth).DELETE()).statusCode();
    }

    /** Broker surumu (/api/overview rabbitmq_version), orn. "4.3.6". Surume bagli davranis testlerinde kullanilir. */
    public String brokerVersion() throws IOException, InterruptedException {
        HttpResponse<String> r = send(HttpRequest.newBuilder(URI.create(base + "/api/overview")).header("Authorization", auth).GET());
        if (r.statusCode() != 200) throw new IOException("GET overview -> " + r.statusCode());
        return json.readTree(r.body()).get("rabbitmq_version").asString();
    }

    public boolean alive() {
        try {
            return send(HttpRequest.newBuilder(URI.create(base + "/api/overview")).header("Authorization", auth).GET())
                    .statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private HttpResponse<String> send(HttpRequest.Builder b) throws IOException, InterruptedException {
        return http.send(b.timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
