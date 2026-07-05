# GATE REVIEW — git-spine WP2 (+ trail-room R-1) · batched wave 2026-07-05

Fable gate review of the ENTIRE uncommitted working-tree diff, per the
delivery-mode ruling (decisions.md D-006 notes 2026-07-05: ONE batched
falsification + gate per wave, never sprinkled per-phase) and CLAUDE.md's
falsification protocol. Inputs: git-spine CONTRACT v1.2, trail-room CONTRACT
v1, `DIFF_FALSIFICATION_R1.md` (fresh-context adversarial pass), the full
code diff (read in this session, not summarized from artifacts), and the
gate-test results (G8 pair + G11 green at open).

**VERDICT: PASS with fixes applied in-review.** All six falsification
should-fixes were confirmed against the code, fixed directly (delivery mode —
Fable coded), and covered by new executable tests. One NEW finding of the
same class as should-fix 5 was found at gate and fixed (kraft label
overflow). One structural root cause was identified beneath should-fix 5 and
fixed at the source (rect_tree clip semantics). No blocking findings remain.

**Receipts (post-fix):**
- trail-face ns: 19 tests / 456 assertions / 0 failures (was 445 — the new
  clip-regression test adds 11).
- spine + route + gate-pair nss (one JVM, serial): 14 tests / 175 assertions
  / 0 failures (includes 3 new deftests: asserter-custody,
  git-commit-rejected, retry-idempotency; and replay-resilience).
- Full serial suite: **191 tests / 2202 assertions / 0 failures / 0 errors**
  (one JVM, serial, all 25 test namespaces; up from 187/2168 at session open —
  the deltas are the reviewer-added tests).

---

## Architecture (as reviewed)

The falsification artifact's architecture section was verified accurate
against the code and stands. Two additions from the gate read:

1. **The clip model was structurally broken, not just mis-applied.** In both
   `tree->rects` and `tree->text-ops` (`rect_tree.cljc`), a `clip?` node's
   child-clip REPLACED the ancestor clip instead of intersecting with it.
   That single semantic is why R-1's builder faced a forced choice: expansion
   `clip? true` broke gate-6 (children escaped the viewport clip and emitted
   negative-y bg rects in the scroll-model), while removing it stripped
   card-width text truncation from every non-material section. Both were the
   same bug seen from two sides.
2. **The route and spine share the idempotency discipline now.** Both paths
   use deterministic keys derived from the relation-id ("spine:<rid>:<basis>"
   import-side, "assert:<rid>" route-side), so the journal — not luck — is
   the duplicate guard on both.

## Fixes applied at gate (delivery mode)

| # | Finding (falsification / gate) | Fix | Test |
|---|---|---|---|
| 1 | asserter-id unvalidated → lying 200 + poison log line | `validate-assert-params` requires non-blank asserter-id + keyword asserter-type | `asserter-custody-validated-no-write` |
| 2 | per-POST UUID idempotency key → curl-retry duplicate decision/event/activity rows (trap 4) | deterministic `"assert:<relation-id>"` default; optional `:idempotency-key` override for deliberate re-asserts (blank overrides fall through — they'd be depot-rejected post-200) | `retry-idempotency-converges-at-the-journal` |
| 3 | `:git-commit` allowlist target dangles (bare-sha key ≠ rendered object key — the v1.1 B1 non-join, re-exposed) | dropped from the route allowlist; docstring names the joinable alternative (:container + oc:doc id, which View-3 prints) | `git-commit-target-kind-rejected` |
| 4 | replay aborts on one torn line (lines after silently un-replay forever); extractor dies on one unreadable file | per-line try/catch in `replay-assert-log!` (returns `{:replayed :failed}`); per-file try/catch in `extract-session-joins!` (failed file's cursor NOT advanced → retries next boot) | `replay-resilience-and-field-preservation` |
| 5 | expansion clip? removal stripped card-width text clip from info/relations/holes/omissions | **root-cause fix**: `rect_tree` child-clip now INTERSECTS the ancestor clip (`intersect-clip`); expansion is `clip? true` again; the `:material-clip` special-case wrapper removed (redundant under intersection) | `expansion-and-kraft-text-clip-test` + gate-6 `clip-containment-test` still green |
| 6 | reader select-keys drops future record fields; assert-log charset unpinned (JEP-400 hazard) | reader rebuilds payload/target-refs from ALL stored keys (`map->Record` keeps unknowns in the ext map); UTF-8 pinned on route writer, replay reader, and transcript readers | `replay-resilience-and-field-preservation` (future-field + non-ASCII note) |
| G1 (new, found at gate) | kraft connector/line/handle labels are root-level nodes with no clip ancestor — an off-screen label (`├ kind -> oc:doc:<40-hex> (off screen) · asserter`) ≈ 100 chars ≈ 660px bleeds across lanes, same class as #5 | kraft nodes are `clip? true` with the label op in a child node (the cards.cljc idiom); full far-end id preserved in `:data :trail-face/off-screen` | same test, kraft block |

Blast-radius note for fix 5: every production call site of both walks is
arity-1 (nil incoming clip — verified by grep across src/), where
`intersect-clip` degenerates to the node's own bounds, byte-identical to the
old behavior. The only explicit-clip call site in the tree is the gate-6 test
itself. Editor/sidebar/dg_flow rendering is unaffected.

## Failure modes attempted (beyond the falsification's, at gate)

- **Blank `:idempotency-key` override** (my own fix 2's edge): `""` is truthy
  in Clojure — `(or ...)` would have passed it through to a post-200 depot
  reject. Guarded with `present-string?`. (Caught by self-falsification
  before commit; encoded in the docstring.)
- **Kernel re-assert semantics** (fix 2's test depends on it): verified in
  `relation-outcome` that a re-assert with a FRESH key is an ACCEPTED
  transition writing a new event row — so the retry test's 2-event arithmetic
  is sound, and the override affordance is real (note updates / post-retract
  re-asserts work).
- **Same-partition FIFO for the retry test's negative proof**: retry and
  override share the relation-id → same routing key → same task FIFO, so
  awaiting the override's event proves the retry was already processed (and
  dropped). No sleep-as-proof-of-negative.
- **Mode-flip glitch frame in combined_text** (R-1 wiring): `trail-mode?`
  (from `!effective-local-world`) and `rim-slots` (from `!trail-face-scene`)
  co-vary across two watches under one `m/latest` — a transient frame can see
  trail-mode with a nil scene. The guard `(and trail-mode? (seq rim-slots))`
  falls back to the editor strip for that frame; converges next emission.
  Benign; noted, not fixed (single-`m/latest` derivation would need a shared
  source that doesn't exist).
- **Kraft label truncation vs R6 ("name the far end")**: paint now truncates
  at card-w (~44 chars at size 12 — kind + ~20 chars of the far id visible);
  the FULL id stays on the node as `:data :trail-face/off-screen`. R6 holds
  at data level and paint level (the far end is named, partially); R-2's
  hover/copy consumes the data. Recorded as a design note for R-2.

## Writers / readers / clearers (delta vs falsification table)

| State | Change at gate |
|---|---|
| `data/relation-assert-log.ednl` | writer now UTF-8-pinned + only depot-acceptable envelopes reach it (fix 1); reader per-line isolated + UTF-8 (fixes 4/6). Still unbounded (open doubt, below). |
| relation depot (route) | idempotency key now deterministic → retry rows converge at the journal (fix 2) |
| `git-spine-cursor.edn` | failed files' entries NOT advanced (fix 4) — retry next boot is automatic |
| expansion/kraft text ops | clipped to card-w ∩ viewport under the intersection semantics (fix 5/G1) |

## Async ordering risks

Unchanged from the falsification artifact except: risk 4 (retry ordering
duplicate rows) is CLOSED by fix 2. Risk 1 (optimistic 200) is now
low-consequence: every route-validated request passes the depot's own shape
checks (kind, targets, actor, ids are all pre-validated route-side), so a
depot reject requires a validation the kernel gains later — recorded as the
remaining open doubt 3.

## Error-path cleanup

- `replay-assert-log!` — per-line catch, counted, logged, continues. HELD.
- `extract-session-joins!` — per-file catch, counted, cursor not advanced,
  continues. HELD.
- Route — unchanged (400 short-circuits before file/depot; outer 500 catch;
  503 never forces the delay). HELD.

## Open doubts (carried, with falsifiers — none blocking)

1. **Doc `:produced` edge path-string match** (falsification doubt 3): the
   extractor keys doc edges on the transcript's raw `:file_path`, which must
   equal the watcher's `.getPath` string to join. The dual working-dir
   environment (`/mnt/data/...` vs `/home/sid/...`) makes a dangling edge
   concrete. *Falsifier*: a transcript that edited a doc via the other root;
   check for danglers after Sid's first boot (they render honestly as kraft
   lines naming the far end — the design absorbs this, but count them).
2. **Unbounded assert-log + O(all-asserts-ever) boot replay**: acceptable at
   current write volume; rotation/compaction is future work. *Falsifier*:
   boot time grows linearly with log size.
3. **Optimistic 200** (falsification doubt 6, residual): 200 means "durably
   queued + write-ahead", not "decided". Any FUTURE kernel-side validation
   not mirrored in the route re-opens the poison-line class. *Falsifier*: add
   a kernel validation without a route mirror; the G8 pair test will not
   catch it.
4. **`:transcript-file-updated` fixture parity never exercised live**
   (falsification Seam 6): `$$transcript-file-offsets` is never driven by
   `build-fixture!`; the hand fixture's shape for that kind is checked only
   if a live counterpart ever appears (the test now says so explicitly).
5. **Rim glitch frame on mode flip** (above): benign one-frame editor-strip
   fallback; would only matter if the rim ever carries state the strip
   cannot.

## Contract notes (letter vs spirit)

- **rect_tree.cljc touched outside the R-1 allowlist** — ruled at gate:
  the allowlist bound the R-1 builder; the gate reviewer fixed the shared
  walk because the in-allowlist fix (per-section clip wrappers) would have
  re-imported the gate-6 bug for holes/omissions bg. The change is
  behavior-identical for every existing call site (see blast-radius note).
  Spirit held: paint-only, no node-bounds changes (R4/T-4 untouched).
- **Route response shape** unchanged (`{:ok true :relation-id :request-id}`)
  — CONTRACT §3.E honored; `:request-id` is now deterministic, which the
  contract did not forbid and trap 4 arguably demanded.
- **G3's "names the far end"** now satisfied at data + truncated paint (see
  above); the full-name-at-paint reading of R6 would reintroduce the bleed.
  Flagged for the R-2 contract to make explicit.

## ADDENDUM (2026-07-05, post-gate, same session) — cross-boot cursor bug found and fixed

Found while preparing Sid's boot, AFTER this review passed: the land's
cluster is an in-process IPC (`com.rpl.rama.test/create-ipc`) — EPHEMERAL
per JVM; all edge state rebuilds from re-ingest at boot — but the spine
cursor (`data/git-spine-cursor.edn`) is a DURABLE file. Boot 1 works and
writes the cursor; boot 2 gets a fresh EMPTY cluster whose extractor then
skips every unchanged transcript → conversation→commit/doc edges silently
missing. G7 and the falsification both tested the cursor inside ONE cluster
(deletion, corruption, truncation) — never stale-cursor × fresh-cluster.
As designed, the cursor's ONLY effect was the cross-boot case (extract runs
once per boot), i.e. it was purely harmful.

**Fix (same day, delivery mode):** the cursor is CLUSTER-INSTANCE-scoped —
`{:run-id <spine-run-id> :files {...}}`; a `:spine-run-id` is minted in the
same `file_viewer` delay body that creates the cluster (lifetimes bound by
construction); a foreign/legacy/absent-id cursor reads as absent → full
reprocess, which the contract already declares correct ("deleting the cursor
must never change land state"). New test block in `g5-g6-g7-extractor-gates`
(foreign cursor ignored / same-instance skip / anonymous runs never trust a
cursor). Contract-compatible (strictly stronger than §3.B's cost-only rule).

**Lesson (quirks-grade, recorded):** any DURABLE side-state (cursor, cache,
watermark) keyed to state that lives in an EPHEMERAL store must carry the
store instance's identity. Falsify by class: "stale durable X × fresh
ephemeral Y" for every X the boot writes. The assert-log SURVIVES this class
by design (replay re-appends into the fresh cluster — that is its whole
job); the cursor did not.

## Per-package verdicts

- **spine (P1/P2)** — PASS (fixes 4, 6 applied; resilience + preservation
  tested).
- **route (PW)** — PASS (fixes 1, 2, 3 applied; custody, idempotency,
  allowlist tested; G8 pair green post-fix).
- **names (P3 + reviewer G11)** — PASS as-built (falsification: SOUND; G11
  edge-line full-triple fix verified in the diff; no gate changes).
- **render (R-1)** — PASS (fix 5 + G1 applied at root cause; gate-6 model
  and production camera-pan model now agree; editor path byte-identical).
- **electric-skill verification ns** (`missionary_claims_test.clj`) — PASS
  (regression ns runs in the suite; no changes at gate).
