# Trail-View WP1 — IMPLICIT_SPEC (Phase 0, fresh-context re-derivation)

Status: Phase-0 derived artifact (Opus, fresh context, 2026-07-04). Derived FROM
`build/trail-view/CONTRACT.md` v1 (BINDING) text ONLY, cross-checked against the
on-disk code and Rama references named in the contract's §13 manifest. This is a
DERIVED artifact: on any conflict with the CONTRACT the contract governs
(work-package precedence). A genuine two-reading conflict is recorded in §5
below, NOT resolved here.

Reading rule inherited from the F2 lesson: every entity, key, journal, PState, or
uniqueness claim below names its **partition scope** in the same sentence that
introduces it. Partition-free prose is out of spec for this document.

Convention: the relation kernel's id-part separator is the NUL control byte,
written here only as the word **NUL** (never the raw byte / escape — the trap has
corrupted files here three times).

---

## 0. Owning-module topology (the seam this package spans)

Three EXISTING owning modules supply everything this package reads; the package
adds ONE new module and amends ONE existing module:

- **`object-container-module`** (`object_container.clj`, 2,648 lines, hardened
  Jun 7–8) — a stream topology whose PStates are almost all keyed by
  `extract-object-key` (`partition-by-object-key`), so a document and its blocks,
  and a conversation and its messages, colocate on ONE task. Mirror-read only by
  this package; NEVER written.
- **`object-container-transcript-ops-module`** (same file, `defmodule` at
  `object_container.clj:2547`) — a SEPARATE stream topology owning
  `$$transcript-file-offsets` (keyed by `file-key`, default partitioner) and
  `$$transcript-runs` (keyed by `request-id`, default partitioner). Mirror-read
  only; NEVER written. This is the "ops module" the contract §6/§7 names.
- **`relation-kernel-module`** (`relation_kernel.clj`, 829 lines) — one microbatch
  topology (all decision/journal/audit/authoritative rows colocated on
  `hash(relation-id)`; the two endpoint copies + descriptors on
  `hash(target-key)`) + two query topologies R1/R2. **This package AMENDS it**
  (custody fields, three stance kinds, the activity PState + write, R3).
- **`trail-view-module`** (NEW `src/app/server/rama/trail_view.clj`) — declares
  ONLY mirrors + query topologies; ZERO depots, ZERO stream/microbatch
  topologies, ZERO own PStates (§2, gate 14). Reads OC/ops PStates as mirror
  `local-select>` inside its own query topologies, and ALL relation data via the
  kernel query topologies R1/R2/R3 (never mirror-selects a relation-kernel
  PState — §7 style gate).

Launch order (harness + prod): the three source modules launch BEFORE
`trail-view-module` (mirror dependencies are enforced at launch — Rama
`18-module-dependencies.md`; verification duty §7/§12).

---

## 1. Operations inventory

### 1a. Relation-kernel amendments (write-side; the ONLY writes this package makes)

All amendments land in `relation_kernel.clj` + its test ns; the depot envelope
stays a plain map whose partitioner-read key is the top-level
`:relation/routing-key` (= relation-id), typed records ride only inside payload
fields and PStates (cycle-1 F1 rule; §5 preamble).

| # | Operation | Inputs | Output / effect | PStates written / read | Partition scope |
|---|---|---|---|---|---|
| A1 | **`RelationDecisionRow` +custody** | envelope `:actor` (`:actor/id`,`:actor/type`) at decision time | record gains `envelope-actor-id` + `envelope-actor-type` | writes `$$relation-decisions-by-idempotency`, `$$relation-decisions-by-id` | both on `hash(relation-id)` (journal keyed rel-id→journal-key; audit key-partitioned `partition-by-decision-relation`) |
| A2 | **`RelationEventRow` +custody** | same envelope `:actor` | record gains `envelope-actor-id` + `envelope-actor-type` (full custody trail lives here + decisions) | writes `$$relation-events-by-id` (accepted branch only) | `hash(relation-id)` via `partition-by-event-relation` |
| A3 | **`RelationEdgeRow` +custody** | same envelope `:actor` (LAST transition only) | authoritative + both copies gain `envelope-actor-id` + `envelope-actor-type` | writes `$$relations-by-id` (rel-id task) + `$$relations-by-target` from-copy & to-copy | authoritative on `hash(relation-id)`; from-copy on `hash(from.target-key)`, to-copy on `hash(to.target-key)` |
| A4 | **Envelope `:request/sent-at-ms`** | wall/semantic time supplied by the CLIENT builder | `assert-request`/`retract-request` add top-level `:request/sent-at-ms`; a new accessor reads it off `*request`; **fallback when absent = payload `:asserted-at-ms`** | read by A7 (arrival-at-ms); NOT partitioner-read | lives on the plain-map envelope on the relation-id task; never crosses into identity |
| A5 | **Registry += 3 stance kinds** | n/a (one-line set edit) | `relation-kinds` gains `:confirms :refutes :supersedes` → **10 kinds** | affects `registered-kind?` validation; R1 kind-filter prefix reads; descriptor bound | descriptor PState `$$relation-target-descriptors` stays ≤ 2×10 = 20 rows per target-key, non-subindexed, on `hash(target-key)` |
| A6 | **`RelationActivityRow` defrecord (new)** | fields per §5.3 | 16-field record: `order-key bucket relation-id relation-kind from-kind from-id to-kind to-id asserter-actor-id asserter-type envelope-actor-id relation-status previous-status event-id claimed-at-ms arrival-at-ms` | (schema only) | rides inside `$$relation-activity-by-bucket` values |
| A7 | **Activity write (accepted-branch 4th hop)** | the accepted `*outcome` (row/event fields) + arrival-at-ms | new PState `$$relation-activity-by-bucket`; one row appended per ACCEPTED transition after the to-side copy hop, via `(|hash bucket)`; `claimed-at-ms` = payload `:asserted-at-ms`, `arrival-at-ms` = envelope `:request/sent-at-ms` (fallback `:asserted-at-ms`); `bucket = (quot arrival-at-ms 86400000)` zero-padded fixed width; `order-key = (oc/fixed-width-order-key arrival-at-ms event-id)`; **topology never reads the wall clock** | writes `$$relation-activity-by-bucket`; reads nothing new | activity rows unique per `event-id` within their bucket, on the **bucket's task** `hash(bucket)`; subindexed inner map |
| A8 | **R3 query topology `relation-activity`** | `[bucket-lo bucket-hi]` (fixed-width bucket strings) | ordered `RelationActivityRow`s across the bucket range | reads `$$relation-activity-by-bucket` via subindexed range per bucket, `{:allow-yield? true}` | fans `|hash bucket` per bucket, aggregates at `|origin`; each bucket read is one seek + sequential iteration on that bucket's task |

Sequencing law (§5.1, §12): **A1 lands FIRST (Phase A1)**, before real
assertions accumulate; A2/A3 accompany it; A5 = Phase A2; A6/A7/A8 = Phase A3.
Gates for Phase A: 9, 10, 15, 16.

### 1b. Trail-view-module query topologies (read-only; zero writes)

| # | Query topology | Args | Output shape | Reads (direct mirror PStates unless noted) | Partition scope of the reads |
|---|---|---|---|---|---|
| B1 | **`context-bundle`** | `targets` (vec of ids), `opts` (`:layers` `:caps`) | `{:bundle/address :bundle/rendered-at-ms :bundle/targets {id→target-bundle} :bundle/omissions [...]}` (L0–L5 per target) | mirror PStates `$$containers-by-id`, `$$revisions-by-id`, `$$source-artifacts-by-id`, `$$source-anchors-by-target`, `$$composition-children-by-parent`, `$$composition-parent-by-child`, `$$outline-by-document`, `$$transcript-conversation-projection`, `$$derived-units-by-id`, `$$unit-graduations-by-id`; mirror-QUERIES `read-common-material-for-source` (OC), R1 `relations-for-targets` (kernel), R2 `relation-detail` (kernel, for `rel:*` targets) | per target it derives `target-key = extract-object-key(id)` and reads the OC family on `hash(object-key)` (≤1 container/source point read + 1 structure page); R1 is invoked once per target-key on `hash(target-key)` and returns the whole colocated key-family's relations |
| B2 | **`recent-activity`** | `window` (`:from-ms`/`:to-ms`), `opts` (`:clock`) | `{:feed/address :feed/rendered-at-ms :feed/entries [...] :feed/omissions [...]}` ordered by chosen clock desc | mirror-QUERY R3 `relation-activity` (kernel); mirror PStates `$$transcript-file-offsets`, `$$transcript-runs` (ops module), `$$source-latest-by-ref`, `$$source-ingest-completions-by-ref`, `$$transcript-source-lines-by-file` (OC) | R3 over arrival buckets on `hash(bucket)`; `$$transcript-file-offsets`/`$$transcript-runs` are **cross-partition scans** of their whole (file-key / request-id-partitioned) maps filtered by `updated-at-ms`; conversation address resolved via the **tail row** of `$$transcript-source-lines-by-file` for a file-key (`source-id` → `extract-object-key` → `oc:chat-conversation:` id); `$$source-latest-by-ref`/`$$source-ingest-completions-by-ref` scanned on `hash(source-ref-key)` filtered by the window |
| B3 | **`conversation-trail`** | `conversation` (id), `cursor`, `limit` | ordered page of the threaded/DAG timeline node expansion, byte-offset order | mirror PStates `$$transcript-conversation-projection` (+ `$$transcript-last-message-by-conversation`) | 1 subindexed page per call on `hash(extract-object-key(conversation))` = `chat:<conversation-id>`; last-message on the same object-key task |

Named platform fallback (§7): IF a query topology cannot invoke a mirror query
topology (query→mirror-query), `context-bundle` returns only the material layers
and the CLIENT WRAPPER composes R1/R3 results (2 roundtrips). Wrapper signatures
and result shapes are unchanged either way; this is a verification duty, not a
policy fork (both branches pre-specified).

### 1c. Client wrappers + pure fns (`app.server.rama.trail-view` — the ONLY product surface)

| # | Fn | Kind | Inputs → outputs | Notes |
|---|---|---|---|---|
| C1 | `read-context-bundle` | foreign wrapper | targets, opts → bundle | invokes B1; **stamps `:bundle/rendered-at-ms` client-side** (the ONLY place a clock is read on the read path) |
| C2 | `read-recent-activity` | foreign wrapper | window, opts → feed | invokes B2; stamps `:feed/rendered-at-ms` client-side |
| C3 | `read-conversation-trail` | foreign wrapper | conversation, cursor, limit → trail page | invokes B3; stamps `:trail/rendered-at-ms` client-side (parallel to bundle/feed) |
| C4 | `read-relation-detail` | foreign wrapper | relation-id → `{:row :history}` | **delegates directly to kernel R2** (§3 law 4, §7); no trail-view query topology |
| C5 | `->address` | pure | (query-name, params) → `(trail/<query> <params-map>)` one-line EDN literal | address vocabulary is plain documented EDN (trap 8) |
| C6 | `resolve-address` | foreign wrapper | rendered EDN address → result of the same shape over CURRENT truth | parse + dispatch to the matching wrapper; NO as-of/time-travel (§9.1); round-trip law (gate 2) |
| C7 | `trail/via` resolution | foreign wrapper | `{:spec <doc-id> :rev :latest|<pin> :overrides {...}}` → invoke the resolved spec | reads the spec doc (via OC `read-current-revision` for `:latest`, `read-source-by-ref-version` for a pin), shallow-merges `:overrides` at params level, invokes the named query. View itself writes NOTHING (§3). |
| C8 | `current-verdicts` | **pure fold, exported** | the target-bundle's `:relations` stance-kind rows → per judged target, per asserter, the latest ASSERTED stance row | retracted stances drop from `:current`; disagreeing asserters BOTH current, unmerged; feeds bundle L4 and the text projection (§5.2, gates 6/7) |
| C9 | `render-bundle-text` | **pure** over a bundle | bundle → `;; trail-text v0` text (fixed section order + markers) | deterministic given the bundle (reads `rendered-at` FROM the bundle, does not generate it); ≤ 4,000 chars on the fixture (gate 13) |

---

## 2. Invariants (each cited to a § or gate; partition-scoped)

I-1. **Read-only by construction.** `trail-view-module` declares no depots and no
stream/microbatch topologies; its source contains no `foreign-append!` /
`local-transform>`. A module with no depots physically cannot write kernel truth.
(§2; gate 14.)

I-2. **Consumers touch ONLY the §7 client wrappers** — never a PState, never a raw
`foreign-select`. The seam that keeps the module-reversal cost to a wrapper rename.
(§2 reversal-cost; §7.)

I-3. **Relation data crosses the seam only via query topologies R1/R2/R3.** The
trail-view module never mirror-selects a relation-kernel PState (`$$relations-by-*`,
descriptors, activity); it reads OC/ops PStates as mirror `local-select>` but
relation truth only through R1/R2/R3, each colocated on its own partition
(`hash(target-key)` for R1, `hash(relation-id)` for R2, `hash(bucket)` for R3).
(§7 style gate; §5.3.)

I-4. **The activity write happens ONLY in the microbatch's accepted branch**, as a
4th partition hop `(|hash bucket)` after the to-side copy, exactly-once per attempt
within the batch. Rejected decisions and journal-replayed duplicates (filtered by
`(nil? *prior-decision)` on the relation-id task before the outcome) write ZERO
activity rows. (§5.3; gate 9.)

I-5. **The relation-kernel topology never reads the wall clock.** `bucket`,
`order-key`, `claimed-at-ms`, and `arrival-at-ms` all derive from client-supplied
envelope/payload times on the relation-id task; a crash-replayed microbatch
re-derives byte-identical rows on the bucket task. This is the exact anti-pattern
the OC kernel commits at `object_container.clj:428,448,467` (`decided-at-ms =
(core/now-ms)`) — do NOT copy it. (§5.3, trap 4b; gate 9.)

I-6. **Both clocks on every activity row and every feed entry.** Each
`RelationActivityRow` and each `<entry>` carries `claimed-at-ms`/`:time/claimed-ms`
(nil is honest where absent) AND `arrival-at-ms`/`:time/arrival-ms`. A July
re-ingest of April notes appears in today's `:arrival` window and April's
`:claimed` window, never the reverse. (§5.3, §6, trap 4; gate 4.)

I-7. **Custody is recorded but is NOT in relation identity.** `relation-id` stays
`sha1(kind, from.kind, from.id, to.kind, to.id, asserter-actor-id)` (asserter-scoped,
custody-free), so two agents writing Sid's same assertion converge on ONE relation
on one `hash(relation-id)` task, with `envelope-actor-*` recorded per transition on
the decision/event rows (rel-id task) and last-transition custody on the edge row
(rel-id task) + both copies. Putting the writer into identity would fork one fact
into per-agent duplicate rows (trap 11). (§5.1; gate 10.)

I-8. **`:written-by` is projected only when it differs from `:asserted-by`.** The
edge/text projection omits `:written-by` when envelope actor == payload asserter;
the map still never lies about authorship. (§5.1; gates 10, 13.)

I-9. **Omissions reconcile to totals (exactness rule).** Every cap, truncation,
page boundary, and unresolvable id appears in an `:omissions`/`:bundle/omissions`
entry with counts and (where resumable) a cursor; with caps forced below fixture
sizes, returned + omitted == fixture totals per layer. A bundle never silently
narrows. (§4 omissions law, trap 7; gate 3.)

I-10. **The feed carries a STATIC `:feed/uncovered` declaration.** Named gaps
(direct editor ObjectEditPayload revisions; thin OC session metadata) are recorded
as omission-kind `:feed/uncovered` in the feed's standing `:feed/omissions` so an
agent reading the feed knows what the feed cannot see. (§6; gate 3.)

I-11. **Verdict rows ARE relations; the verdict layer is a fold, not storage.**
`:confirms`/`:refutes`/`:supersedes` are relation-kinds in the one registry;
`current-verdicts` is a pure fold (per judged target, per asserter, latest asserted
stance-kind row; order by claimed time, tiebreak `relation-id`). Verdicts inherit
identity, idempotency (relation-scoped journal on `hash(relation-id)`), retraction
rights, status history, evidence anchoring, and descriptor-gated reads for free.
(§5.2, traps 1–2.)

I-12. **Disagreement is preserved, never merged.** Two asserters' stances on the
same target are BOTH `:current`, badged, unmerged — free from asserter-in-identity.
(§5.2, I-10 input; gate 6.)

I-13. **Stance kinds are binary and directed.** `from` = judgment carrier (recheck
section / superseding doc / the CLI conversation-or-message container where it was
uttered — always ingested, so the span exists), `to` = judged thing;
`evidence-anchor-id` points at the judging span. No unary/`:none` stance form.
`:supersedes` = belief displacement (`from` = replacement, `to` = displaced),
distinct from construction-lineage kinds. (§5.2, trap 5.)

I-14. **Attestation = re-assertion.** The kernel already bumps
`status-changed-at-ms` on re-assert (`transition-row`, `relation_kernel.clj:281`);
`last-attested-ms` in bundle L5 = max over stance/relation transitions touching the
target. No new machinery. (§5.2; gate 8.)

I-15. **`last-walked-ms` is structurally nil in WP1** and renders as `walked
unknown`; absence of walk data must never read as freshness. Walk capture is a
write surface gated on the read→write milestone, never smuggled through the read
path. (§9.2, trap 11b; gate 8.)

I-16. **Addresses are self-describing, readable, and round-trip to NOW.** Every
query result carries its own `:bundle/address` / `:feed/address` / `:trail/address`
(the exact address that produced it); parsing a rendered address and re-invoking
returns the same shape over current truth; `rendered-at-ms` is the drift detector;
addresses are plain documented EDN. NO as-of resolution. (§3 laws 1–3, trap 8; gate 2.)

I-17. **`rendered-at-ms` is stamped by the client wrapper, never inside a query
topology.** Query topologies are deterministic over PState state; only the foreign
wrapper (C1–C3) reads a clock. (§4 "wrapper-stamped, client side"; §6.)

I-18. **Every `:preview` is a mechanical length-capped prefix, never a paraphrase.**
LLM summaries are asserted material by an actor, not assembler magic. (§9.6.)

I-19. **Anchor offset unit is explicit on every raw/anchor block.** Markdown anchors
are CHAR offsets (`markdown_adapter.clj` uses `count`, verified at
`markdown_adapter.clj:273` `(count raw-text)`); transcript anchors are BYTE offsets
(`transcript_adapter.clj:142-152` `:source/byte-offset`+`:source/byte-length`).
`:offset-unit :chars|:bytes` rides every block so spans do not misresolve on
multi-byte chars. (§4, trap 12; gate 5.)

I-20. **Key-family grouping is one R1 seek per target.** A doc and its blocks share
one `extract-object-key`, so ONE R1 read on `hash(object-key)` returns the whole
family; the bundle attributes each row to its actual endpoint id — requested-id rows
→ `:this`, sibling-id rows → `:in-family` keyed by that sibling id. (§4, trap 6;
gate 1.) Verified: `extract-object-key` maps `oc:doc:<k>`, `oc:block:<k>:<u>`,
`oc:chat-conversation:<k>`, `oc:chat-message:<k>`, `du:<k>:…` all to the same
leading key `<k>` (`object_container.clj:279-334`).

I-21. **The descriptor bound holds after the registry grows.** With 10 kinds the
per-target-key descriptor PState `$$relation-target-descriptors` stays ≤ 20 rows,
non-subindexed, on `hash(target-key)`. (§5.2 registry-scope note; gate 15.)

I-22. **The existing relation-kernel suite (2 tests, 165 assertions) stays green
unmodified**, and its gates re-run green against the amended records (positional
constructors updated for the arity change). (§5 preamble; gate 16.)

I-23. **Convergent re-import is order/count-stable.** The OC path is the sole
material source; standalone `tc:*` transcript modules are out of scope;
re-importing the same transcript leaves conversation-trail order and count
unchanged (deterministic ids). (§2, trap 13; gate 11.)

---

## 3. Entity × write matrix

Legend: **W** = written by this package; **R** = read by this package; **R(q)** =
read only transitively inside a mirror query topology (colocated in the owning
module, NOT directly mirrored by trail-view). "—" = untouched. Partition scope is
stated per row; it is load-bearing.

### 3a. relation-kernel-module PStates (owned; amended/new)

| PState (partition scope) | A1 dec | A2 evt | A3 edge | A5 registry | A7 activity | R1 | R2 | R3 | B1 bundle | B2 feed | C4/C8 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `$$relation-decisions-by-idempotency` — journal, keyed rel-id→journal-key, subindexed inner map, on `hash(relation-id)` | **W**(+custody) | — | — | — | — read as journal gate | — | — | — | — | — | — |
| `$$relation-decisions-by-id` — audit, `hash(relation-id)` via `partition-by-decision-relation` | **W**(+custody) | — | — | — | — | — | — | — | — | — | — |
| `$$relation-events-by-id` — audit, `hash(relation-id)` via `partition-by-event-relation` | — | **W**(+custody) | — | — | — | — | — | — | — | — | — |
| `$$relations-by-id` — authoritative row, `hash(relation-id)` | — | — | **W**(+custody, last transition) | — | — read as current-row | — | **R** (R2) | — | R(q) via R2 | — | R2 via C4 |
| `$$relation-status-log-by-relation` — status history, subindexed, `hash(relation-id)` | — | — | — | — | — | — | **R** (R2 history) | — | R(q) via R2 (`rel:*`) | — | R2 via C4 (gate 7) |
| `$$relations-by-target` — from-copy on `hash(from.target-key)`, to-copy on `hash(to.target-key)`, subindexed per target-key | — | — | **W** both copies (+custody) | — | — | **R** (R1 range) | — | — | R(q) via R1 | — | R1 rows feed C8 |
| `$$relation-target-descriptors` — bounded ≤2×registry (now 20) per target-key, non-subindexed, `hash(target-key)` | — | — | — | bound grows to 20 (**W** deltas) | — | **R** (R1 gate) | — | — | R(q) via R1 | — | — |
| **`$$relation-activity-by-bucket`** (NEW) — keyed bucket→order-key, subindexed inner map, on `hash(bucket)` | — | — | — | — | **W** accepted branch only | — | — | **R** (R3 range) | — | R(q) via R3 | — |

### 3b. object-container-module PStates (mirror-read ONLY; never written)

| PState (partition scope) | Read by | Purpose |
|---|---|---|
| `$$containers-by-id` — `hash(object-key)` | B1 (mirror local-select) | L0 identity / L2 kind, container display-name |
| `$$revisions-by-id` — `hash(object-key)` | B1; C7 via `read-current-revision` | L1 current text / spec-doc `:latest` resolution |
| `$$source-artifacts-by-id` — `hash(object-key)` | B1; R(q) inside `read-common-material-for-source` | L1 raw (`:stored?`, byte-count) |
| `$$source-anchors-by-target` — subindexed, `hash(object-key)` | B1 | L1 anchors + evidence-anchor resolution (gate 5) |
| `$$composition-children-by-parent` — subindexed, `hash(object-key)` | B1 | L2 children (capped) |
| `$$composition-parent-by-child` — subindexed, `hash(object-key)` | B1 | L2 parent edge |
| `$$outline-by-document` — subindexed, `hash(object-key)` | B1 | L2 order-semantics / block-path order |
| `$$transcript-conversation-projection` — subindexed, `hash(object-key)` | B1, B3 | conversation material / trail page |
| `$$derived-units-by-id` — `hash(object-key)` | B1 | block/unit L1 text |
| `$$unit-graduations-by-id` — `hash(object-key)` | B1 | graduated/derived current text |
| `$$source-latest-by-ref` — `{String SourceVersionRow}`, `hash(source-ref-key)` | B2 (`:source-ingested`, window-filtered by `created-at-ms`) | doc-ingest feed entries |
| `$$source-ingest-completions-by-ref` — subindexed, `hash(source-ref-key)` | B2 (`:source-ingested`, filtered by `completed-at-ms`) | doc-ingest completion feed entries |
| `$$transcript-source-lines-by-file` — keyed file-key→order-key, subindexed | B2 (tail row → `source-id` → conversation address) | resolve `:transcript-file-updated` target address |
| `$$transcript-last-message-by-conversation` — `{String TranscriptLastMessageRow}`, `hash(object-key)` | B3 | trail cursor/last-message |
| `$$source-containers-by-source` / `$$source-derived-units-by-source` / `$$source-anchors-by-source` / `$$source-edges-by-source` — subindexed, `hash(object-key)` | R(q) inside `read-common-material-for-source` ONLY | L1 common-material bundle (colocated in OC; NOT directly mirrored) |

### 3c. object-container-transcript-ops-module PStates (mirror-read ONLY)

| PState (partition scope) | Read by | Purpose |
|---|---|---|
| `$$transcript-file-offsets` — `{String TranscriptFileOffsetRow}`, keyed file-key, default partitioner (cross-partition scan) | B2 (`:transcript-file-updated`, filtered by `updated-at-ms`) | one row per watched file → file-updated feed entries |
| `$$transcript-runs` — `{String TranscriptRunRow}`, keyed request-id, default partitioner | B2 (listed in §7 "Internally reads") | run-status enrichment for the file-updated branch |

### 3d. Query topologies consumed (the seam surface)

| Query topology | Owning module | Consumed by | New? |
|---|---|---|---|
| `relations-for-targets` (R1) | relation-kernel | B1 context-bundle | existing |
| `relation-detail` (R2) | relation-kernel | C4 read-relation-detail, B1 (`rel:*` targets), gate 7 history | existing |
| `relation-activity` (R3) | relation-kernel | B2 recent-activity | **NEW (A8)** |
| `read-common-material-for-source` | object-container | B1 context-bundle | existing (`object_container.clj:2494`) |
| `read-current-revision` | object-container | B1, C7 trail/via `:latest` | existing (`:2478`) |
| `read-source-by-ref-version` | object-container | C7 trail/via pin | existing (`:2450`) |

---

## 4. Edge cases (derived from the contract)

E-1. **Missing `:request/sent-at-ms`** → arrival-at-ms falls back to payload
`:asserted-at-ms` (§5.3). Consequence: a back-dated relation with NO envelope
sent-at is bucketed by CLAIMED time (its arrival bucket = its claimed day), so it
does NOT appear in an `:arrival` window at "now." Gate 4's fixture supplies
sent-at=now explicitly to exercise the divergence. (Bucket lives on `hash(bucket)`.)

E-2. **Both clocks absent** → `ts` defaults to 0 in `relation-outcome`
(`(long (or (:asserted-at-ms payload) 0))`, `relation_kernel.clj:336`); arrival
falls back to the same → bucket 0 (epoch-day) on `hash(bucket:0…)`. Contract is
silent on rejecting timeless requests → recorded as silence, not filled.

E-3. **Envelope actor == payload asserter** → `envelope-actor-*` still persisted on
the rows (rel-id task), but the bundle/text projection OMITS `:written-by` (§5.1;
gate 10).

E-4. **Same-key convergent custody.** Two agents writing Sid's assertion with the
SAME idempotency-key on `hash(relation-id)`: the journal gate dedups the second
(replays the first decision, writes nothing), so only the FIRST agent's
`envelope-actor-*` is recorded. Distinct keys → the second is a re-assert bumping
`status-changed-at-ms` with the second agent's custody on the new event + edge row.
(Relevant to gate 10 + relation-scoped journal.)

E-5. **Unknown / unresolved target id** → returned under `:bundle/omissions` with
reason `:target/unrecognized`, never silently dropped (§4). Git-commit shas render
`:unresolved` until the WP2 spine lands (dangling is a view state).

E-6. **`:none` unary targets in family grouping.** A unary dead-end's `to` is
`{:target-kind :none :target-id nil :target-key (from's key)}`; R1 dedups it to one
row on `hash(from.target-key)`. It lands in `:this` (its from-side is the requested
id); the bundle must NOT create an `:in-family` entry keyed by the nil `:none` id.

E-7. **Family row between two siblings** (neither endpoint is the requested id, both
share the requested object-key) → the contract says `:in-family` keyed by "that id"
(singular). For a row whose from AND to are distinct siblings, WHICH id keys it is
unspecified → recorded as a silence/finding (F-6).

E-8. **Retracted stances + `include-retracted?`.** Retracted stance rows drop from
`current-verdicts` `:current`; R1 already gates them by `relation-visible?` unless
`include-retracted?` is true; gate 7 requires the retracted row visible WITH
`include-retracted?` and history reachable via the R2 address. (Reads on
`hash(target-key)` for R1, `hash(relation-id)` for R2.)

E-9. **`:supersedes` in the verdict fold.** A `:supersedes` row's judged target is
its `to` (displaced) endpoint; it folds like the other two kinds into
`current-verdicts`. No gate exercises `:supersedes` in the fold directly → literal
reading applied, flagged (F-4).

E-10. **Char-vs-byte anchor resolution.** Gate 5 resolves ONE md anchor (char-unit)
and ONE transcript anchor (byte-unit) to the correct span text; the bundle's
`:offset-unit` must be carried from the anchor's source kind, not assumed. Anchors
live in `$$source-anchors-by-target` on `hash(object-key)`.

E-11. **Empty targets** → `context-bundle` with `[]` returns `{}`-shaped
`:bundle/targets` with zero PState reads (mirrors R1's empty short-circuit,
`distinct-present-target-keys` → `[]`). C-level `read-relations-for-targets`
already short-circuits empty lists.

E-12. **Caps below fixture size** → forces `:omissions` entries whose counts +
returned == fixture totals (gate 3); interacts with E-13 (text budget).

E-13. **Text projection budget vs caps.** `render-bundle-text` must stay ≤ 4,000
chars on the fixture (gate 13); a bundle whose caps admit large children/relations
could exceed the budget — the fixture is sized to fit, but the interaction of caps
and budget is the implementer's to hold.

E-14. **Pinned `:rev` resolution.** `:latest` → `read-current-revision` (current
pointer); a pin → `read-source-by-ref-version` (immutable source version). A pinned
address must keep resolving to the OLD params after the spec doc is revised (gate
12). See F-3 for the container-id → source-ref shape gap.

E-15. **Back-dated claimed vs arrival windows for the `:claimed` clock** — see F-1
(this is the load-bearing contradiction, not a benign edge).

E-16. **Blank routing key** at the kernel: the ONLY silent drop (topology
`filter> (present-string? *relation-id)` on the relation-id task); every other
malformation is a durable rejected decision. Activity write never reached for a
dropped request. (Existing behavior; unchanged by A7.)

---

## 5. Phase-0 findings (classified: implementer-fixable vs policy fork)

Classification rule (work-package stop clause): **implementer-fixable** = the
contract already answers it elsewhere (cite where); **policy fork** = two readings
that cannot both hold physically — described with verbatim citations, NOT resolved.

### F-1 — `:claimed`-clock feed vs arrival-only activity buckets — POLICY FORK

The activity PState is bucketed by ARRIVAL time only: `$$relation-activity-by-bucket`
bucket = `(quot arrival-at-ms 86400000)` (§5.3), and R3's signature is
`[bucket-lo bucket-hi]` — a bounded ARRIVAL-bucket range on `hash(bucket)`. But the
feed offers a `:claimed` clock (§6) and gate 4 requires a back-dated relation
(claimed = 60 days ago, sent-at = now, so it physically lives in TODAY's arrival
bucket) to "does appear in the 60-days-ago `:claimed` window."

- **Reading A (prune buckets by the window)** — §6 branch 1 verbatim: *":relation-transition — R3 over the buckets covering the window."* + §6 read-plan verbatim: *"cost is O(#watched-files + #doc-refs + window-activity) … single roundtrip."* Reading the 60-days-ago ARRIVAL buckets to serve a 60-days-ago `:claimed` window MISSES the back-dated entry (which lives in today's arrival bucket) → **gate 4 fails.**
- **Reading B (scan broadly, filter by claimed)** — gate 4 verbatim: *"the back-dated relation appears in today's `:arrival` window and NOT in today's `:claimed` window (and does appear in the 60-days-ago `:claimed` window)."* To satisfy this, R3 must read the arrival bucket where the row lives (today) — i.e. a `:claimed` window CANNOT be arrival-bucket-pruned; it must scan an unbounded arrival range and filter by `claimed-at-ms` → **violates the O(window-activity) single-roundtrip promise for `:claimed`.**

These cannot both hold: arrival-only buckets + O(window-activity) for `:claimed` +
gate-4's back-dated-in-old-claimed-window are three constraints of which any two
hold at once, never all three. At phase-1 scale (tens/day) a whole-range scan is
cheap, so the gate is *passable* — but the §6 performance promise as written does
not cover the `:claimed` clock, and the contract never states the arrival-bucket
range for a `:claimed` window. **Not resolved here.** (Recommend the plan phase
refuse to certify §6's `:claimed` read plan until the bucket range for the claimed
clock is pinned.)

### F-2 — "kernel PState" scope in the §7 style gate — IMPLEMENTER-FIXABLE

The §7 header style gate reads *"no `foreign-select` on any kernel PState outside
this module"* and gate 14/§11 *"no `foreign-select` on kernel PStates outside the
trail-view module and V1 test readers."* Taken literally against ALL kernel PStates
(OC + relation), the §7 table would violate itself, since it shows `context-bundle`
reading OC PStates (`$$containers-by-id`, …) directly as mirrors. Resolution is in
the contract: the §7 table itself lists OC PStates as legitimate direct mirror
reads, so "kernel PState" must scope to **relation-kernel** PStates (read only via
R1/R2/R3); OC/ops PStates are read as mirror `local-select>` INSIDE trail-view's own
query topologies, and no EXTERNAL consumer foreign-selects any PState (all use the
C-wrappers). Cite: §7 table + §2 "faces and agents consume ONLY the client wrappers."

### F-3 — pinned-`:rev` view-spec resolution shape gap — IMPLEMENTER-FIXABLE (with residue)

§3 addresses specs as `:spec "oc:doc:<object-key>"` (a container id) with `:rev
:latest|<pin>`, and names `read-source-by-ref-version`/`read-current-revision` as
the resolution path. `read-current-revision` (`object_container.clj:2478`) returns
only the CURRENT revision of a container-id; `read-source-by-ref-version`
(`:2450`) needs a `source-ref` + `source-version-key`, NOT a container id or a
container revision-id. There is no exposed query topology that reads an arbitrary
CONTAINER revision by revision-id, and `object-container-module` is
allowlist-frozen (§9.9, §12). The contract answers the intent (§3 names
`read-source-by-ref-version` FIRST for pins, and source versions are immutable), so
the buildable reading is: **a pin resolves via `read-source-by-ref-version` on the
spec doc's source-ref (obtained from the container's `source-id` → source-ref join),
`:latest` via `read-current-revision`.** Residue the implementer must handle: the
container-id → source-ref mapping for a spec addressed as `oc:doc:<k>`, and the fact
that a pin is a source-version-key, not a container revision-id. Not a fork (no
physical impossibility), but the address grammar's `:rev` vocabulary is looser than
the query surface it maps onto.

### F-4 — `:supersedes` participation in `current-verdicts` — IMPLEMENTER-FIXABLE (ambiguity noted)

§5.2 defines the fold as *"per judged target, per asserter, the latest asserted
stance-kind row"* — all three stance kinds uniformly. `:supersedes` (from =
replacement, to = displaced) would therefore fold as a current verdict keyed on its
`to` (displaced) endpoint. No gate exercises `:supersedes` in the fold (gate 7 tests
confirm→refute). Literal reading = uniform treatment; the ambiguity is only whether
belief-displacement belongs in the same per-asserter "latest" slot as confirm/refute.
Answered by the contract's own fold definition (uniform) → implementer-fixable, but
flagged so the plan phase traces a `:supersedes` scenario.

### F-5 — verdict-fold ordering key imprecision — IMPLEMENTER-FIXABLE (ambiguity noted)

§5.2 gives the fold order as *"claimed `first-asserted-at-ms`/`status-changed-at-ms`,
tiebreak `relation-id`"* — two timestamp fields joined by a slash with no rule for
which dominates. For "latest ASSERTED stance," the transition time
(`status-changed-at-ms`, bumped on re-assert per `relation_kernel.clj:281`) is the
semantically correct ordering key; `first-asserted-at-ms` is stable and reads as the
near-tiebreak. Both fields are on every row, so no physical conflict → implementer
picks `status-changed-at-ms` as the "latest" key; flagged because the slash is
imprecise.

### F-6 — `:in-family` key for a sibling-to-sibling row — IMPLEMENTER-FIXABLE (silence recorded)

§4 says sibling rows *"land in `:in-family` keyed by that id"* (singular). A family
row whose from AND to are two DISTINCT siblings (neither the requested id) has two
candidate keys; the contract is silent on which. The requested-id attribution rule
(*"rows on the requested id land in `:this`"*) fully covers rows that touch the
requested id, so this only bites for entirely-sibling rows. Recorded as a silence
(do not fill); the natural reading is to key by the from-side sibling and/or list
under both, but the contract does not say — implementer should pick and comment,
plan phase should trace it.

### F-7 — leading-partitioner optimization unavailable for mirror-PState reads — NON-BLOCKING (platform note)

`13-query-topologies.md:196` (verified): a leading partitioner *"Cannot be a state
partitioner to a mirror PState."* `context-bundle`/`conversation-trail` start by
partitioning to `hash(object-key)` on MIRROR PStates, so they cannot pre-route
client-side; they start at a random task and hop. This is a latency note only; §7
states *"Wall-clock latency is not a gate; shape and honesty are."* No action beyond
awareness; the named query→mirror-query fallback (§7) is the other half of the same
platform verification duty.

### F-8 — query→mirror-query support is a verification duty, not a fork — NON-BLOCKING

Whether a trail-view query topology may invoke a mirror query topology (R1/R2/R3,
`read-common-material-for-source`) is confirmed-plausible but not explicitly
exemplified in the on-disk refs: `18-module-dependencies.md:68` shows
`invokeQuery("*mirrorQuery", …)` from a STREAM topology, and
`13-query-topologies.md:203-207` (verified) documents COLOCATED query invocation;
`13:245` says query topologies can be called *"from topologies in other modules"*
generally. §7 pre-specifies the FALLBACK (client-wrapper composition, 2 roundtrips,
unchanged signatures) so either outcome ships. Implementer resolves via a smoke test
during the Phase-B platform-verification duty. Not escalated.

### Code-citation verification (every file:line claim the CONTRACT makes)

| Contract claim | Location | Verdict |
|---|---|---|
| `object_container.clj` is 2,648 lines (§2 "2,648 hardened lines") | `wc -l` = 2648 | ✅ exact |
| OC decision rows stamp wall clock `decided-at-ms` (§5.3 trap 4b: `object_container.clj:428,448,467`) | lines 428, 448, 467 all `(core/now-ms)`; 467 is literally `:decided-at-ms (core/now-ms)` | ✅ all three |
| md anchors are CHAR offsets, "`markdown_adapter.clj` uses `count`" (§4, trap 12) | `markdown_adapter.clj:273` `(count raw-text)`; block offsets from `markdown-source-lines`, no byte usage | ✅ |
| transcript anchors are BYTE offsets (§4, trap 12) | `transcript_adapter.clj:142-152` `:source/byte-offset`+`:source/byte-length`, end = `(+ offset byte-length)` | ✅ |
| kernel persists only payload `asserter-actor-id`; envelope `:actor` checked for retraction rights and dropped (§5.1) | `relation_kernel.clj:355-356` retraction check uses envelope `actor-id`; edge/event/decision rows store payload `asserter-actor-id` only; envelope actor NOT persisted | ✅ (this is what custody amendment fixes) |
| stance kinds = "the designed-for one-line change" (§5.2) | `relation_kernel.clj:46-47` `relation-kinds` one-line set | ✅ |
| kernel bumps `status-changed-at-ms` on re-assert (§5.2) | `relation_kernel.clj:281` `transition-row` `:status-changed-at-ms ts` | ✅ |
| all §7 mirror PState names exist | verified against `object_container.clj:1685-1770` (main stream) + `:2547-2560` (ops module) | ✅ all 16 |
| `read-common-material-for-source` / `read-current-revision` / `read-source-by-ref-version` exist | `object_container.clj:2494` / `:2478` / `:2450` | ✅ (see F-3 on `:rev` mapping) |
| R1/R2 exist | `relation_kernel.clj:628` (`relations-for-targets`), `:662` (`relation-detail`) | ✅ |
| `13-query-topologies.md:203-207` documents colocated invocation | lines 203-207 = "Invoking colocated query topologies" heading + intro | ✅ |
| `13:196` = leading-partitioner restriction for mirror state partitioners | line 196 = "Cannot be a state partitioner to a mirror PState" | ✅ |

**No contract code-citation failed verification.**

---

## 6. Counts (receipt)

- Operations inventory: **20** (1a: 8 relation-kernel amendments A1–A8; 1b: 3 query
  topologies B1–B3; 1c: 9 wrappers/pure fns C1–C9).
- Invariants: **23** (I-1 … I-23).
- Entity × write matrix rows: **26** (8 relation-kernel PStates incl. the new
  activity PState; 15 OC-module PStates incl. the 4 common-material sub-PStates read
  only via mirror-query; 2 ops-module PStates; 1 relation-kernel `-by-id` history
  row) + a 6-row query-topology consumption sub-table.
- Edge cases: **16** (E-1 … E-16).
- Phase-0 findings: **8** (F-1 policy fork; F-2/F-3/F-4/F-5/F-6 implementer-fixable;
  F-7/F-8 non-blocking platform notes) + a code-citation verification table (0
  failures).

---

## 7. Post-Phase-0 resolutions (Fable, contract author, 2026-07-04 same day — CONTRACT v1.1)

All findings adjudicated before any plan/code tokens. The CONTRACT is
authoritative; this section exists so later phases do not re-flag resolved
items.

- **F-1 RULED (the policy fork): arrival-only window selection.** The feed's
  window is ALWAYS arrival-time; the former `:clock` param is now `:order`
  (in-window ordering by either stamp); cross-land claimed-window selection
  is refused with a pre-named promotion path (claimed-keyed twin bucket
  index). Grounds: the feed serves daily relief (I-13, arrival by nature);
  no consumer demands claimed windows; D-001 forbids the second index ahead
  of a real face. Sweep: CONTRACT §3 example, §6, gate 4 (rewritten), trap 4,
  new §9.10. The three-constraint conflict dissolves: gate 4 no longer asks
  for claimed-window retrieval, so arrival buckets + O(window-activity) hold.
- **F-2 fixed:** §11 style gate now scopes PState access precisely
  (relation-kernel PStates: nothing outside V1 test readers, R1/R2/R3 the
  only reads anywhere; OC/ops PStates: only via the trail-view module's
  mirrors).
- **F-3 fixed:** §3 `:rev` = a SOURCE-VERSION key resolved via
  `read-source-by-ref-version` on the spec doc's source-ref (joined from the
  container's `source-id`); never a container revision-id; OC untouched.
- **F-4 fixed:** §5.2 states all three stance kinds fold uniformly, keyed on
  the judged (`to`) endpoint; gate 7 now exercises `supersedes` in the fold.
- **F-5 fixed:** fold dominance = max `status-changed-at-ms`, tiebreak
  `relation-id`.
- **F-6 fixed:** §4 — an entirely-sibling family row files ONCE under
  `:in-family`, keyed by its `from` endpoint id.
- **F-7 / F-8 stand as written** (latency not a gate; query→mirror-query is
  a Phase-B verification duty with the client-composition fallback
  pre-specified — the plan phase should settle it with a minimal scratchpad
  spike rather than carry the uncertainty into Phase B).

Incident note (byte hygiene): CONTRACT v1's §11 originally contained a RAW
NUL byte (the tool-JSON decode trap on the backslash-u0000 escape — fourth
firing in this repo, again while writing about the escape itself). Caught at
Phase-0 close via file(1) reporting "data"; repaired with the standard perl
re-spell; the file now verifies as UTF-8 text. Standing gate for every
doc/code write in this package: file(1) must say text.
