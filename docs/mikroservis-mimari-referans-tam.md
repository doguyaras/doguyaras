# Mikroservis Mimari Referansı — TAM SÜRÜM (referans + blueprint dosyaları)

> **Nasıl kullanılır:** Bu tek dosyayı başka bir projenin `docs/` klasörüne koyup bir AI ajanına "**bu dokümanı referans alarak projeyi düzenle**" de. Ajan **Bölüm 26 (Uygulama Protokolü)** ile başlar: önce keşif ve uyum raporu, sonra F0→F6 sırasıyla küçük PR'lar. Ek A'daki dosyalar (`AGENTS.md`, `docs/ai/*`, skill'ler, hook'lar, script'ler, testler) repoya birebir kopyalanır; Ek B çalışan bir iskelet örneğidir.

> İki parça: **Referans** (Bölüm 1–26) ve **Ek A/B** (blueprint dosyaları). Tek kaynak repodaki `docs/mikroservis-mimari-referans.md` ve `blueprint/`; bu dosya `python3 scripts/build-blueprint-doc.py` ile üretilir.

---

# Referans

> **Amaç:** Java/Spring Boot tabanlı, çok modüllü (monorepo) bir mikroservis backend'inin **nasıl inşa edileceğini** anlatır. Kapsam: mimari, katmanlar, paketleme, isimlendirme, teknoloji seçimleri, exception ve log yönetimi, güvenlik, veri tutarlılığı, konfigürasyon, test, CI/CD ve AI destekli geliştirme altyapısı. Yeni bir proje tasarlarken şablon olarak kullanılabilir.
>
> **Yer tutucular:**
> - `<org>`: paket kökü (örn. `com.acme`).
> - `<proje>`: proje adı.
> - `<servis>` / `<domain>`: servis adı (örn. `order`).
>
> Örnekler nötr bir `order` domain'i üzerinden verilmiştir.
>
> **Okuma işaretleri:**
> - **Kural:** Uygulanması gereken standart.
> - **Neden:** Gerekçe.
> - **Kaçın:** Kopyalanmaması gereken anti-pattern.
> - **Eşik:** Bir kararın hangi ölçekte/koşulda değişmesi gerektiği (bkz. Bölüm 24).
>
> **Sürüm notu:** Sürüm numaraları ve destek tarihleri **2026-09** itibarıyla geçerlidir. Yeni projeye başlarken Bölüm 25'teki kaynaklardan güncel OSS destek durumu kontrol edilir; destek dışı bir sürümle başlanmaz.
>
> **Bu revizyon (v2.1):** Gerçek bir projenin (9 servis, tek host, tek PostgreSQL) mimari değerlendirmesinden çıkan dersler işlendi: mimari şekil kararı (Bölüm 1.1), uygulama profilleri MVP/Büyüme/Ölçek (Bölüm 1.3), sıcak yolda senkron zincir yasağı ve read-model replikasyonu (Bölüm 4.6), dayanıklılık (Bölüm 4.7), domain event akışı ve Kafka karar kriterleri (Bölüm 12), asimetrik servis kimliği (Bölüm 9.2), güvenlik süreci (Bölüm 9.10), secret yönetimi (Bölüm 15.3), yedekleme/HA (Bölüm 10.5), Alloy/structured logging/SLO ve runbook/olay yönetimi (Bölüm 8), ölçek eşikleri (Bölüm 24), sürüm takibi (Bölüm 25), proje başlangıç checklist'i (Bölüm 21.0) ve **kopyalanabilir `blueprint/` klasörü** (AGENTS.md, `docs/ai/*`, 12 skill, hook'lar, script'ler, ArchUnit/ErrorCode/config-drift testleri — Bölüm 19).
>
> **Kapsam dışı (bilinçli):** çok bölgeli (multi-region) aktif-aktif mimari, platform/SRE ekibi olan 10+ ekipli organizasyonlar, service mesh, Spring dışı ekosistemler (desenler taşınır, şablonlar taşınmaz), veri ambarı/ML platformu tasarımı. Bunlar için Bölüm 24'teki eşikler tutunca ayrı ADR gerekir.

---

## İçindekiler

1. [Mimari Özet](#1-mimari-özet)
2. [Teknoloji Yığını](#2-teknoloji-yığını)
3. [Repo ve Modül Topolojisi](#3-repo-ve-modül-topolojisi)
4. [Servis İçi Katmanlı Mimari](#4-servis-içi-katmanlı-mimari)
5. [İsimlendirme Konvansiyonları](#5-isimlendirme-konvansiyonları)
6. [API Tasarımı ve Contract Kuralları](#6-api-tasarımı-ve-contract-kuralları)
7. [Exception ve Hata Yönetimi](#7-exception-ve-hata-yönetimi)
8. [Loglama ve Gözlemlenebilirlik](#8-loglama-ve-gözlemlenebilirlik)
9. [Güvenlik Mimarisi](#9-güvenlik-mimarisi)
10. [Veri Katmanı: PostgreSQL, Flyway, JPA](#10-veri-katmanı-postgresql-flyway-jpa)
11. [Dağıtık Tutarlılık: Outbox, Inbox, Saga, Multi-Instance](#11-dağıtık-tutarlılık-outbox-inbox-saga-multi-instance)
12. [Mesajlaşma: RabbitMQ](#12-mesajlaşma-rabbitmq)
13. [Realtime: WebSocket/STOMP ve Redis Fan-out](#13-realtime-websocketstomp-ve-redis-fan-out)
14. [Dinamik İş Parametreleri](#14-dinamik-iş-parametreleri)
15. [Konfigürasyon Yönetimi](#15-konfigürasyon-yönetimi)
16. [Test Stratejisi](#16-test-stratejisi)
17. [Yönetim Paneli (Frontend) Mimarisi](#17-yönetim-paneli-frontend-mimarisi)
18. [Build, Container, CI/CD ve Deploy](#18-build-container-cicd-ve-deploy)
19. [AI Destekli Geliştirme Altyapısı](#19-ai-destekli-geliştirme-altyapısı)
20. [Geliştirme Süreci](#20-geliştirme-süreci)
21. [Adım Adım Checklist'ler](#21-adım-adım-checklistler)
22. [Kaçınılacak Anti-Pattern'ler](#22-kaçınılacak-anti-patternler)
23. [Kod Şablonları](#23-kod-şablonları)
24. [Ölçek Eşikleri ve Evrim Yolu](#24-ölçek-eşikleri-ve-evrim-yolu)
25. [Sürüm ve Destek Takibi](#25-sürüm-ve-destek-takibi)
26. [Mevcut Bir Projeye Uygulama Protokolü](#26-mevcut-bir-projeye-uygulama-protokolü)

Ek dosyalar: `blueprint/` (kopyalanabilir AGENTS.md, `docs/ai/*`, 12 skill, hook'lar, script'ler, test şablonları) · `docs/mikroservis-blueprint-dosyalari.md` (aynı içerik tek dosyada).

---

## 1. Mimari Özet

| Boyut | Karar |
|---|---|
| Stil | Domain sınırları `*-api` (contract) + `*-core` (uygulama) modül çiftleriyle çizilir; tek Maven multi-module repo (monorepo). Bu sınırlar **hem modüler monolit hem mikroservis** olarak deploy edilebilir; hangisi olacağı Bölüm 1.1'deki karara bağlıdır. |
| Giriş noktası | Spring Cloud Gateway (reaktif). Kullanıcı JWT'sini doğrular ve her istek için kısa ömürlü, **asimetrik imzalı** bir **service JWT** üretir (token exchange). |
| Senkron iletişim | Servis-servis HTTP (OpenFeign veya Spring HTTP Service Clients). Gateway atlanır. Service JWT + actor allowlist ile korunur. **Sıcak yolda en fazla bir uzak senkron çağrı** (Bölüm 4.6). |
| Asenkron iletişim | Tek generic transactional outbox → **domain event'leri** (RabbitMQ topic exchange) ve komutlar (queue). Diğer servislerin verisi **read-model** olarak replike edilir. Realtime için Redis pub/sub + history pull. |
| Kimlik | Üç ayrı JWT yüzeyi var: **user**, **service**, **admin**. `/internal/**` uçları varsayılan olarak reddedilir (default-deny). Servis başına anahtar çifti, JWKS ile doğrulama. |
| Veri | Tek PostgreSQL, **her servise ayrı schema ve ayrı DB rolü** (GRANT ile zorlanır). Cross-schema FK ve join yasak. Flyway ile servis başına migration, `ddl-auto: validate`. Yedek + PITR + restore provası ilk günden. |
| Paylaşılan altyapı | Valkey/Redis (güvenlik state'i ve cache **ayrı instance**), RabbitMQ 4.x (quorum queue + streams), PostGIS (geo) ve gerekirse OpenSearch, S3 uyumlu object storage (private bucket + signed URL) |
| Tutarlılık | Outbox + `FOR UPDATE SKIP LOCKED` + lease + `claim_token`. Idempotent consumer (inbox). Geri alınabilir uzak işlemler için local saga. |
| Konfigürasyon | Compose `env_file` + Spring config tree (`/run/secrets`); secret'lar git'te SOPS ile şifreli. Config Server **opsiyonel** ve secret taşımaz. Adminin değiştirebildiği iş kuralları için **bounded-staleness** cache'li dinamik parametre kataloğu. |
| Dayanıklılık | Her hop'ta timeout bütçesi, bağımlılık başına circuit breaker + bulkhead, virtual thread'ler (Java 25). Control-plane (parametre, config) düşünce data-plane son bilinen değerle çalışır (static stability). |
| Gözlemlenebilirlik | Micrometer Tracing (OTel) → Alloy (tail sampling) → Tempo. Prometheus + Alertmanager + Grafana. Structured (JSON) log → Alloy → Loki. W3C trace context asenkron sınırlar boyunca taşınır. SLO + burn-rate alarmı. |
| Build/deploy | Affected-module CI, image CI'da build → registry → digest ile deploy, cosign imza. Prod sunucusunda build **yapılmaz**. |
| AI yönetişimi | `AGENTS.md` + `docs/ai/*` kanonik kural seti, proje skill'leri; kritik kurallar ArchUnit/enforcer/hook/CI ile **makineye** bağlanır |

### 1.1 Mimari Şekil Kararı: Modüler Monolit mi, Mikroservis mi, Hibrit mi?

Bu doküman "mikroservis referansı" ama ilk karar **kaç deploy birimi** olacağıdır. Mikroservisin gerçek faydası üçtür: **bağımsız deploy, bağımsız ölçek, hata izolasyonu**. Bu üçü alınmıyorsa (tek host, tek DB, birlikte deploy, senkron zincirler) sistem "dağıtık monolit"tir: mikroservisin maliyetini (ağ, kimlik, outbox, N deploy, N Flyway) öder, faydasını almaz.

| Şekil | Ne zaman | Nasıl |
|---|---|---|
| **A. Modüler monolit** | Ekip ≤ 5 kişi, tek host, ölçek profili homojen, ürün henüz kanıtlanmamış | Aynı `*-api`/`*-core` sınırları tek Spring Boot uygulamasında **Spring Modulith** modülleri olur. Şema/modül ayrımı, `ApplicationModules.verify()` + ArchUnit ile zorlanır. Servisler arası çağrı in-process; outbox yerine Modulith event publication registry. Gateway ve service JWT gereksiz. Bu dokümanın kalan bölümleri (sınırlar, outbox, idempotency, güvenlik, veri, log) **aynen** geçerli kalır. |
| **B. Mikroservis** | Birden fazla ekip, ayrı release kadansı, farklı ölçek profilleri (örn. WebSocket yoğun bir servis), ayrı hata izolasyonu ihtiyacı | Bu dokümanın tamamı. Sıcak yolda senkron zincir **yok** (Bölüm 4.6); diğer servisin verisi event ile replike edilir. |
| **C. Hibrit** (çoğu proje için önerilen başlangıç) | Küçük ekip ama ölçek profili farklı 1–2 alan var (realtime/chat, dış sağlayıcı entegrasyonu, yönetim paneli) | Senkron bağımlı çekirdek domain'ler (kimlik, kullanıcı, ana iş akışı, abonelik) **tek** uygulamada modül olarak; realtime, notification ve backoffice ayrı servis. Modül sınırları ilk günden korunduğu için ileride herhangi bir modül ayrı servise çıkarılabilir. |

**Kural:** Şekil kararı ekip büyüklüğü ve ölçek profiline göre verilir, "mikroservis modern" diye değil. Kararın gerekçesi `docs/ai/repo-context.md`'de yazılır ve Bölüm 24'teki eşikler tutunca yeniden değerlendirilir.

**Sinyaller — dağıtık monolit olduğunuzu gösteren işaretler:**
- Ortak kütüphane değişince tüm servisler rebuild + deploy oluyor.
- Bir kullanıcı isteği 3+ servise senkron gidiyor.
- Tüm servisler aynı secret'ı, aynı `.env`'i, aynı rollout'u paylaşıyor.
- Kontrat modülleri versiyonsuz ve her zaman birlikte derleniyor.
- Servisler birbirinin Redis key'lerini kod içinde kopyalayarak kullanıyor.

Bu sinyaller B'de görülüyorsa ya sınırlar yanlıştır (birleştirin → C) ya da bağımlılıklar asenkronlaştırılmalıdır (Bölüm 4.6).

### 1.2 Sıcak Yol İlkesi

Kullanıcıya doğrudan latency olarak yansıyan her istek ("sıcak yol") için:
- **En fazla bir** uzak senkron çağrı (o da yazma/rezervasyon türünden olmalı; okuma değil).
- Diğer servislerin verisine ihtiyaç varsa o veri **event ile replike edilmiş local read-model**'den okunur.
- Yasal onay, hesap durumu gibi nadir değişen ve kullanıcıya bağlı bayraklar **JWT claim'i** olur; değişince oturum sürümü artırılır.
- Her uzak çağrının timeout'u, üst istek bütçesinin altındadır; circuit breaker açıkken hızlı ve tanımlı bir hata döner.

Availability aritmetiği: %99,9'luk 5 bileşene bağlı bir istek en iyi ihtimalle ≈ %99,5 (yılda ~44 saat kesinti); 2 bileşene bağlıysa ≈ %99,8. p99 latency ardışık çağrıların p99'larının toplamına yakınsar.

### 1.3 Uygulama Profilleri: MVP / Büyüme / Ölçek

Bu doküman kapsamlıdır; hepsi ilk gün yapılmaz. Üç profil, hangi pratiğin ne zaman **zorunlu** olduğunu söyler. Profil kararı ADR'ye yazılır; bir sonraki profile geçiş Bölüm 24 eşikleriyle tetiklenir.

| Alan | **P0 — MVP** (1–3 kişi, ilk kullanıcılar, tek host) | **P1 — Büyüme** (3–8 kişi, gelir var, düzenleyici görünürlük) | **P2 — Ölçek** (çok ekip / çok host) |
|---|---|---|---|
| Şekil | Modüler monolit (Spring Modulith) veya hibrit | Hibrit; farklı ölçek profilli modüller ayrı servis | Mikroservis; read-model'lerle tam asenkron |
| Sınırlar | `*-api`/`*-core` ayrımı, ArchUnit, enforcer, şema+rol/modül — **ilk gün** | + read-model'ler, sıcak yol tablosu | + kontrat versiyonlama, polyrepo değerlendirmesi |
| Veri | Tek Postgres, roller, **yedek + restore provası**, UUIDv7, retention politikası | + PgBouncer, managed/standby, partition'lı büyük tablolar, exporter'lar | + domain bazlı instance ayrımı, wide-column/arama motoru eşiğe göre |
| Mesajlaşma | Generic outbox + RabbitMQ QQ; komut/olay ayrımı ilk günden (envelope ucuz) | + `domain.events` tüketicileri (analytics sink, read-model), streams | + Kafka/Redpanda eşiğe göre, Debezium |
| Güvenlik | Asimetrik service JWT (monolitte gerek yok), secret'lar `/run/secrets`, gitleaks, mock guard'ları, rate limit, `sv` | + passkey admin 2FA, attestation, SOPS, silme saga'sı, DPIA, görsel pipeline | + mTLS/SPIRE, OpenBao, pentest döngüsü |
| Dayanıklılık | Timeout'lar + circuit breaker (config, 1 gün) | + bulkhead, bounded-staleness, yük testi, kapasite planı | + tail sampling, çok host rollout |
| Gözlem | Structured log → Alloy → Loki, Prometheus, **Alertmanager + kanal**, 5 temel alarm | + SLO/burn-rate, tracing tail sampling, runbook'lar, on-call | + SLO bazlı kapasite, çoklu ortam panoları |
| Build/deploy | CI'da image → registry → digest; compose; `restart: always`; staging | + cosign/SBOM, `docker-rollout`, affected-module CI, Renovate | + k3s/Swarm/managed k8s eşiğe göre |
| Test | Unit + binding + Testcontainers (CI'da), ArchUnit, ErrorCode tekilliği | + config drift, dayanıklılık, yük testi, OpenAPI diff + client generation | + contract testleri (polyrepo ise) |
| AI yönetişimi | `AGENTS.md`, `docs/ai/*`, immutability hook + CI, 4 temel skill (code, security, migration, test) | + tüm 12 skill, review gate, PR şablonu | + skill'lerin CI'da otomatik koşması |
| Süre tahmini | Altyapı: 2–3 sprint | Kademeli, çeyreklik | Ürün/ekip yapısına bağlı |

**Kural:** P0'da bile pazarlık edilmeyenler: yedek + restore provası, alarm kanalı, secret hijyeni, DB rolleri, CI'da gerçek DB testleri, modül sınırı testleri, komut/olay ayrımı. Bunlar sonradan eklenmesi en pahalı olanlardır.

```
                    ┌──────────────── İnternet ────────────────┐
                    │ Mobil/Web (user JWT)   Panel (admin JWT)   │
                    └───────────┬─────────────────┬──────────────┘
                                │                 │ reverse proxy (statik panel + API)
                        ┌───────▼────────┐
                        │    gateway     │  user JWT doğrula · oturum sürümü · IP engeli
                        │                │  /internal/** → 404
                        │                │  service JWT bas (aud=route, act=gateway, sub=kullanıcı)
                        └───────┬────────┘
               X-Service-Auth   │  (Ed25519 imzalı, iss=gateway, kid ile rotasyon)
    ┌──────────┬──────────┬─────┴──────┬──────────────┬──────────────┐
    ▼          ▼          ▼            ▼              ▼              ▼
  auth      <domain-a>  <domain-b>   notification    chat         backoffice
  (kimlik)  (iş)        (iş)         (SMS/push/mail)  (realtime)   (yönetim, parametre, audit)
    ▲──── HTTP + service JWT (internal-access allowlist; sıcak yolda ≤1 çağrı) ────▲
    Outbox ──► domain.events (topic) ──► read-model'ler, analytics sink, notification
    Outbox ──► <servis>.commands (queue) ──► SMS/push/mail komutları

 Altyapı: PostgreSQL (schema+rol/servis, PITR) · Valkey ×2 (security / cache) · RabbitMQ 4 (QQ + streams)
          PostGIS (geo) · S3 (private + signed URL) · [opsiyonel] Config Server (secret'sız)
 Gözlem:  OTLP→Alloy→Tempo · /actuator/prometheus→Prometheus→Alertmanager · JSON stdout→Alloy→Loki · Grafana
```

---

## 2. Teknoloji Yığını

### 2.1 Backend

Kural: **Yalnız OSS desteği süren sürümle başlanır** (Bölüm 25). Aşağıdaki sürümler 2026-09 itibarıyla güncel OSS hatlarıdır.

| Katman | Seçim | Not |
|---|---|---|
| Dil | Java 25 LTS | Virtual thread pinning düzeltmeleri (JEP 491, JDK 24) bu sürümde; Spring Framework 7 için önerilen JDK |
| Framework | Spring Boot 4.x (4.0/4.1) | BOM import ile (`spring-boot-dependencies`). Spring Framework 7, Jackson 3, Hibernate ORM 7, modüler starter'lar (`spring-boot-starter-webmvc`, `-flyway`, `-<tech>-test`). 3.x hattının OSS desteği Haziran 2026'da bitti. |
| Cloud | Spring Cloud 2025.1.x (Boot 4.0) / 2026.0.x (Boot 4.1) | BOM import. Gateway artefaktı `spring-cloud-starter-gateway-server-webflux`, property prefix'i `spring.cloud.gateway.server.webflux.*`. |
| Build | Maven ≥ 3.9 (multi-module) + gitflow-incremental-builder veya Maven Build Cache Extension | Affected-module CI (Bölüm 3.2). `${revision}` ile CI-friendly versiyon. |
| Web | Spring MVC + virtual threads (`spring.threads.virtual.enabled=true`) | Core servisler |
| Gateway | Spring Cloud Gateway Server WebFlux | `RequestRateLimiter` (Redis token bucket) gateway'de |
| Config | Compose `env_file` + Spring config tree; Config Server **opsiyonel** | Bölüm 15 |
| Servis çağrısı | Spring HTTP Service Clients (`@HttpExchange` + `@ImportHttpServices`, `RestClient`) | OpenFeign, Spring Cloud 2022.0'dan beri "feature-complete" (yalnız bugfix); mevcut projelerde kalabilir, yeni projede tercih edilmez. |
| Dayanıklılık | Resilience4j (circuit breaker, bulkhead, time limiter) veya Spring Framework 7 `@Retryable`/`@ConcurrencyLimit` | Bölüm 4.7 |
| Güvenlik | Spring Security 7 + özel filtreler | Passkey/WebAuthn desteği (6.4+) admin 2FA için |
| JWT | Nimbus JOSE+JWT | **EdDSA (Ed25519) / ES256**, servis başına anahtar çifti, JWKS + `kid`. HS256 yalnız tek uygulamalı monolitte kabul edilebilir. |
| Persistence | Spring Data JPA / Hibernate 7 | `@UuidGenerator(style = VERSION_7)` |
| Migration | Flyway | Servis başına |
| Veritabanı | PostgreSQL 18 | Native `uuidv7()`; 15 için EOL Kasım 2027 |
| ID üretimi | UUIDv7 (`uuid-creator` / Hibernate 7 / PG 18 `uuidv7()`) | Tek yöntem (Bölüm 10.3) |
| Bağlantı havuzu | HikariCP + PgBouncer (transaction mode) | Bölüm 10.5 |
| Cache/lock | Valkey 9 (BSD) veya Redis 8 (AGPL) — **iki instance**: security (`noeviction` + AOF) ve cache (`allkeys-lru`) | Lua script'li atomik sayaçlar. Redis 7.x bakımsız. |
| Mesaj kuyruğu / event | RabbitMQ 4.3+ (quorum queue, native delayed retry, **streams**) | Publisher confirm + returns. Kafka/Redpanda yalnız Bölüm 24 eşikleri tutunca. |
| Geo / arama | PostGIS (`ST_DWithin`, KNN `<->`) — yalnız metin/çok kriterli/vektör arama gerekirse OpenSearch 3.x (Apache-2.0) | Elasticsearch 8.x lisans (SSPL/ELv2; 8.16+ ve 9.x'te AGPL seçeneği) ve bakım durumu kontrol edilir |
| Object storage | AWS SDK v2 (S3 uyumlu) | Presigned upload → private quarantine bucket → işleme → private delivery bucket + signed GET / imzalı CDN (Bölüm 9.9) |
| Push / mail / SMS | Firebase Admin, Spring Mail + Thymeleaf (şablonlar DB'de), SMS sağlayıcı REST | Push payload'ında içerik yok |
| Ödeme / mağaza | Apple App Store Server API + Notifications V2, Google Play `subscriptionsv2` + RTDN; veya RevenueCat/Adapty | Mock yalnız `@Profile("local|test")` |
| Boilerplate | Lombok | Constructor injection |
| Mapping | Manuel (builder + `toResponse`) | MapStruct opsiyonel |
| API doküman | springdoc-openapi | Prod'da kapalı; CI'da OpenAPI çıktısı üretilir ve istemci client'ı generate edilir (Bölüm 20) |
| Tracing / metrik | Micrometer Tracing OTel bridge + OTLP exporter, Micrometer Prometheus + Actuator | Structured logging (`logging.structured.format.console=ecs`) |
| Test | JUnit 5, Mockito, AssertJ, MockMvc, Logback `ListAppender`, **Testcontainers 2.x** (`@ServiceConnection`), **ArchUnit** | Gerçek DB testleri CI'da koşar |
| Feature flag / deney | OpenFeature SDK + Unleash/GrowthBook veya parametre kataloğunun flag tipi | Bölüm 14.4 |

### 2.2 Frontend (yönetim paneli)

React 19, TypeScript 5 (strict), Vite, MUI 7, react-router, react-hook-form, TanStack Query (server state), notistack, recharts, native `fetch` wrapper, Vitest + Testing Library, ESLint (typescript-eslint, react-hooks) + Prettier, Node 22 LTS.

### 2.3 Operasyon

Docker (Jib veya layered jar, non-root), docker compose (+ `docker-rollout`), GitHub Actions (affected-module matrix, image build → GHCR, cosign), **Grafana Alloy** (Promtail EOL 2026-03) → Loki, Tempo, Prometheus + **Alertmanager**, node-exporter, cAdvisor, postgres/redis/rabbitmq exporter'ları, Grafana, gitleaks (pre-commit **ve CI**), Renovate/Dependabot, WAL-G/pgBackRest (PITR).

---

## 3. Repo ve Modül Topolojisi

### 3.1 Kök Dizin

```
<proje>/
├── pom.xml                       # Parent: BOM import, pluginManagement + build/plugins, modules
├── AGENTS.md                     # AI ajanları için kanonik kurallar
├── CLAUDE.md                     # Yalnız AGENTS.md'ye yönlendirir
├── README.md                     # Tek operasyonel/mimari README
├── docker-compose.yml            # Uygulama servisleri
├── docker-compose.obs.yml        # Gözlemlenebilirlik yığını (ayrı compose projesi)
├── .github/workflows/            # ci-tests, migration-immutability, deploy
├── .agents/skills/<skill>/       # Ajan-agnostik skill'ler
├── .claude/{skills,hooks,settings.json}
├── docs/
│   ├── ai/                       # repo-context, security-rules, context-boundaries, review-checklist, operation-consistency
│   └── <client>-<feature>-integration-v<N>.md   # istemci ekiplerine entegrasyon dokümanları
├── scripts/                      # migration immutability kontrolü + testi, config drift kontrolü
├── obs/                          # prometheus, alert-rules, alertmanager, loki, alloy, tempo, grafana provisioning
├── secrets/                      # SOPS ile şifreli secret dosyaları (*.enc.yaml); .sops.yaml (age recipient'ları)
├── platform/                     # Ortak kütüphaneler — her biri gerçek Spring Boot starter
│   ├── platform-core/            #   ApiResponse, ErrorResponse, ServiceException, ErrorCode, GlobalExceptionHandler, UUIDv7 generator
│   ├── platform-security/        #   service JWT (asimetrik), filtreler, @CurrentAccount, rate limit
│   ├── platform-observability/   #   TraceContextCarrier, erişim logu, SensitiveLogSanitizer, ortak metrikler
│   └── platform-messaging/       #   generic outbox + poller, CloudEvents envelope, inbox, LocalSagaStore
├── gateway/
├── [config-server/]              # Opsiyonel (Bölüm 15); secret taşımaz
├── services/<domain>/<domain>-api  <domain>-core   # Her domain için çift
└── <panel>-web/                  # Yönetim paneli
```

**Neden `platform/*` dört parça?** Tek bir "common" kütüphanesi değişince tüm servisler rebuild + deploy olur ("god library"). Dört ayrı starter'da yalnız ilgili parçaya bağımlı servisler etkilenir ve her parça `AutoConfiguration.imports` ile yüklendiği için `scanBasePackages` hilesi gerekmez (Bölüm 4.5).

### 3.2 Maven ve Build Hattı

- Parent `packaging=pom`. `dependencyManagement` içinde `spring-boot-dependencies` ve `spring-cloud-dependencies` BOM'ları import edilir.
- **Kural:** `maven-enforcer-plugin` (Java/Maven sürümü, `dependencyConvergence`, **`bannedDependencies` ile `*-core` → `*-core` yasağı**) ve `spring-boot-maven-plugin` yalnız `pluginManagement`'ta bırakılmaz, `build/plugins`'e de eklenir. Aksi halde hiç çalışmazlar.
- **Kural — affected-module build:** Her PR'da tüm reactor derlenip test edilmez. `gitflow-incremental-builder` (referans branch'e göre değişen modül + bağımlıları: `buildDownstream`, `buildUpstreamMode=impacted`) veya **Maven Build Cache Extension** (girdi hash'iyle modül çıktısı cache'lenir) kullanılır. En basit hali: `dorny/paths-filter` + servis başına GitHub Actions matrix + `mvn -pl <modül> -amd`.
- Versiyon `${revision}` (CI-friendly); kontrat modülleri birlikte deploy edildiği sürece ayrı semver gerekmez. Ayrı deploy kadansına geçilince `*-api` modülleri Maven repository'ye (GitHub Packages) yayınlanır.
- Kullanılmayan bağımlılık, iskelet modül ("ileride lazım olur" diye eklenen boş servis) tutulmaz.
- **Neden monorepo?** Kontrat değişikliği ve tüketicileri tek PR'da atomik değişir; `AGENTS.md`, skill'ler, hook'lar tek kopya; IDE'de tüm sistem tek projede refactor edilir. Polyrepo ancak birden fazla ekip, ayrı release kadansı ve ayrı on-call olduğunda düşünülür. Monorepo'nun maliyeti build hattıdır; o da yukarıdaki araçlarla ödenir.

### 3.3 `*-api` / `*-core` Ayrımı

| Modül | İçerik | Bağımlı olabileceği modüller |
|---|---|---|
| `<domain>-api` | Servisler arası request/response DTO'ları, istemciye açık enum'lar, contract sabitleri | `spring-web`, `jakarta.validation-api`, `lombok`(optional). Gerekirse başka `*-api`. **Hiçbir `*-core`'a bağımlı olamaz.** |
| `<domain>-core` | Entity, repository, domain logic, controller, migration, config, Feign client'lar | Kendi api'si, çağırdığı servislerin api'leri, ortak kütüphane. **Başka `*-core`'a bağımlı olamaz.** |

**Kurallar:**
- Entity hiçbir zaman servisler arası contract olmaz.
- Cross-service DTO kopyalanmaz; hedefin api modülündeki sınıf kullanılır.
- Contract değişikliği mobil, panel ve internal çağıranlar açısından geriye uyumluluk için değerlendirilir.
- HTTP client arayüzleri **çağıran servisin** `client` paketinde tanımlanır. Hedef başına tek client olur.
- api modülünün kök paketi `com.<org>.<domain>.api` olur. **Neden:** core ile split package oluşmasın.
- Domain event payload'ları da api modülünde (`event` paketi) yaşar; tüketici servis event sınıfını buradan import eder, kopyalamaz.

### 3.4 Servis Kimlik Tablosu (Şablon)

Her servis için bu tablo README'de tek yerde tutulur:

| Servis | Port | `spring.application.name` | Actor (`service-jwt.service-name`) | Audience (`service-jwt.audience`) | DB schema | Hata kodu bloğu | Main sınıf |
|---|---|---|---|---|---|---|---|
| gateway | 8080 | gateway | gateway | route metadata | – | – | `GatewayApp` |
| auth | 8084 | auth | auth-service | auth-api | `auth` | 10000–10999 | `AuthApp` |
| order | 8081 | order | order-service | order-api | `order` | 11000–11999 | `OrderApp` |
| notification | 8091 | notification | notification-service | notification-api | `notification` | … | `NotificationApp` |
| backoffice | 8095 | backoffice | backoffice-service | backoffice-api | `backoffice` | … | `BackofficeApp` |
| config-server | 8888 | config-server | – | – | – | – | `ConfigServerApp` |

**Kural:** HTTP client grubunun/`@FeignClient(name)` değerinin adı hedefin audience'ıdır (`inventory` → `aud=inventory-api`). Tabloya her servis için ayrıca **sıcak yol uzak çağrı sayısı** ve **yayınladığı / tükettiği olaylar** sütunları eklenir.

### 3.5 Paket Kökleri

| Modül | Kök | Alt paketler |
|---|---|---|
| `platform-core` | `com.<org>.platform.core` | `dto, exception, handler, id, util` |
| `platform-security` | `com.<org>.platform.security` | `annotation, context, filter, jwt, jwks, ratelimit, resolver, session` |
| `platform-observability` | `com.<org>.platform.observability` | `tracing, logging, metrics` |
| `platform-messaging` | `com.<org>.platform.messaging` | `outbox, inbox, event, consistency` |
| gateway | `com.<org>.gateway` | `config, filter, session, util` |
| `<domain>-api` | `com.<org>.<domain>.api` | `dto, enums, event` (veya `request, response`). `event` paketi domain event payload'larını taşır (Bölüm 12.2). |
| `<domain>-core` | `com.<org>.<domain>` | `config, controller, entity, enums, exception, client, readmodel, repository, service, service.impl, outbox, worker, saga, util` |

`client/`: HTTP Service Client arayüzleri (eski adıyla `feign/`). `readmodel/`: diğer servislerden event ile replike edilen projeksiyonlar ve tüketicileri (Bölüm 4.6).

---

## 4. Servis İçi Katmanlı Mimari

### 4.1 Paket Şablonu

Katman bazlı paketleme + teknik alt sistem paketleri:

```
com.<org>.<servis>
├── <Servis>App.java      # @SpringBootApplication(scanBasePackages={"com.<org>.<servis>","com.<org>.security"})
├── config/               # SecurityConfig, JpaAuditingConfig, RabbitMQAmqpConfig, OperationConsistencyConfig, *Properties
├── controller/           # Public ve Internal* controller'lar (ince katman)
├── entity/               # JPA entity'leri (suffix'siz)
├── enums/                # Yalnız core'a özel enum'lar
├── exception/            # ErrorCode enum + <Servis>ServiceException
├── client/               # Hedef servis başına bir HTTP client arayüzü (@HttpExchange veya Feign)
├── readmodel/            # Event ile replike edilen projeksiyonlar + @RabbitListener tüketicileri
├── repository/           # Spring Data arayüzleri + JdbcTemplate DAO'ları
├── service/              # Use-case arayüzleri + Guard/Policy/Provider/*TransactionService
├── service/impl/         # Yalnız *ServiceImpl
├── outbox/ | worker/     # *OutboxHandler'lar, *Worker, *RetentionJob (poller platform-messaging'den gelir)
├── saga/                 # Saga recovery worker (koordinatör serviste)
└── util/                 # Stateless yardımcılar
```

Gerekirse teknik alt paketler eklenir: `websocket/`, `redis/`, `consumer/`, `provider/`, `audit/`, `specification/`, `security/`.

**Kaçın:**
- `service/impl` altına Impl olmayan sınıf koymak.
- `@Configuration` sınıfını `config/` dışında tutmak.

**Alternatif:** 10'dan fazla alt domain barındıran büyük servislerde feature bazlı paketleme (`com.<org>.<servis>.<feature>.{api,domain,persistence,worker}`) okunabilirliği artırır.

### 4.2 Katman Sorumlulukları

| Katman | Sorumluluk | Yapmaz |
|---|---|---|
| Controller | HTTP binding, `@Valid`, `@CurrentAccount`, header okuma, status kodu, `Cache-Control` | Business logic, repository erişimi, try/catch ile hata dönüşü |
| Service (arayüz) + ServiceImpl | İş kuralları, orkestrasyon, rate limit, guard çağrıları, mapping | Transaction içinde uzak HTTP çağrısı |
| `*TransactionService` | Yalnız DB yazımını tek transaction'da toplayan **ayrı bean**. Proxy'den geçmesi için ayrı tutulur. | Uzak çağrı |
| Guard / Policy / Provider | Çapraz ön kontroller: yetki, durum, parametre okuma. Fail-closed. | – |
| Repository | Derived query, JPQL, native SQL (claim, keyset), `@Lock` | Başka servisin schema'sına erişim |
| Entity | Tablo eşlemesi, basit türetilmiş durum | Servisler arası contract olmak |
| Client (HTTP) | Başka servisin `/internal/**` ucunu çağırmak; her çağrı timeout + circuit breaker altında | Hedefin core'una bağımlılık; sıcak yolda okuma amaçlı çağrı |
| Read-model + Consumer | Diğer servisin event'lerinden local projeksiyon tutmak (idempotent, `event_id` ile) | Projeksiyonu "kaynak" gibi dışa açmak; kaynağa senkron sormadan yazma kararı vermek gerektiğinde read-model'i tek başına yeterli saymak (o zaman saga/rezervasyon gerekir) |
| Poller / Worker | Outbox ve saga adımlarını claim edip işlemek | Tek instance varsaymak |

**Interface kuralı:** Dışa açık use-case için arayüz + impl yazılır. İç teknik bileşenler (guard, poller, provider) somut sınıftır.

**Mapping:** Servis katmanında ya da ayrı `*Mapper` sınıfında yapılır, tek bir metot adıyla (`toResponse`). Controller'da mapping ve repository erişimi yasaktır. Bu kural ArchUnit ile zorlanır.

### 4.3 Transaction Desenleri

| Desen | Ne zaman |
|---|---|
| `@Transactional` (metot seviyesi) | Standart yazma |
| `@Transactional(readOnly = true)` | Okuma |
| `Propagation.MANDATORY` | Çağıranın transaction'ında yazılmak **zorunda** olan yardımcılar: outbox yazıcı, audit/log kaydı, kilit servisi |
| `Propagation.REQUIRES_NEW` | Ana işlem hata alsa da kalıcı olması gereken kayıt (auth olayı), retry başına taze transaction |
| `noRollbackFor = XServiceException.class` | Hata dönerken yan etki kalıcı olmalı: token ailesi iptali, deneme sayacı |
| `TransactionTemplate` | Uzak okuma transaction dışında kalsın; DB işi kısa ve kilitli olsun |
| `TransactionSynchronization.afterCommit` / `@TransactionalEventListener(AFTER_COMMIT)` | Commit sonrası yan etki: Redis yayını, arama index'i, realtime |
| Hatayı commit sonrası fırlatma | Transaction bir sonuç record'u döndürür, exception commit'ten sonra atılır. Böylece deneme sayacı rollback olmaz. |

**Kural:** Uzak HTTP çağrısı DB transaction'ı, row lock veya advisory lock tutulurken **yapılmaz**. Kanonik biçim:

```
ServiceImpl.execute()                       (@Transactional YOK)
  ├─ rate limit, guard'lar, uzak doğrulamalar   (Feign; fail-closed)
  └─ transactionService.persist(...)        (@Transactional: domain yazımı + outbox satırı)
AFTER_COMMIT → realtime/cache yan etkileri
Poller → outbox → uzak yan etki
```

### 4.4 Uçtan Uca İstek Akışı (Genel)

```
İstemci ─► gateway: user JWT doğrula (claim'ler: sub, sv, legal_ok, …) → oturum sürümü → service JWT (aud=<servis>-api, act=gateway, sub=kullanıcı)
  ─► <servis> ServiceJwtVerificationFilter: imza(JWKS, kid)/iss/aud/exp → request attr "x.accountId"
  ─► Controller(@CurrentAccount UUID, @RequestHeader("X-Idempotency-Key") UUID, @Valid Request)
  ─► ServiceImpl (tx yok): idempotency kaydı → rate limit → guard'lar (LOCAL read-model + JWT claim'leri)
                          → [yalnız gerekiyorsa TEK uzak yazma: rezervasyon/hak tüketimi, circuit breaker altında]
  ─► TransactionService (@Transactional): advisory/row lock → domain yazımı → outbox satırı (event + komut) → saga success
  ─► async: Poller → domain.events (topic) / komut queue'su;  Saga worker → confirm/compensate
  ─► diğer servisler: event'i inbox'a alır → kendi read-model'ini günceller
```

### 4.5 Main Sınıf

```java
@SpringBootApplication
@ConfigurationPropertiesScan("com.<org>.order")
@ImportHttpServices(group = "inventory", types = InventoryClient.class)   // Spring 7 HTTP Service Clients
@EnableScheduling
public class OrderApp { public static void main(String[] a) { SpringApplication.run(OrderApp.class, a); } }
```

- `platform-*` kütüphaneleri **gerçek Spring Boot starter**'dır: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` + `@AutoConfiguration` + `@ConditionalOn*`. `scanBasePackages` ile ortak paket taranmaz.
- HTTP client grubu başına config: `spring.http.serviceclient.inventory.base-url`, `connect-timeout`, `read-timeout`. Feign kullanılıyorsa `@EnableFeignClients(basePackages = "com.<org>.order.client")`.

**Kaçın:** Adı "AutoConfig" olup component scan ile yüklenen sınıflar. Bu durumda `@AutoConfigureAfter` etkisiz kalır ve `@ConditionalOnBean` tarama sırasına bağımlı hale gelir.

### 4.6 Read-Model Replikasyonu: Sıcak Yoldan Senkron Okumayı Çıkarmak

**Sorun:** "Sipariş ver" isteği önce hesabın aktif olup olmadığını (auth), yasal onayı (auth), engel durumunu (user) ve limitini (subscription) senkron soruyorsa 4 uzak çağrı + DB yazımı = 5 bileşenin çarpımı kadar availability ve toplam p99 latency. Microsoft'un mikroservis rehberi bunu açıkça anti-pattern sayar: "bir istemci isteğini karşılarken servisler arası senkron HTTP zinciri… bunun yerine veriyi event'lerle ilk servisin veritabanına replike edin". Chris Richardson'da adı **command-side replica** desenidir.

**Çözüm:** Nadir değişen ve karar için gereken veri, sahibi tarafından **event** olarak yayınlanır; tüketici servis kendi şemasında bir projeksiyon tablosu tutar.

```sql
-- order şemasında; sahibi auth/user servisleri, tüketicisi order.
CREATE TABLE "order".account_standing (
    account_id   UUID PRIMARY KEY,
    active       BOOLEAN NOT NULL,
    legal_ok     BOOLEAN NOT NULL,
    blocked_ids  UUID[]  NOT NULL DEFAULT '{}',
    revision     BIGINT  NOT NULL,          -- kaynak servisin olay sırası; eski olay yeni satırı ezmez
    updated_at   TIMESTAMPTZ NOT NULL
);
```

**Kurallar:**
- Tüketici `UPSERT … WHERE excluded.revision > account_standing.revision` ile yazar; sıra bozuk gelen olay yok sayılır.
- Satır yoksa veya `updated_at` eşikten eskiyse (örn. 24 sa) **fail-closed**: kaynağa senkron sorulur (tek istisna) ya da istek reddedilir. Hangi seçenek olduğu domain başına yazılı olur.
- Read-model **karar** verdirir ama **kaynak** değildir; başka servis bu tabloyu okumaz, API olarak dışa açılmaz.
- Yeni bir tüketici servis, read-model'ini stream/replay ile (Bölüm 12.4) veya sahibin `/internal/.../export` ucundan keyset ile sıfırdan kurabilmelidir ("rebuild" yolu belgelenir).
- **JWT claim alternatifi:** Kullanıcıya bağlı, nadir değişen bayraklar (`legal_ok`, `legal_rev`, `tier`) user JWT'de taşınır; değişince oturum sürümü (`sv`) artırılır → token yenilenir → claim güncellenir. Gateway ve servisler kaynağa hiç sormaz. Kural: claim'ler yetki **sinyali**dir, kaynak DB'yi değiştirmez.
- Yazma niteliğindeki kontrol (hak tüketimi, stok rezervasyonu) read-model'den yapılamaz; o **tek** uzak senkron çağrı olarak kalır ve saga ile korunur (Bölüm 11.4).

**Eşik:** Sıcak yoldaki her istek için "kaç uzak çağrı?" sayısı README'deki servis tablosuna yazılır; 1'i geçen akış mimari inceleme ister.

### 4.7 Dayanıklılık: Timeout, Circuit Breaker, Bulkhead

Sıra (Nygard, *Release It!*): önce **her hop'ta sert timeout**, sonra **bağımlılık başına bulkhead**, sonra **circuit breaker**; retry yalnız bütçeyle ve yalnız idempotent çağrılarda.

| Katman | Kural |
|---|---|
| Gateway | İstek başına toplam bütçe (örn. 3 sn). Downstream timeout'ları bunun altında. |
| HTTP client | connect 1–2 sn, read 2–5 sn; hedef başına `resilience4j.circuitbreaker.instances.<hedef>` + `bulkhead` (eşzamanlı çağrı üst sınırı, semaphore). Circuit açıkken tanımlı `ServiceException` (503/`UPSTREAM_UNAVAILABLE`) döner, thread bloke olmaz. |
| Tuzak | Spring Cloud CircuitBreaker + Resilience4j entegrasyonu varsayılan **1 sn TimeLimiter** ve thread-pool bulkhead ekler; `resilience4j.timelimiter.instances.*` ayarlanmaz ya da `disable-time-limiter` denmezse 5 sn'lik read timeout anlamsızlaşır. |
| Thread modeli | `spring.threads.virtual.enabled=true` (Java 25). Tomcat thread sınırı kalkar; bloklayan IO ucuzlar. Hikari havuzu bilinçli sınır olarak kalır (Bölüm 10.5). |
| Retry | Senkron yolda yok (`Retryer.NEVER_RETRY`). Retry outbox/saga worker'larında, backoff ile. |
| Control plane | Parametre kataloğu, config server, feature flag gibi "control-plane" bağımlılıkları düşünce servis **son bilinen değerle** çalışmaya devam eder (AWS "static stability", Bölüm 14.3). |
| Kapasite | Tomcat varsayılanı 200 thread; 30 sn'lik bir downstream timeout ile 7 istek/sn bile havuzu doldurur. Bu hesap her yeni senkron bağımlılıkta yapılır. |

**Test:** Her HTTP client için "hedef 30 sn cevap vermiyor" senaryosu: çağıran servisin p99'u bütçeyi aşmamalı, circuit açılmalı, health `DEGRADED` dönmeli. Yük testi (k6/Gatling) staging'de haftalık; kapasite planı (Bölüm 24) buna dayanır.

---

## 5. İsimlendirme Konvansiyonları

### 5.1 Java Sınıfları

| Suffix | Anlam | Örnek |
|---|---|---|
| `*App` | Main sınıf | `OrderApp` |
| `*Controller` | Public REST | `OrderController` |
| `Internal*Controller` | `/internal/**` uçları (önek ile) | `InternalOrderController` |
| `*Service` / `*ServiceImpl` | Use-case arayüzü / implementasyonu | `OrderService`, `OrderServiceImpl` |
| `*TransactionService` | Tek transaction'lık yazım bean'i | `OrderTransactionService` |
| `*Repository` | Spring Data veya JdbcTemplate veri erişimi | `OrderRepository` |
| Entity | **Suffix yok**, tekil PascalCase | `Order`, `OrderItem`, `OrderEventOutbox` |
| `*Request` / `*Response` / `*Dto` | API DTO'ları (tek yazım: `Dto`) | `CreateOrderRequest`, `OrderResponse` |
| `*Page` / `PageResponse<T>` | Export cursor sayfası / sayfalı liste | `OrderExportPage` |
| `*Config` / `*Properties` | `@Configuration` / `@ConfigurationProperties` | `RabbitMQAmqpConfig`, `ServiceJwtProperties` |
| `<Hedef>Client` | HTTP client arayüzü (`@HttpExchange` veya Feign) | `InventoryClient` |
| `*OutboxHandler` / `*Worker` / `*RetentionJob` | Outbox handler'ı (poller platform'da) / arka plan işi / retention | `OrderEventOutboxHandler` |
| `*ReadModel` / `*Projection` + `*Consumer` | Event ile replike edilen projeksiyon ve tüketicisi | `AccountStandingReadModel`, `AccountStandingConsumer` |
| `*Publisher` / `*Consumer` / `*Listener` | MQ yayıncı / `@RabbitListener` / Spring veya STOMP event | `NotificationRabbitPublisher` |
| `*Guard` / `*Policy` / `*Provider` | Fail-closed kontrol / kural hesabı / dış değer sağlayıcı | `OwnershipGuard`, `SystemParameterProvider` |
| `*Filter` / `*Interceptor` / `*Resolver` / `*Handler` | Web altyapısı | `ServiceJwtVerificationFilter`, `GlobalServiceExceptionHandler` |
| `*Specification` / `*Runner` | JPA Specification / açılış işi | `OrderSpecification` |
| `ErrorCode` / `<Servis>ServiceException` | Hata enum'u / servis exception'ı | `OrderServiceException` |
| `*Util(s)` | Stateless yardımcı | `ServletUtils` |

### 5.2 Metotlar

| Durum | Kalıp |
|---|---|
| Satır kilidi ile okuma | `findByIdForUpdate`, `lock...` |
| Outbox | `claimPending`, `markFailed`, `markDead`, `deleteProcessed`, `markExpired` |
| Idempotent insert | `insertIfAbsent` |
| Guard | `require...` (throw eder), `is...` (boolean), `...OrThrow` |
| Mapping | `toResponse` / `toDto` (tek biçim seçilir) |
| Enum parse | `static from(String)` |

### 5.3 REST Path'leri

| Tür | Kalıp | Örnek |
|---|---|---|
| Public | `/<kaynak-çoğul>/...`. Gateway prefix'i ile aynı, kebab-case. | `/orders/{orderId}` |
| Kendi kaynağı | `/<kaynak>/me/...` | `/users/me/settings` |
| Internal | `/internal/<kaynak-çoğul>/...` | `/internal/orders/{orderId}/status` |
| Yönetim paneli | `/backoffice/<kaynak-çoğul>` | `/backoffice/reports` |
| Durum geçişi | `POST /{id}/<fiil>` | `/{id}/assign`, `/resolve`, `/confirm`, `/compensate` |
| Güncelleme | `PUT` / `PATCH /{id}` | – |
| Versiyonlama | Baştan karar verilir (`/v1` veya header) | – |

**Kural:** Binding adları açık yazılır: `@PathVariable("orderId")`, `@RequestParam("page")`, `@RequestHeader("X-Idempotency-Key")`. Parameter-name inference'a güvenilmez.

### 5.4 Header'lar ve Request Attribute'ları

| Ad | Anlam |
|---|---|
| `Authorization: Bearer` | user veya admin JWT |
| `X-Service-Auth: Bearer` | service JWT |
| `X-Subject-Id` | Feign çağrısında kullanıcı bağlamı. Interceptor bunu JWT `sub` claim'ine taşır ve header'ı **siler**. |
| `X-Idempotency-Key` | UUID. Tek kullanıcı niyeti = tek key. |
| `X-Trace-Id` | Yanıtta aktif trace id; hata gövdesindeki `error.traceId` ile eşit |
| `X-CSRF-Token` | Cookie tabanlı refresh için double-submit CSRF |
| attr `x.accountId`, `x.actor` | Doğrulanmış kullanıcı ve çağıran servis |

### 5.5 Veritabanı

| Nesne | Kalıp |
|---|---|
| Schema | Servis adı |
| Tablo | snake_case, **tek kural** (önerilen: tekil) |
| Outbox / saga | `<konu>_outbox`, `saga`, `saga_steps`, `idempotent_operation` |
| Kolon | snake_case; zaman `*_at` (`TIMESTAMPTZ`), kimlik `*_id` (UUID) |
| Constraint | `uq_<tablo>_<anlam>`, `ck_<tablo>_<anlam>`, `fk_<tablo>_<hedef>` |
| Index | `idx_<tablo>_<amaç>`, partial unique: `uq_...` + `WHERE` |
| Sequence | `<konu>_seq` |
| Migration | `V<n>__<snake_case>.sql`, `R__<ad>.sql` |

### 5.6 Konfigürasyon ve Diğer İsim Uzayları

| Alan | Kalıp |
|---|---|
| Servis URL | `services.<servis>.base-url` (→ `spring.http.serviceclient.<servis>.base-url`) |
| JWT | `service-jwt.*`, `user-jwt.*`, `admin-jwt.*` |
| Rate limit | `rate-limit.rules.<scope>.{limit,window-seconds}`; scope kebab-case (`login-ip`, `order-create-account`) |
| Poller | `<servis>.outbox.poll-interval-ms` (tek outbox); cron: `<servis>.<iş>.retention-cron` |
| Saga | `operation-consistency.{deadline-ms,lease-ms,poll-ms,monitor-ms,cleanup-cron}` |
| Ortam değişkeni | UPPER_SNAKE (`DB_HOST`, `REDIS_PASS`, `SERVICE_JWT_SECRET`) |
| Redis key | `<alan>:<alt>:<id>` (`rl:<scope>:<sha256>`, `user-session:min-version:<id>`, `presence:online:<id>`); güvenlik ve cache key'leri ayrı instance'ta |
| RabbitMQ | event: `domain.events` (topic), routing `{servis}.{aggregate}.{olay}`, queue `{tüketici}.{amaç}.queue`; komut: `{servis}.commands`, queue `{hedef}.{komut}.queue`; DLX `{servis}.dlx`, DLQ `{queue}.dlq` |
| STOMP | prefix `/app`, topic `/topic/<kaynak>.{id}` |
| Log `code=` alanı | UPPER_SNAKE (`code=ORDER_NOT_FOUND`, `code=OUTBOX_STUCK`) |
| Metrik | snake_case `_total` |
| Span | nokta ayrımlı (`outbox.order-event.apply`) |
| Audit aksiyonu | UPPER_SNAKE fiil+nesne (`CANCEL_ORDER`) |

### 5.7 Testler

| Öğe | Kalıp |
|---|---|
| Sınıf | `<Sınıf>Test`. Büyük servislerde konu bazlı (`<Sınıf><Konu>Test`). Gerçek DB testi: `*PostgresTest` / `*IT`. Diğerleri: `*ContractTest`, `*ConsistencyTest`, `*BoundaryTest`, `*ServiceJwtAccessTest`. |
| Metot | `metot_whenKoşul_beklenenSonuç` veya davranış cümlesi (`staleWorkerCannotCompleteAfterLosingLease`). Tek stil seçilmesi önerilir. |

### 5.8 Dil

| Öğe | Dil |
|---|---|
| Kod, log, API hata mesajı, validation mesajı | İngilizce |
| Kod yorumları, migration yorumları, iç dokümanlar | Ekibin dili (burada Türkçe). Yorum kısa olur, "neden"i anlatır. Changelog tarzı yorum yasak. |
| Kullanıcıya gösterilen metin | İstemci tarafında hata koduna göre çevrilir |

---

## 6. API Tasarımı ve Contract Kuralları

### 6.1 Public ve Internal

| | Public | Internal |
|---|---|---|
| Path | `/<kaynak>/**` | `/internal/**` |
| Gateway | Route edilir | **404** (varlık sızdırmamak için 403 değil) |
| Kimlik | Gateway'in service JWT'si (`act=gateway`, `sub`=kullanıcı) | Çağıran servisin service JWT'si |
| Yetki | `@CurrentAccount` + ownership | `internal-access` allowlist (first-match, default-deny) |

**Kaçın:** `StripPrefix` kullanılan route'larda `/prefix/internal/...` isteği gateway'in `/internal` kontrolünü atlatır. Kontrol, prefix kırpıldıktan sonraki path üzerinde de yapılmalı.

### 6.2 Yanıt Zarfı

```json
{ "ok": false, "data": null,
  "error": { "code": 11020, "message": "Order cannot be cancelled.", "service": "order",
             "path": "/orders/…", "timestamp": 1739500000000,
             "traceId": "4bf92f3577b34da6a3ce929d0e0e4736", "details": ["retryAfterSeconds=120"] } }
```

- `ApiResponse<T>{ok, data, error}` + `ErrorResponse{code, message, service, path, timestamp, traceId, details}`.
- **Kural:** Hatalar her zaman zarflıdır. Başarı yanıtı için tek karar verilir: her zaman zarflı ya da her zaman ham.
- Filtre seviyesindeki redler de aynı formatla yazılır (ortak bir `SecurityErrorWriter`).
- `details` yalnız istemciye gösterilebilir `anahtar=değer` bilgisi taşır. Log'a özel bilgi buraya konmaz (Bölüm 7.4).

### 6.3 Sayfalama

| Tür | Biçim |
|---|---|
| Offset | `page`, `size` → `PageResponse<T>{data:[…], page:{number,size,totalElements,totalPages}}`. Boyut sınırı (örn. 1–100). Sıralama alanı **allowlist**, yön `ASC`/`DESC`. |
| Keyset (export, büyük liste) | `beforeAt` + `beforeId` → `nextAt`, `nextId`. Sorgu `(x < :at OR (x = :at AND id < :id))`, `size+1` kayıt çekilir. |

### 6.4 Idempotency

- Tekrar-güvenli işlemler `X-Idempotency-Key` (UUID) alır. Aynı niyetin retry'ları aynı key'i kullanır.
- Tekillik `(account_id, scope, operation_key)` üzerindedir. Aynı key farklı body ile gelirse ilk istek kazanır.
- Tamamlanmış istek aynı sonucu döner. Sürmekte olan `OPERATION_IN_PROGRESS`, iptal edilmiş `OPERATION_CANCELLED` döner.
- Veri seviyesinde: `INSERT ... ON CONFLICT DO NOTHING`, deterministik `eventId = UUID.nameUUIDFromBytes(...)` + unique.

### 6.5 Kimlik Bağlamı

`@CurrentAccount UUID` ile kimlik service JWT `sub` claim'inden (`x.accountId`) gelir.

**Kural:** Hesap kimliği **asla** path, query veya body'den alınmaz (IDOR koruması). Path'teki id yalnız hedef kaynaktır. Internal uç kullanıcı adına çalışıyorsa JWT `sub` claim'i path'teki hesapla karşılaştırılır.

### 6.6 Validation

- Constraint'ler api DTO'larında Jakarta anotasyonlarıyla yazılır. Çapraz alan kuralı `@AssertTrue` metodu ile.
- **Kural:** Her harici `@RequestBody` `@Valid` taşır.
- Mesajlar İngilizce. Hassas alanlarda `@ToString.Exclude`.

### 6.7 Cache ve Contract Uyumluluğu

- Kişisel veri dönen uçlar `Cache-Control: private, no-store` döner.
- Servisler arası okunan DTO'larda `@NoArgsConstructor` bulunur (Jackson).
- Yeni enum değeri veya event tipi eklenirken **tüketici önce deploy edilir**.
- İstemciyi etkileyen her değişiklik versiyonlu bir entegrasyon dokümanı ile iletilir (Bölüm 20).

### 6.8 Servisler Arası HTTP Client Konvansiyonu

Yeni projede **Spring HTTP Service Clients** (Spring Framework 7 / Boot 4: `@HttpExchange` arayüzü + `@ImportHttpServices`, `RestClient` altyapısı, grup başına `spring.http.serviceclient.<grup>.*`). OpenFeign, Spring Cloud 2022.0'dan beri "feature-complete"tir; mevcut projelerde kalabilir ama yeni client bu modelde yazılır. Her iki modelde kurallar aynıdır:

```java
@HttpExchange("/internal/reservations")
public interface InventoryClient {                                   // hedef servis başına TEK client
    @PostExchange("/{operationKey}/consume")
    ReservationResponse consume(@PathVariable("operationKey") UUID operationKey,
                                @RequestHeader("X-Subject-Id") UUID accountId,   // interceptor sub claim'ine taşır, header'ı siler
                                @RequestBody @Valid ReservationRequest request);
}
```

```yaml
spring.http.serviceclient.inventory:
  base-url: ${services.inventory.base-url}
  connect-timeout: 2s
  read-timeout: 5s
resilience4j.circuitbreaker.instances.inventory: { failure-rate-threshold: 50, wait-duration-in-open-state: 20s, sliding-window-size: 20 }
resilience4j.bulkhead.instances.inventory: { max-concurrent-calls: 25 }
```

Client grubu için ortak `ClientHttpRequestInterceptor` / Feign `RequestInterceptor` şunları sağlar:
- **Kimlik:** `aud` = hedefin audience'ı, `iss` = bu servis, `sub` = `X-Subject-Id`; imza bu servisin **kendi** private key'iyle (Bölüm 9.2); ardından header silinir.
- **Hata çevirisi:** Upstream 4xx → `ServiceException` (status korunur, gövde okunmaz ve loglanmaz); 5xx/timeout → 502/503 `UPSTREAM_*`.
- **Dayanıklılık:** circuit breaker + bulkhead (Bölüm 4.7). Retry **yok**; retry outbox ve saga worker'larındadır.
- **Tracing:** W3C header'ları otomatik (`micrometer` entegrasyonu).

**Kaçın:** Karşı tarafta `@RequestHeader("X-Subject-Id")` beklemek. Interceptor bu header'ı sildiği için kimlik orada `@CurrentAccount` ile okunmalı. Sıcak yolda **okuma** amaçlı client çağrısı (Bölüm 4.6).

---

## 7. Exception ve Hata Yönetimi

### 7.1 Hiyerarşi

```
RuntimeException
└── ServiceException                       (ortak kütüphane)
    ├── RateLimitExceededException / MissingRateLimitRuleException / RateLimitUnavailableException
    └── <Servis>ServiceException           (her core modülde; gerekirse tipli alt sınıflar)

«interface» ErrorCode { int getCode(); String getMessage(); String getService(); HttpStatus getHttpStatus(); }
├── <servis>-core/.../exception/ErrorCode  (enum, servis başına)
├── SecurityErrorCode                      (service="security")
└── SystemErrorCode                        (service="system": INTERNAL_ERROR 500, UPSTREAM_ERROR 502)
```

`ServiceException` alanları:
- `code`, `service`, `httpStatus`
- `details` (değiştirilemez liste)
- `safeLogReason`, `safeLogCategory` (**yalnız log için**)

Cause mesajı exception mesajına taşınmaz.

### 7.2 Kod Aralıkları

**Kural:** Kodlar global olarak benzersizdir. Her servise ayrı bir blok verilir, blok içinde alt gruplar yorumla ayrılır.

| Servis | Blok | Alt gruplar (örnek) |
|---|---|---|
| ortak: validation | 90000 | Bean validation, bind, malformed body, type mismatch, eksik header |
| ortak: security | 90100–90199 | rate limit aşıldı / kural yok / store yok, access denied |
| ortak: system | 99998–99999 | upstream, bilinmeyen |
| auth | 10000–10999 | hesap, OTP, token, admin, şifre, parametre |
| order | 11000–11999 | … |

Tablo README'de tek yerde tutulur. Bir unit test tüm enum'ların çakışmadığını doğrular.

**Kaçın:** Servislerin aynı numaraları kullanması. Kod ancak `service` alanıyla birlikte tekil olursa istemci eşlemesi yanlış mesaj gösterir.

### 7.3 Global Handler

| Exception | HTTP | code | Log |
|---|---|---|---|
| `ServiceException` | kendi status'u | kendi kodu | Kullanıcı kaynaklı 4xx → WARN; diğer → ERROR. `code=`, `reason=`, `category=` alanlarıyla. |
| `MethodArgumentNotValidException`, `BindException` | 400 | validation | WARN; reddedilen değer **yazılmaz** |
| `HttpMessageNotReadableException` | 400 | validation | WARN; exception metni yazılmaz |
| `MethodArgumentTypeMismatchException`, `MissingRequestHeaderException`, `MissingServletRequestParameterException`, `ConstraintViolationException` | 400 | validation | WARN |
| `NoResourceFoundException` / 405 / 415 / `MaxUploadSizeExceeded` | 404 / 405 / 415 / 413 | ilgili kod | WARN |
| `AccessDeniedException` | 403 | security | WARN |
| `OptimisticLockException` | 409 | `CONCURRENT_UPDATE` | WARN |
| `Exception` | 500 | system | ERROR + sanitize edilmiş özet + stack trace |

Her yanıta `X-Trace-Id` header'ı eklenir. `traceId` aktif span'den alınır.

**Kaçın:** Standart MVC hatalarının 500'e düşmesi. Handler'ı `ResponseEntityExceptionHandler`'dan türetin veya bu tipleri açıkça eşleyin.

### 7.4 `safeLogReason` Ayrımı (Katı Kural)

- Log'a özel neden ve kategori yalnız `safeLogReason` / `safeLogCategory` alanlarında tutulur. **Asla `details` içine konmaz**, çünkü `details` istemciye döner.
- Kullanıcıya genel bir kod gösterilir. Örnek: "kayıt tamamlanamadı". Log'a ise güvenli bir sebep yazılır: `reason=BLOCKED_CATEGORY`.

### 7.5 Fırlatma Kalıbı

```java
Order order = orderRepository.findById(id).orElseThrow(() -> {
    log.warn("Order cancel rejected: code=ORDER_NOT_FOUND");
    return new OrderServiceException(ErrorCode.ORDER_NOT_FOUND);
});
```

- **Kural:** Açıkça throw edilen her noktada, fırlatmadan hemen önce, yapılandırılmış tek bir log satırı yazılır: işlem, `code=`, güvenli `reason=` ve gerekli minimum teknik id.
- Ham exception mesajı, body, token, PII yazılmaz.
- **Seviye:** Kullanıcı kaynaklı reddetmeler WARN veya INFO olur, ERROR'u altyapı ve beklenmeyen hatalar için saklayın. Tüm iş reddetmelerini ERROR yazmak alarm gürültüsü üretir.

### 7.6 Filtre Seviyesi ve İstemci Tarafı

- Filtre redleri (401/403/400) DispatcherServlet'ten önce oluşur. Aynı `ErrorResponse` formatında yazılmalıdır.
- Redis veya secret gibi altyapı hataları temiz bir 503 olarak dönmelidir, 500 olarak değil.
- İstemciler (panel, mobil) `code` → kullanıcı mesajı eşleme tablosu tutar. Eşleşme yoksa sunucu mesajı gösterilir. Tablonun backend enum'larıyla uyumu bir contract testiyle doğrulanır.

---

## 8. Loglama ve Gözlemlenebilirlik

### 8.1 Logger

- Lombok `@Slf4j` kullanılır. Merkezi bir "log utility" eklenmez.
- MDC'ye elle yazılmaz; `traceId` ve `spanId`'yi Micrometer Tracing koyar.
- **Kural:** Loglar **structured (JSON)** yazılır — Spring Boot 3.4+ yerleşik desteği:

```yaml
logging.structured.format.console: ecs        # veya logstash / gelf
logging.structured.json.stacktrace: { root: first, max-length: 4000 }   # 3.5+
```

MDC alanları ve fluent API (`log.atInfo().addKeyValue("outcome", "SUCCESS")`) JSON alanı olur; Loki tarafında `json` stage veya native OTLP alımı ile `code=`, `outcome=` gerçek alan olarak sorgulanır. Alternatif: Boot 4 `spring-boot-starter-opentelemetry` ile OTLP log export.

**Kaçın:** Düz metin log + regex ile alan çıkarma. Kırılgandır ve çok satırlı stack trace'lerde bozulur.

### 8.2 Mesaj Formatı

```
<Sabit olay cümlesi>: key=value key=value
```

| Alan | Örnek |
|---|---|
| `operation=` | `operation=ORDER_CREATE` |
| `outcome=` | `outcome=SUCCESS` / `CREATED` / `ALREADY_EXISTS` / `LAST_KNOWN` |
| `code=` | `code=ORDER_NOT_FOUND`, `code=OUTBOX_STUCK` |
| `reason=`, `category=` | sabit, server tanımlı değerler |
| `exceptionType=` | `e.getClass().getSimpleName()` (**mesaj değil**) |
| Batch sayaçları | `claimed=`, `applied=`, `failed=`, `deferred=` |
| Teknik id | yalnız gerekiyorsa (`eventId=`, `sagaId=`) |

```java
log.info("Order created: operation=ORDER_CREATE outcome=SUCCESS");
log.info("Order outbox batch finished: claimed={} applied={} failed={}", n, ok, fail);
log.warn("Order side effect failed; retry scheduled: eventId={} retry={} exceptionType={}", id, r, type);
log.error("Order side effect still failing: code=OUTBOX_STUCK eventId={} retry={} exceptionType={}", id, r, type);
log.info("http_request method={} path={} status={} duration_ms={} traceId={} spanId={}", ...); // erişim logu
```

**Kaçın:** Aynı projede `[TAG]` önekleri, snake_case olay adları, parantezli ve pozisyonel serbest cümleler gibi birden çok stilin karışması.

### 8.3 Seviye Politikası

| Seviye | Kullanım |
|---|---|
| ERROR | Beklenmeyen hata, altyapı hatası, kalıcı başarısızlık alarmı (her N denemede `*_STUCK`) |
| WARN | Kurtarılabilir bozulma: retry planlandı, fallback'e düşüldü, lease kısaldı, kullanıcı kaynaklı reddetme |
| INFO | Normal akışın önemli adımları ve sonucu (`operation`/`outcome`), batch özeti, erişim logu |
| DEBUG | Ayrıntı. Filtre başarı logları burada durur. |

**Normal Flow Logging:**
- Akış, DEBUG açmadan INFO seviyesinden izlenebilir olmalı: işlem, önemli dal ve sonuç.
- Her metot loglanmaz; aynı olay katmanlar arasında tekrarlanmaz.
- Döngüler özetlenir; boş poll turları loglanmaz.
- Commit görülmeden "success" yazılmaz.

### 8.4 Hassas Veri

**Hiçbir seviyede loglanmaz:**
- OTP, token, şifre, secret, private key.
- Ham telefon, e-posta, kimlik numarası, konum, IP, user-agent.
- Şifreli içerik ve anahtar.
- Request/response body, DTO/entity `toString()`, ham path/query/header, credential içeren URL, sağlayıcı yanıtı, ham exception mesajı.

**Yardımcılar (ortak kütüphanede `SensitiveLogSanitizer`):**

| Metot | İşlev |
|---|---|
| `sanitize` | CR/LF temizliği, JSON/`k=v` içindeki token/password/secret/otp/ciphertext alanlarını redakte eder, uzun base64'ü redakte eder, 240 karakterde keser |
| `safeExceptionSummary` | root-cause tipi + sanitize edilmiş mesaj |
| `maskPhone` | telefonu maskeler |
| `maskEmail` | e-postayı maskeler |
| `tokenFingerprint` | kısa sha256 parmak izi |

**Kurallar:**
- UUID, hesap id, mask ve hash **otomatik güvenli değildir**; trace context tercih edilir.
- Alanlar allowlist ile seçilir.
- Parameterized logging sanitization yerine geçmez.
- Rate limit ve IP gibi Redis key'lerinde ham değer yerine hash kullanılır.

### 8.5 Log Testleri

Logback `ListAppender` ile iki şey birlikte assert edilir:
1. Güvenli alanlar (`outcome=`) **mevcut**.
2. Sentetik hassas işaretler rendered mesajda, argümanlarda, MDC'de ve exception'da **yok**.

Her testten sonra appender ayrılır ve logger seviyesi geri yüklenir (Bölüm 23.6).

### 8.6 Tracing

- `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`
- `propagation.type: W3C`, `spring.reactor.context-propagation: auto`, HTTP client micrometer entegrasyonu açık
- **Sampling:** Uygulama %100 head sampling ile Alloy/OTel Collector'a gönderir; Collector **tail sampling** yapar: hatalı ve yavaş (p99 üstü) izlerin tamamı, kalanın %10'u (`tailsampling` processor; `decision_wait` 30 sn; bir izin tüm span'leri aynı collector'a gelmeli). Boot varsayılanı (%10 head) düşük trafikte yeterli olsa da hata izlerini kaybettirir.
- **Asenkron sınırlar:** Outbox satırında `traceparent` ve `tracestate` kolonları tutulur. Poller trace'e bu değerlerden devam eder. MQ header'larına inject edilir. Redis zarfında trace metadata **HMAC kapsamına dahildir**. WebSocket handshake ve STOMP frame'leri için interceptor bulunur.
- **Kural:** Trace id yetki sinyali değildir. Span attribute'larına PII konmaz. Route'lar static template olarak yazılır. Trace id Loki label'ı yapılmaz.

### 8.7 Metrik ve Gözlem Yığını

- Actuator exposure: `health,info,prometheus`; `management.endpoint.health.probes.enabled=true` (readiness/liveness ayrı). **`gateway` actuator ucu asla açılmaz** (SpEL ile ortam değiştirme CVE'leri). Prometheus job `/actuator/prometheus`.
- Altyapı hataları için özel sayaçlar tutulur: `*_failures_total`.
- **Zorunlu iş/altyapı metrikleri:** `outbox_oldest_pending_age_seconds{outbox=…}` (takılan poller'ın en erken sinyali), `readmodel_lag_seconds{model=…}`, `parameter_staleness_seconds`, `circuitbreaker_state`, endpoint bazlı RED (Micrometer otomatik), domain sayaçları (`<domain>_created_total` vb.).
- İş katalog tutarlılığı gibi kontroller health indicator olarak eklenir.
- **SLO:** Ürünün 3–5 kritik akışı için SLO tanımlanır (örn. "ana yazma isteği p99 < 300 ms, hata oranı < %0,5"); alarm Google SRE Workbook **multiwindow multi-burn-rate** ile: 14,4× (1 sa/5 dk) ve 6× (6 sa/30 dk) → sayfa, 1× (3 gün/6 sa) → ticket.

| Bileşen | Rol |
|---|---|
| **Grafana Alloy** | Log toplama (Docker SD; Promtail **EOL 2026-03-02**, geçiş `alloy convert --source-format=promtail`), OTel Collector işlevi (tail sampling, batch, retry), metrik scrape. Pipeline: docker → JSON parse → `labels` (düşük kardinalite: `service`, `level`) → trace/kullanıcı id'leri **structured metadata**'ya (label değil). |
| Loki | Log deposu, TSDB + schema v13, retention ayarlı. Label değerleri onlarla sınırlı; `logcli series --analyze-labels` ile denetlenir. |
| Tempo | OTLP 4317/4318; retention |
| Prometheus | Metrik + alert rules → **Alertmanager** (Telegram/Slack/e-posta/on-call receiver; tek host'ta Grafana unified alerting da yeter) |
| Exporter'lar | node-exporter, cAdvisor, **postgres-exporter** (bağlantı, replication slot, bloat), **redis-exporter**, **RabbitMQ prometheus plugin** (queue derinliği, unacked) |
| Grafana | Provisioned datasource'lar (sabit `uid`), Loki derivedField → Tempo, Tempo tracesToLogs → Loki; SLO panoları |

Tüm gözlem portları yalnız `127.0.0.1`'e açılır; erişim SSH tüneli veya SSO proxy ile yapılır.

**Alarm asgari seti:** container restart-loop, `restart: on-failure` sonrası ölü servis (health `DOWN` > 2 dk), disk/RAM, PostgreSQL WAL arşiv gecikmesi ve replication slot lag, Redis bellek/eviction, RabbitMQ queue derinliği ve DLQ > 0, `outbox_oldest_pending_age_seconds` > lease, `*_STUCK` log sayacı, SLO burn-rate.

**Kaçın:**
- Alertmanager (veya eşdeğeri) olmadan alarm kuralı yazmak — kural vardır, kimse haber almaz.
- Container adı regex'lerini compose'un ürettiği gerçek adlarla (`<proje>-<svc>-1`) doğrulamamak.
- Datasource `uid`'lerini sabitlememek.
- %100 head sampling'i trafik büyüdükten sonra da sürdürmek (Tempo ingester belleği ve depolama aynı host'ta).

### 8.8 Runbook, On-call ve Olay Yönetimi

Alarm, cevabı olmayan bir sorudur. Her alarm kuralının bir **runbook** sayfası vardır (`docs/runbooks/<alarm-adı>.md`) ve alarm mesajı ona link verir.

**Runbook şablonu:**
```
# <Alarm adı>
Ne anlama gelir · Kullanıcı etkisi · İlk 5 dakika (bak: pano linki, log sorgusu, komut) ·
Olası nedenler ve düzeltme (sıralı) · Ne zaman eskalasyon · Kalıcı düzeltme için ADR/issue linki
```

**Asgari runbook seti (P0):** servis DOWN / restart-loop · disk doluyor · PostgreSQL bağlantı tükenmesi · WAL arşiv gecikmesi · Redis bellek/eviction · RabbitMQ DLQ > 0 · `outbox_oldest_pending_age_seconds` > lease · circuit breaker açık kaldı · deploy geri alma · yedekten restore.

**On-call:** Tek kişilik ekipte bile "kim bakar, hangi kanaldan, hangi saatlerde" yazılıdır. Alertmanager route'ları önem derecesine göre (sayfa / ticket) ayrılır; gece yalnız kullanıcı-etkili alarmlar sayfa atar.

**Olay (incident) süreci:**
1. Tespit → alarm veya kullanıcı bildirimi; olay kanalı açılır, tek koordinatör.
2. Azaltma (mitigate) önce, kök neden sonra: geri alma (önceki digest), circuit'i elle açma, feature flag kapatma, parametre ile limit düşürme.
3. İletişim: kullanıcıya görünür etki varsa durum notu (şablon hazır).
4. **Postmortem** (suçlamasız, 5 iş günü içinde): zaman çizelgesi, etki, kök neden(ler), neyin işe yaradığı, aksiyonlar (sahipli, tarihli). Aksiyonlar Bölüm 24 eşiklerini veya bu dokümanı güncelleyebilir.
5. Tekrarlayan olay = eksik alarm/test/runbook; postmortem aksiyonu bunu kapatır.

**Kural:** Runbook'suz alarm ve postmortem'siz olay kabul edilmez; release-readiness skill'i bunları kontrol eder.

---

## 9. Güvenlik Mimarisi

### 9.1 Güven Sınırları

```
[İstemci] ─user JWT─► [gateway] ─service JWT(act=gateway, sub=kullanıcı, aud=<route>)─► [servis A]
                                                                                         │ Feign
                          service JWT(act=<A>-service, sub=?, aud=<B>-api) ◄─────────────┘
                                                                                         ▼
                                                                                    [servis B]
[Panel] ─admin JWT─► gateway (permitAll /backoffice/**) ─► backoffice AdminJwtFilter
        refresh: HttpOnly cookie + CSRF header ─► auth /auth/admin/**
```

- İnternet trafiği yalnız gateway'den girer. Servis portları host'a açılmaz.
- Core servislerin Spring Security zinciri stateless'tır ve CSRF kapalıdır. **Asıl yetki** `ServiceJwtVerificationFilter` + `internal-access` allowlist + `@CurrentAccount` ile verilir.
- Yönetim servisi bunlara ek olarak `@EnableMethodSecurity` kullanır: `/backoffice/**` authenticated, geri kalan her şey `denyAll`.

### 9.2 JWT Yüzeyleri

| | User JWT | Service JWT | Admin JWT |
|---|---|---|---|
| Üreten | auth | gateway ve her servis (client interceptor) — **her biri kendi private key'iyle** | auth |
| Doğrulayan | gateway (ve WebSocket handshake) — auth'un public key'iyle | tüm core servisler — `iss` → JWKS eşlemesiyle | yönetim servisi |
| Algoritma | EdDSA (Ed25519) veya ES256 | EdDSA / ES256 | EdDSA / ES256 |
| Claim'ler | `typ`, `iss`, `aud`, `sub`, `iat`, `exp`, `sv`, iş bayrakları (`legal_ok`, `tier`) | `typ`, `jti`, `iss`=çağıran servis, `aud`=hedef, `sub` (opsiyonel), `act`, `iat`, `exp` | `typ=admin`, `iss`, `aud`, `sub`, `exp`, `sv`, (`roles`) |
| TTL | kısa (örn. 15 dk) | çok kısa (30–45 sn) | kısa (örn. 15 dk) |
| Taşıyıcı | `Authorization` | `X-Service-Auth` | `Authorization` |
| Replay koruması | – | Opsiyonel (aşağıda) | – |
| İptal | `sv` < Redis'teki minimum sürüm → red | – | `sv` == DB'deki sürüm |

**Kurallar:**
- **Asimetrik imza zorunludur.** Simetrik (HS256) anahtarda her doğrulayıcı aynı zamanda imzalayıcıdır: secret'ı bilen tek bir servis (veya sızan bir `.env`) her aktörü ve her kullanıcıyı taklit edebilir; actor allowlist'i yalnız dürüst çağıranları kısıtlar, güvenlik sınırı olmaz. OWASP Microservices Security Cheat Sheet'in kabul ettiği iki desen mTLS ya da STS'nin **public** anahtarıyla doğrulanan token'dır; NIST SP 800-204B servis kimliği için mTLS'i "de facto" sayar.
- Her imzalayıcı (gateway, auth, her servis) kendi anahtar çiftine sahiptir; `iss` = imzalayanın adı. Doğrulayan taraf `iss` → JWKS eşlemesini paylaşılan salt-okunur bir JWKS dosyasından veya her servisin `/.well-known/jwks.json` ucundan okur (Spring Security `JwtIssuerAuthenticationManagerResolver`).
- Rotasyon: JWKS'te `kid`; yeni anahtar yayınlanır, eski anahtar bir TTL boyunca doğrulamada kalır, sonra kaldırılır. Restart gerekmez.
- Üç yüzey birbirinin yerine kabul edilmez: farklı `typ` claim'i **ve** farklı anahtar (RFC 8725 bölüm 3.11–3.12: karşılıklı dışlayıcı doğrulama kuralları).
- Private key'ler compose `secrets:` / config tree ile mount edilir (Bölüm 15.3); config reposunda literal anahtar/secret bulunmaz (RFC 8725 bölüm 3.5: düşük entropili anahtar tek token'dan brute-force edilir).

**Replay koruması (`jti`) kararı:** RFC 9700 (OAuth 2.0 Security BCP) bearer token replay'ine karşı `jti` deposu yerine **sender-constrained** token (mTLS RFC 8705 / DPoP RFC 9449) + sıkı `aud` önerir. İç ağda TLS + 45 sn TTL + `aud` varken, her istekte Redis `SET NX` yapan bir replay guard ~45 sn'lik aynı-servis replay koruması satın alır; bedeli her çağrıda Redis RTT ve sert bir availability bağımlılığıdır. Karar: mTLS varsa replay guard **yok**; yoksa yalnız gateway → servis (dış kaynaklı) token'larda, **TTL sınırlı** bir depoda (asla boyut sınırlı — Spring Security'nin DPoP `jti` cache'i bu yüzden CVE aldı) ve fail politikası açıkça yazılmış olarak.

**Sonraki adım (opsiyonel):** mTLS iç CA ile (step-ca; Spring Boot SSL bundle hot-reload, `server.ssl.client-auth=need`, kimlik = SAN) veya SPIFFE/SPIRE (compose'da çalışır; Docker workload attestor; `aud`-bound JWT-SVID ev yapımı service JWT'nin yerine geçer). Service mesh yalnız Kubernetes'te anlamlı.

### 9.3 Gateway

Filtre sırası:

1. **Trace:** trace başlatılır, `X-Trace-Id` eklenir, erişim logu yazılır.
2. **`/internal/**` engeli:** 404 döner.
3. **IP engeli:** Redis'teki `ip-block:<sha256(ip)>` kaydına bakılır.
4. **User JWT doğrulama:** imza, `iss`, `aud`, `exp`.
5. **Oturum sürümü:** token `sv` değeri < Redis minimumu ise anonim sayılır.
6. **Service JWT basma:**
   - `aud` = route metadata'daki `audience`. Metadata yoksa istek fail-closed ilerler.
   - İstemcinin gönderdiği `X-Service-Auth` **ezilir**.
   - `X-Forwarded-For` normalize edilir.

```yaml
spring.cloud.gateway:
  default-filters: [AddResponseHeader=X-Gateway, on]
  routes:
    - id: order
      uri: http://order:8081
      predicates: [Path=/orders/**]
      metadata: { audience: order-api }
```

**Kurallar:**
- Gateway, istemciden gelen iç header'ları temizler (`RemoveRequestHeader`): `X-Subject-Id`, `X-User-*`.
- CORS origin listesi açıkça tanımlanır, `*` kullanılmaz.
- WebSocket için token query parametresinde taşınmaz. Taşınması zorunluysa erişim logundan maskelenir.
- **Gateway'de rate limit vardır:** `RequestRateLimiter` (Redis token bucket: `replenishRate`, `burstCapacity`, `KeyResolver` = IP veya `sub`). Kimliksiz istek seli auth servisine ulaşmadan kesilir; downstream limitler iş kuralı içindir.
- İstek başına toplam timeout bütçesi gateway'de tanımlıdır (Bölüm 4.7).
- `/internal` kontrolü `StripPrefix` **sonrası** path üzerinde de yapılır.
- Gateway `X-Forwarded-*` header'larını yalnız güvenilen proxy'den kabul eder (trusted-proxy ayarı; CVE-2025-41235).

### 9.4 `ServiceJwtVerificationFilter`

```
0. Path decode + normalize. Çift kodlama, '..'/'.', '//', '\', NUL → 400
1. exclude-paths → doğrulama yok
2. X-Service-Auth yok → 401
3. typ / iss (bilinen imzalayıcı) / kid → JWKS'ten public key / imza / aud / exp(+leeway) → 401
4. [opsiyonel] jti daha önce görülmüş → 401   (jti yetki kontrolünden ÖNCE tüketilir)
5. sub → attr x.accountId ; act → attr x.actor   (act == iss olmalı; farklıysa 401)
6. internal-access FIRST-MATCH: eşleşen ilk kuralda act ∉ allowed-actors → 403
7. /internal/** için eşleşen kural yok → 403 (DEFAULT-DENY)
8. sub varsa SecurityContext kurulur; finally'de temizlenir
```

**Kural:** Allowlist eşleşmesi decode ve normalize edilmiş path üzerinde yapılır. Ham URI ile eşleştirme, `%61` gibi kodlanmış karakterlerle atlatılabilir.

### 9.5 `internal-access` Allowlist

```yaml
service-jwt:
  audience: order-api              # bu servisin aud'u
  service-name: order-service      # bu servisin act'i ve imzalarken iss'i
  private-key-path: /run/secrets/order-service-signing-key   # config tree ile mount edilir
  jwks-path: /run/config/service-jwks.json                   # iss → public key; kid ile rotasyon
  ttl-seconds: 30
  replay-guard: { enabled: false }  # mTLS yoksa ve dış kaynaklı token'lar için gerekiyorsa açılır; TTL sınırlı depo
  exclude-paths: [/actuator/health, /actuator/info, /actuator/prometheus]
  internal-access:
    # İlk eşleşen kural kazanır: dar kurallar catch-all'dan ÖNCE yazılır.
    - path: "/internal/orders/*/status"
      allowed-actors: [ "payment-service" ]
    - path: "/internal/orders/**"
      allowed-actors: [ "backoffice-service" ]
```

**Kurallar:**
- Allowlist gerçek kullanım kadar dar tutulur.
- Hesabı body'den alan internal uç hiçbir aktöre açılmaz.
- Public path'ler de aktöre kısıtlanabilir (örn. yalnız `gateway`).
- Allowlist tek kaynaktan üretilir, veya local yml ile config repo arasındaki fark CI'da test edilir.

**Kaçın:** Kuralı local yml'e ekleyip deploy config'ine eklemeyi unutmak. Bu durumda deploy ortamında 403 alınır.

### 9.6 Rate Limit

- Redis üzerinde Lua ile atomik **fixed window**:

```lua
local current = redis.call('INCRBY', KEYS[1], ARGV[2])
if current == tonumber(ARGV[2]) then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
return current
```

- Key formatı `rl:<scope>:<sha256(scope \0 key)>`; ham PII Redis'e yazılmaz.
- Config: `rate-limit.rules.<scope>.{limit,window-seconds}`.
- Fail davranışı:

| Durum | Sonuç |
|---|---|
| Kural tanımlı değil | 503 (fail-closed) — config hatası, üretime çıkmadan yakalanmalı |
| Redis hatası | Scope başına karar: güvenlik yüzeyleri (OTP, login, refresh) **fail-closed 503**; iş yüzeyleri (arama, listeleme) **fail-open** + `rate_limit_store_failures_total` metriği ve alarm (Stripe'ın rate limiter'ları bilinçli fail-open'dır) |
| Limit aşıldı | 429 + `Retry-After` header'ı |

- Scope'lar anahtar tipine göre ayrılır: `<aksiyon>-ip`, `<aksiyon>-account`, `<aksiyon>-transaction`, `<aksiyon>-device`.
- Kapsanacak yüzeyler: login, OTP, refresh, arama, mesaj, upload, rapor, satın alma.
- **Kural:** Sayaçlar JVM belleğinde tutulmaz.

**Redis/Valkey güvenlik state'i kuralları (rate limit, session version, replay, IP block):**
- Bu key'ler **cache ile aynı instance'ta tutulmaz**. `maxmemory-policy allkeys-*` cache instance'ında, güvenlik instance'ında `noeviction` + AOF (`appendfsync everysec`). Aksi halde bellek dolunca Redis session-version ve replay key'lerini sessizce evict eder ve koruma kalkar.
- Güvenlik instance'ı için HA: Sentinel (≥3 sentinel, `min-replicas-to-write 1`) veya managed servis. Asenkron replikasyon nedeniyle failover'da son yazmalar kaybolabilir; bu, güvenlik key'leri için kabul edilebilir (yeniden yazılır) ama tasarımda bilinir.
- Her key tipi için fail-open/fail-closed kararı tek tabloda (README) belgelenir; gateway ve servisler aynı tabloyu uygular.

### 9.7 Kimlik Doğrulama Desenleri

**OTP:**
- `SecureRandom` ile 6 haneli kod üretilir.
- `sha256(salt:code)` olarak saklanır, sabit zamanlı karşılaştırılır.
- TTL ve deneme sınırı vardır.
- Bilinmeyen numaraya gönderim yapılmaz ama yanıt aynıdır (enumeration koruması).
- Test profilinde whitelist dışındaki numaralara sabit kod verilir.

**Çok adımlı giriş:** OTP doğrulaması oturum açmaz.
- Doğrulama sonucunda `nextStep` ve kısa ömürlü bir `stepToken` (`<uuid>.<secret>`) döner. DB'de yalnız `sha256(secret)` tutulur.
- Step token, oturum sürümüne bağlıdır.

**Şifre:**
- BCrypt kullanılır; 72 byte sınırı uzunluk kuralına yansıtılır.
- Politika (min uzunluk, deneme sayısı, kilit süresi) dinamik parametrelerden okunur.
- Hatalı denemede kilit uygulanır; `details` içinde `retryAfterSeconds` döner.

**Refresh token:**
- Opak değer; `sha256(plain:salt)` olarak saklanır.
- **Aile rotasyonu** yapılır: iptal edilmiş token tekrar kullanılırsa bütün aile iptal edilir.
- Mutlak süre uzamaz. İptaller `noRollbackFor` ile kalıcıdır.

**Oturum sürümü (`sv`):**
1. Şifre sıfırlama, logout-all veya ban sonrası `session_version + 1` yapılır ve refresh token'lar iptal edilir.
2. Commit sonrası Redis'e `user-session:min-version:<id>` yazılır. Lua script yalnız büyük değeri yazar.
3. Gateway ve realtime servisi bu sürümün altındaki token'ı reddeder.
4. Bir reconciler bu değeri periyodik olarak yeniden yayınlar.

**Kural:** Ban dahil tüm hesap kapatma olayları `sv`'yi artırmalıdır. Aksi halde açık access token'lar ve WebSocket bağlantıları TTL dolana kadar çalışmaya devam eder.

**SMS OTP'nin yeri:** NIST SP 800-63B-4 PSTN üzerinden OTP'yi (SMS/sesli) tek "restricted" authenticator sayar (SIM swap, SS7). Kullanıcı girişi için kabul edilebilir ama: belgelenmiş risk değerlendirmesi, en az bir restricted-olmayan alternatif (passkey, TOTP, e-posta magic link) ve geçiş planı bulunur. **Admin için SMS 2FA kullanılmaz.**

**SMS pumping (toll fraud) savunması** (`otp/start` ucu için zorunlu):
- Hizmet verilmeyen ülke kodları engellenir (geo-permissions); `libphonenumber` ile doğrulama, VoIP/premium aralık reddi.
- Numara, IP ve **cihaz** başına günlük tavan; kısa pencerede farklı numaralara gönderim tespiti (velocity).
- "OTP gönder" öncesi bot tespiti / cihaz attestation sinyali (aşağıda).
- Sağlayıcı harcamasında alarm ve otomatik kesici (saatlik tavan aşılınca OTP gönderimi durur, admin uyarılır).

**Cihaz attestation (mobil public API):** Google **Play Integrity** (`deviceIntegrity`, `appIntegrity`, `recentDeviceActivity`, `appAccessRisk`, `deviceRecall`; standard request + `requestHash`, sunucuda doğrulama) ve Apple **App Attest** (donanım anahtar attestation + istek başına assertion) + **DeviceCheck** (cihaz başına 2 bit; "deneme hakkı kullanıldı" için). Kayıt, OTP-gönder, satın alma ve suistimale açık aksiyonlarda **kademeli risk sinyali** olarak; ikili kapı olarak değil (rooted cihaz/token relay ile aşılabilir).

**Admin 2FA:**
1. E-posta ve şifre doğrulanır.
2. İkinci faktör **passkey/WebAuthn** (phishing'e dirençli; Spring Security 6.4+ `webAuthn()` DSL, `/webauthn/register`, `/login/webauthn`, JDBC repository'leri) veya **TOTP** (RFC 6238). SMS yalnız kurtarma kanalı olarak, o da tek kullanımlık kodla.
3. Doğrulama sonrası access token yanıt gövdesinde döner. Refresh token **HttpOnly, Secure, SameSite=Strict, dar Path**'li cookie'ye yazılır. Ayrıca JS'in okuyabildiği bir CSRF cookie'si verilir.
4. Refresh ve logout isteklerinde `X-CSRF-Token` header'ı gönderilir (double-submit).

Diğer kurallar:
- Kurtarma kanalı OTP'si de tuzlu hash ve sabit zamanlı karşılaştırma kullanır.
- Filtre her istekte admin hesabının aktif olduğunu ve `sv` eşitliğini doğrular. Bu kontrolü yapamazsa fail-closed davranır.
- Admin rolü değişince `sv` artırılır; roller token'da taşınıyorsa değişiklik anında etkili olur.

**Kaçın:**
- OTP kodunu outbox payload'ında düz metin bırakmak. İletim sonrası temizleyin veya şifreleyin.
- Bilinen parolalı bir admin hesabını migration ile tohumlamak. Bunun yerine tek seferlik bootstrap runner kullanın; parola env'den gelsin ve ilk girişte değiştirilsin.
- BCrypt'e 72 byte'tan uzun girdi vermek (kütüphane sürümüne göre eşleşme bypass'ı olabilir); uzunluk kuralı ve kütüphane yaması birlikte tutulur.

### 9.8 RBAC ve Audit (Yönetim Servisi)

- Roller migration ile tohumlanır (örn. `ADMIN`, `MODERATOR`, `SUPPORT`).
- Yetkiler `@PreAuthorize("hasRole(...)")` ile sınıf ve metot seviyesinde verilir. Gerekirse servis içinde ek kontrol yapılır (örn. "yalnız atanan kişi veya ADMIN").
- Roller token'da taşınıyorsa rol değişikliği TTL kadar gecikir. Anında etki gerekiyorsa roller her istekte kaynaktan okunur ve panel de aynı kaynağı kullanır.
- **Audit:** `@Audited(action, entityType)` + `@Around` aspect. Başarı ve hata ayrı satır olarak yazılır.
  - `details` alanı **allowlist** ile doldurulur. Anahtar, şifreli içerik, serbest metin, ham IP ve parola yazılmaz.
  - Audit yazılamazsa iş sonucu değişmez, yalnız hata loglanır.
  - Tablo değiştirilemezdir: JPA `@Immutable` + DB seviyesinde `REVOKE UPDATE, DELETE` veya trigger.

### 9.9 Hassas İçerik ve Privacy

**Uçtan uca şifreli içerik (opsiyonel desen) — dürüst sınıflandırma:**

| Şema | Ne verir | Ne vermez |
|---|---|---|
| **"E2EE-lite":** kullanıcı başına statik X25519 anahtarı + HKDF(sohbet id) ile türetilen tek sohbet anahtarı + AES-GCM | Sunucu içeriği okuyamaz (at-rest gizlilik) | Forward secrecy yok (bir private key sızarsa tüm geçmiş ve gelecek çözülür); post-compromise security yok; anahtar dağıtımı sunucuda → sunucu/DB erişimi olan MITM yapabilir; çoklu cihazda anahtar kopyalama ve nonce çakışma riski (NIST SP 800-38D: rastgele IV'li GCM'de anahtar başına 2^32 sınırı) |
| **Signal Protocol** (X3DH/PQXDH + Double Ratchet) veya **MLS** (RFC 9420; 2 kişilik grup dahil, cihaz = yaprak) | FS + PCS, cihaz başına kimlik, safety-number ile anahtar doğrulama | İstemci karmaşıklığı; Dart için resmi libsignal binding'i yok (platform channel ile Java/Swift, veya OpenMLS Dart) |

- Ürün "uçtan uca şifreli" iddiasıyla pazarlanacaksa Signal/MLS; değilse gizlilik politikasında "sunucu içeriği okuyamaz" ile sınırlı ve **FS/MITM sınırlamaları yazılı**.
- Her iki şemada da: istemci karşı tarafın anahtarını **pin'ler** (TOFU) ve değişince uyarır; sunucu yalnız ciphertext saklar, boyut ve alıcı eşitliğini doğrular; push payload'ında içerik yok.
- **Şikayet/moderasyon kanıtı** için doğru desen **message franking**: gönderici mesaj başına rastgele anahtarla düz metin üzerinden HMAC commitment üretir; sunucu düz metni görmeden bu tag'i imzalayıp saklar; şikayette raporlayan düz metin + anahtarı verir, sunucu hem imzasını hem commitment'ı doğrular (AES-GCM key-committing olmadığı için ayrı HMAC şart). Sohbet anahtarının yönetim paneline verilmesine gerek kalmaz. Kanıt, yalnız yetkili adminin tarayıcısında görüntülenir; her erişim audit'e yazılır, yanıt `no-store` döner, kanıt retention sonrası silinir.
- **Kaçın:** GPL lisanslı istemci kripto kütüphanesini kapalı kaynak uygulamaya bağlamak; denetlenmemiş kripto implementasyonuna güvenmek.

**Privacy kuralları:**
- Kişisel veri minimize edilir. Ürün DPIA (veri koruma etki değerlendirmesi) ile başlar; kişisel veri sayılan alanlar (telefon, e-posta, kimlik no, **araç plakası**, konum, belge) aydınlatma metninde ayrıca sayılır.
- Düşük entropili kimlikler (telefon, plaka, e-posta) için düz SHA-256 yeterli değildir; secret pepper'lı **HMAC** (arama için) + şifreli orijinal (gösterim için).
- Bir kimlik üzerinden **varlık oracle'ı** verilmez: kayıtlı olmayan, gizli ve engellenmiş kayıtlar için tek tip "sonuç yok". Kimlik sorgulama uçları hız limitli, cihaz bazlı velocity alarmlı ve audit loglu.
- Arama index'i, cache, log ve backup da aynı kurallara uyar.
- Kişisel veri dönen uçlar `no-store` döner.
- Kişisel veri dışa aktarımı (export) keyset sayfalı internal uçlarla yapılır.

**Konum gizliliği** (konum tabanlı ürünlerde):
- Rastgele kaydırma (fuzzing) **yetersizdir**: istek başına farklı gürültü tekrarlı örneklemeyle ortalanır (Polakis vd., CCS 2015); mesafe gizlense bile "X km içinde mi" gibi ikili filtreler **oracle trilateration** ile ~2 m hassasiyete iner (KU Leuven, USENIX Security 2024: 15 dating app'in 6'sında kesin konum).
- Kural: kullanıcı başına **deterministik** grid hücresi veya sunucuda saklanan sabit seed'li ofset (~1–3 km; kullanıcı hareket etmeden değişmez); aynı yuvarlanmış nokta **geo sorgusunda ve filtrelerde de** kullanılır (yalnız gösterimde değil); mesafe aralık olarak gösterilir; mesafeye göre kesin sıralama yapılmaz; minimum yarıçap; konum sorguları hız limitli.

**Silme hakkı (KVKK Md. 7/11, GDPR Md. 17, Apple Guideline 5.1.1 uygulama içi hesap silme):**
- Silme, servisler boyunca bir **silme saga'sı**dır: `account.deletion_requested` event'i → her servis kendi verisini siler/anonimleştirir (idempotent `DELETE /internal/<kaynak>/accounts/{id}`) → tamamlanma kaydı ve kanıt. KVKK için 30 gün, GDPR için 1 ay içinde tamamlanır; üçüncü taraf işleyicilere (SMS, push, analytics) silme bildirilir.
- Kapsam: DB şemaları, arama index'i (delete-by-query; segment merge'e kadar fiziksel kalır), cache key namespace'i, object storage (**tüm versiyonlar** + CDN purge), loglar (pseudonymize), yedekler ("beyond use" + restore sonrası yeniden silme).
- Yasal saklama gereken kayıtlar (ödeme, moderasyon, audit) anonimleştirilir, silinmez; hukuki dayanağı yazılıdır.
- Yedeklerdeki veri için **crypto-shredding**: kullanıcı başına veri anahtarı; silmede anahtar yok edilir, tüm kopyalar okunamaz olur.

**Dosya yükleme (görsel pipeline):**
```
istemci ─presigned PUT─► private QUARANTINE bucket ─► worker: magic byte/boyut/pixel-bomb doğrulama
   → yeniden kodlama (EXIF/GPS/cihaz metadata'sı GİDER; orientation uygulanır) → boyutlar/thumbnail
   → moderasyon: CSAM hash eşleme (PhotoDNA / Google CSAI Match) → NSFW sınıflandırıcı → gerekirse insan incelemesi
   → private DELIVERY bucket (rastgele, tahmin edilemez key) ─► istemciye kısa ömürlü signed GET / imzalı CDN URL
```
- **Kaçın:** public bucket'a doğrudan yükleme. Ham telefon fotoğrafı EXIF GPS ile kullanıcının ev adresini sızdırır; public URL bir kez sızınca süresiz çalışır, kullanıcı silse de CDN'de kalır, kazıma ve ters görsel arama ile kimlik tespiti kolaylaşır.
- Yükleme sonrası HEAD, içerik ve ETag doğrulaması yapılır. Temp → final taşıma yapılır, yetim dosyalar temizlenir (cron + dağıtık kilit).
- Kullanıcı üretimi içerik barındıran uygulamalar için mağaza gereksinimleri (Apple Guideline 1.2: filtre, zamanında yanıtlanan raporlama, engelleme, iletişim) ve bölgesel yükümlülükler (NCMEC raporlama, EU DSA notice-and-action, UK OSA) tasarımda hesaba katılır.

### 9.10 Güvenlik Süreci: Tehdit Modelleme, Tarama, Test

Güvenlik bir review skill'i değil, süreçtir. Asgari döngü:

| Ne | Ne zaman | Nasıl |
|---|---|---|
| **Tehdit modeli** (STRIDE-lite) | Her yeni özellik/uç/entegrasyon tasarımında; yılda bir sistem geneli | 4 soru: ne inşa ediyoruz (veri akış diyagramı: güven sınırları), ne yanlış gidebilir (STRIDE: spoofing, tampering, repudiation, info disclosure, DoS, elevation), ne yapacağız (kontrol → bu dokümandaki bölüm), yeterince iyi mi. Çıktı ADR veya `docs/threat-model/<özellik>.md`. |
| Bağımlılık taraması | Her PR + haftalık | Dependabot/Renovate + GitHub Advisory/OSV; OWASP dependency-check veya Trivy (`trivy fs`, `trivy image`); CRITICAL/HIGH bulgu PR gate'i. |
| Image taraması | Her build | Trivy/Grype; base image güncel; non-root doğrulaması. |
| Secret taraması | Pre-commit + CI + geçmiş | gitleaks (`--log-opts` ile tüm geçmiş). |
| Statik analiz | Her PR | ErrorProne/Checkstyle (özel kurallar: throw öncesi log, literal fallback yok), SpotBugs find-sec-bugs, Semgrep (Spring kural seti). |
| DAST / API testi | Release öncesi | OWASP ZAP API scan (OpenAPI çıktısından); auth'suz erişim, IDOR, rate limit testleri otomasyonda. |
| Pentest | İlk prod + yılda bir + büyük özellik (ödeme, mesajlaşma, kimlik) sonrası | Dış ekip; kapsam: mobil API, panel, gateway, servisler arası kimlik. Bulgular issue + ADR. |
| Erişim gözden geçirme | Çeyreklik | Admin rolleri, DB rolleri, CI secret'ları, bucket politikaları, JWKS anahtarları; kullanılmayanlar kaldırılır. |
| Anahtar/secret rotasyonu | JWT anahtarları 6 ay (`kid` ile kesintisiz), DB parolaları yıllık, sızıntı şüphesinde anında | Prosedür `docs/runbooks/secret-rotation.md`. |
| Güvenlik olayı | Her olayda | Bölüm 8.8 süreci + yasal bildirim süreleri (KVKK: Kurul'a 72 saat içinde ihlal bildirimi; GDPR: 72 saat) runbook'ta. |

**Kural:** Kimlik, ödeme, mesajlaşma, dosya yükleme ve moderasyon alanlarındaki değişiklikler tehdit modeli olmadan tasarlanmaz; `proj-security-review` bunu ister.

---

## 10. Veri Katmanı: PostgreSQL, Flyway, JPA

### 10.1 Sahiplik

- Her servis kendi schema'sında **ve kendi DB rolüyle** çalışır: `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?currentSchema=<schema>`, `username=svc_<schema>`.
- Entity'lerde `@Table(name = "...", schema = "...")` her zaman açıkça yazılır.
- **Kural:**
  - Hiçbir modül başka modülün tablosuna, schema'sına, repository'sine, entity'sine veya migration'ına erişmez. Yönetim servisi de istisna değildir.
  - Cross-schema FK ve join yasaktır. Başka servise ait kimlikler FK olmadan düz UUID olarak tutulur.
  - Veri ihtiyacı API, event veya read-model (Bölüm 4.6) ile karşılanır.
  - Bu hem mimari hem de **yetki/veri sahipliği** riski olarak ele alınır.
- **Zorlama — DB seviyesinde (birincil):** Sınır testle değil, GRANT ile korunur. Bir `nativeQuery` başka şemaya erişmeye kalkarsa çalışma zamanında hata alır:

```sql
CREATE ROLE svc_order LOGIN PASSWORD '<secret>';
CREATE SCHEMA "order" AUTHORIZATION svc_order;          -- şema sahibi = servis rolü; Flyway bu rolle koşar
REVOKE ALL ON SCHEMA public FROM PUBLIC;
-- svc_order başka şemada USAGE yetkisine sahip değildir; cross-schema join çalışmaz.
```

  Microsoft Azure Architecture Center ve AWS Prescriptive Guidance aynı fiziksel sunucuyu paylaşmayı kabul eder; sorun şema/tablo paylaşımıdır. Rol ayrımı ileride şemayı ayrı instance'a taşımayı da kolaylaştırır (Bölüm 24).
- **Zorlama — testle (ikincil):**
  - Migration dosyalarında başka schema adı geçmesini yasaklayan test.
  - HTTP client metotlarında yanlış modül DTO'su kullanılmasını yasaklayan test.
  - ArchUnit: `*-core` → başka `*-core` sınıfı yok; Maven enforcer `bannedDependencies`.
- **Paylaşılan Redis key'leri** örtük bir contract'tır. Formatları `platform-core` içinde tek `RedisKeys` sınıfında tutun veya bu paylaşımı API/event'e çevirin.

**Veri ayrımı merdiveni** (Bölüm 24 eşikleriyle birlikte okunur):
1. Servis başına rol + şema, tek instance (başlangıç).
2. Aynı instance'ta servis başına ayrı *logical database* (bağımsız `pg_dump/restore`, extension'lar).
3. En sıcak/en büyük domain (örn. mesajlaşma, olay logu) için **ayrı instance**.
4. Tam database-per-service — yalnız ayrı ekipler ve ayrı on-call varsa.

### 10.2 Flyway

| Konu | Kural |
|---|---|
| Konum | `<servis>-core/src/main/resources/db/migration` |
| İsim | `V<n>__<snake>.sql` (artan tamsayı), `R__<ad>.sql` |
| İlk dosya | `V1__init_schema.sql`: `CREATE SCHEMA IF NOT EXISTS <schema>;` |
| Ayar | `enabled: true`, `locations: classpath:db/migration`, `baseline-on-migrate: true`, `schemas: <schema>`, `clean-disabled: true` |
| JPA | `ddl-auto: validate`. Flyway ile `update`/`create` birlikte **kullanılmaz**. |
| Değişmezlik | Base branch'teki `V*.sql` değiştirilmez, silinmez, yeniden adlandırılmaz; düzeltme yeni bir `V` dosyasıyla yapılır. Kural script + CI + AI hook ile zorlanır (Bölüm 19.4). |
| Seed | Test ve demo verisi `db/seed-<env>` gibi ayrı bir location'da tutulur ve yalnız local/test profilinde eklenir. |
| Sürüm | Sıralı; `out-of-order` ve `V9999__temp` gibi sıçramalar kullanılmaz. |

**DDL konvansiyonları:**

```sql
-- Neden bu tablo var, sahibi kim, neden FK yok: kısa yorum.
CREATE TABLE <schema>.order_item (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID        NOT NULL,
    status      TEXT        NOT NULL,                 -- enum: TEXT + CHECK (native ENUM yok)
    quantity    INT         NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),   -- her zaman TIMESTAMPTZ
    CONSTRAINT ck_order_item_status   CHECK (status IN ('OPEN','CLOSED')),
    CONSTRAINT ck_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT fk_order_item_order    FOREIGN KEY (order_id) REFERENCES <schema>."order"(id)  -- yalnız aynı schema
);
-- Hangi sorgu için: sahibin listesi, yeni→eski, deterministik tie-breaker.
CREATE INDEX idx_order_item_order_created ON <schema>.order_item (order_id, created_at DESC, id DESC);
-- Eşzamanlılık kuralı DB'de: tek aktif kayıt.
CREATE UNIQUE INDEX uq_order_item_one_open ON <schema>.order_item (order_id) WHERE status = 'OPEN';
```

**Migration inceleme kuralları:**
- Gereksiz `IF NOT EXISTS` kullanılmaz; drift'i gizler.
- NOT NULL, unique veya FK eklemeden önce mevcut veri kontrol edilir.
- Büyük tablolarda `NOT VALID` + ayrı validate, `CREATE INDEX CONCURRENTLY` kullanılır; DDL, backfill ve validate ayrı migration'lara bölünür.
- Yıkıcı değişiklikte expand/backfill/contract uygulanır; guard olarak `DO $$ … RAISE EXCEPTION`.
- Poll edilen tablolarda claim index'i bulunur.
- Index'ler gerçek bir sorguya dayanır; duplicate ve gereksiz prefix index yazılmaz.
- `ON DELETE CASCADE` bilinçli kullanılır; audit, ödeme ve yasal kayıtlar cascade ile silinmez.
- Soft delete'te unique kural partial index ile yazılır.
- Append-only tablolar trigger ile korunur.

### 10.3 Entity

| Konu | Kural |
|---|---|
| ID | **UUIDv7** (zaman sıralı); tek üretim yöntemi: `platform-core`'daki generator (`uuid-creator` `getTimeOrderedEpoch()`) veya Hibernate 7 `@UuidGenerator(style = VERSION_7)` + PostgreSQL 18 `DEFAULT uuidv7()`. **Neden:** UUIDv4 rastgele insert'lerle B-tree sayfa bölünmesi ve WAL şişmesi yaratır (ölçümler: v7 ile index ~%26 küçük, insert belirgin hızlı); v7 ile `created_at DESC, id DESC` keyset sıralaması id ile doğal olarak uyuşur. `@PrePersist` ve elle atama yasak. |
| Zaman | `Instant` ↔ `TIMESTAMPTZ`, `hibernate.jdbc.time_zone: UTC`, `jackson.time-zone: UTC` |
| Auditing | Tek yöntem (Spring Data `@CreatedDate/@LastModifiedDate`) |
| Eşzamanlılık | Eşzamanlı güncellenen aggregate'te `@Version`; çakışma 409 döner |
| Kısmi update | Kilitsiz okuyup yazan akışlar araya giren güncellemeyi ezmesin diye `@DynamicUpdate` |
| Enum | `@Enumerated(EnumType.STRING)`, DB'de `TEXT + CHECK` |
| JSON | `@JdbcTypeCode(SqlTypes.JSON)` + `jsonb` |
| İlişki | Yalnız aynı aggregate içinde, `LAZY` |
| Index/constraint | Anotasyonla değil, Flyway'de |
| Değişmez kayıt | Yalnız `@Getter`, `updatable = false`, `@Immutable` |
| Lombok | `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`. **`@Data` ve `@ToString` kullanılmaz**; hassas alan sızdırabilir. |
| Base sınıf | Opsiyonel `@MappedSuperclass BaseEntity` (id, createdAt, updatedAt, version) |

### 10.4 Repository ve Kilitleme

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
@Query("select o from Order o where o.id = :id")
Optional<Order> findByIdForUpdate(@Param("id") UUID id);
```

| Desen | Kullanım |
|---|---|
| `PESSIMISTIC_WRITE` + lock timeout | Satır bazlı sıralı güncelleme |
| `pg_advisory_xact_lock(hashtext('<anahtar>'))` | Henüz olmayan satır için check-then-act; çift, hesap veya kaynak bazlı sıralama |
| `pg_try_advisory_xact_lock` | Claim içinde "başkası alıyorsa atla" |
| `FOR UPDATE SKIP LOCKED` + lease | Kuyruk ve outbox claim (Bölüm 11) |
| `@Version` | Kısa, nadir çakışmalar |
| Unique / partial unique | Son savunma hattı |
| Sabit kilit sırası | Deadlock önleme; birden çok kilit alan akışlarda sıra dokümante edilir |

**Diğer repository kuralları:**
- Kilitsiz ön okuma için interface projection kullanılır; persistence context'e bayat entity girmez.
- Toplu yüklemede `JOIN FETCH` kullanılır (N+1 önleme).
- Filtre ekranlarında JPA Specification.
- JdbcTemplate ile CTE + `UPDATE … RETURNING`.
- **DB'siz JPQL doğrulama testi:** repository sorguları, bir `EntityManagerFactory` açılarak başlangıçta parse edilir.

### 10.5 Yedekleme, PITR, HA ve Kapasite

Bu bölüm **ilk sprint** işidir; "sonra bakarız" denen tek konu değildir.

| Konu | Kural |
|---|---|
| Yedek | Sürekli WAL arşivi + gecelik base backup → S3 uyumlu object storage. Araç: **WAL-G** (S3 uyumlu, delta, şifreleme; "<100 GB, doğrudan object storage" için en basit) veya **pgBackRest** (paralel, blok-artımlı, güçlü retention/doğrulama). Retention 7–30 gün, şifreli. |
| Restore provası | **Aylık**, otomatik: yedek ayrı bir container'a restore edilir, Flyway `validate` + smoke test koşar, sonucu alarm/rapor. Test edilmemiş yedek, yedek değildir. |
| RPO/RTO | README'de yazılı (örn. RPO 5 dk, RTO 1 sa). Arşiv gecikmesi (`archive lag`) alarmı. |
| HA | Tek compose host'unda araç ne olursa olsun HA **yoktur**. Seçenekler: managed PostgreSQL (standby + otomatik failover + PITR + dahili PgBouncer; küçük ekip için önerilen) veya ikinci host + streaming replica. Patroni ≥3 DCS node ister; tek host'ta anlamsız. |
| Bağlantı bütçesi | Her PG bağlantısı bir OS process'i. HikariCP rehberi: havuz ≈ `(çekirdek × 2) + disk`; "daha az bağlantı daha hızlı". N servis × instance × havuz hesabı README'de. **PgBouncer** transaction mode (`default_pool_size` 20; 1.21+ prepared statement destekler) `max_connections`'ı korur. |
| Gözlem | `pg_stat_statements` açık; postgres-exporter (bağlantı, replication slot, bloat, uzun transaction). |
| Büyüyen tablolar | Outbox, audit, log, olay tabloları için retention ve gerekirse **partition** politikası tanımlıdır (Bölüm 10.6). |
| Redis/Valkey | Güvenlik instance'ı AOF (`appendfsync everysec`); cache instance'ı kaybedilebilir. Sentinel veya managed. |
| Object storage | Bucket versioning + lifecycle; silme saga'sı tüm versiyonları temizler. |
| RabbitMQ | Quorum queue'lar disk'te; definitions export'u yedeklenir. |

### 10.6 Append-Only ve Yüksek Hacimli Tablolar

Mesaj, olay, log, audit gibi yalnız eklenen ve zamanla büyüyen tablolar için standart:

| Konu | Kural |
|---|---|
| Partition | Zaman bazlı **declarative partitioning** (aylık/haftalık) + **pg_partman** (otomatik partition oluşturma, `retention_keep_table=false` ile eski partition **drop**). Retention `DELETE` ile değil `DROP PARTITION` ile yapılır (metadata işlemi; bloat ve vacuum yükü yok). Eşik: tablo sunucu RAM'ini aşmadan önce. |
| Index | `created_at` üzerinde **BRIN** (append-only + fiziksel sıra korelasyonu için tasarlanmış, çok küçük). Keyset sorguları için `(partition_key, created_at DESC, id DESC)` B-tree. |
| Blob | Opak/şifreli içerik `BYTEA` + `ALTER TABLE … SET STORAGE EXTERNAL` (TOAST sıkıştırma denemesini atlar; şifreli veri sıkışmaz). Base64 `TEXT` **yasak** (%33 şişme). |
| Sıra numarası | Tüketicinin boşluk tespit edebilmesi için aggregate başına monoton `seq` (örn. sohbet başına). |
| PK | UUIDv7 (sona ekleme). |
| Ne zaman Postgres yetmez | Bölüm 24: çok-TB, >10k insert/sn sürekli, çok bölge aktif-aktif → wide-column (ScyllaDB/Cassandra). Bu eşiğe kadar partition'lı Postgres yeter; MongoDB bu iş yükünde artı getirmez. |

---

## 11. Dağıtık Tutarlılık: Outbox, Inbox, Saga, Multi-Instance

### 11.1 Multi-Instance Kuralları

- Production'da her servisin **birden fazla instance** ile çalıştığı varsayılır.
- `@Scheduled` job, poller ve worker'lar tek instance varsaymaz. İş, `FOR UPDATE SKIP LOCKED` + `locked_until` lease ile atomik olarak claim edilir. ShedLock gerekmez.
- Check-then-act (`exists` → `save`) güvenli değildir. Yerine unique/partial unique index, koşullu update veya lock kullanılır.
- Instance'lar arası state JVM'de tutulmaz; PostgreSQL veya Redis'te tutulur. Bu kapsama rate limit sayacı, cache, WebSocket üyeliği ve "tek aktif kayıt" kuralı girer.
- Poller ve consumer'lar idempotenttir. Lease süresi dolarsa veya mesaj yeniden teslim edilirse aynı iş iki kez işlenebilir.
- Lease süresi, en kötü batch süresinden uzun olmalıdır. Ayrıca sonuç yazılırken sahiplik yeniden doğrulanır (`claim_token`/`lock_token`).
- Instance-local dosyanın başka bir instance tarafından okunacağı varsayılmaz.
- Bilinçli olarak tek instance'ta çalışan davranış kod veya config'te açıkça belirtilir ve gerekçesi yorumla yazılır.
- **İzin verilen JVM-local cache:** Kısa TTL'li (5–60 sn) ve bayatlığı tolere edilen okumalar. Örnek: parametre, yasal onay veya şablon cache'i.

### 11.2 Transactional Outbox

**Kural: servis başına tek generic outbox tablosu ve tek poller** (`platform-messaging` starter'ından). Konu başına ayrı outbox tablosu/poller yazılmaz. **Neden:** aynı problemi (claim, lease, backoff, DEAD, trace) her seferinde yeniden ve farklı kalitede çözmek — bir projede 11 tablo / 7 poller / 3 farklı desen görüldü — bakım ve hata kaynağıdır. Konuya özgü olan yalnız **handler**'dır.

```
[Domain işlemi] ──aynı TX (writer: Propagation.MANDATORY)──► outbox_event (PENDING, traceparent)
        @Scheduled(fixedDelayString="${<svc>.outbox.poll-interval-ms:1000}")   (tüm instance'larda)
claim: CTE SELECT … FOR UPDATE SKIP LOCKED LIMIT n → UPDATE status='PUBLISHING', locked_until, claim_token RETURNING *
her satır: span aç → handler'a yönlendir (kind'a göre: EVENT → topic exchange publish+confirm; COMMAND → queue; HTTP → client)
 başarı → sil (veya arşiv tablosuna taşı) · hata → PENDING + retry_count+1 + next_retry_at=now+backoff
 her N denemede ERROR "*_STUCK" · kalıcı 4xx (401/403/408/429 hariç) → DEAD (iş türüne göre)
```

**Standart tablo** (Debezium Outbox Event Router şemasıyla uyumlu; ileride CDC'ye geçiş kod değişikliği istemez):

```sql
CREATE TABLE <schema>.outbox_event (
  id              UUID PRIMARY KEY,                    -- UUIDv7; aynı zamanda event id (CloudEvents "id")
  kind            TEXT NOT NULL,                       -- CHECK (kind IN ('EVENT','COMMAND','HTTP'))
  aggregate_type  TEXT NOT NULL,                       -- 'order'
  aggregate_id    UUID NOT NULL,                       -- routing/partition key; sıralama bunun içinde
  event_type      TEXT NOT NULL,                       -- 'order.order.created' (CloudEvents "type")
  payload         JSONB NOT NULL,                      -- CloudEvents "data"
  headers         JSONB NOT NULL DEFAULT '{}',         -- traceparent, tracestate, subject, dataschema, hedef (HTTP için)
  status          TEXT NOT NULL DEFAULT 'PENDING',     -- CHECK (status IN ('PENDING','PUBLISHING','DEAD'))
  retry_count     INT  NOT NULL DEFAULT 0,
  next_retry_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  locked_until    TIMESTAMPTZ,
  claim_token     UUID,
  last_error_code VARCHAR(120),                        -- exception SimpleName / HTTP status (mesaj değil)
  expires_at      TIMESTAMPTZ,                         -- domain TTL'li mesajlarda (OTP)
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_outbox_event_claim ON <schema>.outbox_event (status, next_retry_at, locked_until, created_at);
```

- `id` deterministik gerekiyorsa (`UUID.nameUUIDFromBytes(kaynak:hedef:faz)`) yazıcı sağlar; tüketici inbox bu id ile dedup yapar.
- Aynı `aggregate_id` için sıralama korunmalıysa poller aynı aggregate'in satırlarını `created_at` sırasıyla ve tek worker'da işler (claim sorgusu `aggregate_id`'ye göre gruplar) — ya da sıra gereksinimi olan olaylar RabbitMQ'da single-active-consumer queue'ya gider.

**CDC alternatifi (Debezium):** Polling publisher, Kafka/Connect yoksa "saner default"tır (Richardson) ve 1 sn poll ile ~500 ms p50 gecikme verir. **Debezium Server** (Kafka Connect gerektirmez; Kafka, RabbitMQ streams, Redis Streams, NATS, HTTP sink'leri) ile poller'lar kalkar ve gecikme ~50–200 ms'ye iner; bedeli `wal_level=logical` + replication slot yönetimidir: connector durursa veya yakalanan tablo boştayken diğer tablolar yazarsa **WAL disk'i doldurur** → `heartbeat.interval.ms` + `heartbeat.action.query`, `max_slot_wal_keep_size` ve slot lag alarmı şart. Eşik: Bölüm 24.

**Poller sabitleri:**

| Sabit | Değer |
|---|---|
| Batch | 50 |
| Lease | 120 sn |
| Lease güvenlik payı | 30 sn. Payın içine girildiyse yeni satıra başlanmaz; kalanlar `deferred` sayılır ve deneme sayısı artmaz. |
| Backoff | `min(600, 30·2^n)` sn |
| STUCK alarmı | Her 10 denemede bir |

**Loglama:**
- Batch sonunda tek bir INFO özet satırı yazılır.
- Hatalar WARN seviyesinde, `eventId` ve `exceptionType` ile loglanır.
- Hesap kimlikleri loglara yazılmaz.

**DEAD politikası:**
- Güvenlik yan etkilerinde (ban veya engel uygulaması) DEAD olmaz; iş en uzun aralıkla sonsuza kadar denenir.
- Kısa ömürlü mesajlarda (OTP) `expires_at` sonrasında satır DEAD olur.

**Payload:** Gereksiz PII veya secret taşınmaz. Hassas alan iletildikten sonra NULL'lanır.

**Yeni outbox türleri:**

| Tür | Kullanım |
|---|---|
| Sıra numaralı outbox | Durum senkronizasyonu (örn. hesap durumu). Karar DB sequence'ından sıra numarası alır, alıcı eski sırayı yok sayar. |
| Superseded kontrolü | Göndermeden önce daha yeni bir kararın olup olmadığı kontrol edilir; varsa satır gönderilmeden silinir. |

### 11.3 Idempotent Consumer (Inbox)

- Tüketici `INSERT … ON CONFLICT (event_id) DO NOTHING` çalıştırır; satır zaten varsa sessizce çıkar.
- Üretici deterministik bir `eventId` üretir.
- Hedef internal uçlar idempotenttir. Örneğin conflict sonrası "zaten uygulandı" yanıtı başarı olarak döner.

### 11.4 Local Saga (Geri Alınabilir Uzak Tüketim)

**Mekanizma seçimi:**

| Gereksinim | Mekanizma |
|---|---|
| Yalnız okuma | Saga gereksiz |
| Tüm yazımlar tek local TX'te | Local TX + constraint |
| Tek mutation'ı duplicate'ten korumak | Idempotency / uniqueness |
| Commit sonrası event veya bildirim | Outbox + idempotent consumer |
| Local commit + uzak **geri alınabilir** mutation | **Local saga** |
| Geri alınamaz etki / global atomiklik / uzun insan onayı | Saga uygun değil → mimari karar gerekir |

**Koordinatör tabloları:** Koordinatörün kendi schema'sında `saga` ve `saga_steps` bulunur.
- `saga`: `UNIQUE(account_id, scope, operation_key)`
- `saga_steps`: `next_action` (`CONFIRM`/`COMPENSATE`), `lock_token`, `locked_until`
- Index'ler: claim, expired-lease ve retention için

**Durumlar:**
- saga: `STARTED → SUCCEEDED → CONFIRMED`, `STARTED → CANCEL_REQUESTED → COMPENSATED`, `MANUAL_REVIEW`
- step: `PENDING`, `RETRY`, `RUNNING`, `DONE`, `MANUAL_REVIEW`

**Akış (`LocalSagaStore`, JdbcTemplate):**
1. `begin()`: Ayrı bir TX'te `INSERT … ON CONFLICT DO NOTHING` çalışır. Step `COMPENSATE/PENDING` olarak açılır, `next_attempt_at = now + deadline (15 sn)`. Süreç çökerse deadline dolunca otomatik telafi başlar. Kayıt zaten varsa önceki sonuç replay edilir.
2. Katılımcıya `consume` çağrısı gider (TX dışında, aynı `operationKey` ile).
3. Domain yazımı ve `success()` **aynı local TX'te**, ayrı bir bean üzerinden yapılır. `success()` compare-and-set'tir: recovery araya girip iptal ettiyse domain işlemi rollback olur.
4. Recovery worker adımları sırayla işler:
   - `claim`: SKIP LOCKED + `lock_token` + lease.
   - `prepare`: `FOR UPDATE` ile sahiplik ve durum uzlaştırması.
   - Uzak `confirm` veya `compensate` çağrısı.
   - `complete`: token eşleşmesi zorunlu.
   - Hata veya belirsiz sonuçta önce `GET` ile katılımcının durumu sorgulanır; çelişki varsa `MANUAL_REVIEW`.
5. `monitor`: 15 dk'dan eski çözülmemiş kayıtlar ERROR olarak raporlanır. `cleanup`: yalnız terminal kayıtlar 30 gün sonra silinir; `MANUAL_REVIEW` silinmez.

**Katılımcı sözleşmesi:**

```
POST /internal/<kaynak>/operations/{operationKey}/consume
GET  /internal/<kaynak>/operations/{operationKey}
POST /internal/<kaynak>/operations/{operationKey}/confirm
POST /internal/<kaynak>/operations/{operationKey}/compensate
```

- Tekillik: `UNIQUE(caller_service, account_id, operation_key)`; `caller_service` değeri JWT `act` claim'inden alınır.
- Yetki iki katmanlıdır: allowlist + kod içi "aktör → izinli işlem tipi" haritası.
- Kilit: advisory lock + `FOR UPDATE`.

| İşlem | Kayıt | Sonuç |
|---|---|---|
| consume | yok | `APPLIED` / `REJECTED` |
| consume | var | replay |
| confirm | `APPLIED` | `CONFIRMED` |
| compensate | yok | `CANCELLED` **tombstone** (geç gelen consume uygulanmaz) |
| compensate | `APPLIED` | iade → `COMPENSATED` |
| compensate | `CONFIRMED` | `MANUAL_REVIEW` (iade yok) |

İade orijinal kaynağa ve geçerlilik penceresine bağlıdır; çift iade `original_id` ile engellenir.

**İşletim değerleri:**

| Parametre | Değer |
|---|---|
| deadline | 15 sn |
| HTTP client timeout | 2 / 5 sn |
| poll | 5 sn |
| lease | 60 sn |
| max backoff | 5 dk |
| uyarı | 15 dk |
| retention | 30 gün |

Config key'leri `operation-consistency.*` altında tutulur.

**Öneri:**
- Store'u domain'den bağımsız tasarlayın: step adı parametre olsun, çok adım desteklensin.
- Zaman kaynağı olarak DB `now()` kullanın.
- Merkezi bir coordinator servisi kurmayın.

### 11.5 Zorunlu Tutarlılık Test Matrisi

**Senaryolar:**
- Normal başarı ve replay.
- Aynı key farklı body; eşzamanlı aynı key; farklı key ile aynı kaynak; aynı UUID farklı hesap veya aktör.
- Çökme noktaları: intent sonrası ve consume öncesi; katılımcı commit edip yanıt kayboldu; consume commit + domain rollback; domain commit + confirm öncesi çökme.
- Tombstone: geç gelen consume.
- confirm/compensate timeout; eşzamanlı confirm ve compensate.
- İki worker ile expired lease.
- Request success ile recovery cancel yarışı; tekrarlanan compensate.
- Hatalı girdi: eksik veya bozuk key; geçersiz JWT, yanlış actor, reuse edilmiş JTI.
- Cleanup ve monitor; migration ve restart.

**Kanıt seviyeleri:**
1. Unit ve MVC testleri
2. Gerçek PostgreSQL entegrasyonu
3. Owner→participant runtime testi
4. Release

**Sonuç:** `PASS` / `FAIL` / `BLOCKED`.

---

## 12. Mesajlaşma: Komutlar, Domain Event'leri ve Kafka Kararı

### 12.1 İki Ayrı Akış

| | **Komut** ("şu SMS'i gönder") | **Domain event** ("sipariş oluştu") |
|---|---|---|
| Anlam | Tek bir alıcıya iş emri; gönderen alıcıyı bilir | Olmuş bir gerçek; kaç tüketici olduğunu üretici bilmez |
| RabbitMQ yapısı | `<servis>.commands` direct exchange → `<hedef>.<komut>.queue` (quorum) + DLQ | `domain.events` **topic exchange**; routing key `<servis>.<aggregate>.<olay>`; tüketici başına queue + DLQ |
| Bilinmeyen tip | DLQ (bir hata) | **Yok sayılır** (ileri uyumluluk) |
| Replay | Gerekmez | Gerekebilir → RabbitMQ **Streams** (Bölüm 12.4) |
| Örnek | SMS/mail/push, dış sağlayıcıya çağrı | `order.order.created`, `account.standing.changed`, `user.block.created` |

**Kural:** Bir servis başka bir servisin **verisini** değiştirmek için komut göndermez; kendi olayını yayınlar, ilgili servis olayı tüketip kendi kararını verir. Komut yalnız gerçek "iş emri" (dış sağlayıcı entegrasyonu) içindir. "Outbox üzerinden RPC" (üreticinin tüketicinin adresini ve ucunu bilmesi) anti-pattern'dir.

**Sistemde en az şu olaylar yayınlanır** (analytics, read-model, moderasyon ve fraud sinyalleri buna dayanır): hesap durumu/rıza değişiklikleri, engel/şikayet, ana iş akışı adımları (oluşturma, eşleşme, tamamlama), abonelik/hak değişiklikleri, mesaj metadata'sı (içerik değil).

### 12.2 Event Envelope ve Şema Evrimi

**CloudEvents 1.0** yapısı: zorunlu `id`, `source`, `specversion`, `type`; opsiyonel `subject` (aggregate id), `time`, `datacontenttype`, `dataschema`, `data`; extension `traceparent`/`tracestate`. AMQP 0-9-1'de (Spring AMQP) CloudEvents'in resmi binding'i yok (yalnız AMQP 1.0); attribute'lar mesaj header'larına `ce-*` öneki ile eşlenir ve bu eşleme `platform-messaging`'de tek yerde yaşar.

**Şema evrimi kuralları (Confluent BACKWARD uyumluluğu ile aynı):**
- Alan **ekleme** (opsiyonel, default'lu) geriye uyumludur; `type` değişmez.
- Alan silme/yeniden adlandırma/tip değiştirme **kırıcıdır**: yeni `type` (veya `type` içinde `.v2`) açılır; üretici bir süre **iki olayı birden** yayınlar; tüketiciler geçince eski kapatılır.
- Tüketici bilmediği alanı yok sayar (`FAIL_ON_UNKNOWN_PROPERTIES=false`), bilmediği `type`'ı loglayıp ack'ler.
- Payload sınıfları `<domain>-api` modülünün `event` paketinde; şema registry (Apicurio, Apache-2.0) yalnız çok ekipli ortamda.
- **Deploy sırası:** tüketici önce (yeni alanı/tipi tanısın), üretici sonra.

### 12.3 RabbitMQ 4.x Konfigürasyonu

| Bileşen | Desen |
|---|---|
| Sürüm | 4.3+ (community-destekli hat; 3.13 desteği 2024-09'da bitti). Mnesia yok (Khepri); classic mirrored queue yok. |
| Queue tipi | **Quorum queue** (`x-queue-type: quorum`), `delivery-limit` (varsayılan 20 → DLX), `dead-letter-strategy: at-least-once`, `x-delivery-count` başlığı |
| Gecikmeli retry | QQ native `x-delayed-retry-type: failed` + `x-delayed-retry-min/max` (4.3; lineer backoff). **Delayed Message Exchange plugin'i kullanılmaz** (arşivlendi, Mnesia tabanlı). |
| Exchange/queue adları | komut: `<servis>.commands` / `<hedef>.<komut>.queue`; event: `domain.events` (topic) / `<tüketici>.<amaç>.queue`; DLX `<servis>.dlx`; DLQ `<queue>.dlq` |

**Producer:**

```yaml
spring.rabbitmq: { publisher-confirm-type: correlated, publisher-returns: true, template.mandatory: true }
```

- `Jackson2JsonMessageConverter` + `RabbitTemplate.setMandatory(true)`.
- Publisher, `CorrelationData` ekler ve trace header'larını enjekte eder. Broker ACK'i sınırlı süre beklenir (örn. 5 sn).
- NACK, unroutable mesaj veya timeout durumunda exception fırlatılır ve outbox retry'ı devreye girer.
- **Yayın her zaman outbox'tan yapılır.** Doğrudan `convertAndSend` kullanılmaz.

**Consumer:**
- `@RabbitListener(queues = …, containerFactory = …)`; container factory `spring.rabbitmq.listener.*` ile yapılandırılır (elle kurulan factory bu property'leri yok sayar).
- **`defaultRequeueRejected=false`** — Spring AMQP varsayılanı `true`'dur ve iş hatası fırlatan mesaj "sonsuza kadar yeniden teslim edilebilir" (Spring dokümanının ifadesi).
- Hata sınıflandırması:
  - Kalıcı hata → `AmqpRejectAndDontRequeueException` → DLQ.
  - Geçici hata (DB/ağ) → `RetryInterceptorBuilder.stateful()` + exponential backoff + `RepublishMessageRecoverer`/DLX; ya da QQ native delayed retry.
- `prefetch` açıkça (10–50; Spring varsayılanı **250**) ve `concurrency` ayarlanır.
- Idempotent handler: inbox `ON CONFLICT (event_id) DO NOTHING` (Bölüm 11.3).
- DLQ için izleme (derinlik > 0 alarmı) ve replay aracı bulunur.

**Kaçın:** Gecikmesiz requeue (`ImmediateRequeueAmqpException` ile DB kesintisinde sıcak döngü; QQ'da log/disk büyümesi). Consumer'ı prefetch'siz bırakmak.

**Kural:** Hassas veya güvenlik kritik olay tipleri (moderasyon kararı, ödeme durumu, mağaza bildirimi) kuyruk yerine yalnız imzası doğrulanmış bir internal HTTP uçtan veya webhook'tan kabul edilir. Bu tipler kuyruktan gelirse DLQ'ya düşer.

### 12.4 Replay ve Çoklu Tüketici: RabbitMQ Streams

Aynı olayı birden çok bağımsız tüketicinin okuması ve **geçmişi baştan okuma** (yeni read-model kurma, bug sonrası yeniden işleme, analytics) gerekince, Kafka'ya geçmeden önce **RabbitMQ Streams**: append-only log, non-destructive read, broker'da offset, `max-age`/`max-length-bytes` retention, Spring `spring-rabbit-stream`. `domain.events`'in bir kopyası stream'e de yazılır; işlemsel tüketiciler queue'dan, analitik/replay tüketicileri stream'den okur. Stream'de TTL/öncelik/DLX yok; bunlar queue işidir.

### 12.5 Kafka Ne Zaman?

RabbitMQ'nun kendi karşılaştırma sayfası çizgiyi net çeker: task queue, RPC, öncelik, mesaj başına durum → RabbitMQ; yüksek hacimli stream, **stateful stream processing**, log compaction → Kafka. Kafka 4.x (KRaft, ZooKeeper yok; 4.2 "share groups" ile per-message ack) hâlâ TTL/öncelik/gecikmeli retry/DLX vermez; Spring Kafka `DefaultErrorHandler` poison mesajda partition'ı bloke eder, `@RetryableTopic` sıralamayı bozar. Broker varsayılan 1 GB heap + page cache + partition planlama; tek host'ta anlamı azdır. Sektör anketleri Kafka kurulumlarının çoğunun 1 MB/sn'nin altında çalıştığını söyler — yani çoğu ekip tasarlandığı hacmin çok altında operasyon yükü taşır.

**Kafka (veya Redpanda: Kafka API, C++, BSL) şu koşullardan ≥2'si oluşunca:**
- Aynı olayı ≥3 bağımsız tüketici okuyor ve replay + **stream processing** (Kafka Streams/Flink: pencereleme, join) ihtiyacı var.
- Günlük olay hacmi on milyonları geçti; RabbitMQ streams yetmiyor.
- Veri/analitik ekibi kuruldu; warehouse'a CDC (Debezium → Kafka) gerekiyor.
- Birden fazla host/cluster var.

Bölüm 12.2'deki envelope ve topic disiplini kurulmuşsa geçiş yalnız transport değişimidir (Spring Cloud Stream veya kendi soyutlamanız); üretici/tüketici kodu aynı kalır. **Kaçın:** tüketicisi olmayan bir event log'u için ilk günden Kafka kurmak.

### 12.6 Yeni Olay/Komut Checklist'i
1. Komut mu event mi? (Bölüm 12.1) Adı belirle: `<servis>.<aggregate>.<olay>` / `<hedef>.<komut>`.
2. Payload sınıfını `<domain>-api/event` altına yaz; CloudEvents attribute'larını `platform-messaging` doldurur.
3. Consumer tarafında queue (quorum), DLQ, binding; `defaultRequeueRejected=false`, prefetch, retry politikası.
4. Listener + inbox dedup + (read-model ise) `revision` ile UPSERT.
5. Üretici: domain transaction'ında `outbox_event` satırı (kind, aggregate, type, payload).
6. Config key'leri (local + deploy). Şema evrimi notu (`type` versiyonu).
7. **Önce consumer'ı deploy et.**
8. Testler: outbox satırı yazılıyor mu (TX ile birlikte rollback), consumer idempotent mi, bilinmeyen tip yok sayılıyor mu.

---

## 13. Realtime: WebSocket/STOMP ve Redis Fan-out

| Konu | Desen |
|---|---|
| Endpoint | `/ws/<alan>`, gateway `ws://` route'u + IP engeli |
| Broker | Simple broker (`/topic`, `/queue`) + uygulama prefix'i `/app`. Yüksek ölçekte broker relay değerlendirilir. |
| Handshake | `Authorization` varsa user JWT ve `sv` kontrol edilir |
| CONNECT | Native `Authorization` **zorunlu**. Doğrulanınca kimlik, `tokenExp` ve `sv` session'a yazılır; `Authorization` header mesajdan silinir. Süresi dolmuş token'la gelen her komut reddedilir. |
| SUBSCRIBE | **Deny-all allowlist**: `/topic/<kaynak>.{id}` yalnız üyeye, `/topic/user.{id}` yalnız kendi kimliğine açık |
| Oturum iptali | Periyodik süpürücü: süresi dolan ve `sv`'si iptal edilen session'ları kapatır (Redis toplu okuma) |
| Fan-out | Kullanıcı başına Redis kanalı (`<alan>:user:{id}`). Her pod yalnız kendisine bağlı kullanıcıların kanalına abone olur. Pub/sub **at-most-once**'tır ("bir mesaj en fazla bir kez teslim edilir, hiç edilmeyebilir"). |
| Zarf | Payload + `ts` + trace + **HMAC-SHA256**. Tazelik penceresi (örn. 30 sn) ve `relayId` ile tekrar eleme. Local ortamda da imzasız zarf kabul edilmez. |
| Yayın | `AFTER_COMMIT` listener. Alıcı sayısı 0 ise push bildirime düşülür (payload'da içerik yok). |
| Sıra numarası | Her kanal/sohbet için DB'de monoton `seq`. İstemci "son gördüğüm seq=N" ile yeniden bağlanır; boşluk varsa history pull yapar. |
| Limitler | Payload boyutu, mesaj ve receipt rate limit'i |
| Endpoint | Native WebSocket; SockJS yalnız eski tarayıcı desteği gerekiyorsa (mobil istemciler için gereksiz) |

Realtime yayını outbox'lı olmadığı için kaybolabilir. İstemci tasarımı **"önce DB'ye yaz → pub/sub ile it → yeniden bağlanınca seq ile kaçırılanları çek"** olmalıdır (Centrifugo'nun tasarım dokümanındaki desen). Bağlı istemciye at-least-once gerekiyorsa Redis **Streams** (consumer group, `XACK`/`XAUTOCLAIM`, `MAXLEN`) veya Centrifugo (history + recovery); Kafka bu iş için değil. Çok pod'da STOMP broker relay yerine bu Redis modeli yeter; ölçek eşiği Bölüm 24.

---

## 14. Dinamik İş Parametreleri

Adminin değiştirebildiği iş kuralı değerleri (limit, süre, seçenek listesi) **config veya env'e konmaz**. Yönetim servisindeki parametre kataloğunda tutulur.

| Rol | Bileşen |
|---|---|
| Sahip | Yönetim servisi: `SystemParameterService`, `ParameterDefinitionRegistry` (tip ve sınır doğrulaması), internal controller |
| Contract | `<yönetim>-api`: `ParameterGroup` enum, `SystemParameterKey` enum (key + group + tip), `ParameterGroupDto`, `ParameterValues.find(...)` |
| Depolama | `system_parameter` (`value jsonb`, tipe göre CHECK) + `parameter_revision` (append-only trigger, audit FK) |
| Internal uçlar | `GET /internal/parameters/groups/{group}`, `…/since/{revision}`, `…/at?at=<instant>` |
| Admin | `PUT /backoffice/parameters/groups/{group}`: `expectedRevision` ile optimistic kontrol (409), global advisory lock. Audit + revizyon + değer aynı TX'te yazılır. |
| Tüketici | Servis başına tek `BackofficeParameterClient` + `SystemParameterProvider` |
| Kayma tespiti | Açılış kontrolü (ERROR log) + health indicator DOWN + enum↔seed↔registry↔frontend tutarlılık testleri |

**İsim ve tip:**
- Group: kebab-case. Key: `<alan>.<ad>` snake_case. Enum sabiti: UPPER_SNAKE.
- Tipler: `INTEGER` (+unit), `DURATION` (her zaman saniye), `OPTION_LIST` (`code`, `labels.tr/en`, `order`, `active`).
- Yayınlanmış key yeniden adlandırılmaz. `usage_status`: `DEFINED_ONLY` / `ACTIVE` / `PARTIAL`.

**Okuma kuralları:**
- **Doğruluk hataları fail-closed:**

| Durum | Hata |
|---|---|
| Key yok | `PARAMETER_NOT_DEFINED` |
| Değer bozuk | `PARAMETER_VALUE_INVALID` |

  Kod içi default değer, yml fallback ve hatayı yutmak yasaktır: parametre tanımsızsa bu bir **deploy hatasıdır** ve açılış kontrolünde yakalanır.

- **Erişilemezlik: bounded staleness (static stability).** Parametre kaynağı bir *control plane*'dir; düştüğünde *data plane* (tüm servisler) durmamalıdır. "5 sn cache + 503" modeli yönetim servisini her servisin tier-0 bağımlılığı yapar (yönetim paneli restart olurken ana iş akışı 503 döner). Kural:
  - Tüketici son başarılı `ParameterGroupDto`'yu (revizyonuyla) **bellekte ve diskte** (local snapshot; soğuk açılışta kaynak yoksa bile ayağa kalkar) tutar.
  - Kaynak erişilemezse grup başına tanımlı **en fazla T** süre boyunca son bilinen değer kullanılır (örn. hak limitleri 10 dk, yaş/uygunluk kuralları 1 sa); `parameter_staleness_seconds{group}` metriği yayınlanır ve eşik alarmı vardır.
  - T aşılınca yalnız **güvenlik-kritik** olarak işaretli gruplar `PARAMETER_UNAVAILABLE` (503) döner; diğerleri son bilinen değerle devam eder. Hangi grubun kritik olduğu katalogda `criticality` alanıyla tanımlıdır.
  - Bu, tutarlılığı bozmaz: kalıcı sonuç yazılırken kullanılan revizyon zaten snapshot olarak kaydedilir. AWS "static stability" ilkesi ve tüm feature-flag SDK'ları (Unleash: 15 sn poll + disk yedeği + sunucu yoksa yedekten servis; OpenFeature: provider hatasında default) aynı modeli uygular.
  - Alternatif/ek: parametre revizyonları **event** olarak yayınlanır (Bölüm 12), tüketiciler local tabloda tutar; yönetim servisi yalnız yazma yoludur.

- **Tazelik:**
  - `group(...)`: 5 sn'lik instance cache (+ bounded-staleness fallback).
  - `freshGroup`: kullanıcı girdisini doğrulayan yazma akışları (kaynak erişilemezse fallback **yok**, 503).
  - `freshGroupSince(revision)`: eski revizyonlu kayıtları kırpmak (clamp).
  - `groupAt(instant)`: geçmiş bir anın değeri.
- Birlikte anlamlı key'ler aynı revizyondan okunur. Kalıcı sonuçlara değer ve **revizyon snapshot'ı** yazılır.
- Parametre TX ve lock dışında okunur. `@Value` / `@PostConstruct` ile bağlanmaz. Worker her turda yeniden okur.
- Değer düşürüldüğünde mevcut veriyi uzlaştıran bir worker gerekir.
- Loglara parametre değeri, ham yanıt ve key adı yazılmaz.

### 14.4 Feature Flag ve Deney

Parametre kataloğu **iş kuralı değerleri** içindir. Özellik açma/kapama, kademeli çıkış (% rollout) ve A/B deneyi ayrı bir ihtiyaçtır:
- **OpenFeature** SDK (CNCF; vendor-bağımsız API, provider hatasında default) + provider: **Unleash** (self-host, tek container) veya **GrowthBook** (MIT, warehouse-native deney metrikleri) / PostHog.
- Ya da katalog `BOOLEAN` ve `PERCENTAGE` (kullanıcı-bucket'lı) tipleriyle genişletilir; küçük ekipte ikinci sistem kurmadan başlamak için yeterli.
- Kural: flag değerlendirmesi local ve bounded-staleness'lıdır; flag servisi düşünce ürün düşmez. Flag'ler geçicidir; çıkış tamamlanınca kod ve flag silinir (ölü flag alarmı).
- Deney için **olay akışı** şarttır (Bölüm 12.1): hangi kullanıcı hangi varyantı gördü + sonuç olayları → warehouse. Firebase → BigQuery export (ücretsiz, ham) istemci olayları için sıfır altyapılı başlangıçtır; sunucu olayları için "event sink" tüketicisi → Postgres `analytics` şeması / ClickHouse.

**Yeni parametre ekleme:**
1. Enum sabitini ekle.
2. Yeni migration ile seed satırını ekle.
3. Registry'ye doğrulama kuralını ekle.
4. Gerekirse frontend key listesine ekle.
5. Tüketici okumasını yaz ve `usage_status`'u güncelle.
6. Tüm tutarlılık testlerini, build'i ve migration immutability kontrolünü çalıştır.
7. Deploy sırası: önce sahip servis, sonra tüketici.

---

## 15. Konfigürasyon Yönetimi

### 15.1 Katmanlar

**Varsayılan model (Config Server'sız):** iş config'i repoda yml olarak, ortam farkı env değişkenleriyle, secret'lar mount edilmiş dosyalarla.

```
Öncelik (yüksek → düşük)
/run/secrets/*  (config tree)          → secret'lar: DB/Redis/MQ parolaları, private key'ler, sağlayıcı kimlikleri (compose secrets: ← SOPS ile çözülmüş dosyalar)
ENV (compose env_file, <env>.env)      → ortama özel değerler: host adları, portlar, log seviyesi, sampling
<modül>/config/<svc>.yml                → servisin tüm iş config'i (repoda, profil bağımsız)
<modül>/config/application.yml          → ortak: tracing, management, jackson, http client timeout'ları
<modül>/application-local.yml          → yalnız local profil
<modül>/application.yml                → port, name, config import satırları
```

Modül içi `application.yml`:

```yaml
server.port: 8081
spring:
  application.name: order
  config.import:
    - "optional:configtree:/run/secrets/"      # her dosya bir property olur (Boot 2.4+); yoksa local'de atlanır
    - "classpath:config/application.yml"
    - "classpath:config/order.yml"
  lifecycle.timeout-per-shutdown-phase: 25s
server.shutdown: graceful
springdoc: { api-docs.enabled: false, swagger-ui.enabled: false }
```

**Config Server ne zaman?** Onlarca servis, birden fazla ortam ve merkezi refresh (`/actuator/refresh`, Bus) ihtiyacı varsa. Kullanılıyorsa: (1) **secret taşımaz** — Spring Cloud Config'in güvenlik dokümanı "uygulama adını bilen her kimliği doğrulanmış istemci başka uygulamanın config'ini isteyebilir" der; (2) `optional:` **yok**, `spring.cloud.config.fail-fast=true` + retry — aksi halde sunucu düşükken servis **sessiz kısmi config** ile açılır; (3) native backend production için değil (dokümanın kendi ifadesi: "başlangıç ve test için"), git backend + TLS; (4) güncel sürüm (4.1.x native backend'de 2026'da CRITICAL kimlik doğrulamasız CVE'ler aldı); (5) config-server port'u yalnız iç ağda ve SSRF'e açık servislerden erişilemez.

Ortak `application.yml` (config reposunda):

```yaml
spring:
  jackson.time-zone: UTC
  reactor.context-propagation: auto
  rabbitmq: { publisher-confirm-type: correlated, publisher-returns: true, template.mandatory: true }
  cloud.openfeign.micrometer.enabled: true
management:
  endpoints.web.exposure.include: health,info,prometheus
  tracing: { propagation.type: W3C, sampling.probability: <oran> }
  otlp.tracing.endpoint: ${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT}
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
app.http: { connect-timeout-ms: 2000, read-timeout-ms: 5000 }
```

Servis dosyası:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?currentSchema=order
    username: ${DB_USER}
    password: ${DB_PASS}
    hikari: { maximum-pool-size: ${DB_POOL_MAX:10} }
  flyway: { enabled: true, locations: classpath:db/migration, baseline-on-migrate: true, schemas: order }
  jpa: { hibernate.ddl-auto: validate, show-sql: false, properties.hibernate.jdbc.time_zone: UTC }
services:
  inventory.base-url: http://inventory:8086
```

### 15.2 Kurallar

- Yeni bir property, URL, port, audience, issuer, rate limit, bağlantı ayarı, internal uç veya feature flag eklendiğinde şu dosyalar **birlikte** güncellenir:
  - deploy tanımı,
  - config reposundaki `<svc>.yml`,
  - `application-local.yml`.
  - Dockerfile yalnız build veya runtime davranışı değişiyorsa güncellenir.
- **Dockerfile'a config için `ENV` eklenmez.**
- Key isimleri local, test ve prod ortamlarında aynıdır.
- Güvenlik etkisi olan config değişikliği ayrıca belirtilir.
- Profil dosyaları (`-test`, `-prod`) yalnız ortam farkını taşır; iş config'i base dosyada durur.
- Admin'in değiştirebileceği değerler config'e değil, parametre kataloğuna gider (Bölüm 14).
- Secret'lar için `${ENV:literal-varsayılan}` fallback kullanılmaz. Secret eksikse uygulama açılmamalıdır (fail-fast). `${JWT_SECRET:changeme}` gibi bir fallback, env eksikse uygulamayı sessizce tahmin edilebilir anahtarla açar.
- `show-sql` prod'da kapalıdır.
- Kullanılmayan key ve bağımlılık tutulmaz.
- Local yml ↔ deploy config drift'i CI'da testle yakalanır (`scripts/config-drift-check`): allowlist, rate-limit scope'ları, servis URL key'leri iki tarafta da var mı.

### 15.3 Secret Yönetimi

| Seviye | Mekanizma | Ne zaman |
|---|---|---|
| 1 (başlangıç, zorunlu) | **Docker Compose `secrets:` + Spring config tree** (`/run/secrets/<ad>` → property). Secret dosyaları deploy sırasında CI secret'larından üretilir, `chmod 600`, `.env` yerine. | Her proje, ilk gün |
| 2 (git'te şifreli) | **SOPS + age** (CNCF sandbox): `secrets/<env>.enc.yaml`; yalnız değerler şifrelenir, diff okunabilir kalır; `.sops.yaml` recipient listesi; deploy'da `sops -d` ile çözülüp compose secret dosyalarına yazılır. Rotasyon = dosyayı güncelle + commit. | Birden fazla ortam / birden fazla kişi secret yönetiyorsa |
| 3 (dinamik) | **OpenBao** (MPL-2.0, Vault API uyumlu, Linux Foundation) veya Vault/Infisical: dinamik DB kimlikleri, audit, lease. `spring-cloud-vault-config` ile `spring.config.import=vault://`. | Onlarca servis, düzenleyici audit ihtiyacı, dinamik kimlik |

**Kurallar:**
- Secret hiçbir zaman: repoda düz metin, Config Server'da, Dockerfile `ENV`'inde, image katmanında, log'da, `.env` build context'inde.
- `.dockerignore`: `.env*`, `secrets/`, `.git`, `**/target`, `node_modules`.
- **gitleaks** hem pre-commit hem **CI**'da; tarama geçmişi de kapsar (`--log-opts`).
- Rotasyon prosedürü yazılıdır: hangi secret, kim, ne sıklıkla, nasıl (JWT anahtarları `kid` ile kesintisiz; DB parolaları PgBouncer üzerinden çift kullanıcı ile).
- Secret'ın nereden geldiği izlenebilir (CI secret adı → compose secret adı → property adı eşlemesi README'de).

---

## 16. Test Stratejisi

| Tür | Nasıl |
|---|---|
| Unit | JUnit 5 + Mockito + AssertJ. Saat test edilebilir bir `Instant now()` metodu üzerinden verilir. |
| HTTP binding | `MockMvcBuilders.standaloneSetup(controller)` + `CurrentAccountArgumentResolver` + `GlobalServiceExceptionHandler`; kimlik `requestAttr("x.accountId", …)` ile verilir (Bölüm 23.5). |
| Security erişimi | Gerçek `ServiceJwtVerificationFilter`, config'ten okunan allowlist ve sentetik token ile izinli ve izinsiz aktör testleri |
| HTTP client contract | `MockRestServiceServer` (RestClient) veya `Feign.builder()` + sahte `Client` ile giden istek (method, path, header, body) yakalanır. Interface'i mock'lamak istek oluşumunu kanıtlamaz. |
| Log privacy | Logback `ListAppender` (Bölüm 23.6) |
| JPQL doğrulama | DB'siz: `EntityManagerFactory` açılır, sorgular parse edilir |
| Gerçek DB | **Testcontainers 2.x** (`testcontainers-postgresql`, `-redis`/valkey, `-rabbitmq`) + Spring Boot **`@ServiceConnection`** (`@DynamicPropertySource` yerine), gerçek Flyway migration'ları. CI'da `ubuntu-latest` runner'da Docker hazırdır; "reuse" modu deneyseldir ve **CI için değildir** — Spring context cache + JVM başına tek container yeter. Daha ucuz alternatif: workflow `services:` bloğu. **Kural:** gerçek DB testleri CI'da **her PR'da** koşar; env ile açılıp CI'da atlanan test yok sayılır. |
| Concurrency | `CountDownLatch` + executor. İki paralel claim'in ayrık satırlar aldığı, eşzamanlı ikinci insert'in reddedildiği kanıtlanır. |
| Mimari kurallar | **ArchUnit** (`layeredArchitecture()`, `noClasses().that().resideInAPackage("..controller..").should().dependOnClassesThat().resideInAPackage("..repository..")`, `slices().should().beFreeOfCycles()`, `@Configuration` yalnız `config/`, `service.impl`'de yalnız `*ServiceImpl`, `@RequestBody` → `@Valid`). Modüler monolitte ek olarak Spring Modulith `ApplicationModules.of(App.class).verify()` + `spring.modulith.runtime.verification-enabled`. Maven enforcer `bannedDependencies` ile `*-core` → `*-core` yasağı. |
| Tutarlılık / sınır | Enum↔seed↔registry↔frontend eşleşmesi. Migration'larda başka schema adı yok. Client'ta yanlış modül DTO'su yok. Hata kodu çakışması yok. |
| Statik config | yml ve alarm kuralı dosyalarını okuyup doğrulayan testler. Local ↔ deploy config drift testi. Hook komutlarının örnek girdiyle testi. |
| Dayanıklılık | Her HTTP client için "hedef yanıt vermiyor" testi: timeout bütçesi, circuit açılması, tanımlı hata (Bölüm 4.7). |
| Yük | k6/Gatling senaryoları staging'de (haftalık ve release öncesi): p99 ve hata oranı SLO'ya karşı; sonuç kapasite planına (Bölüm 24) yazılır. |
| Contract (servisler arası) | Monorepo'da derleme zamanı tip kontrolü yeter; Pact'in kendi karşılaştırması bile "iki tarafı aynı ekip aynı repoda yazıyorsa az katkı" der. Polyrepo'ya geçilirse Pact/Spring Cloud Contract. |
| İstemci contract | CI'da her servisin `/v3/api-docs` çıktısı üretilir, birleştirilir, `openapi-diff` ile breaking change yakalanır, `openapi-generator` ile istemci client'ı (örn. `dart-dio`, `typescript-fetch`) üretilir (Bölüm 20). |

**Kurallar:**
- Önce davranış ve edge case'ler belirlenir.
- Negatif case'ler ve yetki hataları zorunludur. Her bug için regression testi yazılır.
- Poller ve check-then-act kodu için concurrency testi zorunludur.
- Kırılgan test yazılmaz: sıraya, saate, rastgeleliğe veya dış servise bağımlı olmamalı.
- HTTP binding, controller metodunu doğrudan çağırarak kanıtlanmaz. Ayırt edilebilir sentetik değerler kullanılır ve geçersiz girdide servise hiç ulaşılmadığı (`verifyNoInteractions`) doğrulanır.
- Feign interface'ini mock'lamak istek oluşumunu kanıtlamaz.
- JWT doğrulaması tamamen mock'lanmaz. Yalnız dış yan etkiler (SMS, e-posta) mock'lanır.
- Parametre kaynağı erişilemez, key yok veya değer bozuk olduğunda **hiç yan etki oluşmadığı** ve varsayılan değere düşülmediği kanıtlanır.
- Test isimlendirmesinde tek stil seçilir; açıklamalar ekibin dilinde yazılır.
- **CI'da çalışacaklar (PR gate):** değişen modüller ve bağımlılarının tüm testleri (gerçek DB testleri dahil), ArchUnit, `tsc` + lint, frontend build, migration immutability, config drift, secret taraması, OpenAPI diff. Staging'de ayrıca yük testi.

---

## 17. Yönetim Paneli (Frontend) Mimarisi

```
<panel>-web/src/
├── main.tsx  App.tsx (provider zinciri + route'lar)  testSetup.ts
├── api/        httpClient.ts, errorMessages.ts (code→metin), parameterKeys.ts, <domain>Api.ts
├── auth/       authSession.ts (bellek içi token, listener, CSRF cookie okuma)
├── context/    AuthContext, ThemeModeContext
├── components/ Layout, ProtectedRoute, RouteErrorBoundary, ui/ (DataTable, ConfirmDialog, FilterBar, …)
├── hooks/      useUrlFilters, useHotkeys, …
├── pages/      <Ad>Page.tsx (route başına bir sayfa)
├── theme/  types/  utils/ (crypto, csv, dates)
```

| Konu | Karar |
|---|---|
| Access token | **Yalnız bellekte** tutulur; localStorage ve sessionStorage kullanılmaz (testle doğrulanır) |
| Refresh | HttpOnly cookie + `X-CSRF-Token`. 401 geldiğinde tek refresh yapılır: sekme içinde paylaşılan promise, sekmeler arasında `navigator.locks`. Başarısızsa oturum temizlenir. |
| HTTP | `fetch` wrapper. `credentials: 'include'`, timeout için `AbortController`. Yanıt zarfı açılır. Hata `HttpError(status, code, userMessage)` olarak fırlatılır, mesaj kod→metin eşlemesinden gelir. |
| Yetki | `ProtectedRoute requiredRoles`. Rol matrisi **tek dosyada** tutulur; menü ve route aynı dosyadan okur. Roller sunucunun kullandığı kaynaktan alınır. |
| Filtreler | URL query'de tutulur (paylaşılabilir link) |
| State | Server state için TanStack Query önerilir. Global olan yalnız auth ve tema. |
| Güvenlik | CSV formül enjeksiyonu koruması, open-redirect koruması. Şifreli içerik tarayıcıda Web Crypto ile çözülür; anahtar extractable değildir ve kullanımdan sonra sıfırlanır. |
| Kalite | TypeScript strict + `noUncheckedIndexedAccess`, ESLint (react-hooks) + Prettier, CI'da `tsc -b` + build + test |
| Deploy | Statik `dist` reverse proxy'den sunulur. SPA fallback ve güvenlik header'ları (CSP, HSTS) proxy config'inde repoda tutulur. |

---

## 18. Build, Container, CI/CD ve Deploy

### 18.1 Image Üretimi

**Kural: image yalnız CI'da, bir kez build edilir; registry'ye push edilir; imzalanır; deploy yalnız pull yapar.** "Build once, promote everywhere." Prod sunucusunda `docker compose build` **yapılmaz**: prod CPU/disk build'e gider, "hangi commit prod'da" sorusu belirsizleşir, rollback = eski commit'i yeniden build (dakikalar, hata riski), tekrarlanabilirlik yok.

Tercih sırası:
1. **Jib** (`jib-maven-plugin`): daemon'suz, tekrarlanabilir katmanlar (bağımlılıklar/kaynaklar/sınıflar ayrı), varsayılan non-root, `-pl` listesi derdi yok (Maven reactor çözer). CI'da `mvn -pl services/order/order-core -am jib:build -Djib.to.image=ghcr.io/<org>/order:${GIT_SHA}`.
2. Spring Boot **layered jar** + `jarmode=tools extract` ile çok aşamalı Dockerfile (aşağıda) veya **Buildpacks** (`spring-boot:build-image`).

```dockerfile
# Yalnız Jib kullanılmıyorsa. Build CI'da; image tag = git SHA; deploy digest ile.
FROM eclipse-temurin:25-jre AS extract
WORKDIR /app
COPY services/order/order-core/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
RUN useradd -r -u 10001 app
USER app
WORKDIR /app
COPY --from=extract /app/extracted/dependencies/ ./
COPY --from=extract /app/extracted/spring-boot-loader/ ./
COPY --from=extract /app/extracted/snapshot-dependencies/ ./
COPY --from=extract /app/extracted/application/ ./
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s CMD wget -qO- http://127.0.0.1:8081/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

**Kurallar:**
- `.dockerignore`: `.git`, `**/target`, `node_modules`, `.env*`, `secrets/`.
- Non-root, `HEALTHCHECK` (+ `start_period`), heap tek yerden (`MaxRAMPercentage`), image tag = git SHA, deploy **digest** (`image@sha256:…`) ile — tag'ler değiştirilebilir işaretçidir.
- **cosign** keyless imza (GitHub OIDC) veya `actions/attest-build-provenance`; Syft ile SBOM. Deploy tarafı imzayı doğrular.
- Base image ve bağımlılıklar Renovate/Dependabot ile güncel tutulur.

### 18.2 Compose

- Uygulama servisleri tek compose dosyasında tanımlanır. Ortak env ve limitler YAML anchor'larıyla verilir:
  - `x-common-env`
  - `x-java-service-defaults`: `restart: always` (Docker'ın production rehberi; `on-failure:N` sonrası ölü kalan servis kimseye haber vermez), `stop_grace_period`, `mem_limit`, `cpus`, `pids_limit`, `ulimits`
- `secrets:` bloğu ile secret dosyaları `/run/secrets/` altına mount edilir (Bölüm 15.3).
- Başlatma sırası `depends_on: condition: service_healthy` ile kurulur; **her** serviste healthcheck vardır (readiness ucu).
- Servis portları dışarı açılmaz; yalnız gateway `127.0.0.1`'e bağlanır; önünde reverse proxy (nginx/Traefik; TLS, `limit_req`).
- Sıfır kesintili rollout: **`docker-rollout`** (servisi ×2 ölçekle, yeni container healthy olunca eskiyi kaldır; `container_name`/`ports` kullanılmaz) veya iki compose projesiyle blue/green + proxy swap.
- Gözlem yığını ayrı bir compose projesidir; uygulama network'üne external olarak bağlanır.
- Kapasite: host RAM = Σ(`mem_limit`) + altyapı (PostgreSQL, Valkey ×2, RabbitMQ, obs yığını) + %20 pay. İkinci instance için host kapasitesi **planlıdır** (Bölüm 24).
- Tek host compose, Docker'ın desteklediği "en basit" production seçeneğidir; Kubernetes/k3s/Swarm yalnız ≥3 host ve/veya birden fazla ekip olunca (Bölüm 24).

### 18.3 CI/CD

| Workflow | İçerik |
|---|---|
| `ci` | PR tetikler. `permissions: contents: read`, concurrency ile iptal. **Affected-module** tespiti (`dorny/paths-filter` + GIB/`-amd`) → servis başına matrix: `mvn -B -ntp verify` (Testcontainers ile gerçek DB testleri dahil, ArchUnit); başarısızsa surefire raporu artifact. Frontend: `npm ci`, lint, `tsc -b`, `npm test`, `npm run build`. Ek: gitleaks, config drift, OpenAPI diff, hook testleri. |
| `migration-immutability` | PR tetikler (`edited` dahil). Head SHA ve `fetch-depth: 0` ile checkout; `node scripts/<migration>-immutability.js check --base origin/$BASE_REF`; script'in kendi testleri. |
| `build-images` | `develop`/`release`/`main` push. Değişen servislerin image'ları Jib ile build → GHCR push → cosign imza + SBOM. Çıktı: `<servis>@sha256:…` listesi (artifact). |
| `deploy` | `release` → staging (otomatik), `main` → production (**GitHub environment protection** ile onay). Sunucuya SSH: yalnız `docker compose pull` + `docker-rollout`. Registry, digest ve imza doğrulaması. |

**Deploy script deseni** (`set -Eeuo pipefail` + `trap rollback ERR`):
1. **Ön kontrol:** Disk ve RAM, altyapıya (DB, Redis, MQ) TCP erişimi, image imza doğrulaması (`cosign verify`).
2. **Hazırlık:** Secret dosyaları üretilir (`chmod 600`), önceki digest listesi yedeklenir.
3. **Rollout sırası:** contract/enum/event tipi ekleyen sahip servis → tüketiciler → auth → gateway. Her serviste `docker-rollout`, readiness bekleme, stabilite penceresi (hata oranı ve restart sayısı).
4. Frontend `dist` deploy, gözlem yığını güncellemesi, eski image temizliği.
5. **Durum kaydı:** Mevcut ve önceki digest listesi saklanır. Hata olursa önceki digest'ler ile `docker compose up -d` (saniyeler), secret ve frontend geri yüklenir.
6. Deploy sonrası smoke test (health + 2–3 kritik uç) ve SLO panosu linki deploy çıktısına yazılır.

---

## 19. AI Destekli Geliştirme Altyapısı

### 19.1 Yapı

Bu bölümdeki her dosyanın **kopyalanabilir gerçek hali** repodaki `blueprint/` klasöründedir (`blueprint/README.md` kurulum adımlarını verir). Aynı içerik, gizli klasörler (`.agents`, `.claude`, `.github`) uygulamada görünmeyebileceği için tek dosya olarak `docs/mikroservis-blueprint-dosyalari.md`'de de bulunur.

```
AGENTS.md                          ← kanonik giriş: okuma sırası + 15 temel kural bölümü (tüm ajanlar)
CLAUDE.md / .github/copilot-instructions.md  ← yalnız "AGENTS.md'yi oku" (kural tekrarı yok, boş da değil)
.github/PULL_REQUEST_TEMPLATE.md   ← etki kutucukları + çalıştırılan skill'ler ve kararları tablosu
docs/ai/
├── repo-context.md                ← modül haritası, kimlik tablosu, SICAK YOL tablosu, yüksek sinyalli dosyalar
├── security-rules.md              ← secret, kimlik, OTP/abuse, log, privacy, dosya, DB, tedarik zinciri (tek kaynak)
├── context-boundaries.md          ← token ekonomisi: hariç klasörler, görev→yüzey tablosu
├── review-checklist.md            ← değişiklik türü → zorunlu skill'ler; makine kontrolleri; karar formatları
└── operation-consistency.md       ← mekanizma seçimi, idempotency, outbox, event, saga, doğrulama matrisi
docs/adr/0000-template.md          ← ADR şablonu
.agents/skills/<skill>/SKILL.md    ← TEK KAYNAK (+ template.md, references/*.md); 12 skill (19.3)
.claude/skills → ../.agents/skills ← symlink
.claude/settings.json              ← 3 hook tanımı
.claude/hooks/flyway-immutability.js  ← PreToolUse(Edit|Write|MultiEdit) adaptörü, fail-closed
.claude/hooks/review-gate.sh          ← PreToolUse(Bash git push): son 1 saatte review skill'i? → "ask"
.claude/hooks/review-stamp.sh         ← PostToolUse(Skill): damga
scripts/flyway-immutability.js (+ .test.js)  ← kuralın tek kaynağı; CI + hook + elle; 12 testi var
tests/ArchitectureRulesTest.java   ← ArchUnit (düz @Test): katmanlar, controller→repository yok, core→core yok, config/, @Valid, döngü yok
tests/ErrorCodeUniquenessTest.java ← tüm ErrorCode enum'ları global tekil + blok içinde + mesaj formatı
tests/ConfigDriftTest.java         ← local ↔ deploy config key kümeleri; ${ENV} placeholder'ları env şablonunda; secret fallback yok
skeleton-example/                  ← Boot 4.1.1 + ArchUnit 1.5.1 ile doğrulanmış boş iskelet: `mvn test` yeşil, 8 kasıtlı ihlal yakalandı
```

**Doğrulama notu (2026-09-28):** Test şablonları boş bir Spring Boot 4.1.1 iskeletinde derlendi ve çalıştırıldı; kasıtlı 8 ihlal (controller→repository, `@Valid`'siz body, yanlış pakette `@Configuration`, impl paketinde yabancı sınıf, çakışan/blok dışı/formatsız ErrorCode, local↔deploy rate-limit drift'i, secret fallback) her biri doğru kuralda yakalandı. Denemede çıkan dört tuzak (`blueprint/skeleton-example/README.md`): enforcer `*-core` deseni platform modülünü de yakalar (`includes` ile istisna); ArchUnit'in JUnit engine'i Boot 4 BOM'la **0 test** çalıştırıp build'i sahte yeşil bıraktı → kurallar düz `@Test`; `Properties.stringPropertyNames()` sayısal YAML değerli key'leri sessizce atar → `keySet()`; `withOptionalLayers(true)`. **Kural:** CI mimari test sınıflarının test sayısını da doğrular; "0 test" = başarısız.

### 19.2 `AGENTS.md` Bölümleri

| Bölüm | İçerik |
|---|---|
| Okuma Sırası | Görev tipine göre hangi `docs/ai` dosyasının açılacağı |
| Temel Kurallar | Gereksiz geniş tarama yapma. Doğrulanmamış bilgiyi kesin yazma. Secret'ı yeni içeriğe taşıma. Kuralı tekrar etme, ilgili dosyaya link ver. Ownership kontrolü yap. |
| Kanıt ve Varsayım Disiplini | Varsayımla kod, config, endpoint, tablo veya secret adı **uydurma**. Doğrulanamayanı "**net kanıt bulunamadı**" diye yaz. Kritik bir belirsizlik varsa kod yazmadan önce sor. Mevcut bir pattern yoksa yenisini icat etme. |
| Modül Sınırı | Cross-module DB erişimi yasak (yönetim servisi dahil). İhtiyaç varsa önce sor. |
| Migration Değişmezliği | Kural + kontrol komutu |
| API/Core Contract | Bölüm 3.3 |
| Environment/Config Etkisi | Bölüm 15.2 |
| Multi-Instance | Bölüm 11.1 |
| Yorum Disiplini | Kısa, ekibin dilinde, "neden"i anlatan yorum. Changelog tarzı yorum yasak. Business rule, güvenlik, sınır, fallback ve non-obvious kararlar için yorum zorunlu. |
| Çıktı Disiplini | Kısa, kanıta dayalı. Risk açıkça yazılır, belirsizlik gizlenmez. |

**İlkeler:**
- **Tek kaynak:** Her kural bir yerde yaşar; diğer dosyalar anchor link ile yönlendirir.
- **Standart bölümler:** Her dokümanda "Taşınmayanlar" ve "Net Kanıt Bulunamayan Alanlar" bölümleri bulunur.
- **Token ekonomisi:** `target/`, `node_modules/`, `dist/`, IDE klasörleri, loglar ve `.env` varsayılan olarak context dışında tutulur.

### 19.3 Skill Seti (12 Adet; tam metinler `blueprint/.agents/skills/`)

Üç skill (`resilience`, `event-design`, `release-readiness`) bu revizyonda eklendi; bir projede en pahalı hataların (senkron zincir, outbox üzerinden RPC, yedeksiz/alarmsız prod, destek dışı sürüm) review'da yakalanmadığı görüldüğü için.

| Skill | Kontrol ettiği | Karar formatı |
|---|---|---|
| `<proje>-resilience-review` **(yeni)** | Sıcak yol uzak çağrı sayısı (≤1), timeout bütçesi zinciri, circuit breaker/bulkhead (TimeLimiter tuzağı), senkron retry yok, fail politikası, control-plane bounded-staleness, Redis eviction, kapasite (thread/havuz), "hedef yanıt vermiyor" testi | Bağımlılık tablosu + `APPROVE…BLOCK`; sıcak yolda ikinci senkron okuma → en az `REQUEST CHANGES` |
| `<proje>-event-design-review` **(yeni)** | Komut/olay ayrımı, "outbox üzerinden RPC" yasağı, CloudEvents envelope, `revision`, şema evrimi (kırıcı → yeni type + çift yayın), RabbitMQ 4.x topolojisi (QQ, DLQ, native retry), üretici (outbox'tan yayın), tüketici (`defaultRequeueRejected=false`, inbox, read-model UPSERT), analytics sink | Olay tablosu + `APPROVE…BLOCK` |
| `<proje>-release-readiness-review` **(yeni)** | Yedek + restore provası tarihi, alarm kanalı testi, asgari alarm seti, SLO/yük testi, kapasite, digest/imza/rollback, güvenlik duruşu, **sürüm/EOL**, silme akışı/DPIA, runbook/on-call, dokümantasyon | Kontrol tablosu + `PASS/FAIL/BLOCKED`; yedek/restore/alarm eksikse `FAIL` |
| `<proje>-spring-code-review` | İnce controller, constructor injection, geniş catch yok, hedef başına tek client, throw öncesi log, Normal Flow Logging, tracing/async context, Multi-Instance Safety, dinamik parametre kuralları, **sıcak yolda okuma çağrısı yok** | 8 maddelik yapılandırılmış çıktı + `APPROVE…BLOCK` |
| `<proje>-security-review` | Trust boundary, ownership/IDOR, OTP/token/abuse, WebSocket, input/query, object storage, log/secret (`safeLogReason` katı kuralı), tracing güvenliği, privacy | Risk `CRITICAL…OK`; bulgu başına severity, dosya, kanıt, exploit senaryosu, düzeltme, test; `APPROVE / APPROVE WITH NON-BLOCKING COMMENTS / REQUEST CHANGES / BLOCK` |
| `<proje>-db-migration-review` | Flyway güvenliği, mevcut veri/expand-contract, modül sahipliği, lock/rewrite riski, tip/constraint, soft delete, index gerekçesi, seed/cascade, privacy | Risk `BLOCKER…OK` + doğrulama SQL'leri + nihai karar |
| `<proje>-api-contract-review` | DTO yeri, api→core bağımlılığı yok, entity dışarı açılmıyor, geriye uyumluluk, **HTTP parameter binding** (açık isimler, HTTP üzerinden doğrulama) | Contract riski, breaking-change riski, etkilenen çağıranlar, binding bulguları |
| `<proje>-architecture-boundary-review` | Cross-module DB/repository/entity/migration erişimi, yönetim servisi bypass'ı, api/core yönü, read-model kuralları, paylaşılan Redis/queue sözleşmeleri, dağıtık monolit sinyalleri; **kod yazılmadan önce** de çalışır | Sınır risk seviyesi, ihlal yeri, alternatif, sorular; sınır ihlali `APPROVE WITH COMMENTS` alamaz |
| `<proje>-environment-impact-review` | Güncellenmesi gereken config yüzeyleri, Dockerfile ENV yasağı, replica bağımlılığı, tracing/obs tutarlılığı | Etki özeti, güncellenen dosyalar, kanıtsız dosyalar, doğrulama komutu |
| `<proje>-operation-consistency-review` | Saga uygunluğu → implementasyon (12 adım) → doğrulama (senaryo matrisi, 4 kanıt seviyesi). `references/{assessment,implementation,verification}.md` | `saga unnecessary / existing suitable / extension required / blocked` + `PASS/FAIL/BLOCKED` |
| `<proje>-test-writer` | Anlamlı test, negatif ve yetki case'leri, concurrency, parametre, HTTP binding, safe logging testleri | Eklenen case'ler, kalan boşluklar, komutlar |
| `<proje>-<client>-integration-doc` | Değişikliğin istemciyi etkileyip etkilemediği (gate) → koddan gerçekleri toplama → versiyonlu doküman (`vN+1`) + API test koleksiyonu güncellemesi + öz-kontrol | `docs/<client>-<feature>-integration-vN.md` |

**SKILL.md şablonu:**

```markdown
---
name: <proje>-<alan>-review
description: Use this skill when reviewing <kapsam> in code review or after changes.
---

<Tek cümle görev tanımı.>

Kontrol et:
- <kısa, test edilebilir, kanıt odaklı madde>
- Net bir mevcut pattern yoksa yenisini icat etme; net kanıt bulunamadı yaz ve sor.

## <Alt alan>
- … ([ilgili kural](../<diğer-skill>/SKILL.md#anchor))

Çıktı:
1. <risk seviyesi / karar enum'u>
2. <bulgu formatı: severity, dosya, kanıt, düzeltme, test>
3. <nihai karar>
```

**Yazım ilkeleri:**
- Karar enum'ları sabittir.
- Spekülatif bulgu üretilmez; kanıt yoksa `needs verification` yazılır.
- Sorun yoksa sabit bir "bulgu yok" cümlesi kullanılır.
- Referans implementasyon yolları "doğruluk garantisi değil, örnek" notuyla verilir.

### 19.4 Hook'lar ve Makine Zorlaması

| Olay | Matcher | Dosya | Amaç |
|---|---|---|---|
| `PreToolUse` | `Edit\|Write\|MultiEdit` | `.claude/hooks/flyway-immutability.js` | Base'teki migration'a yazmayı **engelle** (exit 2, fail-closed; bozuk girdi ve git hatasında da engeller; base yoksa HEAD ağacına göre korur) |
| `PreToolUse` | `Bash` (komut `git push` içeriyorsa) | `.claude/hooks/review-gate.sh` | Son 1 saatte review skill'i çalışmadıysa kullanıcıya sor (`hookSpecificOutput.permissionDecision: "ask"`); push dışı komutlarda sessiz |
| `PostToolUse` | `Skill` | `.claude/hooks/review-stamp.sh` | `*-review`, `*test-writer`, `*integration-doc` çalıştıysa `.claude/.last-review-check` damgası |

Hook komutları **ayrı dosyalarda** yaşar; `settings.json` yalnız dosyayı çağırır. **Neden:** JSON içine gömülü shell komutunda kaçış hatası hook'u sessizce etkisiz bırakır (bir projede push gate'i bu yüzden hiç çalışmamıştı). Her hook `bash -n` ve örnek stdin ile CI'da kuru çalıştırılır (`blueprint/README.md`).

**Migration immutability script'i** (tek kaynak; CI, hook ve elle kullanım aynı kodu çağırır):
- Regex `/(^|\/)db\/migration\/(.+\/)?V[^/]*\.sql$/i`.
- `check [--base <ref>]`: `git merge-base` → `git diff --name-status --no-renames -z` (working tree ve staged dahil). `A` dışındaki her durum ihlaldir.
- `check-file <yol>`: yol base ağacında varsa korunur.
- Base sırası: argüman → env → varsayılan branch. `-` ile başlayan ref reddedilir.
- Çıkış kodları: 0 uygun, 1 ihlal, 3 doğrulanamadı (fail-closed).
- Testler geçici git repolarında `node --test` ile çalışır.

**Kural:** Hook komutları örnek girdiyle test edilir (`bash -n` + sahte stdin ile CI'da). JSON içine gömülü shell komutlarında kaçış hatası kolay yapılır ve hook sessizce etkisiz kalabilir.

**Immutability script'i doğrulanmış davranış** (`node --test scripts/flyway-immutability.test.js`, 12 test): base V dosyasını değiştirme/silme/`git mv` → ihlal; iç içe klasör korunur; branch'te eklenen V ve tüm R__ serbest; Windows ters bölü; `check-file` mutlak/göreli yol; base yokken `fail` → exit 3, `head` → HEAD ağacı; `-` ile başlayan ref reddi; CLI çıkış kodları 0/1/3.

### 19.5 Kural → Makine İlkesi

Dokümandaki bir kural, AI ajanı veya geliştirici unutsa bile **bir şey kırmıyorsa** kural değil dilektir. Her kural için "hangi makine kontrolü yakalar?" sorusu cevaplanır:

| Kural | Makine kontrolü |
|---|---|
| controller → repository yasak, core → core yasak, `@Configuration` yalnız `config/` | ArchUnit + Maven enforcer |
| `@RequestBody` → `@Valid` | ArchUnit veya ErrorProne özel kontrolü |
| throw öncesi structured log | Checkstyle/ErrorProne özel kontrolü ya da review skill |
| Migration değişmezliği | script + CI + hook |
| Secret literal fallback yok | gitleaks + config lint (regex `\$\{[A-Z_]+:[^}]+\}` secret key'lerinde) |
| Local ↔ deploy config drift | `scripts/config-drift-check` CI |
| Hata kodu çakışması | unit test (tüm `ErrorCode` enum'ları) |
| Sıcak yolda ≤1 uzak çağrı | README tablosu + review skill (otomatik: trace'te span sayısı testi) |
| Sürüm/EOL | Renovate + Bölüm 25 çeyreklik kontrol |

`.claude/skills/` elle kopya değil, `.agents/skills/`'e **symlink** veya CI'da senkron kontrolü. PR şablonunda "çalıştırılan review skill'leri ve kararları" bölümü bulunur; review izi kalır.

---

## 20. Geliştirme Süreci

| Konu | Pratik |
|---|---|
| Branch | `feature/<kişi>-<açıklama>` → PR `develop`'a. `release` test ortamına, `main` production'a deploy edilir. |
| PR kontrolleri | Testler, migration immutability, secret taraması |
| Commit | Conventional (`fix(<modül>): …`, `feat(<modül>): …`), ekibin dilinde |
| Push öncesi | İlgili review skill'leri çalıştırılır |
| Doğrulama | Değişiklik izole bir DB'ye karşı servis gerçekten ayağa kaldırılarak doğrulanır |
| API sözleşmesi (tek kaynak) | **OpenAPI üretilir, elle yazılmaz.** CI her servisin `/v3/api-docs` çıktısını alır, tek `<proje>-api.yaml`'a birleştirir, `openapi-diff` ile breaking change'i PR'da işaretler, `openapi-generator` ile istemci client paketini üretir (Flutter: `dart-dio` stable; web: `typescript-fetch`). Postman/Bruno koleksiyonu OpenAPI'den türetilir; elle üçüncü kopya tutulmaz. |
| API versiyonlama | İlk günden karar: `/v1` prefix (önerilen; mobil uygulama mağazada eski sürümüyle aylarca yaşar) veya header. Kırıcı değişiklik yeni versiyon; eski versiyon sunset tarihiyle en az N ay yaşar. |
| İstemci handoff | İstemciyi etkileyen her değişiklik için versiyonlu entegrasyon dokümanı yazılır (şablon aşağıda) — endpoint/alan listesi OpenAPI'den gelir, doküman **davranış, ekran akışı ve hata kodu → ekran** eşlemesine odaklanır |
| Mimari plan | Büyük alanlar için modül içi `docs/` planı; kodla farkları periyodik güncellenir |
| **ADR** (Architecture Decision Record) | Mimari şekil, veri ayrımı, yeni altyapı bileşeni, versiyonlama, güvenlik modeli gibi geri alması pahalı her karar `docs/adr/NNNN-<baslik>.md` olarak yazılır (şablon: `blueprint/docs/adr/0000-template.md`): bağlam, seçenekler, karar, sonuçlar, **yeniden değerlendirme eşiği** (Bölüm 24). ADR'siz mimari değişiklik PR'ı `REQUEST CHANGES`. |
| Local geliştirme | `docker compose -f deploy/docker-compose.local.yml up -d` altyapıyı (Postgres, Valkey ×2, RabbitMQ, Alloy/Grafana) kaldırır; servisler IDE'den `local` profiliyle; Testcontainers dev-time desteği (`SpringApplication.from(App::main).with(LocalContainers.class)`) alternatif. Seed verisi `db/seed-local`. `make up / test / lint / check` hedefleri README'de. İlk kurulum 30 dakikayı geçmemeli; geçiyorsa `docs/onboarding.md` güncellenir. |
| Deploy sırası | Contract, enum veya event tipi ekleyen taraf tüketiciden **önce** deploy edilir |
| Bağımlılık hijyeni | Renovate/Dependabot haftalık; çeyrekte bir Bölüm 25 EOL kontrolü; destek dışı sürüm PR gate'te uyarı |
| Dokümantasyon hijyeni | README kimlik/sıcak yol/fail politikası/kapasite tabloları, `docs/ai/repo-context.md`, `docs/versions.md` her release'te; runbook'lar her yeni alarmda; bu referans dokümanı çeyreklik gözden geçirme |

**İstemci entegrasyon dokümanı şablonu:**

```
# <Client> <özellik> entegrasyonu [vN]
  (giriş + "<tarih> tarihli <branch> <sha> koduna dayanır")
## Değişikliklerin özeti
## Kurallar                       (**Kritik:** yalnız veri kaybı/güvenlik/ücret/bozuk akış için)
## Değişiklikler ve <client>'a etkisi
## Kullanılan endpoint'ler
## API sözleşmesi
### <işlem>
#### Senaryo: <başarı> / <hata>  (HTTP status + body; zarflı mı ham mı)
#### Alan eşleme
#### <Dil> örneği
## Hata kodları ve ekran davranışı
## Güvenlik ve log kuralları
## Önceki sürüme göre farklar
## Manuel test akışı (uygulama + API koleksiyonu)
## Açık sorular
```

---

## 21. Adım Adım Checklist'ler

### 21.0 Yeni Proje Başlangıcı (ilk iki sprint; "sonra" yok)

**Karar ve yapı**
- [ ] Mimari şekil kararı (Bölüm 1.1) yazılı: modüler monolit / hibrit / mikroservis; gerekçe ve yeniden değerlendirme eşiği (Bölüm 24).
- [ ] Sıcak yol listesi ve her biri için "kaç uzak çağrı" hedefi (≤1).
- [ ] Domain sınırları ve olay listesi (Bölüm 12.1) — hangi servis hangi olayı yayınlar, kim tüketir.
- [ ] Teknoloji yığını yalnız OSS-destekli sürümlerle (Bölüm 25 kontrolü tarihli olarak README'de).
- [ ] API versiyonlama kararı (`/v1`); OpenAPI üretimi ve istemci client generation CI'da.
- [ ] Monorepo + affected-module CI + `platform/*` starter'ları.

**Veri ve operasyon (yedek olmayan sistem production değildir)**
- [ ] PostgreSQL: servis başına rol + şema, PgBouncer, `pg_stat_statements`, **WAL arşivi + base backup + aylık restore provası**, RPO/RTO yazılı. Managed PostgreSQL değerlendirildi.
- [ ] Valkey ×2 (security `noeviction`+AOF / cache), Sentinel veya managed.
- [ ] RabbitMQ 4.x quorum queue'lar, `defaultRequeueRejected=false`, prefetch, DLQ alarmı.
- [ ] Object storage: private bucket'lar, versioning, signed URL, görsel pipeline (Bölüm 9.9).
- [ ] Alertmanager + bildirim kanalı; asgari alarm seti (Bölüm 8.7) ilk deploy'dan önce.
- [ ] Alloy → Loki/Tempo, structured JSON log, tail sampling; postgres/redis/rabbit exporter'ları.
- [ ] 3–5 SLO ve burn-rate alarmı.
- [ ] Kapasite planı: host RAM hesabı, ikinci instance/host senaryosu.

**Güvenlik**
- [ ] Asimetrik service JWT (servis başına anahtar, JWKS, `kid`); simetrik secret yok.
- [ ] Secret'lar compose `secrets:` + config tree; SOPS; gitleaks pre-commit + CI; literal fallback yok (fail-fast).
- [ ] Gateway: rate limit, timeout bütçesi, header temizliği, CORS listesi, `gateway` actuator kapalı.
- [ ] Admin 2FA passkey/TOTP; SMS OTP için pumping savunması; cihaz attestation planı.
- [ ] Mock entegrasyonlar (`@Profile("local|test")`) + prod açılış kontrolü; seed verisi ayrı location.
- [ ] DPIA; kişisel veri envanteri; silme saga'sı tasarımı (Bölüm 9.9); export ucu.
- [ ] Ban/rol değişimi → `sv` artışı.

**Kalite ve yönetişim**
- [ ] Testcontainers ile gerçek DB testleri CI'da; ArchUnit; enforcer `bannedDependencies`; hata kodu çakışma testi; config drift testi.
- [ ] `AGENTS.md`, `docs/ai/*`, skill'ler, migration immutability hook + CI; hook'lar test edilmiş.
- [ ] Image CI'da build → registry → digest + cosign; `docker-rollout`; staging → prod onay kapısı; Renovate.

### 21.1 Yeni Servis (veya Modül)

1. `<domain>-api` ve `<domain>-core` modüllerini parent pom'a ekle (`services/<domain>/`).
2. Kök paketi `com.<org>.<domain>` yap. Main sınıf Bölüm 4.5'teki gibi; `platform-*` starter bağımlılıkları.
3. Kimlik tablosunu doldur: port, `application.name`, actor/iss, audience, schema, DB rolü, hata kodu bloğu, **sıcak yol uzak çağrı sayısı**, yayınladığı/tükettiği olaylar (Bölüm 3.4).
4. Config dosyalarını oluştur: `application.yml` (import satırları), `config/<svc>.yml`, `application-local.yml`; env değişkenleri deploy `env_file`'ına; secret'lar `secrets/<env>.enc.yaml`'a.
5. DB: `CREATE ROLE svc_<schema>` + `CREATE SCHEMA … AUTHORIZATION` (altyapı migration'ı), `V1__init_schema.sql`, Flyway `schemas`, `currentSchema`, `ddl-auto: validate`. PgBouncer pool'u.
6. Signing key çifti üret; JWKS'e public key + `kid` ekle; doğrulayan servislerin `iss` listesine ekle.
7. Security config: stateless, CSRF kapalı, service JWT filtresi. Filtre servlet'e ayrıca kaydolmasın (`FilterRegistrationBean.setEnabled(false)`).
8. `ErrorCode` enum'u (yeni blok) ve `<Servis>ServiceException` sınıfını yaz; çakışma testi geçsin.
9. `outbox_event` tablosu (Bölüm 11.2) + gerekiyorsa `inbox` ve read-model tabloları.
10. Gateway route'u (`metadata.audience`) + timeout + rate limit scope'ları.
11. Altyapı kayıtları: Jib config, compose servisi (anchor'lar, healthcheck, secrets), deploy servis listesi, Prometheus hedefi, Alertmanager route'u, SLO panosu.
12. ArchUnit test sınıfı (platform'dan kopya), Testcontainers temel testi.
13. `docs/ai/repo-context.md` ve README'deki kimlik tablosunu güncelle.

### 21.2 Yeni Public Endpoint

- [ ] DTO'lar api modülünde, validation anotasyonlarıyla. Hassas alanlarda `@ToString.Exclude`.
- [ ] Controller: `@CurrentAccount`, `@Valid`, açık binding adları.
- [ ] Ownership kontrolü serviste. Sayfalama sınırı ve sıralama allowlist'i.
- [ ] Rate limit scope'u tanımlı (local ve config repo).
- [ ] Reddetme: `log.warn("<Olay> rejected: code=X reason=Y")` + throw. Başarı: INFO, `operation=` / `outcome=`.
- [ ] Kişisel veri dönüyorsa `no-store`.
- [ ] Gateway'de gereksiz `permitAll` yok.
- [ ] Testler: MockMvc binding (geçerli / geçersiz / kimlik eksik), servis, log privacy.
- [ ] API koleksiyonu ve istemci entegrasyon dokümanı güncel.

### 21.3 Yeni Internal Endpoint

- [ ] Path `/internal/...`.
- [ ] Dar bir allowlist kuralı, catch-all'dan önce, **local ve config repo'da birlikte**.
- [ ] Kullanıcı adına çalışıyorsa JWT `sub` doğrulaması.
- [ ] DTO'lar hedefin api modülünde.
- [ ] Çağıranda hedef başına tek HTTP client (`@HttpExchange` grubu veya Feign; `aud` = hedef). `base-url`, timeout, circuit breaker ve bulkhead her ortamda tanımlı.
- [ ] Bu uç sıcak yolda **okuma** için mi çağrılıyor? Öyleyse durdur: read-model veya JWT claim (Bölüm 4.6).
- [ ] Uç idempotent.
- [ ] Testler: gerçek filtreyle actor erişim testi + client contract testi + "hedef yanıt vermiyor" dayanıklılık testi.

### 21.4 Yeni Asenkron Yan Etki (Komut veya HTTP)

- [ ] Gerçekten komut mu? Başka servisin verisini değiştiriyorsa **event** olmalı (Bölüm 21.8).
- [ ] `outbox_event` satırı (`kind=COMMAND|HTTP`), yazıcı `MANDATORY`, deterministik `id` gerekiyorsa sağlandı, payload'da PII yok, iletim sonrası hassas alan NULL.
- [ ] Handler: uzak iş TX dışında; DEAD politikası ve `expires_at` iş türüne göre tanımlı.
- [ ] Tüketici/hedef idempotent. Queue ise Bölüm 12.3 ayarları ve önce consumer deploy.
- [ ] Testler: outbox satırı domain TX ile birlikte rollback oluyor; gerçek DB ile paralel claim ayrıklığı, retry, lease kaybı; log privacy.

### 21.5 Yeni Migration

- [ ] `V<sonraki>__<snake>.sql`. Base'teki dosyalara dokunulmadı; immutability kontrolü çalıştırıldı.
- [ ] Yalnız kendi schema'sı, tam nitelikli adlar, cross-schema FK yok.
- [ ] Neden-odaklı yorumlar. `TIMESTAMPTZ`, `TEXT + CHECK`, isimli constraint/index.
- [ ] Mevcut veri, lock ve rewrite riski değerlendirildi. Seed verisi prod location'ında değil.
- [ ] `ddl-auto: validate` ile uyumlu. Migration review skill'i çalıştırıldı.

### 21.6 Uzak Kaynak Tüketen İşlem (Saga)

- [ ] Assessment: saga gerçekten gerekli mi?
- [ ] `X-Idempotency-Key` UUID. `begin()` ayrı TX'te. Participant çağrısı TX dışında. Domain + `success()` aynı TX'te, ayrı bean'de.
- [ ] Katılımcı: idempotent operation tablosu, tombstone, advisory lock.
- [ ] Recovery, monitor ve cleanup tanımlı. `MANUAL_REVIEW` kayıtları korunuyor.
- [ ] Bölüm 11.5 test matrisi.

### 21.7 Yeni İş Kuralı Değeri

- Admin değiştirebilecekse → Bölüm 14 (parametre kataloğu; `criticality` ve bounded-staleness T değeri tanımlı).
- Özellik açma/kapama veya deney → Bölüm 14.4 (flag; geçici, silinme tarihi var).
- Değiştiremeyecekse → `@ConfigurationProperties` + local ve deploy config (Bölüm 15.2).

### 21.8 Yeni Domain Event ve Read-Model

- [ ] Olay adı `<servis>.<aggregate>.<olay>`; payload sınıfı `<domain>-api/event`; CloudEvents attribute'ları platform'dan.
- [ ] Üretici: domain TX'inde `outbox_event` (`kind=EVENT`, `aggregate_id` routing key).
- [ ] Tüketici: kendi queue'su + DLQ, inbox dedup, bilinmeyen tip yok sayılır.
- [ ] Read-model tablosu kendi şemasında; `revision` ile UPSERT; eskime eşiği ve fail-closed davranışı yazılı; rebuild yolu (stream replay veya sahibin export ucu) belgelendi.
- [ ] `readmodel_lag_seconds` metriği + alarm.
- [ ] Şema evrimi notu (`type` versiyonu); önce consumer deploy.
- [ ] Analytics sink bu olayı alıyor mu? (Bölüm 14.4)
- [ ] Testler: outbox rollback, tüketici idempotent, sıra bozuk olay yok sayılıyor, read-model'den karar veren akışın "satır yok" davranışı.

### 21.9 Hesap Silme (Silme Saga'sı)

- [ ] Uygulama içinde "hesabı sil" akışı var (mağaza gereksinimi); onay + soğuma süresi (örn. 7 gün, geri alınabilir).
- [ ] `account.deletion_requested` event'i; her servis idempotent `DELETE /internal/<kaynak>/accounts/{id}` uygular ve `account.deletion_completed.<servis>` yayınlar; orkestratör tamamlanmayı izler, süre aşımında alarm.
- [ ] Kapsam listesi servis başına yazılı: tablolar (sil/anonimleştir), arama index'i, cache key'leri, object storage (tüm versiyonlar + CDN purge), cihaz token'ları, üçüncü taraf işleyiciler.
- [ ] Yasal saklama gereken kayıtlar anonimleştirilir; dayanak yazılı. Ban/fraud için hash'li asgari kayıt.
- [ ] Yedekler: crypto-shredding veya "beyond use" + restore sonrası yeniden silme prosedürü.
- [ ] Tamamlanma kaydı (audit) ve kullanıcıya bildirim; KVKK 30 gün / GDPR 1 ay süresi ölçülüyor.

---

## 22. Kaçınılacak Anti-Pattern'ler

**Mimari şekil ve ölçek**
- "Dağıtık monolit": N servis, tek DB, tek host, tek secret, birlikte deploy, sıcak yolda 3–4 senkron çağrı. Mikroservis maliyeti ödenir, faydası alınmaz (Bölüm 1.1).
- Sıcak yolda başka servise **okuma** amaçlı senkron çağrı; availability çarpımı ve p99 toplamı.
- Outbox üzerinden RPC: üreticinin tüketicinin adresini ve ucunu bilmesi; domain event yerine noktadan noktaya yan etki.
- Domain event akışı olmadan ürün geliştirmek: analytics, deney, öneri, fraud ve read-model için sinyal yok.
- Konu başına ayrı outbox tablosu/poller yazmak (aynı problem N kez, N kalitede).
- Erken Kafka: tüketicisi ve replay ihtiyacı olmayan event log'u için tek host'ta broker işletmek. Erken NoSQL: partition'lı Postgres yetiyorken mesaj tablosunu Cassandra/Mongo'ya taşımak. Erken Kubernetes: tek host, tek ekip.
- Geç ayrım: en sıcak domain'in disk IO'sunu diğer 8 şemayla aynı instance'ta tutmaya, TB'lara kadar devam etmek (Bölüm 24 eşikleri).
- Polyrepo'ya "mikroservis böyle olur" diye geçmek; sorun pipeline'dayken repo bölmek.
- Control-plane bağımlılığı: parametre/config/flag servisi düşünce tüm ürünün 503 dönmesi.
- Yedeksiz production. Restore provası yapılmamış yedek. Tek host'ta "HA var" sanmak.
- Prod sunucusunda image build etmek; registry'siz deploy; tag ile (digest yerine) deploy.
- Tek "common" kütüphanesi: her değişiklikte tüm servisler rebuild + deploy.

**Güvenlik**
- Profil koruması olmayan mock/sahte entegrasyon bean'leri (ödeme doğrulama, SMS, mağaza). Mock'lar `@Profile("local|test")` altında olmalı ve prod'da açılış kontrolüyle engellenmeli.
- Her ortamda çalışan seed migration'ları, özellikle bilinen parolalı admin veya test kullanıcıları.
- Tüm servislerde tek simetrik service-JWT secret'ı ve sabit `iss`.
- Local yml ile deploy config'i arasında allowlist drift'i.
- OTP, token veya anahtarın outbox/log payload'ında düz metin kalması.
- İkinci bir kimlik doğrulama yolunun (admin OTP gibi) daha zayıf hash ve karşılaştırma kullanması.
- Hesap kapatma olaylarının oturum sürümünü artırmaması.
- Gateway'in istemciden gelen iç header'ları temizlememesi. `StripPrefix` ile `/internal` kontrolünün atlanması.
- CORS `*`. Token'ın query parametresinde taşınması.
- İmzasız token parse eden ölü kod.
- Secret için literal fallback değerleri. Secret'ları Config Server'dan dağıtmak.
- Root container. `.env`'in build context'ine girmesi.
- Güvenlik state'ini (session, replay, rate limit) `allkeys-*` eviction'lı cache Redis'inde tutmak.
- Her istekte Redis'e giden replay guard'ı, mTLS/asimetrik imza yerine güvenlik sınırı sanmak.
- Kullanıcı fotoğrafını public bucket'a ham (EXIF/GPS'li), moderasyonsuz yüklemek.
- Rastgele konum fuzzing'ini yeterli saymak (trilateration ile geri alınır).
- "Uçtan uca şifreli" iddiasıyla forward-secrecy'siz, sunucu-dağıtımlı statik anahtar şeması sunmak.
- Admin 2FA'yı SMS'e dayamak; kullanıcı OTP ucunu SMS-pumping savunmasız bırakmak.
- Mağaza makbuz doğrulamasını mock bırakmak veya `originalTransactionId`/`purchaseToken` tekilliği ve sunucu bildirimleri olmadan hak vermek.
- Hesap silme akışı olmadan yayına çıkmak (mağaza reddi + KVKK/GDPR süresi).
- Destek dışı framework/broker/DB sürümüyle üretimde kalmak; yaması ticari sürümde olan CVE'lerle yaşamak.

**Doğruluk ve tutarlılık**
- Kilitsiz outbox poller'ı veya TX içinde uzak çağrı.
- Lease süresini aşan batch'ler; sonuç yazılırken sahiplik kontrolü yapılmaması.
- Gecikmesiz requeue. DLQ'nun izlenmemesi.
- Karşı tarafın interceptor'ın sildiği header'ı beklemesi.
- Dockerfile `-pl` listesinde eksik modül (paylaşılan cache'e güvenmek).
- Dağıtık kilitsiz sabit cron.
- Paylaşılan Redis key formatlarının servislerde kopyalanması.
- Spring AMQP varsayılan `defaultRequeueRejected=true` ve prefetch 250 ile consumer yazmak; RabbitMQ delayed-exchange plugin'i (arşivlendi).
- Read-model'i kaynak gibi dışa açmak; sıra numarasız event tüketip eski olayla yeni satırı ezmek.
- Timeout'suz, circuit breaker'sız HTTP client; Resilience4j'nin varsayılan 1 sn TimeLimiter'ını fark etmemek.
- UUIDv4 PK ile yüksek insert'li tablolar; base64 TEXT ile blob; partition'sız append-only tablo ve `DELETE` ile retention.
- `optional:configserver:` ile sessiz kısmi config.

**Hata ve log**
- Servisler arası çakışan hata kodları.
- Standart MVC hatalarının 500'e düşmesi.
- Filtre ve controller'da iki farklı hata formatı. 200 status ile `success=false` dönmek.
- Kullanıcı kaynaklı 4xx'lerin ERROR seviyesinde loglanması (alarm gürültüsü). Stack trace'in hiç basılmaması.
- Ham `e.getMessage()`, PII veya her istekte kullanıcı kimliği loglamak.
- Birden çok log stilinin karışması.
- Alertmanager'sız alarm kuralı, eşleşmeyen label regex'i, sabitlenmemiş datasource `uid`'si.

**Yapı ve hijyen**
- `Dto`/`DTO` karışıklığı. Birden çok mapping metot adı, Feign URL key biçimi ve client adı biçimi.
- Birden çok entity ID üretim ve auditing yöntemi. Tablo adında tekil/çoğul karışıklığı. İki index öneki.
- api ve core'un aynı paketi paylaşması (split package).
- Controller'dan repository'ye erişim. `@Valid`'siz `@RequestBody`.
- `service.impl` altında Impl olmayan sınıf. `config/` dışında `@Configuration`.
- "AutoConfig" adlı ama auto-config olmayan sınıflar. Yalnız `pluginManagement`'ta kalıp hiç çalışmayan enforcer.
- Kullanılmayan bağımlılık, DTO, client, property. Boş AI talimat dosyaları. Kırık yardımcı script'ler.
- Build'e dahil olup deploy edilmeyen iskelet modüller.
- `show-sql` prod'da açık. `out-of-order` migration, "temp" adlı migration'lar.
- Frontend: lint/format yok, etkisiz `eslint-disable` yorumları, bildirilmemiş bağımlılık, Rules of Hooks ihlali, `dist/` git'te, rol matrisi birden çok yerde.

---

## 23. Kod Şablonları

### 23.1 ErrorCode ve Servis Exception'ı

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode implements com.<org>.security.exception.ErrorCode {
    // Order (11000-11099)
    ORDER_NOT_FOUND(11001, "Order not found.", HttpStatus.NOT_FOUND),
    ORDER_NOT_CANCELLABLE(11002, "Order cannot be cancelled.", HttpStatus.CONFLICT),
    // Parametre (11500-11599)
    PARAMETER_NOT_DEFINED(11500, "System parameter is not defined.", HttpStatus.INTERNAL_SERVER_ERROR),
    PARAMETER_VALUE_INVALID(11501, "System parameter value is invalid.", HttpStatus.INTERNAL_SERVER_ERROR),
    PARAMETER_UNAVAILABLE(11502, "System parameter service is temporarily unavailable.", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    @Override public String getService() { return "order"; }
}

public class OrderServiceException extends ServiceException {
    public OrderServiceException(ErrorCode c) { super(c); }
    public OrderServiceException(ErrorCode c, Object... args) { super(c, args); }
    public OrderServiceException(ErrorCode c, Throwable cause) { super(c, cause); }
    protected OrderServiceException(ErrorCode c, String safeLogReason, String safeLogCategory) {
        super(c, safeLogReason, safeLogCategory);
    }
}
```

### 23.2 Controller

```java
/** Siparis islemleri. Kimlik her zaman dogrulanmis baglamdan gelir; path'teki id yalniz hedef kaynaktir. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @CurrentAccount UUID accountId,
            @RequestHeader("X-Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(accountId, idempotencyKey, request));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancel(@CurrentAccount UUID accountId,
                                       @PathVariable("orderId") UUID orderId) {
        orderService.cancel(accountId, orderId);
        return ResponseEntity.noContent().build();
    }

    // Sayfa boyutu serviste 1..50 araligina cekilir.
    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> list(
            @CurrentAccount UUID accountId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.list(accountId, page, size));
    }
}
```

### 23.3 Outbox Claim Sorgusu

```java
public interface OrderEventOutboxRepository extends JpaRepository<OrderEventOutbox, UUID> {

    // Birden fazla instance ayni satiri alamaz: SKIP LOCKED kilitli satirlari atlar, locked_until kira suresidir.
    // Kirasi dolan PUBLISHING satiri (instance cokmesi) yeniden claim edilir.
    @Transactional
    @Query(value = """
            WITH candidates AS (
                SELECT id FROM <schema>.order_event_outbox
                WHERE (status = 'PENDING' OR (status = 'PUBLISHING' AND locked_until <= :now))
                  AND next_retry_at <= :now
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT :limit)
            UPDATE <schema>.order_event_outbox o
            SET status = 'PUBLISHING', locked_until = :lockedUntil, claim_token = :claimToken
            FROM candidates WHERE o.id = candidates.id
            RETURNING o.*
            """, nativeQuery = true)
    List<OrderEventOutbox> claimPending(@Param("now") Instant now, @Param("lockedUntil") Instant lockedUntil,
                                        @Param("claimToken") UUID claimToken, @Param("limit") int limit);

    // Sonucu yalniz claim sahibi yazar; kirasi elinden alinmis eski worker satiri ezemez.
    @Transactional @Modifying
    @Query("DELETE FROM OrderEventOutbox o WHERE o.id = :id AND o.claimToken = :claimToken")
    int deleteProcessed(@Param("id") UUID id, @Param("claimToken") UUID claimToken);

    @Transactional @Modifying
    @Query("""
            UPDATE OrderEventOutbox o SET o.status = 'PENDING', o.retryCount = :retry, o.nextRetryAt = :next,
                   o.lockedUntil = null, o.claimToken = null, o.lastErrorCode = :err
            WHERE o.id = :id AND o.claimToken = :claimToken
            """)
    int markFailed(@Param("id") UUID id, @Param("claimToken") UUID claimToken, @Param("retry") int retry,
                   @Param("next") Instant next, @Param("err") String err);
}
```

### 23.4 Outbox Poller

```java
/**
 * Siparis olaylarinin yan etkilerini uygular. Birden fazla instance'ta calisir; satirlar SKIP LOCKED + kira ile
 * claim edilir, uzak cagri transaction disindadir. Hedef uc idempotent oldugu icin tekrar islenme zararsizdir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventOutboxPoller {

    static final int BATCH_SIZE = 50;
    static final long LEASE_SECONDS = 120;
    static final long LEASE_SAFETY_SECONDS = 30;   // kiranin son 30 sn'sinde yeni satira baslanmaz
    static final long MAX_BACKOFF_SECONDS = 600;
    static final int STUCK_ALERT_EVERY = 10;       // kalici hata alarmi her denemede degil, aralikla

    private final OrderEventOutboxRepository repository;
    private final InventoryClient inventoryClient;
    private final TraceContextCarrier traceContextCarrier;

    @Scheduled(fixedDelayString = "${order.event-outbox.poll-interval-ms:5000}")
    public void poll() {
        Instant claimedAt = now();
        Instant leaseEnd = claimedAt.plusSeconds(LEASE_SECONDS);
        UUID claimToken = UUID.randomUUID();
        List<OrderEventOutbox> entries = repository.claimPending(claimedAt, leaseEnd, claimToken, BATCH_SIZE);
        if (entries.isEmpty()) return;                                  // bos turlar loglanmaz

        Instant workDeadline = leaseEnd.minusSeconds(LEASE_SAFETY_SECONDS);
        int applied = 0, failed = 0, deferred = 0;
        for (OrderEventOutbox entry : entries) {
            if (now().isAfter(workDeadline)) { deferred++; continue; }   // kira dolunca baska instance alir
            Span span = traceContextCarrier.startSpan(
                    traceContextCarrier.from(entry.getTraceparent(), entry.getTracestate()), "outbox.order-event.apply");
            try (var ignored = traceContextCarrier.withSpan(span)) {
                inventoryClient.apply(entry.toRequest());
                repository.deleteProcessed(entry.getId(), claimToken);
                applied++;
            } catch (Exception e) {
                span.error(e);
                markFailure(entry, claimToken, e);
                failed++;
            } finally {
                span.end();
            }
        }
        log.info("Order event outbox batch finished: claimed={} applied={} failed={} deferred={}",
                entries.size(), applied, failed, deferred);
    }

    Instant now() { return Instant.now(); }                             // test edilebilir zaman kaynagi

    private void markFailure(OrderEventOutbox entry, UUID claimToken, Exception e) {
        int retries = entry.getRetryCount() + 1;
        String errorType = e.getClass().getSimpleName();                // exception mesaji degil
        repository.markFailed(entry.getId(), claimToken, retries, now().plusSeconds(backoffSeconds(retries)), errorType);
        if (retries % STUCK_ALERT_EVERY == 0) {
            log.error("Order side effect still failing: code=ORDER_OUTBOX_STUCK eventId={} retry={} exceptionType={}",
                    entry.getEventId(), retries, errorType);
        } else {
            log.warn("Order side effect failed; retry scheduled: eventId={} retry={} exceptionType={}",
                    entry.getEventId(), retries, errorType);
        }
    }

    // min(600, 30 * 2^n) sn
    static long backoffSeconds(int retries) {
        return Math.min(MAX_BACKOFF_SECONDS, 30L * (1L << Math.min(Math.max(retries, 0), 20)));
    }
}
```

### 23.5 Controller Binding Testi

```java
/** Binding HTTP katmani uzerinden dogrulanir; kimlik yalniz dogrulanmis baglamdan gelir. */
class OrderControllerTest {

    private final OrderService orderService = mock(OrderService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new OrderController(orderService))
            .setCustomArgumentResolvers(new CurrentAccountArgumentResolver())
            .setControllerAdvice(new GlobalServiceExceptionHandler())
            .build();

    private final UUID accountId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @Test
    void cancel_bindsPathTargetAndAuthenticatedAccount() throws Exception {
        mockMvc.perform(post("/orders/{orderId}/cancel", orderId).requestAttr("x.accountId", accountId.toString()))
                .andExpect(status().isNoContent());
        verify(orderService).cancel(accountId, orderId);
    }

    @Test
    void cancel_whenPathIsNotUuid_rejectsBeforeReachingService() throws Exception {
        mockMvc.perform(post("/orders/{orderId}/cancel", "not-a-uuid").requestAttr("x.accountId", accountId.toString()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    @Test
    void cancel_whenAuthenticatedAccountIsMissing_doesNotReachService() throws Exception {
        mockMvc.perform(post("/orders/{orderId}/cancel", orderId))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    @Test
    void cancel_whenNotCancellable_returnsErrorEnvelope() throws Exception {
        doThrow(new OrderServiceException(ErrorCode.ORDER_NOT_CANCELLABLE)).when(orderService).cancel(accountId, orderId);
        mockMvc.perform(post("/orders/{orderId}/cancel", orderId).requestAttr("x.accountId", accountId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value(11002))
                .andExpect(jsonPath("$.error.service").value("order"));
    }
}
```

### 23.6 Log Privacy Testi

```java
private Logger logger;
private ListAppender<ILoggingEvent> appender;

@BeforeEach
void attachAppender() {
    logger = (Logger) LoggerFactory.getLogger(OrderServiceImpl.class);
    logger.setLevel(Level.INFO);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
}

@AfterEach
void detachAppender() {
    logger.detachAppender(appender);
    logger.setLevel(null);
}

@Test
void create_logsOutcomeWithoutAccountIdOrSensitiveInput() {
    String marker = "SENSITIVE-MARKER-123";
    service.create(accountId, UUID.randomUUID(), requestWithNote(marker));

    assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
            .anyMatch(m -> m.contains("outcome=SUCCESS"))                                // güvenli alan VAR
            .noneMatch(m -> m.contains(accountId.toString()) || m.contains(marker));     // kimlik/hassas veri YOK
}
```

### 23.7 Migration

Bölüm 10.2'deki DDL şablonu, Bölüm 10.6'daki append-only tablo kuralları ve Bölüm 11.2'deki `outbox_event` tablosu kullanılır.

### 23.8 HTTP Client ve Gateway Route

Bölüm 6.8 ve Bölüm 9.3'e bakın.

> **Not — 23.3 ve 23.4 hakkında:** Bu iki şablon claim/lease/backoff mekaniğini gösterir. Yeni projede bu kod **`platform-messaging` starter'ında bir kez** yaşar (`OutboxPoller` + `OutboxHandler` arayüzü); servis yalnız handler yazar (`kind`/`event_type` → ne yapılacağı). Konu başına poller kopyalanmaz (Bölüm 11.2).

---

## 24. Ölçek Eşikleri ve Evrim Yolu

Her karar "bugün için doğru" ve "şu eşikte değişir" çiftiyle verilir. Eşikler README'de izlenen metriklere bağlanır; tutunca mimari inceleme açılır. Sayılar sektör pratiğidir, kesin sınır değildir.

| Alan | Bugün (başlangıç) | Değişim eşiği | Sonraki adım |
|---|---|---|---|
| Deploy birimi | Modüler monolit / hibrit (Bölüm 1.1) | ≥2 ekip, ayrı release kadansı, bir modülün farklı ölçek profili (WebSocket, CPU-yoğun) | Modülü ayrı servise çıkar; sınırlar zaten hazır |
| Repo | Monorepo | ≥2 ekip + ayrı on-call + ayrı release | `*-api` modüllerini Maven repo'ya semver ile yayınla, sonra core'ları ayır |
| Host | Tek host compose + `docker-rollout` | RAM/CPU %70 sürekli; ikinci instance ihtiyacı; tek host SPOF kabul edilemez | İkinci host + proxy upstream; ≥3 host ve/veya çok ekip → k3s/Swarm/managed k8s |
| PostgreSQL | Tek instance, rol+şema/servis, PgBouncer | Bağlantı ≥ `max_connections`×0,7; bir şemanın IO'su diğerlerini etkiliyor; yedek/restore penceresi SLO'yu aşıyor | Ayrı logical DB → en sıcak domain için ayrı instance → managed + standby |
| Append-only büyük tablo | Partition'lı Postgres (Bölüm 10.6) | > 1–2 TB veya > 2–3 milyar satır; sürekli > 10k insert/sn; çok bölge aktif-aktif; ayrı ekip/on-call | Wide-column (ScyllaDB/Cassandra) — yalnız o tablo; transactional kısım Postgres'te kalır |
| Geo/arama | PostGIS | Metin/çok kriterli/vektör arama; ranking pipeline; on milyonlarca kayıtta geo-shard ihtiyacı | OpenSearch (Apache-2.0) veya ES 9 (lisans kararı); ES yalnız arama için, kaynak DB değil |
| Redis/Valkey | 2 instance (security/cache), Sentinel | Tek instance belleği > 25 GB veya throughput sınırı | Cluster (≥3 master, 6 node); multi-key işlemler hash slot'a göre |
| Mesajlaşma | RabbitMQ 4.x QQ + streams | ≥3 bağımsız tüketici + replay + stream processing; günlük olay > on milyon; CDC → warehouse; çok host | Kafka/Redpanda; envelope aynı, transport değişir (Bölüm 12.5) |
| Outbox | Polling publisher (1 sn) | Gecikme SLO'yu aşıyor (< 200 ms gerekiyor); poller yükü DB'de görünür; Kafka zaten var | Debezium Server (CDC) + WAL slot alarmları |
| Realtime | Redis pub/sub + seq + history pull | Kayıp mesaj ürün sorunu; bağlı istemciye at-least-once şart; WebSocket bağlantısı > ~50k/pod | Redis Streams veya Centrifugo; chat servisi ayrı ölçeklenir |
| Servis kimliği | Asimetrik JWT + JWKS | Düzenleyici/uyum gereği transport kimliği; çok host | mTLS (iç CA) veya SPIFFE/SPIRE |
| Secret | Compose secrets + SOPS | Dinamik DB kimliği, audit zorunluluğu, onlarca servis | OpenBao/Vault |
| Config | Env + yml + config tree | Merkezi refresh ihtiyacı, onlarca servis × ortam | Config Server (git backend, fail-fast, secret'sız) |
| Trace sampling | Tail sampling (%100 hata + %10) | Tempo depolama/ingester belleği | Oranı düşür, hata/yavaş izleri koru |
| Contract testi | Derleme zamanı tipleme + OpenAPI diff | Polyrepo | Pact / Spring Cloud Contract |
| Feature flag | Katalog `BOOLEAN/PERCENTAGE` veya OpenFeature+Unleash | Deney metrikleri warehouse'da | GrowthBook/PostHog |
| Ekip | 1–5 kişi | 6+ kişi | Yukarıdaki "≥2 ekip" satırları tetiklenir |

**Kural:** Eşik tutmadan sonraki adıma geçilmez; tuttuğunda da bekletilmez. Her çeyrekte bu tablo metriklerle karşılaştırılır.

---

## 25. Sürüm ve Destek Takibi

**Kural:** Üretimde yalnız OSS güvenlik yaması alan sürümler çalışır. Bir bileşenin OSS desteği bitmeden **en az 3 ay önce** upgrade planlanır. Renovate/Dependabot minor/patch'i otomatik açar; major upgrade'ler çeyreklik planda.

**Kontrol edilecek kaynaklar (çeyrekte bir, tarihli olarak README'ye işlenir):**

| Bileşen | Kaynak | Not (2026-09) |
|---|---|---|
| Spring Boot / Framework / Security / Cloud | `spring.io/support-policy`, proje sayfalarındaki destek tabloları, `endoflife.date/spring-boot` | Boot 3.x OSS desteği bitti (3.5: 2026-06); 4.0.x 2026-12'ye, 4.1.x sonrasına kadar. Spring Cloud OSS: 2025.1.x (Boot 4.0), 2026.0.x (Boot 4.1). Her minor ≥13 ay; major'ın son minor'u ticari desteğe geçer. |
| Java | `endoflife.date/oracle-jdk`, Adoptium/Temurin | 25 LTS (2025-09); 21 LTS hâlâ destekli; LTS dışı sürümler üretimde kullanılmaz |
| PostgreSQL | `postgresql.org/support/versioning` | 5 yıl; 15 → 2027-11, 14 → 2026-11; 18 güncel |
| Redis / Valkey | GitHub releases; lisans: `LICENSE.txt` | Redis 7.x bakımsız; 8.x AGPL/RSALv2/SSPL; Valkey 9.x BSD |
| RabbitMQ | `rabbitmq.com/release-information`, `endoflife.date/rabbitmq` | Community-destekli tek seri en yeni minor; eski minor'lar aylar içinde düşer |
| Elasticsearch / OpenSearch | `elastic.co/support/eol`, OpenSearch releases | ES 8.x bakım sonu 2027-01; lisans dalına göre farklı |
| Grafana yığını | GitHub releases; Promtail EOL (2026-03-02) | Alloy, Loki, Tempo, Prometheus minor'ları birlikte güncellenir |
| Testcontainers, ArchUnit, Resilience4j, Debezium | GitHub releases | Testcontainers 2.x artefakt adları değişti |
| Node / React / Vite / MUI | `endoflife.date/nodejs`, proje sayfaları | Node LTS dışı sürüm CI/Dockerfile'da kullanılmaz |
| CVE'ler | `spring.io/security`, GitHub Advisory DB (Dependabot alerts), `osv.dev` | Yaması yalnız ticari sürümde olan CVE = upgrade tetikleyicisi |

**Upgrade disiplini:**
- Major Spring Boot upgrade'i (örn. 3.x → 4.x: Framework 7, Jakarta EE 11, Jackson 3, Hibernate 7, modüler starter'lar, `@MockitoBean`, Testcontainers 2) ayrı bir PR dizisi; `spring-boot-properties-migrator` ve OpenRewrite reçeteleri; önce `platform-*` starter'ları, sonra servisler.
- Broker/DB major upgrade'i staging'de prod yedeğinden restore edilmiş veriyle prova edilir; RabbitMQ için blue/green veya belgelenmiş minor→minor yolu (örn. 3.13 → 4.2 → 4.3).
- Upgrade sonrası kontrol: Testcontainers testleri, ArchUnit, yük testi, SLO panosu 24 sa.
- Lisans değişiklikleri (Redis, Elastic, Redpanda BSL, GPL istemci kütüphaneleri) hukuki inceleme ister; `LICENSE` dosyası bağımlılık güncellemesinde okunur.

---

## 26. Mevcut Bir Projeye Uygulama Protokolü

> Bu bölüm bir AI ajanına (veya yeni gelen bir mühendise) "**bu dokümanı referans alarak projeyi düzenle**" dendiğinde izlenecek adımları tanımlar. Amaç: mevcut projeyi bozmadan, kanıta dayalı, geri alınabilir adımlarla bu yapıya yaklaştırmak. Doküman **hedef**tir; her projede her bölüm uygulanmaz — profil (Bölüm 1.3) ve eşikler (Bölüm 24) belirler.

### 26.1 Temel kurallar (ajan için)

1. **Önce oku, sonra yaz.** Kod değiştirmeden önce 26.2'deki keşif tamamlanır ve bir "uyum raporu" üretilir. Rapor onaylanmadan yapısal değişiklik yapılmaz.
2. **Kanıt disiplini.** Her bulgu dosya/satır ile; doğrulanamayan "net kanıt bulunamadı" olarak yazılır; uydurulmaz.
3. **Bozma.** Çalışan davranış, public API, mevcut migration'lar ve deploy akışı korunur. Kırıcı değişiklik yalnız ADR + kullanıcı onayı ile.
4. **Küçük ve geri alınabilir adımlar.** Her adım ayrı PR/commit; her biri kendi başına yeşil (testler + mevcut CI).
5. **Sıra sabittir:** güvenlik ve veri kaybı riskleri → makine kuralları → yapı → altyapı. "Güzel yapı" için veri kaybı riski ertelenmez.
6. **Profil seç, profil dışını yapma.** P0 için Bölüm 1.3'teki liste; P1/P2 maddeleri "sonraki adım" olarak rapora yazılır, uygulanmaz.
7. **Kullanıcıya sor:** mimari şekil (1.1), profil (1.3), kırıcı değişiklikler, silinecek/yeniden adlandırılacak şeyler, secret rotasyonu.

### 26.2 Adım 1 — Keşif ve uyum raporu (kod değişikliği yok)

Çıktı: `docs/adr/0001-referans-mimariye-uyum.md` (ADR) + `docs/uyum-raporu.md`.

Keşif listesi (her satır: mevcut durum · referans bölümü · fark · risk · profil):
- Repo ve modül topolojisi; api/core ayrımı var mı; ortak kütüphane(ler); build/deploy hattı (nerede build ediliyor, registry var mı).
- Servis kimlik tablosu (port, actor, audience, şema, hata bloğu); **sıcak yol tablosu** (her kritik istek kaç uzak senkron çağrı yapıyor).
- Veri: DB sayısı/instance, şema/rol ayrımı, **yedek/PITR/restore provası**, büyüyen tablolar, ID stratejisi.
- Mesajlaşma: broker ve sürümü, outbox var mı/kaç tane, komut/olay ayrımı, consumer ayarları (requeue, prefetch), DLQ.
- Güvenlik: JWT türleri ve algoritmaları, secret'ların yeri ve fallback'ler, allowlist/drift, rate limit, mock entegrasyonlar, seed verisi, dosya yükleme, PII/silme akışı.
- Dayanıklılık: timeout'lar, circuit breaker, thread modeli, control-plane bağımlılıkları.
- Gözlem: log formatı, toplayıcı (Promtail?), tracing, **alarm kanalı**, SLO, runbook.
- Test: gerçek DB testleri CI'da mı, ArchUnit var mı, hata kodu tekilliği, drift testi.
- Sürümler: her bileşen için OSS destek durumu (Bölüm 25) ve bilinen CVE'ler.
- AI yönetişimi: AGENTS.md, docs/ai, skill'ler, hook'lar var mı; çalışıyor mu (kuru çalıştırma).

Rapor sonu: **profil önerisi**, **şekil kararı önerisi**, ve 26.3'e göre sıralanmış adım listesi (her biri tahmini büyüklük ve risk ile).

### 26.3 Adım 2 — Uygulama sırası

| Faz | Ne | Neden önce |
|---|---|---|
| **F0 — Kanama durdur** | Yedek + restore provası; alarm kanalı; secret fallback'lerini kaldır + `.dockerignore`; mock entegrasyonlara profil guard'ı; seed verisini prod location'ından çıkar; ban/rol → `sv`; destek dışı sürümlerde bilinen CVE'ler için acil önlem | Veri kaybı ve güvenlik; hepsi küçük ve geri alınabilir |
| **F1 — Makine kuralları** | `blueprint/` kopyala (AGENTS.md, docs/ai, skill'ler, hook'lar); immutability script + CI; ArchUnit (önce **raporlama modu**: mevcut ihlalleri listele, sonra kural kural aç); ErrorCode tekillik testi; config drift testi; gerçek DB testlerini CI'da aç | Sonraki her adım bu ağın içinde yapılır; ihlaller görünür olur |
| **F2 — Sınırlar** | DB rolleri + GRANT (önce `svc_*` rolleri oluştur, sonra bağlantıları taşı); api/core ayrımı ve split package düzeltmesi; ortak kütüphaneyi starter'lara bölme; controller→repository ve core→core ihlallerini kapatma | Yapısal temizlik; ArchUnit ihlal listesi sıfıra iner |
| **F3 — Sıcak yol** | Sıcak yol tablosu; okuma amaçlı senkron çağrıları read-model/claim'e çevir (önce en yüksek trafikli akış); timeout bütçesi + circuit breaker | Availability ve latency; en görünür kazanım |
| **F4 — Mesajlaşma** | Generic outbox'a geçiş (yeni outbox'lar önce, eskiler kademeli); komut/olay ayrımı; `domain.events` topic; consumer ayarları; broker sürümü | Analytics/read-model'lerin ön koşulu |
| **F5 — Gözlem ve operasyon** | Structured log, Alloy, exporter'lar, SLO + burn-rate, runbook'lar, tail sampling; CI'da image build → registry → digest; `docker-rollout` | F3–F4'ün etkisi ölçülür |
| **F6 — Sürüm ve platform** | Boot/Cloud/broker/DB major upgrade'leri (Bölüm 25 sırası); profil geçişleri (Bölüm 1.3), eşik tablosu (Bölüm 24) | En riskli, en son; önceki fazların test ağı olmadan yapılmaz |

Her faz sonunda: uyum raporu güncellenir, ilgili review skill'leri çalıştırılır, `proj-release-readiness-review` ile durum `PASS/FAIL/BLOCKED` olarak kaydedilir.

### 26.4 Adım 3 — Yeni projede (sıfırdan)

1. `blueprint/` kopyala, yer tutucuları değiştir, symlink ve hook testlerini çalıştır (`blueprint/README.md`).
2. `skeleton-example`'ı temel al: `platform-core` + `<domain>-api/core` + enforcer + testler; `mvn test` yeşil olmadan ilk özellik yazılmaz.
3. ADR-0001: şekil (1.1) ve profil (1.3). ADR-0002: veri stratejisi (10.1/10.5). ADR-0003: mesajlaşma (12).
4. Bölüm 21.0 checklist'ini P0 satırlarıyla uygula; P1/P2 satırlarını "sonraki" olarak issue'lara aç.
5. İlk özellikten önce: yedek + alarm + secret hijyeni + CI'da gerçek DB testleri. Bunlar "ilk özellikten sonra" yapılmaz.

### 26.5 Ajan çıktı formatı ("kendini bu yapıya göre düzenle" isteğine cevap)

1. **Keşif özeti** (10–20 satır): şekil, profil, en kritik 5 fark.
2. **Uyum raporu tablosu** (bölüm · mevcut · fark · risk · faz).
3. **Önerilen ilk PR** (F0'dan, tek konu, geri alınabilir) ve neden.
4. **Sorular** (26.1/7).
5. Onay sonrası: her PR için `docs/ai/review-checklist.md`'deki skill'ler ve PR şablonu.

**Kaçın:** Tek dev PR ile "her şeyi düzenlemek"; ArchUnit'i ilk gün tüm kurallarla zorunlu yapıp build'i günlerce kırık bırakmak; upgrade'i test ağı olmadan yapmak; profil dışı altyapı (Kafka, k8s, Vault) kurmak; mevcut migration'lara dokunmak; secret değerlerini rapora yazmak.

---

# Ek A — Blueprint Dosyaları (repoya birebir kopyalanır)

İçindekiler:

- `README.md`
- `AGENTS.md`
- `CLAUDE.md`
- `.github/copilot-instructions.md`
- `.github/PULL_REQUEST_TEMPLATE.md`
- `docs/ai/repo-context.md`
- `docs/ai/security-rules.md`
- `docs/ai/context-boundaries.md`
- `docs/ai/review-checklist.md`
- `docs/ai/operation-consistency.md`
- `docs/adr/0000-template.md`
- `.agents/skills/proj-api-contract-review/SKILL.md`
- `.agents/skills/proj-architecture-boundary-review/SKILL.md`
- `.agents/skills/proj-client-integration-doc/SKILL.md`
- `.agents/skills/proj-client-integration-doc/template.md`
- `.agents/skills/proj-db-migration-review/SKILL.md`
- `.agents/skills/proj-environment-impact-review/SKILL.md`
- `.agents/skills/proj-event-design-review/SKILL.md`
- `.agents/skills/proj-operation-consistency-review/SKILL.md`
- `.agents/skills/proj-operation-consistency-review/references/assessment.md`
- `.agents/skills/proj-operation-consistency-review/references/implementation.md`
- `.agents/skills/proj-operation-consistency-review/references/verification.md`
- `.agents/skills/proj-release-readiness-review/SKILL.md`
- `.agents/skills/proj-resilience-review/SKILL.md`
- `.agents/skills/proj-security-review/SKILL.md`
- `.agents/skills/proj-spring-code-review/SKILL.md`
- `.agents/skills/proj-test-writer/SKILL.md`
- `.claude/settings.json`
- `.claude/hooks/flyway-immutability.js`
- `.claude/hooks/review-gate.sh`
- `.claude/hooks/review-stamp.sh`
- `scripts/flyway-immutability.js`
- `scripts/flyway-immutability.test.js`
- `tests/ArchitectureRulesTest.java`
- `tests/ErrorCodeUniquenessTest.java`
- `tests/ConfigDriftTest.java`

---

### `README.md`

### Blueprint — Yeni Proje İskeleti (AI Yönetişimi, Skill'ler, Hook'lar, Makine Kuralları)

Bu klasör `docs/mikroservis-mimari-referans.md`'nin **kopyalanabilir** parçasıdır. Yeni bir repo açarken bu klasörün içeriği repo köküne kopyalanır, `<proje>`/`proj-` yer tutucuları proje adıyla değiştirilir.

```
blueprint/
├── AGENTS.md                         # Tüm AI ajanları için kanonik giriş (okuma sırası + temel kurallar)
├── CLAUDE.md                         # Yalnız AGENTS.md'ye yönlendirir
├── .github/
│   ├── copilot-instructions.md       # Yalnız AGENTS.md'ye yönlendirir
│   └── PULL_REQUEST_TEMPLATE.md      # Çalıştırılan review skill'leri ve kararları burada kayda geçer
├── docs/
│   ├── ai/
│   │   ├── repo-context.md           # Modül haritası, portlar, stack, yüksek sinyalli dosyalar, sıcak yol tablosu
│   │   ├── security-rules.md         # Secret, log, JWT, internal uç, rate limit, privacy, dosya kuralları
│   │   ├── context-boundaries.md     # Token ekonomisi: hariç klasörler, şartlı açılacak yüzeyler
│   │   ├── review-checklist.md       # Değişiklik sonrası kontrol listesi (skill'lere link)
│   │   └── operation-consistency.md  # Servisler arası tutarlılık standardı (outbox / event / saga)
│   └── adr/
│       └── 0000-template.md          # Architecture Decision Record şablonu
├── .agents/skills/                   # TEK KAYNAK — .claude/skills buna symlink'tir
│   ├── proj-spring-code-review/SKILL.md
│   ├── proj-security-review/SKILL.md
│   ├── proj-db-migration-review/SKILL.md
│   ├── proj-api-contract-review/SKILL.md
│   ├── proj-architecture-boundary-review/SKILL.md
│   ├── proj-environment-impact-review/SKILL.md
│   ├── proj-operation-consistency-review/{SKILL.md, references/{assessment,implementation,verification}.md}
│   ├── proj-resilience-review/SKILL.md          # YENİ: sıcak yol, timeout, circuit breaker, read-model
│   ├── proj-event-design-review/SKILL.md        # YENİ: komut/event ayrımı, envelope, şema evrimi, read-model
│   ├── proj-release-readiness-review/SKILL.md   # YENİ: yedek, alarm, SLO, runbook, sürüm/EOL, kapasite
│   ├── proj-test-writer/SKILL.md
│   └── proj-client-integration-doc/{SKILL.md, template.md}
├── .claude/
│   ├── settings.json                 # Hook tanımları
│   └── hooks/
│       ├── flyway-immutability.js    # PreToolUse: base'teki V*.sql'e yazmayı engeller (fail-closed)
│       └── review-gate.sh            # PreToolUse(Bash git push): son 1 saatte review skill'i çalıştı mı
├── scripts/
│   ├── flyway-immutability.js        # Kuralın TEK kaynağı: CI + hook + elle kullanım
│   └── flyway-immutability.test.js   # node --test
├── tests/                            # Makine zorlamalı kurallar için Java test şablonları (skeleton-example'da doğrulandı)
│   ├── ArchitectureRulesTest.java    # ArchUnit (düz @Test): katmanlar, controller→repository yok, core→core yok, config/, @Valid, döngü yok
│   ├── ErrorCodeUniquenessTest.java  # Tüm ErrorCode enum'larında global tekillik + blok + mesaj formatı
│   └── ConfigDriftTest.java          # application-local.yml ↔ deploy config drift; ${ENV} ↔ env şablonu; secret fallback yasağı
└── skeleton-example/                 # Boot 4.1.1 + ArchUnit 1.5.1 ile `mvn test` yeşil; 8 kasıtlı ihlal yakalandı (README'sine bak)
    ├── pom.xml                       # BOM, ${revision}, enforcer (Java/Maven sürümü + core→core bannedDependencies)
    ├── platform-core/  order-api/  order-core/  deploy/prod.env.example
```

**Doğrulanmış olanlar:** `scripts/flyway-immutability.js` (12 test), hook'lar (örnek stdin ile kuru çalıştırma), `tests/*.java` + enforcer (`skeleton-example` içinde `mvn test`, negatif ve pozitif). Skill'ler metin olarak tamamlandı; gerçek bir PR üzerinde bir Claude Code oturumunda henüz koşturulmadı — ilk kullanımda karar formatlarının uyumu gözden geçirilir.

#### Kurulum

```bash
cp -r blueprint/. <yeni-repo>/
cd <yeni-repo>
grep -rl "proj-\|<proje>" . --exclude-dir=.git | xargs sed -i 's/proj-/<proje>-/g; s/<proje>/<proje-adı>/g'
ln -s ../.agents/skills .claude/skills          # kopya değil, symlink
chmod +x .claude/hooks/review-gate.sh
node --test scripts/flyway-immutability.test.js  # script'in kendi testleri
bash -n .claude/hooks/review-gate.sh && echo '{"tool_input":{"command":"git push"}}' | .claude/hooks/review-gate.sh   # hook kuru çalıştırma
```

#### İlkeler

1. **Tek kaynak:** Her kural bir dosyada yaşar; diğerleri anchor link ile yönlendirir. Skill'ler `docs/ai/*`'ı tekrar etmez.
2. **Kanıt zorunluluğu:** Doğrulanamayan şey "**net kanıt bulunamadı**" diye yazılır; uydurulmaz.
3. **Kural → makine:** Her kuralın bir makine kontrolü vardır (ArchUnit, enforcer, hook, CI script, test). Skill'ler "ne yapmalı"yı, testler "yapıldı mı"yı taşır.
4. **Sabit karar formatları:** Her skill'in çıktısı sabit enum'larla biter (`APPROVE / REQUEST CHANGES / BLOCK`, `PASS / FAIL / BLOCKED`); serbest metin karar sayılmaz.
5. **Skill'ler kısa ve test edilebilir:** Her madde bir dosyaya bakarak evet/hayır denebilecek biçimde yazılır.

---

### `AGENTS.md`

### AGENTS.md — <proje> için AI Ajan Kuralları

Bu dosya tüm AI kodlama ajanları (Claude Code, Codex, Copilot, Cursor vb.) için **kanonik giriş noktasıdır**. `CLAUDE.md` ve `.github/copilot-instructions.md` yalnız buraya yönlendirir. Kurallar burada **tekrar edilmez**; ilgili dosyaya link verilir.

#### 1. Okuma Sırası

Önce bu dosya. Sonra göreve göre:

| Görev | Aç |
|---|---|
| Her görev | `docs/ai/repo-context.md` (modül haritası, portlar, sıcak yol tablosu) |
| Güvenlik, kimlik, secret, log, dosya, privacy | `docs/ai/security-rules.md` |
| Büyük repo taraması gerekiyorsa | `docs/ai/context-boundaries.md` (neyi açma, neyi şartlı aç) |
| Değişiklik bitince | `docs/ai/review-checklist.md` (hangi skill'ler çalışacak) |
| Servisler arası yazma, outbox, event, saga | `docs/ai/operation-consistency.md` |
| Mimari karar (yeni servis, yeni altyapı bileşeni, veri ayrımı) | `docs/adr/` (mevcut ADR'leri oku, yenisini şablondan aç) |
| Mimari referans (nasıl inşa edilir) | `docs/mikroservis-mimari-referans.md` — yalnız ilgili bölüm |

#### 2. Temel Kurallar

- Gereksiz geniş tarama yapma; `docs/ai/context-boundaries.md`'deki sınırlara uy.
- Doğrulanmamış bilgiyi kesin yazma. Dosya yolu, key, env, path, tablo, schema, secret, paket veya servis adı **uydurma**. Doğrulayamıyorsan "**net kanıt bulunamadı**" yaz ve sor.
- Mevcut bir pattern varsa onu kullan; yoksa yenisini icat etmeden önce sor.
- Secret, token, parola, private key değerlerini hiçbir yeni içeriğe (kod, doküman, test, log, PR açıklaması) taşıma. Yalnız isimleri yaz.
- Aynı kuralı iki yerde yazma; belgeye link ver.
- Her değişiklikte ownership kontrolü: kimlik her zaman doğrulanmış bağlamdan (`@CurrentAccount`), path/body'den değil.
- Ana README kökteki `README.md`'dir; servis kimlik tablosu ve hata kodu blokları oradadır.

#### 3. Kanıt ve Varsayım Disiplini

- Varsayımla kod, config, migration, endpoint veya güvenlik kuralı üretme.
- Kritik bir belirsizlik varsa (güvenlik, veri kaybı, ödeme, yasal) kod yazmadan önce sor.
- Bir iddiayı yazarken dayandığı dosyayı/satırı belirt.

#### 4. Modül Sınırı ve DB Erişimi

- Hiçbir modül başka modülün tablosuna, schema'sına, repository'sine, entity'sine veya migration'ına erişmez. **Yönetim (backoffice) servisi de istisna değildir.**
- Cross-schema FK ve join yasaktır. Başka servisin kimliği düz UUID kolon olarak tutulur.
- Başka servisin verisi gerekiyorsa: public API/contract, event ile replike edilen read-model (`readmodel/` paketi) veya JWT claim. Böyle bir ihtiyaç doğarsa **kod yazmadan önce** `proj-architecture-boundary-review` skill'ini çalıştır ve sor.
- DB rolleri şema bazlıdır; kod bu sınırı aşmaya çalışırsa çalışma zamanında hata alır. Bu hatayı "GRANT ekleyerek" çözme.

#### 5. Sıcak Yol Kuralı

- Kullanıcıya latency olarak yansıyan bir istekte **en fazla bir** uzak senkron çağrı olur, o da yazma/rezervasyon türünden. Okuma amaçlı senkron çağrı eklemek yasaktır; read-model veya JWT claim kullan.
- Yeni bir uzak çağrı ekliyorsan `docs/ai/repo-context.md`'deki sıcak yol tablosunu güncelle ve `proj-resilience-review` skill'ini çalıştır.
- Her HTTP client çağrısı timeout + circuit breaker + bulkhead altındadır; bunlar olmadan client ekleme.

#### 6. Migration Değişmezliği

- Base branch'e (`develop`/`release`/`main`) girmiş `V*.sql` dosyasına dokunulmaz (değiştirme, silme, yeniden adlandırma yok). Düzeltme yeni `V<sonraki>` ile yapılır.
- Migration'a her dokunuştan sonra: `node scripts/flyway-immutability.js check --base origin/<hedef-branch>`.
- Hook bunu yazma anında engeller; engellenirse kuralı aşmaya çalışma, yeni dosya aç.

#### 7. API / Core Contract

- `<domain>-api` yalnız DTO, enum, sabit ve event payload'ı taşır; hiçbir `*-core`'a bağımlı olamaz.
- `<domain>-core` başka bir `*-core`'a bağımlı olamaz (enforcer bunu kırar).
- Entity dışarı açılmaz; DTO kopyalanmaz; hedefin api modülü import edilir.
- Contract değişikliği geriye uyumluluk açısından mobil, panel ve internal çağıranlar için değerlendirilir; `proj-api-contract-review` çalıştırılır.

#### 8. Event ve Outbox

- Başka servisin verisini değiştirmek için komut gönderilmez; kendi domain event'in yayınlanır (`outbox_event`, `kind=EVENT`).
- Yayın her zaman outbox'tan; doğrudan `convertAndSend` yasak.
- Yeni event/komut için `proj-event-design-review` çalıştırılır; tüketici önce deploy edilir.

#### 9. Environment ve Config Etkisi

- Yeni property, URL, port, audience, issuer, rate limit scope'u, bağlantı ayarı, internal uç veya feature flag eklendiğinde şu yüzeyler **birlikte** güncellenir: deploy `env_file`/secrets, `config/<svc>.yml`, `application-local.yml`. Dockerfile yalnız build/runtime davranışı değişiyorsa.
- Dockerfile'a config için `ENV` eklenmez. Secret için `${ENV:literal}` fallback yazılmaz.
- Admin'in değiştirebileceği iş kuralı değeri config'e değil parametre kataloğuna gider.
- `proj-environment-impact-review` çalıştırılır.

#### 10. Multi-Instance

- Her servisin birden fazla instance ile çalıştığı varsayılır. `@Scheduled`/poller/worker `FOR UPDATE SKIP LOCKED` + lease + `claim_token` ile claim eder.
- Check-then-act güvenli değildir; unique/partial unique index, koşullu update veya lock kullan.
- Instance'lar arası state JVM'de tutulmaz. Bilinçli single-instance davranış yorumla gerekçelendirilir.

#### 11. Loglama

- Throw öncesi tek structured log satırı: `"<Olay> rejected: code=X reason=Y"`. Kullanıcı kaynaklı reddetme WARN, altyapı/beklenmeyen ERROR.
- Ham exception mesajı, body, token, OTP, telefon, e-posta, plaka, konum, IP, şifreli içerik loglanmaz. `docs/ai/security-rules.md`.

#### 12. Kod Açıklama Disiplini

- Yorumlar kısa, ekibin dilinde (Türkçe, ASCII), "**neden** var" anlatır. Changelog tarzı yorum ("şu güncellendi") yasak.
- Business rule, güvenlik, modül sınırı, fallback, rate limit, ownership, migration etkisi ve non-obvious kararlar için yorum şart. Kendini açıklayan koda yorum yazılmaz.

#### 13. Çıktı Disiplini

- Kısa, kanıta dayalı, görev odaklı. Güvenlik ve privacy riski açıkça yazılır. Belirsiz alan gizlenmez.
- Push öncesi `docs/ai/review-checklist.md`'deki skill'ler çalıştırılır; PR şablonundaki "Review skill'leri" bölümü doldurulur.

#### 14. Bu Belgede Özellikle Taşınmayanlar

Secret değerleri, default admin parolası, üretim host adları, CI token'ları. Bunlar deploy secret'larında yaşar; dokümanlarda yalnız **isimleri** geçer.

#### 15. Net Kanıt Bulunamayan Alanlar

Her `docs/ai/*` dosyasının sonunda bu başlık bulunur; ajan doğrulayamadığı iddiaları buraya ekler, uydurmaz.

---

### `CLAUDE.md`

### CLAUDE.md

Bu repo için tüm kurallar `AGENTS.md` dosyasındadır. Önce onu oku; göreve göre yönlendirdiği `docs/ai/*` dosyalarını aç.

Kural burada tekrar edilmez. Proje skill'leri `.claude/skills/` altındadır (`.agents/skills/`'e symlink). Değişiklik bitince `docs/ai/review-checklist.md`'deki skill'leri çalıştır.

---

### `.github/copilot-instructions.md`

### Copilot Instructions

Bu repo için tüm kurallar `AGENTS.md` dosyasındadır. Önce onu oku; göreve göre yönlendirdiği `docs/ai/*` dosyalarını aç. Kural burada tekrar edilmez.

Özellikle: secret değeri yazma; modül sınırını aşma; base'teki migration'a dokunma; sıcak yola okuma amaçlı uzak çağrı ekleme; kimliği path/body'den alma.

---

### `.github/PULL_REQUEST_TEMPLATE.md`

### Ne değişti

<!-- 2–5 madde. Neden yapıldığı; ne yapıldığı değil. -->

### Etki

- [ ] Contract (api modülü, OpenAPI diff): değişti / değişmedi — breaking: evet / hayır
- [ ] Migration: var / yok — `node scripts/flyway-immutability.js check --base origin/<hedef>` ✅
- [ ] Config/env/secret yüzeyi: `env_file` · `config/<svc>.yml` · `application-local.yml` · Dockerfile — güncellendi / etkilenmedi
- [ ] Yeni uzak senkron çağrı: var / yok — varsa sıcak yol tablosu güncellendi (`docs/ai/repo-context.md`)
- [ ] Yeni event/komut: var / yok — varsa tüketici önce deploy edilecek
- [ ] İstemciyi etkiliyor: evet / hayır — `docs/<client>-<feature>-integration-vN.md`: `…`
- [ ] Güvenlik/privacy etkisi: var / yok — özet: …

### Çalıştırılan review skill'leri ve kararları

<!-- Her satır: skill adı → nihai karar (sabit enum). Çalıştırılmadıysa "atlandı: <neden>". -->

| Skill | Karar |
|---|---|
| proj-spring-code-review | |
| proj-security-review | |
| proj-db-migration-review | |
| proj-api-contract-review | |
| proj-architecture-boundary-review | |
| proj-environment-impact-review | |
| proj-operation-consistency-review | |
| proj-resilience-review | |
| proj-event-design-review | |
| proj-test-writer | |
| proj-release-readiness-review (release PR'larında) | |

### Doğrulama

<!-- Nasıl doğrulandı: hangi testler, izole DB'de ayağa kaldırıldı mı, yük testi (gerekiyorsa). -->

### Net kanıt bulunamayan alanlar

<!-- Doğrulanamayan varsayımlar; boşsa "yok". -->

---

### `docs/ai/repo-context.md`

### repo-context.md — <proje> Repo Haritası

> Amaç: Ajanın ilk 2 dakikada sistemi anlaması. Kurallar burada değil; `AGENTS.md` ve `security-rules.md`'de. Bu dosya **gerçekleri** taşır ve her yeni servis/uç/olayda güncellenir.

#### 1. Mimari şekil (ADR-0001)

- Şekil: **hibrit** — çekirdek domain'ler (`auth`, `user`, `<ana-iş>`, `subscription`) tek uygulama (`core-app`) içinde Spring Modulith modülleri; `chat` (realtime), `notification` (dış sağlayıcılar), `backoffice` (yönetim) ayrı servisler. *(Projeye göre düzenle: modüler monolit / mikroservis.)*
- Yeniden değerlendirme eşiği: referans Bölüm 24 (≥2 ekip, farklı ölçek profili, ikinci host).
- Repo: Maven multi-module monorepo; `platform/*` starter'ları; `services/<domain>/<domain>-api|core`.

#### 2. Servis kimlik tablosu

| Servis | Port | `application.name` | Actor / `iss` | Audience | DB schema / rol | Hata kodu bloğu | Yayınladığı olaylar | Tükettiği olaylar | Sıcak yol uzak çağrı sayısı |
|---|---|---|---|---|---|---|---|---|---|
| gateway | 8080 | gateway | gateway | route metadata | – | – | – | – | – |
| core-app (auth, user, order, subscription modülleri) | 8081 | core | core-service | core-api | `auth`,`users`,`order`,`subscription` / `svc_*` | 10000–14999 | `account.*`, `user.*`, `order.*`, `subscription.*` | – | order.create: **1** (subscription consume, in-process değilse) |
| chat | 8094 | chat | chat-service | chat-api | `chat` / `svc_chat` | 15000–15999 | `chat.message.sent` (metadata) | `account.standing.changed`, `user.block.created` | message.send: **0** |
| notification | 8091 | notification | notification-service | notification-api | `notification` / `svc_notification` | 16000–16999 | `notification.delivered` | `*.commands`, `order.order.created`, … | – |
| backoffice | 8095 | backoffice | backoffice-service | backoffice-api | `backoffice` / `svc_backoffice` | 17000–17999 | `parameter.revision.published`, `moderation.action.applied` | `*.report.created` | – |

Ortak kod blokları: validation 90000, security 90100–90199, system 99998–99999.

#### 3. Sıcak yol tablosu (referans Bölüm 1.2)

| Akış | Uç | Uzak senkron çağrı | Read-model / claim ile karşılanan kontroller | Hedef p99 |
|---|---|---|---|---|
| Ana yazma | `POST /v1/orders` | 1 (hak tüketimi) | hesap durumu (`account_standing` read-model), yasal onay (`legal_ok` claim) | 300 ms |
| Mesaj gönder | `POST /v1/conversations/{id}/messages` | 0 | engel (`block_relation` read-model), üyelik (local) | 150 ms |
| Giriş | `POST /v1/auth/password/login` | 0 | – | 500 ms |

Bu tabloya 1'den fazla uzak çağrı yazılamaz; yazılmak isteniyorsa ADR + `proj-resilience-review`.

#### 4. Yüksek sinyalli dosyalar

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

#### 5. Altyapı

| Bileşen | Sürüm | Not |
|---|---|---|
| Java / Spring Boot / Spring Cloud | 25 / 4.x / 2025.1.x | OSS destek kontrolü: `docs/versions.md` (tarihli) |
| PostgreSQL | 18 | tek instance, şema+rol/servis, PgBouncer, WAL-G → S3 |
| Valkey | 9 ×2 | `security` (noeviction+AOF, Sentinel) / `cache` (allkeys-lru) |
| RabbitMQ | 4.3 | quorum queue; `domain.events` topic; streams: `domain.events.stream` |
| Object storage | S3 uyumlu | `quarantine`, `delivery` (private), signed GET |
| Gözlem | Alloy → Loki/Tempo; Prometheus + Alertmanager; Grafana | portlar yalnız 127.0.0.1 |

#### 6. Komutlar

```bash
mvn -B -ntp verify -pl services/order/order-core -amd      # değişen modül + bağımlıları
node scripts/flyway-immutability.js check --base origin/develop
npm --prefix <panel>-web run lint && npm --prefix <panel>-web run test
docker compose -f deploy/docker-compose.local.yml up -d     # local altyapı
```

#### 7. Bu Belgede Özellikle Taşınmayanlar

Secret değerleri, üretim host adları, CI token'ları, kişisel veri örnekleri.

#### 8. Net Kanıt Bulunamayan Alanlar

- (ajan doğrulayamadığı iddiaları buraya ekler; boşsa "yok")

---

### `docs/ai/security-rules.md`

### security-rules.md — Güvenlik ve Privacy Kuralları

> Tek kaynak. Skill'ler (`proj-security-review` başta) buraya link verir, tekrar etmez. İhlal = `REQUEST CHANGES` veya `BLOCK`.

#### 1. Secret ve Config

- Secret değeri (parola, token, private key, API key, salt/pepper) **hiçbir** kod, config, doküman, test, log, PR metni veya commit mesajına yazılmaz. Yalnız **adı** geçer.
- Secret'lar `/run/secrets/` (compose `secrets:` + Spring config tree) ile gelir; git'te yalnız SOPS ile şifreli (`secrets/<env>.enc.yaml`).
- `${ENV:literal-varsayılan}` biçiminde secret fallback yazılmaz; secret eksikse uygulama **açılmaz** (fail-fast).
- Config Server kullanılıyorsa secret taşımaz.
- `.dockerignore`: `.env*`, `secrets/`, `.git`, `**/target`, `node_modules`.
- Local yml'de gerçek secret bulunmaz; local için ayrı, açıkça sahte değerler (`local-only-…`) ve profil guard'ı.

#### 2. Kimlik ve Yetki

- Üç JWT yüzeyi (**user**, **service**, **admin**) birbirinin yerine kabul edilmez: farklı `typ`, farklı anahtar, farklı `aud`.
- Service JWT **asimetrik** (EdDSA/ES256); her imzalayıcının kendi anahtar çifti; `iss` = imzalayan; doğrulama JWKS + `kid`. Simetrik paylaşılan secret **yasak**.
- Hesap kimliği **yalnız** doğrulanmış bağlamdan (`@CurrentAccount` ← service JWT `sub`). Path/query/body'den kimlik alınmaz (IDOR). Path'teki id yalnız hedef kaynaktır; ownership serviste kontrol edilir.
- `/internal/**` uçları default-deny; `service-jwt.internal-access` allowlist'i gerçek kullanım kadar dar; dar kural catch-all'dan önce (first-match). Allowlist eşleşmesi decode+normalize edilmiş path üzerinde.
- Kullanıcı adına çalışan internal uçta JWT `sub` path'teki hesapla karşılaştırılır. Hesabı body'den alan internal uç hiçbir aktöre açılmaz.
- Gateway: `/internal/**` → 404 (prefix kırpma sonrası da), iç header'ları (`X-Subject-Id`, `X-User-*`) temizler, `X-Forwarded-*` yalnız güvenilen proxy'den, CORS listesi açık (`*` yasak), rate limit ve timeout bütçesi var, `gateway` actuator ucu kapalı.
- Oturum sürümü (`sv`): şifre sıfırlama, logout-all, **ban**, rol değişimi → `sv + 1`; gateway ve realtime bu sürümün altını reddeder.
- Refresh token: opak, `sha256(plain:salt)` ile saklanır, aile rotasyonu, reuse → aile iptali. Mutlak süre uzamaz.
- Admin 2FA: passkey/WebAuthn veya TOTP. SMS yalnız kurtarma. Admin OTP'si de tuzlu hash + sabit zamanlı karşılaştırma.
- Roller token'da taşınıyorsa değişimde `sv` artar; panel ve sunucu aynı rol kaynağını okur.

#### 3. OTP, Şifre, Abuse

- OTP: `SecureRandom`, 6 hane, `sha256(salt:code)`, sabit zamanlı karşılaştırma, TTL + deneme sınırı, enumeration koruması (bilinmeyen numaraya aynı yanıt). Outbox payload'ında OTP düz metin **kalmaz** (iletim sonrası NULL/şifreli).
- SMS pumping: ülke/prefix allowlist, `libphonenumber` doğrulama, numara/IP/cihaz başına günlük tavan, velocity alarmı, harcama kesicisi.
- Cihaz attestation (Play Integrity / App Attest) kayıt, OTP-gönder ve suistimale açık aksiyonlarda **kademeli risk sinyali** (ikili kapı değil).
- Şifre: BCrypt, 72 byte sınırı uzunluk kuralına yansır; politika parametre kataloğundan; hatalı denemede kilit (`retryAfterSeconds` details'te).
- Rate limit: Redis Lua fixed window, key `rl:<scope>:<sha256>`; sayaç JVM'de tutulmaz; scope'lar `<aksiyon>-ip|account|transaction|device`; fail politikası scope başına README tablosunda (güvenlik yüzeyleri fail-closed, iş yüzeyleri fail-open + metrik).
- Mock entegrasyonlar (mağaza makbuzu, SMS, ödeme) yalnız `@Profile("local|test")`; prod açılışında mock bean varsa uygulama **açılmaz**.

#### 4. Loglama ve Hata Yanıtı

**Hiçbir seviyede loglanmaz:** OTP, access/refresh/service/admin token, şifre, secret, salt, private key; ham telefon, e-posta, kimlik no, plaka, kesin konum, IP, user-agent; şifreli içerik ve anahtar; request/response body, DTO/entity `toString()`, ham path/query/header, credential içeren URL; sağlayıcı yanıtı, ham exception mesajı, stack trace (yalnız 500'lerde sanitize edilmiş özet + trace).

- Throw öncesi tek structured log: `"<Olay> rejected: code=X reason=Y"`. Yalnız `code=`, `reason=`, `category=`, `exceptionType=` (`getSimpleName`), gerekli teknik id.
- Log'a özel neden `ServiceException.safeLogReason/safeLogCategory`'de; **asla** `details` içinde (details istemciye döner).
- `details` yalnız istemciye gösterilebilir `anahtar=değer` bilgisi taşır.
- UUID, hesap id, mask ve hash otomatik güvenli değildir; alanlar allowlist ile seçilir; trace context tercih edilir. Parameterized logging sanitization yerine geçmez.
- Structured JSON log; trace/kullanıcı id'si Loki **label** değil (structured metadata).
- Log privacy testi: sentetik hassas işaret **yok**, güvenli alan (`outcome=`) **var**.

#### 5. Veri ve Privacy

- Kişisel veri envanteri ve DPIA ürünle birlikte başlar. Aydınlatma metni alanları sayar (telefon, e-posta, kimlik, plaka, konum, belge, cihaz).
- Düşük entropili kimlikler (telefon, plaka, e-posta) için düz SHA-256 **yetersiz**: secret pepper'lı **HMAC** (arama) + şifreli orijinal (gösterim).
- Kimlik sorgulama uçlarında **varlık oracle'ı** yok: kayıtlı olmayan/gizli/engelli için tek tip "sonuç yok"; hız limiti, velocity alarmı, audit.
- Konum: rastgele fuzzing **yetersiz**; kullanıcı başına deterministik grid/ofset (~1–3 km), aynı yuvarlanmış nokta geo sorgusu ve filtrelerde; mesafe aralık olarak; kesin mesafe sıralaması yok.
- Kişisel veri dönen uçlar `Cache-Control: private, no-store`.
- Arama index'i, cache, log, yedek aynı kurallara uyar.
- Export (taşınabilirlik) keyset sayfalı internal uçlarla; **silme** silme saga'sı ile (tüm servisler, index, cache, object storage tüm versiyonlar, üçüncü taraflar); yasal saklama gerekenler anonimleştirilir; yedekler için crypto-shredding. KVKK 30 gün / GDPR 1 ay.
- Uçtan uca şifreleme iddiası varsa Signal/MLS; değilse "sunucu okuyamaz" ile sınırlı ve sınırlamalar yazılı. İstemci karşı tarafın anahtarını pin'ler. Şikayet kanıtı **message franking** ile; sohbet anahtarı panele verilmez. Kanıt erişimi audit'li, `no-store`, retention'lı.

#### 6. Dosya Yükleme

- Presigned PUT → **private quarantine** bucket → worker: magic byte/boyut/pixel-bomb → **yeniden kodlama** (EXIF/GPS temizlenir) → CSAM hash eşleme + NSFW sınıflandırma → private delivery bucket → kısa ömürlü signed GET / imzalı CDN.
- Public bucket'a doğrudan yükleme **yasak**.
- Yükleme sonrası HEAD/ETag doğrulaması; yetim dosya temizliği (cron + dağıtık kilit).

#### 7. Veritabanı

- Servis başına DB rolü; şema sahibi rol; başka şemaya USAGE yok. Cross-schema erişim hatası "GRANT ekleyerek" çözülmez.
- Audit tabloları `@Immutable` + DB'de `REVOKE UPDATE, DELETE` / trigger.
- Seed/test verisi prod migration location'ında değil; bilinen parolalı admin tohumlanmaz (bootstrap runner + env + ilk girişte değiştir).
- Yedek şifreli; restore provası aylık.

#### 8. Tracing

- Trace id yetki sinyali değildir. Span attribute'larına PII konmaz. Route'lar static template. Redis zarfında trace metadata HMAC kapsamında.

#### 9. Tedarik Zinciri

- Image CI'da build, cosign imzalı, digest ile deploy; non-root; base image Renovate ile güncel.
- gitleaks pre-commit + CI; Dependabot/GitHub Advisory alarmları; yaması yalnız ticari sürümde olan CVE = upgrade tetikleyicisi.
- GPL lisanslı kütüphane kapalı kaynak uygulamaya bağlanmadan hukuki inceleme.

#### 10. Bu Belgede Özellikle Taşınmayanlar

Secret değerleri, pepper/salt değerleri, üretim IP/host adları, mevcut CVE listesi (Dependabot'ta yaşar).

#### 11. Net Kanıt Bulunamayan Alanlar

- (ajan ekler)

---

### `docs/ai/context-boundaries.md`

### context-boundaries.md — Token Ekonomisi ve Tarama Sınırları

> Amaç: Ajanın gereksiz dosya okumasını önlemek; büyük repoda doğru yüzeyi doğru zamanda açmak.

#### 1. Varsayılan olarak context DIŞI

`**/target/`, `**/node_modules/`, `**/dist/`, `**/build/`, `.idea/`, `.vscode/`, `*.log`, `.env*`, `secrets/`, `**/*.enc.yaml`, `obs/**/data/`, `postman/`, `docs/<client>-*-integration-v*.md` (yalnız istemci dokümanı görevinde), `docs/adr/` (yalnız mimari karar görevinde), `**/src/test/resources/**/*.json` (fixture'lar), image/binary dosyalar.

#### 2. Şartlı açılacak yüzeyler

| Görev | Aç | Açma |
|---|---|---|
| Kod değişikliği (servis içi) | İlgili `*-core` modülünün `service`, `controller`, `repository`, `entity`, ilgili `*-api` DTO'ları, `config/<svc>.yml` | Diğer servislerin core'ları; `obs/`; workflow'lar |
| Yeni internal uç / client | Hedefin `*-api`'si, hedefin `config/<svc>.yml` (`internal-access`), çağıranın `client/` | Hedefin core iç sınıfları |
| Migration | İlgili `db/migration/` klasörü (yalnız son 3–5 dosya + `V1`), ilgili entity'ler | Diğer servislerin migration'ları |
| Event / read-model | `<domain>-api/event`, `platform-messaging`, tüketicinin `readmodel/` | – |
| Güvenlik incelemesi | `platform-security`, `gateway/config`, ilgili controller/filter | Frontend (ayrı görev) |
| Config/env etkisi | `deploy/`, `config/<svc>.yml`, `application-local.yml`, `.github/workflows/deploy.yml` | Dockerfile (yalnız build/runtime değişiyorsa) |
| Gözlemlenebilirlik | `obs/`, `platform-observability` | Servis iş kodu |
| İstemci dokümanı | `docs/<client>-<feature>-integration-v*.md` (son sürüm), OpenAPI çıktısı | Backend iç kodu (yalnız contract) |
| Frontend | `<panel>-web/src/{api,pages/<ilgili>,components/<ilgili>}` | `dist/`, tüm pages |

#### 3. Tarama disiplini

- Önce `docs/ai/repo-context.md` Bölüm 4 "yüksek sinyalli dosyalar"; sonra grep ile daraltılmış hedef; sonra dosya.
- Bir dosyayı tamamen okumadan önce boyutuna bak; 500 satırı geçen dosyada önce `grep -n "class\|public .*(" ` ile harita çıkar.
- Aynı dosyayı iki kez okuma; not al.
- "Bütün repoyu tara" isteği gelirse önce hangi soruya cevap arandığını netleştir; soruya göre yukarıdaki tablodan yüzey seç.
- Büyük doküman (referans, plan) yalnız ilgili bölümüyle açılır (`grep -n "^## "` ile başlık haritası → offset ile oku).

#### 4. Bu Belgede Özellikle Taşınmayanlar

Repo'nun dosya sayısı/boyutu gibi hızlı değişen sayılar.

#### 5. Net Kanıt Bulunamayan Alanlar

- (ajan ekler)

---

### `docs/ai/review-checklist.md`

### review-checklist.md — Değişiklik Sonrası Kontrol Listesi

> Push öncesi çalıştırılacak skill'ler ve hangi değişiklikte hangisinin zorunlu olduğu. Skill'ler `.claude/skills/` (→ `.agents/skills/`) altındadır. Her skill'in nihai kararı PR şablonundaki tabloya yazılır.

#### 1. Değişiklik türü → zorunlu skill'ler

| Değişiklik | Zorunlu | Koşullu |
|---|---|---|
| Her Java değişikliği | `proj-spring-code-review`, `proj-test-writer` | |
| Controller, filter, JWT, rate limit, log, dosya, privacy | `proj-security-review` | |
| `db/migration/` | `proj-db-migration-review` + `node scripts/flyway-immutability.js check` | |
| `*-api` DTO/enum/event, controller imzası, OpenAPI diff kırmızı | `proj-api-contract-review` | istemciyi etkiliyorsa `proj-client-integration-doc` |
| Yeni repository/entity/migration erişimi, yeni client, read-model | `proj-architecture-boundary-review` | |
| Yeni property/env/secret/port/audience/internal uç/flag | `proj-environment-impact-review` | |
| Servisler arası yazma, outbox, saga | `proj-operation-consistency-review` | |
| Yeni uzak senkron çağrı, timeout/circuit breaker değişikliği, sıcak yol | `proj-resilience-review` | |
| Yeni event/komut, envelope, tüketici, şema değişikliği | `proj-event-design-review` | |
| Release PR (`release`/`main`'e) | `proj-release-readiness-review` | |

#### 2. Makine kontrolleri (CI'da; lokalde de çalıştırılır)

```bash
mvn -B -ntp verify -pl <değişen modüller> -amd          # testler + ArchUnit + ErrorCode tekilliği + config drift
node scripts/flyway-immutability.js check --base origin/<hedef>
gitleaks detect --no-banner
npm --prefix <panel>-web run lint && npm --prefix <panel>-web run build && npm --prefix <panel>-web test
```

#### 3. Öz-kontrol (skill'lerden bağımsız)

- [ ] Yeni uzak senkron çağrı sıcak yola eklendi mi? Eklendiyse read-model/claim alternatifi değerlendirildi ve `repo-context.md` tablosu güncellendi.
- [ ] Yeni outbox satırı domain transaction'ında mı (MANDATORY)?
- [ ] Hata yolu: throw öncesi structured log; kullanıcı 4xx → WARN.
- [ ] Yeni `@RequestBody` → `@Valid`; binding adları açık.
- [ ] Config yüzeyleri birlikte güncellendi (`env_file`, `config/<svc>.yml`, `application-local.yml`).
- [ ] Testler: negatif + yetki + concurrency (poller/check-then-act) + log privacy.
- [ ] `docs/ai/repo-context.md` ve README kimlik tablosu güncel.
- [ ] Mimari karar verildiyse ADR açıldı.

#### 4. Karar formatları

- Review skill'leri: `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`.
- Tutarlılık ve release-readiness: `PASS` / `FAIL` / `BLOCKED`.
- Saga uygunluğu: `saga unnecessary` / `existing saga suitable` / `extension required` / `decision blocked`.
- Sorun yoksa sabit cümle: **"Bu kapsamda bulgu yok."** Spekülatif bulgu üretilmez; kanıt yoksa `needs verification`.

---

### `docs/ai/operation-consistency.md`

### operation-consistency.md — Servisler Arası Tutarlılık Standardı

> Tek kaynak. `proj-operation-consistency-review` ve `proj-event-design-review` buraya link verir. Referans: mimari doküman Bölüm 11–12.

#### 1. Mekanizma seçimi (en basit yeterli olan)

| Gereksinim | Mekanizma |
|---|---|
| Yalnız okuma | Hiçbiri (read-model veya claim; senkron okuma sıcak yolda yasak) |
| Tüm yazımlar tek local TX'te | Local TX + constraint |
| Tek mutation'ı duplicate'ten korumak | Idempotency key `(account_id, scope, operation_key)` / domain uniqueness |
| Commit sonrası başka servisin tepki vermesi | **Domain event** (outbox `kind=EVENT`) + idempotent consumer (inbox) |
| Dış sağlayıcıya iş emri (SMS, push, mail, webhook) | **Komut** (outbox `kind=COMMAND` → queue) veya `kind=HTTP` |
| Local commit + uzak **geri alınabilir** mutation (hak/stok tüketimi) | **Local saga** (tek adım, tek katılımcı) |
| Geri alınamaz etki / global atomiklik / uzun insan onayı | Saga uygun değil → ADR + mimari karar (genelde: süreç tablosu + durum makinesi + insan adımı) |

**Kural:** "Başka servisin verisini değiştir" komutu yoktur; olay yayınlanır, sahibi karar verir.

#### 2. Idempotency

- Public tekrar-güvenli işlemler `X-Idempotency-Key` (UUID) alır. Aynı niyetin retry'ları aynı key; yeni niyet yeni key.
- Tekillik `(account_id, scope, operation_key)`. Aynı key farklı body → ilk istek kazanır (fingerprint tutulmaz, belgelenir).
- Tamamlanmış istek aynı sonucu döner; süren `OPERATION_IN_PROGRESS`; iptal edilmiş `OPERATION_CANCELLED`.
- Veri seviyesinde: `INSERT … ON CONFLICT DO NOTHING`, deterministik id (`UUID.nameUUIDFromBytes(kaynak:hedef:faz)`) + unique.

#### 3. Outbox (tek generic tablo: `outbox_event`)

- Yazıcı `Propagation.MANDATORY`; domain TX'i olmadan outbox yazılamaz.
- Poller `platform-messaging`'den: CTE + `FOR UPDATE SKIP LOCKED` + `locked_until` lease + `claim_token`; uzak iş TX **dışında**; sonuç yalnız claim sahibi tarafından yazılır.
- Sabitler: batch 50, lease 120 sn, güvenlik payı 30 sn, backoff `min(600, 30·2^n)`, STUCK alarmı her 10 denemede (ERROR + `outbox_oldest_pending_age_seconds` metriği).
- DEAD politikası iş türüne göre: güvenlik yan etkisi (ban, engel) **asla DEAD olmaz**; TTL'li mesaj (OTP) `expires_at` sonrası DEAD; kalıcı 4xx (401/403/408/429 hariç) DEAD.
- Payload'da gereksiz PII/secret yok; iletim sonrası hassas alan NULL.
- Aynı `aggregate_id` için sıra korunacaksa tek worker/single-active-consumer.
- Sıra numaralı durum senkronu: karar DB sequence'ından sıra alır; alıcı eski sırayı yok sayar (`applied=false` ile başarı döner).
- Superseded kontrolü: göndermeden önce daha yeni karar varsa satır gönderilmeden silinir.

#### 4. Event (CloudEvents) ve tüketici

- `id` (UUIDv7), `source` (servis), `type` (`<servis>.<aggregate>.<olay>`), `subject` (aggregate id), `time`, `dataschema`, `traceparent`.
- Tüketici: inbox `ON CONFLICT (event_id) DO NOTHING`; bilinmeyen `type` **yok sayılır** (komutlarda DLQ); read-model `revision` ile UPSERT (eski olay yeni satırı ezmez).
- Şema evrimi: alan ekleme uyumlu; silme/yeniden adlandırma/tip değişimi → yeni `type`, bir süre çift yayın. Tüketici önce deploy.
- Read-model kaynak değildir; dışa açılmaz; eskime eşiği ve "satır yok" davranışı yazılıdır; rebuild yolu belgelidir.

#### 5. Local saga (tek adım)

- Koordinatör tabloları koordinatörün şemasında: `saga` (`UNIQUE(account_id, scope, operation_key)`), `saga_steps` (`next_action CONFIRM|COMPENSATE`, `lock_token`, `locked_until`).
- Akış: `begin()` ayrı TX'te (`ON CONFLICT DO NOTHING`; yeni kayıt → step `COMPENSATE/PENDING`, `next_attempt_at = now + deadline 15 sn`; varsa replay) → katılımcıya `consume` (TX dışı, aynı `operationKey`, circuit breaker altında) → domain yazımı + `success()` **aynı local TX'te**, ayrı bean (`success` compare-and-set; recovery iptal ettiyse rollback) → recovery worker: `claim` (SKIP LOCKED + `lock_token` + lease 60 sn) → `prepare` (`FOR UPDATE`) → uzak `confirm`/`compensate` → `complete` (token eşleşmesi) → hata/belirsiz sonuçta `GET` ile durum sorgusu; çelişki → `MANUAL_REVIEW`.
- `monitor` (60 sn): 15 dk'dan eski çözülmemiş → ERROR + metrik. `cleanup`: terminal kayıtlar 30 gün sonra; `MANUAL_REVIEW` silinmez.
- Katılımcı: `UNIQUE(caller_service, account_id, operation_key)`; `caller_service` JWT `act`/`iss`'ten; iki katmanlı yetki (allowlist + aktör → izinli işlem tipi); advisory lock + `FOR UPDATE`; tablo: consume/confirm/compensate sonuçları (`APPLIED/REJECTED/CONFIRMED/CANCELLED tombstone/COMPENSATED/MANUAL_REVIEW`); çift iade `original_id` ile engellenir.
- Merkezi coordinator servisi kurulmaz; çok adımlı ihtiyaç doğarsa `LocalSagaStore` genelleştirilir (step adı parametre) — ADR ile.
- Zaman kaynağı DB `now()`.

#### 6. Zorunlu doğrulama matrisi

Kanıt seviyeleri: (1) unit + MVC, (2) gerçek PostgreSQL (Testcontainers), (3) owner→participant runtime, (4) release. Sonuç `PASS/FAIL/BLOCKED`.

Senaryolar: normal başarı ve replay · aynı key farklı body · eşzamanlı aynı key · farklı key aynı kaynak · aynı UUID farklı hesap/aktör · intent sonrası çökme · katılımcı commit + yanıt kaybı · consume commit + domain rollback · domain commit + confirm öncesi çökme · geç consume vs tombstone · confirm/compensate timeout · eşzamanlı confirm ve compensate · iki worker + expired lease · request success vs recovery cancel yarışı · tekrarlanan compensate · eksik/bozuk key · geçersiz JWT / yanlış aktör · cleanup ve monitor · migration ve restart · outbox satırı domain TX ile rollback · tüketici duplicate olay · sıra bozuk olay · bilinmeyen tip.

#### 7. Bu Belgede Özellikle Taşınmayanlar

Somut servis adları ve operasyon tipleri (`repo-context.md`'de).

#### 8. Net Kanıt Bulunamayan Alanlar

- (ajan ekler)

---

### `docs/adr/0000-template.md`

### ADR-0000: <Karar başlığı>

- **Durum:** Önerildi | Kabul edildi | Reddedildi | Yerini aldı: ADR-NNNN
- **Tarih:** YYYY-MM-DD
- **Karar verenler:** …
- **İlgili eşik (referans Bölüm 24):** … (bu kararın hangi metrikte yeniden değerlendirileceği)

#### Bağlam

Hangi sorun/ihtiyaç; hangi kısıtlar (ekip, ölçek, bütçe, düzenleme). Kanıt: metrikler, olaylar, ölçümler.

#### Seçenekler

| Seçenek | Artı | Eksi | Maliyet |
|---|---|---|---|
| A | | | |
| B | | | |

#### Karar

Seçilen seçenek ve **neden**. Reddedilen seçeneklerin neden reddedildiği tek cümleyle.

#### Sonuçlar

- Olumlu:
- Olumsuz / kabul edilen risk:
- Etkilenen dosyalar / servisler / config yüzeyleri:
- Geri alma yolu:

#### Yeniden değerlendirme koşulu

Bu karar şu metrik/olay gerçekleşince yeniden açılır: …

---

### `.agents/skills/proj-api-contract-review/SKILL.md`

---
name: proj-api-contract-review
description: Use this skill when reviewing API contracts — DTO placement, api/core dependency direction, backward compatibility for mobile/panel/internal callers, HTTP parameter binding, OpenAPI diff and versioning.
---

Contract değişikliğini mimari referans Bölüm 3.3, 5.3–5.4, 6 ve 20'ye göre incele. OpenAPI diff çıktısı varsa önce onu oku. Sorun yoksa: **"Bu kapsamda contract bulgusu yok."**

Kontrol et:

#### Yerleşim ve bağımlılık yönü
- Servisler arası DTO/enum/event payload'ı hedefin `<domain>-api` modülünde (`com.<org>.<domain>.api.*`); core'da kopyası yok.
- `*-api` hiçbir `*-core`'a bağımlı değil; `*-core` başka `*-core`'a bağımlı değil (enforcer).
- Entity dışarı açılmıyor; `toResponse` ile DTO.
- Servisler arası okunan DTO'da `@NoArgsConstructor` (Jackson).
- Event payload'ı `api/event` altında; CloudEvents attribute'ları platform'dan.

#### Geriye uyumluluk
- Alan silme/yeniden adlandırma/tip değişimi/zorunlu alan ekleme = **breaking**. Etkilenen çağıranlar: mobil (mağazadaki eski sürüm aylarca yaşar), panel, internal client'lar, event tüketicileri.
- Breaking ise: yeni versiyon (`/v2` veya yeni `type`), eski versiyon sunset tarihiyle yaşıyor, ADR var.
- Enum'a yeni değer: tüketiciler bilinmeyen değeri tolere ediyor (`from()` fallback); **tüketici önce deploy**.
- Yanıt zarfı tutarlı (hata her zaman zarflı; başarı için tek karar); 200 ile `success=false` yok.
- Hata kodları servisin bloğunda ve global tekil; istemci kod→mesaj tablosu güncellendi (contract testi).
- Sayfalama: boyut sınırı, sıralama allowlist, keyset alanları tutarlı.

#### HTTP parameter binding
- `@PathVariable("ad")`, `@RequestParam("ad")`, `@RequestHeader("Ad")` **açık isimli**; parameter-name inference'a güvenilmiyor.
- `required`/`defaultValue` bilinçli; opsiyonel parametre `Optional` veya default.
- Binding gerçek MVC üzerinden test edilmiş (`MockMvc standaloneSetup`; geçersiz tip 400 ve servise ulaşmıyor).
- Idempotent uçlar `X-Idempotency-Key` alıyor; UUID tipiyle bağlanmış.
- Kimlik `@CurrentAccount`; path/body'de hesap kimliği yok.

#### Path ve isim
- Kaynak çoğul kebab-case; fiiller yalnız durum geçişi alt kaynaklarında (`/{id}/cancel`); güncelleme `PUT/PATCH`; versiyon prefix'i var.
- Internal uç `/internal/<kaynak-çoğul>/...`; public/internal karışmıyor.

#### OpenAPI ve istemci
- OpenAPI diff: breaking değişiklik işaretli mi; istemci client generate ediliyor mu; `docs/<client>-<feature>-integration-vN.md` gerekli mi (→ `proj-client-integration-doc`).
- Örnek değerler ve açıklamalar üretilen OpenAPI'de anlamlı (springdoc anotasyonları).

Çıktı:
1. **Contract risk seviyesi:** `HIGH` / `MEDIUM` / `LOW` / `OK`.
2. **Breaking-change riski:** evet/hayır + gerekçe; etkilenen çağıranlar (mobil / panel / internal / event tüketicileri).
3. **Yerleşim/bağımlılık bulguları:** `dosya:satır · kanıt · düzeltme`.
4. **Binding bulguları:** `dosya:satır · kanıt · düzeltme · eksik test`.
5. **Daha güvenli yapı önerisi** (breaking ise versiyonlama/çift yayın planı).
6. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`.

---

### `.agents/skills/proj-architecture-boundary-review/SKILL.md`

---
name: proj-architecture-boundary-review
description: Use this skill before writing code that touches another module's data, adds a cross-service dependency, a new HTTP client, a read-model, or changes api/core dependency direction — and when reviewing such changes.
---

Modül sınırı ihlallerini ve bağımlılık yönünü `AGENTS.md` Bölüm 4–5, `docs/ai/operation-consistency.md` ve mimari referans Bölüm 1.1/3.3/4.6/10.1'e göre incele. Bu skill **kod yazılmadan önce** de çalıştırılır: ihtiyaç bir sınırı aşıyorsa alternatif önerilir ve kullanıcıya sorulur. Sorun yoksa: **"Bu kapsamda sınır bulgusu yok."**

Kontrol et:

#### Veri sahipliği
- Başka modülün tablosu, şeması, repository'si, entity'si, migration'ı okunuyor/yazılıyor mu? (native SQL'de başka şema adı, JPQL'de başka modül entity'si, `@Table(schema=…)` uyuşmazlığı). Yönetim servisi de dahil. **Varsa `BLOCK`.**
- Cross-schema FK/join var mı?
- DB rolü hatası "GRANT" ile mi çözülmüş? (**`BLOCK`**)
- Başka servisin verisi gerekiyorsa hangi yol seçilmiş: (a) hedefin public/internal API'si (yalnız yazma/rezervasyon türü, sıcak yol dışı okuma), (b) event ile read-model, (c) JWT claim. Sıcak yolda (a) ile okuma → `REQUEST CHANGES`, (b)/(c) öner.

#### Bağımlılık yönü
- `*-api` → `*-core` bağımlılığı yok; `*-core` → başka `*-core` yok; `platform-*` → servis modülü yok.
- Yeni Maven bağımlılığı bu kuralları ihlal etmiyor (enforcer `bannedDependencies` geçiyor).
- Split package yok (api kökü `com.<org>.<domain>.api`).
- Hedef servis başına tek client; client çağıranın `client/` paketinde; DTO hedefin api'sinden.

#### Read-model kuralları
- Read-model tüketicinin kendi şemasında; `revision` ile UPSERT; eskime eşiği ve "satır yok" davranışı yazılı; dışa açılmıyor; rebuild yolu var.
- Read-model'den **yazma** kararı (hak/stok) veriliyor mu? Bu yasak; saga/rezervasyon gerekir.

#### Paylaşılan altyapı sözleşmeleri
- Redis key formatı başka serviste kopyalanmış mı? `RedisKeys` (platform-core) veya API/event'e çevir.
- RabbitMQ queue/exchange adı başka servisin sahasına giriyor mu (kendi queue'sunu tanımlıyor mu)?
- Parametre okuması yalnız kendi `SystemParameterProvider`'ı üzerinden mi?

#### Mimari şekil
- Değişiklik "dağıtık monolit" sinyali üretiyor mu (yeni senkron zincir, ortak kütüphaneye servis-özel kod, birlikte deploy zorunluluğu)? ADR gerekiyor mu?
- Yeni servis/modül ekleniyorsa Bölüm 1.1 kararıyla tutarlı mı (modül olarak mı, servis olarak mı)?

Çıktı:
1. **Sınır risk seviyesi:** `BLOCKER` / `HIGH` / `MEDIUM` / `LOW` / `OK`.
2. **İhlaller:** `dosya:satır/fonksiyon · hangi kural · kanıt`.
3. **Alternatif:** her ihlal için sınır içinde kalan tasarım (API / event + read-model / claim / saga) ve maliyeti.
4. **Kod yazmadan önce sorulacak sorular** (belirsizlik varsa).
5. **Nihai karar:** `APPROVE` / `REQUEST CHANGES` / `BLOCK` — sınır ihlali `APPROVE WITH COMMENTS` alamaz.

---

### `.agents/skills/proj-client-integration-doc/SKILL.md`

---
name: proj-client-integration-doc
description: Use this skill after backend changes to decide whether a client (mobile/web) is affected and, if so, to produce the versioned integration document from code and OpenAPI — never from memory.
---

İstemci etkisini değerlendir ve gerekiyorsa `docs/<client>-<feature>-integration-vN.md` üret. Kapsam: push edilmemiş her değişiklik. Gerçekler **koddan ve üretilen OpenAPI'den** toplanır; hatırlanan/varsayılan bilgi yazılmaz. Endpoint ve alan listesi OpenAPI'den gelir; doküman **davranış, ekran akışı, hata kodu → ekran** eşlemesine odaklanır.

#### 1. Etki gate'i (biri evetse doküman gerekir)
- Yeni/değişen public endpoint, path, method, versiyon?
- Request/response alanı, enum değeri, validation kuralı, sayfalama biçimi değişti mi?
- Yeni hata kodu / değişen status?
- Davranış değişti mi (rate limit, idempotency, sıralama, gizlilik)?
- Realtime (WebSocket topic/zarf), push payload'ı, deep link?
- Auth akışı (token TTL, `sv`, refresh, 2FA, attestation)?
- Kullanıcıya görünür veri (yeni alan gösterimi, gizleme)?

Hiçbiri evet değilse çıktı: **"İstemci etkisi yok."** + gerekçe (hangi dosyalar incelendi).

#### 2. Gerçekleri topla
- OpenAPI diff (CI artifact'ı) — breaking işaretleri.
- Controller imzaları, DTO'lar (`*-api`), `ErrorCode` enum'ları, validation anotasyonları.
- Gateway route/permitAll, rate-limit scope'ları (429 davranışı), idempotency header'ları.
- Yanıt zarfı biçimi (zarflı/ham) — uç bazında.
- Realtime/push: topic adları, zarf alanları, `seq`/history pull.

#### 3. Dokümanı üret (`template.md`)
- Sürüm: mevcut `vN` üzerine yazılmaz; `v(N+1)` açılır; "Önceki sürüme göre farklar" bölümü doldurulur.
- Her senaryo: HTTP status + body örneği (zarflı mı ham mı açık), alan eşleme, istemci dilinde örnek (`dio`/`fetch`), **bilinmeyen enum değeri için fallback**.
- Hata kodları ve ekran davranışı tablosu (kod → mesaj → ekran aksiyonu → retry?).
- Güvenlik ve log kuralları (istemci ne loglamaz, token nerede tutulur).
- Manuel test akışı: uygulama + API koleksiyonu (OpenAPI'den üretilmiş).
- "Kritik" etiketi yalnız veri kaybı / güvenlik / ücret / bozuk akış için.
- Kaynak damgası: "<tarih> tarihli <branch> <sha> koduna dayanır."

#### 4. Öz-kontrol
- [ ] Her endpoint OpenAPI'de var ve path/method aynı.
- [ ] Her hata kodu backend enum'unda var; istemci kod→mesaj tablosu güncellendi.
- [ ] Zarf biçimi uç başına doğru.
- [ ] Örneklerde gerçek kişisel veri/secret yok.
- [ ] Generated client paketi versiyonu ve değişiklik notu.
- [ ] Deploy sırası / eski istemci uyumluluğu (sunset) yazılı.

Çıktı:
1. **Gate kararı:** etkiliyor / etkilemiyor + kanıt.
2. **Doküman yolu:** `docs/<client>-<feature>-integration-vN.md` (yeni sürüm) ve özet (Türkçe, 5–8 madde).
3. **Breaking değişiklikler** ve istemci için zorunlu aksiyonlar.
4. **Açık sorular** (ürün/istemci ekibine).

---

### `.agents/skills/proj-client-integration-doc/template.md`

### <Client> <özellik> entegrasyonu [vN]

<2–4 cümle giriş: bu özellik ne yapar, istemci için ne değişir.> Bu doküman **<GG Ay YYYY>** tarihli `<branch>` branch'indeki `<sha>` backend koduna ve CI'da üretilen `<proje>-api.yaml` (`<sürüm>`) OpenAPI çıktısına dayanır. Generated client paketi: `<paket>@<sürüm>`.

#### Değişikliklerin özeti
- …
- …

#### Kurallar
> **Kritik:** yalnız veri kaybı / güvenlik / ücret / bozuk akış için kullanılır.
- **Kritik:** …
- …

#### Değişiklikler ve <client>'a etkisi
##### 1. <değişiklik>
Ne değişti · neden · istemci ne yapmalı · geriye uyumluluk (eski sürüm ne görür).

#### Kullanılan endpoint'ler
| Method | Path | Auth | Idempotency | Rate limit (429) | Zarf |
|---|---|---|---|---|---|
| POST | `/v1/...` | user JWT | `X-Idempotency-Key` | `<scope>` | ham / zarflı |

#### API sözleşmesi
##### <işlem>
###### Senaryo: <başarı>
`HTTP 201` — body (ham/zarflı):
```json
{ ... }
```
###### Senaryo: <hata>
`HTTP 409` — zarflı:
```json
{ "ok": false, "data": null, "error": { "code": 11002, "message": "...", "service": "order", "traceId": "...", "details": ["retryAfterSeconds=120"] } }
```
###### Alan eşleme
| Alan | Tip | Zorunlu | Not (bilinmeyen enum → fallback) |
|---|---|---|---|
###### <Dil> örneği
```dart
// dio / generated client kullanımı; ApiException; unknown enum fallback
```

#### Realtime / push (varsa)
Topic/zarf alanları, `seq` ve history pull, push payload'ı (içerik yok).

#### Hata kodları ve ekran davranışı
| code | Anlam | Ekran aksiyonu | Retry? |
|---|---|---|---|

#### Güvenlik ve log kuralları
Token yalnız bellekte; loglanmayacaklar; attestation gereken uçlar.

#### Önceki sürüme göre farklar (v(N-1) → vN)
- …

#### Manuel test akışı
##### Uygulama üzerinden
1. …
##### API koleksiyonu ile
1. …

#### Deploy ve uyumluluk
Backend deploy sırası; eski istemci sürümü davranışı; sunset tarihi (varsa).

#### Açık sorular
- …

---

### `.agents/skills/proj-db-migration-review/SKILL.md`

---
name: proj-db-migration-review
description: Use this skill when reviewing PostgreSQL schema changes, Flyway migrations, indexes, constraints, partitioning, retention, seed data, module ownership or production migration risk.
---

`db/migration/` değişikliklerini mimari referans Bölüm 10 ve `docs/ai/security-rules.md` Bölüm 7'ye göre incele. Her bulgu için **çalıştırılabilir doğrulama SQL'i** ver. Sorun yoksa: **"Bu kapsamda migration bulgusu yok."**

Önce çalıştır: `node scripts/flyway-immutability.js check --base origin/<hedef>` — çıkış 0 değilse `BLOCKER`.

Kontrol et:

#### Flyway güvenliği
- Base'teki `V*.sql` değişmemiş/silinmemiş/yeniden adlandırılmamış. Düzeltme yeni `V<sonraki>` ile.
- Sürüm sıralı; `out-of-order`, `V9999`, "temp" adlı migration yok.
- `R__*` yalnız idempotent referans verisi/view; iş verisi veya parola içermiyor.
- Seed/test verisi prod location'ında değil (`db/seed-<env>`, yalnız local/test profili).
- Migration servisin **kendi rolüyle** koşuyor; `GRANT` başka şemaya erişim açmıyor (varsa `BLOCKER`).

#### Modül sahipliği
- Dosyada yalnız kendi şeması; tam nitelikli adlar; başka şema adı geçmiyor (test de bunu kontrol eder).
- Cross-schema FK yok; başka servisin kimliği düz UUID.
- Read-model tablosu tüketicinin kendi şemasında ve `revision` kolonu var.

#### Mevcut veri ve expand/contract
- Yeni `NOT NULL`, unique, FK öncesi mevcut veri kontrolü SQL'i verilmiş (`SELECT count(*) … WHERE … IS NULL`, duplicate sorgusu).
- Büyük tabloda: `NOT VALID` + ayrı `VALIDATE`; `CREATE INDEX CONCURRENTLY` (transaction dışı migration, Flyway `executeInTransaction=false`); DDL / backfill / validate ayrı dosyalar.
- Yıkıcı değişiklik expand → backfill → contract; guard `DO $$ … RAISE EXCEPTION`; kolon silme uygulama deploy'undan **sonraki** release'te.
- Tablo rewrite'ı tetikleyen değişiklik (tip değişimi, default'lu NOT NULL eski PG'de) işaretlenmiş; lock süresi tahmini var.

#### Tip ve constraint
- Zaman `TIMESTAMPTZ`; enum `TEXT + CHECK`; id UUID (v7, DB default `uuidv7()` PG18); isimli `uq_/ck_/fk_/idx_`.
- Eşzamanlılık kuralı DB'de: unique / partial unique (`WHERE status = 'ACTIVE'`); repository sorgusu aynı predicate'i kullanıyor.
- Soft delete'te unique kural partial index ile.
- `ON DELETE CASCADE` bilinçli; audit, ödeme, yasal kayıt, moderasyon kanıtı cascade ile silinmiyor.
- Append-only tablolar trigger ile korunuyor; audit `REVOKE UPDATE, DELETE`.
- Blob `BYTEA` (+ `STORAGE EXTERNAL` şifreli veri için); base64 `TEXT` yok.

#### Index gerekçesi
- Her index gerçek bir sorguya dayanıyor (yorumla yazılmış); duplicate/prefix-redundant index yok.
- Poll edilen tablolarda claim index'i `(status, next_retry_at, locked_until, created_at)`.
- Keyset export index'i deterministik tie-breaker ile `(owner, created_at DESC, id DESC)`.
- Append-only büyük tabloda `created_at` BRIN değerlendirilmiş.

#### Büyüme ve retention
- Sürekli büyüyen tablo (outbox, log, audit, olay, mesaj) için retention politikası ve gerekirse partition (pg_partman) tanımlı; `DELETE` ile retention yerine `DROP PARTITION`.
- Tahmini satır/boyut büyümesi ve eşik (referans Bölüm 24) yorumda.

#### Privacy
- Kişisel veri kolonu: HMAC+pepper/şifreleme kararı; envanter güncel; silme saga'sı kapsamına alınmış.
- Kişisel veri veya gerçek kullanıcı verisi migration'da yok.

#### Dinamik parametre migration'ı
- Yeni `system_parameter` satırı: kolon sırası registry/testin beklediği gibi; `data_type`, `criticality`, `usage_status` doğru; enum sabiti api modülüne eklenmiş.

#### Entity uyumu
- `ddl-auto: validate` ile uyumlu (kolon adı/tip/nullable); JPQL doğrulama testi geçiyor.

Çıktı:
1. **Risk:** `BLOCKER` / `HIGH` / `MEDIUM` / `LOW` / `OK`.
2. **İncelenen kapsam:** dosya listesi + immutability komut çıktısı.
3. **Bulgular:** `severity · dosya:satır · kanıt · etki (lock, veri kaybı, sahiplik, privacy) · düzeltme`.
4. **Doğrulama SQL'leri:** production'da migration öncesi çalıştırılacak sorgular.
5. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`.

---

### `.agents/skills/proj-environment-impact-review/SKILL.md`

---
name: proj-environment-impact-review
description: Use this skill when a change adds or modifies configuration, environment variables, secrets, ports, audiences, issuers, rate-limit scopes, internal endpoints, feature flags, service URLs, tracing/observability settings, Dockerfile or deploy definitions.
---

Config/env etkisini `AGENTS.md` Bölüm 9 ve mimari referans Bölüm 15, 18, 8'e göre incele. Amaç: bir yüzeyde eklenip diğerinde unutulan key (drift) ve deploy'da patlayan ayar. Sorun yoksa: **"Bu kapsamda environment bulgusu yok."**

Kontrol et:

#### Birlikte güncellenmesi gereken yüzeyler
Yeni/değişen her key için tabloyu doldur:

| Key | `config/<svc>.yml` | `application-local.yml` | deploy `env_file` / `secrets/<env>.enc.yaml` | compose `secrets:` | Dockerfile (yalnız build/runtime) | Gateway route | Prometheus/alert | Not |
|---|---|---|---|---|---|---|---|---|

- Eksik hücre = bulgu. `scripts/config-drift-check` / `ConfigDriftTest` bu key'i kapsıyor mu?
- Key adı üç ortamda **aynı**; profil dosyaları yalnız ortam farkı taşıyor, iş config'i base'de.
- Dockerfile'a config için `ENV` **eklenmemiş**.
- Secret: `${ENV:literal}` fallback yok; `/run/secrets` yolu; SOPS dosyasına eklenmiş; `.dockerignore` kapsıyor; PR'da değeri yok. Config Server'a konmamış.
- Admin'in değiştirebileceği iş kuralı config'e değil parametre kataloğuna gitmiş (Bölüm 14).

#### Servisler arası
- Yeni internal uç: hedefin `service-jwt.internal-access` kuralı dar ve catch-all'dan önce; local + deploy'da birlikte.
- Yeni client: `spring.http.serviceclient.<grup>.base-url` + timeout + circuit breaker/bulkhead config'i her ortamda; `services.<svc>.base-url` compose servis adıyla uyumlu.
- Yeni audience/issuer: JWKS'e public key + `kid`; doğrulayanların `iss` listesi; gateway route `metadata.audience`.
- Yeni rate-limit scope: `rate-limit.rules.<scope>` local + deploy; fail politikası tablosuna satır.
- Yeni event/queue: RabbitMQ definitions (queue/DLQ/binding) local + deploy; tüketici önce deploy sırası deploy notunda.

#### Çalışma zamanı ve replica
- Değişiklik instance-local dosya/dizin varsayıyor mu? Sticky session gerektiriyor mu? (multi-instance kuralı)
- Yeni `@Scheduled`/cron dağıtık kilitli mi?
- Bellek/CPU etkisi: compose `mem_limit`, JVM `MaxRAMPercentage`, Hikari havuzu, PgBouncer pool'u güncellendi mi; kapasite tablosu (README) uyumlu mu?
- Healthcheck/readiness yeni bağımlılığı (DB, broker, Redis) kapsıyor mu?

#### Gözlemlenebilirlik
- Yeni servis/uç: Prometheus scrape hedefi, Alloy log kaynağı, SLO/pano, alarm kuralı (özellikle yeni outbox/queue için `outbox_oldest_pending_age_seconds`, DLQ derinliği).
- Tracing: sampling, OTLP endpoint, yeni async sınırda `traceparent` taşınıyor.
- Yeni log alanı structured JSON'a uyuyor; label kardinalitesi artmıyor.

#### Deploy
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

---

### `.agents/skills/proj-event-design-review/SKILL.md`

---
name: proj-event-design-review
description: Use this skill when adding or changing a domain event, a command, an outbox handler, a consumer, a read-model projection, an event schema, or RabbitMQ topology (exchange/queue/binding/DLQ).
---

Olay tasarımını `docs/ai/operation-consistency.md` Bölüm 3–4 ve mimari referans Bölüm 12'ye göre incele. Temel sorular: **Bu bir komut mu, olay mı? Sahibi kim? Yarın üçüncü bir tüketici geldiğinde üretici değişmeden çalışır mı?** Sorun yoksa: **"Bu kapsamda event tasarım bulgusu yok."**

Kontrol et:

#### Komut / olay ayrımı
- "Başka servisin verisini değiştir" niyetli mesaj **komut değil olay** olmalı; üretici tüketiciyi bilmiyor. Outbox `kind` doğru (`EVENT`/`COMMAND`/`HTTP`).
- Komut yalnız dış sağlayıcı iş emri (SMS, push, mail, webhook).
- "Outbox üzerinden RPC" (üretici hedefin ucunu/adresini biliyor) → `REQUEST CHANGES`.

#### İsim ve envelope
- `type` = `<servis>.<aggregate>.<olay>` (geçmiş zaman: `created`, `cancelled`, `changed`); routing key aynı.
- CloudEvents attribute'ları: `id` (UUIDv7), `source`, `specversion`, `type`, `subject` (aggregate id), `time`, `dataschema`, `traceparent`. AMQP 0-9-1 header eşlemesi `platform-messaging`'den.
- Payload sınıfı `<domain>-api/event`; `@NoArgsConstructor`; kopya yok. Payload **gerçeği** taşır (id'ler, durum, revision), tüketiciye "ne yapması gerektiğini" değil.
- Payload'da PII/secret/şifreli içerik yok (mesaj olayı yalnız metadata).
- `revision`/sıra numarası var (tüketici eski olayı yeni satıra yazmasın).

#### Şema evrimi
- Değişiklik geriye uyumlu mu (yalnız opsiyonel alan ekleme)? Kırıcıysa yeni `type`/versiyon + çift yayın planı + sunset.
- Tüketici bilinmeyen alanı yok sayıyor; bilinmeyen `type` olaylarda **ack + log** (komutlarda DLQ).
- Tüketici önce deploy; PR açıklamasında sıra yazılı.

#### Topoloji (RabbitMQ 4.x)
- Olay: `domain.events` topic exchange; tüketici başına queue (`<tüketici>.<amaç>.queue`, quorum) + DLQ; binding pattern dar (`order.order.*`, `#` yok).
- Komut: `<servis>.commands` direct → `<hedef>.<komut>.queue` + DLQ.
- Queue quorum; `delivery-limit`; `dead-letter-strategy: at-least-once`; gecikmeli retry native (`x-delayed-retry-*`), delayed-exchange plugin **yok**.
- Sıra gereksinimi varsa single-active-consumer veya aggregate bazlı tek worker.
- Replay gerekiyorsa stream kopyası (`domain.events.stream`) ve retention.

#### Üretici
- Yayın yalnız outbox'tan (domain TX'i içinde satır); `convertAndSend` doğrudan yok.
- Publisher confirm + mandatory; NACK/unroutable → outbox retry.
- Aynı olay birden çok tabloya/outbox'a yazılmıyor (tek satır, çok tüketici).

#### Tüketici ve read-model
- `defaultRequeueRejected=false`; prefetch/concurrency açık; kalıcı hata → DLQ; geçici → stateful retry + backoff.
- Inbox dedup `ON CONFLICT (event_id) DO NOTHING`; handler idempotent.
- Read-model: tüketicinin şemasında; `UPSERT … WHERE excluded.revision > current.revision`; eskime eşiği + "satır yok" davranışı yazılı; dışa açılmıyor; rebuild yolu (stream replay / export ucu) belgeli; `readmodel_lag_seconds` metriği.
- Tüketici kendi transaction'ında yazıyor; başka servise senkron çağrı yapmıyor.

#### Gözlem ve test
- Metrik/alarm: DLQ derinliği, `outbox_oldest_pending_age_seconds`, tüketici lag.
- Testler: outbox satırı TX ile rollback; tüketici duplicate; sıra bozuk olay; bilinmeyen tip; şema uyumluluğu (eski payload yeni tüketicide parse oluyor).
- Analytics sink bu olayı alıyor mu (Bölüm 14.4)?

Çıktı:
1. **Sınıflandırma:** komut / olay / HTTP — doğru mu.
2. **Olay tablosu:** `type · üretici · tüketiciler · routing key · queue · DLQ · replay (evet/hayır) · revision alanı`.
3. **Bulgular:** `severity · dosya:satır/config · kanıt · düzeltme`.
4. **Şema evrimi notu:** uyumlu / kırıcı + plan; deploy sırası.
5. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`.

---

### `.agents/skills/proj-operation-consistency-review/SKILL.md`

---
name: proj-operation-consistency-review
description: Use this skill for any cross-service write, outbox, inbox, idempotency or saga work — to assess whether a saga is needed, to implement it correctly, and to verify it against the mandatory scenario matrix.
---

Servisler arası tutarlılığı `docs/ai/operation-consistency.md` (tek kaynak) ve mimari referans Bölüm 11'e göre üç fazda ele al. Kapsam: push edilmemiş her değişiklik. Mevcut altyapı sınırı: `LocalSagaStore` **tek adımlı, tek katılımcı**; merkezi coordinator yok. Çok adımlı ihtiyaç → `extension required` + ADR.

#### Faz 1 — Assessment (`references/assessment.md`)
- İhtiyacı sınıflandır: yalnız okuma / tek local TX / duplicate koruması / commit sonrası tepki (event) / dış iş emri (komut) / local commit + uzak geri alınabilir mutation (saga) / geri alınamaz-global-insan onayı (saga uygun değil).
- "En basit yeterli mekanizma" seçilmiş mi? Saga, event yeterliyken kullanılıyorsa fazla; event, saga gerekirken kullanılıyorsa eksik.
- Sıcak yol etkisi: saga consume çağrısı tek uzak çağrı mı (Bölüm 1.2)?
- Karar: `saga unnecessary` / `existing saga suitable` / `extension required` / `decision blocked` (+ neden).

#### Faz 2 — Implementation (`references/implementation.md`, 12 adım)
- Idempotency key: `X-Idempotency-Key` UUID, `(account_id, scope, operation_key)` tekilliği, replay/IN_PROGRESS/CANCELLED davranışı.
- `begin()` ayrı TX; consume TX dışında ve circuit breaker altında; domain + `success()` aynı TX'te ayrı bean; compare-and-set.
- Katılımcı: `UNIQUE(caller_service, account_id, operation_key)`, tombstone, advisory lock + `FOR UPDATE`, aktör→işlem tipi haritası, iade kuralları, `original_id` ile çift iade engeli.
- Recovery worker: claim (SKIP LOCKED + `lock_token` + lease), prepare (`FOR UPDATE`), confirm/compensate, complete (token), belirsizlikte GET, çelişkide `MANUAL_REVIEW`; monitor + cleanup; `MANUAL_REVIEW` silinmez.
- Outbox kullanımı: `outbox_event` (generic), `kind` doğru, yazıcı MANDATORY, handler idempotent, DEAD politikası iş türüne göre, hassas alan iletim sonrası NULL.
- Inbox/read-model: `ON CONFLICT (event_id) DO NOTHING`; `revision` ile UPSERT; bilinmeyen `type` yok sayılır.
- Loglar: `sagaId`/`operationKey` ile, hesap kimliği yok; metrikler: `saga_unresolved_total`, `outbox_oldest_pending_age_seconds`.
- Config: `operation-consistency.*` key'leri local + deploy.

#### Faz 3 — Verification (`references/verification.md`)
- Yapısal kontroller (statik) + senaryo matrisi (operation-consistency.md Bölüm 6) + kanıt seviyesi (1 unit/MVC, 2 gerçek PG, 3 owner→participant runtime, 4 release).
- Her senaryo için: test adı / dosya / kanıt seviyesi / sonuç. Testi olmayan senaryo `FAIL` değil `BLOCKED` (kanıt yok) sayılır ve listelenir.

Çıktı:
1. **Uygunluk kararı:** `saga unnecessary` / `existing saga suitable` / `extension required` / `decision blocked` + gerekçe.
2. **Implementasyon bulguları:** `adım · dosya:satır · kanıt · düzeltme`.
3. **Doğrulama tablosu:** senaryo → test → kanıt seviyesi → `PASS/FAIL/BLOCKED`.
4. **Doğrulama durumu:** `PASS` / `FAIL` / `BLOCKED`.
5. **Nihai karar:** `APPROVE` / `REQUEST CHANGES` / `BLOCK`.

---

### `.agents/skills/proj-operation-consistency-review/references/assessment.md`

### Assessment — Mekanizma Seçimi

#### 1. Soru ağacı

1. **Bu işlem başka servisin verisini değiştiriyor mu?**
   - Hayır → local TX + constraint. Bitti.
   - Evet → 2.
2. **Diğer servis yalnız "haberdar" mı olacak (kendi kararını kendi verecek)?**
   - Evet → **domain event** (outbox `kind=EVENT`) + idempotent consumer. Bitti.
   - Hayır (bizim işlem onun sonucuna bağlı) → 3.
3. **Uzak etki geri alınabilir mi (compensate edilebilir)?**
   - Evet ve tek katılımcı, tek adım → **local saga**.
   - Evet ama çok adım / çok katılımcı → `extension required` (ADR).
   - Hayır (para transferi, dış sistem, geri alınamaz) → saga uygun değil → süreç tablosu + durum makinesi + insan/dış onay adımı; `decision blocked` ile mimari karar iste.
4. **İşlem sıcak yolda mı?**
   - Evet → uzak çağrı sayısı 1'i geçemez; consume dışındaki kontroller read-model/claim ile.
5. **Dış sağlayıcıya iş emri mi (SMS, push, webhook)?**
   - Evet → **komut** (outbox `kind=COMMAND` → queue) veya `kind=HTTP`.

#### 2. Karar tablosu

| Durum | Mekanizma | Örnek |
|---|---|---|
| Kendi tablomu yazıyorum | TX + unique | Profil güncelleme |
| Aynı isteğin tekrarını engellemek | Idempotency key | Sipariş oluşturma retry'ı |
| Başkası haberdar olsun | Event + inbox | `order.order.created` → notification, analytics |
| Başkasının verisini karar için okumam lazım | Read-model / JWT claim | Hesap durumu, engel listesi |
| Başkasından geri alınabilir bir hak/stok tüketmem lazım | Local saga | Kampanya hakkı, envanter rezervi |
| Geri alınamaz dış etki | Süreç + insan/dış onay, ADR | Ödeme çekimi, resmi bildirim |

#### 3. Çıktı formatı

- Karar: `saga unnecessary` / `existing saga suitable` / `extension required` / `decision blocked`
- Gerekçe (2–4 cümle), seçilen mekanizma, sıcak yol etkisi (uzak çağrı sayısı), açık sorular.

---

### `.agents/skills/proj-operation-consistency-review/references/implementation.md`

### Implementation — 12 Adım (Local Saga + Outbox)

Her adım için "nerede" (dosya/paket) ve "kanıt" (satır) yazılır.

1. **Idempotency key sözleşmesi** — Controller `@RequestHeader("X-Idempotency-Key") UUID`; scope sabiti (`MATCH_SWIPE` gibi UPPER_SNAKE); tekillik `(account_id, scope, operation_key)`; replay / `OPERATION_IN_PROGRESS` / `OPERATION_CANCELLED` yanıtları tanımlı.
2. **Koordinatör tabloları** — `saga`, `saga_steps` koordinatörün şemasında; unique, CHECK, claim/expired-lease/retention index'leri; migration `proj-db-migration-review`'dan geçmiş.
3. **`begin()`** — ayrı TX (`REQUIRES_NEW` veya `TransactionTemplate`), `INSERT … ON CONFLICT DO NOTHING`; yeni kayıtta step `COMPENSATE/PENDING`, `next_attempt_at = now + deadline`; mevcutsa önceki sonucu döndür.
4. **Guard'lar** — rate limit, read-model/claim kontrolleri **TX dışında**, saga `begin()`'den sonra ve consume'dan önce; fail-closed.
5. **Consume** — katılımcı client'ı TX dışında, aynı `operationKey`, timeout + circuit breaker; `REJECTED` → domain yazılmaz, saga `CANCEL_REQUESTED`.
6. **Domain yazımı + `success()`** — `*TransactionService` içinde tek TX: lock → domain → outbox satırları (event) → `sagaStore.success()` compare-and-set (`STARTED→SUCCEEDED`, step `CONFIRM`); CAS başarısızsa exception → rollback.
7. **Katılımcı uçları** — `consume/get/confirm/compensate` `/internal/<kaynak>/operations/{operationKey}/…`; `UNIQUE(caller_service, account_id, operation_key)`; `caller_service` JWT `act`/`iss`'ten; allowlist + kod içi aktör→işlem tipi; advisory lock + `FOR UPDATE`; tombstone; iade kuralları; `original_id`.
8. **Recovery worker** — claim (SKIP LOCKED + `lock_token` + lease 60 sn), prepare (`FOR UPDATE`, sahiplik + durum uzlaştırma), uzak confirm/compensate, complete (token eşleşmesi); hata → retry backoff `min(300, 2^n)`; belirsiz → `GET`; çelişki → `MANUAL_REVIEW`.
9. **Monitor + cleanup** — monitor 60 sn: 15 dk'dan eski çözülmemiş → ERROR + `saga_unresolved_total`; cleanup cron: terminal kayıtlar 30 gün; `MANUAL_REVIEW` silinmez; admin görünürlüğü (panel listesi).
10. **Outbox/event** — domain event `outbox_event` (`kind=EVENT`) ile; handler idempotent; tüketici inbox; `traceparent` taşınıyor.
11. **Config + gözlem** — `operation-consistency.{deadline-ms,lease-ms,poll-ms,monitor-ms,cleanup-cron}` local + deploy; metrikler ve alarm kuralı; loglar `sagaId`/`operationKey` ile, hesap kimliği yok.
12. **Testler** — `references/verification.md` matrisi; gerçek PG (Testcontainers) ile paralel claim ve expired lease; MVC binding; log privacy.

---

### `.agents/skills/proj-operation-consistency-review/references/verification.md`

### Verification — Yapısal Kontroller ve Senaryo Matrisi

#### 1. Yapısal kontroller (statik, kod okuyarak)

- [ ] `begin()` ayrı TX'te; consume TX dışında; domain + `success()` aynı TX'te ayrı bean.
- [ ] `success()` compare-and-set; başarısızlıkta exception ve rollback.
- [ ] Recovery `complete` yalnız `lock_token` eşleşince yazıyor.
- [ ] Katılımcıda advisory lock + `FOR UPDATE`; tombstone; `UNIQUE(caller_service, account_id, operation_key)`.
- [ ] Outbox yazıcı MANDATORY; handler idempotent; inbox dedup.
- [ ] `MANUAL_REVIEW` cleanup'ta silinmiyor; monitor metriği var.
- [ ] Loglarda hesap kimliği yok; `sagaId`/`operationKey` var.
- [ ] Config key'leri local + deploy.

#### 2. Senaryo matrisi

| # | Senaryo | Beklenen | Kanıt seviyesi | Test | Sonuç |
|---|---|---|---|---|---|
| 1 | Normal başarı | saga CONFIRMED, hak tüketildi, domain yazıldı | 2 | | |
| 2 | Aynı key ile replay | aynı sonuç, ikinci consume yok | 2 | | |
| 3 | Aynı key farklı body | ilk istek kazanır | 1 | | |
| 4 | Eşzamanlı aynı key | tek saga, tek consume | 2 | | |
| 5 | Farklı key aynı kaynak | domain uniqueness reddeder, saga compensate | 2 | | |
| 6 | Aynı UUID farklı hesap/aktör | ayrı saga; yetki reddi | 1 | | |
| 7 | begin sonrası, consume öncesi çökme | deadline → otomatik compensate (no-op tombstone) | 2 | | |
| 8 | Katılımcı commit + yanıt kaybı | GET ile durum; consume replay aynı sonucu döner | 3 | | |
| 9 | Consume commit + domain rollback | recovery compensate → iade | 2 | | |
| 10 | Domain commit + confirm öncesi çökme | recovery confirm | 2 | | |
| 11 | Geç gelen consume, tombstone var | consume uygulanmaz (`CANCELLED`) | 2 | | |
| 12 | confirm/compensate timeout | retry backoff; belirsizlikte GET | 1 | | |
| 13 | Eşzamanlı confirm ve compensate | tek sonuç; çelişki `MANUAL_REVIEW` | 2 | | |
| 14 | İki worker + expired lease | eski worker complete edemez | 2 | | |
| 15 | Request success vs recovery cancel yarışı | CAS kaybeden rollback | 2 | | |
| 16 | Tekrarlanan compensate | idempotent, çift iade yok | 2 | | |
| 17 | Eksik/bozuk key | 400, servise ulaşmaz | 1 | | |
| 18 | Geçersiz JWT / yanlış aktör | 401/403, saga yok | 1 | | |
| 19 | Cleanup ve monitor | terminal silinir, MANUAL_REVIEW kalır, metrik artar | 2 | | |
| 20 | Migration + restart | tablo/index uyumlu, in-flight saga kurtarılır | 2/4 | | |
| 21 | Outbox satırı domain TX ile rollback | satır yok | 2 | | |
| 22 | Tüketici duplicate olay | tek etki | 2 | | |
| 23 | Sıra bozuk olay (eski revision) | yok sayılır | 2 | | |
| 24 | Bilinmeyen event type | ack + log, DLQ değil | 1 | | |

Kanıt seviyeleri: 1 unit/MVC · 2 gerçek PostgreSQL (Testcontainers) · 3 owner→participant runtime (iki servis ayakta) · 4 release/staging.

#### 3. Sonuç

- Tüm satırlar `PASS` → **PASS**.
- Herhangi bir satır `FAIL` → **FAIL** (liste).
- Test/kanıt olmayan satır → **BLOCKED** (liste; "test yok" = geçmiş sayılmaz).

---

### `.agents/skills/proj-release-readiness-review/SKILL.md`

---
name: proj-release-readiness-review
description: Use this skill on release PRs (to release/main), before the first production deploy of a new service, and quarterly — to verify backups, alerting, SLOs, runbooks, capacity, version/EOL status, security posture and rollback readiness.
---

Sürümün üretime çıkmaya hazır olup olmadığını mimari referans Bölüm 8, 10.5, 18, 21.0, 24 ve 25'e göre değerlendir. Bu skill kod kalitesine değil **operasyonel gerçeklere** bakar: yedek var mı, alarm gidiyor mu, geri dönüş kaç dakika. Her madde için kanıt (dosya, config, pano linki, tarih) istenir; kanıtsız madde `BLOCKED`. Sorun yoksa: **"Release hazırlık kontrolü geçti."**

Kontrol et:

#### Veri ve yedek (yoksa `FAIL`)
- WAL arşivi + base backup çalışıyor (son başarılı yedek tarihi); retention; şifreli.
- **Restore provası** son 30 gün içinde yapılmış ve kayıtlı (süre, doğrulama).
- RPO/RTO README'de; HA durumu (managed/standby/yok) açıkça yazılı ve kabul edilmiş.
- Redis security instance AOF; RabbitMQ definitions yedeği; object storage versioning.
- Bu release'in migration'ları prod benzeri veri hacminde denenmiş (süre, lock).

#### Alarm ve gözlem (yoksa `FAIL`)
- Alertmanager/Grafana alerting bir kanala **gerçekten** bildirim gönderiyor (test alarmı tarihi).
- Asgari alarm seti (referans 8.7): restart-loop, health DOWN, disk/RAM, WAL arşiv gecikmesi, Redis bellek/eviction, RabbitMQ queue/DLQ, `outbox_oldest_pending_age_seconds`, `*_STUCK`, SLO burn-rate.
- Yeni servis/uç için scrape hedefi, log kaynağı, pano.
- Structured log; trace sampling politikası; Loki label kardinalitesi.

#### SLO ve kapasite
- Kritik akışlar için SLO tanımlı ve pano var; son yük testi tarihi ve sonucu (p99, hata oranı) SLO içinde.
- Kapasite tablosu: host RAM/CPU kullanımı < %70; ikinci instance/host planı; DB bağlantı bütçesi; Bölüm 24 eşiklerinden yaklaşılan var mı.

#### Deploy ve geri dönüş
- Image CI'da build, registry'de, cosign imzalı, digest ile deploy; prod'da build yok.
- Rollout sırası (sahip → tüketici → auth → gateway) release notunda; tüketici-önce kuralı yeni event/enum için sağlanmış.
- Rollback: önceki digest listesi mevcut; migration expand/contract'a uygun (eski image yeni şemayla çalışır); prova edilmiş rollback süresi.
- Healthcheck ve readiness her serviste; `restart: always`; `docker-rollout`/blue-green.
- Staging'de smoke test geçti; prod deploy onay kapısı var.

#### Güvenlik duruşu
- Secret'lar `/run/secrets`; literal fallback yok; gitleaks CI temiz; `.dockerignore`.
- Asimetrik service JWT; JWKS/`kid`; rotasyon prosedürü.
- Mock entegrasyon bean'i prod profilinde yok (açılış guard'ı testi).
- Seed/test verisi prod location'ında yok; bilinen parolalı admin yok.
- Gateway: rate limit, timeout, CORS listesi, `gateway` actuator kapalı, trusted proxy.
- Bağımlılık taraması (Dependabot/OSV) açık CRITICAL/HIGH yok veya kabul edilmiş risk yazılı.

#### Sürüm ve destek (Bölüm 25)
- Java, Spring Boot, Spring Cloud, PostgreSQL, Redis/Valkey, RabbitMQ, arama motoru, Alloy/Loki/Tempo/Prometheus/Grafana, Node: **hepsi OSS destek içinde**; bitişe < 3 ay kalan için upgrade PR/plan var.
- Yaması yalnız ticari sürümde olan bilinen CVE yok.
- Lisans değişikliği (Redis, Elastic, BSL, GPL) gözden geçirilmiş.

#### Uyum ve ürün
- Hesap silme akışı çalışıyor (uygulama içi + saga); export ucu; KVKK/GDPR süreleri ölçülüyor.
- Kişisel veri envanteri ve DPIA güncel; yeni alanlar eklendi mi.
- İstemci entegrasyon dokümanı ve OpenAPI/generated client release ile uyumlu; API sunset tarihleri.
- Runbook'lar: her alarm için "ne yapılır" sayfası; on-call kim; olay sonrası postmortem şablonu.

#### Dokümantasyon
- README kimlik tablosu, sıcak yol tablosu, fail politikası tablosu, kapasite tablosu güncel.
- Bu release için ADR gerektiren karar varsa ADR var.
- `docs/ai/repo-context.md` güncel; `docs/versions.md` tarihli.

Çıktı:
1. **Kontrol tablosu:** her başlık → `PASS` / `FAIL` / `BLOCKED` + kanıt (dosya/link/tarih).
2. **Engelleyiciler:** `FAIL` olanlar ve düzeltme; `BLOCKED` olanlar ve istenen kanıt.
3. **Kabul edilen riskler:** yazılı, sahipli, tarihli.
4. **Bölüm 24 eşik durumu:** yaklaşılan eşikler ve planlanan adım.
5. **Nihai durum:** `PASS` / `FAIL` / `BLOCKED`. Yedek, restore provası veya alarm kanalı eksikse durum `FAIL`.

---

### `.agents/skills/proj-resilience-review/SKILL.md`

---
name: proj-resilience-review
description: Use this skill when a change adds or modifies a remote synchronous call, a hot-path flow, timeouts, retries, circuit breakers, bulkheads, thread/connection pools, caches with fallback, or any control-plane dependency (parameters, config, flags).
---

Dayanıklılığı mimari referans Bölüm 1.2, 4.6, 4.7, 7.2/14.3 ve `AGENTS.md` Bölüm 5'e göre incele. Temel soru: **"Bu bağımlılık yavaşlar veya düşerse kullanıcı ne görür, servis ne yapar?"** Her uzak bağımlılık için bu cevap yazılı olmalı. Sorun yoksa: **"Bu kapsamda dayanıklılık bulgusu yok."**

Kontrol et:

#### Sıcak yol
- Değişen akış sıcak yolda mı (`docs/ai/repo-context.md` Bölüm 3)? Uzak senkron çağrı sayısı **≤ 1** mi? Artıyorsa `REQUEST CHANGES`: read-model / JWT claim / asenkron alternatif ve maliyeti yazılır; tablo güncellenir.
- Tek uzak çağrı yazma/rezervasyon türü mü (okuma değil)?
- Availability aritmetiği: bağımlı bileşen sayısı × %99,9 → beklenen üst sınır; p99 toplamı hedefle karşılaştırılmış.

#### Timeout bütçesi
- Gateway toplam bütçesi > client read timeout > downstream'in kendi downstream timeout'u (bütçe zinciri tutarlı).
- Her client grubunda `connect-timeout` ve `read-timeout` açıkça set; varsayılan/sonsuz timeout yok.
- Uzun işlemler (export, toplu işlem) senkron uçta değil; job + poll.

#### Circuit breaker / bulkhead / retry
- Hedef başına `resilience4j.circuitbreaker.instances.<hedef>` ve `bulkhead` tanımlı; açıkken tanımlı `ServiceException` (503 `UPSTREAM_UNAVAILABLE`) ve fallback davranışı belgeli.
- Spring Cloud CircuitBreaker kullanılıyorsa **TimeLimiter (varsayılan 1 sn)** ve thread-pool bulkhead ayarlanmış/kapatılmış.
- Senkron yolda retry **yok** (`NEVER_RETRY`); retry yalnız outbox/saga worker'ında, backoff ile, idempotent hedefe.
- Cascading failure hesabı: Tomcat/virtual thread modeli, Hikari havuzu, downstream timeout × istek hızı → havuz doluyor mu?

#### Fail politikası
- Yeni bağımlılık için fail-open/fail-closed kararı yazılı ve README tablosuyla uyumlu (güvenlik → closed; iş → open + metrik).
- Control-plane bağımlılığı (parametre, config, flag): bounded-staleness (son bilinen değer + disk snapshot + `*_staleness_seconds` metriği + T eşiği); "5 sn cache + 503" **yok**.
- Redis güvenlik state'i ile cache ayrı instance; eviction politikası doğru.
- Read-model "satır yok/eski" davranışı tanımlı.

#### Kapasite ve kaynak
- Yeni thread pool/executor sınırlı ve isimli; virtual thread'lerle pinning riski (`synchronized` + IO) yok.
- Hikari `maximum-pool-size` × instance ≤ PgBouncer/`max_connections` bütçesi.
- Bellek: yeni cache'in üst sınırı ve TTL'i var.
- WebSocket/uzun bağlantı sayısı pod başına sınırlı; presence/fan-out Redis'te.

#### Gözlem ve test
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

---

### `.agents/skills/proj-security-review/SKILL.md`

---
name: proj-security-review
description: Use this skill when reviewing authentication, authorization, service JWT, input validation, sensitive logging, uploads, rate limiting, DB queries, WebSocket, privacy, secrets or supply-chain aspects of a change.
---

Değişikliği `docs/ai/security-rules.md` (tek kaynak) ve OWASP ASVS/API Security Top 10 ile karşılaştır. Her bulgu için **exploit senaryosu** yaz; senaryo yazamıyorsan bulgu değil, öneridir. Kanıt yoksa `needs verification`. Sorun yoksa: **"Bu kapsamda güvenlik bulgusu yok."**

Kontrol et:

#### Trust boundary ve kimlik
- Üç JWT yüzeyi karışmıyor: `typ`, anahtar ve `aud` ayrı. Service JWT asimetrik; `iss` = imzalayan; `kid`/JWKS ile doğrulama; simetrik paylaşılan secret **yok** (varsa `BLOCK`).
- Filtre sırası: path decode+normalize → imza/iss/aud/exp → (opsiyonel jti) → allowlist first-match → default-deny. Allowlist ham URI ile eşleşmiyor.
- `internal-access` kuralı gerçek kullanım kadar dar; dar kural catch-all'dan önce; local yml ve deploy config **birlikte** güncellenmiş.
- Kullanıcı adına internal uç `sub` == path hesabı kontrolü yapıyor; hesabı body'den alan internal uç hiçbir aktöre açık değil.
- Gateway: `/internal` engeli `StripPrefix` sonrası da; iç header temizliği; CORS `*` yok; token query'de yok; `gateway` actuator ucu kapalı; trusted-proxy ayarı; rate limit ve timeout bütçesi.

#### Ownership / IDOR
- Hesap kimliği yalnız `@CurrentAccount`; path/query/body'den değil. Path'teki id hedef kaynak; ownership serviste doğrulanıyor.
- Liste/sayfalama uçlarında sıralama alanı allowlist; boyut sınırı.
- Read-model'den yetki kararı veriliyorsa "satır yok / eski" davranışı fail-closed.

#### OTP, token, abuse
- OTP: `SecureRandom`, tuzlu hash, sabit zamanlı karşılaştırma, TTL + deneme sınırı, enumeration koruması; outbox payload'ında düz metin OTP kalmıyor.
- SMS pumping savunması `otp/start` için: geo-permissions, numara doğrulama, numara/IP/cihaz tavanı, velocity, harcama kesicisi.
- Refresh token: opak + hash, aile rotasyonu, reuse → aile iptali, mutlak süre.
- `sv` artışı: şifre sıfırlama, logout-all, **ban**, rol değişimi.
- Admin 2FA passkey/TOTP; SMS yalnız kurtarma. Admin OTP'si kullanıcı OTP'sinden zayıf değil.
- Rate limit: yeni saldırıya açık uç scope aldı; sayaç Redis'te; fail politikası scope tablosuyla uyumlu; `Retry-After`.
- Mock entegrasyon (`MockStoreValidation`, `MockSms`…) `@Profile("local|test")` ve prod açılış guard'ı var (yoksa `BLOCK`).
- Cihaz attestation sinyali gereken uçlarda (kayıt, OTP-gönder, satın alma) risk skoruna bağlanmış.

#### Input ve sorgu güvenliği
- `@Valid`, boyut sınırları, enum parse (`from`), path/header/param adları açık.
- Native SQL'de parametre bağlama; dinamik sıralama/filtre allowlist; JPQL string birleştirme yok.
- Dosya adı/path kullanıcıdan gelmiyor; `..` normalize.
- JSON: bilinmeyen alan politikası bilinçli; polimorfik deserialization kapalı.

#### WebSocket / realtime
- CONNECT'te native `Authorization` zorunlu; süresi dolmuş token ile komut reddi; SUBSCRIBE deny-all allowlist; `sv` süpürücüsü; Redis zarfı HMAC + tazelik + `relayId`; local'de bile imzasız zarf kabul edilmiyor.

#### Object storage ve içerik
- Yükleme private quarantine → yeniden kodlama (EXIF/GPS) → moderasyon (CSAM hash + NSFW) → private delivery + signed GET. Public bucket'a doğrudan yükleme `BLOCK`.
- Presigned URL kısa TTL; key sunucuda üretilmiş; HEAD/ETag doğrulaması.
- Uçtan uca şifreleme varsa: iddia ile şema uyumlu (FS/MITM sınırları yazılı), anahtar pinleme, message franking; sohbet anahtarı panele verilmiyor.

#### Log, hata, secret
- `security-rules.md` Bölüm 4 yasak listesi: hiçbir ham PII/secret/body/exception mesajı log'da yok. Log'a özel neden `safeLogReason`'da; `details`'te yalnız istemciye gösterilebilir bilgi.
- Yeni secret: değeri hiçbir yerde yok; `/run/secrets` ile geliyor; `${ENV:literal}` fallback yok; SOPS dosyasına eklenmiş; `.dockerignore` kapsıyor.
- Hata yanıtları tek format (`ErrorResponse`); filtre redleri de aynı; altyapı hatası 503 (500 değil).
- Audit: `details` allowlist; anahtar/şifreli içerik/serbest metin/ham IP yok; tablo değiştirilemez.

#### Privacy
- Yeni kişisel veri alanı: envanter + aydınlatma + retention + silme saga'sı kapsamı güncellendi.
- Düşük entropili kimlik için HMAC+pepper; varlık oracle'ı yok; konum deterministik grid; kişisel veri dönen uç `no-store`.
- Arama index'i/cache/yedek aynı kurallara uyuyor.

#### Tedarik zinciri
- Yeni bağımlılık: lisans (GPL/AGPL/BSL) ve bilinen CVE kontrolü; sürüm OSS destekli.
- Dockerfile non-root, `.dockerignore`, image CI'da build.

Çıktı:
1. **Risk seviyesi:** `CRITICAL` / `HIGH` / `MEDIUM` / `LOW` / `OK`.
2. **Bulgular** (her biri): `severity · dosya:satır · kanıt · exploit senaryosu (kim, nasıl, sonuç) · düzeltme · doğrulayan test`.
3. **Kural referansı:** her bulgu için `security-rules.md` bölümü.
4. **Net kanıt bulunamayan alanlar.**
5. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`. `CRITICAL` veya `HIGH` bulgu varsa karar `APPROVE` olamaz.

---

### `.agents/skills/proj-spring-code-review/SKILL.md`

---
name: proj-spring-code-review
description: Use this skill when reviewing Spring Boot service code in code review or after changes — layering, transactions, multi-instance safety, exception/log discipline, dynamic parameters, hot-path calls.
---

Spring servis kodunu mimari referansa ve `AGENTS.md`'ye göre incele. Kanıt odaklı ol: her bulgu dosya + satır + gerekçe taşır. Spekülatif bulgu üretme; kanıt yoksa `needs verification` yaz. Sorun yoksa sabit cümle: **"Bu kapsamda bulgu yok."**

Kontrol et:
- Controller ince mi: yalnız binding, `@Valid`, `@CurrentAccount`, header okuma, status/`Cache-Control`. Business logic, repository erişimi, try/catch ile hata dönüşü **yok**.
- Constructor injection (`@RequiredArgsConstructor`); field injection yok.
- Geniş `catch (Exception)` yok; yakalanan hata ya tipli ServiceException'a çevriliyor ya da yeniden fırlatılıyor.
- Use-case servisleri arayüz + `service/impl/*ServiceImpl`; guard/policy/provider/poller somut sınıf. `service.impl` altında Impl olmayan sınıf yok; `@Configuration` yalnız `config/`.
- Mapping servis katmanında veya `*Mapper`; tek metot adı (`toResponse`). Controller'da mapping yok.
- Hedef servis başına **tek** HTTP client (`client/` paketi); DTO'lar hedefin `*-api` modülünden; `X-Subject-Id` karşı tarafta beklenmiyor (`@CurrentAccount` ile okunuyor).
- Kimlik yalnız `@CurrentAccount`; path/query/body'den hesap kimliği alınmıyor.
- Yeni `@RequestBody` → `@Valid`; `@PathVariable("...")`, `@RequestParam("...")`, `@RequestHeader("...")` adları açık.

#### Transaction ve Sıcak Yol
- Uzak HTTP çağrısı `@Transactional` metot, row lock veya advisory lock **içinde değil**. Kanonik biçim: `ServiceImpl` (tx yok) → guard'lar → `*TransactionService` (tx) → `AFTER_COMMIT` yan etkileri → outbox.
- Outbox yazıcı `Propagation.MANDATORY`; audit/log kaydı `REQUIRES_NEW` yalnız gerekçeliyse.
- `noRollbackFor` kullanımı gerekçeli (deneme sayacı, token iptali).
- Sıcak yolda (kullanıcıya latency yansıyan akış) **okuma amaçlı** uzak çağrı eklenmiş mi? Eklendiyse `REQUEST CHANGES`: read-model veya JWT claim öner; `docs/ai/repo-context.md` sıcak yol tablosuna bak. Yazma türü tek çağrı kabul; ikincisi `proj-resilience-review` ister.
- Her client çağrısı timeout + circuit breaker + bulkhead altında mı (grup config'i var mı)?

#### Multi-Instance Safety
- `@Scheduled`/poller/worker tek instance varsaymıyor: `FOR UPDATE SKIP LOCKED` + lease + `claim_token`; sonuç yazımı claim sahibine kısıtlı; lease güvenlik payı.
- Check-then-act (`existsBy` → `save`) yerine unique/partial unique, koşullu update veya lock.
- JVM-local state yok (rate limit, cache, "tek aktif kayıt", WebSocket üyeliği). İzinli JVM cache: kısa TTL + bayatlık toleransı + yorumla gerekçe.
- Bilinçli single-instance davranış yorumla belgelenmiş.

#### Exception ve Log
- Throw/rethrow öncesi tek structured log: `"<Olay> rejected: code=X reason=Y"`; kullanıcı kaynaklı 4xx WARN, altyapı/beklenmeyen ERROR.
- Ham `e.getMessage()`, body, token, PII loglanmıyor (`docs/ai/security-rules.md` Bölüm 4). Log'a özel neden `safeLogReason`'da, `details`'te değil.
- Normal Flow Logging: akış INFO'dan izlenebilir (`operation=`, `outcome=`); her metot loglanmıyor; batch'ler özetleniyor; boş poll turu loglanmıyor; commit görülmeden "success" yazılmıyor.
- Tek log stili (`"<Olay>: key=value"`); `[TAG]`/snake_case karışımı yok.
- `ErrorCode` yeni sabiti servisin bloğunda ve global tekil (test geçiyor mu?).

#### Tracing ve Async Context
- Outbox satırında `traceparent/tracestate`; poller span'i bu bağlamdan başlıyor; MQ header'larına inject; span attribute'larında PII yok.
- `@Async`/executor kullanımında context propagation korunuyor.

#### Sistem Parametreleri
- Admin'in değiştirebileceği iş kuralı değeri config/env/`@Value` ile **değil**, `SystemParameterProvider` ile okunuyor; key yalnız enum.
- Fail politikası: key yok / değer bozuk → fail-closed; kaynak erişilemez → bounded-staleness (grup `criticality`'sine göre) — kod içi default/yml fallback **yok**.
- Tazelik doğru seçilmiş: kullanıcı girdisini doğrulayan yazma `freshGroup`; kalıcı sonuçta revizyon snapshot'ı; birlikte anlamlı key'ler aynı revizyondan; TX/lock dışında okuma; worker her turda yeniden okuyor.
- Değer düşürülünce mevcut veriyi uzlaştıran worker var mı?
- Yeni key için: enum + migration + registry kuralı + `usage_status` + tutarlılık testleri.

#### Entity ve Repository
- Entity suffix'siz; `@Enumerated(STRING)`; `Instant` ↔ `TIMESTAMPTZ`; `@Data`/`@ToString` yok; hassas alanlarda `@ToString.Exclude`.
- ID üretimi tek yöntem (UUIDv7, platform generator); `@PrePersist`/elle atama yok.
- Optimistic lock gereken aggregate'te `@Version`; 409 handler.
- Kilit sırası birden çok kilit alan akışta belgelenmiş; lock timeout hint'i var.
- Native sorgular tam nitelikli `<schema>.<tablo>` ve **yalnız kendi şeması**.

Çıktı (7 madde):
1. **Ne değişti** (2–4 cümle, dosya listesi).
2. **Neden önemli** (risk: doğruluk / güvenlik / operasyon / performans).
3. **Bulgular**: her biri `severity (BLOCKER|HIGH|MEDIUM|LOW) · dosya:satır · kanıt · düzeltme`.
4. **Testler**: eksik test case'leri (negatif, yetki, concurrency, log privacy).
5. **Log doğrulaması**: hassas veri sızıntısı var mı; seviye politikası doğru mu.
6. **INFO akış görünürlüğü**: akış DEBUG açmadan izlenebilir mi.
7. **Parametre ve sıcak yol uyumu**: parametre kuralları; sıcak yol uzak çağrı sayısı (tablo güncellendi mi).
8. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`.

---

### `.agents/skills/proj-test-writer/SKILL.md`

---
name: proj-test-writer
description: Use this skill when writing or reviewing unit, HTTP binding, security access, repository/concurrency, contract, resilience, log-privacy or regression tests for a change.
---

Testleri mimari referans Bölüm 16 ve `docs/ai/security-rules.md` Bölüm 4'e göre yaz/incele. Önce **davranış ve edge case listesi**, sonra test. Anlamsız test (yalnız mock'un çağrıldığını doğrulayan) yazılmaz. Çıktıda eklenen case'ler, kalan boşluklar ve çalıştırma komutları listelenir.

Kontrol et / yaz:

#### Kapsam kuralları
- Negatif case'ler ve **yetki hataları** zorunlu (izinsiz aktör 403, kimliksiz 400/401, başka kullanıcının kaynağı 404/403).
- Her bug için regression testi.
- Poller, worker ve check-then-act kodu için **concurrency** testi: iki paralel claim ayrık satır; eşzamanlı ikinci insert reddedilir; expired lease'te eski worker complete edemez (gerçek PG).
- Kırılgan test yok: sıra, saat (`Instant now()` metodu üzerinden verilir), rastgelelik, dış servis bağımlılığı yok.
- Test isimlendirmesi tek stil (`metot_whenKoşul_beklenenSonuç`); yorumlar ekibin dilinde.

#### Türler
- **HTTP binding:** `MockMvcBuilders.standaloneSetup(controller)` + `CurrentAccountArgumentResolver` + `GlobalServiceExceptionHandler`; kimlik `requestAttr("x.accountId", …)`. Geçerli / geçersiz tip / eksik kimlik / eksik header; geçersiz girdide `verifyNoInteractions(service)`. Controller metodunu doğrudan çağırmak binding'i kanıtlamaz.
- **Security erişimi:** gerçek `ServiceJwtVerificationFilter` + config'ten okunan allowlist + sentetik token (test anahtar çiftiyle imzalı): izinli aktör 200, diğerleri 403, `/internal` kural yoksa 403, kodlanmış path (`%2e%2e`) 400. JWT doğrulaması tamamen mock'lanmaz.
- **Client contract:** `MockRestServiceServer`/sahte `Client` ile giden istek (method, path, header, body) yakalanır; interface mock'u yeterli değil.
- **Repository / gerçek DB:** Testcontainers `@ServiceConnection`; gerçek Flyway migration'ları; JPQL doğrulama testi (DB'siz parse) ayrıca.
- **Outbox/event:** satır domain TX ile rollback oluyor; handler idempotent; tüketici duplicate/sıra bozuk/bilinmeyen tip davranışı.
- **Saga:** `references/verification.md` matrisi (operation-consistency skill'i).
- **Parametre:** kaynak erişilemez (bounded-staleness: T içinde son değer, T sonrası kritik grup 503), key yok / değer bozuk → hiç yan etki yok, default'a düşülmüyor.
- **Dayanıklılık:** hedef yanıt vermiyor → timeout bütçesi, circuit açılıyor, tanımlı hata; retry yok.
- **Log privacy:** Logback `ListAppender`; sentetik hassas işaret (`SENSITIVE-MARKER-…`, telefon, token) rendered mesajda / argümanlarda / MDC'de / exception'da **yok**; güvenli alan (`outcome=`) **var**; "hiç log yok" testi geçirmez; `@AfterEach` appender detach + seviye reset.
- **ArchUnit / statik:** yeni paket veya kural varsa `ArchitectureRulesTest` güncel; config drift testi yeni key'i kapsıyor; ErrorCode tekillik testi geçiyor.
- **Frontend:** Vitest + Testing Library; kod→mesaj tablosu backend enum'larıyla contract testi.

#### Komutlar
```bash
mvn -B -ntp verify -pl <modül> -amd
mvn -B -ntp test -pl <modül> -Dtest='<Sınıf>Test'
node --test scripts/
npm --prefix <panel>-web test
```

Çıktı:
1. **Davranış listesi** (test öncesi): normal, sınır, hata, yetki, eşzamanlılık.
2. **Eklenen test case'leri:** `sınıf#metot · ne kanıtlıyor · tür`.
3. **Kalan boşluklar:** yazılamayan/atlanan case'ler ve nedeni.
4. **Doğrulanan binding ve log davranışı** (kısa).
5. **Çalıştırma komutları ve sonuç** (geçti/kaldı; kaldıysa çıktı).

---

### `.claude/settings.json`

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Edit|Write|MultiEdit",
        "hooks": [
          {
            "type": "command",
            "command": "node \"$CLAUDE_PROJECT_DIR/.claude/hooks/flyway-immutability.js\"",
            "timeout": 20
          }
        ]
      },
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR/.claude/hooks/review-gate.sh\"",
            "timeout": 10
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Skill",
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR/.claude/hooks/review-stamp.sh\"",
            "timeout": 10
          }
        ]
      }
    ]
  }
}
```

---

### `.claude/hooks/flyway-immutability.js`

```javascript
#!/usr/bin/env node
/**
 * Claude Code PreToolUse hook (Edit|Write|MultiEdit): base'teki V*.sql dosyasina yazmayi engeller.
 * Kuralin tek kaynagi scripts/flyway-immutability.js'dir; bu dosya yalnizca adaptordur.
 *
 * Fail-closed: beklenmeyen hata veya bozuk girdi de engeller (exit 2). stderr Claude'a gider.
 * Base: FLYWAY_BASE_REF yoksa origin/develop; origin yoksa HEAD agacina gore korur.
 */
'use strict';
const path = require('node:path');

function fail(msg) {
  process.stderr.write(`[flyway-immutability hook] ${msg}\n`);
  process.exit(2);
}

let raw = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', (c) => { raw += c; });
process.stdin.on('end', () => {
  let input;
  try {
    input = JSON.parse(raw);
  } catch (e) {
    fail(`hook girdisi JSON degil: ${e.message}`);
  }
  const filePath = input && input.tool_input && input.tool_input.file_path;
  if (!filePath) fail('tool_input.file_path yok; fail-closed');

  const root = process.env.CLAUDE_PROJECT_DIR || process.cwd();
  let script;
  try {
    script = require(path.join(root, 'scripts', 'flyway-immutability.js'));
  } catch (e) {
    fail(`scripts/flyway-immutability.js yuklenemedi: ${e.message}`);
  }

  try {
    const r = script.checkFile(filePath, { baseRef: process.env.FLYWAY_BASE_REF, onMissingBase: 'head', cwd: root });
    if (r.protected) {
      fail(`${r.file} base branch'te (${r.base}) mevcut bir Flyway migration'i; degistirilemez/silinemez. Degisikligi yeni bir V<sonraki>__*.sql dosyasiyla yap.`);
    }
    process.exit(0);
  } catch (e) {
    fail(`dogrulanamadi (fail-closed): ${e.message}`);
  }
});
```

---

### `.claude/hooks/review-gate.sh`

```bash
#!/usr/bin/env bash
# Claude Code PreToolUse hook (Bash): `git push` oncesi son 1 saatte bir review skill'i calismis mi?
# Calismadiysa kullaniciya sorar (permissionDecision: "ask"). Push disi komutlarda sessizce gecer.
#
# Neden ayri dosya: settings.json icine gomulu shell komutlarinda kacis hatasi kolay yapilir ve hook
# sessizce etkisiz kalir. Bu dosya `bash -n` ve ornek girdiyle test edilir:
#   echo '{"tool_input":{"command":"git push origin x"}}' | .claude/hooks/review-gate.sh
set -euo pipefail

INPUT="$(cat)"
CMD="$(printf '%s' "$INPUT" | node -e 'let s="";process.stdin.on("data",c=>s+=c).on("end",()=>{try{const j=JSON.parse(s);process.stdout.write(String((j.tool_input&&j.tool_input.command)||""))}catch(e){process.stdout.write("")}})')"

case "$CMD" in
  *"git push"*) ;;
  *) exit 0 ;;
esac

ROOT="${CLAUDE_PROJECT_DIR:-$PWD}"
STAMP="$ROOT/.claude/.last-review-check"
MAX_AGE=3600
NOW="$(date +%s)"

if [[ -f "$STAMP" ]]; then
  LAST="$(cat "$STAMP" 2>/dev/null || echo 0)"
  if [[ "$LAST" =~ ^[0-9]+$ ]] && (( NOW - LAST < MAX_AGE )); then
    exit 0
  fi
fi

cat <<'JSON'
{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"ask","permissionDecisionReason":"Son 1 saatte review skill'i (proj-*-review / proj-test-writer) calistirilmadi. docs/ai/review-checklist.md'deki skill'leri calistirmadan push etmek istiyor musun?"}}
JSON
exit 0
```

---

### `.claude/hooks/review-stamp.sh`

```bash
#!/usr/bin/env bash
# Claude Code PostToolUse hook (Skill): review skill'i calistiysa damga yaz.
set -euo pipefail
INPUT="$(cat)"
NAME="$(printf '%s' "$INPUT" | node -e 'let s="";process.stdin.on("data",c=>s+=c).on("end",()=>{try{const j=JSON.parse(s);process.stdout.write(String((j.tool_input&&(j.tool_input.skill||j.tool_input.name))||""))}catch(e){process.stdout.write("")}})')"
case "$NAME" in
  *-review|*test-writer|*integration-doc)
    ROOT="${CLAUDE_PROJECT_DIR:-$PWD}"
    mkdir -p "$ROOT/.claude"
    date +%s > "$ROOT/.claude/.last-review-check"
    ;;
esac
exit 0
```

---

### `scripts/flyway-immutability.js`

```javascript
#!/usr/bin/env node
/**
 * Flyway migration degismezligi — TEK KAYNAK.
 * CI (workflow), AI hook (.claude/hooks/flyway-immutability.js) ve elle kullanim ayni kodu cagirir.
 *
 * Kural: base branch'e girmis (merge-base'te var olan) her `db/migration/**\/V*.sql` dosyasi
 * degistirilemez, silinemez, yeniden adlandirilamaz. Branch'te yeni eklenen V* dosyalari ve
 * tum R__* dosyalari serbesttir.
 *
 * Kullanim:
 *   node scripts/flyway-immutability.js check [--base <ref>] [--on-missing-base fail|head]
 *   node scripts/flyway-immutability.js check-file <yol> [--base <ref>] [--on-missing-base fail|head]
 *
 * Cikis kodlari: 0 uygun · 1 ihlal · 3 dogrulanamadi (git hatasi; fail-closed).
 * Base cozumleme sirasi: --base → FLYWAY_BASE_REF → origin/develop. '-' ile baslayan ref reddedilir.
 */
'use strict';

const { execFileSync } = require('node:child_process');
const path = require('node:path');

const MIGRATION_RE = /(^|\/)db\/migration\/(.+\/)?V[^/]*\.sql$/i;
const DEFAULT_BASE = 'origin/develop';

function normalize(p) {
  return String(p).replace(/\\/g, '/');
}

function isMigrationPath(p) {
  return MIGRATION_RE.test(normalize(p));
}

function git(args, opts = {}) {
  return execFileSync('git', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'], ...opts });
}

function assertSafeRef(ref) {
  if (typeof ref !== 'string' || ref.length === 0 || ref.startsWith('-')) {
    throw new Error(`Gecersiz base ref: ${JSON.stringify(ref)}`);
  }
  return ref;
}

function resolveBase(explicit) {
  return assertSafeRef(explicit || process.env.FLYWAY_BASE_REF || DEFAULT_BASE);
}

/** merge-base bulunamazsa: 'fail' → hata (exit 3), 'head' → HEAD agacina gore koru. */
function resolveComparePoint({ baseRef, onMissingBase = 'fail', cwd }) {
  try {
    return git(['merge-base', baseRef, 'HEAD'], { cwd }).trim();
  } catch (err) {
    if (onMissingBase === 'head') {
      try {
        return git(['rev-parse', 'HEAD'], { cwd }).trim();
      } catch (e2) {
        throw new Error(`HEAD cozumlenemedi: ${e2.message}`);
      }
    }
    throw new Error(`merge-base bulunamadi (base=${baseRef}): ${err.message}`);
  }
}

function repoRoot(cwd) {
  return git(['rev-parse', '--show-toplevel'], { cwd }).trim();
}

function toRepoRelative(filePath, cwd) {
  const root = repoRoot(cwd);
  const abs = path.isAbsolute(filePath) ? filePath : path.resolve(cwd || process.cwd(), filePath);
  return normalize(path.relative(root, abs));
}

/**
 * Tum degisiklikleri (working tree + staged + branch commit'leri) merge-base'e gore inceler.
 * --no-renames: yeniden adlandirma D + A olarak gorunur; D ihlaldir.
 */
function check({ baseRef, onMissingBase = 'fail', cwd } = {}) {
  const base = resolveBase(baseRef);
  const point = resolveComparePoint({ baseRef: base, onMissingBase, cwd });
  const out = git(['diff', '--name-status', '--no-renames', '-z', point], { cwd });
  const parts = out.split('\0').filter((s) => s.length > 0);
  const violations = [];
  for (let i = 0; i + 1 < parts.length; i += 2) {
    const status = parts[i].trim();
    const file = normalize(parts[i + 1]);
    if (!isMigrationPath(file)) continue;
    if (status === 'A') continue; // branch'te yeni eklenen V* serbest
    violations.push({ status, file });
  }
  return { base, point, violations };
}

/** Tek dosya: base agacinda varsa korunur (hook bu fonksiyonu yazma aninda cagirir). */
function checkFile(filePath, { baseRef, onMissingBase = 'fail', cwd } = {}) {
  const rel = toRepoRelative(filePath, cwd);
  if (!isMigrationPath(rel)) return { protected: false, file: rel };
  const base = resolveBase(baseRef);
  const point = resolveComparePoint({ baseRef: base, onMissingBase, cwd });
  const listed = git(['ls-tree', '-r', '--name-only', point, '--', rel], { cwd }).trim();
  return { protected: listed.length > 0, file: rel, base, point };
}

function parseArgs(argv) {
  const args = { _: [] };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--base') args.base = argv[++i];
    else if (a === '--on-missing-base') args.onMissingBase = argv[++i];
    else args._.push(a);
  }
  return args;
}

function main(argv) {
  const args = parseArgs(argv);
  const cmd = args._[0];
  try {
    if (cmd === 'check') {
      const r = check({ baseRef: args.base, onMissingBase: args.onMissingBase });
      if (r.violations.length === 0) {
        console.log(`flyway-immutability: OK (base=${r.base}, merge-base=${r.point.slice(0, 12)})`);
        return 0;
      }
      console.error(`flyway-immutability: IHLAL — base'teki migration dosyalarina dokunulmus (base=${r.base}):`);
      for (const v of r.violations) console.error(`  ${v.status}\t${v.file}`);
      console.error('Duzeltme: dosyayi geri al; degisikligi yeni bir V<sonraki>__*.sql ile yap.');
      return 1;
    }
    if (cmd === 'check-file') {
      const file = args._[1];
      if (!file) throw new Error('check-file icin dosya yolu gerekli');
      const r = checkFile(file, { baseRef: args.base, onMissingBase: args.onMissingBase });
      if (r.protected) {
        console.error(`flyway-immutability: IHLAL — ${r.file} base'te (${r.base}) mevcut; degistirilemez. Yeni V<sonraki>__*.sql ac.`);
        return 1;
      }
      console.log(`flyway-immutability: OK (${r.file})`);
      return 0;
    }
    console.error('Kullanim: check [--base <ref>] [--on-missing-base fail|head] | check-file <yol> [--base <ref>]');
    return 3;
  } catch (err) {
    console.error(`flyway-immutability: DOGRULANAMADI — ${err.message}`);
    return 3;
  }
}

module.exports = { MIGRATION_RE, isMigrationPath, resolveBase, check, checkFile, main };

if (require.main === module) {
  process.exit(main(process.argv.slice(2)));
}
```

---

### `scripts/flyway-immutability.test.js`

```javascript
'use strict';
// node --test scripts/flyway-immutability.test.js
const { test, beforeEach, afterEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');

const script = require('./flyway-immutability.js');

let repo;
function git(...args) {
  return execFileSync('git', args, { cwd: repo, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
}
function write(rel, content) {
  const p = path.join(repo, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content);
}
function commitAll(msg) {
  git('add', '-A');
  git('-c', 'user.email=t@t', '-c', 'user.name=t', 'commit', '-q', '-m', msg);
}

beforeEach(() => {
  repo = fs.mkdtempSync(path.join(os.tmpdir(), 'flyway-imm-'));
  git('init', '-q', '-b', 'develop');
  write('svc/src/main/resources/db/migration/V1__init.sql', 'CREATE SCHEMA s;');
  write('svc/src/main/resources/db/migration/R__seed.sql', '-- seed');
  write('svc/src/main/resources/db/migration/nested/V2__nested.sql', 'SELECT 1;');
  commitAll('base');
  git('checkout', '-q', '-b', 'feature/x');
});

afterEach(() => {
  fs.rmSync(repo, { recursive: true, force: true });
});

test('regex: V dosyalari eslesir, R ve migration disi eslesmez', () => {
  assert.equal(script.isMigrationPath('a/db/migration/V1__x.sql'), true);
  assert.equal(script.isMigrationPath('a/db/migration/sub/V9__x.SQL'), true);
  assert.equal(script.isMigrationPath('a\\db\\migration\\V1__x.sql'), true); // Windows
  assert.equal(script.isMigrationPath('a/db/migration/R__x.sql'), false);
  assert.equal(script.isMigrationPath('a/db/other/V1__x.sql'), false);
  assert.equal(script.isMigrationPath('a/db/migration/V1__x.txt'), false);
});

test('base ref guvenligi', () => {
  assert.throws(() => script.resolveBase('--upload-pack=x'));
  assert.equal(script.resolveBase('develop'), 'develop');
});

test('temiz branch: OK', () => {
  const r = script.check({ baseRef: 'develop', cwd: repo });
  assert.equal(r.violations.length, 0);
});

test("base'teki V dosyasini degistirmek ihlal (working tree)", () => {
  write('svc/src/main/resources/db/migration/V1__init.sql', 'CREATE SCHEMA s; -- changed');
  const r = script.check({ baseRef: 'develop', cwd: repo });
  assert.deepEqual(r.violations.map((v) => v.status), ['M']);
});

test("base'teki V dosyasini silmek ihlal (commit'li)", () => {
  fs.rmSync(path.join(repo, 'svc/src/main/resources/db/migration/V1__init.sql'));
  commitAll('delete');
  const r = script.check({ baseRef: 'develop', cwd: repo });
  assert.deepEqual(r.violations.map((v) => v.status), ['D']);
});

test('git mv ile yeniden adlandirma ihlal (D + A)', () => {
  git('mv', 'svc/src/main/resources/db/migration/V1__init.sql', 'svc/src/main/resources/db/migration/V1__renamed.sql');
  const r = script.check({ baseRef: 'develop', cwd: repo });
  assert.equal(r.violations.some((v) => v.status === 'D'), true);
});

test('ic ice klasordeki base migration da korunur', () => {
  write('svc/src/main/resources/db/migration/nested/V2__nested.sql', 'SELECT 2;');
  const r = script.check({ baseRef: 'develop', cwd: repo });
  assert.equal(r.violations.length, 1);
});

test("branch'te eklenen yeni V dosyasi serbest, sonradan duzenlenebilir", () => {
  write('svc/src/main/resources/db/migration/V3__new.sql', 'SELECT 3;');
  commitAll('add v3');
  write('svc/src/main/resources/db/migration/V3__new.sql', 'SELECT 33;');
  const r = script.check({ baseRef: 'develop', cwd: repo });
  assert.equal(r.violations.length, 0);
});

test('R__ dosyalari serbest', () => {
  write('svc/src/main/resources/db/migration/R__seed.sql', '-- seed v2');
  const r = script.check({ baseRef: 'develop', cwd: repo });
  assert.equal(r.violations.length, 0);
});

test('check-file: base dosyasi korunur, yeni dosya korunmaz, migration disi dosya korunmaz', () => {
  assert.equal(script.checkFile('svc/src/main/resources/db/migration/V1__init.sql', { baseRef: 'develop', cwd: repo }).protected, true);
  assert.equal(script.checkFile('svc/src/main/resources/db/migration/V4__later.sql', { baseRef: 'develop', cwd: repo }).protected, false);
  assert.equal(script.checkFile('svc/src/main/java/X.java', { baseRef: 'develop', cwd: repo }).protected, false);
  // mutlak yol ve ters bolu
  const abs = path.join(repo, 'svc\\src\\main\\resources\\db\\migration\\V1__init.sql');
  assert.equal(script.checkFile(abs, { baseRef: 'develop', cwd: repo }).protected, true);
});

test('base yoksa: fail → hata; head → HEAD agacina gore', () => {
  assert.throws(() => script.check({ baseRef: 'no-such-branch', cwd: repo }));
  const r = script.checkFile('svc/src/main/resources/db/migration/V1__init.sql', {
    baseRef: 'no-such-branch', onMissingBase: 'head', cwd: repo,
  });
  assert.equal(r.protected, true);
});

test('main: cikis kodlari', () => {
  const cwd = process.cwd();
  process.chdir(repo);
  try {
    assert.equal(script.main(['check', '--base', 'develop']), 0);
    write('svc/src/main/resources/db/migration/V1__init.sql', 'x');
    assert.equal(script.main(['check', '--base', 'develop']), 1);
    assert.equal(script.main(['check', '--base', 'no-such']), 3);
    assert.equal(script.main(['bogus']), 3);
  } finally {
    process.chdir(cwd);
  }
});
```

---

### `tests/ArchitectureRulesTest.java`

```java
package com.acme.order;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mimari kurallarin makine zorlamasi. Her core modulde bu sinifin bir kopyasi bulunur;
 * yalniz ROOT (paket koku) degisir. Referans: mikroservis-mimari-referans.md Bolum 4, 16, 19.5.
 *
 * Bagimlilik: com.tngtech.archunit:archunit-junit5 (test scope) — kurallar duz JUnit @Test olarak
 * calisir; ArchUnit'in kendi JUnit engine'ine (@ArchTest) bagimli DEGILDIR. Neden: engine, JUnit
 * Platform major surumleriyle uyumsuz kalabiliyor ve testler sessizce "0 test" olarak gecebiliyor.
 * Spring Boot 4.1 + ArchUnit 1.5.1 ile dogrulandi (bos iskelette kasitli ihlaller yakalandi).
 */
class ArchitectureRulesTest {

    static final String ROOT = "com.acme.order";
    static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    /** Katmanlar: controller → service → repository. Controller repository'ye dokunamaz. */
    @Test
    void layersAreRespected() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true) // readmodel/outbox paketi henuz yoksa "Layer is empty" ihlali uretmesin
                .layer("Controller").definedBy(ROOT + ".controller..")
                .layer("Service").definedBy(ROOT + ".service..")
                .layer("Repository").definedBy(ROOT + ".repository..")
                .layer("ReadModel").definedBy(ROOT + ".readmodel..")
                .layer("Outbox").definedBy(ROOT + ".outbox..", ROOT + ".worker..", ROOT + ".saga..")
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "ReadModel", "Outbox")
                .check(classes);
    }

    @Test
    void controllersDoNotUseRepositoriesOrEntities() {
        noClasses().that().resideInAPackage(ROOT + ".controller..")
                .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".repository..", ROOT + ".entity..")
                .because("controller ince katmandir; mapping ve veri erisimi serviste yapilir")
                .check(classes);
    }

    /** service.impl altinda yalniz *ServiceImpl bulunur. */
    @Test
    void implPackageOnlyHoldsServiceImpls() {
        classes().that().resideInAPackage(ROOT + ".service.impl..").and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("ServiceImpl")
                .check(classes);
    }

    /** @Configuration yalniz config/ altinda. */
    @Test
    void configurationsLiveInConfigPackage() {
        classes().that().areAnnotatedWith(Configuration.class)
                .should().resideInAPackage(ROOT + ".config..")
                .check(classes);
    }

    /**
     * core → baska core yasak. Yalniz kendi paketi, platform starter'lari, *-api modulleri ve
     * ucuncu taraf kutuphaneler. (Maven enforcer bannedDependencies bunun birincil kontroludur.)
     */
    @Test
    void noOtherCoreDependencies() {
        classes().that().resideInAPackage(ROOT + "..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        ROOT + "..",
                        "com.acme.platform..",
                        "com.acme..api..",
                        "java..", "javax..", "jakarta..", "org..", "com.fasterxml..", "lombok..",
                        "io..", "net..", "reactor..", "kotlin..")
                .because("baska bir *-core'a bagimlilik modul sinirini ihlal eder")
                .check(classes);
    }

    /** Paket dongusu yok. */
    @Test
    void noPackageCycles() {
        slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(classes);
    }

    /** Her @RequestBody parametresi @Valid tasir. */
    @Test
    void requestBodiesAreValidated() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .should(haveValidOnEveryRequestBodyParameter());
        rule.check(classes);
    }

    private static ArchCondition<JavaMethod> haveValidOnEveryRequestBodyParameter() {
        return new ArchCondition<>("have @Valid on every @RequestBody parameter") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method.getParameters().forEach(p -> {
                    boolean body = p.isAnnotatedWith(RequestBody.class);
                    boolean valid = p.isAnnotatedWith(Valid.class);
                    if (body && !valid) {
                        events.add(SimpleConditionEvent.violated(method,
                                method.getFullName() + " parametresi @RequestBody ama @Valid degil"));
                    }
                });
            }
        };
    }

    /** Entity'ler servisler arasi contract olamaz: api paketinden entity'ye referans yok. */
    @Test
    void apiDoesNotSeeEntities() {
        JavaClasses api = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.acme");
        noClasses().that().resideInAPackage("com.acme..api..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".entity..")
                .check(api);
    }
}
```

---

### `tests/ErrorCodeUniquenessTest.java`

```java
package com.acme.order;
import com.acme.platform.core.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tum servislerin ErrorCode enum'lari global olarak tekil ve kendi bloklarinda olmali.
 * Bu test, tum core modulleri classpath'ine alan bir "aggregate" test modulunde (veya platform-core'da
 * test-jar bagimliliklariyla) calisir. Bloklar README'deki tabloyla ayni tutulur.
 *
 * Referans: mikroservis-mimari-referans.md Bolum 7.2.
 */
class ErrorCodeUniquenessTest {

    /** service → [min, max]. README'deki hata kodu bloklariyla birebir. */
    private static final Map<String, int[]> BLOCKS = Map.of(
            "validation", new int[]{90000, 90099},
            "security", new int[]{90100, 90199},
            "system", new int[]{99998, 99999},
            "auth", new int[]{10000, 10999},
            "order", new int[]{11000, 11999},
            "notification", new int[]{16000, 16999},
            "backoffice", new int[]{17000, 17999});

    @Test
    void allErrorCodesAreGloballyUniqueAndInsideTheirBlock() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.acme");

        Map<Integer, String> seen = new HashMap<>();
        List<String> problems = new java.util.ArrayList<>();

        for (JavaClass jc : classes) {
            if (!jc.isEnum() || !jc.isAssignableTo(ErrorCode.class)) continue;
            Class<?> enumClass = jc.reflect();
            for (Object constant : enumClass.getEnumConstants()) {
                ErrorCode code = (ErrorCode) constant;
                String owner = enumClass.getName() + "." + ((Enum<?>) constant).name();
                String prev = seen.putIfAbsent(code.getCode(), owner);
                if (prev != null) {
                    problems.add("cakisma: " + code.getCode() + " → " + prev + " ve " + owner);
                }
                int[] block = BLOCKS.get(code.getService());
                if (block == null) {
                    problems.add("bilinmeyen service: " + code.getService() + " (" + owner + ")");
                } else if (code.getCode() < block[0] || code.getCode() > block[1]) {
                    problems.add("blok disi: " + owner + " = " + code.getCode()
                            + " (beklenen " + block[0] + "–" + block[1] + ")");
                }
                if (code.getMessage() == null || code.getMessage().isBlank() || !code.getMessage().endsWith(".")) {
                    problems.add("mesaj formati: " + owner + " → Ingilizce, nokta ile biten kisa cumle olmali");
                }
            }
        }
        assertThat(problems).as("ErrorCode kurallari").isEmpty();
        assertThat(seen).isNotEmpty();
    }
}
```

---

### `tests/ConfigDriftTest.java`

```java
package com.acme.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

/**
 * Local yml ile deploy config'i arasindaki drift'i yakalar:
 *  1) Guvenlik ve baglanti key'leri (internal-access, rate-limit scope'lari, client base-url'leri)
 *     application-local.yml ve config/<svc>.yml'de AYNI kumeyi olusturmali.
 *  2) config/*.yml icinde kullanilan her ${ENV_VAR} placeholder'i deploy env sablonunda tanimli olmali.
 *  3) Secret key'lerinde literal fallback (${X:deger}) bulunmamali.
 *
 * Yollar modul kokune goredir; deploy sablonu repo kokundedir (../../deploy/prod.env.example).
 * Referans: mikroservis-mimari-referans.md Bolum 15.2, 19.5.
 */
class ConfigDriftTest {

    private static final Path LOCAL = Path.of("src/main/resources/application-local.yml");
    private static final Path SERVICE = Path.of("src/main/resources/config/order.yml");
    /** Repo kokundeki deploy sablonu; kok, `deploy/` klasoru bulunana kadar yukari cikilarak bulunur. */
    private static final Path ENV_TEMPLATE = repoRoot().resolve("deploy/prod.env.example");

    /**
     * Iki dosyada da ayni KEY kumesini olusturmasi gereken prefix'ler (degerler ortama gore farkli olabilir:
     * localhost vs ${ENV}). Ornek: bir rate-limit scope'u local'de tanimli ama deploy'da unutulmus → drift.
     */
    private static final Set<String> MIRRORED_KEY_PREFIXES = Set.of(
            "service-jwt.internal-access",
            "rate-limit.rules",
            "spring.http.serviceclient",
            "services.");

    /** Key + DEGER olarak birebir ayni olmasi gereken guvenlik prefix'leri (allowlist path ve aktorleri). */
    private static final Set<String> MIRRORED_VALUE_PREFIXES = Set.of("service-jwt.internal-access");

    /** Fallback yasak olan secret key parcalari. */
    private static final Pattern SECRET_KEY = Pattern.compile("(secret|password|pass|token|key|credential)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z0-9_]+)(:[^}]*)?}");

    @Test
    void mirroredKeysAreIdenticalBetweenLocalAndDeployConfig() {
        Properties local = load(LOCAL);
        Properties service = load(SERVICE);
        for (String prefix : MIRRORED_KEY_PREFIXES) {
            boolean withValues = MIRRORED_VALUE_PREFIXES.contains(prefix);
            Set<String> l = keysWithPrefix(local, prefix, withValues);
            Set<String> s = keysWithPrefix(service, prefix, withValues);
            assertThat(l).as("prefix '%s': application-local.yml ↔ config/order.yml %s kumesi",
                            prefix, withValues ? "key+deger" : "key")
                    .containsExactlyInAnyOrderElementsOf(s);
        }
    }

    @Test
    void everyPlaceholderInServiceConfigExistsInDeployEnvTemplate() throws IOException {
        Set<String> declared = Files.readAllLines(ENV_TEMPLATE).stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("#") && l.contains("="))
                .map(l -> l.substring(0, l.indexOf('=')).trim())
                .collect(Collectors.toSet());
        Set<String> used = placeholders(Files.readString(SERVICE));
        // /run/secrets ile gelen degerler env degil, config tree'dir; onlar env sablonunda aranmaz.
        used.removeIf(v -> v.startsWith("SECRET_"));
        assertThat(declared).as("deploy env sablonunda eksik degiskenler").containsAll(used);
    }

    @Test
    void secretKeysHaveNoLiteralFallback() {
        Properties service = load(SERVICE);
        Set<String> offenders = new TreeSet<>();
        for (String key : keys(service)) {
            if (!SECRET_KEY.matcher(key).find()) continue;
            Matcher m = PLACEHOLDER.matcher(String.valueOf(service.get(key)));
            while (m.find()) {
                if (m.group(2) != null) offenders.add(key + " = " + m.group());
            }
        }
        assertThat(offenders).as("secret key'lerinde literal fallback yasak (fail-fast)").isEmpty();
    }

    private static Properties load(Path p) {
        YamlPropertiesFactoryBean f = new YamlPropertiesFactoryBean();
        f.setResources(new FileSystemResource(p));
        Properties props = f.getObject();
        assertThat(props).as("yml okunamadi: %s", p).isNotNull();
        return props;
    }

    private static Set<String> keysWithPrefix(Properties props, String prefix, boolean withValues) {
        return keys(props).stream()
                .filter(k -> k.startsWith(prefix))
                // internal-access[0].path gibi listeler icin sirayi degil kumeyi karsilastir
                .map(k -> withValues ? k + "=" + String.valueOf(props.get(k)).replaceAll("\\s+", "") : k)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * TUZAK: Properties.stringPropertyNames() yalniz String degerli girdileri doner; YamlPropertiesFactoryBean
     * sayisal degerleri (limit: 60) Integer olarak koydugu icin o key'ler sessizce kaybolur ve drift testi
     * hic bir sey yakalamaz. Bu yuzden keySet() uzerinden gidilir.
     */
    private static Set<String> keys(Properties props) {
        return props.keySet().stream().map(String::valueOf).collect(Collectors.toCollection(TreeSet::new));
    }

    /** Modul dizininden yukari cikarak `deploy/` klasorunu (repo koku) bulur; yoksa test aciklayici hata verir. */
    private static Path repoRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("deploy"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("repo kokunde deploy/ klasoru bulunamadi; ENV_TEMPLATE yolunu ayarla");
    }

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) out.add(m.group(1));
        return out;
    }
}
```

---

# Ek B — Doğrulanmış Boş İskelet (`blueprint/skeleton-example/`)

Spring Boot 4.1.1 + ArchUnit 1.5.1 ile `mvn test` yeşil; 8 kasıtlı ihlal yakalandı. Yeni projede başlangıç noktası.

---

### `skeleton-example/README.md`

### skeleton-example — Doğrulanmış Boş İskelet

`blueprint/tests/*.java` şablonlarının ve enforcer kuralının **gerçekten derlenip çalıştığı** en küçük Maven multi-module projesi. Referans dokümanın (Bölüm 3, 4, 7, 16, 19.5) somut, çalışan karşılığı.

- Spring Boot **4.1.1** BOM, Java 21 (25 ile de uyumlu), ArchUnit 1.5.1, Maven 3.9.11.
- Modüller: `platform-core` (ErrorCode arayüzü, ServiceException), `order-api` (DTO), `order-core` (controller/service/impl/repository/entity/exception/config + testler).
- Config: `application-local.yml`, `config/order.yml`, `deploy/prod.env.example` (drift testi için).

#### Doğrulama sonucu (2026-09-28)

```
mvn -q -B -ntp test   → EXIT 0
ArchitectureRulesTest   8 test  (katman, controller→repository, impl paketi, config/, core→core, döngü, @Valid, api→entity)
ConfigDriftTest         3 test  (key kümeleri, ${ENV} ↔ env şablonu, secret fallback)
ErrorCodeUniquenessTest 1 test  (global tekillik, blok, mesaj formatı)
```

**Negatif doğrulama:** kasıtlı 8 ihlal enjekte edildi (controller→repository, `@Valid`'siz `@RequestBody`, `service/` altında `@Configuration`, `service.impl`'de Impl olmayan sınıf, çakışan + blok dışı + noktasız ErrorCode, local'de olup deploy'da olmayan rate-limit scope'u, `${SECRET_DB_PASSWORD:changeme}` fallback'i) → **8 failure**, hepsi doğru kuralda yakalandı; kaldırılınca yeniden yeşil.

#### Denemede öğrenilen 4 ders (şablonlara işlendi)

1. **Enforcer `bannedDependencies`:** `com.acme:*-core` deseni `platform-core`'u da yakalar. Çözüm: `<includes><include>com.acme:platform-core</include></includes>` — ya da platform modüllerini `-core` ile bitirmemek.
2. **ArchUnit `@ArchTest` + JUnit engine:** Spring Boot 4.1 BOM'un yönettiği JUnit Platform ile ArchUnit'in kendi engine'i **0 test** çalıştırdı; build yeşil göründü ama hiçbir kural kontrol edilmedi. Kurallar düz `@Test` + `rule.check(classes)` olarak yazıldı; engine bağımlılığı yok.
3. **`Properties.stringPropertyNames()` tuzağı:** `YamlPropertiesFactoryBean` sayısal değerleri (`limit: 60`) Integer koyar; `stringPropertyNames()` bu key'leri **sessizce atlar** → drift testi rate-limit scope'larını hiç görmedi. `keySet()` + `String.valueOf` kullanılır.
4. **`layeredArchitecture().withOptionalLayers(true)`:** henüz `readmodel/` veya `outbox/` paketi olmayan yeni serviste "Layer is empty" ihlali üretmemesi için.

Bu dört ders "yeşil build = kural çalışıyor" varsayımının yanlış olabileceğini gösterdi; bu yüzden `proj-release-readiness-review` ve CI, mimari testlerin **test sayısını** da doğrular (0 test = başarısız).

#### Çalıştırma

```bash
cd blueprint/skeleton-example
mvn -q -B -ntp test
```

---

### `skeleton-example/deploy/prod.env.example`

```bash
# deploy env sablonu — degerler CI secret'larindan gelir
DB_HOST=
DB_PORT=5432
DB_NAME=app
INVENTORY_URL=http://inventory:8086
```

---

### `skeleton-example/order-api/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
  <parent><groupId>com.acme</groupId><artifactId>skeleton</artifactId><version>${revision}</version></parent>
  <artifactId>order-api</artifactId>
  <dependencies><dependency><groupId>jakarta.validation</groupId><artifactId>jakarta.validation-api</artifactId></dependency></dependencies>
</project>
```

---

### `skeleton-example/order-api/src/main/java/com/acme/order/api/dto/CreateOrderRequest.java`

```java
package com.acme.order.api.dto;
import jakarta.validation.constraints.NotBlank;
public record CreateOrderRequest(@NotBlank String sku, int quantity) {}
```

---

### `skeleton-example/order-core/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
  <parent><groupId>com.acme</groupId><artifactId>skeleton</artifactId><version>${revision}</version></parent>
  <artifactId>order-core</artifactId>
  <dependencies>
    <dependency><groupId>com.acme</groupId><artifactId>platform-core</artifactId><version>${revision}</version></dependency>
    <dependency><groupId>com.acme</groupId><artifactId>order-api</artifactId><version>${revision}</version></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webmvc</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>com.tngtech.archunit</groupId><artifactId>archunit-junit5</artifactId><scope>test</scope></dependency>
  </dependencies>
</project>
```

---

### `skeleton-example/order-core/src/main/java/com/acme/order/OrderApp.java`

```java
package com.acme.order;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class OrderApp { public static void main(String[] a) { SpringApplication.run(OrderApp.class, a); } }
```

---

### `skeleton-example/order-core/src/main/java/com/acme/order/config/WebConfig.java`

```java
package com.acme.order.config;
import org.springframework.context.annotation.Configuration;
@Configuration
public class WebConfig {}
```

---

### `skeleton-example/order-core/src/main/java/com/acme/order/controller/OrderController.java`

```java
package com.acme.order.controller;
import com.acme.order.api.dto.CreateOrderRequest;
import com.acme.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/v1/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<UUID> create(@RequestHeader("X-Idempotency-Key") UUID key, @Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(service.create(UUID.randomUUID(), key, req));
    }
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable("orderId") UUID orderId) { service.cancel(UUID.randomUUID(), orderId); return ResponseEntity.noContent().build(); }
}
```

---

### `skeleton-example/order-core/src/main/java/com/acme/order/entity/Order.java`

```java
package com.acme.order.entity;
import java.util.UUID;
public class Order { private UUID id; private String sku; public UUID getId() { return id; } public String getSku() { return sku; } }
```

---

### `skeleton-example/order-core/src/main/java/com/acme/order/exception/ErrorCode.java`

```java
package com.acme.order.exception;
import org.springframework.http.HttpStatus;
public enum ErrorCode implements com.acme.platform.core.ErrorCode {
    ORDER_NOT_FOUND(11001, "Order not found.", HttpStatus.NOT_FOUND),
    ORDER_NOT_CANCELLABLE(11002, "Order cannot be cancelled.", HttpStatus.CONFLICT);
    private final int code; private final String message; private final HttpStatus httpStatus;
    ErrorCode(int c, String m, HttpStatus s) { code = c; message = m; httpStatus = s; }
    public int getCode() { return code; } public String getMessage() { return message; }
    public String getService() { return "order"; } public HttpStatus getHttpStatus() { return httpStatus; }
}
```

---

### `skeleton-example/order-core/src/main/java/com/acme/order/repository/OrderRepository.java`

```java
package com.acme.order.repository;
import com.acme.order.entity.Order;
import java.util.Optional; import java.util.UUID;
import org.springframework.stereotype.Repository;
@Repository
public class OrderRepository { public Optional<Order> findById(UUID id) { return Optional.empty(); } }
```

---

### `skeleton-example/order-core/src/main/java/com/acme/order/service/OrderService.java`

```java
package com.acme.order.service;
import com.acme.order.api.dto.CreateOrderRequest;
import java.util.UUID;
public interface OrderService { UUID create(UUID accountId, UUID idempotencyKey, CreateOrderRequest req); void cancel(UUID accountId, UUID orderId); }
```

---

### `skeleton-example/order-core/src/main/java/com/acme/order/service/impl/OrderServiceImpl.java`

```java
package com.acme.order.service.impl;
import com.acme.order.api.dto.CreateOrderRequest;
import com.acme.order.exception.ErrorCode;
import com.acme.order.repository.OrderRepository;
import com.acme.order.service.OrderService;
import com.acme.platform.core.ServiceException;
import java.util.UUID;
import org.springframework.stereotype.Service;
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository repo;
    public OrderServiceImpl(OrderRepository repo) { this.repo = repo; }
    public UUID create(UUID a, UUID k, CreateOrderRequest r) { return UUID.randomUUID(); }
    public void cancel(UUID a, UUID id) { repo.findById(id).orElseThrow(() -> new ServiceException(ErrorCode.ORDER_NOT_FOUND)); }
}
```

---

### `skeleton-example/order-core/src/main/resources/application-local.yml`

```yaml
service-jwt:
  internal-access:
    - path: "/internal/orders/*/status"
      allowed-actors: [ "payment-service" ]
    - path: "/internal/orders/**"
      allowed-actors: [ "backoffice-service" ]
rate-limit:
  rules:
    order-create-account: { limit: 60, window-seconds: 60 }
spring:
  http:
    serviceclient:
      inventory: { base-url: "http://localhost:8086", connect-timeout: 2s, read-timeout: 5s }
services:
  inventory.base-url: http://localhost:8086
```

---

### `skeleton-example/order-core/src/main/resources/config/order.yml`

```yaml
service-jwt:
  private-key-path: ${SECRET_ORDER_SIGNING_KEY_PATH}
  internal-access:
    - path: "/internal/orders/*/status"
      allowed-actors: [ "payment-service" ]
    - path: "/internal/orders/**"
      allowed-actors: [ "backoffice-service" ]
rate-limit:
  rules:
    order-create-account: { limit: 60, window-seconds: 60 }
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?currentSchema=order
    password: ${SECRET_DB_PASSWORD}
  http:
    serviceclient:
      inventory: { base-url: "${INVENTORY_URL}", connect-timeout: 2s, read-timeout: 5s }
services:
  inventory.base-url: ${INVENTORY_URL}
```

---

### `skeleton-example/order-core/src/test/java/com/acme/order/ArchitectureRulesTest.java`

```java
package com.acme.order;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mimari kurallarin makine zorlamasi. Her core modulde bu sinifin bir kopyasi bulunur;
 * yalniz ROOT (paket koku) degisir. Referans: mikroservis-mimari-referans.md Bolum 4, 16, 19.5.
 *
 * Bagimlilik: com.tngtech.archunit:archunit-junit5 (test scope) — kurallar duz JUnit @Test olarak
 * calisir; ArchUnit'in kendi JUnit engine'ine (@ArchTest) bagimli DEGILDIR. Neden: engine, JUnit
 * Platform major surumleriyle uyumsuz kalabiliyor ve testler sessizce "0 test" olarak gecebiliyor.
 * Spring Boot 4.1 + ArchUnit 1.5.1 ile dogrulandi (bos iskelette kasitli ihlaller yakalandi).
 */
class ArchitectureRulesTest {

    static final String ROOT = "com.acme.order";
    static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    /** Katmanlar: controller → service → repository. Controller repository'ye dokunamaz. */
    @Test
    void layersAreRespected() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true) // readmodel/outbox paketi henuz yoksa "Layer is empty" ihlali uretmesin
                .layer("Controller").definedBy(ROOT + ".controller..")
                .layer("Service").definedBy(ROOT + ".service..")
                .layer("Repository").definedBy(ROOT + ".repository..")
                .layer("ReadModel").definedBy(ROOT + ".readmodel..")
                .layer("Outbox").definedBy(ROOT + ".outbox..", ROOT + ".worker..", ROOT + ".saga..")
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "ReadModel", "Outbox")
                .check(classes);
    }

    @Test
    void controllersDoNotUseRepositoriesOrEntities() {
        noClasses().that().resideInAPackage(ROOT + ".controller..")
                .should().dependOnClassesThat().resideInAnyPackage(ROOT + ".repository..", ROOT + ".entity..")
                .because("controller ince katmandir; mapping ve veri erisimi serviste yapilir")
                .check(classes);
    }

    /** service.impl altinda yalniz *ServiceImpl bulunur. */
    @Test
    void implPackageOnlyHoldsServiceImpls() {
        classes().that().resideInAPackage(ROOT + ".service.impl..").and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("ServiceImpl")
                .check(classes);
    }

    /** @Configuration yalniz config/ altinda. */
    @Test
    void configurationsLiveInConfigPackage() {
        classes().that().areAnnotatedWith(Configuration.class)
                .should().resideInAPackage(ROOT + ".config..")
                .check(classes);
    }

    /**
     * core → baska core yasak. Yalniz kendi paketi, platform starter'lari, *-api modulleri ve
     * ucuncu taraf kutuphaneler. (Maven enforcer bannedDependencies bunun birincil kontroludur.)
     */
    @Test
    void noOtherCoreDependencies() {
        classes().that().resideInAPackage(ROOT + "..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        ROOT + "..",
                        "com.acme.platform..",
                        "com.acme..api..",
                        "java..", "javax..", "jakarta..", "org..", "com.fasterxml..", "lombok..",
                        "io..", "net..", "reactor..", "kotlin..")
                .because("baska bir *-core'a bagimlilik modul sinirini ihlal eder")
                .check(classes);
    }

    /** Paket dongusu yok. */
    @Test
    void noPackageCycles() {
        slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(classes);
    }

    /** Her @RequestBody parametresi @Valid tasir. */
    @Test
    void requestBodiesAreValidated() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .should(haveValidOnEveryRequestBodyParameter());
        rule.check(classes);
    }

    private static ArchCondition<JavaMethod> haveValidOnEveryRequestBodyParameter() {
        return new ArchCondition<>("have @Valid on every @RequestBody parameter") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method.getParameters().forEach(p -> {
                    boolean body = p.isAnnotatedWith(RequestBody.class);
                    boolean valid = p.isAnnotatedWith(Valid.class);
                    if (body && !valid) {
                        events.add(SimpleConditionEvent.violated(method,
                                method.getFullName() + " parametresi @RequestBody ama @Valid degil"));
                    }
                });
            }
        };
    }

    /** Entity'ler servisler arasi contract olamaz: api paketinden entity'ye referans yok. */
    @Test
    void apiDoesNotSeeEntities() {
        JavaClasses api = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.acme");
        noClasses().that().resideInAPackage("com.acme..api..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".entity..")
                .check(api);
    }
}
```

---

### `skeleton-example/order-core/src/test/java/com/acme/order/ConfigDriftTest.java`

```java
package com.acme.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

/**
 * Local yml ile deploy config'i arasindaki drift'i yakalar:
 *  1) Guvenlik ve baglanti key'leri (internal-access, rate-limit scope'lari, client base-url'leri)
 *     application-local.yml ve config/<svc>.yml'de AYNI kumeyi olusturmali.
 *  2) config/*.yml icinde kullanilan her ${ENV_VAR} placeholder'i deploy env sablonunda tanimli olmali.
 *  3) Secret key'lerinde literal fallback (${X:deger}) bulunmamali.
 *
 * Yollar modul kokune goredir; deploy sablonu repo kokundedir (../../deploy/prod.env.example).
 * Referans: mikroservis-mimari-referans.md Bolum 15.2, 19.5.
 */
class ConfigDriftTest {

    private static final Path LOCAL = Path.of("src/main/resources/application-local.yml");
    private static final Path SERVICE = Path.of("src/main/resources/config/order.yml");
    /** Repo kokundeki deploy sablonu; kok, `deploy/` klasoru bulunana kadar yukari cikilarak bulunur. */
    private static final Path ENV_TEMPLATE = repoRoot().resolve("deploy/prod.env.example");

    /**
     * Iki dosyada da ayni KEY kumesini olusturmasi gereken prefix'ler (degerler ortama gore farkli olabilir:
     * localhost vs ${ENV}). Ornek: bir rate-limit scope'u local'de tanimli ama deploy'da unutulmus → drift.
     */
    private static final Set<String> MIRRORED_KEY_PREFIXES = Set.of(
            "service-jwt.internal-access",
            "rate-limit.rules",
            "spring.http.serviceclient",
            "services.");

    /** Key + DEGER olarak birebir ayni olmasi gereken guvenlik prefix'leri (allowlist path ve aktorleri). */
    private static final Set<String> MIRRORED_VALUE_PREFIXES = Set.of("service-jwt.internal-access");

    /** Fallback yasak olan secret key parcalari. */
    private static final Pattern SECRET_KEY = Pattern.compile("(secret|password|pass|token|key|credential)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z0-9_]+)(:[^}]*)?}");

    @Test
    void mirroredKeysAreIdenticalBetweenLocalAndDeployConfig() {
        Properties local = load(LOCAL);
        Properties service = load(SERVICE);
        for (String prefix : MIRRORED_KEY_PREFIXES) {
            boolean withValues = MIRRORED_VALUE_PREFIXES.contains(prefix);
            Set<String> l = keysWithPrefix(local, prefix, withValues);
            Set<String> s = keysWithPrefix(service, prefix, withValues);
            assertThat(l).as("prefix '%s': application-local.yml ↔ config/order.yml %s kumesi",
                            prefix, withValues ? "key+deger" : "key")
                    .containsExactlyInAnyOrderElementsOf(s);
        }
    }

    @Test
    void everyPlaceholderInServiceConfigExistsInDeployEnvTemplate() throws IOException {
        Set<String> declared = Files.readAllLines(ENV_TEMPLATE).stream()
                .map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("#") && l.contains("="))
                .map(l -> l.substring(0, l.indexOf('=')).trim())
                .collect(Collectors.toSet());
        Set<String> used = placeholders(Files.readString(SERVICE));
        // /run/secrets ile gelen degerler env degil, config tree'dir; onlar env sablonunda aranmaz.
        used.removeIf(v -> v.startsWith("SECRET_"));
        assertThat(declared).as("deploy env sablonunda eksik degiskenler").containsAll(used);
    }

    @Test
    void secretKeysHaveNoLiteralFallback() {
        Properties service = load(SERVICE);
        Set<String> offenders = new TreeSet<>();
        for (String key : keys(service)) {
            if (!SECRET_KEY.matcher(key).find()) continue;
            Matcher m = PLACEHOLDER.matcher(String.valueOf(service.get(key)));
            while (m.find()) {
                if (m.group(2) != null) offenders.add(key + " = " + m.group());
            }
        }
        assertThat(offenders).as("secret key'lerinde literal fallback yasak (fail-fast)").isEmpty();
    }

    private static Properties load(Path p) {
        YamlPropertiesFactoryBean f = new YamlPropertiesFactoryBean();
        f.setResources(new FileSystemResource(p));
        Properties props = f.getObject();
        assertThat(props).as("yml okunamadi: %s", p).isNotNull();
        return props;
    }

    private static Set<String> keysWithPrefix(Properties props, String prefix, boolean withValues) {
        return keys(props).stream()
                .filter(k -> k.startsWith(prefix))
                // internal-access[0].path gibi listeler icin sirayi degil kumeyi karsilastir
                .map(k -> withValues ? k + "=" + String.valueOf(props.get(k)).replaceAll("\\s+", "") : k)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * TUZAK: Properties.stringPropertyNames() yalniz String degerli girdileri doner; YamlPropertiesFactoryBean
     * sayisal degerleri (limit: 60) Integer olarak koydugu icin o key'ler sessizce kaybolur ve drift testi
     * hic bir sey yakalamaz. Bu yuzden keySet() uzerinden gidilir.
     */
    private static Set<String> keys(Properties props) {
        return props.keySet().stream().map(String::valueOf).collect(Collectors.toCollection(TreeSet::new));
    }

    /** Modul dizininden yukari cikarak `deploy/` klasorunu (repo koku) bulur; yoksa test aciklayici hata verir. */
    private static Path repoRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("deploy"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("repo kokunde deploy/ klasoru bulunamadi; ENV_TEMPLATE yolunu ayarla");
    }

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) out.add(m.group(1));
        return out;
    }
}
```

---

### `skeleton-example/order-core/src/test/java/com/acme/order/ErrorCodeUniquenessTest.java`

```java
package com.acme.order;
import com.acme.platform.core.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tum servislerin ErrorCode enum'lari global olarak tekil ve kendi bloklarinda olmali.
 * Bu test, tum core modulleri classpath'ine alan bir "aggregate" test modulunde (veya platform-core'da
 * test-jar bagimliliklariyla) calisir. Bloklar README'deki tabloyla ayni tutulur.
 *
 * Referans: mikroservis-mimari-referans.md Bolum 7.2.
 */
class ErrorCodeUniquenessTest {

    /** service → [min, max]. README'deki hata kodu bloklariyla birebir. */
    private static final Map<String, int[]> BLOCKS = Map.of(
            "validation", new int[]{90000, 90099},
            "security", new int[]{90100, 90199},
            "system", new int[]{99998, 99999},
            "auth", new int[]{10000, 10999},
            "order", new int[]{11000, 11999},
            "notification", new int[]{16000, 16999},
            "backoffice", new int[]{17000, 17999});

    @Test
    void allErrorCodesAreGloballyUniqueAndInsideTheirBlock() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.acme");

        Map<Integer, String> seen = new HashMap<>();
        List<String> problems = new java.util.ArrayList<>();

        for (JavaClass jc : classes) {
            if (!jc.isEnum() || !jc.isAssignableTo(ErrorCode.class)) continue;
            Class<?> enumClass = jc.reflect();
            for (Object constant : enumClass.getEnumConstants()) {
                ErrorCode code = (ErrorCode) constant;
                String owner = enumClass.getName() + "." + ((Enum<?>) constant).name();
                String prev = seen.putIfAbsent(code.getCode(), owner);
                if (prev != null) {
                    problems.add("cakisma: " + code.getCode() + " → " + prev + " ve " + owner);
                }
                int[] block = BLOCKS.get(code.getService());
                if (block == null) {
                    problems.add("bilinmeyen service: " + code.getService() + " (" + owner + ")");
                } else if (code.getCode() < block[0] || code.getCode() > block[1]) {
                    problems.add("blok disi: " + owner + " = " + code.getCode()
                            + " (beklenen " + block[0] + "–" + block[1] + ")");
                }
                if (code.getMessage() == null || code.getMessage().isBlank() || !code.getMessage().endsWith(".")) {
                    problems.add("mesaj formati: " + owner + " → Ingilizce, nokta ile biten kisa cumle olmali");
                }
            }
        }
        assertThat(problems).as("ErrorCode kurallari").isEmpty();
        assertThat(seen).isNotEmpty();
    }
}
```

---

### `skeleton-example/platform-core/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
  <parent><groupId>com.acme</groupId><artifactId>skeleton</artifactId><version>${revision}</version></parent>
  <artifactId>platform-core</artifactId>
  <dependencies><dependency><groupId>org.springframework</groupId><artifactId>spring-web</artifactId></dependency></dependencies>
</project>
```

---

### `skeleton-example/platform-core/src/main/java/com/acme/platform/core/ErrorCode.java`

```java
package com.acme.platform.core;
import org.springframework.http.HttpStatus;
public interface ErrorCode { int getCode(); String getMessage(); String getService(); HttpStatus getHttpStatus(); }
```

---

### `skeleton-example/platform-core/src/main/java/com/acme/platform/core/ServiceException.java`

```java
package com.acme.platform.core;
public class ServiceException extends RuntimeException {
    private final ErrorCode errorCode;
    public ServiceException(ErrorCode c) { super(c.getMessage()); this.errorCode = c; }
    public ErrorCode getErrorCode() { return errorCode; }
}
```

---

### `skeleton-example/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.acme</groupId><artifactId>skeleton</artifactId><version>${revision}</version><packaging>pom</packaging>
  <properties>
    <revision>0.1.0-SNAPSHOT</revision>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <spring-boot.version>4.1.1</spring-boot.version>
    <archunit.version>1.5.1</archunit.version>
  </properties>
  <modules><module>platform-core</module><module>order-api</module><module>order-core</module></modules>
  <dependencyManagement><dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-dependencies</artifactId><version>${spring-boot.version}</version><type>pom</type><scope>import</scope></dependency>
    <dependency><groupId>com.tngtech.archunit</groupId><artifactId>archunit-junit5</artifactId><version>${archunit.version}</version><scope>test</scope></dependency>
  </dependencies></dependencyManagement>
  <build>
    <plugins>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-enforcer-plugin</artifactId><version>3.5.0</version>
        <executions><execution><id>enforce</id><goals><goal>enforce</goal></goals><configuration><rules>
          <requireJavaVersion><version>[21,)</version></requireJavaVersion>
          <requireMavenVersion><version>[3.9,)</version></requireMavenVersion>
          <!-- core → core yasak: *-core artefaktlari yalniz kendi modulunde bulunur -->
          <bannedDependencies><excludes><exclude>com.acme:*-core</exclude></excludes><includes><include>com.acme:platform-core</include></includes></bannedDependencies>
        </rules></configuration></execution></executions></plugin>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.5.3</version></plugin>
    </plugins>
  </build>
</project>
```
