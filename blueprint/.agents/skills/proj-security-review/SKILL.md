---
name: proj-security-review
description: Use this skill when reviewing authentication, authorization, service JWT, input validation, sensitive logging, uploads, rate limiting, DB queries, WebSocket, privacy, secrets or supply-chain aspects of a change.
---

Değişikliği `docs/ai/security-rules.md` (tek kaynak) ve OWASP ASVS/API Security Top 10 ile karşılaştır. Her bulgu için **exploit senaryosu** yaz; senaryo yazamıyorsan bulgu değil, öneridir. Kanıt yoksa `needs verification`. Sorun yoksa: **"Bu kapsamda güvenlik bulgusu yok."**

Kontrol et:

## Trust boundary ve kimlik
- Üç JWT yüzeyi karışmıyor: `typ`, anahtar ve `aud` ayrı. Service JWT asimetrik; `iss` = imzalayan; `kid`/JWKS ile doğrulama; simetrik paylaşılan secret **yok** (varsa `BLOCK`).
- Filtre sırası: path decode+normalize → imza/iss/aud/exp → (opsiyonel jti) → allowlist first-match → default-deny. Allowlist ham URI ile eşleşmiyor.
- `internal-access` kuralı gerçek kullanım kadar dar; dar kural catch-all'dan önce; local yml ve deploy config **birlikte** güncellenmiş.
- Kullanıcı adına internal uç `sub` == path hesabı kontrolü yapıyor; hesabı body'den alan internal uç hiçbir aktöre açık değil.
- Gateway: `/internal` engeli `StripPrefix` sonrası da; iç header temizliği; CORS `*` yok; token query'de yok; `gateway` actuator ucu kapalı; trusted-proxy ayarı; rate limit ve timeout bütçesi.

## Ownership / IDOR
- Hesap kimliği yalnız `@CurrentAccount`; path/query/body'den değil. Path'teki id hedef kaynak; ownership serviste doğrulanıyor.
- Liste/sayfalama uçlarında sıralama alanı allowlist; boyut sınırı.
- Read-model'den yetki kararı veriliyorsa "satır yok / eski" davranışı fail-closed.

## OTP, token, abuse
- OTP: `SecureRandom`, tuzlu hash, sabit zamanlı karşılaştırma, TTL + deneme sınırı, enumeration koruması; outbox payload'ında düz metin OTP kalmıyor.
- SMS pumping savunması `otp/start` için: geo-permissions, numara doğrulama, numara/IP/cihaz tavanı, velocity, harcama kesicisi.
- Refresh token: opak + hash, aile rotasyonu, reuse → aile iptali, mutlak süre.
- `sv` artışı: şifre sıfırlama, logout-all, **ban**, rol değişimi.
- Admin 2FA passkey/TOTP; SMS yalnız kurtarma. Admin OTP'si kullanıcı OTP'sinden zayıf değil.
- Rate limit: yeni saldırıya açık uç scope aldı; sayaç Redis'te; fail politikası scope tablosuyla uyumlu; `Retry-After`.
- Mock entegrasyon (`MockStoreValidation`, `MockSms`…) `@Profile("local|test")` ve prod açılış guard'ı var (yoksa `BLOCK`).
- Cihaz attestation sinyali gereken uçlarda (kayıt, OTP-gönder, satın alma) risk skoruna bağlanmış.

## Input ve sorgu güvenliği
- `@Valid`, boyut sınırları, enum parse (`from`), path/header/param adları açık.
- Native SQL'de parametre bağlama; dinamik sıralama/filtre allowlist; JPQL string birleştirme yok.
- Dosya adı/path kullanıcıdan gelmiyor; `..` normalize.
- JSON: bilinmeyen alan politikası bilinçli; polimorfik deserialization kapalı.

## WebSocket / realtime
- CONNECT'te native `Authorization` zorunlu; süresi dolmuş token ile komut reddi; SUBSCRIBE deny-all allowlist; `sv` süpürücüsü; Redis zarfı HMAC + tazelik + `relayId`; local'de bile imzasız zarf kabul edilmiyor.

## Object storage ve içerik
- Yükleme private quarantine → yeniden kodlama (EXIF/GPS) → moderasyon (CSAM hash + NSFW) → private delivery + signed GET. Public bucket'a doğrudan yükleme `BLOCK`.
- Presigned URL kısa TTL; key sunucuda üretilmiş; HEAD/ETag doğrulaması.
- Uçtan uca şifreleme varsa: iddia ile şema uyumlu (FS/MITM sınırları yazılı), anahtar pinleme, message franking; sohbet anahtarı panele verilmiyor.

## Log, hata, secret
- `security-rules.md` Bölüm 4 yasak listesi: hiçbir ham PII/secret/body/exception mesajı log'da yok. Log'a özel neden `safeLogReason`'da; `details`'te yalnız istemciye gösterilebilir bilgi.
- Yeni secret: değeri hiçbir yerde yok; `/run/secrets` ile geliyor; `${ENV:literal}` fallback yok; SOPS dosyasına eklenmiş; `.dockerignore` kapsıyor.
- Hata yanıtları tek format (`ErrorResponse`); filtre redleri de aynı; altyapı hatası 503 (500 değil).
- Audit: `details` allowlist; anahtar/şifreli içerik/serbest metin/ham IP yok; tablo değiştirilemez.

## Privacy
- Yeni kişisel veri alanı: envanter + aydınlatma + retention + silme saga'sı kapsamı güncellendi.
- Düşük entropili kimlik için HMAC+pepper; varlık oracle'ı yok; konum deterministik grid; kişisel veri dönen uç `no-store`.
- Arama index'i/cache/yedek aynı kurallara uyuyor.

## Tedarik zinciri
- Yeni bağımlılık: lisans (GPL/AGPL/BSL) ve bilinen CVE kontrolü; sürüm OSS destekli.
- Dockerfile non-root, `.dockerignore`, image CI'da build.

Çıktı:
1. **Risk seviyesi:** `CRITICAL` / `HIGH` / `MEDIUM` / `LOW` / `OK`.
2. **Bulgular** (her biri): `severity · dosya:satır · kanıt · exploit senaryosu (kim, nasıl, sonuç) · düzeltme · doğrulayan test`.
3. **Kural referansı:** her bulgu için `security-rules.md` bölümü.
4. **Net kanıt bulunamayan alanlar.**
5. **Nihai karar:** `APPROVE` / `APPROVE WITH NON-BLOCKING COMMENTS` / `REQUEST CHANGES` / `BLOCK`. `CRITICAL` veya `HIGH` bulgu varsa karar `APPROVE` olamaz.
