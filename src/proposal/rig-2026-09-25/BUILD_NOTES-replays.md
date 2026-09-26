# Build notes: phase 8, the replays (passes 1 to 3)

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

## Pass 2, after wave 1

Claude Opus 5.5 at max effort, 26 September 2026, from 05:33 IST, in
`/mnt/data/projects/Softland-rig-build-replays`. `git merge rig-2026-09-25`
at `2034cce5` (wave 1: phases 2 and 3 merged and wired to the read exit)
made `2e3e9abd`, with no conflict.

### The bindings pass 1 left for wave 1

Each was read against the merged code before the run, then seen at work
(or not) in it. Wave 1 kept every name and arity the replay binds, so the
table changed only in its provenance comments (`9a22407e`).

- **The merged handle** `(merge (read-exit/connect ipc)
  (micro-client/connect ipc))`: confirmed. Both wrap `c/connect`; the micro
  client's keys win, so the handle holds one door atom and one lease pool,
  and the exit's `:read-point`, `:read-pattern` and `:index-ops` stay.
  Wave 1's own test merges them the same way (`wave1_test.clj` L158).
- **`opens? [store layer fid]`**: confirmed, kept by wave 1 (W1-9,
  `read-as-of` internal by contract; `client.clj` L396-410). It gave A1's
  and A8's `:values`.
- **The stream gate and 4-element permission ids**: confirmed. The gate
  walks `permit/chain` (`permit.clj` L29-38) and `env/pid?` takes ids four
  deep (`envelope.clj` L296-305); `open-session!`'s grant
  `[:alice-session :alice :alice [:alice :alice :alice]]` answered yes in
  all ten seeds.
- **`:members` as a control key**: confirmed. It is one (`envelope.clj`
  L43-52) and the stream gate refuses it (W1-7), but the group's making act
  goes to the micro gate (`make-group!` builds it by entity), which admitted
  it in all ten seeds.
- **Phase 3's door after a refused lease (KD1)**: not confirmed, and not a
  binding to change: `write!` seals under no lock and the micro gate
  answers `:not-sealed`. Finding 1 below.
- **Phase 2's person functions**: confirmed. `people-layer` is `:people`
  (`client.clj` L346); `make-person! [store p]` L348, `forget-person!
  [store p]` L355, `person-on-task [store p pkey]` L483. A forgotten
  entry is `{:lock nil :erased-at stamp}` (`locks.clj` L806), which the
  fan-out wait polls for; every forget's fan-out was seen on all 64 keys
  within the wait.
- **Phase 3's micro-client seed**: confirmed. `make-base! [store {:root
  :grants}]`, `make-group! [store L {:members :root :grants}]`,
  `open-session! [store S p layers]` (`micro_client.clj` L438-516), their
  answers in the shapes `answers-in` reads; the world's base root
  `[:operator :base :base]` is the micro seed's own (`locks/root-actor` is
  `:operator`). All 19 seed acts answered yes in every case.
- **`write!`, `open-act`, `stock!`**: confirmed. `write! [store spec &
  {:keys [lease-who lease-permission grain]}]` gives `{:answer :offer
  :lease :locks}`, the answer carrying the `:batch` the frontier wait
  needs (`micro_client.clj` L298-318; `micro.clj` L1176-1188); `open-act
  [store e nm]` reads micro-act's `{:record :rows}` through the name's F
  (L331-341); `stock! [store who layer session n permission]`
  (`client.clj` L322-332).

### The road, changed

Try 1 (05:42 IST, on `9a22407e`) ran the main road as pass 1 built it: a
fresh module per case on one in-process cluster. It was the first run
with cases played, so the first time the road launched and destroyed the
merged module more than twice. What it showed:

- every destroyed instance's client channels timed out about 25 s after
  its destroy: 22 "Unexpected channel pong timeout; closing channel" and 33
  "ModuleAssignmentInfoNotFoundException from resolve-leader; clearing
  caches" lines, the road check's two instances among them, though no
  client ever connected to those;
- each case ran longer than the one before: A1 5.1 s, A2 7.4, A3 8.3, A4
  10.6, A5 14.9, A6 22.6, A7 33.4, then about 42 s for A8, D1 and D2;
- A8's and D2's seeds stopped at their fifteenth act, the grant `[:bob
  :bob-hand :bob-hand]`, on a 10 s Rama read timeout at task 3
  (`c/grant-offer` reads the layer's settings, with no retry), where the
  same act answered yes in the other eight cases.

The machine was idle (load average 0.77 over the five minutes to 05:48,
0.95 over fifteen, on 24 CPUs), and the store starts no threads of its own
(no `future`, executor or timer under `src/rig`). Checked: the case times,
the log lines, the load, the store's source. Derived, not proven: that the
stale instances' channels, closing while a case runs, stall that case's
reads. The rerun is consistent with it and does not isolate it further.

The fix is the replay's (`ceacabdc`): on the main road each case gets an
in-process cluster of its own, its module launched in it, destroyed, and
the cluster closed after; the road check runs in a cluster closed before
any case; the fallback road keeps one shared cluster and module. In try 2
no pong timeout or resolve error appeared, and every case ran in 5.1 to
7.3 s. Separately, the report's roads line said "the group read through
the exit" whenever no read went through the frontier, which A1 (it reads
no group) and the cases that stopped early printed, though the shared
read does not exist yet; it now names a road only for a read of a group
layer, else "no group read" (`c83e939a`).

### The runs

This namespace, each run once, under the lock, from the rig folder:

- Try 1: 05:42:13 to 05:46:19 IST, 246 s wall (the report's 236 s): 21
  tests, 752 assertions, 3 failures (A8, D1, D2), 0 errors. Its report is
  kept as `runs/phase8-replays-pass2-try1.txt` (`e0371b53`).
- Try 2, after the road fix: 05:52:38 to 05:53:59 IST, 81 s wall (the
  report's 72 s): 21 tests, 752 assertions, 2 failures (D1, D2), 0 errors.
  Road check: launch 2,965 ms, destroy 261 ms, relaunch 2,284 ms (pass 1:
  1,402, 246, 799; the merged module is larger). Stages 1 (11 APIs), 2 (9),
  3 (12) and 5a (2) resolved; 4 (0 of 5) and 5's rest (0 of 1) missing.
  Its report is `runs/phase8-replays-pass2.txt`. Raw logs, git-ignored:
  `runs/phase8-replays-pass2.log` (try 1), `-pass2-rerun.log` (try 2).

### The cases (try 2)

| case | state | known differences |
|---|---|---|
| A1 | as said | KD2 KD6 KD7 KD8 KD9 KD10 KD11 KD12. KD10 attributed: Alice's exit read of `:alice` after her forget, the entry's lease refused `:person-forgotten` and the entry `:no-such-lock` on its face; the store's view beside it shows both facts erased |
| A2 to A7 | as said | KD2 KD7 KD8 KD9 KD11 KD12. KD6 predicted, not seen: their reads are group reads through phase 3's frontier, which record no entry. KD10 predicted, not seen in A3, A4, A6, for the same reason, as pass 1's notes foresaw |
| A8 | as said | KD2 KD6 KD7 KD8 KD9 KD11 KD12 (7b: the mention of Bob in Alice's own layer open after Bob's forget) |
| B1 to B4 | not practical | stage 4 missing: `client/lease-landing!`, `promote!`, `promotion-status`, and for B1 and B2 `inject/hold!`, `release!` |
| D1 | approximated (prepare and commit are one batch); fails on finding 1 | KD2 KD6 KD7 KD8 KD9 KD11 KD12 KD20; KD1 predicted, not seen (finding 1) |
| D2 | practical; fails on finding 1 | KD2 KD7 KD8 KD9 KD11 KD12 KD20; KD1 and KD6 predicted, not seen |

Every played case's `:values` equals the model's: A1's note and mention
erased; A2 erased; A3, A4, A5 open; A6, A7 erased; A8 open; D1 and D2
missing.

### Every DIFFERS line, with its cause

The rig's, under baseline:

1. D1: `answer o0: its lease was refused: no permission-from-another-layer;
   model no permission-from-another-layer, rig no not-sealed`. Finding 1.
2. D2: `answer o1: its lease was refused: no permission-revoked; model no
   permission-revoked, rig no not-sealed`. Finding 1.

The model's own lines under the configurations other than baseline
(`scenarios/report`'s, the same as pass 1's), now each with the rig's
played outcome beside it: A1 under `baseline-but-not-owner-required`
(the mention open there, erased in the rig); A8 under
`baseline-with-a-read-as-owner-and-an-other` (erased there, open in the
rig); A2 and A6 under `baseline-with-the-third-reading-of-a` (open there,
erased in the rig); D1 under `baseline-but-not-permissions-in-their-layer`
(open there, missing in the rig). Each configuration is a reading the
rulings did not take; the rig implements baseline, so it differs from
each exactly where the model's baseline does. None fails the test.

Try 1's other two, not in the saved report: A8 and D2, `the play: error
ExecutionException: CallbackException: Callback failure {:reason :timeout
... :timeout-millis 10000 ... :task-id 3 ... :intent :read}`. The road,
above, fixed in the replay.

### Findings about the rig

1. **The micro door answers `:not-sealed` after a refused lease, where the
   plans give `:no-such-lock`.** D1 and D2. The model refuses the value act
   itself: D1's o0 `:permission-from-another-layer` (Alice's session
   permission lives in `:alice-hand`), D2's o1 `:permission-revoked` (her
   group permission revoked first). The rig refuses the lease act with the
   same reason, recorded, as KD1 expects. Then `micro-client/write!`
   (`micro_client.clj` L298-318) takes no locks (`ks` is `{}`), `seal`
   calls `locks/seal` with a nil lock, which returns nil (`locks.clj`
   L118-139), and the value act goes out with `:sealed nil`; the micro
   gate's parse refuses it `:not-sealed` on its face (`micro.clj`
   `not-sealed?` and `parse-micro`, L240-278), before any lock is looked
   up. The stream door does otherwise, by a documented rig choice: after a
   refused lease it seals under throwaway locks citing the ids the refused
   lease would have minted, so its gate answers `:no-such-lock`
   (`client.clj` `assign!`, L155-185). P2 L1968-1973 says such a value act
   "can cite no lock, so it is refused `:no-such-lock` on the face,
   unrecorded"; P3's test list names "a lock id never leased" among the
   `:no-such-lock` faces (PLAN-micro-store.md L773-777). The outcome is the
   model's (the value missing on both sides, nothing recorded for the value
   act); the reason is not, and `:not-sealed` names a malformed offer where
   the cause was a permission. Not fixed here: the store is not the
   replay's to change. The likely fix is the store's: `write!` doing what
   the stream door's `assign!` does, or sending no value act (then F8's
   rule, the lease alone, matches). Until then D1 and D2 fail the test by
   design.

No other finding: every other answer, read and value matched the model or
a named known difference.

### New known differences

None. Finding 1 is not a known difference: it is a difference between the
rig's two doors, and between the micro door and its plans, not a designed
difference from the model. KD1's rule is unchanged, so KD1 shows in D1
and D2 once the door is fixed.

### For RIG.md (pass 2)

- **P8-2, revised.** A fresh in-process cluster and module per case (was:
  a fresh module per case on one cluster); the road check in a cluster
  closed before any case; the fallback (one module, a fresh world per
  case) unchanged. Why: on one cluster every destroyed instance's client
  channels timed out about 25 s after its destroy, cases slowed from 5 s
  to 42 s, and two seeds stopped on a 10 s read timeout; with a cluster
  per case every case ran in 5 to 7 s.
- **Found (finding 1).** The micro door answers a value act `:not-sealed`
  after its lease is refused, where the stream door and P2 answer
  `:no-such-lock`: D1 and D2.

### Still to confirm, for wave 2

Pass 1's items 1 and 2, unchanged: phase 4's `lease-landing!`, `promote!`
(and its resend with the same name and claimed-when), `promotion-status`,
`inject/hold!` and `release!` at `:before-read-out` and `:before-forward`,
`env/landing-name`'s 3-arity, the crossing's answer by `c/record` of
`env/crossing-name` and the landing's by the micro `lookup`; phase 5's
rest, `rig.store.shared-reads/moment` (with it, A3, A4, A6 and B2, B4 read
the group through the exit, and KD6 and KD10 should show there). For pass
3 also: KD1 in D1 and D2 once finding 1 is fixed in the store.

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

## Pass 3 plan, after wave 2 (prep)

Claude Opus 5.5 at max effort, builder for pass 3 under builder C, 26
September 2026 from 07:14 IST, in `/mnt/data/projects/Softland-rig-build-replays-3`
on `rig-build-replays-3`, made off `rig-2026-09-25` at `c9684356`. The prep
reads wave 2's three branches read-only (`git show`), as they stood then:
`rig-build-promotion` at `5303418f` (phase 4), `rig-build-reads-rest` at
`33817364` (phase 5's rest), `rig-build-tools` at `2d2f666a` (phase 6).
Wave 2's merge (`rig-wave2`) was in progress: phases 4 and 6 merged
(`0feb72ef`, `df980ac7`), phase 5's rest with conflicts in `gate.clj`,
`micro.clj` and `module.clj`. Line numbers are those branches': "P4b" for
`rig-build-promotion`, "P5b" for `rig-build-reads-rest`, "P6b" for
`rig-build-tools`, "here" for this branch at `c9684356`. The build re-finds
every one on the merged tree before relying on it.

Loaded: the rama skill (SKILL.md, phases.md, phase-2-plan-validate.md and
phase-build.md, the two steps this pass takes). Read: this file whole;
PLAN-replays.md's scope, adapter, world, step kinds, cases, known
differences, report, APIs and open questions; RIG.md's overnight state (at
`040b58ba` too: step 6b is not started and is left to Sid); the model's B
cases (scenarios.clj L56-77) and their traces
(`runs/phase8-model-traces.txt` L117-244); `test/rig/replay_test.clj` whole;
phase 4's, phase 5's rest's and phase 6's build notes (their phase 8 names,
shared-file changes and decisions). The model the replay loads (the rig
branch's `../formal-model-2026-09-24/src`) has the same source as main's:
main's only later change there is `STARTER-round-3.md`.

### What wave 2 changes for the replays

**Phase 4** puts the door's promotion in `rig.store.promote-client`, not in
`rig.store.client` as the plan assumed:

- `connect [cluster]` (P4b promote_client.clj L29-34): micro-client's
  handles plus the `promotion-status` query as `:status-q`. Phase 4's own
  test merges it with the exit's: `(merge (rx/connect ipc) (pc/connect
  ipc))` (P4b promote_test.clj L164).
- `lease-landing! [store {:who :target :class :session :permission
  :uuid}]` (L81-93) returns `{:name :offer :lock-id :answer :public :for}`.
  The answer is under `:answer` (pass 1's `call-lease-landing!` read the
  whole map as the answer). `:public` is the landing row's public key, read
  back once the lease's batch is settled (`take-public`, L65-79, up to
  30 s), nil after a refusal or a timeout; `:for` is the landing name the
  row is bound to, `[T C :landing u]` (P4b promote_shape.clj L113-133).
- `request-offer [spec]` (L95-111) builds the request `[L :by-layer :offer
  u]` from the uuid the lease was taken under: one `:promote-request` fact
  whose value holds `:source :target :class :lease :public :permission
  :replaces :subjects`. Its `:source-stamp` and `:source-e` are the
  caller's; `promote!` reads them by `c/record` and `c/raw-row` (L136-137).
- `resend! [store p]` (L113-118) is `(c/offer-until-answered! store (:offer
  p))`.
- `promote! [store spec]` (L120-155) takes the lease, sends the request and
  reads the status, in one call. The replay must pause the micro topology
  between the lease and the request in B3 and B4, so it calls the two steps
  itself, with the functions `promote!` calls and in its order, as phase
  4's own test does (`promote-with!`, P4b promote_test.clj L95-111).
- `promotion-status [store layer req as-of]` (L38-45) gives a map;
  `status-of` (L47-50) gives the keyword alone, phase 8's form.
- The holds are `rig.store.inject/hold! [point nm]`, `release!`, `held?`
  (P4b inject.clj L77-100), keyed by the request's name. The continuation
  checks `held?` at `:before-read-out` and `:before-forward` (P4b
  promote_flow.clj L151, L154) and ends the record's processing there,
  blocking nothing; the door's resend, answered from the record, continues
  it (`continues?` is true for a yes from the record, P4b promote.clj
  L44-48).
- The request's ack carries the crossing's answer under `:crossing` when
  the read-out ran in its event (promote_flow.clj L153; phase 4's D2). A
  landing for the micro gate is appended to `*micro-offers` with
  `:append-ack` (L124), so while the micro topology is paused it waits in
  the depot, as the model's waits in the micro inbox.
- `env/landing-name` has the 3-arity `[req target class]` (P4b envelope.clj
  L275-282); `env/crossing-name [req]` gives `[L nil :crossing u]`
  (L269-273), and the crossing's record answers by name (V-1), so
  `c/record` of that name reads it.
- The status is `:done` only once the landing is settled under its task's
  frontier (P4b promote.clj `settled?` and `status`, L246-293): KD13.

**Phase 5's rest** makes the group readable through the one exit:

- `read-exit/read!` (P5b read_exit.clj L105-140) takes `:layer` a group (or
  the re-classed base), `:read [:point fids]` or `[:pattern p]`, and `:as-of`
  nil or `{:frontier F}` (`check-call`'s `moment-ok?`, L66-75). The query
  sends a layer of the shared kind to `shared-read-point` or
  `shared-read-pattern` on the same task (P5b reads.clj L1511, L1562); a
  frontier moment on a one-owner layer is refused `:moment-kind` (L1517,
  L1568). The read is as of F = min(asked, the frontier on the layer's
  task) (P5b shared_reads.clj `moment`, L282-294), visible to a member
  whose membership's batch is at or below F (`visible?`, L296-316); the
  answer carries `:moment {:frontier F}`, `:max-stamp`, and rows with
  `:batch` (`shared-answer`, L378-388).
- `rig.store.shared-reads/moment`, the adapter's sign for the shared read
  (here L629), exists (P5b L282), so the group's reads leave phase 3's
  frontier read for the exit on their own. A3's and A6's closing reads then
  reach KD10's rule (both members forgotten, Alice reads); A4's is Bob's
  (F4), so KD10 does not show there.
- The person forget with the micro index purge is `read-exit/forget-person!`
  (P5b read_exit.clj L362-370): phase 2's act, then `purge-person!`'s pages
  (L337-360), each waiting on the micro topology (`micro-op!`, L280-298).
  Phase 2's act alone is unchanged, and the stream gate's forget touches
  nothing on the micro side (the branch's `module.clj` change is one
  require and the queries' declaration).
- A read entry names its session (FRR10); the read keys gain
  `:read/standing`, `:read/delivery`, `:read/closed` (standing reads, which
  the replays do not take).

**Phase 6** moves the stream side's grammar into facts:

- The stream gate and the one-owner exit read a key's grammar only from
  `:grammar` facts in the layer (P6b grammar.clj L9-21; D-P3 in its notes);
  a key with no grammar is permissive, so on the stream side a `:mention`
  with no grammar fact names no one. The micro store still reads the
  constant `grammar/grammars` (L28-33) until step 6b, which is not started.
- The earlier suites write the model's grammar as facts through a test
  helper, `rig.store.toy-grammars/write! [store layers]` (P6b
  test/rig/store/toy_grammars.clj L33-40): one operator act per layer with
  the `:mention` grammar (`:subjects-at [:persons]`) and a `:note` grammar
  that names no one, indexed by value as `seed-hints` had it (D-P4). The
  replay's seed writes none, so on the merged tree A1's and A8's mentions
  in `:alice` would name no one: the rig's world would no longer be the
  model's, and a match there would be a match for another reason.

### The bindings, as pass 3 binds them

| pass 1's binding to confirm | as wave 2 built it | cases |
|---|---|---|
| `client/lease-landing! [store spec]` | `promote-client/lease-landing! [store {:who :target :class :session :permission :uuid}]`; the answer under `:answer`, with `:public` and `:for` | B1-B4 |
| `client/promote!`, the request named before sending | `promote-client/request-offer`, whose name `[L :by-layer :offer u]` is known before sending, sent by `client/offer-until-answered!` after the lease; `:source-stamp` by `client/record`, `:source-e` by `client/raw-row`, as `promote!` reads them | B1-B4 |
| the door's resend of the same request, from the record | `promote-client/resend! [store {:offer o}]` | B1, B2 |
| `client/promotion-status [store layer req nil]` | `promote-client/status-of [store layer req nil]`, the keyword | B1-B4 |
| `inject/hold!`, `release!` at `:before-read-out`, `:before-forward` | the same names, phase 4's, `[point nm]` | B1, B2 |
| `env/landing-name` with the class | the same, 3-arity, with the class the request carries | B3, B4 |
| the crossing's answer by `c/record` of `env/crossing-name` | the same | B1-B4 |
| the landing's answer by the micro `lookup` of its name | the same (`micro-client/lookup`, answering only once the frontier passes the landing's batch) | B3, B4 |
| the merged handle `(merge (read-exit/connect ipc) (micro-client/connect ipc))` | `(merge (read-exit/connect ipc) (promote-client/connect ipc))` when phase 4 resolves (micro-client's handles plus `:status-q`), else as before | all |
| `rig.store.shared-reads/moment`, the sign of the shared read | the same | the group's reads |
| (new) the request's uuid | `env/uuid7`, made before the lease, as `promote!` makes it | B1-B4 |
| (new) the model's grammar as facts | `rig.store.toy-grammars/write!` (stage 6) | all |
| (dropped) `env/make-name` for the request's name | the name comes from `request-offer` | — |

### The changes, in the order the build makes them

**P3-1. The merge.** `git merge rig-2026-09-25` once wave 2 has landed
there. No wave 2 branch touches the replay's files; if the merge brought an
edit to `replay_test.clj` (wave 2's merger runs `rig.replay-test` in its
suite), that edit is read first and kept.

**P3-2. The API table** (`apis`, here L559-630). Stage 4 rebound to
`rig.store.promote-client`: `connect`, `lease-landing!`, `request-offer`,
`resend!` and `status-of`, each `:built`; `inject/hold!` and `release!`
marked built. Added for the B cases: `client/raw-row` (stage 2) and
`env/uuid7` (stage 1). Dropped: `env/make-name`, which only named the
request. Added for every case: `rig.store.toy-grammars/write!` as stage
"6", which joins `stage-order`.

**P3-3. The promotion's plays** (here L1128-1303).

- `play-request!`, at the model's decision of the request: u by
  `env/uuid7`; the target's class by `micro-client/settings-of`, as
  `promote!` reads it; `lease-landing!` into the target by Alice in her
  session `:alice-session`, citing her permission there (the model's
  landing permission mapped, `[:alice :group :group [:group :group
  :group]]`). The landing lease is recorded as a rig-only act with its
  `(:answer ll)`; a refusal is a difference named as itself (F11). With no
  `:public` the door sends no request (P4-11), so the model's request has
  no counterpart, reported as such. Else `request-offer` with the mapped
  source, its stamp and entity read as `promote!` reads them, the target,
  its class, the public key, the landing permission, the model's
  `:replaces` mapped (nil in all four), the subjects nil (the door's `#{}`)
  and u. KD3 is marked once the lease answered yes with its `:public` and
  its `:for` is the landing this request will cause, `(env/landing-name
  (:name o) T C)`; a lease bound to another landing is a difference. Then,
  by the model's later decisions as now (`decided-later?`): the hold armed
  on `(:name o)` at `:before-read-out` (B1, B2), or the micro topology
  paused (B3, B4). Then the door's send. The request's record keeps its
  offer and its ack.
- `resend-request!`: `promote-client/resend!` with the kept offer; the
  resend's ack is kept on the request's record.
- `play-crossing!`: the road is unchanged (release and resend when held,
  then the crossing's record by `c/record` of its name). Added, D2's check:
  when the request's or the resend's ack carries `:crossing`, its answer
  and reason must be the record's, else a difference.
- `play-landing!`: the landing's name with the class the request carries.
  On a model yes and a rig no, the line names the phase 4 F9 condition the
  rig's reason points to: `:landing-lock-gone`, her landing lease row gone
  (her forget deleted her lease rows in the target, or closed her session
  there); `:person-forgotten`, a person check on the landing's `:who`,
  which the micro gate does not make (PR13); `:no-such-lock`, no lease row
  bound to the landing.
- `call-promotion-status`: `status-of`, the keyword.
- `connect` (here L759-766): the exit's handles merged with
  promote-client's when phase 4 resolves, else with micro-client's.

**P3-4. The group read through the exit** (here L1031-1037, L1372-1449,
L1470-1539).

- `exit-read!` takes the read's moment: nil for a one-owner layer,
  `{:frontier F}` for the group, F read by `micro-client/frontier` just
  before the read. The store's view below the exit, beside a refusal, takes
  the same moment. The answer's `:moment` is kept, and the read line prints
  it (`group as alice at F`).
- An answered group read's moment must cover every batch the replay waited
  past: `wait-frontier!` records the largest, and an answer whose moment is
  `{:frontier F'}` with F' below it is a difference (a read that could miss
  a decided act). A refused read has no moment to check.
- F2 completed on the group. The group is also read by pattern below the
  exit (`read-pattern`, `[:all]`, the same moment, as the live member); a
  row that is no counterpart of a model fact in this read and not a
  rig-only kind is a difference, as for one-owner layers. The group side of
  F2 was "every rig name the replay sent into the group", a stand-in for
  this read until it existed.
- F1 completed on the group. `rig-values!` reads the group by `[:k k]` below
  the exit too, as the live member (else Alice) at `{:frontier F}`, as it
  reads a one-owner layer by its owner; a row matching a ref that no act
  the replay knows of carries is a difference.
- The layers a read covers include the target of every request sent so
  far, where its landing lease went (D6: the layers the rig wrote into). So
  B1 and B2 read the group at every read, and a landing the rig made where
  the model made none would show.
- `rig-only-keys` (here L931-936) gains `:grammar` (P3-5) and the standing
  reads' keys `:read/standing`, `:read/delivery`, `:read/closed`.
- The roads line says "the group read through the exit at {:frontier F}".

**P3-5. The world after phase 6.**

- The seed writes the model's grammar as facts, `toy-grammars/write!`, into
  the model's three one-owner layers (`:alice`, `:alice-hand`,
  `:alice-agent`), right after they are made and before any other act in
  them. Bob's rig-only `:bob-hand` gets none: no value is written there.
  The `:note` fact the helper also writes names no one, as the model's
  `:note` does; its value index is read by no replay read.
- KD8's rule gains the grammar: the model's one grammar is a constant; since
  phase 6 the rig's is a fact in each one-owner layer, written by the
  operator in the seed, and the micro side reads the constant until step
  6b.
- A guard, so step 6b cannot change the world under a case without it
  showing: when the micro gate takes grammar facts (`:grammar` in
  `rig.store.micro/control-keys`, the sign step 6b's own list names, P6b
  BUILD_NOTES-tools-and-grammars.md L237, L248), a case that writes a
  `:mention` into the group (A2, A4 to A7) is not practical: "step 6b is
  built: the group's grammar is a fact, and the seed writes it on the
  stream side only". With its stages resolved that fails the test.

**P3-6. The report and these notes.** The header's stages line gains stage
6; the roads line names the promotion's two steps and the group's moment. A
section "Pass 3, after wave 2" records the run, the cases, every DIFFERS
line with its cause, the findings and "For RIG.md".

### The B cases: what each binds to, and what the model says it must answer

The model's side is its own run under `baseline` (the traces, L117-244);
names are the model's (`o0`, `o2`, `o3`, `crossing:o2`, `landing:o2`), and
stamps are compared only by the relations KD12 names. Every case's seed is
the world table's plus P3-5's grammar facts; every case settles Alice's
note `o0` (`e0 :note {:token "v1"}` into `:alice`) first: the model's yes at
`[:work 0]`, the rig's `stock!` lease in `:alice` (KD2) and the value act,
both yes. Reads compare the case's own facts (KD11): in `:alice` through
the exit as Alice (the store's view below it beside a refusal), in the
group per the next section, and each request's status by `status-of`.

**B1, a value forget queued before the read-out: pending, then refused.**

| model op | the model decides | the rig plays, and must answer |
|---|---|---|
| `[:promote 0 :group 1]`, `[:forget-value 0]` | queues `o2` (source `o0#0`), then `o3` (target `o0#0`) | nothing yet: the rig acts at the decision |
| `[:step 0]` | `o2` yes | the landing lease in `:group`, yes, `:for` the landing (KD3); the hold at `:before-read-out` on the request's name (the model decides `crossing:o2` two ops later); the request, yes, its ack with no `:crossing` |
| `[:read :now]` | `o0#0` v1, `o2#0` `{:source o0#0 :target :group}`; `o2` pending | `[o0 0]` `{:token "v1"}`; `[req 0]` a value holding the mapped source and `:group` (KD20); the group holding no case fact; `:pending` |
| `[:step 0]` | `o3` yes; `o0#0` erased | `forget-value!` by Alice of `[o0 0]`, yes, with `:how` (KD17) |
| `[:read :now]` | `o0#0` erased, `o2#0`, `o3#0` `{:target o0#0}`; pending | `[o0 0]` erased after its stamp (KD12); `[req 0]`; `[o3 0]` holding `[o0 0]` (KD20); `:pending` |
| `[:step 0]` | `crossing:o2` no `:source-erased` | release, the door's resend answered from the record (yes), whose event reads out: the crossing's record no `:source-erased` (KD4), the resend's ack `:crossing` the same (D2's check) |
| closing read | as before; `o2` refused | as before; `:refused` |

Values: `alice note` erased. Shown: `[:pending :pending :refused]`. Phase
4's T1 played the same shape against the model's `:shown` (P4b
promote_test.clj L327-357).

**B2, Alice forgotten before the read-out: pending, then refused.**

| model op | the model decides | the rig plays, and must answer |
|---|---|---|
| `[:promote 0 :group 1]`, `[:step 0]` | `o2` yes | the landing lease, yes (KD3); the hold; the request, yes |
| `[:read :now]` | `o0#0` v1, `o2#0`; pending | `[o0 0]` v1, `[req 0]` (KD20); the group holding no case fact; `:pending` |
| `[:forget-person :alice]` | her person lock destroyed | `forget-person!` of Alice, yes, its fan-out seen on all 64 keys (KD7) |
| `[:work 0]` | `crossing:o2` no `:source-erased` | release, the door's resend by a forgotten requester, answered from the record (yes; phase 4's T2 did the same for Ann, P4b promote_test.clj L338-366); the crossing no `:source-erased` (KD4), the ack's the same |
| closing read | `o0#0` erased, `o2#0`; refused | `:alice` refused for Alice (KD10, F3's rule: the entry's lease `:person-forgotten`, the entry `:no-such-lock`), the store's view beside it: `[o0 0]` erased, `[req 0]`; the group, read as Bob, holding no case fact; `:refused` |

Values: `alice note` erased. Shown: `[:pending :refused]`.

**B3, a value forget after the read-out: crossed, then done; the copy stays.**

| model op | the model decides | the rig plays, and must answer |
|---|---|---|
| `[:promote 0 :group 1]`, `[:work 0]` | `o2` yes, then `crossing:o2` yes; `landing:o2` queued | the landing lease, yes (KD3); the micro topology paused (the model decides the landing at `[:batch]`); the request, yes, its ack `:crossing` yes; the forward waits in `*micro-offers`; the crossing's record yes (KD4), the ack's the same |
| `[:read :now]` | `o0#0` v1, `o2#0`, `crossing:o2#0` `{:request o2 :source o0#0}`; crossed | `[o0 0]` v1, `[req 0]`, `[crossing 0]` holding the request's name and `[o0 0]` (KD20); the group holding no case fact (the shared read runs while paused: a query, not the batch); `:crossed` |
| `[:forget-value 0]`, `[:work 0]` | `o3` yes; `o0#0` erased | `forget-value!`, yes, `:how` (KD17) |
| `[:read :now]` | `o0#0` erased, `o2#0`, `crossing:o2#0`, `o3#0`; crossed | the same, `[o0 0]` erased; `:crossed` |
| `[:batch]` | `landing:o2` yes, at `[:micro 0]` and `[:micro 2]` (one yes, F6) | the topology resumed; the landing's answer by `micro-client/lookup` of `(env/landing-name req :group :by-entity)`: yes, not `:landing-lock-gone`; the frontier past its batch (KD13) |
| `[:read :now]` | adds `landing:o2#0` in the group, v1; done | `:alice` as before; the group as Alice through the exit at `{:frontier F}`: `[landing 0]` `{:token "v1"}`; `:done` |
| closing read | the same | the same |

Values: `alice note` erased, `group note` open (the store's opening of the
landing's row through F). Shown: `[:crossed :crossed :done :done]`.

**B4, Alice forgotten after the read-out: crossed, then done; the copy stays.**

| model op | the model decides | the rig plays, and must answer |
|---|---|---|
| `[:promote 0 :group 1]`, `[:work 0]` | `o2` yes, `crossing:o2` yes | as B3: lease, pause, request, crossing |
| `[:read :now]` | `o0#0` v1, `o2#0`, `crossing:o2#0`; crossed | as B3's first read; `:crossed` |
| `[:forget-person :alice]` | her lock destroyed | `forget-person!` of Alice while the micro topology is paused: the act and its fan-out are the stream gate's, so nothing waits on the pause; yes, fan-out seen (KD7). Not `read-exit/forget-person!`, whose purge pages would wait on the paused topology |
| `[:batch]` | `landing:o2` yes | resumed; the landing yes (F5: a refusal fails the test, its line naming F9's condition); her bare landing lease row outlived her forget (phase 4's T4, P4b promote_test.clj L433-434); the frontier past its batch (KD13) |
| `[:read :now]` | `o0#0` erased, `o2#0`, `crossing:o2#0`, `landing:o2#0` v1; done | `:alice` refused for Alice (KD10), the store's view beside it; the group as Bob (F4) through the exit: `[landing 0]` v1; `:done` |
| closing read | the same | the same |

Values: `alice note` erased, `group note` open. Shown: `[:crossed :done
:done]`.

Under `baseline-but-not-p6-line-at-the-read-out` the model's B lines are as
said with P6 objecting to B3's and B4's copies; the rig's line beside
them says the outcome is baseline's and the objections are the model's own
(unchanged since pass 1).

### The group read through the exit, case by case

The reader is the live member (F4): Alice, else Bob; with neither, Alice,
whose refusal KD10 judges. Each group read asks `{:frontier F}` with F the
frontier just before it; point reads of the counterparts of the model's
group facts go through the exit (an entry in the reader's working layer,
KD6), and the pattern read below the exit looks for anything else.
Predicted from the code read, not run:

| case | reader at the reads that touch the group | predicted |
|---|---|---|
| A2 | Alice (Bob forgotten) | the mention of Bob erased |
| A3 | Alice, forgotten: refused | KD10; the store's view: the note open |
| A4 | Bob (Alice forgotten) | the mention of both open; KD10 predicted by the plan's table and not seen, since F4 gave the group a live reader (a stale prediction, printed, failing nothing) |
| A5 | Alice (Bob forgotten) | open |
| A6 | Alice, forgotten: refused | KD10; the store's view: erased |
| A7 | Alice (Bob forgotten) | the marked mention erased |
| B1, B2 | Alice; Bob after B2's forget | no case fact in the group: no point read and no entry, the pattern read only |
| B3 | Alice | reads 1 and 2 (paused): no case fact; read 3 and the closing read: the landing's copy v1 |
| B4 | Alice at read 1; Bob after her forget | read 1: no case fact; read 2 and the closing read: the landing's copy v1 |
| D1 | Alice | no case fact (the write refused): the pattern read only |
| D2 | Alice | the revoke `o0#0`, a control fact holding the mapped pid (KD20) |

The landing's copy through the exit (B3, B4) is the first read of a landed
copy through the shared exit anywhere in the rig: phase 4's tests read it
through the frontier (`micro-copy`, P4b promote_test.clj L127-131), and
phase 5's rest's tests have no landing (neither `reads_rest_test.clj` nor
`shared_reads_test.clj` names one). Block 2d indexes an act's rows in the
batch that decides it, and `open-entry>` opens an entry from its copies of
the row and its lock record (P5b shared_reads.clj L700-721); whether a
landing's row gets such an entry depends on how wave 2's merger joined
phase 4's landing path and phase 5's block 2d in `micro.clj`, in conflict
at the prep. Predicted open. If the copy is unreadable or absent through
the exit while the store's own opening finds it open, that is a finding
about the rig (a seam between phases 4 and 5's rest), once the trace shows
the entry; it is not the adapter's.

### Not exercised, and why

- `promote!` whole: its two steps are, with its own functions and order.
- `read-exit/forget-person!`, the person forget with the micro index purge:
  B4 forgets Alice while the micro topology is paused, and `purge-person!`'s
  pages wait on it (`micro-op!`), so the wrapper would stall B4 for 120 s.
  The replay keeps phase 2's act, as passes 1 and 2 did. The shared read
  opens an entry through `$$persons` (`open-entry>`), so a dying entry reads
  erased whether or not the purge ran; the purge's own promise is phase 5's
  rest's tests' to check.
- The re-classed base: no fixed history has a case fact there (KD8), and
  the model's reads show only its grants, which KD11 leaves out, so the
  replays do not read it. The shared read of the base is phase 5's rest's
  tests'.
- Standing reads, deltas and `[:kv]` reads: the fixed histories have no
  counterpart.
- The group's grammar as a fact: step 6b, not built; guarded (P3-5).

### What the run should show

One run, under the lock, from the rig folder, then `cp
runs/phase8-replays.txt runs/phase8-replays-pass3.txt`. Predicted: 14
cases, 14 played; A1 to A8 and D2 "as said"; D1 "approximated (prepare and
commit are one batch …); as said"; B1 and B2 "approximated (the hold at
:before-read-out and a resend …); as said"; B3 and B4 "approximated (the
paused micro topology …); as said"; stages 1, 2, 3, 4, 5a, 5's rest and 6
resolved; the group read through the exit at `{:frontier F}` in A2 to A7,
B3, B4 and D2. Known differences seen, beyond pass 2's merged run
(`runs/phase8-replays.txt` at 06:45:59, KD1 seen in D1 and D2): KD3, KD4 in
B1 to B4; KD13 in B3, B4; KD17 in B1, B3; KD20 in B1 to B4; KD10 in B2, B4
and, through the group, A3 and A6; KD6 in A2 to A7 and D2, whose group
reads now make entries (pass 2 printed it "predicted, not seen" there).
Predicted, not seen: KD10 in A4 (F4, above). Every DIFFERS line is either
a finding about the rig, for RIG.md, or a bug in the adapter, and the
notes say which, with the trace.

### The plan validated, the rama skill's way (its phase 2)

The skill's rule: the verdict starts at FAIL, and a check passes only after
a concrete scenario is traced through the plan's lines and the code they
bind to; a check whose body calls something a gap, not ideal or a tradeoff
fails. The template's module checks (PState schemas, partitioning,
topologies, depots, query topologies, throughput) have nothing to check in
a test adapter that adds no part to the module, as pass 1's self-validation
found (PLAN-replays.md L788-795). The plan's lines below are this file's.

**V1. SPEC.md's phase 8**, verbatim: "Where practical, replay the model's
fixed histories from scenarios.clj through the rig and compare answers and
reads with the model's. Report every difference; a difference is a finding,
not something to hide." Scenario, B1: the model decides `o0`, `o2`, `o3`
and `crossing:o2` (traces L121-124); the rig's counterparts are played at
those ops and judged name by name (L686-700), 4 of 4, the landing lease a
rig-only act that must answer yes (KD3, F11). Each of the three reads
compares `[o0 0]`, `[req 0]` and `[o3 0]` fact by fact, control facts by
their references (KD20), and reads the group too (L641-644), so a landing
where the model has none would be a DIFFERS line. `:shown` is the three
`status-of` answers against `[:pending :pending :refused]`. PASS.

**V2. The brief's B cases**, verbatim: "the B cases, with phase 4's names
from rig-build-promotion: rig.store.promote-client, where status-of gives
the bare keyword". Scenario, B3's first read: `call-promotion-status` calls
`status-of store :alice req nil` (L615), which is `(:status
(promotion-status …))` (P4b promote_client.clj L47-50); the request's record
is yes, the crossing's yes, no landing record while the topology is paused,
so `promote/status` gives `:crossed` (P4b promote.clj L253-293). The four
call functions: the lease reads its answer under `:answer`, which pass 1's
shape did not (L583-591); the request is `request-offer` sent by the door
(L591-601); the resend is `resend!` (L602-603); the status is `status-of`.
PASS.

**V3. The brief's group reads**, verbatim: "group reads through the exit,
from rig-build-reads-rest: rig.store.read-exit/read! with a group or the
re-classed base and {:frontier F}". Scenario, A4's closing read: Alice is
forgotten, so the reader is Bob (F4); F is `micro-client/frontier`, say 57;
`read!` gets `:reader :bob :working :bob-hand :permission [:bob :bob-hand
:bob-hand] :layer :group :read [:point [[o0 0]]] :as-of {:frontier 57}`;
`check-call` takes the moment map (P5b read_exit.clj L66-75); `read-point`
finds no stream settings for `:group`, a layer of the shared kind (P5b
reads.clj L485-493), and hands it to `shared-read-point` (L1511); F' =
min(57, the group task's frontier) = 57 (P5b shared_reads.clj L282-294);
Bob's membership batch is below 57 (L296-316); the row opens; the entry
lands in `:bob-hand`; the answer carries `:moment {:frontier 57}`, which
P3-4 checks against the largest waited batch (L627-630). The re-classed
base is not read: no fixed history has a case fact there, and the model's
reads show only its grants, which KD11 leaves out (L795-798), so a base
read would compare nothing. PASS.

**V4. The brief's comparison**, verbatim: "Wherever the formal model (…)
has the same history, compare answers. A difference is a finding: record
it, never hide it." Scenario, A8 on the merged tree without P3-5: `:alice`
has no `:mention` grammar row, so the stream gate names no one in `{:token
"v2" :persons #{:bob}}` where the model's grammar names Bob; A8's value is
open on both sides, but the rig's history is no longer the model's, and
the match would stand on another world. With P3-5 the seed's grammar act
makes the row before the first mention (L651-660), and the gate reads the
model's grammar again. Step 6b, when built, would do the same to the group
under A2 and A4 to A7; the guard names it instead of letting "seen
differs" hide the cause (L661-667). PASS.

**V5. R33**, verbatim: "A fresh in-process cluster and module per case".
Scenario, B3's pause: `pause!` uses the env's `:ipc`, which `play-case!`
sets to the case's own cluster (here L1602-1603); `cleanup!` resumes the
topology in `play-case!`'s `finally` before that cluster closes (here
L1582-1588, L1624-1628). Nothing in P3-3 or P3-4 shares a cluster between
cases. PASS.

**V6. Pass 3's own ask** (L144-152), verbatim: "First confirm phase 4's
shapes against its build (the list above) and adapt the four call
functions; then check `:shown` against the model's (…), KD3, KD4 and KD13
seen, and that B4's landing is not refused (F5: a refusal fails the test
and its line names which of phase 4's F9 conditions it points to)."
Scenario, B4's landing answered `:landing-lock-gone`: `judge-answer` finds
model yes, rig no, unexplained, so the case fails, and P3-3's note names
"her landing lease row gone" (L608-614). KD3 is marked only on its evidence,
the lease's `:for` (L595-597); KD4 when the crossing's record is read; KD13
after the frontier passes the landing's batch. `:shown` is compared by
`judge-case`, `rig-seen` against `seen-of` (here L1541-1546, L502-512).
PASS.

**V7. F1 and F2** (PLAN-replays.md L182-192, L203-213), verbatim in part:
"a rig fact matching the ref that the adapter does not know of is a
difference"; "a value fact that is not a rig-only kind (…) and has no
counterpart is a difference". Scenario, B1 against a rig that lands a copy
it should not: before P3-4 the group is not read in B1 at all (no model
fact and no replay act there); with P3-4 the group is read at every read
(L641-644), and its `[:all]` pattern read below the exit finds `[landing 0]`,
key `:note`, no counterpart: DIFFERS. The same rule runs on every group
read (L631-640). PASS.

**V8. R35**, verbatim: "A continuation is held only when the model decides
it in a later op". Scenario: `decided-later?` (here L1168-1174) at B1's
request finds `crossing:o2` two ops later, so the hold; at B3's `[:work 0]`
the crossing is in the same op and the landing at `[:batch]`, so the pause.
The test `holds-follow-the-model` (here L2218-2236) pins both. The hold is
armed on `(:name o)`, the name the gate checks (`(get *offer :name)`, P4b
promote_flow.clj L147-151), the same name phase 4's own test arms
(`promote-held!`, P4b promote_test.clj L87-93). PASS.

**V9. The pause blocks nothing the model decides meanwhile** (PLAN-replays.md
open question 2, L832-842). Scenario, B4 between the pause and the resume:
the request (the stream gate), the crossing's record (a PState read), the
`:alice` read through the exit (its entry in `:alice-hand`, the stream
gate), the group's pattern read below the exit (the `read-pattern` and
`shared-read-pattern` query topologies, P5b reads.clj L1562, shared_reads.clj
L1688), `status-of` (the `promotion-status` query, which phase 4's T3 read
while paused, P4b promote_test.clj L403-406), Alice's forget
(`client/forget-person!`, the stream gate's act and fan-out; phase 5's rest
adds to the stream flow only a require and query declarations) and its
wait (`person-on-task`, a PState read). None runs in the micro topology.
The purge wrapper, which would, is not called (L788-794). PASS.

**V10. B2's resend by a forgotten requester**, answered from the record:
phase 4's T2 resent Ann's request after `forget-person! :ann` and read
`:source-erased` in the ack's `:crossing` (P4b promote_test.clj L338-366).
The replay sends the same map again (`resend!` with the kept offer), so the
digest is the record's. PASS.

**V11. The new checks cannot fire on a correct rig.** The ack's
`:crossing` is the decision's own ack, whose record `write-decided>`
writes, or on a resend the record's `recorded-ack` (P4b promote_flow.clj
L65-72, L91-103), so answer and reason agree with `c/record`. A landing
row's `:for` is the landing its lease act named, `[T C :landing u]` (P4b
promote_shape.clj L113-133), and `(env/landing-name (:name o) T C)` is `[T C
:landing u]` for the same u (P4b envelope.clj L275-282). Block 0 writes the
frontier on every task in every batch (here micro.clj L1262-1266) and a
microbatch's writes become visible together, so after `wait-frontier!` the
group's task holds the frontier task 0 showed: F' = F, at or above every
waited batch. The model has no `:grammar` fact and no standing reads, so
the keys P3-4 adds to `rig-only-keys` hide none of its facts. PASS.

**V12. Sid's rules for the build**: one run under the lock (L805-806); a fix
reruns only `rig.replay-test`, the one namespace the build touches; no
reset, checkout, restore, stash, rm or mv of tracked files; `git add` of
named files; commits on this branch only, never pushed; nothing deleted
(`env/make-name` leaves the replay's table, not the store). PASS.

**What only the run can show**, each a prediction with its other outcome
named, which is what the replays are for: the landing's copy read through
the exit (L771-783); D2's revoke read through the shared exit, a control
fact in the micro index (block 2d indexes every row block 2b gives, P5b
shared_reads.clj L191-206, so predicted there); Bob's first reads through
the exit (A4, B4; pass 2 read the group through the frontier, so his
working layer has never taken an entry).

**Self-consistency.** Nothing above calls a constraint's handling a gap or
a tradeoff. The items not exercised (L785-801) sit outside the checked
constraints: no model counterpart, or the model's step played by another
of the store's roads, each with its reason. The first reading of the plan
found three places to fix, in P3-3's and P3-4's wording rather than their
design, fixed in place above: KD3's `:for` check named the request's name
before `request-offer` made it; the moment check did not say a refused
read has none; F1's group read named no reader or moment.

Verdict: **minor-fail**, the three fixes applied in place.
PHASE_VALIDATION:minor-fail

### Prep: where it stopped (07:41 IST)

The plan is written and validated, and nothing is built: `replay_test.clj`
is as pass 2 left it. Next, on builder C's word that wave 2 has landed on
`rig-2026-09-25`: P3-1 to P3-6, through the rama skill's build step
(implement; validate the implementation against this plan; the tests the
changes need, with no cluster; validate them), then `rig.replay-test`
once under the lock, then "Pass 3, after wave 2" in this file with each
case's match or finding and "For RIG.md", committed, and the report to C.

## Pass 3, after wave 2

Claude Opus 5.5 at max effort, 26 September 2026 from 07:52 IST, in
`/mnt/data/projects/Softland-rig-build-replays-3`, on builder C's word that
wave 2 had landed on `rig-2026-09-25` at `27543fd7` (one suite on the merged
code: 148 tests, 7,699 assertions, 0 failures). `git merge rig-2026-09-25`
made `ab36a274` with no conflict. Wave 2's merge had already edited the
replay (W2-2 in BUILD_NOTES-wave2.md): the seed writes phase 6's toy
grammars as facts into each one-owner layer, and `:grammar` is a rig-only
key. Builder C's two calls: build on W2-2's write and add no second one;
yes to P3-5's guard (step 6b is left to Sid).

### What was built

Against the pass 3 plan (P3-2 to P3-6; P3-5's grammar write is W2-2's),
in `test/rig/replay_test.clj`, commit `b91ce2f4`:

- **The API table**: phase 4's door bound where it was built,
  `rig.store.promote-client` (`connect`, `lease-landing!`, `request-offer`,
  `resend!`, `status-of`); `inject/hold!` and `release!` built;
  `env/uuid7`, `client/raw-row` and `micro/control-keys` added;
  `env/make-name` gone; the shared read's sign marked built.
- **The promotion's plays**: `play-request!` sends `promote!`'s two steps
  (the landing lease under the request's uuid, then `request-offer` and the
  door's send), with the hold or the pause between them; KD3 marked only
  on the lease's `:for`; D2's check (`crossing-ack-says`) of every ack's
  crossing against the crossing's record; a refused landing names phase 4's
  F9 condition (`f9-condition`); `status-of` for `:shown`; `connect` merges
  promote-client's handles (they carry `:status-q`).
- **The group read through the exit**: `read!` at `{:frontier F}`, F the
  frontier just before the read; an answered moment checked against the
  largest batch waited past (`moment-says`); the group read by pattern
  below the exit for facts the model lacks (F2) and by `[:k k]` in
  `rig-values!` (F1); each request's target read at every read.
- **The guard**: `world-refusal` makes A2 and A4 to A7 not practical, which
  fails the test, once `:grammar` is among the micro gate's control keys.
- **Tests with no cluster**: four new deftests and two extended (TEST_VALIDATION-replays.md,
  "Pass 3").
- **Validations**: IMPLEMENTATION_VALIDATION-replays.md and
  TEST_VALIDATION-replays.md, "Pass 3", both pass before the run
  (`dcf43247`). Two divergences: a refused F1 pattern read is now a
  difference in `rig-values!`, as it was in `read-layer!` (D-P3-1); W2-2
  writes the grammars into `:bob-hand` too (D-P3-2), kept.

### The bindings, seen at work

Every binding the plan listed resolved on the merged tree (the report's
stages line: stage 4 all 7, stage 5's rest 1, stage 6 1) and did its work
in the run:

- **The landing lease** (`promote-client/lease-landing!`): answered yes
  with its public key in all four B cases, and its `:for` was the landing
  the request caused in all four, so KD3 was marked (a mismatch would have
  been a DIFFERS line).
- **The request** (`request-offer`, then the door's send): yes in all four,
  its fact read back through the exit holding the mapped source and
  `:group` (KD20).
- **The hold and the resend** (`inject/hold!`, `release!`,
  `promote-client/resend!`): B1 and B2 read `:pending` while held; each
  resend answered yes from the record and read out, the crossing refused
  `:source-erased`. D2's check found no ack whose crossing differed from
  the crossing's record (it compares only an ack that carries one).
- **The pause** (Rama's `pause-microbatch-topology!` on `"micro"`): while
  the landing waited in the depot, B3 read `:crossed` twice and B4 once;
  the frontier stayed at 12 while paused and was 14 after the landing.
- **`status-of`**: the four `:shown` sequences, the model's exactly.
- **The shared read** (`read-exit/read!` at `{:frontier F}`): every group
  read went through the exit, its moment covering every batch waited past
  (no `moment-says` line). The landing's copy read open through the exit,
  `{:token "v1"}`, in B3 as Alice and in B4 as Bob: the first read of a
  landed copy through the shared exit, where phases 4 and 5's rest meet.
- **The grammar facts** (W2-2): the seed's four "grammars in L" acts
  answered yes in every case (seed 23 acts, 19 in pass 2).

### The run

From the rig folder, once, under the lock:

```
flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:test rig.replay-test
cp runs/phase8-replays.txt runs/phase8-replays-pass3.txt
```

Queued at 08:01:55 IST and took the lock at once (no other run held it);
the JVM ran to 08:03:56 IST: **25 tests, 792 assertions, 0 failures, 0
errors**. The report (`runs/phase8-replays.txt`, copied to
`runs/phase8-replays-pass3.txt`): run 08:02:05 IST, 110 s, on `fab4f8f8`
with nothing uncommitted in the rig folder; road check launch 3,639 ms,
destroy 286 ms, relaunch 3,051 ms, so a fresh cluster and module per case
(R33); every stage resolved; **14 cases, 14 played, 14 as said, 0 differ,
0 not practical, 0 failing**. Each case ran in 5.9 to 8.3 s. The log,
git-ignored: `runs/phase8-replays-pass3.log` (no pong timeout, no
`ModuleAssignmentInfoNotFoundException`, no exception).

### The cases

| case | state | answers | reads, and the group's reader | known differences |
|---|---|---|---|---|
| A1 | as said | 1 of 1 | `:alice` refused for Alice after her forget, the store's view: note and mention erased | KD2 KD6 KD7 KD8 KD9 KD10 KD11 KD12 |
| A2 | as said | 1 of 1 | group as Alice at F=14: the mention of Bob erased | KD2 KD6 KD7 KD8 KD9 KD11 KD12 |
| A3 | as said | 1 of 1 | group as Alice (both forgotten) refused `:no-such-lock`, the store's view: the note open | KD2 KD6 KD7 KD8 KD9 KD10 KD11 KD12 |
| A4 | as said | 1 of 1 | group as Bob (F4) at F=14: open | KD2 KD6 KD7 KD8 KD9 KD11 KD12; KD10 predicted, not seen |
| A5 | as said | 1 of 1 | group as Alice at F=14: open | KD2 KD6 KD7 KD8 KD9 KD11 KD12 |
| A6 | as said | 1 of 1 | group as Alice (both forgotten) refused, the store's view: erased | KD2 KD6 KD7 KD8 KD9 KD10 KD11 KD12 |
| A7 | as said | 1 of 1 | group as Alice at F=14: the marked mention erased | KD2 KD6 KD7 KD8 KD9 KD11 KD12 |
| A8 | as said | 1 of 1 | `:alice` as Alice: the mention of Bob open | KD2 KD6 KD7 KD8 KD9 KD11 KD12 |
| B1 | approximated (the hold at :before-read-out and a resend …); as said | 4 of 4: `o0` yes, `o2` yes, `o3` yes, `crossing:o2` no `:source-erased` | `:alice` as Alice at each read; group as Alice at F=11, no case fact; `:pending :pending :refused` | KD2 KD3 KD4 KD6 KD7 KD8 KD9 KD11 KD12 KD17 KD20 |
| B2 | approximated (the hold …); as said | 3 of 3: `o0`, `o2` yes, `crossing:o2` no `:source-erased` | read 1 as Alice; closing: `:alice` refused for Alice, the store's view beside it; group as Bob; `:pending :refused` | KD2 KD3 KD4 KD6 KD7 KD8 KD9 KD10 KD11 KD12 KD20 |
| B3 | approximated (the paused micro topology …); as said | 5 of 5: `o0`, `o2`, `crossing:o2`, `o3`, `landing:o2` yes | group at F=12 twice (paused, no case fact), then as Alice at F=14: the landing's copy v1; `:crossed :crossed :done :done` | KD2 KD3 KD4 KD6 KD7 KD8 KD9 KD11 KD12 KD13 KD17 KD20 |
| B4 | approximated (the paused micro topology …); as said | 4 of 4: `o0`, `o2`, `crossing:o2`, `landing:o2` yes | read 1 as B3's; then `:alice` refused for Alice, the store's view beside it, and group as Bob at F=14: the copy v1; `:crossed :done :done` | KD2 KD3 KD4 KD6 KD7 KD8 KD9 KD10 KD11 KD12 KD13 KD20 |
| D1 | approximated (prepare and commit are one batch …); as said | 2 of 2: `o0` KD1, `o2` yes | `:alice-hand` as Alice: the revoke; group at F=14, no case fact | KD1 KD2 KD6 KD7 KD8 KD9 KD11 KD12 KD20 |
| D2 | as said | 2 of 2: `o0` yes, `o1` KD1 | group as Alice at F=17: the revoke, a control fact holding the mapped pid | KD1 KD2 KD6 KD7 KD8 KD9 KD11 KD12 KD20 |

Every played case's `:values` and `:shown` equal the model's: A1's note and
mention erased; A2 erased; A3, A4, A5 open; A6, A7 erased; A8 open; B1 and
B2 the note erased; B3 and B4 the note erased and the group's copy open;
D1 and D2 missing. KD10's attributions: A1, A3 and A6 by the entry's lease
refused `:person-forgotten`; B2 and B4 by the entry sealed under a lock
leased before the forget (Alice had read before she was forgotten, so her
door's pool held locks), `:no-such-lock` on its face, F3's third rule.
KD6 now shows in A2 to A7 and D2, whose group reads make entries (pass 2
printed it "predicted, not seen" there). KD17's `:how` was `:row-deleted`
in B1 and B3.

### Every DIFFERS line, with its cause

The rig's, under baseline: **none**. The model's own lines under the
configurations other than baseline are pass 2's, each with the rig's
played outcome beside it: A1 under `baseline-but-not-owner-required`, A8
under `baseline-with-a-read-as-owner-and-an-other`, A2 and A6 under
`baseline-with-the-third-reading-of-a`, D1 under
`baseline-but-not-permissions-in-their-layer`; the rig implements baseline,
so it differs from each where the model's baseline does. Under
`baseline-but-not-p6-line-at-the-read-out` all four B lines agree, with P6
objecting to B3's and B4's copies, which the rig's line calls the model's
own property. None fails the test.

### Findings about the rig

None new. What pass 3 is the first to show, each as the plans said:

1. A landed copy reads open through the shared exit, for a member and for
   a member other than the requester after the requester is forgotten (B3,
   B4): block 2b opens a landing with its box's lock and block 2d indexes
   the rows it makes, on the merged tree as on the plans.
2. A promotion's read-out held at `:before-read-out` resumes from the
   door's resend, also after the requester is forgotten (B2), and refuses
   `:source-erased` as the model does.
3. A forgotten requester's bare landing lease still lands her copy (B4):
   F5's prediction, from PR9, holds.
4. A control fact in a group (D2's revoke) reads through the shared exit.

### New known differences

None. KD10 in A4 prints "predicted, not seen" every run: the plan's case
table predicted a refusal there before its fix F4 gave the group a live
reader, Bob. It is left as the plan's data, printed, failing nothing.

### For RIG.md (pass 3)

Rig choices, each changeable without touching a record (the orchestrator
assigns R numbers):

- **P8-11.** The replay sends `promote-client/promote!`'s two steps itself
  (`lease-landing!`, then `request-offer` and the door's send), with the
  functions and order `promote!` uses, so a hold or the pause sits between
  them; KD3 is counted only when the lease's `:for` is the request's
  landing, and every ack's crossing is checked against the crossing's
  record.
- **P8-12.** Group reads go through the one exit at `{:frontier F}`, F the
  frontier just before the read; an answered moment must cover every batch
  the replay waited past. The group is also read by pattern below the exit
  (F2) and by key (F1), like a one-owner layer, and each request's target
  is read at every read.
- **P8-13.** The person forget stays phase 2's act: phase 5's rest's wrapper
  (`read-exit/forget-person!`, the act and then the micro index purge) is
  not exercised, since its pages wait on the micro topology, which B4 holds
  paused; the shared read opens through `$$persons` either way.
- **P8-14.** Step 6b's guard: a case writing a `:mention` into the group is
  refused, failing the test, once the micro gate takes grammar facts, until
  the seed writes the group's grammar.

Found tonight: nothing new. Questions for Sid: none new; the run shows
For Sid 14 (KD1), 16 (KD2 to KD5) and 28 (KD10) case by case, as before.
First-record placeholders: none. The replays write no record form of
their own; the request's uuid and names are the door's, and the replay's
own names are test data in records nobody keeps.

### Not exercised yet

The fallback road and phase 4's `:before-forward` hold (KD5; TV6); the
person forget's micro purge (P8-13); a read of the re-classed base, which
no fixed history gives a case fact; standing reads, deltas and `[:kv]`
reads; step 6b.

### For Sid to look at, not to delete

`runs/phase8-replays.txt` (rewritten by every run) and
`runs/phase8-replays-pass3.txt` (this pass's copy) are the same bytes
tonight. Pass 3's plan above stays as the record of what was predicted.
