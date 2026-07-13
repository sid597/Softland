# scene-substrate — thread file

## STANDING (frozen at open, 2026-07-12)

- Binding docs: `decisions.md` > `CONTRACT.md` (this package) > this file.
  This file is a baton, not a source of truth; if it contradicts
  CONTRACT.md or decisions.md, those win — flag the discrepancy in NOW.
- Package: birth the scene substrate (store + container transforms + pick
  seam + actions router) per CONTRACT v1. Phases P1–P4; P5+ staged.
- New-files allowlist: `scene_store.cljc`, `containers.cljc`, their tests,
  the P2 dev probe (tagged, delete-to-remove). Renderer/shader edits in P2
  only; face-path edits in P3 only. Everything else out of scope.
- Coordination: block-write + machine-cut edits sit UNCOMMITTED in
  `runtime/*`, `electric_flow.cljc`, `face_projection.clj`,
  `block_distiller.clj`. Keep P2/P3 diffs additive beside them; commits
  are coordinated by the machine-cut close session + Sid (board ⚠ T11).
- Verification duties: platform claims (WGSL struct alignment, stride,
  storage-buffer limits) checked against renderer.cljs + a live probe
  before code lands, not from memory.
- Definition of done: G1–G10 green + G11 worn by Sid + falsification pass
  + gate review + retro.
- Stop clauses: CONTRACT §9. Never patch around a wall; re-derive.
- Hard rules: docs commits on the local docs branch as-you-go; code and
  docs never in one commit; code commit timing is Sid's call; never read
  `env.clj`.
- Waits on Sid: nothing to start. Naming pass on "scene-substrate"
  (cheap re-rule), G11 wearing when P3 lands.

## NOW (append per session, ≤15 lines each)

**2026-07-13 · Fable · GATE REVIEW PASS + RETRO — package closes on Sid's two looks**
- Suites re-run by the gate session: 68t/888a green. ONE finder over the
  FULL diff (Opus, 192k): PASS-with-fixes, 0 HIGH. Record: `GATE.md`.
- Fixed at gate + compile-verified in the live watch: F1 echo trigger
  re-keyed to face-context identity (per-keystroke full repack of every
  copy — gone; content behavior identical) · F2 bundle build guarded on
  the agent-submit path · F3 cid recycling (overflow = reactor death).
- F1's lane half (copies consume the overlay-merged source) folds into
  P3c — it IS P3c's wall #1; per-slot echo diff (F8) joins it there.
- Backdrop-card pool concern REFUTED by trace (F5). Open doubts +
  falsifiers: GATE.md. RETRO.md written (recheck = Sid's call on cost).
- Board T11 flag corrected: block-write code landed in `ad19b96`.
- Remaining before full close: Sid's spawn(2)/(3) look + G8 block-edit
  look (steps in the board's wear list). Then prune to done-pointer.

**2026-07-13 · Fable (same session) · [SCENE-CTX] WORN ✓ · backdrop card landed — G11 nearly closed**
- SCENE-CTX end-to-end: the bundle rode Sid's real agent turn (:visible
  65 units incl. the conv root; agent ran, $0.46, answered with repo
  awareness). :vi/:address nil = the click was on the MAIN face, which
  lives OUTSIDE the store until P3c — expected shape, noted for review.
- spawn(3) still interleaved (copies overlap; cascade alone can't fix
  text-through-gaps) → backdrop card landed `82d9981`: opaque rounded
  card behind every copy, helper shared by spawn AND refresh-all-slots!
  so it survives echo rebuilds; no :address so picks can't be hijacked.
- Remaining wearing: ONE look at spawn(2)+spawn(3) after hard refresh
  (cards + cascade) · G8 (block edit + restart + forceStale, zero copies
  open for clean latency). Then gate review — top carried item: copies
  read the 1s-debounced full pull, not the fast overlay lane.

**2026-07-13 · Fable (same session) · G11 round 2: click ✓ spawn(2) ✓ echo ✓-but-slow; two mechanisms named**
- Reactor fix CONFIRMED (click works, no crash). spawn(2) in-frame ✓.
  spawn(3) stacked on spawn(2) (60px step) → fixed `7d901c8` (0.36vh
  cascade + scale floor 0.25). Copies still have NO backdrop rect →
  text interleaves with the main face where boxes gap — backdrop =
  gate-review item (rt-tree node schema needed, no blind hack).
- ECHO-TO-COPIES SECONDS — mechanism named: copies rebuild from
  @!face-context = projection ONLY; the narrow single-unit overlay
  channel merges into the MAIN face path only. So the fast echo redraws
  the edited face, then refresh-all-slots! rebuilds copies STALE, and
  fresh content arrives only with the INV-19 1s-debounced full FacePull
  (+~140ms full repack per slot). Fix-shape: slots consume the same
  overlay-merged source as the main face — a real slice, gate review.
- [SCENE-CTX] non-finding: bundle attaches AT SUBMIT (:run branch);
  Sid's console had no [AGENT][CLIENT][SUBMIT] → message never sent.
  Retest: click a block → Ctrl+K → type → ENTER; sceneContext.bundle()
  inspects without an agent run.

**2026-07-13 · Fable (same session) · G6 CLOSED · G11 round 1: reactor-killing pick bug found+fixed**
- G6 CLOSED: Sid confirms zoom 0.5 in-frame / 2 crisp / 5 sharp after the
  center-anchor fix. Speed + visual halves both green.
- ROOT BUG (Sid's first face click, zero slots): `(and (any-slots?) (pick))`
  leaked literal FALSE into record-pick! → `(assoc false …)` → **Electric
  reactor death**. Everything downstream in his report — echo lag, /face
  erroring, no [SCENE-CTX], "can't spawn again" — is the dead reactor.
  Fixed `926214a`: `when`, not `and` (record-pick! contract = map-or-nil).
- Spawn placement: x-off = n·(w+40) with real face w ≈ 3.6k px → copies
  off-screen ("spawned at x 7352, nothing happens"). Fixed same commit:
  in-frame, scaled to 45% vw, 60px cascade; move/scale adjust.
- Copy mouse-drag NOT built (only text drag-select exists) — gesture slice
  staged P5+; console move/scale is the affordance. Sid's report = expected.
- OPEN for gate review: (1) echo perf with live slots — spawn repack cost
  63ms/1 slot, 138ms/2 slots ([RAF] text-gpu); refresh-all-slots! repacks
  EVERY slot per projection change — Sid: "dog slow … maybe not using the
  editor infrastructure" (real; measure clean after reactor fix; likely
  per-slot diff needed). (2) face-mode sidebar/header text tiny — design
  note for the legibility batch. (3) [SCENE-CTX] absent — retest first,
  it likely died with the reactor. G11 retest = Sid, after hard refresh.

**2026-07-13 · Fable (same session) · G4/G6 WORN — PASS at 240Hz, 60× gate scale**
- Receipts (Sid's pastes, steady-state): 16c/9.6k glyphs → 4.2ms p50 /
  4.9 p95 / 238fps over 13.2k frames (~55s soak) · 256c/159k glyphs →
  4.1ms · 1000c/628k glyphs → 4.2ms p50, probeMs 0.3 (= the per-frame
  1000-transform write itself), bodyMs 0.1 · zoom driven to 10 → 4.1ms.
  instancePacks constant within every soak — G4's zero-instance-writes
  counter assertion held at all scales.
- Gate letter: G4 asks ≥16c / ≥10k glyphs / 60s@60fps / p95 ≤ 8ms →
  beaten 60× on glyphs at 4× the frame rate, p95 4.9. G6 asks zoom
  [0.5, 2] → speed clean to the clamp (10), but the VISUAL half found a
  probe gap: origin-anchored zoom sent every container off-frame at the
  ends (Sid's paste + screenshots). Fixed `b796d61` (center-anchored
  pan = C·(1−z)); retest = zoom(0.5)/2/5, text should magnify in place
  and stay crisp. "Unreadable at zoom 1" = by design (grid scales
  0.55–0.75). 155ms one-off at sweep start = start!/warmup transient.
- 240Hz persisted: setup-monitors.py gained a post-layout rate-upgrade
  pass (verify + retry + fallback to preferred; rehearsed live, lands
  239.99). fps receipt = recent 1000/p50 (`309a599`).
- Package remaining: sceneFaces G11 + [SCENE-CTX] wearing → gate review
  (fresh context, full-diff falsification) → retro.

**2026-07-13 · Fable (same session) · DIAGNOSIS CLOSED: SwiftShader CONFIRMED + FIXED — G4 signal now vsync-bound**
- adapter.info receipt: `isFallbackAdapter: true, type: CPU, description:
  "SwiftShader Device (Subzero)"` — the 91-106ms frames were CPU raster.
- Fix worn: chrome://flags #enable-vulkan → Enabled, relaunch. Receipt
  after: `frameMsP50 16.7 · sampleMs 0.7 · bodyMs 0.1 · probeMs 0 ·
  waitMs 15.9` — app JS ~0.8ms/frame, rest is pure 60Hz vsync wait.
  Sid: "omggg the smoothness of the renderrrr".
- 60 vs 240: xrandr shows the 4K primary AT 60.00Hz with 239.99 available
  (second monitor 1440p@180). Next wearing step: set 240Hz, expect
  frameMsP50 → ~4.2ms. `fps` receipt field fixed to recent 1000/p50
  (`309a599`) — cumulative was polluted by hidden-tab wall-clock.
- Scale ladder for G4/G6: max-containers = 1024 (renderer.cljs storage
  buffer, cid ≥ 1024 throws) → start(256) → start(1000) ≈ 600k glyphs;
  watch bodyMs (transform-write JS scales with n) + waitMs. 16k containers
  needs a constant+buffer bump — only if a real consumer wants it.
- Flag debt (Sid's chrome://flags): GPU rasterization sits Disabled
  (old manual flag; chrome://gpu lists it under Problems) → set Default.
  #force-enable-webgpu-interop NOT needed (Vulkan compositing active).

**2026-07-13 · Fable · DIAGNOSIS: prime suspect = SwiftShader (software WebGPU), verification issued**
- Sid's new receipts: p50 ~104-106ms / 9-10fps, flat at 300 vs 600 writes,
  instancePacks 1. His pre/post spine-boot A/B DISPROVES the Rama-ingest
  suspect: p50 106→103.9 (unchanged); only frameMsMax was boot-correlated
  (541→161). Testing ground confirmed correct (probe over live app = G4
  by design; receipts still zoom-1 only, G6 sweep pending).
- Smoking gun (machine-checked): Chrome runs with --enable-unsafe-webgpu
  but NO Vulkan feature → Dawn falls back to SwiftShader (CPU raster) on
  Linux. Hardware itself fine: 2× RX 7900 XTX, conformant Vulkan 1.4, GL
  accelerated. Explains flat ~100ms full-viewport redraws, quiet JS, menu
  jank, and why normal use (identical?-skip, no redraws) felt fine.
- Landed `5430829`: receipts gain sampleMs/bodyMs/probeMs/waitMs p50 split
  (rAF stamp → body entry → step! → derived GPU wait) — suspect list #1-#4
  all become one receipt. Hard refresh needed after the hot swap.
- Sid verification: `(await navigator.gpu.requestAdapter()).info` +
  chrome://gpu "WebGPU" row; fix = relaunch Chrome with Vulkan enabled,
  re-run probe. Expected on hardware: p50 low single-digit ms, fps → 240.

**2026-07-13 · Fable (session close) · FIRST WEARING: G4 FAIL signal — 91ms frames, diagnosis OPEN**
- Sid wore the probe (16 containers, orbit visible, "something cool ...
  but laggy"). Receipts (his paste, two consecutive):
  `{:fps 11 :frameMsP50 91 :frameMsP95 95.6 :frameMsMax 193 :glyphs 9580
  :instancePacks 1 :containerWrites 600}` — uniform ~91ms EVERY frame;
  right-click menu lags 1-2s (main thread saturated).
- Established: ONE dev server at OS level (his "2 shells" = a dead
  terminal, not the cause). instancePacks=1 ⇒ instance buffers NOT
  re-uploading; the 91ms is in the frame loop AROUND the transform write.
- Suspects for the next session, ordered: (1) cost in the m/latest
  SAMPLING side — invisible to [RAF] (raf-t0 starts inside the reduce
  body); (2) console flooding (which label scrolls?); (3) per-frame
  recompute driven by some ticking input; (4) accumulated hot-reload
  consumers (needs hard-refresh check). Asked Sid for: [RAF] lines ·
  scrolling-label check · ctProbe.stop() A/B · 3s Performance recording.
- Sid's new bar, verbatim: "can we make it more fps??? like 240??? my
  monitor is 240hz" — RAF follows the monitor; 240Hz budget = 4.2ms.
- DIAGNOSIS = next machine move, BEFORE gate review. Probe receipts now
  carry frameMs p50/p95/max (`9fc7329`).

**2026-07-13 (past midnight, same session) · Fable · P4 LANDED — dispatched phases COMPLETE**
- Opus subagent (~169k): context-bundle (pure cljc, §5 shape exact,
  visible ranked by screen area, cap 32 + count carried) · wire-in at the
  agent seam as `:scene-context` EDN (NO stop-clause needed — the body was
  already pr-str'd; server `parse-edn-body` tolerates extra keys by
  construction, Fable-verified at server_jetty.clj:96-100) · last-pick at
  the mouse edge, cleared via the ONE P3b lifecycle · actions router with
  trail-face's case migrated (goldens unchanged) · G9 resolve trace:
  address byte-identical to read-unit's key (file:line cited).
- Fable re-verified: 68t/888a green across scene-store + block-edit +
  trail-face; build clean. Committed `8199322`.
- Package state: P1–P4 ALL landed + committed; wave-1/wave-2
  falsifications PASS; staged: P3c (main-face flip) · second assembly
  artery · P5+ (incl. gesture slice w/ zoom-pick gate).
- REMAINING to close: Sid's wearing (ctProbe G4/G6 + sceneFaces G11 +
  [SCENE-CTX]) → package gate review (fresh context, full-diff
  falsification protocol) → retro. Board carries the pointers.

**2026-07-12 · Fable (same session) · P3b LANDED + wave-2 findings closed**
- Opus subagent (~218k): rung 1 (per-vi builds — N different faces over
  one live conversation via capture-at-spawn; despawn + close-all +
  stale-drop: orphans impossible by construction) + rung 2 (G8: per-slot
  cloned text geos, keystroke reshapes ONLY its slot, msdf+slug through
  the one path — T12 held). Rung 3 (main-face flip) STOP-CLAUSED on two
  real walls → staged as P3c in CONTRACT (+ the second assembly artery).
- Wave-2 findings #1/#2/#3 all closed in-slice (routed mid-flight via
  SendMessage — new mechanism, worked well).
- Fable re-verified: 35t/172a green, build clean. Judgment calls all
  ACCEPTED (capture-at-spawn · upsert-baked container-idx · stop-clause).
- Committed: `20ee578` (P1+P2) · `a05d6ca` (P3a) · `44cbad6` (P3b) —
  code-only commits per Sid's "keep committing".
- Wearing: wear face A → `sceneFaces.spawn(1)` → wear face B (same conv)
  → `sceneFaces.spawn(3)` → edit a block → both echo, each through its
  own face; `.close(1)` despawns; `/face off` clears all.
- Next: P4 (pick/context bundle + actions router — Fable-direct candidate)
  · P3c when pulled · gate review + G11 wearing before package close.

**2026-07-12 · Fable (same session) · P3a LANDED — plurality is real**
- Opus subagent (~245k tok): `scene_runtime.cljs` (store+registry atoms,
  edge mutations, `<store-frame`/`<effective` flows, `window.sceneFaces`)
  + `stamp-block-addresses` (cljc) + render/mouse wiring. Suites re-run by
  Fable: 30t/150a green (scene-store 19/76 + block-edit 11/74, no
  regression); build green, zero P3 warnings.
- Adjudicated + ACCEPTED two divergences: (1) slots rebuild from the
  singleton `!face-scene` at the consumer edge — the literal "generalize
  the build" would put a side effect in m/latest (T4); the wall was
  reported, not patched. (2) MAIN face stays legacy/world-anchored;
  spawned copies are store-managed/draggable. → P3b: true per-vi builds +
  main-face migration + per-slot text geos (G8).
- Wearing (G7 browser half, Sid): wear a face → console `sceneFaces.spawn(2)`
  → `.move(2,1400,120)` / `.scale(2,1.3)` → edit a block in either → echo
  lands in BOTH. Verdict goes here.
- Next machine move: wave-2 falsifier (ONE finder, capped ~60–80k, per the
  wave-1 rule) after Sid's wear; then P3b/P4.

**2026-07-12 · Fable (same session, close) · wave 1 FALSIFIED — PASS + fixes in**
- ONE finder (per machine-cut retro rule; Opus, ~188k tok): VERDICT PASS,
  0 HIGH/MED, 5 LOW. Record: `FALSIFY.md`. Risk center (stride math ×4
  pipelines + pool packers) traced EXACTLY consistent; G5 back-compat
  byte-identical at zoom 1.0.
- Fixed same session w/ regressions: #1 nested-same-address write order
  (deepest-first) · #2 probe step! try/catch self-deactivates · #4
  store-fns-free? walks metadata. #3 = CONTRACT G4 read-plan amended in
  place. #5 (unregistered :container silent) carried to P3 as a gate.
- Suite 17t/59a green (was 15t/54a); compile green at final state (one
  intermediate paren failure between two edits, self-resolved).
- Wave subagent cost, honest ledger: P1 ~134k + finder ~188k ≈ **322k**
  vs the ~150–200k stated up front — overrun flagged to Sid.
- Waits on Sid: ctProbe wearing (G4/G6, one-liner/URL on the board).
  Next machine move: P3 (face path → store; fresh context under §11).

**2026-07-12 · Fable (same session, later) · P1 LANDED + P2 code complete**
- P1 (Opus subagent, ~134k tok): `scene_store.cljc` (217 ln) +
  `containers.cljc` (119 ln) + suite — 15t/54a green; re-run independently
  by Fable, green. G1–G3 covered verbatim. Three judgment calls flagged
  (root `[]` path special-case · pick = first ADDRESSED hit · pr-str
  tie-break) — all read + accepted by Fable; re-resolve idempotency noted
  for the P3 reviewer.
- P2 (Fable direct): all four pipelines carry `container_idx` u32
  (strides 116/84/52/100) + shared `containers` uniform (vec4×1024,
  binding 1 rect/shadow, 4 text) + screen-flag camera select; packers
  thread `:container-idx` (default 0 = identity, T6); `write-containers!`
  consumes `containers/effective` output as-is; draw-frame! `:zoom` kwarg
  wakes the dormant camera (default 1.0 untouched). Probe
  `container_probe.cljs` + 4 tagged mount lines in `runtime/render.cljs`
  (islands-probe pattern): `ctProbe.start(16)` → 16 containers ×20 lines,
  one orbits, per-frame writes = transforms only; `.zoom(z)` drives G6.
- Dev app was NOT running (machine rebooted ~20:45) — restarted via
  `clj -A:dev -X dev/-main`; compile GREEN (watch caught 2 arity misses at
  the font-swap bind-group sites — fixed; remaining 7 warnings are
  island-probe's pre-existing infer-warnings).
- Headless G4 attempt FAILED at the environment, not the code: headless
  Chrome on this box cannot create the WebGPU device at all ("Failed to
  initialize vulkan surface") — extends the framework-retro GPU-capture
  finding; SwiftShader would measure CPU raster (meaningless for the
  gate). G4/G6 receipts therefore come from Sid's HEADED browser:
  `ctProbe.start(16)` or just open `localhost:8080/?ct-probe=16` —
  [CT-PROBE] logs print fps/writes receipts every 300 frames. Do NOT
  re-burn time on headless WebGPU here.
- G5's honest form on this box (no GPU capture): container-0 identity is
  mathematically exact in-shader (0 + x·1 = x, IEEE), finder-traced, plus
  Sid's eyes at the live app.
- Next: finder verdict → fixes → wave-gate record → P3 (fresh context).

**2026-07-12 · Fable · derivation + contract + P1 dispatch/P2 start**
- Derived the base layer from first principles against the record:
  `DERIVATION.md` — five unlocks collapse onto four capabilities, one
  organ (the scene substrate); server floor already exists; test table
  passes with two genuinely-later items, both Sid-gated.
- Ruled into settled ground (decisions.md "One render substrate" entry
  amended in place; supersedes separate container-transforms /
  point-and-say / scene-diff packages — folded as legs/consumers).
- CONTRACT v1 written, in force. Board flipped (FOREST → base line).
- P1 (store + containers, pure cljc + JVM tests) dispatched to a
  fresh-context Opus subagent under contract. P2 (GPU transform leg)
  started by Fable directly per §11.
- Next: land P1+P2 → ONE falsification finder over the wave → P3.
