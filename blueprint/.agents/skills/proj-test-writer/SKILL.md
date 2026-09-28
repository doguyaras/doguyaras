---
name: proj-test-writer
description: Use this skill when writing or reviewing unit, HTTP binding, security access, repository/concurrency, contract, resilience, log-privacy or regression tests for a change.
---

Testleri mimari referans Bölüm 16 ve `docs/ai/security-rules.md` Bölüm 4'e göre yaz/incele. Önce **davranış ve edge case listesi**, sonra test. Anlamsız test (yalnız mock'un çağrıldığını doğrulayan) yazılmaz. Çıktıda eklenen case'ler, kalan boşluklar ve çalıştırma komutları listelenir.

Kontrol et / yaz:

## Kapsam kuralları
- Negatif case'ler ve **yetki hataları** zorunlu (izinsiz aktör 403, kimliksiz 400/401, başka kullanıcının kaynağı 404/403).
- Her bug için regression testi.
- Poller, worker ve check-then-act kodu için **concurrency** testi: iki paralel claim ayrık satır; eşzamanlı ikinci insert reddedilir; expired lease'te eski worker complete edemez (gerçek PG).
- Kırılgan test yok: sıra, saat (`Instant now()` metodu üzerinden verilir), rastgelelik, dış servis bağımlılığı yok.
- Test isimlendirmesi tek stil (`metot_whenKoşul_beklenenSonuç`); yorumlar ekibin dilinde.

## Türler
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

## Komutlar
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
