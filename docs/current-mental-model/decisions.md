# Softland Decision Log — current law

What is decided, right now, grouped by axis — never how we got here. This file
holds CURRENT STATE ONLY (Sid, 2026-07-12): amendments edit the text in place,
superseded text is deleted, and git keeps every prior form — look at this
file's git history only if Sid wants to see what was here before. Execution
records (gates, retros, fix waves, countersign events) live in `build/<pkg>/`
artifacts; "where are we now" lives on the board (`docs/sessions/next-prompt.md`).

Companion: `BETS.md` (same directory) — North (Sid's authorship only) + the bet
ladder. This log governs closures; BETS.md governs direction.

## Operating rules

- A decision is **ACCEPTED by default**. Only a decision of the type "forever
  consequences / huge irreversible cost" stops for Sid's sign-off. Operating
  test (D-010): if this is wrong, is undoing it a revert or a rewrite?
  Revert-cheap ⇒ proceed and record; rewrite-costly ⇒ Sid.
- Every decision records the evidence it rests on — one line pointing at the
  artifact that holds the detail — so a reopening attempt can be checked
  against what was already known.
- A standing decision reopens only on **evidence of a used form breaking
  against it** — never on a new argument, research round, or model opinion.
- D-numbers are stable names, not positions — cite them from anywhere. Axes
  grow, shrink, and merge as the territory changes. A new ruling takes the
  next D-number and lands under its axis (mint a new axis if none fits); it
  does not append a diary entry.

## Governance

- **D-001 — arbiter rule.** The runtime/substrate grows only when a form in
  actual daily use breaks against it; "an imagined future form would need X"
  is never a reason to build X. Evidence: Sid's own recurring
  runtime→too-big→need-a-view→runtime loop (four iterations) and the 27-04
  wall margin note ("my crisis is based on me trying to build either end
  without the other").
- **D-010 — approve-by-default.** Work products (contracts, phase closes,
  retros, batch scopes, wave dispatches) are approved by default: adopt the
  recommended option, record alternatives. Only future-binding cost —
  irreversible architecture, vision/BETS authorship, spend, foreclosing a
  named door — stops for Sid. Reach (2026-07-08): the same test governs
  Fable's own behavior; sessions commit their own work without word-gates.
  2026-07-12: promoted to this log's own operating rule (header above).
  Work happens on **three frontiers only: rama · ui · framework**; the
  benchmark front is pushed back (resume is Sid's call; its rankings feed
  intake when resumed).
- **Standing stops, unaffected by any default:** never push/merge the docs
  branch; never read `env.clj`; code and docs in separate commits; North
  stays Sid-authored; spend stops for Sid.

## The form — what we are building first

- **D-002 — first form = the trail view** (Sid's 27-04-2026 wall panel):
  timeline · what-changed-concretely · product-DAG with dead-ends, answering
  "where are we / how did we get here / what crossroads did we take" —
  rendered in Softland, over Softland's own material. View 3 (agent-legible)
  is part of the same form: the material must be legible to agents, not only
  to Sid. **Unit (A1, 2026-07-08): sense-line units** — episodes and their
  marks; the container trail (sessions/files/commits) demotes to one
  evidence-lens. Model: `sense-line-model.md`; working map:
  `build/sense-line-mvp/DIRECTION.md`. Evidence for the unit rescope: the
  2026-07-07 form-break (`design/claude/atomic-unit-2026-07-07.md` §1 —
  container granularity cannot meet this decision's goal at any rendering).
- **D-005 — view-first sequencing.** The view renders NOW on material already
  in the substrate; it will be ugly and gappy, and each gap names the next
  ingestor slice — the rendered view orders the import work, never the other
  way around. Evidence: the wall margin note (data and view co-evolve; a
  completeness-gated import round has no signal for "done enough").
- **Face order** (ruled 2026-07-04 on Sid's delegation): **View 3 first** —
  nearly coextensive with the data contract itself, a minimal text projection;
  the threaded/DAG timeline second, as Sid's first pixel surface; canvas
  parked until the wall-replacement ambition earns activation.
- **D-008 — write surface.** The view is READ-ONLY and structurally cannot
  write (declares no depots, no ETL topologies). The write surface is the
  Claude CLI: the agent asserts on Sid's instruction — payload asserter = sid,
  envelope actor = the agent, and the write path durably records BOTH.
  Machine marks (A2, 2026-07-08) write as provenance-first observations via
  the lawful worker→Rama path (back-arrow compliant; silver/gold provenance
  tiers visible — the map must not lie). Human write-gestures through the UI
  stay gated until spec'd: read→write is the named second milestone for UI
  gestures, entered on observed read-only friction. Watchers are triggers
  over existing ingestors, never new ingestors. Evidence: Sid's 2026-07-04 +
  2026-07-08 rulings, verbatim in `vision/LOG.md`.

## Code regimes — how code enters the land

- **D-003 — Regime 1: external code as VIEW.** git owns versioning, the
  filesystem owns storage; Softland imports a derived view. Address the code
  (blob-sha + path anchors), don't copy it. Two boundaries: import (git→Rama,
  commit-grained) and commit (Rama→git); Rama is truth only for in-flight
  work between them; on conflict git wins. Settled inputs: git-authority,
  commit-boundary ingest, exactness ("the map must not lie"),
  file-granularity, fail-closed scope (`env.clj` can never enter). The first
  form needs only the **Regime-1 spine**: commit metadata + sha/path
  addresses + the transcript↔commit/doc joins. Evidence: Sid's 2026-06-30
  consolidation ("code ingestor" was two problems wearing one name; the tell
  is the unit — flow vs code).
- **D-011 — middle regime: the land's furniture as the land's data.**
  Assemblies are arrangement-only DATA; no code enters Rama — keywords and
  addresses persist, fn values never (framework CONTRACT §4/§6/§8). Anything
  that wants to be a program gets to be a real one, in the code lane.
  Machine-written assemblies ride D-008's worker path and land validated +
  error-carded, never executed. Evidence: the shipped substrate already split
  exactly this way (`rect_tree.cljc` header records the desire path);
  live-medium precedent (ROAD § invariant 2).
- **D-012 — the Regime-2 gate (self-hosting test).** Regime 2 (recursive
  Softland; unit = capability) opens only when a design conversation produces
  a usable view without leaving the land and without hand-translation:
  conversation → proposed assembly → validated + rendered beside the chat →
  worn by Sid. Passing for ARRANGEMENT does not open Regime 2 for CODE — that
  additionally needs a used form breaking against the code lane's hot-reload
  speed. `object-kernel-revision.md` is entirely gated as authority behind
  this test (ruled 2026-07-04); no Regime-1 artifact may cite it as a
  requirement source.

## Kernel & data model

- **D-004 — the missing noun is a typed RelationEdge.** Landed:
  `src/app/server/rama/relation_kernel.clj` (`relation-kernel-module`).
  Every relation carries first-class `asserted-by` provenance
  (sid | llm | import) so LLM-proposed glue is visibly distinct from
  human-asserted structure. Kinds are a **closed enum** — the source of truth
  is `relation-kinds` in the kernel (wall arrows + stance + mechanical +
  sense-block + discourse families) — grown additively on form-break only:
  one authorized kernel edit per package, each kind naming its direction
  semantics at registration. Precedent chain: block-kernel
  `:grounds :assembled-from :refines` · code-atom `:requires :calls` ·
  machine-cut `:pairs-with` (Sid, 2026-07-12). Contract:
  `build/relation-kernel/CONTRACT.md`.
- **Two-clock discipline** (law for every new Regime-1 artifact carrying
  time; adopted 2026-07-04): claimed-time (`asserted-at-ms`, orders semantic
  history) is distinct from ingest/arrival-time (when the land learned it) —
  a July re-import of April notes must not render as "today."
- **Custody vs assertion** (same ruling): wherever envelope actor and payload
  asserter legitimately differ, the write path durably records both.
- **Transcript↔commit joins are durable import-asserted RelationEdges**
  (ruled 2026-07-05): asserter-type `:import`, version-free actor-id
  (`"import:git-spine"`), shas verified against the repo before asserting,
  exactness via note-grammar v1 (promoted to a structured field only when a
  view demands rendering it). Grounds + traps: `build/git-spine/CONTRACT.md` §2.

## Render substrate

- **D-009 — ONE SUBSTRATE** (Sid, 2026-07-06: "one substrate to rule them
  all"): one scene store keyed `(view-instance, address)`, one diff pipeline,
  one camera loop, one spec grammar; the world, the islands, and the token
  face are PROJECTIONS over the single substrate — seams are where
  projections change, never where architectures change. Ruled on foreclosure
  asymmetry: three-substrate foreclosures are vision-shaped and permanent;
  one-substrate foreclosures are cost-shaped and dated. Binding on DIRECTION;
  build stays D-001-paced. Ordered instrument: `build/render-north/DELTA-B1.md`
  (Δ1 H6-keying + Δ2 f64-store/camera-relative-f32 bind at scene-store birth;
  Δ3 store promotion at the FACE-2 contract; Δ7 slug-glyph expansion gates
  face-2 shipping the design language). One substrate ≠ one program: the two
  thin registries are the modularity story. Full record:
  `build/render-north/FORK-2.md`.

## Editor write transport

Pre-registered criterion, used three times unchanged: echo p95 ≤ 50ms AND ≤1
stall >100ms per sustained minute.

- **D-013 — microbatch REJECTED** for the editor/block-write transport: echo
  p95 307–394ms, every event a stall; the ~210ms floor IS the microbatch
  iteration cadence (payload- and load-independent — the topology CLASS, not
  a tuning problem). No tuning-rescue rounds. The 2026-03 "7.5ms" figure was
  the optimistic ONE-WAY write — never cite it as echo evidence. Numbers:
  `build/write-echo/NOW.md`.
- **D-014 — transport COMMITTED: direct write over a STREAM topology, no
  optimistic text echo** (Sid, 2026-07-12). Stream echo p95 7.66ms (~6.5×
  under budget), 0/1620 stalls; composed E2E ~11ms; the caret rides the echo
  signal (the L8 co-variance law). Binding obligations: (1) **op-id
  idempotency is CONTRACT-BINDING on block-write** — deterministic op-id from
  the request-id so a retried stream event overwrites the same keys (stream
  is at-least-once); an acceptance gate must exercise a replayed event.
  (2) The **local caret affordance** is the only pre-registered concession if
  Sid's fingers find caret feel broken despite the numbers — text truth stays
  streamed either way. (3) **Clustered re-measure is a mandatory gate at
  substrate change** (same criterion; probe repro is one command,
  `src/app/probe/stream_echo_probe.clj`). (4) The **browser E2E closes at the
  block-write acceptance gate** — real leg-3, same criterion, Sid's fingers
  final. (5) Module shape (own stream topology vs text-kernel write-path
  migration) is block-write CONTRACT territory, bounded by (1) either way.
  Optimistic-with-reconciliation returns only via a form-break against the
  shipped transport, never via argument. Numbers + method:
  `build/write-echo/NOW.md`.

## Working model — how the work runs

- **D-006 — Fable allocation** (bet; evaluation running). Fable's window =
  contracts, gate reviews, fork adjudication, bet formation + the questioning
  practice (D-007), succession docs — and, since delivery mode (Sid,
  2026-07-05: "our goal is delivery"), Fable MAY implement directly when it
  is the fastest path. QC shape: ONE batched falsification + ONE end-gate per
  coding wave, never sprinkled per-phase; **fresh CONTEXT ≠ fresh session** —
  one orchestrating session runs phases with fresh-context subagents as the
  QC layers. Evaluation state: criteria pre-registered 2026-07-03 (traps
  caught · counterfactual probe · implementation contact · token ledger).
  Consolidated finding across three packages: the load-bearing QC layer is
  **fresh-context + real-corpus + default-fail** — it repeatedly caught what
  fixture/synthetic layers were structurally blind to. Counterfactual probe
  run once (relation-kernel: leans BET-HOLDS, one ding), waived since with a
  pre-registered reopen (runs iff the final evaluation is close). The running
  ledger lives in the package GATE_REVIEW/RETRO artifacts and this file's git
  history (pre-2026-07-12); the final weigh is Sid's.
- **D-007 — the bet foundry.** Raw claims land verbatim in `vision/LOG.md`
  first; the BETS.md Candidates inbox holds only sharpened forms (falsifiable
  statement + pre-registered kill/confirm evidence + dependencies);
  Fable-grade sessions sharpen, and decompose promoted bets into
  work-package-shaped probes (cheapest discriminating probe first). The
  questioning practice: a Fable session touching vision/bets opens or closes
  with 1–3 questions that each name their frontier source (no quota — a
  question that can't name its source isn't asked). Guardrails: one ACTIVE
  bet; promotion only by Sid at review sittings; form-break fixes outrank
  speculative probes.
- **Process rules.** Every session ends with an assertion-grade entry in its
  THREAD file and a board-line flip (adopted 2026-07-04). The board
  (`next-prompt.md`, charter 2026-07-10) is pointer + status lines only;
  detail lives in `build/<pkg>/NOW.md` (entries ≤15 lines); prune at close;
  git is the archive. Pre-registered board falsifier: a fresh session that
  mis-orients, or content creeping back onto the board, means revert or
  amend — don't suffer. Package mechanics live in
  `.claude/skills/work-package/SKILL.md` (Fable signs canon).

## Open questions queued for ruling

- **Durability fork** (Sid's — future-binding write-side architecture):
  durable cluster vs durable spine-edge replay log. The in-memory
  `:analyzer-basis` and cross-boot edge retraction slot behind the same seam
  when ruled.
- Confidence/credential algebra for the trail→code join; rename/move
  continuity (parked in the code-ingestor contract).
- Question-as-first-class-unit design (form-break evidence acquired
  2026-07-04; queued on the design track — board thread 7).
- Benchmark front resume (pushed back by D-010; Sid's call).
- D-006 final evaluation sitting (Sid weighs; the criterion-2 probe reopens
  iff it would swing the verdict).
