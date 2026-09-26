# Step 6b plan validation

Codex, GPT-6, 26 September 2026. Read validation only; no cluster run.
Scope is STARTER-6b.md and PLAN-grammar-micro.md, not a redesign of the
already built store. Main owns the verdict; independent scenario evidence
is recorded below when returned.

“the latest grammar fact admitted before its batch”: with grammar G0 on
key :link, batch B containing G1 and a :link value must use G0 for shape,
subjects and hints even if G1 sorts first. The first materialized block
reads before any key-row writes. Block 2b uses its subjects and hints;
the fold uses immutable gathered rows for rebuild checks. G1 governs B+1.
Checked against PLAN's road, fold and index paragraphs: pass by tracing.

“in either order”: an unindexed :link use and first by-value grammar in B
must refuse the grammar even when the use sorts later or fails permission.
The initial draft derived uses from gathered :offer skeletons, which would
miss a missing-lock face. Corrected before implementation: every parsed
envelope contributes [:use L k] independently of the name/face path. Those
flags affect refusal only; only yes facts can set persistent :used.

“exactly once per batch”: G1, two :link values and G2 with unchanged hints
can all be admitted. Projection composition uses a separate evolving map;
the output location [L :key-rows :link] holds one final row with :used true
and G2. Admission continues to use G0. A no or recorded retry never calls
projection composition. Pass by tracing.

“neither a plaintext value nor a bare lock leaves its task”: first block
holds no leases at the layer hop. On arrival it computes values then drops
them, materializing only the sealed envelope, subjects, hints, gathered
rows and existing carry-rows (which excludes bare operator lease rows).
The leader sees the skeleton, never the envelope's sealed payload. The
second open makes keyed digests before partitioning. Rebuild summaries
likewise discard opened values before hash(L). Pass by tracing.

“micro rows first ... $$layers on a miss”: inherited :used true from a
stream row forbids changing hints after reclass. A micro row with nil
grammar is still a hit and cannot accidentally resurrect a stream grammar.
Shared kv reads also retain the independent stream-era check. Pass.

“a setting key ... about another entity”: :grammar's entity is its key,
so it is deliberately outside the four setting keys in misplaced-setting?.
Adding it to micro/control-keys does not alter that predicate; malformed
controls and authorization retain precedence over rebuild refusal. Pass.

Partitions, retry and resources: key rows share hash(L) and the existing
typed subindexed grammar schema. The temporary source is materialized and
read on arrival partitions. Re-attempts rebuild temporary state from the
same depot batch and pre-batch PStates. There is no durable cleanup or new
external side effect. Distinct-key seeks are K(2-p) for N=1,16,128; no
broadcast. Read loops and maintenance pages retain cooperative yielding.
The extra barrier and earlier layer reads on retries are explicit costs.

Read-output check: shared visibility runs before grammar refusal; the
opening cursor uses an already authorized answer, so form-only parsing
does not skip authorization. Effective rows mark only micro-era values;
stream marks remain their own. Rebuild uses :no-copy/:opaque/:by-value from
the layer at the same place all index writes are assembled. Pass.

Independent read returned two points. The reference says materialized
data emits across tasks, like depot data; it does not prove arbitrary
redistribution, so the proposed wrong-task scenario is not established.
Nevertheless ownership is now explicit: carry the arrival task id and
|direct there before block 2b's local reads. Second, spell out F4 extraction:
use keys are fact :k, governed grammar keys are :e. Both clarifications
were applied before code. No change to the selected road.

PHASE_VALIDATION:pass
