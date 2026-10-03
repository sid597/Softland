# Plan validation — step R, the fixes from the review of wave 1

Validator: Claude Opus 5.5, a fresh session running the rama skill's Phase 2
(`phase-2-plan-validate.md`, the template `artifact-plan-validation.md`).
The default verdict is FAIL; a check passes only after a traced scenario
with citations.

**What was read.** `PLAN-review-fixes.md` whole (before and after my edits);
`REVIEW-wave1.md` whole; RIG.md's "Overnight state" (1 to 173), For Sid 4
(566 to 583) and For Sid 37 (640 to 661); the relay folder's
`build-instructions` whole; the formal model's `control-keys` (model.clj 54
to 57), `apply-control` and `admit` (467 to 503) and `op-layer-setting` (943
to 953).

**The code, and which tree.** The plan says the merged tree "had not
landed". At this validation it exists: branch `rig-wave2` at `0edf1dac`
(07:32 IST, "Wave 2 merge, 3 of 3"), the three branches merged
(`rig-build-promotion` `5303418f`, `rig-build-tools` `2d2f666a`,
`rig-build-reads-rest` `33817364`) plus the merge's own edits (W2-1, W2-2);
its suite was in flight (`runs/wave2-suite-cmd.sh`, untracked in its
worktree). Every binding point was checked there by `git show`, and against
the base (the worktree at `c9684356`) and the branch it comes from. Line
numbers below are `rig-wave2`'s unless marked "base". `gate_test.clj` was
excluded from every search and never opened; nothing under `src/app` was
read. No clojure, no cluster; only read-only git.

**What is checked, derived or assumed.** Checked means read in the code at
the lines cited. Two things are derived from reading, not run: the micro
gate's writes for a refused act (the answer under each entity, no row), and
that a forget of a grammar fact answers yes with `:how nil`. One thing is
assumed from `deps.edn` (no direct-linking flag in the `:test` alias): that
`with-redefs` on `mc/take-locks` reaches `write!`'s call.

## The verdict in brief

Minor-fail. The plan's mechanisms are the right ones and hold on the
merged tree: one pure clause in the shared `gate/refusal` reaches both
gates; the door check is what the review's pending micro assertion needs;
R-2's seam carries the keyed form on every pattern answer the exit can
hold; the F-1 edge throws where it should. Eight things failed, each fixed
by a localized edit to the plan (listed at the end):

- **V1.** The plan points to a "Prep" section ("see "Prep"") that did not
  exist, so nothing said how the build gets onto the merged tree or when
  the binding points are rechecked.
- **V2.** "Checked: every setting fact built in `src/` and `test/` on all
  three branches is about its layer's own entity" is false:
  `micro_prepare_test.clj`'s generators build setting facts about other
  entities. Harmless (totality properties only, never through a door), but
  not what the plan said it checked.
- **V3.** "`review_wave1_test.clj` by none" is false on the merged tree:
  the merge (W2-2) adds the toy grammars to its seed and its O11 probe.
- **V4.** O1: "After R-1 every such fact is store vocabulary in its place,
  so nothing free-text is left for it to miss" is false: a `:grammar`
  value's shape can hold strings (`[:enum "..."]`), and a request's
  `:public` is 32 chosen bytes; a forget of either answers yes and erases
  nothing.
- **V5.** Minimality: F-1's edge had two checks for one condition
  (`write!`'s `:no-locks` and `seal`'s `:no-lock`). Kept `seal`'s alone.
- **V6.** Test and placement details that would fail or mislead: `write!`'s
  door check was placed "before it leases", after the grain read, which is
  a store access the plan's own nil-store test forbids; with a nil store
  `mc/offer!` does not throw without the check (it polls to its timeout);
  the pure `write!` test needs `:grain` given; the depot checks of the
  honest door must go by name, since the raw roads append the same texts;
  the door check must be total over odd `:facts`.
- **V7.** The runs miss `rig.store.grammar-test`, the one direct test of
  phase 6's conditions inside the very `:malformed-control` clause the step
  edits; and they count five changed source namespaces where there are
  four.
- **V8.** Two precision errors in claims that go to Sid or steer the build:
  the table said a `:forget`'s position is checked (its entity is not);
  the For Sid 4 caveat said the guess works "against any layer she can
  see" (only where the key is indexed by value).

## Sections of the template that do not apply, and why

Step R is a bug-fix step on an existing module. It adds no depot, PState,
partitioner, topology, query topology or task global, and no read or write
on any path; it adds one pure clause inside an existing decision, and
client-side checks.

- **Query topology.** Not applicable: no query topology is added or
  changed. R-2 reads a key the `read-pattern` and `shared-read-pattern`
  answers already carry (reads.clj 1041; shared_reads.clj 1430, 384), in a
  pure client-side function (`entry-facts`, reads.clj 1068). N and M are
  unchanged for every query.
- **PState schemas.** Not applicable: no schema changes. A read line's
  `:pattern` lives inside a value the door seals, so its new form is bytes
  to every schema; a refused act writes only the answer record every no
  already writes (gate.clj 386 to 406; micro.clj 932 to 946, 973 to 978).
- **Partitioning, and its efficiency table.** Not applicable: no
  partitioner and no read moves. The clause runs where the decision already
  runs (the stream gate's event on the layer's home task, gate_event.clj
  163; the micro fold, micro.clj 958). The plan has no efficiency table and
  needs none.
- **Topologies.** Not applicable: no new topology. The clause sits in the
  stream gate's existing event and the micro gate's existing microbatch
  fold; it needs no latency class of its own.
- **Internal depot usage, cross-topology correctness, stream topology
  correctness.** Not applicable: no internal depot, no new flow between
  topologies, no `depot-partition-append!`.
- **In-memory state efficiency.** Not applicable: no task global or cache.
  The door's memory atom (client.clj 33 to 40) is untouched; the door check
  holds no state.

## Production readiness

- **Many concurrent clients.** Pass. The gate clause is a pure function of
  the offer; the door check is a pure function of the offer it is handed;
  `seal`'s throw is local to one call. No shared state is added. Two
  clients sending misplaced settings at once are each refused on their own
  act (the stream gate decides a layer's acts in order on its home task;
  the micro fold decides a batch in order).
- **A client process restarts.** Pass. The checks are stateless: a
  restarted door refuses the same offer the same way. After F-1's edge
  throws, the lease act is committed and answered yes, and nothing else was
  sent; a restarted door holds nothing about it and the next `write!` takes
  a fresh lease; the old lease's rows go at the session's close (micro.clj
  1038 to 1039).
- **A worker restarts during a topology execution.** Pass. A stream event
  retried from the depot finds no record under the name and decides again,
  by the same pure clause, the same way; a record already written answers
  from the record (`answer-from-record`, gate.clj 332 to 338). A microbatch
  replays the same batch through the same pure fold. The clause cannot
  throw (keyword lookups on parsed maps), which matters because a fold that
  throws deterministically retries for ever; the plan names this (its
  "The module" section).
- **Large scale.** Pass. No collection is added; the clause and the door
  check are O(facts), and an act's facts are bounded by the envelope.
- **Non-idempotent writes in stream topologies.** None added. The change
  removes writes: a misplaced setting now writes one answer record instead
  of a log row, heads and index entries.
- **Partial failure across partition hops.** None added: a refused act
  writes only on the home task (stream) or in the fold's one write set
  (micro), as every no already does.

## Minimality — adversarial simplification

**The simplest design I can build that meets the spec.** One predicate,
"a setting key on an entity other than the act's layer"; one clause using
it in the shared `gate/refusal`, so both gates refuse with one reason; the
same predicate checked by the doors before anything is sent, because only
the sender can keep a depot clean; `entry-facts` records the answer's
`:recorded-pattern`; `mc/seal` refuses to seal under a missing lock. Tests
turned into ordinary assertions. Diffed against the plan: the plan had one
extra mechanism, a second no-lock check in `write!` (V5).

### The clause in `gate/refusal-with-rows`
- **Delete it:** R-1 fails: "refuse control-key facts the gate does not act
  on, at both gates" (RIG.md, Next 2). A raw client's misplaced setting is
  admitted as plaintext in the log and both id indexes.
- **Merge or bypass:** it is already merged into the existing
  `:malformed-control` `or` (gate.clj 221 to 228), which both gates share
  (`decide*` 423; `micro-decision` 926). Pass.

### `gate/misplaced-setting?`
- **Delete it:** the gate clause and the door check would each restate the
  rule, and could drift. One named predicate is the smaller design. Pass.

### The door check (`refuse-misplaced!`, four call sites)
- **Delete it:** the review's pending micro assertion fails: `(is (not
  in-depot) "no plaintext value in the micro depot")` (review test, base
  243 to 244), which C's brief makes ordinary; and I-L4 (IMPLICIT_SPEC 298
  to 304) counts "the queue of offers a gate reads from" as part of the
  store. The gate refuses after the append, so no gate-side change can pass
  it.
- **Merge or bypass:** the four sites were each constructed and weighed.
  `c/offer!` and `mc/offer!` are the primitives. `c/offer-until-answered!`
  wraps `offer!` in a catch-all retry (client.clj 296 to 303): with the
  check only inside `offer!`, a misplaced act would be retried 60 times,
  250 ms apart, with a lookup each, and thrown at the end (15 s and 60
  reads for nothing). `mc/write!` reads the grain and leases before it
  offers (micro_client.clj 315 to 321): with the check only in `mc/offer!`,
  an act with a value fact and a misplaced setting would mint lease rows in
  the store for an act never sent. Each site saves real work. Pass.

### R-2: strict `entry-facts`
- **Delete it:** R-2 fails ("store a `[:kv]` pattern's value keyed in the
  old entry form too").
- **Merge or bypass:** the fallback to `(:pattern answer)` costs the same
  code and is weaker (it writes the text whenever the key is missing).
  Pass.

### F-1: `write!`'s own no-locks check (the plan's change 1)
- **Delete it:** nothing breaks. With `seal`'s throw in place, `write!`
  after a yes lease with no or too few locks throws from `seal` before
  `offer!`, sends nothing, and the lease name is in the thrown lock id
  (`[lease-name i]`, locks.clj 199 to 205). The only loss is a second
  error shape (`:no-locks` beside `:no-lock`) for the one failure, which a
  caller would have to know twice. **FAIL** — removed (edit 9).

### F-1: `seal`'s throw (the plan's change 2)
- **Delete it:** the door's sealing function can emit `:sealed nil`, which
  is exactly the bug ("the door never seals under no lock" names the door,
  and `seal` is the door's sealing: micro_client.clj 179 to 182). With
  change 1 gone it is the one guard. Pass.

### The new pure namespace `review_fixes_test.clj`
- A test namespace, not a module mechanism; it keeps the step's run list
  exact. Not assessed as over-engineering.

## Throughput — adversarial

Nothing to cheapen. No seek, iterator read or network roundtrip is added on
any path. The gate clause is O(facts) comparisons on an offer already in
hand, after the reads the decision already made; the door check is the
same on the client; R-2 is one map lookup on the client, and the HMAC it
records is already computed by the query (reads-rest). F-1's check is a nil
test per sealed value. A refused misplaced setting now costs the gate one
answer record where it used to cost a log row, heads and two to five index
entries. The lowest-cost design I can construct is this one. Pass.

## Spec coverage — every item traced

### S1. R-1 at the stream gate

- **Source.** RIG.md, Overnight state, Next 2: "refuse control-key facts
  the gate does not act on, at both gates, extending W1-7". For Sid 37:
  "The build will refuse such facts at both gates in step R, after wave 2's
  merge, extending W1-7, and phase 4's gate is to refuse a client-made
  `:crossed` (the review's question 5)." REVIEW-wave1.md R-1: "The stream
  gate admits all six: `:owner`, `:kind`, `:class` and `:lock-grain` (on
  any entity but the layer's own), and `:promote-request` and `:crossed`
  (anywhere)."
- **Trace.** Alice, settings of `:alice` `{:kind :personal :owner :alice
  :class :by-layer :grain :per-value}`, permission `[:alice :alice
  :alice]`. A raw `foreign-append!` of `{:e :rv1 :k :owner :v "rv1 owner
  text"}` under `[:alice :by-layer :offer u1]`:
  1. `env/parse`: `:owner` is a control key, so the value slot keeps
     `{:v "rv1 owner text"}` (envelope.clj 336 to 340); parsed.
  2. `gate/decide` (gate_event.clj 163) → `decide*` → `refusal` (423) →
     `refusal-with-rows`: no fact layer (207); class in force `:by-layer`
     from the settings (158) equals the offer's; the permission chain
     passes; the `:malformed-control` `or` (221 to 227): today no disjunct
     holds, since `control-fact?` is false for it (`setting-fact?` needs
     `(= (:layer offer) (:e f))`, 61, and `:owner` is in none of the other
     sets, 79 to 81); with the new disjunct `misplaced-setting?` is true,
     so `:malformed-control`.
  3. A no: `log` nil (435), `reads/no-index-writes` (441), no heads, no
     settings, no permissions; the answer record under the name with reason
     `:malformed-control`.
  4. `:class` or `:lock-grain` about `:rv1`: store-placed (envelope.clj
     41, 229 to 240), so the door names it `[:alice nil :offer u2]`; the
     stream gate takes a nil-class name (418 refuses only `:by-entity`), the
     tag matches (244 to 248); the same reason. `:kind`: as `:owner`.
  5. The owner's forget of `[[:alice :by-layer :offer u1] 0]`:
     `c/forget-value!` takes the stamp from the no's record (a no carries a
     stamp, gate.clj 443 to 446) and, with no row, `e` = `:alice` (client.clj
     393 to 399); the gate: `refusal` nil (the owner may forget, 148),
     `stream-refusal` nil, `lock-refusal` finds no row at the target:
     `:no-such-value` (locks.clj 773 to 774).
  6. Phase 4's two, as the plan predicts: `:promote-request` with a string
     value: `control-fact?` true (81), `request-value-ok?` false (a string
     is not a map, promote_shape.clj 88): `:malformed-control`. `:crossed`
     with a string: `crossed-value-ok?` false (105 to 106), and the
     `:malformed-control` clause comes before `:control-not-allowed` (230):
     `:malformed-control`. `:members` with a string: no disjunct of
     `refusal` holds; `stream-refusal` (280 to 285): `:control-not-allowed`.
  7. Through the door: `refuse-misplaced!` throws before `sealed` and the
     append; `*offers` holds nothing under that name.
- **Fault tolerance.** Worker restart: the event either committed its
  writes or is retried from the depot and decided again the same way (pure
  clause). Retry: same reason; a resend under the name answers from the
  record. Multi-partition: a no writes only on the layer's home task.
- **Races.** Two clients writing about `:rv1` at once: both acts are
  decided in order on `:alice`'s home task, each refused. Out of order
  arrival: not applicable (one task decides a layer).
- **Flaws found.** None in the mechanism, with reasoning: every step above
  is read from the merged code, and the clause is the one disjunct the
  plan adds. The test's depot check needed to go by name (V6, under S9).
- **Verdict.** PASS.

### S2. R-1 at the micro gate

- **Source.** As S1 ("at both gates"); REVIEW-wave1.md R-1: "At the micro
  gate Bob writes `:owner` on another entity in the group. The gate admits
  it: plaintext in `*micro-offers` and in the row's `:v`."
- **Trace.** Bob, session `:rvs`, permission `(sp :rvs :bob)` in `:group`
  (`:kind :group :class :by-entity :grain :per-value`). A raw `mc/send!` of
  `{:e :rv1m :k :owner :v "rv1 micro owner text"}` under `[:group
  :by-entity :offer u3]`:
  1. `parse-micro` (micro.clj 272 to 301): `env/parse :micro` keeps the
     plaintext; `not-sealed?` (258 to 270) with micro's own `control-keys`
     (48 to 52, which hold the four setting keys): no `:sealed` → not
     refused.
  2. The fold's `decide-envelope` (948): `micro-decision` (905) calls
     `(gate/refusal o settings rows heads)` (926), the 4-arity, which runs
     `refusal-with-rows` with key rows nil (268): the new disjunct gives
     `:malformed-control`. `micro-extras` (743 to 776) has nothing for
     `:owner` (its `:unsupported-reclass` needs `setting-fact?`, 775);
     value, grain, person and forget reasons are nil for a control-only
     act; `first-in-order` (80 to 83): `:malformed-control`.
  3. Writes of a no (derived from reading, not run): the name's answer and
     fingerprint, the answer under each entity of the act (976), the task
     clocks; no heads, rows, settings, members or permissions (979 onward
     only for a yes); rows are written only for a yes (`rows-written?`,
     1242 to 1247). No lease cited, so none consumed.
  4. `mc/await-answer` (micro_client.clj 225 to 243) finds the record decided
     for this envelope's fingerprint: `{:answer :no :reason
     :malformed-control}`.
  5. `:class` and `:lock-grain` about `:rv1m` are store-placed (nil class);
     the micro gate takes such names (micro_test 290 and 303 admit an
     operator's grain switch so named); the same reason.
  6. Through the door, `mc/write!` of the `:owner` fact: no value facts, so
     `write!` would go straight to `mc/offer!` (313 to 314); the check at
     `write!`'s top (or in `mc/offer!`) throws before `send!`.
- **Fault tolerance.** The fold is pure over the batch and W; a replayed
  microbatch decides the same; the clause cannot throw, so it cannot make
  the batch retry for ever.
- **Races.** Two envelopes in one batch are decided in the fold's order,
  each refused on its own.
- **Flaws found.** None in the mechanism, with reasoning as above.
- **Verdict.** PASS.

### S3. The table: is a setting key about another entity the only control-key fact still admitted and never acted on, after wave 2?

- **Source.** The caller's question, and RIG.md For Sid 37: "A fact under
  a control key that the gate does not act on keeps its value as
  plaintext, outside every lock".
- **Trace, key by key** (envelope.clj 55 to 57 holds fifteen control keys;
  micro.clj 51 to 52 holds fourteen, without `:grammar`).
  - Stream gate: `:kind :owner :class :lock-grain` on the layer's entity
    are projected (433 to 434) or refused (221 to 253); on another entity
    no disjunct holds and `setting-updates` skips them: admitted, acted on
    by nothing. `:permission`, `:revoke`: shape-checked (107 to 108), the
    operator's (146), projected (457 to 470). `:forget`, `:lease`,
    `:session-closed`: shape (110, 115 to 117, 123 to 125), for a lease and
    a close the entity too; a forget's entity is not checked (110).
    `:person`, `:forget-person`: the operator's in the store layer only
    (145). `:members`: `:control-not-allowed` (280 to 285).
    `:promote-request`: shape (119), one fact per act (225).
    `:crossed`: shape (120), the store's alone (144). `:grammar`: in the
    language, not on a store key (127), the owner's or the operator's
    (149).
  - Micro gate: the four setting keys as at the stream gate, plus
    `:unsupported-reclass` for a made layer (775); `:permission`,
    `:revoke`, `:forget`, `:lease`, `:session-closed` through
    `gate/refusal` and `micro-extras`; `:person`, `:forget-person`,
    `:promote-request`, `:crossed` refused `:control-not-allowed` (54 to
    57, 774); `:members` only on the layer's entity in its making act
    (756 to 759); `:grammar` in plaintext refused `:not-sealed` on its face
    (not in micro's `control-keys`, so `not-sealed?`'s last branch, 270).
  - So yes: after wave 2, the one control-key fact admitted and acted on by
    nothing is a setting key about another entity, at both gates, because
    both take their settings rule from `gate/setting-fact?` (gate.clj 433;
    micro.clj 917). Two nuances that do not change the answer: a second
    grant of a granted pid is admitted and changes nothing (P8, 458 to
    462), but its value is a checked pid, not text; and facts a gate does
    act on can still carry text (S5).
  - The model's reading, as the plan says: seven control keys (model.clj
    54 to 57), `apply-control` acts on `:class` and `:lock-grain` whatever
    the entity (467 to 470), `op-layer-setting` always writes on the
    layer's entity (943 to 953), and the replays play no setting act
    (`rowless-kinds` has `:set-grain` and `:reclass`, replay_test.clj 278
    to 282), so they cannot see the difference.
- **Fault tolerance, races.** Not applicable to a reading of the code.
- **Flaws found.** The table's cell "shape and position checked" for
  `:forget` is false: its entity is not checked (V8).
- **Verdict.** FAIL, fixed by edit 3. The conclusion the plan draws from the
  table holds.

### S4. The door refusal (R-1 at the doors)

- **Source.** The review's pending micro assertion (base 243 to 244):
  `(is (not in-depot) "no plaintext value in the micro depot")`; RIG.md
  Next 2: "The review's pending tests (`RIG_PENDING=1`) become
  assertions."; I-L4: "nothing the store keeps holds that value's
  plaintext: not the log, not an index, not the queue of offers a gate
  reads from ... in the rig the offers a gate reads are part of the store."
- **Trace.** Without a door check the review's micro block still fails:
  `mc/write!` (n = 0) → `mc/offer!` → `send!` appends the plaintext record
  (206 to 210), and the gate refuses after the append. With the check,
  nothing is appended. Production callers on the merged tree and every
  branch, none of which sends a setting key about another entity:
  client.clj (`make-layer-offer` 523 to 531, grants, revokes, persons,
  forgets, leases), micro_client.clj (`make-base!` 446 to 461,
  `reclass-offer` 463 to 469, `make-group!` 471 to 493, `lease-offer`,
  `send-any!` 416 to 422 through the two doors), read_exit.clj
  (`entry-offer` 91 to 103, the close 201 to 206, `forget-entry-offer` 214
  to 222, the restore fact 408 to 410), standing.clj (`line-offer` 31 to
  38), promote_client.clj (63, 89, 105, 118, 151), runner.clj (97; a tool
  may not write a store key, recipe.clj 181). Tests through a door: none
  (V2's search). The reason `:malformed-control` matches how the gates
  already name a control fact out of place: a lease not about its session
  (gate.clj 117), a person fact not about its person (111 to 113),
  `:members` off the group's entity at the micro gate (micro.clj 757); the
  review's road 1 and For Sid 37 name the same reason.
- **The throw.** The stream door already throws when it cannot lease
  (`assign!`, client.clj 184); a throw says nothing was offered and leaves
  the name unused. An answer shaped like the gate's would be the door
  speaking for the gate. Right.
- **Placement and totality.** `write!` reads the grain from the store
  (`settings-of`, 315) before it leases, for an act with a value fact;
  "before it leases" left that read before the check, and the plan's own
  pure test ("with a nil store throws before any store access") would then
  hit the nil store first. At the door the facts are raw: the check must
  look only when `:facts` is sequential, as `value-indices` does (client.clj
  97 to 100), or a caller's odd `:facts` (a number) would throw a different
  exception than the gate's `:malformed` answer (V6).
- **Fault tolerance.** Client-side and stateless: a restart or a resend
  refuses the same way.
- **Races.** None: no shared state.
- **Flaws found.** The placement in `write!` and the totality wording (V6).
- **Verdict.** FAIL, fixed by edit 4.

### S5. O1, the review's sub-question under R-1

- **Source.** REVIEW-wave1.md, question 1: "A second question under it:
  should a forget of a fact with no lock be refused rather than answered
  yes? OP9 leaves this open (O1)."
- **Trace** (from reading, not run). The layer's owner writes `{:e :note
  :k :grammar :v {:shape [:enum "a private sentence"] :subjects-at nil
  :opaque false :index #{}}}`: `grammar/refusal` is nil (grammar.clj 86 to
  108; `[:enum v ...]` takes any EDN values, shape.clj 12 and 79 to 80),
  the entity is not a store key, the owner may write it (gate.clj 149):
  admitted, the text plaintext in the row's `:v` and the id indexes. Her
  forget of it: the target row exists, it has no lock, so yes with `:how
  nil`, and the text stays. The same holds for a request's `:public`, 44
  bytes of which 32 are the requester's choice (box.clj 73 to 81, 97 to
  105).
- **Flaws found.** The plan said: "After R-1 every such fact is store
  vocabulary in its place, so nothing free-text is left for it to miss."
  That is false for `:grammar` and, in 32 bytes, for `:promote-request`.
  Step R's scope is right (these are facts a gate acts on, not R-1's kind);
  the claim, which goes to Sid, was not (V4).
- **Verdict.** FAIL, fixed by edit 6, which also carries the fact to the
  notes under For Sid 37's sub-question.

### S6. R-2: a `[:kv]` read entry keeps its value keyed

- **Source.** RIG.md Next 2: "store a `[:kv]` pattern's value keyed in the
  old entry form too (seam `reads/recorded-pattern`)". For Sid 4: "The
  build will store the pattern's value keyed, like the fingerprint (an HMAC
  under the fingerprint secret), so a line still re-runs and compares but
  confirms nothing to a reader, in step R after wave 2's merge."
  REVIEW-wave1.md R-2: "Alice's own entry, read back through the exit,
  still reads `{:pattern [:kv :note "rv2 a value about rv-b alone"] :exact
  [[fid stamp]]}`."
- **Trace.** `:rv-b` writes `"rv2 a value about rv-b alone"` into `:base`
  (one-owner on the stream gate at that point; the merged seed writes the
  toy grammars, so `:note` is indexed by value there). Alice's `rx/read!`
  with `[:pattern [:kv :note text]]`: `read-pattern` (reads.clj 1648)
  reaches `pattern-answer` (1760; 1032 to 1047), whose `:recorded-pattern`
  is `[:kv :note {:keyed h}]`, h the hex HMAC-SHA256 under `fp-secret` of
  the UTF-8 of `"softland.read-pattern/1\n"` and `"\"rv2 a value about
  rv-b alone\""` (959). `entry-offer` (read_exit.clj 91 to 103) calls
  `entry-facts` (reads.clj 1068), which after the change writes `:pattern
  [:kv :note {:keyed h}]` beside moment, role, mark, count 1, fingerprint,
  the secret's id and `:exact [[fid s]]`, marked `:own-row`; the door seals
  it under `:alice-hand`'s lock. After `:rv-b`'s forget, Alice's point read
  of her entry opens the line: its pattern is not `[:kv :note text]`, so
  `pairs?` is false and the text is nowhere in it.
  - Every pattern answer the exit can hold carries the key: one-owner
    (reads.clj 1760); shared (1667 to 1668 dispatch to
    `shared-read-pattern`, then `shared-pattern>` at shared_reads.clj 1362
    calls `pattern-answer` at 1430, and `shared-answer`, 378 to 386, keeps
    it). The standing read's opening line already records it (standing.clj
    80); its delivery and closing lines record no pattern.
  - `entry-facts` has one caller, read_exit.clj 103.
  - Tests that read back a line's pattern: review_wave1_test (rewritten);
    read_exit_test 489 asserts `[:e :t2]`, unchanged because
    `recorded-pattern` returns every other form as it is (960); tools_test
    273 and 277 (`[:k :mention]`, `[:k :tool]`), unchanged; reads_rest_test
    313 (standing, already keyed). Hand-made answers: reads_test 487
    (`pans`, which the plan fixes) and 522 (`{:kind :pattern}`, nil before
    and after); the totality property at 608 holds (`entry-facts` catches
    everything, 1082 to 1096).
  - Strict rather than a fallback: the secret "never leaves" the module's
    functions (`fp-secret`, 916 to 920), so the exit cannot compute the
    form and a fallback would write the text. With nil facts, `c/build`
    makes `:facts []`, the gate refuses `:empty-act` on its face
    (envelope.clj 400), and `read!` answers `{:refused :empty-act :entry
    nm}` (read_exit.clj 142) with nothing shown. No module answer takes
    that road. Right.
- **Fault tolerance.** The HMAC is computed in a query topology, which
  writes nothing; a retried query computes the same h. The entry is an
  ordinary act: a resend of the same map is answered from the record.
- **Races.** Two readers of one pattern get the same h: that is the
  caveat, S7.
- **Flaws found.** None in the mechanism: it builds the brief's road
  exactly, on the seam the brief names.
- **Verdict.** PASS.

### S7. The For Sid 4 caveat the plan carries

- **Source.** For Sid 4 as quoted in S6 ("confirms nothing to a reader").
- **Trace.** The keyed form is a function of the value alone. A reader who
  can read Alice's old line issues `[:kv :note g]`; the module computes the
  same form for g whether or not anything matches; her new line records
  it; equal digests mean g was the value. The read must be one the module
  accepts: the key indexed by value in the layer read, else `kv-refusal`
  (reads.clj 1662) refuses and no line is written.
- **Flaws found.** The caveat is right and is exactly what the build rules
  ask for ("surface the divergence"): the brief's road stops a reader
  reading a value off a line, not confirming a guess, so For Sid 4's
  "confirms nothing to a reader" holds only in the first sense. Two
  wordings were loose (V8): the guess works "against any layer she can
  read where the key is indexed by value", not "any layer she can see";
  and the scenario's "needs to reach nothing more" contradicted the caveat
  that follows it.
- **Verdict.** FAIL, fixed by edits 7 and 8.

### S8. F-1's last edge

- **Source.** REVIEW-wave1.md F-1: "Not fixed alongside it. A lease
  answered yes whose `take-locks` times out after 30 s still seals under no
  lock. The stream door throws after its tries instead." RIG.md Next 2:
  "F-1's last edge, a lease answered yes whose `take-locks` times out
  still seals under no lock."
- **Trace.** Bob writes one value in `:group`. `write!` (micro_client.clj
  298 to 324): the lease is answered yes; `take-locks` (278 to 290) polls
  `micro-lease` and returns `{}` at the 30 s deadline; before the change,
  `seal` (179 to 195) called `(locks/seal nil bytes)`, which is nil
  (`seal-with` needs `lock?`, locks.clj 120 to 136), so the envelope
  carried `:sealed nil`, and `parse-micro`'s `not-sealed?` refused it
  `:not-sealed` (micro.clj 267). After the change, `seal` throws
  `{:door :no-lock :name nm :lock-id [ln 0]}` before `offer!`; nothing is
  appended under the act's name and no answer is recorded; the lease stays
  answered yes and its row goes at the session's close (micro.clj 1038 to
  1039). Two values under per-value grain with one lock taken: `seal`
  throws at the second. A refused lease: `seal-ks` holds a throwaway lock
  for every id of `(:ids l)` (322), so `seal` never throws there and F-1's
  fix stands.
  - Every caller of `seal`, on the merged tree and each branch: `write!`
    (323); micro_prepare_test (60, 260, 317, 319, 485, 486, 537, 542,
    546, 555, 556, 568) and micro_test (166, 204 to 206, 250, 364, 386,
    390, 433, 434, 457, 461, 465, 469, 477, 504, 505, 525, 552, 565, 574,
    616). Each seals under the locks of the lease whose ids it cites (or a
    fresh lock for the id it names, 457), and each expects an answer other
    than `:not-sealed`, which a nil seal would have produced; micro_test's
    only `:not-sealed` expectation (226) sends a plaintext value, not
    through `seal`.
  - Alternatives, constructed: throwaway locks for a yes lease's real ids
    reach the gate, which finds the lease rows, fails to open the value,
    records `:does-not-open` under the act's name and consumes the rows (a
    door failure recorded as an answer); a longer wait or a second lease
    only moves the throw, the stream door throws after its tries; throwing
    sends nothing. Throw is right.
- **Fault tolerance.** Client-side. A worker restart that delays the lease
  batch lengthens the wait, then throws; nothing is half sent. A client
  restart holds nothing about the lease.
- **Races.** Two `write!`s each take their own lease; nothing shared.
- **Flaws found.** Two checks for one condition (V5, see Minimality).
- **Verdict.** FAIL, fixed by edits 9, 10, 13, 14 and 16.

### S9. The review's pending tests become ordinary assertions

- **Source.** RIG.md Next 2: "The review's pending tests (`RIG_PENDING=1`)
  become assertions."
- **Trace, each pending assertion** (base review test 137 to 139, 162 to
  164, 241 to 244) and what the plan puts in its place, on the merged tree:
  - R-2: `(is (not pairs?))` holds (S6); plus the line equals
    `(reads/recorded-pattern [:kv :note text])` (the test requires
    `rig.store.reads`) and the text is nowhere in the entry.
  - R-1 stream: `(is (empty? admitted))` and "a forget answered yes leaves
    the value readable nowhere" become (a) four door throws with reason
    `:malformed-control`, (b) four raw sends answered `[:no
    :malformed-control]` with no row, no index entry, nothing shown, and
    the owner's forget answered `[:no :no-such-value]`, (c) phase 4's two
    `[:no :malformed-control]` and `:members` `[:no :control-not-allowed]`
    (S1, points 5 to 7). The old `sent` loop would have died on the door's
    throw; the plan rewrites the block.
  - R-1 micro: "no plaintext value in the micro depot" and the forget's
    assertion become the honest door's throw and four raw sends answered
    `[:no :malformed-control]` with no row on `:rv1m` (S2).
  - The F-1 edge is new: `with-redefs` on `mc/take-locks` reaches `write!`
    (a var call; the `:test` alias sets no direct linking).
- **Flaws found.** (i) "`review_wave1_test.clj` by none" is false on the
  merged tree: the merge adds `tg/write!` to the seed (the R-2 block's
  `[:kv :note ..]` read of the base needs it) and to the O11 probe (V3).
  (ii) The honest door's depot checks must go by the act's name: the raw
  roads append records with the same texts, so a check by text alone would
  fail, and (b) must use fresh names (V6). (iii) In the pure namespace,
  `(c/offer-until-answered! nil o)` without the check retries for 15 s and
  `(mc/offer! nil o)` polls to its 60 s timeout and returns a map, so the
  parenthesis "a nil store would throw otherwise" was false for the micro
  door; the calls take one try and a short timeout (V6). (iv) The pure
  `write!` test must give `:grain`, or `write!` reads the grain from the
  nil store (V6).
- **Verdict.** FAIL, fixed by edits 2 and 11 to 14.

### S10. Sid's build rules for tonight

- **Source.** build-instructions: "It is a candidate for the store core:
  build in the form it should be, because it may be kept." "Anything else:
  the simplest thing that can change later without touching a record. If
  it would touch a record, use the simplest placeholder, mark it
  first-record, write the question under "For Sid" at the top of RIG.md,
  and carry on with whatever doesn't depend on it."
- **Trace.** R-1 narrows what a record may carry and adds no form: a
  refused act writes the answer record every no writes, with an existing
  reason; For Sid 37 carries the question, and the plan keeps road 2 as
  Sid's alternative. R-2's line form is reads-rest's first-record
  placeholder carried to one more line, and the plan marks it and adds the
  caveat to For Sid 4. The door checks and F-1's throw touch no record and
  can change later freely. "The form it should be": one rule in one
  function for both gates and both doors; a sealing function that cannot
  claim a seal it did not make.
- **Fault tolerance, races.** As S1 to S8.
- **Flaws found.** None, with reasoning: each change is either a refusal
  under an existing reason or client-side, and both first-record touches
  are marked and sent to Sid.
- **Verdict.** PASS.

### S11. Sid's test rule, and the runs

- **Source.** RIG.md, Overnight state: "only running test once is fine imo
  it just adds to the time"; as builder B applies it: "after a fix, rerun
  only the namespaces the fix touches; run the full suite once, at the end;
  never rerun green code to confirm it".
- **Trace.** The changed source namespaces are four (gate, client,
  micro_client, reads); almost every test namespace calls one of them, so
  "the namespaces the fix touches" has to be chosen by what can change. The
  plan chooses by search (misplaced settings, `[:kv]` lines read back,
  seals under a missing lock); I repeated the searches on the merged tree
  and they hold (V2's correction aside). One namespace was missing:
  `rig.store.grammar-test` calls `gate/refusal` directly on phase 6's
  grammar conditions (grammar_test.clj 54 to 80), which sit inside the same
  `:malformed-control` `or` the step edits; a slip there shows nowhere else
  before the full suite. It is pure and runs in seconds.
- **Flaws found.** grammar-test missing; "five source namespaces" where
  there are four (V7).
- **Verdict.** FAIL, fixed by edit 15.

### S12. The merged tree and the recheck before code

- **Source.** RIG.md Next 2 (step R comes "after wave 2's merge"); For Sid
  4 and 37 ("in step R, after wave 2's merge"); the plan's own line 8:
  "checked again on the merged tree before any code is written (see
  "Prep")".
- **Trace.** There was no "Prep" section. The branch is cut from
  `c9684356`, where `refusal-with-rows` (base gate.clj has one `refusal`,
  165), `recorded-pattern` and `:recorded-pattern` (absent from base
  reads.clj) do not exist, so a builder following the plan on this branch
  alone could not build R-2 or place R-1's clause as written. The merged
  tree now exists (`rig-wave2`, `0edf1dac`) and every binding point the
  plan names holds there, except as V2, V3 and V8 say.
- **Flaws found.** A dangling reference where the plan's one safeguard
  should be (V1).
- **Verdict.** FAIL, fixed by edit 1.

## Every factual claim about the code

| Claim in the plan | Checked at | Holds? |
|---|---|---|
| Phase 6 adds `:grammar` to `control-keys` | envelope.clj 55 to 57 | yes |
| Phase 4's `landing-name` is a 3-arity | envelope.clj 286 to 287 | yes |
| gate.clj: `exempt-actors` gains `:store`; `control-fact?` gains `:promote-request`, `:crossed` with value checks; `:crossed` the store's alone; a request its act's one fact | gate.clj 42 to 48, 81, 119 to 120, 144, 225 | yes |
| Phase 6: `refusal` a 4- and 5-arity over `refusal-with-rows`; `:grammar` in `control-fact?`; `store-key?` new | gate.clj 196 to 269, 79, 63 to 70 | yes |
| Phase 5's rest: `:session-closed` takes `:reads` | gate.clj 123 to 125 | yes |
| gate_event.clj calls `gate/decide` | gate_event.clj 163 | yes |
| micro.clj's control keys, `foreign-control-keys`, `micro-extras`' control clauses unchanged by wave 2; the fold decides by `gate/refusal` | diff base..rig-wave2 (no such lines); micro.clj 926 | yes |
| reads.clj: `recorded-pattern` on every pattern answer, the shared read too; standing opening line; `entry-facts` still `(:pattern answer)`; phase 6 changes neither | reads.clj 947 to 961, 1041, 1092; shared_reads.clj 1430; standing.clj 80; reads-rest..rig-wave2 diff | yes |
| client.clj: only `lookup-many` added | diff base..rig-wave2 | yes |
| micro_client.clj unchanged | diff base..rig-wave2 empty | yes |
| `reads_test.clj`'s `entry-facts` test unchanged by wave 2 | diff base..rig-wave2 | yes |
| `review_wave1_test.clj` changed by none | merge adds W2-2 lines | **no** (V3, edit 2) |
| The table's cells | S3 | **no** for `:forget`'s position (V8, edit 3) |
| Both gates take the settings rule from `setting-fact?` | gate.clj 433; micro.clj 917 | yes |
| The model's seven keys, `apply-control`, `op-layer-setting`; replays cannot see the difference | model.clj 54 to 57, 467 to 470, 943 to 953; replay_test.clj 278 to 282 | yes |
| "Every setting fact built in `src/` and `test/` ... about its layer's own entity" | micro_prepare_test.clj 704 to 733 | **no** (V2, edit 5) |
| `offer-until-answered!` retries every exception | client.clj 296 to 303 | yes |
| The raw stream road: parse, `decide`, no index writes for a no, forget `:no-such-value` | S1 | yes |
| The raw micro road: `first-in-order` keeps `:malformed-control`, no row, no lease consumed | S2 | yes |
| "Unchanged" cases | gate.clj 147, 246 to 249 | yes |
| O1: "nothing free-text is left for it to miss" | S5 | **no** (V4, edit 6) |
| "Not done" 1 and 2 | envelope.clj 225; gate.clj 397 | yes |
| R-2's strictness road ends in `:empty-act` and `{:refused :empty-act :entry nm}` | envelope.clj 400; read_exit.clj 142 | yes |
| The caveat | S7 | yes, with the layer precision (V8, edit 8) |
| F-1: before, `:sealed nil` refused `:not-sealed` | micro_client.clj 192; locks.clj 127; micro.clj 267 | yes |
| "Every caller of `seal` passes a lock for every id it assigns" | S8 | yes |
| A nil store "would throw otherwise" (the pure door tests) | micro_client.clj 245 to 256 | **no** for `mc/offer!` (V6, edit 14) |
| "The tests of the five source namespaces it changes" | four: gate, client, micro_client, reads | **no** (V7, edit 15) |
| The clause is O(facts), pure, total; totality matters in the fold | gate.clj 221 to 228 | yes |

## Self-consistency check

Re-read. Every block where I wrote that something was false, loose,
missing or twice is marked FAIL (S3, S4, S5, S7, S8, S9, S11, S12, and the
minimality block for `write!`'s check). The blocks marked PASS (S1, S2, S6,
S10, production readiness, throughput, and the other minimality blocks)
carry no flaw. The caveat in S6 and S7 is a property of the brief's own
road, which the plan surfaces to Sid as the rules ask; it is not a defect
of the plan, and S7's FAIL is for the wording only. None of the failures
needs the plan rethought: each is a claim corrected, a section added, a
duplicate check removed, or a test detail made exact. So minor-fail, and
the plan is fixed in place.

## Edits made to PLAN-review-fixes.md

1. Added "## Prep (added by the plan validation)" before "The merged tree
   at these seams": merge wave 2's tree in by a merge commit
   (`rig-wave2` `0edf1dac` at this validation), recheck every binding
   point on it before code, and repeat the setting-fact search (V1).
2. "The merged tree ... Tests": `review_wave1_test.clj` is changed by the
   merge (W2-2's `tg/write!` in the seed and the O11 probe), and step R
   keeps those lines (V3).
3. The table's `:forget :lease :session-closed` row: shape checked, and the
   entity for a lease or a close; a forget's entity is not checked (V8).
4. R-1 change 3: `write!`'s check right after it builds the offer, before
   it reads the grain or leases; the check is total over odd `:facts`, as
   `value-indices` is (V6).
5. "Why the doors as well as the gates": the "checked" parenthesis now says
   what was checked, where, and names `micro_prepare_test.clj`'s
   generators and why they do not matter (V2).
6. "What stays as it is", O1: grammar shapes can carry strings and a
   request's `:public` 32 chosen bytes, a forget of either answers yes and
   erases nothing; to the notes under For Sid 37's sub-question (V4).
7. R-2's scenario: "and needs to reach nothing more" became "whether
   anything more must be reached is the caveat below (For Sid 4)" (V8).
8. The For Sid 4 caveat: "against any layer she can read where the key is
   indexed by value" (V8).
9. F-1's change: one change, `seal` throws `{:door :no-lock :name nm
   :lock-id lid}`; `write!`'s separate `:no-locks` check removed; the
   callers' claim extended to every call in micro_test and
   micro_prepare_test (V5).
10. F-1's scenario: `seal`, called by `write!`, throws `:no-lock` (V5).
11. Tests, R-1 at the stream gate: (a)'s depot check by the offer's name;
    (b) under fresh names, since its records carry the same texts (V6).
12. Tests, R-1 at the micro gate: the depot check by the door's act name;
    the raw sends' own records are the "Not done" item (V6).
13. Tests, F-1's edge (cluster): `:no-lock`; the lease named by the lock
    id; the act's name from the ex-data (V5).
14. Tests, `review_fixes_test.clj`: the nil-store door calls take one try
    and a short timeout, and assert the `:door :refused` data; the pure
    `write!` test gives `:grain`; `:no-lock` (V5, V6).
15. Runs: `rig.store.grammar-test` added, with why; "five" source
    namespaces corrected to four (V7).
16. Design difficulty log, F-1's entry: the throw sits in `seal` alone
    (V5).

PHASE_VALIDATION:minor-fail
