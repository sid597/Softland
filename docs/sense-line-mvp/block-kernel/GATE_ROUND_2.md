# Gate Review — Round 2 · block-kernel (sense-block-v0)

Fresh-context, default-fail package gate. Independent of the fix session; every
claim below reproduced against the working tree (uncommitted diff on
`docs/current-mental-model-local`). Round-1 gate artifact left untouched.

## Verdict: **PASS**

The four Round-1 blockers (F1–F4) are each independently reproduced as fixed. The
three suites are green, and the real 7c80ce2a definition-of-done receipt runs
clean with 0 import rejections (independently confirmed at the decision-status
level, not just by grep). No surviving contract-level blocker; no named
acceptance gate (G1–G13) fails. Three non-blocking findings are recorded below —
the top one (F3 surfaceless-river miscount) should be closed before package
RETRO, but it breaks no gate and does not corrupt the river deliverable.

## Receipts (independently reproduced)

**Three suites, one JVM** (`clojure -M:test`, block-distiller + object-container +
relation-kernel):

```
Ran 27 tests containing 1244 assertions.
0 failures, 0 errors.
:FAIL 0 :ERROR 0 :PASS 1244 :TESTS 27
```

Matches the implementer's receipt exactly.

**Real 7c80ce2a receipt** (`real-example-river-page-receipt!`, page limit 32):

- harvest: `:observations-appended 759`, `:parse-error-count 0`, `:containers-created-count 1676`, `:status :complete`
- distillation: `:river 244`, `:debris 402`, `:edge-count 44`
- read-plan: `{:projection-range-seeks 1 :projection-rows-iterated 1417 :events-read 8
  :input-source-point-seeks 8 :surfaces-read 8 :common-material-range-seeks 8
  :unit-reads 32 :unit-point-seeks 64 :seek-count 81 :seek-bound 129 :limit 32
  :river-events-total 244 :river-events-rendered 8 :surfaces-rendered 8
  :blocks-returned 32 :truncated? true :page-complete? false}`
- seek-count arithmetic verified: `1 + 8 + 8 + (2*32) = 81 ≤ 129 = 1 + 4*32`.
- **0 import rejections — independently confirmed** by re-running and inspecting
  every decision status (not the pprint, which omits them):
  `RIVER-DECISIONS 244 NON-ACCEPTED 0`, `DEBRIS-DECISIONS 399 NON-ACCEPTED 0`,
  distinct statuses `(:accepted)` for both. No `:rejected` string anywhere; no
  runtime stacktrace; `RECEIPT-DONE :ok`.

Note the 244+399 = 643 durable class rows vs 646 conversation events — see
Finding 1.

## Per-fix falsification

### F1 / G12 — measured, bounded, truncation-signalled seek plan

CONTRACT §8 G12 was amended in place (2026-07-10, Sid ruled Option A): forbid
*conversation-scaled* fan-out; accept a hard page cap + a page-size-bounded,
truncation-signalled measured seek plan; the denormalized
`read-conversation-sources` query is the §10 scale extension (not built). Read
cold; the amendment is present and self-consistent with §12/g + PHASE_0 §2/g.

Attacks:
- **Is `:seek-bound (+ 1 (* 4 limit))` a real upper bound?** Traced every
  dimension: projection range = 1; `events-read ≤ limit` (`river-rows` is
  `take limit`, reduce `reduced`s on caps); `surfaces-read ≤ limit` (guard
  `(>= surfaces-read limit)` in both reduces); `unit-reads` bounded because the
  per-surface bundle is limited to `remaining = limit - blocks-so-far`, blocks
  never exceed `limit` (`take remaining`), and — critically — the F2 pre-filter
  drops foreign refs *before* `read-unit`, so every read ref renders a block:
  `unit-reads == blocks ≤ limit`. `2*unit-reads` is honest: `read-unit`
  (object_container.clj:2487) does exactly two point-reads for a found unit
  (`$$derived-units-by-id` + `$$unit-graduations-by-id`). So
  `seek-count ≤ 1 + limit + limit + 2*limit = 1 + 4*limit`. Real receipt: 81 ≤ 129.
- **Foreign-distiller units on a river surface (multi-stratum) inflating the
  plan?** They cannot be read: `render-river-source` filters refs by
  `(str/includes? target-id ":sense-block-v0:")` before any `read-unit`. A
  foreign stratum is rejected with zero point-reads. Verified `unit-reads (32)
  == blocks-returned (32)` on the real file — no wasted point-reads.

Outcome: **G12 (as amended) satisfied.** The plan is recorded, every dimension
is a function of page size, filtered strata do not inflate it, and short pages
are flagged. F1 resolved.

### F2 — pre-filter + truncation honesty

`render-river-source` / `river-page`, block_distiller.clj:1263–1400.

Attacks:
- **`:truncated?` separates complete from capped in both directions?** Yes.
  `truncated? = (or (> total-river-events events-read) (>= surfaces-read limit)
  (>= (count blocks) limit))`. Fixture full page (5 river, limit 64): events-read
  5 = total, caps unhit → `false` (test asserts). Capped (limit 3): total 5 >
  events-read → `true` (test asserts). Real (limit 32): total 244 > events-read 8
  → `true`. All three reproduced.
- **Silent under-fill from empty tool-result surfaces?** No: `river-page`
  removes blank parts (`remove #(str/blank? …)`) before the part loop, so an
  empty tool_result never consumes surface budget. A *non-empty* tool_result
  surface (has text, zero minted units per §4.3) does consume one surface with
  zero blocks — but that can only trip the `surfaces-read >= limit` ceiling,
  which sets `:truncated? true`. Flagged, not silent.
- **Does the filter DROP a legitimate ref or ADMIT a foreign one?** Legit
  sense-block-v0 ids are `du:<object-key>:sense-block-v0:<block-path>`;
  block-paths are numeric/`refine:`-prefixed and object-keys are `chat:<hex>` —
  the only `:sense-block-v0:` occurrence is the distiller segment, so no legit
  ref is dropped. A foreign id would need the colon-delimited substring
  `:sense-block-v0:`; adjacent-name distillers (`x-sense-block-v0`,
  `sense-block-v0-ext`) do not contain it. No false admit reachable via the mint
  path (distiller-id + source-id fields are set consistently in `part-rows`).

Outcome: **F2 resolved** for the single-stratum v0 deliverable. One multi-stratum
under-fill edge remains (Finding 3), out of v0 scope.

### F3 — durable versioned class ledger + import relaxation

object_container.clj:723,779 (relaxation), block_distiller.clj:792–819
(`debris-import-request`), :946–955 (driver else branch).

Attacks:
- **Are key/fingerprint/anchor/native checks still enforced for a
  projection-hint-only payload?** Traced `import-request-validation-errors`
  cold. The relaxation changes only one branch: `(empty? source-rows)` →
  `(and (empty? source-rows) (empty? projection-hint-rows))`. Every other
  cond-> branch is unchanged — request-type, partition/import/object/idempotency
  key presence, material fingerprint, `authorized-request?`, source-hash
  mismatch, missing-current-revisions, duplicate native claims, and all three
  anchor checks still fire. For a hint-only payload the anchor/native/revision
  checks are vacuously satisfied (empty sets); nothing malformed is admitted. An
  import *with* sources is provably unaffected (`(empty? source-rows)` already
  false ⇒ the `and` is false either way). A truly empty payload (no sources, no
  hints) is still rejected.
- **Durable / versioned / queryable / distinguishable from never-classified?**
  Yes on all four for TRUE debris: materialized as a
  `:transcript-conversation-projection` row, `entry-kind :debris`,
  `content-preview = "river-debris-v0/<reason>"` (carries the classifier
  version + reason), queryable via `read-transcript-conversation-projection`. A
  never-classified event has no `sb:` row at all → distinguishable. Fixture test
  asserts all six; real run materialized 399 with all decisions `:accepted`.
- **Leak into `river-ledger-row?`?** No — `river-ledger-row?` requires
  `(= :river (:entry-kind row))`; debris rows are `:debris`. `river-page`'s
  message filter is `:message`; debris rows are neither. And the driver's F3
  input read filters `:message`, so `sb:` rows are never re-ingested.
- **Idempotence / second stratum?** `import-key = imp:tr:<ok>:sb:<sha256(uuid)>`
  is deterministic per event; a re-run dedups in the OC journal → no new rows.
  Each event is river XOR debris, so river and debris `sb:` order-keys are
  disjoint (one row per order). Suite green on re-runs.
- **Surfaceless RIVER event?** Mis-handled — see Finding 1. `debris-import-request`
  guards on `(= :debris …)`, so a river event with no surface falls to the bare
  `(update acc :debris inc)` branch: counted as debris, **no durable class row**.

Outcome: **F3 resolved for its charter** (durable debris class). One
pre-existing edge left open (Finding 1).

### F4 — (surface, span) identity resolution in `refine!`

block_distiller.clj:1001–1128, new `resolve-existing-unit-at-span` + reuse path
+ new `f4-refine-resolves-existing-identity` test.

Attacks:
- **Coincident-span case (whole [0,101] → sub [0,34] already occupied):**
  Resolves to the incumbent free-cut sub-block's id (`:resolved? true`), mints
  nothing (`:decision nil`), asserts one `:refines` edge from the resolved id.
  Reproduced by the F4 test (finer-id == sub-id; would-be `refine:` id absent
  from `$$derived-units-by-id`; unit count on the surface unchanged).
- **Genuinely-new sub-span still mints?** Yes — `existing-uid` nil ⇒
  `reuse? false` ⇒ the mint block (identical to pre-F4, just moved inside
  `when-not reuse?`) runs; `finer-uid = (derived-unit-id …)` as before. The G7
  re-refine test (now taking the resolve-reuse path on the second call) is green
  and byte-identical.
- **Reject sub-span == coarse span?** Yes — `resolve-existing-unit-at-span`
  returns the coarse id, and the `(= existing-uid coarse-unit-id)` guard throws
  before minting. (The proper-child bounds check alone would let `[0,101]`
  through, since it only requires `start<end`; the identity guard is what
  catches it.)
- **`:refines` edge points at the RESOLVED id, not a phantom `refine:` id?**
  `from-ref = (rk/->target-ref :block finer-uid)` with `finer-uid = existing-uid`
  on reuse. Test asserts `(:target-id (:from row)) == sub-id`.
- **Multi-stratum surface — only same-distiller units resolved?**
  `resolve-existing-unit-at-span` filters candidates by the `:sense-block-v0:`
  substring, so a foreign-distiller unit at the same span is not resolved to
  (correct — R3 strata are id-disjoint identity spaces).

Outcome: **F4 resolved.**

## Inherited P0–P4 regression check (changed files)

- OC import topology given F3: hint-only imports validate, materialize, and
  complete (`import-completion-row` tolerates `(:source-id (first []))` = nil
  benignly; `source-ingest-completion-row` only runs per source-version). No
  object-container-test regression (suite green). The additive relaxation is the
  only OC change (`git diff` = 6 lines, T13-compliant additive touch).
- Driver given the debris additions: `distill-conversation!`'s new
  `:debris-decisions` accumulator is additive; river path unchanged. Green.
- relation_kernel.clj (`:grounds :assembled-from :refines`, T11) is already
  committed and clean; relation-kernel-test green. The gate brief's "uncommitted
  relation_kernel.clj" is stale.

## New findings

**Finding 1 — [MEDIUM, non-blocking] Surfaceless river events miscounted as
debris + left without a durable class row.**
`block_distiller.clj:946–955` (the bare `(update acc :debris inc)` else branch) +
`debris-import-request` guarding on `(= :debris …)`. On the real 7c80ce2a file, 3
events are river-class (`user` / reason `:material`) whose single `tool_result`
part has EMPTY text (0 blocks). `event-import-request` returns nil (no surface)
AND `debris-import-request` returns nil (not debris), so they take the count-only
branch: incremented into `:debris` (summary `:debris 402` overcounts; `:river`
undercounts by 3) and given no durable class row. Net: the durable ledger holds
244 river + 399 debris = 643 rows for 646 events; 3 events are unpersisted.
The driver invariant "summary `:debris` == durable debris ledger count" holds on
the fixture (no such events) but breaks on real data — the fixture gate cannot
catch it. Impact is benign for the deliverable (empty tool_results carry no
markable material and never enter the river page), but it is a real
count/honesty inconsistency and a soft §3.1 shortfall (the class exists
in-memory but is not retained). Verified pure (no cluster): the 3 are all
`{:part-types [:tool-result] :n-blocks 0 :part-texts []}` — genuinely empty, not
dropped content. Recommend: emit a durable `:river` (or a dedicated
`:river-no-material`) class hint for surfaceless river events and count them as
river, before RETRO.

**Finding 2 — [LOW, acknowledged] No river-page cursor; hard cap 64.**
`river-page` always starts from the first river event; `max-river-page-size 64`.
A conversation with >64 renderable river blocks (real: 244 river events) can
never be paged past page 1 — `:truncated?` stays `true` with no advance
mechanism. Explicitly deferred to CONTRACT §10 (the dedicated conversation-page
query). DoD is "the first river page," so this is within v0 scope; noting it so
it is not mistaken for a working paginator.

**Finding 3 — [LOW, out of v0 scope] Multi-stratum under-fill.**
`render-river-source` limits the CommonMaterialBundle to `remaining` *total*
derived-units (all strata) then filters to sense-block-v0. If a second distiller
shares a surface and its units sort ahead by order-key within that window,
legitimate sense-block-v0 blocks beyond the window are dropped and — if no cap is
otherwise hit — `:truncated?` may stay `false` (silent). Only reachable with a
coexisting foreign stratum on the same surface AND `remaining` < that surface's
sense-block-v0 unit count; v0 has a single distiller, and river-page renders one
named stratum, so it is not reachable in the delivered system. If G5 strata are
ever rendered together, the bundle limit must account for filtered-out foreign
refs.

## Architecture note

The adapter shape holds: the changed surface is small and additive. The one OC
edit is a 6-line, precedent-cited relaxation of a single validation branch (T13
compliant). The river-page read plan is the right instrument for G12 — it makes
the seek cost auditable and self-checking (the test recomputes `seek-count` from
its components). The F4 resolve path correctly relocates SPEC §6.1 identity to
the (surface, span) key rather than the id-minting convention, which is the
right seam. The recurring theme in all three findings is the same v0 boundary:
single stratum, first page only — each finding lives just past that boundary.

## Open doubts

- Finding 1's fix should also decide whether the receipt's headline `:river` /
  `:debris` numbers (the dual-benchmark hands them to Sid ∥ Fable) should read
  from the durable ledger rather than the driver counters, so the two can never
  diverge again.
- `read-unit`'s two-point-read model (the `* 2` in `:unit-point-seeks`) is
  correct for a *found* unit; a not-found unit is one read. river-page only reads
  ids it just enumerated, so not-found is not expected — but the seek bound would
  be an over-estimate, not an under-estimate, if it happened. Safe direction.

## Next gate

None required to accept F1–F4. Before package close/RETRO, land Finding 1
(durable class for surfaceless river events + count reconciliation) and re-run
the fixture + real receipt; Findings 2–3 are CONTRACT §10 warm refusals, not
gate items. This artifact does not touch the Round-1 gate record.
