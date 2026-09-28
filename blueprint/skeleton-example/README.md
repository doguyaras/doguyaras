# skeleton-example — Doğrulanmış Boş İskelet

`blueprint/tests/*.java` şablonlarının ve enforcer kuralının **gerçekten derlenip çalıştığı** en küçük Maven multi-module projesi. Referans dokümanın (Bölüm 3, 4, 7, 16, 19.5) somut, çalışan karşılığı.

- Spring Boot **4.1.1** BOM, Java 21 (25 ile de uyumlu), ArchUnit 1.5.1, Maven 3.9.11.
- Modüller: `platform-core` (ErrorCode arayüzü, ServiceException), `order-api` (DTO), `order-core` (controller/service/impl/repository/entity/exception/config + testler).
- Config: `application-local.yml`, `config/order.yml`, `deploy/prod.env.example` (drift testi için).

## Doğrulama sonucu (2026-09-28)

```
mvn -q -B -ntp test   → EXIT 0
ArchitectureRulesTest   8 test  (katman, controller→repository, impl paketi, config/, core→core, döngü, @Valid, api→entity)
ConfigDriftTest         3 test  (key kümeleri, ${ENV} ↔ env şablonu, secret fallback)
ErrorCodeUniquenessTest 1 test  (global tekillik, blok, mesaj formatı)
```

**Negatif doğrulama:** kasıtlı 8 ihlal enjekte edildi (controller→repository, `@Valid`'siz `@RequestBody`, `service/` altında `@Configuration`, `service.impl`'de Impl olmayan sınıf, çakışan + blok dışı + noktasız ErrorCode, local'de olup deploy'da olmayan rate-limit scope'u, `${SECRET_DB_PASSWORD:changeme}` fallback'i) → **8 failure**, hepsi doğru kuralda yakalandı; kaldırılınca yeniden yeşil.

## Denemede öğrenilen 4 ders (şablonlara işlendi)

1. **Enforcer `bannedDependencies`:** `com.acme:*-core` deseni `platform-core`'u da yakalar. Çözüm: `<includes><include>com.acme:platform-core</include></includes>` — ya da platform modüllerini `-core` ile bitirmemek.
2. **ArchUnit `@ArchTest` + JUnit engine:** Spring Boot 4.1 BOM'un yönettiği JUnit Platform ile ArchUnit'in kendi engine'i **0 test** çalıştırdı; build yeşil göründü ama hiçbir kural kontrol edilmedi. Kurallar düz `@Test` + `rule.check(classes)` olarak yazıldı; engine bağımlılığı yok.
3. **`Properties.stringPropertyNames()` tuzağı:** `YamlPropertiesFactoryBean` sayısal değerleri (`limit: 60`) Integer koyar; `stringPropertyNames()` bu key'leri **sessizce atlar** → drift testi rate-limit scope'larını hiç görmedi. `keySet()` + `String.valueOf` kullanılır.
4. **`layeredArchitecture().withOptionalLayers(true)`:** henüz `readmodel/` veya `outbox/` paketi olmayan yeni serviste "Layer is empty" ihlali üretmemesi için.

Bu dört ders "yeşil build = kural çalışıyor" varsayımının yanlış olabileceğini gösterdi; bu yüzden `proj-release-readiness-review` ve CI, mimari testlerin **test sayısını** da doğrular (0 test = başarısız).

## Çalıştırma

```bash
cd blueprint/skeleton-example
mvn -q -B -ntp test
```
