#!/bin/sh
# Wave 2's one full suite (Sid's test rule): run once, at the end, under the cluster lock.
cd /mnt/data/projects/Softland-rig-wave2/src/proposal/rig-2026-09-25 || exit 2
echo "queued   $(date '+%F %T %Z') at $(git rev-parse --short=8 HEAD) on $(git rev-parse --abbrev-ref HEAD)"
RIG_REPLAY_REPORT=runs/wave2-replays.txt flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock sh -c '
  echo "lock     $(date "+%F %T %Z")"
  clojure -M:test rig.smoke-test rig.claims-test rig.revision-test rig.store.clock-test rig.store.envelope-test rig.store.stream-gate-test rig.store.reads-test rig.store.read-exit-test rig.store.read-model-test rig.store.lock-test rig.store.forget-test rig.store.micro-prepare-test rig.store.micro-test rig.store.wave1-test rig.store.review-wave1-test rig.replay-test rig.store.promote-unit-test rig.store.promote-test rig.store.shared-reads-test rig.store.reads-rest-test rig.store.shape-test rig.store.grammar-test rig.store.recipe-test rig.store.tools-test
  echo "exit     $?"
  echo "end      $(date "+%F %T %Z")"'
