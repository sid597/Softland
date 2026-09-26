# Test validation — step R, the fixes from the review of wave 1

The rama skill's phase 6, done by the builder in the build session, over
the tests as written before any run: `test/rig/store/review_fixes_test.clj`
(new, pure), `test/rig/store/review_wave1_test.clj` (the review's cluster
namespace, its two PENDING blocks made ordinary, three blocks rewritten,
one added) and `test/rig/store/reads_test.clj` (its `entry-facts` test's
hand-made answer). Against `PLAN-review-fixes.md`'s "Tests" and the spec it
carries. Default verdict major-fail; every check cites the block that
exercises it. All three namespaces load (`clojure -M:load -e (require
...)`, no cluster, 26 September).

## Minimize IPC launches

`review_wave1_test` keeps its one `deftest` and its one in-process cluster
(4 tasks); step R's blocks go inside it, the micro ones inside its existing
group `let`. `review_fixes_test` starts no cluster: six `deftest`s of pure
calls (the stream gate's `decide`, the micro fold's `micro-decision`, the
doors with a nil store, `with-redefs` over the micro door's client calls),
seconds long in all. `reads_test` is pure. **Pass.**

## Coverage of the plan's items (the spec here)

R-1, the gates:
- Each of the four setting keys about another entity is `:malformed-control`
  at the stream gate, by the layer's owner and by the operator, a recorded
  no with no row, no index entry, no settings and nothing of the value in
  the record: `review_fixes_test/the-stream-gate-refuses-a-misplaced-setting`
  (doseq over keys and actors, through `gate/refusal` and `gate/decide`).
  End to end, by a client that is not the door (a raw `foreign-append!`):
  `review_wave1_test` "R-1 (step R, W1-7 widened)", each answered `[:no
  :malformed-control]`, no row (`c/raw-rows`), no id-index entry
  (`entries-of`), no index field holds its text (`fields`, now with
  `:ix-s`), the exit's `[:e :rv1]` shows none, and the owner's forget of
  one answers `[:no :no-such-value]`.
- The same at the micro gate, for each key: `the-micro-gate-refuses-a-
  misplaced-setting` (`micro-decision` and `first-in-order` with
  `micro-extras`, no settings version); end to end by `mc/send!` and
  `mc/await-answer`, `[:no :malformed-control]` and no row on `:rv1m`:
  "R-1 at the micro gate (step R)".
- A misplaced setting beside a well-placed one still refuses the act:
  "beside a well-placed setting fact".
- Unchanged decisions: the owner's grain switch admitted,
  `:control-not-allowed` for the owner rewriting her owner,
  `:layer-already-made` for the operator, the group's own grain switch
  admitted ("... decided as before", both gates); W1-7's `:members`
  (`stream-refusal`, pure, and end to end in "R-1 (step R): the other
  control keys"), and phase 4's `:promote-request` and `:crossed`
  refused `:malformed-control` with nothing kept (same block).

R-1, the doors:
- Each door throws `{:door :refused :reason :malformed-control}` before any
  store access (`c/offer!`, `c/offer-until-answered!` with one try,
  `mc/offer!` with a 1 ms timeout, `mc/write!`, each with a nil store, so a
  missing check fails fast instead of hanging or passing):
  `no-door-sends-a-misplaced-setting`; what the refusal names, a mixed act
  naming only the misplaced fact, and a well-placed act and a value act
  passing the check (same deftest).
- End to end: the honest stream door (`send!`, i.e. `c/offer-until-
  answered!`) throws for each of the four keys, nothing is appended under
  the door's name and no record in `*offers` holds the door's text ("R-1
  (step R, W1-7 widened)"); the honest micro door (`mc/write!`) throws,
  nothing is appended under its name and no record in `*micro-offers`
  holds its text, the review's "no plaintext value in the micro depot"
  ("R-1 at the micro gate (step R)", checked before the raw sends, which
  carry other texts).
- The depot copies the plan leaves as questions are printed, not asserted:
  a raw client's own records, and a control value out of shape sent
  through the door (OBSERVED lines in the two stream blocks).

R-2:
- The module's answer carries the keyed pattern, the line records it and
  holds no text, the exact list is ids and stamps, other forms are
  recorded as they are, and an answer without the key gives nil:
  `a-kv-line-keeps-its-value-keyed` (over `reads/pattern-answer`, the
  module's function).
- End to end, the review's case made ordinary: after `:rv-b`'s forget,
  Alice's entry line equals `(reads/recorded-pattern [:kv :note text])`,
  `pairs?` is false, the text is nowhere in the entry, and the two "holds"
  stay: "R-2 (step R)".
- `reads_test/entry-facts`: its hand-made answer carries
  `:recorded-pattern` as module answers do, and the `[:kv]` case its keyed
  form, so its existing assertions keep testing what they tested.

F-1's last edge:
- A yes lease with no lock taken, with one of two, with every lock, and a
  refused lease (F-1's fix unchanged), with `lease!`, `take-locks` and
  `offer!` redefined: `seal` throws `:no-lock` at the first value with no
  lock and `offer!` is never called; with every lock, or after a refused
  lease, one sealed envelope is sent: `the-micro-door-never-seals-under-
  no-lock`. `seal` alone: no lock, or not a lock (3 bytes), throws with
  the act's name and the id; a lock seals (same deftest).
- End to end: "F-1's last edge (step R)", on the cluster, with
  `take-locks` giving `{}` and then the lease's first lock only: `:no-lock`,
  the lease (named by the lock id) answered yes, nothing appended and no
  answer under the act's name, and `seal` stopping at id 0 and id 1.

## Synchronization

Every read that follows a write waits for the write's decision: the stream
door's `send!` and the raw `foreign-append! ... :ack` return after the
gate's event (the stream ack); `c/forget-value!` goes through
`offer-until-answered!`; the micro raw sends are followed by
`mc/await-answer`, which polls until the envelope's own answer, and a
microbatch commits its answer and its rows together, so a row an admitted
act wrote would be visible when its answer is; `mc/write!`'s lease is
awaited inside `lease!` before `mc/record-of` reads it. The depot reads
come after the appends they check. **Pass.**

## Test namespaces compile

Loaded, with the changed sources, by `(require 'rig.store.review-fixes-test
'rig.store.review-wave1-test 'rig.store.reads-test)`: `:loaded`. The new
namespace imports `ExceptionInfo` and `UUID`, requires only public vars
(`gate/misplaced-setting?`, `c/refuse-misplaced!`, `micro/micro-decision`,
`micro/micro-extras`, `micro/first-in-order`, `mc/seal`, `mc/write!`,
`reads/pattern-answer`, `reads/entry-facts`, `reads/recorded-pattern`,
`locks/lease-ids`, `locks/lease-name-of`, `locks/fresh-lock`). **Pass.**

## Self-consistency

Nothing above is marked missing or weak. The things the tests only print
are the plan's named questions for Sid, not uncovered behaviour of this
step.

## Verdict

pass — every item of the plan's "Tests" is exercised by a named block, in
both a pure form and end to end where the plan asks, with the waits in
place.

PHASE_VALIDATION:pass
