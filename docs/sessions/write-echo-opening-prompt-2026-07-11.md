# write-echo — opening prompt (2026-07-11)

**Session name:** `write-echo` · **Model: Opus 4.8** · one session, measurement spike.
Probe code stays UNCOMMITTED (evidence, not product — framework probe convention).
The verdict is NOT yours to rule: numbers go to `build/write-echo/NOW.md`; a Fable
session drafts the decisions.md PROPOSED entry from them; Sid countersigns.

## The question this spike answers

Sid's target editor architecture (stated 2026-07-11, vision LOG): **write directly
to Rama and stream back — no optimistic echo — unless infeasible.** This spike
measures whether the direct path is feasible at typing speed. It decides the
`block-write` package's route.

## Pre-registered evaluation (drafted by Fable BEFORE this spike; do not adjust
## thresholds after seeing data — that's the point of pre-registration)

- **Load:** single-character edit events against ONE block artifact, sustained
  bursts: 12 events/sec for 60s (fast typist) and 3/sec for 60s (relaxed), with
  the dev app's normal watchers running.
- **Measured loop (all three legs mandatory for a run to count as END-TO-END):**
  (1) client emit → depot append **ack**; (2) materialization (PState/topology,
  not a bare server atom); (3) stream back through Electric into a client atom
  (browser). Echo time = emit timestamp → client receipt; report RAF alignment
  separately.
- **Record:** p50 / p95 / p99 / max; stall count (>100ms) per minute; Rama-side
  events/sec sustained; payload shape used.
- **Direct-write STANDS if:** p95 ≤ 50ms AND ≤1 stall (>100ms) per sustained
  minute. **FAILS otherwise.** Sid's fingers on the real editor remain the final
  acceptance in BOTH directions — but these numbers pre-commit what "infeasible"
  means so the fallback can't creep in by vibes.
- **Caret sub-measurement:** report echo as caret-relevant latency explicitly.
  Pre-registered fallback ladder (Fable, from the 2026-07-11 analysis): if text
  passes but caret feel fails Sid later, the concession is a LOCAL CARET
  AFFORDANCE only (text truth stays streamed). Full optimistic text stays off
  the table unless the direct path FAILS outright (Sid: "never by default").

## How to build the probe (least invention, most reality)

- **Write leg precedent:** W2 shipped the first write e/defn (`RecordFaceWear` +
  outbox — see `build/framework/W2-INT.md` and the wiring in
  `face_wiring.cljs`/`electric_flow.cljc`). Ride that shape for the probe emit.
- **Server leg:** use an EXISTING lawful request path — `text_kernel.clj` has the
  request depot with per-key serialization + `:append-ack` + status
  materialization (`*text-requests-depot`, `foreign-append!` at :562). Do NOT
  add new event types or kernel edits; a benign existing request type against a
  probe artifact is fine. Document exactly which path you measured.
- **Read leg:** a real materialized read streamed via the Electric bridge (the
  faces pull / server-atom + e/watch S40 pattern). NOT `!editor-doc-atom` (a
  bare mirror — measuring it would flatter the numbers).
- If a full browser E2E harness proves too heavy for one session: measure
  composed legs ((1)+(2) JVM-side, (3) separately) and label the result
  **COMPOSED, NOT END-TO-END** — an honest composed number beats a fake E2E one.
- Consult `.claude/skills/electric-docs/SKILL.md` + `.claude/skills/rama/` before
  wiring anything reactive (L1/R2/R3/L8 laws).

## Fences & duties

- New probe files only; UNCOMMITTED at session end. No kernel/depot/schema edits.
  NEVER read `src/app/server/env.clj`.
- Results (numbers table + method + deviations) → `build/write-echo/NOW.md`
  (docs commit on the docs branch). Flip your board line only.
- Suite must still be green at close (you added no product code, so baseline).
