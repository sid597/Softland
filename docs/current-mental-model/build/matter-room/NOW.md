# matter-room — thread file

## STANDING (frozen at package open 2026-07-27 — do not edit while active)

- **Binding docs:** `CONTRACT.md` (this package) · `build/editable-material/
  DIRECTION.md` (parent spine) · `docs/current-mental-model/decisions.md`.
  Precedence verbatim: **this file is a baton, not a source of truth; if it
  contradicts CONTRACT.md or decisions.md, those win — flag the discrepancy
  in NOW.**
- **Process:** one phase per fresh context under the work-package skill;
  P1 (matter address) → P2 (room) → P3 (hands/mouth) → P4 (citizens/gauges);
  plan + fresh default-fail plan-validation before P1 source; one fresh
  falsifier per phase aimed at the new machinery; slim gates (G partition in
  CONTRACT §6, sum-checked); phase artifacts in this directory with
  diff-derived changed-file lists.
- **Allowlist:** CONTRACT §11. NEVER `app/server/cascade.clj`, NEVER
  `binding_material.cljc` enums, NEVER `env.clj` (never read).
- **Verification duties before code:** T6 (room id UUID-shape law vs
  `episode.clj`'s minted-uuid spawn law) · P3 call sites into the P6 artery
  pinned from `activation_event.cljc` + `material_circulation.clj` at plan
  time · every memory-derived platform claim checked against on-disk
  references.
- **Definition of done:** G1–G10 green (tiers + owners per CONTRACT §8),
  receipts banked at capture time, board line flipped, residue note in the
  gate record; retro joins the stratum batch.
- **Stop clause:** CONTRACT §9. The implementing session never improvises
  policy on binding docs; manifests bind on substance, locator drift
  re-locates + logs.
- **Hard rules:** commits — never Co-Authored-By, in any form; code and docs
  in separate commits; docs only on the local docs branch, never pushed.
  Parallel-sessions git discipline (memory: exact-path staging, foreign-hunk
  check) applies to every commit.
- **Must not start without Sid:** nothing — building is covered by his
  2026-07-27 word; commit decisions per close remain his; veto anytime.

## NOW (newest first; ≤15 lines per entry)

- **2026-07-28 · Fable · P3 COMMIT RULING EXECUTED (Sid: commit both).**
  Code commit `620c021` = exactly the amended PLAN §P3 seven-source-plus-
  two-test set, staged path-exact on base `20aeca8`; diffstat re-matched
  GATE_P3 (9 files, +798/−21); every `ground.cljs` hunk re-read and
  recognized (the four inert registrations only) before staging. Docs
  commit = P3.md + GATE_P3.md + this file, separate per the never-mix
  law. HEAD-dynamic suites re-run at the new code HEAD before this
  entry: git-spine + code-atoms, 16t/365a, 0 failures 0 errors (docs
  commit touches no source byte). Foreign multi-cascade/decisions/
  board/LOG/probe write-set untouched; board block updated on disk
  only (shared file, stays uncommitted). Gate findings 1–3 remain
  non-blocking residue for the next contract. Next: P4 (citizens and
  gauges) per PLAN §P4 — fresh implementer context, G8/G9/G10-machine;
  halo contract session unblocked (its bank line opens it AT the P3
  gate, which has now passed).

- **2026-07-28 · Fable · P3 SLIM GATE PASS** — `GATE_P3.md`. Fence exact
  (amended 7+2 set; four inert ground registrations only; P6 owners +
  forbidden zero-diff; foreign set untouched; env.clj never read; empty
  index). Suite re-run 103t/1,600a; CLJS 272/0 warnings. G6/G7 suite +
  full-diff trap checks green; live re-driven: preview immobility (23,826
  bytes before==during==after a worn client preview), durable cycle
  accepted end-to-end on a bootstrapped isolated runtime, forged rollback
  refused. G10m attested first (headless Chrome 150, swiftshader/CPU —
  conservative): n=43, p95=18.0ms < 52. Findings, non-blocking with fixes
  named in the gate record: decision-path rejections serve a MUTE error
  card; unseeded-master wedge reachable via the new lane on dev boots
  (machinery pre-existing); one vacuous T9 test negative. Shared cluster
  never touched; rig torn down. Next: Sid's commit ruling (code + docs
  separate). P4 stays closed until a fresh context opens it.

- **2026-07-28 · Codex · P3 IMPLEMENTER COMPLETE — P3-F2 accepted.**
  Resumed at docs-only ruling HEAD `20aeca8`; source/test bytes were untouched.
  PLAN now permits exactly four inert `{:invoke (fn [_] nil)}` registrations
  in `ground.cljs`; durable/preview logic remains prohibited there.
  P3-F2 replay list EMPTY: G6/G7 durable+preview+live receipts stand;
  G10-machine 43/43, p95=16.3ms; fresh new-write/endpoint falsifier PASS;
  post-falsifier 103t/1,600a and CLJS 0 warnings stand on the legalized tree.
  Seven source + two test paths remain parked; P6 owners and foreign set
  untouched. Receipt `P3.md` is complete; nothing staged, committed, or pushed.
  Next: fresh independent G6/G7/G10-machine under §8, then Sid's commit ruling.
  P4 remains closed.

- **2026-07-28 · Fable · P3-F2 RULED — ground scope amended; replay list
  EMPTY; P3 may declare complete.** Verified on disk before ruling: the
  closed-vocabulary test (binding_dispatch_test "the verb registry is the
  only vocabulary the kernel registers") requires ground's register-verb!
  set == verb-registry/names EXACTLY; the parked hunk is four literal
  `{:invoke (fn [_] nil)}` no-ops naming the real lanes in its comment;
  PLAN's own Behavior text + G7 compel frozen-v1 accept-then-no-op.
  Ruling: PLAN §P3 ground scope now permits exactly those four inert
  registrations; durable/preview logic stays prohibited there; the
  equality invariant is NOT weakened — a declared verb without a kernel
  implementation is a lying registry, T9's honesty family. Receipts were
  captured on the exact tree this ruling legalizes → nothing re-runs.
  Next: implementer finalizes P3.md complete; fresh independent slim
  gate; then Sid's commit ruling. Substance-over-letter class, as F1.

- **2026-07-28 · Codex · P3-F2 STOP under CONTRACT §9 — Sid/Fable rules.**
  Behavior is green: G6/G7; G10m 43 echoes, p95=16.3ms; live durable acts;
  one fresh falsifier PASS; post-falsifier 103t/1,600a; CLJS 0 warnings.
  Final fence audit found a binding conflict: PLAN scopes `ground.cljs` to
  preview-lane touch only, while the existing closed-vocabulary test requires
  every new registry verb to have a literal `register-verb!` implementation.
  The four inert registrations make v1 accept-then-no-op and v2+ refuse as
  PLAN requires, but three registrations name durable verbs and exceed the
  literal preview-only parenthetical. Green hunk remains parked, unstaged.
  Recommend amend scope to exactly four inert registrations; do not weaken the
  registry/implementation equality invariant. Conditional P6 owners zero-diff;
  foreign write-set untouched; empty index; no commit/push. Receipt: `P3.md`.
  Do not gate or open P4 until P3-F2 ruling supplies the replay list.

- **2026-07-28 · Fable · P3-F1 RULED — PLAN §P3 fence amended; P3 resumes.**
  Implementer stopped pre-edit under CONTRACT §9: PLAN §P3's Files list
  omitted `shared/material_portal.cljc`, yet L5, §6-P3, T9, and G7 all
  compel the briefing's "read-only" sentence to change in P3 — T9/G7 say
  same phase, SAME COMMIT — and the sentence physically lives in that
  file (:874). §11 already allowlists it package-wide. Every cited
  clause re-verified on disk this session before ruling. Ruling: add the
  file to PLAN §P3 scoped SOLELY to the L5/T9 sentence + its tests;
  every other fence line unchanged. Same class as the 2026-07-27
  multi-cascade manifest ruling: fences bind on substance; a locator
  omission the contract contradicts four times is amended, never obeyed
  into a lying portal. decisions.md escalation entry deliberately
  skipped (parked foreign write-set); this entry is the record.
  Next: implementer resumes P3 under the amended fence, no re-orientation.

- **2026-07-28 · Fable · P2 slim gate PASS + commit ruling executed (Sid
  in-session: commit both) — P3 may open.**
  Adopted interrupted Opus WIP after full-diff read; restored both off-PLAN
  portal paths, then finished within PLAN §P2's seven code/test paths.
  Room residents: deterministic birth through the existing episode artery,
  monotone edit refresh, append-only trail, served entry, one-batch widened
  experience. G3/G4/G5 green; G10m n=43, p95=21.8ms (<52).
  One fresh falsifier found a real fail-open seq-read fallback: an old accepted
  edit replayed while new bytes were lost. Catch removed; exact attack now
  throws; concurrent-open + fingerprint attacks re-run PASS.
  Post-falsifier slim suite 82t/1,201a green; CLJS 0 warnings; diff-check clean.
  Durable-cluster one-shot accepted on inherited pre-shutdown receipt; current
  product path independently re-proved on isolated :8092; shared land untouched.
  No stage/commit/push; root probes removed. Receipts: P2.md + GATE_P2.md.
  Next: fresh P3 context per P2.md's starter.

- **2026-07-28 · Fable · P1 slim gate PASS + commit ruling executed (Sid
  in-session: "Its your call") — P2 opened.** Gate fresh-context vs the
  Codex implementation, slim tier per CONTRACT §8. Independently: suite
  re-run on the committed tree 18t/342a + compat 21t/276a green; entity pin
  `deb12d4d…`/22,064 held in the gate JVM; here-key sentinel enumeration
  machine-verified complete (9/9 tree-wide grep); room id re-derived from
  raw md5 (matches); full diff read, fence exact, nothing foreign staged.
  Cross-JVM anchor sha differs per boot — the retired claim behaving as
  documented; in-JVM equality held here. Residue in GATE_P1.md: kondo
  0-warnings needs the analysis cache; CLJS compile + G10 live accepted on
  receipt; G10 headed half at wear. Commits: code `7a3127c` · docs this
  one; board line edited on disk only (sibling write-set shares the board).
  Next: P2 per P1.md's starter — fresh implementer context, Fable
  orchestrating + gating; falsifier fresh; stop clause waits for Sid.

- **2026-07-28 · Codex · P1 PASS — uncommitted; Sid decides.**
  Exact PLAN §P1 code/test list only; sibling + late foreign LOG untouched.
  G1/G2: 18 tests, 342 assertions, 0 failures/errors; all 17 answers hold.
  Entity bytes stay pinned: `deb12d4d…`, 22,064 B, both-address equality.
  Final gate-JVM anchor SHA `706f9d9c…`; room `a983e774…`; explicit address.
  G10m live: n=36, p50 15.7, p95 23.6, max 35.6ms; 0 over 52ms.
  Fresh falsifier first FAILed a mixed-master wearer/blast leak; fixed by
  anchor-filtering once while separating omitted from supplied snapshots.
  Same one falsifier re-probed mixed/irrelevant inputs: PASS; entity pin holds.
  Compatibility 21t/276a green; lint 0/0; diff-check clean; CLJS 0 warnings.
  Non-green signals: unsupported `-n` harness form + test-fixture paren,
  neither executed a failing behavior test; substantive falsifier preserved.
  No stage/commit/push; full suite not attempted (contract slim tier).
  Receipt + P2 starter: `P1.md`; do not start P2 before Sid's commit ruling.

- **2026-07-27 · Fable · PLAN VALIDATED — P1 may open source (fresh
  session).** Same staging session, after Sid's "carry on here" (docs
  only; zero source touched). PLAN.md authored → R1 FAIL (18 findings,
  5 HIGH) → contract amended (11 sections) + plan reworked → R2 FAIL
  (13/18 confirmed; 12 new, 2 HIGH — both in R1 fixes: the gauge repin
  would have killed the §8 detector; resident birth missed the `role`
  slot machine classification reads) → all 12 fixed → R2.5 resolution
  check minor-fail (4 one-line sweeps) → applied per the minor-fail
  law, validation not re-run. Records: PLAN_VALIDATION_{R1,R2,}.md
  (final = round history + verdict VALIDATED). Judgment calls for the
  next reviewer: gauge v0 provenance-scoped (per-master = L7 re-cut
  LATER) · no release chains for extraction verbs (§5 amended) · room
  entry rides the drill-URL MVP. Next: fresh implementation session
  per phase — P1 starter in the session close + board block.

- **2026-07-27 · Fable · package STAGED.** Contract authored in the direction
  session that settled the shape (register flipped to build at Sid's word:
  "Cant you do it here since all the context is here????"). Ground truth
  read this session: portal pair, binding grammar, verb registry, cascade
  R1, reply-to-block seam, circulation (escape report :418,
  experience-around-many :1004), facet-masters registry — all manifest
  locators machine-verified. Key finds recorded in CONTRACT: the portal's
  read-only sentence + DIRECTION's met precondition (§1), the built escape
  detector (L7), the drill-UUID constraint (T6). Sibling note: multi-cascade
  R2's uncommitted write-set is parked on disk (decisions.md, board, R2
  files) — this session committed ONLY its own new files + the LOG append;
  board edits left uncommitted beside the sibling's per the git memory.
  Next: fresh-context PLAN (P1–P4 against the contract) → fresh default-fail
  plan validation → P1. Starter prompt in the session close.
