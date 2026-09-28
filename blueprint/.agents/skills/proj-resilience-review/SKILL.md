---
name: proj-resilience-review
description: Use this skill when a change adds or modifies a remote synchronous call, a hot-path flow, timeouts, retries, circuit breakers, bulkheads, thread/connection pools, caches with fallback, or any control-plane dependency (parameters, config, flags).
---

Dayanıklılığı mimari referans Bölüm 1.2, 4.6, 4.7, 7.2/14.3 ve `AGENTS.md` Bölüm 5'e göre incele. Temel soru: **"Bu bağımlılık yavaşlar veya düşerse kullanıcı ne görür, servis ne yapar?"** Her uzak bağımlılık için bu cevap yazılı olmalı. Sorun yoksa: **"Bu kapsamda dayanıklılık bulgusu yok."**

Kontrol et:

## Sıcak yol
- Değişen akış sıcak yolda mı (`docs/ai/repo-context.md` Bölüm 3)? Uzak senkron çağrı sayısı **≤ 1** mi? Artıyorsa `REQUEST CHANGES`: read-model / JWT claim / asenkron alternatif ve maliyeti yazılır; tablo güncellenir.
- Tek uzak çağrı yazma/rezervasyon türü mü (okuma değil)?
- Availability aritmetiği: bağımlı bileşen sayısı × %99,9 → beklenen üst sınır; p99 toplamı hedefle karşılaştırılmış.

## Timeout bütçesi
- Gateway toplam bütçesi > client read timeout > downstream'in kendi downstream timeout'u (bütçe zinciri tutarlı).
- Her client grubunda `connect-timeout` ve `read-timeout` açıkça set; varsayılan/sonsuz timeout yok.
- Uzun işlemler (export, toplu işlem) senkron uçta değil; job + poll.

## Circuit breaker / bulkhead / retry
- Hedef başına `resilience4j.circuitbreaker.instances.<hedef>` ve `bulkhead` tanımlı; açıkken tanımlı `ServiceException` (503 `UPSTREAM_UNAVAILABLE`) ve fallback davranışı belgeli.
- Spring Cloud CircuitBreaker kullanılıyorsa **TimeLimiter (varsayılan 1 sn)** ve thread-pool bulkhead ayarlanmış/kapatılmış.
- Senkron yolda retry **yok** (`NEVER_RETRY`); retry yalnız outbox/saga worker'ında, backoff ile, idempotent hedefe.
- Cascading failure hesabı: Tomcat/virtual thread modeli, Hikari havuzu, downstream timeout × istek hızı → havuz doluyor mu?

## Fail politikası
- Yeni bağımlılık için fail-open/fail-closed kararı yazılı ve README tablosuyla uyumlu (güvenlik → closed; iş → open + metrik).
- Control-plane bağımlılığı (parametre, config, flag): bounded-staleness (son bilinen değer + disk snapshot + `*_staleness_seconds` metriği + T eşiği); "5 sn cache + 503" **yok**.
- Redis güvenlik state'i ile cache ayrı instance; eviction politikası doğru.
- Read-model "satır yok/eski" davranışı tanımlı.

## Kapasite ve kaynak
- Yeni thread pool/executor sınırlı ve isimli; virtual thread'lerle pinning riski (`synchronized` + IO) yok.
- Hikari `maximum-pool-size` × instance ≤ PgBouncer/`max_connections` bütçesi.
- Bellek: yeni cache'in üst sınırı ve TTL'i var.
- WebSocket/uzun bağlantı sayısı pod başına sınırlı; presence/fan-out Redis'te.

## Gözlem ve test
- Yeni bağımlılık için metrik (`circuitbreaker_state`, latency histogram) ve alarm.
- "Hedef 30 sn yanıt vermiyor" testi: çağıranın p99 bütçeyi aşmıyor, circuit açılıyor, health `DEGRADED`.
- Yük testi senaryosu güncellendi mi (sıcak yol değiştiyse).
- Runbook: bağımlılık düştüğünde ne yapılır (Bölüm 8.8).

Çıktı:
1. **Bağımlılık tablosu:** her uzak çağrı → sıcak yol mu · timeout · CB/bulkhead · fail politikası · düşünce kullanıcı ne görür.
2. **Bulgular:** `severity · dosya:satır/config key · kanıt · düzeltme`.
3. **Sıcak yol sayısı:** önce/sonra; tablo güncellendi mi.
4. **Eksik testler/alarmlar.**
5. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`. Sıcak yolda ikinci senkron okuma → en az `REQUEST CHANGES`.
