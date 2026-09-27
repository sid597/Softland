#!/bin/sh
# The rig's one full suite on the merged tree (this branch with the citation
# session's): the citation session's list (its runs/citation-suite-cmd.sh), and
# this build's screen test. One JVM, one in-process cluster; on the MacBook Air
# the turns on the cluster are agreed with the citation session. The full log
# goes to ../runs/merged-suite.log (not kept in git); a summary to
# ../runs/merged-suite.txt.
#   sh src/proposal/pointer-screen-2026-09-27/tools/merged_suite.sh
here="$(cd "$(dirname "$0")" && pwd)"
runs="$here/../runs"
cd "$here/../../rig-2026-09-25" || exit 2
{
  echo "start    $(date '+%F %T %Z') at $(git rev-parse --short=8 HEAD) on $(git rev-parse --abbrev-ref HEAD), $(git status --short . | wc -l | tr -d ' ') paths uncommitted"
  RIG_REPLAY_REPORT="$runs/merged-suite-replays.txt" clojure -M:test \
    rig.smoke-test rig.claims-test rig.revision-test rig.store.clock-test rig.store.envelope-test \
    rig.store.stream-gate-test rig.store.reads-test rig.store.read-exit-test rig.store.read-model-test \
    rig.store.lock-test rig.store.forget-test rig.store.micro-prepare-test rig.store.micro-test \
    rig.store.wave1-test rig.store.review-wave1-test rig.replay-test rig.store.promote-unit-test \
    rig.store.promote-test rig.store.shared-reads-test rig.store.reads-rest-test rig.store.shape-test \
    rig.store.grammar-test rig.store.recipe-test rig.store.tools-test rig.store.review-fixes-test \
    rig.store.grammar-micro-test rig.store.spec-fixes-test \
    rig.material-test rig.cite-test rig.store.recipe-citation-test rig.store.dependents-test rig.cite-tools-test \
    rig.store.screen-test \
    > "$runs/merged-suite.log" 2>&1
  status=$?
  echo "exit     $status"
  echo "end      $(date '+%F %T %Z')"
  echo "--- per namespace, failures and errors, totals"
  grep -E '^Testing |^FAIL in|^ERROR in|^Ran |failures|^\{:test' "$runs/merged-suite.log"
  exit $status
} > "$runs/merged-suite.txt" 2>&1
