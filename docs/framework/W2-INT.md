# W2-INT — integration artifact (faces-as-assemblies, Wave 2)

2026-07-11 · Fable orchestrating session (lanes D/E ran as parallel Opus 4.8
subagents; INT + all gate fixes implemented directly — the 2026-07-05 (e2)
ruling, doubly so after Sid's mid-wave token flag). Binding: CONTRACT v2
§§16–21 (G24–G26).

## What was wired

- **Server boot chain** (`file_viewer.cljc`, the face-projection-runtime
  delay): arsenal module launch (synchronous — the handle joins the runtime
  map) → WAL replay → faces initial-sweep → live watcher (replay/sweep/watch
  in a FUTURE per the trail precedent + gate fix F8; boot failures yield a
  poisoned-but-TOTAL map per fix F7). `face-ctx` gains `:arsenal-rt`.
  Verified live: `replay: 0/0 → sweep: 3 of 3 imported → watcher running →
  default conversation distilled river=247`.
- **Electric surface**: two more GENERIC pulls through the SAME `FacePull`
  (assembly-source at wear-time; face-list for the roster — dispatch stays
  server-side, T8) + the codebase's FIRST write e/defn `RecordFaceWear`
  (outbox atom → `record-wear-safe!` totality wrapper → arsenal; never
  through the read artery, T15).
- **Wear-from-Rama** (`face_wiring.cljs`, rewritten): the W1 HTTP fetch of
  `/faces/<name>.edn` is RETIRED — `/face <name>` arms the `:assembly`
  request; the served source compiles client-side with the same `.cljc`
  compiler the server validates with (T18); value-compare on the source
  string (T13) + the F4-era publish guard; wear-id minted client-side, time
  stamped server-side; epoch re-stamps close the save→re-render loop.
- **Sidebar FACES section** (`sidebar.cljs` + the flow chain): roster from
  Rama via `:!face-list` (T14) — name + status glyph (● worn · ○ candidate ·
  × retired · ⚠ invalid) + wear count; honest empty/unavailable rows;
  display-only (click = no-op; wearing stays on the command until
  `:actions`). Threaded through `<sidebar` (6/6 arity), render pass-throughs,
  and the scroll clamp.
- **`face-arsenal` registered as kernel #6** in the KERNEL-SHAPE taxonomy
  (intent-only; `:face/name` partitioner; counts updated 5→6).
- **The superseded W1 test pin** (`g13-serve-dispatch` registry
  `[:conversation]`) amended to the three-projection set.

## The name lesson (found by the first live run)

Faces are addressed by their ENVELOPE name (`outline-face`), not the file
stem — W1's fetch resolved by filename; W2's Rama serve resolves by the
face's true identity (§17). `/face outline` now error-cards honestly (and
its attempted wear logs truthfully). The sidebar shows the wearable names.

## G24 — the arsenal live (fresh JVM, real corpus; W2-INT-live-results.json)

- **Roster from Rama**: 3 faces (`boxes-face`, `minimap-reader-face`,
  `outline-face`), statuses + `valid? true` + zero counts at first light.
- **Wear-from-Rama**: `/face outline-face` → compiled from the `:assembly`
  projection (no HTTP) → wear count 0→1 visible in the roster via the ack
  refresh.
- **Scrub/epoch re-pulls append NOTHING** (T15 live-asserted): count 1
  before and after `/face until <ms>` + `until off` + epoch traffic.
- **The Step-7A loop, live twice** (both JVMs): edit `outline.edn` on disk →
  watcher import → epoch push → debounced re-pull → changed source →
  recompile fired with NO re-wear command. Claude Code can author faces
  through the watcher from this moment.

## G25 — the flip (Step-3 exit; fresh JVM, real `7c80ce2a`, post-fix)

The same conversation (35 turns / 64 blocks, page 1 of 247, truncation
honest) worn through THREE faces, flipped live by command; final counts
[boxes-face 1, minimap-reader-face 1, outline-face 1]:

- **Outline** — 134 rects / 574 text ops, content 16,161px
  (`W2-INT-outline.png`).
- **Boxes** — 150 rects / 405 text ops, 9,550px: the russian-doll
  containment anatomy live — turn frames ⊃ header bands (right-aligned
  meta) ⊃ block cards, long blocks clipped at ~6 lines with honest "▸ N more
  lines" stubs (`W2-INT-boxes.png`).
- **Minimap + Reader** — 95 rects / 289 text ops, 5,582px: proportional
  density strip (√words sliver heights, kind palette, per-turn word counts,
  the `:river-page-has-no-cursor` paging-lack shown at the strip's foot) +
  the reader pane at full fidelity (`W2-INT-minimap.png`).
- Screenshots are CPU rasterizations of the LIVE `!face-scene` trees (the
  exact rects/text ops the GPU consumes) — the W1 environment lack #5
  stands (GPU swapchain never composites into CDP on this box).

**Reader-turn serve (D-010 revert-cheap, applied at INT):** the first live
minimap wearing rendered its reader pane as an HONEST error card
(`each-not-seq` over the absent `:reader-turn`) — lane E's logged lack #2
doing exactly what the guard promises. The data serve was a 10-line additive
projection key (`:reader-turn`, `:params {:focus-turn <order>}`-aware,
default = first served turn), so it landed rather than staying carded; focus
CONTROL (click/scroll) remains `:actions`-era work. The pre-fix error-card
screenshot was itself the totality law rendering correctly.

## Lacks exposed by the wearing (D-005: each is ORDERED work)

1. **Pair structure** (lane E lack 1, now double-confirmed): both new faces
   want turn ⊃ user|response pair structure the projection cannot serve —
   THE machine-cut evidence item (ROAD Step 5's timing rides this, §20).
2. **Reader focus control**: the data serve landed; picking the focused turn
   by click/scroll is `:actions`-era (D-008 item-5).
3. **Paging** (carried from W1): still the first data-work item.
4. **Name-conflict row field** (gate residue W2-F5): warn-only today.
5. **Dev-boot relation kernel**: lineage edges skip in the dev wearing boot
   (no rk runtime attached); G19 proves them in-suite. Pre-named extension.

## Run record

Same method as W1 (puppeteer + `window.__softland_atoms` + synthetic
keydowns), with two W2 discoveries: the bundled Chromium has no WebGPU — the
driver MUST use system Chrome (150) with `--enable-unsafe-webgpu
--enable-features=Vulkan`; and rect ops carry FLAT `r g b a` keys while
text-ops nest per-node (the rasterizer's first two blank-PNG runs were
mapping errors, not render failures — the op counts always matched).
Suite at close: **46t / 804a / 0f / 0e**. SLOT re-checks in W2-GATE.md.
