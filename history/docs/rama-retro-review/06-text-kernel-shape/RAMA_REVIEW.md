# Rama Review - Text Kernel Shape

Status: reviewed.

RETRO_RAMA_REVIEW:major-fail

## Contract Restatement

This block asks whether the committed split preserved one shared Rama lifecycle
contract across Text, Space, Compute, LLM, and Transcript, or whether the split
left parallel local conventions behind.

```text
Intended shared lifecycle:
ActionRequest asks.
ActionDecision records acceptance or rejection.
KernelEvent records accepted world fact.
Topology-owned PStates materialize accepted truth and projections.

Intended text correction:
Text is the first carrier instance, not the ontology.

Intended kernel-shape correction:
KERNEL-SHAPE is Clojure-readable, inspectable data describing the current five
committed kernel instances.
```

The committed direction is right, but the shared kernel-shape contract is not
actually finished. The most important issue is blunt: the `app.server.rama.kernel`
namespace does not load, so the central `KERNEL-SHAPE` artifact is not
inspectable Clojure data.

## Rama-First Reconstruction

```text
New data:
Kernel-shape metadata, shared core envelope helpers, text-kernel request/event
records, Space rename records, and module identity headers.

Entity owning local ordering:
Text routes by [:artifact artifact-id]. Space routes by [:space space-id].
Compute/LLM route by run id. Transcript routes request/status by request id and
observations by ingest request id in the committed code.

First physical depot record:
Text and Space: ActionRequest-style record.
Compute and LLM: run request record.
Transcript: transcript harvest/watch request.

Request/proposal vs accepted fact:
Text cleanly preserves request -> decision -> event. Other modules have local
variants of request/decision/event or request/run/status.

Authoritative decision:
Rama topologies, not client helpers. But the common decision contract exists
only as convention outside Text.

Fast reads:
Per-module PStates and read helpers. There is no shared shape query because
KERNEL-SHAPE cannot be required.
```

## Phase Diagnosis

```text
Phase 0 - Implicit spec:
Strong. The source docs correctly distinguish ActionRequest, ActionDecision, and
KernelEvent, and correctly say text is not the ontology.

Phase 1 - Plan:
Mixed. Data-only KERNEL-SHAPE is an acceptable intermediate step, but the plan
needed a load/inspection test and an explicit statement that non-Text modules
were still local variants.

Phase 2 - Plan validation:
Should have failed until the shared shape namespace loaded and until tests
guarded drift between KERNEL-SHAPE and the committed modules.

Phase 3 - Implementation:
Text split and Space rename mostly landed. The KERNEL-SHAPE implementation is
unreadable, compatibility remains broad, and the non-Text modules retain local
request/decision contracts.

Phase 4 - Implementation validation:
The committed module tests pass, but they do not require the shared kernel-shape
namespace.

Phase 5 - Tests:
Text has good envelope tests. There is no test for KERNEL-SHAPE loadability or
for shape/example parity across the five kernels.

Phase 6 - Test validation:
Green tests prove module behavior, not the shared contract.

Phase 7 - Finish/runtime:
Finish gate should have caught the unreadable namespace before commit.
```

Primary Failure Phase: phase-3

## Files Reviewed

### Phase Reconstruction

```text
docs/current-mental-model/build/rama-retro-review/06-text-kernel-shape/BRIEF.md
docs/current-mental-model/build/rama-retro-review/06-text-kernel-shape/PHASE_RECONSTRUCTION.md
```

### Code

```text
src/app/server/rama/kernel.clj
src/app/server/rama/core.clj
src/app/server/rama/text_kernel.clj
src/app/server/rama/dogfood/compute.clj
src/app/server/rama/dogfood/space.clj
src/app/server/rama/dogfood/llm.clj
src/app/server/rama/dogfood/transcript.clj
```

### Tests

```text
test/app/server/rama/text_kernel_test.clj
test/app/server/rama/dogfood_compute_test.clj
test/app/server/rama/dogfood_space_test.clj
test/app/server/rama/dogfood_llm_test.clj
test/app/server/rama/dogfood_transcript_test.clj
```

### Docs

```text
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/10-anchors/rama-world-kernel-text-instance.md
docs/current-mental-model/architecture/action-request-kernel-routing.md
```

### Commit Anchors

```text
1c03e70 2026-05-13 kernel: add shape spec and KERNEL-SHAPE data form
1ef1cbd 2026-05-13 rama: split text kernel and rename space
c0dfafe 2026-05-13 docs: update rama space rename context
e751434 2026-05-19 rama: add identity headers to all 5 kernels
```

## Shape / Module Inventory

### Shared Core

```text
ActionRequest builder:
src/app/server/rama/core.clj:123-159

KernelEvent builder:
src/app/server/rama/core.clj:161-188

Request validation:
src/app/server/rama/core.clj:198-250

Event validation:
src/app/server/rama/core.clj:252-280

Accepted/rejected decisions:
src/app/server/rama/core.clj:303-324
```

### Text Kernel

```text
Identity header:
src/app/server/rama/text_kernel.clj:14-30

Shared envelope helpers used:
src/app/server/rama/text_kernel.clj:107-218

Compatibility event adapter:
src/app/server/rama/text_kernel.clj:220-277

Topology and PStates:
src/app/server/rama/text_kernel.clj:395-499

Read helpers:
src/app/server/rama/text_kernel.clj:538-570
```

### Non-Text Kernels

```text
Compute local request/decision:
src/app/server/rama/dogfood/compute.clj:78-264

Space local request/decision:
src/app/server/rama/dogfood/space.clj:109-290

LLM local request/decision:
src/app/server/rama/dogfood/llm.clj:173-376

Transcript local request/run-state:
src/app/server/rama/dogfood/transcript.clj:78-154
```

## Findings

### F1. `app.server.rama.kernel` Does Not Load

Severity: critical

Evidence:
- `src/app/server/rama/kernel.clj:481-506` says `defkernel` stores the shape as
  inspectable Clojure data and does not generate code.
- `src/app/server/rama/kernel.clj:510-523` says `KERNEL-SHAPE` is the current
  observed shape drawn from the five instances.
- `src/app/server/rama/kernel.clj:636` contains
  `{:opts...}` inside a quoted example: `(declare-object setup *X-executor
  (X-executor-task-global {:opts...}))`.
- Clojure still reads quoted forms. A map literal must contain an even number of
  forms, so the namespace fails before any data can be inspected.

Command:

```text
clojure -M:test -e "(require 'app.server.rama.kernel)"
```

Observed result:

```text
Syntax error reading source at (app/server/rama/kernel.clj:636:88).
Map literal must contain an even number of forms
```

Rama concern:
The supposed shared kernel-shape artifact is not executable, inspectable, or
testable. This defeats the main value of putting `KERNEL-SHAPE` in code instead
of prose.

Failure phase:
phase-3

Consequence:
Any agent or future developer following the identity headers into
`app.server.rama.kernel` hits a read-time failure. The shared shape cannot serve
as a reliable pattern for the next Rama module.

Repair shape:
Make every quoted example reader-valid Clojure data, then add a test that
requires `app.server.rama.kernel` and inspects at least the top-level
`:kernel/shape` keys and the five committed instance examples.

### F2. The Shared Lifecycle Exists For Text, But Not As One Code Contract Across Kernels

Severity: high

Evidence:
- `docs/current-mental-model/10-anchors/rama-world-kernel-text-instance.md:31-41`
  defines the lifecycle as ActionRequest asks, ActionDecision records the
  answer, KernelEvent happened.
- `docs/current-mental-model/10-anchors/rama-world-kernel-text-instance.md:166-202`
  says the first physical record is an ActionRequest and Rama derives the
  accepted KernelEvent or rejected ActionDecision.
- `src/app/server/rama/core.clj:123-324` implements shared builders and
  validators for ActionRequest, KernelEvent, and decisions.
- `src/app/server/rama/text_kernel.clj:107-277` uses the shared helper shape for
  Text requests, decisions, and events.
- `src/app/server/rama/dogfood/compute.clj:78-264` implements a local run
  request, local validation, local run event, and local decision functions.
- `src/app/server/rama/dogfood/space.clj:109-290` implements local
  `space-action-request`, local validation, and local accepted/rejected
  decisions.
- `src/app/server/rama/dogfood/llm.clj:173-376` implements local LLM run
  requests and decisions.
- `src/app/server/rama/dogfood/transcript.clj:78-154` implements transcript
  requests and run rows without the shared ActionDecision/KernelEvent envelope.

Rama concern:
The phase intent was to prevent "text proof became ontology." Text now follows
the general envelope, but the rest of the committed modules remain parallel
local conventions. That is not automatically wrong for every domain, but it is
not one shared lifecycle contract.

Failure phase:
phase-2

Consequence:
Later reviews can say "all kernels are request-first" and still miss that
request/decision/event semantics differ by module. This is exactly how the
duplicate request, unauthorized observation, approval, and transcript-capture
failures in earlier blocks can coexist with a plausible-looking kernel shape.

Repair shape:
Choose one of two explicit states:

```text
Option A:
Migrate Compute, Space, LLM, and Transcript toward shared core envelope helpers
where the shared lifecycle is meant to be real.

Option B:
Keep them as local variants, but mark KERNEL-SHAPE as descriptive taxonomy and
remove claims that all intent depots fold ActionRequests into ActionDecisions and
KernelEvents.
```

Either way, add tests that encode the chosen status.

### F3. The Text Compatibility Path Is Too Broad For A Transitional Adapter

Severity: high

Evidence:
- `src/app/server/rama/core.clj:338-353` builds a `:compat/record` request with
  caller-provided event type, action type, target, capability, and payload.
- `src/app/server/rama/core.clj:355-393` turns that request into a KernelEvent
  after only common request/event validation.
- `src/app/server/rama/text_kernel.clj:220-277` repeats the same compatibility
  request-to-event path in the Text module.
- `src/app/server/rama/text_kernel.clj:476-491` accepts `:compat/record` in the
  topology and writes the resulting event into `$$events-by-id`.
- `src/app/server/rama/core.clj:252-280` validates generic envelope shape but
  does not restrict compatibility event types or payload schemas to a narrow
  legacy allowlist.

Rama concern:
Compatibility records are accepted KernelEvents. Without a narrow allowed set,
the adapter can become a generic event minting path.

Failure phase:
phase-1

Consequence:
The code preserves the request/decision/event distinction syntactically, but it
does not prevent legacy compatibility from becoming the real public contract for
arbitrary events.

Repair shape:
Make compatibility visibly temporary and narrow:

```text
- define allowed compatibility event types
- validate payload shape per allowed type
- preserve explicit legacy provenance
- add tests that arbitrary event types are rejected
- document removal/migration criteria
```

### F4. Identity Headers Are Useful Comments, Not Contracts

Severity: medium

Evidence:
- Identity headers were added to Text, Compute, Space, LLM, and Transcript.
- Each header points readers to `app.server.rama.kernel`.
- Search over committed Rama code/tests finds `app.server.rama.kernel` only in
  comments outside `kernel.clj`; no module imports it and no test requires it.

Observed search result:

```text
src/app/server/rama/text_kernel.clj:29
src/app/server/rama/dogfood/transcript.clj:30
src/app/server/rama/dogfood/llm.clj:32
src/app/server/rama/dogfood/compute.clj:29
src/app/server/rama/dogfood/space.clj:36
```

Rama concern:
Comments are helpful for navigation, but they do not guard depot shapes,
partitioning, PState names, request contracts, or retry semantics. Because the
target namespace does not load, the comments currently point to a broken
contract artifact.

Failure phase:
phase-4

Consequence:
The headers improve readability but can give a false sense that the modules are
checked against a shared shape.

Repair shape:
Keep the headers, but pair them with an executable shape-load test and a small
shape parity test. If the headers remain purely descriptive, say that explicitly.

### F5. Tests Pass While The Shared Shape Is Broken

Severity: medium

Evidence:
- `test/app/server/rama/text_kernel_test.clj:6-61` validates the Text
  ActionRequest/KernelEvent envelope and rejects hidden payload event ids.
- `test/app/server/rama/text_kernel_test.clj:76-136` validates the Text Rama loop.
- `test/app/server/rama/text_kernel_test.clj:138-188` validates hidden event-id
  and malformed unknown-action rejection.
- The committed Compute, Space, LLM, and Transcript tests pass with their local
  contracts.
- No test imports `app.server.rama.kernel`.

Command:

```text
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
                      'app.server.rama.dogfood-transcript-test)
                    (shutdown-agents)
                    (System/exit 0)"
```

Observed result:

```text
Ran 52 tests containing 447 assertions.
0 failures, 0 errors.
{:test 52, :pass 447, :fail 0, :error 0, :type :summary}
```

Rama concern:
Green IPC tests are valuable, but this test set cannot validate the thing this
block is about: shared kernel shape. It proves individual module behavior and
misses the broken shape namespace.

Failure phase:
phase-5

Consequence:
The code can appear review-clean while the central shared-shape artifact is
unloadable and untested.

Repair shape:
Add a focused shape test:

```clojure
(deftest kernel-shape-loads-test
  (require 'app.server.rama.kernel)
  ;; inspect KERNEL-SHAPE and verify the five committed instances.
  )
```

Then add parity checks for depot names, optional depots, and PState counts if the
shape is meant to stay in sync with the committed modules.

## Non-Findings / Green Checks

### World-To-Space Rename Is Mostly Aligned In Runtime Code

The direct code/test search for `world`, `World`, and `world-` found runtime
matches only in historical rename comments and local-world prose, not stale
runtime namespaces or test names.

```text
src/app/server/rama/kernel.clj:349-430 contains the rename table.
src/app/server/rama/dogfood/space.clj:13,24,30 contain prose such as
"local-world" and "small world."
```

### Text Kernel Preserves The Core Envelope Better Than The Other Modules

Text has the strongest implementation of the ActionRequest/ActionDecision/
KernelEvent distinction:

```text
Request has no :event/id:
test/app/server/rama/text_kernel_test.clj:19-23

Hidden payload :event/id rejected:
test/app/server/rama/text_kernel_test.clj:138-188

Rejected request persists a decision without event:
test/app/server/rama/text_kernel_test.clj:190-208
```

This is the right direction. The failure is that the shared shape did not become
loadable or contract-bearing across the full committed kernel family.

## Recommended Repairs

1. Fix `src/app/server/rama/kernel.clj` so it is reader-valid and
   `app.server.rama.kernel` can be required.
2. Add a test that requires `app.server.rama.kernel` and inspects
   `(:kernel/shape KERNEL-SHAPE)`.
3. Decide whether `KERNEL-SHAPE` is descriptive taxonomy or an enforceable shared
   contract.
4. If enforceable, migrate non-Text modules toward shared core envelope helpers
   or write adapter-level contract tests for each local variant.
5. Narrow `:compat/record` so it cannot be a generic event minting surface.
6. Keep identity headers, but stop treating them as contract proof without
   executable shape tests.

## Tests / Commands Run

```text
clojure -M:test -e "(require 'app.server.rama.kernel)"

Result:
Syntax error reading source at (app/server/rama/kernel.clj:636:88).
Map literal must contain an even number of forms
```

```text
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
                      'app.server.rama.dogfood-transcript-test)
                    (shutdown-agents)
                    (System/exit 0)"

Result:
Ran 52 tests containing 447 assertions.
0 failures, 0 errors.
```

## Open Questions

```text
1. Should non-Text kernels migrate to the shared ActionRequest/ActionDecision/
   KernelEvent helpers, or should KERNEL-SHAPE explicitly classify them as local
   lifecycle variants?

2. Should Transcript produce ActionDecision/KernelEvent records at all, or is it
   intentionally a passive request/status/observation kernel with a different
   envelope?

3. Is :compat/record still needed after the text split, and if so what exact
   legacy event types are allowed?
```
