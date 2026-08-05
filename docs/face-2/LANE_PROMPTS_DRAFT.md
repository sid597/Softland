# face-2 builder-lane prompt DRAFTS (Trunk-5, 2026-07-06 — NEVER DISPATCHED)

**Status: DRAFT INPUT ONLY.** Trunk-5 prepared these three Codex-lane
prompts after the countersign and ran the §8 wave-global duties, but Sid
halted dispatch before any builder started — ZERO face-2 code exists.
The face-2 wave belongs to the next (product-side) session, which may
reshape the lane split freely. Value here: the verified anchors, the
landed R-2 shape facts, and the gate-to-lane mapping.

**Trunk-verified duty results baked into these prompts (2026-07-06):**
- update-camera 1.0 sites live at renderer.cljs:1516/:1517/:1559/:1562 (defn :1494)
- clone-text-system :902-915 shares camera buffer + bind-group (trap 21 confirmed)
- stride sites: rect :531 · MSDF :713 · slug :768 (clip set); shadow :935 OUT
- slug AA already zoom-aware (~:320)
- far-origin f32 spike (G2 constants): anchor 1e9, entity +100.25, px-scale 100 → absolute-f32 error 27.75 world units = 2775px; anchor-relative = 0.0px; re-base at far cell holds 0.0px
- R-2 LANDED shape (post-gate, newer than contract text): threads/assign returns {:assignment :thread-rank :thread-index :fold :edges}; threads/moves is 5-arity; carry = {:assignment :edges :moves :feed}; feed rt entries carry detail :from/:to {:id :kind} (dead-end :to {:id nil :kind :none})

---

## LANE A — substrate core (scene_store · spec · actions · token_face)

You are Lane A of the Softland face-2 build wave (repo: /mnt/data/projects/Softland, branch docs/current-mental-model-local). Your lane: THE SUBSTRATE CORE — scene store, spec, action registry, token face. Pure cljc only. Work autonomously until done.

READ FIRST (binding, in order):
1. docs/current-mental-model/build/face-2/CONTRACT.md — the COUNTERSIGNED contract. Your lane implements §2 (LAWS B1–B5), §3.1 (store/differ/actions), §3.4 (Δ10 spec, Δ11 token face), honoring §4.1/4.2/4.3 adjudications and the §5 traps ledger (cite trap numbers at the code sites the ledger names).
2. src/app/client/workspace/trail_face/scene.cljc + cards.cljc + threads.cljc — the R-2 landed shape you serve. LANDED FACTS (post-gate, newer than the contract text): threads/assign returns {:assignment :thread-rank :thread-index :fold :edges}; threads/moves is 5-arity (prev-assign prev-edges new-assign new-edges entries); the carry is {:assignment :edges :moves :feed}; feed rt entries carry :entry/detail :from/:to {:id :kind} (dead-end :to {:id nil :kind :none}).
3. src/app/client/workspace/rect_tree.cljc — read-only; understand rt-node shape for slot hit-shape values.
4. test/resources/trail_face/feed.edn — the fixture (source-entry asserters are nil, live-true).

YOUR FILE FENCE (NEW files only; touching ANYTHING else = STOP and report):
- src/app/client/substrate/scene_store.cljc
- src/app/client/substrate/token_face.cljc
- src/app/client/workspace/trail_face/spec.cljc
- src/app/client/workspace/trail_face/actions.cljc
- test/app/client/substrate/scene_store_test.clj
- test/app/client/substrate/token_face_test.clj
- test/app/client/workspace/trail_face_spec_test.clj
(you may add small EDN fixtures under test/resources/trail_face/ — mirror live shapes, never invent keys the live builders don't emit)

DELIVERABLES:
1. scene_store.cljc — LAW B1: slots keyed (view-instance-id, address) with fan-out index address → #{[view-instance-id address]}; slot values are the entity's RESOLVED appearance (§4.2: {:instances [...] :hit-shape <rect-tree-node or bounds> :token-rows [...]} — EDN-round-trippable, NO closures, actions as descriptors {:action kw :target address}). LAW B5: a six-op consumer for the incseq vocabulary (:grow :degree :shrink :permutation :change :freeze) — :change → slot write; :grow/:shrink → slot lifecycle with freed slots CLEARED (a stale slot masks truth); :freeze → settled rows leave the diff stream; :permutation applies as order-indirection ONLY (LAW B4: order is rank DATA on rows; the store holds an order indirection, never bakes sequence position). LAW B3: mutation happens through ONE named entry point (e.g. apply-ops!) — the consumer edge; every read is a value snapshot (one deref); no other mutation API. Include the client-side keyed differ (§4.3 transport): (prev-rows, new-rows) → six ops, address-keyed; our producers NEVER emit :permutation (rank changes ride :change) — but the CONSUMER still handles :permutation (foreign producers). Include a world->anchor-relative helper: pack-relative [world-f64 anchor] → f32-safe value (LAW B2's math home; renderer consumes it later).
2. spec.cljc — Δ10: (make-spec overrides) → {:spec/schema-version 1 :query {...} :projection :layout/trail-time-lane@v1 :camera {:x 0.0 :y 0.0 :zoom 1.0} :policy {:band 2 :folds {}} :style-rules [] :actions {} :lineage nil}; EDN round-trip helpers; a migration hook (migrate-spec: dispatches on :spec/schema-version, v1 = identity); zoom->band policy fn ((:policy spec), zoom) → band 0|1|2 (trap 11: thresholds live in the spec :policy, NEVER hardcoded in builders). Ephemerals (hover/drag) explicitly NOT in the spec — document it in the ns docstring.
3. actions.cljc — Δ6 descriptor registry: (register! kind handler-fn opts) + (dispatch registry descriptor) resolving {:action kw :target address}. READ-ONLY kinds admitted: :open :close-appearance :zoom-to :follow :toggle-band :copy-address. Registering a world-changing kind (anything declared :writes? true, or a kind in a denylist you define with :assert :retract :edit :delete) THROWS with an honest message naming D-008 (the wall) and D-008.5 (the extension point). Registry is data (a map in an atom or passed value — prefer passed value for purity; an atom wrapper is acceptable for wiring later).
4. token_face.cljc — Δ11: (store-snapshot, spec) → token rows: per address {:address … :marks [...] :fold-count n :actions [descriptors] :band …} — pure, derived from slot :token-rows + spec :policy. Fold rows carry counts that SUM to the unfolded set (ledger 16 / G12b).

GATES — write as deftests in the same batch (these names, greppable):
- G1: one address in TWO view-instances with independent geometry; an address-level write via the fan-out patches both; picking each returns (same address, distinct appearance).
- G2: far-origin f32 math — verified constants from the trunk's spike: anchor 1e9, entity at anchor+100.25, px-scale 100 (1px per 0.01 world unit): (float entity) absolute error = 27.75 world units = 2775px; (float (- entity anchor)) error = 0.0px; anchor re-base at a far cell (anchor + 987654*1024) keeps error 0.0px. Assert absolute error > 0.5px threshold AND relative < 0.5px.
- G3 (pure half): pack-relative signature takes (world, anchor); a test asserts emitted values are anchor-relative (feeding absolute world without anchor is not expressible through the API).
- G5: a scripted six-op stream (incl. :degree, :freeze, a :permutation, interleaved :grow/:shrink) applies with patch-vector equivalence; freed slots cleared; a permuted-input fixture canonicalizes to a byte-identical store value (pr-str equality).
- G6: assignment/order APIs return address-keyed MAPS; rank/order are slot DATA; no API returns a re-sorted sequence as a contract surface.
- G11 (spec half): spec round-trips EDN with :spec/schema-version intact; migration hook applies; each param change (band, projection, camera) is a data edit exercised by test.
- G12b (unit): folded token output counts sum to the unfolded set over a fixture store.
- G13: a store value round-trips EDN with descriptors intact and replays identically (apply same ops twice from empty → same value); every descriptor kind used in fixtures is registered; registering a world-changing kind FAILS.
- G-B3 (your half): a test that the store's mutation API is the ONLY mutation path (grep-style assertion over your own ns public fns is fine) and that differ/token fns are pure (same inputs → same value, no store delta).

RULES: NEVER read src/app/server/env.clj. NO git commits. NO files outside the fence. All new nss must load under clojure -M:test on the JVM (cljc, reader conditionals only where needed). Run YOUR test nss at the end in ONE JVM: clojure -M:test -e "(require '[clojure.test :as t] '<your nss>) (t/run-tests ...)" — if a failure looks like cluster/port contention (this repo has known flakes in OTHER nss), rerun standalone once. file(1) must say text for every file you touched. Style: match the repo's comment density and docstring style (see threads.cljc — heavy law-citing docstrings); cite trap numbers from CONTRACT §5 at the exact code sites.

WHEN DONE: write docs/current-mental-model/build/face-2/LANE_A_REPORT.md — what built, per-gate pass/fail with the test output numbers, deviations (implementer-fixable class only — anything policy-shaped is a STOP), doubts with falsifiers, exact file list. Then print a one-paragraph summary.
---

## LANE B — projections · per-entity projector · gesture FSM

You are Lane B of the Softland face-2 build wave (repo: /mnt/data/projects/Softland, branch docs/current-mental-model-local). Your lane: PROJECTIONS + PER-ENTITY PROJECTOR + GESTURE FSM. Pure cljc only. Work autonomously until done.

READ FIRST (binding, in order):
1. docs/current-mental-model/build/face-2/CONTRACT.md — the COUNTERSIGNED contract. Your lane implements §3.3 (Δ9 client-side named projection registry), the per-entity projector from §3.1 (H1's grain), and §3.2's Δ14 gesture FSM (the pure decision fn ONLY — no mouse.cljs wiring, that is another lane). Honor the §5 traps ledger; cite trap numbers at code sites.
2. src/app/client/workspace/trail_face/threads.cljc — you WRAP this, never rewrite it. LANDED FACTS (post-gate, newer than contract text): threads/assign returns {:assignment :thread-rank :thread-index :fold :edges}; assignment maps target-id → thread-id-or-:band; threads/moves is 5-arity; feed rt entries carry :entry/detail :from/:to {:id :kind}.
3. src/app/client/workspace/trail_face/cards.cljc + scene.cljc — the island grammar your projector calls (feed-entry-card is band-parameterized via geom :band; band-line-ops shape; expansion-node for band 3).
4. src/app/client/workspace/rect_tree.cljc — resolve-layout/tree->rects/tree->text-ops (read-only; the projector calls resolve-layout on card nodes in LOCAL 2D — trap 8/N4: islands lay out locally then project, NEVER zoom-aware layout).
5. test/resources/trail_face/feed.edn — the fixture.

YOUR FILE FENCE (NEW files only; touching ANYTHING else = STOP and report):
- src/app/client/workspace/trail_face/projection.cljc
- src/app/client/workspace/trail_face/projector.cljc
- src/app/client/workspace/trail_face/gesture_fsm.cljc
- test/app/client/workspace/trail_face_projection_test.clj
- test/app/client/workspace/trail_face_projector_test.clj
- test/app/client/workspace/trail_face_gesture_test.clj

DELIVERABLES:
1. projection.cljc — the Δ9 NAMED PLURAL registry (client-side; the Rama placement is EXPLICITLY deferred — do not build any server-facing code):
   - A registry map projection-id → pure fn over the client-assembled feed entries, returning ADDRESS-KEYED rows {target-id {:thread <id-or-nil> :lane <rank-or-nil> :rank <int-or-nil> :band? <bool>}}.
   - :layout/trail-time-lane@v1 — EXACTLY R-2's assignment: wrap threads/assign (fold-first, connected components over lineage kinds, band for the unthreaded, arrival-ms rank). Do NOT reimplement the algorithm; derive rows from assign's output maps.
   - :layout/trail-flat@v1 — single-column time order, no thread structure (every row {:thread nil :lane 0 :rank <arrival-order-index-as-DATA> :band? false}), deterministic tiebreak (arrival-ms, then target-id) — the dense overview/agent lens.
   - (resolve-projection id) → fn or honest error naming the known ids. Projections run at DIFF rate by design — they are plain fns; NO caching/memoization inside (the caller owns rate).
2. projector.cljc — H1's grain, per entity: (project-entity entity-rows projection-row spec geom) → {:address … :instances [...] :hit-shape <resolved rect-tree node> :token-rows [...]} — the entity's RESOLVED appearance. It CALLS cards/feed-entry-card (+ expansion-node when the spec/view-state says open) with band from spec :policy (accept band as an explicit arg so the caller applies zoom→band policy), then rect-tree resolve-layout in LOCAL 2D; instances derive from tree->rects/tree->text-ops output (keep the shape the store expects: EDN-safe maps, actions as descriptors {:action kw :target address} — NO closures). One entity's change re-runs ONE entity's projection; bounded output (assert 1–50 instances per entity in tests). Cross-entity structure (threads/lanes/folds) arrives as the projection-row INPUT — never computed here.
3. gesture_fsm.cljc — Δ14's PURE decision fn: (decide state event config) → {:state <next> :intent <intent-or-nil>} where event = {:kind :mousedown|:mousemove|:mouseup|:wheel|:key :pos [sx sy] :modifiers #{} :hit <hit-result-or-nil> :key <kw>} and config = {:drag-threshold-px 4 :camera {:x :y :zoom}}. Laws: thresholds in SCREEN px (a 4px drag is 4px at every zoom — divide by zoom ONLY when converting to world); mousedown LATCHES the hit path (drags keep routing to the latched target when the pointer outruns it); intent = hit-result kind × modifiers × thresholds; wheel over empty ground → :zoom-to-cursor {:anchor-world [wx wy]} (cursor world point invariant across the zoom step — perceptually-linear exponential: zoom' = zoom * (exp (* k (- delta-y)))), wheel over a scrollable island → :island-scroll; Esc → :abandon (clears latch). Include screen→world (f64: wx = (sx - pan-x) / zoom) and the cursor-invariant zoom-step helper (returns {:zoom' :pan-x' :pan-y'} keeping the cursor's world point fixed).

GATES — write as deftests in the same batch (these names):
- G10 (registry halves): both named projections resolve by name; each returns address-keyed rows over the feed fixture; re-running one over the same feed state is byte-identical (pr-str equality); the two projections return DISTINCT row sets over the same fixture (the two-scenes-by-data-edit half that doesn't need the scene builder); trail-time-lane@v1 rows agree exactly with threads/assign's assignment/rank on the fixture.
- G15: the FSM decision fn yields the SAME intents at zoom 0.5/1/2 with the world panned (synthetic hit results; assert threshold behavior at exactly 3.9px vs 4.1px screen drag at each zoom); drag latches the mousedown hit path (move the pointer off the target mid-drag, intent still routes to the latched path); Esc abandons; the zoom-step helper keeps the cursor's world point invariant (assert to 1e-9 in f64).
- Projector tests: same inputs → identical output (purity, G-B3 half); instance count bounded 1–50 per fixture entity; output round-trips EDN (no closures); band parameter changes the appearance (band 0 vs band 2 differ; band from the caller, never hardcoded — trap 11).

RULES: NEVER read src/app/server/env.clj. NO git commits. NO files outside the fence. All nss load under clojure -M:test on the JVM. Run YOUR test nss at the end in ONE JVM; contention-shaped failure in OTHER nss → rerun standalone once. file(1) says text for every touched file. Style: match threads.cljc's law-citing docstring density; cite CONTRACT §5 trap numbers at code sites.

WHEN DONE: write docs/current-mental-model/build/face-2/LANE_B_REPORT.md — what built, per-gate pass/fail with test output numbers, deviations (implementer-fixable only; policy-shaped = STOP), doubts with falsifiers, exact file list. Then print a one-paragraph summary.
---

## LANE C — renderer surgery · the ONE lawful rect_tree edit

You are Lane C of the Softland face-2 build wave (repo: /mnt/data/projects/Softland, branch docs/current-mental-model-local). Your lane: THE RENDERER SURGERY — camera pass-through, chrome camera isolation, clip-index plumbing, anchor-relative pack discipline, and the ONE lawful rect_tree edit. This is the highest-risk lane: every non-trail mode must stay bit-identical. Work autonomously until done.

READ FIRST (binding, in order):
1. docs/current-mental-model/build/face-2/CONTRACT.md — the COUNTERSIGNED contract, especially §2 LAW B2, §3.2 (all bullets), §4.5, §4.6, §5 traps 2/7/8/14/17/21, §6 (your fence + the ONE lawful rect_tree edit), §8.6/§8.8 duties.
2. src/app/client/substrate/webgpu/renderer.cljs IN FULL before any edit — you must understand the pipeline creation, camera uniform flow, instance packing, and the text-system clone before touching anything.

TRUNK-VERIFIED ANCHORS (re-verified today at the current tree; still re-grep before each edit):
- update-camera defn :1494; the four hardcoded-1.0 call sites :1516/:1517 (primary/comparison), :1559 (text-sys), :1562 (chrome-text-sys — behind a guard at :1561; the contract says that guard is always false = dead code; VERIFY that claim yourself and record what you find).
- camera-floats (js/Float32Array. 6) :1107.
- clone-text-system :902-915 — its OWN docstring says it shares pipeline, bind-group, camera with the parent. This is trap 21: chrome would zoom with the world.
- Stride sites: rect :531, MSDF :713, slug :768 (IN the clip set); shadow :935 (OUT — v1.1/A2, do not touch shadow's layout).
- Slug AA is already zoom-aware (dilation = 0.5 / max(camera.zoom, 1e-4) around :320).
- Far-origin math (G2 constants, trunk-spiked): absolute f32 at 1e9 world = 27.75 world units error; anchor-relative = exact.

YOUR FILE FENCE (AMEND only; touching ANYTHING else = STOP and report):
- src/app/client/substrate/webgpu/renderer.cljs
- src/app/client/workspace/rect_tree.cljc — ONLY the named lawful edit below
- test/app/client/workspace/rect_tree_clip_test.clj (NEW test ns for the rect_tree halves)
STOP-CLAUSES (from the contract §12): any rect_tree change beyond the named edit; clip-index stride change breaking a non-trail pipeline's visual identity; any server file; any insert-before-shaped diff consumer.

DELIVERABLES:
1. Camera pass-through (§3.2 first bullet + §4.6): the four 1.0 sites become per-mode pass-throughs — the render entry points accept a camera value {:x :y :zoom} per mode (thread it from where pan-x/pan-y already flow); editor/sidebar/dg/settings/cmd-panel pass (0, -scroll, 1.0) producing BIT-IDENTICAL behavior (zoom stays 1.0 for them); the trail path accepts a live camera value (its caller wiring is another lane — you expose the parameter surface). update-camera already takes zoom; this is plumbing, not shader work.
2. Chrome camera isolation (§4.5, duty §8.8, gate G8b): chrome-text-sys gets its OWN camera uniform buffer AND its OWN bind-group (clone-text-system today shares both — either extend clone-text-system with an opt or allocate at the chrome creation site). Chrome updates with IDENTITY camera values (pan 0/0 or the chrome-correct constant, zoom 1.0) every frame. Repair or delete the dead guard/call at :1561-1562 per what you actually find. Add an init-time assertion that chrome's camera buffer and bind-group are NOT object-identical to text-sys's (G8b's assertable half). Then retire the +scroll-y counter-compensation ONLY IF it lives inside renderer.cljs chrome paths — if the compensation lives in combined_text.cljs (another lane's file), do NOT touch it; instead document in your report exactly which call sites must change and what to pass (the wiring lane executes it).
3. Clip-index plumbing (Δ13, §3.2): add a per-instance clip-index attribute to rect/MSDF/slug pipelines (stride grows accordingly — update pack fns and arrayStride consistently); a clip-stack (max depth 8) uploaded as a small uniform/storage buffer of world-space clip rects; fragment shaders resolve the instance's clip index against the stack (discard/alpha-zero outside). Clip index 0 = no clip (identity). Shadow pipeline untouched. Non-trail modes emit clip-index 0 everywhere → visually identical output (state in the report how you verified identity at the source level).
4. Anchor-relative pack discipline (LAW B2, §3.2 encode bullet, gates G3): pack fns take (world, anchor) and write f32(world − anchor); no pack path accepts absolute world without an anchor (anchor (0,0) is the identity for non-camera modes — bit-identical). The camera uniform gains the anchor (or pan is expressed anchor-relative) so the shader math composes; document the exact uniform layout change.
5. The ONE lawful rect_tree edit (v1.1/B1, §6): tree->text-ops gains an OPT-IN clip mode (e.g. trailing options {:clip-mode :emit-index}) that EMITS ops carrying :clip-index (indices into a clip-stack vector it builds from the ancestor clip? chain) and drops/truncates NOTHING; the DEFAULT path stays byte-identical (regression deftest: default-mode output over a representative fixture tree equals the pre-edit output — capture the pre-edit output FIRST, pin it in the test); depth cap 8 with an honest error past it. tree->rects gets the same opt-in ONLY if the T-4 clamp interacts (justify in the report either way). Layout resolution, hit-test, dispatch-event, node grammar UNTOUCHED.

TESTS/VERIFICATION:
- rect_tree halves are JVM-testable: write test/app/client/workspace/rect_tree_clip_test.clj — G14 regression half (default byte-identity), emit-index half (indices consistent with ancestor clips; nothing dropped; cap-8 error), and run it: clojure -M:test -e "(require '[clojure.test :as t] 'app.client.workspace.rect-tree-clip-test) (t/run-tests 'app.client.workspace.rect-tree-clip-test)". ALSO run the existing trail-face ns (its gates flatten through tree->text-ops default mode — they must stay green): app.client.workspace.trail-face-test.
- renderer.cljs is CLJS: do NOT run a standalone shadow-cljs compile (10-minute cold-start rule in this repo). Verify at source level: re-read every edited region; check shader WGSL struct offsets against the stride constants by hand and SHOW the arithmetic in your report; if a shadow-cljs dev server happens to be watching, its hot-reload output is your compile check — do not start one.

RULES: NEVER read src/app/server/env.clj. NO git commits. NO files outside the fence. file(1) says text for every touched file. Match the repo's comment style; cite CONTRACT §5 trap numbers (2/7/8/14/17/21) at the exact code sites.

WHEN DONE: write docs/current-mental-model/build/face-2/LANE_C_REPORT.md — what built; the dead-guard finding at :1561-1562 (verified, with the actual code); per-pipeline stride arithmetic (before/after, shown); the exact wiring surface the integration lane must call (camera param signatures, chrome compensation call sites, clip-stack upload API); G8b/G14 receipts; deviations; doubts with falsifiers; exact file list. Then print a one-paragraph summary.