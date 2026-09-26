# Implementation Validation — tools and grammars (rig phase 6)

Validator: the build session itself (Claude Opus 5.5, effort max), as the
rama skill's build step has it (`references/phase-build.md`: "you are
doing that work yourself"), following `references/phase-4-impl-validate.md`
and the template `references/artifact-impl-validation.md`. Default verdict
FAIL; each check below was traced through the code at the lines cited, at
branch `rig-build-tools` after `9511ffa4` plus the queries' yielding loops
(uncommitted when written, committed with this file). Paths are under
`src/rig/store/`.

Read for this: the plan whole (and its validation), SPEC.md phase 6, the
brief, the code I changed or added (`shape.clj`, `grammar.clj`,
`recipe.clj`, `runner.clj`, and my edits in `envelope.clj`, `gate.clj`,
`gate_event.clj`, `locks.clj`, `module.clj`, `reads.clj`, `client.clj`),
and the code they call at the lines cited.

## Scenario traces

**T1. A grammar fact, one stream event** (`:alice`, home task t). The
operator's act `{:e :mention :k :grammar :v g}`. `gate/intake` (gate.clj
341) names the keys to read: `grammar/rows-to-read` gives `[:mention]`
(`:grammar` is a store key, so no row; the governed key is). The event
(`gate_event.clj` 155-162) reads `[:alice :key-rows :mention]` on t, then
`locks/decision-reads>` (locks.clj 1127) turns the rows into a grammar map
and runs the value checks under it, and returns `lx` with `:key-rows`
(1144). `gate/decide*` reads them from `lx` (gate.clj 392): `refusal`'s
5-arity passes `control-value-ok?`'s `:grammar` clause (114: `grammar/refusal`
nil, `:mention` not a store key) and `control-allowed?` (operator), then
the rebuild check (217) finds no hint change from the defaults; the yes
branch returns `:key-rows` (442), `[[:mention {:used false :grammar row}]]`,
and `write-decided>` writes it with one `termval` (gate_event.clj 85) in
the decision's group. No partitioner was added. *Run*: the probe
(`runs/phase6-tools-lease-probe.txt`) shows the row projected. PASS.

**T2. A value under a grammar.** Alice's `:mention` of three people in
`:alice`. The event reads the `:mention` row; `grammar/grammars-of` reads
its shape text back; `read-values`' naming step (locks.clj 452) calls
`grammar/named`, whose `shape/check` finds `[:persons]` over max 2 and
returns `:value-shape`; `lock-refusal` places it at L27's place, recorded.
*Run*: the probe's "three people {:answer :no :reason :value-shape}". A
value with one person: subjects `#{:bob}` from the row, the record's
`:subjects` `#{:alice :bob}` (probe), `:used` set true in the same group
(probe's "row after use"). PASS.

**T3. Replay.** A crash before the event's commit: nothing of the event
was committed, so the replay reads the same rows and recomputes the
`:key-rows` list with `grammar/key-row-writes`, pure over the facts, the
rows read, the name and the stamp. The replay's stamp can differ (the wall
may have moved), but every write of the replayed event takes that one
stamp, the record's and the row's `:grammar :stamp` alike, so they agree.
After the commit, the record path answers from `:answers` and writes
nothing of mine. PASS.

**T4. The rebuild check in one act (V-F4).** The first `:note` grammar
with `:by-value` and a `:note` value in one act: `grammar/rebuild-refusal`
sees `:note` in the act's written keys and a hint change from the
defaults: `:grammar-change-needs-rebuild`, after `:control-not-allowed`
(gate.clj 214-217). Tested pure (`grammar-test`) and on the cluster (G4).
PASS.

**T5. A `[:kv]` read of a private layer by someone who cannot see it.**
`read-pattern` parses with `parse-hints` (reads.clj 1333: the pattern's key
counted as by-value, so the form and value are checked), then
`check-layer`, then reads settings and the key's row, and only then, in
the `<<cond` (1353, after `:not-visible` and `:re-classed`), answers
`:opaque` or `:not-indexed`. A reader who cannot see the layer gets
`:not-visible` whatever its grammars, which keeps the read exit's F4. PASS.

**T6. The runner, one pass** (`runner.clj`). `run-pass!` reads `[:k :tool]`
through `read!` as the operator (permission nil, which `check-call`
accepts; probe (a)), parses each tool, runs `recipe/loop-free`, and for
each tool reads its matches as the tool under its pid: the entry is an
ordinary act sealed at the door in the tool's default session, whose lease
the door makes as the tool, sealed under Alice's lock (W1-1; probe (b)).
Each run's act is `output-act`, a pure function of (layer, tool fact,
matched fact): name from `recipe/run-name`, `:claimed-when` from the later
stamp's millisecond, session the tool's default. `client/lookup-many`
(client.clj 268) reads every run name in one `foreign-select-one` with
`submap`; answered names are reported `:recorded` and not offered. A second
pass, a restarted runner, or two runners at once offer the same map under
the same name, so the gate answers from its record (the digest covers
`:claimed-when` and the session, both derived). PASS, traced; the runs are
R1 and R2 of the proof.

**T7. The runner never throws.** Every door and exit call is inside a
`try` that turns a throwable into a report line (`failed`); the recipe's
`run` and `parse-tool` are total; the whole pass is wrapped. PASS.

## Redundant conditionals

`reads.clj` 1339-1343: `(<<if (some? *kvk) (local-select> ...) (else>)
(identity nil ...))`, two different operations. No other conditional was
added in topology code. PASS.

## Consecutive keypath

Every added path is one `keypath` with all its keys:
`(keypath *layer :key-rows *kk)` (gate_event.clj 85, 160; reads.clj the
key-row loops and `key-rows-of>`). PASS.

## Select-compute-transform

The key-row write (gate_event.clj 85) sets a whole row computed from the
row read earlier in the same event: the skill's "If you already HAVE the
value, compute the new value and use `(termval *new-val)`", which reads
nothing more. It is not an aggregation (`+compound` does not apply: the row
is a grammar's projection and a flag). PASS.

## Unnecessary nil->val

None added. PASS.

## :allow-yield?

No added read iterates a subindexed structure: every added read is a point
read of one row. The loops that make many point reads: the gate event's
(`gate_event.clj` 155), bounded by the act's distinct keys; the queries'
shown-key loops (reads.clj, read-point and read-pattern), bounded by the
page's distinct keys (up to 10,000), which now call `(yield-if-overtime)`
like the queries' other loops; the rebuild pages' `key-rows-of>`, bounded by
the page (2,048 rows) and deliberately not yielding, since a rebuild page
must not yield (the read exit's D3: a forget landing inside a yield would
have a purged value written back). PASS.

## Non-subindexed collections without size limits

`:key-rows` is a subindexed map (`grammar/layer-fields`). Inside a row:
`:subjects-at`, a vector bounded by the shape's depth (8); `:index`, a set
of at most 2 hint names (`grammar/refusal` checks the subset). PASS.

## Stream topology idempotency

The only write added to the stream gate is the whole-row `termval` per
changed key (T1, T3). The runner's offers carry names made client-side,
derived (6.3), never in the topology. No internal depot append was added.
PASS.

## Partial failure in stream topologies

Every added read and write is on the layer's home task, inside the
decision's one event, with no partitioner added (gate_event.clj 155-162,
85). The person fan-out that follows the answer is phase 2's and
untouched. PASS.

## Single depot append per client operation

The runner's operations are each one offer under its own name: the
operator's tools read entry, each tool's read entry, each run's act, and
the door's lease acts. A crash between them leaves each answered or not by
name; the next pass recomputes the same names and looks them up first. The
door's `offer-until-answered!` resends one map under one name. PASS.

## Application-state caches survive restart

The runner holds nothing between passes: what it has done is in the
store, under names it recomputes. No TaskGlobal and no in-memory cache was
added. PASS.

## No reimplementation of built-in operations

The version 8 UUID from SHA-256 (`recipe/run-name`) has no Rama built-in
(`ops/random-uuid7` does not exist in 1.6.0, R12, and a derived id is not
random). The shape checker and the formula evaluator are the language,
not a copy of a built-in. PASS.

## Plan conformance

Each divergence, with why. A divergence that is not a correctness or a
clear performance reason would be a FAIL; none is left as such.

- **D1. The micro gate's grammar (a4, b4, V-F4's batch case) is deferred
  to phase 6b** (D-P1, approved by the orchestrator). Built micro code
  opens each value twice (block 1 and block 2b, after 2a), which the plan
  did not know; the design question is in the build notes. Meanwhile the
  micro store keeps the constant and refuses grammar facts as data. Not a
  silent gap: it is the orchestrator's decision, named, with its step.
- **D2. No runner lease step** (D-P2). The plan's F6 had the runner lease
  as the operator because a tool could not lease; wave 1's W1-1 lets it,
  sealing under the layer's owner. Correctness: the tool's reads and
  outputs are sealed under the owner's lock, so the owner's forget reaches
  the tool's unconsumed rows, which the operator's bare rows would not.
- **D3. The constants stay in source for what still reads them** (D-P3):
  `grammar/grammars` as the 3-arity default the micro store's opens and
  phase 2's pure tests use; `reads/seed-hints` unedited, `hints-of` taking
  only its store keys' `:no-copy`. The stream gate and the exit read rows
  only (gate.clj 410, reads.clj 1333, 1188, 1223). Reason: phase 5's rest
  edits `seed-hints` in parallel, and the micro store's reading waits for
  6b; deleting them would break the siblings' merge, not correct anything.
- **D4. A `[:kv]` read applies its key's grammar after the visibility
  check** (T5), where the plan read the row before `parse-pattern`.
  Correctness: the plan's order would answer `:opaque` or `:not-indexed` to
  a reader who cannot see the layer, which the read exit's F4 forbids.
- **D5. A plain scalar in a formula's place stands for itself**
  (`recipe.clj`). The plan's own test tool writes `[:str [:count ...]
  " named"]`, which its formula table (only vectors) could not parse.
  Correctness: the plan's example is the spec of the proof.
- **D6. An `:emit` whose value is nil fails its step (`:no-value`).** A nil
  value is a retract in the envelope; a tool's output asserts. Not in the
  plan; a total evaluator must decide it, and a retract with no
  `:replaces` would be meaningless.
- **D7. The runner runs a tool fact only while no other tool fact in the
  page replaces it** (`runner/tools-in`). The plan says a new version runs
  over the existing matches and is silent on the old one; running both
  would double every output for ever. A rig choice (P6-3).
- **D8. The proof's G cases run in a fresh layer each, and the forgotten
  person is Gus, not Bob** (tests only): the plan's G2, G4 and G6 each
  change `:mention`'s grammar in one layer on purpose (V-F11); in their own
  layers they cannot change what R1 to R5 read in `:alice`, and Bob stays
  a live person for them.

## Verdict

`minor-fail` → fixed in place. One check failed before the fix: the
queries' key-row reads were a non-yielding loop of up to 10,000 point
reads on the layer's task (the `:allow-yield?` check's concern for long
loops), fixed by inlining them as yielding loops like the queries' others.
Every other check passes with the traces above; the divergences D1 to D8
each have a correctness reason or are the orchestrator's decision.

PHASE_VALIDATION:minor-fail
