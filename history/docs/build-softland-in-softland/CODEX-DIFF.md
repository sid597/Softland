# Diff: MODEL.md and the Codex proposal

2026-09-11. Historical comparison, followed by the discussion recorded below.

**Current status after discussion.** Sid requested the document updates. The
revised [MODEL.md](MODEL.md) and the concrete [BUILD.md](BUILD.md) now carry the
next experiment; implementation awaits his go-ahead. The comparison below is
preserved as a record of the original versions, not a list of current defects in
the revised model or a set of decisions already made for Sid.

- Definitions as material, match for applicability and names for composition are
  converged. Placement follows authority, dependencies and lifetime; the old
  rule/server and view/client table was a default. Agent admission is an editable
  starting policy: ordinary content with a visible origin, shared behavior through
  candidates. Callable values, cardinality transitions, explicit conflicts,
  selective unanswered-event retention and positive recursion with stratified
  negation are now explicit. My history wording is corrected: a maintained
  reinterpretation need not be an activity that reissues commands.
- Actual Electric is the prepared host for one isolated build. Its absence from
  the earlier model was a host disagreement, not a missing universal requirement.
  Sid authorized Electric in the original Smalltalk worktree; the next brief
  proposes another scoped use. Neither experiment settles main's permanent host.
- The earlier recommendation for general recursion inside bodies is deferred.
  Build an authored algorithm under the September 6 restriction first: total
  steps with explicit state, repeated by an owner with a budget and cancellation.
  Executor continuations are a starting mechanism, not proof of that construction.
  A language extension still requires evidence and Sid's decision.
- Closure remains a hypothesis. Log native additions as optimization, required
  primitive or convenience, with the corresponding authored expression,
  limitation or admission. A common name is not equivalence; a failed attempt
  alone is not an impossibility proof. Read that log beside the product, durable
  acceptance/recovery, narrow propagation and disposal evidence.
- The brief includes a small real `ask` and its minimum external-activity
  lifecycle. References, admission and read outcomes are builder decisions now.
  General geometry correspondence, broad runner optimization and the full
  in-world implementation rebuild/recovery loop remain later tests. The gaps
  below do not form a prerequisite checklist for all of those later facilities.

**Original comparison, before these revisions.** Section references below refer
to those versions; the linked documents now include the discussion's corrections.

Compared: [MODEL.md](MODEL.md), sections 1–13, and [my full proposal](/mnt/data/projects/codex-smalltalk-electric/docs/smalltalk-electric/SOFTLAND-PROPOSAL.md), sections 1–18. “Mine” below means that written proposal, not an earlier chat sketch. Supporting ground is the September 6 [“Tools are records over a vocabulary” decision](../../../docs/decisions.md#tools-are-records-over-a-vocabulary-settled-2026-09-06) and the relevant passages of [vision/LOG.md](../../../vision/LOG.md). This diff records positions; the implementation and lived-evidence boundaries appear at the end.

**Agreement.**

| Subject | MODEL | Mine | Diff |
|---|---|---|---|
| Top noun | §§3.2, 4 | §§6, 12, 15 | Agree: definitions are material; a tool is an authored bundle, with no built-in tool category required in the execution floor. |
| Match versus call | §§3.2, 4 | §§5–6 | Agree: match decides applicability, named composition obtains another definition's result, and resolution takes a context. |
| Three outcomes | §3.5 | §7 | Agree: maintained results withdraw with support, accepted edits persist independently, and activities have owners and lifetimes. |
| Material and meaning | §§3.1, 5, 7–8 | §§2, 4, 12, 15 | Agree on stable addressing, plural authored classifications, selective durability, deterministic navigation, and human/agent work supplying meaning. |
| Representation and rebuilding | §§2, 3.2, 8, 11 | §§5, 11, 16 | Agree: records can have several editing views and compiled runners; semantic diffs need correspondence; the definition format can change through a controlled rebuild and migration. |
| What remains unproved | §§4, 11, 13 | §§6, 17–18 | Agree: naming Rama, a reactive runtime and the renderers does not establish the missing matcher, performance, or closure. |

**Disagreements.**

1. **What an author may express in a body.** MODEL **§3.2 and §12** preserve total recipes under the September 6 restriction and explicitly rule out a general program language in bodies. Mine **§5**, exercised conceptually in **§14**, proposes parameters, conditionals, named invocation and repetition/recursion, and explicitly asks to change that restriction. I retain that proposal: authors should be able to express an algorithm in a reusable structured definition, with execution bounded and long work owned. MODEL puts a boundary around bodies and sends work beyond that boundary into activities; it has not specified the authored control language of those activities. This is a real difference in the proposed authoring contract. It is **not proof that MODEL's eventual combination of recipes and activities would be computationally weaker**, nor a difference between text and visual editing. The current law remains the law until Sid rules otherwise.

2. **What counts as breaking closure.** MODEL **§2** includes body-vocabulary capabilities in the Softland code stratum with the floor; **§§11–12** make new floor code for a new tool, effect or view a falsifier. Mine **§§15–16** separates a definition's meaning from its interpreted or compiled implementation and explicitly permits adding an external-target adapter through the in-world build path. I retain the distinction: a new application-specific dispatch branch is a failure of composition; a verified compiled implementation of an existing definition need not be. MODEL already permits compiled runners, so compilation itself is not our disagreement. Its closure claim needs to distinguish extending the floor's semantics from extending an implementation or target. **Neither document proves a closed floor; we differ on what that hypothesis should exclude.**

3. **Where execution belongs.** MODEL's **§4** table assigns rules to Rama and views to the client. Mine **§§4, 7, 9–10** assigns work by its dependencies, authority and lifetime, with Electric carrying the server/client relationship. A session-selection rule can run under a client session owner; a view can depend on server computation. I would not make view-versus-rule determine placement. Durable admission still belongs to Rama in both proposals. MODEL's omission of actual Electric is a separate gap below, not evidence that it deliberately rejects Electric.

4. **The remaining condition on the fixpoint guarantee.** MODEL **§3.5** says finite facts without newly manufactured values converge. Mine **§6** additionally restricts recursive maintained evaluation to positive rules, with negation over completed lower dependencies. I retain that restriction. Finite possible facts alone do not prevent oscillation if rules can test absence and withdraw support: “maintain A while B is absent” and “maintain B while A is present” have no stable result despite introducing only A and B. If MODEL intends positive recursion, this is an omitted condition rather than another mechanism. Its repaired distinction between finite closure and unbounded activity otherwise stands.

5. **Reinterpreting old events.** MODEL **§3.5** permits a maintained definition over the event table to show what an old event would mean now, without asserting its effects again. Mine **§7** puts intentional “replay or reinterpretation” into a separate activity. **I take MODEL's distinction here.** My wording is too broad: a changing explanation can be a maintained query. Reissuing commands with lasting effects needs explicit admission and execution ownership. This correction does not imply that either proposed history interface has been built.

**Additional choices in MODEL: whether I take them.**

| MODEL choice | MODEL / nearest place in mine | My position and why |
|---|---|---|
| Attribute cardinality is itself material, with one value by default; a thing's own value shadows a definition default. | §§3.1, 3.4 / mine §§2–3, 7 | **Take these defaults.** They make ordinary fields and local variation straightforward while leaving collections explicit. Changing cardinality for existing material still needs an admitted transition; the metadata alone does not resolve existing values. |
| Layer order arbitrates two asserted changes from one event; a tie becomes a conflict event. | §3.5 / mine §§3, 7–8 | **Do not take layer order as the universal write policy.** I take the explicit conflict. Overlay priority already answers which value a context sees; competing mutations of one target should follow that target's declared admission/choice policy. Layer priority can be one such declared policy. |
| An unmatched event remains visible as unanswered work; a question is a pattern without a body. | §§3.2–3.3, 9 / mine §§6, 12, 14 | **Take both as authored facilities.** Unanswered meaningful requests should be addressable; a saved pattern can expose its matches. For retention, §5's deliberate durability rule must govern: this should not silently retain every unmatched raw input. “Announce” can be a view or rule over the matches. |
| Agent output never directly changes the base. | §7 / mine §§8, 12 | **Take candidate-first as a seed default for changes to shared behavior, not a universal restriction on agents.** An agent authorized to record an observation or make a requested edit should use the same admission policy as another authorized participant. Origin should not replace authority. |
| The concrete genesis interaction is blank-space writing, an `ask` shortcut, semantic zoom, halo, definition view and unanswered view. | §9 / mine §12 | **Take it as a candidate initial design.** Mine specifies the experience but leaves these particular defaults open. This is a product choice to live, not an experience established by seven schematic definitions. |

**Gaps in MODEL: positions mine states that MODEL does not yet specify.**

| Gap | MODEL location / mine location | What is missing |
|---|---|---|
| Ordinary callable computations that yield neither paint nor a change | §3.2's exhaustive view/rule table, alongside §3.5's derived count / mine §§5–7 | A precise place for `parent(subject)` or a geometry calculation: parameters, returned values, and invocation without requiring a user event or a visible thing. Named composition and the derived-count example suggest this is intended. “No third kind” does not yet explain it. This need not introduce a new built-in kind. |
| Actual Electric and its ownership | §§2, 4 / mine §9 | An explicit commitment to the Electric compiler/runtime, its server/client dependency and lifetime role, and how runtime-authored plans create tracked branches. MODEL names Rama and Missionary; those names do not answer this requirement. |
| Resolution over time and coherent publication | §§3.2, 3.4, 6 / mine §§3, 7, 9 | Stable references distinct from display names; pinned revisions versus live bindings; a resolution basis retained for an invocation; coordinated publication through a revision manifest; explicit removal versus deleting an override. Candidate layers are present. These temporal and removal semantics are not. “Everyone follows” after promotion must mean contexts following that base, not pinned or independently overridden contexts. |
| The acceptance protocol and usable history | §§3.5, 8 / mine §§4, 8 | Authenticated authority distinct from the actor named in context; request identity, preconditions, rejection/unconfirmed decisions, deduplication, ownership/locality and cross-owner publication boundaries. Retaining depot events is covered; a queryable revision/decision history is not specified. These determine what “accepted” and “recoverable” mean operationally. |
| Demand, query completeness and terminal read outcomes | §§3.5, 4 / mine §§6, 9, 15 | Explicit demand roots and query scope; when absence is known rather than merely unobserved; failed/forbidden outcomes distinct from absent/pending; actual read ownership through changing calls and branches. MODEL already requires declared reads, support withdrawal and indexed matching. The gap is the narrower execution contract, not those principles. |
| Equivalence across runners | §§2–4 / mine §§9, 16 | The same declared inputs, results, dependencies, effects, errors and relevant numerical behavior across interpreted and compiled implementations. A shared name or output slot alone does not establish substitutability. |
| Activities that cross a process boundary | §§3.5, 4, 7 / mine §§8, 10 | Accepted intent, executor claims, worker observations, retry/idempotency or reconciliation, and unknown outcomes. MODEL specifies ownership, cancellation and budgets; mine also requires isolation or yielding for opaque work and accounting across child activities. These make its responsiveness promise implementable. |
| Material parts, occurrences and edit mappings | §§3.1, 4, 6, 8, 10 / mine §§2, 7, 11, 14 | Stable part and occurrence identities, suitable text/geometry chunking, and declared mappings from manipulation back to material operations. MODEL has hit provenance and recognizes semantic correspondence; it does not specify how a handle changes its source or how an annotation survives a changed mesh. |
| Reproducible work and a recoverable implementation loop | §§2, 7–8, 10 / mine §§2–3, 10, 14, 16 | Preserving source material beside interpretations; retaining exact experiment inputs; source/toolchain/build/artifact identity and retention; activation while keeping a usable runtime; recovery to a last usable environment. Addressable source plus “rebuild” names the route, but does not yet define these guarantees. |
| Demonstrations beyond live promotion | §6 and §§11–13 / mine §§13, 17–18 | The explicit parent fallback that keeps the tool addressable; pending/rejected continuity; a second independently usable saved tool; fresh-session recovery; two-view disposal; an authored nontrivial algorithm; and changing/rebuilding the implementation from inside. MODEL's traced scene does not include these obligations. Naming them in mine is not claiming they have all passed. |

**Implementation, proposed position, lived evidence.**

- **Implementation described by the documents:** mine **§17** identifies the pointing build's three-instruction definition language, compiled selection/inspector handling, and Electric ownership around Softland rendering. MODEL **§§3.5–4** identifies the record executor and its read barrier as existing pieces. These are two mechanisms with narrower scope than either proposed execution model. Neither citation establishes the general matching/composition system above them. This diff does not add a fresh runtime receipt to those accounts.
- **Positions:** all disagreements, adoptions and gaps above concern what the proposals would require. MODEL's **§§1 and 13** classify more as derived from the test; mine **§§17–18** keeps the broader claims as further demonstrations. I support the whole-environment aim. A successful pointer interaction alone cannot establish that every editor is authored material, that general composition works, or that the floor is closed.
- **Lived evidence:** mine **§17** cites recorded test/browser checks, not Sid's acceptance of the experience. Sid's report in this conversation that he was at a loss about what he was looking at and what to do remains distinct evidence about the interface. MODEL's **§6** is a proposed walkthrough. Neither that walkthrough nor the existing read barrier establishes a lived experience of either full model.
