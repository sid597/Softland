# scene-substrate — package gate review (Fable, 2026-07-13)

**VERDICT: PASS** (finder: PASS-with-fixes; all three fix-now findings
landed + compile-verified this session). The floor — store, container
transforms, stride math across all four pipelines, pick, lifecycle — traced
clean by a fresh-context finder over the FULL package diff. Every real
defect was bounded to the spawned-copies dev affordance or latent; none
contradicted an adjudicated ruling.

## Inputs

- Diff: `git diff 20ee578^..HEAD -- src/ test/` — 2,450 insertions, 11
  files (all eleven scene-substrate code commits `20ee578`→`82d9981`,
  including the wearing-round fixes that had no prior falsifier).
- Suites re-run BY THE GATE SESSION (never Phase-N's word): scene-store +
  block-edit + trail-face = **68t/888a green** at committed HEAD.
- Finder: ONE fresh-context subagent (Opus 4.8, 192k tok — inside the
  one-finder budget rule), default-fail, full falsification protocol.
- Fable's independent spot-checks: scene_runtime.cljs full read (edge-only
  mutations, single m/latest per source, wave-1 finding-#5 assert present);
  the echo trigger path traced first-hand at render.cljs:214-235 BEFORE the
  finder reported — both arrived at the same mechanism independently.

## Findings → dispositions

1. **F1 · MED · echo-to-copies reads the slow lane AND full-repacks every
   copy per keystroke** (render.cljs echo trigger; the package's top
   carried item — CONFIRMED, and worse than the wearing log stated). The
   trigger was `face-scene-changed?`, which flips on every keystroke/caret
   move (its m/latest struct includes overlay-merged edit state), while
   copies rebuild from `@!face-context` (changes only on the debounced
   FacePull). So every open copy repacked per keystroke for ZERO visual
   change (63ms/1 slot, 138ms/2 — Sid's "dog slow"), and the echo itself
   waited on the 1s debounce. **Split ruling:** (a) trigger re-keyed to
   face-context identity — FIXED at gate (content behavior byte-identical,
   per-keystroke repack gone; render.cljs). (b) the LANE unification
   (copies consume the overlay-merged source the main face reads) is
   literally P3c's wall #1 (threading edit-state + truth-overlay into the
   store build) → folded into P3c's scope with the mechanism now precisely
   named. Fixing it standalone then re-doing it at the main-face flip
   would be double work. NOT a G7 letter-failure: the echo lands in both,
   correct but slow — copies never show wrong content, only old content.
2. **F2 · MED · context-bundle build unguarded on the agent-submit path**
   (agent_flow.cljs). Currently unreachable-throw (context-bundle traced
   total over well-formed stores), but a throw there would kill a
   user-visible agent run. **FIXED at gate:** try/catch → warn + submit
   bundle-less.
3. **F3 · LOW · cids never recycle** — ~992 spawn/despawn cycles walk into
   the 1024-container ceiling and the write-containers! throw lands OUTSIDE
   the draw try/catch → reactor death from a dev affordance. **FIXED at
   gate:** freed cids return to a pool (scene_runtime.cljs `alloc-cid!`/
   `free-cid!`); counter grows only when the pool is empty.
4. **F4 · LOW · ct-probe and sceneFaces share the containers buffer** —
   `start(1000)` overlaps face cids 32+; each write-containers! clobbers
   the other's range. Two delete-to-remove dev tools colliding → RESIDUE
   (recorded; both probes are staged for deletion, no product path).
5. **F5 · NOTE · backdrop-card pool concern REFUTED by trace** — nil-`:id`
   cards in the ordered pool write by position (`:id` is only a skip
   optimization); no corruption/ghosting; the card carries no `:address`
   anywhere pick sees, cannot hijack, falls through correctly; survives
   spawn/refresh/despawn/close-all.
6. **F6 · NOTE · double resolve-layout on face trees** (build-face-tree
   output re-resolved inside build-slot; predates the backdrop) → OPEN
   DOUBT, falsifier: REPL `(= t (resolve-layout t))` on a real
   apply-assembly output, or screenshot-diff copy vs main at scale 1.0.
7. **F7 · NOTE · G9's executable half asserts store-index resolution;
   server read-API byte-identity lives in the P4 baton (file:line-verified
   at adjudication), not in a test** → OPEN DOUBT, falsifier: one test
   asserting bundle `:address` == server read-unit key byte-for-byte.
8. **F8 · NOTE · `update-nodes-by-address` (the Δ1 surgical fan-out
   primitive) has zero runtime callers** — the live echo is a full rebuild;
   per-vi different faces preclude a pure address-patch, but a per-slot
   diff could restore the identity skip → feeds the P3c/perf slice.
9. **F9 · NOTE · the echo trigger rides editor_compute's pre-existing
   reset!-inside-m/latest cache pattern** (not scene code; recorded as
   coupling, no new diamond).

## Falsification-protocol record (finder + Fable, merged)

- **Architecture**: pure store (vi→slot + address→#{vi} index) / container
  tree → effective transforms / GPU leg (container_idx u32 in all four
  pipelines, 16B uniform select, container 0 = identity) / runtime edge
  (one store atom + one registry atom, mutations at edges only) / P4 seam
  (pick → last-pick → context bundle EDN on agent turns; descriptor
  router).
- **Traps T1–T12**: all clean or n/a except T5, which is violated at echo
  granularity only (the F1 repack — steady-state identity holds; gate fix
  removes the keystroke case; the remaining per-echo rebuild is the F8/P3c
  item). T12 re-verified deep: slot geos are clone-text-system sharing
  pipeline/bind-group/font, owning only their instance buffer; destroy on
  a clone frees only its own buffer; font-change reclone forecloses
  stale-clone use-after-free.
- **Writers/readers/clearers**: single-writer edges throughout; !last-pick
  cleared ONLY via close-all-slots! (the one P3b lifecycle — bounded
  because record-pick! fires only in face mode).
- **Async ordering**: spawn-vs-echo (atom, last-swap-wins, no torn read);
  mode-exit rects linger ≤1 frame then self-heal via store-frame-changed?;
  refresh's stale-close-then-rebuild captures !vi-faces once, no lost
  update.
- **Error paths**: spawn re-spawns clean (close-first); refresh total
  (apply-assembly renders error cards, never throws); probe step!
  self-deactivates (wave-1 fix held); bundle now guarded (F2 fix);
  write-containers! overflow foreclosed by cid recycling (F3 fix).
- **Byte-level re-verification**: strides 116/84/52/100, offsets
  112/80/48/96, container-0 identity IEEE-exact, WGSL 16KB uniform under
  the 64KB floor, bindings consistent across all four pipelines + both
  pool packers.

## Open doubts (non-blocking, falsifiers named)

- resolve-layout idempotency on double-resolved trees (F6, above).
- G9 server-read byte-identity as a test, not a baton citation (F7, above).
- context-bundle totality over adversarial stores (fuzz scale-0 /
  empty-bounds / nil-child slots) — would downgrade F2's residual risk to
  nil (the guard now bounds it regardless).

## Evaluation notes (D-006 style)

- The traps ledger earned its keep again: T4/T5/T8/T12 were each the exact
  frame the finder used to classify findings — F1 is precisely "T5 at echo
  granularity," a category the ledger made nameable.
- The wearing layer (G11 rounds) found everything user-visible BEFORE the
  gate (reactor-killing pick bug, off-screen spawn, interleaving); the gate
  finder's unique catches were the latent items wearing can't reach
  (keystroke-repack trigger, cid overflow, unguarded submit garnish) — the
  two layers caught DISJOINT defect classes, which is the QC model working
  as designed.
- One-finder-per-wave held: 192k tok, three fix-now findings, zero noise
  findings. The wave-1 (188k) and gate (192k) finders cost the same and
  each paid for itself.

## G-ledger at gate

G1–G3 ✓ (JVM, re-run) · G4 ✓ (worn: 240Hz, 628k glyphs, 4.2ms p50 — 60×
gate scale) · G5 ✓ (byte-identical, finder-retraced) · G6 ✓ (worn after
center-anchor fix; crisp at 0.5/2/5) · G7 ✓ (worn; echo latency ruled
above) · G8 ✓ (per-slot isolation, T12 re-verified; Sid's block-edit
wearing look still listed under G11 residual) · G9 ✓ (with F7 doubt
recorded) · G10 ✓ (JVM, re-run) · G11 rounds 1–3 worn; residual = one
spawn(2)/(3) look after hard refresh + the G8 block-edit look (Sid).
