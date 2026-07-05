# Implementation Validation - Object Container kernel, Slice 1

Phase 4 Rama artifact.

Validated implementation:

```text
src/app/server/rama/object_container.clj
src/app/server/rama/core.clj
```

Inputs read:

```text
docs/current-mental-model/architecture/object-container-spec.md
docs/current-mental-model/build/object-container/IMPLICIT_SPEC.md
docs/current-mental-model/build/object-container/PLAN.md
docs/current-mental-model/build/object-container/PLAN_VALIDATION.md
/mnt/data/projects/Softland/.agents/skills/rama/SKILL.md
/mnt/data/projects/Softland/.agents/skills/rama/references/phase-4-impl-validate.md
/mnt/data/projects/Softland/.agents/skills/rama/references/artifact-impl-validation.md
```

## Verdict

MINOR-FAIL.

The implementation has the right module shape and several runtime smoke paths pass, but Phase 4 found
localized implementation defects:

```text
F1 duplicate request/id is not idempotent when idempotency/key changes
F2 markdown-block-v0 is implemented as a nonblank line splitter, not markdown heading/paragraph/list block derivation
F3 redundant <<if true wrappers remain in the ingest branch
F4 consecutive keypath navigators remain in multiple paths
F5 the plan's read-request helper is exposed as read-audit-request instead of read-request
```

The strongest failure class is `minor-fail`: each defect is repairable by editing specific implementation
lines in the existing module, without changing the PState/depot/topology architecture.

## Evidence Commands

Compile/load:

```text
clojure -M -e "(require 'app.server.rama.object-container :reload) (println :loaded)"
=> :loaded
```

Whitespace/diff hygiene:

```text
git diff --check -- src/app/server/rama/core.clj src/app/server/rama/object_container.clj
=> no output
```

Runtime duplicate request-id probe:

```text
:d1 :accepted evt:2c7093386b716c1ece47203555e39141a277e62c59ff277b12f0666ef5decbbc:same-req
:d2 :accepted evt:2c7093386b716c1ece47203555e39141a277e62c59ff277b12f0666ef5decbbc:same-req
:content Second
:history-count 2
:revision-ids [rev:2c7093386b716c1ece47203555e39141a277e62c59ff277b12f0666ef5decbbc:same-req rev:2c7093386b716c1ece47203555e39141a277e62c59ff277b12f0666ef5decbbc:same-req]
```

This proves F1: same request id can advance content and create two revision-history rows when the
idempotency key changes.

## Spec / Plan Conformance

### Module File And Target Kinds

- Requirement:
  - `PLAN.md:33-37` requires `src/app/server/rama/object_container.clj`.
  - `PLAN.md:39-47` requires adding `:source-artifact`, `:object-container`, `:derived-unit`,
    `:composition-edge`, and `:source-anchor` to target kinds.
- Code:
  - `object_container.clj:1-8` declares `app.server.rama.object-container`.
  - `core.clj:42-46` includes the required target kinds.
- Runtime trace:
  - Requiring the namespace succeeds.
  - Requests built by `source-ingest-request` and `object-edit-request` use target kinds accepted by
    shared core validation.
- Verdict: PASS.

### Source Is Not Container / Storage Shape

- Requirement:
  - The spec says `SourceArtifact` never becomes a container and the old `textArtifact` collapse is retired
    (`object-container-spec.md:52-70`).
  - `PLAN.md:49-50` explicitly forbids reintroducing the collapsed textArtifact shape.
- Code:
  - `SourceArtifactRow`, `ObjectContainerRow`, and `RevisionRow` are distinct records at
    `object_container.clj:66-85`.
  - Source materialization builds separate source, document-container, and later revision rows at
    `object_container.clj:536-579` and `object_container.clj:855-863`.
- Runtime trace:
  - `source/ingest` writes a source row under `$$source-artifacts-by-id` and a document container under
    `$$containers-by-id`; `object/edit` writes revisions without modifying the source row.
- Verdict: PASS.

### Composition Outside Container

- Requirement:
  - The spec says composition lives outside containers as first-class edges (`object-container-spec.md:20-23`).
  - `PLAN.md:696-705` defines separate composition-edge PStates.
- Code:
  - `ObjectContainerRow` has identity/current-content fields but no child list (`object_container.clj:78-81`).
  - `CompositionEdgeRow` is a separate record (`object_container.clj:100-102`).
  - PStates `$$composition-children-by-parent` and `$$composition-parent-by-child` are declared separately
    at `object_container.clj:1064-1068`.
- Runtime trace:
  - Ingest writes each edge to the separate composition PStates at `object_container.clj:1153-1166`.
  - Edit retargets the edge row separately from the container row at `object_container.clj:1304-1314`.
- Verdict: PASS.

### Markdown Block Derivation

- Requirement:
  - `IMPLICIT_SPEC.md:24-28` requires one `DerivedUnit` per markdown block
    `(heading/paragraph/list-item)` via `markdown-block-v0`.
  - `IMPLICIT_SPEC.md:36-38` requires every derived block reachable through composition edges with source
    order and source anchors.
  - `object-container-spec.md:64-65` says markdown headings/paragraphs/plain text without native ids produce
    `DerivedUnit`s.
- Code:
  - `markdown-line-blocks` splits on newline and filters blank lines (`object_container.clj:480-503`).
  - Each emitted row has `:unit-kind :markdown/line` and `:parent-slot-id nil`
    (`object_container.clj:496-500`).
  - `source-materialization` uses `markdown-line-blocks` as the distiller input
    (`object_container.clj:518`).
- Runtime trace:
  - A wrapped markdown paragraph such as `A\nB` becomes two `DerivedUnit`s, not one paragraph block.
  - List nesting and heading hierarchy are not represented because every row has `parent-slot-id nil`.
- Verdict: FAIL.
- Fix class: localized implementation fix. Replace `markdown-line-blocks` with a real `markdown-block-v0`
  block distiller that groups paragraphs/list-items/headings and sets parent/order paths.

### Request / Decision Audit And Request-Id Idempotency

- Requirement:
  - `PLAN.md:119-137` says audit rows are keyed by `audit/id`, and request/decision helpers must build
    that key from `(partition/key, request/id)`.
  - `PLAN.md:495-500` says duplicate `request/id` produces the same decision and no new revision.
  - `IMPLICIT_SPEC.md:79-81` says duplicate request id is idempotent.
- Code:
  - The topology writes `$$requests-by-audit-id[audit/id]` before any duplicate decision check
    (`object_container.clj:1078-1086`).
  - Duplicate detection only checks `$$decisions-by-idempotency[partition-key][idempotency-key]`
    (`object_container.clj:1085-1088`).
  - There is no read of `$$decisions-by-audit-id[decision/id]` before proceeding with a different
    idempotency key.
  - Revision and event ids are deterministic from `request/id` (`object_container.clj:806-808`), but
    revision-history rows are keyed by order key (`object_container.clj:1279-1281`), so the same revision id
    can be written into multiple history slots.
- Runtime trace:
  - First edit with `request/id "same-req"` and `idempotency/key "idem-a"` accepted.
  - Second edit with the same `request/id "same-req"` and `idempotency/key "idem-b"` accepted.
  - The container current content became the second edit text.
  - Revision history contained two rows with the same revision id.
- Verdict: FAIL.
- Fix class: localized topology fix. Check `$$decisions-by-audit-id[decision/id]` before writing the request
  row or before materializing effects; if present, replay that decision and avoid rewriting request/effect
  rows. The request audit write must not clobber the original request on duplicate request id.

### Client Helper Checklist

- Requirement:
  - `PLAN.md:1204-1219` lists helper functions analogous to existing modules.
  - `PLAN.md:133-137` specifically names `read-request` and `read-decision` helpers for audit reads.
- Code:
  - The implementation exposes `read-audit-request`, not `read-request` (`object_container.clj:1406-1411`).
  - `read-decision` correctly accepts either the request or `(partition-key, request-id)`
    (`object_container.clj:1413-1418`).
- Runtime trace:
  - Callers looking for the plan's audit helper name cannot call `read-request`.
- Verdict: FAIL.
- Fix class: localized helper alias/rename.

## Template Checks

### Redundant conditionals

Check verbatim: if every branch of an `<<if`, `<<cond`, or `<<switch` does the same operation with only a
variable differing, replace with a single operation using that variable directly.

- Code:
  - Four ingest materialization loops are wrapped in unconditional `<<if true`
    (`object_container.clj:1131`, `object_container.clj:1138`, `object_container.clj:1145`,
    `object_container.clj:1153`).
- Runtime trace:
  - When source materialization reaches these forms, the predicate is always true; there is no alternate
    branch and no conditional routing benefit. The loops would emit the same rows if the `<<if true` forms
    were removed and their bodies were placed directly in the source branch.
- Verdict: FAIL.
- Fix class: localized cleanup.

### Consecutive keypath

Check verbatim: `(keypath *a) (keypath *b) -> (keypath *a *b)`.

- Code examples:
  - `object_container.clj:1085` uses `[(keypath *partition-key) (keypath *idempotency-key)]`.
  - `object_container.clj:1101` uses `[(keypath *partition-key) (keypath *source-hash)]`.
  - `object_container.clj:1150` uses `[(keypath *document-id) (keypath *outline-key) ...]`.
  - `object_container.clj:1174`, `object_container.clj:1179`, `object_container.clj:1184`,
    `object_container.clj:1193`, `object_container.clj:1216`, `object_container.clj:1231`,
    `object_container.clj:1247`, `object_container.clj:1262`, `object_container.clj:1279`,
    `object_container.clj:1282`, `object_container.clj:1300`, `object_container.clj:1309`,
    and `object_container.clj:1324` show the same pattern.
- Runtime trace:
  - These paths navigate the same nested key structure as a single `(keypath *outer *inner)` navigator.
  - The implementation does not change semantics, but it violates the Rama path-style requirement in the
    Phase 4 template.
- Verdict: FAIL.
- Fix class: localized path cleanup.

### Select-compute-transform

Check verbatim: `local-select>` followed by computation followed by `local-transform>` with `termval` -
replace with `+compound` and an aggregator when possible.

- Code:
  - `object/edit` reads current rows (`object_container.clj:1206-1248`) and passes them into
    `edit-effects` (`object_container.clj:1249-1257`).
  - The resulting complete rows are written with `termval` (`object_container.clj:1258-1314`).
- Runtime trace:
  - The read values are needed to decide target existence, stale-edit rejection, parent revision, outline
    update, event payload, and ack decision. These writes span multiple PStates and cannot be collapsed into
    a single `+compound` transform on one selected value.
- Verdict: PASS.

### Unnecessary nil->val

Check verbatim: navigators handle nil as empty collection - do not add `nil->val` unless the next navigator
requires a non-nil value.

- Code:
  - Search found no `nil->val` in `object_container.clj`.
- Runtime trace:
  - No path creates unnecessary default values before navigation.
- Verdict: PASS.

### :allow-yield?

Check verbatim: `local-select>` or `select>` that iterates over a subindexed structure on a non-mirror PState
should include `{:allow-yield? true}` whenever the iteration count can exceed about 100 entries.

- Code:
  - Topology `local-select>` calls are point reads by key, not range iteration
    (`object_container.clj:1085`, `object_container.clj:1101`, `object_container.clj:1206-1248`,
    `object_container.clj:1329-1349`).
  - Range reads are exposed through direct foreign client helpers `read-outline` and
    `read-revision-history` (`object_container.clj:1430-1450`), not topology `local-select>` or `select>`.
  - Ingest loops call `yield-if-overtime` while exploding generated row sequences
    (`object_container.clj:1133-1156`).
- Runtime trace:
  - The only topology iteration is over in-memory materialization vectors created from the request; each loop
    includes `yield-if-overtime`.
  - No topology range scan over a subindexed PState is present.
- Verdict: PASS.

### Non-subindexed collections without size limits

Check verbatim: for every write to a non-subindexed inner collection, verify the application explicitly
enforces a maximum size.

- Code:
  - Growing inner maps are declared subindexed: `$$decisions-by-idempotency`
    (`object_container.clj:1042-1043`), `$$source-versions-by-ref` (`object_container.clj:1048-1049`),
    `$$source-ingest-completions-by-ref` (`object_container.clj:1051-1052`),
    `$$revision-history-by-container` (`object_container.clj:1055-1057`),
    `$$composition-children-by-parent` (`object_container.clj:1064-1066`),
    `$$outline-by-document` (`object_container.clj:1069-1071`), and `$$edit-order-by-target`
    (`object_container.clj:1072-1074`).
- Runtime trace:
  - Writes into those inner maps use nested keypaths and therefore land in subindexed collections.
  - PStates without inner collections are top-level maps partitioned by key.
- Verdict: PASS.

### Stream topology idempotency

Check verbatim: for each stream topology, trace through what happens if any event retries. Check every write
and side effect for idempotency, topology-generated ids, and internal depot appends.

- Code:
  - Stream topology source uses `:retry-mode :all-after` (`object_container.clj:1077`).
  - Writes use `termval` complete-row overwrites (`object_container.clj:1084-1326`).
  - IDs are derived in pure helpers from source/ref, source/hash, block path, and request id
    (`object_container.clj:128-178`); request ids are client-side in builders (`object_container.clj:938-940`,
    `object_container.clj:987-989`).
  - No `depot-partition-append!` appears in this module.
- Runtime trace:
  - Source ingest retry after object-key writes but before completion repeats deterministic object writes and
    then writes version/latest/completion rows (`object_container.clj:1113-1186`).
  - Object edit retry with the same idempotency key replays from `$$decisions-by-idempotency`
    (`object_container.clj:1085-1093`).
  - However, retry with the same request id but a different idempotency key is not protected by audit decision
    lookup, as proven in F1.
- Verdict: FAIL because of F1.
- Fix class: localized request-id dedupe check.

### Partial failure in stream topologies

Check verbatim: for each stream event that writes to multiple PStates across multiple partitions, consider
what happens if the event fails and retries after some writes have committed but others have not.

- Code:
  - Source ingest starts on source-ref key, repartitions to object key for source/document/unit/edge/outline
    rows (`object_container.clj:1077-1122`), then repartitions to source-ref key for source version/latest,
    completion, and decision idempotency rows (`object_container.clj:1167-1187`).
  - Completion is checked before skipping object materialization (`object_container.clj:1101-1112`).
- Runtime trace:
  - If failure occurs after object-key writes and before completion, retry sees no completion row and repeats
    deterministic object-key writes before repairing source-ref indexes and completion. This satisfies the
    repair rule in `PLAN.md:901-907`.
  - Object edits route to object key before reads/writes (`object_container.clj:1203`) and write object-local
    rows with deterministic ids.
- Verdict: PASS for partial failure repair, excluding the separate duplicate request-id failure already
  recorded in F1.

### Single depot append per client operation

Check verbatim: each client write operation must call `foreign-append!` exactly once.

- Code:
  - The only client append helper is `append-object-container-request!`
    (`object_container.clj:1396-1400`).
  - Request builders only construct request maps (`object_container.clj:927-1033`).
- Runtime trace:
  - A caller builds either a source-ingest or object-edit request, then appends it through one depot call.
  - No client helper issues a second append for the same operation.
- Verdict: PASS.

### Application-state caches survive restart

Check verbatim: for each TaskGlobal or in-process cache holding application state, verify durable source and
rebuild path.

- Code:
  - Search found no `declare-task-global`, TaskGlobal, atom-backed cache, or in-process application-state
    cache in `object_container.clj`.
- Runtime trace:
  - All module state is in PStates.
- Verdict: PASS.

### No reimplementation of built-in operations

Check verbatim: scan for custom code duplicating functionality already provided by Rama built-in namespaces.

- Code:
  - `partition-by-object-key` and `partition-by-audit-id` are custom partitioners required by `PLAN.md:104-137`
    and implemented at `object_container.clj:199-252`.
  - The module uses `ops/explode` for sequence emission (`object_container.clj:1133`, `object_container.clj:1140`,
    `object_container.clj:1147`, `object_container.clj:1155`).
- Runtime trace:
  - The custom partitioners perform domain-specific key extraction before hashing; this is not a built-in
    Rama operation replacement.
- Verdict: PASS.

## Self-Consistency Check

This artifact contains failures and therefore cannot emit `pass`. Every section marked PASS avoids stating a
gap. Every section with a gap is marked FAIL. The strongest failure class is `minor-fail` because the fixes
are localized to the current module:

```text
F1 add request-id/audit decision replay before materialization and avoid clobbering original request audit
F2 replace markdown-line-blocks with a markdown-block-v0 block distiller
F3 remove unconditional <<if true wrappers
F4 collapse consecutive keypath navigators
F5 add/rename read-request helper
```

PHASE_VALIDATION:minor-fail
