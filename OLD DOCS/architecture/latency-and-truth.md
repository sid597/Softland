# Latency and truth — the no-optimism model

Analysis + receipts behind the standing ruling (decisions.md / S3: transport
committed, **no optimistic echo**). Landed 2026-07-17 from the first-light
P0-P1 session on Sid's "write this all down." Supporting analysis, not new
law — the rulings it cites stay where they live.

## The three segments (they are different pipes — never compare across)

Measured 2026-07-17, first-light P1 G1 drills (receipts:
`build/first-light/P1.md`):

| Segment | What it measures | Number |
|---|---|---|
| Rama echo, on-cluster | envelope → durable truth readable (foreign client, no browser) | p95 7.86ms |
| Browser truth echo | keydown → server round trip → **confirmed truth painted** | p95 26.6ms |
| Feel path | keydown → the typed char + caret painted from the local buffer | ~1–2 frames (240Hz ≈ 4–12ms), zero network |

The 26.6 contains the 7.86 plus the websocket hop each way and frame
alignment. The browser path was never "7.6ms" — that number is the
transport-and-server segment alone. Historical browser-path points:
block-write G7 (IPC era) p95 31.7–40.7 · durable-ground G3 (cluster, n=10
cold) p95 52 · pre-flip control p95 24.2 · post-flip p95 26.6.

## What the pending buffer is — and is not

The focused block paints the **edit buffer** (pending input + caret) before
Rama confirms. That is mechanically local echo, but it is not an optimistic
update, and the difference is epistemic, in the kernel's own vocabulary
(request asks / decision answers / event happened):

- **Optimistic update**: the client writes the edit into its copy of the
  TRUTH — the shared-state cache. Every reader sees it as fact; other views
  show it; it survives blur; rollback is a hidden reconciliation. The ask is
  presented as the answer.
- **The pending buffer**: a separate value composed over truth at render
  time, at the ONE surface doing the asking. Scoped to the focused block
  only; dies on blur (truth reasserts); no downstream reader — copies,
  trail, agents never see it as material; a refusal is a first-class visible
  event (`⟂ edit refused: <reason>` + revert), not a rollback of a claim.

**The honest gray zone**: in the focused block the pending text renders in
the same pixels where truth normally renders, unmarked except by focus and
the caret. A reader who doesn't know the model can't tell composed-input
from confirmed-record by looking. That is a LEGIBILITY gap, not an optimism
gap — and DIRECTION's "provenance + point-of-use legibility together" guard
is the standard it falls short of.

Two wish-shaped options, deliberately unbuilt (they wait for the land to
ask):

1. **Truth-only mode** — no pending buffer at all; characters appear only
   when Rama confirms. At local latency (~26ms) this would feel like a good
   terminal. Viable exactly while the land is local.
2. **Pending-tail marking** — unconfirmed characters ghosted, snapping
   solid as truth confirms. Draws the input-vs-record line in the pixels.
   Nearly subliminal at 26ms; exactly the honesty wanted at 250ms remote.

## If Rama ever leaves this PC

Echo ≈ 26ms + network RTT (general-knowledge RTTs, not measured): near
region (Mumbai/Bangalore from here) +10–40ms → ~40–65ms total — inside the
100ms "instant" threshold and near the S3 50ms budget. Far region (EU/US)
+120–280ms → the truth path becomes honestly slow.

What degrades with distance and what doesn't:

- **Typing feel: unchanged.** The buffer paint has zero network in it.
- **The quiet truth swap: invisible** when it agrees (almost always).
- **Refusals: the real casualty.** A stale-edit revert at 250ms lands after
  2–3 more characters were typed on a doomed buffer — a visible yank-back.
- Cross-view echo (copies, other faces) lags a beat; agent turns don't care.

The no-optimism ruling priced this in: distance makes the honesty more
visible, it doesn't break the model. Sid's PC as the durable ground is
currently a FEATURE — local-first with real durability. If hosting is ever
forced, the lever is region choice, not architecture.

## Upstream corroboration 1 — Electric v3 tutorial (read 2026-07-17)

The stack's own grain agrees with the ruling:

- **`e/Token` is Electric's no-optimism primitive**: click → busy →
  disabled → the server's REAL verdict → error fed back into the
  originating control. Block-write's envelope/decision/refusal loop is a
  hand-rolled durable-truth version of the same state machine.
- **Inputs-as-expressions**: Electric models "what the input currently
  says" as the input's own first-class reactive value, distinct from any
  record (the typeahead renders in-progress filter vs committed selection
  as two different values). There is no third category of "optimistically
  updated truth" in the model at all.
- **Continuous vs discrete**: never backpressure typing (stay responsive
  locally); backpressure the user only on discrete transactions (the busy
  token). Our split exactly.
- **"Synchronous dataflow"** — reactive propagation completes in the
  triggering stack frame — is the named mechanism behind the P1 G1 latency
  bug (heavy derived work riding keystroke/websocket turns) and why the
  microtask deferral is the right shape for heavy derivations.
- **Divergence discipline**: reactive cycles must reach a fixed point
  (their temperature-converter rounds at conversion boundaries). Carry into
  P3–P5, where wish → proposal → revision creates new loops.
- **The tutorial's persistence story ends where block-write begins** ("your
  edit state will not persist… we will get to that soon"). The durable
  request→decision→truth-overlay loop is Softland's own construction on
  Electric, consistent with its Token philosophy but not provided by it.

## Upstream corroboration 2 — Rama on local-first (Slack, verbatim)

Captured 2026-07-17 from the Rama Slack (thread date not captured; Sid's
question, Nathan Marz's answers):

> **siddharth yadav**: Is there some way to use rama in a local first way,
> maybe starting an ipc in addition to the distributed cluster? I want some
> subset of my data to be present locally — for faster interaction, I am
> trying to make a very interactive application; occasionally offline mode;
> to be able to modify data but not commit to db; reducing initial load
> time by using local data that was last interacted with. another thing I
> want to do is optimistic updates, make changes in ui, save locally,
> reactively query it and on another thread send the local changes by
> batching them to the main hosted cluster.
>
> **nathanmarz**: those are all interesting goals, but IPC's ephemeral
> nature doesn't support them. IPC is really just meant for testing and
> experimentation. with the full Rama release you could run a single node
> cluster (conductor, ZK, and supervisor) on the same node, but that may be
> too much overhead for those kinds of tasks which probably want everything
> managed in the same process.
>
> **siddharth yadav**: I see thanks Nathan, I read in some other thread
> that currently IPC saves its state in tmp directory if in future we could
> make the directory configurable, do you think it would be possible to use
> ipc in a local first way?
>
> **nathanmarz**: it's possible, but it's not a priority right now

What it settles:

- **IPC is not a local-first substrate** (ephemeral, testing-only — from
  the author). Durable-ground's move OFF IPC onto the real single-node
  cluster was the right call, independently confirmed.
- **The single-node cluster on one machine is Rama's supported local
  shape** — exactly the architecture the land now runs on by default.
  Nathan's "may be too much overhead" is answered empirically: it holds
  echo p95 7.86ms and survives kill-9 (durable-ground GATE).
- **The thread's optimistic-updates ask is historical.** It predates the
  measured reversal: microbatch echo FAILED (~210ms) → STREAM echo 7.66ms →
  transport committed, S3 ruled, no optimistic echo. The position changed
  because the numbers removed the need for the lie.
