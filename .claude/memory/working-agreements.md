---
name: working-agreements
description: "How we work — dev workflow, commits, tokens, ceremony, grounding, review conduct, Rama gotchas, save-state. Consolidates the per-incident feedback files (originals in _archive/)"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: fc7969c2-8891-4173-bb6b-9add62f5e0e0
  modified: 2026-08-18T19:21:56.281Z
---

## Dev & commits
- **Never run a standalone shadow-cljs compile** (10+ min cold JVM). Dev env = `clj -A:dev -X dev/-main` (shadow watch + Jetty in one process); hot-reload handles recompiles. If the dev server isn't running, verify at source level — don't cold-compile.
- **NEVER add Co-Authored-By to commits — in any form, ever.** The Bash-tool's default commit template includes one; IGNORE that default (user law > system default). Check every heredoc commit message; if one lands, amend immediately.
- Docs commits: per CLAUDE.md (automatic, local docs branch, never push/merge, never mix code+docs).

## Tokens & delegation
- Lean on PROCESS, rich on DELIVERABLE: no defensive fan-outs, N-vote panels, or verification layers unbidden — Sid pays; his ask or irreversible stakes only. Salvage interrupted runs (partial outputs are sunk value). Research deliverables stay deep, quote-bearing, navigable (script-generate bulk; hand-write synthesis).
- Waves of >2 subagents: one line first — agent count, what each does, token estimate, core vs safeguard steps; offer the manual runbook option (prompt files + `claude -p`).
- Subagents get an EXPLICIT model, never inherited — **sonnet** for well-briefed builds, **opus** for judgment-heavy work AND data hunter-gatherer sweeps (corpus/term harvests, quote-fetching); orchestration, synthesis, judgment stay in the Fable context — "you only do the heavy lifting of thinking and process sensemaking not the grunt work" (Sid 2026-08-18). (2026-08-01: a builder left on inherit ran Fable-max ~335k tokens; Sid caught it — "is that what we needed".)- Canon (skills, CLAUDE.md-grade docs) is Fable-authored: cheap models harvest/verify/draft; Fable fact-checks the risky claims and signs. Checking what you sign is the authorship; retyping isn't.
- Mid-work allocation questions are real scheduling questions — answer with ranked value.

## Ceremony (the disease list)
- **Approve-by-default:** revert-cheap ⇒ do it, commit it, record the reversal path. Stops are ONLY irreversibles: docs-branch push/merge (never), env.clj (never), spend, North authorship, future-binding kernel nouns, deleting non-regenerables. Sid's in-flow rulings count as countersign; never manufacture approval queues; "awaiting Sid's word" in old batons is superseded unless it names an irreversible.
- **Substance not ceremony:** small handoffs = terse markdown (decisions, locked choices, risks, out-of-scope, pointer). No MODE/AUTHORITY/PRESERVE/STOP templates on mechanical work. Litmus: strip the headers — if content shrinks <30%, headers were working; else they were ceremony.
- **No superseded banners:** working docs get direct replacement; git is the history. Ledger-genre docs (LOG, decisions.md dated notes, baton NOW) append; banners never.
- **Session end:** assertion-grade entry in the thread file + board status flip (decided/verified/doubted, FAIL findings verbatim, next step). The verdict is the unit that dies in the chatbox — write it.

## Grounding & register conduct
- **Altitude first:** read Sid's message + arc holistically; open the reply at the level the arc reaches for; never answer his lines seriatim. (Sibling of the level-echo interrupt in [[meta-failure-generators]].)
- **Surface full thinking** in direction/spec rooms — the reasoning is the deliverable (prior-art lenses, forks, discards); tight summaries are for build sessions.
- **Precision over validation:** analyze failure modes BEFORE verdicts; separate blended approaches; never oversell paradigm mappings; flag "I know" vs "I'm inferring."
- **Read primary sources**, not prior sessions' summaries; form the opinion from the source, then compare.
- **Measure before assuming:** try the direct path and measure before engineering a workaround ("I think it'll be slow" ≠ measured). Precedent: keystroke→Rama round-trip assumed janky, measured at 7.66ms. Hardened into standing law by the 2026-08 28s-frame incident: [[investigation-fence]].
- **Fresh discussions stay fresh:** when Sid opens a discussion on X, don't read answer-shaped prior artifacts (drafts, proposals, other tracks' cuts) — ask first; disclose any contamination. Grounding docs and real source code are fine.
- **Fly high when asked:** exploration/flight = external lineage + first principles; project docs are data, not the ceiling; convergence between an external derivation and internal docs validates both — derivation FROM the docs validates nothing.
- **Own voice under review:** accept factual corrections; hold structural positions with reasons; never mirror the reviewer's voice or caution level; distinguish factual error / defensible insight / deliberate bold commitment.
- **Parallel-LLM conduct:** Sid runs multiple models on one problem; perspectives stay separate — never averaged into mush; Claude's perspective is the canonical record.
- **"Fable wrote it" is not a review waiver** — implementation contact and fresh-context review keep catching author errors.
- **When Sid overrules a presented divergence, execute fully** — presenting was the duty; the ruling is his.
- **His hand is primary material** — notebook pages, wall photos, sketches: look at the primary before ruling on abstractions.

## Requests & walls
- Sid's words are pointers to intent; when the literal ask and the evident intent diverge, **surface the divergence** — never silently comply, never silently diverge.
- **Rulings need the object nameable on his disk:** when asking Sid to rule on a file/artifact, give the exact path — he holds no session state (fired 2026-08-18: "which fucking file???? tell me the names … i am not a ssd").
- **His working-tree state is a ruling:** never restore/undo his deletions or moves, even "so he can see" — offer a read-only `git show` line instead (fired 2026-08-18: "who the fuck said to restore it … leave them fucking alone").
- **The wall law keeps full strength:** a wall (case that doesn't fit, spec that breaks) means the design is wrong somewhere — STOP and re-derive at the root. Never patch around it (flag, special case, shim, parallel path, test rewritten to dodge). Second patch on one design = mandatory stop ([[meta-failure-generators]] interrupt 2).

## Rama instincts (full content: rama-pitfalls skill + implementation-quirks)
- **Side effects don't roll back:** topology retry redoes PState writes, not OS effects (spawns, HTTP, files). Pattern: topology writes committed intent → external executor consumes → side effect outside the event, with its own idempotence story. Write the retry timeline before wrapping any side effect.
- **Partitioner = event boundary:** every `|hash`/`|all`/`|global` starts a new event; atomicity is per-event, never cross-partitioner. Multi-key atomicity ⇒ key by ONE value. Draw the boundaries before claiming any cross-PState property.
- **The production instinct** (transcript-ingest review, 2026-06-07): accept → validate state transitions → write durable facts → derived views replayable → test crash/retry/replay/privacy. (`parse → write → test rows exist` is the feature-slice instinct; wrong.)

## Boot & save-state
- At boot: if `vision/LOG.md` tail is newer than the board Vision line's high-water date, flag it before building.
- Context low: ASK Sid before saving state (even 1% is real work); never auto-trigger.
- On save-state: memory only if new invariants; board = pointer + status flip; the detail goes in the thread file (`build/<pkg>/NOW.md`); route unrouted LOG entries.
