# Diff Paradigms — Transport of Differences Across Mappings

**Research synthesis, 2026-06-13.** Question: for a world holding heterogeneous versioned
material (text, code, chat/agent artifacts) composed into executable pipelines with stochastic
LLM nodes — how do input deltas propagate **forward** (effect, with early cutoff when the delta
dies), how are observed output differences attributed **backward** (to a prompt edit, a source
swap, a workflow change), what must be **stored** for either to be computable, what **equivalence**
each diff is taken relative to, and how the **counterfactual register** ("build X vs Y") differs
from all of the above. Framing: diff is not an operation on objects; it is the study of how
differences transport across mappings. Diff-of-things is the solved part; the arrow is the problem.

**Calibration:** 29/29 sources usable, 231 lessons, extraction by Opus 4.8 background agents,
synthesis by Fable in the main loop (no synthesis subagent). **NO adversarial verify pass — cut
by request.** Date 2026-06-13. Known source weaknesses: the visual-regression source (qtrl.ai)
is vendor marketing — used only for its noise taxonomy and one blind-spot example; the Kohavi
chapter PDF was binary-undecodable and its mechanisms were reconstructed from secondary sources
quoting it; the Diffy README documents the algorithm only qualitatively (thresholds live in
source, not docs); the counterfactuals paper is by **Lucas de Lara** (single author) — the search
layer misattributed it to Saengkyongam et al., caught by the extractor. Companion docs:
`versioning-paradigms-research.md` (identity/value/place, composition models, conflicts as
values), `../code-ingestor/code-as-material-research.md` (minted identity, declared-vs-
reconstructed continuity). This doc uses their vocabulary by reference.

---

## Finding 1 — The laws of delta transport exist, they are exactly three, and four independent theories state them

The chat-level prediction ("delta composition, round-trip lenses, propagation functoriality are
where formal laws would pay rent") is confirmed **verbatim** by the categorical line:

**Delta lenses** (Diskin et al. 2011; Johnson & Rosebrugh; nLab): states are objects, deltas are
morphisms, the forward map (Get) is a functor, and backward propagation (Put) obeys exactly:

- **(a) Identity** — `φ(a, 1) = 1`: a null view-delta produces a null source-delta. *The dead-delta
  / early-cutoff law.*
- **(b) Lift / delta-PutGet** — `f(φ(a,u)) = u`: pushing the propagated delta back through the
  forward map recovers exactly the original delta. *The round-trip law.*
- **(c) Composition / delta-PutPut** — `φ(a, v∘u) = φ(a′,v) ∘ φ(a,u)`: propagating a composite
  delta equals propagating the parts and composing. *The chain rule.*

**Change actions** (Alvarez-Picallo & Ong 2019) state the same three from the differentiation
side: a delta type is a **monoid acting on a base set** (no subtraction required); a derivative
satisfies `f(a ⊕ δa) = f(a) ⊕ ∂f(a, δa)` (forward transport, by definition); "regular"
derivatives satisfy `∂f(a, 0) = 0` (cutoff) and a chain rule `∂(g∘f)(a,δa) = ∂g(f(a), ∂f(a,δa))`
makes multi-stage pipelines compositional. **ILC** (Cai, Giarrusso et al., PLDI 2014, Agda-
checked) adds the storage invariant: changes are typed against the version they diff FROM, with
the consistency law `v ⊕ (u ⊖ v) = u`, and a 4-rule static transform (`Derive`) that propagates
changes through arbitrary λ-terms — including **changes to functions themselves**
(`Δ(σ→τ) = σ→Δσ→Δτ`), which is exactly "the prompt edit is a delta of the mapping, not of the
data." **Edit lenses** (Hofmann, Pierce, Wagner, POPL 2012) give the monoid-of-edits with a
partial action and propagation as a stateful monoid homomorphism.

Three further law-grade results to keep:

- **Subtraction is not part of the concept.** Change actions deliberately use a monoid, not a
  group; provenance semirings have no additive inverse; and the monus impossibility result
  (Amsterdamer/Deutch/Tannen 2011) proves provenance-for-difference cannot be made invariant
  under equivalent-query rewriting (the identity set is co-r.e.-complete, not finitely
  axiomatizable). "A diff is new minus old" is false in general; where there is no inverse,
  deltas cannot be recovered by subtracting snapshots and **must be stored as first-class objects**.
- **Derivatives are not unique.** Many `∂f` satisfy the forward law; the category's morphisms are
  pairs (function, **chosen** derivative). A mapping's propagation rule is selected and stored,
  not implied. There is also a declared well-behavedness ladder (differentiable ⊋ regular ⊋
  additive) — assuming deltas combine linearly is unjustified for nonlinear/stochastic stages.
- **The global soundness law** is from-scratch consistency (Adapton; "Build Systems à la Carte"
  states it as CORRECTNESS + MINIMALITY): any incremental/cutoff machinery is legitimate iff its
  result equals full recomputation up to the declared equivalence, executing each stage at most
  once. For an event-sourced world this is literally the reconciliation invariant: incrementally
  maintained projections must equal projections rebuilt from the log.

**Kernel take:** the kernel's diff contract is three local laws (dead-delta, round-trip,
composition) plus one global law (from-scratch consistency) — all four published, none invented
here; deltas are first-class stored material with a chosen propagation rule per mapping.

---

## Finding 2 — Backward attribution is stored, not derived: five traditions converge

The single strongest cross-cluster convergence. You cannot in general compute backward
attribution from the forward mapping; you must **record a lifting policy at write time**:

- **Delta lenses:** Put is *primitive data* — a cleavage (chosen opcartesian lifts), equivalently
  a cofunctor agreeing with Get on objects. Backward transport is part of the lens's definition.
- **Symmetric lenses:** the **complement** stores exactly the information each side discards;
  round-trip laws hold only when threading the *updated* complement. Composition multiplies
  complements (`C1 × C2`) — pipeline storage grows with stages and never shrinks.
- **Self-adjusting computation / Adapton:** the recorded trace (DDG/DCG with per-edge observed
  values) IS what makes propagation and attribution computable; Acar's **trace stability law**
  bounds propagation cost by the *edit distance between the two runs' traces* — the cost of
  transporting a diff is itself a diff metric, and pipelines must be designed for trace
  stability or forfeit incrementality.
- **Build systems à la carte:** the rebuilder ladder is a precise schema of "store more provenance
  → compute more": dirty bit → verifying traces (dep hashes + result hash) → constructive traces
  (+ values = cloud build) → deep constructive traces (+ lose early cutoff). And **self-tracking**:
  the rules/tasks are themselves hashed inputs — "which workflow edit caused this" is just
  another dependency edge.
- **Counterfactuals (de Lara 2023):** a per-run counterfactual requires the **abducted exogenous
  noise U** of the actual run (abduction → action → prediction). Distribution-level diffs need
  only the model; run-level diffs need the stored randomness. Seeds are provenance.

SymDiff adds the structural precondition: no relational diff can even be *formed* without a
stored **correspondence** between the two versions' parts (its procedure-mapping config) — and
diffoscope shows the production version (match by name first, then TLSH fuzzy-hash distance under
a threshold; when the matcher silently degrades, renames masquerade as delete+add phantom
deltas). **Matching precedes diffing** — which is the code-ingestor doc's identity finding
arriving from an independent direction: minted, stable identity is what makes diffs *small*;
without it every diff degenerates to "delete everything, add everything" (nbdime's admitted
no-move failure; Adapton's nominal-vs-structural naming, below).

**Kernel take:** attribution capability is decided at write time. The kernel records, per
mapping: its chosen propagation rule, its correspondence/identity keys, its trace (at the chosen
rung of the rebuilder ladder), and for stochastic runs the seeds/noise. Containers' minted IDs
are the correspondence backbone — the identity decision pays a third time.

---

## Finding 3 — Cutoff is an equivalence choice made at every node, it is fragile, and it needs a two-tier guard

Every system's early cutoff is a per-node equality test under a *declared* equivalence, and the
choice is consequential:

- Adapton: **pointer equality** (O(1); structurally-equal-but-reallocated values spuriously
  propagate). Jane Street Incremental: cutoff is a **pluggable predicate** per node (physical
  equality default; semantic or tolerance-based allowed) — and cutoff and blind-dirtying are
  mutually exclusive (their V1 dirty-counting algorithm made cutoff impossible). DVC/build
  systems: **content hash** (md5/byte identity — rebuilt-but-identical outputs still cut off,
  which timestamps cannot do). VisTrails: cache keyed by the **signature of the upstream
  subworkflow**. Semirings: absorption (`0` annihilates `×`) gives cutoff algebraically.
  diffoscope: "no difference" is `None`, a first-class outcome that prunes the tree.
- **Salsa's two production lessons.** (1) Cutoff bounds *recompute* but not *verify*: even with
  perfect laziness, deciding "nothing changed" cost O(reachable graph) — typing one character
  re-verified ~300ms of stdlib queries. The fix is an **eager coarse guard over the lazy precise
  machinery**: durability tiers (volatile/normal/durable) forming a version *vector*, with
  derived durability = MIN of inputs (a lattice meet), letting whole stable subgraphs be skipped
  by one integer compare. (2) Cutoff shields are **the main human cost**: one volatile field
  (source positions) leaking into a result type collapses the firewall for everything downstream.
  Equivalence must be engineered into the data types at every boundary.
- Two corrections to naive cutoff from the counterfactual cluster: **VOI**: a delta's value is
  decision-relative, not magnitude-relative — VOI = 0 exactly when no downstream argmax flips;
  a huge numeric change that crosses no decision boundary is dead, a tiny one that flips a
  decision carries full value. **de Lara**: conditional independence is not preserved under
  intervention — a change that looks inert in observed correlations can have real interventional
  effect; cutoff rules learned from observational data are unsound for interventions on nodes
  with descendants.

**Kernel take:** every pipeline node carries a declared cutoff predicate (byte / structural /
pointer / tolerance / distributional-with-params) — the equivalence registry is part of the
pipeline's versioned structure, not configuration. LLM nodes are *volatile-tier by definition*
in a Salsa-style durability vector (imported git facts: durable; in-loop editions: normal;
stochastic outputs: volatile). And cutoff for decision-feeding deltas must be decision-relative.

---

## Finding 4 — The delta as primary stored object is production-validated; forgetting is a governed semantic act

- **VisTrails (2007)** stored *only* actions — the version tree IS the transaction log; workflows
  are projections of action sequences ("uses substantially less space than storing multiple
  versions"). Log-primary, validated in a shipping scientific-workflow system two decades ago.
  Structural diff = invert actions to the common ancestor, replay forward, then **compress by
  cancellation** (an add later deleted contributes nothing — dead-delta elimination inside the
  diff itself).
- **Differential dataflow:** a collection IS a set of `(data, time, diff)` triples over a
  partially ordered time lattice; state at t = sum of diffs ≤ t. Arrangements share one indexed
  copy of the update history across consumers; frontier advances seal immutable batches
  (versioned commit boundaries); consolidation (sum equal-(data,time) diffs, drop zeros) is the
  cancellation primitive. Its deepest design move: **compaction is split into a semantic act and
  a physical act.** Logical compaction = a declared equivalence on timestamps ("update times may
  change as long as no comparison to any still-reachable query time changes") — you formally
  declare which historical distinctions you stop caring about, enabling cancellation. Physical
  compaction = merging batches, a pure retention decision. Forgetting is governed, monotone
  (frontiers only advance), and decoupled from storage mechanics.
- **nbdime:** a diff is a recursive tree of typed ops mirroring the structure (not a flat patch);
  RFC 6902 JSONPatch was judged *inadequate* (no range ops, no clean recursion) and replaced.
  Its merge stores **merge-decision objects** (both sides' diffs, common path, chosen action,
  conflict flag) — reconciliation as first-class material, echoing the versioning doc's
  conflicts-as-values (Pijul).

**Kernel take:** Rama's log-primary stance is re-validated from two more directions; diffs and
merge-decisions are containers like everything else; and the kernel should adopt the logical/
physical split for history — a declared coarsening equivalence (what the world agrees to stop
distinguishing) separate from retention policy. OSTRICH's VM/DM/VQ read forms (versioning doc)
are the query-side complement.

---

## Finding 5 — Transporting a diff to a *different* carrier exists (VisTrails analogy), and proof-shaped diffs invert the question

Two mechanisms beyond same-carrier diffing:

- **VisTrails "analogy":** compute diff(A, A′) as a *template*, then remap it onto an unrelated
  workflow B. The remapping uses **soft matching**: a product graph of candidate module pairings
  with PageRank-style score diffusion until convergence, thresholded — exact subgraph isomorphism
  is intractable and brittle; relaxation injects global structure. The delta is separated from
  its carrier and becomes a portable function. This is the literal "apply the change I made to
  pipeline X onto pipeline Y," shipped circa 2007, admitted non-foolproof.
- **SymDiff (CAV 2012):** a semantic diff is a *relational proof attempt* — compose both versions
  into one program, try to prove "equal inputs ⟹ equal outputs" (mutual summaries as the declared
  relational equivalence; differential invariants inferred Houdini-style as cutoff certificates).
  Equivalence is the cheap desirable outcome; **the diff's content materializes only on failure**,
  as a counterexample input pair plus highlighted divergent traces — backward attribution with a
  witness. Differential Assertion Checking is the cheap asymmetric variant: prove the new version
  is *no worse than* the old (the old version is an implicit spec) — regression-shaped, no
  absolute spec needed. Admitted boundary: relational proofs need structural alignment; they
  excel on evolutionary edits and degrade on rewrites.
- **Metamorphic relations** (Chen et al., CSUR 2018) are declared transport laws usable when no
  oracle exists: a necessary relation over a *tuple* of runs — input transformation + expected
  output relation, where the expected relation is often **directional** (subset, ≤, known
  algebraic shift), not equality. MRs compose along pipelines (composite MRs empirically
  out-detect components); one well-chosen relation found 100+ compiler bugs (EMI). The unsolved
  part the survey names: finding good MRs is domain-manual, and MR quality is measurable only
  empirically (mutation kill rate) — a declared equivalence's discriminating power cannot be
  known a priori.

**Kernel take:** three diff modes the kernel should distinguish — *computed* (same carrier,
matched identity), *transported* (template diff + soft correspondence onto a sibling carrier),
and *refuted* (equivalence proof attempt whose failure carries the attribution witness). The
second is the JIT/mockup workflow's natural mechanic; the third is what "did this change behavior?"
should mean for deterministic stages.

---

## Finding 6 — The noise discipline for stochastic nodes exists in pieces, and the pieces assemble

No single system has it, but the components are each production- or paper-validated:

1. **Learn the noise floor from a control channel; never hand-write the noise model.** Diffy runs
   the known-good version *twice* (primary + secondary): their disagreement IS the empirical
   noise distribution, measured live on the same inputs; only candidate-vs-primary disagreement
   *in excess of* that baseline is signal. Frequencies are aggregated over the whole request
   history — never judged per single run. Kohavi's A/A tests and Sample-Ratio-Mismatch checks are
   the same move (validate the diff apparatus before trusting any delta), and Twyman's law is its
   prior: "any figure that looks interesting is usually wrong" — a surprising delta indicts the
   instrumentation first.
2. **Gate on repetition stability before trusting any difference.** The position-bias study
   (pairwise LLM judges) imposes repetition consistency > ~0.85 as an *admission gate* — below
   it, differences are indistinguishable from sampling jitter. Its other transferables: declared
   invariance (same verdict under input permutation) is a measurable equivalence; consistency
   does NOT imply fairness (a node can be order-stable yet directionally biased — equivalence
   tests must be multi-dimensional); and the hardest regime is near-equivalent inputs (quality
   gap ≈ 0), where nuisance perturbations dominate and attribution should be flagged
   low-confidence. Notable nulls: verbosity bias largely dissolves when quality is controlled;
   temperature does not drive flip-rate.
3. **Decompose variance to separate signal from noise.** The ICC paper: ICC = σ²_between /
   (σ²_between + σ²_within); a config change is trustworthy only if **both** the mean and the
   reliability improve (mean alone can be lucky sampling). The budget law: for fixed B = n·T,
   variance is minimized by *more items, fewer trials* (wide-and-shallow; n=100×T=4 beats
   n=10×T=40 by 68% on SE). Reliability is per-scenario, not per-model (same model: ICC 0.774 on
   easy tasks, 0.629 on hard). Their "Evaluation Card" — never a bare number; always
   {estimate ± CI, ICC variant, n, T} — is the minimal provenance schema for any stochastic
   measurement.
4. **Three-valued verdicts; dual significance; sequential cutoff; fingerprint the trace.**
   AgentAssay: every stochastic comparison returns PASS / FAIL / **INCONCLUSIVE** over a
   confidence interval — never silent equality; a regression requires statistical (α) AND
   practical (δ) significance; SPRT stops sampling the moment the verdict is decided (sequential
   early cutoff for distributional deltas); and execution traces are compressed to ~14-dim
   behavioral fingerprints compared by Hotelling's T² — because **binary pass-rate diffing has 0%
   detection power for regressions that change the process but not the answer** (different tools,
   different cost, same output), which fingerprints catch at 86%. Trace-first storage makes 4 of
   6 test types recomputable offline at zero new LLM cost. Honest core trap, admitted: treating
   "no statistically detected difference" as equivalence — the equivalence is only as honest as
   the noise model and sample size behind it.
5. **Attribution without ground truth = majority vote across mappings.** Csmith attributes a
   divergence to the minority compiler across N compilers × opt levels, with zero oracle — N-
   version voting transfers directly to LLM pipelines. Its discipline transfers too: the
   equivalence class is enforced at *generation* time (inputs confined to well-defined behavior;
   conservatism costs coverage — CsmithEdge found bugs the conservatism hid), reduction steps
   must re-validate class membership or attribution lands on a phantom cause, and **"no diff
   found" is always relative to the input distribution tested** — distributional equivalence
   claims must state their distribution.

**Kernel take:** the stochastic-node diff protocol assembles as: control-channel noise floor
(run baseline twice) → repetition-stability admission gate → wide-and-shallow trial allocation →
three-valued verdict with α + δ → SPRT for cost → trace fingerprint alongside output → evaluation-
card provenance on every stored measurement. All components published; the assembly is not.

---

## Finding 7 — The LLM-eval platforms are end-state diffing with no delta object and (mostly) no statistics; the bridge to the propagation machinery is unoccupied

The production platforms (Braintrust, LangSmith) and the KDD '25 agent-eval survey describe the
same architecture: materialize two full runs (experiments over immutable dataset versions), join
rows on input equality, diff per-example scores. Within that frame they contribute real pieces:
the **evaluator as the pluggable declared equivalence** (code/heuristic → embedding → LLM-judge →
pairwise → human, an explicit equivalence ladder); immutable, timestamp+tag-addressable dataset
versions (`as_of=` replay); pairwise comparison when absolute scoring is unreliable; trials with
mean+SD. And the gaps are stark: **Braintrust has no significance testing anywhere** (no CI, no
p-values — "run trials and average" is the entire noise story, so sub-noise deltas are reported
as regressions); LangSmith has no forward propagation, no cutoff, no structural attribution —
cost is full reruns × repetitions; the survey concedes the field's only distributional tool is
pass@k / pass^k (no distribution distances, no variance bounds), attribution is trajectory-vs-
hardcoded-gold, and **no surveyed system records seeds, dataset versions, or formal provenance**.

Meanwhile the propagation machinery (Findings 1–3) is deterministic-only: every rebuilder,
trace, and cutoff assumes same-inputs ⟹ same-output, and non-determinism breaks them outright
(verifying traces degrade to perpetual rebuild; constructive traces serve wrong cached values).
Build Systems à la Carte names the exact wall: you cannot reuse content-trace machinery on a raw
LLM node **until you quotient out sampling noise first** — i.e., Finding 6's protocol is the
missing adapter between Finding 1's laws and Finding 7's practice. Nobody in the haul connects
them. One more transferable from the build-systems side: **Applicative vs Monad tasks** = static
vs dynamic dependencies — a fixed prompt-template pipeline is Applicative (graph knowable before
running; plannable, cuttable), an agent that picks its next tool from intermediate output is
Monad (the graph is discovered by running; you can only record what it actually touched). Declare
which kind each pipeline stage is; they have different diff stories.

**Kernel take:** Softland's diff layer occupies the seam: delta-transport laws and trace/cutoff
machinery from the deterministic tradition, with stochastic nodes wrapped in the Finding-6
protocol so they present a lawful "quotiented" surface (distributional equivalence + verdict) to
the propagation graph. No existing system sits there.

---

## Finding 8 — The counterfactual register has exact laws, a decidable boundary, and an empirical prior

- **Two different diffs hide under "what if"** (de Lara 2023): the structural counterfactual
  (do-intervention, re-run the model, descendants re-propagate — *total* effect) and the
  potential-outcome (condition on observed covariates, descendants pinned — *direct* effect).
  They coincide **iff the intervened node has no output-relevant descendants whose distribution
  shifts** — a graph-topology test, statically checkable. Conflating them is systematically wrong
  on exactly the interesting cases (upstream edits with downstream descendants). Also: equality-
  in-law (distributional) and almost-sure equality (per-run, coupled) are different equivalence
  relations — a null distributional diff can coexist with nonzero per-run counterfactual diffs;
  per-run counterfactuals require the stored exogenous noise (Finding 2).
- **Value of Information** (Howard lineage): VOI = E[max] − max[E] ≥ 0, zero exactly when no
  decision flips (the cutoff law, decision-relative); EVPI ≥ EVPPI ≥ EVSI gives a cheap dominating
  bound — if total EVPI is below threshold, no partial investigation is worth running (a pruning
  rule for which strategy-diffs to even compute). VOI is order-invariant in total but **not
  additive** across causes — per-cause attributions must not be reported as a linear
  decomposition (interaction terms are real; Shapley-style joint attribution or nothing). The
  practical estimator collapses nested Monte Carlo via regression metamodels — valid only under
  a declared fit-quality gate (store R²/residuals with the attribution; fall back to nested
  simulation when the surrogate fails).
- **Online controlled experiments** (Kohavi, Tang, Xu 2020): randomized control = the
  counterfactual baseline made empirical; the OEC is the declared equivalence (the one quantity
  whose difference counts); significance + power + CI is the noise model; SRM and A/A tests are
  integrity checks on the diff apparatus itself. The empirical prior transfers whole: **roughly
  ⅓ of changes are positive, ⅓ flat, ⅓ negative** — most deltas die, and intuition cannot
  predict which (Bing's +$100M/yr change sat deprioritized for six months; effect sizes are
  heavy-tailed). Deltas also have *temporal profiles* (primacy/novelty inflate early readings;
  carryover contaminates later ones) — "propagate dA to dB" must state its horizon.

**Kernel take:** a strategy diff is two future-configurations + a shared **outcome model that is
itself a versioned artifact**, with: the total-vs-direct choice declared per comparison (and the
graph test recorded), seeds/noise stored when per-run counterfactuals are wanted, VOI as the
prioritization operator over which diffs to resolve, and Twyman's-law skepticism wired in as
default posture toward surprising deltas.

---

## Ranked relevance to the Softland diff layer

1. **Delta lenses + change actions + ILC + edit lenses** — the four-law contract (dead-delta,
   round-trip, chain rule, from-scratch consistency), deltas-as-monoid storage schema, function-
   changes, complements. The formal spine; the rent test passed before implementation.
2. **Build Systems à la Carte + Salsa** — the rebuilder ladder (provenance stored → capability
   bought), self-tracking rules, Applicative/Monad dependency split, durability vectors, the
   verify-cost lesson, cutoff-shield engineering. The closest engineering playbook.
3. **Diffy + ICC + AgentAssay + position-bias** — the assembled stochastic protocol (control
   channel, repetition gate, variance decomposition, three-valued verdicts, dual significance,
   SPRT, trace fingerprints, evaluation cards). The adapter Softland must build.
4. **VisTrails** — change-based provenance validated in production; diff-by-inverse-replay with
   cancellation; the analogy operation (transported diffs); purity contracts with non-cacheable
   flags. The closest whole-system ancestor.
5. **Differential dataflow** — deltas-as-primary at substrate level; logical vs physical
   compaction (governed forgetting); frontiers as commit boundaries. Conceptually closest to the
   Rama layer itself.
6. **de Lara + VOI + Kohavi** — the counterfactual register's laws, boundary conditions, pruning
   bound, and empirical priors.
7. **SymDiff + metamorphic testing + Csmith** — proof-shaped diffs, relations-as-transport-laws,
   majority-vote attribution, equivalence-class discipline under reduction.
8. **nbdime + diffoscope** — recursive typed diff trees, pluggable/graded equivalence predicates,
   render-then-diff (equivalence lives in the projection; the diff operator is dumb), matching
   silently failing = phantom deltas.
9. **Acar + Adapton + Jane Street Incremental** — DDG/DCG mechanics, trace stability, demand-
   driven scoping, nominal identity as the enabler of reuse, the production cost ledger
   (4–8× recording overhead, ~30ns/node, seven rewrites).
10. **LangSmith/Braintrust/KDD survey** — the current practice ceiling and its named gaps (no
    statistics, no provenance, no delta object); the negative space that defines the opportunity.

---

## Open problems the literature itself flags (nobody has solved these)

- **Stochastic × incremental:** no system connects delta-propagation/cutoff machinery to
  distributional equivalence at stochastic nodes — the build-systems papers name the wall, the
  eval platforms ignore it. (The central open seam; Softland's opportunity.)
- **The distributional delta as an object:** everyone aggregates to scalars (means, pass@k);
  nobody stores or transports a distribution-valued delta. Graded similarity is computed then
  discarded even where it exists (diffoscope's TLSH distances).
- **Attribution for difference/negation:** provably cannot ride on semiring provenance
  (monus impossibility); retraction-bearing pipelines need a separate, weaker story.
- **Equivalent-mutant / absence-of-evidence:** "no detected difference = equivalence" is
  undecidable in general and silently masks rare or sub-threshold deltas (AgentAssay admits it;
  MT has no a-priori MR-quality metric).
- **Uniform low-amplitude drift** defeats tolerance-band equivalences (the 8px→6px corner-radius
  case): a globally consistent small change is indistinguishable from per-region noise.
- **Complement/trace growth:** lens complements compose multiplicatively; traces and arrangements
  grow with history without governed compaction; memo eviction is unsolved in SAC/Adapton.
- **Judge noise:** when the evaluator is an LLM, its own variance is an uncounted second noise
  source (ICC paper admits conflation; survey notes zero bias accounting in practice).
- **Structural distance breaks relational diffing:** SymDiff-style proofs and VisTrails-style
  soft matching both degrade as the two versions' structures diverge — rewrites vs edits remain
  qualitatively different regimes.

---

## People index (the non-famous ones worth knowing)

Juliana Freire, Cláudio Silva, David Koop, Carlos Scheidegger (VisTrails); Todd Green, Grigoris
Karvounarakis, Val Tannen (provenance semirings); Zinovy Diskin (delta lenses; with Xiong &
Czarnecki); Michael Johnson & Robert Rosebrugh (lenses ↔ opfibrations); Mario Alvarez-Picallo &
Luke Ong (change actions); Yufei Cai & Paolo Giarrusso (ILC); Martin Hofmann (†), Benjamin
Pierce, David Wagner (symmetric/edit lenses); Umut Acar (self-adjusting computation); Matthew
Hammer (Adapton; nominal names); Frank McSherry (differential dataflow); Andrey Mokhov, Neil
Mitchell, Simon Peyton Jones (build systems à la carte); Yaron Minsky (Jane Street Incremental);
Shuvendu Lahiri & Chris Hawblitzel (SymDiff); T.Y. Chen (metamorphic testing); John Regehr,
Xuejun Yang (Csmith); Lucas de Lara (potential outcomes vs do-interventions); Ronald Howard
(VOI); Ron Kohavi, Diane Tang, Ya Xu (trustworthy experiments); Puneet Mustahsan et al. (ICC for
agentic evals); Varun Pratap Bhardwaj (AgentAssay).

---

## Sources (quality as rated by the extractors)

Primary: VisTrails / AOSA vol.1 <https://aosabook.org/en/v1/vistrails.html> ·
Provenance Semirings, PODS 2007 <https://web.cs.ucdavis.edu/~green/papers/pods07.pdf> ·
nbdime docs <https://nbdime.readthedocs.io/en/latest/index.html> ·
DVC pipelines + metrics diff docs <https://doc.dvc.org/user-guide/pipelines/defining-pipelines> ·
Delta lens (nLab) <https://ncatlab.org/nlab/show/delta+lens> ·
Change Actions, FoSSaCS 2019 <https://arxiv.org/abs/1902.05465> ·
ILC / Theory of Changes, PLDI 2014 <https://arxiv.org/pdf/1312.0658> ·
Symmetric Lenses POPL 2011 / Edit Lenses POPL 2012 <https://www.cis.upenn.edu/~bcpierce/papers/symmetric.pdf> ·
Acar, Self-Adjusting Computation (CMU thesis 2005) <https://www.cs.cmu.edu/~rwh/students/acar.pdf> ·
Adapton, PLDI 2014 <https://www.cs.tufts.edu/~jfoster/papers/cs-tr-5027.pdf> ·
Salsa "Durable Incrementality" (2023) <https://rust-analyzer.github.io/blog/2023/07/24/durable-incrementality.html> ·
Differential Dataflow book — Arrangements <https://timelydataflow.github.io/differential-dataflow/chapter_5/chapter_5.html> ·
Build Systems à la Carte, ICFP 2018 <https://www.microsoft.com/en-us/research/wp-content/uploads/2018/03/build-systems.pdf> ·
Diffy README (twitter-archive) <https://github.com/twitter-archive/diffy> ·
SymDiff, CAV 2012 <https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/paper-44.pdf> ·
Metamorphic Testing survey (CSUR 2018 / HKU TR) <https://www.cs.hku.hk/data/techreps/document/TR-2017-04.pdf> ·
Csmith, PLDI 2011 <https://users.cs.utah.edu/~regehr/papers/pldi11-preprint.pdf> ·
diffoscope <https://diffoscope.org/> ·
Position bias in pairwise LLM-judge (arXiv 2406.07791) <https://arxiv.org/html/2406.07791v5> ·
ICC for agentic evals (arXiv 2512.06710) <https://arxiv.org/pdf/2512.06710> ·
AgentAssay (arXiv 2603.02601) <https://arxiv.org/pdf/2603.02601> ·
Braintrust comparing-experiments docs <https://www.braintrust.dev/foundations/comparing-experiments> ·
LangSmith evaluation concepts <https://docs.langchain.com/langsmith/evaluation-concepts> ·
de Lara, potential outcomes vs do-interventions (arXiv 2309.05997) <https://arxiv.org/abs/2309.05997>.
Secondary: LLM-agent eval survey, KDD '25 (arXiv 2507.21504) <https://arxiv.org/html/2507.21504v1> ·
Value of Information (Wikipedia + EVPI + Jalal et al., via hops) <https://en.wikipedia.org/wiki/Value_of_information> ·
Kohavi/Tang/Xu ch.1 (reconstructed from quoting secondaries; PDF binary) <https://experimentguide.com/> ·
Jane Street "Introducing Incremental" (blog) <https://blog.janestreet.com/introducing-incremental/>.
Blog/vendor (weak): qtrl.ai visual regression post (2026) — noise taxonomy + uniform-drift blind spot only.

---

## Implications for the Softland diff layer (graded; not a design)

**Strong (multiple independent sources, no contradictions):**

- **The diff contract is four laws**: dead-delta, round-trip, composition, from-scratch
  consistency. State them in the kernel contract; they are the acceptance tests for any
  propagation/cutoff/caching machinery (Finding 1).
- **A delta is a first-class container**: typed against its source revision, carrying its declared
  equivalence, its chosen propagation rule, and provenance. No subtraction assumption — deltas
  are stored, not recovered from snapshots (Findings 1, 4).
- **Attribution is provisioned at write time**: traces/correspondences/seeds recorded per the
  rebuilder-ladder rung the workflow declares; minted container identity is the correspondence
  backbone (Finding 2).
- **Per-node declared equivalence registry** with volatility tiers and engineered cutoff shields;
  equivalence lives in the projection (render-then-diff), the diff operator stays dumb
  (Findings 3, 5).
- **Stochastic nodes get the assembled protocol**: control channel, repetition gate, three-valued
  verdicts, dual significance, wide-and-shallow trials, trace fingerprints, evaluation-card
  provenance (Finding 6). This is the adapter that lets stochastic stages participate lawfully in
  an otherwise deterministic propagation graph (Finding 7).

**Medium (good precedent, one source family, or known costs):**

- Logical-vs-physical compaction as the kernel's history-coarsening model (differential dataflow).
- Transported diffs (VisTrails analogy) as the mechanic for "apply this change to that sibling
  pipeline/mockup" — soft matching required, non-foolproof by design, decision-grade output.
- Proof-shaped diffs (SymDiff/DAC posture) for deterministic stages: try to prove no-difference;
  materialize the witness on failure; "no worse than old" as the cheap regression question.
- Applicative/Monad declaration per pipeline stage (static vs discovered dependencies) deciding
  plannability of forward diffs (Build Systems à la Carte).
- Strategy diffs as configurations + versioned outcome-model artifacts, with the total-vs-direct
  choice declared and VOI as the prioritization operator (Finding 8).

**Speculative (single source or analogical):**

- Behavioral fingerprints (trace-shape vectors) as a standard projection stored alongside every
  agent-pipeline run (AgentAssay; one paper).
- Durability version-vectors generalized to the whole world graph (Salsa; one system).
- A "diff card" UI norm: every displayed delta carries its equivalence, sample size, and verdict
  class — never a bare number (ICC evaluation card, generalized).
