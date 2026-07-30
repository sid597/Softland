# smalltalk-ui-vm — thread

## STANDING (frozen at package open, 2026-07-30)

Binding docs: `CONTRACT.md` (this directory) + `DIRECTION.md` (settled
2026-07-30) + `decisions.md`. This file is a baton, not a source of
truth; if it contradicts CONTRACT.md or decisions.md, those win — flag
the discrepancy here, do not pause. Two phases (P1 engine cutover FULL
tier · P2 Workshop slim tier), each: fresh implementer context builds
whole → fresh in-phase falsifier → fresh gate → Sid's commit ruling.
Before P1: ONE fresh default-fail validation round over CONTRACT.md
(V1–V6 + coherence). Allowlists per CONTRACT §6; stop clauses §9 —
never improvise policy; manifest binds on SUBSTANCE. Hard rules: commit
decisions are Sid's · code/docs separate commits · docs branch never
pushed · never Co-Authored-By · `env.clj` NEVER read. Does not start
without Sid: nothing (standing 07-24 word covers campaign-class code
commits; veto anytime).

## NOW (newest first; ~15-line budget per entry)

**2026-07-30 · Fable (fresh session) · validation round → MINOR-FAIL,
fixes applied, no re-run.** Artifact: `VALIDATION.md` (walk + receipts +
the P1 starter). Code tree verified byte-identical to the manifest cut
(`git diff db8e21d..HEAD -- src/ test/ bin/` empty). Findings, verbatim
headlines: **F1** W1 had no instance-data bind form — `[:view key]`
added with a closed view-key vocabulary (data-resolution law); **F2** G2
data-exactness was unsatisfiable through the machinery — equality
relation named, interpreter-provenance keys normalized, ids exact;
**F3** non-generic facet surfaces are FOUR, not three —
`block-wear-census` ground.cljs:347 gains `:anatomy`; **F4** render-sig
is 16-element; **F5** corpus surface named (`__ground.blocks()`
post-merge + committed EDN goldens, JVM harness); **F6** `worn-five`
recipe set disturbed — P2 rules it; **F7** V6 receipt: CLI 2.1.220 has
`--model` AND `--effort low..max` — stop-clause (c) positive; **F8**
"grammar-v2" = second version key. Judgment call flagged for the gate:
F1/F2 sat at the minor/substantive line — ruled minor because no file
set, gate substance, phase, or §0 ruling moved. V1 walk found NO §9(a)
convention blocker (layout-children passes unpositioned nodes through —
the block's explicit-geometry pattern survives the engine). Next: fresh
implementer builds P1 whole; starter at VALIDATION.md tail.

**2026-07-30 · Fable (orchestrating session) · contract cut.**
DIRECTION settled + committed `25ce803` earlier this session; CONTRACT
authored after four fresh terrain sweeps (render path · material
machinery · write lanes/halo · test surface), all pins machine-verified
at HEAD `25ce803`. Terrain rulings banked in CONTRACT §0: the assembly
machinery (compile/apply/registry) IS the interpreter — the block
becomes its next wearer; anatomy rides OC as `fm:anatomy` (no new
module); the loop needs ZERO new endpoints and ZERO new verbs; the
halo's enter is the Workshop door; type-preview = master-candidate
preview on the membrane. Next act: fresh default-fail validation round
over CONTRACT.md (V1–V6). Starter prompt banked on the board.
