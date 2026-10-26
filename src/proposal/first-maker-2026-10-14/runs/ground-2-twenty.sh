#!/usr/bin/env bash
# Ground check, part 2: the read-path proof run twenty times, each run under the
# cluster lock, from the main repository root. A run, not code: it changes nothing.
# Each run's whole output goes to raw-ground-2-read-path-NN.txt (never opened; read
# only by grep below); the cut goes to ground-2-read-path-20-runs.txt.
set -u
ROOT=/mnt/data/projects/Softland
RUNS=$ROOT/src/proposal/first-maker-2026-10-14/runs
LOCK=$ROOT/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock
OUT=$RUNS/ground-2-read-path-20-runs.txt
cd "$ROOT" || exit 1
{
  echo "Ground check part 2: clojure -M:store-module -m softland.store.read-path-proof, twenty runs, each under flock $LOCK, from $ROOT, HEAD $(git -C "$ROOT" rev-parse --short HEAD)."
  echo "Per run: the PASSED/FAILED line of the check 'through three layers', the SHOWN lines holding 'a layer that joins' or 'farther orders' (each distinct line once; the proof prints each SHOWN line twice, inline and in its recap), the count line, every other FAILED line, and for a failing run the clojure.test FAIL report of that check (lines cut to 1500 characters), then the exit status."
  echo
} > "$OUT"
for i in $(seq -w 1 20); do
  RAW=$RUNS/raw-ground-2-read-path-$i.txt
  start=$(date +%s)
  flock "$LOCK" clojure -M:store-module -m softland.store.read-path-proof > "$RAW" 2>&1
  st=$?
  echo "exit status: $st" >> "$RAW"
  end=$(date +%s)
  {
    echo "=== run $i (started $(date -u -d @$start +%H:%M:%SZ), $((end-start)) s including any wait for the lock)"
    grep -E '^(PASSED|FAILED) through three layers' "$RAW" | cut -c1-1500
    grep -E '^SHOWN' "$RAW" | grep -E 'a layer that joins|farther orders' | awk '!seen[$0]++' | cut -c1-1500
    grep -E '^\{:test' "$RAW"
    grep -E '^(FAILED|ERROR in)' "$RAW" | grep -v 'through three layers' | cut -c1-1500
    if grep -qE '^FAILED through three layers' "$RAW"; then
      echo "--- the FAIL report of that check:"
      grep -n -A6 -E '^FAIL in' "$RAW" | grep -B1 -A5 'through three layers' | cut -c1-1500 | head -20
    fi
    grep -E '^exit status' "$RAW"
    echo
  } >> "$OUT"
done
echo "all twenty runs done $(date -u +%H:%M:%SZ)" >> "$OUT"
