# SPEC — Sense-Line Block Grammar · v0

2026-07-09 · spec room (Sid + Fable) · **status: v0 — COUNTERSIGNED by Sid 2026-07-09 ("all agree on the specs"); BINDING** — normative for the **block layer**; marks/edges specified as interfaces; episode sketched only. OPEN items 2–3 stand at their defaults; their feel-check stays live at first render. Warrant: `GROUNDS.md` (same folder — read it for the *why* of every law here). All names are working scaffolding (ledger: GROUNDS §8.5); renaming is Sid's and never breaks the grammar.

**SPEC ≠ BENCHMARK** (standing correction, DIRECTION §0): this document defines the grammar. Whether an implementation *breaks well* is judged later by the dual-benchmark (Sid ∥ Fable, independent then merged). Conformance (§16) tests machinery, never break quality.

**Reading rules:** MUST = checkable obligation; SHOULD = default with named escape; MAY = permitted; OPEN = Sid's pending ruling. Two meta-laws govern everything:

- **Layering law.** block → marks → edges → episode. Layers combine by layering, never merging.
- **Separability law.** Chunking and labeling are separate stages; re-running either MUST NOT break the other.

---

## 1 · Nouns

| noun | definition |
|---|---|
| **surface** | An immutable text container: one addressable stretch of stored raw material. For chat: `(event-id, part-path)` — e.g. one text part of one assistant event. For world artifacts: `(artifact, version)`. Assemblies mint new surfaces. |
| **production event** | The act that brought surfaces into being: a human message, one agent API call, an assembly. Carries actor, delegation chain, context-parents, time. |
| **block** | `(surface-id, span)` + a **form**. The unit of pointing, reacting, marking. An address, never a copy. |
| **span** | `[start, end)` offsets within a surface's exact stored text. |
| **form** | The block's *material* type, given by the producer's structure (thinking, paragraph, list-item, code-fence, tool-use, human-message, …). Form is fact. |
| **kind** | A mark's *interpretive* type (claim, question, verdict, …). Kind is proposal. **Form is material; kind is interpretation** — this distinction is the Separability law made concrete. |
| **segmentation** | One run of one chunking rule over a surface, producing blocks. Plural segmentations over the same surface coexist. |
| **refinement** | Demand-driven minting of a finer block inside an existing one. |
| **occurrence** | One appearance of a block inside an assembly: `(block-id, assembly-surface, position)`. |
| **assembly** | A production event whose inputs are blocks and whose output is a new surface. |
| **hole** | A referenced-but-unminted endpoint. First-class: storable, queryable, renderable. |
| **river / debris** | River = markable conversation material. Debris = harness/operational events (retained, unmarkable by default). |
| **actor** | Who actually produced an event — resolved, never assumed from message role (§3.4). |

## 2 · Material model

2.1 **Surfaces are immutable.** The stored text of a surface is the **canonical text**: the producer's exact output after ONE declared, versioned redaction pass at ingest (secrets never enter storage — hard rule; redactions are recorded with rule-id@version and MUST render as visible redaction marks, never silent gaps — the map must not lie). After mint, the canonical text never changes (MUST). Each surface stores a content-hash for drift detection. (Contract ruling R1.)

2.2 **A block is an address.** `(surface-id, span)`, span in offsets over the surface's canonical text. Offset unit: **UTF-16 code units** — native to both JVM and JS string indexing; MUST never split a surrogate pair; codepoint conversion stays derivable. (Contract ruling R2.)

2.3 **Reconstruction guarantee (immersion law).** Rendering a surface's text from storage MUST reproduce the canonical text exactly; blocks are an overlay. The wall is never shredded — immersive reading and block operations are two projections of one untouched material.

2.4 **Parsed blocks are immutable forever.** Born-native blocks (a human composing in blocks, later) version by `supersedes` — a new block, never an edit.

## 3 · Classification — river vs debris (step zero)

3.1 Every ingested event MUST receive a class from a versioned classifier; the classifier's rule-id rides on the result.

3.2 **Debris classes** (grounded in the actual harness jsonl, 2026-07-08 peek): harness ops (`mode`, `permission-mode`, `file-history-snapshot`), meta wrappers (`isMeta` caveats), local-command events (command-name / command-stdout), telemetry. Debris is retained — it is world-line-ish evidence — but is unmarkable by default and never enters the river's block stream.

3.3 **River classes:** human messages; assistant content parts; tool activity (tool_use as agent act; tool_result as material, §4.3).

3.4 **Actor-resolution law: role ≠ actor** (MUST). In the API format, tool results arrive as *user-role* events; harness injections are user-role; only some user-role events are the human. Resolution: user-role + tool_result content → actor `tool`; `isMeta`/command wrappers → actor `harness`; remaining user-role events → the human; assistant events → the model id, on-behalf-of the invoking chain. An implementation that classes tool_results as human speech has failed conformance (§16.1).

## 4 · The free cut (structural segmentation)

4.1 Runs at ingest over every river surface. Cost target: mechanical — no model calls (MUST for the free cut; model-driven segmentations are additional strata, §14).

4.2 **Assistant events:** the provider's typed content parts are blocks as given — `thinking`, `text`, `tool_use`. Within a `text` part, markdown structural units become blocks: paragraph; list item (one block per item); header (its own block — a statement of scope; OPEN); code fence (atomic — MUST never split); blockquote (atomic per quote); table (atomic).

4.3 **tool_use** → one block, form `tool-use` (name + args are its content). **tool_result** → a surface only, NO pre-chunking: 2,000-line dumps are never eagerly blocked; spans mint lazily on engagement (§5). The `(tool_use, tool_result)` pair is bound by a `produced`/`grounds` edge per §11.3, not by containment.

4.4 **Human messages** → one whole-message block always (form `human-message`) — *humans are never forced to pre-chunk* (law). Where the message carries markdown structure, the free cut SHOULD additionally mint silver structural sub-blocks (default ON; OPEN — Sid may turn this off). Both coexist under the overlap rule (§6.2).

4.5 **Every segmentation declares itself:** `(segmentation-id, rule-id@version, producer, run provenance)` (MUST). An undeclared chunk is torn paper — not a block.

4.6 **Form vocabulary v0** (closed list; grows only by spec amendment): `thinking · prose-para · list-item · header · code-fence · blockquote · table · tool-use · tool-result-span · human-message · human-sub` — plus `assembled` for §8 surfaces.

## 5 · Refinement — the demand law

5.1 Any engagement — a human gesture, a marker needing a finer target, a transclusion pulling a sub-span — MAY mint a finer block inside an existing one. The engagement is recorded as the refinement's provenance (MUST): grain deepens exactly where feet land, and the trace of *where refinement was needed* is itself evaluation data for the free cut.

5.2 Refinement adds; it never invalidates. The coarser block persists with its marks (MUST).

5.3 Err coarse (default): under-chunking heals by refinement (a mint); over-chunking needs identity surgery after marks accrue to both halves.

5.4 Semantic segmenters (e.g. a stance-flip-seam model, GROUNDS §3) run only as declared silver strata — never as edits to the free cut.

## 6 · Identity & resolution

6.1 **Identity key = `(surface-id, span)`.** First materialization mints the block-id; any later segmentation producing the same key RESOLVES to the existing id — never duplicates (MUST; this makes re-runs idempotent).

6.2 **Overlap and nesting are legal and unremarkable.** A paragraph-block and a sentence-block within it coexist. Containment and order are COMPUTED from spans; hierarchy is never stored (MUST NOT). Every tree anyone draws over blocks is a query result.

6.3 **Identity is never path-dependent.** A block is a global citizen; its residence in session/turn/reply is provenance (edges), not address.

## 7 · Production events & provenance

7.1 Every production event MUST record: **actor** with delegation chain (human → session → agent → sub-agent; the jsonl's `parentUuid`/`isSidechain`/`promptId` carry this today; automations record their spec/plan ref — the spec being itself blocks), **context-parents** (which blocks were in view / replied-to), and time.

7.2 **Degenerate parents are legal but labeled.** At chat ingest, context = the linear prefix; record it AS degenerate (a prefix pointer), never dressed up as real attention. Block-native production (later) records real, sparse parents. The map must not lie about which it has.

7.3 Blocks inherit provenance through their surface's production event — nothing is stored twice.

## 8 · Composition — assembly & transclusion

8.1 **Assembly is a production event**: inputs = blocks, output = a new surface (form `assembled`), with an `assembled-from` edge to every source block (MUST). Assembled surfaces re-enter the grammar at §2 — the loop is closed.

8.2 **Transclude by default.** Lifting a block is re-addressing: same identity, new occurrence. A NEW block is minted only when new words are written; it takes `based-on` edges to its sources (MUST).

8.3 **Occurrences are addressable**: `(block-id, assembly-surface, position)`. Stance-marks attach to occurrences by default (§10.3).

8.4 **Buildup is not breakdown⁻¹.** Breakdown = lossless projection (mechanical, cheap). Buildup = generative synthesis that cites (intelligent, expensive). The consolidator, morning answer, weekly sense, and briefing are all buildup with a register parameter — one operation, not four machines.

8.5 **The severing gesture is copy-paste.** Interfaces built over this grammar MUST make assembly-with-parents cheaper than copying, or the provenance DAG tears exactly where reuse is highest. (Hard requirement handed to DESIGN ROOM.)

## 9 · The addressability universe (what can be pointed at)

Blocks · occurrences · raw spans (auto-mint to blocks on first use) · surfaces · production events · marks · edges · episodes · world-line artifacts (commit, file@rev — Regime-1 addresses) · **holes**. Anything a mind might take a stance on is addressable; if it isn't yet, pointing at it mints it.

**Holes are first-class** (RATIFIED 2026-07-08): an edge MAY carry an absent endpoint — an opposes-with-no-target, an untested serves-link. Holes are stored, queryable, renderable. This is how the unsaid becomes visible *before* it becomes expensive (the dissolution's lesson: you can only cheaply kill what has been minted; a hole shows where nothing was).

## 10 · Marks — interface (kind vocabularies deferred to the kinds round)

10.1 `mark = {target, scheme@version, kind, proposer, tier, confidence, run-id}`. Target ∈ the §9 universe.

10.2 **Tier law:** `silver` = machine-proposed (total coverage, cheap, fallible, displayed as such); `gold` = Sid-ratified or corrected, via ordinary in-flow gestures. Moving-on raises *confidence*, never *tier*. Corrections feed the marker's eval set — the river generates its own benchmark.

10.3 **Target defaults** (RATIFIED 2026-07-09): stance-marks (agree/disagree/kill/love) default to the **occurrence** — agreement is contextual; content-marks (factual error, kind-labels) default to the **type** (the block) — wrong everywhere. Both MUST be expressible.

10.4 Kind vocabularies are per-scheme and versioned; plural schemes coexist as strata. Cross-scheme reads require declared bridges (deferred; panproto-style lenses are the candidate mechanism — GROUNDS §7).

## 11 · Edges — interface + the mechanical floor

11.1 **Two families, kept separate** (MUST): epistemic — `supports · opposes · informs · grounds · answers`; process — the D-004 kernel kinds `based-on · supersedes · produced · dead-end · elaborates · references`, plus this spec's `assembled-from · refines`. `serves` (purpose-chain) and `invokes` (test) are recorded candidates, family-unassigned until the kinds round.

11.2 Every edge carries `asserted-by` + tier (the relation kernel's existing pattern is the substrate).

11.3 **The mechanical floor** (MUST — the cheapest, most certain edges in the system; no interpretation involved): write-tool call → `produced` edge to the world artifact touched; read-tool call → a `grounds` observation of what was read; assembly → `assembled-from`; refinement → `refines`; succession of authored blocks → `supersedes`. Tool blocks are the seam where the sense-line touches the world-line — an implementation gets these edges right before any model reads a word of prose.

## 12 · Episode — sketch only (not normative in v0)

One turn of the loop: tension → proposal(s) → deliberation → verdict-with-grounds → yield. Episodes are slow-pass groupings over CLOSED spans, owned by the consolidator; they close late; roles (tension, deliberation, yield) are positions filled by blocks and marks — the same words may later earn block-kind status by recurrence, and that merge, if it emerges, is recorded, not designed.

## 13 · Derived statuses — the projection law

Sediment / frontier / settled / live / dead are PROJECTIONS computed from engagement topology (what got landed-on, cited, collided-with). They MUST NOT be stored fields. Stored status rots; computed status is always current (pace-layer law at block grain).

## 14 · Re-run laws (Separability made normative)

Re-classify, re-cut, re-mark are re-projections: each new run is a stratum with its own provenance, and MUST NOT mutate or orphan prior blocks, ids, or marks. Better models re-read the whole past; the log appreciates. Nothing downstream may depend on there being exactly one segmentation or one marking.

## 15 · Worked fixture — the real head of `7c80ce2a` (9 events, from the 2026-07-08 peek)

| # | event (real) | class | actor | material outcome |
|---|---|---|---|---|
| 1 | `type: mode` | debris/harness-op | harness | none |
| 2 | `type: permission-mode` | debris/harness-op | harness | none |
| 3 | `type: file-history-snapshot` | debris/harness-op | harness | none |
| 4 | user event, `isMeta`, local-command-caveat (`e258a023…`) | debris/meta | harness | none — **user-role, not the human** (§3.4) |
| 5 | user event, `<command-name>/effort` (`ebc872dd…`) | debris/command | human-via-harness | retained, unmarkable |
| 6 | user event, command-stdout (`56352706…`) | debris/command-output | harness | none |
| 7 | `type: file-history-snapshot` | debris/harness-op | harness | none |
| 8 | user message "put on the principal-designer hat — …" | **river** | human: sid | surface S1; block B1 form `human-message` (whole); silver sub-blocks B1.1… (paragraphs + the bold-headed sections) per §4.4 |
| 9 | assistant `33df2523…`, `content[0]` = thinking | **river** | claude-fable-5 ⟵ on-behalf-of sid | surface S2; block B2 form `thinking`; usage/model metadata → production-event provenance, not material |

Seven of nine events are debris — **the ratio is reality**: most of the stream is not river, which is why classification is step zero and not a nicety.

## 16 · Conformance (machinery, not quality)

An implementation conforms when it demonstrates, on a real session file:

1. **Classification + actor resolution** — every event classed; no tool_result or meta event attributed to the human (§3.4).
2. **Free cut** — blocks with forms + declared segmentation provenance; fences/tables atomic; human messages whole + silver subs (§4).
3. **Idempotence** — re-running the same cut yields the same ids; a second segmentation adds a stratum, disturbing nothing (§6.1, §14).
4. **Reconstruction** — every surface re-renders its canonical text exactly from storage, and no planted secret survives anywhere in the store (§2.1, §2.3).
5. **Refinement** — a finer block mints on demand with engagement provenance; the coarse block and its marks persist (§5).
6. **Mechanical edge floor** — produced/grounds/assembled-from/refines emitted wherever detectable (§11.3).
7. **Holes** — an edge with an absent endpoint persists and is queryable (§9).
8. **Occurrences** — a transcluded block yields an addressable occurrence, not a copy (§8).

Break *quality* — do the seams land where stances flip? — is the dual-benchmark's question (Sid ∥ Fable, independent passes, then merge and reconcile), run only after this machinery stands. SPEC ≠ BENCHMARK.

## OPEN — Sid's rulings pending

1. Human-side silver sub-chunking default ON (§4.4) — feel-check at first render.
2. Header as own block (§4.2) — feel-check at first render.
3. All names (surface · form · free cut · river/debris · occurrence · hole · …) — scaffolding until recurrence + Sid's naming.
