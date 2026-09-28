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
- `scripts/flyway-immutability.js`
- `scripts/flyway-immutability.test.js`
- `tests/ArchitectureRulesTest.java`
- `tests/ErrorCodeUniquenessTest.java`
- `tests/ConfigDriftTest.java`
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

### Kurulum

```bash
cp -r blueprint/. <yeni-repo>/
cd <yeni-repo>
grep -rl "proj-\|<proje>" . --exclude-dir=.git | xargs sed -i 's/proj-/<proje>-/g; s/<proje>/<proje-adı>/g'
ln -s ../.agents/skills .claude/skills          # kopya değil, symlink
chmod +x .claude/hooks/review-gate.sh
node --test scripts/flyway-immutability.test.js  # script'in kendi testleri
bash -n .claude/hooks/review-gate.sh && echo '{"tool_input":{"command":"git push"}}' | .claude/hooks/review-gate.sh   # hook kuru çalıştırma
```

### İlkeler

1. **Tek kaynak:** Her kural bir dosyada yaşar; diğerleri anchor link ile yönlendirir. Skill'ler `docs/ai/*`'ı tekrar etmez.
2. **Kanıt zorunluluğu:** Doğrulanamayan şey "**net kanıt bulunamadı**" diye yazılır; uydurulmaz.
3. **Kural → makine:** Her kuralın bir makine kontrolü vardır (ArchUnit, enforcer, hook, CI script, test). Skill'ler "ne yapmalı"yı, testler "yapıldı mı"yı taşır.
4. **Sabit karar formatları:** Her skill'in çıktısı sabit enum'larla biter (`APPROVE / REQUEST CHANGES / BLOCK`, `PASS / FAIL / BLOCKED`); serbest metin karar sayılmaz.
5. **Skill'ler kısa ve test edilebilir:** Her madde bir dosyaya bakarak evet/hayır denebilecek biçimde yazılır.

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
- Her değişiklikte ownership kontrolü: kimlik her zaman doğrulanmış bağlamdan (`@CurrentAccount`), path/body'den değil.
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

- Kullanıcıya latency olarak yansıyan bir istekte **en fazla bir** uzak senkron çağrı olur, o da yazma/rezervasyon türünden. Okuma amaçlı senkron çağrı eklemek yasaktır; read-model veya JWT claim kullan.
- Yeni bir uzak çağrı ekliyorsan `docs/ai/repo-context.md`'deki sıcak yol tablosunu güncelle ve `proj-resilience-review` skill'ini çalıştır.
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
- Yeni event/komut için `proj-event-design-review` çalıştırılır; tüketici önce deploy edilir.

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
- [ ] Yeni uzak senkron çağrı: var / yok — varsa sıcak yol tablosu güncellendi (`docs/ai/repo-context.md`)
- [ ] Yeni event/komut: var / yok — varsa tüketici önce deploy edilecek
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

<!-- Nasıl doğrulandı: hangi testler, izole DB'de ayağa kaldırıldı mı, yük testi (gerekiyorsa). -->

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

### 3. Sıcak yol tablosu (referans Bölüm 1.2)

| Akış | Uç | Uzak senkron çağrı | Read-model / claim ile karşılanan kontroller | Hedef p99 |
|---|---|---|---|---|
| Ana yazma | `POST /v1/orders` | 1 (hak tüketimi) | hesap durumu (`account_standing` read-model), yasal onay (`legal_ok` claim) | 300 ms |
| Mesaj gönder | `POST /v1/conversations/{id}/messages` | 0 | engel (`block_relation` read-model), üyelik (local) | 150 ms |
| Giriş | `POST /v1/auth/password/login` | 0 | – | 500 ms |

Bu tabloya 1'den fazla uzak çağrı yazılamaz; yazılmak isteniyorsa ADR + `proj-resilience-review`.

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

- Servis başına DB rolü; şema sahibi rol; başka şemaya USAGE yok. Cross-schema erişim hatası "GRANT ekleyerek" çözülmez.
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

- [ ] Yeni uzak senkron çağrı sıcak yola eklendi mi? Eklendiyse read-model/claim alternatifi değerlendirildi ve `repo-context.md` tablosu güncellendi.
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
- Aynı `aggregate_id` için sıra korunacaksa tek worker/single-active-consumer.
- Sıra numaralı durum senkronu: karar DB sequence'ından sıra alır; alıcı eski sırayı yok sayar (`applied=false` ile başarı döner).
- Superseded kontrolü: göndermeden önce daha yeni karar varsa satır gönderilmeden silinir.

### 4. Event (CloudEvents) ve tüketici

- `id` (UUIDv7), `source` (servis), `type` (`<servis>.<aggregate>.<olay>`), `subject` (aggregate id), `time`, `dataschema`, `traceparent`.
- Tüketici: inbox `ON CONFLICT (event_id) DO NOTHING`; bilinmeyen `type` **yok sayılır** (komutlarda DLQ); read-model `revision` ile UPSERT (eski olay yeni satırı ezmez).
- Şema evrimi: alan ekleme uyumlu; silme/yeniden adlandırma/tip değişimi → yeni `type`, bir süre çift yayın. Tüketici önce deploy.
- Read-model kaynak değildir; dışa açılmaz; eskime eşiği ve "satır yok" davranışı yazılıdır; rebuild yolu belgelidir.

### 5. Local saga (tek adım)

- Koordinatör tabloları koordinatörün şemasında: `saga` (`UNIQUE(account_id, scope, operation_key)`), `saga_steps` (`next_action CONFIRM|COMPENSATE`, `lock_token`, `locked_until`).
- Akış: `begin()` ayrı TX'te (`ON CONFLICT DO NOTHING`; yeni kayıt → step `COMPENSATE/PENDING`, `next_attempt_at = now + deadline 15 sn`; varsa replay) → katılımcıya `consume` (TX dışı, aynı `operationKey`, circuit breaker altında) → domain yazımı + `success()` **aynı local TX'te**, ayrı bean (`success` compare-and-set; recovery iptal ettiyse rollback) → recovery worker: `claim` (SKIP LOCKED + `lock_token` + lease 60 sn) → `prepare` (`FOR UPDATE`) → uzak `confirm`/`compensate` → `complete` (token eşleşmesi) → hata/belirsiz sonuçta `GET` ile durum sorgusu; çelişki → `MANUAL_REVIEW`.
- `monitor` (60 sn): 15 dk'dan eski çözülmemiş → ERROR + metrik. `cleanup`: terminal kayıtlar 30 gün sonra; `MANUAL_REVIEW` silinmez.
- Katılımcı: `UNIQUE(caller_service, account_id, operation_key)`; `caller_service` JWT `act`/`iss`'ten; iki katmanlı yetki (allowlist + aktör → izinli işlem tipi); advisory lock + `FOR UPDATE`; tablo: consume/confirm/compensate sonuçları (`APPLIED/REJECTED/CONFIRMED/CANCELLED tombstone/COMPENSATED/MANUAL_REVIEW`); çift iade `original_id` ile engellenir.
- Merkezi coordinator servisi kurulmaz; çok adımlı ihtiyaç doğarsa `LocalSagaStore` genelleştirilir (step adı parametre) — ADR ile.
- Zaman kaynağı DB `now()`.

### 6. Zorunlu doğrulama matrisi

Kanıt seviyeleri: (1) unit + MVC, (2) gerçek PostgreSQL (Testcontainers), (3) owner→participant runtime, (4) release. Sonuç `PASS/FAIL/BLOCKED`.

Senaryolar: normal başarı ve replay · aynı key farklı body · eşzamanlı aynı key · farklı key aynı kaynak · aynı UUID farklı hesap/aktör · intent sonrası çökme · katılımcı commit + yanıt kaybı · consume commit + domain rollback · domain commit + confirm öncesi çökme · geç consume vs tombstone · confirm/compensate timeout · eşzamanlı confirm ve compensate · iki worker + expired lease · request success vs recovery cancel yarışı · tekrarlanan compensate · eksik/bozuk key · geçersiz JWT / yanlış aktör · cleanup ve monitor · migration ve restart · outbox satırı domain TX ile rollback · tüketici duplicate olay · sıra bozuk olay · bilinmeyen tip.

### 7. Bu Belgede Özellikle Taşınmayanlar

Somut servis adları ve operasyon tipleri (`repo-context.md`'de).

### 8. Net Kanıt Bulunamayan Alanlar

- (ajan ekler)

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
- Migration servisin **kendi rolüyle** koşuyor; `GRANT` başka şemaya erişim açmıyor (varsa `BLOCKER`).

### Modül sahipliği
- Dosyada yalnız kendi şeması; tam nitelikli adlar; başka şema adı geçmiyor (test de bunu kontrol eder).
- Cross-schema FK yok; başka servisin kimliği düz UUID.
- Read-model tablosu tüketicinin kendi şemasında ve `revision` kolonu var.

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
- `revision`/sıra numarası var (tüketici eski olayı yeni satıra yazmasın).

### Şema evrimi
- Değişiklik geriye uyumlu mu (yalnız opsiyonel alan ekleme)? Kırıcıysa yeni `type`/versiyon + çift yayın planı + sunset.
- Tüketici bilinmeyen alanı yok sayıyor; bilinmeyen `type` olaylarda **ack + log** (komutlarda DLQ).
- Tüketici önce deploy; PR açıklamasında sıra yazılı.

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

### Tüketici ve read-model
- `defaultRequeueRejected=false`; prefetch/concurrency açık; kalıcı hata → DLQ; geçici → stateful retry + backoff.
- Inbox dedup `ON CONFLICT (event_id) DO NOTHING`; handler idempotent.
- Read-model: tüketicinin şemasında; `UPSERT … WHERE excluded.revision > current.revision`; eskime eşiği + "satır yok" davranışı yazılı; dışa açılmıyor; rebuild yolu (stream replay / export ucu) belgeli; `readmodel_lag_seconds` metriği.
- Tüketici kendi transaction'ında yazıyor; başka servise senkron çağrı yapmıyor.

### Gözlem ve test
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

## `.agents/skills/proj-operation-consistency-review/SKILL.md`

---
name: proj-operation-consistency-review
description: Use this skill for any cross-service write, outbox, inbox, idempotency or saga work — to assess whether a saga is needed, to implement it correctly, and to verify it against the mandatory scenario matrix.
---

Servisler arası tutarlılığı `docs/ai/operation-consistency.md` (tek kaynak) ve mimari referans Bölüm 11'e göre üç fazda ele al. Kapsam: push edilmemiş her değişiklik. Mevcut altyapı sınırı: `LocalSagaStore` **tek adımlı, tek katılımcı**; merkezi coordinator yok. Çok adımlı ihtiyaç → `extension required` + ADR.

### Faz 1 — Assessment (`references/assessment.md`)
- İhtiyacı sınıflandır: yalnız okuma / tek local TX / duplicate koruması / commit sonrası tepki (event) / dış iş emri (komut) / local commit + uzak geri alınabilir mutation (saga) / geri alınamaz-global-insan onayı (saga uygun değil).
- "En basit yeterli mekanizma" seçilmiş mi? Saga, event yeterliyken kullanılıyorsa fazla; event, saga gerekirken kullanılıyorsa eksik.
- Sıcak yol etkisi: saga consume çağrısı tek uzak çağrı mı (Bölüm 1.2)?
- Karar: `saga unnecessary` / `existing saga suitable` / `extension required` / `decision blocked` (+ neden).

### Faz 2 — Implementation (`references/implementation.md`, 12 adım)
- Idempotency key: `X-Idempotency-Key` UUID, `(account_id, scope, operation_key)` tekilliği, replay/IN_PROGRESS/CANCELLED davranışı.
- `begin()` ayrı TX; consume TX dışında ve circuit breaker altında; domain + `success()` aynı TX'te ayrı bean; compare-and-set.
- Katılımcı: `UNIQUE(caller_service, account_id, operation_key)`, tombstone, advisory lock + `FOR UPDATE`, aktör→işlem tipi haritası, iade kuralları, `original_id` ile çift iade engeli.
- Recovery worker: claim (SKIP LOCKED + `lock_token` + lease), prepare (`FOR UPDATE`), confirm/compensate, complete (token), belirsizlikte GET, çelişkide `MANUAL_REVIEW`; monitor + cleanup; `MANUAL_REVIEW` silinmez.
- Outbox kullanımı: `outbox_event` (generic), `kind` doğru, yazıcı MANDATORY, handler idempotent, DEAD politikası iş türüne göre, hassas alan iletim sonrası NULL.
- Inbox/read-model: `ON CONFLICT (event_id) DO NOTHING`; `revision` ile UPSERT; bilinmeyen `type` yok sayılır.
- Loglar: `sagaId`/`operationKey` ile, hesap kimliği yok; metrikler: `saga_unresolved_total`, `outbox_oldest_pending_age_seconds`.
- Config: `operation-consistency.*` key'leri local + deploy.

### Faz 3 — Verification (`references/verification.md`)
- Yapısal kontroller (statik) + senaryo matrisi (operation-consistency.md Bölüm 6) + kanıt seviyesi (1 unit/MVC, 2 gerçek PG, 3 owner→participant runtime, 4 release).
- Her senaryo için: test adı / dosya / kanıt seviyesi / sonuç. Testi olmayan senaryo `FAIL` değil `BLOCKED` (kanıt yok) sayılır ve listelenir.

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
   - Evet → uzak çağrı sayısı 1'i geçemez; consume dışındaki kontroller read-model/claim ile.
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

### 3. Sonuç

- Tüm satırlar `PASS` → **PASS**.
- Herhangi bir satır `FAIL` → **FAIL** (liste).
- Test/kanıt olmayan satır → **BLOCKED** (liste; "test yok" = geçmiş sayılmaz).

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
- Rollout sırası (sahip → tüketici → auth → gateway) release notunda; tüketici-önce kuralı yeni event/enum için sağlanmış.
- Rollback: önceki digest listesi mevcut; migration expand/contract'a uygun (eski image yeni şemayla çalışır); prova edilmiş rollback süresi.
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

### Sıcak yol
- Değişen akış sıcak yolda mı (`docs/ai/repo-context.md` Bölüm 3)? Uzak senkron çağrı sayısı **≤ 1** mi? Artıyorsa `REQUEST CHANGES`: read-model / JWT claim / asenkron alternatif ve maliyeti yazılır; tablo güncellenir.
- Tek uzak çağrı yazma/rezervasyon türü mü (okuma değil)?
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
- Read-model "satır yok/eski" davranışı tanımlı.

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
3. **Sıcak yol sayısı:** önce/sonra; tablo güncellendi mi.
4. **Eksik testler/alarmlar.**
5. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`. Sıcak yolda ikinci senkron okuma → en az `REQUEST CHANGES`.

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
- Gateway: `/internal` engeli `StripPrefix` sonrası da; iç header temizliği; CORS `*` yok; token query'de yok; `gateway` actuator ucu kapalı; trusted-proxy ayarı; rate limit ve timeout bütçesi.

### Ownership / IDOR
- Hesap kimliği yalnız `@CurrentAccount`; path/query/body'den değil. Path'teki id hedef kaynak; ownership serviste doğrulanıyor.
- Liste/sayfalama uçlarında sıralama alanı allowlist; boyut sınırı.
- Read-model'den yetki kararı veriliyorsa "satır yok / eski" davranışı fail-closed.

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

## `.claude/hooks/review-stamp.sh`

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

## `skeleton-example/README.md`

## skeleton-example — Doğrulanmış Boş İskelet

`blueprint/tests/*.java` şablonlarının ve enforcer kuralının **gerçekten derlenip çalıştığı** en küçük Maven multi-module projesi. Referans dokümanın (Bölüm 3, 4, 7, 16, 19.5) somut, çalışan karşılığı.

- Spring Boot **4.1.1** BOM, Java 21 (25 ile de uyumlu), ArchUnit 1.5.1, Maven 3.9.11.
- Modüller: `platform-core` (ErrorCode arayüzü, ServiceException), `order-api` (DTO), `order-core` (controller/service/impl/repository/entity/exception/config + testler).
- Config: `application-local.yml`, `config/order.yml`, `deploy/prod.env.example` (drift testi için).

### Doğrulama sonucu (2026-09-28)

```
mvn -q -B -ntp test   → EXIT 0
ArchitectureRulesTest   8 test  (katman, controller→repository, impl paketi, config/, core→core, döngü, @Valid, api→entity)
ConfigDriftTest         3 test  (key kümeleri, ${ENV} ↔ env şablonu, secret fallback)
ErrorCodeUniquenessTest 1 test  (global tekillik, blok, mesaj formatı)
```

**Negatif doğrulama:** kasıtlı 8 ihlal enjekte edildi (controller→repository, `@Valid`'siz `@RequestBody`, `service/` altında `@Configuration`, `service.impl`'de Impl olmayan sınıf, çakışan + blok dışı + noktasız ErrorCode, local'de olup deploy'da olmayan rate-limit scope'u, `${SECRET_DB_PASSWORD:changeme}` fallback'i) → **8 failure**, hepsi doğru kuralda yakalandı; kaldırılınca yeniden yeşil.

### Denemede öğrenilen 4 ders (şablonlara işlendi)

1. **Enforcer `bannedDependencies`:** `com.acme:*-core` deseni `platform-core`'u da yakalar. Çözüm: `<includes><include>com.acme:platform-core</include></includes>` — ya da platform modüllerini `-core` ile bitirmemek.
2. **ArchUnit `@ArchTest` + JUnit engine:** Spring Boot 4.1 BOM'un yönettiği JUnit Platform ile ArchUnit'in kendi engine'i **0 test** çalıştırdı; build yeşil göründü ama hiçbir kural kontrol edilmedi. Kurallar düz `@Test` + `rule.check(classes)` olarak yazıldı; engine bağımlılığı yok.
3. **`Properties.stringPropertyNames()` tuzağı:** `YamlPropertiesFactoryBean` sayısal değerleri (`limit: 60`) Integer koyar; `stringPropertyNames()` bu key'leri **sessizce atlar** → drift testi rate-limit scope'larını hiç görmedi. `keySet()` + `String.valueOf` kullanılır.
4. **`layeredArchitecture().withOptionalLayers(true)`:** henüz `readmodel/` veya `outbox/` paketi olmayan yeni serviste "Layer is empty" ihlali üretmemesi için.

Bu dört ders "yeşil build = kural çalışıyor" varsayımının yanlış olabileceğini gösterdi; bu yüzden `proj-release-readiness-review` ve CI, mimari testlerin **test sayısını** da doğrular (0 test = başarısız).

### Çalıştırma

```bash
cd blueprint/skeleton-example
mvn -q -B -ntp test
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
