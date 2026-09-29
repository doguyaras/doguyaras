# Mikroservis Backend Mimari Referansı

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
> **v2.3 (2026-09-29):** Sürüm/tarih/CVE iddiaları internet kaynaklarıyla doğrulandı (Ek B); düzeltmeler: Spring Cloud 2025.1.x Boot 4.0 **ve** 4.1'i kapsar (2026.0 → Boot 4.2), Node 24 LTS, Config Server için üçüncü CVE (2026-47894), gateway `trusted-proxies` property adı, RabbitMQ 4.3 `khepri_db` ön koşulu. Eklemeler: SSRF ve dışa giden istekler (9.11), rol bazlı PG zaman aşımları ve RLS opsiyonu (10.1), para tipi (10.3), outbox tablo sağlığı (11.2), claim-check (12.2), mutasyon testi (16), JDK 25 JVM ayarları (18.1), GitHub Actions SHA pinleme (18.3), Framework 7 API versiyonlama + Deprecation/Sunset header'ları (20), RFC 9457 notu (7.3), Boot 4.1 özellikleri (2.1).
>
> **Bu revizyon (v2.2):** Dış bir mimari değerlendirmenin (2026-09) bulguları işlendi: kural sınıfları (Bölüm 1.4), sıcak yolda gecikme bütçesi ve kritik akış kaydı (Bölüm 1.2), kaynak başına read-model revizyonu ve tam durum/değişiklik olay sözleşmesi (Bölüm 4.6), `AFTER_COMMIT` ≠ teslim garantisi (Bölüm 4.3), servis kimliği ≠ kullanıcı adına yetki ve delegasyon matrisi (Bölüm 9.2.1), migration/uygulama rol ayrımı ve `baseline-on-migrate` (Bölüm 10.1–10.2), tek outbox'ta iş türü izolasyonu, üretici sıralaması ve inbox atomikliği (Bölüm 11.2–11.3), değişiklik türüne göre rollout sözleşmesi ve uyumluluk matrisi (Bölüm 18.4), doğrulama kapsamı ve kanıt kaydı (Bölüm 19.6), generic outbox'la tutarlı kod şablonları (Bölüm 23.3–23.4), tarihli sürüm eki (Ek A).
>
> **Önceki revizyon (v2.1):** Gerçek bir projenin (9 servis, tek host, tek PostgreSQL) mimari değerlendirmesinden çıkan dersler işlendi: mimari şekil kararı (Bölüm 1.1), uygulama profilleri MVP/Büyüme/Ölçek (Bölüm 1.3), sıcak yolda senkron zincir yasağı ve read-model replikasyonu (Bölüm 4.6), dayanıklılık (Bölüm 4.7), domain event akışı ve Kafka karar kriterleri (Bölüm 12), asimetrik servis kimliği (Bölüm 9.2), güvenlik süreci (Bölüm 9.10), secret yönetimi (Bölüm 15.3), yedekleme/HA (Bölüm 10.5), Alloy/structured logging/SLO ve runbook/olay yönetimi (Bölüm 8), ölçek eşikleri (Bölüm 24), sürüm takibi (Bölüm 25), proje başlangıç checklist'i (Bölüm 21.0) ve **kopyalanabilir `blueprint/` klasörü** (AGENTS.md, `docs/ai/*`, 12 skill, hook'lar, script'ler, ArchUnit/ErrorCode/config-drift testleri — Bölüm 19).
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
27. [Kanıt Haritası: Hangi İddia Nasıl Doğrulandı](#27-kanıt-haritası-hangi-iddia-nasıl-doğrulandı)
- [Ek A — Sürüm Notları (tarihli anlık görüntü)](#ek-a--sürüm-notları-tarihli-anlık-görüntü)
- [Ek B — Doğrulama Kaynakları](#ek-b--doğrulama-kaynakları-2026-09-29)

Ek dosyalar: `blueprint/` (kopyalanabilir AGENTS.md, `docs/ai/*`, 12 skill, hook'lar, script'ler, test şablonları) · `docs/mikroservis-blueprint-dosyalari.md` (aynı içerik tek dosyada).

---

## 1. Mimari Özet

| Boyut | Karar |
|---|---|
| Stil | Domain sınırları `*-api` (contract) + `*-core` (uygulama) modül çiftleriyle çizilir; tek Maven multi-module repo (monorepo). Bu sınırlar **hem modüler monolit hem mikroservis** olarak deploy edilebilir; hangisi olacağı Bölüm 1.1'deki karara bağlıdır. |
| Giriş noktası | Spring Cloud Gateway (reaktif). Kullanıcı JWT'sini doğrular ve her istek için kısa ömürlü, **asimetrik imzalı** bir **service JWT** üretir (token exchange). |
| Senkron iletişim | Servis-servis HTTP (OpenFeign veya Spring HTTP Service Clients). Gateway atlanır. Service JWT + actor allowlist ile korunur. Sıcak yolda gecikme bütçesi ve bağımlılık listesi yazılı; varsayılan **en fazla bir uzak senkron çağrı** (Bölüm 1.2, 4.6). |
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
| **A. Modüler monolit** | Ekip ≤ 5 kişi, tek host, ölçek profili homojen, ürün henüz kanıtlanmamış | Aynı `*-api`/`*-core` sınırları tek Spring Boot uygulamasında **Spring Modulith** modülleri olur. Şema/modül ayrımı, `ApplicationModules.verify()` + ArchUnit ile zorlanır. Servisler arası çağrı in-process; **modüller arası** olaylar için Modulith event publication registry: olay kaydı (`event_publication`) iş satırıyla **aynı transaction'da** yazılır, birlikte commit/rollback olur (in-process outbox). Teslim ise en-az-bir-kezdir: Modulith "tamamlandı" bilgisini dinleyicinin transaction'ı commit olduktan **sonra** ayrı bir yazımla işaretler; arada çöküş ya da çalışan bir teslim sırasında yapılan resubmit aynı olayı yeniden teslim eder. Bu yüzden `@ApplicationModuleListener` dinleyicileri de idempotent yazılır (Bölüm 11.3). Uygulama dışına giden her mesaj (notification sağlayıcısı, ayrı bir servis, analytics) yine `outbox_event` + RabbitMQ ile (Bölüm 11.2). Modulith externalization gönderim kaydını `event_publication`'da tutar, `outbox_event`'i kullanmaz; ikisi birlikte kullanılacaksa dışa gönderimin tek kaynağı ADR ile seçilir. Gateway ve service JWT gereksiz. Bu dokümanın kalan bölümleri (sınırlar, outbox, idempotency, güvenlik, veri, log) **aynen** geçerli kalır. |
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

Kullanıcıya doğrudan latency olarak yansıyan her istek ("sıcak yol") için **zorunlu güvence**: akışın gecikme bütçesi, uzak bağımlılıkları, karar verirken kabul edilen veri eskiliği ve bağımlılık düşünce davranışı **yazılıdır** (aşağıdaki kayıt). **Varsayılan tercih** (gerekçeli ADR ile değişebilir): en fazla bir uzak senkron çağrı, o da yazma/rezervasyon türünden; diğer servislerin verisi event ile replike edilmiş local read-model'den veya JWT claim'inden okunur.

"Bir çağrı" mutlak sınır değildir: iki kontrollü, timeout bütçeli çağrı bazı akışlarda daha doğru olabilir. Veri çoğaltmanın da bedeli vardır (güncelleme gecikmesi, yeniden oluşturma, işletim); Microsoft'un CQRS rehberi eventual consistency ve ek karmaşıklığı açıkça değerlendirme konusu yapar. Karar her akış için ayrı verilir ve her **ek** senkron bağımlılık gerekçelendirilir.

**Kritik akış kaydı** (her sıcak yol için `docs/ai/repo-context.md` sıcak yol tablosunda):

| Alan | Örnek |
|---|---|
| Kullanıcının beklediği sonuç ve tamamlanma koşulu | "Sipariş kabul edildi; hak düşüldü" |
| Gecikme bütçesi (p99) | 300 ms |
| Uzak senkron bağımlılıklar ve her birinin gerekçesi | subscription.consume (hak tüketimi yazma; read-model ile yapılamaz) |
| Karar için kabul edilen veri eskiliği | hesap durumu ≤ 60 sn, engel listesi ≤ 30 sn, yasal onay: token ömrü |
| Bağımlılık erişilemezse davranış | subscription: 503 `UPSTREAM_UNAVAILABLE`; read-model satır yok: reddet (fail-closed) |
| Yeniden değerlendirme ölçümü | p99 > bütçe 3 gün, read-model lag > eşik, hata oranı > SLO |

- Yasal onay, hesap durumu gibi nadir değişen ve kullanıcıya bağlı bayraklar **JWT claim'i** olabilir; değişince oturum sürümü artırılır.
- Her uzak çağrının timeout'u, üst istek bütçesinin altındadır; circuit breaker açıkken hızlı ve tanımlı bir hata döner.

Availability aritmetiği: %99,9'luk 5 bileşene bağlı bir istek en iyi ihtimalle ≈ %99,5 (yılda ~44 saat kesinti); 2 bileşene bağlıysa ≈ %99,8. p99 latency ardışık çağrıların p99'larının toplamına yakınsar.

### 1.3 Uygulama Profilleri: MVP / Büyüme / Ölçek

Bu doküman kapsamlıdır; hepsi ilk gün yapılmaz. Üç profil, hangi pratiğin ne zaman **zorunlu** olduğunu söyler. Profil kararı ADR'ye yazılır; bir sonraki profile geçiş Bölüm 24 eşikleriyle tetiklenir.

| Alan | **P0 — MVP** (1–3 kişi, ilk kullanıcılar, tek host) | **P1 — Büyüme** (3–8 kişi, gelir var, düzenleyici görünürlük) | **P2 — Ölçek** (çok ekip / çok host) |
|---|---|---|---|
| Şekil | Modüler monolit (Spring Modulith) veya hibrit | Hibrit; farklı ölçek profilli modüller ayrı servis | Mikroservis; read-model'lerle tam asenkron |
| Sınırlar | `*-api`/`*-core` ayrımı, ArchUnit, enforcer, şema+rol/modül — **ilk gün** | + read-model'ler, sıcak yol tablosu | + kontrat versiyonlama, polyrepo değerlendirmesi |
| Veri | Tek Postgres, roller, **yedek + restore provası**, UUIDv7, retention politikası | + PgBouncer, managed/standby, partition'lı büyük tablolar, exporter'lar | + domain bazlı instance ayrımı, wide-column/arama motoru eşiğe göre |
| Mesajlaşma | Şekil A: Modulith event registry (in-process) + dışa giden mesajlar için generic outbox; Şekil C/B: generic outbox + RabbitMQ QQ. Komut/olay ayrımı ilk günden (envelope ucuz) | + `domain.events` tüketicileri (analytics sink, read-model), streams | + Kafka/Redpanda eşiğe göre, Debezium |
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
    ▲──── HTTP + service JWT (internal-access allowlist; sıcak yol bütçesi yazılı, varsayılan ≤1 çağrı) ────▲
    Outbox ──► domain.events (topic) ──► read-model'ler, analytics sink, notification
    Outbox ──► <servis>.commands (queue) ──► SMS/push/mail komutları

 Altyapı: PostgreSQL (schema+rol/servis, PITR) · Valkey ×2 (security / cache) · RabbitMQ 4 (QQ + streams)
          PostGIS (geo) · S3 (private + signed URL) · [opsiyonel] Config Server (secret'sız)
 Gözlem:  OTLP→Alloy→Tempo · /actuator/prometheus→Prometheus→Alertmanager · JSON stdout→Alloy→Loki · Grafana
```

### 1.4 Kural Sınıfları ve Kural Şablonu

Bu dokümandaki her ifade üç sınıftan birine girer; aynı kesinlikle okunmamalıdır:

| Sınıf | Anlamı | Örnekler | Nasıl değişir |
|---|---|---|---|
| **Zorunlu güvence** | Her profilde, her akışta sağlanır; ihlali `BLOCK` | Yetkisiz veri erişimi engellenir; tekrar teslim çift iş üretmez; base migration değişmez; secret düz metin tutulmaz; yedek + restore provası | Değişmez |
| **Varsayılan tercih** | Aksi gerekçelendirilmedikçe uygulanır | Polling outbox; modüler monolit/hibrit başlangıç; sıcak yolda ≤1 uzak çağrı; tek generic outbox; RabbitMQ-önce | Gerekçeli ADR ile |
| **Başlangıç ayarı** | Ölçümle değiştirilecek sayılar | Batch 50, lease 120 sn, backoff `min(600, 30·2^n)`, timeout 2/5 sn, cache 5 sn, T (staleness) değerleri | Yük testi ve metrikle |

Önemli kurallar mümkün olduğunca şu beş alanla yazılır: **Gerekçe** (hangi hatayı önler) · **Uygulanma koşulu** (profil/akış) · **İstisna** (hangi koşulda farklı karar) · **Doğrulama** (hangi test/işletim kanıtı) · **Yeniden değerlendirme** (hangi ölçüm kararı açar). Bir kural bu alanlardan "doğrulama"yı veremiyorsa kural değil dilektir (Bölüm 19.5).

---

## 2. Teknoloji Yığını

### 2.1 Backend

Kural: **Yalnız OSS desteği süren sürümle başlanır** (Bölüm 25). Aşağıdaki sürümler 2026-09 itibarıyla güncel OSS hatlarıdır.

| Katman | Seçim | Not |
|---|---|---|
| Dil | Java 25 LTS (2025-09) | Virtual thread pinning düzeltmesi (JEP 491, JDK 24) dahil; **Scoped Values** final (JEP 506) — istek bağlamı için `ThreadLocal` yerine tercih edilir (virtual thread'lerde ucuz ve sızmaz); Compact Object Headers final (JEP 519, heap %10–20 küçülür); AOT cache + method profilleri (JEP 514/515) açılış süresini kısaltır (Bölüm 18.1). Structured Concurrency hâlâ preview: production'da kullanılmaz. |
| Framework | Spring Boot 4.x (4.1 önerilir; 4.0 OSS 2026-12-31'de biter) | BOM import ile (`spring-boot-dependencies`). Spring Framework 7, Jackson 3, Hibernate ORM 7, modüler starter'lar (`spring-boot-starter-webmvc`, `-flyway`, `-<tech>-test`). 4.1 ile gelenler ve bu dokümanda kullanımı: `InetAddressFilter` (SSRF; Bölüm 9.11), `spring.datasource.connection-fetch=lazy` (bağlantı yalnız ilk SQL'de alınır; havuz baskısı düşer), `@Async` bağlam yayılımı, `spring-boot-starter-opentelemetry` iyileştirmeleri, Spring gRPC starter (bu referansta iç iletişim HTTP kalır). 3.x hattının OSS desteği 2026-06-30'da bitti. |
| Cloud | Spring Cloud **2025.1.x** (Oakwood; Boot 4.0 **ve** 4.1 — 4.1 uyumu 2025.1.2'den itibaren) | BOM import. 2026.0.x (Paddington) Boot 4.2 hattını hedefler ve 2026-09'da milestone aşamasındadır; GA olmadan kullanılmaz. Gateway artefaktı `spring-cloud-starter-gateway-server-webflux`, property prefix'i `spring.cloud.gateway.server.webflux.*`. |
| Build | Maven ≥ 3.9 (multi-module) + gitflow-incremental-builder veya Maven Build Cache Extension | Affected-module CI (Bölüm 3.2). `${revision}` ile CI-friendly versiyon. |
| Web | Spring MVC + virtual threads (`spring.threads.virtual.enabled=true`) | Core servisler |
| Gateway | Spring Cloud Gateway Server WebFlux | `RequestRateLimiter` (Redis token bucket) gateway'de |
| Config | Compose `env_file` + Spring config tree; Config Server **opsiyonel** | Bölüm 15 |
| Servis çağrısı | Spring HTTP Service Clients (`@HttpExchange` + `@ImportHttpServices`, `RestClient`) | OpenFeign, Spring Cloud 2022.0'dan beri "feature-complete" (yalnız bugfix); mevcut projelerde kalabilir, yeni projede tercih edilmez. |
| Dayanıklılık | Resilience4j (circuit breaker, bulkhead, time limiter) veya Spring Framework 7 `@Retryable(includes = …, maxRetries = 2, …)`/`@ConcurrencyLimit` (`@EnableResilientMethods` şart; `maxAttempts` niteliği yoktur, `maxRetries` ilk denemeyi saymaz: 2 = toplam 3 deneme; `includes` verilmezse kalıcı hatalar da tekrarlanır) | Bölüm 4.7 |
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
| API doküman | springdoc-openapi **3.x** (Boot 4 hattı; Boot 4.1 için 3.1.x, 3.0.2 de doğrulandı; Boot 3 hattı 2.8.x ile karıştırılmaz), `springdoc-openapi-starter-webmvc-api` (swagger-ui prod'a girmez) | Prod'da kapalı; CI'da OpenAPI çıktısı testte üretilir (varsayılan 3.1; diff gate'i 3.0 çıktısıyla, Bölüm 16) ve istemci client'ı generate edilir (Bölüm 20) |
| Tracing / metrik | Micrometer Tracing OTel bridge + OTLP exporter, Micrometer Prometheus + Actuator | Structured logging (`logging.structured.format.console=ecs`) |
| Test | JUnit 5, Mockito, AssertJ, MockMvc, Logback `ListAppender`, **Testcontainers 2.x** (`@ServiceConnection`), **ArchUnit** | Gerçek DB testleri CI'da koşar |
| Feature flag / deney | OpenFeature SDK + Unleash/GrowthBook veya parametre kataloğunun flag tipi | Bölüm 14.4 |

### 2.2 Frontend (yönetim paneli)

React 19, TypeScript 5 (strict), Vite, MUI 7, react-router, react-hook-form, TanStack Query (server state), notistack, recharts, native `fetch` wrapper, Vitest + Testing Library, ESLint (typescript-eslint, react-hooks) + Prettier, Node **24 LTS** (Active LTS; 22 bakım modunda, EOL 2027-04-30).

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

**Zorunlu derleyici bayrağı:** `spring-boot-starter-parent` yerine BOM import kullanan projelerde `<maven.compiler.parameters>true</maven.compiler.parameters>` açıkça yazılır; Spring Framework 6.1+/7 `@PathVariable`/`@RequestParam` adlarını yansımadan okur, bayrak yoksa çalışma zamanında "Name for argument … not specified" hatası alınır (iskelette yaşandı).

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
| `TransactionSynchronization.afterCommit` / `@TransactionalEventListener(AFTER_COMMIT)` | **Yalnız kaybı tolere edilen** commit-sonrası yan etkiler: realtime pub/sub bildirimi, cache invalidation. `AFTER_COMMIT` bir çalıştırma zamanıdır, teslim garantisi değildir: commit'ten hemen sonra süreç çökerse listener hiç çalışmaz. Kalıcı teslim gereken her iş (arama index'i güncelleme, bildirim, başka servise etki) **outbox** ile yapılır (Bölüm 11.2). |
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

**Çözüm:** Nadir değişen ve karar için gereken veri, sahibi tarafından **event** olarak yayınlanır; tüketici servis kendi şemasında **kaynak başına bir projeksiyon** tutar. Farklı kaynakların revizyonları karşılaştırılamaz (auth revizyonu 100 iken user revizyonu 20 olabilir); bu yüzden tek `revision` kolonlu birleşik tablo **yanlıştır**.

```sql
-- order şemasında; her projeksiyonun TEK sahibi var; kaynak başına revizyon ve tüketim konumu ayrı tutulur.
CREATE TABLE "order".rm_account_status (          -- sahibi: auth; olay: account.status.changed (TAM DURUM)
    account_id      UUID PRIMARY KEY,
    active          BOOLEAN NOT NULL,
    legal_ok        BOOLEAN NOT NULL,
    source_revision BIGINT NOT NULL,              -- auth'un bu hesap için olay sırası; küçük/eşit revizyon yok sayılır
    source_time     TIMESTAMPTZ NOT NULL,         -- olayın kaynaktaki zamanı (son iş değişikliği)
    applied_at      TIMESTAMPTZ NOT NULL          -- tüketicinin uyguladığı an
);
CREATE TABLE "order".rm_block_relation (          -- sahibi: user; olay: user.block.created / user.block.removed (DEĞİŞİKLİK)
    blocker_id UUID NOT NULL, blocked_id UUID NOT NULL,
    source_seq BIGINT NOT NULL,                   -- bilgi amaçlı; sıra takibi bu tabloda YAPILAMAZ (removed satırı siler, seq kaybolur)
    PRIMARY KEY (blocker_id, blocked_id)
);
CREATE TABLE "order".rm_delta_position (          -- delta projeksiyonları için aggregate başına son uygulanan sıra (FOR UPDATE ile kilitlenir)
    source TEXT NOT NULL, aggregate_id UUID NOT NULL, last_seq BIGINT NOT NULL,
    PRIMARY KEY (source, aggregate_id)            -- seq <= last_seq → tekrar (yok say); seq != last_seq+1 → boşluk (dur, sayaç); aksi → uygula + last_seq aynı TX
);
CREATE TABLE "order".rm_consumer_position (       -- tüketim konumu: tazelik buradan ölçülür, satırın yaşından değil
    source TEXT PRIMARY KEY,                      -- 'auth', 'user'
    last_seq BIGINT NOT NULL, last_event_time TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
```

**Olay sözleşmesi (her read-model olayı için yazılı):**

| Alan | Seçenekler | Etkisi |
|---|---|---|
| **Tür** | **Tam durum** (snapshot: olay önceki durumu bütünüyle kapsar) / **Değişiklik** (delta: artış, tek engel ekleme) | Tam durum: "küçük revizyonu atla" güvenlidir. Delta: **hiçbir olay atlanamaz**; sıra boşluğunda uygulama durur ve kaynakla uzlaştırılır (rebuild veya `since` ucu). |
| **Sıralama kapsamı** | aggregate (hesap) başına / kaynak geneli | Revizyon yalnız kapsamı içinde karşılaştırılır; kaynaklar arası karşılaştırma yok |
| **Tekrar teslim** | idempotent uygulama (inbox `(handler, event_id)`) | Aynı olay iki kez gelirse tek etki |
| **Eksik sıra** | delta'da: dur + uzlaştır; snapshot'ta: sonraki snapshot düzeltir | Alarm `readmodel_gap_total` |
| **Silme ve eski olay replay'i** | tombstone olayı / rebuild'de eski olayların sırayla uygulanması | Replay sonunda mevcut durumla aynı sonuç (deterministik) |
| **Rebuild** | stream replay (Bölüm 12.4) veya sahibin keyset `export` ucu | Yeni tüketici sıfırdan kurabilir |

**Tazelik ve karar kuralları:**
- Tazelik, satırın `applied_at`'ından değil **tüketim konumundan** ölçülür: `readmodel_lag_seconds{source} = now − rm_consumer_position.last_event_time` (tüketilen son olayın kaynaktaki zamanı; konum satırı yoksa +Inf ve alarm). Kaynak head seq'ini yayınlıyorsa ek olarak `readmodel_seq_lag{source} = kaynak_head_seq − last_seq` tutulur; iki metrik farklı büyüklüktür, karıştırılmaz. Bir hesabın durumu bir ay değişmemiş olabilir; eski `source_time` gecikme değildir.
- **Sessiz kaynak sorunu:** zaman bazlı lag, kaynak olay yayınlamadığında da büyür ve tamamen güncel bir read-model'de fail-closed tetikler. Bu yüzden her read-model kaynağı periyodik bir `<kaynak>.position.heartbeat` olayı (head seq + zaman) yayınlar; tüketici bunu yalnız konuma işler (projeksiyon değişmez). Heartbeat aralığı en sıkı kararın `maxLag`'ının en az yarısıdır (engel kararı 30 sn ise heartbeat ≤ 15 sn). Heartbeat yoksa bu kabul yazılır.
- Her karar için "**en fazla ne kadar eski bilgiyle verilebilir**" ayrı cevaplanır: profil görselinin gecikmesi ile engelleme kararının gecikmesi aynı risk değildir. Örnek: engel kararı lag ≤ 30 sn ister; aşılırsa fail-closed (kaynağa sor veya reddet); hesap aktiflik bayrağı lag ≤ 5 dk tolere eder.
- Satır yoksa davranış yazılıdır (varsayılan: fail-closed).
- Read-model **karar** verdirir ama **kaynak** değildir; dışa açılmaz; başka servis okumaz.
- **JWT claim alternatifi:** Kullanıcıya bağlı, nadir değişen bayraklar (`legal_ok`, `legal_rev`, `tier`) user JWT'de taşınır; değişince oturum sürümü (`sv`) artırılır → token yenilenir → claim güncellenir. Kural: claim'ler yetki **sinyali**dir, kaynak DB'yi değiştirmez; token ömrü kadar eskilik kabul edilmiş demektir.
- Yazma niteliğindeki kontrol (hak tüketimi, stok rezervasyonu) read-model'den yapılamaz; senkron kalır ve saga ile korunur (Bölüm 11.4).

**Doğrulama:** delta olayı sıra boşluğunda uygulanmıyor ve sayaç artıyor; snapshot'ta küçük/eşit revizyon yok sayılıyor; snapshot olayları karışık sırada teslim edilince son durum ve konum aynı (konum `GREATEST` ile ilerler, geri gitmez); duplicate tek etki; rebuild aynı export ile iki kez aynı satırları veriyor ve export'ta olmayan eski satırı siliyor; lag konumdan ölçülüyor (30 gün eski satır + taze konum ⇒ ALLOW); "satır yok" ve "konum yok" fail-closed; inbox TX'i içindeki çağrı rollback olunca ne projeksiyon ne konum değişiyor. **Çalışan hali (seviye 2):** `platform-messaging/readmodel` + `ReadModelBehaviourIT` (15 test, 6 mutasyon yakalandı; "uygula-sonra-kontrol-et" mutasyonu DB durumuyla görünmez — sayaç/spy projeksiyonla yakalanır).

**Eşik:** Sıcak yoldaki her istek için kritik akış kaydı (Bölüm 1.2) README/`repo-context.md`'de; varsayılanı aşan her ek senkron bağımlılık ADR ister.

### 4.7 Dayanıklılık: Timeout, Circuit Breaker, Bulkhead

Sıra (Nygard, *Release It!*): önce **her hop'ta sert timeout**, sonra **bağımlılık başına bulkhead**, sonra **circuit breaker**; retry yalnız bütçeyle ve yalnız idempotent çağrılarda.

| Katman | Kural |
|---|---|
| Gateway | İstek başına toplam bütçe (örn. 3 sn). Downstream timeout'ları bunun altında. |
| HTTP client | Timeout'lar gateway bütçesinden türetilir: senkron zincirdeki `connect + read` toplamı gateway bütçesinden **küçük** olmalı (bütçe 3 sn ise tek hop için connect 0,5 sn / read 1,5 sn; iskelette yavaş katılımcı 1,5 sn'de 503 aldı). connect 2 / read 5 sn yalnız arka plan/worker client'ları içindir. Hedef başına `resilience4j.circuitbreaker.instances.<hedef>` + `bulkhead` (semaphore). Sıra `CircuitBreaker(Bulkhead(http))`: circuit açıkken bulkhead izni alınmaz. `record-exceptions` yalnız belirsiz/erişilemez sonucu (timeout, IO, 5xx) içerir; 4xx iş kararları ve `BulkheadFullException` `ignore-exceptions`'a yazılır (Resilience4j listede olmayanı başarı sayar; yazılmazsa iş cevapları hata oranını sulandırır ya da kendi yük kısıtımız circuit'i açar). `bulkhead.max-wait-duration: 0` açıkça yazılır. Circuit açıkken tanımlı `ServiceException` (503/`UPSTREAM_UNAVAILABLE`) döner, thread bloke olmaz. |
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
| Rate limit | `rate-limit.rules.<scope>.{limit,window-seconds,fail-policy}`; scope kebab-case (`login-ip`, `order-create-account`) |
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
- Yeni enum değeri veya event tipi eklenirken **tüketici önce deploy edilir**; tam sözleşme Bölüm 18.4.
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
  connect-timeout: 500ms        # connect + read < gateway bütçesi (3 sn); worker client'ları için 2s / 5s
  read-timeout: 1500ms
resilience4j.circuitbreaker.instances.inventory:
  { failure-rate-threshold: 50, wait-duration-in-open-state: 20s, sliding-window-size: 20,
    record-exceptions: [java.io.IOException, org.springframework.web.client.HttpServerErrorException],
    ignore-exceptions: [com.acme.platform.core.ServiceException, io.github.resilience4j.bulkhead.BulkheadFullException] }
resilience4j.bulkhead.instances.inventory: { max-concurrent-calls: 25, max-wait-duration: 0 }
```

Client grubu için ortak `ClientHttpRequestInterceptor` / Feign `RequestInterceptor` şunları sağlar:
- **Kimlik:** `aud` = hedefin audience'ı, `iss` = bu servis, `sub` = `X-Subject-Id`; imza bu servisin **kendi** private key'iyle (Bölüm 9.2); ardından header silinir.
- **Hata çevirisi:** Upstream 4xx → `ServiceException` (status korunur, gövde okunmaz ve loglanmaz); 5xx/timeout → 502/503 `UPSTREAM_*`.
- **Dayanıklılık:** circuit breaker + bulkhead (Bölüm 4.7). Retry **yok**; retry outbox ve saga worker'larındadır.
- **Tracing:** W3C header'ları otomatik, ama **yalnız** client Boot'un enjekte ettiği `RestClient.Builder` (veya `@ImportHttpServices`) ile kurulduysa: `RestClient.builder()`/`RestClient.create()` observation customizer'ını atlar ve `traceparent` göndermez (iskelette mutasyonla gösterildi).
- **Arka plan çağrısında kimlik:** worker'da HTTP isteği yoktur; interceptor'ın varsayılan "mevcut istekteki hesap" kaynağı boş kalır ve katılımcı `REQUIRED` delegasyonda 403 döner (her saga `MANUAL_REVIEW`'e düşer). Worker `sub`'ı yalnız kendi akışının kalıcı kaydından (saga/outbox satırı) aktarır; client `sub`'ı çağrı parametresinden alır.

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
| `MissingApiVersionException`, `InvalidApiVersionException` | 400 | validation (`API_VERSION_INVALID`) | WARN |
| `Exception` | 500 | system | ERROR; `exceptionType=` + sanitize edilmiş root-cause özeti. Ham throwable log olayına **eklenmez**: stack trace mesaj zincirini (PG DETAIL, URL, token, telefon) taşır (Bölüm 8.4). Stack trace gerekiyorsa mesajları sanitize eden bir throwable converter ile yazılır. |

Her yanıta `X-Trace-Id` header'ı eklenir. `traceId` aktif span'den alınır.

**RFC 9457 (Problem Details) notu:** Spring `ProblemDetail` (`application/problem+json`) standarttır ve dış/ortak API'lerde tercih edilebilir; bu referans mobil istemci ve panelin **tek** zarfı için kendi `ErrorResponse`'unu kullanır. Karar ADR'ye yazılır; ikisi karıştırılmaz (filtre redleri dahil tek biçim).

**Kaçın:** Standart MVC hatalarının 500'e düşmesi. Handler `ResponseEntityExceptionHandler`'dan türetilir ve tek zarf `handleExceptionInternal` override'ında üretilir (400/404/405/406/413/415 ve API version hataları tek noktadan geçer; türetme kaldırılınca 17 test 500 gördü). `ConstraintViolationException` taban sınıfta **yoktur**; `@ExceptionHandler` ile ayrıca eşlenir. Bilinmeyen path Boot 4'te `NoResourceFoundException` → 404.

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

- Boot 4: **`spring-boot-starter-opentelemetry`** (OTel API + Micrometer tracing bridge + metrik ve trace için OTLP exporter'lar; ayarlar `management.*` altında, `otel.*` değil). Ayrı ayrı `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` eklemek 3.x kalıntısıdır.
- `propagation.type: W3C`, `spring.reactor.context-propagation: auto`, HTTP client micrometer entegrasyonu açık (client'lar Boot'un `RestClient.Builder` bean'inden kurulur, Bölüm 6.8). Test: katılımcının aldığı `traceparent` W3C biçiminde (`00-<32hex>-<16hex>-<2hex>`; flags `01` veya `03` olabilir) ve trace-id çağırana gelenle aynıdır.
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
| Kütüphane notu | Nimbus JOSE+JWT 10.x'in `Ed25519Signer/Verifier` sınıfları **opsiyonel** `com.google.crypto.tink` bağımlılığını çalışma zamanında ister (derleme geçer, ilk imzada `NoClassDefFoundError`). JDK 15+ Ed25519'u yerli destekler: platform-security'deki `JdkEd25519Signer/Verifier` (`Signature.getInstance("Ed25519")`) Tink'siz çalışır. Doğrulayıcı `alg`'ı EdDSA'ya sabitler; `alg`'a göre verifier seçmek (HS256 → MACVerifier) alg-confusion açığıdır (public anahtar byte'larını HMAC secret'i yapan token ile test edilir) | | |
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

### 9.2.1 Servis Kimliği ≠ Kullanıcı Adına İşlem Yetkisi

A servisi kendi anahtarıyla imzalayınca B, token'ın A'dan geldiğini doğrular. Bu, A'nın token'a koyduğu **herhangi bir** `sub` adına işlem yapmaya yetkili olduğunu kanıtlamaz. Kimlik (authentication) ile devredilen yetki (delegation) ayrı ele alınır; RFC 8693'ün *delegation* (aktör zinciri açık: `act`) ile *impersonation* (aktör gizli) ayrımı bu tasarımın temelidir — ev yapımı token biçimi RFC'ye otomatik uyumlu değildir, sadece aynı kavramları kullanır.

**Zorunlu güvence:** Hedef servis üç şeyi birlikte kontrol eder: (1) çağıran kimliği (`iss`/`act`), (2) bu çağıranın bu **işlem** için allowlist'te olması, (3) `sub` varsa bu kullanıcının bu **kaynak** üzerindeki yetkisi (ownership) — ilk ikisi üçüncüyü atlatmaz.

**Delegasyon matrisi** (`docs/ai/repo-context.md`'de tutulur; her internal uç için bir satır):

| Çağıran (`act`) | Hedef işlem | Kullanıcı bağlamı (`sub`) | Kaynak yetkisi kontrolü | Bağlam kaynağı | Ele geçirilirse zarar |
|---|---|---|---|---|---|
| gateway | her public uç | zorunlu (user JWT'den) | hedef: ownership | kullanıcı isteği | tüm kullanıcı işlemleri → gateway en kritik bileşen |
| order-service | `subscription: consume/confirm/compensate/get` | zorunlu; yalnız kendi saga kaydındaki hesap | subscription: `operation_key` + hesap eşleşmesi | consume: kullanıcı isteği (senkron); confirm/compensate/get: recovery worker, `sub` = `saga.account_id` (isteğin `sub`'ı saga kaydında saklanır) | yalnız hak tüketimi; başka işlem yok |
| order-service (worker) | `notification: commands` | opsiyonel | – | arka plan (outbox) | spam gönderimi → rate limit |
| backoffice-service | `user: moderate` | yok (admin adına; admin id ayrı claim) | user: admin rolü + audit | panel isteği | moderasyon kararları |

Kurallar:
- **Kullanıcı isteğiyle çalışan çağrı** (`sub` = isteği yapan) ile **arka plan işi** (`sub` yok veya `on_behalf_of` claim'i ayrı) token'da ayırt edilir; hedef, arka plan token'ıyla kullanıcı-yetkisi gerektiren işlem kabul etmez.
- Bir servis yalnız kendi akışında gördüğü `sub`'ı aktarabilir; "her kullanıcı adına her şey" allowlist satırı **yoktur**.
- `act` = **doğrudan çağıran** servis ve her zaman `iss`'e eşittir (farklıysa 401); doğrulayan `kid`'i yalnız o issuer altında arar. Zincirleme delegasyonda (`A → B → C`) önceki halkalar ayrı bir `via` (dizi) claim'inde taşınır; C allowlist kararını `act` ile verir, `via`'yı audit ve isteğe bağlı ek kısıtlar için kullanır. `via` imzalayanın kendi beyanıdır; güvenlik sınırı `act == iss` + o issuer'ın anahtarıdır. (Doğrulandı: `act == iss` kontrolü kaldırılınca backoffice kendi geçerli anahtarıyla `act=order-service` diyerek delegasyonu geçti.)
- Matris değişince `proj-security-review` ve `proj-architecture-boundary-review` çalışır; testler izinli/izinsiz aktör + yanlış `sub` + arka plan token'ıyla kullanıcı işlemi senaryolarını kapsar.

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
- Gateway `X-Forwarded-*`/`Forwarded` header'larını yalnız güvenilen proxy'den kabul eder: `spring.cloud.gateway.server.webflux.trusted-proxies` (Java regex, örn. `10\.0\.0\..*`); CVE-2025-41235 sonrası düzeltilmiş sürümlerde bu ayar olmadan forwarded header işlevi **kapalıdır** (bilinçli).

### 9.4 `ServiceJwtVerificationFilter`

```
0. Path decode + normalize: filtre `/*`'a bağlı (yalnız `/internal/*` değil; `/v1/../internal/x` görülsün), `getRequestURI()` (ham; servlet path zaten decode edilmiş gelir) bir kez %XX decode + `URI.normalize`. /internal'a dokunup dokunmadığı Spring MVC'nin yönlendirme için gördüğü forma göre sorulur: her segmentteki `;...` path parametreleri atılır ve harf büyüklüğü yok sayılır; böylece `/internal;x/admin`, `/internal%3Bx/admin`, `/v1/..;/internal/x` ve `/INTERNAL/x` de internal sayılır (ilk sürümde `/internal;x/admin` public sayılıp token'sız geçiyordu; bağımsız inceleme yakaladı, test eklendi). Decode yalnız yüzde-kodlamaya göre yapılır (`URLDecoder` `+`'yı boşluğa çevirir, kullanılmaz). /internal'a dokunan istekte '%', '\', NUL, '//', ';', ham `%2F`, '.'/'..' segmenti → 400 `{"code":"INTERNAL_PATH_INVALID"}`; public kurallara ASLA geri düşülmez. Filtre DispatcherServlet'ten önce çalıştığı için 400/401/403 zarfını (`{code, message}`, token/kid sızdırmadan; 401'de `WWW-Authenticate: Bearer`) kendisi yazar.
1. exclude-paths → doğrulama yok
2. X-Service-Auth yok → 401
3. typ / iss (bilinen imzalayıcı) / kid → JWKS'ten public key (yalnız o issuer'ın anahtarları) / imza (alg header'a bakılmaz, EdDSA sabit) / aud (**tam eşitlik**: tek elemanlı `[bu-servis]`; çoklu aud → 401) / exp+nbf (±30 sn, enjekte edilen `Clock`) → 401
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
  jwks-path: /run/config/service-jwks.json                   # issuer'a göre bölümlenmiş: {"gateway":{"keys":[...]},"order-service":{"keys":[...]}}; kid yalnız kendi issuer'ı altında aranır (aksi halde her kayıtlı servis kendi anahtarıyla başka iss taklit eder)
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
local pttl = redis.call('PTTL', KEYS[1])
if pttl < 0 then redis.call('EXPIRE', KEYS[1], ARGV[1]); pttl = tonumber(ARGV[1]) * 1000 end
return { current, pttl }
```

- EXPIRE koşulu `current == maliyet` değil `PTTL < 0`'dır: TTL'siz kalmış bir sayaç (eski sürüm, INCR ile EXPIRE arasında çöken istemci, elle `SET`) bir sonraki vuruşta iyileşir; aksi halde özne sonsuza dek kilitli kalır (gerçek Redis 7'de mutasyonla gösterildi).
- Script `[sayaç, kalan_ms]` döner; `Retry-After = max(1, ceil(kalan_ms/1000))` ek bir round-trip olmadan hesaplanır.
- Script `SCRIPT LOAD` ile yüklenip `EVALSHA` ile çağrılır; `NOSCRIPT` gelirse (Redis restart/failover sonrası script cache boştur) yeniden yüklenir ve bir kez tekrar denenir. Bu yapılmazsa restart sonrası fail-open scope süresiz limitsiz, fail-closed scope süresiz 503 kalır.

- Key formatı `rl:<scope>:<hex(HMAC-SHA256(pepper, scope \0 özne))>`; `pepper` secret store'dan gelir (`SECRET_RATE_LIMIT_PEPPER`), rotasyonda sayaçlar sıfırlanır (kabul edilebilir). Düz `sha256` ham metni gizler ama telefon/IPv4 uzayı küçük olduğu için saniyeler içinde geri çözülür; bu yüzden RDB/AOF yedekleri ve replikalar PII taşıyor sayılır.
- Config: `rate-limit.rules.<scope>.{limit, window-seconds, fail-policy}`; `fail-policy` = `OPEN` | `CLOSED` ve **varsayılanı yoktur**: eksikse uygulama açılışta hata verir (varsayılan ya limitsiz geçiş ya da kesintide tüm yüzeyin 503'ü demektir).
- Rate limit Redis bağlantısı sıcak yoldadır: komut timeout'u kısa (50–250 ms), kopukken komutlar kuyruğa alınmaz, hemen reddedilir (Lettuce `DisconnectedBehavior.REJECT_COMMANDS`), yeniden bağlanma gecikmesi en fazla 1 sn. Lettuce varsayılanında kopukken komutlar timeout'a kadar bekler; fail-open scope bile her istekte o kadar gecikir.
- Fail davranışı:

| Durum | Sonuç |
|---|---|
| Kural tanımlı değil | 503 (fail-closed) — config hatası, üretime çıkmadan yakalanmalı |
| Redis hatası | Scope başına karar: güvenlik yüzeyleri (OTP, login, refresh) **fail-closed 503 + Retry-After** (`rate_limit_fail_closed_total{scope}`); iş yüzeyleri (arama, listeleme) **fail-open** (`rate_limit_fail_open_total{scope}`) ve alarm; sayaçlar açılışta her scope için 0 ile kaydedilir (ilk kesintiyi `increase()` alarmı yakalasın) (Stripe'ın rate limiter'ları bilinçli fail-open'dır) |
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

### 9.11 SSRF ve Dışa Giden İstekler

Kullanıcıdan gelen bir URL'yi sunucunun çağırdığı her yer (webhook adresi, avatar/önizleme URL'si, OIDC discovery, dosya içe aktarma) SSRF yüzeyidir: iç ağ (`10/8`, `172.16/12`, `192.168/16`), link-local metadata (`169.254.169.254`), localhost, config-server ve actuator portları hedef olur.

- **Zorunlu güvence:** kullanıcı kaynaklı URL ile giden her istek (1) şema `https` ve host allowlist/denylist, (2) **DNS çözümü sonrası** IP kontrolü (DNS rebinding'e karşı; Boot 4.1 `InetAddressFilter` (`org.springframework.boot.http.client`) bean'i auto-configured `RestClient.Builder`/`WebClient.Builder` üzerinden bu builder'lardan üretilen **tüm** client'lara uygulanır; engellenen istek `FilteredHostException` ile bağlantı kurulmadan kesilir), (3) redirect takibinde de aynı kontrol, (4) kısa timeout ve boyut sınırı, (5) iç servis client'larıyla izolasyon: bean global olduğu için iç ağ adresine giden client'lar aynı builder'ı kullanıyorsa onlar da kesilir. Ya filtre iç hedefleri açıkça izinler (`externalAddresses().or("10.0.0.0/8", …)`), ya da iç client'lar filtresiz ayrı bir `ClientHttpRequestFactory` ile kurulur; ayrı bir egress bean'i **tek başına** izolasyon sağlamaz (iskelette doğrulandı).
- Webhook hedefleri kayıt anında doğrulanır (challenge) ve değişince yeniden; gönderim outbox `kind=HTTP` lane'inden, imzalı (HMAC) ve yeniden denemeli.
- Config Server ve actuator portları yalnız iç ağda; SSRF'e açık servislerden erişilemez (Bölüm 15.2).
- Test: iç IP'ye, metadata adresine ve DNS ile iç IP'ye çözülen host'a giden istek reddediliyor.

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
- **Zorlama — DB seviyesinde (birincil):** Sınır testle değil, GRANT ile korunur. Bir `nativeQuery` başka şemaya erişmeye kalkarsa çalışma zamanında hata alır. **Migration rolü ile uygulama rolü ayrıdır**: şema sahibi ve DDL yetkisi yalnız migration rolündedir; uygulama çalışma zamanında yalnız tablo/sequence/fonksiyon yetkisiyle çalışır. **Neden:** ele geçirilen uygulama süreci tablo düşürememeli, trigger/`REVOKE` kaldıramamalı, audit tablosunu değiştirememeli.

```sql
-- Altyapı migration'ı (superuser / DBA rolüyle, bir kez):
CREATE ROLE svc_order_migrate LOGIN PASSWORD '<secret>';   -- Flyway bu rolle koşar
CREATE ROLE svc_order         LOGIN PASSWORD '<secret>';   -- uygulama bu rolle çalışır
CREATE SCHEMA "order" AUTHORIZATION svc_order_migrate;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA "order" TO svc_order;
-- Migration rolünün oluşturacağı nesnelerde uygulama rolünün yetkisi otomatik gelsin:
ALTER DEFAULT PRIVILEGES FOR ROLE svc_order_migrate IN SCHEMA "order"
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO svc_order;
ALTER DEFAULT PRIVILEGES FOR ROLE svc_order_migrate IN SCHEMA "order"
    GRANT USAGE, SELECT ON SEQUENCES TO svc_order;
-- Append-only/audit tablolarında uygulama rolünden UPDATE/DELETE ayrıca REVOKE edilir (migration içinde).
-- svc_order başka şemada USAGE yetkisine sahip değildir; cross-schema join çalışmaz; DDL yapamaz.
-- DİKKAT: flyway_schema_history de migration rolü tarafından "order" şemasında yaratılır; yukarıdaki default privilege
-- ona da DML verir (ele geçirilen uygulama history satırını silip sonraki deploy'u bozabilir). Flyway callback'i
-- db/migration/afterMigrate.sql her migrate sonunda geri alır (idempotent, elle yapılan drift'i de onarır):
--   REVOKE ALL ON "${flyway:defaultSchema}"."${flyway:table}" FROM svc_order;
-- Alternatif: history tablosu uygulama rolünün USAGE yetkisi olmayan ayrı bir şemada (spring.flyway.default-schema).
```

  Kesin GRANT listesi uygulamanın **doğrulanmış** erişim ihtiyacından çıkarılır (örn. `pg_stat_statements` ile gözlenen ifadeler); "her ihtimale karşı" yetki verilmez. Spring: `spring.datasource` uygulama rolü, `spring.flyway.user/password` migration rolü.

  **Rol bazlı zaman aşımları** (başlangıç ayarı; global `postgresql.conf` yerine role bağlanır, migration rolü ayrı tutulur):

```sql
ALTER ROLE svc_order SET statement_timeout = '10s';                    -- kaçak sorgu
ALTER ROLE svc_order SET lock_timeout = '3s';                          -- kilit kuyruğu birikmesin
ALTER ROLE svc_order SET idle_in_transaction_session_timeout = '60s'; -- açık unutulan TX kilit/bloat üretmesin
ALTER ROLE svc_order_migrate SET lock_timeout = '10s';                 -- DDL kilidi beklerken üretimi kilitlemesin; statement_timeout yok (backfill)
```

  Uzun raporlama/export sorguları ayrı rol veya `SET LOCAL statement_timeout` ile; sıcak yol sorguları 10 sn'ye yaklaşıyorsa sorun timeout değil sorgudur.

  **Row Level Security (opsiyon, varsayılan değil):** hesap sahipliği için ikinci savunma hattı olarak `ENABLE ROW LEVEL SECURITY` + policy `USING (account_id = NULLIF(current_setting('app.account_id', true), '')::uuid)` kullanılabilir. **Neden bu biçim:** parametre hiç set edilmemişse `current_setting(name)` 42704 hatası verir; `SET LOCAL` yapılan TX bittikten sonra değer NULL değil boş string'dir ve `''::uuid` 22P02 verir — `missing_ok=true` + `NULLIF` ile bağlamsız sorgu hata yerine **0 satır** döndürür (kapalı varsayılan). Bağlam TX içinde `SELECT set_config('app.account_id', ?, true)` ile verilir (`SET LOCAL` literal ister, parametre bağlanamaz); düz `SET` PgBouncer transaction mode'da **başka bir istemcinin** sonraki TX'inde görünür (seviye 3'te gözlendi: 2 satır sızdı) ve Hikari'den dönen bağlantı da bağlamı taşır. Uygulama rolü `BYPASSRLS` değildir, view'ların sahibi superuser değildir, policy'siz tablo = herkese kapalı; tablo sahibi (migration rolü) `FORCE ROW LEVEL SECURITY` yoksa RLS'i atlar (backfill için istenen davranış, yazılır). Sahibi olmayan rolün `GRANT`'ı hata vermez, WARNING ile etkisiz kalır: yetki testi SQLState değil `information_schema.role_table_grants` üzerinden "grant yok" iddiasını doğrular. Uygulama katmanındaki ownership kontrolü (Bölüm 9.7) kalkmaz; RLS onu tamamlar.

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
| İlk dosya | Şema 10.1'deki altyapı migration'ında (`CREATE SCHEMA … AUTHORIZATION svc_<x>_migrate`) yaratılır; `V1__*.sql` `CREATE SCHEMA` içermez, doğrudan ilk tabloyla başlar (`spring.flyway.schemas: <schema>`; history tablosu o şemada, migration rolü sahipliğinde). `CREATE SCHEMA IF NOT EXISTS` yalnız altyapı script'i olmayan tek-rol kurulumlarında. |
| Ayar | `enabled: true`, `locations: classpath:db/migration`, `schemas: <schema>`, `clean-disabled: true`, `user/password` = migration rolü (uygulama datasource'undan ayrı). **`baseline-on-migrate` varsayılan olarak kapalıdır**: Flyway dokümanı bu ayarın, migration'ı yanlış (boş olmayan, yönetilmeyen) bir veritabanına uygulamayı önleyen kontrolü kaldırdığı konusunda uyarır. Mevcut bir DB'yi Flyway yönetimine alma, ayrı ve tek seferlik bir prosedürdür (`flyway baseline` komutu, belgelenmiş `baselineVersion`), config'te sürekli açık bir bayrak değil. |
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
| Para | `BigDecimal` ↔ `NUMERIC(19,4)` **veya** en küçük birim `BIGINT` (kuruş) + `currency CHAR(3)`; `double`/`float` asla; yuvarlama kuralı (`RoundingMode`) domain'de tek yerde |
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
| Bağlantı bütçesi | Her PG bağlantısı bir OS process'i. HikariCP rehberi: havuz ≈ `(çekirdek × 2) + disk`; "daha az bağlantı daha hızlı". N servis × instance × havuz hesabı README'de. **PgBouncer** transaction mode (`default_pool_size` 20) `max_connections`'ı korur. Protokol seviyesi prepared statement'lar 1.21+ ile transaction mode'da desteklenir; `max_prepared_statements` 1.21–1.24.0'da varsayılan **0 (kapalı)**, 1.24.1'den itibaren 200 — sürüme güvenilmez, `pgbouncer.ini`'de **her zaman açıkça** `max_prepared_statements = 200` yazılır (kapalıyken JDBC `prepareThreshold` ile 42P05 "already exists", 26000 "does not exist" ve en tehlikelisi 08P01 / başka istemcinin planıyla yanlış sonuç üretir; 1.22.0'da gözlendi). JDBC sürücüsü için `ignore_startup_parameters = extra_float_digits`. SQL `PREPARE` desteklenmez. Transaction mode kuralları: oturum durumu yok (`SET` yerine `SET LOCAL`/`set_config(…, true)` — `server_reset_query` bu modda çalışmaz, `SET` başka istemcinin TX'ine sızar; advisory lock yalnız `pg_advisory_xact_lock`, `LISTEN/NOTIFY` yok), Hikari `connection-init-sql`/`schema` gibi oturuma bağlı ayarlar kullanılmaz. Boot 4.1 `spring.datasource.connection-fetch=lazy` ile bağlantı yalnız ilk SQL'de alınır. |
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
  priority        SMALLINT NOT NULL DEFAULT 0,          -- yüksek önce; güvenlik kararları > iş olayları > toplu işler
  dead_policy     TEXT NOT NULL DEFAULT 'DEAD_ON_PERMANENT', -- CHECK IN ('DEAD_ON_PERMANENT','NEVER_DEAD'); güvenlik yan etkisi NEVER_DEAD
  retry_count     INT  NOT NULL DEFAULT 0,
  next_retry_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  locked_until    TIMESTAMPTZ,
  claim_token     UUID,
  last_error_code VARCHAR(120),                        -- exception SimpleName / HTTP status (mesaj değil)
  expires_at      TIMESTAMPTZ,                         -- domain TTL'li mesajlarda (OTP)
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_outbox_event_claim ON <schema>.outbox_event (kind, status, next_retry_at, priority DESC, created_at);
```

- `id` deterministik gerekiyorsa (`UUID.nameUUIDFromBytes(kaynak:hedef:faz)`) yazıcı sağlar; tüketici inbox bu id ile dedup yapar.
- **Üretici tarafı sıralama:** Tüketicideki single-active-consumer tek başına sırayı garanti etmez; sıra **yayın anında** bozulabilir (iki poller instance'ı aynı aggregate'in iki satırını paralel claim eder, yeniden deneme eski olayı sonraya atar). Sıra gereken `aggregate_id` için: claim sorgusu aynı aggregate'in satırlarını **tek worker'a** verir ve `created_at` sırasıyla işler; bir satır başarısız olursa aynı aggregate'in sonraki satırları **beklet**ilir (`next_retry_at` ileri alınır, retry sayılmaz). Sıra gerekmeyen olaylarda bu kısıt uygulanmaz (throughput). Tüketici tarafında ayrıca `source_revision` karşılaştırması (Bölüm 4.6) sıra hatasını tolere eder.
- **`claim_token` yalnız poller'ın kendi yazma yarışını çözer** (kirası dolmuş eski worker sonucu ezemez). Uzak hedefe aynı işin iki kez ulaşmasını **engellemez**: hedef idempotent olmak zorundadır (inbox / `Idempotency-Key` / deterministik `id`).
- **Tablo sağlığı:** outbox/inbox yüksek insert+delete churn'ü üretir; varsayılan autovacuum ölçeği (`%20`) yetmez → tablo başına `autovacuum_vacuum_scale_factor = 0.01`, `autovacuum_vacuum_cost_delay` düşük; `n_dead_tup` ve tablo boyutu alarmda. Günde milyon satırı geçince `created_at` partition + `DROP PARTITION` (Bölüm 10.6). İşlenen satırı silmek yerine arşiv tablosuna taşımak yalnız denetim ihtiyacı varsa.
- **Publisher confirm ≠ tüketici işledi.** Confirm yalnız broker'ın mesajı kalıcı aldığını söyler. "İş tamamlandı" bilgisi gerekiyorsa tüketici kendi olayını yayınlar (`notification.delivered`), üretici onu tüketir; senkron RPC'ye dönülmez.

**CDC alternatifi (Debezium):** Polling publisher, Kafka/Connect yoksa "saner default"tır (Richardson) ve 1 sn poll ile ~500 ms p50 gecikme verir. **Debezium Server** (Kafka Connect gerektirmez; Kafka, RabbitMQ streams, Redis Streams, NATS, HTTP sink'leri) ile poller'lar kalkar ve gecikme ~50–200 ms'ye iner; bedeli `wal_level=logical` + replication slot yönetimidir: connector durursa veya yakalanan tablo boştayken diğer tablolar yazarsa **WAL disk'i doldurur** → `heartbeat.interval.ms` + `heartbeat.action.query`, `max_slot_wal_keep_size` ve slot lag alarmı şart. Eşik: Bölüm 24.

**Poller sabitleri** (başlangıç ayarı — Bölüm 1.4; yük testiyle değişir):

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

**Tek outbox içinde iş türü izolasyonu ve öncelik:** Tek tablo, tek kuyruk anlamına gelmez. Yavaş bir HTTP hedefi (SMS sağlayıcısı) event yayınını, ya da 10 000 satırlık bir toplu bildirim tek bir güvenlik kararını **bekletemez**. Kurallar:
- Poller `kind` (ve gerekirse `event_type` grubu) başına **ayrı claim döngüsü ve ayrı worker havuzu** çalıştırır (`<svc>.outbox.lanes: [EVENT, COMMAND, HTTP]`; her lane'in kendi batch/concurrency'si). Bir lane'in hatası diğerini etkilemez.
- `priority SMALLINT NOT NULL DEFAULT 0` kolonu (yüksek önce) ve claim sıralaması `ORDER BY priority DESC, created_at` (index'e eklenir). Güvenlik kararları (ban, engel) en yüksek öncelik.
- Toplu işler (kampanya bildirimi) ayrı bir `event_type` grubu / lane ile sınırlı concurrency'de; tek satırlık kritik işlerle aynı havuzu paylaşmaz.
- Metrikler lane bazında: `outbox_oldest_pending_age_seconds{lane}`, `outbox_pending_total{lane}`.

**Durum senkronu desenleri:**

| Desen | Kullanım |
|---|---|
| Sıra numaralı outbox | Durum senkronizasyonu (örn. hesap durumu). Karar DB sequence'ından (aggregate başına monoton) sıra numarası alır; alıcı küçük/eşit sırayı yok sayar. |
| Superseded kontrolü | Göndermeden önce daha yeni bir kararın olup olmadığı kontrol edilir; varsa satır gönderilmeden silinir. **Zorunlu güvence:** eski bir güvenlik kararı (engel kaldırıldı) yeniden deneme yüzünden yeni kararı (yeniden engellendi) asla ezmez — hem üretici (superseded) hem tüketici (`source_revision`) tarafında korunur. |

Bölüm 23.3–23.4'teki kod şablonları bu generic tabloyu ve lane'li poller'ı gösterir.

**Şekil A'da Modulith registry'nin işletimi:** `event_publication` tablosu uygulamanın kendi Flyway migration'ıdır (Modulith jar'ındaki `schemas/v2/schema-postgresql.sql` birebir kopyalanır; `spring.modulith.events.jdbc.schema-initialization.enabled=false` kalır). `completion-mode=update` ise tamamlanan satırlar `CompletedEventPublications.deletePublicationsOlderThan(...)` ile periyodik silinir. Tamamlanmamış kayıtları tek bir zamanlanmış iş (advisory lock ile tek instance) `IncompleteEventPublications.resubmitIncompletePublicationsOlderThan(eşik)` ile yeniden gönderir; `republish-outstanding-events-on-restart` çoklu instance'ta kapalı tutulur. **Uyarı (Modulith 2.1.1 JDBC):** `resubmitIncompletePublications(ResubmissionOptions.withMinAge(d))` FAILED kayıtlarda yaş eşiğini yok sayar (SQL'de parantez hatası; 1 dk'lık kayıt 5 dk eşiğine rağmen yeniden teslim edildi); yaş eşiği için `resubmitIncompletePublicationsOlderThan` kullanılır. `modulith-example`'daki `knownDefect_` testi sürüm yükseltmesinde durumu gösterir.

### 11.3 Idempotent Consumer (Inbox)

At-least-once teslimde aynı mesaj birden çok kez gelir (yeniden teslim, tüketici çökmesi, poller yeniden denemesi). **Zorunlu güvence:** tekrar teslim çift iş üretmez. Bunun için **inbox satırı ile iş değişikliği aynı transaction'da** olmak zorundadır; aksi halde "inbox'a yazdı, işi yapmadan çöktü" (olay kaybı) veya "işi yaptı, inbox'a yazmadan çöktü" (çift iş) pencereleri açık kalır.

```sql
CREATE TABLE <schema>.inbox_event (
  handler     TEXT NOT NULL,            -- 'OrderCreatedNotificationHandler'
  event_id    UUID NOT NULL,            -- CloudEvents id
  received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (handler, event_id)       -- dedup kapsamı HANDLER; aynı olay farklı handler'larda ayrı işlenir
);
```

```
@Transactional  (tek TX)
  1. INSERT INTO inbox_event(handler, event_id) … ON CONFLICT DO NOTHING  → 0 satır ⇒ duplicate, hiçbir şey yapmadan çık (ack)
  2. iş değişikliği (read-model UPSERT / domain yazımı / outbox satırı)
commit
  3. broker ack — YALNIZ commit'ten sonra: Spring `AcknowledgeMode.MANUAL` + `ChannelAwareMessageListener`, `basicAck` inbox TX'i döndükten sonra (`AUTO` = container listener dönüşünde ack'ler, broker auto-ack değildir; `NONE` broker no-ack'tir ve mesajı kaybeder). Geçici hatada `basicReject(tag, true)` (Bölüm 12.3: yalnız reject sayaç artırır ve gecikmeli retry'ı tetikler); commit'ten önce ack'lenen mesaj çökmede kaybolur (mutasyonla doğrulandı)
```

- Handler başarısız olursa TX rollback → inbox satırı da geri alınır → mesaj `basicReject(tag, requeue=true)` ile broker'a döner; QQ gecikmeli retry ile yeniden teslim eder, `x-delivery-limit` dolunca DLQ (Bölüm 12.3). Kalıcı hata `basicReject(tag, false)` → anında DLQ.
- **Dedup kapsamı handler'dır**, tüketici servis değil: aynı `order.order.created` olayını hem read-model handler'ı hem bildirim handler'ı işliyorsa iki ayrı inbox satırı vardır.
- İnbox'ta tutulan iş, yalnız tüketicinin **kendi DB'sindeki** etkiyi idempotent yapar. Handler dış bir sisteme yan etki üretiyorsa (SMS gönder) o etki inbox TX'i içinde yapılamaz; handler dış etkiyi **outbox** satırı olarak yazar (aynı TX), outbox handler'ı dış sisteme sağlayıcının idempotency anahtarıyla gider.
- Üretici `id`'yi deterministik üretir (Bölüm 11.2); tüketici bunu inbox anahtarı olarak kullanır.
- Retention: `received_at` üzerinden (örn. 30 gün; üretici yeniden yayın penceresinden uzun) partition/DELETE.
- **Doğrulama (Bölüm 11.5):** aynı olay iki kez → tek etki; handler ortasında exception → inbox satırı yok ve yeniden teslimde iş yapılır; commit sonrası ack'ten önce çökme → yeniden teslimde duplicate olarak yutulur.

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

**Çalışan hali:** `blueprint/skeleton-example/platform-messaging` — `saga/LocalSagaStore` (begin/success/fail, claim/prepare/complete/retry/manualReview, monitor/cleanup), `saga/SagaRecoveryWorker`, `db/platform/saga_coordinator.sql`; katılımcı örneği ve senaryolar `SagaBehaviourIT` (Bölüm 11.5).

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
| HTTP client timeout | Senkron consume: 0,5 / 1,5 sn (connect + read < gateway bütçesi); worker'ın confirm/compensate/get çağrıları: 2 / 5 sn |
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

**Ek senaryolar (Bölüm 11.2–11.3, 4.6):** inbox satırı + iş aynı TX (handler ortasında exception → satır yok) · commit sonrası ack öncesi çökme → duplicate yutulur · iki poller instance'ı aynı aggregate'in sıralı iki satırı → tek worker, sıra korunur · bir lane'de takılı hedef diğer lane'i durdurmuyor · eski güvenlik kararı yeniden denemede yeni kararı ezmiyor (superseded + `source_revision`) · publisher confirm alınmış ama tüketici işlememiş → üretici "tamamlandı" saymıyor · delta olayında sıra boşluğu → uygulama durur, alarm · snapshot olayında küçük revizyon yok sayılır.

**Çalışan örnekler (seviye 2, gerçek PostgreSQL):** `blueprint/skeleton-example/platform-messaging/src/test/.../OutboxBehaviourIT.java` (outbox/inbox, 13 senaryo) ve `SagaBehaviourIT.java` (saga senaryoları 1–20, 19 test; koordinatör `LocalSagaStore` + `SagaRecoveryWorker`, katılımcı `QuotaParticipant` in-process, belirsizlikler `FlakyParticipant` ile). Katılımcının HTTP/JWT katmanı ve gerçek broker teslimi (seviye 3) bu örneklerin kapsamı dışındadır.

**Kanıt seviyeleri** (kayıt biçimi Bölüm 19.6):
1. Unit ve MVC testleri
2. Gerçek PostgreSQL entegrasyonu (Testcontainers; CI'da her PR)
3. Owner→participant runtime testi (iki servis + broker ayakta; süreç öldürme, yeniden teslim)
4. Release/staging (prod benzeri veri, restart provası)

**Sonuç:** `PASS` / `FAIL` / `BLOCKED`. Testi olmayan senaryo `BLOCKED`'dır; "yazılı ama koşulmamış" `PASS` sayılmaz.

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
- **Boyut:** olay payload'ı küçük tutulur (hedef < 64 KB; RabbitMQ `max_message_size` varsayılanı 16 MB ama kuyruk belleği ve tüketici gecikmesi büyür). Büyük içerik (rapor, görsel, toplu liste) **claim-check**: içerik object storage/DB'ye yazılır, olay yalnız referans (`dataref`, CloudEvents `data` yerine) ve özet taşır; tüketici referansı çözer. Referansın yaşam süresi olayın yeniden işlenme penceresinden uzun olmalıdır.
- **Rollout:** olay değişikliği tek bir "tüketici önce" kuralıyla yönetilmez; değişiklik türüne göre sözleşme ve uyumluluk matrisi **Bölüm 18.4**'te (opsiyonel alan ekleme: sıra serbest; yeni `type`/kırıcı değişiklik: tüketici önce + çift yayın; yeni tüketici: kuyruk/binding önce).

### 12.3 RabbitMQ 4.x Konfigürasyonu

| Bileşen | Desen |
|---|---|
| Sürüm | 4.3+ (community-destekli hat; 3.13 desteği 2024-09'da bitti). Mnesia yok (Khepri tek metadata store; `khepri_db` feature flag'i 4.3'e geçmeden **önce** açılır, aksi halde boot sırasında zorunlu göç); classic mirrored queue yok. |
| Queue tipi | **Quorum queue** (`x-queue-type: quorum`); `x-delivery-limit` (varsayılan 20; N = ilk teslim + N yeniden teslim, `x-delivery-count` > N olunca DLX); `x-dead-letter-strategy: at-least-once` (**ön koşul:** `x-overflow: reject-publish` + `x-dead-letter-exchange`, aksi halde bildirim reddedilir); `x-delivery-count` başlığı ilk teslimde **yoktur**, yalnız `delivery_failed=true` yeniden teslimlerinde 1,2,3… (gerçek 4.3.0'da doğrulandı) |
| Gecikmeli retry | QQ native, **kuyruk argümanı** olarak: `x-delayed-retry-type: failed`, `x-delayed-retry-min`, `x-delayed-retry-max` (ms). Policy yolu **sürüme bağlıdır**: 4.3.0 `delayed-retry-*` anahtarlarını policy olarak reddeder ("not recognised policy settings", yerel broker), 4.3.6 kabul eder ve argümansız bir QQ'da gecikmeyi gerçekten uygular (CI'da ölçüldü). Kuyruk argümanı her 4.3.x'te çalıştığı için varsayılan odur; policy'ye geçmek broker'ın alt sürümünü sabitlemeyi gerektirir. Gecikme lineer: `min(min·delivery_count, max)` (1 s, 2 s, 3 s ölçüldü); `failed` tipi yalnız `delivery_failed=true` ile geri verilen mesajı geciktirir (aşağıdaki reject/nack ayrımı). **Delayed Message Exchange plugin'i kullanılmaz** (arşivlendi, Mnesia tabanlı). |
| Exchange/queue adları | komut: `<servis>.commands` / `<hedef>.<komut>.queue`; event: `domain.events` (topic) / `<tüketici>.<amaç>.queue`; DLX `<servis>.dlx`; DLQ `<queue>.dlq` |

**Producer:**

```yaml
spring.rabbitmq: { publisher-confirm-type: correlated, publisher-returns: true, template.mandatory: true }
```

- `Jackson2JsonMessageConverter` + `RabbitTemplate.setMandatory(true)`.
- Publisher, `CorrelationData` ekler ve trace header'larını enjekte eder. Broker ACK'i sınırlı süre beklenir (örn. 5 sn).
- NACK veya timeout → exception → outbox retry. **Unroutable mandatory mesajda broker `basic.return` gönderir ve ardından yine pozitif ACK verir**: yalnız confirm'e bakmak olayı sessizce kaybeder; ACK gelse bile `CorrelationData.getReturned() != null` ise unroutable sayılır ve exception fırlatılır (4.3.0'da doğrulandı).
- **Yayın her zaman outbox'tan yapılır.** Doğrudan `convertAndSend` kullanılmaz.

**Consumer:**
- `@RabbitListener(queues = …, containerFactory = …)`; container factory `spring.rabbitmq.listener.*` ile yapılandırılır (elle kurulan factory bu property'leri yok sayar).
- **`defaultRequeueRejected=false`** — Spring AMQP varsayılanı `true`'dur ve iş hatası fırlatan mesaj "sonsuza kadar yeniden teslim edilebilir" (Spring dokümanının ifadesi). Bu ayar yalnız container'ın kendi gönderdiği reject'leri (AUTO mod) yönetir; **MANUAL modda listener'ın verdiği bayrak esastır.**
- **Ack modu:** `AcknowledgeMode.MANUAL` + `ChannelAwareMessageListener`; `basicAck` inbox TX'i commit olduktan **sonra** (Bölüm 11.3). (`AUTO`, container'ın listener dönüşünde ack'lemesidir, broker auto-ack değildir; `NONE` broker no-ack'tir ve mesajı kaybeder.)
- **Hata sınıflandırması (4.3.0 AMQP 0-9-1'de doğrulanmış semantik):**
  - Geçici hata (DB/ağ) → `channel.basicReject(tag, requeue=true)`. Yalnız **reject** `delivery_failed=true` sayılır: `x-delivery-count` artar, QQ native gecikmeli retry uygulanır, `x-delivery-limit` dolunca DLQ.
  - `basic.nack requeue=true` (Spring'in `ImmediateRequeueAmqpException` / AUTO-mode requeue yolu `basicNack(tag, multiple, requeue)` gönderir) **düz requeue**'dur: sayaç artmaz, gecikme yok, limit dolmaz → milisaniyelik sıcak döngü (ölçüldü: +7/+24/+26 ms). Kullanılmaz.
  - Kalıcı hata / zehirli mesaj (parse edilemeyen body, bilinmeyen şema) → `basicReject(tag, requeue=false)` → anında DLQ (at-least-once). Bilinmeyen `type` → ack + log (DLQ değil).
  - `RetryInterceptorBuilder.stateful()` + `RepublishMessageRecoverer` uygulama içi alternatiftir; 4.3'te broker tarafı retry varken gereksizdir.
- **Tüketici zaman aşımı:** quorum queue için `consumer-timeout` policy anahtarı (ms; ya da `x-consumer-timeout` consumer argümanı; global varsayılan 30 dk) `basic.consume` anında okunur — policy tüketici başlamadan **önce** kurulur. Süre dolunca QQ ack'lenmemiş mesajı geri alır ve tüketiciye `basic.cancel` gönderir (kanal kapanmaz); Spring container consumer'ı yeniden başlatır, mesaj `redelivered=true` ile gelir (5 sn policy ile doğrulandı). Inbox TX'i bu süreden kısa tutulur; uzun işler outbox satırına devredilir.
- `prefetch` açıkça (10–50; Spring varsayılanı **250**) ve `concurrency` ayarlanır.
- Idempotent handler: inbox `INSERT INTO inbox_event (handler, event_id) … ON CONFLICT (handler, event_id) DO NOTHING` (Bölüm 11.3; PK `(handler, event_id)` olduğu için `ON CONFLICT (event_id)` PostgreSQL'de "no unique constraint" hatası verir).
- DLQ için izleme (derinlik > 0 alarmı) ve replay aracı bulunur.

**Kaçın:** Gecikmesiz requeue (`ImmediateRequeueAmqpException` / `basic.nack requeue=true` ile DB kesintisinde sıcak döngü; QQ'da log/disk büyümesi). Consumer'ı prefetch'siz bırakmak. Commit'ten önce ack: mesaj çökmede kaybolur ve aynı tag'e ikinci ack/reject `PRECONDITION_FAILED unknown delivery tag` ile kanalı kapatır.

**Çalışan hali (seviye 3, gerçek RabbitMQ 4.3.0 + PostgreSQL 18):** `blueprint/skeleton-example/broker-example` — `OutboxEventPublisher` (confirms + returns), `BrokerTopology` (QQ + DLX/DLQ + delayed retry argümanları + stream), manuel ack'li inbox listener, `BrokerBehaviourIT` 12 senaryo (uçtan uca 20 olay, unroutable, gecikmeli retry ölçümü, delivery-limit → DLQ, zehirli mesaj, commit sonrası ack, duplicate, broker `stop_app`/`start_app`, stream replay, consumer timeout, policy-red kanıtı, bilinmeyen tip). Aynı test CI'da `rabbitmq:4.3-management` servis container'ıyla koşar.

**Kural:** Hassas veya güvenlik kritik olay tipleri (moderasyon kararı, ödeme durumu, mağaza bildirimi) kuyruk yerine yalnız imzası doğrulanmış bir internal HTTP uçtan veya webhook'tan kabul edilir. Bu tipler kuyruktan gelirse DLQ'ya düşer.

### 12.4 Replay ve Çoklu Tüketici: RabbitMQ Streams

Aynı olayı birden çok bağımsız tüketicinin okuması ve **geçmişi baştan okuma** (yeni read-model kurma, bug sonrası yeniden işleme, analytics) gerekince, Kafka'ya geçmeden önce **RabbitMQ Streams**: append-only log, non-destructive read, broker'da offset, `max-age`/`max-length-bytes` retention. Stream (`x-queue-type: stream`, `x-max-age`) `domain.events`'e `#` ile bağlanır — üretici ikinci kez yazmaz, exchange kopyalar; işlemsel tüketiciler queue'dan, analitik/replay tüketicileri stream'den okur. Okuma iki yolla: `spring-rabbit-stream` (stream protokolü, 5552) ya da düz AMQP 0-9-1 `basicConsume(..., {x-stream-offset: first|last|next|<offset>|<timestamp>})` — 0-9-1'de prefetch (QoS) ve manuel ack **zorunludur**; ack yalnız kredi açar, mesaj stream'de kalır. Aynı offset'ten ikinci okuyucu aynı mesajları sırayla alır; queue tüketicileri etkilenmez (doğrulandı). Stream'de TTL/öncelik/DLX yok; bunlar queue işidir.

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
3. Consumer tarafında quorum queue bildirimi: `x-delivery-limit`, `x-dead-letter-strategy=at-least-once` + `x-overflow=reject-publish`, DLX/DLQ, `x-delayed-retry-type/min/max`, binding; container MANUAL ack, prefetch, `defaultRequeueRejected=false`; listener kararı: geçici → `basicReject(tag,true)`, kalıcı/zehirli → `basicReject(tag,false)`, bilinmeyen tip → ack. `consumer-timeout` policy'si tüketici başlamadan önce.
4. Listener + inbox satırı ve iş **aynı TX'te** (Bölüm 11.3) + (read-model ise) kaynak başına `source_revision` ve tam durum/değişiklik sözleşmesi (Bölüm 4.6).
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

### 14.1 Sahiplik ve Contract

| Rol | Bileşen |
|---|---|
| Sahip | Yönetim servisi: `SystemParameterService`, `ParameterDefinitionRegistry` (tip ve sınır doğrulaması), internal controller |
| Contract | `<yönetim>-api`: `ParameterGroup` enum, `SystemParameterKey` enum (key + group + tip), `ParameterGroupDto`, `ParameterValues.find(...)` |
| Depolama | `system_parameter` (`value jsonb`, tipe göre CHECK) + `parameter_revision` (append-only trigger, audit FK) |
| Internal uçlar | `GET /internal/parameters/groups/{group}`, `…/since/{revision}`, `…/at?at=<instant>` |
| Admin | `PUT /backoffice/parameters/groups/{group}`: `expectedRevision` ile optimistic kontrol (409), global advisory lock. Audit + revizyon + değer aynı TX'te yazılır. |
| Tüketici | Servis başına tek `BackofficeParameterClient` + `SystemParameterProvider` |
| Kayma tespiti | Açılış kontrolü (ERROR log) + health indicator DOWN + enum↔seed↔registry↔frontend tutarlılık testleri |

### 14.2 İsim ve Tip

- Group: kebab-case. Key: `<alan>.<ad>` snake_case. Enum sabiti: UPPER_SNAKE.
- Tipler: `INTEGER` (+unit), `DURATION` (her zaman saniye), `OPTION_LIST` (`code`, `labels.tr/en`, `order`, `active`).
- Yayınlanmış key yeniden adlandırılmaz. `usage_status`: `DEFINED_ONLY` / `ACTIVE` / `PARTIAL`.

### 14.3 Okuma Kuralları: Fail-Closed ve Bounded Staleness

- **Doğruluk hataları fail-closed:**

| Durum | Hata |
|---|---|
| Key yok | `PARAMETER_NOT_DEFINED` |
| Değer bozuk | `PARAMETER_VALUE_INVALID` |

  Kod içi default değer, yml fallback ve hatayı yutmak yasaktır: parametre tanımsızsa bu bir **deploy hatasıdır** ve açılış kontrolünde yakalanır.

- **Erişilemezlik: bounded staleness (static stability).** Parametre kaynağı bir *control plane*'dir; düştüğünde *data plane* (tüm servisler) durmamalıdır. "5 sn cache + 503" modeli yönetim servisini her servisin tier-0 bağımlılığı yapar (yönetim paneli restart olurken ana iş akışı 503 döner). Kural:
  - Tüketici son başarılı `ParameterGroupDto`'yu revizyonu ve **alınma anıyla** (`fetchedAt`) birlikte **bellekte ve diskte** (local snapshot; soğuk açılışta kaynak yoksa bile ayağa kalkar) tutar; staleness = `now − fetchedAt`. Disk snapshot'ı grup başına bir JSON dosyasıdır, atomik yazılır (tmp + rename), instance'a özel kalıcı volume'de durur; dosya adı yalnız kebab-case grup adından üretilir (diğer adlar reddedilir: path traversal).
  - **Soğuk açılışta snapshot yoksa** ve kaynak erişilemiyorsa grup, kritikliğinden bağımsız `PARAMETER_UNAVAILABLE` (503) döner; kod içi default burada da yasaktır. İlk deploy'da snapshot dizini boş olduğundan açılış kontrolü kaynağın en az bir kez ulaşılabilir olmasını fiilen zorunlu kılar.
  - Kaynak erişilemezse grup başına tanımlı **en fazla T** süre boyunca son bilinen değer kullanılır (örn. hak limitleri 10 dk, yaş/uygunluk kuralları 1 sa). `parameter_staleness_seconds{group}` gauge'u her okumada güncellenir (son başarılı fetch'ten geçen saniye; başarılı fetch'te 0); alarm eşiği grup başına T'nin altında seçilir (örn. %80). Micrometer gauge'u zayıf referans tuttuğundan durum nesnesi provider'da güçlü referansla tutulur (aksi halde metrik NaN olur).
  - T aşılınca yalnız **güvenlik-kritik** olarak işaretli gruplar `PARAMETER_UNAVAILABLE` (503) döner; diğerleri son bilinen değerle devam eder. Hangi grubun kritik olduğu katalogda `criticality` alanıyla tanımlıdır.
  - Bu, tutarlılığı bozmaz: kalıcı sonuç yazılırken kullanılan revizyon zaten snapshot olarak kaydedilir. AWS "static stability" ilkesi ve tüm feature-flag SDK'ları (Unleash: 15 sn poll + disk yedeği + sunucu yoksa yedekten servis; OpenFeature: provider hatasında default) aynı modeli uygular.
  - Alternatif/ek: parametre revizyonları **event** olarak yayınlanır (Bölüm 12), tüketiciler local tabloda tutar; yönetim servisi yalnız yazma yoludur.

- **Tazelik:**
  - `group(...)`: 5 sn'lik instance cache (+ bounded-staleness fallback).
  - `freshGroup`: kullanıcı girdisini doğrulayan yazma akışları (kaynak erişilemezse fallback **yok**, 503).
  - `freshGroupSince(revision)`: kaynaktan okur; dönen revizyon istenenden küçükse (replika gecikmesi) `PARAMETER_REVISION_STALE` (503) ile reddeder, fallback yoktur. Kullanım: eski revizyonlu kayıtları kırparken (clamp) kararın en az o revizyonla verilmesini garanti etmek.
  - `groupAt(instant)`: geçmiş bir anın değeri.
- Birlikte anlamlı key'ler aynı revizyondan okunur. Kalıcı sonuçlara değer ve **revizyon snapshot'ı** yazılır.
- Parametre TX ve lock dışında okunur. `@Value` / `@PostConstruct` ile bağlanmaz. Worker her turda yeniden okur.
- Değer düşürüldüğünde mevcut veriyi uzlaştıran bir worker gerekir.
- Loglara parametre değeri ve ham yanıt yazılmaz. Grup ve key adı yalnız hata kodu mesajlarında (`PARAMETER_NOT_DEFINED`, `PARAMETER_VALUE_INVALID`, açılış kontrolü) yer alır; olağan okuma loglarında yer almaz.

**Çalışan hali (seviye 1):** `skeleton-example/platform-parameters` (23 test, 6 mutasyonun 6'sı yakalandı).

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

**Config Server ne zaman?** Onlarca servis, birden fazla ortam ve merkezi refresh (`/actuator/refresh`, Bus) ihtiyacı varsa. Kullanılıyorsa: (1) **secret taşımaz** — Spring Cloud Config'in güvenlik dokümanı "uygulama adını bilen her kimliği doğrulanmış istemci başka uygulamanın config'ini isteyebilir" der; (2) `optional:` **yok**, `spring.cloud.config.fail-fast=true` + retry — aksi halde sunucu düşükken servis **sessiz kısmi config** ile açılır; (3) native backend production için değil (dokümanın kendi ifadesi: "başlangıç ve test için"), git backend + TLS; (4) güncel sürüm — Config Server 2026'da üç kez delindi: CVE-2026-22739 (Mart, native backend profil ile path traversal + git backend SSRF, CVSS 8.6), CVE-2026-40982 (Mayıs, kimlik doğrulamasız directory traversal, CVSS 9.8; düzeltme 4.3.3 / 5.0.3), CVE-2026-47894 (Ağustos, native backend'de wildcard ile repo dışı dosya okuma; 5.0.0–5.0.4 ve 4.3.0–4.3.4 etkilenir). Native backend hiçbir ortamda production'a çıkmaz; Config Server bağımlılığı Renovate ile günler içinde güncellenir; (5) config-server port'u yalnız iç ağda ve SSRF'e açık servislerden erişilemez.

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
app.http: { connect-timeout-ms: 500, read-timeout-ms: 1500 }   # connect + read < gateway bütçesi (Bölüm 4.7); worker client'ları ayrı grupta 2000 / 5000
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
- **gitleaks** hem pre-commit hem **CI**'da, iki modda: `gitleaks git` (tüm geçmiş, silinmiş secret dahil; `--log-opts` yalnız aralığı daraltır) ve `gitleaks dir` (commit'lenmemiş çalışma ağacı); ikisi birbirinin açığını kapatır. `detect`/`protect` 8.19+'da gizli/eski komutlardır. `--redact` ile secret log'a yazılmaz. gitleaks tarama hatasında da exit 1 döner ("no leaks found in partial scan"); sarmalayıcı (`scripts/gitleaks-check.sh`) JSON raporda bulgu yoksa sonucu "doğrulanamadı" (exit 3) sayar.
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
| Mimari kurallar | **ArchUnit** (`layeredArchitecture()`, `noClasses().that().resideInAPackage("..controller..").should().dependOnClassesThat().resideInAPackage("..repository..")`, `slices().should().beFreeOfCycles()`, `@Configuration` yalnız `config/`, `service.impl`'de yalnız `*ServiceImpl`, `@RequestBody` → `@Valid`). Modüler monolitte ek olarak Spring Modulith `ApplicationModules.of(App.class).verify()` testi **ve negatif eşi**: test kaynaklarında bir modülün paketinde başka modülün internal sınıfına bağımlı kasıtlı bir sınıf; yalnız o paketi içeren bir `ImportOption` ile kurulan `verify()`'ın `Violations` fırlattığı ve mesajın o bağımlılığı adlandırdığı doğrulanır (negatif test olmadan yeşil `verify()` bir şey kanıtlamaz). İzinli bağımlılıklar `@ApplicationModule(allowedDependencies = …)` ile açıkça yazılır; boş dizi "hiçbiri", varsayılan "hepsi" demektir. Çalışma zamanı doğrulaması (`spring.modulith.runtime.verification-enabled`) istenirse `spring-modulith-runtime` ayrıca eklenir; starter-core/-jdbc onu getirmez. Maven enforcer `bannedDependencies` ile `*-core` → `*-core` yasağı. |
| Tutarlılık / sınır | Enum↔seed↔registry↔frontend eşleşmesi. Migration'larda başka schema adı yok. Client'ta yanlış modül DTO'su yok. Hata kodu çakışması yok. |
| Statik config | yml ve alarm kuralı dosyalarını okuyup doğrulayan testler. Local ↔ deploy config drift testi. Hook komutlarının örnek girdiyle testi. |
| Dayanıklılık | Her HTTP client için "hedef yanıt vermiyor" testi: timeout bütçesi, circuit açılması, tanımlı hata (Bölüm 4.7). |
| Mutasyon | Kritik modüllerde (outbox, saga, güvenlik filtreleri, para hesabı) **PIT** (`pitest-maven` + `pitest-junit5-plugin`; plugin yoksa 0 test bulur ve sessiz geçebilir) ile testlerin gerçekten yakaladığı doğrulanır; hedef mutasyon skoru README'de (başlangıç ≥ %80 kritik paketlerde). CI'da haftalık; PR gate'te değil (süre). `skeleton-example`'daki elle mutasyonlar bu pratiğin küçük hali. |
| Yük | k6/Gatling senaryoları staging'de (haftalık ve release öncesi): p99 ve hata oranı SLO'ya karşı; sonuç kapasite planına (Bölüm 24) yazılır. |
| Contract (servisler arası) | Monorepo'da derleme zamanı tip kontrolü yeter; Pact'in kendi karşılaştırması bile "iki tarafı aynı ekip aynı repoda yazıyorsa az katkı" der. Polyrepo'ya geçilirse Pact/Spring Cloud Contract. |
| İstemci contract | CI'da her servisin OpenAPI çıktısı testte üretilir (prod'da `api-docs` kapalı; test `springdoc.api-docs.enabled=true` ile açar) ve repoda commit'li baseline ile `openapi-diff` karşılaştırılır; kırıcı fark PR gate'ini kırar. **Dikkat:** openapi-diff 2.1.x OpenAPI 3.1 belgede şema tipini okumaz (string → integer "değişiklik yok" çıkar); gate springdoc'un **3.0 çıktısını** (`springdoc.api-docs.version=openapi_3_0`) karşılaştırır, istemciye 3.1 belge yayınlanır. Araç opsiyonel istek alanının yeniden adlandırılmasını/kaldırılmasını ve `operationId` değişikliğini kırıcı saymaz; bunlar review'da yakalanır (`operationId` `@Operation` ile sabitlenir). Baseline yalnız bilinçli komutla güncellenir ve PR diff'inde görünür. İstemci client'ı `openapi-generator` ile (örn. `dart-dio`, `typescript-fetch`) üretilir (Bölüm 20). |

**Kurallar:**
- Önce davranış ve edge case'ler belirlenir.
- Negatif case'ler ve yetki hataları zorunludur. Her bug için regression testi yazılır.
- Poller ve check-then-act kodu için concurrency testi zorunludur.
- Kırılgan test yazılmaz: sıraya, saate, rastgeleliğe veya dış servise bağımlı olmamalı.
- HTTP binding, controller metodunu doğrudan çağırarak kanıtlanmaz. Ayırt edilebilir sentetik değerler kullanılır ve geçersiz girdide servise hiç ulaşılmadığı (`verifyNoInteractions`) doğrulanır.
- Feign interface'ini mock'lamak istek oluşumunu kanıtlamaz.
- JWT doğrulaması tamamen mock'lanmaz. Yalnız dış yan etkiler (SMS, e-posta) mock'lanır.
- Parametre testleri Bölüm 14'ün iki ayrı kuralını ayrı ayrı kanıtlar: (a) **doğruluk hatası** (key yok, değer bozuk) → hiç yan etki yok, kod içi/yml default'a düşülmüyor (fail-closed); (b) **erişilemezlik** → son bilinen değer yalnız T süresi içinde ve yalnız kritik olmayan gruplarda kullanılıyor, `parameter_staleness_seconds` artıyor, T aşılınca kritik grup 503; `freshGroup` isteyen yazma akışında fallback yok. "Varsayılan değere düşülmez" kuralı **kod içi default**'u yasaklar, bounded-staleness'ı değil.
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
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseCompactObjectHeaders", "-XX:AOTCache=/app/app.aot", "-jar", "app.jar"]
```

JDK 25 notları: `-XX:+UseCompactObjectHeaders` (JEP 519, final) heap'i %10–20 küçültür; AOT cache (JEP 514/515) build'de bir **training run** ile üretilir (`-XX:AOTCacheOutput=app.aot`, temsilî istek trafiğiyle) ve image'a konur, açılış %15–40 kısalır — cache JDK sürümüne ve classpath'e bağlıdır, image her build'de yeniden üretir. `docker-rollout` sırasında iki container'ın aynı anda yaşayacağı bellek `MaxRAMPercentage` hesabına katılır.

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
| `ci` | PR tetikler. `permissions: contents: read`, concurrency ile iptal. **Tüm üçüncü taraf action'lar 40 karakterlik commit SHA'ya pinlenir** (`uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1`; SHA'lar `git ls-remote --tags` ile alınır); tag mutable işaretçidir — tj-actions/changed-files olayı (CVE-2025-30066, 2025-03) tag'leri yeniden yazıp ~23 000 repodan CI secret'ı sızdırdı. Renovate `helpers:pinGitHubActionDigests` ile SHA'lar güncellenir; org düzeyinde "SHA pinning zorunlu" policy'si (GitHub, 2025-08) açılır. **Affected-module** tespiti (`dorny/paths-filter` + GIB/`-amd`) → servis başına matrix: `mvn -B -ntp verify` (Testcontainers ile gerçek DB testleri dahil, ArchUnit); başarısızsa surefire raporu artifact. Frontend: `npm ci`, lint, `tsc -b`, `npm test`, `npm run build`. Ek: gitleaks (`scripts/gitleaks-check.sh all .`: geçmiş + çalışma ağacı), config lint (`scripts/config-lint.js`), config drift, OpenAPI diff, hook testleri. CI adımlarında `test -f X && çalıştır || true` kalıbı kullanılmaz: script varken kırmızı sonucu da yutar (fail-open). |
| `migration-immutability` | PR tetikler (`edited` dahil). Head SHA ve `fetch-depth: 0` ile checkout; `node scripts/<migration>-immutability.js check --base origin/$BASE_REF`; script'in kendi testleri. |
| `build-images` | `develop`/`release`/`main` push. Değişen servislerin image'ları Jib ile build → GHCR push → cosign imza + SBOM. Çıktı: `<servis>@sha256:…` listesi (artifact). |
| `deploy` | `release` → staging (otomatik), `main` → production (**GitHub environment protection** ile onay). Sunucuya SSH: yalnız `docker compose pull` + `docker-rollout`. Registry, digest ve imza doğrulaması. |

**Doğrulanmış şablon:** `blueprint/.github/workflows/ci.yml` (kopyalanabilir) — aynı yapı bu deponun `.github/workflows/skeleton-ci.yml` dosyası olarak **gerçek GitHub Actions'ta koştu** (Ek B): `ubuntu-latest` runner'da gömülü PostgreSQL non-root `runner` kullanıcısıyla çalışır; `services:` bloğundaki `rabbitmq:4.3-management` container'ı (4.3.6, Erlang 27) 10 sn'de hazır olur; 4 modül + 44 test **27 sn**; hook kuru çalıştırması ilk denemede iki tasarım hatası yakaladı (`CLAUDE_PROJECT_DIR` göreli verilince hook fail-closed çalıştı ve `set -e` altında `code=$?` deseni hiç çalışmadı) — CI adımı hook'u gerçek dosya yollarıyla (korunan dosya → exit 2, yeni dosya → exit 0, bozuk yapılandırma → exit 2) doğrular.

**Deploy script deseni** (`set -Eeuo pipefail` + `trap rollback ERR`):
1. **Ön kontrol:** Disk ve RAM, altyapıya (DB, Redis, MQ) TCP erişimi, image imza doğrulaması (`cosign verify`).
2. **Hazırlık:** Secret dosyaları üretilir (`chmod 600`), önceki digest listesi yedeklenir.
3. **Rollout sırası:** değişiklik türüne göre Bölüm 18.4'teki sözleşme (release notunda yazılı); genel varsayılan: şema expand → tüketiciler → üreticiler → auth → gateway. Her serviste `docker-rollout`, readiness bekleme, stabilite penceresi (hata oranı ve restart sayısı).
4. Frontend `dist` deploy, gözlem yığını güncellemesi, eski image temizliği.
5. **Durum kaydı:** Mevcut ve önceki digest listesi saklanır. Hata olursa önceki digest'ler ile `docker compose up -d` (saniyeler), secret ve frontend geri yüklenir. **Image rollback ≠ veri rollback** (Bölüm 18.4): eski image yeni şemayla çalışabilmeli (expand/contract); yeni image'ın yazdığı veri, yayınladığı olaylar ve tüketicilerde uyguladığı read-model satırları geri alınmaz.
6. Deploy sonrası smoke test (health + 2–3 kritik uç) ve SLO panosu linki deploy çıktısına yazılır.

### 18.4 Değişiklik Türüne Göre Rollout Sözleşmesi

"Tüketici önce" tek başına yetersiz ve bazen yanlıştır: yeni bir tüketici için kuyruk ve binding üreticiden önce var olmalıdır; opsiyonel alan eklemede sıra serbesttir; kırıcı değişiklikte iki tarafın da iki sürümü bir süre birlikte çalışır. Her PR, etkilediği satırların sözleşmesini release notuna yazar.

| Değişiklik türü | Sıra | Birlikte çalışması gereken sürümler | Geri dönüş |
|---|---|---|---|
| Olaya **opsiyonel alan** ekleme | Serbest (tüketici bilmediği alanı yok sayar) | eski üretici ↔ yeni tüketici (alan `null`/default), yeni üretici ↔ eski tüketici (alan yok sayılır) | Image rollback yeterli |
| **Yeni olay `type`** (kırıcı değişiklik) | Tüketici önce (yeni type'ı tanır) → üretici çift yayın (eski + yeni) → tüketiciler yeni type'a geçer → eski type kapatılır (sunset tarihi) | çift yayın süresince her kombinasyon | Üretici rollback: yeni type kesilir, eski sürer. Tüketici rollback: eski type hâlâ yayında olmalı → sunset'ten önce |
| **Yeni tüketici** (mevcut olay) | Kuyruk + binding (declare) önce → tüketici deploy → gerekiyorsa geçmiş için rebuild (stream/export) | – | Tüketici kaldırılır; kuyruk birikimini önlemek için binding de |
| **Yeni internal uç** | Sağlayıcı önce (allowlist dahil) → çağıran | eski çağıran uçu kullanmaz | Çağıran rollback yeterli |
| **Internal uç kırıcı değişikliği** | Sağlayıcı yeni sürümü **yanında** açar (`/v2` veya yeni alan) → çağıranlar geçer → eski kapatılır | iki sürüm birlikte | Çağıran rollback eski uçun hâlâ açık olmasına bağlı |
| **Şema: kolon/tablo ekleme** | Migration (expand) → uygulama | eski image yeni şemayla çalışır (`ddl-auto: validate` yeni kolonu bilmese de geçer; yeni `NOT NULL` kolon `DEFAULT` ister) | Image rollback yeterli; şema kalır |
| **Şema: kolon silme / tip değişimi** (contract) | Expand (yeni kolon) → uygulama iki kolonu da yazar → backfill → uygulama yalnız yeniye geçer → **sonraki release'te** contract | N ve N-1 image aynı şemayla | Contract'tan sonra rollback **yoktur**; bu yüzden ayrı release |
| **Enum/parametre değeri** ekleme | Tüketen tüm servisler önce (bilinmeyen değeri tanısın) → üreten | eski tüketici bilinmeyen değeri reddedebilir → ekleme öncesi `from()` toleransı | Değer üretimi durdurulur |
| **JWT claim ekleme/kaldırma** | Doğrulayan (tüketen) önce → basan | eski doğrulayan bilmediği claim'i yok sayar | Basan rollback yeterli; kaldırmada tersi |
| **Public API (istemci) alan değişikliği** | Opsiyonel yanıt alanı ekleme: serbest. Yanıt alanı kaldırma/yeniden adlandırma, istek alanını zorunlu yapma, yeni zorunlu istek alanı, yanıtta yeni enum değeri: **kırıcı** → yeni sürüm yanında açılır, eski sürüm `Deprecation`/`Sunset` ile yaşar (Bölüm 20) | eski istemci ↔ yeni sunucu (mağazadaki eski uygulama) | Sunucu rollback'i yeni alanı kaldırır; eski alan sunset'ten önce kaldırılmaz |
| **Allowlist / config** | Config değişikliği ilgili servis restart'ıyla; **kod ile aynı deploy'da** ise önce config | – | Config geri alınır (versiyonlu) |

**Uyumluluk matrisi** her kırıcı değişiklik için PR'a yazılır (dört hücre): `yeni üretici → eski tüketici`, `eski üretici → yeni tüketici`, `yeni → yeni`, `eski → eski`. Bir hücre "çalışmaz" ise çift yayın veya feature flag ile kapatılır; "çalışmaz"ı kabul eden rollout planı `proj-release-readiness-review`'da `FAIL`.

**Geri dönüş sınırı:** Rollback image'ı geri alır; **veriyi almaz**. Yeni sürümün yazdığı satırlar, yayınladığı olaylar (tüketiciler işledi), tüketicilere dağılmış read-model değişiklikleri ve dış sisteme gönderilmiş komutlar kalır. Bu yüzden: (1) yeni yazma biçimi eski image tarafından okunabilir olmalı (expand); (2) olay şeması geriye uyumlu; (3) geri alınamaz etkiler (SMS, ödeme) feature flag arkasında açılır ki kod rollback'i değil flag kapatır. Rollback provası staging'de "N+1 deploy → veri yaz → N'e dön → oku" adımıyla yapılır.

**Doğrulama:** CI'da eski tüketici testleri yeni payload örneğiyle (fixture) koşar; `openapi-diff` ve olay şema testi; release notunda satır bazlı sözleşme; `proj-event-design-review` ve `proj-release-readiness-review` sözleşmeyi ister.

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
.claude/hooks/review-gate.sh          ← PreToolUse(Bash git push): son 1 saatte, BU içerik üzerinde review skill'i? → "ask"
.claude/hooks/review-stamp.sh         ← PostToolUse(Skill): damga = epoch + çalışma ağacı içerik hash'i
.claude/hooks/tree-state.sh           ← ortak: git write-tree ile içerik kimliği (commit'ten bağımsız)
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
| `<proje>-resilience-review` **(yeni)** | Kritik akış kaydı (gecikme bütçesi, bağımlılıklar, kabul edilen eskilik; varsayılan ≤1 uzak çağrı aşılıyorsa ADR), timeout bütçesi zinciri, circuit breaker/bulkhead (TimeLimiter tuzağı), senkron retry yok, fail politikası, control-plane bounded-staleness, Redis eviction, kapasite (thread/havuz), "hedef yanıt vermiyor" testi | Bağımlılık tablosu + `APPROVE…BLOCK`; sıcak yolda ikinci senkron okuma → en az `REQUEST CHANGES` |
| `<proje>-event-design-review` **(yeni)** | Komut/olay ayrımı, "outbox üzerinden RPC" yasağı, CloudEvents envelope, `revision`, şema evrimi (kırıcı → yeni type + çift yayın), RabbitMQ 4.x topolojisi (QQ, DLQ, native retry), üretici (outbox'tan yayın, üretici tarafı sıralama), tüketici (`defaultRequeueRejected=false`, inbox + iş aynı TX, kaynak başına `source_revision`, tam durum/değişiklik sözleşmesi), rollout sözleşmesi (Bölüm 18.4), analytics sink | Olay tablosu + `APPROVE…BLOCK` |
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
| `PreToolUse` | `Bash` (komut `git push` içeriyorsa) | `.claude/hooks/review-gate.sh` | Son 1 saatte **ve bu çalışma ağacı içeriği üzerinde** review skill'i çalışmadıysa kullanıcıya sor (`hookSpecificOutput.permissionDecision: "ask"`); push dışı komutlarda sessiz |
| `PostToolUse` | `Skill` | `.claude/hooks/review-stamp.sh` | `*-review`, `*test-writer`, `*integration-doc` çalıştıysa `.claude/.last-review-check` damgası: `<epoch> <tree-hash>`. Tree hash `tree-state.sh` ile (`git write-tree`, geçici index, ignore edilmeyen tüm dosyalar; damga dosyası hariç). Commit atmak içeriği değiştirmez → damga geçerli kalır; dosya değiştirmek → geçersiz |

Hook komutları **ayrı dosyalarda** yaşar; `settings.json` yalnız dosyayı çağırır. **Neden:** JSON içine gömülü shell komutunda kaçış hatası hook'u sessizce etkisiz bırakır (bir projede push gate'i bu yüzden hiç çalışmamıştı). Her hook `bash -n` ve örnek stdin ile CI'da kuru çalıştırılır (`blueprint/README.md`).

**Migration immutability script'i** (tek kaynak; CI, hook ve elle kullanım aynı kodu çağırır):
- Regex `/(^|\/)db\/migration\/(.+\/)?V[^/]*\.sql$/i`.
- `check [--base <ref>]`: `git merge-base` → `git diff --name-status --no-renames -z` (working tree ve staged dahil). `A` dışındaki her durum ihlaldir.
- `check-file <yol>`: yol base ağacında varsa korunur.
- Base sırası: argüman → env → varsayılan branch. `-` ile başlayan ref reddedilir.
- Çıkış kodları: 0 uygun, 1 ihlal, 3 doğrulanamadı (fail-closed).
- Testler geçici git repolarında `node --test` ile çalışır.

**Kural:** Hook komutları örnek girdiyle test edilir (`bash -n` + sahte stdin ile CI'da). JSON içine gömülü shell komutlarında kaçış hatası kolay yapılır ve hook sessizce etkisiz kalabilir.

**Bulunan ve düzeltilen hata (2026-09-29):** `checkFile` `git ls-tree`'yi proje kökünde çalıştırıyordu; proje kökü repo kökünün **alt klasörüyse** (monorepo) ls-tree pathspec'i cwd'ye göre çözüldüğü için base dosyası "yok" sanılıyor ve yazma **serbest kalıyordu** (fail-open). Git her zaman `--show-toplevel` kökünde çalıştırılır; regresyon testi eklendi (13 test). Ders: hook'lar yalnız "repo kökü = proje kökü" senaryosuyla değil, alt klasör senaryosuyla da test edilir.

**Immutability script'i doğrulanmış davranış** (`node --test scripts/flyway-immutability.test.js`, 13 test): base V dosyasını değiştirme/silme/`git mv` → ihlal; iç içe klasör korunur; branch'te eklenen V ve tüm R__ serbest; Windows ters bölü; `check-file` mutlak/göreli yol; base yokken `fail` → exit 3, `head` → HEAD ağacı; `-` ile başlayan ref reddi; CLI çıkış kodları 0/1/3.

### 19.5 Kural → Makine İlkesi

Dokümandaki bir kural, AI ajanı veya geliştirici unutsa bile **bir şey kırmıyorsa** kural değil dilektir. Her kural için "hangi makine kontrolü yakalar?" sorusu cevaplanır:

| Kural | Makine kontrolü |
|---|---|
| controller → repository yasak, core → core yasak, `@Configuration` yalnız `config/` | ArchUnit + Maven enforcer |
| `@RequestBody` → `@Valid` | ArchUnit veya ErrorProne özel kontrolü |
| throw öncesi structured log | Checkstyle/ErrorProne özel kontrolü ya da review skill |
| Migration değişmezliği | script + CI + hook |
| Secret literal fallback yok; local dışında düz secret yok | `scripts/config-lint.js`: secret görünümlü key veya env adında `${AD:...}` fallback'i (boş ve iç içe fallback, rakamlı env adı ve küçük harfli property placeholder dahil) ihlaldir; local profil dışında düz secret değeri ihlaldir; yalnız local'de `# lint:allow-secret-fallback <gerekçe>`. gitleaks düşük entropili sahte değerleri (`changeme`) **yakalamaz**; yüksek entropili gerçek anahtarlar için ayrı kontroldür. |
| Local ↔ deploy config drift | `scripts/config-drift-check` CI |
| Hata kodu çakışması | unit test (tüm `ErrorCode` enum'ları) |
| Sıcak yol: gecikme bütçesi ve bağımlılık listesi yazılı; varsayılan (≤1 uzak çağrı) aşılıyorsa ADR | kritik akış kaydı (`repo-context.md`) + review skill; otomatik: trace'te uzak span sayısı testi (kayıttaki listeyle eşit) + p99 yük testi eşiği |
| Sürüm/EOL | Renovate + Bölüm 25 çeyreklik kontrol |

`.claude/skills/` elle kopya değil, `.agents/skills/`'e **symlink** veya CI'da senkron kontrolü. PR şablonunda "çalıştırılan review skill'leri ve kararları" bölümü bulunur; review izi kalır.

### 19.6 Doğrulama Kapsamı ve Kanıt Kaydı

Bu dokümanın ve `blueprint/`'in **iki farklı doğrulama seviyesi** vardır; ikisi karıştırılmaz:

| Seviye | Ne kanıtlar | Bu referansta durumu |
|---|---|---|
| **Yapısal** | Kurallar derlenir ve ihlal yakalanır: ArchUnit, enforcer, ErrorCode tekilliği, config drift, immutability script/hook | `skeleton-example` ile **doğrulandı** (pozitif build + 8 kasıtlı ihlal); tarih README'sinde |
| **Davranışsal** | Sistem koşarken tutarlılık güvenceleri sağlanır: outbox tekrar teslimi çift iş üretmez, iki worker aynı satırı işlemez, süreç ölünce kira dolar ve iş devralınır, saga recovery telafi eder, inbox atomik | Outbox/inbox ve saga `skeleton-example/platform-messaging` içinde **gerçek PostgreSQL 17.5 üzerinde doğrulandı** (seviye 2): `OutboxBehaviourIT` 13 senaryo (#21, #22, #25, #27, #28, #29, #32 + iki poller/300 satır, SKIP LOCKED, backoff, DEAD politikası, öncelik, kira güvenlik payı) ve `SagaBehaviourIT` 19 test (senaryo 1–20: replay, eşzamanlı aynı key, çökme noktaları, yanıt kaybı + GET, tombstone, kira devri, istek-recovery yarışı, MANUAL_REVIEW, cleanup/monitor); toplam 11 kasıtlı regresyon yakalandı — `skeleton-example/README.md`. **Koşturulmayan:** owner→participant HTTP/JWT katmanı ve broker ile gerçek yeniden teslim (seviye 3), release/staging provası (seviye 4). Projede P0'ın çıkış koşulu: seviye 3 senaryoları `PASS` ve kanıt kaydı dolu |

"Yapısal olarak doğrulanmış" bir kural davranışsal olarak da doğru olduğu anlamına gelmez (ArchUnit outbox'ın çift yayın yapmadığını söyleyemez). Doküman, blueprint README'si ve uyum raporu bu ayrımı açıkça yazar.

**Kanıt kaydı** (PR şablonunda ve `proj-operation-consistency-review` doğrulama tablosunda her senaryo için):

| Alan | Örnek |
|---|---|
| Senaryo | 22 — tüketici duplicate olay |
| Kanıt seviyesi | 2 (gerçek PostgreSQL) |
| Test / komut | `OrderCreatedHandlerIT#duplicateEventHasSingleEffect` · `mvn -pl services/order/order-core verify` |
| Commit | `a1b2c3d` (test bu commit'te koştu) |
| Ortam | CI job `ci / order-core (PR #123)`, Testcontainers PG 18 |
| Sonuç | `PASS` (link: CI çalıştırması) |
| Tarih | 2026-09-29 |

**Review damgası kanıt değildir.** `review-gate` hook'unun damgası yalnız "bir review skill'i bu içerik üzerinde çalıştı" der; çalışma ağacının içerik hash'ine bağlıdır (`blueprint/.claude/hooks/review-stamp.sh` + `tree-state.sh`; içerik değişince damga geçersizdir, commit atmak bozmaz) ve **yerel** bir kolaylıktır; hook'lar atlanabilir, damga dosyası elle yazılabilir. Zorunlu güvence CI'dır: PR gate'teki testler, immutability, drift, secret taraması ve **test sayısı kontrolü** (0 test = başarısız; `skeleton-example` dersi 2). Bir PR "skill'ler çalıştı" damgasıyla değil, CI'nın yeşil ve kanıt kaydının dolu olmasıyla birleşir.

---

## 20. Geliştirme Süreci

| Konu | Pratik |
|---|---|
| Branch | `feature/<kişi>-<açıklama>` → PR `develop`'a. `release` test ortamına, `main` production'a deploy edilir. |
| PR kontrolleri | Testler, migration immutability, secret taraması, config lint, OpenAPI diff (kırıcı değişiklikte gate kırılır; bilinçli baseline güncellemesi PR diff'inde görünür) |
| Commit | Conventional (`fix(<modül>): …`, `feat(<modül>): …`), ekibin dilinde |
| Push öncesi | İlgili review skill'leri çalıştırılır |
| Doğrulama | Değişiklik izole bir DB'ye karşı servis gerçekten ayağa kaldırılarak doğrulanır |
| API sözleşmesi (tek kaynak) | **OpenAPI üretilir, elle yazılmaz.** CI her servisin `/v3/api-docs` çıktısını alır, tek `<proje>-api.yaml`'a birleştirir, `openapi-diff` ile breaking change'i PR'da işaretler, `openapi-generator` ile istemci client paketini üretir (Flutter: `dart-dio` stable; web: `typescript-fetch`). Postman/Bruno koleksiyonu OpenAPI'den türetilir; elle üçüncü kopya tutulmaz. |
| API versiyonlama | İlk günden karar: `/v1` prefix (önerilen; mobil uygulama mağazada eski sürümüyle aylarca yaşar) veya header. Spring Framework 7 versiyonlamayı **birinci sınıf** destekler: `@GetMapping(version = "1.1")`; Boot 4.1'de çözümleme kodsuz property ile: `spring.mvc.apiversion.use.header=API-Version` (veya `use.path-segment`, `use.query-parameter`, `use.media-type-parameter`), `required`, `supported`, `default`, `detect-supported`. **Dikkat:** `required=true` stratejili DispatcherServlet'teki sürümsüz route'lara da (`/internal/**`, aynı porttaki actuator) uygulanır; actuator ayrı management portunda çalışır ya da `required=false` + `default` seçilir. `supported` birebir eşler (1.0.7, 1.0 değildir). Kırıcı değişiklik yeni versiyon; kaldırılacak sürüm `StandardApiVersionDeprecationHandler` bean'iyle **`Deprecation: @<epoch>` (RFC 9745) + `Sunset` (RFC 8594) + `Link rel="deprecation"`** taşır ve en az N ay yaşar. Bu handler yalnız header ekler; sunset sonrası 410 ayrıca uygulanır (sürüm `supported`'dan çıkarılır ve 410 dönen bir filtre/handler eklenir). |
| İstemci handoff | İstemciyi etkileyen her değişiklik için versiyonlu entegrasyon dokümanı yazılır (şablon aşağıda) — endpoint/alan listesi OpenAPI'den gelir, doküman **davranış, ekran akışı ve hata kodu → ekran** eşlemesine odaklanır |
| Mimari plan | Büyük alanlar için modül içi `docs/` planı; kodla farkları periyodik güncellenir |
| **ADR** (Architecture Decision Record) | Mimari şekil, veri ayrımı, yeni altyapı bileşeni, versiyonlama, güvenlik modeli gibi geri alması pahalı her karar `docs/adr/NNNN-<baslik>.md` olarak yazılır (şablon: `blueprint/docs/adr/0000-template.md`): bağlam, seçenekler, karar, sonuçlar, **yeniden değerlendirme eşiği** (Bölüm 24). ADR'siz mimari değişiklik PR'ı `REQUEST CHANGES`. |
| Local geliştirme | `docker compose -f deploy/docker-compose.local.yml up -d` altyapıyı (Postgres, Valkey ×2, RabbitMQ, Alloy/Grafana) kaldırır; servisler IDE'den `local` profiliyle; Testcontainers dev-time desteği (`SpringApplication.from(App::main).with(LocalContainers.class)`) alternatif. Seed verisi `db/seed-local`. `make up / test / lint / check` hedefleri README'de. İlk kurulum 30 dakikayı geçmemeli; geçiyorsa `docs/onboarding.md` güncellenir. |
| Deploy sırası | Değişiklik türüne göre Bölüm 18.4 tablosu geçerlidir: yeni enum değeri / olay tipi için **tüketici önce**, yeni internal uç için **sağlayıcı önce**, opsiyonel alan eklemede sıra serbest. Yanıtta yeni enum değeri `openapi-diff`'te kırıcı görünür; istemci bilinmeyen değeri tolere eden sürümle önce yayınlanır. |
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
- [ ] Sıcak yol listesi ve her biri için kritik akış kaydı (Bölüm 1.2: gecikme bütçesi, bağımlılıklar ve gerekçeleri, kabul edilen eskilik, düşünce davranış).
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
- [ ] Read-model tablosu kendi şemasında; **kaynak başına** `source_revision` ve tüketim konumu tablosu; olay sözleşmesi (tam durum / değişiklik) ve delta'da sıra boşluğu davranışı yazılı; karar başına kabul edilen eskilik (T) ve fail-closed davranışı yazılı; rebuild yolu (stream replay veya sahibin export ucu) belgelendi (Bölüm 4.6).
- [ ] Rollout sözleşmesi satırı (Bölüm 18.4) ve kırıcıysa 4 hücreli uyumluluk matrisi PR'da.
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

### 23.3 Outbox Claim Sorgusu (generic `outbox_event`, `platform-messaging`)

Bölüm 11.2'deki tek generic tabloya karşı çalışır; servisler bu repository'yi yazmaz, starter'dan alır. Lane (`kind`) parametreli olduğu için bir lane'in takılması diğerini bekletmez. **Çalışan, test edilmiş JDBC hali:** `blueprint/skeleton-example/platform-messaging` (`OutboxRepository`, `OutboxPoller`, `InboxProcessor`, `db/platform/outbox_inbox.sql`); aşağıdaki JPA şablonu aynı sorguları taşır.

```java
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    // Birden fazla instance ayni satiri alamaz: SKIP LOCKED kilitli satirlari atlar, locked_until kira suresidir.
    // Kirasi dolan PUBLISHING satiri (instance cokmesi) yeniden claim edilir. Lane = kind; oncelik yuksek olan once.
    // Sira gereken aggregate'ler icin: ayni aggregate_id'nin daha eski PENDING satiri baska worker'daysa bu satir atlanir
    // (NOT EXISTS) — sira uretici tarafinda korunur (Bolum 11.2).
    @Transactional
    @Query(value = """
            WITH candidates AS (
                SELECT o.id FROM <schema>.outbox_event o
                WHERE o.kind = :kind
                  AND (o.status = 'PENDING' OR (o.status = 'PUBLISHING' AND o.locked_until <= :now))
                  AND o.next_retry_at <= :now
                  AND NOT EXISTS (SELECT 1 FROM <schema>.outbox_event p
                                  WHERE p.aggregate_id = o.aggregate_id AND p.created_at < o.created_at
                                    AND p.status IN ('PENDING','PUBLISHING'))
                ORDER BY o.priority DESC, o.created_at
                FOR UPDATE OF o SKIP LOCKED
                LIMIT :limit)
            UPDATE <schema>.outbox_event o
            SET status = 'PUBLISHING', locked_until = :lockedUntil, claim_token = :claimToken
            FROM candidates WHERE o.id = candidates.id
            RETURNING o.*
            """, nativeQuery = true)
    List<OutboxEvent> claim(@Param("kind") String kind, @Param("now") Instant now,
                            @Param("lockedUntil") Instant lockedUntil, @Param("claimToken") UUID claimToken,
                            @Param("limit") int limit);

    // Sonucu yalniz claim sahibi yazar; kirasi elinden alinmis eski worker satiri ezemez.
    // claim_token uzak hedefe cift teslimi ENGELLEMEZ; hedef idempotent olmak zorundadir (Bolum 11.3).
    @Transactional @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.id = :id AND o.claimToken = :claimToken")
    int deleteProcessed(@Param("id") UUID id, @Param("claimToken") UUID claimToken);

    @Transactional @Modifying
    @Query("""
            UPDATE OutboxEvent o SET o.status = :status, o.retryCount = :retry, o.nextRetryAt = :next,
                   o.lockedUntil = null, o.claimToken = null, o.lastErrorCode = :err
            WHERE o.id = :id AND o.claimToken = :claimToken
            """)
    int release(@Param("id") UUID id, @Param("claimToken") UUID claimToken, @Param("status") String status,
                @Param("retry") int retry, @Param("next") Instant next, @Param("err") String err);
}
```

### 23.4 Outbox Poller (lane bazlı, handler'a yönlendiren)

```java
/**
 * Generic outbox poller (platform-messaging). Her lane (kind) icin ayri zamanlanmis dongu; boylece yavas bir HTTP
 * hedefi event yayinini bekletmez. Satirlar SKIP LOCKED + kira ile claim edilir, uzak is transaction disindadir.
 * Isi yapan handler kind'a gore secilir: EVENT -> topic exchange publish (confirm), COMMAND -> queue, HTTP -> client.
 * Hedefler idempotent oldugu icin tekrar islenme zararsizdir; claim_token yalniz poller'in kendi yazma yarisini cozer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository repository;
    private final Map<String, OutboxHandler> handlersByKind;      // "EVENT","COMMAND","HTTP" (bean adi = kind)
    private final OutboxProperties props;                           // baslangic ayarlari: batch 50, lease 120 sn, safety 30 sn,
                                                                    // maxBackoff 600 sn, stuckEvery 10 (Bolum 11.2, olcumle degisir)
    private final TraceContextCarrier traceContextCarrier;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${platform.outbox.poll-interval-ms:1000}")
    public void pollEvents()   { poll("EVENT"); }
    @Scheduled(fixedDelayString = "${platform.outbox.poll-interval-ms:1000}")
    public void pollCommands() { poll("COMMAND"); }
    @Scheduled(fixedDelayString = "${platform.outbox.poll-interval-ms:1000}")
    public void pollHttp()     { poll("HTTP"); }

    void poll(String kind) {
        Instant claimedAt = now();
        Instant leaseEnd = claimedAt.plusSeconds(props.leaseSeconds());
        UUID claimToken = UUID.randomUUID();
        List<OutboxEvent> entries = repository.claim(kind, claimedAt, leaseEnd, claimToken, props.batchSize());
        if (entries.isEmpty()) return;                                  // bos turlar loglanmaz

        OutboxHandler handler = handlersByKind.get(kind);
        Instant workDeadline = leaseEnd.minusSeconds(props.leaseSafetySeconds());
        int applied = 0, failed = 0, deferred = 0, dead = 0;
        for (OutboxEvent entry : entries) {
            if (now().isAfter(workDeadline)) { deferred++; continue; }   // kira dolunca baska instance alir; deneme sayilmaz
            Span span = traceContextCarrier.startSpan(
                    traceContextCarrier.from(entry.getHeaders()), "outbox." + kind.toLowerCase() + ".apply");
            try (var ignored = traceContextCarrier.withSpan(span)) {
                handler.handle(entry);                                   // publish+confirm / queue / HTTP; TX disinda
                repository.deleteProcessed(entry.getId(), claimToken);
                applied++;
            } catch (PermanentFailureException e) {                      // kalici 4xx (401/403/408/429 haric); is turune gore
                span.error(e);
                if (entry.isNeverDead()) { markFailure(entry, claimToken, e); failed++; }   // guvenlik yan etkisi DEAD olmaz
                else { repository.release(entry.getId(), claimToken, "DEAD", entry.getRetryCount(), now(),
                        e.getClass().getSimpleName()); dead++; }
            } catch (Exception e) {
                span.error(e);
                markFailure(entry, claimToken, e);
                failed++;
            } finally {
                span.end();
            }
        }
        meterRegistry.counter("outbox_processed_total", "lane", kind, "outcome", "applied").increment(applied);
        log.info("Outbox batch finished: lane={} claimed={} applied={} failed={} dead={} deferred={}",
                kind, entries.size(), applied, failed, dead, deferred);
    }

    Instant now() { return Instant.now(); }                             // test edilebilir zaman kaynagi

    private void markFailure(OutboxEvent entry, UUID claimToken, Exception e) {
        int retries = entry.getRetryCount() + 1;
        String errorType = e.getClass().getSimpleName();                // exception mesaji degil
        repository.release(entry.getId(), claimToken, "PENDING", retries,
                now().plusSeconds(backoffSeconds(retries, props.maxBackoffSeconds())), errorType);
        if (retries % props.stuckAlertEvery() == 0) {
            log.error("Outbox entry still failing: code=OUTBOX_STUCK lane={} eventId={} retry={} exceptionType={}",
                    entry.getKind(), entry.getId(), retries, errorType);
        } else {
            log.warn("Outbox entry failed; retry scheduled: lane={} eventId={} retry={} exceptionType={}",
                    entry.getKind(), entry.getId(), retries, errorType);
        }
    }

    // min(maxBackoff, 30 * 2^n) sn — baslangic ayari
    static long backoffSeconds(int retries, long maxBackoff) {
        return Math.min(maxBackoff, 30L * (1L << Math.min(Math.max(retries, 0), 20)));
    }
}
```

Servis tarafında yalnız handler'a takılan **yönlendirme** yazılır (örn. `event_type` → hangi exchange/routing key; HTTP → hangi client). Servis kendi poller'ını, kendi tablosunu, kendi claim sorgusunu yazmaz (Bölüm 22 anti-pattern).

### 23.5 Controller Binding Testi

```java
/** Binding HTTP katmani uzerinden dogrulanir; kimlik yalniz dogrulanmis baglamdan gelir. */
class OrderControllerTest {

    private final OrderService orderService = mock(OrderService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new OrderController(orderService))
            .setCustomArgumentResolvers(new CurrentAccountArgumentResolver())
            .setControllerAdvice(new GlobalServiceExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC)))
            // Controller'da @GetMapping(version=...) varsa strateji sart; yoksa kurulum
            // "API version specified, but no ApiVersionStrategy configured" ile kirilir. Her istek API-Version header'i tasir.
            .setApiVersionStrategy(new DefaultApiVersionStrategy(List.of(new HeaderApiVersionResolver("API-Version")),
                    new SemanticApiVersionParser(), true, null, true, null, null))
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
        // Kimlik yoklugu istemci girdisi hatasi degildir: platform-security resolver'i 401 ACCOUNT_CONTEXT_REQUIRED doner
        // (guvenlik katmani duz {code, message} zarfi kullanir; is hatalari Bolum 7'deki zarfi).
        mockMvc.perform(post("/orders/{orderId}/cancel", orderId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_CONTEXT_REQUIRED"));
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

Kimlikler ayırt edici sentetik sabitlerdir (`aaaaaaaa-…a001`, sahte `bbbbbbbb-…b002`); `randomUUID` ile kimliğin yanlış kaynaktan geldiği ayırt edilemez. Ek zorunlu senaryolar: geçersiz body → 400 + `verifyNoInteractions(service)`; aynı istekte query/header/body'de sahte accountId gönderilir ve servis yalnız `x.accountId` attribute'undaki kimlikle çağrılır (`verify(service, never()).create(eq(spoofed), any(), any())`). **Çalışan hali (seviye 1):** `order-core` web katmanı testleri (20 test, 11 mutasyonun 11'i yakalandı).

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

Şablon ret yolunu da kapsar: istemci serbest metni (örn. `sku`) içine CR/LF + sentetik telefon (`+905551234567`), e-posta (`jane.doe@example.com`) ve `token=SECRET-TOKEN-MARKER-…` gömülür. `outcome=REJECTED` satırının ve maskeli hallerin (`+90********67`, `j***@e***.com`, `token=[REDACTED]`) **varlığı**, ham işaretlerle accountId'nin ise rendered mesaj, `getArgumentArray()`, `getMDCPropertyMap()` ve `getThrowableProxy()` zincirinde **yokluğu** assert edilir; yalnız `getFormattedMessage` kontrolü argüman ve exception zincirindeki sızıntıyı görmez.

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

**Kontrol edilecek kaynaklar ve son anlık görüntü:** Ek A (tarihli). Çeyrekte bir güncellenir ve projede `docs/versions.md` olarak tarihli tutulur; bu bölümdeki kurallar tarihten bağımsızdır, Ek A'daki sayılar değildir.

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

Keşif listesi (her satır: mevcut durum · referans bölümü · kural sınıfı (zorunlu güvence / varsayılan tercih / başlangıç ayarı — Bölüm 1.4) · fark · risk · doğrulama seviyesi (yapısal / davranışsal / yok) · profil):
- Repo ve modül topolojisi; api/core ayrımı var mı; ortak kütüphane(ler); build/deploy hattı (nerede build ediliyor, registry var mı).
- Servis kimlik tablosu (port, actor, audience, şema, hata bloğu); **kritik akış kaydı** (her sıcak yol için bütçe, uzak bağımlılıklar ve gerekçeleri, kabul edilen eskilik — Bölüm 1.2); **delegasyon matrisi** (internal uçlarda çağıran × işlem × `sub` × kaynak yetkisi — Bölüm 9.2.1).
- Veri: DB sayısı/instance, şema/rol ayrımı, **yedek/PITR/restore provası**, büyüyen tablolar, ID stratejisi.
- Mesajlaşma: broker ve sürümü, outbox var mı/kaç tane, komut/olay ayrımı, consumer ayarları (requeue, prefetch, **ack zamanı ve inbox atomikliği**), DLQ, read-model'lerde kaynak başına revizyon ve olay sözleşmesi (tam durum/değişiklik).
- Güvenlik: JWT türleri ve algoritmaları, secret'ların yeri ve fallback'ler, allowlist/drift, rate limit, mock entegrasyonlar, seed verisi, dosya yükleme, PII/silme akışı.
- Dayanıklılık: timeout'lar, circuit breaker, thread modeli, control-plane bağımlılıkları.
- Gözlem: log formatı, toplayıcı (Promtail?), tracing, **alarm kanalı**, SLO, runbook.
- Test: gerçek DB testleri CI'da mı, ArchUnit var mı (ve kaç test koşuyor), hata kodu tekilliği, drift testi. **Doğrulama seviyesi** her satırda ayrı yazılır: yapısal (kural derlenir) / davranışsal (outbox tekrar teslimi, iki worker, restart, saga recovery koşturuldu mu) — Bölüm 19.6.
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
   Uyum raporu tablosunda kural sınıfı ve doğrulama seviyesi sütunları dolu; "zorunlu güvence" ihlalleri her zaman ilk sırada.
4. **Sorular** (26.1/7).
5. Onay sonrası: her PR için `docs/ai/review-checklist.md`'deki skill'ler ve PR şablonu.

**Kaçın:** Tek dev PR ile "her şeyi düzenlemek"; ArchUnit'i ilk gün tüm kurallarla zorunlu yapıp build'i günlerce kırık bırakmak; upgrade'i test ağı olmadan yapmak; profil dışı altyapı (Kafka, k8s, Vault) kurmak; mevcut migration'lara dokunmak; secret değerlerini rapora yazmak.

---

## 27. Kanıt Haritası: Hangi İddia Nasıl Doğrulandı

Bu doküman iki tür ifade taşır: **kural** (ne yapılmalı) ve **iddia** (bu yaklaşım şu koşulda çalışır). İddialar bu tabloda kanıtına bağlanır; kanıtı olmayan iddia açıkça "kanıt yok" diye işaretlenir. Seviyeler Bölüm 19.6'daki gibidir: **Y** yapısal (kural derlenir/ihlal yakalanır), **1** unit/MockMvc, **2** gerçek PostgreSQL/Redis/PgBouncer süreci, **3** iki süreç/uygulama gerçek HTTP/AMQP, **CI** GitHub Actions'ta koştu, **W** web kaynağıyla doğrulandı (Ek B), **S** gerçek Claude Code oturumu.

| İddia (bölüm) | Kanıt | Seviye | Nerede |
|---|---|---|---|
| Katman/sınır kuralları makineyle zorlanır (3, 4, 19.5) | ArchUnit 8 kural + enforcer; 8 kasıtlı ihlal yakalandı | Y | `skeleton-example/order-core` `ArchitectureRulesTest` |
| Hata kodu tekilliği, config drift, secret fallback yasağı (7.2, 15.2) | testler + kasıtlı ihlaller | Y | `ErrorCodeUniquenessTest`, `ConfigDriftTest` |
| Migration değişmezliği hook + CI (10.2, 19.4) | 13 script testi; hook gerçek yolla exit 2/0; alt klasör fail-open hatası düzeltildi | Y, CI | `blueprint/scripts`, `.github/workflows/skeleton-ci.yml` |
| Review damgası içerik hash'ine bağlı (19.4, 19.6) | 11 hook senaryosu | Y | `blueprint/.claude/hooks` |
| Outbox: tekrar teslim tek etki, iki worker, kira devri, lane izolasyonu, üretici sıralaması, backoff/DEAD (11.2) | 13 senaryo + 5 mutasyon | 2 | `platform-messaging` `OutboxBehaviourIT` |
| Inbox atomikliği ve handler kapsamlı dedup (11.3) | senaryo #22, #25 | 2 | `OutboxBehaviourIT` |
| Local saga: begin/consume/success CAS, recovery, tombstone, yanıt kaybı, yarış, MANUAL_REVIEW, cleanup (11.4, 11.5) | 19 test, matris 1–20, 6 mutasyon yakalandı + 1 eşdeğer | 2 | `SagaBehaviourIT` |
| Gömülü PostgreSQL non-root CI runner'da, RabbitMQ 4.3 servis container'ı, test sayısı koruması, SHA-pinli action'lar (16, 18.3) | run 2 yeşil, 44 test, 27 sn | CI | Actions run 36547695286 |
| Broker senaryoları ve kaos testi (`stop_app`/`start_app`) CI servis container'ında (`docker exec`) | run 36554502553 yeşil; ilk koşu (36552830835) iki sürüme bağlı varsayımı yakaladı: 4.3.6 policy ile gecikmeli retry'ı kabul eder, broker durunca `AmqpIOException` da gelir | CI | Actions run 36554502553 |
| Sürüm/EOL/CVE iddiaları (2, Ek A) | 30+ kaynak | W | Ek B |
| Servis JWT filtresi: EdDSA, kid rotasyonu, aud/iss/exp, alg-confusion, path normalize, first-match allowlist, delegasyon matrisi (9.2–9.5) | 94 test; ilk sürümde `/internal;x/...` bypass'ı bağımsız incelemede bulundu, düzeltildi ve teste bağlandı; ikinci inceleme ACCEPT | 1 | `platform-security` |
| Read-model: kaynak başına revizyon, konumdan tazelik, delta boşluğu, deterministik rebuild (4.6) | 15 test, 6 mutasyon; ACCEPT | 2 | `platform-messaging/readmodel` (`ReadModelBehaviourIT`) |
| DB rol ayrımı, default privileges, rol zaman aşımları, RLS SET LOCAL, `uuidv7()`, Flyway baseline güvenlik ağı, PgBouncer prepared statements (10.1–10.5) | 15 test gömülü PostgreSQL 18 + gerçek PgBouncer 1.22; uygulama rolünün `flyway_schema_history`'ye yazabilmesi bağımsız incelemede bulundu, `afterMigrate` REVOKE ile kapatıldı; ikinci inceleme ACCEPT | 2/3 | `db-security-example` |
| Global handler, binding testi, log privacy, ECS log, API versiyonlama + Deprecation/Sunset, SSRF `InetAddressFilter`, lazy connection, `@Retryable`/`@ConcurrencyLimit` (6, 7.3, 8, 9.11, 20) | 20 test, 11/11 mutasyon; bağımsız doğrulama ACCEPT | 1 | `order-core` |
| Parametre bounded-staleness, soğuk açılış, `freshGroupSince`, staleness metriği (14.3) | 23 test, 6/6 mutasyon; ACCEPT | 1 | `platform-parameters` |
| Redis Lua fixed-window rate limit, TTL iyileşmesi, `NOSCRIPT`, fail-open/closed, kopuk bağlantı gecikmesi (9.6) | 15 test gerçek Redis 7 ile, 12/12 mutasyon; ACCEPT | 2 | `rate-limit-example` |
| Modulith `verify()` + negatif test, event publication registry atomikliği ve en-az-bir-kez teslimi, 2.1.1 `withMinAge` hatası (1.1, 11.2, 16) | 12 test, gömülü PostgreSQL 18; 5/5 mutasyon; ACCEPT | 1/2 | `modulith-example` |
| OpenAPI üretimi + openapi-diff kırıcı değişiklik yakalama (16, 18.4, 20) | 16 test: baseline gate'i, 8 varyant, araç sınırları sabitlendi (3.1 tip körlüğü, opsiyonel istek alanı adı, operationId); 9 mutasyon, 8 yakalandı + 1 beklenen uyumlu; ACCEPT | 1 | `contract-example` |
| gitleaks (geçmiş + çalışma ağacı, tarama hatası ayrımı) + config-lint (15.3, 18.3, 19.5) | 30 test, 10/11 mutasyon; CI adımları fail-closed; ACCEPT | Y | `blueprint/scripts` |
| İki uygulama arası saga, timeout/circuit breaker/bulkhead, delegasyon, restart, trace yayılımı (4.7, 6.8, 8.6, 9.2.1, 11.4) | İki gerçek Boot uygulaması, gerçek HTTP + servis JWT + PostgreSQL; 8 senaryo, 7/8 mutasyon + 1 eşdeğer; ACCEPT. Bulgu: worker'ın `sub` kaynağı ve timeout/bütçe çelişkisi | 3 | `runtime-example` |
| RabbitMQ 4.3: confirms+returns, QQ+DLQ, native delayed retry, ack-after-commit, streams replay, consumer-timeout, broker down (12.3, 12.4) | 12 senaryo PASS: yerelde 4.3.0, CI'da 4.3.6 (Actions run 36554502553); 6 mutasyon yakalandı, 1 eşdeğer mutasyon açıklandı | 3 | `broker-example/BrokerBehaviourIT` |
| Hook'lar ve skill'ler gerçek Claude Code oturumunda (19.3, 19.4) | Headless `claude -p` oturumunda migration hook'u engelledi, review-gate sordu, damga skill çağrısıyla yazıldı; 7 kusur bulundu ve düzeltildi (hook alt dizin fail-open'ı dahil) | S | `blueprint/README.md` gerçek oturum tablosu |
| 12 review skill'i gerçek bir PR'da (19.3) | Tohumlanmış kusurlu PR'da 67 beklenen eşleşmeden 63'ü yakalandı, 3 tuzağın hiçbiri işaretlenmedi; kaçırılanlar skill metinlerine işlendi (revize metin yeniden koşulmadı) | S | doguyaras/doguyaras PR #1 özet yorumu |
| **Kanıtı olmayanlar** | — | — | `docker-rollout` sıfır kesinti (Docker yok), deploy script/rollback provası, Debezium CDC, WebSocket/Redis fan-out, ScyllaDB/OpenSearch eşikleri, k6 yük testi, SOPS/OpenBao akışı, App Store/Play doğrulama, KVKK silme saga'sı uçtan uca. Bunlar projede yazılır; bu referans yalnız desenleri verir. |

---

## Ek A — Sürüm Notları (tarihli anlık görüntü)

> **Anlık görüntü tarihi: 2026-09.** Bu ek, dokümanın kural bölümlerinden ayrı tutulur: kurallar (Bölüm 25) değişmez, aşağıdaki sürümler ve tarihler eskir. Yeni projeye başlarken bu tablo kaynaklardan yeniden doğrulanır ve projede `docs/versions.md` olarak tarihli kopyalanır. Bölüm 2'deki sürüm numaraları da bu tarihe aittir.

**Kontrol edilecek kaynaklar:**

| Bileşen | Kaynak | Not (2026-09) |
|---|---|---|
| Spring Boot / Framework / Security / Cloud | `spring.io/support-policy`, proje sayfalarındaki destek tabloları, `endoflife.date/spring-boot` | Boot 3.x OSS desteği bitti (3.5: **2026-06-30**, son OSS yaması 3.5.16); **4.0.x OSS: 2026-12-31**, **4.1.x OSS: 2027-07-31** (ticari 2028-07-31); 4.1.0 çıkışı 2026-06-10. Spring Cloud OSS: **2025.1.x Boot 4.0 + 4.1** (2025.1.2 ile 4.1 uyumu; 2025.1.3 Ağustos 2026); 2026.0.x → Boot 4.2 (milestone). Spring Modulith 2.x (Boot 4; 2.1.1 Ağustos 2026). Her minor ≥13 ay; major'ın son minor'u ticari desteğe geçer. |
| Java | `endoflife.date/oracle-jdk`, Adoptium/Temurin | 25 LTS (2025-09); 21 LTS hâlâ destekli; LTS dışı sürümler üretimde kullanılmaz |
| PostgreSQL | `postgresql.org/support/versioning` | 5 yıl; 15 → 2027-11, 14 → 2026-11; 18 güncel |
| Redis / Valkey | GitHub releases; lisans: `LICENSE.txt` | Redis 7.x bakımsız; 8.x AGPL/RSALv2/SSPL; Valkey 9.x BSD |
| RabbitMQ | `rabbitmq.com/release-information`, `endoflife.date/rabbitmq` | 4.3 (2026-04): Mnesia tamamen kaldırıldı, **`khepri_db` feature flag'i 4.3'e yükseltmeden önce açılır**; QQ native delayed retry ve consumer timeout policy ile. Community-destekli tek seri en yeni minor |
| Elasticsearch / OpenSearch | `elastic.co/support/eol`, OpenSearch releases | ES 8.x bakım sonu 2027-01-15, destek sonu 2027-07-15; 9.5.x güncel. OpenSearch 3.8 (2026-08) güncel; lisans dalına göre farklı |
| Grafana yığını | GitHub releases; Promtail EOL (2026-03-02) | Alloy, Loki, Tempo, Prometheus minor'ları birlikte güncellenir |
| Testcontainers, ArchUnit, Resilience4j, Debezium | GitHub releases | Testcontainers 2.x artefakt adları `testcontainers-` önekli, eski koordinatlar 1.21.4'te dondu; Debezium 3.6 (2026-07, Kafka Connect 4.3); HikariCP 7.x (Java 11+); PIT 1.30 + `pitest-junit5-plugin` |
| Node / React / Vite / MUI | `endoflife.date/nodejs`, proje sayfaları | Node 24 Active LTS (bakım 2026-10-20, EOL 2028-04-30); 22 bakımda (EOL 2027-04-30); LTS dışı sürüm CI/Dockerfile'da kullanılmaz |
| CVE'ler | `spring.io/security`, GitHub Advisory DB (Dependabot alerts), `osv.dev` | Yaması yalnız ticari sürümde olan CVE = upgrade tetikleyicisi |

---

## Ek B — Doğrulama Kaynakları (2026-09-29)

Bu ekteki tarih ve sürüm iddiaları aşağıdaki kaynaklardan doğrulandı; kaynağı olmayan iddia dokümanda "doğrulanmadı" diye işaretlidir. Çeyreklik kontrolde bu liste yenilenir.

| İddia | Kaynak |
|---|---|
| Spring Boot 4.1.0 2026-06-10; 4.1 OSS 2027-07-31; 4.0 OSS 2026-12-31; 3.5 OSS 2026-06-30 | spring.io/blog (2026-06-10), herodevs.com Spring Boot EOL tablosu, versionlog.com/spring-boot/4.1 |
| Spring Cloud 2025.1.2 Boot 4.1 uyumu; 2026.0.0-M1 Boot 4.2 tabanlı | spring.io/blog 2026-06-11 ve 2026-09-24 |
| Config Server CVE-2026-22739 / 40982 / 47894 | spring.io/security/cve-2026-22739, -40982, -47894 |
| Gateway CVE-2025-41235 ve `trusted-proxies` | spring.io/security/cve-2025-41235; docs.spring.io Gateway HttpHeadersFilters |
| Spring Framework 7 API versiyonlama, `@Retryable`/`@ConcurrencyLimit` | docs.spring.io webmvc-versioning; spring.io/blog 2025-09-09 ve 2025-09-16 |
| Boot 4.1 `InetAddressFilter`, `connection-fetch=lazy`, gRPC | github.com/spring-projects/spring-boot Spring Boot 4.1 Release Notes; infoq.com 2026-06 |
| `spring-boot-starter-opentelemetry` | spring.io/blog 2025-11-18 |
| Spring Modulith 2.0 GA (2025-11-21), 2.1.1/2.2 M1 (2026-08) | spring.io/blog |
| Spring Security 7 (passkeys, OTT, DPoP 6.5+) | docs.spring.io/spring-security whats-new |
| OpenFeign feature-complete (2022.0.0'dan beri) | docs.spring.io/spring-cloud-openfeign |
| Java 25 LTS, JEP 491/506/514/515/519 | openjdk.org/jeps, inside.java 2025-10 |
| PostgreSQL 18 (2025-09-25): `uuidv7()`, AIO, skip scan, OAuth | postgresql.org/docs/release/18.0 |
| PgBouncer prepared statements 1.21+, `max_prepared_statements` varsayılan 200 (1.24.1) | pgbouncer.org/faq, pganalyze.com |
| PostgreSQL zaman aşımı parametreleri | postgresql.org/docs/current/runtime-config-client |
| RabbitMQ 4.3 (2026-04-23): delayed retry, consumer timeout policy, Mnesia kaldırıldı | rabbitmq.com/blog/2026/04/23/rabbitmq-4.3-release; rabbitmq.com/docs/quorum-queues |
| Kafka 4.2 (2026-02-17) share groups GA | kafka.apache.org/blog 4.2.0 announcement |
| Valkey 9.0 (2025-09) | valkey.io/blog/introducing-valkey-9 |
| Redis 8 AGPLv3 (2025-05-01) | redis.io/blog/agplv3 |
| Testcontainers 2.x artefakt önekleri; eski koordinatlar 1.21.4 | github.com/testcontainers/testcontainers-java releases; docs.openrewrite.org testcontainers2migration |
| Promtail EOL 2026-03-02 | grafana.com/docs/loki send-data/promtail |
| Flyway `baselineOnMigrate` uyarısı | documentation.red-gate.com flyway-baseline-on-migrate-setting |
| Debezium Server sink'leri (RabbitMQ AMQP + stream, Redis Streams, NATS, HTTP); Debezium 3.6 | debezium.io/documentation debezium-server; debezium.io/blog 2026-07-01 |
| OWASP ASVS 5.0 (2025-05) | owasp.org ASVS |
| Elasticsearch 8.x bakım sonu 2027-01-15 / destek 2027-07-15; OpenSearch 3.8 | elastic.co/support/eol; opensearch.org |
| Node 24 Active LTS (EOL 2028-04-30), 22 EOL 2027-04-30 | nodejs.org/en/about/eol |
| tj-actions/changed-files CVE-2025-30066; GitHub SHA pinning policy (2025-08-15) | wiz.io blog; github.blog/changelog 2025-08-15 |
| RFC 9745 Deprecation header, RFC 8594 Sunset | rfc-editor.org/info/rfc9745 |
| PIT 1.30 (2026-08), JUnit 5 plugin | pitest.org, github.com/hcoles/pitest |
| `docker-rollout` (wowu) aktif | github.com/wowu/docker-rollout |
| CI şablonu gerçek GitHub Actions'ta koştu: gömülü PostgreSQL non-root runner'da, `rabbitmq:4.3-management` servis container'ı (4.3.6 / Erlang 27.3.4), 44 test, tam build 27 sn; hook kuru çalıştırması gerçek yollarla yeşil | github.com/doguyaras/doguyaras/actions/runs/36547695286 (run 2; run 1'deki kırmızı, CI adımının kendi tasarım hatasıydı) |
| Flyway immutability hook'unda alt klasör (monorepo) fail-open hatası bulundu ve düzeltildi | `blueprint/scripts/flyway-immutability.js` `checkFile`, regresyon testi `flyway-immutability.test.js` (13 test) |
| Hibernate `@UuidGenerator(style = VERSION_7)` (6.5+) | docs.hibernate.org UuidVersion7Strategy |
