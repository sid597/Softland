# Fix Plan — Session 5, Transcript Kernel (May-era remnant)

Date: 2026-06-12. Inputs: FINDINGS.md (TR-01..09, TT-01..04), IMPLEMENTATION_VALIDATION.md,
TEST_VALIDATION.md, IMPLICIT_SPEC.md, prior probe retro RAMA_REVIEW.md (F1–F6).
This is the "small Phase-1 plan" FINDINGS requires for TR-03 (redaction pipeline) and the
deferral design for TR-08, plus the batch plan for the rest of the session scope.

## Constraint surface (June layer must not break)

Functions in `transcript.clj` the June object-container layer calls — contracts frozen:
- `read-jsonl-observations` 3-arity (partial trailing line IS returned as last record — June
  callers `transcript_ingest.clj:883` rely on the documented shape). DO NOT change semantics.
- `read-complete-appended-lines` — contract `{:observations :next-offset}` frozen; the TOCTOU
  inside is a bug fix, not a contract change (`transcript_ingest.clj:973` benefits).
- `walk-jsonl-files`, `source-file-key`, `source-line-key`, `file-id`, `request-id`,
  `request-validation-errors`, `transcript-request`, `parsed-tool-use-blocks`,
  `object-container-runtime?`, `await-materialized` — untouched or additive only.
- `transcript-run-status-record` — additive opts only (`:executor/id`).
- `redacted-preview` — signature `string → string` frozen; behavior becomes redact-then-truncate
  (strictly safer; `transcript_ingest.clj:847` re-redacts previews anyway).
- `harvest-transcripts!` / `start-transcript-watch!` OC dispatch branches — untouched.
- Fold/row builders (`fold-run-status`, `increment-run-counts`, `initial-run-row`,
  `rejected-run-row`, `conversation-entry`, `source-file-state-entry`, `obs-tool-call-first`)
  are used ONLY by the May module — free to restructure (verified by grep 2026-06-12).

## Batch 0 — failing probes first (protocol step 2)

New ns `test/app/server/rama/dogfood_transcript_probe_test.clj`, ONE IPC launch, sequential
`testing` blocks, semantic-payload assertions (style: `text_kernel_probe_test.clj`):

1. **utf8-byte-identity** — multi-byte fixture (2- and 3-byte chars): observation byte
   offsets/lengths equal real byte positions; harvest → ledger payload text byte-identical;
   re-read from recorded offset re-matches line hash (I5).
2. **parse-error-redaction** — malformed line carrying `api_key`-shaped secret → ledger row
   preview does NOT contain the secret; `:transcript/redactions` non-empty (I2 / TR-03).
3. **duplicate-submit-post-complete** — re-append the same request after `:complete` → status,
   counters, owner unchanged (TR-02 / A5).
4. **duplicate-submit-different-payload** — same id, different paths → truth unchanged,
   conflict recorded on the row (TR-02).
5. **harvest-partial-tail** — file ends mid-line → no ledger row for the fragment, file cursor
   = end of last complete line, conversation excludes fragment (TR-04 / I5).
6. **multi-tool-use** — one line, two `tool_use` blocks → BOTH ids readable, each with
   `:tool-call/name` AND redacted `:tool-call/input` (TR-07).
7. **late-claim-post-terminal** — `:running` claim after `:complete` → status stays
   `:complete`; rejection auditable on the row (TR-02 / I7).
8. **duplicate-obs-replay** — re-append an identical observation → counters and views
   unchanged (I4; guards the new per-run counting).
9. **orphan-obs** — observation for an unsubmitted request-id → no phantom run row.
10. **per-run-counters** — re-harvest with new request-id → second run's own
    `observed-line-count` equals lines it processed (A8; pre-fix reports 0).
11. **claim-arbitration** — second `:running` claim with a different `:executor/id` on an
    owned run → owner unchanged, loser rejected (TR-05).
12. **failed-then-cancel** — `:failed` terminal claim then `:cancelled` → stays `:failed` (I7).

Expected pre-fix failures: 2,3,4,5,6,7,10,11,12 (1,8,9 pin already-correct behavior).

## Batch 1 — TR-03 redaction pipeline (the required Phase-1 plan)

Design — two redactors, both metadata-producing, both in `transcript.clj` (cannot live in
`transcript_ingest.clj` — circular; must not modify `llm.clj` — Session 2 owns it; reuse
`llm/sensitive-key?` which is public):

- `redact-text` (string → `{:text :redactions}`): conservative pattern redaction for raw,
  possibly-malformed text. Patterns, each masking the VALUE span with `"[REDACTED]"`:
  - `"<sensitive-key>" : "<value>"` JSON pairs (key matched by `llm/sensitive-key?`
    vocabulary), including the UNTERMINATED variant (`"api_key":"sk-...` cut mid-write —
    the exact shape of the proven F1 breach), and unquoted `key=value` forms.
  - bearer tokens (`Bearer <blob>`), and bare credential shapes: `sk-…`, `ghp_…`/`gho_…`,
    `xox[baprs]-…`, `AKIA[0-9A-Z]{16}`, `eyJ…` JWT-ish triplets.
  - Metadata per hit: `{:redaction/kind :text-pattern :redaction/pattern <name>
    :redaction/length <chars masked>}` — answers "what was masked?" without the content.
- `redact-payload-with-redactions` (parsed payload → `{:payload :redactions}`): structural
  walk mirroring `llm/redact-provider-payload` exactly (same `llm/sensitive-key?` predicate,
  same `"[REDACTED]"` value) but collecting
  `{:redaction/kind :key-name :redaction/key <name k> :redaction/path <key path>
  :redaction/length <count of str v>}`. Output payload byte-identical to the current
  redactor's, so June/test behavior is unchanged.
- `redacted-preview` := redact-text FIRST, then truncate to 200 (truncate-after-redact: a
  truncation can cut a secret's closing quote, so the unterminated-pair pattern exists and
  redaction must see the whole line, not the truncated prefix).
- `transcript-observation`: parsed branch populates `:transcript/redactions` from the
  structural walk; parse-error branch stores the redacted preview AND its text-pattern
  redactions. `:transcript/redactions []` stays the value for rows where nothing matched.

## Batch 2 — byte-correct, newline-gated consumption (TR-04 / TR-06)

- New `reduce-jsonl-observations` — streaming, BOUNDED reader: reads raw bytes from
  `start-offset` up to a caller-supplied `end-offset` (the length snapshot), UTF-8-decodes
  once, calls `(f acc obs)` per newline-TERMINATED line, returns
  `{:acc :next-offset}` where `:next-offset` advances only past consumed newlines. A partial
  line at the bound is not emitted, not a parse error, and does not advance the cursor (I5).
- `read-complete-appended-lines` reimplemented on it: snapshot `len` once; the bound IS the
  newline gate — the live-EOF TOCTOU race (S4 trace 2) is structurally gone. Same contract.
- Non-OC `harvest-transcripts!`: per-file `read-complete-appended-lines` (newline-gated;
  fixes harvest-side partial ingestion + mid-line cursor advance) with appends as lines are
  read (streaming, F3) and per-file try/catch (I8-lite: file error recorded as a progress
  claim, run continues; loop-level failure → `:failed`, never stuck `:running`).
- Non-OC watch: `known-at-start` and `offsets` keyed by `source-file-key(file-id)` (inode)
  instead of `File` path objects — recreated-at-path becomes a new file read from 0;
  in-memory cursor advances only AFTER the lines' appends return (`:append-ack` durable);
  per-file try/catch isolates bad files; a fatal poll error appends `:failed` AND stops the
  loop (no terminal-then-keep-polling incoherence).

## Batch 3 — idempotent folds, sticky terminals, claim arbitration, tool completeness (TR-01/02/05/07)

Topology restructure of the observation event — kill the cross-partition dedup gate; every
write becomes independently idempotent at its own write site (validated fix direction):

- Ledger line row: `(term #(core/write-if-absent % *obs))` — first observation wins;
  duplicates/replays no-op (provenance keeps first read-time).
- File cursor row: whole-row fold keeping the row with the highest
  `:source/last-byte-offset` (monotonic watermark at row level) — replays/reorders can
  never rewind the cursor.
- Run counters: become PER-RUN processing counts (spec op 5, A8). New PState
  `$$transcript-run-seen-lines {String (map-schema String Boolean {:subindex? true})}`
  keyed by request-id (colocated with the run row). Seen-check + seen-write + counter
  increment all on the run task between partitioner boundaries → atomic; replay after
  commit hits the seen marker → no double count; partial replay cannot split them.
  Guarded by run-row existence → no phantom rows for orphan observations.
- Conversation entry: unconditional `[(keypath *conversation-id *line-key) (termval …)]`
  (consecutive-keypath hygiene fixed en route) — deterministic per identity, idempotent.
- Tool calls: `explode` ALL `tool-call-index-rows` (not first-only), each row
  `[(keypath id) (termval row)]`; rows gain `:tool-call/input` (already-redacted block
  input). `obs-tool-call-first` deleted (no external callers).
- Request source: `filter>` blank ids (poison guard), single `<<if/else>` building the row
  (redundant-conditional hygiene), then a pure `fold-request`: absent → row stamped with
  `:request/fingerprint` (fingerprint over the request MINUS `:request/time-ms`); same
  id+fingerprint → identical row (replay no-op); different fingerprint → bounded
  conflict note on the row, state untouched.
- Claim source: pure `fold-claim` behind the compute-style guard chain:
  1. `:claim/id` already in bounded `:applied-claim-ids` → identical row (replay no-op);
  2. `core/authorize-mutation` — accepting `#{:pending :running}`, sticky
     `terminal-statuses`, owner proof (`:claim/owner` row key vs `:executor/id` record key)
     enforced once an owner exists;
  3. accepted → grant owner on first `:running` claim carrying `:executor/id`, fold status
     via `core/sticky-status`, record claim-id;
  4. rejected → bounded `:claim-errors` audit entry (token-free, via the auth dead-letter),
     semantic fields untouched.
  `filter> (not (identical? …))` skips no-op writes (compute convention).
- Executors (non-OC harvest/watch): mint `:executor/id`; after the `:running` claim, await
  the grant and VERIFY ownership — a loser returns `{:status :claim-lost}` without touching
  files (at-most-once execution). Harvest awaits `observed-line-count` == its appended count
  before appending `:complete` (kills the torn-complete race, TT-03), and `:complete`
  carries no `:counts` (counters are topology-owned).

## Deferred with flags (recorded here + in the postfix validation)

- **F4 partitioning deviation** — `*transcript-obs-depot` stays `hash-by
  :transcript/ingest-request-id`; the contract said source-file. Deviation documented in
  the module header; realignment needs a depot migration — out of session scope.
- **TR-08 conversation/ledger reshaping** — target design when taken up: conversations as
  `{String (map-schema String Object {:subindex? true})}` keyed by a zero-padded
  `offset:hash` order-key (source-ordered range reads), message content included; ledger
  re-keyed per file with a subindexed offset map + per-run note rows (op 9 range access).
  Out of scope: schema migration + read-path rewrite. The unbounded non-subindexed inner
  conversation map REMAINS a known liability at production scale.
- **TR-09 behavior items** — `:since` floor, empty-file ledger note, parse-error
  conversation visibility under path fallback, obs-depot funnel: untouched. Hygiene items
  on lines this session already rewrites (redundant conditionals, consecutive keypath,
  `foreign-select-one` reimpl) are fixed en route.
- **A4 EOF-baseline silent skip note** — unchanged (tested contract).

## Verification protocol

After every batch: `transcript`, `transcript-ingest`, `object-container` test namespaces +
the probe ns. Done gate: all probes pass; postfix blame-scoped Phase-4 re-validation by a
fresh-context agent → `IMPLEMENTATION_VALIDATION-postfix.md` with verdict.
