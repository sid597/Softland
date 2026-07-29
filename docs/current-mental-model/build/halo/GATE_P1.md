# halo P1 — independent gate record

2026-07-29 · Fable (orchestrating session, fresh gate context — no shared
context with the implementer) · tier: **SLIM per CONTRACT §8** ("claim-risk
sized"; the contract's explicit tier ruling governs over the skill's
durable-touch default). Inputs used as prior-pass records, never authority:
`P1.md`, `NOW.md`. The judged object is the exact unstaged worktree at HEAD
`785841d82b1c64e64cd1b329019b6e286fa00d26`.

## VERDICT: PASS

Gate PASS does not close the package: Sid rules the separate code and docs
commits. Sid's headed unscaffolded first open remains deliberately OUTSIDE
this gate (C2's instrument, CONTRACT §8 — scaffolding it would poison the
measurement).

## Fence — re-verified this session, not taken from the receipt

- HEAD `785841d` unchanged before/after every gate step; index empty;
  nothing staged; `git diff --check` clean.
- Modified tracked paths = exactly the 17 CONTRACT §6 source/test paths
  (12 source + 5 test), plus `build/halo/{NOW,P1}.md` (docs, free). The two
  pre-existing foreign probe artifacts untouched.
- Zero-diff pins re-hashed byte-exact:
  `e1f1836f…af4179 episode.clj` · `ab283b47…772ca2 relation_kernel.clj`;
  `cascade.clj` zero-diff. `env.clj` never read.
- Full diff read (18 files, +1,239/−133): every hunk inside its §6 mandate;
  no new namespace; no new import composer/module/depot/PState/topology
  (T3 census surface unchanged).

## G1 — suite + compile, independently re-run

Selection re-derived by this session per the machine-cut rule (grep of the
test tree for every edited file/namespace, then the slurp-scan subset): the
five changed namespaces PLUS the six adjacent pin-carriers
(`material_truth` V4 args pin · `face_arsenal` face-projection read-surface
scan · `provenance_material` · `reply_to_block` · `git_spine_gate`
(HEAD-dynamic, green at 785841d) · `face_integration`) + `episode_test`.

- **138 tests · 2,113 assertions · 0 failures · 0 errors · exit 0** — a
  strict superset of the implementer's 114/1,626, green first run.
- CLJS full dev compile (the pinned `clj -M:dev -m shadow.cljs.devtools.cli
  compile dev`): **272 files · 0 warnings · exit 0**. Honest note: shadow's
  content-hash cache was warm from the byte-identical tree (0 recompiled);
  the receipt is a same-bytes revalidation, not a cold compile.

## G2 — the one contract-critical live receipt (this session's own rig)

Isolated current-tree server booted `LAND_CLUSTER=0 LAND_PINNED=1` on
`:8098` (box verified idle first: no servers, no Rama daemons; in-process
runtimes — no shared/durable land state touched; rig torn down after). The
durable say lifecycle driven at the REAL HTTP boundary with curl:

- first say → 200 `:accepted`, import decision `:accepted` AND relation
  `:materialized` (both truths queryable before success), `:stratum :gold`,
  unit `du:chat:…:ep:decf22c8:000000`, relation
  `rel:a64eb54f…`, request `circulation:halo-say:rel:a64eb54f…`;
- exact-body replay → 200, SAME unit-id, SAME relation-id (one root + one
  edge per say-id);
- divergent target, same say-id → rejected
  `:idempotency/material-fingerprint-conflict`,
  `:relation-appended? false` — refused before any relation append;
- fresh say-id → distinct root/edge pair;
- GET → 405; every response parsed by a data-only EDN reader
  (record-free boundary live-confirmed);
- room open on the same rig → `:ok`, identical `object-key`
  `chat:569e…bb11`, entry `?drill=a983e774…`.

Mark honesty (`:halo/say` never `wish`) and room-block read-back are pinned
through the real `fp/serve` / `read-utterance-rows` in the suite run above
(`material_portal_test` composition test); the headed-browser halves
(condensation render, native-menu scope, live meta-row refusal) stand on the
implementer's attested G2 (SwiftShader attestation first, per the
scene-substrate rule) plus this gate's code-level verification of their
mechanisms.

## G3 — G10-machine

Taken as implementer-attested at slim tier: environment attested FIRST
(swiftshader/CPU fallback), warm n=62 p95=29.8ms < 52ms with the six-master
halo open, cold pass honestly recorded, one 94.8ms outlier retained. Not
re-run; the standing G10m harness is the cheap falsifier if doubt arises.

## G4 — falsifier standing

The fresh falsifier's first-FAIL (divergent-target retry reaching the depot
duplicate decision) and its smallest in-fence repair (composed-request
fingerprint + owner's durable-decision conflict predicate BEFORE either
append) are visible in the diff and its counterexamples are committed as
fixtures (`halo-say-replay-divergence-and-partial-repair…`,
`halo-meta-is-universally-reserved…`, `halo-kernel-scope…` incl. the two
SHA-256 composition pins). The live-found tagged-record EDN response bug's
repair (`record-free-edn`) carries a round-trip regression assertion and was
re-proven live by this gate.

## Trap spot-checks (full-diff read)

- **T1** universal reservation: `meta-gesture-reserved?` site-blind; read by
  material `valid-row?` AND all three camera lanes (V3 re-verified on disk:
  `space_material.cljc` master validation · `ground.cljs` console install
  refusal `:binding/meta-gesture-reserved` · served instance wear filter);
  source-scan test pins one-owner/three-consumers.
- **T2** derived-only: one pick → ordinary claim chain → floor row →
  `:halo/condense`; census from per-subject `wears-for` (six block wears /
  `fm:space`), never `:claim/facets`; labels from registry effect classes.
- **T3** one write path: say = existing `utterance-import-request` (one map
  arity) + existing private `append-asserted-edge!` via the new public
  `bank-reference!`; no new owner anywhere in the diff; eight-owner census
  pin green.
- **T4/T8** honest handles, never inert: handles render only per listed worn
  master; miss condenses the space; `dispatch-descriptor` on action-less
  hits is a verified total no-op (`::unregistered`); no double-dispatch with
  the trail-face lane (different key `:trail-face/click`, ground-routed
  first).
- **T5** identity: relation-id = f(kind, from-root(say-id), to, actor) —
  deterministic replay convergence proven live and in-suite; partial repair
  test ships (stubbed reference bank → `:incomplete` → exact replay
  repairs).
- **T6** scope/order: button 2 filtered from `mousedown`/`mouseup` before
  the pointer machine (no drag can begin); `preventDefault` only inside the
  synchronous `ground-active?` branch; source-pinned ordering test.
- **T7** heavy verbs: only ask/enter/preview/say descriptors exist;
  negative source pins for deviate/activate/rollback handles.
- **T9** legibility: drill 16 probes (4 meta, all floor); served table pins
  4 meta rows; gold-mark projection reads `:source-unit-id` + `:mark/type`,
  legacy wish rows keep `:wish-unit-id`; `bank-gold!` byte-untouched.
- **T10** visible widening: version note at `legal-gestures`; twelve frozen
  probe receipts byte-identical; `legal-gestures` exact-set re-cut.

## Non-blocking findings — each with its cheap falsifier

1. **The two SHA-256 zero-diff pins are now STANDING suite assertions**
   (`material_portal_test`): the next package legitimately touching
   `episode.clj` or `relation_kernel.clj` fails them by design. Falsifier/
   duty: that package re-cuts the two hash literals in the same change (the
   pinned-scan law), or relaxes them to substance pins at that touchpoint.
2. **`:relation-appended? true` is set even when `bank-reference!` refused
   pre-append** (`:runtime-unavailable`/`:invalid-reference` after a passed
   preflight — practically unreachable). Falsifier: one assertion on that
   envelope's honesty.
3. **`:replay?` reads false on an exact HTTP replay** (observed live; the
   awaited deduped decision doesn't carry the flag). Informational only —
   the ids prove convergence. Falsifier: assert the flag on a second
   identical call, or drop the field.
4. **Divergent-conflict HTTP status is 400** while sibling rejected acts
   return 422 (`matter-act-http-status` routes any non-nil error to 400).
   API-shape inconsistency only; no caller branches on it today.
5. **`halo-action!` ask-parse trusts the `question/ask` array shape**; a
   portal shape change degrades to "0 questions" silently. Falsifier: one
   client-side shape assertion at the seam.

## Evaluation notes (D-006 style)

- The one-phase cut held: one implementer context built H1–H8 whole; both
  real defects (T5 depot-race, EDN record boundary) were caught by the
  in-phase falsifier and the live drive — the layers the cadence ruling
  kept. Nothing this gate found rises above residue.
- The contract's plan-grade specificity (V1–V5, exact pins) is visibly what
  made the implementation converge: every V4 pin was re-cut exactly, no
  drift item reached this gate as a surprise (contrast rung-3's undisclosed
  eighth item).
- The disclosed fingerprint deviation (receipt hint excluded from the shared
  OC fingerprint → strengthen the composed request's fingerprint locally)
  is substance-faithful to the amended H5 and was the falsifier's own
  repair; correctly logged, not stopped — the manifest-substance law working
  as intended.

## Next

1. Sid's commit ruling: code commit (the 17 §6 paths, exact-path staging)
   and separate docs commit (`build/halo/` artifacts + board flip), never
   mixed, docs branch never pushed.
2. After the code commit lands: re-run the HEAD-dynamic suites
   (`git_spine_gate` + code-atoms) at committed HEAD — green pre-commit is
   not green at committed HEAD.
3. C2's clock arms at Sid's first wear session with the halo live; retro
   joins the stratum batch (2026-07-27 cadence ruling).
