# Implementation Validation — relation-kernel-module

<!-- Phase 4. Adversarial validation of src/app/server/rama/relation_kernel.clj
     against CONTRACT.md (binding), IMPLICIT_SPEC.md, and PLAN.md. -->

Reviewer: Claude (Opus 4.8), 2026-07-03. Default verdict is `major-fail`; each
check below was moved off it only by explicit code-tracing with line citations.

## Inputs traced

- Module source: `src/app/server/rama/relation_kernel.clj` (801 lines), read in full.
- `CONTRACT.md` (binding, §11 = 11 acceptance gates, §9 = 6 refusals).
- `IMPLICIT_SPEC.md` (entity×write matrix, F2 relation-scoped amendment).
- `PLAN.md` (read strategy R1/R2, PState design, F1 depot-map fix).
- Rama references verified against (no local precedent existed for three idioms):
  `microbatch.md`, `query-topologies.md`, `batch.md`, `pstate-schema.md`,
  `paths.md`, `app-design.md`; precedent in `object_container.clj`,
  `core.clj`.

## Compilation / lint status

- `clojure -M -e "(require 'app.server.rama.relation-kernel)"` → `:COMPILE-OK`
  (independently re-run this session; Rama `defmodule` macroexpansion validated
  partition alignment, cross-branch unification of `<<if`, and batch/`|origin`
  structure of both query topologies).
- clj-kondo: 0 warnings; the 4 `Unresolved symbol: $$…` errors are the known
  Rama-macro-scoping false positives (PState refs inside query topologies),
  identical class to `object_container.clj`. The Rama compiler is authoritative.

---

# A. Template checks (references/artifact-impl-validation.md)

## A1. Redundant conditionals — PASS

Check: *if every branch of an `<<if`/`<<cond`/`<<switch` does the same operation
with only a variable differing, replace with a single operation.*

- `L559 <<if (outcome-accepted? *outcome)` — single-armed (then only). Not redundant.
- `L615 <<if (empty? *ranges)` — then binds `*row` via `(identity nil …)`; else
  binds `*row` via `ops/explode` + range `local-select>`. Different operations
  (nil seed vs. read). Not redundant; the two arms deliberately diverge in read
  cost (0 seeks vs. N).
- `L636/L638 <<if` (relation-detail) — the outer else (`valid-relation-id?` false)
  skips the point read entirely (0 PState reads for malformed ids, per PLAN R2
  L211/L230); the inner else (valid id, `*row` nil) has *already paid* one point
  read as the absence proof. Both yield `{:row nil :history []}` but via
  intentionally different read costs, so they cannot be merged without either
  adding a seek to the malformed path or dropping the absence proof. Not redundant.

## A2. Consecutive keypath — PASS

Check: `(keypath *a) (keypath *b)` → `(keypath *a *b)`.

Every navigation already fuses its keys: `(keypath *relation-id *journal-key)`
(L540/L552), `(keypath *from-tk *from-sk)` (L590), `(keypath *from-tk *from-dk)`
(L591), `(keypath *relation-id *order-key)` (L568). Range reads use
`(keypath *target-key) (sorted-map-range …) MAP-VALS` (L622) — a keypath followed
by a *range* navigator, not a second keypath. No consecutive separable keypaths.

## A3. Select-compute-transform — PASS (justified)

Check: `local-select>` → compute → `local-transform>` with `termval` should become
`+compound`/aggregator *when possible*.

The one select→compute→termval chain is on `$$relations-by-id`:
`L545 local-select> :> *current-row` → `L546 relation-outcome … :> *outcome` →
`L567 local-transform> (termval *row)`. It is **not collapsible**: `*outcome`
(derived from `*current-row`) feeds seven writes across three tasks — the
decision (L552-554), event/log (L566/568), and the two cross-partition target
copies + descriptor deltas (L590-601), the last reached only after `(|hash …)`.
`(term f)` on `$$relations-by-id` would recompute the row but could not surface
`*outcome` as a value for the cross-partition writes. So the read must be
materialized. This is the documented "when possible" escape, not the anti-pattern.

The descriptor writes correctly use `term` (single read-modify-write, no separate
select — L592/600), and the target-row copies correctly use blind `termval`
(no read — L590/598, the trap-7 full-overwrite). Both follow the skill's no-read
guidance.

## A4. Unnecessary nil->val — PASS

`nil->val` appears nowhere in the module. Nil-safety is delegated to the
navigators themselves (`paths.md`: `MAP-VALS`/`sorted-map-range` on nil → empty)
and to plain helpers (`relation-read-ranges` folds over a possibly-nil
`descriptor-map` via `for`, L432; `relation-visible?` treats nil `*row` as the
presence seed, L441-447). No defensive nil->val to remove.

## A5. :allow-yield? — PASS

Check: unbounded subindexed range reads on non-mirror PStates need `{:allow-yield? true}`.

- R1 unbounded read: `L622-623 (sorted-map-range *lo *hi) MAP-VALS …
  $$relations-by-target {:allow-yield? true}` — present. `$$relations-by-target`
  inner map is subindexed and unbounded (a popular doc accrues unbounded
  relations). ✓
- R2 unbounded read: `L639-640 (subselect MAP-VALS) …
  $$relation-status-log-by-relation {:allow-yield? true}` — present. Status log
  is unbounded per relation. ✓ (matches `paths.md:640` yield-on-subselect idiom).
- Bounded reads correctly OMIT yield: journal point read (L540, single key),
  authoritative row point read (L545, single key), descriptor map read
  (L613, whole inner map bounded to ≤14 rows — see A6). Adding yield there would
  be pure overhead. ✓

## A6. Non-subindexed collections without size limits — PASS

Every inner collection that can grow is subindexed:
`$$relation-decisions-by-idempotency` (L499), `$$relation-status-log-by-relation`
(L512), `$$relations-by-target` (L516) — all `{:subindex? true}`.

The one non-subindexed inner map is `$$relation-target-descriptors`
(L519-520, `{String (map-schema String RelationTargetDescriptorRow)}`). Its size
is **structurally capped**, not merely assumed: keys are `descriptor-key =
"<dir>:<kind>"` where `dir ∈ {"o","i"}` (2, from `direction-code`, L140) and
`kind ∈ relation-kinds` (7, L46-47), enforced because an unregistered kind is
rejected before any descriptor write (`request-shape-errors` L251-252 →
`relation-outcome` rejects → the `<<if outcome-accepted?` gate at L559 skips all
descriptor writes). Max 14 rows/target. This is the PLAN's stated bound
(L587-591). ✓

The top-level `{String …}` maps (`$$relation-decisions-by-id`, `-events-by-id`,
`$$relations-by-id`) are PState top levels, not inner collections — inherently
partitioned, not a size concern.

## A7. Stream topology idempotency — PASS (N/A: microbatch)

No stream topology exists. The single write topology is microbatch
(`L493 (microbatch-topology topologies "relation-kernel-topology")`).
`microbatch.md` `(Microbatch-ExactlyOnce)`: retry of a microbatch id applies
PState updates exactly-once via deterministic replay. This is what makes the
non-idempotent descriptor increment (`term` RMW, L592/600) safe — a stream
topology's at-least-once delivery would double-count it. The module performs **no
internal `depot-partition-append!`** (the one microbatch op that is *not*
exactly-once, `microbatch.md` `(Microbatch-Append-NoGuarantee)`), so that carve-out
does not apply. ✓

## A8. Partial failure in stream topologies — PASS (N/A: microbatch)

The seven writes (decision×2, event, row, log, and the two endpoint copies +
descriptors) span up to three tasks (relation-id, from-key, to-key). Under
`microbatch.md` `(Txn-Preserve)`, the `|hash` hops at L589/L597 do **not**
fragment the transaction — all Write effects remain one atomic microbatch scope.
A mid-batch crash discards the attempt and re-primes to the previous committed
state, then replays; there is no committed partial state where one endpoint copy
exists without the other. ✓ This is CONTRACT §5's exactness-by-construction claim,
verified.

## A9. Single depot append per client operation — PASS

`append-relation-request!` (L735-744) performs exactly one `foreign-append!`
(L743) of one envelope. No client operation issues multiple appends; the client
builds the whole envelope (`envelope`, L677-690) and appends once. There are no
server-side internal depot appends. ✓

## A10. Application-state caches survive restart — PASS (N/A)

No `TaskGlobal`, no in-process cache. All state is in the seven durable PStates.
Query topologies use only the implicit per-invocation temp PState (`+vec-agg`
aggregation), which is by design ephemeral. Nothing to rebuild on restart. ✓
(PLAN "Worker restart" L874.)

## A11. No reimplementation of built-in operations — PASS

- `positive-partition` (L76-80) reimplements the 2-line `(mod (hash k) N)` idiom.
  This is **required, not a violation**: STANDING permits importing only
  `extract-object-key`/`fixed-width-order-key`/`actor-row` from object-container;
  `positive-partition` is not on that list, and it is not a Rama built-in (a
  `:key-partitioner` is by definition a user fn). Copying it is the correct move
  and matches `object_container.clj:273` / `transcript_ingest.clj:139` verbatim.
- `sha1-hex` (L61-66) is content-identity logic, not a Rama built-in.
- The module *uses* `ops/explode`, `aggs/+vec-agg` (verified below) rather than
  hand-rolling fan-out/aggregation. ✓

---

# B. CONTRACT §11 acceptance gates (all 11)

Each traced against the code path a Phase-5 IPC test would exercise.

| # | Gate | Verdict | Trace |
|---|---|---|---|
| 1 | Assert + dual read (different partition keys) | PASS | `relation-outcome` accepted branch (L365-395) builds `:from-copy` under `(:target-key from)` and `:to-copy` under `(:target-key to)`; topology writes each after `(|hash *from-tk)` L589 and `(|hash *to-tk)` L597. R1 `relations-for-targets` reads both target keys (fan-out L611-613). Distinct keys ⇒ distinct tasks ⇒ readable from both. |
| 2 | Idempotent client retry (same idem key) | PASS | First request: journal miss `L540-542`, `filter> (nil? *prior-decision)` L543 passes, writes decision to `[relation-id journal-key]` L552. Retry: journal hit ⇒ `filter>` stops the record ⇒ zero further writes. The first decision persists (replay = read the journal). One relation, one event. Same-batch retry also stopped: RYW via in-memory buffer (`pstate-schema.md:37`) makes record-2's `local-select>` see record-1's write. |
| 3 | Convergent re-import (diff idem keys, same relation-id) | PASS | `relation-id-for` (L86-93) excludes idempotency-key ⇒ both requests route to the same task. 2nd request: journal miss (diff key), reads `*current-row` (present), `relation-outcome` `:else` reassert; `transition-row` preserves `first-asserted-at-ms` (L279-283) ⇒ identical `target-sort-key` ⇒ `termval` copy overwrites in place (no 2nd index row); `count-deltas` `(= old new)` ⇒ `{:total 0 :asserted 0}` (L267) ⇒ descriptor unchanged. One row, one index entry/direction, two audit events. |
| 4 | Retract consistency (A-side, B-side, by-id; ordered history) | PASS | Retract writes `*row` (status `:retracted`) to `$$relations-by-id` L567 and `termval` copies to both target indexes L590/598 at the **same** sort-keys (first-ms preserved) ⇒ both endpoints agree. `count-deltas` asserted→retracted `{:total 0 :asserted -1}` L269. R2 returns row + `sort-by :order-key` history (L480). Visible from A/B under `include-retracted? true` (descriptor `total-count` still 1 ⇒ range read fires). |
| 5 | Retraction rights (diff actor → rejected) | PASS | `relation-outcome` L355-360: `:relation/retract` with `actor-id ≠ (:asserter-actor-id current-row)` ⇒ rejected `:relation/retraction-forbidden`, no row/index write (accepted `<<if` skipped). Two-layer defense: identity includes asserter (a forged `:asserter-actor-id` routes to a different/absent relation ⇒ `:relation/absent`), and the rights check catches an envelope-`:actor` mismatch against the stored asserter. |
| 6 | Registry (unregistered kind → rejected, no non-audit writes) | PASS | `request-shape-errors` L251-252 flags `:relation/kind-unregistered`; `relation-outcome` returns `{:accepted? false}` (L343-346). Topology writes decision to journal + by-id (L552-555, the audit spine) then the `<<if outcome-accepted?` (L559) is false ⇒ zero event/row/log/target/descriptor writes. |
| 7 | Dangling (target matches nothing → accepted) | PASS | No existence check anywhere in `relation-outcome`; `well-formed-target?` (L212-217) validates *shape* only (kind keyword, present id, present key). A target-id addressing nothing is well-formed ⇒ accepted ⇒ readable from its derived key. |
| 8 | Unary `:none` (one target-key touched; readable from-side) | PASS | `unary-to-ref` (L672-675) sets `to.target-key = from.target-key`. Both `(|hash *from-tk)` and `(|hash *to-tk)` route to the same task (equal keys) ⇒ one target-key's index touched, two sort-keys `"o:…"`/`"i:…"`. R1 dedups by relation-id (`relations-pairs->map` L463-466) ⇒ surfaced once. No nil-key global hotspot (trap 8). |
| 9 | Asserter separation (sid vs llm → two ids) | PASS | `relation-id-for` includes `asserter-actor-id` (L92) ⇒ different ids ⇒ different rows, different sort-keys. R1 dedup keys on relation-id, so both survive; both returned under the shared target key. |
| 10 | Kind filter (returns only matching kinds) | PASS | `relation-read-ranges` (L429-439) emits a prefix bound-pair only when `(contains? kinds kind)`; `descriptor-prefix-bounds` (L151-156) yields `["dk:" "dk;")` which — via `sorted-map-range` `[start,end)` (`paths.md:459`) — selects exactly the `"<dir>:<kind>:…"` sort-keys and nothing else. No cross-kind leakage. |
| 11 | Cross-relation key reuse (F2: both succeed) | PASS | Journal is nested `{relation-id → {idem-key → decision}}` (L498-499); the gate reads `[(keypath *relation-id *journal-key)]` (L540). Reuse of one idem-key across two relation-ids lands under two different top-level keys on two tasks ⇒ neither collapses ⇒ both succeed. Matches the relation-scoped F2 ruling (CONTRACT §5). |

**Style gates (CONTRACT §11 footer):** typed defrecords for all rows (L160-198) ✓;
partition helpers imported not reimplemented (`oc/extract-object-key`,
`oc/fixed-width-order-key` L149/L667-668; `positive-partition` correctly *not*
imported per the dependency allow-list) ✓; `{:allow-yield? true}` on both
unbounded reads ✓; consumers restricted to R1/R2, V1 readers marked test-only
(L654-657, L763) ✓.

---

# C. CONTRACT §9 refusals (all honored)

| # | Refusal | Honored? | Evidence |
|---|---|---|---|
| 1 | No FK validation of targets | Yes | No existence read in `relation-outcome`; `well-formed-target?` checks shape only (L212-217). Trap 3. |
| 2 | No confidence / credential algebra | Yes | No confidence field on any row (L187-190) or in validation. |
| 3 | No relation-as-container | Yes | Relations are edge rows; nothing treats a relation-id as a commentable container. |
| 4 | No auto-merge across asserters | Yes | Asserter in identity (L92) ⇒ separate rows; no merge path. |
| 5 | No cascading on retraction | Yes | Retract flips status + writes copies for that one id only (L365-395); no traversal of neighbors. |
| 6 | No deletion, ever | Yes | Every write is `termval`/`term` upsert. No `NONE>`/`setval NONE`/delete navigator anywhere. `total-count` never decreases (`count-deltas` L266-269); retraction is a status. |

---

# D. Deep dives (baton-mandated)

## D1. Partition alignment & colocation — PASS (validated by hardened precedent)

The depot `(hash-by :relation/routing-key)` (L491) routes each request to
`rama-hash(relation-id)`. Two PStates carry a **custom** `:key-partitioner` built
on `positive-partition` = `(mod (clojure.core/hash k) N)`:
`$$relation-decisions-by-id` / `$$relation-events-by-id`
(`{:key-partitioner partition-by-decision-relation}` L503, `-event-` L507). Their
rows are written by a **local** transform on the relation-id task (L554/566, no
`|hash` precedes them). Correct alignment therefore requires
`clojure.core/hash(relation-id) ≡ rama-hash(relation-id) (mod N)`.

This is the **identical idiom** object-container relies on and has hardened:
`$$decisions-by-audit-id`/`$$requests-by-audit-id` use
`{:key-partitioner partition-by-audit-id}` (`object_container.clj:1690-1692`,
`partition-by-audit-id = positive-partition(audit-key)` :347-349), colocated with
its `(hash-by :partition/key)` depot (:1686), and its stream journal gate reads
them via a bare `local-select>` on the depot task with no `|hash` (:1778). If
those hashes disagreed mod N, object-container's audit reads would always miss —
so the colocation holds in the running system. The relation-kernel is a
one-to-one copy of that pattern.

The five default-partitioner PStates (`$$relations-by-id`, `-by-target`,
`-status-log-`, `-decisions-by-idempotency`, `-target-descriptors`) are written on
the task reached via the depot hash / `|hash`, and read via `|hash` in the query
topologies (L611/636) — both Rama's own hash, so self-consistent regardless of
the clojure-hash question. Alignment is sound. *(Flagged as an Open Doubt below
only because I validated it by precedent, not by reading Rama's `hash-by` source;
the Phase-5 partition-provenance test the PLAN already carries, L641-644, closes it.)*

## D2. `term`/`partial` closure over dataflow vars — PASS

`(term (partial apply-descriptor-delta *from-desc *total-delta *asserted-delta))`
(L592) closes over three dataflow vars. This is the documented mechanism:
`core.clj:614` uses `(term #(write-if-absent % *row))` closing over `*row`.
`(partial f *a *b *c)` is equivalent to `(fn [x] (f *a *b *c x))` — the current
value arrives as `apply-descriptor-delta`'s last param `existing` (L299-307),
which is the intended arg order. Rama's compiler walks the operation-argument form
to bind dataflow vars regardless of whether the wrapper is `#(...)` or
`(partial …)`; the module compiling `:COMPILE-OK` with `*from-desc`/`*total-delta`/
`*asserted-delta` bound at L575-580 confirms they were captured (an unrecognized
`*`-symbol would fail dataflow validation). First use of `term`+`partial` in this
codebase, but semantically the documented `term`+closure idiom.

## D3. Descriptor-gated reads (R1) & retry-safety — PASS on correctness

- Fan-out shape matches the documented cross-partition join
  (`query-topologies.md:82-90`): `ops/explode` targets → `|hash` → descriptor
  point read → gated range reads → `|origin` → `aggs/+vec-agg` → post-agg map.
  `aggs/+vec-agg` is a real, documented aggregator (`query-topologies.md:89`),
  with **no** prior use in this codebase — verified against the reference, not
  precedent.
- Every requested target appears in the result: each target emits either a nil
  seed (`empty? *ranges` L616) or ≥1 row; `relations-pairs->map` (L449-474) turns
  the nil seed into `[]` and dedups real rows by relation-id. Empty/blank input →
  `distinct-present-target-keys` → `ops/explode []` → `+vec-agg` fires on zero
  rows emitting `[]` (`batch.md:43`) → `{}`.
- Descriptor counter is retry-safe: the `term` increment is non-idempotent but
  microbatch exactly-once (A7) replays from the primed prior state, so no
  double-count on retry. Cross-partition atomic with the row/copy writes (A8).

## D4. Unary `:none` end-to-end — PASS

Client: `->target-ref :none nil from-target-key` (L659-670) → key inherited.
Validation: `unary-target?` (L219-228) requires `:none` + nil id + key equal to
from's. Write: both copies + both descriptors land on the one from-task under
`"o:"`/`"i:"` keys. Read: R1 reads both prefixes, dedups to one row. Retract:
`count-deltas` decrements both prefixes' asserted-count symmetrically. Consistent.

---

# E. Falsification pass (CLAUDE.md review protocol)

**Writers / readers / clearers, per changed state:**
- `$$relations-by-id[relation-id]` — writer: accepted branch `termval *row` (L567,
  single task). Reader: R2 (L637), write-path fold (L545). Clearer: none by design
  (no deletion, §9.6) — status transitions overwrite, never clear. Single writer
  (relation-id task) ⇒ no writer race.
- `$$relations-by-target[target-key][sort-key]` — writer: `termval *row` on
  from-task and to-task (L590/598). Sort-key is stable across status changes
  (first-asserted-at-ms preserved), so status flips overwrite the same slot rather
  than accreting; the copies cannot disagree across a batch (one transaction).
- `$$relation-target-descriptors[target-key][dk]` — writer: `term` delta
  (L592/600). Only accepted transitions write it; rejected/replayed requests do
  not (gated by the `<<if outcome-accepted?` and the journal `filter>`).
- Journal `[relation-id][idem-key]` — writer: `termval *decision` (L552) for every
  processed request; reader: the gate (L540). Clearer: none (durable replay token).

**Async / ordering:** assert-then-retract for one relation serialize on the
relation-id task (colocated). Across batches, batch N primes on batch N-1's
committed state ⇒ later status wins. Within a batch, RYW via the in-memory buffer
(D3/A7) ⇒ record order = depot order preserved. An older callback cannot overwrite
newer state because there is a single writer per key and sort-keys are stable.

**Error path:** a malformed/forbidden request produces a durable rejected decision
(journal + by-id) and touches nothing else — the in-flight "world truth" writes are
all inside `<<if outcome-accepted?`. No locks/pending-sets to leak (none exist).
A mid-batch crash replays the whole attempt (microbatch), releasing nothing because
nothing external was acquired.

**Shape:** traced `[(keypath *target-key) (sorted-map-range "dk:" "dk;") MAP-VALS]`
against the actual `{target-key {sort-key row}}` structure — sort-keys are
`"<dir>:<kind>:<fixed-width-order-key>"`, and `"dk:"`/`"dk;"` bracket exactly that
group (`:` = 0x3A, `;` = 0x3B). Path executes against the real shape. ✓

**Done gate (one plausible failure named per gate):** the sharpest was gate 3
duplicate index rows from a same-batch fork; ruled out by in-batch RYW (D3). The
remaining residual is the same-batch multi-key convergence *if* RYW did not hold —
see Open Doubts; it does not affect any of the 11 gates (tested with the RAW
barrier between appends).

---

# F. Divergences from PLAN / CONTRACT (the finding)

## F1. R1 no-filter path issues N prefix seeks, not the planned single range — MINOR

`PLAN.md` R1 states, for the no-kind-filter case, to **"generate one `:all`
range-read descriptor"** (L786-789) and its cost example promises **"1 descriptor
point read + 1 target range read"** (L159-160). CONTRACT §6 makes the same
performance claim for the dominant query: *"all relations touching X — is 1 seek +
sequential iteration"* (L183-184).

The implementation does not do this. `relation-read-ranges` (L429-439) emits one
prefix bound-pair **per surviving `(direction, kind)` descriptor** in *all* cases;
when `kinds-filter` is empty, the `:when` reduces to `(pos? surviving)`, so a
no-filter read of a target that is the endpoint of relations spanning D directions
× K kinds issues D×K separate `sorted-map-range` seeks instead of one whole-map
`MAP-VALS`. Bounded at 2×7 = 14 seeks/target (registry cap), typically 2–8.

- **Not a correctness defect:** the union of the per-prefix ranges returns exactly
  the same rows as one whole-map read; gates 1/4/7/9 (which exercise no-filter
  reads) still pass.
- **But** it is a real I/O divergence from both the PLAN's explicit instruction and
  CONTRACT §6's stated "1 seek," on precisely the dominant read path the
  `$$relations-by-target` denormalization exists to make cheap. The skill's
  "never trade I/O for code simplicity" rule applies: the per-prefix path was
  written once for the kind-filter case and reused for no-filter, trading seeks for
  a single code path.
- **Fix is localized** (⇒ minor, not major): special-case no-filter in
  `relation-read-ranges` to return a single whole-map read plan (gated by "any
  descriptor survives for the status mode"), and branch the R1 else-arm (L621-623)
  to a `[(keypath *target-key) MAP-VALS]` read when the plan is the whole-map
  marker. No PState, topology, write-path, or public-API change. The include-
  retracted?=false filtering already happens at `relation-visible?` (L624), so a
  whole-map read + status filter is correct.

No other divergence found. The F1 depot-map fix (map envelope, not
`RelationRequestRow`) is correctly implemented (L169-176, L677-690, depot L491);
the F2 relation-scoped journal is correctly nested (L498-499, L540).

---

# G. Open doubts (verification recommendations, not blockers)

1. **Microbatch intra-batch read-your-writes.** The same-batch idempotency/
   convergence argument (gates 2/3) rests on `pstate-schema.md:37` (writes hit an
   in-memory buffer flushed at batch boundary) + the single-threaded task model
   (`app-design.md:13`). The references do not contain an *explicit* "a later
   record's read sees an earlier record's write" sentence; I inferred it (strongly).
   If it did not hold, two same-relation requests with different idem-keys AND
   different timestamps landing in one batch could write two target sort-keys and
   double the descriptor count. This affects **no** §11 gate (all tested with the
   RAW barrier between appends). Recommended Phase-5 test: append two such requests
   *before* waiting on processed-count, then assert one row, one index entry per
   direction, and `asserted-count = 1`.

2. **`hash-by` ≡ `clojure.core/hash` (mod N).** D1's colocation is validated by
   object-container precedent, not by reading Rama's partitioner source. The PLAN
   already carries the partition-provenance test (append → read
   `decisions-by-id`/`events-by-id` via `foreign-select`, which routes by the
   custom key-partitioner → must hit). Keep it in Phase 5; it is the direct
   falsifier.

Neither doubt is a contract-breaking flaw (stop clause not triggered): both are
covered by, or should be added to, Phase-5 tests, and both are backed by hardened
precedent / authoritative reference.

---

# Verdict

One finding: **F1** — the R1 no-filter read path issues up to 14 prefix seeks where
the PLAN specifies one whole-map range and CONTRACT §6 promises "1 seek," an I/O
divergence on the dominant read path. It is not a correctness defect and is fixable
with a localized edit to `relation-read-ranges` + the R1 else-arm (no PState/
topology/API change). All 11 acceptance gates, all 6 §9 refusals, partition
alignment, the three-copy atomic write, idempotency replay, retraction rights,
descriptor gating, unary `:none`, and every template check pass. Per the rubric
(one failure, localized-line fix) the verdict is minor-fail: apply the fix in Phase
3, then proceed to Phase 5 without re-running Phase 4.

PHASE_VALIDATION:minor-fail
