# repo-context.md — <proje> Repo Haritası

> Amaç: Ajanın ilk 2 dakikada sistemi anlaması. Kurallar burada değil; `AGENTS.md` ve `security-rules.md`'de. Bu dosya **gerçekleri** taşır ve her yeni servis/uç/olayda güncellenir.

## 1. Mimari şekil (ADR-0001)

- Şekil: **hibrit** — çekirdek domain'ler (`auth`, `user`, `<ana-iş>`, `subscription`) tek uygulama (`core-app`) içinde Spring Modulith modülleri; `chat` (realtime), `notification` (dış sağlayıcılar), `backoffice` (yönetim) ayrı servisler. *(Projeye göre düzenle: modüler monolit / mikroservis.)*
- Yeniden değerlendirme eşiği: referans Bölüm 24 (≥2 ekip, farklı ölçek profili, ikinci host).
- Repo: Maven multi-module monorepo; `platform/*` starter'ları; `services/<domain>/<domain>-api|core`.

## 2. Servis kimlik tablosu

| Servis | Port | `application.name` | Actor / `iss` | Audience | DB schema / rol | Hata kodu bloğu | Yayınladığı olaylar | Tükettiği olaylar | Sıcak yol uzak çağrı sayısı |
|---|---|---|---|---|---|---|---|---|---|
| gateway | 8080 | gateway | gateway | route metadata | – | – | – | – | – |
| core-app (auth, user, order, subscription modülleri) | 8081 | core | core-service | core-api | `auth`,`users`,`order`,`subscription` / `svc_*` | 10000–14999 | `account.*`, `user.*`, `order.*`, `subscription.*` | – | order.create: **1** (subscription consume, in-process değilse) |
| chat | 8094 | chat | chat-service | chat-api | `chat` / `svc_chat` | 15000–15999 | `chat.message.sent` (metadata) | `account.standing.changed`, `user.block.created` | message.send: **0** |
| notification | 8091 | notification | notification-service | notification-api | `notification` / `svc_notification` | 16000–16999 | `notification.delivered` | `*.commands`, `order.order.created`, … | – |
| backoffice | 8095 | backoffice | backoffice-service | backoffice-api | `backoffice` / `svc_backoffice` | 17000–17999 | `parameter.revision.published`, `moderation.action.applied` | `*.report.created` | – |

Ortak kod blokları: validation 90000, security 90100–90199, system 99998–99999.

## 3. Kritik akış kaydı — sıcak yol tablosu (referans Bölüm 1.2)

| Akış | Uç | Gecikme bütçesi (p99) | Uzak senkron bağımlılıklar (gerekçe) | Read-model / claim ile karşılanan kontroller (kabul edilen eskilik) | Bağımlılık düşünce davranış | Yeniden değerlendirme |
|---|---|---|---|---|---|---|
| Ana yazma | `POST /v1/orders` | 300 ms | 1: `subscription.consume` (hak tüketimi = yazma; read-model ile yapılamaz) | hesap durumu (`rm_account_status`, lag ≤ 5 dk), yasal onay (`legal_ok` claim, token ömrü) | subscription: 503 `UPSTREAM_UNAVAILABLE`; read-model satır yok: reddet | p99 > 300 ms 3 gün; `readmodel_lag_seconds{source="auth"}` > 300 |
| Mesaj gönder | `POST /v1/conversations/{id}/messages` | 150 ms | 0 | engel (`rm_block_relation`, lag ≤ 30 sn; aşılırsa fail-closed), üyelik (local) | – | lag alarmı |
| Giriş | `POST /v1/auth/password/login` | 500 ms | 0 | – | – | – |

Varsayılan: ≤1 uzak senkron çağrı. Aşan satır ADR + `proj-resilience-review` ister; kayıt alanlarından biri boş olan satır `REQUEST CHANGES`.

## 3.1 Delegasyon matrisi (referans Bölüm 9.2.1)

Her internal uç için bir satır. Hedef servis üçünü birlikte kontrol eder: çağıran allowlist'te mi, bu işlem için mi, `sub` varsa bu kaynakta yetkili mi.

| Çağıran (`act`) | Hedef işlem | Kullanıcı bağlamı (`sub`) | Kaynak yetkisi kontrolü | Bağlam kaynağı | Ele geçirilirse zarar |
|---|---|---|---|---|---|
| gateway | tüm public uçlar | zorunlu (user JWT) | hedef: ownership | kullanıcı isteği | tüm kullanıcı işlemleri |
| core-service (order) | `subscription: consume/confirm/compensate` | zorunlu; yalnız kendi sipariş akışındaki hesap | `operation_key` + hesap eşleşmesi | kullanıcı isteği (senkron) | yalnız hak tüketimi |
| core-service (worker) | `notification: commands` | yok (arka plan token'ı) | – | outbox | spam → rate limit |
| backoffice-service | `user: moderate` | yok; admin id ayrı claim | admin rolü + audit | panel isteği | moderasyon kararları |

Arka plan token'ıyla kullanıcı-yetkisi gerektiren işlem kabul edilmez; "her kullanıcı adına her şey" satırı yoktur.

## 4. Yüksek sinyalli dosyalar

| Konu | Dosya |
|---|---|
| Servis JWT doğrulama | `platform/platform-security/.../jwt/ServiceJwtVerificationFilter.java` |
| Allowlist config | `services/<svc>/<svc>-core/src/main/resources/config/<svc>.yml` → `service-jwt.internal-access` |
| Outbox | `platform/platform-messaging/.../outbox/{OutboxPoller,OutboxHandler,OutboxWriter}.java`; servis handler'ları `services/<svc>/.../outbox/` |
| Event envelope | `platform/platform-messaging/.../event/CloudEventEnvelope.java` |
| Saga | `platform/platform-messaging/.../consistency/LocalSagaStore.java` |
| Read-model'ler | `services/<svc>/.../readmodel/` |
| Global exception handler | `platform/platform-core/.../handler/GlobalServiceExceptionHandler.java` |
| Log sanitizer | `platform/platform-observability/.../logging/SensitiveLogSanitizer.java` |
| Parametre provider | `services/<svc>/.../service/SystemParameterProvider.java` |
| Gateway route'ları | `gateway/src/main/resources/config/gateway.yml` |
| Migration'lar | `services/<svc>/<svc>-core/src/main/resources/db/migration/` |
| Deploy | `.github/workflows/{ci,build-images,deploy}.yml`, `deploy/docker-compose.yml` |
| Obs | `obs/{alloy,prometheus,alert-rules,alertmanager,loki,tempo,grafana}` |

## 5. Altyapı

| Bileşen | Sürüm | Not |
|---|---|---|
| Java / Spring Boot / Spring Cloud | 25 / 4.1.x / 2025.1.x (Boot 4.0+4.1) | OSS destek kontrolü: `docs/versions.md` (tarihli; referans Ek A/B) |
| PostgreSQL | 18 | tek instance, şema+rol/servis, PgBouncer, WAL-G → S3 |
| Valkey | 9 ×2 | `security` (noeviction+AOF, Sentinel) / `cache` (allkeys-lru) |
| RabbitMQ | 4.3 | quorum queue; `domain.events` topic; streams: `domain.events.stream` |
| Object storage | S3 uyumlu | `quarantine`, `delivery` (private), signed GET |
| Gözlem | Alloy → Loki/Tempo; Prometheus + Alertmanager; Grafana | portlar yalnız 127.0.0.1 |

## 6. Komutlar

```bash
mvn -B -ntp verify -pl services/order/order-core -amd      # değişen modül + bağımlıları
node scripts/flyway-immutability.js check --base origin/develop
npm --prefix <panel>-web run lint && npm --prefix <panel>-web run test
docker compose -f deploy/docker-compose.local.yml up -d     # local altyapı
```

## 7. Bu Belgede Özellikle Taşınmayanlar

Secret değerleri, üretim host adları, CI token'ları, kişisel veri örnekleri.

## 8. Net Kanıt Bulunamayan Alanlar

- (ajan doğrulayamadığı iddiaları buraya ekler; boşsa "yok")
