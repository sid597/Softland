# Implementation validation — step R, the fixes from the review of wave 1

The rama skill's phase 4, done by the builder in the build session (as
`phase-build.md` has it: "you are doing that work yourself, not a fresh
reviewer"), against `PLAN-review-fixes.md` as validated
(`PLAN_VALIDATION-review-fixes.md`, minor-fail, fixed in place) and the
spec it carries (C's brief, `REVIEW-wave1.md` R-1, R-2 and F-1, RIG.md For
Sid 4 and 37). Default verdict major-fail; each check below was traced in
the code at the lines cited, on branch `rig-build-fixes` after the merge of
`rig-2026-09-25` (`f89addf0`), before any run.

What changed, in four source files:

- `gate.clj` 63 to 72: `misplaced-setting?`; 232 to 235: the clause in
  `refusal-with-rows`' `:malformed-control` slot; docstrings of `refusal`
  and `micro-control-keys`.
- `client.clj` 102 to 121: `refuse-misplaced!`; 264 (`offer!`) and 317
  (`offer-until-answered!`): its calls.
- `micro_client.clj` 179 to 206: `seal` throws `:no-lock`; 264 (`offer!`)
  and 333 (`write!`): the door check.
- `reads.clj` 1070 to 1109: `entry-facts`' pattern line takes
  `:recorded-pattern`; `recorded-pattern`'s docstring.

No schema, depot, partitioner, topology or query topology changed.

## The template's Rama checks

- **Redundant conditionals, consecutive keypaths, select-compute-transform,
  unnecessary nil->val, `:allow-yield?`, non-subindexed collections without
  size limits, no reimplementation of built-in operations.** No dataflow
  code, path or PState write was added or changed. The one change that runs
  inside a topology is `misplaced-setting?`, a plain Clojure predicate
  called by `refusal-with-rows` (a pure function), which runs in the stream
  gate's event (gate_event.clj 163, `gate/decide`, then `decide*` at
  gate.clj 446) and in the micro fold (micro.clj 926, `micro-decision`'s
  call to the 4-arity). It reads the offer in hand: no seek, no iteration of
  a PState, O(facts) comparisons (at most the act's facts). **Pass.**
- **Stream topology idempotency; partial failure in stream topologies.** A
  replayed stream event recomputes `decide` from the same offer and the same
  reads; the new clause is a pure function of the offer, so a replay reaches
  the same reason and writes the same record (a refused act writes its
  answer record and, stage 2, its consumption; nothing new). No write was
  added. **Pass.**
- **Totality in topology code** (the rig's rule: an exception in topology
  code is fatal to the worker, and a microbatch that throws
  deterministically retries for ever). `misplaced-setting?` (gate.clj 72):
  `(boolean (and (map? f) (contains? env/setting-keys (:k f)) (not=
  (:layer offer) (:e f))))`. For a non-map fact it is false; for a nil offer
  `(:layer nil)` is nil; `contains?` on a set takes any key. It cannot
  throw. The stream path is also inside `decide`'s catch (a throw there is
  the face refusal `:gate-error`); the micro fold has no catch around
  `micro-decision`, so totality is what matters there, and it holds.
  **Pass.**
- **Single depot append per client operation.** The doors now make zero
  appends for an act they refuse (`refuse-misplaced!` throws before
  `foreign-append!`; `seal` throws before `offer!`). `write!` keeps its two
  appends (the lease act, then the value act), the lease road's design since
  phase 2, not changed here. **Pass.**
- **Application-state caches survive restart.** No cache added. The door's
  checks are stateless: a restarted door refuses the same offer the same
  way. **Pass.**

## The plan's changes, traced

### R-1, the gates

- **Check:** "the `:malformed-control` clause gains `(some
  #(misplaced-setting? offer %) facts)`" and "both gates call `refusal`".
- **Code:** gate.clj 232 to 242: the `or` in the `:malformed-control` slot
  begins with the new disjunct (234). The slot sits after the permission
  reasons (229 to 230) and before `:control-not-allowed`, the order L27
  and the micro gate's `reason-order` already hold; no reason moved.
- **Trace, stream.** Alice's raw act `{:e :rv1 :k :owner :v "t"}` in
  `:alice` with her permission, settings `{:kind :personal :owner :alice
  :class :by-layer}`: 218 no fact outside the layer; 221 in force
  `:by-layer`; 224 the class matches; 229 `permit/refusal` nil; 234
  `misplaced-setting?`: `:owner` is in `env/setting-keys`, `:rv1` is not
  `:alice`, true, so `:malformed-control`. `decide*` (439 onward): `yes?`
  false, so no `:log`, `reads/no-index-writes`, no heads, no `:settings`,
  no permission rows; the record is a no with this reason.
- **Trace, micro.** The operator's `{:e :rv1m :k :lock-grain :v "t"}` in
  `:group`: `micro-decision` (micro.clj 905 onward) calls `(gate/refusal o
  settings rows heads)` (926), the 4-arity, `refusal-with-rows` with nil
  key rows: exempt, so no permission reason; 234 true, `:malformed-control`.
  `decide-envelope` places it with `first-in-order`; `micro-extras` adds
  nothing for a setting key off the layer (its `:class` clause looks only at
  setting facts on the layer, `gate/setting-fact?`). The fold writes the
  name row and the answer, no heads, no settings version (the updates in
  `micro-decision` come from setting facts on the layer only).
- **Unchanged cases.** A setting fact on the layer's own entity:
  `misplaced-setting?` false (`(:e f)` equals `(:layer offer)`), every later
  clause as before. `:members` at the stream gate: not a setting key, so
  `refusal` passes it and `stream-refusal` (gate.clj W1-7) refuses it.
  **Pass.**

### R-1, the doors

- **Check:** "`refuse-misplaced!` ... before anything is sealed, leased or
  appended. Called first in `c/offer!` and `c/offer-until-answered!` ...
  and in `mc/offer!` and `mc/write!` (... right after it builds the offer,
  before it reads the grain or leases). It is total ...".
- **Code:** client.clj 102 to 121; calls at 264 (first form of `offer!`'s
  body, before `sealed` and `foreign-append!`), 317 (first form of the
  3-arity of `offer-until-answered!`, before its loop, which would catch the
  throw as an append error and retry for 15 s); micro_client.clj 264
  (first form of `offer!`'s 3-arity; the 2-arity delegates to it) and 333
  (the binding right after `build`, before `value-count`, `settings-of` and
  `lease!`).
- **Totality:** `facts` is taken only when the offer is a map with a
  sequential `:facts`; `misplaced-setting?` filters non-maps out before
  `(juxt :e :k)`. A nil, a string or a map without facts gives nil and the
  door goes on to the gate's parse, as before.
- **Trace:** `(c/offer! st o)` with Alice's `{:e :rv1 :k :owner}`: 264
  throws `{:door :refused :reason :malformed-control :name nm :misplaced
  [[:rv1 :owner]]}`; nothing sealed, nothing appended, the door's `:held`
  untouched (the throw comes before `sealed`), `answered!` not called.
- **Callers that must still pass:** every setting fact any `src/` caller
  builds is about its layer (`make-layer-offer`, `make-base!`,
  `make-group!`, `reclass-offer`, the tests' grain switches; the plan's
  search, repeated on this tree). **Pass.**

### R-2

- **Check:** "the line's `:pattern` is the answer's `:recorded-pattern` ...
  A pattern answer that carries no `:recorded-pattern` gives no entry
  facts".
- **Code:** reads.clj 1102: `(when-let [recorded (:recorded-pattern
  answer)] [...])`, the line's `:pattern` `recorded`; the rest of the line
  unchanged.
- **Trace:** the review's read `[:kv :note "rv2 ..."]` of the base (one
  owner): `read-pattern` ends in `pattern-answer` (1034), whose 1043 sets
  `:recorded-pattern (recorded-pattern (:pattern pp))` = `[:kv :note
  {:keyed h}]`; the exit's `entry-offer` (read_exit.clj 103) calls
  `entry-facts`, which puts that in the line. A shared read's answer comes
  from the same `pattern-answer` (shared_reads.clj 466 and 1430). No text
  of the value is in the line: `:exact` holds `[fid stamp]` pairs, the
  fingerprint is over those pairs.
- **The no-key road:** a hand-made answer without the key gives nil; in
  the exit that is an entry offer with no facts, refused on its face
  `:empty-act`, so `read!` shows nothing and answers `{:refused :empty-act
  :entry nm}`. No module answer takes that road. **Pass.**

### F-1's last edge

- **Check:** "`seal` ... when `locks/seal` gives nil for a value fact ...
  throws `{:door :no-lock :name nm :lock-id lid}` ... `write!` gets no lock
  check of its own".
- **Code:** micro_client.clj 196 to 204: `sealed` is computed first, and
  `(when (nil? sealed) (throw ...))` at 199; the fact is built only from a
  non-nil `sealed`. `write!` (313 onward) has no lock check besides.
- **Trace:** a yes lease of 2 ids under per-value grain, `take-locks`
  giving `{}`: `seal` (345) assigns id 0 to the first value; `(get {}
  [ln 0])` is nil; `locks/seal nil ..` returns nil (it takes only a
  32-byte lock, locks.clj `seal-with`); `seal` throws `{:door :no-lock
  :name nm :lock-id [ln 0]}`; `offer!` is never reached, so nothing is
  appended and no answer is recorded under `nm`. With `{[ln 0] K}` only:
  the first value seals, the second throws with `[ln 1]`.
- **The refused lease (F-1's fix):** `seal-ks` holds a fresh lock for every
  id (the `:ids` of the refused lease), so `seal` never throws there.
- **Other callers of `seal`:** every one in `micro_test` and
  `micro_prepare_test` passes a lock for each id it assigns (the plan
  validation's check); none relies on `:sealed nil`. **Pass.**

## Plan conformance

Every change is the plan's, as validated. The one detail the plan leaves
open, `misplaced-setting?`'s exact form, is `(boolean (and (map? f) ...))`:
the plan says "pure and total", and the `map?` guard is what makes it total
over the micro fold's inputs, which the plan validation found include
`micro_prepare_test`'s generated facts. No divergence. **Pass.**

## Self-consistency

No block above names a gap, a tradeoff or a "not ideal". The limits the
plan names (the door appends a control value out of shape; a client that
is not the door can put its own text in a depot; O1; the For Sid 4 caveat)
are outside this step's scope by the plan and C's brief, and go to the
notes; they are not failures of this implementation against its plan.

## Verdict

pass — each change is the validated plan's, every path traced holds, and
nothing inside a topology can throw or read more than before.

PHASE_VALIDATION:pass
