#!/bin/bash
# SessionStart: names the saved output style and the two kinds of session, and
# adds a note when the hook input names a Fable model. Always exits 0: a missing
# file, bad JSON or an unknown shape adds nothing and never fails the session.
root="${CLAUDE_PROJECT_DIR:-$PWD}"

# The outputStyle saved in settings file $1, when it is a non-empty string.
saved_style() {
  python3 -I -c '
import json, sys
try:
    with open(sys.argv[1]) as f:
        v = json.load(f).get("outputStyle")
except Exception:
    v = None
if isinstance(v, str) and v.strip():
    print(v.strip())
' "$1" 2>/dev/null
}

style=""
for f in "$root/.claude/settings.local.json" "$root/.claude/settings.json" "$HOME/.claude/settings.json"; do
  style=$(saved_style "$f")
  [ -n "$style" ] && break
done
echo "Output style: ${style:-default}."
echo "Kinds of session: /output-style exploration (thinking with Sid) or /output-style build."

# The model in the hook input: a string, or an object with an id or display name.
model=$(python3 -I -c '
import json, sys
try:
    m = json.load(sys.stdin).get("model")
except Exception:
    m = None
if isinstance(m, dict):
    m = " ".join(str(m.get(k) or "") for k in ("id", "display_name", "displayName"))
if isinstance(m, str):
    print(m)
' 2>/dev/null)

shopt -s nocasematch
if [[ $model == *fable* ]]; then
  cat <<'EOF'
For Fable. Sid's line "fable tries to make shortcuts" means this: your shortcut is to gather and cite instead of thinking at the level asked.
From the inside it looks like diligence.
EOF
fi
exit 0
