# Object Container Common Infra - Retro Fix Brief

Origin prompt:

> "IS there a doc or smth i can see what is changed and why both from the product pov and the engineering like this all is too much for me to grok"

Date: 2026-06-08

## One-Screen Summary

The work was trying to make Object Container the common substrate for imported material, not just a transcript-shaped system with container names.

The earlier implementation moved in the right direction, but it still had the old Rama failure pattern: a happy-path row existed, yet the product path was not fully wired, rejection was weak, watch behavior skipped real bytes, and completion could be reported before the durable truth was queryable.

The fix made transcript harvest and watch use the common Object Container import path as a real product flow:

```text
transcript file line
  -> transcript observation
  -> common Object Container import request
  -> accepted/rejected common decision
  -> common containers/revisions/anchors/edges/projections
  -> transcript operational resume offset advances only after common completion
```

Product meaning: chat transcript history is now becoming native Object Container material, with conversation/message/tool topology and resume state tied to common truth.

Engineering meaning: the helper does not just append requests and hope. It waits for accepted common decisions, waits for source-line completion, advances offsets only when common completion matches, and fails the transcript run when those guarantees break.

## Product POV

### What changed for the product

Before, markdown import had a real path into common Object Container, but transcript import was only partially common. Transcript observations could be converted into common import requests, but the existing harvest/watch product paths could still behave like old transcript-local systems.

Now the product-level flow is:

```text
Harvest existing transcript files
  -> import each observed line into common Object Container
  -> expose conversation/message/tool projection from common Object Container
  -> update file resume offset only after common import completed

Watch live transcript files
  -> detect appended complete JSONL lines
  -> import through the same common path
  -> expose the same common projection
  -> advance the same safe resume offset
```

This matters because Softland needs one substrate for material, provenance, and relations. A transcript message should not live in a private transcript island if the rest of the world is reading Object Containers.

### What user-visible bug class this prevents

The old shape could say "harvest complete" even if common Object Container rejected one of the imported messages. That would create a false product state: the transcript run looked done, but the common world was missing material.

Now rejected common import fails the transcript run, and the run row carries the common import failure reason.

### What changed in the transcript graph

The transcript common path now preserves the product graph contract more concretely:

```text
conversation contains message
message follows previous message
assistant message produced tool call
tool call produced tool result
```

The tests now check the important shape through the common read APIs, not only through helper return values.

## Engineering POV

### Main repaired boundaries

1. Common import decision is authoritative.

Transcript harvest/watch now append common Object Container import requests and wait for the decision. If the decision is not accepted, the transcript run is marked failed.

Evidence:

```text
src/app/server/rama/dogfood/transcript.clj
  import-observations-into-object-container!
  transcript-import-error
```

2. Source-line completion is the bridge between common truth and operational resume state.

The transcript adapter now annotates file-state lines with the exact common import key, material fingerprint, source-line key, and common order key. File offset advancement only succeeds when the observed line matches the completed common source-line row.

Evidence:

```text
src/app/server/rama/dogfood/transcript.clj
  common-transcript-source-line
  common-source-line-completion-matches?
  await-common-source-line-completion
  append-and-await-object-container-file-state!
```

3. Mirror lag is treated as a real distributed-systems condition.

The ops module reads completed source-line rows through a mirror. A common import decision can become visible before the ops mirror has caught up. The adapter now performs bounded idempotent file-state re-appends and only reports success once the safe offset advances.

4. Watch startup now snapshots the baseline offset.

The old watch path computed the starting offset lazily on first poll. If a file existed at watch start, then bytes were appended before the first poll, the first poll could treat the appended bytes as the baseline and skip them.

Now watch records starting offsets when the watcher starts.

Evidence:

```text
src/app/server/rama/dogfood/transcript.clj
  start-transcript-watch-into-object-container!

src/app/server/rama/dogfood/transcript_ingest.clj
  start-watch-ingest!
```

5. Legacy harvest completion now waits for materialization.

The old transcript-ingest harvest could append observation events and then append terminal `:complete` before observation-derived projection/audit rows finished. Since terminal statuses are monotonic, late progress could be ignored.

Now legacy harvest counts appended observations and waits until the run row reflects them before appending `:complete`.

Evidence:

```text
src/app/server/rama/dogfood/transcript_ingest.clj
  observation-count
  await observed-line-count before complete
```

6. Rama mirror select API misuse was fixed.

The ops topology had `:allow-yield?` on a mirror `select>`, which Rama rejected at runtime. That option is valid for large local PState reads, not for mirror selects.

Evidence:

```text
src/app/server/rama/object_container.clj
  transcript-operational-control-topology
```

## Why The Earlier Phases Missed It

The phases proved the new common path existed. They did not sufficiently prove the product path was correct under the old failure modes.

What was missing:

```text
Happy path existed
  but rejection was not tested.

Harvest existed
  but watch was not tested through the common path.

Rows eventually materialized
  but helpers could return before truth was queryable.

File offsets existed
  but offset safety was not tied to common source-line completion.

The common module and ops module launched
  but mirror lag and mirror API limits were not exercised.
```

That is the exact retro-review lesson: do not accept "row exists" as proof that the world contract is true.

## Tests Added Or Strengthened

The focused regression net is in:

```text
test/app/server/rama/object_container_test.clj
```

Important tests:

```text
transcript-harvest-uses-common-object-container-path-test
  imports transcript harvest through common Object Container
  checks conversation projection
  checks follows/tool-result graph shape
  checks file offset reaches file length

transcript-harvest-fails-when-common-import-rejects-test
  creates a native identity conflict
  checks transcript run becomes :failed
  checks failure reason comes from common Object Container

transcript-watch-uses-common-object-container-path-test
  starts watch on an existing file
  appends a line after startup
  checks common projection appears
  checks safe file offset advances
```

Focused verification run:

```text
clojure -M:test -e "(do (require '[clojure.test :as t] 'app.server.rama.object-container-test) (let [r (t/run-tests 'app.server.rama.object-container-test)] (shutdown-agents) (System/exit (if (or (pos? (:fail r)) (pos? (:error r))) 1 0))))"

clojure -M:test -e "(do (require '[clojure.test :as t] 'app.server.rama.dogfood-transcript-test) (let [r (t/run-tests 'app.server.rama.dogfood-transcript-test)] (shutdown-agents) (System/exit (if (or (pos? (:fail r)) (pos? (:error r))) 1 0))))"

clojure -M:test -e "(do (require '[clojure.test :as t] 'app.server.rama.dogfood.transcript-ingest-test) (let [r (t/run-tests 'app.server.rama.dogfood.transcript-ingest-test)] (shutdown-agents) (System/exit (if (or (pos? (:fail r)) (pos? (:error r))) 1 0))))"
```

All three focused suites passed.

## What Still Feels Too Big

`src/app/server/rama/object_container.clj` is still large, even after runtime/test IPC helpers moved into:

```text
src/app/server/rama/object_container/runtime.clj
```

Size alone is not the verdict. Rama modules often keep schema, topology, row constructors, and queries close together.

But this file is now carrying multiple ownership zones:

```text
Common Object Container kernel
  common import/edit decisions
  containers, revisions, anchors, composition edges
  native identity and idempotency

Transcript adapter materialization
  transcript observations -> common import payload
  conversation/message/tool projection hints
  transcript source-line status hints

Transcript operational control
  harvest/watch run rows
  file-state depot
  safe resume offsets
  mirror of common transcript source-line completions
```

That is not just "a large file" smell. It is the same ownership-collapse risk the retro-review warned about.

## Recommended Next Repair

Do not split cosmetically.

Split only along real ownership:

```text
object_container.clj
  common Object Container kernel

object_container/transcript_adapter.clj
  transcript observation -> common material request
  transcript graph/projection/source-line payload builders

object_container/transcript_ops.clj
  transcript run/file-state operational module
  safe offset advancement
  source-line completion mirror handling

object_container/runtime.clj
  test/dev IPC launch and read helpers
```

The reason to split is not "file length." The reason is to keep the common kernel source-neutral and make transcript-specific product/ops ownership explicit.

## Mental Model

The fixed rule is:

```text
Do not mark transcript work complete because transcript lines were observed.
Mark it complete only after common Object Container accepted the material
and operational resume state is safe to continue from.
```

That one sentence is the product and engineering contract.
