#!/usr/bin/env bash
# sync-rama-skill.sh — vendor the Rama skill from redplanetlabs/rama-ai-learn
# into both agent lanes, pinned to an upstream commit.
#
#   scripts/sync-rama-skill.sh check          lanes == pinned upstream?  has upstream moved?
#   scripts/sync-rama-skill.sh update [REF]   vendor REF (default origin/HEAD), re-apply overlay, rewrite pin
#
# Layout
#   .claude/skills/rama/   verbatim upstream plugins/rama-skill/skills/rama   (Claude Code lane)
#   .agents/skills/rama/   same + Softland overlay paragraph in SKILL.md
#                          + agents/openai.yaml (Codex-only metadata, local)   (Codex lane)
#   <lane>/UPSTREAM        the pin: repo / path / sha / date. Written by `update`, read by `check`.
#
# Upstream never bumps its plugin version string (1.0.0 across every skill
# update so far), so the commit SHA in UPSTREAM is the only honest version.
set -euo pipefail

REPO=https://github.com/redplanetlabs/rama-ai-learn
SKILL_PATH=plugins/rama-skill/skills/rama
ROOT=$(cd "$(dirname "$0")/.." && pwd)
CLAUDE_LANE=$ROOT/.claude/skills/rama
CODEX_LANE=$ROOT/.agents/skills/rama
PIN=$CLAUDE_LANE/UPSTREAM

# Softland-only paragraph. Lives here, not in the vendored files, so the
# vendored tree stays byte-identical to upstream. Inserted into the Codex
# lane's SKILL.md immediately before OVERLAY_ANCHOR.
OVERLAY_ANCHOR='## Implementation Goals'
read -r -d '' OVERLAY <<'EOF' || true
For Softland Rama work on existing modules, source adapters, common kernels, or product-path reviews, also use `$rama-retro-lens` before accepting a plan or final diff. The phased artifacts are not evidence by themselves; the retro lens checks whether product paths, ownership boundaries, retries/idempotency, terminal states, and runtime tests actually avoid prior failure patterns.
EOF

die() { echo "error: $*" >&2; exit 2; }

# Sparse, blobless clone: full history (for `log`), only the skill's blobs.
# Sets WORK, SHA, SHA_DATE, TIP.
fetch_upstream() {
  local ref=$1
  WORK=$(mktemp -d)
  trap 'rm -rf "$WORK"' EXIT
  git clone -q --filter=blob:none --no-checkout "$REPO" "$WORK"
  git -C "$WORK" sparse-checkout set "$SKILL_PATH"
  git -C "$WORK" checkout -q "$ref"
  SHA=$(git -C "$WORK" rev-parse HEAD)
  SHA_DATE=$(git -C "$WORK" log -1 --format=%cs)
  TIP=$(git -C "$WORK" rev-parse origin/HEAD)
  [ -d "$WORK/$SKILL_PATH" ] || die "$SKILL_PATH not present at $SHA"
}

apply_overlay() {
  local file=$1 n
  n=$(grep -cxF "$OVERLAY_ANCHOR" "$file" || true)
  [ "$n" = 1 ] || die "overlay anchor '$OVERLAY_ANCHOR' found $n times in $file (need exactly 1) — upstream restructured SKILL.md; fix OVERLAY_ANCHOR"
  awk -v anchor="$OVERLAY_ANCHOR" -v text="$OVERLAY" '$0==anchor{print text; print ""} {print}' "$file" > "$file.tmp"
  mv "$file.tmp" "$file"
}

write_pin() {
  local lane=$1
  cat > "$lane/UPSTREAM" <<EOF
# Vendored from redplanetlabs/rama-ai-learn — do not hand-edit this directory.
# Update:  scripts/sync-rama-skill.sh update [REF]
# Verify:  scripts/sync-rama-skill.sh check
repo=$REPO
path=$SKILL_PATH
sha=$SHA
date=$SHA_DATE
EOF
}

pinned_sha() { [ -f "$PIN" ] || die "no pin at $PIN — run: $0 update"; sed -n 's/^sha=//p' "$PIN"; }

cmd_update() {
  local ref=${1:-origin/HEAD} old
  old=$( [ -f "$PIN" ] && sed -n 's/^sha=//p' "$PIN" || echo none )
  fetch_upstream "$ref"
  rsync -a --delete --exclude=UPSTREAM                   "$WORK/$SKILL_PATH/" "$CLAUDE_LANE/"
  rsync -a --delete --exclude=UPSTREAM --exclude=agents/ "$WORK/$SKILL_PATH/" "$CODEX_LANE/"
  apply_overlay "$CODEX_LANE/SKILL.md"
  write_pin "$CLAUDE_LANE"; write_pin "$CODEX_LANE"
  echo "pin: ${old:0:7} -> ${SHA:0:7} ($SHA_DATE)   upstream tip: ${TIP:0:7}"
  git -C "$ROOT" diff --stat -- .claude/skills/rama .agents/skills/rama | tail -1
}

cmd_check() {
  local pinned drift=0 behind
  pinned=$(pinned_sha)
  fetch_upstream "$pinned"
  echo "pin ${pinned:0:7} ($SHA_DATE)"
  if diff -r --exclude=UPSTREAM "$WORK/$SKILL_PATH" "$CLAUDE_LANE" >/dev/null; then
    echo "  .claude/skills/rama  == upstream"
  else
    drift=1; echo "  .claude/skills/rama  DRIFT:"; diff -rq --exclude=UPSTREAM "$WORK/$SKILL_PATH" "$CLAUDE_LANE" | sed 's/^/    /'
  fi
  cp -r "$WORK/$SKILL_PATH" "$WORK/expected-codex"; apply_overlay "$WORK/expected-codex/SKILL.md"
  if diff -r --exclude=UPSTREAM --exclude=agents "$WORK/expected-codex" "$CODEX_LANE" >/dev/null; then
    echo "  .agents/skills/rama  == upstream + overlay"
  else
    drift=1; echo "  .agents/skills/rama  DRIFT:"; diff -rq --exclude=UPSTREAM --exclude=agents "$WORK/expected-codex" "$CODEX_LANE" | sed 's/^/    /'
  fi
  behind=$(git -C "$WORK" log --oneline "$pinned..origin/HEAD" -- "$SKILL_PATH" | wc -l)
  if [ "$behind" = 0 ]; then
    echo "upstream tip ${TIP:0:7}: no skill changes since pin"
  else
    echo "upstream tip ${TIP:0:7}: $behind commit(s) touch the skill since pin — run: $0 update"
    git -C "$WORK" log --format='    %h %cs %s' "$pinned..origin/HEAD" -- "$SKILL_PATH"
  fi
  return $drift
}

case "${1:-}" in
  check)  cmd_check ;;
  update) shift; cmd_update "$@" ;;
  *)      sed -n '2,15p' "$0"; exit 1 ;;
esac
