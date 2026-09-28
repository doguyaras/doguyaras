# Ne değişti

<!-- 2–5 madde. Neden yapıldığı; ne yapıldığı değil. -->

# Etki

- [ ] Contract (api modülü, OpenAPI diff): değişti / değişmedi — breaking: evet / hayır
- [ ] Migration: var / yok — `node scripts/flyway-immutability.js check --base origin/<hedef>` ✅
- [ ] Config/env/secret yüzeyi: `env_file` · `config/<svc>.yml` · `application-local.yml` · Dockerfile — güncellendi / etkilenmedi
- [ ] Yeni uzak senkron çağrı: var / yok — varsa sıcak yol tablosu güncellendi (`docs/ai/repo-context.md`)
- [ ] Yeni event/komut: var / yok — varsa tüketici önce deploy edilecek
- [ ] İstemciyi etkiliyor: evet / hayır — `docs/<client>-<feature>-integration-vN.md`: `…`
- [ ] Güvenlik/privacy etkisi: var / yok — özet: …

# Çalıştırılan review skill'leri ve kararları

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

# Doğrulama

<!-- Nasıl doğrulandı: hangi testler, izole DB'de ayağa kaldırıldı mı, yük testi (gerekiyorsa). -->

# Net kanıt bulunamayan alanlar

<!-- Doğrulanamayan varsayımlar; boşsa "yok". -->
