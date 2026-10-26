#!/usr/bin/env bash
# Ground check, part 1: from the detached checkout of HEAD that holds the fixture of
# 0d24b03e, run the run-again proof, then the full comparison script, each under
# the cluster lock. A run, not code: it changes nothing.
set -u
ROOT=/mnt/data/projects/Softland
CHECK=/mnt/data/projects/Softland-first-maker-check
RUNS=$ROOT/src/proposal/first-maker-2026-10-14/runs
LOCK=$ROOT/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock
cd "$CHECK" || exit 1
echo "checkout $CHECK at $(git rev-parse --short HEAD); differing files:"; git status --short
s=$(date +%s)
flock "$LOCK" clojure -M:store-module -m softland.store.run-again-proof > "$RUNS/raw-ground-1-run-again-old-fixture.txt" 2>&1
echo "exit status: $?" >> "$RUNS/raw-ground-1-run-again-old-fixture.txt"
echo "proof done in $(( $(date +%s) - s )) s including any wait for the lock"
s=$(date +%s)
flock "$LOCK" clojure -M:store-module "$RUNS/ground-1-compare.clj" > "$RUNS/ground-1-compare-output.txt" 2>&1
echo "exit status: $?" >> "$RUNS/ground-1-compare-output.txt"
echo "compare done in $(( $(date +%s) - s )) s including any wait for the lock"
