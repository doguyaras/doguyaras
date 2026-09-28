#!/usr/bin/env bash
# Claude Code PostToolUse hook (Skill): review skill'i calistiysa damga yaz.
set -euo pipefail
INPUT="$(cat)"
NAME="$(printf '%s' "$INPUT" | node -e 'let s="";process.stdin.on("data",c=>s+=c).on("end",()=>{try{const j=JSON.parse(s);process.stdout.write(String((j.tool_input&&(j.tool_input.skill||j.tool_input.name))||""))}catch(e){process.stdout.write("")}})')"
case "$NAME" in
  *-review|*test-writer|*integration-doc)
    ROOT="${CLAUDE_PROJECT_DIR:-$PWD}"
    mkdir -p "$ROOT/.claude"
    date +%s > "$ROOT/.claude/.last-review-check"
    ;;
esac
exit 0
