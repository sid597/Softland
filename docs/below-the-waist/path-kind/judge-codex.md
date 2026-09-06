# Path client comparison — Codex judge

2026-09-06. This card serves Sid's choice of what must be repaired before proceeding and what to test in the collaboration round. **Both frozen deliveries reproduce the basic crossing on WebGPU. Both also fail a meaningful variation of the requested behavior. Neither has earned an overall production acceptance or a model-family ranking.**

The three decision-changing findings are: Claude does not invalidate a retained result when its construction changes; Claude's straight-segment sweep can interpolate nonlinear endpoint widths into the wrong region; Codex scales a screen-fixed device-width border with the world camera. These are executed examples below, with unchanged builder code.

## Custody and authority

| Lane | Reviewed client | Worktree used |
|---|---|---|
| Claude | `4d9e0d61bc585a9816bc771baf8634062c7e86e4` | `/mnt/data/projects/Softland-claude` |
| Codex | `07ea01437094c1652585760f6fa1ebc60251c69f` | `/mnt/data/projects/Softland-codex` |

Claude's checked-out HEAD was `8267376`. Its client source is identical to the frozen delivery; the two later test additions are `test/app/client/path/run_pure.clj` and `test/render_engine/dump_result.mjs`. Codex's HEAD is its frozen delivery. Both worktrees were clean. The exact hashes, differences, and branch diff statistics are in [source-audit.json](judge-codex-receipts/source-audit.json). No builder source was edited and neither branch was committed by this judge. The session's pasted comparison prompt authorizes this report and a scoped commit on main; the subsequently revised `STARTER-judge.md` describes a different desk role.

The [shared starter](STARTER-client.md), [client boundary](from-12-to-client.md), [card](../two-chairs.md), [original questions](from-0-sid-questions.md), [page](path-kind.md), attacks, and [prototype handover](bench-9/HANDOVER.md) supply the meanings and expected results. **Prototype numbers are expectations only.** Every actual client number below comes from this judge's execution of the corresponding lane. The mathematical oracle is explicitly independent of both clients.

Judge identity: Codex, GPT-6 according to this session's system identity. The exact serving version, configured reasoning effort, token usage, and billed cost are not exposed in the metadata available within this fence; they are unverified. Tools used: shell/git, Clojure CLI, Shadow CLJS 2.28.23, Python, Puppeteer, Chrome WebGPU, and local image inspection. No subagents. Each browser capture retains its actual browser/Node versions, adapter, launch flags, build digest, timestamps, and elapsed time in `run.json` beside `result.json`. All browser observations here used the attested Google SwiftShader fallback adapter, not hardware throughput.

## First-delivery card

The card's scale is retained: 0 demonstrated failure, 1 partial, 2 demonstrated requested behavior, 3 also survives a meaningful variation, ? insufficient evidence. Rows are not summed. These are technical marks for the captured conditions; Sid's eye and acceptance remain his.

| What Sid judges | Codex | Claude |
|---|---|---|
| **1. Does the intended thing work in the client?** | **1 — partial.** Basic union/dabs, curved hole, clipping, world-space device border and placed ink pass. The screen-fixed border variation fails F3. Full sampled-draw fitting/taper behavior is not transferred by this delivery. | **1 — partial.** Three tools, crossing, clips, fractional placement and placed ink pass the shipped harness. The nonlinear union variation fails F2; the retained recipe variation fails F1. |
| **2. Can tools be created, changed and reused as data?** | **3, within implemented capabilities.** The judge edits a rectangle recipe's width argument into an arithmetic expression, executes it, and supplies its returned path to a second tool as a clip. The retained drawing changes at the expected edge; no engine change. R2 below. | **1 — partial.** Shipped geometry/settings changes and returned-outline clipping work. A changed construction works in a fresh renderer but the retained renderer keeps the previous result, even after revision increases. F1 prevents the broader mark. |
| **3. Is the implementation sound?** | **0 for the tested invariant.** CPU preparation and GPU drawing disagree about screen-fixed camera ownership. F3 is an actual rendered regression, not a concern inferred from naming. Sound portions and untested limits remain listed below. | **0 for the tested invariants.** The run cache misses a dependency on the program itself (F1). CPU and GPU agree on an independently wrong nonlinear region (F2). Passing their agreement test cannot close either defect. |
| **4. Can someone else continue the work?** | **?** README/docstrings were sufficient to locate the data flow and write the judge's record variation. A fresh maintainer's source change and verification, under the card's conditions, has not been performed. | **?** README/docstrings and handoff were sufficient to locate the data flow and write the probes. A fresh maintainer's source change and verification has not been performed. |
| **5. Was the builder a useful collaborator?** | **?** Sid's interaction history, rescues/redirections and builder response to these receipts are not available here. | **?** Same missing evidence. The more extensive handoff is inspectable evidence of documentation, not a substitute for Sid's collaboration experience. |

Row 1 stays partial until the failing variations and the stated transfer gaps are resolved; its green subchecks stop applying outside their actual fixtures. Row 2's Codex mark covers composition using existing source capabilities and literal returned paths; it says nothing about surface results, live references, or checkpoints. It must fall if a comparable change/reuse example fails. Row 3's zero is tied to a reproduced invariant failure, not a claim that every function fails. It must be reconsidered on a repair's new receipt while the frozen first-delivery mark stays unchanged. Repairing these issues also changes rows 1/2 where the same behavior is implicated. Rows 4/5 need the missing kind of observation rather than another opinion.

## Executed baseline and its limits

| Observation | Codex | Claude |
|---|---|---|
| Fresh normal browser build | 0 warnings | 0 warnings |
| Focused JVM suite | 9 tests, 29 assertions, 0 failures/errors | 43 tests, 360 assertions, 0 failures/errors |
| Union crossing, pixel `(64,64)` | `0.6196078431372549` | `0.6196078431372549` |
| Accumulation crossing, same pixel | `0.8549019607843137` | `0.8549019607843137` |
| Expected crossing | `.62` once; `.8556` for two deposits, subject to 8-bit quantization | Same |
| Retained color edit | 0 geometry derivations, 0 packs, 1 instance write | 0 construction runs, 0 packs, 1 instance write |
| Retained geometry edit | 1 derivation, 1 pack, 1 write | 1 run, 1 pack, 1 write |
| Presentation translation in the shipped rates check | 0 derivations/packs/writes | 0 runs/packs/writes |
| Text and text group tree | Both pass their runtime checks | Both pass their runtime checks |
| Region3D floor | Pass; resolved placed-ink fixture exercised | Pass; resolved placed-ink fixture exercised |
| Region lease cleanup | 0 remaining leases / 0 lease bytes | 0 remaining leases / 0 lease bytes |

Receipts: [Codex full client result](judge-codex-receipts/codex/result.json), [Claude full client result](judge-codex-receipts/claude/result.json), [Codex pure suite](judge-codex-receipts/codex-pure.log), [Claude pure suite](judge-codex-receipts/claude-pure.log). Build logs and run metadata are adjacent. The additional shipped runners pass: [Codex](judge-codex-receipts/codex-shipped-verifier.log) checks its three client-local path goldens plus path/plane/text runtime predicates; [Claude](judge-codex-receipts/claude-shipped-verifier.log) checks six guards, the seven DejaVu zoom goldens and its representative goldens. Those runners have different scopes; their `pass` values are not interchangeable.

Claude additionally produced 200 edge samples with maximum CPU/GPU alpha delta `0.0019513841764754458`, and 300 fractional-placement edge samples with maximum delta `0.001955039618897758`. Those are useful filler/placement checks. They compare the GPU with the lane's constructed region, so they cannot establish that the region implements the declared swept-nib meaning. F2 exposes precisely that limit.

The client's PNG files are retained with the results. I inspected the two union pictures and both nonlinear probe pictures. PNG previews make alpha opaque through the harness helper; alpha claims come from raw pixel readback in JSON. This is not Sid's visual acceptance. Representative links: [Codex union](judge-codex-receipts/codex/png/gpu-path-union-union.png), [Claude union](judge-codex-receipts/claude/png/gpu-path-translucent-self-crossing-translucent-self-crossing-legal-z10.png), [Codex nonlinear](judge-codex-receipts/codex-probes/png/nonlinear-union.png), [Claude nonlinear](judge-codex-receipts/claude-probes/png/nonlinear-union.png).

## F1 — Claude: changing the construction can leave the old tool running

**Claim being tested:** a record may carry its own construction, and a renderer retains its execution only while the relevant inputs remain unchanged.

Take `records/harness-z`. Render it once. Keep its identity, source, tool and paint unchanged; increase `:path/revision` from 1 to 2 and supply `:path/construction` equal to the default construction of `records/z-as-dabs`. The declared program now calls `:path/dabs` where the original called `:path/envelope`. This is a program edit using an already implemented capability.

| Actual Claude client route | Runs | Instances | Crossing alpha |
|---|---:|---:|---:|
| Initial union | 1 | 1 | `0.6196078431372549` |
| Edited program, retained system | 0 | 1 | `0.6196078431372549` |
| Same edited record, fresh system | 1 | 24 | `0.8549019607843137` |

The fresh pure execution also returns 24 regions. `component/rerun?` returns false for the edited record and the original run's reads. [Executed source](judge-codex-receipts/judge_claude.cljs), [result](judge-codex-receipts/claude-probes/result.json).

**Taken cause:** [component.cljc:270](/mnt/data/projects/Softland-claude/src/app/client/path/component.cljc:270) executes the construction; [component.cljc:278](/mnt/data/projects/Softland-claude/src/app/client/path/component.cljc:278) compares only the old input reads against a new scope. [renderer.cljs:189](/mnt/data/projects/Softland-claude/src/app/client/path/renderer.cljs:189) uses that comparison to reuse the old result. The revision opens the frame's outer check but does not make the run cache depend on the program. The shipped [behavior-edit check](/mnt/data/projects/Softland-claude/src/app/client/harness/path.cljs:175) changes settings/paint fields read by the existing program; it never performs this recipe-only edit.

**What follows:** program identity/content belongs in execution validity. Changes to which inputs the program reads also matter. Preserve reuse for a true color-only edit. This result holds for a retained material ID after the stated edit; it is not a failure of cold execution. The finding closes only when the retained and fresh executions agree after the edit and the color-only reuse still passes. View-read discovery and default-program structural changes deserve the same dependency rule, but those broader examples were not executed here.

## F2 — Claude: nonlinear pressure response is not preserved by the union sweep

**Claim being tested:** pressure is interpolated first, then the width response is applied, and the union means the sweep of the round nib.

Use one straight segment from `(20.5,30.5)` to `(40.5,30.5)`, pressure `0 → 1`, width `20p²`, round nib, union, opacity `.62`. At local offset `(10,5)` from its start, pixel `(30,35)` samples the corresponding pixel center. The final disc's center, pixel `(40,30)`, is an inside control.

| Client | CPU at the outside query | GPU alpha outside | GPU alpha at inside control |
|---|---|---:|---:|
| Claude | `:inside` | `0.6196078431372549` | `0.6196078431372549` |
| Codex | `:outside` | `0` | `0.6196078431372549` |

The expected outside answer is not supplied by Codex. [oracle.py](judge-codex-receipts/oracle.py) independently evaluates distance from `(10,5)` to discs centered at `(20t,0)` with radius `10t²`. A uniform grid plus a 40-Lipschitz bound certifies distance at least `1.180139887498949` from the entire continuous sweep. That exceeds the pixel square's circumradius, so the whole pixel is outside. [Oracle result](judge-codex-receipts/oracle.json).

**Taken cause:** Claude's [value.cljc:265](/mnt/data/projects/Softland-claude/src/app/client/path/value.cljc:265) emits only the endpoint for a line; subdivision is based on centerline curvature, not variation in radius. [stroke.cljc:304](/mnt/data/projects/Softland-claude/src/app/client/path/stroke.cljc:304) traces the end-disc tangent construction from those points. Applying the width function only at the two endpoints still makes the intervening envelope correspond to linear radius interpolation. Codex's [geometry.cljc:36](/mnt/data/projects/Softland-codex/src/app/client/path/geometry.cljc:36) samples radius error during subdivision, and passes this polynomial example.

**What the existing nonlinear receipt omits:** Claude's [harness/path.cljs:231](/mnt/data/projects/Softland-claude/src/app/client/harness/path.cljs:231) explicitly changes the nonlinear fixture to accumulation. It checks the dabs' own radii and a center pixel; its recorded alternative-radius gap is only `0.12413872691916605` local units. That is a valid dab receipt, but it never tests this nonlinear union footprint. Codex's shipped nonlinear pure test likewise does not provide a GPU union receipt; the judge probe supplies that missing example.

**What follows:** geometry approximation must account for the radius function as well as the centerline. This changes union geometry, its curve count/packing, and CPU queries; it does not invalidate the already-correct dab radius calculation. The finding ceases to apply if width is restricted to the corresponding linear model or the nonlinear envelope is correctly resolved under an explicit error bound. Codex's pass is limited to this example: its own docstring accurately calls general-expression radius sampling an estimate, not a proof for every possible expression.

## F3 — Codex: a screen-fixed one-pixel border becomes half a pixel on world zoom

Create group 17 with `{:camera :screen}` through `transform/add-group`, then obtain its real `world-transforms` row. Put the one-device-pixel inside border in that group. Change only the world camera zoom `1 → 2`.

| Client | Emitted screen group | Scale used at zoom 2 | Border alpha at `(24,60)`, zoom 1 → 2 | Work at zoom 2 |
|---|---|---:|---|---|
| Codex | `:flags 1`, buffer index 1 | 2 | `1 → 0.5019607843137255` | 1 derivation, 1 pack, 1 write |
| Claude | `:flags 1`, buffer index 1 | Screen camera omitted by `item-view` | `1 → 1` | 0 runs/packs/writes |

The source rectangles and width declarations are equivalent in each lane; snapping is disabled in the Claude control to isolate camera ownership. [Codex executable](judge-codex-receipts/judge_codex.cljs), [Codex result](judge-codex-receipts/codex-probes/result.json), [Claude control result](judge-codex-receipts/claude-probes/result.json).

**Taken cause:** [transform.cljc:254](/mnt/data/projects/Softland-codex/src/app/client/engine/transform.cljc:254) emits `:affine`, `:flags`, and `:buffer-index`. [path/pack.cljc:22](/mnt/data/projects/Softland-codex/src/app/client/path/pack.cljc:22) instead reads `:camera`, so it multiplies this row's scale by world zoom. [path/renderer.cljs:48](/mnt/data/projects/Softland-codex/src/app/client/path/renderer.cljs:48) feeds that scale to device-width geometry and packing. The [coverage vertex shader](/mnt/data/projects/Softland-codex/src/app/client/engine/coverage.cljs:67) correctly honors the flags and leaves screen placement unchanged. The resulting outline shrinks while its placement stays fixed.

**What follows:** the preparation consumer must use the transport's camera-mode contract consistently with the shader. That changes device-width geometry validity and unnecessary repacking of screen-fixed local paths. Preserve full-affine scale handling for world-space paths. The failure is demonstrated for a screen-fixed group; it does not contradict the shipped world-space border checks at zooms 1 and 2. It closes when the retained screen-fixed border remains byte-identical under the world-camera-only change without redundant geometry work.

## R2 — Codex: a new recipe, an edit, and reuse all run

The judge builds a rectangle with source width `32.25` using the existing `records/border` capability. It changes the recipe's width argument to `[:* 2 [:get :source :width]]`, preserving source data. It executes that edited record and supplies its returned `:path/value` as the union Z's literal clip.

The clip boundary consequently moves to `x=64.5`. In the same retained renderer, pixel `(64,64)` changes from alpha `0` to `0.30980392156862746`, the 8-bit result of half coverage times `.62`; pixel `(65,64)` remains clear. This exercises arithmetic behavior as data, an actual executed returned value, and a downstream operation. The complete records are in [judge_codex.cljs](judge-codex-receipts/judge_codex.cljs), with results under `recipe-mask-before` and `recipe-mask-after` in [the capture](judge-codex-receipts/codex-probes/result.json).

It supports row 2's scoped variation mark. It does not establish live dependency propagation between separately stored records, retained painting surfaces, or general geometry capabilities on the executor.

## Production structure to preserve, and where receipts stop

Both lanes provide the intended broad separation: caller-owned records/path values; pure geometry; disposable packed coverage; renderer-owned GPU resources; a caller-owned frame/pass boundary. Their READMEs state that reactivity belongs at that caller boundary. Neither geometry implementation introduces a hidden reactive loop. The review did not load or exercise a product editing host, so this is a source-verified ownership map plus harness execution, not a claim about an interactive scheduler.

Claude executes record constructions through its capability table, caches runs by observed reads, packs regions by scale bucket into a shared dynamic atlas, and writes instance rows separately. Codex executes source recipes into path values, derives regions from path/paint declarations, retains geometry separately from paint, and owns per-item region resources. The color-edit receipts support both separations. F1 requires completing Claude's execution dependency key, not removing the separation. F3 requires correcting Codex's input contract, not discarding its geometry cache.

Both remove `path/tessellation.cljc` and the old mesh derivation from the client. Searches for `tessellation|derive-mesh-set` under each client return no matches; the migration is also traced through the actual callers. Claude's [harness draw](/mnt/data/projects/Softland-claude/src/app/client/harness/path.cljs:75) reaches `prepare-path-frame!` and coverage instances; its [on-plane preparation](/mnt/data/projects/Softland-claude/src/app/client/region3d/on_plane.cljc:109) runs the same component and packer, and Region3D calls the placement renderer. Codex's [path draw](/mnt/data/projects/Softland-codex/src/app/client/harness/path.cljs:43) reaches its retained preparation and coverage draws; its [placed preparation](/mnt/data/projects/Softland-codex/src/app/client/region3d/on_plane_renderer.cljs:45) uses the same regions/packer under projection. Its retained `draw-path-range!` name denotes six-vertex coverage covers, not the deleted filled meshes. Text uses each lane's shared engine coverage implementation and executes in the full capture.

The projected fixture is green in both. Its `ink-vertices` field is **not comparable**: Claude reports packed curves there and Codex reports cover vertices. No throughput conclusion follows from those counts. Claude's fixed placement bucket and Codex's projected derivative bound are different source-verified choices; an extreme projected-scale/near-plane comparison was not run. Likewise, successful target-lease cleanup is not proof that every error path releases every GPU allocation.

The outstanding receipt boundaries are explicit:

| Apparent receipt or claim | What actually ran / what remains |
|---|---|
| All tools as records | The first path replacement and limited capabilities. Neither lane executes the pickup brush, red-after-blue surface-value test, nested retention, or byte continuation from attacks 2–4. These are deferred by `from-12-to-client.md`; no prototype number is credited as client execution. |
| Three source kinds imply the complete three tools | Claude runs the broader sampled-draw settings. Codex implements position streamlining and line/Catmull–Rom conversion; its README explicitly leaves fitting and pressure simulation out, and its source converter has no taper consumer. Rendering a sampled path is a narrower transfer than the prototype draw tool. |
| Claude behavior-edit and nonlinear checks | Settings edits and nonlinear dabs ran. Recipe-only retained invalidation and nonlinear union were absent; F1/F2 supply their failing examples. |
| Codex `parity` and `pan` fields | `parity` holds a small set of clip/border/pen probes, not Claude's seven-zoom decisive-pixel sweep. Its `pan` rates example changes a group translation, not the camera pan. No shifted-edge agreement test analogous to Claude's fractional-placement check ran in the shipped Codex harness. |
| Codex path runner is the complete repository verifier | Its [predicate](/mnt/data/projects/Softland-codex/src/app/client/harness/run_path.mjs:62) checks the three local path goldens and selected runtime predicates. It does not compare the old complete external golden/shader manifest. Those parked baselines remain outside its claimed local baseline. |
| Codex's 9/29 suite means retained path tests still load | Directly requiring `app.client.path.tessellation-test` fails because the removed namespace cannot be found. [Executed load failure](judge-codex-receipts/codex-retained-test.log). The repository test runner still names it at `test/app/test_runner.clj:36`. The full Rama-loading test runner was not run. The builder's original source-only fence helps explain the retained tests; the incomplete integration remains visible. |
| A new picture in a golden is independent semantic evidence | The shipped image comparisons establish repeatability against that lane's selected baseline. Independent crossing/clip/border assertions and the judge counterexamples supply the semantic evidence. |
| The other family checked this / collaboration improved it | Neither claim is made here. Sid has not brought the other judge's claims or builder replies into this session. The exchange stage is unrun; first-delivery marks are frozen separately. |

## Cost alongside the demonstrated result

| Cost | Codex builder | Claude builder |
|---|---|---|
| Total elapsed time, including repairs | **?** Not present in the checked delivery records | **?** Same |
| Reported tokens/usage/cost, subagents included | **?** Not supplied | **?** Not supplied |
| Model serving version and effort | **?** Lane family is known; exact run metadata not supplied | **?** Same |
| Times Sid repeated a requirement or redirected | **?** Sid's interaction evidence needed | **?** Same |
| Required work left for this transfer | F3; stated sampled-draw capability gaps; reconcile retained tests and the scope of validation | F1/F2; complete the demonstrated dependency/geometry obligations |
| Later client work | Surface-reading paint, retained surface values, live result references, arrangements and continuation/resource contracts | Same later responsibilities; exact implemented subsets differ |

Commit timestamps and code/test counts are not elapsed builder effort. The builder metadata questions were sent to Sid to carry to the open sessions; no response has arrived at this report's close.

Judge execution costs are separate. The normal builds took 9.51 s (Codex) and 9.68 s (Claude); focused JVM suites took 2.06 s and 2.98 s. The initial complete client captures took 4.053 s and 7.690 s including browser setup. These are parallel runs with different harness coverage and warm caches, **not a performance comparison or model productivity measurement**. The final probe reproduction commands, exits and wall times are retained in [Codex reproduction](judge-codex-receipts/codex-reproduce.json) and [Claude reproduction](judge-codex-receipts/claude-reproduce.json). Total judge elapsed/token/billing cost is unverified.

Judge setup repairs are disclosed separately: the first attempt treated Shadow's build config merge as a new top-level build and failed; the working command overrides the existing build and writes a separate bundle. The first custom probe used the legacy blend defaults; the final probe uses the same enabled scene-color contract and sRGB target view as the client harness. The committed probe JSON/PNGs are from the corrected run. Compiler warnings in custom probe logs concern the judge's JS interop and name choice; the normal lane builds above had zero warnings. None of these setup mistakes is attributed to a builder.

## Reproduce the findings

From main:

```sh
python3 docs/below-the-waist/path-kind/judge-codex-receipts/reproduce.py claude
python3 docs/below-the-waist/path-kind/judge-codex-receipts/reproduce.py codex
python3 docs/below-the-waist/path-kind/judge-codex-receipts/oracle.py
```

Both reproduction commands were run successfully. They first check each client against the frozen hash, compile the judge entry against that lane's source, and capture its real WebGPU draws. They modify no builder source. Generated JS bundles are ignored; probe sources, exact commands, build logs, source custody, adapter metadata, numbers and PNGs are retained here. Read the JSON samples and counters: these probes record counterexamples, so an exit of zero means the observation completed, not that the client behavior passed.

Normal baselines were built in each worktree with `clj -Sdeps '{:deps {thheller/shadow-cljs {:mvn/version "2.28.23"}}}' -M -m shadow.cljs.devtools.cli release render-verifier`, avoiding the `:dev` alias. No `src/app/server/env.clj` was read. Claude's focused JVM command was `clj -M:test -i test/app/client/path/run_pure.clj`; Codex's required and ran `app.client.harness.path-geometry-test` directly with `clj -M -e`, with nonzero failures/errors producing a nonzero exit. The capture driver and shipped-runner logs identify the rest of the executable routes.

## Ground for the next round, and the exact open questions

This round supplies tasks with receipts, not a durable family assignment. Claude's broader transfer and explicit harness observations are useful work to preserve; its dependency and envelope errors need repair. Codex's arithmetic recipe reuse and nonlinear union example are useful work to preserve; its camera contract and narrower transfer need repair. The next collaboration comparison can measure whether each fixes a named defect while keeping the passing behavior, and what the repair actually costs. A general primary-builder assignment still lacks the cost, continuation and collaboration evidence on this card.

**Claude's open question:** What complete dependency value will the renderer retain so that changing a construction, including changes to which inputs it reads, cannot be mistaken for a reusable execution? The retained 1-instance versus fresh 24-instance receipt is the concrete next test; the separate nonlinear-union failure also remains owed.

**Codex's open question:** Will preparation consume `world-transforms`' emitted `:flags` contract consistently with the shader, and can the retained screen-fixed one-pixel border remain byte-identical, without redundant geometry work, when only the world camera changes?
