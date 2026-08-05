# GATE REVIEW — t4-spine (four backend seams)

**Stamp: Trunk-5 · 2026-07-06 · Fable gate. Code read IN FULL (every diff
hunk + surrounding source), custody grep-verified independently — not
trusted from the branch report.**
**Verdict: PASS — with two fixes applied AT gate (endpoint projection +
F1 mark-after-append), touched nss re-run green (39/973/0 across
trail-face + trail-view + git-spine).**

Inputs: `BRANCH_REPORT_SEAMS.md` · full working-tree diff vs 9552aa4 ·
serial suite (Trunk-5 run: **204 tests / 2500 assertions / 0 fail / 0 err**,
all 26 nss, one JVM) · `DIFF_FALSIFICATION_SEAMS.md` +
`DIFF_FALSIFICATION_CROSS.md` (fresh-context subagents).

## Architecture

Four seams, all additive, zero row shapes broken:

1. **claimed-ms (seam 1)** — a NEW explicit request key `:claimed/at-ms`
   declared only by builders that genuinely hold a material-claimed clock
   (git-spine: the committer clock). The kernel copies it onto
   `SourceIngestCompletionRow` as an additive trailing field; the feed's
   `source-activity-entry` reads it into `:time/claimed-ms`. The critical
   design refusal is correct: `:request/time-ms` wall-clock-defaults in
   `core/action-request`, so reading it as claimed would stamp every md
   watcher ingest's ARRIVAL as a claim — md rows stay claimed-nil-honest.
2. **Alias rebase (seam 2)** — `canonical-under-roots` now returns the
   matching cfg root's TEXTUAL prefix + canonical remainder instead of the
   raw transcript path. Existence + root-membership gates unchanged (G6:
   no fabricated edges); only the alias-string identity split dies.
3. **Stats truth (seam 3)** — `spine-sync!` splits `:ingested` (meaning
   preserved for G2) into `:fresh`/`:converged`/`:rejected`/`:unresolved`
   via `pass-started-ms` + `:replayed-from-decision-id`; extractor dedup
   moves from per-file to run-level `[session-id x]` volatiles threaded
   from the driver (live fact: up to 298 sibling files share one
   sessionId). Boot log prints counts minus the id vector (printed map
   only; return value unchanged).
4. **Seam 4** — `:workers 2` smoke passed in-branch (probe, not a
   committed test — the kernel test ns is out of fence); incremental-jsonl
   deliberately NOT built (instance-scoped cursor makes cross-boot skip
   impossible; honest routes are policy-grade → SITTING).

## Custody — writers / readers / clearers (verified by grep, this session)

- `SourceIngestCompletionRow`: exactly TWO positional constructor sites in
  the tree — `object_container.clj:604` (the live write, via
  `source-ingest-completion-row` at the `$$source-ingest-completions-by-ref`
  transform, oc:2122) and `markdown_adapter.clj:227` (the dead row —
  accessor `materialization-completion-row` has ZERO consumers,
  grep-verified). Both updated. One PState schema use (oc:1712) stores the
  record whole — additive trailing field is safe, and the per-JVM ephemeral
  IPC means no old-format rows ever coexist.
- `:claimed/at-ms`: written by `commit->import-request` only (assoc after
  the md builder — deterministic from the commit record, G1 byte-identity
  preserved); read at the two constructor sites; never defaulted.
- Dedup volatiles: created once per extract run in
  `extract-session-joins!`, threaded into `process-jsonl-file!`, never
  escape the run. Separate volatiles for sha vs doc — no cross-kind key
  collision. Keys `[session-id sha]` / `[session-id watch-path]` mirror
  edge identity exactly (conversation-scoped relation-ids).

## Failure modes attempted (Fable pass)

- **Wall clock leaking into claimed** (replay determinism): the value
  derives from `(:committed-at-ms commit)` at request build; the kernel
  copies `(:claimed/at-ms request)` from the depot record — retry replays
  the same request, same value. md requests never set the key → nil all
  the way. CANNOT HAPPEN.
- **Fresh printed as converged**: needs `decided-at-ms < pass-started-ms`
  for a decision made during this pass — same-JVM clock, decisions can't
  predate the pass. The boundary case (`decided-at-ms == pass-started-ms`)
  correctly prints FRESH (strict `<`). CANNOT HAPPEN.
- **Converged printed as fresh**: the identical-request-id branch
  ack-returns the ORIGINAL decision row whose `decided-at-ms` predates the
  pass → converged. Residual: a SECOND `spine-sync!` in the same
  millisecond as the first decision would misprint — cosmetic-stat-only,
  ms-window. DOUBT, not a defect.
- **Dedup UNDER-count**: two different edges collapsing to one key needs
  identical `[session-id x]` with different targets — but sha keys map
  1:1 to `sha->doc sha` targets and doc keys ARE the target-id input
  (`doc-document-id watch-path`). CANNOT HAPPEN within a run.
- **Rebase minting a path the watcher never minted**: both the extractor's
  `land-doc-roots` and the watcher derive from the same `repo-root` cfg
  string (`git_spine.clj:520-524`) — byte-consistency holds unless
  repo-root itself carries a trailing slash (boot cfg; effectively never).
  DOUBT recorded below.
- **Mid-append failure orphaning a dedup key**: pair marked seen →
  append throws → file-level catch → sibling files skip the pair this
  boot; the failed file's cursor entry is NOT advanced and the next boot's
  fresh volatiles reprocess everything. Missing-for-one-boot, self-heals —
  same shape the per-file code had. DISCLOSED (report doubt 1), accepted.

## Async ordering risks

None new: the volatiles live inside one single-threaded extract pass;
`pass-started-ms` is read once before the appends; the kernel topology
paths are untouched except the additive field copy (same event, same
single write site — rama-pitfalls run in-branch, re-checked at gate:
no fingerprint participation, no routing change, retry-idempotent).

## Error-path cleanup

Per-file try/catch (pre-existing, wave-1 fix) still bounds extraction
failures; `run-git-spine-boot!`'s per-stage catch still bounds boot. The
dissoc-on-print cannot throw (pure map op on the stage's return).

## Falsification results (fresh-context subagents) — FOLDED

**SEAMS falsifier (`DIFF_FALSIFICATION_SEAMS.md`): NO BLOCKER.** Custody,
claimed-ms determinism, fresh/converged mechanics, and dedup key identity
all HELD under attempted breaks (probes incl. real symlink constructions
for `canonical-under-roots`). One SHOULD-FIX:

- **F1 — mark-seen-BEFORE-append (git_spine.clj, both edge blocks):** a
  transient `assert-edge!` throw after the mark made every same-session
  sibling file skip that key for the whole boot; the old per-file
  volatiles healed same-boot, run-level only healed next boot — the
  branch report's "same healing shape" claim was INACCURATE (corrected
  here, honest-ledger). **FIXED AT GATE**: the mark now happens after
  `assert-edge!` returns normally (it returns, never throws, on the
  already-asserted pre-check, so same-run re-marks stay impossible).

Doubts D1–D6 carried with falsifiers in the artifact — top two: the
positionally-serialized record trailing-field pattern is safe ONLY under
ephemeral per-JVM IPC (will bite at durability — rides the incremental-
jsonl POLICY FORK to the sitting); `doc-document-id` mixes path with
CURRENT file content hash, so the rebase makes joins path-aligned but
still content-conditional (pre-existing, named).

**CROSS falsifier (`DIFF_FALSIFICATION_CROSS.md`): claimed-ms seam
VERIFIED CORRECT end-to-end** — all three deliberate key names
(`:claimed/at-ms` → `:claimed-at-ms` → `:time/claimed-ms`) map correctly,
arity sound, nil honest at every hop, and the additive field is invisible
to the client (no closed-spec rejection). Its BLOCKER (B1, endpoint gap)
and drift siblings are dispositioned in the endpoint-fix section below
and in GATE_REVIEW_R2.md.

## Open doubts (carried with falsifiers)

1. Trailing-slash repo-root would split rebase output from watcher refs —
   falsifier: boot with `repo-root "/mnt/data/projects/Softland/"`, count
   danglers. Not defended (cfg never produces it today).
2. Same-ms double-sync misprints converged-as-fresh — cosmetic; falsifier
   in stats split test with a forced clock.
3. Nested symlink inside a land root still splits identity (report doubt
   2; none exist today, checked).
4. `:unresolved` is an unknown, not an outcome (report doubt 3) — accepted
   semantics, documented in the code.
5. Remote-cluster clock skew vs `pass-started-ms` (report doubt 4) —
   revisit if the runtime leaves the JVM.
6. The adapter's dead completion row reads the LEGACY request's key (nil
   even for commits) — harmless at zero consumers; whoever wires
   `materialization-completion-row` must thread the key (report doubt 5).
7. Transcript-conversation source rows stay claimed-nil by scope (report
   doubt 6) — future additive use of the same key.

## Endpoint additive fix (trunk-ordered, applied AT this gate)

The wave's known cross-package gap: `relation-activity-entry`
(trail_view.clj) emitted `:entry/detail` WITHOUT `:from`/`:to` while the
activity row carries all four endpoint fields
(`RelationActivityRow` — relation_kernel.clj:271, verified) and the client
threads exclusively off `(get-in d [:from :id])`/`[:to :id]`. Fix: detail
gains `:from {:id :kind}` / `:to {:id :kind}` projected verbatim from the
row. **APPLIED AT GATE** (trail_view.clj `relation-activity-entry` now
projects `:from {:id :kind}` / `:to {:id :kind}` verbatim; a `:dead-end`
row honestly projects `:to {:id nil :kind :none}` — pinned by the new
trail_view_test assertion, which caught exactly that case on first run).
Touched nss re-run green. Consumer-side dispositions in GATE_REVIEW_R2.md.

## Gate ruling

**t4-spine PASSES.** All four seams sound as built; F1 + the endpoint
projection applied at gate; the branch report's one inaccurate healing
claim corrected in this artifact. Commit scope: git_spine.clj ·
trail_view.clj · ingest_watchers.clj · object_container.clj ·
markdown_adapter.clj · git_spine_test.clj · trail_view_test.clj (one
code commit, spine package). Sitting items raised: incremental-jsonl
policy fork (durable cluster vs durable spine-edge replay log — now ALSO
carrying the record-serialization durability doubt D1); importer
provenance on source rows (see R2 gate S1-cross).
