#!/usr/bin/env bash
# usage: run_proofs.sh <tree-dir> <out-dir>
# Runs every proof that needs no outside cluster, one after the other, and times each.
cd "$1" || exit 2
out="$2"; mkdir -p "$out"; : > "$out/summary.txt"
run() { # alias namespace
  local name="${2##*.}-$1"; local t0=$(date +%s)
  clojure -M:"$1" -m "$2" > "$out/$1__$2.txt" 2>&1; local rc=$?
  local t1=$(date +%s)
  printf '%-14s %-44s rc=%s  %4ss  PASSED=%s FAILED=%s\n' "$1" "$2" "$rc" "$((t1-t0))" \
    "$(grep -c '^PASSED' "$out/$1__$2.txt")" "$(grep -c '^FAILED' "$out/$1__$2.txt")" >> "$out/summary.txt"
}
T0=$(date +%s)
run store        softland.store.act-proof
run store        softland.store.pure-proof
run store        softland.store.read-path-pure-proof
run wire         softland.wire.pure-proof
run runtime      softland.runtime.pure-proof
run auth         softland.auth.reference-proof
run store-module softland.store.module-proof
run store-module softland.store.door-proof
run store-module softland.store.read-path-proof
run store-module softland.store.run-again-proof
run auth         softland.auth.module-check
run runtime      softland.runtime.proof
run runtime      softland.runtime.command-proof
echo "TOTAL $(( $(date +%s) - T0 ))s" >> "$out/summary.txt"
