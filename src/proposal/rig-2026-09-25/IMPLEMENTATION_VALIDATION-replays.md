# Implementation validation: phase 8, the replays

Validated by Claude Opus 5.5 at max effort, 26 September 2026, in the build
session on branch `rig-build-replays`, against `PLAN-replays.md` (its
validation's fixes F1 to F12 applied) and SPEC.md phase 8. It follows the
rama skill's `phase-4-impl-validate.md`: default FAIL, each check stated,
the code cited (`L…` is `test/rig/replay_test.clj` as committed with this
file), a runtime trace, a verdict. The build step does the validation
itself, so every failure found here was fixed in the source before this
file was committed; each fix is named IV1, IV2, and so on.

**What was run, and what was only reasoned.** On this branch only phase 1,
the read exit and the revision reader exist; phases 2, 3 and 4 are not
merged. So the model side and the report were run (the lockstep against
`fm/run` for all fourteen cases, the report rendered with every case "not
practical"); every rig-side path below is traced by reading, against the
built code of phase 2 (`rig-build-locks` at `fd41f6d2`) and phase 3
(`rig-build-micro` at `e60c8ee1`) read with `git show`, and against the
plans for phase 4 and phase 5's rest. Nothing on the rig side has run yet.
That is the first thing pass 2 changes.

**Verdict: minor-fail, fixed in place** (IV1 to IV7). The adapter's shape
holds as planned. Every failure was a localized edit to one or two
functions. Two fixes were made before the first commit, in the implement
step's own self-check, and are not counted here: every case on the
fresh-module road gets the full seed, and each read also covers the
layers the rig wrote into.

## The template's module checks

The adapter is a test namespace. It declares no depot, PState, topology or
query, and writes nothing to the store except acts through the door, as a
person or the operator would send them.

- **Redundant conditionals, consecutive keypath, select-compute-transform,
  unnecessary nil->val, :allow-yield?, non-subindexed collections, stream
  topology idempotency, partial failure in stream topologies, application
  caches, reimplementation of built-in operations.** Not applicable: no
  dataflow code, no path into a PState of its own, no topology. Read and
  found nothing to check.
- **Single depot append per client operation.** Not applicable as written:
  the adapter appends only through the stages' own door functions
  (`stock!`, `offer-until-answered!`, `write!`, `forget-person!`,
  `forget-value!`, the micro `offer!`), whose append discipline is theirs.
  Its one pair of sends that is two appends by design is KD2: the lease act
  before a value act (L1062-1065), which is the lease road's contract
  (default 1), not one client operation split in two.

## Plan conformance, check by check

### V1. The lockstep ends where `fm/run` ends. PASS (run)

*Plan:* "steps the model one op at a time, with the model's own public
functions (`fm/init`, `fm/seed-permissions`, `fm/step`, `fm/drain`,
`fm/read-as-of`, `fm/now`, as `fm/run` composes them, so the end state is
`fm/run`'s)".

`lockstep` (L481-502) is `run` (model.clj L1127-1133) unfolded: `(fm/seed-
permissions (fm/init config))`, a `reduce` of `fm/step` over the history
recording each op's effects, `fm/drain`, then `(update :tick inc)` and a read
as of `(fm/now s)` conj'ed onto `:reads`, the same three forms `run` uses.
Run, no cluster, before the file was written: for all fourteen cases
`(= (ls-end baseline h) (fm/run baseline h))` printed `true`, and `play`
printed "as said" for each. The model-side test `lockstep-ends-where-run-ends`
asserts both for every case and every named configuration.

### V2. Effects read off the model's state. PASS after IV1 (traced)

*Plan:* sends are names new in `(:sent st)`; decisions are names that gained
a recorded answer, and for `[:prepare]` the decisions in `[:micro :prepared
:delta]` [F7]; person forgets are persons new in `(:persons st)`; reads are
reads new in `(:reads st)`.

`op-effects` (L454-479): `sent` is keys of `:sent` not in the before-state
(L462); `decided` is every name answered after and not before (L463-465,
`answered?` L402 over `fm/answers-for`); `prepared` reads
`[:micro :prepared :delta]` for `[:prepare]` only (L467-472), which the probe
showed holds D1's `o0` refused `:permission-from-another-layer` at parts
`[0]`; forgets L477; reads L478. Trace, B3's `[:work 0]` after the promote:
before, `o2` is sent and unanswered; after, `o2` answered at `[:stream 0]`
stamp 7 and `crossing:o2` at stamp 8; `decided` holds both, ordered by
`order-decisions` (L426-452) on one stream place by stamp: `o2`, then
`crossing:o2`. `landing:o2` is new in `:sent` and unanswered, so a send and
not a decision. Trace, D1: at `[:prepare]` `decided` is empty (the model
records nothing there) and `prepared` holds `o0`; at `[:commit]` `o0` gains
its answer and is in `decided`; `play-step!` (L1564-1578) finds it in
`:played` and calls `check-commit!` (L1303-1312), so it is checked and not
sent again [F7].

The plan's guard ("asserts, per op, that it never sees two decisions whose
order it cannot tell"): `order-decisions` returns `{:guard why}` for two
decisions on different places in one op outside a batch (the drain can do
it), and `guard-refusal` (L516-519) makes such a history not practical;
with its stages resolved, `judge-case` (L1635-1666) then fails it ("not
practical with its stages resolved"), so a later history that breaks the
rule is caught, not mis-ordered. The model-side test
`the-guard-catches-decisions-it-cannot-order` runs such a history.

### V3. Each rig act at the op that decides its counterpart. PASS (traced)

*Plan:* "the rig's counterpart of a model offer is sent at the model step
that decides it, not at the step that queued it."

The model is pure, so the lockstep is computed whole before any rig act
(`prepare-case` L1621-1633); `play-case!` (L1588-1617) then walks its steps
in order and `play-step!` plays only an op's decisions, forgets and reads
(L1564-1578). Sends are recorded and never played. Trace, B1:
`[:forget-value 0]` only sends `o3`; nothing happens on the rig. The first
`[:step 0]` decides `o2` alone; the rig plays the request. `[:read :now]`
reads: the rig's `o0` is still open, as the model's is. The second
`[:step 0]` decides `o3`; only now does the rig send `forget-value!`. The
failure the plan's design log names (the forget decided before the first
read) cannot happen.

### V4. The step kinds and their rows. PASS (traced)

*Plan:* the table of step kinds; "the adapter refuses a history containing a
step kind it has no row for, with the step named".

`step-rows` (L228-278) has one row per kind the fixed histories use, each
with the model's side, the rig's, and whether it is faithful, and a
`:covers?` predicate; `history-refusal` (L288-298) names the first op no row
covers, and names the six rowless kinds (`rowless-kinds` L280-284:
`:retry :reuse :set-grain :reclass :failover :zombie`) as "named, not
built". The `:offer` row covers `:times 1`, `:stood-on nil` and `:cite` nil
or `:session` only; `:promote` covers target `:group`, `:times 1`; `:read`
covers `:now`; so `[:read [:at 3]]` or an offer sent twice is outside its
row and refused, not played in part. The plays: `:offer` at its decision by
`play-value!` (L1045-1072); `[:forget-person p]` by `play-person-forget!`
(L1107-1125); `[:forget-value i]` by `play-forget!` (L1074-1089);
`[:promote …]` by `play-request!` (L1207-1246); `[:revoke pid]` by
`play-revoke!` (L1091-1105); `[:read :now]` and the closing read by
`play-read!` (L1468-1491); `[:work]`, `[:step]`, `[:batch]` by their
decisions; `[:prepare]`/`[:commit]` as V2 traced.

`effect-refusal` (L521-534) adds one semantic check: a value forget whose
target is in a shared layer has no rig row tonight (phase 2's
`forget-value!` goes to the stream gate). No fixed history does it (B1 and
B3 forget in `:alice`). It is not in the plan's table; it keeps such a
history from being played wrongly, so it is recorded as divergence D12.

### V5. Continuations held to the model's order. FAIL, fixed (IV1)

*Plan:* "When the model decides a continuation in a **later** op than the
part before it, the adapter holds the rig's continuation until that op";
"When the model decides both parts in the same op … nothing is held."

**IV1 (fixed).** The first commit held a continuation whenever it was *not
decided in this op*. Trace, B1: the third `[:step 0]` decides
`crossing:o2`, refused `:source-erased`, and the model never decides
`landing:o2`. `play-crossing!` found the landing not decided in that op and
paused the micro topology before the release and resend: a pause for a
landing that never comes, held to the end of the case, and the case's line
marked "the paused micro topology stands for the model's queued landing",
an approximation it never used. The same in B2. Fixed: `decided-later?`
(L1166-1172) asks whether the model decides the continuation in an op after
this one, over the lockstep's steps kept in the play record
(`play-case!` L1595); `play-request!` (L1238-1239) and `play-crossing!`
(L1268) use it. Traces after the fix: B1 and B2, the request's op holds at
`:before-read-out` (the crossing is decided later) and the crossing's op
holds nothing (no landing is ever decided); B3 and B4, the request's op
decides the crossing too, so no read-out hold, and pauses the micro topology
(the landing is decided at the later `[:batch]`), after the landing lease
was answered and before `promote!` (L1230-1244), as the plan orders; the
landing's op resumes (L1289) and waits for the landing's answer and the
frontier past its batch (L1294-1298).

The pause's fallback (`hold-landing!` L1191-1203): if pausing throws, the
`:before-forward` hold, released at the landing's op with the door's resend
(L1290-1293), which is where KD5 shows. The plan names this fallback.

### V6. `:values` from the store's own opening, found from the rig's records. PASS (traced)

*Plan [F1]:* "Which rig fact answers a ref `[layer k persons]` is found from
the rig's own records, never through the model's fact ids … matched on
layer, key and the persons of the value as the replay sent it. 'Missing'
means no act the rig admitted carries such a fact. Where a pattern read of
the layer exists … a rig fact matching the ref that the adapter does not
know of is a difference."

`rig-values!` (L1502-1537): candidates are the facts of every act in
`:acts` answered yes in the ref's layer with its key and persons (L1515-
1520), the acts the replay sent and the landing the store made
(`play-landing!` records it with the source's value, L1300-1301); none is
`:missing`, whatever the model did. Trace, D2 with a wrongly admitted write:
the act is in `:acts` with a yes, so the ref `[:group :note nil]` finds it
and opens it, `:open`, where the model says `:missing`: a `seen` difference
that fails the test. The opening is the store's: `opens?` for a stream-side
fact, phase 3's frontier read (`open-act`) for a group row (`open-rig-fact`
L1493-1500); an opened value must equal the value sent (L1532-1534). A
one-owner layer is also read by pattern `[:k k]` below the exit
(L1521-1526), and a matching row the replay did not make is a difference.
Two rig facts for one ref is a difference too (L1527-1528).

One limit, stated: an *erased* row carries no value, so the pattern check
cannot match its persons; an unknown erased mention is not flagged by this
check (it is still seen by the reads' pattern check, V7, as a fact the model
lacks).

### V7. A reader's view, and every fact the rig holds. PASS after IV5 (traced)

*Plan:* reads through the exit as a live reader; [F2] one-owner layers also
by pattern below the exit, and every rig name sent into the group through F;
[F4] the group as Alice, else Bob in `:bob-hand`; KD10 printed beside the
store's judgment.

`play-read!` (L1468-1491) reads every layer the model's case facts are in,
and every layer the rig wrote into (L1476-1478), so an extra fact where the
model has none is still read. `read-layer!` (L1407-1447): a one-owner layer
by its owner through the exit, point reads of the counterparts' fids
(L1419-1424), then a pattern read `[:all]` below the exit whose rows that are
neither counterparts nor rig-only kinds are differences (L1425-1428,
`extra-rows` L1363-1368, `rig-only-keys` L929-934); the group through the
exit once phase 5's rest resolves (`shared-read?` L1336), as a live member
(`live-member` L1356-1361), else through phase 3's frontier, every group act
the rig admitted opened by `open-act` (L1439-1440, `open-act-rows`
L1396-1405), and any row that is not a counterpart is a difference
(L1441-1446). `compare-read!` (L1449-1466) judges each model fact against
its row (`judge-fact` L895-927).

**IV5 (fixed).** With the shared read resolved and both members forgotten
(A3, A6), the first commit had no reader: an empty read, and every model
fact reported "no rig row", unexplained. The plan predicts "Closing read:
refused for Alice, forgotten (KD10)". Fixed: the reader is a live member,
else Alice (L1433-1434), whose refusal KD10's rule then judges.

### V8. The attributions are exact. PASS (traced)

- **KD1** (`judge-answer` L828-854): matches only a model no with reason R
  against a lease no with the same R and a value act refused
  `:no-such-lock` on its face, or [F8] no value act. A lease refused for
  another reason, or a value act refused otherwise, is "its lease was
  refused …", unexplained. Trace, D1 on phase 3's door: `write!` leases
  citing the act's permission `[:alice :group :alice-hand]`, refused
  `:permission-from-another-layer`; the micro door then seals under no lock
  (`seal` returns nil blobs, micro_client.clj L182-198) and offers; if the
  gate's answer is not `:no-such-lock`, the line says so and the test fails,
  a finding about the merged door rather than a hidden one.
- **[F11]** every lease's answer is judged with its act (L977-990): a
  refused lease outside KD1 is a difference in itself; a refused landing
  lease (L1237), a refused person forget (L1119-1120) and a refused seed
  act (L1548-1562, which stops the case) are differences; an entry refused
  outside KD10 is one (L1385-1391).
- **KD10** (`judge-exit-refusal` L856-880): only for a reader whose
  `forget-person!` the rig answered yes, refused `:person-forgotten`, or
  `:no-such-lock` on the entry's face with the entry's lease refused
  `:person-forgotten` or leased before the forget; the lease comes from the
  depot (`entry-lease` L1345-1354: the entry's record by name, its lock ids,
  the lease's record by name). Trace, A1's closing read: Alice's exit read of
  `:alice` makes an entry in `:alice-hand`; phase 2's door leases for it
  (her pool there is empty), the lease is refused `:person-forgotten` (L11
  revised), the door cites the ids it would have minted, the entry is
  refused `:no-such-lock` on its face; the depot holds the sealed entry,
  whose lock ids name the refused lease; KD10 explains it, and the store's
  view (a point read below the exit) is compared beside it.
- **KD20** (`control-refs` L882-893, `judge-fact`): a control fact must be
  there and hold every model reference mapped through the correspondence
  (request: source fid and target; crossing: request name and source fid;
  forget: target fid; revoke: the rig's pid), found anywhere in its value,
  so the rig's form does not matter and a wrong reference does.
- **F6** (`reduce-answers` L387-400): all yes, or all no with one reason,
  to one answer; otherwise `:split`, which `judge-answer` reports as the
  model's own finding, unexplained.

### V9. Known differences reported every run, predictions checked. FAIL, fixed (IV2, IV3)

*Plan:* each KD "appears in the lines of the cases it touches, by number";
"A predicted difference that does not show is printed as 'predicted, not
seen'".

The table (L118-204) is data, each entry citing its cases or saying why no
fixed history exercises it; the header prints all twenty every run
(`report-lines` L1800-1806); a case's line prints the KDs that showed and
the predicted ones that did not (`rig-lines` L1732-1733; `judge-case`
L1657).

**IV2 (fixed).** KD7, KD9, KD11 and KD12 are predicted for every case, and
nothing in the first commit marked them seen, so every played case would
have printed them "predicted, not seen": a prediction printed as refuted
when it held. Fixed: KD7 when the seed makes a person (L1559) and at every
person forget; KD9 whenever a write, revoke or request cites a mapped
permission (L1060, L1103, L1236); KD11 at every read (L1485); KD12 whenever
a rig answer carries a stamp (L989).

**IV3 (fixed).** KD6 was marked only when an exit read returned rows; a
refused entry is an entry act too. Fixed at L1386.

### V10. The world and the permissions. PASS (traced)

`world` (L317-346) is the plan's table: persons made by acts; Alice's three
layers and Bob's rig-only `:bob-hand` [F4]; every model permission as the
rig's id (`rig-pid` L348-359: `[p l l]` for p's own layer, `[p l l root]`
beneath the group's or the base's root, `[p l :alice-hand]` for a session
permission); the base and the group as phase 3's `shared-world` has them; a
session per writer; each person's working layer and permission there. The
model-side test `the-world-grants-every-model-permission` checks that every
pid in `fm/permissions` maps to a grant in the rig's seed, on both roads.
`door-road` (L361-367) is `:direct`: phase 2's and phase 3's doors, as
built, take a permission held by the act's `:who` or its `:session`
(`rig.store.permit/refusal`), so the plan's direct road is the literal
mapping and the line says so.

### V11. The APIs by stage, and "not practical (the missing names)". PASS (run)

`apis` (L561-632) lists every rig API the adapter calls with its stage, the
var it binds to, `:built` or `:plan`, and the cases that need it;
`resolve-api` (L645-660) finds each with `requiring-resolve`: `:missing` for
a namespace or var not merged yet, `:broken` for a namespace whose file is
on the classpath and does not load (`ns-file-present?` L638-643), which
fails the test rather than hiding a broken build as "not practical". The
grammar entry carries a check of its value. `case-missing` (L667-674) gives
each case its missing names, `missing-str` (L676-681) groups them by stage.
Run on this branch (the dry render): every case not practical, each line
naming its stage 2 and stage 3 names (and stage 4's for B1 to B4).

### V12. Waits bounded; never a hung suite. FAIL, fixed (IV4)

*Plan:* "Waits are bounded: an answer, the frontier passing a batch, a
fan-out, each at most 60 s, polled at 50 ms … A timeout is that case's
'differs: no answer within 60 s', never a hung suite."

`wait-for` (L715-727) polls at 50 ms for at most 60 s and throws
`::timeout`, caught per case (L1607-1613). The micro door waits 60 s on its
own (`await-answer`).

**IV4 (fixed).** The stream door's `offer!` blocks on an acked append and
`offer-until-answered!` resends up to 60 times after an error; nothing in
the first commit bounded a call that never returns (a hold that blocks, a
gate stuck after the merge), so one stuck call could hold the suite for
many minutes. Fixed: every rig call runs through `bounded-call` (L1009-
1021) with a bound of 120 s (`call-ms` L1002-1007), two of the waits'
60 s because a door call may itself wait 60 s for a micro answer after
leasing; the below-exit queries (L1338-1343) and each seed step (L1555) are
bounded the same way. A call that does not return marks the case, and
`run-replays!` plays no later case (L1866-1883): a stale call might
otherwise reach the next case's module, which has the same name. That
case and every case not run fail the test.

### V13. A fresh module per case, and the fallback road. PASS (traced; the road check run)

`road-check` (L735-757) launches, destroys and launches again, timed: the
fallback when the second launch fails or passes 15 s, or when
`RIG_REPLAY_ROAD=one-module` forces it (so a pass can exercise it). On the
fresh road each case launches and destroys its module (L1599, L1616);
`inject/reset-all!` starts each case (L1598) and `cleanup!` (L1580-1586)
releases every hold and resumes the micro topology whatever happened. The
fallback (`fallback-names` L307-315) renames persons, one-owner layers, the
group and the sessions with the case's id and keeps one base; the first
case makes the store layer and the base, later ones grant their persons'
base permissions through the gate that orders the base by then (`grant!`
L786-792, `seed-steps` L794-816).

### V14. The report. FAIL, fixed (IV6, IV7) (run, dry)

*Plan:* header with date and time (IST), the rig's and the model's commits,
the task count, the run time, the stages that resolved, F10's three; per
case set and configuration, `scenarios/report`'s own lines, each followed by
the rig's; the detail lines once per case under baseline; the B objections
"the model's property, which the rig does not evaluate"; written in a
`finally`.

`report-lines` (L1768-1831) and `write-report!` (L1835-1844), called in the
deftest's `finally` (L1897-1898). The model's lines are
`scenarios/report`'s output for the one case and configuration
(`model-lines` L538-542); the rig's line under baseline (`rig-lines`
L1703-1741) leads with "as said", "differs", "approximated (why)" or "not
practical (the missing names)", then answers, reads, values and shown,
rig-only acts, KDs seen and not seen, roads, and every difference with its
KD or as DIFFERS; under another configuration (`other-config-line`
L1746-1757) the rig's baseline outcome against that configuration's.

**IV6 (fixed).** The rig's line never said that the model's objections are
the model's own properties, which the plan asks for B3 and B4 under
`baseline-but-not-p6-line-at-the-read-out`. Fixed: wherever the model's line
carries objections, the rig's line says so (`objections-note` L1743-1744).

**IV7 (fixed).** The header's stage 4 read "2 of 7 resolved", counting
`env/crossing-name` and `env/landing-name`, which phase 1 built; it read as
if phase 4 were partly merged. Filed under stage 1 with the plan's stage
noted; the line now reads "stage 4: 0 of 5 resolved".

### V15. The verdict rules. PASS (traced)

`judge-case` (L1635-1666) fails a played case on a baseline `seen` that
differs from the model's, on any difference no KD explains, and on a status
worse than predicted (`worse-than-predicted?` L938-943); a case not
practical fails only for a broken namespace or a refusal with its stages
resolved. The deftest (L1890-1907) also fails if the module did not launch.
The model-side test `the-verdict-rules` runs each rule on made records.

## Divergences from the plan that stand

- **D1. The stream door's lease is `stock!`.** The value act is then sealed
  from the door's pool. Phase 2's `offer!` would lease inside, where its
  answer is not seen; F11 needs every lease's answer. Same door, same lease
  act.
- **D2. One set of sessions for every case.** `:alice-session` in `:alice`
  and `:group`, `:bob-session` in `:group`: the union of the layers the
  fixed histories write, where the world table says "in each layer the
  history writes" and the seed row says one seed for every case. Rig-only,
  never compared.
- **D3. Alice's root permissions are grants after the making act** (phase
  2's built seed), where the world table puts the root in the making act
  (P10). Rig-only.
- **D4. Rig calls bounded at 120 s**, not 60 (IV4); the polling waits are
  60 s at 50 ms as planned.
- **D5. The store's view beside a refused exit read is compared** with the
  model's read, not only printed: more is reported, nothing hidden.
- **D6. Reads cover the layers the rig wrote into** as well as the model's.
- **D7. The road check runs once before the cases** (launch, destroy,
  launch), the plan's rule measured up front rather than at case two.
- **D8. KD5 is not predicted** for B3 and B4: it shows only on the
  `:before-forward` fallback, which the plan says.
- **D9. A group read with no live member reads as Alice** (IV5).
- **D10. An error in a case is 'differs: error …'** with its class, message
  and first frame; the plan names only the timeout.
- **D11. `opens?` is phase 2's**, which builder A asked the merge to keep
  only as an internal body or test helper; if the merge removes it, the
  stream-side cases say "not practical (rig.store.client/opens? missing)"
  until it is rebound (a binding to confirm).
- **D12. A value forget in a shared layer has no row tonight**
  (`effect-refusal`); no fixed history has one.

## What pass 1 cannot show, carried to passes 2 and 3

- Every rig-side path above is traced, not run. The first merged run is the
  first evidence.
- Phase 4's calls are assumed shapes (`call-lease-landing!`,
  `call-promote!`, `call-promotion-status`, `hold!`, `release!`); the
  resend of a held request assumes `promote!` rebuilds the same map from a
  given name and claimed-when.
- The rest of phase 5 is detected by `rig.store.shared-reads/moment`; if its
  build names the namespace otherwise, the group is read through phase 3's
  frontier and the line says so.
- The micro door after a refused lease (D1's KD1, V8).
- The rig's request and crossing acts may carry more facts than the model's
  one; any that is not a rig-only kind will be reported as a fact the model
  lacks, and then either named as a known difference or fixed.

## Self-consistency

Read back after writing. Each check that found a failure says FAIL and
names its fix; V6 states one limit of its extra check and says that the
reads' check covers it, so V6 passes on its requirement (F1's lookup from
the rig's records), not on the extra. The traces of rig-side paths are
reasoned against built code and plans, and say so at the top. Every fix is
a localized edit; none changes the plan's architecture (the lockstep, the
two observations, the holds and the pause, the fresh module per case, the
known differences as data). Hence minor-fail, fixed in place.

PHASE_VALIDATION:minor-fail

## Pass 3, after wave 2 (the rama skill's phase 4, adversarial)

Claude Opus 5.5 at max effort, 26 September 2026 from 08:05 IST, on
`rig-build-replays-3` after `git merge rig-2026-09-25` at `27543fd7` (wave
2 landed; merge `ab36a274`), against the pass 3 plan
(BUILD_NOTES-replays.md, "Pass 3 plan, after wave 2 (prep)") and the
merged code. The default verdict is major-fail; each check below is traced
through the code at `b91ce2f4` (`test/rig/replay_test.clj`, line numbers
there) and the store code it calls. Rig-side behaviour is traced against the
merged source and phase 4's and 5's own tests, not yet run in this
namespace; the run follows.

**Before the build: the merged code against the branches read at the prep.**
`promote_client.clj`, `promote_flow.clj`, `promote.clj`, `promote_shape.clj`,
`inject.clj` and `read_exit.clj` are byte for byte the branches'
(`git diff` quiet). `envelope.clj` differs from phase 4's by phase 6's
`:grammar` control key; `shared_reads.clj` gains W2-3's stream-era grammar
functions after L1270, none of `moment`, `visible?`, `shared-answer`,
`open-entry>` or `index-writes`; `micro_client.clj` is unchanged since
`c9684356`. The landing's copy reaches the shared index: block 2b's
`row-wraps` opens a landing with its box's lock (micro.clj L1138-1170,
`delivered-locks` L425-441), `fact-rows` makes its rows with their lock rows,
and block 2d's `index-block>` takes the same `*frows` (L1636-1639); wave 2's
merge touched only `micro.clj`'s requires (BUILD_NOTES-wave2.md L95-107).
W2-2 already writes the model's grammars into each one-owner layer in the
seed (L840-845) and made `:grammar` a rig-only key: pass 3 builds on it and
adds no second write (builder C's call).

### P3V1. The API table (P3-2). PASS (traced; resolved dry)

`apis` (L559-651): stage 4 bound to `rig.store.promote-client` (`connect`,
`lease-landing!`, `request-offer`, `resend!`, `status-of`), `inject/hold!` and
`release!` built; `env/uuid7` (stage 1) and `client/raw-row` (stage 2) added
for the B cases; `env/make-name` gone; `micro/control-keys` added, optional,
for P3-5's guard; `shared/moment` marked built. Resolved with no cluster
(`resolve-apis`, then `prepare-case` for every case): every API resolves,
all fourteen cases practical, `micro/control-keys` without `:grammar`, the
toy grammars two facts.

### P3V2. The request as `promote!` sends it (P3-3). PASS (traced)

`play-request!` (L1256-1317), B3's request: u by `env/uuid7`; `cls` by
`micro-client/settings-of` of `:group`, `:by-entity`, as `promote!` reads it
(promote_client.clj L130); the lease into `:group` by Alice in
`:alice-session` citing `[:alice :group :group [:group :group :group]]`
(`cited-pid` of the model's landing permission); `(:answer ll)` read as the
answer (the map pass 1 took for the answer is the lease's record). No
`:public`, or a refusal, is a difference and the request gets no counterpart,
as the door would send none (P4-11). Then `request-offer` with the source's
stamp by `client/record` and entity by `client/raw-row`, exactly
`promote!`'s L136-137, `:replaces` mapped (nil here), u; KD3 marked only when
`:for` equals `(env/landing-name (:name o) :group :by-entity)`, both `[:group
:by-entity :landing u]` (promote_shape.clj L113-133). The hold or the pause
is armed after the offer is built and before the door's send; the hold is
on `(:name o)`, the name `continue>` checks (promote_flow.clj L147-151). The
request's record keeps the offer and every ack.

### P3V3. The resend, the crossing and D2's check (P3-3). PASS (traced)

B1's third `[:step 0]`: `play-crossing!` (L1330-1353) finds the hold, releases
it, and `resend-request!` (L1319-1328) sends the kept offer by
`promote-client/resend!`, which is `offer-until-answered!` of the same map
(same digest, so answered from the record; `continues?` is true for a yes
from the record, promote.clj L44-48); the resend's ack joins the request's
acks. The crossing's record by `client/record` of `env/crossing-name`;
`crossing-ack-says` (L1203-1210) compares each ack's `:crossing` with it by
answer and reason: the first ack carries none (held), the resend's carries
the decision's own ack, whose record `write-decided>` wrote
(promote_flow.clj L91-103), so they agree on a correct rig and differ only
on a real split. B3: one ack, carrying the crossing from the request's own
event.

### P3V4. The landing (P3-3). PASS (traced)

`play-landing!` (L1355-1384), B4 at `[:batch]`: the landing's name with the
class the request carries, `[:group :by-entity :landing u]`; the topology
resumed; `micro-client/lookup` by that name answers only once the frontier
passes its batch (micro.clj `lookup-result`, L1176-1190); `wait-frontier!`
(L1067-1075) keeps the batch as the largest waited past; KD13. A model yes
and a rig no is judged unexplained by `record-act!` and names its F9
condition in a note (`f9-condition`, L1192-1201).

### P3V5. The group read at `{:frontier F}` (P3-4). PASS (traced)

A4's closing read: `read-layer!` (L1512-1583) takes the group branch; the
shared read resolves; the reader is Bob (`group-reader`, L1506-1510); F is
`micro-client/frontier`, task 0's frontier; `exit-read!` (L1465-1504) asks
`read!` with `:as-of {:frontier F}`, which `check-call` accepts (read_exit.clj
L66-75) and `read-point` hands to `shared-read-point` (reads.clj L1511);
the answer's moment is checked by `moment-says` (L1452-1463) against the
largest batch waited past. A refused read (A3, A6) is judged by KD10's
rule with the store's view below the exit at the same moment. The pattern
read below the exit (`[:all]`, the same moment, the same reader) adds F2's
extras, merged by fact id with those of the point read. With no case fact
and no replay act in the group (B1, B2, D1; B3 and B4 before the landing)
only the pattern read runs, and no entry is written.

### P3V6. Reads cover each request's target, and F1 on the group (P3-4). PASS (traced)

`play-read!` (L1585-1606) adds `(keep :model-target (:requests @p))` to the
layers read, so B1's three reads each read the group. `rig-values!`
(L1621-1662) reads the group by `[:k k]` below the exit as its reader at
`{:frontier F}` once the shared read resolves. **A divergence, D-P3-1**:
`rig-values!` now reports a refused pattern read as a difference, as
`read-layer!` already did; before, it passed over the refusal silently,
which could hide a stream-side fact F1 is meant to find. It applies to the
one-owner layers too.

### P3V7. The world and the guard (P3-5). PASS (traced; the guard's test pure)

The grammar facts are W2-2's (L840-845), before any case act in each
one-owner layer; KD8's rule (L153-156) names them; `rig-only-keys`
(L965-972) holds `:grammar` and the standing reads' three keys.
`world-refusal` (L1765-1775) refuses a case whose history offers a
`:mention` into `:group` when `:grammar` is among the micro gate's control
keys, through `prepare-case` (L1777-1790), which fails a case not practical
with its stages resolved. Tonight the micro keys lack `:grammar`, so
nothing is refused (checked dry above). **D-P3-2**: W2-2 writes the grammars
into `:bob-hand` too, which the plan left out; harmless (no value is
written there) and kept, as builder C asked.

### P3V8. The report (P3-6). PASS (traced)

`read-str` (L1846-1861) prints a group read's moment (`at F=…`); the roads
line (L1892-1901) names the group read at `{:frontier F}` and, for a case
with requests, the promotion's two steps; the stages line has stage 6
(`stage-order`, L656).

### P3V9. Nothing else moved. PASS (traced)

`judge-answer`, `judge-exit-refusal`, `judge-fact`, the lockstep, the
holds' rule (`decided-later?`), the road (a cluster per case, R33), the
seed and the verdict rules are unchanged; `cleanup!` still resumes a paused
topology and releases holds in `play-case!`'s `finally`.

### Self-consistency (pass 3)

Nothing above calls a check's handling a gap. D-P3-1 widens an existing
rule to the one place it was missing; D-P3-2 is wave 2's, kept on the
coordinator's word. No failure was found, so nothing was fixed.

PHASE_VALIDATION:pass
