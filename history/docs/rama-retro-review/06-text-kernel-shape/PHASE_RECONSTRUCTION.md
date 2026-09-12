# Phase Reconstruction - Text Kernel Shape

Status: reconstructed.

PHASE_RECONSTRUCTION:complete

This artifact reconstructs the Rama phase pack that should have existed for the
committed kernel-shape and text-kernel split work. It is a retroactive review
aid, not a request to build a new generator or registry.

## Source Evidence

```text
docs/current-mental-model/build/rama-retro-review/06-text-kernel-shape/BRIEF.md
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/10-anchors/rama-world-kernel-text-instance.md
docs/current-mental-model/architecture/action-request-kernel-routing.md

src/app/server/rama/kernel.clj
src/app/server/rama/core.clj
src/app/server/rama/text_kernel.clj
src/app/server/rama/dogfood/compute.clj
src/app/server/rama/dogfood/space.clj
src/app/server/rama/dogfood/llm.clj
src/app/server/rama/dogfood/transcript.clj

test/app/server/rama/text_kernel_test.clj
test/app/server/rama/dogfood_compute_test.clj
test/app/server/rama/dogfood_space_test.clj
test/app/server/rama/dogfood_llm_test.clj
test/app/server/rama/dogfood_transcript_test.clj

Commit anchors:
1c03e70 2026-05-13 kernel: add shape spec and KERNEL-SHAPE data form
1ef1cbd 2026-05-13 rama: split text kernel and rename space
c0dfafe 2026-05-13 docs: update rama space rename context
e751434 2026-05-19 rama: add identity headers to all 5 kernels
```

## Phase 0 - Implicit Spec

The committed work was trying to separate three things that had been folded
together:

```text
General kernel lifecycle:
ActionRequest asks.
ActionDecision records the answer.
KernelEvent happened.

Text carrier:
Text artifacts, revisions, line units, branch status, and projections are the
first concrete carrier instance.

Space carrier:
Chat/local-world state is no longer "world text proof"; it is a separate Space
kernel with turns, context bundles, objects, patches, and LLM dispatch.
```

The user/world problem was not "make a nice namespace map." It was: stop letting
the first text proof accidentally become the ontology. The codebase needed one
shared lifecycle vocabulary that future kernels could copy without re-inventing
request, decision, event, depot, PState, and projection conventions.

The docs imply these invariants:

```text
- The first physical write for a world mutation is an ActionRequest.
- An ActionRequest must not contain a top-level :event/id.
- Accepted requests produce KernelEvents.
- Rejected requests produce durable ActionDecisions with :event/id nil.
- Event identity is Rama-owned or explicitly proposed, never hidden in payload.
- Request routing is explicit through :routing/key.
- :action/type is the canonical operation and must not drift from :request/type.
- Clients append depots; topologies write PStates.
- Text is one carrier, not the general ontology.
- Space is the local-world/work-area kernel, not the old world-text proof.
- Kernel shape should be inspectable Clojure data if it lives in code.
- Identity headers should make real module boundaries legible.
```

Expected failure cases:

```text
- KERNEL-SHAPE becomes comment prose instead of readable data.
- Module headers describe intent but no executable contract checks them.
- One module uses shared ActionRequest/ActionDecision/KernelEvent helpers while
  others invent local variants.
- The World-to-Space rename leaves stale runtime names or stale tests.
- Compatibility adapters keep accepting arbitrary event payloads and become the
  real public contract.
- Existing tests pass because no test imports the shared kernel-shape namespace.
```

## Phase 1 - Plan

The reconstructed plan should have separated descriptive documentation from
executable contract checks.

```text
Files:
src/app/server/rama/core.clj
  Shared envelope builders and validators for ActionRequest, ActionDecision,
  and KernelEvent.

src/app/server/rama/kernel.clj
  Clojure-readable KERNEL-SHAPE data describing the current five committed
  kernels. Data-only is allowed, but it must load and be inspectable.

src/app/server/rama/text_kernel.clj
  First carrier instance using the shared core helpers end to end.

src/app/server/rama/dogfood/space.clj
src/app/server/rama/dogfood/compute.clj
src/app/server/rama/dogfood/llm.clj
src/app/server/rama/dogfood/transcript.clj
  Existing dogfood kernels with identity headers and shape examples.
```

Expected shared shape:

```text
Always present:
- intent depot
- request validator/interpreter
- durable request/decision or run-state materialization
- accepted world facts/events where the kernel participates in world truth
- read helpers shaped around declared PStates

Optional:
- claim depot
- observation depot
- control depot
- task-global executor
- cross-module mirror depots
- projections
```

Expected KERNEL-SHAPE constraints:

```text
- Namespace must load.
- KERNEL-SHAPE must be valid Clojure data.
- The five examples must match committed depot/PState names.
- Optional fields must list which kernels use them.
- Examples must not use unreadable placeholder literals.
- The data form must not silently become stale; at minimum tests should require
  the namespace and assert core examples/counts.
```

Expected rename plan:

```text
world-kernel-module -> text-kernel-module
world-module -> space-kernel-module
*world-requests-depot -> *text-requests-depot
*world-action-depot -> *space-action-depot
world thread/turn vocabulary -> space/turn vocabulary
dogfood_world_test -> dogfood_space_test
world_kernel_test -> text_kernel_test
```

Expected compatibility plan:

```text
Compatibility helpers may exist only as narrow legacy adapters.

Required guardrails:
- compatibility request type is visibly legacy/transitional
- payload is validated against an allowed event set
- target/action/capability are not arbitrary pass-throughs
- tests prove hidden :event/id is still rejected
- tests prove arbitrary event creation is not the supported path
- removal or migration criteria are documented
```

## Phase 2 - Plan Validation

A Rama review should have passed the text-kernel split direction but failed the
shared-shape plan until it had executable validation.

```text
Plan strengths:
- The core ActionRequest/ActionDecision/KernelEvent distinction is correct.
- Text as first carrier, not ontology, is the right conceptual correction.
- World-to-Space rename is directionally correct.
- Identity headers make module boundaries more legible.
- KERNEL-SHAPE as data-only can be a valid intermediate step before generation.

Plan risks:
- Data-only KERNEL-SHAPE creates no runtime contract by itself.
- Header comments can drift immediately if no tests import or inspect them.
- Existing Compute/Space/LLM/Transcript modules already had local request and
  decision conventions, so "one shared lifecycle" required an explicit migration
  or at least an explicit "descriptive only" verdict.
- Compatibility paths need a sharper allowed surface.
- A green module test suite can miss a broken shape namespace if nobody requires
  it.

Verdict the plan should have received:
PASS for Text split and Space rename.
FAIL as a shared kernel contract until KERNEL-SHAPE loads, is tested, and the
contract status of non-Text modules is explicit.
```

## Phase 3 - Expected Implementation Shape

```text
Expected Text module:
- *text-requests-depot partitioned by :routing/key
- $$requests-by-id
- $$decisions-by-id
- $$events-by-id
- $$artifacts
- $$artifact-heads
- $$branches
- $$policies
- $$text-revisions
- $$units-by-artifact
- $$unit-status-by-branch
- $$projection-cache
- ingest request -> accepted/rejected decision -> event only on acceptance
- status request -> reject if target unit missing
- hidden payload :event/id rejected before dispatch

Expected Space module:
- *space-action-depot partitioned by :routing/key
- mirrored LLM depots for run/control dispatch
- Space vocabulary in module, depot, PState, test, and helper names
- request-first Space actions with durable decisions/events

Expected Compute module:
- *compute-depot, *compute-claim-depot, *compute-obs-depot
- request -> decision -> pending run
- claim -> claimed run
- observation -> folded run state

Expected LLM module:
- *llm-depot, *llm-claim-depot, *llm-obs-depot, *llm-control-depot
- Space-derived run request -> decision -> pending run
- claim before observation
- control plane separate from observations

Expected Transcript module:
- *transcript-depot, *transcript-claim-depot, *transcript-obs-depot
- passive request/status/observation materialization
```

Expected validation:

```text
- Requiring app.server.rama.kernel succeeds.
- KERNEL-SHAPE can be inspected with (:kernel/shape KERNEL-SHAPE).
- Tests assert the current five instance names and depot examples.
- Tests assert Text has the shared core envelope behavior.
- Tests assert non-Text modules are either migrated to shared helpers or
  explicitly classified as local variants.
- Tests assert old "world" runtime names are gone from code/test surfaces.
- Tests assert compatibility adapters do not bypass the event lifecycle.
```

## Phase 4 - Expected Implementation Validation

The implementation validation gate should have used both module behavior tests
and shape tests.

```text
Shape commands:
clojure -M:test -e "(require 'app.server.rama.kernel)"

clojure -M:test -e "(require '[app.server.rama.kernel :as k])
                    (println (keys (:kernel/shape k/KERNEL-SHAPE)))"

Module commands:
clojure -M:test -e "(require 'clojure.test
                             'app.server.rama.text-kernel-test
                             'app.server.rama.dogfood-compute-test
                             'app.server.rama.dogfood-space-test
                             'app.server.rama.dogfood-llm-test
                             'app.server.rama.dogfood-transcript-test)
                    (clojure.test/run-tests
                      'app.server.rama.text-kernel-test
                      'app.server.rama.dogfood-compute-test
                      'app.server.rama.dogfood-space-test
                      'app.server.rama.dogfood-llm-test
                      'app.server.rama.dogfood-transcript-test)"
```

## Phase 5 - Test Plan

```text
Tests that should exist:
- kernel namespace loads
- KERNEL-SHAPE is valid inspectable data
- KERNEL-SHAPE includes exactly the committed five examples
- KERNEL-SHAPE depot/PState examples match declarations in the committed modules
- Text requests cannot contain top-level or payload event ids
- Text accepted decisions carry KernelEvents; rejected decisions do not
- Text status requests reject missing units
- Compatibility adapter rejects arbitrary event identities and unsupported event
  shapes
- World-to-Space runtime names are absent from committed code and tests
- Module identity headers have matching namespace/depot/PState facts
```

## Phase 6 - Test Validation

The tests should be judged by whether they can fail the exact drift modes the
split was meant to prevent.

```text
Acceptable:
- happy text loop
- rejection durability
- Space rename behavior
- module-specific Compute/LLM/Transcript tests

Not sufficient:
- tests that never require app.server.rama.kernel
- tests that prove Text only, then infer all kernels share the same lifecycle
- tests that let compatibility helpers mint arbitrary accepted events
- tests that search rename text only in production code but skip tests/helpers
```

## Phase 7 - Finish / Runtime Gate

The finish gate should have been:

```text
1. app.server.rama.kernel loads.
2. KERNEL-SHAPE is inspectable Clojure data.
3. Text kernel tests pass.
4. Compute, Space, LLM, and Transcript committed module tests pass.
5. No stale World-to-Space runtime names remain outside historical documentation.
6. The review record explicitly says whether non-Text modules are shared-contract
   implementations or local variants.
```

In the committed state reviewed here, the module tests pass, but the shared
shape namespace does not load. That means the split shipped useful module work
without finishing the shared kernel-shape contract.
