# skeleton-example — Doğrulanmış Boş İskelet

`blueprint/tests/*.java` şablonlarının, enforcer kuralının ve **generic outbox/inbox'ın** gerçekten derlenip çalıştığı en küçük Maven multi-module projesi. Referans dokümanın (Bölüm 3, 4, 7, 11.2–11.3, 16, 19.5–19.6, 23.3–23.4) somut, çalışan karşılığı.

- Spring Boot **4.1.1** BOM, Java 21 (25 ile de uyumlu), ArchUnit 1.5.1, Maven 3.9.11.
- Modüller: `platform-core` (ErrorCode arayüzü, ServiceException), `platform-messaging` (generic outbox/inbox: `OutboxRepository`, `OutboxPoller`, `InboxProcessor`, `db/platform/outbox_inbox.sql`; local saga: `LocalSagaStore`, `SagaRecoveryWorker`, `SagaParticipant`, `db/platform/saga_coordinator.sql`), `order-api` (DTO), `order-core` (controller/service/impl/repository/entity/exception/config + yapısal testler).
- Config: `application-local.yml`, `config/order.yml`, `deploy/prod.env.example` (drift testi için).

## Doğrulama sonucu (2026-09-29)

```
mvn -B -ntp test   → BUILD SUCCESS
platform-messaging  OutboxBehaviourIT      13 test  (davranışsal, gerçek PostgreSQL 17.5 — gömülü, Docker gerekmez)
                    SagaBehaviourIT        19 test  (davranışsal, saga senaryoları 1–20; aynı gömülü PG)
order-core          ArchitectureRulesTest   8 test  (katman, controller→repository, impl paketi, config/, core→core, döngü, @Valid, api→entity)
                    ConfigDriftTest         3 test  (key kümeleri, ${ENV} ↔ env şablonu, secret fallback)
                    ErrorCodeUniquenessTest 1 test  (global tekillik, blok, mesaj formatı)
```

### Yapısal (seviye: kural derlenir, ihlal yakalanır)

**Negatif doğrulama:** kasıtlı 8 ihlal enjekte edildi (controller→repository, `@Valid`'siz `@RequestBody`, `service/` altında `@Configuration`, `service.impl`'de Impl olmayan sınıf, çakışan + blok dışı + noktasız ErrorCode, local'de olup deploy'da olmayan rate-limit scope'u, `${SECRET_DB_PASSWORD:changeme}` fallback'i) → **8 failure**, hepsi doğru kuralda yakalandı; kaldırılınca yeniden yeşil.

### Davranışsal (seviye 2: gerçek PostgreSQL) — `OutboxBehaviourIT`

| # (verification.md) | Senaryo | Sonuç |
|---|---|---|
| 21 | Outbox satırı domain TX ile rollback | PASS |
| 22 | Tekrar teslim tek etki; dedup kapsamı handler (aynı olay ikinci handler'da ayrı işlenir) | PASS |
| 25 | Handler ortasında exception → inbox satırı yok, etki yok; yeniden teslimde iş yapılır | PASS |
| – | İki poller paralel, 300 satır → her satır tam bir kez, tablo boş | PASS |
| – | SKIP LOCKED: başka TX'in kilitlediği satır beklenmez, atlanır | PASS |
| 27 | Aynı aggregate'in sıralı iki satırı: ikinci satır başka worker'a verilmez; ilk satır başarısızsa ikincisi bekler; sonra sırayla | PASS |
| 32 | Kira dolumu: 119 sn'de devralınamaz, 121 sn'de devralınır; geri gelen eski worker eski token'la silemez/PENDING'e çekemez, B'nin claim'i bozulmaz | PASS |
| 28 | HTTP lane'i sağlayıcıda takılıyken EVENT lane'i beklemeden yayınlar | PASS |
| – | Geçici hata → PENDING, retry_count 1, `last_error_code=IOException` (mesaj/host yok), backoff 60 sn; dolmadan alınmaz, dolunca işlenir | PASS |
| – | DEAD politikası iş türüne göre: kalıcı hata → DEAD; `NEVER_DEAD` (güvenlik yan etkisi) → PENDING | PASS |
| – | Yüksek `priority` en yeni olsa da önce claim edilir | PASS |
| 29 | Read-model UPSERT: küçük `source_revision` (geç gelen eski karar) yeni kararı ezmez; aynı revizyon da değiştirmez | PASS |
| – | Kira güvenlik payı: süre dolmak üzereyken yeni satıra başlanmaz (`deferred`), deneme sayılmaz, kira dolunca başka instance alır | PASS |

**Negatif doğrulama (mutasyon):** kod kasıtlı bozuldu, testler yakaladı: (1) claim sorgusundan `NOT EXISTS` (üretici sıralaması) kaldırıldı → #27 FAIL; (2) `claim_token` koşulu kaldırıldı → #32 FAIL; (3) `SKIP LOCKED` kaldırıldı → SKIP LOCKED testi timeout; (4) inbox satırı ile iş ayrı TX'e alındı → #25 FAIL; (5) kira dolumu koşulu (`locked_until <= now`) kaldırıldı → #32 ve güvenlik payı testi FAIL. Geri alınınca yeşil.

### Davranışsal (seviye 2: gerçek PostgreSQL) — `SagaBehaviourIT`

Koordinatör `order` şemasında (`saga`, `saga_steps`, `order_item`), katılımcı `subscription` şemasında (`quota`, `operation`) — aynı PG instance'ı, ayrı şemalar. Katılımcı in-process (`QuotaParticipant`); HTTP belirsizlikleri `FlakyParticipant` ile enjekte edilir (FAIL: istek ulaşmadı; LOSE_RESPONSE: katılımcı commit etti, yanıt kayboldu). Zaman deterministik `Clock` (deadline 15 sn, lease 60 sn, backoff `min(300, 2^n)`, uyarı 15 dk, retention 30 gün).

| # (verification.md) | Senaryo | Sonuç |
|---|---|---|
| 1 | Normal başarı: SUCCEEDED → worker confirm → CONFIRMED; hak düştü; sipariş yazıldı | PASS |
| 2 | Aynı key ile replay (confirm öncesi ve sonrası): aynı sonuç, ikinci consume yok | PASS |
| 3 | Aynı key farklı body: ilk istek kazanır | PASS |
| 4 | 8 thread eşzamanlı aynı key: tek saga, tek consume, tek sipariş; biri OK, diğerleri REPLAY/IN_PROGRESS | PASS |
| 5 | Farklı key aynı kaynak: domain unique reddeder → `fail()` → recovery compensate → iade | PASS |
| 6 / 18 | Aynı UUID farklı hesap → ayrı saga'lar; yanlış aktör (`chat-service`) → Forbidden, kayıt yok | PASS |
| 7 / 11 | begin sonrası çökme: deadline dolmadan dokunulmaz; dolunca compensate → tombstone (CANCELLED); geç consume uygulanmaz; aynı key ile retry → OPERATION_CANCELLED | PASS |
| 8 | Katılımcı commit + yanıt kaybı: istek 503; retry IN_PROGRESS; deadline sonrası recovery compensate → iade → COMPENSATED; retry → CANCELLED | PASS |
| 8b | Katılımcıya hiç ulaşmadı: recovery tombstone ile kapatır | PASS |
| 9 | Consume commit + domain yazılmadan çökme: iade | PASS |
| 10 / 20 | Domain commit + confirm öncesi çökme; "restart" (yeni store/worker nesneleri, yalnız DB) → CONFIRMED | PASS |
| 12 | confirm timeout: RETRY, attempt 1, `last_error_code`, backoff 2 sn; dolmadan alınmaz; sonra CONFIRMED. Yanıt kaybı: GET ile CONFIRMED görülür, retry yok | PASS |
| 13 | Eşzamanlı confirm ve compensate (10 tur): ya CONFIRMED + MANUAL_REVIEW (iade yok) ya COMPENSATED + Conflict (tek iade); asla ikisi | PASS |
| 14 | İki worker + expired lease: 59 sn'de devralınamaz, 61 sn'de devralınır; eski token ile complete/prepare boş | PASS |
| 15a | Recovery önce iptal etti (domain TX açıkken başka thread'de): success() CAS kaybeder, sipariş rollback, iade | PASS |
| 15b | Gerçek yarış, 20 tur, iki tarafa değişen gecikme: her tur ya (CONFIRMED, sipariş 1, kota 4) ya (COMPENSATED, sipariş 0, kota 5); üç koşuda 10/10, 8/12, 10/10 dağılım — iki dal da görüldü | PASS |
| 16 | Tekrarlanan compensate: tek iade; confirm sonrası compensate → MANUAL_REVIEW, iade yok | PASS |
| 17 | Eksik key / boş scope: saga açılmaz; TX dışında `success()` reddedilir (HTTP 400 binding'i MVC testinin işi) | PASS |
| 19 | Monitor: 16 dk sonra yalnız STARTED sayılır; cleanup 31 gün sonra yalnız CONFIRMED'i siler, MANUAL_REVIEW kalır, adımlar CASCADE | PASS |

**Negatif doğrulama (mutasyon):** (1) `success()` CAS koşulu kaldırıldı → 15a FAIL; (2) `prepare()` STARTED→CANCEL_REQUESTED yapmıyor → 7, 8, 8b, 9, 15a FAIL; (3) `complete()` lock_token koşulu kaldırıldı → 14 FAIL; (4) worker belirsizlikte GET sormuyor → 12 FAIL; (5) katılımcı tombstone yazmıyor → 7, 8b FAIL; (6) katılımcı advisory lock yok → 13 FAIL. (7) `refunded_at IS NULL` koşulu kaldırıldı → yakalanmadı: durum makinesi (`COMPENSATED` dalı replay) zaten çift iadeyi engelliyor, koşul yedek savunma — **eşdeğer mutasyon**, test açığı değil.

**Koşturulmayan (dürüst sınır):** katılımcının gerçek HTTP/JWT katmanı (allowlist, `act` claim'i, 400/401/403 binding'leri — seviye 1 MVC ve seviye 3 runtime testleri projede yazılır), gerçek broker ile yeniden teslim/ack (seviye 3), staging provası (seviye 4). Bu iskelet outbox/inbox ve saga durum makinesinin PostgreSQL üzerindeki güvencelerini kanıtlar.

## Denemede öğrenilen dersler (şablonlara işlendi)

1. **Enforcer `bannedDependencies`:** `com.acme:*-core` deseni `platform-core`'u da yakalar. Çözüm: `<includes><include>com.acme:platform-*</include></includes>` — ya da platform modüllerini `-core` ile bitirmemek.
2. **ArchUnit `@ArchTest` + JUnit engine:** Spring Boot 4.1 BOM'un yönettiği JUnit Platform ile ArchUnit'in kendi engine'i **0 test** çalıştırdı; build yeşil göründü ama hiçbir kural kontrol edilmedi. Kurallar düz `@Test` + `rule.check(classes)` olarak yazıldı; engine bağımlılığı yok.
3. **`Properties.stringPropertyNames()` tuzağı:** `YamlPropertiesFactoryBean` sayısal değerleri (`limit: 60`) Integer koyar; `stringPropertyNames()` bu key'leri **sessizce atlar** → drift testi rate-limit scope'larını hiç görmedi. `keySet()` + `String.valueOf` kullanılır.
4. **`layeredArchitecture().withOptionalLayers(true)`:** henüz `readmodel/` veya `outbox/` paketi olmayan yeni serviste "Layer is empty" ihlali üretmemesi için.
5. **`failIfNoTests` modül bazında:** parent pom'da açılınca testsiz kontrat modülleri (`platform-core`, `order-api`) build'i kırdı. Kural test içeren modüllerin kendi pom'unda; "0 test = başarısız" böyle sağlanır.
6. **DDL script'i `;` ile bölünmez:** yorum satırındaki `;` (`-- UUIDv7; ayni zamanda …`) naif `split(";")`'i kırdı. `ScriptUtils.executeSqlScript` kullanılır.
7. **Zaman kaynağı testte bile tuzaklı:** `next_retry_at = created_at` iken "gelecekte" oluşturulmuş satır claim edilmedi ve öncelik testi yanlış satırı gösterdi. Deterministik `Clock` + satır zamanlarını geçmişe koymak; testin kendi zamanı da kayıt altına alınır.
8. **Gömülü PostgreSQL root ile çalışmaz** (`initdb` reddeder): CI runner'larında sorun yok; root container'da test ayrı bir kullanıcıyla koşturulur (`runuser -u <user> -- mvn …`). Docker varsa Testcontainers aynı testi koşturur.

9. **Recovery worker'ı istek TX'inin thread'inde çağırmak yanlış test verir:** `JdbcTemplate` thread'e bağlı bağlantıyı kullanır; claim istek TX'inin içinde kalır, `REQUIRES_NEW` prepare onu göremez → "recovery hiçbir şey yapmadı" gibi görünür. Yarış testlerinde worker ayrı thread'de koşar (üretimdeki gibi).
10. **Yarış testinde iki dal da görülmeli:** worker'ı sabit anda başlatınca 20/20 hep aynı taraf kazandı (önce hep istek, sonra hep recovery). İki tarafa da değişen gecikme verilince dağılım ~10/10 oldu; test yalnız değişmezleri (invariant) doğrular, hangi tarafın kazandığını değil. Ayrıca `pastDeadline()` begin'den **sonra** çağrılmalı; önce çağrılınca adım hiç claim edilemedi (deadline saatle birlikte kaydı).

Bu dersler "yeşil build = kural çalışıyor" varsayımının yanlış olabileceğini gösterdi; bu yüzden `proj-release-readiness-review` ve CI, mimari testlerin **test sayısını** da doğrular (0 test = başarısız) ve davranışsal testler kasıtlı regresyonla (mutasyon) en az bir kez sınanır; eşdeğer mutasyon (davranışı değiştirmeyen) test açığı sayılmaz ama yazılır.

## Çalıştırma

```bash
cd blueprint/skeleton-example
mvn -B -ntp test                     # yapısal + davranışsal; gömülü PG binary'si Maven Central'dan gelir (io.zonky.test)
# root kullanıcıdaysan (initdb root'u reddeder):
runuser -u <non-root-user> --preserve-environment -- mvn -B -ntp -Dmaven.repo.local=$HOME/.m2/repository test
```
