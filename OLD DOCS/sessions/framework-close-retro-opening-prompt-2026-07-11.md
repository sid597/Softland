# Opening prompt — framework package CLOSE (retro + adversarial recheck)

Paste into a FRESH session (Opus is right for this — the retro is written
from the trail, per the code-atom close precedent; Fable is not needed).
Session name suggestion: `framework-close-retro`.

---

You are closing the Softland **framework (faces-as-assemblies)** work
package per the `/work-package` skill's close protocol — boot that skill
FIRST and follow its "Closing a package" section exactly. Both waves are
CLOSED and gate-passed; commits are in (`c84ebfa` W1, `1725f55` W2 code,
docs on the local docs branch). Your job is steps 3–5: the RETRO, its
ADVERSARIAL RECHECK (a fresh subagent, default-fail), and routing the
lessons.

**The trail (read in this order):**
`docs/current-mental-model/build/framework/NOW.md` (the full per-session
log) · `CONTRACT.md` (v2; note the dated in-place amendments — G12
read-source, §17 idempotency, G20 stamp carve-out, §8 T11 carve-out) ·
`PROBE.md` · lane artifacts `W1-A/B/C`, `W2-D/E` · `W1-INT.md`/`W1-GATE.md` ·
`W2-INT.md`/`W2-GATE.md` · the source itself (the code is ground truth —
spot-verify scorecard claims against it, never trust the artifacts alone).

**Retro must cover (skill's list, plus these package-specific seams):**
- QC-layer scorecard: what each layer caught vs missed — note especially:
  the LIVE WEARING found real defects in BOTH waves (W1 epoch-bump, W2
  projection routing) that no suite layer could see; the falsification wave
  found a CONTRACT-TEXT error (§17 idempotency — the predicted
  every-Fable-contract-carries-one class); the W2-E/W2-D lanes ran fully
  parallel with disjoint fences and zero collisions.
- What the next contract does differently — each rule traceable to a
  concrete failure HERE (candidates: face→projection resolution should have
  been contract-specified, not left static; rasterizer/driver op-shape
  assumptions; the name-vs-stem identity lesson; the delay-totality class).
- Mechanisms that earned their keep (the wearing as a gate layer; the
  roster-as-registry resolution; WAL-first ordering).
- Residue for the NEXT contract: the name-conflict row field (falsifier:
  two files one name → flap) · pair structure = the machine-cut evidence ·
  reader focus control (:actions-era) · paging (block-kernel §10) ·
  dev-boot rk attach · fsync boundary · post-wave harmonization
  (ui_primitives rename, CONTRACT §2's pre-named extension point).
- D-006 evaluation notes → decisions.md (the pre-registered criteria; note
  the in-session-fixes-at-gate pattern under Sid's token flag).

**Then:** adversarial recheck by a FRESH subagent (verify every scorecard
claim against artifacts + git + a fresh suite run: the serial suite is 8
namespaces, 46t/804a — command shape in W2-GATE.md); route lessons
(implementation-quirks / work-package skill amendments as PROPOSED for
Fable's signature / decisions.md); then PRUNE board thread 8 to a one-line
done-pointer per the board charter. Commit docs as you go (docs branch,
never push). Hard rules: never read `src/app/server/env.clj`; code and docs
in separate commits (you should need no code commits).