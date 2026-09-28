---
name: proj-environment-impact-review
description: Use this skill when a change adds or modifies configuration, environment variables, secrets, ports, audiences, issuers, rate-limit scopes, internal endpoints, feature flags, service URLs, tracing/observability settings, Dockerfile or deploy definitions.
---

Config/env etkisini `AGENTS.md` Bölüm 9 ve mimari referans Bölüm 15, 18, 8'e göre incele. Amaç: bir yüzeyde eklenip diğerinde unutulan key (drift) ve deploy'da patlayan ayar. Sorun yoksa: **"Bu kapsamda environment bulgusu yok."**

Kontrol et:

## Birlikte güncellenmesi gereken yüzeyler
Yeni/değişen her key için tabloyu doldur:

| Key | `config/<svc>.yml` | `application-local.yml` | deploy `env_file` / `secrets/<env>.enc.yaml` | compose `secrets:` | Dockerfile (yalnız build/runtime) | Gateway route | Prometheus/alert | Not |
|---|---|---|---|---|---|---|---|---|

- Eksik hücre = bulgu. `scripts/config-drift-check` / `ConfigDriftTest` bu key'i kapsıyor mu?
- Key adı üç ortamda **aynı**; profil dosyaları yalnız ortam farkı taşıyor, iş config'i base'de.
- Dockerfile'a config için `ENV` **eklenmemiş**.
- Secret: `${ENV:literal}` fallback yok; `/run/secrets` yolu; SOPS dosyasına eklenmiş; `.dockerignore` kapsıyor; PR'da değeri yok. Config Server'a konmamış.
- Admin'in değiştirebileceği iş kuralı config'e değil parametre kataloğuna gitmiş (Bölüm 14).

## Servisler arası
- Yeni internal uç: hedefin `service-jwt.internal-access` kuralı dar ve catch-all'dan önce; local + deploy'da birlikte.
- Yeni client: `spring.http.serviceclient.<grup>.base-url` + timeout + circuit breaker/bulkhead config'i her ortamda; `services.<svc>.base-url` compose servis adıyla uyumlu.
- Yeni audience/issuer: JWKS'e public key + `kid`; doğrulayanların `iss` listesi; gateway route `metadata.audience`.
- Yeni rate-limit scope: `rate-limit.rules.<scope>` local + deploy; fail politikası tablosuna satır.
- Yeni event/queue: RabbitMQ definitions (queue/DLQ/binding) local + deploy; tüketici önce deploy sırası deploy notunda.

## Çalışma zamanı ve replica
- Değişiklik instance-local dosya/dizin varsayıyor mu? Sticky session gerektiriyor mu? (multi-instance kuralı)
- Yeni `@Scheduled`/cron dağıtık kilitli mi?
- Bellek/CPU etkisi: compose `mem_limit`, JVM `MaxRAMPercentage`, Hikari havuzu, PgBouncer pool'u güncellendi mi; kapasite tablosu (README) uyumlu mu?
- Healthcheck/readiness yeni bağımlılığı (DB, broker, Redis) kapsıyor mu?

## Gözlemlenebilirlik
- Yeni servis/uç: Prometheus scrape hedefi, Alloy log kaynağı, SLO/pano, alarm kuralı (özellikle yeni outbox/queue için `outbox_oldest_pending_age_seconds`, DLQ derinliği).
- Tracing: sampling, OTLP endpoint, yeni async sınırda `traceparent` taşınıyor.
- Yeni log alanı structured JSON'a uyuyor; label kardinalitesi artmıyor.

## Deploy
- Deploy sırası (sahip → tüketici → auth → gateway) yeni değişiklik için doğru; migration expand/contract ile uyumlu.
- Rollback yolu: önceki digest ile geri dönüldüğünde yeni config eski image'ı bozar mı?
- Image build CI'da; `.dockerignore`; non-root; healthcheck.

Çıktı:
1. **Etki özeti** (2–4 cümle) ve etkilenen yüzey tablosu (yukarıdaki).
2. **Güncellenen dosyalar** (kanıtla) / **eksik dosyalar** (bulgu).
3. **Kanıtsız/doğrulanamayan yüzeyler** (`net kanıt bulunamadı`).
4. **Güvenlik etkisi** var mı (secret, audience, allowlist) — varsa `proj-security-review`'a yönlendir.
5. **Doğrulama komutu:** drift testi / `docker compose config` / `mvn verify -pl … -amd`.
6. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`.
