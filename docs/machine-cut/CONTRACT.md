# Machine-Cut Contract — the LLM annotator asserts conversation structure as RelationEdges

Status: DRAFT v1 (Fable, 2026-07-12, session `machine-cut-contract`).
**Pending Sid:** (a) contract countersign; (b) the §4 `:pairs-with` enum
authorization (the ONE relation_kernel.clj edit — the framework §8 pre-named
form-break has fired); (c) wave dispatch. Realizes ROAD Step 5
(`build/framework/ROAD.md:93`) under D-004 (edges) + D-008 A2 (the machine
marker's lawful write path) + D-005 (the view ordered this data work).

Every source claim in this contract was re-verified at file:line by the
authoring session against the working tree at HEAD `d6bbde7`.

---

## 1 · Purpose, evidence, consumers, scope

**Purpose.** An LLM annotator asserts CONVERSATION STRUCTURE as D-004
RelationEdges with `asserted-by` first-class: machine-guessed structure lands
as silver marks (asserter-type `:llm`), visibly distinct from anything a human
asserts, resolvable to the run that produced it. First assertion type: **pair
bindings** — which river events form a `user-message ⊃ response(s)` unit (the
design's "turn"). Faces that want structure render it honestly; material with
no machine cut renders exactly as today.

**The D-005 evidence (why now, and why pairs first).** The W2 wearing
double-confirmed the lack: BOTH transcribed faces want `turn ⊃ user|response`
pair structure the `:conversation` projection cannot serve — v0 `:turns` are
single-speaker with flat `:blocks` (`face_projection.clj:56-88` groups by
`:event-uuid`, one turn per event), so Boxes' 4-layer russian doll flattens to
3 and the Minimap reader loses its user/response split. Sources:
`build/framework/W2-E-plurality.md` §Named-lacks item 1; `W2-INT.md` §Lacks
item 1; `RETRO.md` §5 item 1 ("the single most important residue"); gate G25.
The view ordered this data work — D-005 running in its intended direction.

**Consumers, in order (scope = consumer 1; 2–3 are extension points, D-001):**
1. The Boxes/Minimap faces becoming structure-honest: their pair-grouping
   binds served by the `:conversation` projection reading the new edges (§6),
   proven by a live wearing (§9 G14).
2. The Arcs/Score-class faces the design round holds — these want block
   ROLES (nucleus/satellite/aside). Roles ride the same annotator loop with
   new vocabulary later; NOT built here (§10.1).
3. The trail view's epistemic edges — episode boundaries (the sense-line
   unit, D-002 A1). Same loop, own package after the kinds/marks round
   (§10.2).

**What this package is NOT** (§10 in full): no roles, no episodes, no
auto-annotation daemon, no UI write surface, no multi-page annotation, no
gold-capture/reconciliation UI (board thread 2 owns that), no cross-asserter
merging, no new Rama module.

## 2 · Placement ruling

**New driver namespace `src/app/server/rama/machine_cut.clj`** — plain
Clojure over existing kernel APIs, sibling of the two driver precedents:
`git_spine.clj` (asserts import edges with stable keys, `git_spine.clj:215-235`)
and `code_atoms.clj` (assert/retract reconcile over a desired set,
`code_atoms.clj:812-832`). Pure core (input build, prompt render, output
parse/validate, pair diff) separable from the IPC shell, so goldens run
JVM-side with no LLM and no cluster.

Why not the alternatives:
- **Not a new Rama module.** The T11 class ("two truths drift",
  block-kernel §2): every piece of state this package needs already has a
  home — run lifecycle in llm-module, edges in the relation kernel, durable
  annotations in a WAL file (§5.5), material reads in the block kernel. A
  new module would duplicate ontology to hold nothing.
- **Not inside `dogfood/`** — that family is space-coupled (space/compute/
  llm/transcript); the annotator is a consumer of llm-module, not a member.
- **Not inside `object_container/`** — those are ingest adapters; machine-cut
  ingests nothing (writes no OC objects, mints no `imp:` prefix — §8 MC-T15).
- **Not inside `relation_kernel.clj`** — the kernel stays annotator-agnostic;
  its ONE edit is the enum line (§4.1).

**Projection changes live in `face_projection.clj`** (additive keys on the
existing `:conversation` projection — §6), and **boot attach in
`file_viewer.cljc`** (the dev wearing boot carries no rk runtime today —
W2-INT §Lacks item 5 pre-named this extension).

**Reversal cost.** The driver is leaf code behind one API
(`annotate-conversation!`). If run-tracking later outgrows llm-module (a
dedicated annotation kernel), the swap touches the driver only; edges,
WAL, and the projection read are unaffected. Edge placement is already
seam-protected by the relation kernel's query-only rule (rk CONTRACT §7).

## 3 · The ride: llm-module intent→executor, unchanged

The annotation RUN is an llm-module turn-run. No llm-module code changes.

- **Intent**: `turn-run-request` (`llm.clj:175-218`) appended via
  `append-turn-run-request!` (`llm.clj:1756`). Validation requires non-blank
  `:space/id :turn/id :context-bundle/id` and nothing space-semantic beyond
  that (`llm.clj:264-274` — blank checks only). Ruling: **synthetic,
  deterministic ids**:
  - `:space/id` = `"annotation:machine-cut"` (one annotation space, constant)
  - `:llm-thread/id` = `"mc-thread:" + <conversation object-key>` (runs for
    one conversation group into one llm-module thread — history for free)
  - `:turn/id` = `"mc-turn:" + <run-hash>`, `:llm-turn-run/id` =
    `"llm-run-mc:" + <run-hash>`, `:context-bundle/id` = `"mc-bundle:" +
    <run-hash>` where `run-hash = sha-256(conversation object-key,
    input-hash, annotator-version, salt)` (§5.1). Run ids are single-use,
    first-request-wins (`llm.clj:1497-1500`), and the default idempotency key
    `"space-event:<turn-id>:<bundle-id>"` (`llm.clj:195-197`) is therefore
    deterministic: re-submitting the same annotation intent is a total no-op.
    A deliberate re-guess over identical input takes an explicit `:salt`.
    *(Amended 2026-07-12, FALSIFY_A F1: the no-op is VERIFIED — the prior
    succeeded run's own WAL line is re-reconciled against current edge state
    and converged, never taken on run-state faith; a succeeded run with no
    WAL line returns honest `:stale-no-wal`.)*
- **Executor**: the driver claims and runs synchronously via
  `run-one-pending-with-claude!` (`llm.clj:2260-2267`) — headless
  `claude -p --input-format stream-json --output-format stream-json`
  (`llm.clj:1830-1842`; note: NO model flag — the run executes on the CLI's
  configured default; the receipt records the OBSERVED model from the run's
  observations, honest recording instead of unplumbed control, §10.8).
  The prompt rides the context bundle's `:rendered/model-input`
  (`llm.clj:1889-1896`). `load-context-bundle` is an injected fn
  (`llm.clj:2214-2228`): it REBUILDS the bundle deterministically from the
  recipe encoded in the bundle id (conversation, window, version) and
  verifies the rebuilt input's hash against the id — mismatch = the run is
  failed-stale, honestly (§8 MC-T9). No bundle blob is stored anywhere.
- **Observations** stream back through the existing obs depot; terminal
  status + result text come from the run's `:claude/result` fold. Dead
  letters must stay empty in every gate (§9 G7).
- **Side-effect law** (`feedback_rama_side_effects.md`; Slice-A lesson): the
  LLM process spawn happens in the DRIVER, never inside a topology event.
  llm-module's rows are the intent/lifecycle record; relation writes are
  foreign appends from the driver after validation. Nothing here can re-fork
  on a topology retry (§8 MC-T5).
- **Test seam**: `claude-stream-json-adapter` accepts canned `:lines`
  (`llm.clj:2179-2184`) — every suite gate runs with a fake adapter; only
  the §9 G13 receipt and G14 wearing touch a live Claude.
- **Auth**: subscription mode strips sensitive env (`llm.clj:1851-1865`).
  `env.clj` is never read (hard rule); no new secret plumbing.

## 4 · Edge schema — identity AND data-resolution (one section, by rule)

*(The work-package Amendment-B rule: the same section that fixes identity
fixes how consumers resolve the data.)*

### 4.1 The kind — `:pairs-with` (Sid's authorization required)

**Semantics**: `A pairs-with B` = river event A is a RESPONSE belonging to
the prompt event B. Directed: `from` = the response event, `to` = the
user-message (prompt) event. A design-"turn" (pair) = one prompt event + its
N response events, derived by grouping edges on `to`. Reading as a sentence:
"this response pairs-with that prompt."

**Why no existing kind fits** (audit of all 15, `relation_kernel.clj:58-80`):
`:references` = footnote citation (conflating citation with discourse
pairing makes every kind-filtered view lie); `:elaborates` = kraft-overlay
refinement (an answer is not an elaboration of its question); `:produced` =
construction lineage + the write-tool floor edge — a third meaning would
poison the trail view's product DAG with intra-conversation noise;
`:based-on` = derivation lineage on the wall panel — same product-DAG
pollution; `:grounds` = read-tool evidence; the stance kinds
(`:confirms/:refutes/:supersedes`) are judgments about claims; the
mechanical kinds (`:requires/:calls/:assembled-from/:refines`) are
code/assembly structure. Pair binding is discourse structure — a genuinely
new relation.

**The authorization**: `pairs-with` is the ONE pre-named candidate addition
(ROAD Step 5; framework CONTRACT §8: "a named authorization item requiring
Sid's explicit countersign on a used-form break — never a silent enum
edit"). The used-form break has fired (§1 evidence). The edit is one line in
`relation-kinds` (`relation_kernel.clj:58-80`), additive, this package's ONE
and ONLY kernel-file edit, landing only after Sid's countersign (recorded in
`decisions.md`, dated 2026-07-12 entry). Block-kernel P0 ruling (1) is the
precedent shape.

### 4.2 Endpoint addressing (existing forms only — verified)

- **target-id** = `(tid/chat-message-id <conversation object-key>
  <event-uuid>)` → `"oc:chat-message:<ok>:<uuid>"`
  (`transcript_identity.clj:18-20`). `event-uuid` is the river ledger's
  `:message-uuid` (`block_distiller.clj:1355`) — the transcript event's own
  uuid, deterministic across convergent re-ingest.
- **target-kind** = `:container` → `->target-ref` derives target-key via
  `oc/extract-object-key` (`relation_kernel.clj:848-854`), and the
  `oc:chat-message:` branch routes to the CONVERSATION object-key
  (`object_container.clj:329-330`). **Consequence, load-bearing: every pair
  edge of one conversation colocates under ONE target-key = the conversation
  object-key.** Both endpoint copies land under the same key
  (`relation_kernel.clj:505-508`), so a reader must dedup by `relation-id`
  (§6, §8 MC-T14).
- **No new target-kind, no new id fn, no kernel routing edit.**

### 4.3 Identity, provenance, idempotency

- **asserter-actor-id** = `"llm:machine-cut/v1"`; **asserter-type** `:llm`.
  The version is the ANNOTATOR version (prompt text + output schema +
  validation semantics), never the run: relation identity includes the
  asserter (rk CONTRACT §3), so a run-scoped actor would duplicate every
  edge per re-run (§8 MC-T1). Any change to prompt/schema/validation bumps
  v1→v2 = a new asserter; cross-version supersession is a pre-named
  extension (§10.7), v0 ships v1 only.
- **relation-id** = `relation-id-for(:pairs-with, from, to, actor)` —
  deterministic; re-annotation converges on the same ids (rk trap 2).
- **idempotency-key = request-id** = stable per logical TRANSITION. *Amended
  in place 2026-07-12 at the gate (FALSIFY_A F2/F3 — the pre-registered ≥1
  contract-text-error ledger's instance for this contract): v1's flat
  `"mc:" + relation-id` was stable per-edge-FOREVER, and the relation
  journal REPLAYS a duplicate key's prior decision — so
  assert→retract→re-assert replayed the original assert decision and the
  edge stayed retracted (the map lied); WAL replay could not converge an
  A→B→A history either.* The rule: the FIRST assert of a never-seen
  relation keys `"mc:" + relation-id`; any transition on an INCUMBENT row
  keys `"mc:" + relation-id + ":" + <desired-status> + ":" + <incumbent
  status-changed-at-ms>` — deterministic from READ state, so a client retry
  of the same transition re-derives the same key. MC-T2's ban stands: never
  a run-id, never a wall clock (the framework W2-F3 fire class). Discipline
  precedent: `git_spine.clj:215-235` (whose edges never flip and so keep
  the flat key legitimately).
- **evidence-source-id** = the response event's FIRST river block's
  `:source-id` (a real per-part SourceArtifactRow,
  `block_distiller.clj:1293-1301`); evidence-anchor-id nil in v0.
- **note grammar v1** (documented string, git-spine precedent):
  `"machine-cut v1 run=<run-id> model=<observed-model>
  window=<blocks-returned>/<river-events-total>"`.
- **asserted-at-ms** = the run's terminal wall time; `sent-at-ms` the same
  (two-clock discipline: this is when the machine asserted, distinct from
  any later re-ingest).

### 4.4 Data-resolution (the consumer route, fixed here)

The `:conversation` projection resolves pair structure by ONE call:
`rk/read-relations-for-targets(rk-rt, [<conversation object-key>],
[:pairs-with], false)` (`relation_kernel.clj:937-946`) — one target-key
(§4.2 colocation), retracted rows already filtered, then: dedup rows by
`relation-id` → keep rows whose `asserter-actor-id` = the configured
machine-cut actor (v0 constant in `machine_cut.clj`, imported by the
projection — one constant, two call sites) → group by `to` target-id →
pairs. Foreign asserters are COUNTED, never merged (§6, §8 MC-T10). The
read plan: 1 descriptor seek + 1 subindexed range scan per serve, added to
river-page's `1 + 4·limit` bound — O(limit) preserved.

## 5 · The annotator (driver laws)

### 5.1 Input = the projection's own serve (coverage parity)

The annotation input is the SAME data the faces wear: the pure shape of
`conversation-projection` output (`face_projection.clj:175-203` — river-page
at the same clamped limit, default 64, `block_distiller.clj:1233-1237`),
`until-ms` nil. Never a second read path over raw transcript state (§8
MC-T4; the framework T12 class). The **window record** `{address, limit,
river-events-total, blocks-returned, truncated?}` and the **input-hash** =
`sha-256` over the ordered `(event-uuid, unit-id, sha-256(text))` triples
travel into the run-hash (§3), the WAL (§5.5), and the receipt (§9 G13).
**Coverage-verb law**: every coverage claim this package makes names its
verb over the WINDOW, never the conversation — the claim grammar is
"window W of conversation C: N events classified, P pairs asserted"; the
`:truncated?` flag rides every artifact.

### 5.2 Output contract (strict, closed-world, total)

The prompt instructs strict JSON:
`{"pairs": [{"prompt": "<event-uuid>", "responses": ["<event-uuid>", ...]}, ...],
  "unpaired": ["<event-uuid>", ...]}`
covering EVERY shown event exactly once. Ambiguity policy, fixed: **when
unsure, unpaired** — silver marks under-claim, never over-claim (the map
must not lie, applied to guesses). Prompt wording is lane freedom within
this contract.

### 5.3 Validation (driver-side, before any write)

Classification is total and keyed on the THING (the /atomize A5 law):
- Closed world: every returned uuid must be in the shown set; unknown uuids
  reject the CONTAINING assignment and are counted (`:unknown-ids`) —
  hallucinated ids must not mint dangling-legal edges (§8 MC-T3).
- Totality: shown events missing from the output → `:unpaired` + counted
  (`:unclassified`). Each event in at most one pair; duplicates counted +
  first-in-river-order wins deterministically.
- Prompt-side actor law: a pair's `prompt` event's actor must be `human:*`
  (`resolve-actor`, `block_distiller.clj:187-199` — tool results are `tool`,
  harness noise is `harness`; T1 role≠actor is already the kernel's law).
  Violations reject the pair + count (`:non-human-prompts`).
- Malformed JSON / non-terminal run → the run is recorded failed; ZERO edge
  writes; honest counts. The validation verdict (accepted pairs + every
  rejection count) is the WAL line's core (§5.5).

### 5.4 Reconcile (re-annotation must not let the map rot)

Desired set = the validated pair edges. Existing set = §4.4's read filtered
to the machine-cut actor, scoped to the annotated window's event ids. Diff →
asserts (new), retracts (stale — `retract-request` with the ORIGINAL
asserter matched, `relation_kernel.clj:888-894`), unchanged (no-op; the
journal replays). This is durable-read reconcile — better grounded than
code-atom's in-memory basis because HERE the existing set IS queryable
(colocated edges, §4.2); no basis atom, nothing to lose across boots.

### 5.5 Durability: WAL-first on the ephemeral dev cluster

The dev substrate is an in-process `create-ipc` cluster — rk state does not
survive a boot; paid LLM output must (§8 MC-T7). Discipline (the arsenal
wear-log + git-spine `/assert` precedent, `face_arsenal.clj:244-306`,
WAL-first): one edn line per completed annotation run, written BEFORE the
relation appends, carrying: annotator-version, address, window record,
input-hash, run provenance (run-id, observed model, terminal status,
timestamps, token usage if present), and the validated pair-set + rejection
counts. Boot replay: re-derive the edge requests from each line
deterministically and re-append (journal + deterministic ids converge; ZERO
adapter calls — §9 G11); torn trailing line dropped honestly (arsenal
`:146` precedent); later lines re-apply reconcile in file order, each line
scoped to its OWN annotated window *(amended 2026-07-12, FALSIFY_A F6 — an
address-wide replay reconcile would retract edges outside the window the
line annotated)*. WAL path follows the arsenal's relative-path convention;
compaction is a non-goal.

### 5.6 Completion side-effects

After relation appends ack: bump `util-fns/!ingest-epoch-atom`
(`util_fns.cljc:134`) exactly like a watcher import
(`ingest_watchers.clj:199`) so INV-19 re-pulls fire and worn faces
re-render without a re-wear command. The W1 epoch-bump defect (framework
RETRO §2a) is THE trap here (§8 MC-T6); the wearing gate proves it live.

### 5.7 Trigger surface (v0)

A driver fn invoked from the dev REPL / Claude Code CLI:
`(machine-cut/annotate-conversation! ctx address opts)`. D-008 A2's writer
is exactly this agent-on-Sid's-instruction path. No workspace command, no
Electric write surface (§10.4).

## 6 · Projection serve (additive, total)

`conversation-projection` gains pair structure; **`:turns` stays
byte-identical** (§8 MC-T8 — shipped faces must not notice). Additive keys:

- `:pairs` — vector in river order of prompt turns:
  `{:id <prompt event-uuid> :user-turn <turn> :responses [<turn> ...]
    :asserted-by "llm:machine-cut/v1"}`; responses in river order.
- `:unpaired` — turns in no surviving pair (asides, harness noise, prompts
  the annotator declined, trailing material), river order. Totality law:
  every served turn appears in exactly one of `:pairs` (as prompt or
  response) or `:unpaired`.
- `:conversation/structure` — `{:source :machine-cut|:none :asserter <actor>
  :pairs-count N :unpaired-count M :edges-read K :conflicts C
  :foreign-asserter-edges F}` plus a pre-composed honest header string
  `:conversation/structure-line` (e.g. `"machine-cut v1 · 12 pairs · 3
  unpaired"` / `"no machine cut"`) — computation lives in the projection,
  never the assembly (the §4-guard division of labor).

Grouping laws: derive pairs AFTER the `apply-until-ms` cut (§8 MC-T11 —
prefix consistency, G11 block-kernel class): a cut prompt strands its
responses in `:unpaired`; a prompt whose responses are all cut keeps an
empty `:responses`. Edge conflicts (one response event with two prompts)
resolve deterministically (river-order-earliest prompt) + count in
`:conflicts`. Absent `rk-rt`, no edges, or a read failure → `:pairs []`,
`:structure {:source :none}` — total, never a throw (the serve totality law,
`face_projection.clj:364-388`).

Boot: `file_viewer.cljc`'s face runtime attaches `rk-rt` into face-ctx
(W2-INT §Lacks 5's pre-named extension) and wires WAL replay after attach.
**Delay-totality gate class applies** (work-package Amendment C; the W2-F7
fire, poisoned-but-total precedent `file_viewer.cljc:254-262, 340-343`):
every lazily-booted handle added here yields a poisoned-but-TOTAL value on
failure, never a cached throw (§8 MC-T12).

## 7 · The wearing surface (consumer 1 proven end-to-end)

A NEW pairs-aware face (working name `boxes-paired-face`, name is the
identity — framework §17) transcribed from Boxes, binding `[:pairs]` with
user|response frames and the `:conversation/structure-line` header (the
silver mark: machine-guessed structure NAMES itself on the face). The worn
`boxes-face` is NOT mutated; the new face's envelope carries
`:assembly/based-on "boxes-face"` (the §17 lineage machinery does the
rest). Expected to need ZERO interpreter/grammar changes — if a
transcription genuinely cannot express the pair nesting, that is a
stop-clause (§11), never an improvised grammar edit.

## 8 · Traps ledger (cite MC-T numbers in code comments)

| # | Naive choice | Concrete failure | Ruling |
|---|---|---|---|
| MC-T1 | Run-scoped asserter actor (`llm:.../run-abc`) | Identity includes asserter (rk §3) → every re-run duplicates every edge; the view stacks ghost pairs | §4.3: actor = `llm:machine-cut/v1`; run provenance in note + WAL |
| MC-T2 | run-id (or any per-run volatile) in the relation idempotency key | Fresh key per run defeats the journal (framework W2-F3 fired exactly this way) | §4.3: key = `"mc:" + relation-id`, stable |
| MC-T3 | Trust LLM-returned event ids | Dangling targets are LEGAL in rk (trap 3) → hallucinated uuids mint silver edges to phantoms; the map lies | §5.3 closed-world validation; unknown ids rejected + counted |
| MC-T4 | Annotator reads raw transcript state directly | Second read path drifts from what faces wear; coverage claims lie (framework T12 class) | §5.1: input = the projection's own serve; window + hash recorded |
| MC-T5 | LLM spawn inside a topology event | At-least-once retry re-forks the process (Slice-A; `feedback_rama_side_effects.md`) | §3: spawn in the driver; topologies only record intent/state |
| MC-T6 | Edges land, no epoch bump | Faces stay flat, masking now-present truth (the W1 G15 epoch defect, found only live) | §5.6 bump like a watcher import; G14 proves it worn |
| MC-T7 | rk-only persistence on the ephemeral dev cluster | Reboot silently discards paid annotations; re-runs guess differently → the map flaps | §5.5 WAL-first + boot replay, zero adapter calls |
| MC-T8 | Reshape `:turns` into pairs | Every shipped face breaks on next wear | §6: additive `:pairs`/`:unpaired`/`:structure`; `:turns` byte-identical |
| MC-T9 | Deterministic run-id without the input-hash | Content changes but the run dedups as already-done (or a stale bundle annotates silently) | §3/§5.1: hash in run-hash; rebuild-mismatch = failed-stale |
| MC-T10 | Projection merges asserters or silently picks one | Gold/silver mush — provenance destroyed at serve time (rk trap 5's read-side twin) | §4.4/§6: v0 serves the machine-cut actor only; foreign counted, never merged |
| MC-T11 | Pair-group before the until-ms cut | Scrub shows pairs containing cut blocks — prefix inconsistency (block-kernel G11 class) | §6: group after the cut; cut sides degrade honestly |
| MC-T12 | Lazy rk/llm boot handle that caches a throw | A `Delay` re-throws forever → one boot failure = permanent black serve (W2-F7) | §6: poisoned-but-total handles (Amendment C gate class) |
| MC-T13 | Mechanical baseline smuggled in as asserted truth | Provenance lies about the mechanism that guessed | §9 G13: baseline is a gate-side comparator ONLY; v0 asserts nothing mechanically |
| MC-T14 | Count pairs from raw `$$relations-by-target` rows under the conversation key | Both endpoint copies of every intra-conversation edge live under ONE key (`relation_kernel.clj:505-508`) → everything double-counts | §4.4: dedup by relation-id before grouping |
| MC-T15 | Minting an `imp:` prefix / OC objects for annotations | The thrice-fired foreign-read routing class, for objects nothing needs | §2: machine-cut writes edges + WAL only; no OC writes (G16 scans it) |

## 9 · Acceptance gates

Suite gates (G1–G12) run with the fake adapter (`llm.clj:2179-2184` canned
`:lines`) — no live LLM, IPC only where stated. Every gate is executable;
style gates name where they stop.

**Pure (JVM, no cluster):**
- **G1 input determinism** — same synthetic data-context → identical
  input-hash, bundle/run/turn ids; one changed block text → all change.
- **G2 validation totality** — canned outputs: valid / unknown-uuid /
  missing-uuid / duplicate-uuid / non-human-prompt / malformed JSON → exact
  accepted sets + rejection counts; malformed → zero planned writes.
- **G3 pair-plan diff** — desired vs existing fixtures → exact
  {asserts, retracts, unchanged}.
- **G4 grouping pure core** — synthetic turns+edges → `:pairs`/`:unpaired`/
  `:structure` exact; totality (every turn exactly once); conflict pick
  deterministic; until-ms interplay (cut prompt → responses unpaired; cut
  responses → empty `:responses`); dedup-by-relation-id (MC-T14 pinned with
  a both-copies fixture).

**IPC (fake adapter):**
- **G5 end-to-end assert** — fixture corpus → llm-module run rows exist
  under the synthetic ids; rk edges land with §4 identity/provenance/
  evidence/note exactly; dual-readable from both endpoint target-ids AND
  via the one conversation-key read; projection serves the pairs.
- **G6 idempotent re-run** — same input+version: second invocation performs
  ZERO adapter calls (run decision replays), relation journal replays, all
  read-back PStates value-identical.
- **G7 never-drop** — `$$llm-dead-letters` empty across all gates; every
  annotation observation folded into a known run.
- **G8 reconcile** — canned v2 output moves one pairing → stale edge
  `:retracted` (history shows both events), new edge asserted, projection
  reflects; unchanged edges untouched (journal, not re-fold).
- **G9 epoch** — the epoch atom bumps by ≥1 only AFTER edge acks; no bump
  on a failed/zero-write run.
- **G10 foreign-asserter isolation** — a hand-asserted `sid` pairs-with
  edge on the same events: projection serves machine-cut pairs only,
  `:foreign-asserter-edges` counts it, nothing merges.
- **G11 WAL replay** — fresh cluster + existing WAL → edges re-asserted,
  ids identical, ZERO adapter calls; torn trailing line dropped with the
  rest replayed.
- **G12 serve totality** — absent rk-rt / no edges / poisoned handle →
  `:pairs []` + `:structure {:source :none}`, never a throw; `:turns`
  byte-identical to the pre-package golden.

**Receipt + wearing (live; the first full-real-corpus contact):**
- **G13 receipt (real corpus, real Claude, run once and kept)** — annotate
  the default worn conversation's served window (the `7c80ce2a` corpus:
  35-turn/64-block page of 247 river events). The receipt ASSERTS, against
  durable state, never merely prints: every served event-uuid has exactly
  one disposition; every asserted edge's endpoints ∈ the served set;
  rk-visible edge count == the plan's assert count; WAL line present and
  replay-equivalent. It RECORDS: observed model, token usage, rejection
  counts, and the **mechanical-baseline comparison** — agreement % against
  plain adjacency grouping (each `human:*` event opens a pair; following
  non-human events belong to it), with EVERY disagreement enumerated and
  classified (annotator-right | annotator-wrong | ambiguous). The
  comparison is honest-ledger data for the kinds round and D-006 — the
  baseline asserts nothing (MC-T13).
- **G14 the wearing (QC layer 5)** — wear the pairs-aware face live over
  the annotated conversation: pair frames render; the structure-line names
  the machine provenance; then re-annotate (salted) and confirm the face
  re-renders WITHOUT a re-wear command (epoch law, MC-T6). Evidence capture
  asserts op-count parity FIRST (Amendment D — a blank capture is a harness
  bug until op counts disagree).

**Style gates (mechanical, each names where it stops):**
- **G15 fences** — `face_projection.cljc?` remains read-only: no depots, no
  topologies, no direct PState paths (form scan over the file, the
  framework-G12 precedent; stops at this one file). `relation_kernel.clj`
  diff = exactly the one enum line. `transcript_identity.clj`,
  `object_container.clj`: zero edits. No new `imp:` prefix anywhere in the
  diff (grep; stops at the package diff).
- **G16 hard rules** — `env.clj` unread/untouched (grep the diff; stops at
  the diff); no `try` inside any `e/defn` (none should be touched at all);
  no raw control bytes in source literals.

## 10 · Non-goals (each an extension point, none a void)

1. **No block roles** (nucleus/satellite/aside) — consumer 2's need; same
   loop, new vocabulary, own authorization round.
2. **No episode boundaries** — the sense-line unit (D-002 A1); rides the
   kinds/marks round (the "sidetrackkkk"-branch evidence pile waits there).
3. **No auto-annotation** (watcher/daemon) — v0 is command-worn; a watcher
   is one `on-import` hook away when a used form demands it.
4. **No UI write surface / workspace command** — D-008 item-5 gesture work;
   v0's writer is the CLI agent (D-008 A2).
5. **No multi-page annotation** — the window is river-page's first page,
   same as the faces wear (block-kernel §10 scale extension owns paging).
6. **No gold capture / reconciliation UI** — board thread 2. The edge
   substrate already supports Sid's gold overlay (his assertion = a new
   identity; rk §3), which is exactly what thread 2 needs served later.
7. **No cross-version supersession** — v2 retracting/superseding v1's edges
   is designed-for (stable per-version actors) but unbuilt until v2 exists.
8. **No model plumbing** through the bundle→argv path; observed-model
   recording only (§3).
9. **No new Rama module, no durable-cluster work** (Sid's durability fork
   stands untouched), **no benchmark-room work** (SPEC ≠ BENCHMARK).

## 11 · Stop clauses (escalate, never improvise)

- Sid's `:pairs-with` countersign absent when a lane reaches the enum edit —
  everything else can proceed (the edit lands at INT), but no edge-writing
  gate may run against a patched-in kind without the countersign recorded.
- Any need for a SECOND relation_kernel.clj / object_container.clj /
  transcript_identity.clj edit.
- llm-module semantics conflicting with synthetic ids at implementation
  contact (e.g. a validation path this contract's file:line reads missed).
- The pair nesting inexpressible in the assembly grammar without logic
  (grammar change = amendment; framework §4 guard).
- The projection unable to meet the §6 totality laws without a kernel edit.
- Two binding docs in genuine conflict (classify implementer-fixable vs
  policy fork first — the skill's rule; a gate one-liner strict-fail is
  classified literal-vs-intent before enforcement).

## 12 · Input manifest (D-006 probe, pinned even while waived)

`decisions.md` D-004/D-005/D-008(A2)/D-010 + the 2026-07-11 blanket;
`build/relation-kernel/CONTRACT.md` §§3-8; `relation_kernel.clj:58-80,
223-282, 505-508, 843-957`; `object_container.clj:284-345`;
`transcript_identity.clj:14-20`; `block_distiller.clj:187-210, 1233-1404`;
`face_projection.clj` (whole file); `llm.clj:162-330, 1447-1560, 1830-1896,
2130-2268`; `git_spine.clj:215-235`; `code_atoms.clj:812-916`;
`face_arsenal.clj:244-330`; `util_fns.cljc:120-140`;
`ingest_watchers.clj:180-205`; `file_viewer.cljc:254-262, 340-343`;
`build/framework/{ROAD.md §Step5, RETRO.md §5, W2-INT.md §Lacks,
W2-E-plurality.md §lacks, CONTRACT.md §8/§17/§20}`;
`memory/feedback_rama_side_effects.md`. Probe question: "design the
machine-cut (LLM structure annotator) contract for this substrate."

## 13 · Handoff

- **Lanes (Opus 4.8 subagents, one phase per fresh context, disjoint
  fences; both boot `/rama` + `/rama-pitfalls` before Rama-touching code;
  prompts pinned in `build/machine-cut/LANES.md`):**
  - **Lane A — the driver**: `machine_cut.clj` + `test/app/machine_cut_test.clj`
    (+ fixtures). Gates G1–G3, G5–G9, G11 (fake adapter throughout). May
    NOT touch face_projection/file_viewer/kernels. Cites MC-T numbers.
  - **Lane B — the serve + the face**: `face_projection.clj` additive keys
    + pure grouping core + tests; the pairs-aware face `.edn` + golden.
    Gates G4, G10, G12 (hand-seeded rk edges — no driver dependency; runs
    parallel to A). May NOT touch machine_cut/llm/kernels.
- **INT (this orchestrating session, Fable — the 2026-07-05 (e2) shape):**
  the enum line (post-countersign), `file_viewer.cljc` boot attach + WAL
  replay wiring (delay-totality), G13 receipt, G14 wearing, G15/G16 scans,
  ONE serial suite at the end of the wave, falsification-by-class batch,
  gate review per the CLAUDE.md protocol.
- **Allowlist (complete):** NEW `src/app/server/rama/machine_cut.clj`,
  `test/app/machine_cut_test.clj`, test fixtures/goldens, one face `.edn`;
  EDIT `face_projection.clj` (+ its tests), `file_viewer.cljc` (boot only),
  `relation_kernel.clj` (the one enum line, Sid-gated). Nothing else.
- **Definition of done:** all gates green including G13's asserted receipt
  and G14's wearing; suites re-run at committed HEAD (close protocol step
  2); docs committed; retro + adversarial recheck; board pruned.
- **After green:** consumer-2/3 extension decisions (roles, episodes) ride
  the G13 disagreement data + the kinds round to Sid; thread 2
  (reconciliation UI) picks up the gold overlay against these edges.

## 14 · D-006 pre-registration (evaluation notes anchor)

Criterion 1 (traps): 15 entries above, each naive-alternative → concrete
failure → ruling. Criterion 3 (implementation contact): this contract must
survive to green gates without amendment; the honest ledger expects ≥1
contract-text error to be caught by the fresh layers — that is what they
are for. Criterion 4 (ledger): contract session ≈ one read-heavy Fable
session (this one); lane + INT costs recorded in NOW at close. Criterion 2
stays WAIVED per the 2026-07-11 blanket (manifest pinned in §12; reopen
condition unchanged). Additional pre-registered honest-ledger item: G13's
mechanical-agreement number + disagreement classification — recorded for
the kinds round and the final D-006 evaluation, not argued from.
