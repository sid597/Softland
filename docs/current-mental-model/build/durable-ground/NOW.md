# durable-ground — thread file

## STANDING (frozen at open, 2026-07-17)

- Binding docs: `CONTRACT.md` (this package) + `decisions.md`. This file is a
  baton, not a source of truth; if it contradicts CONTRACT.md or decisions.md,
  those win — flag the discrepancy here, do not pause.
- Process: work-package skill (2026-07-13 amendment level) + /rama skill ops
  references. One phase per fresh CONTEXT; Fable orchestrates and implements
  directly (Sid 2026-07-17: "get into work mode... first light asap").
- Scope guard: NEW files only in `app.server.rama.cluster` ns + `bin/land` +
  `ops/`; edits to existing src limited to the boot-flag seam in
  `file_viewer.cljc` (T6) and WAL-path config removal at P4. No new Rama
  organs. IPC constructors untouched.
- Verification duties: every platform claim checks against
  `.claude/skills/rama/references/operate.md` / `docs/reference/rama/*` on
  disk before code; memory-derived claims are marked and verified.
- Definition of done: CONTRACT §7 G1–G6 green + gate review + retro; the dev
  boot defaults to the durable cluster; acked writes survive kill-9 and
  machine reboot with named-process receipts; ingest explicit; WAL writes
  off; backup recipe exercised.
- Stop clauses: CONTRACT §8. Hard rules: never read `env.clj`; code and docs
  in separate commits; docs only on the local docs branch (never push);
  no Co-Authored-By lines, ever.
- Must NOT start without Sid: machine-reboot drill (his act) · any
  system-package install · any spend.

## NOW (append per session; ~15 lines each)

- **2026-07-17 · Fable · P0 (inventory + environment) — DONE.**
  Facts: Java 21 ✓ (Rama supports 8/11/17/21) · 62GB RAM / 24 cores / 286GB
  free on /mnt/data · no existing rama install. Module inventory → CONTRACT
  §4 (five modules; dogfood excluded). Finding worth flagging: the product
  boot runs TWO IPC clusters with two disjoint object-container instances
  (trail_view.clj:779-788 vs object_container/runtime.clj:13-18) → CONTRACT
  §3 unification ruling (one cluster, one OC deployment, both handle bundles
  foreign). License question answered before open (free ≤2 supervisor
  nodes; RPL 2025-03-18). Backups honesty pinned: free tier = COLD backup
  procedure (22-backups.md:124-134), scripted at P6; crash/reboot recovery
  is native and continuous regardless. One TBD for P2: where file_viewer
  wires face-arsenal handles (attach point named in §4 table).
  Next: P1 — download release 1.6.x, `ops/rama.yaml`, daemons up, G1.
