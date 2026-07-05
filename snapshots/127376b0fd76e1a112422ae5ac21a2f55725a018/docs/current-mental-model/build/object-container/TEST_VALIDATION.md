# Test Validation - Object Container kernel, Slice 1

Phase 6 Rama artifact.

Validated test source:

```text
test/app/server/rama/object_container_test.clj
```

Inputs read:

```text
docs/current-mental-model/architecture/object-container-spec.md
docs/current-mental-model/build/object-container/IMPLICIT_SPEC.md
docs/current-mental-model/build/object-container/PLAN.md
test/app/server/rama/object_container_test.clj
src/app/server/rama/object_container.clj
/mnt/data/projects/Softland/.agents/skills/rama/SKILL.md
/mnt/data/projects/Softland/.agents/skills/rama/references/phase-6-test-validate.md
/mnt/data/projects/Softland/.agents/skills/rama/references/artifact-test-validation.md
```

## Verdict

MINOR-FAIL.

The test namespace is correctly placed, compiles, uses a single IPC/module launch, and exercises the
Phase 5 minimum invariant list. However, adversarial review against the full implicit spec found several
localized coverage gaps in the existing test namespace. These can be fixed by adding assertions/testing
blocks to `object-container-slice-contract-test`; no new namespace or suite restructure is required.

## Minimize IPC launches

Check verbatim: each `create-ipc` + `launch-module!` adds 30+ seconds. Default to one `deftest` with
testing blocks; justify any additional launch.

Verdict: PASS.

Evidence:

- `object_container_test.clj:93-95` has one `deftest`, one `with-open [ipc (rtest/create-ipc)]`, and one
  runtime binding.
- `object_container_test.clj:9-39` launches `oc/object-container-module` once and obtains all handles.
- All scenario coverage is grouped inside `object-container-slice-contract-test` under testing blocks at
  `object_container_test.clj:96`, `134`, `148`, `196`, `220`, `238`, `296`, `320`, `341`, and `377`.

No extra IPC launch is present.

## Implicit Spec Coverage

Check verbatim: read `IMPLICIT_SPEC.md` and verify every edge case and entity state x write combination is
tested. List each one and the test that covers it. If any are missing, add tests.

Verdict: FAIL.

### Covered Cases

- W1 source ingest produces source, document container, derived markdown blocks, outline, and composition
  order. Covered by `object_container_test.clj:96-132`.
- Raw source preservation and `read-source` by id/ref+hash are covered by `object_container_test.clj:103-121`.
- Markdown paragraph grouping plus heading/list-item kinds are covered by `object_container_test.clj:109-126`.
- Document container read is covered by `object_container_test.clj:107-122`.
- Source ref/hash re-ingest dedupe is covered by `object_container_test.clj:134-146`.
- Derived unit graduation, first revision, `read-unit`, `read-container`, source immutability, SourceAnchor
  copying, outline rewrite, and composition edge retargeting are covered by `object_container_test.clj:148-194`.
- Editing an already-graduated unit as a revise on the same container is covered by
  `object_container_test.clj:196-218`.
- Re-ingest with a new hash not overwriting old authored content is covered by `object_container_test.clj:220-236`.
- Duplicate idempotency key and duplicate request id idempotency are covered by
  `object_container_test.clj:238-294`.
- Stale same-client edit rejection and durable request/decision rows are covered by
  `object_container_test.clj:296-318`.
- Missing target rejection and durable request/decision rows are covered by `object_container_test.clj:320-339`.
- Empty file, whitespace-only file, single-line ingest setup, and empty edit are covered by
  `object_container_test.clj:341-375`.
- Large ordered outline/range read is covered by `object_container_test.clj:377-391`.

### Failures

F1. Missing concurrency/race coverage for W1 same-ref/hash ingest and W2 first-touch edit.

- Spec requirement:
  - `IMPLICIT_SPEC.md:44-46` requires two ingests of the same `(ref, hash)` to produce exactly one result.
  - `IMPLICIT_SPEC.md:76-78` requires two concurrent edits to the same not-yet-graduated unit to produce
    exactly one graduation, with the later edit becoming a revise and no lost edit.
- Existing tests:
  - `object_container_test.clj:134-146` tests sequential re-ingest only after the first ingest has already
    materialized.
  - `object_container_test.clj:196-218` tests a sequential edit after an already-graduated unit is observed.
- Why this fails:
  - The tests do not append two source ingests or two edits back-to-back before awaiting decisions. They
    therefore do not exercise the retry/order boundary described by the concurrency clauses.
- Fix class:
  - Localized test addition. Add one testing block that appends two same-ref/hash ingests before awaiting
    both decisions, and one testing block that appends two edits to the same derived unit before awaiting
    either decision, then asserts one container, two revisions, same container id, and final content from
    the later request.

F2. Missing explicit `DerivedUnit` metadata coverage for SourceAnchor per unit and distiller id/version.

- Spec requirement:
  - `IMPLICIT_SPEC.md:24-28` requires a SourceAnchor per unit.
  - `IMPLICIT_SPEC.md:36-38` requires each DerivedUnit to carry a SourceAnchor plus distiller id/version.
  - `IMPLICIT_SPEC.md:111-113` requires `read-unit` on derived rows to expose source-derived content and
    carry SourceAnchor.
- Existing tests:
  - `object_container_test.clj:158` reads one SourceAnchor before graduation.
  - `object_container_test.clj:191-194` checks copied anchor fields after graduation.
- Why this fails:
  - Only one unit's anchor is sampled. The test does not assert every derived unit has a source anchor.
  - No assertion checks `:distiller-id` or `:distiller-version` on any `DerivedUnit`.
- Fix class:
  - Localized assertions in the first ingest testing block. Iterate over all outline nodes, read each unit,
    assert `:source-anchor-id`, `:distiller-id "markdown-block-v0"`, and `:distiller-version 1`, and assert
    `read-source-anchor` returns an anchor for each target id.

F3. Missing full revision append-only chain coverage.

- Spec requirement:
  - `IMPLICIT_SPEC.md:141-145` requires first revision parent-rev = nil, later revisions chained to the prior,
    and full history range-readable.
  - `IMPLICIT_SPEC.md:136-138` requires prior revisions to remain readable after a durable container edit.
- Existing tests:
  - `object_container_test.clj:169-179` checks first history count is 1.
  - `object_container_test.clj:213-218` checks revised history count is 2.
  - `object_container_test.clj:263-294` checks history count under idempotency/request-id retry.
- Why this fails:
  - No assertion checks first revision `:parent-revision-id` is nil.
  - No assertion checks the second revision's `:parent-revision-id` equals the first revision id.
  - No assertion checks revision-history pagination/range reads, only outline pagination.
- Fix class:
  - Localized assertions in the graduation/revision blocks. Read revision rows, assert parent chain and
    previous revision content, and call `read-revision-history` with a cursor/limit.

F4. Missing source ingest negative/rejected-decision coverage.

- Spec/protocol requirement:
  - The slice is expressed in the common ActionRequest -> decision -> event -> materialization contract
    (`IMPLICIT_SPEC.md:5-8`).
  - W1 accepts only markdown with matching content hash and supported distiller metadata
    (`IMPLICIT_SPEC.md:24-28`; implementation validation also treated rejected requests as durable decisions).
- Existing tests:
  - `object_container_test.clj:320-339` covers an object/edit missing-target rejection.
  - There is no malformed `source/ingest` request.
- Why this fails:
  - The tests do not exercise a rejected source ingest decision, such as content-hash mismatch or unsupported
    source format/distiller, nor assert that no source row/event is written for that rejection.
- Fix class:
  - Localized testing block. Build a source request, corrupt `[:payload :source-hash]` or
    `[:payload :source-format]`, append/await, assert durable rejected decision and no source artifact.

F5. Missing public await helper request-arity coverage.

- Plan/protocol requirement:
  - `PLAN.md:1221-1223` requires `await-object-container-decision` to accept the original request or
    `(partition/key, request/id)` and forbids a bare request-id lookup.
- Existing tests:
  - `object_container_test.clj:41-47` helper `append-and-await!` always calls
    `await-object-container-decision` with `(partition/key, request/id)`.
  - No test calls `await-object-container-decision` with the original request map.
- Why this fails:
  - Only one required public arity is exercised.
- Fix class:
  - Localized assertion. In any append path, call `(oc/await-object-container-decision runtime request 5000)`
    and compare with the `(partition/key, request/id)` result.

## Synchronization

Check verbatim: every write that precedes a read must be followed by processing synchronization before the
read.

Verdict: PASS.

Evidence:

- All source and edit writes flow through `append-and-await!` at `object_container_test.clj:41-47`,
  `ingest!` at `object_container_test.clj:75-84`, or `edit!` at `object_container_test.clj:86-91`.
- `append-and-await!` appends the request, then waits for the decision by `(partition/key, request/id)`
  before the caller reads PStates.
- Direct scenario writes use `append-and-await!`: same-ref/hash re-ingest at `object_container_test.clj:140`,
  stale edit at `object_container_test.clj:309`, and missing target at `object_container_test.clj:332`.
- There are no raw `foreign-append!` calls in the test namespace.

The module is a stream topology, not microbatch, so no `wait-for-microbatch-processed-count` is required.
The explicit decision wait is the test harness synchronization point used by this module.

## Test Namespaces Compile

Check verbatim: every test namespace must load cleanly; verify imports, refer entries, and that no test
depends on a private namespace.

Verdict: PASS.

Evidence command:

```text
clojure -M:test -e "(require 'app.server.rama.object-container-test :reload) (println :loaded)"
=> :loaded
```

The command loaded the test namespace successfully and did not execute the test suite.

## Self-Consistency Check

This artifact names five gaps under "Implicit Spec Coverage", so the corresponding check is FAIL. The
overall verdict cannot be `pass`. Each failure is local to existing `object_container_test.clj` coverage and
does not require a new namespace or restructuring.

## Verdict

minor-fail - the suite compiles and covers the Phase 5 minimum list, but full implicit-spec validation
requires localized additions for concurrency/retry edges, per-unit metadata, revision chaining,
source-ingest rejection, and await request-arity coverage.

PHASE_VALIDATION:minor-fail
