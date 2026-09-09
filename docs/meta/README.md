# Meta — the loop we build Softland with

This folder holds the *way* we work, not the work. The loop below, the
kinds of prompt it uses, and a ledger of how the loop itself changes.
Instances live in their own round directories under `docs/below-the-waist/`
and are pointed at from here, never copied. Sid's voice is `vision/LOG.md`;
law is `docs/decisions.md` and memory. This folder is neither.

## The loop

Think until there is a hypothesis, make a prompt out of it, build it out in
parallel, read what the builds say back, merge, go again. After the merge,
one more step looks at the loop itself.

```
                  ┌────────────────────────────────────────────────────┐
                  │                                                    │
                  ▼                                                    │
    ╔═══════════════════╗                                              │
    ║ 1  THINK          ║  chat until there is a hypothesis            │
    ╚════════╤══════════╝                                              │
             ▼                                                         │
    ╔═══════════════════╗                                              │
    ║ 2  PROMPT         ║  /prompt-maker → body + asks + flavorings    │
    ╚════════╤══════════╝  Sid decides what to append, to how many     │
             ▼                                                         │
    ╔═══════════════════╗                                              │
    ║ 3  BUILD          ║  money where mouth is: parallel worktrees,   │
    ╚════════╤══════════╝  each from its own plan, builds are tests    │
             ▼                                                         │
    ╔═══════════════════╗                                              │
    ║ 4  READ           ║  what the builds say back: docs only,        │
    ╚════════╤══════════╝  N asks × 2 families, silences recorded      │
             ▼                                                         │
    ╔═══════════════════╗                                              │
    ║ 5  MERGE          ║  docs first, reads after → what should       │
    ╚════════╤══════════╝  exist, what to build first                  │
             ├─────────────► next hypothesis ──────────────────────────┘
             │
             ▼
    ╔═══════════════════╗
    ║ 6  META           ║  what this instance says about the loop
    ╚═══════════════════╝  → this folder, the skill, memory
```

Steps 1–5 are one instance. Step 6 runs after 5 and never in the same
session as 5: its evidence is the chats, not the code, and in one room the
meta wins.

## Why it is shaped like this

```
    ┌────────────────────────────────────────────────────────────────┐
    │  codex does what the words say.                                │
    │  claude becomes what is in the room, and the biggest thing in  │
    │  the room wins. the idea a session finds first organizes       │
    │  everything after it.                                          │
    └────────────────────────────────────────────────────────────────┘
```

That hypothesis (the prompt-maker skill, `.claude/skills/prompt-maker/`)
explains every split in the loop: why the body is fixed and the ask is one
paragraph (the ask is the first idea in the room), why the same ask runs in
both families (cross-family agreement is the signal to trust), why meta is
its own session, and why terrain goes in the message for Claude and as
attachments for Codex.

## The kinds of prompt

```
    prompt kinds
    │
    ├── meta ──────── the loop itself; after the merge; evidence = the chats
    │
    └── instance
        ├── body ──── one, fixed: goal · ground · files · Sid's questions
        │             · no verdict to carry
        ├── ask ───── one paragraph; the first idea in the room; N of them
        ├── flavoring one line; the room: claude = read scope,
        │             codex = attached + the question
        └── merge ─── docs first, reads after; written when the reads exist
```

## The read step, as it stands

```
                            body
              ┌──────────────┼──────────────┐
            gaps         repetition      boundary        ← asks
            ┌┴┐            ┌┴┐             ┌┴┐
            C X            C X             C X           ← C claude · X codex
            └┬┘            └┬┘             └┬┘
             └──────────────┼───────────────┘
                            ▼
                          merge ◄── the docs first, the six reads after
                            │
                 ┌──────────┴──────────┐
         what should exist        where the docs went silent
         what to build first      (→ back to the builder session, not the code)
```

Predictions are written before any read exists, so the round can check
itself. Gaps is the control for repetition.

## Where the instances are

- `docs/below-the-waist/ecs-layer-2026-09-08/ASKS.md` — the current read:
  body, three asks, predictions, flavorings.
- `docs/below-the-waist/ecs-layer-2026-09-08/` and
  `docs/below-the-waist/vantage/` — the plans the three builds came from.
- `docs/below-the-waist/visioning-2026-09-07/` — the "what next" round that
  produced the hypothesis.
- `docs/below-the-waist/two-chairs.md` — the loop's earlier shape: two
  chairs on one kind, comparison judged.

## How it changes

`LEDGER.md`, dated entries, one per change in the loop's shape. This file
is rewritten to match; it never accretes.
