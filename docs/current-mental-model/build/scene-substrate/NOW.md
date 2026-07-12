# scene-substrate — thread file

## STANDING (frozen at open, 2026-07-12)

- Binding docs: `decisions.md` > `CONTRACT.md` (this package) > this file.
  This file is a baton, not a source of truth; if it contradicts
  CONTRACT.md or decisions.md, those win — flag the discrepancy in NOW.
- Package: birth the scene substrate (store + container transforms + pick
  seam + actions router) per CONTRACT v1. Phases P1–P4; P5+ staged.
- New-files allowlist: `scene_store.cljc`, `containers.cljc`, their tests,
  the P2 dev probe (tagged, delete-to-remove). Renderer/shader edits in P2
  only; face-path edits in P3 only. Everything else out of scope.
- Coordination: block-write + machine-cut edits sit UNCOMMITTED in
  `runtime/*`, `electric_flow.cljc`, `face_projection.clj`,
  `block_distiller.clj`. Keep P2/P3 diffs additive beside them; commits
  are coordinated by the machine-cut close session + Sid (board ⚠ T11).
- Verification duties: platform claims (WGSL struct alignment, stride,
  storage-buffer limits) checked against renderer.cljs + a live probe
  before code lands, not from memory.
- Definition of done: G1–G10 green + G11 worn by Sid + falsification pass
  + gate review + retro.
- Stop clauses: CONTRACT §9. Never patch around a wall; re-derive.
- Hard rules: docs commits on the local docs branch as-you-go; code and
  docs never in one commit; code commit timing is Sid's call; never read
  `env.clj`.
- Waits on Sid: nothing to start. Naming pass on "scene-substrate"
  (cheap re-rule), G11 wearing when P3 lands.

## NOW (append per session, ≤15 lines each)

**2026-07-12 · Fable · derivation + contract + P1 dispatch/P2 start**
- Derived the base layer from first principles against the record:
  `DERIVATION.md` — five unlocks collapse onto four capabilities, one
  organ (the scene substrate); server floor already exists; test table
  passes with two genuinely-later items, both Sid-gated.
- Ruled into settled ground (decisions.md "One render substrate" entry
  amended in place; supersedes separate container-transforms /
  point-and-say / scene-diff packages — folded as legs/consumers).
- CONTRACT v1 written, in force. Board flipped (FOREST → base line).
- P1 (store + containers, pure cljc + JVM tests) dispatched to a
  fresh-context Opus subagent under contract. P2 (GPU transform leg)
  started by Fable directly per §11.
- Next: land P1+P2 → ONE falsification finder over the wave → P3.
