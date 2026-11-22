#!/usr/bin/env bash
# sync-rama-skill.sh — vendor the Rama skill from redplanetlabs/rama-ai-learn
# into both agent lanes, pinned to an upstream commit, with Softland's overlay.
#
#   scripts/sync-rama-skill.sh check          lanes == pinned upstream + overlay?  has upstream moved?
#   scripts/sync-rama-skill.sh update [REF]   vendor REF (default origin/HEAD), re-apply overlay, rewrite pin
#
# Layout
#   .claude/skills/rama/   upstream plugins/rama-skill/skills/rama
#                          + Softland overlay in SKILL.md & artifact-plan.md   (Claude Code lane)
#   .agents/skills/rama/   same + Codex paragraph in SKILL.md
#                          + agents/openai.yaml (Codex-only metadata, local)   (Codex lane)
#   <lane>/UPSTREAM        the pin: repo / path / sha / date. Written by `update`, read by `check`.
#
# The overlay's text is kept in this script and update writes it into the
# lanes, so it is edited here, never there. In both lanes it inserts the
# section "How Softland uses this skill" and rewrites one phrase of the
# frontmatter description in SKILL.md, and backports upstream 968b027's
# third-person comment under "## Design difficulty log" in
# references/artifact-plan.md until upstream merges it; the Codex lane also
# gets the Codex paragraph. update and check build the lanes the same way,
# from upstream + overlay, so a hand-edit in either lane shows as DRIFT. An
# anchor or a replaced phrase that does not match exactly once stops the
# script before any lane is touched.
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

# --- The overlay -------------------------------------------------------------
# Both lanes: this section, inserted immediately before SECTION_ANCHOR.
SECTION_ANCHOR='## How to Build a Rama Module'
read -r -d '' SECTION <<'EOF' || true
## How Softland uses this skill

The design rules in this file and everything under `references/` on Rama's API, costs and failure cases apply to all Rama work here. The phased process described next (one phase per session, a required page per phase) is not the default in Softland; Sid asks for it when he wants it. What we keep from it is the idea under it: a different session checks a module's design, and what counts in that check is a run: a model of a cross-task protocol that runs, a probe on a real cluster, a proof against a reference.
EOF

# Both lanes: in the frontmatter description, DESC_OLD becomes DESC_NEW.
# DESC_NEW holds ': ', which breaks a plain YAML scalar, so the description has
# to be a block scalar (`description: >-`), as upstream writes it.
DESC_OLD='this skill first and following its phased build process.'
DESC_NEW='this skill first. In Softland its phased process is not the default: see "How Softland uses this skill".'

# Both lanes: in references/artifact-plan.md, replace the first-person prompt
# under "## Design difficulty log" (which triggers Opus reasoning_extraction
# stops) with upstream branch commit 968b027's wording until upstream merges it.
read -r -d '' DIFF_LOG_OLD <<'EOF' || true
## Design difficulty log
<!-- An honest, first-person record of where this design was hard to settle. Write it as you design, not as a summary. For each decision that was genuinely contested:
- The decision (which PState schema, partitioning, placement, or topology).
- The competing approach(es) you weighed, and how close the call was — was an alternative genuinely competitive, or was the choice forced once you costed it against the requirements?
- What finally settled it.
If nothing was contested — every choice was forced once the requirements were read — say so plainly and briefly. Do not manufacture difficulty; do not hide it. -->
EOF
read -r -d '' DIFF_LOG_NEW <<'EOF' || true
## Design difficulty log
<!-- The decisions in this design that had a competitive alternative. For each: the decision (PState schema, partitioning, placement, or topology), the alternative(s) considered with their costs against the requirements, and the requirement or cost that settled it. If every choice was forced by the requirements, say so in one line. -->
EOF

# Codex lane only: this paragraph, inserted immediately before CODEX_ANCHOR.
CODEX_ANCHOR='## Implementation Goals'
read -r -d '' CODEX_PARAGRAPH <<'EOF' || true
For Softland Rama work on existing modules, source adapters, common kernels, or product-path reviews, also use `$rama-pitfalls` before accepting a plan or final diff. Prose artifacts are not evidence by themselves; falsify event boundaries, retries/idempotency, terminal states, ownership boundaries, and read-after-write visibility, and verify with runtime probes.
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

# insert_before FILE ANCHOR TEXT: TEXT and a blank line go in immediately
# before the one line of FILE that equals ANCHOR. TEXT may span lines; it
# reaches awk through ENVIRON, which passes it whole and as written (awk -v
# would rewrite backslash escapes in it).
insert_before() {
  local file=$1 anchor=$2 text=$3 n
  n=$(grep -cxF -- "$anchor" "$file" || true)
  [ "$n" = 1 ] || die "anchor '$anchor' found $n times in $file (need exactly 1) — upstream restructured SKILL.md; fix the anchor in $0"
  ANCHOR=$anchor TEXT=$text awk '$0 == ENVIRON["ANCHOR"] { print ENVIRON["TEXT"]; print "" } { print }' "$file" > "$file.tmp"
  mv "$file.tmp" "$file"
}

# replace_in_description FILE OLD NEW: OLD, which must occur exactly once in
# FILE and that once in the frontmatter, becomes NEW. The frontmatter's
# description must be a block scalar (see DESC_NEW).
replace_in_description() {
  local file=$1 old=$2 new=$3 all infm block
  read -r all infm block < <(OLD=$old awk '
    NR == 1 && $0 == "---" { fm = 1; next }
    fm && $0 == "---"      { fm = 0 }
    fm && /^description:[ \t]*[>|]/ { block = 1 }
    { s = $0
      while ((i = index(s, ENVIRON["OLD"])) > 0) { all++; if (fm) infm++; s = substr(s, i + length(ENVIRON["OLD"])) } }
    END { print all + 0, infm + 0, block + 0 }' "$file") || true
  [ "$all $infm" = "1 1" ] || die "phrase '$old' found $all times in $file, $infm of them in the frontmatter (need exactly 1, there) — upstream rewrote the description; fix DESC_OLD in $0"
  [ "$block" = 1 ] || die "the description in $file is not a block scalar (description: >-) — DESC_NEW holds ': ', which would break its YAML; fix the overlay in $0"
  OLD=$old NEW=$new awk '{ i = index($0, ENVIRON["OLD"]); if (i) $0 = substr($0, 1, i - 1) ENVIRON["NEW"] substr($0, i + length(ENVIRON["OLD"])); print }' "$file" > "$file.tmp"
  mv "$file.tmp" "$file"
}

# replace_once FILE LABEL OLD NEW: OLD (which may span lines) must occur
# exactly once in FILE and becomes NEW. When upstream merges 968b027, the match
# count drops to 0 and stops the script loudly so the local patch can be removed.
replace_once() {
  local file=$1 label=$2 old=$3 new=$4 n
  n=$(OLD=$old awk '
    { buf = (NR == 1 ? $0 : buf "\n" $0) }
    END {
      o = ENVIRON["OLD"]; c = 0
      while ((i = index(buf, o)) > 0) { c++; buf = substr(buf, i + length(o)) }
      print c
    }' "$file")
  [ "$n" = 1 ] || die "$label matched $n times in $file (need exactly 1) — upstream changed $file; if 968b027 merged, drop DIFF_LOG_OLD in $0"
  OLD=$old NEW=$new awk '
    { buf = (NR == 1 ? $0 : buf "\n" $0) }
    END {
      o = ENVIRON["OLD"]; i = index(buf, o)
      printf "%s%s%s\n", substr(buf, 1, i - 1), ENVIRON["NEW"], substr(buf, i + length(o))
    }' "$file" > "$file.tmp"
  mv "$file.tmp" "$file"
}

# From upstream's tree in $WORK, build the two lanes as they must be:
# $WORK/expected-claude and $WORK/expected-codex. update copies them into the
# lanes and check diffs the lanes against them, so the two cannot disagree.
build_expected() {
  cp -Rp "$WORK/$SKILL_PATH" "$WORK/expected-claude"
  insert_before          "$WORK/expected-claude/SKILL.md"                    "$SECTION_ANCHOR"        "$SECTION"
  replace_in_description "$WORK/expected-claude/SKILL.md"                    "$DESC_OLD"              "$DESC_NEW"
  replace_once           "$WORK/expected-claude/references/artifact-plan.md" "Design difficulty log"  "$DIFF_LOG_OLD" "$DIFF_LOG_NEW"
  cp -Rp "$WORK/expected-claude" "$WORK/expected-codex"
  insert_before          "$WORK/expected-codex/SKILL.md"                     "$CODEX_ANCHOR"          "$CODEX_PARAGRAPH"
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
  build_expected
  rsync -a --delete --exclude=UPSTREAM                   "$WORK/expected-claude/" "$CLAUDE_LANE/"
  rsync -a --delete --exclude=UPSTREAM --exclude=agents/ "$WORK/expected-codex/"  "$CODEX_LANE/"
  write_pin "$CLAUDE_LANE"; write_pin "$CODEX_LANE"
  echo "pin: ${old:0:7} -> ${SHA:0:7} ($SHA_DATE)   upstream tip: ${TIP:0:7}"
  git -C "$ROOT" diff --stat -- .claude/skills/rama .agents/skills/rama | tail -1
}

cmd_check() {
  local pinned drift=0 behind
  pinned=$(pinned_sha)
  fetch_upstream "$pinned"
  echo "pin ${pinned:0:7} ($SHA_DATE)"
  build_expected
  # `|| true` on each listing: under pipefail a diff that finds drift would
  # otherwise end the script here, before the other lane and the tip are shown.
  if diff -r --exclude=UPSTREAM "$WORK/expected-claude" "$CLAUDE_LANE" >/dev/null; then
    echo "  .claude/skills/rama  == upstream + overlay"
  else
    drift=1; echo "  .claude/skills/rama  DRIFT:"; { diff -rq --exclude=UPSTREAM "$WORK/expected-claude" "$CLAUDE_LANE" || true; } | sed 's/^/    /'
  fi
  if diff -r --exclude=UPSTREAM --exclude=agents "$WORK/expected-codex" "$CODEX_LANE" >/dev/null; then
    echo "  .agents/skills/rama  == upstream + overlay + Codex paragraph"
  else
    drift=1; echo "  .agents/skills/rama  DRIFT:"; { diff -rq --exclude=UPSTREAM --exclude=agents "$WORK/expected-codex" "$CODEX_LANE" || true; } | sed 's/^/    /'
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
  *)      awk 'NR > 1 { if (/^#/) print; else exit }' "$0"; exit 1 ;;
esac
