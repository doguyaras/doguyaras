# Blueprint — Yeni Proje İskeleti (AI Yönetişimi, Skill'ler, Hook'lar, Makine Kuralları)

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
    └── platform-messaging/           # Generic outbox/inbox (JDBC) + OutboxBehaviourIT: gerçek PostgreSQL üzerinde 13 davranışsal senaryo
```

## Doğrulama kapsamı (dürüst sınır — referans Bölüm 19.6)

| Seviye | Ne | Durum |
|---|---|---|
| **Yapısal** (kural derlenir, ihlal yakalanır) | `scripts/flyway-immutability.js` (12 test); hook'lar (11 senaryo: damga yok / damga var / içerik değişti / commit sonrası damga geçerli / ignore edilen dosya / eski biçim / git yok); `tests/*.java` + enforcer (`skeleton-example` içinde `mvn test`, pozitif + 8 kasıtlı ihlal) | **Doğrulandı** (2026-09-29) |
| **Davranışsal** (sistem koşarken tutarlılık güvenceleri) | outbox tekrar teslimi çift iş üretmez, iki worker aynı satırı işlemez, süreç ölünce kira devri, inbox atomikliği, üretici sıralaması, lane izolasyonu, backoff/DEAD, eski karar yeni kararı ezmez | **Outbox/inbox: doğrulandı** (2026-09-29, seviye 2) — `skeleton-example/platform-messaging/OutboxBehaviourIT`, gerçek PostgreSQL 17.5 (gömülü, Docker'sız), 13 senaryo (#21, #22, #25, #27, #28, #29, #32 + 6), 5 kasıtlı regresyon yakalandı. **Koşturulmadı:** saga recovery (seviye 2), owner→participant runtime ve broker ile yeniden teslim (seviye 3) — projede P0 çıkış koşulu |
| **Skill'ler** | 12 skill metni | Gerçek bir PR üzerinde Claude Code oturumunda henüz koşturulmadı; ilk kullanımda karar formatlarının uyumu gözden geçirilir |

Yapısal `PASS` davranışsal `PASS` değildir; uyum raporu ve PR şablonu ikisini ayrı yazar.

## Kurulum

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

## İlkeler

1. **Tek kaynak:** Her kural bir dosyada yaşar; diğerleri anchor link ile yönlendirir. Skill'ler `docs/ai/*`'ı tekrar etmez.
2. **Kanıt zorunluluğu:** Doğrulanamayan şey "**net kanıt bulunamadı**" diye yazılır; uydurulmaz.
3. **Kural → makine:** Her kuralın bir makine kontrolü vardır (ArchUnit, enforcer, hook, CI script, test). Skill'ler "ne yapmalı"yı, testler "yapıldı mı"yı taşır.
4. **Kural sınıfları:** her kural zorunlu güvence / varsayılan tercih / başlangıç ayarı sınıfındadır (referans Bölüm 1.4); skill'ler sayıyı güvence gibi, güvenceyi tercih gibi ele almaz.
5. **Sabit karar formatları:** Her skill'in çıktısı sabit enum'larla biter (`APPROVE / REQUEST CHANGES / BLOCK`, `PASS / FAIL / BLOCKED`); serbest metin karar sayılmaz.
6. **Skill'ler kısa ve test edilebilir:** Her madde bir dosyaya bakarak evet/hayır denebilecek biçimde yazılır.
