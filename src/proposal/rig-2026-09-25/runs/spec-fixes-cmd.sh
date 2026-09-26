#!/bin/sh
# The spec fixes (H-1, M-1): one run of the namespaces the fixes touch, once,
# under the machine-wide cluster lock (Sid's test rule; C's instruction).
# Touched: every test namespace that loads a source namespace the fixes
# changed (micro, micro-client, read-exit, standing, shared-reads, recipe,
# runner) and exercises the changed paths, plus the new spec-fixes-test.
# Not run: the stream-only namespaces, which load the module but take no
# changed path (smoke, claims, revision, clock, envelope, stream-gate, reads,
# lock, forget, promote-unit, shape, grammar).
cd "$(dirname "$0")/.." || exit 2
echo "queued   $(date '+%F %T %Z') at $(git rev-parse --short=8 HEAD) on $(git rev-parse --abbrev-ref HEAD)"
RIG_REPLAY_REPORT=runs/spec-fixes-replays.txt flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock sh -c '
  echo "lock     $(date "+%F %T %Z")"
  clojure -M:test rig.store.spec-fixes-test rig.store.micro-prepare-test rig.store.review-fixes-test rig.store.recipe-test rig.store.shared-reads-test rig.store.micro-test rig.store.grammar-micro-test rig.store.read-exit-test rig.store.read-model-test rig.store.reads-rest-test rig.store.tools-test rig.store.wave1-test rig.store.review-wave1-test rig.store.promote-test rig.replay-test
  suite_status=$?
  echo "exit     $suite_status"
  echo "end      $(date "+%F %T %Z")"
  exit "$suite_status"'
