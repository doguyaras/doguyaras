package com.acme.platform.ratelimit;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.Delay;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate limit icin Lettuce baglantisi. Guvenlik state'i cache Redis'inden AYRI instance'tadir (Bolum 9.6:
 * noeviction + AOF); bu yuzden Spring'in varsayilan RedisConnectionFactory'si yerine ayri, acik ayarli baglanti.
 *
 * <p>Neden bu ayarlar: limiter her istegin sicak yolundadir. Varsayilan Lettuce davranisi kopukken komutlari
 * kuyruga alir ve 60 sn bekletir; bu, "fail-open" scope'u bile fiilen durdurur. Burada:
 * <ul>
 *   <li>kopukken komut aninda reddedilir (REJECT_COMMANDS) -> fail politikasi milisaniyede uygulanir;</li>
 *   <li>yanit vermeyen (asili) Redis icin kisa komut timeout'u -> ayni politika timeout sonunda;</li>
 *   <li>yeniden baglanma ust siniri kisa -> Redis donunce limit hizla yeniden devreye girer.</li>
 * </ul>
 */
public final class RateLimitRedis implements AutoCloseable {

    private final ClientResources resources;
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;

    private RateLimitRedis(ClientResources resources, RedisClient client,
                           StatefulRedisConnection<String, String> connection) {
        this.resources = resources; this.client = client; this.connection = connection;
    }

    /** @param commandTimeout sicak yol butcesi; tipik 50-250 ms. */
    public static RateLimitRedis connect(RedisURI uri, Duration commandTimeout) {
        ClientResources resources = DefaultClientResources.builder()
                .reconnectDelay(Delay.exponential(Duration.ofMillis(20), Duration.ofSeconds(1), 2, TimeUnit.MILLISECONDS))
                .build();
        RedisClient client = RedisClient.create(resources, RedisURI.builder(uri).withTimeout(commandTimeout).build());
        client.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .timeoutOptions(TimeoutOptions.enabled(commandTimeout))
                .socketOptions(SocketOptions.builder().connectTimeout(commandTimeout.multipliedBy(4)).build())
                .build());
        try {
            return new RateLimitRedis(resources, client, client.connect());
        } catch (RuntimeException e) {
            client.shutdown();
            resources.shutdown();
            throw e;
        }
    }

    public StatefulRedisConnection<String, String> connection() { return connection; }

    @Override
    public void close() {
        connection.close();
        client.shutdown();
        resources.shutdown();
    }
}
