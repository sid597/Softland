# P3 DIFF Falsification — QC layer 4 (fresh context, block-kernel)

**Reviewer:** fresh-context adversarial pass (Opus 4.8, max effort). Did NOT write
P3. Default-fail: every "OK" below is traced to file:line against the actual code +
binding docs, not to prose (the author's `P3_FALSIFICATION.md` was read ONLY at the
end, to reconcile — see §Reconciliation).

**VERDICT: no CONFIRMED blockers. The P3a+P3b diff is functionally correct and
conformant to SPEC §11.3/§4.3/§9/§4.6/§5.1 + CONTRACT §8/§12. 1 PLAUSIBLE
robustness gap (non-blocking, not deterministically reachable) + 3 latent NOTEs
(2 pre-P3, 1 cosmetic). P4 is NOT gated.**

Independent suite re-run (one JVM, this session) — reproduces the author's counts
EXACTLY:

- `block_distiller_test` — **15 tests / 740 assertions / 0 fail / 0 err**
- `relation_kernel_test` — **2 tests / 222 assertions / 0 fail / 0 err**
- `object_container_test` — **6 tests / 149 assertions / 0 fail / 0 err**

Diff surface confirmed from the working tree (all uncommitted, branch
`docs/current-mental-model-local`):
`relation_kernel.clj` = pure additive `relation-kinds` edit (both packages' kinds
coexist; no removal — `git diff` verified); `block_distiller.clj` §H (pure edge
specs) + §J (edge driver) + `event-ctx` + refactored `distill-conversation!` /
`start-`/`close-distiller-runtime!`; `block_distiller_test.clj` +3 deftests;
`fixture.jsonl` `a-1` +2 tool_use / `u-tr` +1 tool_result; `golden.edn` regen.

---

## Architecture

The block distiller adds **no Rama surface** — no module, depot, PState, or
topology (CONTRACT §2 adapter-shape dividend). P3b is a **foreign client** to two
existing kernels:

1. `mechanical-edge-plan` (pure, §H) indexes tool_use/tool_result PARTS by
   `tool-use-id` and emits `produced`/`grounds` edge specs — write-tool →
   `:produced`, read-tool → `:grounds`, else no floor edge (`tool-edge-kind`
   :434-441).
2. For **paired** edges the driver demand-mints a coarse `:tool-result-span` block
   via an OC import (`coarse-block-import` :813-833; Sid's N3 option A) as the
   concrete `:block` endpoint.
3. **Unpaired** write/read tool_use → a `hole-endpoint-id` (:474-480)
   `du:<ok>:sense-block-v0:pending-result:<tuid>` that is deterministically never
   minted — G9's dangling target is a *queryable* absence.

Two idempotency surfaces carry retry-safety: the OC import-key (dedups the coarse
block) and the RK relation-scoped idempotency key (dedups the edge). Both are
deterministic from the transcript, so a replay writes nothing.

**rama-pitfalls verdict (P3b foreign-append surface):**

| # | section | result |
|---|---|---|
| 1 | EVENT BOUNDARY | N/A — driver appends to an existing depot; RK's own triple-write atomicity is unchanged, gate-passed pre-P3 |
| 2 | SIDE-EFFECT RETRY | PASS — the only "side effects" are depot appends (OC import, RK assert); both are idempotent by deterministic key. No process/HTTP/file effect. |
| 3 | PSTATE OWNERSHIP | PASS — driver writes NO PState; OC/RK topologies own their PStates; driver is a foreign client only |
| 5 | ID IDEMPOTENCE | PASS — `relation-id-for`/`edge-idempotency-key`/`import-key` all sha over source ids; no `random-uuid` |
| 6 | ACK LEVEL | PASS — RK edges `:append-ack` (:928 default) + test-side processed-count barrier; OC coarse import awaited via decision poll |
| 7 | STREAM vs MICROBATCH | PASS — RK is microbatch (exactly-once triple write); barrier is processed-count, never `:append-ack`-implies-visible |
| 10 | HASH EXTRACTOR | PASS — RK depot `(hash-by :relation/routing-key)`, routing-key = relation-id (always `rel:…`, never nil) |

OVERALL: **READY** — the small foreign-client surface is why.

---

## Hunt by CLASS (each CONFIRMED / PLAUSIBLE / REFUTED, line-cited)

### 1. Coarse-block mint idempotence — **REFUTED (defended)**
Traced the OC dedup, not the test. `coarse-block-import` (:813-833) builds a
**distinct** `endpoint-uuid = "<u-tr-key>:tool-result-span:<part-path>"` → a
distinct deterministic `import-key = imp:sense-block:<ok>:sha256(endpoint-uuid)`
(:58-62), separate from P1's order-8 event import-key. OC dedup is import-key →
`ImportCompletionRow.material-fingerprint` (`material-fingerprint-conflict?`
oc:487-491; `import-material-fingerprint-conflict-error` oc:509-514).
`import-material-fingerprint` (oc:1524) is `sha-256(pr-str …)` over **content-
identity `select-keys` only** — `:source-raw-text`, `:derived-content-hash`,
`:unit-id`, `:block-path`, `:distiller-id` … and deliberately EXCLUDES
`:created-at-ms` / `:created-by` / `:event-id` / `:production-event` — so the
fingerprint is stable across runs. Re-run → same import-key + same fingerprint →
dedup, zero duplicate. The re-declared surface is **byte-identical** to P1's (same
`event-ctx` :715-727, same part, same actor `"tool"`) → idempotent upsert, not
corruption. G4-style re-run gate (test :735-751) confirms `coarse-before ==
coarse-after`.

### 2. The hole endpoint — **REFUTED (defended); deeper than the author's pass**
The false-green risk is *mis-routing* nil, not just collision. Traced
`extract-object-key` (oc:284-339) on BOTH ids:
- real unit `du:chat:<hex>:sense-block-v0:000007:02:000000` → `du:` branch →
  `leading-object-key("chat:<hex>:…")` (oc:267-276) → `chat:` branch →
  `split #":" 3` → `["chat" "<hex>" "sense-block-v0:…"]` → **`chat:<hex>`**.
- hole `du:chat:<hex>:sense-block-v0:pending-result:tu-3` → same `du:` →
  `leading-object-key("chat:<hex>:sense-block-v0:pending-result:tu-3")` →
  `split #":" 3` → `["chat" "<hex>" "sense-block-v0:pending-result:tu-3"]` →
  **`chat:<hex>`**.

Identical object-key ⇒ identical partition ⇒ the G9 read (`read-unit-physical`
test:371-375, `foreign-select-one [(keypath hole-id)] $$derived-units-by-id`) hits
the **same task** a real unit occupies. So `nil` (test:724) is a **true absence**,
not a mis-route. Collision is impossible: `pending-result:…` can never match the
all-digits `%06d:%02d:%06d` block-path tail (`block-path` :44-48). Matches SPEC §9
holes-first-class.

### 3. `event-ctx` refactor — **REFUTED (defended by construction)**
`event-ctx` (:715-727) is the SINGLE source of `event-id` / `document-container-id`
/ `created-at-ms`, consumed by both the P1 surface path (`event-import-request`
:751-774) and the P3b coarse mint (`coarse-block-import` :822). The golden is taken
over the PURE `distill-event` (:516-549), which `event-ctx` does not touch — so the
golden delta is exactly `a-1` +2 tool_use blocks (verified against fixture:
tu-2 Read → `content/3` block-path `000007:03:000000` span [0 32]; tu-3 Write →
`content/4` `000007:04:000000` span [0 54]), and `u-tr` blocks stay `[]`
(tool_result → no distill-event block; the +1 tool_result adds no golden row).
Byte-identity vs the *unshown* pre-refactor cannot be diffed (P1/P2 uncommitted +
overwritten) — but there is no prior stored state (fresh IPC per deftest), so no
cross-version drift is possible, and every downstream id is self-consistent
(G3/G6/G1/G2 physical gates green). Residual: a genuine byte-diff would need the
P2 file; reasoned-consistent, not diff-proven (see Open doubts).

### 4. Microbatch barrier — **REFUTED (defended)**
RK depot `(hash-by :relation/routing-key)` (:629); every envelope sets
`:relation/routing-key = relation-id` (`envelope` :866) which is always `"rel:…"`
(`relation-id-for` :112-119, never blank) ⇒ the topology's `(filter> (present-
string? *relation-id))` (:675) never drops a floor edge ⇒ **submit == +1**. Barrier
`wait-for-microbatch-processed-count` (test:679, :742) counts consumed records
post-commit (microbatch materialization signal — `await-relation` docstring :995
confirms `:append-ack` ⊬ visibility). Cumulative re-run: `(+ edge-count summary1
summary2)` = 3+3 (test:742-744); run-2's 3 records journal-replay
(`(nil? *prior-decision)` false at :686 → no writes) but are still consumed +
counted → reaches 6. No off-by-one; fresh IPC ⇒ no cross-test contamination.

### 5. Edge endpoints / direction vs SPEC §11.3 — **REFUTED (defended)**
`from = tool_use block`, `to = coarse tool_result block | hole`
(`mechanical-edge-plan` :496-504). Both are `->target-ref :block uid` (:843-854)
→ `:block` is an OPEN target-kind → `:else` branch → `target-key = (str
target-id) = unit-id`; `well-formed-target?` (:296-301) satisfied. Direction
matches SPEC §11.3 verbatim ("write-tool call → `produced` edge **to** the world
artifact touched; read-tool call → a `grounds` observation of what was read") and
§4.3 ("the `(tool_use, tool_result)` pair is bound by a `produced`/`grounds` edge …
not by containment"). CONTRACT §8 line 132 matches field-for-field (target-kind
`:block`, kinds produced/grounds, asserter `sense-block/mechanical@1`, silver,
driver-side T11, idem `sha256(unit-id ∥ kind ∥ target-id)`). **Structural
part-index alignment** (not luck): `tool-index` computes the result `to-unit-id`
as `derived-unit-id(ok, block-path(order, part-index, 0))` (:455-468) and
`coarse-block-import` mints the block at the SAME `block-path(order, part-index,
0)` (:824) → edge `:to` == minted `unit-id` by construction.

### 6. Determinism / retry-safety — **REFUTED (defended)**
`relation-id-for` (:112-119) = sha1 over (kind, from.kind/id, to.kind/id,
asserter) — ids only, text-independent. `edge-idempotency-key` (:64-68) = sha256
over ids. `mechanical-edge-request` (:835-848): `asserted-at-ms` = `sent-at-ms` =
the tool_use event's `production-time-ms` (parsed transcript timestamp,
`parse-timestamp` :97-101) — replay-stable, never wall-clock; `request-id ==
idempotency-key`. No `System/currentTimeMillis` / `random-uuid` in the edge or
coarse-block path. RK topology's activity/order keys derive from client stamps
("NO wall clock (trap 4b)", :757-759). (See NOTE-2 for a pre-P3 pr-str caveat that
does NOT affect edge identity.)

### 7. Fixture / golden — **REFUTED (defended)**
Golden regenerated honestly: `redacted-events` (test:45-55) runs each fixture line
through the REAL `tr/redact-payload-with-redactions` then `inject-nul`
(test:31-40, `[NUL]` → real U+0000) — so `[REDACTED]` / secret-absence come from
the production redactor, not a test fake. Planted `sk-FAKE-SECRET-…` (fixture tu-1
`authorization`) → `[REDACTED]` in golden (span [0 95] reflects redacted length) →
G6 asserts absent from every surface + unit (test:450-458). Spans hand-verified
(tu-2 [0 32], tu-3 [0 54]). The `u-tr` +1 tool_result (tu-2) is the correct new
coarse-block pair source (event 8, part-index 1 → block-path `000008:01:000000`,
test:651). NUL survives pr-str→read-string (T8, test:231-241).

### 8. Tests PROVE the gates (not MASK) — **REFUTED (defended)**
All gate reads are PHYSICAL foreign-selects, not public query APIs:
`read-unit-physical` (test:371-375) = `foreign-select-one [(keypath uid)]
$$derived-units-by-id`; `read-anchor-physical` (:377-381); G8/G9 edges via
`rk/read-relation-row` (rk:973-975, raw `$$relations-by-id` select). Asserter /
kind / status / asserter-type all pinned (test:691-693, :708, :719-721): kind
`:produced`/`:grounds`, status `:asserted`, asserter `sense-block/mechanical@1`,
asserter-type `:machine`. The G9 negative is a physical absence on the correctly-
routed task (Class 2).

### 9. Two-IPC launch (N4) — **REFUTED (defended)**
`start-distiller-runtime! {:relations? true}` (:909-918) → OC via
`ocr/start-object-container-runtime!` + RK via `rk/start-relation-runtime!` on
**separate `create-ipc`** clusters. Barrier targets `rk-rt` (`:ipc`/`:module-name`
of rk-rt, test:679-680); OC coarse mint awaits on `oc-rt` (:874) — no cross-cluster
confusion. `close-distiller-runtime!` (:920-925) closes both; deftest `try/finally`
(test:752). Each module owns its own PStates on its own cluster → no ownership
hazard. (See NOTE-3 for a cosmetic start-order leak.)

### 10. Cross-package coordination — **REFUTED (defended)**
`relation-kinds` now = 15 kinds; block's `:grounds :assembled-from :refines` +
code-atom's `:requires :calls` coexist on one set literal (rk:58-73; `git diff` =
pure additive, no removal). **No test pins the exact set:** the only size-sensitive
test, `relation_kernel_test.clj:556`, is `(<= (count descriptors) (* 2 (count
rk/relation-kinds)))` — a bound computed *dynamically* from the live registry, so
new kinds only loosen it. `relation_assert_route_test.clj:90` uses `:relates-to`
(registered by neither package) as its negative — still rejects. Both suites green
with all 15 kinds present (222a + 740a). `test/resources/code-atom/
relation_kernel.clj.txt` is a frozen specimen fixture, not a live assertion.
⚠ At commit: re-run BOTH packages' gates after either lands (T11).

---

## Failure modes attempted (specific scenarios)

1. **Re-run duplicates the coarse block** → could not: distinct deterministic
   import-key + content-only fingerprint → OC journal dedup (Class 1).
2. **G9 nil is a mis-routing false-green** → could not: hole id routes to the same
   `chat:<hex>` object-key/task as real units (Class 2, the deeper trace).
3. **A real block collides with a hole id** → could not: `pending-result:…` ≠
   all-digits block-path (Class 2).
4. **event-ctx refactor silently changed stored ids** → could not: single-source
   ctx, no prior state, green physical gates (Class 3).
5. **Barrier returns pre-materialization / miscounts the re-run** → could not:
   processed-count is post-commit; cumulative 3+3 (Class 4).
6. **Two tool_uses collide on one relation-id** → could not: distinct part-index →
   distinct from-unit-id → distinct relation-id (test asserts 3 distinct, :655).
7. **RK rejects `:asserter-type :machine`** → could not: `request-shape-errors`
   (:314-340) has no asserter-type allowlist (verify-first; contrast OC's set
   rejection); micro-gate + G8 confirm `:machine` on the row.
8. **Wall-clock / random in edge or block id/time** → could not: hygiene-clean,
   production-time only (Class 6).

## Writers / readers / clearers per changed state

| state | writer | reader | clearer | idempotence basis |
|---|---|---|---|---|
| `relation-kinds` (+3, +2 sibling) | none (const) | `registered-kind?` (rk:294) | — | guard intact — bogus kind still `:relation/kind-unregistered` (test:620-626) |
| coarse `:tool-result-span` unit (`$$derived-units-by-id`) | OC import topology via driver | edge `:to` endpoint; river-page (P5) | never (immutable stratum) | distinct deterministic import-key + content-only fingerprint (Class 1) |
| `produced`/`grounds` edge (`$$relations-by-id` + 2 endpoint copies) | RK microbatch | `read-relation-row` / `read-relations-for-targets` | retract (unused) | journal `(relation-id, idem-key)` (rk:684-686) |
| per-part surface `:production-event` (F2 home) | OC import (P1) + re-declared by P3b coarse import | G2 physical read | never | byte-identical re-declaration (same ctx) |

## Async ordering risks

- Driver is single-threaded; `assert-mechanical-edges!` (:850-878) mints ALL paired
  coarse blocks (each awaited, :872-874) BEFORE asserting ANY edge (:876-877), so a
  paired `:to` resolves to a real block when its edge lands. Reversed order is still
  legal (holes persist), so no correctness dependence on the order.
- Concurrent driver invocations would be safe by idempotency (same request-ids /
  import-keys → journal + fingerprint dedup), though the driver is not designed for
  concurrency.
- A→B→A-finishes-after-B on RK: journal replay makes re-assert a no-op; final state
  order-independent.

## Error-path cleanup

- `close-distiller-runtime!` closes both runtimes; every IPC deftest wraps it in
  `try/finally`.
- **PLAUSIBLE (non-blocking):** `assert-mechanical-edges!` awaits the coarse-import
  decision (:874) but does NOT inspect `:status`; a rejected/timed-out mint would
  silently degrade a *paired* edge into a dangling one (holes are legal, so not
  *wrong*, but the pairing intent is lost invisibly). NOT deterministically
  reachable: the import-key + fingerprint are deterministic and the tool_result text
  is read from stored source, so the re-declare never conflicts → never rejected.
  Falsifier already in-suite: G8 asserts the coarse block LANDED (non-nil), so a
  real rejection fails the test. (Author flagged the same; I concur — non-blocking.)

## Open doubts (non-blocking; each with a falsifier)

- **NOTE-1 (Class 3 residual):** `event-ctx` byte-identity vs the pre-refactor P1/P2
  code is *reasoned*, not *diff-proven* (those versions are uncommitted +
  overwritten). Falsifier for a future skeptic: `git stash`-diff once P0–P2 are
  committed. Risk is nil in-test (no prior state) but unverifiable at the byte level
  today.
- **NOTE-2 (pre-P3, out of diff scope):** `tool-use-text` (:106-110) is
  `(pr-str (:input block))`; for a tool input with >8 keys, `PersistentHashMap` seq
  order is JVM-hash-dependent, so the tool_use BLOCK text (and its content-hash)
  could differ across JVM *versions*. Within one deployment it is stable, so
  idempotence + fingerprint dedup hold; and the EDGE identity is id-based
  (text-independent), so edges are unaffected. Fixture inputs are all ≤4 keys
  (array-map, insertion-ordered) → not triggered. Pre-P3 construct; recorded for the
  gate, not a P3 finding.
- **NOTE-3 (cosmetic):** `start-distiller-runtime!` (:909-918) has no `try` around
  the RK launch — if `rk/start-relation-runtime!` throws after OC started, the OC
  IPC leaks. Dev/test-runtime helper only; production uses real clusters. Trivial
  fix if desired (`try`/close-oc-on-throw), not required.
- **By-design (recorded, not a defect):** the `u-tr` tool_result surface ends up
  with TWO `ImportCompletionRow`s (P1 event import + P3b coarse import, distinct
  import-keys) pointing at one source-id. Content is identical → benign; auditors
  should know the surface has two "importers".
- **Edge-direction family question** (agree with author): whether epistemic
  `grounds` should ultimately point evidence→claim is a *kinds-round* question, not
  a mechanical-floor correctness one. SPEC §11.1 keeps `grounds` an interface,
  family-assigned later; the mechanical floor direction here IS §11.3-conformant.

## Reconciliation with the author self-review (read last)

My independent verdict CORROBORATES the author's (`P3_FALSIFICATION.md`): no
blocking defects; same open doubts (unchecked coarse-import decision; edge-direction
family; N4 production). Where this pass went **deeper**: (a) Class 2 — the author
argued only *no collision*; I additionally proved the hole id *routes to the same
object-key/task* via `extract-object-key`/`leading-object-key`, closing the
mis-routing false-green the opening prompt flagged; (b) Class 1 — I traced
`import-material-fingerprint`'s content-only `select-keys` to show *why* the dedup
is replay-stable; (c) Class 10 — I confirmed no test pins the exact kind set (the
descriptor bound is dynamic). No disagreement with any author claim.

## Done gate

Named failure mode that cannot happen: **a re-run duplicating edges or coarse
blocks, or a G9 false-green from a mis-routed hole read.** Edges + coarse blocks are
deterministic-key, journal/fingerprint-deduped, and byte-identity-gated; the hole
read is a true absence on the correctly-routed task. **P3 diff PASSES QC layer 4.**
