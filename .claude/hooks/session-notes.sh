#!/bin/bash
# SessionStart: names the saved output style and the two opt-in session styles.
# Always exits 0: a missing file, bad JSON or an unknown shape adds nothing and
# never fails the session.
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
echo "Kinds of session: /output-style Exploration (exploring with Sid) or /output-style Build."
exit 0
