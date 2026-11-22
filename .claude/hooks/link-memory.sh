#!/bin/bash
root="${CLAUDE_PROJECT_DIR:-$PWD}"
enc=$(printf '%s' "$root" | sed 's#[/.]#-#g')
tgt="$HOME/.claude/projects/$enc/memory"; src="$root/.claude/memory"
[ -L "$tgt" ] && exit 0
mkdir -p "$(dirname "$tgt")"; [ -d "$tgt" ] && rmdir "$tgt" 2>/dev/null
[ -e "$tgt" ] || ln -s "$src" "$tgt"
