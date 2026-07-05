# CONTRACT — git-spine WP2 (+ /assert write shim) · v1.2

Fable-authored 2026-07-05 in the HQ session, under Sid's in-session time-box
blanket ("use the recommended option, note the others"). **v1.1 amends v1
after `CONTRACT_VALIDATION_R1.md` returned FAIL** (B1: v1's edge targets
could not join rendered objects — a Fable adjudication error caught at text
time; B2: replay had no allowlisted home; S1/S2/A1–A4 folded). The R1
artifact is kept verbatim; scoring per D-006 criterion 3 belongs in the
close-out. **v1.2 amends v1.1 after `CONTRACT_VALIDATION_R2.md` FAIL**: R1's
blockers verified CLOSED, but v1.1's own S1 fix was impossible — record
literals throw under `*read-eval* false` (empirically proven in-repo). v1.2
adopts the validator's fix verbatim (plain-map serialization, §3.C), pins
the shared log path as a literal (SF-R2.1), reconciles §3.D with G9
(SF-R2.2), and pins the canonical-text line format as a cross-builder
interface. **Round 3 waived under the time-box** — R2's sole blocker
carries a named mechanical fix adopted as-is; residual risk assigned to
duty §8.7 and gate G8. Alternative (a third validation round) recorded,
not taken. Binding together with
`decisions.md`. `INPUTS.md` (same dir) is the verified fact base — builders
**re-grep every cite before relying on it** (line numbers drift); the facts
stand. Process: phases run as fresh-context Opus subagents inside the
orchestrating session (decisions.md 2026-07-05 ruling); validation and
falsification layers are default-fail; per-round artifacts, never overwrite a
FAIL.

## §1 Goal and scope

Give the trail view **threads** and **names**, and give the land its **first
write affordance** — the three things first light demanded (F-L2, F-L3, D-008
loop) and the H1 arming event needs.

In scope:
- **A.** Git commit metadata enters the object container as source artifacts
  (D-003 Regime-1: metadata ingested; code content stays addressed, never copied).
- **B.** Commit↔commit `:based-on` edges from parent links.
- **C.** Transcript→commit `:produced` edges extracted from raw session
  `.jsonl` (shas verified against the actual repo before asserting).
- **D.** Transcript→doc `:produced` edges (capped: one per session×file).
- **E.** Display names in the feed (F-L3), data-layer seam only.
- **W.** `/assert` HTTP affordance + replay-file durability (D-008 write
  surface; OI-1 interim answer).

NOT in scope: relation-kernel changes of any kind (verified unnecessary —
INPUTS §3.5: `:git-commit` target-kind exists); transcript-corpus ingest into
PStates (stays the named follow-up); rename/move continuity + confidence
algebra (parked, decisions.md); new faces or pixel work; OC schema changes
(display-name truth seam §5.7c is explicitly deferred).

## §2 Adjudications (Fable, recorded; alternatives preserved)

**2.1 Durable edges, not projection-time joins** (the adjudication queued in
the baton). Joins are asserted as durable RelationEdges with
`asserter-type :import`. Grounds: edges carry provenance/custody/stance and
retraction; they feed the existing activity buckets and R-queries, so threads
render with ZERO new query machinery; deterministic relation-ids make re-runs
converge (INPUTS §3.6). Alternative NOT taken: projection-time joins — no
storage and no staleness, but cannot carry `asserted-by`/exactness/retraction
and would demand new query topologies per join type. Reverse only if a used
form breaks against edge staleness (D-001).

**2.2 Exactness rides `note`, grammar v1.** `note = "spine-v1|<basis>"`,
basis ∈ `sha-verified` | `parent` | `file-write`. Alternative NOT taken: a
structured kernel field now — that is a kernel amendment + migration for a
value no face renders yet; promote when a view demands rendering exactness
differently (D-001).

**2.3 Edges must JOIN rendered objects (amended v1.1, was: bare-sha
targets).** The bundle join resolves a relation to material via
`extract-object-key` on the target id (validator: `read-context-bundle`,
`trail_view.clj` ~466-468; green-suite precedent targets CONTAINER ids,
`trail_view_test.clj` ~135-137). Therefore:
- **Commit endpoints** target `:container` with the commit's
  document-container-id — minted WITHOUT ingest by the same pure fns that
  build the import request (`git_spine` exposes `commit->document-id` =
  existing OC id math over the canonical text; single source of truth, so
  extractor and adapter can never disagree). The sha stays human-legible in
  the object's `source-ref "git-commit:<sha>"` and in `note`. v1's
  decoupling SURVIVES: ids derive from pure functions, not from ingest state.
- **Conversation endpoints**: duty §8.2 decides. If the chat-conversation
  container id derives from session-id alone, target it; if not, keep
  `:conversation` + session-id as a DELIBERATELY dangling endpoint — the
  design already renders that honestly (room-card-lane R6: standalone kraft
  line naming the far end). Record which path was taken.
- **Doc endpoints**: target whatever id the bundle join actually resolves
  for rendered doc objects (duty §8.6 — match the test precedent, don't
  guess); note version-addressing semantics in the phase artifact.
Alternative recorded (v1's `:git-commit`+sha): kernel-native and
ingest-independent, but provably invisible to bundle/View-3 joins — G11
unbuildable. Rejected on validator evidence.

**2.4 Import actor identity is VERSION-FREE:** asserter-actor-id
`"import:git-spine"`, asserter-type `:import`; envelope actor the same
(automation custody). Extractor version lives in `note` ONLY. Grounds:
relation-id includes the asserter (INPUTS §3.6) — versioning the actor id
forks every edge identity on upgrade.

**2.5 Everything runs in-process.** The live cluster is an IPC inside the app
JVM (INPUTS §6.1-6.2, §6.9): no standalone script can reach it. Spine sync
hooks the existing watcher layer (which already writes on this exact handle —
INPUTS §6.7); /assert is an HTTP route on the running server. A CLI agent
reaches the land via `curl`, never via its own cluster.

## §3 Components

**A — `git_spine.clj` (new ns), adapter half.**
- `read-commits`: `git log --all` via ProcessBuilder (repo path from cfg,
  never hardcoded), explicit `--pretty` format + `--name-status -z` parsing;
  rename lines normalized to `old -> new`.
- `commit->canonical-text`: PURE and byte-stable, LABELED lines in fixed
  order — `sha:` `parents:` `author:` `authored-at:` (UTC ISO-8601)
  `committed-at:` (UTC) `subject:` then blank line, body, blank line,
  `files:` (sorted, one per line, renames normalized `old -> new`); no
  locale, no wall clock. This exact format is a CROSS-BUILDER INTERFACE —
  P3's bundle-subject parse reads the `subject:` line; neither side may
  deviate.
- `commit->import-request`: wraps canonical text via the EXISTING
  markdown-adapter request builder with `source-ref = "git-commit:<sha>"` —
  materialization rides the proven md path end to end (INPUTS §1.6-1.9), zero
  new topology.
- `spine-sync!`: idempotent full pass — batch commit ingests (bounded
  concurrency; do NOT serially await 5s each), then parent `:based-on` edges
  via `rk/append-relation-request!` with an existence pre-check (skip edges
  already asserted with the same relation-id, so re-runs do not grow event
  rows).

**B — extractor half (same ns).**
- `extract-session-joins!`: stream `.jsonl` files under cfg
  `:transcript-roots` line by line (never slurp; dir is ~1.2 GB). Candidate
  shas = 7–40 hex tokens in tool_use command text and tool_result text in
  git-ish context; resolve short shas via `git rev-parse`; **assert only shas
  that exist in this repo** (`sha-verified`). Emit
  conversation→git-commit `:produced`; evidence-source-id
  `"transcript:<session-id>"`, evidence-anchor-id = the jsonl entry uuid (or
  line number if absent, recorded which). Session×doc: Edit/Write
  `:file_path` under the land roots → conversation→doc-file `:produced`, ONE
  per (session, file), in-run deduped.
- Cursor file (cfg path, untracked): session-file → {mtime, size}. Cost
  optimization ONLY — correctness comes from the idempotency journal;
  deleting the cursor must never change land state.

**C — `ingest_watchers.clj` amendment (owned by the same builder as A/B):**
after the initial sweep in the existing boot future:
`(replay-assert-log! cfg)` → `(spine-sync! cfg)` →
`(extract-session-joins! cfg)`; cfg gains `:repo-root :transcript-roots
:spine-cursor-path :assert-log-path`
(`:assert-log-path` defaults to the shared literal
`data/relation-assert-log.ednl`, repo-root-relative). `file_viewer.cljc`
changes are cfg-plumbing ONLY. **Boot replay lives HERE (v1.1, closes B2):**
`replay-assert-log!` is defined in `git_spine.clj` (P1's ns), reads the
assert-log file, and re-appends each stored envelope VERBATIM to the depot —
same request-id + idempotency-key. Semantics (validator A1): within one
cluster lifetime, double replay adds nothing (journal drops); on a FRESH
cluster the first replay creates fresh accepted decisions — durability means
STATE RECONSTRUCTION with identical relation-ids, not decision-row reuse.
Serialization (v1.2 — R2 proved the v1.1 scheme impossible: record literals
throw under `*read-eval* false`): the log stores PLAIN MAPS. Before append,
the envelope's `:payload` (and its `:from`/`:to`) convert record→map
(`into {}`, recursive); the line is `pr-str` of that pure-edn value;
readback is `clojure.edn/read-string` — no tags in the log, no read-eval
anywhere. Replay rebuilds a depot-acceptable envelope from the stored map
with `:request/id` and `:idempotency/key` VERBATIM (identity lives in the
ids, not the types); duty §8.7 settles whether the depot path accepts
plain-map payloads directly or needs the public record constructors.

**D — `trail_view.clj` feed enrichment (E), separate builder:** add
`:display-name` at the feed constructors (INPUTS §5.7b): md/doc/file entries →
basename of the adjacent `:source-ref`/`:file-path`; commit-artifact entries →
`<sha7>` derived from the sha-bearing source-ref (v1.2 per G9 — the feed
completion row carries no subject; the BUNDLE/View-3 rendering shows
`<sha7> · <subject>` by reading the `subject:` line of the material
content-text, §3.A format); View-3 text projection prefers display-name
when present. `cards.cljc` untouched — it
already consumes `:display-name` (INPUTS §5.1).

**E — W, the /assert shim (`server_jetty.clj`, separate builder):**
- `POST /api/relation/assert` `{kind from-kind from-id to-kind to-id note
  asserter-id asserter-type}` → validate IN THE ROUTE (kind ∈
  `rk/relation-kinds`; target-kinds against an explicit route-side allowlist
  — validator A4: the kernel exports no target-kind validator and
  `->target-ref` never throws; non-blank targets) → build via
  `rk/assert-request` → **write-ahead**: append ONE line in the §3.C plain-map format to the
  assert-log at the LITERAL shared path `data/relation-assert-log.ednl`
  (repo-root-relative, untracked, flush per append — SF-R2.1: PW computes
  the path from this constant independently and NEVER requires P1's ns; the
  final-phase G8 pair test catches drift) →
  `rk/append-relation-request!` on the trail runtime → 200
  `{relation-id request-id}`. Invalid → 4xx, NO file append, NO depot
  append. Runtime access: `fv/trail-rt` is a bare delay whose deref can
  block through cluster boot (validator A3) — wrap in future+timeout, return
  503 with a clear message, never hang, never let a stray probe trigger
  boot. Replay is NOT this component's job (§3.C owns it); the two meet only
  at the file format + path.

## §4 Traps ledger (each: naive alternative → concrete failure)

1. **Versioned actor id** ("import:git-spine-v2") → relation-id forks →
  duplicate edges for every join on upgrade. Actor id is version-free (§2.4).
2. **Standalone extractor script** → cannot reach the in-process cluster; it
  would silently build its own empty IPC (tests-only runtimes, INPUTS §6.9)
  and "succeed" against a void. Everything runs in-app (§2.5).
3. **Asserting unverified sha mentions** → a hallucinated/example sha in a
  transcript becomes a "fact" edge — the map lies. Only repo-verified shas
  (`sha-verified` basis); mention-only NEVER asserted in v1.
4. **Re-assert with per-run-random idempotency keys** → deterministic
  relation-ids converge the EDGE but every re-run writes new event/activity
  rows — activity buckets fill with phantom writes (validator A2: the
  journal keys on the idempotency key; it is the real guard). Import asserts
  use a STABLE key: `"spine:" + relation-id + ":" + basis`. Pre-check via
  `read-relation-detail` (`:relation-detail-query` wrapper) before append as
  a cost guard on top.
5. **Replay that re-MINTS ids** → new request-ids/idempotency-keys → new
  decisions → duplicate events. `:request/id` and `:idempotency/key` travel
  VERBATIM from the stored map; reconstructing payload TYPES from the map is
  permitted (v1.2), minting anything is not.
6. **Locale/clock-dependent canonical text** → same commit yields different
  source-hash across machines/runs → fingerprint conflict (rejected rows) on
  re-import. Canonical text is byte-stable, UTC, sorted (§3.A).
7. **Serial 5s awaits over hundreds of commits** (the ingest_watchers idiom
  copied naively) → boot blocked for minutes. Batch + bounded concurrency +
  await counts; the sweep future must not delay first paint.
8. **Git `--name-status` naive split** → rename/copy lines (`R100\told\tnew`)
  crash or mis-file the files-touched list. Use `-z` and explicit status
  parsing.
9. **Slurping jsonl** → 1.2 GB in heap. Stream line-by-line, always.
10. **NUL bytes in written docs/fixtures** (fired 4× historically) — `file(1)`
  must say "text" for every new file; gate 12.

## §5 Gates (executable; suite additions in the new test ns)

- **G1** adapter purity: same sha → byte-identical canonical text, identical
  request (incl. idempotency key), across two runs.
- **G2** double-ingest convergence: fixture repo ingested twice → second pass
  all replay-decisions, container/source row counts unchanged.
- **G3** parity: ingested commit count == `git rev-list --all --count` on the
  fixture repo.
- **G4** parent edges: every non-root commit has `:based-on` edge(s); merge
  commit → one per parent; second sync run → zero new event rows.
- **G5** extractor honesty: fixture jsonl with real + fake shas → only
  repo-verified shas asserted; every edge carries evidence-source-id,
  evidence-anchor-id, and `note = "spine-v1|sha-verified"`.
- **G6** session×doc cap: multiple Edits of one file in one session → exactly
  one edge; files outside land roots → none.
- **G7** cursor is cost-only: delete cursor, re-run → identical land state
  (row counts + edge set), only more work done.
- **G8** shim + replay pair (final-phase test spanning P1+PW output): valid
  POST → 200, edge visible via `await-relation`, assert-log grew by one line
  containing NO reader tags, round-tripping through `clojure.edn/read-string`
  (v1.2); invalid kind/blank target → 4xx, file unchanged;
  same-cluster double replay → zero new decision/event rows; FRESH cluster +
  replay → same relation-ids, edges asserted, exactly one event per edge in
  the new cluster (validator A1: durability = state reconstruction with
  stable identity, not decision-row reuse).
- **G9** names (amended v1.1 — validator S2: the feed completion row carries
  the sha-bearing source-ref but no subject): md feed entries carry basename
  display-names; commit FEED entries carry `<sha7>` derived from source-ref;
  the View-3 BUNDLE rendering (which reads content) shows
  `<sha7> · <subject>`. Subject-in-feed-title is a recorded follow-up, not
  silently absorbed.
- **G10** cross-package suite green in one run: pre-existing 302 (trail-view)
  + 909-suite assertions + all new gates.
- **G11** first-thread render: View-3 text projection over a fixture with one
  transcript→commit join shows the commit and the session in ONE thread
  (text-level check; pixels not required).
- **G12** `file(1)` reports text for every new/changed source and doc file.

## §6 Phases (fresh-context Opus subagents; HQ session orchestrates + gates)

- **P-V** contract validation (fresh context, default-fail): buildability
  under platform semantics — esp. §2.5 in-process claim, md-adapter reuse
  with synthetic source-refs, replay write-ahead semantics, gate
  executability, allowlist-vs-components completeness. Artifact
  `CONTRACT_VALIDATION_R1.md` (per-round files thereafter).
- **P1** components A+B+C hook (gates G1-G4, G7, G12).
- **P2** component B extractor (G5-G7). Same builder/ns as P1, after it.
- **P3** component D names (G9). Parallel-safe with P1/P2 (different files).
- **PW** component E shim (G8). Parallel-safe with P1/P2 (server_jetty only;
  does NOT touch file_viewer — reads `fv/trail-rt` which already exists).
- **Final:** G10, G11; batched diff-falsification (fresh context); Fable gate
  in the HQ session. Time-box deviation recorded: a single P-V round replaces
  the full Phase-0 re-derivation — grounds: INPUTS.md is same-day,
  cite-verified recon, and P-V is default-fail.

## §7 File allowlist

NEW: `src/app/server/rama/git_spine.clj`; `test/app/server/rama/
git_spine_test.clj`; test fixtures (synthetic git repo built inside the test
via ProcessBuilder + tmp dir; fixture jsonl). Runtime state files (replay,
cursor) at cfg'd untracked paths.
AMEND: `ingest_watchers.clj` (hook + cfg — P1 only); `file_viewer.cljc`
(cfg plumbing lines ONLY — P1 only); `server_jetty.clj` (route ONLY — PW
only); `trail_view.clj` (feed `:display-name` + View-3 name preference ONLY —
P3 only).
UNTOUCHED: `relation_kernel.clj`, `object_container.clj` and all adapters,
`cards.cljc`, everything else. One builder per file, no exceptions — the
2026-07-05 double-dispatch collision is the grounds.

## §8 Builder verification duties (before writing code)

1. Re-grep every INPUTS cite you rely on.
2. Conversation endpoint (per §2.3 v1.1): read the transcript adapter's id
   minting and determine whether the chat-conversation container id derives
   from session-id ALONE. Derivable → target it (edges join). Not →
   target `:conversation` + session-id as the deliberately dangling endpoint
   and RECORD it (the design's standalone-kraft-line rule renders dangling
   ends honestly — room-card-lane R6).
6. For EVERY target kind you emit, PROVE the join: the emitted target-id,
   passed through the bundle-join path (`extract-object-key` →
   `read-context-bundle`), must resolve the rendered object — mirror the
   green-suite precedent (grep current `trail_view_test.clj` for the
   container-id-targeted edges) and assert it in the gate tests.
7. Before building replay: empirically confirm whether the depot decision
   path accepts a plain-map payload (request-shape validation + row
   construction), or whether replay must rebuild typed payloads via the
   public constructors (`->RelationMutationPayload`, `->RelationTargetRef`
   / `rk/assert-request`). Either is contract-legal (ids verbatim either
   way); RECORD which, with the probe cite.
3. Verify the markdown-adapter request builder accepts a synthetic
   (non-path) source-ref like `"git-commit:<sha>"` with no path assumptions.
4. Open ONE real transcript jsonl file; confirm the entry uuid field name and
   tool_use/tool_result shapes before writing the parser; record the actual
   transcript dir path into cfg defaults.
5. Confirm `--name-status -z` output shape on a merge commit and a rename
   commit in THIS repo.

## §9 Stop clause

Unbuildable / platform-wrong / binding-doc conflict → classify
implementer-fixable vs policy fork; escalate forks to decisions.md Open
Questions as PROPOSED with verbatim citations; never improvise policy.
Pre-flagged: md-adapter rejects synthetic source-refs → implementer-fixable
(dedicated request builder copying the md idiom); route hit before runtime
boot → implementer-fixable (503, never hang); anything touching relation
kernel schema → POLICY FORK, stop.

## §10 Standing hard rules

Never read `src/app/server/env.clj`. Code and docs in separate commits;
commits only on Sid's word (his 2026-07-05 blanket covers gate-green commits
this session, alternatives noted in the baton). Docs only on the local docs
branch. CLAUDE.md Missionary/Electric patterns and implementation-quirks are
in force for any cljc/Electric touchpoint.
