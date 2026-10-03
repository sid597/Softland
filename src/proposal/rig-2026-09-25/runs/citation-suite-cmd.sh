#!/bin/sh
# The citation's one full suite (27 September 2026, BUILD_NOTES-citation.md):
# the rig's 26 namespaces of step 6b's suite, the spec fixes' tests, and the
# citation's 5, in one JVM, one in-process cluster at a time. On the MacBook
# Air there is no cluster lock file: the turns are agreed with the pointer
# session. The full log goes to runs/citation-suite.log (not kept in git); a
# summary to runs/citation-suite.txt.
cd "$(dirname "$0")/.." || exit 2
{
  echo "start    $(date '+%F %T %Z') at $(git rev-parse --short=8 HEAD) on $(git rev-parse --abbrev-ref HEAD), $(git status --short . | wc -l | tr -d ' ') paths uncommitted"
  RIG_REPLAY_REPORT=runs/citation-suite-replays.txt clojure -M:test \
    rig.smoke-test rig.claims-test rig.revision-test rig.store.clock-test rig.store.envelope-test \
    rig.store.stream-gate-test rig.store.reads-test rig.store.read-exit-test rig.store.read-model-test \
    rig.store.lock-test rig.store.forget-test rig.store.micro-prepare-test rig.store.micro-test \
    rig.store.wave1-test rig.store.review-wave1-test rig.replay-test rig.store.promote-unit-test \
    rig.store.promote-test rig.store.shared-reads-test rig.store.reads-rest-test rig.store.shape-test \
    rig.store.grammar-test rig.store.recipe-test rig.store.tools-test rig.store.review-fixes-test \
    rig.store.grammar-micro-test rig.store.spec-fixes-test \
    rig.material-test rig.cite-test rig.store.recipe-citation-test rig.store.dependents-test rig.cite-tools-test \
    > runs/citation-suite.log 2>&1
  status=$?
  echo "exit     $status"
  echo "end      $(date '+%F %T %Z')"
  echo "--- per namespace, failures and errors, totals"
  grep -E '^Testing |^FAIL in|^ERROR in|^Ran |failures|^\{:test' runs/citation-suite.log
  exit $status
} > runs/citation-suite.txt 2>&1
