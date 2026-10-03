# Implementation Validation — stage 1, "stream-store"

<!-- Phase 4 of the rama skill, inside the stage's build session (Claude Opus
5.5, 25 September 2026). Template: references/artifact-impl-validation.md.
Reviewed against PLAN-stream-store.md (with its fixes F1-F14), SPEC.md and
the sources it names (PROGRESS.md "Now", model.clj `baseline`),
IMPLICIT_SPEC.md sections 0-3 and its stream-side entries, and RIG.md's rig
choices. Line numbers cite the files as they stand after the fix recorded
below. What was traced is marked "traced"; what was run is marked "ran"
(only the smoke test and the F14 probe had run when this was written). -->

Module source reviewed: `src/rig/store/module.clj` (the `Store` module),
`src/rig/store/gate.clj` (the pure decision), `src/rig/store/envelope.clj`
(the envelope, digest, names), `src/rig/store/inject.clj` (test hooks),
`src/rig/store/client.clj` (the offerer's side).

Default verdict at the start of the review: major-fail. One finding, a
schema violation reachable from input, was found and fixed in place; every
check below is traced against the fixed code.

## Finding, and its fix

- **An owner value reached a Keyword slot unchecked.** `owner-in-force`
  took an act's own `:owner` setting fact's value as it came, and
  `answer-record` puts the owner into the record's `:subjects`
  `(set-schema Keyword)`. The record is written for a refused act too. Trace:
  the operator offers a making act into an unmade layer `:x` with
  `{:e :x :k :owner :v "alice"}`; parse accepts it (a string is EDN data);
  `refusal` returns `:malformed-control` (gate.clj 161-162); `decide*`
  still builds the record (gate.clj 273) with `:subjects #{"alice"}`; the
  write at module.clj 121 violates the schema, which throws inside the
  topology: fatal to the worker, and the record replays for ever under
  `:all-after`. The F14 probe ran the same class of write (a string into a
  Keyword settings slot) and it threw `ValueSchemaMismatchException`.
  **Fixed** (gate.clj 91-99): the act's own owner fact counts only when its
  value is a readable keyword; `:owner settings` is a Keyword by schema.
  Classification: a localized edit (minor). Re-traced below under
  "Stream topology idempotency" and "Plan conformance" item 13.

## Redundant conditionals

- `(<<if (contains? *in :refuse) ...)` (module.clj 80): the branches differ
  (a face ack vs the whole decision path). Not redundant.
- `(<<if (some? *rec) ...)` (90): recorded/taken path vs fresh decision.
  Not redundant.
- `(<<if (= :decide (get *d :kind)) ...)` (116): writes vs none (the
  `:gate-error` face). Not redundant.
- `(<<if (= :yes (get *record :answer)) ...)` (122): the yes-only writes vs
  none. Not redundant.
- `(<<if (some? *new-settings) ...)` (138): write vs no write. Without it a
  yes with no setting fact would write nil over the layer's settings. Not
  redundant.
- The `loop<-` bodies' `<<if (empty? *todo)` (101, 109): emit vs continue.
- PASS.

## Consecutive keypath

Every path uses the multi-arity form: `(keypath *layer :answers *name)`
(89, 121), `(keypath *layer :settings)` (96, 139), `(keypath *layer
:permissions *pid)` (105, 142), `(keypath *layer :heads *hk)` (113, 131,
134), `(keypath *layer :log *name)` (124), `(keypath *layer :stood-on
*name *sf)` (128). Client paths likewise (client.clj 49, 94, 100, 104, 107,
112). PASS.

## Select-compute-transform

Three selects are followed by a computed `termval` of the same location:

- `$$clock` (97 then 143): the new value is the stamp, `max(wall, clock +
  1, ...)` (gate.clj 186-193), which needs the old clock to compute; the
  stamp also goes into the record and the heads, so it cannot be a bare
  aggregator. The `termval` of the value in hand is the skill's no-read
  write (SKILL.md "If you already HAVE the value ... use (termval
  *new-val)").
- `:settings` (96 then 139): read for the class check, the owner and
  `:layer-already-made`; written whole as `(merge settings updates)`
  (gate.clj 281). One no-read write; the per-field `termval` the plan's
  text names would read the settings value before each write, since
  `(keypath layer :settings :kind)` navigates into a stored value
  (paths.md "No-Read Optimizations"). An aggregator does not express
  "these fields, from these facts".
- Permission rows (105 then 142): read for the four permission checks and
  P8's stale-revoke and first-grant rules; written whole from the row in
  hand (gate.clj 283-296).
- PASS: in each case the read is needed by the decision, and the write is a
  no-read `termval` of a value computed from it.

## Unnecessary nil->val

No `nil->val` anywhere in the module or the gate. PASS.

## :allow-yield?

No read in the topology iterates a subindexed structure: every
`local-select>` is a point read (89, 96, 97, 105, 113). The two `loop<-`s
iterate the act's own read keys (bounded by the offer), each a point read.
No yield is wanted: a later offer on the task must not run between this
decision's reads and writes (dataflow.md "Yielding and ordering"). The
client's `foreign-select ... ALL` reads (client.clj 94, 100) are foreign
reads, not topology code. PASS.

## Non-subindexed collections without size limits

Every write to a non-subindexed inner collection, and its enforced bound:

- Answer record `:subjects` `(set-schema Keyword)`: carried subjects capped
  at 256 by the parser (envelope.clj 320, F3), plus the owner: at most 257.
- Log row `:mark` `(set-schema Keyword)`: only `:die-with-any` and
  `:own-row` pass the parser (envelope.clj 288): at most 2.
- `:settings` fixed-keys: four fields.
- Permission row fixed-keys: two fields.
- Record `:permission` (3 keywords), `:because-of` (a 4-element name), a row's
  `:replaces` (a 2-element fact id), heads keys `[e k fid]`: fixed length by
  `pid?`, `valid-name?`, `fid?` (envelope.clj 251-267).
- Unbounded ones are subindexed: `:answers`, `:log` and its row vector (F1),
  `:stood-on` and its inner map (F2), `:heads`, `:permissions`
  (module.clj 21-61). The row vector is `{:subindex? true}`: Rama 1.6.0
  refuses `:subindex-options` on a `vector-schema` (F14 probe, ran), so its
  size is tracked.
- A value (`:v`) is canonical text, a String, not a collection.
- PASS.

## Stream topology idempotency

Each write, traced for a retry of the event:

- Answer record `termval` (121): keyed by name. If the first attempt
  committed, the replay reads it at 89 and takes the recorded path (92-94),
  writing nothing. If it did not commit, nothing of it exists and the replay
  decides on the same state (the event is one atomic group, see below).
- Log rows, one whole-vector `termval` (124); stood-on entries `termval`
  (126-128); heads `NONE>` (129-131) and `termval` (132-134); settings
  `termval` (138-139); permission rows `termval` (140-142); `$$clock`
  `termval` (143). All sets of values computed before any write, keyed by
  name, fact id, `[e k fid]`, pid or layer. No `AFTER-ELEM`, no `term inc`,
  no counter. A replay that reaches them (only when nothing committed)
  writes the same rows; the stamp may differ from the lost attempt's, but
  the lost attempt gave none (no ack left the task).
- IDs: names are made by the offerer before the gate (client.clj 21-38,
  `env/uuid7`); the gate makes none. Fact ids are `[name idx]`, derived.
- No `depot-partition-append!` in this stage.
- A record that completed and replays after a later crash (RQ 2) reads its
  own record and answers from it: `answer-from-record` (gate.clj 200-206)
  compares digests; the digest is deterministic (sorted canonical text,
  constant secret: envelope.clj 133-189), so it is `:recorded` with the
  same answer and stamp, nothing written.
- No schema violation is reachable from input in any write (the no-throw
  invariant, I-G1): every class is the schema's because `parse` normalises
  it (`(into [] ...)` on every vector: envelope.clj 274-275, 329, 333,
  335-336; `long` on every Long position: 275, 335, 337; readable keywords
  everywhere a keyword goes); a yes writes setting values only after
  `control-value-ok?` (gate.clj 59-72, R13); the record's owner is a
  keyword (the finding above, fixed). The F14 probe showed each of these
  classes of violation throws when it does reach a write.
- `intake` and `decide` catch Throwable (gate.clj 213-227, 298-307),
  `parse` too (envelope.clj 346-358), so no exception leaves the pure code;
  a failure is the unrecorded face refusal `:gate-error`, answered through
  the ack (module.clj 82, 145).
- PASS.

## Partial failure in stream topologies

The event has no partitioner (module.clj 78-145): source, reads, decision
and every write run on the layer's home task, the depot's `hash-by :layer`
task, so every write is in one atomic group (stream.md "Writes between two
partitioners form one atomic group"; RIG.md phase 0 claim 1, ran). A
failure mid-writes discards all of them; there is no committed partial
state for a replay to miss or duplicate. `<<atomic` blocks add no commit
boundary. PASS.

## Single depot append per client operation

- `offer!` (client.clj 40-45): one `foreign-append!` per offer.
- `offer-until-answered!` (74-90): a resend of the same map under the same
  name after an error is the protocol (I-G3, RQ 4); the gate answers it from
  the record. It is not a second write of one operation.
- `seed!` (156-164), `make-layer-offer`, `grant-offer`, `revoke-offer`: each
  offer is its own operation under its own name; `seed!` is a script of
  independent operations, each atomic at the gate. A client that dies
  mid-seed and reruns it makes new names; a second making act is then
  refused `:layer-already-made` (F8), a second grant leaves the first
  (P8).
- PASS.

## Application-state caches survive restart

No TaskGlobal and no in-process cache of application state. The atoms in
`rig.store.inject` are test hooks (P14), not state the store reads for a
decision. Everything the gate reads is in `$$layers` and `$$clock`. PASS.

## No reimplementation of built-in operations

- `envelope/uuid7` (215-223) makes UUID7 names. `com.rpl.rama.ops` in 1.6.0
  has no `random-uuid7` (checked: `(resolve 'com.rpl.rama.ops/random-uuid7)`
  is nil; the only random op is `current-random-source`), so this is R12,
  not a reimplementation. It runs on the client, not in dataflow.
- HMAC and hex (envelope.clj 157-189): JDK crypto; Rama has none.
- `canonical`, `normalize-value`, `edn-value?`: no Rama counterpart.
- The two `loop<-` read loops are the dataflow form; no `ops` function reads
  a list of keys.
- PASS.

## Plan conformance

Every divergence from PLAN-stream-store.md, and its standing:

1. **Row vector size tracking stays on** (module.clj 47-50). The plan says
   "size tracking off" for the row vector [F1]; Rama 1.6.0 rejects
   `:subindex-options` on `vector-schema` ("Invalid option", valid
   `#{:subindex?}`; F14 probe, ran). The plan's form is not expressible.
   PASS (plan wrong on a Rama fact).
2. **Settings and permission rows written whole, not per field**
   (gate.clj 281, 283-296; module.clj 137-142). The plan's "Writes" names
   one `:settings` field `termval` per setting fact and the row's
   `:revoked` `termval` for a revoke, and counts every write as a no-read
   set ("1 + f + 2r + s + p + b no-read sets"). A per-field path navigates
   into the stored value and reads it first; the whole-value `termval` of
   the value read in this event is the no-read set the plan's own count
   assumes. The resulting rows are the ones the plan names. PASS (matches
   the plan's cost model; the literal path would not).
3. **Grant targets' rows are read** (gate.clj 103-113). The plan's step 3
   reads "the cited permission's row", and its category (d) counts a row
   for a revoke only. P8 requires that "a second grant fact under the same
   id is admitted as a fact and leaves the index's first grant and its
   revocation state as they are": without reading the row a grant write
   would overwrite the first grant and clear the revocation. PASS
   (correctness of P8; one seek per grant fact, operator acts only, within
   category (d)'s count of 4).
4. **`gate/intake` computes the digest and the read keys right after
   `parse`, inside one guard** (gate.clj 213-227; module.clj 79). The plan's
   step 2 is `(envelope/parse *raw :> *p)` and the digest is "derived by
   the gate at decision time". Same functions, same order, and a failure in
   the digest becomes `:gate-error` instead of an exception. PASS.
5. **`answer-from-record` in place of `gate/status`**: the same three
   outcomes (recorded, taken, undecided as "no record"). Naming. PASS.
6. **The value domain is P5's list** (envelope.clj `edn-value?`): lists,
   symbols, characters, records, Java floats and non-EDN `Number`s are
   malformed, and numbers are normalised to one form per `=` class
   (`normalize-value`). Java floats were at first normalised to doubles;
   phase 7's first run showed `#{(float -3.0)}` is not `=` to `#{-3.0}`
   (Clojure hashes a Float apart from the equal Double), so they are
   refused instead: EDN has no single-precision float. The plan
   lists values as "maps, vectors, sets, keywords, strings, numbers,
   booleans, nil, uuids"; the pre-plan code accepted more. This brings the
   code to the plan and makes the digest respect `=` (I-G7): `(= [1] '(1))`,
   `(= 1 1N)`, `(= 1.0M 1.00M)`, `(= 0.0 -0.0)` hold while their printed
   forms differ. Proposed as a rig choice in the build notes. PASS.
7. **Two revoke facts for one pid in one act are `:stale-revoke`** (gate.clj
   171-173). The plan: "a revoke of an unknown, elsewhere-kept or already
   revoked permission is refused `:stale-revoke`"; the second of two
   revokes in one act revokes one the first revokes. PASS (the plan's rule
   read within one act, as a doubled replace is stale).
8. **A carried stood-on stamp must be below 2^62** (envelope.clj 62-67,
   269, 312-315). Not in the plan. Correctness: the stamp is at least a
   carried stamp + 1 and the task's clock follows it; `inc` on
   Long/MAX_VALUE throws, which `decide` turns into `:gate-error` for that
   offer and, once a clock sits at the maximum, for every later offer on
   the task. Proposed as a rig choice. PASS (correctness).
9. **R13's `:malformed-control` and `:control-not-allowed`** (gate.clj
   59-82, 161-165), after the permission checks and before
   `:stale-replaces`. A rig choice already in RIG.md; no plan fix covers
   the same ground (F6 checks the envelope, not setting values; F8 refuses
   re-making and re-class back, not a person writing control facts; P8's
   "only the operator grants and revokes" is enforced by no plan reason).
   PASS.
10. **`inject` records `:seen` and `:recorded` passes for watched names**
    (inject.clj; module.clj 88, 93) besides the crash points the plan
    names (P14). Test-only, no store state. PASS.
11. **`lookup` keeps a 2-arity** `(lookup store offer)` beside the plan's
    `(lookup store name digest)` (client.clj 52-65), for the existing smoke
    test. Additive. PASS.
12. **`head` in place of the plan's `head?`** (client.clj 109-113): returns
    the stamp, as the plan's text says. Naming. PASS.
13. **`owner-in-force` takes only a keyword owner from the act** (the
    finding above). The plan's P15 "the layer's owner unioned with what the
    offer carries" holds for every act that can be admitted (an admitted
    owner fact passed `control-value-ok?`). PASS (correctness).

Checked as conforming, with no divergence: one depot `*offers` by
`hash-by :layer` with retry mode `:all-after` (module.clj 73, 78); one
stream topology `"gate"` owning `$$layers` and `$$clock` (74-76); the
`$$layers` schema of the plan with F1 and F2 (15-61); `$$clock` `Long` with
initial value 0; reads in the plan's order, record first, then settings,
clock, wall, permission rows, heads (F4; 89-114); no `ops/explode` on the
read path (F7); `decide` precomputes every write (gate.clj 261-296); the
face refusals of P7 with `:reserved-who` and `:gate-error` (F6) and the
subjects cap (F3); the reason order of the plan's step 4 with F8's two
reasons after `:stale-revoke` (gate.clj 139-182); the stamp formula of P9
(186-193); the answer record's fields without `:stood-on` (F2; 236-251);
the ack shapes of step 6 (197-211); the client functions of "The client
side" (client.clj), including `lookup` with a nil digest (F13) and `facts`
and `stood-on` by `foreign-select ... ALL` (F1, F2); the seed of the
model's one-owner world (122-164).

PASS after the fix.

## Spec conformance, beyond the template

Traced against the sources, not the plan:

- **Sid's phase 1** (SPEC.md): a depot for offers (module.clj 73); a stream
  gate topology (74); the envelope as ruled: positional core
  `{:e :k :v :replaces}` with act id and index as name and position, the
  named parts, `:version 1`, unknown parts refused (envelope.clj 24-33,
  283, 303); one-owner layers placed by layer (`hash-by :layer`, the
  `$$layers` key partitioner); N fixed at launch (the launch config); per
  task the log (`:answers` + `:log` + `:stood-on` by name, F12), answers by
  name, the task's stamp (`$$clock`); names random, made by the offerer,
  tagged with layer and class (client.clj 21-38; envelope.clj 225-235);
  the class checked against the layer's class fact (gate.clj 143-147); a
  reuse with other content refused by digest (gate.clj 200-206); a retry
  answered from the record (same); acts whole or not at all (one reason
  per act, one event); layer on the act (gate.clj 140-141). Traced.
- **I-G1** (never throw): see "Stream topology idempotency". Traced; the
  property and IPC tests of phase 5 are to show it.
- **I-G2** (idempotent under replay, same answer and stamp): traced above.
- **I-G3** (resend, answer from the record): `offer-until-answered!`.
- **I-G4** (by name plus layer, never a scan): `record`/`lookup`, one
  point read routed by the name's layer (client.clj 47-65).
- **I-G5** (check order): face checks first, reading nothing (the tag
  check among them); then the record (taken before recorded is the same
  read); then the model's order with the rig's reasons placed. The model's
  `refusal` (model.clj 404-452) has the same relative order for every
  reason it has.
- **I-G6** (whole, one stamp): every row shares the record's stamp
  (heads-put, gate.clj 279).
- **I-G7** (tagged names, digest, reserved schemes): `:mis-tagged`,
  `:wrong-gate`, `:reserved-scheme` (envelope.clj 322, 341-343); digest
  above.
- **I-G8** (envelope; unknown parts refused): envelope.clj 283, 303.
- **I-O2** (clock promises): stamp ≥ clock + 1 per task, ≥ carried
  stood-on + 1, ≥ replaced fact's recorded stamp + 1, ≥ wall.
- **I-P1, I-P3, I-P4**: the four permission checks for every non-operator;
  the record keeps who, layer (its key) and permission; the operator exempt.
- **Implicit spec stream-side edge cases**: an empty act refused on its face
  (`:empty-act`); a very large act admitted whole in one event; an act
  mixing store-placed and ordinary facts is tagged by its class (not every
  key is placed); stood-on naming an unknown fact uses the carried stamp
  (P9); a layer not made refused `:no-such-layer`, recorded (E3 L0 ×
  offer); a value about no one in a one-owner layer has the owner as its
  subject; a revoke of an unknown or revoked permission `:stale-revoke`
  (E4 P0 and P2 × revoke); re-class back `:unsupported-reclass` (E3 L3); a
  second making act `:layer-already-made`.

## Self-consistency check

Re-read of the entries above for "gap", "not ideal", "tradeoff", "does not
fully": the one defect found is stated as a finding with its fix; the
divergences are each justified by a Rama fact, a correctness requirement or
the plan's own cost model, or are naming; none is left open. The
proposed rig choices (items 6 and 8, and the doubled revoke of item 7) are
picks where the rulings are silent, written to the build notes for RIG.md.

## Verdict

**pass** (after the fix). Before it, the owner-in-force finding made this
a **minor-fail**: a localized edit to one function, applied in place and
re-traced; every other check passes on code-tracing.

## Hybrid clock (26 September)

Validated by the clock session against PLAN-stream-store.md "Hybrid clock
(26 September)" and ruling 4 ("Never backward within a unit; never earlier
than anything the fact stood on"), with CONCLUSION.md R2 as the default the
note implements (a default, not a ruling). Default verdict fail; each check
below is traced in code.

- **The encoding lives in one place.** `clock.clj` 22-49: `pack` is
  `ms × 65536 + counter` with checked `*` and `+`; `ms-of` is an arithmetic
  shift by 16; `counter-of` masks the low 16 bits; `next-stamp` is
  `max(pack(ms-now, 0), last + 1, each stood-on + 1)` by a `reduce` with
  checked `inc`. Nothing else in `src/` multiplies, shifts or masks a stamp:
  a grep for `65536`, `bit-shift` and `ms-of` outside `clock.clj` finds R16's
  constant 2^62, the digest's hex encoder, the UUID7 builder (names, not
  stamps) and one docstring; the bench's two reads use `ms-of`. Pass.
- **Never backward within a unit.** The unit is the task (I-G9). `module.clj`
  100 reads the task's last stamp (`STAY $$clock`) in the same event that
  146 sets `$$clock` to the decided stamp; `gate.clj` 188-197 passes it as
  `last`, so the stamp is at least `last + 1`. Every decision that gives a
  stamp (a yes and a recorded no, `decide*` 269) writes the clock; the
  recorded and taken paths (`module.clj` 95) and face refusals give none and
  write none. A crash before the commit discards the clock write with the
  rest; the replay reads the old clock and stamps at least one past it.
  Pass.
- **Never earlier than anything the fact stood on.** `stamp-for` hands
  `next-stamp` every carried stood-on stamp (`(vals (:stood-on offer))`) and
  every replaced fact's recorded head stamp (`heads`, read at `module.clj`
  108-114), each + 1. The carried values are parsed as longs below 2^62
  (`envelope.clj` 270, 315, 336, R16). Pass.
- **At the wall's millisecond, within clock skew.** `wall-now` (`gate.clj`
  17) is `TopologyUtils/currentTimeMillis` on the deciding task, packed with
  counter 0, so the stamp's millisecond is the wall's unless the task's last
  stamp or a stood-on stamp has reached that millisecond's counter 65535;
  then it is theirs + 1 (the counter carries). Pass, as the note states it.
- **One number.** Every stamp is a Long in the same slots as before
  (`module.clj` `:stamp`, `:stood-on`, `:heads`, `$$clock`); no schema
  changed, no field added. `max`, `inc` and `+` over longs return Long, which
  the schema requires (a Double or Integer would throw in the write). Pass.
- **The gate never throws.** `next-stamp` can throw only on overflow
  (`ArithmeticException`) or a non-number (`ClassCastException`); it runs
  inside `decide`'s `catch Throwable` (`gate.clj` 302-311), which answers
  `:gate-error` unrecorded. Overflow needs a carried stamp past 2^62, which
  the parser refuses, or a wall past millisecond 2^47 (the year 6400). Pass.
- **A resend or replay keeps its recorded stamp.** `answer-from-record`
  returns the record's `:stamp`; nothing on that path calls `stamp-for`.
  Pass.
- **Idempotency and I/O.** No new read, write, PState or partitioner; the
  clock is still one `STAY` read and one root `termval` per decided offer.
  The earlier "Stream topology idempotency" entry stands: the clock write is
  a set of a value computed before any write. Pass.
- **Plan conformance.** `wall-now` stays in the gate and is packed by
  `next-stamp`; `decide`'s arguments are unchanged (`clock` a stamp, `wall`
  milliseconds); comments in `module.clj`, `client.clj` and `envelope.clj`
  name the unit; the bench's `:stamp-ahead-of-wall-ms` and probe line read
  the millisecond with `ms-of`. No divergence. Pass.
- **Left as the note says.** `:claimed-when` stays the offerer's wall
  milliseconds (`client.clj` `build`); R16's bound is unchanged; `$$clock`
  starts at 0. The model's `stamp-for` computes the same maximum over ticks;
  answers compare, stamp values are not compared.

Self-consistency: no entry above names a gap left open; the counter carry
past 65,536 decisions in one millisecond is the stated behaviour of the
default, not a defect. **Verdict: pass.**
