#!/usr/bin/env python3
"""blueprint/ klasorunu tek dosyada toplar ve referans + blueprint'i tek 'tam' dokumanda birlestirir.
Uretilenler:
  docs/mikroservis-blueprint-dosyalari.md   — yalniz blueprint dosyalari
  docs/mikroservis-mimari-referans-tam.md    — referans dokumani + blueprint (baska projeye tek dosya olarak atilabilir)
"""
import pathlib, re
repo=pathlib.Path(__file__).resolve().parent.parent
root=repo/'blueprint'
order=['README.md','AGENTS.md','CLAUDE.md','.github/copilot-instructions.md','.github/PULL_REQUEST_TEMPLATE.md',
 'docs/ai/repo-context.md','docs/ai/security-rules.md','docs/ai/context-boundaries.md','docs/ai/review-checklist.md','docs/ai/operation-consistency.md',
 'docs/adr/0000-template.md','docs/versions.md','.github/workflows/ci.yml']
skills=sorted(p for p in root.glob('.agents/skills/**/*.md'))
rest=['.claude/settings.json','.claude/hooks/flyway-immutability.js','.claude/hooks/review-gate.sh','.claude/hooks/review-stamp.sh','.claude/hooks/tree-state.sh',
 'scripts/flyway-immutability.js','scripts/flyway-immutability.test.js',
 'scripts/config-lint.js','scripts/config-lint.test.js','scripts/gitleaks-check.sh','scripts/gitleaks-check.test.js',
 'scripts/fixtures/config-lint/application-local.yml','scripts/fixtures/config-lint/prod.env.example','scripts/fixtures/config-lint/service-bad.yml','scripts/fixtures/config-lint/service-ok.yml',
 'tests/ArchitectureRulesTest.java','tests/ErrorCodeUniquenessTest.java','tests/ConfigDriftTest.java']
skeleton=sorted(p for p in (root/'skeleton-example').rglob('*') if p.is_file() and 'target' not in p.parts)
files=[root/p for p in order]+skills+[root/p for p in rest]+skeleton
lang={'.md':'markdown','.json':'json','.js':'javascript','.sh':'bash','.java':'java','.xml':'xml','.yml':'yaml','.example':'bash'}

def embed(f, shift):
    rel=f.relative_to(root).as_posix(); txt=f.read_text(encoding='utf-8'); ext=f.suffix
    out=['---','',f'{"#"*shift} `{rel}`','']
    if ext=='.md':
        out.append('\n'.join(('#'*(shift-1)+l if l.startswith('#') else l) for l in txt.splitlines()))
    else:
        fence='````' if '```' in txt else '```'
        out+=[f'{fence}{lang.get(ext,"")}',txt.rstrip('\n'),fence]
    out.append(''); return out

# 1) yalniz blueprint
bp=['# Mikroservis Blueprint Dosyaları (tek dosya görünümü)','',
'> `blueprint/` klasörünün birebir içeriği. Gizli klasörler (`.agents`, `.claude`, `.github`) bazı görüntüleyicilerde görünmediği için burada tek dosyada toplanmıştır. **Düzenleme `blueprint/` altında yapılır**; bu dosya `python3 scripts/build-blueprint-doc.py` ile yeniden üretilir.','',
'## İçindekiler','']+[f'- `{f.relative_to(root).as_posix()}`' for f in files]+['']
for f in files: bp+=embed(f,2)
(repo/'docs'/'mikroservis-blueprint-dosyalari.md').write_text('\n'.join(bp),encoding='utf-8')

# 2) tam dokuman = referans + blueprint (referansin basliklari korunur, blueprint 'Ek' olarak 2 seviye altta)
ref=(repo/'docs'/'mikroservis-mimari-referans.md').read_text(encoding='utf-8')
head=['# Mikroservis Mimari Referansı — TAM SÜRÜM (referans + blueprint dosyaları)','',
'> **Nasıl kullanılır:** Bu tek dosyayı başka bir projenin `docs/` klasörüne koyup bir AI ajanına "**bu dokümanı referans alarak projeyi düzenle**" de. Ajan **Bölüm 26 (Uygulama Protokolü)** ile başlar: önce keşif ve uyum raporu, sonra F0→F6 sırasıyla küçük PR\'lar. Ek A\'daki dosyalar (`AGENTS.md`, `docs/ai/*`, skill\'ler, hook\'lar, script\'ler, testler) repoya birebir kopyalanır; Ek B çalışan bir iskelet örneğidir.','',
'> İki parça: **Referans** (Bölüm 1–26) ve **Ek A/B** (blueprint dosyaları). Tek kaynak repodaki `docs/mikroservis-mimari-referans.md` ve `blueprint/`; bu dosya `python3 scripts/build-blueprint-doc.py` ile üretilir.','','---','']
body=ref.split('\n',1)[1]  # ilk H1 basligini at
ekA=['','---','','# Ek A — Blueprint Dosyaları (repoya birebir kopyalanır)','',
'İçindekiler:','']+[f'- `{f.relative_to(root).as_posix()}`' for f in files if 'skeleton-example' not in f.parts]+['']
for f in files:
    if 'skeleton-example' in f.parts: continue
    ekA+=embed(f,3)
ekB=['','---','','# Ek B — Doğrulanmış Boş İskelet (`blueprint/skeleton-example/`)','',
'Spring Boot 4.1.1 + ArchUnit 1.5.1 ile `mvn test` yeşil; 8 kasıtlı ihlal yakalandı. Yeni projede başlangıç noktası.','']
for f in files:
    if 'skeleton-example' in f.parts: ekB+=embed(f,3)
full='\n'.join(head)+'\n# Referans\n'+body+'\n'.join(ekA)+'\n'.join(ekB)
full=re.sub(r'§(\d)',r'Bölüm \1',full)
(repo/'docs'/'mikroservis-mimari-referans-tam.md').write_text(full,encoding='utf-8')
print('blueprint dosya:',len(files),'| tam satir:',full.count('\n'))
