# (PROBE ARTIFACT — NOT BINDING) Counterfactual RelationEdge contract, blind Opus 4.8, 2026-07-05

> This is the D-006 criterion-2 probe output, verbatim (HTML entities from the
> transport de-escaped; nothing else touched). It was authored BLIND from the
> CONTRACT §13 input manifest by a fresh Opus 4.8 subagent that never saw the
> real contract, the kernel code, or post-contract decision text. Method +
> caveats + scoring: `COUNTERFACTUAL_PROBE.md` (same directory). Nothing in
> this file is binding; it exists to evaluate the D-006 bet.

---

# CONTRACT — RelationEdge Kernel (D-004)

**Status:** BINDING, ready for phased Rama implementation. **Implements:** D-004 (typed, asserted, provenance-carrying, non-compositional relation edge between containers). **Governed by:** D-001 (no speculative surface), D-003 (endpoints may be Regime-1 addresses, not just materialized containers), D-005 (the trail view is the first and only consumer that orders this work). **Input manifest (for the D-006 counterfactual probe):** `object_container` records L38–171 + partition helpers L262–350 + module PStates L1685–1770; `space` L1456–1490 + L1628–1680; `rama-SKILL.md`; `decisions-D001..D006`. No other source was read.

Grounds are stated inline. Where I reason from the excerpt idiom rather than from a compiled module, I mark it `[inferred idiom]`.

---

## 0. The one thing that makes this hard

A relation is intrinsically **two-object**: `A --produced--> B` where `A` and `B` have *different* object-keys and therefore live on *different* Rama tasks. Everything else in the object-container substrate is *single-object* — `extract-object-key` exists precisely to funnel every row about one object onto one task. A relation cannot colocate with "its object" because it has two. Every load-bearing decision below falls out of resolving that tension: **route and dedup by exactly one endpoint (the `from`), and pay a cross-partition write to maintain the reverse index — never a cross-partition read.**

---

## 1. PLACEMENT

**Decision: a new module `relation-kernel-module` (`src/app/server/rama/relation_kernel.clj`).** It owns its own action depot, its own request→decision→event triple, and its own edge PStates. It reuses the object-container *ontology and helpers* (`TargetRefRow`, `ActorRow`, `extract-object-key`, `partition-by-object-key`, `partition-by-audit-id`) but is a separate deployable module. It mirrors object-container PStates **only** in the optional read/enrichment topology (§7), never on the write path.

**Why not extend object-container (the sibling of `CompositionEdgeRow` lives there):**
- Object-container is the substrate's most-depended-on module and was hardened Jun 7–8. Relations are the opposite risk profile: **revisable judgments on an immutable log** (a `dead-end` flips — D-006). A Rama module is the *deployment/update unit*; every relation-schema churn or relation-topology bug would force a module-update of the entire ingestion substrate. Isolate the churn.
- Every object-container row is single-object-colocated. A relation violates that invariant by construction (§0). Housing a two-object write discipline inside a one-object module invites the next implementer to "just partition it by object-key like everything else" — which silently breaks the reverse index (Trap T5).

**Why not the space kernel — even though D-004 says "Home: the space kernel":** I read D-004's phrase *"the connector layer that makes relations among artifact types"* as naming a **conceptual role**, and I realize that role as a first-class module rather than as forms inside the dogfood *workflow* runtime. The space kernel's `$$artifact-graph`/`$$artifact-graph-in` connects space-native objects (turns, bundles, llm-runs) colocated inside one space; the trail-view relations (D-002/D-005) connect object-container containers (transcripts, docs) and Regime-1 commit addresses (D-003), which are foreign to the space kernel. Putting them there **inverts the dependency** — the trail view would depend on the dogfood runtime — and couples relation reads to that runtime's release cadence. D-006 names three future consumers (trail view, space kernel, DG protocol); a standalone module is the noun all three can mirror, which is exactly what D-006 asks for.

**This is the single place where the implementer must confirm intent with Sid before Phase 0** — D-004 literally says "space kernel." I am overriding the *file*, not the *role*, and I owe that flag.

**Reversal cost:** LOW before the first production relation write, MODERATE after. §2–§6 (schema, identity derivation, dual-index, dedup scope, read plan) are **placement-independent** — they don't reference which `defmodule` wraps them. Reversal = move the `declare-depot`/`declare-pstate`/topology forms into the target `defmodule`; if this happens before any relation is written, there is no data to migrate. After production writes, it is a PState-rename migration (`references/pstate-migration.md`). **Do the placement confirmation now so this stays a text edit.**

---

## 2. SCHEMA + IDENTITY

### 2.1 Records

```clojure
(defrecord RelationEdgeRow
  [relation-id relation-kind
   from-target-kind from-target-id from-target-address
   to-target-kind   to-target-id   to-target-address
   asserter-key asserted-by actor              ; provenance (D-004 first-class)
   assertion-status confidence note            ; MUTABLE, non-identity
   source-id source-anchor-id                  ; evidence anchor (nullable)
   relation-fingerprint proposition-key
   idempotency-key request-id audit-id event-id
   asserted-at-ms updated-at-ms
   retracted-by-event-id retracted-by-actor retracted-at-ms])
```

Endpoints are **flattened** (`from-*` / `to-*`), matching `CompositionEdgeRow`'s `parent-target-*`/`child-target-*` — local convention, and it keeps the dual-index keypaths (`from-target-id`, `to-target-id`) directly navigable without descending into a nested record. Addresses are carried (unlike `CompositionEdgeRow`) because D-003 relations bind to *addresses that resolve later* (commit sha + path).

`relation-kind ∈ {:based-on :produced :built-over :new-direction :dead-end :elaborates :references}` — the D-004 starter set, closed. Adding a kind is a topology-free data change but MUST be a reviewed amendment (validation rejects unknown kinds, §3).

`asserted-by ∈ {:sid :llm :import}` — the D-004 provenance class, **denormalized onto the edge** so the honest read ("is this human structure or LLM glue?") is a field read, not a join.

### 2.2 Identity — the crux

**Identity tuple = `[from-target-kind from-target-id to-target-kind to-target-id relation-kind asserter-key]`.** Directed (`from` before `to`). Content-addressed:

```
relation-fingerprint = sha256-hex( pr-str identity-tuple )        ; full
proposition-key      = sha256-hex( pr-str tuple-without-asserter ) ; grouping
relation-id          = "rel:" <from-object-key> ":" <kind-tag> ":" (subs relation-fingerprint 0 12)
```

- **Deterministic id ⇒ idempotency by construction.** The same assertion always computes the same `relation-id`. This is load-bearing for the cross-partition write (§4, T2), not a convenience.
- **`from-object-key` is embedded in the id** so `extract-object-key` routes `$$relations-by-id` onto the `from`-owner task with zero extra logic. **Substrate touch required:** add a `"rel:"` branch to `extract-object-key` returning `(leading-object-key (subs s 4))`. This is the only edit outside the new module. It is additive and colocated with the existing `ce:`/`du:`/`sa:` branches.
- **Identity is by `target-id`, NOT `target-address`.** A commit amend or path move changes the address but not *which two things are related*. Address in the identity forks identity → duplicate edges for "the same" relation (T14). Address is mutable resolution metadata.
- **`asserter-key` is in the identity.** Two actors asserting the same proposition are **two edges**, both stored, both visible. Collapsing them and overwriting `asserted-by` would erase whether Sid or only an LLM backs the claim — "the map must not lie" (D-004). `asserter-key` is a *stable* actor identity (`"sid"`; the importer id; the LLM **agent/model** identity — *not* the per-run id), so re-running the same ingest is idempotent while Sid vs. the agent stay distinct.

**Immutable (identity):** `from-*id/kind`, `to-*id/kind`, `relation-kind`, `asserter-key`, and everything derived from them (`relation-fingerprint`, `proposition-key`, `relation-id`).
**Mutable (non-identity):** `assertion-status`, `confidence`, `note`, `*-address` (last-known resolution), `updated-at-ms`, `event-id`/`request-id` (point at latest touching event), retraction fields.

### 2.3 `dead-end` / `new-direction` are directed binary edges

All seven kinds are 2-ary directed. `A --dead-end--> B` means "the direction A was reached-from/ended-at B"; `A --new-direction--> B` means "work pivoted from A to B." A **unary** "this node is abandoned" mark is **out of scope** (D-001: build it when a used form breaks against its absence — see T12 for why forcing it now is worse than deferring).

---

## 3. WRITE PATH

**Depot:** `(declare-depot setup *relation-action-depot (hash-by :routing/key))` — matches the space kernel idiom. **`routing-key` = the `from`-object-key** on every request, so the depot delivers each event *already on the from-owner task*.

**Envelope:**
```clojure
(defrecord RelationRequestRow
  [audit-id partition-key request-id request-type routing-key idempotency-key
   actor from-target to-target relation-kind          ; assert
   confidence note source-id source-anchor-id
   target-relation-id                                  ; retract / annotate operate BY ID
   requested-at-ms raw-request])
;; request-type ∈ {:assert-relation :retract-relation :annotate-relation}
```

**Topology:** stream `[inferred idiom — object-container declares a stream-topology]`, `{:retry-mode :all-after}`. Chosen over microbatch for latency parity with the substrate and idiom-match; the cross-partition hazard microbatch would remove for free is instead **neutralized by construction** (deterministic ids + idempotent `termval` writes, §4) and **pinned by Gate G5**. If a future form needs a reader that must *never* observe a half-written dual index, swap to microbatch — the write logic is idempotent either way, so it is a contained swap (that is the topology reversal story).

**Validation (structural only — no cross-module read):**
1. `relation-kind` ∈ the closed set, else decision `:rejected` (no event, no row).
2. `from-target`/`to-target` are well-formed `TargetRefRow`s with non-nil `target-id`; `from ≠ to` for kinds where a self-loop is meaningless (all seven), else `:rejected`.
3. **Endpoint existence is NOT validated.** See §6 and T4 — endpoints may be Regime-1 commit addresses that are not yet containers.

**Decision/event semantics:** exactly one of `{:asserted :retracted :annotated :deduped :replayed :no-op :rejected}` per request. `:asserted`/`:retracted`/`:annotated` mint a `RelationEventRow` (`event-type ∈ {:relation-asserted :relation-retracted :relation-annotated}`). `:deduped`/`:replayed`/`:no-op`/`:rejected` mint **no** event.

**Client API** (foreign-append helpers, object-container request→decision shape):
```clojure
(assert-relation!  cluster {:from {…} :to {…} :kind :produced
                            :asserted-by-actor {…} :idempotency-key "…"
                            :confidence 0.8 :note "…" :source-id … :source-anchor-id …})
;; → {:status :asserted|:deduped|:replayed|:rejected :relation-id "rel:…"}
(retract-relation! cluster {:relation-id "rel:…" :actor {…} :idempotency-key "…"})
(annotate-relation! cluster {:relation-id "rel:…" :actor {…} :note "…" :confidence …
                             :idempotency-key "…"})
```
Client appends with an ack level sufficient to read its decision back (append-ack + decision select, per the object-container pattern). Reads go through the query surface (§7), never through the client re-deriving ids.

---

## 4. IDEMPOTENCY / DEDUP — and its SCOPE

Two **distinct** dedup layers, both colocated on the from-owner task, both checked before any write, in this order:

**Layer 1 — idempotency-key (client retry / at-least-once depot delivery).**
`$$relation-idempotency : {String (map-schema String RelationDecisionRow {:subindex? true})}`, outer key = **from-object-key**, inner = `idempotency-key`. A hit ⇒ replay the stored decision verbatim (`status :replayed`), **no** new event. Answers "the same *request* twice."

**Layer 2 — fingerprint (two different requests asserting the same proposition-by-the-same-asserter).**
`$$relation-dedup-by-fingerprint : {String (map-schema String RelationDecisionRow {:subindex? true})}`, outer = **from-object-key**, inner = `relation-fingerprint` → the winning decision (carries `relation-id` + first-writer `request-id`). A hit by a *different* request ⇒ `status :deduped`, return the existing `relation-id`, **no duplicate row**; if the new request carries fresher mutable fields, apply them as an `:annotated` mutation (last-writer-wins on `note`/`confidence` by event-time — never on identity). Answers "the same *relation* asserted twice."

### The scope invariant (this is the whole point of question 4)

> **Because `routing-key = from-object-key ⊆ the identity tuple`, every assertion of a given `relation-fingerprint` is routed to exactly one task.** That task is single-threaded, so the check-and-claim of the fingerprint (Layer 2) and the primary+out-index write are atomic **and globally authoritative** — a *task-local* dedup index yields *global* uniqueness.

This holds **only** because routing is a deterministic function of the identity. Route by `request-id`, a UUID, or round-robin (T3) and the same fingerprint can be processed on two tasks; the task-local claim on task A cannot see the claim on task B; two identical edges are written and never reconciled. **The dedup PStates MUST be partitioned by `partition-by-object-key` over the from-object-key, colocated with `$$relations-by-id` and `$$relations-out-by-from`** — mirroring the space kernel's `$$send-by-idempotency` ("colocation with the deciding task makes check-and-claim atomic").

**Replay safety of the claim** (the D-006 microbatch/stream fine-print): a fingerprint claim stores the owning `request-id`. Replay of the **same** `request-id` (retry) re-confirms the claim as a no-op success; a **different** `request-id` on the same fingerprint is a dedup. The claim is never a hard "conflict" against its own replay.

---

## 5. PARTITIONING + READ PLAN

**PStates** (all `relation-kernel-module`):

| PState | Schema | Partitioner (outer key) | Role |
|---|---|---|---|
| `$$relations-by-id` | `{String RelationEdgeRow}` | `partition-by-object-key` (relation-id ⇒ from-ok) | point read |
| `$$relations-out-by-from` | `{String (map-schema String RelationEdgeRow {:subindex? true})}` | `partition-by-object-key` (from-target-id ⇒ from-ok) | outbound adjacency |
| `$$relations-in-by-to` | `{String (map-schema String RelationEdgeRow {:subindex? true})}` | `partition-by-object-key` (to-target-id ⇒ to-ok) | inbound adjacency |
| `$$relation-dedup-by-fingerprint` | `{String (map-schema String RelationDecisionRow {:subindex? true})}` | `partition-by-object-key` (from-ok) | validation-only |
| `$$relation-idempotency` | `{String (map-schema String RelationDecisionRow {:subindex? true})}` | `partition-by-object-key` (from-ok) | validation-only |
| `$$relation-events-by-id` | `{String RelationEventRow}` | `partition-by-object-key` (event-id ⇒ from-ok) | history (product-readable) |
| `$$relation-decisions-by-audit` / `$$relation-requests-by-audit` | `{String …Row}` | `partition-by-audit-id` | audit-only |

**Rows in both adjacency indexes are the FULL denormalized `RelationEdgeRow`, not a pointer.** A neighborhood read then costs 1 seek + N `Next`, no per-edge point-read (the skill's read/write trade: pay write, save read seeks). The cost is that `retract`/`annotate` must propagate the mutated row to **both** indexes (§6). Subindex is mandatory on the adjacency maps — a high-degree node without it loads its whole adjacency into memory on every read and blocks the single-threaded task (T11); large scans use `{:allow-yield? true}`.

**Write path partition dance** (the space `$$artifact-graph`/`$$artifact-graph-in` pattern):
1. Event arrives already on **from-owner task** (depot `hash-by :routing/key`). Atomically: Layer-1/Layer-2 dedup, mint event, write `$$relations-by-id[rel-id]`, `$$relations-out-by-from[from-id][rel-id]`, claim both dedup PStates, write `$$relation-events-by-id[evt-id]`.
2. `(|hash to-object-key)` → **to-owner task**. Write `$$relations-in-by-to[to-id][rel-id]` with `(termval row)` — deterministic, idempotent, safe under replay.

**Read plan — seeks enumerated, per promised query shape:**

| # | Query | Plan | Cost |
|---|---|---|---|
| Q1 | relation by id | `$$relations-by-id[rel-id]` | **1 seek** |
| Q2 | all **outbound** of X | scan `$$relations-out-by-from[X]` | **1 seek + N `Next`**, N = out-degree |
| Q3 | all **inbound** of X | scan `$$relations-in-by-to[X]` | **1 seek + M `Next`**, M = in-degree |
| Q4 | **full neighborhood** of X (D-004's "all relations of X") | Q2 + Q3 | **2 seeks + (N+M) `Next`, 1 roundtrip** |
| Q5 | who asserts proposition (A,kind,B) / is it Sid-backed | scan `$$relations-out-by-from[A]`, filter `to=B ∧ kind`, collect `asserter-key`/`asserted-by` | **1 seek + N `Next`** |
| Q6 | history of an edge | scan `$$relation-events-by-id` for `relation-id` (colocated) | **1 seek + K `Next`** |
| Q7 | dedup existence (internal) | `$$relation-dedup-by-fingerprint[from-ok][fp]` | **1 seek** |

**The payoff:** out-index is partitioned by `from`'s object-key, in-index by `to`'s object-key — so **the full neighborhood of X (both directions) is always colocated with X** and served by one query-topology invocation (one roundtrip). The *only* cross-task step is the reverse-index **write**; there is **no** cross-task read for any node query. That is the correct trade (skill: "a small increase in write-path work that dramatically reduces read-path seeks is almost always worth it"). Q5 is served by the out-index — **no separate proposition PState is declared** (it would not colocate on the owner task, since a proposition-hash key doesn't embed an object-key; filtering the already-colocated out-index is 1 seek and strictly simpler).

---

## 6. PROVENANCE + RETRACTION

- **`asserted-by` (`:sid`/`:llm`/`:import`) and the full `actor` are first-class on every edge and event.** `confidence` is carried for `:llm` edges so LLM glue is visibly provisional. `source-id`/`source-anchor-id` link the edge to the material it was derived from (nullable for direct human assertion). This satisfies D-004's "the map must not lie."
- **Retraction is never a delete** (skill: "Never delete data"). `:retract-relation` operates **by `relation-id`**, flips `assertion-status → :retracted` on the primary row, sets `retracted-by-event-id`/`-actor`/`-at-ms`, mints a `:relation-retracted` event, and **propagates the status flip to both adjacency indexes** (owner-task update of primary + out-index, then `(|hash to-ok)` update of in-index — same idempotent discipline as assert). History (assert → retract → re-assert flap) is fully recoverable from `$$relation-events-by-id`.
- **Retraction is per-`relation-id`, i.e., per-asserter.** Sid retracting his edge does **not** touch the LLM's identical-proposition edge (different `relation-id`). You cannot retract someone else's assertion (T8). Authorization: `actor`'s `asserter-key` must equal the edge's, **or** `actor-type = :sid` (human override). Retracting an already-retracted or non-existent edge ⇒ `:no-op`, no event.
- **Re-assertion after retraction** recomputes the same `relation-id`, flips status back to `:asserted`, mints a fresh event. The dedup claim persists across the flap, so re-assert is recognized as the same edge.

---

## 7. QUERY SURFACE

**Product-readable** (trail view, and later the space kernel + DG protocol):
- `$$relations-by-id`, `$$relations-out-by-from`, `$$relations-in-by-to`, `$$relation-events-by-id`.
- A `relation-neighborhood` **query topology**: given X, returns `{:outbound … :inbound …}` (both filtered to `:asserted` by default; `:include-retracted?` opt for history views) in **one roundtrip** off X's colocated indexes. Product code MUST use this rather than multiple client `foreign-select`s (skill: prefer query topologies over N client roundtrips). This topology MAY `mirror-pstate` object-container's `$$containers-by-id` to hydrate endpoint previews; that mirror is the **only** cross-module coupling, and it is read-side and optional.

**Validation-only — product code MUST NOT read these as truth:** `$$relation-dedup-by-fingerprint`, `$$relation-idempotency`, `$$relation-decisions-by-audit`, `$$relation-requests-by-audit`. They are gates and audit trails; their shape is internal and they do not reflect `assertion-status`/retraction (T10).

---

## 8. ACCEPTANCE GATES (each executable as a test; green ALL before close)

1. **Create+index.** `assert-relation!(A,produced,B,sid)` ⇒ `$$relations-by-id`, `$$relations-out-by-from[A]`, `$$relations-in-by-to[B]` all return the edge; exactly one `:relation-asserted` event.
2. **Deterministic identity.** Two asserts of the same `(from,to,kind,asserter)` with *different* idempotency-keys ⇒ one row per index, same `relation-id`, second decision `:deduped`, **no** second event.
3. **Idempotency-key replay.** Same request appended twice (at-least-once) ⇒ one row, one event, second decision `:replayed`, byte-identical to the first.
4. **Cross-actor provenance.** `sid` and `llm` assert the same `(A,produced,B)` ⇒ **two** rows / two `relation-id`s, both present in `$$relations-out-by-from[A]`; Q5 returns both asserter-keys with distinct `asserted-by`.
5. **Cross-partition fault injection (the Slice-A invariant).** Kill+restart the worker *between* the out-index write and the in-index write; on replay `$$relations-in-by-to[B]` converges to exactly one correct row — no orphan, no duplicate, dedup unbroken.
6. **Retraction.** `retract-relation!` flips status in `$$relations-by-id`, `$$relations-out-by-from[A]`, **and** `$$relations-in-by-to[B]`; default neighborhood read excludes it; history read includes it; the other asserter's edge (Gate 4) is untouched.
7. **Re-assert flap.** Assert→retract→assert on one identity ⇒ same `relation-id`, final status `:asserted`, event log shows all three in order.
8. **Neighborhood read plan.** With N outbound + M inbound edges on X, `relation-neighborhood(X)` returns all N+M in **one** query-topology roundtrip; instrument/assert the seek count is 2 (out + in), not O(N+M).
9. **Late-bound endpoint (D-003).** Assert a relation whose `to-target-id` is a commit sha with **no** materialized container ⇒ `:asserted` (no cross-module rejection); the edge stores the address and reads back normally.
10. **Kind validation.** Assert with a kind ∉ the closed set ⇒ `:rejected`, no event, no row, no index entry.
11. **Partition alignment.** For an edge whose `from` and `to` hash to *different* tasks, assert `$$relations-in-by-to[B]` is present **on B's task** (a misaligned implementation that copied the out-index partitioner would leave it empty here — this gate is the alignment tripwire).
12. **Mutable ≠ identity.** Re-assert the same identity with a different `note`/`confidence` ⇒ one row, an `:annotated` event, updated fields, unchanged `relation-id` and index cardinality.

---

## 9. TRAPS (naive alternative → concrete failure)

- **T1 — Dedup by `(from,to,kind)` without asserter.** LLM assertion merges into / overwrites Sid's; `asserted-by` is clobbered → the map can no longer say whether a human backs the claim. *The map lies.* Fix: asserter ∈ identity.
- **T2 — Random `relation-id` (UUID).** The reverse-index write is cross-partition and non-atomic; on retry a fresh UUID makes a second, orphaned in-index row, and dedup can't recognize the replay → the Slice-A v1→v2.1 duplication class. Fix: content-addressed id (§2.2), Gate 5.
- **T3 — Route by `request-id`/UUID/round-robin.** The same fingerprint reaches two tasks; each task-local claim is blind to the other → two identical edges, never reconciled. Fix: `routing-key = from-object-key ⊆ identity` (§4).
- **T4 — Synchronous cross-module endpoint existence check.** Rejects valid Regime-1 commit endpoints (not yet containers), couples every relation write to object-container availability, and adds a cross-task seek per write. Fix: structural validation only; endpoints are addresses that may resolve later (§3, §6, Gate 9).
- **T5 — In-index partitioned by `from`'s object-key (copy-paste of the out-index partitioner).** "All inbound of X" now scatter-gathers across all tasks → O(#tasks) seeks per node view. Fix: in-index partitioned by `to`'s object-key (§5, Gate 11).
- **T6 — Single (out-only) index.** "All relations TO X" becomes a full scan of every partition. Fix: dual index.
- **T7 — Retraction as delete.** Destroys history (what was once asserted becomes invisible), breaks event-source replay, and strands any consumer that had read the edge. Fix: status flip + event (§6).
- **T8 — Retract/annotate by `(from,to,kind)`.** Nukes/edits *every* asserter's edge for that proposition. Fix: operate by `relation-id`.
- **T9 — Edge in a `:global? true` PState.** Funnels all relation writes through task 0 → hotspot, no storage balance. Fix: partition by object-key.
- **T10 — Product code reads the dedup/idempotency PState as truth.** Those gates don't carry `assertion-status`; a retracted edge still shows a live claim → product renders retracted relations. Fix: read the edge PStates (§7).
- **T11 — Adjacency maps without `{:subindex? true}`.** A hub node loads its entire adjacency into memory per read and blocks the single-threaded task → latency spikes across all reads on that task. Fix: subindex + `{:allow-yield? true}` on large scans.
- **T12 — Forcing `dead-end` into a unary mark now (nil `to`).** A nil `to-target-id` breaks the in-index `keypath` (keypath on nil) and either strands the edge out of the reverse index or demands a sentinel hack that pollutes every neighborhood read. Fix: all kinds are directed binary (§2.3); a true unary mark waits for a form to break against its absence (D-001).
- **T13 — `confidence`/`note` in the identity.** Every re-annotation forges a new `relation-id` → dedup defeated, index bloat, N "copies" of one relation. Fix: mutable, non-identity (§2.2, Gate 12).
- **T14 — `target-address` in the identity.** A commit amend or path move re-forks identity → duplicate edges for the same relation. Fix: identity by `target-id`; address is mutable resolution metadata (§2.2, Gate 9).

---

## 10. Substrate touches outside the new module (exhaustive)

1. `extract-object-key`: add a `"rel:"` branch (`(leading-object-key (subs s 4))`) so `$$relations-by-id` routes to the from-owner task. Additive, one line.

Nothing else in object-container or space is modified. The relation kernel is otherwise self-contained.
