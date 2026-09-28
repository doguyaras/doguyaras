#!/usr/bin/env python3
"""blueprint/ klasorunu docs/mikroservis-blueprint-dosyalari.md olarak tek dosyada toplar."""
import pathlib
root=pathlib.Path(__file__).resolve().parent.parent/'blueprint'
order=['README.md','AGENTS.md','CLAUDE.md','.github/copilot-instructions.md','.github/PULL_REQUEST_TEMPLATE.md',
 'docs/ai/repo-context.md','docs/ai/security-rules.md','docs/ai/context-boundaries.md','docs/ai/review-checklist.md','docs/ai/operation-consistency.md',
 'docs/adr/0000-template.md']
skills=sorted(p for p in root.glob('.agents/skills/**/*.md'))
rest=['.claude/settings.json','.claude/hooks/flyway-immutability.js','.claude/hooks/review-gate.sh','.claude/hooks/review-stamp.sh',
 'scripts/flyway-immutability.js','scripts/flyway-immutability.test.js',
 'tests/ArchitectureRulesTest.java','tests/ErrorCodeUniquenessTest.java','tests/ConfigDriftTest.java']
files=[root/p for p in order]+skills+[root/p for p in rest]
lang={'.md':'markdown','.json':'json','.js':'javascript','.sh':'bash','.java':'java'}
out=['# Mikroservis Blueprint Dosyaları (tek dosya görünümü)','',
'> `blueprint/` klasörünün birebir içeriği. Gizli klasörler (`.agents`, `.claude`, `.github`) bazı görüntüleyicilerde görünmediği için burada tek dosyada toplanmıştır. **Düzenleme `blueprint/` altında yapılır**; bu dosya `python3 scripts/build-blueprint-doc.py` ile yeniden üretilir.','',
'## İçindekiler','']
for f in files:
    out.append(f'- `{f.relative_to(root).as_posix()}`')
out.append('')
for f in files:
    rel=f.relative_to(root).as_posix(); txt=f.read_text(encoding='utf-8'); ext=f.suffix
    out+=['---','',f'## `{rel}`','']
    if ext=='.md':
        out.append('\n'.join(('#'+l if l.startswith('#') else l) for l in txt.splitlines()))
    else:
        fence='````' if '```' in txt else '```'
        out+=[f'{fence}{lang.get(ext,"")}',txt.rstrip('\n'),fence]
    out.append('')
(root.parent/'docs'/'mikroservis-blueprint-dosyalari.md').write_text('\n'.join(out),encoding='utf-8')
print('ok', len(files))
