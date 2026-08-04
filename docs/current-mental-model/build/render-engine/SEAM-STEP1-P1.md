# SEAM-STEP1 P1 — implementer phase artifact

**Session:** 2026-08-03 · Codex implementer  
**Working tree:** `/mnt/data/projects/Softland`  
**Branch / opening HEAD:** `docs/current-mental-model-local` / `841a250e2adb302de8e83b8b87c40b6e8a87731c`  
**Package result:** Acts 1→2→3 implemented in order; every Codex-owned in-phase gate is green under its contract-defined condition. No §14 stop fired. No commit or push was made. This is implementer evidence, not Fable FULL gate closure, Sid's commit ruling, or product approval.

## What was built

### Act 1 — maintained views replace per-frame order derivation

- `scene_tape.cljc` now owns the shared ordered-view kit. The existing scalar, sequence, order, and entry-validation functions are public; `entry-key-compare` calls `compare-order` with entry-shaped wrappers and breaks remaining ties through `compare-seq` / `compare-scalar` rather than Clojure's length-first vector comparator (T1). `ordered-insert` adds insert-edge validation while `compile-tape` retains its independent validation (T6).
- `scene_store.cljc` now carries `:ordered`, keyed by `[order-token entry-id]`. All public store writes patch it alongside `:slots` and `:index`; `rebuild-ordered` is the one EDN-rehydration door (T2/T3). `maintained-entries` is a direct sorted-map walk, independent of both `scene-tape` batch arities.
- Store paint and pick now consume the same stamped maintained order. Pick still delegates filtering/reverse traversal to `pick-reverse` and still looks up current effective transforms per entry. Untouched entries retain the exact slot and ops-array identities through the maintained walk (T10). `<store-frame` preserves its prior public keys and drops only the unconsumed `:scene-tape` key.
- The R1 casualty was repaired exactly as pinned: only the two overlapping upserts in `pick-layer-and-miss` gained the effective registry `:stack-path`; its assertions were not changed.
- `renderer.cljs` owns a frame-local maintained arrangement using the same key/comparator vocabulary (T8). Producers still run per frame. Existing semantic keys cheaply rebind payloads, changed/absent semantic `[entry-id order]` pairs remove/reinsert, and `frame-idx` has no ancestry into arrangement order (T12).
- `compile-frame-tape` remains executable behind `frame-tape-twin-check!`. When `globalThis.__softland_frame_tape_twin_check` is true, every frame independently compiles the batch oracle, compares the full entry sequence, records a receipt, logs divergence, and throws on mismatch (T6).

### Act 2 — stable editor chains, proven sharing, and keyed text lookup

- G4's four Missionary claims were added and passed before production unification: one upstream process is shared by N subscribers, a late subscriber receives the latest value without restart, zero subscribers stop the upstream and resubscription restarts it, and a side-effecting raw `m/latest` changes lifecycle/count when wrapped by `m/signal` (T5).
- After that proof, only the pure `<layout` sharing point was wrapped in `m/signal`. `<sidebar` and `<trail-face>` remain deliberately unsignaled because their combines perform effects and G4 proved the lifecycle difference. Further forks were not chased in this BEGUN slice.
- Run content and editor content are now stable→overlay chains rather than diamonds (T4/L8). Stable inputs exclude `!caret-visible` and `!shimmer-phase`; stable computation retains caret geometry and precomputes dim/bright paint variants. Overlay stages only select shimmer paint and toggle caret presence. The adopted dormant-mode guard sits before proportional editor layout in the stable stage.
- The G5L probe increments `globalThis.__softland_seam_stable_recomputes` only while `globalThis.__softland_seam_probe_stable` is true, allowing Sid to prove that a visibly blinking caret causes zero stable recomputes over ten seconds.
- Renderer text positioning now memoizes a `:line/id → line` index by layout-result object identity in a `WeakMap`. A present `[:source :revision]` is asserted numeric and logged as a receipt, never used as the cache key; inline unstamped layouts retain the existing fallback.

### Act 3 — camera quarantine and camera-aware picking

- `ground/!camera` left `<world-snapshot>` completely. The RAF sink dereferences it locally, compares it to its own previous camera, and redraws when the world changed, the camera moved, or a dirty rect is pending. Camera moves retain the full-clear rule. The relocated `first-light P2b` comment records the ownership move.
- `ss/pick` accepts either the old bare vector (the point in both spaces) or `{:world … :screen …}`. Each entry selects screen coordinates exactly when its current effective transform has `:flags 1`; world entries remain world-coordinate picks (T9).
- Both cursor sites now supply both spaces: `runtime/mouse.cljs` carries its raw pointer y alongside scene y, and the one permitted `ground.cljs` scalpel changes only `pick-at`. World-only material-context bundle callers remain backward-compatible. The direct-caller enumeration found no additional production caller (T9/T11).

## First-run contract signals

G1/G2's first dedicated run did not go green. The captured failure was:

```text
Testing app.client.substrate.maintained-view-test

FAIL in (g1-maintained-store-equivalence-across-public-writes) (maintained_view_test.clj:30)
every public write surface keeps the maintained view fenced
maintained and oracle pick agree at [0 0]
expected: (= (select-keys (ss/pick store effective point) [:vi]) (oracle-pick store effective point))
  actual: (not (= {} nil))
```

The same finding occurred twice more at probe point `[250 50]`. The runner ended verbatim with `Ran 40 tests containing 256 assertions.` and `3 failures, 0 errors.` Production maintained and oracle sequences were already equal; the test projected `nil` with `select-keys`, producing `{}`. The only correction was making the test projection nil-preserving with `some->`. The rerun was `40 tests`, `256 assertions`, `0 failures`, `0 errors`.

G8's first `npm run verify:render-engine` invocation also exited 1 and printed:

```json
{
  "pass": false,
  "classification": "candidate-pick-parity-failure",
  "receipt": "target/render-verifier/receipt.json",
  "images": 21,
  "deterministic": "21/21",
  "q8AffineTransport": true,
  "q5AffineRasterBoundary": true,
  "candidateParity": "14/21",
  "productBoundsDivergenceSentinels": "7/7",
  "updateRequested": false,
  "updateAuthorized": true,
  "environmentFingerprint": "9213da9a74738c3397e99ef62b08c3833cbcaab9b6124d8255c1a4616e58a03c"
}
```

That process-level RED is the required MSDF counterexample, not a golden regression: all 21 determinism rows and all 21 golden rows pass; Q8 transport and Q5 raster boundary pass; the 14 non-MSDF candidate rows pass; all seven MSDF rows remain RED at exactly 47 mismatches; and all seven current-product divergence sentinels pass. An explicit close-condition query returned `true`. Chrome was available at `/usr/bin/google-chrome`, so G8 ownership did not flip. The verifier used Puppeteer 15.2.0, Chrome 150.0.7871.46, and SwiftShader (`vendor=google`, `architecture=swiftshader`). No golden or manifest update was requested or made.

## Gate receipts

| Gate | Implementer receipt | Result |
|---|---|---|
| G1 | Maintained store vs independent one-arg batch oracle after every public write; pick parity probes; insert validation; EDN rebuild; fn-free store; stamped and stampless R1 probes. Rerun: 40 tests / 256 assertions in the combined slice. | GREEN after the first-run test-oracle correction recorded above. |
| G2 | Synthetic maintained frame arrangement covers enter, exit, order-token change, payload rebind, and visibility toggle against `compile-tape`. | GREEN in the same JVM slice. |
| G3 | `verify_scene_tape_fence.mjs` and `verify_text_layout_fence.mjs`; pins moved, all forbids and seeded negatives retained. | GREEN on first run and final rerun. |
| G4 | Missionary b.46 sharing/latest/zero-subscriber/side-effect lifecycle claims: 12 tests / 47 assertions in the claims namespace. | GREEN on first run, before the production `m/signal` edit. |
| G5s | Text fence proves the stable editor region contains neither ticker atom; dormant guard remains early; seeded ticker and late-guard negatives are rejected. | GREEN. G5L remains Sid-owned. |
| G6s | Scene fence proves there is no `(m/watch ground/!camera)` and the RAF sink contains `@ground/!camera`. | GREEN. G6L remains Sid-owned. |
| G7 | Screen container and unchanged world-container semantics under the non-identity camera point pair. Post-Act-3 slice: 41 tests / 260 assertions. | GREEN on first run. |
| G8 | 21/21 determinism; 21/21 byte-identical goldens; Q8 and Q5 true; seven MSDF 47-mismatch cases remain RED; 7/7 product divergence sentinels. | GREEN by the contract's explicit dual close condition; command exit signal recorded above. |
| G9 | Full `clj -X:test`: 56 namespaces, 527 tests, 7,443 assertions/pass, 0 fail, 0 error; every registered flake passed on attempt 1. | GREEN on first run. |

Additional runnable-state evidence after the last source comment adjustment: focused maintained-view + scene-store + Missionary run `48 tests`, `234 assertions`, `0 failures`, `0 errors`; both fences green; dev compile `286 files`, `16 compiled`, `0 warnings`. The compile also emitted the pre-existing `reader-conditional?` namespace replacement warning from the foreign Rama path; it is not a CLJS build warning and this package did not touch it.

## Judgment calls and bounded flags

- R1's stamped paint order is the maintained truth. The two-arity refreshed batch form remains available only to explicit-transform callers/tests; it is not allowed back into live pick.
- G4 proved that `m/signal` changes side-effect count and lifecycle. Therefore the pure layout fan-out was unified, while `<sidebar>` and `<trail-face>` remain raw and are explicitly flagged for a later deliberate ownership slice.
- `bundle-for-viewport` / `bundle-at-world-point` continue through the backward-compatible world-only form because they resolve world-space material context rather than cursor furniture. If that product meaning changes, it is a later decision, not an unreviewed expansion here.
- The next identical keyed-rescan surface is still `src/app/client/workspace/text_layout.cljc:669-670` (`clip-result`). It is named-deferred and untouched.
- Camera-following furniture migration remains out of scope and untouched: material error card (`ground.cljs:1093`, update/register `1104-1115`), Studio birth handle (`ground.cljs:2298-2309`, inside the foreign hunk), binding-lint card (`ground.cljs:3532-3583`), and Workshop door (`workshop_playground.cljs:1128-1150`, camera watch `1416-1417`). Their comments still describe the now-removable workaround; changing those foreign sites belongs to the later migration.
- No new direct `ss/pick` caller was found. The direct production doors remain `scene_runtime/pick-world` and `scene_store/context-bundle`; only the two named cursor paths thread the map form.

## Sid-owned live handoff

These receipts are not claimed by this implementer phase:

1. **G5L:** after idle editor settlement, run `globalThis.__softland_seam_probe_stable = true; globalThis.__softland_seam_stable_recomputes = 0;`, leave the caret visibly blinking for at least 10 seconds, then read the counter. Expected: `0`. Turn the probe flag off afterward.
2. **G6L:** the receipt's first field must state `isFallbackAdapter` plus adapter description. Prove pan still redraws and record before/after pan measurement. The pre-package side comes from a read-only worktree at the opening HEAD, or Sid explicitly waives it using the known baseline; name which.
3. **G10 twin:** initialize `globalThis.__softland_frame_tape_twin_receipt = {frames: 0, divergences: 0}; globalThis.__softland_frame_tape_twin_check = true;`, wear the real ground for at least one minute, disable the flag, and capture the receipt. Required: zero logged/recorded divergences. This line is mandatory and non-inheritable.
4. **G10 wearing:** hand-feel pan, wheel, and inspector on the real ground. Then Fable performs the independent FULL-tier gate and Sid makes the commit decision.

## Prediction check — two-plus-transport

The plumbing dialects did begin converging toward two-plus-transport. Store arrangement and renderer frame arrangement now share one canonical `[order-token entry-id] → entry` maintained-view vocabulary, one comparator, and one validation kit; the editor now expresses document-derived truth as stable shared computation followed by a small overlay projection; camera motion is quarantined as sink-local transport/mosaic input rather than document/world derivation. The prediction is only partially confirmed: the renderer's line index remains consumer-owned, dirty mosaic policy remains at the sink, the transport layout itself was intentionally untouched, and the side-effecting editor forks remain un-unified. This is convergence evidence, not a claim that the dialect reduction is finished.

## Diff-derived changed-file custody snapshot

The snapshot below was taken after this artifact and the NOW append both existed on disk. `git diff --name-only` reported the tracked portion:

```text
docs/current-mental-model/build/render-engine/SEAM-STEP1-NOW.md
docs/current-mental-model/build/studio/PLAYGROUND.md
src/app/client/substrate/scene_tape.cljc
src/app/client/substrate/webgpu/renderer.cljs
src/app/client/workspace/editor_compute.cljs
src/app/client/workspace/ground.cljs
src/app/client/workspace/runtime/mouse.cljs
src/app/client/workspace/runtime/render.cljs
src/app/client/workspace/scene_runtime.cljs
src/app/client/workspace/scene_store.cljc
src/app/server/rama/object_container/facet_master.clj
src/app/server_jetty.clj
test/app/anatomy_test.clj
test/app/client/workspace/scene_store_test.clj
test/app/material_truth_test.clj
test/app/missionary_claims_test.clj
test/app/test_runner.clj
test/render_engine/verify_scene_tape_fence.mjs
test/render_engine/verify_text_layout_fence.mjs
```

`git status --short` added the untracked portion and was:

```text
 M docs/current-mental-model/build/render-engine/SEAM-STEP1-NOW.md
 M docs/current-mental-model/build/studio/PLAYGROUND.md
 M src/app/client/substrate/scene_tape.cljc
 M src/app/client/substrate/webgpu/renderer.cljs
 M src/app/client/workspace/editor_compute.cljs
 M src/app/client/workspace/ground.cljs
 M src/app/client/workspace/runtime/mouse.cljs
 M src/app/client/workspace/runtime/render.cljs
 M src/app/client/workspace/scene_runtime.cljs
 M src/app/client/workspace/scene_store.cljc
 M src/app/server/rama/object_container/facet_master.clj
 M src/app/server_jetty.clj
 M test/app/anatomy_test.clj
 M test/app/client/workspace/scene_store_test.clj
 M test/app/material_truth_test.clj
 M test/app/missionary_claims_test.clj
 M test/app/test_runner.clj
 M test/render_engine/verify_scene_tape_fence.mjs
 M test/render_engine/verify_text_layout_fence.mjs
?? docs/current-mental-model/build/render-engine/SEAM-STEP1-P1.md
?? docs/current-mental-model/build/smalltalk-ui-vm/PODCAST_SCRIPT.md
?? src/app/client/substrate/image_material.cljc
?? src/app/client/workspace/studio.cljc
?? src/app/client/workspace/workshop_playground.cljs
?? src/app/shared/studio.cljc
?? src/app/shared/workshop_playground.cljc
?? test/app/client/substrate/maintained_view_test.clj
```

### Classification

**Allowlist — modified (13/13 §12 modified paths present):**

1. `src/app/client/substrate/scene_tape.cljc`
2. `src/app/client/workspace/scene_store.cljc`
3. `src/app/client/workspace/scene_runtime.cljs`
4. `src/app/client/substrate/webgpu/renderer.cljs`
5. `src/app/client/workspace/runtime/render.cljs`
6. `src/app/client/workspace/runtime/mouse.cljs`
7. `src/app/client/workspace/editor_compute.cljs` — §3 foreign hunk adopted; package work added around it.
8. `src/app/client/workspace/ground.cljs` — §3 scalpel: the package's only hunk is `pick-at`; all other hunks are foreign.
9. `test/render_engine/verify_scene_tape_fence.mjs`
10. `test/render_engine/verify_text_layout_fence.mjs` — §3 foreign hunk adopted; pins and seeded tests moved forward.
11. `test/app/missionary_claims_test.clj`
12. `test/app/test_runner.clj` — one pure-tier entry only.
13. `test/app/client/workspace/scene_store_test.clj` — exactly the §4a two-upsert re-stamp; assertions untouched.

**New-per-contract / mandated close output (3):**

1. `test/app/client/substrate/maintained_view_test.clj` — §12 new test.
2. `docs/current-mental-model/build/render-engine/SEAM-STEP1-P1.md` — §12 phase artifact.
3. `docs/current-mental-model/build/render-engine/SEAM-STEP1-NOW.md` — existing file appended by the mandatory §10/direct-prompt close instruction; the appended entry is 13 physical lines, within the ≤15-line limit.

**DRIFT-flagged foreign custody — present before the package and not edited by this implementer (11):**

1. `docs/current-mental-model/build/studio/PLAYGROUND.md` — tracked modified.
2. `src/app/server/rama/object_container/facet_master.clj` — tracked modified.
3. `src/app/server_jetty.clj` — tracked modified.
4. `test/app/anatomy_test.clj` — tracked modified.
5. `test/app/material_truth_test.clj` — tracked modified.
6. `docs/current-mental-model/build/smalltalk-ui-vm/PODCAST_SCRIPT.md` — untracked.
7. `src/app/client/substrate/image_material.cljc` — untracked.
8. `src/app/client/workspace/studio.cljc` — untracked.
9. `src/app/client/workspace/workshop_playground.cljs` — untracked.
10. `src/app/shared/studio.cljc` — untracked.
11. `src/app/shared/workshop_playground.cljc` — untracked.

**Sum-check against §12:** 13/13 listed modified surfaces + 2/2 listed new surfaces = all 15 §12 implementation/artifact paths, with no extra implementation path. The mandatory NOW close-ledger append adds one explicitly required §10 output. Those 16 package/close paths + 11 pre-existing DRIFT paths = all 27 `git status --short` paths. `git diff --name-only` contains 14 tracked package/close paths + 5 tracked DRIFT paths = all 19 tracked paths; status additionally contains 2 untracked package paths + 6 untracked DRIFT paths = 27 total. No path is unclassified.

## 2026-08-04 — focused G5L repair continuation

**Decision served:** determine by runtime value whether the reported stable
caret was a dead ticker, wrong focus/caret state, or a missing overlay redraw;
repair only if the failure was in SEAM-STEP1's implementation. No source was
changed before the receipt.

### Runtime-value probe and measured ruling

A controlled headed Chrome client on the live local app sampled the raw
`!caret-visible` atom, `!focus`, the editor GPU pool's keyed rects, and pool
changes 50 ms after each visibility transition.

- On the reported ground/face input surface, the raw value alternated through
  11 sampled transitions over 5.30 seconds and focus stayed `:ground-input`.
  Nevertheless `:ground-caret` remained present at alpha `1`, with identical
  `{x 286.52, y 756, w 2, h 27}` geometry, and the GPU pool recorded zero
  changes. Observed ruling: the ticker and focus state are healthy; that
  surface does not propagate the visibility phase to the caret projection.
- The same client was temporarily switched, client-locally and reversibly, to
  the standalone editor: the SEAM Act-2-owned path. From performance timestamp
  `504633.7` to `533870.3` (29.24 seconds), focus stayed `:editor`; 29 sampled
  transitions produced 15 visible samples with keyed rect `:caret` at alpha
  `1` and 14 hidden samples with no `:caret`; the GPU pool change counter
  advanced once per phase. `globalThis.__softland_seam_stable_recomputes`
  remained exactly `0`. This passes G5L's visibly-blinking-for-at-least-10s
  requirement with no stable-stage recompute.
- The client-local face state was restored and the temporary REPL watches and
  controlled Chrome client were removed after capture.

The ground/face caret is produced through `block-caret-prim` / the assembly
slot path (`face_primitives.cljc`), while SEAM Act 2 changed the standalone /
file editor stable-to-overlay chain in `editor_compute.cljs`; SEAM's only
`ground.cljs` change is the contract-pinned `pick-at` scalpel. No SEAM edit
owns the failing ground-caret propagation. Therefore the reported observation
is preserved as real but non-SEAM-attributed, and no code repair was made.

### Remaining live receipts and queued observations

- G10 twin receipt supplied live: `{"frames":569,"divergences":0,"lastFrame":1877}` — PASS.
- Pan, wheel, and inspector were supplied as functioning normally.
- Supplementary headed-client adapter attestation: `vendor=amd`,
  `architecture=rdna-3`, `GPUAdapterInfo.isFallbackAdapter=false` (the current
  Chrome API exposes the fallback field on `adapter.info`).
- The separate shaping samples (`text-gpu 2539.0ms / TOTAL 2563.5ms` and
  `text-gpu 520.8ms / TOTAL 553.1ms` immediately after one reshaped slot)
  remain evidence for shaping-correction. No shaping code was opened or
  changed here.
- Square-box rendering for some Unicode/table/icon characters remains queued
  with cause and SEAM attribution unknown. The cheapest attribution baseline
  is an A/B screenshot of the exact same code points/block at the same font,
  backend, zoom, camera, Chrome, and AMD adapter in (a) a read-only worktree at
  SEAM P1's opening HEAD `841a250e2adb302de8e83b8b87c40b6e8a87731c`
  and (b) this tree. Boxes in both baselines refute SEAM introduction; absent
  before and present now earns a narrower seam toggle. No glyph fix was made.

### Fable bounded-review handoff

Fable reviews at most three decision-changing points: (1) accept the measured
G5L PASS for the contract's idle-editor/Act-2 surface without widening SEAM to
the separately failing ground/face caret; (2) verify that this continuation
added no source/test hunk and that the taken-path receipt supports that scope
ruling; (3) carry the ground-caret and missing-glyph observations as explicit
residue while using the supplied G10 twin/wearing and adapter attestation in
the FULL gate. This is still not Sid's commit ruling or product approval.

## 2026-08-05 — FULL-gate residual receipt closure

**Decision served:** close only the two live receipt-form gaps left by Fable's
one bounded FULL-tier review: G6L's one-capture adapter-first A/B pan receipt
and G10's wall-clock-bounded twin line. The review's code-surface PASS is not
reopened. No shaping-correction or glyph work was opened, and no source/test
hunk was added.

### G6L — PASS

The receipt's first field was the adapter attestation from the same headed
capture as the measurement:

```text
adapter={isFallbackAdapter:false, description:"Radeon RX 7900 XTX",
         vendor:"amd", architecture:"rdna-3"}
```

Capture conditions: Chrome 150 at 1280×800, one real 174-block ground, zoom
`1`, actual `GPUCanvasContext.getCurrentTexture` calls as the redraw counter,
and the same three trusted-pointer samples (`dx=180`, `dy=90`, three steps,
100 ms cadence). The before side was the contract-named detached read-only
worktree at `841a250e2adb302de8e83b8b87c40b6e8a87731c`; generated build files
stayed inside that temporary worktree, which was removed after capture.

- **Before / opening HEAD:** camera `0,0,1 → 180,90,1`; two actual GPU-canvas
  draws; pan duration `6796.5 ms`; first draw `5111.4 ms`; pointer dispatch
  wall times `[1,1607,1572] ms`; four long tasks of `1727`, `1707`, `1673`,
  and `1672 ms`; no page error.
- **After / current SEAM working tree:** camera `0,0,1 → 180,90,1`; two actual
  GPU-canvas draws; pan duration `433.2 ms`; first draw `116.2 ms`; pointer
  dispatch wall times `[4,3,1] ms`; zero long tasks; no page error. The
  current sample carried its own identical adapter-first attestation.

Both sides visibly advanced the real camera and reached the GPU canvas, so the
functional redraw half passes. The after side shows no pan regression from
camera quarantine under the matched sample; it removes the multi-second
baseline stalls rather than worsening them. The first exploratory harness had
left the camera translated while trying to reverse a gesture; that harness
state was not used for the matched current row. The app's own camera settle
path restored `0,0,1`, and a fresh page independently read
`{x:0,y:0,zoom:1}` over the same 174 blocks.

### G10 mandatory twin line — PASS

The current headed page ran the real ground under an 8 px continuous pan
oscillation with the twin flag on from `performance.now()=51321.3` through
`116381.0`: `65059.7 ms` total. Flag-off receipt:

```json
{"frames":254,"divergences":0,"lastFrame":257}
```

The console recorded zero `[FRAME-TAPE-TWIN/DIVERGENCE]` lines and the page
recorded zero errors. Adapter for this capture was again
`isFallbackAdapter=false`, Radeon RX 7900 XTX, AMD/RDNA-3. Camera was
`0,0,1` before the interval, at flag-off, after settle, and on the subsequent
fresh-page read.

### Closing state

Fable's FULL gate remains PASS on the code surface, and its findings 1 and 2
now have the literal receipts they required. Finding 3 remains queued only for
the later shaping-correction contract: make the adapter-feature fingerprint
order-insensitive before that package opens. The bounded review has already
been consumed; Fable's remaining action is a mechanical receipt check against
those two findings, not a second review or a reopening of code scope. The next
authority is Sid's commit ruling. No commit or push was made.
