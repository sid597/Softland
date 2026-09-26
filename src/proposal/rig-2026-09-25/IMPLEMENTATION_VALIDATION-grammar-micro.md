# Step 6b implementation validation

Codex, GPT-6; reading and namespace load only, 26 September 2026.

The scoped implementation follows PLAN-grammar-micro.md. No test or cluster
has run at this stage. `runs/grammar-micro-load.log` ends `:loaded` after
requiring rig.store.module; the existing promote-flow/continue> warning is
unchanged. Topology launch/behavior proof belongs to the final suite.

The source trace, in execution order:

- micro/declare! block 1a reads key rows on hash(L), then |direct returns
  to the captured arrival task before lease/person reads. arrival-open's
  explicit grammar argument reaches locks/read-values' 4-arity. Only its
  subject map/union/reasons reach skeleton; no :values/:plain/:lock-of.
  $$micro-arrivals contains ciphertext input, safe skeleton/carry-rows,
  layer projections and hints. Block 2b routes to the captured task again.
- grammar-rows emits uses independently of name-step and layer-rows'
  eventual face. Every parsed envelope contributes these rows after the
  name/entity branch rejoins. prepare snapshots pre-key-rows before the
  ordered fold. decide-envelope overlays batch uses only for the refusal
  call, then projects yes facts against evolving key-rows and coalesces
  writes with put. A refused or recorded offer never reaches that writer.
- micro-decision reuses gate/refusal's 5-arity, retaining malformed-control
  and owner/operator checks. The new reason's position is immediately after
  control-not-allowed. gate/misplaced-setting? is unchanged; grammar facts
  about their key ids remain legal while misplaced settings remain refused.
- row-wraps' topology arity accepts saved per-value subjects and only opens
  bytes for digests. It never consults grammar. The existing per-act union,
  live-person wrapping, rows-written? name/digest/fingerprint/batch filter,
  row/record-lock choice and block-2c consumption still bound every write.
- Both kv-digests and index-block> receive the same saved hints. A grammar
  change in block 2a cannot affect those writes. A promotion landing enters
  this same arrival-open path with its target :layer, so no promotion-specific
  grammar code or source-layer grammar is used.
- shared-reads/key-rows-of> is local to the layer, micro first and stream
  only on a nil micro row. No whole subindex crosses a partition. Its loop
  yields. New durable state is grammar/layer-fields, the existing typed
  subindexed schema; no Object schema, new depot, external side effect or
  TaskGlobal state is introduced.
- rebuild-facts reduces opened values to an encrypted entry, keyed digest
  and erasure date on the entity task. put-page> groups those by layer, then
  reads distinct effective keys on hash(L) and applies rebuild-writes.
  sweep-one> does the same after its entity visit. Existing bounds, progress,
  current-batch exclusion, tombstone guards and forget replay are retained.
- Shared pattern and delta reads parse form without seed hints; effective
  kv refusal runs after visibility. W2-3's independent stream-era refusal
  remains. micro-marks> changes only micro-era shown rows, while stream-marks>
  keeps stream-era semantics. Point reads and delta deliveries call it too.
  An opening cursor parses an answer already authorized by the query.

The skill's mechanical checks: new state is subindexed; new writes are
whole-row termval through the existing coalesced location writer; selects
do not reread already held values to compute a write; no stream retry path
or client append lifecycle changed; no redundant all-task fanout; all new
local key-row reads have an explicit layer-task caller. The only extra
batch barrier is the stated separation between safe arrival materialization
and the fold. Existing pure compatibility arities remain, with no topology
caller; their removal is listed for Sid, not performed.

PHASE_VALIDATION:pass
