# Frame layer digest — src/app/client/substrate/frame_*.cljc

DECISION SERVED (all blocks): explaining the frame layer.
All paths relative to /mnt/data/projects/Softland.

## INDEX
- A. Flow: two lanes. Semantic lane = inputs→families→arrangement→deltas→one reducer→effect view→plan view→plan. Binding lane bypasses the reducer entirely. Only caller of the whole edge is `renderer/draw-frame!`, and renderer.cljs is required by verifier.cljs alone.
- B. Scheduler: 7 legal causes, a session deadline registry, a pure `decide` returning encode?/state; the impure `decide!`/`decide-at!` have ZERO callers anywhere.
- C. Graph: resources+passes+schedule; `compile-plan-structure` (reusable, index-free) + `bind-entry-ranges` (per-frame, index-carrying); `maintain-frame-plan` is test-only; the live plan comes from frame_plan_view instead.
- D. Inputs: 10 families each declaring a set of input keys; a 3-tier sameness ladder; raw camera is a *forbidden* declared input, quantized "doors" only; a global mutable ledger of work counters.
- E. Delta: 4 semantic kinds + 3 binding kinds, minted by scene_runtime (container), region_bindings (region topology/lease/payload), renderer (entry, global, viewport-size).
- F. Semantic state: 65-line reducer; empty semantic-delta set returns the identical prior state; generation increments only when something actually changed.
- G. Effects/views: frame_effects = grammar + full-recompute oracle; frame_effect_view = maintained membership keyed by entry keys; frame_plan_view = maintained plan fragments. Both maintained views carry `oracle-equal?` checked against the batch compiler.
- H. Three phrases: each has code and a naming comment.
- I. Dead vs live: 27 fns with no src caller; `maintain-frame-plan` and `maintain-effect-spans` are test-only; `apply-deltas` (semantic-state) has exactly one src caller.
- J. Three surprises: forbidden-input fence executed at load time; plan hash is a generation counter, not a content hash; a second independent compiler kept alive only as an oracle.
- K. Generations: "W4" is the current stratum vocabulary (all eight ns docstrings); "SEAM-STEP1" is an older tape/arrangement migration surviving as comments in renderer/scene_tape/scene_store; the maintained-view rewrite (frame_effect_view/frame_plan_view/frame_semantic_state) is the newest and demoted the frame_graph/frame_effects maintainers to tests.

---

## A. THE FLOW IN ORDER

**FACT.** One frame runs as a single `let` inside `renderer/draw-frame!`. (1) `frame-input-map` gathers the frame's raw values; `frame-inputs/changed-families` compares them against last frame's map and returns only the families whose *declared* keys changed. (2) `produce-frame-entries` runs only those families' producers; `update-frame-arrangement` folds the produced entries into a sorted map keyed by scene-tape order, and `frame-delta/entry-deltas` diffs old vs new ordered keys into `:insert/:move/:remove` rows. (3) Container deltas are drained from scene_runtime's journal, region deltas from region3d_gpu, and `frame-delta/global-deltas` + `viewport-size-delta` are minted inline. (4) All *semantic* deltas go into one call: `frame-semantic-state/apply-deltas`, which fans out to `frame-effect-view/apply-deltas` then `frame-plan-view/apply-deltas` and returns `{:state :work}`. (5) The exits are read back out: `frame-effect-view/project-spans` gives `effect-spans` (numeric entry ranges per effect container) and `frame-plan-view/plan` gives the frame plan (resources, ordered passes, color-mode). (6) `compositor-gpu/draw-multipass!` receives `:plan`, `:effect-spans`, `:arrangement` and encodes. The verifier separately calls the *batch* compilers (`frame-effects/derive-effect-spans` + `frame-graph/compile-frame-plan`) to produce receipts.

Binding deltas (`:region-lease`, `:region-payload`, `:viewport-size`) never enter the reducer — they are passed straight to `draw-multipass!` as `:binding-deltas`.

**SOURCE.** src/app/client/substrate/webgpu/renderer.cljs:3678 (`draw-frame!`), :3788–3838 (input→delta gathering), :3830 (`frame-semantic-state/apply-deltas`), :3870–3877 (project-spans / plan), :3947 (`:plan plan` into `draw-multipass!`); frame_semantic_state.cljc:6–60.

**EXTRACTION** (renderer.cljs:3792–3800, the proportionality hop):
```clojure
changed-families (frame-inputs/changed-families @!prev-frame-inputs inputs)
produced (produce-frame-entries inputs changed-families)
arrangement-state (if (seq changed-families)
                    (update-frame-arrangement prior-arrangement produced changed-families)
                    prior-arrangement)
entry-deltas (if (seq changed-families) (:entry-deltas arrangement-state) [])
```

**UNCERTAINTY.** Nothing outside verifier.cljs requires `webgpu.renderer` (grep of `webgpu.renderer` over src/ + test/ returns only verifier.cljs:35 and the ns line itself), and `draw-frame!` has zero call sites. shadow-cljs.edn has exactly one build, `:render-verifier`, whose init-fn is `verifier/start!`. So the frame layer's only executing driver in-tree is the verifier harness; whether a product loop calls it through some non-require path I did not check.

---

## B. SCHEDULER

**FACT.** A *cause* is one of seven keywords naming a reason a frame might need re-encoding. `derive-causes` translates the render edge's raw booleans into that set (note camera-moved folds into `:viewport`, dirty-rect into `:interaction`). A *deadline* is a registered clocked consumer: `{:next-deadline :cadence :stop-predicate}`, held in a module-level `!deadlines` atom; `due-deadlines` is the pure step that retires stopped ones and advances due ones by whole cadence steps. `decide` is pure: given prior state, a time, causes and the deadline map, it validates causes against `legal-causes`, adds `:clock` if any deadline came due, sets `encode?` to "cause set non-empty", and returns the decision plus next state (encode/skip counters and a 64-row causes ring). `pulse-alpha` is a sink-only 30 Hz selection-outline waveform, `1.0` when disarmed. `replay` re-runs recorded `{:time :causes :plan-hash}` rows through the same pure `decide` to reproduce decisions. The *receipt* is `select-keys` over version, encodes, skips, causes-ring, clock-mode, last-plan-hash.

`decide` is pure because time is a passed-in value; `decide-at!`/`decide!` are the impure edge that reads the module deadline atom and (for `decide!`) resolves host frame time through an injected clock source exactly once.

**SOURCE.** frame_scheduler.cljc:11–12 (legal-causes), :39–68 (deadline registry), :70–83 (derive-causes), :95–117 (due-deadlines), :128–163 (decide), :165–177 (decide-at!/decide!), :179–187 (pulse-alpha), :189–197 (replay), :199–202 (receipt).

**EXTRACTION** (frame_scheduler.cljc:2–7, the purity rationale, verbatim):
```clojure
"Pure W4 cause scheduler plus the session deadline registry.

 The injected time value is consumed only by decide and sink-side helpers.
 It is never a scene/store derivation input. Runtime supplies the timestamp
 already carried by its frame pulse; replay rebinds the source to recorded
 logical time."
```
and :165–168 / :173–176:
```clojure
"Runtime edge over the module deadline registry for an already-resolved
 logical time. State itself remains owned by the render reduce."
"Resolve host frame time through the injected source exactly once, then step
 the module deadline registry."
```

**UNCERTAINTY.** `decide-at!`, `decide!`, `due-deadlines`, `deadlines`, `validate-deadline!` have zero callers in src/ or test/. `decide`/`receipt`/`initial-state` are called only at verifier.cljs:4986–4990; `replay` only at verifier.cljs:3964/3967; `pulse-alpha` only at verifier.cljs:3978/3983. The one production wiring, `frame-runtime/sync-pulse-deadline!` (frame_runtime.cljs:285–305), which calls `register-deadline!`/`unregister-deadline!`, itself has zero callers. So the scheduler is currently a tested/receipted module with no live consumer.

---

## C. GRAPH

**FACT.** A *resource* is a declared GPU surface: `{:kind :format :usage :lifetime :budget-owner}` (frame_graph.cljc:103–107). A *pass* is `{:pass/id :pass/kind :topology-rank :reads :attachments}` plus optional `:produces`, `:copy-writes`, `:presentation-terminal?`. The compiler takes `{:effect-spans :regions :capabilities :viewport :forced-color-mode :arrangement}` and runs two halves: `structure-input` reduces the inputs to a *structure key* (effect topology, region ids, capabilities, viewport shape, color mode) with all numeric parameters and entry indices stripped; `compile-plan-structure` expands that key into resources + a ranked pass list, and stamps a `:structure/hash`. Then `bind-entry-ranges` walks the current arrangement and attaches `:entry-ranges`, stamping `:plan/hash`. Structural reuse is `maintain-frame-plan`: if the structure key equals last frame's, the compiled structure object is reused unchanged and only range binding re-runs; counters `:structure-compiles`/`:structure-reuses`/`:reused?` are the receipt. `validate-plan!` fails closed on: duplicate pass ids, tied topology ranks, out-of-order pass list, per-pass read/produce ordering, exactly one presentation terminal, every resource having a lifetime + budget owner + known kind/lifetime, family laws, and region-specific laws (regions require linear color, exactly one interior producer per region, every resolve must have a scene reader). The *oracle* is `oracle-compile-frame-plan`, a second, independently written expansion (`oracle-plan-structure`) that never calls the maintained path. The *export plan* is `compile-export-plan`: a fixed three-pass PNG/raster variant (world render → transfer → async readback), always linear, binding only world-stratum entry ranges.

`legal-causes` is duplicated verbatim in both files — frame_scheduler.cljc:11–12 and frame_graph.cljc:19–20 — as two independent `def`s of the same seven-keyword set; the graph embeds it in every plan's `:schedule` map (`{:policy :on-demand :clock-source :injected/monotonic :causes legal-causes}`, frame_graph.cljc:481–484). Neither namespace requires the other.

**SOURCE.** frame_graph.cljc:19–29, :89–101, :413–485, :493–513, :615–666, :668–690, :692–775, :790–828.

**EXTRACTION** (frame_graph.cljc:413–415 and :493–495, the split rationale):
```clojure
"Compile reusable pass/resource structure. No entry index is accepted or
 retained by this function."
"Resolve the current arrangement against a reusable structure. This is run
 every frame; the returned plan is the only value that carries indices."
```
and :692–695 (why the oracle is separately written):
```clojure
"Fresh batch expansion kept separate from compile-plan-structure. Shared leaf
 constructors define vocabulary, while this independent orchestration is the
 executable fence against stale/reused structure."
```

**UNCERTAINTY.** `maintain-frame-plan` is exercised only by frame_graph_test.clj:44/51/56 — compositor_gpu.cljs:489 allocates `:!plan-state (atom (frame-graph/empty-maintained-state))` but nothing ever reads or swaps that atom (grep of `!plan-state`/`!effect-state` returns only those two allocation lines). The live plan is built by frame_plan_view instead.

---

## D. INPUTS

**FACT.** A *family input declaration* is a set of keyword input names owned by one render family: `:render.family/rect` declares `#{:rects :ordered-vis :ops-count-by-vi :order-by-vi :rect-clips-by-vi :editor-pool-info :editor-rect-count}`, and so on for shadow, msdf, slug, clip, image, path, connector, chrome, region-3d — ten families. `changed-families` compares last frame's input map to this frame's, key by key, per declaration, and returns exactly the families that changed; a device replacement invalidates all of them. That set is what limits the work: only those families' producers run. Sameness is a three-tier ladder in `input-value-same?`: pointer identity, then "system carrier identity + shape-rev" for mutable system maps, then value equality. Change *classification* per se lives in frame_delta, not here; what frame_inputs classifies is which family is stale. Raw camera values are illegal as declared inputs (`forbidden-declared-inputs` = `#{:frame-idx :pan-x :pan-y :zoom :pixel-size}`); anything camera-derived must pass a registered *quantization door* — a versioned rung, e.g. `region-encode-rung` floors log(scale)/log(1.12) so only crossing a rung counts as change. The *ledger* is a module-level atom of ~25 zeroed work counters (`:comparator-calls`, `:arrangement-upserts`, `:plan-fragments-touched`, `:region-encoded`, …) reset by `begin-ledger!` at the top of a frame, incremented along the way, and published to `globalThis.__softlandFrameLedger` at the end. It is the measured proof that work was proportional.

**SOURCE.** frame_inputs.cljc:13–23 (doors), :25–26 (forbidden), :28–69 (declarations), :72–91 (`validate-declarations!`), :100–114 (ladder), :134–147 (`changed-families`), :167–175 (rungs), :199–243 (ledger); renderer.cljs:3704 (`begin-ledger!`), :3794, :4010 (`publish-ledger!`).

**EXTRACTION** (frame_inputs.cljc:72–91, the fence, condensed):
```clojure
(when (seq raw-camera)
  (throw (ex-info "Raw camera input declared at semantic frame edge" {:inputs raw-camera})))
(when (seq unregistered)
  (throw (ex-info "Camera-derived input has no registered quantization door" {:inputs unregistered})))
...
(validate-declarations!)   ; ← runs at namespace load, line 91
```

**UNCERTAINTY.** None material. Note `validate-declarations!` is invoked as a bare top-level form on load (line 91), so a bad declaration is a load-time failure, not a test-time one.

---

## E. DELTA

**FACT.** Seven kinds in two classes.

| kind | shape (keys) | minted by |
|---|---|---|
| `:entry` (semantic) | `{:delta/kind :entry :op #{:insert :move :remove} :entry/id :old-entry :new-entry :old-key :new-key}` | renderer.cljs:3530 via `frame-delta/entry-deltas` |
| `:container` (semantic) | `{:delta/kind :container :container/id :class #{:parameter :topology} :old-decl :new-decl}` | scene_runtime.cljs:70 via `frame-delta/container-delta`, journalled at :45 |
| `:region-topology` (semantic) | `{:delta/kind :region-topology :region/id :op :old :new}` (`:old`/`:new` narrowed to `#{:region/id :shadow?}`) | region_bindings.cljs:86, :116, :120 |
| `:global` (semantic) | `{:delta/kind :global :field :old :new}` over `[:viewport-format :capabilities :forced-color-mode]` | renderer.cljs:3824 |
| `:region-lease` (binding) | `{:binding/kind :region-lease :region/id :old-lease-key :new-lease-key}` | region_bindings.cljs:89, :126, :182 |
| `:region-payload` (binding) | `{:binding/kind :region-payload :region/id :changed-fields :old :new}` | region_bindings.cljs:131 |
| `:viewport-size` (binding) | `{:binding/kind :viewport-size :old :new}` | renderer.cljs:3827 |

The `:class` on a container delta is the load-bearing bit: `:topology` if the container's `topology-signature` (parent, stack-path, depth, effect kinds, mask?) changed, `:parameter` otherwise — numbers and colours are deliberately outside the signature, so a slider drag is a `:parameter` delta that never re-plans passes. Effectless→effectless writes mint nothing at all.

**SOURCE.** frame_delta.cljc:9–10, :18–28, :99–107, :148–164, :166–197.

**EXTRACTION** (frame_delta.cljc:3–6, the whole stance):
```clojure
"Deltas are minted by the owner that already knows the changed key.  This
 namespace classifies and shapes those values; it never scans a frame to
 discover that something changed."
```

**UNCERTAINTY.** `semantic-delta?`, `binding-delta?`, `topology-signature` are exported but have zero callers outside frame_delta.cljc (the classification is done inline by the shapes' consumers instead).

---

## F. SEMANTIC STATE

**FACT.** `apply-deltas` is a 55-line reducer and the whole namespace is 65 lines. It takes prior state plus one frame's already-minted semantic deltas (entry, container, region-topology, global) plus the arrangements/registry/regions/globals, and runs exactly two children in order: `frame-effect-view/apply-deltas` then `frame-plan-view/apply-deltas` (fed the effect view's `topology-rows`). It returns `{:state :work}` where `:work` is the merged per-child work counters. The *generation* is a monotone integer on the state; it is 1 at bootstrap and increments once per applying call. An *arrangement view* here is just `:arrangement` — the ordered map of entries the frame settled on, stored beside the two derived views so all three advance under one generation number. The early-return branch is the important one: if there is prior state and the semantic delta set is empty, it returns the identical prior state with all-zero work — which is how "a binding-only frame costs nothing semantically" is enforced. `apply-deltas` has exactly one src caller (renderer.cljs:3830) and one test caller (frame_view_region_binding_test.clj:32, :41, :145, :177, :186, :202).

**SOURCE.** frame_semantic_state.cljc:1–65; renderer.cljs:3830.

**EXTRACTION** (frame_semantic_state.cljc:2, :7–9):
```clojure
"One generation authority for arrangement, effect view, and plan view."
"Apply one frame's already-minted semantic deltas.  Binding/payload deltas
 never call this reducer.  The returned `:state` is published with one swap."
```

**UNCERTAINTY.** None.

---

## G. EFFECTS + VIEWS

**FACT.** *Effects* are container-level compositing declarations, not per-entry styling: `legal-effect-keys` is `#{:effects/version :opacity :mask :layer-blur :backdrop-blur :isolate?}` — so opacity, an alpha mask, layer blur, backdrop blur, and forced isolation. (No hover; the selection halo is the scheduler's `pulse-alpha`, consumed by chrome_gpu.cljs:224–234, not an effect.) The three files split as: **frame_effects** holds the grammar (`validate-effects!`, `effectful?`, `derived-effect-declarations`, `projected-blur`), the full-recompute span oracle `derive-effect-spans`, and a CPU linear-premultiplied colour oracle (`source-over`, `apply-group-opacity`, `composite-group`). **frame_effect_view** holds the *maintained* membership state: containers keyed by id, members and boundary runs keyed by stable entry keys, never by index — `project-spans` walks the arrangement once to turn boundary keys into `[start end]` ranges for the compositor. **frame_plan_view** holds the maintained plan as *fragments* (one per effect container, one per region, one global edge fragment) in a sorted `:order` of stable pass ids; `materialize-plan` projects that to a dense `:topology-rank`-numbered pass list.

The fence: each maintained view exposes `oracle-equal?`, which recomputes from scratch and compares. `frame-effect-view/oracle-equal?` compares `project-spans` against `frame-effects/derive-effect-spans`; `frame-plan-view/oracle-equal?` compares the maintained plan against `frame-graph/oracle-compile-frame-plan`, normalized to drop `:topology-rank` and `:entry-ranges`. Both are called from `renderer/frame-tape-twin-check!` (renderer.cljs:3552–3588) behind the `__softland_frame_tape_twin_check` global flag, alongside `frame-inputs/assert-twin-equal!` on the arrangement itself. The tests that do the checking: **frame_view_region_binding_test.clj** (s2 asserts `effect-view/oracle-equal?` after an insert and after the matching remove, plus `identical?` on the untouched sibling container) and **frame_graph_test.clj:74/76** (`plan-equivalent?` of the maintained plan against a fresh oracle across an insert and a remove).

**SOURCE.** frame_effects.cljc:16–17, :204–226, :250–289, :291–317; frame_effect_view.cljc:186–241, :244–272, :274–277; frame_plan_view.cljc:111–137, :180–230, :232–313, :317–331; renderer.cljs:3552–3588; test/app/client/substrate/frame_view_region_binding_test.clj:96–119; test/app/client/substrate/frame_graph_test.clj:44–76.

**EXTRACTION** (frame_effect_view.cljc:244–247, why the maintained view refuses indices):
```clojure
"Project durable boundary keys to the batch span shape.  The arrangement is
 walked once to assign ephemeral numeric positions; positions never enter
 maintained truth."
```
and frame_effects.cljc:204–207 (why contiguity is not assumed):
```clojure
"Full recompute oracle. A container may lawfully bind multiple disjoint
 ranges; pass-class precedes stack-path in scene ordering, so contiguity is an
 optimization fact, never a compiler assumption."
```

**UNCERTAINTY.** `test/app/client/substrate/maintained_view_test.clj` does NOT touch these eight files — it is a scene-tape pick-order oracle test (requires scene-tape, containers, rect-tree, scene-store only). If the caller expected that file to be the fenced-view test, the actual one is frame_view_region_binding_test.clj.

---

## H. THE THREE PHRASES

**FACT (1) "recompute proportional to change, at every layer".** Four nested gates, each a different layer. Layer 1: `frame-inputs/changed-families` (frame_inputs.cljc:134–147) → only stale families produce. Layer 2: `frame-semantic-state/apply-deltas` (frame_semantic_state.cljc:20–28) → an empty semantic-delta set returns the identical state, zero work. Layer 3: `frame-effect-view/apply-deltas` (frame_effect_view.cljc:207–232) → only containers reachable from a delta are touched; untouched containers keep object identity. Layer 4: `frame-plan-view/apply-deltas` (frame_plan_view.cljc:270–276) → `plan-relevant?` short-circuits, and `local-topology-update` (:180–230) rebuilds only fragments named by a topology delta while `patch-effect-parameters` (:160–169) updates a parameter in place. The ledger's `:effect-containers-touched` / `:plan-fragments-touched` / `:plan-order-nodes-visited` counters (frame_inputs.cljc:199–223) are the receipt, asserted as zeroes in frame_view_region_binding_test.clj:55–59.

**FACT (2) "no execution clock as an ancestor of derivation".** Two mechanisms. The scheduler docstring is the statement (frame_scheduler.cljc:4–5: *"The injected time value is consumed only by decide and sink-side helpers. It is never a scene/store derivation input."*), and `set-clock-source!` (:18–25) makes the clock an injected function so replay can substitute recorded time. The enforcement is in frame_inputs: `forbidden-declared-inputs` includes `:frame-idx` and every raw camera value (:25–26), and `validate-declarations!` throws at load if any family declares one (:72–91). frame_runtime.cljs:2–7 restates it: *"No clock value enters scene derivation."*

**FACT (3) "the fenced incremental view with its batch oracle".** The pattern is written twice. frame_graph: `maintain-frame-plan` (:677–690) vs `oracle-compile-frame-plan` (:765–775), the latter deliberately routed through a separately-written `oracle-plan-structure` so it cannot share the bug. frame_effects: `maintain-effect-spans` (:250–289) vs `derive-effect-spans` (:204–226), named in the ns docstring (:8–9: *"The maintained door re-derives only changed containers while derive-effect-spans is its full-recompute oracle."*). The live-path version is the `oracle-equal?` pair (frame_effect_view.cljc:274, frame_plan_view.cljc:317) gated by `frame-tape-twin-check!` (renderer.cljs:3552–3588), whose comment names it: *"SEAM-STEP1 T6: the batch compiler stays executable as the independent flag-on oracle after the maintained arrangement becomes the live path."*

**SOURCE / EXTRACTION.** As anchored above.

**UNCERTAINTY.** Phrase (2)'s *enforcement* is real (a load-time throw); phrase (2)'s *scheduler* half is currently unwired (see B), so the clock discipline is presently maintained by the fence rather than exercised by a running clock.

---

## I. DEAD vs LIVE

**FACT.** Method: for each public `defn` in the eight files, grep `/<name>` across src/ and test/ excluding the defining file. Result:

**Zero callers anywhere (13):** `frame_scheduler/validate-deadline!`, `/deadlines`, `/due-deadlines`, `/decide-at!`, `/decide!`; `frame_graph/stable-hash`, `/structure-input`, `/compile-plan-structure`, `/bind-entry-ranges`; `frame_inputs/system-identity`; `frame_delta/semantic-delta?`, `/binding-delta?`, `/topology-signature`; `frame_plan_view/presentation-variant`. (The graph four are internal helpers of `compile-frame-plan`/`maintain-frame-plan`; the scheduler five are the impure edge.)

**Test-only, no src caller (14):** `frame_scheduler/set-clock-source!`, `/reset-clock-source!`, `/clock-time`, `/clear-deadlines!`, `/derive-causes`; `frame_graph/validate-plan!`, `/maintain-frame-plan`, `/plan-equivalent?`, `/region-executable?`; `frame_inputs/validate-declarations!` (also invoked at load); `frame_effects/entry-effect-chain`, `/maintain-effect-spans`, `/apply-alpha-mask`.

**Specifically asked.** `maintain-frame-plan` — callers: frame_graph_test.clj:44, :51, :56 only. No src caller; compositor_gpu.cljs:489 allocates a `!plan-state` atom seeded with `empty-maintained-state` that nothing reads. `apply-deltas` — three namespaces define one; `frame-semantic-state/apply-deltas` has one src caller (renderer.cljs:3830) and six test call sites (frame_view_region_binding_test.clj:32, 41, 145, 177, 186, 202); `frame-effect-view/apply-deltas` and `frame-plan-view/apply-deltas` are called from frame_semantic_state.cljc:30 and :39, plus directly in that same test.

**What verifier.cljs calls from these eight files:**
- `frame-inputs/family-ids` — verifier.cljs:1098, :1102
- `frame-inputs/begin-ledger!` — :5035, :5050
- `frame-inputs/ledger-receipt` — :5041, :5058
- `frame-effects/derive-effect-spans` — :3028, :3191
- `frame-effects/blur-algorithm-version` — :3225
- `frame-effects/composite-group` — :3320; `/source-over` — :3323; `/apply-group-opacity` — :3324, :3325
- `frame-graph/compile-frame-plan` — :3029, :3192, :3293, :4374, :4540, :4817
- `frame-scheduler/replay` — :3964, :3967
- `frame-scheduler/pulse-alpha` — :3978, :3983
- `frame-scheduler/decide` — :4986; `/initial-state` — :4987; `/receipt` — :4990

Note the verifier calls only the *batch* road (`compile-frame-plan`, `derive-effect-spans`) and never the maintained views — it re-derives independently and compares receipts.

**UNCERTAINTY.** "Zero callers" is grep over src/ and test/ only; a name reached dynamically (`resolve`, a string in a JS shim) would not show. I saw no such pattern in these files.

---

## J. THREE MOST SURPRISING DESIGN CHOICES

**1. The input fence executes at namespace load, not in a test.** `validate-declarations!` is called as a bare top-level form, so declaring `:zoom` as a family input makes the namespace fail to load.
```clojure
;; frame_inputs.cljc:88–91
(when (seq unregistered)
  (throw (ex-info "Camera-derived input has no registered quantization door"
                  {:inputs unregistered})))
     true)))

(validate-declarations!)
```

**2. The live plan's hash is a generation counter, not a content hash — while the batch compiler's is a real content hash.** Two different identity schemes coexist for the same artifact.
```clojure
;; frame_plan_view.cljc:126–136
:structure/hash (str "fpv1-" generation)
...
:plan/generation generation
:plan/hash (str "fpv1-" generation)}]
;; vs frame_graph.cljc:485 / :513
(assoc structure :structure/hash (stable-hash structure))
(assoc plan :plan/hash (stable-hash (dissoc plan :structure/hash)))
```

**3. A second, deliberately independent compiler is kept alive with no product job — its only purpose is to disagree.** `oracle-plan-structure` duplicates ~70 lines of pass construction rather than sharing the orchestration.
```clojure
;; frame_graph.cljc:692–695
"Fresh batch expansion kept separate from compile-plan-structure. Shared leaf
 constructors define vocabulary, while this independent orchestration is the
 executable fence against stale/reused structure."
;; :765–768
"Independent batch compiler used only as the maintained road's executable
 oracle."
```

**UNCERTAINTY.** #2 is a structural read of two `assoc` sites; I did not trace whether any consumer compares an `fpv1-` hash to an `fg1-` hash (a mismatch there would be a bug, not a design choice). `frame-plan-view/oracle-equal?` normalizes both plans before comparing and drops the hashes, so at least the fence is unaffected.

---

## K. GENERATIONS

**FACT.** Three strata are visible in-tree.

- **"W4"** is the current vocabulary and covers the whole batch/pure layer. It appears in the ns docstrings of frame_scheduler.cljc:2, frame_graph.cljc:2, frame_effects.cljc:2, in frame_runtime.cljs:2 ("Flag-only W4 product join") and :173, in scene_tape.cljc:593 ("W4 additive plan vocabulary"), compositor_gpu.cljs:20, renderer.cljs:3715, verifier.cljs:2884. Code belonging to it: the scheduler, `compile-frame-plan`/`maintain-frame-plan`/`compile-export-plan`, `derive-effect-spans`/`maintain-effect-spans`, the CPU colour oracle. An older marker survives inside it: `validate-plan!`'s docstring cites **"W0-C section 5.5's ten laws"** (frame_graph.cljc:616) — the validation law list predates the W4 compiler that enforces it. `shadow-cljs.edn:5` names the verifier build "Permanent W0-A".
- **"SEAM-STEP1"** is an arrangement/tape migration, surviving as numbered comments (T1, T2/T10, T3, T6, T8, T9, T12) in scene_tape.cljc:696, :711, scene_store.cljc:191, :306, :381, renderer.cljs:3381, :3511, :3555. Its code paths: the sorted-map arrangement with `entry-key-compare`, `update-frame-arrangement`, and the twin check itself — the T6 comment is exactly the "keep the batch compiler as the flag-on oracle" decision.
- **The maintained-view stratum (unnamed in comments)** is the newest: frame_delta.cljc, frame_semantic_state.cljc, frame_effect_view.cljc, frame_plan_view.cljc carry no W-marker at all and no generation tag. It is identifiable by consequence rather than label: it is the road renderer.cljs actually runs, and its arrival is what left `frame-graph/maintain-frame-plan` and `frame-effects/maintain-effect-spans` test-only and left compositor_gpu.cljs:489–490's two maintained-state atoms allocated but never read.

**SOURCE.** As anchored. Marker census: `grep -rho "SEAM-STEP[0-9A-Z-]*\|W4 " src/` → 8× SEAM-STEP1, 9× W4, 1× FRAME-TAPE-TWIN/DIVERGENCE.

**EXTRACTION** (renderer.cljs:3554–3556, the seam between strata 2 and 3):
```clojure
;; SEAM-STEP1 T6: the batch compiler stays executable as the independent
;; flag-on oracle after the maintained arrangement becomes the live path.
(let [batch (compile-frame-tape inputs)
```

**UNCERTAINTY (resolved to FACT).** The markers `143497a`, `LAYOUT-RETENTION`, `FRAME-RETENTION`, `FRAME-VIEW-REGION-BINDING` return **zero hits in src/ and test/ code**. They are *contract document names*, living in history/docs/render-engine/: LAYOUT-RETENTION-CONTRACT.md, FRAME-RETENTION-CONTRACT.md + FRAME-RETENTION-NOW.md, FRAME-VIEW-REGION-BINDING-CONTRACT.md, FRAME-VIEW-LOWER-RESOLUTION-CONTRACT.md, W4-FRAME-RUNTIME-NOW.md, T2-INPUT-FLOOR-CONTRACT.md, plus 9 mentions of the commit `143497a`. Doc-mention counts: SEAM-STEP1 101, FRAME-RETENTION 50, FRAME-VIEW-REGION-BINDING 17, LAYOUT-RETENTION 11. So the naming convention is: the *contract* carries the generation name, the *code* carries only "W4" and "SEAM-STEP1" inline; the third (maintained-view) stratum's name is FRAME-VIEW-REGION-BINDING, evidenced by its test filename. I did not read those contract docs.
