---
name: proj-event-design-review
description: Use this skill when adding or changing a domain event, a command, an outbox handler, a consumer, a read-model projection, an event schema, or RabbitMQ topology (exchange/queue/binding/DLQ).
---

Olay tasarımını `docs/ai/operation-consistency.md` Bölüm 3–4 ve mimari referans Bölüm 12'ye göre incele. Temel sorular: **Bu bir komut mu, olay mı? Sahibi kim? Yarın üçüncü bir tüketici geldiğinde üretici değişmeden çalışır mı?** Sorun yoksa: **"Bu kapsamda event tasarım bulgusu yok."**

Kontrol et:

## Komut / olay ayrımı
- "Başka servisin verisini değiştir" niyetli mesaj **komut değil olay** olmalı; üretici tüketiciyi bilmiyor. Outbox `kind` doğru (`EVENT`/`COMMAND`/`HTTP`).
- Komut yalnız dış sağlayıcı iş emri (SMS, push, mail, webhook).
- "Outbox üzerinden RPC" (üretici hedefin ucunu/adresini biliyor) → `REQUEST CHANGES`.

## İsim ve envelope
- `type` = `<servis>.<aggregate>.<olay>` (geçmiş zaman: `created`, `cancelled`, `changed`); routing key aynı.
- CloudEvents attribute'ları: `id` (UUIDv7), `source`, `specversion`, `type`, `subject` (aggregate id), `time`, `dataschema`, `traceparent`. AMQP 0-9-1 header eşlemesi `platform-messaging`'den.
- Payload sınıfı `<domain>-api/event`; `@NoArgsConstructor`; kopya yok. Payload **gerçeği** taşır (id'ler, durum, revision), tüketiciye "ne yapması gerektiğini" değil.
- Payload'da PII/secret/şifreli içerik yok (mesaj olayı yalnız metadata).
- `revision`/sıra numarası var ve **kapsamı** yazılı (aggregate başına / kaynak geneli); kaynaklar arası karşılaştırılmıyor.
- **Olay sözleşmesi** yazılı: **tam durum** (snapshot; küçük revizyon atlanabilir) mi **değişiklik** (delta; hiçbir olay atlanamaz, sıra boşluğunda uygulama durur + uzlaştırma + alarm) mi. Delta olayı için boşluk tespiti (`source_seq` monoton) ve rebuild/`since` yolu var.

## Şema evrimi
- Değişiklik geriye uyumlu mu (yalnız opsiyonel alan ekleme)? Kırıcıysa yeni `type`/versiyon + çift yayın planı + sunset.
- Tüketici bilinmeyen alanı yok sayıyor; bilinmeyen `type` olaylarda **ack + log** (komutlarda DLQ).
- **Rollout sözleşmesi** (referans Bölüm 18.4) değişiklik türüne göre PR'da: opsiyonel alan (sıra serbest) / yeni `type` (tüketici önce + çift yayın + sunset) / yeni tüketici (kuyruk+binding önce, rebuild). Kırıcıysa 4 hücreli uyumluluk matrisi (yeni→eski, eski→yeni, yeni→yeni, eski→eski); "çalışmaz" hücresi çift yayın/flag ile kapatılmış. Image rollback'in veriyi geri almadığı not edilmiş.

## Topoloji (RabbitMQ 4.x)
- Olay: `domain.events` topic exchange; tüketici başına queue (`<tüketici>.<amaç>.queue`, quorum) + DLQ; binding pattern dar (`order.order.*`, `#` yok).
- Komut: `<servis>.commands` direct → `<hedef>.<komut>.queue` + DLQ.
- Queue quorum; `delivery-limit`; `dead-letter-strategy: at-least-once`; gecikmeli retry native (`x-delayed-retry-*`), delayed-exchange plugin **yok**.
- Sıra gereksinimi varsa single-active-consumer veya aggregate bazlı tek worker.
- Replay gerekiyorsa stream kopyası (`domain.events.stream`) ve retention.

## Üretici
- Yayın yalnız outbox'tan (domain TX'i içinde satır); `convertAndSend` doğrudan yok.
- Publisher confirm + mandatory; NACK/unroutable → outbox retry.
- Aynı olay birden çok tabloya/outbox'a yazılmıyor (tek satır, çok tüketici).
- **Üretici tarafı sıralama:** sıra gereken aggregate için claim aynı aggregate'i tek worker'a sırayla veriyor; başarısız satırın ardılları bekletiliyor. Sıra gerekmiyorsa bu kısıt yok (throughput).
- **Lane izolasyonu:** olay lane'i komut/HTTP lane'inden ayrı claim döngüsü ve havuzda; toplu yayın (kampanya) kritik tek satırları bekletmiyor; `priority` doğru.
- Publisher confirm "tüketici işledi" sayılmıyor; tamamlanma bilgisi gerekiyorsa tüketicinin olayı tüketiliyor.
- Eski bir karar (superseded) yeniden denemede yeni kararı ezemiyor.

## Tüketici ve read-model
- `defaultRequeueRejected=false`; prefetch/concurrency açık; kalıcı hata → DLQ; geçici → stateful retry + backoff.
- **Inbox atomikliği (zorunlu güvence):** `inbox_event(handler, event_id)` satırı ve iş değişikliği **aynı TX'te**; 0 satır → çık; ack **commit'ten sonra** (manual ack; AUTO ack `BLOCK`). Dedup kapsamı handler. Dış yan etki inbox TX'inde değil, aynı TX'te outbox satırı.
- Read-model: tüketicinin şemasında; **kaynak başına** projeksiyon ve `source_revision` (tek `revision` kolonlu birleşik tablo `REQUEST CHANGES`); `rm_consumer_position` ile tazelik (satır yaşından değil); karar başına kabul edilen eskilik T ve aşılınca davranış (fail-closed varsayılan) yazılı; "satır yok" davranışı yazılı; dışa açılmıyor; rebuild yolu (stream replay / export ucu) belgeli; replay deterministik; `readmodel_lag_seconds{source}` ve `readmodel_gap_total` metrikleri.
- Tüketici kendi transaction'ında yazıyor; başka servise senkron çağrı yapmıyor.

## Gözlem ve test
- Metrik/alarm: DLQ derinliği, `outbox_oldest_pending_age_seconds`, tüketici lag.
- Testler: outbox satırı TX ile rollback; tüketici duplicate (tek etki); handler ortasında exception → inbox satırı yok; sıra bozuk olay; delta'da sıra boşluğu → dur + alarm; bilinmeyen tip; şema uyumluluğu (eski payload yeni tüketicide, yeni payload eski tüketicide); iki poller + sıralı satırlar; lane izolasyonu. Her `PASS` için kanıt kaydı (commit, komut, sonuç — `operation-consistency.md` Bölüm 9).
- Analytics sink bu olayı alıyor mu (Bölüm 14.4)?

Çıktı:
1. **Sınıflandırma:** komut / olay / HTTP — doğru mu.
2. **Olay tablosu:** `type · üretici · tüketiciler · routing key · queue · DLQ · replay (evet/hayır) · revision alanı`.
3. **Bulgular:** `severity · dosya:satır/config · kanıt · düzeltme`.
4. **Şema evrimi ve rollout notu:** uyumlu / kırıcı + plan; değişiklik türüne göre sıra (Bölüm 18.4) ve kırıcıysa uyumluluk matrisi.
5. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`.
