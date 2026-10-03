# Step 6b: the micro gate's grammar

Codex, GPT-6, 26 September 2026. Branch `rig-build-grammar-micro`, worktree
`/mnt/data/projects/Softland-rig-build-grammar-micro`. Scope: STARTER-6b.md.

## Receipt

The one full suite passed: **163 tests, 8,085 assertions, 0 failures,
0 errors**, exit 0, on `acf33699`. It ran from 09:01:29 to 09:18:12 IST
(1,003 seconds including startup), 26 September 2026. All 26 namespaces
completed, including the new four-task/two-worker grammar scenarios.
No repairs or reruns were needed.

No tests ran during planning, implementation or test writing. Namespace
loads and reading supplied the three validation artifacts. Before the full
suite, `git merge rig-2026-09-25` reported already up to date at `13c8360b`.
The command is `runs/grammar-micro-suite-cmd.sh`: the wave-2 namespaces plus
review-fixes-test and grammar-micro-test, all under the required cluster
lock. Full output: `runs/grammar-micro-suite.txt`; replay report:
`runs/grammar-micro-replays.txt`.

The replay finished in 145 seconds: all 14 fixed histories played, all 14
as the model says, zero differences and zero not practical. Its dirty-tree
label includes generated run files and the draft of these notes; no
production or test source was edited while the full suite ran.

Commits leading to the run:

- `fe6031da`: the road, plan and reading validation.
- `bc7b8938`: the micro grammar and shared-read implementation, with its
  reading/load validation.
- `acf33699`: the new tests, explicit grammar seeds in earlier fixtures
  and replays, test validation, and the full-suite command.

The commit containing these notes adds only this receipt and the two run
outputs to that tested source. The relay can merge this branch; nothing was
pushed or merged into the relay worktree, and RIG.md was not edited.

During the run the relay branch advanced to `749336e9`. A final scoped
diff from `13c8360b` shows only its new benchmark harness files and
documentation (the full-spec review, benchmark notes and RIG.md). It
changes no store source, dependency file or namespace in this full suite.
Those later relay commits are not folded into this branch: the required
pre-suite merge was the single merge above. The relay's merge can preserve
those independent files beside this tested store change; its phase-7
harnesses still have their own forthcoming run.

## The road and why

Road 1: carry the subjects that block 1 computed under the pre-batch rows
into block 2b. Carry the index hints too. Each value keeps its own subject
set; carrying only the act's union would give every per-value lock the
same subjects. Block 2b opens the sealed bytes again for their keyed
digests, but it does not interpret a grammar again.

The handoff is Rama's attempt-local materialized data, not a new durable
per-act row. Block 1a visits hash(L), reads the settings, permissions,
stream heads and distinct relevant grammar rows, then returns to the
captured arrival task to open and check. It materializes the parsed sealed
envelope, safe skeleton, hints, layer projections and sealed resend lease
rows. There are no opened sealed values or bare locks in that handoff;
ordinary control facts remain clear, as they already do in the store.
Block 1b performs the name/entity reads and the fold. Block 2b explicitly
routes back to the saved task before it reads the leases and live persons.

This costs one extra logical transfer per fresh offer and one extra batch
barrier. The grammar read costs K local seeks when every micro row exists,
or 2K when every key falls back to the stream rows, for K distinct relevant
keys. There is no new depot, durable PState or decision field. Temporary
working data lasts only this batch attempt. Road 2 would require old-row
versioning and another layer visit after the fold; road 3 would refuse
otherwise valid shape/subject edits beside their uses. Neither is needed.

## What was built

The micro gate admits :grammar control facts and projects their rows in
`$$micro [L :key-rows k]`. It reads a micro row first and `$$layers` only
when that row is absent; a present micro row with nil grammar wins whole.
The new refusal follows :control-not-allowed, leaving malformed-control
and the misplaced-setting rule ahead of it. Only :kind/:owner/:class/
:lock-grain are misplaced settings: a grammar about its key id is legal.

Admission uses immutable pre-batch rows. The gather emits a use for every
offered non-store fact key, even when that offer later takes a face or
recorded path. F4's rebuild check sees all those uses in that layer in
either order. A refused use still blocks a hint-changing grammar in the
same batch, but does not persist :used. Separate evolving rows compose
only yes writes; the output coalesces to one final write per [L k], keeping
the last admitted grammar and every admitted use. Shape and subject-path
changes affect the following batch; hint changes beside any use are refused.

Arrival checks pass the rows' grammars to read-values. Row wrapping uses
the saved per-value subjects and the persons still live in block 2b. The
existing admitted-but-closed behavior survives a person disappearing
between the blocks. The ordinary landing path consequently names people
under its target layer's grammar, with no landing-specific constant.

Shared index writes take the saved hints. Rebuild put and sweep paths open
on the entity task and reduce opened values to sealed entries, keyed
digests and erasure dates before visiting the layer's grammar. They read
distinct effective key rows on hash(L). Current-batch exclusions, purge
tombstones and the forget replay remain in force. Shared pattern, point,
opening and delta reads take effective rows after visibility and mark
micro-era opaque values; W2-3's separate stream-era marks and value-index
refusal remain. An unindexed used stream era cannot silently disappear
from a re-classed layer's value query.

Replay seeds now write the model's grammars into base and each group
through the micro gate. P3-5 still refuses the five mention histories when
the resolved shared seed is missing. Earlier micro, promotion and shared
read tests seed grammar facts explicitly. New test keys are written only
as facts: :custom, :before, :after, :shape-change, :private, :opaque,
:inherited and :landed require no key-specific store branch.

## The machinery count

The ledger uses the same unit as the existing stream count: a named
compiled step, which may span helper definitions (for example stream a2
and a8), or an existing code unit whose behavior changes. It is not a
function count, line count or thesis count. The implementation inventory
below makes the boundary inspectable.

**Micro side: 4 units: 2 promised, 2 unanticipated, 0 capabilities.**

| Unit | Class | Code and reason |
| --- | --- | --- |
| a4, micro key-row reader/writer | promised | micro/control-keys, micro-schema, grammar-rows' key rows, init-row/init-w, decide-envelope's projection composition and declare! block 2a; the second gate must admit grammars and maintain the same layer projection |
| micro shared-read grammar inputs | promised | shared-reads' micro index writes, put/sweep, pattern/point/opening/delta and opaque marks; this is the phase-5 constant substitution promised by a8/a9, counted as one code unit just as wave 2 counted this namespace's stream side |
| b4, pre-batch open and block-2b handoff | unanticipated | arrival-open, skeleton, row-wraps and declare! blocks 1a/1b/2b; rows live on hash(L), locks on arrival, and the built store opens twice, so saved per-value subjects and a batch materialization are required |
| micro F4 gather/fold | unanticipated | grammar-rows' offered-use rows, init-row/init-w, prepare and micro-decision/decide-envelope's immutable refusal inputs; b1's single-act rebuild guard is insufficient when another envelope in the batch uses the key |

The micro read/index unit is classified promised because the phase-5 plan
explicitly requires the micro hints to be replaced by rows in phase 6.
Wave 2's three units retain RIG.md's stated unanticipated classification;
they were discovered at the earlier merge. Tests, seeds and documentation
are evidence, not additional compiled store machinery.

| Scope | Promised | Unanticipated | Capabilities | Total |
| --- | ---: | ---: | ---: | ---: |
| Stream side, existing ledger | 10 | 6 | 10 | 26 |
| Wave-2 merge, kept separate | 0 | 3 | 0 | 3 |
| Micro side, this step | 2 | 2 | 0 | 4 |
| Total | 12 | 11 | 10 | 33 |

Two deferred units a4/b4 are now built; two existing micro behaviors also
had to change. No new capability vocabulary was added. Once these four
steps exist, adding the proof's custom grammars needs zero additional
compiled steps.

For a source audit, 30 named production definitions have changed behavior
or are new, grouped into the four ledger units above rather than added
again to the count:

- micro (13): control-keys, reason-order, micro-schema, arrival-open,
  skeleton, grammar-rows, init-row, init-w, micro-decision, decide-envelope,
  prepare, row-wraps, declare!.
- shared-reads (17): key-rows-of>, index-block>, rebuild-facts,
  rebuild-keys, rebuild-writes, put-page>, sweep-one>, sweep-page>,
  micro-ops>, stream-hints, micro-shown-keys, mark-micro-rows, micro-marks>,
  shared-pattern>, shared-point>, delta-read>, delta>.

## Shared-file changes

- src/rig/store/micro.clj: the second gate's controls, key-row schema,
  gather/fold, arrival and block-2b handoff.
- src/rig/store/shared_reads.clj: effective-row reader, saved index hints,
  rebuild summaries and routing, micro marks and grammar-aware reads.
- src/rig/store/gate.clj, grammar.clj and reads.clj: docstrings only,
  identifying retained compatibility arities instead of claiming the
  micro topology still uses their constants.
- test/rig/replay_test.clj: shared grammar seed and P3-5 guard; existing
  histories and model comparisons remain.
- test/rig/store/toy_grammars.clj plus micro_test, wave1_test,
  review_wave1_test, promote_test and reads_rest_test: explicit seeds.
  grammar_test: wording for its retained legacy-arity test. New coverage
  is grammar_micro_test.clj.
- PLAN/PLAN_VALIDATION/IMPLEMENTATION_VALIDATION/TEST_VALIDATION files
  suffixed grammar-micro; the suite command, output and replay report;
  these build notes. RIG.md is not edited.

## For RIG.md

Rig choices: road 1 with attempt-local materialization and explicit return
to the arrival task; per-value subject sets, pre-batch hints, immutable
admission rows separate from yes-only projection composition. The offered
use set is per layer and includes faces and recorded offers, as F4 says
"any envelope". A shared read keeps W2-3's stream-era check in addition to
the effective micro check. These are reversible implementation choices.

First-record placeholders: T-FR1/2/3 remain unchanged (grammar fact shape,
unsealed control status, no grammar version stored on the decision).
The key-row projection uses the already specified schema. No new kept
record slot or wire part was added. The argument for deriving a grammar
from the layer history before the recorded batch remains T-FR3, not a new
ruling. Questions for Sid remain grammar authority across layers and
T-FR3's eventual edition-one record; this step does not settle either.

- Does edition one keep grammar authority per layer, or adopt another
  layer's grammar through stood-on facts?
- Must a decision record a grammar version, or is derivation from the
  layer history before its batch sufficient?

Retained, not deleted: grammar/grammars; the legacy arities of
locks/read-values, value-context and delivered-context; micro/arrival-open
and row-wraps; shared-reads/kv-digests; reads/seed-hints/current-hints and
the old pure rebuild helpers. The production Store paths now pass rows,
hints or saved subjects explicitly. Their compatibility callers are pure
fixtures. Removal can be a separate authorized cleanup.

The older rig.bench.lock-slice still has its own :mention constant. It is
left as the existing isolated phase-2 experiment, not treated as a finished
Store measurement. The phase-7 branch rig-build-numbers' current driver
uses rig.bench.lock-growth and numbers/launch! to launch the real Store;
its seeds write grammar facts. Its benchmark runs belong to the relay and
were not run here. This lists the remaining source constant without
rewriting the historical experiment or deleting it.

PHASE_VALIDATION:pass
