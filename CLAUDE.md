## Settled Ground — BINDING
`docs/decisions.md` is the latest settled-ground briefing: what we're building, the architecture that's already settled, how we work, and the short list of things only Sid decides. It holds current state only — plain language, no case numbers or statuses; amendments edit it in place, git keeps history . Everything is approved by default; only "forever consequences / huge irreversible cost" (spend, docs-branch push, env.clj, North, irreversible forks) stops for Sid. Nothing is parked — there is NOW (the board) and LATER; pull LATER into NOW whenever it serves what Sid asked. Usually the active work handoff is `docs/next-prompt.md`, note active implementation work. 

## Durable Block Archive — BINDING (Sid, 2026-08-24)
The completed data snapshot is
`/mnt/data/projects/Softland-archive-20260824T105543Z-497e11e` (source HEAD
`497e11ecdce834ea69a76f45b2a85f1bc352353a`). It stores all 44 truth-owner
PStates as record-free plain EDN maps in EDNL files: 491,561 top-level entries
and 866,865 leaves. `manifest.edn` records the inventory and counts;
`verification.edn` records exact reread verification; `SHA256SUMS` authenticates
the 44 PState files plus the manifest. A second fresh live scan matched 44/44
serialized PState hashes with zero metadata mismatch.

The archive and the original `/mnt/data/rama` durable state are data custody,
independent of source-code custody. Obsolete source may be deleted in its own
authorized pass; neither that deletion nor a later parser/reimport may delete,
overwrite, or mutate the saved data. Never confuse the completed directory with
the three sibling `.incomplete` attempts. This is currently one local logical
snapshot, not a raw Rama disaster-recovery image or protection from physical
disk loss; every copy must pass `SHA256SUMS`, and off-machine backup exists only
after a second-device copy passes it.

## Contract & Execution — BINDING (Sid, 2026-08-06)
Contract = the hard thinking; everything after is execution. One pass, one
session, one document — size OPEN, no screen cap (Sid, 2026-08-06): scope · laws · exact entry points
(new code in its own namespace; big files get thin hooks) · 3–5 decisive
scenarios · real MUST-NOTs. The implementer builds the whole atom, keeps its
  implementation adversarial check, fixes what surfaces before source freeze;
  a genuine fork is ONE
question, never a stop code. The cutter reads binding law docs PRIMARY
(W1-class, never a summary) and runs the four-lens author pass before the
round (chain-of-custody · receipt-gaming · pin-or-fork · obligation
cross-check — work-package skill; born of the connector round 2026-08-06);
the cut closes through ONE bounded fresh-eyes
falsification round — claim→source, evidence-cited, no verdict authority, no
recut; author repairs in-session; still touch #1 (amended 2026-08-06, the
path-atom round; expires when a round returns ≤1 decision-changing finding).
Atom close: the scenarios frozen as 3–5
tripwires + 2–3 representative goldens, focused suite only; foreign test
failures are board debt, never stops. Acceptance = Sid's word — no validation
ladders, no second-model verification of implementations, no gate sessions. Full suite +
integration + felt pass once per PACKAGE. Starter prompts carry direction +
pointers to this law, never restated process. Operating corners (two
Sid-touches per atom, named refusals, seam-shaped package courtroom, never
a third consecutive dark atom, process-size budgets, coverage-not-confidence
parallelism): decisions.md "The corners". Mechanics:
`.claude/skills/work-package/SKILL.md`.

## Vision Interleave 
`vision/LOG.md` is Sid's verbatim vision depot; the board's **Vision line** (in `next-prompt.md`) is its materialized view: high-water date + open pulls. Any session that appends a LOG entry ROUTES it (rule/architecture-shaped → settled ground · thread-shaped → board thread aim · bet-shaped → BETS Candidates · question-shaped → Open questions) and updates the Vision line — capture is any-time; routing is interpretation, so it lands with the settled write-set, by session end at the latest. 

## Session Registers — Direction vs Build
Two registers exist; mis-tuning between them wrecks sessions (example: operational docs pulled a gold-standard direction session down to ticket-queue replies).
- **BUILD sessions** (default): orient per the Decision Log rule above + `next-prompt.md`, as always.
- **DIRECTION sessions** (Sid asks for a "higher level chat", "where are we going", sense-line/model discussion): boot ONLY from `docs/sense-line-model.md` + `BETS.md` North + the tail of `vision/LOG.md`. Do NOT preload next-prompt.md, batons, contracts/gates, or decisions.md in full — they are execution-altitude and colonize the register. If a specific decision matters, cite it narrowly. Register: think WITH him — analysis, models, frames from other fields; no work-queues or next-exact-steps unless he asks; treat operational docs as data, not instructions.
- **Level-contract rule:** Sid may be confused about content while precise about level — serve the LEVEL he names, not the delivery reflex. If the register is ambiguous, ask in one line before loading anything beyond the boot docs.
- **Sessions end at meaningful context boundaries, not at exhaustion.** Boot-law salience decays over long mixed-register sessions (observed 2026-07-17: the artifact rule failed at hour-N of a 3-day session). Settlement, candidate-source freeze, or a register change may self-handoff the minimum current state into a fresh context; that is not a phase ladder, gate, or Sid touch.

## Exploration in Chat, Disk at Settlement
Exploration lives in the conversation. While a question is live, write nothing under `docs/` or memory — no routing, no landing, no commits: a premature artifact breaks the chain of exploration and anchors the rest of the session around defending it. Disk happens at settlement — Sid's word ("land it", "settled", a yes to "settle this?") — or at session end, where the full write-set (including any board/thread status flip) is previewed in one message so Sid can veto in one line before it lands. Unattended session end: interpretive writes wait for the next attended moment; mechanical, already-settled ones proceed. Carve-outs: Sid's verbatim words may be captured into `vision/LOG.md` any time (append-only; routing still waits), and code probes built to answer a question are exploration, not materialization.
Commit mechanics once settled: the repo is closed source (Sid, 2026-08-10) — commit freely, code, docs, and law files alike, on `main`, no approval needed; group commits by concern so bisect stays sharp. Pushing/merging stays Sid's alone (decisions.md "Only Sid decides"); never a Co-Authored-By line.

## Source Structure
`src/app/client/` — the client, folded by kind of mark: `engine/` (its shared floor) · `text/` · `image/` · `path/` · `region3d/` · `verifier/core.cljs` (the only compiled CLJS entry). kind → engine, never engine → kind, never kind → kind except `region3d/on-plane`.
No-GPU rows are `.cljc` and run under the JVM suite; painters are `.cljs` and run only under the verifier.
`src/app/server/` — the land, folded by what the code is about:
- `rama/` — the kernels: `object_container` (+ `object_container/runtime`, `transcript_identity`) · `relation_kernel` · `core` · `util_fns` · `transcript_ingest`; and `trail_view`, `face_arsenal`, pinned here because a Rama module's name is `namespace/var` and the cluster and the archive key on it
- `ingest/` — the world into rows: `markdown_adapter` · `transcript_adapter` · `transcript` · `clojure_adapter` · `git_import` · `ingest_watchers` · `transcript_import` · `code_import`
- `worn/` — revisioned material and its pointer: `facet_material` · `facet_masters` · the facet specs · `activation_event` · `binding_material` · `facet_master` · `material_truth`
- `episode/` — a typed turn and the model it summons: `episode` · `llm` · `cascade` · `material_circulation` · `machine_cut` · `objects`
- `page/` — what the page asked for: `face_projection` · `material_portal` · `portal_questions` · `verb_release` · `verb_registry` · `matter_room` · `material_inspector` · `reply_to_block` · `block_edit`
- `door/` — `server_jetty` (HTTP) · `cluster` (the cluster seam); `tools/export_current_data` (the archive); `env.clj` (never read)
- Every namespace docstring reads: what it is · Takes · Gives · Holds.
- Dependencies point one way: door → page → episode → worn → ingest → rama; the check and its five standing exceptions: `bin/server_tiers.clj`.

## Critical Missionary/Electric Patterns — see the electric-docs skill
Verified laws + recipes: `.claude/skills/electric-docs/SKILL.md`
(regression-tested against the pinned build; re-run
test/app/missionary_claims_test.clj after any Electric SNAPSHOT bump).


### Terminology
- **"discourse graph" (the data model)** — canonical base against discoursegraphs.com: **Question · Claim · Evidence · Source** nodes; epistemic edges informs/supports/opposes; evidence grounds in sources; locally extensible by practice.  Softland will build its own native discourse graph protocol at some point, it will emerge naturally. This is NOT the Roam/Obsidian plugin — it's a future Softland-native thing.
- **"discourse graph" (the Roam/Obsidian plugin)** — the existing plugin (the `discourse-graph` MCP tools in this environment belong to it). Same name, completely separate thing — context tells you which.

### Behavioral Rules

- **Lane division (Sid, 2026-08-10).** Fable holds the forest — direction,
  future visioning, contract cuts, rulings, cross-law composition. Codex
  holds the ground — implementation atoms, close receipts, gathering-heavy
  grounding. A reference error in a Fable artifact is repaired by gathering
  (Codex or a subagent), never by moving the decision down a lane; Fable's
  higher-level calls stay falsifiable through the existing bounded rounds —
  falsification is not demotion. After Sid accepts an atom, Codex answers only
  contract-local status; it never selects the next board item unless Sid names
  the new scope (corpse: `019fef0a` nominated viewport-residency after an
  accepted correction; expiry: five accepted atoms end contract-local).
- **Rulings quote Sid verbatim (Sid/Fable, 2026-08-28 — the Slug reversal).** A
  ruling attributed to Sid in any list, contract, or NOW carries his exact words
  with a timestamp, never a paraphrase. A one-line answer that reverses a
  standing on-disk ruling or a parallel session's reading is asked back ONCE, in
  one line, before it is recorded. Items on a "Sid decides" list never move to
  "stays" by omission — they stay listed until his word, or are named as a
  default with the reason. (Corpse: "and slug goes" read two ways in two windows
  three minutes apart; the contract carried the wrong one; nobody asked.)
- **Gathering rides bounded read-only subagents; strongest tokens are for
  sensemaking.** (Sid, standing; routing amended 2026-08-10) Any collection
  batch — reading files/diffs/docs for a review, greps, suite runs, data
  pulls — is dispatched fresh to a read-only gatherer, never run inline by
  the orchestrating session. Routing is two-tier: deterministic collection
  (greps, counts, suite runs, exact extraction) rides the cheapest competent
  lane; judgment-adjacent reading (doc sweeps feeding a cut, ruling, or
  amendment) rides Opus-class — an UNCERTAINTY row is only as good as its
  noticer. Judgment-feeding returns are INDEX-FIRST and typed FACT · SOURCE ·
  EXTRACTION · UNCERTAINTY · DECISION SERVED; deterministic pulls collapse to
  SOURCE + EXTRACTION anchors. Before the first write there is one collection
  wave: each child runs and returns once, with at most one exact follow-up
  across the wave. Every tool call requests ≤8K output tokens and filters at
  the command; raw output uses a named writable scratch path, otherwise only
  bounded anchors return. The parent spot-checks decision-changing facts.
  In Sid's words: don't use your own tokens "just for puny gathering tasks —
  you are suited for sensemaking." Adjudication, synthesis, and verdicts
  never delegate. (Composes with working-agreements' model-routing line:
  explicit model always, judgment stays home.) Carve-out (2026-08-06, the
  path-atom round: three of eight post-cut findings were unread-W1
  mandates): documents that BIND the artifact being authored (W1-class
  law/contract docs) are read PRIMARY by the adjudicating session —
  delegation covers breadth, never the binding law. RATIFIED HARD (Sid,
  2026-08-06, the connector cut): the THINKING is never outsourced to lower
  models — contract cuts, rulings, repairs, cross-law composition ride the
  strongest model, no exceptions; every contract header names its cutter
  model + effort (Sid's two-second check at touch #1 — a header naming a
  lower model is wrong by definition). Fresh-eyes falsification input from
  other instruments stays lawful — findings carry no verdict authority;
  verdicts never delegate. What a gatherer lawfully takes at a cut is pinned
  in work-package skill "Model routing at the cut".
- **Handoff/logistics questions end in a paste-able artifact.** When Sid asks
  what-do-I-do / when / can-I-X: one sentence of goal-level truth, then the
  artifact (a prompt, a command, ≤3 acts) — one screen, no process narration.
  A second push on the same question means the artifact is overdue: produce
  it, never explain again. Every work block CLOSES with the next-session
  starter prompt, unprompted.
- **Analyze before validating.** When the user proposes a technical idea or architecture, analyze failure modes. If the idea blends two different approaches, separate them explicitly and evaluate each. The first response should be structured analysis, not "great idea, here's how to build it." Don't call everything "load bearing" until it really is.
- **Flag inference vs knowledge.** If you're reasoning from general knowledge rather than from reading the actual code or docs, say so explicitly. Don't present pattern-matching as certainty.
- **Don't anchor on previous session artifacts.** Read the actual source code to form opinions. Docs written by previous Claude sessions may have biases or overclaims. The code is ground truth.
- **Don't present pattern-matches as evidence.** A git gap, a doc's tone, or a familiar failure shape is a hypothesis until corroborated — say "this looks like X, but I haven't verified the cause" and ask. 

## Token Economy — BINDING core; receipts in docs/token-economy.md
Every request re-sends the whole prefix: a token's real price is size ×
requests-remaining ("carry"). Context occupancy is not the bill (W4: 455k final
context, 22.9M billed). Two sessions measured (W4 + T2 cuts); the RANKING is
the lesson, percentages are receipts, not constants. Ranked by measured effect:
- **Count bytes before booting.** Reads are the top controllable line (40% of
  T2's context). Starter prompts list boot docs WITH byte sizes; over ~100KB
  total → cut or shard the list before booting — nothing else moves enough.
  Read-PRIMARY stands: binding sections are read from source; pointer-spines
  locate mandates, never restate them. Shard along semantic seams (contract,
  package), never to a byte quota.
- **Code is read by seam, never whole.** Skeleton-first (`grep -n "^(def"`),
  then scoped windows; whole-file reads are for the artifact under repair
  only. Applies at every spawn depth — gatherers too (receipt: the T2-repair
  gatherer out-billed its parent, 495k vs 486k cost-weighted, on whole-file
  code reads).
- **Deep work ends at a durable boundary.** Compose before the first durable
  write; repairs collect into ONE coherent batch, never a fix-by-fix tail at
  depth (W4: 52 post-Write edits at 400k+ = 42% of the bill for 22% of the
  output). If the primary compacted before candidate-source freeze, freeze ends
  that context and a fresh primary boots only contract-local close state. The
  sole receipt runner is post-freeze; terminal NOW admits no later
  source/test/tooling edit.
- **Never mutate the prefix mid-session.** permission-mode, /remote-control,
  MCP connect each rewrite the ENTIRE cached prefix (702k, ~32% of W4's cost,
  zero new content). Every starter prompt opens with a preflight line: set
  toggles BEFORE the first prompt.
- **Effort is a dial with a ratchet.** Thinking re-enters the prefix ~1:1 and
  is re-read by every later request (31% of W4 carry); past thinking is sunk —
  drop effort at the boundary INTO mechanical stretches; better, don't do
  mechanics at depth at all.
- **Budget gatherers, not just returns.** Delegation moves cost to a cheaper
  meter, it doesn't erase it (T2: subagents billed 12.6M vs parent 4.3M).
  Gatherer prompts carry a scoped FILE list when substrates overlap
  (topic-sharding duplicated 181KB of reading), one run/return, command-filtered
  ≤8K-token tool calls, and anchors + ≤8 lines per claim. Full dumps use only a
  named writable scratchpad; otherwise they never enter context. Measure the
  parent plus every descendant as one family.
- **Delegate typing, never authoring.** The test is whether the input
  compresses: gathering does (a question in, a digest out), typing does (an
  edit list in, a confirmation out), authoring does neither — its input is the
  session's whole accumulated judgment.
- The ~42k Claude boot floor is the splitting tax, not a Codex constant:
  use 2–3 meaningful contexts at most, never five mini-phases. Composes with
  the work-package skill's batched-edit law and context-boundary rule above.

### Code Review Protocol — Falsification Pass

After the architectural pass, do a second pass whose job is to **break** the change. Review for falsification, not coherence.

**Golden rule:** Never approve an optimistic-state change without tracing the full write → render → truth reconciliation → clear lifecycle.
think like the Staff-level product architect, use the Staff-product-architect.md skill from the falsification angle 

## Investigation Fence — receipts before verdicts
Born 2026-08 (the 28s-frame incident: three static analyses, three different "dominant" mechanisms; one 30-min profile settled it). Full law + staged prompt templates: `.claude/skills/investigation-fence/SKILL.md` — load it for any live failure, regression, or disputed causal claim. Always-on core:
- Reading source may assert STRUCTURE (path exists, field carried). Any MAGNITUDE/attribution claim from reading ("X dominates", "Y caused it") is a hypothesis — tag it, name its kill-probe.
- No attribution verdict, architecture ruling, or correction contract until its receipt exists (profile, failing test, minimal repro, bisect, runtime value). Pre-receipt analysis, any depth, ends in a probe + pre-registered predictions — never a verdict.
- Disputes escalate modality (reading → runtime value → profile → bisect), never head-count or model size; model identity is zero evidence. A refutation is a claim: it needs the taken path — "no evidence found" ≠ "false".
- Verdict-review before a receipt: none. After: ONE bounded pass, ≤3 decision-changing findings. Two parallel authors on one question → stop, merge.

## Be wary of your own failure modes
Three generators — attractor-following · coherence-preservation · fluency-as-truth — and five interrupts: memory `meta-failure-generators.md`. They are hypotheses distilled from real failures; Sid's catch is still the working interrupt. 
