#!/usr/bin/env bash
# Phase 7's driver (PLAN-numbers.md 8.2). Started once, from anywhere, in the
# background:
#
#   nohup test/rig/bench/phase7-final.sh <set> > runs/phase7-final-driver.log 2>&1 &
#
# <set>: test, short (F11, about 20 minutes), min (about 45), full (about 65),
# smoke (one small run of each harness, not run tonight under Sid's rule of 26
# September: set PHASE7_SMOKE=1 and
# PHASE7_OUT=runs/phase7-smoke), or one step's name. Each step is its own JVM
# and its own in-process cluster, under the relay's cluster lock (8.1): the
# lock is taken per step, so another session can use the machine between
# steps. A start and an end line per step go to <out>-progress.log; the step's
# console to <out>-<step>.log; results to <out>-<number>.edn, and `report`
# writes the .txt files. Nothing runs after a failed `test` (a harness that
# does not measure what it claims measures nothing); any other failure is
# logged and the driver goes on (8.7). Nothing is rerun by the script.
set -u

RIG="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$RIG" || exit 2
LOCK=/mnt/data/projects/rig-relay-2026-09-26/cluster.lock
OUT="${PHASE7_OUT:-runs/phase7-final}"
PROG="${OUT}-progress.log"
SET="${1:-min}"

# The order (Sid's rule, 26 September: the minimum set is the harnesses' first
# run, so a broken harness must fail near the start, not late in the set):
# `test` first, which loads every harness and runs T1 to T11 (the store's write
# lists, the windows' counts, the lock-store pick); then the first run of each
# harness, the shortest first (`lock-growth-h40-p3`, about a minute); then the
# repeats. No step depends on another's output.
steps() {
  case "$1" in
    test)  echo "test" ;;
    short) echo "test lock-growth-h40 agent-rate-1 one-thread-1 lock-growth-h200 report" ;;
    min)   echo "test lock-growth-h40-p3 agent-rate-1 one-thread-1" \
                "lock-growth-h40 lock-growth-h200 lock-growth-h40-p2 lock-growth-h40-p5" \
                "agent-rate-2 one-thread-2 agent-rate-3 one-thread-3 report" ;;
    full)  echo "test lock-growth-h40-p3 agent-rate-1 one-thread-1 agent-sessions-1 agent-reads-1" \
                "one-thread-reads lock-growth-h40-act4" \
                "lock-growth-h40 lock-growth-h200 lock-growth-h40-p2 lock-growth-h40-p5" \
                "agent-rate-2 one-thread-2 agent-rate-3 one-thread-3" \
                "agent-sessions-2 agent-sessions-3 agent-reads-2 agent-reads-3 lock-growth-h40-again report" ;;
    smoke) echo "agent-rate-1 agent-sessions-1 agent-reads-1 one-thread-1 one-thread-reads" \
                "lock-growth-h40 lock-growth-h40-p5 lock-growth-h40-act4 report" ;;
    *)     echo "$1" ;;
  esac
}

command_of() {
  case "$1" in
    test)             echo "clojure -M:test rig.bench.numbers-test" ;;
    agent-rate-*)     echo "clojure -M:bench rig.bench.agent-rate run ${1#agent-rate-}" ;;
    agent-sessions-*) echo "clojure -M:bench rig.bench.agent-rate sessions ${1#agent-sessions-}" ;;
    agent-reads-*)    echo "clojure -M:bench rig.bench.agent-rate reads ${1#agent-reads-}" ;;
    one-thread-reads) echo "clojure -M:bench rig.bench.one-thread reads 1" ;;
    one-thread-*)     echo "clojure -M:bench rig.bench.one-thread run ${1#one-thread-}" ;;
    lock-growth-*)    echo "clojure -M:bench rig.bench.lock-growth run ${1#lock-growth-}" ;;
    report)           echo "clojure -M:bench rig.bench.numbers report" ;;
    *)                return 1 ;;
  esac
}

note() { echo "$(date '+%F %T') $*" >> "$PROG"; }

free_gb() { df --output=avail -k "${TMPDIR:-/tmp}" | tail -1 | awk '{print int($1 / 1048576)}'; }

note "set $SET starts (out $OUT${PHASE7_SMOKE:+, small runs})"
for step in $(steps "$SET"); do
  cmd=$(command_of "$step") || { note "unknown step $step"; exit 2; }
  log="${OUT}-${step}.log"
  if [ "$(free_gb)" -lt 10 ]; then note "stop before $step: under 10 GB free under the JVM's temp dir"; exit 3; fi
  note "start $step; load $(cut -d' ' -f1-3 /proc/loadavg); java processes $(pgrep -c java || true)"
  t0=$(date +%s)
  if [ "$step" = report ]; then
    $cmd > "$log" 2>&1
    code=$?
  else
    if ! flock -n "$LOCK" true; then note "$step waiting for the cluster lock"; fi
    flock "$LOCK" $cmd > "$log" 2>&1
    code=$?
  fi
  note "end $step; exit $code; $(( $(date +%s) - t0 )) s"
  if [ "$step" = test ] && [ "$code" -ne 0 ]; then
    note "stop: the harness test failed, so nothing is measured"
    exit 1
  fi
done
note "set $SET done"
