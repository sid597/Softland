## Settled Ground — BINDING
`docs/decisions.md` is the latest settled-ground briefing: what we're building, the architecture that's already settled, how we work, and the short list of things only Sid decides. It holds current state only — plain language, no case numbers or statuses; amendments edit it in place, git keeps history . Everything is approved by default; only "forever consequences / huge irreversible cost" (spend, docs-branch push, env.clj, North, irreversible forks) stops for Sid. Nothing is parked — there is NOW (the board) and LATER; pull LATER into NOW whenever it serves what Sid asked. Usually the active work handoff is `docs/next-prompt.md`, note active implementation work. 

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
Commit mechanics once settled: docs-only commits on the local docs branch, no approval needed, done by the session itself. NEVER push or merge this branch; never mix code and docs in one commit.

## Source Structure (post-refactor)
Key source files live under `src/app/`:
- `electric_flow.cljc` — Electric reactive UI (layout, text rendering, DOM)
- `client/substrate/webgpu/renderer.cljs` — WebGPU render loop, GPU pipeline
- `client/workspace/` — workspace UI: runtime, editor, sidebar, shell, events, themes, etc.
- `client/workflows/` — domain workflows (dg_flow, jit)
- `shared/` — `.cljc` consumed by BOTH server and client (P1 gate ruling 2026-07-24; `face_assembly.cljc` is grandfathered under `client/workspace/` and migrates at a natural touchpoint)


## Critical Missionary/Electric Patterns — see the electric-docs skill
Verified laws + recipes: `.claude/skills/electric-docs/SKILL.md`
(regression-tested against the pinned build; re-run
test/app/missionary_claims_test.clj after any Electric SNAPSHOT bump).


### Terminology
- **"discourse graph" (the data model)** — canonical base against discoursegraphs.com: **Question · Claim · Evidence · Source** nodes; epistemic edges informs/supports/opposes; evidence grounds in sources; locally extensible by practice.  Softland will build its own native discourse graph protocol at some point, it will emerge naturally. This is NOT the Roam/Obsidian plugin — it's a future Softland-native thing.
- **"discourse graph" (the Roam/Obsidian plugin)** — the existing plugin (the `discourse-graph` MCP tools in this environment belong to it). Same name, completely separate thing — context tells you which.

### Behavioral Rules

- **Gathering rides bounded native subagents; strongest tokens are for
  sensemaking.** (Sid, standing) Any collection batch — reading
  files/diffs/docs for a review, greps, suite runs, data pulls — is dispatched
  fresh to the lowest competent read-only collector/explorer and never run
  inline by the orchestrating session. The return is INDEX-FIRST and typed
  FACT · SOURCE · EXTRACTION · UNCERTAINTY · DECISION SERVED; raw output stays
  in scratch and the parent spot-checks only decision-changing facts.
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
  output). Candidate-source freeze may self-handoff close mechanics into one
  fresh context; the terminal NOW write admits no later source/test/tooling edit.
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
  (topic-sharding duplicated 181KB of reading), bounded verbatim returns
  (anchors + ≤8 lines per claim), full dump to scratchpad.
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
