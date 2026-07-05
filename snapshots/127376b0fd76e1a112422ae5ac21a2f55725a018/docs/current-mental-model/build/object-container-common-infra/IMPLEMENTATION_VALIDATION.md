# Implementation Validation

<!-- Phase 4. Filled after the current Phase 3 repair slice. -->

Validated source:

```text
src/app/server/rama/core.clj
src/app/server/rama/object_container.clj
```

Validation inputs:

```text
docs/current-mental-model/build/object-container-common-infra/PRODUCT.md
docs/current-mental-model/build/object-container-common-infra/IMPLICIT_SPEC.md
docs/current-mental-model/build/object-container-common-infra/PLAN.md
.agents/skills/rama/references/phase-4-impl-validate.md
.agents/skills/rama/references/artifact-impl-validation.md
```

Runtime evidence gathered:

```text
clojure -M:test -e "(require 'app.server.rama.core :reload 'app.server.rama.object-container :reload)"
  => exit 0

Targeted transcript common-import probe:
  => {:accepted :accepted,
      :duplicate :accepted,
      :duplicate-same-event true,
      :changed-status :rejected,
      :changed-reason :import/material-fingerprint-conflict,
      :completion true,
      :version true,
      :conv-kind :chat-conversation,
      :tool-result-kind :tool-result,
      :tool-result-rev "ran",
      :anchors 1,
      :parents 1,
      :children 1,
      :material {:containers 4, :derived-units 0, :anchors 4, :edges 3}}

clojure -M:test -e "(require '[clojure.test :as t] 'app.server.rama.object-container-test) ..."
  => Ran 1 tests containing 1 assertions. 0 failures, 1 errors.
     The error occurs at test/app/server/rama/object_container_test.clj:73,
     where the old helper reads $$source-anchors-by-target with
     foreign-select-one [(keypath target-id)]. The implementation changed that
     PState to a subindexed map, so this test harness must be ported in Phase 5.

git diff --check -- src/app/server/rama/object_container.clj src/app/server/rama/core.clj docs/current-mental-model/build/object-container-common-infra/IMPLEMENTATION_VALIDATION.md
  => exit 0 before this artifact rewrite
```

The implementation is a meaningful W1 repair over the prior artifact: import
completion rows, material fingerprint conflict checks, plural anchor/parent
indexes, source enumeration indexes, transcript-to-common object creation, and
common read helpers now exist. It still does not satisfy the full Phase 3 plan.

## Product / Plan Contract Trace

### Common markdown + transcript substrate

Status: FAIL.

Trace:

- The product requires markdown and transcript material to enter the same common
  object substrate. The plan says markdown `:source/ingest` must be replaced by
  or become compatibility sugar over `:object-container/import-material`
  (`PLAN.md:818-860`).
- Current markdown requests still use `:request-type :source/ingest` in
  `source-ingest-request` at `src/app/server/rama/object_container.clj:1708-1766`.
- The stream still has a separate `:source/ingest` branch at
  `src/app/server/rama/object_container.clj:1943-2129`.
- Transcript observations use `:object-container/import-material` at
  `src/app/server/rama/object_container.clj:1154-1389` and the W1 branch starts at
  `src/app/server/rama/object_container.clj:2131`.

Runtime result:

Both paths write some common PStates, but markdown has not been converted to the
planned common import adapter shape. The substrate is therefore shared at the row
level, not at the write contract level required by W2.

### W1 completion and idempotency repair

Status: FAIL.

Trace:

- `ImportCompletionRow` exists at `src/app/server/rama/object_container.clj:85-88`,
  and `$$import-completions-by-key` is declared at
  `src/app/server/rama/object_container.clj:1851-1852`.
- The W1 branch checks import completion at
  `src/app/server/rama/object_container.clj:2141-2195` and writes completion
  before accepted idempotency at `src/app/server/rama/object_container.clj:2415-2424`.
- However, the global idempotency hit path at
  `src/app/server/rama/object_container.clj:1920-1940` compares only material
  fingerprint. For same-fingerprint prior decisions it replays immediately and
  never verifies the import completion required by `PLAN.md:1986-1990`.
- The legacy `:source/ingest` branch writes `$$source-ingest-completions-by-ref`
  and then idempotency at `src/app/server/rama/object_container.clj:2100-2119`,
  not `$$import-completions-by-key`.

Runtime result:

The targeted duplicate import probe passed for the different-idempotency import
completion path. The full F6 repair contract is still not satisfied because a
same-idempotency accepted decision can be replayed without checking completion,
and markdown still uses the older completion mechanism.

### Idempotency conflict semantics

Status: PARTIAL PASS.

Trace:

- Decision rows now carry `material-fingerprint` and conflict metadata at
  `src/app/server/rama/object_container.clj:64-67`.
- The global idempotency branch compares current and prior fingerprints at
  `src/app/server/rama/object_container.clj:1920-1940`.
- Import completion conflict handling rejects changed material for the same
  import key at `src/app/server/rama/object_container.clj:2180-2195`.

Runtime result:

The targeted probe proved same import key + changed material rejects with
`:import/material-fingerprint-conflict`. This check is still partial rather than
full pass because the same-idempotency replay path does not verify completion as
described above.

### Native identity conflict guard

Status: FAIL.

Trace:

- `NativeIdentityClaimRow` exists at `src/app/server/rama/object_container.clj:120-123`,
  and `$$native-identity-claims-by-container` is declared at
  `src/app/server/rama/object_container.clj:1892-1893`.
- W1 loops over incoming containers, builds one claim, and compares it with the
  already durable claim at `src/app/server/rama/object_container.clj:2197-2216`.
- The plan requires a request-local deterministic incoming-claim map that
  canonicalizes exact duplicate candidates and rejects conflicting same-payload
  duplicate claims before PState reads or base-row overwrites (`PLAN.md:1480-1488`,
  `PLAN.md:2006-2017`).
- Current code does not build that incoming map. If one payload contains two
  container rows with the same `container-id` but no pre-existing durable claim,
  the validation loop sees no conflict, then the write loop at
  `src/app/server/rama/object_container.clj:2231-2263` can overwrite the same
  container/claim key.

Runtime result:

The durable conflict guard exists for already accepted claims, but same-payload
duplicate canonicalization is missing. This is a core W1 invariant failure.

### Source / provenance closure before base overwrite

Status: FAIL.

Trace:

- `import-request-validation-errors` validates only payload-local source ids,
  target ids, current revision ids, and anchor sets at
  `src/app/server/rama/object_container.clj:976-1036`.
- The plan requires W1 to point-read durable SourceArtifact/ObjectContainer/
  DerivedUnit/SourceAnchor rows when closure references are not in the current
  payload (`PLAN.md:1490-1498`, `PLAN.md:2030-2049`).
- The W1 materialization branch goes from native-claim checks directly into base
  row writes at `src/app/server/rama/object_container.clj:2218-2424`. There is no
  durable source/provenance closure phase between validation and base overwrite.

Runtime result:

The implementation accepts the simple case where all provenance is in the
payload, but it does not implement the planned accepted-durable closure or
repair-pending/rejected closure path.

### PState schema shape

Status: FAIL.

Trace:

- The plan requires uniform row values to use `fixed-keys-schema` maps, not stored
  whole-row defrecord classes (`PLAN.md:1087-1095`).
- Current uniform rows are defrecords at
  `src/app/server/rama/object_container.clj:31-130`.
- PStates store those record classes directly at
  `src/app/server/rama/object_container.clj:1843-1898`.
- Search evidence: no `fixed-keys-schema` appears in
  `src/app/server/rama/object_container.clj`.

Runtime result:

The implementation compiles and runs in IPC, but it still violates the planned
schema contract.

### Plural source anchors and parent composition

Status: PASS WITH NAMING DRIFT.

Trace:

- `$$source-anchors-by-target` is now a subindexed map at
  `src/app/server/rama/object_container.clj:1871-1873`.
- `$$composition-parent-by-child` is now a subindexed map at
  `src/app/server/rama/object_container.clj:1877-1879`.
- Markdown/source-ingest writes use `[target-id anchor-id]` at
  `src/app/server/rama/object_container.clj:1994-2007` and
  `src/app/server/rama/object_container.clj:2034-2056`.
- W1 import writes use `[target-id anchor-id]` and `[child-id parent-ref-key]` at
  `src/app/server/rama/object_container.clj:2318-2386`.
- Edit reads use `(subselect MAP-VALS)` at
  `src/app/server/rama/object_container.clj:2465-2492`, and edit writes preserve
  the plural shapes at `src/app/server/rama/object_container.clj:2552-2603`.

Runtime result:

The core plural shape is implemented. The PState name remains singular
`$$composition-parent-by-child` rather than the plan's
`$$composition-parents-by-child`, but the storage shape and wrapper
`read-composition-parents` provide the plural behavior.

### Source enumeration indexes and common source material reads

Status: PARTIAL PASS.

Trace:

- Source material ref PStates are declared at
  `src/app/server/rama/object_container.clj:1880-1890`.
- Markdown/source-ingest writes source material refs for document, units,
  anchors, and edges at `src/app/server/rama/object_container.clj:1975-2099`.
- W1 import writes source material refs at
  `src/app/server/rama/object_container.clj:2244-2257`,
  `src/app/server/rama/object_container.clj:2303-2316`,
  `src/app/server/rama/object_container.clj:2334-2344`, and
  `src/app/server/rama/object_container.clj:2372-2385`.
- `read-common-material-for-source` exists at
  `src/app/server/rama/object_container.clj:2802-2807`.

Runtime result:

The targeted transcript probe returned source material counts for containers,
anchors, and edges. This remains partial because the plan calls for a query
topology with requested categories, cursors, and limits (`PLAN.md:2380-2414`),
while the current wrapper does four client-side full `MAP-VALS` reads at
`src/app/server/rama/object_container.clj:2782-2807`.

### Required query/read surface

Status: FAIL.

Trace:

- Implemented query topologies are `read-latest-source-by-ref`,
  `read-source-by-ref-version`, `read-unit`, and `read-current-revision` at
  `src/app/server/rama/object_container.clj:2618-2674`.
- Foreign wrappers cover common source/material/anchor/edge reads at
  `src/app/server/rama/object_container.clj:2762-2857`.
- Missing planned query/read surfaces include `read-common-material-for-source`
  as a real query topology with categories/cursors, `read-transcript-object-bundle`,
  transcript conversation projection reads, and transcript operational reads
  (`PLAN.md:2380-2457`, `PLAN.md:594-653`).

Runtime result:

Common direct reads can prove a useful W1 subset. They do not cover the planned
read surface for the full slice.

### Transcript harvest/watch/file-state operational contract

Status: FAIL.

Trace:

- The plan requires `*transcript-control-depot`, `*transcript-file-state-depot`,
  and transcript operational/projection PStates (`PLAN.md:862-1039`,
  `PLAN.md:1618-1661`, `PLAN.md:2185-2233`).
- Current code declares only `*object-container-requests-depot` at
  `src/app/server/rama/object_container.clj:1841`.
- The current PState block at `src/app/server/rama/object_container.clj:1843-1898`
  has no `$$transcript-runs`, `$$transcript-file-offsets`,
  `$$transcript-source-lines-by-file`, `$$transcript-conversation-projection`,
  `$$transcript-tool-calls-by-name`, `$$transcript-audit-by-request`, or
  `$$transcript-last-message-by-conversation`.

Runtime result:

W3, W4, W6, and the transcript operational/projection part of W8 are not
implemented in the common module.

### Transcript tool-result and pending-edge relation

Status: FAIL.

Trace:

- Tool-result containers and revisions are created at
  `src/app/server/rama/object_container.clj:1252-1274`, and the runtime probe
  confirmed `:tool-result` with revision content `"ran"`.
- The tool-result produced edge is still built with parent `message-id`, not the
  matching tool-call id, at `src/app/server/rama/object_container.clj:1271-1274`.
- The plan requires tool results to become real objects and, when a tool-call
  parent is missing, to write a pending edge keyed by expected tool-call id and
  repair it when the parent arrives (`PLAN.md:2581-2599`).
- There is no pending-composition PState in the declaration block at
  `src/app/server/rama/object_container.clj:1843-1898`.

Runtime result:

F5's content requirement is now satisfied for the simple probe, but the common
tool-call/result relationship and pending repair contract are not.

### Parse-error handling

Status: PARTIAL PASS.

Trace:

- The transcript adapter sets containers, revisions, anchors, and edges to empty
  vectors on parse errors at `src/app/server/rama/object_container.clj:1275-1293`.
- That avoids fake common ObjectContainers, matching the product fail condition
  against parse-error fake containers.
- However, the plan also requires parse-error projection/audit rows
  (`PLAN.md:985-994`, `IMPLICIT_SPEC.md:1136-1141`), and the common module has no
  transcript audit/projection PStates.

Runtime result:

The fake-container failure is avoided, but parse-error visibility is not
implemented.

### Cooperative multitasking

Status: PARTIAL PASS.

Trace:

- Large markdown/source-ingest loops now call `yield-if-overtime` at
  `src/app/server/rama/object_container.clj:2011-2099`.
- Large W1 import loops now call `yield-if-overtime` at
  `src/app/server/rama/object_container.clj:2197-2390`.
- The implementation does not have large local-select range reads in query
  topologies.
- Client wrappers for source material and anchor/edge reads use full `MAP-VALS`
  reads without cursor/limit at `src/app/server/rama/object_container.clj:2782-2822`,
  which does not match the paged read designs in `PLAN.md:461-570` and
  `PLAN.md:2380-2414`.

Runtime result:

The stream-loop yielding gap from the prior validation is repaired. The common
source material read surface still lacks planned paging/category controls.

## Template Checks

## Redundant conditionals

Status: PASS.

Trace:

- The main `<<cond` dispatches distinct request families at
  `src/app/server/rama/object_container.clj:1942-2616`.
- Nested branches either short-circuit audit/idempotency/completion cases or
  conditionally write optional edit effects. No branch pair was found where every
  branch performs the same operation with only a variable differing.

Runtime result:

No redundant conditional required a simplification.

## Consecutive keypath

Status: PASS.

Trace:

- Searches found no `(keypath *a) (keypath *b)` pattern.
- Multi-key paths use a single `keypath`, for example
  `src/app/server/rama/object_container.clj:1917`,
  `src/app/server/rama/object_container.clj:2280`, and
  `src/app/server/rama/object_container.clj:2368`.

Runtime result:

No consecutive keypath issue was found.

## Select-compute-transform

Status: PASS.

Trace:

- Row materialization writes use full-row `termval` upserts after the row has
  already been computed, for example
  `src/app/server/rama/object_container.clj:2227-2229`,
  `src/app/server/rama/object_container.clj:2241-2243`, and
  `src/app/server/rama/object_container.clj:2417-2424`.
- The implementation does not do a select-compute-transform update that should
  be an aggregator or compound update.

Runtime result:

No select-compute-transform rewrite was required by the template check.

## Unnecessary nil->val

Status: PASS.

Trace:

- Search for `nil->val` in `src/app/server/rama/object_container.clj` returned no
  occurrences.

Runtime result:

No unnecessary `nil->val` navigator is present.

## :allow-yield?

Status: PASS FOR LOCAL RANGE READS.

Trace:

- The query topologies at `src/app/server/rama/object_container.clj:2618-2674`
  perform point reads only.
- Local subindexed reads in the edit branch use `(subselect MAP-VALS)` at
  `src/app/server/rama/object_container.clj:2465-2492`, but they are target-local
  provenance/parent reads expected to be small in this edit path.
- Large import loops call `yield-if-overtime` as traced above.

Runtime result:

No local `local-select>` or `select>` range read over an expected >100-entry
subindex was found without `{:allow-yield? true}`. Paged foreign read design
gaps are covered under the product/plan checks above.

## Non-subindexed collections without size limits

Status: PASS FOR IMPLEMENTED INNER COLLECTIONS.

Trace:

- Implemented inner maps that can grow are subindexed:
  `$$decisions-by-idempotency` at `src/app/server/rama/object_container.clj:1847-1848`,
  `$$source-versions-by-ref` at `src/app/server/rama/object_container.clj:1855-1856`,
  `$$revision-history-by-container` at `src/app/server/rama/object_container.clj:1864-1866`,
  `$$source-anchors-by-target` at `src/app/server/rama/object_container.clj:1871-1873`,
  `$$composition-children-by-parent` at `src/app/server/rama/object_container.clj:1874-1876`,
  `$$composition-parent-by-child` at `src/app/server/rama/object_container.clj:1877-1879`,
  source material indexes at `src/app/server/rama/object_container.clj:1880-1890`,
  outline at `src/app/server/rama/object_container.clj:1894-1896`, and edit order
  at `src/app/server/rama/object_container.clj:1897-1898`.

Runtime result:

No implemented non-subindexed inner collection without a size limit was found.
Missing planned transcript/pending-edge subindexes are product/plan failures, not
mis-shaped implemented collections.

## Stream topology idempotency

Status: FAIL.

Trace:

- PState writes are deterministic full-row upserts and IDs are deterministic or
  request-supplied.
- W1 writes accepted import idempotency after import completion at
  `src/app/server/rama/object_container.clj:2415-2424`.
- But the global idempotency replay path at
  `src/app/server/rama/object_container.clj:1920-1940` does not verify completion
  for accepted import decisions.
- The W7 edit branch writes decision and idempotency before checking whether
  `effect-has-event?` and before writing event/material rows at
  `src/app/server/rama/object_container.clj:2518-2525`.

Runtime result:

Retrying exact deterministic row writes is mechanically safe, but the logical
idempotency/repair protocol is not yet safe for partial failures.

## Partial failure in stream topologies

Status: FAIL.

Trace:

- W1 writes many common rows before event, decision, completion, and idempotency
  at `src/app/server/rama/object_container.clj:2218-2424`.
- W1 does have an import-completion marker, but a same-idempotency retry can still
  replay at `src/app/server/rama/object_container.clj:1920-1940` without checking
  that marker.
- W7 writes decision/idempotency at `src/app/server/rama/object_container.clj:2518-2524`
  before optional event/material writes at `src/app/server/rama/object_container.clj:2525-2603`.
- The planned source-line/file-offset/projection completion repair paths are
  absent because W3/W4/W6/W8 PStates are absent.

Runtime result:

Partial failure can still be hidden by a durable decision/idempotency row in
some paths. This is major because it is exactly the F6 class of failure.

## Single depot append per client operation

Status: PASS FOR IMPLEMENTED WRAPPER, FAIL FOR PLANNED SURFACE.

Trace:

- `append-object-container-request!` performs one `foreign-append!` at
  `src/app/server/rama/object_container.clj:2732-2736`.
- There are no client wrappers or depots for planned transcript harvest/watch or
  file-state operations.

Runtime result:

The implemented write wrapper follows the rule, but the full planned write
surface cannot be certified because it is missing.

## Application-state caches survive restart

Status: PASS.

Trace:

- The plan says no TaskGlobal is required (`PLAN.md:2774-2778`).
- The module declares no TaskGlobals.

Runtime result:

No in-process application-state cache was found that would need a rebuild path.

## No reimplementation of built-in operations

Status: PASS.

Trace:

- The module uses Rama dataflow/path operations directly, including `source>`,
  `local-select>`, `local-transform>`, `keypath`, `termval`, and `loop<-`.
- No hand-rolled replacement for a Rama built-in operation was found.

Runtime result:

No built-in Rama operation appears to have been reimplemented in custom code.

## Verdict

major-fail - The repair materially improves W1 and proves a useful transcript
common-import subset, but the implementation still misses major planned
architecture: markdown W2-as-W1 import, W3/W4/W6 transcript operational depots
and PStates, W8 transcript projections, source/provenance durable closure,
same-payload native claim canonicalization, pending edge repair, fixed-key
schemas, query-topology source material reads, and partial-failure-safe
idempotency for all paths.

PHASE_VALIDATION:major-fail
