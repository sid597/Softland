# Step 6b test validation

Codex, GPT-6, 26 September 2026. Reading and namespace loads only; no
cluster or test has run during the build. Scope is STARTER-6b.md and the
six falsifiers in PLAN-grammar-micro.md, with SPEC phase 6 and the earlier
plans' a4, b4, F4, M1 and T-FR3. These are this addition's contract; the
store exposes functions and queries, not a separate protocol namespace.

## Minimize IPC launches

Check: "Minimize IPC launches."

PASS. Four pure tests exercise the gather/fold and saved subjects. The one
new IPC launch, grammar-micro-test/grammars-through-the-micro-store, holds
all live scenarios on disjoint keys in testing blocks. It uses four tasks,
two threads and two workers, so a subject/grammar transfer cannot pass
merely because the cluster is one task. Existing suites retain their
launch structure; fixture changes add only grammar offers to those runs.

## Implicit spec coverage

Check: "Verify every edge case and entity state x write combination is tested."

PASS for the scoped addition, walked against the six planned falsifiers:

1. The block "a key named only in facts controls shape, subjects and value
   indexes" creates :custom with a map/set shape and a subject path, checks
   the answer's subjects and the row's actual lock, and queries by value.
   A number under that key is refused :value-shape. An unauthorized grammar
   is refused :control-not-allowed; a reader outside the group sees
   :not-visible, not the grammar's indexing status. An ungrammared :note
   is not indexed, explicitly detecting fallback to the old constant.
2. grammar-use-in-both-orders checks F4 before and after a use, and within
   one act. The live subject and hint blocks give offers deterministic
   UUIDv7s and assert their M2 order; pausing the topology places both in
   one batch and the subject block checks their recorded batch ids. The
   shape/subjects edits affect the next batch, with the current batch's
   wrap and answer still naming Alice. A retry under the new grammar keeps
   its original answer and Alice wrap. The hint-changing edit is refused
   in either order. A missing-lock face still blocks a same-batch hint
   change, while neither offer persists a synthetic :used row.
3. composed-rows-and-refusal-precedence requires one final whole key row,
   retaining both :used and the last admitted grammar. It checks the exact
   grammar fact id. A misplaced :kind wins :malformed-control over the
   grammar-rebuild refusal. refused-offers-still-count-as-batch-uses also
   checks that another layer's offered use neither blocks the grammar nor
   creates a row in that other layer. Existing grammar-test supplies the
   shared parser/rebuild refusal edge cases; review-fixes-test retains both
   doors' and gates' misplaced-setting checks.
4. "reclass falls back to stream rows, then micro rows take precedence"
   first names Alice through a stream grammar, then Bob through the micro
   override, and refuses a hint change on the used key. The custom-hints
   block exercises an opening and delta under an unknown :private key,
   observes its :no-copy entries before and after a full put/sweep rebuild,
   and checks its value query still returns the value. Opaque marks are
   checked on point and pattern answers, and value queries are refused
   before and after rebuild. Existing reads-rest-test retains forgotten
   value, tombstone, rebuild race and standing-read invariants.
5. "a landing names subjects under the target grammar" puts a value in
   Alice's source with no source grammar, lands it under the group's
   :right subject path, waits for the API's actual success status :done,
   and checks Bob on the landing record and wrap and the target value index.
6. replay-test's seed writes the resolved toy grammars through the micro
   gate in base and every group. Its P3-5 test retains the missing-seed
   refusal for exactly A2/A4/A5/A6/A7 and adds the positive resolved-seed
   case plus both shared seed steps. Actual seed answers remain part of
   replay execution, so adding a seed description cannot mask a refused
   admission. Existing micro, wave1, promotion and shared-read fixtures
   explicitly seed their model grammar before ordinary values use it.

saved-subjects-are-per-value additionally checks two distinct values name
different people, wraps take those per-value sets, the act union contains
both people, the skeleton carries no byte arrays, and no grammar names no
value subjects. These assertions catch accidental use of only the union.

## Synchronization

Check: "Every write that precedes a read must be followed by
`(harness/wait-for-processing! client)` before the read."

PASS using this store's equivalent APIs. The stream offers use the full
answer road. Micro offers wait for their own settled answer; the new
grammar!, write-value! and in-one-batch! helpers additionally wait for all
four task frontiers to reach the answer's batch before dependent reads.
The landing waits for :done and then all four frontiers. Rebuild's driver
waits for each maintenance page's progress. Existing fixture additions use
the same offer!/await-answer road before a later batch is offered.

## Test namespaces compile

Check: "Every test namespace must load cleanly."

PASS. `runs/grammar-micro-test-load-final.log` ends `:tests-loaded` after
loading the new suite and all eight changed test/fixture dependencies;
no IPC was created. The earlier reader/scope errors were fixed before
this load. Read validation also corrected an impossible :landed status
assertion and a numeric assertion on rebuild's map receipt. The only
warning is the existing promote-flow/continue> name shadowing.

## Verdict

pass: each scoped contract case has an observable assertion, the writes
and dependent reads synchronize, and every changed test namespace loads.
Execution proof is the single final suite, after the relay merge.

PHASE_VALIDATION:pass
