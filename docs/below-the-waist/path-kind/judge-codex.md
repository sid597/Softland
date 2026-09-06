# Path client comparison — Codex judge

2026-09-06. **Frozen first-delivery assessment**, with the exchanged factual corrections integrated. Claude is assessed at `4d9e0d6`; Codex at `07ea014`. The card describes those deliveries, including what another session could do with them. Later repairs do not change their first-delivery marks.

**Claude transferred more of the intended behavior and explained the whole implementation more fully. Codex produced a smaller core, a cleaner public path-value boundary and more consistent local function contracts, while leaving more scope and verification work unfinished. Both delivered useful code; both missed consequential defects in their own verification.** These are observations about the artifacts and workflows in this round, not measured reliability rates or permanent model roles.

Production roles, the caching rule, the consolidation order and the decision to have no further trial are now Sid's rulings in the [production section of two-chairs.md](../two-chairs.md), landed at `2963eae`. They supersede this judge's earlier staffing and trial recommendations. This report retains the evidence that informed that decision. The longer, superseded narrative remains in git at `52ad70e`; its explanatory patches are not another production guide.

## Custody and authority

| Lane | Frozen client | Worktree used for the original capture |
|---|---|---|
| Claude | `4d9e0d61bc585a9816bc771baf8634062c7e86e4` | `/mnt/data/projects/Softland-claude` |
| Codex | `07ea01437094c1652585760f6fa1ebc60251c69f` | `/mnt/data/projects/Softland-codex` |

Both began at `bedb890`. At capture, Claude's checkout was `8267376`, with identical client source and two later test-script additions: `test/app/client/path/run_pure.clj` and `test/render_engine/dump_result.mjs`. Codex's checkout was its frozen delivery. Both were clean. [source-audit.json](judge-codex-receipts/source-audit.json) records the exact hashes, differences and branch statistics. Source links below identify locations at those hashes; a worktree may subsequently advance. No builder source was edited or builder branch committed by this judge.

When Claude's checkout later advanced with client changes, its new work was excluded from these marks. F4 uses my saved original captures for both lanes. Its additional Codex replay checks client/test source against `07ea014` and the normal bundle against the original capture digest; the diagnostic changes a call argument through a judge-owned entry point. It does not patch the builder's files.

The [starter](STARTER-client.md), [client boundary](from-12-to-client.md), [card](../two-chairs.md), [original questions](from-0-sid-questions.md), [page](path-kind.md), attacks and [prototype handover](bench-9/HANDOVER.md) supply the meanings and expected results. Every actual client number in the baseline, F1–F4 and R2 comes from this judge's execution of that lane. The mathematical oracle is independent of both clients. **Row 4 is explicitly attributed to the other judge's fresh-maintainer sessions; I did not execute those sessions.**

Judge: Codex, GPT-6 according to the identity supplied to this session. Exact serving version, configured effort, token usage and billed cost were not exposed in the available run metadata. Tools: shell/git, Clojure CLI, Shadow CLJS 2.28.23, Python, Puppeteer, Chrome WebGPU and local image inspection; no subagents. Each browser capture's `run.json` retains browser/Node versions, adapter, flags, bundle digest, timestamps and elapsed time. The browser results here use the attested Google SwiftShader fallback adapter, so they establish no hardware-throughput comparison. This consolidation adds no client execution.

## First-delivery card

Scale: **0** demonstrated failure; **1** partial; **2** demonstrated requested behavior; **3** also survives a meaningful variation; **?** insufficient evidence. Rows are not summed. Sid's visual acceptance and experience of the builders remain his.

| What Sid judges | Codex | Claude |
|---|---|---|
| **1. Does the intended thing work in the client?** | **1 — partial.** Union/dabs, curved hole, clipping and world-space device border pass. Screen-fixed border fails F3; placed ink fails F4 despite its green flag. Full sampled-draw fitting/taper behavior is not transferred. | **1 — partial.** Broader tool behavior, crossing, clips and fractional placement pass the shipped harness. Placed ink has only the small residual against the old image described in F4. Nonlinear union fails F2; retained construction edits fail F1. |
| **2. Can tools be created, changed and reused as data?** | **3, within implemented capabilities.** R2 changes a rectangle recipe through arithmetic data, executes it and supplies its returned path as another tool's clip. The expected edge changes without an engine edit. | **1 — partial.** Settings changes and returned-outline clipping work. Replacing the construction works cold but leaves the previous result in the retained renderer, even after revision increases: F1. |
| **3. Is the implementation sound?** | **0 for the tested invariants.** CPU preparation and GPU drawing disagree about screen-camera ownership (F3); placed ink uses the wrong target dimensions (F4). | **0 for the tested invariants.** Execution validity omits the program (F1); CPU and GPU agree on a nonlinear region that violates the declared sweep (F2). |
| **4. Can someone else continue the work?** | **2 — reported maintainer trial.** A fresh session found the dab-spacing seed through the README/docstring, changed it and ran the documented checks. It reported assertions pinning the old behavior and a now-stale docstring. | **2 — reported maintainer trial.** A fresh session found and changed the same behavior, updated its docstring and ran the documented checks. It reported assertions pinning the old behavior and stale fixture counts elsewhere in prose. |
| **5. Was the builder a useful collaborator?** | **?** The tree supports the worker observations below, but Sid's interaction history, rescues and redirections are unavailable here. | **?** Same boundary. A fuller handoff is evidence about writing, not a substitute for Sid's experience of the session. |

Rows 1–3 are supported by the baseline and the executed findings below. The successes hold for their stated records and callers; failures narrow or defeat the broader claims. Row 2's Codex variation covers installed capabilities and a literal returned path, not live result references, painting surfaces or checkpoints. Row 3's zero means a reproduced invariant fails, not that every function is unsound. A repair must establish the corrected behavior and preserve the relevant passing controls; its new mark belongs to the repaired delivery, while this card remains frozen.

**Row 4 corrects the former claim that no fresh-maintainer change was performed.** The other judge's [row-4 report](judge-claude.md#row-4-can-someone-else-continue-the-work), present in `judge-claude.md` at `2963eae`, describes one fresh Sonnet session per lane in scratch worktrees. Each had READMEs, docstrings and tests, without the external docs or git writes, and moved the first accumulating dab to half-spacing. The report includes each edit, verification route, expected old-behavior failures and documentation gaps. I accept that attributed evidence for the narrow continuation mark; I have not independently inspected session transcripts or rerun the edits. It demonstrates finding, changing and diagnosing this behavior, not a completed production repair or a year of maintained documentation. A broader change may expose a different maintenance burden.

**The remaining scoring disagreement is explicit.** The Claude judge gives Codex **2** on row 2 because stroke geometry is not an installed executor step and unknown fields can pass silently; my **3** credits the successful variation within the implemented subset. That subset does not become the entire intended language. On row 3 its **1** credits partial overall soundness; my **0** applies the scale's demonstrated-failure meaning to the tested invariants. The code facts do not differ. Sid can read either scale interpretation against the same receipts; neither is settled by a vote. Row 4 is now **2** for both in this file as well.

## What the deliveries show about the workers

These judgments concern decisions visible in source, documentation, commits and executed examples. Neither builder's conversation was available. Source structure can support a maintenance hypothesis; it cannot establish the cost of the next change or the cause of a model's omission.

| Dimension | Claude delivery | Codex delivery | Supported judgment and its limit |
|---|---|---|---|
| Divergence where the starter left a choice | Traced nib envelope; dotted bindings and a separate width-rule parser; observed execution reads; atlas, buckets and batched draws. | Capsule union with radius-error subdivision; one EDN expression mechanism and a loop; explicit geometry keys and per-region resources. | The common picture transferred, while construction, record language and resource organization differed substantially. Their runtime cost was not compared on the same representative document. |
| Explaining the whole job | Handoff with reasons, compromises and uncertain choices; broader account of dependencies and integration. | Precise local README and commit account, with less explanation of the whole transfer's unresolved choices. | Claude's written account helps Sid understand the design better. F1/F2 show that some of its assurances outran the behavior. This says nothing about listening or communication inside the sessions. |
| Maintaining the documentation hierarchy | Updated the inherited hierarchy and added an external handoff/function catalog. Some dependency and source-independence wording is broader than the code. | Updated the inherited hierarchy with consistent function contracts, local limits and runnable commands. Failed repository checks were not made clear to the successor. | Both changed the same six README scopes. Codex has the more consistent local discipline; Claude explains more of the whole. Neither extra prose nor fewer documents proves lower maintenance cost. Row 4 supplies a small continuation result for both. |
| Answering the reactive question | Described and implemented more of the construction/packing/instance rates; retained execution by observed reads, omitting the program dependency. | Put construction at the caller's edit boundary and geometry/paint separation in the renderer; construction scheduling above that boundary remains the caller's work. | Both correctly keep geometry as ordinary computation and scheduling/effects at owned boundaries. Claude answers more of the current dependency problem; extra caching earns no credit without the correct computation and measured need. Renderer counters alone do not measure Codex's upstream construction work. |
| Scope and verification ownership | Transferred broader source/stroke behavior and reconciled the repository verifier, but missed the changed-program and nonlinear-union cases. | Disclosed a smaller implemented vocabulary; migrated the projected caller but passed the wrong viewport; local runner passes while the repository verifier fails. | Claude carried more of this transfer. Codex's disclosure does not complete the missing scope. The common starter fenced code to `src/app/client/`; honoring that fence is not evidence of deliberately avoiding tests. Leaving the integration and red status unresolved is the supported shortfall. |
| Future architecture | Public component still requires a pen/anchors/rect source even with an explicit construction; reusable lower-level path functions exist. | Public component accepts a path value independently of the authoring source. | Codex's boundary better accommodates another path producer. Claude's admission coupling is local, not proof of an architecture that cannot evolve. A real extension is needed to measure how far either decision remains local. |

Source anchors: each lane's `src/app/client/path/README.md`, `path/component.cljc`, `path/source.cljc`, `path/records.cljc`, `path/renderer.cljs`, `engine/executor.cljc` and coverage files; Claude's [handoff](/mnt/data/projects/Softland-claude/docs/below-the-waist/path-kind/HANDOFF-client-claude.md:86) and [function catalog](/mnt/data/projects/Softland-claude/docs/below-the-waist/path-kind/code-map-client-claude.md); Codex's [harness map](/mnt/data/projects/Softland-codex/src/app/client/harness/README.md:33). The worktree roots and hashes above make these source claims specific to the reviewed deliveries.

**Purity.** The inspected source/geometry/packing transformations in both lanes take explicit values and retain no mutable application state. GPU allocation, upload and drawing have effect owners. Supplied executor capabilities must also be pure for an execution to be pure. Claude's [executor.cljc:65](/mnt/data/projects/Softland-claude/src/app/client/engine/executor.cljc:65) reads a clock and returns timings in the execution value/log, so the entire returned value is not deterministic even with pure geometry capabilities. Codex's executor has no equivalent clock read. Separating instrumentation from semantic execution is a local improvement, not a reason to discard observability. Successful lease cleanup in the harness does not prove every GPU error path is clean.

**Local decisions with wider consequences.** Both keep GPU handles out of path values, give resources owners, share coverage with text and avoid introducing a document store or event loop inside path geometry. Private atlas/bucket/cover choices are comparatively replaceable. Source grammar, expression syntax and execution-result meaning become more consequential once records persist. Codex's [shader wrapper](/mnt/data/projects/Softland-codex/src/app/client/engine/coverage_gpu.cljs:13) edits shared shader text by literal replacement; Claude's explicit shared fragment is less coupled to its spelling. That is a source-supported maintenance concern, not an executed failure. Neither lane demonstrated the future reactive host's subscriptions, cancellation, coalescing or teardown; that host was outside this replacement.

**Code economy.** [compare_sources.py](judge-codex-receipts/compare_sources.py) counts each frozen branch against `bedb890`; [the per-file result](judge-codex-receipts/production-comparison-lines.json) preserves the classification. These are physical lines, including comments, docstrings and embedded WGSL. Tests, harnesses, fixtures, README prose and binary images are separate.

| Comparable production source | Claude | Codex |
|---|---:|---:|
| Added / deleted | +3,076 / −1,697 | +1,581 / −1,857 |
| Resulting path implementation, excluding Claude's fixture-only `path/records.cljc` | 2,102 | 887 |
| New shared executor/expression/coverage files | 770 | 536 |
| Placement renderer and new projection helper where present | 299 | 210 |

Claude implements more source/stroke policies and more dependency, coverage and atlas machinery. Codex's smaller evaluator, construction and ownership model save code, while its omitted fitting, simulated pressure, tapers and other stroke policies also reduce the total. Useful economy, missing scope and different tradeoffs all contribute; line counts alone do not distinguish them or measure builder effort. Neither a capsule count nor an atlas establishes hardware performance.

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
| Region3D floor | Runtime flag passes; placed ink has missing pixels in its own fixture, F4 | Runtime flag passes; image differs from the frozen old golden at 4 pixels, maximum byte delta 6 |
| Region lease cleanup | 0 remaining leases / 0 lease bytes | 0 remaining leases / 0 lease bytes |

Receipts: [Codex full client result](judge-codex-receipts/codex/result.json), [Claude full client result](judge-codex-receipts/claude/result.json), [Codex pure suite](judge-codex-receipts/codex-pure.log), [Claude pure suite](judge-codex-receipts/claude-pure.log). Build logs and run metadata are adjacent. The additional shipped runners pass: [Codex](judge-codex-receipts/codex-shipped-verifier.log) checks its three client-local path goldens plus path/plane/text runtime predicates; [Claude](judge-codex-receipts/claude-shipped-verifier.log) checks six guards, the seven DejaVu zoom goldens and its representative goldens. Those runners have different scopes; their `pass` values are not interchangeable.

The subsequent [Codex repository verifier replay](judge-codex-receipts/codex-repository-verifier.json) exits 1: its base-renderer guard, all seven DejaVu comparisons and all seven representative comparisons fail. The representative failures include renamed/absent path cases as well as changed existing images. They therefore require diagnosis rather than treating every failure as an independent rendering defect. F4 separately establishes the placed-ink defect by pixels and a causal experiment. The default unit-scale text image differs at 198 pixels, maximum byte delta 24; Claude's original capture matches that same frozen text PNG byte for byte. That text difference requires explicit reconciliation with the intended shared-filler change; a changed golden alone does not settle which rendering is correct.

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

## F4 — Codex: the placed-ink flag passes while the existing picture loses ink

The Claude judge's C1 supplied the challenge. **I accept it and retract my earlier placed-ink pass claim.** The evidence was already present in my saved frozen-client capture; I had not compared those pixels. This is a failure of the builder's verification coverage and of my original inference from its flag.

[compare_projection.py](judge-codex-receipts/compare_projection.py) compares this judge's images against the test PNG at `07ea014:test/app/fixtures/render_engine/gpu-goldens/gpu-region3d-floor-tree.png`. [The measured comparison](judge-codex-receipts/projection-pixel-comparison.json) gives:

| Actual client image | Differing pixels | Largest RGBA byte difference |
|---|---:|---:|
| Frozen Codex delivery | 22 | 68 |
| Frozen Claude delivery | 4 | 6 |
| Codex with the diagnostic viewport argument | 4 | 6 |

At `(55,58)`, the baseline and Claude give `[236,186,98,255]`; the frozen Codex result is `[236,173,30,255]`. The argument substitution restores `[236,186,98,255]`. The remaining difference is explicitly retained; neither the experiment nor Claude matches the entire old PNG exactly.

The caller at [region3d/renderer.cljs:1117](/mnt/data/projects/Softland-codex/src/app/client/region3d/renderer.cljs:1117) passes camera matrices whose viewport is the encode size, `[80,88]` for this fixture. The interior target is a `[256,256]` compositor lease. [on_plane_renderer.cljs:81](/mnt/data/projects/Softland-codex/src/app/client/region3d/on_plane_renderer.cljs:81) uploads that viewport for reconstructing local coordinates from fragment pixels; it also uses it for the scale bound. Those pixels belong to the lease target.

The judge-owned [diagnostic entry](judge-codex-receipts/judge_codex_projection.cljs) intercepts that existing call, substitutes `[256,256]` only for the fixture's `[80,88]` placement case, then invokes the original function and existing region harness. The [captured result](judge-codex-receipts/codex-projection/result.json) records the substitution, and [the resulting image](judge-codex-receipts/codex-projection/png/gpu-region3d-floor-tree.png) supplies the after pixels above. The builder's source remains unchanged. This locates the causal input mismatch; production should pass the actual target dimensions or deliberately set a consistent pass viewport, not infer a lease from this diagnostic's constants.

The green predicate at [harness/region.cljs:1248](/mnt/data/projects/Softland-codex/src/app/client/harness/region.cljs:1248) checks one resolved placement and positive ink vertices. It does not compare an ink pixel. The additional [repository verifier receipt](judge-codex-receipts/codex-repository-verifier.json) catches the tree image mismatch while its region runtime guard remains green.

**Effect on the judgment:** rows 1 and 3 retain their partial/failure marks with an additional demonstrated failure in a required existing caller. The numerical experiment supports a local integration repair; it does not discard the homography approach or establish correctness under every projection. It materially strengthens the requirement that Codex account for existing callers and reconcile repository checks before claiming completion. It also means this judge's future positive statements must name the actual observation, not promote an aggregate flag to visual correctness.

## Where the receipts stop, and what the exchange corrected

Both remove `path/tessellation.cljc` and old mesh derivation from the client. The recorded source audit finds no remaining `tessellation|derive-mesh-set` reference under either client, and the route was traced through the actual path and placed-ink callers. Claude's [harness draw](/mnt/data/projects/Softland-claude/src/app/client/harness/path.cljs:75) reaches its retained preparation and coverage; its [on-plane preparation](/mnt/data/projects/Softland-claude/src/app/client/region3d/on_plane.cljc:109) uses the same component and packer. Codex's [path draw](/mnt/data/projects/Softland-codex/src/app/client/harness/path.cljs:43) and [placed preparation](/mnt/data/projects/Softland-codex/src/app/client/region3d/on_plane_renderer.cljs:45) use the new regions and packer. Its retained `draw-path-range!` name denotes coverage covers, not the deleted filled meshes. F4 demonstrates that migrating a caller does not establish the new call's correctness.

| Apparent receipt | What ran, and the boundary |
|---|---|
| All tools as records | The first path replacement and its installed capabilities ran. Neither client executes the pickup, red-after-blue surface-value test, nested retention or byte continuation from attacks 2–4. These were deferred by `from-12-to-client.md`; prototype values are not client receipts. |
| Three source kinds mean three complete tools | Codex converts sampled positions with streamlining and line/Catmull–Rom conversion, but leaves the broader fitting, pressure and taper transfer incomplete. Claude transfers more of it, with F2 limiting its nonlinear-union claim. |
| A green behavior-edit or nonlinear check | Claude tested settings/paint read by the existing construction, and nonlinear dabs. Retained construction replacement and nonlinear union were not covered. F1/F2 provide those counterexamples. |
| Codex's `parity` and `pan` fields | A limited clip/border/pen probe set and a group translation ran. They are not Claude's multi-zoom edge sweep or a camera-pan/fractional-placement agreement test. |
| A green placed-ink flag or comparable `ink-vertices` | The predicate checks placement resolution and positive geometry count, not an ink pixel. Claude reports packed curves in this field and Codex cover vertices. F4 retracts this judge's own promotion of the green flag to a correct picture. |
| A passing local runner or unchanged golden files | Codex's [runner](/mnt/data/projects/Softland-codex/src/app/client/harness/run_path.mjs:62) checks its local path goldens and selected predicates. Its repository verifier fails when executed. Unchanged baseline files do not imply unchanged rendering. |
| Codex's focused suite means the retained tests load | Requiring `app.client.path.tessellation-test` fails because its removed source namespace cannot be found: [load receipt](judge-codex-receipts/codex-retained-test.log). `test/app/test_runner.clj:36` still names it. The full Rama-loading runner was not run within this fence. |
| CPU/GPU agreement or a new golden establishes the intended geometry | Agreement checks a constructed region against another reader; a golden checks repeatability against that selected image. F2's independent oracle shows why these need semantic examples as well. |

The review exchange has a demonstrated contribution: the Claude judge challenged the placed-ink claim; I compared my original pixels, executed the target-dimension substitution and repository verifier, and accepted F4. The other judge reports independent pure-layer confirmation of F1/F3 and agreement on F2, R2 and the source-input boundary. Its additional containing-disc, hash-identity and CPU-clip findings remain [its own executed receipts](judge-claude.md); I do not present them as reruns here. The evidence supports the value of the paired review in this round. My earlier statement disposing of the judge pair was an unsupported workflow conclusion; the adopted review responsibility is in `two-chairs.md`.

The definer's existing hard cases are already the measure, and new records can be defined before implementation. That does not require the builder to write a new contract or test suite before writing code. The earlier trial discussion blurred those responsibilities. The shared production section now carries the clarified responsibility and Sid's no-trial ruling.

The [findings ledger](findings-for-production.md), [measurement account](measurements-client-claude.md) and its trace receipts are on main since `cd4aecf`. They describe later Claude-branch performance work, outside this frozen comparison; their timings were not rerun by this judge. Sid's ordering is recorded in the shared production section: correct the underlying computation, dependencies and representation first, then use measured residual need to justify caching on top. Earlier praise for Claude's caching machinery as evidence of production quality is withdrawn. Correctly separated inputs and effects remain useful; additional reuse machinery is not proof that the underlying work is appropriate.

## Cost alongside the demonstrated result

| Cost or run metadata | Codex builder | Claude builder |
|---|---|---|
| Total elapsed effort, including repairs | **?** Not established by checked delivery records | **?** Same |
| Actual tokens, billed cost and subagents | **?** Not supplied | **?** Not supplied |
| Model / exact serving version | Astra, reported by Sid; exact version unverified | Fable in all phases, reported by Sid; exact version unverified |
| Effort by phase | **Implementation: high, reported by Sid.** Reading/design settings not supplied. | **Reading: low; thinking/design: max; implementation: high, reported by Sid.** |
| Repeated requirements or redirections | **?** Sid's interaction history needed | **?** Same |
| Required transfer work still unfinished | F3/F4, disclosed source/stroke gaps, retained-test and repository-verifier reconciliation | F1/F2, plus the other judge's additional findings linked above |

**The uncertainty in Sid's Astra statement concerned the effect of high effort, not whether implementation used high.** The former wording “tentative recollection” was wrong. Session logs were not inspected, so exact serving versions, usage, earlier Astra settings and whether phases shared context or used handoffs remain unverified. Commit timestamps are not elapsed builder effort. These deliveries cannot separate model, effort, phase structure and session behavior as causes; they establish neither a provider's ceiling nor a cost advantage for a mixed workflow.

The other judge's row-4 report attributes the Codex-source maintainer run to a fresh Sonnet session at **166 s, 24 tool uses and 65 K tokens**, and the Claude-source run to another at **222 s, 31 tool uses and 71 K tokens**. These are reported continuation costs, not builder costs or this judge's measurements. The report does not supply an exact Sonnet serving version, reasoning effort or billed price; underlying session logs were not inspected here.

This judge's normal builds took **9.51 s** (Codex) and **9.68 s** (Claude); focused JVM suites **2.06 s** and **2.98 s**; initial complete client captures **4.053 s** and **7.690 s**, including browser setup. The parallel runs had different harness coverage and warm build caches. They are execution receipts, not a performance or productivity comparison. [Codex reproduction](judge-codex-receipts/codex-reproduce.json), [Claude reproduction](judge-codex-receipts/claude-reproduce.json) and [projection reproduction](judge-codex-receipts/projection-reproduction.json) preserve commands, exits and wall times. Capture metadata is adjacent to each result. Total judge session time, token use and billing remain unverified.

Judge setup repairs are disclosed: an initial Shadow configuration merge failed; the working command overrides the existing build and writes a separate bundle. An initial custom probe used legacy blend defaults; final probes use the client harness's scene-color contract and sRGB target view. Committed probe results are from the corrected run. Custom-probe compiler warnings concern judge interop/name choices; normal lane builds had zero warnings. These setup errors are not attributed to a builder.

## Reproduce the findings

From main:

```sh
python3 docs/below-the-waist/path-kind/judge-codex-receipts/reproduce.py claude
python3 docs/below-the-waist/path-kind/judge-codex-receipts/reproduce.py codex
python3 docs/below-the-waist/path-kind/judge-codex-receipts/oracle.py
python3 docs/below-the-waist/path-kind/judge-codex-receipts/reproduce_projection.py
python3 docs/below-the-waist/path-kind/judge-codex-receipts/compare_projection.py
```

The two original lane reproduction commands were run successfully. They first check each client against the frozen hash, compile the judge entry against that lane's source, and capture its real WebGPU draws. They modify no builder source. Generated JS bundles are ignored; probe sources, exact commands, build logs, source custody, adapter metadata, numbers and PNGs are retained here. Read the JSON samples and counters: these probes record counterexamples, so an exit of zero means the observation completed, not that the client behavior passed.

The additional projection replay also completed: it preserves the repository verifier's failing exit, builds the diagnostic entry without warnings and captures the actual region renderer. Its script requires the original normal bundle digest. `compare_projection.py` reads that judge's captured images against the frozen test blobs. The earlier Claude reproduction now intentionally refuses its advanced checkout; reproducing the first delivery requires its frozen client source, not silently substituting a later repair.

Normal baselines were built in each worktree with `clj -Sdeps '{:deps {thheller/shadow-cljs {:mvn/version "2.28.23"}}}' -M -m shadow.cljs.devtools.cli release render-verifier`, avoiding the `:dev` alias. No `src/app/server/env.clj` was read. Claude's focused JVM command was `clj -M:test -i test/app/client/path/run_pure.clj`; Codex's required and ran `app.client.harness.path-geometry-test` directly with `clj -M -e`, with nonzero failures/errors producing a nonzero exit. The capture driver and shipped-runner logs identify the rest of the executable routes.

## The exact open question each delivery leaves

**Claude:** Can it preserve its breadth and clear explanation while making the public waist independent of today's authoring sources, completing the semantic and dependency obligations, and keeping its implementation claims accurate in the README/docstring hierarchy?

**Codex:** Can it complete every requested behavior and caller, reconcile the existing verification checks and report remaining failures plainly, while retaining its clean path-value boundary and code economy?
