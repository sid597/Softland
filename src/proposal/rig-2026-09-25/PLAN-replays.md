# Plan — phase 8, "replays": the model's fixed histories through the rig

Planned by Claude Opus 5.5 at max effort, in a fresh session on branch
`rig-plan-replays`, 26 September 2026 from 03:11 IST. It is the plan step;
a fresh session validates it, and another builds it after phases 2, 3 and 4
are built. The rama skill's phase 1 shape is followed; the template's
sections that do not apply to a test adapter (it declares no depot, PState
or topology) say so in one line each, under "The template's sections".

Sid's phase 8, verbatim (SPEC.md): "Where practical, replay the model's
fixed histories from scenarios.clj through the rig and compare answers and
reads with the model's. Report every difference; a difference is a finding,
not something to hide." RIG.md ("What is next") adds: "Phase 8's replays
are a standing practice at every step, not a final phase."

## Sources read, and at which commit

- The model: `src/proposal/formal-model-2026-09-24/src/formal/model.clj`,
  `scenarios.clj`, `run.clj` (the named configurations its report uses,
  lines 236-252) and README.md lines 20-152, on this branch (`5aba1253`).
- The model run for this plan, no cluster: `runs/phase8-model-traces.clj`
  and its output `runs/phase8-model-traces.txt` (all 14 cases "as said"
  under `baseline`, with every answer, every read and the trace). The
  model's side of every case below is taken from that output, not from
  memory.
- The validated plans on this branch: `PLAN-locks-and-forgetting.md`
  (phase 2), `PLAN-micro-store.md` (phase 3), `PLAN-read-exit.md` (phase
  5a). The phase 4 plan being validated:
  `/mnt/data/projects/Softland-rig-plan-promotion/src/proposal/rig-2026-09-25/PLAN-promotion.md`
  at `d6712941`. Line numbers below are "P2 L…", "P3 L…", "P4 L…", "P5 L…"
  for those four files. Collection from them was done by four read-only
  gatherers; the lines that decide a case here were read again by this
  session (P4 L826-888, L980-985; P3 L715-742, L758-764, L2448-2454; P2
  L742-743, L1716-1726).
- RIG.md in the main rig worktree (the newest copy): "For Sid" 1 to 13,
  the defaults 1 to 8, R1 to R19. IMPLICIT_SPEC.md D2 to D8. SPEC.md.
- Existing code the adapter stands on: `src/rig/store/client.clj`
  (`build`, `offer!`, `offer-until-answered!`, `lookup`, `model-world`,
  `grant-offer`, `revoke-offer`), `src/rig/store/inject.clj`, and
  `test/rig/store/stream_gate_test.clj` (which already asks
  `formal.model/run` under `baseline` for its answers). The micro build's
  `micro_client.clj` in `/mnt/data/projects/Softland-rig-build-micro`
  (`003f92a4`) was read for names only; it is a build in flight, not a
  contract.
- Rama 1.6.0's test namespace, listed with no cluster:
  `com.rpl.rama.test` has `create-ipc`, `launch-module!`,
  `destroy-module!`, `pause-microbatch-topology!`,
  `resume-microbatch-topology!`, `update-module!`,
  `wait-for-microbatch-processed-count`.

## Scope, in one paragraph

One test namespace plays each of scenarios.clj's fourteen fixed histories
(eight A, four B, two D) through the rig's door, gates and read exit, and
the same history through the model under `baseline`, the configuration the
rig implements. It compares what `scenarios/play` compares (the `:values`
map of open, erased or missing, and the `:shown` sequence of promotion
states), and beside that every answer (yes, or no with its reason) and
every read. It writes one line per case per configuration, as
`scenarios/report` prints for the model, with the rig's line beside it,
to `runs/phase8-replays.txt`, which RIG.md reads. It adds nothing to the
store: no depot, PState, topology or query. It needs the APIs of phases 1
to 5 as their plans name them; where one is missing at build time the
case says "not practical" with the missing name, never a stand-in that
forces a match.

## The adapter's shape

### Lockstep, driven by the model's decisions

The model separates sending from deciding: `[:offer …]`, `[:promote …]`,
`[:forget-value …]` and `[:revoke …]` only put an offer on a queue, and
`[:work p]`, `[:step p]`, `[:batch]`, `[:prepare]`/`[:commit]` and the
closing `drain` decide what is queued. The rig's door waits for its
answer: a stream offer is decided before the acked append returns, a
micro offer before `micro-lookup` answers and the frontier passes its
batch. So the rig's counterpart of a model offer is sent **at the model
step that decides it**, not at the step that queued it. Sending at the
queueing step would decide it too early: in B1 the value forget is queued
before the first `[:step 0]` and decided after the first read, and a rig
forget sent at the queueing step would make that read show the note erased
where the model shows it open.

The adapter therefore steps the model one op at a time, with the model's
own public functions (`fm/init`, `fm/seed-permissions`, `fm/step`,
`fm/drain`, `fm/read-as-of`, `fm/now`, as `fm/run` composes them, so the
end state is `fm/run`'s), and after each op diffs the model's state for
that op's effects:

1. **Sends**: names new in `(:sent st)`, with the offer as the model built
   it. This is how the model's picks are resolved: `[:forget-value 0]`'s
   target is the sent forget's `:facts` target, `[:promote 0 :group 1]`'s
   source is the request's `:promote :source`. The adapter never
   re-implements a pick.
2. **Decisions**: names that gained a recorded answer (`fm/answers-for`),
   each with store, partition, yes or no, and reason; and, for
   `[:prepare]`, the decisions in `(:delta (:prepared st))`, which the
   model makes at prepare and shows only at commit.
3. **Person forgets**: persons new in `(:persons st)`.
4. **Reads**: a read new in `(:reads st)`.

Each effect with a rig counterpart is then played on the rig in the
model's order. Within one op the fixed histories decide at most two
offers, both on one stream partition (B3's and B4's `[:work 0]`: the
request, then its read-out), so stamp order on that partition is the
model's order. A general history would take a batch's order from the
prepared delta and a work step's from the partition's stamps; the fixed
histories do not need either, and the adapter asserts, per op, that it
never sees two decisions whose order it cannot tell (a guard, so a later
history that breaks this is caught, not mis-ordered).

The adapter keeps one table, **the correspondence**: model name → rig
name, and model fact id `[name i]` → rig fact id. Every later rig step
that names a model fact (a forget's target, a promotion's source, a read's
point) goes through it. A model name with no rig counterpart (the rig
refused what the model admitted, or the reverse) is itself a difference,
reported, and any later step that needs it is played as "no counterpart"
and reported, never guessed.

### Continuations held to the model's order

The rig runs a promotion's read-out and forward as continuations of the
request's own record (P4 PR7, L649-652; PR8, L660-664). The model queues
each as the store's own offer. When the model decides a continuation in a
**later** op than the part before it, the adapter holds the rig's
continuation until that op:

- **Read-out later than its request** (B1, B2): `inject/hold!` on the
  request's name at `:before-read-out` before `promote!` (P4 L836-841);
  at the model's decision of `crossing:<req>`, `inject/release!` and the
  door's resend of the same request, answered from the record, which
  continues the read-out.
- **Landing later than its read-out** (B3, B4): the micro topology paused
  (`rtest/pause-microbatch-topology!`) after the landing lease is answered
  and before `promote!`, so the forward's append waits in `*micro-offers`,
  as the model's forward waits in the micro inbox; at the model's decision
  of `landing:<req>`, `rtest/resume-microbatch-topology!` and a wait for
  the landing's answer and for the frontier to pass its batch. Fallback,
  if pausing is not usable with the merged module: `inject/hold!` at
  `:before-forward` and, at the landing's decision, release and resend
  (P4 L841-844 names the same stand-in); the line then says so, and that
  road also exercises the re-send from the stored forward (KD5).

When the model decides both parts in the same op (the request and its
read-out in B3's and B4's `[:work 0]`), nothing is held.

### Two observations: the store's, and a reader's

- **The store's judgment**, for `play`'s `:values`: the model's
  `readable?` asks whether a value's lock still opens, whoever reads. The
  rig's counterpart is its own opening of the value with the store's
  person locks: phase 2's `opens?` (P2 L1932-1933, `open-value>` inside)
  for a stream-side layer, phase 3's row read through the frontier for a
  group row (`micro-act` at F plus the pure open, P3 L1724-1728 and the
  build's `open-row`). `{:value v}` is open (and v must equal the model's
  value), `{:erased-at s}` is erased, no rig fact is missing. This is
  reader-independent on both sides, so it is compared for every case.
- **A reader's view**, for each read's answer: at every `[:read :now]` and
  at `run`'s closing read, the adapter reads through the exit what the
  model's read shows of the case's own facts: `read-exit/read!` for
  `:alice`, point reads of the rig fact ids of every model fact the case
  made in that layer, as of now; and `promotion-status` as of now for each
  request (P4 L543-557), which is the `:shown` entry. A one-owner layer
  only tonight (P5 L40-41); the group through the shared-layer read if
  phase 5's rest has built it, else phase 3's frontier read, and the line
  names which. Where the exit refuses (a reader who has been forgotten,
  KD10), the refusal is the rig's answer to that read, and the store's
  judgment is printed beside it so nothing is hidden.

`:shown` comes from `promotion-status` (reader-independent in P4's query
signature `[*layer *req *as-of]`), so the B cases' `play` comparison does
not depend on the exit.

## The world, as data

The model's world (model.clj L37-67) and the rig's, one map the namespace
holds as data. Each case runs on a fresh module (see "How it runs"), so
the model's names are used as the rig's names and the mapping is the
identity wherever the rig allows it.

| model | rig | source |
|---|---|---|
| people `:alice`, `:bob` | persons `:alice`, `:bob`, made by the operator's `make-person!` acts in the `:people` layer before any layer (P2 L1926-1927, L1940-1942) | KD7 |
| `:alice` personal, owner Alice, home 0 | one-owner layer `:alice`, made by the operator's making act with `:kind :personal :owner :alice :class :by-layer :lock-grain :per-value` and root `[:alice :alice :alice]` (P10; P3 L454-456); home = hash, not 0 | KD8 |
| `:alice-hand` hand, `:alice-agent` agent | the same, kinds `:hand` and `:agent` | KD8 |
| `:base`, shared from the start | `make-base!`: one-owner on the stream gate, owner `:operator`, root `[:operator :base :base]`, re-classed to by entity by `make-group!` (default 6; P3 L606-644) | KD8 |
| `:group`, members Alice and Bob | `make-group!`: the micro gate's making act, `:members #{:alice :bob}`, root `[:group :group :group]`, the members' permissions beneath it (P3 L623-630, L939-941) | KD8 |
| entities `:e0` to `:e3` | the same keywords; placement by the rig's hash, not the model's `entity-part` | — |
| key `:note`, plain | `:note`, no grammar entry, names no one (P2 L188-189) | — |
| key `:mention`, names `(:persons v)` | `:mention` under the grammar `{:mention {:subjects-at [:persons]}}` (P2 L183, L1726) | — |
| a value `{:token "v1"}` or `{:token "v2" :persons #{:bob}}` | the model's value map verbatim (R17's domain), so an opened rig value is compared to the model's `:v` exactly | — |
| mark `#{:die-with-any}` | the same mark (P2 L203) | — |
| permission `[p l :own]` | `[p l l]` when p owns l, else `[p l l root]` beneath l's root: `[:alice :group :group [:group :group :group]]`, `[:bob :base :base [:operator :base :base]]` (P3 L729-732, M20) | KD9 |
| permission `[:alice l :session]`, kept in `:alice-hand` | `[:alice l :alice-hand]`, granted by the operator as a fact in `:alice-hand` (phase 1's `model-world`; M20) | KD9 |
| the operator, the store | `:operator`; the store's own steps are continuations with no `:who` of their own (For Sid 2: the root actor and the operator are one principal tonight) | — |
| a person's writing session | a session id for each writer, `:alice-hand` for Alice and `:bob-session` for Bob, opened by `open-session!` in each layer the history writes (P3 L456-461; P2 L334-336), so leases have a session | KD2 |
| the seed: every permission a first fact | the rig's seed: `:people` and the two persons; Alice's three layers; the base; the group (with the base's re-class); the permissions above; the sessions. Every seed act is the rig's, answered yes before the history starts; none is compared | KD8 |

## The step kinds and their rig counterparts

Every step kind the fixed histories use, with what the adapter plays at
the op that decides it. "Door" is the merged `rig.store.client` (the
micro side through its dispatch by tag, P3 L2113-2120).

| model step | the model does | the rig plays | faithful? |
|---|---|---|---|
| `[:offer {:who :layer :facts :stood-on nil :times 1}]` | queues an offer; `:times 1` sends once | at its decision: the door's `offer!` of the act, `:who`, `:layer`, the layer's class, the mapped permission, `:stood-on {}`, the model's facts with their values; the door leases and seals first (default 1; P2 L1919-1925), so a lease act precedes it | yes, with KD2's added lease act |
| `… :cite :session` | cites `[who layer :session]` | cites `[:alice l :alice-hand]` | yes |
| `[:work p]` | runs stream partition p's queue to empty | nothing of its own: each decision it made is played as its own effect (an offer sent, a continuation released) | yes |
| `[:step p]` | one stream event on p | the one decision that event made, played as its effect; where the event is a promotion's read-out, the release and resend above | approximated where it is a read-out (a hold stands for the separate event) |
| `[:batch]` | micro prepare and commit | each decision it made, sent and waited for: `micro-lookup` answers and the frontier passes its batch (P3 L2128-2133); for a held landing, the resume first | yes; a held landing is approximated |
| `[:prepare]` then `[:commit]` | decides at prepare, shows at commit | the decision's offer sent at `[:prepare]` and waited for, decided and committed together; `[:commit]` is then a check that the answer is visible, nothing sent | approximated: the rig has no step between a batch's decision and its commit, and no hook there (P3 L2452-2454) |
| `[:forget-person p]` | destroys p's person lock at once, stamped after everything | the operator's `forget-person!` act in `:people` and its fan-out to every task, waited for (P2 L1926-1927; P4 L878) | yes, with KD7 |
| `[:forget-value i]` | queues a forget of the i-th readable locked value, by the owner (the operator in a shared layer) | at its decision: `forget-value!` by the same writer, the target through the correspondence (P2 L1928-1930) | yes |
| `[:promote i :group 1]` | queues Alice's request for the i-th readable locked value of `:alice` into the group | at its decision: `lease-landing!` in `:group`, answered; then, per the holds above, `promote!` of the request naming the mapped source and target (P4 L820-821) | yes, with KD3's landing lease; approximated where a hold is needed |
| `[:read :now]` | a read of every layer as of the largest clock, with each promotion's status | the reader's view and `promotion-status` above, as of now | yes, with KD6 and KD11 |
| `[:revoke pid]` | the operator's revoke, a fact in the permission's layer standing on its grant | the operator's revoke act in the mapped permission's layer through that layer's gate, standing on its grant (client `revoke-offer`; P3 L1229) | yes |
| closing `drain` and read | works off every queue, resends the unanswered, then reads as of now | each remaining decision played; then the closing read as above | yes |

Step kinds the fixed histories do not use, named so a later history is not
silently skipped: `[:retry i]` and `[:reuse i spec]` (the door's resend of
the same map, and a new offer under a used name, both phase 1's);
`[:set-grain l]` and `[:reclass l]` (the owner's and the operator's
control acts); `[:failover store skew]` and `[:zombie store]`, which have
no rig counterpart (IMPLICIT_SPEC D7: no leader epoch the rig controls,
replication factor 1). The adapter refuses a history containing a step
kind it has no row for, with the step named, so the report says "not
practical" rather than playing part of it.

## The fourteen cases

The model's side of each is from `runs/phase8-model-traces.txt` (baseline)
and `runs/phase8-model-report.txt` (the model's own report under the
configurations `run.clj` names for the fixed histories). Names are the
model's: `o0`, `o2`, … in the order it made them, `crossing:o2` and
`landing:o2` for a promotion's store-made offers. "Settle" is scenarios.clj's
`[:work 0] [:work 1] [:work 2] [:batch]`. Every case's seed is the rig's
seed above; every value write carries a lease act before it (KD2); every
read through the exit is an entry act (KD6). The status is the prediction
this plan makes, to be confirmed or refuted by the run; the three classes
are defined under "The report".

### A: forgetting (A1 to A8)

| case | the model's history and answers (baseline) | the rig plays | status, and the predicted line |
|---|---|---|---|
| A1 in Alice's layer a note and a mention of Bob both die with Alice | Alice's `o0` (`e0 :note`, `e1 :mention #{:bob}`) into `:alice`, yes at `[:work 0]`; Alice forgotten; values `alice note` erased, `alice mention bob` erased | at `[:work 0]` the door's `offer!` of the two-fact act into `:alice`; at the forget, `forget-person! :alice`; the store's judgment by `opens?` on both rig facts | **practical**. Values as said (both `{:erased-at s}`, the person forget's stamp). Answers 1 of 1 as the model's. Closing read: the exit's read for Alice is refused (KD10, predicted); beside it, the store's judgment |
| A2 in the group a mention of Bob alone dies with Bob | Bob's `o0` (`e0 :mention #{:bob}`) into `:group`, yes at `[:batch]`; Bob forgotten; `group mention bob` erased | at `[:batch]` Bob's write into `:group` through the micro door (lease act, then the sealed act), waited for with the frontier; `forget-person! :bob`; the store's judgment by the group row read through the frontier | **practical**. Values as said. Answers as the model's. Closing read for Alice: the group through phase 5's shared read if built, else the frontier read, named in the line |
| A3 in the group a plain note about no one survives both forgets | Bob's `o0` (`e0 :note`) into `:group`; Bob, then Alice forgotten; `group note` open | as A2, two person forgets in the model's order | **practical**. Values as said (open, its value equal to the model's). Closing read: refused for Alice, forgotten (KD10) |
| A4 in the group a value about Alice and Bob survives Alice alone | Bob's `o0` (`e0 :mention #{:alice :bob}`); Alice forgotten; open | as A2 | **practical**. As said. Closing read refused (KD10) |
| A5 … survives Bob alone | the same; Bob forgotten; open | as A2 | **practical**. As said. Closing read for Alice: open |
| A6 … and dies when both are forgotten | the same; Bob, then Alice forgotten; erased | as A2 | **practical**. As said. Closing read refused (KD10) |
| A7 marked, dies with Bob | Bob's `o0` (`e0 :mention #{:alice :bob}`, mark `#{:die-with-any}`); Bob forgotten; erased | as A2, the mark on the fact | **practical**. As said. Closing read for Alice: erased, with its date |
| A8 (ruled 25 September) in Alice's layer a mention of Bob survives Bob | Alice's `o0` (`e1 :mention #{:bob}`) into `:alice`; Bob forgotten; open | as A1, `forget-person! :bob` | **practical**. As said. Closing read for Alice: open |

Under the other three A configurations the rig's outcome does not change
(it implements baseline), so its line says "differs from this
configuration" exactly where the model's own line says DIFFERS (A1 under
`baseline-but-not-owner-required`; A8 under
`baseline-with-a-read-as-owner-and-an-other`; A2 and A6 under
`baseline-with-the-third-reading-of-a`) and agrees elsewhere. A rig line
that agrees with a non-baseline configuration where baseline differs is a
finding: the rig then decides as that reading, not Sid's.

### B: promotion against a forget (B1 to B4)

| case | the model's history and answers (baseline) | the rig plays | status, and the predicted line |
|---|---|---|---|
| B1 a value forget queued before the read-out: pending, then refused | Alice's note `o0` yes; `[:promote 0 :group 1]` queues `o2`; `[:forget-value 0]` queues `o3` (target `o0#0`); `[:step 0]` decides `o2` yes; read: note open, `o2` pending; `[:step 0]` decides `o3` yes (note erased); read: pending; `[:step 0]` decides `crossing:o2` no `:source-erased`; closing read refused. Values `alice note` erased; shown `[:pending :pending :refused]` | `o0` at `[:work 0]`; at the first `[:step 0]`, the model decided `o2` alone, so: `lease-landing!` in `:group`, answered; `inject/hold!` on the request's name at `:before-read-out`; `promote!` → the request's yes; the read; at the second `[:step 0]`, `forget-value!` of the mapped `o0#0`; the read; at the third, `release!` and the door's resend of the request, answered from the record, whose continuation's read-out answers | **approximated**: the model's separate read-out event is the hold and the resend. Predicted: values and shown as said; the crossing's no carries `:source-erased` as the model's; KD3, KD4 |
| B2 Alice forgotten before the read-out: pending, then refused | `o0` yes; `o2` queued; `[:step 0]` decides `o2`; read pending; Alice forgotten; `[:work 0]` decides `crossing:o2` no `:source-erased`; closing refused. Values `alice note` erased; shown `[:pending :refused]` | as B1 up to the first read; `forget-person! :alice`, its fan-out waited for; at `[:work 0]` release and resend (P4 T2, L876-879) | **approximated** (the hold). Predicted as said. The closing read's exit read is refused for Alice (KD10); `promotion-status` still answers `:refused` |
| B3 a value forget after the read-out: crossed, then done; the copy stays | `o0` yes; `o2` queued; `[:work 0]` decides `o2` and `crossing:o2`, both yes, and queues `landing:o2`; read crossed; `o3` queued, decided at `[:work 0]`; read crossed; `[:batch]` decides `landing:o2` yes; read done; closing done. Values `alice note` erased, `group note` open; shown `[:crossed :crossed :done :done]` | `o0`; at the first `[:work 0]`: `lease-landing!` answered, the micro topology paused, `promote!` (request and read-out as one record; the forward's append waits); read; `forget-value!`; read; at `[:batch]` the topology resumed, the landing's answer and the frontier waited for; read; closing read | **approximated**: the model's queued landing is the paused micro topology. Predicted as said; KD3, KD4, KD13 (done is read only after the frontier passes the landing's batch) |
| B4 Alice forgotten after the read-out: crossed, then done; the copy stays | as B3 with Alice forgotten in place of the value forget, and no read between the forget and `[:batch]`. Shown `[:crossed :done :done]` | as B3, `forget-person! :alice` in place of the value forget; the model's history followed exactly, so no read is added (P4's T4 leaves this open; the replay does not) | **approximated** (the pause). Predicted as said on values and shown, **at risk**: the landing lease is Alice's, and a lease row is sealed under its act's writer (For Sid 4). If her forget leaves the landing's lock unopenable, the rig answers `:landing-lock-gone` and shows `[:crossed :refused :refused]` where the model shows done: a finding, reported as such. P4 names PR13 as what lets the landing land; the run decides. Closing read refused for Alice (KD10) |

Under `baseline-but-not-p6-line-at-the-read-out` the model's B lines are
all "as said" with P6 objecting to B3's and B4's copies. The rig's line
beside says the outcome is the same and that the objection is the model's
property, which the rig does not evaluate (the properties use the model's
`:order`, a ruler the rig has no counterpart of).

### D: permissions (D1, D2)

| case | the model's history and answers (baseline) | the rig plays | status, and the predicted line |
|---|---|---|---|
| D1 a group write under Alice's session permission; the session revokes it between prepare and commit | Alice's `o0` (`e0 :note`) into `:group` citing `[:alice :group :session]`, queued; `[:prepare]` decides it no `:permission-from-another-layer`; the operator's revoke `o2` in `:alice-hand` queued, decided yes at `[:step 1]`; `[:commit]` shows `o0`'s no. Value `group note` missing | at `[:prepare]`, Alice's write into `:group` citing `[:alice :group :alice-hand]`: the door's lease act is refused `:permission-from-another-layer` (recorded, under the lease's name) and the value act, holding no lock, is refused `:no-such-lock` on its face (nothing recorded); both waited for; at `[:step 1]` the operator's revoke of `[:alice :group :alice-hand]` in `:alice-hand`, yes; at `[:commit]` a check that the answers are visible | **approximated**: prepare and commit are one batch in the rig, so the value's decision is committed before the revoke where the model's is committed after; under baseline the answer rests on where the permission lives, which the pid itself says, so the order cannot change it. Predicted: value missing, as said; answers differ as KD1. Under `baseline-but-not-permissions-in-their-layer` the model's value is open (rv objects) and the rig's line says "differs from this configuration" |
| D2 a group write under her group permission, revoked in the group first | the operator's revoke `o0` of `[:alice :group :own]` in `:group`, yes at `[:batch]`; Alice's `o1` into `:group`, no `:permission-revoked` at `[:batch]`. Value missing | at the first `[:batch]` the operator's revoke of `[:alice :group :group [:group :group :group]]` in `:group`, standing on its grant, waited for with the frontier; at the second, Alice's write: the lease act refused `:permission-revoked`, the value act `:no-such-lock` on its face | **practical**. Value missing, as said; answers differ as KD1. The walk (R19) finds the revoke on the cited pid itself here, so no ancestor is involved |

## Differences known before the run, reported every run

These are reported, never filtered: each appears in the lines of the cases
it touches, by number, with what each side saw. "Predicted" marks those
reasoned from the plans and not yet run.

**Acts the rig adds that the model does not have.**

- **KD2. A lease act before each value write** (default 1; P2 L105-107:
  "phase 8 reports the lease acts as a difference by construction"). The
  door leases in the value's layer, the gate mints the locks, the act cites
  them. Control acts (forgets, revokes, the request) carry no value and
  lease nothing. Every case.
- **KD3. The landing lease before a promotion** (P4 L69-71, PR4, PR9): a
  lease act of count 1 in the target, bound to the one landing name the
  request will cause. B1 to B4.
- **KD4. The read-out as a continuation of the request's record** (P4 PR7,
  L649-652): no queued offer, no depot record; the request's ack carries
  the crossing's answer. The order is the model's; the granularity is not,
  which is why B1 and B2 need a hold. B1 to B4.
- **KD5. The re-send from the stored forward** (P4 PR8, L660-664,
  L1023-1026): the rig re-sends the stored forward byte for byte, even
  after the source is forgotten; the model sends again only while the
  source opens. No fixed history crashes between forward and landing, so
  this shows only if B3 or B4 falls back to the `:before-forward` hold,
  where the landing is then sent after the forget: the outcome is the
  model's (done, the copy open), by another road, and the line says so.
- **KD6. Every read through the exit is an act** (defaults 3 and 4; P5
  L259-277): an entry in the reader's working layer, sealed at the door
  under its own lease, stamped after its moment. The model's reads are not
  acts. The entries land in `:alice-hand`, which no fixed history
  compares, so they cannot change a compared answer; they are counted in
  the line.
- **KD7. Persons are data** (P2 L1904-1906, L7, L8): the `:people` layer,
  the operator's `make-person!` and `forget-person!` acts, each answered
  and stamped, with a fan-out to every task. The model has no make-person
  step, and its person forget is a lock-store step with one global stamp,
  "later than everything so far" (IMPLICIT_SPEC D3). Every case's seed;
  every A case and B2, B4.
- **KD8. Layers, the base and the group are made by acts** (P10, default
  6, P3 L641-644): the model's layers are static. The base is one-owner on
  the stream gate until the group's making re-classes it; the model's is
  shared from the start ("the seed is the only place the two differ", P3
  L641-644). Every case's seed; no fixed history writes into the base.

**Refusals and answers that differ.**

- **KD1. A writer without permission is refused on the lease, and the
  value act on its face** (For Sid 1; P2 L1968-1973; P3 L225-232): the
  lease act, citing the writer's permission, is refused with the model's
  reason and recorded; the value act then cites no lock and is refused
  `:no-such-lock` on its face, nothing recorded. The model records the
  permission reason on the value act itself. D1 (`:permission-from-another-layer`)
  and D2 (`:permission-revoked`). The value is missing on both sides.
- **KD9. Permission ids and chains** (P3 L442-445, L478-484, M20): the
  rig's id is `[holder layer in parent]` beneath a layer's root, at most
  four deep, and the holder may be the offer's session; the model's is a
  flat triple held by `:who`. Mapped as data above; no fixed history
  depends on a chain except D2, where the revoke is on the cited id itself.
- **KD10. After a person is forgotten, the exit cannot read for them, and
  nobody can read their one-owner layers through it** (predicted): an exit
  read appends an entry sealed under a lease in the reader's working layer,
  and a lease whose layer's owner has a destroyed person lock is refused
  `:person-forgotten` (P2 L742, L1716-1726). So a forgotten Alice cannot
  read the group either (her working layer is `:alice-hand`; Bob has no
  working layer in the model's world, and the replay adds none). A
  one-owner layer is visible only to its owner (P5 L679-682), so after
  Alice's forget nobody reads `:alice` through the exit; the model's read
  is nobody's and shows her facts with their erased dates. The
  replay attempts the read and reports what the exit answers (predicted a
  refusal; which reason, `:person-forgotten` from the lease or a refusal
  of the entry, the run shows), with the store's judgment beside it. The
  model's `:values` comparison does not depend on it. A1, A3, A4, A6, B2,
  B4 (closing reads, and B2's and B4's reads after the forget).

**Observations that differ in form, not in answer.**

- **KD11. What a read is and what "now" means** (P5 L1293-1316): the
  model's read covers every layer as of the largest clock anywhere, with
  every promotion's status; the rig's covers one layer as of that layer's
  task clock (a shared layer as of the frontier), and a promotion's status
  is its own query. The replay reads, at each model read, every layer the
  case's facts are in, and each request's status; it compares the case's
  own facts, never the seed's, never the rig-only acts.
- **KD12. Stamps** (default 2; P3 L693-701): the rig's hybrid clock values
  (ms × 65536 + counter), per task, against the model's integers. Never
  compared as values; the replay checks the relations both promise
  (a decision after what it stood on; an erased date after the value).
- **KD13. Done means settled** (P4 L329-331, L553-555): the rig reads done
  only once the landing is at or below the frontier; the model's, once the
  landing's stamp is at or before T. The replay reads after the frontier
  passes the landing's batch, so the two agree at the model's read points.
- **KD17. A value forget's answer carries `:how`** (P2 L1108-1110):
  `:row-deleted` or `:excised`; the model's is a trace note. Printed, not
  compared.

**Model features the fixed histories do not use, so not replayed.**

- **KD14.** Leaders, epochs, fencing, failover and the deposed leader
  (`[:failover …]`, `[:zombie …]`; IMPLICIT_SPEC D7); `:tick` and `:order`
  (the model's rulers). No fixed history steps them; the adapter refuses a
  history that does (above). A random history containing them would be
  "not practical" for these steps.
- **KD15.** Rama replays completed records after a crash (IMPLICIT_SPEC
  D4); the model replays only unfinished offers. No fixed history crashes.
- **KD16.** Batch order: the rig orders one batch by the names' UUID7s,
  the model by its inbox (P3 L1144-1146). No fixed history has two offers
  in one batch.
- **KD18.** Per-act lock grain with mixed marks (P2 L1512-1514): the
  rig's act lock dies with any subject of the union. No fixed history
  switches grain.

A difference the run finds that is none of these is a new finding: its
line says "differs" with what each side saw, the test fails, and RIG.md
carries it under "Found tonight" (or its successor) until Sid or builder A
names its cause.

## The report

`runs/phase8-replays.txt`, rewritten by every run, read by RIG.md. Its
header: the date and time (IST), the rig commit, the model's commit, the
task count, the run time, and which stages' APIs resolved. Then, for each
case set and each configuration `run.clj` names for it (A under
`baseline` and the three A readings; B under `baseline` and
`baseline-but-not-p6-line-at-the-read-out`; D under `baseline` and
`baseline-but-not-permissions-in-their-layer`), exactly the lines
`scenarios/report` prints for the model (its verdict, what it saw when it
differs, each named check's objection), each followed by the rig's line:

```
  under baseline
    as said   B: a value forget queued before the read-out: pending, then refused
      rig:    approximated (the hold at :before-read-out and a resend stand for the read-out's own step); as said
              answers  4 of 4 as the model's (o0 yes, o2 yes, o3 yes, crossing:o2 no source-erased)
              reads    1: alice o0#0 open v1, o2 pending | 2: o0#0 erased, o2 pending | closing: o0#0 erased, o2 refused
              rig-only lease 1, landing lease 1, entry 3 (each with its lease)
              known    KD2 KD3 KD4 KD6 KD7 KD8
  under baseline-but-not-owner-required
    DIFFERS   A: in Alice's own layer a note and a mention of Bob both die with Alice
              saw: alice note erased, alice mention bob open
               p6 objects: {...}
      rig:    as under baseline; differs from this configuration: rig alice mention bob erased, this configuration open
```

The rig's line leads with one of:

- **as said**: every model effect had its counterpart, played by the door
  alone in the model's order, and the rig saw what the model saw (`play`'s
  `:values` and `:shown`).
- **differs**: the same, and the rig saw something else; the line gives
  what each side saw, per value and per read.
- **approximated (why)**: some step needed a test hook or a merged step (a
  hold, a paused topology, prepare and commit as one batch), named in the
  parentheses; the outcome follows, "as said" or "differs" as above.
- **not practical (why)**: the case could not be played: a stage's API did
  not resolve (named), or the history has a step kind with no row above.
  Nothing is played partly.

The detail lines (answers, reads, rig-only acts, known differences) are
printed once per case, under `baseline`. Every answer is compared name by
name through the correspondence; a model answer whose rig counterpart
differs in a way a known difference explains names that difference
(KD1's rule is data: a model no with reason R on a value act matches a rig
lease act no with R plus the value act's face `:no-such-lock`). A
predicted difference that does not show is printed as "predicted, not
seen", so a prediction is checked, not assumed. The model's named checks
(P1 to P8, x1, x2, rv) are the model's alone: they use its `:order`, which
the rig has no counterpart of; the rig's line never claims them.

**The test's verdict.** Under `baseline` the test fails on any `:values`
or `:shown` difference, on any answer or read difference no known
difference explains, and on a case whose status is worse than its
predicted status in the case tables above once the stages it needs have
resolved (a case that is not practical only because its stage has not
merged yet is not a failure; the header lists which stages resolved). It
passes with known differences printed. The report is
written in a `finally`, so a failing run still leaves its lines.

## How it runs

- **One namespace**, `rig.replay-test`, file `test/rig/replay_test.clj`
  under the rig folder's `test/`, so it is also a standing check. It
  requires `formal.model`, `formal.scenarios` (both on the `:test`
  classpath already, deps.edn), `com.rpl.rama.test` and phase 1's
  namespaces. Every later stage's function is found at run time with
  `requiring-resolve`, so the namespace compiles and runs at every step of
  the build: a case whose APIs do not resolve yet is "not practical (the
  missing names)" and the rest still run. That is what makes the replays a
  standing practice from now on rather than a final phase.
- **Command**, from the rig folder:
  `flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:test rig.replay-test`.
  It never names `gate_test.clj`, so that file never loads.
- **One in-process cluster** for the namespace, `(rtest/create-ipc)`.
  **A fresh module per case**: `launch-module!` of `rig.store.module/Store`
  with `{:tasks 4 :threads 2}`, the seed, the history, the observations,
  then `destroy-module!`. Four tasks so a layer's home, the group's entities
  and the person fan-out cross tasks; fixed so runs repeat. Each case
  starts with `inject/reset-all!`, and a `finally` releases any hold and
  resumes the micro topology, so one failed case cannot leave the next one
  paused. **Fallback**, if a second launch of the same module in one
  cluster fails or costs more than 15 s: one module, and a fresh world per
  case (persons and layers named with the case's number, the base shared,
  since no fixed history writes into it); the report's header says which
  road ran.
- **Waits** are bounded: an answer, the frontier passing a batch, a
  fan-out, each at most 60 s, polled at 50 ms (the micro plan's rule, P3
  L2128-2133). A timeout is that case's "differs: no answer within 60 s",
  never a hung suite.
- **The model's side** runs in the same JVM: the lockstep for the
  correspondence, and `scenarios/play` under each named configuration for
  the model's lines. The test asserts the lockstep's end state gives the
  same `seen` as `play`, so the lockstep cannot drift from the model's own
  run.
- **Run time, estimated, to be measured**: a launch of `Store` a few
  seconds (phase 1's whole suite, one launch and 19 tests, ran in 17 to
  19 s); the seed about twenty acts, of which about five are micro acts at
  two batch cycles each on the 250 ms tick (P3 L1466-1471), so 3 to 5 s;
  a history 1 to 4 s (its micro steps dominate); so 8 to 15 s a case and
  2 to 3.5 minutes for the fourteen. The build writes the measured time in
  the report's header.

## The APIs it needs, by stage

The build checks each exists in the merged code under the name the plan
gives, or records it as stubbed or missing; a missing one makes the cases
that need it "not practical", named. "P1" is phase 1's code as it stands.

| API | stage | named at | used for | cases |
|---|---|---|---|---|
| `client/connect`, `build`, `offer!`, `offer-until-answered!`, `lookup`, `record`, `settings`, `permission` | 1 | `client.clj` (built) | the door's sends and answers by name | all |
| `client/grant-offer`, `revoke-offer`, `make-layer-offer` | 1 | `client.clj` (built) | the seed's grants, D1's stream-side revoke, Alice's layers | all; D1 |
| `inject/reset-all!`, `watch!`, `count-of` | 1 | `inject.clj` (built) | a clean start per case; seeing a continuation resume | all |
| `offer!` sealing at the door, leasing when short; `lease!`; `close-session!` | 2 | P2 L1913-1925 | KD2's lease before each value write | all |
| `make-person!`, `forget-person!` (acts in `:people`); the fan-out waited for, by the ack if it covers the fan-out, else by `person-on-task` on every task | 2 | P2 L1926-1927, L1934-1942 | the seed's persons; `[:forget-person p]` | A1-A8, B2, B4 |
| `forget-value!` `[store who layer fid]` | 2 | P2 L1928-1930 | `[:forget-value i]` | B1, B3 |
| `opens?` `[store layer fid]` → `{:value v}` or `{:erased-at s}` | 2 | P2 L1932-1933 | the store's judgment, stream-side layers | A1, A8, B1-B4 |
| the grammar `{:mention {:subjects-at [:persons]}}`; the mark `:die-with-any` | 2 | P2 L183, L203 | a mention's subjects; A7's mark | A1-A8 |
| the micro door: `*micro-offers` through the client's dispatch by tag; `micro-lookup` (the build's `await-answer`); micro leases | 3 | P3 L1451-1452, L1698-1702, L2113-2120 | every group write, revoke and grant | A2-A7, B3, B4, D1, D2 |
| `make-base!`, `make-group!` (with the base's re-class), `open-session!` | 3 | P3 L606-644, L623-630, L456-461 | the seed's base, group and sessions | all |
| `micro-frontier`, and a batch's id to wait past | 3 | P3 L646-691, L662-663 | the wait at every `[:batch]` | A2-A7, B3, B4, D1, D2 |
| `micro-act` at F and the pure open of a row (the build's `open-row`) | 3 | P3 L1724-1728 | the store's judgment for a group row | A2-A7, B3, B4 |
| `rtest/pause-microbatch-topology!`, `resume-microbatch-topology!` on the micro topology's name | Rama | `com.rpl.rama.test` (listed above); P3 L762 | holding a landing to the model's `[:batch]` | B3, B4 |
| `lease-landing!` | 4 | P4 L820, L220-230 | KD3's landing lease | B1-B4 |
| `promote!`, with the request's name known before sending (a build step or a caller-given name; P4 gives no argument list) | 4 | P4 L820-821, L238-246 | `[:promote i :group 1]` | B1-B4 |
| `promotion-status` `[layer req as-of]` → `:none`, `:pending`, `:crossed`, `:done`, `:refused` | 4 | P4 L404-405, L543-557, L822 | `:shown` at every read | B1-B4 |
| `inject/hold!` and `release!` at `:before-read-out`, `:before-forward` | 4 | P4 L836-841, PR15 | B1's and B2's read-out step; B3's and B4's fallback | B1-B4 |
| `env/crossing-name`, `env/landing-name` | 4 | P4 L359-365 | the crossing's and the landing's answers by name | B1-B4 |
| `read-exit/connect`, `read!` (point reads, `:as-of nil`) | 5a | P5 L797-815 | the reader's view at every read | all |
| the `read-point` query, called below the exit | 5a | P5 L671 | the store's view beside a refused exit read (KD10) | A1, A3, A4, A6, B2, B4 |
| the exit's shared-layer read | 5 (rest) | P5 L1043-1049 (named, not planned here) | the group through the exit; else phase 3's frontier read, named in the line | A2-A7, B3, B4 |
| `rtest/create-ipc`, `launch-module!`, `destroy-module!` | Rama | listed above | a fresh module per case | all |
| `fm/init`, `seed-permissions`, `step`, `drain`, `read-as-of`, `now`, `answers-for`, `all-facts`, `readable?`; `scenarios/play`, `a-cases`, `b-cases`, `d-cases` | model | model.clj, scenarios.clj (public) | the lockstep and the model's lines | all |

Gaps the build must close or record (found while planning): P4's list of
changes to earlier stages (L803-824) does not name `rig.store.inject`,
though its tests need `hold!`, `release!` and the new points; P4 gives no
argument list for `lease-landing!`, `promote!` or `promotion-status`; P3
names no function for pausing its topology (Rama's is used) and no hold
between a batch's decision and its commit (so D1 is approximated).

## The template's sections

- **Reads.** None new. The adapter reads through the stages' own reads,
  listed in the table above; their costs are their plans'.
- **Writes.** None new. Every write is an act through the door, as a
  person or the operator would send it.
- **PState Design.** Does not apply: the adapter declares no PState.
- **Depots.** Does not apply: it appends only to the stages' depots,
  through the door.
- **Topologies and PStates.** Does not apply: no topology; it pauses and
  resumes the micro topology in B3 and B4 only.
- **Query Topologies.** Does not apply: it calls the stages' queries.
- **Partitioning efficiency.** Does not apply: no read or write path of
  its own; the stages' tables stand.
- **State primitive selection.** The adapter's state is test-local
  Clojure data (the model's state, the correspondence, the report lines);
  nothing durable, nothing in the module.
- **Resource usage.** One in-process cluster, four tasks, two threads, one
  module at a time; the model's states are a few kilobytes each; run time
  as estimated above.

## Design decisions

1. **Send at the model's decision, not at its send.** Weighed against
   playing the history in its own order (send when the model sends, wait
   when the model works). That order decides every rig offer at once,
   before the model does: B1's first read would show the forget. The
   lockstep keeps the model's order of decisions and reads, which is what
   the history means; its cost is a diff of the model's state per op,
   through public functions only.
2. **A fresh module per case**, weighed against one module with a fresh
   world per case and against one world for all. Forgetting is permanent
   and the base is one per module, so one world leaks; a fresh world per
   case needs renamed layers and the micro helpers name `:group` and
   `:base`. A fresh module keeps the model's names as the rig's, at the
   cost of fourteen launches; the fallback is named for when that cost is
   too high.
3. **Two observations**, weighed against the exit alone. The model's
   `readable?` belongs to nobody; the exit reads for a person and cannot
   read for a forgotten one (KD10). `:values` uses the store's own opening,
   reads use the exit, and both are printed, so KD10 is visible instead of
   turning into false "differs" lines.
4. **Holding continuations with hooks the stages already plan** (phase
   4's hold, Rama's pause), weighed against new hooks in the gates. The
   replay adds no hook to the store. Where no hook exists (D1's commit),
   the case is approximated and says why.
5. **Known differences as data, unknown ones failing the test**, weighed
   against a report-only run. A report nobody asserts on drifts; a test
   that fails on every difference cannot stay green while the leases stand.
   Attributing each difference by rule keeps both: known differences
   print, new ones fail.
6. **Names found at run time** (`requiring-resolve`), weighed against
   plain requires. Plain requires would stop the whole namespace compiling
   until phase 4 merges; the standing practice needs it to run at every
   step.

## Rig choices proposed

Each can change without touching a record.

- **RP1.** Replays play in lockstep with the model, each rig act sent at
  the model step that decides its counterpart.
- **RP2.** A fresh module per case on one in-process cluster, four tasks,
  two threads; the named fallback when relaunching fails or is slow.
- **RP3.** `:values` compares the store's own opening; reads go through
  the exit; both are printed.
- **RP4.** A read-out is held by phase 4's `:before-read-out` hold and a
  resend; a landing by pausing the micro topology, with the
  `:before-forward` hold as fallback.
- **RP5.** D1's prepare and commit play as one batch, marked approximated.
- **RP6.** Writers' sessions: `:alice-hand` for Alice (her hand layer's id)
  and `:bob-session` for Bob, a session id with no layer, since the
  model's world gives Bob none.
- **RP7.** The test fails on a baseline `:values` or `:shown` difference
  and on any difference no known difference explains; the attribution
  rules are data in the namespace.
- **RP8.** `runs/phase8-replays.txt` is rewritten by every run, with the
  header above.

## Design difficulty log

- **The model queues; the rig's door waits.** The first design played
  the history in its own order and read as the model reads; tracing B1
  showed the forget decided before the first read. Changed to the
  lockstep. Every later choice leans on it.
- **Who reads.** The model's read has no reader; the exit needs one, and a
  forgotten one cannot lease its entry. Found by tracing A1's closing read
  against P2's `:person-forgotten` lease refusal (P2 L1716-1726) and P5's
  entry being sealed like any value. Split into two observations rather
  than calling six cases "differs".
- **D1's split.** Found that the model records D1's answer only at commit,
  though it decided it at prepare (model.clj `micro-prepare` keeps the
  decision in `:prepared`); a lockstep reading only recorded answers would
  have sent the rig's write after the revoke. The adapter reads the
  prepared delta at `[:prepare]`.
- **B4's landing lease.** Not settled by any plan read here: whether
  Alice's forget leaves her landing lease openable. Carried as a risk with
  both outcomes' lines stated, rather than predicted either way.

## Self-validation

Against `references/artifact-plan-validation.md`: its module checks
(query topologies, PState schemas, partitioning, topologies, production
readiness, internal depots, cross-topology and stream correctness,
in-memory state, minimality and throughput of a module) do not apply to a
plan that adds no module part; each was read and has nothing to check
here. Its spec coverage, traced:

- *"replay the model's fixed histories from scenarios.clj through the
  rig"*: all fourteen are listed with their rig plays; none is left out,
  and a step kind with no row stops a history rather than being skipped
  inside it.
- *"compare answers and reads with the model's"*: answers name by name
  through the correspondence; reads at every model read and the closing
  read, per fact of the case, plus `play`'s own comparison.
- *"Where practical"*: the three classes, each case predicted and why, and
  "not practical" named per missing API rather than silently skipped.
- *"Report every difference; a difference is a finding, not something to
  hide"*: the eighteen known differences print by number in the cases
  they touch, predictions print when not seen, and an unexplained
  difference fails the test while its line is still written.
- *"a standing practice at every step"*: the namespace compiles and runs
  before phases 2 to 5 merge, reporting what it cannot yet play.
- The ask's five parts: the adapter and the world as data (above); the
  known differences, including the four named in the ask and "For Sid" 1
  (KD1 to KD5, KD6 added); the report (above); how it runs (above); the
  APIs by stage (above).

Knock-on checks after the lockstep change: the holds are keyed to "the
model decided the next part in a later op", which the lockstep supplies;
the closing read follows `drain`, as `run` does; the model's picks come
from its sends, so no index is re-resolved on the rig.

## Open questions

For the build's first check:

1. Does `destroy-module!` and a second `launch-module!` of `Store` in one
   in-process cluster work in Rama 1.6.0, and at what cost? (RP2's
   fallback otherwise.)
2. Does pausing the micro topology block anything the model decides while
   a landing is held? In B3 and B4 those are stream-side (a value forget
   in `:alice`; a person forget in `:people` and its fan-out). If the
   fan-out reaches a copy of `$$persons` through the micro topology, the
   pause would block it, and the `:before-forward` fallback is used.
3. `promote!`'s arguments: can the request's name be known before it is
   sent, so `hold!` is armed on it?
4. Is the group readable through the exit by then (phase 5's rest)?

For Sid, none blocking, each with what the replay does meanwhile:

5. KD10: after a person's forget, nobody can read their layers through the
   exit, and they cannot read at all. It follows from defaults 1 and 3 and
   P2's L11; the model's read, being nobody's, shows erased dates. Is that
   the intended reach of forget? The replay reports it every run.
6. B4 at risk: does a person's forget reach the landing lease of a
   promotion already read out? The model says the copy lands (the forget
   is after the read-out, so it does not recall); the rig's answer rests on
   For Sid 4 (lease rows sealed under their writer). The run shows which.
