# Test validation: phase 8, the replays

Validated by Claude Opus 5.5 at max effort, 26 September 2026, in the build
session on branch `rig-build-replays`, following the rama skill's
`phase-6-test-validate.md`: default FAIL, each check stated, the tests that
answer it cited (`L…` is `test/rig/replay_test.clj` as committed with this
file), a verdict. The tests live in the adapter's own namespace,
`rig.replay-test`, so the one command runs them with the replays.

**What the tests are.** One deftest with a cluster, `replays` (L1888),
plays every practical case and fails on the plan's verdict rules; twenty
deftests with no cluster (L1915-2259) test the adapter's model side and the
pure rules the run judges by. The orchestrator's two asks are
`lockstep-ends-where-run-ends` (L1915: the lockstep's end state gives
`scenarios/play`'s `seen`, for every case under every named configuration
of its set, 44 runs, and is `fm/run`'s end state) and
`known-differences-cite-their-cases` (L2021: KD1 to KD20 once each, each
citing the cases it touches or, for the five no fixed history exercises,
none with its why; the plan's case tables' KDs cite their cases).

**Evidence that they catch mistakes.** `runs/phase8-test-mutations.clj`
redefines one function at a time into a plausible bug and runs the test
meant to catch it; output `runs/phase8-test-mutations.txt`: thirty mutants,
thirty caught, the unmutated baseline with no failures. The first run
missed one (below, TV1).

**Verdict: minor-fail, fixed in place** (TV1 to TV5), with one item carried
to pass 2 (TV6) that needs merged code to test.

## Minimize IPC launches. PASS

One `create-ipc` for the namespace, in `replays` (L1888, through
`run-replays!`). A fresh module per case is the plan's RP2 (a relaunch
measured at about 380 ms by the plan's probe), not a new cluster. The
twenty other deftests need no cluster, so they do not share mutable state
with it and cost no launch; they are separate deftests so each rule's
failure names itself.

## Implicit spec coverage: the plan's requirements and fixes

The requirement is SPEC.md phase 8 and PLAN-replays.md; its F1 to F12 and
this build's IV1 to IV7 are the edge cases. Each, with the tests that
answer it.

| item | tests | note |
|---|---|---|
| the lockstep composes `run` | `lockstep-ends-where-run-ends` | every case, every named configuration |
| effects read off the state | `lockstep-finds-every-effect` (L1927) | every decision once, reads in order, person forgets; B1's three steps, B3's request then read-out by stamp, the landing at the batch |
| F6, one answer per name | `answers-per-name-reduce-to-one` (L1999); D2 and B3 in L1927 | a split is the model's finding and fails |
| F7, prepare and commit | L1927's D1 block | the mutant ignoring the delta is caught |
| the guard | `the-guard-catches-decisions-it-cannot-order` (L1974) | a drain deciding on two partitions; with its stages resolved, not practical fails |
| step rows, and refusing rowless steps | `every-step-of-the-fixed-histories-has-a-row` (L1985) | failover, retry, a past read, an offer sent twice, a promotion into the base, a forget in the group |
| the model's own lines | `the-model-report-is-reproduced` (L2011), `the-report-renders` (L2168) | the named configurations and checks give runs/phase8-model-report.txt's 64 lines exactly |
| KD1 to KD20 as data | `known-differences-cite-their-cases` (L2021) | TV1 |
| the world, KD9, F4, F9 | `the-world-grants-every-model-permission` (L2041) | both roads; TV2, TV3 |
| KD1, F8, F11 | `judging-an-answer` (L2071) | ten cases, each reason and each side |
| KD10, F3 | `judging-an-exit-refusal` (L2089) | both clauses, a lease after the forget, a reader never forgotten |
| KD20, KD12's relation, F2 | `judging-a-fact` (L2103), `control-references-for-every-kind` (L2235), `extra-rows-are-facts-the-model-lacks` (L2228) | a control fact by its references; an erasure dated at its value's stamp |
| the verdict rules, "predicted, not seen" | `the-verdict-rules` (L2127) | worse than predicted fails, better does not; a broken namespace fails |
| F8, the APIs | `the-api-table` (L2154) | TV4 |
| F10, the header | `the-report-renders` (L2168) | TV5 |
| IV1, the holds | `holds-follow-the-model` (L2190) | the first commit's rule is caught as a mutant |
| IV4, the bounds | `a-call-or-a-wait-that-does-not-return-is-cut` (L2247) | a stuck call is cut and marked; a wait times out; an exception stays its own |
| IV5, the group's reader | `the-group-keeps-a-reader` (L2210) | |
| F11, the seed's answers | `seed-answers-are-found-in-every-shape` (L2218) | make-group!'s re-class answer counted |

**TV1 (fixed).** `known-differences-cite-their-cases` read the table's
citations through `kd-by-n`, a copy made at load time; the mutant that
drops A4 from KD10 was missed because the copy did not change. In a real
edit both would reload, but a test must not lean on a copy. It now reads
the table itself, and `kd-by-n`, unused since, is gone. The mutant is
caught.

**TV2 (fixed).** F4's `:bob-hand` was in the world, but no test said it is
Bob's working layer, made as his hand layer and granted to him. Added to
L2041.

**TV3 (fixed).** F9's session road (`cited-pid` with `door-road :session`)
had no test; the direct road is the one run, and a merge that needs the
session road would switch to code never checked. Added to L2041.

**TV4 (fixed).** F8 says the replay never resolves phase 3's placeholder
person acts and never uses `rig.claims/hold!`, which blocks a task; nothing
asserted it. Added to `the-api-table`.

**TV5 (fixed).** IV6's note (the model's objections are its own) was not
checked. `the-report-renders` now checks that under
`baseline-but-not-p6-line-at-the-read-out` exactly B3's and B4's rig lines
carry it.

## The rig side: what the tests reach, and what they cannot yet

The plays (`play-value!` to `play-read!`, `rig-values!`) call the stages'
APIs, and on this branch phases 2, 3 and 4 are not merged, so no test can
run them; `replays` reports every case "not practical" with its missing
names and fails nothing for it, which is the plan's rule. Their judging is
tested here in isolation; their calls and waits are exercised by `replays`
from pass 2 on, where each case's line and its verdict are the tests: a
baseline `seen` that differs, an unexplained difference, or a status worse
than predicted fails `replays` (L1900-1905).

**TV6 (carried to pass 2).** Two rig-side paths will stay unexercised by
the fixed histories even after the merges, unless a run is made for them:
the fallback road (one module, a fresh world per case), run only when the
relaunch fails or `RIG_REPLAY_ROAD=one-module` forces it; and the
`:before-forward` hold (KD5), run only when pausing the micro topology
fails. Their pure parts are tested (renaming on both roads, L2041; the hold
decisions, L2190). A forced fallback run in pass 2 or 3 would exercise the
first, and costs one more run of the cases; it is the orchestrator's call
under tonight's run-once rule. The marks of KD2 to KD20 during a play (IV2,
IV3) are checked only by the first played case's "known" and "predicted,
not seen" lines.

## Synchronization. PASS (traced)

The model-side tests write nothing. In the plays every write is awaited
before the next step: a stream act by its acked append (`offer-until-
answered!`), a micro act by the door's `await-answer` (60 s) and then
`wait-frontier!` past its batch; a person forget by its ack and then
`person-on-task` on every task (64 routing keys); a landing by its answer by
name and the frontier past its batch; a held read-out by the resend's
answer and then the crossing's record. Every read happens after the op's
decisions. Every wait is bounded (IV4).

## Test namespaces compile. PASS

`rig.replay-test` loads with the `:test` classpath and lints with no
diagnostic (clojure-lsp's clj-kondo). It requires only phase 1's
`rig.store.module` and the model; everything later is found at run time.
It reads two private vars of the model's public namespace, `formal.scenarios`
(`find-fact`, `ref-str`), through their var-quotes, so that `seen` and the
ref's printed form are the model's own; a rename in the model would make
them fail loudly, never silently.

## Self-consistency

Read back after writing. TV1 to TV5 each say FAIL and name their fix.
TV6 is not a gap in the tests of what exists; it names two roads the fixed
histories do not take, and says what would exercise them and whose call
that is. The rig side is stated as untested on this branch, as the plan
designs it. Hence minor-fail: every failure was a local edit to an existing
test.

PHASE_VALIDATION:minor-fail
