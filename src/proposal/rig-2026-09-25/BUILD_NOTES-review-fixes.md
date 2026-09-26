# Build notes — step R, the fixes from the review of wave 1

Builder R (Claude Opus 5.5), reporting to builder C. Branch
`rig-build-fixes`, worktree `/mnt/data/projects/Softland-rig-build-fixes`,
cut from `c9684356`, with `rig-2026-09-25` merged in at `f89addf0` (wave 2
landed at `27543fd7`, and phase 8's pass 3 after it). The step, the plan
and its validation: `PLAN-review-fixes.md`,
`PLAN_VALIDATION-review-fixes.md`; the build's own validations:
`IMPLEMENTATION_VALIDATION-review-fixes.md`,
`TEST_VALIDATION-review-fixes.md`.

## Receipt

Commits, in order:

| commit | what |
|---|---|
| `2c32c208` | the plan, validated by a fresh session (rama phase 2: minor-fail, sixteen localized edits in place) |
| `f89addf0` | merge of `rig-2026-09-25` (wave 2, then phase 8's pass 3) |
| `468476f6` | the source changes (gate.clj, client.clj, micro_client.clj, reads.clj) and the implementation validation (pass) |
| `30e9cf81` | the tests (`review_fixes_test.clj` new; `review_wave1_test.clj`; `reads_test.clj`) and the test validation (pass) |
| *(the commit of these notes)* | these notes and the run's summary, `runs/review-fixes-run.txt` |

The run: 54 tests, 2,218 assertions, 0 failures, 0 errors (one run, 389 s; "The run" below).

## What was built

**R-1 (For Sid 37, W1-7 widened): a setting key about another entity.**
After wave 2 the one control-key fact still admitted and never acted on is
a setting key (`:kind :owner :class :lock-grain`) on an entity other than
the act's layer: the stream gate admitted all four, the micro gate
likewise; each kept its value as plaintext in the log and the indexes,
under no lock, beyond every forget (phase 4 already refuses a client's
`:crossed` and checks a `:promote-request` whole; W1-7 refuses `:members`
at the stream gate).
- `gate/misplaced-setting?`, and one disjunct in `refusal`'s
  `:malformed-control` clause. Both gates decide stage 1's list by
  `refusal` (the stream gate through `decide`, the micro fold through
  `micro-decision`), so one clause refuses it at both, a recorded no with
  no row, no index entry, no heads and no settings.
- `client/refuse-misplaced!`, called first by both doors (`c/offer!`,
  `c/offer-until-answered!`, `mc/offer!`, `mc/write!`): the door throws
  `{:door :refused :reason :malformed-control :name .. :misplaced [[e k]
  ..]}` before anything is sealed, leased or appended. A depot keeps what
  is appended, so only the sender can keep that plaintext out of it; the
  review's "no plaintext value in the micro depot" is the honest door's
  property. The raw roads (`foreign-append!`, `mc/send!`) stay open, and
  the tests use them to play a client that is not the door.

**R-2 (For Sid 4): a `[:kv]` read line keeps its value keyed.**
`reads/entry-facts` records the answer's `:recorded-pattern` (reads-rest's
`recorded-pattern`: `[:kv k {:keyed hex}]`, an HMAC under the fingerprint
secret), in every `:read/pattern` line, one-owner and shared, as the
standing read's opening line already did. The exit runs on the client's
side and never keys a value itself; a pattern answer without the key gives
no entry (the exit's offer is then an empty act, refused on its face; no
module answer takes that road).

**F-1's last edge: the micro door never seals under no lock.**
`micro-client/seal` throws `{:door :no-lock :name nm :lock-id lid}` when it
holds no lock for a cited id (or not a lock), where it used to put
`:sealed nil` in the envelope. So `write!`, after a lease answered yes whose
locks `take-locks` could not take in time (all or some), sends nothing, as
the stream door throws after its tries. The lease stays answered yes; its
unconsumed rows go when the session closes. A refused lease keeps F-1's fix
(throwaway locks citing the would-be ids, `:no-such-lock`).

**The review's pending tests are ordinary assertions.** The `pending?`
switch is gone; the R-2 block asserts the keyed line and no text; the R-1
blocks are rewritten for the refusals at both gates and doors; an F-1
edge block runs on the cluster (`take-locks` redefined to give what its
timeout gives, `{}`, then the lease's first lock only). The merge's W2-2
lines (toy grammars in the seed and the O11 probe) are kept.

## Divergences from the plan

None. Two things the validated plan left to the build: `misplaced-setting?`
is `(boolean (and (map? f) ...))`, total over the micro fold's generated
facts; and two small test cases were added at test validation (a misplaced
setting beside a well-placed one at the gate, and beside a value fact at
the door).

## The run

One run, once each (Sid's rule), under the cluster lock, taken at 08:15:25
IST with no queue, finished 08:21:54 (389 s), on `30e9cf81`:

```
flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:test rig.store.review-fixes-test rig.store.review-wave1-test rig.store.envelope-test rig.store.grammar-test rig.store.reads-test rig.store.micro-prepare-test rig.store.stream-gate-test rig.store.read-exit-test rig.store.micro-test
```

**54 tests, 2,218 assertions, 0 failures, 0 errors** (summary
`runs/review-fixes-run.txt`, full log `runs/review-fixes-run1.log`,
git-ignored). Nothing needed a fix, so nothing was rerun. The namespaces:
the two whose assertions step R writes, and the tests of the four source
namespaces it changes (the plan's "Runs" says why the others cannot change
outcome). The full suite is C's.

What the run showed beyond the assertions (OBSERVED lines): Alice's entry
after `:rv-b`'s forget reads `{:pattern [:kv :note {:keyed "0d07..."}]
:exact [[fid stamp]]}`; a raw client's four refused records are in
`*offers`, and so are the door's `:promote-request`, `:crossed` and
`:members` texts refused for their shape (the two questions below); `seal`
stopped at id 0 with no lock taken and at id 1 with one of two.

## For RIG.md

**Rig choices (C assigns the R numbers).**
- **A setting key about another entity is refused at both gates**
  (`:malformed-control`, `gate/refusal`), extending W1-7 (R26): no gate
  admits a control-key fact it does not act on. Changes what a record may
  carry (narrows it); For Sid 37's road 1, built as its default.
- **No door sends one**: both doors throw before sealing, leasing or
  appending (`client/refuse-misplaced!`), so no depot holds that plaintext
  from the door. Touches no record.
- **Every `:read/pattern` line records the module's keyed pattern**
  (`entry-facts` takes `:recorded-pattern`), one-owner and shared; a
  pattern answer without it gives no entry. For Sid 4's default, built.
- **The micro door never seals under no lock**: `seal` throws `:no-lock`;
  `write!` after a yes lease whose locks it could not take sends nothing.
  Touches no record.

**For Sid, updates to existing items.**
- **37 (control keys).** Built: road 1 at both gates and both doors, as the
  entry said the build would. Road 2 stays the alternative (seal the key
  as a value away from the place the gate acts on: the model's reading for
  `:kind` and `:owner`). *Where the rig differs from the model:* the model
  has no `:kind` or `:owner` key (a fact under either is a sealed value
  there), and its `apply-control` acts on `:class` and `:lock-grain`
  whatever the fact's entity (model.clj 467 to 470); the rig refuses all
  four about another entity. The model's own acts put every setting fact on
  the layer's entity (`op-layer-setting`), so no fixed history shows it.
  *The sub-question (O1), sharpened by the plan validation:* after step R
  every admitted control-key fact is one its gate acts on, in its place;
  that is not the same as holding no text. A `:grammar` value's shape may
  carry strings (`[:enum v ...]`, up to 64 EDN values, `shape.clj`), and a
  promotion request's `:public` is 32 bytes the requester chooses; a forget
  of either answers yes with `:how nil` and erases nothing. Should such a
  forget be refused, and may a grammar carry text?
- **4 (what a read entry may hold of a value).** Built: keyed in every
  line. The fact to add: keyed stops a reader reading the value off a
  line, not confirming a guess. The module keys any `[:kv]` read's value
  the same way, so a reader who can read an old line can issue `[:kv k
  guess]` against a layer she can read where `k` is indexed by value (the
  answer need not match anything) and compare her new line's digest with
  the old one; for a short value that confirms a forgotten value. The forms
  that confirm nothing: keep nothing of the value (the line cannot re-run),
  or key it under a salt the reader cannot choose again (the entry's own
  name, if the module mints it). The line form and its label stay
  reads-rest's first-record placeholder.

**New questions for Sid.**
- **Should the door check a control value's shape before it appends?** The
  honest door appends a control fact's value as given. When the shape is
  wrong (free text under `:forget`, `:permission`, `:promote-request`,
  `:crossed`, or `:members` sent to the stream gate) the gate refuses it,
  but the text is already in the depot, beyond every forget (the review
  namespace prints these depot copies as OBSERVED). Closing it means the
  door runs the gates' shape rules on every control fact before sending,
  which changes the door's road for every control fact and moves the
  tests of the gates' refusals (they send through the door) to the raw
  road. Not built.
- **A client that is not the door can put its own text in a depot.** The
  gates refuse it and keep no row, but the depot keeps the record, and the
  refused name's answer record keeps the act's parts digest (keyed, over
  the act, control values included). The same holds today for a plaintext
  value fact from such a client (refused `:not-sealed` on its face). It is
  the depot's nature (every append is kept until a trim); the forget
  ruling reaches what the store admitted. Named, not built; it sits beside
  For Sid 50 (a caller identity for the door's road).

**First-record placeholders.** None new. R-2 carries reads-rest's
placeholder (`[:kv k {:keyed hex}]`, label `softland.read-pattern/1`) into
every `:read/pattern` line.

**Overnight state, Next item 2 (step R):** done, on `rig-build-fixes`,
for C to merge.

## Listed, not deleted

Nothing was deleted. Outside the repository: the plan's and tests' drafts
in this session's scratchpad, and the plan validator's working copy of
`promote_test.clj` there.
