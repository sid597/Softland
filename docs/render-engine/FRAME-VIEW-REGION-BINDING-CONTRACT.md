# FRAME-VIEW / REGION-BINDING — the correction contract

Cut 2026-08-09. **Cutter: Fable 5 (`claude-fable-5`), effort xhigh** — effort
set by `/effort` before the first prompt of the cutting session; the model
name is the environment's self-description, and the perf dossier's E1 showed
that field can drift from the served model — the two-second check at touch #1
is the session transcript, never this header alone.

**Status: CUT + round complete — awaiting Sid touch #1.** The ONE bounded
fresh-eyes falsification round (Codex, 2026-08-09, at Sid's hand) returned
three decision-changing findings; all three confirmed against source by the
author session (verbatim re-read of every cited window) and folded in one
batched author pass: the §6a container-delta mint moved to the registry
write site (the in-file "already exists" producer was a population-sized
discovery — the mechanism §4.1 outlaws); §6d execution order became an
incrementally maintained subview with WORK counters (§4.4, S2/S3/S4, §15);
the §6e epoch gained its missing production producer seam (renderer
compositor-cache replacement; S5 drives it through `draw-frame!`);
background's second payload consumer (the interior draw-order partition)
pinned, with an S4 kind-transition leg; the §5 budget-wedge refusal
citation now names both the mint and the draw-time fill consumer. No
recut.

**Input basis:** the reviewed Fable-Max draft (2026-08-09, in-chat) · the
Fable review verdict (three decision-changing findings, all folded — F1 in
its sharpened four-neighbor form, F3 in its two-value form; the sharpened
forms are the tail refinement notes of the commissioning prompt) · a
verbatim code-anchor sweep by this session's gatherer, every anchor
re-verified against the working tree on 2026-08-09. One reconciliation made
by the cutter: the commissioning line "road = maintained select-color-mode
value incl. forced/copy-present/throw" is shorthand for the road MODEL;
in code (verified) `select-color-mode` takes `{effect-spans regions
forced-color-mode}` and capabilities enter separately at compile
(`frame_graph.cljc:363-365`) — so the contract binds the two-value split
(§6d), which is the code-exact reading.

**Supersedes** (exact edits in §4): FRAME-RETENTION-CONTRACT §5e · the
lease-shape classification in §5g and NOW.md's standing default · refusal
R5 (brought into scope — C1 is the summons) · and it EXTENDS
REGION3D-FLOOR-CONTRACT S5's counting boundary.

---

## 1. What this atom is — scope in plain words

Four kinds of state exist in the frame road and each change must go only to
the state that owns it: **scene meaning** (which entries exist, their order,
their semantic fields) · **effect membership** (which entries belong to
which effect containers) · **plan topology** (which logical passes and
resources exist and their dependency edges) · **GPU binding** (which
physical texture/lease/buffer/bind-group currently implements a logical
resource, on which device).

Today a Region3D lease resize — a pure GPU-binding change — walks the whole
semantic road: shape mint → entry produce → arrangement update → deep
equality over complete plan inputs → population-sized effect and plan
maintenance. The C1 receipt (closed evidence, §2) shows that road costing
27–36ms per mint frame on mobile, dominating zoom.

The correction has two mandatory halves, and neither is sufficient alone
(receipt-backed — §12 first entry):

- **Half A — reclassify.** Region3D lease size, composite slot, and
  background leave semantic entry identity and plan topology. A lease-rung
  crossing becomes a binding delta: one region's physical resources
  reconcile; zero semantic entries, zero arrangement work, zero effect
  work, zero plan work.
- **Half B — make real semantic changes proportional.** When an entry,
  effect container, or plan-topology fact genuinely changes, explicit
  deltas flow through incrementally maintained views — arrangement, effect
  execution view, keyed plan fragments — touching only the affected set.
  The batch computations stay alive as oracles (the growth law,
  decisions.md: a batch stage is never deleted; it is demoted to its
  incremental sibling's oracle).

**What this atom is NOT:** no partial redraw, no retained command buffers,
no patch-driven GPU executor (encoding a frame may still walk visible
work); no change to draw-order semantics or Contract-O; no scene_tape
edits; no new durable event vocabulary, no Rama-side change; no real
`GPUDevice.lost` wiring (F2 — §11); no generic event bus or Electric clone
— the delta vocabulary is render-specific and small.

---

## 2. Receipts basis — closed evidence; not re-litigated

- **C1, the dominant remaining cost** — `PERF-DOSSIER-2026-08-09.md:212-253`.
  Mobile: plan bucket 27–36ms (spikes 55–62) on lease-mint frames, total
  41–52ms, ~12–20fps; interleaved no-mint frames 9–15ms. Desktop, same
  chain: 1 produced entry, 1974 comparator calls, effects + plan both
  maintained. RECEIPT, closed.
- **Run P correction** — `FRAME-RETENTION-NOW.md:15`. The arrangement is
  NOT rebuilt post-boot (zero constructions); the ~1.9k comparator calls
  are the plan gate's deep `=` over old/new arrangement maps embedded in
  `plan-inputs`; effects/plan remain population-sized per 1-entry delta.
  RECEIPT (stack trap), closed. The repair target is therefore change
  DISCOVERY and effect/plan MAINTENANCE — never "make arrangement
  incremental" (already done).
- **The mechanism enumeration** — Fable review verdict + this session's
  verbatim sweep: the only reader of `plan [:structure/key :regions]` in
  the client is the compositor's lease acquisition
  (`compositor_gpu.cljs:1165-1196`; grep: 2 writers, 1 reader) — the plan
  is a transport route for sizes. `region-resources`/`region-passes` never
  read lease width/height (grep over `frame_graph.cljc:128-193` for
  `size|width|height`: zero hits) — formats, sample counts, usage flags,
  and edges key off `:region/id` and `:shadow?` alone. The compiler reads
  only `(:format viewport)` from the viewport (`frame_graph.cljc:363`).
  STRUCTURE, verified twice.
- **The token-only gate is insufficient** — `maintain-frame-plan` runs
  `bind-entry-ranges` + `validate-plan!` unconditionally even when the
  structure key reuses (`frame_graph.cljc:622-635`); `maintain-effect-spans`
  full-derives on any order-token change (`frame_effects.cljc:257-267`).
  Killing only the comparator leaves the population work standing.
  STRUCTURE, verified.

The fresh-eyes round must not re-trace or re-profile C1; challenging a
receipt requires equal taken-path evidence (investigation fence).

---

## 3. Provenance — where the defect entered (and the uncommitted-tree fact)

- **W4 (`143497a`, 2026-08-07)** supplied the coarse population-sized
  plan/effect substrate — latent weakness, no Region3D trigger yet.
- **Package 3 Atom A (`8b1ab98`, 2026-08-07)** committed the
  classification error: lease-size into the entry paint and into
  `structure-input` (git `-S` receipt in the review verdict).
- **FRAME-RETENTION (contract `02868a4`, built 2026-08-08)** repaired
  camera-frame semantic work but preserved the classification — §5g
  "lease-rung changes mint shape" — and deferred plan/effect internals
  (§5e "internals stay untouched", R5 "LATER if a typing receipt summons
  it"). The retention-era code then inherited the classification into the
  **uncommitted** `region-entry-shape-key` and registered
  `:region-lease-size` as a door in the **untracked**
  `frame_inputs.cljc` registry.
- **2026-08-09**: the C1 receipt invalidates the deferral. Note for the
  supersession record: R5 named a TYPING receipt as its summons; C1
  arrived via the lease-mint road — the same population mechanism,
  summoned by a different door.

**Uncommitted-tree fact (named, not resolved here):** part of the surface
this contract corrects exists only in the working tree
(`frame_inputs.cljc` untracked; the shape key uncommitted), which sits on
Sid's standing "uncommitted tree spanning atoms" concern, next to the open
scene_tape SHA fork awaiting his ruling (`FRAME-RETENTION-NOW.md:11`).
Banking is Sid's call at touch #1; this atom neither commits nor depends
on a particular banking order, and it must not fold edits into the
scene_tape ruling (MUST-NOT 2).

---

## 4. Supersessions — exact

1. **FRAME-RETENTION-CONTRACT §5e — replaced.** Dead law: effects/plan
   gated behind whole-value identity over `[arrangement-identity,
   container-registry, viewport(w/h/format), capabilities]` with internals
   untouched. New law: arrangement, effect view, and plan view consume
   EXPLICIT deltas (§6a); no complete old/new input comparison exists as a
   live change-discovery mechanism; effect and plan work is proportional
   to the touched set; the batch computations are oracles.
2. **§5g's lease-shape sentence + FRAME-RETENTION-NOW.md 2026-08-08
   default ("lease-rung changes mint shape") — replaced.** New law: a
   lease-rung crossing mints a BINDING delta; the semantic Region3D entry,
   the arrangement, the effect view, and the plan are unchanged by it.
   The rest of §5g (per-region prepare gate, two-ladder split, exact pick
   viewport) stands, amended only as §6e states.
3. **R5 — brought into scope.** This atom IS the population-sized
   effect/plan repair R5 deferred. It does not survive as future debt.
4. **REGION3D-FLOOR S5 — extended, not replaced.** S5's counters stop at
   region-side uploads/encodes/leases; the extension adds downstream
   counters (semantic entries produced · arrangement upserts/removes ·
   order-comparator calls · effect containers touched · container
   declarations inspected · plan fragments compiled · plan-order
   nodes/edges visited · full plan validations) so a subsystem-green
   receipt cannot hide work moved downstream — the counters meter WORK
   (inputs inspected, nodes visited), never outputs alone: a build that
   rebuilds a population but mutates one row passes every output count.
   This atom's S1/S4 carry them.
5. **The door registry** — `:region-lease-size` is struck from
   `quantization-doors` (`frame_inputs.cljc:20-23`) and from the
   camera-door allowlist (`frame_inputs.cljc:89-92`), and the region-3d
   family declaration drops it: post-correction NO camera-derived value
   enters semantic identity through any door. Camera reaches Region3D only
   through the binding lane (the 256px allocation ladder, constant
   unchanged) and the payload lane (the 1.12× encode ladder, unchanged).
   `:region-encode-scale` was already payload-local (verified: prepare/view
   keys only, absent from shape key and paint) and stays.
6. **FRAME-RETENTION-NOW.md STANDING, third line** ("Region3D admits
   camera into semantic work through the 256px allocation ladder…") — flips
   at THIS atom's close to the binding-lane wording above. A
   supersession-pending marker lands with this cut; the flip itself is the
   implementer's close write-set.

Everything else in FRAME-RETENTION-CONTRACT stands: §5a–§5d, §5f
(prepare-gate completeness law — amended reading in §6e), §5h twin law,
MUST-NOTs 2–8, the ladder constants, the instrumentation law.

---

## 5. Binding laws — pointers, never restatements (each names the scenario that fails under its violation)

- `docs/decisions.md` "recompute proportional to change" block (~445-544):
  proportional-to-affected-set at every layer · change minted once at the
  write site · minted diffs are values · one generation authority per
  entangled truth, independent cadences meet at the frame pull · the
  fenced incremental view with batch-demoted-to-oracle · the
  five-declaration derivation contract (keyed inputs, door, ownership,
  projections, oracle+fence) · no execution clock as derivation ancestor.
  Violations fail S1/S2 (proportionality counters), S5 (generation
  stamps), all scenarios (oracle fence); a clock-fed rev or delta fails
  the retained-vs-batch twin in every scenario (MUST-NOT 5).
- `FRAME-RETENTION-CONTRACT.md` §5d payload indirection (fails S1 —
  stale-texture class) · §5f prepare-gate completeness + shape-rev law
  (fails S1's lying-prepare leg) · §5h twin (fails every scenario's
  oracle-divergence assertion) · MUST-NOT 3 no raw camera in semantic
  keys (fails S1) · MUST-NOT 4 encode-side camera never gated (fails
  S1's projection assertion) · MUST-NOT 5 twin never gates the live path
  (fails every scenario's oracle assertions + S1's lying-build kill).
- `W4-FRAME-RUNTIME-CONTRACT.md` — plan pass-kind vocabulary, the
  ABSOLUTE aliasing law (producer edges for sampled intermediates), the
  color-mode law (any effect ⇒ whole-frame linear; regions join the
  trigger). `select-color-mode` (`frame_graph.cljc:71-87`) is that law's
  implementation and stays byte-unchanged (the color-mode law fails
  S3's road legs; the aliasing law + pass vocabulary fail S4's
  fragment/edge assertions).
- `REGION3D-FLOOR-CONTRACT.md` §5.6 lease ladder + M10 pool law, the
  refusal road, `decisions.md:170-173` proportionality charter for
  regions (fails S1/S4 lease-lifecycle receipts).
- The budget-wedge lifecycle laws (HEAD `43ae9d9` lineage; in-source
  rationale at `compositor_gpu.cljs:1173-1178`): old-generation leases die
  BEFORE the new generation acquires (two generations never double-bill
  the budget) · async retirement rides `onSubmittedWorkDone` with identity
  re-check (`:640-652`) · refusal produces a receipt + refusal fill, never
  a silent skip (receipt + refusal VALUE minted at `:590-602`; the
  draw-time fill consumer is `region3d_gpu.cljs:1371-1386`) · idempotent
  destroy. Behavior-frozen; the
  ONE lawful re-plumb is §6e's desired-set source (fails S1/S4/S5).
- `scene_tape.cljc` Contract-O ordering: comparator
  `[stratum, pass-class, stack-path, part-rank, stable-tie]` (`:641-651`),
  `entry-key = [(:order entry) (:entry/id entry)]` (`:705-706`) — consumed
  as-is, zero edits (fails S2's order assertions and MUST-NOT 2).
- CLAUDE.md Token Economy + work-package law govern the implementer
  session (execution straight through, one falsification pass in-session,
  acceptance = Sid's word). Session law — governs conduct, not the
  artifact; no scenario attaches.

---

## 6. The design

### 6a. The delta vocabulary — `frame_delta.cljc`

Small, render-specific, pure values. Two lanes, and the lane boundary is
the load-bearing wall:

**Semantic deltas** (enter the reducer, §6f):
- entry delta — `{:delta/kind :entry, :op :insert|:remove|:update|:move,
  :entry/id, :old-entry, :new-entry, :old-key, :new-key}` where keys are
  `scene/entry-key` values. An update whose old/new order keys differ IS a
  move (processed as remove-at-old + insert-at-new for touched-set
  purposes). Minted by the arrangement maintainer as it applies family
  produces — `family-entry-delta` (`frame_inputs.cljc:188`) already
  computes upserts/removes per family; it is promoted from ledger
  bookkeeping to the delta source.
- container delta — `{:delta/kind :container, :container/id,
  :class :parameter|:topology, :old-decl, :new-decl}`. Minted at the
  REGISTRY WRITE SITE, keyed by the container id the mutator already
  holds — `set-effects!` (`scene_runtime.cljs:303-310`) is the proven
  exemplar; sibling registry mutators that change an effectful
  container's nesting mint likewise, covering the moved container's
  stack-path subtree (proportional to the subtree, never the registry
  population); thin hook per mutator vs one keyed registry journal is
  implementer detail, bounded by the declarations-inspected counter
  (§4.4). The per-container old/new-decl compare at
  `frame_effects.cljc:264-265` supplies only the diff/classification
  SHAPE, applied to the minted ids; its enclosing discovery — the
  whole-registry `declarations` rebuild + all-keys union
  (`frame_effects.cljc:241-245, 256-266`) — is itself the population
  mechanism this contract kills (verified: no function in that file
  takes a single container id). It dies on the live road and survives
  only inside the oracle. Classification pin: **topology** = the SET
  of effect kinds present, nesting (`:parent/container-id`, `:stack-path`,
  `:depth`), mask structure — anything that changes pass shape;
  **parameter** = numeric/color values within a kind (opacity value, blur
  magnitude, color). Grounding (verdict, non-blocking note): no "declared
  algorithm boundary" vocabulary exists in the plan today — blur radius is
  parameter/payload; the executor's radius-driven downsample-chain
  expansion is applier-private residency and stays that way.
- region topology delta — `{:delta/kind :region-topology, :region/id,
  :op :open|:close|:shadow-flip}`. Minted by the Region3D prepare, which
  already computes `live-ids` vs prior (`region3d_gpu.cljs:1001-1003`)
  and `shadow?` per region.
- global delta — `{:delta/kind :global, :field :viewport-format|
  :capabilities|:forced-color-mode, :old, :new}`. Minted at the renderer
  frame edge by comparing exactly these three scalars against their
  previous values — three cheap compares, never a map diff.

**Binding deltas** (never enter the reducer; consumed by §6e):
- `{:binding/kind :region-lease, :region/id, :old-lease-key,
  :new-lease-key}` — minted by the Region3D prepare at the allocation
  ladder.
- `{:binding/kind :region-payload, :region/id, …}` — background, encode
  rung, composite-row values.
- `{:binding/kind :viewport-size, :old [w h], :new [w h]}` — minted at
  the renderer frame edge from the attachment-size compare; consumed by
  the renderer's EXISTING surface/attachment rebind road, never the
  region owner; width/height leave plan topology (current source has no
  topology consumer of them — §2 enumeration; if the implementer finds
  one, that is fork F-d, one question, never a stop).

Law: GPU objects (textures, views, bind groups, buffers, devices) never
appear inside any delta payload or any semantic state — stable ids,
revisions, lease KEYS, and the §5d stable system/pool reference handles
only (handles are opaque here; their internals resolve at encode —
MUST-NOT 6).

### 6b. Arrangement — return the delta, never rediscover it

The arrangement maintainer (already incremental — Run P) RETURNS the exact
entry deltas it applied, in arrangement order. Downstream views consume
those deltas; **no downstream code may compare complete old/new
arrangements, plan inputs, or entry vectors to rediscover change** — the
renderer's `plan-inputs` map (`renderer.cljs:3834-3838`) and its deep-`=`
fallthrough (`frame_inputs.cljc:111-125` third tier) cease to exist on
this road. The arrangement still owns: entry existence, `:entry/id`,
order keys, semantic paint fields, stable references to payload owners
(§5d). It does not own: physical dimensions, handles, bind groups, device
epoch, lease generations, composite slots.

The encoder may still traverse the visible arrangement — this atom removes
redundant population-wide DERIVATION before encoding, and claims nothing
about the visible-walk cost (§11 R2/R3).

### 6c. The effect execution view — `frame_effect_view.cljc` (F1 folded)

**Current disease** (verified): membership keys on stack-path prefix alone
(`frame_effects.cljc:215-217`) while the order comparator ranks pass-class
BEFORE stack-path (`scene_tape.cljc:644-651`) — so a container's members
are lawfully non-contiguous ("contiguity is an optimization fact, never a
compiler assumption", the in-source law at `frame_effects.cljc:205-207`),
and the live door is `arrangement-token` (`:247`, consumed `:257-258`): a
full vector rebuild whose any-change answer is `derive-effect-spans` over
the population.

**Maintained state, per container** (durable truth): ordered member set
keyed by `scene/entry-key` · topology signature (§6a classification) ·
parameter revision. **Durable numeric entry positions are illegal** —
inserting one entry must never renumber the durable identity of any other
entry, run, or range.

**Execution representation — the ruled default is the event index**: one
persistent ordered structure of group-open / entry / group-close events,
keyed so that events sort by member order-key with phase and nesting
ranks, ties broken by container-id/entry-id. Splits and merges are
order-LOCAL event edits. The exact key-tuple encoding is implementer
detail, bounded by two laws: (i) projecting the event index to the batch
span/action shape must equal `derive-effect-spans` (the oracle); (ii)
updates are order-local per the touched-set law below. Written fork F-a:
deriving execution actions inside the encode-time forward walk (a fused
adapter — the shape `world-export-projection` already has at
`compositor_gpu.cljs:1296-1318`) is the legal alternative ONLY if the
event index hits a real blocker; note it in NOW. Either way the two laws
bind.

**The touched-set law (F1, sharpened — the binding formula):** for an
entry insert, remove, or move, the touched containers are the union of
the effect chains (stack-path-derived container chains) of:
1. the changed entry — old form and new form;
2. the immediate predecessor and successor entries at the DEPARTURE
   boundary, read from the OLD arrangement (remove/move only);
3. the immediate predecessor and successor entries at the ARRIVAL
   boundary, read from the NEW arrangement (insert/move only).

Six chain lookups at most, bounded by nesting depth — never population.
This covers: joining a run, splitting a run (a foreign non-member insert
splits a container it does not belong to — the executable receipt is
`frame_effects_test.clj:54-73`, one non-member at index 2 forcing
`[[0 2] [3 4]]`), merging two runs (removing that foreign entry — it
belongs to NEITHER chain; only the departure neighbors name the
container), opening the first run, closing the last, moving between
containers, and pass-class interleaving. The update applied to a touched
container is order-local: only run boundaries/events between the
departure/arrival predecessors and successors change; the container's
other runs and all untouched sibling containers preserve identity
(`identical?`-checkable). A value update with unchanged order key touches
only the entry's own old∪new chains (equal sets unless stack-path
changed, which changes the order key and is therefore a move).

**Consumers:** the compositor's per-run actions are already keyed
`[container-id entry-range]` with `:all-entry-ranges` preserved for masks
(`compositor_gpu.cljs:832-846`) — the maintained view feeds these shapes
through projection at the required forward traversal; masks and export
keep their current semantics (export already projects to temporary
numeric indices over a filtered subset — that adapter idiom is the
sanctioned one). Numeric positions may exist as EPHEMERAL projection
outputs only, recomputed inside the single required walk, never stored as
semantic truth.

**Oracle:** `derive-effect-spans` stays byte-unchanged. Twin/tests project
the maintained view into the batch span shape and compare; the oracle
result never feeds the live path.

### 6d. The plan view — `frame_plan_view.cljc` (F3 folded)

**Fragments, keyed:** `:fragment/global` · `[:fragment/region region-id]`
· `[:fragment/effect container-id]` · the small fixed export/readback
fragments the current structure carries. Each fragment: stable fragment
id · stable logical pass ids and resource ids (today's
`region-resource-id` minting from stable region-id hash stays) · explicit
dependency edges · a topology signature · a generation. Pass execution
order = topological sort over explicit edges with a deterministic stable
tie (pr-str of fragment/pass id) — durable dense ranks are illegal
(inserting a fragment must not rewrite later passes' identity), and the
order is itself an incrementally maintained subview: a local topology
delta re-derives order only for the affected subgraph (the changed
fragment's passes plus nodes/edges adjacent or downstream of them); a
whole-graph re-sort is lawful only inside a declared global transition
or the oracle. Order work is receipted as WORK counters
(`plan-order-nodes-visited` / `plan-order-edges-visited` — §4.4), never
inferred from output counts: a build that re-sorts the population per
local delta passes every output-equality assertion and is killed by
these counters alone.

**Region fragment signature** = `[region-id shadow?]` — NEVER lease
width/height, background, composite slot, physical views/bind groups, or
encode rung (§2 enumeration is the evidence; fork F-d is the door if a
consumer surfaces). **Effect fragment signature** = the container's
topology class (§6a). **Global fragment** owns surface format, the two
road values below, and the base/present structure.

**The road — two maintained values (F3 in its two-value form):**
- **color mode** = `select-color-mode` byte-unchanged, fed from MAINTAINED
  inputs in the SHAPES its contract requires (it seq-checks
  `effect-spans`, seq-checks `regions`, and its throw names
  `(mapv :region/id regions)`): the plan view hands it the maintained
  region row set (`{:region/id …}` rows from the region fragment index)
  and the maintained container-topology rows — never bare counts. Its
  precedence (regions > forced > effects, `:legacy` default) and the
  regions+forced-legacy THROW (`frame_graph.cljc:73-76`) are preserved
  exactly.
- **presentation variant** = a small pure function of (color mode, surface
  format, capabilities) — conceptually `:linear-present` /
  `:legacy-direct` / `:legacy-copy-present`, realized by the existing
  compile parameters (`linear?`, `copy-present?` at
  `frame_graph.cljc:363-366`). Capabilities do NOT enter
  `select-color-mode`; `:copy-present` can change presentation topology
  without changing color mode.

A change in EITHER value is a declared global transition: the global
fragment updates, and a color-road flip lawfully RE-SIGNS ALL fragments
(rare + declared + receipted as `:global-transition`, never counted
against proportionality). Maintained counts make first/last transitions
direct reads — and note the converse assertion: adding a region while the
scene is already linear changes the count but not the value → NO global
re-sign (S3 asserts both directions). Neither value is ever reduced to a
first/last boolean counter.

**Viewport:** width/height leave topology signatures (binding lane);
format and capabilities remain plan inputs (global deltas).

**Validation + identity:** the live maintainer validates changed
fragments and their affected edges only. Full `validate-plan!` and the
full `stable-hash` (the pr-str canonical walk, `frame_graph.cljc:42-61`,
which hashes every entry id and grows with the arrangement) are demoted
to oracle/diagnostic modes. Runtime plan identity = a monotonic plan
generation (ruled default, fork F-b). Diagnostics update incrementally —
instrumentation must never recreate the population walk it measures.

**Oracle:** the batch compiler road (`structure-input` →
`compile-plan-structure` → `bind-entry-ranges` → `validate-plan!`) stays
alive as the oracle. Because the paint no longer carries `:lease-size`,
`arrangement-regions`' silent fallback to world-rect dimensions
(`frame_graph.cljc:95-98`) MUST be deleted — post-repair it would invent
sizes and hide oracle divergence. The oracle's region input (id, size,
shadow?) is assembled EXPLICITLY by the twin harness from the binding
owner's desired state + topology facts, never scavenged from entries.

### 6e. The Region3D binding owner — `webgpu/region_bindings.cljs` (F2 folded)

Device-local state, per stable region id: desired lease size (quantized,
256 ladder — constant unchanged) · shadow resource requirement · current
lease key + generation · current views/bind groups · refusal state ·
composite slot · encode/payload revisions · device epoch · the semantic
generation it last reconciled against.

**Device epoch (F2):** there is no live `GPUDevice.lost` road to preserve
(verified: zero wiring; all hits are type hints or the image-system's
receipt vocabulary). The epoch is the COMPOSITOR/SYSTEM IDENTITY
generation — the existing structural mechanism (a new compositor is empty
atoms by construction; the image-system recreate policy at
`renderer.cljs:1387-1425` and retention's device-identity reset at §5f
lifecycle pins are the precedents). The epoch has exactly ONE live
producer, and it does not exist yet: a renderer-owned SAME-DEVICE
replacement seam that (i) calls `destroy-compositor!` on the old
compositor, (ii) REPLACES the `!compositors-by-device` entry for that
device — today the cache has no eviction anywhere
(`ensure-frame-compositor!` returns the cached compositor forever,
`renderer.cljs:3453-3460`; `destroy-compositor!` cannot reach the cache,
`compositor_gpu.cljs:676-682`), so destroying without replacing hands
the next frame a dead cached compositor — (iii) mints the new
compositor = the new epoch, (iv) reattaches it as the Region3D owner,
and (v) leaves the semantic state untouched: `reset-frame-retention!`
(`renderer.cljs:3498-3505`) stays keyed on DEVICE identity and must not
fire on a same-device epoch bump (the device-change road lawfully
resets semantic state and is therefore NOT the epoch proxy). S5's live
leg drives THIS seam, then the next `draw-frame!` through production
custody, inheriting the Region3D S5 receipt. Real
`GPUDevice.lost`/`uncapturederror` wiring is refusal R1′ (board debt);
capability-changing recovery is exercised in the pure tier only.

**Lease resize road** (the exact C1 action): prepare mints a
`:binding/region-lease` delta → the owner compares old/new lease keys →
retires the old generation and acquires the new UNDER the frozen
lifecycle laws (§5: old-dies-before-new, after-submit retirement,
refusal receipt) → rebuilds only generation-dependent device objects →
marks that region's roles dirty → the region re-encodes required roles.
The existing invariant that a lease-key change forces re-encode
(`region3d_gpu.cljs:1312-1353`) is preserved — a resized lease is never
sampled un-encoded. Arrangement, effect view, plan view: untouched by
construction (the reducer never sees a binding delta).

**Invocation custody:** `prepare-region3d-frame!` remains invoked every
frame from its existing renderer call site — prepare-RUN is not
family-CHANGED. Post-strike its semantic early-out zeroes semantic work
on camera frames; the binding/payload lanes run their cheap compares
each invocation and mint deltas only at rung crossings. S1's
`region-prepared 1` with `changed-families []` is exactly this split.
The binding owner reports through the SAME `frame_inputs` ledger
(`assoc-ledger!`/`increment-ledger!` — `.cljc`, callable from `.cljs`):
it is the named producer of `region-binding-updates`,
`leases-acquired`, `leases-retired` in §15's receipt.

**The one lawful lifecycle re-plumb:** `active-region-leases!`
(`compositor_gpu.cljs:1165-1196`) stops reading
`plan [:structure/key :regions]` and reads the binding owner's desired
state. The lifecycle FUNCTIONS and their order/refusal/retirement
semantics are behavior-frozen (MUST-NOT 3).

**Composite slots:** `composite-index` is currently the `map-indexed`
POSITION over the regions vector (`region3d_gpu.cljs:1008-1012`), stored
in shape key + paint, landing as the composite-instance ROW index
(`region3d_gpu.cljs:963-968`, stride 5 floats: x y w h container-idx).
Replaced by a slot ALLOCATOR keyed by stable region id: slot assigned at
region open (lowest free slot, else extend) · stable across reorder ·
released at close only after safe retirement (the same
`onSubmittedWorkDone` guard road) · holes legal · free-list reuse only
after retirement. **The allocator owns the composite-instance buffer
layout**: rows live at slot positions, the buffer sizes to max-slot+1,
holes are never sampled because draws select only live slots. Draw order
still comes from the arrangement; the slot only selects that region's GPU
payload row. Opening/closing/moving one region never rewrites another
region's row.

**Background** is payload: it moves to the binding/payload lane (slot row
or its existing uniform site), consumed at its current draw sites through
the owner. Changing a background never reproduces a semantic entry and
never recompiles a fragment. Background has TWO payload consumers and the
lane must feed both: (i) the interior clear value
(`region3d_gpu.cljs:1222-1228` reads color + kind) — any background
change dirties the interior role; (ii) the region-local draw-order
partition — background KIND feeds `transparent-background?`, which ORs
into every interior instance's opaque/transparent classification and
flips its sort direction (`region3d_gpu.cljs:908-921`; today this rides
the material key, `:1098-1101`). An opaque↔transparent kind transition
therefore additionally recomputes THAT region's interior draw-order
before encode — region-local, proportional, still zero semantic and zero
fragment work. A slot-row/uniform-only build that re-encodes with the
previously prepared draw-order renders the wrong partition on kind
transitions; S4's background leg kills it.

**The shape key after the strike** (`region3d_gpu.cljs:985-992`): from
`[region-id composite-index lease-size shadow? background source-order]`
exactly four fields leave — `composite-index` → slot allocator,
`lease-size` → binding owner, `background` → payload lane, `shadow?` →
region-topology delta (plan fragment + binding requirement; the semantic
entry does not depend on it — fork F-d covers a discovered composite-level
consumer). `[region-id source-order]` remain; the entry paint keeps
`:region-system` (stable §5d reference) and its rect (store-frame
geometry, riding the store-frame input door as today).

**Prepare-gate completeness (§5f), amended reading:** the region
prepare's SEMANTIC early-out key lawfully drops lease-size/encode-rung
because, post-strike, no semantic output depends on them — the
completeness law binds over inputs that reach the family's semantic
outputs. The prepare still computes both values every gesture frame; they
feed the binding/payload lanes, each lane running its own cheap compare.
The twin remains blind to prepare-level under-declaration (§5f), so S1
carries the kill: a build where lease-size DOES still reach a semantic
output diverges the twin on the crossing frame.

### 6f. One semantic generation — `frame_semantic_state.cljc`

One pure reducer: `(apply-deltas semantic-state deltas) → semantic-state'`
where semantic-state = `{arrangement, effect-view, plan-view, generation,
last-delta-receipt}`. The renderer publishes it with ONE swap (replacing
the separate `!frame-arrangement` / `!frame-effect-state` /
`!frame-plan-state` / `!frame-plan-inputs` atoms) — no observer can see a
new arrangement with an old effect or plan view. Independent cadences
stay separate per the ownership law: camera meets the frame at the pull;
binding state is device-local, stamped `[semantic-generation,
device-epoch]`; async assets keep their id+revision stamps. At encode, LIVE
incompatibility is pinned NARROW: device-epoch mismatch or missing
lease → the EXISTING refusal road (refusal fill + receipt — no new
vocabulary). The semantic-generation stamp is receipt/twin material
only — the dev-tier twin throws on a stale-generation resolve — never a
live gate: a live generation-equality gate would refuse lawful frames
where semantics changed but this region's binding requirements did not.

### 6g. The frame flow + the change-routing table

Per frame: read upstream facts → mint/collect explicit deltas (semantic
and binding lanes) → IF semantic deltas: reduce + one swap → IF binding
deltas: reconcile bindings → read current camera → traverse plan
fragments + visible arrangement → project with current camera → resolve
payload by stable id through §5d/§6e → encode, submit, retire safe
generations. The negative case is the point: a frame whose only delta is
a region lease resize runs reconcile + re-encode and NOTHING else.

| Change | Arrangement/effects | Plan topology | GPU/encode |
|---|---|---|---|
| Camera pan/orbit | none | none | project current camera; region encode per its own ladders |
| Region lease-rung crossing | none | none | one binding reconcile: retire old + acquire new lease, re-encode required roles |
| Region encode-rung crossing | none | none | re-encode affected roles (already payload-local today) |
| Region background change | none | none | payload update + interior re-encode; kind transition also recomputes that region's interior draw-order |
| Region order/geometry change | one entry delta; touched-set law | none | draw order/geometry from arrangement |
| Region open | one entry insert; touched-set | insert one region fragment; global ONLY if color-mode value changes | slot + lease allocate |
| Region close | one entry remove; touched-set | remove one region fragment; global ONLY if value changes | safe slot + lease release |
| Region shadow on/off | none | replace that region's fragment | reconcile its shadow resources, re-encode |
| Effect parameter change | parameter revision only | none | uniform/binding update |
| Effect kind/nesting change | affected containers via touched-set | replace affected effect fragment; maybe global (value change) | encode through new topology |
| Viewport w/h change | none | none | surface rebind + full visible re-encode (encode-side law) |
| Surface format / capabilities change | none | global transition (variant and/or mode) | rebuild affected pipelines/resources |
| Compositor destroy/recreate (epoch bump) | none — semantic state retained | retained if capabilities equal; global transition if not | all bindings reset, next frame re-acquires |

---

## 7. Entry points — exact

**New namespaces (own namespaces, pure `.cljc` unless noted):**
- `src/app/client/substrate/frame_delta.cljc` — §6a vocabulary +
  classification helpers.
- `src/app/client/substrate/frame_effect_view.cljc` — §6c maintained
  state + event index + oracle projection.
- `src/app/client/substrate/frame_plan_view.cljc` — §6d fragments +
  road values + local validation.
- `src/app/client/substrate/frame_semantic_state.cljc` — §6f reducer.
- `src/app/client/substrate/webgpu/region_bindings.cljs` — §6e owner
  (device-local, `.cljs`).
Fork F-c: merging some of these is legal if the result stays coherent;
note it in NOW. Ownership boundaries outrank filenames.

**Thin hooks in existing files:**
- `renderer.cljs` — replace the gate block (`:3811-3868`): keep
  ledger/changed-families/produce; feed `family-entry-delta` output to the
  reducer; one swap; binding reconcile; encode. The `plan-inputs` map,
  `!frame-plan-inputs`, and the deep-`=` change discovery die. Owns the
  §6e epoch producer: the same-device compositor replacement seam over
  `!compositors-by-device` (`:3453-3460`).
- `scene_runtime.cljs` — the registry write sites (`set-effects!`
  `:303-310` and nesting-changing siblings) mint the keyed §6a container
  delta (thin hook per mutator or one keyed registry journal).
- `region3d_gpu.cljs` — shape-key strike (`:985-992`), paint strike
  (`:1194-1199`), prepare splits outputs into semantic / topology /
  binding / payload lanes, composite-instance writes (`:963-968`) move
  under the slot allocator.
- `frame_graph.cljc` — live road replaced by `frame_plan_view`; batch
  compiler + `validate-plan!` + `stable-hash` retained as oracle;
  `arrangement-regions` rect fallback (`:95-98`) deleted;
  `select-color-mode` byte-unchanged.
- `frame_effects.cljc` — `derive-effect-spans` byte-unchanged as oracle;
  `arrangement-token` (`:247`) and the `full?` door (`:257-258`) die on
  the live path; the declaration diff (`:264-265`) becomes the container
  delta source.
- `compositor_gpu.cljs` — `active-region-leases!` reads binding desired
  state (§6e); action consumption via the §6c projection; composite draw
  selects rows by slot.
- `frame_inputs.cljc` — door strikes (§4.5); ledger gains the §4.4
  counters (binding counters produced per §6e invocation custody).
- the twin — NO new home: extends the EXISTING §5h twin (shipped
  throwing form; `assert-twin-equal!` in `frame_inputs.cljc`, renderer
  twin call site) with the §6d oracle input assembly (regions read from
  the binding owner via a narrow receipt fn) and the §6c/§6d
  projections.

---

## 8. The five decisive scenarios (frozen as tripwires at close; wrong-builds named)

**S1 · the crossing is binding-only.** At 200+ entries with a live
Region3D region: (a) cross exactly one lease quantum by zoom; (b) resize
the viewport. Assert, per leg: screen correct with CURRENT camera
projection; pick still resolves through the current viewport; zero
changed families for this cause, zero entries produced, arrangement
identity unchanged, zero order-comparator calls, zero effect containers
touched, zero plan fragments compiled, zero full plan validations;
leg (a) additionally: exactly one binding update, one lease acquired, one
retired old-before-new, only required roles re-encoded on the SAME frame,
within-quantum zoom motion acquires zero leases; twin equal throughout.
*Wrong-builds named:* suppressing the mint WITHOUT moving lease ownership
passes the counters — the same-frame re-encode assertion + the crossing
golden kill it (stale texture); keying the binding compare on raw
pixel-size passes correctness — the zero-acquisitions-within-quantum
counter kills it; a build where lease-size still reaches any semantic
output passes quiet frames — the twin diverges on the crossing frame
(§6e kill).

**S2 · a genuine one-entry change stays local — including the foreign
interleaving legs (mandatory, F1).** (a) insert an entry into container
A; (b) insert a foreign NON-member between two members of A (clone the
`frame_effects_test.clj:54-73` shape) — A's run splits; (c) remove that
foreign entry — A's runs merge; (d) move an entry from inside A's run
into container B — A merges behind it, B splits, container C untouched.
Assert: exactly one explicit entry delta per action; arrangement updates
only affected keys (order-comparator calls < 64 at n≈260 — measured at
TEST tier by a counting comparator wrapped around `compare-order` in the
harness, never an in-code `scene_tape` counter — MUST-NOT 2); touched
containers ⊆ the six-lookup formula's union, and in legs (b)/(c) container
A is touched despite being in NEITHER chain of the changed entry;
untouched containers and untouched runs preserve identity (`identical?`);
plan fragments compiled = 0 and plan-order nodes visited = 0 (no
topology change in any leg); container declarations inspected = 0 (no
registry write in any leg — the counter that proves no whole-registry
declaration diff ran); no complete
old/new map comparison ran; oracle projection equal after every leg.
*Wrong-builds named:* touched-set = the changed entry's own chains only —
passes (a), dies on (b)/(c); rediscovering deltas by diffing whole maps —
passes every output equality, the comparator/touched counters kill it;
rebuilding a touched container's ENTIRE run set — passes equality, the
run-identity (`identical?`) assertions kill it.

**S3 · parameter vs topology vs the road.** (a) opacity/blur magnitude
change → parameter revision only, zero fragments compiled, container
declarations inspected = 1 (the written container alone); (b) effect
kind added/removed, nesting change → that effect fragment (+ dependents)
only — declarations inspected = 1 for a kind change, ⊆ the moved
container's stack-path subtree for a nesting change, and plan-order
work bounded by the affected subgraph, never the registry or fragment
population; (c) road legs over the VALUE functions: empty scene + first effect
(`:legacy`→`:scene-color/linear`, global transition, full re-sign
receipted); add a region while already linear → count changes, VALUE
unchanged → NO global re-sign; regions present + forced `:legacy` →
THROW preserved; capabilities gain `:copy-present` under legacy →
presentation VARIANT transition without color-mode change (pure tier —
live capabilities are `#{}` today). Assert nested/disjoint/mask/export
semantics against the oracle in every leg. *Wrong-builds named:* treating
any effect change as global — fragment counters kill it; a first/last
boolean — the three-valued precedence legs (region-while-linear no-op,
forced-override, throw) kill it; capabilities wired into
`select-color-mode` — the variant-without-mode-change leg kills it.

**S4 · Region3D topology + slots.** Open region 1 (global transition iff
the road value changes) → open region 2 (ONE fragment insert, no global)
→ reorder regions (entry deltas only; slots STABLE; composite order
follows arrangement; zero fragment compiles) → toggle region 1's shadow
(that fragment replaced, zero arrangement change, shadow resources
reconciled) → flip region 1's background opaque↔transparent (zero
entries produced, zero fragments compiled, ONE region's interior
draw-order recomputed + re-encoded; composite correct in both
directions — golden or partition assertion) → close region 1 → open
region 3 (takes the freed slot only
after safe retirement; holes legal meanwhile; region 2's row never
rewritten — upload counters) → close all (global back iff value
changes). Every fragment insert/remove/replace leg also asserts
plan-order work proportional to the affected subgraph
(`plan-order-nodes-visited` bounded by that fragment's passes + adjacent
edges, never the population). Lease + refusal receipts throughout
(forced tiny budget leg:
refusal receipt + fill, never silent). *Wrong-builds named:* positional
composite-index passes until the reorder — slot-stability kills it; slot
compaction on close passes single-region tests — the hole + row-untouched
assertions kill it; shadow toggle re-signing all fragments — fragment
counters kill it; a uniform-only background move that reuses the
prepared draw-order passes every static golden — the kind-transition
leg's ordering assertion kills it; a whole-graph re-sort per open/close
passes output equality — the order-work counters kill it.

**S5 · epoch and recovery (F2).** Live leg: destroy/recreate the
compositor THROUGH the §6e replacement seam — the seam fires, then the
next `draw-frame!` resolves the NEW compositor from production custody
→ semantic arrangement/effect/plan views
preserve identity (`identical?`); ALL bindings, slots, lease generations
reset; no stale generation survives; next frame re-acquires cleanly;
equivalent capabilities reuse logical fragments. Pure leg:
capability-changing recovery → exactly one declared global transition.
*Wrong-builds named:* bindings keyed by bare region-id without epoch —
pass recreate-with-same-ids while resolving stale views; the epoch-stamp
+ refusal-on-incompatible assertion kills it; resetting semantic state
too — passes visually after rebuild; the retained-identity assertions
kill it; a test-constructed second compositor that bypasses
`!compositors-by-device` — passes every identity assertion while
production custody would still return the destroyed cached compositor;
the through-`draw-frame!` requirement kills it.

Counters ride the existing `frame_inputs` ledger (extended per §4.4) and
must themselves update incrementally (§6d diagnostics law).

---

## 9. Representative goldens (2–3, reuse before minting)

1. Region composite before/after one lease-rung crossing (no omission, no
   stale binding) — extend the existing region bank if a home exists.
2. Nested + disjoint effect ordering through the maintained view — the
   projection equals today's composite (existing effect goldens stay
   byte-identical; add at most one if no existing home covers disjoint
   runs).
3. Region shadow + refusal-fill composite through the topology/binding
   split.

The 44-image bank, the region/seam goldens (47 + 3), and the MSDF
counterexample (RED) stay byte-identical.

---

## 10. MUST-NOTs (real only)

1. NEVER read/require `src/app/server/env.clj`.
2. `scene_tape.cljc`: ZERO edits from this atom. The standing SHA fork
   (`FRAME-RETENTION-NOW.md:11`) is Sid's separate ruling — do not fold
   edits into it.
3. The budget-wedge lifecycle laws (§5 pointer) are behavior-frozen; the
   ONE lawful re-plumb is §6e's desired-set source. No other lifecycle,
   refusal, or retirement change.
4. Batch computations (`derive-effect-spans`, the batch plan road incl.
   `validate-plan!` + `stable-hash`) are never deleted — demoted to
   oracles; oracle results never feed the live path; the twin never
   gates or repairs the live path (its dev-tier THROW on divergence is
   the existing shipped fence — a kill, not a gate; the runtime-loop
   residual is recorded at `FRAME-RETENTION-NOW.md:14`). The §6d
   rect-fallback deletion is the ONE named oracle repair — it removes a
   silent input fabrication, not a computation.
5. No execution clock (frame-idx, wall time) as an input to any delta,
   revision, reducer, or gate.
6. GPU objects never enter pure semantic state or delta payloads — with
   the ONE lawful §5d form: stable system/pool REFERENCES in paint
   (e.g. `:region-system`) are identity handles, opaque to the reducer
   and views; their device internals resolve only at encode. Entry
   deltas carrying old/new entries carry those handles opaquely.
7. Post-strike, NO camera-derived value enters semantic identity at all;
   camera reaches Region3D only through the binding lane (256 ladder)
   and payload lane (1.12× encode ladder).
8. Speed never by omission: every current visual, pick, held-target,
   shadow, refusal, and device-recovery behavior is preserved; skipping
   required work to green a counter is the lying-build class.
9. Existing golden banks byte-identical (§9).
10. The instrumentation/ledger stays until Sid closes this work.

---

## 11. Refusals (named, one line, routed — think wide, build narrow)

- **R1′ — real `GPUDevice.lost`/`uncapturederror` wiring** → board debt
  (F2); the epoch rides compositor/system identity here.
- **R2′ — partial redraw / retained command buffers / patch-driven
  executor** → exists only if a frame profile summons it (decisions.md:
  "today's walk waits as its oracle").
- **R3′ — compositor action sequencing** (`sequence-actions` full sweep,
  `group-for-index` scan — O(n·s²)-shaped) → the PRE-NAMED next suspect
  if frame time stays high after the plan bucket dies; STRUCTURE claim
  only, needs its own receipt before any verdict (investigation fence).
- **R4′ — amortized/async region interior re-encode** (retention R1) →
  its own package; NAMED FORK recorded now: its two-generation composite
  bridge collides with the old-dies-before-new budget law — that package
  must open with this fork, not discover it.
- **R5′ — sub-entry produce granularity** (retention R3) → LATER at the
  next receipt; family grain stays the floor.
- **R6′ — encode-ladder constant tuning** → the felt pass (unchanged).
- **R7′ — Contract-O comparator internals** (retention R2) → LATER; the
  1.9k-comparator receipt dies with the deep-`=` gate regardless.
- **R8′ — boot-path costs** (retention R7) → unchanged board debt.

## 12. Named non-solutions — refused roads (receipt-backed where marked)

- **Identity/token-only plan gate** — RECEIPT-BACKED refusal:
  `bind-entry-ranges` + `validate-plan!` run unconditionally on every
  maintain pass even on structure-key reuse (`frame_graph.cljc:622-635`);
  killing the comparator leaves population work standing, and Half A
  alone leaves every real semantic change population-sized.
- **Bigger lease quantum** — lowers frequency, corrects nothing.
- **Debounce / gesture-end / throttled leases** — alters interaction
  timing to hide the defect.
- **Opaque plan cache** — a maintained view with declared inputs and an
  oracle is required; a cache moves change discovery and stale risk.
- **Suppressing the family mint without moving lease ownership** — stale
  textures; violates indirection-not-omission (S1 kill).
- **A Region3D-only second plan system** — regions ride the same
  fragment/binding architecture future families will.
- **Deleting the batch compilers** — MUST-NOT 4.
- **A generic event bus / Electric clone** — the need is a small
  render-specific vocabulary (§6a).

## 13. Forks seen and written (defaults chosen, per the law)

- **F-a execution representation:** event index (chosen) vs encode-walk
  fused derivation (legal only on a real event-index blocker, noted in
  NOW; both bound by oracle-equality + order-locality).
- **F-b plan identity:** monotonic generation (chosen) vs incrementally
  maintained digest (only if a deterministic-digest consumer is found;
  full `stable-hash` stays oracle-only).
- **F-c namespace granularity:** five new namespaces (chosen) vs merged
  (legal if coherent, noted in NOW).
- **F-d the one live fork door** (raise only if reached, ONE question,
  never a stop): a discovered CURRENT consumer that makes pass/resource
  topology depend on lease width/height or viewport width/height, or a
  composite-level semantic consumer of `shadow?`/`background` — the §2
  enumeration found none.
- **F-e (routed out):** R4′'s two-generation bridge vs the budget law —
  belongs to that package's cut, recorded here so it cannot be
  rediscovered expensively.

## 14. Implementation order — one atom, straight through (internal passes, never gates)

1. **Deltas + one generation:** `frame_delta.cljc`, reducer skeleton,
   arrangement returns its delta, renderer swaps once; old effect/plan
   roads behind temporary adapters so the system stays runnable; the
   deep-`=` discovery dies here.
2. **Region reclassification (Half A):** shape-key + paint strike,
   binding owner, lease road from binding deltas, slot allocator,
   compositor reads binding state, the §6e epoch replacement seam,
   background's two payload consumers (clear + interior draw-order),
   door strikes in `frame_inputs.cljc`,
   rect-fallback deletion + explicit oracle input assembly. After this
   pass the exact C1 action produces zero semantic work — not atom
   completion (Half B still owed).
3. **Effect view:** §6c state, touched-set law, event index, oracle
   projection; the §6a container-delta mint lands at the registry write
   sites; `arrangement-token` door dies.
4. **Plan view:** §6d fragments, two road values, local validation,
   monotonic generation; full validate/hash behind oracle doors.
5. **Compositor integration:** binding-resolved resources, projected
   actions, order/pick/mask/export/refusal/held-target preserved.
6. **Falsification (the implementer's own pass):** run S1–S5 + goldens;
   drive the real desktop path at the known n≈261 scene and the mobile
   route with dozens of instances; counts before timings; confirm the
   27–36ms plan bucket is ABSENT on the crossing action. If total frame
   time stays high, record the next bucket (R3′ is pre-named) — never
   pull it into this atom without a new receipt.

## 15. Definition of done + the expected receipt

Done means ALL of: lease size absent from semantic shape identity and
live plan identity · composite slot + background changes produce zero
semantic entries · no complete old/new input comparison anywhere on the
live road · the crossing action produces zero arrangement/effect/plan
work · a real one-entry change touches only the §6c/§6d affected sets ·
no durable numeric entry positions · plan is stable keyed fragments
with incrementally maintained execution order · bindings owned by
explicit device-local state with epoch stamps · batch
oracles alive and equal · every §5 law green · S1–S5 + goldens green ·
desktop AND mobile taken-path receipts show the plan bucket absent on
the crossing action · no full-frame executor claim without its own
receipt · not called done on a delta token alone.

Expected crossing receipt (desktop, n≈261 — measure the milliseconds,
never invent them):

```
change: region-binding | region-id: <one> | lease: old→new
changed-families [] · produced 0 · arrangement-upserts 0 · removes 0
comparator-calls 0 · effect-containers-touched 0
container-declarations-inspected 0
plan-fragments-touched 0 · plan-order-nodes-visited 0
plan-full-validations 0
region-binding-updates 1 · leases-acquired 1 · leases-retired 1
region-prepared 1 · region-encoded <required roles only>
oracle-divergences 0
```

(`comparator-calls` rides the Run-P road — profile/stack-trap at runtime,
counting-comparator harness at test tier — never an in-code `scene_tape`
counter; the ledger counters are §4.4's, produced per §6e.)

## 16. Close mechanics + the implementer's opening prompt

At close: freeze S1–S5 as tripwires + the §9 goldens (focused suite only;
foreign failures are board debt) · changed-file list diff-derived · one
NOW entry (≤15 lines, self-audit line included) · flip the
FRAME-RETENTION-NOW STANDING line (§4.6) · board line flip · acceptance
is Sid's word alone.

---

**Implementer's opening prompt** (paste into a fresh session):

> **Preflight, before the first prompt:** set permission mode, MCP set,
> and remote-control NOW — never mid-session (prefix-rewrite law).
>
> Implement the FRAME-VIEW/REGION-BINDING correction atom — one pass,
> straight through, per the work-package law.
>
> **Boot (byte-sized; read PRIMARY, sections only — total ~75KB):**
> - `docs/render-engine/FRAME-VIEW-REGION-BINDING-CONTRACT.md` — whole
>   (~50KB). THE contract; §6 design, §7 entry points, §8 scenarios.
> - `docs/render-engine/FRAME-RETENTION-CONTRACT.md` §5d + §5f + §8
>   (~10KB of 40KB) — payload indirection, prepare/shape-rev law,
>   MUST-NOTs that stay law.
> - `docs/decisions.md` "recompute proportional to change" block
>   (~lines 445-544, ~9KB) — the governing laws.
> - `docs/render-engine/FRAME-RETENTION-NOW.md` — whole (5KB) — current
>   runtime state incl. Run P and the twin's shipped form.
> Code by seam only (contract §7 lists every anchor); never whole-file.
> - Region3D S5 receipt vocabulary: `REGION3D-FLOOR-CONTRACT.md:662-678`
>   if S4/S5 receipts need their shape (~2KB).
>
> Build order = contract §14. Your own falsification pass is §14.6.
> Ambiguity → strongest default + a NOW note; a genuine fork (contract
> §13 F-d) is ONE question in the thread file, never a stop. Foreign
> test failures are board debt. Close per contract §16. Acceptance is
> Sid's word.
