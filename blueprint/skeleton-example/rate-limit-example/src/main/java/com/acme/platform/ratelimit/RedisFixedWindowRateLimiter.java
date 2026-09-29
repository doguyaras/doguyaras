package com.acme.platform.ratelimit;

import io.lettuce.core.RedisException;
import io.lettuce.core.RedisNoScriptException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis uzerinde atomik fixed-window rate limiter (referans Bolum 9.6).
 *
 * <ul>
 *   <li>Sayac YALNIZ Redis'tedir (Bolum 22: JVM sayaci yasak). N replika ayni limiti paylasir; Redis yoksa bu
 *       sinif "yerel tahmin" yapmaz, scope'un fail politikasini uygular.</li>
 *   <li>Tek Lua script (INCRBY + TTL yoksa EXPIRE) EVALSHA ile calisir: govde bir kez yuklenir, her istekte
 *       yalniz 40 byte'lik SHA gider. Redis yeniden baslayip script cache'i bosalirsa (NOSCRIPT) bir kez yeniden
 *       yuklenip tekrar denenir.</li>
 *   <li>Key {@code rl:<scope>:<sha256(scope \0 ozne)>}: telefon/IP gibi ham ozne Redis'e (ve MONITOR/slowlog'a)
 *       yazilmaz; scope hash'e katildigi icin ayni ozne farkli scope'larda iliskilendirilemez.</li>
 * </ul>
 *
 * Baglanti {@link RateLimitRedis#connect} ile kurulmalidir: kisa komut timeout'u ve kopukken komut reddi olmadan
 * Redis kesintisi istek thread'lerini bekletir, fail politikasi ise ancak timeout dolunca devreye girer.
 */
public final class RedisFixedWindowRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisFixedWindowRateLimiter.class);

    static final String SCRIPT = loadScript();
    static final String SCRIPT_SHA = sha1Hex(SCRIPT);

    private final StatefulRedisConnection<String, String> connection;
    private final RateLimitRules rules;
    private final Map<String, Counter> failOpen;
    private final Map<String, Counter> failClosed;
    private final Counter ruleMissing;
    /** Yalniz log gurultusunu kesmek icin (her istekte WARN degil, durum degisiminde bir kez). Sayac degildir. */
    private final AtomicBoolean storeHealthy = new AtomicBoolean(true);

    public RedisFixedWindowRateLimiter(StatefulRedisConnection<String, String> connection, RateLimitRules rules,
                                       MeterRegistry registry) {
        this.connection = connection;
        this.rules = rules;
        // Sayaclar acilista 0 ile kayitlanir: alarm kurali (increase(...) > 0) serinin ilk hatada dogmasini
        // beklemez, ilk kesinti kacmaz.
        this.failOpen = counters(registry, "rate_limit.fail_open");
        this.failClosed = counters(registry, "rate_limit.fail_closed");
        this.ruleMissing = Counter.builder("rate_limit.rule_missing").register(registry);
        loadScriptQuietly();
    }

    /** Tek istek = maliyet 1. */
    public RateLimitDecision tryAcquire(String scope, String subject) { return tryAcquire(scope, subject, 1); }

    public RateLimitDecision tryAcquire(String scope, String subject, int cost) {
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("ozne bos olamaz: " + scope);
        if (cost < 1) throw new IllegalArgumentException("maliyet >= 1 olmali");
        RateLimitRule rule = rules.find(scope).orElse(null);
        if (rule == null) {
            // Kural yok = config hatasi. Limitsiz gecirmek yerine 503: test/staging'de hemen gorunur.
            ruleMissing.increment();
            log.error("rate limit rule missing scope={} code={}", scope,
                    RateLimitErrorCode.RATE_LIMIT_RULE_MISSING.getCode());
            return RateLimitDecision.ruleMissing();
        }
        List<Object> reply;
        try {
            reply = evalCounter(key(scope, subject), rule.windowSeconds(), cost);
        } catch (RedisException e) {
            return onStoreFailure(rule, e);
        }
        markHealthy();
        long count = (Long) reply.get(0);
        long pttlMillis = (Long) reply.get(1);
        if (count <= rule.limit()) {
            return new RateLimitDecision(RateLimitDecision.Outcome.ALLOWED, rule.limit() - count, 0);
        }
        // Retry-After tam saniye; PTTL yukari yuvarlanir ki istemci pencere bitmeden donmesin, en az 1.
        long retryAfter = Math.max(1, (pttlMillis + 999) / 1000);
        return new RateLimitDecision(RateLimitDecision.Outcome.EXCEEDED, 0, retryAfter);
    }

    /** Key formati tek yerde: servisler kendi formatini kopyalamaz (Bolum 22: paylasilan key formati kopyasi). */
    public static String key(String scope, String subject) {
        return "rl:" + scope + ":" + sha256Hex(scope + '\0' + subject);
    }

    private List<Object> evalCounter(String key, int windowSeconds, int cost) {
        String[] keys = { key };
        String window = Integer.toString(windowSeconds);
        String c = Integer.toString(cost);
        try {
            return connection.sync().evalsha(SCRIPT_SHA, ScriptOutputType.MULTI, keys, window, c);
        } catch (RedisNoScriptException e) {
            // Redis yeniden basladi / failover: script cache bos. Yukle ve bir kez tekrar dene. NOSCRIPT'te
            // script hic calismamistir, tekrar deneme cift sayim yapmaz.
            connection.sync().scriptLoad(SCRIPT);
            return connection.sync().evalsha(SCRIPT_SHA, ScriptOutputType.MULTI, keys, window, c);
        }
    }

    private RateLimitDecision onStoreFailure(RateLimitRule rule, RedisException e) {
        if (storeHealthy.compareAndSet(true, false)) {
            // Ham exception mesaji host/port icerebilir; yalniz sinif adi (Bolum 7.4).
            log.warn("rate limit store unavailable scope={} policy={} reason={}", rule.scope(), rule.failPolicy(),
                    e.getClass().getSimpleName());
        }
        if (rule.failPolicy() == FailPolicy.OPEN) {
            failOpen.get(rule.scope()).increment();
            return RateLimitDecision.failOpen();
        }
        failClosed.get(rule.scope()).increment();
        return RateLimitDecision.unavailable();
    }

    private void markHealthy() {
        if (storeHealthy.compareAndSet(false, true)) log.info("rate limit store available again");
    }

    private void loadScriptQuietly() {
        try {
            String sha = connection.sync().scriptLoad(SCRIPT);
            if (!SCRIPT_SHA.equals(sha)) throw new IllegalStateException("script SHA uyusmuyor: " + sha);
        } catch (RedisException e) {
            // Acilista Redis yoksa uygulama yine kalkar; ilk istekte NOSCRIPT yolu yukler.
            log.warn("rate limit script not preloaded reason={}", e.getClass().getSimpleName());
        }
    }

    private Map<String, Counter> counters(MeterRegistry registry, String name) {
        return rules.all().stream().collect(Collectors.toUnmodifiableMap(RateLimitRule::scope,
                r -> Counter.builder(name).tag("scope", r.scope()).register(registry)));
    }

    private static String loadScript() {
        try (InputStream in = RedisFixedWindowRateLimiter.class.getResourceAsStream("/ratelimit/fixed_window.lua")) {
            if (in == null) throw new IllegalStateException("/ratelimit/fixed_window.lua bulunamadi");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String sha256Hex(String s) { return hex("SHA-256", s); }

    private static String sha1Hex(String s) { return hex("SHA-1", s); }

    private static String hex(String algorithm, String s) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance(algorithm).digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
