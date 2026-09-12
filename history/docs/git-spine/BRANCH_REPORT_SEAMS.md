# BRANCH REPORT — read-only backend seams · Trunk-4 / t4-spine · 2026-07-06

Four named seams from the trunk prompt, worked in order. Fence held: server
side only (git_spine.clj, trail_view.clj feed assembly, ingest_watchers.clj,
their test namespaces) + the fence's sanctioned additive-field carve-out into
the OC kernel (one record field + its two positional constructor sites). NO
trail_face/*, NO workspace files, NO commits, env.clj never read.

**Receipts (all run this session, one JVM per line):**
- `app.server.rama.git-spine-test` — 7 tests / 167 assertions / 0 failures
  (was 6/~130; adds the seam-1/2/3 assertions + 1 new deftest).
- `trail-view-test + object-container-test + ingest-watchers-test +
  git-spine-gate-test` (one JVM, serial) — 13 tests / 295 assertions / 0
  failures.
- `relation-assert-route-test` (G8 pair, contract-gate assurance) — 7 tests /
  44 assertions / 0 failures.

---

## Seam 1 — claimed-ms on source-ingested feed rows: **DONE**

**Was:** commits carry a claimed clock (committed-at rides the import
request), but the feed's `source-ingested` rows hard-coded
`:time/claimed-ms nil` — MEASUREMENT_RAF's "claimed unknown on every commit
card".

**Why `:request/time-ms` could NOT be the carrier:** `core/action-request`
defaults `:time-ms` to `(now-ms)` when absent (core.clj:174). Reading it as
claimed would stamp every watcher md ingest's ARRIVAL wall clock as a claim —
md must stay claimed-nil-honest (trail-view CONTRACT §6). So the claim is
**declared explicitly**: a new optional request key `:claimed/at-ms`, set only
by a builder that genuinely has a material-claimed clock.

**The plumb (additive end to end, no row shape broken):**
1. `git_spine.clj / commit->import-request` — assocs
   `:claimed/at-ms (:committed-at-ms commit)` onto the built request
   (deterministic; G1 request-equality still green).
2. `object_container.clj / SourceIngestCompletionRow` — additive trailing
   field `claimed-at-ms`; the single live write site
   (`source-ingest-completion-row`, used by the import-material topology at
   the one `$$source-ingest-completions-by-ref` transform) copies
   `(:claimed/at-ms request)`. Old rows / md requests read nil.
3. `markdown_adapter.clj:227` — the OTHER positional constructor call (inside
   `source-materialization`, runs on every md request build) gains the same
   arg; without it the record's new arity would throw on every import. Its
   row is dead (accessor `materialization-completion-row` has zero consumers
   — grep-verified) but must compile-and-run.
4. `trail_view.clj / source-activity-entry` —
   `:time/claimed-ms (:claimed-at-ms row)`.

**Proof:** git-spine-test `t4-spine seam 1` blocks — every fixture commit's
feed entry carries claimed = its committed-at-ms (2026-01-01 fixture clock)
while arrival stays the wall clock, and they differ; g5 asserts the md
`note.md` entry stays claimed-nil. trail-view-test G9 feed test extended with
the same pair at the feed-assembly seam (request built with
`:claimed/at-ms`, md request without). Convergent re-import discipline holds:
same sha → same request incl. the claimed key; a re-request short-circuits on
the prior audit decision and never rewrites the row (same value anyway).

**rama-pitfalls verdict for the kernel field** (protocol run pre-edit): no
section fails — same event, same single write site, same owner topology,
value deterministic from the depot record (retry-idempotent), no routing /
fingerprint participation (`hash-by :partition/key` untouched; the material
fingerprint is a fixed literal map that never sees top-level request keys),
no new side effects/acks/proxies, and the ephemeral per-JVM IPC means no
old-format rows ever coexist with the new record class.

## Seam 2 — dangling doc edges / dual working-dir: **REAL, FIXED**

**Diagnosis against live data (2026-07-06):**
- `/home/sid/projects/Softland` is a **symlink** to
  `/mnt/data/projects/Softland` (readlink + same dev/inode — verified).
- Live corpus (~2,204 jsonl files, 1.6 GB): **1,089** doc-land
  `"file_path"` occurrences under `/mnt/data/...` vs **exactly 1** under
  `/home/sid/...` (session `00f57865-…` in `-mnt-data-projects-Softland/`).
  Every recorded session cwd is `/mnt/data`-rooted.
- The extractor keyed `doc-document-id` on the RAW transcript path string,
  while the watcher ingests docs under source-refs textual to the boot cwd
  (`user.dir`). So: (a) the one alias-recorded edit minted a doc id **no
  ingested doc ever matches** — a dangling edge whose target IS present, i.e.
  a DISHONEST dangler, at any boot; (b) the symmetric exposure was worse — a
  boot from the symlink cwd would have made **all 1,089** `/mnt`-recorded doc
  joins dangle.

**Fix:** `canonical-under-roots` now **rebases** into the watcher's textual
form — the file must still exist and canonicalize under a land root (G6
unchanged), and the returned path is the matching cfg root's TEXTUAL prefix +
the canonical remainder, which is byte-identical to the source-ref the md
watcher mints beneath that root. Joins hold in BOTH boot-cwd directions.
Honesty ruling honored: a dangler whose target truly is absent (file deleted
/ never under the roots) still gets **no fabricated edge** — existence +
root-membership gates are unchanged; what's gone is only the alias-string
identity split.

**The one live dangler heals itself:** import edges do not ride the assert
log, and the cluster is ephemeral — the dishonest edge lived only inside the
2026-07-05 boot's cluster. Next boot re-extracts with the rebase and mints
the joining id; the dangler is never re-created.

**Proof:** new deftest `alias-rebase-and-run-level-dedup` — a symlinked repo
root, one session split across two files, alias + real paths of one doc: the
edge targets the watcher-form doc id (joins), the alias-form relation-id does
NOT exist, `:doc-edges 1`.

## Seam 3 — [GIT-SPINE] server stats truthfulness: **TWO DRIFTS FOUND, FIXED**

1. **`spine-sync!` read a converged re-run as fresh material.** Verified in
   the topology source: a byte-identical re-request (spine's G1-deterministic
   request-ids) hits the PRIOR-AUDIT branch — the original `:accepted`
   decision row is ack-returned untouched (`:replayed-from-decision-id` never
   set on this path; `replay-decision-row` only fires for a fresh request-id
   + same idempotency key). Stats now split additively — `:ingested` keeps
   its gate-tested meaning (G2's convergence proof reads it), plus `:fresh`
   (accepted AND decided during THIS pass), `:converged` (replayed-from set
   OR decided-at-ms predates the pass), `:rejected`, `:unresolved` (no
   decision inside the await window — an unknown, not a failure claim).
   First empirical run falsified my initial `:replayed-from`-only split (it
   read 5 fresh / 0 replayed on the second pass); the shipped split is the
   one the kernel actually supports. Test: first pass all `:fresh`, second
   pass all `:converged`, zero fresh.
2. **Extractor edge counts over-reported by up to ~300×.** Live corpus fact:
   up to **298 jsonl files share ONE sessionId** (agent/sidechain files carry
   the parent session's id) and relation-ids are conversation-scoped, so the
   old per-FILE dedup re-appended the same edge once per sibling file — the
   journal kept the land honest, but `:sha-edges`/`:doc-edges` counted every
   duplicate append, and each paid a pre-check query + a depot append. Dedup
   is now RUN-level on `[session-id x]`, mirroring edge identity. Test: same
   session split across two files → `:sha-edges 1`, `:doc-edges 1`, one
   event in the edge history.
3. Cosmetic-but-load-bearing: the boot log line no longer prints
   `:edge-relation-ids` (hundreds of ids burying the counts) —
   `run-git-spine-boot!` dissocs it from the PRINTED map only; the return
   value is unchanged for tests/queries. `replay-assert-log!`'s
   `{:replayed :failed}` counts verified truthful as-is (per-line append
   attempts vs isolated failures; existing resilience test pins 2/1).

## Seam 4 — STRETCH

- **`:workers 2` relation-kernel smoke: PASS** (in-session probe, not a
  committed test — relation_kernel_test is outside this branch's fence).
  `start-relation-runtime! {:tasks 4 :threads 2 :workers 2}` launches; an
  assert materializes (`:asserted`, history 1); `relations-for-targets`
  resolves across the multi-worker IPC. Evidence: `[W2-SMOKE]` probe output
  this session.
- **Incremental jsonl over the 1.2 GB corpus: ANALYZED, DELIBERATELY NOT
  BUILT.** The WP-B2 follow-up was named when the extract cursor was still
  durable-cross-boot. The 2026-07-05 gate addendum made the cursor
  CLUSTER-INSTANCE-scoped precisely so a fresh boot reprocesses everything —
  edges MUST re-assert into each boot's empty ephemeral cluster, and that
  requires re-reading the corpus; a byte-offset cursor cannot skip it. Within
  one boot, extract runs exactly once (transcript roots are not watched), so
  offset resume has zero live call sites. The naive form is therefore moot;
  the real cross-boot cost fixes are policy-grade and out of fence: (a) a
  durable production cluster, or (b) spine edges written to a durable replay
  log (assert-log-style) replayed at boot instead of re-extracting — (b)
  touches the D-008 write-surface design and belongs to a trunk decision, not
  a branch. Building the offset cursor anyway would have been imagined-demand
  work (D-001).

---

## Files touched

- `src/app/server/rama/git_spine.clj` — seam 1 (`:claimed/at-ms` on the
  import request), seam 2 (`canonical-under-roots` rebase), seam 3
  (`spine-sync!` truthful stat split; run-level dedup volatiles threaded into
  `process-jsonl-file!`).
- `src/app/server/rama/trail_view.clj` — feed assembly only:
  `source-activity-entry` reads `:claimed-at-ms`.
- `src/app/server/ingest_watchers.clj` — boot-hook log line prints counts
  minus the id vector.
- `src/app/server/rama/object_container.clj` — ADDITIVE `claimed-at-ms` field
  on `SourceIngestCompletionRow` + the live constructor (fence carve-out:
  "no kernel schema changes beyond additive fields").
- `src/app/server/rama/object_container/markdown_adapter.clj` — forced arity
  companion at the second (dead-row) constructor site.
- `test/app/server/rama/git_spine_test.clj` — seam 1/3 assertions in
  g2-g4 and g5-g6-g7; new `alias-rebase-and-run-level-dedup` deftest.
- `test/app/server/rama/trail_view_test.clj` — G9 feed test carries the
  two-clock pair (declared claim vs md nil-honest).

## Doubts + falsifiers (none blocking)

1. **Mid-append failure vs run-level dedup:** if a file throws AFTER a
   `[session x]` pair is marked seen but before/during its append, a sibling
   file in the SAME run skips the pair — the edge is missing for that boot
   (next boot's full reprocess heals it; the failed file's cursor entry is
   also not advanced). Same healing shape the per-file code had. *Falsifier:*
   kill extraction mid-file; edge present after next boot.
2. **Rebase covers root-level aliases only:** a symlinked SUBDIRECTORY inside
   a land root would still split textual forms. None exist today (checked
   with `find -type l`). *Falsifier:* create one, edit through it, count
   danglers.
3. **`:unresolved` is an unknown, not an outcome:** a decision that lands
   after the 20s await window prints as unresolved that pass even if later
   rejected. *Falsifier:* compare `:unresolved` vs decisions-by-audit reads
   on a slow boot.
4. **`pass-started-ms` comparison assumes one clock:** true in-process (IPC
   shares the JVM clock); a future remote cluster with skew could misread
   fresh as converged. Revisit if the runtime ever leaves the JVM.
5. **The adapter's dead completion row** reads the LEGACY request's
   `:claimed/at-ms` (nil even for commit imports — git_spine assocs onto the
   FINAL request). Harmless while `materialization-completion-row` has zero
   consumers; whoever wires it must thread the key. Named here so it cannot
   drift silently.
6. **Claimed coverage is commits-only by scope:** transcript-conversation
   source rows still read claimed-nil at the feed's source-ingested branch
   (their `:produced` relation entries DO carry claimed via assert requests).
   Promoting transcript claimed clocks onto import requests is a future
   additive use of the same `:claimed/at-ms` key.

Stamp: **Trunk-4 / t4-spine** · session 2026-07-06 · no commits made.
