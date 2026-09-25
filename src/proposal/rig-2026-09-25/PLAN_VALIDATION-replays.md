# Plan validation — phase 8, "replays"

Validated by Claude Opus 5.5 at max effort, in a fresh session on branch
`rig-plan-replays`, 26 September 2026 from 03:36 IST, against
`PLAN-replays.md` at `946a6bc6`, in the shape of the rama skill's phase 2
(`references/phase-2-plan-validate.md`): default FAIL, the requirement
quoted verbatim, a concrete case traced for each check, the plan's lines
cited ("L…" below is PLAN-replays.md at `946a6bc6`), a self-consistency
check last. The fixes are applied in place in PLAN-replays.md, each marked
`[F1]` … `[F12]`.

**Verdict: minor-fail.** The plan's architecture holds: the lockstep, the
two observations, the holds and the pause, a fresh module per case, known
differences as data with unknown ones failing. Every failure below is a
localized edit, and each is made. Two of them matter most: the rig's side of
`:values` could agree with the model by construction for a missing value
(F1), and reads that compare only the model's own fact ids cannot see a fact
the rig made and the model did not (F2).

## Sources, read by this session

- The requirement, SPEC.md L118-120 (main rig worktree): "Where practical,
  replay the model's fixed histories from scenarios.clj through the rig and
  compare answers and reads with the model's. Report every difference; a
  difference is a finding, not something to hide."
- `scenarios.clj` in full (128 lines); `model.clj` L142-180, L400,
  L730-756, L1127-1133; `run.clj` L232-256.
- `runs/phase8-model-traces.txt` (the planner's run of the model): every
  case's answers and persons; B1, B3, B4 and D1 in full.
  `runs/phase8-model-report.txt` L49-70.
- The validated phase 4 plan, `PLAN-promotion.md` in the main rig worktree
  (last changed at `2942e888`, "the validation's fixes F1 to F9 applied"):
  L758-770, L890-920 (PR8 to PR13 and F9), L968-985 (holding a step),
  L1008-1035 (T1 to T4).
- `PLAN-locks-and-forgetting.md` L738-746 (L11 revised), L1712-1728 (the
  lease table), L1966-1974 (KD1's source); `PLAN-micro-store.md` L125-135,
  L225-233, L524-538; `PLAN-read-exit.md` L797-816, L322, L712.
- The builds in flight, collected by one read-only gatherer (Sonnet) and
  spot-checked here where a check turns on them: locks `cc506303`, micro
  `48562bde`, reads `aaee646b`. This session read `opens?` (locks
  `client.clj` L375-381), `open-row` (micro `micro_client.clj` L304-313),
  `persons-to-check` (micro `micro.clj` L439-452) and `rig.claims/hold!`
  (locks `src/rig/claims.clj` L34-38) itself.
- One probe, under the cluster lock: `runs/probe-replays-2026-09-26.clj`,
  output `runs/probe-replays-2026-09-26.txt`.

## Template checks that do not apply to a test adapter

- Query topology reads: not applicable, the adapter declares no query.
- PState schemas: not applicable, it declares no PState (L582).
- Partitioning, and the partitioning-efficiency table: not applicable, no
  read or write path of its own (L588-589).
- Topologies (microbatch unless justified; stream concerns): not
  applicable, it declares no topology; it pauses the micro one (checked
  under C4 below).
- Production readiness (concurrent clients, restarts, scale): not
  applicable to a test run in one JVM; its fault behaviour is its bounded
  waits and its `finally` (checked under C6).
- Internal depots, cross-topology and stream correctness, commit
  boundaries: not applicable, it appends only through the door.
- In-memory state efficiency: not applicable, its state is test-local data
  (L590-592).
- Minimality and throughput of a module: not applicable as module checks.
  Minimality of the adapter was attacked instead: the one mechanism a
  simpler plan would drop is the lockstep, and C2 shows dropping it decides
  B1's forget before the first read, so it stays.

## C1. Every case covered, and each predicted status honest

**Requirement:** "replay the model's fixed histories from scenarios.clj"
and "Where practical".

scenarios.clj has 8 A, 4 B and 2 D cases (L25-89). The plan lists all
fourteen (L248-288) and every configuration `run.clj` names for them
(L232-256 there; plan L259-266, L277-281, L287, L421-426): checked
against the model's report L14-65, where the DIFFERS lines are exactly A1
under `baseline-but-not-owner-required`, A8 under
`baseline-with-a-read-as-owner-and-an-other`, A2 and A6 under
`baseline-with-the-third-reading-of-a`, D1 under
`baseline-but-not-permissions-in-their-layer`. **Pass** on coverage.

Statuses, case by case against the model's own trace:

**B1** (traces L117-146). Model: o0 yes at stamp 2 (settle's `[:work 0]`);
`[:promote 0 :group 1]` sends o2; `[:forget-value 0]` sends o3 (target
`o0#0`); first `[:step 0]` decides o2 yes at 8 and queues `crossing:o2`;
read as of 8: `o0#0` open v1, o2 pending; second `[:step 0]` decides o3
yes at 10; read as of 10: `o0#0` erased@10, o2 pending; third `[:step 0]`
decides `crossing:o2` no `:source-erased` at 12; drain decides nothing new;
closing read: refused. Rig (L272): at settle's `[:work 0]` the door's
lease act and value act into `:alice`, yes; at the first `[:step 0]` the
lockstep sees o2 decided and `crossing:o2` not, so it arms the hold at
`:before-read-out` before `promote!` (knowable from the op alone, L131-134);
`lease-landing!` in `:group` (a micro act, waited for); `promote!` → the
request's yes, processing ends at the hold; the read (status pending); at
the second `[:step 0]` `forget-value!` of the mapped `o0#0`, on `:alice`'s
home; the read; at the third, `release!` and the door's resend, answered
from the record, whose read-out finds the source erased. Predicted
values and shown as said. This depends on the hold being P4's kind, where
"the record's processing ends there with the request's answer" (P4
L974-977): a blocking hold on the stream task would hold up the forget on
the same task until timeout. The builds define a `hold!` today only in
`rig.claims` (locks `src/rig/claims.clj` L34-35: "Block this task thread
until the tag's gate is delivered"), phase 0's throwaway, which is that
blocking kind. The plan names `inject/hold!` from P4 (L547), which is
right, but does not warn against the one that exists. Fixed in F8. The
status "approximated" is honest, and one thing it hides is not said: in
the rig the read-out is in the request's own event (P4 PR7), so B1's order
exists in the rig only on the recovery road after a crash, which is what
the hold plus resend plays. The replay therefore tests that road, not the
normal one. Fixed in F10.

**B3** (traces L172-208). Model: `[:work 0]` decides o2 yes at 7 and
`crossing:o2` yes at 8, and sends `landing:o2` to the micro inbox; read
crossed; o3 decided at the next `[:work 0]` (10); read crossed;
`[:batch]` decides `landing:o2` yes at 12, recorded at **two** micro
partitions, `[[:micro 0] :yes nil] [[:micro 2] :yes nil]` (L176); read
done; closing done. Rig (L274): the landing lease answered, the micro
topology paused, `promote!` (request and read-out as one record, the
forward's append waiting in `*micro-offers`); reads; `forget-value!`; at
`[:batch]` resume and wait for the landing and the frontier. The pause is
if anything more faithful than "approximated" suggests: the rig's forward
really is a depot record waiting for the next batch; the pause only fixes
a real race (the landing's batch against the forget) to the model's order.
Honest. **Fail found:** the plan compares answers "name by name" (L461)
but gives no rule for a model name with two answers at two partitions
against the rig's one answer. A naive build either reports a false
difference or takes the first answer, which would hide a model name whose
partitions disagreed. Fixed in F6.

**B4** (traces L209-240). Model: as B3 to the first read; Alice forgotten
at stamp 9; `[:batch]` decides `landing:o2` yes at 10 (two partitions);
reads as of 10: `o0#0` erased@9, `landing:o2#0` in `:group` v1, o2 done.
The plan (L275) predicts "at risk": "the landing lease is Alice's, and a
lease row is sealed under its act's writer (For Sid 4)", citing P4 at
`d6712941`, the version then being validated. **Fail found:** the
validated P4 settles it the other way. PR9 (L892-895): "landing leases are
bare, bound to one landing"; L765-766: "Bare, because the copy must not die
with its former owner (B case 4)". F9 (L915-920) names what the build
confirms: the requester's person forget neither closes her session in T
nor deletes her lease rows there. T4 (L1025-1031) asserts her landing
lease row is still there after her forget and before the batch. PR13, a
value act's `:who` not checked against `$$persons`, is already true in
phase 3's build: `persons-to-check` (micro `micro.clj` L439-452) checks a
value act's subject union and a lease act's `:who`, never a value act's
`:who`. Phase 2's fan-out runs in the stream gate (locks `locks.clj`
L1270-1283, `module.clj` L123, L185), which cannot write the micro store's
lease rows. So the prediction is "as said", conditional on phase 4 being
built as validated, and a landing refusal is an unexplained difference
that fails the test, not a pre-excused risk. Leaving "at risk" in place
would let a `:landing-lock-gone` line read as expected. Fixed in F5.

**D1** (traces L241-257). Model: o0 sent; `[:prepare]` decides it no
`:permission-from-another-layer`, kept in `[:micro :prepared :delta]`
(model.clj L738-739) and recorded only at commit (L749-752); the revoke o2
sent; `[:step 1]` decides o2 yes at 6 on stream p1; `[:commit]` records
o0's no at stamp 3. Rig (L287): at `[:prepare]` Alice's lease act in
`:group` citing `[:alice :group :alice-hand]` is refused
`:permission-from-another-layer`, recorded, and the value act is refused
`:no-such-lock` on its face (P2 L1968-1973); at `[:step 1]` the operator's
revoke in `:alice-hand`; at `[:commit]` a check only. The approximation
(the rig commits the value's decision before the revoke) is stated exactly
(L287), and it cannot turn a difference into "as said": under baseline the
refusal rests on the pid's own `in`, and a rig that failed to enforce it
would admit the value in the rig's order and show it open, a difference.
The case's subject, a revoke falling between prepare and commit, is not
reproduced at all, and the line says that. Honest. **Two fails found:**
(a) the plan reads the prepared decisions as `(:delta (:prepared st))`
(L100-101); the model keeps them at `[:micro :prepared :delta]`, so that
path reads nil and the lockstep would send the rig's write at `[:commit]`,
after the revoke, which is the order the plan's own design log (L668-672)
set out to avoid. (b) At `[:commit]` o0 gains its recorded answer; the
lockstep's rule "names that gained a recorded answer" (L98) would play it
a second time unless a name played from the prepared delta is marked
played. Both fixed in F7.

**D2.** The world table (L196) allows two roads: Alice's lease cites her own
pid `[:alice :group :group [:group :group :group]]`, or, "where the merged
door requires a session's permission for a lease", the session's grant
beneath it. D2's row says "the walk (R19) finds the revoke on the cited pid
itself here, so no ancestor is involved" (L288), and KD9 says no case but
D2 depends on a chain (L347-348). On the session road the cited pid is the
grant beneath the revoked one, and the refusal comes from the walk's cut
(P3 L524-531: `:permission-revoked` "when it is revoked or cut"). The
predicted reason is the same on both roads, but L288 as written holds only
on the first road: a self-inconsistency. Fixed in F9.

**A1 to A8.** The model decides each value act at settle (stream p0 for
`:alice`, micro 0 for `:group`) and destroys person locks in the history's
order (persons L7-108 of the traces). The rig plays each at its decision
and each person forget as phase 2's act (L250-257). "Practical" is honest
**if** the person forget is phase 2's act. **Fail found:** L565-567 says
that against phase 3's placeholder alone "a person forget is
'approximated (its effect written directly, no act)'". The placeholder
(micro `micro_client.clj` L159-163) appends the effect to
`*persons-placeholder`, which a stream topology writes straight into
`$$persons`: no gate, no answer, no stamp from a decision. This goes
against "every forget is a fact" (SPEC.md L79, IMPLICIT_SPEC D3). An
approximation that skips the forget's own decision and fan-out could turn a
difference in them into "as said", which C1's rule forbids. So it must be
"not practical (phase 2's person act missing)", not approximated. Fixed in
F8.

**Verdict C1: FAIL, fixed** (F5, F6, F7, F8, F9, F10).

## C2. Sending each rig act at the step that decides it

**Requirement:** "compare answers and reads with the model's", which only
means something if the rig decides in the model's order.

Trace (B1, above): sending at the queueing step would send `forget-value!`
at `[:forget-value 0]`; the rig's door waits for its answer, so the note
would be erased before the first `[:read :now]`, where the model shows it
open (traces: "read as of 8 … o0#0 alice v1"). Sending at the decision
keeps the model's order of decisions and reads. Stamps agree in kind: the
model stamps at decision, and the rig stamps at decision, which is when it
is sent. The picks come from the model's sends (L93-97), so no index is
re-resolved on the rig. The rule is right (L79-85, L599-605). It needs
nothing the fixed histories lack: every "later" continuation is visible in
the op that decides the part before it (B1's first `[:step 0]`: o2
decided, `crossing:o2` queued; B3's `[:work 0]`: `landing:o2` sent, not
decided).

What it hides, which the plan did not say:
1. No rig offer is ever in flight while another is decided, except under
   a hold or the pause. The rig's own ordering of offers queued together is
   not exercised. On a stream partition that order is the depot's append
   order, which is the send order, so nothing is lost there. On the micro
   side it is KD16 (UUID7 order within a batch), which no fixed history
   reaches.
2. B1 and B2 test the read-out's recovery road only (C1, B1).
3. The door's retry and timeout behaviour, since every rig act is awaited.

**Verdict C2: FAIL on the reporting, fixed** (F10 states the three in the
plan and in the report's header). The rule itself passes.

## C3. `:values` from the store's own opening, `:shown` from the status

**Requirement:** "compare answers and reads with the model's", where
`play`'s `:values` is `find-fact` by `[layer k persons]` over every fact
(`scenarios.clj` L91-93): missing when no such fact exists, open when
`readable?` (model.clj L400: no erasure of its lock), else erased.

The opening itself is independent on both sides. Phase 2's `opens?`
(locks `client.clj` L375-381) reads `read-as-of` at the value's own stamp:
a real open, and since a destroyed lock stays destroyed it gives today's
openability, as `readable?` does. Phase 3's `open-row` (micro
`micro_client.clj` L304-313) opens with the person locks it reads from
`$$persons` through `locks/open-with`, the pure half of `open-value>`.
Neither reads a status flag. Pass on the opening.

**Fail found: the lookup can agree by construction.** The plan finds the
rig's fact through the correspondence, as the rig fact id of the model's
fact (L117-118, L157-160: "no rig fact is missing"; `opens?` takes a fid,
L537). In D1 and D2 the model has no fact, so there is no model fact id to
map, and the rig's side is "missing" whatever the rig did. Concrete case:
if the micro gate wrongly admitted Alice's D2 write, the rig would hold an
open `group note`, and the `:values` comparison would still print
"missing, as said". Only the separate answer comparison would catch it.
The same holds for any expected ref the model never made. Fixed in F1: the
rig's side of each ref is found from the rig's own records. For every act
the replay sent, and every act the store made under the rig's names (a
request's landing through `env/landing-name`), the adapter takes the
rig's answer and, for a yes, the facts the rig recorded (`opens?` on the
stream side, `open-act` through F on the micro side). It matches them on
layer, key and the persons of the value as the replay sent it. "Missing"
then means that no act the rig admitted carries such a fact. Where a layer
read by pattern exists (`read-pattern`, P5 L712, for a one-owner layer),
it is also used, and a rig fact matching the ref that the adapter does not
know of is a difference.

`:shown`: `promotion-status [layer req as-of]` takes no reader (L173-175),
and the request's rig name is the rig's own. Pass, with one tightening in
F1: each read's list of statuses is built from the requests the replay sent
so far, not from the model read's `:promotions`. Built that way, a request
the model does not show cannot vanish from the rig's line.

**Verdict C3: FAIL, fixed** (F1).

## C4. The known differences, and whether one can mask another

**Requirement:** "Report every difference; a difference is a finding, not
something to hide."

The plan prints every KD by number in the lines it touches (L292-294,
L459-466). A predicted one that does not show prints "predicted, not seen"
(L464-466), and an unexplained difference fails the test while its line is
still written (L470-477). The rule is right. Attack, KD by KD, on whether
the attribution is exact enough that it cannot absorb an unknown:

- **KD1** is data and exact: same reason R on the lease, plus the value
  act's face `:no-such-lock` (L462-463). A lease refused for another reason
  does not match. Pass. One gap: the rule assumes the merged door sends the
  value act after a refused lease. If it does not, there is no face answer
  to match. F8 has the build record which, and the rule then matches the
  lease alone, named.
- **KD2, KD3, KD6, KD7, KD8** (rig-only acts). Their answers are not
  compared with the model's, but the plan does not say that each must be a
  yes. A seed act or a lease refused where the plan predicts yes would
  surface only indirectly, through a later value act. Fixed in F11: every
  rig-only act must answer yes, except a lease under KD1's rule, and a
  refused one is an unexplained difference named as itself.
- **KD10. Fail found.** Its attribution is loose: "predicted a refusal;
  which reason, `:person-forgotten` from the lease or a refusal of the
  entry, the run shows" (L359-361). Any refusal after a forget would read
  as KD10, including an exit bug. The source is exact: L11 revised (P2
  L740-746) says a lease act "whose layer's owner has no person lock or a
  destroyed one is refused the same way, recorded", and a lease made
  before the forget makes the offer `:no-such-lock` on its face. Fixed in
  F3: KD10 matches only a reader whose `forget-person!` the rig answered
  yes, refused with the entry's lease `:person-forgotten`, or `:no-such-lock`
  on the entry's face for a lease leased before the forget. Any other
  refusal, or a refusal of a reader not forgotten, is unexplained. A
  forgotten reader's read that succeeds is compared on its content and
  printed "predicted, not seen". **And its reach is wider than it needs to
  be:** "Bob has no working layer in the model's world, and the replay adds
  none" (L355-356). After Alice's forget in A4 and B4, the group therefore
  has no reader through the exit, though Bob, a member not forgotten, could
  read it. That is practical, so "where practical" asks for it. Fixed in F4:
  a rig-only working layer `:bob-hand` in the seed (KD8), and the group read
  through the exit as a member who is not forgotten, once the shared read
  exists.
- **KD11.** "It compares the case's own facts, never the seed's, never the
  rig-only acts" (L373-374), through point reads of the ids the model made
  (L162-165). **Fail found:** a rig fact with no model counterpart, such as a
  second copy from a landing sent twice under different names, or a value
  written where the model refused, is never read. Masking by omission.
  Fixed in F2: the case's one-owner layers are also read by pattern below
  the exit (`read-pattern`, P5 L322, L712), and on the micro side every rig
  name the replay sent into the group is read through F. Any value fact
  that is not a rig-only kind and has no counterpart is a difference. The
  control facts the model's reads show (the request `o2#0`, the crossing
  `crossing:o2#0`, a forget `o3#0`, a revoke: traces L125-127, L181-184) are
  compared on their existence and on their references through the
  correspondence, not on their literal value maps. That form difference is
  named KD20.
- **KD12, KD13, KD17**: relations or settled reads, stated exactly. Pass.
- **KD4, KD5**: only on named roads. Pass.
- **KD14, KD15, KD16, KD18, KD19**: not exercised, printed once. Pass. A
  history that steps them is refused, not partly played (L229-231).

B4's risk (For Sid 4 against PR9) was carried as a pre-excused outcome.
Under F5 it becomes a plain prediction that fails loudly.

**Verdict C4: FAIL, fixed** (F2, F3, F4, F5, F11).

## C5. The test fails on an unexplained difference; "predicted, not seen"

**Requirement:** "a difference is a finding, not something to hide".

L470-477: fails on any baseline `:values` or `:shown` difference, on any
answer or read difference no KD explains, and on a status worse than
predicted once its stages resolved. It passes with known differences
printed, and the report is written in a `finally`. L464-466 give "predicted,
not seen". A rig that agrees with a non-baseline configuration where
baseline differs (L264-266) is already a baseline difference, so it fails.
The lockstep's end state must give `play`'s `seen` (L511-513), so the
lockstep cannot drift from the model. With F1 to F3, F6 and F11 the
attribution rules are exact enough to support this verdict. Tonight, with
phase 4 unbuilt, B1 to B4 are "not practical (phase 4 missing)" and the
test passes. That is honest because the header lists the stages resolved
(L422, L474-475). **Verdict C5: pass** (given the fixes).

## C6. The APIs against the builds in flight

The plan's table (L529-553) and its note (L555-574) against locks
`cc506303`, micro `48562bde`, reads `aaee646b` (the plan saw locks
`b2c435d6` and reads `b070858c`):

| planned | now | what the build does |
|---|---|---|
| phase 2 `connect`, `offer!`, `lease!`, `close-session!`, `make-person!`, `forget-person! [store p]`, `forget-value! [store who layer fid]`, `read-as-of`, `opens? [store layer fid]`, `seed!`, `person-on-task [store p pkey]` | all exist (locks `client.clj` L42-514); `forget-person!` is an act through `offer-until-answered!`; `open-value>` is a `deframafn` (locks.clj L1320), not a var to resolve | use them after the merge |
| the fan-out waited for | in the stream gate: `fan-out>` with `\|all` (locks.clj L1270-1283), called from module.clj L123 and L185, so the gate's acked append covers it | wait on the ack; `person-on-task` as the check |
| `micro-frontier` | no such name; it is `micro-client/frontier [store]`, `[store pkey]` (L59-62) | use `frontier` |
| `micro-lookup` (the build's `await-answer`) | `micro-lookup` and `micro-act` are query topologies (micro.clj L1375, L1390), called through `foreign-query`; the client functions are `await-answer [store offer]` (L215) and `act`/`open-act` | resolve the client functions; a missing query shows as a throw of `foreign-query`, caught per case as "not practical" |
| micro leases | `micro-client/lease! [store spec]` (L254), a different shape from phase 2's `lease! [store who layer session n permission nm]` | the merged door's one lease road; the build records it |
| `make-base!`, `make-group! [store L {:keys [members root grants]}]`, `open-session! [store S p layers]`, `open-row [store e row stamp]` | exist (micro_client.clj L304-465) | use them |
| phase 3 `forget-person! [store p stamp]` | **a placeholder**: appends the effect to `*persons-placeholder`, written straight into `$$persons` by a stream topology (L159-163, micro.clj L1060-1078), no act, against "every forget is a fact" | never resolved by the replay. With it alone, a case with a person forget is "not practical (phase 2's person act missing)". The merge must drop it: `put-person!` already throws once phase 2's gate merges (L151), and `$$persons` can have only one owner |
| three `connect`s | `client/connect`, `micro-client/connect` (L27, wraps it), `read-exit/connect` (L28, wraps it) | merge the handles once per module |
| `read-exit/read! [store spec]`, `read-point` | `read!` exists (read_exit.clj L88); `read-point` and `read-pattern` are query topologies (reads.clj L1085) | as the micro queries |
| `inject/hold!`, `release!`, `:before-read-out`, `:before-forward`, `lease-landing!`, `promote!`, `promotion-status` | none exist (phase 4 unbuilt). A `hold!` exists only in `rig.claims` (phase 0), and it blocks the task thread | the replay must never use `rig.claims/hold!`, since it would hold up B1's forget on the held request's task. Phase 4's hold ends the record's processing (P4 L974-977) |
| `env/crossing-name`, `env/landing-name` | exist in `envelope.clj` in all three builds | use them |
| `rtest` pause, resume, create, launch, destroy | Rama 1.6.0 | probed below |

**Verdict C6: FAIL, fixed** (F8 updates the table and the note).

## C7. The two probes

`runs/probe-replays-2026-09-26.clj`, run under the cluster lock
03:41:51 to 03:42:05 IST, output `runs/probe-replays-2026-09-26.txt`:

- **Relaunch (RP2).** Phase 1's `Store` at 4 tasks and 2 threads was
  launched, given one append, and destroyed, three times in one in-process
  cluster: launch 851, 377 and 381 ms; destroy 238, 157 and 162 ms. A fresh
  module per case works and costs under a second at stage 1. The merged
  module will be heavier; the build measures it. The fallback, whose world
  renaming the micro helpers' fixed `:group` and `:base` would hamper, is
  not needed on this evidence.
- **Pause during a stream fan-out (open question 2).** A toy module with a
  stream topology fanning out with `|all` and a microbatch topology: with
  the microbatch topology paused, an acked append through the stream
  topology returned in 14 ms and its write was on every task probed. The
  micro record waited (nil after 1.5 s), was processed 46 ms after resume,
  and a module destroyed while paused relaunched running. Together with
  the builds (phase 2's person fan-out is in the stream gate; the micro
  topology only reads `$$persons`, micro.clj L1135-1414, no write), pausing
  the micro topology does not block B4's person forget. The `:before-forward`
  fallback stays for a merge that changes this. The build re-checks it on
  the merged module.

Neither failure would have changed the design. Both answers are carried
into the plan's open questions (F12).

## The fixes, applied in PLAN-replays.md

- **F1** (C3): the rig's side of `:values` from the rig's own records,
  never through the model's fact ids; `:shown` per read from the requests
  sent so far.
- **F2** (C4, KD11): pattern reads of the case's one-owner layers and every
  rig name sent into the group; control facts compared by existence and
  references; KD20 added.
- **F3** (C4, KD10): the exact attribution rule; a successful read by a
  forgotten reader is compared and printed "predicted, not seen".
- **F4** (C4, KD10): a rig-only `:bob-hand`, so a live member reads the
  group through the exit after Alice's forget, once the shared read exists.
- **F5** (C1, B4): predicted "as said" per the validated P4 (PR9, PR13,
  F9, T4) and phase 3's `persons-to-check`; a landing refusal fails the
  test; sources re-pointed to the validated P4.
- **F6** (C1, B3/B4): the model's answers for one name across partitions
  reduced to one before comparison; a model name whose partitions disagree
  is printed as the model's own and fails.
- **F7** (C1, D1): the prepared delta at `[:micro :prepared :delta]`; a name
  played at `[:prepare]` is not played again at `[:commit]`.
- **F8** (C1, C6): the API table and note against the builds in flight; the
  placeholder person forget makes a case "not practical", not approximated;
  `rig.claims/hold!` named as not to be used; KD1's door road recorded.
- **F9** (C1, D2): D2 and KD9 made conditional on the door's road.
- **F10** (C2): what the lockstep does not test, in the plan and in the
  report's header.
- **F11** (C4): every rig-only act must answer yes, except a lease under
  KD1's rule.
- **F12** (C7): open questions 1 and 2 answered by the probe.

## Self-consistency check

Read back after writing. Each check that found a fail says FAIL and names
its fix; none says pass while describing a gap. C2 passes the rule and
fails the reporting, and both are stated. C5 passes only given F1 to F3,
F6 and F11, and says so. Items left open are open by the plan's design, not
by this validation: B1 to B4 are "not practical" until phase 4 merges; the
group is read through phase 3's frontier until phase 5's shared read
exists; the merged module's relaunch cost and its fan-out's topology are
re-checked by the build. None is a gap in the plan once the fixes stand.
No failure needs the architecture rethought: the lockstep, the two
observations, the holds, the pause and the fresh module per case all
survive the traces and the probe. Hence minor-fail, fixed in place.

## For Sid (carried from the plan, sharpened)

1. KD10's reach (the plan's question 5): after a person's forget, nothing
   can be read for them through the exit, and nobody can read their
   one-owner layers there. The model's read belongs to nobody and shows
   erased dates. F4 keeps the group readable by a member who is not
   forgotten; the one-owner layers stay unreadable through the exit.
2. B4 (the plan's question 6) is no longer a question for Sid. The
   validated P4 answers it (PR9: landing leases bare), so the run confirms
   or refutes it.

PHASE_VALIDATION:minor-fail
