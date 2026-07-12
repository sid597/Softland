# block-write — thread file

## STANDING (frozen at open, 2026-07-12)

- **Binding:** decisions.md (settled ground) > `CONTRACT.md` (this package) >
  this file. Precedence rule verbatim: this file is a baton, not a source of
  truth; if it contradicts CONTRACT.md or decisions.md, those win — flag the
  discrepancy in NOW, do not pause.
- **Package:** connect the reader face's blocks to the EXISTING
  object-container `:object/edit` stream path; echo = materialized truth;
  measure E2E vs the settled criterion. No new module/depot/topology (BW-T1).
- **Allowlist:** Lane B = new client namespace(s) under
  `src/app/client/workspace/` + reader-face wiring; Lane A = ADDITIVE-only
  edits in `face_projection.clj` / artery (`electric_flow.cljc`) + IPC test
  namespaces. NOBODY touches `edit-effects`/kernel semantics, `text_kernel.clj`,
  or `machine_cut.clj`. Probe files stay uncommitted.
- **Verification duty:** memory-derived platform claims re-verified against
  the §11 manifest before code; suites per /rama + /work-package mechanics.
- **Definition of done:** gates G1-G9 green (G7 numbers in the gate artifact,
  G8 worn by Sid) → falsification gate review → close + retro.
- **Stop clauses:** CONTRACT §10 (S1 resolution ambiguity, S2 overlay break,
  S3 echo fail post-narrowing). Escalate, never improvise.
- **Must not start without Sid:** wave dispatch (subagent spend). T11
  coordination: machine-cut code is committed; only its close session could
  still touch `face_projection.clj`/artery — coordinate via the board.
- **Hard rules:** docs commits on the docs branch only, code/docs never
  mixed; never read `env.clj`.

## NOW (≤15 lines per entry; newest last)

- 2026-07-12 · Fable (this session) · **PACKAGE OPENED — CONTRACT v1
  authored.** Read pass discovered the write organ already exists:
  object-container STREAM topology + `:object/edit` with validation,
  graduation/revision model, stale-seq protection, object-key-scoped
  idempotency journal; `read-unit` already overlays graduated content
  (`block_distiller.clj:1317`) → the reader face's river-page SEES edits.
  Package re-scoped to the honest gap: client affordance + outbox
  (envelope CONTRACT §3), `document-container-id` resolution pin (§4,
  stop-clause S1), epoch-bump echo + pre-named single-unit narrowing (§5),
  browser E2E G7 vs the unchanged criterion, wearing G8. Every claim
  file:line-verified (§11). Traps BW-T1-T10. Policy vocabulary/visibility
  NOT here — parallel design session, opening prompt at
  `docs/sessions/policy-design-opening-prompt-2026-07-12.md`. Next: Sid
  reads contract; lanes dispatch AFTER machine-cut INT lands.

- 2026-07-12 · Fable (orchestrating session) · **WAVE DISPATCHED.** Phase-0
  ran first (fresh Opus subagent): `document-container-id` **PINNED — (b)
  projection field** carrying the unit's OWN field (a per-event
  `chat-message-id`, `block_distiller.clj:740`), NOT
  `chat-conversation-id(object-key)` — §4's candidate (a) was a category
  slip (projection READ key ≠ block's document parent); echo can't tell
  them apart, durable provenance can (map-must-not-lie broke the tie; not
  S1). Traces re-verified in-session; pin ADJUDICATED ACCEPTED —
  `PHASE_0.md`. Lanes A ∥ B dispatched (Opus, disjoint fences). Two
  pre-adjudicated calls, flagged for gate: (1) `content-hash` stamped
  server-side in Lane A's entry point via kernel `source-hash` (no cljs
  crypto port; §4 table said Lane B); (2) block_distiller one-key add sits
  beyond STANDING's allowlist but inside CONTRACT §4's "ONE additive
  river-page/projection field" — contract wins. INT (G7/G8) next here.

- 2026-07-12 · Fable (orchestrating session) · **LANES GREEN + S2 FIRED AND
  RULED at INT.** Lane A: G1-G5(IPC)+G9 first-run green (41a) + entry point
  `submit-block-edit!` in the artery (face_projection is read-only BY
  CONSTRUCTION — g12 grep; placement divergence flagged). Lane B: outbox +
  buffer machine + G6/G5-client green (6t/47a). INT wired the seam (decision
  shape adapted ONE place, `atom-submit!`), face click-to-focus, `:face-edit`
  key routing, scene-level pending-input/caret/refusal overlay, `:block-truth`
  single-unit serve + truth overlay (§5 narrowing). **S2 verbatim: river-page
  served `:text (:derived-content-text unit)` — the raw import row, never the
  graduation overlay; probe run showed full-pull channel echoed 0/120 and
  edits could not survive reboot (G8).** Ruling (S2: "stop, re-verify,
  contract amends"): distiller `:text` → `(:content-text read-result)` (the
  overlay, total); CONTRACT §1 amended in place; gated (edited block serves
  revised content, never-edited blocks byte-identical). Suites: block-write
  49a + distiller + face-projection = 1003a green. G7 smoke (10s@12/s,
  headed, real WebGPU face): narrow-path echo p95 40.7ms, 1 stall — criterion
  shape holds; full 60s run next. INV-19 1s debounce = the as-built full-pull
  miss (BW-T10 finding, Lane A). Probe UNCOMMITTED (`block_edit_probe.cljs` +
  tagged require in runtime.cljs).

- 2026-07-12 · Fable (orchestrating session) · **G7 MEASURED — S3 FIRED,
  returns to Sid.** 4 × 60s @ 12/s headed runs (real WebGPU face, real
  keydowns, full product path): narrow-channel echo p95 31.7-39.5ms (budget
  50 — every run PASSES) **but stalls >100ms = 2/4/7/2 vs the ≤1/min clause
  — FAIL every run.** Thin 0.3-1% tail, max ~145ms, not load/length-shaped
  (R4 forensics: t=0.75s cold + t=16s isolated). As-built full-pull channel
  p50 ~31s (INV-19 re-arms under typing) — first-measure miss recorded, §5
  narrowing engaged per contract. Typist never waits (pending-input paints
  immediately); echo bounds only refusal-revert (~145ms worst). Real boundary
  found: ~1.8KB block → 14 relieve-dropped keystrokes (whole-face rebuild per
  key) → LATER: focused-block partial rebuild. Restart-survival observed live
  (smoke edits survived the server restart). Record + options: `INT.md`.
  **Sid: rule S3** (rec: re-express the tail bound as p99 ≤100ms — all runs
  pass) **+ wear G8** (INT.md §5, ~5 min). Falsification finder next here.
