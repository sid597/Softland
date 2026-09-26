# Test Validation — stage 4, "promotion"

<!-- The rama skill's build step 4 (phase-6-test-validate.md, template
artifact-test-validation.md), 26 September 2026, 06:25 IST, by the build
session (Claude Opus 5.5, effort max), branch rig-build-promotion.
Default verdict major-fail. Validated: test/rig/store/promote_unit_test.clj
(7 deftests, no cluster) and test/rig/store/promote_test.clj (one cluster
deftest, `promotion`) at b0721065, against IMPLICIT_SPEC.md OP13 to OP15,
PLAN-promotion.md's T1 to T14 and U1 to U4, its fixes F1 to F9, and the
build's divergences (IMPLEMENTATION_VALIDATION-promotion.md). -->

**Verdict: minor-fail, fixed in place.** Three findings, each fixed by
editing the existing namespaces and committed: TV-1, a second cluster
launch with no state that justified it; TV-2, implicit-spec cases with no
test; TV-3, an assertion that assumed an order nothing promises. After
them every check below passes by walk-through.

## Findings and fixes

| # | Finding | Fix | Commit |
|---|---|---|---|
| TV-1 | The hold hook's test launched its own cluster (`deftest the-hold-hook`). No shared mutable state made it interfere with the main deftest: it needs only the base on the stream gate, which the main deftest has before it makes the group. | Folded into `promotion`'s base phase as the `testing` block "the hold hook: ...". | 7bf0515e |
| TV-2 | OP13 to OP15 cases without a test: sources in a hand or agent session; two promotions of one value; a refused request's status; a request for a value already erased; a fact with no value; a per-act grain source; the source layer re-classed before the read-out (the build's V-2); a landing citing a permission never granted; the copy carrying no marks; a replace of a head erased since; F2's small-order key at the read-out on a cluster; the stream gate's D6. | `testing "T15: ..."` (its eleven blocks) and `testing "D6: ..."`. | 7bf0515e, b0721065 |
| TV-3 | T7 a asserted the replayed append existed as soon as the promotion was done. The replay and the landing's batch both restart with the worker; nothing orders the replay's append against the batch that decides the first one. | `wait-until` the second record, with the reason in the message. | b0721065 |

Found by running, not by this walk-through, and fixed in the same
commits: T11 read every session's lease map in one foreign select, which
timed out (each value is a subindexed map); it now reads the sessions by
`MAP-KEYS` and each session's rows by `client/lease-rows`.

## Minimize IPC launches

`promote_test.clj` has one deftest, `promotion`, one `create-ipc` and one
`launch-module!`; every case is a `testing` block in it, on disjoint
entities and sessions (`:t1`, `:s/t1`, ...), so none interferes with
another. Person forgets are irreversible, so B2, B4 and T13 use fresh
persons (`:ann`, `:ava`, `:abe`, `:bea`) rather than a cluster each; a
re-class is irreversible, so V-2's case uses `:amy`, whose layer no later
case uses. The base's re-class is the one ordering constraint: every
stream-target case runs before `mc/make-group!`. `promote_unit_test.clj`
launches nothing. PASS.

## Implicit spec coverage

Each item of IMPLICIT_SPEC.md's OP13, OP14 and OP15, and each of the
plan's named tests, with the block that exercises it (file
`promote_test.clj` unless marked *unit*).

**OP13, the request.**

| Item | Test |
|---|---|
| An act in the owner's layer naming source, target, head, landing permission | *unit* `u4-the-request-check`; every `pc/promote!` case; the head in T9 (b), T15 "two requests" |
| It stands on the source, stamped after it | T5 "the source, the request, the crossing, the landing: in stamp order" |
| The read-out's and landing's names fixed before the first gate | `promote-held!` (the request's name made and held before anything is sent); *unit* `u2` (names from the request) |
| Sources in hand or agent sessions (O18) | T15 "a hand session's and an agent session's value ..." |
| "Say all of this at the point of promotion" | T14; T1's `:pending` statement; *unit* `u3` (the statements) |
| A yes continues at least once, again on every replay | T6 (both targets), T7 a and b, the hold hook |
| Two requests for one value | T15 "two requests for one value ..." (both land; the second stale when the first moved its head) |
| A retried request lands once | T6 on the stream gate and into the group |
| A refused request: no read-out, no status | T15 "a refused request is no promotion" |
| A request for a value already erased | T15 "a request for a value already erased ..." |
| A request for a fact with no value | T15 "a request for a fact with no value: no such fact, a retract" |

**OP14, the read-out and the forward.**

| Item | Test |
|---|---|
| The store's own step: named from the request, no permission, claims no class, placed where the source's lock is | *unit* `u2` (`:who :store`, no permission, nil class in its name); T15 "the source layer re-classed ... (V-2)" |
| Opens the value through its lock; writes the crossing only if it opens | T3, T4, T8 (yes); T1, T2 (no crossing yes, no forward, no landing) |
| Refuses when the source has no value or is erased (row deleted, excised, wrap closed) | T1 (row deleted), T2 (wrap closed), T15 "already erased", "a fact with no value"; *unit* `the-read-out-verdict` |
| A forget before it refuses; after it the copy is not recalled | T1, T2 / T3, T4, T8, T7 (b, held) |
| After a yes it sends the landing; a replay sends it again | T6, T7 a and b; the stored forward sent after the source's erasure: T7 (b, held), reported against the model's `forward` |
| The model's "crossed for ever" consequence | T7 (b, held): the rig's stored forward lands where the model's would send nothing; reported, not hidden |
| A per-act grain source | T15 "a source under per-act grain" |
| A re-classed source layer | T15 "... re-classed between the request and the read-out (V-2)" |
| With a value forget on the same task: ordered there | T5 (the forget's stamp either side of the crossing's, B1 and B3) |
| An opaque source | not testable in this stage: no key's grammar declares opaque values until phase 6 writes grammars as facts; the read-out opens an opaque value as any other (derived, same `open-row>`) |
| Whether the read-out leaves a read entry | open (O19); nothing to test |

**OP15, the landing.**

| Item | Test |
|---|---|
| Named from the request; a second landing a duplicate | T6, T7 a (at least two records, one answer, one copy row) |
| As the requester, citing the landing permission in the target | T9 (a) (a session permission revoked), T15 "a landing citing a permission that was never granted" |
| Stamped after the crossing and the source | T5 (stamp order) |
| The copy: source's e, k, value; target layer; the head replaced; no marks; a new lock by the target's rule under its own subjects | T13 (wrapped `any-of [:bea]`), T15 "the copy carries no marks", T9 (b) (the head), T8 (the record lock opens it) |
| Refused: stale replace, permission revoked or missing, class mismatch; ends refused after crossed | T9 (b), T9 (a), T15 (never granted), T10 c |
| The landing does not check its source | T3, T4, T7 (b, held) |
| A landing after the source was forgotten: done, the copy open | T3, T7 (b, held), T8 |
| The head named since erased: the replace allowed | T15 "the head the request names, erased since" |

**The plan's tests.** U1 `u1-the-box`; U2 `u2-the-store-made-offers`;
U3 `u3-the-status`; U4 `u4-the-request-check` (all *unit*). T1 to T14 as
named `testing` blocks. T1 to T4 assert the model's `:shown` sequences and
`:values` from `formal.scenarios/b-cases` (with a fresh person in Alice's
part for B2 and B4, said in the namespace's docstring).

**The plan's fixes.** F1: *unit* U4's lease cases and
`the-landing-lease-and-crossing-checks`; T12 (a) to (d). F2: U1's
non-key and small-order cases; *unit* `the-read-out-verdict`; T15's
small-order request on the cluster. F3: T12 (c), (d); T9 (c). F4: T6's
landing answered unchanged across resends of the stored envelope;
*unit* U2's forward. F5: *unit* U3; T5's `:none` before the request. F6:
T7 a (the landing found its lease row where it arrived). F7: T13's
subject forgotten after landing (the window itself, between the
read-out and the landing, is phase 2's accepted one and is not tested as
a failure). F8: reversed by the build (V-2), tested in T15. F9: T4 (the
row outlives the forget, bare, and is consumed by the landing).

**The build's divergences.** D2 (the ack's shape): T6's acks; D3
(`:landings`): every `lease-landing!`; D6: `testing "D6: ..."`.

PASS after TV-2.

## Synchronization

- Stream-gate writes are read after their ack, which returns when the
  record's whole event tree is done (the request, the read-out, the
  forward's append or the stream landing).
- Micro-gate writes are read after `mc/offer!` or `mc/await-answer`
  (micro-lookup through the frontier), or `settled!`, which polls
  `promotion-status` until done or refused, and the status reads the
  landing only when its batch is at or below its task's frontier.
- `micro-copy` reads through the frontier of the name's task, which
  `settled!` has seen past the landing.
- Reads made while the micro topology is paused (T3, T4, T12 d) are of
  stream-side state or of PState rows read raw (`mc/lease-rows`), never of
  a micro answer.
- T7 a's second record: waited for (TV-3).
PASS after TV-3.

## Test namespaces compile

Both load: `(require 'rig.store.promote-test)` with the test paths, and
`clojure -M:test rig.store.promote-unit-test` runs. No private namespace
is required; the helpers the tests need are public (`promote-client`,
`promote-shape`, `box`, `micro-client`). PASS.

## Self-consistency

The one item marked not testable (an opaque source) is outside this
stage: no key is opaque before phase 6. The rest are covered by the
named blocks. TV-1 to TV-3 were fixed before the verdict.

## Verdict

**minor-fail**: three findings, each fixed by editing the existing test
namespaces; no new namespace or restructuring.

PHASE_VALIDATION:minor-fail
