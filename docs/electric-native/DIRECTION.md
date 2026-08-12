# Electric-Native — connect the engine to the correct Electric; prove it by making a block

2026-08-12/13 · Sid's ruling, verbatim: "we should first fix the architecture
truly — electric should drive the rendering engine and vice versa … the real
work is having the solid architecture of electric based but engine based +
webgpu …" and "… the first test is if i can make the block component in it
using ecs style/agent driven development." The workshop is the WANT; the
architecture is the ORDER; both are that sentence.

This file is the guide. Detail lives in pointers, never restated here:
binding law = decisions.md "The render seam" (read PRIMARY before cutting
any contract) · evidence = `RECON.md` beside this file (tree receipts,
2026-08-13) · history/why = `vision/LOG.md` 2026-08-12 entry.

## The picture

- **HAVE** — Rama (all truth, face definitions included) · Electric (the
  courier) · the engine (draws everything: text, images, paths, 3D). They
  are already connected: ordinary blocks render through the engine today.
- **PROBLEM** — it is the WRONG connection: every change re-sends the whole
  page (~1/sec) as one anonymous value, and the engine's real editing (T2)
  is not wired to real blocks. Every felt bug lives at this seam.
- **WORK** — make the CORRECT connection. "Correct Electric" means exactly
  two arrows: ↓ changes travel by id (one edit moves one thing, instantly)
  · ↑ the view tells Electric what it needs (pay only for what you look at).
- **THEN** — the first test: the agent MAKES the block in Softland; Sid
  uses it. That act opens the workshop, which then grows by use (browser
  organs · paper-page drawing, User Goal 1 · AI drawing in-land, User
  Goal 2).

Fuller map of what exists (nascent VM · engine floors · the missing
browser · the block half-crossed: engine eye, old hand): `RECON.md` + the
LOG entry. Don't re-derive it.

## The road

**1 · Electric drives the engine** (the ↓ arrow) — one connection, three
organs, dependency order:

- **1a — typing stops lagging.** ← NEXT ACT. The shaping correction is
  already contracted with a banked, binding profiling receipt
  (`docs/shaping-correction/CONTRACT.md`; it precedes 1b's close). Check
  Sid's felt lag against that receipt first — fence: no fresh blind
  profile, no verdict from reading. In scope → executing the existing
  contract IS the relief. Out of scope → fresh probe, pre-registered
  predictions.
- **1b — the screen maintains itself from one source.** SEAM-STEP1
  (landed `5f55cf5`) closes; rides 1a.
- **1c — edits travel by id.** The keyed wire + store contract:
  `e/diff-by :unit-id` server-side / `e/for-by` client-side — our ids on
  top, their machinery inside, feeding the scene store and the stubbed
  buffer-pool bridge; the hand-wired watches collapse into per-key reads.
  Comparison-minting is a NAMED temporary stage — demotion to
  write-site/per-key minting pre-registered (comparer becomes the oracle,
  per the growth law). Converged 2026-08-13, two sessions merged; probes
  and receipts in `RECON.md`.

**2 · The engine drives Electric** (the ↑ arrow) — viewport-residency as
demand v1: "resident cost scales with what you're looking at, not what you
own" (Sid, board NOW). Renderer publishes visibility facts; a broker turns
them into subscriptions; residency ≠ existence. Collaboration is a named
beneficiary of this wire.

**3 · The spec sitting** (parallel lane — Sid + Fable, any time, must land
before 4): dissect the block as the specimen — anatomy + verb registry at
intent grain ("editable text" · "accepts children"), human names only; deep
behaviors are engine verbs material binds to (T2 stays the one text
editor); every gap surfaces as a NAMED verb request (the escape gauge,
reborn).

**4 · The face taste-test** — the block-face built both ways (Electric
generic host vs Missionary host), judged on real handling (close/reopen ·
mid-drag teardown · hot-swap). The last machinery question answers itself.

**5 · THE GATE — the test Sid named.** The agent makes the Block through
the one shared controller: grammar v1 = only the operations the making
needs; candidates Rama-side, real and functional, mint on accept (Sid's
07-31 rulings); one grammar, two hands (Sid's future halo edits emit the
same operations). Sid wears it. The workshop's first act.

Playgrounds stay lawful at any step (Sid's 08-01 stream ruling: broad,
playable, allowed to break): drawing-as-PLAY any time on the existing
floors; drawing-that-STICKS is what this road buys. Workshop-first was
lived and ruled out (G7 + playground cuts: "the root cause of all problems
is trying to build out stuff for which this is not made"). Staged ≠
patchy: a patch hides a systemic wrong; a stage names its replacement in
the contract.

## Rulings in force (Sid, 2026-08-12/13; verbatim in the LOG)

- **Faces are data, all the way** — landed in the render-seam section; the
  machinery probe (road 4) stays open, its face now named: the Block.
- **Stand on shoulders, never rebuild** — already the settled four
  dispositions; reaffirmed.
- **Hand-porting the block: wrong move.** "Fully ready" is DEFINED by the
  gate — never ready-in-the-abstract.

## Working vocabulary (names finalize by recurrence; the naming is Sid's)

**constitution** — what explains an entity: the resolved graph (facets ·
active revisions · deviations · recipes · relations · bindings · anatomy ·
interpreter/host contributions); never one parent edge, never a "type" ·
**the gesture** — halo = one "why this?" move, recursive along whichever
edge, applies to its own instruments · **the shoreline** — walks end where
material meets code; campaigns move it inward.

## Guards

Browser organs friction-pulled only (G7 stands guard) · no new floors
until lived pull (breadth necessary, never sufficient) · engine close is a
relay (after the block: ink · live components · Region3D) · ladder debt
flagged, untouched: review sitting due (H1 · C1/C2 instruments · C3).

## Open — who closes what

- **Receipt (1a):** the lag's mechanism; whether relief = the existing
  contract executing.
- **Probes (1c–2, 4; listed in `RECON.md`):** per-key overhead at real
  cardinalities · pool-bridge contract · full-pull cost · demotion trigger
  · host machinery · row shape (what a served unit carries).
- **Sitting (3):** anatomy spec · verb grain + names — Sid redlines output.
- **Gate (5):** grammar v1 · the draft room's first form.
- **Sid, now: nothing.** Two dormant forks are asked AT the sitting: gate
  scope (made vs made-and-worn-daily) · how far "faces are data all the
  way" reaches into host machinery.

## Parked trees (fence: receipts before attribution)

click-to-focus offset (hybrid-seam suspect class) · typing lag → road 1a ·
softer text → board-tracked under frame-view, pinned pixel A/B required.
