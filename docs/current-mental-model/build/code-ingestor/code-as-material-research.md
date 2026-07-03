# Code as Material — Unit, Identity Threads, Reference Graph, and the Three Registers

**Research synthesis, 2026-06-12.** Question: when a CODE ingestor turns a Clojure/ClojureScript
codebase into kernel objects (containers + revisions + source anchors + composition edges +
projections + decisions), what is the atomic unit of code-as-material; how does a function-grained
unit keep identity across edits, renames, moves, splits; how should the reference graph be
represented when functions are first-class versioned objects composed into versioned groups; and
how do the three versioning registers — imported git history, native in-loop edit history,
emergent semantic versions minted from activity logs — relate? Hard constraint: files keep
round-tripping so the codebase still builds with ordinary tools.

**Calibration:** 27 sources mined, 216 lessons, extraction by Opus 4.8 background agents,
synthesis by Fable. **NO adversarial verify pass — cut by request.** Date 2026-06-12. Several
primary PDFs were paywalled or bot-blocked (CodeShovel, CodeTracker deck, RefactoringMiner TSE,
SmartCommit, Flexeme, Negara ICSE14); extractors reconstructed mechanism from the authors' own
repos, companion papers, and docs and flagged this — treat exact benchmark numbers from those
sources as paper-reported, not re-verified. Companion doc: `versioning-paradigms-research.md` —
this report uses its vocabulary (identity/value/place, OID/VID, bound vs generic references,
composition Models A–D, version proliferation, the version-count wall) by reference, without
re-explaining it.

---

## Finding 1 — The atomic unit: the top-level form earns it, but as *form*, not *function* — and the file does not disappear

Five independent systems converged on definition-grain as the unit of code-as-material: Unison
terms (Chiusano 2015), codeq's "code quantum" = the top-level form, defined for Clojure
specifically (Hickey 2012), ENVY's method-always-an-edition, auto-versioned on every accept (OTI,
via Sharp 1997), Epicea's method/class-grained change operations (Dias et al. 2013–15), and
CodingTracker's AST-node operations with persistent IDs (Negara et al. 2012/2014).
EpiceaUntangler and SmartCommit work at edit-operation/diff-hunk grain but immediately map those
onto declarations to get any semantic signal. The granularity *floor* comes from SCIP's
accessibility rule: only entities reachable from outside the document get global identity —
locals get document-scoped throwaway ids (SCIP — Sourcegraph 2022; kondo's locals `:id` carries
no stability guarantee). The top-level form is the smallest world-identity-bearing code object.

The honest counterexample: Software Heritage, at planetary scale, deliberately stops at the
whole-file blob — sub-file addressing is a fragile `lines=` qualifier that breaks when
surrounding lines shift (SWHID docs 2021). File grain is *viable*; the reason to go finer is the
workflows codeq named ("what's the history of this function definition?"), not necessity.

**Where function-as-unit hurts — the complications, named:**

1. **Not every top-level form defines anything.** codeq's analyzer keys off forms starting with
   `def`; the rest (side-effecting forms, `comment` blocks, the `ns` form) are forms without
   definitions — and one form can mint several vars (`defrecord`/`defprotocol` — background,
   unfetched). Unit = form; definition = analysis *output*. Don't conflate them.
2. **`defmethod` and protocol impls break one-form-one-object.** clj-kondo models a multimethod
   as ONE var-definition (`defmulti`) plus N var-usages tagged `:defmethod true` +
   `:dispatch-val-str`, and protocol impls as separate `:protocol-impls` records (clj-kondo docs
   2025). One logical object spans many forms, possibly many files — needs a head-container +
   contribution-containers model, not a flat form list.
3. **Ordering is load-bearing.** tools.namespace reloads out of dependency order throw
   `No implementation of method` for protocols/multimethods; unload must be sorted against the
   OLD graph and load against the NEW one (tools.namespace source; rymndhng 2018). A Clojure file
   is an *ordered sequence* of forms; a bag-of-functions model cannot round-trip a buildable file.
4. **Inter-form material needs an owner.** rewrite-clj achieves losslessness precisely by making
   whitespace, commas, and comments first-class nodes; the bytes between top-level forms must
   belong to something — the file container. ENVY's file/make support arrived as a later 3.01
   bolt-on (the bridge is real work), and Epicea's replay-to-files shipped order-sensitivity bugs
   (Pharo issues 4875/4876).
5. **cljc reader conditionals: one form, two meanings.** clj-kondo emits per-branch entries
   tagged `:lang` with no merge story; rewrite-clj keeps both branches losslessly but `sexpr`
   collapses to one resolution.

So hunch 1 passes — *and* hunch 2's "groups are versioned compositions of versioned members"
arrives with a correction: the file/namespace is not merely a composition rule over members
(Model A); it is itself a container with its own material (ordering, trivia, the `ns` form) that
no member owns.

**Kernel take:** containers at two grains — form-grained units AND the file/namespace as an
ordered composition owning what forms don't — with SCIP's accessibility rule as the identity
floor (locals never get world identity).

---

## Finding 2 — Identity: the content hash is a value key, never a thread; every working system runs at least two identity registers

The single most repeated finding in the haul, and the direct answer to hunch 1's open question
("how is identity attached to functions?"):

- **Unison** — the canonical content-addressed code system — does NOT preserve function identity
  across edits. Every body edit mints a new hash; "the function-as-evolving-thing has no stable
  hash at all." Continuity is reconstructed entirely in the mutable name→hash layer plus the
  `update`/patch machinery (Chiusano 2015; Unison blog ~2023; Chiusano 2020). Renames are free;
  edits cascade re-hashing through the transitive dependent set, and type-changing edits cannot
  propagate at all (branch + scratch-file ritual).
- **codeq** needed TWO `:db.unique/identity` attributes on code: `:code/sha` (whitespace-minified
  content) AND `:code/name` (fully-qualified symbol). The cross-edit timeline of a function keys
  on the NAME, not the SHA (Hickey 2012; codeq repo). codeq is the identity/value/place triple
  rendered as Datomic attributes: `:code/name` ≈ thread, `:code/sha` ≈ value, `:codeq`
  (file + loc) ≈ place.
- **Kythe** makes named entities' identity their qualified-name-derived VName signature and
  explicitly *forbids* source locations in it; structural hashing is only the fallback for
  anonymous entities (Kythe docs). **Glean** keys facts by structural KEY with the volatile
  payload in the value — deliberately so a body edit keeps identity (Marlow 2022). **SCIP** uses
  descriptor-path strings. All name-register systems sever on rename.
- **SWH** splits intrinsic (recomputable-from-bytes) identity from extrinsic qualifiers
  (origin/path/lines) that never alter the core ID (SWHID 2021).

This is the versioning doc's Finding 1 (three pure identity strategies) instantiated in code,
with a code-specific sharpening: **no fetched system obtains identity-through-edit from content
addressing** — the one that leans hardest on hashes (Unison) explicitly inverts the question
("don't: track continuity in a names/history register layered over immutable hashes"). Hash
systems make rename free and edits expensive; name systems make edits free and renames fatal;
only minted IDs (strategy 1 — MolhadoRef, CodingTracker's persistent node IDs) survive both.
Normalization belongs to the *value* register: codeq's ws-minify makes reformatting
identity-invariant; alpha-hashing (Maziarz, Ellis, Lawrence, Fitzgibbon, Peyton Jones 2021) drops
bound names, keeps free names (which are exactly the reference-graph edge set), runs O(n log²n),
is incremental, and is collision-safe at 128 bits — a dedup and equivalence layer, never the
thread.

**Kernel take:** the container id is minted (strategy 1); the FQN and the normalized content hash
are revision *attributes/indexes* — never derive the thread from either.

---

## Finding 3 — Continuity is acquired in two modes, declared vs reconstructed — and the registers are asymmetric because only the in-loop register can declare

**Declared:** Epicea records a rename as a typed first-class event carrying BOTH endpoints plus
the entity (`EpBehaviorNameChange`: oldName, newName, behavior); refactorings are reified events
whose fan-out edits point back via `#trigger` tags (Epicea repo/papers). CodingTracker assigns
persistent IDs to AST nodes at edit time and maintains a position→ID map across edits (Negara et
al.). MolhadoRef (versioning doc, Finding 1) is the same stance: declared by the refactoring
engine, never inferred by diffing. ENVY auto-records every accept. Declaration is exact and
cheap — and only possible where the editing instrument is yours.

**Reconstructed:** the entire method-history tool family re-derives identity per query by
matching — HistoryFinder's signature→body-similarity cascade, CodeShovel's similarity walk,
CodeTracker's refactoring-aware cascade, GumTree's two-phase mapping, RefactoringMiner's
replacement search. None stores a stable ID; identity is an *emergent property of a matching
walk*, probabilistic and config-dependent (GumTree's mapping shifts with its three
hyperparameters; re-tuning changed 21.8% of edit scripts — Falleri et al. 2014 + follow-ups).
Reconstruction is the ONLY option for history you didn't record.

Two structural requirements fall out. (a) **Move/split/merge must be first-class N:M
operations:** CodingTracker's admitted failure is move modeled as delete+add — the thread dies
(old ID dies, new ID born). GumTree's 1:1 node mapping structurally cannot represent a split;
RefactoringMiner's multi-mappings (N fragments → 1 extracted method) and CodeTracker's lineage
graph *with forks* are what expressing split/merge actually takes (Tsantalis et al. 2020; Jodavi
& Tsantalis 2022). A simple prior-revision pointer cannot carry this; lineage edges need
cardinality. (b) **The two modes differ in epistemic status:** a declared rename is a fact; a
reconstructed one is an inference bounded by its detector — which maps directly onto the kernel's
decision/audit noun.

**Kernel take:** identity-continuity edges are typed N:M events — declared facts in-loop, minted
defeasible decisions (with detector provenance) on import.

---

## Finding 4 — Recovering threads from existing git history: what the numbers say

The asker's framing ("code arrives with three structure layers already built") is true for git's
trees/refs and **false for function identity** — that layer must be computed on top, and the gap
is measured:

- AST-aware tracking: HistoryFinder 97.4–99.1 F1; CodeTracker 99.97 P / 100 R (methods), ~99.8
  (blocks); RefactoringMiner 99.6 P / 94 R on a 7,226-instance oracle — with NO similarity
  thresholds at all (exhaustive syntactically-valid AST replacements until byte-identical,
  validated for consistency across the type; faster than threshold tools, ~60 ms/commit in v3)
  (Islam et al. 2025; Jodavi & Tsantalis 2022; Tsantalis et al. 2020).
- Git-native tracking: GitFuncName 51.7–61.5 F1; GitLineRange 59.8–69.6; `git log -L` 73.7 P /
  83.5 R, and it "prematurely ends the tracing process when the methods in a file get
  reorganized." A ~30-point cliff.

Caveats that transfer directly: all of this is Java. Similarity thresholds break under
rename+rewrite+move in one commit (CodeShovel — Grund et al. 2021). Merge commits require
per-parent comparison or you fabricate history (CodeTracker's documented wrong-parent bug).
Splits/extractions are punted even by the 99-F1 tools — HistoryFinder records a split as a
brand-new method; recovering it costs RefactoringMiner-class analysis (~4× mean runtime).
Parsing dominates cost (80%+ of runtime) and fails outright on broken syntax. Change-KIND typing
matters enormously: excluding formatting/annotation noise changed 40 of 200 oracle histories
(+59 real commits, −186 spurious). And ground truth is human-expensive (~300 h/reviewer for 200
methods) — even a Distinguished-Paper oracle was materially wrong (CodeShovel corrected from 99 P
to ~88.6 F1 on the fixed oracle).

For Clojure: **no RefactoringMiner-class tool exists** (noted in the CodeTracker extraction).
RefactoringMiner's threshold-free *strategy* is language-agnostic but its engine is not. So v1
import realistically lands in the codeq tier: exact-content matching (ws-minified SHA) catches
moves and reformatting for free; name matching gives per-FQN timelines; renames and splits are
simply *unrepresented* unless a matcher pass or a human/LLM decision adds them later.

**Kernel take:** v1 import mints name+content timelines (codeq tier), records rename/move/split
threads only as low-confidence decisions or not at all, and leaves the upgrade path open for a
later matcher pass — never bake reconstructed lineage in as truth.

---

## Finding 5 — The reference graph: name-keyed, role-typed, late-bound edge facts as a derived projection; hash-binding reserved for freeze points

Production consensus is unusually clean. clj-kondo encodes the call graph as a flat edge list on
var-usages — callee = symbolic `(:to, :name)`, caller = `(:from, :from-var)` (caveat:
`:from-var` is a bare name; qualify it with `:from` yourself) (clj-kondo docs 2025). Kythe stores
`(source-VName, kind, target-VName, fact, value)` entries; merging two analyzers' output is set
union; cross-file resolution works with no linker because every indexer derives the same VName
from the qualified name by convention (Kythe docs). SCIP names targets by human-readable symbol
strings inside self-contained per-file documents, stores edges forward-only, and explicitly
refuses the reverse index — "bidirectional lookup is best served by a query engine"; its
predecessor LSIF's opaque global numeric IDs are the documented failure mode (repo-wide poisoning
from off-by-one bugs, no incremental updates) (Sourcegraph 2022). Glean models edges as facts
referencing facts in one homogeneous store (Marlow 2022).

All of these are **generic (late-bound) references** in the versioning doc's sense: the edge
names a symbol; resolution against a concrete revision happens at query/projection time. The one
bound-pole system is Unison — hashes literally stored in the syntax tree — which buys an
immutable dependency DAG and per-hash caching forever, and pays the transitive update cascade
plus name degradation ("easy to accidentally replace your human readable names with mysterious
hashes"). ENVY shows binding as a *separate deliberate act*: a class version must be RELEASED
into an application's load set — "committed" and "part of the build" are different facts — and
config maps pin {group → version} sets as first-class versioned objects (Katz's
configuration-as-object, versioning doc Finding 2). Polylith adds the interface indirection:
edges target an interface identity; the concrete component is bound per project via an
`ifc->comp` map (Polylith docs).

Three transferable details. (a) **Edge taxonomy:** SCIP's role bitset (Definition/Import/Read/
Write/Generated/Test), SmartCommit's typed edges with a structural-vs-reference flag
(CONTAIN/DEFINE vs CALL/ACCESS/IMPORT…), Flexeme's name-flows — annotate the edge with the NAME
flowing across it; that lexical tag is what lets related edits cluster later. (b) **Direction:**
tools.namespace materializes BOTH adjacency maps because the load-bearing query is the *reverse*
transitive closure (affected-set); its two-graph discipline (teardown sorted by the OLD graph,
rebuild by the NEW) is required whenever the dependency structure itself changed. (c)
**Incompleteness:** parse-derived graphs systematically miss things — tools.namespace sees only
static `ns` clauses (dynamic requires invisible); kondo loses locations on macro-expanded usages;
and codeq shipped with NO uses at all. The schema anticipated def+use codeqs sharing codenames,
but body-walking + alias resolution + macroexpansion never landed: the analyzer is the hard 80%,
the schema the easy 20% (codeq repo/wiki).

For multi-version *views*, Flexeme's move is elegant: one graph holding both versions with
version-labeled nodes/edges, projected by filtering — the graph analog of
log-primary/state-is-projection (Pârțachi et al. 2020).

**Kernel take:** reference and composition edges are role-typed facts emitted per source
revision, targeting container identities by name; revision-binding is computed at
projection/freeze time (generic by default, bound on demand — ENVY's release); the reverse index
is a projection, never stored truth.

---

## Finding 6 — The import register: git is evidence, not ground truth

The fidelity hierarchy is inverted from the naive view. CodingTracker's measurement: **37% of
real code changes are shadowed** — overwritten before ever reaching VCS — and 78% of those are
authentic work, not undo churn; within commits, 58% of methods are both refactored AND
hand-edited, so a snapshot diff blends structural moves with semantic edits into one
indistinguishable hunk (Negara et al. 2012/2014). EpiceaUntangler's framing is the same: each
line edit shadows prior edits; intermediate states are unrecoverable from commits (Dias et al.
2015). Git is a lossy down-sample of the edit history — but for pre-Softland code it is also all
that survives, so import = best-effort reconstruction over a lossy record, not communion with
truth.

Import mechanics with precedent — the codeq pattern (Hickey 2012; codeq repo): import git's model
**intact** (commits/trees/blobs as first-class entities, `:commit/parents` cardinality-many — the
DAG, matching Softland's semantic-time-is-a-DAG stance, `:git/sha` as universal identity;
multi-repo import conflict-free purely from content-addressing). Analysis is a SECOND pass over
imported objects, provenance-stamped: `:tx/op` (:import vs :analyze), `:tx/analyzer`,
`:tx/analyzerRev` — re-running a newer analyzer is a distinct, attributable layer, the precedent
for versioning *derivations* separately from material. Per-construct timelines are DERIVED by
query (name + commit-DAG join), not stored as version chains — log-primary, validated in 2012.

Two warnings from SWH: own your canonical serialization (git hash compatibility is "incidental
and not guaranteed"); and git lacks a whole-repo observation object — SWH had to invent the
snapshot type. When the upstream model lacks a register you need, mint a new object type rather
than overloading commits (SWHID 2021). Polylith contributes the cheap human register on top: a
movable named marker (`stable-*` tag) defining the "since" window for change derivation.

**Kernel take:** ingest git objects as immutable facts under kernel-owned serialization; every
analysis pass writes provenance (analyzer + revision); everything derived from import is
re-derivable projection or decision, never truth.

---

## Finding 7 — The in-loop register: editions vs versions, coalescing, punctuation — and the noise tax

ENVY ran the two-tier model in production for ~30 years: every method accept auto-records an
**edition** (timestamped, continuous, no ceremony); a **version** is a deliberate named freeze;
and **release** is a third act — wiring a frozen version into the parent group's load set (Sharp
1997). The asker's in-loop/emergent split is literally ENVY's edition/version split, with release
as the composition-binding act hunch 2 needs. Mutability propagates *up* the containment tree by
auto-promoting containers to editions when a child changes — the copy-on-write-up-the-tree rule a
versioned-composition kernel needs (and the controlled form of the proliferation cascade the
versioning doc warns about: value-level propagation, no new identity threads).

Epicea adds the event-model detail: every IDE operation reified with before/after meta-model
snapshots (revert/redo from the log alone, no live image needed); undo/redo are themselves
events; `#prior` tags chain history and `#trigger` tags encode causal fan-out; and — the key
register-relation move — **Monticello commits are events IN the same fine-grained stream**,
tagged inline ("P version 1"). The coarse register is punctuation inside the fine register, not a
parallel timeline to reconcile (Dias et al. 2013–15).

Costs, measured: raw fine-grained streams overwhelm humans — EpiceaUntangler's deployed
developers complained intermediate-edit noise made the stream unusable without folding; the
versioning doc's version-count wall (21k versions = systems fail to load) is the hard ceiling on
revision grain. CodingTracker's answer at the front: coalesce ("glue") keystroke bursts into
coherent edits BEFORE inferring operations — and its gluing heuristics admittedly over-segment
sometimes. Mid-edit unparseable states are normal, not exceptional: parser-dependent inference
must degrade gracefully (HistoryFinder fails outright on broken syntax; CodingTracker emits
spurious/missing ops without a parsable neighbor state) — acutely relevant for Clojure, where
unbalanced parens mid-edit are the steady state. rewrite-clj supplies the edit mechanics:
`subedit` isolates a form's subtree so a one-form edit provably round-trips as a one-form diff.

**Kernel take:** in-loop revisions are coalesced bursts (editions) carrying a typed change-kind
(body/signature/rename/move/format/doc — the CodeShovel/HistoryFinder taxonomy); named freezes
and release-into-group are separate explicit acts; commits/pushes appear as punctuation events in
the same stream.

---

## Finding 8 — The emergent register: deterministic precedent everywhere; the LLM is a boundary-chooser and namer, not the mechanism

Pushback first, because hunch 3 calls this register "something newly unlocked by LLMs." The haul
says otherwise — the machinery is 2006–2021:

- **Epicea condense** computes the net semantic effect of an edit span deterministically (undo to
  the neutralised point, redo only surviving changes) — lawful compression of raw edits into a
  semantic delta, no model required (Dias et al.).
- **EpiceaUntangler** clusters fine-grained changes into tasks with 13 pairwise voters + a
  random forest + adaptive-gap dendrogram cutting: median 91.5% deployed success, trainable from
  ~200 changes (Dias, Bacchelli, Gousios et al. 2015).
- **Flexeme** partitions a version-labeled dependence graph with a Weisfeiler-Lehman kernel:
  0.81 accuracy, 32× faster than prior SOTA — on *synthetic* tangles (Pârțachi et al. 2020).
- **SmartCommit** runs max-weight-first union-find over hard (dependency) / soft (similarity,
  proximity) / cross-version (refactor, move) edges: 71–84% median, deployed to 83 engineers for
  9 months — and its edge taxonomy does double duty as the intent label (REFACTOR/FIX/FEATURE…)
  of the minted group (Shen et al. 2021).
- **Mylyn** folds the interaction log into per-element degree-of-interest (DOI = 1·selections +
  0.2·edits − 0.1·decay) and reifies a task context as a saveable, union/intersect-able artifact
  (Kersten & Murphy 2006).
- **Negara ICSE14** mines recurring change patterns from continuous operation streams (discovers
  patterns; does not narrate episodes).

What LLMs genuinely add is judgment at the boundaries and narration/naming — the segmentation and
net-effect machinery predates them. A deterministic fold should compute an episode's *content*
while the LLM chooses where episodes begin/end and what they mean. Two more results to keep:

- **Signal ranking surprise, replicated twice:** temporal/log-order adjacency BEAT the reference
  graph as a co-change signal. EpiceaUntangler's top voters were time-between-changes, ordered
  distance in the event stream, and same-class — the message-send/variable-access voters
  underperformed, to the authors' surprise. SmartCommit's accuracy rides its hard dependency
  edges plus position; its similarity math is naive averaging. Position in the append-only stream
  is a first-class feature.
- **Discipline:** interest/episode values are never stored on events (Mylyn is explicit); decay
  is displacement-based, not wall-clock; episodes reference log spans, never rewrite them; and
  every minted episode needs detector provenance (codeq's `:tx/analyzerRev`; GumTree's
  config-dependent mappings show why) — an episode minted by LLM-X-with-prompt-P must say so, or
  it cannot be honestly re-segmented later.

**Kernel take:** semantic versions are derived, provenance-stamped artifacts (decisions +
projections) over spans of the other registers — deterministic fold for net effect, LLM for
boundaries and names, re-segmentable without loss.

---

## Finding 9 — Clojure/ClojureScript feasibility: decomposition and round-trip are already paid for; identity, cljc, and canonicalization are net-new

**Paid:**

- **rewrite-clj** solves lossless round-trip by making trivia first-class nodes (round-trip =
  string-concat walk of the tree); `subedit` isolates per-form edits; it is what
  cljfmt/zprint/clojure-lsp already build on. The "still builds with ordinary tools" constraint
  is satisfiable by construction: on import the kernel never rewrites at all (codeq posture — git
  blobs stay canonical, the kernel is an additive index); in-loop, file emission goes through
  rewrite-clj.
- **clj-kondo** is the production decomposer codeq lacked: var-definitions / var-usages / locals
  / keywords with full spans, arities, `:defined-by`, defmethod + protocol-impl modeling,
  namespace deps — per snapshot. It is what clojure-lsp's index is built on.
- **tools.namespace** contributes the propagation algorithm (dual adjacency, reverse transitive
  closure, two-graph topo ordering) and the cautionary identity tale (rename = silent remove+add;
  its own `move` utility ships an ALPHA warning).
- **Polylith** contributes the grouping precedent (interface indirection, dependency graph
  derived purely from parsing `:require` forms, `stable-*` markers) and a counter-position worth
  respecting: components are deliberately UN-versioned ("living code" vs "frozen libraries"). Its
  identity-by-directory breaks on rename — confirming the weakness of name/place identity rather
  than refuting per-unit versioning — but its warning aligns with the proliferation finding:
  per-unit version *ceremony* is a real cost, and the mitigation is minting revisions
  automatically (ENVY-style), not manually.

**Net-new (no one provides it):**

- The identity layer. kondo's identity is bare `(:ns, :name)`; locals `:id` is snapshot-local;
  positions go stale after edits (rewrite-clj's maintainers say so); kondo guarantees neither
  determinism nor stable output ordering — canonicalize (sort) before hashing anything derived
  from it. The most widely-used structural-editing library in the ecosystem has no concept of
  form identity at all — strong evidence the kernel is building genuinely new machinery, not
  formalizing existing practice.
- The macro boundary. Macro-expanded usages carry no location in kondo; codeq punted on
  macroexpansion entirely; `:defined-by` recovers the defining macro but not expansion-generated
  edges. The reference graph under macros is systematically incomplete — record the
  incompleteness rather than pretending the edge set is total.
- cljc branch merging, the defmethod head/contribution model, and graceful degradation on
  unparseable mid-edit states (anchor at file-revision grain until the reader recovers).

**Kernel take:** the interpreter = clj-kondo (per-revision decomposition + edges) + rewrite-clj
(anchors + round-trip), with kernel-owned canonicalization, identity minting, and continuity
events layered on top — none of which can be inherited.

---

## Ranked relevance to the code-ingestor

1. **codeq (Hickey 2012)** — the direct ancestor: same language, same substrate posture (derived
   index over git, log-primary, timelines by query), the sha+name double identity, the codeq
   anchor entity, analyzer provenance. Its shipped gap (no uses) defines the remaining work.
2. **clj-kondo + rewrite-clj** — the two libraries that make the interpreter feasible *today*;
   also the sharpest list of what they refuse to guarantee (identity, determinism, positions).
3. **Unison (three posts)** — the bound-reference pole, names-as-metadata, the edit-cascade and
   name-degradation costs; proof that content-addressing answers a different question than
   identity.
4. **Epicea + CodingTracker (Negara)** — the in-loop register design: reified ops with
   before/after snapshots, declared renames, condense, VCS-commits-as-punctuation, the 37%
   shadowing result, the move=delete+add failure to avoid.
5. **RefactoringMiner / CodeTracker / HistoryFinder / CodeShovel / GumTree** — the reconstruction
   tier: the 30-point AST-vs-git gap, threshold-free matching, N:M multi-mappings, oracle costs,
   the Clojure tooling gap.
6. **Kythe + SCIP + Glean** — industrial graph representation: name-derived identity, anchors as
   indirection, edge facts with union merge, stacking + ownership algebra, and the unsolved
   incremental-derivation warning.
7. **ENVY** — edition/version/release as the production three-act model; config maps as
   first-class pinned compositions; ownership per edition-of-container.
8. **SWH** — the file-grain counterexample, own-serialization warning, snapshot-object invention.
9. **EpiceaUntangler + Flexeme + SmartCommit + Mylyn** — emergent-register baselines, the
   temporal-beats-structural signal result, episode-as-artifact discipline.
10. **tools.namespace + Polylith** — Clojure propagation/grouping reality and the
    deliberate-non-versioning counter-position.

---

## Open problems the literature itself flags

- **Split/extract/inline lineage** — punted by nearly every tracker (HistoryFinder records
  splits as new methods; GumTree cannot multi-map); recovered only by RefactoringMiner-class
  analysis at heavy cost; nothing exists for Clojure.
- **Incremental re-derivation of computed facts over layered stores** — Meta left the general
  case unsolved (Glean: "a hard problem that we'll return to probably next year"); search-heavy
  queries pay ~3× over stacks.
- **No cheap oracle for function lineage** — ~300 h/reviewer per 200 methods; published oracles
  were wrong. Reconstructed identity needs a repair loop, not a one-shot import.
- **Identity through unparseable intermediate states** — parser-dependent tools fail outright; no
  fetched system handles mid-edit gracefully.
- **The macro boundary** — no analyzer in the haul walks expansions with locations; the reference
  graph under macros is systematically partial.
- **Name-layer rot in hash-identity systems** — Unison's "mysterious hashes"; the human-readable
  projection must be actively maintained.
- **Semantic merge** (carried over from the versioning doc) — merging declared refactorings with
  ordinary edits remains the named frontier; nothing in this haul touches it.

---

## People index (the non-famous ones worth knowing)

Paul Chiusano (Unison); Martín Dias, Stéphane Ducasse, Damien Cassou (Epicea; Dias also
EpiceaUntangler with Alberto Bacchelli & Georgios Gousios); Nikolaos Tsantalis & Mehran Jodavi
(RefactoringMiner, CodeTracker); Jean-Rémy Falleri, Matias Martinez, Martin Monperrus (GumTree);
Felix Grund & Reid Holmes (CodeShovel); Krzysztof Maziarz, Tom Ellis, Andrew Fitzgibbon — with
Simon Peyton Jones (alpha-equivalence hashing); Simon Marlow (Glean); Stas Negara & Danny Dig
(CodingTracker); Mik Kersten & Gail Murphy (Mylar/Mylyn); Profir-Petru Pârțachi, Miltiadis
Allamanis, Earl Barr (Flexeme); Bo Shen (SmartCommit); Joakim Tengstrand (Polylith); Stuart
Sierra (tools.namespace); Alec Sharp (the ENVY chapter; the system itself is OTI's).

---

## Sources (quality as rated by the extractors)

Primary: Unison update 7 (Chiusano 2015) <https://pchiusano.github.io/2015-04-23/unison-update7.html> ·
Unison reducing churn (Chiusano 2020) <https://www.unison-lang.org/blog/reducing-churn/> ·
Epicea repo + papers <https://github.com/tinchodias/epicea> ·
EpiceaUntangler (SANER 2015) <https://hal.science/hal-01116225> ·
ENVY/Developer (Sharp 1997 chapter; original guide URL unreachable, substituted) <https://people.engr.ncsu.edu/efg/517/f04/common/lectures/lectures/lec3/envyin10.html> ·
codeq blog (Hickey 2012) <https://blog.datomic.com/2012/10/codeq.html> ·
codeq repo <https://github.com/Datomic/codeq> ·
Kythe URI/VName spec <https://kythe.io/docs/kythe-uri-spec.html> ·
SCIP (Sourcegraph 2022; proto + DESIGN.md) <https://sourcegraph.com/blog/announcing-scip> ·
Glean incremental indexing (Marlow 2022) <https://glean.software/blog/incremental/> ·
SWHID spec v1.6 (2021) <https://docs.softwareheritage.org/devel/swh-model/persistent-identifiers.html> ·
HistoryFinder (arXiv 2025) <https://arxiv.org/html/2507.14716> ·
CodeTracker (ESEC/FSE 2022; deck + README + successor paper) <https://speakerdeck.com/tsantalis/accurate-method-and-variable-tracking-in-commit-history> ·
GumTree (ASE 2014 / ICSE 2024 journal-first; ACM page paywalled, mechanism cross-checked via secondary papers) <https://dl.acm.org/doi/10.1145/3597503.3639148> ·
RefactoringMiner 2.0 (TSE 2020; PDF not text-extractable, detail from companion arXiv + repo) <https://users.encs.concordia.ca/~nikolaos/publications/TSE_2020.pdf> ·
Hashing modulo alpha-equivalence (PLDI 2021) <https://arxiv.org/abs/2105.02856> ·
CodeShovel (ICSE 2021; landing page blocked, sourced from repo + HistoryFinder critique) <https://www.researchgate.net/publication/351417509_CodeShovel_Constructing_Method-Level_Source_Code_Histories> ·
clj-kondo analysis data (2025.06.05) <https://cljdoc.org/d/clj-kondo/clj-kondo/2025.06.05/doc/analysis-data> ·
rewrite-clj <https://github.com/clj-commons/rewrite-clj> ·
tools.namespace <https://github.com/clojure/tools.namespace> ·
Polylith <https://github.com/polyfy/polylith> ·
Flexeme (ESEC/FSE 2020) <https://dl.acm.org/doi/10.1145/3368089.3409693> ·
SmartCommit (ESEC/FSE 2021; paper blocked, sourced from SmartCommitCore source) <https://www.researchgate.net/publication/354057848_SmartCommit_a_graph-based_interactive_assistant_for_activity-oriented_commits> ·
Negara et al. ICSE 2014 / CodingTracker (PDF binary; mechanism from ECOOP 2012 companion) <http://dig.cs.illinois.edu/papers/ICSE14_Stas.pdf>.
Secondary/blog: Unison improved update process (preview post; mechanism recovered from the "Big Idea" doc) <https://www.unison-lang.org/blog/new-update-process/> ·
tools.namespace under the hood (rymndhng 2018; claims verified against source) <https://rymndhng.github.io/blog/2018/01/31/tools-namespace-under-the-hood/> ·
Mylar/Mylyn FSE 2006 (ACM paywalled; reconstructed from authors' talk + Eclipse Integrator Reference) <https://dl.acm.org/doi/10.1145/1181775.1181777>.

---

## Implications for the code-ingestor import contract

Not a design — graded implications mapping the evidence onto the kernel nouns. The design
conversation happens after this report.

**Strong (multiple independent sources, no contradicting evidence):**

- **Container.** A form-grained code unit gets a *minted* container id; its FQN and normalized
  content hash are revision attributes/indexes, never the id (Finding 2; versioning doc
  strategy 1). The file/namespace is ALSO a container — an ordered composition owning inter-form
  trivia, the `ns` form, and load order (Finding 1). Locals and sub-form structure get no world
  identity (SCIP's accessibility floor).
- **Revision.** Every revision carries a typed change-kind (body/signature/rename/move/format/
  doc…) — cosmetic-vs-semantic typing is what keeps history honest (+59/−186 oracle correction)
  and what the emergent register filters on. Content hashing is over a normalized form
  (ws-minify at minimum) so reformatting is identity-invariant.
- **Source anchor.** A separate anchor object joins (file-revision, span) → container — exactly
  codeq's `:codeq` entity and Kythe's anchor node. Spans are per-file-revision facts recomputed
  by analysis, never part of identity; macro-generated material may legitimately have no anchor.
- **Composition / reference edges.** Role-typed facts emitted per source revision, targeting
  container identities by NAME (generic/late-bound); the reverse index is a projection.
  Distinguish structural edges (contains/defines) from reference edges (calls/reads/requires),
  and carry the referenced name on the edge.
- **Projection.** Per-construct timelines, the bidirectional reference index, affected-sets
  (reverse transitive closure), and version-filtered graph views are all derived — log-primary is
  confirmed independently by codeq, Glean, SCIP, and Mylyn.
- **Decision.** Reconstructed identity continuity (rename/move/split inferred from git) and
  minted semantic episodes are decision-grade artifacts with detector/LLM provenance
  (`:tx/analyzerRev` pattern), revisable without rewriting the log. Declared in-loop continuity
  events are fact-grade. Continuity edges are typed and N:M.
- **Import posture.** Git objects land intact (commit DAG, trees, blobs) as immutable facts under
  kernel-owned serialization; the kernel is an additive index over canonical blobs, which makes
  round-trip free on the import path.

**Medium (good precedent, one source family, or known costs to weigh):**

- **defmethod/protocol impls** as head-container + contribution-containers linked by composition
  edges keyed on dispatch value / impl type (kondo's model, generalized).
- **Group revisions without proliferation.** Member edits propagate *value-level* group revisions
  cheaply (Model D hash-pin / ENVY auto-promotion) but mint no new identity threads; deliberate
  freeze + release-into-group are separate explicit acts (ENVY; Katz configuration-as-object).
- **Register relation as punctuation.** Commits/pushes/imports appear as events inside the one
  in-loop stream (Epicea's Monticello pattern), not as a parallel timeline needing
  reconciliation.
- **In-loop revision grain** = coalesced edit bursts (CodingTracker gluing), respecting the
  version-count wall; parse failure degrades to file-revision-grain anchoring until the reader
  recovers.
- **v1 import identity bar** = codeq tier (content-exact move/reformat continuity + per-FQN
  timelines); rename/split threads deferred to a later matcher or to human/LLM decisions.
- **Canonicalize analyzer output** (sort node lists, qualify `:from-var`) before hashing or
  storing — kondo guarantees no determinism.

**Speculative (single source or analogical):**

- Flexeme-style version-labeled multiversion reference graph as the projection shape for
  diff/compare views.
- Polylith-style interface indirection for composition edges (edge targets a contract identity;
  binding per composition) — attractive for swap-an-implementation workflows, unproven at
  function grain.
- Alpha-equivalence hashing (Maziarz) as the dedup/equivalence index once Clojure's binder forms
  are enumerated — strictly an upgrade over ws-minify, never the identity thread.
- Mylyn-style DOI folds over the kernel event log as attention-weighted projections feeding
  episode segmentation.
- EpiceaUntangler/SmartCommit-style deterministic pre-segmentation (temporal adjacency + hard
  dependency edges) as the cheap first pass an LLM then adjudicates — the two-stage emergent
  pipeline.
