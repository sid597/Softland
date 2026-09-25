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
