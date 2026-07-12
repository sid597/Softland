# Softland — settled ground

Read this once, then go build. Everything here is in force now; git holds
history. If reality contradicts this file, reality wins — fix the file in
place. This is not a statute book: no case numbers, no statuses, no evidence
citations. What you get from reading it: what we're building, the architecture
that's already settled (so you don't re-derive it), how we work, and the short
list of things only Sid decides.

Direction lives in `BETS.md` (North — Sid's own words — plus the active bet).
Sid's raw vision stream is `vision/LOG.md`: every new entry gets routed into
the structure same-session and the board's Vision line tracks it — nothing
Sid says is allowed to drop.

## How we work

- **Default is ACTION.** Build it, wear it, keep what survives. Everything is
  approved by default; pick the strongest option, record what you chose, move.
- **Nothing is parked.** There is NOW (the board) and LATER (everything
  else). Any session may pull LATER into NOW when it serves what Sid asked.
  No permission slips, no gating ceremonies.
- **Sid's requests are approximate** — the simplest, cleanest design outranks
  his literal words; a wall means the design is wrong: re-derive, never patch
  around (CLAUDE.md top section).
- **Settled ≠ frozen.** To change anything below: bring a first-principles
  case or a measurement, straight to Sid — he rules fast. What's not okay is
  re-arguing it session after session with no new substance.
- **New capability?** Answer Sid's own questions (2026-07-11), then build the
  strongest version: what do we lose and what do we get · how controllable is
  it · how does it follow from Softland's own eyes · is it buildable,
  modifiable, liveable from INSIDE the land?
- **Quality machinery that stays** (it keeps catching real bugs, cheaply):
  one batched falsification + one gate per coding wave; fresh-context
  subagents as the validation layers (fresh context ≠ fresh session); a
  receipt over the full REAL corpus for anything that processes the whole
  corpus; every session ends with a thread-file entry + board-line flip.
  Mechanics: `.claude/skills/work-package/SKILL.md`.
- **Role split:** Fable writes contracts, runs gates, adjudicates forks — and
  implements directly whenever that's the fastest path; cheaper models run
  phases under contracts.

## What we're building NOW

**The trail view** — Sid's 27-04 wall panel, alive: timeline · what changed
concretely · the product-DAG with dead-ends, over Softland's own material
(code, commits, docs, chats), answering "where are we, how did we get here,
which crossroads did we take."

- **The unit is the sense-line**: episodes and their marks. Files, sessions,
  and commits are one evidence lens, not the unit. Model:
  `sense-line-model.md`; working map: `build/sense-line-mvp/DIRECTION.md`.
- **Agent-legible first** (View 3): the material must be legible to machine
  minds, not only to Sid — the first paying reader is an agent. Then the
  threaded/DAG timeline (Sid's first pixel surface). Canvas when we get there.
- **The view renders NOW**, ugly and gappy — each gap names the next ingestor
  slice. Import work is ordered by what the rendered view needs, never the
  other way around.
- **Writing:** today the view reads and the Claude CLI writes (the agent
  asserts on Sid's word; both actors durably recorded). The editor write path
  is committed: **direct write over a STREAM topology, echo streamed back**
  (measured: stream echo p95 7.66ms; microbatch measured ~210ms cadence and
  rejected — don't reopen it, the numbers are in `build/write-echo/NOW.md`).
  No optimistic text echo. Block-write contract is the live next step; UI
  write gestures come right after it.

## Settled architecture — use it, don't re-derive it

- **Rama is truth.** Workers, agents, and UI actions stream observations and
  requests INTO Rama as events; the UI reads materialized state OUT. No
  side-channel state, no truth outside the log.
- **The map must not lie.** Provenance is first-class everywhere:
  `asserted-by` (sid | llm | import) rides every relation; machine output is
  visibly distinct from Sid's hand, always; silver/gold tiers where machine
  work grounds human claims.
- **Relations are typed edges** (`relation_kernel.clj`): a closed kind enum,
  grown by one reviewed line when a real consumer needs a kind; identity,
  idempotency, retraction, and history built in. All discourse structure
  rides this kernel.
- **Two clocks on everything that carries time:** claimed-time (orders
  semantic history) is distinct from arrival-time (when the land learned it).
  A July re-import of April notes must never render as "today."
- **External code is a view:** git owns versioning, the filesystem owns
  storage; address code (blob-sha + path anchors), never copy it; on
  conflict git wins.
- **Furniture is data:** faces/assemblies are arrangement-only data over a
  primitive vocabulary; keywords and addresses persist in Rama, fn values
  never; anything that wants to be a program becomes real code in the code
  lane. The loop already closes end-to-end: design conversation → assembly →
  validated → rendered → worn (first done 2026-07-11).
- **One render substrate:** one scene store keyed `(view-instance, address)`,
  one diff pipeline, one camera loop; the world, the islands, and the text
  faces are projections over it. Seams change projections, never
  architectures. (Instrument: `build/render-north/DELTA-B1.md`.)
- **Stream writes are at-least-once:** every stream write path derives a
  deterministic op-id from the request-id so a replay overwrites the same
  keys — never duplicates. A replayed-event test ships with every write path.
- **Ingest is idempotent and convergent everywhere:** deterministic ids,
  re-import safe by construction; a new import-key prefix registers its
  routing and ships a foreign-read gate.

## Only Sid decides

Spending money · pushing/merging the docs branch (never) · `env.clj` (never
read it) · North's text · genuinely irreversible architecture forks. Current
open fork of that kind: **durability** — durable cluster vs durable
spine-edge replay log.

## Open questions — undecided; say your take when you hit one

- The durability fork (above — Sid's).
- Confidence/credential algebra for the trail→code join.
- Question-as-first-class-unit design (design track).
- The bet check ~2026-07-19: is new thinking starting inside Softland or
  still on paper? (`BETS.md` verdict log — the active bet's clock.)
