---
name: proj-api-contract-review
description: Use this skill when reviewing API contracts — DTO placement, api/core dependency direction, backward compatibility for mobile/panel/internal callers, HTTP parameter binding, OpenAPI diff and versioning.
---

Contract değişikliğini mimari referans Bölüm 3.3, 5.3–5.4, 6 ve 20'ye göre incele. OpenAPI diff çıktısı varsa önce onu oku. Sorun yoksa: **"Bu kapsamda contract bulgusu yok."**

Kontrol et:

## Yerleşim ve bağımlılık yönü
- Servisler arası DTO/enum/event payload'ı hedefin `<domain>-api` modülünde (`com.<org>.<domain>.api.*`); core'da kopyası yok.
- `*-api` hiçbir `*-core`'a bağımlı değil; `*-core` başka `*-core`'a bağımlı değil (enforcer).
- Entity dışarı açılmıyor; `toResponse` ile DTO.
- Servisler arası okunan DTO'da `@NoArgsConstructor` (Jackson).
- Event payload'ı `api/event` altında; CloudEvents attribute'ları platform'dan.

## Geriye uyumluluk
- Alan silme/yeniden adlandırma/tip değişimi/zorunlu alan ekleme = **breaking**. Etkilenen çağıranlar: mobil (mağazadaki eski sürüm aylarca yaşar), panel, internal client'lar, event tüketicileri.
- Breaking ise: yeni versiyon (`/v2` veya yeni `type`), eski versiyon sunset tarihiyle yaşıyor, ADR var.
- Enum'a yeni değer: tüketiciler bilinmeyen değeri tolere ediyor (`from()` fallback); **tüketici önce deploy**.
- Yanıt zarfı tutarlı (hata her zaman zarflı; başarı için tek karar); 200 ile `success=false` yok.
- Hata kodları servisin bloğunda ve global tekil; istemci kod→mesaj tablosu güncellendi (contract testi).
- Sayfalama: boyut sınırı, sıralama allowlist, keyset alanları tutarlı.

## HTTP parameter binding
- `@PathVariable("ad")`, `@RequestParam("ad")`, `@RequestHeader("Ad")` **açık isimli**; parameter-name inference'a güvenilmiyor.
- `required`/`defaultValue` bilinçli; opsiyonel parametre `Optional` veya default.
- Binding gerçek MVC üzerinden test edilmiş (`MockMvc standaloneSetup`; geçersiz tip 400 ve servise ulaşmıyor).
- Idempotent uçlar `X-Idempotency-Key` alıyor; UUID tipiyle bağlanmış.
- Kimlik `@CurrentAccount`; path/body'de hesap kimliği yok.

## Path ve isim
- Kaynak çoğul kebab-case; fiiller yalnız durum geçişi alt kaynaklarında (`/{id}/cancel`); güncelleme `PUT/PATCH`; versiyon prefix'i var.
- Internal uç `/internal/<kaynak-çoğul>/...`; public/internal karışmıyor.

## OpenAPI ve istemci
- OpenAPI diff: breaking değişiklik işaretli mi; istemci client generate ediliyor mu; `docs/<client>-<feature>-integration-vN.md` gerekli mi (→ `proj-client-integration-doc`).
- Örnek değerler ve açıklamalar üretilen OpenAPI'de anlamlı (springdoc anotasyonları).

Çıktı:
1. **Contract risk seviyesi:** `HIGH` / `MEDIUM` / `LOW` / `OK`.
2. **Breaking-change riski:** evet/hayır + gerekçe; etkilenen çağıranlar (mobil / panel / internal / event tüketicileri).
3. **Yerleşim/bağımlılık bulguları:** `dosya:satır · kanıt · düzeltme`.
4. **Binding bulguları:** `dosya:satır · kanıt · düzeltme · eksik test`.
5. **Daha güvenli yapı önerisi** (breaking ise versiyonlama/çift yayın planı).
6. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`.
