# No active work package

Queue item 1 (work-package succession skill) DONE 2026-07-03, Fable session:

1. **Adversarial recheck of `build/relation-kernel/RETRO.md` ran FIRST** (as
   queued, xhigh): every scorecard/narrative claim traced to the 7 phase
   artifacts, the baton trail in git (per-phase commits on this file), the
   module + test source, and a fresh suite run on the committed code
   (`clojure -M:test` → 2 tests, 165 assertions, 0 failures). Verdict: retro
   substantially sound. Recorded in RETRO.md's "Adversarial recheck" addendum:
   4 corrections (stale "decisions.md not in git" residue; lesson-1 cost
   overcount; lesson-3 wrong causal story — read-plan clauses are hygiene, not
   a Phase-2 mover, since promises in binding docs don't self-enforce;
   lesson-2 slogan precision — partitioner-read key on a plain map envelope,
   typed records may ride inside), 2 new lessons (8: never overwrite a FAIL
   validation artifact — both PLAN_VALIDATION FAIL rounds are unrecoverable,
   only the baton saved the findings; 9: a ruling amendment is a sweep, not a
   banner — IMPLICIT_SPEC still says "ten gates"), 1 residue addition
   (importer-timestamp discipline must enter the D-003 extractor / trail-view
   contract).
2. **Skill written**: `.claude/skills/work-package/SKILL.md` — all five spec
   items from decisions.md (five-layer QC model / opening a package / gate
   review without Fable / re-entry triggers / retro procedure + the
   pre-registered retro-recheck step), mechanisms kept verbatim, lessons
   encoded in recheck-corrected form. decisions.md spec bullet marked
   EXECUTED; queue line struck.

COMMITTED 2026-07-04 (Sid's go; this branch only — never push, never merge to
main): three docs commits — (1) relation-kernel retro recheck + work-package
skill (skill force-added past the `.claude/` ignore rule, consistent with the
other already-tracked skills), (2) vision log (HCI thesis + April–June
back-fill + replies) + notebook photos + BETS H3/H5 updates + D-007 PROPOSED,
(3) trail-view INPUTS.md + this baton. Code files untouched across these
sessions (suite re-run only). Deliberately left uncommitted (not ours / not
docs): `.gitignore` (+external-resources/), `.agents/.../openai.yaml`,
`codex_implementation`, `scripts/`, `src-build/build/`.

## Queue (Sid sequences; per decisions.md Fable-window queue)

1. **Trail-view data contract** (next Fable-window item) — the contract +
   acceptance gates for D-002's first form, consuming the relation kernel via
   its two query topologies. Open it per the new `/work-package` skill, in a
   FRESH session. **INPUT MANIFEST: `build/trail-view/INPUTS.md`** (gathered
   2026-07-03/04: Sid-decided items incl. bundle=everything-layered,
   staleness-renders-differently, minimal-token agent legibility,
   view-specs-as-data; carried relation-kernel obligations; Fable-proposed
   verdict-capture + select/type/sign gesture). Prior trail-view/design docs
   are input NOT authority (Sid 2026-07-04: "the prev docs were written by
   lower reasoning fables") — ground truth is decisions.md + code + INPUTS.md.
2. **D-003 Regime-1 spine** (needs Sid's go): transcript→commit/doc join
   extractor + git-commit-metadata adapter emitting `:produced`/`:based-on`
   assertions with evidence anchors, feeding the 27-04 trail view.
3. Slot-anywhere, cheap-model: D-006 criterion-2 counterfactual probe (fresh
   Opus, input manifest = CONTRACT §13); `:workers 2` smoke test of the
   relation kernel; make the test-harness task count injectable (not only
   `rand-nth`-randomized) whenever the harness is next touched, so partition
   sweeps are reproducible.

Added 2026-07-03 (bet-foundry session, same day):

4. **D-007 awaits Sid's countersign** (decisions.md): the bet foundry —
   claims→bets intake via the BETS.md Candidates inbox + the Fable questioning
   practice (1–3 frontier questions per vision/bets-touching Fable session).
   Sid's verbatim proposal + HCI thesis landed in `vision/LOG.md` (2026-07-03
   entry).
5. **First BETS.md intake sitting** (Fable-grade, Sid live): seed the empty
   Candidates inbox. **PRE-READ: `docs/current-mental-model/intake/
   2026-07-04-painting.md`** — the full typed decomposition of the 07-03/04
   sessions (12 threads, parallelism map, carried questions). Sources:
   `vision/LOG.md` (HCI thesis, economy vision, April–June research notes)
   plus the wall panels. Status of the three posed frontier questions:
   Q1 (beachhead) ANSWERED — H3 re-sequenced in BETS.md 2026-07-04; Q3 (unit
   that leaves the chatbox) ANSWERED — "everything, raw + native + view-forward"
   (feeds the trail-view contract); Q2 (minimum viable discipline for a solo
   operator) still OPEN. New question posed 2026-07-04: what is the
   commit-gesture for thought (what mints a semantic version of an
   exploration)? — feeds the trail-view contract directly.
6. **Vision back-fill** — PROGRESSED 2026-07-04: April 28 / June 6 / June 11 /
   June 12 raw notes + the ~June 28 notebook pages landed in `vision/LOG.md`
   (photos preserved at `vision/images/2026-06-28-notebook-p{1,2}.png`).
   More may remain in Sid's notes apps; pull in under original dates when he
   supplies them.

Added 2026-07-04 (deep-thinking thread):

7. **Question-unit design** (Fable window; Sid: "lets make it a dedicated
   thing... have a solution for it"): the atomic unit of a question —
   hole-shaped relation rows (`? based-on B`), announcement-at-pattern-match,
   frontier = high-density holes. Sequencing: the trail-view contract only
   keeps the door open (INPUTS.md item 5); the dedicated design slot comes
   after/alongside that contract, build paced by D-005 (the view orders it).
   **D-001 evidence acquired 2026-07-04**: the decisions.md open-questions
   wall broke against Sid in daily use (verbatim in LOG; requirement recorded
   in INPUTS.md item 5) — this item now has a real form-break behind it, not
   an imagined demand. Interim: open questions go to Sid as Roam cards.
8. **Model-UXR benchmark design** ("user research for models", H3 machine
   half — BETS.md H3 2026-07-04 sharpening): frozen land snapshot +
   orientation-question bank (gold answers derivable only from the trail) +
   permission-scoped ablations (drop decisions.md / baton / relations, measure
   degradation). Metrics: tokens-to-orientation, wrong-authority rate,
   re-derivation ratio, join-question success, invented-structure rate.
   Cheap-model buildable once the trail view's queries exist; reruns as CI.
9. **Interim working protocol** (proposed by Fable 2026-07-04 after the
   form-break thread; zero-build relief, effective unless Sid objects):
   (a) thread-scoped sessions, not 400k monoliths — one painting-thread per
   session, Fable carries orientation via this baton + the intake pre-reads;
   (b) Sid engages decisions/questions ONLY via Roam cards — markdown stays
   the machine-facing record, maintained by models; (c) every session ends
   with an assertion-grade baton entry (the Q2 rule — **ADOPTED by Sid
   2026-07-04** via Roam card "yes adopt this"; also in decisions.md and
   Claude memory). Face order also RULED 2026-07-04 on Sid's delegation:
   View 3 → threaded/DAG timeline → canvas (decisions.md Open Questions).

Docs tracking (Sid, 2026-07-03): full docs tree tracked on this branch
(`84b3d82`), including `decisions.md`; docs commits ONLY on this local branch,
never pushed, never merged into main; code and docs always in separate
commits.

Session hygiene for whoever writes next: `memory/implementation-quirks.md` →
"NUL bytes in source" (the tool-JSON NUL-escape trap fired twice MORE in the
recheck session, both times while writing docs ABOUT the escape — file(1)-check
anything that mentions it) and "Microbatch test barrier" (reuse for every
kernel suite).
