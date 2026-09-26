# Step 6b: the micro gate reads grammar facts

Cutter: Codex, GPT-6, inherited session effort. 26 September 2026.
Scope: STARTER-6b.md; phase 6's a4, b4, F4, M1 and T-FR3. The earlier
micro, locks, promotion and read plans continue to govern their mechanisms.

## The road and the decision

Use road 1: compute each value's subjects once under the pre-batch grammar
and carry the subjects to block 2b. Use Rama's attempt-local materialized
data between blocks, rather than adding a durable per-act projection.
The data contains the parsed envelope (sealed bytes), subject ids, hints,
the skeleton, sealed resend lease rows, and the layer's gathered rows. It
contains no opened value or bare lock. It is materialized on the arrival
task with its task id; block 2b explicitly routes with |direct to that id
before any lease/person read. This is batch working data, not
a cross-request cache and not a new record format.

Block 1 becomes two barriers. In the first, parse; visit hash(L) for the
existing settings/permission/stream-head reads and the relevant key rows;
return to the arrival task; open/check, name subjects, check persons and
materialize the safe result. In the second, do the existing name and entity
reads and the fold. Block 2b consumes the same materialized input, reopens
bytes for their keyed digests, and wraps with the subjects from block 1.
It performs no grammar interpretation. All lease reads remain on arrival;
lease consumption remains after rows in 2c. Face records also survive the
materialization and take the existing face path.

Road 2 would reread changed rows after 2a and needs versioning plus two
transfers per act. Road 3 changes the specified meaning by refusing harmless
shape/subject edits beside uses. Neither is needed. Materialization adds a
barrier and bounded attempt memory, but avoids persistent per-act metadata,
an extra disk write/read, a second grammar application and durable cleanup.

## Reads, writes, schemas and partitioning

Add grammar/layer-fields to $$micro, so [L :key-rows k] uses the same
typed, subindexed row as $$layers. Read a micro row first, and $$layers
on a miss. An existing micro row, including a nil grammar, wins wholesale.
Read only the distinct non-store fact keys and governed grammar keys.
No new depot, durable PState, foreign roundtrip, or record slot is needed.

The fold gathers these rows as [:key-row L k]. Keep immutable pre-batch
rows for admission and separate evolving rows for yes-write composition.
Precompute the set of offered keys per layer from [:use L k] rows emitted
for every parsed envelope, independent of its later name/face path and fold
order, including an envelope later refused for a missing lock. Uses are
fact :k values; a grammar's governed key is its :e, via grammar/governed.
The rebuild check sees that set as used, along with prior :used. The gate's
ordinary refusal checks still run and the new refusal is placed immediately
after :control-not-allowed. Malformed grammar and misplaced setting refusal
therefore retain precedence. A grammar fact about a key other than L is
valid; only :kind/:owner/:class/:lock-grain are misplaced settings.

For each yes, grammar/key-row-writes composes on the evolving rows. The
fold coalesces writes by [L k], so multiple uses and grammar edits give
exactly one final whole-row write per changed location in block 2a, on
hash(L). Refusals and recorded retries write no key rows.

The arrival open explicitly passes grammar/grammars-of key rows. The
materialized subjects are indexed by fact position, not just an act union.
Per-act locks still use the union of their values; per-value locks retain
distinct subjects. The ordinary landing path consequently uses its target
layer's grammar. Block 2b still rereads live persons and lease rows, preserving
the existing admitted-but-closed behavior when a person disappears between
blocks. Name/digest/fingerprint/batch filtering remains unchanged.

Compute index digests with the carried pre-batch hints and pass those same
hints into index-block>. A same-batch edit cannot alter :no-copy, :opaque,
or :by-value for a used key because F4 refuses it.

For rebuilds, open rows on their entity task and turn each open result into
a safe summary: the sealed index entry, keyed value digest, and erased date.
No opened sealed-value plaintext leaves the entity; control facts stay clear.
Group these summaries by layer as the
existing put page already groups writes. On hash(L), read the distinct
keys' effective rows once, derive hints and make the writes. Sweep does
the same for its single found row, returning to hash(L) before reading
hints. Tombstone guards and current-batch exclusions stay intact. Pure
legacy helpers remain where existing tests use them; no file is deleted.

Shared pattern/point/delta reads apply grammar rows after visibility.
Pattern parsing uses reads/parse-hints (form-only), then kv-refusal uses
the effective micro-first row. W2-3's independent stream-era kv check
remains, so an unindexed used stream era cannot be silently omitted.
Micro-era shown rows get opaque marks from effective rows; stream-era
rows retain their stream grammar marks. Opening cursors parse the already
authorized answer without the old constant.

Additional grammar I/O, K distinct relevant keys per act/page on layer L:

| input | proportion | N=1 seeks | N=16 seeks | N=128 seeks | iterations |
| --- | --- | --- | --- | --- | --- |
| existing micro rows | p | K | K | K | 0 |
| micro miss, stream fallback | 1-p | 2K | 2K | 2K | 0 |

Weighted additional seeks are K(2-p), independent of N. The relocated
layer visit adds one logical transfer to a fresh offer (arrival -> layer
-> arrival -> name -> entities instead of arrival -> name -> layer ->
entities). An extra global barrier separates arrival from fold. No new
transfer for 2b or shared rebuild writes; no grammar rows sent to task 0
beyond its existing gathered projection inputs. Temporary memory is bounded
by the configured batch record count times the envelope/subject limits.
For controls with no relevant keys K=0. Retry lookups now incur the layer
visit too; the recorded answer still wins over current grammar checks.

## Falsifiers and tests to write (do not run during the build)

1. An unknown grammar/key written as facts governs shape, per-value subjects,
   opaque output and custom by-value indexes in a group; no source key case.
2. Hint-changing grammar and use in either order, within one act or distinct
   envelopes, are refused for the grammar; shape-only changes in the same
   batch are admitted but affect only the next batch. Both wrap subjects
   and the answer's union must reflect the pre-batch grammar.
3. Multiple yes writes coalesce :used and final grammar without dropping
   either. Other layers, refused writes and recorded retries do not mutate
   these rows. Reason precedence and misplaced-setting handling remain.
4. Re-classed layers inherit stream rows on a miss, then a micro row wins.
   A custom key works in kv reads/openings/deltas and survives index rebuild;
   opaque/no-copy hints reach every micro read and maintenance road.
5. A landing's subjects come from the target grammar, including after a
   shape/subjects edit; no reliance on the compiled :mention constant.
6. Replay seeds write grammars into shared layers through their gate and
   the P3-5 guard verifies that seed, rather than being silently removed.
   Update the existing micro/shared/landing fixtures that relied on constants.

## Execution and first-record accounting

Read-validate this plan, implement and load-check without a cluster, then
write and read-validate tests. Commit named files on rig-build-grammar-micro.
Merge rig-2026-09-25 once the work is built. Run wave2-suite-cmd.sh's full
namespace list plus review-fixes-test and the new suites exactly once under
the cluster lock; save runs/grammar-micro-suite.txt. Fix failures and rerun
only affected namespaces. Commit final notes/counts and append the required
completion line to the relay log. Never edit RIG.md or the relay worktree.

Inherited first-record placeholders T-FR1/2/3 apply unchanged: grammar fact
shape; unsealed control status; no grammar version in the decision. The
new key projection has the already specified row shape. Temporary materialized
data introduces no kept-record choice. Open for Sid: T-FR3 and grammar
authority across layers remain the questions in the phase 6 plan, not new
rulings made here. The machinery count will enumerate actual changed units
in three classes and keep the stream 26 and wave-2 3 separately visible.
