# Test Validation

<!-- Phase 6. Fill in after tests are written in Phase 5. -->

> **RETROSPECTIVE artifact (2026-06-11).** Phase 6 run in retro mode against
> `test/app/server/rama/dogfood_transcript_test.clj` (written May 2026, pre-skill) and the
> retro `IMPLICIT_SPEC.md`. Protocol = the module's public foreign-facing functions and
> docstrings in `src/app/server/rama/dogfood/transcript.clj` (no separate protocol file).
> Static validation only; tests were not run. A fail verdict here is the retro result, not
> a fix-loop trigger.

Review the test source files. For each check, state pass or fail with evidence. Then emit one of three verdicts at the end of this artifact, per the rubric below.

## Minimize IPC launches

**Check (verbatim):** "Each create-ipc + launch-module! adds 30+ seconds. Default to one deftest with testing blocks for organization. Justify every additional deftest by naming the specific shared mutable state that would interfere — 'different operation' or 'different concern' is not a valid justification. Scenarios on disjoint keys do not interfere and belong in one deftest."

**Verdict: FAIL.**

The suite performs **4 IPC launches** where 1 suffices. `with-transcript-runtime` (test file lines 6–12) wraps `transcript/start-transcript-runtime!`, which calls `create-ipc` + `launch-module!` (module lines 514–527). Call sites:

1. `transcript-harvest-contract-test` (line 37) — keys: `"harvest-1"`, `"harvest-2"`, `"conv-1"`, `"tool-1"`, own temp dir.
2. `transcript-builder-boundary-test` (line 72) — keys: `"boundary-harvest"`, `"boundary-conv"`, own temp dir.
3. `transcript-watch-offset-contract-test`, first block (line 99) — keys: `"watch-harvest"`, `"watch-1"`, `"watch-conv"`, own temp dir.
4. `transcript-watch-offset-contract-test`, **second** `with-transcript-runtime` inside the same deftest (line 133) — keys: `"watch-unknown"`, `"existing-conv"`, `"new-conv"`, own temp dir.

Walk-through for interference: every scenario uses a unique temp directory (`temp-dir`, line 14) and disjoint request-ids / conversation-ids / tool-call ids, so all PState keys (`$$transcript-runs`, `$$transcript-observed-conversations`, `$$transcript-tool-call-index`, `$$transcript-source-ledger`) are disjoint across scenarios. The builder-boundary pre-assertions (`(nil? (read-run runtime "boundary-harvest"))`, line 84; `(empty? (read-conversation runtime "boundary-conv"))`, line 85) are key-scoped, so writes from other scenarios in a shared runtime cannot affect them. The watch background threads (poll-ms 10000) watch only their own temp dirs. No shared mutable state justifies any of the 3 extra launches. Per the rubric, all four scenarios belong in one deftest with `testing` blocks on one runtime. Consolidating 3 deftests / 4 runtimes into 1 is suite restructuring → this failure is **major** by the phase doc's rule ("Do NOT pick minor-fail when the fix requires ... restructuring the suite").

## Implicit spec coverage

**Check (verbatim):** "Read IMPLICIT_SPEC.md and verify every edge case and entity state × write combination is tested. List each one and the test that covers it. If any are missing, add tests." (Retro mode: missing cases are reported, not added.)

**Verdict: FAIL.** Coverage exists for the happy-path core but large May-scope regions have zero tests, and several covered-looking assertions do not actually pin the contract.

### Protocol functions and docstrings (quoted where they exist)

Public foreign-facing surface exercised or exercisable: `start-transcript-runtime!`, `close-transcript-runtime!`, `transcript-request`, `transcript-routing-key`, `append-transcript-request!`, `append-transcript-status!`, `append-transcript-observation!`, `harvest-transcripts!`, `start-transcript-watch!` (returns `{:poll-once! :stop! ...}`), `read-run`, `read-ledger-line`, `read-source-file-state`, `read-conversation`, `read-tool-call`, `read-jsonl-observations`, `await-materialized`. Only two carry docstrings:

- `read-jsonl-observations`: "Read JSONL lines from `file` starting at byte `start-offset`, producing one observation per line. ... Reads raw bytes and decodes UTF-8 explicitly. ... corrupting multi-byte characters AND inflating byte-length — which cascades into every downstream byte offset and line hash. This reader computes source identity from the actual byte slice instead: byte-offset = offset of the line's first byte; byte-length = content bytes + 1 for the trailing \n ...; line-hash = sha256 of the exact content bytes (terminator excluded)."
- `line-hash-bytes`: "Byte-correct source-identity hash of a line's raw content bytes (terminator excluded). Hashes the exact on-disk bytes so the hash is stable regardless of how the line decodes — the basis for dedup key (source, file-id, byte-offset, line-hash)."

**No test contains any multi-byte UTF-8 content.** Every fixture line (`valid-line`, lines 28–34; the literal `"{not-json"`; the 20-byte split at lines 119–122) is pure ASCII. The single documented reason this reader exists — byte-correct offsets/hashes under multi-byte UTF-8 (spec I5: "multi-byte UTF-8 content must not skew them") — is untested. Missing case.

### Covered (with citations)

| Spec item | Test |
|---|---|
| Op 1 harvest happy path: 3 lines, 1 malformed → `:complete`, result 3 appended, run 3 observed / 1 parse-error, conversation 2 | `transcript-harvest-contract-test`, lines 57–61 |
| Op 7: parse-error rows absent from conversation | same, line 61 (count 2 of 3) |
| Op 8: tool call by id, `:tool-call/name` "bash" | same, line 62 |
| I4 dedup: re-harvest with new request-id keeps conversation at 2 | same, lines 66–69 |
| I6 build/persist boundary: build → run nil, conversation empty; request append → `:pending`; single obs append → conversation 1 | `transcript-builder-boundary-test`, lines 83–96 |
| Op 6: unknown request-id → nil | `transcript-builder-boundary-test`, line 84 |
| Op 7: unknown conversation-id → empty | `transcript-builder-boundary-test`, line 85 |
| I5 watch partial line: 20-byte headless append emits nothing; completion emits exactly one | `transcript-watch-offset-contract-test` block 1, lines 119–127 (see caveat below) |
| Watch cursor: known file emits nothing on first poll after harvest | block 1, lines 117–118 (see caveat — confounded) |
| Op 3 cancel: stop! → `:cancelled` materialized | block 1, lines 128–131 |
| Watch EOF baseline for existing-unknown files | block 2, lines 145–146 |
| Watch new-file-after-start from byte 0 | block 2, lines 153–159 |

### Covered-looking but NOT actually pinning the contract

1. **I2 redaction assertions are vacuous** (harvest test, lines 63–64). The two projections scanned for `"never-store-me"` carry no payload at all: `conversation-entry` (module lines 426–436) has no content/payload field, and `tool-call-index-rows` (module lines 276–287) includes `:tool-call/name` but **no input field**. The secret cannot appear in those projections whether or not redaction ran. The one projection that persists the redacted payload — the source ledger, where `local-transform>` stores the full obs including `:transcript/redacted-payload` (module line 497) — is never read (`read-ledger-line` is never called in the test file). I2 says "Nothing persists unredacted ... a secret string placed in a tool_use input must not appear in any readable projection"; the suite never scans the readable projection where the payload actually lives. Spec op 8 also requires "redacted inputs" to be returned by `read-tool-call` — no test asserts inputs are present at all, so a projection with no inputs passes.
2. **Watch resume-from-ledger-offset is confounded by dedup** (block 1, lines 117–118). The assertion is `count == 1` after polling a file already harvested. If the watch wrongly re-read from byte 0, the re-emitted line has the same I3 identity and folds away under I4 — count is 1 either way. The cursor contract ("already-ingested lines are NOT re-emitted", spec op 2) is not distinguishable from "re-emitted and deduped" by this assertion. Pinning it requires reading run counters, the ledger, or `read-source-file-state` — none of which the test does.
3. **I5 partial-line intermediate state only partially pinned** (block 1, lines 119–121). A wrongly-consumed partial would become a parse-error obs whose conversation-id is the file-path fallback (module lines 324–332), invisible to `read-conversation runtime "watch-conv"` — so the `count == 1` assertion at line 121 is blind to it. The violation is only caught indirectly by the final `count == 2` await (a consumed partial would advance the cursor and the completed line would never assemble). Spec matrix rows "R-run: counters unchanged" and "R-ledger: no line row" for the partial are never read.
4. **Conversation assertions are count-only.** Spec op 7 requires messages "in source order (byte offset within their file)" with "redacted content only". No test asserts ordering, message fields, or that content is present at all. A content-less or mis-ordered conversation view passes every assertion in the suite.
5. **Re-harvest counters unread** (spec A8 acknowledges this): lines 66–69 never call `read-run runtime "harvest-2"`, so per-run counter semantics under re-harvest are untested.

### May-scope areas with NO coverage (missing cases, each named)

**Op 9 — audit ledger: zero coverage.** No test calls `read-ledger-line` or `read-source-file-state`. Per-line provenance (read-time, policy, redaction counts), per-run totals, partial-line notes, per-file failure notes — none asserted. This is an entire spec operation with no tests.

**Op 5 / I7 — run lifecycle: almost no coverage beyond `:pending` and `:cancelled`.**
- `pending × claim → :running` visible: never asserted (harvest goes straight to a `:complete` read).
- `terminal × late progress / duplicate terminal → no regression`: untested. (High value: `fold-run-status`, module lines 185–193, unconditionally assocs `:status`, so a late `:running` claim after `:complete` would regress — a real divergence a test here would catch.)
- `does-not-exist × orphan claim`: untested (module skips via `<<if (some? *run-row)`, line 484 — unverified by tests).
- `running × terminal :failed`: untested.
- `pending × duplicate submit same request-id` (A5, "must not corrupt ... no counter doubling"): untested.
- Rejected/invalid request → `:failed` run row (`rejected-run-row`): untested.
- Claim-at-most-once across executors: untested.

**Op 1 harvest edge cases — none tested:** empty directory (zero counts, not error); empty file; non-existent/unreadable path (I8 fail-isolated); file deleted mid-walk (I9); file rotated mid-run; `:since` floor (A2); disk full; concurrent harvests; **harvest-side trailing partial line (I5)** — only the watch path tests partial lines. Notably `harvest-transcripts!` calls `read-jsonl-observations ... 0` directly (module line 918), whose docstring says the partial trailing line IS returned as the last record ("Partial trailing line (no terminator) — last record", module line 410) — i.e., the harvest path appends partials, violating I5/spec op 1 ("the partial trailing line is skipped per I5"). A harvest-with-partial-tail test would catch this real divergence. Missing.

**Op 4 / op 8 — multiple `tool_use` blocks per line:** spec says "each block indexes separately" while remaining one line record. Untested. (High value: the module's `obs-tool-call-first` (lines 446–448) indexes only the FIRST block — `(first (tool-call-index-rows obs))` — so a two-block line loses its second tool call. A test would catch this real divergence.)

**Op 8 remaining:** unknown tool-use id → nil: untested. Tool-id collision across distinct lines (A6 determinism): untested. Tool-index dedup under re-harvest: not directly asserted (lines 66–69 check only the conversation).

**I3 — same (file-id, offset), different hash** (in-place mutation/truncate+rewrite → second distinct record): untested.

**Op 2 watch edge cases — none tested:** rotation by inode; deletion (tail stops, rows persist); re-creation at same path = new inode from byte 0; truncation; watcher disconnection/reconcile; zero-byte new file; restart resume across process restart (the "resume" test resumes within one runtime from a prior harvest, not across restart); cancel idempotency (stop! twice); cancel-before-first-poll; no-new-poll-after-cancel.

**Op 10 builder edge cases:** offset beyond EOF → empty: untested. Offset mid-line: untested. Built-but-not-appended records already redacted (I2 on builder output): untested (the boundary fixture contains no secret).

**Op 4:** nil message-uuid row still ingested; conversation-id file-basename fallback grouping (init-less file): untested as such (the parse-error line exercises the fallback id internally but no test reads the fallback conversation).

Fixing this check requires substantial new test scenarios (ledger reads, lifecycle regression, multi-block tool_use, UTF-8 fixtures, harvest partial-tail, mutation/rotation cases) — restructuring-scale work → **major**.

## Synchronization

**Check (verbatim):** "Every write that precedes a read must be followed by `(harness/wait-for-processing! client)` before the read. Verify no write-then-read sequences skip the wait." (Adaptation: this codebase gates with `transcript/await-materialized` polling; every append uses ack level `:append-ack` — module lines 536–555 — which acks on durable depot append, NOT on topology processing completion. So a read is gated only if an `await-materialized` condition covers the value being asserted.)

**Verdict: FAIL.** Gated correctly in some places, but the flagship harvest assertions race.

Walk-through of every write→read sequence:

| Site | Gated? | Analysis |
|---|---|---|
| `harvest-transcripts!` internals (module 907–937): request append → `await-materialized some?`; obs appends; `:complete` claim append → `await-materialized` on **status only** | partial | The await condition is `#(= :complete (:status %))`. Obs events flow through a different depot (`*transcript-obs-depot`) than the claim (`*transcript-claim-depot`); cross-depot processing order is not guaranteed, and each obs event traverses multiple `|hash` hops (line-key → file-state-key → request-id → conversation-id → tool-call-id, module lines 488–512) before the conversation/tool/counter writes land. `:complete` status can materialize while obs events are in flight. |
| `transcript-harvest-contract-test` lines 54–62: reads `run` counters, `conversation`, `tool-call` immediately after `harvest-transcripts!` returns | **NO** | All three assertions (`observed-line-count` 3, `parse-error-count` 1, conversation count 2, tool-call "bash") depend on obs-event materialization that the `:complete` gate does not imply. Flake risk and a contract-verification hole: status and counters are written by different events (claim fold vs `increment-run-counts`), so the single `read-run` at line 54 can legally observe `:complete` with stale counters. No `await-materialized` covers counters, conversation, or tool-call. |
| Lines 66–69: re-harvest then `(is (= 2 (count ...)))` | **NO** | Gated only on harvest-2's run status. If dedup were broken, the duplicate conversation write could still be in flight when the count is read — the idempotency assertion can **false-pass**. It needs a barrier that proves obs processing finished (e.g., await harvest-2's own `observed-line-count` = 3, which is also the missing A8 coverage). |
| Boundary test lines 87–91: request append → `await-materialized` on `:pending` → assert | yes | Condition matches assertion. |
| Boundary test lines 92–96: obs append → `await-materialized` on conversation count 1 → assert | yes | Condition matches assertion. |
| Watch block 1, lines 117–118: poll → immediate `count == 1` read | weak | If behavior is correct, no write occurs (nothing to wait on); but the preceding harvest's conversation materialization is itself ungated (see above), so this read can see 0 → spurious fail; and a wrongful re-emission would be hidden by I4 regardless (see coverage check). |
| Watch block 1, lines 119–121: partial append → poll → `count == 1` | acceptable | Negative assertion; correct behavior writes nothing. Inherently unable to await absence; would need run-counter/ledger barrier to be sound (noted under coverage). |
| Watch block 1, lines 122–127: complete line → poll → `await-materialized` count 2 → assert | yes | Gated. |
| Watch block 1, lines 128–131: stop! → `await-materialized` on `:cancelled` → assert | yes | Gated. |
| Watch block 2, line 146: poll → `(is (empty? ...))` | weak | Negative assertion with no quiescence barrier: if the watch wrongly emitted historic content, the `:append-ack` append could still be unprocessed at read time → **false pass** possible. |
| Watch block 2, lines 147–159: appends → poll → `await-materialized` count 1 (×2) → assert | yes | Gated. |

The ungated harvest reads (lines 54–64, 69) are fixable by adding `await-materialized` conditions on the asserted values — line edits, minor in isolation — but the check as a whole fails.

## Test namespaces compile

**Check (verbatim):** "Every test namespace must load cleanly. Verify imports, `:refer` entries on record constructors (e.g. `->FooRecord`), and that no test depends on a private namespace."

**Verdict: PASS** (with one fragility note).

- Requires (test lines 1–4): `app.server.rama.dogfood.transcript`, `clojure.java.io`, `clojure.test`. All exist on disk; the transcript ns's own requires (`app.server.rama.core`, `app.server.rama.object-container`, `...object-container.{transcript-adapter,transcript-identity,runtime}`, `app.server.rama.dogfood.llm`, `clojure.data.json`, `com.rpl.rama.test`) all exist as files.
- No record constructors / `:refer` entries used — n/a.
- No private vars referenced: every `transcript/...` symbol used by the tests (`start-transcript-runtime!`, `close-transcript-runtime!`, `transcript-request`, `transcript-routing-key`, `harvest-transcripts!`, `read-run`, `read-conversation`, `read-tool-call`, `read-jsonl-observations`, `append-transcript-request!`, `append-transcript-observation!`, `await-materialized`, `start-transcript-watch!`) is a public `defn` in the module ns (verified at module lines 83, 514, 529, 536, 550, 561, 574, 580, 584, 907, 1065, 362, 61).
- Java interop (`java.nio.file.Files/createTempDirectory`, `FileAttribute` array) is fully qualified — compiles.
- **Fragility note (not a load failure):** lines 63–64 call `clojure.string/includes?` without requiring `clojure.string` in the test ns. It loads only because the transcript ns transitively requires `[clojure.string :as str]` before the test body compiles. It works, but the test ns should require what it uses.

## Verdict

**major-fail.**

One-sentence justification: two checks fail at restructuring scale — the suite spends 4 IPC launches on disjoint-key scenarios that belong in one deftest, and entire May-scope spec regions (audit ledger, run-lifecycle monotonicity/regression, multi-block tool_use indexing, multi-byte UTF-8 byte-correctness, harvest-side partial-line I5) have zero coverage while the headline redaction and cursor-resume assertions are vacuous or confounded — plus the harvest test's post-`:complete` reads are unsynchronized (`:append-ack` across separate depots), so the strongest category applying to any single failure is major.

Failure severity breakdown:
- Minimize IPC launches: **major** (consolidation = suite restructuring).
- Implicit spec coverage: **major** (new scenarios required; missing tests mask two identified real divergences — first-block-only tool indexing and harvest-path partial-line appends — and the I2/cursor assertions don't pin what they claim).
- Synchronization: minor in isolation (line-level `await-materialized` fixes).
- Compile: pass.
