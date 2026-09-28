# operation-consistency.md — Servisler Arası Tutarlılık Standardı

> Tek kaynak. `proj-operation-consistency-review` ve `proj-event-design-review` buraya link verir. Referans: mimari doküman Bölüm 11–12.

## 1. Mekanizma seçimi (en basit yeterli olan)

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

## 2. Idempotency

- Public tekrar-güvenli işlemler `X-Idempotency-Key` (UUID) alır. Aynı niyetin retry'ları aynı key; yeni niyet yeni key.
- Tekillik `(account_id, scope, operation_key)`. Aynı key farklı body → ilk istek kazanır (fingerprint tutulmaz, belgelenir).
- Tamamlanmış istek aynı sonucu döner; süren `OPERATION_IN_PROGRESS`; iptal edilmiş `OPERATION_CANCELLED`.
- Veri seviyesinde: `INSERT … ON CONFLICT DO NOTHING`, deterministik id (`UUID.nameUUIDFromBytes(kaynak:hedef:faz)`) + unique.

## 3. Outbox (tek generic tablo: `outbox_event`)

- Yazıcı `Propagation.MANDATORY`; domain TX'i olmadan outbox yazılamaz.
- Poller `platform-messaging`'den: CTE + `FOR UPDATE SKIP LOCKED` + `locked_until` lease + `claim_token`; uzak iş TX **dışında**; sonuç yalnız claim sahibi tarafından yazılır.
- Sabitler: batch 50, lease 120 sn, güvenlik payı 30 sn, backoff `min(600, 30·2^n)`, STUCK alarmı her 10 denemede (ERROR + `outbox_oldest_pending_age_seconds` metriği).
- DEAD politikası iş türüne göre: güvenlik yan etkisi (ban, engel) **asla DEAD olmaz**; TTL'li mesaj (OTP) `expires_at` sonrası DEAD; kalıcı 4xx (401/403/408/429 hariç) DEAD.
- Payload'da gereksiz PII/secret yok; iletim sonrası hassas alan NULL.
- Aynı `aggregate_id` için sıra korunacaksa tek worker/single-active-consumer.
- Sıra numaralı durum senkronu: karar DB sequence'ından sıra alır; alıcı eski sırayı yok sayar (`applied=false` ile başarı döner).
- Superseded kontrolü: göndermeden önce daha yeni karar varsa satır gönderilmeden silinir.

## 4. Event (CloudEvents) ve tüketici

- `id` (UUIDv7), `source` (servis), `type` (`<servis>.<aggregate>.<olay>`), `subject` (aggregate id), `time`, `dataschema`, `traceparent`.
- Tüketici: inbox `ON CONFLICT (event_id) DO NOTHING`; bilinmeyen `type` **yok sayılır** (komutlarda DLQ); read-model `revision` ile UPSERT (eski olay yeni satırı ezmez).
- Şema evrimi: alan ekleme uyumlu; silme/yeniden adlandırma/tip değişimi → yeni `type`, bir süre çift yayın. Tüketici önce deploy.
- Read-model kaynak değildir; dışa açılmaz; eskime eşiği ve "satır yok" davranışı yazılıdır; rebuild yolu belgelidir.

## 5. Local saga (tek adım)

- Koordinatör tabloları koordinatörün şemasında: `saga` (`UNIQUE(account_id, scope, operation_key)`), `saga_steps` (`next_action CONFIRM|COMPENSATE`, `lock_token`, `locked_until`).
- Akış: `begin()` ayrı TX'te (`ON CONFLICT DO NOTHING`; yeni kayıt → step `COMPENSATE/PENDING`, `next_attempt_at = now + deadline 15 sn`; varsa replay) → katılımcıya `consume` (TX dışı, aynı `operationKey`, circuit breaker altında) → domain yazımı + `success()` **aynı local TX'te**, ayrı bean (`success` compare-and-set; recovery iptal ettiyse rollback) → recovery worker: `claim` (SKIP LOCKED + `lock_token` + lease 60 sn) → `prepare` (`FOR UPDATE`) → uzak `confirm`/`compensate` → `complete` (token eşleşmesi) → hata/belirsiz sonuçta `GET` ile durum sorgusu; çelişki → `MANUAL_REVIEW`.
- `monitor` (60 sn): 15 dk'dan eski çözülmemiş → ERROR + metrik. `cleanup`: terminal kayıtlar 30 gün sonra; `MANUAL_REVIEW` silinmez.
- Katılımcı: `UNIQUE(caller_service, account_id, operation_key)`; `caller_service` JWT `act`/`iss`'ten; iki katmanlı yetki (allowlist + aktör → izinli işlem tipi); advisory lock + `FOR UPDATE`; tablo: consume/confirm/compensate sonuçları (`APPLIED/REJECTED/CONFIRMED/CANCELLED tombstone/COMPENSATED/MANUAL_REVIEW`); çift iade `original_id` ile engellenir.
- Merkezi coordinator servisi kurulmaz; çok adımlı ihtiyaç doğarsa `LocalSagaStore` genelleştirilir (step adı parametre) — ADR ile.
- Zaman kaynağı DB `now()`.

## 6. Zorunlu doğrulama matrisi

Kanıt seviyeleri: (1) unit + MVC, (2) gerçek PostgreSQL (Testcontainers), (3) owner→participant runtime, (4) release. Sonuç `PASS/FAIL/BLOCKED`.

Senaryolar: normal başarı ve replay · aynı key farklı body · eşzamanlı aynı key · farklı key aynı kaynak · aynı UUID farklı hesap/aktör · intent sonrası çökme · katılımcı commit + yanıt kaybı · consume commit + domain rollback · domain commit + confirm öncesi çökme · geç consume vs tombstone · confirm/compensate timeout · eşzamanlı confirm ve compensate · iki worker + expired lease · request success vs recovery cancel yarışı · tekrarlanan compensate · eksik/bozuk key · geçersiz JWT / yanlış aktör · cleanup ve monitor · migration ve restart · outbox satırı domain TX ile rollback · tüketici duplicate olay · sıra bozuk olay · bilinmeyen tip.

## 7. Bu Belgede Özellikle Taşınmayanlar

Somut servis adları ve operasyon tipleri (`repo-context.md`'de).

## 8. Net Kanıt Bulunamayan Alanlar

- (ajan ekler)
