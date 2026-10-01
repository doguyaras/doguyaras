# Gemini İyileştirme Raporuna Yanıt

Rapordaki her öneri dokümanın mevcut haliyle karşılaştırıldı. Mümkün olanlar çalıştırılarak doğrulandı: JDK 25.0.4, pg_partman 5.0.1 kaynağı, ArchUnit 1.5.1 ile iskelette derleme ve test. Kabul edilen maddeler `docs/mikroservis-mimari-referans.md` dosyasına işlendi. İskeletin tam build'i 305 testle yeşil.

Özet: 19 önerinin 3'ü olduğu gibi, 8'i düzeltilerek veya kısmen kabul edildi, 8'i reddedildi. Reddedilenlerin her birinde ya ölçülmüş bir karşı kanıt ya da çalışmayan kod var. Reddedilen iki maddenin (pg_partman, DLQ) amacı doğru bulundu ve dokümana doğru yöntemiyle eklendi.

## 1. JVM ve işletim sistemi

**ulimits (Kısmen katılıyorum).** Doküman `ulimits` anahtarını zaten listeliyordu ama değer vermiyordu. `nofile: 65536` eklendi. Gerekçe ise rapordakinden farklı: JVM açılışta soft limiti kendiliğinden hard limite yükseltir. Soft 1024 verilen süreçte JVM içinden 20000 görüldü (`MaxFDLimit=true`). Asıl belirleyici olan hard limit, bu da ayrıca not edildi. Virtual thread'ler dosya tanımlayıcısı açmaz; soket sayısını havuzlar sınırlar.

**Generational ZGC'yi zorunlu kılmak (Katılmıyorum).** ZGC daha fazla bellek ve CPU ister. Küçük heap'li, I/O ağırlıklı servislerde G1 yeterlidir. Doğru kural: ölçülmüş GC duraklaması p99 hedefini aşıyorsa ve heap birkaç GB'tan büyükse ZGC.

**Önerilen ENTRYPOINT (Katılmıyorum, bir hatası var).** `-XX:+ZGenerational` JDK 25'te şu uyarıyla yok sayılıyor: "Ignoring option ZGenerational; support was removed in 24.0". JDK 24'ten beri ZGC yalnız generational çalışıyor, bayrak gereksiz.

**Raporun kaçırdığı, ölçümle bulunan gerçek risk.** JVM, 2'den az CPU'lu veya yaklaşık 1792 MB'tan az bellekli container'da G1 yerine **Serial GC** seçiyor. Ölçüm: 1 CPU / 1 GB'ta `UseSerialGC`, 2 CPU / 2 GB'ta `UseG1GC`. `cpus: 1` ile koşan servis sessizce tek thread'li, uygulamayı durduran GC'ye düşüyor. Dokümana `-XX:+UseG1GC` eklendi.

**DNS TTL (Kısmen katılıyorum).** JDK varsayılanı başarılı çözümde zaten 30 sn, başarısızda 10 sn (ölçüldü). Kısaltma ancak daha hızlı failover gerekiyorsa anlamlı. Mevcut havuz bağlantıları DNS'e hiç bakmaz; onları Hikari `maxLifetime` yeniler. Not olarak eklendi, zorunlu yapılmadı.

## 2. PgBouncer ve JDBC

**`prepareThreshold=0&preparedStatementCacheQueries=0` (Katılmıyorum).** Bu, PgBouncer 1.21 öncesinin geçici çözümü. 1.21+ ile `max_prepared_statements` açıkça verilince prepared statement'lar transaction mode'da çalışıyor. Bunu gerçek PgBouncer 1.22 ile test ettik. `08P01` ve "başka istemcinin planı" hatası tam olarak `max_prepared_statements=0` iken çıkıyor; doküman bunu zaten yazıyor. `prepareThreshold=0` plan önbelleğini boşuna kapatır. Dokümana "yalnız destek olmayan pooler'da" notu eklendi.

**"HikariCP kendi statement önbelleğini kullanır" (Yanlış).** HikariCP bilinçli olarak statement önbelleği tutmaz; önbellek pgjdbc'nindir.

**`tcpKeepAlive=true` (Kısmen katılıyorum).** Aynı amaç için Hikari `keepaliveTime` eklendi.

**"SET LOCAL kirli bağlantıyla sonraki isteğe sızar" (Katılmıyorum).** Tersi doğru: `SET LOCAL` transaction bitince sona erer. İskelette ölçtük: commit sonrası değer boş string oluyor, sonraki istemci bağlamı görmüyor. Sızan şey düz `SET`; doküman bunu zaten ayrı kural olarak yazıyor. `DISCARD ALL` transaction mode'da prepared statement'ları da siler ve `server_reset_query` bu modda çalışmaz, yani öneri hem gereksiz hem zararlı.

## 3. Outbox ve partition

**Hibrit polling (Düzeltilerek katılıyorum).** Raporun kurgusunda bir hata var. "Outbox'a yazıldığı an" sinyal gönderilirse poller henüz commit edilmemiş satırı göremez. Sinyal **commit'ten sonra** (`afterCommit`) gönderilmeli. Ayrıca yalnız aynı JVM'i uyandırır ve `LISTEN/NOTIFY` PgBouncer transaction mode'da çalışmaz. "%80 yük azalması" ölçüm olmadan yazılmış bir sayı; boş tabloda indeksli bir sorgunun maliyeti küçük. Opsiyonel olarak eklendi.

**pg_partman kod şablonu (Katılmıyorum, kod çalışmıyor).** pg_partman 5.0.1 kaynağına baktık:
- `create_parent('<schema>.outbox_event', 'created_at', 'native', 'daily')` 5.x'te hata verir. `'daily'` gibi eski aralık adları açıkça reddediliyor ("Special partition interval values from old pg_partman versions … are no longer supported"), `'native'` tipi de kaldırılmış.
- `CALL partman.run_maintenance()` da hatalı: `run_maintenance` bir function, procedure olan `run_maintenance_proc`.

Daha önemlisi, **outbox partition edilmemeli**: satırlar iletimden sonra siliniyor, tablo küçük kalıyor. Partition append-only büyüyen tablolar (audit, olay günlüğü) içindir. Dokümana 5.x'in doğru sözdizimi ve bu ayrım eklendi.

## 4. Güvenlik

**JTI için Cuckoo/Bloom filter (Katılmıyorum).** Olasılıksal filtrelerde yanlış pozitif vardır. Replay kontrolünde yanlış pozitif, geçerli bir isteğin reddi demektir. Cuckoo filter elemana süre (TTL) de vermez; dönen filtreler gerekir. Servis JWT ömrü 60 sn olduğu için `SET NX EX 60` ile tutulan anahtar sayısı yalnız "saniyedeki istek × 60" kadardır. "%90 RAM tasarrufu" ölçümsüz. Doküman da jti deposu yerine mTLS ve kısa TTL'yi seçiyor (RFC 9700).

**Ed25519'da BouncyCastle'a düşme (Katılmıyorum).** Spring Security BouncyCastle'ı provider olarak kaydetmez. Biri `Security.addProvider` ile eklese bile listenin sonuna eklenir, JDK'nın SunEC'si önce seçilir. İskeletteki imzalayıcı zaten `Signature.getInstance("Ed25519")` ile JDK'yı kullanıyor.

**User-Agent standardı (Katılıyorum, başka başlık altında).** Dış sağlayıcıya giden çağrılarda tanımlayıcı User-Agent iyi bir pratik ve eklendi. Ama SSRF korumasıyla ilgisi yok; HTTP client konvansiyonuna konuldu.

## 5. RabbitMQ

**Prefetch formülü (Düzeltilerek katılıyorum).** `concurrency × prefetch` sınırı eklendi. Ancak quorum queue mesaj gövdelerini bellekte değil diskte (Raft log) tutar. Bu formül asıl olarak tüketici JVM'inin belleğini ve aynı anda işlenen iş sayısını sınırlar. Broker'ın OOM ile çökmesi senaryosu QQ için abartılı. "Virtual thread ile concurrency 100'e kolay çıkılır" da yanlış; listener eşzamanlılığı container ayarıdır.

**DLQ'ya `RepublishMessageRecoverer` ile exception header'ı (Katılmıyorum, amacına katılıyorum).** Varsayılan davranış ham exception mesajını ve stack trace'i broker'a yazar. Bunlar PII veya secret taşıyabilir; dokümanın log gizliliği kuralını delip geçer. Ayrıca republish ile ack atomik değildir. Dokümandaki yöntem şu şekilde netleştirildi: reddederken `eventId` ve hata koduyla yapılandırılmış log yaz, DLQ'daki mesajı broker'ın `x-death` header'ı ve `eventId` üzerinden bu log satırına bağla.

## 6. Gözlemlenebilirlik

**Pyroscope (Opsiyonel olarak katılıyorum).** Eklendi, ama raporun dayanağı yanlış. "Micrometer JFR verisini OTLP ile aktarır" ifadesi doğru değil; profiling ayrı bir agent ve depolama ister. Virtual thread pinning konusunda da JDK 24+ (JEP 491) `synchronized` kaynaklı pinning'i kaldırdı. Kalan pinning JFR'nin `jdk.VirtualThreadPinned` olayıyla görülür.

**Baggage (Kısıtlarla katılıyorum).** Eklendi. Baggage, üçüncü taraflar dahil her downstream'e gider. Bu yüzden yalnız allowlist'teki, PII olmayan, düşük kardinaliteli alanlar taşınır ve yetki kararında kullanılmaz. `tenant_id` gibi bir kimliği baggage'a koymak bu kuralla çelişir.

## 7. AI yönetişimi ve ArchUnit

**Husky / pre-commit (Kısmen katılıyorum).** "İnsan geliştirici kuralları atlar" tespiti eksik: aynı kurallar zaten CI'da koşuyor (migration değişmezliği, gitleaks, config-lint, ArchUnit). Yerel hook'lar `--no-verify` ile atlanabilir; kolaylık sağlar ama güvence değildir. Bu çerçeveyle eklendi.

**ArchUnit kuralları (Fikre katılıyorum, kod derlenmiyor).** İki parça iskelette derlenmeye çalışıldı:
- `andShould(new DescribedPredicate<>(...))`: `andShould` bir `ArchCondition` bekler; derleyici "incompatible types" verdi.
- `notCallMethodsThat(...)`: ArchUnit'te böyle bir metot yok; "cannot find symbol".
- Ayrıca `@ArchTest` kullanılmış; doküman bilinçli olarak düz JUnit `@Test` kullanıyor (ArchUnit engine'inin sessizce "0 test" geçme riski).
- Kod sınıf düzeyindeki `@Transactional`'ı da kaçırırdı.

Çalışan sürüm iskelete eklendi: `order-core/.../TransactionBoundaryRulesTest`, 4 test. Kurallar gerçek kodda geçiyor ve kasıtlı ihlal içeren fixture'da hem metot hem sınıf düzeyinde kırılıyor. Gerçek outbox yazıcısı `REQUIRED` yapılınca kural tam o metodu adıyla gösterdi. Sınırı da yazıldı: ArchUnit yalnız doğrudan çağrıyı görür, yardımcı bean üzerinden dolaylı çağrıyı göremez.

## 8. İstemci entegrasyonu

**Tolerant reader (Katılıyorum).** Bölüm 17'ye eklendi: bilinmeyen alan yok sayılır, bilinmeyen enum değeri `UNKNOWN`'a düşer. Bir düzeltme: `zod` `z.object` bilinmeyen alanı varsayılan olarak zaten atar (strip); asıl risk `.strict()` kullanmak ve enum'larda `.catch()` vermemek. Dart için `unknownEnumValue` eklendi.

**Gateway'de 410 global filtresi (Katılıyorum).** Doküman 410'u uygulama tarafında öneriyordu. Birden çok servis için gateway'de tek filtre alternatifi eklendi.

## Gemini'ye genel not

Raporun yönü çoğunlukla doğru ama verdiği kodların üçü çalışmıyor: ENTRYPOINT'teki bayrak yok sayılıyor, pg_partman çağrıları 5.x'te hata veriyor, ArchUnit kodu derlenmiyor. Ölçüm içermeyen yüzdeler var ("%80", "%90"). Bazı tespitler ise dokümanda zaten olan ya da test ettiğimiz davranışın tersini söylüyor (SET LOCAL, prepared statement). Sonraki turda öneri başına bir doğrulama yolu (komut, test veya kaynak satırı) istemek faydalı olur.

## Ek: `skeleton-ci.yml` için önerilen tam dosya

**"Dosya yarıda kalmış" (Katılmıyorum).** Repodaki dosya tam: 79 satır, son adımı surefire raporu yükleme. Gemini'ye giden kopya kesilmiş olmalı. Önerilen dosya bizimkinin neredeyse aynısı; gerçek farklar aşağıdaki üç madde.

**Java 25 (Katılıyorum, haklı ve önemli bir tespit).** Referans Java 25 öneriyor ve prod image `eclipse-temurin:25-jre`. Ama iskelet `maven.compiler.release=21` ile derleniyor ve CI JDK 21'de koşuyordu, yani prod runtime'ı hiç test edilmiyordu. Önce tam build'i JDK 25 runtime ile koşturduk (305 test yeşil). Ardından derleme hedefini de 25'e çektik (class dosyaları major version 69) ve yine 305 test yeşil. İki CI dosyası da 25'e alındı ve referansa "CI, derleme hedefi ve prod image aynı Java sürümü" kuralı eklendi.

**Kurulum adımını satırlara bölmek (Katılıyorum, gerçek bir hatayı düzeltiyor).** Bizdeki tek satır `apt-get update && apt-get install ... && systemctl stop ... || true` idi. Bash'te `|| true` tüm zincire uygulanır, dolayısıyla `apt-get install` başarısız olsa bile adım yeşil geçiyordu. Öneride `|| true` yalnız servis durdurma satırında kalıyor. İki CI dosyasında da düzeltildi ve kural referansa yazıldı.

**Test sayısı koruması `-gt 0` (Katılmıyorum, geriye gidiş).** Bizdeki koruma `-ge 290`. Amacı "0 test" durumunu değil, testlerin **sessizce azalmasını** yakalamak: bir modülün testleri yanlışlıkla devre dışı kalırsa toplam 305'ten 200'e düşer ve `-gt 0` bunu geçirir. "0 test = başarısız" kontrolü zaten her modülde `failIfNoTests=true` ile yapılıyor.

**SHA pinning (Zaten var).** Önerilen dosyadaki SHA'lar bizim dosyadakilerle aynı; dosyamız zaten pinli.

**`cd blueprint/skeleton-example && mvn verify` (Fark yok).** Bizdeki `mvn -f blueprint/skeleton-example/pom.xml verify` ile eşdeğer.
