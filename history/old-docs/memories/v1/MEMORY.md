# Softland — Project Memory

## What Softland IS
Softland = Soft + Land. A land made of software. A world you inhabit, not a tool you use.

**Software that is a place, not a tool.** Every elaboration — epistemic interface, projection workshop, coupling device — is a footnote to what the name already says. A place has terrain (structure), weather (dynamics), history (trails), neighborhoods (local worlds), visitors (public form), renovation (self-modifying code), and many versions (infinite Softlands in Rama).

> Full perspective docs: `vision/what-softland-is-claude.md` (word: "place"), `vision/what-softland-is-codex.md` (word: "relation")

**Two centers** (different altitudes, both true):
- Design center: *"A world for holding understanding in public form"*
- Engineering center: *"A programmable epistemic interface"*

**Habitability criteria** — what makes the place good to inhabit:
- Terrain must be navigable (you know where you are)
- Weather must be survivable (pacing — not overwhelmed)
- History must be present (trails, provenance)
- The map must not lie (calibration — fact vs hypothesis vs guess)
- Others must be able to visit (public form, transferable local worlds)
- Renovation must be possible (the place grows with its inhabitants)

**Continuous semantic zoom** — one world at every scale:
- **Zoom 0.001**: Knowledge landscape — all of human understanding, explorable like Google Earth
- **Zoom 0.01**: Domain — autoimmune diseases, category theory, a field
- **Zoom 0.1**: Topic — a paper, a discourse thread, a claim and its evidence
- **Zoom 1.0**: Ground level — interactive models, data, the REPL for knowledge
- **Zoom 10.0**: Below ground — the software that renders everything above
- **Zoom 100.0**: The code itself — visible, modifiable, rebuildable

The code editor IS Softland at zoom 100. Not a dev tool. The bedrock layer of the world.

## The Theory of Change
Current bet: if we make ONE agent's reasoning trail genuinely useful for code review (the narrowest case), that validates the core primitive (structured exploration logs > static prose). Then generalize: research papers as trails, scientific exploration as trails, knowledge as explorable structured logs.

- **Discourse graph** (`Q->C->E->D->R->F`) = the data model for all knowledge
- **Category theory** = the structural language that makes tools domain-agnostic (functors map between domains)
- **Rama event sourcing** = append-only trace substrate; semantic time is braided/DAG causality derived from it
- **WebGPU** = chosen for continuous semantic zoom (not performance) — one rendering surface from knowledge landscape to source code
- **Multi-LLM** = "conflicts are good for progress" — disagreement IS the frontier, synthesis FROM disagreement is the knowledge product

## The Collaboration Model
Not "here are cursors of all the people." Collaboration across zoom levels:
- People at zoom 0.1 (knowledge layer) can raise requests: "wish I could do X"
- Requests flow DOWN to zoom 100 (software layer) where someone (human or AI) implements them
- New malleable software version flows UP — use it, share it, merge to canonical
- The system modifies itself in response to the needs of its inhabitants

## Who the User Is
- Thinks in **feelings first, structure second** — evaluates by how it feels to use, not whether it meets a spec
- Values **rediscovery over consumption** — "I want to do the work, not know it"
- Sees **category theory as worldview**, not just a technical choice — structure and coherence across domains
- Believes the **medium is the message** — "have you used it in your dreams?"
- Works with **multiple LLMs in parallel** — each perspective preserved separately, Claude's is canonical
- Thinks in **sub-projects and flows**, not tasks — wants the manager view, not the task list

## Key Reframes (from vision docs)
- "AI interaction should be MATERIAL, not output" — a first-class object with provenance, connections, forks (moonshot doc)
- "The editor IS the knowledge graph, you just haven't zoomed out" — camera dolly, not mode switch (moonshot doc)
- "The command panel is a chat-paradigm fossil" — the entire document is the interaction surface (moonshot doc)
- "SCI is the REPL for knowledge, not just code" — query understanding, not just evaluate expressions (moonshot doc)
- "Papers should be logs" — replay how understanding was built, not read a static result (README)

## Core Reframes (from 3-way synthesis: Claude + Codex + user, 2026-03-07)
> Full document: `memory/core-reframes.md` — permanent orientation, not session notes
> Hidden center: **"Softland is a world for holding understanding in public form."**
> Refined: `docs/vision/epistemic-framework.md` — 5-round Claude+Codex+Gemini refinement (2026-03-08)
> Key refinement: two centers at different altitudes (design center vs engineering center), three-level separation (coordinates/pressures/loop), calibration+pacing as first-class constraints

- **Public form means inhabitable by another mind.** Not maximal display: make important structure explicit, keep the rest compressed but recoverable, and let understanding remain plural and composable without losing truth.
- **Public form must stay compressed, plural, and composable.** Softland should hold understanding in a form that survives compression, preserves disagreement, and can be handed between minds and workflows without losing truth.
- **Softland is a world substrate, not an application.** Ontologically one world; architecturally still distinct substrates. The primary nouns are local world, split, trail, artifact, workflow, transformation — not WebGPU, Electric, Rama.
- **A split is preserved tension**, not layout. It holds coupled things in co-presence so understanding doesn't require mental reconstruction of severed relations.
- **The local world is the atomic unit** — the smallest identity-bearing packet of understanding that can be inhabited, revisited, handed off, zoomed, and transformed. A region of space × time × modality × plurality.
- **Workflows are traversals between local worlds.** Maturity climbs produced → preserved → navigable → hand-offable → composable → synthesizable local worlds.
- **Commands are larval workflows** — verb first, place later. Intent appears as command, matures into ritual, crystallizes into inhabitable spatial structure.
- **Material → process → artifact** is the recurrent triad that explains why 3-pane keeps appearing.
- **JIT is a self-translation channel** that expands the world's expressive vocabulary for all workflows.
- **Three grammars must converge**: visual (space), epistemic (time), transformational (modality). Plurality is the fourth axis and a pressure on the other three — preserved disagreement until synthesis becomes possible.
- **Semantic zoom is lawful compression** — identity, provenance, and relations must survive across all three axes or the zoom is fake.
- **The event log is primary**, rendered state is projection. Attention itself becomes content — acts of arrangement, focus, and relation-making are meaningful events.
- **One ontology, many projections** — discourse relation, spatial split, temporal event, filesystem structure are different views of the same thing.
- **The refactor is self-modification** — the filesystem IS the knowledge graph at zoom 100. Wrong ontology bends all future growth.
- **Preserved disagreement + synthesis** is a native artifact type, not just a process observation.
- **Universal design criterion**: minimize private reconstruction. Make important structure public, keep the rest compressed but recoverable. Guardrail: minimal hidden coupling, not maximal display.
- **Developmental law**: verb first, place later. Command → output → local world → inhabitable → composable. DG=stage 3, JIT=stage 2, file/chat=stage 3.
- **Feature compass**: does this reduce private reconstruction by making some dimension of understanding public in a form another mind can inhabit?
- **Recoverable compression**: hide by folding, not severing. Compress by preserving recoverability, not erasing structure.
- **Late-bound consensus**: preserve disagreement structurally until synthesis is meaningful. Synthesis is transformation of preserved plurality, not summary.
- **Composable handoff**: a local world must expose material, process, artifact, provenance, tensions, and transformations — enough for another mind or workflow to enter without rebuilding context.
- **Dynamics layer** (Gemini push): the ontology describes coordinates; dynamics describe how understanding moves. Tension is potential energy, synthesis is phase transition, cooling matters as much as heat.
- **Semantic time is a DAG**, not a line. Linear logs create false causality when multiple agents work in parallel. The log is substrate; the causality graph is temporal ontology.
- **The artifact pole is executable** — the preview pane is a REPL for the current local world, not a display surface. Connects to SCI as "REPL for knowledge."
- **Artifact slot is general; artifact type is workflow-specific.** The preview pane is a consequence slot filled differently by DG, JIT, file/chat, and future knowledge workflows.

## Current Strategic Position (as of Session 40, 2026-03-22)
The project is building from zoom 100 upward:
1. **Code editor** (zoom 100) — ~90% done. WebGPU MSDF, editing, SCI, themes, sidebar
2. **Agent runtime** (zoom 50) — working. SSE streaming, --resume, multi-provider
3. **Reasoning trails** (zoom 10) — ~85% done. Interactive trail UI, click-to-navigate, markdown rendering
4. **Workflow screens** (zoom 5) — 25%. Screen 1 (intake) of 5 built, gate pending. Intake scroll still broken.
5. **Component library** (zoom 20) — DEFERRED. Architecture approved, no UI entry point. Reconnects via UI Excellence Phase 3
6. **Knowledge layer** (zoom 0.1-1.0) — not started. This is where discourse graphs, papers-as-logs, explorable models live
7. **Runtime architecture** (zoom 100 infra) — S37: runtime split. S38: differential architecture. S39: buffer pool + arch reconciliation. S40: sidebar as first Rama-backed slice. **S47: Slug font backend** — switched from MSDF (sampled distance field) to Slug (direct Bézier curve evaluation). Resolution-independent text at any zoom. Only DejaVu Sans Mono has slug data; other fonts need curve extraction tooling.

### S38 Architecture Breakthrough (2026-03-15)
Deep 3-way conversation (Claude + Codex + User) starting from "should interactions be pull-based?" arrived at:
- **Differential principle**: every layer operates on changes, never full state. Current code uses snapshot-rebuild + identical? check. Target: Electric e/for-by keyed diffs end-to-end.
- **Three things, not five layers**: Rama (ground truth), Electric (connective tissue), WebGPU (terminal). Electric spans client+server, removes API boundary.
- **Commitment boundary**: the deepest missing piece. Currently handlers mutate atoms directly. Need: signal → proposal → commitment pipeline. Defines undo, history, public form.
- **Three gaps that unlock everything**: (1) commitment boundary, (2) atoms → PStates, (3) snapshot → differential.
- **Electric is more than UI**: generic mount (5 callbacks, not DOM-specific), keyed diffs (e/diff-by), server→client transfer. Old code (`electric_flow_old.cljc`) proves Electric+WebGPU was attempted but differential rendering wasn't fully achieved.
- **Converged definition**: "A recursive spiral of local worlds, opened by pressure, where the primary material is understanding."
- Main refactor map: `docs/architecture/migration-concrete-s38.md`
- Destination sketch: `docs/architecture/differential-pipeline-s38.md`

## Navigation — Where to Look
1. **Status dashboard**: `docs/thread-map-claude.md` (CANONICAL — never read Codex/Gemini thread maps unless asked)
2. **Master index**: `docs/_map.md` — causality tree, points to everything
3. **Orientation**: `docs/plans/where-we-are.md` — what exists, decided, next
4. **Checklist**: `docs/plans/ui-ux-tracker.md` — implementation tiers
5. **History**: `docs/history/progressive-summary.md` — session milestones
6. **Implementation workarounds**: `memory/implementation-quirks.md` — coding gotchas learned through pain

### Per-Thread Detail Files
| Thread | Read These |
|--------|-----------|
| Vision / Soul | `exploration/claude-3-moonshot.md`, `vision/design-brief-reasoning-trails.md`, `vision/epistemic-framework.md`, `vision/what-softland-is-claude.md`, `vision/what-softland-is-codex.md`, `vision/terminology-glossary.md` |
| Workflow contract | `plans/commission-consensus.md` (THE locked contract) |
| Screen specs | `plans/screen-1-spec.md`, `plans/ui-mockups-master-detail.md` |
| Component library | `architecture/component-library-jit.md`, `plans/ui-excellence-spec.md` Phase 3 |
| GPU / Layout | `architecture/gpu-component-library.md`, `architecture/virtual-layout-engine.md` |
| Rect tree | `architecture/rect-tree-ui.md` |
| Prompt-driven UI | `vision/prompt-driven-ui-gemini.md`, `vision/auto-prompt-workflow-claude.md` |
| Differential migration | `architecture/migration-concrete-s38.md` (refactor map), `architecture/differential-pipeline-s38.md` (destination sketch) |
| Master architecture | `architecture/softland-master-architecture.md` (Codex, canonical law/ontology), `architecture/claude-architecture-s39.md` (Claude, engineering appendix — DRAFT v1) |

## Hard Rules
- **NEVER read `src/app/server/env.clj`** — contains API keys. Reference as symbols only.
- **NEVER commit .md files** — docs are private ("secret sauce")
- **NEVER commit .gitignore changes** that reference internal docs
- Only commit code files (`.clj`, `.cljc`, `.cljs`, `.json`)
- CLAUDE.md has the coding anti-patterns (m/ap crashes, Electric limitations) — don't duplicate here

## Decision Forks That Explain the Current Shape
- **Extraction-first -> JIT pivot** (S30): browser extraction was accurate but slow. Claude reading source code is faster. Component library pivoted to pre-populated inventory with JIT conversion on click.
- **Full sidebar -> file-explorer-only** (S28): tabs (Files/Review/UI) removed. 3-pane layout handles what sidebar modes used to do.
- **Auto-bootstrap -> manual /bootstrap** (S28): flow starts at `:idle`, ticket list only after explicit `/dg` command.
- **"Build more infra" vs "test the thesis"** (S31): 3-LLM consensus debate. Claude's position won — test reasoning trails before building more. Sessions 32-34 validated it.
- **"Start with Rama" vs "add it later"** (moonshot): the vision says start with event sourcing. Reality: HTTP path was built first, Rama was retrofitted (S18). The plumbing works but half the PStates are unused scaffolding.
- **"Flatten-first" → "Rama-first by slice"** (S39): S38 said flatten sidebar then add Rama later. S39 corrected: define Rama truth for one slice first, then derive the flat scene from that truth. Prevents building on atoms then rewriting. Sidebar is the first slice.
- **Rama subscription → server atom workaround** (S40): `foreign-proxy-async` with path `[]` on global PStates causes RocksDBWrapper serialization crash in Rama 1.6.0. Workaround: server-side atom updated by `emit-sidebar-event!`, bridged to Electric via `e/watch`. Per-key subscriptions untested but may fix. See `memory/implementation-quirks.md`.
- **MSDF → Slug font backend** (S47): User evaluated side-by-side and preferred slug's raw mathematical rendering (matches terminal crispness, no tuning needed). Slug is now default. MSDF code preserved but inactive. Patent was disclaimed March 17, 2026. Only DejaVu Sans Mono has slug data — other fonts need curve extraction tooling.

## Where to Write (for Claude — what goes where)
| Type of information | Write it HERE | NOT here |
|---|---|---|
| Project soul, user model, sub-project status, decision forks | `memory/MEMORY.md` | Don't put in docs/ |
| Terminology the user uses (synonyms, clarifications) | `CLAUDE.md` → Terminology section | Don't guess meanings — only add with user approval |
| Coding gotchas, tool workarounds, anti-patterns learned in-session | `memory/implementation-quirks.md` | Not MEMORY.md, not CLAUDE.md |
| Anti-patterns that prevent crashes (m/ap, Electric) | `CLAUDE.md` (root) — already there | Don't duplicate in memory |
| Session resume context (what was done, what's next) | `docs/sessions/next-prompt.md` | Not MEMORY.md |
| Major milestone completed | `docs/history/progressive-summary.md` | Not next-prompt |
| Thread status changed (done, blocked, deferred) | `docs/thread-map-claude.md` | Not where-we-are.md |
| New architecture decision or spec | New file in `docs/architecture/` or `docs/plans/` | Not inline in MEMORY.md |
| Vision / product thinking | `docs/vision/` | Not MEMORY.md |

**Key rule**: MEMORY.md is the **index and soul**, not a dumping ground. If you're about to write more than 2 lines of detail into MEMORY.md, it probably belongs in a separate file that MEMORY.md points to.

## Session Protocol (for Claude, not the user)
When a new session starts, Claude should:
1. **Check `docs/sessions/next-prompt.md`** — if it exists, read it silently for context
2. **Listen to what the user says** — infer the session type from their opening message:
   - If they mention building/implementing something → orient quickly (read 1-2 relevant docs), then start coding within 10 minutes
   - If they're asking questions or exploring → help them think, read docs they point to, no code pressure
   - If they say "continue" or "pick up" → read next-prompt.md and resume exactly where it left off
   - If they're debugging → go straight to the bug, minimal orientation
3. **Don't dump orientation** — don't read 5 docs and summarize unless asked. Orient yourself silently, act on what you know
4. **Don't drift into meta-work** during build sessions — if docs are stale, note it but keep building. Fix docs at the end
5. **When the user says "save state"** (or context is getting low and they agree):
   - Write `docs/sessions/next-prompt.md` with resume context
   - Update `progressive-summary.md` if a major milestone shipped
   - Save new implementation quirks to `memory/implementation-quirks.md` if any
   - Update `thread-map-claude.md` only if a thread's status meaningfully changed

## Feedback
- [Dev workflow](feedback_dev_workflow.md) — never run standalone shadow-cljs compile; use `clj -A:dev -X dev/-main` for dev server with hot-reload
- [No Co-Authored-By](feedback_no_coauthored_by.md) — never add co-authored-by lines to commits
- [Precision over validation](feedback_precision_over_validation.md) — analyze failure modes BEFORE validating ideas. Don't be agreeable first — be precise first. Flag "I know" vs "I'm inferring."
- [Own voice under peer review](feedback_own_voice.md) — when Codex (or another LLM) reviews Claude's work, accept factual corrections but don't mirror the reviewer's voice or systematically dampen bold claims. Defend what you believe. Be yourself.
- [Falsification review protocol](feedback_falsification_review.md) — when reviewing code, do an adversarial falsification pass after the architectural pass. Trace lifecycles, consumers, error paths, ordering, shape. Don't say DONE without naming a failure mode. Protocol is in CLAUDE.md.
- [Measure before assuming](feedback_measure_before_assuming.md) — don't lock in engineering workarounds (batching, hybrid models) before testing the direct path. "I think this will be slow" ≠ "I measured this and it's too slow."
- [Read primary sources](feedback_read_primary_sources.md) — fetch actual linked articles before proposing work, don't rely on local summary docs from prior sessions
- [Fix don't defer](feedback_fix_dont_defer.md) — when you identify a small cleanup and you're in the files, just do it. "Not a blocker" is not an excuse to leave dead code.
- [Rama topology side effects](feedback_rama_side_effects.md) — `completable-future>`/`each-async` tie future delivery to event success but do NOT undo OS-level side effects on retry. Use AOR back-arrow: topology writes intent, executor reactively consumes.
- [Rama partitioner = event boundary](feedback_rama_event_boundaries.md) — atomicity is per-event not per-topology. Multi-key writes across `(|hash …)` are NOT visible together. Key by ONE value for atomic visibility.

## Decided Questions
- [Editor Rama latency](editor_rama_measurement.md) — 7.5ms avg round-trip. Direct committed path is the architecture. No hybrid/batching needed.
- Commitment boundary design — answered by Phase 4B measurement: auto-commit for all editing events, fire-and-forget to Rama, local-first for responsiveness.
- [Renderer law](renderer_law_synthesis.md) — 5-clause law from 3-way synthesis: semantic truth separate from render caches, cache expensive structure, update only changed, sample from stable identities, identity survives across strata.

## Open Questions
- When does the component library reconnect? (Phase 3 spec exists, not prioritized)
- Screen 1 acceptance gate — keyboard nav, command regression testing still pending
- Preview pane content — currently 5% placeholder, waiting for component demos or knowledge artifacts
- Server-side Rama spatial queries vs client-side viewport filtering for 1000+ node zoom — open design question
- MSDF quality envelope — does MSDF survive the zoom range continuous semantic zoom needs? 30-min test determines whether Slug is needed.
