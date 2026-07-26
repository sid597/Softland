# space-as-entity rung 3 (G10 lift) — Fable gate review

2026-07-26 · Fable (slim gate per the contract's sizing: drive R3-G2/G3/G4
live under fresh request ids, re-run censuses + suites + fast lane; full
re-derivation not required). Inputs: `RUNG3_CONTRACT.md` (binding),
`RUNG3_P1.md` (prior-pass record, used as input not authority), the full
working-tree diff read in this session. **Verdict: PASS.** No stop clause
fired; no read-only file touched; commit word remains Sid's.

## Independent re-runs (this session, never P1's word)

- **Suites** — focused 24 tests / 400 assertions · fast lane 190 / 1,795 ·
  isolated 15/232, 8/61, 5/165, 16/284, 11/103 — all 0 failures / 0 errors,
  matching P1 exactly. Shadow compile: 272 files, **0 compiled**, 0 warnings
  — the served client was already byte-current with the reviewed tree.
- **Censuses (R3-G6)** — re-derived by fresh grep: ONE
  `instance-site-legal?` defn with exactly three owner-aware product reads
  (write lane by parent facet `facet_master.clj:551`, console by claim
  subject `ground.cljs:2263`, extraction by facet `ground.cljs:2319`); ONE
  `camera-gesture-reserved?` var with exactly three reads
  (`ground.cljs:2257`, `ground.cljs:2323`, `space_material.cljc:68`), no
  fourth. Read-only files zero diff vs HEAD (`--numstat` empty over all
  six). `git diff --check` clean. Zero added atoms. Claim chain untouched.
- **Live server drives, fresh UUIDs** (receipt harness, server-read):
  - G5: all three hostile writes refused with the exact taxonomy (inherited
    fence `:facet-master/bindings-invalid` · `:instance-site-refused` with
    the block-only card · `:space/zoom-clamp-invalid` via the T9 seam);
    space history 4 revisions / 13 pointers before AND after; foreign 0/0.
  - G3: deviation lands `[0.1 3.0]` worn while shared serves `[0.1 8.0]`;
    tap/marquee `:instance`, cameras `:floor`; re-import mints the
    byte-identical content-addressed revision id; release restores 8.0 and
    master tiers.
  - G4: pin holds 8.0 over an activated shared 2.0; unpin wears 2.0;
    rollback restores base; pinned wear carries the pinned base revision id.
- **Live browser drives** (own harness, Chrome `--headless=new` +
  unsafe WebGPU; attestation first — adapter non-fallback, "google", NOT
  SwiftShader; CDP-trusted wheel):
  - G3 felt: wheel saturates at exactly zoom **3** under the deviation;
    worn tier instance; labels exactly `instance:space`.
  - G4 felt: pinned saturates at exactly **8** over shared 2.0; unpinned at
    exactly **2**; restored at exactly **8** and **0.1**.
  - G2: legal install → tap `:instance`, label exact; camera row refused
    `camera-gesture-reserved` (fence ordered before legality); `not-space`
    at ground refused site-illegal with block-only card; block install
    unchanged; clear restores master; cameras floor throughout.
- **G1 re-run note**: the harness's `:no-space-instance?` check is now
  permanently unsatisfiable — the durable instance master exists forever
  (append-only; release = holds/inherit), a world-moved fact, not a defect.
  Every BEHAVIORAL check re-ran true: all twelve probe decisions
  byte-identical to the banked baseline, worn base `[0.1 8.0]`, zero server
  instance-table rows, same active shared revision. G1 was legitimately
  gated by P1 when its precondition physically held; my re-run proves the
  strictly harder claim (released instance ⇒ behavior identical).
- **G7** re-verified inside the G2/G3 table reads (client label exact;
  server table zero instance tier per the declared P6 asymmetry).

## Falsification pass

The diff was read in full against the traps ledger. T-R1 bridges are
exactly two, both commented, both guarded by exact string `"space"` (a
foreign durable space subject stays under its own key — verified in code
and pinned in `space_material_test`). T-R2: the clamp reads
`wears-for space-subject` through the existing cache; the prewarm is
correctly suppressed when ANY facet serves an instance for subject
`"space"`, so a block whose subject-uid collided with `"space"` cannot be
masked. T-R3: owner-aware legality at all three lanes; T-R4: no fourth
fence call — the write lane's refusal proven inherited (G5a); T-R5
transition declared and observed; T-R6 pins moved and re-pinned. No new
state atom, no per-event work class added (read plan holds: pristine wheel
is a by-subject cache hit; post-instance identity change pays one
resolve+memoize — the declared deviant-block class).

## Rulings and findings (all non-blocking)

1. **Judgment call CONFIRMED** — the G3 repeat-import
   `:idempotency/material-fingerprint-conflict` is the pre-existing
   bootstrap-vs-normal import asymmetry in the Object Container adapter
   (bootstrap import carries its active pointer; a later normal import of
   the same source computes a different material fingerprint under the same
   import key). The contract's G3 demands fresh ids, server-read durable
   effect, and content-addressed revision-id equality — all proven; the
   first-ever deviation reported cleanly accepted. Surfacing rather than
   patching read-only code was correct. **Residue + cheap falsifier**: one
   OC test asserting a normal re-import of a bootstrap-imported source
   REPLAYS instead of conflicting — for whenever an OC package next opens.
   (The harness's `fresh-write-path-accepted?` or-check is acceptable only
   because `deviation-landed?` asserts pointer equality beside it.)
2. **Allowlist drift, unflagged** — `src-dev/dev.cljc` gained a
   `LAND_PINNED=1` guard (serve compiled client, no shadow watch). Not in
   the contract allowlist and absent from the phase artifact's deliverable
   list. Harness-class, and the default path is verbatim the old code
   (verified in the diff), so behavior-identical for every boot that
   doesn't set the var — but it must ride into Sid's commit decision
   visibly, not silently. Process note for the retro: the phase artifact's
   file list should have named it.
3. **Live-handoff drift** — the handoff's "dev app PID 1029880 live" was
   stale at gate boot (process dead, :8080 refused). Relaunched pinned
   (`LAND_PINNED=1`) from the reviewed tree for the browser drives; left
   live. The cluster survived throughout. Ops note, not a code finding.
4. **G1 harness staleness** — finding 2 above's sibling in the test tree:
   `g1`'s `:no-space-instance?` check describes a world that no longer
   exists; worth a one-line fix at close (assert released/inherit instead).

## D-006 evaluation notes

- Traps ledger earned its keep again: all six T-R comments sit at their
  sites; T-R1/T-R2 each name a silent-wrong failure the naive lift WOULD
  have shipped.
- The gate-partition sum-check rule (added at the rungs-1+2 close) held:
  P1 = all eight gates, zero remainder, no actor-bound gate staged — and
  none was needed at review.
- The single falsification finder (sized per the machine-cut rule) found
  three REAL counterexamples pre-gate — cache rescan, facet-only bridge
  hijack, prewarm masking — all repaired and pinned. The layer paid for
  itself; the gate found its repairs sound and nothing behind them.
- Implementer honesty was good: failures retained in the artifact, the
  import asymmetry surfaced for judgment instead of buried, no Fable
  verdict ghost-written.

## State left live

Shared `fm:space` base `[0.1 8.0]` active; instance master released/inherit
(nothing worn); console lane clear; cluster `:active`, UI :8888 HTTP 200;
dev app relaunched pinned on :8080 (this session). Worktree uncommitted —
code commit is Sid's word; after it lands, re-run HEAD-dynamic suites per
the close protocol, then retro.
