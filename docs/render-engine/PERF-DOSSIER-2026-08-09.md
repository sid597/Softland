# PERF DOSSIER — session of 2026-08-08→09

**What this is.** A handoff dossier, not a contract and not a verdict. It carries
(a) the hard receipts this session produced, (b) the reasoning that produced them
with its epistemic status marked, (c) what was checked and ruled out so nobody
redoes it, and (d) the branches that were never opened. Written by Fable 5
(`claude-fable-5`), effort high→max.

**Read this doc as a suspect.** Sid's instruction at the close: *"don't be overly
confident in your findings we need to first see if the direction is right
independently in new session but that does not mean re-searching what you already
checked."* Every claim below is tagged. Check the direction; don't repeat the
search.

## Tags used throughout

- **RECEIPT** — a measured value, quoted from a real run. Trust the number; the
  interpretation next to it may still be wrong.
- **STRUCTURE** — read from source this session, with file:line. Asserts that a
  path exists or a field is carried. Never asserts magnitude.
- **HYPOTHESIS** — an inference not yet closed by a receipt. Carries its
  kill-probe.
- **FALSIFIED** — something this session claimed and then disproved. Left in
  deliberately so the next session doesn't re-derive the dead branch.
- **UNEXAMINED** — named but never looked at.

## Session shape (relevant to how much to trust it)

Opened 2026-08-08 21:31 IST as a **summarization** task (what happened across
Claude + Codex that day). Sat idle overnight. Resumed 2026-08-09 09:29 IST and
became a **live performance investigation** when Sid pasted a device log. The
register was never renegotiated. That drift is itself a finding — see Area B.

---

# The six areas

Sid named three at the close. Three more were surfaced during the session and
belong on the same list.

| | Area | Status |
|---|---|---|
| **A** | Uncommitted tree spanning several atoms | Sid named |
| **B** | Atom B / Region3D felt pass — still owed a dedicated *product* session | Sid named |
| **C** | Performance findings, on the mobile↔PC and 2D↔3D↔3D-in-2D dimensions | Sid named |
| **D** | Boot / time-to-interactive | **added** |
| **E** | Retention correctness is unverified, and one closing receipt is false | **added** |
| **F** | Mobile as a testable surface (enabling constraint for C and E) | **added** |

Two threads that came up while summarizing 2026-08-08 are **already on the board
and this session added nothing to them** — listed here only so a new session
doesn't think they went missing: the **viewport-residency** rung (shape only
on-screen text; routed to `docs/next-prompt.md` on 2026-08-08) and **S4 RED**
(typed layout planes are not EDN-round-trippable; routed to the durable-custody
ruling in `cc36f2e`).

---

# Area A — the uncommitted tree

**RECEIPT** — as of 2026-08-09, `docs/current-mental-model-local`, 18 modified +
6 untracked, `1170 insertions(+), 405 deletions(-)`:

```
resources/public/index.html                        115 ++++
src/app/client/substrate/scene_tape.cljc             2 +-      ← see E1
src/app/client/substrate/webgpu/buffer_pool.cljs    23 +-
src/app/client/substrate/webgpu/chrome_gpu.cljs     22 +-
src/app/client/substrate/webgpu/connector_gpu.cljs  54 +-
src/app/client/substrate/webgpu/path_gpu.cljs       22 +-
src/app/client/substrate/webgpu/region3d_gpu.cljs  270 +++++---
src/.../region3d_placement_gpu.cljs                  6 +-
src/app/client/substrate/webgpu/renderer.cljs      733 +++++++++++-------
src/app/client/substrate/webgpu/verifier.cljs       68 +-
src/app/client/workspace/region3d_runtime.cljs      30 +-
src/app/client/workspace/runtime.cljs                4 +-
src/app/client/workspace/runtime/fonts.cljs         79 ++-
src/app/client/workspace/runtime/render.cljs       100 ++-
src/app/client/workspace/text_shaper.cljs           30 +-
test/app/client/substrate/image_citizenship_test.clj 7 +-
test/app/test_runner.clj                             1 +
test/render_engine/verify_scene_tape_fence.mjs       9 +-      ← see E1

?? .frame-retention-impl.log          (Codex run log — do not commit)
?? docs/.obsidian/                    (editor junk — do not commit)
?? docs/render-engine/FRAME-RETENTION-NOW.md
?? src/app/client/substrate/frame_inputs.cljc
?? src/app/client/workspace/runtime/touch.cljs
?? test/app/client/substrate/frame_inputs_test.clj
```

## Distinct bodies of work tangled in there

1. **FRAME-RETENTION atom** — Codex, 2026-08-08 16:48 onward, headless then
   attended. `frame_inputs.cljc` + gates through renderer / region3d / connector /
   path / chrome / image / pool / verifier, + `frame_inputs_test.clj` +
   `FRAME-RETENTION-NOW.md`.
2. **Two GPU regression repairs**, both shaken out on 2026-08-08 evening:
   - Claude 17:55 — chrome/path/connector executors read the base
     `bgra8unorm` pipeline off the source system, discarding `linearize-entry`'s
     `rgba16float` override → attachment mismatch → command buffer invalidated.
   - Codex 19:57–20:11 — legacy status-background rect exposes its vertex buffer
     as `:instance-buffer`; the new encode-time resolver only looked for
     `:buffer`, so `setVertexBuffer(0, …)` was skipped on `Draw(6,1,0,3)`.
     Fixed at `renderer.cljs:~2712` + regression tripwire at `verifier.cljs:2346`,
     invoked at `verifier.cljs:5593`.
3. **Mobile touch adapter** — Claude, 2026-08-08 ~11:00. `touch.cljs` (new) +
   the `?mdbg=1` overlay in `index.html` (the 115 added lines).
4. **This session's probe** — `render.cljs:897-903`, one `changed:` field on the
   `[DRAW]` line. **This is what cracked C1. Keep it until the perf work closes.**

## UNEXAMINED in Area A

- **No `git diff` was read for attribution.** Eight modified files are
  unattributed to any of the four bodies above: `text_shaper.cljs`,
  `fonts.cljs`, `runtime.cljs`, `region3d_runtime.cljs`, `test_runner.clj`,
  `image_citizenship_test.clj`, `verify_scene_tape_fence.mjs`, `scene_tape.cljc`.
  A new session should attribute each hunk before splitting commits.
- Whether these should land as one commit or several. FRAME-RETENTION has no
  acceptance (Area E), so committing it as "done" would be a false receipt.
  The touch adapter and the two GPU repairs are independently defensible.

---

# Area B — Atom B / Region3D felt pass

**Sid's framing, verbatim:** *"3d slowness and maybe the atom b felt is still not
a dedicated session because i felt performance and then we went into it but that
is not product that is eng."*

That is exactly what happened, and it happened twice — first on 2026-08-08 when
the mobile lag complaint turned into the FRAME-RETENTION contract, and again in
this session. **The felt pass keeps getting eaten by the investigation it
triggers.**

## Standing state

- Atom A landed `8b1ab98` (2026-08-07), **Sid-accepted** at `?region3d=1`.
- Atom B contract `40c766c`, built and banked in `43ae9d9`. That commit says in
  its own message: **"Sid demo acceptance PENDING."** Still pending.
- Codex session (2026-08-08 00:30→09:32) repaired interaction: whole panel drags
  by title bar / outer border (the 3D interior no longer doubles as drag
  surface), single-click → gizmo, W/E/R = move/rotate/scale on shapes *and*
  lights, orbit only on empty 3D background.
- **Codex named the next issue and nobody has tested it**, verbatim: *"If it
  becomes offset only after zooming, that is the next bounded fix: the panel
  drag/picking coordinate conversion—not another test campaign."*
- **Carried debt written into B's contract** (§ "Carried debt, binding on the
  close"): object manipulation in A's felt pass FELT SLOW — *observed /
  unmeasured / unattributed* — with an explicit ban on fixing it inside B,
  deferred to "the courtroom's one adapter-pinned frame trace (§9)."
- Obligation (6) of B's eight: **the package courtroom over Packages 2+3 rides
  B's close.** Not done.

## What this session may have changed for B

This session produced adapter-pinned frame traces on two phones and a desktop.
Whether that discharges §9's "one adapter-pinned frame trace" obligation for the
FELT-SLOW debt is **a judgment for the felt session, not for this dossier.** The
traces exist (Area C); whether they answer the question B asked is open.

## UNEXAMINED in Area B

- The zoom seam: does panel drag / object picking / gizmo dragging stay aligned
  at every workspace zoom level? Named 2026-08-08, never tested.
- The seven frozen obligations beyond THE SEAM DEMO.
- The courtroom itself.
- **On a phone, W/E/R and shift+drag are unreachable** — no modifier keys. The
  touch adapter (`touch.cljs`) re-dispatches touches as synthetic mouse/wheel at
  the same coordinates, so orbit and pinch-zoom work, but gizmo *mode switching*
  does not. If 3D manipulation is meant to be a mobile surface at all, that is an
  unaddressed design hole.

---

# Area C — performance

## C0. The invariant holds. This is the session's main positive result.

**RECEIPT — desktop**, same session, a frame with real changes vs a camera-only
pan frame:

| | changed frame | camera-only pan |
|---|---|---|
| `produce` | 8.3ms | **0.1ms** |
| `plan` | 9.4ms | **0.0ms** |
| `comparator-calls` | 1163 | **0** |
| `produced` | 56 | **0** |
| `changed-families` | 5 families | **`[]`** |
| `arrangement-identical?` | false | **true** |
| effects / plan maintained | true / true | **false / false** |
| TOTAL | 37.7ms | **5.1–5.7ms** |

**RECEIPT — iPhone, Safari 26.3 / iOS 18.7**, steady camera-only pan:
`produce 0–1ms · plan 0ms · region 0–1ms · n=0 · TOTAL 7–11ms`.

**RECEIPT — iPhone, Chrome CriOS 151 / iOS 26.3**, steady camera-only pan:
`changed: [] plan?false rgn-prep0 · TOTAL 9–15ms · encode 8–13ms`.

**Baseline it beat** (FRAME-RETENTION-CONTRACT §2, banked 2026-08-08, iPhone,
n=235 entries / 197 slots): `produce 35–58ms · plan 12–20ms · region 11–16ms ·
conn 2–8ms · encode 7–10ms`, camera-only pan at **8–26fps**.

**RECEIPT — observed pan rate now**: `[PACE] fps~45–60, raf p50 17.0ms`; idle
`fps~60 p50 17.0 p95 17.0`.

A camera-only frame re-derives nothing, on both platforms. That is the contract's
headline claim and it is met for pan. **This does not constitute acceptance** —
see Area E.

## C1. Region-3d shape mint → full plan rebuild (and apparently a full arrangement rebuild)

**This is the dominant remaining cost and it is one mechanism.**

**RECEIPT — iPhone Chrome, dozens of instances:**
```
plan: 27–36 (spikes 55–62) | fams: n=1 | changed: ["region-3d"] plan?true rgn-prep1
TOTAL: 41–52ms (spikes 76–80)
[PACE] fps~12–20, raf p50 56–107ms, during wheel 14–19/s
```
alternating with
```
plan: 0 | fams: n=0 | changed: [] plan?false rgn-prep0
TOTAL: 9–15ms
```

**RECEIPT — desktop, same chain, confirmed by Sid:**
```
{"arrangement-identical?":false,"region-held":0,"changed-families":["region-3d"],
 "effects-maintained?":true,"comparator-calls":1974,"region-encoded":2,
 "region-prepared":1,"produced":1,"plan-maintained?":true}
```

### The chain — STRUCTURE, read this session

1. zoom changes `lease-size` = `quantize-region-size(w × zoom × dpr)`, quantum
   256 — `frame_inputs.cljc:9` (`region-lease-quant`), computed at
   `region3d_gpu.cljs:1024-1027`.
2. `region-entry-shape-key` **includes `(:lease-size row)`** —
   `region3d_gpu.cljs:985-991`.
3. `entry-shape-changed?` → `bump-shape-rev!` — `region3d_gpu.cljs:1151-1153`.
4. shape-rev rides `region3d-system-token`, a declared input of
   `:render.family/region-3d` — `frame_inputs.cljc:76-78`.
5. → family enters `changed-families` → producer runs → 1 entry.
6. → `update-frame-arrangement` runs — `renderer.cljs:3534`.
7. → `arrangement-identical?` false → `plan-inputs` differ → `maintain-plan?`
   true — `renderer.cljs:3823-3830`.
8. → `maintain-frame-plan` + `maintain-effect-spans` rebuild —
   `renderer.cljs:3835-3858`.

**Steps 1–5 are LAWFUL** per FRAME-RETENTION-CONTRACT §5g ("lease-rung changes
mint shape"). The defect is steps 6–8 costing O(everything) for a 1-entry delta.

### C1a. The 1974 comparator calls — HYPOTHESIS, highest-value open question

- 1 remove + 1 insert on a 263-entry sorted map should cost **~16** comparisons.
  `ordered-insert` is a plain `assoc`, `ordered-remove` a plain `dissoc` —
  `scene_tape.cljc:708-719`. **STRUCTURE, checked.**
- `update-frame-arrangement` (`renderer.cljs:3534-3558`) **is genuinely
  incremental.** The rebuild is not there. **CHECKED AND RULED OUT.**
- 263 × log₂(263) ≈ **2115**, and the receipt reads **1974**. That is the
  arithmetic signature of *constructing* the sorted map from scratch, not of a
  1-entry patch.
- **Kill-probe:** instrument the `sorted-map-by` construction site
  (`renderer.cljs:~3311`, where the counting comparator wrapper is already
  installed) with a stack capture on any post-boot construction.
- **Corroborating oddity, unexplained:** this frame reads `region-held: 0` where
  every earlier sample read 1 or 2, and `region-encoded: 2`. A region lifecycle
  reset would plausibly rebuild retained state wholesale. **UNEXAMINED.**
- **Why it matters more than the milliseconds:** the contract's banked baseline
  was *"~3–5k comparator calls per frame"* — that is the cost FRAME-RETENTION
  exists to kill. Clean pan frames now read **0**. This frame reads **1974**,
  back inside the original problem's range. If the hypothesis holds, **the
  atom's headline win is partially undone on shape frames.**

### C1b. The two-ladder collapse at dpr 3 — arithmetic sound, conclusion FALSIFIED

- Rung spacing = 256 ÷ (region_width × dpr). Region is 720×480.
  - dpr 1 → **36%** zoom per allocation rung
  - dpr 2 → 18%
  - dpr 3 → **12%**
- The encode ladder is 1.12× ≈ **12% flat** — `frame_inputs.cljc:11,178-186`.
- So at dpr 1 there are three encode rungs per allocation rung (ladders properly
  separated); at dpr 3 they collapse into one. **The arithmetic is sound.**
- **FALSIFIED:** the conclusion drawn from it — *"therefore this is rare on
  desktop"* — did not survive. Sid reproduced `changed: ["region-3d"]` readily on
  PC. Either his desktop is dpr 2, or he crossed many rungs, or **something
  besides the lease ladder also mints that shape.** Unresolved.
- **A new session should treat every claim in this dossier that runs from a
  constant to an observed frequency as suspect.** That inference failed once.
- Possibly relevant and **UNEXAMINED**: `max-lease-size` clamps pixel-size
  (`region3d_gpu.cljs:1024-1027`). Once clamped, lease-size stops changing and
  the mints stop. That may be why some zoom stretches are clean.

## C2. The plan gate trips on something outside `changed-families`

**RECEIPT — desktop, sustained run after a pan settles:**
```
changed-families: ["clip"] · produced: 0 · comparator-calls: 0
arrangement-identical?: TRUE · plan-maintained?: TRUE · region-prepared: 1
plan: 8.3–10.0ms
```
**RECEIPT — iPhone:** `changed: [] plan?true rgn-prep0` with `plan: 10–12ms` —
the plan rebuilds with **no changed families at all**. Also
`changed: ["clip"] plan?true` → 10–12ms.

**STRUCTURE:** `plan-inputs` = `{:arrangement :container-registry :viewport
:capabilities}` — `renderer.cljs:3823-3826`. `input-value-same?` falls back to
full `=` — `frame_inputs.cljc:111-124` — so a value-equal freshly-built
`viewport` map does **not** trip it. Since `arrangement-identical?` was true,
**the trip came from `container-registry`, `viewport`, or `capabilities`.**

**HYPOTHESIS floated and weakened:** `container-registry` (regions are
containers; `region-prepared: 1` on those frames). Weakened because 3D-interaction
frames show `region-prepared: 1` with `plan-maintained?: false` — so region
prepare running does not by itself force the rebuild. **Unresolved.**

**Kill-probe:** log which member of `plan-inputs` compares unequal, one settled
stretch. ~5 lines of temporary instrumentation.

## C3. Encode is the new floor, and 240fps was never reachable

- **RECEIPT:** desktop clean pan, `encode 4.2–4.6ms` of a `5.1–5.7ms` frame.
  iPhone, `encode 5–13ms` of a `7–15ms` frame.
- 240fps = 4.16ms for the entire frame. Encode alone exceeds it.
- **iPhone displays run at 60Hz, or 120Hz with ProMotion. 240fps is not
  physically reachable there.** Real targets: 60 (16.6ms) / 120 (8.3ms). Pan is
  already inside the 120 budget.
- **RECEIPT:** `persistent-render-target?: false` and `dirty-rect: full` on
  nearly every mobile frame → the full 1179×2205 (2.6MP at dpr 3) is redrawn
  every frame. Desktop sometimes gets `partial`.
- **UNEXAMINED:** encode has never been profiled — only measured as a bucket.
  Draw-call count, batching, why the persistent render target is off, why the
  dirty rect is almost always full on mobile.

## C4. Connector re-produces every frame during 3D interaction

- **RECEIPT — desktop:** `changed-families: ["connector"]` / `["clip","connector"]`,
  `produced: 4`, `conn 2.6–3.9ms`, **`comparator-calls: 14`**, `region-encoded: 1`,
  `region-prepared: 1`, `plan-maintained?: false`. TOTAL 8.2–12.2ms.
- **RECEIPT — iPhone:** same families, `n=4`, `conn 4–7ms`, `plan 0–18ms`,
  TOTAL 14–31ms.
- **Probably correct.** The seam demo has a labeled connector anchored from the
  2D paragraph into a Region3D object; when the region camera moves the anchor
  moves and the route must re-derive.
- **Note the contrast that matters:** `comparator-calls: 14` here versus 1974 on
  region-shape frames, for 4 produced entries versus 1. **The delta path can be
  cheap.** That is evidence for C1a rather than against it.
- **UNEXAMINED:** whether it re-routes or merely re-labels; whether all four
  entries need to re-produce.

## C5. `clip` marks changed while producing zero entries

**RECEIPT:** `changed-families: ["clip"], produced: 0`. Clip's declared inputs are
`#{:partial? :clear-quad :dirty-rect}` — `frame_inputs.cljc:70`. `dirty-rect`
flips between `partial` and `full` exactly across that boundary. Either correct
(nothing to clip) or the inputs churn on a value that doesn't matter.
**UNEXAMINED.**

## C6. Selection re-produces everything

**RECEIPT — iPhone:** clicking a block → `produce: 39ms, n=262, changed:
["rect","region-3d","slug","chrome","msdf","connector","path","image","shadow"],
plan: 17ms, TOTAL 72ms`. A second instance: `produce 32ms, n=264, TOTAL 63ms`.
Arguably correct (selection changes chrome) but nine families and 262 entries for
one tap is a large hammer. **UNEXAMINED.**

## The two dimensions Sid asked for

### mobile ↔ PC

- **The mechanism is identical.** C1 and C2 both reproduce on both platforms.
- **Cost per occurrence is ~3× on mobile**: plan rebuild 8–12ms desktop vs
  27–36ms mobile. That half of the mobile-vs-PC story survives.
- **Frequency**: the dpr arithmetic (C1b) is sound but the conclusion drawn from
  it is falsified. Do not assume desktop is safe.
- **Mobile redraws full-screen** (`dirty-rect: full`, 2.6MP at dpr 3); desktop
  sometimes partial.
- **Two mobile browsers tested**, both with `navigator.gpu` present and both
  behaving identically on every finding: Safari 26.3 / iOS 18.7, and Chrome
  CriOS 151 / iOS 26.3. WebGPU reach on iOS is real.

### 2D ↔ 3D ↔ 3D-in-2D

- **2D only, camera pan** — clean. 0 produce, 0 plan, 0 comparator. 45–60fps on
  mobile. The atom works.
- **3D proper (the region's own orbit camera)** — `region-prepared: 1,
  region-encoded: 1` every frame is **lawful**: REGION3D-FLOOR §5.4's two-camera
  law makes a region's own orbit view session truth, explicitly *not* camera. Cost
  lands in connector (C4), not in the plan.
- **3D-in-2D (a region on screen while the workspace camera moves)** — this is
  where C1 fires, and **it is the case nobody designed for explicitly.** The
  region is a citizen of the 2D canvas whose *texture allocation* depends on the
  2D camera. That coupling is the whole finding. It is a seam between two
  contracts (FRAME-RETENTION's camera law and REGION3D-FLOOR's lease ladder), and
  seams are where this codebase's real bugs have lived all week.

---

# Area D — boot / time-to-interactive (added)

Nothing in FRAME-RETENTION touches this path. Mechanisms are entirely different:
font asset loading, text shaping/reconcile, GPU buffer allocation. **It is the
largest felt number in the whole session.**

**RECEIPT — iPhone Chrome run:**
- frame 1: `TOTAL 624ms` (region 408, conn 54, encode 115, n=43)
- frame 2: `TOTAL 2567ms`, of which **reconcile 2466ms** (produce 51, plan 21, n=263)
- `WARN [GROUND] slow reconcile 10123.0 ms — paste __ground.report() output`
- `[PACE] fps~1 raf p50 10312.0ms`

**RECEIPT — iPhone Safari run** (same shape, independent): frame 1 604ms
(region 456, encode 85); frame 2 2575ms (reconcile 2478); `slow reconcile
10596.0 ms`; `[PACE] max 3724ms`, later `max 10762ms`.

**RECEIPT — ~60 consecutive buffer reallocations, identical in both runs:**
```
[GPU-BUDGET] text/chrome buffer text-resize: 13.00 KB -> 19.80 KB
[GPU-BUDGET] text/chrome buffer text-resize: 13.00 KB -> 502.66 KB
[GPU-BUDGET] text/chrome buffer text-resize: 13.00 KB -> 1.50 MB
[GPU-BUDGET] text/chrome buffer text-resize: 13.00 KB -> 105.96 KB
…
```
**Every one starts from 13.00 KB.** Progressive growth would read 13→20→50→94.
**HYPOTHESIS: reallocation thrash — the buffer is rebuilt from base each time.
Not traced to a call site.**

**RECEIPT — font assets.** Chrome run: atlas.json 504ms, slug_meta 855ms,
slug_curve 877ms, shaper load-provider 880ms, slug_band 902ms, **atlas.png
1937ms**, decode 256ms. Safari run: 29 / 84 / 84 / 85 / 106 / 90 / 207ms.
**The order-of-magnitude gap between runs means these are tunnel/network
variance, not a code fact.** Do not build on them.

**RECEIPT:** `[BOOT] Configuring WebGPU canvas {"clientWidth":300,
"clientHeight":150,"devicePixelRatio":3}` — configured at the HTML default
300×150 before the real 393×773 viewport arrives.

## UNEXAMINED in Area D — all of it

- **`__ground.report()` has never been run.** The warning explicitly asks for it.
  Free, and it is the obvious first probe.
- The 60× realloc — never traced.
- Whether the 2.5s reconcile is shaping, layout, or upload.
- The 300×150 pre-configure.
- **All boot data is from the dev build.** Release may boot differently.

---

# Area E — correctness is unverified, and one closing receipt is false (added)

## E1. `scene_tape.cljc` — the protected-ground claim does not hold

`FRAME-RETENTION-NOW.md` states, in its own closing receipt:

> *"Protected ground: `scene_tape.cljc` SHA-256 remains
> `f47aa9f34279a0ad7fc29838c4a14f328c6370a2e1ee64aa49307725686c045c`; scene tape
> and GPU-golden diffs are empty."*

**RECEIPT — measured 2026-08-09:**
```
bdb0d1f94146ee6545248dd82d25570b1cbfe30b5e889c3e6ce5dd65f8ad571c  scene_tape.cljc
```
**The SHA does not match. The file has a one-line change, uncommitted:**

```clojure
;; image family :entry-paint-required-keys
-   [:sub-draws]
+   [:paint/source :paint/source-type :op-offset :instance-count]
```

**Assessment, offered as a reading and not a ruling:** the change is *coherent*
with FRAME-RETENTION §5d (stable payload indirection — paint becomes a reference
to a source system plus offsets, resolved at encode time, instead of carrying
resolved sub-draws). It looks intentional and correct. **What is wrong is the
receipt**, which asserts the file was untouched. That is precisely the class of
thing the falsification round and the courtroom exist to catch, and it slipped
through both.

**Consequence for a new session:** the other claims in `FRAME-RETENTION-NOW.md`
now deserve independent verification rather than trust — specifically
*"8 tests, 19 assertions"*, *"compile dev passes (320 files, 4 preserved foreign
warnings)"*, *"GPU-golden diffs are empty"*, and *"S1-S5 classes … pass"*. None
were re-checked this session.

## E2. The scene-tape fence was edited in the same change-set

`test/render_engine/verify_scene_tape_fence.mjs`, three edits:

1. `requireToken("image producer", …, "contiguous-binding-runs")` **moved** to a
   new `imageResolver` target, with an in-code justification citing *"FRAME-RETENTION
   §5d moved sub-draw derivation from the producer to encode-time resolution; the
   pin moves with it (T7: pins move, never delete)."* **Cites its law. Plausible.**
2. `requireToken("image registry", registry, ":execute! execute-image-batch!")`
   → `requireToken(…, "execute-image-batch!")`. **This is a weakening with no
   stated reason.** It no longer asserts the executor is registered under the
   `:execute!` key — only that the string appears somewhere in the registry.
3. The same weakening mirrored into the receipt object
   (`imageExecutorRegistered`).

**The fence protecting the scene tape was loosened in the same uncommitted
change-set that modified the scene tape.** Edit 1 is defensible on its face. Edit
2 is not explained. **This needs a human ruling, not a session's opinion.**

## E3. Retention has no runtime proof

- The twin oracle exists exactly for this —
  `globalThis.__softland_frame_tape_twin_check = true`, implemented at
  `renderer.cljs:3568-3592`, comparing the maintained arrangement against the
  batch compiler and throwing on divergence.
- **It was run for one desktop stretch this session and then turned off. It has
  never been run on mobile, and never across a full exercise of all families.**
- `FRAME-RETENTION-NOW.md`: *"Headless WebGPU verifier was attempted once:
  release compiled, then the run stopped at the T1 browser-layout receipt
  (`cluster-count 0`) before any FRAME-RETENTION scenario; no golden was
  rewritten."*
- **So S1–S5 have no runtime proof.** The 8 JVM tests that pass are pure-ledger
  tests.
- **Acceptance is Sid's word and has not been given.**
- A retention bug shows as *stale pixels*. You can stare at 60fps and not notice
  one. Speed receipts are not correctness receipts.

## UNEXAMINED in Area E

- Twin on mobile, across a full exercise (pan, zoom, type, drag, select, orbit,
  gizmo, connector).
- Why the headless verifier dies at T1 with `cluster-count 0`.
- Any independent re-check of NOW.md's other claims.

---

# Area F — mobile as a testable surface (added)

Small, but it gates C and E, and it is why the device receipt took a full day.

- **Tunnel:** cloudflared quick tunnel. Binary now at `~/.local/bin/cloudflared`
  (installed 2026-08-09, persistent — the previous one lived in a session
  scratchpad and died with it). Started detached via `setsid`, log at
  `~/softland-tunnel.log`. **The URL is random and dies with the process; a
  restart mints a new one.** Not ngrok — ngrok needs an auth token.
- **No console on mobile.** `?mdbg=1` (the overlay at `index.html:21-135`) is the
  only instrument: it captures `console.log` behind a `dbg` pill and turns red
  with an error count.
- **shadow-cljs hot-reload does not reach the phone over the tunnel** —
  `ERROR shadow-cljs - remote-error {"isTrusted":true}` at boot in both runs.
  Every code change needs a manual refresh on the device.
- **Consequence:** every mobile receipt is a round trip through Sid. That is a
  real tax on any perf work whose target is the phone.

## UNEXAMINED in Area F

- Whether the shadow-cljs websocket can be made to work over the tunnel.
- A stable URL (named cloudflared tunnel, or Tailscale) instead of per-run random.
- Whether Safari remote debugging is reachable from Linux (it is not, directly —
  Chrome-on-iOS via `chrome://inspect` needs USB and a desktop Chrome).

---

# Instruments, and the bias in each

**Read this before trusting any log in this dossier.**

1. **`[RAF]` only logs frames over 5ms** — `render.cljs:879`,
   `(when (> (- raf-t4 raf-t0) 5) …)`. **Every `[RAF]`/`[DRAW]` line we have is
   ≥5ms by construction.** We have never seen the frame-time distribution, only
   the slow tail. All reasoning from those lines is biased pessimistic.
   **`[PACE]` (fps, p50, p95, max) is the unbiased instrument** — prefer it for
   any rate claim.
2. **`fams:` only prints families whose produce time was measurable.**
   Sub-millisecond families are invisible there. This is exactly why `n=1` was
   unattributable for three rounds.
3. **`__softlandFrameLedger`** — `frame_inputs.cljc:206-233`, published each
   frame. The counters increment **inside the real function bodies**, not beside
   the call sites (contract §5a's anti-receipt-gaming rule). That is why it is
   trustworthy where a call-site counter would not be.
4. **The `changed:` field on `[DRAW]`** — added this session,
   `render.cljs:897-903`, uncommitted. Prints `changed-families`,
   `plan-maintained?`, `region-prepared`. **This one field is what cracked C1.**
   Keep it until the perf work closes.
5. **The twin doubles the frame's work by design** — off for timing runs, on for
   correctness runs. Never both.

---

# What was checked and RULED OUT — do not redo

- `update-frame-arrangement` is **not** the arrangement-rebuild site. It is
  genuinely incremental — `renderer.cljs:3534-3558`.
- `ordered-insert` / `ordered-remove` are plain `assoc` / `dissoc`, O(log n).
  **Not** the source of 1974 comparisons — `scene_tape.cljc:708-719`.
- `input-value-same?` has a full `=` fallback, so a value-equal freshly-built
  `viewport` map does **not** trip the plan gate — `frame_inputs.cljc:111-124`.
- **The retention fence itself is sound.** Raw camera is structurally
  undeclarable: `forbidden-declared-inputs #{:frame-idx :pan-x :pan-y :zoom
  :pixel-size}` — `frame_inputs.cljc:29-30`. A family cannot accidentally depend
  on the camera.
- **Paths and connectors are not a zoom-cost problem in the normal range.** There
  are exactly three zoom regimes over the whole span — `:legal-min` 0.01–0.1,
  `:floor-default` **0.1–8.0**, `:legal-max` 8.0–1000 —
  `path_material.cljc:14-44`. Ordinary zooming never crosses a path boundary.
- `region-prepared: 1` **alone** does not force a plan rebuild — 3D-interaction
  frames show `region-prepared: 1` with `plan-maintained?: false`.
- **The invariant holds for pure camera pan on both platforms.** No need to
  re-verify C0.

---

# Method — how this session searched, so you can judge the search

Sid asked for this explicitly: *"its both finding and how you took the problem
and did the search for how to fix it."*

1. **The register drifted and nobody caught it.** This opened as a summarization
   task and became an investigation the moment a log was pasted. The felt/product
   question (Area B) was displaced by the engineering one (Area C) — for the
   second time in two days. If a future session opens on a perf complaint,
   **decide the register before the first read.**

2. **Structure was asserted; magnitude was refused.** Per the investigation
   fence: claims like *"raw zoom is structurally undeclarable"* were made from
   source with anchors. Claims like *"the ladders collapse, therefore this is a
   mobile problem"* were tagged as prediction. **That discipline is the only
   reason the falsification in C1b was clean instead of embarrassing.**

3. **The decisive move was cheap, and reading harder would not have found it.**
   Three rounds of source reading could not identify which family produced the
   `n=1` entry, because `fams:` hides sub-millisecond families. **One log field
   answered it in a single gesture.** Generalizable: *when reading cannot
   distinguish two hypotheses, add a field rather than read harder.*

4. **What falsified the author.** A dpr-arithmetic prediction that region-3d
   would appear "sparsely" on desktop. Sid's desktop receipt showed it readily.
   The arithmetic was right; the step from arithmetic to observed frequency was
   not. **Be suspicious of every constant→frequency inference in this dossier.**

5. **Delegation was skipped, and probably shouldn't be next time.** Project law
   (CLAUDE.md) routes gathering to Opus subagents. This session ran narrow greps
   and seam-scoped reads inline because each was a few hundred lines. **The
   sweeps named below — tree-diff attribution (A), the boot path (D) — are broad
   and should be delegated.**

6. **Where the search stopped, and why.** Never read: the plan / effect-span
   internals, the sorted-map construction site, `__ground.report()`, the boot
   path, `region3d_placement_gpu.cljs`, the uncommitted diffs. Each stopped
   because the next receipt was cheaper to obtain from Sid's device than from
   reading. That was right for speed and wrong for completeness — **the whole GPU
   side of every measurement in this dossier is unexamined.** Every number here
   is CPU. There are no GPU timestamp queries and no frame capture. If the GPU is
   the real wall at some zoom levels, nothing here would show it.

---

# The branches nobody opened

Ranked by expected value, most valuable first.

1. **Where the arrangement's sorted map gets rebuilt** (C1a). Single highest-value
   probe in the dossier. If it exists, it partially undoes the atom.
2. **`__ground.report()`** (D). Free, explicitly requested by the warning, and
   points at a 10-second stall.
3. **Which `plan-inputs` member trips** (C2). ~5 lines.
4. **Twin oracle on mobile, full exercise** (E3). Correctness has zero device
   proof.
5. **The `scene_tape` / fence ruling** (E1, E2). Needs Sid, not a session.
6. **Whether `maintain-frame-plan` / `maintain-effect-spans` can be incremental
   at all** — never read. If their cost is inherent, C1's fix has to be "don't
   mint the shape" rather than "make the rebuild cheap," which changes the whole
   approach.
7. **The 256 quantum as a design question** — dpr-relative, or scale-space like
   its sibling ladder? Not investigated at all.
8. **Encode profiling** (C3) — the new floor, never opened.
9. **The GPU side of everything.**
10. `max-lease-size` clamping (C1b) · connector re-route vs re-label (C4) · clip
    churn (C5) · selection re-produce (C6) · the 60× realloc (D) · the 300×150
    canvas pre-configure (D).

---

# Starter prompts

Direction and pointers only, per the work-package law. Each assumes the dossier
is read first; none restates its findings.

**Preflight for all of them: set permission mode and toggles BEFORE the first
prompt** (token-economy law — mid-session toggle changes rewrite the whole cached
prefix).

### C — the region-3d shape-mint chain

> Investigation, not a contract cut. Boot: this dossier (Area C, §C1 and §C1a)
> and `docs/render-engine/FRAME-RETENTION-CONTRACT.md` §5g PRIMARY. A camera-only
> frame re-derives nothing and that is measured on two platforms; the open
> question is why a single changed Region3D entry appears to rebuild both the
> arrangement and the frame plan. The dossier names the kill-probe and lists what
> was already ruled out — start from the probe, not from re-reading. Receipts
> before verdicts: no attribution ruling and no correction contract until the
> probe returns. If the probe shows the rebuild is real, the fork worth putting
> to Sid in one line is whether to make the rebuild incremental or to stop
> minting the shape.

### D — boot

> Investigation. Boot: this dossier (Area D). Ten seconds to interactive on a
> phone, reproduced on two browsers, and nothing in FRAME-RETENTION touches that
> path. `__ground.report()` has never been run and the warning explicitly asks
> for it — start there. The ~60 buffer reallocations all starting from the same
> 13.00 KB are a hypothesis, not a traced fact. All existing data is dev-build;
> whether release differs is unknown.

### E — the custody ruling

> Adjudication, needs Sid's word at the end. Boot: this dossier (Area E) and
> `docs/render-engine/FRAME-RETENTION-NOW.md` PRIMARY. The atom's own closing
> receipt asserts `scene_tape.cljc` is byte-unchanged; it is not, and the fence
> that protects it was loosened in the same uncommitted change-set — one edit
> cites its law, one does not. Read both diffs whole before forming a view. The
> question is not whether the grammar change is correct (it looks coherent with
> §5d) but what a false closing receipt means for the rest of that document's
> claims, none of which were re-checked.

### B — the felt pass

> **Product session. Not engineering.** Boot: `docs/render-engine/REGION3D-SEAM-CONTRACT.md`
> §1 and §9 PRIMARY, and this dossier's Area B for what is already known.
> Sid's words: *"that is not product that is eng."* The last two attempts at this
> were eaten by performance investigations — if a number tempts you mid-session,
> write it down and keep walking the surface. Atom B has been BUILT and banked
> since 2026-08-08 with acceptance PENDING; the zoom seam Codex named has never
> been tested; the FELT-SLOW debt from Atom A is still owed its ruling.

### A — the tree

> Mechanical, delegate the gathering. Boot: this dossier (Area A). Eighteen
> modified files, four distinct bodies of work, eight files unattributed to any
> of them. Attribute each hunk before proposing a commit split. FRAME-RETENTION
> has no acceptance, so committing it as done would be a false receipt; the touch
> adapter and the two GPU repairs are independently defensible.
> `.frame-retention-impl.log` and `docs/.obsidian/` should not land at all.

---

*Written 2026-08-09 by Fable 5 (`claude-fable-5`), effort max. Every number here
came from a real run; every interpretation next to a number may still be wrong.*
