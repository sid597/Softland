# FRAME-VIEW / LOWER-RESOLUTION — the correction contract (the admission rung)

Cut 2026-08-10. **Cutter: Fable 5 (`claude-fable-5`), effort max** — effort
per Sid's commissioning line; the two-second check at touch #1 is the session
transcript, never this header alone (the E1 drift precedent stands).

**Status: CUT — the ONE bounded fresh-eyes falsification round (Codex-class,
at Sid's hand) is pending; then Sid touch #1.** No recut exists; author
repairs in-session after the round.

**Input basis:** Sid's fullscreen re-wear (felt receipt,
`FRAME-VIEW-REGION-BINDING-NOW.md:22`) · the W4 pool repair receipt
(`NOW.md:19-21`, `region-reserve-preserved? true`) · this session's
read-only gatherer sweep (2026-08-10, Opus-class, every anchor quoted
verbatim from the working tree) · PRIMARY reads of
`FRAME-VIEW-REGION-BINDING-CONTRACT.md`, `REGION3D-FLOOR-CONTRACT.md`
§5.6/§5.8/S5, `FRAME-RETENTION-CONTRACT.md` §5g, `docs/decisions.md`
"The spatial model & the representation ladder".

**Supersedes (exact edits in §4):** the refusal TRIGGER in
REGION3D-FLOOR-CONTRACT §5.6 · adds the SECOND lawful re-plumb to
FRAME-VIEW-REGION-BINDING-CONTRACT §5 budget-wedge / MUST-NOT 3 · amends
FRAME-RETENTION-CONTRACT MUST-NOT 6's "refusal road byte-untouched" by
exactly that trigger. Nothing else moves.

---

## 1. What this atom is — scope in plain words

At maximum fullscreen workspace zoom, a Region3D's desired lease
(`ceil(w·zoom·dpr)`, canvas-clamped, 256-quantized) prices near the whole
shared 512MB pool — 56 bytes/px across four targets — so admission fails
and the region draws the declared refusal fill (checkerboard + corner
glyph). Sid's re-wear: continuous mesh/text/ink visibility at that extreme
is not accepted; a checkerboard where the material was is a refusal worn
too early.

The correction: **when the desired lease does not admit, step down a
declared admission-rung ladder and acquire the sharpest lease that DOES
admit** — the region renders its CURRENT frame into the smaller lease and
composites it across the full rect, visibly worn, honestly receipted.
Refusal remains the floor rung, exactly as today, when no rung admits.
This is the bottom rung of the representation ladder made real
(decisions.md, settled 2026-08-10: material is "budgeted, degradable,
honestly refusing (the refusal fill stays, as the floor rung)"; "the rung
is WORN visibly"). The in-codebase degrade precedent is the blur road
(`try-acquire-target!` — degrade on budget refusal instead of killing the
frame, stateless per-frame recovery); this contract copies its degrade
shape and REPAIRS its honesty flaw locally (§3, §6e).

**One new decision, one place:** which lease KEY to request when the
desired one won't admit. It lives ahead of the release/acquire loop in the
compositor, computed by a new pure namespace. Everything upstream of the
binding lane — semantic identity, plan fragments, arrangement, effect view,
pick road, camera math, encode mechanics — is untouched by construction,
and the scenarios prove it stays untouched.

**What this atom is NOT:** no cap/constant change of any kind (§10.2) ·
no change to the lease lifecycle functions or their order/refusal/
retirement semantics · no preemption or rebalancing of other regions'
held leases · no progressive/async refinement (R4ᴸ) · no encode-ladder
retune · no new semantic door, shape-key field, or plan-signature input ·
no pick-road edit · no blur-road repair beyond naming its debt (R7ᴸ) ·
no representation-ladder machinery beyond this one rung (the ladder
section itself says: "nothing here changes the current frame-view
correction's scope" — this cut takes that literally in both directions).

---

## 2. Vocabulary pin — three ladders now exist; never conflate them

- **Allocation quantum** — `region-lease-quant 256` / `region-lease-max
  4096` (`compositor_gpu.cljs:17-18`): sizes the lease KEY. Unchanged.
- **Encode rung** — the 1.12× geometric camera door
  (`frame_inputs.cljc:11,173-181`): quantizes the view-key pixel-size term;
  feeds ONLY `scene/camera-matrices` (aspect/lens), never sizes an
  attachment. Unchanged.
- **Admission rung (NEW)** — a divisor `d ∈ {1, 2, 4, 8}` applied to the
  desired pixel-size before quantization, chosen per region per frame by
  budget admission math. `d = 1` is sharp; `d > 1` is WORN; no rung
  admitting is the existing refusal. The word "rung" unqualified in this
  document means the admission rung.

The gatherer's naming-collision finding is the reason this section exists:
"rung" already meant two different things in code.

---

## 3. Receipts basis — closed evidence; not re-litigated

- **The felt receipt** — `FRAME-VIEW-REGION-BINDING-NOW.md:22`: maximum
  fullscreen workspace zoom reaches the declared refusal fill; acceptance
  open. FELT, closed (Sid's re-wear is the receipt).
- **The wall's arithmetic** — STRUCTURE, verified: desired =
  `ceil(w·zoom·dpr)` clamped to canvas backing (`renderer.cljs:3787-3798`),
  quantized (`compositor_gpu.cljs:493-498`); lease bytes = rgba16float 4× +
  depth24plus 4× + rgba16float 1× (+ 2048² depth32float while a
  shadow-casting light exists) = 56 bytes/px (`compositor_gpu.cljs:523-529`);
  pool = `default-pool-budget-bytes` 512MB SHARED with the whole frame road
  (`compositor_gpu.cljs:19-25`). A 4K-canvas fullscreen lease alone prices
  ~446MB. No profile is needed: admission failure at that size is byte
  arithmetic, and the refusal fill on screen is the taken-path receipt.
- **The reserve repair** — `NOW.md:19-21` + `verifier.cljs:3874-3897`:
  Region3D admission preflights the whole lease
  (`ensure-target-capacity!`, `compositor_gpu.cljs:192-202,585-587`) and
  never reclaims the free frame reserve (`acquire-region-target!`
  `:preserve-free? true`, `:510-521`). RECEIPT
  (`region-reserve-preserved? true`), closed. This contract builds ON that
  law and must not weaken it.
- **The blur precedent + its named flaw** — STRUCTURE, verified:
  `try-acquire-target!` (`compositor_gpu.cljs:807-817`) returns nil on
  budget refusal and the chain keeps the sharpest source it holds
  (`:832-851`); recovery is stateless — every frame re-attempts the full
  ladder. FLAW: the `:blur-projections` receipt carries the DECLARED
  projection minted before the attempt; achieved levels are never written
  back — the only degrade trace is the pool's `:refusals` vector. This
  contract's receipts are minted POST-admission (achieved, never intent);
  the blur road's own repair is routed R7ᴸ, not folded in.
- **Pick's resolution-independence** — STRUCTURE, verified twice: the
  resolver rebuilds an EXACT camera at `region-size × (zoom·dpr)`
  (`region3d_runtime.cljs:205-233`, in-source comment: "Pick is exact even
  while the retained interior camera is held inside an encode rung");
  `ray-from-region-point` divides by that camera's `:viewport`
  (`region3d_scene.cljc:560-566`); no lease dimension reaches the chain.
  §5g's pick law ("pick resolves through the CURRENT viewport, never a
  quantized one") already governs.
- **Composite geometry-neutrality** — STRUCTURE with one open inference:
  `encode-interior!` issues no `setViewport`/`setScissorRect` (grep: zero
  hits), so the pass covers the whole attachment; the composite maps the
  whole texture onto the rect (`out.uv = uv`,
  `region3d_gpu.cljs:376-384`) through a linear sampler (`:640-641`).
  Today's quantize-UP already renders at a texel density ≠ rect density,
  so a smaller attachment changes DENSITY, not geometry — this is the
  gatherer's flagged inference, and it is an implementer verify-point
  (§14.1) plus the L1 golden's job, never assumed silently.

The fresh-eyes round challenges these only with equal taken-path evidence
(investigation fence).

---

## 4. Supersessions — exact

1. **REGION3D-FLOOR-CONTRACT §5.6, the REFUSAL sentence — trigger
   amended.** Dead reading: "when a lease would exceed the budget cap →
   refusal receipt, region pass halts, composite draws the refusal fill."
   New law: when the DESIRED lease does not admit, admission walks the
   rung ladder (§6a); the refusal receipt + fill + halted pass fire
   exactly as before **iff no rung admits**. The refusal road's mechanics
   — receipt shape, `:last-region-refusal`, fill pipeline, never-a-silent-
   skip — are byte-frozen.
2. **FRAME-VIEW-REGION-BINDING-CONTRACT §5 (budget-wedge bullet) +
   MUST-NOT 3 — a SECOND lawful re-plumb is added.** The first (§6e
   desired-set source) stands. The second: a desired→granted lease-key
   mapping (§6b) runs ahead of the release/acquire loop in
   `active-region-leases!`. The lifecycle FUNCTIONS
   (`acquire-region-lease!`, `release-region-lease!`,
   `ensure-target-capacity!`, `acquire-region-target!` and its
   `:preserve-free?` policy) and their order — old dies before new,
   `onSubmittedWorkDone` retirement, idempotent destroy — stay
   behavior-frozen.
3. **FRAME-RETENTION-CONTRACT MUST-NOT 6** ("lease lifecycle, refusal
   road, and every budget-wedge behavior are byte-untouched") — amended by
   exactly the two lines above, nothing else.
4. **`FRAME-VIEW-REGION-BINDING-NOW.md:22`** (acceptance-open line) —
   flips at THIS atom's close; the flip is the implementer's close
   write-set, not this cut's.

Everything else in all three contracts stands whole — including frame-view
S1's "within-quantum motion acquires zero leases", which this atom's L4
re-asserts at a worn rung.

---

## 5. Binding laws — pointers, never restatements (each names the scenario that fails under its violation)

- `docs/decisions.md` "The spatial model & the representation ladder"
  (settled 2026-08-10): degradable-before-refusing, refusal fill as floor
  rung, rung worn visibly, device limits price today's rung never the
  ceiling. Violation = silent wear or a skipped floor → fails L1 (glyph +
  receipt) and L5 (floor).
- `FRAME-VIEW-REGION-BINDING-CONTRACT.md` §6e lease-resize road +
  invocation custody (fails L4) · §6f narrow live-incompatibility pin —
  device-epoch mismatch or MISSING lease refuses; a worn lease is PRESENT
  and never trips it (fails L1) · MUST-NOT 5 no execution clock (fails
  L3's stateless-recovery legs) · MUST-NOT 8 speed never by omission
  (fails L1's content assertion).
- `REGION3D-FLOOR-CONTRACT.md` §5.6 lease lifecycle/M10 one-owner law
  (fails L1/L3 lease receipts) · §5.8 one pick road (fails L2) · S5's
  lifecycle receipt vocabulary (L1/L3/L5 extend it).
- `FRAME-RETENTION-CONTRACT.md` §5g two-ladder split — camera enters
  region derivation only through the versioned ladders; the admission rung
  is a BUDGET door, not a camera door: camera reaches it only through the
  already-laddered desired size (fails L4's door assertion) · §5g pick
  substitution law (fails L2).
- The reserve law (`NOW.md:19-21`, in-source rationale
  `compositor_gpu.cljs:510-521`): a worn or sharp region lease never
  reclaims the free frame reserve; mandatory `frame/group-output/*` always
  admits (fails L1's reserve leg).
- CLAUDE.md Token Economy + work-package law govern the implementer
  session. Session law — no scenario attaches.

---

## 6. The design

### 6a. The admission-rung ladder — pure math, `region_rungs.cljc`

Given: desired pixel-size `[pw ph]` (the EXISTING value: ceil'd,
canvas-clamped — computed at `region3d_gpu.cljs:1037-1050`, unchanged),
`shadow?`, the region's currently-held lease `{key bytes}` or nil, and a
pool snapshot `{reserved-bytes budget-cap-bytes}`.

For each divisor `d` in `1, 2, 4, 8` in order:
- candidate `[qw qh] = [quantize-region-size (pw / d), quantize-region-size
  (ph / d)]` — the existing quantize (256-multiple, 4096-clamp,
  `compositor_gpu.cljs:493-498`), never a new one. Both axes take the SAME
  divisor (fork F-1ᴸ pins uniform).
- candidate bytes = `region-lease-bytes qw qh shadow?`
  (`compositor_gpu.cljs:523-529`, unchanged).
- admission test: `(reserved-bytes − self-credit) + candidate-bytes ≤
  budget-cap-bytes`, where **self-credit = the bytes of THIS region's own
  currently-held lease** (lawful because the lifecycle releases the old
  key before acquiring the new one inside the same pass — the old
  generation's budget bytes free first; §4.2's frozen order is the
  guarantor). Never credit another region's lease (no preemption, R2ᴸ).
- first `d` that admits → granted key `[region-id qw qh]`, done.

No `d` admits → **the existing refusal road, unchanged** (the floor rung).
The ladder is a FROZEN set — `{1 2 4 8}` — with the floor at one quantum
per axis; no divisor below 8, no dynamic extension, no sub-quantum lease
(L5 kills a runaway ladder). At a 4K canvas the rungs price ~446MB · ~117MB
· ~30MB · ~9MB (+16MB shadow) — real dynamic range under the shared pool.

Two regions under pressure admit **sequentially in the regions vector's
existing iteration order** (arrangement order — deterministic), each
against the pool's then-current snapshot with self-credit only. This is
deliberately conservative: when two regions cross in one frame, the second
may wear one rung deeper for one frame than a global optimum would give;
the stateless re-grant (§6d) corrects it the next frame. Determinism over
optimality; no cross-region solver (R2ᴸ).

`region_rungs.cljc` is pure `.cljc` (the `frame_delta.cljc` /
`frame_inputs.cljc` family precedent): ladder walk, admission math, and
the wear-receipt VALUE mint are all JVM-testable with no GPU.

### 6b. Custody — where the grant runs

`active-region-leases!` (`compositor_gpu.cljs:1210-1246`) currently maps
regions → desired keys, releases held leases not in the desired set, then
acquires. The correction inserts ONE step: **desired rows → granted keys
via `region_rungs/grant`, and the release/acquire loop runs on GRANTED
keys.** This placement is forced, not chosen: if the loop kept operating
on desired keys, a stably-worn region's held granted lease would sit
outside the desired set and be released/re-acquired every frame — the
gatherer's release-loop extraction (`:1218-1223`) is the receipt for why
the mapping must precede the loop.

The `update?` predicate (`:1234-1246`) keys on `expected` — which becomes
the granted key; rung crossings therefore ride the existing five-way
predicate and the existing `:region-binding-updates` /
`:leases-acquired` / `:leases-retired` counters with zero predicate edits.

The lease VALUE gains `:rung-divisor` (and the desired key for the
receipt); wear state is compositor-side, on the lease, because the pool
owns budget truth and the draw site already reads the lease (`:refused?`
at `region3d_gpu.cljs:1427-1441`) — fork F-3ᴸ records the alternative.
The binding owner's rows (`region_bindings.cljs`) stay DESIRED-only:
desired is truth about want; granted is truth about the device budget;
the owner's `[semantic-generation, device-epoch]` discipline is untouched.

### 6c. The wear surface — worn visibly, cheaply

When `rung-divisor > 1`, the composite draw for that region additionally
renders a small corner WEAR GLYPH — a sibling of the refusal corner glyph
(`region3d_gpu.cljs:394-401`), visually DISTINCT from it (refusal's orange
corner means "nothing rendered"; wear means "rendered, coarser"). The
glyph reads ONLY the lease's `:rung-divisor` — binding-lane state at the
draw site, exactly the refusal precedent — never a semantic paint field
(L4 kills the wrong custody). Mechanism (uniform flag on the composite
pipeline vs a third pipeline variant) is implementer detail bounded by the
L1 golden. Styling is FELT-TUNABLE (the floor contract's §5.5 precedent):
Sid may restyle or drop the glyph at the felt pass in one line; the
RECEIPT (§6e) is the non-negotiable honesty floor, the glyph is its
visible face. The interior content itself needs no wear treatment — lower
texel density IS the honest appearance of a lower rung; nothing fakes
sharpness back (no sharpening, no upscale filter beyond the existing
linear sampler).

### 6d. Recovery — stateless, sharp-first, never worse

There is no worn FLAG to clear and no recovery machinery: `grant` runs
per invocation from the current pool snapshot and always tries `d = 1`
first (the blur road's stateless-recovery shape, `compositor_gpu.cljs`
:807-851 precedent). Pressure relents → the next `active-region-leases!`
grants a sharper key → that is an ordinary lease crossing: old worn lease
released, new acquired, required roles re-encoded the SAME frame (§6f).
Direct jumps (÷4 → ÷1) are lawful — grant picks the sharpest admissible
rung, not the adjacent one.

**The never-worse law:** while pressure holds and the desired size is
unchanged, repeated invocations re-derive the SAME granted key — zero
releases, zero acquisitions, zero refusals (admission math is
deterministic in its inputs; the held lease is self-credited, so its own
existence never flips its own verdict). A recovery ATTEMPT is just
arithmetic — it touches no lease unless a sharper rung actually admits.
There is no release-then-hope road; the held worn lease is never given up
for a candidate that might refuse (L3's pressure-held leg kills it).
Oscillation against the OPTIONAL lanes is structurally damped: held
leases are never preempted (M10), mandatory frame targets ride the
protected reserve, and blur yields without pushing back. No hysteresis
constant exists in this atom; if thrash is ever FELT, it is a felt-pass
tunable (R3ᴸ), not silent machinery.

### 6e. Honesty — receipts minted post-admission

- The wear receipt is minted by `grant` AFTER the admission walk, from
  what was actually granted — `{:region/id, :desired-key, :granted-key,
  :rung-divisor, :candidate-bytes, :reserved-bytes,
  :budget-cap-bytes}` — never from intent (the blur road's declared-vs-
  achieved gap is the corpse this law is built on; L1's forced-÷4 leg
  kills an intent-minted receipt).
- Ledger (`frame_inputs.cljc:205-226` map): two new counters —
  `:region-rungs-worn` (a granted `d > 1` this pass) and
  `:region-rung-recoveries` (a crossing to a strictly smaller `d`). They
  ride the existing `assoc-ledger!`/`increment-ledger!` road and the
  sticky `__softlandFrameLastBindingLedger` snapshot (`:242-253`) —
  drivable receipts, no new plumbing.
- `region-leases-receipt` (`compositor_gpu.cljs:691-698`) adds
  `:rung-divisor` per lease row. `:last-region-refusal` unchanged.
- NO new quantization door: the admission rung is not semantic input and
  never registers in `quantization-doors` (L4 asserts the registry is
  unchanged).

### 6f. What does NOT change — the negative space, stated positively

- **Desired-size math**: `region3d_gpu.cljs:1037-1050` byte-unchanged —
  camera aspect and picking stay on the unclamped pixel-size, exactly the
  in-source comment's law.
- **Encode mechanics**: the interior pass renders the whole granted
  attachment exactly as it renders the whole quantize-UP attachment today
  — no `setViewport`, no scissor, no shader edit for wear (the glyph rides
  the composite, not the interior). Screen-metric elements (7px markers,
  3px gizmo strokes — `region3d_gpu.cljs:241,:344,:808`) are NDC-fraction
  sized against the desired viewport, so they keep their on-screen size at
  any rung and simply blur with the density — verify-point §14.1, never
  assumed.
- **The stale-texture guard**: a lease-key change already forces
  re-encode before sampling (`region3d_gpu.cljs:1312-1353`; encode
  gate `:1375-1386`) — a rung crossing IS a lease-key change, so a worn
  lease is never composited holding another rung's pixels. "Current-frame"
  in this contract's title is this existing invariant doing its job, not
  new machinery.
- **Pick**: zero edits anywhere on the chain `scene_store.cljc:429-437 →
  scene_runtime.cljs:368-376 → region3d_runtime.cljs:211-239 →
  region3d_scene.cljc:560-586`.
- **Refusal road, lifecycle functions, reserve policy, epoch/slot
  machinery, semantic reducer, plan fragments, effect view, twin, scene
  tape**: untouched (§4, §10).

---

## 7. Entry points — exact

**New namespace (own namespace, pure):**
- `src/app/client/substrate/region_rungs.cljc` — the ladder walk,
  admission math, wear-receipt mint (§6a/§6e). Sits with its family
  (`frame_delta.cljc`, `frame_inputs.cljc`). Depends on nothing
  GPU-side; takes `quantize` and `lease-bytes` as values/args so the JVM
  tier tests the real arithmetic (chain-of-custody: the test consumes the
  same fns the compositor passes in).

**Thin hooks in existing files:**
- `compositor_gpu.cljs` — `active-region-leases!` (`:1210-1246`): the
  desired→granted mapping ahead of the release loop; loop operates on
  granted keys; lease value carries `:rung-divisor`; receipt + counters
  per §6e. `region-leases-receipt` (`:691-698`) adds the divisor field.
  `acquire-region-lease!`, `release-region-lease!`,
  `ensure-target-capacity!`, `acquire-region-target!`: ZERO edits.
- `region3d_gpu.cljs` — the draw-time branch (`:1427-1441`) gains the
  worn case (composite + wear glyph) beside the existing refused case;
  glyph shader/uniform beside `refusal-fragment-shader` (`:394-401`).
  Desired-size math (`:1037-1050`): zero edits.
- `frame_inputs.cljc` — ledger map (`:205-226`) gains the two §6e
  counters. `quantization-doors`: zero edits (L4 asserts). The observed
  dead re-declaration of `region-lease-quant`/`region-lease-max` at
  `frame_inputs.cljc:9-10` (no readers — gatherer, verified) is NOTED and
  left alone; one NOW line if it confuses the implementer.
- `region_bindings.cljs` — zero edits expected (rows stay desired-only,
  §6b); F-3ᴸ is the door if the draw site turns out to need owner custody.

**Test homes (lane named per the collision finding):**
- `test/app/client/substrate/region_rungs_test.clj` — NEW, pure JVM: the
  ladder walk, self-credit, sequential admission, floor termination,
  post-admission receipt (L2's math legs, L5's ladder-freeze leg).
- `test/app/client/substrate/frame_view_region_binding_test.clj` — L4
  rides the existing frame-view JVM harness shape.
- `verifier.cljs` Region3D floor lane (`region3d-s5-lifecycle!` and
  its harness, `:4850+`, forced-budget pattern `:4922-4939`) +
  `test/render_engine/run_verifier.mjs` gates (`:3706-3757`) — L1, L3,
  L5 driven legs and the new golden; the existing floor S1–S5 fields
  stay green untouched (their compositors never force budget pressure,
  so they never see a worn rung).

---

## 8. The five decisive scenarios (frozen as tripwires at close; wrong-builds named)

**L1 · degrade instead of refuse at the wall** *(floor lane, driven)*.
A forced-budget compositor (the existing `:budget-cap-bytes` constructor
option, `verifier.cljs:4922` pattern) sized so the desired lease refuses
but ÷2 admits; a live region with mesh content. Assert: composite carries
CURRENT-frame interior content (scene mutated after pressure applied;
sampled interior pixels match the new content class, never the
checkerboard) · wear glyph present at the corner · wear receipt shows
desired-key ≠ granted-key, `:rung-divisor 2`, and pool fields with
`reserved-bytes ≤ budget-cap-bytes` · ledger: `:region-rungs-worn 1`,
`:leases-acquired 1`, refusal receipts ABSENT, `:last-region-refusal`
nil for this region · the reserve leg: a released
`frame/group-output/*` target's bytes are untouched by the grant
(the `region-reserve-preserved?` shape, re-asserted under a worn grant) ·
second leg, budget sized so ÷2 refuses and ÷4 admits: receipt says
`:rung-divisor 4` — the post-admission mint law. *Wrong-builds named:*
compositing the prior sharp resolve scaled down/up (stale) — the
content-after-mutation assertion + same-frame `:region-encoded` receipt
kill it; a receipt minted from the first-attempted divisor (the blur
flaw) — the ÷4 leg kills it; granting ÷1 by eating the reserve — the
reserve leg kills it.

**L2 · exact picking at the worn rung** *(pure JVM + one driven leg)*.
Pure: with desired `[dw dh]` and granted = desired/4, construct the pick
camera per the road (`region-size × zoom·dpr`) and assert
`ray-from-region-point` + BVH resolve to the IDENTICAL object-id and t
(declared tolerance) as the sharp-rung construction — the granted
dimensions appear NOWHERE in the inputs (structural assertion: the pure
test never even passes them in, proving the chain doesn't need them).
Driven: at a worn rung, a click over a mesh resolves that mesh; a click
inside region bounds missing all meshes returns `:region-background`,
never the 2D beneath (the floor's container law, re-asserted worn).
*Wrong-builds named:* mapping region-local through texture space
(scaling by granted/desired) — passes at ÷1, off-center identity/t
mismatch kills it at ÷4; pick viewport read from the lease — same kill.
(The floor's CPU-vs-GPU pixel-parity oracle stays scoped to sharp rungs —
at reduced density its 2-byte bound is not a lawful assertion; wear
correctness is L1's golden + these identity assertions.)

**L3 · sharp-rung recovery + never-worse holding** *(floor lane,
driven)*. Start worn at ÷2 under forced budget. Leg A (hold): N repeated
draws with pressure held — granted key STABLE, `:leases-acquired 0` and
`:leases-retired 0` across the span, zero refusal receipts, composite
stays worn (never checkerboard, never black). Leg B (recover): raise the
cap (or release the competing consumer) → next draw crosses to ÷1 —
`:leases-retired 1` (old worn lease, old-dies-before-new),
`:leases-acquired 1`, `:region-rung-recoveries 1`, wear glyph GONE,
composite sharp, required roles re-encoded that frame. Leg C (partial):
raise the cap only enough for ÷2→ nothing happens if already ÷2 (grant
is sharpest-admissible, not restless). *Wrong-builds named:* sticky wear
(a worn flag someone must clear) — leg B kills it; release-then-fail
upgrade probing — leg A's zero-retires + zero-refusals kills it; a
recovery that skips re-encode and samples the retired rung's content —
leg B's same-frame encode receipt kills it.

**L4 · the worn crossing is still binding-only** *(frame-view JVM
lane)*. Drive a rung crossing (both directions) through the frame-view
harness at 200+ entries: `changed-families []` · `produced 0` ·
arrangement upserts/removes 0 · comparator calls 0 · effect containers
touched 0 · plan fragments compiled 0 · full validations 0 · binding
update/acquire/retire `1/1/1` · twin equal throughout · the region's
plan-fragment signature stays `[region-id shadow?]`
(`region_bindings.cljs:204-205` road) · `quantization-doors` and the
camera-door allowlist BYTE-UNCHANGED (no `:region-admission-rung` door
exists) · within-quantum zoom motion at a HELD worn rung acquires zero
leases. *Wrong-builds named:* the divisor entering a shape key, paint
field, or registered door — the door-registry byte-assertion + twin
divergence kill it; the wear glyph fed from a semantic field — the
`produced 0` counter + the §6c custody assertion kill it.

**L5 · the floor rung is the existing honest refusal** *(floor lane,
driven)*. Budget forced below the ÷8 candidate (the 5MB-compositor
pattern): the EXISTING refusal road fires — `:last-region-refusal`
receipt present, checkerboard + orange corner composite BYTE-IDENTICAL
to the existing refusal golden, encode skipped
(`region3d_gpu.cljs:1375-1386` shape), mandatory frame targets + reserve
intact, and the pure tier proves the ladder TERMINATES: exactly four
candidates evaluated, divisor set frozen `{1 2 4 8}`, floor one quantum
per axis, then refusal — no fifth rung, no 0×0 lease, no loop.
*Wrong-builds named:* extending the ladder below the floor (a 128px or
0×0 lease) — the frozen-set assertion kills it; replacing the refusal
fill with black/empty at the floor — the byte-identical refusal golden
kills it; a silent skip (no receipt, no fill) — both kill it.

Counters ride the existing `frame_inputs` ledger; the floor legs read the
sticky binding-ledger snapshot and `region-leases-receipt` — no new
receipt plumbing (chain-of-custody: every asserted value above has a
named producer in §6e and a named reader in the test home in §7).

---

## 9. Representative goldens (reuse before minting)

1. **ONE new golden** — `gpu-region3d-floor-worn.png`: a worn-rung (÷2)
   composite with mesh content + wear glyph, byte-determinism-checked
   like the existing six floor cases, added to the manifest.
2. The existing refusal-fill golden is L5's assertion, byte-identical.
3. The six floor goldens, the 44-image bank, the region/seam goldens
   (47 + 3), and the MSDF counterexample (RED) stay byte-identical —
   none of their harnesses force budget pressure, so none ever sees a
   worn rung (verified against the harness setup, not assumed).

---

## 10. MUST-NOTs (real only)

1. NEVER read/require `src/app/server/env.clj`.
2. **The four numeric governors are byte-frozen**: `default-pool-budget-
   bytes` (512MB, `compositor_gpu.cljs:25`) · `region-lease-quant 256` /
   `region-lease-max 4096` (`:17-18`) · the canvas-backing clamp
   (`renderer.cljs:3787-3798`) · the 56 bytes/px lease pricing
   (`region-lease-bytes`, `:523-529`). Also `region-encode-step 1.12`.
   The gatherer found no single symbol named "the cap" — this list IS
   the cap. Tests force pressure ONLY via the `:budget-cap-bytes`
   constructor option.
3. The lease lifecycle functions and their order/refusal/retirement/
   reserve semantics are behavior-frozen (§4.2's list); the rung selector
   chooses WHICH key to request, ahead of the loop, and touches nothing
   else.
4. No preemption: another region's held lease is never released,
   credited, or rebalanced to admit this one.
5. The pick chain (§6f's four files/anchors): zero edits.
6. `scene_tape.cljc`: zero edits.
7. No execution clock (frame-idx, wall time) in grant, receipt, or glyph
   — the pool snapshot is state, not time.
8. No semantic/plan identity change: no new quantization door, no
   shape-key or paint field, no plan-signature input; the divisor lives
   on the lease and in receipts only.
9. Speed/fit never by omission: a worn lease is always re-encoded before
   sampling (the §6f guard); no build may green L1 by compositing stale
   or foreign-rung pixels, and refusal is never replaced by silence.
10. Wear receipts state achieved values post-admission, never intent.
11. Existing golden banks byte-identical (§9); the one new golden is the
    only image-lane change.
12. The instrumentation/ledger stays until Sid closes this work.

---

## 11. Refusals (named, one line, routed — think wide, build narrow)

- **R1ᴸ — solidity/provenance wear channels** (the full ladder-wear
  vocabulary: measured/derived/reconstructed/generated) → LATER by lived
  want (decisions.md ladder law: "machinery arrives by lived want"); the
  corner glyph + receipt are this atom's whole wear surface.
- **R2ᴸ — cross-region arbitration/preemption/rebalancing** → LATER at a
  real multi-region pressure want; sequential-conservative admission
  stands.
- **R3ᴸ — hysteresis/thrash damping** → felt pass IF felt; no clock
  inputs exist to build it with anyway (MUST-NOT 7).
- **R4ᴸ — progressive refinement** (grant low now, refine async) → the
  R4′ amortized-encode package; its budget-law fork is already recorded
  there (frame-view F-e).
- **R5ᴸ — per-axis/anisotropic divisors** → refused; uniform divisor,
  frozen set.
- **R6ᴸ — encode-ladder constant tuning** → the felt pass (frame-view
  R6′, unchanged).
- **R7ᴸ — the blur road's declared-vs-achieved receipt gap** (the
  precedent's own honesty flaw, found by this cut's gatherer) → board
  debt, its own line; this atom repairs the LAW for its own receipts and
  does not touch the blur road.
- **R8ᴸ — out-of-loop higher rungs** (streamed frames, remote render) →
  the SPACE lane (board); this atom is the bottom rung only.

## 12. Named non-solutions — refused roads

- **Cap bump / pool split** — moves the wall, corrects nothing, breaks
  the shared-pool arbitration the reserve repair just settled.
- **Scaling up the last sharp texture under pressure** — a stale frame
  wearing a sharp frame's clothes; the exact "silent/stale fallback" the
  commissioning line forbids (L1 kill).
- **Debounce/gesture-end lease acquisition** — alters interaction timing
  to hide the wall (the frame-view contract's same refusal, inherited).
- **Preempting held leases or the reserve** — L1's reserve leg + M10.
- **A worn flag with recovery machinery** — stateless re-grant is the
  precedent-proven shape; a flag someone must clear is the sticky-wear
  wrong-build (L3 kill).
- **Conflating the admission rung with the encode rung** — the encode
  rung is a camera door feeding aspect/lens; wiring admission into it
  would move semantic state on budget pressure (L4 kill; §2 exists
  because of this trap).

## 13. Forks seen and written (defaults chosen, per the law)

- **F-1ᴸ ladder shape:** divisors `{1 2 4 8}` on desired pixel-size,
  re-quantized (CHOSEN — bounded walk, real byte spacing) vs stepping
  256-quanta downward (rejected: ~15 steps at 4K, near-flat byte deltas)
  vs a geometric 1.12-family ladder (rejected: churn + name collision
  with the camera door).
- **F-2ᴸ wear surface:** corner wear glyph, felt-tunable styling
  (CHOSEN) vs receipt-only (one-line fallback if Sid drops the glyph at
  the felt pass; the receipt never falls back).
- **F-3ᴸ wear custody:** divisor on the lease value, compositor-side
  (CHOSEN — the draw site already reads the lease) vs on the binding
  owner's rows (legal if the implementer finds the draw site needs owner
  custody; note in NOW).
- **F-dᴸ — the one live fork door** (raise only if reached; ONE
  question, never a stop): a discovered consumer that sizes
  placements/text/aspect off the LEASE dimensions rather than the
  desired pixel-size — the gatherer could not fully rule out
  `region3d_placement_gpu.cljs` — or a real geometry distortion at a
  worn rung that breaks §3's neutrality inference.

## 14. Implementation order — one atom, straight through (internal passes, never gates)

1. **Pure ladder:** `region_rungs.cljc` + `region_rungs_test.clj` — the
   walk, self-credit, sequential admission, floor, post-admission
   receipt. VERIFY-POINT first: drive the real fullscreen path once
   unmodified and confirm §3's composite-neutrality inference (whole
   texture → rect) on the live road; if it fails, that is F-dᴸ, one
   question.
2. **Grant custody:** the `active-region-leases!` hook — mapping ahead
   of the loop, granted keys through release/acquire, lease value +
   receipts + counters.
3. **Wear surface:** the draw-branch worn case + glyph.
4. **Scenarios:** L4 in the frame-view JVM lane; L1/L3/L5 + the golden
   in the floor lane; L2 pure + driven leg.
5. **Falsification (the implementer's own pass):** drive the REAL
   fullscreen re-wear road — Sid's exact gesture, maximum workspace zoom
   on the desktop AMD box — and confirm: mesh/text/ink continuously
   visible, wear glyph at the extreme, no checkerboard until the true
   floor, receipts honest (counts before timings). Then the mobile
   route's pinch at its budget. A remaining checkerboard at
   NON-extreme zoom is a finding to record, never silently retuned
   (the ladder set is frozen; retuning is Sid's felt call).

## 15. Definition of done + the expected receipt

Done means ALL of: the fullscreen wall composites worn current-frame
content instead of refusing until the true floor · refusal intact at the
floor, byte-identical fill · pick exact at every rung · recovery to
sharp on the frame pressure relents, held rung stable while it doesn't ·
zero semantic/arrangement/effect/plan work on any rung crossing · all
four governors + lifecycle byte-frozen · receipts post-admission ·
L1–L5 + the new golden green, existing banks byte-identical · the felt
re-wear receipt: Sid's gesture reaches material, not checkerboard.

Expected worn-crossing receipt (desktop, 4K canvas — measure, never
invent):

```
change: region-rung | region-id: <one> | desired 3840×2176 → granted 1920×1088 (÷2)
changed-families [] · produced 0 · comparator-calls 0
effect-containers-touched 0 · plan-fragments-touched 0 · plan-full-validations 0
region-binding-updates 1 · leases-acquired 1 · leases-retired 1
region-rungs-worn 1 · region-rung-recoveries 0
reserved-bytes ≤ budget-cap-bytes · refusals 0
region-encoded <required roles, same frame> · oracle-divergences 0
```

## 16. Close mechanics + the implementer's opening prompt

At close: freeze L1–L5 as tripwires + the §9 golden (focused suite only;
foreign failures are board debt) · changed-file list diff-derived · one
NOW entry (≤15 lines, self-audit line included) in
`FRAME-VIEW-LOWER-RESOLUTION-NOW.md` · flip
`FRAME-VIEW-REGION-BINDING-NOW.md:22`'s acceptance line (§4.4) · board
line flip · acceptance is Sid's word alone.

---

**Implementer's opening prompt** (paste into a fresh session):

> **Preflight, before the first prompt:** set permission mode, MCP set,
> and remote-control NOW — never mid-session (prefix-rewrite law).
>
> Implement the FRAME-VIEW/LOWER-RESOLUTION correction atom — the
> Region3D admission rung — one pass, straight through, per the
> work-package law.
>
> **Boot (byte-sized; read PRIMARY, sections only — total ~45KB):**
> - `docs/render-engine/FRAME-VIEW-LOWER-RESOLUTION-CONTRACT.md` — whole
>   (~24KB). THE contract; §6 design, §7 entry points, §8 scenarios.
> - `docs/render-engine/FRAME-VIEW-REGION-BINDING-NOW.md` — whole
>   (4.2KB) — current runtime state.
> - `FRAME-VIEW-REGION-BINDING-CONTRACT.md` §5 (budget-wedge bullet) +
>   §6e (~8KB of 59KB) — the frozen lifecycle laws + binding owner.
> - `REGION3D-FLOOR-CONTRACT.md` §5.6 + §5.8 (~5KB of 52KB) — lease
>   lifecycle + the one pick road.
> - `docs/decisions.md` "The spatial model & the representation ladder"
>   (~lines 592-652, ~4KB) — the ladder direction this rung serves.
> Code by seam only (contract §7 lists every anchor); never whole-file.
>
> Build order = contract §14; §14.1's verify-point runs FIRST. Your own
> falsification pass is §14.5 — Sid's real fullscreen gesture is the
> felt receipt. Ambiguity → strongest default + a NOW note; the fork
> door is §13 F-dᴸ — ONE question, never a stop. Foreign test failures
> are board debt. Close per contract §16. Acceptance is Sid's word.
