# GROUNDS — how the block grammar is being derived (spec-room record)

2026-07-08 · spec room (Sid + Fable, live session) · **status: LIVING** — grows with each iteration; captured on Sid's word ("we should capture this"). This is the **warrant layer** of the spec work: how we thought, the principles, the receipts, the breadth the spec must answer. It is NOT the spec.

## 0 · The artifact map (what gets captured where)

| artifact | role | state |
|---|---|---|
| `GROUNDS.md` (this) | the **warrant** — derivation, principles, receipts, open forks, ledgers | LIVING, grows per iteration |
| `SPEC.md` | the **yield** — the normative artifact other rooms consume (block grammar · kinds · relations) | NOT YET — freezes to v0 only after ≥1 manual specimen pass |
| specimen passes | the **evidence** — hand-marked stretches, checked against independent description | next iteration |
| rama-room CONTRACT | "make it real in our system" — downstream, gated by SPEC v0, never folded into it | other room |

Self-reference, deliberate: this structure is the model's own episode grammar applied to the spec work — deliberation→warrant (grounds), artifact→yield (spec), grounding→evidence (specimens). A hand-made stand-in for what the sense-line will one day capture automatically.

## 1 · The why (Sid, this session — the function the grammar serves)

Wall-of-text fails not because it is long but because **conversation-with-a-goal is stance-work**: two minds, aligned on a sediment layer, building on top of it — and the reader's operations are *agree / disagree / pull-this-thread / defer*, possibly different per span. **Blocks exist to receive stances.** Two boundary notes, both Sid's:

- **Immersion is preserved by construction** (harvest-sharpened): blocks are addresses over an untouched wall — the wall stays whole and readable (immersive mode) while blocks are a queryable overlay (operational mode). Structuring is non-destructive; there is no choice between sitting-with and operating-on.
- **Blocks serve the local mind:** even spans nobody currently engages deserve blocks — for the mind (human or agent) actually working at that altitude. Blocks are *places, not things*; minting one costs a pointer, so total coverage is affordable.

## 2 · The four laws (principles with arguments; normative phrasing belongs to SPEC.md)

**Material.** A block is `(stream, span)` into an immutable raw stream — an address, never a copy. Blocks never change; "editing" mints a successor with a supersedes edge. (The world-line law recurring at micro-grain.)

**Boundary.** Segmentations are plural, provenance-carrying, demand-refined:
- *First cut free* — accept producer structure: the API's typed content-blocks (thinking / text / tool_use); markdown paragraphs, list items, fences, headers. Fences, tables, quotes are never split — there, surface structure IS semantic.
- *Every segmentation declares its rule* — (rule-id, run-id, producer). An undeclared chunk is torn paper (Sid: "without the specific spec it just makes it a chunk which will not be useful").
- *Refinement is demand-driven* — the engagement that needs a finer target mints the sub-block, and is recorded as that refinement's provenance. Desire-path chunking: grain deepens exactly where feet land.
- *Err coarse* — error costs are asymmetric: under-chunking heals by refinement (a mint); over-chunking needs identity surgery after marks have accrued to both halves.

**Identity.** Identity = (stream, span); minted on first engagement or storage; any later run producing the same span RESOLVES to the existing block, never duplicates it. Overlap and nesting are allowed; **containment is computed from offsets, never stored** — every tree is a query result. Identity is never path-dependent. Corollary: re-chunking can never orphan a mark.

**Composition.** Every production event — human message, agent reply, assembly — records **context-parents**: the blocks in view / replied-to. Any assembly that gets used becomes new material with `assembled-from` edges, re-entering at Material. Statement+parents at every mint — the commit gesture at thought grain.
- **Buildup is not breakdown⁻¹** (harvested): breakdown = lossless projection; buildup = generative synthesis that *cites*. A spiral, not an elevator — each buildup lays new sense on top with a drillable line to source.
- **Transclude by default** (lean; harvested, and independently implied by the Material law): lifting a block into new material is re-addressing — same identity. Mint a NEW block only when new words are written; it then `based-on` its sources.
- **The return path is buildup with a register parameter** (harvested): consolidator, morning answer, weekly sense, briefing — one operation at different tempos and target minds, not separate machinery.
- The severing gesture is copy-paste → hard DESIGN-ROOM requirement: assembly-with-parents must be *cheaper* than copying, or the DAG tears exactly where reuse is highest.

## 3 · What a GOOD break is (testable, per consumer)

- **Stance-flip seam** (generative criterion; harvested — grounded in Sid's own why): break exactly where a reasonable reader's stance could flip. Agree-with-all-or-none → one block; buy sentence 1, reject sentence 2 → that seam is a boundary.
- **Reaction test** (repair-side): no engagement should ever need "only the first half of that block" — refinement events are the measurable trace of misses.
- **Mark test:** marks land coterminous with existing blocks at high rate.
- **Travel test:** a transcluded block reads alone; where anaphora breaks it, surrounding context unfolds on demand (fold, don't sever).

## 4 · Material facts (grounded in the actual harness jsonl, 2026-07-08 peek)

- Assistant events carry `message.content[]` as TYPED blocks (thinking / text / tool_use) — the provider hands the structural cut for free. Full API metadata (model id, stop_reason, usage) rides along.
- `parentUuid` threads every event (linear — positional, not attentional); `isSidechain` flags sub-agent forks; `promptId` groups a turn. The delegation chain is half-present in the raw already.
- The stream is NOISY: mode / permission / file-history-snapshot / meta-caveat events interleave with conversation ⇒ **step zero of the grammar is a classification pass: river vs debris** (debris = world-line telemetry, not markable conversation — kept, not garbage).
- **Tool calls are the world-line seam, pre-typed** (harvested): write-tools (Edit/Write) → artifact-delta → `produced` (process family); read-tools (Read/Grep) → observation → `grounds` (epistemic family). The cheapest, most certain edges a marker will ever mint are tool-derived.
- tool_use = natively a block; tool_result = MATERIAL (a stream), chunked lazily on engagement — 2,000-line dumps are never pre-chunked. [The parallel reply treats tool_result as a block; our record keeps the stream treatment.]

## 5 · Symmetry, with exactly three asymmetries

One operation everywhere: **assembled context-blocks + actor → new blocks (with parents)**. The actor (human / model / automation) is metadata, not a different shape — multiplayer is substrate, not a feature. Chat is the degenerate case: context = the whole prefix, so parents are technically true and informationally nothing; block-native context-assembly makes parents real, sparse, meaningful. *The medium knows what a mind was thinking-with, because the medium assembled it.*

The asymmetries that remain: **ceremony** (no-ritual protects the human only — law phrasing, harvested: "humans are never forced to pre-chunk"; the chat box *forces* walls, Softland *affords* blocks — affordance may make humans author-mint naturally, but never ceremony), **tier** (gold/silver), **tempo**.

Automation closure (harvested): an automation-spec is itself blocks; an automation is an action whose authorship grounds out in blocks — author chains never leave the medium.

## 6 · Harvest ledger (parallel reply, 2026-07-08 — Sid-supplied; extraction, not critique)

Adopted: stance-flip seam (§3) · immersion-by-construction (§1) · transclude-by-default + new-words⇒new-block (§2) · buildup≠inverse / spiral (§2) · return-path-as-buildup(register) (§2) · tool-family seam (§4) · "never forced to pre-chunk" + affordance-not-ceremony (§5) · automation-spec-is-blocks (§5) · occurrence-vs-type mark fork (§8) · derived-status principle (§8) · "sidetrackkkk" as a hand-minted branch receipt → kinds-round evidence pile · observation that the author-mint/reader-mint fork recurs at every layer (blocks and marks).

Stand-asides (recorded, not argued — per Sid: harvest, don't correct): tool_result-as-block (our record: stream); its characterization of amendment A1 (unverified here; the substance — containers demote to one evidence-lens — is already ours via computed containment).

## 7 · Prior-art lens inventory (input vocabulary, never frames — DG's status, generalized)

- **Standoff annotation (LAF/GrAF, corpus linguistics):** immutable base text; plural annotation layers — *including competing tokenizations* — live separately as spans with declared schemes. Plurality of segmentation is solved practice, not exotica.
- **W3C PROV:** Entity / Activity / Agent; `actedOnBehalfOf` = the human→agent→sub-agent delegation chain; `Plan` = the automation-spec.
- **panproto** (Sid-supplied; fetched 2026-07-08): schema version-control — schemas as labeled directed graphs; git-style commit/branch/merge OF SCHEMAS; **lenses** = bidirectional converters migrating data across schema versions; GATs/colimits underneath. Relevance: the KINDS round — names finalize by recurrence *without orphaning marks made under old names* (rename ⇒ lens, not dangle); also cross-scheme consumption (§8.3).
- **Roam:** block identity ≠ location; block-refs. **git:** forced statement+parents; dumb hunks under semantic messages. **Discourse graph (Q/C/E/Source):** epistemic-family vocabulary (placed 2026-07-07).

## 8 · Open forks (leans recorded; rulings are Sid's)

1. **Marks on block vs block-occurrence** (type vs token; opened by harvest): a transcluded block appears in two walls — does a mark attach globally or per-appearance? Working split: *stance-marks default to OCCURRENCE* (agreement is contextual); *content-marks default to TYPE* (a factual error is wrong everywhere). Both must be expressible.
2. **Derived-status principle:** sediment/frontier (and kin: settled/live/dead) are PROJECTIONS computed from engagement topology — never stored fields. Status-rot prevention; the pace-layer law at block grain. Candidate spec principle.
3. **Cross-scheme consumption** (the named "something we haven't thought through" behind scaffold-plurality): plural kind-vocabularies coexist as strata *until* edges/episodes/views must READ marks — then bridges (lenses/functors) stop being optional. → kinds round; panproto/ologs as lenses there.
4. tool_result refinement grain (lazy-chunk protocol details).
5. All names are scaffolding pending recurrence + Sid's naming. **Names-pending ledger:** block · surface · form · free cut · stance-flip seam · river/debris · context-parents · occurrence · sediment/frontier · spiral · hole.

## 9 · Ratified in-flow this session (Sid's gestures; informal gold record)

- **Holes as first-class** — dangling edges / unbacked links must be storable, queryable, renderable ("making the unsaid said … #wins") ⇒ standing spec requirement.
- **The four laws + their Part-2 readings** — "YEEESSSS I AGREE … each and every chunk."
- **Plural breakings, gated by each declaring its spec** — his formulation; converged independently with the Boundary law.
- Register instruction (meta, now in memory): surface the full thinking; don't compress the reasoning out of replies.
- **Occurrence-vs-type mark split** (2026-07-09): stance-marks → occurrence, content-marks → type — "Agree with your stance."
- **SPEC v0 + block-kernel CONTRACT countersigned** — "all agree on the specs" (Sid, 2026-07-09). OPEN feel-items (human-side silver subs ON; header-own-block) ride at defaults to first render.
- **Placement challenge (Sid, 2026-07-09, post-countersign): "a block IS the atomic container of text" — UPHELD against source.** The container kernel already runs the spec's pattern for markdown (`markdown-block-v0` distiller; span anchors; unit-kinds; containment edges; deterministic `du:` ids; foreign-side distillation) — an independent convergence receipt for the SPEC's Material/Boundary/Identity laws, AND a live catch of Fable anchoring on a prior contract's characterization instead of reading the module (process miss, owned; contract ON HOLD pending Sid's placement ruling — options in its banner).
- **Sequencing ruling** (2026-07-09, Sid): SPEC.md + the Rama implementation CONTRACT are authored in THIS Fable-max session (context loaded, thoroughness where it pays); implementation, example-chat run, and benchmark run in fresh sessions by worker models guided by fable-high. Dual-benchmark design is Sid's: he and Fable mark the implementation's output INDEPENDENTLY, then merge and reconcile; then a dogfood UI renders that reconciliation in Softland; then the marks side of the grammar opens.

## 10 · Breadth — what SPEC.md must answer (checklist, grows)

Classification (river/debris) · the free cut per source-type · refinement protocol (who may mint finer grain; refinement provenance) · identity + resolution rule · overlap/nesting semantics · context-parents on every production event · assembly/transclusion rule + new-words⇒new-block · **what can be pointed at** (blocks, spans, marks, episodes, world-artifacts, HOLES) · occurrence-vs-type mark targets · tool-call typing across both edge families · the two reading modes (immersive/operational) · derived-status projections · the human-side law (never forced to pre-chunk) · [kinds & relations — next rounds].

## 11 · The agreed path (Sid's sequence, 2026-07-09 — supersedes the earlier manual-pass-first ordering)

1. **SPEC.md** — written this session (v0 draft, awaiting Sid's redline; carries a real fixture from the `7c80ce2a` head so no clause is vapor).
2. **Rama implementation CONTRACT** — this session (Fable, rama + work-package skills loaded, pitfalls falsification applied). The contract-writing is deliberately a forcing-function on the spec's thoroughness.
3. **Implement** — worker models, fresh session(s), guided by fable-high.
4. **Run one example chat through it** — chunks land in Rama, readable back out.
5. **Dual benchmark** — Sid and Fable mark the output INDEPENDENTLY; merge; reconcile. (This is the spec's falsifier — v0 stays LIVING until it survives this step.)
6. **Dogfood UI** — render the independent-marking + reconciliation in Softland itself.
7. **If it works** — open the other side of the grammar (the marks/kinds side) and the other side of the chat.
8. Decide more after.

The hand-application discipline didn't vanish — it moved into the spec (the §15 fixture) and into step 5, where it becomes two-headed and adversarial.
