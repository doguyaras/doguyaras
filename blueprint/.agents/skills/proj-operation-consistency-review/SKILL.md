---
name: proj-operation-consistency-review
description: Use this skill for any cross-service write, outbox, inbox, idempotency or saga work — to assess whether a saga is needed, to implement it correctly, and to verify it against the mandatory scenario matrix.
---

Servisler arası tutarlılığı `docs/ai/operation-consistency.md` (tek kaynak) ve mimari referans Bölüm 11'e göre üç fazda ele al. Kapsam: push edilmemiş her değişiklik. Mevcut altyapı sınırı: `LocalSagaStore` **tek adımlı, tek katılımcı**; merkezi coordinator yok. Çok adımlı ihtiyaç → `extension required` + ADR.

## Faz 1 — Assessment (`references/assessment.md`)
- İhtiyacı sınıflandır: yalnız okuma / tek local TX / duplicate koruması / commit sonrası tepki (event) / dış iş emri (komut) / local commit + uzak geri alınabilir mutation (saga) / geri alınamaz-global-insan onayı (saga uygun değil).
- "En basit yeterli mekanizma" seçilmiş mi? Saga, event yeterliyken kullanılıyorsa fazla; event, saga gerekirken kullanılıyorsa eksik.
- Sıcak yol etkisi: saga consume çağrısı kritik akış kaydında gerekçeli mi; bütçe içinde mi (Bölüm 1.2)?
- Karar: `saga unnecessary` / `existing saga suitable` / `extension required` / `decision blocked` (+ neden).

## Faz 2 — Implementation (`references/implementation.md`, 12 adım)
- Idempotency key: `X-Idempotency-Key` UUID, `(account_id, scope, operation_key)` tekilliği, replay/IN_PROGRESS/CANCELLED davranışı.
- `begin()` ayrı TX; consume TX dışında ve circuit breaker altında; domain + `success()` aynı TX'te ayrı bean; compare-and-set.
- Katılımcı: `UNIQUE(caller_service, account_id, operation_key)`, tombstone, advisory lock + `FOR UPDATE`, aktör→işlem tipi haritası, iade kuralları, `original_id` ile çift iade engeli.
- Recovery worker: claim (SKIP LOCKED + `lock_token` + lease), prepare (`FOR UPDATE`), confirm/compensate, complete (token), belirsizlikte GET, çelişkide `MANUAL_REVIEW`; monitor + cleanup; `MANUAL_REVIEW` silinmez.
- Outbox kullanımı: `outbox_event` (generic), `kind` doğru, yazıcı MANDATORY, handler idempotent, DEAD politikası iş türüne göre, hassas alan iletim sonrası NULL.
- Inbox/read-model: inbox satırı `(handler, event_id)` + iş **aynı TX**, ack commit sonrası; kaynak başına `source_revision`; olay sözleşmesi (tam durum/değişiklik); bilinmeyen `type` yok sayılır.
- Outbox lane izolasyonu ve üretici tarafı sıralama; `claim_token` ≠ uzak idempotency; publisher confirm ≠ işlendi.
- Loglar: `sagaId`/`operationKey` ile, hesap kimliği yok; metrikler: `saga_unresolved_total`, `outbox_oldest_pending_age_seconds`.
- Config: `operation-consistency.*` key'leri local + deploy.

## Faz 3 — Verification (`references/verification.md`)
- Yapısal kontroller (statik) + senaryo matrisi (operation-consistency.md Bölüm 6) + kanıt seviyesi (1 unit/MVC, 2 gerçek PG, 3 owner→participant runtime, 4 release).
- Her senaryo için: test adı / dosya / kanıt seviyesi / **commit SHA / ortam / sonuç linki / tarih** (kanıt kaydı, `operation-consistency.md` Bölüm 9). Testi olmayan senaryo `FAIL` değil `BLOCKED` (kanıt yok) sayılır ve listelenir. Yapısal kontrol (`ArchUnit` yeşil) davranışsal senaryo için kanıt değildir.

Çıktı:
1. **Uygunluk kararı:** `saga unnecessary` / `existing saga suitable` / `extension required` / `decision blocked` + gerekçe.
2. **Implementasyon bulguları:** `adım · dosya:satır · kanıt · düzeltme`.
3. **Doğrulama tablosu:** senaryo → test → kanıt seviyesi → `PASS/FAIL/BLOCKED`.
4. **Doğrulama durumu:** `PASS` / `FAIL` / `BLOCKED`.
5. **Nihai karar:** `APPROVE` / `REQUEST CHANGES` / `BLOCK`.
