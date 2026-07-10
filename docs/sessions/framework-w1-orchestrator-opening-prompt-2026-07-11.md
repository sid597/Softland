# Opening prompt — framework Wave 1 ORCHESTRATOR (Fable session)

**Wave word: GIVEN (Sid, 2026-07-11 — recorded in CONTRACT header + NOW).**
Model: Fable — this session dispatches the lanes, runs W1-INT itself (Fable
may implement directly per the 2026-07-05 (e2) ruling), and holds the gate.
Lanes run as fresh Opus 4.8 subagent contexts (the /work-package
one-orchestrating-session shape; subagent model policy: judgment stays here).

## Boot (in this order; nothing else preloaded)

1. `docs/current-mental-model/build/framework/NOW.md` — STANDING (fences,
   hard rules, precedence rule) + the NOW tail.
2. `docs/current-mental-model/build/framework/CONTRACT.md` — **v1.1,
   BINDING.** §2 fences · §5–§7 the specs · §9 traps (lanes cite trap
   numbers in code comments) · §10 finalized PROBE slots · §11 gates ·
   §14 handoff + stop clauses.
3. `docs/sessions/framework-w1-lane-prompts-2026-07-11.md` — the three lane
   prompts (each self-contained: common boot + lane §) + the W1-INT spec.
4. `docs/current-mental-model/build/framework/PROBE.md` — the numbers; the
   probe walker (`src/app/client/workspace/face_probe.cljc` + test) sits
   UNCOMMITTED in the tree as lane A's harvest candidate.

## Sequence

1. **Cost visibility first** (standing rule): state the agent count and a
   rough token estimate for the wave (3 lane subagents + falsification
   subagents at the end) before dispatching. The spend is approved; the
   statement is still owed.
2. **Dispatch lanes A · B · C in parallel** — three fresh Opus 4.8 subagent
   contexts. Each lane's instruction: read
   `docs/sessions/framework-w1-lane-prompts-2026-07-11.md` (common boot +
   YOUR lane section) and execute it; fences are disjoint by construction
   (CONTRACT §2 — lane C solely owns `electric_flow.cljc` +
   `file_viewer.cljc`; everyone else new-files-only; shared runtime files
   belong to W1-INT, not to any lane). Definition of done per lane: its
   CONTRACT §11 gates green in-context + a ≤15-line NOW entry appended to
   `build/framework/NOW.md` + artifact paths reported back. Lane A
   additionally records harvest-or-discard of the probe walker (and leaves
   nothing uncommitted-and-orphaned either way).
3. **W1-INT (this session, after all three lanes return):** per the lane
   prompts' W1-INT section — wire the registry, mount the pane through
   `<world-snapshot` (CONTRACT §5 touch-list + flow discipline: value-compare
   scene cache, trap T9/T13; camera scroll + `:assembly/content-h`), connect
   the artery, run G14 (both instances), G15 (the wearing: real `7c80ce2a`
   conversation through the Outline face, command entry, `:until-ms` scrub,
   screenshot for Sid, every exposed lack LOGGED as ordered data work), and
   re-verify the §10 SLOT-A number in the browser (record, don't gate).
4. **G16 end-gate:** full serial suite → ONE batched falsification-by-class
   (fresh Opus subagents, class-hunting not instance-hunting) → the Fable
   gate in THIS context (CLAUDE.md falsification protocol; re-run the suite
   independently; spot-check contract-named traps in the diff). Fixes
   applied at gate per current practice.
5. **Close the wave:** code-only commits (NEVER mixed with docs; docs-only
   commits as-you-go on this branch; never push/merge), INT artifact +
   gate artifact in `build/framework/`, ≤15-line NOW entry, board line
   flipped (`docs/sessions/next-prompt.md` thread 8). Wave 2 does NOT start.

## Hard lines

- Stop clauses per CONTRACT §14 — a genuine binding-doc conflict escalates
  to `decisions.md` Open Questions with options + recommendation; NEVER
  improvised in-lane or in-session.
- NOT in this wave (CONTRACT §14): D-011/D-012 countersign (still pending
  Sid — the wave word did not cover them); `pairs-with` or any relation-kind
  addition; any Wave-2 work (watcher, kernel objects, wearing log,
  `imp:asm:`); anything on main; `src/app/server/env.clj` is never read.
- Dev app: `clj -A:dev -X dev/-main` — never standalone shadow-cljs.
- Exit = Step-2 exit: a real past conversation rendered through the Outline
  face, replay/scrub working, screenshot in the INT artifact.
