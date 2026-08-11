# REGION3D / POINTER — the correction contract (the typed pointer packet)

Cut by: **Claude Fable 5** (`claude-fable-5`), effort high — 2026-08-11, one
pass, one session. Born of Sid driving `?live-atoms=1&region3d=1&seam-demo=1`
(2026-08-11): "there is big drift … the expectation is that the pointer is
anchored to that point … the W/E/R axes are much more confusing than useful."
Pre-cut falsification: one Codex structural round on the assessment (at Sid's
hand) — three findings, all accepted, folded below. The contract's own
fresh-eyes round: RUN 2026-08-11 (Codex, at Sid's hand — touch #1), six
findings, all folded in-session: §4/§5c/§11 F1 ambience reconciliation ·
§7 custody header · S3(iii) projection custody · S4(c) honesty · S5 court
extraction + broadened greps · §8 required hover golden · §13 custody
greps. The round returned ≥2 decision-changing findings, so the
fresh-eyes law stands for the next cut.

---

## 1. What this atom is — scope in plain words

The Region3D interior currently runs pointer math in three coordinate
representations — canvas CSS px, region-local logical units, and
region-local × zoom·dpr (device) — and different handlers cross-pair them.
Separately, the interior camera has no pointer-anchored navigation at all,
one wheel event can reach two cameras, and the gizmo's pickable geometry is
not the geometry it paints.

This atom makes Region3D pointer interaction coherent:

1. **One typed pointer packet.** Every pointer event over a focused region
   resolves once into a packet carrying ALL representations explicitly;
   every consumer declares which representation it takes; a point and a
   viewport in one computation always come from the same representation —
   never a cross-pair. (Codex finding 3, accepted: one undifferentiated
   conversion would merely relocate the mismatch.)
2. **Pointer-anchored navigation.** Wheel dolly keeps the world point under
   the cursor fixed on screen (the ground's §9.4 law, lifted into the
   interior); pan glues the grabbed world point to the cursor exactly.
3. **One wheel court.** A wheel event reaches exactly one camera. The
   routing decision lives at one named seam (`scroll.cljs`); Region3D's own
   canvas wheel listener is deleted.
4. **Gizmo you can hit.** Pick geometry = painted geometry (true segment
   distance over the full drawn span, glass-anchored slop), hover feedback,
   and drag rays built from one representation so the object tracks the
   cursor instead of jumping.

It is NOT: a gesture grammar, an ambience-transfer mechanism, an orbit-feel
redesign, a gizmo affordance/legend redesign, or any touch/pinch work
(refusals §10).

---

## 2. Receipts basis — closed evidence; not re-litigated

Gathered 2026-08-11 (Opus gatherer, index-first; anchors re-quoted below are
the contract's own custody, no re-gather needed). Cutter spot-checked the
decision-changing anchor (`scroll.cljs:23-27`).

**Investigation-fence position:** every defect below is STRUCTURAL —
provable from source, no magnitude claim made or needed. Which mechanism
dominates the drift Sid *felt* is deliberately unadjudicated: the fix
removes all four defects, and D3's standing tripwire (§7 S5) is the
permanent receipt that would attribute any recurrence. No harness probe is
owed before implementation (Codex verdict, accepted).

The defects register — scenarios in §7 trace back to these:

- **D1 — no pointer anchor.** `dolly` mutates only `:distance`
  (`region3d_scene.cljc:617-619`); the wheel handler uses the pointer only
  to gate (`region3d_runtime.cljs:564-570`). Nothing re-anchors navigation
  to the point under the cursor. Contrast: the ground's cursor-anchored
  zoom, `ground.cljs:3647-3652`.
- **D2 — pan cross-pair.** Pan receives deltas in canvas CSS px
  (`canvas-point`, `region3d_runtime.cljs:241-244`) paired with a viewport
  in region-local units (`(:region-size hit)`,
  `region3d_runtime.cljs:540-543` → `region3d_scene.cljc:630-640`), plus a
  silent `[720.0 480.0]` fallback that happens to equal the fixture's
  extent. Units agree only when effective outer scale = 1.
- **D3 — dual wheel dispatch** (structurally present; Codex finding 1,
  accepted, chain closed). The WebGPU canvas is one node
  (`electric_flow.cljc:803`, `:830`); the ordinary wheel flow attaches to
  it (`runtime.cljs:402`) and routes to `ground/handle-wheel!` when the
  ground is active (`scroll.cljs:23-27`, cutter-verified); Region3D
  attaches its own capture-phase listener to the same node
  (`region3d_runtime.cljs:611-618`) and `consume!` is `stopPropagation`
  (`:295-298`), which never suppresses same-node siblings. One wheel event
  over a focused region can dolly the interior AND zoom the ground.
  Contribution to felt drift: unmeasured, and stays so — the fix is
  structural ownership, not attribution.
- **D4 — gizmo defects.** (a) Drag start unprojects the scaled
  `:region-ray-point` (`region3d_runtime.cljs:348-350`, stamped `:238`)
  while every move unprojects the unscaled `:region-local` (`:546-547` →
  `:412`) through the same scaled-viewport camera — at any zoom·dpr ≠ 1
  the current ray is wrong. (b) Paint and pick geometry are not identical:
  the GPU draws each axis from the pivot over the full span
  (`region3d_gpu.cljs:277-279`); picking samples only 0.30…1.0 of it as 8
  screen-space disks of fixed device radius
  (`region3d_scene.cljc:667-668`, `:869-879`). The exact dead interval is
  viewport-dependent (Codex finding 2 wording, accepted); hover feedback
  is definitely absent. (c) Fixed device-px radii shrink in glass terms as
  dpr grows — 10 device px is 5 CSS px at dpr 2.

---

## 3. Supersessions — exact

- `FRAME-VIEW-LOWER-RESOLUTION-CONTRACT.md` MUST-NOT 5 ("the pick chain:
  zero edits") was that atom's negative space and expired at its
  SID-ACCEPTED close (2026-08-11). THIS contract lawfully opens
  `region3d_runtime.cljs:205-255` and the `region3d_scene.cljc` pick
  helpers. The pick-authority LAW it protected (§4 below) is preserved,
  not superseded.
- `region3d_scene.cljc` `dolly`/`pan` (`:610-640`) are superseded by the
  anchored versions in the new namespace and deleted (gatherer: no callers
  outside `region3d_runtime`; implementer re-verifies by grep at build).
- `scene/scale-ratio` (`region3d_scene.cljc:972`) is dead code (no caller)
  — delete in passing or leave; not load-bearing either way.

---

## 4. Binding laws — pointers, never restatements (each names the scenario that fails under its violation)

- `FRAME-RETENTION-CONTRACT.md` §5g two-ladder split: the admission rung is
  a BUDGET door, never a camera door — camera/pick authority is the
  desired (unclamped) size. The packet's device representation is
  `region-size × effective-scale`, NEVER the granted lease/rung size.
  Violation fails S3(iii)'s projection-custody leg — a rung camera lands
  projected points at the rung ratio, off the device-px expectation (ray
  legs alone cannot see it; S3's honesty note).
- `REGION3D-FLOOR-CONTRACT.md` §5.8 one pick road: every pointer consumer
  resolves through `ss/pick → resolve-region-pick → pick-region`; §5.2
  coordinate law (right-handed, +Y up, −Z forward, quaternion `[x y z w]`).
  A second ray road or cached-hit side channel fails S3/S4, which drive the
  one road only. Gizmo slop stays "screen-metric, declared radius"
  (G §4.7) — this atom pins the declaration unit (§6 F4).
- `docs/decisions.md` "The spatial model & the representation ladder"
  (2026-08-10): ambient space owns naked camera gestures; machinery
  arrives by lived want. Reconciliation pinned (fresh-eyes finding 1,
  2026-08-11): the reservation covers NAKED gestures — the floor
  contract's own words are "naked drag/wheel AT GROUND stays the world
  camera's", riding T2's dispatch precedence (session > chrome > legacy;
  sessions OWN their input). A wheel over a FOCUSED region is
  session-addressed input, not a naked gesture; custody: `on-wheel`
  (`region3d_runtime.cljs:564-570`) already routes it to the interior
  today — this atom removes the dual dispatch, it does not newly grant
  the interior the wheel. If Sid rules the ambience law reaches focused
  regions too, the flip is ONE line at the §5c court — that
  reversibility is why the court exists. This atom builds NO ambience
  machinery; the wheel court (§5c) is the single seam where that grammar
  lands later. Violation (a capture-priority framework, mode flags
  beyond today's focus) fails the §9.7 cross-check, no scenario — it is
  scope law.
- `FRAME-VIEW-LOWER-RESOLUTION-CONTRACT.md` §6f encode mechanics: interior
  renders the whole attachment, composite maps full uv — geometry-neutral;
  nothing here touches encode/composite. Violation fails the golden leg
  (§8: existing bank byte-identical).
- CLAUDE.md Token Economy + work-package law govern the implementer
  session. Session law — no scenario attaches.

---

## 5. The design

### 5a. The typed pointer packet — `region3d_pointer.cljc` (new, pure)

One builder resolves a pointer sample (a DOM event or the wheel flow's
sample map — both carry canvas-rect-relative CSS x/y; custody:
`events.cljs:36-38` and `canvas-point`, `region3d_runtime.cljs:241-244`,
already produce the same space) plus the pick return into:

```clojure
{:css-point       [x y]     ;; canvas CSS px
 :region-local    [lx ly]   ;; slot-local logical units (ss/pick's :region-local)
 :region-device   [dx dy]   ;; region-local × S
 :viewport-local  [w h]     ;; (:region-size hit) — logical
 :viewport-device [W H]     ;; viewport-local × S — the DESIRED size (pick/camera authority)
 :scale           S}        ;; effective outer scale × dpr
```

`S = dpr × ground-zoom × container-scale`. Today container-scale = 1 and
the road is `current-viewport-scale` (`region3d_runtime.cljs:205-209`,
fed by the io map, `render.cljs:976-979`). The generalization road:
`ss/pick` already inverts the container affine
(`scene_store.cljc:429-437`) and MAY stamp `:region-scale` on the
`:route :region3d` return (≤5 lines; the store stays session-free — scale
is geometry, not camera). Never a parallel derivation of the affine.

**The law: consumers declare their representation; point and viewport in
one computation come from the SAME representation — never a cross-pair.**
Ray construction consumes device point + device viewport (the desired-size
authority pairing already in force in `resolve-region-pick`). Screen-space
hit tests consume device points with glass-declared radii (§6 F4). The
missing-pick pan fallback `[720.0 480.0]` is deleted — a pan step with no
resolvable packet is a no-op, never a guess.

### 5b. Pointer-anchored navigation (pinned algorithms)

Both operations use the drag/gesture-START camera for ray casting — frozen
matrices, no feedback loop.

**Anchored dolly.** Anchor `P` = BVH hit point under the cursor if any,
else cursor-ray ∩ plane(pivot, view-normal); ray parallel to the plane →
plain distance scale (today's behavior). With `s = exp(deltaY × 0.001)`
(today's constant):

```
pivot' = P + s·(pivot − P);   distance' = s·distance;   yaw/pitch unchanged
```

A similarity transform of the rig about `P` — `project(P)` is invariant
exactly. Wheel coalescing (`events.cljs:49-53`: summed `:dy`, last `:x :y`)
anchors the summed delta at the last position: accepted, one sample per
frame. `material/canonical-view` clamps, if any, apply after — the
implementer preserves whatever it enforces today.

**Glued pan.** At pan start capture `P₀` (hit point, else pivot-plane
point) and plane Π = (P₀, view-normal at start). Each move, cast the
cursor ray through the START camera, `Q = ray ∩ Π`, then
`pivot' = pivot₀ + (P₀ − Q)`. Translation-only rig move — the grabbed
point reprojects to the cursor exactly. No `units-per-pixel` heuristic
survives anywhere.

### 5c. One wheel court — `scroll.cljs:23` (thin hook)

`scroll-consumer` becomes the only wheel dispatcher on the canvas:

```clojure
(if (region3d/wheel! wheel-evt)          ;; truthy iff focused region under cursor consumed it
  nil
  (if (ground/ground-active?) (ground/handle-wheel! wheel-evt) …existing…))
```

`region3d/wheel!` resolves focus + hit from `!session` and applies the
anchored dolly. Region3D's own canvas `"wheel"` listener registration
(`region3d_runtime.cljs:617`) is DELETED — one owner by construction, not
by suppression. Dependency direction verified at build: `scroll.cljs`
requires `region3d-runtime`; nothing in the runtime's require chain reaches
back into `runtime/scroll`.

Default: **focused-region-wins** (§11 F1 — session-addressed input under
T2's dispatch precedence, not a naked ambient gesture; reconciliation
pinned in §4). This court is the named seam
where the settled ambience grammar lands later; nothing else about gesture
ownership is built now. Pointer/mouse drag duality is NOT rewired in this
atom: `consume!`'s `preventDefault` on pointerdown suppresses compat mouse
events per the Pointer Events spec (browser behavior, not our code); the
S5 tripwire counter stands guard — if it ever fires on a drag, that is one
board-debt line, not silent scope creep.

### 5d. Gizmo — same representation, pick what you paint, hover

- **Rays.** `start-ray` and every `current-ray` are built by one function
  from the packet's device pairing, through the drag-START camera. The
  `:region-ray-point` / `:region-local` split dies.
- **Picking.** Axes: project the DRAWN segment endpoints (pivot → pivot +
  axis·scale, exactly the WGSL geometry `region3d_gpu.cljs:277-279`) and
  test true point-to-segment distance ≤ slop. Rings: point-to-polyline
  distance over the existing sample chain (covers between-sample gaps).
  Plane handles: unchanged (center-radius matched the GPU — Codex
  finding 2). Ranking stays distance-then-stable-id
  (`region3d_scene.cljc:869-879`'s ordering law).
- **Slop.** Declared in CSS (glass) px, converted once: `slop-device =
  slop-css × dpr`. Zoom never scales slop — pointer accuracy lives on the
  glass (§11 F4). Values stay 10 css px gizmo / 8 css px glyph.
- **Hover.** On non-dragging move, the gizmo pick result lands in the
  session as `:gizmo-hover <handle-id|nil>`; the gizmo uniform gains one
  hover field and the WGSL brightens the hovered element
  (`region3d_gpu.cljs:255-360` shader, `:963-981` uniform write — the ONLY
  gpu-file edits).
- Uniform-scale screen-delta branch (`region3d_runtime.cljs:443-447`):
  deltas become packet `:region-device`, constant retuned by feel at build.

### 5e. What does NOT change — the negative space, stated positively

- Encode/composite/lease/rung machinery, refusal road, reserve law, the
  four numeric governors: byte-untouched.
- `ground.cljs` camera math and `events.cljs` sampling: zero edits — the
  court adapts to the existing sample shape.
- The pick-road ladder order (gizmo → objects → glyph → background) and
  BVH internals: unchanged; only handle geometry/metric changes.
- Gizmo painted geometry: unchanged (hover brightening aside) — the
  existing golden bank stays byte-identical.
- W/E/R mode keys and mode semantics: unchanged (affordance redesign is a
  refusal).

---

## 6. Entry points — exact

- **NEW** `src/app/client/workspace/region3d_pointer.cljc` — packet
  builder, anchored dolly, glued pan, segment/polyline distance, slop
  conversion. Pure; JVM-testable.
- `src/app/client/workspace/region3d_runtime.cljs` — `canvas-point`/
  `event-points`/`pick-at` (`:241-255`) produce the packet;
  `resolve-region-pick` (`:205-239`) consumes it (device pairing, meaning
  unchanged); `start-drag!`/`update-gizmo-preview!`/`settle-gizmo!`
  (`:337-470`) one-representation rays + start camera; `on-pointermove`
  (`:525-563`) glued pan + hover write; wheel listener deleted from
  `boot!` (`:600-627`); new `wheel!` export.
- `src/app/client/workspace/runtime/scroll.cljs:23` — the court branch,
  extracted as a pure callable that the `m/reduce` closure invokes (thin
  hook, ≤6 lines at the seam; S5 drives the extracted callable, never a
  hand-built copy).
- `src/app/client/substrate/region3d_scene.cljc` — `axis-handles`/
  `ring-handles`/`pick-screen-handles` (`:645-738`, `:866-944`) →
  drawn-span segment metrics + glass slop; `dolly`/`pan` (`:610-640`)
  deleted.
- `src/app/client/substrate/webgpu/region3d_gpu.cljs` — gizmo uniform
  hover field + WGSL brighten branch ONLY (`:255-360`, `:963-981`).
- `src/app/client/workspace/scene_store.cljc:429-437` — MAY stamp
  `:region-scale` (≤5 lines, session-free).
- Session state: `:gizmo-hover` on the focused region row.

---

## 7. The five decisive scenarios (frozen as tripwires at close; wrong-builds named)

Pure JVM tests in the new namespace's home prove the MATH; that the
production handlers RIDE that math is a separate custody question
(fresh-eyes finding 2: a build can green perfect helpers and leave the
`.cljs` handlers on the old roads) answered by the §13 custody greps +
Sid's felt pass — helper-green alone is never close evidence. ε: 0.5
device px for reprojection; 1e-6 for ray equality; 1e-3 world units for
analytic deltas. Every scenario runs at S ∈ {1.0, 2.0, 3.5} unless noted —
S = 1 is the case that hid every defect.

- **S1 anchored dolly** (D1): camera fixture, cursor pinned OFF-center at
  (0.23·W, 0.71·H), anchor from ray∩pivot-plane; three successive wheel
  steps; assert `|project(P_original) − cursor| ≤ ε` after EACH step and
  after the compound. Wrong build named: center-dolly passes a centered
  cursor (hence off-center pin); re-deriving the anchor from the CURRENT
  camera each step hides accumulated drift (hence the compound assert
  against the ORIGINAL P).
- **S2 glued pan** (D2): grab at g₀, four successive moves ending at g₁
  (Δ = [180, −120] device px); assert `project(P₀) = cursor ≤ ε` at each
  step, S ∈ {1.0, 2.75}. Wrong build named: today's units-per-pixel
  heuristic passes S = 1 (hence 2.75); per-move current-camera glue
  feedback passes one step (hence four).
- **S3 gizmo ray/delta + projection custody** (D4a): at S = 2, (i)
  start-ray and current-ray built for the SAME cursor position are equal
  ≤ 1e-6; (ii) a known device-px cursor travel along the +X screen axis
  produces the analytic world translate-delta ≤ 1e-3, computed under the
  desired-size camera; (iii) projection custody — `project` of a fixture
  world point under the packet camera lands at the analytically expected
  DEVICE px coordinate, S ∈ {2.0, 3.5}. Honesty note (fresh-eyes
  finding 3): ray math is scale-INVARIANT — `camera-matrices` sees the
  viewport only as aspect, `ray-from-region-point` only as x/W y/H
  (`region3d_scene.cljc:560-588`) — so any consistently-paired scale
  yields the identical ray; (i)+(ii) kill cross-pairs (the actual D4a
  defect: scaled-start/unscaled-move fails (i), a delta cross-pair —
  device travel over a local viewport or vice versa — fails (ii)), and
  only (iii) is scale-SENSITIVE, because projected px and glass slop
  meet in screen space: a consistent local/local pairing lands `project`
  at 1/S of the expectation, a rung-sized camera lands it at the rung
  ratio — both named wrong builds pass (i)+(ii) and die on (iii).
- **S4 pick what you paint** (D4b/c): fixture where an axis projects
  ≈140 device px: (a) cursor at 15% along the drawn axis (the old dead
  interval) hits; (b) cursor at the midpoint between two of the old
  8 sample positions hits; (c) boundary honesty — a point 0.4 px inside
  the slop distance hits and 0.4 px outside misses; (d) hover: move over
  the axis sets `:gizmo-hover` to its handle id, move off sets nil;
  (e) slop at dpr 2 equals 10 css px = 20 device px. Wrong builds named:
  every sampler this code has ever run dies on (c) — 8 disks, full or
  partial span, overestimate the inside point's distance by ≥2 px (disk
  union only overestimates, so the kill threshold is sample spacing
  > ~5.6 px at these radii). Honesty note (fresh-eyes finding 4): a
  sampler densified past ~26 full-span disks converges under ±0.4 px and
  passes (a)–(c) — the boundary pair pins BEHAVIOR, not algorithm; the
  segment metric stays mandated in §5d because it is exact and simpler
  than dense sampling, not because this tripwire can tell them apart.
  Keeping device-fixed radii passes (a)–(d) — leg (e) kills it.
- **S5 one wheel owner** (D3): drive the court as a pure routing call:
  (i) sample over a focused region under cursor → region view changes,
  ground camera untouched, consumed truthy; (ii) open ground, no focused
  hit → `ground/handle-wheel!` road taken; (iii) a dev-mode dual-dispatch
  counter (increments when >1 camera road mutates off one input sample)
  reads exactly 1 per sample across (i)+(ii). The routing callable S5
  drives is the ONE `scroll-consumer` invokes (§6 extraction) — a
  directly-invoked router that production never calls greens the pure
  legs while every real wheel routes to ground (fresh-eyes finding 6).
  Wrong builds named: keeping Region3D's own canvas listener while the
  court also consumes, and re-registering it as `(name :wheel)` or any
  other spelling that evades a string-literal grep — so the source-shape
  legs are: (1) `grep -in 'wheel'
  src/app/client/workspace/region3d_runtime.cljs` — the close receipt
  QUOTES the hit list and every hit is the `wheel!` export chain, no
  listener registration under any spelling; (2) `grep -n 'wheel!'
  src/app/client/workspace/runtime/scroll.cljs` — shows the require-side
  call inside `scroll-consumer`. The counter ships in dev builds as the
  standing tripwire (fence law: permanent receipts carry attribution).

---

## 8. Representative goldens (reuse before minting)

The existing Region3D golden bank: **byte-identical** (hover is off in
golden scenes; painted geometry unchanged). Plus ONE new REQUIRED golden
(fresh-eyes finding 5): hover forced on a named axis handle via the
fixture, appended to the bank, prior bytes unchanged, per the standing
golden law. S4(d) alone is state-only — a build that never writes the
hover field into the gizmo uniform (`region3d_gpu.cljs:963-981`) or
whose WGSL branch never brightens passes S4(d) and a hover-off bank; the
appended image is the visible road's permanent receipt
(session → uniform → WGSL branch).

---

## 9. MUST-NOTs (real only)

1. NEVER read/require `src/app/server/env.clj`.
2. The packet's device representation is desired-size only
   (`region-size × S`); the encode rung / granted lease NEVER enters
   pointer math (FRAME-RETENTION §5g; fails S3(iii)).
3. Lease/binding/budget machinery, the four numeric governors, refusal
   road, lifecycle semantics: zero edits. `region3d_gpu.cljs` edits are
   the gizmo hover uniform + WGSL branch only.
4. `scene_tape.cljc`: zero edits.
5. One pick road (floor §5.8): no second ray/unprojection road, no
   cached-hit side channel; the new namespace's math is CALLED from the
   existing chain, never a bypass around it.
6. `ground.cljs` camera math, `events.cljs` sampling: zero edits.
7. No gesture-grammar machinery: no ambience transfer, no
   capture-priority framework, no new mode state beyond today's focus +
   `:gizmo-hover`. The court's default is a default at one seam.
8. No predicted/optimistic camera state: every mutation derives from the
   session view + packet at event time (falsification-review law: no
   write→render path without its truth reconciliation).

Cross-check (lens 4): MUST-NOT 3 × §5d hover — the carve-out is explicit
(uniform + branch); MUST-NOT 5 × §5a builder — the builder consumes the
pick road's return, it never re-picks; MUST-NOT 7 × §5c — focused-region
routing uses ONLY the pre-existing `:focused-region` state; the expired
lower-res pick-chain MUST-NOT is superseded in §3, not violated.

---

## 10. Refusals (named, one line, routed — think wide, build narrow)

- Orbit-pivot re-anchoring on click (orbit around the picked point) —
  felt/design decision; the package felt pass will surface it → LATER,
  design track.
- Numeric readout / snapping / typed magnitudes for gizmo drags ("move by
  exactly this much") → LATER.
- W/E/R affordance UI (legend, onboarding, mode indicator) → design
  track, LATER.
- Touch/pinch interior navigation → LATER (mobile pressure leg elsewhere
  is already RECEIPT PENDING).
- Ambience-transfer machinery (diving bell, gesture grammar) → arrives by
  lived want at the §5c seam (decisions.md law), never in this atom.
- `editing_runtime.cljs:287-298` builds pick points from raw `clientX`
  with no rect subtraction — same defect class, different surface → one
  board-debt line, not this atom.
- Full outer-affine generality (container rotation/skew in the packet) →
  scale+translate today; the rotation fork is written (§11 F6), LATER.

---

## 11. Forks seen and written (defaults chosen, per the law)

- **F1 wheel owner:** focused-region-wins (today's intent, made exclusive)
  vs ambient-wins (the settled spatial model's eventual grammar). DEFAULT:
  focused-region-wins — a focused region's wheel is session-addressed
  input (T2 precedence; §4 reconciliation), not a naked camera gesture;
  if Sid rules the ambience law reaches focused regions too, the flip is
  ONE line at the §5c court. The court is where ambient law lands later.
- **F2 pan glue plane:** grab-point plane vs pivot plane. DEFAULT:
  grab-point plane on a BVH hit; pivot-plane fallback on miss.
- **F3 dolly anchor on miss:** pivot-plane point under cursor (DEFAULT)
  vs unanchored center dolly. Empty-space wheel still anchors.
- **F4 slop unit:** glass-declared (css × dpr, DEFAULT — pointer accuracy
  is a property of the glass) vs device-fixed. Zoom never scales slop.
- **F5 hover paint:** uniform field + WGSL branch (DEFAULT, smallest) vs
  CPU geometry recolor.
- **F6 outer affine:** packet carries scalar S today (scale ∘ dpr);
  container rotation/skew would need a 2×3 affine in the packet — routed
  LATER, not needed by any current fixture.

---

## 12. Implementation order — one atom, straight through (internal passes, never gates)

1. New namespace + pure math; S1–S4 written first, red → green.
2. Runtime rewiring: packet through `resolve-region-pick`, drag state,
   gizmo rays, hover; delete the pan fallback and the ray-point split.
3. The wheel court + listener deletion; S5 + counter.
4. GPU hover uniform + branch.
5. The one implementation adversarial check: drive the real seam demo —
   wheel on/off objects at zoom ≠ 1, pan glue, each W/E/R handle at
   dpr ≥ 2 — fix what surfaces, then source freeze.

## 13. Definition of done + close mechanics

S1–S5 frozen as tripwires; existing goldens byte-identical + the hover
golden appended (§8); the S5 source-shape legs quoted; the custody greps
below clean; dual-dispatch counter live in dev builds; focused suite
green (foreign failures = board debt); changed files derived from
`git diff --name-only`; NOW ≤15 lines; board pointer flipped. Acceptance =
Sid drives the seam demo and says so — the felt receipt (pointer stays
glued, gizmo hittable and hoverable) IS the close, and it is also the
runtime custody leg (fresh-eyes finding 2).

**Custody greps** (source-shape close receipts; quoted, not just run):
dead roads gone — `units-per-pixel`, `:region-ray-point`, `720.0 480.0`
grep EMPTY in `region3d_runtime.cljs`; `(defn dolly` / `(defn pan` grep
EMPTY in `region3d_scene.cljc`; live road in — `region3d-pointer` appears
in `region3d_runtime.cljs`'s requires AND inside each §6-named handler
body; `scroll.cljs` requires the runtime and calls `wheel!` inside
`scroll-consumer` (S5 legs 1–2).

**Implementer's opening prompt** (paste-ready):

> Preflight: set permission mode / remote-control / MCP BEFORE this
> prompt; no mid-session toggles.
> Boot docs (bytes): `docs/render-engine/REGION3D-POINTER-CONTRACT.md`
> (~28KB, PRIMARY — carries all anchors + the folded fresh-eyes round;
> no re-gather) · `REGION3D-FLOOR-CONTRACT.md` §5.2 + §5.8 only (~4KB) ·
> work-package skill. Under 35KB total — boot and build.
> You own the whole atom: contract §12 order, straight through. Source is
> read by seam (skeleton grep, scoped windows). Ambiguity → strongest
> default + note; a genuine fork → ONE question. Freeze, close receipt
> per §13, terminal NOW.
