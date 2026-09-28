---
name: proj-release-readiness-review
description: Use this skill on release PRs (to release/main), before the first production deploy of a new service, and quarterly — to verify backups, alerting, SLOs, runbooks, capacity, version/EOL status, security posture and rollback readiness.
---

Sürümün üretime çıkmaya hazır olup olmadığını mimari referans Bölüm 8, 10.5, 18, 21.0, 24 ve 25'e göre değerlendir. Bu skill kod kalitesine değil **operasyonel gerçeklere** bakar: yedek var mı, alarm gidiyor mu, geri dönüş kaç dakika. Her madde için kanıt (dosya, config, pano linki, tarih) istenir; kanıtsız madde `BLOCKED`. Sorun yoksa: **"Release hazırlık kontrolü geçti."**

Kontrol et:

## Veri ve yedek (yoksa `FAIL`)
- WAL arşivi + base backup çalışıyor (son başarılı yedek tarihi); retention; şifreli.
- **Restore provası** son 30 gün içinde yapılmış ve kayıtlı (süre, doğrulama).
- RPO/RTO README'de; HA durumu (managed/standby/yok) açıkça yazılı ve kabul edilmiş.
- Redis security instance AOF; RabbitMQ definitions yedeği; object storage versioning.
- Bu release'in migration'ları prod benzeri veri hacminde denenmiş (süre, lock).

## Alarm ve gözlem (yoksa `FAIL`)
- Alertmanager/Grafana alerting bir kanala **gerçekten** bildirim gönderiyor (test alarmı tarihi).
- Asgari alarm seti (referans 8.7): restart-loop, health DOWN, disk/RAM, WAL arşiv gecikmesi, Redis bellek/eviction, RabbitMQ queue/DLQ, `outbox_oldest_pending_age_seconds`, `*_STUCK`, SLO burn-rate.
- Yeni servis/uç için scrape hedefi, log kaynağı, pano.
- Structured log; trace sampling politikası; Loki label kardinalitesi.

## SLO ve kapasite
- Kritik akışlar için SLO tanımlı ve pano var; son yük testi tarihi ve sonucu (p99, hata oranı) SLO içinde.
- Kapasite tablosu: host RAM/CPU kullanımı < %70; ikinci instance/host planı; DB bağlantı bütçesi; Bölüm 24 eşiklerinden yaklaşılan var mı.

## Deploy ve geri dönüş
- Image CI'da build, registry'de, cosign imzalı, digest ile deploy; prod'da build yok.
- Rollout sırası (sahip → tüketici → auth → gateway) release notunda; tüketici-önce kuralı yeni event/enum için sağlanmış.
- Rollback: önceki digest listesi mevcut; migration expand/contract'a uygun (eski image yeni şemayla çalışır); prova edilmiş rollback süresi.
- Healthcheck ve readiness her serviste; `restart: always`; `docker-rollout`/blue-green.
- Staging'de smoke test geçti; prod deploy onay kapısı var.

## Güvenlik duruşu
- Secret'lar `/run/secrets`; literal fallback yok; gitleaks CI temiz; `.dockerignore`.
- Asimetrik service JWT; JWKS/`kid`; rotasyon prosedürü.
- Mock entegrasyon bean'i prod profilinde yok (açılış guard'ı testi).
- Seed/test verisi prod location'ında yok; bilinen parolalı admin yok.
- Gateway: rate limit, timeout, CORS listesi, `gateway` actuator kapalı, trusted proxy.
- Bağımlılık taraması (Dependabot/OSV) açık CRITICAL/HIGH yok veya kabul edilmiş risk yazılı.

## Sürüm ve destek (Bölüm 25)
- Java, Spring Boot, Spring Cloud, PostgreSQL, Redis/Valkey, RabbitMQ, arama motoru, Alloy/Loki/Tempo/Prometheus/Grafana, Node: **hepsi OSS destek içinde**; bitişe < 3 ay kalan için upgrade PR/plan var.
- Yaması yalnız ticari sürümde olan bilinen CVE yok.
- Lisans değişikliği (Redis, Elastic, BSL, GPL) gözden geçirilmiş.

## Uyum ve ürün
- Hesap silme akışı çalışıyor (uygulama içi + saga); export ucu; KVKK/GDPR süreleri ölçülüyor.
- Kişisel veri envanteri ve DPIA güncel; yeni alanlar eklendi mi.
- İstemci entegrasyon dokümanı ve OpenAPI/generated client release ile uyumlu; API sunset tarihleri.
- Runbook'lar: her alarm için "ne yapılır" sayfası; on-call kim; olay sonrası postmortem şablonu.

## Dokümantasyon
- README kimlik tablosu, sıcak yol tablosu, fail politikası tablosu, kapasite tablosu güncel.
- Bu release için ADR gerektiren karar varsa ADR var.
- `docs/ai/repo-context.md` güncel; `docs/versions.md` tarihli.

Çıktı:
1. **Kontrol tablosu:** her başlık → `PASS` / `FAIL` / `BLOCKED` + kanıt (dosya/link/tarih).
2. **Engelleyiciler:** `FAIL` olanlar ve düzeltme; `BLOCKED` olanlar ve istenen kanıt.
3. **Kabul edilen riskler:** yazılı, sahipli, tarihli.
4. **Bölüm 24 eşik durumu:** yaklaşılan eşikler ve planlanan adım.
5. **Nihai durum:** `PASS` / `FAIL` / `BLOCKED`. Yedek, restore provası veya alarm kanalı eksikse durum `FAIL`.
