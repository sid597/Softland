# Trail-View WP1 — Implementation PLAN (Phase, fresh-context)

Status: PLAN artifact (Opus, fresh context, 2026-07-04). Executable phase-by-phase
by a fresh Codex/Opus session under `/rama` + `/work-package`. Derived FROM
`build/trail-view/CONTRACT.md` v1.1 (BINDING) + `IMPLICIT_SPEC.md` (incl. §7
post-Phase-0 resolutions) + the on-disk code and Rama references. On any conflict
the CONTRACT governs; genuine two-reading conflicts are recorded in §10 Open items,
never silently picked (work-package stop clause).

Reading rule inherited from the F2 lesson: every key / journal / PState / uniqueness
claim below names its **partition scope** in the sentence that introduces it.

NUL discipline: the relation kernel's id-part separator is the NUL control byte,
written here only as the word **NUL** (never the raw byte / escape sequence).

---

## 0. Duty receipts (settled before planning)

### Duty 2 — baseline suite (GREEN)
`clojure -M:test -e "(require 'clojure.test 'app.server.rama.relation-kernel-test) (clojure.test/run-tests 'app.server.rama.relation-kernel-test)"`
→ **Ran 2 tests containing 165 assertions. 0 failures, 0 errors.** Matches the
contract's gate-16 baseline exactly. Planning proceeds on a green baseline.

### Duty 1 — F-8 spike (VERDICT: WORKS → single-roundtrip design, NOT the fallback)
Two throwaway modules under the scratchpad (never committed), launched A-then-B on
`create-ipc`, proved every load-bearing Phase-B mechanic:

1. **A query topology in module B CAN invoke a mirror query topology declared from
   module A** via `(invoke-query *mirror-query *arg :> *out)` (mirrors.md:150,153).
   Spike `b-query` returned `[val-x val-x]` — the mirror-query invocation and the
   mirror-PState read both resolved.
2. **A query topology CAN `local-select>` a mirror PState after an object
   partitioner** — but the partitioner MUST be the mirror form `(|hash$$ $$mirror
   *k)` (mirrors.md:113,133), NOT plain `|hash`. Plain `|hash` routes within the
   *current* module's task space and would silently mis-read (rama skill goal 5:
   "misalignment is silent").
3. **Custom-key-partitioner routing works** (the OC family case): OC family PStates
   store a full-id map key but choose the task by `partition-by-object-key`
   (`object_container.clj:336`, = `hash(extract-object-key(id)) mod N`). The spike's
   module A mirrored this (two PStates, custom key-partitioner, keyed `"doc:K"`/
   `"rev:K"`, partitioned by `K`). `(|hash$$ $$mirror *object-key)` with the
   **extracted** object-key routed to the correct source task for every key:
   `:FAM K1 [D-K1 R-K1]`, `alpha`, `beta-9` all correct; `MISSING → [nil nil]`.
4. **One `|hash$$` sets the mirror-partition index for sibling same-module,
   same-partitioner mirror PStates read next** (module-dependencies.md:119): the
   spike read `$$a-doc` then `$$a-rev` after a *single* `(|hash$$ $$a-doc-mirror
   *okey)` and both hit. → B1 reads a whole OC family in **one partition hop**.

Module-name format confirmed (`get-module-name` → `"<ns>/<module-var>"`; spike
printed `spike-f8/spike-a-module`). The three source module names Phase B mirrors:
- relation-kernel-module → **`"app.server.rama.relation-kernel/relation-kernel-module"`**
- object-container-module → **`"app.server.rama.object-container/object-container-module"`**
- object-container-transcript-ops-module → **`"app.server.rama.object-container/object-container-transcript-ops-module"`**
  (verified: the ops module's own `mirror-pstate` literal at `object_container.clj:2550-2553`
  uses `"app.server.rama.object-container/object-container-module"`.)

Residual (untested-in-spike, §10): invoking the *complex* R1 (explode+fan+agg) as a
mirror query — the spike proved the mechanism on a simple query; R1's internal shape
is already-tested kernel logic. The pre-specified fallback (client composition,
unchanged wrapper signatures, §7) remains available if a complex-query-as-mirror edge
surfaces in Phase B.

---

## 1. Module topology, launch order, and the seam (unchanged from IMPLICIT_SPEC §0)

- **Amend** `relation-kernel-module` (`relation_kernel.clj`, 829 lines, ONE microbatch
  topology + R1/R2): custody fields, three stance kinds, `:request/sent-at-ms`
  envelope key, the activity PState + accepted-branch write, R3.
- **New** `trail-view-module` (`src/app/server/rama/trail_view.clj`, ns
  `app.server.rama.trail-view`): mirrors + THREE query topologies; **zero depots,
  zero stream/microbatch topologies, zero own PStates** (gate 14 = read-only by
  construction).
- **Untouched**: `object-container-module` + `object-container-transcript-ops-module`
  (mirror-read only), and every standalone `tc:*` transcript module (out of scope,
  §2 / trap 13).

**Launch order (harness + prod): the three source modules BEFORE trail-view-module.**
Rama enforces mirror dependencies at launch (18-module-dependencies.md:145-149;
mirrors.md:193-195): `launch(trail-view)` fails if any mirrored module is not running.
Order in the trail-view IPC harness: object-container-module → its transcript-ops
module → relation-kernel-module → trail-view-module.

---

## 2. Phase A1 — custody recording (§5.1; gates 10, 16; lands FIRST, before daily use)

**Fact being fixed** (verified `relation_kernel.clj:355-356` retraction check reads
envelope `actor-id`, but every stored row keeps only the *payload* `asserter-actor-id`;
the envelope actor is never persisted). Phase-1's write pattern (Sid instructs, agent
appends: payload asserter = `sid`, envelope actor = the agent) would erase custody
(trap 10).

### 2.1 Record-shape changes — add two fields to each of THREE records
Append `envelope-actor-id` + `envelope-actor-type` to the END of each positional
field vector (end-append minimises the constructor edits, but note defrecord
positional `->` constructors are order-sensitive — every call site MUST update):
- `RelationDecisionRow` (`relation_kernel.clj:178-180`) → gains the two fields.
- `RelationEventRow` (`:182-185`) → gains the two fields (the full custody trail lives
  in events + decisions).
- `RelationEdgeRow` (`:187-190`) → gains the two fields (edge carries LAST-transition
  custody only).
- `RelationStatusLogRow` / `RelationTargetDescriptorRow` — **unchanged** (custody is
  not per-status-log; descriptors are counts).

### 2.2 Envelope actor flow into `relation-outcome`
`relation-outcome` (`:314-395`) already binds `actor` (`:325`) and `actor-id` (`:326`).
Add `envelope-actor-type` = `(:actor/type actor)` beside `actor-id`. Then thread both
into every row constructor built inside `relation-outcome`:

| Constructor call | line(s) | change |
|---|---|---|
| `rejected-decision-row` (helper `:309-312`) | called at `:345, :351, :358` | helper gains `envelope-actor-id envelope-actor-type` params (append to its arglist + the inner `->RelationDecisionRow`); all 3 call sites pass `actor-id` + `envelope-actor-type` |
| accepted `->RelationDecisionRow` | `:383-384` | append the two custody args |
| `->RelationEventRow` | `:373-376` | append the two custody args |
| edge row via `transition-row` (`:271-286`) | called at `:371` | `transition-row` gains the two params; the **new-row** branch (`:284-286`) passes them to `->RelationEdgeRow`; the **assoc** branch (`:278-283`) additionally `assoc`es `:envelope-actor-id`/`:envelope-actor-type` so the edge always reflects the LATEST transition's writer |

Note: `endpoint-copy` (`:288-297`) copies the whole `*row` (via `termval *row` at the
copy hops `:610, :618`) — so the from/to target copies inherit custody automatically;
**no change to `endpoint-copy` or the copy hops** beyond the record shape.

### 2.3 Bundle/edge projection (deferred to Phase B, stated here for the contract)
`:written-by` is projected from the edge row's `envelope-actor-id` ONLY when it differs
from `asserter-actor-id` (§5.1; gate 10). Pure projection fn lives in trail-view (B).

### 2.4 Arity-forced test updates
Grep confirms the test ns constructs NO `->Relation*Row` directly — it uses
`assert-request`/`retract-request`/`->target-ref`/`relation-id-for` + the V1 readers
only (`relation_kernel_test.clj`). So the defrecord arity change forces updates ONLY
inside `relation_kernel.clj` (§2.2). Existing assertions stay valid: for asserts the
envelope actor defaults to the asserter (`envelope` fn `:714`), so `envelope-actor-id
== asserter-actor-id` and the full-row-equality assertion (gate 7 / T10,
`relation_kernel_test.clj:168`) still holds across all three copies. **Additive** test:
one deftest block asserting custody diverges when envelope actor ≠ payload asserter
(gate 10; §7 below).

### 2.5 Migration note
The pre-amendment relation corpus is dev-stage. A clean dev relaunch + convergent
re-import regenerates every row WITH custody (deterministic ids converge — no dup
rows). **Sequencing law (§5.1): A1 lands before real assertions accumulate.** No
PState migration code; relaunch regenerates.

---

## 3. Phase A2 — stance kinds (§5.2; gate 15)

Single-line registry edit at `relation_kernel.clj:46-47`:
```clojure
(def relation-kinds
  #{:based-on :produced :built-over :new-direction :dead-end :elaborates :references
    :confirms :refutes :supersedes})              ; +3 → 10 kinds
```

**Verified zero other code path hard-codes the kind list.** Grep of `src/` and
`test/` for the kind keywords shows every kind check routes through
`registered-kind?` (`:210`, `(contains? relation-kinds kind)`) or `relation-kinds`
itself; there is no second enumeration, no per-kind switch, no client allowlist. R1's
kind filter is descriptor-driven (`relation-read-ranges :438-453`), not a literal
kind list. So `:confirms/:refutes/:supersedes` are accepted the instant they are in
the set. Descriptor-bound consequence (gate 15): the per-target-key
`$$relation-target-descriptors` PState (non-subindexed, on `hash(target-key)`) stays
≤ 2 × 10 = 20 rows — still bounded (§5.2 registry-scope note).

Stance semantics are enforced by the same machinery, no new code: binary directed
edges (`from` = judgment carrier, `to` = judged thing), `:supersedes` `from` =
replacement / `to` = displaced. The degenerate pure-stance case uses the CLI
conversation/message container as `from` (always ingested) — no unary stance form
(trap 5). These are conventions for the writer + the verdict fold (Phase B), not
kernel changes.

---

## 4. Phase A3 — activity projection + R3 (§5.3; gates 9, 16)

### 4.1 `RelationActivityRow` (new defrecord, 16 fields, exactly per §5.3)
```clojure
(defrecord RelationActivityRow
  [order-key bucket relation-id relation-kind from-kind from-id to-kind to-id
   asserter-actor-id asserter-type envelope-actor-id relation-status
   previous-status event-id claimed-at-ms arrival-at-ms])
```
(Note: activity row carries `envelope-actor-id` but NOT `envelope-actor-type` — the
feed shows the writer id; §5.3 field list is authoritative.)

### 4.2 New PState (declared in the SAME microbatch topology, near `:518-540`)
```clojure
(declare-pstate mb $$relation-activity-by-bucket
                {String (map-schema String RelationActivityRow {:subindex? true})})
;; outer key = bucket (fixed-width UTC day-index string), on hash(bucket);
;; inner subindexed map: order-key → row, on the bucket's task.
```
Partition scope: activity rows live on **`hash(bucket)`** — one task hosts one UTC
day's writes (bucket hotspot bound, §5.3; phase-1 volume tens/day; pre-named promotion
= shard `day:hash(relation-id) mod k`).

### 4.3 Envelope `:request/sent-at-ms` (A4)
- `envelope` fn (`:705-718`): add top-level `:request/sent-at-ms` = `(:sent-at-ms opts)`
  with fallback `(:asserted-at-ms opts)`. This stays a **top-level plain-map key** on
  the envelope (never inside a record; the partitioner-read key `:relation/routing-key`
  is untouched — cycle-1 F1 rule preserved).
- New accessor `(defn relreq-sent-at-ms [request] (:request/sent-at-ms request))`
  beside the other accessors (`:200-206`).
- `assert-request`/`retract-request` (`:720-734`) pass `:sent-at-ms` through opts
  (they already spread opts into `envelope`).

### 4.4 Arrival/claim derivation (pure, inside `relation-outcome`; NEVER the wall clock)
In the accepted branch of `relation-outcome` (`:365-395`), compute and add to the
outcome map:
```clojure
claimed-at-ms = ts                                     ; already (:asserted-at-ms payload), :336
arrival-at-ms = (long (or (relreq-sent-at-ms request)  ; envelope sent-at
                          (:asserted-at-ms payload) 0)) ; fallback; NEVER (core/now-ms)
bucket        = (format "%08d" (quot arrival-at-ms 86400000))   ; zero-padded UTC day index
activity-ok   = (oc/fixed-width-order-key arrival-at-ms event-id) ; event-id already at :373
activity-row  = (->RelationActivityRow activity-ok bucket relation-id kind
                  (:target-kind from) (:target-id from) (:target-kind to) (:target-id to)
                  asserter-id asserter-typ envelope-actor-id new-status prev-status
                  event-id claimed-at-ms arrival-at-ms)
```
**Trap 4b (cited in a code comment):** the OC kernel stamps `decided-at-ms
(core/now-ms)` at `object_container.clj:428,448,467` — a crash-replayed microbatch
re-stamps differently and double-buckets. This topology reads NO clock; `bucket`,
`order-key`, `claimed-at-ms`, `arrival-at-ms` all derive from client-supplied
envelope/payload times, so a replayed batch re-derives byte-identical rows.

`order-key` scope: **unique per accepted transition within its bucket, on the bucket's
task** — `event-id` embeds `relation-id` + the transition's `fixed-width-order-key`
(`event-id-for :106`), so distinct transitions of the same relation on the same day
never collide; `arrival-at-ms` prefix gives arrival ordering within the bucket.

Add outcome accessors `outcome-activity-row`/`outcome-bucket` beside `:399-414`.

### 4.5 Accepted-branch write — the 4th partition hop (after the to-side copy, `:617-621`)
```clojure
;; ... existing to-side copy on hash(to-tk) ends at :621 ...
(outcome-activity-row *outcome :> *activity-row)
(outcome-bucket *outcome :> *bucket)
(activity-order-key *activity-row :> *activity-ok)          ; accessor on the row
(|hash *bucket)                                             ; 4th hop → hash(bucket) task
(local-transform> [(keypath *bucket *activity-ok) (termval *activity-row)]
                  $$relation-activity-by-bucket)
```
Written ONLY in the accepted branch (inside the existing `<<if (outcome-accepted?
*outcome)` at `:579`), so rejected decisions and journal-replayed duplicates (filtered
by `(nil? *prior-decision)` at `:562-563` before the outcome) write ZERO activity rows
(gate 9). Same microbatch = cross-partition exactly-once per attempt as the existing
triple write (trap 1). `termval` = write-only, no read.

### 4.6 R3 query topology `relation-activity` (A8)
```clojure
(<<query-topology topologies "relation-activity" [*bucket-lo *bucket-hi :> *result]
  (activity-bucket-range *bucket-lo *bucket-hi :> *buckets)  ; pure: fixed-width bucket
                                                             ; strings from lo..hi day ints
  (ops/explode *buckets :> *bucket)
  (|hash *bucket)                                            ; per bucket → its task
  (local-select> [(keypath *bucket) MAP-VALS]
                 $$relation-activity-by-bucket {:allow-yield? true} :> *row)
  (|origin)
  (aggs/+vec-agg *row :> *rows)
  (sort-activity-rows *rows :> *result))                     ; pure: order by order-key
```
Read plan: **range over buckets** — each `*bucket` is read on its own `hash(bucket)`
task (one seek + sequential iteration over that day's inner subindexed map,
`{:allow-yield? true}`), fanned per bucket, aggregated at `|origin`. Because a bucket
is one whole day, R3 returns ALL of each covered day's rows; the trail-view feed
wrapper (C2) filters to the exact ms window + orders by `:order`. `*bucket-lo/-hi` are
fixed-width bucket strings (parse to ints to enumerate the inclusive day range —
top-level bucket keys live on different tasks, so a `sorted-map-range` across them is
impossible; the covered days are enumerated and fanned).

### 4.7 New V1 activity reader (validation-only, for gate 9 negative assertions)
`start-relation-runtime!` (`:739-756`) gains `:activity-by-bucket (foreign-pstate ipc
module-name "$$relation-activity-by-bucket")` and a `relation-activity-query`
foreign-query handle; add a V1 reader `read-activity-rows` (foreign-select
`[(keypath bucket) MAP-VALS]`) beside `:792-816`. Gate 9's "adds ZERO" checks read the
PState physically (public R3 would mask a leaked row).

---

## 5. Phase B — trail-view module + wrappers + text projection (gates 1–8, 11–14)

### 5.1 Module declarations (`src/app/server/rama/trail_view.clj`)

**Mirror PStates** — `(mirror-pstate setup $$<local> (get-module-name <src-var>)
"$$<name>")` (idiom: `dogfood/space.clj:1457`; syntax: mirrors.md:88-90). Require
`[app.server.rama.object-container :as oc]` and `[app.server.rama.relation-kernel :as
rk]` for the module vars.

From **object-container-module** (`(get-module-name oc/object-container-module)`;
all `partition-by-object-key`, `hash(object-key)`), verified present at
`object_container.clj:1706-1758`:
`$$containers-by-id`, `$$revisions-by-id`, `$$source-artifacts-by-id`,
`$$source-anchors-by-target`, `$$composition-children-by-parent`,
`$$composition-parent-by-child`, `$$outline-by-document`,
`$$transcript-conversation-projection`, `$$derived-units-by-id`,
`$$unit-graduations-by-id`, `$$transcript-last-message-by-conversation`;
and (default partitioner) `$$source-latest-by-ref` (`hash(source-ref-key)`, `:1703`),
`$$source-ingest-completions-by-ref` (`hash(source-ref-key)`, `:1704`),
`$$transcript-source-lines-by-file` (`hash(file-key)`, `:1756`).

From **object-container-transcript-ops-module**
(`(get-module-name oc/object-container-transcript-ops-module)`; verified
`object_container.clj:2555-2556`, default partitioner):
`$$transcript-file-offsets` (`hash(file-key)`), `$$transcript-runs` (`hash(request-id)`).

**Mirror queries** — `(mirror-query setup *<local> (get-module-name <src-var>)
"<name>")`:
- From relation-kernel-module: `relations-for-targets` (R1, `:628`), `relation-detail`
  (R2, `:662`), `relation-activity` (R3, new A3).
- From object-container-module: `read-common-material-for-source` (`:2494`).

**trail/via resolution queries are consumed at CLIENT level, not mirrored into the
module** (C7 below): `read-latest-source-by-ref` (`:2436`), `read-source-by-ref-version`
(`:2450`) are held as client `foreign-query` handles. Rationale: `trail/via` is a
client wrapper, not a query topology; the style gate (F-2) restricts foreign-SELECT on
PStates, not foreign-invoke-query on public query topologies (queries ARE the public
surface). C4 (`read-relation-detail`) likewise holds R2 as a client handle
(IMPLICIT_SPEC C4).

**NO depots, NO stream/microbatch topologies, NO own PStates, NO
`foreign-append!`/`local-transform>` anywhere in the file** (gate 14).

### 5.2 The three query topologies (dataflow sketches; spike-verdict-honoring)

Architectural split (testability + colocated-I/O separation): each query topology does
ONLY the colocated/mirror **I/O gathering** and returns raw gathered material; the
**pure assembly** (family grouping, verdict fold, caps→omissions, address stamping)
lives in exported pure fns called by the client wrappers. This keeps §4's family
grouping + F-6 + the verdict fold unit-testable without IPC, and keeps the topologies
free of unbounded work. The seam (§7) is preserved: consumers call ONLY the C-wrappers.

**B1 `context-bundle [*targets *opts :> *raw]`** — one roundtrip, batch-first (§4):
```
pre-agg (per target, material fan):
  (ops/explode *targets :> *target-id)
  (bundle-target-class *target-id :> *class)          ; pure: :material | :relation | :unresolved
  <<cond
    :relation  → (invoke-query *relation-detail-mirror *target-id :> *rd)   ; rel:* via R2
    :material  → (oc/extract-object-key *target-id :> *object-key)          ; SPIKE RULE:
                 (|hash$$ $$containers-by-id *object-key)                    ; extracted key,
                 ;; ONE hop; index reused across the OC family (spike part 2):
                 (local-select> [(keypath *target-id)] $$containers-by-id :> *container)
                 (local-select> [(keypath *current-revision-id)] $$revisions-by-id :> *rev)
                 (local-select> [(keypath *target-id)] $$derived-units-by-id :> *unit)
                 (local-select> [(keypath *target-id)] $$unit-graduations-by-id :> *grad)
                 (local-select> [(keypath *target-id) MAP-VALS] $$source-anchors-by-target :> *anchors)
                 (local-select> [(keypath *target-id) (sorted-map-range-from *cur *cap) MAP-VALS]
                                $$composition-children-by-parent {:allow-yield? true} :> *children)
                 (local-select> [(keypath *target-id) MAP-VALS] $$composition-parent-by-child :> *parent)
                 (local-select> [(keypath *doc-id)   (sorted-map-range-from *cur *cap) MAP-VALS]
                                $$outline-by-document {:allow-yield? true} :> *outline)
                 ;; L1 raw for the source: invoke read-common-material-for-source (mirror query)
                 (invoke-query *read-common-material-mirror *source-id *cats *cursors *cap :> *cm)
    :unresolved → (identity nil ...)                    ; → omission :target/unrecognized
  collect per-target material + the DISTINCT object-keys
agg (relations, ONE invocation, batch-first):
  (invoke-query *relations-for-targets-mirror *all-object-keys nil false :> *rel-map)   ; R1
origin:
  emit *raw = {material-by-target, rel-map, rel-details, unresolved}
```
- Mirror family reads: `(|hash$$ $$containers-by-id *object-key)` with the EXTRACTED
  object-key (spike part 2 proved the custom-key-partitioner routing + sibling-index
  reuse). `keypath` uses the FULL id (map key); `|hash$$` uses the object-key (task).
- R1 invoked ONCE with all distinct object-keys (R1 is batch-first, `:628`; returns
  `{object-key [RelationEdgeRow…]}` on `hash(object-key)` per key).
- `read-common-material-for-source` invoked as a mirror query for L1 raw (`stored?`,
  byte-count, offset-unit).

**Pure assembly (client-side, `assemble-bundle`, exported):**
- Per target build `<target-bundle>` L0–L5 (§4 shape). `:offset-unit` from the source
  kind: markdown → `:chars` (`markdown_adapter.clj:273` `(count raw-text)`), transcript
  → `:bytes` (`transcript_adapter.clj:142-152` `:source/byte-offset`) — trap 12 / gate 5.
- **Family grouping (§4, trap 6):** a doc and its blocks share one
  `extract-object-key`, so R1's rows for that object-key cover the whole family. For
  each row, attribute by endpoint id: rows whose `from`/`to` id == the requested id →
  `:this`; sibling-id rows → `:in-family` keyed by that sibling id. **F-6 rule:** a row
  whose from AND to are two DISTINCT siblings (neither the requested id) files ONCE
  under `:in-family`, keyed by its **`from`** endpoint id (v1.1). **E-6:** a unary
  `:none` row (to = `{:target-kind :none :id nil}`) lands in `:this` (its from is the
  requested id); never create an `:in-family` entry keyed by the nil `:none` id. R1's
  per-target dedup by relation-id (`relations-pairs->map :469-494`) already prevents
  doubles.
- **L4 verdicts** = `(current-verdicts …)` fold (C8 below) over the target's
  stance-kind rows (`:confirms/:refutes/:supersedes`).
- **Omissions (§4 exactness rule, trap 7; gate 3):** every cap/truncation/page/
  unresolved-id → an `:omissions` entry with counts and (where resumable) a cursor;
  returned + omitted == fixture totals per layer.
- `:written-by` on each `<edge>` only when envelope-actor-id ≠ asserter (gate 10).
- `last-walked-ms` ALWAYS nil (§9.2 / I-15; renders "walked unknown").

**B2 `recent-activity [*window *opts :> *raw]`** — gathers three branches:
```
;; R3 branch (relation transitions): C2 computes [bucket-lo bucket-hi] from the window
(invoke-query *relation-activity-mirror *bucket-lo *bucket-hi :> *activity-rows)
;; file-updated branch: full cross-partition scan of the ops registry
(|all$$ $$transcript-file-offsets)                              ; mirror-all (mirrors.md:114)
(local-select> [ALL] $$transcript-file-offsets :> *file-offset-row)
;; resolve conversation address: tail row of $$transcript-source-lines-by-file per file-key
;; source-ingested branch: scans of $$source-latest-by-ref / $$source-ingest-completions-by-ref
(|origin) (aggs/+vec-agg … :> …)
emit *raw = {activity-rows, file-offset-rows, source-rows, line-tails}
```
Scans are `|all$$` mirror-all fans aggregated at origin (O(#watched-files + #doc-refs),
small at phase-1 scale). **§6 v1.1 (C2 pure filter/order/merge):** window selection is
ALWAYS arrival-time; `:order :arrival|:claimed` (default `:arrival`) orders WITHIN the
window; entries ALWAYS carry BOTH stamps (`:time/claimed-ms` nil-honest, `:time/arrival-ms`);
a `:claimed`-window *selection* request is REFUSED by C2 (§9.10). Static
`:feed/uncovered` omissions (direct editor `ObjectEditPayload` revisions; thin OC
session metadata) are attached to `:feed/omissions` (I-10; gate 3 checks presence).

**B3 `conversation-trail [*conversation *cursor *limit :> *raw]`** — single partition:
```
(oc/extract-object-key *conversation :> *object-key)     ; "chat:<conversation-id>"
(|hash$$ $$transcript-conversation-projection *object-key)
(local-select> [(keypath *conversation) (sorted-map-range-from *cursor *limit) MAP-VALS]
               $$transcript-conversation-projection {:allow-yield? true} :> *rows)  ; byte-offset order
(local-select> [(keypath *conversation)] $$transcript-last-message-by-conversation :> *last)
(|origin) → emit *raw = {rows, last, next-cursor}
```
Byte-offset order comes from the projection's order-key (§7). Convergent re-import is
order/count-stable (deterministic ids; gate 11).

### 5.3 Client wrappers + pure fns (`app.server.rama.trail-view` — the ONLY product surface)

| Fn | Kind | Notes |
|---|---|---|
| `read-context-bundle [rt targets opts]` | wrapper | invoke B1 → `assemble-bundle` (pure) → stamp `:bundle/rendered-at-ms` (client clock, the ONLY read-path clock, I-17) + `:bundle/address` |
| `read-recent-activity [rt window opts]` | wrapper | compute `[bucket-lo bucket-hi]` from `window` (day ints → fixed-width strings), invoke B2 → `assemble-feed` (pure: filter to exact ms window, order by `:order`, merge branches, attach `:feed/uncovered`) → stamp `:feed/rendered-at-ms` + `:feed/address`; **refuse `:claimed`-window selection** (§9.10) |
| `read-conversation-trail [rt conversation cursor limit]` | wrapper | invoke B3 → assemble page → stamp `:trail/rendered-at-ms` + `:trail/address` |
| `read-relation-detail [rt relation-id]` | wrapper | **delegates to kernel R2** via a client foreign-query handle (§3 law 4; C4) — no trail-view topology |
| `->address [query params]` | pure | `(trail/<query> <params-map>)` one-line EDN literal (trap 8) |
| `resolve-address [rt address]` | wrapper | parse EDN + dispatch to the matching wrapper; result of the same shape over CURRENT truth; NO as-of (§9.1); round-trip law (gate 2) |
| `trail/via` resolution | wrapper | read the spec doc's raw text: `:latest` → `read-latest-source-by-ref` on the spec's source-ref; a `:rev` pin → `read-source-by-ref-version` on the source-ref + source-version-key (§3 v1.1 / F-3). Container-id → source-ref join via the container's `source-id` (residue §10). Shallow-merge `:overrides` at params level, invoke. View writes NOTHING. |
| `current-verdicts [relations]` | **pure fold, exported** | per judged target (a stance row's `to`; all three kinds fold uniformly incl. `:supersedes`, F-4), per asserter, the LATEST **asserted** stance-kind row; **latest = max `status-changed-at-ms`, tiebreak `relation-id`** (F-5 v1.1); retracted stances drop from `:current`; disagreeing asserters BOTH current, badged, unmerged (I-10; gate 6) |
| `render-bundle-text [bundle]` | **pure** | `;; trail-text v0` header + §8 sections/markers, fixed order; reads `rendered-at` FROM the bundle (does not generate it); asserter badge always, `:written-by` badge only when it differs; `walked unknown` printed while nil; ≤ 4,000 chars on the fixture (gate 13) |

The trail-view runtime (`start-trail-view-runtime!`) launches all four modules in
order (§1) and wires the B1/B2/B3 query handles + the client-level R2 / OC-query
handles. `->address`/`current-verdicts`/`render-bundle-text`/`assemble-bundle`/
`assemble-feed` are pure and exported for the test ns.

---

## 6. Read plans (F2 discipline — every key names its partition scope)

- **B1 context-bundle**, per material target: derive `object-key =
  extract-object-key(target-id)`; ONE `|hash$$` to the OC family task
  **`hash(object-key)`** reads container + revision + unit + graduation + anchors +
  children + parent + outline + common-material (index reused across the family, spike
  part 2) = ≤1 container point-read + 1 structure page. Relations: R1 invoked ONCE with
  all distinct object-keys — each object-key's rows come from **`hash(object-key)`**
  (R1 descriptor-gated, 1-seek dominant path, `:632-651`). `rel:*` targets: R2 on
  **`hash(relation-id)`**. Total: one roundtrip (batch-first, §4).
- **B2 recent-activity**: R3 over the arrival buckets covering the window, each on
  **`hash(bucket)`** (one seek + day-iteration per bucket). `$$transcript-file-offsets`
  / `$$transcript-runs`: full cross-partition `|all$$` scans of the whole
  (file-key- / request-id-partitioned) maps, window-filtered. Conversation address:
  tail row of `$$transcript-source-lines-by-file` for a **`hash(file-key)`** file.
  `$$source-latest-by-ref` / `$$source-ingest-completions-by-ref`: scans on
  **`hash(source-ref-key)`**, window-filtered. Cost O(#watched-files + #doc-refs +
  window-activity); single roundtrip (§6 read plan).
- **B3 conversation-trail**: 1 subindexed page on
  **`hash(extract-object-key(conversation))`** = `hash("chat:<id>")`; last-message on
  the same object-key task; byte-offset order.
- **R3 relation-activity**: per bucket in `[lo hi]`, one seek + sequential iteration on
  **`hash(bucket)`**, `{:allow-yield? true}`, aggregated at `|origin`.
- **C4 read-relation-detail / gate-7 history**: R2 on **`hash(relation-id)`**.

---

## 7. Test plan (`test/app/server/rama/trail_view_test.clj` + kernel test additions)

### 7.1 Shared fixture (§11) — ingested through the OC path
Build via the cited OC-test idioms (do NOT hand-roll payloads):
- **1 markdown doc, ≥3 blocks:** `markdown-adapter/source-ingest-request raw-text
  source-ref opts` → `append-and-await!` (idiom: `object_container_test.clj:107-116`,
  the `ingest!` helper). Raw text sized so the text projection fits ≤4,000 chars.
- **1 transcript conversation, ≥4 messages + ≥1 tool call:**
  `transcript-observation conversation-id line-idx message-uuid role content-vec`
  (`object_container_test.clj:125-143`) + `transcript-adapter/transcript-observation-import-request
  obs {:request/id … :time-ms …}` (`:624`) → `append-and-await!`. Content vec carries
  `{:type "text" …}`, `{:type "tool_use" …}`, `{:type "tool_result" …}` (`:619-639`);
  thread messages via `:transcript/previous-message-container-id` (`:640-641`). The
  `:chat-conversation` container id (from the request payload, `:650-654`) is the
  conversation address; conversation + messages + tool-call/result colocate on
  `extract-object-key` = `"chat:<id>"` (verified `object_container.clj:262-334`).
- **Relations** (via `rk/assert-request` + `append-relation-request!`): `based-on`
  (cross-doc), `produced` (conversation→doc), `dead-end` (unary on a block).
- **Verdicts:** `refutes` + `confirms` on the SAME block, from two asserters (`sid`
  human, `llm:…`) → disagreement (gate 6); a `supersedes` row (gate 7 fold). One
  `refutes` carries an `evidence-anchor-id` that joins to a `SourceAnchorRow` (gate 5).
- **One back-dated relation:** `:asserted-at-ms` = 60 days ago, `:sent-at-ms` = now →
  physically in TODAY's arrival bucket, claimed 60 days old (gate 4).

### 7.2 Harness structure (honors implementation-quirks invariants)
- **Launch order** (module-dependency enforcement): object-container-module →
  object-container-transcript-ops-module → relation-kernel-module → trail-view-module,
  on ONE `create-ipc`. `start-trail-view-runtime!` encapsulates this.
- **Deterministic microbatch barrier, never polling:** reuse the relation-kernel
  submit!/drain! counting closure (`relation_kernel_test.clj:68-87`,
  `wait-for-microbatch-processed-count`); every relation `submit!` carries a present
  routing key so `submit! == +1` processed. OC/transcript ingests use
  `append-and-await!` (decision await, `object_container_test.clj:63-69`) since OC is a
  STREAM topology (`:append-ack` waits). The barrier proves gate-9 negatives (a
  replayed/rejected request is fully consumed before the physical read).
- **Physical V1 reads for negative assertions:** gate 9 ("adds ZERO") reads
  `$$relation-activity-by-bucket` via the new `read-activity-rows` V1 reader (§4.7) —
  public R3 dedups/gates and would MASK a leaked row. Gate 14 (no writes) asserts via
  module inspection / foreign-depot lookup failing on trail-view-module.
- **Minimize IPC launches:** ONE trail-view deftest for the read gates (all
  self-contained on disjoint fixture ids in one launch); the kernel-side custody/
  activity gates ride the existing kernel launches (dt1/dt2) or one added kernel
  deftest. A second launch only if a named shared-mutable-state interference demands it
  (none foreseen — no pause/resume in trail-view tests).
- **Injectable task counts:** `start-trail-view-runtime!` takes per-module launch opts
  (default `{:tasks (rand-nth [2 4 8]) :threads 2}` per module, like
  `relation_kernel_test.clj:62`) so partition sweeps are reproducible.
- **NUL only as the `backslash-u0000` string escape** in any source literal (never a raw byte).

### 7.3 Gate → deftest coverage
| Gate | Covered by |
|---|---|
| 1 bundle completeness (6 layers; rows == R1; `:in-family` block verdicts) | trail-view dt: `context-bundle` on doc + conversation; cross-check each edge vs `rk/read-relations-for-targets` |
| 2 address round-trip | trail-view dt: `->address` → `resolve-address` == original (modulo `rendered-at-ms`) for bundle/feed/trail |
| 3 omissions honesty | trail-view dt: `:caps` below fixture sizes → returned + omitted == totals per layer; assert `:feed/uncovered` present |
| 4 two clocks (v1.1) | trail-view dt: back-dated relation in TODAY arrival window w/ claimed-ms 60d old; `:order :claimed` places it; 60-days-ago arrival window excludes it; `:claimed`-window selection request throws/refused; both stamps on every entry |
| 5 evidence anchors resolve | trail-view dt: `refutes` anchor joins a `SourceAnchorRow`; one md anchor (`:chars`) + one transcript anchor (`:bytes`) resolve to expected span text |
| 6 disagreement preserved | trail-view dt: both asserters' stances `:current`, badged, unmerged |
| 7 supersession fold | trail-view dt: same asserter retracts `confirms`, asserts `refutes` → `:current` shows only `refutes`; retracted row visible with `include-retracted?`; R2 history reachable; a `supersedes` row folds keyed on its `to` |
| 8 attestation staleness | trail-view dt: re-assert bumps `last-attested-ms`; never-attested target nil; every bundle `last-walked-ms` nil + text prints `walked unknown` |
| 9 activity exactness | **kernel dt** (physical V1): each accepted transition (assert/re-assert/retract) → exactly one activity row; duplicate (same idem key) + rejected → zero; `arrival-at-ms == client sent-at-ms` and `bucket` derived from sent-at (proves no wall clock) |
| 10 custody | **kernel dt**: envelope actor ≠ payload asserter → both on decision + event + edge rows; trail-view dt: bundle edge shows `:written-by`; when equal, omitted |
| 11 conversation trail | trail-view dt: byte-offset order; page cursor; convergent re-import leaves order + count unchanged |
| 12 view-spec resolution | trail-view dt: `trail/via` `:latest` reflects the current spec source version; a pinned `:rev` keeps resolving to old params (see §10 residue on how a spec is "revised") |
| 13 text projection (style) | trail-view dt: fixture bundle renders all §8 markers + address header + asserter badges + ≤4,000 chars |
| 14 read-only by construction | trail-view dt: module declares no depots/ETL (foreign-depot lookup fails); source contains no `foreign-append!`/`local-transform>` |
| 15 registry regression | **kernel dt**: unregistered kind still rejected; the 3 new kinds accepted; descriptor bound ≤ 2×10 |
| 16 kernel regression | re-run the existing 2 tests / 165 assertions green against the amended records |

---

## 8. Compile/run commands (for the tests + gate phases)
- Load-check: `clojure -M:test -e "(require 'app.server.rama.trail-view)"` and
  `"(require 'app.server.rama.trail-view-test)"`.
- Kernel regression (gate 16): `clojure -M:test -e "(require 'clojure.test
  'app.server.rama.relation-kernel-test) (clojure.test/run-tests
  'app.server.rama.relation-kernel-test)"`.
- Full trail-view suite: same idiom on `app.server.rama.trail-view-test`.
- `-Xss16m` comes from the `:test` alias (Rama topology-graph compilation overflows
  the default stack; `deps.edn` `:test`).

---

## 9. Risks the validation session must scenario-trace hardest

1. **The 4th-hop activity write under replay (gate 9).** Trace: microbatch retry
   re-processes the same records; because `bucket`/`order-key`/`arrival-at-ms` derive
   from client stamps (never `now-ms`), the re-derived rows are byte-identical and the
   `(keypath bucket order-key)` write is idempotent per attempt. Falsify by looking for
   ANY wall-clock read on the accepted path (there must be none — contrast
   `object_container.clj:428,448,467`). Also trace: a same-idem-key duplicate is
   dropped at `:562-563` BEFORE the outcome, so it never reaches the 4th hop → zero
   extra activity rows.
2. **Family grouping edge cases (§4, F-6, E-6, E-7).** Trace: (a) a verdict on block
   `du:<key>:…` when the DOC was requested → `:in-family` keyed by the block id; (b) a
   row between two DISTINCT siblings → ONE `:in-family` entry keyed by `from` (F-6);
   (c) a unary `:none` dead-end → `:this`, never an `:in-family` nil key (E-6); (d) R1
   dedup by relation-id prevents the o:/i: copies doubling.
3. **Mirror async-boundary consistency within one bundle** (mirrors.md:70,126;
   module-dependencies.md:131-141). Each mirror `local-select>` is an async boundary,
   so B1's sequential OC family reads may see snapshots at slightly different instants.
   This is acceptable under the contract's honesty model (the bundle resolves to NOW,
   not an atomic instant; `:bundle/rendered-at-ms` is the drift detector; §7 "wall-clock
   latency is not a gate; shape and honesty are"). Trace: no bundle field claims
   cross-PState atomicity; a mid-read OC append can only ADD material (append-mostly),
   never corrupt a returned row. Flag if any assembly step assumes two mirror reads are
   the same instant.
4. **`tc:`/`chat:` id-prefix routing through `extract-object-key`** (E-5, trap 13).
   Trace: `oc:chat-conversation:chat:ab…`, `oc:chat-message:chat:ab…`,
   `oc:tool-call:…`, `oc:tool-result:…` ALL extract to `"chat:<id>"`
   (`object_container.clj:262-334`, `leading-object-key` keeps the `chat:` segment), so
   a conversation family colocates. `tc:*` / `src:tr:` / `imp:*` ids are OUT of scope
   (§2): B1's `bundle-target-class` must accept ONLY §4's id set (`oc:*`, `du:`, `src:`,
   `rel:*`, git shas) and route everything else to `:bundle/omissions`
   `:target/unrecognized` (never let a `tc:*` id reach a mirror read). Git shas render
   `:unresolved` until WP2.
5. **Complex-query-as-mirror-query** (§0 residual): the spike proved a simple mirror
   query; B1 invokes R1 (explode+fan+agg). If Phase B hits an edge, the pre-specified
   fallback (C1 composes R1 client-side, unchanged signatures, §7) applies — this is a
   verification duty with a ready-made out, not a redesign.

---

## 10. Open items (recorded, not silently resolved)

- **F-3 residue — container-id → source-ref join + what "revising a spec" means
  (gate 12).** CONTRACT §3 (v1.1) names `read-latest-source-by-ref` for `:latest` and
  `read-source-by-ref-version` for a `:rev` pin — BOTH take a `source-ref`, but a
  view-spec address is a container id `oc:doc:<object-key>`. The buildable path
  (implementer-fixable, F-3): join container → `source-id` (`ObjectContainerRow`,
  `:99-102`) → `SourceArtifactRow.source-ref` (`:82-84`), then invoke the source-ref
  query. IMPLICIT_SPEC C7 mentions `read-current-revision` for `:latest`; the CONTRACT
  text (`read-latest-source-by-ref`) governs (contract > derived spec). Consequence for
  gate 12: a spec is "revised" by RE-INGESTING the spec file (new `SourceVersionRow`),
  not by an `ObjectEditPayload` revision — the gate-12 fixture must revise the spec via
  re-ingest so `:latest` moves and a pinned source-version-key stays put. This is a
  discrepancy between two documents, NOT a physical two-reading impossibility, so it is
  recorded here and the contract text is followed; if the implementer finds re-ingest
  cannot produce a stable pinnable source-version-key for a spec doc, THAT is a
  stop-clause escalation.
- **F-7 (platform note, non-blocking):** B1/B3 start by partitioning to
  `hash(object-key)` on MIRROR PStates, which cannot be a *leading* partitioner
  (13-query-topologies.md:196) — they start at a random task and hop. Latency only; §7
  says latency is not a gate.
- **R3 `[bucket-lo bucket-hi]` type:** the signature uses fixed-width bucket STRINGS
  (IMPLICIT_SPEC A8); R3 parses them to integer day indices to enumerate the covered
  days (top-level bucket keys live on different `hash(bucket)` tasks, so the range is
  enumerated + fanned, not a `sorted-map-range`). C2 computes the day ints from the
  arrival window. Stated so the validation session checks the lo/hi convention is
  consistent across A7 write, R3, and C2.

---

## 11. Phase sequencing (one phase per FRESH session; §12 handoff)
1. **A1 custody** (`relation_kernel.clj` records + `relation-outcome` + test arity;
   gates 10, 16) — FIRST, before daily use.
2. **A2 stance kinds** (registry line; gate 15).
3. **A3 activity + R3** (`RelationActivityRow`, `$$relation-activity-by-bucket`,
   `:request/sent-at-ms`, 4th-hop write, R3, V1 activity reader; gates 9, 16).
4. **B trail-view module + wrappers + text projection + fixtures** (gates 1–8, 11–14).
5. Tests phase writes + compile-checks; run-to-green is its own phase; then the Fable
   gate (traps 3, 4b, 10, 12 spot-checked; falsification pass).

File allowlist (baton STANDING): NEW `src/app/server/rama/trail_view.clj`,
`test/app/server/rama/trail_view_test.clj` (+ fixture resources); AMENDED
`relation_kernel.clj` (ONLY §5 items) + its test ns (additive + arity-forced). Nothing
else; object-container module untouched.
