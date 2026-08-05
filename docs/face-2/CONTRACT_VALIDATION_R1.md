# CONTRACT_VALIDATION_R1 — face-2 (the camera ground) · fresh-context, default-FAIL

**Date:** 2026-07-06
**Model:** Opus 4.8 (1M), fresh context, contract-validator role.
**Subject:** `docs/current-mental-model/build/face-2/CONTRACT.md` v1 PROPOSED (authored at git sha `719a9d9`).
**Default-FAIL statement:** this verdict starts at FAIL. PASS is only reachable by explicit
scenario tracing with citations. I did not author this contract and owe it nothing; the point
of this pass is a kill record. Findings below are traced against the code and the binding docs,
not vibed.

**Inputs actually read (path — what for):**
- `build/face-2/CONTRACT.md` (subject, full).
- `decisions.md` (D-001, D-005, D-006 notes, D-008, D-009 — the binding chain).
- `build/render-north/DELTA-B1.md` (every Δ the contract fires, incl. Δ5/Δ8/Δ9/Δ12/Δ13/Δ15 triggers + falsifiers).
- `build/render-north/FORK-2.md` (D-009 ONE SUBSTRATE record).
- `build/render-north/PROBE-10K.md` (§2/§3/§4/§6 obligations; JVM `->seq-differ`+`patch-vec` provenance).
- `build/render-north/NORTH.md` (§2/§3/§8/§9/§11/§12), `MECHANICS.md` (§1/§4/§5/§7/§8), `HARD-PROBLEMS.md` (H1–H10).
- `build/trail-room/CONTRACT_R2.md` v1.1 COUNTERSIGNED (the floor — §2.1, §2.4, §2.5, §5 allowlist).
- `design/claude/render-demands-2026-07-05.md` (W1–W5, D1–D15), `room-card-lane-2026-07-05.md` (R1–R7).
- Code @ working tree (branch `docs/current-mental-model-local`, base sha `719a9d9`; builder branches live):
  `renderer.cljs` (camera struct, `update-camera`, `camera-floats`, slug AA, `clone-text-system`),
  `buffer_pool.cljs` (`gpu-mount`), `rect_tree.cljc` (`:actions`, `tree->text-ops`, `dispatch-event`),
  `trail_face/scene.cljc` (ns docstring), `combined_text.cljs` (trail-face flatten),
  `runtime/render.cljs` (draw call + `[RAF]`), `runtime/state.cljs` (`!trail-face-state`),
  `server/rama/trail_view.clj` + `relation_kernel.clj`, `deps.edn`.

---

## VERDICT: **FAIL** — 2 blockers, 2 should-fixes, 3 advisories.

Two of the contract's in-scope items cannot be executed as written against the actual tree:
Δ13/G14 (clip) collides with the contract's own rect_tree fence, and the Δ9 server-side
projection both (a) fires on imagined-demand grounds against D-001 + the binding R-2 §2.1
promotion condition and (b) is not reachable inside the §6 allowlist as the trail-view module is
actually structured. Both are text-time fixable; neither is fatal to the design. Everything else
(birth laws, camera wiring numbers, six-op JVM testability, descriptor/spec/token gates)
traces clean.

---

## BLOCKERS

### B1 — Δ13/G14 "no per-op text dropping" contradicts the §6 rect_tree fence (+ §12 stop-clause)

**Contract text (§3.2, Δ13):** *"Text clips at pixel grain — the line-pop per-op drop retires."*
**Contract text (G14):** *"ONE clip path — flatten emits clip-stack indices; no per-op text dropping on the trail path."*
**Contract text (§6 UNTOUCHED):** *"`rect_tree.cljc` (Δ21 — the island grammar core …)"* and
*"LEAVE-ledger items Δ17–Δ22 touched at all = a gate finding."*
**Contract text (§12):** *"Pre-flagged stops: `rect_tree.cljc` needs changing (§6)."*

**Trace (kills it):** The trail face's text ops are flattened by `rect_tree/tree->text-ops`, not by
any trail-face-local code:
- `combined_text.cljs:284` — `[(if trail-face-scene (tree->text-ops trail-face-scene) [])` (the
  "Trail face (WP-B2): flatten from the CACHED scene" branch).
- `rect_tree.cljc:305–349` — `tree->text-ops` performs the **per-op text drop** (`in-clip?`,
  :322–343, drops ops outside the clip) and **per-op truncation** (`truncate-op`, :310–321, cuts
  text at `clip-right` using the 0.56 advance). This IS the "line-pop per-op drop" G14 retires.

To satisfy G14 ("no per-op text dropping … flatten emits clip-stack indices") you must stop the
drop/truncate and instead emit a clip-stack index — i.e. edit `tree->text-ops` inside
`rect_tree.cljc`. The clip-index the §6 AMEND puts in `renderer.cljs` cannot un-drop ops that
`tree->text-ops` already discarded upstream, and `combined_text.cljs` is allowlisted only for
"mode-branch lines." So the only place the per-op drop can be retired is the fenced file. §12
then forces a stop-clause. An in-scope item (Δ13/G14, fired by the camera going live per §1) is
therefore un-completable in-wave under the contract's own fence — and G-Δ would flag exactly this
at gate time (a LEAVE item touched, or a fired trigger's falsifier unmet).

**Smallest fix:** add a NAMED fence exception (the way §6 already carves out Δ19's frame loop)
authorizing the clip-emission edit to `rect_tree/tree->text-ops` ONLY — *"the ONE lawful
rect_tree edit: `tree->text-ops` swaps per-op clip drop/truncate for a clip-stack index; layout,
hit-path, and node grammar untouched."* Alternative fix: narrow Δ13's in-wave scope to
world-space **rect/bg** clip + hit-test-against-clip, explicitly DEFER the text line-pop
retirement to a later rect_tree touch, and reword G14 to drop "no per-op text dropping" this wave.

### B2 — Δ9 server-side layout projection: imagined-demand grounds vs D-001 + R-2 §2.1, and unreachable inside the §6 allowlist

**Contract text (§3.3):** *"It runs server-side in the trail-view module's QUERY surface … No new
depots, no ETL topologies … grounds for taking Δ9 now: the plural-lens demand is Sid's, the
transform is already pure cljc (lift cost ~0), and agent-visible layout rows are what make
walk/spec joins possible later."*
**Contract text (§6 AMEND):** *"server: `trail_view.clj` + its test ns (the named projection
queries ONLY — additive)."*
**Binding conflict — R-2 §2.1 (COUNTERSIGNED, BINDING):** *"Alternative NOT taken: a server-side
thread projection/query — zero new query machinery is the standing win; promote only if the
client component pass shows up in frame profiles (form-break evidence, D-001)."*
**Binding conflict — D-001 (CLOSED):** *"'An imagined future form would need X' is not a valid
reason to build X."*

**Trace (kills it), two independent legs:**

1. *Grounds fail D-001 and R-2 §2.1.* All three grounds the contract offers for moving layout
   server-side NOW are imagined-demand, not the frame-profile form-break R-2 §2.1 requires: the
   plural-lens is a *demand* (satisfiable client-side), "lift cost ~0" is a *cost* argument, and
   "walk/spec joins possible later" is explicitly *future*. No frame-profile evidence exists —
   the corpus is 10²–10³ and PROBE-10K §3/§6 + H1 both say client-side is fine at this scale. The
   contract's reconciliation leans on *"DELTA-B1 (later, sitting-ruled) sets Δ9's trigger"* — but
   DELTA-B1's own header says **"DIRECTION-GRADE INSTRUMENT … It does not start code … every item
   is D-001-paced,"** and decisions.md D-009 blesses only Δ1/Δ2/Δ3 as firing at face-2, never Δ9.
   A non-binding instrument cannot override a binding contract's D-001 form-break gate. (The
   plural-lens SHAPE is legitimate to build, like the W5 spec shape — it is the server-side
   PLACEMENT that lacks a form-break.)

2. *Not reachable inside the §6 allowlist.* `trail-view-module` mirrors ONLY object-container
   PStates (`trail_view.clj:286–300`) — it mirrors **no relation-kernel PState**. Relations reach
   the client via foreign queries to the relation kernel (`read-context-bundle:546`,
   `read-recent-activity:623` → `rk/read-relation-activity`), and the feed (the projection's
   input) is assembled **client-side** in `assemble-feed` (`:596`), per the recorded
   CLIENT-COMPOSITION path (ns docstring, lines 14–25). Connected-components over lineage edges is
   a whole-corpus cross-partition join; to run it inside a trail-view query topology you must add
   (i) a NEW cross-module `mirror-pstate` of a relation-kernel edge PState
   (`$$relations-by-id`/`$$relations-by-target`/`$$relation-activity-by-bucket`) and (ii) a NEW
   `|all$$`/`|origin` gather topology. Neither is "the named projection **queries** ONLY —
   additive"; a mirror is not a query. So the item as scoped exceeds its own allowlist. (It does
   NOT breach gate 14 — a mirror is read-only — so §12's "depot/ETL" stop-clause would not even
   fire to catch it; the builder just hits an un-authorized surface.)

**Smallest fix:** keep Δ9's plural projections CLIENT-SIDE this wave — two pure named cljc fns
(`:layout/trail-time-lane@v1`, `:layout/trail-flat@v1`) run at diff rate over the client-assembled
feed; the scene build reads their address-keyed rows. This satisfies Sid's world-and-lenses
demand and the load-bearing half of the Δ9 falsifier (named + plural + no per-FRAME world-layout
recompute), costs nothing D-001 forbids, and honors R-2 §2.1 by DEFERRING the server placement to
the frame-profile form-break it (and H1) already name. Reword G10's *"zero client-side
world-layout recompute"* → *"zero PER-FRAME world-layout recompute"* so the gate tests the real
property (layout out of the camera/frame path), not a placement. If server-side is insisted:
supply the frame-profile evidence R-2 §2.1 demands (there is none today) AND add the mirror +
gather topology to §6 explicitly.

---

## SHOULD-FIX

### S1 — Chrome does NOT have its own camera buffer: §4.5 citation is false; chrome shares text-sys's camera AND bind-group

**Contract text (§4.5):** *"Chrome camera: identity uniform on the existing separate buffer …
the buffer exists (two `update-camera` calls already differ by system)."*
**Contract text (§3.2):** *"the buffer already exists — chrome-text-sys has a separate
`update-camera` call."*

**Trace:** `renderer.cljs:902–915` `clone-text-system` — *"Create a lightweight text system clone
**sharing pipeline, bind-group, camera**, and font resources with the parent. Only the instance
buffer is new."* chrome-text-sys is `(assoc parent-text-sys :instance-buffer …)`, so it inherits
text-sys's `:camera-uniform-buffer` (and `:bind-group`). Consequently the guard at
`renderer.cljs:1561` — `(not= (:camera-uniform-buffer chrome-text-sys) (:camera-uniform-buffer
text-sys))` — is **always false**, and the "separate" chrome `update-camera` at `:1562` is DEAD
code that never runs. So the two `update-camera` calls do NOT "differ by system"; chrome and
text-sys share ONE camera buffer and ONE bind-group. Under the contract's plan (text-sys → live
zoom, chrome call stays `1.0`), chrome would zoom WITH text-sys — producing exactly the chrome
swim (trap 7) §4.5 claims to prevent. G8's pure store test does not exercise GPU buffer sharing,
so only the visual G-FL would catch it, late.

**Smallest fix:** correct §4.5/§3.2 — chrome currently shares text-sys's camera buffer AND
bind-group (`clone-text-system`, renderer.cljs:902–915); an identity-camera rim requires a NEW
dedicated camera buffer PLUS a NEW bind-group for chrome-text-sys (not the existing dead separate
call). Add a §8 verification duty ("confirm/allocate chrome's own camera buffer + bind-group")
and bind the risk to G8 (make G8's chrome half a renderer-integration check, not only a store
test).

### S2 — Traps 5 and 16 have no enforcing acceptance gate

**Contract text (trap 5 ruling):** *"LAW B3; updates at the reduce/consumer edge only;
falsification hunts this class."* **(trap 16 ruling):** *"camera is spec-value view-state;
assertions wait for D-008.5."*

**Trace:** §9 gates are "executable unless marked visual," but the store-write-at-consumer-edge
invariant (LAW B3 / trap 5 — the CLAUDE.md side-effect ban + diamond-glitch, the single most
Missionary-dangerous rule in this wave) is routed ONLY to §11 falsification prose, with no
numbered gate. Trap 16 similarly has none (defensible — it's a NOT-in-scope refusal). Per the
contract's own gate discipline and the traps-need-gates rule, B3 deserves at least a mechanical
check.

**Smallest fix:** add a grep/AST gate to §9 (e.g. G-B3): "no `reset!`/`swap!`/`writeBuffer` on the
store reachable from inside an `m/latest`/projector body; the store mutates only at the named
reduce/consumer edge." Cheap, and it converts a falsification-hope into a repeatable gate.

---

## ADVISORIES

- **A1 — "all five shaders" overcounts (§3.2).** `renderer.cljs` declares `struct Camera` in FOUR
  shaders (`:7, :131, :234, :283` — rect / rich-rect / MSDF / slug); the 5th pipeline, clear-quad
  (`:1023`), is cameraless and correctly so. Reword *"all five shaders"* → *"all four content
  shaders (clear-quad is cameraless)."* Load-bearing part is accurate.
- **A2 — shadow pipeline omitted from Δ13's clip set.** §3.2/§6 amend the clip-index into
  "rect/MSDF/slug"; §8.6's duty names FOUR instance layouts ("28/20/12/24-float"). The shadow
  system (`init-shadow-system`, `:918–924`, stride 80 = 20 floats) is not in the clip set. Name
  whether island-frame shadows clip under the unified representation, or record shadow-clip as
  out-of-scope this wave.
- **A3 — chrome "separate update-camera call" is dead code (§3.2).** The call at `:1562` exists in
  source but never executes (guard always false, S1). Subsumed by S1; noted so the cite is not
  read as live evidence.

---

## GATE-BY-GATE EXECUTABILITY

| Gate | As-written | Note |
|---|---|---|
| G1 (Δ1/B1 keying) | **OK** | pure JVM; fan-out index + pair-pick well-defined |
| G2 (Δ2 float math) | **OK** | numbers discriminate: f32 ulp @~1e9 ≈ 128 world-units → >0.5px at zoom≥1; anchor-relative <1e4 → ulp ~1e-3 → <0.01px. §8.5 pins ε. Pure JVM float |
| G3 (B2 discipline) | **OK** | grep + signature assertion; anchor-relative encode-path check |
| G4 (Δ3/Δ5 one store) | **OK** | identity-check + `build-*-tree` grep gate |
| G5 (B5 six-op) | **OK** | `hyperfiddle.incseq/patch-vec` on JVM classpath (deps.edn electric; PROBE ran it on JVM) |
| G6 (B4 API) | **OK** | keyed-map return surface; rank-as-data |
| G7 (Δ4/Δ8 counter) | **OK** | counter-assertion feasible given the stratum split; `identical?` needs the cached-scene law (INV-11) |
| G8 (camera/W4) | **FINDING-ref S1** | cursor-zoom math OK; chrome-invariance half not exercised by a pure store test (buffer sharing is a GPU detail) — real risk caught only by G-FL |
| G9 (trap 8) | **OK** | counter-assertion; near-tautological (zoom is a uniform) but valid; band-cross half needs a boundary-spanning fixture |
| G10 (Δ9) | **FINDING-ref B2** | presumes server-side placement ("zero client-side recompute"); server-side unreachable in §6 allowlist; reword to "per-frame" |
| G11 (Δ10 spec) | **OK** | EDN round-trip w/ `:spec/schema-version`; (spec+feed)→scene reproduce |
| G12 / G12b (Δ11) | **OK** | address/mark-set equality; fold counts sum to unfolded set |
| G13 (Δ6 descriptors) | **OK** | EDN round-trip + registry-refuses-world-changing (D-008 wall) |
| G14 (Δ13 clip) | **FINDING-ref B1** | "no per-op text dropping" requires editing fenced `rect_tree/tree->text-ops` |
| G15 (Δ14 FSM) | **OK** | pure decision fn over synthetic hits at zoom 0.5/1/2 |
| G16 (Δ15 cond-RAF) | **OK** | both outcomes valid; measurement-gated |
| G17 (Δ7 glyph) | **OK (unverifiable outcome)** | gated on BRANCH_REPORT_D7 (mid-build); records OI-2 interim |
| G18 (suite green) | **OK** | HEAD baseline, no hardcoded count |
| G19 (file(1) text) | **OK** | standing gate |
| G-FL (visual) | **OK** | catches S1 chrome-swim + B1 clip-pop, but late/visual |
| G-Δ (instrument) | **OK — and confirms B1** | reading DELTA-B1 vs the diff at gate time WILL surface the Δ13-vs-Δ21 fence collision (B1) as a self-inflicted finding |

## CITATION VERIFICATION

| Contract cite | Status |
|---|---|
| `update-camera` 4 hardcoded `1.0` sites @1516/1517/1559/1562 | **VERIFIED** (exactly four) |
| `update-camera` already takes zoom param (@1494) | **VERIFIED** |
| `camera-floats (js/Float32Array. 6)` @1107 | **VERIFIED** |
| slug AA zoom-aware @320 (`dilation = 0.5 / max(camera.zoom,…)`) | **VERIFIED** |
| LAW B2 as-built f32 shader math @7,41 | **VERIFIED** |
| "camera struct … in all five shaders" (§3.2) | **DRIFTED** → A1 (4 of 5; clear-quad cameraless) |
| chrome "own uniform buffer / two calls differ by system" (§4.5/§3.2) | **FALSE/STALE** → S1 (clone shares camera+bind-group, :902–915; guard @1561 always false) |
| `gpu-mount` §419-437 (§13) | **VERIFIED** (defn @419; docstring documents the insert-before/permutation corruption) |
| `rect_tree` `:actions` @51, `dispatch-event` @407 | **VERIFIED** |
| `scene.cljc` "the camera carries scroll" docstring | **VERIFIED** |
| `!trail-face-state` @198 (state.cljs) | **VERIFIED** |
| `trail_view.clj` query-only (no depots/ETL/own PStates → gate 14 holds) | **VERIFIED** — but mirrors OC ONLY; relations client-composed (basis for B2) |
| `hyperfiddle.incseq` on JVM classpath (scene_store pure-cljc / G5) | **VERIFIED** (deps.edn `com.hyperfiddle/electric`; PROBE-10K §8 JVM run) |
| scenes scroll-independent; `pan-y` generalizes (§3.2) | **VERIFIED** (no conflict with R-2 §1/S7 geometry — face-2 does not change y=time) |
| PRIMITIVES `!zoom-factor` stale (trap 19) | not re-checked (PRIMITIVES not read); the contract already flags it stale |

## WHAT I COULD NOT VERIFY (honest list)

- **GPU-visual claims** — actual chrome swim, clip pop, pixel-grain text under fractional zoom
  (G-FL, G14 visual half): no GPU/render in this pass.
- **BRANCH_REPORT_D7 / slug glyph-set outcome** (G17): the font/atlas builder branch is mid-build.
- **R-2 as-LANDED shape** (BRANCH_REPORT_R2): builder branch live; per instructions I judged §3.3/§6
  against CONTRACT_R2's CONTRACTED end-state, not the mid-build `trail_face/*` tree. §8.1
  re-verification remains a real duty.
- **t4-spine landed `claimed-ms` / trail_view.clj feed edits**: mid-build; not the base I checked.
- **Whether the editor diff-view camera buffers (primary-sys/comparison-sys @1516/1517) are
  separate at runtime** — not load-bearing for the trail face; not chased.
