# Plate App — Mimari Eleştirel Değerlendirme

> **Amaç:** `plate-app` mimarisinde gerçek anlamda eksik, kusurlu veya geliştirilmesi gereken yapıları; hangi kararların bugün doğru, hangilerinin yarın sorun olacağını; ve neyin **yapılmaması** gerektiğini belgelemek. Referans dokümanındaki (§22) tek tek kod tutarsızlıkları burada tekrar edilmedi; burada **mimari seviyedeki** kararlar tartışılıyor.
>
> **Kaynaklar:** (1) `plate-app-mimari-referans.md` (2026-09-28 tarihli, 3104 satır) satır satır okundu. (2) Beş konu kümesinde (veri katmanı, mesajlaşma, güvenlik, platform/ops, dating-app domain'i) internet araştırması yapıldı; kaynaklar §16'da. Koda doğrudan erişim **yoktu**; yalnız referans dokümandan çıkarılan noktalar "**doğrulanmalı**" diye işaretli.
>
> **Önem işaretleri:**
> - 🔴 **Kritik** — Prod'da gerçek kullanıcı varken kabul edilemez; ilk sprint.
> - 🟠 **Yüksek** — 3 ay içinde; büyüme/olay olmadan önce.
> - 🟡 **Orta** — 6–12 ay; ölçek veya ekip büyüyünce.
> - 🟢 **Hijyen** — Düşük maliyetli, fırsat bulunca.

---

## İçindekiler

1. [Yönetici Özeti](#1-yönetici-özeti)
2. [Asıl Soru: Mikroservis mi, Dağıtık Monolit mi?](#2-asıl-soru-mikroservis-mi-dağıtık-monolit-mi)
3. [Monorepo Sorun mu?](#3-monorepo-sorun-mu)
4. [Veri Katmanı: Tek PostgreSQL, Chat, Elasticsearch, Redis](#4-veri-katmanı)
5. [Mesajlaşma ve Event Mimarisi: Kafka Sorusu](#5-mesajlaşma-ve-event-mimarisi-kafka-sorusu)
6. [Güvenlik Mimarisi](#6-güvenlik-mimarisi)
7. [Dayanıklılık (Resilience) ve Çalışma Zamanı](#7-dayanıklılık-ve-çalışma-zamanı)
8. [Gözlemlenebilirlik](#8-gözlemlenebilirlik)
9. [Sürüm ve Bakım Borcu (EOL Tablosu)](#9-sürüm-ve-bakım-borcu)
10. [Test ve Kalite Güvencesi](#10-test-ve-kalite-güvencesi)
11. [Dating-App Domain'ine Özgü Eksikler](#11-dating-app-domainine-özgü-eksikler)
12. [Build, Deploy ve Operasyon](#12-build-deploy-ve-operasyon)
13. [Backoffice Web ve Mobil Sözleşme](#13-backoffice-web-ve-mobil-sözleşme)
14. [AI Yönetişimi Katmanı](#14-ai-yönetişimi-katmanı)
15. [Yol Haritası ve "Yapmayın" Listesi](#15-yol-haritası)
16. [Kaynaklar](#16-kaynaklar)

---

## 1. Yönetici Özeti

Mimarinin **güçlü** yanları gerçek ve ortalamanın üstünde: transactional outbox + `SKIP LOCKED` disiplini, idempotency ve local saga tasarımı, `/internal/**` default-deny allowlist, privacy-safe log kuralları, Flyway değişmezliği, sistem parametre kataloğunun revizyon/snapshot modeli ve AI yönetişim katmanı. Bunlar korunmalı.

Gerçek sorunlar bunların **etrafında**:

| # | Bulgu | Önem | Kısa gerekçe |
|---|---|---|---|
| 1 | Sistem bir **mikroservis değil, dağıtık monolit**: tek DB, tek host, tek secret, tek deploy birimi, sıcak yolda 3–4 ardışık senkron HTTP çağrısı | 🟠 | Mikroservisin maliyetini ödüyor (ağ, JWT, outbox, 9 deploy), faydasını (bağımsız ölçek/deploy/hata izolasyonu) almıyor. §2 |
| 2 | **Monorepo sorun değil; build/deploy pipeline'ı sorun.** Image'lar prod sunucusunda build ediliyor, registry yok, her Dockerfile `COPY . .` + tam Maven install, rollback = `git reset` + yeniden build | 🟠 | Polyrepo bunu düzeltmez, kötüleştirir. §3, §12 |
| 3 | Tek PostgreSQL'de **DB seviyesinde izolasyon yok** (tek DB kullanıcısı, cross-schema yasağı yalnız testle); **yedekleme/PITR/HA hakkında tek satır yok** | 🔴 | Şema sınırı kağıt üstünde. Veri kaybı senaryosu belgelenmemiş. §4.1 |
| 4 | **Chat için farklı DB bugün gerekmiyor**; ama tablo tasarımı (base64 TEXT, partition yok, retention yok) ölçek eşiğini gereksiz öne çekiyor | 🟡 | Önce partitioning + ayrı instance; ScyllaDB/Cassandra eşiği somut sayılarla tanımlı. §4.2 |
| 5 | **Kafka bugün gerekmiyor; ama "domain event stream" eksikliği gerçek.** 11 outbox tablosunun çoğu asenkron RPC yapıyor, event yayınlamıyor; analytics, recommendation, fraud, read-model replikasyonu için hiçbir veri akışı yok | 🟠 | Kafka'nın çözeceği problem henüz tanımlı değil; önce event envelope + topic + Debezium, Kafka'ya geçiş sonra ucuz. §5 |
| 6 | **Paylaşılan tek HS256 secret** ile servis kimliği; her istekte Redis'e replay kontrolü | 🟠 | Bir servis ele geçirilirse tüm aktörler taklit edilebilir; Redis her isteğin sıcak yolunda. §6.1 |
| 7 | **E2EE gerçek anlamda E2EE değil ("E2EE-lite")**: statik anahtar + HKDF, forward secrecy yok, anahtar dağıtımı sunucunun kontrolünde (MITM mümkün) | 🟠 | Ürün "uçtan uca şifreli" iddiasıyla pazarlanıyorsa hukuki/itibar riski. §6.2 |
| 8 | **Mağaza makbuz doğrulaması mock** ve her ortamda aktif | 🔴 | Gelir sızıntısı; referans doküman §22.1 #1 ile aynı, burada çözüm yolu var. §11.3 |
| 9 | **Sürümler EOL ve yamasız CVE'ler:** Spring Boot 3.2.5 (OSS desteği Aralık 2024'te bitti), Spring Cloud 2023.0.1 (Temmuz 2025), Promtail (Mart 2026), RabbitMQ 3.13, Elasticsearch 8.10, Redis 7. Kullanılan Config Server ve Gateway sürümlerinde **CRITICAL** CVE'ler var; düzeltmeler bu dallar için yalnız ticari sürümde | 🔴 | Config Server 4.1.x native backend'de kimlik doğrulamasız dizin geçişi (CVE-2026-40982); Gateway 4.1.x'te 9.9 puanlı SpEL (CVE-2025-41243). §9 |
| 10 | **Backoffice tier-0 bağımlılık:** parametre kataloğu erişilemezse tüm servisler 503 (fail-closed, 5 sn cache) | 🟠 | Moderasyon paneli düştüğünde swipe/mesaj/abonelik de düşüyor. §7.2 |
| 11 | **Gerçek-PostgreSQL testleri CI'da hiç çalışmıyor** (Testcontainers yok, env ile açılıyor) | 🟠 | En kritik concurrency testleri (claim, unique, lock) yalnız geliştirici makinesinde koşuyor. §10 |
| 12 | **Ürün analitiği / event pipeline'ı yok** (funnel, retention, eşleşme kalitesi ölçülemiyor); feature flag ve A/B yok | 🟠 | Dating app'in temel geri besleme döngüsü eksik. §11.2 |
| 13 | **Dayanıklılık desenleri yok:** circuit breaker, bulkhead, retry budget, virtual thread; Tomcat thread'i 4 ardışık uzak çağrı boyunca bloke | 🟠 | Bir servisin yavaşlaması zincirleme thread tükenmesine dönüşür. §7.1 |
| 14 | **KVKK silme (right to erasure)** için 9 şema + ES + Redis + S3 + yedekler boyunca orkestrasyon yok (export var, silme yok) | 🟠 | Yasal yükümlülük; doğrulanmalı. §6.7 |

**Tek cümlelik özet:** Kod içi disiplin çok iyi, ama sistemin *şekli* (dağıtık monolit + tek host + tek DB + senkron zincirler) ile *ürün ihtiyaçları* (event akışı, analytics, güvenilir ödeme, gerçek E2EE) arasında boşluk var. Öncelik, yeni teknoloji eklemek değil, mevcut sınırları **gerçek** sınırlara çevirmek ve ölçek eşiklerini önceden tanımlamak.

---

## 2. Asıl Soru: Mikroservis mi, Dağıtık Monolit mi?

### 2.1 Kanıtlar

Referans dokümandan, sistemin bağımsız servisler gibi değil tek bir uygulama gibi davrandığını gösteren noktalar:

| Kanıt | Nerede |
|---|---|
| Tek PostgreSQL instance, tek DB kullanıcısı (muhtemel), şema/servis | §10.1 |
| Tek host `docker compose`; tüm servisler aynı `.env`, aynı rollout | §18.2 |
| Tek simetrik service-JWT secret, `iss=gateway` her yerde | §9.2 |
| `common-security` değişince **tüm** servisler rebuild + deploy | §18.3 deploy script |
| `*-api` modülleri versiyonsuz (`1.0.0`), aynı build'de derleniyor | §3.2 |
| Sıcak yolda senkron zincir: swipe = legal (auth) → hidden (user) → block (user) → consumeRight (subscription) → DB | §4.5-A |
| Chat mesajı: legal → hidden → block (user) → DB | §4.5-D |
| Backoffice `ModerationServiceImpl.moderate` transaction + advisory lock içinde user-core'u senkron çağırıyor | §4.4 |
| Her servis, backoffice parametre kataloğuna ve config-server'a açılışta/çalışmada bağımlı | §14.3, §15.1 |
| Chat/user/match/backoffice arasında Redis key sözleşmeleri kodda kopyalanmış (`presence:*`, `seen:*`, `ip-block:*`) | §10.1 |

### 2.2 Bunun somut maliyeti

**Availability çarpımı.** Swipe isteği 5 bileşene bağlı (gateway, match, auth, user, subscription) + PostgreSQL + Redis. Her biri %99,9 ise uçtan uca teorik üst sınır ≈ %99,3 (yılda ~61 saat kesinti). Bir servisin yavaşlaması (örn. user-core'da uzun bir GC), match-core'daki Tomcat thread'lerini 2 sn connect + 5 sn read timeout boyunca bloke eder; 200 thread × 7 sn → match-core saniyede ~28 isteğin üstünde kuyruğa girer.

**Latency toplamı.** p99 = dört çağrının p99'larının toplamına yakınsar. Mobil UX'te swipe'ın 100 ms altında hissedilmesi beklenir.

**Operasyon.** 9 deploy birimi, 9 Flyway history, 11 outbox, 7 poller, 32 Feign client, allowlist drift'i (referans §9.5). Bu yükü tek ekip taşıyor.

### 2.3 Seçenekler

| Seçenek | Ne yapılır | Artı | Eksi |
|---|---|---|---|
| **A. Modüler monolit'e konsolide** (Spring Modulith) | Aynı `*-api`/`*-core` sınırları tek Spring Boot uygulamasında modül olur. Şema/modül ayrımı, `@ApplicationModule` + ArchUnit ile zorlanır. Feign çağrıları in-process metod çağrısı; outbox yerine Modulith event publication registry. Gateway ve config-server kalkar. | Latency ve availability sorunu kökten biter; JWT/replay/allowlist karmaşıklığının çoğu silinir; tek deploy; test ve debug kolay. Küçük ekip için operasyon yükü 1/5. | Bağımsız ölçek yok (bugün zaten yok). Konsolidasyon işi 4–8 hafta. "Mikroservis" etiketi gider (bu bir maliyet değil). |
| **B. Mikroservis kal, zincirleri kes** | Sıcak yoldaki senkron okumaları event-driven **read-model**'e çevir (bkz. 2.4). Servis kimliğini asimetrik yap. DB'yi domain başına ayır (önce roller, sonra instance). | Mevcut yatırım korunur; büyüyünce ölçek kolay. | Event altyapısı + read-model tutarlılık işi (eventual consistency). Operasyon yükü devam eder. |
| **C. Hibrit** | auth+user+match+subscription (senkron bağımlı grup) tek "core" uygulamasında birleşir; chat (WebSocket, farklı ölçek profili), notification (dış sağlayıcılar) ve backoffice ayrı kalır. | Zincirin %90'ı in-process; chat bağımsız ölçeklenir. | İki mimari stil aynı anda yaşar; sınır disiplinine dikkat gerekir. |

**Öneri:** Ekip 1–3 geliştirici ise **C** (hibrit) en yüksek getiri/maliyet oranını verir; **A** da savunulabilir. Ekip büyüyecek ve ölçek yakınsa **B**. Hangisi seçilirse seçilsin, 2.4'teki read-model işi **her durumda** gerekli, çünkü chat ve notification zaten ayrı kalacak.

> Bu bir "mikroservis kötü" tezi değil. Tez şu: mikroservisin gerçek faydası *bağımsız* deploy, ölçek ve hata izolasyonudur; bu sistemde üçü de yok. Faydayı almadan maliyeti ödemek yerine ya faydayı gerçekten alın (B) ya da maliyeti düşürün (A/C).

### 2.4 Her durumda yapılacak: sıcak yoldaki senkron okumaları kaldırmak

Swipe ve mesaj gönderimindeki dört kontrolün hepsi **okuma**: "hesap yasal onaylı mı", "gizli modda mı", "engel var mı", "hakkı var mı". Bunların üçü nadiren değişir ve **event ile replike edilebilir**:

```
user-core / auth-core                       match-core, chat-core
  account standing değişti  ──event──►  local tablo: account_standing(account_id, banned, hidden,
  (ban, hidden, legal_ok, block)                      legal_ok, blocked_ids[], revision, updated_at)
```

- Kontroller local DB okumasına döner (aynı transaction içinde bile yapılabilir).
- Engel (block) için **fail-closed** kalır: local tabloda satır yoksa ya da `updated_at` eşikten eskiyse o zaman senkron sor. Bugünkü Redis "engel yok" cache'i (30 sn) zaten eventual consistency'yi kabul ediyor; read-model bunu açık ve denetlenebilir hale getirir.
- **Legal onay** için daha ucuz yol: onay durumu **user JWT claim'i** olur (`legal_ok=true`, `legal_rev=N`). Yeni belge yayınlanınca `sv` (session version) artırılır → token yenilenir → claim güncellenir. Gateway kontrol eder; servisler auth-core'a hiç sormaz.
- **Hak tüketimi** (consumeRight) zaten saga ile yazma; o senkron kalır. Ama tek kalan uzak çağrı olur.

Sonuç: swipe = 1 uzak çağrı (subscription) + local DB. Availability ≈ %99,8, p99 yarıya iner.

---

## 3. Monorepo Sorun mu?

**Kısa cevap: Hayır.** Bu ekip büyüklüğü ve bu bağımlılık grafiği için monorepo doğru karar. Polyrepo'ya geçmek şu an sahip olunan şeyleri kaybettirir:

- Contract (`*-api`) değişikliği ve tüketicileri **tek PR'da, atomik** değişiyor. Polyrepo'da bu 3–5 PR + versiyon yayınlama + bağımlılık güncelleme döngüsü olur.
- `AGENTS.md`, skills, Flyway hook, review checklist — tek kopya, tek CI.
- IDE'de tüm sistemi tek projede refactor edebilme.

### 3.1 Gerçek sorunlar (monorepo'nun değil, pipeline'ın)

| Sorun | Etki | Çözüm |
|---|---|---|
| Her PR'da `mvn verify` **tüm** modülleri derleyip test ediyor | CI süresi modül sayısıyla lineer büyüyor | Affected-module tespiti: **gitflow-incremental-builder** (GIB 4.7.0; `buildDownstream`, `buildUpstreamMode=impacted`, referans branch'e göre yalnız değişen modül + bağımlıları) veya **Maven Build Cache Extension** (1.3.0, Ağustos 2026; girdi hash'ine göre modül çıktısını cache'ler, Maven 3.9+/4.x). En basit hali: `dorny/paths-filter` + servis başına GitHub Actions matrix + `mvn -pl <modül> -amd`. Gradle/Bazel'e geçiş ~1000 dosyada haklı çıkmaz. |
| Dockerfile'lar `COPY . .` + `mvn install -pl <liste> -am` | Her image build'i tüm bağımlılık ağacını yeniden derler; `-pl` listesi elle tutuluyor ve **yanlış** (referans §22.2 #29: backoffice'te `notification-api` eksik) | Jib (`jib-maven-plugin` zaten pom'da tanımlı, hiç kullanılmıyor) veya Spring Boot layered jar + Buildpacks. `-pl` listesi Maven reactor'dan otomatik türer. |
| Build **prod sunucusunda** yapılıyor | Prod CPU/disk build'e gidiyor; "bende çalışıyordu" image'ı yok; rollback = eski commit'i yeniden build (dakikalar, hata riski) | CI'da image build → GHCR'a push → deploy yalnız `docker compose pull` + `up`. Image tag = git SHA, deploy digest ile. Rollback = önceki digest. |
| `*-api` modülleri versiyonsuz | Bir servis yeni api ile derlenirken diğeri eskisiyle çalışıyor olabilir (deploy sırası kuralı buna bağlı) | Monorepo'da bu **kabul edilebilir**: contract değişiklikleri geriye uyumlu tutuluyor (referans §6.9). Bağımsız versiyonlama yalnız birlikte deploy etmeyi bıraktığınızda gerekir; o güne kadar `${revision}` (CI-friendly version) + uyumluluk testleri yeter. `@since`/deprecation etiketi ve consumer-first deploy kuralı yazılı olmalı; zaten var. |
| `common-security` bir "god library": JWT, filtreler, exception, rate limit, saga store, tracing, SMS | Her değişiklik 9 servisi rebuild ediyor; test yüzeyi belirsiz | Referans §21.1'deki öneri doğru: `platform-core`, `platform-security`, `platform-observability`, `platform-consistency` olarak **gerçek Spring Boot starter**'lara (`AutoConfiguration.imports`) böl. |
| Split package (api ve core aynı paket kökü) | Sahiplik belirsiz; ileride JPMS/modülerlik kapalı | `com.<org>.<servis>.api.*` (subscription-api zaten böyle) |
| `payment`, `validation` iskelet modülleri build'de | Gereksiz derleme; "var ama yok" kafa karışıklığı | Sil; ihtiyaç olunca git history'den al. |

### 3.2 Ne zaman polyrepo düşünülür?

Literatür net: Google'ın monorepo makalesi (Potvin & Levenberg, CACM 2016) faydaları atomik değişiklik, diamond-dependency sorununun olmaması ve büyük ölçekli refactor olarak sayar; maliyeti ise **araç yatırımı** (affected-target tespiti, build cache) ve "implementasyon detayına bağımlanma" riskidir. Sam Newman'ın (*Building Microservices*, 2. baskı) ölçütü ise "bağımsız deploy edilebilirlik": servisler arası değişiklik **sık** oluyorsa sorun repo değil, **sınırlar**dır. Plate'te `common-security` değişikliklerinin 9 servisi rebuild etmesi tam bu sinyal (§3.1 son satır).

Polyrepo şu koşullarda düşünülür:
- Birden fazla ekip, ayrı release kadansı ve **ayrı on-call** olduğunda.
- Bir servis başka bir ürün/şirketle paylaşıldığında.

Bu koşullar oluşana kadar monorepo. Oluşursa da önce `*-api` modüllerini Maven repository'ye (GitHub Packages) semver ile yayınlamak, sonra core'ları ayırmak doğru sıra.

---

## 4. Veri Katmanı

### 4.1 Tek PostgreSQL, şema/servis — 🔴 yedekleme, 🟠 izolasyon

**Bugün doğru karar.** Database-per-service, küçük ekipte operasyon yükünü (9 yedek, 9 izleme, 9 bağlantı havuzu) haklı çıkarmaz. Şema/servis, "mantıksal ayrım + tek operasyon" dengesinin bilinen orta yolu. Referanslar da bunu söylüyor: Microsoft Azure Architecture Center — "iki servis aynı veri deposunu paylaşmamalı… ama aynı **fiziksel** sunucuyu güvenle paylaşabilir; sorun aynı şemayı/tabloları paylaşınca başlar." AWS Prescriptive Guidance "shared-database-per-service"i **geçiş deseni** olarak kabul eder (servis başına özel tablo/şema şartıyla) ve tek DB'nin tek hata noktası olduğunu vurgular. microservices.io "shared database"i anti-pattern sayar; "database per service"in üç uygulama biçiminden biri zaten **schema-per-service**.

Pratik merdiven: **(a)** servis başına rol + şema (şimdi) → **(b)** aynı instance'ta servis başına ayrı *logical database* (bağımsız `pg_dump/restore`, extension) → **(c)** en sıcak domain (chat) için ayrı instance → **(d)** tam database-per-service. Bugün (a), 12 ay içinde muhtemelen (c).

**Ama dört şey eksik:**

**(a) DB seviyesinde izolasyon yok.** Referans dokümana göre tüm servisler `${DB_USER}` ile bağlanıyor — büyük olasılıkla tek kullanıcı (**doğrulanmalı**). "Cross-schema erişim yasak" kuralı yalnız `BackofficeMigrationBoundaryTest` gibi testlerle ve konvansiyonla korunuyor. Bir `nativeQuery` ile `users.user` tablosuna backoffice'ten erişmek teknik olarak mümkün. Bu hem mimari hem **yetki** sorunudur (referans §10.1 kendisi de öyle diyor).

Çözüm ucuz:
```sql
CREATE ROLE svc_match LOGIN PASSWORD '...';
GRANT USAGE ON SCHEMA match TO svc_match;
GRANT ALL ON ALL TABLES IN SCHEMA match TO svc_match;
ALTER DEFAULT PRIVILEGES IN SCHEMA match GRANT ALL ON TABLES TO svc_match;
-- svc_match başka şemayı göremez; cross-schema join çalışma zamanında hata verir.
```
Flyway her servis için kendi rolüyle koşar (şema sahibi o rol). Sınır artık **çalışma zamanında** zorlanır; ileride şemayı ayrı instance'a taşımak da kolaylaşır (rol ve şema hazır).

**(b) Yedekleme / PITR / HA belgelenmemiş.** Referans dokümanda (README dahil) `pg_dump`, WAL arşivi, replica, RPO/RTO kelimeleri geçmiyor. Tek host + tek Postgres = tek disk arızası ile tüm ürün verisi. Bu, listedeki en ucuz ve en yüksek getirili iş:
- **Minimum:** sürekli WAL arşivi + gecelik base backup → DigitalOcean Spaces (S3 uyumlu). Araç: **WAL-G** (Apache-2.0, 3.0.9 – Ağustos 2026; S3 uyumlu, delta backup, şifreleme; "<100 GB, doğrudan object storage" senaryosu için en basit) veya **pgBackRest** (2.59.2 – Eylül 2026; paralel, blok-artımlı, daha güçlü retention/doğrulama. Not: Nisan 2026'da tek bakımcısı projeyi arşivledi, haftalar içinde AWS/Supabase/Percona vb. sponsorluğuyla canlandı — tek bakımcılı araç riskini gösteren bir olay). Retention 7–30 gün, şifreli, **aylık restore provası**, arşiv gecikmesi alarmı.
- **Tercih:** DigitalOcean Managed PostgreSQL (1 GiB'den başlıyor, standby node tam fiyat; 7 gün PITR, otomatik failover, dahili PgBouncer — fiyat/özellik **doğrulanmalı**). Patroni gibi HA araçları ≥3 DCS node ister; tek host'ta anlamsız. Tek compose host'unda araç ne olursa olsun **HA yoktur**; HA istiyorsanız ya managed ya ikinci host + streaming replica.
- Redis için AOF (`appendfsync everysec`); S3 için bucket versioning. Bunlar da belgelenmeli.

**(c) Bağlantı bütçesi ve gürültülü komşu.** 9 servis × `maximum-pool-size 10` + poller'lar = 90+ bağlantı; instance sayısı 2'ye çıkınca 180. PostgreSQL'de her bağlantı bir OS process'i (birkaç MB). HikariCP'nin kendi rehberi havuz boyutunu `(çekirdek × 2) + disk` civarında tutmayı ve "daha az bağlantı daha hızlıdır" ilkesini önerir. **PgBouncer** (transaction mode; varsayılan `default_pool_size 20`; 1.21+ protokol seviyesinde prepared statement destekliyor) tek host'ta bile değer. Ayrıca `pg_stat_statements` açık olmalı; şu an DB gözlemlenebilirliği yok (Prometheus'ta postgres-exporter da yok).

**(d) Tablo hijyeni.** Outbox, log, audit, `login_history`, `notification_event_log` gibi sürekli büyüyen tablolar için retention/partition yok (yalnız `MessageRetentionJob` var, kapalı). `audit_log` `@Immutable` ama DB tarafında `REVOKE UPDATE/DELETE` yok.

### 4.2 Chat farklı bir veritabanında mı tutulmalı? — 🟡

**İş yükü analizi.** Chat mesajı tablosu:
- Append-only (mesaj değişmez; okundu bilgisi ayrı).
- Doğal partition key = `conversation_id`, sıralama = `created_at, id`.
- İçerik **opak** (E2EE ciphertext, ≤16 KB); sunucu hiç parse etmiyor → arama, join, ikincil index ihtiyacı **yok**.
- Okuma deseni = "son N mesaj" ve keyset ile geriye kaydırma.
- Silme deseni = retention (zaman bazlı) ve sohbet kapanınca.

Bu profil **wide-column** (Cassandra/ScyllaDB) için ders kitabı örneği. Discord'un yolu tam bu: 2015'te tek MongoDB replica'sı ~100 milyon mesajda (veri + index RAM'e sığmayınca) zorlandı → 2017'de 12 node Cassandra (milyarlarca mesaj) → 2022'de trilyonlarca mesajda 177 Cassandra node'u (hot partition, JVM GC, compaction birikimi) → **ScyllaDB** 72 node; okuma p99 40–125 ms'den 15 ms'ye, yazma p99 5–70 ms'den 5 ms'ye (Discord blog, Mart 2023). Slack ise MySQL'de kaldı ve Vitess ile shard'ladı; WhatsApp mesajı yalnız teslimata kadar tutar (karşılaştırılabilir değil). Yani "chat = Cassandra" değil; "chat = **ölçekte** partition'lanabilir append-only depo".

**Bugün taşınmamalı.** PostgreSQL bu iş yükünü, doğru tasarlanırsa, düşük milyarlarca satıra kadar taşır. Referans dokümandaki mevcut tasarım ise eşiği gereksiz öne çekiyor:

| Mevcut | Sorun | Düzeltme |
|---|---|---|
| Ciphertext base64 `TEXT` | %33 depolama şişmesi. ≤16 KB'lık her mesaj zaten TOAST'a gidiyor (eşik ~2 KB); TOAST önce **sıkıştırmayı dener**, şifreli veri sıkışmaz → CPU boşa | `BYTEA` + `ALTER TABLE … ALTER COLUMN ciphertext SET STORAGE EXTERNAL` (sıkıştırma denemesini atlar) |
| Partition yok | Retention = `DELETE` → bloat, vacuum yükü; index büyüdükçe insert yavaşlar | Aylık **declarative partitioning** (**pg_partman 5.x**: otomatik partition oluşturma, background worker, `retention_keep_table=false` ile eski partition **drop**; PG ≥14). `DROP PARTITION` metadata işlemi; `DELETE + VACUUM` değil. `created_at` üzerinde **BRIN** index (append-only + fiziksel sıra korelasyonu için tasarlanmış, çok küçük). |
| UUIDv4 PK | Rastgele insert → B-tree sayfaları dağılır, WAL büyür | UUIDv7 (zaman sıralı) — bkz. 4.5 |
| Aynı instance'ta diğer 8 şema ile | Chat yazma yükü auth/subscription'ın disk IO'sunu etkiler | **Önce ayrı PostgreSQL instance** (rol ve şema hazır olduğu için taşıma = dump/restore + URL değişimi). Bu, "chat'i ayır" isteğinin %80'ini karşılar. |

**ScyllaDB/Cassandra'ya geçiş eşiği** (bunlardan biri tutarsa ciddi düşünülür; sayılar sektör sezgisi, kesin kaynak yok):
- Mesaj tablosu **> 1–2 TB** veya **> 2–3 milyar satır** (partition'lı Postgres'te bile vacuum/backup pencereleri sorun olur).
- Sürekli yazma **> 10 bin mesaj/sn** (iyi donanımlı tek Postgres primary'nin pratik sınırı bu civarda; bunun için ~1M aktif kullanıcı gerekir). ScyllaDB'nin "7,5M insert/sn" gibi rakamları **cluster** rakamıdır.
- Çoklu bölge (multi-region) aktif-aktif yazma ihtiyacı; lineer çok-node yazma ölçeği.
- Chat için ayrı bir ekip ve on-call.

Geçiş olursa: chat-core zaten ayrı servis, veri erişimi repository arkasında; `chat.messages` ScyllaDB'ye giderken `conversation`, `membership`, `report_outbox` Postgres'te kalır (onlar transactional). MongoDB **önerilmez** (bu iş yükünde artı getirmez, operasyon ekler).

**Realtime katmanı ayrı konu:** WebSocket oturum/presence/fan-out zaten Redis'te; DB seçimi bunu etkilemez.

### 4.3 Elasticsearch: tek use-case için ağır bileşen — 🟡

ES yalnız user-core'da, `users` index'inde, geo sorgu için (referans §2.1). Bunun için:
- Ayrı JVM, ayrı bellek (tek host'ta en az 1–2 GB), ayrı yedek, ayrı sürüm takibi (8.10.4 → destek dışı, bkz. §9).
- ES ↔ Postgres senkronizasyonu (`UserSearchRepairJob` var → tutarsızlık zaten yaşanmış).

**Alternatifler:**
- **PostGIS** (`geography` + GiST, `ST_DWithin` yarıçap filtresi, `ORDER BY loc <-> :nokta` ile index destekli gerçek-mesafe KNN — geography için 2.2'den beri): tek DB, transaction içinde tutarlı, milyonlarca kullanıcıda yeterli. Öneri/filtreleme (yaş, cinsiyet, ilgi) SQL `WHERE` ile. Tinder'ın geoshard'lı ES'i on milyonlarca kullanıcı ve 100 mil yarıçap için gerekti; Türkiye pazarı ölçeğinde "spatial için search cluster" erken optimizasyon.
- **Redis GEO** (`GEOSEARCH`, 52-bit geohash sorted set): presence ile aynı yerde; hızlı ama **öznitelik filtresi yok** (uygulamada post-filter), Haversine hatası %0,5'e kadar.
- ES/OpenSearch'ü **recommendation** büyüyünce (metin, çok kriterli skorlama, vektör arama) geri getirin. Şu an "recommendation snapshot" Redis'te; ranking mantığı ES'te değil.

Eğer ES kalacaksa karar zorunlu: 8.10.4 hiç yama almıyor (8.x'in son sürümü 8.19.22, Eylül 2026; 8.x bakım sonu Ocak 2027). Lisans: 8.10 dalı **yalnız SSPL + ELv2**; AGPL seçeneği 8.16+ ve 9.x'te (9.5.4, Eylül 2026). **OpenSearch** 3.8 (Ağustos 2026) Apache-2.0, Linux Foundation altında. Spring Data ES ile OpenSearch uyumu için `opensearch-java` client'ı gerekir; geçiş maliyeti tek index için düşük.

### 4.4 Redis: sekiz sorumluluk, tek instance — 🟠

Redis burada cache değil, **güvenlik ve tutarlılık altyapısı**: rate limit (fail-closed 503), JTI replay (fail-closed 401), session version (gateway'de **fail-open**, chat'te fail-open), IP block (fail-closed 503), presence, snapshot cache, pub/sub fan-out, block-relation cache.

Sorunlar:
- **HA yok** (referans dokümanda Sentinel/Cluster geçmiyor; **doğrulanmalı**). Redis düşerse: OTP/login 503, servisler arası çağrılar 401 (replay guard), swipe 503 (rate limit). Yani Redis = tüm ürün.
- **Eviction politikası güvenlik key'lerini silebilir.** Aynı instance'ta cache ile güvenlik state'i varsa ve `maxmemory-policy allkeys-*` ise Redis **JTI, session-version ve rate-limit key'lerini de** evict eder → replay koruması sessizce kalkar, oturum iptali unutulur. Redis'in kendi dokümanı cache ile kalıcı key'ler karışınca "iki ayrı instance çalıştırın" der. Mevcut ayar **doğrulanmalı**.
- **Fail-open / fail-closed tutarsız.** Session iptali fail-open (banlı kullanıcı Redis kesintisinde devam eder), rate limit fail-closed (Redis kesintisinde kimse giriş yapamaz). Her ikisi de savunulabilir ama **birlikte** tuhaf: aynı kesintide "güvenlik kontrolü atlanır" ve "herkes kilitlenir" aynı anda oluyor. Karşılaştırma: Stripe'ın rate limiter'ları bilinçli olarak **fail-open** (Redis yoksa limit uygulanmaz); replay guard'da fail-open güvenlik açığı, fail-closed kesinti — karar key tipine göre açıkça verilmeli.
- **Her istekte Redis'e yazma** (JTI `SET NX`). TLS + 30–45 sn TTL varken per-request replay guard'ın kazandırdığı güvenlik marjinal, maliyeti her isteğe bir Redis round-trip. Bkz. §6.1.
- **Lisans ve bakım:** 7.2 ve öncesi BSD; 7.4 (Mart 2024) RSALv2/SSPL; 8.0 (Mayıs 2025) buna AGPLv3 ekledi. 7.x dalının son sürümleri Ağustos 2025'te çıktı, o zamandan beri yok → **"Redis 7" fiilen bakımsız**. Güncel: Redis 8.10.x. **Valkey** (Linux Foundation, 7.2.4 fork'u, BSD; 9.1.x – Ağustos 2026; DigitalOcean destekçiler arasında) drop-in alternatif.
- **Sentinel** en az 3 Sentinel (bağımsız hata alanlarında) ister ve asenkron replikasyon nedeniyle onaylanmış yazmayı kaybedebilir (`min-replicas-to-write 1` ile azaltılır). Bu uygulama için doğru boy Sentinel'dir; Cluster (≥3 master, 6 node) yatay ölçek içindir, bugün gereksiz.

Öneri: (1) Sentinel veya managed Redis/Valkey; (2) güvenlik kararı veren key'leri (rate limit, session, ip-block, JTI: `noeviction` + AOF) ile cache/pub-sub'ı (`allkeys-lru`) **ayrı instance**'a ayır; (3) fail politikasını key tipi başına tek tabloda belgele; (4) Valkey 9'a geç.

### 4.5 Kimlik üretimi: UUIDv4 → UUIDv7 — 🟢

73 entity'nin 63'ü UUID, 5 farklı üretim yöntemi (referans §10.3). UUIDv4 rastgele; B-tree PK'da sayfa bölünmeleri ve WAL şişmesi yaratır (özellikle mesaj, outbox, log gibi yüksek insert tablolarında). UUIDv7 zaman sıralı: insert'ler sona eklenir, `created_at DESC, id DESC` keyset sıralaması id ile doğal olarak uyuşur. Ölçümler: v7 ile B-tree ~%26–27 daha küçük ve yaprak sayfalar ardışık; bazı benchmark'larda insert ~%49 daha hızlı, `ORDER BY id` ~3× (credativ, PG 18 üzerinde).

- Java: **`uuid-creator`** 6.1.x (MIT; `UuidCreator.getTimeOrderedEpoch()`) veya **Java Uuid Generator** 5.1 (Apache-2.0). Hibernate'in `@UuidGenerator(style = VERSION_7)`'si **yalnız ORM 7'de** (`@Incubating(since="7.0")`, yani Spring Boot 4); Boot 3.2.5'in Hibernate 6.4.4'ünde yok — "6.5+" diyen bloglar yanlış.
- PostgreSQL 18 (GA Eylül 2025; 18.6 – Ağustos 2026): native `uuidv7()` (RFC 9562, backend içinde monotonik) ve `uuid_extract_timestamp()`. PG 15'te yok.
- Tek yöntem: ortak modülde tek generator (`platform-core`), `@PrePersist` ve elle atama kalkar; PG 18'e geçince DB default `uuidv7()` de eklenir.

### 4.6 Outbox çeşitliliği: 11 tablo, 3 desen — 🟡

Referans §11.2 envanteri: `retry_count`/`attempt_count`, lease var/yok, `claim_token` var/yok, DEAD var/yok, "eski desen" (`ProvisioningOutboxPoller`: kilitsiz, TX içinde Feign). Aynı problem 7 kez, 3 farklı kalitede çözülmüş. Her yeni outbox aynı hataları tekrar etme riski taşıyor (referans §22.2 #16: auth outbox'ta lease aşımı).

Seçenekler:
1. **Tek generic outbox** (`platform-consistency` starter): Debezium'un önerdiği şema — `outbox_event(id, aggregate_type, aggregate_id, event_type, payload jsonb, headers jsonb, status, ...)` — + tek poller + hedef bazlı `OutboxHandler` arayüzü. Tüm servisler aynı tabloyu kendi şemasında kullanır. Chris Richardson'ın sınıflamasında bu "polling publisher": her DB'de çalışır, gecikme poll aralığına bağlı (1 sn poll ≈ 500 ms p50), Kafka/Connect yoksa "**saner default**". **Bugün için doğru seçim.**
2. **Debezium Outbox Event Router** (transaction log tailing): poller'lar tamamen kalkar; Debezium WAL'dan okur (~50–200 ms gecikme), `outbox.event.<aggregate_type>` topic'ine yönlendirir. **Debezium Server** (3.6.x, Eylül 2026) Kafka Connect gerektirmeden çalışır ve 19 sink'i var: Kafka, **RabbitMQ (streams)**, **Redis Streams**, NATS JetStream, HTTP… Operasyon riski: `wal_level=logical` + replication slot; connector durursa veya yakalanan tablo boşta kalırken başka tablolar yazarsa **WAL disk'i doldurur** — `heartbeat.interval.ms` + `heartbeat.action.query` ve `max_slot_wal_keep_size` (PG13+; aşılırsa slot geçersizleşir → yeniden snapshot) + slot lag alarmı şart. Tek host'ta bu risk gerçek; 7 poller'ın bakımı buna değmez **henüz**.
3. **Spring Modulith event publication registry**: iş transaction'ı içinde publication-log satırı; `@Externalized` ile Kafka/AMQP'ye dışa aktarma; `IncompleteEventPublications.resubmit`. Yalnız §2.3-A/C seçilirse ve Boot 3.2.5'ten çıkılınca (Modulith 1.4.x → Boot 3.5, 2.x → Boot 4).

Hangisi seçilirse: Feign'e giden outbox'lar (`user_block_outbox`, `account_status_outbox`, `report_outbox`, `moderation_action_outbox`, `user_notification_outbox`) aslında **event**: "kullanıcı X, Y'yi engelledi", "hesap X banlandı", "şikayet oluştu". Bunlar event olarak yayınlanırsa tüketici match, chat, auth, backoffice kendi subscribe eder; user-core'un kimi çağıracağını bilmesi gerekmez. Bugünkü model "outbox üzerinden RPC": user-core hem event'i hem tüketicinin adresini biliyor.

---

## 5. Mesajlaşma ve Event Mimarisi: Kafka Sorusu

### 5.1 Bugünkü durum dürüstçe

- RabbitMQ yalnız **komut** taşıyor: "şu numaraya SMS gönder", "şu kullanıcıya push at". İki exchange/queue. Bu iş için RabbitMQ **doğru** araç (per-message ack, DLQ, TTL). RabbitMQ'nun kendi karşılaştırma sayfası da aynı çizgiyi çiziyor: task queue, RPC, öncelik, mesaj başına durum → RabbitMQ; yüksek hacimli stream, stateful stream processing, log compaction → Kafka.
- Sistemde **domain event yok**. `SwipeMade`, `MatchCreated`, `MessageSent`, `ProfileUpdated`, `UserBlocked`, `SubscriptionActivated`, `ReportFiled` gibi olaylar hiçbir yerde yayınlanmıyor. Yan etkiler outbox → Feign ile **noktadan noktaya** uygulanıyor.
- Sonuç: analytics yok (§11.2), recommendation için sinyal yok, fraud/abuse tespiti için akış yok, read-model replikasyonu (§2.4) yapılamıyor, olay geçmişi tekrar oynatılamıyor.
- **Consumer tarafı tehlikeli biçimde ayarsız** (referans §12.3). Spring AMQP'de `defaultRequeueRejected=true` **varsayılandır**: iş hatası fırlatan mesaj "sonsuza kadar yeniden teslim edilebilir" (Spring dokümanının kendi ifadesi). Varsayılan `prefetchCount` 250 — tek consumer 250 mesajı alıp DB kesintisinde hepsini `ImmediateRequeue` ile döndürür → sıcak döngü. DLQ replay aracı yok.
- **RabbitMQ 3.13'ün community desteği 17 Eylül 2024'te bitti** (son community patch 3.13.7). Bugün community-destekli tek seri **4.3.x** (Nisan 2026; 4.3.6 – Eylül 2026; Erlang 27+). Yükseltme yolu: 3.13 → 4.2.x → 4.3 (4.3'e yalnız 4.2'den geçilir) veya blue/green. 4.0'da classic **mirrored** queue kalktı (classic queue kaldı ama replikasız); 4.3'te Mnesia kalktı (Khepri).

### 5.2 Kafka ne zaman mantıklı, bu sistemde nerede?

Kafka'nın RabbitMQ'ya göre **farklı** olduğu yerler (daha iyi değil, farklı):

| Özellik | Kafka (4.x) | RabbitMQ (4.3, quorum queue + streams) |
|---|---|---|
| Aynı olayı **birden çok bağımsız tüketici grubu** okur (analytics + recommendation + moderasyon) | Doğal (consumer group, offset) | Topic exchange + tüketici başına queue; olur ama her tüketici için queue yönetimi. **Streams** ile doğal. |
| **Replay** (geçmişi baştan okuma: yeni read-model, bug sonrası yeniden işleme) | Doğal (retention + offset reset) | Queue'da yok (mesaj tüketilince gider); **RabbitMQ Streams** (append-only log, non-destructive read, broker'da offset, max-age/max-bytes retention) ile var. Spring: `spring-rabbit-stream`. |
| Sıralama garantisi (partition key = account/conversation) | Doğal | Single active consumer ile kısıtlı |
| Uzun retention / audit / veri ambarına CDC | Doğal | Streams ile kısmen |
| Per-message ack, **gecikmeli retry**, DLQ, öncelik, TTL | Zayıf: `DefaultErrorHandler` = 10 anlık deneme, **partition'ı bloke eder**; `@RetryableTopic` sıralamayı bozar; Kafka 4.2 "share groups" (Şubat 2026) per-message ack ekledi ama TTL/öncelik/gecikmeli retry/DLX yok | Doğal: QQ `delivery-limit` (varsayılan 20 → DLX), 4.3'te **native gecikmeli retry** (`x-delayed-retry-*`, lineer backoff), 32 öncelik, `x-death` başlıkları |
| Operasyon (tek host) | KRaft (4.0'dan beri ZooKeeper yok) ama broker varsayılan 1 GB heap + page cache + partition planlama; **Redpanda** (C++, tek binary; BSL 1.1 — üretimde ücretsiz) daha hafif; **NATS JetStream** (Apache 2.0) daha hafif ama Kafka API değil | Hafif; zaten var |

**Bu sistemde Kafka'nın anlamlı olacağı somut use-case'ler:**
1. **Domain event stream** → analytics/warehouse (ClickHouse/BigQuery), recommendation feature'ları, fraud/abuse skorlama, moderasyon kuyruğu besleme.
2. **Read-model replikasyonu** (§2.4): `account_standing` gibi projeksiyonları yeni bir servise **replay ile** sıfırdan kurabilme.
3. **CDC** (Debezium) ile outbox'ları poller'sız yayınlama.
4. **Audit/olay geçmişi**: "bu kullanıcı bu hesabı ne zaman engelledi/açtı" gibi soruları DB'yi sorgulamadan cevaplama.

**Kafka'nın anlamsız olacağı yerler:** SMS/mail/push komutları (RabbitMQ kalmalı), servisler arası saga adımları (senkron + idempotent kalmalı), WebSocket fan-out (Redis kalmalı). Gunnar Morling'in (Debezium) 2025 notu: "queuing, Kafka'nın tipik use-case'i değil." Sektör anketleri Kafka kullanıcılarının ~%55'inin 1 MB/sn'nin altında olduğunu söylüyor (ikincil kaynak) — yani çoğu kurulum, Kafka'nın tasarlandığı hacmin çok altında ve operasyon yükünü boşuna taşıyor. Sık tekrarlanan Kafka operasyon ağrıları: rebalance, upgrade, kapasite planlama.

### 5.3 Öneri: üç aşamalı yol (Kafka'ya bugün geçmeden)

**Aşama 0 — Broker'ı ve consumer'ları düzelt (1 hafta, RabbitMQ 4.3'e geçişle birlikte):**
- Queue'ları **quorum queue** yap (`delivery-limit` 20 varsayılan; `dead-letter-strategy=at-least-once`).
- `defaultRequeueRejected=false` **veya** `AmqpRejectAndDontRequeueException`; iş hatası → DLQ, altyapı hatası → `RetryInterceptorBuilder.stateful()` + exponential backoff + `RepublishMessageRecoverer`. Açık `prefetch` (10–50) ve `concurrency`.
- **Delayed Message Exchange plugin'ini kullanmayın**: 24 Eylül 2026'da arşivlendi, Mnesia tabanlı (4.3'te kalktı). Gecikmeli retry için QQ'nun native `x-delayed-retry-*` özelliği (4.3).
- DLQ'dan yeniden oynatma komutu (küçük bir admin uç veya CLI).

**Aşama 1 — Event kavramını sisteme sok (RabbitMQ ile, 2–4 hafta):**
- Standart **event envelope**, **CloudEvents 1.0** şemasında: zorunlu `id, source, specversion, type`; opsiyonel `subject` (aggregate id), `time`, `datacontenttype`, `dataschema`, `data`; `traceparent` extension. Not: CloudEvents'in AMQP binding'i yalnız AMQP **1.0** için; AMQP 0-9-1 (Spring AMQP) ile header eşlemesini kendiniz yaparsınız — sorun değil, belgelenmeli.
- `domain.events` **topic exchange**; routing key `<servis>.<aggregate>.<olay>` (`user.account.blocked`, `match.match.created`). Tüketici başına bir queue + DLQ.
- Şema evrimi kuralı: geriye uyumlu değişiklik (opsiyonel alan ekleme) `type`'ı değiştirmez; kırıcı değişiklikte yeni `type` ve bir süre **iki olay birden** üretilir (CloudEvents primer). Tüketici bilinmeyen `type`'ı **yok sayar** (bugün DLQ'ya düşürüyor — referans §6.9; event stream'de bu yanlış). Consumer-first deploy kuralı korunur. Schema registry (Apicurio, Apache 2.0) bu ölçekte opsiyonel.
- Outbox'lar Feign yerine event yayınlar; tüketiciler idempotent inbox (`ON CONFLICT (event_id) DO NOTHING`, zaten var).
- **Kazanç:** §2.4 read-model'i (Richardson'ın "command-side replica" deseni: `ConsentRecorded`, `HiddenModeChanged`, `BlockCreated`, `AccountBanned` olayları match/chat'te local tabloya yazılır, swipe local bayraklara bakar), §11.2 analytics'in ilk adımı (bir "event sink" tüketicisi olayları Postgres/ClickHouse'a yazar), user-core'un tüketicileri tanıması biter.

**Aşama 2 — Replay ve çoklu tüketici gerekince: RabbitMQ Streams (aynı broker):**
- `domain.events` için stream (max-age retention, non-destructive read); analytics/recommendation gibi "geçmişi baştan oku" tüketicileri stream'den okur, işlemsel tüketiciler queue'dan. Kafka'nın replay faydasının çoğu, yeni bileşen eklemeden.
- Debezium'a geçiş (poller'sız outbox) bu aşamada değerlendirilir (§4.6 seçenek 2; WAL riski).

**Aşama 3 — Kafka/Redpanda (şu koşullardan ≥2'si oluşunca):**
- Aynı olayı ≥3 bağımsız tüketici okuyor, replay ve **stateful stream processing** (Kafka Streams/Flink) ihtiyacı çıktı.
- Günlük olay hacmi on milyonları geçti; RabbitMQ stream'leri yetmiyor.
- Veri/analitik ekibi kuruldu veya warehouse'a CDC gerekiyor.
- Birden fazla host'a geçildi (Kafka'nın tek host'ta anlamı az).

Aşama 1'de envelope ve topic yapısı doğru kurulursa Aşama 3 yalnız transport değişimidir; üretici/tüketici kodu (Spring Cloud Stream veya kendi soyutlamanız) aynı kalır.

### 5.4 WebSocket fan-out: Redis pub/sub yeterli mi?

At-most-once ("bir mesaj en fazla bir kez teslim edilir, hiç edilmeyebilir" — Redis dokümanı), kalıcılık yok, abone olmayan pod'a mesaj gitmez. Mobil tarafın "realtime + history pull" yapması (referans §13) bunu **tolere edilebilir** kılıyor; Centrifugo'nun tasarım dokümanı da aynı deseni önerir: önce DB'ye yaz, sonra pub/sub ile it, istemci yeniden bağlanınca **sıra numarasıyla** kaçırdıklarını çeker. Eksik olan muhtemelen bu **sıra numarası** (mesaj tablosunda sohbet başına monoton `seq`): istemci "son gördüğüm seq = N" diyerek boşluğu tespit edebilmeli (**doğrulanmalı**).

Alternatifler ancak şu durumlarda: Redis kesintisinde mesajların "kaybolmuş gibi" görünmesi ürün sorunu olursa → **Redis Streams** (consumer group, `XACK`/`XAUTOCLAIM`, `MAXLEN` trimming; Socket.IO'nun Streams adapter'ı bu yüzden pub/sub adapter'ının yerine geldi) veya **Centrifugo** gibi ayrı realtime sunucu (history + recovery ile retention içinde at-least-once; STOMP/SockJS de kalkar; Flutter için SockJS zaten gereksiz). Kafka bu iş için değil.

---

## 6. Güvenlik Mimarisi

### 6.1 Servis kimliği: paylaşılan HS256 secret — 🟠

Referans §9.2 "Dikkat" zaten söylüyor; burada nedeni ve çözüm sırası:

- **Risk:** Simetrik MAC'te her doğrulayıcı aynı zamanda imzalayıcıdır. Secret'ı bilen her bileşen (9 servis, config-server, sızan bir `.env` — referans §22.2 #27: `.env` build context'e giriyor) `act=backoffice-service, sub=<herhangi hesap>` token'ı basabilir. Bir servisteki tek SSRF/RCE/deserialization açığı = tam yatay hareket + kullanıcı taklidi; **actor allowlist'i yalnız dürüst çağıranları kısıtlar**, güvenlik sınırı değildir. `iss=gateway` her yerde olduğundan issuer bir güven sinyali taşımıyor; `act` token sahibinin kendi beyanı.
- **RFC 8725 (JWT BCP)** §3.5: düşük entropili HS256 anahtarı (config'teki literal default gibi) ele geçirilen **tek** token'dan offline brute-force ile bulunur; §3.11–3.12: farklı JWT türleri (user/admin/service) için açık `typ` ve birbirini dışlayan doğrulama kuralları — ideali farklı anahtarlar. OWASP Microservices Security Cheat Sheet iki kabul edilebilir desen tanımlar: mTLS **veya** STS'nin **public** anahtarıyla offline doğrulanan token; paylaşılan simetrik anahtar ikisi de değil. NIST SP 800-204B: servis kimliği için mTLS "de facto" mekanizma.
- **Rotasyon:** tek anahtar, 10 process, `kid` yok = eşzamanlı restart veya elle yazılmış çift-kabul penceresi; tek sızıntı her yerde rotasyon.
- **JTI replay guard'ın maliyeti:** her istekte Redis `SET NX` (fail-closed). RFC 9700 (OAuth 2.0 Security BCP, Ocak 2025) replay için `jti` deposu yerine **sender-constrained** token (mTLS RFC 8705 / DPoP RFC 9449) + `aud` kısıtı önerir; bearer token ele geçirilirse zaten replay edilebilir. TLS içinde 45 sn TTL + sıkı `aud` varken kalan tehdit "token'ı meşru olarak gören taraf" (hedef servis, log, trace) — `aud` bunu zaten aynı servise hapsediyor. Redis kontrolü ~45 sn'lik aynı-servis replay koruması satın alıyor; bedeli her çağrıda Redis RTT + sert availability bağımlılığı. Uyarı: Spring Security'nin kendi DPoP `jti` cache'i **boyut sınırlı** olduğu için flood ile evict edilebildi (CVE-2026-41707, HIGH, Ağustos 2026) — bir `jti` deposu **TTL sınırlı** olmalı (≥ token TTL), asla boyut sınırlı değil; Redis'te `allkeys-*` eviction ile aynı hata oluşur (§4.4).

**Çözüm sırası (kolaydan zora):**
1. **Asimetrik imza, yeni altyapı yok:** Gateway ve her servis kendi **Ed25519/ES256** anahtar çiftine sahip; `iss=<servis-adı>`; doğrulayan taraf `iss`+`aud`+`kid` ile public key'i JWKS'ten (paylaşılan salt-okunur JWKS dosyası veya her servisin `/.well-known/jwks.json` ucu) okur. Nimbus JOSE ve Spring Security resource-server (`JwtIssuerAuthenticationManagerResolver`) bunu destekler; kod değişikliği `ServiceJwtSigner`/`ServiceJwtVerificationFilter` ile sınırlı. Bir servis sızsa yalnız **kendi** kimliğiyle konuşabilir; allowlist gerçekten işe yarar. Servisler user JWT'yi de imza anahtarı tutmadan doğrulayabilir.
2. Replay guard'ı **kapat** veya yalnız gateway → servis (dış kaynaklı) token'lar için tut; tutulursa TTL sınırlı ve fail politikası açık.
3. Anahtar rotasyonu: JWKS'te `kid` + iki anahtar aynı anda geçerli.
4. Daha sonra: **mTLS** iç CA ile (step-ca veya basit openssl; Spring Boot 3.1+ SSL bundle'ları hot-reload destekler, `server.ssl.client-auth=need`, kimlik = sertifika SAN) — replay ve taklidi transport katmanında çözer; maliyeti sertifika yaşam döngüsü. **SPIFFE/SPIRE** compose container'ı olarak çalışır (Docker workload attestor, container label/image digest ile X.509-SVID/JWT-SVID); Eylül 2026'da Apache Camel'in tamamen docker-compose üstünde SPIRE + OPA örneği yayınlandı. `aud`-bound JWT-SVID, ev yapımı service JWT'nin doğrudan yerine geçer. RFC 8693 token exchange ("gateway, accountId adına hareket ediyor" için doğru semantik) bir authorization server ister; anahtar güvenini kendi başına çözmez. Service mesh (Istio/Linkerd) Kubernetes'siz pratik değil.

### 6.2 E2EE: gerçek durum ve iddia arasındaki fark — 🟠

Mevcut şema (referans §9.9): kullanıcı başına **tek statik** X25519 anahtarı (public key user-core'da), sohbet anahtarı = HKDF(ECDH(a, B), salt=conversationId), AES-256-GCM.

Bu şemanın eksikleri kriptografik literatürde iyi bilinir:
- **Forward secrecy yok:** Bir cihazın private key'i sızarsa, o kullanıcının **tüm geçmiş ve gelecek** sohbetleri (sunucudaki ciphertext ile birlikte) çözülür. Signal Double Ratchet'in varlık sebebi bu.
- **Post-compromise security yok:** Anahtar sızdıktan sonra iyileşme mekanizması yok.
- **Anahtar dağıtımı sunucuda, doğrulama yok:** Sunucu (veya sunucuya erişen biri) B'nin public key'i yerine kendi anahtarını verirse A fark etmez → **MITM**. Signal'de "safety number", WhatsApp'ta kod karşılaştırma bu yüzden var. Backoffice'in `conversationKey` ile kanıt çözebilmesi, bu modelde sunucunun zaten anahtar üzerinde güç sahibi olduğunu gösteriyor.
- **Çoklu cihaz / yeniden kurulum:** Private key nerede? Cihaz değişince geçmiş sohbetler okunamaz (ya da anahtar bir yerde yedekleniyorsa o yedek yeni saldırı yüzeyi). Referans dokümanda bu konuda bilgi yok (**doğrulanmalı**).
- **Nonce yönetimi:** NIST SP 800-38D rastgele IV'li GCM'de anahtar başına **2^32** şifrelemeyi sınır koyar; nonce tekrarı gizliliği ve tag bütünlüğünü birlikte kırar. Tek sohbette bu sayıya ulaşılmaz ama **aynı kullanıcının iki cihazı** aynı statik anahtarı paylaşıyorsa nonce çakışma riski gerçek olur.

**Ne yapılmalı:**
- **Kısa vadede dürüstlük:** Bu şema "sunucu içeriği okuyamaz (at-rest gizlilik)" iddiasını karşılar; "uçtan uca şifreli" pazarlaması yapılıyorsa FS/MITM sınırlamaları gizlilik politikasında yazılmalı. KVKK/tüketici hukuku açısından yanıltıcı iddia riski. Hemen yapılabilecek ucuz iyileştirme: istemcide karşı tarafın anahtarını **pin'lemek** (TOFU) ve anahtar değişince kullanıcıyı uyarmak (Signal'in "safety number" modelinin asgarisi).
- **Orta vadede:** Signal Protocol (X3DH → PQXDH 2023 → SPQR "Triple Ratchet" Ekim 2025) veya MLS (RFC 9420; 2 kişilik "grup" için de uygun, cihaz = yaprak → multi-device doğal). Sunucu tarafı: prekey bundle depolama (`user-core`'daki tek public key yerine identity key + signed prekey + one-time prekeys), mesaj başına ratchet header. chat-core mesaj tablosu değişmez (yine opak blob). **Flutter tarafı dikkat:** resmi `libsignal` yalnız Java/Swift/TypeScript/Rust binding'i sunar, Dart yok. `libsignal_protocol_dart` (pub.dev, 0.8.x) saf Dart X3DH+Double Ratchet ama PQXDH yok, bağımsız denetim yok ve **GPL-3.0** — kapalı kaynak uygulama için lisans sorunu. `openmls` Dart (3.2.0, MIT, flutter_rust_bridge üzerinden OpenMLS 0.9) mevcut ama altındaki kripto 1.0 öncesi. Pratik yol: platform channel ile resmi libsignal Java/Swift'i çağırmak, ya da OpenMLS.
- **Şikayet kanıtı** için doğru desen **message franking** (Facebook Messenger 2016; Grubbs–Lu–Ristenpart 2017'de formalize edildi): gönderici her mesaj için rastgele anahtarla düz metin üzerinden HMAC commitment üretir; sunucu düz metni görmeden bu tag'i imzalayıp saklar; şikayette alıcı düz metin + anahtarı verir, sunucu hem kendi imzasını hem commitment'ı doğrular. AES-GCM key-committing olmadığı için ayrı HMAC şart. WhatsApp şikayette rapor edilen mesaj + önceki 4 mesajı **raporlayanın cihazında** çözüp iletir. Mevcut şema da aynı deseni alabilir: istemci tarafı çöz-ve-ilet + sunucuda mesaj başına commitment. `conversationKey`'in backoffice'e verilmesine gerek kalmaz.

### 6.3 Kimlik doğrulama: SMS OTP ve admin 2FA — 🟠

- **NIST SP 800-63B-4** (final, 2025): PSTN üzerinden OTP (SMS/sesli) tek "**restricted**" authenticator; kullanmak için belgelenmiş risk değerlendirmesi, en az bir restricted-olmayan alternatif sunma, kullanıcıyı bilgilendirme ve geçiş planı gerekir. Kullanıcı girişi için pratikte kabul edilebilir (dating app'lerin çoğu kullanıyor) ama **admin paneli** için SMS 2FA zayıf: TOTP (RFC 6238) veya **passkey/WebAuthn** (phishing'e dirençli). Spring Security **6.4** (Kasım 2024) `webAuthn()` DSL'i, `/webauthn/register` ve `/login/webauthn` uçları ve JDBC repository'leriyle passkey'i native destekliyor; Boot 3.2.5'in Security 6.2.4'ünde yok → Boot ≥3.4, pratikte 4.x (§9). Admin hesapları moderasyon kanıtına ve tüm kullanıcı verisine erişiyor.
- **SMS pumping (toll fraud / AIT):** Saldırgan `otp/start` ucunu premium numaralara SMS attırmak için kullanır; fatura patlar. Rate limit var (`otp-start`, `otp-start-ip`), ama tipik ek önlemler (Twilio'nun toll-fraud rehberi): hizmet verilmeyen ülke kodlarını engelleme (geo-permissions), `libphonenumber` doğrulaması + VoIP/premium aralık reddi, numara başına günlük tavan, "OTP gönder"den önce bot tespiti/attestation, harcama alarmı ve otomatik kesici. NetGSM tarafında benzer bir "fraud guard" var mı **doğrulanmalı**.
- **Bot/scraper koruması:** Public API'de cihaz attestation yok; profil kazıma ve toplu fake hesap için engel yalnız rate limit. Dating app'lerde fake profil ana abuse vektörü (§11.1). Attestation **kademeli risk sinyali** olarak kullanılmalı, ikili kapı olarak değil (rooted cihaz/token relay ile aşılabilir); önce kayıt, OTP-gönder ve beğeni/swipe uçlarına.

### 6.4 Secret ve config yönetimi — 🟠

- Config reposunda literal varsayılanlar (gateway JWT, SMTP, `QR_SALT`), local yml'lerde `${ENV:literal}` fallback'leri, `SPRING_CONFIG_IMPORT` URL'sinde basic-auth bilgisi (referans §15.4). `${JWT_SECRET:changeme}` gibi bir fallback, env eksikse uygulamayı **sessizce tahmin edilebilir anahtarla** açar; RFC 8725 §3.5'e göre bu anahtar tek token'dan geri kazanılır.
- `.env` sunucuda `chmod 600` ile üretiliyor; kaynağı GitHub Secrets. Bu küçük ekip için **kabul edilebilir** bir başlangıç. Eksikler: rotasyon prosedürü yok, secret'ın nereden geldiği izlenemiyor, `.env` build context'e giriyor.
- **Config Server'ın kendisi 2026'da ciddi bir risk oldu.** Spring Cloud Config'in güvenlik dokümanı: varsayılan HTTP Basic "pratikte yararlı değil", TLS ayrıca kurulmalı ve **"uygulama adını bilen her kimliği doğrulanmış istemci, başka uygulamanın config'ini isteyebilir"** — yani gateway, backoffice'in secret'larını okuyabilir. Üstüne, kullanılan 4.1.x dalında ve özellikle **native backend**'de 2026 CVE'leri: **CVE-2026-40982 CRITICAL** dizin geçişi (4.1.0–4.1.9; OSS düzeltmesi yalnız 4.3.3/5.0.3), **CVE-2026-22739 HIGH, kimlik doğrulamasız** — `profile` parametresi ile arama dizinlerinden kaçış/SSRF (4.1.0–4.1.8), CVE-2026-47894 native repo ifşası, CVE-2026-41004 trace log'larında secret sızması. Config-server port'u host'a açık değil (compose iç ağ) ama SSRF'e açık herhangi bir servis üzerinden erişilebilir.
- Öneri (kolaydan zora): (1) `.dockerignore`'a `.env*`; gitleaks CI'a. (2) Fallback'siz **fail-fast** (secret yoksa uygulama açılmaz). (3) Secret'ları Config Server'dan **çıkar**: Docker Compose `secrets:` + Spring Boot **config tree** (`spring.config.import=optional:configtree:/run/secrets/`, Boot 2.4+; sıfır bağımlılık) — her dosya bir property olur. (4) Git'te şifreli secret için **SOPS + age** (CNCF sandbox; yalnız değerleri şifreler, diff okunabilir kalır); deploy'da compose secret dosyalarına çözülür. (5) Dinamik DB kimliği ve audit gerekirse **OpenBao** (MPL-2.0, Vault API uyumlu, Linux Foundation) veya Infisical; Vault (BSL) şart değil.
- **Config Server kararı:** native backend + kardeş repo + `optional:` import. Dokümanın kendi ifadesiyle native backend "başlangıç ve test için harika… production'da dosya sistemi güvenilir ve tüm instance'lar arasında paylaşılmış olmalı". Bugün `optional:` demek, sunucu düşükse **sessiz kısmi config** demek. İki yol: (a) kaldır — tek host + compose'da `env_file` + profil yml + config tree, çalışma zamanı bağımlılığı sıfır; (b) tut — Spring Cloud 2025.1 (5.0.4+) sürümüne yükselt, `optional:` kaldır veya `spring.cloud.config.fail-fast=true` + retry, secret'ları dışarı al, CI'da local yml ↔ config repo drift testi. Bu ekip boyutunda (a) önerilir; secret-siz iş config'i zaten git'te yaşıyor.

### 6.5 Gateway katmanı — 🟡

- Rate limit yalnız downstream'de; gateway'de yok → kimliksiz istek seli doğrudan auth-core'a ulaşır. Spring Cloud Gateway `RequestRateLimiter` (Redis) veya önündeki reverse proxy'de (nginx `limit_req`) IP bazlı limit ucuz.
- CORS `*`: mobil için gereksiz, panel için tehlikeli.
- Header temizliği (`X-Subject-Id`, `X-User-*`) ve `StripPrefix` sonrası `/internal` kontrolü — referans §22.2 #17–18.
- WebSocket `?token=` query parametresi erişim loguna düşüyor.

### 6.6 Fotoğraf pipeline'ı — 🟠 (doğrulanmalı)

Presigned POST ile **doğrudan public bucket'a** yükleme + `validateDocument` (HEAD, başlık, ETag). Referans dokümanda **EXIF temizleme** geçmiyor. Telefon fotoğraflarında EXIF `GPSLatitude/Longitude` bulunur; konum fuzzing yapan bir uygulamanın profil fotoğrafından **ev adresini** sızdırması klasik hata. Ayrıca: yeniden boyutlandırma/thumbnail yok (mobil veri), NSFW/CSAM taraması yok (§11.1).

İkinci sorun **public bucket'ın kendisi**: object key rastgele olsa da bucket public ise bir kez sızan/kazınan URL süresiz çalışır, kullanıcı fotoğrafını silse bile CDN'de kalır, "gizli mod"daki kullanıcının fotoğrafı erişilebilir kalır. Dating app'lerde profil fotoğrafı kazıma (scraping) ve ters görsel arama ile kimlik tespiti bilinen saldırı.

Öneri: Yükleme private "inbox" bucket'a; bir işleme adımı (imgproxy/Thumbor veya user-core'da bir worker) **yeniden kodlar** (EXIF gider), boyutlar üretir, moderasyon API'sine gönderir (§11.1), sonra **private** servis bucket'ına taşır; istemciye kısa ömürlü **signed GET** URL'si (veya imzalı CDN URL'si) verilir. Belgeler için zaten böyle yapılıyor (private bucket, 5 dk presigned görüntüleme); aynı model profil fotoğrafına uygulanmalı.

### 6.7 KVKK: silme hakkı orkestrasyonu — 🟠 (doğrulanmalı)

Export (veri taşınabilirliği) detaylı tasarlanmış (referans §9.10). **Silme** için: 9 şema, ES index'i, Redis key'leri, S3 (fotoğraf + belge), notification cihaz token'ları, backoffice kanıtları (yasal saklama süresiyle çelişir), yedekler. Bunu tek transaction'da yapmak imkânsız; **silme saga'sı** gerekir: `account.deletion_requested` event'i → her servis kendi verisini siler/anonimleştirir → tamamlanma raporu. Yasal saklama gereken kayıtlar (moderasyon, ödeme) **anonimleştirilir**, silinmez. Yedeklerde kalan veri için **crypto-shredding** (kullanıcı başına anahtar; silmede anahtarı yok et) en pratik çözüm.

Bu yalnız KVKK meselesi değil: **Apple App Store Review Guideline 5.1.1**, hesap oluşturan her uygulamanın **uygulama içinden hesap silme** sunmasını zorunlu kılıyor; Google Play'de de benzer "veri silme" beyanı var. Silme akışı yoksa mağaza incelemesinde ret sebebi.

Yasal süreler ve kapsam:
- **KVKK** (6698) Md. 7: işleme amacı ortadan kalkınca silme/yok etme/anonimleştirme; Md. 11/13: ilgili kişi başvurusu **30 gün** içinde sonuçlandırılır. Silme Yönetmeliği (2017): "silme" (erişilemez/kullanılamaz), "yok etme" (geri getirilemez), "anonim hale getirme" ayrı tanımlı; periyodik imha aralığı en fazla **6 ay**; veri üçüncü tarafa aktarıldıysa (SMS sağlayıcı, FCM, analytics) onlara da silme bildirilir; tüm işlemler loglanır ve kayıtlar ≥3 yıl saklanır.
- **GDPR** (AB kullanıcısı varsa) Md. 17 + Md. 12(3) bir ay; Md. 19 her alıcıya bildirim; ICO: yedeklerdeki veri orantısızsa tutulabilir ama "**beyond use**" olmalı ve restore sonrası silme yeniden koşmalı — crypto-shredding tam bunu sağlar.
- Kapsam kontrol listesi: DB (9 şema), ES index (delete-by-query; segment merge'e kadar doküman fiziksel olarak kalır), Redis (kullanıcı başına key namespace + TTL), S3 (**tüm versiyonlar** + lifecycle + CDN purge), log/APM (pseudonymize), üçüncü taraf işleyiciler, ban/fraud için belgelenmiş hukuki dayanakla asgari hash'li kayıt.

### 6.8 Konum gizliliği — 🟡

`LocationFuzzer` var. Araştırma literatürü rastgele kaydırmanın (fuzzing) **yetersiz** olduğunu iki kez göstermiş:
- **Polakis vd., CCS 2015 ("Where's Wally")**: istek başına rastgele gürültü, tekrarlı örneklemeyle ortalaması alınarak etkisizleşir; ortalama 56 sorgu / 7 sn'de konum bulundu. Önerilen çözüm sabit **spatial cloaking** (grid hücresi).
- **KU Leuven, USENIX Security 2024 / Black Hat 2024**: 15 dating app'in 6'sında (Bumble, Hinge, Happn, Grindr, Badoo, Hily) kesin konum çıkarılabildi; Grindr'da ~111 m. Daha önemlisi **"oracle trilateration"**: mesafe gizlense bile "X km içinde mi" gibi ikili filtreler yeterli — ~2 m hassasiyete inildi. Öneri: mesafeyi gerçek koordinattan değil, **genelleştirilmiş noktadan** (grid hücresi / ilçe merkezi) hesapla; yakınlık oracle'ı olabilecek filtreleri kaldır.

Yeterli sayılan önlemler: kullanıcı başına **deterministik** sabit grid hücresi veya sunucuda saklanan sabit (seed'li) ofset (~1–3 km; kullanıcı hareket etmeden değişmez), aynı yuvarlanmış noktanın **ES geo sorgusu ve filtrelerde de** kullanılması (yalnız gösterimde değil), mesafeyi **aralık** olarak gösterme ("<2 km"), minimum yarıçap, mesafeye göre kesin sıralama yapmama, kullanıcı/cihaz başına arama hız limiti. `LocationFuzzer`'ın deterministik mi rastgele mi olduğu ve ES sorgusunun ham koordinatı mı kullandığı **doğrulanmalı**. Harita özelliği (`X-Map-*` header'ları, referans §5.5) bu açıdan en riskli yüzey.

---

## 7. Dayanıklılık ve Çalışma Zamanı

### 7.1 Bloklayan zincirler ve thread tükenmesi — 🟠

- Spring MVC (Tomcat, platform thread) + senkron Feign + 2/5 sn timeout. Retry yok (bilinçli), circuit breaker yok (Resilience4j yalnız backoffice yml'inde, kullanılmıyor).
- Bir downstream yavaşlayınca üstteki servisin thread'leri timeout süresince bloke; kuyruk büyür; **cascading failure**.

Öneriler (Nygard'ın *Release It!* sırası: önce her hop'ta sert timeout, sonra bağımlılık başına bulkhead, sonra circuit breaker; retry yalnız bütçeyle):
1. **Timeout bütçesi:** Gateway'de toplam istek timeout'u (örn. 3 sn); downstream'ler daha kısa. Bugün gateway timeout'u belirtilmemiş (**doğrulanmalı**). Tomcat varsayılanı `server.tomcat.threads.max=200`; 30 sn'lik bir downstream timeout'u ile 7 istek/sn bile havuzu doldurur.
2. **Resilience4j + OpenFeign:** `spring.cloud.openfeign.circuitbreaker.enabled=true` her Feign metodunu `FooClient#bar()` adıyla sarar; hedef başına circuit breaker (açılınca hızlı 503, thread bloke olmaz), bulkhead (hedef başına eşzamanlı çağrı üst sınırı), `fallbackFactory`. **Tuzak:** Spring Cloud CircuitBreaker'ın Resilience4j entegrasyonu varsayılan olarak **1 sn'lik TimeLimiter** ve thread-pool bulkhead da ekler; `resilience4j.timelimiter.instances.*` ayarlanmazsa 5 sn'lik read timeout'unuz anlamsızlaşır (`spring.cloud.circuitbreaker.resilience4j.disable-time-limiter` / `enableSemaphoreDefaultBulkhead`).
3. **Virtual threads** (`spring.threads.virtual.enabled=true`, Boot 4'te de opt-in): Tomcat thread sınırı kalkar; bloklayan IO ucuzlar. Java 21'de `synchronized` bloğu carrier thread'i **pin'ler** (JDBC sürücüleri/havuzlar dahil); JEP 491 (JDK 24) bunu çözdü → **Java 25 LTS'te** değerlendirin, 21'de değil. Hikari havuzu yine sınır — bilinçli tutulmalı.
4. **OpenFeign'in geleceği:** Spring Cloud 2022.0.0'dan (Aralık 2022) beri "**feature-complete**" — yalnız bugfix; Spring ekibi HTTP Service Clients'a geçişi öneriyor. Hâlâ 2025.1'de (5.0.2) ve 2026.0-M1'de geliyor; ölü değil. Spring Framework 7 / Boot 4'te `@HttpExchange` arayüzleri + `@ImportHttpServices`, grup başına `spring.http.serviceclient.<grup>.base-url|connect-timeout|read-timeout`, `RestClient` altyapısı; Framework 7 ayrıca çekirdeğe `@Retryable` / `@ConcurrencyLimit` (`@EnableResilientMethods`) ekledi. Boot 4 upgrade'iyle birlikte planlanır; yeni client'lar o modelde yazılır.
5. §2.4 read-model işi bunların hepsinden daha etkili.

### 7.2 Backoffice parametre kataloğu = tier-0 bağımlılık — 🟠

Kural: "default yok, fallback yok, süresi geçmiş cache yok, hata yutulmaz → 503". Tutarlılık için doğru, **availability** için yanlış: moderasyon paneli servisi (backoffice-core) yeniden başlarken veya bir migration hata verirken **swipe, mesaj, abonelik, şifre politikası** 503 döner. auth-core'un `groupOrLastKnown` (10 dk) istisnası zaten bu kuralın pratikte tutmadığını gösteriyor.

Öneri: **bounded staleness** politikası — her tüketici son başarılı `ParameterGroupDto`'yu (revizyon ile) bellekte **ve diskte** (local snapshot; soğuk açılışta backoffice yoksa bile ayağa kalkar) tutar; backoffice erişilemezse **en fazla T dakika** (grup bazında; hak limitleri için 10 dk, yaş sınırı için 1 saat) son bilinen değeri kullanır, `parameter_staleness_seconds` metriğini yayınlar, T aşılınca yalnız **gerçekten güvenlik-kritik** parametrelerde 503'e düşer. Snapshot/revizyon modeli zaten var; kalıcı sonuç yazarken hangi revizyonun kullanıldığı kaydediliyor. Bu, tutarlılığı bozmaz; sadece "hiç cevap vermemek" yerine "bilinen son kuralı uygulamak"tır.

Bu, sektörün standart deseni: AWS'nin "**static stability**" ilkesi — data plane, control plane bozulduğunda **son bilinen konfigürasyonla** çalışmaya devam eder. Unleash Java SDK 15 sn'de bir poll eder, `unleash-repo.json` yedeğini diske yazar ve sunucu düşünce onu servis etmeye devam eder; OpenFeature SDK provider hatasında default döner; LaunchDarkly/Flagsmith/GrowthBook aynı (local evaluation + last-known-good). "5 sn cache + 503", backoffice'i her servisin tier-0 bağımlılığı yapar.

Alternatif: parametre revizyonlarını **event** olarak yayınla (§5.3); tüketiciler local tabloda tutar; backoffice yalnız yazma yolu olur.

### 7.3 Açılış bağımlılıkları ve health — 🟡

- Healthcheck yalnız 3 serviste (config-server, gateway, auth). Compose `depends_on: service_healthy` diğerleri için çalışmıyor.
- Readiness ≠ liveness: Spring Boot `management.endpoint.health.probes.enabled=true` ile `/actuator/health/readiness` ayrı; gateway hazır olmayan servise route etmesin.
- `restart: on-failure:5` — beşinci hatadan sonra servis **ölü kalır** ve kimse haber almaz (Alertmanager yok, §8).
- Graceful shutdown (`server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase`) belirtilmemiş (**doğrulanmalı**); `stop_grace_period 30s` var ama uygulama in-flight isteği bitirmiyorsa anlamsız. Poller'ların lease modeli çökmeyi tolere ediyor; bu iyi.

### 7.4 Kapasite — 🟡

Tek host: 9 JVM × 700 MB limit = 6,3 GB + Postgres + Redis + RabbitMQ + ES (≥1 GB) + obs yığını (Loki, Tempo, Prometheus, Grafana ≈ 2 GB). Bu host en az 16 GB olmalı; yatay ölçek (ikinci instance) için host kapasitesi **yok**. "Multi-instance kuralı" kodda var, altyapıda karşılığı yok. Bu bir çelişki değil (hazırlık iyi) ama ikinci host planı olmalı: hangi servis önce çoğalır (chat: WebSocket bağlantı sayısı; gateway), oraya nasıl yönlendirilir (host nginx upstream), DB bağlantı bütçesi (§4.1-c).

---

## 8. Gözlemlenebilirlik

Yığın doğru bileşenlerden kurulu (OTLP → Tempo, Prometheus, Loki, Grafana), ama **kapatılmamış döngü** var: alarm var, kanal yok.

| Eksik | Önem | Çözüm |
|---|---|---|
| **Alertmanager yok**; alarm kuralları hiçbir yere gitmiyor | 🔴 | Alertmanager + Telegram/Slack/e-posta receiver; tek host'ta Grafana'nın kendi unified alerting'i de yeter (Grafana 11+). Container restart-loop ve `*_STUCK` logları için kural. |
| **Promtail EOL** — Grafana'nın kendi dokümanı: "**2026-03-02 itibarıyla EOL, gelecekte destek veya güncelleme yok**"; tüm geliştirme Alloy'da | 🟠 | Grafana **Alloy**'a geç (1.20, Eylül 2026; içinde OTel Collector 0.161). Geçiş aracı hazır: `alloy convert --source-format=promtail`, geçişte `--config.format=promtail` ile eski config'i doğrudan çalıştırabilir. |
| Loglar düz metin; `key=value` regex ile ayrıştırılıyor | 🟡 | Spring Boot 3.4+ **structured logging** (`logging.structured.format.console=ecs\|gelf\|logstash`; MDC ve fluent `addKeyValue` JSON alanı olur; 3.5'te stacktrace kontrolü); Loki `json` stage; `code=`, `outcome=` gerçek alan olur. Boot 4 ile OTLP log export (`spring-boot-starter-opentelemetry`) ve Loki'nin native OTLP alımı da seçenek. |
| Loki label disiplini belirsiz | 🟢 | Loki rehberi: label değerleri onlarla sınırlı; trace/kullanıcı id'si **asla label değil** (referans §8.6 bunu zaten söylüyor) → **structured metadata** (schema v13 + TSDB; mevcut Loki config'i v13 — uygun). `logcli series --analyze-labels` ile denetle. |
| %100 trace sampling | 🟡 | Düşük trafikte sorun değil (Boot varsayılanı %10). Büyüyünce **tail sampling** (Alloy/OTel Collector `tailsampling`: tüm hatalı + yavaş izler %100, kalanı olasılıksal; `decision_wait` 30 sn, 50k iz bellekte) — aynı host'ta Tempo ingester belleği ve depolama için. |
| OTel Collector yok; uygulamalar doğrudan Tempo'ya | 🟢 | Alloy zaten Collector; araya girince sampling, retry, vendor değişimi tek yerden. |
| DB/Redis/Rabbit metrikleri yok (exporter yok) | 🟠 | postgres-exporter, redis-exporter, RabbitMQ prometheus plugin. Bağlantı havuzu, replication slot, queue derinliği görülmeli. |
| **SLO yok**; hangi hata oranı "sorun" bilinmiyor | 🟡 | Endpoint bazlı RED metrikleri zaten Micrometer'da; 2–3 SLO tanımla (swipe p99 < 300 ms, mesaj teslim < 1 sn, OTP teslim < 10 sn); Google SRE Workbook'un **multiwindow multi-burn-rate** alarmı (14,4× 1 sa/5 dk → sayfa; 6× 6 sa/30 dk → sayfa; 1× 3 gün/6 sa → ticket). |
| Obs yığını sürümleri geride | 🟢 | Güncel: Grafana 13.2, Loki 3.7, Tempo 3.0, Prometheus 3.15 (Eylül 2026). Mevcut 11.5 / 3.3 / 2.7 / 3.2 birkaç minor geride; Alloy geçişiyle birlikte güncelle. |
| İş metrikleri yok (eşleşme/gün, mesaj/gün, OTP başarı oranı, outbox gecikmesi) | 🟡 | Micrometer `Counter/Gauge`; `outbox_oldest_pending_age_seconds` her outbox için — takılan poller'ın en erken sinyali. |
| `*_STUCK` alarmı log satırı; metriğe bağlı değil | 🟢 | Loki ruler veya metrik. |

---

## 9. Sürüm ve Bakım Borcu

Bu bölüm **🔴 kritik**: yığının büyük kısmı OSS desteği bitmiş sürümlerde ve bilinen, yaması yalnız ticari sürümde olan CVE'ler taşıyor.

### 9.1 Destek durumu (2026-09-28 itibarıyla)

| Bileşen | Mevcut | Durum | Hedef |
|---|---|---|---|
| Spring Boot | 3.2.5 | 3.2.x OSS desteği **2024-12** sonunda bitti (son OSS 3.2.12). 3.3.x → 2025-06, 3.4.x → 2025-12, **3.5.x → 2026-06-30** (son 3.x; ticari destek 2032'ye kadar). Güncel OSS: **4.0.x** (OSS 2026-12-31'e kadar) ve **4.1.x** (4.1.1, Ağustos 2026); 4.2 GA Kasım 2026 bekleniyor. | **4.0.x/4.1.x** — 3.5 yalnız geçiş basamağı, durak değil |
| Spring Cloud | 2023.0.1 | 2023.0.6 son OSS sürümü; **2025-07-01'den beri yalnız ticari**. 2025.0.x (Boot 3.5) OSS 2026-06-30'da bitti. Güncel OSS: **2025.1.x "Oakwood"** (Boot 4.0; Gateway 5.0.2, Config 5.0.4, OpenFeign 5.0.2); 2026.0.0-M1 (Eylül 2026) Boot 4.1 için. | 2025.1.x |
| Spring Framework | 6.1.6 | 6.1 OSS 2025-06-30'da bitti; 6.2 Haziran 2026'da bitti; **7.0** güncel (Kasım 2025), 7.1 Kasım 2026 | 7.0 (Boot 4 ile gelir) |
| Java | 21 | LTS, destekleniyor | **25 LTS** (GA 2025-09-16; virtual thread pinning düzeltmesi JDK 24'te; Framework 7 için önerilen JDK) |
| PostgreSQL | 15 | Destek **2027-11-11**'e kadar (14: 2026-11-12). Güncel 18.6 (Ağustos 2026); 19 GA Ekim 2026 | 18 (`uuidv7()`, performans) |
| Redis | 7 (image tag'i) | 7.x son sürümleri Ağustos 2025; **fiilen bakımsız**. 7.4+ RSALv2/SSPL, 8.x AGPL ekli (8.10.2 güncel) | **Valkey 9.x** (BSD) veya Redis 8.x |
| RabbitMQ | 3.13 | Community desteği **2024-09-17**'de bitti. Community-destekli tek seri **4.3.x** (4.3.6, Eylül 2026); 4.0–4.2 destek dışı | 4.3.x (yol: 3.13 → 4.2 → 4.3 veya blue/green; classic mirrored queue kalktı → quorum queue) |
| Elasticsearch | 8.10.4 | Yama almıyor (8.x son: 8.19.22; 8.x bakım sonu Ocak 2027). 8.10 lisansı yalnız SSPL/ELv2 | OpenSearch 3.x (Apache-2.0) veya ES 9.x (AGPL seçeneği) — ya da PostGIS (§4.3) |
| Promtail | 3.3.2 | **EOL 2026-03-02** | Grafana Alloy 1.20 |
| Grafana / Loki / Tempo / Prometheus | 11.5 / 3.3 / 2.7 / 3.2 | Çalışır; güncel 13.2 / 3.7 / 3.0 / 3.15 | Alloy geçişiyle güncelle |
| Spring Cloud OpenFeign | 4.1.x | "Feature-complete" (Aralık 2022'den beri), bugfix alıyor; 5.x var | Boot 4 ile HTTP Service Clients'a kademeli geçiş (§7.1) |
| Spring Cloud Gateway | 4.1.1 (WebFlux) | Artefakt 2025.0'da `spring-cloud-starter-gateway-server-webflux`, property prefix'i `spring.cloud.gateway.server.webflux.*` oldu; eski adlar 5.0'da kaldırıldı (OpenRewrite reçetesi var) | 5.0.x |
| Testcontainers | yok | 2.0 (Nisan 2026) artefakt adlarını değiştirdi (`testcontainers-postgresql`), JUnit 4 kalktı | 2.x |
| React / MUI / Vite | 18.3 / 5.18 / 6.4 | Çalışır | React 19, MUI 7 (acele yok) |

### 9.2 Mevcut sürümleri etkileyen bilinen CVE'ler

"(E)" = düzeltme kullanılan dal için **yalnız ticari** sürümde; OSS'te yalnız yeni major/minor'a geçerek kapanır.

| Bileşen | CVE | Şiddet | Ne | Düzeltme |
|---|---|---|---|---|
| Spring Cloud Gateway 4.1.1 | CVE-2025-41243 | **CRITICAL 9.9** | Açık `gateway` actuator ucu üzerinden SpEL ile ortam değişkeni değiştirme | 4.1.11 (E) |
| | CVE-2025-41253 | HIGH | Env/property ifşası | 4.1.12 (E) |
| | CVE-2025-41235 | HIGH | `X-Forwarded-*` güvenilen proxy hatası | 4.1.8 (OSS) |
| | CVE-2026-47879 | HIGH | JsonToGrpc SSRF/dosya okuma | (E) |
| Spring Cloud Config 4.1.1 (native) | CVE-2026-40982 | **CRITICAL** | Dizin geçişi | 4.3.3 / 5.0.3 |
| | CVE-2026-22739 | HIGH, **kimlik doğrulamasız** | `profile` parametresiyle arama dizininden kaçış / SSRF | (E) |
| | CVE-2026-47894, -41004, -59315 | MEDIUM | Native repo ifşası, trace log'da secret, Monitor DoS | 5.0.5 |
| Spring Boot 3.2.5 | CVE-2025-22235 | HIGH | `EndpointRequest.to()` null matcher | 3.2.14 (E) |
| | CVE-2026-22733 | HIGH | `/cloudfoundryapplication` altında auth bypass | (E) |
| Spring Security 6.2.4 | CVE-2025-22228 | HIGH | BCrypt **>72 karakter** eşleşme bypass'ı — şifre 64 karakter/72 byte ile sınırlandığından muhtemelen etkisiz, **doğrulanmalı** | 6.2.10 (E) |
| | CVE-2024-38821 | **CRITICAL** | WebFlux static-resource yetki bypass'ı (gateway WebFlux) | 6.2.7 (OSS) |
| Spring Framework 6.1.6 | CVE-2024-38819 | HIGH | Functional web path traversal | 6.1.14 (OSS) |
| | CVE-2025-41249 | HIGH | Annotation tespiti → method-security bypass (backoffice `@PreAuthorize`) | 6.1.23 (E) |

Gateway'in `gateway` actuator ucu açık değilse (exposure `health,info,prometheus`) CVE-2025-41243 tetiklenmez; yine de yamasız kalınmamalı.

### 9.3 Upgrade sırası

1. **Java 25** (Boot 3.2 Java 25'te çalışır; önce JDK).
2. **Boot 3.5 + Cloud 2025.0** — birkaç gün: `taskExecutor` bean adı `applicationTaskExecutor`, katı boolean/profil ayrıştırma, Gateway artefakt/property adları (OpenRewrite), Hibernate 6.6. Bu basamakta Testcontainers CI (§10) ve `common-security` → starter dönüşümü (§3.1) yapılır; sonraki adımı kolaylaştırır.
3. **Boot 4.0/4.1 + Cloud 2025.1** — 2–4 mühendis-haftası: Spring Framework 7 (Jakarta EE 11, Tomcat 11, `RestTemplate` deprecated), **Jackson 3** (`tools.jackson`, `JsonMapper`; `spring-boot-jackson2` köprüsü), Hibernate ORM 7.1/7.2 (`@UuidGenerator` v7), modüler starter'lar (`spring-boot-starter-webmvc`, `-flyway`, `-<tech>-test`), `@MockBean` → `@MockitoBean`, `@SpringBootTest`'in MockMvc'yi otomatik vermemesi, Testcontainers 2 artefakt adları. `spring-boot-properties-migrator` ve OpenRewrite reçeteleri işi kısaltır.
4. Dependabot/Renovate ile borcun yeniden birikmesini engelle.

---

## 10. Test ve Kalite Güvencesi

Mevcut test kültürü güçlü (1.600+ test metodu, log-privacy testleri, concurrency testleri, binding testleri). Boşluklar:

| Boşluk | Önem | Çözüm |
|---|---|---|
| **Gerçek-PG testleri CI'da atlanıyor** (`@EnabledIfEnvironmentVariable`); Testcontainers yok | 🟠 | Testcontainers (`postgres`, `redis`, `rabbitmq`) + Spring Boot **`@ServiceConnection`** (3.1+; `@DynamicPropertySource`'un yerine). `ubuntu-latest` runner'da Docker hazır. Not: Testcontainers "reuse" modu deneysel ve **CI için uygun değil**; CI'da Spring context cache + JVM başına tek container yeter. Testcontainers 2.x artefakt adları değişti (`testcontainers-postgresql`). Daha ucuz alternatif: workflow `services:` bloğu ile Postgres; mevcut `PLATE_*_PG_TESTS` env'ini CI'da set etmek **bugün yarım saatlik iş** — Testcontainers'ı beklemeden. |
| Mimari kurallar yalnız dokümanda (controller→repository yasağı, core→core yasağı, `service.impl`'de yalnız Impl) | 🟡 | **ArchUnit** (1.5.x) testleri her core modülde: `noClasses().that().resideInAPackage("..controller..").should().dependOnClassesThat().resideInAPackage("..repository..")`, `layeredArchitecture()`, `slices().should().beFreeOfCycles()`; `*-core` → başka `*-core` bağımlılığı Maven enforcer `bannedDependencies` ile. §2.3-A/C seçilirse **Spring Modulith** `ApplicationModules.of(App.class).verify()` (döngü yok, yalnız API paketine erişim, `allowedDependencies`) + `spring.modulith.runtime.verification-enabled` ile açılışta ihlalde durma; Modulith 2.0 GA (Kasım 2025, Boot 4), 1.4.x Boot 3.x için. |
| Feign contract testleri yalnız **istek** biçimini doğruluyor; sağlayıcı tarafı yok | 🟢 | Monorepo'da derleme zamanı tip kontrolü zaten var; Pact'in kendi karşılaştırma sayfası bile "iki tarafı aynı ekip yazıyor ve kod aynı repoda ise az katkı sağlar" der. Bunun yerine `@WebMvcTest` ile internal controller'ların gerçek path/binding testi (backoffice'te var, diğerlerinde yok). |
| **Yük testi yok** (k6/Gatling); swipe/mesaj kapasitesi bilinmiyor | 🟠 | Staging'de haftalık k6 senaryosu: N eşzamanlı kullanıcı swipe + mesaj; p99 ve hata oranı SLO'ya karşı. Kapasite planı (§7.4) buna dayanır. |
| **Migration testi**: prod benzeri veri hacminde migration süresi/lock ölçülmüyor | 🟡 | Aylık prod yedeğinden anonimleştirilmiş staging DB; migration'lar orada koşar. |
| Frontend: `tsc -b`, `vite build`, lint CI'da yok | 🟢 | Referans §17 önerisi. |
| Mobil sözleşme: 16 el yazımı Flutter dokümanı, OpenAPI prod'da kapalı | 🟡 | Bkz. §13.2. |

---

## 11. Dating-App Domain'ine Özgü Eksikler

### 11.1 Güven ve güvenlik (Trust & Safety) — 🟠

Moderasyon iş akışı (report → case → action → appeal) olgun. Eksik olan **otomatik ilk katman**:
- **Fotoğraf moderasyonu:** NSFW/çıplaklık tespiti (AWS Rekognition, Hive, Sightengine; Bumble'ın açık kaynak "Private Detector" modeli) ve **CSAM hash eşleme**: Microsoft **PhotoDNA Cloud Service** (nitelikli kuruluşlara ücretsiz; görsel hash'lenir, saklanmaz) ve Google **CSAI Match / Content Safety API**. ABD'ye hizmet veren sağlayıcılar için NCMEC'e bildirim yükümlülüğü var (18 U.S.C. §2258A). Yükleme pipeline'ı (§6.6) bunun için doğru yer.
- **Selfie/liveness doğrulama** ("fotoğraf doğrulandı" rozeti): Tinder Ekim 2025'te **Face Check**'i (video selfie liveness, onboarding'de zorunlu, yüz vektörüyle çoklu hesap tespiti) birçok pazarda zorunlu yaptı; bu artık sektör çıtası. Fake profil ve catfish'e karşı en etkili araç. Araç doğrulama (plaka/ruhsat) var, **kişi** doğrulama yok.
- **Fake profil / scam sinyalleri:** Bumble'ın "Deception Detector"ı (2024) ve romance-scam istatistikleri (FTC 2023: 64 bin şikayet, 1,14 milyar $) bunun dating app'lerde birincil abuse vektörü olduğunu gösteriyor. Sinyaller: aynı cihaz/IP'den çoklu hesap, mesaj şablonu tekrarı, hızlı toplu beğeni, plaka tarama davranışı. E2EE nedeniyle içerik sınıflandırıcı (Bumble'ın "Rude Message Detector"ı gibi) **kullanılamaz**; yalnız metadata sinyalleri kalır. Bunlar için §5'teki event akışı gerekli; bugün sinyal yok.
- **Cihaz attestation** (§6.3): Google **Play Integrity** (`deviceIntegrity`, `recentDeviceActivity` bot hacmi sinyali, `appAccessRisk`, Kasım 2025'te eklenen `deviceRecall` ile reinstall/reset sonrası tekrar eden kötüye kullanıcı tespiti; varsayılan 10.000 istek/gün) ve Apple **App Attest** (donanım anahtar attestation + istek başına assertion) + **DeviceCheck** (cihaz başına 2 bit, reinstall'da kalır — "deneme hakkı kullanıldı" için). Kayıt, giriş ve satın almada risk sinyali olarak.
- **Düzenleyici / mağaza:**
  - **Apple Guideline 1.2** (UGC): rahatsız edici içerik filtresi, **zamanında yanıtlanan** raporlama, engelleme, yayınlanmış iletişim bilgisi — ilk ikisi otomasyon olmadan ölçeklenmez. "Gerçek kişilerin nesneleştirilmesi (hot-or-not)" kategorisindeki uygulamalar kaldırılıyor; plaka üzerinden kişi beğenme ürünü bu sınıra **yakın**; inceleme notlarında konumlandırma önemli.
  - **Google Play**: sosyal/dating uygulamaları Child Safety Standards beyanı ve yaş kısıtlaması (18+) zorunlu.
  - **EU DSA** (AB kullanıcısı varsa): notice-and-action (Md. 16), etkilenen kullanıcıya gerekçe (Md. 17), iç şikayet (Md. 20), yıllık şeffaflık raporu (Md. 15/24; 1 Temmuz 2025'ten itibaren standart şablon). Mevcut appeal/statement akışı buna yakın; rapor üretimi için audit verisi zaten var.
  - **UK OSA**: dating app'ler açıkça kapsamda; illegal-harms risk değerlendirmesi ve tedbirler Mart 2025'ten beri yürürlükte.
  - **Türkiye 5651**: günlük erişim **1 milyonu** geçince "sosyal ağ sağlayıcı" → yerel temsilci, BTK'ya 6 aylık raporlar, verinin Türkiye'de barındırılması. Bugün DigitalOcean'da barındırılıyorsa (bölge **doğrulanmalı**) bu eşik bir altyapı taşıma kararı demek.
  - Yaş doğrulama: 18+ sadece beyan mı? (**doğrulanmalı**)

### 11.2 Ürün analitiği, deney ve öneri motoru — 🟠

Dating app'in çekirdek döngüsü: **öneri kalitesi → eşleşme → sohbet → tutunma**. Bunu ölçmeden iyileştiremezsiniz. Bugün:
- Sunucu tarafı event akışı yok (§5). İstemci tarafı analytics (Firebase Analytics?) referans dokümanda yok (**doğrulanmalı**). Funnel (plaka arama → profil bulundu → beğeni → eşleşme → ilk mesaj → yanıt), retention ve eşleşme kalitesi Postgres'teki **satır durumundan** hesaplanamaz; ham olay logu gerekir.
- Feature flag yok → yeni özellik "ya hep ya hiç" çıkıyor; A/B yok → öneri ağırlıkları (backoffice'te "öneri ağırlıkları" var) körlemesine ayarlanıyor.
- Öneri motoru: ES geo sorgu + Redis snapshot. Bu, Tinder/Hinge'in "önceden hesaplanmış feed cache + index fallback" desenine zaten benziyor; iyi. Tinder'ın S2 geosharding'i on milyonlarca kullanıcıda gerekti (40–100 geoshard, shard = ayrı ES index'i); bugün gereksiz. Ama **ranking** (Tinder TinVec: swipe loglarından embedding; Hinge "Most Compatible": Gale-Shapley) için `SwipeMade(from, to, direction)`, `MatchCreated`, `MessageSent/Replied` olaylarının bir yerde birikmesi şart. Bumble'ın 2025 mimari tanımı da "retrieval → ranking → decisioning, gerçek zamanlı sinyallerle"; hepsi event akışına dayanıyor.

Küçük ekip için minimum (maliyet sırasıyla):
1. Firebase zaten var → **GA4/Firebase → BigQuery export** (standart hesaplarda ücretsiz, ham/örneklemesiz, günlük + intraday) istemci olayları için sıfır altyapı.
2. Sunucu olayları: event envelope (§5.3 Aşama 1) → tek "event sink" tüketicisi → Postgres `analytics` şeması (başlangıç) veya **ClickHouse** → Metabase/Grafana funnel panoları. Alternatif: **PostHog** (self-host; ClickHouse tabanlı analytics + session replay + flag + deney tek üründe) veya **RudderStack** (open-source, warehouse-first).
3. Feature flag / deney: **OpenFeature** (CNCF incubating, vendor-bağımsız SDK — Java, Kotlin, Swift) + **GrowthBook** (MIT, warehouse-native metrik) ya da Unleash; veya mevcut parametre kataloğuna `BOOLEAN`/`PERCENTAGE` tipi ekleyip kullanıcı-bucket'lı flag desteği vermek (kataloğun revizyon/snapshot modeli deney için zaten uygun). E2EE nedeniyle **içerik** olayı asla; yalnız metadata (`message_sent`, yanıt gecikmesi).

### 11.3 Uygulama içi satın alma — 🔴

`MockStoreValidationService` her ortamda aktif (referans §22.1 #1). Somut risk: token'ı olan herkes `/subscriptions/verify` ile premium açar; gerçek bir makbuz/purchaseToken **hesaplar arasında replay** edilir (bir abonelik → sınırsız premium hesap); iade/iptal/süre dolumu işlenmediği için churn eden kullanıcı premium kalır.

Doğru uygulama:
- **Apple:** `verifyReceipt` **WWDC23'ten beri deprecated**. **App Store Server API** JWS imzalı transaction döner; sertifika zinciri Apple köküne + OCSP ile doğrulanır, ardından `appAppleId`, `bundleId`, `environment` kontrol edilir. Apple'ın **App Store Server Library** (Java sürümü var) JWS doğrulama ve JWT auth'u hazır veriyor. **App Store Server Notifications V2**: iç içe JWS (`signedPayload` → `signedTransactionInfo` + `signedRenewalInfo`), tipler `SUBSCRIBED`, `DID_RENEW`, `DID_CHANGE_RENEWAL_STATUS`, `EXPIRED`, `REFUND`, `REVOKE`; `notificationUUID` ile dedup; 200 dön ve asenkron işle (retry 1s→72s). Satın almada set edilen **`appAccountToken`** (UUID) tüm transaction/bildirimlerde gelir → hak, hesaba bununla bağlanır.
- **Google:** `purchaseToken` **global benzersiz** → primary key; daha önce görülen token reddedilir. Doğrulama `purchases.subscriptionsv2:get`; `orderId` üzerine anahtar **kurulmaz** (promosyonlarda yok). Satın almada `obfuscatedAccountId` set edilir ve doğrulamada çağıranla eşleştirilir. **3 gün içinde acknowledge** edilmezse Google otomatik iade eder. `PENDING` durumunda hak verilmez. **RTDN** (Pub/Sub) yalnız "durum değişti" der; API çağrısı şart. Tipler: `PURCHASED`, `RENEWED`, `CANCELED`, `ON_HOLD`, `IN_GRACE_PERIOD`, `EXPIRED`, `REVOKED` + `voidedPurchaseNotification`.
- İkisinde de: makbuz → **bir kez** bir hesaba bağlanır (`original_transaction_id` / `purchase_token` UNIQUE); sunucu bildirimleri ile abonelik durumu tek kaynak olur; istemcinin "verify" çağrısı yalnız tetikleyicidir. Mevcut `purchase_order` + `idempotent_operation` altyapısı bunun üstüne oturur.
- Alternatif: **RevenueCat** (2,5k $ MTR'ye kadar ücretsiz, sonra %1) / **Adapty** (5k $'a kadar ücretsiz): iki mağazayı tek webhook'a indirger; küçük ekip için haftalar kazandırır.
- Store bildirimleri **RabbitMQ'ya değil**, imzası doğrulanmış webhook → subscription-core outbox'ına girer (referans §12.1'deki "moderasyon olayları kuyruktan kabul edilmez" ilkesiyle aynı).

### 11.4 Plaka tabanlı eşleşmenin gizlilik profili — 🟠

Plaka, KVKK ve GDPR'da **kişisel veri** (araç sahibine ulaşılabildiği için). Türkiye'de doğrudan emsal var:
- **KVKK Kurulu 2021/1110** (2 Kasım 2021): borçluların TCKN, telefon, **plaka** ve adresini yayınlayan şirket — plaka kişisel veri sayıldı, işleme hukuka aykırı bulundu.
- **KVKK Kurulu 2023/924** (1 Haziran 2023): otopark işletmecisinin **tek adımlı plaka sorgusu** ile borç bilgisi göstermesi — Kurul, sorguya **ikinci faktör** (kişisel bilgi veya SMS/e-posta doğrulaması) eklenmesini ya da durdurulmasını istedi. Yani "plaka gir → sahibi hakkında bir şey öğren" akışı, tek başına, Kurul'un sorunlu bulduğu desen.
- AB: EDPS (2017) ve EDPB bağlantılı araç rehberleri plakayı gerçek kişiye bağlanabildiğinde kişisel veri sayar.

Ürünün doğası — "trafikte gördüğüm plakayı arayıp o kişiye ulaşmak" — **takip ve taciz** vektörü; emsaller de bunu gösteriyor: Bump.com (ABD, 2010) "her plakaya mesaj" ile "stalker'ca" diye eleştirildi; **Plaka.io** (Türkiye, 2018) sahibinin kaydı olmadan her plakaya yorum yazdırdı ve kadın sürücülere karşı taciz kanalı olarak anıldı. Bu ürünün var olan savunmaları iyi: gizli mod, plaka araması hak tüketiyor (rate limit doğal), araç/ruhsat doğrulama, engel/şikayet. Eksik olabilecekler (**doğrulanmalı**):
- Plaka araması **yalnız karşı taraf opt-in** ise sonuç veriyor mu? Varsayılan görünürlük "kapalı" olmalı; kayıtlı olmayan ve gizli plakalar için **tek tip "sonuç yok"** (varlık oracle'ı olmasın).
- Aynı hesabın/cihazın **çok sayıda farklı plakayı** araması (tarama davranışı) tespit ediliyor mu? Hız limiti + velocity alarmı + arama audit logu (moderasyon için).
- Plaka, DB'de düz metin mi? `PhoneHashing` tuzsuz SHA-256 (referans §9.10) — plaka için de düşük entropili (TR plakası ~10^9 olasılık) → düz SHA-256 rainbow table'a açık; **HMAC + pepper** (arama için) + şifreli orijinal (gösterim için) şart (referans §9.10 kuralı zaten bunu söylüyor; uygulanıp uygulanmadığı doğrulanmalı).
- Kimlik yalnız **karşılıklı beğenide** açılmalı; plaka araması tek başına profil detayı vermemeli.
- Plaka aramasının hedefe bildirilmesi ("biri plakanı aradı") ve hedefin kaç kez arandığını görmesi — şeffaflık aynı zamanda caydırıcı.
- KVKK: açık rıza + aydınlatma metni plakayı ayrıca saymalı; bu ürün için **DPIA** (veri koruma etki değerlendirmesi) yapılmış olmalı.

### 11.5 Bildirim ve etkileşim — 🟢

FCM + kampanya altyapısı var. E2EE nedeniyle push'ta içerik yok (doğru; FCM/APNs payload'ı okuyabilir — Signal'in boş bildirim + fetch modeli, Firebase'in kendi "opak data message + fetch" tavsiyesiyle uyumlu). Push yalnız `conversationId` + collapse key taşımalı; iOS'ta `mutable-content` + Notification Service Extension ile istemci yerelde çözer.

Eksikler: sessiz saat / kullanıcı tercihi (var mı **doğrulanmalı**), bildirim yorgunluğu limiti (haftada 2–5 bildirimde kullanıcıların ~%46'sı opt-out ediyor — sektör anketleri; sosyal uygulamalarda Android opt-in ~%49, iOS ~%50), üç katman (işlemsel her zaman / davranışsal / promosyon opt-in), kullanıcı başına frekans tavanı, teslim/açılma oranı metriği (§8).

---

## 12. Build, Deploy ve Operasyon

Referans §18'deki tespitler doğru; burada **sıra** ve **neden**:

1. 🔴 **Yedekleme** (§4.1-b) — her şeyden önce.
2. 🔴 **Alertmanager** (§8) — `restart: on-failure:5` sonrası ölü servis fark edilmiyor. Docker'ın production rehberi `restart: always` önerir.
3. 🟠 **CI'da image build → GHCR → digest ile deploy.** "Build once, push to registry, promote" ilkesi; tag'ler değişebilir işaretçi, `@sha256:` değişmez. Prod'da build bitince: rollback = önceki digest'i deploy (saniyeler), "hangi commit prod'da" sorusu digest ile kesin, prod CPU'su build'e gitmez. Image: **Jib** (pom'da hazır; daemon'suz, tekrarlanabilir katmanlar, varsayılan non-root) veya Spring Boot layered jar / Buildpacks (`spring-boot:build-image`). Deploy = `docker compose pull && docker compose up -d <svc>`.
4. 🟠 **Staging ortamı.** `release` → test, `main` → hiçbir yere. Prod deploy'u elle mi? (**doğrulanmalı**). `main` → prod, onay kapısıyla (GitHub environment protection).
5. 🟠 **Non-root container, `HEALTHCHECK` + `start_period`, `.dockerignore`.**
6. 🟡 **Compose'da kalın** (Docker dokümanı tek host'u desteklenen "en basit" production seçeneği sayar), ama ikinci host planıyla (§7.4). Tek host'ta sıfır-kesinti için: önde proxy (Traefik/nginx) + **`docker-rollout`** (servisi ×2 ölçekle, healthy olunca eskisini kaldır) veya iki compose projesiyle blue/green. Kubernetes bu ekip boyutunda erken (Tinder'da ~200 serviste değdi); k3s/Swarm ancak ≥3 host olunca.
7. 🟡 **Image imzalama + SBOM:** cosign keyless (GitHub OIDC) veya GitHub `actions/attest-build-provenance`; Syft ile SPDX SBOM — tedarik zinciri; düşük maliyet.
8. 🟢 **Dependabot/Renovate** — §9'daki borcun birikmesini engeller.

---

## 13. Backoffice Web ve Mobil Sözleşme

### 13.1 Backoffice web — 🟢

Referans §17 önerileri yeterli (ESLint/Prettier/tsc CI, TanStack Query, rol matrisi tek dosya, route splitting). Mimari düzeyde tek not: **rol claim'i JWT'de** (≤900 sn gecikme) — admin yetkisi düşürüldüğünde anında etki gerekiyorsa `sv` artırma (zaten şifre değişiminde var) rol değişimine de bağlanmalı.

### 13.2 Mobil sözleşme: 16 el yazımı doküman yerine OpenAPI — 🟡

`docs/flutter-<feature>-integration-vN.md` (16 adet) + Postman + skill ile üretilen Dart örnekleri. Bu, kontratın **üçüncü** kopyası (kod, Postman, doküman). Kaydığında kimse fark etmiyor.

Öneri: springdoc zaten var → CI'da her servisin `/v3/api-docs` çıktısını üret → tek `plate-api.yaml`'a birleştir → **openapi-generator** (7.25, Ağustos 2026; `dart-dio` generator **stable**, `built_value` veya `json_serializable` serileştirme) ile Flutter client paketi üret → mobil ekip tip güvenli client alır, breaking change CI'da (openapi-diff) yakalanır. Flutter dokümanları "davranış ve ekran akışı"na daralır; endpoint/alan listesi üretilir. **API versiyonlama** (`/v1` veya header) de bu noktada kararlaştırılmalı; mobil uygulama mağazada eski sürümüyle aylarca yaşar.

---

## 14. AI Yönetişimi Katmanı

Bu katman projenin en özgün yanı ve iyi kurulmuş. Üç geliştirme:
1. **Kural → makine:** Dokümandaki kuralların çoğu (controller→repository yasağı, core→core yasağı, `@Valid` zorunluluğu, `log.error` öncesi throw) ArchUnit/Checkstyle/ErrorProne ile **CI'da** zorlanabilir. AI ajanı kuralı unutsa test yakalar; skill'ler "ne yapmalı"yı, testler "yapıldı mı"yı taşır.
2. **Push hook'u** kırık (referans §19.4) ve `.claude/skills` elle kopya — symlink + `bash -n` testi.
3. **Skill'lerin ürettiği kararlar** (APPROVE/REQUEST CHANGES) hiçbir yerde kayıt altında değil; PR template'ine "çalıştırılan skill'ler ve sonuçları" bölümü eklenirse review izi oluşur.

---

## 15. Yol Haritası

### Şimdi (0–4 hafta) — düşük maliyet, yüksek risk azaltma
- [ ] 🔴 PostgreSQL yedek (WAL-G → Spaces) + sürekli WAL arşivi + **restore provası**; Redis AOF; S3 versioning. (§4.1-b)
- [ ] 🔴 Alertmanager (veya Grafana alerting) + bildirim kanalı; container restart ve `*_STUCK` alarmı. (§8)
- [ ] 🔴 `MockStoreValidationService` → `@Profile("local|test")`; prod'da gerçek doğrulama yoksa satın alma **kapalı**. (§11.3)
- [ ] 🔴 Seed migration'ları prod location'ından çıkar (referans §22.1 #2).
- [ ] 🔴 Config Server CVE'leri için acil önlem: secret'ları Config Server'dan çıkar (compose `secrets:` + config tree), config-server'a yalnız gerekli servislerden erişim (compose network/ACL), `optional:` → fail-fast; kalıcı çözüm §9.3 upgrade. (§6.4, §9.2)
- [ ] 🔴 Gateway'de `gateway` actuator ucunun kapalı olduğunu doğrula (CVE-2025-41243). (§9.2)
- [ ] 🟠 Servis başına DB rolü + GRANT. (§4.1-a)
- [ ] 🟠 CI'da gerçek-PG testleri (`PLATE_*_PG_TESTS` env + `services:` bloğu ile bugün; Testcontainers `@ServiceConnection` sonra). (§10)
- [ ] 🟠 `.dockerignore` `.env*`; gitleaks CI; secret fallback'lerini kaldır. (§6.4)
- [ ] 🟠 EXIF/GPS: yüklenen fotoğrafın yeniden kodlandığını doğrula; değilse acil. Profil fotoğrafı bucket'ının public olup olmadığını netleştir. (§6.6)
- [ ] 🟠 Ban → `sv` artır (referans §22.1 #9) — tek satır.
- [ ] 🟠 RabbitMQ consumer'ları: `defaultRequeueRejected=false`, prefetch, stateful retry + backoff, DLQ. (§5.3-0)
- [ ] 🟠 Redis eviction politikasını kontrol et; güvenlik key'leri `noeviction` instance'ında olmalı. (§4.4)
- [ ] 🟠 Konum: `LocationFuzzer` deterministik grid'e; ES sorgusunun ham koordinat kullanmadığını doğrula. (§6.8)

### 3 ay
- [ ] 🟠 Java 25 → Boot 3.5 + Cloud 2025.0 (basamak) → **Boot 4.0/4.1 + Cloud 2025.1**; RabbitMQ 4.3 (quorum queue); Promtail → Alloy + obs yığını güncelleme; Valkey 9. (§9)
- [ ] 🟠 CI image build (Jib) → GHCR → digest deploy; cosign; non-root; healthcheck her serviste; `docker-rollout`; staging→prod onay kapısı. (§12)
- [ ] 🟠 Asimetrik service-JWT (Ed25519, `iss=<servis>`, JWKS config, `kid`); replay guard'ı daralt veya kaldır. (§6.1)
- [ ] 🟠 CloudEvents envelope + `domain.events` topic exchange; ilk tüketiciler: `account_standing` read-model (match, chat) ve analytics sink. (§5.3-1, §2.4)
- [ ] 🟠 Legal onay → JWT claim + `sv`. (§2.4)
- [ ] 🟠 Parametre kataloğu için bounded-staleness + disk snapshot + `parameter_staleness_seconds`. (§7.2)
- [ ] 🟠 Timeout bütçesi; Resilience4j circuit breaker/bulkhead (TimeLimiter tuzağına dikkat); virtual threads Java 25'te. (§7.1)
- [ ] 🟠 Fotoğraf işleme pipeline'ı (private quarantine → re-encode/EXIF strip → PhotoDNA/NSFW → private bucket + signed URL). (§6.6, §11.1)
- [ ] 🟠 Gerçek mağaza doğrulaması (App Store Server API + Notifications V2, Play `subscriptionsv2` + RTDN) veya RevenueCat. (§11.3)
- [ ] 🟠 Config Server'ı kaldır veya 5.0.x'e yükselt + fail-fast + drift testi. (§6.4)
- [ ] 🟡 ArchUnit + enforcer `bannedDependencies`; `common-security` → 4 starter. (§10, §3.1)
- [ ] 🟡 postgres/redis/rabbit exporter'ları; outbox yaş metriği; 3 SLO + burn-rate alarmı. (§8)
- [ ] 🟡 KVKK/Apple 5.1.1 hesap silme saga'sı tasarımı + crypto-shredding kararı. (§6.7)
- [ ] 🟡 Plaka görünürlüğü opt-in + varlık oracle'ı yok + HMAC-pepper depolama + arama audit'i; DPIA. (§11.4)

### 6–12 ay
- [ ] 🟡 Mimari şekil kararı: hibrit konsolidasyon (§2.3-C, Spring Modulith ile) **veya** read-model'lerle mikroservis. Karar kriteri: ekip boyutu ve ikinci host ihtiyacı.
- [ ] 🟡 Chat: `BYTEA` + `STORAGE EXTERNAL`, aylık partition (pg_partman), BRIN, retention, sohbet başına `seq`; ihtiyaç halinde ayrı Postgres instance. ScyllaDB eşiği belgelendi, izleniyor. (§4.2, §5.4)
- [ ] 🟡 Elasticsearch kararı: PostGIS'e in **veya** OpenSearch 3 / ES 9 + recommendation için genişlet. (§4.3)
- [ ] 🟡 E2EE: anahtar pinleme (TOFU) hemen; Signal Protocol/MLS'e geçiş planı; şikayet için message franking. (§6.2)
- [ ] 🟡 Admin passkey/TOTP (Boot 4 sonrası); kullanıcı OTP için Play Integrity/App Attest + SMS pumping kesicisi. (§6.3)
- [ ] 🟡 OpenAPI → Dart client; API versiyonlama. (§13.2)
- [ ] 🟡 Feature flag (OpenFeature + GrowthBook/Unleash veya katalog genişletme) + ilk A/B (öneri ağırlıkları); Firebase → BigQuery export. (§11.2)
- [ ] 🟡 Selfie/liveness doğrulama. (§11.1)
- [ ] 🟡 Yük testi + kapasite planı + ikinci host. (§10, §7.4)
- [ ] 🟡 RabbitMQ Streams ile replay/çoklu tüketici (§5.3-2). Debezium ve/veya Kafka **yalnız** §5.3-3 kriterleri oluşursa.
- [ ] 🟡 Türkiye'de 1M günlük erişim eşiğine yaklaşılıyorsa 5651 hazırlığı (temsilci, veri lokasyonu). (§11.1)

### Yapmayın
- ❌ **Polyrepo'ya geçmeyin.** Sorun pipeline'da. (§3)
- ❌ **Kafka'yı bugün eklemeyin.** Tüketicisi olmayan event log, tek host'ta bakım yükü. Önce envelope + topic + read-model. (§5)
- ❌ **Chat'i bugün ScyllaDB/Cassandra/MongoDB'ye taşımayın.** Önce partition + bytea + ayrı instance. (§4.2)
- ❌ **Kubernetes'e geçmeyin** (tek host, tek ekip). (§12)
- ❌ **Database-per-service'e (9 ayrı DB) geçmeyin.** Roller + gerektiğinde domain bazında instance ayırma yeter. (§4.1)
- ❌ **Vault kurmayın** (compose secrets + config tree, sonra SOPS yeter; gerekirse OpenBao). (§6.4)
- ❌ **RabbitMQ Delayed Message Exchange plugin'ini kullanmayın** (Eylül 2026'da arşivlendi; 4.3 native retry var). (§5.3-0)
- ❌ **`libsignal_protocol_dart`'ı kapalı kaynak uygulamaya koymayın** (GPL-3.0). (§6.2)
- ❌ **Merkezi saga orchestrator / Temporal eklemeyin.** Mevcut local saga tek adımlı ihtiyaca yetiyor; çok adımlı ihtiyaç doğarsa `LocalSagaStore`'u genelleştirin. (Referans §11.4)
- ❌ Outbox/poller'ı **sekizinci kez** elle yazmayın; generic outbox veya Debezium. (§4.6)

---

## 16. Kaynaklar

Araştırma 2026-09-28'de yapıldı. Bazı birincil siteler (postgresql.org, docs.spring.io, redis.io, rabbitmq.com, kvkk.gov.tr, nist.gov, ietf.org vb.) erişim kısıtı nedeniyle doğrudan okunamadı; bu durumlarda projelerin GitHub'daki doküman kaynakları, resmi blog/duyuru sayfaları veya arama özetleri kullanıldı. Yalnız arama özetine dayanan ya da doğrulanamayan noktalar metinde "doğrulanmalı" / "ikincil kaynak" diye işaretli.

### Mimari ve dağıtık sistemler
- Potvin & Levenberg, "Why Google Stores Billions of Lines of Code in a Single Repository", CACM 2016 — https://dl.acm.org/doi/10.1145/2854146
- Sam Newman, *Building Microservices*, 2. baskı — https://samnewman.io/books/building_microservices_2nd_edition/
- microservices.io: Database per service / Shared database / Transactional outbox / Polling publisher / Command-side replica / API composition — https://microservices.io/patterns/
- Microsoft Azure Architecture Center, "Data considerations for microservices" — https://learn.microsoft.com/en-us/azure/architecture/microservices/design/data-considerations
- Microsoft .NET Microservices, "Communication in a microservice architecture" (senkron zincir anti-pattern'i) — https://learn.microsoft.com/en-us/dotnet/architecture/microservices/architect-microservice-container-applications/communication-in-microservice-architecture
- AWS Prescriptive Guidance, "Shared-database-per-service pattern" — https://docs.aws.amazon.com/prescriptive-guidance/latest/modernization-data-persistence/shared-database.html
- AWS, "Static stability" (fault isolation boundaries) — https://docs.aws.amazon.com/whitepapers/latest/aws-fault-isolation-boundaries/static-stability.html
- Gunnar Morling, "Reliable Microservices Data Exchange With the Outbox Pattern" (Debezium, 2019) — https://debezium.io/blog/2019/02/19/reliable-microservices-data-exchange-with-the-outbox-pattern/
- Gunnar Morling, Postgres-as-queue tartışması (2025) — https://github.com/gunnarmorling/discussions.morling.dev/discussions/336
- Spring Modulith 2.0 GA — https://spring.io/blog/2025/11/21/spring-modulith-2-0-ga-1-4-5-and-1-3-11-released/ ; verification ve events dokümanları — https://github.com/spring-projects/spring-modulith/tree/main/src/docs/antora/modules/ROOT/pages

### Build, deploy, CI
- gitflow-incremental-builder — https://github.com/gitflow-incremental-builder/gitflow-incremental-builder
- Maven Build Cache Extension — https://github.com/apache/maven-build-cache-extension/releases
- Apache Maven sürümleri (4.0.0-rc-7, 3.10.0) — https://github.com/apache/maven/releases
- Docker Compose in production — https://docs.docker.com/compose/how-tos/production/
- docker-rollout — https://github.com/wowu/docker-rollout
- Spring Boot efficient container images (layered jar, Buildpacks) — https://docs.spring.io/spring-boot/reference/packaging/container-images/efficient-images.html
- sigstore cosign — https://github.com/sigstore/cosign ; GitHub attest-build-provenance — https://github.com/actions/attest-build-provenance
- Google Cloud, container image digest'leri — https://cloud.google.com/kubernetes-engine/docs/concepts/about-container-images
- Testcontainers + Spring Boot 3.1 `@ServiceConnection` — https://spring.io/blog/2023/06/23/improved-testcontainers-support-in-spring-boot-3-1/ ; reuse — https://github.com/testcontainers/testcontainers-java/blob/main/docs/features/reuse.md
- ArchUnit — https://github.com/TNG/ArchUnit
- Pact, karşılaştırmalar (tek ekip/tek repo notu) — https://docs.pact.io/getting_started/comparisons
- openapi-generator `dart-dio` — https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/dart-dio.md

### Veri katmanı
- HikariCP, "About Pool Sizing" — https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
- PgBouncer config — https://github.com/pgbouncer/pgbouncer/blob/master/doc/config.md
- PostgreSQL 18 release notes (`uuidv7()`) — https://www.postgresql.org/docs/release/18.0/ ; versioning policy — https://www.postgresql.org/support/versioning/
- PostgreSQL TOAST/storage ve BRIN dokümanları — https://www.postgresql.org/docs/current/storage-toast.html , https://www.postgresql.org/docs/current/brin-intro.html
- pg_partman — https://github.com/pgpartman/pg_partman
- credativ, UUIDv4 vs UUIDv7 in PostgreSQL 18 — https://www.credativ.de/en/blog/postgresql-en/a-deeper-look-at-old-uuidv4-vs-new-uuidv7-in-postgresql-18/
- uuid-creator — https://github.com/f4b6a3/uuid-creator ; Java Uuid Generator — https://github.com/cowtowncoder/java-uuid-generator ; Hibernate `@UuidGenerator` (VERSION_7, `@Incubating(since="7.0")`) — https://github.com/hibernate/hibernate-orm/blob/main/hibernate-core/src/main/java/org/hibernate/annotations/UuidGenerator.java
- Discord, "How Discord Stores Trillions of Messages" (2023) — https://discord.com/blog/how-discord-stores-trillions-of-messages ; InfoQ özeti — https://www.infoq.com/news/2023/06/discord-cassandra-scylladb/
- Slack, "Scaling Datastores at Slack with Vitess" — https://slack.engineering/scaling-datastores-at-slack-with-vitess/
- Elasticsearch sürümler ve lisans — https://github.com/elastic/elasticsearch/releases , https://www.elastic.co/blog/elasticsearch-is-open-source-again , EOL — https://www.elastic.co/support/eol
- OpenSearch sürümleri — https://github.com/opensearch-project/OpenSearch/releases
- PostGIS KNN — https://postgis.net/docs/geometry_distance_knn.html
- Redis GEOADD — https://redis.io/docs/latest/commands/geoadd/ ; eviction — https://redis.io/docs/latest/develop/reference/eviction/ ; Sentinel — https://redis.io/docs/latest/operate/oss_and_stack/management/sentinel/ ; Pub/Sub — https://redis.io/docs/latest/develop/interact/pubsub/ ; Streams — https://redis.io/docs/latest/develop/data-types/streams/
- Redis lisans (LICENSE.txt, 8.0 release notes) — https://github.com/redis/redis/blob/unstable/LICENSE.txt , https://github.com/redis/redis/blob/8.0/00-RELEASENOTES ; sürümler — https://github.com/redis/redis/releases
- Valkey — https://github.com/valkey-io/valkey/releases
- Stripe, "Scaling your API with rate limiters" — https://stripe.com/blog/rate-limiters
- pgBackRest — https://github.com/pgbackrest/pgbackrest ; arşivlenme/canlanma haberi — https://lwn.net/Articles/1069951/
- WAL-G — https://github.com/wal-g/wal-g
- Patroni — https://github.com/patroni/patroni
- DigitalOcean Managed PostgreSQL — https://docs.digitalocean.com/products/databases/postgresql/

### Mesajlaşma
- RabbitMQ release-information / endoflife.date — https://www.rabbitmq.com/release-information , https://endoflife.date/rabbitmq
- RabbitMQ 4.0 / 4.3 release notes — https://github.com/rabbitmq/rabbitmq-server/blob/main/release-notes/4.0.1.md , https://github.com/rabbitmq/rabbitmq-server/blob/main/release-notes/4.3.0.md
- RabbitMQ upgrade guide — https://www.rabbitmq.com/docs/upgrade ; Streams — https://www.rabbitmq.com/docs/streams ; Quorum queues — https://www.rabbitmq.com/docs/quorum-queues ; DLX — https://www.rabbitmq.com/docs/dlx ; consumer prefetch — https://www.rabbitmq.com/docs/consumer-prefetch ; RabbitMQ vs Kafka — https://www.rabbitmq.com/docs/compare/kafka
- RabbitMQ Delayed Message Exchange (arşivlendi) — https://github.com/rabbitmq/rabbitmq-delayed-message-exchange
- Spring AMQP: resilience/retry — https://docs.spring.io/spring-amqp/reference/amqp/resilience-recovering-from-errors-and-broker-failures.html ; container attributes — https://docs.spring.io/spring-amqp/reference/amqp/containerAttributes.html ; stream — https://docs.spring.io/spring-amqp/reference/stream.html
- Apache Kafka 4.0 duyurusu — https://kafka.apache.org/blog/2025/03/18/apache-kafka-4.0.0-release-announcement/ ; sürümler — https://endoflife.date/apache-kafka ; Docker örnekleri — https://github.com/apache/kafka/blob/trunk/docker/examples/README.md
- Confluent, Queues for Kafka (share groups GA) — https://www.confluent.io/blog/kafka-queue-semantics-share-consumer-ga/
- Spring Kafka error handling — https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html ; retry topics — https://docs.spring.io/spring-kafka/reference/retrytopic.html ; share consumer — https://spring.io/blog/2025/10/14/introducing-spring-kafka-share-consumer/
- Redpanda BSL — https://github.com/redpanda-data/redpanda/blob/dev/licenses/bsl.md ; NATS JetStream — https://docs.nats.io/nats-concepts/jetstream
- Debezium Outbox Event Router — https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html ; Debezium Server — https://github.com/debezium/debezium-server ; RDS/WAL dersleri — https://debezium.io/blog/2020/02/25/lessons-learned-running-debezium-with-postgresql-on-rds/
- CloudEvents spec / primer / bindings — https://github.com/cloudevents/spec
- Confluent schema evolution — https://docs.confluent.io/platform/current/schema-registry/fundamentals/schema-evolution.html ; Apicurio Registry — https://github.com/Apicurio/apicurio-registry
- Socket.IO Redis Streams adapter — https://github.com/socketio/socket.io-redis-streams-adapter ; Centrifugo design — https://centrifugal.dev/docs/getting-started/design

### Güvenlik ve gizlilik
- RFC 8725 JSON Web Token Best Current Practices — https://www.rfc-editor.org/info/rfc8725/
- RFC 9700 OAuth 2.0 Security Best Current Practice — https://www.rfc-editor.org/info/rfc9700/
- RFC 8693 OAuth 2.0 Token Exchange — https://datatracker.ietf.org/doc/html/rfc8693
- OWASP Microservices Security Cheat Sheet — https://cheatsheetseries.owasp.org/cheatsheets/Microservices_Security_Cheat_Sheet.html ; Secrets Management Cheat Sheet — https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html
- NIST SP 800-204B (servis mesh/mTLS) — https://csrc.nist.gov/pubs/sp/800/204/b/final
- NIST SP 800-63B-4 — https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-63B-4.pdf
- NIST SP 800-38D (GCM) — https://csrc.nist.gov/pubs/sp/800/38/d/r1/iprd
- SPIRE Docker workload attestor — https://github.com/spiffe/spire/blob/main/doc/plugin_agent_workloadattestor_docker.md ; Apache Camel SPIFFE örneği (2026) — https://camel.apache.org/blog/2026/09/camel-spiffe-workload-identity/
- Spring Security passkeys — https://docs.spring.io/spring-security/reference/servlet/authentication/passkeys.html
- Spring Cloud Config Server security — https://docs.spring.io/spring-cloud-config/reference/server/security.html ; CVE'ler — https://spring.io/security/cve-2026-40982/ , https://spring.io/security/cve-2026-22739/ , https://spring.io/security/cve-2026-47894/
- Spring Cloud Gateway CVE'leri — https://spring.io/security/cve-2025-41243/ , https://spring.io/security/cve-2025-41253/ , https://spring.io/security/cve-2025-41235/ , https://spring.io/security/cve-2026-47879/
- Spring Boot / Security / Framework CVE'leri — https://spring.io/security/cve-2025-22235/ , https://spring.io/security/cve-2026-22733/ , https://spring.io/security/cve-2025-22228/ , https://spring.io/security/cve-2024-38821/ , https://spring.io/security/cve-2024-38819/ , https://spring.io/security/cve-2025-41249/ , https://spring.io/security/cve-2026-41707/
- Spring Boot config tree (`configtree:`) — https://docs.spring.io/spring-boot/reference/features/external-config.html
- SOPS — https://github.com/getsops/sops ; OpenBao — https://openbao.org/ ; Infisical Spring Boot — https://infisical.com/docs/integrations/frameworks/spring-boot-maven
- Signal PQXDH — https://signal.org/docs/specifications/pqxdh/ ; SPQR — https://signal.org/blog/spqr/ ; safety numbers — https://support.signal.org/hc/en-us/articles/10223569377562-Automatic-Key-Verification
- MLS RFC 9420 — https://www.rfc-editor.org/info/rfc9420/
- libsignal — https://github.com/signalapp/libsignal ; `libsignal_protocol_dart` — https://pub.dev/packages/libsignal_protocol_dart ; `openmls` Dart — https://pub.dev/packages/openmls
- Grubbs, Lu, Ristenpart, "Message Franking via Committing Authenticated Encryption" (2017) — https://eprint.iacr.org/2017/664.pdf
- Neil Madden, "Galois/Counter Mode and random nonces" — https://neilmadden.blog/2024/05/23/galois-counter-mode-and-random-nonces/
- Twilio, preventing toll fraud — https://www.twilio.com/docs/verify/preventing-toll-fraud
- Google Play Integrity — https://developer.android.com/google/play/integrity/overview ; Apple App Attest — https://developer.apple.com/documentation/devicecheck/assessing-fraud-risk
- imgproxy options (`STRIP_METADATA`) — https://docs.imgproxy.net/configuration/options ; Cloudflare Images features — https://developers.cloudflare.com/images/optimization/features/
- Microsoft PhotoDNA Cloud Service — https://www.microsoft.com/en-us/photodna/cloudservice ; Google tools for partners — https://protectingchildren.google/tools-for-partners/ ; Technology Coalition CSAM reporting — https://technologycoalition.org/wp-content/uploads/CSAM-Identification_Reporting_R3-1.pdf
- FTC TAKE IT DOWN Act — https://www.ftc.gov/business-guidance/resources/complying-take-it-down-act
- GDPR erasure (DPC) — https://www.dataprotection.ie/en/individuals/know-your-rights/right-erasure-articles-17-19-gdpr ; ICO right to erasure — https://ico.org.uk/for-organisations/uk-gdpr-guidance-and-resources/individual-rights/individual-rights/right-to-erasure/
- KVKK: başvuruların cevaplanması — https://www.kvkk.gov.tr/Icerik/2046/ ; Silme/Yok Etme/Anonimleştirme Yönetmeliği — https://www.kvkk.gov.tr/Icerik/5441/ ; Kurul kararı 2021/1110 — https://www.kvkk.gov.tr/Icerik/7265/2021-1110 ; Kurul kararı 2023/924 — https://www.kvkk.gov.tr/Icerik/7763/2023-924
- EDPS, plakaların işlenmesi (2017) — https://edps.europa.eu/sites/default/files/publication/17-08-08_license_plates_ecb_en.pdf
- Crypto-shredding — https://granit-fx.dev/blog/crypto-shredding-gdpr-erasure-without-deleting-rows/

### Konum gizliliği
- Dhondt vd., "Swipe Left for Identity Theft", USENIX Security 2024 — https://www.usenix.org/conference/usenixsecurity24/presentation/dhondt ; Black Hat USA 2024 whitepaper — https://i.blackhat.com/BH-US-24/Presentations/US24-Dhondt-Swipe-Left-for-Identity-Theft-wp.pdf ; KU Leuven haberi — https://nieuws.kuleuven.be/en/content/2024/you-share-more-than-you-know-dating-apps-and-privacy-are-not-always-a-good-match
- Polakis vd., "Where's Wally?", CCS 2015 — https://www.cs.columbia.edu/~suphannee/papers/polakis.ccs2015.location.pdf
- Hoang vd., Grindr colluding attack (2016) — https://arxiv.org/pdf/1604.08235

### Dating-app domain'i
- Tinder Geosharded Recommendations (3 bölüm) — https://medium.com/tinder/geosharded-recommendations-part-1-sharding-approach-d5d54e0ec77a
- Tinder's move to Kubernetes — https://medium.com/tinder/tinders-move-to-kubernetes-cda2a6372f44
- TinVec (MLconf 2017) — https://mlconf.com/sessions/personalized-user-recommendations-at-tinder-the-t/
- Hinge "Most Compatible" (TechCrunch 2018) — https://techcrunch.com/2018/07/11/hinge-employs-new-algorithm-to-find-your-most-compatible-match-for-you/
- Bumble: multilingual message moderation — https://medium.com/bumble-tech/multilingual-message-content-moderation-at-scale-7ea562e29e25 ; Private Detector — https://medium.com/bumble-tech/bumble-inc-open-sources-private-detector-and-makes-another-step-towards-a-safer-internet-for-women-8e6cdb111d81
- Tinder Face Check (2025) — https://www.tinderpressroom.com/2025-10-22-Tinder-to-Expand-Facial-Verification-Feature-Across-the-U-S-,-Setting-a-New-Standard-for-Dating-Safety
- FTC romance scam verileri — https://www.ftc.gov/business-guidance/blog/2024/02/love-stinks-when-scammer-involved
- Apple App Store Review Guidelines (1.2, 5.1.1) — https://developer.apple.com/app-store/review/guidelines/
- Apple App Store Server API / Notifications V2 (WWDC22/23) — https://developer.apple.com/videos/play/wwdc2023/10141 , https://developer.apple.com/videos/play/wwdc2022/10040
- Google Play Billing security — https://developer.android.com/google/play/billing/security ; RTDN reference — https://developer.android.com/google/play/billing/rtdn-reference
- Ofcom Online Safety Act illegal harms — https://www.ofcom.org.uk/online-safety/illegal-and-harmful-content/statement-protecting-people-from-illegal-harms-online
- EU DSA transparency reporting — https://digital-strategy.ec.europa.eu/en/news/commission-harmonises-transparency-reporting-rules-under-digital-services-act
- 5651 sosyal ağ sağlayıcı yükümlülükleri (özet) — https://kilinclaw.com.tr/sosyal-ag-saglayici-icin-yukumlulukler-sorumluluklar-ve-yaptirimlar/
- Plaka.io haberi (2018) — https://egirisim.com/2018/01/27/plaka-io-trafikte-gordugunuz-araclara-yorum-yapabilmenizi-saglayan-uygulama/ ; Bump.com (2010) — https://www.popsci.com/cars/article/2010-09/social-networking-site-uses-license-plates-connect-drivers/
- Firebase → BigQuery export — https://firebase.google.com/docs/projects/bigquery-export ; Firebase FCM encryption guidance — https://firebase.google.com/docs/cloud-messaging/encryption
- OpenFeature (CNCF) — https://www.cncf.io/blog/2023/12/19/openfeature-becomes-a-cncf-incubating-project/ ; Java SDK — https://github.com/open-feature/java-sdk ; Unleash Java SDK — https://github.com/Unleash/unleash-client-java
- Push bildirim istatistikleri (ikincil) — https://www.mobiloud.com/blog/push-notification-statistics

### Gözlemlenebilirlik ve çalışma zamanı
- Grafana Promtail EOL — https://grafana.com/docs/loki/latest/send-data/promtail/ ; Alloy migration — https://grafana.com/docs/alloy/latest/set-up/migrate/from-promtail/
- Loki label best practices — https://grafana.com/docs/loki/latest/get-started/labels/bp-labels/ ; structured metadata — https://grafana.com/docs/loki/latest/get-started/labels/structured-metadata/
- OTel Collector tail sampling processor — https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/processor/tailsamplingprocessor/README.md
- Google SRE Workbook, "Alerting on SLOs" — https://sre.google/workbook/alerting-on-slos/
- Spring Boot structured logging (3.4) — https://spring.io/blog/2024/08/23/structured-logging-in-spring-boot-3-4/ ; OpenTelemetry with Spring Boot (2025) — https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/
- Spring Cloud 2022.0.0 (OpenFeign feature-complete) — https://spring.io/blog/2022/12/16/spring-cloud-2022-0-0-codename-kilburn-has-been-released ; Spring Cloud OpenFeign docs — https://docs.spring.io/spring-cloud-openfeign/reference/
- Spring Cloud CircuitBreaker Resilience4j — https://docs.spring.io/spring-cloud-circuitbreaker/reference/spring-cloud-circuitbreaker-resilience4j.html ; Resilience4j TimeLimiterConfig — https://github.com/resilience4j/resilience4j/blob/master/resilience4j-timelimiter/src/main/java/io/github/resilience4j/timelimiter/TimeLimiterConfig.java
- JEP 491 (virtual thread pinning) — https://openjdk.org/jeps/491
- Spring Cloud 2025.0 (Gateway renames) — https://spring.io/blog/2025/05/29/spring-cloud-2025-0-0-is-abvailable/ ; 2025.1 release notes — https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2025.1-Release-Notes ; 2026.0.0-M1 — https://spring.io/blog/2026/09/24/spring-cloud-2026-0-0-M1-has-been-released
- Spring Boot support policy — https://spring.io/support-policy ; 3.5 / 4.0 release notes ve migration guide — https://github.com/spring-projects/spring-boot/wiki ; Spring Framework versions — https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-Versions
- Oracle Java 25 duyurusu — https://www.oracle.com/news/announcement/oracle-releases-java-25-2025-09-16/
- Spring Cloud Config Server docs — https://docs.spring.io/spring-cloud-config/reference/server.html ; Spring Cloud Gateway docs — https://docs.spring.io/spring-cloud-gateway/reference/
