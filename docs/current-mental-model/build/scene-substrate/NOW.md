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
