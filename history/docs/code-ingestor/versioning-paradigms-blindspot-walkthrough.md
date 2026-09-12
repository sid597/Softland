# Blindspot Research, Rounds 2–5 — A Walkthrough

**Research walkthrough, 2026-06-26.** This is the companion to `versioning-paradigms-research.md` (Round 1) — same purpose, same format: *what went down in the research, source by source, and how it relates to Softland.* Round 1 asked "what is the identity of code once a file stops being the atom?" and answered from inside the version-control corpus. These four rounds asked the **opposite-facing** question:

> The kernel now makes eight load-bearing claims (below). **Where does the wider world assume something the kernel has no slot for?** Each round aimed sources *at* the kernel to break it, then — in the last round — *at* the kernel to harden it.

This doc walks you through **all 124 sources** I actually read, grouped the way Round 1 grouped its 25: by angle, one entry per source, each a claim + what the source argues + how it bears on the kernel and on you. The adversarial-verification bookkeeping (which strains "held up," which were "loose metaphor") lives in the sibling `blindspot-research-rounds-2-5.md` — **it is deliberately absent here.** This is the journey, not the verdict.

### The eight claims every source was aimed at
- **C1** — Identity = a stable *container*; change = a chain of immutable *revisions*.
- **C2** — The event *log is primary*; all materialized state is a *projection* of it.
- **C3** — *Rama is ground truth*; workers/agents stream observations *back* to Rama; the UI reads Rama ("back-arrow"). Nothing streams to the UI as truth.
- **C4** — *Declared > inferred*: inferred structure is never accepted as truth at ingest.
- **C5** — *Two times*: arrival/transaction vs committed/valid.
- **C6** — *Groupings ("configurations") are first-class versioned objects*: version the group, snapshot the members.
- **C7** — Minting is at *commit grain*, not save grain; *semantic time is a DAG*.
- **C8** — *Preserved disagreement* is first-class; synthesis is *late-bound*.
- Plus the **flagship axiom** ("semantic zoom = lawful compression — identity/provenance/relations survive every axis or the zoom is fake") and the **soul axiom** ("Softland is a *place* inhabitable by another mind").

### The four acts
- **Round 2 — the CS corpus** (30 sources): turn the kernel's *own* discipline against it.
- **Round 3 — leaving the corpus** (32): history, law, religion, cartography, oral tradition.
- **Round 4 — the formal & experiential sciences** (34): physics, logic, control theory, learning science.
- **Round 5 — the constructive round** (28): the math the kernel is *built from*, used to *fix* it.

There's a **"How this relates to *you*"** section at the very end — the handful of sources that turned out to be about your own taste, not just the kernel.

---

# ROUND 2 — The CS Corpus

**What this round hunted.** Round 1 read version-control systems *with* the kernel — confirming "identity = container, change = immutable revisions." Round 2 read the *rest* of computer science *against* it: collaborative editing, build systems, event sourcing, provenance standards, transparency logs. The headline it found: **the kernel's mechanics are sound, but three of its claims are universal overclaims** — "Rama is the one truth" (C3), "the log is primary and the projection is cheap" (C2), and "preserved disagreement is always good" (C8) — each of which a mature CS subfield treats as one design point with real costs, not a law.

## Angle A — Concurrency & collaboration without a central truth
*The premise here is the exact opposite of the back-arrow: many editors (for us, parallel LLM agents) converging with no authoritative node. The question — does that refute "Rama is the one truth"?*

### [Local-First Software](https://www.inkandswitch.com/essay/local-first/) — convergence-as-truth, with no arbiter
The Ink & Switch manifesto argues "the copy of the data on your local device is the *primary* copy; servers hold secondary copies." Truth is a **mathematical property of a merge function**, computed identically everywhere — never a *decision* made by a privileged node. That denies the very category C3 depends on: there is no ground-truth location to stream back to. For Softland's parallel detached agents, the sharp gap it names is "this agent's local world was fully authoritative for the 6 hours it ran offline, and Rama must *merge* it, not *re-arbitrate* it" — which the kernel can't currently say. (It also, honestly, concedes the cost of going coordinator-free — unbounded history, no authoritative schema — which is exactly what Rama's central authority buys you.)

### [Sun, Jia, Zhang, Yang & Chen — "…Convergence, Causality Preservation, and Intention Preservation…" (1998)](https://www.cs.cityu.edu.hk/~jia/research/reduce98.pdf) — convergence is not intention
The foundational Operational-Transformation paper, and the single most load-bearing source of the whole round. Its theorem: serializing concurrent edits into one agreed order achieves **convergence but provably destroys intention** — the `ABCDE → A1CDE` counterexample where every replica agrees and every replica is *wrong*. The lesson for the kernel: **the back-arrow gives you an *order*, not a *merge*.** A Rama append-log + deterministic fold is necessary but not sufficient when agents edit the same artifact from divergent snapshots; truth = log + a transform function, and the transform is the hard half the kernel never named.

### [Kleppmann & Beresford — "A Conflict-Free Replicated JSON Datatype" (2017)](https://arxiv.org/abs/1608.03960) — conflict resolution is a design-time property, not a runtime decision
A CRDT bakes merge semantics *into the data structure* so every operation is unconditionally accepted and concurrent ops commute by construction — "operations cannot fail after they have been performed on one replica." There is **no arbiter and no accept/reject step**; the kernel's ActionDecision interpreter has no analogue here. It also gives a name to something C8 lacks: the **multi-value register** — disagreement held *structurally and eagerly* in the data, not via a late human synthesis step.

### [How Figma's Multiplayer Technology Works](https://www.figma.com/blog/how-figmas-multiplayer-technology-works/) — sometimes the right merge is to *throw information away*
Figma's truth is a materialized latest-value map; the op-stream is ephemeral transport — **C2 inverted** (state is canonical, the log is not primary). And it makes a quality judgment the kernel never makes: last-writer-wins per property, "AB or BC but never ABC," because a merged interleaving would be *garbage*. That **refutes the universality of C8**: in some domains preserved disagreement is corruption. Figma also *defends* the central-authority half of C3 (one server, deliberately not P2P) — so it cuts both ways.

### [Kleppmann — "CRDTs: The Hard Parts" (2020)](https://martin.kleppmann.com/2020/07/06/crdt-hard-parts-hydra.html) — the merge can converge *and be wrong*
The interleaving anomaly: two individually-valid concurrent edits merge into a deterministically-converged result that is irreversibly semantic garbage — "convergence is not correctness." C8 has no slot for "the merge succeeded *and* destroyed meaning." Read inversely, though, the talk is a defense of C3: most of the "hard parts" come from refusing a coordinator, and Rama *is* one — so Softland buys its way out of un-GC-able tombstones by keeping the authority.

## Angle B — The operation / driving layer (where jj lives)
*Versioning as something you* drive *— where what's versioned is not just state but the* acts *of versioning. Does the kernel's "exactness flag" survive a world where the working copy is already a commit?*

### [Jujutsu — "The working copy is a commit"](https://docs.jj-vcs.dev/latest/working-copy/) — a live, mutable head with stable identity
jj's working copy is a *first-class commit that is amended in place* on every save — a **stable change-id** with a **changing commit-id** — until it's superseded. The kernel's C1 ("a chain of *immutable* revisions") has no slot for a revision that's mutable-until-final. And jj refutes the kernel's "parked = inferred" equation: disk edits are *declared content*, committed automatically as fully real; provisionality is about *finalization grain*, never epistemic confidence.

### [Mercurial — Changeset Evolution](https://wiki.mercurial-scm.org/ChangesetEvolution) — rewrite turns a chain into an unstable second graph
A decade of `hg evolve` experience: the moment a stable identity may be rewritten, you don't get a chain — you get an **obsolescence graph** overlaid on the commit DAG that spontaneously breaks (orphans, content-divergence, phase-divergence) and needs a perpetual repair loop. Two things the kernel lacks fall out: a tombstone/redirect overlay store, and **mutability-as-phase** (draft → public → secret gating *when* a thing may still be rewritten). Notably, Mercurial treats two living successors of one identity as a *corrosive instability to eliminate* — a direct challenge to C8's bet that per-identity divergence is habitable.

### [Jujutsu — Operation log](https://docs.jj-vcs.dev/latest/operation-log/) — the acts of versioning are themselves a versioned, mergeable log
Undo/rebase/regroup aren't edits to the log — they're new entries in a *second* log whose subject is the repository state itself, and that op-log is a DAG that **merges on concurrency**. So "undo of an undo" is ordinary forward motion. The kernel has *one* log (content events) and no first-class notion of an event whose subject is another versioning act — which is exactly the primitive parallel multi-writer history needs.

### [Jujutsu — Glossary: changes vs commits](https://docs.jj-vcs.dev/latest/glossary/) — stable identity across rewrite, done as a pointer not a chain
"Changes themselves don't exist… only the change ID does." Identity is a *mutable pointer*; on rewrite the prior revision is *abandoned*, not linked as a predecessor. The cheap, working form of "stable identity across rewrite" is the opposite of C1's accumulate-only container — it throws revisions *out* of the container, a state the kernel has no slot for.

### [Jujutsu — Conflicts (technical)](https://docs.jj-vcs.dev/latest/technical/conflicts/) — disagreement as an algebra with cancellation laws
The most beautiful one in the angle, and it speaks to your category-theory instinct. A conflict is a first-class *value* (`Merge<T>`: a start term + alternating ±diff pairs) of the *same type* as resolved content, so rebase/merge/diff/checkout work on it unchanged — and conflicts **compose and cancel**, so disagreement can flow through a chain of transformations and sometimes auto-resolve as a lawful consequence. It unifies C2 and C8 the kernel keeps separate: the human-readable conflict is a *projection* of an algebra of diffs, and synthesis is *evaluation of that algebra*, not a manual act.

## Angle C — Incremental computation & build systems (the projection half we never studied)
*"Log is truth, state is a projection" — the projection half is a mature field under another name: minimal recomputation over a content-addressed dependency DAG.*

### [Build Systems à la Carte (Mokhov, Mitchell, Peyton Jones, 2018/2020)](https://www.microsoft.com/en-us/research/wp-content/uploads/2018/03/build-systems.pdf) — "state is just a projection" is the easy half
The unifying theory of build systems shows the store is *never just the projection*: a correct, *minimal* projection needs a **rebuilder** plus a persistent **trace store** that is *not derivable from the log* (it records the exact upstream versions each derived value consumed), plus **early cutoff** (stop propagating when a recomputed value is unchanged). For Softland's agents re-deriving syntheses over revised claims, this is the missing 90% of "projection." Its applicative-vs-monadic axis also names what the kernel can't: projections whose dependencies are knowable up front vs discovered only by running them (the LLM-agent case).

### [Durable Incrementality in Salsa (rust-analyzer, 2023)](https://rust-analyzer.github.io/blog/2023/07/24/durable-incrementality.html) — "re-derive from the log" has a floor cost
Empirical evidence from the largest live incremental engine: even a *no-op* input change pays ~300 ms of whole-graph revalidation. The fix is **durability** — a hand-assigned *rate-of-change* partition on inputs (volatile vs durable) that bounds how much of the projection you even *look at*. The kernel models *what* is true and *when* it committed, but has no axis for *how often* a thing changes — and that axis is what makes the projection affordable.

### [Adapton: Composable, Demand-Driven Incremental Computation (Hammer et al., 2014)](https://www.cs.tufts.edu/~jfoster/papers/cs-tr-5027.pdf) — demand is a first-class input
Adapton inserts a *third* layer between log and projection: a memoized computation graph that does *no work until forced*. The consequence the kernel's "UI reads Rama" misses: **reading is the event that causes materialization** — demand flows *back* into the derivation substrate and decides what gets computed. The kernel treats reads as side-effect-free; here, the read is a second back-arrow.

### [Self-Correcting Materialized Views (Materialize)](https://materialize.com/blog/self-correcting-materialized-views/) — the projection has its own clock, and it drifts
Differential/timely dataflow maintains a per-view **write frontier** — a commit boundary that advances *independently* of the source log — and the materialized view *self-corrects* because its output can drift across engine versions. So there's a **third time** the kernel's two-times model has no slot for, and the projection is a *second stateful authority*, not a free replay.

### [salsa-rs/salsa](https://github.com/salsa-rs/salsa) — verification-time and early cutoff as first-class machinery
Salsa separates "when was this produced" (`changed_at`) from "when did I last confirm it still valid" (`verified_at`) — a clock that is *neither* arrival nor valid time — and backdates: if a recompute yields an identical value, the change does *not* propagate. Two independent clocks, two DAGs (one causal, one data-dependency); the kernel currently has the causal DAG but no operative data-dependency DAG to decide what actually needs recomputing.

## Angle D — Event sourcing & temporal databases, read directly
*We invoke "the event log is primary" everywhere and had never read the discipline, or the closest production system to the kernel.*

### [The Ugly of Event Sourcing — Projection Schema Changes (Dennis Doomen)](https://www.dennisdoomen.com/2017/06/the-ugly-of-event-sourcing-projection.html) — the projector is a stateful autonomous artifact
From a practitioner: projection is the *expensive* half. Each projector has its own checkpoint position in the stream, producing inter-projection read-skew, in-flight wrong-state windows, and non-linear rebuild cost. "The log is primary" silently inherits all of that — it names the recoverable part and hides the costly part.

### [Versioning in an Event Sourced System (Greg Young)](https://leanpub.com/esversioning/read) — a committed event is only as durable as the schema to read it
The bytes are immutable, but the *meaning* of an event *type* drifts. Young's whole apparatus (upcasting, the tolerant reader, new-event-type-on-incompatible-change) addresses a lifecycle the kernel has *zero* representation for: revisions of the event *vocabulary* itself. C1/C7 version objects and commit-times; nothing versions what an event *means*.

### [Datomic data model — datoms, accumulate-only vs append-only](https://docs.datomic.com/transactions/model.html) — the closest system to the kernel, and it splits two things the kernel fuses
Datomic distinguishes **accumulate-only (a semantic property)** from **append-only (a structural one)** — you can be fully history-preserving without a replayable append log; its durable primary is background-maintained *indexes*, not the log. It also makes only *one* time first-class (transaction time) and treats valid time as an ordinary attribute — a quiet challenge to C5's two-kernel-times and to "replay-from-log" as a structural commitment.

### [Kafka Compacted Topics as a Database (Conduktor)](https://www.conduktor.io/blog/kafka-compacted-topics-as-database) — "source of truth" is a continuously-running GC process
The durable industrial form of "log is primary" — log compaction — keeps the log bounded *only by discarding superseded values* ("retain the latest value for each key"). So the deployed log is **lossy by design**, with tunable aggressiveness. "Complete replay" is the textbook story; "retain the latest per key" is what actually ships.

### [Temporal Databases (Snodgrass survey)](https://www2.cs.arizona.edu/~rts/pubs/EDC.pdf) — the kernel *mislabels* its two times
The canonical source, and it caught a real bug: in the literature **transaction time = the committed/stored time**; **valid time = reality time**. The kernel glues "committed" onto "valid" and calls arrival-vs-committed its two times — but both of those are flavors of transaction-time, and *valid-time is missing entirely*. Snodgrass also names the correctness law C2 was asserted without: **snapshot reducibility** (the temporal answer at any time-slice must reduce to the ordinary non-temporal answer), enforced by *coalescing*.

## Angle E — Provenance & argumentation (closest to the soul)
*Softland's data model* is *a discourse/argument graph (Q→C→E→D→R→F). There is a standardized field for representing provenance and argument we'd only approached sideways.*

### [Micropublications (Clark, Ciccarese, Goble, 2014)](https://pmc.ncbi.nlm.nih.gov/articles/PMC4530550/) — a claim's standing is *computed*, not stored
The load-bearing primitive is an **attributed statement** with a Toulmin warrant and external support/challenge edges, whose standing is **defeasible and non-monotonic** — and there are *two distinct* attacks: **rebut** (deny the conclusion) vs **undercut** (sever a supporter's warrant). C8 stores disagreement; it has no notion that the graph *determines* a status that flips when a sibling claim arrives. This is the sharpest single sharpening of the discourse-graph idea in the whole run.

### [Conklin et al. — "Facilitated Hypertext… 15 Years on from gIBIS" (2001)](http://www.cognexus.org/conklin-ht01.pdf) — someone has to *do* the structuring, and they pay at the worst moment
A 15-year retrospective on the design-rationale capture problem: structured sensemaking is not a property of data — it's continuous human labor, and it names a first-class costed role the kernel lacks (**the technographer**). Producing the structured log is the hard, adoption-killing part, and capture is most needed at breakdowns — exactly when the producer has least spare capacity. Softland's whole "reasoning trails are first-class material" bet rests on the unpriced assumption that the trail accretes for free.

### [Horner & Atwood — "Design Rationale: The Rationale and the Barriers" (2006)](https://research.cs.vt.edu/ns/cs5724papers/1.motivatingreuse.tpgap.atwood.drationale.pdf) — capture cost falls on the producer; benefit on a later reader
The survey of why design-rationale systems failed for 20+ years: **Grudin's disparity** (producer pays, consumer benefits) plus an un-closable gulf of evaluation, and the finding that situational relevance is "independent of truth" (a ~41% retrieval ceiling). The back-arrow models streaming observations back as a free, self-evidently good write; this paper is the documented record of that exact bet failing.

### [PROV-DM: The PROV Data Model (W3C)](https://www.w3.org/TR/prov-dm/) — identity is a non-hierarchical lattice, not one canonical container
The provenance interchange standard models the *opposite* of C1: every revision is a *separate entity*; "change" is an external edge (`wasRevisionOf`). And it gives **perspectival identity** as a lattice (`alternateOf` / `specializationOf`) — one real-world thing legitimately spawns many coexisting entities, each fixing different aspects, with no privileged center. Plus `wasInvalidatedBy`: a first-class **retraction-without-successor** that a revision chain can only fake as a tombstone.

### [Nanopublications (Kuhn et al., 2018)](https://ar5iv.labs.arxiv.org/html/1809.06532) — identity = the hash of the content, and it travels with the bytes
The cleanest inversion of C1: a nanopublication has *no container* — identity **is** the cryptographic hash of its immutable content (a "trusty URI"), so identity and content can't diverge and any party can verify it *offline* without contacting the store that made it. For "understanding inhabitable by another mind," this is decisive: a Rama-minted id is meaningful only inside Rama; a content hash is meaningful everywhere.

## Angle F — Preservation, authenticity & the frame-break
*Holding understanding across time is a civilizational discipline with formal standards — and for parts of this, the right primitive may be addressing, trust, or preservation, not versioning at all.*

### [RFC 6962 — Certificate Transparency](https://datatracker.ietf.org/doc/html/rfc6962) — authority is a verifiable artifact, not a location
CT exists *because* "a single self-declared log is exactly what you must not trust by fiat." It makes truth checkable anywhere via a signed tree head + O(log n) inclusion/consistency proofs + cross-party gossip. That inverts C3's dependency: a compromised or forked Rama is *undetectable* by any consumer who merely reads it. For a public form another mind can inhabit, the trail must carry its own integrity proof — the deepest tension with the back-arrow.

### [Perkeep — Permanode & Claim Schema](https://github.com/perkeep/perkeep/blob/master/doc/schema/permanode.md) — minting as a cryptographic act, offline, with no authority
A permanode is a client-generated random nonce plus a signature; the signature both *names* and *authorizes*. Identity exists the moment a writer signs it — offline, in parallel, with zero coordination and no commit. C7's "mint at commit grain inside Rama" blocks exactly the partitioned/offline parallel agents Softland is built around; Perkeep shows minting needn't touch an authority at all.

### [NARA — Significant Properties (Archival Framework, 2009)](https://www.archives.gov/files/era/acera/pdf/significant-properties.pdf) — "keep every revision" is necessary but not sufficient
Archival science's primitive is the inverse of "keep all the bits": identity across time is carried by a **declared, typed, ranked set of properties** that *must* survive a transformation, plus an account of what was knowingly sacrificed — because an immutable byte snapshot becomes *unreadable* as formats obsolesce. Softland's own "semantic zoom is lawful compression" axiom *is* a significant-properties claim, but the kernel has no first-class object that says, per zoom/migration, which dimensions are load-bearing.

### [Ted Nelson — Transclusion / Xanalogical Structure (Xanadu)](https://xanadu.com.au/ted/XU/XuPageKeio.html) — the same content, knowably in many places, without copying
"A transclusion is a connection between things which are the *same*." C6 snapshots members by *copying* content into each configuration; Nelson's thesis is that copying is the defect. The missing capability: one canonical span, transcluded into N contexts, edit-propagating and co-present — so a claim cited by three agents stays *one* editable thing, not three frozen copies.

### [Let's Encrypt — End-of-Life Plan for RFC 6962 CT Logs](https://letsencrypt.org/2025/08/14/rfc-6962-logs-eol) — get the truth node *out* of the read path
The ten-year production bill for "the append-only log is primary and serving it is free": running the Merkle log *as* the live service was a seven-figure, read-amplification liability. The fix wasn't to abandon append-only — it was to **split write-path from read-path**, serving immutable, content-addressed *tiles* with embedded proofs that any CDN can cache. C2/C3 treat "UI reads the one truth store" as settled; production says interpose a frozen, verifiable projection.
---

# ROUND 3 — Leaving the Corpus

**What this round hunted.** Round 2 broke the kernel's *frame* but kept its *sources* — every disagreement it found was internal to computer science. Round 3 left the corpus entirely and searched in other disciplines' own vocabulary: anthropology, jurisprudence, cartography, accounting, estimation theory, art conservation, political philosophy. The rule was "a source that re-invents a CS idea is worthless; a field that solved the kernel's problem under a different name — or holds inhabitable understanding with no software at all — is gold." This is the round where the verdict moved toward the soul: it **fatally refuted the flagship compression axiom**, found that several of the kernel's roots live *outside software*, and discovered that the most durable, inhabitable knowledge systems humans ever built *invert nearly every kernel choice*.

*(Thirty-one real sources below; one of the thirty-two a scout returned was an empty placeholder page, dropped honestly rather than dressed up.)*

## G1 — Pre-digital & non-Western memory, disagreement & trust
*How have humans held inhabitable public understanding across generations with no log, no write, no software — and how did they handle disagreement and trust?*

### [Lynne Kelly — *Knowledge and Power in Prehistoric Societies* (2015)](https://www.lynnekelly.com.au/?page_id=1325) — performative persistence; the walk *is* the index
The single most important non-CS source in the run, and the strongest existence proof *for* Softland's soul. Aboriginal songlines encode knowledge *in the landscape itself*, held with **no log, no write, no stored state at all** — yet the corpus persists accurately for tens of thousands of years, longer than any event-sourced system has run. Durability is an *ongoing obligation to re-perform*, not a write to storage; the *walk* through places in sequence **is** the query mechanism. This both vindicates "Softland is a place, not a tool" *and* delivers the deepest warning of the whole run: **place-ness is a property of medium + practice + body + power — a substrate can host it but cannot manufacture it.** You're trying to buy with event-sourcing what songlines got from terrain, ritual, and trained bodies.

### [Talmud, Eruvin 13b — *"Eilu v'eilu divrei Elohim chayim"*](https://www.sefaria.org/Eruvin.13b) ([+ commentary](https://www.chabad.org/library/article_cdo/aid/5437548/)) — rule *and* preserve, forever, without merging
The sharpest single hit on C8. The verdict "*these and these* are the words of the living God; **however**, the halakha follows Beit Hillel" does two textually separable things at once: it issues a **binding ruling** *and* **preserves both views as permanently, equally true** — without merging them. That splits a primitive the kernel fuses: **actionable-resolution** (act on Hillel now) vs **ontological-resolution** (both stay true forever, *by design*). Softland has a type for "ruled" and one for "not yet synthesized" — it has *no type* for "we act on Hillel **and** Shammai stays equally true forever." So C8's telos — synthesis as the eventual goal — is a *cultural assumption*, not a law. (Bonus: authority to rule is awarded for *epistemic humility* — Hillel taught Shammai's view first — and a never-killed minority view can be reactivated centuries later without re-minting.)

### [*ʿIlm al-Rijāl* — the science of hadith narrators](https://studioarabiya.com/ilm-al-rijal-in-hadith/) ([+ biographical evaluation](https://en.wikipedia.org/wiki/Biographical_evaluation)) — distributed, graded, weakest-link trust, ~1200 years old
A complete trust calculus with no central truth store. The same content has **no single truth value** — only a *vector of path-relative reliabilities* — computed by walking the attributed chain (isnad) and composing **per-transmitter, two-axis credentials** (honesty `ʿadāla` × accuracy `ḍabṭ`) on a continuous scale, **weakest-link along the path**, with independent corroborating chains raising standing (`mutawātir`). Two things land hard on the kernel: trust is *recomputed per claim*, not stored (vs C3's single truth node); and the hadith scientists learned the hard way that **forging the isnad was more common than forging the content** — the very provenance edge the back-arrow treats as truth is the adversary's favorite target.

### [The CARE Principles for Indigenous Data Governance (2020)](https://datascience.codata.org/articles/10.5334/dsj-2020-043) — entitlement is part of what the fact *is*
Access here is not a permission layer over neutral data — it is **constitutive of the knowledge itself**, defined by a standing relationship (kinship, custodianship, protocol). CARE names *unconditioned public form* as the **harm**, not the goal, and says some knowledge *must not* be concentrated or uniformly served — and that decision is *not the holder's to make*. This forces the soul axiom's operative clause from "inhabitable by *another* mind" to "inhabitable by an *entitled/prepared* mind," and reframes the back-arrow's "pull every observation into one truth store" as a *structurally extractive* act.

## G2 — Lawful compression across scale (the flagship axiom's real discipline)
*Is "identity/provenance/relations survive every zoom axis or the zoom is fake" achievable — or does a mature science of compressing information across scale prove you must always sacrifice something?*

### [Gauss — *Theorema Egregium* (1827)](https://en.wikipedia.org/wiki/Theorema_Egregium) — you cannot flatten a sphere without distortion
The hard mathematical core of the flagship's refutation. The flagship axiom is a *multi-property conjunction* over a representation-changing transform — structurally identical to demanding a flat map be conformal **and** equal-area **and** equidistant at once. Gauss proves that conjunction is **unsatisfiable** whenever source and target differ in intrinsic curvature. Semantic zoom *is* such a transform — so "all of identity, provenance, and relations survive every axis" is not merely hard, it is *impossible*, the same way a distortion-free flat world map is impossible.

### [Töpfer & Pillewizer — the Radical Law (1966)](https://www.polyu.edu.hk/proj/gef/index.php/glossary/topfers-radical-law/) — a hard, shrinking carrying capacity
The earliest formal law of zoom-out: the number of features you can show shrinks as the square root of scale (Nd = Ns·√(Ms/Md)), a budget fixed by the *medium and the eye*, not the data. As you zoom out, most members must be **deleted, not folded** — and their identity does *not* survive, nor do pointers back to them. Softland assumes the substrate can always carry one more revision, one more member; cartography says there is a **representational carrying capacity** and the budget is brutal.

### [McMaster & Shea — *Generalization in Digital Cartography* (1992)](https://cartogis.org/docs/proceedings/archive/auto-carto-13/pdf/visualizing-cartographic-generalization.pdf) — the named operators of lawful destruction
The taxonomy of generalization operators — *merge* (dissolves member identity), *displace* (destroys true position — a relation), *typify*, *collapse* — is a centuries-mature discipline whose entire professional competence is the **disciplined destruction** of exactly the three things the flagship insists must survive. And it's *not* data-driven compression: it's **perception-driven, authored, purpose-relative** re-authoring. Going coarser you deliberately *move things off their true position* and *draw them bigger than reality* to stay legible.

### [Monmonier — *How to Lie with Maps* (3rd ed., 2018)](https://press.uchicago.edu/ucp/books/book/chicago/H/bo27400568.html) ([overview](https://en.wikipedia.org/wiki/How_to_Lie_with_Maps)) — disclosed, measured infidelity is the unit of honesty
Cartography's honesty test is *inverted* from the kernel's: a zoom that loses nothing is the useless 1:1 map; the honest small-scale map is the one that **sacrifices the most**. The canonical instrument is **Tissot's indicatrix** — a field of ellipses drawn *on* the map showing, at every point, exactly how much was distorted. That's the flagship repair the kernel lacks: not "preserve everything" but **declare and publish the distortion budget, per axis, per cell**. Good compression is lawful *because it lies legibly*, not despite it.

## G3 — Pre-digital ledgers & law (the seal and the meaning-drift the open log lacks)
*Accounting and law have run immutable, time-versioned authoritative records for centuries. What primitives do they have that a perpetually-open append log lacks?*

### [Bank Reconciliation (Principles of Accounting, ch. 6)](https://www.principlesofaccounting.com/chapter-6/bank-reconciliation/) — truth as the forced intersection of two independent ledgers
A 500-year-old engineered *rejection* of single-store truth. Accounting deliberately keeps a **second, independently-authored ledger** (the bank's) so that no single writer can define cash reality, and treats truth as the **reconciled intersection** of the two — with integrity measured as a **collusion threshold** (how many independent authors would have to conspire). This is the integrity model C3's single back-arrow structurally cannot have: a confidently-wrong agent writes uncontested fact because there's no separated second ledger to disagree.

### [The Closing Process / Periodicity (UTS open accounting)](https://oer.pressbooks.pub/utsaccounting2/chapter/describe-and-prepare-closing-entries-for-a-business/) — the *seal* the open log lacks
Accounting agrees the journal is primary and the ledger is a projection — but adds a primitive the kernel has no slot for: the **close**. You **cannot assert authoritative truth about an open interval**, only about a *closed* one; the period close is a governed, irreversible fold-and-seal past which history is frozen and authoritative, staked on a named authority's reputation. Reconstructability (C2/C7's ever-open DAG) is *not* the same as declared finality.

### [Albanesi — *Amendment and Repeal* (2021)](https://kups.ub.uni-koeln.de/53560/1/albanesi-2021-amendment-repeal-clarity-drafting.pdf) — change with *zero* edit to the thing that changed
Legislative drafting names **change-by-context**: provision A's operative meaning shifts because a *separately-identified* later provision B now stands in tension with it — *A's text is never touched* (non-textual / implied amendment). The kernel's container+revision-chain literally cannot represent this — there is no diff, no revision, yet the meaning changed, located by an interpreter at read time. (Round 4's Paradox of Precedent confirms it: *Casey* was byte-identical after *Dobbs*, its authority gone.)

### [The Paradox of Precedent About Precedent (Harvard Law Review, 2024)](https://harvardlawreview.org/wp-content/uploads/2024/12/138-Harv.-L.-Rev.-797.pdf) — the holding is inferred *later*, by other courts
The operative truth of a case — what it "stands for" — is **not declared by the deciding court at commit**; it is *inferred by later courts*, and that later inference is the binding artifact (the Marks rule for fragmented courts makes this explicit). That's a head-on hit on C4 (declared > inferred) *and* a self-reference puzzle the single-stratum kernel can't host: a court overrules its own "never overrule" rule.

## G4 — Truth as graded belief & evidence fusion (not accept/reject)
*The kernel orders, accepts, or rejects observations. What if truth from many noisy observers is a fusion weighted by confidence — a distribution, not a value?*

### [Kalman — "A New Approach to Linear Filtering and Prediction" (1960)](https://www.cs.unc.edu/~welch/kalman/media/pdf/Kalman1960.pdf) — the back-arrow runs the wrong *operation*
Kalman *agrees* with C3's topology (observations flow back to one estimator) but proves the kernel uses the wrong operation on them. Combining many noisy observers of one underlying state is an **estimation** problem with a provably optimal minimum-variance answer — blend each in proportion to its reliability — so *picking a winner and discarding the rest is strictly suboptimal*. And the kernel has no **covariance** field: no way to represent that an accepted observation is itself uncertain. Truth as a *second moment*, not just a value.

### [Alchourrón, Gärdenfors & Makinson — the AGM theory of belief revision (1985)](https://philpapers.org/rec/ALCOTL-2) — an append-only mind can't stay rational
AGM proves a corpus that can only **expand** cannot stay rational: the moment observation N+1 contradicts an accepted belief, append yields `p ∧ ¬p` and a closed theory detonates. You need **contraction** (rationally *give up* a belief and propagate the withdrawal) governed by an **epistemic-entrenchment** ordering — and crucially, that ordering is **not recoverable from the events or their arrival order**. So "materialized state is a deterministic projection of the log" is false under conflict: current truth = events × an entrenchment ranking the log doesn't contain.

### [Glenn Shafer — *A Mathematical Theory of Evidence* (1976)](https://www.glennshafer.com/books/amte.html) — the width of non-commitment is first-class
C4's accept/inferred binary is a 1-bit collapse of a 2-D object. Dempster–Shafer carries **two** numbers per proposition — belief (committed support) and plausibility (failure-to-refute) — plus an explicit **mass of ignorance**, distinguishing states the kernel can't tell apart ("weak evidence for A" vs "no evidence either way" vs "contested"). It also flags, via Zadeh's counterexample, that **some sources must not be pooled** — high conflict makes naive combination absurd.

### [Cox & Jaynes — *Probability as Extended Logic* (1946 / 2003)](https://philpapers.org/rec/COXPFA-3) — the calculus of belief is forced, not chosen
Cox's theorem: the instant you reason under uncertainty *and* want to stay consistent, you are **forced** onto a real-valued plausibility scale obeying the sum/product rules; binary {accepted, rejected} is the degenerate endpoint. And consistency demands **path-independence** — all derivation paths to the same claim must yield the same degree. The kernel guarantees you can *trace* how a claim was derived (provenance), but has no slot for the demand that all paths *agree*.

## G5 — Defeasible argumentation: the *computation* of standing
*Storing preserved disagreement (C8) is not the same as knowing what the graph currently warrants believing. How is a claim's standing computed, and how does it flip?*

### [Guhe — *An Indian Theory of Defeasible Reasoning* (Nyāya upādhi, 2022)](https://www.hup.harvard.edu/books/9780674273412) — inference is first-class truth, *defeasibly*
Classical Indian epistemology institutionalizes the opposite of "declared > inferred": inference (*anumāna*) is a *pramāṇa* — a first-class source of valid knowledge equal to perception. An inferred conclusion **is** truth — just *defeasible* truth, warranted exactly while no **upādhi** (an undercutting defeater that voids the *connection*, not the conclusion) is known. The kernel can store a claim and a counter-claim but has no object for "the warrant is currently severed," and no notion of a *stands-until-defeated* status.

### [Uṣūl al-fiqh — *Taʿāruḍ wa-l-Tarjīḥ*](https://shajarah.org/reconciliation-between-contradictory-texts-of-shariah/) — a *third* temporal status: currently-superseded-but-preserved
Islamic legal theory runs a **standing function**: given the permanently-preserved corpus of conflicting texts, compute which one currently *governs* via an ordered arbitration pipeline (reconcile → prefer → abrogate → suspend). **Naskh** (abrogation) exhibits a temporal status C5's two times can't hold: a verse has an arrival record, *was* valid, is *permanently preserved* — yet its operative warrant is *currently defeated* by a later text. "Currently-superseded but still-recited" is neither arrival-time nor valid-time; it's a re-derivable *relation*.

### [H.L.A. Hart / SEP, "Defeasible Reasoning"](https://plato.stanford.edu/entries/reasoning-defeasible/) — standing is defined *negatively*, by absence of a defeater
The legal origin of defeasibility: a title is valid **unless defeated** by something discovered later — with *no revision to the title document at all*. Nothing in the object changed; its **warrant** did. The kernel can only express change as a new revision appended to a container; it has no representation for a status that flips (valid → void) with no edit to the thing.

## G6 — Non-discrete, non-diffable modalities
*The kernel's verbs — diff, merge, fold, snapshot — presuppose discrete, comparable units. What about content that has none?*

### ["Can I Unmix This Track?" (audio engineering) + non-destructive editing](https://www.audiomasterclass.com/blog/can-i-unmix-this-track) — superposition is many-to-one; the inverse is ill-posed
The kernel's deepest unstated assumption is that materialization is *free and reversible* ("state is a projection of the log"). Audio says: only if the plurality is kept as **recipe**, never as **rendered artifact**. The moment two disagreeing mixes are summed into continuous media, the disagreement is **physically gone** — you cannot un-sum a waveform. C8's "preserve two mixes co-present and synthesize later" is impossible once the projection is baked; some projections are physically one-way.

### [Pentimento (art conservation)](https://en.wikipedia.org/wiki/Pentimento) — revision and revised are the *same matter*, and it mutates after authoring
Oil paint breaks the kernel's separability assumption: the three positions of the Arnolfini foot are not three addressable snapshots — they are *one fused pigment stack*, with no chain and no immutability. Worse, the "final" state **mutates after authoring** (lead-white films grow translucent over centuries, revealing buried revisions the painter meant to hide). A clean polarity inversion of C2: the artifact is primary and its history is a *backward-projected hypothesis* recovered by infrared, not a forward log.

## G7 — Adversarial epistemics & mandated forgetting
*The kernel assumes honest agents and a globally-readable, permanently-immutable log. What breaks under strategic actors and legal erasure?*

### [The Athenian Amnesty of 403 BCE — *mē mnēsikakein*](https://www.academia.edu/23732270/Oath_Covenants_and_Laws_in_the_Athenian_Reconciliation_Agreement_of_403_BCE) — mandated unrecoverability, with recall as the new crime
Athens enacted the opposite of "keep the full log, render forgetting as a projection": a defined region of the past was made **legally non-existent**, and *appealing to it became the crime*. This is a legitimate, sometimes-mandatory **forgetting primitive** the immutable log structurally cannot honor — ground truth becomes the community's *covenant about the log*, which can declare a region null, not the log itself.

### [Damnatio memoriae (Roman memory sanctions)](https://en.wikipedia.org/wiki/Damnatio_memoriae) — the scar is the deliverable
The exact inversion of the flagship axiom: Softland says "if identity doesn't survive the zoom, the zoom is fake"; damnatio says **survival is the failure mode** and the deliverable is *legible non-survival* — the chiselled-out name whose visible gap *carries the condemnation*. "Recoverable compression preserves the erased content" is precisely the breach. Erasure as a *salience amplifier*, an authored object, not a loss.

### [Leo Strauss — *Persecution and the Art of Writing* (SEP)](https://plato.stanford.edu/entries/strauss-leo/) — the access gate lives in the *reader*
Under hostile actors, full public legibility is the failure mode. The same artifact is built to be read as *different, even opposed* propositions depending on the reader's qualification — stratification achieved not by encryption or permissions but by **rhetorical construction**, with the decryption key being the reader's own cultivated capacity. Zoom-by-entitlement, not zoom-by-detail — a slot the kernel's single globally-readable projection lacks.

## G8 — The legitimacy critique (the angle every prior round was built to skip)
*Not "is the mechanism wrong" but "should this exist, who controls it, and what does it erase?"*

### [Michael Polanyi — *The Tacit Dimension* (1966)](https://library.oapen.org/handle/20.500.12657/48467) — "we know more than we can tell"
The deepest hit on "minimize private reconstruction." The most load-bearing knowing is **tacit** — the *from-which* you can only point *from*, not *at*; the instant you make it focal/explicit it stops functioning. Private reconstruction isn't a defect to minimize — it's *the mode in which understanding exists at all*. And the kernel's escape hatch ("compressed but recoverable") fails on irreversibility: you cannot losslessly fold a tacit integration back out of an explicit record.

### [Bowker & Star — *Sorting Things Out* (1999)](https://museumofdata.org/wp-content/uploads/2019/03/Bowker_and_Star_-_Sorting_Things_Out_Intro.pdf) — classification is an exercise of power, and it *tortures* what doesn't fit
The violence of classification happens **at the moment of declaration** — forcing a knower to pre-shape their knowing into the schema's boxes before it counts (their word is **torque**: the twisting when a formal category pulls against a lived trajectory; apartheid reclassification is their extreme case). Aimed straight at C4 (declared > inferred) and the privileged Q→C→E→D→R→F ontology: choosing which nouns are first-class is itself an unversioned, unaccountable act that produces a residual "not-elsewhere-classified" bin for everything human.

### [James C. Scott — *Seeing Like a State* (1998)](https://en.wikipedia.org/wiki/Seeing_Like_a_State) — legibility *destroys* the local knowledge it tries to capture
The sharpest finding of the round, because it's an *internal contradiction*, not an external critique. The back-arrow — rewrite every local act of knowing into Rama's schema before it counts — *is* Scott's central state register. His empirical finding: this doesn't capture local knowledge (**metis**), it **destroys** it. Brasília and the dead *Normalbaum* monoculture forest are lawfully-legible and *uninhabitable*. So Softland's soul axiom (habitability) and C3 (legibility-to-Rama as ground truth) can be **opposites** — maximizing the latter strangles the former.

### [Donna Haraway — "Situated Knowledges" (1988)](https://www.academia.edu/38004894/Situated_Knowledges_The_Science_Question_in_Feminism_and_the_Privilege_of_Partial_Perspective) — the "god trick," rendered as architecture
A single primary log from which every situated view is "just a projection" is exactly the "god trick of seeing everything from nowhere" — Rama positioned as the *unmarked, unlocatable* vantage. Haraway: objectivity is **situated and accountable** ("irresponsible means unable to be called into account"), and the store itself is a *marked position*, not a view from nowhere. Provenance is bookkeeping; accountability is ontological — and the kernel has the former, not the latter.
---

# ROUND 4 — The Formal & Experiential Sciences

**What this round hunted.** Having left the humanities corpus in Round 3, Round 4 took the kernel to the sciences it had never consulted: phenomenology of skill, learning science, metalogic, quantum foundations, control theory, information thermodynamics, process biology, affective neuroscience, information economics. The expectation was that physics and logic would be most lethal. **The opposite happened** — and the inversion is the round's real finding. The heaviest formal theorems (Tarski, the quantum no-go cluster, observability, Landauer) almost all *glanced off*, every one for the same reason: **the kernel is the wrong *type* of object** — it's classical, discrete, an append-only record, energetically trivial — so a theorem about non-commuting observables or continuous dynamical systems finds nothing to grip. When the strongest hammers miss for a principled reason, that's a *stability signal*. The genuine damage instead came from a soft humanistic cluster — learning science and the phenomenology of skill — and it landed on the kernel's *engineering slogan* ("minimize private reconstruction"), which turned out to be **your own taste**. The two formal sources that *did* land were **constructive** — they named repairs Round 3 had left open.

## H1 — Embodied & enactive cognition: knowing that dies when made addressable
*Is load-bearing understanding constituted by an agent's sensorimotor enactment — so it can't be streamed back or reconstructed from a record?*

### [Dreyfus & Dreyfus — "A Five-Stage Model of Skill Acquisition" (1980)](https://apps.dtic.mil/sti/tr/pdf/ADA084551.pdf) — expertise is non-representational, and articulating it *taxes* it
The kernel's whole ontology is representational — everything that exists can be recorded and re-presented. Dreyfus names a dominant form of knowing that *isn't*: expert situational discrimination. The Kaplan experiment shows expert competence runs with the verbalizable channel *fully occupied* — so the emitted reasoning is a *detachable side-channel*, not the source of the skill, and forcing articulation *degrades* performance. At the tier where understanding is most load-bearing, there is no representational content for a reasoning-trail to carry.

### [Varela, Thompson & Rosch — *The Embodied Mind* (enaction)](https://mitpress.mit.edu/9780262529365/the-embodied-mind/) — the second mind must lay down its own path in walking
Enaction's most literal hit on the soul axiom. In the kernel, projection is a passive read: state pre-exists and is rendered. Enaction says the act of traversal **brings forth** the state it appears to retrieve — reading is *doing*, and private re-enactment is **irreducible, not a defect**. You can hand over the trail's artifacts but not the understanding; the visitor regrows it by doing. "Minimize private reconstruction" mistakes the constitutive act for a cost.

### [Hutchins — *Cognition in the Wild* (1995)](https://mitpress.mit.edu/9780262581462/cognition-in-the-wild/) — some truth lives in coordination, with no node to read out
Cognitive ethnography of ship navigation: much computation is carried by *coordination and physical arrangement* — relations that are neither events nor located in any node. The truth is achieved transiently *across* the assembly and is never wholly present anywhere. The kernel assumes cognition decomposes into node-local observations streamable to one sink (C3); Hutchins shows empirically it doesn't.

### [Etak navigation (David Lewis / Gladwin)](https://en.wikipedia.org/wiki/Etak_(navigation)) — fix the self, let the world stream past
A 3000-year-validated counterexample to C1's extrinsic address. Every kernel claim presupposes a *fixed exterior frame* (the container, the absolute log) with mobile content moving through it. Etak inverts the polarity: the navigator is the immovable center, the islands move, and load-bearing spatial identity is indexed to a *continuously-updated body-state* with **no exterior absolute address** — identity you can only point *from*, not *at*.

### [Merleau-Ponty — motor intentionality (Jackson, 2018)](https://philarchive.org/archive/JACMMC/1000) — *solicitation*: being-drawn-toward, which is not an emitted event
The kernel's ontology is emitted acts (ActionRequest/KernelEvent) and their results. Merleau-Ponty names **solicitation** — the world's *pull* on a prepared body, a pre-act "being-summoned-to-act" that is intentional yet not an event the agent emits. An event-sourced kernel can record the act and the result but has no slot for the directedness that precedes them — and it's exactly that bodily "set" that a feelings-first inhabitant lives in.

## H2 — Constructivist learning science (the angle aimed straight at you)
*Can understanding be transmitted at all, or only occasioned — is the learner's effortful re-derivation the understanding itself, not waste?*

### [Rozenblit & Keil — "The Illusion of Explanatory Depth" (2002)](https://pmc.ncbi.nlm.nih.gov/articles/PMC3062901/) — a richer projection manufactures *false* understanding
The most kernel-literal of the learning sources. For *causal/explanatory* content — which is exactly Softland's payload (why a claim holds, how a trail reached its conclusion) — exposure to a vivid projection raises the *feeling* of understanding while the ability to actually reproduce the mechanism stays flat until the visitor reconstructs it themselves. And the richer the provenance display, the *stronger* the illusion. There's a quantity the kernel measures nowhere: the gap between displayed and internalized understanding — calibration error, located in *whose head it's in*.

### [The generation effect (McCurdy et al. meta-analysis, 2020)](https://link.springer.com/article/10.3758/s13423-020-01762-3) — a dose-response curve in the kernel's face
The data run *dose-response*: **lower** generation constraint — i.e. **more** private reconstruction by the reader — yields a **larger, longer-lasting** memory benefit. Minimizing reconstruction is *literally the manipulation that degrades durable retention*. The kernel's universal criterion optimizes toward the read/high-constraint pole — the empirically worst condition for understanding to actually take root in a visiting mind.

### [Bjork & Bjork — desirable difficulties (2011/2020)](https://www.unh.edu/teaching-learning-resource-hub/sites/default/files/media/2023-06/itow-introducing-desirable-difficulties-into-practice-and-instruction-bjork-and-bjork.pdf) — retrieval is a more powerful event than restudy
The storage/retrieval asymmetry: durable understanding (storage strength) is *structurally unobservable* from any readout and *inversely coupled* to the only thing you can observe (fluent access). The act of reconstructing from memory is "a more powerful event than restudying," and *lowering* accessibility is what makes the next reconstruction durable. A system whose explicit goal is to minimize reconstruction is optimizing the wrong variable.

### [Ackermann — Piaget's Constructivism, Papert's Constructionism](https://learning.media.mit.edu/content/publications/EA.Piaget%20_%20Papert.pdf) — the projection transmits a sand castle, not the act of building one
If the learning sciences are right, Softland's central promise — render the log into a public form a visitor inhabits and thereby *understands* — is the **conduit metaphor** Piaget says "won't do." An external representation produces understanding *only in the mind that constructed it*. This is the round's clearest statement of why the soul survives but the engineering slogan breaks: it restores your own value ("do the work, not know it") *as theory, not preference.*

## H3 — Self-reference & metalogic (and the one constructive landing)
*A self-modifying world ("the refactor is self-modification") needs rules-about-rules exempt from themselves. Can a single-stratum kernel host that?*

### [Hart — *The Concept of Law*: the Rule of Recognition](https://en.wikipedia.org/wiki/Rule_of_recognition) — the truth-criterion can't be a fact *in* the system it grounds
The strongest, cleanest landing of the round — and it's *constructive*. C2 claims every fact is a projection of the log. Hart shows this must fail at exactly one point: the proposition "the committed log is what counts as a fact" is the kernel's **rule of recognition**, and by a regress argument it cannot itself be validated, versioned, or revised by the kernel's own machinery. It is **accepted exogenously** — a social fact among minds (Hart's *internal point of view*), never a row in any depot. This names the un-versionable foundation the canon's "everything is a versioned object / the refactor is self-modification" slogans can't reach — and unifies it with the place thesis (a place is shared only because its inhabitants accept the same ground criterion of what is real there).

### [Suber — *The Paradox of Self-Amendment* (the Alf Ross argument)](https://legacy.earlham.edu/~peters/writing/psa/sec05.htm) — a pure-derivation system can't lawfully revise its own change-rule
Ross proves that under a *pure derivation model of validity*, a supreme change-rule cannot lawfully revise itself — the move is contradictory. The kernel runs exactly a pure-derivation model (truth = projection of the log), so it cannot host "the refactor is self-modification" without hitting Ross's contradiction. The only escape is validity-by-*acceptance* (Hart again) — a meta-stratum the single-stratum kernel doesn't have.

### [Tarski's Undefinability Theorem](https://en.wikipedia.org/wiki/Tarski's_undefinability_theorem) — truth isn't definable at the level it judges
Judging the truth of statements requires a language *strictly more expressive* than the one judged, and the ascent is unbounded. The kernel's flat single-stratum design presumes "what counts as accepted truth" can be computed within the same fact-space it judges. *(This one is more rhyme than bite — the kernel is a record, not a deductive theory satisfying the diagonal lemma — but it points at the same missing meta-stratum Hart and Ross name.)*

### [Russell's Vicious-Circle Principle & Ramified Types](https://plato.stanford.edu/entries/russell-paradox/) — an index for *definitional order* the kernel lacks
The kernel has rich indices for *when* (C5), *which-version* (C1), *causal-when* (C7) — but none for **definitional order**: is this grouping/edge/projection defined *predicatively* (without quantifying over a totality it belongs to) or *impredicatively*? A renovation/JIT-authored grouping defined by a predicate over "all groupings" is impredicative, and the kernel has no order-index to detect or stratify it.

## H5 — Quantum & relational foundations (the cluster that bounced)
*Is there an observer-independent fact to centralize at all?* The honest answer for Softland: **these don't bite** — agent observations of code and text are classical and co-measurable, so a joint table always exists. But each one supplies an *assumption-accounting template* worth keeping.

### [Brukner — "A No-Go Theorem for Observer-Independent Facts" (2018)](https://arxiv.org/abs/1804.00749) — names the hidden assumption: observations are co-measurable and passive
The theorem doesn't refute C3 for classical observations. What it does is expose C3's silent assumption: that there is always a single global joint table where "what A saw" and "what B saw" coexist and reconcile — which holds only while agents observe *inert* artifacts and recording doesn't disturb the recorded.

### [Relational Quantum Mechanics (Rovelli, SEP)](https://plato.stanford.edu/entries/qm-relational/) — comparison is itself an interaction
RQM's keepable idea: reconciling two perspectives is *itself an event* that creates a new observer-indexed fact, not a side-effect-free window onto a shared ledger. The kernel's data model is already structurally relational (per-agent observations + preserved disagreement) — but C3 then promotes one store to absolute arbiter, re-privileging exactly the absolute frame RQM abandons.

### [The Kochen-Specker Theorem (SEP)](https://plato.stanford.edu/entries/kochen-specker/) — some values don't pre-exist the act that elicits them
Dramatizes a regime where "a single context-free value exists to be recorded" is provably empty. The strain on C4 isn't that Softland facts are quantum — it's that C4 imagines *declaration as recording a pre-existing determinate fact*, and there are domains where that assumption is simply void.

### [QBism (Fuchs & Schack, SEP)](https://plato.stanford.edu/entries/quantum-bayesian/) — owned credence with no agent-neutral reduction
A conceptual reclassification, not a theorem: *if* agent observations are credences — and LLM outputs literally **are** confidence-weighted, owner-bearing claims — then the "primary log" is a pool of beliefs, not a pool of agent-neutral facts-in-waiting, and there may be no canonical merge into a shared fact.

## H6 — Cybernetics & control (the dynamics layer the kernel admits it lacks)
*The kernel's valid-time is a static stamp. Is it missing a regulation layer — belief that decays, with bounds on what's recoverable?* These mostly rhyme rather than bite (the kernel isn't a continuous dynamical system), but they name the admitted gap precisely.

### [Observability & State Estimation (Stanford EE363)](https://web.stanford.edu/class/ee363/lectures/011_observ.pdf) — there's a computable ceiling on what observation can reconstruct
C3 tacitly assumes truth is always reconstructable from enough observations. Observability theory proves a computable *upper bound* on what's reconstructable at all — and even an unbounded observation volume can leave part of the state permanently unknowable. Truth as a point *plus an uncertainty ellipsoid*, not a crisp value.

### [Ashby — "Requisite Variety" (1956)](http://panarchy.org/ashby/variety.1956.html) — a regulator needs as much variety as the world it regulates
By the law of requisite variety, the world-variety Rama can faithfully represent is bounded by Rama's *schema* variety; the surplus passes through unregulated and "truth" silently diverges. The kernel has rich nouns for *storage* but none for the *controller's capacity measured against the variety of the world it claims as truth*. (The CALM theorem in Round 5 is the rigorous, biting version of this.)

### [Second-order cybernetics (von Foerster)](https://en.wikipedia.org/wiki/Second-order_cybernetics) — the back-arrow is a *loop*, not a gradient
The kernel asserts a clean one-way truth gradient (observe → report → store → read). But an agent reads Rama, decides, emits a KernelEvent — and that event is the next thing it and other agents observe. That's **circular causality**, not a gradient; identity is an *eigenform* (the fixpoint of an observe-act loop), not a peg given prior to observation.

### [Lyapunov stability](https://en.wikipedia.org/wiki/Lyapunov_function) — validity is a trajectory with a decay rate, not a frozen stamp
A single "valid" stamp is a snapshot of a quantity whose reliability is *itself time-varying* — uncertainty grows between observations; estimates must be re-excited. The kernel treats validity as a property *of a value at an instant*; dynamics treats it as a trajectory with a decay rate.

## H7 — Thermodynamics of information & the second constructive landing
*Is materialization free and reversible (the kernel's unstated assumption)?*

### [Inverse problems / Hadamard well-posedness](https://en.wikipedia.org/wiki/Inverse_problem) — the missing *regularizer/prior* (the round's other real landing)
The constructive twin of Round 3's AGM finding. "All materialized state is a recoverable projection" is safe **only for injective (view-like) projections**. A non-injective *aggregating* fold severs information *by the structure of the map itself* — its pre-image is a whole affine subspace of equally-log-consistent reconstructions, and the **regularizer/prior** that selects *which* one is **provably not in the data**. The kernel models only the *constraint* side (events constrain the possible); it has no noun for the *selection* side.

### [60 Years of Landauer's Principle (Nature Reviews Physics)](https://www.nature.com/articles/s42254-021-00400-8) + [Bennett's Notes](https://arxiv.org/abs/physics/0210005) + [Bérut et al. experimental verification (2012)](https://www.nature.com/articles/nature10872) — forgetting has a physical price; merging is the irreversible act
The kernel models logging/projecting/retaining as free. Physics supplies a conserved currency: erasing a bit costs `k_B·T·ln2`, and **logical irreversibility lives in *merging*, not only in deletion**. The keepable, non-metaphorical takeaways: append-and-project genuinely *are* cheap (the kernel sits in the near-zero reversible regime) — but "keep everything forever" is not a free steady state, it's an *unclosed cycle* whose bill is paid in unbounded storage; and the type-distinction the kernel's single verb "fold" erases — *injective* (reversible, cheap) vs *many-to-one* (irreversible) — is exactly the one that should govern governed-forgetting.

## H8 — Process metaphysics & the biology of persistence
*Is identity a stored container, or a pattern actively maintained by flux?*

### [Process Philosophy / Whitehead (IEP)](https://iep.utm.edu/processp/) — an enduring object is a *society of occasions*, not a vessel
Round 3's sharpening ("identity = intrinsic invariant") survives *only* if cashed out as an invariant *of the pattern/route* the society transmits — not as a vessel the route is stored in. The kernel's container is an addressing convenience the implementation reifies into an ontological primitive; Whitehead's argument is that becoming is primary and the "container" is derived.

### [Autopoiesis (Varela, Maturana & Uribe, 1974)](https://www.sciencedirect.com/science/article/abs/pii/0303264774900318) — the kernel is *allopoietic*, and the canon calls its cargo *living*
Maturana's organization/structure distinction is the kernel's deepest blind spot. The kernel versions *structure* beautifully — but a living unity conserves its **organization** while *totally replacing* its structure. A kernel object is the textbook *allopoietic* triple (product-other-than-producer, organization-specified-from-outside, boundary-not-self-made), so C1 is category-correct for *inert artifacts* — but the canon insists its real cargo is *living* ("grows with its inhabitants," "the refactor is self-modification"), which is autopoietic, and the kernel has no vocabulary for that.

### [Schoenheimer — "The Dynamic State of Body Constituents" (1942)](http://jn.nutrition.org/content/121/11/1701.full.pdf) — persistence *because* the substrate is continuously replaced
The kernel equates "unchanged" with "same stored bits." Schoenheimer's lab fact: for a living system, persistence **is** total replacement — identity is a maintained flux equilibrium, not a stored substrate. A regime the kernel's "no event = no change" corollary can't represent.

### [von Uexküll — Umwelt theory](https://en.wikipedia.org/wiki/Jakob_Johann_von_Uexk%C3%BCll) — two agents don't observe the same object differently; they observe *different* objects
What is perceivable is constituted by the perceiver's receptor/effector scheme, not a property of the object. So two agents with different tool-schemes don't perceive the same Rama-object differently — they perceive **different carriers**. C3's "stream observations of one shared world back to one store" assumes a shared world the biology denies.

## H9 — Affect as epistemic (the other angle aimed at you)
*Is feeling load-bearing information — so a purely propositional substrate can't hold understanding the way a feelings-first mind has it?*

### [The Somatic Marker Hypothesis (Bechara & Damasio, 2005)](https://people.ict.usc.edu/~gratch/CSCI534/Readings/The%20somatic%20marker%20hypothesis.pdf) — the felt signal arrives first and performs better
The sharpest hit in the cluster. The somatic marker is a non-propositional, experientially-acquired *valence* signal that is a *necessary input to judgment* and operates *before* any declarative content. Damasio shows normal subjects choose advantageously in the *pre-hunch* period before they can articulate why, while patients with full declarative knowledge but damaged affect choose disastrously. The kernel's Q→C→E→D→R→F ontology is exhaustively propositional — it has no field for the thing that, for a feelings-first inhabitant, *arrives first*.

### [Epistemic Feelings are Affective Experiences (Loev 2022; Dokic 2012)](https://journals.sagepub.com/doi/10.1177/17540739221104464) — the felt frontier of inquiry
The non-propositional, graded affective signal — the felt sense of "rightness," of "almost there," of a question being live — is the interface between implicit competence and explicit reasoning. Loev's misattribution finding: strip the *felt* valence and the epistemic capacity is *destroyed*, while the unconscious propositional cue can't even be used because it isn't conscious. A log of typed nodes cannot carry the vehicle the epistemic stance actually rides on.

### [The Theory of Constructed Emotion (Barrett, 2017)](https://affective-science.org/pubs/2017/barrett-tce-scan-2017.pdf) — the only system that *does* semantic zoom compresses lossily, by relevance
The one existing engine that actually performs semantic zoom — the brain — compresses **lossily and interest-relatively**, organized by *allostatic relevance*, discarding everything not budget-relevant (detached accurate modeling is "metabolically reckless"). That's independent confirmation that real zoom is necessarily lossy and *goal-indexed* — the flagship's "lossless across all axes" describes nothing that exists. And the same content *means something different to read* depending on the reader's bodily state — affect as a *prior on categorization itself*.

## H10 — Decentralized computation & the untranslatable
*Is centralizing dispersed knowledge even possible — and does meaning cross between frames losslessly?*

### [Hayek — "The Use of Knowledge in Society" (1945)](https://www.econlib.org/library/Essays/hykKnw.html) — a price is deliberately *non-recoverable* compressed knowledge
The information-economics argument against the back-arrow, stronger than Scott's empirical one. A price is a compressed scalar computed at the periphery whose *entire job* is to let a local actor act correctly **without** ever knowing or recovering the global state that produced it. The kernel has no name for state that is *deliberately non-recoverable* — a signal whose value is precisely that the originating particulars are *severed and never reassembled*. The upward channel is lossy *in principle*: aggregation "abstracts from minor differences," and those differences are the content.

### [Quine — Indeterminacy of Translation ("gavagai")](https://iep.utm.edu/indeterm/) — at the hardest modality, reference doesn't survive
The flagship axiom claims identity/relations survive a *modality* transform. Quine shows that at the hardest case — natural language to natural language — reference provably does **not** survive: distinct, incompatible reconstructions remain consistent with *all* the evidence. So "relations survive across modality" is false exactly where it's most testable, and another mind reading the compressed form can't recover a unique original.
---

# ROUND 5 — The Constructive Round

**What this round hunted.** The first four rounds were *destructive* — they aimed sources *at* the kernel to break it. Round 5 inverted the stance: it went to the disciplines the kernel is *built from* — category theory, distributed systems, database provenance, social choice, information theory, causal inference — chosen because they would *agree* with the kernel and hand it **literal formal structure** to repair the holes the earlier rounds opened. The question per source was no longer "where does this break us?" but "what *named theorem or primitive* does this give us — does it **harden** a claim, **bound** it, or supply a **missing piece**?" It delivered: every surviving claim now has a named formal home. The catch it also surfaced: the kernel is now *over-supplied with formal homes but not yet an instance of any of them* — the remaining work is a handful of **encoding commitments**, not more research.

*(Twenty-eight sources across seven angles below. The eighth angle I'd planned — adversarial & success-pathology epistemics, i.e. what breaks when the kernel* succeeds *— got capped out and returned no reads; it remains a real, un-run gap, noted in Open Problems.)*

## I1 — Category & sheaf theory (your own worldview, finally turned on the kernel)
*"One ontology, many projections" is a functor claim; "the local world is the atomic unit" + preserved disagreement is the sheaf question — when do locally-consistent sections glue into a global one, and when do they provably not?*

### [Abramsky & Brandenburger — "The Sheaf-Theoretic Structure of Non-Locality and Contextuality" (2011)](https://arxiv.org/abs/1102.0264) — a single merged account exists *iff* the disagreement is non-contextual
The literal structure for C8's "some disagreement is constitutively terminal." Model local worlds as **local sections over a measurement cover**; a compatible family extends to a **global section** (a single merged account) *iff* the model is non-contextual. So "can these perspectives be synthesized?" stops being a vibe and becomes a checkable property of the diagram — and "no global section exists" is a *real, provable* state, not a failure to try hard enough.

### [Abramsky, Mansfield & Barbosa — "The Cohomology of Non-Locality and Contextuality" (2011)](https://arxiv.org/pdf/1111.3620) — a *certificate* that a disagreement can't be reconciled
Goes one better: it computes a **cohomological obstruction class** (a Čech H¹ class over the cover of contexts) that is a *sound one-sided certificate of non-gluability*. For Softland: when agents genuinely can't be merged, you can **store the proof of that**, rather than leaving "we couldn't synthesize" as an unexplained gap. (Honest bound, carried from the research: a *zero* obstruction is *inconclusive* — it doesn't prove synthesis is possible — so the certificate only ever certifies the negative.)

### [The HoTT Book — *Homotopy Type Theory* (2013)](https://homotopytypetheory.org/book/) — identity as a *path*, revision as path-composition
Speaks directly to C1. In HoTT, identity is the type of **paths** between two points; a revision is a path constructor, a chain is path composition, and univalence says equivalent structures are *identical*. It's the formal version of Round 4's "identity is a route, not a vessel." The honest catch: the kernel is **directed** (append-only, no inverses) — a *free category*, not HoTT's invertible groupoid — so you can borrow the path/identity-type framing but not full univalence.

### [Leinster — *Basic Category Theory* (2014)](https://arxiv.org/pdf/1612.09375) — lawful zoom *is* a functor; a handoff *is* a colimit
The cleanest fit to your stated worldview. **Lawful zoom = a functor** (preserves identity and composition of relations); "relations survive" = the functor is **faithful** — and a zoom-out functor *generically isn't* faithful, which is exactly why the flagship axiom is false and a lossy zoom is still *lawful*. And a grouping/merge/handoff (C6) is a **colimit** of the diagram of member worlds and their overlaps — a coproduct when disjoint (clean handoff, no coupling), a pushout when glued. One sharp warning it also gives: a colimit *collapses* its inputs into one object, so it's the right operation for *preserving* a diagram but the *wrong* one for a synthesis that must hold both sides.

## I2 — Distributed-systems consistency (the exact condition the back-arrow needs)
*When is a single authoritative store necessary vs unnecessary — and is "semantic time is a DAG" literally a causal clock?*

### [Hellerstein & Alvaro — "Keeping CALM" (2020)](https://arxiv.org/abs/1901.01930) — the precise gate: coordination is needed *exactly* at non-monotone operators
The CALM theorem gives C3 a sharp boundary: a computation is consistent *and* coordination-free **iff it is monotone**. So tag every Rama fold with a **monotone? bit** — monotone folds (accumulating observations) need *no* arbiter and converge freely; non-monotone folds (delete, redact, aggregate-as-truth, arbitrate, *synthesize*) **provably require a coordination seal** before their output counts as truth. This is the formal license for "the back-arrow is estimation on the monotone part, sealed authority on the rest."

### [Shapiro et al. — Conflict-Free Replicated Data Types (2011)](https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type) — the merge primitive: a join-semilattice least-upper-bound
The missing convergent merge operator the kernel needs (encoding decision #1). A state-based CRDT makes a materialized PState a **join-semilattice** whose merge is the *least upper bound* (⊔) — provably **commutative, associative, idempotent** — so concurrent, incomparable agent-claims are *provably never silently lost* and converge with no coordinator. This is Round 2's convergence problem solved *with the monotonicity gate attached* (it's the part that's safe to do without a seal).

### [Lamport (1978) + Fidge/Mattern vector clocks](https://lamport.azurewebsites.net/pubs/time-clocks.pdf) — C7's DAG, named
C7's "semantic time is a DAG" *is* Lamport's **happened-before** strict partial order, exactly characterized by the **vector clock**: `VC(a) < VC(b)` iff a→b, incomparable iff concurrent. And Lamport's converse-failure result is the bound: a single scalar commit-offset **cannot** certify causality or detect concurrency — it's a tiebreak only. So the kernel must *not* read its commit counter as a causal signal.

### [Bailis et al. — "Coordination Avoidance in Database Systems" (2015)](https://arxiv.org/abs/1402.2237) — *Invariant Confluence*, which theorem-justifies a decision you already made
I-confluence gives a per-invariant decision procedure for when an operation can run coordination-free. Its sharpest gift: **identity-uniqueness is provably *not* I-confluent** — so minting a unique identity under concurrency **must** be a single-point operation. That is *exactly* why Slice-A mints `run-id` before `foreign-append!`. The research independently re-derived your own correction from a theorem.

## I3 — Provenance semirings, bitemporal databases & diplomatics (the kernel's house discipline)
*How does provenance propagate through a projection, when does lineage stay recoverable, and what are the two times really?*

### [Green, Karvounarakis & Tannen — "Provenance Semirings" (2007)](https://web.cs.ucdavis.edu/~green/papers/pods07.pdf) — the exact algebra of which lineage survives a projection
Gives C2 a precise account of recoverability. Annotate every event with a **provenance polynomial** (the free commutative semiring N[X]); projections are semiring **homomorphisms**, and lineage is recoverable *iff* the evaluation semiring is N[X] — lost *exactly* at each non-injective homomorphism. So "this fold severs information" stops being hand-wavy: **semiring annihilation** is the formal name for the non-injective fold's loss.

### [Cheney, Chiticariu & Tan — "Provenance in Databases: Why, How, Where" (2009)](https://homepages.inf.ed.ac.uk/jcheney/publications/provdbsurvey.pdf) — why the distortion budget *must* be multi-axis
The survey behind the semiring model, with one decisive result for the flagship repair: **where-provenance is provably *not* derivable from how-provenance** (Prop 5.12) — two how-equivalent queries can have different where-provenance. So a single global "fidelity" number is impossible; the distortion budget **must** carry at least two independent axes (derivation-structure *and* copy-location).

### [Jensen, Snodgrass & Soo — the Bitemporal Conceptual Data Model / TSQL2](https://people.cs.aau.dk/~csj/Thesis/pdf/chapter12.pdf) — C5, finally labeled correctly
The cleanest single source of the round (and the one with no overreach). It hands C5 the canonical definitions Round 3 found mislabeled: **transaction time** = append-only log position (monotone, ≤ now); **valid time** = user-assertable truth interval (may be past or future). Drop "committed/valid" — it fused an act with an assertion. It also supplies coalescing (lossless compaction) and the until-changed marker for the append-only transaction-time relation Rama's depot already *is*.

### [Duranti — Diplomatics / InterPARES on reliability, authenticity, accuracy](https://www.interpares.org/display_file/ip1-2_dissemination_ls_duranti_lucas_2007.pdf) — splits the one trust axis the kernel mis-fused
Archival science decomposes "trustworthy" into three orthogonal axes the kernel currently collapses into C4's binary: **reliability** (competence of author × controls at creation, established *a priori*), **authenticity** (identity × integrity in custody, *a posteriori*), and **accuracy** (content). The killer line for C3: append-only custody guarantees only **authenticity** — *the un-tampered record of what was claimed* — and *never* reliability. "Ground truth" is truth-about-custody, not truth-about-the-world.

## I4 — Social choice, judgment aggregation & paraconsistency (is late-bound synthesis even possible?)
*Is C8's "synthesize the preserved plurality into one accepted fact" achievable, or is there an impossibility theorem against it?*

### [List & Pettit — "Aggregating Sets of Judgments: An Impossibility Result" (2002)](https://researchonline.lse.ac.uk/704/1/List_Econ&Phil_18(1).pdf) — a theorem against C8's telos
On a *logically connected* agenda, **no** synthesis operator can simultaneously be universal-domain, collective-rational (consistent + complete), anonymous, and systematic — the discursive-dilemma/doctrinal-paradox generalization. So "synthesis is always eventually reachable" is *provably false* in general. The constructive corner it also gives: a **premise-based** procedure (relax systematicity) is an escape route — synthesis *can* succeed if you declare a priority ordering on which claims are foundational.

### [Dietrich & List — "Arrow's Theorem in Judgment Aggregation" (2007)](https://researchonline.lse.ac.uk/5817/1/Arrows_theorem_in_judgement_aggregation_(LSERO).pdf) — *exactly when* the impossibility bites
Sharpens the bound: the impossibility bites only on agendas that are **non-simple** (contain a minimal inconsistent set of ≥3 claims) and path-connected. This is decision-useful for Softland: *whether* the discourse graph's typed edges (supports/contradicts) form such an agenda determines whether late-bound synthesis is sometimes *provably impossible* — or whether simple per-claim folds are perfectly fine. (An open question, flagged below.)

### [Paraconsistent Logic (SEP) — Belnap-Dunn FDE & Priest's LP](https://plato.stanford.edu/entries/logic-paraconsistent/) — how to hold a contradiction without the projection exploding
C8 keeps mutually contradictory claims live — but classical logic *explodes* on contradiction (anything follows from `A ∧ ¬A`). The fix is a **four-valued standing** {true, false, **both/glut**, **neither/gap**} with a **non-explosive** consequence relation: a glut is a first-class *terminal* status that propagates only along typed defeat edges, so one poisoned contradiction can't trivialize the whole graph. The kernel needs this the moment it holds live counter-claims (which is its whole premise).

### [Sentz & Ferson — Dempster-Shafer evidence combination](https://www.stat.berkeley.edu/~aldous/Real_World/dempster_shafer.pdf) — a merge-gate that flags un-poolable sources
For C4/C3's corroboration step: the **conflict mass K** is a computable scalar measuring how much of two sources' belief lands on contradictory ground — and Zadeh's counterexample proves that when K→1, naive pooling produces *absurd* results. So K is a **merge-gate**: it tells you when two sources *must not* be combined. (This is the confidence-*boosting* combine the idempotent provenance-semiring algebra provably *can't* do — which is the open fork on the corroboration algebra.)

## I5 — Information-theoretic compression & scale (the math under the dead flagship)
*The flagship axiom is refuted — what's the actual math of what must be discarded, and what survives a scale change?*

### [Rate-Distortion Theory (Shannon, 1959)](https://en.wikipedia.org/wiki/Rate%E2%80%93distortion_theory) — the formal distortion budget
Quantifies Round 3's "declared distortion budget" repair. The rate-distortion function R(D) is the exact achievable frontier: minimum bits to represent a source within distortion D, with R(0) = full entropy and R(D_max) = 0. Each zoom axis carries a declared distortion measure and budget; the map literally cannot promise more fidelity than R(D) allows. (Honest bound: the *measurable contract* is adoptable; the absolute bit-floors aren't computable without a source distribution — encoding decision #5.)

### [The Renormalization Group](https://en.wikipedia.org/wiki/Renormalization_group) — what survives a zoom is the small set of *relevant* variables
The physics of zooming out: coarse-graining sorts couplings into **relevant / marginal / irrelevant**, and "most observables are irrelevant" — systematically *forgotten* under zoom-out, with only the small relevant set (the universality class) surviving. So the right invariant for semantic zoom is **the relevant-variable set**, not "all relations." This is the constructive form of the flagship's overturning: zoom-out *correctly* discards almost everything.

### [Minimum Description Length (Rissanen)](http://www.scholarpedia.org/article/Minimum_description_length) — a formal promotion gate for declared-vs-inferred
Gives C4 a principled test for promoting inferred structure to declared: promote S iff it **shortens the total two-part code** L(model) + L(data | model). "Declared > inferred" becomes "declared = the structure that pays for itself in compression" — a graded, computable criterion instead of a binary gate.

### [Kolmogorov Complexity & the Incompressibility Theorem](https://en.wikipedia.org/wiki/Kolmogorov_complexity) — the hard floor under "recoverable compression"
The counting argument: for all but a vanishing fraction of strings, *no* lossless description shorter than the string exists. So lossless semantic zoom of generic content is **impossible** — which is why the kernel's rule must be "re-project from the retained fine state, never invert a baked artifact," and why log-discarding pipelines are forbidden.

## I6 — Causal inference (what an observational log can and cannot reconstruct)
*Agents* act *(intervene), but the log records* observations*. What causal structure can a fold of observations reconstruct?*

### [The Causal Hierarchy Theorem (Bareinboim et al.)](https://causalai.net/r60.pdf) — an intervention is a *different type of event* than an observation
Pearl's ladder makes a distinction C3 blurs: an agent's **intervention** (a `do`-event, Layer 2) is a *typed object distinct* from a passive **observation** (Layer 1), and by the Causal Hierarchy Theorem the former generally *cannot* be inferred from a pile of the latter. So in Softland, agent *actions* must be **recorded as do-events**, not reconstructed by folding observations.

### [do-calculus completeness (Huang & Valtorta; Shpitser & Pearl, 2006)](https://arxiv.org/abs/1206.6831) — a per-query gate on what's recoverable
do-calculus is *complete*: there's a decision procedure that, for any causal query, either returns a closed-form estimator from observational data or proves it **unrecoverable**. That's an **identifiability gate** on causal-reconstruction folds — the kernel can know, per query, whether the trail can answer it at all.

### [The Back-Door Criterion & d-separation (Pearl)](https://en.wikipedia.org/wiki/Do-calculus) — and the bound on C7's DAG
d-separation is the path-blocking calculus that says which trail-facts are conditionally independent — but it only works on a **structural causal graph with confounder edges**, which is *not* the same as C7's happened-before DAG. So the bound: C7's DAG is temporal-causal *precedence* only; reading it as a structural causal model (to answer "what caused what") requires a *separately declared* graph.

### [Danto — "Narrative Sentences" (philosophy of history)](https://abuss.narod.ru/Biblio/eng/danto_narrsentences.htm) — some truths about an event can only be told *later*
A precise structural limit on "papers as logs / replay how understanding was built." A **narrative sentence** is a true description of an event whose truth-condition references *later* events — so it is **unassertible by any witness or fold confined to commit-time** (Danto's Ideal Chronicler can't write it). Some true facts about a logged event were *never* in any fold up to that commit; the log can't be the sole bearer of historical meaning.

## I7 — The felt/embodied channel & how it actually transmits (closing the soul)
*Round 4 proved the felt channel is absent and re-enaction is constitutive. How do real communities transmit embodied understanding — what should Softland build* around *the gap?*

### [Goodman — *Languages of Art*: autographic vs allographic](https://plato.stanford.edu/entries/goodman-aesthetics/) — a formal test for what *can* be losslessly handed off
Gives the soul axiom a precise boundary: a work is **allographic** (losslessly transferable by its declared notation — like a musical score) iff it meets Goodman's notationality criteria; otherwise it's **autographic** (identity bound to its production history — like a painting). So Softland can *test* each trail: the allographic part transmits as truth; the autographic/felt residue is **constitutively non-copyable** — point at it, don't pretend to serialize it.

### [Collins & Evans — Interactional Expertise](https://en.wikipedia.org/wiki/Interactional_expertise) — the ceiling of what a linguistic record can carry
Names the exact ceiling: a purely linguistic record can confer **interactional** expertise (fluency in the practice's *discourse*) but never **contributory** expertise (the capacity to *perform* it). So "minimize private reconstruction" is bounded to the *discourse*, never the *skill* — and a reasoning trail, however rich, hands over the ability to *talk about* the work, not to *do* it.

### [Gibson — The Theory of Affordances](https://en.wikipedia.org/wiki/Affordance) — the one buildable felt-adjacent primitive
The single instantiable piece of the missing felt channel. An **affordance** is a *two-place relation* — `affords-φ(feature, ability)` — between a local-world feature and a visitor's capability, *not* a unary property stored on the object. So Softland can build it as a **parametric, observer-relative query evaluated at inhabitation time** ("what can *this* mind do *here*"), never a materialized field. That's the soul's felt dimension rendered as something the kernel can actually compute.

### [Lave & Wenger — Legitimate Peripheral Participation (1991)](https://www.cambridge.org/highereducation/books/situated-learning/6915ABD21C8E4619F750A4D4ACA616CD) — scaffold re-enaction, don't promise transmission
Gives the mechanism for what to build *around* the felt-channel gap: a **legitimate peripheral participation** trajectory — a visitor enters a local world at a *real but low-stakes* periphery and moves toward full participation by *doing*. Understanding is re-enacted along that trajectory, not transmitted by record — so Softland's job is to *scaffold the traversal*, not to ship the understanding.
---

# Cross-cutting synthesis — what all 124 sources mean together

Read end to end, the four rounds tell one story. **The kernel is a sound record substrate that over-claimed its own reach.** Its mechanics survived every assault — an append-only log genuinely *is* the right spine, container-as-record is category-correct for inert artifacts, snapshots are a legitimate response to non-injective folds. What did *not* survive is the universalizing canon layered on top.

The single sentence the whole research converges on: **versioning is the root of *content-change* only; the kernel routed five other jobs through it, and each has a stronger root elsewhere.**

- **Identity** wants **addressing** — a content/intrinsic handle, not a "container" (nanopublications, Perkeep, PROV-DM, Whitehead, Leinster).
- **The projection** wants **incremental computation with a regularizer** — it drifts and needs an out-of-band prior the log doesn't contain (Build Systems à la Carte, Salsa, Materialize, AGM, inverse problems, provenance semirings).
- **Multi-agent truth (the back-arrow)** wants **estimation + independent corroboration**, not single-sink arbitration — and append-only custody buys *authenticity, never reliability* (Kalman, bank reconciliation, isnad, Shafer, CALM, diplomatics).
- **Disagreement** wants **jurisprudence + defeasible argumentation** — a binding ruling held *simultaneously* with permanent preservation, sometimes never merged by design (machloket, uṣūl, Nyāya, Hart, sheaf cohomology, judgment-aggregation, paraconsistency).
- **Semantic zoom** wants a **declared distortion budget**, not a preservation guarantee — the flagship "survive every axis or it's fake" axiom is **fatally refuted** (Gauss, Töpfer, Monmonier, rate-distortion, renormalization group, Kolmogorov).
- **Preservation** needs a **governed forgetting** primitive and a **seal** the open log lacks (Athenian amnesty, damnatio, the accounting close, Landauer/Bennett).

And the deepest finding — the one only the non-CS rounds could reach: **the "place," Softland's very soul, is a ritual/embodiment/power property a substrate can host but never manufacture** (songlines), and the back-arrow (legibility-to-Rama) is in *genuine internal contradiction* with habitability (Scott). The reflexive sting: this research *method itself* — fan out searches, pull everything into one centralized legible report — *is* the back-arrow, so it is structurally blind to exactly the tacit/embodied/restricted knowledge the kernel is blind to. The kernel's blind spot and the method's blind spot are the same blind spot.

# How this relates to *you* (not just the kernel)

Several sources turned out to be about your own stated taste, not abstract design — which is why they land harder than any theorem:

- **"I want to do the work, not know it" is now a research finding, not a preference.** The entire learning-science cluster — the generation effect's dose-response, Bjork's desirable difficulties, Rozenblit & Keil's illusion of explanatory depth, Piaget/Papert — proves that *effortful re-derivation is the understanding*, and that a richer projection manufactures *false* confidence. Your instinct ("rediscovery over consumption") is the empirically correct one, and it puts your *design center* (a place that occasions understanding) in direct tension with one *engineering slogan* ("minimize private reconstruction"). The science is on the soul's side.
- **"Feelings first" is a structural gap, not a vibe.** Damasio's somatic markers, Barrett's interoceptive priors, Merleau-Ponty's solicitation: a load-bearing, *non-propositional* felt channel arrives *before* declarative content and the brain's own semantic zoom is organized by it. The Q→C→E→D→R→F ontology is exhaustively propositional — so it structurally cannot hold understanding the way *its own intended inhabitant* has it. The one buildable piece (Gibson's affordance-as-relation) is in the spec.
- **Category theory as worldview paid off literally.** Round 5's category/sheaf theory wasn't decoration — it gave C8 and the flagship their *only* literal repairs (the gluing obstruction, the colimit, the functor/faithfulness account of lawful-vs-fake zoom). Your worldview turned out to be the right tool for the hardest claim.
- **"Softland is a place" is the best-supported axiom in the whole corpus.** Songlines are a 10,000-year existence proof that "inhabit, don't query" is the most durable knowledge medium humans ever built. The research vindicates the soul — and *bounds* it: the place must be *cultivated as practice*, not bought with a substrate, and it's inhabitable by an *entitled/prepared* mind, not an arbitrary one.

# What this changes for the kernel

The destructive findings became a constructive spec: **`build/code-ingestor/object-kernel-revision.md`** (the sibling to this walkthrough). In one breath: keep C2-as-architecture and C6 snapshots; **split C1** into addressing + a directed revision path; restate **C3** as authenticity-not-reliability + estimation-with-a-seal; **fix C5's mislabel** with the canonical bitemporal definitions; interpret **C7's DAG** as a vector-clock partial order (not a causal graph); convert **C8's** refuted telos into a four-valued, non-explosive, certificate-bearing disagreement engine; **overturn the flagship** into a declared per-axis distortion budget; **bound the soul** to its allographic layer and build the affordance primitive. Plus seven new layers (estimation back-arrow, seal/close, governed forgetting, distortion budget, felt channel, content-typed transmission, non-explosive disagreement container).

# Open problems (what the research itself leaves unsolved)

- **Five encoding commitments** stand between "hardened claims" and a spec whose theorems actually bite: (1) a declared convergent merge operator ⊔; (2) a committed claim/disagreement encoding (C8 has five competing terminal-disagreement formalizations); (3) a credential algebra + a home for confidence-*boosting* corroboration (provably *not* the idempotent semiring — the open Dempster-Shafer-vs-Bayesian fork); (4) a treatment of the kernel's actual non-monotone/non-injective folds, which sit outside every clean theorem; (5) a source distribution for the distortion budget.
- **Three residues are constitutively beyond formal reach** — declare the bound, don't chase them: the *felt/embodied channel* (Goodman/Collins prove it's non-serializable), *arbitration authority* (who may seal — a social rule-of-recognition), and C7's *"semantic" surplus* beyond happened-before.
- **The adversarial angle never ran.** What breaks when the kernel *succeeds* — data poisoning, Goodhart on the discourse types, ontology lock-in, disagreement-as-a-DoS-surface — got capped out of Round 5 and is a real, unexamined gap. A refutation-shaped run is structurally blind to it.
- **The method's own ceiling:** a web-search run reaches only published, propositional, formalized, English knowledge. It located the *edge* of the formalizable with precision; it cannot cross into the tacit/embodied/restricted by reading more sources. "Done" means declaring *at* that boundary, not treating the beyond as unfinished homework.

# The corpus at a glance

| Round | Character | Sources | The ones that moved the verdict |
|---|---|---|---|
| **2** | The CS corpus | 30 | Sun et al. (convergence ≠ intention), Build Systems à la Carte, Snodgrass (the C5 mislabel), Certificate Transparency, nanopublications |
| **3** | Leaving the corpus | 31 | Songlines (Kelly), machloket (Talmud), Theorema Egregium, Scott's *Seeing Like a State*, Polanyi, isnad |
| **4** | Formal & experiential sciences | 34 | The generation effect & desirable difficulties, Damasio's somatic markers, enaction (Varela), Hart's rule of recognition, inverse problems |
| **5** | The constructive round | 28 | Leinster (functor/colimit), CALM, join-semilattice CRDTs, BCDM bitemporal, sheaf cohomology, Goodman, Gibson |

Full per-source detail with the adversarial-verification bookkeeping (which strains held up under scrutiny, which were discounted as loose analogy) lives in the sibling `blindspot-research-rounds-2-5.md`. This walkthrough is the journey; that one is the audit.
