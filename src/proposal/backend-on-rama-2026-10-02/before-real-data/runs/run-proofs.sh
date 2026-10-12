#!/usr/bin/env bash
# Runs the store's proofs one after the other, from the repository root, and keeps for each:
#   raw-<label>-<proof>.txt   the whole output (never opened by a session: it can be very large)
#   <label>-<proof>.txt       the cut: the command, the time, the SHOWN lines once, the count line,
#                             and the exit status
#
# Use:  bash run-proofs.sh <label> [<proof> ...]
#   <label>  a word for the tree that is proven: before, after-<part>, final
#   <proof>  act | pure | module | door | read-path-pure | read-path | rebuild ; all that exist when
#            none is named
#
# A proof's JVM loads the source when it starts. So no file under src-store/ or test-store/ is
# edited while this script still has a proof to start.
#
# A proof that starts Rama's in-process cluster waits for runs/cluster.lock (flock), so that two
# sessions never run two clusters at once. The time that the cut gives for a run holds that wait.
set -u
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="${PROOF_ROOT:-$(cd "$here/../../../../.." && pwd)}"
# PROOF_ROOT names another checkout of the repository to prove, a detached worktree of a commit for
# one. The outputs and the lock stay in this folder, so that every run of the build waits for the
# one lock.
label="${1:?a label for the tree: before, after-<part>, final}"
shift

alias_of() {
  case "$1" in
    act|pure|read-path-pure|rebuild-pure) echo store ;;
    *) echo store-module ;;
  esac
}

proofs=("$@")
if [ "${#proofs[@]}" -eq 0 ]; then
  proofs=(act pure module door read-path-pure read-path)
  [ -f "$root/test-store/softland/store/rebuild_pure_proof.clj" ] && proofs+=(rebuild-pure)
  [ -f "$root/test-store/softland/store/rebuild_proof.clj" ] && proofs+=(rebuild)
fi

cd "$root" || exit 2
status=0
for proof in "${proofs[@]}"; do
  ns="softland.store.${proof}-proof"
  al="$(alias_of "$proof")"
  raw="$here/raw-$label-$proof.txt"
  cut="$here/$label-$proof.txt"
  start=$(date +%s)
  if [ "$al" = store-module ]; then
    # One in-process cluster at a time on this machine: two collide on a port. Every run of this
    # build that starts a cluster takes this lock first, a probe's run too.
    flock "$here/cluster.lock" clojure -M:"$al" -m "$ns" >"$raw" 2>&1
  else
    clojure -M:"$al" -m "$ns" >"$raw" 2>&1
  fi
  code=$?
  end=$(date +%s)
  {
    echo "The proof \"$proof\", on the tree \"$label\" ($(date -u +%Y-%m-%dT%H:%MZ), head $(git rev-parse --short HEAD)), cut to its SHOWN lines once, its count line and its exit status."
    echo "Command, from the repository root: clojure -M:$al -m $ns"
    echo "The whole output is raw-$label-$proof.txt beside this file, and is not committed. The run took $((end - start)) seconds."
    echo
    awk '/^THE PROOF, IN ONE PLACE/ {seen=1} seen && (/^THE PROOF, IN ONE PLACE/ || /^SHOWN/) {print}' "$raw"
    grep -E '^\{:test [0-9]+, :pass [0-9]+, :fail [0-9]+, :error [0-9]+\}' "$raw" | tail -1
    grep -E '^(FAIL in|ERROR in)' "$raw" | head -40
    echo
    echo "exit status: $code"
  } >"$cut"
  echo "$proof $(grep -E '^\{:test' "$cut" | tail -1) exit $code ($((end - start)) s)"
  [ "$code" -ne 0 ] && status=1
done
exit $status
