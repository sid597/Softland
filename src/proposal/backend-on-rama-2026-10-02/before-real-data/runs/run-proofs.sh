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
set -u
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(cd "$here/../../../../.." && pwd)"
label="${1:?a label for the tree: before, after-<part>, final}"
shift

alias_of() {
  case "$1" in
    act|pure|read-path-pure) echo store ;;
    *) echo store-module ;;
  esac
}

proofs=("$@")
if [ "${#proofs[@]}" -eq 0 ]; then
  proofs=(act pure module door read-path-pure read-path)
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
  clojure -M:"$al" -m "$ns" >"$raw" 2>&1
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
