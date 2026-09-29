# versions.md — Sürüm ve Destek Anlık Görüntüsü

> **Tarih:** <YYYY-MM-DD> (çeyrekte bir güncellenir; `proj-release-readiness-review` bu tarihi kontrol eder). Kurallar mimari referans Bölüm 25'te; bu dosya yalnız **sayıları ve tarihleri** taşır. Kaynağı olmayan satır yazılmaz.

| Bileşen | Kullanılan sürüm | OSS destek sonu | Kaynak | Not |
|---|---|---|---|---|
| Java | 25 LTS | – (LTS) | endoflife.date/oracle-jdk | |
| Spring Boot | 4.1.x | 2027-07-31 | spring.io/projects/spring-boot#support | 4.0.x: 2026-12-31 |
| Spring Cloud | 2025.1.x | Boot 4.1 hattıyla | github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions | 2026.0 → Boot 4.2 |
| Spring Modulith (şekil A/C) | 2.1.x | – | spring.io/projects/spring-modulith | |
| PostgreSQL | 18.x | 2030-11 | postgresql.org/support/versioning | |
| Valkey / Redis | 9.x / 8.x | – | valkey.io, redis.io/legal/licenses | lisans: BSD / AGPLv3 |
| RabbitMQ | 4.3.x | en yeni minor community | rabbitmq.com/release-information | Erlang 27.x; `khepri_db` |
| Grafana Alloy / Loki / Tempo | 1.x / 3.x / 2.x | – | GitHub releases | Promtail EOL 2026-03-02 |
| Testcontainers | 2.x | – | GitHub releases | `testcontainers-` önekli artefaktlar |
| Node (panel/CI) | 24 LTS | 2028-04-30 | nodejs.org/en/about/eol | |
| Docker base image | eclipse-temurin:25-jre | – | hub.docker.com | Renovate |

**Bilinen CVE tetikleyicileri** (yaması yalnız ticari sürümde olan → upgrade): <liste veya "yok">

**Son kontrol komutları:** `mvn versions:display-dependency-updates`, Renovate panosu, `osv-scanner`.
