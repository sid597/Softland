# Path client comparison — Codex judge

2026-09-06. The decision is how to use these builders for production: who understands and carries the intended scope, makes good local decisions, communicates the reasons, maintains the documentation, and leaves extensible code with an appropriate verification burden. Passing examples and reproduced defects are evidence for that assessment. They do not replace it.

**My assessment of these deliveries: Claude carries more of the intended transfer and explains its system-level reasoning more fully. Codex leaves a smaller implementation, a cleaner public path-value boundary, and more disciplined documentation of the implemented subset. Both make credible engineering decisions; neither has yet demonstrated reliable unsupervised completion of this whole task.** This is an assessment of the checked artifacts, not a measured reliability rate for either family.

**Correction after the Claude judge's exchange:** my earlier claim that Codex's placed-ink fixture passed was too broad. Its flag passed while its picture was wrong. Comparing my own original captures and executing a viewport-argument substitution confirms the defect and its cause (F4). A fresh run of its repository verifier also fails. These findings strengthen the preference for Claude as accountable owner of the next broad transfer, and reduce the weight I gave Codex's precise-looking verification account. They also expose a mistake in this judge's use of the evidence.

## Production judgment: where they diverged, and what it buys

Both began at `bedb890`, removed the triangle path route, adopted common directed curves and shared coverage, and kept GPU ownership outside their geometry. Their divergence is substantial inside that shared direction:

| Decision | Claude delivery | Codex delivery | My assessment |
|---|---|---|---|
| What the renderer receives | An authored record, source/tool declarations and optional construction; preparation executes and retains the construction. | A common path component; a caller executes the source recipe before handing over the value. | Claude owns more of the current end-to-end edit problem. Codex provides a better boundary for future independent path producers. Neither advantage erases the other responsibility. |
| Source behavior transferred | Streamlining, simulated pressure, width expression, tapers, fitting/decimation and spline conversion; broader cap/join/alignment/dash choices. | Streamlining and line/Catmull–Rom conversion; round-nib union/accumulation and restricted closed borders; several prototype behaviors explicitly deferred. | Claude is closer to the full tool transfer. Codex's smaller scope cannot be credited as implementing the same thing more efficiently. The nonlinear sweep defect limits Claude's semantic success within that breadth. |
| Editable behavior language | Dotted-path bindings, a separate arithmetic-string parser, source/geometry capabilities, execution/read logs. | Explicit EDN expressions and references, capability calls and an `each` loop; the installed path capability table only converts sources. | Claude supplies more current path operations and observability. Codex's explicit references and smaller evaluator are attractive for unambiguous saved records. Its loop is useful generic machinery, not evidence that a stateful brush is implemented. |
| Dependency work | Observed-read run cache, frame keys, scale buckets, pack/row caches. | Construction scheduling at the caller, geometry-content keys, exact projected-scale keys, per-item resources. | Claude attempts more automatic reuse and owns more bookkeeping. Codex has a more explicit division of responsibilities but leaves the caller's construction invalidation to be built. Renderer counters alone do not compare the same work in the two designs. |
| GPU storage/draw organization | Shared dynamic atlas, compaction/regrowth, instance pool, batched path draws and optional cell covers. | Individually owned region textures/buffers and ordered per-region draws. | Claude spends code on reuse and batching; Codex spends less code on resource coordination. Which uses less time/memory under a representative document is unmeasured. Private storage organization is comparatively easy to replace without changing authored data. |
| Projected consumer | Reuses the path region/pack route with a fixed placement bucket. | Computes a projected derivative bound and reconstructs samples through a plane homography, with explicit near-plane rejection. | Codex addresses projection explicitly but passes the encode viewport where its fragment calculation needs the render target's dimensions. F4 confirms missing ink in the existing fixture. Claude preserves that fixture with a small residual against the old image. Neither has a broad projected-scale performance receipt. |

The decision-changing source is in each lane's `path/component.cljc`, `path/records.cljc`, `path/source.cljc`, `path/renderer.cljs`, `engine/executor.cljc`, `engine/coverage*.cljs`, and `region3d/on_plane_renderer.cljs`. The exact worktree roots and frozen hashes are under Custody below. The detailed findings further down preserve the executed observations.

### Communication and hierarchical documentation are different judgments

**For explaining the whole implementation to Sid, Claude is stronger in the available written handoff.** [HANDOFF-client-claude.md](/mnt/data/projects/Softland-claude/docs/below-the-waist/path-kind/HANDOFF-client-claude.md:86) explains why the caches have their rates, why an undo can rerun, where the camera enters, why atlases have separate lifetimes, where reactivity belongs, and which guesses need measurement. It gives a reader the reasons needed to challenge a decision or continue it. Codex's commit and local docs explain the result and limitations well, but give a thinner account of the choices behind the whole transfer. I have not read either builder's conversation, so this does not establish who was better at listening, asking questions, or keeping Sid informed during execution.

**For maintaining the requested hierarchy itself, I give Codex the edge in these artifacts.** Both updated the same six affected README scopes, and both put computational roles and function contracts in the code. Much of the surrounding documentation structure already existed at `bedb890`; neither gets credit for inventing that inherited hierarchy. Codex puts the implemented subset and limitations directly in [path/README.md](/mnt/data/projects/Softland-codex/src/app/client/path/README.md:28), the runnable commands and baseline limits in [harness/README.md](/mnt/data/projects/Softland-codex/src/app/client/harness/README.md:33), and the projection restriction in the Region3D map. The next maintainer can enter through the hierarchy without first finding an external handoff.

Claude's handoff and [separate function catalog](/mnt/data/projects/Softland-claude/docs/below-the-waist/path-kind/code-map-client-claude.md) help orientation, but the catalog is also another account of function contracts and state to keep synchronized. I would not count that duplication as better maintenance of the hierarchy. Its folder README also makes broader assertions about complete dependency discovery and source independence than the checked implementation earns. Its `pack.cljc` says the camera enters nowhere above packing, while device-width and snapping constructions explicitly read the view upstream. Those exceptions need consistent wording at the relevant levels. Codex's narrower, explicit scope statements are more trustworthy for extension, although its screen-camera defect still makes its behavior wrong in F3.

These are writing/maintenance judgments grounded in the changed docs, not a claim that more prose or more docstrings is automatically better. Neither delivery has demonstrated documentation staying correct through several subsequent implementation rounds. That longitudinal maintenance question remains open.

The exchange adds a material qualification to the Codex edge: the hierarchy explains the local runner but does not clearly tell a successor that the repository verifier and retained test namespace fail. Its sentence that the runner checks projected ink invites a stronger reading than the predicate supports. Organization and docstring discipline remain strengths; the published verification status is incomplete. Claude's fuller explanation is also not automatically a more accurate account: F1/F2 contradict broader claims about dependency discovery and nonlinear width.

### The Electric/Missionary question was present, and both answered it

The common starter explicitly asked the builders to work out data layers, data flow and where reactive programming belongs, if anywhere. Within this client replacement, **both make the right broad decision: geometry remains ordinary computation; the caller owns event/frame scheduling; a renderer owns retained derived data and GPU effects.** Adding Missionary inside a geometry function would not be evidence of a more complete answer.

**Claude answers more of the current dependency-and-rate problem end to end.** Its [handoff](/mnt/data/projects/Softland-claude/docs/below-the-waist/path-kind/HANDOFF-client-claude.md:95), path map, frame functions and observed counters connect source edits, construction execution, packing, instance updates and frame presentation. It also gives the renderer responsibility for determining whether construction inputs changed. F1 is consequently significant for this judgment: the builder took on a small dependency engine but did not include the program itself in execution validity. The outer [frame key](/mnt/data/projects/Softland-claude/src/app/client/path/frame.cljc:50) also decides view sensitivity from known declarations, which does not by itself establish the broader promise of dependencies discovered from any future recipe.

**Codex gives the cleaner scheduling boundary but completes less of that whole chain.** Its [path map](/mnt/data/projects/Softland-codex/src/app/client/path/README.md:28) and [construct docstring](/mnt/data/projects/Softland-codex/src/app/client/path/records.cljc:29) explicitly place recipe execution at the caller's edit boundary. That lets a later Missionary host schedule it without putting flows inside geometry. However, the delivered renderer does not own or demonstrate that caller's construction cache. A zero geometry-derivation counter on a color edit does not establish whether an upstream caller reran the recipe unnecessarily. Its exact-scale packing policy also does more preparation work on small scale changes than Claude's bucket policy; the impact on an actual document is unmeasured.

Neither builds or executes a complete future reactive host with input subscriptions, change coalescing, cancellation, error propagation and teardown. That host is outside this first drawing replacement. I would credit both for preserving that boundary, give Claude more credit for the current rate decomposition, and keep the unimplemented host work explicit. Neither has completed the prototype's compiled per-element execution story either: Claude parses arithmetic into an AST and evaluates it, while Codex interprets the expression data. That is a source-verified implementation limit, not a measured speed claim.

### How much code, and why

Counts below are produced from each frozen branch against `bedb890`, using [compare_sources.py](judge-codex-receipts/compare_sources.py); [the per-file JSON](judge-codex-receipts/production-comparison-lines.json) makes the classification inspectable. They are physical source lines, including comments, docstrings and embedded WGSL. Tests, harnesses, fixtures, README prose and binary images are separate. These are not estimates of typing effort or runtime cost.

| Production source changed | Claude | Codex |
|---|---:|---:|
| Added lines | 3,076 | 1,581 |
| Deleted lines | 1,697 | 1,857 |
| Net change | +1,379 | −276 |

| Resulting code in comparable areas | Claude | Codex |
|---|---:|---:|
| Path implementation, excluding Claude's fixture-only `path/records.cljc` | 2,102 | 887 |
| New shared executor/expression/coverage files | 770 | 536 |
| Placement renderer and its new projection helper, where present | 299 | 210 |

Within the path totals, source conversion is 227 versus 93 lines; path/stroke geometry is 735 versus 429; packing is 380 versus 68; the renderer is 328 versus 111. Claude's additional code largely implements broader source/stroke policies, dependency/run bookkeeping, the CPU coverage twin, cover generation and atlas-oriented preparation. Codex has less of that work, a smaller expression mechanism, and simpler resource organization. **Some of the difference is useful economy; some is omitted scope; some is a different tradeoff. Calling all of it bloat or all of it sophistication would hide the question.**

To call one implementation better on economy, I would ask what remains after the same required source behaviors and caller obligations are complete, then inspect whether its abstractions reduce the cost of the next change. These numbers alone cannot establish that.

### Are the functions pure?

The inspected new path/source/geometry/packing transformations in both lanes take explicit values and return derived values without retained mutable storage. GPU allocation, uploads and draws live in effectful owners. Both show a sound functional core in that practical sense. Both generic executors can call supplied capabilities; their purity also depends on those capabilities.

There is a concrete qualification in Claude: [engine/executor.cljc:65](/mnt/data/projects/Softland-claude/src/app/client/engine/executor.cljc:65) reads `System/nanoTime` or `performance.now`, and returns elapsed timing in the run result and step log. Therefore the **whole returned execution value is not a pure deterministic transformation**, even when its geometry capabilities are pure. This is instrumentation inside the computation, not hidden retained application state. Separating timing from the semantic execution result is a local improvement; it does not require redesigning the path kind. Codex's [executor return](/mnt/data/projects/Softland-codex/src/app/client/engine/executor.cljc:87) has no equivalent clock input. I would give Codex the edge on this specific functional boundary, while preserving the value of Claude's execution observability.

### Which local decisions leave future architecture free?

The practical test is what the next change has to touch. Replacing an implementation behind the same exchanged value is relatively local. Requiring new authored records, changes across unrelated callers, or a new location for application truth is a stronger commitment.

**I prefer Codex's public value boundary for future path producers.** Its [component](/mnt/data/projects/Softland-codex/src/app/client/path/component.cljc:1) accepts directed paths and paint independently of the authoring source. A future network/arrangement tool can hand it a path without teaching the renderer the network's source grammar. Claude's [component schema](/mnt/data/projects/Softland-claude/src/app/client/path/component.cljc:123) requires an authored source admitted as pen, anchors or rect even when an explicit construction is supplied. The lower-level path/region functions remain reusable, but the public component boundary has more source/construction policy attached. That is a current coupling to relax before treating this component format as the durable waist; it is not proof that the whole architecture is trapped.

There are good local decisions in both: names do not dispatch rendering; path values do not contain GPU handles; resource owners have lifetimes; text keeps its own layout/packing; neither installs a document model, store or new event loop in path geometry. Those preserve choices for future architecture.

There are also choices to keep private or provisional. Atlas organization, scale buckets and cover thresholds can be changed behind the value boundary. Expression syntax, source grammar and what an execution result means become harder to change once tool records persist. Codex's [literal shader text replacements](/mnt/data/projects/Softland-codex/src/app/client/engine/coverage_gpu.cljs:13) couple its region wrapper to the exact spelling of the shared shader; I prefer Claude's explicit shared coverage fragment on this maintenance point. This is a source-supported refactoring risk, not an executed failure or an invitation to choose an entire architecture by style.

### Can Sid rely on them to carry implementation?

My current view is that **both are useful implementation owners, with different supervision needs visible in this round**. Claude gives stronger evidence of carrying the breadth of this transfer and explaining its design, but needs challenge at the public data boundary, the dependency model and definitions that its broad claims assume. Codex gives stronger evidence of compact value-oriented implementation and explicit local limits, but needs accountability for completing the specified breadth and all callers rather than stopping at a smaller passing subset. Its consumer-contract misses in F3/F4 and its red repository verifier make that integration gap concrete. A clean-looking API and a passing local runner did not establish completion.

**My recommendation for the next production round is Claude as accountable builder and Codex as definer working through the actual implementation, with one production line.** There is enough evidence to make that assignment now. It is a starting assignment, not a permanent family hierarchy. Codex can also own a bounded numerical/value-layer implementation with an explicit scope; Claude remains responsible for integrating the result. I would change the broad assignment if Claude required repeated intervention to preserve the waist, or Codex demonstrated full scope completion with better maintenance/repair cost. We have not yet observed those follow-up costs, so there is no honest numerical reliability estimate.

The counterexamples show what escaped each delivery's own verification and where supervision is needed. They cannot, on their own, decide who is the better communicator, maintainer, architect or overall builder. The technical card and detailed receipts below are evidence within this production assessment.

### Caching: Sid's production criterion

Sid's clarification, 2026-09-06: "caching is never an option it means the underlying thing is as it should be and then still need more performance therefore we nede to do caching on top".

The production order is: establish the right representation, algorithm, explicit inputs and dependency structure; establish correct behavior; measure the remaining performance problem; then justify caching that derived work. Caching cannot make an unsuitable computation or an incomplete dependency model acceptable. Its contents are disposable derived data: recomputing from the same explicit inputs must preserve the result, including after a program or record edit.

This narrows the earlier praise of Claude's rate and cache machinery. Its explicit decomposition of what depends on what remains useful design work. The existence of more caches, observed-read tracking or scale buckets earns no performance advantage by itself; this review has not established their necessity or benefit on representative workloads. Likewise, Codex's smaller retained system is not automatically sufficient. The production builder must justify the computation first and any performance layer afterward. Atlas packing and batching also need their own evidence; neither is a substitute for this argument.

### The production findings ledger

Sid supplied [findings-for-production.md](/mnt/data/projects/Softland-claude/docs/below-the-waist/path-kind/findings-for-production.md), committed as `e8cb946` in the Claude worktree. It is absent from main at this reading. I also read its [measurement account](/mnt/data/projects/Softland-claude/docs/below-the-waist/path-kind/measurements-client-claude.md), the trace structure and the relevant current client preparation code. The ledger usefully separates observed costs, proposed remedies and landing state. It belongs in the next builder's terrain. I did not rerun these performance traces; their timings remain the builder's receipts and do not alter the frozen comparison marks.

Under Sid's caching criterion, the consequential distinctions are:

- **Finding 1 is first a change-propagation question.** The current `path/renderer.cljs:235` takes a changed frame through all items, rereads run dependencies and reconstructs rows. The source supports that work pattern; it does not establish the benefit of the drafted item cache. Work out how affected work reaches the caller/frame boundary and why unchanged items are revisited before adopting another retention layer. The proposed revision promise also needs to be explicit at that boundary. Its use cannot replace complete program/input dependencies or make the earlier stale-result finding acceptable.
- **Findings 2 and 3 contain dependency corrections.** Distinguishing the relevant pan fraction and removing bucket sensitivity from lowering that does not depend on tolerance address unnecessary invalidation. They are different from choosing a larger cache. Moving device effects into placement is a candidate ownership change that must preserve the same region meaning for every reader. The ledger's additional bucket bands and deferred repacks remain proposals with accuracy and presentation consequences to establish.
- **Finding 4 is an algorithm/representation investigation.** A more compact stroker is a promising response to excessive derived geometry, but the suggested offset construction and its expected savings are not yet demonstrated. It must preserve the declared swept nib, including nonlinear width and containing-disc cases. A new representation may change curve counts and work counts; it must not silently change the expected geometric answers. A faster incorrect outline would not satisfy this finding.

Progressive opening in finding 6 controls when work is presented and scheduled; it does not reduce the total geometry/packing work by itself. Keep it as an explicit loading-policy proposal alongside the underlying algorithm work. The undo LRU in finding 7 is already conditional on an observed need, which matches Sid's rule. The adapter observations and cover-threshold measurements in findings 5 and 8 remain bounded to their workload: the measurement account's queue-completion duration includes waiting, and it does not establish an untested multiplier of GPU capacity.

This ledger sharpens the production assignment rather than changing it. Claude owns resolving the underlying computation and dataflow, measuring the result, and justifying any remaining performance layer. Codex works the edits, view changes and reusable results through that design, including the semantic cases an algorithm replacement must preserve. These are findings to carry into the production buildout; this judge makes no further lab implementation changes.

### How to continue production, and how the chairs change

Keep the complementary attention described in [two-chairs.md](../two-chairs.md), and move the work into one implementation:

| Chair | Production responsibility |
|---|---|
| **Claude: composer and accountable builder** | Carry the whole purpose into data boundaries and working code. Own implementation, all callers, resource lifetimes, verification, repairs and hierarchical docs. Choose and justify the underlying algorithms; measure before adding performance machinery. Completion remains this chair's responsibility even when Codex supplies a finding or a bounded implementation. |
| **Codex: definer and construction author** | Create tools as records, change their behavior, and reuse their results through the actual client. Expose missing vocabulary, hidden work, incorrect meanings and dependencies with executed constructions. Offer simpler alternatives when they resolve a concrete problem. This continues the constructive exploratory role; reviewing code is one means, not its whole job. |
| **Sid: purpose, priorities and acceptance** | Decide product meanings and consequential forks, see the resulting behavior, and accept it. Routine implementation choices and verification belong to the production owner. |

The exploratory composer carried the picture and bench; the production composer now carries the working client and its maintenance. The definer's standing question stays the same: can someone create a tool, change its behavior through data, and reuse its results? Its constructions now run through the client. Either chair can propose a better construction or boundary; the roles identify responsibility, not a limit on thought.

The next work is consolidation and completion of the path delivery: close the demonstrated geometry, dependency and caller failures, complete the required tool behavior, and reconcile the repository verification route with explicit explanations for intended changes. Use Claude's broader delivery as the practical integration starting point while judging each retained decision on its merits. Preserve or reimplement the sound ideas exposed by the Codex lane, particularly the producer-independent path-value boundary and the numerical counterexamples. Selecting the builder does not settle which branch's geometry, expression language or storage organization must survive.

After that, bring the next already-defined capability through the same client interfaces; the surface-reading pickup brush is a useful next construction for the executor/compositor because it exercises execution, paint/sample and reusable results. Keep earlier records passing. The evidence should come from client harness files and pictures; this work needs no new editor screen.

A bounded exchange of repairs can occur inside that work. Another complete paired implementation, a standing pair of judges or another ranking round is not a prerequisite for production. The working cycle is: a capability runs in the client; the definer works a meaningful new construction through it; the builder reproduces and resolves findings, updating code, tests and the affected docs together; Sid sees the behavior and decides acceptance. A new architecture discussion is warranted when the construction exposes a missing exchanged value or responsibility, as the existing handoff states.

## Custody and authority

| Lane | Reviewed client | Worktree used |
|---|---|---|
| Claude | `4d9e0d61bc585a9816bc771baf8634062c7e86e4` | `/mnt/data/projects/Softland-claude` |
| Codex | `07ea01437094c1652585760f6fa1ebc60251c69f` | `/mnt/data/projects/Softland-codex` |

Claude's checked-out HEAD was `8267376`. Its client source is identical to the frozen delivery; the two later test additions are `test/app/client/path/run_pure.clj` and `test/render_engine/dump_result.mjs`. Codex's HEAD is its frozen delivery. Both worktrees were clean. The exact hashes, differences, and branch diff statistics are in [source-audit.json](judge-codex-receipts/source-audit.json). No builder source was edited and neither branch was committed by this judge. The session's pasted comparison prompt authorizes this report and a scoped commit on main; the subsequently revised `STARTER-judge.md` describes a different desk role.

Those are the original capture conditions. During the exchange, Claude's checkout had advanced to `c902552` with client changes. Its new work is not included in these first-delivery marks: the added pixel comparisons use my saved frozen-client captures. Codex remained at `07ea014`; the added repository replay verified its client and test source against that hash, and its normal bundle against my original capture digest. The projection experiment changes a call argument through a judge-owned entry point, without editing the builder's source.

The [shared starter](STARTER-client.md), [client boundary](from-12-to-client.md), [card](../two-chairs.md), [original questions](from-0-sid-questions.md), [page](path-kind.md), attacks, and [prototype handover](bench-9/HANDOVER.md) supply the meanings and expected results. **Prototype numbers are expectations only.** Every actual client number below comes from this judge's execution of the corresponding lane. The mathematical oracle is explicitly independent of both clients.

Judge identity: Codex, GPT-6 according to this session's system identity. The exact serving version, configured reasoning effort, token usage, and billed cost are not exposed in the metadata available within this fence; they are unverified. Tools used: shell/git, Clojure CLI, Shadow CLJS 2.28.23, Python, Puppeteer, Chrome WebGPU, and local image inspection. No subagents. Each browser capture retains its actual browser/Node versions, adapter, launch flags, build digest, timestamps, and elapsed time in `run.json` beside `result.json`. All browser observations here used the attested Google SwiftShader fallback adapter, not hardware throughput.

## First-delivery card

The card's scale is retained: 0 demonstrated failure, 1 partial, 2 demonstrated requested behavior, 3 also survives a meaningful variation, ? insufficient evidence. Rows are not summed. These are technical marks for the captured conditions; Sid's eye and acceptance remain his.

| What Sid judges | Codex | Claude |
|---|---|---|
| **1. Does the intended thing work in the client?** | **1 — partial.** Basic union/dabs, curved hole, clipping and world-space device border pass. The screen-fixed border fails F3 and the existing placed-ink picture fails F4 despite its green runtime flag. Full sampled-draw fitting/taper behavior is not transferred by this delivery. | **1 — partial.** Three tools, crossing, clips and fractional placement pass the shipped harness; placed ink is preserved with the image-comparison qualification in F4. The nonlinear union variation fails F2; the retained recipe variation fails F1. |
| **2. Can tools be created, changed and reused as data?** | **3, within implemented capabilities.** The judge edits a rectangle recipe's width argument into an arithmetic expression, executes it, and supplies its returned path to a second tool as a clip. The retained drawing changes at the expected edge; no engine change. R2 below. | **1 — partial.** Shipped geometry/settings changes and returned-outline clipping work. A changed construction works in a fresh renderer but the retained renderer keeps the previous result, even after revision increases. F1 prevents the broader mark. |
| **3. Is the implementation sound?** | **0 for the tested invariants.** CPU preparation and GPU drawing disagree about screen-fixed camera ownership (F3); projected ink uses the wrong viewport dimensions (F4). Both have rendered counterexamples. Sound portions and untested limits remain listed below. | **0 for the tested invariants.** The run cache misses a dependency on the program itself (F1). CPU and GPU agree on an independently wrong nonlinear region (F2). Passing their agreement test cannot close either defect. |
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

## Production structure to preserve, and where receipts stop

Both lanes provide the intended broad separation: caller-owned records/path values; pure geometry; disposable packed coverage; renderer-owned GPU resources; a caller-owned frame/pass boundary. Their READMEs state that reactivity belongs at that caller boundary. Neither geometry implementation introduces a hidden reactive loop. The review did not load or exercise a product editing host, so this is a source-verified ownership map plus harness execution, not a claim about an interactive scheduler.

Claude executes record constructions through its capability table, caches runs by observed reads, packs regions by scale bucket into a shared dynamic atlas, and writes instance rows separately. Codex executes source recipes into path values, derives regions from path/paint declarations, retains geometry separately from paint, and owns per-item region resources. The color-edit receipts support both separations. F1 requires completing Claude's execution dependency key, not removing the separation. F3 requires correcting Codex's input contract, not discarding its geometry cache.

Both remove `path/tessellation.cljc` and the old mesh derivation from the client. Searches for `tessellation|derive-mesh-set` under each client return no matches; the migration is also traced through the actual callers. Claude's [harness draw](/mnt/data/projects/Softland-claude/src/app/client/harness/path.cljs:75) reaches `prepare-path-frame!` and coverage instances; its [on-plane preparation](/mnt/data/projects/Softland-claude/src/app/client/region3d/on_plane.cljc:109) runs the same component and packer, and Region3D calls the placement renderer. Codex's [path draw](/mnt/data/projects/Softland-codex/src/app/client/harness/path.cljs:43) reaches its retained preparation and coverage draws; its [placed preparation](/mnt/data/projects/Softland-codex/src/app/client/region3d/on_plane_renderer.cljs:45) uses the same regions/packer under projection. Its retained `draw-path-range!` name denotes six-vertex coverage covers, not the deleted filled meshes. Text uses each lane's shared engine coverage implementation and executes in the full capture.

The projected runtime flag is green in both, but it misses Codex's incorrect picture in F4. Its `ink-vertices` field is **not comparable**: Claude reports packed curves there and Codex reports cover vertices. No throughput conclusion follows from those counts. Claude's fixed placement bucket and Codex's projected derivative bound are different source-verified choices; an extreme projected-scale/near-plane comparison was not run. Likewise, successful target-lease cleanup is not proof that every error path releases every GPU allocation.

The outstanding receipt boundaries are explicit:

| Apparent receipt or claim | What actually ran / what remains |
|---|---|
| All tools as records | The first path replacement and limited capabilities. Neither lane executes the pickup brush, red-after-blue surface-value test, nested retention, or byte continuation from attacks 2–4. These are deferred by `from-12-to-client.md`; no prototype number is credited as client execution. |
| Three source kinds imply the complete three tools | Claude runs the broader sampled-draw settings. Codex implements position streamlining and line/Catmull–Rom conversion; its README explicitly leaves fitting and pressure simulation out, and its source converter has no taper consumer. Rendering a sampled path is a narrower transfer than the prototype draw tool. |
| Claude behavior-edit and nonlinear checks | Settings edits and nonlinear dabs ran. Recipe-only retained invalidation and nonlinear union were absent; F1/F2 supply their failing examples. |
| Codex `parity` and `pan` fields | `parity` holds a small set of clip/border/pen probes, not Claude's seven-zoom decisive-pixel sweep. Its `pan` rates example changes a group translation, not the camera pan. No shifted-edge agreement test analogous to Claude's fractional-placement check ran in the shipped Codex harness. |
| Codex path runner is the complete repository verifier | Its [predicate](/mnt/data/projects/Softland-codex/src/app/client/harness/run_path.mjs:62) checks the three local path goldens and selected runtime predicates. The repository verifier fails when executed. Keeping its baseline files unchanged did not establish unchanged rendering or a completed migration of the verification surface. |
| Codex placed-ink check proves the ink draws correctly | The boundary predicate checks a resolved placement and a positive vertex count. F4 shows that the actual ink pixels fail while that predicate passes; my original report also made this inference error. |
| Codex's 9/29 suite means retained path tests still load | Directly requiring `app.client.path.tessellation-test` fails because the removed namespace cannot be found. [Executed load failure](judge-codex-receipts/codex-retained-test.log). The repository test runner still names it at `test/app/test_runner.clj:36`. The full Rama-loading test runner was not run. The builder's original source-only fence helps explain the retained tests; the incomplete integration remains visible. |
| A new picture in a golden is independent semantic evidence | The shipped image comparisons establish repeatability against that lane's selected baseline. Independent crossing/clip/border assertions and the judge counterexamples supply the semantic evidence. |
| The other family checked this / collaboration improved it | Sid brought the Claude judge's assessment. Its placed-ink claim corrected this report after independent pixel comparison and a caller-argument experiment. That is demonstrated value from exchanging reviews; the builders' repair exchange remains unrun here. Other new Claude probes and successor-session costs remain that judge's receipts, not executions credited to this judge. |

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

Judge execution costs are separate. The normal builds took 9.51 s (Codex) and 9.68 s (Claude); focused JVM suites took 2.06 s and 2.98 s. The initial complete client captures took 4.053 s and 7.690 s including browser setup. These are parallel runs with different harness coverage and warm caches, **not a performance comparison or model productivity measurement**. The final probe reproduction commands, exits and wall times are retained in [Codex reproduction](judge-codex-receipts/codex-reproduce.json) and [Claude reproduction](judge-codex-receipts/claude-reproduce.json). The additional [projection reproduction](judge-codex-receipts/projection-reproduction.json) retains the repository replay, diagnostic build and capture commands, exits and durations; its browser metadata is in [run.json](judge-codex-receipts/codex-projection/run.json). Total judge elapsed/token/billing cost is unverified.

Judge setup repairs are disclosed separately: the first attempt treated Shadow's build config merge as a new top-level build and failed; the working command overrides the existing build and writes a separate bundle. The first custom probe used the legacy blend defaults; the final probe uses the same enabled scene-color contract and sRGB target view as the client harness. The committed probe JSON/PNGs are from the corrected run. Compiler warnings in custom probe logs concern the judge's JS interop and name choice; the normal lane builds above had zero warnings. None of these setup mistakes is attributed to a builder.

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

## What the next round should resolve about the builders

The recommended assignment has a reason and a limit: Claude has shown more scope ownership and explanatory follow-through here; Codex has shown a cleaner producer-independent value boundary and smaller core. Those observations support moving production forward with the roles above. They do not establish a permanent family advantage or a reliability rate. The cache machinery is not a reason for this assignment; Sid's clarified order of computation, measurement and optimization applies to both. The missing cost and interaction history stay missing rather than cancelling every useful judgment that can already be made.

Claude's suggested repair exchange is a useful first continuation: give each builder the other's named defects and observe whether it preserves sound decisions, updates the documentation where the change belongs, and investigates a red check. This should be a bounded repair of the existing implementation, with the encountered failures and remaining work reported plainly. The common starter literally fenced code to `src/app/client/`; observing that fence is not evidence of bad citizenship. The engineering shortfall is leaving the verification migration unresolved and insufficiently reported. Without the sessions, I cannot know whether either builder sought a scope ruling or deliberately set a check aside.

The exchange tests repair and continuation; it does not yet establish freedom for future architecture. An already named next capability should subsequently exercise the existing value interfaces, keep earlier records passing, and update the affected documentation levels. That reveals whether extension stays local and how much explanation or rescue Sid must supply. Capsule/envelope curve counts, an atlas or a shorter file are reasons to measure that next change, not standalone proof of production performance or a permanent worker ranking.

**The open question about Claude:** Can it preserve its breadth and clear explanation while making the public waist independent of today's authoring sources, completing the dependency model, and keeping implementation claims accurate in the hierarchy rather than relying on a parallel handoff to qualify them?

**The open question about Codex:** Can it complete all requested behavior and callers, reconcile the existing verification checks, and report remaining failures plainly while retaining its clean value boundary and code economy?
