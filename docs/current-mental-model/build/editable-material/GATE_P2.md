# P2 gate — the material inspector — PASS

2026-07-24 · Fable orchestration session (same session as the P1 gate; the
fresh-context law is satisfied because Codex authored P2 elsewhere — this
context judged, never wrote, the package). Full diff read end-to-end (6
modified files + 2 new, ~740 lines). Code commit `c6bb5d8`.

## Independent receipts (all re-run/driven this session)

- Full suite, all 45 `*_test.clj` namespaces: **385 tests / 5,220
  assertions / 0 failures / 0 errors** — matches Codex's claim exactly and
  is FULLY green (the kernel-shape repair holds; no flake fired), achieved
  under heavy concurrent load (cluster recovery + dev app + browser replay).
- CLJS dev build at boot: **302 files, 0 warnings**.
- **Live-cluster replay — the step Codex was blocked on (cluster down at
  their turn; root cause: HOST REBOOT ~19:21, not any session's doing).**
  This session recovered the cluster (`bin/land up`, normal supervisor
  recovery, no unwedge needed — crash-consistent class), booted the dev app,
  and drove the console API headless (puppeteer, framework-W1 precedent):
  - `__material` installs; `wearers()` = **34 wearers** (34 fold-header /
    34 machine-rail / 1 episode-boundary), all wearing the v0 default
    revision `…dc857…` — exactly what P1's rollback left active.
  - `inspect(<real sense-block id>)`: entity found (`prose-para`), master
    `fm:provenance`, **active ≠ latest live** — latest is `…26c3d…`, the
    malformed drill candidate P1 retained, byte-identical to NOW.md's
    record, having survived an unplanned host reboot (a free re-receipt of
    P1's restart drill).
  - Trail: **3 candidates / 2 activations / 1 rollback**, complete — the
    exact durable P1 history. Attachment honestly `:derived`,
    `wears-active? true`.
  - **`edn()` twice → byte-equal (`===`), 22,197 bytes, live.**
  - `picked()` honestly null headless (no click); the pick-capture line is
    Sid's one headed click: click a block → `__material.picked()`.

## Falsification pass — held

- **Dispatch:** `:material-inspector` rides `resolve-projection-kind`'s
  fall-through to the face keyword → registry hit — same pattern as P1's
  `:provenance-material`. Verified in source; the test drives the real
  `serve` path (G13 precedent).
- **Read-only fence:** projection calls exactly `read-unit` +
  `read-master` + 2× bounded `read-revision-history` (limit = OC page size
  1000, with an honest `revision-trail-complete?` flag — the no-silent-caps
  rule). No append/transform anywhere in the diff. The test proves
  before/after equality with PHYSICAL history reads (the
  negative-invariant-needs-a-physical-reader rule).
- **Fixed read cost:** wearers travel IN the request from the client scene
  snapshot; server never reads per-wearer. ~6 foreign reads total,
  wearer-count-independent.
- **Determinism:** no wall clock in the result (unlike P1's projection —
  deliberately); canonicalize = pr-str-sorted maps / sorted sets / vectors;
  records reduced to maps; transport token excluded from result AND edn.
  Proven by test (two tokens byte-equal) and live (`===` at 22KB).
- **Honest authority:** the wearer/attachment basis is labeled
  `:derived` / `:current-client-scene` everywhere — the Horizon fence
  (durable wear index) untouched, and the seam P6/P7 will formalize is
  named instead of faked. This was P2's best judgment call.
- **Pinned-scan discipline:** the g21 read-surface scan on
  `face_projection.clj` widened by exactly `ocr/read-revision-history` in
  the same change; the forbidden-write half untouched.
- **Client machinery:** request armed ONLY on console call (zero ambient
  cost); inspector atoms are console-lane only — ground never watches them;
  timeout + watch cleanup correct; concurrent `inspect()` calls degrade
  console-grade (second wins, first times out) — acceptable, noted.

## Adjudications

- **Live replay obligation (layer-5 wearing):** Codex stopped honestly at
  the dead cluster (correct — restarting would have made P2 an ops
  package). The gate ran the wearing itself; all replay receipts above are
  live-cluster. Remaining Sid-half: one headed click → `picked()` — folds
  into his next session, no gate hold.
- **Scene-derived wearers vs the prompt's "current wearers":** ruled
  CORRECT reading of the fences (a durable index = new durable truth =
  §Horizon; DIRECTION assigns wear indexes to the material-truth owner,
  P6's decision). The honest labels make the projection safe to consume
  without laundering.
- **No face built:** correct — CAMPAIGN says console-grade first, face
  "only if genuinely cheap"; the console surface answers every P2 question.

## Open doubts (non-blocking, falsifiers named)

- **Arsenal-roster shadowing:** a face NAMED "material-inspector" (or
  "provenance-material") registered in the arsenal would shadow the static
  registry route to `:conversation`. Same exposure as P1; falsifier: one
  registry-collision assertion when P7 names faces, or simply avoid the
  reserved names.
- **Trail classification is derived:** rollback = pointer target seen
  before ∧ ≠ previous — correct for P1-shaped histories (proven on the
  real one); when P6 mints real activation EVENTS with declared kinds, the
  inspector should switch to event truth (falsifier: P6 review checks the
  inspector's trail against the event log).

## Ops record (this session, sanctioned paths only)

Host rebooted ~19:21 (uptime evidence; worker logs end 14:52) → cluster
down, dev app down, /tmp scratchpads wiped (numbers were banked — no
loss). Recovery: `bin/land up` → conductor "started successfully", five
workers, UI 200 — NO unwedge needed (plain kill of a RUNNING cluster
recovers natively; the wedge class needs an interrupted DRAIN). Dev app
rebooted, land left UP with the dev app running for Sid's return.
