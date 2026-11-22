# Plan: Auth, stage 1 of 4, `invite-and-enter`

<!-- Phase 1 Step 5. Fill in after completing Steps 1-4. After completing this, fill in PLAN_VALIDATION.md. -->

Phase 1 of the Rama skill's build of the module `Auth`, for the first of the
four stages of [DECOMPOSITION.json](DECOMPOSITION.json). [SPEC.md](SPEC.md)
is the only spec; where [IMPLICIT_SPEC.md](IMPLICIT_SPEC.md) differs from
it, SPEC.md holds. In particular SPEC.md has settled that `invite`, `enter`
and `own-layer-made` name no root and take nothing out (its first bullet of
"What takes a thing out of what is live"), that Auth refuses nothing for a
person whose layer is not made, that a repeat is recognised first and the
refusals then go in the contract's order, and that a provider is at most
256 characters and `:own-layer` a UUID.

This stage owns `open`, `invite`, `enter` and `own-layer-made`. It starts
the module: `src-auth/softland/auth/module.clj` (the module `Auth`),
`src-auth/softland/auth/client.clj` (the namespace `softland.auth.client`),
pure rules in files beside them, checks under `test-auth/softland/auth/`;
Rama 1.6.0, the Clojure API, the deps alias `:auth`.

<!-- STATUS: steps under way; sections below are filled in as each step settles. -->

## Reads
<!-- For each read operation: access method, path expression, partition -->

**Client reads owned by this stage: none.** `who-is`, `under`, `ask-of` and
`watch-root` belong to later stages. This stage's writes return their
results themselves (see Writes), so no client read is needed to finish
any of them.

**Reads that this stage's state must make possible for later stages.**
These are not designed here; the placement below is what makes them
achievable.

| Later read or write | What it needs of this stage's state | Where it finds it |
|---|---|---|
| `who-is {:root P ...}` (stage 2), every request, one round trip to one task | `:own-layer` and `:own-layer-made?` of P; nil and false for a P who waits or is unknown | `$$persons`, key P, on P's task. P is the one key that all of `who-is`'s parts share (its session may be under another sign-in of P than the one asked), so a later stage that keeps what is live under P on P's task reads these two fields in the same `local-select>`. One seek. |
| `begin-sign-in`, `make-actor`, `end-actor`, `answer-ask` (stages 2, 3) | whether P is an entered person, her own root; `:is-a-person` for `end-actor` of P | `$$persons`, key P: a non-nil `:own-layer` means entered. |
| `under P`, `watch-root P` (stages 2, 4) | nothing of this stage's state but that P exists | as above |

**Reads made inside this stage's writes** (each is a `local-select>` in the
stream topology, on the task that the event has been partitioned to):

| Inside | Read | Task | Seeks |
|---|---|---|---|
| `invite` | P's invitation: her claim, settled or under way | P's task, `$$persons` | 1 |
| `invite` | the claim's person, and whether it is used | the claim's task, `$$claims` | 1 |
| `enter` | the account's link, or its decision under way | the account's task, `$$accounts` | 1 |
| `enter` (no link) | the code's claim, then the email's claim if the code's does not hold | each claim's task, `$$claims` | 1 or 2 |
| `enter` | the person's record: her own layer, `:making`, the flag | P's task, `$$persons` | 1 |
| `own-layer-made` | the person's record | P's task, `$$persons` | 1 |

The full paths are in "PState Design".

**Latency and rates** (SPEC "The client's contract"; IMPLICIT_SPEC's
planning figures, which SPEC leaves to the plan):

- `enter`: a person waits on it after her provider's redirect. Target: a
  few milliseconds per hop, tens of milliseconds end to end at the most
  hops. Rate: about one per browser sign-in of a human per device per
  secret's life, with bursts when many invited persons enter at once. Far
  below `who-is`.
- `invite`: an admin's action or the setup command. Up to a second is
  fine. Rate: about one per person who ever enters; thousands at once when
  sign-up moves to a list.
- `own-layer-made`: once per entered person, plus repeats. Up to a second.
- `open`: once per process start; tens of copies.

## Writes
<!-- For each write operation: depot, event type -->

Each write of the contract is one depot append of one record that carries
the caller's map as given (after the client has checked that it fits),
plus the kind of write. Each returns a value computed while the record is
processed, so each is decided by the stream topology and returns through
`ack-return>` (see "Topologies and PStates", "How a write's result gets
back"). A record that does not fit is never appended: the client gives
`{:could-not :read-the-event}` and nothing, not even the event, is kept
(SPEC, "Rules for every call").

| Write | Record (Clojure record in `softland.auth.events`) | Depot | Results |
|---|---|---|---|
| `invite` | `Invite {:person P :claim C :invited-by B :at t}` | see "Depots" | `{:invited P}`, `{:refused :claim-in-use}`, `{:refused :person-in-use}` |
| `enter` | `Enter {:account [prov subj] :says {..} :email E-or-nil :code-hash H-or-nil :own-layer L :making {:uuid U :session M} :at t}` | see "Depots" | `{:person P :own-layer L' :making {:uuid U' :session M'} :own-layer-made? b}`, `{:refused :not-invited}` |
| `own-layer-made` | `OwnLayerMade {:person P :at t}` | see "Depots" | `{:made P}`, `{:refused :no-such-person}` |

`open` is no write: it makes the client (see "Client").

None of the three names a `:root`, so none takes anything out (SPEC, "What
takes a thing out of what is live", first bullet). They read and write no
sign-in and no session.

## PState Design
<!-- For each PState:
If obvious: why this is the only reasonable design
If not obvious:
  Option A: schema — query cost: N seeks, M iterations because why
  Option B: schema — query cost: N seeks, M iterations because why
  Chosen: A or B because why -->

Three PStates, one per key that this stage's writes agree across: the
person, the account, the claim. All three are maps from that key, placed
by Rama's default key partitioner (the hash of the top-level key), so
`(|hash k)` in the topology and the depots' `hash-by` reach the same task.
Two key types are Clojure records in `softland.auth.events`, so that the
schema names a class and no `Object` appears:

- `Account [provider subject]`: `provider` a keyword, `subject` the
  provider's own text, kept as given. Made by the client from the
  contract's `[provider subject]`.
- `Claim [by value]`: `by` is `:email` or `:code`; `value` is the email or
  the code's hash. A claim is its kind and its value, so an email claim
  and a code claim never name the same thing.

### `$$persons`: the record of a person, from her invitation on

```clojure
{java.util.UUID                                   ; P
 (fixed-keys-schema
   {:claim           softland.auth.events.Claim   ; her one claim
    :claim-settled?  Boolean                      ; false while an invite of hers is under way
    :invited-by      java.util.UUID               ; the inviting actor; nil means :system
    :account         softland.auth.events.Account ; the account that entered as her; nil while she waits
    :own-layer       java.util.UUID               ; fixed at her first enter; nil while she waits
    :making          (fixed-keys-schema {:uuid java.util.UUID :session java.util.UUID})
    :own-layer-made? Boolean})}
```

- Entered means `:own-layer` is non-nil. A person who waits has `:claim`
  and no `:own-layer`. An id with no entry names nobody.
- `:invited-by` is kept as given (SPEC **invite.**); the contract's two
  forms are an actor's UUID and `:system`, so nil stands for `:system`
  without loss, since every invitation has an inviter.
- `:account` and, for an email claim, `:claim` are the two fields that
  name a human being here. They are what makes a later erase of a person
  reach her other entries without a scan ("What can be erased later",
  below).
- Paths. `invite`: `(local-select> [(keypath *person)] $$persons :> *rec)`,
  one seek, and writes of `:claim`, `:claim-settled?`, `:invited-by` by
  `(multi-path ...)` with `termval`. `enter`'s step at the person and
  `own-layer-made`: the same read, then `termval` writes of the fields
  that change. Stage 2's `who-is`: `[(keypath P) (submap [:own-layer
  :own-layer-made?])]` on P's task, one seek.
- Obvious design, with one alternative weighed. The invitation and the
  entered facts share the key and the partitioner, so the skill's rule
  puts them in one PState. The alternative of keeping the two naming
  fields in a PState of their own, `:private?`, would keep them out of
  any client read; it was not taken because no requirement of the spec
  is about who reads (SPEC "Who appends") and the split costs a PState
  partition on every task for nothing that a requirement asks. The
  record is bounded (about 100 to 900 bytes: one claim, one account,
  fixed fields), so `who-is` reads a bounded amount however long her
  history is.

### `$$accounts`: the link of an account, and its decision under way

```clojure
{softland.auth.events.Account                     ; A
 (fixed-keys-schema
   {:person   java.util.UUID                      ; the link; nil while none
    :says     {String String}                     ; what the provider said at the enter decided last
    :said-at  Long                                ; that enter's :at
    :deciding (fixed-keys-schema                  ; the decision under way; nil when none
                {:uuid      java.util.UUID        ; its :making :uuid, the decision's identity
                 :session   java.util.UUID        ; its :making :session
                 :own-layer java.util.UUID        ; its offer
                 :code-hash String                ; its claims, as given (nil when none)
                 :email     String
                 :says      {String String}
                 :at        Long
                 :chosen    softland.auth.events.Claim})})  ; nil until the decision has chosen
 ;; {:private? true}
```

- `:says` is not subindexed: the client bounds it at 32 entries, an
  application-enforced bound. No read of the contract gives it back.
- `:deciding` is the one place where the account's first enter is
  decided. It exists only between an enter's first step and its end; when
  the decision ends with a link it is set to nil, and when it ends with a
  refusal the whole entry is removed (`NONE>`), since "nothing of it is
  kept but the event" (SPEC **enter.**). That removal is what the spec
  requires, so it breaks no "Nothing is deleted".
- `:private? true`: no read of the contract reads an account, and the
  entry names a human being in its key, `:says` and `:deciding`.
- Paths: `(local-select> [(keypath *account)] $$accounts :> *rec)`, one
  seek, at each of the enter's visits to the account's task; writes by
  `termval` on the fields, or `NONE>` on the key.
- Alternative weighed: keep `:own-layer`, `:making` and the flag here too,
  so that an enter by a linked account touches one task. Rejected:
  `own-layer-made` names only the person, so it would have to reach the
  account through `$$persons` (two tasks), and the flag would live in two
  places that must agree; the linked enter's one extra hop (about a
  millisecond, at the rate of browser sign-ins) costs less than a second
  copy of state that `who-is` also reads.

### `$$claims`: what each claim names, and whether it is used

```clojure
{softland.auth.events.Claim                       ; C
 (fixed-keys-schema
   {:person  java.util.UUID                       ; the person it names, for good
    :used-by java.util.UUID})}                    ; the :uuid of the decision that used it; nil while it waits
 ;; {:private? true}
```

- An entry exists only once an invite has bound the claim; its `:person`
  never changes. `:used-by` is set once and never cleared: "a used claim
  never matches again".
- `:used-by` holds the deciding enter's `:making :uuid`, not the account:
  it is what lets a repeat of the same decision recognise its own use,
  and it names no human being, so a code claim's entry holds nothing that
  must be erased.
- Paths: `(local-select> [(keypath *claim)] $$claims :> *rec)`, one seek;
  writes `(keypath *claim) :person (termval P)` and `(keypath *claim)
  :used-by (termval U)`.
- Alternative weighed: key the claims (and the accounts) by a SHA-256 of
  the value, so that no PState holds a raw email or account. Rejected:
  the spec keeps the subject "as given", a digest of an email still names
  her to anyone who has the email (so it would be erased all the same),
  and `:says` is raw in any case. It buys a smaller key and nothing that
  a requirement asks.

### What no PState holds

No PState of this stage holds anything of a refused enter, of a claim
that names nobody, or of a sign-in, a session or an actor other than the
person herself. No PState is global.

## Depots
<!-- - *name: (hash-by :key), event types [...] -->

- `*invites`: `(hash-by :person)`, records `[Invite]`. Retry mode
  `:individual`.
- `*enters`: `(hash-by :account)`, records `[Enter]`. Retry mode
  `:all-after`.
- `*own-layers-made`: `(hash-by :person)`, records `[OwnLayerMade]`.
  Retry mode `:individual`.

All three take client appends only from `softland.auth.client`, at ack
level `:ack`. None is global; no tick depot and no internal depot.

Grounds:

- **Partition keys.** Each depot is partitioned by the key of the task
  where its write is first judged, so the first step needs no hop: an
  invite is judged first at its person (see "The agreement across keys"),
  an enter at its account, `own-layer-made` at its person. `hash-by` with
  the record's field gives the same task as `(|hash k)` on the same value,
  which is the key of the PState read there. The client builds `:account`
  as the `Account` record before it appends, so the depot and `$$accounts`
  hash one value.
- **Three depots, not one or two.** Invites and enters are partitioned by
  different keys, so they cannot share a depot. `own-layer-made` could
  share `*invites` (both by person); it is kept apart because it carries
  nothing that names a human being, while every enter and every email
  invite does: an erase later rewrites a depot by a migration that reads
  the whole depot ("What can be erased later"), so the depots that hold a
  human being's names hold only what must be rewritten. No ordering is
  needed between an invite and an `own-layer-made` (the second is judged
  only on whether she has entered, which an enter decides), so the split
  loses nothing.
- **`:all-after` for `*enters`.** The enters of one account are all on
  one partition, in the order in which they were appended. `:says` must
  be "what the provider said at the enter that was decided last". Under
  `:individual`, a retried old record would be decided again after a
  newer enter of the same account and write its older `:says` over the
  newer. Under `:all-after`, a failure replays that record and every later
  record of the partition, in order, so the last decided `:says` is the
  last appended. The cost: a failure replays the rest of its partition,
  every step of which is safe to repeat (below). The risk of `:all-after`
  is a record that fails every time, which would hold back its whole
  partition; the topology is written never to throw for any record (it
  runs the client's fit check again and answers a misfit without writing),
  so only a fault of Rama or of a machine fails a record, and those pass.
- **`:individual` for the two others.** Neither has a field whose value
  depends on the order of the same person's records: an invite's repeat
  and its refusals are decided by state, and `own-layer-made` sets a flag
  that never goes back.

## Topologies and PStates
<!-- Each topology owns zero or more PStates. Only the owning topology can write to a PState.
A module can have multiple topologies of different types.

TOPOLOGY TYPE RULES:
- Default is microbatch. Every topology is microbatch unless one of these forces stream:
  (a) millisecond-level update latency — the caller needs PState changes visible immediately
  (b) ack coordination — the caller needs to block or receive a return value via the depot append
- A single user-facing operation often has multiple processing concerns with different latency
  requirements. Split them by type — the latency-sensitive part belongs in the stream topology,
  everything else in the microbatch topology. Multiple topologies can consume the same depot
  independently, or the stream topology can communicate to the microbatch topology with an
  internal depot.

TOPOLOGY COUNT RULES:
- Extending an existing module: add to the topologies it already has. Do NOT add a topology
  per feature, per subsystem, or per build stage. A new topology permanently partitions PState
  write access and cannot be undone without a migration.
- Declare a topology only when work requires it. A module with no ETL — mirrors plus query
  topologies — has none at all.
- At most ONE stream topology. Stream topologies all share one performance profile — an event
  takes a few milliseconds end to end — so a split isolates nothing and only costs write access:
  a PState is declared on one topology and only that topology can write it, so a later feature
  needing to write an existing PState from a new event cannot. If you declare a second, state
  what forces it.
- Split microbatch topologies ONLY with arithmetic: state each concern's iteration time and show
  they differ by an order of magnitude. A microbatch cycle is as slow as the slowest work in it,
  so a 0.5s computation colocated with a 5s one takes 5s. Feature boundaries and differing "feel"
  are NOT reasons — nearly all derived-view work is in one latency class.

For each topology, list:
- topology-name: microbatch | stream
- Why this type: cite which stream reason applies, or state "default microbatch"
- For each processing concern in the topology: does it actually require this topology type?
  If any concern does not need the topology's stream/latency guarantees, it belongs in a
  separate microbatch topology.
- If stream: list each write and whether it is idempotent or non-idempotent.
    Non-idempotent writes in stream topologies will duplicate on retry.
    Move non-idempotent writes to microbatch, make them idempotent, or explain the dedup mechanism.
  PStates:
    - $$name: full typed schema — no Object anywhere -->

### `decide`: stream

The module's one stream topology. This stage declares it with three
sources; the later stages add their sources to it rather than declaring
another (the skill: at most one stream topology, and a second one would
fix for ever which events may write which PStates).

- **Why stream.** Both of the template's reasons apply to every write of
  this stage. (b) The contract has each write return a value computed
  while it is processed (`{:invited P}` or a refusal; the person, her own
  layer, `:making` and the flag; `{:made P}`), and returns "when its
  effect can be read by any copy of the runtime": `ack-return>` with an
  `:ack` append gives both in one call. (a) A person waits on `enter`
  after her provider's redirect. A microbatch topology cannot serve this
  stage: its `:ack` confirms the depot only, and it has no `ack-return>`;
  a client that polled state for its result would find nothing for a
  refused enter, because "nothing of it is kept but the event".
- **Sources.**
  - `(source> *invites {:retry-mode :individual} :> *e)`
  - `(source> *enters {:retry-mode :all-after} :> *e)`
  - `(source> *own-layers-made {:retry-mode :individual} :> *e)`
- **Every concern here needs the stream's ack**: each of the three is a
  write whose caller waits on its result. There is no derived view, so
  nothing belongs in a microbatch topology.
- **It never throws for a record.** Each source first runs the client's
  pure fit check (`softland.auth.fit`) on the record; a misfit gives
  `(ack-return> {:could-not :read-the-event})` and writes nothing. Only
  the client appends, and it never appends a misfit, so this branch runs
  only for an append made around the client; it is there so that no
  record can be retried for ever, which under `:all-after` would hold
  back a partition.
- **PStates owned:** `$$persons`, `$$accounts` (`:private? true`),
  `$$claims` (`:private? true`), with the schemas of "PState Design".

**Each write in the topology, and why it is right when processed twice.**
Every write below is a `termval` or a `NONE>` that is taken only when the
state read just before it on the same task says so. Nothing appends to a
list and nothing counts. A retry, a second record of the same write, and
a second enter that drives another's decision (below) each find the state
that the first left and take the same branch or a later one.

| Write | Task | Taken when | Twice |
|---|---|---|---|
| P's claim `C`, unsettled, with `:invited-by` | P | P has no entry | the second finds the entry and does not write |
| `$$claims[C] :person P` | C | C has no entry | the second finds P and goes on as bound |
| P's claim settled | P | P's claim is C, unsettled | `termval true` again |
| P's entry removed | P | P's claim is C, unsettled, and C names another | the second finds no entry, or another claim, and does nothing |
| `$$accounts[A] :deciding D` | A | A has no link and no decision | the second finds D and drives D |
| `D :chosen c` | A | D is the decision there and has not chosen | the second finds the choice and follows it |
| `D :chosen nil` | A | D chose c and c is used by another decision (which never undoes) | the second finds no choice, or another, and re-judges |
| `$$claims[c] :used-by U` | c | c is unused | the second finds U and goes on |
| P's own layer, `:making`, `:account`, flag false | P | P has no own layer | the second finds them and reads them |
| A's link, `:says`, `:said-at`; `:deciding nil` | A | D is the decision there | the second finds the link and does not write D's `:says` |
| A's entry removed | A | D is the decision there and found no claim | the second finds no D |
| A's `:says`, `:said-at` | A | A has a link (the linked enter) | the same values; `:all-after` keeps a later enter's last |
| P's flag true | P | P has an own layer | `termval true` again |

Commit points are the partitioners: each hop commits what was written on
the task it leaves. Every state that a failure between two hops can leave
is one that the next attempt continues from ("The agreement across
keys").

### How a write's result gets back

1. The client appends the record with `(foreign-append! depot record
   :ack)`.
2. The topology decides it, hopping between the tasks of its keys, and
   calls `ack-return>` once, on the task of its last step, with the
   contract's result map.
3. Rama answers the append when the record's whole event tree has ended,
   and with `:ack` that is after every task's writes of that tree have
   been committed and replicated: from then on a read through any copy of
   the runtime sees the effect (stream "Read-after-write"). The answer is
   the map `{"decide" result}`; the client gives `(get answer "decide")`.
4. A Rama exception or a timeout of the append gives `{:could-not
   :reach-the-module :threw e}`; an answer without a `"decide"` value
   gives the same word. Either says nothing of whether the write was
   taken. The runtime then sends the same map again, which is a new
   record of the same write: `invite` finds its repeat at the person and
   gives `{:invited P}` (or the refusal that holds now); `enter` finds the
   link, or the decision under way, and gives the person; `own-layer-made`
   gives `{:made P}`.

### Client (`softland.auth.client`)

- `(open cluster module-name)`: takes the foreign handles of the three
  depots by name (later stages add theirs to the same client). A module
  that is not on the cluster gives `{:could-not :find-the-module ...}`; a
  cluster that does not answer gives `{:could-not :reach-the-module ...}`.
  The client is a map of Rama's foreign handles, which are safe to share
  across threads; it holds nothing true about Auth's state.
- Each write: check the map against `softland.auth.fit` (the contract's
  bounds; provider at most 256 characters; `:own-layer`, the `:making`
  ids and the person ids UUIDs; a hash 64 lower-case hex characters; a
  claim one of its two forms; `:invited-by` a UUID or `:system`), make
  the record (the `Account` and `Claim` records from the contract's
  forms), append, give the result. A misfit gives `{:could-not
  :read-the-event}` and appends nothing. Every call is wrapped so that
  nothing throws.
- One call from the client for each write, and one round trip: the append
  carries the result back.

## The agreement across keys

An invite touches two keys (the person, the claim) and an enter three (the
account, a claim, the person). They hash to different tasks, and no task
may be one that every person's writes pass through. Rama gives no atomic
write across tasks in a stream topology, retries an event from its source
without undoing what other tasks committed, and may run two attempts of
one record at once. The design therefore gives each agreement **one task
that decides it**, keeps the decision under way as state on that task,
and makes every step a conditional write that any later attempt can take
again or skip:

- an invite is decided at the **person's** task; the claim's entry is
  written once, finally, and is never "under way";
- an account's first enter is decided at the **account's** task; the
  account chooses one claim there before any claim is used.

Any record that meets a decision under way **drives it to its end**, then
judges itself. This is how a write that cannot wait (a stream event cannot
pause) still gives a true answer, and how a decision that lost its own
attempt to a failure is finished by the next one. Rama delivers the events
that one task sends another in the order they were sent (stream.md,
"Partition ordering"); the design is right without that, and uses it only
in the traces to fix one order of arrival.

### invite `{:person P :claim C :invited-by B :at t}`

- **I1, at P** (the source task). Read P's entry.
  - Her claim is C, settled: `{:invited P}` (a repeat; nothing written).
  - Her claim is X, not C, settled: hop to C and read it. C names a person:
    `{:refused :claim-in-use}`; else `{:refused :person-in-use}`. (A repeat
    first, then the contract's order.)
  - Her claim is X, not C, unsettled (another invite of P under way): drive
    it, I2 and I3 for (P, X) with the entry's `:invited-by`, then judge
    (P, C) again at P.
  - No entry: write claim C, unsettled, `:invited-by` B; go to I2. Her claim
    is C, unsettled (this write under way): go to I2, writing nothing.
- **I2, at C.** C names P: bound. C names Q, not P: lost. No entry: write
  `:person P`: bound. Hop to P.
- **I3, at P.** Bound: settle her claim if it is still unsettled;
  `{:invited P}`. Lost: if her claim is C, unsettled, remove her entry;
  `{:refused :claim-in-use}`.

**What holds (J).** A claim's entry names P only while P's entry holds
that claim. I2 writes `:person P` only after I1 has written or found P's
claim C; P's unsettled claim C is removed only when C names another
person, which never changes; so no late I2 can bind C to P after her claim
was removed. Two things follow: a settled claim at P proves that C names
P, so a repeat is answered at P alone; and an enter, which reads only
claims, never meets an invitation half made.

**Races of "A claim names one person for good" and "A person has one
claim".**

- *Two invites of P and of Q under one claim C, at once.* Their records are
  on P's and Q's partitions. Both write an unsettled claim C at their
  person. At C the first to arrive binds, say P's; Q's finds P: lost; at Q
  her entry is removed. Results: `{:invited P}`, `{:refused
  :claim-in-use}`. One stands, and Q is left as she was.
- *Two invites of P under C1 and under C2, at once.* Both records are on P's
  partition and are judged at P in order. (P, C1) writes C1 unsettled and
  goes to C1. (P, C2) finds C1 unsettled and drives (P, C1): C1 has no
  entry, so it binds C1 to P; at P it settles C1; then it judges (P, C2)
  again, finds C1 settled, reads C2 (no entry): `{:refused
  :person-in-use}`. (P, C1)'s own steps find C1 bound and settled:
  `{:invited P}`. Had C1 named Q, the drive would have found it lost and
  removed P's entry, and (P, C2) would then be taken: the order in which
  (P, C1) is refused and (P, C2) is taken.

### enter `{:account A ... :making {:uuid U :session M} :at t}`

D is the decision under way at A, named by the `:uuid` of the record that
began it. "This record" is the enter being processed.

- **E1, at A** (the source task, and the start of each new round). Read A's
  entry.
  - A links to P: write this record's `:says` and `:said-at`; hop to P; read
    her entry; give `{:person P :own-layer :making :own-layer-made?}` from
    it. (The linked enter: no claim is looked at.)
  - D is there: drive D: to E3 with D's choice if D has chosen, else to E2.
  - Nothing there, and this record names no claim: `{:refused
    :not-invited}`, nothing written.
  - Nothing there: write `:deciding` D from this record (its `:uuid`,
    `:session`, `:own-layer`, `:code-hash`, `:email`, `:says`, `:at`); go to
    E2.
- **E2, the probe of D's claims.** Hop to D's code claim, if any, and read
  it: usable for D when it names a person and is unused or used by D's
  `:uuid`. If not usable, hop to D's email claim, if any, the same. Hop to
  A with the choice: the first usable claim, or none.
- **E2', at A, the choice.** Read A's entry.
  - D is not there any more (it ended, or another decision is there): go
    to E1.
  - D has chosen c (another attempt chose first): to E3 with c.
  - D has not chosen, and the probe chose c: write `:chosen c`; to E3.
  - D has not chosen, and the probe found none: remove A's entry (it holds
    only D, since A has no link). If this record is D's own (its `:uuid`
    is D's), or names the same claims as D: `{:refused :not-invited}`.
    Else go to E1, where this record is judged on its own claims.
- **E3, at c, the use.** Read c. Unused: write `:used-by` D's `:uuid`; hop to
  c's person P; E4. Used by D's `:uuid`: E4. Used by another: hop to A; if
  D is there with `:chosen c`, write `:chosen nil`; go to E1.
- **E4, at P.** Read her entry. No own layer: write D's `:own-layer`,
  `:making {:uuid :session}`, `:account` A, the flag false, and settle her
  claim if it is unsettled. Read her own layer, `:making` and the flag. Hop
  to A.
- **E5, at A, the link.** Read A's entry. If D is there: write `:person P`,
  D's `:says` and `:at` as `:says` and `:said-at`, `:deciding nil`. Then,
  this record being D's own: give the person's fields read at E4. This
  record having driven another's D, it now stands as a linked enter:
  write its own `:says` and `:said-at`, and give the fields read at E4. If
  D is not there, A links to P (D can end no other way once it has used a
  claim, below): give the fields; write this record's `:says` only if it
  is not D's own.

**What holds.**

- *(K) One decision uses one claim at most.* A claim is used only at E3,
  which follows the choice written at A. A has one choice at a time, and a
  choice is withdrawn only for a claim that another decision has used,
  for good, so D never uses the withdrawn claim. Once D has used a claim,
  `:chosen` is never withdrawn and D is never removed, so D ends only with
  the link to that claim's person.
- *(L) Never a link while the claim waits.* The link is written at E5,
  after the use (E3) and her entering (E4) have committed.
- *(M) One own layer and one `:making` per person.* They are written at E4
  only while she has none; she has one claim, and the claim one user, so
  only one decision reaches her.
- *A late attempt does no harm.* An attempt that reaches A after its D
  ended judges itself from what A holds now. A use reaches c only after a
  choice that was at A when the attempt left A; since a choice is withdrawn
  only for a claim used by another, a late use of a withdrawn choice fails
  at c.
- *The rounds end.* Each new round at A follows a decision's end or a
  withdrawn choice. A decision withdraws at most as many choices as it has
  claims (two), and a new decision comes only from another record of the
  same account, so the rounds are bounded by that account's enters in
  flight.

### Traces of the races that SPEC names under **enter.**, "What must hold"

**1. Two enters of one new account at the same moment, through two
copies.** Records E (offer L_E, U_E) and F (offer L_F, U_F), one account
A, one claim C that waits for P. Both are on A's partition and are judged
at A in the order appended; say E first.

| # | Task | E | F |
|---|---|---|---|
| 1 | A | nothing there: writes D = E's decision; probes | finds D (E's): drives D; D has not chosen: probes D's claims |
| 2 | C | C names P, unused: usable | usable |
| 3 | A | D has not chosen: writes `:chosen C` | D has chosen C: follows it |
| 4 | C | unused: writes `:used-by U_E` | used by U_E, which is D's: goes on |
| 5 | P | no own layer: writes L_E, U_E, account A, flag false; settles her claim | own layer L_E: reads it |
| 6 | A | D there: link to P, `:says` E's; D ends | D gone, link to P: F is not D's own: writes F's `:says` |
| | result | `{:person P :own-layer L_E :making {U_E M_E} :own-layer-made? false}` | the same |

One person, one own layer, one `:making`; F's offer is dropped. Had F's
steps arrived first at any task, the table's rows hold the same: at row 3
whichever choice is written first is followed by the other, and at rows 4
and 5 the second finds what the first wrote.

**2. Two accounts that prove one claim at the same moment.** Accounts A
and B, records on two partitions, one claim C that waits for P.

| # | Task | A's record | B's record |
|---|---|---|---|
| 1 | A / B | writes D_A; probes C | writes D_B; probes C |
| 2 | C | usable | usable |
| 3 | A / B | `:chosen C` | `:chosen C` |
| 4 | C | arrives first: unused: `:used-by U_A` | used by U_A, not U_B: back to B |
| 5 | P / B | enters P with D_A's offer | D_B chose C: `:chosen nil`; new round: probe: C used by another; B's email claim, if any, the same; none: B's entry removed |
| 6 | A / – | link to P | |
| | result | `{:person P ...}` | `{:refused :not-invited}` |

One becomes the person and the other is refused; had B proved a second
claim that waits, its new round would take that one. Nothing of B is kept
but its event.

**3. An enter that is run again after a failure gives what it gave.** The
runtime sends the same map again after a `:could-not` (the same `:uuid`
and offer), or Rama replays the record. By where the first attempt was
cut:

| Cut after | What A, C, P hold | The next attempt | Result |
|---|---|---|---|
| its end (only the answer was lost, or Rama replays a record whose writes all committed) | link to P | the linked enter | P, the fixed own layer and `:making`, the present flag |
| E1 (D written) | D, no choice | drives D: probe, choice, use, E4, E5 | what the first would have given |
| E2' (`:chosen c`) | D chose c | E3: c unused or used by D: E4, E5 | the same |
| E3 (`:used-by` D) | c used by D | E3 finds D's use: E4, E5 | the same, D's offer |
| E4 (she has entered) | P has D's offer, no link | E3, E4 reads, E5 links | the same |
| a refusal | nothing but the event | judged again | refused again, unless an invite has since made a claim that it proves wait: a refusal binds nothing |

A replay under `:all-after` also replays the records after it on the
partition, in order, so `:says` ends as the last appended enter's.

### Other races of this stage

- **One new account that proves C1 (person P1) through one copy and C2
  (P2) through another, at once.** E (C1) writes D first; F finds D,
  drives it to the link to P1, then finds the link and gives P1. C2 is
  never used and stays waiting. Had D been refused (C1 not waiting), F,
  whose claims differ, is judged on its own and may take P2.
- **The precedence of the code over the email, while an invite lands.** D
  has a code claim c1 that names nobody at its first probe, and an email
  claim c2 that waits. Attempt X probes (c1 none, c2 usable) and writes
  `:chosen c2`; an invite then binds c1; attempt Y probes (c1 usable) but
  finds `:chosen c2` at A and follows it. Only c2 is used; c1 keeps
  waiting. This case is why the choice is written at A before any use:
  without it X would use c2 and Y c1, two claims for one account.
- **An invite racing an enter that proves the same claim.** The enter's
  probe at C before the invite's I2: C has no entry, the enter takes
  another claim or is refused, and the invite then binds C. After it: the
  enter uses C, and at E4 her claim may still be unsettled (I3 not yet
  arrived): E4 settles it, and I3 then finds it settled and gives
  `{:invited P}`. No link while the claim waits (L).
- **`own-layer-made` racing an enter of the same person.** Both are judged
  at P. The enter's flag is the one read at E4: false or true by that
  order.

## Query Topologies
<!-- - name:
  Input example 1: describe input → N total reads, M meaningful (non-empty) reads
  Input example 2: different input → N total reads, M meaningful (non-empty) reads
  Fixed or variable: fixed if M is same for all inputs, variable if M differs
  If variable, dynamic approach: ops/explode + aggregator | loop<- | etc. -->

None in this stage. It owns no client read: each of its writes returns
its result through the append, and the reads that its writes need are
`local-select>` calls inside `decide`, on the task the event has reached.
The reads that will read this stage's state (`who-is` above all) belong
to stage 2, which chooses their access method; what this stage gives them
is that everything of a person that `who-is` needs from it is one seek of
`$$persons` on P's task.

The steps of an enter vary in number with its input, and the topology
issues only the steps that input needs (the reads below are all
meaningful: each reaches an entry that decides the outcome):

| Input | Tasks visited (each one seek) | Hops |
|---|---|---|
| a linked account | A, P | 1 |
| a new account, no claim given | A | 0 |
| a new account whose code's claim waits (or email only, and it waits) | A, c, A, c, P, A | 5 |
| a new account whose code's claim does not hold and whose email's waits | A, c1, c2, A, c2, P, A | 6 |
| a new account that proves no claim | A, c1 (, c2), A | 2 or 3 |

The variable part is handled by `<<if` branches on what each read found,
inside a `loop<-` whose body starts at A (a new round goes back to A with
`continue>`; partitioners inside a `loop<-` body suspend and resume it,
dataflow.md). An invite: a repeat 1 task; a new one P, C, P; a
person-in-use P, C; a claim-in-use P, C, P. `own-layer-made`: P.

## Partitioning efficiency
<!-- FIRST derive the optimal placement, THEN derive the partitioners from it, THEN validate with the table.

**Optimal placement (do this first, before choosing any partitioner).** State the placement the dominant read wants: for each key, the set of task(s) its data should live on — `f(key) → task(s)` — that minimizes total seeks for that read while keeping aggregate load balanced across tasks. THEN derive the partitioner(s) that implement that `f`: `|hash` = `hash(k) mod N`, `|all` = every task, `|direct` = any `f` you compute or store (see `pstate-schema.md` "Partitioning control"). Do NOT start from a partitioner and ask "is it good enough" — start from `f` and implement it.

**Validate with the table.** For the dominant read operation, build the table below at THREE cluster sizes: N = 1, N = 16, and N = 128 tasks. N = 1 is the single-task baseline (all data on one task).

Rows are DATA CATEGORIES — categorizer the data by each input regime, so the table includes all kinds of data, both common and infrequent. "Frequency proportion" is the fraction of operations that hit that category; proportions MUST sum to 1. "Seeks/op" is the **total number of tasks the operation reads from**, summed across the whole cluster — one local read (seek) per task touched. A read that fans to all N tasks costs **N**, even if some tasks' local slice is empty (the read is still dispatched there). This is a TOTAL across tasks — do NOT count per-task or divide by the task count (a per-task number falls as N grows for any design and measures nothing). "Iterator reads/op" is likewise the total elements iterated across all tasks.

Fill the table for each N and compute the two weighted sums:
  Weighted seeks          = Σ(proportion × seeks/op)
  Weighted iterator reads = Σ(proportion × iterator-reads/op)

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| <input category 1>  | <0–1> | <#> | <#> |
| <input category 2>  | <0–1> | <#> | <#> |
Weighted seeks = <#>   |   Weighted iterator reads = <#>

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| <input category 1>  | <0–1> | <#> | <#> |
| <input category 2>  | <0–1> | <#> | <#> |
Weighted seeks = <#>   |   Weighted iterator reads = <#>

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| <input category 1>  | <0–1> | <#> | <#> |
| <input category 2>  | <0–1> | <#> | <#> |
Weighted seeks = <#>   |   Weighted iterator reads = <#>

If weighted seeks grow substantially from N=1 to N=128, the partitioning is INEFFICIENT — redesign (consider creative `|direct` placement). A good design keeps weighted seeks roughly flat as N grows. -->

**Optimal placement.** Every read of this stage, and the read of every
request that will read its state (`who-is`), names one key and wants
everything of that key on one task. So for each key the placement is one
task: `f(P) = hash(P) mod N` for `$$persons`, `f(A) = hash(A) mod N` for
`$$accounts`, `f(C) = hash(C) mod N` for `$$claims`. That `f` is exactly
`|hash` (and the PStates' default key partitioner), so no `|direct` and
no stored placement is needed.

- Many keys per task: persons, accounts and claims are each millions, so
  at N = 128 a task holds tens of thousands of each, and hash variance is
  about 1% (`√(2·ln 128 / 78,000)`).
- No skewed key: each person, account and claim holds one bounded record,
  and no key takes a disproportionate share of events (one human's enters
  are one per sign-in; one person's invites are one, plus repeats).
- Stored placement was considered for one case: putting a person's record
  on her claim's task, which would make an invite a one-task decision.
  Rejected on cost: P's task would then be known only through a lookup
  `P -> task`, a second seek and a second hop on every `who-is`, which
  must be one round trip to one task. The invite's extra hops (two, at the
  rate of admin actions) cost far less.

The dominant operation of this stage is `enter` (its only operation a
person waits on; the stage has no client read). Proportions: almost every
enter is a later sign-in of a linked account; the first enter happens once
per person.

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| linked account | 0.960 | 2 | 0 |
| new account, one claim that holds | 0.030 | 6 | 0 |
| new account, code fails, email holds | 0.002 | 7 | 0 |
| new account, claims given, none holds | 0.006 | 4 | 0 |
| new account, no claim given | 0.002 | 1 | 0 |
Weighted seeks = 2.14   |   Weighted iterator reads = 0

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| linked account | 0.960 | 2 | 0 |
| new account, one claim that holds | 0.030 | 6 | 0 |
| new account, code fails, email holds | 0.002 | 7 | 0 |
| new account, claims given, none holds | 0.006 | 4 | 0 |
| new account, no claim given | 0.002 | 1 | 0 |
Weighted seeks = 2.14   |   Weighted iterator reads = 0

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| linked account | 0.960 | 2 | 0 |
| new account, one claim that holds | 0.030 | 6 | 0 |
| new account, code fails, email holds | 0.002 | 7 | 0 |
| new account, claims given, none holds | 0.006 | 4 | 0 |
| new account, no claim given | 0.002 | 1 | 0 |
Weighted seeks = 2.14   |   Weighted iterator reads = 0

Flat from N = 1 to N = 128: no step fans out, and each step reads one
entry of one key. (At N = 1 the hops are local, but each visit still reads
its entry, so the count is the same.) The read of every request that this
stage's state serves, `who-is`'s part, is one seek of `$$persons` at any
N. `invite` (1 to 3 seeks) and `own-layer-made` (1) are flat likewise.

## Design Decisions
<!-- - Subindexing: which PState collections are subindexed and why
- Colocation: how depot partitioning aligns with PState keys -->

- **Subindexing: none.** No location in this stage's PStates grows: a
  person has one claim and one account; an account one link, one `:says`
  of at most 32 entries (a bound the client enforces), and one decision at
  a time; a claim one person and one user. Each is read whole at each
  visit, which is what every step needs.
- **Colocation.** Each depot's first step is on the task of the PState it
  reads first: `*invites` and `*own-layers-made` by `:person` on
  `$$persons`' task, `*enters` by `:account` on `$$accounts`' task. The
  other keys of a write are reached with `(|hash k)` on the same value
  that keys the PState, which is the PState's own placement. `$$persons`
  is keyed by the person id alone, so whatever a later stage keeps under a
  root, keyed by the root's id, lands on the same task with the same
  `|hash`.
- **No task that everything passes through.** No `|global`, no global
  PState or depot, no `|all`. Each write touches the tasks of its own
  keys only: an invite its person's and its claim's; an enter its
  account's, at most two claims', and one person's; `own-layer-made` its
  person's.
- **One order of a person's events.** Every write that changes a person
  (I1, I3, E4, `own-layer-made`) is judged at her task, which runs one
  event at a time; so her events are decided in one order. The same holds
  of an account at A and of a claim at C.
- **No clock.** No step reads a clock; `:at` is carried from the record
  into `:said-at` and the decision's `:at`, and no rule of this stage
  depends on a time.
- **No ids made by Auth.** Every id is the record's; Auth makes none
  (unique-ids.md is not needed).

## State primitive selection
<!-- For every piece of state in the design, name the storage class and justify:
- $$name (PState): per-source-event write volume O(?) in <bounded inputs>. Durable.
- *cache (TaskGlobal): non-durable. Rebuild by reading from PState on each cache miss. Cite the code that does it.
- external: <name>  -->

- `$$persons` (PState): the source of truth of who is a person and of her
  own layer. Writes per source event: O(1), at most one entry (bounded
  fields). Durable.
- `$$accounts` (PState): the source of truth of the links; its
  `:deciding` must survive a crash, since the next attempt continues from
  it. Writes per source event: O(1) per visit, at most four visits.
  Durable.
- `$$claims` (PState): the source of truth of what each claim names and
  whether it is used. Writes per source event: O(1). Durable.
- No TaskGlobal: there is no derived state to cache, and the decisions
  under way must be durable.
- No external system: Auth reads no clock, no provider and not the store
  (SPEC: "Auth never reads the store").

## Resource usage analysis
<!-- For each storage location (PState, TaskGlobal), estimate the resource footprint per task under load. The goal is to minimize memory and disk usage while staying within latency constraints.

### Disk usage (PStates)
For each PState and depot, estimate bytes per entry and total size per task:
- Entry size: key size + value size (include all fields, nested structures, index overhead for subindexed structures)
- Growth rate: how many entries per unit time
- Total per task: entries × entry size / number of tasks

### Memory usage (TaskGlobals)
For each TaskGlobal, estimate:
- Entry size in bytes: count every field stored per entry. Use primitives (long, int) instead of boxed objects (Long, Integer) where possible.
- Entries per task: worst-case count
- Total memory per task: entries × entry size
- GC pressure: large object graphs with many small maps/vectors create GC overhead. Flat structures, primitive arrays, or compact representations MUST be used when possible since GC causes long pauses which degrade latency-sensitive operations like foreign reads.

### Minimization
For each storage location, state whether the current design is minimal or whether data could be reduced:
- Can any of the data in the cache be fetched from a PState at query time instead of cached in memory without violating latency requirements?
- Can fields be stored as primitives instead of objects?
- Can per-entry overhead be reduced by using arrays or packed representations instead of maps?
- Does the design duplicate data across storage locations? If so, justify why (latency constraint) or eliminate. -->

Planning figures: 10 million persons (SPEC "Scale": millions), 128 tasks,
about 25 enters per person per year (two devices, a secret's life of about
a month). Sizes are serialized bytes, before RocksDB's compression and
before replication.

### Disk usage (PStates)

| PState | Entry, typical | Entry, worst | Entries | Total | Per task (128) |
|---|---|---|---|---|---|
| `$$persons` | ~300 B (UUID key; claim ~60; account ~60; own layer, `:making`, `:invited-by` 64; flags; record tags) | ~2.5 KB (a 256-character email, provider and subject) | one per person invited, ~10 M | ~3 GB | ~23 MB |
| `$$accounts` | ~700 B (key ~60; `:says` ~600 for about ten entries; person, `:said-at`) | ~220 KB (`:says` at 32 × (256 + 2,048) characters) | one per linked account, ~10 M; a refused account none | ~7 GB | ~55 MB |
| `$$claims` | ~130 B (key ~80; two UUIDs) | ~800 B | one per bound claim: persons plus claims never proven, ~10 to 20 M over years | ~1.3 to 2.6 GB | ~10 to 20 MB |

Growth: `$$persons` and `$$claims` by one entry per invitation, which is
bounded by people inviting people; `$$accounts` by one per first enter.
None grows with sign-ins: a later enter rewrites one entry in place.

### Depots

| Depot | Record, typical | Records | Growth | Per task per year (128) |
|---|---|---|---|---|
| `*enters` | ~850 B (account, `:says`, email, code hash, offer, `:at`) | ~25 per person per year | ~250 M a year, ~210 GB a year | ~1.7 GB |
| `*invites` | ~150 B | ~1 per person, plus repeats | ~1.5 GB over 10 M persons | small |
| `*own-layers-made` | ~60 B | ~1 per person, plus repeats | ~0.6 GB over 10 M persons | small |

`*enters` is the one that grows without bound. Nothing in this build
trims it; Rama's trimming (`depot.max.entries.per.partition`) keeps it
possible later, since no topology of this stage ever reads a depot from
its start.

### Memory usage (TaskGlobals)

None. The only memory beyond Rama's own is the event trees in flight: a
decision under way carries one record (~1 KB) for a few milliseconds.

### Minimization

- `$$persons` holds two fields that repeat what another PState's key
  holds: `:account` (the key of `$$accounts`) and `:claim` (the key of
  `$$claims`). Kept: without them a later erase of a person would have to
  scan every account to find hers (no read of the contract needs them;
  "What can be erased later" does). About 120 B per person.
- `$$accounts` holds `:says` once, the latest only; earlier ones are in
  the events alone, as SPEC says. The decision under way holds a second
  `:says` for the milliseconds of a first enter.
- Fixed fields are `Long`, `Boolean` and `UUID`; the records' sizes are
  dominated by the provider's text, which the spec keeps as given.
- Nothing is cached, and no read of this stage's writes reads more than
  one entry per task visited.

## Design difficulty log
<!-- An honest, first-person record of where this design was hard to settle. Write it as you design, not as a summary. For each decision that was genuinely contested:
- The decision (which PState schema, partitioning, placement, or topology).
- The competing approach(es) you weighed, and how close the call was — was an alternative genuinely competitive, or was the choice forced once you costed it against the requirements?
- What finally settled it.
If nothing was contested — every choice was forced once the requirements were read — say so plainly and briefly. Do not manufacture difficulty; do not hide it. -->

Given as each contested decision and its grounds.

1. **Where an invite is decided: at the person, with the claim written
   once.** The other way, deciding at the claim with a reservation there,
   costs the same hops. It was rejected because a reserved claim is
   something that enters read: an enter or another account would meet a
   claim half bound and would have to drive an invite. Deciding at the
   person keeps every claim entry final, so the race that happens in use
   (two admins, or two runs of the setup command, inviting one email as two
   persons) is settled by one conditional write at C, and the state under
   way sits at the person, where only two invites of the same person id
   meet it. Close call on cost, not on what it leaves the enter.
2. **An enter's claim is chosen at the account before any claim is
   used.** The cheaper way (use the first claim that holds, as the probe
   finds it) saves two hops on a first enter. It was rejected because two
   attempts of one decision can then use two claims when an invite lands
   between them (trace "The precedence of the code over the email"): one
   account, two persons, which "An account has one person at most"
   forbids. Not close once that trace exists.
3. **A record that meets another's decision drives it and then judges
   itself, rather than taking its outcome.** Taking the outcome is
   simpler, and right when the decision ends with a link. When it ends
   with a refusal and the second record proves another claim, taking the
   outcome refuses an enter whose own claim waits, which no order of the
   two enters gives. So the second record takes the outcome only when its
   claims are the same; else it is judged on its own.
4. **Stream, not microbatch, for every write here.** Forced: the contract
   returns a result from each write and returns when its effect can be
   read; a microbatch's `:ack` confirms the depot only, and a refused
   enter leaves no state that a poll could find.
5. **`:all-after` for `*enters`.** `:individual` is Rama's default and
   confines a failure to its record. `:all-after` was taken so that a
   replay never leaves an older `:says` as the last; its cost, a replay of
   the rest of the partition, is safe because every step is, and its risk,
   a record that always fails, is met by the topology never throwing.
   Close; it turns on a field that no read gives back, so it is listed as
   a fork.
6. **The own layer, `:making` and the flag at the person only.** A copy
   on the account would save the linked enter's one hop; it was rejected
   because `own-layer-made` names only the person and `who-is` reads the
   flag at the person, so a copy is a second truth to keep in step for a
   saving of about a millisecond at the rate of sign-ins.
7. **Raw accounts and emails as keys, not digests.** See `$$claims` in
   "PState Design": a digest does not remove the need to erase, and the
   spec keeps the subject as given.

## What can be erased later

SPEC: what names a human being (her account, her email, what her provider
said) must be erasable later from everything Auth keeps, its events too;
nothing of it is built here. This is where it lives, and what a later
erase would do to each place.

**Where it lives, and nowhere else.**

| Place | What names her | What else is there |
|---|---|---|
| `$$accounts[A]` | the key A; `:says`; while a first enter is under way, `:deciding`'s `:email` and `:says` | the link to P, `:said-at` |
| `$$claims[C]`, C an email claim | the key (the email) | `:person`, `:used-by` (a decision's UUID) |
| `$$claims[C]`, C a code claim | nothing (the key is a code's hash) | the same |
| `$$persons[P]` | `:account`; `:claim` when it is an email claim | her own layer, `:making`, the flag, `:invited-by` |
| `*enters`, every record, a refused one's too | `:account`, `:email`, `:says` | `:code-hash`, the offer, `:at` |
| `*invites`, a record whose claim is `{:by :email}` | the email | `:person`, `:invited-by`, `:at` |
| `*own-layers-made` | nothing | `:person`, `:at` |

No result of a write carries any of it (an enter gives the person, her own
layer and `:making`), no id is derived from it (Auth makes no id; the
runtime's person ids are random, "a random UUID that says nothing"), and no
depot of a later stage needs it.

**Finding all of one human's entries is point reads, never a scan.**
From her account: `$$accounts[A] :person` gives P, then `$$persons[P]`
gives her claim. From her person: `$$persons[P]` gives `:account` and
`:claim`. From her email: `$$claims[email] :person` gives P. These are
the reason `$$persons` holds `:account` and `:claim`.

**What a later erase would do.**

- **PStates.** An erase is a write of `decide` (the owner) from a depot
  that a later build adds, decided at the three keys' tasks as an enter
  is. It removes `$$accounts[A]` with `NONE>` (the entry is the link and
  the provider's text, nothing else); removes or rewrites `$$claims[C]`
  for an email claim (what keeps the claim "used for good" afterwards, if
  anything, is that build's choice); and sets `$$persons[P] :account`,
  and `:claim` when it is an email claim, to nil. What stays is the
  person, her own layer, `:making`, the flag and `:invited-by`: none of
  them names her, and none is found only through an account or an email.
  No PState migration is needed: it is an ordinary write of entries by
  key.
- **Depots.** A module update gives `*enters` and `*invites` a
  `:migration (depot-migration id f)`, where `f` is a pure, idempotent
  function of one record: when the record's `:account` or `:email` (or an
  invite's email claim) is one being erased, it gives the record with
  those fields set to nil and `:says` to `{}` (keeping `:at`, the offer,
  the code's hash), or `DEPOT-TOMBSTONE`; else the record unchanged.
  Offsets stay. `decide` consumed those records long ago and a worker's
  restart does not replay a depot, so no PState changes by the rewrite.
  The migration reads the whole depot in the background with two logs on
  disk, so erases are best gathered into one update; a new migration id
  restarts from the original log, so its `f` covers everyone erased by a
  migration that has not finished. `*own-layers-made`, and every depot of
  a later stage, need no migration.

What this stage does to keep that possible: the naming data stays in the
places of the table, as fields of their own, never inside an id, a result
or another key; the depots that hold it hold nothing else that a later
stage writes; and the pointers above make every entry reachable from the
account, the person and the email.

## What later stages read or extend

- **The topology `decide`.** Later stages add their sources and PStates to
  it; the module keeps one stream topology.
- **`$$persons`**, the record of a person who has entered, is this stage's
  interface:
  - `who-is` (stage 2) reads `:own-layer` and `:own-layer-made?`: one seek
    on P's task, bounded, with nothing of her history in it.
  - `begin-sign-in`, `make-actor`, `end-actor` (stage 2) and `answer-ask`
    (stage 3) read whether P is an entered person (her `:own-layer` is
    non-nil) and so an actor and her own root; `end-actor` of P herself is
    `:is-a-person`.
  - No later write of the contract changes a field of this record (they
    change actors, sign-ins, sessions and asks), so no later stage has to
    rewrite it. A later stage keeps what is under a root in its own
    PStates keyed by the root's id, which `|hash` places on the same task
    as `$$persons[P]`; this plan does not design them.
- **`$$accounts` and `$$claims`** are read by no later write or read of
  the contract; a later erase writes them.
- **The depots** of this stage stay as they are; later stages add their
  own.
- **The client.** Later stages add their calls to `softland.auth.client`,
  and their depots' handles to the client that `open` makes.
- **Watched and read on each request, apart from history.** This stage
  keeps no history in a PState: each entry is one bounded record replaced
  in place, never a list that grows. The history of enters is in
  `*enters` alone.

## What binds this stage, and where the plan meets it

| SPEC | Where |
|---|---|
| Rules for every call: no throw; `:could-not` words; ids are the runtime's; a write that arrives twice; returns when its effect can be read; no clock; the bounds | Client; "How a write's result gets back"; the table of writes in `decide`; Design Decisions |
| **invite.**: a claim names one person for good; a person has one claim; the same P under C again changes nothing, also after she entered; `:invited-by` kept; no time | "The agreement across keys", invite; `$$persons`, `$$claims` |
| **enter.**: a link gives its person whatever claim; the code's claim before the email's; the link, the used claim and the actor as one outcome; a refusal keeps only the event; the first enter fixes `:own-layer` and `:making`; `:says` of the enter decided last | "The agreement across keys", enter, its invariants K, L, M; `$$accounts` |
| **enter.**, "What must hold" | the three traces |
| the break between Auth and the store: until `own-layer-made` every enter says false; `own-layer-made` of a person who waits is `:no-such-person` | E1 and E4 read the flag; `own-layer-made` at P |
| First bullet of "What takes a thing out of what is live": these writes take nothing out | Writes |
| "What the runtime does": no refusal for a layer not made; a repeat first, then the contract's order; a provider at most 256 characters, `:own-layer` a UUID | I1's order; enter refuses nothing for the flag; Client's fit |
| What can be erased later | its section |
| Not in this build | nothing of it is planned: one account per person, no erase, no block, no counts |
| Full life: no one task; `who-is` one seek; nothing deleted; scale; order; who appends | Design Decisions; Partitioning efficiency; Resource usage |

## Forks taken

Where the spec leaves the way open, this plan went as follows; each can be
turned.

1. **An invite is decided at the person**, with the claim's entry written
   once. Grounds: claims stay final, so enters never meet an invite half
   made.
2. **An enter's account chooses one claim before using any** (two more
   hops on a first enter). Grounds: otherwise one account can take two
   persons when an invite lands between two attempts.
3. **A second enter of an account under way drives the first's decision,
   then takes its outcome only when it is a link or the claims are the
   same**; otherwise it is judged on its own claims.
4. **`*enters` replays with `:all-after`**, so that `:says` is always the
   last decided enter's; the topology never throws, so no record holds a
   partition back.
5. **Three depots**, with `own-layer-made` apart from invites, so that only
   the depots that name a human being would ever need an erase migration.
6. **Raw accounts and emails as keys**, not digests; `$$accounts` and
   `$$claims` are `:private?`.
7. **`$$persons` keeps her `:account` and `:claim`**, so that a later erase
   reaches every entry of hers by point reads.
8. **`:invited-by` is kept in `$$persons`**, with nil for `:system`.
9. **A refused enter removes its account's entry**, so nothing of it is
   kept but its event.
10. **"Decided last" for `:says`** is the step at the account where an
    enter's result is fixed (the linked enter's first step, or the step
    that finds the link after driving a decision).
11. **The flag in an enter's result** is the one read at her task within
    that enter (E4, or the linked enter's read), not re-read after the
    link.
12. **The topology repeats the client's fit check** and answers a misfit
    with `{:could-not :read-the-event}`, writing nothing.
13. **Names:** the topology `decide`; the records `Invite`, `Enter`,
    `OwnLayerMade`, `Account`, `Claim` in `softland.auth.events`; the fit
    rules in `softland.auth.fit`.
