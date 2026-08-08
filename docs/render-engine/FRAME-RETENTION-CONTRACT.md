# FRAME-RETENTION — camera-only frame cost (contract)

Cutter: Fable 5 (claude-fable-5), high effort. Cut 2026-08-08. One pass, one
document (work-package law). Status: ROUND CLOSED — the one bounded
fresh-eyes falsification round ran 2026-08-08 (fresh headless Codex session
at Sid's word; 3 decision-changing + 1 precision findings, all repaired in
place: prepare-gate completeness law, retention lifecycle pins, the
two-ladder region encode split, extra-text-geos identity). Round count is
ABOVE the ≤1 expiry threshold — the round requirement stays for the next
cut. Sid-touch #1 spent; acceptance of the built atom is Sid's word, touch
#2. Ready for the implementer prompt at the tail.

Sid's framing: fix performance as a whole, not piecemeal. This contract is
not a patch list — it is the enforcement of standing law (W0-C §5.5–5.7 and
decisions.md "The render seam") at the one place the code never honored it:
the frame edge.

## 1. What this atom is

**THE INVARIANT: a frame where only the camera moved re-derives nothing
semantic.** Entry production, arrangement maintenance, effect-span
maintenance, frame-plan maintenance, and every subsystem prepare
(image/path/connector/chrome/region3d) run only when their declared semantic
inputs change. The camera lives in exactly two per-frame places: the camera
uniform write and the scissor projection of the retained arrangement. Steady
camera-only frame cost = uniforms + projection + encode.

"Camera" is pinned: the workspace `pan-x`/`pan-y`/`zoom`, plus their raw
derivatives (`pixel-size` before quantization). It is NOT: `dpr` and canvas
size (viewport truth — semantic, entering through declared ladders), and NOT
a region's own orbit view (session truth per REGION3D-FLOOR §5.4's
two-camera law — semantic). Raw camera values may reach semantic derivation
ONLY through named, versioned quantization doors (the path zoom regime; the
region lease ladder, quant 256 / max 4096); each door is enumerated in the
ledger (§5a). Continuous camera motion inside a quantum/regime is, by
construction, not a semantic change.

The mechanism is the constitution's fenced incremental view (decisions.md
"The render seam"): declared keyed inputs per family, change minted once at
the write site, the batch producer path kept alive as the flag-on oracle
that verifies every retention decision.

## 2. Receipts basis — banked, structure, hypothesis

Banked device receipts (2026-08-08, iPhone, real space + region3d demo,
n=235 entries, 197 slots, camera-only pan at 8–26fps; instrumentation
[RAF]/[DRAW]/fams in the working tree):
- `produce` bucket 35–58ms/frame; family produce inside it is only msdf
  5–9ms — the rest is `update-frame-arrangement` re-assoc'ing all entries
  through the allocating Contract-O comparator (~3–5k comparator calls per
  frame).
- `plan` 12–20ms/frame: `maintain-frame-plan` + effect spans run every
  frame.
- `region` 11–16ms/frame steady; occasional 407–443ms frames during
  gestures; `conn` 2–8ms; `encode` 7–10ms.
- Already banked in-tree, verified on device, KEPT (they compose with this
  atom): the slot-reconcile identity gate (26ms→0, `render.cljs:77-166`) and
  projection-to-vector (27ms→0, `renderer.cljs:3608-3618`).

Verified structure (read from source this cut, 2026-08-08; line anchors are
working-tree):
- The `[DRAW] region` bucket times `prepare-region3d-frame!`
  (`region3d_gpu.cljs:975-1095`), not `region3d-entries` (which only reads
  `@(:!prepared)` and mints one tape entry per region). Inside prepare, per
  region per frame with NO gate: full region canonicalization
  (`validate-region!` + `session-region-value`, :992-993) feeding an
  O(scene) `material-key` deep compare; `scene/camera-matrices` (:1017,
  incl. a 4×4 inverse); `scene/shadow-light-space` over every triangle
  (:1018 — camera-independent, yet per-frame); `draw-order` with two
  `pr-str` sorts (:1044) plus placement `sort-draws` outside its own
  `changed?` branch (`region3d_placement_gpu.cljs:461-464`); the pick-state
  rebuild (:1068-1072) and a 5-traversal receipt swap (:1073-1094). GPU
  writes and both render passes ARE correctly held on pure pan.
- The zoom road is a distinct mechanism: `view-key` (:1019-1021) carries
  UNQUANTIZED `pixel-size`, so every zoom frame flips `view-changed?` and
  forces a full interior re-encode; crossing a 256px lease quantum
  additionally destroys 4 textures and creates 3–4 (one 4×-MSAA rgba16 up
  to 4096²) — the boot-init allocation shape. Pure pan changes neither.
- `update-frame-arrangement` (`renderer.cljs:3343-3363`) runs all 10
  producers and re-assocs every entry every frame: ≈2·n·log n Contract-O
  comparator calls with zero change; `entry-key-compare`
  (`scene_tape.cljc:689-703`) allocates two wrapper maps + a closure per
  comparison, `compare-order` builds an eager 5-vector, and
  `compare-scalar` falls to `pr-str` for non-numbers.
- `maintain-effect-spans` rebuilds an O(n) `arrangement-token` every frame
  (`frame_effects.cljc:247-258`); `maintain-frame-plan` reuses structure by
  key (no camera term, `frame_graph.cljc:105-114`) but `bind-entry-ranges`
  + `validate-plan!` + a `clj->js` plan receipt still run per frame.
- `frame_effects` reads only `:entry/id` + `:order`/`:stack-path` from
  entries (grep-verified); `frame_graph`'s `arrangement-regions` reads
  `paint.lease-size` (quantized — a legal ladder value).
- Frame entries already carry `:family/id` (`renderer.cljs:2639-2651`).
- `pan-x`/`pan-y` are NOT in the `frame` map — they reach only
  `project-entry-scissors` as positional args; `:zoom`/`:dpr` are in
  `frame`.
- The budget-wedge lifecycle is in HEAD 43ae9d9 (stale leases die at frame
  start `compositor_gpu.cljs:1173-1181`; failed frames bump the reclaim
  epoch :1244-1250; lease clamp to attachment `region3d_gpu.cljs:1004-1015`;
  idempotent `destroy-target!` :300-329; after-submit identity guard
  :1154-1159). NOT working-tree — this atom composes with it, byte-untouched.

Hypotheses (magnitude, no verdicts — each carries its kill-probe): that
removing the ungated derivations returns pan to encode-bound (~7–10ms
class) — probe: the same [RAF]/[DRAW] receipt on the same device after S1;
that within-quantum zoom becomes cheap — probe: the S3 receipt. Per the
investigation fence, this contract promises COUNTS and identities
(deterministic), never milliseconds.

## 3. Scope rulings

1. **Per-family gating, not a whole-frame switch.** Each of the 10
   registered producers declares its inputs; a producer re-runs iff its
   declared inputs changed. The camera-only skip is the degenerate case
   (empty changed set). This also makes semantic frames proportional:
   typing re-produces the text family, not ten.
2. **Region3d: derivation gating + view-key quantization IN; re-render
   scheduling OUT.** The steady 11–16ms is this invariant exactly (the
   clean-region law of REGION3D-FLOOR §5.3 — "a clean region encodes
   nothing" — extended upward from encode to derivation: a clean region
   DERIVES nothing). The zoom road's fix (view-key rides the quantized
   lease-size, never raw pixel-size) shares the root cause — no key
   distinguished camera from semantic — and is one line of key
   composition, so it is in. The legitimate 407–443ms re-render at a
   quantum crossing is real work whose scheduling (async/amortized encode,
   gesture-end settle, W0-C §5.7 rule 4 interplay) is its own package —
   named refusal R1.
3. **Stable payload indirection** (Sid's constraint, given with the
   receipts): the per-frame GPU payload rebind becomes stable indirection —
   entries reference systems, not per-frame values — never silently
   skipped. This restores Contract-O §3.1's own shape (`:paint` is a
   registered batch REFERENCE) and makes retained-entry staleness
   unrepresentable rather than fenced.
4. **Effects + plan consume semantic entries, camera consumes nothing of
   theirs.** `maintain-effect-spans` and `maintain-frame-plan` move
   upstream of the scissor projection (they read no camera-projected field
   — verified for effects; the plan's `lease-size` read is a ladder value).
   The projected vector becomes encode-only data.

## 4. Laws — pointers, never restatements (each names the scenario that fails under its violation)

- **W0-C §5.5** "Compile when registry, enabled capabilities, formats, or
  viewport resource shape changes — not once per ordinary frame"; **§5.6**
  step 5 (invalidation from declared causes); **§5.7** rules 1–2 (coalesce;
  sleep — motion is never an always-on derivation loop). This atom makes
  those sentences code-real for produce/arrange/effects/plan. → S1, S3.
- **decisions.md "The render seam"** — recompute proportional to change;
  change minted once at the site that knows it; no execution clock as a
  derivation ancestor; the fenced incremental view (batch demoted to
  oracle, never deleted); the five declarations on every derivation that
  feeds frames. The ledger of §5a IS those five declarations for the frame
  edge. → S1, S2, S4.
- **SEAM-STEP1 §4b + T6 + T12** — the keying source for order re-derivation
  is the diff of produced `[entry-id order-token]` pairs, never `frame-idx`,
  never a revision stamp (PRESERVED — this atom only narrows WHEN the diff
  is computed and scopes it per family; the "producers still run per frame
  (bounded, ~dozens)" sizing premise is what n=235 broke, and it was
  sizing, not law). T6: the oracle is demoted, never weakened —
  `scene_tape.cljc` compile/validation byte-identical; the flag-on twin is
  STRENGTHENED to verify retention itself (§5h). T12: `frame-idx` legal at
  the sink and inside the oracle's own compile only. → S4.
- **W1 Contract-O §3.1–3.2** — the order token's fields, the two
  projections, batching only under forward-order equivalence. Ordering
  semantics are UNTOUCHED this atom: `scene_tape.cljc` is byte-untouched
  (MUST-NOT 1); entries carry the same tokens; the arrangement is the same
  sorted map, just not rewritten when nothing changed. → S4.
- **REGION3D-FLOOR §5.3** (clean-region law — extended to derivation by
  ruling 2), **§5.4** (two cameras, never conflated — the region's orbit
  view is semantic; the workspace camera is not), **§5.6** (lease key
  quantized 256/4096, versioned constants; M10 single owner; refusal
  receipts), **§5.7** (the region family's own render-seam five — its "a
  camera move dirties the region pass ONLY" is the region's SESSION VIEW
  camera, untouched here: an orbit move still dirties the pass with zero
  instance uploads; its oracle+fence, `maintained_view_test.clj`, stays
  lawful over the retained prepared value). This atom REFINES one §5.7
  input's grain: "viewport scale" enters region derivation at the §5.6
  ladder (quantized lease-size), never raw pixel-size — the same quantum
  the lease key already obeys. The view-key repair brings the encode gate
  under that ladder. → S1, S3.
- **LAYOUT-RETENTION §4 — the Contract-T accessor amendment** (staged to
  land as CONTRACT-T §7.5 at that atom's close; no §7.5 exists in
  CONTRACT-T yet) — any touch on text-side state rides the accessor API;
  no raw plane reads outside the accessor namespace. This atom's one
  text-adjacent surface is `session-layout-snapshot` flowing into
  `region3d_placement_gpu` (`renderer.cljs:3542`). → S2's text leg.
- **Golden law** (chrome contract's shared-file law lineage): existing
  goldens byte-unchanged; a golden that can only pass by re-blessing is a
  finding, never an edit. → S4.

## 5. The design

### 5a. The family input ledger

New namespace `app.client.substrate.frame-inputs`
(`src/app/client/substrate/frame_inputs.cljc`, loads pure). It owns, as
data:

1. **The declarations**: for each registered family, the exact set of input
   keys its producer may read. Joined to `frame-family-registry` at
   registration; a producer receives `(select-keys inputs declared)` — an
   undeclared FRAME-KEY read is unrepresentable (structural tier).
   System-INTERNAL state — the atoms hanging off a declared system,
   `@(:!prepared …)` and kin — CANNOT be fenced by `select-keys`; it is
   covered by the shape-rev law (§5f) plus the twin oracle (fenced tier,
   named honestly). The seeded self-test therefore seeds a LYING PREPARE —
   mutate an internal atom without bumping shape-rev — and asserts the
   flag-on twin throws.
2. **The quantization-door registry**: every legal camera→semantic door,
   named with its versioned constants — today: the path zoom regime; the
   region lease ALLOCATION ladder (quant 256 / max 4096); the region
   ENCODE ladder (geometric, default step 1.12× — §5g); the connector
   zoom door (§5f — struck from the registry if connector zoom proves
   payload-only). A camera-derived value not in this registry is illegal
   as a declared input (fence assert).
3. **`changed-families`**: `(prev-inputs, inputs) → #{family-id}` via the
   three-tier ladder proven by the slot-reconcile repair: `identical?`
   per key first, declared value-token comparison second, changed third.
   Comparison is against the inputs at THIS frame — never a cached or
   deferred snapshot (a semantic change arriving before the RAF lands in
   that same frame's produce).
4. **The ledger receipt**: a per-frame `__softlandFrameLedger` JS receipt —
   `{:changed-families #{…} :produced <n> :comparator-calls <n>
   :arrangement-identical? <bool> :effects-maintained? <bool>
   :plan-maintained? <bool> :region-prepared <n>
   :region-encoded/:region-held <n>}`. Counters are incremented INSIDE the
   real bodies (`produce-frame-entries`, `update-frame-arrangement`, the
   maintain calls, the region prepare) — never beside them (receipt-gaming:
   a counter beside the call proves the call site, not the work).
   `:comparator-calls` counts through a counting WRAPPER installed where
   the arrangement's sorted map is constructed (`renderer.cljs:3311`
   `sorted-map-by`) — `scene_tape.cljc` stays byte-untouched.

The declared-input table at cut (the implementer VERIFIES each row by
reading the producer's actual `frame` destructuring — the enumeration
method — and the twin oracle fences completeness; known members):

| family | producer | declared inputs (classes + known members) |
|---|---|---|
| `:render.family/rect` | `rect-entries` | store-frame (`:rects :ordered-vis :ops-count-by-vi :order-by-vi`), editor-pool shape-rev + `editor-rect-count`, sidebar-pool shape-rev, cmd-rect shape-rev + `cmd-panel-visible`, settings-rect shape-rev + `settings-visible`, `agent-visible`, chrome-text shape-rev (row verified against the destructuring at `renderer.cljs:2893-2896`) |
| `:render.family/shadow` | `shadow-entries` | editor-shadow-pool shape-rev + `editor-shadow-count`, sidebar-shadow-pool shape-rev, store-frame `:shadows` |
| `:render.family/msdf`, `:render.family/slug` | `text-entries-for-family` ×2 | text-sys shape-rev, `extra-text-geos` identity (made REAL by the §6 render.cljs change — upstream currently mints a fresh vector every RAF, which would mark both text families changed every frame), chrome-text-sys shape-rev + `chrome-base-line-count`, `settings-line-count`, `settings-visible`, `diagnostics-visible`, `diagnostics-line-index`, `cmd-panel-visible`, font/atlas rev, text precision regime IF one exists (verify; if none, none) (row verified against `renderer.cljs:2944-2948`) |
| `:render.family/clip` | `clip-entries` | `dirty-rect`, `clear-quad`, `partial?` |
| `:render.family/image` | `image-entries` | image-system rev (today's `:!last-images` site), store-frame image keys |
| path | `path-gpu/path-entries` | path-system rev (paths + zoom REGIME — the existing door) |
| connector | `connector-gpu/connector-entries` | connector-system rev (today's `:!last-mesh-set-key` site) |
| chrome | `chrome-gpu/chrome-entries` | chrome-system shape-rev (`:!last-chromes` site), diagnostics/agent visibility flags — `pulse-alpha` is NOT here: it is payload by the clock law (§5f) |
| region-3d | `region3d-gpu/region3d-entries` | per-region prepared-rev (§5g) |

`:frame-idx`, `pan-x/pan-y`, raw `:zoom`, unquantized `pixel-size` are not
declarable — the ledger rejects them (fence assert + seeded self-test).

### 5b. The three-stage frame

`draw-frame!` becomes three explicitly-staged blocks:

- **SEM** (semantic derivation): assemble the input map (cheap — stable
  references + scalars), `changed-families`, and if non-empty: produce
  exactly the changed families, family-scoped arrangement maintenance
  (§5c), then effects/plan maintenance gated on their own inputs (§5e).
  Empty set → the arrangement, effect-state, and plan-state atoms are not
  even swapped — identity holds by construction.
- **CAM** (camera projection): every encoded frame — camera uniform write
  (`update-camera`) + `project-entry-scissors` over the retained
  arrangement into the encode vector. Never gated, never skipped.
- **ENC** (encode): unchanged; consumes the plan + the projected vector.
  The scheduler's existing whole-frame early-out (`:encode?` false) stays
  above all three.

### 5c. Family-scoped arrangement maintenance

`update-frame-arrangement` takes the produced entries PLUS the set of
produced families. The removal diff — produced `[entry-id order-token]`
pairs against current keys (SEAM-STEP1's keying law, preserved) — is
computed against ONLY the produced families' current entries (entries carry
`:family/id`; the arrangement keeps a family→keys index, or filters —
implementer's choice, same semantics). Unchanged families' entries are
never visited. Camera-only frame: zero producer calls, zero comparator
calls, the same sorted-map object.

### 5d. Stable payload indirection

`:paint` becomes a stable reference bundle: the system object (stable
identity), statically-stable fields, and per-draw slice fields that are
genuinely semantic (buffer layout offsets/counts derived from store-frame).
Any field a system prepare can change (e.g. `:num-instances` after a
re-pack) is resolved THROUGH the reference at encode time
(`execute-gpu-batch!`/`execute-image-batch!`). The law: **an entry never
embeds a value that a prepare can change without that entry's family
re-producing** — with encode-time resolution, violating it is
unrepresentable. Exact encoding is implementer freedom under that law.
"Never silently skipped" is pinned: the repair is indirection, not
omission — a build that simply stops rebinding and hopes is the S5 kill.
A THIRD executor exists: `execute-verifier-entry!`
(`verifier.cljs:2326-2333`) destructures raw `:paint` fields, and the
verifier harness drives the batch/maintained pair itself
(`verifier.cljs:1098-1100`). Both re-plumb to the same encode-time
resolution and the new arities (§6). The gpu goldens are PIXELS (64 PNGs
under `test/app/fixtures/render_engine/gpu-goldens/`) — byte-identical
stays the bar, now through the re-plumbed executor.

### 5e. Effects + plan, gated and re-plumbed

`maintain-effect-spans` and `maintain-frame-plan` are called only when
`[arrangement-identity, container-registry, viewport(w/h/format),
capabilities]` changed (identity ladder), and are fed SEMANTIC entries
(pre-projection). Their internals stay untouched — the existing
structure-key reuse becomes the second gate behind a door that mostly never
opens. Pass ranges bound over semantic entries are valid over the projected
vector because projection is 1:1 and order-preserving (twin mode asserts
length equality). The `clj->js` plan receipt publishes only when plan-state
identity changed.

### 5f. Prepares gate, then mint a SHAPE revision

Every subsystem prepare obeys the invariant itself: an identity/value
early-out over its declared semantic inputs BEFORE any derivation. Image
(`renderer.cljs:2750-2757`) and path (`path_gpu.cljs:271-278`) already
have this shape; region3d gains it in §5g; connector and chrome gain it
here. Connector today has none — `derive-route-set` runs every frame on
raw `zoom` (`connector_gpu.cljs:232-252`; the banked `conn 2–8ms`), so
the connector prepare gets an early-out over its COMPLETE input set —
`[connector-ops, targets-by-address, effective-transforms, font-assets
(provider identity), content-text-system, region-anchor-resolver,
region-doors]` (the full signature at `connector_gpu.cljs:229-252`;
`connector_route.cljc:571-592` already diffs region-doors and provider-id
internally — the early-out must not starve those diffs) — plus the
REGISTERED connector zoom door (same regime pattern as path). If the
implementer finds connector zoom feeds only screen-constant label/arrow
scale, that is payload — route it through §5d indirection, strike the
door from the registry, note it in NOW.

**The prepare-gate completeness law** (round finding, 2026-08-08): a
prepare's early-out key is derived from its COMPLETE argument list plus
every value its internal derivation diffs — enumerated from the
signature, never curated. The twin CANNOT catch prepare-level
under-declaration — both its sides read the same prepared atoms — so the
fence for this class is the scenario kills (S2's region-door and
font-arrival legs), which are mandatory, not illustrative.

At its existing change-decision point each prepare mints a monotonic
**`shape-rev`** on its system — bumped IFF re-producing the family now
would yield different entry VALUES (post-indirection: entry set, order
tokens, semantic slice fields). Every other prepare effect is **payload**:
visible only through §5d encode-time resolution, never a rev bump, never a
produce. The prepare knows which kind it made; a prepare that changes
entry-visible values without bumping shape-rev is the lying-prepare class
the twin throws on (S4/S5). CLOCK-DRIVEN values — the selection
pulse-alpha (`frame_scheduler.cljc:179-187` → `chrome_gpu.cljs:226`) —
are payload BY LAW: they reach the GPU only as frame-edge uniform writes
under their `:clock` cause (T12: the sink; W0-C §5.7 rule 3), never as a
ledger input, never a shape-rev bump. A rev is never `frame-idx`, never
wall time — it increments only on actual change (the constitution: change
minted once, at the write site).

**Lifecycle pins** (round finding, 2026-08-08 — the retained state is
process-wide `defonce` at `renderer.cljs:3310` while systems are
device-keyed WeakMaps): a system-carried ledger input compares as the
PAIR `[system-object-identity, shape-rev]` — a bare numeric rev is
illegal (a new device's system at rev N would compare equal to the old
device's N). The prev-inputs snapshot carries the DEVICE identity; a
device change ⇒ every family changed, the retained
arrangement/effect/plan state rebuilt from scratch (retained entries
reference old-device systems — resolving through one is the kill), and
prev-inputs reseeded. First frame: nil prev-inputs ⇒ every family
changed (boot behavior unchanged by construction).

### 5g. Region3d: derive only on change; camera enters by ladder only

Inside `prepare-region3d-frame!`, per region, a gate BEFORE any derivation:
the identity ladder over `[region-op identity, session values
(view/selection/display-mode/gizmo/transforms), quantized lease-size, dpr,
placements input identity, font/atlas rev]`. Unchanged → the prepared value
is retained untouched: no canonicalization, no `material-key` build, no
camera-matrices, no shadow-light-space, no draw-order/sort-draws, no
pick-state rebuild, no receipt swap. Specifically:

- `shadow-light-space` moves under the material gate (it is a function of
  scene only — verified).
- `view-key` drops raw `pixel-size` for a TWO-LADDER split (round
  finding, 2026-08-08: exact-pixel consumers EXIST inside the interior
  pass — point markers compute a 7px extent and gizmo strokes a 3px
  normal by dividing by the exact viewport in `camera_info`
  (`region3d_gpu.cljs:241`, :344, written at :808) — so a naive hold
  would drift them up to ~2× on small regions):
  - **Allocation ladder** (unchanged): the lease KEY keeps the §5.6
    256/4096 ladder — texture destroy/create only at quantum crossings.
  - **Encode ladder** (new door, registered): the view-key's pixel-size
    term is quantized on a versioned GEOMETRIC ladder (default step
    1.12× — implementer may tune the constant; Sid judges it at the felt
    pass). Crossing an encode step re-ENCODES the interior into the SAME
    lease (no allocation); within a step the interior is held and
    screen-metric elements drift by at most the step ratio — transient
    and bounded, re-exactified at every re-encode. Gesture-end settle
    re-encode is R1's package.
  This still kills the every-zoom-frame re-encode (today's 400ms road)
  while keeping the invariant: camera enters region derivation only
  through the two versioned ladders. Aspect is zoom-invariant (both dims
  scale), so within-step view uniforms stay correct.
- Pick keeps EXACT values: the retained prepared `:camera` carries a
  `:viewport` from prepare time, and `ray-from-region-point` maps picks
  through it (`region3d_scene.cljc:578-583`; pick-state retains it at
  `region3d_gpu.cljs:1068-1072`; consumed via
  `region3d_runtime.cljs:209,369`) — so once view-key quantizes, a
  retained viewport can be up to one 256px quantum stale. The pick road
  therefore substitutes the CURRENT viewport/pixel-size at pick time;
  prepared state supplies scene/BVH only. S3(d) asserts it. (Same fork
  door if pick truly needs per-frame prepared camera state.)
- `region3d-entries` (:1097-1118) stops embedding `:zoom`/`:dpr` in
  `:paint` — they are DEAD fields (the composite encode at :1288-1289
  never reads them), so removal is behavior-neutral, MUST-NOT 3 becomes
  satisfiable, and retained-vs-batch twin equality holds within a
  quantum.
- `sort-draws` moves inside its `changed?` branch; `draw-order` under the
  region's own view/material gates (region orbit view = semantic).
- The prepared-rev (§5f) bumps iff any region's prepared value changed;
  `region3d-entries` is the region family's producer, gated on it.
- The lease lifecycle, refusal road, and every budget-wedge behavior are
  byte-untouched (MUST-NOT 6).

### 5h. The twin oracle verifies retention

`frame-tape-twin-check!` under its existing flag runs EVERY frame —
including (especially) frames where SEM was skipped: it batch-produces all
families unconditionally from the full current inputs (sharing entry
minting with the live path, sharing NONE of the gating) and asserts
entry-sequence equality against the retained arrangement, plus
projected-vector length equality. A stale retention — an under-declared
input, a lying prepare — throws here. The twin is compare-only: its batch
result never feeds the live path (a repairing oracle would hide the bug it
exists to catch). `scene_tape.cljc`'s compile/validation stays
byte-identical; `compile-frame-tape`'s edits are limited to the producers'
new input plumbing.

## 6. Entry points — exact

- **NEW** `src/app/client/substrate/frame_inputs.cljc` — declarations,
  quantization-door registry, `changed-families`, ledger receipt. Pure
  loads; no GPU, no DOM.
- `src/app/client/substrate/webgpu/renderer.cljs` (big file — thin hooks at
  existing seams only): `draw-frame!` staging (~:3578-3644) with the CAM
  stage's `update-camera` (:2574-2581, called :3505-3511);
  `produce-frame-entries` per-family invocation (:3336); family-scoped
  `update-frame-arrangement` (:3343-3363) + the `:comparator-calls`
  counting wrapper at the arrangement's `sorted-map-by` construction
  (:3311); `frame-entry`/`gpu-paint` reference shape (:2639-2660) +
  encode-time resolution in `execute-gpu-batch!`/`execute-image-batch!`;
  `frame-tape-twin-check!` (:3371-3391); plan-receipt publish gate
  (:3639-3644); image prepare gate + shape-rev mint (:2750-2757).
- `src/app/client/substrate/webgpu/region3d_gpu.cljs` —
  `prepare-region3d-frame!` per-region gate (:975-1095), shadow-light-space
  under the material gate (:1018), view-key recomposition (:1019-1021),
  draw-order gating (:1044), pick-state/receipt change-driven (:1068-1094),
  `region3d-entries` dead `:zoom`/`:dpr` paint fields dropped (:1097-1118).
- `src/app/client/workspace/region3d_runtime.cljs` (:209, :369) +
  `src/app/client/substrate/region3d_scene.cljc` (:578-583) — pick-time
  viewport freshness (§5g's pick road; exact current pixel-size
  substituted at pick, prepared state supplies scene/BVH only).
- `src/app/client/substrate/webgpu/verifier.cljs` — the third executor
  `execute-verifier-entry!` (:2326-2333) re-plumbs to encode-time
  resolution; the harness's batch/maintained drive (:1098-1100) takes the
  new arities. The 64 gpu-golden PNGs stay byte-identical through it.
- `src/app/client/substrate/webgpu/region3d_placement_gpu.cljs` —
  `sort-draws` inside `changed?` (:435-464).
- `src/app/client/substrate/webgpu/{path_gpu,connector_gpu,chrome_gpu}.cljs`
  — prepare gates (§5f) + shape-rev mints at the existing change-decision
  points (path already gated :271-278; connector gains the early-out +
  zoom door around `derive-route-set` :232-252; chrome at :226).
- `src/app/client/substrate/{frame_graph,frame_effects}.cljc` — internals
  UNTOUCHED; their renderer call sites gate + re-plumb to semantic entries.
- `src/app/client/workspace/runtime/render.cljs` — ONE structural change
  (round finding): the `:extra-text-geos` assembly (:832-834) becomes
  identity-stable when its sources are unchanged — the same three-tier
  ladder as the banked slot-reconcile beside it. Everything else
  untouched; its existing identity gates (`content-same?`,
  `chrome-same?`, geo identity) are the upstream signals the ledger
  consumes.
- `src/app/client/substrate/scene_tape.cljc` — **BYTE-UNTOUCHED.**
- `src/app/client/substrate/frame_scheduler.cljc` — untouched (it owns
  WHEN; the ledger owns WHAT).

## 7. The five decisive scenarios (frozen as tripwires at close)

**S1 — camera-only frames derive nothing.** Sustained pan and
within-quantum/within-regime zoom over a real space (n≥200 entries, ≥1
region3d): for every such frame the ledger shows `changed-families #{}`,
zero producer calls, zero comparator calls, `arrangement-identical? true`,
effects/plan not maintained, `region-prepared 0`, all region passes held —
AND encode ran with fresh camera uniforms (the screen actually pans).
One stretch runs WITH a live selection: the pulse rides its frame-edge
uniform under the `:clock` cause and `changed-families` stays `#{}`.
Anti-gaming: counters live inside the real bodies; the encode assertion
kills the frozen-screen build that passes by skipping everything.

**S2 — a semantic change lands in its own frame, during camera motion.**
While panning: type into a slot, change selection, move a connector — the
change's family (and only it) produces in that same frame. Two MANDATORY
prepare-level legs (the twin is blind to this class — §5f's completeness
law): orbit a region's session camera (region-doors change →
region-anchored connector routes re-derive same frame, region + connector
families produce); complete a font/atlas arrival (provider identity
change → connector labels and text re-derive). Twin flag on, equality
holds; text-side reads ride the layout accessors (MUST-NOT 7). Kills
under-declared inputs — producer AND prepare level — and
deferred/one-frame-late gating. Run WITH S1's counters live: the
untouched families stay at zero.

**S3 — the zoom roads are quantized, on two ladders.** (a) Zoom inside
one ENCODE step: no interior re-encode, no lease churn, no family
produce; screen-metric interior elements (markers/gizmo) drift at most
the step ratio — the scenario asserts the BOUND, not pixel-exactness.
(b) Cross an encode step: one interior re-encode into the SAME lease (no
allocation), elements re-exactified. (c) Cross a 256 lease quantum:
exactly one lease re-key (acquire new, release old after submit — the
wedge lifecycle observably intact) plus its re-encode, region family
re-produces, plan recompiles (`structure-input` sees the new
lease-size). (d) Cross the path zoom regime: path family alone
re-produces. (e) Pick during any within-step zoom stays exact: a region
pick resolves through the CURRENT viewport, never a quantized one (§5g's
pick road). Anti-gaming: both ladders land at their versioned constants
exactly — a build that "wins" (a) by never re-encoding (giant step)
fails (b)/(c)'s crossing assertions.

**S4 — the twin verifies retention across a mixed script, including the
lifecycle transitions.** The script BEGINS at first frame after boot (nil
prev-inputs → one full produce, then retention), runs flag-OFF through a
retention stretch, then flips the flag ON mid-script (off→on must verify
the arrangement retained while off), then: pan → type → within-step zoom
→ encode-step cross → lease-quantum cross → viewport resize → async
font/image arrival → a simulated device loss (new device ⇒ every family
re-produces; no retained entry resolves through an old-device system —
the §5f lifecycle pins). Batch equality every frame including skipped
ones; `scene_tape.cljc` byte-identical; all existing goldens
byte-unchanged. A golden that only passes re-blessed is a finding.
Anti-gaming: the twin is compare-only — a build that feeds the batch
result into the live path masks retention bugs and is wrong by definition.

**S5 — indirection, not stale rebind.** An edit re-packs a system
(instance count changes) while that family's entry set/order is unchanged
— a PAYLOAD change, so the prepare does NOT bump its shape-rev (§5f's
two-tier law): the family does not re-produce (counters), the retained
entries draw the NEW values through their references (screen shows the
edit), and twin-mode paint comparison shows no retained entry embedding a
prepare-mutable value. Kills both wrong builds: the silent-skip (stale
values drawn) and the produce-everything-on-any-prepare (counters — a
build whose shape-rev bumps on every payload change fails here).

Close of the atom: S1–S5 frozen as tripwires (unit level: ledger delta,
family-scoped maintenance, view-key composition, declaration fence; runtime
level: the twin-flag mixed script) + 2–3 representative goldens from the S4
script. Minutes, not matrices. Device ms receipts are felt-pass material
for Sid, never CI assertions.

## 8. MUST-NOTs (real only)

1. `scene_tape.cljc` byte-untouched; Contract-O ordering semantics and all
   existing goldens byte-unchanged.
2. No execution clock (`frame-idx`, wall time) as an input to any gate,
   rev, or declaration (T12 lineage; fence assert).
3. Raw camera (`pan-x/pan-y`, raw `zoom`, unquantized `pixel-size`) never
   enters a semantic key, rev, or producer input — only registered
   quantization doors.
4. Encode-side camera work is never gated: every encoded frame writes
   current uniforms and projects scissors from the retained arrangement.
5. The twin oracle path never gates, never repairs the live path; its
   equality check runs on skipped frames too.
6. The budget-wedge lifecycle (HEAD 43ae9d9 — stale-lease death at frame
   start, epoch bump on failure, attachment clamp, idempotent destroy,
   after-submit guard) byte-untouched.
7. Text-side state only through the layout accessor API (LAYOUT-RETENTION
   §4; staged as CONTRACT-T §7.5 at that atom's close).
8. The instrumentation ([RAF]/[DRAW]/fams + `__softlandFrameLedger`) stays
   until Sid closes this work; removal is one sweep at his word.

## 9. Refusals (named, one line, routed — think wide, build narrow)

- **R1 — lease re-render scheduling.** The legitimate 407–443ms interior
  re-render at a quantum crossing (async/amortized encode, gesture-end
  settle, scheduler rule-4 interplay) → its own package; this atom only
  makes those frames RARE (quantum-gated) instead of every-zoom-frame.
- **R2 — Contract-O comparator internals.** The `pr-str` leaf, eager
  5-vector, wrapper allocations → LATER; kill-probe pre-registered: the
  [DRAW] produce bucket on a TYPING receipt after this atom lands.
- **R3 — sub-family produce granularity** (per-slot/per-entry deltas) →
  LATER at the next receipt; family grain is this atom's floor.
- **R4 — partial redraw under camera motion** (dirty rects while panning;
  today camera motion = full clear, unchanged) → LATER.
- **R5 — effect-span/plan O(n)-per-semantic-frame** (`arrangement-token`,
  `bind-entry-ranges`) → LATER if a typing receipt summons it; they die on
  camera frames here.
- **R6 — cheaper semantic keys for region material** (the O(scene) compare
  when something DID change) → LATER; dies on camera frames here.
- **R7 — boot-path costs** (14.4s ground reconcile [GROUND], 3.4s initial
  shaping of 176 slots) → separate follow-ups, already recorded as board
  debt; not this atom.

## 10. Forks seen and written (defaults chosen, per the law)

- **F1 gate mechanism**: whole-frame cause-skip vs per-family declared
  inputs → per-family (proportionality on semantic frames too; whole-frame
  skip is the empty set).
- **F2 revision custody**: system-carried prepared-rev minted at write
  sites vs a central observer diffing values → system-carried (change
  minted once at the site that knows it).
- **F3 twin semantics**: compare-only, runs on skipped frames (chosen) vs
  only-on-produce-frames (rejected: the silent-stale class becomes
  invisible exactly when retention is wrong).
- **F4 region zoom road**: view-key on the quantized ladder (chosen — the
  floor contract's own §5.6 intent) vs time-throttled re-encode (rejected:
  an execution clock in derivation).
- **F5 region scope**: split as ruled in §3.2 (rejected alternative — all
  region perf as one separate package — because the steady cost IS this
  invariant and the shared root cause is repaired by this ledger).
- **The one live fork door** (raise only if reached): a genuine
  exact-pixel-size SEMANTIC dependency inside the region view uniform or
  prepared pick state (§5g verify points). One question, never a stop.

## 11. Close + the implementer's opening prompt

Atom close per the work-package law: S1–S5 as tripwires + 2–3 goldens,
focused suite only, diff-derived file list, one NOW entry
(`FRAME-RETENTION-NOW.md`, ≤15 lines) + board flip. Foreign test failures
are board debt. Acceptance = Sid's word.

---

**Implementer opening prompt** (paste into a fresh session after the
falsification round closes):

> PREFLIGHT before the first prompt: set permission mode / remote-control /
> MCP toggles NOW (a later flip rewrites the whole cached prefix).
>
> Implement the FRAME-RETENTION atom in /mnt/data/projects/Softland.
> Boot docs, byte-budgeted (~45KB — read these PRIMARY, nothing else up
> front):
> - docs/render-engine/FRAME-RETENTION-CONTRACT.md (~19KB) — the contract;
>   binding.
> - docs/render-engine/W0-C.md §5.5–5.7 only (lines 500–575, ~4KB).
> - docs/render-engine/SEAM-STEP1-CONTRACT.md lines 159–238 + 405–430
>   (~6KB) — the arrangement kit law, T6, T12.
> - docs/render-engine/W1/CONTRACT-O.md (4.2KB).
> - docs/render-engine/REGION3D-FLOOR-CONTRACT.md lines 385–515 (~9KB) —
>   §5.3 clean-region law, §5.4 two cameras, §5.6 lease law, §5.7 the
>   region derivation contract (its "camera move" = the region's own
>   session view camera).
> - docs/render-engine/LAYOUT-RETENTION-CONTRACT.md lines 176–208 (~2KB) —
>   the §7.5 accessor law.
> Code is read by seam, never whole (skeleton `grep -n "^(def"`, scoped
> windows); the contract's §6 entry points carry the line anchors.
> Working-tree note: renderer.cljs + runtime files carry banked verified
> repairs (slot-reconcile identity gate, projection-to-vector) and the
> [RAF]/[DRAW]/fams instrumentation — KEEP all of it; build on top.
> Build the whole atom straight through: new namespace first
> (frame_inputs.cljc), then the renderer staging, then region3d. Keep your
> own falsification pass; fix what surfaces in-session. Ambiguity → the
> strongest default + a note in FRAME-RETENTION-NOW.md. The one genuine
> fork (exact-pixel-size semantic dependency, contract §10) is ONE
> question, never a stop. Close per contract §11. Code commits land on
> docs/current-mental-model-local, code and docs never mixed in one
> commit.
