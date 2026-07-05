# RelationEdge Contract — relation-kernel-module

Status: DRAFT v1 (Fable, 2026-07-03). Implements decision D-004; serves D-002
(trail view) and D-005 (view-first sequencing). Pending: Codex falsification
round (per D-006 evaluation), then handoff as a work package.

---

## 1. Purpose and scope

A **RelationEdge** is a typed, provenance-carrying assertion that two addressable
things stand in a named relation: `A based-on B`, `chat produced commit`,
`panel-2 elaborates panel-1`, `branch X dead-end`. It is the epistemic edge the
wall is made of and the substrate lacks (D-004).

Consumers, in order: the trail view (27-04 panel: timeline joins, product-DAG,
dead-ends); agent context bundles (View 3); later the DG protocol.

Non-goals for this contract: confidence/credential algebra, auto-merge of
assertions, relation-as-container (commenting on relations), semantic search.
All are extension points, listed in §9.

## 2. Ruling: where relations live

**A new module: `relation-kernel-module`** (own namespace, own depot, one
microbatch topology, one query topology). Not inside object-container-module,
not inside space-kernel-module.

Why not space-kernel (Sid's named "connector" home): the space kernel as built
is a chat-workspace kernel (turns, bundles, llm-runs). Its `$$artifact-graph` /
`$$artifact-graph-in` pair is **derived workflow adjacency** — untyped
`{String Object}` rows written by the topology as side effects of space actions
(`space.clj:1655-1674`). Epistemic relations are a different kind of truth:
first-class *asserted facts* with identity, lifecycle, and provenance. Mixing
them into the plumbing graph conflates system bookkeeping with human/LLM
judgment — exactly what `asserted-by` exists to keep apart. The relation kernel
IS the connector role Sid assigned to the space kernel, implemented as its own
module; the plumbing graph stays where it is and can later be *projected into*
typed relations (`asserted-by :system`) if the view wants one graph.

Why not object-container-module: its stream topology and partition discipline
are document-scoped (everything about one doc/conversation colocates via
`extract-object-key`). Relations cross documents by nature, and the module is
2,648 lines of hardened import path — blast radius for a cheaper implementing
model is the dominant risk. Relations touch nothing existing; they only need
`extract-object-key` / `fixed-width-order-key` / `actor-row`, which are plain
fns importable from `app.server.rama.object-container`.

Reversal cost if this ruling is wrong: PState data migration across modules is
manual and disruptive. Mitigation: all access goes through the two query
topologies (§7) — consumers never hard-code PState paths — so a future merge
into space-kernel changes the module name behind a mirror, not consumers.

## 3. Data model

```clojure
(defrecord RelationTargetRef
  [target-kind    ; keyword, open: :container :source :git-commit :doc-file
                  ;   :conversation :none (unary marks) — new kinds allowed
   target-id      ; string address, e.g. "oc:doc:notes.md", commit sha
   target-key])   ; partition key, derived — see §4

(defrecord RelationEdgeRow
  [relation-id          ; deterministic — see identity rule
   relation-kind        ; keyword from the kind registry
   from                 ; RelationTargetRef
   to                   ; RelationTargetRef
   asserter-actor-id    ; e.g. "sid", "llm:opus-4-8/run-abc", "import:transcript"
   asserter-type        ; :human | :llm | :import | :system
   relation-status      ; :asserted | :retracted
   evidence-source-id   ; nullable — SourceArtifact that evidences this relation
   evidence-anchor-id   ; nullable — SourceAnchor (byte range) of the evidence
   note                 ; nullable, short free text
   first-asserted-at-ms
   status-changed-at-ms
   event-id request-id])
```

**Identity rule.** `relation-id = "rel:" + sha1(relation-kind, from.kind,
from.id, to.kind, to.id, asserter-actor-id)`. Identity includes the asserter:
Sid asserting `based-on` and an LLM proposing the same `based-on` are two
relations (two facts about the world). The view groups by `(kind, from, to)`
and shows asserter badges; it never merges storage. Re-assertion by the same
asserter is a status event on the same identity, not a new row.

**Kind registry.** A code-level set, starter contents from D-004:
`#{:based-on :produced :built-over :new-direction :dead-end :elaborates
:references}`. Requests with unregistered kinds are **rejected** at decision
time. Adding a kind is a one-line reviewed code change. The registry is the
guard against LLM-glue kind mush.

**Unary marks** (dead-end on a node with no counterpart): `to = {:target-kind
:none :target-id nil :target-key (:target-key from)}`. The reverse-index write
then lands on the from task — no extra hop, no partition spray of nil keys.

**Directionality.** Edges are directed as asserted. `based-on`: A based-on B
means A's `from`. `produced`: producer → produced. The view may render either
direction; storage never flips.

## 4. Depot and partitioning

```clojure
(declare-depot setup *relation-request-depot (hash-by :relation/routing-key))
;; routing-key = relation-id  →  request, journal, decision, and the
;; authoritative $$relations-by-id row all colocate on one task.
```

Request envelope: `{:relation/routing-key, :request/id, :request/type
(:relation/assert | :relation/retract), :idempotency/key, :actor, :payload
(the proto-row fields)}`. **The client computes `relation-id` before append**
(deterministic hash — same convention as Electric minting run-id pre-append).

`target-key` derivation, per target-kind:
- `:container`, `:source`, `:doc-file`, `:conversation` → `extract-object-key`
  of target-id (colocates with the object-container material it describes —
  cheap future joins on the same task are possible via mirrors).
- `:git-commit` → the sha itself.
- `:none` → copied from `from.target-key` (§3).
- unknown kinds → target-id verbatim. Never throw on unknown kinds: relations
  outlive today's kind list.

## 5. Write topology — microbatch, exactly-once

One **microbatch** topology. Rationale: a relation write must land in three
places on up to three different tasks (§6); microbatch gives cross-partition
exactly-once atomicity per batch, so the dual-index write is safe by
construction rather than idempotent-by-convention. Relations are not
latency-critical (the trail view reads seconds-old truth happily); stream's
latency advantage buys nothing here.

Fold, per request:

1. `%microbatch` source → requests emit on their `:relation/routing-key` task.
2. **Journal gate** (colocated): `$$relation-decisions-by-idempotency` lookup;
   duplicate idempotency-key → re-emit prior decision, stop. (Spine copied
   from object-container's decisions-by-idempotency pattern.)
   **Scope ruling (2026-07-03, PLAN_VALIDATION F2):** idempotency keys are
   **relation-scoped** — the journal lives on the relation's task; effective
   uniqueness is per (relation-id, idempotency-key). Global uniqueness across
   relations is explicitly NOT provided: the key is a client retry token, not
   an operation identity (that is `relation-id`); collapsing same-key requests
   across different relations would silently drop an assertion (exactness
   rule). Matches object-container's partition-scoped journal precedent.
3. Validate: kind in registry; targets well-formed; retract only by the
   original asserter (`asserter-actor-id` match) — else rejected decision row.
   **No existence check on targets** (§8, trap 3).
4. Fold status: for `:relation/assert` on existing id → status `:asserted`,
   bump `status-changed-at-ms` (idempotent if already asserted); for new id →
   full row. For `:relation/retract` → status `:retracted`. Write decision +
   event rows, and authoritative row to `$$relations-by-id` (this task).
5. `(|hash (:target-key from))` → upsert full row copy into
   `$$relations-by-target` under from-side sort key.
6. `(|hash (:target-key to))` → upsert full row copy under to-side sort key.

Steps 4–6 always write the **entire current row** (`termval`), never
read-modify-write on the index copies — a status change rewrites all three
copies in the same batch, so the copies cannot disagree across batch
boundaries (trap 8).

Retry/duplicate safety, three layers: microbatch exactly-once (mid-batch crash
replays the batch, PState writes are transactional per batch); idempotency-key
journal (client retries of `foreign-append!`); deterministic relation-id
(cross-session duplicate submissions — e.g. re-running a transcript import —
converge on the same identity instead of accreting rows).

## 6. PStates

```clojure
$$relation-decisions-by-idempotency {String Object}          ; journal gate
$$relation-decisions-by-id          {String Object}          ; audit
$$relation-events-by-id             {String Object}          ; audit
$$relations-by-id                   {String RelationEdgeRow} ; authoritative
$$relation-status-log-by-relation                            ; history, subindexed
  {String (map-schema String Object {:subindex? true})}      ; rel-id → order-key → event-ref
$$relations-by-target                                        ; THE read shape
  {String (map-schema String RelationEdgeRow {:subindex? true})}
  ;; target-key → sort-key → full row copy
  ;; sort-key = direction ("o"|"i") + relation-kind + fixed-width-order-key(first-asserted-at-ms) + relation-id
```

Denormalization rationale (skill rule: write-path work is amortized, read-path
seeks are per-query): full row copies in `$$relations-by-target` mean the trail
view's dominant query — "all relations touching X" — is **1 seek + sequential
iteration**. Pointer-only indexes would cost 1 seek per edge (30 edges ≈ 15ms
vs ≈ 0.7ms). Cost: 3 copies per relation, updated together (§5).

The composite sort-key makes kind-filtered and direction-filtered reads range
scans within the seek, and gives stable ordering for pagination.

## 7. Query topologies (the only public read surface)

```clojure
;; relations-for-targets: [target-keys kinds-filter include-retracted?] →
;;   {target-key → [RelationEdgeRow ...]}
;;   Fans |hash per target-key, local-select with {:allow-yield? true} over the
;;   subindexed range (narrowed by sort-key prefix when kinds-filter has one
;;   kind), filters status, |origin aggregates into one map.
;;   Batch-first signature: the trail view resolves a whole visible DAG in one
;;   roundtrip, not one query per node.

;; relation-detail: [relation-id] →
;;   {:row RelationEdgeRow :history [status events...]}
;;   |hash relation-id, point read + one subindexed range read.
```

Consumers (Electric server, trail-view projections, agents) use these two —
never foreign-select on the PStates directly. This is the seam that keeps the
§2 reversal cost low and lets read shapes evolve.

## 8. Provenance and policy

- `asserter-type :llm` rows are proposals. Human endorsement of an LLM proposal
  is **Sid asserting his own relation** (a new identity, §3) — no special
  endorsement machinery; the view stacks the badges. The map must not lie:
  machine glue and human judgment are never storage-merged.
- Evidence: an imported relation (e.g. `produced` extracted from a transcript
  tool call) SHOULD carry `evidence-source-id` + `evidence-anchor-id` pointing
  at the transcript line that proves it — reusing SourceAnchor machinery, so
  "why does this edge exist?" has a byte-accurate answer.
- Retraction rights: only the original asserter (matched on
  `asserter-actor-id`). Retraction is a status, not a deletion — history stays
  readable (skill rule: never delete).
- Dangling targets are legal and permanent-until-resolved: a relation may point
  at a commit whose metadata isn't imported yet. The **view** renders
  unresolved endpoints as explicitly unresolved (exactness rule). No repair
  daemon in MVP.

## 9. What this contract explicitly refuses (extension points)

1. No foreign-key validation of targets (see trap 3).
2. No confidence scores / credential algebra — field can be added to the row
   later; queued open question in decisions.md.
3. No relation-as-container (discussable relations). If DG work later needs
   it, a relation can *graduate* the way units do — the deterministic
   relation-id is a stable future container-id; nothing here blocks that.
4. No auto-merge of same-(kind,from,to) assertions across asserters.
5. No cascading anything on retraction.
6. No deletion, ever.

## 10. Traps ledger — naive choice → failure → ruling

Recorded per D-006 evaluation criterion 1.

| # | Naive choice | Concrete failure | Ruling |
|---|---|---|---|
| 0 | Design a relation graph from scratch without reading the space kernel | Duplicate of the existing `$$artifact-graph` machinery; or worse, epistemic edges written into the untyped plumbing graph, destroying the asserted/derived distinction | §2: new module; plumbing graph acknowledged and left in place |
| 1 | Copy the existing dual-write patterns (composition pair, artifact-graph) into a **stream** topology | Composition's dual index only colocates because parent and child share a document key; relations cross keys. Stream = at-least-once: a crash between forward and reverse hop replays the chain — safe only if every downstream write stays overwrite-idempotent forever; one read-modify-write added later corrupts silently | §5: microbatch, exactly-once cross-partition |
| 2 | Mint relation-id as a UUID | Re-running a transcript import re-asserts every extracted relation as a new row; graph accretes duplicates unboundedly; dedup then requires a content index that IS the deterministic id | §3: content-derived identity incl. asserter |
| 3 | Validate that both endpoints exist before accepting | Import-order dependency hell: transcript joins reference commits not yet imported → either rejected edges (data loss) or a retry queue (new machinery); plus cross-module existence reads race their own truth | §8: relations are assertions about addresses (D-003 "address, don't copy"); dangling is a view state |
| 4 | Store each relation once, under `from` | "What points AT X?" (the trail view's backward walk: what was based-on this doc?) becomes a scan of every partition | §6: dual-indexed by both endpoints |
| 5 | One shared row merged across asserters (LLM re-assertion overwrites Sid's) | Provenance destroyed; the map lies about who claimed what; retraction rights unenforceable | §3: asserter inside identity; view-level grouping |
| 6 | Open kind namespace (accept any keyword) | LLM glue invents `:relates-to`, `:connected-with`, `:refers` variants; the trail view's kind filters silently miss edges; taxonomy repair becomes a data migration | §3: registry validation, rejected decisions |
| 7 | Status change updates authoritative row only (indexes hold pointers, or "we'll fix the copies lazily") | With pointers: 1 seek per edge on the hot read path (30× cost). With lazy copies: from-side says asserted, to-side says retracted — the same edge disagrees with itself depending on which node you ask from | §5/§6: full-row copies, all three rewritten in one exactly-once batch |
| 8 | `:none` targets hashed on nil / constant key | All unary marks (every dead-end on the wall) funnel to one task — the `|global` hot-spot anti-pattern | §3: `:none` inherits from-side key |

## 11. Acceptance gates (IPC tests the implementation MUST pass)

1. **Assert + dual read**: assert `A based-on B` where A and B have different
   partition keys; readable via `relations-for-targets` from BOTH A and B.
2. **Idempotent client retry**: same request appended twice (same
   idempotency-key) → one relation, one event, second decision replays first.
3. **Convergent re-import**: same logical assertion submitted twice with
   different idempotency-keys (fresh import run) → same relation-id, no
   duplicate rows in either index.
4. **Retract consistency**: retract → status `:retracted` visible from A-side,
   B-side, and by-id; history shows both events in order.
5. **Retraction rights**: retract by a different actor → rejected decision,
   status unchanged.
6. **Registry**: unregistered kind → rejected decision, zero PState writes
   outside the decision/audit spine.
7. **Dangling**: assert against a target-id that matches nothing → accepted;
   readable; (view-level resolution is out of scope here).
8. **Unary**: `dead-end` with `:none` counterpart → exactly one target-key's
   index touched; readable from the from-side.
9. **Asserter separation**: same (kind, from, to) from `sid` and from
   `llm:...` → two relations, two ids, both returned grouped by the query.
10. **Kind filter**: `relations-for-targets` with kinds-filter returns only
    matching kinds (verifies sort-key prefix discipline).
11. **Cross-relation key reuse** (added by F2 ruling): the same
    idempotency-key submitted for two different relations → both relations
    succeed independently (two ids, two journal entries, nothing collapses).

Implementation style gates: typed defrecords (object-container style, not
space-kernel loose maps); partition helpers imported, not reimplemented;
`{:allow-yield? true}` on unbounded range reads; no PState access from
consumers except via §7 queries.

## 12. Work-package handoff

- Implementer: Opus 4.8 or Codex, under the `/rama` skill's phased process
  (this contract is the Phase-0 input). New files:
  `src/app/server/rama/relation_kernel.clj` (+ test ns). No edits to existing
  kernels except requiring the partition-helper fns.
- Reviewer gate (Fable): §11 tests green + traps 1/2/7 spot-checked in the
  diff + Falsification Pass per CLAUDE.md review protocol.
- First real payload after green: the transcript→commit/doc join extractor
  (separate work package) emitting `:produced` / `:based-on` assertions with
  evidence anchors — feeding the 27-04 view.

## 13. Input manifest (for the D-006 counterfactual probe)

Given to any model reproducing this contract: `docs/current-mental-model/
decisions.md` (D-001..D-006); `object_container.clj` lines 38–171 (records),
262–350 (partition helpers), 1685–1770 (module/PState decls); `space.clj`
lines 1456–1490 and 1628–1680; the D-002/D-004 summaries of the 27-04 and
panel-2/2.5 wall demands; the `/rama` skill SKILL.md. Probe question: "design
the RelationEdge contract for this substrate."
