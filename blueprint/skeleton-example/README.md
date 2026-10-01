# skeleton-example — Doğrulanmış Boş İskelet

`blueprint/tests/*.java` şablonlarının, enforcer kuralının ve **generic outbox/inbox'ın** gerçekten derlenip çalıştığı en küçük Maven multi-module projesi. Referans dokümanın (Bölüm 3, 4, 7, 11.2–11.3, 16, 19.5–19.6, 23.3–23.4) somut, çalışan karşılığı.

- Spring Boot **4.1.1** BOM, Java 25 (derleme hedefi ve CI; prod image `eclipse-temurin:25-jre` ile aynı), ArchUnit 1.5.1, Maven 3.9.11.
- Modüller: `platform-core` (ErrorCode arayüzü, ServiceException), `platform-messaging` (generic outbox/inbox: `OutboxRepository`, `OutboxPoller`, `InboxProcessor`, `db/platform/outbox_inbox.sql`; local saga: `LocalSagaStore`, `SagaRecoveryWorker`, `SagaParticipant`, `db/platform/saga_coordinator.sql`), `order-api` (DTO), `order-core` (controller/service/impl/repository/entity/exception/config + yapısal testler). `broker-example` (seviye 3: gerçek RabbitMQ 4.3 topolojisi, `OutboxEventPublisher`, `OrderCancelledListener`, stream okuyucu; `BrokerBehaviourIT`).
- Config: `application-local.yml`, `config/order.yml`, `deploy/prod.env.example` (drift testi için).

## Doğrulama sonucu (2026-09-29)

```
mvn -B -ntp test   → BUILD SUCCESS
platform-messaging  OutboxBehaviourIT      13 test  (davranışsal, gerçek PostgreSQL 17.5 — gömülü, Docker gerekmez)
                    SagaBehaviourIT        19 test  (davranışsal, saga senaryoları 1–20; aynı gömülü PG)
broker-example      BrokerBehaviourIT      12 test  (davranışsal, gerçek RabbitMQ 4.3.0 + gömülü PostgreSQL 18.1; senaryo a–l, 5 mutasyon)
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

### Davranışsal (seviye 3: gerçek RabbitMQ 4.3.0 + gerçek PostgreSQL 18.1) — `BrokerBehaviourIT` (`broker-example`)

Üretici `order` şeması (generic outbox, `OutboxPoller` EVENT lane'i → `OutboxEventPublisher`) → `bvt.domain.events` topic exchange → `bvt.notification.order-cancelled.queue` **quorum queue** (`x-delivery-limit=3`, `x-dead-letter-strategy=at-least-once`, `x-overflow=reject-publish`, DLX `bvt.dlx` → `…dlq`, **native gecikmeli retry** `x-delayed-retry-type=failed`, min 1000 / max 5000 ms) → `OrderCancelledListener` (manuel ack, prefetch 10, `InboxProcessor` ile inbox satırı + etki tek TX) `notification` şemasında. Aynı exchange'e `#` ile bağlı `bvt.domain.events.stream` replay için. Broker Docker'sız, lokal RabbitMQ 4.3.0 (Khepri); PostgreSQL gömülü 18.1 (`embedded-postgres-binaries-bom` 18.1.0). Poller zamanı deterministik `Clock`, tüketici ölçümleri duvar saati.

| # | Senaryo | Sonuç |
|---|---|---|
| a | 20 outbox satırı tek TX → poll → 20 publisher confirm (callback sayacı), outbox boş, 20 etki + 20 inbox satırı, her olay tam bir kez | PASS |
| b | Binding'i olmayan routing key: `mandatory` + returns → `basic.return` → `UnroutableEventException` → satır PENDING, `retry_count=1`, backoff 60 sn; broker yine ACK verdiği için returned kontrolü olmadan olay sessizce kaybolurdu | PASS |
| c | İlk 2 teslimde geçici hata → `basic.reject requeue=true` → broker gecikmeli yeniden teslim: aralıklar ≥ 1000 ms ve ≥ 2000 ms (ölçülen 1010 / 2011 ms), `x-delivery-count` 1, 2; 3. teslim başarılı; tek etki | PASS |
| d | Her teslimde hata → ilk + 3 yeniden teslim (`x-delivery-count` 0,1,2,3; gecikmeler 1+2+3 sn) → at-least-once DLQ (management API derinlik 1), ana kuyruk boş, etki/inbox yok | PASS |
| e | Zehirli mesaj (JSON değil) → `basic.reject requeue=false` → anında DLQ, tek deneme | PASS |
| f | Inbox satırı yazıldıktan sonra iş patlar → TX rollback (inbox satırı da), `reject requeue=true` → gecikmeli yeniden teslim → başarı; tam 1 etki + 1 inbox satırı (ack yalnız commit sonrası) | PASS |
| g | Aynı olay (aynı `ce-id`) iki kez yayınlanır → APPLIED + DUPLICATE, 1 etki, 1 inbox satırı, kuyruk boş | PASS |
| h | `rabbitmqctl stop_app` → poll exception sızdırmaz: PENDING, `retry_count=1`, `last_error_code=AmqpConnectException` (host/mesaj yok); `start_app` → backoff sonrası yayın, container kendini toparlar (recoveryInterval), tüketici uygular | PASS |
| i | Stream: 5 olay → `x-stream-offset=first` ile 5'i sırayla (offset 0..4); ikinci okuyucu offset 0'dan aynısını alır (yıkıcı değil); quorum queue'daki 5 mesaj etkilenmez | PASS |
| j | `consumer-timeout=5000` policy'si: ack'lenmeyen mesajı QQ 5 sn sonra geri alır, tüketiciye `basic.cancel` gönderir (kanal kapanmaz), Spring container consumer'ı yeniden başlatır, mesaj `redelivered=true` ile gelir; ölçülen aralık ≥ 5 sn | PASS |
| k | Gecikmeli retry kuyruk argümanlarıyla etkin (management API `arguments`). Policy yolu sürüme bağlı: **4.3.0'da 400 "not recognised policy settings"**, **4.3.6'da (CI) kabul edilir** ve argümansız QQ'da reject sonrası ikinci teslim ≥ 900 ms gecikir | PASS |
| l | Binding `order.order.*` ile gelen bilinmeyen tip (`order.order.created`) loglanır, ack'lenir; etki/inbox/DLQ yok | PASS |

**Negatif doğrulama (mutasyon):** (1) publisher'da `CorrelationData.getReturned()` kontrolü kaldırıldı → b FAIL (`failed` 1 beklenirken 0: olay kayboldu); (2) ack `inbox.process` öncesine alındı → f ve c FAIL (commit başarısızken mesaj ack'lendi, kayıp; sonraki reject "unknown delivery tag" kanal hatası); (3) inbox dedup atlandı → g FAIL (`[APPLIED, APPLIED]`), a FAIL (inbox 0); (4a) container'da `defaultRequeueRejected=true` → d, e **değişmedi**: `AcknowledgeMode.MANUAL`'da container yalnız `AmqpRejectAndDontRequeueException(rejectManual)` için reject gönderir (Spring AMQP 4.1.1 `BlockingQueueConsumer.rollbackOnExceptionIfNecessary`), bayrak listener'ın kendi reject'lerini etkilemez — **eşdeğer mutasyon**; asıl koruma listener'ın açık `requeue=false`/`reject requeue=true` kararlarıdır; (4b) zehirli mesaj yolunda `requeue=true` → e FAIL (1 yerine 4 deneme); (4c) geçici hata yolunda `basic.nack requeue=true` → d FAIL (DLQ'ya hiç düşmez: sıcak döngü; 20 sn sınırında yakalandı), c FAIL (`x-delivery-count` artmadı, gecikme yok); (5) `x-delayed-retry-*` argümanları kaldırıldı → c FAIL (aralık 19 ms), k ERROR. Geri alınınca 12/12 yeşil.

**Broker'da doğrulanan gerçekler (referans 12.3 için düzeltme notları):** RabbitMQ 4.3.0'da `delayed-retry-type/min/max` **policy anahtarı olarak kayıtlı değildir** (`rabbit_policies` validator listesinde yok; `rabbitmqctl set_policy` "not recognised policy settings" der) — kuyruk argümanı `x-delayed-retry-*` kullanılır; `rabbit_quorum_queue:get_delayed_retry_config/1` policy'yi de okur ama policy tanımlanamaz. AMQP 0-9-1'de **`basic.reject requeue=true`** `modify{delivery_failed=true}` olarak işlenir (sayaç artar, `failed` tipi gecikme uygulanır, `delivery-limit` dolunca DLQ); **`basic.nack requeue=true`** ise düz `return`dür (`delivery_failed=false`): sayaç artmaz, gecikme yok, limit hiç dolmaz → gecikmesiz sonsuz requeue. `requeue=false` (nack/reject) anında dead-letter. Gecikme lineerdir: `min(min·delivery_count, max)`. `consumer-timeout` policy anahtarı geçerlidir ve **consume anında** okunur (sonradan konan policy mevcut tüketiciyi etkilemez); süre dolunca QQ mesajı geri alır ve `basic.cancel` gönderir (kanal kapanmaz), Spring container consumer'ı yeniden başlatır.

**Koşturulmayan (dürüst sınır):** `spring-rabbit-stream` (stream protokolü 5552) classpath'te olmadığından stream 0-9-1 `x-stream-offset` ile okundu; Spring Boot auto-config (`spring.rabbitmq.*` property'leri) yerine container/template elle kuruldu (aynı ayarlar); çoklu instance tüketici yarışı ve staging provası (seviye 4).

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

11. **Mutasyon "bayrak çevir" her zaman davranış değiştirmez:** `defaultRequeueRejected` MANUAL ack'te etkisizdir (4a). Konfigürasyon bayrağını mutasyona uğratmak yetmez; listener'ın verdiği fiili karar (requeue flag'i) mutasyona uğratılır (4b/4c) — aksi halde "yakalandı" sanılan koruma aslında yoktur.
12. **Management API istatistikleri gecikir (≈5 sn):** DLQ derinliği bir önceki testten bayat okunabilir; her test doğrulanmış sıfır derinlikten başlar ve önce listener gözlemi (deneme sayısı), sonra broker sayacı beklenir. Anlık sayım gerekince `queue.declare passive` (`RabbitAdmin.getQueueInfo`) kullanılır.
13. **Broker semantiği dokümana değil koda bakılarak doğrulanır:** `basic.nack` ile `basic.reject`'in `requeue=true` davranışı 4.3.0'da farklıdır; bu fark yalnızca gerçek broker + kaynak (`beam_lib` debug_info'dan `rabbit_channel`/`rabbit_fifo`) ile görüldü ve test c/d'ye kanıt olarak işlendi.
14. **Paylaşılan broker'da temizlik ve kaos:** benzersiz isim öneki (`bvt.`), `@AfterAll`'da topolojinin silinmesi, policy'nin `@AfterEach`'te kaldırılması; `stop_app/start_app` node'u ayakta bırakır ama çalışan diğer testlerin container'ları da düşer — kaos senaryosu tek başına koşturulur. Probe için açılan exchange'ler de silinir.

Bu dersler "yeşil build = kural çalışıyor" varsayımının yanlış olabileceğini gösterdi; bu yüzden `proj-release-readiness-review` ve CI, mimari testlerin **test sayısını** da doğrular (0 test = başarısız) ve davranışsal testler kasıtlı regresyonla (mutasyon) en az bir kez sınanır; eşdeğer mutasyon (davranışı değiştirmeyen) test açığı sayılmaz ama yazılır.

## Çalıştırma

```bash
cd blueprint/skeleton-example
mvn -B -ntp test                     # yapısal + davranışsal; gömülü PG binary'si Maven Central'dan gelir (io.zonky.test)
# broker-example icin lokal RabbitMQ 4.3+ (5672, management 15672 guest/guest) ve kaos senaryosu (h) icin rabbitmqctl gerekir:
#   -Dbvt.rabbitmqctl=/path/to/rabbitmqctl -Dbvt.rabbitmq.node=rabbit@localhost -Dbvt.erlang.bin=/path/to/erlang/bin
#   broker container'daysa (CI servis container'i): BVT_RABBITMQCTL="docker exec <container> rabbitmqctl" BVT_RABBITMQ_NODE=local
# root kullanıcıdaysan (initdb root'u reddeder):
runuser -u <non-root-user> --preserve-environment -- mvn -B -ntp -Dmaven.repo.local=$HOME/.m2/repository test
```
