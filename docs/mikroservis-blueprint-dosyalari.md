# Mikroservis Blueprint Dosyaları (tek dosya görünümü)

> `blueprint/` klasörünün birebir içeriği. Gizli klasörler (`.agents`, `.claude`, `.github`) bazı görüntüleyicilerde görünmediği için burada tek dosyada toplanmıştır. **Düzenleme `blueprint/` altında yapılır**; bu dosya `python3 scripts/build-blueprint-doc.py` ile yeniden üretilir.

## İçindekiler

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
- `.claude/hooks/tree-state.sh`
- `scripts/flyway-immutability.js`
- `scripts/flyway-immutability.test.js`
- `tests/ArchitectureRulesTest.java`
- `tests/ErrorCodeUniquenessTest.java`
- `tests/ConfigDriftTest.java`
- `skeleton-example/.gitignore`
- `skeleton-example/README.md`
- `skeleton-example/deploy/prod.env.example`
- `skeleton-example/order-api/pom.xml`
- `skeleton-example/order-api/src/main/java/com/acme/order/api/dto/CreateOrderRequest.java`
- `skeleton-example/order-core/pom.xml`
- `skeleton-example/order-core/src/main/java/com/acme/order/OrderApp.java`
- `skeleton-example/order-core/src/main/java/com/acme/order/config/WebConfig.java`
- `skeleton-example/order-core/src/main/java/com/acme/order/controller/OrderController.java`
- `skeleton-example/order-core/src/main/java/com/acme/order/entity/Order.java`
- `skeleton-example/order-core/src/main/java/com/acme/order/exception/ErrorCode.java`
- `skeleton-example/order-core/src/main/java/com/acme/order/repository/OrderRepository.java`
- `skeleton-example/order-core/src/main/java/com/acme/order/service/OrderService.java`
- `skeleton-example/order-core/src/main/java/com/acme/order/service/impl/OrderServiceImpl.java`
- `skeleton-example/order-core/src/main/resources/application-local.yml`
- `skeleton-example/order-core/src/main/resources/config/order.yml`
- `skeleton-example/order-core/src/test/java/com/acme/order/ArchitectureRulesTest.java`
- `skeleton-example/order-core/src/test/java/com/acme/order/ConfigDriftTest.java`
- `skeleton-example/order-core/src/test/java/com/acme/order/ErrorCodeUniquenessTest.java`
- `skeleton-example/platform-core/pom.xml`
- `skeleton-example/platform-core/src/main/java/com/acme/platform/core/ErrorCode.java`
- `skeleton-example/platform-core/src/main/java/com/acme/platform/core/ServiceException.java`
- `skeleton-example/platform-messaging/pom.xml`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/inbox/InboxProcessor.java`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/OutboxEvent.java`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/OutboxHandler.java`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/OutboxPoller.java`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/OutboxProperties.java`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/OutboxRepository.java`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/PermanentFailureException.java`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/saga/LocalSagaStore.java`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/saga/SagaCancelledException.java`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/saga/SagaParticipant.java`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/saga/SagaProperties.java`
- `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/saga/SagaRecoveryWorker.java`
- `skeleton-example/platform-messaging/src/main/resources/db/platform/outbox_inbox.sql`
- `skeleton-example/platform-messaging/src/main/resources/db/platform/saga_coordinator.sql`
- `skeleton-example/platform-messaging/src/test/java/com/acme/platform/messaging/OutboxBehaviourIT.java`
- `skeleton-example/platform-messaging/src/test/java/com/acme/platform/messaging/SagaBehaviourIT.java`
- `skeleton-example/platform-messaging/src/test/java/com/acme/platform/messaging/saga/FlakyParticipant.java`
- `skeleton-example/platform-messaging/src/test/java/com/acme/platform/messaging/saga/OrderFlow.java`
- `skeleton-example/platform-messaging/src/test/java/com/acme/platform/messaging/saga/QuotaParticipant.java`
- `skeleton-example/pom.xml`

---

## `README.md`

## Blueprint — Yeni Proje İskeleti (AI Yönetişimi, Skill'ler, Hook'lar, Makine Kuralları)

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
│       ├── review-gate.sh            # PreToolUse(Bash git push): son 1 saatte ve BU içerik üzerinde review skill'i çalıştı mı
│       ├── review-stamp.sh           # PostToolUse(Skill): damga = epoch + çalışma ağacı içerik hash'i
│       └── tree-state.sh             # ortak: git write-tree ile içerik kimliği (commit atmak damgayı bozmaz, dosya değiştirmek bozar)
├── scripts/
│   ├── flyway-immutability.js        # Kuralın TEK kaynağı: CI + hook + elle kullanım
│   └── flyway-immutability.test.js   # node --test
├── tests/                            # Makine zorlamalı kurallar için Java test şablonları (skeleton-example'da doğrulandı)
│   ├── ArchitectureRulesTest.java    # ArchUnit (düz @Test): katmanlar, controller→repository yok, core→core yok, config/, @Valid, döngü yok
│   ├── ErrorCodeUniquenessTest.java  # Tüm ErrorCode enum'larında global tekillik + blok + mesaj formatı
│   └── ConfigDriftTest.java          # application-local.yml ↔ deploy config drift; ${ENV} ↔ env şablonu; secret fallback yasağı
└── skeleton-example/                 # Boot 4.1.1 + ArchUnit 1.5.1 ile `mvn test` yeşil; 8 kasıtlı yapısal + 5 davranışsal ihlal yakalandı (README'sine bak)
    ├── pom.xml                       # BOM, ${revision}, enforcer (Java/Maven sürümü + core→core bannedDependencies), *IT dahil
    ├── platform-core/  order-api/  order-core/  deploy/prod.env.example
    └── platform-messaging/           # Generic outbox/inbox + local saga (JDBC); OutboxBehaviourIT (13) + SagaBehaviourIT (19): gerçek PostgreSQL üzerinde davranışsal senaryolar
```

### Doğrulama kapsamı (dürüst sınır — referans Bölüm 19.6)

| Seviye | Ne | Durum |
|---|---|---|
| **Yapısal** (kural derlenir, ihlal yakalanır) | `scripts/flyway-immutability.js` (12 test); hook'lar (11 senaryo: damga yok / damga var / içerik değişti / commit sonrası damga geçerli / ignore edilen dosya / eski biçim / git yok); `tests/*.java` + enforcer (`skeleton-example` içinde `mvn test`, pozitif + 8 kasıtlı ihlal) | **Doğrulandı** (2026-09-29) |
| **Davranışsal** (sistem koşarken tutarlılık güvenceleri) | outbox tekrar teslimi çift iş üretmez, iki worker aynı satırı işlemez, kira devri, inbox atomikliği, üretici sıralaması, lane izolasyonu, backoff/DEAD; saga: replay, eşzamanlı aynı key, çökme noktaları, yanıt kaybı, tombstone, istek-recovery yarışı, MANUAL_REVIEW, cleanup | **Doğrulandı** (2026-09-29, seviye 2, gerçek PostgreSQL 17.5, gömülü/Docker'sız): `OutboxBehaviourIT` 13 senaryo + `SagaBehaviourIT` 19 test (matris 1–20 + 21–32'nin outbox/inbox kısmı); 11 kasıtlı regresyon yakaladı (1 eşdeğer mutasyon). **Koşturulmadı:** katılımcı HTTP/JWT katmanı ve broker ile yeniden teslim (seviye 3), staging provası (seviye 4) — projede P0 çıkış koşulu |
| **Skill'ler** | 12 skill metni | Gerçek bir PR üzerinde Claude Code oturumunda henüz koşturulmadı; ilk kullanımda karar formatlarının uyumu gözden geçirilir |

Yapısal `PASS` davranışsal `PASS` değildir; uyum raporu ve PR şablonu ikisini ayrı yazar.

### Kurulum

```bash
cp -r blueprint/. <yeni-repo>/
cd <yeni-repo>
grep -rl "proj-\|<proje>" . --exclude-dir=.git | xargs sed -i 's/proj-/<proje>-/g; s/<proje>/<proje-adı>/g'
ln -s ../.agents/skills .claude/skills          # kopya değil, symlink
chmod +x .claude/hooks/*.sh
echo '.claude/.last-review-check' >> .gitignore   # review damgası yerel; commit'lenmez
node --test scripts/flyway-immutability.test.js  # script'in kendi testleri
bash -n .claude/hooks/review-gate.sh && echo '{"tool_input":{"command":"git push"}}' | .claude/hooks/review-gate.sh   # hook kuru çalıştırma → "ask"
echo '{"tool_input":{"skill":"proj-security-review"}}' | .claude/hooks/review-stamp.sh && echo '{"tool_input":{"command":"git push"}}' | .claude/hooks/review-gate.sh   # damga sonrası → sessiz (izin)
```

### İlkeler

1. **Tek kaynak:** Her kural bir dosyada yaşar; diğerleri anchor link ile yönlendirir. Skill'ler `docs/ai/*`'ı tekrar etmez.
2. **Kanıt zorunluluğu:** Doğrulanamayan şey "**net kanıt bulunamadı**" diye yazılır; uydurulmaz.
3. **Kural → makine:** Her kuralın bir makine kontrolü vardır (ArchUnit, enforcer, hook, CI script, test). Skill'ler "ne yapmalı"yı, testler "yapıldı mı"yı taşır.
4. **Kural sınıfları:** her kural zorunlu güvence / varsayılan tercih / başlangıç ayarı sınıfındadır (referans Bölüm 1.4); skill'ler sayıyı güvence gibi, güvenceyi tercih gibi ele almaz.
5. **Sabit karar formatları:** Her skill'in çıktısı sabit enum'larla biter (`APPROVE / REQUEST CHANGES / BLOCK`, `PASS / FAIL / BLOCKED`); serbest metin karar sayılmaz.
6. **Skill'ler kısa ve test edilebilir:** Her madde bir dosyaya bakarak evet/hayır denebilecek biçimde yazılır.

---

## `AGENTS.md`

## AGENTS.md — <proje> için AI Ajan Kuralları

Bu dosya tüm AI kodlama ajanları (Claude Code, Codex, Copilot, Cursor vb.) için **kanonik giriş noktasıdır**. `CLAUDE.md` ve `.github/copilot-instructions.md` yalnız buraya yönlendirir. Kurallar burada **tekrar edilmez**; ilgili dosyaya link verilir.

### 1. Okuma Sırası

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

### 2. Temel Kurallar

- Gereksiz geniş tarama yapma; `docs/ai/context-boundaries.md`'deki sınırlara uy.
- Doğrulanmamış bilgiyi kesin yazma. Dosya yolu, key, env, path, tablo, schema, secret, paket veya servis adı **uydurma**. Doğrulayamıyorsan "**net kanıt bulunamadı**" yaz ve sor.
- Mevcut bir pattern varsa onu kullan; yoksa yenisini icat etmeden önce sor.
- Secret, token, parola, private key değerlerini hiçbir yeni içeriğe (kod, doküman, test, log, PR açıklaması) taşıma. Yalnız isimleri yaz.
- Aynı kuralı iki yerde yazma; belgeye link ver.
- Kurallar üç sınıftadır (referans Bölüm 1.4): **zorunlu güvence** (ihlali `BLOCK`; değişmez), **varsayılan tercih** (sapma gerekçeli ADR ister), **başlangıç ayarı** (sayılar; ölçümle değişir). Bir sayıyı "kural" diye savunma, bir güvenceyi "tercih" diye gevşetme.
- Her değişiklikte ownership kontrolü: kimlik her zaman doğrulanmış bağlamdan (`@CurrentAccount`), path/body'den değil. Servis kimliği (service JWT) kullanıcı adına yetki **değildir**; internal uçlar `docs/ai/repo-context.md` Bölüm 3.1 delegasyon matrisine göre çağıran × işlem × kullanıcı bağlamı × kaynak yetkisini birlikte kontrol eder.
- Ana README kökteki `README.md`'dir; servis kimlik tablosu ve hata kodu blokları oradadır.

### 3. Kanıt ve Varsayım Disiplini

- Varsayımla kod, config, migration, endpoint veya güvenlik kuralı üretme.
- Kritik bir belirsizlik varsa (güvenlik, veri kaybı, ödeme, yasal) kod yazmadan önce sor.
- Bir iddiayı yazarken dayandığı dosyayı/satırı belirt.

### 4. Modül Sınırı ve DB Erişimi

- Hiçbir modül başka modülün tablosuna, schema'sına, repository'sine, entity'sine veya migration'ına erişmez. **Yönetim (backoffice) servisi de istisna değildir.**
- Cross-schema FK ve join yasaktır. Başka servisin kimliği düz UUID kolon olarak tutulur.
- Başka servisin verisi gerekiyorsa: public API/contract, event ile replike edilen read-model (`readmodel/` paketi) veya JWT claim. Böyle bir ihtiyaç doğarsa **kod yazmadan önce** `proj-architecture-boundary-review` skill'ini çalıştır ve sor.
- DB rolleri şema bazlıdır; kod bu sınırı aşmaya çalışırsa çalışma zamanında hata alır. Bu hatayı "GRANT ekleyerek" çözme.

### 5. Sıcak Yol Kuralı

- **Zorunlu güvence:** kullanıcıya latency olarak yansıyan her akışın kritik akış kaydı (`docs/ai/repo-context.md` Bölüm 3: gecikme bütçesi, uzak bağımlılıklar ve gerekçeleri, kabul edilen veri eskiliği, bağımlılık düşünce davranış) yazılıdır ve güncel tutulur.
- **Varsayılan tercih:** en fazla bir uzak senkron çağrı, o da yazma/rezervasyon türünden; okuma amaçlı senkron çağrı yerine read-model veya JWT claim. Varsayılanı aşan her ek bağımlılık ADR + `proj-resilience-review` ister; "ikinci çağrı" yasak değildir, gerekçesiz ve bütçesiz olanı yasaktır.
- Yeni bir uzak çağrı ekliyorsan kaydı güncelle ve `proj-resilience-review` skill'ini çalıştır.
- Her HTTP client çağrısı timeout + circuit breaker + bulkhead altındadır; bunlar olmadan client ekleme.

### 6. Migration Değişmezliği

- Base branch'e (`develop`/`release`/`main`) girmiş `V*.sql` dosyasına dokunulmaz (değiştirme, silme, yeniden adlandırma yok). Düzeltme yeni `V<sonraki>` ile yapılır.
- Migration'a her dokunuştan sonra: `node scripts/flyway-immutability.js check --base origin/<hedef-branch>`.
- Hook bunu yazma anında engeller; engellenirse kuralı aşmaya çalışma, yeni dosya aç.

### 7. API / Core Contract

- `<domain>-api` yalnız DTO, enum, sabit ve event payload'ı taşır; hiçbir `*-core`'a bağımlı olamaz.
- `<domain>-core` başka bir `*-core`'a bağımlı olamaz (enforcer bunu kırar).
- Entity dışarı açılmaz; DTO kopyalanmaz; hedefin api modülü import edilir.
- Contract değişikliği geriye uyumluluk açısından mobil, panel ve internal çağıranlar için değerlendirilir; `proj-api-contract-review` çalıştırılır.

### 8. Event ve Outbox

- Başka servisin verisini değiştirmek için komut gönderilmez; kendi domain event'in yayınlanır (`outbox_event`, `kind=EVENT`).
- Yayın her zaman outbox'tan; doğrudan `convertAndSend` yasak.
- Tüketici: inbox satırı ve iş değişikliği **aynı transaction'da**; ack commit'ten sonra. Read-model'de kaynak başına `source_revision`; olay sözleşmesi (tam durum / değişiklik) yazılı.
- Yeni event/komut için `proj-event-design-review` çalıştırılır; rollout sözleşmesi (referans Bölüm 18.4: değişiklik türüne göre sıra + uyumluluk matrisi) PR'a yazılır. "Tüketici önce" tek başına kural değildir.

### 9. Environment ve Config Etkisi

- Yeni property, URL, port, audience, issuer, rate limit scope'u, bağlantı ayarı, internal uç veya feature flag eklendiğinde şu yüzeyler **birlikte** güncellenir: deploy `env_file`/secrets, `config/<svc>.yml`, `application-local.yml`. Dockerfile yalnız build/runtime davranışı değişiyorsa.
- Dockerfile'a config için `ENV` eklenmez. Secret için `${ENV:literal}` fallback yazılmaz.
- Admin'in değiştirebileceği iş kuralı değeri config'e değil parametre kataloğuna gider.
- `proj-environment-impact-review` çalıştırılır.

### 10. Multi-Instance

- Her servisin birden fazla instance ile çalıştığı varsayılır. `@Scheduled`/poller/worker `FOR UPDATE SKIP LOCKED` + lease + `claim_token` ile claim eder.
- Check-then-act güvenli değildir; unique/partial unique index, koşullu update veya lock kullan.
- Instance'lar arası state JVM'de tutulmaz. Bilinçli single-instance davranış yorumla gerekçelendirilir.

### 11. Loglama

- Throw öncesi tek structured log satırı: `"<Olay> rejected: code=X reason=Y"`. Kullanıcı kaynaklı reddetme WARN, altyapı/beklenmeyen ERROR.
- Ham exception mesajı, body, token, OTP, telefon, e-posta, plaka, konum, IP, şifreli içerik loglanmaz. `docs/ai/security-rules.md`.

### 12. Kod Açıklama Disiplini

- Yorumlar kısa, ekibin dilinde (Türkçe, ASCII), "**neden** var" anlatır. Changelog tarzı yorum ("şu güncellendi") yasak.
- Business rule, güvenlik, modül sınırı, fallback, rate limit, ownership, migration etkisi ve non-obvious kararlar için yorum şart. Kendini açıklayan koda yorum yazılmaz.

### 13. Çıktı Disiplini

- Kısa, kanıta dayalı, görev odaklı. Güvenlik ve privacy riski açıkça yazılır. Belirsiz alan gizlenmez.
- Push öncesi `docs/ai/review-checklist.md`'deki skill'ler çalıştırılır; PR şablonundaki "Review skill'leri" bölümü doldurulur.

### 14. Bu Belgede Özellikle Taşınmayanlar

Secret değerleri, default admin parolası, üretim host adları, CI token'ları. Bunlar deploy secret'larında yaşar; dokümanlarda yalnız **isimleri** geçer.

### 15. Net Kanıt Bulunamayan Alanlar

Her `docs/ai/*` dosyasının sonunda bu başlık bulunur; ajan doğrulayamadığı iddiaları buraya ekler, uydurmaz.

---

## `CLAUDE.md`

## CLAUDE.md

Bu repo için tüm kurallar `AGENTS.md` dosyasındadır. Önce onu oku; göreve göre yönlendirdiği `docs/ai/*` dosyalarını aç.

Kural burada tekrar edilmez. Proje skill'leri `.claude/skills/` altındadır (`.agents/skills/`'e symlink). Değişiklik bitince `docs/ai/review-checklist.md`'deki skill'leri çalıştır.

---

## `.github/copilot-instructions.md`

## Copilot Instructions

Bu repo için tüm kurallar `AGENTS.md` dosyasındadır. Önce onu oku; göreve göre yönlendirdiği `docs/ai/*` dosyalarını aç. Kural burada tekrar edilmez.

Özellikle: secret değeri yazma; modül sınırını aşma; base'teki migration'a dokunma; sıcak yola okuma amaçlı uzak çağrı ekleme; kimliği path/body'den alma.

---

## `.github/PULL_REQUEST_TEMPLATE.md`

## Ne değişti

<!-- 2–5 madde. Neden yapıldığı; ne yapıldığı değil. -->

## Etki

- [ ] Contract (api modülü, OpenAPI diff): değişti / değişmedi — breaking: evet / hayır
- [ ] Migration: var / yok — `node scripts/flyway-immutability.js check --base origin/<hedef>` ✅
- [ ] Config/env/secret yüzeyi: `env_file` · `config/<svc>.yml` · `application-local.yml` · Dockerfile — güncellendi / etkilenmedi
- [ ] Yeni uzak senkron çağrı: var / yok — varsa kritik akış kaydı güncellendi (`docs/ai/repo-context.md` Bölüm 3); varsayılan (≤1) aşılıyorsa ADR: `…`
- [ ] Yeni/değişen internal uç: var / yok — varsa delegasyon matrisi satırı (`repo-context.md` Bölüm 3.1)
- [ ] Yeni event/komut/tüketici/şema/enum/claim değişikliği: var / yok — **rollout sözleşmesi** (referans Bölüm 18.4): tür: `…` · sıra: `…` · kırıcıysa uyumluluk matrisi (yeni→eski / eski→yeni / yeni→yeni / eski→eski): `…`
- [ ] İstemciyi etkiliyor: evet / hayır — `docs/<client>-<feature>-integration-vN.md`: `…`
- [ ] Güvenlik/privacy etkisi: var / yok — özet: …

## Çalıştırılan review skill'leri ve kararları

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

## Doğrulama

<!-- Yapısal (ArchUnit/enforcer/drift/immutability) ve davranışsal (outbox/inbox/saga/restart) ayrı yazılır. Davranışsal her PASS için kanıt kaydı. -->

**Yapısal:** `mvn verify` (commit `…`, CI job `…`): PASS / FAIL — test sayısı: `…` (0 = başarısız)

**Davranışsal kanıt kaydı** (yalnız tutarlılık/olay/saga değişikliklerinde):

| Senaryo | Seviye (1–4) | Test / komut | Commit | Ortam | Sonuç (link) | Tarih |
|---|---|---|---|---|---|---|
| | | | | | | |

## Net kanıt bulunamayan alanlar

<!-- Doğrulanamayan varsayımlar; boşsa "yok". -->

---

## `docs/ai/repo-context.md`

## repo-context.md — <proje> Repo Haritası

> Amaç: Ajanın ilk 2 dakikada sistemi anlaması. Kurallar burada değil; `AGENTS.md` ve `security-rules.md`'de. Bu dosya **gerçekleri** taşır ve her yeni servis/uç/olayda güncellenir.

### 1. Mimari şekil (ADR-0001)

- Şekil: **hibrit** — çekirdek domain'ler (`auth`, `user`, `<ana-iş>`, `subscription`) tek uygulama (`core-app`) içinde Spring Modulith modülleri; `chat` (realtime), `notification` (dış sağlayıcılar), `backoffice` (yönetim) ayrı servisler. *(Projeye göre düzenle: modüler monolit / mikroservis.)*
- Yeniden değerlendirme eşiği: referans Bölüm 24 (≥2 ekip, farklı ölçek profili, ikinci host).
- Repo: Maven multi-module monorepo; `platform/*` starter'ları; `services/<domain>/<domain>-api|core`.

### 2. Servis kimlik tablosu

| Servis | Port | `application.name` | Actor / `iss` | Audience | DB schema / rol | Hata kodu bloğu | Yayınladığı olaylar | Tükettiği olaylar | Sıcak yol uzak çağrı sayısı |
|---|---|---|---|---|---|---|---|---|---|
| gateway | 8080 | gateway | gateway | route metadata | – | – | – | – | – |
| core-app (auth, user, order, subscription modülleri) | 8081 | core | core-service | core-api | `auth`,`users`,`order`,`subscription` / `svc_*` | 10000–14999 | `account.*`, `user.*`, `order.*`, `subscription.*` | – | order.create: **1** (subscription consume, in-process değilse) |
| chat | 8094 | chat | chat-service | chat-api | `chat` / `svc_chat` | 15000–15999 | `chat.message.sent` (metadata) | `account.standing.changed`, `user.block.created` | message.send: **0** |
| notification | 8091 | notification | notification-service | notification-api | `notification` / `svc_notification` | 16000–16999 | `notification.delivered` | `*.commands`, `order.order.created`, … | – |
| backoffice | 8095 | backoffice | backoffice-service | backoffice-api | `backoffice` / `svc_backoffice` | 17000–17999 | `parameter.revision.published`, `moderation.action.applied` | `*.report.created` | – |

Ortak kod blokları: validation 90000, security 90100–90199, system 99998–99999.

### 3. Kritik akış kaydı — sıcak yol tablosu (referans Bölüm 1.2)

| Akış | Uç | Gecikme bütçesi (p99) | Uzak senkron bağımlılıklar (gerekçe) | Read-model / claim ile karşılanan kontroller (kabul edilen eskilik) | Bağımlılık düşünce davranış | Yeniden değerlendirme |
|---|---|---|---|---|---|---|
| Ana yazma | `POST /v1/orders` | 300 ms | 1: `subscription.consume` (hak tüketimi = yazma; read-model ile yapılamaz) | hesap durumu (`rm_account_status`, lag ≤ 5 dk), yasal onay (`legal_ok` claim, token ömrü) | subscription: 503 `UPSTREAM_UNAVAILABLE`; read-model satır yok: reddet | p99 > 300 ms 3 gün; `readmodel_lag_seconds{source="auth"}` > 300 |
| Mesaj gönder | `POST /v1/conversations/{id}/messages` | 150 ms | 0 | engel (`rm_block_relation`, lag ≤ 30 sn; aşılırsa fail-closed), üyelik (local) | – | lag alarmı |
| Giriş | `POST /v1/auth/password/login` | 500 ms | 0 | – | – | – |

Varsayılan: ≤1 uzak senkron çağrı. Aşan satır ADR + `proj-resilience-review` ister; kayıt alanlarından biri boş olan satır `REQUEST CHANGES`.

### 3.1 Delegasyon matrisi (referans Bölüm 9.2.1)

Her internal uç için bir satır. Hedef servis üçünü birlikte kontrol eder: çağıran allowlist'te mi, bu işlem için mi, `sub` varsa bu kaynakta yetkili mi.

| Çağıran (`act`) | Hedef işlem | Kullanıcı bağlamı (`sub`) | Kaynak yetkisi kontrolü | Bağlam kaynağı | Ele geçirilirse zarar |
|---|---|---|---|---|---|
| gateway | tüm public uçlar | zorunlu (user JWT) | hedef: ownership | kullanıcı isteği | tüm kullanıcı işlemleri |
| core-service (order) | `subscription: consume/confirm/compensate` | zorunlu; yalnız kendi sipariş akışındaki hesap | `operation_key` + hesap eşleşmesi | kullanıcı isteği (senkron) | yalnız hak tüketimi |
| core-service (worker) | `notification: commands` | yok (arka plan token'ı) | – | outbox | spam → rate limit |
| backoffice-service | `user: moderate` | yok; admin id ayrı claim | admin rolü + audit | panel isteği | moderasyon kararları |

Arka plan token'ıyla kullanıcı-yetkisi gerektiren işlem kabul edilmez; "her kullanıcı adına her şey" satırı yoktur.

### 4. Yüksek sinyalli dosyalar

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

### 5. Altyapı

| Bileşen | Sürüm | Not |
|---|---|---|
| Java / Spring Boot / Spring Cloud | 25 / 4.x / 2025.1.x | OSS destek kontrolü: `docs/versions.md` (tarihli) |
| PostgreSQL | 18 | tek instance, şema+rol/servis, PgBouncer, WAL-G → S3 |
| Valkey | 9 ×2 | `security` (noeviction+AOF, Sentinel) / `cache` (allkeys-lru) |
| RabbitMQ | 4.3 | quorum queue; `domain.events` topic; streams: `domain.events.stream` |
| Object storage | S3 uyumlu | `quarantine`, `delivery` (private), signed GET |
| Gözlem | Alloy → Loki/Tempo; Prometheus + Alertmanager; Grafana | portlar yalnız 127.0.0.1 |

### 6. Komutlar

```bash
mvn -B -ntp verify -pl services/order/order-core -amd      # değişen modül + bağımlıları
node scripts/flyway-immutability.js check --base origin/develop
npm --prefix <panel>-web run lint && npm --prefix <panel>-web run test
docker compose -f deploy/docker-compose.local.yml up -d     # local altyapı
```

### 7. Bu Belgede Özellikle Taşınmayanlar

Secret değerleri, üretim host adları, CI token'ları, kişisel veri örnekleri.

### 8. Net Kanıt Bulunamayan Alanlar

- (ajan doğrulayamadığı iddiaları buraya ekler; boşsa "yok")

---

## `docs/ai/security-rules.md`

## security-rules.md — Güvenlik ve Privacy Kuralları

> Tek kaynak. Skill'ler (`proj-security-review` başta) buraya link verir, tekrar etmez. İhlal = `REQUEST CHANGES` veya `BLOCK`.

### 1. Secret ve Config

- Secret değeri (parola, token, private key, API key, salt/pepper) **hiçbir** kod, config, doküman, test, log, PR metni veya commit mesajına yazılmaz. Yalnız **adı** geçer.
- Secret'lar `/run/secrets/` (compose `secrets:` + Spring config tree) ile gelir; git'te yalnız SOPS ile şifreli (`secrets/<env>.enc.yaml`).
- `${ENV:literal-varsayılan}` biçiminde secret fallback yazılmaz; secret eksikse uygulama **açılmaz** (fail-fast).
- Config Server kullanılıyorsa secret taşımaz.
- `.dockerignore`: `.env*`, `secrets/`, `.git`, `**/target`, `node_modules`.
- Local yml'de gerçek secret bulunmaz; local için ayrı, açıkça sahte değerler (`local-only-…`) ve profil guard'ı.

### 2. Kimlik ve Yetki

- Üç JWT yüzeyi (**user**, **service**, **admin**) birbirinin yerine kabul edilmez: farklı `typ`, farklı anahtar, farklı `aud`.
- Service JWT **asimetrik** (EdDSA/ES256); her imzalayıcının kendi anahtar çifti; `iss` = imzalayan; doğrulama JWKS + `kid`. Simetrik paylaşılan secret **yasak**.
- Hesap kimliği **yalnız** doğrulanmış bağlamdan (`@CurrentAccount` ← service JWT `sub`). Path/query/body'den kimlik alınmaz (IDOR). Path'teki id yalnız hedef kaynaktır; ownership serviste kontrol edilir.
- `/internal/**` uçları default-deny; `service-jwt.internal-access` allowlist'i gerçek kullanım kadar dar; dar kural catch-all'dan önce (first-match). Allowlist eşleşmesi decode+normalize edilmiş path üzerinde.
- Kullanıcı adına çalışan internal uçta JWT `sub` path'teki hesapla karşılaştırılır. Hesabı body'den alan internal uç hiçbir aktöre açılmaz.
- **Servis kimliği ≠ kullanıcı adına yetki.** A'nın imzası yalnız "A'dan geldi" demektir; A'nın token'a koyduğu `sub` adına işlem yetkisi vermez. Hedef üçünü birlikte kontrol eder: (1) `act`/`iss` allowlist'te, (2) bu **işlem** için, (3) `sub` bu **kaynakta** yetkili (ownership). Kullanıcı isteğiyle çalışan çağrı ile arka plan işi token'da ayrılır (`sub` yok / `on_behalf_of` ayrı claim); arka plan token'ıyla kullanıcı-yetkisi gerektiren işlem reddedilir. Zincirde (`A → B → C`) `act` zinciri korunur. Matris: `repo-context.md` Bölüm 3.1 (RFC 8693 delegation/impersonation ayrımı).
- Gateway: `/internal/**` → 404 (prefix kırpma sonrası da), iç header'ları (`X-Subject-Id`, `X-User-*`) temizler, `X-Forwarded-*` yalnız güvenilen proxy'den, CORS listesi açık (`*` yasak), rate limit ve timeout bütçesi var, `gateway` actuator ucu kapalı.
- Oturum sürümü (`sv`): şifre sıfırlama, logout-all, **ban**, rol değişimi → `sv + 1`; gateway ve realtime bu sürümün altını reddeder.
- Refresh token: opak, `sha256(plain:salt)` ile saklanır, aile rotasyonu, reuse → aile iptali. Mutlak süre uzamaz.
- Admin 2FA: passkey/WebAuthn veya TOTP. SMS yalnız kurtarma. Admin OTP'si de tuzlu hash + sabit zamanlı karşılaştırma.
- Roller token'da taşınıyorsa değişimde `sv` artar; panel ve sunucu aynı rol kaynağını okur.

### 3. OTP, Şifre, Abuse

- OTP: `SecureRandom`, 6 hane, `sha256(salt:code)`, sabit zamanlı karşılaştırma, TTL + deneme sınırı, enumeration koruması (bilinmeyen numaraya aynı yanıt). Outbox payload'ında OTP düz metin **kalmaz** (iletim sonrası NULL/şifreli).
- SMS pumping: ülke/prefix allowlist, `libphonenumber` doğrulama, numara/IP/cihaz başına günlük tavan, velocity alarmı, harcama kesicisi.
- Cihaz attestation (Play Integrity / App Attest) kayıt, OTP-gönder ve suistimale açık aksiyonlarda **kademeli risk sinyali** (ikili kapı değil).
- Şifre: BCrypt, 72 byte sınırı uzunluk kuralına yansır; politika parametre kataloğundan; hatalı denemede kilit (`retryAfterSeconds` details'te).
- Rate limit: Redis Lua fixed window, key `rl:<scope>:<sha256>`; sayaç JVM'de tutulmaz; scope'lar `<aksiyon>-ip|account|transaction|device`; fail politikası scope başına README tablosunda (güvenlik yüzeyleri fail-closed, iş yüzeyleri fail-open + metrik).
- Mock entegrasyonlar (mağaza makbuzu, SMS, ödeme) yalnız `@Profile("local|test")`; prod açılışında mock bean varsa uygulama **açılmaz**.

### 4. Loglama ve Hata Yanıtı

**Hiçbir seviyede loglanmaz:** OTP, access/refresh/service/admin token, şifre, secret, salt, private key; ham telefon, e-posta, kimlik no, plaka, kesin konum, IP, user-agent; şifreli içerik ve anahtar; request/response body, DTO/entity `toString()`, ham path/query/header, credential içeren URL; sağlayıcı yanıtı, ham exception mesajı, stack trace (yalnız 500'lerde sanitize edilmiş özet + trace).

- Throw öncesi tek structured log: `"<Olay> rejected: code=X reason=Y"`. Yalnız `code=`, `reason=`, `category=`, `exceptionType=` (`getSimpleName`), gerekli teknik id.
- Log'a özel neden `ServiceException.safeLogReason/safeLogCategory`'de; **asla** `details` içinde (details istemciye döner).
- `details` yalnız istemciye gösterilebilir `anahtar=değer` bilgisi taşır.
- UUID, hesap id, mask ve hash otomatik güvenli değildir; alanlar allowlist ile seçilir; trace context tercih edilir. Parameterized logging sanitization yerine geçmez.
- Structured JSON log; trace/kullanıcı id'si Loki **label** değil (structured metadata).
- Log privacy testi: sentetik hassas işaret **yok**, güvenli alan (`outcome=`) **var**.

### 5. Veri ve Privacy

- Kişisel veri envanteri ve DPIA ürünle birlikte başlar. Aydınlatma metni alanları sayar (telefon, e-posta, kimlik, plaka, konum, belge, cihaz).
- Düşük entropili kimlikler (telefon, plaka, e-posta) için düz SHA-256 **yetersiz**: secret pepper'lı **HMAC** (arama) + şifreli orijinal (gösterim).
- Kimlik sorgulama uçlarında **varlık oracle'ı** yok: kayıtlı olmayan/gizli/engelli için tek tip "sonuç yok"; hız limiti, velocity alarmı, audit.
- Konum: rastgele fuzzing **yetersiz**; kullanıcı başına deterministik grid/ofset (~1–3 km), aynı yuvarlanmış nokta geo sorgusu ve filtrelerde; mesafe aralık olarak; kesin mesafe sıralaması yok.
- Kişisel veri dönen uçlar `Cache-Control: private, no-store`.
- Arama index'i, cache, log, yedek aynı kurallara uyar.
- Export (taşınabilirlik) keyset sayfalı internal uçlarla; **silme** silme saga'sı ile (tüm servisler, index, cache, object storage tüm versiyonlar, üçüncü taraflar); yasal saklama gerekenler anonimleştirilir; yedekler için crypto-shredding. KVKK 30 gün / GDPR 1 ay.
- Uçtan uca şifreleme iddiası varsa Signal/MLS; değilse "sunucu okuyamaz" ile sınırlı ve sınırlamalar yazılı. İstemci karşı tarafın anahtarını pin'ler. Şikayet kanıtı **message franking** ile; sohbet anahtarı panele verilmez. Kanıt erişimi audit'li, `no-store`, retention'lı.

### 6. Dosya Yükleme

- Presigned PUT → **private quarantine** bucket → worker: magic byte/boyut/pixel-bomb → **yeniden kodlama** (EXIF/GPS temizlenir) → CSAM hash eşleme + NSFW sınıflandırma → private delivery bucket → kısa ömürlü signed GET / imzalı CDN.
- Public bucket'a doğrudan yükleme **yasak**.
- Yükleme sonrası HEAD/ETag doğrulaması; yetim dosya temizliği (cron + dağıtık kilit).

### 7. Veritabanı

- Servis başına **iki** DB rolü: `svc_<x>_migrate` (şema sahibi, DDL; yalnız Flyway) ve `svc_<x>` (uygulama; tablo/sequence DML, `ALTER DEFAULT PRIVILEGES` ile). Uygulama rolü DDL yapamaz, audit/append-only tablolarda UPDATE/DELETE yetkisi yoktur. Başka şemaya USAGE yok. Cross-schema erişim hatası "GRANT ekleyerek" çözülmez.
- `spring.flyway.baseline-on-migrate` config'te **açık tutulmaz**; mevcut DB'yi Flyway'e alma tek seferlik belgelenmiş `baseline` prosedürüdür.
- Audit tabloları `@Immutable` + DB'de `REVOKE UPDATE, DELETE` / trigger.
- Seed/test verisi prod migration location'ında değil; bilinen parolalı admin tohumlanmaz (bootstrap runner + env + ilk girişte değiştir).
- Yedek şifreli; restore provası aylık.

### 8. Tracing

- Trace id yetki sinyali değildir. Span attribute'larına PII konmaz. Route'lar static template. Redis zarfında trace metadata HMAC kapsamında.

### 9. Tedarik Zinciri

- Image CI'da build, cosign imzalı, digest ile deploy; non-root; base image Renovate ile güncel.
- gitleaks pre-commit + CI; Dependabot/GitHub Advisory alarmları; yaması yalnız ticari sürümde olan CVE = upgrade tetikleyicisi.
- GPL lisanslı kütüphane kapalı kaynak uygulamaya bağlanmadan hukuki inceleme.

### 10. Bu Belgede Özellikle Taşınmayanlar

Secret değerleri, pepper/salt değerleri, üretim IP/host adları, mevcut CVE listesi (Dependabot'ta yaşar).

### 11. Net Kanıt Bulunamayan Alanlar

- (ajan ekler)

---

## `docs/ai/context-boundaries.md`

## context-boundaries.md — Token Ekonomisi ve Tarama Sınırları

> Amaç: Ajanın gereksiz dosya okumasını önlemek; büyük repoda doğru yüzeyi doğru zamanda açmak.

### 1. Varsayılan olarak context DIŞI

`**/target/`, `**/node_modules/`, `**/dist/`, `**/build/`, `.idea/`, `.vscode/`, `*.log`, `.env*`, `secrets/`, `**/*.enc.yaml`, `obs/**/data/`, `postman/`, `docs/<client>-*-integration-v*.md` (yalnız istemci dokümanı görevinde), `docs/adr/` (yalnız mimari karar görevinde), `**/src/test/resources/**/*.json` (fixture'lar), image/binary dosyalar.

### 2. Şartlı açılacak yüzeyler

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

### 3. Tarama disiplini

- Önce `docs/ai/repo-context.md` Bölüm 4 "yüksek sinyalli dosyalar"; sonra grep ile daraltılmış hedef; sonra dosya.
- Bir dosyayı tamamen okumadan önce boyutuna bak; 500 satırı geçen dosyada önce `grep -n "class\|public .*(" ` ile harita çıkar.
- Aynı dosyayı iki kez okuma; not al.
- "Bütün repoyu tara" isteği gelirse önce hangi soruya cevap arandığını netleştir; soruya göre yukarıdaki tablodan yüzey seç.
- Büyük doküman (referans, plan) yalnız ilgili bölümüyle açılır (`grep -n "^## "` ile başlık haritası → offset ile oku).

### 4. Bu Belgede Özellikle Taşınmayanlar

Repo'nun dosya sayısı/boyutu gibi hızlı değişen sayılar.

### 5. Net Kanıt Bulunamayan Alanlar

- (ajan ekler)

---

## `docs/ai/review-checklist.md`

## review-checklist.md — Değişiklik Sonrası Kontrol Listesi

> Push öncesi çalıştırılacak skill'ler ve hangi değişiklikte hangisinin zorunlu olduğu. Skill'ler `.claude/skills/` (→ `.agents/skills/`) altındadır. Her skill'in nihai kararı PR şablonundaki tabloya yazılır.

### 1. Değişiklik türü → zorunlu skill'ler

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

### 2. Makine kontrolleri (CI'da; lokalde de çalıştırılır)

```bash
mvn -B -ntp verify -pl <değişen modüller> -amd          # testler + ArchUnit + ErrorCode tekilliği + config drift
node scripts/flyway-immutability.js check --base origin/<hedef>
gitleaks detect --no-banner
npm --prefix <panel>-web run lint && npm --prefix <panel>-web run build && npm --prefix <panel>-web test
```

### 3. Öz-kontrol (skill'lerden bağımsız)

- [ ] Yeni uzak senkron çağrı sıcak yola eklendi mi? Eklendiyse kritik akış kaydı (`repo-context.md` Bölüm 3: bütçe, gerekçe, eskilik, düşünce davranış) güncellendi; varsayılan (≤1) aşılıyorsa ADR.
- [ ] Yeni/değişen internal uç delegasyon matrisinde (`repo-context.md` Bölüm 3.1).
- [ ] Yeni tüketici: inbox satırı + iş aynı TX; ack commit sonrası.
- [ ] Olay/uç/şema değişikliği için rollout sözleşmesi satırı PR'da (referans Bölüm 18.4).
- [ ] Yeni outbox satırı domain transaction'ında mı (MANDATORY)?
- [ ] Hata yolu: throw öncesi structured log; kullanıcı 4xx → WARN.
- [ ] Yeni `@RequestBody` → `@Valid`; binding adları açık.
- [ ] Config yüzeyleri birlikte güncellendi (`env_file`, `config/<svc>.yml`, `application-local.yml`).
- [ ] Testler: negatif + yetki + concurrency (poller/check-then-act) + log privacy.
- [ ] `docs/ai/repo-context.md` ve README kimlik tablosu güncel.
- [ ] Mimari karar verildiyse ADR açıldı.

### 4. Karar formatları

- Review skill'leri: `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`.
- Tutarlılık ve release-readiness: `PASS` / `FAIL` / `BLOCKED`.
- Saga uygunluğu: `saga unnecessary` / `existing saga suitable` / `extension required` / `decision blocked`.
- Sorun yoksa sabit cümle: **"Bu kapsamda bulgu yok."** Spekülatif bulgu üretilmez; kanıt yoksa `needs verification`.

---

## `docs/ai/operation-consistency.md`

## operation-consistency.md — Servisler Arası Tutarlılık Standardı

> Tek kaynak. `proj-operation-consistency-review` ve `proj-event-design-review` buraya link verir. Referans: mimari doküman Bölüm 11–12.

### 1. Mekanizma seçimi (en basit yeterli olan)

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

### 2. Idempotency

- Public tekrar-güvenli işlemler `X-Idempotency-Key` (UUID) alır. Aynı niyetin retry'ları aynı key; yeni niyet yeni key.
- Tekillik `(account_id, scope, operation_key)`. Aynı key farklı body → ilk istek kazanır (fingerprint tutulmaz, belgelenir).
- Tamamlanmış istek aynı sonucu döner; süren `OPERATION_IN_PROGRESS`; iptal edilmiş `OPERATION_CANCELLED`.
- Veri seviyesinde: `INSERT … ON CONFLICT DO NOTHING`, deterministik id (`UUID.nameUUIDFromBytes(kaynak:hedef:faz)`) + unique.

### 3. Outbox (tek generic tablo: `outbox_event`)

- Yazıcı `Propagation.MANDATORY`; domain TX'i olmadan outbox yazılamaz.
- Poller `platform-messaging`'den: CTE + `FOR UPDATE SKIP LOCKED` + `locked_until` lease + `claim_token`; uzak iş TX **dışında**; sonuç yalnız claim sahibi tarafından yazılır.
- Sabitler: batch 50, lease 120 sn, güvenlik payı 30 sn, backoff `min(600, 30·2^n)`, STUCK alarmı her 10 denemede (ERROR + `outbox_oldest_pending_age_seconds` metriği).
- DEAD politikası iş türüne göre: güvenlik yan etkisi (ban, engel) **asla DEAD olmaz**; TTL'li mesaj (OTP) `expires_at` sonrası DEAD; kalıcı 4xx (401/403/408/429 hariç) DEAD.
- Payload'da gereksiz PII/secret yok; iletim sonrası hassas alan NULL.
- **Üretici tarafı sıralama:** sıra gereken `aggregate_id` için claim sorgusu aynı aggregate'in satırlarını tek worker'a `created_at` sırasıyla verir; bir satır başarısız olursa sonrakiler bekletilir. Single-active-consumer tek başına yeterli değildir.
- **Lane izolasyonu:** `kind` (EVENT/COMMAND/HTTP) başına ayrı claim döngüsü ve worker havuzu; `priority` kolonu (güvenlik kararları en yüksek); toplu işler ayrı grup/sınırlı concurrency. Yavaş HTTP hedefi event yayınını bekletemez. Metrikler lane bazında.
- `claim_token` yalnız poller'ın yazma yarışını çözer; uzak hedefe çift teslimi engellemez → hedef idempotent. Publisher confirm ≠ tüketici işledi; "tamamlandı" bilgisi tüketicinin kendi olayıyla gelir.
- Sıra numaralı durum senkronu: karar DB sequence'ından (aggregate başına monoton) sıra alır; alıcı küçük/eşit sırayı yok sayar (`applied=false` ile başarı döner).
- Superseded kontrolü: göndermeden önce daha yeni karar varsa satır gönderilmeden silinir. **Zorunlu güvence:** eski güvenlik kararı yeniden deneme yüzünden yeni kararı asla ezmez (üretici superseded + tüketici `source_revision`).

### 4. Event (CloudEvents) ve tüketici

- `id` (UUIDv7), `source` (servis), `type` (`<servis>.<aggregate>.<olay>`), `subject` (aggregate id), `time`, `dataschema`, `traceparent`.
- Tüketici (**inbox atomikliği, zorunlu güvence**): `INSERT INTO inbox_event(handler, event_id) … ON CONFLICT DO NOTHING` ve iş değişikliği **aynı TX'te**; 0 satır → duplicate, çık; ack yalnız commit'ten sonra (manual ack). Dedup kapsamı **handler**'dır. Dış yan etki inbox TX'i içinde yapılmaz; aynı TX'te outbox satırı olarak yazılır. Bilinmeyen `type` **yok sayılır** (komutlarda DLQ).
- Read-model: **kaynak başına** projeksiyon ve `source_revision` (kaynaklar arası revizyon karşılaştırılmaz); `rm_consumer_position` ile tüketim konumu (tazelik buradan ölçülür, satır yaşından değil); olay sözleşmesi yazılı: **tam durum** (küçük revizyon atlanabilir) mi **değişiklik** (hiç olay atlanamaz; sıra boşluğunda dur + uzlaştır + alarm) mi; karar başına kabul edilen eskilik T ve aşılınca davranış (fail-closed varsayılan); replay deterministik; tombstone.
- Şema evrimi: alan ekleme uyumlu; silme/yeniden adlandırma/tip değişimi → yeni `type`, bir süre çift yayın. Rollout değişiklik türüne göre (referans Bölüm 18.4) + kırıcıysa 4 hücreli uyumluluk matrisi; image rollback veri rollback'i değildir.
- Read-model kaynak değildir; dışa açılmaz; rebuild yolu belgelidir.

### 5. Local saga (tek adım)

- Koordinatör tabloları koordinatörün şemasında: `saga` (`UNIQUE(account_id, scope, operation_key)`), `saga_steps` (`next_action CONFIRM|COMPENSATE`, `lock_token`, `locked_until`).
- Akış: `begin()` ayrı TX'te (`ON CONFLICT DO NOTHING`; yeni kayıt → step `COMPENSATE/PENDING`, `next_attempt_at = now + deadline 15 sn`; varsa replay) → katılımcıya `consume` (TX dışı, aynı `operationKey`, circuit breaker altında) → domain yazımı + `success()` **aynı local TX'te**, ayrı bean (`success` compare-and-set; recovery iptal ettiyse rollback) → recovery worker: `claim` (SKIP LOCKED + `lock_token` + lease 60 sn) → `prepare` (`FOR UPDATE`) → uzak `confirm`/`compensate` → `complete` (token eşleşmesi) → hata/belirsiz sonuçta `GET` ile durum sorgusu; çelişki → `MANUAL_REVIEW`.
- `monitor` (60 sn): 15 dk'dan eski çözülmemiş → ERROR + metrik. `cleanup`: terminal kayıtlar 30 gün sonra; `MANUAL_REVIEW` silinmez.
- Katılımcı: `UNIQUE(caller_service, account_id, operation_key)`; `caller_service` JWT `act`/`iss`'ten; iki katmanlı yetki (allowlist + aktör → izinli işlem tipi); advisory lock + `FOR UPDATE`; tablo: consume/confirm/compensate sonuçları (`APPLIED/REJECTED/CONFIRMED/CANCELLED tombstone/COMPENSATED/MANUAL_REVIEW`); çift iade `original_id` ile engellenir.
- Merkezi coordinator servisi kurulmaz; çok adımlı ihtiyaç doğarsa `LocalSagaStore` genelleştirilir (step adı parametre) — ADR ile.
- Zaman kaynağı DB `now()`.

### 6. Zorunlu doğrulama matrisi

Kanıt seviyeleri: (1) unit + MVC, (2) gerçek PostgreSQL (Testcontainers), (3) owner→participant runtime, (4) release. Sonuç `PASS/FAIL/BLOCKED`. Her `PASS` bir **kanıt kaydı** ister (Bölüm 9).

Senaryolar: normal başarı ve replay · aynı key farklı body · eşzamanlı aynı key · farklı key aynı kaynak · aynı UUID farklı hesap/aktör · intent sonrası çökme · katılımcı commit + yanıt kaybı · consume commit + domain rollback · domain commit + confirm öncesi çökme · geç consume vs tombstone · confirm/compensate timeout · eşzamanlı confirm ve compensate · iki worker + expired lease · request success vs recovery cancel yarışı · tekrarlanan compensate · eksik/bozuk key · geçersiz JWT / yanlış aktör · cleanup ve monitor · migration ve restart · outbox satırı domain TX ile rollback · tüketici duplicate olay · sıra bozuk olay · bilinmeyen tip · inbox satırı + iş aynı TX (handler ortasında exception → satır yok) · commit sonrası ack öncesi çökme → duplicate yutulur · iki poller instance'ı, aynı aggregate'in sıralı iki satırı → sıra korunur · bir lane'de takılı hedef diğer lane'i durdurmuyor · eski güvenlik kararı yeni kararı ezmiyor · publisher confirm alınmış, tüketici işlememiş → "tamamlandı" sayılmıyor · delta olayında sıra boşluğu → dur + alarm · snapshot olayında küçük revizyon yok sayılır.

### 7. Bu Belgede Özellikle Taşınmayanlar

Somut servis adları ve operasyon tipleri (`repo-context.md`'de).

### 8. Net Kanıt Bulunamayan Alanlar

- (ajan ekler)

### 9. Kanıt Kaydı ve Doğrulama Kapsamı (referans Bölüm 19.6)

İki seviye karıştırılmaz: **yapısal** (ArchUnit/enforcer/drift/immutability: kural derlenir ve ihlal yakalanır) ve **davranışsal** (sistem koşarken tekrar teslim çift iş üretmez, iki worker aynı satırı işlemez, restart sonrası iş devralınır). Yapısal `PASS` davranışsal `PASS` değildir.

Her davranışsal `PASS` şu alanlarla kaydedilir: `senaryo · kanıt seviyesi · test/komut · commit SHA · ortam (CI job / Testcontainers sürümü) · sonuç (link) · tarih`. Testi olmayan senaryo `BLOCKED`; "yazılı ama koşulmamış" `PASS` sayılmaz. Review damgası (`review-gate` hook'u) kanıt değildir; zorunlu güvence CI'dır (test sayısı dahil: 0 test = başarısız).

Başlangıç noktası: `blueprint/skeleton-example/platform-messaging` — `OutboxBehaviourIT` ve `SagaBehaviourIT` matrisin 1–32 satırlarının seviye 2 (gerçek PostgreSQL) halini içerir; projeye kopyalanıp katılımcı gerçek HTTP client'ıyla (seviye 3) genişletilir.

---

## `docs/adr/0000-template.md`

## ADR-0000: <Karar başlığı>

- **Durum:** Önerildi | Kabul edildi | Reddedildi | Yerini aldı: ADR-NNNN
- **Tarih:** YYYY-MM-DD
- **Karar verenler:** …
- **İlgili eşik (referans Bölüm 24):** … (bu kararın hangi metrikte yeniden değerlendirileceği)

### Bağlam

Hangi sorun/ihtiyaç; hangi kısıtlar (ekip, ölçek, bütçe, düzenleme). Kanıt: metrikler, olaylar, ölçümler.

### Seçenekler

| Seçenek | Artı | Eksi | Maliyet |
|---|---|---|---|
| A | | | |
| B | | | |

### Karar

Seçilen seçenek ve **neden**. Reddedilen seçeneklerin neden reddedildiği tek cümleyle.

### Sonuçlar

- Olumlu:
- Olumsuz / kabul edilen risk:
- Etkilenen dosyalar / servisler / config yüzeyleri:
- Geri alma yolu:

### Yeniden değerlendirme koşulu

Bu karar şu metrik/olay gerçekleşince yeniden açılır: …

---

## `.agents/skills/proj-api-contract-review/SKILL.md`

---
name: proj-api-contract-review
description: Use this skill when reviewing API contracts — DTO placement, api/core dependency direction, backward compatibility for mobile/panel/internal callers, HTTP parameter binding, OpenAPI diff and versioning.
---

Contract değişikliğini mimari referans Bölüm 3.3, 5.3–5.4, 6 ve 20'ye göre incele. OpenAPI diff çıktısı varsa önce onu oku. Sorun yoksa: **"Bu kapsamda contract bulgusu yok."**

Kontrol et:

### Yerleşim ve bağımlılık yönü
- Servisler arası DTO/enum/event payload'ı hedefin `<domain>-api` modülünde (`com.<org>.<domain>.api.*`); core'da kopyası yok.
- `*-api` hiçbir `*-core`'a bağımlı değil; `*-core` başka `*-core`'a bağımlı değil (enforcer).
- Entity dışarı açılmıyor; `toResponse` ile DTO.
- Servisler arası okunan DTO'da `@NoArgsConstructor` (Jackson).
- Event payload'ı `api/event` altında; CloudEvents attribute'ları platform'dan.

### Geriye uyumluluk
- Alan silme/yeniden adlandırma/tip değişimi/zorunlu alan ekleme = **breaking**. Etkilenen çağıranlar: mobil (mağazadaki eski sürüm aylarca yaşar), panel, internal client'lar, event tüketicileri.
- Breaking ise: yeni versiyon (`/v2` veya yeni `type`), eski versiyon sunset tarihiyle yaşıyor, ADR var.
- Enum'a yeni değer: tüketiciler bilinmeyen değeri tolere ediyor (`from()` fallback); **tüketici önce deploy**.
- Yanıt zarfı tutarlı (hata her zaman zarflı; başarı için tek karar); 200 ile `success=false` yok.
- Hata kodları servisin bloğunda ve global tekil; istemci kod→mesaj tablosu güncellendi (contract testi).
- Sayfalama: boyut sınırı, sıralama allowlist, keyset alanları tutarlı.

### HTTP parameter binding
- `@PathVariable("ad")`, `@RequestParam("ad")`, `@RequestHeader("Ad")` **açık isimli**; parameter-name inference'a güvenilmiyor.
- `required`/`defaultValue` bilinçli; opsiyonel parametre `Optional` veya default.
- Binding gerçek MVC üzerinden test edilmiş (`MockMvc standaloneSetup`; geçersiz tip 400 ve servise ulaşmıyor).
- Idempotent uçlar `X-Idempotency-Key` alıyor; UUID tipiyle bağlanmış.
- Kimlik `@CurrentAccount`; path/body'de hesap kimliği yok.

### Path ve isim
- Kaynak çoğul kebab-case; fiiller yalnız durum geçişi alt kaynaklarında (`/{id}/cancel`); güncelleme `PUT/PATCH`; versiyon prefix'i var.
- Internal uç `/internal/<kaynak-çoğul>/...`; public/internal karışmıyor.

### OpenAPI ve istemci
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

## `.agents/skills/proj-architecture-boundary-review/SKILL.md`

---
name: proj-architecture-boundary-review
description: Use this skill before writing code that touches another module's data, adds a cross-service dependency, a new HTTP client, a read-model, or changes api/core dependency direction — and when reviewing such changes.
---

Modül sınırı ihlallerini ve bağımlılık yönünü `AGENTS.md` Bölüm 4–5, `docs/ai/operation-consistency.md` ve mimari referans Bölüm 1.1/3.3/4.6/10.1'e göre incele. Bu skill **kod yazılmadan önce** de çalıştırılır: ihtiyaç bir sınırı aşıyorsa alternatif önerilir ve kullanıcıya sorulur. Sorun yoksa: **"Bu kapsamda sınır bulgusu yok."**

Kontrol et:

### Veri sahipliği
- Başka modülün tablosu, şeması, repository'si, entity'si, migration'ı okunuyor/yazılıyor mu? (native SQL'de başka şema adı, JPQL'de başka modül entity'si, `@Table(schema=…)` uyuşmazlığı). Yönetim servisi de dahil. **Varsa `BLOCK`.**
- Cross-schema FK/join var mı?
- DB rolü hatası "GRANT" ile mi çözülmüş? (**`BLOCK`**)
- Başka servisin verisi gerekiyorsa hangi yol seçilmiş: (a) hedefin public/internal API'si (yalnız yazma/rezervasyon türü, sıcak yol dışı okuma), (b) event ile read-model, (c) JWT claim. Sıcak yolda (a) ile okuma → `REQUEST CHANGES`, (b)/(c) öner.

### Bağımlılık yönü
- `*-api` → `*-core` bağımlılığı yok; `*-core` → başka `*-core` yok; `platform-*` → servis modülü yok.
- Yeni Maven bağımlılığı bu kuralları ihlal etmiyor (enforcer `bannedDependencies` geçiyor).
- Split package yok (api kökü `com.<org>.<domain>.api`).
- Hedef servis başına tek client; client çağıranın `client/` paketinde; DTO hedefin api'sinden.

### Read-model kuralları
- Read-model tüketicinin kendi şemasında; `revision` ile UPSERT; eskime eşiği ve "satır yok" davranışı yazılı; dışa açılmıyor; rebuild yolu var.
- Read-model'den **yazma** kararı (hak/stok) veriliyor mu? Bu yasak; saga/rezervasyon gerekir.

### Paylaşılan altyapı sözleşmeleri
- Redis key formatı başka serviste kopyalanmış mı? `RedisKeys` (platform-core) veya API/event'e çevir.
- RabbitMQ queue/exchange adı başka servisin sahasına giriyor mu (kendi queue'sunu tanımlıyor mu)?
- Parametre okuması yalnız kendi `SystemParameterProvider`'ı üzerinden mi?

### Mimari şekil
- Değişiklik "dağıtık monolit" sinyali üretiyor mu (yeni senkron zincir, ortak kütüphaneye servis-özel kod, birlikte deploy zorunluluğu)? ADR gerekiyor mu?
- Yeni servis/modül ekleniyorsa Bölüm 1.1 kararıyla tutarlı mı (modül olarak mı, servis olarak mı)?

Çıktı:
1. **Sınır risk seviyesi:** `BLOCKER` / `HIGH` / `MEDIUM` / `LOW` / `OK`.
2. **İhlaller:** `dosya:satır/fonksiyon · hangi kural · kanıt`.
3. **Alternatif:** her ihlal için sınır içinde kalan tasarım (API / event + read-model / claim / saga) ve maliyeti.
4. **Kod yazmadan önce sorulacak sorular** (belirsizlik varsa).
5. **Nihai karar:** `APPROVE` / `REQUEST CHANGES` / `BLOCK` — sınır ihlali `APPROVE WITH COMMENTS` alamaz.

---

## `.agents/skills/proj-client-integration-doc/SKILL.md`

---
name: proj-client-integration-doc
description: Use this skill after backend changes to decide whether a client (mobile/web) is affected and, if so, to produce the versioned integration document from code and OpenAPI — never from memory.
---

İstemci etkisini değerlendir ve gerekiyorsa `docs/<client>-<feature>-integration-vN.md` üret. Kapsam: push edilmemiş her değişiklik. Gerçekler **koddan ve üretilen OpenAPI'den** toplanır; hatırlanan/varsayılan bilgi yazılmaz. Endpoint ve alan listesi OpenAPI'den gelir; doküman **davranış, ekran akışı, hata kodu → ekran** eşlemesine odaklanır.

### 1. Etki gate'i (biri evetse doküman gerekir)
- Yeni/değişen public endpoint, path, method, versiyon?
- Request/response alanı, enum değeri, validation kuralı, sayfalama biçimi değişti mi?
- Yeni hata kodu / değişen status?
- Davranış değişti mi (rate limit, idempotency, sıralama, gizlilik)?
- Realtime (WebSocket topic/zarf), push payload'ı, deep link?
- Auth akışı (token TTL, `sv`, refresh, 2FA, attestation)?
- Kullanıcıya görünür veri (yeni alan gösterimi, gizleme)?

Hiçbiri evet değilse çıktı: **"İstemci etkisi yok."** + gerekçe (hangi dosyalar incelendi).

### 2. Gerçekleri topla
- OpenAPI diff (CI artifact'ı) — breaking işaretleri.
- Controller imzaları, DTO'lar (`*-api`), `ErrorCode` enum'ları, validation anotasyonları.
- Gateway route/permitAll, rate-limit scope'ları (429 davranışı), idempotency header'ları.
- Yanıt zarfı biçimi (zarflı/ham) — uç bazında.
- Realtime/push: topic adları, zarf alanları, `seq`/history pull.

### 3. Dokümanı üret (`template.md`)
- Sürüm: mevcut `vN` üzerine yazılmaz; `v(N+1)` açılır; "Önceki sürüme göre farklar" bölümü doldurulur.
- Her senaryo: HTTP status + body örneği (zarflı mı ham mı açık), alan eşleme, istemci dilinde örnek (`dio`/`fetch`), **bilinmeyen enum değeri için fallback**.
- Hata kodları ve ekran davranışı tablosu (kod → mesaj → ekran aksiyonu → retry?).
- Güvenlik ve log kuralları (istemci ne loglamaz, token nerede tutulur).
- Manuel test akışı: uygulama + API koleksiyonu (OpenAPI'den üretilmiş).
- "Kritik" etiketi yalnız veri kaybı / güvenlik / ücret / bozuk akış için.
- Kaynak damgası: "<tarih> tarihli <branch> <sha> koduna dayanır."

### 4. Öz-kontrol
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

## `.agents/skills/proj-client-integration-doc/template.md`

## <Client> <özellik> entegrasyonu [vN]

<2–4 cümle giriş: bu özellik ne yapar, istemci için ne değişir.> Bu doküman **<GG Ay YYYY>** tarihli `<branch>` branch'indeki `<sha>` backend koduna ve CI'da üretilen `<proje>-api.yaml` (`<sürüm>`) OpenAPI çıktısına dayanır. Generated client paketi: `<paket>@<sürüm>`.

### Değişikliklerin özeti
- …
- …

### Kurallar
> **Kritik:** yalnız veri kaybı / güvenlik / ücret / bozuk akış için kullanılır.
- **Kritik:** …
- …

### Değişiklikler ve <client>'a etkisi
#### 1. <değişiklik>
Ne değişti · neden · istemci ne yapmalı · geriye uyumluluk (eski sürüm ne görür).

### Kullanılan endpoint'ler
| Method | Path | Auth | Idempotency | Rate limit (429) | Zarf |
|---|---|---|---|---|---|
| POST | `/v1/...` | user JWT | `X-Idempotency-Key` | `<scope>` | ham / zarflı |

### API sözleşmesi
#### <işlem>
##### Senaryo: <başarı>
`HTTP 201` — body (ham/zarflı):
```json
{ ... }
```
##### Senaryo: <hata>
`HTTP 409` — zarflı:
```json
{ "ok": false, "data": null, "error": { "code": 11002, "message": "...", "service": "order", "traceId": "...", "details": ["retryAfterSeconds=120"] } }
```
##### Alan eşleme
| Alan | Tip | Zorunlu | Not (bilinmeyen enum → fallback) |
|---|---|---|---|
##### <Dil> örneği
```dart
// dio / generated client kullanımı; ApiException; unknown enum fallback
```

### Realtime / push (varsa)
Topic/zarf alanları, `seq` ve history pull, push payload'ı (içerik yok).

### Hata kodları ve ekran davranışı
| code | Anlam | Ekran aksiyonu | Retry? |
|---|---|---|---|

### Güvenlik ve log kuralları
Token yalnız bellekte; loglanmayacaklar; attestation gereken uçlar.

### Önceki sürüme göre farklar (v(N-1) → vN)
- …

### Manuel test akışı
#### Uygulama üzerinden
1. …
#### API koleksiyonu ile
1. …

### Deploy ve uyumluluk
Backend deploy sırası; eski istemci sürümü davranışı; sunset tarihi (varsa).

### Açık sorular
- …

---

## `.agents/skills/proj-db-migration-review/SKILL.md`

---
name: proj-db-migration-review
description: Use this skill when reviewing PostgreSQL schema changes, Flyway migrations, indexes, constraints, partitioning, retention, seed data, module ownership or production migration risk.
---

`db/migration/` değişikliklerini mimari referans Bölüm 10 ve `docs/ai/security-rules.md` Bölüm 7'ye göre incele. Her bulgu için **çalıştırılabilir doğrulama SQL'i** ver. Sorun yoksa: **"Bu kapsamda migration bulgusu yok."**

Önce çalıştır: `node scripts/flyway-immutability.js check --base origin/<hedef>` — çıkış 0 değilse `BLOCKER`.

Kontrol et:

### Flyway güvenliği
- Base'teki `V*.sql` değişmemiş/silinmemiş/yeniden adlandırılmamış. Düzeltme yeni `V<sonraki>` ile.
- Sürüm sıralı; `out-of-order`, `V9999`, "temp" adlı migration yok.
- `R__*` yalnız idempotent referans verisi/view; iş verisi veya parola içermiyor.
- Seed/test verisi prod location'ında değil (`db/seed-<env>`, yalnız local/test profili).
- Migration **migration rolüyle** (`svc_<x>_migrate`, şema sahibi) koşuyor; uygulama **ayrı rolle** (`svc_<x>`, yalnız DML) çalışıyor; `ALTER DEFAULT PRIVILEGES` uygulama rolüne yeni tablolarda DML veriyor; uygulama rolüne DDL/`OWNER` verilmemiş. `GRANT` başka şemaya erişim açmıyor (varsa `BLOCKER`). Kesin GRANT listesi gözlenen ihtiyaca dayalı ("her ihtimale karşı" yok).
- `spring.flyway.baseline-on-migrate: true` config'te kalıcı olarak yok (varsa `HIGH`); mevcut DB'yi Flyway'e alma tek seferlik belgelenmiş `baseline` adımı.

### Modül sahipliği
- Dosyada yalnız kendi şeması; tam nitelikli adlar; başka şema adı geçmiyor (test de bunu kontrol eder).
- Cross-schema FK yok; başka servisin kimliği düz UUID.
- Read-model tablosu tüketicinin kendi şemasında; **kaynak başına** ayrı tablo ve `source_revision`; tek `revision` kolonlu birleşik tablo yok; `rm_consumer_position` var. Inbox tablosu `(handler, event_id)` PK.

### Mevcut veri ve expand/contract
- Yeni `NOT NULL`, unique, FK öncesi mevcut veri kontrolü SQL'i verilmiş (`SELECT count(*) … WHERE … IS NULL`, duplicate sorgusu).
- Büyük tabloda: `NOT VALID` + ayrı `VALIDATE`; `CREATE INDEX CONCURRENTLY` (transaction dışı migration, Flyway `executeInTransaction=false`); DDL / backfill / validate ayrı dosyalar.
- Yıkıcı değişiklik expand → backfill → contract; guard `DO $$ … RAISE EXCEPTION`; kolon silme uygulama deploy'undan **sonraki** release'te.
- Tablo rewrite'ı tetikleyen değişiklik (tip değişimi, default'lu NOT NULL eski PG'de) işaretlenmiş; lock süresi tahmini var.

### Tip ve constraint
- Zaman `TIMESTAMPTZ`; enum `TEXT + CHECK`; id UUID (v7, DB default `uuidv7()` PG18); isimli `uq_/ck_/fk_/idx_`.
- Eşzamanlılık kuralı DB'de: unique / partial unique (`WHERE status = 'ACTIVE'`); repository sorgusu aynı predicate'i kullanıyor.
- Soft delete'te unique kural partial index ile.
- `ON DELETE CASCADE` bilinçli; audit, ödeme, yasal kayıt, moderasyon kanıtı cascade ile silinmiyor.
- Append-only tablolar trigger ile korunuyor; audit `REVOKE UPDATE, DELETE`.
- Blob `BYTEA` (+ `STORAGE EXTERNAL` şifreli veri için); base64 `TEXT` yok.

### Index gerekçesi
- Her index gerçek bir sorguya dayanıyor (yorumla yazılmış); duplicate/prefix-redundant index yok.
- Poll edilen tablolarda claim index'i `(status, next_retry_at, locked_until, created_at)`.
- Keyset export index'i deterministik tie-breaker ile `(owner, created_at DESC, id DESC)`.
- Append-only büyük tabloda `created_at` BRIN değerlendirilmiş.

### Büyüme ve retention
- Sürekli büyüyen tablo (outbox, log, audit, olay, mesaj) için retention politikası ve gerekirse partition (pg_partman) tanımlı; `DELETE` ile retention yerine `DROP PARTITION`.
- Tahmini satır/boyut büyümesi ve eşik (referans Bölüm 24) yorumda.

### Privacy
- Kişisel veri kolonu: HMAC+pepper/şifreleme kararı; envanter güncel; silme saga'sı kapsamına alınmış.
- Kişisel veri veya gerçek kullanıcı verisi migration'da yok.

### Dinamik parametre migration'ı
- Yeni `system_parameter` satırı: kolon sırası registry/testin beklediği gibi; `data_type`, `criticality`, `usage_status` doğru; enum sabiti api modülüne eklenmiş.

### Entity uyumu
- `ddl-auto: validate` ile uyumlu (kolon adı/tip/nullable); JPQL doğrulama testi geçiyor.

Çıktı:
1. **Risk:** `BLOCKER` / `HIGH` / `MEDIUM` / `LOW` / `OK`.
2. **İncelenen kapsam:** dosya listesi + immutability komut çıktısı.
3. **Bulgular:** `severity · dosya:satır · kanıt · etki (lock, veri kaybı, sahiplik, privacy) · düzeltme`.
4. **Doğrulama SQL'leri:** production'da migration öncesi çalıştırılacak sorgular.
5. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`.

---

## `.agents/skills/proj-environment-impact-review/SKILL.md`

---
name: proj-environment-impact-review
description: Use this skill when a change adds or modifies configuration, environment variables, secrets, ports, audiences, issuers, rate-limit scopes, internal endpoints, feature flags, service URLs, tracing/observability settings, Dockerfile or deploy definitions.
---

Config/env etkisini `AGENTS.md` Bölüm 9 ve mimari referans Bölüm 15, 18, 8'e göre incele. Amaç: bir yüzeyde eklenip diğerinde unutulan key (drift) ve deploy'da patlayan ayar. Sorun yoksa: **"Bu kapsamda environment bulgusu yok."**

Kontrol et:

### Birlikte güncellenmesi gereken yüzeyler
Yeni/değişen her key için tabloyu doldur:

| Key | `config/<svc>.yml` | `application-local.yml` | deploy `env_file` / `secrets/<env>.enc.yaml` | compose `secrets:` | Dockerfile (yalnız build/runtime) | Gateway route | Prometheus/alert | Not |
|---|---|---|---|---|---|---|---|---|

- Eksik hücre = bulgu. `scripts/config-drift-check` / `ConfigDriftTest` bu key'i kapsıyor mu?
- Key adı üç ortamda **aynı**; profil dosyaları yalnız ortam farkı taşıyor, iş config'i base'de.
- Dockerfile'a config için `ENV` **eklenmemiş**.
- Secret: `${ENV:literal}` fallback yok; `/run/secrets` yolu; SOPS dosyasına eklenmiş; `.dockerignore` kapsıyor; PR'da değeri yok. Config Server'a konmamış.
- Admin'in değiştirebileceği iş kuralı config'e değil parametre kataloğuna gitmiş (Bölüm 14).

### Servisler arası
- Yeni internal uç: hedefin `service-jwt.internal-access` kuralı dar ve catch-all'dan önce; local + deploy'da birlikte.
- Yeni client: `spring.http.serviceclient.<grup>.base-url` + timeout + circuit breaker/bulkhead config'i her ortamda; `services.<svc>.base-url` compose servis adıyla uyumlu.
- Yeni audience/issuer: JWKS'e public key + `kid`; doğrulayanların `iss` listesi; gateway route `metadata.audience`.
- Yeni rate-limit scope: `rate-limit.rules.<scope>` local + deploy; fail politikası tablosuna satır.
- Yeni event/queue: RabbitMQ definitions (queue/DLQ/binding) local + deploy; tüketici önce deploy sırası deploy notunda.

### Çalışma zamanı ve replica
- Değişiklik instance-local dosya/dizin varsayıyor mu? Sticky session gerektiriyor mu? (multi-instance kuralı)
- Yeni `@Scheduled`/cron dağıtık kilitli mi?
- Bellek/CPU etkisi: compose `mem_limit`, JVM `MaxRAMPercentage`, Hikari havuzu, PgBouncer pool'u güncellendi mi; kapasite tablosu (README) uyumlu mu?
- Healthcheck/readiness yeni bağımlılığı (DB, broker, Redis) kapsıyor mu?

### Gözlemlenebilirlik
- Yeni servis/uç: Prometheus scrape hedefi, Alloy log kaynağı, SLO/pano, alarm kuralı (özellikle yeni outbox/queue için `outbox_oldest_pending_age_seconds`, DLQ derinliği).
- Tracing: sampling, OTLP endpoint, yeni async sınırda `traceparent` taşınıyor.
- Yeni log alanı structured JSON'a uyuyor; label kardinalitesi artmıyor.

### Deploy
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

## `.agents/skills/proj-event-design-review/SKILL.md`

---
name: proj-event-design-review
description: Use this skill when adding or changing a domain event, a command, an outbox handler, a consumer, a read-model projection, an event schema, or RabbitMQ topology (exchange/queue/binding/DLQ).
---

Olay tasarımını `docs/ai/operation-consistency.md` Bölüm 3–4 ve mimari referans Bölüm 12'ye göre incele. Temel sorular: **Bu bir komut mu, olay mı? Sahibi kim? Yarın üçüncü bir tüketici geldiğinde üretici değişmeden çalışır mı?** Sorun yoksa: **"Bu kapsamda event tasarım bulgusu yok."**

Kontrol et:

### Komut / olay ayrımı
- "Başka servisin verisini değiştir" niyetli mesaj **komut değil olay** olmalı; üretici tüketiciyi bilmiyor. Outbox `kind` doğru (`EVENT`/`COMMAND`/`HTTP`).
- Komut yalnız dış sağlayıcı iş emri (SMS, push, mail, webhook).
- "Outbox üzerinden RPC" (üretici hedefin ucunu/adresini biliyor) → `REQUEST CHANGES`.

### İsim ve envelope
- `type` = `<servis>.<aggregate>.<olay>` (geçmiş zaman: `created`, `cancelled`, `changed`); routing key aynı.
- CloudEvents attribute'ları: `id` (UUIDv7), `source`, `specversion`, `type`, `subject` (aggregate id), `time`, `dataschema`, `traceparent`. AMQP 0-9-1 header eşlemesi `platform-messaging`'den.
- Payload sınıfı `<domain>-api/event`; `@NoArgsConstructor`; kopya yok. Payload **gerçeği** taşır (id'ler, durum, revision), tüketiciye "ne yapması gerektiğini" değil.
- Payload'da PII/secret/şifreli içerik yok (mesaj olayı yalnız metadata).
- `revision`/sıra numarası var ve **kapsamı** yazılı (aggregate başına / kaynak geneli); kaynaklar arası karşılaştırılmıyor.
- **Olay sözleşmesi** yazılı: **tam durum** (snapshot; küçük revizyon atlanabilir) mi **değişiklik** (delta; hiçbir olay atlanamaz, sıra boşluğunda uygulama durur + uzlaştırma + alarm) mi. Delta olayı için boşluk tespiti (`source_seq` monoton) ve rebuild/`since` yolu var.

### Şema evrimi
- Değişiklik geriye uyumlu mu (yalnız opsiyonel alan ekleme)? Kırıcıysa yeni `type`/versiyon + çift yayın planı + sunset.
- Tüketici bilinmeyen alanı yok sayıyor; bilinmeyen `type` olaylarda **ack + log** (komutlarda DLQ).
- **Rollout sözleşmesi** (referans Bölüm 18.4) değişiklik türüne göre PR'da: opsiyonel alan (sıra serbest) / yeni `type` (tüketici önce + çift yayın + sunset) / yeni tüketici (kuyruk+binding önce, rebuild). Kırıcıysa 4 hücreli uyumluluk matrisi (yeni→eski, eski→yeni, yeni→yeni, eski→eski); "çalışmaz" hücresi çift yayın/flag ile kapatılmış. Image rollback'in veriyi geri almadığı not edilmiş.

### Topoloji (RabbitMQ 4.x)
- Olay: `domain.events` topic exchange; tüketici başına queue (`<tüketici>.<amaç>.queue`, quorum) + DLQ; binding pattern dar (`order.order.*`, `#` yok).
- Komut: `<servis>.commands` direct → `<hedef>.<komut>.queue` + DLQ.
- Queue quorum; `delivery-limit`; `dead-letter-strategy: at-least-once`; gecikmeli retry native (`x-delayed-retry-*`), delayed-exchange plugin **yok**.
- Sıra gereksinimi varsa single-active-consumer veya aggregate bazlı tek worker.
- Replay gerekiyorsa stream kopyası (`domain.events.stream`) ve retention.

### Üretici
- Yayın yalnız outbox'tan (domain TX'i içinde satır); `convertAndSend` doğrudan yok.
- Publisher confirm + mandatory; NACK/unroutable → outbox retry.
- Aynı olay birden çok tabloya/outbox'a yazılmıyor (tek satır, çok tüketici).
- **Üretici tarafı sıralama:** sıra gereken aggregate için claim aynı aggregate'i tek worker'a sırayla veriyor; başarısız satırın ardılları bekletiliyor. Sıra gerekmiyorsa bu kısıt yok (throughput).
- **Lane izolasyonu:** olay lane'i komut/HTTP lane'inden ayrı claim döngüsü ve havuzda; toplu yayın (kampanya) kritik tek satırları bekletmiyor; `priority` doğru.
- Publisher confirm "tüketici işledi" sayılmıyor; tamamlanma bilgisi gerekiyorsa tüketicinin olayı tüketiliyor.
- Eski bir karar (superseded) yeniden denemede yeni kararı ezemiyor.

### Tüketici ve read-model
- `defaultRequeueRejected=false`; prefetch/concurrency açık; kalıcı hata → DLQ; geçici → stateful retry + backoff.
- **Inbox atomikliği (zorunlu güvence):** `inbox_event(handler, event_id)` satırı ve iş değişikliği **aynı TX'te**; 0 satır → çık; ack **commit'ten sonra** (manual ack; AUTO ack `BLOCK`). Dedup kapsamı handler. Dış yan etki inbox TX'inde değil, aynı TX'te outbox satırı.
- Read-model: tüketicinin şemasında; **kaynak başına** projeksiyon ve `source_revision` (tek `revision` kolonlu birleşik tablo `REQUEST CHANGES`); `rm_consumer_position` ile tazelik (satır yaşından değil); karar başına kabul edilen eskilik T ve aşılınca davranış (fail-closed varsayılan) yazılı; "satır yok" davranışı yazılı; dışa açılmıyor; rebuild yolu (stream replay / export ucu) belgeli; replay deterministik; `readmodel_lag_seconds{source}` ve `readmodel_gap_total` metrikleri.
- Tüketici kendi transaction'ında yazıyor; başka servise senkron çağrı yapmıyor.

### Gözlem ve test
- Metrik/alarm: DLQ derinliği, `outbox_oldest_pending_age_seconds`, tüketici lag.
- Testler: outbox satırı TX ile rollback; tüketici duplicate (tek etki); handler ortasında exception → inbox satırı yok; sıra bozuk olay; delta'da sıra boşluğu → dur + alarm; bilinmeyen tip; şema uyumluluğu (eski payload yeni tüketicide, yeni payload eski tüketicide); iki poller + sıralı satırlar; lane izolasyonu. Her `PASS` için kanıt kaydı (commit, komut, sonuç — `operation-consistency.md` Bölüm 9).
- Analytics sink bu olayı alıyor mu (Bölüm 14.4)?

Çıktı:
1. **Sınıflandırma:** komut / olay / HTTP — doğru mu.
2. **Olay tablosu:** `type · üretici · tüketiciler · routing key · queue · DLQ · replay (evet/hayır) · revision alanı`.
3. **Bulgular:** `severity · dosya:satır/config · kanıt · düzeltme`.
4. **Şema evrimi ve rollout notu:** uyumlu / kırıcı + plan; değişiklik türüne göre sıra (Bölüm 18.4) ve kırıcıysa uyumluluk matrisi.
5. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`.

---

## `.agents/skills/proj-operation-consistency-review/SKILL.md`

---
name: proj-operation-consistency-review
description: Use this skill for any cross-service write, outbox, inbox, idempotency or saga work — to assess whether a saga is needed, to implement it correctly, and to verify it against the mandatory scenario matrix.
---

Servisler arası tutarlılığı `docs/ai/operation-consistency.md` (tek kaynak) ve mimari referans Bölüm 11'e göre üç fazda ele al. Kapsam: push edilmemiş her değişiklik. Mevcut altyapı sınırı: `LocalSagaStore` **tek adımlı, tek katılımcı**; merkezi coordinator yok. Çok adımlı ihtiyaç → `extension required` + ADR.

### Faz 1 — Assessment (`references/assessment.md`)
- İhtiyacı sınıflandır: yalnız okuma / tek local TX / duplicate koruması / commit sonrası tepki (event) / dış iş emri (komut) / local commit + uzak geri alınabilir mutation (saga) / geri alınamaz-global-insan onayı (saga uygun değil).
- "En basit yeterli mekanizma" seçilmiş mi? Saga, event yeterliyken kullanılıyorsa fazla; event, saga gerekirken kullanılıyorsa eksik.
- Sıcak yol etkisi: saga consume çağrısı kritik akış kaydında gerekçeli mi; bütçe içinde mi (Bölüm 1.2)?
- Karar: `saga unnecessary` / `existing saga suitable` / `extension required` / `decision blocked` (+ neden).

### Faz 2 — Implementation (`references/implementation.md`, 12 adım)
- Idempotency key: `X-Idempotency-Key` UUID, `(account_id, scope, operation_key)` tekilliği, replay/IN_PROGRESS/CANCELLED davranışı.
- `begin()` ayrı TX; consume TX dışında ve circuit breaker altında; domain + `success()` aynı TX'te ayrı bean; compare-and-set.
- Katılımcı: `UNIQUE(caller_service, account_id, operation_key)`, tombstone, advisory lock + `FOR UPDATE`, aktör→işlem tipi haritası, iade kuralları, `original_id` ile çift iade engeli.
- Recovery worker: claim (SKIP LOCKED + `lock_token` + lease), prepare (`FOR UPDATE`), confirm/compensate, complete (token), belirsizlikte GET, çelişkide `MANUAL_REVIEW`; monitor + cleanup; `MANUAL_REVIEW` silinmez.
- Outbox kullanımı: `outbox_event` (generic), `kind` doğru, yazıcı MANDATORY, handler idempotent, DEAD politikası iş türüne göre, hassas alan iletim sonrası NULL.
- Inbox/read-model: inbox satırı `(handler, event_id)` + iş **aynı TX**, ack commit sonrası; kaynak başına `source_revision`; olay sözleşmesi (tam durum/değişiklik); bilinmeyen `type` yok sayılır.
- Outbox lane izolasyonu ve üretici tarafı sıralama; `claim_token` ≠ uzak idempotency; publisher confirm ≠ işlendi.
- Loglar: `sagaId`/`operationKey` ile, hesap kimliği yok; metrikler: `saga_unresolved_total`, `outbox_oldest_pending_age_seconds`.
- Config: `operation-consistency.*` key'leri local + deploy.

### Faz 3 — Verification (`references/verification.md`)
- Yapısal kontroller (statik) + senaryo matrisi (operation-consistency.md Bölüm 6) + kanıt seviyesi (1 unit/MVC, 2 gerçek PG, 3 owner→participant runtime, 4 release).
- Her senaryo için: test adı / dosya / kanıt seviyesi / **commit SHA / ortam / sonuç linki / tarih** (kanıt kaydı, `operation-consistency.md` Bölüm 9). Testi olmayan senaryo `FAIL` değil `BLOCKED` (kanıt yok) sayılır ve listelenir. Yapısal kontrol (`ArchUnit` yeşil) davranışsal senaryo için kanıt değildir.

Çıktı:
1. **Uygunluk kararı:** `saga unnecessary` / `existing saga suitable` / `extension required` / `decision blocked` + gerekçe.
2. **Implementasyon bulguları:** `adım · dosya:satır · kanıt · düzeltme`.
3. **Doğrulama tablosu:** senaryo → test → kanıt seviyesi → `PASS/FAIL/BLOCKED`.
4. **Doğrulama durumu:** `PASS` / `FAIL` / `BLOCKED`.
5. **Nihai karar:** `APPROVE` / `REQUEST CHANGES` / `BLOCK`.

---

## `.agents/skills/proj-operation-consistency-review/references/assessment.md`

## Assessment — Mekanizma Seçimi

### 1. Soru ağacı

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
   - Evet → kritik akış kaydı güncellenir (bütçe, bağımlılık gerekçesi, eskilik); varsayılan ≤1 uzak çağrı aşılıyorsa ADR; consume dışındaki kontroller read-model/claim ile.
5. **Dış sağlayıcıya iş emri mi (SMS, push, webhook)?**
   - Evet → **komut** (outbox `kind=COMMAND` → queue) veya `kind=HTTP`.

### 2. Karar tablosu

| Durum | Mekanizma | Örnek |
|---|---|---|
| Kendi tablomu yazıyorum | TX + unique | Profil güncelleme |
| Aynı isteğin tekrarını engellemek | Idempotency key | Sipariş oluşturma retry'ı |
| Başkası haberdar olsun | Event + inbox | `order.order.created` → notification, analytics |
| Başkasının verisini karar için okumam lazım | Read-model / JWT claim | Hesap durumu, engel listesi |
| Başkasından geri alınabilir bir hak/stok tüketmem lazım | Local saga | Kampanya hakkı, envanter rezervi |
| Geri alınamaz dış etki | Süreç + insan/dış onay, ADR | Ödeme çekimi, resmi bildirim |

### 3. Çıktı formatı

- Karar: `saga unnecessary` / `existing saga suitable` / `extension required` / `decision blocked`
- Gerekçe (2–4 cümle), seçilen mekanizma, sıcak yol etkisi (uzak çağrı sayısı), açık sorular.

---

## `.agents/skills/proj-operation-consistency-review/references/implementation.md`

## Implementation — 12 Adım (Local Saga + Outbox)

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

## `.agents/skills/proj-operation-consistency-review/references/verification.md`

## Verification — Yapısal Kontroller ve Senaryo Matrisi

### 1. Yapısal kontroller (statik, kod okuyarak)

- [ ] `begin()` ayrı TX'te; consume TX dışında; domain + `success()` aynı TX'te ayrı bean.
- [ ] `success()` compare-and-set; başarısızlıkta exception ve rollback.
- [ ] Recovery `complete` yalnız `lock_token` eşleşince yazıyor.
- [ ] Katılımcıda advisory lock + `FOR UPDATE`; tombstone; `UNIQUE(caller_service, account_id, operation_key)`.
- [ ] Outbox yazıcı MANDATORY; handler idempotent; inbox dedup.
- [ ] `MANUAL_REVIEW` cleanup'ta silinmiyor; monitor metriği var.
- [ ] Loglarda hesap kimliği yok; `sagaId`/`operationKey` var.
- [ ] Config key'leri local + deploy.

### 2. Senaryo matrisi

| # | Senaryo | Beklenen | Kanıt seviyesi | Test · commit · ortam · tarih | Sonuç (link) |
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
| 25 | Handler ortasında exception (inbox atomikliği) | inbox satırı yok; yeniden teslimde iş yapılır | 2 | | |
| 26 | Commit sonrası, ack öncesi çökme | yeniden teslim duplicate olarak yutulur, tek etki | 3 | | |
| 27 | İki poller instance + aynı aggregate'in sıralı iki satırı | tek worker, sıra korunur; ilk satır başarısızsa ikincisi beklet | 2 | | |
| 28 | Bir lane'de takılı hedef (HTTP 30 sn timeout) | EVENT lane'i gecikmeden yayınlıyor | 2 | | |
| 29 | Eski güvenlik kararı yeniden denemede | yeni kararı ezmiyor (superseded + `source_revision`) | 2 | | |
| 30 | Publisher confirm var, tüketici işlemedi | üretici "tamamlandı" saymıyor; tamamlanma tüketici olayıyla | 3 | | |
| 31 | Delta olayında sıra boşluğu | uygulama durur, `readmodel_gap_total` artar, uzlaştırma | 2 | | |
| 32 | Süreç öldürme: PUBLISHING satır + kira dolumu | ikinci instance devralır; ilk instance geri gelince yazamaz | 3 | | |

Kanıt seviyeleri: 1 unit/MVC · 2 gerçek PostgreSQL (Testcontainers veya gömülü PG) · 3 owner→participant runtime (iki servis ayakta) · 4 release/staging.

Seviye 2 referans uygulaması: `skeleton-example/platform-messaging` (`SagaBehaviourIT` satır 1–20, `OutboxBehaviourIT` satır 21–32); senaryo → test adı eşlemesi test metotlarının `// #n` yorumlarında.

### 3. Sonuç

- Tüm satırlar `PASS` → **PASS**.
- Herhangi bir satır `FAIL` → **FAIL** (liste).
- Test/kanıt olmayan satır → **BLOCKED** (liste; "test yok" = geçmiş sayılmaz).
- `PASS` yazılan her satırda commit SHA ve sonuç linki dolu; boşsa `BLOCKED`. Yapısal testler (ArchUnit) bu tablo için kanıt değildir.

---

## `.agents/skills/proj-release-readiness-review/SKILL.md`

---
name: proj-release-readiness-review
description: Use this skill on release PRs (to release/main), before the first production deploy of a new service, and quarterly — to verify backups, alerting, SLOs, runbooks, capacity, version/EOL status, security posture and rollback readiness.
---

Sürümün üretime çıkmaya hazır olup olmadığını mimari referans Bölüm 8, 10.5, 18, 21.0, 24 ve 25'e göre değerlendir. Bu skill kod kalitesine değil **operasyonel gerçeklere** bakar: yedek var mı, alarm gidiyor mu, geri dönüş kaç dakika. Her madde için kanıt (dosya, config, pano linki, tarih) istenir; kanıtsız madde `BLOCKED`. Sorun yoksa: **"Release hazırlık kontrolü geçti."**

Kontrol et:

### Veri ve yedek (yoksa `FAIL`)
- WAL arşivi + base backup çalışıyor (son başarılı yedek tarihi); retention; şifreli.
- **Restore provası** son 30 gün içinde yapılmış ve kayıtlı (süre, doğrulama).
- RPO/RTO README'de; HA durumu (managed/standby/yok) açıkça yazılı ve kabul edilmiş.
- Redis security instance AOF; RabbitMQ definitions yedeği; object storage versioning.
- Bu release'in migration'ları prod benzeri veri hacminde denenmiş (süre, lock).

### Alarm ve gözlem (yoksa `FAIL`)
- Alertmanager/Grafana alerting bir kanala **gerçekten** bildirim gönderiyor (test alarmı tarihi).
- Asgari alarm seti (referans 8.7): restart-loop, health DOWN, disk/RAM, WAL arşiv gecikmesi, Redis bellek/eviction, RabbitMQ queue/DLQ, `outbox_oldest_pending_age_seconds`, `*_STUCK`, SLO burn-rate.
- Yeni servis/uç için scrape hedefi, log kaynağı, pano.
- Structured log; trace sampling politikası; Loki label kardinalitesi.

### SLO ve kapasite
- Kritik akışlar için SLO tanımlı ve pano var; son yük testi tarihi ve sonucu (p99, hata oranı) SLO içinde.
- Kapasite tablosu: host RAM/CPU kullanımı < %70; ikinci instance/host planı; DB bağlantı bütçesi; Bölüm 24 eşiklerinden yaklaşılan var mı.

### Deploy ve geri dönüş
- Image CI'da build, registry'de, cosign imzalı, digest ile deploy; prod'da build yok.
- **Rollout sözleşmesi** (referans Bölüm 18.4) release notunda değişiklik türü bazında: her olay/uç/şema/enum/claim değişikliği için sıra ve birlikte çalışacak sürümler; kırıcı değişiklikler için 4 hücreli uyumluluk matrisi; "çalışmaz" hücresi kabul edilmişse `FAIL`.
- Rollback: önceki digest listesi mevcut; migration expand/contract'a uygun (eski image yeni şemayla çalışır); prova edilmiş rollback süresi. **Image rollback ≠ veri rollback:** yeni sürümün yazdığı veri/olay/read-model ve dış komutlar geri alınmaz; geri alınamaz etkiler flag arkasında; staging'de "N+1 → yaz → N → oku" provası kayıtlı.
- CI test sayısı kontrolü var (mimari/tutarlılık testleri 0 test ile yeşil olamaz).
- Healthcheck ve readiness her serviste; `restart: always`; `docker-rollout`/blue-green.
- Staging'de smoke test geçti; prod deploy onay kapısı var.

### Güvenlik duruşu
- Secret'lar `/run/secrets`; literal fallback yok; gitleaks CI temiz; `.dockerignore`.
- Asimetrik service JWT; JWKS/`kid`; rotasyon prosedürü.
- Mock entegrasyon bean'i prod profilinde yok (açılış guard'ı testi).
- Seed/test verisi prod location'ında yok; bilinen parolalı admin yok.
- Gateway: rate limit, timeout, CORS listesi, `gateway` actuator kapalı, trusted proxy.
- Bağımlılık taraması (Dependabot/OSV) açık CRITICAL/HIGH yok veya kabul edilmiş risk yazılı.

### Sürüm ve destek (Bölüm 25)
- Java, Spring Boot, Spring Cloud, PostgreSQL, Redis/Valkey, RabbitMQ, arama motoru, Alloy/Loki/Tempo/Prometheus/Grafana, Node: **hepsi OSS destek içinde**; bitişe < 3 ay kalan için upgrade PR/plan var.
- Yaması yalnız ticari sürümde olan bilinen CVE yok.
- Lisans değişikliği (Redis, Elastic, BSL, GPL) gözden geçirilmiş.

### Uyum ve ürün
- Hesap silme akışı çalışıyor (uygulama içi + saga); export ucu; KVKK/GDPR süreleri ölçülüyor.
- Kişisel veri envanteri ve DPIA güncel; yeni alanlar eklendi mi.
- İstemci entegrasyon dokümanı ve OpenAPI/generated client release ile uyumlu; API sunset tarihleri.
- Runbook'lar: her alarm için "ne yapılır" sayfası; on-call kim; olay sonrası postmortem şablonu.

### Doğrulama kapsamı ve kanıt (referans Bölüm 19.6)
- Release notu yapısal (ArchUnit/enforcer/drift) ile davranışsal (outbox tekrar teslimi, iki worker, restart, inbox atomikliği, saga recovery) doğrulamayı ayrı listeliyor; davranışsal senaryoların kanıt kaydı (senaryo · seviye · test · commit · ortam · sonuç · tarih) dolu. Kaydı olmayan senaryo `BLOCKED`.
- Yeni servisin ilk prod deploy'unda en az: outbox tekrar teslimi, iki worker, süreç öldürme/kira devri seviye 2/3'te `PASS`.

### Dokümantasyon
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

## `.agents/skills/proj-resilience-review/SKILL.md`

---
name: proj-resilience-review
description: Use this skill when a change adds or modifies a remote synchronous call, a hot-path flow, timeouts, retries, circuit breakers, bulkheads, thread/connection pools, caches with fallback, or any control-plane dependency (parameters, config, flags).
---

Dayanıklılığı mimari referans Bölüm 1.2, 4.6, 4.7, 7.2/14.3 ve `AGENTS.md` Bölüm 5'e göre incele. Temel soru: **"Bu bağımlılık yavaşlar veya düşerse kullanıcı ne görür, servis ne yapar?"** Her uzak bağımlılık için bu cevap yazılı olmalı. Sorun yoksa: **"Bu kapsamda dayanıklılık bulgusu yok."**

Kontrol et:

### Sıcak yol (kritik akış kaydı)
- Değişen akış sıcak yolda mı (`docs/ai/repo-context.md` Bölüm 3)? Kaydın **tüm alanları** dolu mu: gecikme bütçesi (p99), uzak senkron bağımlılıklar ve **her birinin gerekçesi**, karar başına kabul edilen veri eskiliği, bağımlılık düşünce davranış, yeniden değerlendirme ölçümü. Boş alan → `REQUEST CHANGES`.
- Varsayılan tercih ≤1 uzak senkron çağrı, yazma/rezervasyon türünden. Aşılıyorsa: gerekçe + ADR var mı, toplam p99 bütçe içinde mi, alternatif (read-model / JWT claim / asenkron) ve **maliyeti** (replikasyon gecikmesi, rebuild, işletim) yazılmış mı? Gerekçeli ve bütçeli ikinci çağrı `APPROVE WITH NON-BLOCKING COMMENTS` olabilir; gerekçesiz olan `REQUEST CHANGES`.
- Okuma amaçlı senkron çağrı: read-model ile karşılanabiliyorsa ve eskilik toleransı buna izin veriyorsa neden eklendi? Eskilik toleransı sıfırsa (ör. bakiye) senkron kabul edilebilir — yazılı olsun.
- Availability aritmetiği: bağımlı bileşen sayısı × %99,9 → beklenen üst sınır; p99 toplamı hedefle karşılaştırılmış.

### Timeout bütçesi
- Gateway toplam bütçesi > client read timeout > downstream'in kendi downstream timeout'u (bütçe zinciri tutarlı).
- Her client grubunda `connect-timeout` ve `read-timeout` açıkça set; varsayılan/sonsuz timeout yok.
- Uzun işlemler (export, toplu işlem) senkron uçta değil; job + poll.

### Circuit breaker / bulkhead / retry
- Hedef başına `resilience4j.circuitbreaker.instances.<hedef>` ve `bulkhead` tanımlı; açıkken tanımlı `ServiceException` (503 `UPSTREAM_UNAVAILABLE`) ve fallback davranışı belgeli.
- Spring Cloud CircuitBreaker kullanılıyorsa **TimeLimiter (varsayılan 1 sn)** ve thread-pool bulkhead ayarlanmış/kapatılmış.
- Senkron yolda retry **yok** (`NEVER_RETRY`); retry yalnız outbox/saga worker'ında, backoff ile, idempotent hedefe.
- Cascading failure hesabı: Tomcat/virtual thread modeli, Hikari havuzu, downstream timeout × istek hızı → havuz doluyor mu?

### Fail politikası
- Yeni bağımlılık için fail-open/fail-closed kararı yazılı ve README tablosuyla uyumlu (güvenlik → closed; iş → open + metrik).
- Control-plane bağımlılığı (parametre, config, flag): bounded-staleness (son bilinen değer + disk snapshot + `*_staleness_seconds` metriği + T eşiği); "5 sn cache + 503" **yok**.
- Redis güvenlik state'i ile cache ayrı instance; eviction politikası doğru.
- Read-model "satır yok/eski" davranışı tanımlı; tazelik tüketim konumundan (`readmodel_lag_seconds{source}`) ölçülüyor, satır yaşından değil; karar başına T farklı olabilir (engel kararı ≠ profil görseli).

### Kapasite ve kaynak
- Yeni thread pool/executor sınırlı ve isimli; virtual thread'lerle pinning riski (`synchronized` + IO) yok.
- Hikari `maximum-pool-size` × instance ≤ PgBouncer/`max_connections` bütçesi.
- Bellek: yeni cache'in üst sınırı ve TTL'i var.
- WebSocket/uzun bağlantı sayısı pod başına sınırlı; presence/fan-out Redis'te.

### Gözlem ve test
- Yeni bağımlılık için metrik (`circuitbreaker_state`, latency histogram) ve alarm.
- "Hedef 30 sn yanıt vermiyor" testi: çağıranın p99 bütçeyi aşmıyor, circuit açılıyor, health `DEGRADED`.
- Yük testi senaryosu güncellendi mi (sıcak yol değiştiyse).
- Runbook: bağımlılık düştüğünde ne yapılır (Bölüm 8.8).

Çıktı:
1. **Bağımlılık tablosu:** her uzak çağrı → sıcak yol mu · timeout · CB/bulkhead · fail politikası · düşünce kullanıcı ne görür.
2. **Bulgular:** `severity · dosya:satır/config key · kanıt · düzeltme`.
3. **Kritik akış kaydı:** önce/sonra (bağımlılık sayısı, bütçe); kayıt güncellendi mi; varsayılan aşıldıysa ADR linki.
4. **Eksik testler/alarmlar.**
5. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`. Kaydı eksik veya gerekçesiz ek senkron bağımlılık → en az `REQUEST CHANGES`.

---

## `.agents/skills/proj-security-review/SKILL.md`

---
name: proj-security-review
description: Use this skill when reviewing authentication, authorization, service JWT, input validation, sensitive logging, uploads, rate limiting, DB queries, WebSocket, privacy, secrets or supply-chain aspects of a change.
---

Değişikliği `docs/ai/security-rules.md` (tek kaynak) ve OWASP ASVS/API Security Top 10 ile karşılaştır. Her bulgu için **exploit senaryosu** yaz; senaryo yazamıyorsan bulgu değil, öneridir. Kanıt yoksa `needs verification`. Sorun yoksa: **"Bu kapsamda güvenlik bulgusu yok."**

Kontrol et:

### Trust boundary ve kimlik
- Üç JWT yüzeyi karışmıyor: `typ`, anahtar ve `aud` ayrı. Service JWT asimetrik; `iss` = imzalayan; `kid`/JWKS ile doğrulama; simetrik paylaşılan secret **yok** (varsa `BLOCK`).
- Filtre sırası: path decode+normalize → imza/iss/aud/exp → (opsiyonel jti) → allowlist first-match → default-deny. Allowlist ham URI ile eşleşmiyor.
- `internal-access` kuralı gerçek kullanım kadar dar; dar kural catch-all'dan önce; local yml ve deploy config **birlikte** güncellenmiş.
- Kullanıcı adına internal uç `sub` == path hesabı kontrolü yapıyor; hesabı body'den alan internal uç hiçbir aktöre açık değil.
- **Delegasyon** (`repo-context.md` Bölüm 3.1): yeni/değişen internal uç matriste satır aldı mı; hedef **üçünü birlikte** kontrol ediyor mu (çağıran allowlist'te + bu işlem için + `sub` bu kaynakta yetkili); arka plan token'ıyla (sub yok) kullanıcı-yetkisi gerektiren işlem reddediliyor mu; "her kullanıcı adına her şey" satırı var mı (varsa `BLOCK`); zincirde `act` korunuyor mu. Testler: izinli/izinsiz aktör + yanlış `sub` + arka plan token'ıyla kullanıcı işlemi.
- Gateway: `/internal` engeli `StripPrefix` sonrası da; iç header temizliği; CORS `*` yok; token query'de yok; `gateway` actuator ucu kapalı; trusted-proxy ayarı; rate limit ve timeout bütçesi.

### Ownership / IDOR
- Hesap kimliği yalnız `@CurrentAccount`; path/query/body'den değil. Path'teki id hedef kaynak; ownership serviste doğrulanıyor.
- Liste/sayfalama uçlarında sıralama alanı allowlist; boyut sınırı.
- Read-model'den yetki kararı veriliyorsa "satır yok / eski" davranışı fail-closed; eskilik toleransı (T) bu karar için ayrıca yazılı (engel kararı ≤ 30 sn gibi) ve eski bir güvenlik kararı yeniden denemede yenisini ezemiyor.

### OTP, token, abuse
- OTP: `SecureRandom`, tuzlu hash, sabit zamanlı karşılaştırma, TTL + deneme sınırı, enumeration koruması; outbox payload'ında düz metin OTP kalmıyor.
- SMS pumping savunması `otp/start` için: geo-permissions, numara doğrulama, numara/IP/cihaz tavanı, velocity, harcama kesicisi.
- Refresh token: opak + hash, aile rotasyonu, reuse → aile iptali, mutlak süre.
- `sv` artışı: şifre sıfırlama, logout-all, **ban**, rol değişimi.
- Admin 2FA passkey/TOTP; SMS yalnız kurtarma. Admin OTP'si kullanıcı OTP'sinden zayıf değil.
- Rate limit: yeni saldırıya açık uç scope aldı; sayaç Redis'te; fail politikası scope tablosuyla uyumlu; `Retry-After`.
- Mock entegrasyon (`MockStoreValidation`, `MockSms`…) `@Profile("local|test")` ve prod açılış guard'ı var (yoksa `BLOCK`).
- Cihaz attestation sinyali gereken uçlarda (kayıt, OTP-gönder, satın alma) risk skoruna bağlanmış.

### Input ve sorgu güvenliği
- `@Valid`, boyut sınırları, enum parse (`from`), path/header/param adları açık.
- Native SQL'de parametre bağlama; dinamik sıralama/filtre allowlist; JPQL string birleştirme yok.
- Dosya adı/path kullanıcıdan gelmiyor; `..` normalize.
- JSON: bilinmeyen alan politikası bilinçli; polimorfik deserialization kapalı.

### WebSocket / realtime
- CONNECT'te native `Authorization` zorunlu; süresi dolmuş token ile komut reddi; SUBSCRIBE deny-all allowlist; `sv` süpürücüsü; Redis zarfı HMAC + tazelik + `relayId`; local'de bile imzasız zarf kabul edilmiyor.

### Object storage ve içerik
- Yükleme private quarantine → yeniden kodlama (EXIF/GPS) → moderasyon (CSAM hash + NSFW) → private delivery + signed GET. Public bucket'a doğrudan yükleme `BLOCK`.
- Presigned URL kısa TTL; key sunucuda üretilmiş; HEAD/ETag doğrulaması.
- Uçtan uca şifreleme varsa: iddia ile şema uyumlu (FS/MITM sınırları yazılı), anahtar pinleme, message franking; sohbet anahtarı panele verilmiyor.

### Log, hata, secret
- `security-rules.md` Bölüm 4 yasak listesi: hiçbir ham PII/secret/body/exception mesajı log'da yok. Log'a özel neden `safeLogReason`'da; `details`'te yalnız istemciye gösterilebilir bilgi.
- Yeni secret: değeri hiçbir yerde yok; `/run/secrets` ile geliyor; `${ENV:literal}` fallback yok; SOPS dosyasına eklenmiş; `.dockerignore` kapsıyor.
- Hata yanıtları tek format (`ErrorResponse`); filtre redleri de aynı; altyapı hatası 503 (500 değil).
- Audit: `details` allowlist; anahtar/şifreli içerik/serbest metin/ham IP yok; tablo değiştirilemez.

### Privacy
- Yeni kişisel veri alanı: envanter + aydınlatma + retention + silme saga'sı kapsamı güncellendi.
- Düşük entropili kimlik için HMAC+pepper; varlık oracle'ı yok; konum deterministik grid; kişisel veri dönen uç `no-store`.
- Arama index'i/cache/yedek aynı kurallara uyuyor.

### Tedarik zinciri
- Yeni bağımlılık: lisans (GPL/AGPL/BSL) ve bilinen CVE kontrolü; sürüm OSS destekli.
- Dockerfile non-root, `.dockerignore`, image CI'da build.

Çıktı:
1. **Risk seviyesi:** `CRITICAL` / `HIGH` / `MEDIUM` / `LOW` / `OK`.
2. **Bulgular** (her biri): `severity · dosya:satır · kanıt · exploit senaryosu (kim, nasıl, sonuç) · düzeltme · doğrulayan test`.
3. **Kural referansı:** her bulgu için `security-rules.md` bölümü.
4. **Net kanıt bulunamayan alanlar.**
5. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`. `CRITICAL` veya `HIGH` bulgu varsa karar `APPROVE` olamaz.

---

## `.agents/skills/proj-spring-code-review/SKILL.md`

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

### Transaction ve Sıcak Yol
- Uzak HTTP çağrısı `@Transactional` metot, row lock veya advisory lock **içinde değil**. Kanonik biçim: `ServiceImpl` (tx yok) → guard'lar → `*TransactionService` (tx) → `AFTER_COMMIT` yan etkileri → outbox.
- Outbox yazıcı `Propagation.MANDATORY`; audit/log kaydı `REQUIRES_NEW` yalnız gerekçeliyse.
- `noRollbackFor` kullanımı gerekçeli (deneme sayacı, token iptali).
- Sıcak yolda (kullanıcıya latency yansıyan akış) **okuma amaçlı** uzak çağrı eklenmiş mi? Eklendiyse `REQUEST CHANGES`: read-model veya JWT claim öner; `docs/ai/repo-context.md` sıcak yol tablosuna bak. Yazma türü tek çağrı kabul; ikincisi `proj-resilience-review` ister.
- Her client çağrısı timeout + circuit breaker + bulkhead altında mı (grup config'i var mı)?

### Multi-Instance Safety
- `@Scheduled`/poller/worker tek instance varsaymıyor: `FOR UPDATE SKIP LOCKED` + lease + `claim_token`; sonuç yazımı claim sahibine kısıtlı; lease güvenlik payı.
- Check-then-act (`existsBy` → `save`) yerine unique/partial unique, koşullu update veya lock.
- JVM-local state yok (rate limit, cache, "tek aktif kayıt", WebSocket üyeliği). İzinli JVM cache: kısa TTL + bayatlık toleransı + yorumla gerekçe.
- Bilinçli single-instance davranış yorumla belgelenmiş.

### Exception ve Log
- Throw/rethrow öncesi tek structured log: `"<Olay> rejected: code=X reason=Y"`; kullanıcı kaynaklı 4xx WARN, altyapı/beklenmeyen ERROR.
- Ham `e.getMessage()`, body, token, PII loglanmıyor (`docs/ai/security-rules.md` Bölüm 4). Log'a özel neden `safeLogReason`'da, `details`'te değil.
- Normal Flow Logging: akış INFO'dan izlenebilir (`operation=`, `outcome=`); her metot loglanmıyor; batch'ler özetleniyor; boş poll turu loglanmıyor; commit görülmeden "success" yazılmıyor.
- Tek log stili (`"<Olay>: key=value"`); `[TAG]`/snake_case karışımı yok.
- `ErrorCode` yeni sabiti servisin bloğunda ve global tekil (test geçiyor mu?).

### Tracing ve Async Context
- Outbox satırında `traceparent/tracestate`; poller span'i bu bağlamdan başlıyor; MQ header'larına inject; span attribute'larında PII yok.
- `@Async`/executor kullanımında context propagation korunuyor.

### Sistem Parametreleri
- Admin'in değiştirebileceği iş kuralı değeri config/env/`@Value` ile **değil**, `SystemParameterProvider` ile okunuyor; key yalnız enum.
- Fail politikası: key yok / değer bozuk → fail-closed; kaynak erişilemez → bounded-staleness (grup `criticality`'sine göre) — kod içi default/yml fallback **yok**.
- Tazelik doğru seçilmiş: kullanıcı girdisini doğrulayan yazma `freshGroup`; kalıcı sonuçta revizyon snapshot'ı; birlikte anlamlı key'ler aynı revizyondan; TX/lock dışında okuma; worker her turda yeniden okuyor.
- Değer düşürülünce mevcut veriyi uzlaştıran worker var mı?
- Yeni key için: enum + migration + registry kuralı + `usage_status` + tutarlılık testleri.

### Entity ve Repository
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

## `.agents/skills/proj-test-writer/SKILL.md`

---
name: proj-test-writer
description: Use this skill when writing or reviewing unit, HTTP binding, security access, repository/concurrency, contract, resilience, log-privacy or regression tests for a change.
---

Testleri mimari referans Bölüm 16 ve `docs/ai/security-rules.md` Bölüm 4'e göre yaz/incele. Önce **davranış ve edge case listesi**, sonra test. Anlamsız test (yalnız mock'un çağrıldığını doğrulayan) yazılmaz. Çıktıda eklenen case'ler, kalan boşluklar ve çalıştırma komutları listelenir.

Kontrol et / yaz:

### Kapsam kuralları
- Negatif case'ler ve **yetki hataları** zorunlu (izinsiz aktör 403, kimliksiz 400/401, başka kullanıcının kaynağı 404/403).
- Her bug için regression testi.
- Poller, worker ve check-then-act kodu için **concurrency** testi: iki paralel claim ayrık satır; eşzamanlı ikinci insert reddedilir; expired lease'te eski worker complete edemez (gerçek PG).
- Kırılgan test yok: sıra, saat (`Instant now()` metodu üzerinden verilir), rastgelelik, dış servis bağımlılığı yok.
- Test isimlendirmesi tek stil (`metot_whenKoşul_beklenenSonuç`); yorumlar ekibin dilinde.

### Türler
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

### Komutlar
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

## `.claude/settings.json`

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

## `.claude/hooks/flyway-immutability.js`

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

## `.claude/hooks/review-gate.sh`

```bash
#!/usr/bin/env bash
# Claude Code PreToolUse hook (Bash): `git push` oncesi bir review skill'i BU agac uzerinde ve son 1 saatte calismis mi?
# Calismadiysa veya agacin icerigi damgadan sonra degistiyse kullaniciya sorar (permissionDecision: "ask").
# Push disi komutlarda sessizce gecer. Damga kanit degildir; zorunlu guvence CI'dir (referans Bolum 19.6).
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
REASON="Son 1 saatte review skill'i (proj-*-review / proj-test-writer) calistirilmadi."

if [[ -f "$STAMP" ]]; then
  read -r LAST TREE_AT _ < "$STAMP" || true
  LAST="${LAST:-0}"; TREE_AT="${TREE_AT:-}"
  if [[ "$LAST" =~ ^[0-9]+$ ]] && (( NOW - LAST < MAX_AGE )); then
    # shellcheck disable=SC1091
    . "$ROOT/.claude/hooks/tree-state.sh"
    TREE_NOW="$(tree_state "$ROOT")"
    if [[ -n "$TREE_AT" && "$TREE_AT" == "$TREE_NOW" ]]; then
      exit 0
    fi
    # Eski bicim damga (yalniz epoch) veya icerik degismis
    REASON="Review skill'i calisti ama calisma agacinin icerigi o zamandan beri degisti; review damgasi bu icerik icin gecersiz."
  fi
fi

node -e 'const r=process.argv[1];process.stdout.write(JSON.stringify({hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"ask",permissionDecisionReason:r+" docs/ai/review-checklist.md\x27deki skill\x27leri calistirmadan push etmek istiyor musun?"}}))' "$REASON"
exit 0
```

---

## `.claude/hooks/review-stamp.sh`

```bash
#!/usr/bin/env bash
# Claude Code PostToolUse hook (Skill): review skill'i calistiysa damga yaz.
# Damga calisma agacinin icerigine baglidir: "<epoch> <tree hash>". Icerik degisince damga gecersizdir
# (review-gate.sh karsilastirir); commit atmak icerigi degistirmedigi icin damgayi bozmaz. Damga KANIT DEGILDIR; yalnizca "bir review skill'i bu agac uzerinde calisti" der.
# Zorunlu guvence CI'dir (referans Bolum 19.6).
set -euo pipefail
INPUT="$(cat)"
NAME="$(printf '%s' "$INPUT" | node -e 'let s="";process.stdin.on("data",c=>s+=c).on("end",()=>{try{const j=JSON.parse(s);process.stdout.write(String((j.tool_input&&(j.tool_input.skill||j.tool_input.name))||""))}catch(e){process.stdout.write("")}})')"
case "$NAME" in
  *-review|*test-writer|*integration-doc)
    ROOT="${CLAUDE_PROJECT_DIR:-$PWD}"
    mkdir -p "$ROOT/.claude"
    # shellcheck disable=SC1091
    . "$ROOT/.claude/hooks/tree-state.sh"
    printf '%s %s\n' "$(date +%s)" "$(tree_state "$ROOT")" > "$ROOT/.claude/.last-review-check"
    ;;
esac
exit 0
```

---

## `.claude/hooks/tree-state.sh`

```bash
#!/usr/bin/env bash
# review-stamp.sh ve review-gate.sh'in ortak parcasi: calisma agacinin ICERIK kimligi.
# Gecici bir index'e tum (ignore edilmeyen) dosyalar eklenir ve `git write-tree` ile agac hash'i alinir.
# HEAD'den bagimsizdir: review sonrasi commit atmak damgayi bozmaz, icerik degistirmek bozar.
# Damga dosyasinin kendisi haric tutulur. Git yoksa "nogit".
tree_state() {
  local root="$1"
  if ! git -C "$root" rev-parse --git-dir >/dev/null 2>&1; then
    printf 'nogit'; return 0
  fi
  local idx
  idx="$(mktemp)"
  rm -f "$idx"
  GIT_INDEX_FILE="$idx" git -C "$root" add -A -- . ':(exclude).claude/.last-review-check' >/dev/null 2>&1 || true
  GIT_INDEX_FILE="$idx" git -C "$root" write-tree 2>/dev/null || printf 'unknown'
  rm -f "$idx"
}
```

---

## `scripts/flyway-immutability.js`

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

## `scripts/flyway-immutability.test.js`

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

## `tests/ArchitectureRulesTest.java`

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

## `tests/ErrorCodeUniquenessTest.java`

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

## `tests/ConfigDriftTest.java`

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

## `skeleton-example/.gitignore`

```
target/
```

---

## `skeleton-example/README.md`

## skeleton-example — Doğrulanmış Boş İskelet

`blueprint/tests/*.java` şablonlarının, enforcer kuralının ve **generic outbox/inbox'ın** gerçekten derlenip çalıştığı en küçük Maven multi-module projesi. Referans dokümanın (Bölüm 3, 4, 7, 11.2–11.3, 16, 19.5–19.6, 23.3–23.4) somut, çalışan karşılığı.

- Spring Boot **4.1.1** BOM, Java 21 (25 ile de uyumlu), ArchUnit 1.5.1, Maven 3.9.11.
- Modüller: `platform-core` (ErrorCode arayüzü, ServiceException), `platform-messaging` (generic outbox/inbox: `OutboxRepository`, `OutboxPoller`, `InboxProcessor`, `db/platform/outbox_inbox.sql`; local saga: `LocalSagaStore`, `SagaRecoveryWorker`, `SagaParticipant`, `db/platform/saga_coordinator.sql`), `order-api` (DTO), `order-core` (controller/service/impl/repository/entity/exception/config + yapısal testler).
- Config: `application-local.yml`, `config/order.yml`, `deploy/prod.env.example` (drift testi için).

### Doğrulama sonucu (2026-09-29)

```
mvn -B -ntp test   → BUILD SUCCESS
platform-messaging  OutboxBehaviourIT      13 test  (davranışsal, gerçek PostgreSQL 17.5 — gömülü, Docker gerekmez)
                    SagaBehaviourIT        19 test  (davranışsal, saga senaryoları 1–20; aynı gömülü PG)
order-core          ArchitectureRulesTest   8 test  (katman, controller→repository, impl paketi, config/, core→core, döngü, @Valid, api→entity)
                    ConfigDriftTest         3 test  (key kümeleri, ${ENV} ↔ env şablonu, secret fallback)
                    ErrorCodeUniquenessTest 1 test  (global tekillik, blok, mesaj formatı)
```

#### Yapısal (seviye: kural derlenir, ihlal yakalanır)

**Negatif doğrulama:** kasıtlı 8 ihlal enjekte edildi (controller→repository, `@Valid`'siz `@RequestBody`, `service/` altında `@Configuration`, `service.impl`'de Impl olmayan sınıf, çakışan + blok dışı + noktasız ErrorCode, local'de olup deploy'da olmayan rate-limit scope'u, `${SECRET_DB_PASSWORD:changeme}` fallback'i) → **8 failure**, hepsi doğru kuralda yakalandı; kaldırılınca yeniden yeşil.

#### Davranışsal (seviye 2: gerçek PostgreSQL) — `OutboxBehaviourIT`

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

#### Davranışsal (seviye 2: gerçek PostgreSQL) — `SagaBehaviourIT`

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

### Denemede öğrenilen dersler (şablonlara işlendi)

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

### Çalıştırma

```bash
cd blueprint/skeleton-example
mvn -B -ntp test                     # yapısal + davranışsal; gömülü PG binary'si Maven Central'dan gelir (io.zonky.test)
## root kullanıcıdaysan (initdb root'u reddeder):
runuser -u <non-root-user> --preserve-environment -- mvn -B -ntp -Dmaven.repo.local=$HOME/.m2/repository test
```

---

## `skeleton-example/deploy/prod.env.example`

```bash
# deploy env sablonu — degerler CI secret'larindan gelir
DB_HOST=
DB_PORT=5432
DB_NAME=app
INVENTORY_URL=http://inventory:8086
```

---

## `skeleton-example/order-api/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
  <parent><groupId>com.acme</groupId><artifactId>skeleton</artifactId><version>${revision}</version></parent>
  <artifactId>order-api</artifactId>
  <dependencies><dependency><groupId>jakarta.validation</groupId><artifactId>jakarta.validation-api</artifactId></dependency></dependencies>
</project>
```

---

## `skeleton-example/order-api/src/main/java/com/acme/order/api/dto/CreateOrderRequest.java`

```java
package com.acme.order.api.dto;
import jakarta.validation.constraints.NotBlank;
public record CreateOrderRequest(@NotBlank String sku, int quantity) {}
```

---

## `skeleton-example/order-core/pom.xml`

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
  <build><plugins>
    <!-- 0 test = basarisiz: ArchUnit engine uyumsuzlugu gibi sessiz "yesil" durumlarini yakalar (README ders 2) -->
    <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId>
      <configuration><failIfNoTests>true</failIfNoTests></configuration></plugin>
  </plugins></build>
</project>
```

---

## `skeleton-example/order-core/src/main/java/com/acme/order/OrderApp.java`

```java
package com.acme.order;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class OrderApp { public static void main(String[] a) { SpringApplication.run(OrderApp.class, a); } }
```

---

## `skeleton-example/order-core/src/main/java/com/acme/order/config/WebConfig.java`

```java
package com.acme.order.config;
import org.springframework.context.annotation.Configuration;
@Configuration
public class WebConfig {}
```

---

## `skeleton-example/order-core/src/main/java/com/acme/order/controller/OrderController.java`

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

## `skeleton-example/order-core/src/main/java/com/acme/order/entity/Order.java`

```java
package com.acme.order.entity;
import java.util.UUID;
public class Order { private UUID id; private String sku; public UUID getId() { return id; } public String getSku() { return sku; } }
```

---

## `skeleton-example/order-core/src/main/java/com/acme/order/exception/ErrorCode.java`

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

## `skeleton-example/order-core/src/main/java/com/acme/order/repository/OrderRepository.java`

```java
package com.acme.order.repository;
import com.acme.order.entity.Order;
import java.util.Optional; import java.util.UUID;
import org.springframework.stereotype.Repository;
@Repository
public class OrderRepository { public Optional<Order> findById(UUID id) { return Optional.empty(); } }
```

---

## `skeleton-example/order-core/src/main/java/com/acme/order/service/OrderService.java`

```java
package com.acme.order.service;
import com.acme.order.api.dto.CreateOrderRequest;
import java.util.UUID;
public interface OrderService { UUID create(UUID accountId, UUID idempotencyKey, CreateOrderRequest req); void cancel(UUID accountId, UUID orderId); }
```

---

## `skeleton-example/order-core/src/main/java/com/acme/order/service/impl/OrderServiceImpl.java`

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

## `skeleton-example/order-core/src/main/resources/application-local.yml`

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

## `skeleton-example/order-core/src/main/resources/config/order.yml`

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

## `skeleton-example/order-core/src/test/java/com/acme/order/ArchitectureRulesTest.java`

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

## `skeleton-example/order-core/src/test/java/com/acme/order/ConfigDriftTest.java`

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

## `skeleton-example/order-core/src/test/java/com/acme/order/ErrorCodeUniquenessTest.java`

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

## `skeleton-example/platform-core/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
  <parent><groupId>com.acme</groupId><artifactId>skeleton</artifactId><version>${revision}</version></parent>
  <artifactId>platform-core</artifactId>
  <dependencies><dependency><groupId>org.springframework</groupId><artifactId>spring-web</artifactId></dependency></dependencies>
</project>
```

---

## `skeleton-example/platform-core/src/main/java/com/acme/platform/core/ErrorCode.java`

```java
package com.acme.platform.core;
import org.springframework.http.HttpStatus;
public interface ErrorCode { int getCode(); String getMessage(); String getService(); HttpStatus getHttpStatus(); }
```

---

## `skeleton-example/platform-core/src/main/java/com/acme/platform/core/ServiceException.java`

```java
package com.acme.platform.core;
public class ServiceException extends RuntimeException {
    private final ErrorCode errorCode;
    public ServiceException(ErrorCode c) { super(c.getMessage()); this.errorCode = c; }
    public ErrorCode getErrorCode() { return errorCode; }
}
```

---

## `skeleton-example/platform-messaging/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
  <parent><groupId>com.acme</groupId><artifactId>skeleton</artifactId><version>${revision}</version></parent>
  <artifactId>platform-messaging</artifactId>
  <!-- Generic outbox/inbox (referans Bolum 11.2-11.3, 23.3-23.4). Servisler poller/claim sorgusu yazmaz, bu modulu kullanir. -->
  <dependencyManagement><dependencies>
    <!-- Gomulu PostgreSQL: Docker olmayan ortamda gercek PG ile davranissal test (CI'da Testcontainers de kullanilabilir) -->
    <dependency><groupId>io.zonky.test.postgres</groupId><artifactId>embedded-postgres-binaries-bom</artifactId><version>17.5.0</version><type>pom</type><scope>import</scope></dependency>
  </dependencies></dependencyManagement>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-jdbc</artifactId></dependency>
    <dependency><groupId>org.slf4j</groupId><artifactId>slf4j-api</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>test</scope></dependency>
    <dependency><groupId>io.zonky.test</groupId><artifactId>embedded-postgres</artifactId><version>2.1.0</version><scope>test</scope></dependency>
  </dependencies>
  <build><plugins>
    <!-- 0 test = basarisiz: ArchUnit engine uyumsuzlugu gibi sessiz "yesil" durumlarini yakalar (README ders 2) -->
    <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId>
      <configuration><failIfNoTests>true</failIfNoTests></configuration></plugin>
  </plugins></build>
</project>
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/inbox/InboxProcessor.java`

```java
package com.acme.platform.messaging.inbox;

import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Idempotent consumer (referans Bolum 11.3). Zorunlu guvence: inbox satiri ve is degisikligi AYNI transaction'da;
 * satir varsa (duplicate) is yapilmadan cikilir; is exception atarsa satir da geri alinir ve mesaj yeniden gelir.
 * Broker ack bu metodun DONUSUNDEN sonra (commit sonrasi) yapilir; AUTO ack ile bu guvence bozulur.
 * Dedup kapsami handler adidir: ayni olayi iki handler ayri ayri isler.
 */
public class InboxProcessor {

    public enum Outcome { APPLIED, DUPLICATE }

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final String table;

    public InboxProcessor(NamedParameterJdbcTemplate jdbc, TransactionTemplate tx, String schema) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.table = "\"" + schema + "\".inbox_event";
    }

    public Outcome process(String handler, UUID eventId, Runnable work) {
        return tx.execute(status -> {
            int inserted = jdbc.update(
                    "INSERT INTO %s (handler, event_id) VALUES (:h, :e) ON CONFLICT DO NOTHING".formatted(table),
                    Map.of("h", handler, "e", eventId));
            if (inserted == 0) return Outcome.DUPLICATE;      // daha once islendi; hicbir sey yapmadan cik (ack)
            work.run();                                         // is degisikligi ayni TX'te; exception -> rollback (satir dahil)
            return Outcome.APPLIED;
        });
    }
}
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/OutboxEvent.java`

```java
package com.acme.platform.messaging.outbox;

import java.time.Instant;
import java.util.UUID;

/** outbox_event satiri (referans Bolum 11.2). Payload/headers JSON metin olarak tasinir; JPA yok, JDBC. */
public record OutboxEvent(
        UUID id,
        String kind,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload,
        String headers,
        String status,
        int priority,
        DeadPolicy deadPolicy,
        int retryCount,
        Instant nextRetryAt,
        Instant lockedUntil,
        UUID claimToken,
        String lastErrorCode,
        Instant createdAt) {

    public enum DeadPolicy { DEAD_ON_PERMANENT, NEVER_DEAD }

    public boolean isNeverDead() { return deadPolicy == DeadPolicy.NEVER_DEAD; }
}
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/OutboxHandler.java`

```java
package com.acme.platform.messaging.outbox;

/**
 * Bir lane'in (kind) isini yapar: EVENT -> exchange publish + confirm, COMMAND -> queue, HTTP -> client.
 * Transaction DISINDA cagrilir. Hedef idempotent olmak zorundadir: claim_token cift teslimi engellemez (Bolum 11.2).
 * Kalici hata icin {@link PermanentFailureException}; diger her exception gecici sayilir ve backoff ile yeniden denenir.
 */
@FunctionalInterface
public interface OutboxHandler {
    void handle(OutboxEvent event) throws Exception;
}
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/OutboxPoller.java`

```java
package com.acme.platform.messaging.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic outbox poller (referans Bolum 23.4). Her lane (kind) icin ayri dongu: yavas bir HTTP hedefi event yayinini
 * bekletmez. Satirlar SKIP LOCKED + kira ile claim edilir, uzak is transaction disindadir. Hedefler idempotent oldugu
 * icin tekrar islenme zararsizdir; claim_token yalniz poller'in kendi yazma yarisini cozer.
 *
 * Zamanlama (Spring @Scheduled) bu sinifin disinda baglanir; boylece testte poll(kind) dogrudan ve deterministik
 * bir Clock ile cagrilir.
 */
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxRepository repository;
    private final Map<String, OutboxHandler> handlersByKind;
    private final OutboxProperties props;
    private final Clock clock;

    public OutboxPoller(OutboxRepository repository, Map<String, OutboxHandler> handlersByKind,
                        OutboxProperties props, Clock clock) {
        this.repository = repository;
        this.handlersByKind = handlersByKind;
        this.props = props;
        this.clock = clock;
    }

    public record PollResult(int claimed, int applied, int failed, int dead, int deferred) {}

    public PollResult poll(String kind) {
        Instant claimedAt = clock.instant();
        Instant leaseEnd = claimedAt.plusSeconds(props.leaseSeconds());
        UUID claimToken = UUID.randomUUID();
        List<OutboxEvent> entries = repository.claim(kind, claimedAt, leaseEnd, claimToken, props.batchSize());
        if (entries.isEmpty()) return new PollResult(0, 0, 0, 0, 0);          // bos turlar loglanmaz

        OutboxHandler handler = handlersByKind.get(kind);
        if (handler == null) throw new IllegalStateException("No outbox handler for lane " + kind);
        Instant workDeadline = leaseEnd.minusSeconds(props.leaseSafetySeconds());
        int applied = 0, failed = 0, deferred = 0, dead = 0;
        for (OutboxEvent entry : entries) {
            if (clock.instant().isAfter(workDeadline)) { deferred++; continue; }   // kira dolunca baska instance alir
            try {
                handler.handle(entry);                                             // TX disinda
                repository.deleteProcessed(entry.id(), claimToken);
                applied++;
            } catch (PermanentFailureException e) {
                if (entry.isNeverDead()) { markFailure(entry, claimToken, e); failed++; }   // guvenlik yan etkisi DEAD olmaz
                else {
                    repository.release(entry.id(), claimToken, "DEAD", entry.retryCount(), clock.instant(),
                            e.getClass().getSimpleName());
                    dead++;
                }
            } catch (Exception e) {
                markFailure(entry, claimToken, e);
                failed++;
            }
        }
        log.info("Outbox batch finished: lane={} claimed={} applied={} failed={} dead={} deferred={}",
                kind, entries.size(), applied, failed, dead, deferred);
        return new PollResult(entries.size(), applied, failed, dead, deferred);
    }

    private void markFailure(OutboxEvent entry, UUID claimToken, Exception e) {
        int retries = entry.retryCount() + 1;
        String errorType = e.getClass().getSimpleName();                           // exception mesaji degil
        repository.release(entry.id(), claimToken, "PENDING", retries,
                clock.instant().plusSeconds(backoffSeconds(retries, props.maxBackoffSeconds())), errorType);
        if (retries % props.stuckAlertEvery() == 0) {
            log.error("Outbox entry still failing: code=OUTBOX_STUCK lane={} eventId={} retry={} exceptionType={}",
                    entry.kind(), entry.id(), retries, errorType);
        } else {
            log.warn("Outbox entry failed; retry scheduled: lane={} eventId={} retry={} exceptionType={}",
                    entry.kind(), entry.id(), retries, errorType);
        }
    }

    // min(maxBackoff, 30 * 2^n) sn — baslangic ayari
    static long backoffSeconds(int retries, long maxBackoff) {
        return Math.min(maxBackoff, 30L * (1L << Math.min(Math.max(retries, 0), 20)));
    }
}
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/OutboxProperties.java`

```java
package com.acme.platform.messaging.outbox;

/**
 * Poller baslangic ayarlari (referans Bolum 1.4: "baslangic ayari" sinifi; yuk testiyle degisir).
 * batch 50, lease 120 sn, guvenlik payi 30 sn, backoff min(600, 30*2^n), STUCK alarmi her 10 denemede.
 */
public record OutboxProperties(int batchSize, long leaseSeconds, long leaseSafetySeconds,
                               long maxBackoffSeconds, int stuckAlertEvery) {
    public static OutboxProperties defaults() { return new OutboxProperties(50, 120, 30, 600, 10); }
}
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/OutboxRepository.java`

```java
package com.acme.platform.messaging.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generic outbox erisimi (referans Bolum 23.3). Sema adi parametre: her servis kendi semasindaki tabloyu kullanir.
 * JDBC ile yazilmistir; JPA versiyonu ayni sorgulari @Query(nativeQuery=true) ile tasir.
 */
public class OutboxRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final String table;

    public OutboxRepository(NamedParameterJdbcTemplate jdbc, String schema) {
        this.jdbc = jdbc;
        this.table = "\"" + schema + "\".outbox_event";
    }

    /**
     * Domain TX'i icinde cagrilir (Propagation.MANDATORY): outbox satiri domain yazimiyla birlikte commit/rollback olur.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(OutboxEvent e) {
        jdbc.update("""
                INSERT INTO %s (id, kind, aggregate_type, aggregate_id, event_type, payload, headers, priority, dead_policy,
                                next_retry_at, created_at)
                VALUES (:id, :kind, :aggType, :aggId, :eventType, CAST(:payload AS jsonb), CAST(:headers AS jsonb),
                        :priority, :deadPolicy, :nextRetryAt, :createdAt)
                """.formatted(table),
                new MapSqlParameterSource()
                        .addValue("id", e.id()).addValue("kind", e.kind())
                        .addValue("aggType", e.aggregateType()).addValue("aggId", e.aggregateId())
                        .addValue("eventType", e.eventType()).addValue("payload", e.payload())
                        .addValue("headers", e.headers() == null ? "{}" : e.headers())
                        .addValue("priority", e.priority()).addValue("deadPolicy", e.deadPolicy().name())
                        .addValue("nextRetryAt", Timestamp.from(e.nextRetryAt() == null ? e.createdAt() : e.nextRetryAt()))
                        .addValue("createdAt", Timestamp.from(e.createdAt())));
    }

    /**
     * Birden fazla instance ayni satiri alamaz: SKIP LOCKED kilitli satirlari atlar, locked_until kira suresidir.
     * Kirasi dolan PUBLISHING satiri (instance cokmesi) yeniden claim edilir. Lane = kind; oncelik yuksek olan once.
     * Sira: ayni aggregate_id'nin daha eski, henuz bitmemis (PENDING/PUBLISHING) satiri varsa bu satir atlanir
     * (NOT EXISTS) — sira uretici tarafinda korunur; ilk satir basarisiz olursa ardillari kendiliginden bekler.
     */
    public List<OutboxEvent> claim(String kind, Instant now, Instant lockedUntil, UUID claimToken, int limit) {
        return jdbc.query("""
                WITH candidates AS (
                    SELECT o.id FROM %1$s o
                    WHERE o.kind = :kind
                      AND (o.status = 'PENDING' OR (o.status = 'PUBLISHING' AND o.locked_until <= :now))
                      AND o.next_retry_at <= :now
                      AND NOT EXISTS (SELECT 1 FROM %1$s p
                                      WHERE p.aggregate_id = o.aggregate_id
                                        AND (p.created_at, p.id) < (o.created_at, o.id)
                                        AND p.status IN ('PENDING','PUBLISHING'))
                    ORDER BY o.priority DESC, o.created_at, o.id
                    FOR UPDATE OF o SKIP LOCKED
                    LIMIT :limit)
                UPDATE %1$s o
                SET status = 'PUBLISHING', locked_until = :lockedUntil, claim_token = :claimToken
                FROM candidates WHERE o.id = candidates.id
                RETURNING o.*
                """.formatted(table),
                Map.of("kind", kind, "now", Timestamp.from(now), "lockedUntil", Timestamp.from(lockedUntil),
                        "claimToken", claimToken, "limit", limit),
                MAPPER);
    }

    /** Sonucu yalniz claim sahibi yazar; kirasi elinden alinmis eski worker satiri ezemez. */
    public int deleteProcessed(UUID id, UUID claimToken) {
        return jdbc.update("DELETE FROM %s WHERE id = :id AND claim_token = :token".formatted(table),
                Map.of("id", id, "token", claimToken));
    }

    /** PENDING (retry) veya DEAD'e birak; yine yalniz claim sahibi. */
    public int release(UUID id, UUID claimToken, String status, int retryCount, Instant nextRetryAt, String errorCode) {
        return jdbc.update("""
                UPDATE %s SET status = :status, retry_count = :retry, next_retry_at = :next,
                       locked_until = NULL, claim_token = NULL, last_error_code = :err
                WHERE id = :id AND claim_token = :token
                """.formatted(table),
                new MapSqlParameterSource().addValue("status", status).addValue("retry", retryCount)
                        .addValue("next", Timestamp.from(nextRetryAt)).addValue("err", errorCode)
                        .addValue("id", id).addValue("token", claimToken));
    }

    public List<OutboxEvent> findAll() {
        return jdbc.query("SELECT * FROM %s ORDER BY created_at, id".formatted(table), Map.of(), MAPPER);
    }

    static final RowMapper<OutboxEvent> MAPPER = new RowMapper<>() {
        @Override public OutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new OutboxEvent(
                    rs.getObject("id", UUID.class), rs.getString("kind"), rs.getString("aggregate_type"),
                    rs.getObject("aggregate_id", UUID.class), rs.getString("event_type"), rs.getString("payload"),
                    rs.getString("headers"), rs.getString("status"), rs.getShort("priority"),
                    OutboxEvent.DeadPolicy.valueOf(rs.getString("dead_policy")), rs.getInt("retry_count"),
                    instant(rs.getTimestamp("next_retry_at")), instant(rs.getTimestamp("locked_until")),
                    rs.getObject("claim_token", UUID.class), rs.getString("last_error_code"),
                    instant(rs.getTimestamp("created_at")));
        }
        private Instant instant(Timestamp t) { return t == null ? null : t.toInstant(); }
    };
}
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/outbox/PermanentFailureException.java`

```java
package com.acme.platform.messaging.outbox;

/** Yeniden denemenin anlamsiz oldugu hata (kalici 4xx; 401/403/408/429 haric). DEAD karari dead_policy'ye gore verilir. */
public class PermanentFailureException extends RuntimeException {
    public PermanentFailureException(String message) { super(message); }
}
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/saga/LocalSagaStore.java`

```java
package com.acme.platform.messaging.saga;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Local saga store (referans Bolum 11.4). Tek adim, tek katilimci; domain'den bagimsiz (step adi parametre).
 * Zaman kaynagi disaridan verilen Clock (uretimde DB now() ile ayni saat; testte deterministik).
 *
 * Akis: begin() AYRI TX -> consume (TX disi) -> domain yazimi + success() AYNI TX (compare-and-set) ->
 * recovery worker: claim -> prepare -> confirm/compensate -> complete (lock_token eslesmesi).
 */
public class LocalSagaStore {

    public enum SagaStatus { STARTED, SUCCEEDED, CONFIRMED, CANCEL_REQUESTED, COMPENSATED, MANUAL_REVIEW }
    public enum StepStatus { PENDING, RETRY, RUNNING, DONE, MANUAL_REVIEW }
    public enum Action { CONFIRM, COMPENSATE }

    public record Saga(UUID id, UUID accountId, String scope, UUID operationKey, SagaStatus status, String result,
                       Instant createdAt, Instant updatedAt) {
        public boolean isTerminal() {
            return status == SagaStatus.CONFIRMED || status == SagaStatus.COMPENSATED || status == SagaStatus.MANUAL_REVIEW;
        }
    }

    public record Step(UUID id, UUID sagaId, String stepName, Action nextAction, StepStatus status, int attempt,
                       Instant nextAttemptAt, UUID lockToken, Instant lockedUntil, String lastErrorCode) {}

    public record BeginResult(Saga saga, boolean created) {}

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate requiresNew;
    private final String sagaTable;
    private final String stepTable;
    private final SagaProperties props;
    private final Clock clock;

    public LocalSagaStore(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager tm, String schema,
                          SagaProperties props, Clock clock) {
        this.jdbc = jdbc;
        this.requiresNew = new TransactionTemplate(tm);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.sagaTable = "\"" + schema + "\".saga";
        this.stepTable = "\"" + schema + "\".saga_steps";
        this.props = props;
        this.clock = clock;
    }

    // ---------- istek yolu ----------

    /**
     * Ayri TX'te niyet kaydi. Yeni kayitta adim COMPENSATE/PENDING ve next_attempt_at = now + deadline: surec cokerse
     * deadline dolunca otomatik telafi baslar. Kayit zaten varsa mevcut saga (replay/IN_PROGRESS karari cagirana ait).
     */
    public BeginResult begin(UUID accountId, String scope, UUID operationKey, String stepName) {
        Objects.requireNonNull(accountId, "accountId"); Objects.requireNonNull(operationKey, "operationKey");
        if (scope == null || scope.isBlank()) throw new IllegalArgumentException("scope");
        return requiresNew.execute(st -> {
            Instant now = clock.instant();
            UUID id = UUID.randomUUID();
            int inserted = jdbc.update("""
                    INSERT INTO %s (id, account_id, scope, operation_key, status, created_at, updated_at)
                    VALUES (:id, :account, :scope, :key, 'STARTED', :now, :now)
                    ON CONFLICT (account_id, scope, operation_key) DO NOTHING""".formatted(sagaTable),
                    params().addValue("id", id).addValue("account", accountId).addValue("scope", scope)
                            .addValue("key", operationKey).addValue("now", ts(now)));
            if (inserted == 1) {
                jdbc.update("""
                        INSERT INTO %s (id, saga_id, step_name, next_action, status, attempt, next_attempt_at, created_at, updated_at)
                        VALUES (:id, :saga, :name, 'COMPENSATE', 'PENDING', 0, :next, :now, :now)""".formatted(stepTable),
                        params().addValue("id", UUID.randomUUID()).addValue("saga", id).addValue("name", stepName)
                                .addValue("next", ts(now.plusSeconds(props.deadlineSeconds()))).addValue("now", ts(now)));
                return new BeginResult(new Saga(id, accountId, scope, operationKey, SagaStatus.STARTED, null, now, now), true);
            }
            return new BeginResult(find(accountId, scope, operationKey).orElseThrow(), false);
        });
    }

    /**
     * Domain TX'i ICINDE cagrilir (ayni TX; aktif TX yoksa hata). Compare-and-set STARTED -> SUCCEEDED: recovery araya girip
     * iptal ettiyse (CANCEL_REQUESTED) SagaCancelledException -> domain yazimi rollback olur. Adim CONFIRM'e cevrilir ve
     * hemen denenir. RUNNING adima dokunulmaz: recovery prepare() SUCCEEDED'i gorup CONFIRM'e uzlastirir.
     */
    public void success(UUID sagaId, String result) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("success() must run inside the domain transaction");
        }
        Instant now = clock.instant();
        int n = jdbc.update("UPDATE %s SET status = 'SUCCEEDED', result = :r, updated_at = :now WHERE id = :id AND status = 'STARTED'"
                .formatted(sagaTable), params().addValue("r", result).addValue("now", ts(now)).addValue("id", sagaId));
        if (n == 0) throw new SagaCancelledException(sagaId);
        jdbc.update("""
                UPDATE %s SET next_action = 'CONFIRM', status = 'PENDING', next_attempt_at = :now, updated_at = :now
                WHERE saga_id = :id AND status IN ('PENDING','RETRY')""".formatted(stepTable),
                params().addValue("now", ts(now)).addValue("id", sagaId));
    }

    /** Istek yolu basarisiz (REJECTED, domain hatasi): ayri TX'te iptal iste; recovery hemen telafi eder. */
    public void fail(UUID sagaId) {
        requiresNew.executeWithoutResult(st -> {
            Instant now = clock.instant();
            jdbc.update("UPDATE %s SET status = 'CANCEL_REQUESTED', updated_at = :now WHERE id = :id AND status = 'STARTED'"
                    .formatted(sagaTable), params().addValue("now", ts(now)).addValue("id", sagaId));
            jdbc.update("""
                    UPDATE %s SET next_action = 'COMPENSATE', next_attempt_at = :now, updated_at = :now
                    WHERE saga_id = :id AND status IN ('PENDING','RETRY')""".formatted(stepTable),
                    params().addValue("now", ts(now)).addValue("id", sagaId));
        });
    }

    // ---------- recovery worker ----------

    /** SKIP LOCKED + lock_token + lease. Kirasi dolan RUNNING adim (worker cokmesi) yeniden claim edilir. */
    public List<Step> claimSteps(UUID lockToken, Instant now, Instant lockedUntil, int limit) {
        return jdbc.query("""
                WITH c AS (
                    SELECT s.id FROM %1$s s
                    WHERE (s.status IN ('PENDING','RETRY') AND s.next_attempt_at <= :now)
                       OR (s.status = 'RUNNING' AND s.locked_until <= :now)
                    ORDER BY s.next_attempt_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT :limit)
                UPDATE %1$s s SET status = 'RUNNING', lock_token = :token, locked_until = :until, updated_at = :now
                FROM c WHERE s.id = c.id
                RETURNING s.*""".formatted(stepTable),
                params().addValue("now", ts(now)).addValue("limit", limit).addValue("token", lockToken)
                        .addValue("until", ts(lockedUntil)), STEP);
    }

    /**
     * Sahiplik + durum uzlastirmasi (FOR UPDATE). STARTED saga'yi CANCEL_REQUESTED'a ceker (istek yolu success() CAS'i
     * bunu gorur ve rollback olur). SUCCEEDED -> CONFIRM, CANCEL_REQUESTED -> COMPENSATE. Terminal saga -> adim DONE.
     * Kira elden gitmisse (token eslesmez) bos doner.
     */
    public Optional<Action> prepare(Step step, UUID lockToken) {
        return requiresNew.execute(st -> {
            Instant now = clock.instant();
            Saga saga = DataAccessUtils.singleResult(jdbc.query("SELECT * FROM %s WHERE id = :id FOR UPDATE".formatted(sagaTable),
                    Map.of("id", step.sagaId()), SAGA));
            Step owned = DataAccessUtils.singleResult(jdbc.query(
                    "SELECT * FROM %s WHERE id = :id AND lock_token = :token AND status = 'RUNNING'".formatted(stepTable),
                    params().addValue("id", step.id()).addValue("token", lockToken), STEP));
            if (saga == null || owned == null) return Optional.<Action>empty();
            Action action;
            switch (saga.status()) {
                case STARTED -> {
                    jdbc.update("UPDATE %s SET status = 'CANCEL_REQUESTED', updated_at = :now WHERE id = :id".formatted(sagaTable),
                            params().addValue("now", ts(now)).addValue("id", saga.id()));
                    action = Action.COMPENSATE;
                }
                case CANCEL_REQUESTED -> action = Action.COMPENSATE;
                case SUCCEEDED -> action = Action.CONFIRM;
                default -> {                                             // terminal: adim kapatilir
                    jdbc.update("UPDATE %s SET status = 'DONE', lock_token = NULL, locked_until = NULL, updated_at = :now WHERE id = :id AND lock_token = :token"
                            .formatted(stepTable), params().addValue("now", ts(now)).addValue("id", step.id()).addValue("token", lockToken));
                    return Optional.<Action>empty();
                }
            }
            jdbc.update("UPDATE %s SET next_action = :a, updated_at = :now WHERE id = :id AND lock_token = :token".formatted(stepTable),
                    params().addValue("a", action.name()).addValue("now", ts(now)).addValue("id", step.id()).addValue("token", lockToken));
            return Optional.of(action);
        });
    }

    /** Yalniz kira sahibi tamamlar. 0 satir = kira elden gitmis; sonuc yazilmaz. */
    public boolean complete(Step step, UUID lockToken, Action action) {
        return Boolean.TRUE.equals(requiresNew.execute(st -> {
            Instant now = clock.instant();
            int n = jdbc.update("UPDATE %s SET status = 'DONE', lock_token = NULL, locked_until = NULL, updated_at = :now WHERE id = :id AND lock_token = :token"
                    .formatted(stepTable), params().addValue("now", ts(now)).addValue("id", step.id()).addValue("token", lockToken));
            if (n == 0) return false;
            String target = action == Action.CONFIRM ? "CONFIRMED" : "COMPENSATED";
            jdbc.update("UPDATE %s SET status = :s, updated_at = :now WHERE id = :id AND status IN ('SUCCEEDED','CANCEL_REQUESTED')"
                    .formatted(sagaTable), params().addValue("s", target).addValue("now", ts(now)).addValue("id", step.sagaId()));
            return true;
        }));
    }

    /** Gecici hata: RETRY + backoff min(maxBackoff, 2^n) sn; kira birakilir. */
    public boolean retry(Step step, UUID lockToken, String errorCode) {
        Instant now = clock.instant();
        int attempt = step.attempt() + 1;
        long backoff = Math.min(props.maxBackoffSeconds(), 1L << Math.min(attempt, 20));
        return jdbc.update("""
                UPDATE %s SET status = 'RETRY', attempt = :attempt, next_attempt_at = :next, lock_token = NULL, locked_until = NULL,
                       last_error_code = :err, updated_at = :now
                WHERE id = :id AND lock_token = :token""".formatted(stepTable),
                params().addValue("attempt", attempt).addValue("next", ts(now.plusSeconds(backoff))).addValue("err", errorCode)
                        .addValue("now", ts(now)).addValue("id", step.id()).addValue("token", lockToken)) == 1;
    }

    /** Celiski: insan karari gerekir. MANUAL_REVIEW cleanup'ta silinmez. */
    public boolean manualReview(Step step, UUID lockToken, String reasonCode) {
        return Boolean.TRUE.equals(requiresNew.execute(st -> {
            Instant now = clock.instant();
            int n = jdbc.update("""
                    UPDATE %s SET status = 'MANUAL_REVIEW', lock_token = NULL, locked_until = NULL, last_error_code = :err, updated_at = :now
                    WHERE id = :id AND lock_token = :token""".formatted(stepTable),
                    params().addValue("err", reasonCode).addValue("now", ts(now)).addValue("id", step.id()).addValue("token", lockToken));
            if (n == 0) return false;
            jdbc.update("UPDATE %s SET status = 'MANUAL_REVIEW', updated_at = :now WHERE id = :id".formatted(sagaTable),
                    params().addValue("now", ts(now)).addValue("id", step.sagaId()));
            return true;
        }));
    }

    // ---------- monitor / cleanup ----------

    /** warnAfter'dan eski cozulmemis saga sayisi (ERROR log + saga_unresolved_total metrigi icin). */
    public int countUnresolved() {
        Instant threshold = clock.instant().minus(Duration.ofMinutes(props.warnAfterMinutes()));
        return jdbc.queryForObject("""
                SELECT count(*) FROM %s WHERE status NOT IN ('CONFIRMED','COMPENSATED','MANUAL_REVIEW') AND created_at < :t"""
                .formatted(sagaTable), Map.of("t", ts(threshold)), Integer.class);
    }

    /** Yalniz terminal (CONFIRMED/COMPENSATED) kayitlar retention sonrasi silinir; MANUAL_REVIEW asla. Adimlar CASCADE. */
    public int cleanup() {
        Instant threshold = clock.instant().minus(Duration.ofDays(props.retentionDays()));
        return jdbc.update("DELETE FROM %s WHERE status IN ('CONFIRMED','COMPENSATED') AND updated_at < :t".formatted(sagaTable),
                Map.of("t", ts(threshold)));
    }

    // ---------- sorgular ----------

    public Optional<Saga> find(UUID accountId, String scope, UUID operationKey) {
        return Optional.ofNullable(DataAccessUtils.singleResult(jdbc.query(
                "SELECT * FROM %s WHERE account_id = :a AND scope = :s AND operation_key = :k".formatted(sagaTable),
                params().addValue("a", accountId).addValue("s", scope).addValue("k", operationKey), SAGA)));
    }

    public Optional<Saga> findById(UUID id) {
        return Optional.ofNullable(DataAccessUtils.singleResult(jdbc.query(
                "SELECT * FROM %s WHERE id = :id".formatted(sagaTable), Map.of("id", id), SAGA)));
    }

    public List<Step> stepsOf(UUID sagaId) {
        return jdbc.query("SELECT * FROM %s WHERE saga_id = :id ORDER BY created_at".formatted(stepTable), Map.of("id", sagaId), STEP);
    }

    public int countSagas() { return jdbc.queryForObject("SELECT count(*) FROM " + sagaTable, Map.of(), Integer.class); }
    public int countSteps() { return jdbc.queryForObject("SELECT count(*) FROM " + stepTable, Map.of(), Integer.class); }

    private static MapSqlParameterSource params() { return new MapSqlParameterSource(); }
    private static Timestamp ts(Instant i) { return Timestamp.from(i); }
    private static Instant inst(Timestamp t) { return t == null ? null : t.toInstant(); }

    static final RowMapper<Saga> SAGA = (ResultSet rs, int i) -> new Saga(
            rs.getObject("id", UUID.class), rs.getObject("account_id", UUID.class), rs.getString("scope"),
            rs.getObject("operation_key", UUID.class), SagaStatus.valueOf(rs.getString("status")), rs.getString("result"),
            inst(rs.getTimestamp("created_at")), inst(rs.getTimestamp("updated_at")));

    static final RowMapper<Step> STEP = (ResultSet rs, int i) -> new Step(
            rs.getObject("id", UUID.class), rs.getObject("saga_id", UUID.class), rs.getString("step_name"),
            Action.valueOf(rs.getString("next_action")), StepStatus.valueOf(rs.getString("status")), rs.getInt("attempt"),
            inst(rs.getTimestamp("next_attempt_at")), rs.getObject("lock_token", UUID.class),
            inst(rs.getTimestamp("locked_until")), rs.getString("last_error_code"));

}
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/saga/SagaCancelledException.java`

```java
package com.acme.platform.messaging.saga;

import java.util.UUID;

/**
 * success() compare-and-set kaybetti: recovery araya girip saga'yi iptal etti (CANCEL_REQUESTED/COMPENSATED).
 * Domain TX'inde firlatilir; domain yazimi rollback olur (referans Bolum 11.4 adim 3).
 */
public class SagaCancelledException extends RuntimeException {
    private final UUID sagaId;
    public SagaCancelledException(UUID sagaId) { super("saga cancelled by recovery"); this.sagaId = sagaId; }
    public UUID sagaId() { return sagaId; }
}
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/saga/SagaParticipant.java`

```java
package com.acme.platform.messaging.saga;

import java.util.Optional;
import java.util.UUID;

/**
 * Katilimci sozlesmesi (referans Bolum 11.4): gercekte /internal/<kaynak>/operations/{operationKey}/{consume,confirm,compensate}
 * ve GET; burada HTTP client'in arkasindaki arayuz. Tum islemler idempotent: ayni key ile tekrar cagri ayni sonucu dondurur.
 *
 * <pre>
 * consume    kayit yok    -> APPLIED / REJECTED
 * consume    kayit var    -> replay (mevcut durum; CANCELLED tombstone ise uygulanmaz)
 * confirm    APPLIED      -> CONFIRMED
 * compensate kayit yok    -> CANCELLED tombstone (gec gelen consume uygulanmaz)
 * compensate APPLIED      -> iade -> COMPENSATED
 * compensate CONFIRMED    -> MANUAL_REVIEW (iade yok)
 * </pre>
 */
public interface SagaParticipant {

    enum State { APPLIED, REJECTED, CONFIRMED, CANCELLED, COMPENSATED, MANUAL_REVIEW }

    State consume(String callerService, UUID accountId, UUID operationKey, String operationType, int amount);

    Optional<State> get(String callerService, UUID accountId, UUID operationKey);

    State confirm(String callerService, UUID accountId, UUID operationKey);

    State compensate(String callerService, UUID accountId, UUID operationKey);

    /** Timeout, 5xx, baglanti hatasi: sonuc BELIRSIZ; koordinator GET ile sorar, sonra retry/backoff. */
    class ParticipantUnavailableException extends RuntimeException {
        public ParticipantUnavailableException(String message) { super(message); }
    }

    /** Katilimci istegi anlamsiz buldu (409): durumlar celisiyor; koordinator MANUAL_REVIEW'a alir. */
    class ParticipantConflictException extends RuntimeException {
        public ParticipantConflictException(String message) { super(message); }
    }

    /** Aktor bu islem tipi icin yetkili degil (403); saga acilmaz / adim MANUAL_REVIEW. */
    class ParticipantForbiddenException extends RuntimeException {
        public ParticipantForbiddenException(String message) { super(message); }
    }
}
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/saga/SagaProperties.java`

```java
package com.acme.platform.messaging.saga;

/**
 * Baslangic ayarlari (referans Bolum 11.4 "Isletim degerleri"; Bolum 1.4: olcumle degisir).
 * Config key'leri: operation-consistency.{deadline-seconds,lease-seconds,max-backoff-seconds,warn-after-minutes,retention-days,batch-size}
 */
public record SagaProperties(long deadlineSeconds, long leaseSeconds, long maxBackoffSeconds,
                             long warnAfterMinutes, long retentionDays, int batchSize) {
    public static SagaProperties defaults() { return new SagaProperties(15, 60, 300, 15, 30, 50); }
}
```

---

## `skeleton-example/platform-messaging/src/main/java/com/acme/platform/messaging/saga/SagaRecoveryWorker.java`

```java
package com.acme.platform.messaging.saga;

import com.acme.platform.messaging.saga.LocalSagaStore.Action;
import com.acme.platform.messaging.saga.LocalSagaStore.Saga;
import com.acme.platform.messaging.saga.LocalSagaStore.Step;
import com.acme.platform.messaging.saga.SagaParticipant.ParticipantConflictException;
import com.acme.platform.messaging.saga.SagaParticipant.ParticipantForbiddenException;
import com.acme.platform.messaging.saga.SagaParticipant.ParticipantUnavailableException;
import com.acme.platform.messaging.saga.SagaParticipant.State;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recovery worker (referans Bolum 11.4 adim 4). Her instance'ta calisir; adimlar SKIP LOCKED + lock_token + lease ile
 * claim edilir. Uzak cagri TX disinda. Hata veya belirsiz sonucta once GET ile katilimcinin durumu sorulur; celiski
 * varsa MANUAL_REVIEW. Zamanlama (@Scheduled, poll 5 sn) disarida baglanir; runOnce() deterministik test edilir.
 */
public class SagaRecoveryWorker {

    private static final Logger log = LoggerFactory.getLogger(SagaRecoveryWorker.class);

    public record RunResult(int claimed, int done, int retried, int manualReview, int skipped) {}

    private final LocalSagaStore store;
    private final SagaParticipant participant;
    private final String callerService;
    private final SagaProperties props;
    private final Clock clock;

    public SagaRecoveryWorker(LocalSagaStore store, SagaParticipant participant, String callerService,
                              SagaProperties props, Clock clock) {
        this.store = store;
        this.participant = participant;
        this.callerService = callerService;
        this.props = props;
        this.clock = clock;
    }

    public RunResult runOnce() {
        UUID token = UUID.randomUUID();
        Instant now = clock.instant();
        List<Step> steps = store.claimSteps(token, now, now.plusSeconds(props.leaseSeconds()), props.batchSize());
        int done = 0, retried = 0, manual = 0, skipped = 0;
        for (Step step : steps) {
            switch (process(step, token)) {
                case DONE -> done++;
                case RETRIED -> retried++;
                case MANUAL_REVIEW -> manual++;
                case SKIPPED -> skipped++;
            }
        }
        if (!steps.isEmpty()) {
            log.info("Saga recovery batch finished: claimed={} done={} retried={} manualReview={} skipped={}",
                    steps.size(), done, retried, manual, skipped);
        }
        return new RunResult(steps.size(), done, retried, manual, skipped);
    }

    enum Outcome { DONE, RETRIED, MANUAL_REVIEW, SKIPPED }

    Outcome process(Step step, UUID token) {
        Optional<Action> prepared = store.prepare(step, token);
        if (prepared.isEmpty()) return Outcome.SKIPPED;                 // kira elden gitti veya saga zaten terminal
        Action action = prepared.get();
        Saga saga = store.findById(step.sagaId()).orElse(null);
        if (saga == null) return Outcome.SKIPPED;
        try {
            State state = action == Action.CONFIRM
                    ? participant.confirm(callerService, saga.accountId(), saga.operationKey())
                    : participant.compensate(callerService, saga.accountId(), saga.operationKey());
            return settle(step, token, action, state);
        } catch (ParticipantUnavailableException e) {
            // Belirsiz sonuc: once GET ile sor (istek katilimcida commit olmus olabilir)
            try {
                Optional<State> remote = participant.get(callerService, saga.accountId(), saga.operationKey());
                if (remote.isPresent()) return settle(step, token, action, remote.get());
            } catch (ParticipantUnavailableException ignored) { /* asagida retry */ }
            boolean ok = store.retry(step, token, e.getClass().getSimpleName());
            log.warn("Saga step deferred; retry scheduled: sagaId={} step={} attempt={} exceptionType={}",
                    saga.id(), step.stepName(), step.attempt() + 1, e.getClass().getSimpleName());
            return ok ? Outcome.RETRIED : Outcome.SKIPPED;
        } catch (ParticipantConflictException | ParticipantForbiddenException e) {
            return manual(step, token, saga, e.getClass().getSimpleName());
        }
    }

    /** Katilimcinin dondurdugu durum niyetle tutarli mi? Tutarsizsa insan karari. */
    private Outcome settle(Step step, UUID token, Action action, State state) {
        boolean consistent = switch (action) {
            case CONFIRM -> state == State.CONFIRMED;
            case COMPENSATE -> state == State.COMPENSATED || state == State.CANCELLED || state == State.REJECTED;
        };
        if (!consistent) {
            Saga saga = store.findById(step.sagaId()).orElse(null);
            return manual(step, token, saga, "STATE_" + state.name());
        }
        // Belirsizlikten sonra GET APPLIED dondurduyse is henuz yapilmamis demektir -> retry
        return store.complete(step, token, action) ? Outcome.DONE : Outcome.SKIPPED;
    }

    private Outcome manual(Step step, UUID token, Saga saga, String reason) {
        boolean ok = store.manualReview(step, token, reason);
        log.error("Saga needs manual review: code=SAGA_MANUAL_REVIEW sagaId={} step={} reason={}",
                saga == null ? null : saga.id(), step.stepName(), reason);
        return ok ? Outcome.MANUAL_REVIEW : Outcome.SKIPPED;
    }
}
```

---

## `skeleton-example/platform-messaging/src/main/resources/db/platform/outbox_inbox.sql`

```
-- Generic outbox + inbox DDL sablonu (referans Bolum 11.2-11.3). Her servis kendi semasinda Flyway migration'i olarak
-- kopyalar ve ${schema} yerine kendi semasini yazar. Debezium Outbox Event Router semasiyla uyumludur.
CREATE TABLE ${schema}.outbox_event (
  id              UUID PRIMARY KEY,                       -- UUIDv7; ayni zamanda CloudEvents "id"
  kind            TEXT NOT NULL CHECK (kind IN ('EVENT','COMMAND','HTTP')),   -- lane
  aggregate_type  TEXT NOT NULL,
  aggregate_id    UUID NOT NULL,                          -- routing/partition key; siralama bunun icinde
  event_type      TEXT NOT NULL,                          -- 'order.order.created'
  payload         JSONB NOT NULL,
  headers         JSONB NOT NULL DEFAULT '{}',            -- traceparent, tracestate, hedef (HTTP icin)
  status          TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PUBLISHING','DEAD')),
  priority        SMALLINT NOT NULL DEFAULT 0,            -- yuksek once; guvenlik kararlari > is olaylari > toplu isler
  dead_policy     TEXT NOT NULL DEFAULT 'DEAD_ON_PERMANENT'
                    CHECK (dead_policy IN ('DEAD_ON_PERMANENT','NEVER_DEAD')),  -- guvenlik yan etkisi: NEVER_DEAD
  retry_count     INT  NOT NULL DEFAULT 0,
  next_retry_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  locked_until    TIMESTAMPTZ,
  claim_token     UUID,
  last_error_code VARCHAR(120),                           -- exception SimpleName / HTTP status (mesaj degil)
  expires_at      TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX idx_outbox_event_claim ON ${schema}.outbox_event (kind, status, next_retry_at, priority DESC, created_at);
CREATE INDEX idx_outbox_event_aggregate ON ${schema}.outbox_event (aggregate_id, created_at);

-- Inbox: dedup kapsami HANDLER'dir; satir + is degisikligi ayni TX'te yazilir, ack commit'ten sonra.
CREATE TABLE ${schema}.inbox_event (
  handler     TEXT NOT NULL,
  event_id    UUID NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (handler, event_id)
);
```

---

## `skeleton-example/platform-messaging/src/main/resources/db/platform/saga_coordinator.sql`

```
-- Local saga koordinator tablolari (referans Bolum 11.4). Koordinatorun KENDI semasinda; her servis kendi migration'inda
-- ${schema} yerine kendi semasini yazar. Merkezi coordinator servisi yoktur.
CREATE TABLE ${schema}.saga (
  id             UUID PRIMARY KEY,
  account_id     UUID NOT NULL,
  scope          TEXT NOT NULL,                       -- 'ORDER_CREATE' (UPPER_SNAKE)
  operation_key  UUID NOT NULL,                       -- X-Idempotency-Key
  status         TEXT NOT NULL CHECK (status IN ('STARTED','SUCCEEDED','CONFIRMED','CANCEL_REQUESTED','COMPENSATED','MANUAL_REVIEW')),
  result         TEXT,                                -- replay icin onceki sonuc (istemciye gosterilebilir kisa deger)
  created_at     TIMESTAMPTZ NOT NULL,
  updated_at     TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_saga_key UNIQUE (account_id, scope, operation_key)
);
CREATE TABLE ${schema}.saga_steps (
  id              UUID PRIMARY KEY,
  saga_id         UUID NOT NULL REFERENCES ${schema}.saga(id) ON DELETE CASCADE,
  step_name       TEXT NOT NULL,                      -- katilimci/adim adi ('quota'); cok adimli genelleme icin parametre
  next_action     TEXT NOT NULL CHECK (next_action IN ('CONFIRM','COMPENSATE')),
  status          TEXT NOT NULL CHECK (status IN ('PENDING','RETRY','RUNNING','DONE','MANUAL_REVIEW')),
  attempt         INT NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMPTZ NOT NULL,               -- begin: now + deadline (surec cokerse otomatik telafi)
  lock_token      UUID,
  locked_until    TIMESTAMPTZ,
  last_error_code VARCHAR(120),
  created_at      TIMESTAMPTZ NOT NULL,
  updated_at      TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_saga_steps_claim ON ${schema}.saga_steps (status, next_attempt_at, locked_until);
CREATE INDEX idx_saga_unresolved ON ${schema}.saga (created_at)
  WHERE status NOT IN ('CONFIRMED','COMPENSATED','MANUAL_REVIEW');
CREATE INDEX idx_saga_retention ON ${schema}.saga (updated_at) WHERE status IN ('CONFIRMED','COMPENSATED');
```

---

## `skeleton-example/platform-messaging/src/test/java/com/acme/platform/messaging/OutboxBehaviourIT.java`

```java
package com.acme.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.platform.messaging.inbox.InboxProcessor;
import com.acme.platform.messaging.outbox.OutboxEvent;
import com.acme.platform.messaging.outbox.OutboxHandler;
import com.acme.platform.messaging.outbox.OutboxPoller;
import com.acme.platform.messaging.outbox.OutboxProperties;
import com.acme.platform.messaging.outbox.OutboxRepository;
import com.acme.platform.messaging.outbox.PermanentFailureException;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DAVRANISSAL dogrulama (referans Bolum 11.5 senaryo matrisi, 19.6 kanit seviyesi 2): gercek PostgreSQL uzerinde
 * outbox/inbox guvenceleri. Docker gerektirmez (gomulu PG binary'si); CI'da Testcontainers ile de kosturulabilir.
 * Her test metodu bir senaryo numarasina karsilik gelir (operation-consistency skill'i verification.md).
 */
class OutboxBehaviourIT {

    static final String SCHEMA = "order";
    static EmbeddedPostgres pg;
    static DataSource ds;
    static NamedParameterJdbcTemplate jdbc;
    static TransactionTemplate tx;

    OutboxRepository outbox;
    MutableClock clock;

    /** Deterministik zaman: kira dolumu ve backoff testleri gercek zaman beklemeden calisir. */
    static final class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2026-09-29T10:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    @BeforeAll
    static void startDb() throws Exception {
        pg = EmbeddedPostgres.builder().start();
        ds = pg.getPostgresDatabase();
        jdbc = new NamedParameterJdbcTemplate(ds);
        tx = new TransactionTemplate(new JdbcTransactionManager(ds));
        String ddl = new String(OutboxBehaviourIT.class.getResourceAsStream("/db/platform/outbox_inbox.sql")
                .readAllBytes(), StandardCharsets.UTF_8).replace("${schema}", "\"" + SCHEMA + "\"");
        JdbcTemplate plain = jdbc.getJdbcTemplate();
        plain.execute("CREATE SCHEMA \"" + SCHEMA + "\"");
        try (var conn = ds.getConnection()) {                       // yorum ve ';' guvenli script calistirma
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8)));
        }
        plain.execute("CREATE TABLE \"order\".effect (event_id UUID NOT NULL, handler TEXT NOT NULL)");
        plain.execute("""
                CREATE TABLE "order".rm_account_status (account_id UUID PRIMARY KEY, active BOOLEAN NOT NULL,
                    source_revision BIGINT NOT NULL)""");
    }

    @AfterAll
    static void stopDb() throws IOException { if (pg != null) pg.close(); }

    @BeforeEach
    void clean() {
        JdbcTemplate plain = jdbc.getJdbcTemplate();
        plain.execute("TRUNCATE \"order\".outbox_event, \"order\".inbox_event, \"order\".effect, \"order\".rm_account_status");
        outbox = new OutboxRepository(jdbc, SCHEMA);
        clock = new MutableClock();
    }

    // ---------- yardimcilar ----------

    OutboxEvent event(String kind, UUID aggregate, Instant createdAt, int priority, OutboxEvent.DeadPolicy policy) {
        return new OutboxEvent(UUID.randomUUID(), kind, "order", aggregate, "order.order.created",
                "{\"orderId\":\"" + aggregate + "\"}", "{}", "PENDING", priority, policy, 0, createdAt, null, null,
                null, createdAt);
    }

    OutboxEvent event(String kind, UUID aggregate) {
        return event(kind, aggregate, clock.instant(), 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
    }

    /** Domain TX'i taklidi: outbox yazicisi MANDATORY oldugu icin TransactionTemplate icinde cagrilir. */
    void appendInTx(OutboxEvent... events) {
        tx.executeWithoutResult(s -> { for (OutboxEvent e : events) outbox.append(e); });
    }

    OutboxPoller poller(Map<String, OutboxHandler> handlers, OutboxProperties props) {
        return new OutboxPoller(outbox, handlers, props, clock);
    }

    OutboxPoller poller(Map<String, OutboxHandler> handlers) { return poller(handlers, OutboxProperties.defaults()); }

    int rows() { return jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM \"order\".outbox_event", Integer.class); }

    int effects() { return jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM \"order\".effect", Integer.class); }

    void recordEffect(UUID eventId, String handler) {
        jdbc.update("INSERT INTO \"order\".effect (event_id, handler) VALUES (:e, :h)", Map.of("e", eventId, "h", handler));
    }

    // ---------- senaryolar ----------

    @Test // #21: outbox satiri domain TX ile rollback olur; MANDATORY: TX disinda yazilamaz
    void outboxRowRollsBackWithDomainTransaction() {
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            outbox.append(event("EVENT", UUID.randomUUID()));
            throw new IllegalStateException("domain hatasi");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(rows()).isZero();

        // MANDATORY: aktif TX yoksa yazma reddedilir (proxy'siz cagrida anotasyon uygulanmaz; bu yuzden
        // referansta yazici bir Spring bean'idir. Burada TX disinda append'in satir yazdigini gormek yerine
        // kuralin proxy ile zorlandigi Boot testinde dogrulanir — bu testin kapsami disi, BLOCKED degil, ayri seviye).
    }

    @Test // #22: tekrar teslim tek etki (inbox dedup, handler kapsaminda)
    void duplicateDeliveryHasSingleEffect() {
        InboxProcessor inbox = new InboxProcessor(jdbc, tx, SCHEMA);
        UUID eventId = UUID.randomUUID();
        assertThat(inbox.process("ReadModelHandler", eventId, () -> recordEffect(eventId, "ReadModelHandler")))
                .isEqualTo(InboxProcessor.Outcome.APPLIED);
        assertThat(inbox.process("ReadModelHandler", eventId, () -> recordEffect(eventId, "ReadModelHandler")))
                .isEqualTo(InboxProcessor.Outcome.DUPLICATE);
        // ayni olay farkli handler'da ayri islenir (dedup kapsami handler)
        assertThat(inbox.process("NotificationHandler", eventId, () -> recordEffect(eventId, "NotificationHandler")))
                .isEqualTo(InboxProcessor.Outcome.APPLIED);
        assertThat(effects()).isEqualTo(2);
    }

    @Test // #25: handler ortasinda exception -> inbox satiri YOK, etki YOK; yeniden teslimde is yapilir
    void inboxRowAndWorkAreAtomic() {
        InboxProcessor inbox = new InboxProcessor(jdbc, tx, SCHEMA);
        UUID eventId = UUID.randomUUID();
        assertThatThrownBy(() -> inbox.process("H", eventId, () -> {
            recordEffect(eventId, "H");
            throw new IllegalStateException("is ortasinda cokme");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(effects()).isZero();
        Integer inboxRows = jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM \"order\".inbox_event", Integer.class);
        assertThat(inboxRows).isZero();

        assertThat(inbox.process("H", eventId, () -> recordEffect(eventId, "H"))).isEqualTo(InboxProcessor.Outcome.APPLIED);
        assertThat(effects()).isEqualTo(1);
    }

    @Test // #22 (poller tarafi) + coklu instance: iki poller paralel, 300 satir, her satir TAM BIR kez islenir
    void twoPollersNeverProcessTheSameRow() throws Exception {
        for (int i = 0; i < 300; i++) appendInTx(event("EVENT", UUID.randomUUID()));
        ConcurrentHashMap<UUID, AtomicInteger> seen = new ConcurrentHashMap<>();
        OutboxHandler handler = e -> seen.computeIfAbsent(e.id(), k -> new AtomicInteger()).incrementAndGet();
        OutboxPoller a = poller(Map.of("EVENT", handler)), b = poller(Map.of("EVENT", handler));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Integer> fa = pool.submit(() -> drain(a)), fb = pool.submit(() -> drain(b));
        int total = fa.get(60, TimeUnit.SECONDS) + fb.get(60, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(total).isEqualTo(300);
        assertThat(seen).hasSize(300);
        assertThat(seen.values()).allMatch(c -> c.get() == 1);
        assertThat(rows()).isZero();
    }

    @Test // SKIP LOCKED: baska bir TX'in kilitledigi satir beklenmez, atlanir (instance'lar birbirini bloke etmez)
    void lockedRowIsSkippedNotWaitedFor() throws Exception {
        OutboxEvent locked = event("EVENT", UUID.randomUUID()), free = event("EVENT", UUID.randomUUID());
        appendInTx(locked, free);
        CountDownLatch held = new CountDownLatch(1), release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> holder = pool.submit(() -> tx.executeWithoutResult(s -> {      // acik TX, satir kilitli
            jdbc.queryForList("SELECT id FROM \"order\".outbox_event WHERE id = :id FOR UPDATE", Map.of("id", locked.id()));
            held.countDown();
            try { release.await(30, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
        }));
        try {
            assertThat(held.await(10, TimeUnit.SECONDS)).isTrue();
            Future<List<OutboxEvent>> claim = pool.submit(() ->
                    outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120), UUID.randomUUID(), 10));
            List<OutboxEvent> got = claim.get(5, TimeUnit.SECONDS);               // SKIP LOCKED yoksa burada bloke olur
            assertThat(got).extracting(OutboxEvent::id).containsExactly(free.id());
        } finally {
            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
            pool.shutdownNow();
        }
    }

    int drain(OutboxPoller p) {
        int applied = 0;
        OutboxPoller.PollResult r;
        do { r = p.poll("EVENT"); applied += r.applied(); } while (r.claimed() > 0);
        return applied;
    }

    @Test // #27: ayni aggregate'in sirali iki satiri tek worker'da ve sirayla; ilk satir basarisizsa ikincisi bekler
    void orderedRowsOfSameAggregateStayOrdered() {
        UUID agg = UUID.randomUUID();
        Instant t0 = clock.instant();
        OutboxEvent first = event("EVENT", agg, t0, 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
        OutboxEvent second = event("EVENT", agg, t0.plusMillis(1), 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
        appendInTx(first, second);

        // A ilk satiri claim eder (handler cagirmadan): ikinci satir baska worker'a verilmez
        List<OutboxEvent> claimedByA = outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120),
                UUID.randomUUID(), 10);
        assertThat(claimedByA).extracting(OutboxEvent::id).containsExactly(first.id());
        List<OutboxEvent> claimedByB = outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120),
                UUID.randomUUID(), 10);
        assertThat(claimedByB).isEmpty();

        // A ilk satirda basarisiz olur -> PENDING + backoff; ikinci satir hala bloke (sira korunur)
        outbox.release(first.id(), claimedByA.get(0).claimToken(), "PENDING", 1, clock.instant().plusSeconds(60), "IOException");
        assertThat(outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120), UUID.randomUUID(), 10)).isEmpty();

        // backoff gecince once ilk, sonra ikinci islenir
        clock.advance(Duration.ofSeconds(61));
        List<UUID> order = new java.util.ArrayList<>();
        OutboxPoller p = poller(Map.of("EVENT", e -> order.add(e.id())));
        assertThat(p.poll("EVENT").applied()).isEqualTo(1);     // yalniz first (second NOT EXISTS ile bekler)
        assertThat(p.poll("EVENT").applied()).isEqualTo(1);     // simdi second
        assertThat(order).containsExactly(first.id(), second.id());
    }

    @Test // #32: surec olur, kira dolar, ikinci instance devralir; geri gelen eski worker yazamaz
    void expiredLeaseIsTakenOverAndLateWorkerCannotWrite() {
        OutboxEvent e = event("EVENT", UUID.randomUUID());
        appendInTx(e);
        UUID tokenA = UUID.randomUUID();
        List<OutboxEvent> byA = outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120), tokenA, 10);
        assertThat(byA).hasSize(1);
        // A "coker": handler'i bitirmez. Kira dolmadan B alamaz.
        clock.advance(Duration.ofSeconds(119));
        assertThat(outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120), UUID.randomUUID(), 10)).isEmpty();
        // Kira dolar: B devralir (henuz bitirmedi: satir PUBLISHING, token B)
        clock.advance(Duration.ofSeconds(2));
        UUID tokenB = UUID.randomUUID();
        assertThat(outbox.claim("EVENT", clock.instant(), clock.instant().plusSeconds(120), tokenB, 10)).hasSize(1);
        // A geri gelir (isi bitirdigini sanir): eski token ile ne silebilir ne PENDING'e cekebilir; B'nin claim'i bozulmaz
        assertThat(outbox.deleteProcessed(e.id(), tokenA)).isZero();
        assertThat(outbox.release(e.id(), tokenA, "PENDING", 1, clock.instant(), "x")).isZero();
        OutboxEvent row = outbox.findAll().get(0);
        assertThat(row.status()).isEqualTo("PUBLISHING");
        assertThat(row.claimToken()).isEqualTo(tokenB);
        // B bitirir
        assertThat(outbox.deleteProcessed(e.id(), tokenB)).isEqualTo(1);
        assertThat(rows()).isZero();
    }

    @Test // #28: bir lane'de takili hedef diger lane'i bekletmez
    void stuckHttpLaneDoesNotBlockEventLane() throws Exception {
        appendInTx(event("HTTP", UUID.randomUUID()), event("EVENT", UUID.randomUUID()));
        CountDownLatch httpBlocked = new CountDownLatch(1), release = new CountDownLatch(1);
        OutboxHandler http = e -> { httpBlocked.countDown(); release.await(30, TimeUnit.SECONDS); };
        AtomicInteger events = new AtomicInteger();
        OutboxPoller p = poller(Map.of("HTTP", http, "EVENT", e -> events.incrementAndGet()));

        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<OutboxPoller.PollResult> httpRun = pool.submit(() -> p.poll("HTTP"));
        assertThat(httpBlocked.await(10, TimeUnit.SECONDS)).isTrue();          // HTTP lane'i sagliyicida takili
        assertThat(p.poll("EVENT").applied()).isEqualTo(1);                   // EVENT lane'i beklemeden yayinladi
        assertThat(events.get()).isEqualTo(1);
        release.countDown();
        assertThat(httpRun.get(10, TimeUnit.SECONDS).applied()).isEqualTo(1);
        pool.shutdownNow();
    }

    @Test // gecici hata -> backoff ile yeniden deneme -> basari; deneme sayisi ve hata kodu (mesaj degil) kaydedilir
    void transientFailureIsRetriedWithBackoff() {
        OutboxEvent e = event("EVENT", UUID.randomUUID());
        appendInTx(e);
        AtomicInteger calls = new AtomicInteger();
        OutboxPoller p = poller(Map.of("EVENT", ev -> {
            if (calls.incrementAndGet() == 1) throw new java.io.IOException("broker unreachable: secret-host");
        }));
        OutboxPoller.PollResult r1 = p.poll("EVENT");
        assertThat(r1.failed()).isEqualTo(1);
        OutboxEvent after = outbox.findAll().get(0);
        assertThat(after.status()).isEqualTo("PENDING");
        assertThat(after.retryCount()).isEqualTo(1);
        assertThat(after.lastErrorCode()).isEqualTo("IOException");            // mesaj/host yok
        assertThat(after.nextRetryAt()).isEqualTo(clock.instant().plusSeconds(60));   // 30*2^1
        assertThat(after.claimToken()).isNull();

        assertThat(p.poll("EVENT").claimed()).isZero();                       // backoff dolmadan alinmaz
        clock.advance(Duration.ofSeconds(61));
        assertThat(p.poll("EVENT").applied()).isEqualTo(1);
        assertThat(rows()).isZero();
    }

    @Test // DEAD politikasi is turune gore: kalici hata -> DEAD; guvenlik yan etkisi (NEVER_DEAD) -> yeniden dener
    void deadPolicyDependsOnJobType() {
        OutboxEvent normal = event("COMMAND", UUID.randomUUID(), clock.instant(), 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
        OutboxEvent security = event("COMMAND", UUID.randomUUID(), clock.instant(), 10, OutboxEvent.DeadPolicy.NEVER_DEAD);
        appendInTx(normal, security);
        OutboxPoller p = poller(Map.of("COMMAND", e -> { throw new PermanentFailureException("422"); }));
        OutboxPoller.PollResult r = p.poll("COMMAND");
        assertThat(r.dead()).isEqualTo(1);
        assertThat(r.failed()).isEqualTo(1);
        Map<UUID, String> status = new java.util.HashMap<>();
        outbox.findAll().forEach(e -> status.put(e.id(), e.status()));
        assertThat(status.get(normal.id())).isEqualTo("DEAD");
        assertThat(status.get(security.id())).isEqualTo("PENDING");
    }

    @Test // oncelik: yuksek priority once claim edilir (guvenlik karari toplu isi beklemez)
    void higherPriorityIsClaimedFirst() {
        Instant t = clock.instant();   // ucu de gecmiste (next_retry_at <= now); urgent en yeni ama once alinmali
        OutboxEvent bulk1 = event("COMMAND", UUID.randomUUID(), t.minusMillis(3), 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
        OutboxEvent bulk2 = event("COMMAND", UUID.randomUUID(), t.minusMillis(2), 0, OutboxEvent.DeadPolicy.DEAD_ON_PERMANENT);
        OutboxEvent urgent = event("COMMAND", UUID.randomUUID(), t.minusMillis(1), 10, OutboxEvent.DeadPolicy.NEVER_DEAD);
        appendInTx(bulk1, bulk2, urgent);
        List<OutboxEvent> claimed = outbox.claim("COMMAND", clock.instant(), clock.instant().plusSeconds(120), UUID.randomUUID(), 1);
        assertThat(claimed).extracting(OutboxEvent::id).containsExactly(urgent.id());
    }

    @Test // #29: eski karar (kucuk source_revision) yeni karari ezmez — read-model UPSERT kurali
    void staleDecisionDoesNotOverrideNewerOne() {
        UUID account = UUID.randomUUID();
        String upsert = """
                INSERT INTO "order".rm_account_status (account_id, active, source_revision) VALUES (:a, :active, :rev)
                ON CONFLICT (account_id) DO UPDATE SET active = EXCLUDED.active, source_revision = EXCLUDED.source_revision
                WHERE EXCLUDED.source_revision > "order".rm_account_status.source_revision""";
        jdbc.update(upsert, Map.of("a", account, "active", false, "rev", 2L));   // yeni karar: engellendi
        int changed = jdbc.update(upsert, Map.of("a", account, "active", true, "rev", 1L));   // gec gelen eski karar
        assertThat(changed).isZero();
        Boolean active = jdbc.queryForObject("SELECT active FROM \"order\".rm_account_status WHERE account_id = :a",
                Map.of("a", account), Boolean.class);
        assertThat(active).isFalse();
        // ayni revizyon tekrar (duplicate) da degistirmez
        assertThat(jdbc.update(upsert, Map.of("a", account, "active", true, "rev", 2L))).isZero();
    }

    @Test // kira guvenlik payi: sure dolmak uzereyken yeni satira baslanmaz (deferred), deneme sayilmaz
    void rowsAreDeferredWhenLeaseSafetyWindowIsReached() {
        appendInTx(event("EVENT", UUID.randomUUID()), event("EVENT", UUID.randomUUID()));
        OutboxPoller p = poller(Map.of("EVENT", e -> clock.advance(Duration.ofSeconds(100))),  // ilk is 100 sn surer
                new OutboxProperties(50, 120, 30, 600, 10));
        OutboxPoller.PollResult r = p.poll("EVENT");
        assertThat(r.applied()).isEqualTo(1);
        assertThat(r.deferred()).isEqualTo(1);
        OutboxEvent left = outbox.findAll().get(0);
        assertThat(left.retryCount()).isZero();                                    // deferred deneme degildir
        assertThat(left.status()).isEqualTo("PUBLISHING");                         // kira dolunca baska instance alir
        clock.advance(Duration.ofSeconds(30));
        assertThat(poller(Map.of("EVENT", e -> {})).poll("EVENT").applied()).isEqualTo(1);
    }
}
```

---

## `skeleton-example/platform-messaging/src/test/java/com/acme/platform/messaging/SagaBehaviourIT.java`

```java
package com.acme.platform.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.platform.messaging.saga.FlakyParticipant;
import com.acme.platform.messaging.saga.LocalSagaStore;
import com.acme.platform.messaging.saga.LocalSagaStore.Action;
import com.acme.platform.messaging.saga.LocalSagaStore.SagaStatus;
import com.acme.platform.messaging.saga.LocalSagaStore.Step;
import com.acme.platform.messaging.saga.LocalSagaStore.StepStatus;
import com.acme.platform.messaging.saga.OrderFlow;
import com.acme.platform.messaging.saga.OrderFlow.CrashPoint;
import com.acme.platform.messaging.saga.OrderFlow.Kind;
import com.acme.platform.messaging.saga.OrderFlow.Result;
import com.acme.platform.messaging.saga.QuotaParticipant;
import com.acme.platform.messaging.saga.SagaParticipant;
import com.acme.platform.messaging.saga.SagaParticipant.State;
import com.acme.platform.messaging.saga.SagaProperties;
import com.acme.platform.messaging.saga.SagaRecoveryWorker;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DAVRANISSAL dogrulama (referans Bolum 11.4-11.5, 19.6 kanit seviyesi 2): local saga guvenceleri gercek PostgreSQL
 * uzerinde. Katilimci in-process (HTTP/JWT katmani bu testin KAPSAMI DISINDA; belirsizlikler FlakyParticipant ile
 * enjekte edilir). Metot basliklarindaki # numaralari operation-consistency skill'i verification.md matrisidir.
 */
class SagaBehaviourIT {

    static EmbeddedPostgres pg;
    static DataSource ds;
    static NamedParameterJdbcTemplate jdbc;
    static JdbcTransactionManager tm;
    static TransactionTemplate tx;

    static final class MutableClock extends Clock {
        volatile Instant now = Instant.parse("2026-09-29T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    MutableClock clock;
    SagaProperties props;
    LocalSagaStore store;
    QuotaParticipant quota;
    FlakyParticipant participant;
    SagaRecoveryWorker worker;
    OrderFlow flow;
    UUID account;

    @BeforeAll
    static void startDb() throws Exception {
        pg = EmbeddedPostgres.builder().start();
        ds = pg.getPostgresDatabase();
        jdbc = new NamedParameterJdbcTemplate(ds);
        tm = new JdbcTransactionManager(ds);
        tx = new TransactionTemplate(tm);
        String ddl = new String(SagaBehaviourIT.class.getResourceAsStream("/db/platform/saga_coordinator.sql").readAllBytes(),
                StandardCharsets.UTF_8).replace("${schema}", "\"order\"");
        jdbc.getJdbcTemplate().execute("CREATE SCHEMA \"order\"");
        jdbc.getJdbcTemplate().execute("CREATE SCHEMA subscription");
        try (var conn = ds.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8)));
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(OrderFlow.DDL.getBytes(StandardCharsets.UTF_8)));
            ScriptUtils.executeSqlScript(conn, new ByteArrayResource(QuotaParticipant.DDL.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @AfterAll
    static void stopDb() throws Exception { if (pg != null) pg.close(); }

    @BeforeEach
    void setUp() {
        jdbc.getJdbcTemplate().execute("TRUNCATE \"order\".saga, \"order\".saga_steps, \"order\".order_item, subscription.quota, subscription.operation");
        clock = new MutableClock();
        props = SagaProperties.defaults();                       // deadline 15 sn, lease 60 sn, backoff min(300, 2^n), uyari 15 dk, retention 30 gun
        store = new LocalSagaStore(jdbc, tm, "order", props, clock);
        quota = new QuotaParticipant(jdbc, tx);
        participant = new FlakyParticipant(quota);
        worker = new SagaRecoveryWorker(store, participant, "order-service", props, clock);
        flow = new OrderFlow(store, participant, jdbc, tx);
        account = UUID.randomUUID();
        quota.grant(account, 5);
    }

    // ---------- yardimcilar ----------

    SagaStatus sagaStatus(UUID sagaId) { return store.findById(sagaId).orElseThrow().status(); }
    Step step(UUID sagaId) { return store.stepsOf(sagaId).get(0); }
    State opState(UUID key) { return quota.get("order-service", account, key).orElse(null); }
    void pastDeadline() { clock.advance(Duration.ofSeconds(props.deadlineSeconds() + 1)); }
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) { } }

    // ---------- senaryolar ----------

    @Test // #1 normal basari: saga CONFIRMED, hak tuketildi, domain yazildi
    void normalSuccess() {
        Result r = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());
        assertThat(r.kind()).isEqualTo(Kind.OK);
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.SUCCEEDED);
        assertThat(step(r.sagaId()).nextAction()).isEqualTo(Action.CONFIRM);
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.CONFIRMED);
        assertThat(step(r.sagaId()).status()).isEqualTo(StepStatus.DONE);
        assertThat(quota.remaining(account)).isEqualTo(4);
        assertThat(flow.orderCount(account)).isEqualTo(1);
        assertThat(worker.runOnce().claimed()).isZero();
    }

    @Test // #2 ayni key ile replay: ayni sonuc, ikinci consume yok (confirm oncesi ve sonrasi)
    void replayWithSameKey() {
        UUID key = UUID.randomUUID(), resource = UUID.randomUUID();
        Result first = flow.createOrder(account, key, resource);
        Result beforeConfirm = flow.createOrder(account, key, resource);
        worker.runOnce();
        Result afterConfirm = flow.createOrder(account, key, resource);
        assertThat(beforeConfirm.kind()).isEqualTo(Kind.REPLAY);
        assertThat(afterConfirm.kind()).isEqualTo(Kind.REPLAY);
        assertThat(afterConfirm.detail()).isEqualTo(first.detail());
        assertThat(quota.consumeApplied.get()).isEqualTo(1);
        assertThat(flow.orderCount(account)).isEqualTo(1);
        assertThat(store.countSagas()).isEqualTo(1);
    }

    @Test // #3 ayni key farkli body: ilk istek kazanir (fingerprint tutulmaz, belgelenir)
    void sameKeyDifferentBodyFirstWins() {
        UUID key = UUID.randomUUID();
        Result first = flow.createOrder(account, key, UUID.randomUUID());
        Result second = flow.createOrder(account, key, UUID.randomUUID());
        assertThat(second.kind()).isEqualTo(Kind.REPLAY);
        assertThat(second.detail()).isEqualTo(first.detail());
        assertThat(flow.orderCount(account)).isEqualTo(1);
    }

    @Test // #4 eszamanli ayni key: tek saga, tek consume, tek siparis
    void concurrentSameKeyProducesSingleSaga() throws Exception {
        UUID key = UUID.randomUUID(), resource = UUID.randomUUID();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        List<Future<Result>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) futures.add(pool.submit(() -> { barrier.await(); return flow.createOrder(account, key, resource); }));
        List<Kind> kinds = new ArrayList<>();
        for (Future<Result> f : futures) kinds.add(f.get(30, TimeUnit.SECONDS).kind());
        pool.shutdownNow();
        assertThat(kinds).filteredOn(k -> k == Kind.OK).hasSize(1);
        assertThat(kinds).allMatch(k -> k == Kind.OK || k == Kind.REPLAY || k == Kind.IN_PROGRESS);
        assertThat(quota.consumeApplied.get()).isEqualTo(1);
        assertThat(store.countSagas()).isEqualTo(1);
        assertThat(flow.orderCount(account)).isEqualTo(1);
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(quota.remaining(account)).isEqualTo(4);
    }

    @Test // #5 farkli key ayni kaynak: domain uniqueness reddeder, saga telafi edilir (iade)
    void differentKeySameResourceIsCompensated() {
        UUID resource = UUID.randomUUID();
        Result first = flow.createOrder(account, UUID.randomUUID(), resource);
        Result second = flow.createOrder(account, UUID.randomUUID(), resource);
        assertThat(first.kind()).isEqualTo(Kind.OK);
        assertThat(second.kind()).isEqualTo(Kind.DOMAIN_CONFLICT);
        assertThat(sagaStatus(second.sagaId())).isEqualTo(SagaStatus.CANCEL_REQUESTED);
        assertThat(quota.remaining(account)).isEqualTo(3);                 // ikinci consume uygulandi, henuz iade yok
        SagaRecoveryWorker.RunResult r = worker.runOnce();                  // hem confirm (1.) hem compensate (2.)
        assertThat(r.done()).isEqualTo(2);
        assertThat(sagaStatus(first.sagaId())).isEqualTo(SagaStatus.CONFIRMED);
        assertThat(sagaStatus(second.sagaId())).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(quota.remaining(account)).isEqualTo(4);
        assertThat(quota.refunds.get()).isEqualTo(1);
        assertThat(flow.orderCount(account)).isEqualTo(1);
    }

    @Test // #6 ayni UUID farkli hesap: ayri saga'lar; #18 yanlis aktor: yetki reddi, kayit yok
    void sameKeyDifferentAccountIsSeparate_andWrongActorIsForbidden() {
        UUID key = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        quota.grant(other, 5);
        assertThat(flow.createOrder(account, key, UUID.randomUUID()).kind()).isEqualTo(Kind.OK);
        assertThat(flow.createOrder(other, key, UUID.randomUUID()).kind()).isEqualTo(Kind.OK);
        assertThat(store.countSagas()).isEqualTo(2);

        UUID k2 = UUID.randomUUID();
        assertThatThrownBy(() -> quota.consume("chat-service", account, k2, "ORDER_QUOTA", 1))
                .isInstanceOf(SagaParticipant.ParticipantForbiddenException.class);
        assertThat(quota.get("chat-service", account, k2)).isEmpty();
        assertThat(quota.remaining(account)).isEqualTo(4);
    }

    @Test // #7 begin sonrasi, consume oncesi cokme: deadline -> otomatik telafi (tombstone); #11 gec gelen consume uygulanmaz
    void crashAfterBeginIsCompensatedAfterDeadline_andLateConsumeHitsTombstone() {
        UUID key = UUID.randomUUID();
        flow.crashAt = CrashPoint.AFTER_BEGIN;
        assertThatThrownBy(() -> flow.createOrder(account, key, UUID.randomUUID())).isInstanceOf(OrderFlow.SimulatedCrash.class);
        UUID sagaId = store.find(account, "ORDER_CREATE", key).orElseThrow().id();
        assertThat(sagaStatus(sagaId)).isEqualTo(SagaStatus.STARTED);
        assertThat(worker.runOnce().claimed()).isZero();                    // deadline dolmadan dokunulmaz
        pastDeadline();
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(sagaStatus(sagaId)).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(opState(key)).isEqualTo(State.CANCELLED);                // tombstone
        // gec gelen consume (ornegin agda takilmis istek): uygulanmaz, hak dusmez
        assertThat(quota.consume("order-service", account, key, "ORDER_QUOTA", 1)).isEqualTo(State.CANCELLED);
        assertThat(quota.remaining(account)).isEqualTo(5);
        assertThat(quota.consumeApplied.get()).isZero();
        // istemci ayni key ile tekrar denerse: OPERATION_CANCELLED (yeni niyet = yeni key)
        flow.crashAt = CrashPoint.NONE;
        assertThat(flow.createOrder(account, key, UUID.randomUUID()).kind()).isEqualTo(Kind.CANCELLED);
    }

    @Test // #8 katilimci commit etti, yanit kayboldu: istek 503; retry IN_PROGRESS; deadline sonrasi GET/compensate -> iade
    void participantCommittedButResponseLost() {
        UUID key = UUID.randomUUID();
        participant.loseResponseNext("consume");
        Result r = flow.createOrder(account, key, UUID.randomUUID());
        assertThat(r.kind()).isEqualTo(Kind.UPSTREAM_UNAVAILABLE);
        assertThat(quota.remaining(account)).isEqualTo(4);                  // katilimcida uygulandi
        assertThat(flow.createOrder(account, key, UUID.randomUUID()).kind()).isEqualTo(Kind.IN_PROGRESS);
        pastDeadline();
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(opState(key)).isEqualTo(State.COMPENSATED);
        assertThat(quota.remaining(account)).isEqualTo(5);
        assertThat(flow.orderCount(account)).isZero();
        assertThat(flow.createOrder(account, key, UUID.randomUUID()).kind()).isEqualTo(Kind.CANCELLED);
    }

    @Test // #8b istek katilimciya hic ulasmadi (timeout): saga STARTED; recovery tombstone ile kapatir
    void participantUnreachableIsCompensatedWithTombstone() {
        UUID key = UUID.randomUUID();
        participant.failNext("consume");
        Result r = flow.createOrder(account, key, UUID.randomUUID());
        assertThat(r.kind()).isEqualTo(Kind.UPSTREAM_UNAVAILABLE);
        assertThat(quota.remaining(account)).isEqualTo(5);
        pastDeadline();
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(opState(key)).isEqualTo(State.CANCELLED);
    }

    @Test // #9 consume commit + domain rollback (cokme): recovery compensate -> iade
    void consumeCommittedThenDomainNeverWritten() {
        UUID key = UUID.randomUUID();
        flow.crashAt = CrashPoint.AFTER_CONSUME;
        assertThatThrownBy(() -> flow.createOrder(account, key, UUID.randomUUID())).isInstanceOf(OrderFlow.SimulatedCrash.class);
        assertThat(quota.remaining(account)).isEqualTo(4);
        assertThat(flow.orderCount(account)).isZero();
        pastDeadline();
        assertThat(worker.runOnce().done()).isEqualTo(1);
        UUID sagaId = store.find(account, "ORDER_CREATE", key).orElseThrow().id();
        assertThat(sagaStatus(sagaId)).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(quota.remaining(account)).isEqualTo(5);
        assertThat(quota.refunds.get()).isEqualTo(1);
    }

    @Test // #10 domain commit + confirm oncesi cokme; #20 restart: yeni instance'lar in-flight saga'yi kurtarir
    void domainCommittedThenCrashBeforeConfirm_recoveredAfterRestart() {
        Result r = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());
        assertThat(r.kind()).isEqualTo(Kind.OK);
        // "restart": eski nesneler atilir; yalniz DB'deki durum kalir
        LocalSagaStore freshStore = new LocalSagaStore(jdbc, tm, "order", props, clock);
        SagaRecoveryWorker freshWorker = new SagaRecoveryWorker(freshStore, new QuotaParticipant(jdbc, tx), "order-service", props, clock);
        assertThat(freshWorker.runOnce().done()).isEqualTo(1);
        assertThat(freshStore.findById(r.sagaId()).orElseThrow().status()).isEqualTo(SagaStatus.CONFIRMED);
        assertThat(opState(store.findById(r.sagaId()).orElseThrow().operationKey())).isEqualTo(State.CONFIRMED);
        assertThat(quota.remaining(account)).isEqualTo(4);
        assertThat(flow.orderCount(account)).isEqualTo(1);
    }

    @Test // #12 confirm timeout: retry + backoff; belirsizlikte GET (yanit kayboldu ama katilimci confirm etti -> retry yok)
    void confirmTimeoutIsRetried_andLostResponseIsResolvedByGet() {
        Result r = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());
        participant.failNext("confirm").failNext("get");                    // katilimci tamamen erisilemez
        SagaRecoveryWorker.RunResult first = worker.runOnce();
        assertThat(first.retried()).isEqualTo(1);
        Step s = step(r.sagaId());
        assertThat(s.status()).isEqualTo(StepStatus.RETRY);
        assertThat(s.attempt()).isEqualTo(1);
        assertThat(s.lastErrorCode()).isEqualTo("ParticipantUnavailableException");
        assertThat(s.nextAttemptAt()).isEqualTo(clock.instant().plusSeconds(2));   // 2^1
        assertThat(worker.runOnce().claimed()).isZero();                    // backoff dolmadan alinmaz
        clock.advance(Duration.ofSeconds(3));
        assertThat(worker.runOnce().done()).isEqualTo(1);
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.CONFIRMED);

        // yanit kayboldu: katilimci confirm etti; worker GET ile CONFIRMED gorur ve retry yapmadan tamamlar
        Result r2 = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());
        participant.loseResponseNext("confirm");
        SagaRecoveryWorker.RunResult res = worker.runOnce();
        assertThat(res.done()).isEqualTo(1);
        assertThat(res.retried()).isZero();
        assertThat(sagaStatus(r2.sagaId())).isEqualTo(SagaStatus.CONFIRMED);
    }

    @Test // #13 eszamanli confirm ve compensate (katilimci): tek sonuc; celiski MANUAL_REVIEW; cift iade yok
    void concurrentConfirmAndCompensateYieldSingleOutcome() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 10; i++) {
                UUID key = UUID.randomUUID();
                assertThat(quota.consume("order-service", account, key, "ORDER_QUOTA", 1)).isEqualTo(State.APPLIED);
                int before = quota.remaining(account);
                CyclicBarrier barrier = new CyclicBarrier(2);
                Future<Object> c = pool.submit(() -> { barrier.await(); try { return quota.confirm("order-service", account, key); } catch (RuntimeException e) { return e; } });
                Future<Object> k = pool.submit(() -> { barrier.await(); try { return quota.compensate("order-service", account, key); } catch (RuntimeException e) { return e; } });
                Object confirmRes = c.get(30, TimeUnit.SECONDS), compRes = k.get(30, TimeUnit.SECONDS);
                State finalState = opState(key);
                if (finalState == State.MANUAL_REVIEW) {                        // confirm once kazandi: iade yok, insan karari
                    assertThat(confirmRes).isEqualTo(State.CONFIRMED);
                    assertThat(compRes).isEqualTo(State.MANUAL_REVIEW);
                    assertThat(quota.remaining(account)).isEqualTo(before);
                } else {                                                        // compensate once kazandi: iade; confirm celiski
                    assertThat(finalState).isEqualTo(State.COMPENSATED);
                    assertThat(compRes).isEqualTo(State.COMPENSATED);
                    assertThat(confirmRes).isInstanceOf(SagaParticipant.ParticipantConflictException.class);
                    assertThat(quota.remaining(account)).isEqualTo(before + 1);
                }
                quota.grant(account, 5);                                        // sonraki tur icin sifirla
            }
        } finally { pool.shutdownNow(); }
        assertThat(quota.refunds.get()).isLessThanOrEqualTo(10);
    }

    @Test // #14 iki worker + expired lease: eski worker complete edemez
    void expiredLeaseIsTakenOverAndOldWorkerCannotComplete() {
        Result r = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());
        UUID tokenA = UUID.randomUUID();
        List<Step> byA = store.claimSteps(tokenA, clock.instant(), clock.instant().plusSeconds(props.leaseSeconds()), 10);
        assertThat(byA).hasSize(1);                                          // A claim etti ve "coktu"
        clock.advance(Duration.ofSeconds(props.leaseSeconds() - 1));
        assertThat(worker.runOnce().claimed()).isZero();
        clock.advance(Duration.ofSeconds(2));
        assertThat(worker.runOnce().done()).isEqualTo(1);                    // B devraldi
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.CONFIRMED);
        assertThat(store.complete(byA.get(0), tokenA, Action.CONFIRM)).isFalse();   // A geri geldi: token eslesmez
        assertThat(store.prepare(byA.get(0), tokenA)).isEmpty();
        assertThat(step(r.sagaId()).status()).isEqualTo(StepStatus.DONE);
    }

    @Test // #15a recovery once iptal etti: istek yolunun success() CAS'i kaybeder, domain yazimi rollback olur
    void recoveryCancelBeforeSuccessRollsBackDomainWrite() {
        UUID key = UUID.randomUUID();
        flow.beforeSuccess = () -> {                                          // domain TX acikken recovery BASKA thread'de kosar
            pastDeadline();
            ExecutorService other = Executors.newSingleThreadExecutor();
            try { assertThat(other.submit(worker::runOnce).get(30, TimeUnit.SECONDS).done()).isEqualTo(1); }
            catch (Exception e) { throw new IllegalStateException(e); }
            finally { other.shutdownNow(); }
        };
        Result r = flow.createOrder(account, key, UUID.randomUUID());
        assertThat(r.kind()).isEqualTo(Kind.CANCELLED);
        assertThat(flow.orderCount(account)).isZero();                        // rollback
        assertThat(sagaStatus(r.sagaId())).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(quota.remaining(account)).isEqualTo(5);                    // iade edildi
        assertThat(opState(key)).isEqualTo(State.COMPENSATED);
    }

    @Test // #15b gercek yaris: istek ile recovery paralel; her turda tutarli uc sonuctan biri, asla yarim durum
    void requestSuccessVersusRecoveryCancelRaceIsAlwaysConsistent() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        int confirmed = 0, compensated = 0;
        try {
            for (int i = 0; i < 20; i++) {
                UUID key = UUID.randomUUID();
                CountDownLatch begun = new CountDownLatch(1);
                // Iki tarafa da degisen gecikme: bazen istek, bazen recovery once davranir; her iki dal da gorulmeli
                final long requestJitter = (i % 4) * 3L, workerDelay = ((i + 2) % 5) * 4L;
                flow.beforeSuccess = () -> sleep(requestJitter);
                flow.afterBegin = () -> { pastDeadline(); begun.countDown(); };   // begin commit oldu; deadline gecmis sayilir; worker baslayabilir
                Future<Result> req = pool.submit(() -> flow.createOrder(account, key, UUID.randomUUID()));
                begun.await();
                Future<SagaRecoveryWorker.RunResult> rec = pool.submit(() -> { sleep(workerDelay); return worker.runOnce(); });
                Result r = req.get(30, TimeUnit.SECONDS);
                rec.get(30, TimeUnit.SECONDS);
                for (int n = 0; n < 3; n++) worker.runOnce();                   // kalan adimi kapat
                SagaStatus status = sagaStatus(r.sagaId());
                int orders = flow.orderCount(account), remaining = quota.remaining(account);
                if (status == SagaStatus.CONFIRMED) {
                    confirmed++;
                    assertThat(r.kind()).isEqualTo(Kind.OK);
                    assertThat(orders).isEqualTo(1);
                    assertThat(remaining).isEqualTo(4);
                } else {
                    compensated++;
                    assertThat(status).isEqualTo(SagaStatus.COMPENSATED);
                    assertThat(r.kind()).isIn(Kind.CANCELLED, Kind.REJECTED);
                    assertThat(orders).isZero();
                    assertThat(remaining).isEqualTo(5);
                }
                jdbc.getJdbcTemplate().execute("TRUNCATE \"order\".order_item");
                quota.grant(account, 5);
            }
        } finally { pool.shutdownNow(); flow.afterBegin = () -> {}; }
        assertThat(confirmed + compensated).isEqualTo(20);
        System.out.println("RACE_OUTCOMES confirmed=" + confirmed + " compensated=" + compensated);
    }

    @Test // #16 tekrarlanan compensate: idempotent, tek iade
    void repeatedCompensateRefundsOnce() {
        UUID key = UUID.randomUUID();
        assertThat(quota.consume("order-service", account, key, "ORDER_QUOTA", 1)).isEqualTo(State.APPLIED);
        assertThat(quota.compensate("order-service", account, key)).isEqualTo(State.COMPENSATED);
        assertThat(quota.compensate("order-service", account, key)).isEqualTo(State.COMPENSATED);
        assertThat(quota.remaining(account)).isEqualTo(5);
        assertThat(quota.refunds.get()).isEqualTo(1);
        // confirm sonrasi compensate: iade yok, MANUAL_REVIEW
        UUID k2 = UUID.randomUUID();
        quota.consume("order-service", account, k2, "ORDER_QUOTA", 1);
        quota.confirm("order-service", account, k2);
        assertThat(quota.compensate("order-service", account, k2)).isEqualTo(State.MANUAL_REVIEW);
        assertThat(quota.remaining(account)).isEqualTo(4);
    }

    @Test // #17 eksik/bozuk key: saga acilmaz (HTTP 400 binding'i MVC testinin isi; burada store seviyesi)
    void missingKeyOrScopeIsRejectedBeforeAnySideEffect() {
        assertThatThrownBy(() -> store.begin(account, "ORDER_CREATE", null, "quota")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> store.begin(account, " ", UUID.randomUUID(), "quota")).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.countSagas()).isZero();
        assertThatThrownBy(() -> store.success(UUID.randomUUID(), "x")).isInstanceOf(IllegalStateException.class);  // TX disinda success yok
    }

    @Test // #19 monitor: 15 dk'dan eski cozulmemis kayit sayilir; cleanup: terminal 30 gun sonra silinir, MANUAL_REVIEW asla
    void monitorCountsUnresolved_andCleanupKeepsManualReview() {
        Result ok = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID());     // -> CONFIRMED
        worker.runOnce();
        Result manual = flow.createOrder(account, UUID.randomUUID(), UUID.randomUUID()); // katilimci disaridan compensate edilmis -> confirm celiskisi
        quota.compensate("order-service", account, store.findById(manual.sagaId()).orElseThrow().operationKey());
        assertThat(worker.runOnce().manualReview()).isEqualTo(1);
        assertThat(sagaStatus(manual.sagaId())).isEqualTo(SagaStatus.MANUAL_REVIEW);
        flow.crashAt = CrashPoint.AFTER_BEGIN;
        UUID stuckKey = UUID.randomUUID();
        assertThatThrownBy(() -> flow.createOrder(account, stuckKey, UUID.randomUUID())).isInstanceOf(OrderFlow.SimulatedCrash.class);

        assertThat(store.countUnresolved()).isZero();                         // henuz 15 dk gecmedi
        clock.advance(Duration.ofMinutes(16));
        assertThat(store.countUnresolved()).isEqualTo(1);                     // yalniz STARTED; MANUAL_REVIEW cozulmus sayilir (insan)
        assertThat(store.cleanup()).isZero();                                 // 30 gun gecmedi
        clock.advance(Duration.ofDays(31));
        assertThat(store.cleanup()).isEqualTo(1);                             // yalniz CONFIRMED
        assertThat(store.findById(ok.sagaId())).isEmpty();
        assertThat(store.findById(manual.sagaId())).isPresent();
        assertThat(store.countSagas()).isEqualTo(2);
        assertThat(store.countSteps()).isEqualTo(2);                          // CASCADE
    }
}
```

---

## `skeleton-example/platform-messaging/src/test/java/com/acme/platform/messaging/saga/FlakyParticipant.java`

```java
package com.acme.platform.messaging.saga;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hata enjeksiyonu: gercek HTTP katmaninin uretecegi belirsizlikleri taklit eder.
 * FAIL: istek katilimciya hic ulasmadi (timeout/baglanti). LOSE_RESPONSE: katilimci commit etti, yanit kayboldu.
 */
public class FlakyParticipant implements SagaParticipant {

    enum Fault { FAIL, LOSE_RESPONSE }

    private final SagaParticipant delegate;
    private final Map<String, Deque<Fault>> faults = new ConcurrentHashMap<>();

    public FlakyParticipant(SagaParticipant delegate) { this.delegate = delegate; }

    public FlakyParticipant failNext(String method) { faults.computeIfAbsent(method, k -> new ArrayDeque<>()).add(Fault.FAIL); return this; }
    public FlakyParticipant loseResponseNext(String method) { faults.computeIfAbsent(method, k -> new ArrayDeque<>()).add(Fault.LOSE_RESPONSE); return this; }

    private <T> T call(String method, java.util.function.Supplier<T> real) {
        Deque<Fault> q = faults.get(method);
        Fault f = q == null ? null : q.poll();
        if (f == Fault.FAIL) throw new ParticipantUnavailableException(method + " timeout");
        T result = real.get();
        if (f == Fault.LOSE_RESPONSE) throw new ParticipantUnavailableException(method + " response lost");
        return result;
    }

    @Override public State consume(String c, UUID a, UUID k, String t, int n) { return call("consume", () -> delegate.consume(c, a, k, t, n)); }
    @Override public Optional<State> get(String c, UUID a, UUID k) { return call("get", () -> delegate.get(c, a, k)); }
    @Override public State confirm(String c, UUID a, UUID k) { return call("confirm", () -> delegate.confirm(c, a, k)); }
    @Override public State compensate(String c, UUID a, UUID k) { return call("compensate", () -> delegate.compensate(c, a, k)); }
}
```

---

## `skeleton-example/platform-messaging/src/test/java/com/acme/platform/messaging/saga/OrderFlow.java`

```java
package com.acme.platform.messaging.saga;

import com.acme.platform.messaging.saga.LocalSagaStore.BeginResult;
import com.acme.platform.messaging.saga.SagaParticipant.ParticipantUnavailableException;
import com.acme.platform.messaging.saga.SagaParticipant.State;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Koordinator tarafindaki istek akisi (referans Bolum 11.4 adim 1-3): begin (ayri TX) -> consume (TX disi) ->
 * domain yazimi + success() (ayni TX). Test icin "cokme noktalari" enjekte edilebilir.
 */
public class OrderFlow {

    public enum Kind { OK, REPLAY, IN_PROGRESS, CANCELLED, REJECTED, UPSTREAM_UNAVAILABLE, DOMAIN_CONFLICT }
    public record Result(Kind kind, String detail, UUID sagaId) {}
    public enum CrashPoint { NONE, AFTER_BEGIN, AFTER_CONSUME }
    public static class SimulatedCrash extends RuntimeException { SimulatedCrash(String p) { super("crash at " + p); } }

    static final String CALLER = "order-service", SCOPE = "ORDER_CREATE", STEP = "quota";
    public static final String DDL = """
            CREATE TABLE "order".order_item (id UUID PRIMARY KEY, account_id UUID NOT NULL, resource_id UUID NOT NULL UNIQUE,
                                             operation_key UUID NOT NULL);
            """;

    private final LocalSagaStore store;
    private final SagaParticipant participant;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public volatile CrashPoint crashAt = CrashPoint.NONE;
    public volatile Runnable beforeSuccess = () -> {};          // yaris testleri icin kanca
    public volatile Runnable afterBegin = () -> {};

    public OrderFlow(LocalSagaStore store, SagaParticipant participant, NamedParameterJdbcTemplate jdbc, TransactionTemplate tx) {
        this.store = store; this.participant = participant; this.jdbc = jdbc; this.tx = tx;
    }

    public Result createOrder(UUID account, UUID operationKey, UUID resourceId) {
        BeginResult b = store.begin(account, SCOPE, operationKey, STEP);
        UUID sagaId = b.saga().id();
        if (!b.created()) {
            return switch (b.saga().status()) {                                   // idempotent replay
                case SUCCEEDED, CONFIRMED -> new Result(Kind.REPLAY, b.saga().result(), sagaId);
                case COMPENSATED, MANUAL_REVIEW -> new Result(Kind.CANCELLED, "OPERATION_CANCELLED", sagaId);
                default -> new Result(Kind.IN_PROGRESS, "OPERATION_IN_PROGRESS", sagaId);
            };
        }
        afterBegin.run();
        if (crashAt == CrashPoint.AFTER_BEGIN) throw new SimulatedCrash("AFTER_BEGIN");

        State state;
        try {
            state = participant.consume(CALLER, account, operationKey, "ORDER_QUOTA", 1);   // TX disi, ayni operationKey
        } catch (ParticipantUnavailableException e) {
            return new Result(Kind.UPSTREAM_UNAVAILABLE, "UPSTREAM_UNAVAILABLE", sagaId);  // saga STARTED kalir; deadline -> recovery
        }
        if (state != State.APPLIED) {
            store.fail(sagaId);                                                   // REJECTED/CANCELLED: telafi (no-op/tombstone)
            return new Result(Kind.REJECTED, "QUOTA_" + state, sagaId);
        }
        if (crashAt == CrashPoint.AFTER_CONSUME) throw new SimulatedCrash("AFTER_CONSUME");

        try {
            return tx.execute(st -> {                                              // domain yazimi + success() AYNI TX
                UUID id = UUID.randomUUID();
                jdbc.update("INSERT INTO \"order\".order_item (id, account_id, resource_id, operation_key) VALUES (:id, :a, :r, :k)",
                        Map.of("id", id, "a", account, "r", resourceId, "k", operationKey));
                beforeSuccess.run();
                store.success(sagaId, "ORDER:" + id);                              // CAS; recovery iptal ettiyse exception -> rollback
                return new Result(Kind.OK, "ORDER:" + id, sagaId);
            });
        } catch (SagaCancelledException e) {
            return new Result(Kind.CANCELLED, "OPERATION_CANCELLED", sagaId);
        } catch (DuplicateKeyException e) {
            store.fail(sagaId);                                                   // domain uniqueness reddetti -> telafi
            return new Result(Kind.DOMAIN_CONFLICT, "RESOURCE_ALREADY_ORDERED", sagaId);
        }
    }

    public int orderCount(UUID account) {
        return jdbc.queryForObject("SELECT count(*) FROM \"order\".order_item WHERE account_id = :a", Map.of("a", account), Integer.class);
    }
}
```

---

## `skeleton-example/platform-messaging/src/test/java/com/acme/platform/messaging/saga/QuotaParticipant.java`

```java
package com.acme.platform.messaging.saga;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Katilimci ornegi (referans Bolum 11.4 "Katilimci sozlesmesi"): abonelik/kota servisinin kendi semasinda.
 * Gercekte HTTP arkasindadir; burada in-process. Tekillik UNIQUE(caller_service, account_id, operation_key);
 * caller_service JWT act claim'inden gelir (burada parametre). Iki katmanli yetki: allowlist (HTTP filtresi, burada yok)
 * + kod ici aktor -> izinli islem tipi haritasi. Kilit: advisory lock (islem anahtari) + FOR UPDATE (kota satiri).
 */
public class QuotaParticipant implements SagaParticipant {

    static final Map<String, Set<String>> ALLOWED_OPERATIONS = Map.of("order-service", Set.of("ORDER_QUOTA"));

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;
    public final AtomicInteger consumeApplied = new AtomicInteger(), refunds = new AtomicInteger();

    public QuotaParticipant(NamedParameterJdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    public static final String DDL = """
            CREATE TABLE subscription.quota (account_id UUID PRIMARY KEY, remaining INT NOT NULL CHECK (remaining >= 0));
            CREATE TABLE subscription.operation (
              caller_service TEXT NOT NULL, account_id UUID NOT NULL, operation_key UUID NOT NULL,
              op_type TEXT NOT NULL, status TEXT NOT NULL, amount INT NOT NULL,
              refunded_at TIMESTAMPTZ,
              PRIMARY KEY (caller_service, account_id, operation_key));
            """;

    public void grant(UUID accountId, int remaining) {
        jdbc.update("INSERT INTO subscription.quota (account_id, remaining) VALUES (:a, :r) ON CONFLICT (account_id) DO UPDATE SET remaining = :r",
                new MapSqlParameterSource().addValue("a", accountId).addValue("r", remaining));
    }

    public int remaining(UUID accountId) {
        return jdbc.queryForObject("SELECT remaining FROM subscription.quota WHERE account_id = :a", Map.of("a", accountId), Integer.class);
    }

    @Override
    public State consume(String caller, UUID account, UUID key, String opType, int amount) {
        if (!ALLOWED_OPERATIONS.getOrDefault(caller, Set.of()).contains(opType)) {
            throw new ParticipantForbiddenException("actor not allowed for operation type");   // kod ici aktor -> islem tipi
        }
        return tx.execute(st -> {
            lock(caller, account, key);
            State existing = status(caller, account, key);
            if (existing != null) return existing;                     // replay (CANCELLED tombstone dahil: uygulanmaz)
            Integer remaining = DataAccessUtils.singleResult(jdbc.queryForList(
                    "SELECT remaining FROM subscription.quota WHERE account_id = :a FOR UPDATE", Map.of("a", account), Integer.class));
            State result;
            if (remaining != null && remaining >= amount) {
                jdbc.update("UPDATE subscription.quota SET remaining = remaining - :n WHERE account_id = :a",
                        Map.of("n", amount, "a", account));
                result = State.APPLIED;
                consumeApplied.incrementAndGet();
            } else {
                result = State.REJECTED;
            }
            insert(caller, account, key, opType, result, amount);
            return result;
        });
    }

    @Override
    public Optional<State> get(String caller, UUID account, UUID key) { return Optional.ofNullable(status(caller, account, key)); }

    @Override
    public State confirm(String caller, UUID account, UUID key) {
        return tx.execute(st -> {
            lock(caller, account, key);
            State s = status(caller, account, key);
            if (s == null) throw new ParticipantConflictException("confirm without consume");
            return switch (s) {
                case APPLIED -> { setStatus(caller, account, key, State.CONFIRMED); yield State.CONFIRMED; }
                case CONFIRMED -> State.CONFIRMED;                                       // replay
                default -> throw new ParticipantConflictException("confirm on " + s);   // CANCELLED/COMPENSATED/REJECTED
            };
        });
    }

    @Override
    public State compensate(String caller, UUID account, UUID key) {
        return tx.execute(st -> {
            lock(caller, account, key);
            State s = status(caller, account, key);
            if (s == null) {                                                            // gec gelen consume uygulanmasin
                insert(caller, account, key, "TOMBSTONE", State.CANCELLED, 0);
                return State.CANCELLED;
            }
            return switch (s) {
                case APPLIED -> {                                                       // iade; cift iade refunded_at ile engellenir
                    int n = jdbc.update("""
                            UPDATE subscription.operation SET status = 'COMPENSATED', refunded_at = now()
                            WHERE caller_service = :c AND account_id = :a AND operation_key = :k AND refunded_at IS NULL""",
                            Map.of("c", caller, "a", account, "k", key));
                    if (n == 1) {
                        Integer amount = jdbc.queryForObject("SELECT amount FROM subscription.operation WHERE caller_service = :c AND account_id = :a AND operation_key = :k",
                                Map.of("c", caller, "a", account, "k", key), Integer.class);
                        jdbc.update("UPDATE subscription.quota SET remaining = remaining + :n WHERE account_id = :a", Map.of("n", amount, "a", account));
                        refunds.incrementAndGet();
                    }
                    yield State.COMPENSATED;
                }
                case COMPENSATED, CANCELLED, REJECTED -> s == State.REJECTED ? State.CANCELLED : s;   // replay / no-op
                case CONFIRMED -> { setStatus(caller, account, key, State.MANUAL_REVIEW); yield State.MANUAL_REVIEW; }  // iade yok
                case MANUAL_REVIEW -> State.MANUAL_REVIEW;
            };
        });
    }

    private void lock(String caller, UUID account, UUID key) {
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtext(:k))", Map.of("k", caller + ":" + account + ":" + key), Object.class);
    }

    private State status(String caller, UUID account, UUID key) {
        String s = DataAccessUtils.singleResult(jdbc.queryForList(
                "SELECT status FROM subscription.operation WHERE caller_service = :c AND account_id = :a AND operation_key = :k",
                Map.of("c", caller, "a", account, "k", key), String.class));
        return s == null ? null : State.valueOf(s);
    }

    private void insert(String caller, UUID account, UUID key, String opType, State status, int amount) {
        jdbc.update("INSERT INTO subscription.operation (caller_service, account_id, operation_key, op_type, status, amount) VALUES (:c, :a, :k, :t, :s, :n)",
                new MapSqlParameterSource().addValue("c", caller).addValue("a", account).addValue("k", key)
                        .addValue("t", opType).addValue("s", status.name()).addValue("n", amount));
    }

    private void setStatus(String caller, UUID account, UUID key, State status) {
        jdbc.update("UPDATE subscription.operation SET status = :s WHERE caller_service = :c AND account_id = :a AND operation_key = :k",
                Map.of("s", status.name(), "c", caller, "a", account, "k", key));
    }
}
```

---

## `skeleton-example/pom.xml`

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
  <modules><module>platform-core</module><module>platform-messaging</module><module>order-api</module><module>order-core</module></modules>
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
          <bannedDependencies><excludes><exclude>com.acme:*-core</exclude></excludes><includes><include>com.acme:platform-*</include></includes></bannedDependencies>
        </rules></configuration></execution></executions></plugin>
      <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.5.3</version>
        <!-- *IT siniflari da surefire ile kosar (ayri failsafe fazi bu iskelette gereksiz). "0 test = basarisiz" kurali
             test iceren modullerin kendi pom'unda (failIfNoTests); testsiz kontrat modullerinde build kirilmaz. -->
        <configuration><includes><include>**/*Test.java</include><include>**/*IT.java</include></includes></configuration></plugin>
    </plugins>
  </build>
</project>
```
