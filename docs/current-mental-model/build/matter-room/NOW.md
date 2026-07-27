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
