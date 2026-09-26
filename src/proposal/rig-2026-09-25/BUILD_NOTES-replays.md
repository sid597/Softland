# Build notes: phase 8, the replays (pass 1 of up to 3)

Built by Claude Opus 5.5 at max effort, 26 September 2026 (first commit
05:05 IST, the run 05:29 IST), on branch `rig-build-replays` (made off `rig-2026-09-25` at `9730010c`), per
`PLAN-replays.md` (its validation's fixes F1 to F12 applied) and the rama
skill's build step. Pass 1 ran before wave 1 (phases 2 and 3) and wave 2
(phase 4, the rest of phase 5, phase 6) merged, so it builds the harness,
tests its model side, and runs with every case "not practical".

## What was built

- **`test/rig/replay_test.clj`, namespace `rig.replay-test`**: the adapter
  and its tests, one namespace, so the one command runs both. It requires
  only phase 1's `rig.store.module` and the model; every other rig API is
  found at run time (`requiring-resolve`), so it compiles and runs at every
  step of the build.
  - *The lockstep* (`lockstep`, `op-effects`): the model stepped one op at a
    time with its public functions as `fm/run` composes them; each op's
    sends, decisions (a `[:prepare]`'s from its delta, F7), person forgets
    and reads read off its state; decisions ordered by stamp on one stream
    partition or by the inbox in a batch, and a guard that makes a history
    whose order it cannot tell not practical.
  - *The world as data* (`world`, `rig-pid`, `fallback-names`), *the step
    rows* (`step-rows`, `history-refusal`), *the fourteen cases* with their
    predicted status (`cases`, `predicted`), *KD1 to KD20* as data
    (`known-differences`), *the named configurations* (`named-configs`).
  - *The plays*, one per effect kind, each rig act sent at the model op
    that decides its counterpart; the holds (phase 4's `:before-read-out`
    and a resend) and the pause (Rama's, `:before-forward` as its fallback)
    for continuations the model decides in a later op.
  - *Two observations*: the store's judgment for `:values` (`rig-values!`:
    found from the rig's own records, opened by the store); a reader's view
    at every model read (`read-layer!`: through the exit as a live reader,
    with pattern reads below the exit and the group's acts through the
    frontier for facts the model lacks), KD10's refusal judged with the
    store's view beside it.
  - *Judging* (pure, tested): `judge-answer` (KD1, F8, F11, F6),
    `judge-exit-refusal` (KD10, F3), `judge-fact` (KD20, KD12's relation),
    `judge-case` (the plan's verdict rules).
  - *The report* (`report-lines`, `write-report!` in the deftest's
    `finally`): the header (IST time, rig and model commits, tasks, run
    time, the road, stages resolved, F10's three), every KD, each case set
    under each configuration with `scenarios/report`'s own lines and the
    rig's line beside, the APIs by stage with their bindings.
  - *Bounds*: every poll 60 s at 50 ms; every rig call 120 s; after a call
    that does not return, no later case is played.
  - *Tests*: `replays` (the cluster run) and twenty with no cluster.
- **`IMPLEMENTATION_VALIDATION-replays.md`**: minor-fail, IV1 to IV7 fixed
  in place; divergences D1 to D12; what pass 1 cannot show.
- **`TEST_VALIDATION-replays.md`**: minor-fail, TV1 to TV5 fixed; TV6
  carried (the fallback road and the `:before-forward` hold, which no fixed
  history takes).
- **`runs/phase8-test-mutations.clj`** and its output **`.txt`**: thirty
  mutants against the model-side tests, thirty caught.
- **`runs/phase8-replays.txt`** (the standing report, rewritten by every
  run) and **`runs/phase8-replays-pass1.txt`** (this pass's copy).

## The run (pass 1)

`flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:test
rig.replay-test`, from the rig folder, once, at 05:29 IST on `a5021c6a`:
21 tests, 752 assertions, 0 failures, 0 errors, 12 s wall. The road check:
launch 1,402 ms, destroy 246 ms, relaunch 799 ms, so a fresh module per
case. Stages: 1 (11 APIs) and 5a (2) resolved; 2 (0 of 9), 3 (0 of 12), 4
(0 of 5) and 5's rest (0 of 1) missing. All fourteen cases "not practical",
each naming its missing stage 2 and stage 3 names (and stage 4's for B1 to
B4); none fails. The model's own lines for every case under every
configuration are in the report, each with the rig's line beside.

## The bindings

**Built, bound as the build names them** (the plan's name where it differs):

| API | bound to | plan's name |
|---|---|---|
| phase 1 | `rig.store.client/build`, `offer-until-answered!`, `record`, `settings`, `grant-offer`, `make-layer-offer`, `revoke-offer`; `rig.store.envelope/make-name`, `crossing-name`, `landing-name`; `rig.store.inject/reset-all!` | the same (the plan lists `crossing-name` and `landing-name` under stage 4) |
| phase 2 (`rig-build-locks` `fd41f6d2`) | `rig.store.client/people-layer`, `make-person! [store p]`, `forget-person! [store p]`, `forget-value! [store who layer fid]`, `opens? [store layer fid]`, `person-on-task [store p pkey]`, `depot-records [store]`; `rig.store.grammar/grammars` (its `:mention` entry checked) | `stock! [store who layer session n permission]` for "`offer!` sealing at the door, leasing when short; `lease!`": the door's lease ahead, so the lease act's answer is seen (F11); "the fan-out waited for" is the ack, then `person-on-task` on 64 routing keys |
| phase 3 (`rig-build-micro` `e60c8ee1`) | `rig.store.micro-client/connect`, `make-base! [store spec]`, `make-group! [store L spec]`, `open-session! [store S p layers]`, `settings-of`, `offer! [store offer]`, `grant-offer [pid]`, `revoke-offer [store pid]`; the topology `"micro"` | `frontier [store]` for `micro-frontier`; `write! [store spec]` for the micro door with its lease; `lookup [store nm digest fp]` for `micro-lookup`; `open-act [store e nm]` for `micro-act` at F with the pure open |
| phase 5a (this branch) | `rig.store.read-exit/connect`, `read!`; the `read-point` and `read-pattern` queries through the exit's handle | the same |

**To confirm** (the plan's names, shapes assumed; each isolated in one
function so pass 3 changes one place):

1. Phase 4: `rig.store.client/lease-landing! [store {:who :layer :session
   :permission :request}]` (`call-lease-landing!`); `rig.store.client/promote!
   [store {:name :who :layer :session :permission :source :target
   :landing-permission :claimed-when}]`, its answer the request's or
   `{:request a}` (`call-promote!`), and a resend with the same name and
   claimed-when answered from the record; `rig.store.client/promotion-status
   [store layer req nil]`, a map with `:status` or the keyword
   (`call-promotion-status`); `rig.store.inject/hold! [point req]` and
   `release! [point req]` at `:before-read-out` and `:before-forward`;
   `env/landing-name` with the class as a third argument; the crossing's
   answer by `c/record` of `env/crossing-name`, the landing's by the micro
   `lookup` of the landing name.
2. Phase 5's rest: `rig.store.shared-reads/moment` as the sign that
   `read-exit/read!` reads a group layer; without it the group is read
   through phase 3's frontier and the line says so.
3. The merge of wave 1: the merged handle is `(merge (read-exit/connect ipc)
   (micro-client/connect ipc))`; `opens?` kept (builder A asked the merge to
   make `read-as-of` internal; without `opens?` the stream-side cases say so
   and it is rebound); the stream gate taking 4-element pids, so
   `open-session!`'s grants in `:alice` are admitted; `:members` among phase
   2's control keys (phase 3's note), or the group's making act is refused;
   the micro door's answer after a refused lease (KD1 expects `:no-such-lock`
   on the value act's face; phase 3's `write!` seals under no lock then).

## How each later pass continues

Every pass: from the rig folder, one run, under the lock (the lock is
shared with the waves' suites, so one run, not a loop):

```
flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:test rig.replay-test
cp runs/phase8-replays.txt runs/phase8-replays-passN.txt
```

After a fix, rerun only this namespace; `RIG_REPLAY_ROAD=one-module` forces
the fallback road for one run (TV6).

**Pass 2, after wave 1** (phases 2 and 3 merged on `rig-2026-09-25`):
`git merge rig-2026-09-25` into `rig-build-replays` (the replay files are
new, so no conflict is expected), then the run. Expect A1 to A8, D1 and D2
played, B1 to B4 still "not practical (stage 4 missing: …)". Check, in the
report:

1. The header: stages 1, 2, 3 and 5a resolved; the road still a fresh module
   (the merged module's relaunch well under 15 s).
2. Each played case's lead: A1 to A8 and D2 "as said"; D1 "approximated
   (prepare and commit are one batch …); as said". A worse status fails the
   test.
3. The `known` lines: KD1 in D1 and D2 (the lease refused with the model's
   reason, the value act `:no-such-lock` on its face); KD10 in A1 (Alice's
   exit read of `:alice` refused, the store's view beside it). A3, A4 and A6
   show KD10 only once the shared read exists; before, their group is read
   through the frontier, with no reader, and KD10 prints "predicted, not
   seen", as it should.
4. Every `DIFFERS` line. The first played run is the first evidence for
   every rig-side path, so a difference is either a finding for RIG.md
   ("Found tonight") or a bug in the adapter; say which, with the trace. A
   binding a build renamed shows as "not practical (… missing)"; rebind it
   in `apis`.

**Pass 3, after wave 2** (phase 4, phase 5's rest, phase 6 merged): merge
again and run. Expect B1 to B4 played, each "approximated (…); as said",
and the group read through the exit if `rig.store.shared-reads` resolved.
First confirm phase 4's shapes against its build (the list above) and adapt
the four call functions; then check `:shown` against the model's
(`[:pending :pending :refused]`, `[:pending :refused]`,
`[:crossed :crossed :done :done]`, `[:crossed :done :done]`), KD3, KD4 and
KD13 seen, and that B4's landing is not refused (F5: a refusal fails the
test and its line names which of phase 4's F9 conditions it points to).

## Divergences from the plan (IMPLEMENTATION_VALIDATION D1 to D12)

The stream door's lease is `stock!` (D1); one set of sessions for every
case, the union of the layers the fixed histories write (D2); Alice's root
permissions are grants after the making act, as phase 2's seed does (D3);
rig calls bounded at 120 s, polls at 60 s (D4); the store's view beside a
refused exit read is compared, not only printed (D5); reads cover the
layers the rig wrote into (D6); the road check runs once, up front (D7);
KD5 not predicted (D8); a group read with no live member reads as Alice
(D9); a case's error reported with its class and first frame (D10);
`opens?` bound as phase 2 built it (D11); a value forget in a shared layer
has no row (D12).

## For RIG.md

Rig choices, each changeable without touching a record (the orchestrator
assigns R numbers):

- **P8-1.** The replays play in lockstep with the model: the model stepped
  op by op with its own public functions, each rig act sent at the op that
  decides its counterpart (RP1).
- **P8-2.** A fresh module per case on one in-process cluster, 4 tasks, 2
  threads, chosen by a road check made once per run (launch, destroy,
  launch); the fallback (one module, a fresh world per case) when the
  second launch fails or passes 15 s, or when `RIG_REPLAY_ROAD=one-module`
  forces it (RP2).
- **P8-3.** `:values` from the store's own opening, found from the rig's own
  records; reads through the exit as a live reader; the store's view beside
  a refused exit read, compared too (RP3).
- **P8-4.** A continuation is held only when the model decides it in a
  later op: a read-out by phase 4's `:before-read-out` hold and the door's
  resend, a landing by pausing the micro topology, `:before-forward` as the
  fallback (RP4).
- **P8-5.** D1's prepare and commit play as one batch, approximated (RP5).
- **P8-6.** Writers cite their own permission, the model's pid mapped (the
  direct road); sessions `:alice-session` (in `:alice` and `:group`) and
  `:bob-session` (in `:group`) are opened in every case's seed; Bob reads
  from a rig-only `:bob-hand` (RP6, F4).
- **P8-7.** Known differences are data; the test fails on a baseline
  `:values` or `:shown` difference, on any difference none of them
  explains, on a status worse than predicted, and on a namespace that is
  there and does not load (RP7).
- **P8-8.** The report `runs/phase8-replays.txt` is rewritten by every run
  (RP8); `RIG_REPLAY_REPORT` names another path.
- **P8-9.** Before each stream-side value act the door leases ahead
  (`stock!`), so every lease's answer is seen and judged (F11).
- **P8-10.** Every rig call is bounded at 120 s and every poll at 60 s; after
  a call that does not return no later case is played, since a stale call
  could reach the next module of the same name.

Questions for Sid: none new from pass 1. The run carries For Sid 14 (KD1),
16 (KD2 to KD5) and 28 (KD10), and 15 as KD19, which no fixed history
exercises; from pass 2 on it shows each that a case reaches, case by case,
every run.

First-record placeholders: none. The replays write no record form of their
own; every act they send is a stage's own form, listed in that stage's
notes. The names they choose (`:alice-session`, `:bob-session`,
`:bob-hand`, the fallback's suffixed names) are test data in records
nobody keeps.

## For Sid to look at, not to delete

Nothing of this pass should go. `runs/phase8-replays.txt` and
`runs/phase8-replays-pass1.txt` are the same bytes tonight; the first is
rewritten by every run, the second keeps pass 1's.
