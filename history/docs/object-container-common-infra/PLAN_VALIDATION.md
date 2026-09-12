# Plan Validation - Object-Container Common Infra

Status: Rama Phase 2 artifact, 2026-06-08.

Verdict: PASS.

This artifact validates:

```text
docs/current-mental-model/build/object-container-common-infra/PRODUCT.md
docs/current-mental-model/build/object-container-common-infra/IMPLICIT_SPEC.md
docs/current-mental-model/build/object-container-common-infra/PLAN.md
.agents/skills/rama/references/phase-2-plan-validate.md
.agents/skills/rama/references/artifact-plan-validation.md
```

The prior Phase 2 failure was source/provenance closure. The current `PLAN.md`
now validates source artifacts, source anchors, source-created targets, native
identity claims, completion ordering, file offset advancement, and projection
repair before accepted completion or accepted idempotency can suppress repair.

Self-consistency check: this artifact records a PASS verdict consistently across
all checked sections.

## Query Topology Checks

### `read-source-by-ref-version`

- Input examples present: yes. `PLAN.md:2237-2274`.
- Example 1: missing version. N=1 total read, M=1 meaningful read. N == M: yes.
- Example 2: version exists. N=2 total reads, M=2 meaningful reads. N == M:
  yes.
- M values across examples: `[1 2]`. All same: no.
- Variable handling present: yes. `PLAN.md:2263-2274` requires `<<if` after
  the source version read and routes to `partition-by-object-key` only when the
  version exists.
- Verdict: PASS.

### `read-latest-source-by-ref`

- Input examples present: yes. `PLAN.md:2276-2307`.
- Example 1: no versions. N=1, M=1. N == M: yes.
- Example 2: versions exist. N=2, M=2. N == M: yes.
- M values across examples: `[1 2]`. All same: no.
- Variable handling present: yes. `PLAN.md:2298-2307` branches after the latest
  row read and routes to `partition-by-object-key` only when a row exists.
- Verdict: PASS.

### `read-current-revision`

- Input examples present: yes. `PLAN.md:2309-2344`.
- Example 1: missing container. N=1, M=1. N == M: yes.
- Example 2: invalid accepted container missing current revision. N=1, M=1.
  N == M: yes.
- Example 3: container with current revision. N=2, M=2. N == M: yes.
- M values across examples: `[1 1 2]`. All same: no.
- Variable handling present: yes. `PLAN.md:2335-2344` uses branches for missing
  and invalid containers and only reads the revision when a current revision id
  exists.
- Verdict: PASS.

### `read-derived-unit`

- Input examples present: yes. `PLAN.md:2346-2378`.
- Example 1: missing unit. N=1, M=1. N == M: yes.
- Example 2: derived unit. N=2, M=2. N == M: yes.
- Example 3: graduated unit. N=2, M=2. N == M: yes.
- M values across examples: `[1 2 2]`. All same: no.
- Variable handling present: yes. `PLAN.md:2372-2378` reads graduation only
  after the unit exists.
- Verdict: PASS.

### `read-common-material-for-source`

- Input examples present: yes. `PLAN.md:2380-2414`.
- Example 1: categories `[:containers]`. N=1 range, M=1 range. N == M: yes.
- Example 2: categories `[:containers :derived-units :anchors :edges]`. N=4
  ranges, M=4 ranges. N == M: yes.
- Example 3: categories `[]`. N=0, M=0. N == M: yes.
- M values across examples: `[1 4 0]`. All same: no.
- Variable handling present: yes. `PLAN.md:2407-2414` uses `ops/explode` over
  requested categories and does not issue empty category reads.
- Verdict: PASS.

### `read-transcript-object-bundle`

- Input examples present: yes. `PLAN.md:2416-2460`.
- Example 1: message container, no children. N=3, M=3. N == M: yes.
- Example 2: tool-call container, children requested. N=4, M=4. N == M:
  yes.
- Example 3: missing container. N=1, M=1. N == M: yes.
- M values across examples: `[3 4 1]`. All same: no.
- Variable handling present: yes. `PLAN.md:2450-2457` branches after the
  container read and only reads children when requested.
- Verdict: PASS.

## PState Schema Checks

- Any `Object` type: no. `PLAN.md:1089-1090` says no PState schema should use
  `Object`.
- Uniform record-like values use `fixed-keys-schema`: yes. `PLAN.md:1092-1095`
  requires fixed-key schemas for row-like values with the same fields.
- Different instances at the same PState position use `definterface` +
  `defrecord`: yes. `PLAN.md:1097-1099` and `PLAN.md:1454-1465` require
  interfaces plus defrecords for polymorphic payload/projection values.
- Fixed-key parse-error source version sentinel: yes. `PLAN.md:1467-1471`
  uses an empty string sentinel for `:container/id` when no base container is
  expected and forbids interpreting it as a fake ObjectContainer id.
- Inner collections that can exceed 100 elements are subindexed: yes.
  `PLAN.md:2464-2486` lists all growing inner maps as subindexed, including
  decisions by idempotency, source versions, revision histories, anchors,
  composition edges, source enumeration indexes, edit order, markdown outline,
  transcript source-line ledger, transcript conversation projection, tool-call
  index, and audit by request.
- Non-subindexed collections are point/latest rows: yes. `PLAN.md:2497-2511`
  limits non-subindexed PStates to top-level point lookups, native identity
  claims, runs, file offsets, and last-message-by-conversation.
- Verdict: PASS.

## Topology Checks

- Microbatch unless justified: PASS. The plan uses a stream topology and gives
  the justification at `PLAN.md:1879-1889`: `object/edit` needs interactive
  read-after-write behavior, watch/import tests and dogfood UI benefit from
  ack-visible decisions, and writes are deterministic full-row upserts.
- Low-latency writes handled by stream: PASS. W7 `object/edit` has
  single-digit millisecond desired latency in `IMPLICIT_SPEC.md:540-567`, and
  the stream topology includes `:object/edit` from
  `*object-container-requests-depot` at `PLAN.md:1832-1839` and
  `PLAN.md:1879-1897`.
- Non-idempotent stream writes: none. `PLAN.md:1908-1923` says created ids are
  deterministic or request-supplied, materialization writes are complete-row
  overwrites, and there are no append-style collection writes, counters, or
  random ids.
- Stream topology retry mode: PASS. `PLAN.md:1891-1906` uses `:retry-mode
  :all-after` on all stream sources and explains ordering requirements for
  markdown ref versions, transcript conversations, edit lineage, and file-state
  retries.
- Cooperative multitasking: PASS. `PLAN.md:1961-1966` requires
  `yield-if-overtime` for large import loops and `{:allow-yield? true}` for
  large range reads in query topologies.
- Verdict: PASS.

## Cross-Topology Correctness

- Internal depot flows: none in this slice. `PLAN.md:1874-1875` states that no
  internal depots are required because projections are written by the same common
  import event.
- Duplicate internal records from stream retry: not applicable, because there is
  no internal depot flow.
- Stream `depot-partition-append!` commit boundary: PASS. The plan contains no
  `depot-partition-append!` inside a stream topology. W2/W3/W4 acquisition-side
  appends to W1 are adapter/client behavior (`PLAN.md:823-824`,
  `PLAN.md:879-881`, `PLAN.md:921-923`), not stream-topology internal depot
  appends. Since the plan has no stream-topology depot append, the hard commit
  boundary check is vacuously satisfied.
- Verdict: PASS.

## Production Readiness

### Multiple Concurrent Clients

Scenario: two clients import the same transcript source line with the same
conversation id and source-line key at the same time.

- The request routes by transcript `object/key` (`PLAN.md:143-153`,
  `PLAN.md:175-179`, `PLAN.md:191-193`), so conversation materialization,
  native identity claims, follows edges, and projection rows serialize on the
  conversation task.
- The material fingerprint is deterministic and excludes request id and
  wall-clock time (`PLAN.md:275-295`).
- A same idempotency key with a different fingerprint is rejected before
  materialization (`PLAN.md:297-302`, `PLAN.md:2108-2111`).
- A same source-native container id with different content/provenance/material is
  rejected or repair-pended before base-row overwrite (`PLAN.md:737-746`,
  `PLAN.md:2006-2029`).
- Verdict: PASS.

### Client Process Restart

Scenario: a harvest client appends W1 records for transcript lines 1-10, exits,
then restarts and observes lines 1-12.

- File offsets are operational and cannot prove base materialization
  (`PLAN.md:1021-1028`, `PLAN.md:1442-1446`).
- Safe resume can cross a source line only when the line has
  `:import-complete` and a matching import completion row, or
  `:parse-error-complete` with audit proof (`PLAN.md:2217-2225`).
- Duplicate W1 imports converge through deterministic ids, completion rows, and
  idempotency rows (`PLAN.md:304-322`, `PLAN.md:762-813`).
- Verdict: PASS.

### Worker Restart During Topology Execution

Scenario: the worker crashes after common rows for one transcript message are
written but before import completion.

- Accepted idempotency is deliberately written after completion
  (`PLAN.md:2064-2070`, `PLAN.md:2098-2101`).
- Retry sees no completion row and repairs materialization instead of taking the
  idempotency shortcut (`PLAN.md:2074-2111`).
- Already written rows are full-row deterministic overwrites
  (`PLAN.md:815-816`, `PLAN.md:2090-2091`).
- Verdict: PASS.

### Large Scale

Scenario: a markdown file has 20,000 blocks; a transcript conversation has
50,000 source lines and many tool calls.

- Growing inner collections are subindexed (`PLAN.md:2464-2486`).
- Source-level enumeration uses category indexes rather than whole-store scans
  (`PLAN.md:1808-1828`, `PLAN.md:2380-2414`).
- Large import loops must yield (`PLAN.md:1961-1966`).
- Verdict: PASS.

### Multi-Partition Partial Failure

Scenario: markdown import writes base material on `object/key`, source-version
indexes on `source-ref-key`, decision on `audit/id`, completion on `import/key`,
and idempotency on `partition/key`.

- The completion row is written only after base rows, source indexes, anchors,
  edges, projection rows, last-message state where applicable, audit rows, event,
  and decision are present (`PLAN.md:304-322`, `PLAN.md:2050-2070`).
- If failure occurs before completion, retry repairs all missing rows
  (`PLAN.md:2074-2101`).
- If failure occurs after completion but before source-line status or idempotency,
  retry verifies the completion event and repairs source-line/idempotency rows
  from completion (`PLAN.md:2103-2106`).
- Verdict: PASS.

## Spec Coverage

### Common Substrate Boundary

- Source: PRODUCT says `markdown-created material and transcript-created
  material both become first-class ObjectContainers in the same common
  substrate` (`PRODUCT.md:58-63`).
- Source: IMPLICIT_SPEC says `Canonical base identity may not be source-specific`
  (`IMPLICIT_SPEC.md:18-21`).
- Trace: importing `notes.md` creates `oc:document:<md-key>` through W2
  (`PLAN.md:827-848`). Importing transcript line 42 creates
  `oc:chat-message:<chat-key>:<message-key>` through W5 (`PLAN.md:941-957`).
  Both rows are written to `$$containers-by-id` (`PLAN.md:1537-1540`) and both
  are read by the same R2 path (`PLAN.md:378-405`).
- Fault tolerance: if the transcript write fails before completion, no accepted
  idempotency or source-line completion can hide the missing common write
  (`PLAN.md:2074-2111`).
- Race check: concurrent transcript imports for the same native id serialize on
  `object/key` and are guarded by native identity claims (`PLAN.md:191-193`,
  `PLAN.md:2006-2029`).
- Flaws found: none found, with reasoning: base identity for both formats is
  written to common PStates and transcript-local base identity is explicitly not
  accepted as proof (`PLAN.md:2633-2650`).
- Verdict: PASS.

### Raw Source Preservation And Redaction

- Source: PRODUCT requires `preserve the raw source` and `keep exact anchors
  back to the source` (`PRODUCT.md:70-79`).
- Source: IMPLICIT_SPEC requires transcript modes `local-private raw mode`,
  `redacted captured-source mode`, and `hash/anchor-only mode`
  (`IMPLICIT_SPEC.md:218-233`).
- Trace: W1 rejects SourceArtifact rows missing ref, hash/version,
  family/format, capture mode, or policy provenance (`PLAN.md:714-716`). W5
  writes transcript revisions with redacted content or structured payload
  (`PLAN.md:951-964`). Parse-error records emit SourceArtifact according to
  policy and no fake common object (`PLAN.md:985-994`).
- Fault tolerance: retry repeats source/provenance closure before base overwrite
  when completion is missing (`PLAN.md:2085-2088`).
- Race check: same source line from harvest and watch converges through
  deterministic source-line keys and completion (`PLAN.md:241-252`,
  `PLAN.md:1021-1028`).
- Flaws found: none found, with reasoning: policy-bearing source capture is
  validated before base rows and redaction-sensitive material is represented by
  capture mode and redacted/hash payloads.
- Verdict: PASS.

### W1 `object-container/import-material`

- Source: IMPLICIT_SPEC says accepted import decisions mean common base truth
  exists or is explicitly repairable, duplicate idempotent imports produce the
  same common read results, source-specific projections never replace base
  writes, rejected input does not create fake base objects, every accepted
  source-created container or derived unit has a SourceAnchor, and every accepted
  edge targets existing or repairable identities (`IMPLICIT_SPEC.md:345-385`).
- Trace: request `req-100` imports one transcript message, one tool result, one
  SourceArtifact, two SourceAnchors, revisions, and composition edges. W1 writes
  request audit, checks idempotency, checks completion, canonicalizes native
  claims, validates source/provenance closure, then writes source artifacts,
  containers, revisions, native claims, derived units, anchors, edges, source
  indexes, projection hints, event, decision, completion, source-line completion,
  and idempotency in that order (`PLAN.md:749-813`, `PLAN.md:1968-2071`).
- Fault tolerance: a crash after base rows but before completion makes retry
  overwrite the same deterministic rows and write completion (`PLAN.md:2090-2091`).
- Race check: conflicting replay with same idempotency key rejects before
  materialization (`PLAN.md:297-302`, `PLAN.md:2108-2111`).
- Flaws found: none found, with reasoning: completion and idempotency are last,
  and W1 validates native identity and provenance before common base overwrite.
- Verdict: PASS.

### W1 Source/Provenance Closure

- Source: IMPLICIT_SPEC says `Every source-created base object or derived unit
  needs an anchor` (`IMPLICIT_SPEC.md:235-238`).
- Source: IMPLICIT_SPEC requires source anchors to carry `source id`,
  `target kind`, `target id`, and `anchor hash or line hash`
  (`IMPLICIT_SPEC.md:113-122`).
- Trace: payload A contains `SourceArtifact src:tr:1`, `ObjectContainer
  oc:chat-message:1`, and `SourceAnchor sa:1` from `src:tr:1` to
  `oc:chat-message:1`. W1 collects payload source ids, source-created target
  ids, and anchors by target; because both source and target are in the payload,
  no durable read is required before accepting (`PLAN.md:2030-2042`).
- Bad scenario: payload B contains `ObjectContainer oc:chat-message:2` without a
  payload anchor and no durable anchor. W1 point/range reads durable anchors for
  that target and rejects or repair-pends before base rows, completion,
  source-line `:import-complete`, or accepted idempotency (`PLAN.md:2030-2049`).
- Bad scenario: payload C contains `SourceAnchor sa:3` whose `source/id` is
  missing from payload and durable `$$source-artifacts-by-id`. W1 rejects or
  repair-pends before base rows (`PLAN.md:2035-2036`, `PLAN.md:2043-2049`).
- Fault tolerance: retry repeats the closure check when completion is missing
  (`PLAN.md:2085-2088`).
- Race check: two concurrent clients can converge to the same accepted anchor
  only if the anchor/source/target closure resolves in payload or durable common
  state; otherwise both reject or repair-pend before base overwrite
  (`PLAN.md:789-795`, `PLAN.md:1490-1498`).
- Flaws found: none found, with reasoning: source-created targets, anchor
  sources, and anchor targets are all checked before materialization can succeed.
- Verdict: PASS.

### W1 Native Identity Claims

- Source: IMPLICIT_SPEC rejects `conflicting duplicate ids under the same
  idempotency key` and duplicate native ids with conflicting content
  (`IMPLICIT_SPEC.md:147-165`, `IMPLICIT_SPEC.md:376-385`).
- Trace: a transcript record contains two `tool_use` blocks with the same
  source-native id. If source-native id, source-line/provenance, source-anchor
  id, anchor hash, content hash, material fingerprint, and import key all match,
  W1 canonicalizes them to one claim. If any field differs, W1 writes rejected or
  repair-pending decision before PState reads, base rows, completion,
  source-line completion, or accepted idempotency (`PLAN.md:966-973`,
  `PLAN.md:2006-2017`).
- Durable conflict scenario: a later request with a different idempotency key
  tries to write the same `container/id` with a different content hash. W1 reads
  `$$native-identity-claims-by-container`, rejects or repair-pends, and returns
  before base overwrite (`PLAN.md:2018-2029`).
- Fault tolerance: retry repeats claim canonicalization and durable claim checks
  before row ensures (`PLAN.md:2074-2083`).
- Race check: concurrent clients for the same container id serialize on
  `object/key`, and the durable claim row protects against overwrite
  (`PLAN.md:150-153`, `PLAN.md:1607-1610`).
- Flaws found: none found, with reasoning: same-payload and durable conflicts
  are both checked before common base writes.
- Verdict: PASS.

### W2 `markdown/import-source`

- Source: IMPLICIT_SPEC requires markdown to preserve exact raw source, create a
  document ObjectContainer, create DerivedUnits for anonymous spans, preserve
  source order as common CompositionEdges, avoid duplicate rows on same
  ref/hash, and avoid overwriting graduated containers on new versions
  (`IMPLICIT_SPEC.md:387-417`).
- Trace: a markdown file `guide.md` with three paragraphs emits one
  SourceArtifact, one `:document` ObjectContainer with initial revision, three
  DerivedUnits, anchors for document and units, block tree/order edges, and
  outline hints (`PLAN.md:818-848`).
- Fault tolerance: duplicate same ref/hash uses deterministic source and object
  ids (`PLAN.md:136-141`, `PLAN.md:197-239`) and W1 completion/idempotency
  repair (`PLAN.md:304-322`).
- Race check: two imports of the same source version serialize on source-ref-key
  before materialization (`PLAN.md:185-189`).
- Flaws found: none found, with reasoning: markdown adapter emits common import
  material and relies on W1 for provenance, idempotency, and completion.
- Verdict: PASS.

### W3 `transcript/harvest`

- Source: IMPLICIT_SPEC says harvest does not spawn Claude/Codex, modify source
  files, or trigger downstream actions; harvest and watch produce the same
  native object identities; byte offsets/lengths/line hashes are computed from
  actual bytes; redaction happens before policy-sensitive durable persistence;
  parse errors are audit/projection facts; completion cannot hide missing common
  base material (`IMPLICIT_SPEC.md:418-448`).
- Trace: a folder harvest records `TranscriptIngestRunSchema` and uses existing
  parser/acquisition helpers to walk JSONL files, read complete observations,
  redact previews, compute transcript ids, source-line keys, and append W1
  records for complete source records (`PLAN.md:862-903`).
- Fault tolerance: W1 per-record completion controls common material, and run
  status complete cannot imply every source record succeeded unless audit and
  import completion rows say so (`PLAN.md:2185-2197`).
- Race check: harvest racing with watch converges through source-line keys and
  deterministic common ids (`PLAN.md:241-252`, `PLAN.md:1021-1028`).
- Flaws found: none found, with reasoning: harvest is acquisition/control only;
  common identity is created by W1.
- Verdict: PASS.

### W4 `transcript/watch`

- Source: IMPLICIT_SPEC says watch is passive, resumes from durable file/offset
  state, does not materialize partial trailing lines, maps the same source line
  to the same common identities as harvest, and file offset state cannot prove
  common base success unless repair is possible (`IMPLICIT_SPEC.md:450-478`).
- Trace: watch records/passively maintains a run and application-side polling
  appends W1 for newly complete records (`PLAN.md:904-925`). File-state rows
  include generation identity and safe/observed offsets (`PLAN.md:1370-1392`).
- Fault tolerance: file offset advancement checks source-line status and matching
  import completion before crossing a line (`PLAN.md:2217-2228`).
- Race check: multiple watch loops over the same file converge through
  source-line status and import completion (`PLAN.md:1010-1028`).
- Flaws found: none found, with reasoning: watch cannot advance safe resume from
  observed-only state and cannot treat operational offsets as base truth.
- Verdict: PASS.

### W5 `transcript/observe-record`

- Source: IMPLICIT_SPEC requires conversation/session as `:chat-conversation`,
  message as `:chat-message`, tool-use as `:tool-call`, tool-result as
  `:tool-result` with result content when content exists, no empty placeholder
  for result content, every valid transcript container gets a common
  SourceAnchor, composition/tool structure becomes common edges, and transcript
  projections are secondary (`IMPLICIT_SPEC.md:479-514`).
- Trace: a valid assistant source record with message id `m1`, tool call
  `toolu_1`, tool result `r1`, and artifact `a1` emits source-record
  SourceArtifact, chat conversation, message, tool-call, tool-result,
  chat-artifact, optional run ObjectContainers, native identity claims,
  revisions, source anchors, composition edges, and projection hints
  (`PLAN.md:927-983`).
- Fault tolerance: tool-result content is a common RevisionSchema row, not only a
  projection (`PLAN.md:943-964`), and completion follows base/projection writes
  (`PLAN.md:2050-2070`).
- Race check: duplicate source-line observation converges through source-line key,
  native claims, and completion (`PLAN.md:966-983`, `PLAN.md:2006-2029`).
- Flaws found: none found, with reasoning: all transcript identity-bearing
  material enters common ObjectContainer and Revision rows.
- Verdict: PASS.

### W5 Tool Result Before Tool Call

- Source: IMPLICIT_SPEC says a tool result before a matching tool call must
  preserve the result as a real object and either defer/repair the produced edge
  or record an auditable missing-parent condition (`IMPLICIT_SPEC.md:506-509`).
- Trace: source record line 50 contains result `r1` for missing call `toolu_1`.
  W5 creates real `:tool-result` ObjectContainer and Revision. W1 writes a
  `PendingCompositionEdgeSchema` keyed by expected tool-call id and audits the
  missing parent. When the tool-call arrives, pending edge is repaired into
  common CompositionEdge rows (`PLAN.md:2581-2599`).
- Fault tolerance: pending edge queue is durable and subindexed
  (`PLAN.md:1577-1582`, `PLAN.md:2710-2713`).
- Race check: if the tool call arrives concurrently, both events route through
  the conversation `object/key`, and the pending edge repair branch converges
  (`PLAN.md:150-153`, `PLAN.md:2054-2056`).
- Flaws found: none found, with reasoning: result content is never demoted to a
  projection-only placeholder.
- Verdict: PASS.

### W5 Artifact And Run Identity

- Source: IMPLICIT_SPEC requires `:chat-artifact` when the source exposes an
  artifact/attachment/output and `:agent-run` or `:execution-run` when the source
  distinguishes a run (`IMPLICIT_SPEC.md:180-189`).
- Source: PRODUCT maps `artifact id, if present -> ObjectContainer kind
  :chat-artifact` (`PRODUCT.md:284-300`).
- Trace: W5 emits chat-artifact ObjectContainers and agent/execution run
  ObjectContainers with initial revisions and anchors when the source exposes
  those identities (`PLAN.md:947-964`). Common R2 includes `:chat-artifact` and
  `:agent-run / :execution-run` (`PLAN.md:390-401`).
- Fault tolerance: missing required artifact/run containers are rejected before
  common base rows (`PLAN.md:718-719`) and artifact/run content or metadata must
  not be silently dropped (`PLAN.md:2571-2576`).
- Race check: native claim checks protect source-native artifact/run ids before
  base overwrite (`PLAN.md:2006-2029`).
- Flaws found: none found, with reasoning: artifact and run identity are common
  ObjectContainers with common Revision rows, not projection-only data.
- Verdict: PASS.

### W6 `transcript/file-state-observation`

- Source: IMPLICIT_SPEC says file offset state supports watch resume but is not
  canonical object truth; empty files can create auditable source facts without
  fake message containers; file progress cannot permanently hide missing common
  base writes; file-state rows record request/source/policy context
  (`IMPLICIT_SPEC.md:516-538`).
- Trace: W6 writes file offsets, source-line statuses, empty-file audit, and
  optional empty-file SourceArtifact through W1 (`PLAN.md:996-1039`). It can
  advance safe resume only through source-line statuses backed by import
  completion or explicit parse-error audit (`PLAN.md:2217-2228`).
- Fault tolerance: stale generation, truncation, changed file id/fingerprint,
  policy change, or saved offset into a partial record writes stale/unsafe status
  and does not skip complete lines (`PLAN.md:2201-2212`).
- Race check: concurrent scans converge on greatest safe complete offset because
  observed offset can be ahead while last-byte-offset remains gated by completion
  (`PLAN.md:2226-2228`).
- Flaws found: none found, with reasoning: file progress is operational and is
  explicitly barred from proving base materialization.
- Verdict: PASS.

### W7 `object/edit`

- Source: IMPLICIT_SPEC says editing a DerivedUnit graduates it at most once,
  editing an ObjectContainer appends a Revision, SourceAnchor provenance survives,
  re-import does not overwrite authored revisions, duplicate edit ids are
  idempotent, and concurrent edits produce one container and ordered revisions
  (`IMPLICIT_SPEC.md:540-567`).
- Trace: editing derived unit `du:1` computes fingerprint, writes request audit,
  checks idempotency and edit order, resolves target, writes text-block
  ObjectContainer, first Revision, UnitGraduation, SourceAnchor, outline update,
  event, decision, and idempotency (`PLAN.md:2114-2166`).
- Fault tolerance: if failure happens after material rows but before edit order,
  retry overwrites deterministic rows and then writes EditOrderSchema; if failure
  happens after EditOrderSchema, retry repairs event/decision/idempotency without
  appending a new revision (`PLAN.md:2168-2183`).
- Race check: concurrent edits are ordered by `EditOrderSchema` keyed by edit
  lineage/client and stale/newer edits reject (`PLAN.md:2135-2143`).
- Flaws found: none found, with reasoning: edit order and idempotency prevent
  duplicate graduation and stale overwrite.
- Verdict: PASS.

### W8 `source-specific/projection-upsert`

- Source: IMPLICIT_SPEC says projection rows never become the only durable
  identity store, carry enough refs to explain themselves, are rebuildable or
  reconcilable, and source-line ledger cannot suppress projection repair
  (`IMPLICIT_SPEC.md:568-594`).
- Trace: W8 projection writes are materialized inside W1 from typed projection
  hints after common base rows are ensured (`PLAN.md:1066-1083`). Projection
  ownership is in the common module for this slice so completion certifies both
  common base and required projections (`PLAN.md:1702-1738`).
- Fault tolerance: if projection is missing after partial failure, absence of
  completion forces W1 retry to repair it (`PLAN.md:1081-1083`).
- Race check: concurrent repair and live writes use common ids, source-line keys,
  order keys, or request ids and converge through full-row deterministic
  upserts (`PLAN.md:815-816`, `PLAN.md:1066-1083`).
- Flaws found: none found, with reasoning: projections remain non-canonical and
  completion cannot bypass their required repair in this slice.
- Verdict: PASS.

### Reads R1-R11

- Source: IMPLICIT_SPEC says all reads may be called in any entity state and
  missing/partial material returns nil, empty collections, rejected/pending
  status, or explicit error records rather than throwing
  (`IMPLICIT_SPEC.md:596-600`).
- R1 trace: source artifact by id is one direct point read; exact ref/version is
  handled by query topology (`PLAN.md:345-377`, `PLAN.md:2237-2274`).
- R2 trace: one common read from `$$containers-by-id` works for markdown
  document/text-block and transcript conversation/message/tool/result/artifact/run
  containers (`PLAN.md:378-405`).
- R3 trace: current revision branches on missing/invalid container and reads
  `$$revisions-by-id` only when current revision exists (`PLAN.md:406-435`,
  `PLAN.md:2309-2344`).
- R4 trace: derived unit read combines `DerivedUnitSchema` with optional
  graduation state through a dynamic query topology (`PLAN.md:437-459`,
  `PLAN.md:2346-2378`).
- R5 trace: source anchors by target uses a plural subindexed range read
  (`PLAN.md:461-478`).
- R6 trace: composition children and parents are both subindexed range reads and
  work for markdown and transcript structure (`PLAN.md:479-506`).
- R7 trace: request/decision/audit reads are direct point/range reads
  (`PLAN.md:508-539`).
- R8 trace: common material for source uses category-specific range reads and a
  query topology for bundled responses (`PLAN.md:540-570`,
  `PLAN.md:2380-2414`).
- R9 trace: markdown outline projection is an ordered subindexed range read whose
  rows distinguish derived and graduated targets (`PLAN.md:572-592`).
- R10 trace: transcript conversation projection is an ordered subindexed range
  read carrying common refs and visibly non-canonical parse-error entries
  (`PLAN.md:594-611`).
- R11 trace: transcript operational views expose run status, file offsets,
  source-line repair ledger, tool indexes, and audit while not proving base
  identity (`PLAN.md:613-653`).
- Fault tolerance: read-current-revision returns repair-needed for invalid
  accepted nil-current state, and import/audit reads expose pending/rejected
  states (`PLAN.md:2309-2344`, `PLAN.md:508-539`).
- Race check: reads observe PState state after deterministic stream upserts; the
  write plan prevents accepted completion/idempotency from preceding the rows
  the reads need (`PLAN.md:304-322`, `PLAN.md:1968-2071`).
- Flaws found: none found, with reasoning: every required read has an access
  path, bounded seek behavior, partial-state behavior, and common-substrate proof.
- Verdict: PASS.

### Source-Specific Base Island Fail Condition

- Source: PRODUCT says a plan fails if it creates
  `markdown containers -> common ObjectContainer store` and
  `transcript containers -> transcript-local durable identity store`
  (`PRODUCT.md:409-424`).
- Source: IMPLICIT_SPEC repeats that fail condition and says transcript-local
  SourceArtifact/SourceAnchor/CompositionEdge rows cannot be the only base path
  (`IMPLICIT_SPEC.md:1296-1316`).
- Trace: the recommended Phase 3 direction is to extend
  `object_container.clj` as the common import owner, retire or bypass the
  transcript-local base container store, and preserve transcript run/projection
  behavior as projection/operational PStates (`PLAN.md:69-81`). Compatibility
  explicitly forbids tests that pass only by reading transcript-local
  `TranscriptContainerRow` (`PLAN.md:2633-2650`).
- Fault tolerance: even if transcript projections exist, completion and common
  reads prove common base material (`PLAN.md:304-322`, `PLAN.md:1066-1083`).
- Race check: transcript identity enters common native identity claims and common
  containers on the conversation object task (`PLAN.md:150-153`,
  `PLAN.md:927-983`).
- Flaws found: none found, with reasoning: transcript-local base identity is not
  a valid proof path in this plan.
- Verdict: PASS.

### Acceptance Proof Requirements

- Source: PRODUCT requires proof that one common read can fetch markdown and
  transcript containers, one common composition read traverses source-declared
  structure, one common source-anchor read traces both objects, and
  source-specific projections are rebuildable or non-canonical
  (`PRODUCT.md:426-433`).
- Source: IMPLICIT_SPEC lists the later proof requirements for markdown
  documents/text-blocks, transcript conversation/message/tool-call/tool-result,
  tool-result revision content, source artifact, source anchor, composition
  edge, projections, parse errors, re-harvest/watch dedupe, and ledger safety
  (`IMPLICIT_SPEC.md:1273-1294`).
- Trace: R2, R3, R5, R6, R8, R9, R10, and R11 supply the proof read paths
  (`PLAN.md:378-653`). W2 and W5 create the required markdown and transcript
  material (`PLAN.md:818-848`, `PLAN.md:927-983`). Parse errors emit
  non-canonical projection/audit rows and no fake containers (`PLAN.md:985-994`).
- Fault tolerance: W1 retry and file-state gating prevent completion,
  idempotency, and source-line ledger rows from hiding missing common base or
  projection writes (`PLAN.md:2074-2111`, `PLAN.md:2217-2228`).
- Race check: re-harvest and watch converge through source-line keys,
  completion rows, native claims, and deterministic row overwrites
  (`PLAN.md:241-252`, `PLAN.md:815-816`, `PLAN.md:2006-2029`).
- Flaws found: none found, with reasoning: every acceptance proof has a concrete
  common read or durable non-canonical projection path.
- Verdict: PASS.

## Validation Gate Summary

The explicit gates in `PLAN.md:2789-2836` are satisfied:

- Transcript conversation/message/tool-call/tool-result identity is common:
  PASS, W5 emits common ObjectContainers and R2 reads them
  (`PLAN.md:927-957`, `PLAN.md:390-401`).
- Transcript chat-artifact and agent-run/execution-run identity is common when
  source exposes it: PASS (`PLAN.md:947-964`).
- Every accepted ObjectContainer has current-revision/id and matching Revision:
  PASS (`PLAN.md:324-341`, `PLAN.md:720-722`, `PLAN.md:2050-2051`).
- Tool-result content is in common RevisionSchema: PASS (`PLAN.md:943-964`).
- Artifact/run content or metadata is in common RevisionSchema: PASS
  (`PLAN.md:959-964`).
- Transcript SourceArtifact/SourceAnchor/CompositionEdge rows are common: PASS
  (`PLAN.md:941-957`, `PLAN.md:1500-1616`).
- Transcript SourceArtifact rows populate exact source ref/version index: PASS
  (`PLAN.md:941-943`, `PLAN.md:2057-2059`).
- Every accepted source-created ObjectContainer or DerivedUnit has a SourceAnchor:
  PASS (`PLAN.md:842-848`, `PLAN.md:975-983`, `PLAN.md:2030-2049`).
- Every SourceAnchor source/id resolves to SourceArtifact: PASS
  (`PLAN.md:2035-2036`, `PLAN.md:2043-2049`).
- Every SourceAnchor target/id resolves to ObjectContainer or DerivedUnit: PASS
  (`PLAN.md:2037-2039`, `PLAN.md:2043-2049`).
- Missing source/provenance closure is rejected or repair-pended before base rows,
  completion, source-line completion, or accepted idempotency: PASS
  (`PLAN.md:789-795`, `PLAN.md:2030-2049`).
- Parse errors do not create fake ObjectContainers: PASS (`PLAN.md:985-994`).
- Source-line/import completion follows common base and projection rows: PASS
  (`PLAN.md:2050-2070`).
- Transcript file offsets advance only through completion or explicit parse-error
  audit: PASS (`PLAN.md:2217-2228`).
- Stale file generation/truncation/rotation/path reuse/saved offset ahead of
  length produces visible repair/reject and does not skip records: PASS
  (`PLAN.md:1029-1039`, `PLAN.md:2201-2212`).
- Same idempotency key with different fingerprint rejects before materialization:
  PASS (`PLAN.md:297-302`, `PLAN.md:2108-2111`).
- Same-payload duplicate source-native ids canonicalize only if identical and
  otherwise reject/repair-pend before PState claim reads or base overwrite: PASS
  (`PLAN.md:966-973`, `PLAN.md:2006-2017`).
- Same source-native id with different content/provenance/material rejects or
  repair-pends before base overwrite even with a different idempotency key: PASS
  (`PLAN.md:2018-2029`).
- Common reads prove markdown and transcript share one substrate: PASS
  (`PLAN.md:378-653`).
- Source-specific projections are explicitly non-canonical: PASS
  (`PLAN.md:1066-1083`, `PLAN.md:1702-1738`).
- PState schemas avoid broad Object storage: PASS (`PLAN.md:1089-1090`).
- Large per-source collections are subindexed: PASS (`PLAN.md:2464-2486`).
- Query topologies list variable meaningful reads, dynamic handling, and required
  routing: PASS (`PLAN.md:2235-2460`).

PHASE_VALIDATION:pass
