# Object-Container Kernel — Revision v2 (derived from the 5-round blindspot research)

> **Status:** MOSTLY spec-ready. This is a *hardened-claims* spec — every kernel claim now has a named formal home and a declared bound. It is **not yet** a "theorems-bite" formal spec: five cross-cutting **encoding decisions** (§6) must be committed before the imported theorems literally apply to the kernel as built.
> **Provenance:** distilled from `research/blindspot-research-rounds-2-5.md` (5 rounds, ~297 agents, ~11.7M tokens). Tags carry the R6 verifier's verdict: **[HOLDS]** = the kernel mechanism is literally an instance of the named formal structure; **[PARTIAL]** = the theorem is real but bites only once a stated encoding is committed; discounted loose-metaphor imports are omitted.
> **Private doc** — not committed.
> **Authoring note:** the per-claim *primitives* and *verdicts* are the research's verified output; the *ordering, the fork recommendations (§5), and the build sequencing (§8)* are my synthesis and are marked where they go beyond what the sources establish.

---

## 1. The frame (what survived five rounds)

Versioning is **not** the root primitive of Softland. It is the root of **content-diff change only**. The event-sourcing/object-container kernel is a **correct record substrate** — vindicated, not demolished: an append-only log *is* the canonical reversible computation that saves its input (Bennett), and event-primacy *is* the metaphysics of event sourcing (Whitehead). What broke was never the mechanics; it was the **universalizing canon** — "everything is a versioned object," "minimize private reconstruction," "the refactor is self-modification," and the flagship "lawful compression preserves everything" — each of which overreached past what a record substrate can carry.

The revised kernel is therefore: **a correct content-change-and-addressing core, plus a set of named formal homes for the jobs the canon wrongly routed through versioning, plus an honest fence around three things that are constitutively beyond any record.** The remaining work is not more theory — the kernel is now *over-supplied* with formal homes — it is **five encoding commitments** (§6) that decide whether those theorems bite on the kernel as built.

**Where the roots actually live** (the "is software the primitive?" map):

| Job | Root primitive | Software is… |
|---|---|---|
| Content change / history | versioning | **correct** |
| Identity | addressing / intrinsic invariant | correct (as a key) |
| Projection→state | incremental computation + a regularizer/prior | correct as architecture |
| Multi-agent truth (back-arrow) | estimation + independent corroboration | wrong (it's not arbitration) |
| Disagreement / synthesis | jurisprudence + defeasible argumentation | wrong (it's not merge) |
| Semantic zoom | declared distortion budget (cartography) | wrong (zoom must lie legibly) |
| Preservation ↔ forgetting | governed forgetting (archival/legal) | wrong (immutable log can't forget) |
| The place / habitability | ritual + embodiment + power | **can host, cannot manufacture** |
| The felt channel | — | **no software primitive exists** |

---

## 2. The revised kernel, claim by claim

Each entry: **verdict** · the **formal home** (named primitive + HOLDS/PARTIAL) · the **change** · the **bound**.

### C1 — Identity & change → **SPLIT (confirmed)**
- **Home:** BCDM time-invariant surrogate key (addressing) **[HOLDS]** · Leinster universal-property identity for derived objects **[HOLDS]** · free-category directed path algebra (versioning) **[PARTIAL]** · Goodman allographic invariant (handoff) **[PARTIAL]**.
- **Change:** Stop fusing identity and change. **Identity = a system-minted, time-invariant surrogate key** (key-change ⇒ different object) — *not* a "container." A *derived* object's identity is the **universal property of its defining diagram, not its build trace**. **Change = a directed path algebra** (composition + reflexivity, **no inverses**) — a free category, *not* the HoTT groupoid (univalence over-structures an append-only kernel).
- **Bound:** Handoff identity must be **allographic** (history-independent); the revision chain is *provenance* there, not identity.

### C2 — Log primary, state is projection → **FIX**
- **Home:** section/retraction recoverability ladder **[HOLDS — cleanest literal instance]** · CALM monotone? bit **[HOLDS]** · provenance semiring N[X] homomorphism **[PARTIAL]** · Kolmogorov non-invertibility floor **[HOLDS]**.
- **Change:** Keep log-primary. Give **every fold a contract**: (a) a **monotone? bit** (monotone ⇒ confluent/coordination-free; non-monotone ⇒ output isn't truth until *sealed*); (b) a **declared section `s` with `g∘s = id`** where recoverability is claimed, else mark the fold **lossy**; (c) for relational/difference-free folds, a provenance-semiring level (N[X] = lossless).
- **Bound — HARD RULE [Kolmogorov]:** re-projection is **recomputation from the retained primary log, never inversion of a baked artifact.** Forbid log-discarding pipelines. The semiring guarantee does **not** cover supersession / aggregation / last-write-wins — mark those folds **opaque**.

### C3 — Rama is ground truth / back-arrow → **FIX**
- **Home:** diplomatics reliability/authenticity decomposition **[HOLDS — strongest literal import]** · CALM coordination seal **[PARTIAL]**.
- **Change:** Restate precisely: **append-only custody guarantees AUTHENTICITY** (the un-tampered record of *what agent X claimed*), **never RELIABILITY** (whether it is true). "Ground truth" = *truth-about-custody*. The back-arrow's *operation* is **estimation + corroboration** on the monotone sub-language; every non-monotone op (redaction, deletion, aggregation-as-truth, arbitration, synthesis) requires a **coordination seal** before its output counts as truth.
- **Bound:** *Who* may seal is an **exogenous rule of recognition** — normative, social, un-versionable (Hart). The kernel cannot validate its own ground-truth criterion without regress.

### C4 — Declared > inferred → **FIX (keep the floor, overturn the binary-as-law)**
- **Home:** diplomatics reliability × authenticity × accuracy **[HOLDS]** · MDL two-part promotion gate **[PARTIAL]** · Shafer/trust-semiring propagation **[PARTIAL]**.
- **Change:** Keep the anti-hallucination floor; **replace the binary**. Three orthogonal fields: **reliability** (producer competence + creation controls, at ingest), **authenticity** (identity-at-mint + custody integrity, continuous), **accuracy** (content). "Inferred" = **low-reliability, ranked — never rejected**. Promotion inferred→declared = an **MDL gate** (promote iff it shortens total description length).
- **Bound / open:** confidence-**boosting** corroboration from independent sources is **provably non-semiring** (idempotent min/max can't boost) — needs DST or Bayesian pooling (see fork §5.5).

### C5 — Two times → **FIX (round-3 mislabel fully repaired)**
- **Home:** BCDM bitemporal element **[HOLDS]** · Danto narrative-sentence retro-description **[HOLDS]** · Lamport scalar-offset bound **[HOLDS]**.
- **Change:** Relabel canonically: **transaction time** = append-only log position (monotone, never rewritten, ≤ now); **valid time** = user-assertable truth interval (may be past or future). **Drop "committed/valid"** (it fused an act with an assertion). **Add a first-class narrative annotation:** a bitemporal *retro-description* whose valid-time references an earlier event and whose transaction-time is the later commit that makes it assertible — appended without mutating the earlier event.
- **Bound:** the description-set is **backward-incomplete** (grows monotonically; distinct from C2's forward-lossy). The commit offset is a **linearization tiebreak only** — never a causal signal. The C7 DAG is a *third* axis, not bitemporal.

### C6 — Groupings / configurations → **FIX**
- **Home:** Leinster colimit/pushout universal property **[HOLDS]** · BCDM coalescing normal form **[HOLDS]** · HoTT Structure Identity Principle (query-only) **[PARTIAL]**.
- **Change:** Keep (it's regularization-by-retention). Represent a grouping as a **stored diagram** (members + overlap morphisms); the merged view is the **colimit** (coproduct if disjoint = clean handoff, no coupling; pushout if glued), keyed by universal property so re-derivation yields *equal* identity. **Compaction = coalescing**: merge only value-equivalent *adjacent* revisions, never across a payload change — provably lossless.
- **Bound:** monotone membership-add is seal-free; capped / exclusive / delete-sensitive groupings route through the **seal**.

### C7 — Commit-grain minting; semantic time is a DAG → **FIX**
- **Home:** Lamport happened-before strict partial order + Fidge/Mattern vector clock **[HOLDS]** · scalar-clock converse-failure bound **[HOLDS]** · happened-before ≠ structural causal graph **[HOLDS]**.
- **Change:** Interpret the previously-uninterpreted DAG: **semantic time IS the happened-before strict partial order**; each revision carries a **vector clock** (own-increment + componentwise-max); `VC(a) < VC(b) ⟺ a→b`, incomparable ⟺ concurrent.
- **Bound:** **Forbid the scalar commit offset as the causal signal** (Lamport converse-failure — tiebreak only). The DAG is a happened-before DAG, **not a structural causal graph** — effect/intervention queries need a *separately declared* structural graph with latent-confounder edges (Pearl / Global Markov). The DAG is a distinct axis from the two linear bitemporal clocks.

### C8 — Preserved disagreement; late-bound synthesis → **FIX (telos-refutation converted to structure)**
- **Home:** Belnap–Dunn FDE {t,f,b,n} **[value space HOLDS]** · non-explosive consequence **[PARTIAL]** · sheaf Čech-H¹ obstruction **[PARTIAL]** · CRDT MV/OR-Set **[HOLDS]** · Leinster cocone-without-colimit **[PARTIAL]** · Dietrich–List escape menu **[PARTIAL]**.
- **Change:** (1) **Standing is a 4-valued type** ⟨undefeated-support?, undefeated-attack?⟩ = {true, false, **both/glut**, **neither/gap**}; glut ≠ gap is load-bearing. (2) **Split disagreement**: overlap-incompatible ("signalling") → *arbitrate before synthesis*; compatible-but-no-global-section → *preserve terminal*. (3) **Mechanism**: preserved disagreement is **monotone** (CRDT MV/OR-Set union never loses a concurrent claim); **synthesis is non-monotone** ⇒ "late-bound" = *sealed*, and any post-seal observation **re-opens** it. (4) Synthesis = a **late, dated, explicit operation over a retained cocone** — but a colimit *collapses*, so where synthesis must *hold both sides* it is **not** a quotient. (5) The store is **non-explosive**: a glut propagates only along typed defeat edges (no *ex falso*); disjunctive syllogism is forfeited in glut neighborhoods.
- **Bound:** the telos is genuinely refuted — **some disagreement is constitutively terminal**, not awaiting synthesis. A nonzero Čech-H¹ is a *sound one-sided* certificate of non-reconciliation (store it); **H¹ = 0 is inconclusive**, never "synthesis proven possible."

### FLAGSHIP — semantic zoom = lawful compression → **OVERTURN**
- **Home:** Leinster functor/faithfulness **[HOLDS]** · RG relevant-set/universality **[PARTIAL]** · rate-distortion frontier **[PARTIAL]** · Kolmogorov lossless-impossibility floor **[HOLDS]** · provenance Prop 5.12 (where-provenance non-derivable) **[HOLDS]**.
- **Change:** Overturn "all relations survive or it's fake." **Lawful zoom = a functor** (preserves identity + composition); **FAKE** = breaks composition (not a functor); **LOSSY** = a genuine-but-non-faithful functor — and a zoom-out functor *generically is not faithful*. The preserved invariant is the **relevant-variable set + symmetries**, not all relations (RG). Replace the axiom with a **declared per-axis distortion budget** (§4, Distortion budget layer).
- **Bound — HARD FLOOR [Kolmogorov]:** lossless zoom is impossible for generic content; re-project from retained fine state, never invert a baked artifact. **At least one axis (copy-location) is provably non-derivable from another** (Prop 5.12) ⇒ the budget **must be multi-entry**, never one global number.

### SOUL — a place inhabitable by another mind → **FIX (keep as best axiom; bound it; build the one instantiable felt primitive)**
- **Home:** Goodman autographic/allographic partition **[PARTIAL]** · Gibson affordance-as-relation **[HOLDS]** · Collins interactional/contributory bound **[PARTIAL]** · Lave & Wenger LPP **[PARTIAL]**.
- **Change:** Keep — it is Softland's **best-supported** axiom (songlines = a 10,000-year existence proof). **Bound it:** the "inhabitable without rebuilding context" guarantee holds at most over the **allographic sublayer** (declared records, C2-architecture, C6 snapshots); the autographic/felt residue is **constitutively non-copyable** — point at it, don't serialize it. A linguistic trail confers at most **interactional** (discussable) expertise, never **contributory** (performable) — so "minimize private reconstruction" is bounded to the *discourse*, never the *skill*; re-enaction is constitutive.
- **The one instantiable felt-adjacent primitive:** an **affordance/solicitation = an observer-relative 2-place relation** (local-world-feature × visitor-capability), evaluated as a **parametric query at inhabitation time — never a materialized field.**

---

## 3. Corrected universal criterion

**"Minimize private reconstruction"** is empirically **inverted** for causal/explanatory and embodied content (generation effect, desirable difficulties, illusion of explanatory depth) — and the inversion is *the user's own stated value* ("rediscovery over consumption / do the work, not know it"). The science backs the soul against the engineering slogan.

**Revised criterion:** *minimize only **wasteful** context re-derivation* — locating which structure is relevant (what the kernel was actually for) — **while preserving constitutive re-enaction** and **marking the irreducible tacit/metis residue** (mark where the map stops). Carry a **content-typed transmission class** + a **calibration-error variable** (felt-understanding − producible-understanding, which is *content-typed*: near-zero for facts/procedures, large for causal/explanatory).

---

## 4. New layers the rounds surfaced (all **ADD**)

1. **Estimation back-arrow** — partition every back-arrow fold into a *monotone semilattice core* (per-site idempotent observation folds are a CvRDT — no arbiter) and a *non-monotone tier* (deletes, caps, arbitration, override, synthesis) routed through the seal. An agent's **act = a typed `do`-event, never reconstructed by folding observations** (record the doing).
2. **Seal / close** — an explicit coordination point at every non-monotone boundary; a non-monotone fold's output is not truth until sealed. Identity minting is **provably non-I-confluent ⇒ single-key mint before append** (this *theorem-justifies* the existing Slice-A `run-id`-before-`foreign-append!` decision). *Who* may seal = exogenous, versioned, accountable rule of recognition.
3. **Governed forgetting** — forget by **folding, not severing**: coalesce value-equivalent revisions (lossless); downgrade a projection's provenance level along the homomorphism lattice and *publish the homomorphism* so the coarser view is honestly labeled lossy; drop residual bits up to a declared budget while retaining the relevant set. **Never discard the primary log.**
4. **Distortion budget** (the flagship repair, as a first-class object) — per-projection metadata carrying, **per axis**: `(distortion-measure, bound, measured-E[distortion])`; the record of which relations the projection collapsed (faithfulness failure); the relevant-set preserved. **≥ 2 independent entries mandatory** (derivation-structure *and* copy-location — Prop 5.12). The map cannot silently lie about its fidelity.
5. **Felt channel** — software/versioning is correct **only** for the inert allographic record. The felt/valence/solicitation residue gets: a **non-serializable marker** on the autographic remainder; the **affordance relation** (parametric, observer-relative, query-time) as the one instantiable object; a **scaffolded LPP traversal** for re-enaction. Do not claim to transmit it.
6. **Content-typed transmission** — type every handoff payload by what it can transmit: allographic/notatable content transmits as truth; everything else confers at most interactional standing and **must be re-enacted**. Keep notationality *off* the C4 truth axis (symbol-crispness ≠ credibility — that conflation is a verified category error).
7. **Non-explosive disagreement container** — an FDE glut is a first-class *terminal* standing, not an error; it propagates only along typed defeat edges (no *ex falso*) so one contradiction cannot trivialize the projection. Hold concurrent claims in a CRDT MV/OR-Set; where local worlds form a shared cover, compute the sheaf obstruction.

---

## 5. The six design forks (where two verified primitives prescribe incompatible structure)

These are **genuine decisions**, not errors — two disciplines each gave the kernel a literal structure for the same word, and they disagree. My recommended resolution is marked **→**; where the research settled it, it's cited.

1. **"Synthesis" — collapse vs hold-both.** Category theory says synthesis = a **colimit** (a forced quotient that *collapses* disagreeing elements). The soul + List–Pettit + CRDT-join want synthesis to **hold both sides without identifying them**. → *The colimit is the right operation for **preserving** disagreement (retain the diagram, don't take the colimit); it is the **wrong** operation for the positive act of synthesis.* Use two distinct operators; never let "synthesis" mean quotient by default.
2. **Merge — arbiter or not.** CRDT join says concurrent claims merge by least-upper-bound with **no coordination**. CALM / I-confluence say any **non-monotone** op (collapse-synthesis, arbitration, deletion) **provably requires a seal**. → *Resolvable by partition:* union-preserve = monotone/free; collapse-synthesis = non-monotone/sealed. **Do not let the CRDT "no arbiter needed" license leak onto the synthesis step**, where an arbiter is mandatory.
3. **C7 time — partial order vs linear.** Vector clocks insist concurrency = incomparability and must **not** be linearized (else false causality between parallel agents). BCDM requires a **linear** total order for snapshot/timeslice to be well-defined. → *Keep them as **separate axes**: the causal DAG (partial order, for honest concurrency) and the bitemporal clocks (linear, for snapshots). A revision lives in both; never collapse one into the other.* (Danto compounds this: a concurrent event can be a *later truth-maker*, so description-growth cannot be localized to the causal up-set.)
4. **C7 DAG — happened-before vs causal.** The vector-clock primitive names the DAG as causal *precedence*; Pearl proves happened-before is **not** structural causation. → *Type the DAG as temporal-causal-precedence **only**; forbid reading it as a structural causal model. Effect queries require a separately declared structural graph.*
5. **C4/C3 corroboration — idempotent vs boosting.** Provenance-semiring trust (min/max) **cannot** make two independent corroborating sources yield higher standing than either alone. Dempster–Shafer / Bayesian pooling **is** the confidence-boosting operation C3's "corroboration" needs — but it's independence-assuming and non-injective. → *Open (fork §6.3): pick DST conflict-mass or Bayesian pooling for the boosting step, and accept that **neither serves open-world, new-hypothesis disagreement** — the kernel's hardest case.*
6. **C4 — one credential or two orthogonal axes.** Diplomatics proves authenticity (un-tampered custody) ⊥ reliability (truth-capacity at creation), and custody vouches **only** for authenticity. Single-scalar credential primitives tempt collapsing them. → *Keep reliability and authenticity as **separate fields**; a credential may gate acceptance-weight, but the custody chain must **never** be read as evidence of reliability.*

---

## 6. The five encoding decisions (what blocks "theorems-bite")

The kernel has formal homes but is **"not yet an instance"** of them. These commitments make the theorems literally apply — they are the real remaining work:

1. **A declared convergent merge operator `⊔`** (commutative, associative, idempotent). This is the **precondition** under most distributed-systems theorems (CRDT/CALM/I-confluence). Without it, none of the merge results bite.
2. **A committed claim/disagreement encoding.** C8 has *five* competing terminal-disagreement formalizations (FDE, sheaf-H¹, CRDT, cocone, escape-menu), each gated on a different substrate; none is instantiated until the encoding is chosen.
3. **A C4 credential algebra + a home for corroboration-combination** (the provably non-semiring boosting step — fork §5.5).
4. **A treatment of the non-monotone / non-injective fold class.** The kernel's *actual* arbitration/redaction/last-write-wins folds sit **outside every clean theorem** — they need the seal + an explicit "opaque" mark.
5. **A source distribution for the distortion budget.** FLAGSHIP's quantitative per-axis budget doesn't compute without one (R(D) bit-floors aren't computable in general; the *measurable contract* is, the *information-theoretic minimum* isn't).

**Plus the provenance/section schema commitment** (open question): the semiring-recoverability and cohomology-certificate guarantees are literal *only if* KernelEvents carry `(variable-set, joint-assignment)` structure and folds are relationally expressible. Impose that schema (cost), or mark folds opaque and forfeit the guarantees.

---

## 7. The three declared bounds (constitutively beyond formal reach — fence, don't pursue)

A web-search method can locate the *edge* of the formalizable with precision; it cannot manufacture trans-boundary content. For these, "done" = **the bound is declared and the kernel stops promising to cross it** — not an open TODO:

1. **The felt / valence / solicitation channel.** Proven non-serializable (Goodman autographic residue; Collins contributory expertise). No "felt-transmission primitive" exists in propositional form. Build the affordance relation; mark the rest non-copyable; scaffold re-enaction.
2. **Arbitration authority (who may seal).** A normative rule-of-recognition / social membership fact, orthogonal to every formal source. Declare it; version it as policy; never derive it from the kernel's own machinery.
3. **The "semantic" surplus of C7.** Meaning beyond happened-before requires structural-causal content the formal DAG cannot carry. Bound C7 to temporal precedence; route causal claims to a declared structural graph.

---

## 8. What to build first (my sequencing — goes beyond the research)

The research stops at the spec skeleton; this section is my recommendation for turning it into code, anchored to the existing Slice-A dogfood-runtime work.

1. **The `⊔` + monotone? + seal triad** is the highest-leverage encoding (decision §6.1, §6.4) — it unblocks the most theorems and it *theorem-justifies the Slice-A spawn decision already made* (non-I-confluent mint ⇒ single-key-before-append). Start here; it's the smallest commitment with the widest downstream effect.
2. **Relabel C5 now** (transaction vs valid time; drop "committed/valid") — it's `[HOLDS]`, essentially done, and it's a pure naming fix that prevents the mislabel from propagating into every new PState. Cheap, high-clarity.
3. **The estimation back-arrow partition** (monotone observation core vs sealed non-monotone tier) maps directly onto the existing `*compute-obs-depot` / per-site idempotent observation folds — the back-arrow rule the dogfood-runtime already commits to. This is where the research and the live code meet.
4. **Defer** the C8 disagreement encoding (§6.2) and the distortion budget (§6.5) — they need a chosen claim representation and a source distribution respectively, and neither blocks the compute track. They belong with the *epistemic* (LLMDepot) track, not Slice A.

---

## Appendix — what is load-bearing vs aspirational

**[HOLDS] (literal, build on these):** BCDM bitemporal & surrogate key (C1, C5); section/retraction recoverability + Kolmogorov no-inversion (C2); diplomatics authenticity⊥reliability (C3, C4); CALM monotone bit (C2, C3, back-arrow); vector-clock partial order + scalar-converse-failure (C7); CRDT MV/OR-Set + FDE value space (C8); functor/faithfulness + Kolmogorov floor (FLAGSHIP); Gibson affordance-as-relation (SOUL); non-I-confluent mint (seal).

**[PARTIAL] (real, but gated on an encoding):** provenance semirings (need the section schema); sheaf-H¹ (needs the distribution/cover encoding); MDL promotion + DST/Bayesian corroboration (need the credential algebra); rate-distortion budget (needs a source distribution); colimit-synthesis, escape-menu, LPP scaffolding (need the claim encoding).

**Discounted as loose-metaphor (do not build on):** HoTT univalence/groupoid machinery (the kernel is a free category, directed-only); quantum no-go theorems on C3; control-theoretic observability; Landauer thermodynamics; Tarski/Gödel self-reference-as-contradiction. These *rhyme* with the kernel but the kernel is the wrong type of object for them to grip — which is itself the round-4 stability signal.
