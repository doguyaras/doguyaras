#!/usr/bin/env bash
# Claude Code PreToolUse hook (Bash): `git push` oncesi son 1 saatte bir review skill'i calismis mi?
# Calismadiysa kullaniciya sorar (permissionDecision: "ask"). Push disi komutlarda sessizce gecer.
#
# Neden ayri dosya: settings.json icine gomulu shell komutlarinda kacis hatasi kolay yapilir ve hook
# sessizce etkisiz kalir. Bu dosya `bash -n` ve ornek girdiyle test edilir:
#   echo '{"tool_input":{"command":"git push origin x"}}' | .claude/hooks/review-gate.sh
set -euo pipefail

INPUT="$(cat)"
CMD="$(printf '%s' "$INPUT" | node -e 'let s="";process.stdin.on("data",c=>s+=c).on("end",()=>{try{const j=JSON.parse(s);process.stdout.write(String((j.tool_input&&j.tool_input.command)||""))}catch(e){process.stdout.write("")}})')"

case "$CMD" in
  *"git push"*) ;;
  *) exit 0 ;;
esac

ROOT="${CLAUDE_PROJECT_DIR:-$PWD}"
STAMP="$ROOT/.claude/.last-review-check"
MAX_AGE=3600
NOW="$(date +%s)"

if [[ -f "$STAMP" ]]; then
  LAST="$(cat "$STAMP" 2>/dev/null || echo 0)"
  if [[ "$LAST" =~ ^[0-9]+$ ]] && (( NOW - LAST < MAX_AGE )); then
    exit 0
  fi
fi

cat <<'JSON'
{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"ask","permissionDecisionReason":"Son 1 saatte review skill'i (proj-*-review / proj-test-writer) calistirilmadi. docs/ai/review-checklist.md'deki skill'leri calistirmadan push etmek istiyor musun?"}}
JSON
exit 0
