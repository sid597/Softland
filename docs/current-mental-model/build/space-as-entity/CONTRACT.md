# CONTRACT — space-as-entity, rungs 1+2

Status: BINDING (authored 2026-07-26, Fable, from the staging session that
derived the fence; settled order on the board). Precedence: this contract +
`decisions.md` outrank NOW.md and every phase artifact.

## Purpose

The space stops being the pick MISS and becomes the outermost RUNG of every
claim chain (rung 1), and gains its first facet-master — `fm:space` — so
space policy (zoom clamp form; tap/marquee bindings) is served, revisioned,
activatable, reversible material instead of frozen literals (rung 2). The
camera's gestures become structurally uncapturable by material at any tier
(the reservation fence). Behavior at the moment of the cut is identical,
proven against served truth, not assumed from code.

## Consumers, in order

1. Sid's own wheel/zoom/pan — unchanged on day one (floor answers).
2. The first `fm:space` revision — zoom clamp retuned from inside the land,
   no deploy: the package's felt payoff.
3. Rung 3 (G10 lift): per-space instance deviations — inherits the fence.
4. Future space recipes / nested canvases — the chain rung is their claim
   seam; the fence is their safety floor.

## Non-goals (each refusal is an extension point)

- NO G10 lift (`binding_material.cljc:106` stays) — rung 3's own cut; the
  fence landed here is its prerequisite.
- NO camera-verb unreserving — forever-fenced until a package brings its own
  unbrickable-escape drill.
- NO cross-container pick path, NO per-space camera — the nesting package;
  its seam is the one chain-builder this package leaves behind.
- NO type stratum / recipe — waits for a second space instance.
- NO zoom bands (looks OR acts), NO reactions — the fm:space form is the
  looks half's future home; the camera settle is the acts half's future
  event site. Nothing here may pre-build either.
- NO verb_registry.cljc changes — zero new verbs; a diff touching it fails
  review on sight.

## Placement ruling

- `src/app/shared/space_material.cljc` (NEW): both sides need it — server
  validates/serves, client floors/dispatches. Precedent: all six existing
  materials are `shared/`. Not client-only (durable serve would bypass the
  fence); not a new server namespace (the floor must exist client-side).
- The reservation predicate lives in `binding_material.cljc` beside
  `instance-legal-sites` — grammar lives with the row law (G10's one-place
  precedent). Reversal cost of all of rung 2: low — pure data + predicates;
  the seam is the single predicate var and the single spec entry.

## Traps ledger (cite trap numbers in code comments)

- **T1 — `into`, never `conj`.** `space-claim` is a ONE-ELEMENT VECTOR
  (`binding_material.cljc:418-423`). `(conj claims space-claim)` nests a
  vector as a claim; every space gesture dies (pan/zoom/tap/marquee). Drill
  probe 8 catches it in seconds. Ruling: `(into claims
  binding-material/space-claim)`.
- **T2 — "space-floor-bindings verbatim as master material" is structurally
  impossible.** The material grammar refuses floor-reserved verbs
  (`valid-verb-ref?` → `bindable?`): camera rows in a served revision
  malform the facet and floor it whole — behavior looks identical while the
  master tier is permanently floored and tier receipts lie. Ruling: the
  spec's master bindings are the BINDABLE SUBSET only — tap→`:anchor/place`,
  press-threshold-shift→`:selection/marquee-begin`. Camera rows are
  floor-only, forever, by design.
- **T3 — the fence guards the GESTURE, not a tier.** The P5 gate proved
  capture-by-tier at the instance lane; the mechanism is tier-agnostic: a
  served fm:space MASTER revision binding naked drag/wheel to any legal verb
  outranks the floor's camera rows the moment rung 2 serves. Ruling: one
  predicate refuses MATERIAL rows (master AND instance) at `:space/ground`
  for `[:pointer/press :threshold]` or `[:wheel :complete]` with modifiers
  `#{}` or `:any`. Shift-variants stay open. Floor rows exempt (they ARE the
  camera).
- **T4 — never strengthen frozen grammar versions.** `valid-bindings?` v1
  semantics are FROZEN (durable v1 revisions read under v1 forever; the
  strengthening precedent is a NEW version — `valid-bindings-strict?`).
  Ruling: the fence lands as `fm:space`'s OWN spec validators (a new facet's
  grammar carries it from birth) plus the two instance-install lanes
  (console seam + durable serve). Global v1/v2 validators untouched. If an
  implementation cannot satisfy the fence without reinterpreting durable
  revisions of OTHER facets → stop clause.
- **T5 — the clamp read names its keying source.** The zoom clamp is read
  from the SAME served-wears source the binding tiers read (`wears-for` /
  `current-material-wears`, invalidated by the existing wears-cache epoch on
  activation) with the literal floor values as fallback. A second atom or a
  fresh read path is the scene-substrate F1 class (fast-flipping identity
  over slow source). Assertable: no new state atom in the diff.
- **T6 — wheel-pick is measured, not assumed.** Rung 1 adds a pick per wheel
  event; zoom bursts are 60+/s. Measure at the gate (G7). Pre-approved
  fallback if the echo bar shows it: pick once per burst (first wheel event
  of a burst picks; the burst reuses the hit until the settle debounce
  fires).
- **T7 — exactly ONE chain-builder.** `dispatch-key-eval!` hand-builds its
  chain today; after rung 1 that would be a second builder that drifts.
  Ruling: one shared builder appends the space rung; both the pointer path
  and key-eval route through it. Grep-gate: `space-claim` is referenced by
  the builder + `binding_material` definitions only.
- **T8 — pinned scans and table counts move; sweep them deliberately.** P5/P6
  suites pin interaction-table row counts, drill shapes, and dispatch branch
  counts ("frozen at current branch count once migrated" — DIRECTION
  tripwire). This package adds probes and (after first activation) master
  rows. Before calling suite selection done: grep the test tree for pins
  over `ground.cljs`, `binding_material`, claim-chain, the interaction
  table, and the drill; every enumeration this package moves is updated in
  the same change and LISTED in the phase artifact.

## Deliverables

**Rung 1 (Phase P1):**
1. `claim-chain` appends the space rung via `into` (T1); the miss/stale-slot
   fallback preserved by construction (empty claims → space-only chain).
2. `handle-wheel!` passes the real pick hit (delete the `:hit nil`
   fabrication and its comment).
3. One chain-builder (T7); `dispatch-key-eval!` routes through it.
4. Floor drill: + "wheel at a block" probe (probe 12).
5. T8 sweep for everything rung 1 moves.

**Rung 2 (Phase P2):**
6. `space_material.cljc`: spec on the attention pattern — form
   `:space/zoom-min` 0.1 / `:space/zoom-max` 8.0, clamp-of-clamps validator
   (`0.01 <= min < max <= 1000`); master bindings = the bindable subset
   (T2); floor bindings = today's four `space-floor-bindings` rows,
   verbatim, still sourced from/consistent with `binding_material`.
7. The camera-gesture reservation predicate in `binding_material.cljc` (T3),
   enforced at: the fm:space spec validators (T4), the console install seam,
   and the served-instance lane.
8. `:camera/zoom-at-pointer` reads the clamp through the wears source (T5),
   literal floor as fallback.
9. `facet_masters.cljc`: register the spec; reconcile the hand-entered
   `:space → "code-floor:space"` entry and every test pinning that label
   (T8).
10. `material_circulation/default-material-policy-paths` gains
    `"src/app/shared/space_material.cljc"` (escape detector from birth).

## Acceptance gates (numbered; each executable; one-liners carry their own
scope)

- **G1** — Floor drill, 12 probes, suite AND live console agreeing: the 11
  pre-existing probes report byte-identical verb/tier/facet/outcome to the
  pre-cut baseline **while no fm:space revision is active**; probe 12
  (wheel at a block) reports `:camera/zoom-at-pointer` / tier `:floor` /
  facet `:space` at the space depth.
- **G2** — Served-truth receipt (verb: ENUMERATE), server-read from the live
  cluster, never a client cache: across ALL served binding-bearing
  revisions, zero ACTIVE-revision rows with gesture `:wheel` at any block
  site (`:block/user-hit-area`, `:block/machine-hit-area`,
  `:block/fold-header`); wheel rows in non-active revisions are listed as
  flagged residue, not failures. **If this receipt FAILS, the
  behavior-identical premise is false → stop clause, Sid's word required.**
- **G3** — Live drive: wheel over a real block zooms via the space rung at
  depth > 0 (decision receipt); wheel over empty ground unchanged; pan and
  tap-anchor unchanged.
- **G4** — Fence refusal: a candidate fm:space revision binding
  `[:pointer/press :threshold #{}]` → `:selection/marquee-begin` at
  `:space/ground` is REFUSED at grammar (error card; worn surface
  unharmed); the drill still pans/zooms. Suite test + one live drill run.
- **G5** — Fence one-place (style gate; stops at the reservation set): the
  refused (gesture, phase, modifiers) set is defined in exactly ONE var in
  `binding_material.cljc`; the fm:space validators, console seam, and
  served-instance lane all read it (three call sites cited in the phase
  artifact).
- **G6** — Clamp as material, on the live cluster: activate an fm:space
  revision with `zoom-max 2.0` → wheel clamps at 2.0, no deploy; rollback
  (repoint) restores 8.0; a malformed revision (min ≥ max) refuses to the
  floor values. EXPECTED tier transition, not a regression: after first
  activation, drill probes tap-empty-space and shift-drag-empty-space
  report tier `:master` (the space's meaning served as material — the
  package's payoff); camera probes stay `:floor` forever.
- **G7** — Echo bar: the standard 52ms bar during a sustained zoom burst
  with wheel-picking live; receipt's FIRST line is the environment
  attestation (browser + WebGPU adapter identity, `isFallbackAdapter`). Red
  → apply T6's pick-per-burst, re-measure; still red → stop clause.
- **G8** — Escape detector: membership assertion —
  `space_material.cljc` ∈ `default-material-policy-paths` (stops at
  membership; no new detector harness).
- **G9** — Label coherence: server projection and client tiers emit the SAME
  master-id/floor label for the space post-spec (G14 class); stale
  `"code-floor:space"` pins swept per T8.
- **G10g** — One chain-builder: grep gate per T7 + key/eval probe resolution
  unchanged.
- **G11** — New-facet totality: an arbitrary-garbage fm:space candidate
  floors the WHOLE facet with an error card; all 12 probes stay green.

## Read plan for the one performance promise

The wheel path adds exactly one `ss/pick` per wheel event (or per burst
under T6's fallback) — no other new per-gesture work. The clamp read is one
map lookup on the already-materialized wears value (T5) — no per-gesture
resolution.

## Stop clauses (beyond the standard: contract unbuildable / binding-doc
conflict)

- G2's receipt shows an active wheel row at a block site.
- The fence cannot reach all three lanes from one var without touching
  frozen grammar versions (T4).
- G7 red after the pre-approved fallback.
- Any need to touch `verb_registry.cljc` or lift `instance-legal-sites`.

Escalation per the work-package skill: verbatim citations + options +
recommendation into `decisions.md` Open Questions; no improvisation.

## Input manifest

- `src/app/client/workspace/ground.cljs` — claim-chain 2330-2350 · decide/
  dispatch/dispatch-key-eval! 2666-2735 · handle-wheel! 2888-2900 ·
  `:camera/zoom-at-pointer` 2649-2660 · wears-for ~271 ·
  served-instance-binding-rows 2260-2303 · binding-tables 2411-2448 ·
  floor-binding-rows 2201.
- `src/app/shared/binding_material.cljc` — sites 77-101 ·
  instance-legal-sites 103-120 (G10, untouched) · validators 137-234 (T4:
  v1/v2 frozen) · space floor section 383-423 (space-claim IS a vector —
  T1) · probes 429-521 · table 527-599.
- `src/app/shared/verb_registry.cljc` — floor-reserved 171-195 · bindable?
  234-240 (READ-ONLY this package).
- `src/app/shared/attention_material.cljc` — the form/bindings/validators
  pattern (36-39, 58-86, 119-135).
- `src/app/shared/facet_masters.cljc` — specs 14-20 ·
  floor-master-id-by-facet 42-53 (the hand entry to reconcile).
- `src/app/server/rama/material_circulation.clj` —
  default-material-policy-paths 60-79.
- Test tree: grep for pins per T8 before finalizing suite selection.

## Process

- Implementer: fresh build session (cheaper-model lane per the role split),
  booted from THIS file + NOW.md STANDING. Phases P1 (rung 1) then P2
  (rung 2), each running its gates green in-context.
- Falsification batch: ONE finder, aimed at the genuinely-new machinery —
  the fence (all three lanes) + the chain append (T1/T7 edges).
- Gate review: Fable. Sized by claim-risk (settled ground 2026-07-26): drive
  the seam live (G3, G6), falsify the new claims (fence, append, clamp,
  receipt), re-run the drill; full suite re-derivation NOT required beyond
  the package's own suites + any HEAD-dynamic suites after commits.
- After green: rung 3 (G10 lift, own small cut) or reaction-declaration,
  per the board order. Sid's return wear is untouched by all of it.
