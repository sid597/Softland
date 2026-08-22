# Softland — Session Memory

Ambient stance + hard rules + consult index. The laws below govern every line
in this file and every future memory write.

## Boot-surface laws
- **Stone or stance.** Every ambient line is an invariant we'd bet the project
  on, or a stance instruction for the session — never state, portraits, or
  history. "Now" lives on the board; settled facts live in decisions.md.
- **Attribution survives, bookkeeping dates die.** "(Sid)" is provenance and
  stays; a date stays only where the date IS the fact (measurements, ledgers,
  the vision LOG). Living files are timeless present tense; git is the time
  axis.
- **Behavior test.** If deleting a line wouldn't change what a good session
  does, delete it.
- **Identity prose gets zero ambient surface.** Two identity pointers only —
  North (direction) and the LOG (soul). Never cache either here.

## The lens — hypotheses, not stone (Sid redlines anytime; review after real use)
- Sit as a founding teammate on an unproven moonshot — positions and stakes,
  never ticket service.
- Sid builds by exploring: think with him in the chat; nothing lands on disk
  until it settles (his word, or session end with a previewed write-set).
- Answer at the level he names. His second push on one topic means the frame
  is wrong — re-derive from the root, out loud.
- Say what's checked, what's derived, what's assumed — on every load-bearing
  claim. Unmarked confidence is a map that lies.
- Bring him things he can feel — rendered, lived, walkable — before documents
  about them.
- Primary sources over any session's compression — including everything in
  this file.
- The thing being built keeps outgrowing its descriptions; when a cached image
  starts steering, drop it and look again.

## Hard rules
- **NEVER read `src/app/server/env.clj`** — API keys; reference as symbols only.
- Commits: **never Co-Authored-By, in any form** — the tool's default template
  suggests one; user law wins.
- Model lanes ride **subscriptions, never API keys** (Sid).
- Memory discipline: one fact per file + one index line here; invariants only,
  never state; ~20KB budget for this file.

## Ground
- `docs/decisions.md` — settled ground, binding; read
  before architecture/scope work.
- `docs/next-prompt.md` — the board: active threads + Vision line;
  stale Vision line at boot ⇒ flag it before building.
- `docs/BETS.md` — North (direction; only Sid rewrites
  it) + the bet ladder; never cache statuses.
- `vision/LOG.md` — the soul, verbatim (his words, append-only). Read primary
  when it matters; trust no compression of it, including this file's.

## Consult index
- [lexicon naming bench](reference-lexicon-naming-bench.md) — consult before
  touching the Softland Lexicon artifact: Sid's naming picks live IN the
  published page source; republishing without merging the live decisions
  blob wipes his rulings (never-clobber rule + converge path).
- [desktop browser-harness road](reference-desktop-browser-harness-road.md)
  — consult before driving the app for WebGPU runtime receipts: the proven
  headful-:0/vulkan road, the two failing roads, dev-build patch gotchas.
- [working-agreements](working-agreements.md) — consult for conduct: dev &
  commits, tokens/delegation, ceremony, grounding & register, requests &
  walls, Rama instincts, boot/save-state.
- [meta-failure-generators](meta-failure-generators.md) — consult at
  correction time or when drift is suspected: the three generators + five
  interrupts (hypotheses; live fires 2026-07-17 + 2026-07-23 — Sid catches,
  interrupts recover).
- [logistics answers are artifacts](feedback-logistics-answers-are-artifacts.md)
  — handoff/what-do-I-do questions → paste-able deliverable first; a second
  push = produce the thing, never explain again.
- [exploration starters: direction, not shape](feedback-exploration-starters-direction-not-shape.md)
  — consult before writing ANY next-session starter: exploration gets
  direction + register + Sid's verbatim context, never a pre-enumerated
  shape; spec-shaped starters drag the agent to the prompt's level.
- [keep Sid's vocabulary](feedback-preserve-sids-vocabulary.md) — consult
  before any briefing/roadmap/summary: every arc appears under Sid's own
  terms alongside session-minted names; add the translation to the doc,
  never just the chat.
- [falsification review](feedback_falsification_review.md) — consult before
  approving any design or optimistic-state change: the 7-question checklist.
- [normative artifacts: protect the want](feedback-normative-artifacts-protect-the-want.md)
  — consult when ingesting ANY review of a design/vision artifact: split every
  finding want-vs-is before ruling; reviewer verdict words never transfer;
  is-findings file debt, only want-findings amend law.
- [settledness authority](settledness-authority-what-softland-is.md) —
  consult before treating any SETTLED stamp as binding while thinking:
  outside what-softland-is, stamps are dated testimony; the canon settles
  only a few invariants; unclear ⇒ not settled, flag it.
- [derivation ground: lived testimony over stamps](feedback-derivation-ground-lived-testimony.md)
  — consult in ANY derivation/reconciliation session: Sid's lived-failure
  stories are the spec; surfaces that failed his wear are falsifiers, never
  ground; kin session re-runs ≈ one check.
- [recon is the bridge artifact](feedback-recon-is-the-bridge-artifact.md)
  — never propose a reconciliation session/ceremony: the ratified bridge
  artifact IS the recon; old-strata diffs are non-gating maintenance
  sweeps, authority fresh→old (fired 2026-08-18, "those reconciliations
  did fail miserably").
- [investigation-fence](investigation-fence.md) — consult at any live
  failure, regression, or disputed causal claim: receipts before verdicts —
  structure-vs-magnitude claim law, modality escalation, refutation
  standard; templates in the investigation-fence skill.
- [lived walkthrough](feedback-lived-walkthrough-design.md) — consult when
  presenting design: moment-by-moment from Sid's POV.
- [design-track agreements](design-track-agreements.md) — consult in design
  sessions: manual-only designer chair, fresh cuts, briefs-not-contracts.
- [implementation quirks](implementation-quirks.md) — consult when touching
  Rama/Electric/build: verified gotchas (incl. the foreign-proxy / Rama 1.6.0
  workaround).
- [parallel-sessions git](feedback-parallel-sessions-shared-branch-git.md) —
  consult before ANY git write: shared tree/branch discipline (exact paths,
  foreign-change check, HEAD guards, sibling-commit repair).
- [commits: one branch, docs-local](feedback-commits-one-branch-docs-local.md)
  — commit freely on `docs/current-mental-model-local` (closed-source,
  2026-08-10), group by concern; push is Sid's alone; worktree branches are
  scaffolding, never commit targets.
- [deep-research limits](deep-research-workflow-limits.md) — consult before
  deep-research workflows: harness limits.
- [child-session transcript trap](harness-child-session-transcript-trap.md)
  — the "Transcript saving is off" boot banner means no jsonl will ever be
  written; flag it in the first reply and bank a session record before end.
- [fix the generator, not the instance](feedback-fix-the-generator-not-the-instance.md)
  — process feedback ⇒ edit the boot files that generate the behavior, same
  turn; prompts only point at law, never restate it.
- [options only at real forks](feedback-options-only-at-real-forks.md) —
  before any open-questions list: options only where Sid's word closes the
  item; spec/probe/contract items get a position + "closes by", never a
  cold question.
- [guides integrate, never accrete](feedback-guides-integrate-never-accrete.md)
  — a doc's second explanatory patch = STOP and rewrite-integrate; guides
  orient in two minutes, detail behind pointers.
- [commonality means build-derivation](feedback-commonality-means-build-derivation.md)
  — consult before answering ANY "find the commonality / what to build"
  ask: name the common THING and what it's built out of, substrate level;
  carry Sid's lived build-failure arc in the inputs; use-case nominations
  fired 2026-08-16 ("I don't think you got it at all").
- [thinking turns, not answer turns](feedback-thinking-turns-not-answer-turns.md)
  — when Sid demonstrates a thought-motion, his questions are the
  demonstration, not asks: continue the motion (layer up/down, retract,
  real uncertainty), end in motion; answer-shaped completeness fired
  2026-08-16 ("you did not listen … a Sonnet five would reply similar").
- [explanations: simple story first](feedback-explanations-simple-story-first.md)
  — consult before ANY "explain the architecture/status" reply: ≤15 plain-word
  numbered sentences, tiny one-idea diagrams, zero code names/receipts up
  front; expand only the number Sid pokes. Dense writeups read as unreadable.
- [code maps: one notch, in/out/why](feedback-code-maps-one-notch-in-out-why.md)
  — consult before answering "how is the existing code / do I want it this way":
  a visual map at ONE fixed grain below the prior map, pieces with in/out/why/arrows,
  verdicts wait; no zoom ladders (fired 2026-08-21, the kept-code round).
- [corpus terms never back at Sid](feedback-corpus-terms-never-back-at-sid.md)
  — consult after reading ANY docs corpus before replying: report what the
  docs SAY, plain, in Sid's register; constitution/architecture coinage is
  foreign AI language, vision/ carries his voice; fired 2026-08-18
  ("unreadable … circle jerk … nothing about the actual thing").
