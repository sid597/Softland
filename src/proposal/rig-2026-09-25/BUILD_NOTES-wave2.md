# Build notes — wave 2's merge: phases 4, 5's rest and 6 in one tree

Builder: Claude Opus 5.5 (`claude-opus-5-5`), effort max, the wave 2 merge
builder under builder C, 26 September 2026 from 07:14 IST. Branch
`rig-wave2`, worktree `/mnt/data/projects/Softland-rig-wave2`, made off the
rig's head `c9684356` (wave 1, its review with F-1 fixed, phase 8's pass 2,
phase 7's validated plan). The rama skill loaded before any Rama code was
read.

Tags on load-bearing claims: *checked* (read in the named source, or run),
*derived* (reasoned from checked facts), *assumed*.

## Receipt

**Green: one full suite at the end, 24 namespaces, 148 tests, 7,699
assertions, 0 failures, 0 errors** (*checked*: `runs/wave2-suite.txt`; the
full output `runs/wave2-suite.log`, git-ignored). Run once, at `0edf1dac`
(merge 3 of 3), 07:33:34 to 07:46:25 IST (12 min 51 s, JVM start
included), under the cluster lock, taken with no wait. The namespaces:
wave 1's 14, the review's (`rig.store.review-wave1-test`), the replays
(`rig.replay-test`), phase 4's two (`rig.store.promote-unit-test`,
`rig.store.promote-test`), phase 5's rest's two
(`rig.store.shared-reads-test`, `rig.store.reads-rest-test`) and phase 6's
four (`rig.store.shape-test`, `rig.store.grammar-test`,
`rig.store.recipe-test`, `rig.store.tools-test`). No failure, so no fix and
no rerun. `promote-test` drew 8 tasks.

The command is `runs/wave2-suite-cmd.sh`, committed beside the record. The
replays ran with `RIG_REPLAY_REPORT=runs/wave2-replays.txt`, so phase 8's
committed report (`runs/phase8-replays.txt`) stays as pass 2 left it. Their
report's first lines say "with uncommitted changes in the rig folder": the
script and this file's draft were untracked during the run; neither is
loaded by it, and the code ran as committed at `0edf1dac`.
Their result on this tree: 14 cases, 10 played, all 10 as the model said
(A1 to A8, D1, D2), 4 not practical (B1 to B4: the replay binds phase 4's
door under the plan's names, `rig.store.client/lease-landing!`, `promote!`,
`promotion-status`, which phase 4 built in `rig.store.promote-client`), 0
failing. Every case that reads the group read it through the exit, since
`rig.store.shared-reads/moment`, the replay's optional binding for phase
5's rest, now resolves: the road pass 3 expected.

Every namespace any branch or the rig head ran in its own full suite is in
this one (the branches' suites: 102, 103 and 106 tests; the review's on the
rig head, 94).

**The merges** (*checked*: `git log --first-parent c9684356..rig-wave2`):

| Commit | Merges | Conflicts | Resolutions |
|---|---|---|---|
| `0feb72ef` | `rig-build-promotion` at `5303418f` (phase 4) | none | none needed: the head's changes since `2034cce5` (the review's micro-door fix in `micro_client.clj`, the review and replay tests) touch no file of promotion's |
| `df980ac7` | `rig-build-tools` at `2d2f666a` (phase 6, stream side) | `gate.clj` (4 hunks), `locks.clj` (1), `module.clj` (1) | W2-1, W2-2 |
| `0edf1dac` | `rig-build-reads-rest` at `33817364` (the rest of phase 5) | `gate.clj` (1), `micro.clj` (1, the requires), `module.clj` (2) | W2-3, W2-2 |

Order: promotion first, since it holds `1b1d416c` (the gate's
record-or-decide path moved into `rig.store.gate-event`); then tools, which
merged the same `1b1d416c`, so git took `gate_event.clj` from their common
base and auto-merged both branches' edits to it (promotion's callers,
phase 6's key-row loop and write block); then the rest of phase 5, whose
`module.clj` edits sit outside the moved path, as its notes said.

After merges 2 and 3, every source and test namespace was loaded with no
cluster (`runs/wave2-load-merge2.log`, `runs/wave2-load-merge3.log`,
git-ignored): 9 s and 71 s, each ending `:loaded`. A load compiles every
`deframaop` and `deframafn`; the topologies are built at launch, which only
the suite does.

## Each branch's "Shared-file changes", as followed

**Phase 4, promotion.** Every listed change came in whole. `module.clj`'s
`<<sources` body is the call to `gate-event/record-or-decide>`; the
continuation and the status query's declaration are in place;
`micro_prepare_test.clj`'s landing-parse assertion is promotion's.

**Phase 6, tools and grammars.** The listed edits to `envelope.clj`,
`client.clj`, `grammar.clj`, `reads.clj`, `gate_event.clj` and the six
earlier suites auto-merged. The conflicts were each branch adding to the
same form:
- `gate.clj` `control-fact?`: `:grammar` beside promotion's
  `:promote-request` and `:crossed`; `control-allowed?`'s docstring both
  stages' lines; `refusal`'s `:malformed-control` clause keeps promotion's
  one-fact rule for a request beside phase 6's "one grammar per key per
  act", inside phase 6's `refusal-with-rows` (so the micro fold's 4-arity
  has both); `intake` calls promotion's `intake-offer`, which carries phase
  6's `:keys` (W2-1).
- `locks.clj` `decision-reads>`: promotion's `delivered-context` (the
  landing's `:landing-lock-gone`) given phase 6's grammars (W2-1).
- `module.clj` `layers-schema`: both merge arguments,
  `(promote/layer-fields)` then `(grammar/layer-fields)`.

**Phase 5's rest.** `reads.clj`, `read_exit.clj` and the two tests phase 6
also edited (`read_exit_test.clj`, `reads_test.clj`) auto-merged; the
merged `read-point` and `read-pattern` were read whole (*checked*): the
shared-layer `invoke-query` first, then visibility, the `:moment-kind`
refusal, phase 6's `kv-refusal`, and the opaque marks on the one-owner
path. `seed-hints`' `:no-copy` carries the five read keys, which phase 6's
`hints-of` adds to every layer's hints, and `reads/read-keys` holds them,
which phase 6's `gate/store-key?` reads: both mechanical, as phase 5's notes
asked. The conflicts: `module.clj`'s requires and query declarations (both
kept), `gate.clj`'s `control-value-ok?` (promotion's two clauses, phase 5's
`:session-closed` with `:reads`, phase 6's `:grammar`), `micro.clj`'s
requires (both). The micro side's blocks, where promotion and phase 5 both
edited `micro.clj` (block 2b's `row-wraps`, whose delivered locks include a
landing's box since phase 4, then phase 5's keyed digests from its
`:plain`; the fold's `:purge` writes and block 2a's `:purge` case;
`declare!`), auto-merged and were read (*checked*): each uses the other's
output as its own branch built it.

## Non-mechanical resolutions

**W2-1. A landing into a layer the stream gate orders is decided under that
layer's grammar facts.** Promotion decides a stream landing by the gate's
one path, `gate-event/record-or-decide>`, from `gate/intake-offer`
(`promote/gate-intake`), never from `gate/intake`; phase 6 put the key rows'
names (`:keys`) in `intake` alone and gave the value checks the rows'
grammars through `value-context`. Merged as each branch had it, a landing
into the base while one-owner would have read no key rows and been checked,
wrapped and indexed as if its key had no grammar: a promoted `:mention`
landing in the base under no subjects, the silent divergence promotion's
prep (its option (b)) named. So `intake-offer` carries `:keys`, and
`locks/delivered-context` takes the grammars (its 3-arity keeps the
constant, as `value-context`'s does). Closer to the model and the plans: the
plan decides a landing "through the gate's ordinary decision path"; PR3 and
T13 say the copy is about whom "the target's grammar" names; phase 6's
T-FR3 says a value's grammar is the latest grammar fact for its key in its
layer. Run: promotion's stream cases (T10 a, T8 in the base, T6 on the
stream gate, D6), in this suite with the toy grammars in the base (W2-2),
so each landing's `:note` copy was decided under the base's rows. No case
asserts what only the rows give a landing (a `:mention` copy's subjects in
the base, a value index entry), so that part is *derived*, not run.

**W2-2. The suites these branches never saw get the toy grammars as
facts.** Since phase 6 the stream gate and the one-owner exit read a key's
grammar only from facts in the layer; four suites were written against the
compiled constants (`grammar/grammars`' `:mention`, `seed-hints`' `:note` by
value) and never ran with phase 6: the review's
(`review_wave1_test.clj`), promotion's cluster suite (`promote_test.clj`),
phase 5's (`reads_rest_test.clj`) and the replays (`replay_test.clj`). Each
now writes phase 6's test data (`rig.store.toy-grammars`, D-P4) into its
one-owner layers before their first `:note` or `:mention`, as phase 6 did
for the six earlier suites:
- the review: the seeded layers and the base after the seed (its R-2 block
  reads `[:kv :note ..]` in the base, refused `:not-indexed` without it),
  and `:rv-pl` when made;
- promotion: the seeded layers and the base after the seed, and each
  promoter's layer in `make-promoter!` (T13's `:mention` in Abe's layer
  names Bea again, as under the constant);
- phase 5: `:alice`, `:alice-hand`, `:alice-agent`, `:bob` and the base after
  the seed (RT3's R-2 block, RT6 and RT7 read `[:kv :note ..]` in one-owner
  layers);
- the replays: a seed step per one-owner layer, "grammars in L", through
  the bound `:client/build`, with the facts found at run time like every
  other API (`:grammar/facts`, stage "6", optional, so a tree without phase
  6 seeds as pass 2 did), and `:grammar` joins `rig-only-keys`, so a
  pattern read that finds the seed's grammar facts does not count them as
  facts the model lacks (phase 6 did the same for `read_model_test`'s
  `door-keys`).
The toy `:mention` grammar checks a shape the constant never checked (a map
whose `:persons` holds one or two keywords); every `:mention` value these
suites write fits it (*checked*: promotion's one, T13's `{:persons
#{:bea}}`; the review's and phase 5's suites write none; the model's are
`{:token t :persons ps}` with one or two people, model.clj 830, and the
replays played every A case as the model said).

**W2-3. Phase 5's stream side takes phase 6's grammar facts; its micro side
keeps the constant until step 6b.** Phase 5's rest built its reads against
`reads/current-hints` (the constant) everywhere. On the micro side
(`kv-digests`, `index-block>`, `micro-ops>`, `shared-pattern>`'s parse, the
shared branch of `delta>`) that stays, by the brief. Where its reads meet
the stream store they now read the layer's key rows in `$$layers`, as the
read exit's own queries do since phase 6 (`rig.store.shared-reads`, a new
section "the stream side's grammar"):
- *A one-owner layer's delta* (`delta>`, RS3) parses with `parse-hints` and,
  after the visibility and moment checks, answers `kv-refusal` over its
  key's row, as `read-pattern` does; its rows get the opaque marks
  (`stream-marks>`). Before this, a standing `[:kv k v]` read of a key the
  layer's grammar indexes but the constant does not (any key but `:note`)
  opened through `read-pattern` and then had every delta refused by the
  constant's parse (*derived*: `delta>` parsed with `current-hints`); and a
  standing `[:kv :note v]` read opened while the layer's grammar indexed
  `:note`, whose grammar then changed before `:note`'s first use (allowed:
  the key unused), would have read an index with nothing in it and
  delivered nothing, where now it is refused `:not-indexed`.
- *The opening's cursor* (`opening-answer`) parses the same way (the
  constant for a shared moment).
- *A re-classed layer's stream era* in `shared-pattern>`: a `[:kv k v]`
  read is refused `:not-indexed` (or `:opaque`) when the stream era holds a
  fact under k (its row's `:used`) and its grammar there gives k no value
  index, since that era's value index cannot answer and the micro era's
  answer alone would be silently incomplete; a key the stream era never
  used leaves the read to the micro era. Its rows, in pattern and point
  reads (`shared-point>`), get their opaque marks from `$$layers`' rows;
  the micro era's rows are left to its own grammar (6b).
Every new read is one seek per distinct key on the layer's task, in a loop
that yields (`stream-marks>`); the refusal is data. This is the one
resolution where the two branches' behaviours met rather than conflicted:
phase 5 never saw a grammar fact and phase 6 never saw a delta. What was
run: phase 5's cluster suite (RT3's standing `[:kv :note ..]` read, RT9's
deltas, RT11's re-classed base) and the read exit's D7 block, in this
suite. No case writes a grammar that changes a key's hints or marks a key
opaque before a delta or a re-classed read, so the new refusals and marks
are *derived*, not run.

No behaviour of one branch conflicted with another's: every resolution
above keeps both branches' tested behaviour, and the suite ran every one of
their tests green on the merged tree. So there was nothing to choose
between, and nothing was chosen silently.

## Listed, not deleted

- `reads/placed-by-layer?` has no caller in `src/` since phase 5's rest
  removed the `:re-classed` refusal; only `reads_test.clj` (220-221) calls
  it, and its docstring still says "refused `:re-classed` meanwhile".
- `locks/delivered-context`'s 3-arity (W2-1) has no caller; it mirrors
  `value-context`'s 3-arity, which phase 2's pure tests call.
- A load warning, not a failure: `rig.store.promote-flow` defines
  `continue>`, which shadows Rama's `continue>` in that namespace. Nothing
  there uses Rama's (no `loop<-`), so it is harmless today; a `loop<-`
  added to that namespace would call the wrong op.

## For RIG.md

RIG.md was not edited here. What the orchestrator folds in:

**The state** ("Overnight state", "Stages tonight").
- Wave 2 merged on `rig-wave2` (worktree
  `/mnt/data/projects/Softland-rig-wave2`) off the rig head `c9684356`:
  `0feb72ef` (phase 4), `df980ac7` (phase 6, stream side), `0edf1dac`
  (phase 5's rest), then these notes. `rig-wave2` descends from
  `c9684356`, so bringing it into `rig-2026-09-25` is a fast-forward while
  that branch has not moved.
- One full suite at the end: 24 namespaces, 148 tests, 7,699 assertions, 0
  failures, 0 errors (`runs/wave2-suite.txt`). No fix, no rerun.
- The replays on this tree: A1 to A8, D1 and D2 as the model says, the
  group read through the exit; B1 to B4 not practical until pass 3 rebinds
  phase 4's names (`runs/wave2-replays.txt`).

**Rig choices to number** (RIG.md's list ends at R58): P4-1 to P4-14
(`BUILD_NOTES-promotion.md`), P5-1 to P5-15 (`BUILD_NOTES-reads-rest.md`),
P6-1 to P6-7 (`BUILD_NOTES-tools-and-grammars.md`), as their notes give
them, and three from the merge, each changeable without touching a record:
- **W2-1.** A landing into a layer the stream gate orders is decided under
  that layer's grammar facts, as any act there (`intake-offer` carries the
  key rows' names; `delivered-context` takes the rows' grammars).
- **W2-2.** Every suite written against the compiled constants writes the
  toy grammars as facts first (D-P4 extended to the review's, promotion's,
  phase 5's suites and the replays' seed; `:grammar` a rig-only key in the
  replays).
- **W2-3.** Where phase 5's reads meet the stream store they take the
  layer's grammar facts (a one-owner delta and its opening cursor; a
  re-classed layer's stream era, refused for a `[:kv]` key it used but
  never indexed, and its rows marked opaque); the micro side keeps the
  constant until 6b. One reading 6b's design may revisit: a re-classed
  layer's `[:kv]` read is refused when its stream era cannot answer, where
  it could instead answer from the micro era marked `:partial`.

**Questions for Sid.** The branches' lists stand unchanged: promotion's
five, phase 5's eight, phase 6's Q1 to Q10 and the leaves' language. The
merge adds none.

**First-record placeholders.** None new. The branches' stand: PR1 to PR5
(phase 4), FRR1 to FRR10 (phase 5), T-FR1 to T-FR7 (phase 6). W2-1 applies
T-FR3 and PR3 together: a stream landing's wrap and index entries follow
the target's grammar facts, as the plan's "the target's grammar" names the
copy's subjects. W2-3's refusals and marks are answers and never recorded:
a refused read records nothing, and an entry records no mark
(`reads/entry-facts`: a point line's `:shown` is value, erased, absent or
unreadable; a pattern line's fingerprint is over `[fid stamp]` pairs).

**The machinery count.** Phase 6's stream side (26: 10 + 6 + 10) was
counted before the merge. W2-1 and W2-3 are three more existing units
whose behaviour changed so that phase 6's facts reach phases 4 and 5
(`gate/intake-offer`, `locks/delivered-context`, `rig.store.shared-reads`'
stream side), of phase 6's own kinds a3, a6, a8 and a9: for the
orchestrator to place in the count beside 6b's micro side.

**What the next steps inherit.**
- *Step R.* R-1: phase 4 already refuses a `:crossed` fact from anyone but
  the store's read-out and checks a `:promote-request` whole, so this
  suite's R-1 line reads "admitted at the stream gate as plaintext control
  facts: `(:class :kind :lock-grain :owner)`" (on another entity), no longer
  the request and the crossing; the micro gate still admits `:owner` on
  another entity as plaintext. Phase 6's `:grammar` is a control key the
  gate acts on (its rows), so it is not an R-1 case; until 6b the micro
  gate seals it as a value and refuses it `:malformed-control` (phase 6's
  `grammar-test`, the 4-arity case). R-2: phase 5's seam
  `reads/recorded-pattern` is in, used by a standing read's opening line;
  `entry-facts`, which builds both the one-owner and the shared entries,
  still records a plain `[:kv]` pattern (this suite's R-2 line: the entry
  pairs the forgotten text with its fact id). F-1's last edge
  (`micro_client/write!`, a lease answered yes whose `take-locks` times
  out) is unchanged. The pending blocks run as before
  (`RIG_PENDING=1 clojure -M:test rig.store.review-wave1-test`); their
  layers now hold the toy grammars (W2-2).
- *Step 6b.* The micro side takes the constant at: `micro/control-keys` (no
  `:grammar`); `locks/read-values`' 3-arity in `micro/arrival-open` (block
  1) and `micro/row-wraps` (block 2b), the two opens that also name a
  micro landing's subjects; `shared-reads/kv-digests` (block 2b);
  `index-block>` (block 2d); `micro-ops>` (the rebuild pages);
  `shared-pattern>`'s parse, so a
  re-classed layer's `[:kv]` read of a key its stream era indexed but the
  constant does not is refused `:not-indexed`; the shared branch of
  `delta>` and of `opening-answer` (`stream-hints`). W2-3's
  `stream-era-kv-refusal` and `stream-marks>` read `$$layers`' rows for a
  re-classed layer's stream era: the "`$$layers`' on a miss" half of 6b's
  rule for re-classed layers. `grammar/grammars` keeps `:mention` for the
  micro side, and `rig.bench.lock-slice` its own copy.
- *Phase 8, pass 3.* Rebind phase 4's three names to
  `rig.store.promote-client` (`lease-landing!`, `promote!`,
  `promotion-status`; `status-of` gives the bare keyword) and check B1 to
  B4's shapes against `BUILD_NOTES-promotion.md`, "Phase 8's names". The
  group read through the exit already runs on this tree. The seed writes
  the model's grammars into each one-owner layer (W2-2, a stage "6"
  binding, optional). A default run rewrites `runs/phase8-replays.txt`;
  this suite's report is `runs/wave2-replays.txt`.
- *Phase 7.* B2's and B9's planned fields are built here: `:ix-s` (a third
  id entry per fact, in every layer) and `:key-rows` (a read per distinct
  key of an act in the decision's event, a write on a grammar or a key's
  first use); and a stream layer indexes `:note` by value only when its
  grammar says so (`seed-hints`' `:by-value` no longer reaches the stream
  store). The decision's reads and writes now live in `gate_event.clj`
  (`record-or-decide>`, `write-decided>`), where the binding points are to
  be confirmed.
