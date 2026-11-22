#!/bin/bash
# SessionStart: names the active output style (--settings override first, then
# saved settings files) and the two opt-in session styles.
# Always exits 0: a missing file, bad JSON or an unknown shape adds nothing and
# never fails the session.
root="${CLAUDE_PROJECT_DIR:-$PWD}"

# Extract outputStyle from --settings <json-or-file> in ancestor process argv, if any.
cli_style() {
  python3 -I -c '
import json, os

def parse_val(raw):
    raw = raw.strip()
    if not raw:
        return None
    try:
        data = json.loads(raw) if raw.startswith("{") else json.load(open(raw))
    except Exception:
        return None
    v = data.get("outputStyle") if isinstance(data, dict) else None
    return v.strip() if isinstance(v, str) and v.strip() else None

pid = os.getppid()
for _ in range(6):
    if pid <= 1:
        break
    try:
        args = open(f"/proc/{pid}/cmdline", "rb").read().decode("utf-8", "replace").split("\0")
        for i, a in enumerate(args):
            if a == "--settings" and i + 1 < len(args):
                v = parse_val(args[i + 1])
                if v:
                    print(v); raise SystemExit(0)
            elif a.startswith("--settings="):
                v = parse_val(a.split("=", 1)[1])
                if v:
                    print(v); raise SystemExit(0)
        stat = open(f"/proc/{pid}/stat").read()
        pid = int(stat[stat.rfind(")") + 2:].split()[1])
    except SystemExit:
        raise
    except Exception:
        break
' 2>/dev/null
}

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

style=$(cli_style)
if [ -z "$style" ]; then
  for f in "$root/.claude/settings.local.json" "$root/.claude/settings.json" "$HOME/.claude/settings.json"; do
    style=$(saved_style "$f")
    [ -n "$style" ] && break
  done
fi
echo "Output style: ${style:-default}."
echo "Kinds of session, each set for one session at launch: claude --settings '{\"outputStyle\":\"Exploration\"}' (exploring with Sid) or claude --settings '{\"outputStyle\":\"Build\"}'. Typing /output-style instead saves the style for every session in this project."
echo "If Sid's message reads as the other kind of session, say so in your first reply."
exit 0
