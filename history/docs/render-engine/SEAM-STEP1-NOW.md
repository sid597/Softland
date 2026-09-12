# SEAM-STEP1 — baton (STANDING frozen at package open · NOW appended per session)

## STANDING

- **Precedence, verbatim:** this file is a baton, not a source of truth; if
  it contradicts `SEAM-STEP1-CONTRACT.md` or `decisions.md`, those win —
  flag the discrepancy in NOW, do not pause.
- **Binding docs:** decisions.md "The render seam" (settled + amended
  2026-08-03) · `build/render-engine/SEAM-STEP1-CONTRACT.md`. The handoff
  file (`SEAM-STEP1-CONTRACT-HANDOFF.md`) is reconnaissance, not authority.
- **Shape:** ONE implementation phase (Codex, fresh context), acts 1→2→3,
  gates interleaved — G4 strictly before Act 2's unification. No PLAN.md
  (few-and-large ruling 2026-07-29); the contract carries plan-grade
  specificity and passed one fresh default-fail validation round.
- **Scope guard:** edit surface = CONTRACT §12 allowlist, exactly. Foreign
  uncommitted sibling work in the tree per CONTRACT §3: two files ADOPTED
  (`editor_compute.cljs`, `verify_text_layout_fence.mjs`), `ground.cljs`
  scalpel-limited to `pick-at`, everything else foreign is untouchable.
- **Hard rules:** NO git commits by the implementer (commit decision is
  Sid's, at gate review) · NEVER read `src/app/server/env.clj` · never
  push/merge the docs branch.
- **Verification duties:** memory-derived platform claims are checked
  against on-disk references before code — Missionary claims against
  `docs/reference/missionary-reference.txt` + the electric-docs skill's
  verified laws; m/signal specifically via CONTRACT G4's claims test
  (Missionary b.46 is transitive and unpinned).
- **Definition of done:** CONTRACT §10 — in-phase gates G1–G4, G5s, G6s,
  G7–G9 green · phase artifact `SEAM-STEP1-P1.md` with diff-derived file
  list sum-checked vs §12 · NOW entry · prediction-check paragraph · live
  receipts (G5L, G6L, G10) staged for Sid's sitting.
- **Stop clauses:** CONTRACT §14 (§S1–§S6). Classify first: implementer-
  fixable drift → fix citing precedent; genuine policy fork → STOP +
  decisions.md Open Questions. Never improvise on binding docs.
- **Gate review:** Fable, FULL tier (cutover-class frame path). What must
  NOT start without Sid: commits, anything touching §2's non-goals.

## NOW

- **2026-08-03 · Fable · contract session.** Register: contract authoring
  only (handoff's register correction honored — no `src/`/`test/` writes).
  Produced: `SEAM-STEP1-CONTRACT.md` (one phase, Codex; traps T1–T12,
  gates G1–G10 with owners + partition sum-check, allowlist §12, manifest
  grep-verified same day). Handoff corrections folded in: third store
  write surface (`update-nodes-by-address` funnels through `upsert-slot`
  :194 — projection patch is two-function-complete by construction);
  `ground.cljs` foreign hunks verified non-overlapping with `pick-at`
  (:2850/:4004 bounds); `<editor-rects+sidebar` re-located :414. Adopt
  ruling §3: RECORDED ADOPT at contract landing (Sid's preview).
  Validation R1 (fresh Opus, default-fail): **FAIL** — 4 FAIL-class + 9
  minor, preserved verbatim in `SEAM-STEP1-CONTRACT-VALIDATION-R1.md`;
  authoring re-ran with the artifact as input; all 13 folded in, incl.
  RULING R1 (pick-follows-paint — resolves the stampless-slot policy
  fork the round proved by probe; flagged for Sid's one-line veto at the
  preview). Scoped R2 verified the four FAIL-class resolutions.
  Next: Sid pastes the Codex opening prompt (in the board's engine block
  + below the contract); Codex runs the phase.

## 2026-08-03 · Codex · P1 implementation session

- Acts 1→2→3 completed in order; G4 was green on its first run before the sole production `m/signal` sharing point landed.
- G1/G2 first-run FAIL finding (verbatim):
  `FAIL in (g1-maintained-store-equivalence-across-public-writes) (maintained_view_test.clj:30)`
  `maintained and oracle pick agree at [0 0]`
  `expected: (= (select-keys (ss/pick store effective point) [:vi]) (oracle-pick store effective point))`
  `actual: (not (= {} nil))`
- The same nil-normalization finding repeated twice at `[250 50]`; the test oracle alone changed to `some->`; rerun: `Ran 40 tests containing 256 assertions. 0 failures, 0 errors.`
- G3/G5s/G6s fences green; G7 green (`41 tests`, `260 assertions`); final focused rerun green (`48 tests`, `234 assertions`); dev compile `0 warnings`.
- G8 first invocation signal (verbatim summary fields): `"pass": false`, `"classification": "candidate-pick-parity-failure"`, `"candidateParity": "14/21"`, `"productBoundsDivergenceSentinels": "7/7"`; contract close query then returned `true`; Chrome present, no owner-flip.
- G9 full: `:namespaces 56`, `:test 527`, `:assertions 7443`, `:pass 7443`, `:fail 0`, `:error 0`.
- No §14 stop; no commit/push. Sid/Fable handoff remains G5L, G6L, mandatory G10 flag-on minute + wearing, then FULL independent gate review.

## 2026-08-04 · Codex · G5L focused repair continuation

- Runtime probe: raw caret visibility ticked and focus was correct, but the ground/face `:ground-caret` stayed alpha 1 with zero GPU-pool changes across 11 transitions.
- Scope check: that caret is assembly-slot output outside SEAM Act 2; the only SEAM `ground.cljs` hunk is `pick-at`. No source/test repair was authorized or made.
- Exact G5L on SEAM's standalone editor path: 29.24s, 29 present/absent transitions (15 visible/14 hidden), focus `:editor`, stable recomputes `0` — PASS.
- Supplied live receipts: G10 twin `{"frames":569,"divergences":0,"lastFrame":1877}` PASS; pan/wheel/inspector normal.
- Supplementary adapter attestation: AMD / RDNA-3 / `GPUAdapterInfo.isFallbackAdapter=false`.
- Shaping samples remain separate and untouched. Square-box glyphs remain queued; cheapest attribution A/B is identical code points + font/backend/zoom/camera/Chrome/adapter against read-only opening HEAD `841a250e`.
- Receipt and Fable's three-point bounded-review handoff are appended to `SEAM-STEP1-P1.md`; no commit or push.

## 2026-08-05 · Codex · FULL-gate residual receipt closure

- Fable's one bounded FULL review is authoritative: code surface PASS; only G6L's literal adapter-first A/B and G10's wall-clock twin line were executed here.
- G6L first field: `isFallbackAdapter=false`, Radeon RX 7900 XTX, AMD/RDNA-3; same headed capture and three trusted-pointer samples on the real 174-block ground.
- G6L before at detached read-only `841a250e`: `0,0,1→180,90,1`, 2 GPU-canvas draws, `6796.5 ms`, first draw `5111.4 ms`, four `1672–1727 ms` long tasks, no page error.
- G6L after at the current SEAM tree, matched `0,0,1→180,90,1`: 2 GPU-canvas draws, `433.2 ms`, first draw `116.2 ms`, zero long tasks, no page error — PASS.
- G10 flag-on bounds: `performance.now()` `51321.3→116381.0`, duration `65059.7 ms`; receipt `{"frames":254,"divergences":0,"lastFrame":257}`, logged divergences `0`, page errors `0` — PASS.
- Probe-shifted camera was restored through the app settle path; flag-off, post-settle, and a separate fresh page all read `{x:0,y:0,zoom:1}` over 174 blocks.
- Temporary baseline server/worktree and opaque config/token symlinks were removed; the shared tree was never checked out or stashed.
- No source/test change, shaping-correction, glyph fix, index operation, commit, or push. Finding 3 remains queued for shaping-correction fingerprint normalization before that package opens.
- The one bounded review is consumed; Fable may mechanically confirm these two receipt rows only. Next authority: Sid's commit ruling, then §3 close staging and the post-commit HEAD-dynamic reruns.

## 2026-08-05 · Fable · close session — SEAM-STEP1 code LANDED

- Sid's commit ruling, verbatim: "just commit … just do it". §3 staging executed: 13 §12 files staged whole + `ground.cljs` scalpel (the pick-at hunk alone, +4/−1); all studio P1 / playground cut 1 hunks left uncommitted as found.
- Code commit `5f55cf5` on `docs/current-mental-model-local`: 14 files, +713/−141, new `maintained_view_test.clj` included.
- Post-commit HEAD-dynamic rerun (detached-worktree suite + fences) WAIVED by Sid ("skip"). Residue, named: the committed subset has never been suite-run in isolation — the first post-landing suite run covers it.
- Mitigation run: ns-custody grep at `5f55cf5` clean — no committed src/test path references studio / workshop-playground / image-material namespaces.
- shaping-correction OPENS: next act = CONTRACT §11 manifest sweep → the Codex one-context opening prompt into `build/shaping-correction/NOW.md`.
- SEAM's felt gates + package close remain queued behind shaping-correction per the 2026-08-04 coordination flag.
- Studio/playground archive-vs-survivorship put to Sid this session; tree unchanged pending his word.
