#!/bin/zsh
# One run of the probe, behind the lock that every in-process cluster on this
# machine waits on. Run from this folder, in one of three ways:
#
#   ./run.sh <log name> <scenario> [key value ...]   one scenario
#   ./run.sh <log name> cost [key value ...]          the cost run
#   ./run.sh suite <suite name>                       the scenarios of suites/<suite name>.edn,
#                                                     one after another in one JVM, the lock taken once
#
# The JVM's whole stdout goes to logs/<log name>-stdout.txt (for a suite:
# logs/suite-<suite name>-stdout.txt). The probe's own lines also go to
# logs/<log name>.log, one log for each scenario. Rama's own ERROR and WARN
# lines of each scenario are then put, each distinct line once with its
# count, into logs/<log name>-rama-lines.log.
cd "${0:A:h}"
lock=/mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock

# Takes a log name, and on its stdin the stdout of that scenario.
rama_lines() {
  {
    echo "Rama's own ERROR and WARN lines of the run \"$1\" (from the JVM's stdout): each distinct line once, its count in front, the time of day and the thread's name taken off, cut to 300 characters."
    grep -E " (ERROR|WARN) " | grep -v "^PROBE" \
      | sed -E 's/^[0-9:., -]+//; s/^\[[^]]*\] *//' | cut -c1-300 | sort | uniq -c | sort -rn
  } > logs/$1-rama-lines.log
}

if [[ $1 == suite ]]; then
  out=logs/suite-$2-stdout.txt
  flock $lock clojure -M:order suite $2 > $out 2>&1
  echo "exit $?" >> $out
  for name in $(grep "^PROBE SUITE begin " $out | awk '{print $4}'); do
    awk -v b="PROBE SUITE begin $name" -v e="PROBE SUITE end $name" '$0==b {on=1} on {print} $0==e {on=0}' $out | rama_lines $name
  done
else
  name=$1
  shift
  alias_name=order
  if [[ $1 == cost ]]; then alias_name=cost; shift; fi
  flock $lock clojure -M:$alias_name "$@" :log $name > logs/$name-stdout.txt 2>&1
  echo "exit $?" >> logs/$name-stdout.txt
  rama_lines $name < logs/$name-stdout.txt
fi
