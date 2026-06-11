# Implementation Validation — Transcript Kernel (Retrospective, May-2026 era)

<!-- Phase 4, RETROSPECTIVE + ABBREVIATED mode. No PLAN.md exists for this track;
     validation is against IMPLICIT_SPEC.md + the rama skill's design rules.
     Module: src/app/server/rama/dogfood/transcript.clj (read in full).
     A fail verdict does not trigger a fix loop; this artifact is the retro result. -->

## Mode and era scoping

- **Subject:** the May-2026 lines of `src/app/server/rama/dogfood/transcript.clj` (~589 of 1135 lines by plain blame).
- **Method:** `git blame --line-porcelain` bucketed by commit month, cross-checked with `git blame -w -M`
  (whitespace/move-insensitive) because the June rewrite re-indented May logic into new dispatch wrappers.
  The `-w -M` blame is used for era attribution of findings; it shows the following are **May logic**
  despite plain-blame June timestamps: the non-OC harvest body (912–936), `read-complete-appended-lines`
  (938–961), and the non-OC watch (1070–1090, 1095–1134).
- **May-era (in scope, full treatment):** 1–5, 10–14, 17–198, 207–298, 300–305, 307–362, 380–384,
  388–389, 402, 406, 412, 414–596, 907–908, 912–936, 938–961, 966–973, 979–984, 994–997, 1003–1004,
  1035–1042, 1044–1047, 1052–1059, 1061–1063, 1065–1066, 1070–1090, 1095–1134.
- **June-era (out of scope, brief notes only):** ns imports of object-container (6–9), byte-correct
  reader internals (363–379, 385–413 mostly), the whole OC import pipeline (597–906 mostly), OC watch
  internals (962–1034 mostly), dispatch wrappers (909–911, 1067–1069).
- **Opaque externals (per instructions, not read):** every call into
  `app.server.rama.object-container.*`. Assumptions stated where reasoned past (see "Assumptions").

## Architecture (as implemented, May era)

One Rama module, one stream topology, three depots, four `{String Object}` PStates:

- `*transcript-depot` (hash-by `:transcript/request-id`) → request source: validates, `termval`s an
  initial (`:pending`) or rejected (`:failed`) run row into `$$transcript-runs` (467–478).
- `*transcript-claim-depot` (hash-by `:transcript/request-id`) → claim source: reads the run row,
  `fold-run-status` (185–193), writes back (480–486).
- `*transcript-obs-depot` (hash-by `:transcript/ingest-request-id`, `:retry-mode :all-after`) →
  observation source: dedup gate on `$$transcript-source-ledger` by line identity, then fan-out
  writes across four partitions: ledger row, file-cursor row, run counters, conversation entry,
  tool-call entry (488–512).
- Execution layer is **client-side** (no executor in the module): `harvest-transcripts!` non-OC body
  (912–936) walks files, builds observations (pure, 380–413), appends them, appends lifecycle claims.
  `start-transcript-watch!` non-OC body (1070–1134) polls files on a daemon thread with an in-memory
  cursor atom, persisting cursors only via the topology's file-state rows.

---

# Template checks

## Redundant conditionals
<!-- if every branch of an <<if, <<cond, or <<switch does the same operation with only a variable differing, replace with a single operation using that variable differing -->

**Check:** every branch of an `<<if`/`<<cond`/`<<switch` doing the same operation with only a variable differing must be a single operation.

**Lines:** 473–478 (May).

```clojure
(<<if (empty? *errors)
  (initial-run-row *request :> *run-row)
  (local-transform> [(keypath *request-id) (termval *run-row)] $$transcript-runs))
(<<if (not (empty? *errors))
  (rejected-run-row *request *errors :> *run-row)
  (local-transform> [(keypath *request-id) (termval *run-row)] $$transcript-runs))
```

**Trace:** every request takes exactly one of two complementary `<<if`s; both branches end in the
identical `local-transform>` with only the `*run-row` construction differing. This is the exact
pattern the check forbids — it should be one `<<if`/`else>` (or a plain Clojure fn) emitting
`*run-row`, followed by a single `local-transform>`.

**Verdict: FAIL** (localized rewrite).

## Consecutive keypath
<!-- (keypath *a) (keypath *b) → (keypath *a *b) -->

**Check:** `(keypath *a) (keypath *b)` must be `(keypath *a *b)`.

**Lines:** 507 (May): `[(keypath *conversation-id) (keypath *line-key) (termval *conversation-entry)]`.

**Trace:** two consecutive single-key `keypath` navigators on the same path; must be
`(keypath *conversation-id *line-key)`.

**Verdict: FAIL** (one-line fix).

## Select-compute-transform
<!-- local-select> followed by computation followed by local-transform> with termval — replace with +compound and an aggregator when possible -->

**Check:** `local-select>` → compute → `local-transform>` with `termval` should be an aggregator/path-level update when possible.

**Lines:** 483–486 (claim fold) and 501–504 (counter increment), both May.

**Trace (501–504):** the run row is read in full, `increment-run-counts` (438–444) does
`(update :observed-line-count (fnil inc 0))` + conditional `parse-error-count` inc + timestamp, and
the whole row is `termval`'d back. The counter updates are expressible as a single
`local-transform>` with `(term inc)` on the two count fields (no read of the full row needed);
skill rule "Minimize storage I/O" says do not re-read what a transform can update in place. Same
shape at 483–486 for the status fold. Beyond I/O, the read-modify-write shape is what makes these
writes non-idempotent under retry (see Stream topology idempotency).

**Verdict: FAIL.**

## Unnecessary nil->val
<!-- navigators handle nil as empty collection — do not add nil->val unless the next navigator requires a non-nil value -->

**Check:** no `nil->val` unless the next navigator requires non-nil.

**Lines:** none — the module contains no `nil->val` (whole-file scan). Nil-handling is done in
Clojure fns (`fnil`, `or`), which is legitimate.

**Verdict: PASS.**

## :allow-yield?
<!-- local-select> or select> that iterates a subindexed structure should include {:allow-yield? true} when iteration can exceed ~100 entries -->

**Check:** every range-iterating `local-select>`/`select>` over a subindexed structure needs `{:allow-yield? true}` when it can exceed ~100 entries.

**Lines:** 483, 493, 501 — the only topology reads; all are top-level point lookups
(`[(keypath *k)]`), no range navigators, no subindexed structures anywhere in the module.

**Trace:** a point `keypath` read on a top-level key is one RocksDB seek; no iteration occurs, so
no yield is required. (The *absence* of any subindexed structure is itself a failure — recorded
under "Non-subindexed collections" — but this specific check has no violating read.)

**Verdict: PASS** (vacuous).

## Non-subindexed collections without size limits
<!-- every write to a non-subindexed inner collection must have an explicitly enforced max size, else subindex -->

**Check:** every non-subindexed inner collection written by the module must have an enforced size cap.

**Lines:** 465 (`(declare-pstate n $$transcript-observed-conversations {String Object})`),
507 (write `[(keypath *conversation-id) (keypath *line-key) (termval ...)]`), 426–436
(`conversation-entry`), 574–578 (`read-conversation`). All May.

**Trace:** the value under each conversation-id is a plain Clojure map of line-key → entry. Every
new message in a conversation does an unsubindexed inner write: RocksDB reads the whole top-level
value, assocs one entry, rewrites the whole value. The spec (op 7 "Data growth": "Messages per
conversation up to thousands"; op 4: "per-conversation message lists can reach thousands of
entries... need ordered range access") guarantees this collection exceeds ~100 entries. There is no
code anywhere capping its size (contrast `append-bounded` at 177–183, which correctly caps
`:progress` at 50 — that collection PASSES). At 5,000 messages, every append rewrites a multi-MB
value, and `read-conversation` (574–578) deserializes the entire map for any read. Per the check's
own rule the schema must change to a subindexed structure (and, per spec op 7's ordering
requirement, a subindexed **sorted** map keyed by source order — see Spec S7).

**Verdict: FAIL** (schema restructuring required → major).

## Stream topology idempotency
<!-- trace through what happens if any event retries; every write must be idempotent or deduplicated -->

**Check:** every PState write and side effect must produce correct results if the stream event retries.

stream.md:9 — "A stream topology can retry a record even after all PState writes have completed and
committed... All writes for that record execute again." Walked per source:

1. **Request source (469–478).** `termval` of a deterministic row (`initial-run-row` uses
   `:request/time-ms` from the record, not `now-ms`) — pure-retry idempotent. **But** the write is
   unconditional: it never checks for an existing run row. A *duplicate depot record* with the same
   request-id (client retry after a lost `:append-ack` response, redelivery, double submit — spec
   explicitly requires this be non-corrupting, A5 / Run matrix "pending × duplicate submit") replays
   `initial-run-row` and `termval`s a fresh `:pending` row with zeroed counters over whatever exists.
   **Trace:** submit H1 → `:pending` → claim `:running` → 3 observations counted → `:complete` with
   `{:observed-line-count 3}` → the same request map is appended again → run row is now `:pending`,
   counters 0, terminal status destroyed. Violates I7 (terminal-final) and the matrix row that
   requires "still :pending, one logical run." A guard (`local-select>` + write-if-absent) is absent.
   **FAIL.**
2. **Claim source (480–486).** `fold-run-status` (185–193) does
   `(update :progress append-bounded (:progress claim) 50)` — a vector append, i.e. a
   read-modify-write. **Trace:** claim event with `:progress p1` processes fully, writes commit,
   Rama's progress tracking fails afterward (stream.md:9), record replays → run row already contains
   p1 → p1 appended again → duplicate progress entry. The claim record carries `:claim/id`
   (162–164) but the fold never consults it for dedup — the dedup key exists and is unused.
   stream.md:187: "Under at-least-once retry modes, include a command-id... and short-circuit if
   already seen." **FAIL.**
3. **Observation source (488–512).** The counter increment (501–504) is read-modify-write, guarded
   only by the ledger gate (493–494). **Trace (no double-count):** the ledger write (497) commits at
   the first partitioner boundary (498; stream.md:35 — writes commit at partitioner boundaries), so
   a replay after full completion sees `*existing-line` non-nil and skips — no double increment. The
   gate accidentally protects the increment. **Trace (permanent undercount):** event commits the
   ledger write (497) and file-state write (499), then the streaming batch on the run task fails
   during 501–504 → those writes are discarded (stream.md:92) → retry replays from `source>` → gate
   now sees the committed ledger row → the entire `<<if` body is skipped → `observed-line-count` is
   **never** incremented for that line, permanently. Violates spec op 5 ("Counters on the run must
   agree with what the run actually processed") and op 1's done-criterion (3 observed / 1 parse
   error). **FAIL.** (The same mechanism loses entire views — see next check.)

**Verdict: FAIL** (1 and 2 are localized guards; 3 is structural).

## Partial failure in stream topologies
<!-- writes to multiple PStates across multiple partitions: can partial failure + retry leave writes permanently unexecuted? -->

**Check:** for each stream event writing multiple PStates across partitions, partial failure + retry must not leave any write permanently unexecuted.

**Lines:** 488–512 (May). The observation event spans **five** partitions in sequence:
gate+ledger on `|hash *line-key` (492–497) → file-state on `|hash *file-state-key` (498–499) →
run counters on `|hash *request-id` (500–504) → conversation on `|hash *conversation-id` (505–507)
→ tool-call on `|hash *tool-call-id` (509–512). All downstream writes are *inside* the
`(<<if (nil? *existing-line) ...)` dedup gate evaluated on the first task.

**Runtime trace:** stream.md:90–96 — retry replays the whole event from `source>`; writes already
committed on other tasks via prior partitioner hops are NOT rolled back, and each partitioner
boundary commits the writes before it.

1. Observation for line L arrives; gate at 493 finds no ledger row; ledger write at 497 executes.
2. `(|hash *file-state-key)` at 498 commits the ledger write (stream.md:35).
3. The worker holding the conversation task dies before the write at 507 executes (or any
   streaming-batch failure occurs after step 2 and before tree completion).
4. Retry: the event replays from `source>` (488). `local-select>` at 493 now finds the
   **committed** ledger row → `*existing-line` is non-nil → the `<<if` body never runs.
5. Result, permanent: L exists in `$$transcript-source-ledger` but has **no conversation entry, no
   tool-call entry, no counter contribution** (and/or no file-state advance, depending on where the
   failure landed). No later event can repair it: every future duplicate append of L hits the same
   gate and skips. `:retry-mode :all-after` (488) amplifies exposure — all subsequent records on the
   partition also replay through the gate.

This breaks I4's core sentence — duplicates "MUST collapse to **one** materialized record in every
derived view" — by collapsing to **zero** in some views, and breaks op 7's `:complete` guarantee
("every parseable line of the walked corpus is represented exactly once"). The dedup gate is at the
wrong altitude: dedup must be enforced per-view at each write site (all the `termval` view writes
are naturally idempotent and could run unconditionally; only the counter needs a per-task seen-set),
or the whole pipeline belongs in a microbatch topology whose cross-partition atomicity makes the
gate sound. Either way the event structure changes.

**Verdict: FAIL — major** (restructuring of the observation flow required).

## Single depot append per client operation
<!-- each client write operation must call foreign-append! exactly once; extra writes must be server-side -->

**Check:** each client write operation calls `foreign-append!` exactly once; additional depot writes happen server-side.

**Lines:** 536–555 (append helpers), 912–936 (harvest orchestration), 1070–1134 (watch), May.

**Trace:** each *write operation* is a single append: submit = one `foreign-append!` (540), one
lifecycle status = one append (547), one observation = one append (554). `harvest-transcripts!` is
an executor loop, not a single write op — the spec itself models the lifecycle as separate depot
events (op 5: submit / claim / progress / terminal) and explicitly defines the crash-in-between
behavior as acceptable: Run matrix "running × executor dies → stays `:running` (stale)... idempotent
rerun is the recovery path", Ambiguity A3. I attempted the falsifying scenario: client dies after
the request append, before the `:running` claim → run shows `:pending` forever; after `:running`,
before terminal → `:running` forever; both are states the spec names and permits, and rerun with a
new request-id recovers via I4. No two appends exist whose atomicity the spec requires.

**Verdict: PASS.** (The *missing claim arbitration* on this path is a real spec failure, recorded
under Spec S5 — it is a missing mechanism, not a multi-append atomicity violation.)

## Application-state caches survive restart
<!-- for each in-process cache holding application state: name the durable source and the concrete rebuild path -->

**Check:** every in-process cache must have a durable source and a concrete rebuild path.

**Lines (May):** watch in-memory state — `known-at-start` (1076; OC twin at 969), `offsets` atom
(initialized 1091–1094, advanced 1105), `initialize-offset` rebuild path (1078–1090) reading
`read-source-file-state` (569–572) ← durable source `$$transcript-source-ledger` `"file:..."` rows
written at 498–499.

**Trace (rebuild path works for the ingested case):** watch restarts → file is in `known-at-start`
→ `initialize-offset` finds `:source/last-byte-offset` from the ledger → resumes; re-emitted
overlap folds away via the gate. This matches the spec's "Restart resume" bullet for known files.

**Trace (rebuild path insufficient — lines silently lost):** file F is created *after* watch W1
starts; the CLI writes 3 complete lines; W1's process dies before its next poll reaches F. No
observation was ever appended for F, so **no file-state row exists** — the durable source has
nothing for F. Watch W2 starts: F now exists at start → `known-at-start` contains it →
`initialize-offset` cond (1082–1090): no ledger state, `created-after-start?` false, no
`:backfill?` → **baseline `(.length file)` = EOF**. The 3 lines are never ingested and no ledger
note records the skip. Spec op 2: catch-up content must be tailed and missed windows must be
visible — "possibly late, never silently lost without a ledger trace". The rebuild path only covers
files with ≥1 ingested line; for the rest it silently converts "watched, pending first poll" into
"historic, skipped".

Second falsifying scenario, same cluster: file deleted and **recreated at the same path** (new
inode). Spec File-cursor matrix: "treated as a brand-new file (by inode), read from byte 0."
Implementation: `known-at-start` is a set of `File` objects = **path identity** (1076) → recreated
file is "known" → `new-file?` false → no ledger row under the *new* inode's key → EOF baseline →
the recreated file's content from byte 0 is silently lost. The inode-based identity (`file-id`,
207–216) exists but is not consulted for the baseline decision.

Third, same cluster: in-memory cursor advanced **before** the appends — 1105 `(swap! offsets assoc
file next-offset)` precedes the `doseq` append at 1106–1107. A transient append failure mid-`doseq`
(depot briefly unavailable) is caught by the thread's `catch Throwable` (1115–1121), which appends a
`:failed` status and **continues polling** — but `offsets` already points past the unappended lines,
so this watch never re-reads them. They are lost to the watch with only a run-level `:failed` mark
(which a later `stop!` then overwrites — see Spec S2).

**Verdict: FAIL** (durable source exists but the rebuild path provably loses observed-window lines
in three traced scenarios).

## No reimplementation of built-in operations
<!-- custom code duplicating Rama built-ins is FAIL -->

**Check:** no custom code duplicating functionality Rama already provides.

**Lines:** 557–559 (May):

```clojure
(defn select-pstate-one
  [pstate path]
  (first (foreign-select path pstate)))
```

**Trace:** this is `com.rpl.rama/foreign-select-one` re-implemented via `foreign-select` + `first`;
every read helper (562–582) routes through it. Hand-rolled duplicate of a built-in. (Other helpers
scanned: `append-bounded`, `expand-home`, `blank-string?` have no Rama built-in equivalents;
keyword-accessor wrappers at 173–175/446–456 are dataflow plumbing, not reimplementations.)

**Verdict: FAIL** (mechanical substitution).

---

# Spec conformance checks (May spec, IMPLICIT_SPEC.md)

## S1. I1 — Observation-only

**Lines:** whole module; file I/O at 334–360, 380–413, 938–960; threads at 1108–1125.
**Trace:** all filesystem access is read-only (`FileInputStream`, `RandomAccessFile` mode `"r"`,
`file-seq`, `.length`, `Files/readAttributes`); no subprocess spawn, no HTTP, no writes/renames/
deletes of watched paths; redaction (754–771 of dogfood/llm.clj) is a pure local function. Opaque
`oc-runtime/*` calls assumed append/read-only (see Assumptions).
**Verdict: PASS.**

## S2. I7 — Monotonic run lifecycle / no terminal regression

**Lines:** 185–193 (`fold-run-status`), 480–486 (claim source), 469–478 (request source),
1115–1134 (watch error/stop path). All May.
**Trace 1 (late progress flips terminal):** `fold-run-status` assocs `:status (:status claim)`
unconditionally — no check against `terminal-statuses` (55–56, defined and never used by the fold).
Run is `:complete`; a delayed/redelivered `:running` progress claim processes → row reads
`:running`. Spec op 5: "a stale `:progress` arriving after `:terminal` must not flip `:complete`
back to `:running`." Violated.
**Trace 2 (watch: `:failed` → `:cancelled`):** poll thread hits a transient `Throwable` → appends
`:failed` (1116–1121) — a *terminal* status — then **keeps polling** (loop continues; `stop?`
untouched). User later calls `stop!` (1129–1134) → appends `:cancelled` → fold overwrites the
terminal `:failed`. Terminal flip-flop, forbidden by I7.
**Trace 3 (duplicate submit resets a terminal run):** request source `termval`s `initial-run-row`
unconditionally (473–475) — see Stream topology idempotency item 1. `:complete` → `:pending`.
**Verdict: FAIL.**

## S3. I2 — Nothing persists unredacted

**Lines:** 293–296 (`redacted-preview`), 324–332 (parse-error branch), 309 (`:transcript/redactions []`),
315 (redaction call), 497 (persistence). All May.
**Trace 1 (raw bytes persist for malformed lines):** `redacted-preview` is
`(subs s 0 (min 200 (count s)))` — a **truncation, with no redaction**. A malformed line such as
`{"api_key": "sk-live-..."` (writer killed mid-line, then harvested; or any invalid-JSON line
containing a secret in its first 200 chars) throws in `parse-json-line` before
`redact-provider-payload` is ever reached (314–315), lands in the catch branch, and its raw prefix
is stored in `:transcript/redacted-preview` (331) on the observation, which is `termval`'d wholesale
into `$$transcript-source-ledger` (497) and readable via `read-ledger-line` (565–567). I2 is
explicit: parse-error rows persist "redacted best-effort preview... — never raw bytes." Violated by
construction; the spec's planted-secret test misses it because the planted secret sits in a
well-formed line.
**Trace 2 (redaction metadata never recorded):** `:transcript/redactions` is hardcoded `[]` (309)
and never updated; `redact-provider-payload` (llm.clj 754–771) returns only the masked payload, so
no span/kind/length can ever populate it. I2: redaction events "are recorded as metadata
(span/kind/length) that answers 'what was masked?'" — unanswerable for every row.
**Trace 3 (parsed rows):** payload stored is post-`redact-provider-payload` (315–322) — key-name
based masking covers the tested `tool_use` `api_key` case. That narrow path holds.
**Verdict: FAIL** (trace 1 is a hard invariant breach; trace 2 a required-field breach).

## S4. I5 — Byte-correct, newline-gated consumption

**Lines (May):** harvest call site 918 (`(mapcat #(read-jsonl-observations request % 0) files)`),
`read-complete-appended-lines` 938–960, file-state advance 415–424 + 498–499.
**Trace 1 (harvest emits the trailing partial line):** `read-jsonl-observations` emits the
unterminated tail as a final observation (reader's `:eof` branch — June lines, but the May-era
contract is proven by `read-complete-appended-lines`, May, whose whole job is to `butlast` that
partial away — and the May harvest path does **not** use it). Harvest of a file whose writer is
mid-line (spec op 1 concurrency bullet: partial trailing line "is skipped per I5"): the fragment
becomes a parse-error observation, is appended (926–927), passes the gate (it is novel), and
materializes as a ledger row, a conversation entry under the path-fallback id, and a counter
increment. Worse, its file-state row (415–424) advances `:source/last-byte-offset` to the
**fragment end, mid-line**. A subsequent watch initializes from that cursor (1083–1084) and reads
the *remainder* of the line as a second phantom record when the newline arrives. The true complete
line — offset at fragment start, full content — is **never** ingested: re-reads at its offset
produce the fragment hash or start past it. Permanent corruption of exactly the kind I5 exists to
prevent ("must not be emitted, must not become a parse error, must not advance the file cursor past
itself").
**Trace 2 (watch TOCTOU race emits a partial):** `read-complete-appended-lines` (938–960) snapshots
`len = (.length file)` (941), then reads observations to the **live** EOF (945), then checks the
byte at `len - 1` (946–950). Interleaving: at T0 the file is N bytes ending `\n` (len = N); at T1
the CLI appends a partial line (no newline); at T2 the read sees N+k bytes → last observation is the
fragment; the newline check inspects byte N−1 = `\n` → `ends-with-newline?` true → **all**
observations returned including the fragment, `:next-offset` = N. The fragment is appended as a
phantom parse-error record; the next poll re-reads from N and later ingests the completed line as a
*different* identity — both rows persist. The check and the read must share one snapshot (bound the
read at `len`); they don't. Watch polls every 100 ms (1077) against actively-written files for the
lifetime of usage, so this race is a when, not an if.
**Verdict: FAIL.**

## S5. Op 5 — Claim/lifecycle: at-most-once execution, no torn reads

**Lines (May):** 161–171 (claim record), 480–486 (fold), 912–916 (harvest claims itself `:running`),
926–933 (terminal vs counter race).
**Trace 1 (no claim arbitration):** spec op 1/op 5: "A request-id is claimed/executed at most once
even with multiple executor instances... Two executors racing to claim → exactly one wins; the
loser skips." There is no mechanism: `harvest-transcripts!` appends `:running` unconditionally
(914–916) without testing whether the run is already claimed, and the claim fold (485–486) accepts
any claim over any state. Two processes invoking harvest with the same request both execute fully.
(Materialized views are rescued by the gate, but the invariant — at-most-once *execution* — is
simply not implemented, and run counters/progress interleave from both executors.)
**Trace 2 (torn `:complete`):** op 6 forbids "`:complete` with counters missing the final tally."
Harvest appends all observations at `:append-ack` (550–555 default) — durable append only, **no
PState-visibility guarantee** (stream.md:167) — then immediately appends the `:complete` claim
(928–931). The claim event routes once (`|hash *request-id`) and folds; each observation event must
first traverse the line-key gate task before reaching the run task (492→500), and there is no
cross-depot ordering. The `:complete` claim therefore races ahead of in-flight observation events:
`read-run` returns `:status :complete` with `observed-line-count` below the final tally, which then
ticks upward *after* terminal. Exactly the torn state op 6 names. (The harvest's own
`await-materialized` at 932–933 waits only for `:status = :complete`, so even the in-module caller
observes torn counters.)
**Trace 3 (orphan lifecycle events):** claim for an unknown request-id → `local-select>` (483)
yields nil → `<<if (some? *run-row)` skips → nothing fabricated. Matches the matrix row. This
sub-case passes.
**Verdict: FAIL** (traces 1 and 2).

## S6. Op 1 — Harvest edge cases / I8 fail-isolation

**Lines (May):** 912–936, 334–360.
**Trace 1 (unreadable file kills the run):** `walk-jsonl-files` collects files; one file's
permissions deny reading → `FileInputStream` ctor throws inside `read-jsonl-observations` →
exception propagates out of the lazy `mapcat` when `reduce` realizes it (919–925) → no `:failed`
status is appended, no ledger note recorded, the exception reaches the caller, and the run is stuck
`:running` forever. Spec I8: "a bad file (perms, deletion mid-read) doesn't kill a run. Failures are
recorded in the audit ledger and processing continues with the remainder." Both halves violated.
Same trace for deletion mid-walk (op 1 edge: "error ledger-recorded... run continues").
**Trace 2 (empty file not ledger-recorded):** spec op 1 edge: "Empty file → zero observations, file
still ledger-recorded as seen." File-state rows are written only per novel observation (496–499);
an empty file yields zero observations → no ledger trace of any kind. `read-source-file-state`
returns nil. Violated.
**Trace 3 (`:since` floor):** spec op 1: request carries "optional `:since` time floor (defaults to
epoch)." `transcript-request` (83–103) has no `:since` field; nothing filters by time anywhere.
Missing parameter (behavior happens to equal the epoch default, but a caller-supplied floor is
silently dropped).
**Trace 4 (empty dir / no matches):** `walk-jsonl-files` → `[]` → zero appends → `:complete` claim
→ run terminal with zero counters. Matches spec. Passes.
**Verdict: FAIL** (traces 1–3).

## S7. Op 7 — Read conversation: content, order, dedup

**Lines (May):** 426–436 (`conversation-entry`), 505–507 (write), 574–578 (`read-conversation`),
316/326 (fallback id), 232–243 (id extraction).
**Trace 1 (no content):** spec op 7: "Returns the observed messages of one conversation: parsed
message rows... **redacted content only**," and it "is the base for rendering/rehydration."
`conversation-entry` carries line-key, path, offsets, event-type, uuid, timestamp, error-kind —
**no payload field at all**. Rendering a conversation requires N ledger point-lookups by line-key,
which op 7 expressly rules out ("needs efficient ordered range access per conversation-id, not N
point lookups"). The spec's secret-absence test passes vacuously — there is no content in which a
secret could appear.
**Trace 2 (no ordering):** entries live in an unsorted map keyed by the composite line-key string;
`read-conversation` returns that map as-is. Spec: "in source order (byte offset within their
file)," and the matrix requires a late lower-offset arrival to read in source order. No sort
anywhere; even the key's embedded offset is a plain decimal string ("10" < "9" lexically), so key
order is meaningless. Order is delegated, silently, to the caller.
**Trace 3 (dedup):** duplicate append → gate skips → map unchanged; even if a duplicate slipped
through, the inner `keypath *line-key` write is idempotent per identity. Re-harvest keeps count at
2 (tested contract). Passes — except via the Partial-failure loss (S-check above) the count can be
permanently *low*, which the matrix's "exactly once" also forbids.
**Trace 4 (parse errors visible under fallback id):** Conversation matrix: a row without a
conversation-id (parse error) "stays empty for every id — ledger-visible but
conversation-invisible." Implementation assigns parse errors `conversation-id = (.getPath file)`
(326) and the topology writes conversation entries unconditionally for every novel observation
(505–507) — so parse errors **do** materialize as conversation rows under the file-path id.
Additionally the fallback for parsed init-less rows is the full path, not the spec's "file-basename
fallback, **flagged**" (no flag field exists), and the id extraction or-chain (232–243) falls
through to `:parentUuid` / `:uuid` — message-level identifiers — fragmenting init-less files into
per-message "conversations" instead of the basename fallback.
**Verdict: FAIL** (traces 1, 2, 4).

## S8. Op 8 — Read tool call

**Lines (May):** 268–287 (`parsed-tool-use-blocks`, `tool-call-index-rows`), 446–448
(`obs-tool-call-first`), 508–512 (write).
**Trace 1 (only the first block is indexed):** spec op 4/op 8: "a single assistant message with
several `tool_use` blocks is still one line record; **each block indexes separately** — one indexed
entry per block." `obs-tool-call-first` takes `(first (tool-call-index-rows obs))`; the topology
writes exactly that one row (508–512). A line with blocks `tool-1`, `tool-2`:
`read-tool-call "tool-2"` → nil, forever — no later event re-indexes it (the gate blocks the line's
duplicates). Violated.
**Trace 2 (redacted inputs not stored):** spec op 8: returns "at minimum its tool name
(`:tool-call/name`) and **redacted inputs**." `tool-call-index-rows` (280–287) copies id, name,
source, conversation-id, uuid, line-key, path, offset — the block's `:input` is dropped. The
planted-`api_key`-absent test passes vacuously. Violated.
**Trace 3 (id collisions, A6):** `termval` last-write-wins → single deterministic-at-read answer,
never a merge. Acceptable per A6.
**Verdict: FAIL** (traces 1, 2).

## S9. Op 9 — Audit-ledger query

**Lines (May):** 463–465 (schemas), 493–499 (only ledger writes), 565–572 (only ledger reads).
**Trace:** the ledger is a flat `{String Object}` map keyed by composite line-key strings,
hash-partitioned per key. The spec's required reads: per-file offset-ordered range access, per-run
totals (files-seen / files-parsed / bytes-processed / messages-emitted), watch liveness, notes for
skipped partials / per-file failures / disconnections, redaction history per row, "what was
masked?". Implemented surface: point lookup of one line row (565–567) and one cursor row (569–572).
Per-file range queries are impossible (keys are hashed across tasks; no per-file subindexed
structure exists); per-run rows, notes, and liveness heartbeats are never written by any code path
(no progress claims are ever appended by harvest or watch); redaction history is the hardcoded `[]`
(S3). Op 9's defining question — "what did Softland ingest from my Claude history last week?" — has
no supporting read path or write path. This is not a thin implementation of op 9; op 9 is absent
beyond the per-line row, and the schema shape forecloses it without restructuring.
**Verdict: FAIL** (major — requires schema restructuring, e.g. ledger keyed by file-key with
subindexed sorted offset map, plus per-run note rows).

## S10. Op 10 / I6 — Pure builder boundary

**Lines:** 380–413 (builder), 524–527 (no append in reach), boundary helpers 561–582. May
signature/callers, June internals.
**Trace:** `read-jsonl-observations` performs no depot appends and no PState writes; building
observations leaves `read-run` nil (no run row is created by observations' absence) and
conversations empty — matches the tested boundary (spec I6). Offset beyond EOF: channel positioned
past end → first read returns −1 → `:eof` with zero buffered bytes → `[]`. Matches "Offset beyond
EOF → empty."
**Verdict: PASS.** (Offset-mid-line fabricating a fragment identity is real but sits on June reader
lines — see out-of-scope notes.)

## S11. I3 / I9 — Identity composition, non-retraction

**Lines (May):** 195–230 (hashing, keys), 207–216 (`file-id`), whole module for deletes.
**Trace (I3):** line identity = `source : pr-str(file-id) : byte-offset : line-hash` (218–226) —
all four spec components present; file identity is `unix:dev,ino` (rename-surviving), with a
canonical-path fallback only when the attribute read throws (non-POSIX stores). Same
(file-id, offset) with a different hash yields a distinct key → distinct record, as the matrix
requires. **Trace (I9):** no code path deletes or retracts from any PState (no `NONE>`-style
removal, no key deletion anywhere in the module); rows from deleted files persist.
**Verdict: PASS.**

---

# Skill design-goal checks

## D1. Balanced computation — observation depot funnels every run through one task

**Lines:** 461 (`(declare-depot setup *transcript-obs-depot (hash-by :transcript/ingest-request-id))`),
492 (`(|hash *line-key)`). May.
**Trace:** every observation of a harvest run carries the same request-id, so the depot partitioner
lands the entire 10^5–10^6-line burst (spec op 1 throughput) on **one** task, which then re-hashes
each record by line-key (492) — the depot partition key is never used for a co-located read or
write; it exists only to be immediately discarded. Skill goal 1: partition depots by the key used
for PState lookups; avoid funneling high-throughput writes through one task. The depot should hash
by the line key (or the file key).
**Verdict: FAIL.** (Same-key-then-`|hash` also occurs benignly on the request/claim depots
(459–460, 471, 482): the depot already routes by request-id, so the explicit `|hash` is a redundant
hop — harmless but pure overhead.)

## D2. Topology choice — stream chosen where microbatch is indicated

**Lines:** 462 (stream topology), 488–512. May.
**Trace:** spec latencies are poll-grade: acceptance ~1 s, materialization "within seconds,"
completion batch-grade. Nothing requires single-digit-ms updates, and the only coordination used is
polling (`await-materialized`), not ack-driven read-back. The workload is 10^5–10^6-record bursts
needing dedup ("exactly once in every view") and multi-PState consistency — precisely the skill's
microbatch criteria ("exactly-once, cross-partition atomicity, higher throughput"). Microbatch
would dissolve the two major failures above (the gate becomes sound under exactly-once semantics;
counters and views update atomically per batch). The stream choice is the root cause, not a
neutral alternative.
**Verdict: FAIL** (architectural).

## D3. Colocation / I/O efficiency

**Lines:** 488–512, 463–466. May.
**Trace:** each observation event costs up to 4 partitioner hops + 2 point reads + up to 5 writes.
Per the skill's cost model that is ~4 network hops and ~1 ms of seeks per line, × 10^6 lines per
harvest, on every re-harvest (duplicates still pay the gate hop + read). Ledger line rows, file
cursor rows, and per-file audit ranges all want file-key partitioning (line keys are
file-key-prefixed already, 218–226) — one hop instead of two and free per-file locality. The
conversation/tool hops are inherent to their access patterns, but the chosen flat schemas
additionally force whole-value rewrites (see Non-subindexed check).
**Verdict: FAIL** (subsumed by the schema/topology findings; recorded for completeness).

---

# Falsification protocol sections (per CLAUDE.md review protocol)

## Failure modes attempted (scenario → outcome)

1. Retry after first partitioner boundary in obs event → conversation/tool/counter writes lost
   forever (Partial failure — FAIL).
2. Record replay after full commit (stream.md:9) on claim with `:progress` → duplicate progress
   entry (Idempotency — FAIL).
3. Duplicate submit of same request-id after `:complete` → run reset to `:pending`, counters zeroed
   (S2 — FAIL).
4. Late `:running` heartbeat after `:complete` → terminal regression (S2 — FAIL).
5. Watch transient error then `stop!` → `:failed` overwritten by `:cancelled` (S2 — FAIL).
6. Malformed line containing a secret in first 200 chars → raw bytes persisted in ledger preview
   (S3 — FAIL).
7. Harvest over a file being written mid-line → fragment ingested, cursor advanced mid-line, true
   line never ingested (S4 — FAIL).
8. Watch poll racing CLI append (length snapshot vs live read) → partial line emitted (S4 — FAIL).
9. Two executors harvesting the same request-id → both execute; `:complete` racing in-flight obs →
   torn status/counters (S5 — FAIL).
10. Unreadable/deleted file mid-harvest → run stuck `:running`, nothing recorded, siblings
    unprocessed (S6 — FAIL).
11. Watch restart with a never-ingested file / file recreated at same path → EOF re-baseline,
    silent loss (Caches-survive-restart — FAIL).
12. Line with two `tool_use` blocks → second block unaddressable forever (S8 — FAIL).
13. Orphan claim for unknown request-id → correctly skipped (passes).
14. Offset-beyond-EOF build → empty (passes).

## Writers / readers / clearers

| State | Writers | Readers | Clearers |
|---|---|---|---|
| `$$transcript-runs` row | request source (473–478, unconditional termval); claim source (485–486, unguarded fold); obs source (502–504, gated increment) — **three uncoordinated writer families** | `read-run` (561–563); claim fold; counter increment | none (correct per I9/A3; stale `:running` is spec-sanctioned) |
| `$$transcript-source-ledger` line row | obs source (497, gated termval) | gate (493); `read-ledger-line` (565–567) | none (correct) |
| `$$transcript-source-ledger` `file:` cursor row | obs source (499, per novel obs) | `read-source-file-state` (569–572); watch `initialize-offset` (1081–1084) | none |
| `$$transcript-observed-conversations` | obs source (507) | `read-conversation` (574–578) | none |
| `$$transcript-tool-call-index` | obs source (512) | `read-tool-call` (580–582) | none |
| watch `offsets` atom | init (1091–1094), poll (1105) | poll (1100–1102) | process death; rebuild partial (FAIL above) |
| watch `stop?` atom | `stop!` (1130) | poll loop (1112) | n/a |

Multiple-writer conflict on the run row is real and traced (S2, S5): request-source termval can
erase claim/counter writes; claim merge of `:counts` (191) would overwrite topology-maintained
counters if any claim carried counts (non-OC path never sends them — latent, noted as doubt).

## Async ordering risks

- `:complete` claim vs in-flight observation events (different depots, different hop depth) — torn
  run state (S5, traced).
- File-cursor writes for different lines of one file arrive at the cursor task from different gate
  tasks; stream.md:31 orders only same-pair task messages → cursor can regress to a lower offset.
  Self-healing (watch re-reads fold away via gate) but adds phantom re-reads; noted, not counted.
- `:individual` retry (default; 459–460 depots) re-queues a failed claim while later claims
  proceed → progress/terminal reorder feeding the unguarded fold (S2).

## Error-path cleanup

- Harvest: none — any reader exception aborts with run stuck `:running`, no `:failed` claim, no
  ledger note (S6 FAIL).
- Watch: `catch Throwable` appends `:failed` then continues polling with cursors already advanced
  (1105) — no rollback of the in-memory cursor, terminal status later overwritten by `stop!` (S2,
  caches FAIL).
- `close-transcript-runtime!` (529–534): closes IPC, swallows exceptions — acceptable for a
  harness; no PState cleanup attempted (correct per I9).

## Open doubts

- A request with nil/blank request-id reaches `rejected-run-row` and is written under a nil key
  into a `{String Object}` PState (476–478). If Rama enforces the key schema at write time this is
  a poison record that fails and retries forever under `:individual` mode. **Inference, not
  verified** against Rama's schema-enforcement behavior; flagged, not counted.
- `fold-run-status` merges `:counts` from claims into the same fields the topology increments
  (191 vs 502–504) — two writer families for one field. Dormant on the May path (no claim carries
  counts); becomes live the moment any executor sends counted progress. Flagged.
- `pr-str` of the `file-id` map inside the line key (220) assumes stable key order; stable for
  2-entry array-maps as constructed, but any future change to `file-id`'s shape silently changes
  every line identity. Flagged.
- `transcript-sources` includes `:future/source` (50) whose `(name ...)` is `"source"` — no key
  collision with the `"file:"` prefix today; fragile naming convention. Flagged.

---

# Out-of-scope observations (June era) — one line each

1. `read-jsonl-observations` reader internals (385–413) emit the unterminated trailing line as an
   observation (`:eof` branch, 409–411) — the newline gate lives only in callers.
2. Docstring (363–379) claims byte-length 0 for an unterminated trailing line; code (403) sets it
   to `n` — doc/code contradiction.
3. OC import loop (773–849) is fully synchronous client-side per line: append → poll decision →
   poll completion, with `Thread/sleep` retries — throughput per spec op 1 (10^6 lines) looks
   implausible; not traced (opaque oc-runtime).
4. OC harvest (851–905) re-reads every file from offset 0 each run, relying entirely on import-side
   dedup (opaque).
5. OC watch poll thread sets `stop?` and throws on import failure (1026–1034), then the catch
   appends `:failed` — and a later `stop!` still appends `:cancelled` (1057–1063): same terminal
   flip shape as May's S2, on June lines.
6. `object-container-runtime?` dispatch (597–599, 909–911, 1067–1068) routes on a key's presence in
   the runtime map — stringly-typed mode switch.
7. June OC `offsets` atom keys by inode-based file-key (987–992), fixing the May path-keyed cursor;
   `known-at-start` (May, 969) still path-based, so the recreate-at-path loss survives in the OC
   path too.

## Assumptions made about opaque object-container calls

- `oc-runtime/append-transcript-control!`, `append-object-container-request!`,
  `append-transcript-file-state!`: assumed each is a single `foreign-append!` to a depot owned by
  the object-container module, with no external side effects (I1-compatible).
- `oc-runtime/await-object-container-decision`, `read-transcript-file-offset`,
  `read-transcript-last-message`, `read-transcript-source-line`: assumed read-only foreign
  selects / polling reads.
- `transcript-adapter/transcript-observation-import-request`, `transcript-identity/*`: assumed pure
  functions of their inputs.

---

# Summary of failed checks (May era)

| # | Check | Severity |
|---|---|---|
| F1 | Partial failure: cross-partition dedup gate + stream retry permanently drops conversation/tool/counter/file-state writes (488–512) | **major** (restructure obs flow or move to microbatch) |
| F2 | Lifecycle not monotonic: unguarded fold, duplicate-submit reset, watch `:failed`→`:cancelled` flip (185–193, 473–478, 1115–1134) | **major** (multi-writer protocol on the run row) |
| F3 | I2 breach: raw bytes persist in parse-error preview; redaction metadata hardcoded `[]` (293–296, 309, 331) | minor-fix lines, **high-impact invariant breach** |
| F4 | Conversation view: unbounded non-subindexed inner map, no content, no source ordering (426–436, 465, 507, 574–578) | **major** (schema restructuring) |
| F5 | Tool-call index: only first `tool_use` block indexed; redacted inputs absent (276–287, 446–448, 508–512) | minor |
| F6 | Audit ledger (op 9) absent beyond per-line point row; no per-file range, no run notes, no liveness (463–465, 493–499) | **major** (schema restructuring) |
| F7 | No claim arbitration; torn `:complete` vs counters (161–171, 480–486, 912–933) | **major** |
| F8 | I5: harvest ingests trailing partial line and advances cursor mid-line; watch TOCTOU race emits partials (918, 938–960) | **major** (reader/caller contract restructure) |
| F9 | Watch restart/recreate: never-ingested and recreated-at-path files silently EOF-baselined; cursor advanced before append loses lines (969, 1076–1105) | **major** |
| F10 | Run counters count only globally-novel lines → re-harvest run reports 0 processed; retry can permanently undercount (500–504) | minor (A8 caveat) + structural via F1 |
| F11 | Template hygiene: redundant conditionals (473–478), consecutive keypath (507), select-compute-transform (483–486, 501–504), `foreign-select-one` reimplementation (557–559), obs-depot single-task funnel (461) | minor |
| F12 | Spec gaps: `:since` unsupported (83–103); empty file not ledger-recorded; parse errors conversation-visible under full-path fallback, unflagged (316, 326, 505–507); I8 fail-isolation absent (912–936) | minor each, I8 one **major-leaning** |

# Verdict

**major-fail** — multiple failures (F1, F2, F4, F6, F7, F8, F9) require restructuring rather than
line edits: the observation event's cross-partition dedup gate is unsound under stream retry
semantics, the run row has three uncoordinated writer families with no monotonicity protocol, and
the conversation/ledger schemas cannot meet the spec's read contracts (content, ordering, range
access, audit) without schema changes — the topology/schema shape, not individual lines, is what
violates the spec.
