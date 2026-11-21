# Auth: the spec of the module

**21 November 2026.** Written by the session that builds the spine of
[the runtime's page](../../PROPOSAL.md), after Sid's go. It is the
user-facing spec that the Rama skill's phases start from. It says what Auth
must do and what must hold. It names no depot, no table and no topology: the
plan does.

Sid's decisions that it stands on are entries 2, 5, 7, 8 and 12 of the
runtime's page, and his go of 21 November to the sheet for sign-in: a
deployment is born by one setup command; an invitation is a person id that
waits for an account; a break between Auth and the store is met by writing
Auth first. Everything else here is this session's, and his to turn.

## What Auth is

Auth is a Rama module of its own beside the module `Store`. It holds what
sign-in keeps: which account is which person, who may become a person, which
actors a person has made, which secret stands for which actor, and which
sessions are open. The runtime is its one client: many copies of one JVM
program, each of which holds no state of its own. Nothing true about any of
this lives outside Auth's log.

Auth is apart from the store because it makes another promise about time. The
store's two depots of acts are never trimmed and nothing in them is erased. In
Auth, what names a human being (her account, what her provider said of her)
must be erasable later, and what has ended may one day be thinned. The spine
builds no erasing and no thinning; it must not make either impossible.

Auth never reads the store, and the store never reads Auth.

## Words

- **Account.** A proof of a human being by a provider: `[provider subject]`,
  as `[:google "1082…"]`. The subject is the provider's own text, kept as
  given.
- **Person.** A human being in Softland: a random UUID that says nothing.
- **Link.** Account to person. An account has one person at most. What the
  provider said at her sign-ins is kept with the link.
- **Invitation.** A person who waits for an account. It is made with a
  **claim**: an email address, or the hash of a code. The first account that
  proves the claim becomes that person's account.
- **Actor.** A who: something that sends acts. A person is an actor. An
  **agent** is an actor that another actor made; it has an id of its own, a
  random UUID. The **root** of an actor is the person at the top of who made
  whom. A person is her own root.
- **Sign-in.** The standing of one browser or one program to act as one
  actor, from her yes until it ends. It has an id, a kind (`:browser` or
  `:program`), and one **secret**, which is replaced over time. Auth holds
  the secret's hash, never the secret.
- **Session.** One sitting of a person or one run of a program, opened under
  a sign-in. Its id is the `:session` of every act that is sent in it. It
  outlives the secret: replacing a sign-in's secret changes no session. It
  has a time of its own, which the runtime moves on while the session is in
  use.
- **Ask.** A program's sign-in that is under way: the program has asked, and
  a person has not yet said yes or no. It is found by a short **code**.
- **Standing.** Whether a thing is live now. A sign-in is live when it has
  not ended, its secret's time has not passed, its actor has not ended, and
  no actor above that one has ended. A session is live when it is not
  closed, its time has not passed, and its sign-in is live. Auth reads no
  clock, so "has not passed" is judged in two places: by the runtime, which
  holds each time that a read gives against its own clock; and by Auth at
  each event, against that event's `:at` ("What takes a thing out of what is
  live", below).

## The client's contract

The runtime calls Auth through one namespace, `softland.auth.client`. This
contract is fixed here, because the runtime is built against it at the same
time as the module.

Rules for every call:

- No call throws for what a caller gave or for what Rama did. A call that
  could not be made gives `{:could-not <word> ...}`, a map that holds
  `:could-not`.
- Every write names its ids itself: the runtime makes them, at random or
  derived from a secret-free text by a hash, and no system outside the
  runtime ever chooses the id of a person, an actor, a sign-in or a session.
  Auth trusts that, and checks an id only within its root.
- A write that arrives twice, from one copy of the runtime or from two,
  leaves the same state as once. The second call gives the result of the
  first, which says that the write was taken; it does not say how the thing
  stands now, which a read says. Two writes are the same write when the
  parts that say what is bound are the same: for `invite`, the person and
  the claim; for `begin-sign-in`, the root, the actor, the kind and the
  hash; for `make-actor`, the root and the maker; for `open-session`, the
  root and the sign-in; for `ask`, the hash. The other parts of a repeat
  are dropped, but for `open-session`, whose repeat moves the session's
  time on. A refusal binds nothing: the same write after the state changed
  can be taken.
- A write returns when its effect can be read by any copy of the runtime.
- No write reads a clock. The runtime puts its own clock's time on each
  write as `:at`, milliseconds since 1970. Where a rule below depends on a
  time, it names the time in the event that it is held against.
- Times are fixed-width integers. Ids of persons, actors, sign-ins and
  sessions are UUIDs. A hash is 64 lower-case hex characters. A code is a
  string of 8 characters, compared exactly. A text (`:label`, `:asks`, an
  email, a provider's subject) is a string of at most 256 characters, a
  character being one `char` of the JVM. A provider is a keyword. A `:says`
  map holds at most 32 entries, each from a string of at most 256
  characters to a string of at most 2,048. A write or a read whose input
  does not fit gives `{:could-not :read-the-event}`, and a write then
  writes nothing.
- Two emails are the same when their texts are the same. The runtime gives
  every email in lower case.
- The other words of `:could-not`: `:reach-the-module`, when Rama threw or
  gave no answer in time, with what it threw; and `:closed`, for a call on
  a client that was closed. A `:could-not` from a write says nothing of
  whether the write was taken.

```text
(open cluster module-name)        -> a client, or {:could-not :find-the-module ...}
(close! client)                   -> nil

writes: each takes the client and one map, and gives a map

invite            {:person P :claim C :invited-by B :at t}
                    C is {:by :email :email "a@b"} or {:by :code :hash H}
                    B is an actor's id, or :system
                  -> {:invited P}
                     {:refused :claim-in-use}       the claim names another person
                     {:refused :person-in-use}      the person has another claim

enter             {:account [provider subject] :says {...} :email text-or-nil
                   :code-hash H-or-nil :own-layer L :making {:uuid U :session M}
                   :at t}
                  -> {:person P :own-layer L' :making {:uuid U' :session M'}
                      :own-layer-made? bool}
                     {:refused :not-invited}

own-layer-made    {:person P :at t}
                  -> {:made P}   {:refused :no-such-person}

begin-sign-in     {:root P :actor A :sign-in G :kind K :hash H :until t1
                   :label text-or-nil :at t}
                  -> {:begun G}
                     {:refused :no-such-actor}   {:refused :ended}
                     {:refused :in-use}          G is another sign-in already

replace-secret    {:root P :sign-in G :old-hash H :new-hash H' :until t1
                   :previous-until t2 :at t}
                  -> {:replaced G}
                     {:refused :not-live}   {:refused :wrong-secret}

end-sign-in       {:root P :sign-in G :at t}
                  -> {:ended G}   {:refused :no-such-sign-in}

make-actor        {:root P :actor A :made-by B :label text :at t}
                  -> {:made A}
                     {:refused :no-such-maker}   {:refused :ended}
                     {:refused :in-use}          A is another actor already
                     {:refused :too-deep}        B is 32 makers below its root

end-actor         {:root P :actor A :at t}
                  -> {:ended A}
                     {:refused :no-such-actor}   {:refused :is-a-person}

open-session      {:root P :sign-in G :session S :until t1 :at t}
                  -> {:open S}
                     {:refused :not-live}
                     {:refused :in-use}          S is open under another sign-in

close-session     {:root P :session S :at t}
                  -> {:closed S}   {:refused :no-such-session}

ask               {:code U :hash H :asks text :ends t1 :at t}
                  -> {:asked U}   {:refused :in-use}

answer-ask        {:code U :yes? bool :root P :actor A :new-agent {:label text}-or-nil
                   :sign-in G :until t1 :at t}
                  -> {:yes {:root P :actor A :sign-in G}}
                     {:no U}
                     {:refused :no-such-ask}   {:refused :over}
                     {:refused :no-such-actor} {:refused :ended}
                     {:refused :in-use}        the new agent's id or the
                                               sign-in's id is another's

reads

(who-is client {:root P :sign-in G :session S-or-nil})
                  -> {:sign-in {:actor A :kind K :hash H :until t1
                                :previous {:hash H0 :until t2}-or-nil
                                :ended? bool :actor-ended? bool}-or-nil
                      :session {:sign-in G' :until t3 :closed? bool}-or-nil
                      :own-layer L-or-nil :own-layer-made? bool}

(ask-of client U) -> {:hash H :asks text :ends t1 :state :waiting|:yes|:no
                      :answer {:root P :actor A :sign-in G}-or-nil}, or nil

(under client P)  -> {:actors [{:actor A :made-by B :label text :ended? bool} ...]
                      :sign-ins [{:sign-in G :actor A :kind K :label text-or-nil
                                  :begun t :until t1} ...]}
                     the agents of the root, ended ones too, in the order
                     in which they were made; and its live sign-ins, in the
                     order in which they began

(watch-root client P f)
                  -> a function of no argument that stops the watching
```

`watch-root` calls `f` with one argument each time what is live under the
root `P` changed, through whichever copy of the runtime the change came:

```text
{:sign-ins {G {:actor A :kind K :hash H :until t1 :previous ...}}   the live ones
 :sessions {S {:sign-in G :until t3}}}                               the open ones
```

or with `:softland.auth.client/not-reached` when this process can no longer
say what is live under `P`: within seconds of the loss, once, and `f` is not
called again after it. The caller then stops the watch and registers anew.
No call is owed at the registration: a caller that must know how its own
sign-in stands at the start registers first and then reads with `who-is`.
`f` runs on a thread of Rama's client and must return at once.

## What each write does

**invite.** Makes the person `P` wait under the claim `C`. A claim names one
person for good: once an account has proven it, it is used up, and it never
names another person. A person has one claim: `P` under another claim is
refused. The same `P` under the same `C` again changes nothing, also after
she has entered. A person who waits has no sign-in and is no actor yet.
`:invited-by` is kept as given and not checked: who may invite is the
runtime's. An invitation has no time and cannot be taken back in this
build.

**enter.** An account that its provider has proven comes in. The runtime has
checked the provider's token before it calls; Auth takes the account as
proven.

- An account that has a link gives its person, whatever claim came with it.
- An account that has none takes the person of the claim that it proves: the
  code's, when `:code-hash` names an invitation that waits; else the
  email's, when `:email` names one. The link is written, the claim is used
  up, and the person becomes an actor, her own root.
- An account that has none and proves no claim is refused, and nothing of it
  is kept but the event.
- `:own-layer` and `:making` are what the runtime offers for a person who
  has none yet. The first enter that makes her an actor fixes them; every
  later enter gives the ones that were fixed, and its own offer is dropped.
- `:says` is kept with the link as what the provider said at the enter
  that was decided last, with its `:at`. What earlier enters said is in the
  events alone.

What must hold: two enters of one new account at the same moment, through
two copies of the runtime, give one person, one own layer and one `:making`.
Two accounts that prove one claim at the same moment: one becomes the person
and the other is refused. An enter that is run again after a failure gives
what it gave.

`:own-layer` and `:making` are how a break between Auth and the store is
met. The runtime writes Auth first. Then it sends the act that makes her own
layer. That act is the system's: its who is the store's `system`, and
`:making` holds its UUID and the id that it names as its session. Auth keeps
the two and gives them back, and nothing more: that session is no session of
Auth's. The act is built from nothing but what the enter gave, so that any
copy sends the same act with the same digest. Then the runtime calls
`own-layer-made`. Until that call every enter says `:own-layer-made? false`,
and the copy that made that enter sends the act again. Only an enter does:
no sign-in of hers begins before her layer is made, so the flag of `who-is`
informs and nothing acts on it. `own-layer-made` of a person who waits is
refused with `:no-such-person`: she has no layer yet.

**begin-sign-in.** From now on the secret whose hash is `H` stands for the
actor `A`, until `t1`. `A` is under the root `P` and is live. A sign-in id is
bound once: the same id with the same parts again changes nothing; with
other parts it is refused.

**replace-secret.** The sign-in's secret becomes the one whose hash is `H'`,
with a new end `t1`. `H` is the sign-in's hash now; or it is the same
replacement again, which is so when `H'` is the sign-in's hash now and `H`
is the one before it. A replacement that arrives after a newer one is no
repeat, and is refused with `:wrong-secret`. The old secret keeps standing until `t2`
and no longer. No session changes.

**end-sign-in.** The sign-in ends, and every session under it is closed.
Ending it again changes nothing.

**make-actor.** Makes the agent `A` under the root `P`, made by the live
actor `B` of that root. Who made whom goes 32 deep at most, the store's own
bound on how deep a permission can be; no read costs more with the depth.

**end-actor.** The agent ends, with every actor that it made, in any depth,
and every sign-in and session of each. A person is not ended by this call.

**open-session, close-session.** A session is opened under a live sign-in,
with a time `:until`, and closed once. Its id is made by the runtime at
random. The same id under the same sign-in again, while the session is
open, moves its time to the `:until` that came, when that is later: that is
how the runtime keeps a session that is in use. A session that is closed,
or was taken out because its time passed, is not opened again.

**ask, answer-ask.** A program asks under a code, with the hash of the
secret that it made. A person answers once:

- yes, as herself: a sign-in of kind `:program` begins for her, with the
  ask's hash;
- yes, as a new agent: the agent `A` is made under her, with the label, and
  the sign-in begins for it;
- no: the ask is refused for good.

An answer whose `:at` is at or after the ask's `:ends` is refused with
`:over`. The first answer stands: the same answer again gives the same
result, and another answer gives what stands. Two persons who answer one ask
at the same moment: one answer stands, and both are told which; what of
that the runtime shows the other person is the runtime's.

A yes names the actor that the program becomes: the person herself; a new
agent, which is made by her; or a live agent of hers that exists. The
sign-in that a yes begins has the ask's `:asks` as its label. A no needs a
person who has entered, and judges no other part. `ask-of` judges no time:
it says `:waiting` for an ask that nobody answered, and the runtime holds
`:ends` against its clock. A code is bound for good: an ask that is over
keeps its code, so that a program that still asks under an old code never
reads another program's answer.

## What takes a thing out of what is live

Auth reads no clock, and three things have a time: a sign-in's secret, a
session, and an ask. A sign-in whose time passes with no `end-sign-in`, and
a session that nobody closes because its program crashed, would stay in what
is watched for ever. So:

- Each write that names a `:root` takes out of what is live under that root
  every sign-in and every session whose time is at or before the write's
  `:at`, before the write itself is judged, and whether the write is then
  taken, is a repeat, or is refused. A sign-in that is taken out so is
  ended, as by `end-sign-in`, with its sessions; a session, as by
  `close-session`. Neither is taken back. `invite`, `enter`,
  `own-layer-made` and `ask` name no root and take nothing out. "The same
  state as once" is said of a write's own effect: what a repeat takes out
  follows its time.
- A sign-in's time is its present secret's `:until` alone. The secret
  before it stays with the sign-in until the next replacing; the runtime
  holds its `t2` against the clock.
- What `watch-root` and `under` give is therefore bounded by what was live
  at the root's latest event. A root with no new event shows what was live
  then; the runtime holds each time against its clock when it reads.
- A copy of the runtime whose clock is far ahead ends the sign-ins of the
  roots that it writes under. That is the cost of a time in Auth, and it is
  why the runtime's clock must be kept: here a wrong clock does more than
  write a wrong label.
- A time's edge: a thing is live while the time that it is held against is
  before its end. A sign-in, a session or an ask whose end is at or before
  the `:at` of the event that makes it is taken and is never live. One
  secret before the present one stands, until its `t2`; a later replacing
  ends it whatever its `t2`.

## What the runtime does, and Auth does not check

- No sign-in of a person begins before her own layer is made. That is the
  runtime's order of calls: an enter whose `own-layer-made` did not go
  through ends with no sign-in. Auth refuses nothing for a person whose
  layer is not marked as made.
- Where two refusals apply to one write, a repeat is recognised first, then
  the refusals in the order in which the contract lists them. A session's
  id that is another sign-in's, open or not, is `:in-use`.
- A provider's name is at most 256 characters. `:own-layer` is a UUID.
- `watch-root` on a closed client gives `{:could-not :closed}` in place of
  the function, and `close!` calls no `f` again.
- The numbers that the plan sizes: the module lets 20,000 roots be watched
  on one task, the figure that Red Planet Labs run their own TPC-C module
  with (`src-store/OPEN-QUESTIONS-rama.md`, section 4), and the build
  measures it. `who-is` is made for each request and each opening of a
  connection. `open-session` is made once for a new session, and then at
  most once in half of a session's span for each session that is held; the
  runtime's default span is a day.

## What can be erased later

What names a human being is her account, her email, and what her provider
said. All of it must be erasable later from everything that Auth keeps, its
events too: the plan keeps that possible from the start, and builds none of
it. An enter that is refused keeps its event like any other.

## What must hold at full life

Judge each of these against many writers, many readers and years of data.

- **No one task that everything passes through.** No write and no read of
  one person goes through a task that every person's goes through. On 20
  November every beginning and end of a session went through one task; that
  is what this module replaces.
- **`who-is` is the read of every request.** It is made for each HTTP
  request and each opening of a connection. It is one round trip to one
  task, and it reads a bounded amount however long the person's history is.
- **What a runtime watches for one root stays small.** `watch-root` is held
  open for each person who has an open connection at a copy of the runtime.
  Its value is bounded by what is live under her, not by what has ended: an
  agent that opens a session for each run closes thousands a day.
- **How many roots can be watched.** Rama bounds the live reads of one table
  on one task (`pstate.proxy.limit`; seen at 1,000 on Rama 1.6.0, with no
  option set: `src-store/OPEN-QUESTIONS-rama.md`, section 4). The watched
  roots of one task, over all copies of the runtime together, must not be
  held to that default. The module sets what it needs at launch, and the
  build measures what the option bounds.
- **Nothing is deleted.** An ended sign-in, a closed session, an ended actor
  and an answered ask can still be read: the facts of them stay. What is
  watched and what is read on each request is kept apart from that history.
- **Scale.** Persons: millions. Actors under one person: from one to
  thousands. Live sign-ins under one person: tens; ended ones: without
  bound. Open sessions under one person: tens to hundreds; closed ones:
  without bound. Asks: short-lived, without bound over time.
- **Order.** Events of one person are decided in one order. An end is never
  taken back: no later event makes an ended sign-in, a closed session or an
  ended actor live again.
- **Who appends.** Rama asks no name. Only programs on machines that Sid
  runs may reach Rama's ports (`src-store/README.md`, 11 and 12 October,
  entry 4), and the runtime is the one program that is meant to append here.

## Not in this build

Erasing a link. A second account for one person, and the replacing of an
account. A block on a person. Counts for pacing. A person's name, which is a
fact in the store. Permissions, which are the store's.

## Where it goes

- Source: `src-auth/softland/auth/`, with the module in `module.clj` and the
  client in `client.clj`. Checks: `test-auth/softland/auth/`.
- The explanations follow the store's rules (`src-store/CLAUDE.md`): a
  README for each folder, a docstring for each namespace and each function,
  and checks that say what they check in one sentence each, in the form of
  `test-server/softland/server/proof.clj`.
- The artifacts of the skill's phases stay in this folder.
