# Implicit Spec: Auth

<!-- Phase 0. Fill in before starting Phase 1 (Plan). After completing this, fill in PLAN.md. -->

Phase 0 of the Rama skill's build of the module `Auth`. It is derived from
[SPEC.md](SPEC.md) in this folder; that page's section "The client's
contract" is the interface. This page records what must be true, never how:
it names no table, no log, no topology and no mechanism. The plan does.

## How to read this page

**Marks.** Each requirement says where it comes from.

- *(Sid)*: a decision of Sid's that the SPEC stands on: a deployment is born
  by one setup command; an invitation is a person id that waits for an
  account; a break between Auth and the store is met by writing Auth first;
  and entries 2, 5, 7, 8 and 12 of the runtime's page (the who of an act is
  its signed-in maker; what sign-in keeps is a module of its own; a session
  outlives its secret; an agent has a who of its own; sign-up is by
  invitation, and what the provider gives is kept).
- *(SPEC)*: a statement of the SPEC's author. It holds as written.
- *(derived)*: added here, where the SPEC is silent. A derived point marked
  **(Q n)** is also listed under
  [Questions for the spec's author](#questions-for-the-specs-author). It
  holds as the requirement until the answer replaces it.

**Words used below.**

- **The event's `:at`.** The time the runtime put on the write being judged.
  Every rule about time is held against it, never against a clock.
- **Under P.** Belonging to the root P: P herself, an agent whose root is P,
  a sign-in of one of those actors, a session of one of those sign-ins.
- **Waiting** person: invited, with no account yet. She is no actor.
  **Entered** person: an account has come in as her; she is an actor, her
  own root. A person never ends in this build.
- **Depth.** How many makers stand between an actor and its root: a person
  is at 0, an agent that she made at 1. *(SPEC)* 32 at most.
- **Live**, in a write. *(SPEC)* A sign-in: not ended, its actor and every
  actor above not ended, and the event's `:at` before its present secret's
  `:until`. A session: not closed, the event's `:at` before its `:until`, and
  its sign-in live.
- **An event under a root.** *(derived, Q2)* A write whose map names an
  entered person as `:root` or `:person`, or that comes to one (an enter that
  gives a person), whether it is taken, a repeat or a refusal. An `ask` is
  under no root. A write that does not fit is no event. A read is no event.
- **Taken out.** *(SPEC)* A sign-in or a session that an event under its
  root ended or closed because its time was at or before that event's `:at`:
  a sign-in as by `end-sign-in`, with its sessions; a session as by
  `close-session`.
- **A repeat.** *(SPEC)* The same write again, from one copy of the runtime
  or from two; "the same" is by the parts that say what is bound.

## Operations

### What holds for every operation

These hold for every operation below, and each operation's section adds only
what is particular to it.

1. **No call throws.** *(SPEC)* For anything a caller gave or anything Rama
   did, a call gives a map that holds `:could-not`. The words:
   `:find-the-module` (from `open`); `:read-the-event`, when the input does
   not fit; `:reach-the-module`, when Rama threw or gave no answer in time,
   with what it threw; `:closed`, for a call on a closed client. A
   `:could-not` from a write says nothing of whether the write was taken: the
   same map sent again is always safe.
2. **What does not fit is refused before anything.** *(SPEC)* A write or a
   read whose input does not fit gives `{:could-not :read-the-event}`; a
   write then writes nothing, not even its event (unlike a refused `enter`,
   whose event is kept). The bounds:
   - times are fixed-width integers; ids of persons, actors, sign-ins and
     sessions are UUIDs; a hash is 64 lower-case hex characters; a code is a
     string of 8 characters;
   - a text (`:label`, `:asks`, an email, a provider's subject) is a string
     of at most 256 characters, a character being one `char` of the JVM;
   - a provider is a keyword;
   - a `:says` map holds at most 32 entries, each from a string of at most
     256 characters to a string of at most 2,048;
   - *(by the contract's forms)* a kind is `:browser` or `:program`; a claim
     is `{:by :email :email E}` or `{:by :code :hash H}`; `:invited-by` is a
     UUID or `:system`; `:yes?` is a boolean; `:new-agent` is nil or
     `{:label text}`; the `:uuid` and `:session` of `:making` are UUIDs.
   - *(derived, Q6)* A provider keyword and `:own-layer` are bounded too,
     though the SPEC names no bound for them.
3. **Ids are the runtime's.** *(SPEC)* Every write names its ids itself: the
   runtime makes them, at random or derived from a secret-free text by a
   hash, and no system outside the runtime chooses the id of a person, an
   actor, a sign-in or a session. Auth trusts that and checks an id only
   within its root. *(derived)* Within a root, an id is checked against the
   ids of its own kind: an actor's against the actors of that root, a
   sign-in's against its sign-ins, a session's against its sessions.
   Account, claim and code are found across all of Auth, since they belong to
   no root.
4. **Repeats.** *(SPEC)* A write that arrives twice leaves the same state as
   once. The second call gives the result of the first, which says that the
   write was taken, not how the thing stands now; a read says that. The parts
   that make a repeat: for `invite`, the person and the claim; for
   `begin-sign-in`, the root, the actor, the kind and the hash; for
   `make-actor`, the root and the maker; for `open-session`, the root and the
   sign-in; for `ask`, the hash. A repeat's other parts are dropped, except
   that a repeat of `open-session` moves the session's time on. A refusal
   binds nothing: the same write after the state changed can be taken.
   - *(derived)* The repeat check is the first check of the write itself, so
     a repeat gives the first result even when the thing has ended since.
   - *(SPEC)* `enter`, `replace-secret`, the ends, `close-session`,
     `own-layer-made` and `answer-ask` have rules of their own for a repeat,
     given in their sections. *(derived)* An enter's result carries the
     present `:own-layer-made?`.
   - *(derived, Q2)* A repeat at a later `:at` is still an event under its
     root, and takes out what has passed by then.
5. **A write returns when its whole effect can be read.** *(SPEC)* Once a
   write returns, any copy of the runtime reads its effect. *(derived)* That
   is every effect on every entity the write touches, its take-outs
   included: the whole cascade of an end, the ask and the agent and the
   sign-in of a yes, the link and the claim and the person of an enter. No
   read shows part of one write's effects.
6. **No clock.** *(SPEC)* No write reads a clock; where a rule depends on a
   time, it is held against the event's `:at`. Reads judge no time: they give
   the times (`:until`, `:previous`, `:ends`, `:begun`), and the runtime
   holds each against its own clock. *(derived)* Auth does not judge an `:at`
   for being plausible: the copies' clocks differ, and an `:at` earlier than
   an earlier event's is taken as given.
7. **What has passed is taken out at each event.** *(SPEC)* Each event under
   a root takes out of what is live under that root every sign-in and every
   session whose time is at or before the event's `:at`; neither is taken
   back. A sign-in's time is its present secret's `:until`; a session's is its
   own `:until`.
   - *(derived)* The take-out comes before the event's own write is judged,
     so no write uses, moves on or repeats into life a thing whose time had
     passed at its own `:at`.
   - *(SPEC)* A sign-in, a session or an ask whose end is at or before the
     `:at` of the event that makes it is taken and is never live.
     *(derived)* Likewise a secret that a replacement gives an end at or
     before its own `:at`: once the event is decided, nothing under the root
     whose time is at or before its `:at` is live.
   - *(SPEC)* So what `watch-root` and `under` give is bounded by what was
     live at the root's latest event; a root with no new event shows what was
     live then.
   - *(derived)* The work an event does to take out what has passed grows
     with what is live under the root, never with its history.
   - *(SPEC)* A copy of the runtime whose clock is far ahead ends the
     sign-ins of the roots it writes under; the runtime keeps its clock.
8. **The edge of a time.** *(SPEC)* A thing is live while the time it is
   held against is before its end. One secret before the present one stands,
   until its `t2`; a later replacing ends it whatever its `t2`.
9. **One order.** *(SPEC)* The events of one person are decided in one
   order. *(derived)* So are the events of one account, of one claim and of
   one ask. Where one event touches several of these (an enter: an account, a
   claim, a person; a yes: an ask and a person), every copy sees it decided
   at one place in each of their orders. "First" in this page means first
   decided, not lowest `:at`.
10. **An end is never taken back.** *(SPEC)* No later event makes an ended
    sign-in, a closed session or an ended actor live again, however it
    ended: by its write, by a cascade, or by a take-out.
11. **Nothing is deleted.** *(SPEC)* An ended sign-in, a closed session, an
    ended actor and an answered ask can still be read: through `who-is` (a
    sign-in, a session), `under` (an actor) and `ask-of` (an ask).
    *(derived)* Links, waiting claims and used claims stay too.
12. **What is read on each request is kept apart from history.** *(SPEC)*
    `who-is` and every push of `watch-root` cost no more for a person with
    years of ended sign-ins and closed sessions than for a new one, and no
    read costs more with depth.
13. **No one place that everything passes through.** *(SPEC)* No write and
    no read of one person goes through a task that every person's goes
    through. *(derived)* The same holds for what is found by account, by
    claim and by an ask's code.
14. **Erasable later.** *(SPEC)* What names a human being (her account, her
    email, what her provider said) must be erasable later from everything
    that Auth keeps, its events too; the plan keeps that possible from the
    start and builds none of it. An enter that is refused keeps its event like
    any other. What has ended may one day be thinned; nothing here makes that
    impossible. *(derived)* So no fact that must stay (a person, an actor, a
    sign-in, a session, an ask) is found only through an account or an
    email.
15. **Auth does not ask who calls.** *(SPEC)* Rama asks no name; only
    programs on Sid's machines reach its ports, and the runtime is the one
    program meant to append here. *(derived)* Auth takes as given what the
    runtime asserts (an account is proven, the root P is the caller's, a maker
    is the caller) and checks only that what a write names agrees with what
    Auth holds. Who may invite, end, make or answer is the runtime's and the
    store's permissions'.
16. **Born empty.** *(Sid: one setup command)* *(derived)* Auth needs nothing
    before its first write. The setup command's first `invite`, with
    `:invited-by :system`, works on an empty Auth.
17. **Not in this build.** *(SPEC)* Erasing a link; a second account for one
    person; replacing an account; a block on a person; counts for pacing; a
    person's name; permissions.

### open

`(open cluster module-name)` gives a client, or
`{:could-not :find-the-module ...}`.

- **Latency.** Once, when a runtime process starts. Seconds are acceptable.
- **Throughput.** One per process start; the copies are tens at most.
- **Consistency/correctness invariants.**
  - *(SPEC)* It never throws. A module that is not on that cluster gives
    `:find-the-module`; *(derived)* a cluster that cannot be reached gives
    `:reach-the-module`.
  - *(derived)* The client is safe to call from many threads at once: one
    process holds one client for all its writes, reads and watches.
  - *(derived)* The client holds nothing true about Auth's state.
- **Data growth and scale.** None.
- **Concurrency behavior.** Many copies open clients at once; no copy's
  client changes another's.
- **Edge cases.** A wrong module name. Two opens in one process: two clients
  that share no watch.

### close!

`(close! client)` gives nil.

- **Latency.** At process stop. No bound needed.
- **Throughput.** One per process stop.
- **Consistency/correctness invariants.**
  - *(SPEC)* After it, every call on the client gives
    `{:could-not :closed}`.
  - *(derived, Q7)* After it, no watch of the client calls its function
    again.
  - *(derived)* A write in flight at `close!` may or may not have been
    taken; its repeat from another copy is safe.
- **Data growth and scale.** None.
- **Concurrency behavior.** A `close!` racing a call on another thread gives
  that call its result or a `:could-not`, never a throw.
- **Edge cases.** `close!` twice gives nil. `close!` of a client whose
  cluster is gone gives nil.

### invite

`{:person P :claim C :invited-by B :at t}` gives `{:invited P}`,
`{:refused :claim-in-use}` or `{:refused :person-in-use}`.

- **Latency.** An admin's action or the setup command. Up to a second is
  acceptable (Q9).
- **Throughput.** Driven by people inviting people, and by the setup command
  once. One per person who ever enters; when sign-up moves to a list (Sid,
  8c: "will move to B"), thousands at once. Far below sign-ins.
- **Consistency/correctness invariants.**
  - *(Sid)* An invitation is a person id that waits for an account.
  - *(SPEC)* A claim names one person for good, from its first invite: an
    invite of another person under it is `:claim-in-use`, whether it waits or
    is used up.
  - *(SPEC)* A person has one claim: P under another claim is
    `:person-in-use`, whether she waits or has entered.
  - *(SPEC)* The same P under the same C again changes nothing and gives
    `{:invited P}`, also after she has entered. `:invited-by` and `:at` of a
    repeat are dropped.
  - *(derived, Q5)* The checks, in order: a repeat; `:claim-in-use`;
    `:person-in-use`; then she waits.
  - *(derived)* A claim is its kind and its value: an email claim and a code
    claim never name the same thing.
  - *(SPEC)* A waiting person has no sign-in and is no actor.
  - *(SPEC)* `:invited-by` is kept as given and not checked; who may invite
    is the runtime's. No read gives it back.
  - *(SPEC)* Two emails are the same when their texts are the same; the
    runtime gives every email in lower case.
  - *(SPEC)* An invitation has no time and cannot be taken back in this
    build.
  - *(derived)* An invite of an entered person is an event under her root
    and takes out what has passed (Q2).
- **Data growth and scale.** Claims: one per person, millions. Claims that
  are never proven: without bound over years. Found by the claim's value
  (from an enter: by a code's hash, by an email) and by the person.
- **Concurrency behavior.** Two invites of different persons under one claim
  at once: one stands, the other gives `:claim-in-use`. Two invites of one
  person under two claims at once: one stands, the other gives
  `:person-in-use`. An invite racing an enter that proves the same claim:
  either the invite comes first and the enter takes P, or the enter comes
  first and is refused; never a link to P while the claim still waits.
- **Edge cases.** An email given in upper case: another text, so another
  claim; the runtime never gives one. A code claim's hash that is not 64
  lower-case hex: does not fit. A B that names no actor: kept. The first
  invite of a deployment, by `:system`, on an empty Auth. One human invited
  twice, as P1 by a code and as P2 by her email: two persons, each with one
  claim (see `enter`).

### enter

`{:account [provider subject] :says {...} :email text-or-nil :code-hash
H-or-nil :own-layer L :making {:uuid U :session M} :at t}` gives
`{:person P :own-layer L' :making {:uuid U' :session M'} :own-layer-made?
bool}` or `{:refused :not-invited}`.

- **Latency.** A person waits on it after her provider's redirect: tens to a
  few hundred milliseconds (Q9).
- **Throughput.** One per browser sign-in of a human: about one per person
  per device per secret's life. Bursts when many invited persons enter for
  the first time.
- **Consistency/correctness invariants.**
  - *(SPEC)* The runtime has checked the provider's token; Auth takes the
    account as proven. *(derived)* The runtime gives an `:email` only when
    its provider has proven it; Auth cannot tell.
  - *(SPEC)* An account that has a link gives its person, whatever claim came
    with it; no claim is used up, and a claim that it proves stays waiting.
  - *(SPEC)* An account that has none takes the person of the claim that it
    proves: the code's, when `:code-hash` names a waiting invitation; else
    the email's, when `:email` names one. Then, as one effect: the link is
    written, the claim is used up, the person becomes an actor and her own
    root, and her own layer and `:making` are fixed from this enter's offer.
  - *(derived)* A used claim never matches again.
  - *(SPEC)* An account that has none and proves no waiting claim gives
    `{:refused :not-invited}`; nothing of it is kept but the event.
  - *(SPEC)* The first enter that makes her an actor fixes `:own-layer` and
    `:making`; every later enter gives the fixed ones and drops its own offer.
  - *(SPEC, Sid: write Auth first)* The act that makes her own layer is the
    system's: its who is the store's `system`; `:making` holds its UUID and
    the id that it names as its session. Auth keeps the two and gives them
    back, and nothing more: that session is no session of Auth's.
  - *(SPEC)* Until `own-layer-made` for her, every enter says
    `:own-layer-made? false`, and the copy that made that enter sends the act
    again. Only an enter does: the result of every enter carries the present
    flag.
  - *(SPEC)* No sign-in of hers begins before her layer is made. *(derived,
    Q1)* That is the runtime's order of calls; the contract gives Auth no
    refusal that would hold it.
  - *(SPEC)* An account has one person at most. *(derived)* A person has one
    account at most in this build, since she has one claim.
  - *(SPEC)* `:says` is kept with the link as what the provider said at the
    enter decided last, with its `:at`; what earlier enters said is in the
    events alone.
  - *(SPEC)* The account, the email and `:says` are erasable later, from the
    events too, a refused enter's included.
  - *(derived)* An enter that gives a person is an event under her root and
    takes out what has passed (Q2).
- **Data growth and scale.** Links: one per entered person, millions. Found
  by account. `:says`: one readable map per link. Waiting claims are found by
  a code's hash and by an email.
- **Concurrency behavior.**
  - *(SPEC)* Two enters of one new account at the same moment, through two
    copies: one person, one own layer, one `:making`; both get the same.
  - *(SPEC)* Two accounts that prove one claim at the same moment: one
    becomes the person, the other is refused (with `:not-invited`, unless it
    proves another waiting claim).
  - *(derived)* One new account that proves claim C1 (person P1) through one
    copy and claim C2 (person P2) through another, at once: one link; the
    other claim stays waiting.
  - *(SPEC)* An enter run again after a failure gives what it gave, with the
    present flag.
- **Edge cases.** `:email` and `:code-hash` both nil. A code's hash that
  names a used claim while the email names a waiting one: the email's. A
  code's hash that names a waiting claim of P1 while the email names a
  waiting claim of P2: P1, and P2 keeps waiting; a later account of hers at
  another provider that proves the email becomes P2, a second person for one
  human, which Auth cannot see. An empty `:says`. A `:says` too large: does
  not fit, nothing kept, not even the event. A subject that differs only in
  case: another account. An enter for a person whose layer is made: flag
  true.

### own-layer-made

`{:person P :at t}` gives `{:made P}` or `{:refused :no-such-person}`.

- **Latency.** In the background, after the store has taken the act. Up to a
  second is acceptable.
- **Throughput.** Once per entered person, plus repeats.
- **Consistency/correctness invariants.**
  - *(SPEC, Sid: write Auth first)* After it, every enter and every `who-is`
    of P says `:own-layer-made? true`; it never goes back to false.
  - *(SPEC)* A person who waits gives `:no-such-person`: she has no layer
    yet. *(derived)* So does an id that names no person.
  - *(derived)* A repeat gives `{:made P}` and changes nothing.
  - *(SPEC)* Auth never reads the store: whether the act was taken is the
    runtime's word.
  - *(derived)* It is an event under her root and takes out what has passed
    (Q2).
- **Data growth and scale.** One flag per entered person.
- **Concurrency behavior.** Several copies at once: the same as once. Racing
  an enter of the same person: the enter gives false or true by the order
  decided.
- **Edge cases.** Called before her first enter: refused. Called by a runtime
  whose act the store had not taken: Auth cannot tell.

### begin-sign-in

`{:root P :actor A :sign-in G :kind K :hash H :until t1 :label text-or-nil
:at t}` gives `{:begun G}`, `{:refused :no-such-actor}`,
`{:refused :ended}` or `{:refused :in-use}`.

- **Latency.** A person waits on it at the end of her sign-in: tens to a few
  hundred milliseconds.
- **Throughput.** One per browser sign-in and per program's sign-in (a yes
  also begins one): a few a day per person, more for persons with many
  agents.
- **Consistency/correctness invariants.**
  - *(SPEC)* From now on the secret whose hash is H stands for A, until t1. A
    is under P and live.
  - *(SPEC)* A sign-in id is bound once: the same G with the same root,
    actor, kind and hash gives `{:begun G}` and changes nothing, even if G
    has ended since; with other parts, `:in-use`. Its `:until`, `:label` and
    `:at` are dropped.
  - *(derived, Q5)* The checks, after the take-out, in order: a repeat;
    `:in-use` (G bound under P with other parts); `:no-such-actor` (A is not
    an actor under P: unknown, a waiting person, of another root, or P not
    entered); `:ended` (A, or an actor above it, has ended); then it begins.
  - *(SPEC)* A t1 at or before t: taken, and never live; `who-is` shows it
    ended.
  - *(derived, Q1)* Auth does not refuse a sign-in of a person whose layer is
    not made.
  - *(derived)* Auth does not judge the kind against the actor.
  - *(derived)* No limit on live sign-ins per actor (counts for pacing are
    not in this build).
  - *(derived)* Auth never finds a sign-in by its hash; nothing depends on
    two sign-ins having different hashes.
- **Data growth and scale.** Per person: tens live, ended ones without bound.
  Found by root and sign-in id.
- **Concurrency behavior.** The same G from two copies: bound once, both give
  `{:begun G}`. A begin racing `end-actor` of A or of an actor above it:
  either begun and then ended by the cascade, or `:ended`; never live under
  an ended actor.
- **Edge cases.** A nil label. A label of 257 characters: does not fit. A
  kind other than the two: does not fit. A hash in upper case: does not fit.
  A = P. A t1 far ahead. A G already bound under another root: not checked,
  since the runtime's ids never collide.

### replace-secret

`{:root P :sign-in G :old-hash H :new-hash H' :until t1 :previous-until t2
:at t}` gives `{:replaced G}`, `{:refused :not-live}` or
`{:refused :wrong-secret}`.

- **Latency.** A request or a program's refresh waits on it: tens of
  milliseconds.
- **Throughput.** Once per secret's life for each live sign-in. With
  `open-session`, the largest volume of writes.
- **Consistency/correctness invariants.**
  - *(SPEC)* When G is live and H is its hash now: its hash becomes H', with
    the end t1; the old secret keeps standing until t2 and no longer; no
    session changes. This moves the sign-in's time on.
  - *(SPEC)* When H' is already its hash, it is the same replacement again:
    `{:replaced G}`, no change. *(derived, Q3)* A repeat is recognised when
    H' is its hash now and H is the hash just before it; with H' now and
    another H it is `:wrong-secret`. The t1 and t2 of a repeat are dropped.
  - *(SPEC)* One secret before the present one stands at most: a later
    replacing ends it whatever its t2.
  - *(derived)* `:not-live`: G is not under P, or has ended (by its end, its
    actor's, or a take-out, this event's included).
  - *(derived)* `:wrong-secret`: G is live and H is not its hash now, also
    when H is the previous hash (a second refresh with the old secret).
  - *(derived)* The checks, after the take-out, in order: a repeat;
    `:not-live`; `:wrong-secret`; then it replaces.
  - *(derived)* A t1 at or before t: the replacement is taken and G is no
    longer live.
  - *(derived, Q4)* The sign-in's time is its present secret's `:until`: a
    previous secret whose t2 is later does not keep it live.
  - *(Sid: entry 7)* A session outlives the secret: no session under G
    changes.
- **Data growth and scale.** Per sign-in: the hash now and one previous are
  readable. Every replacement is an event; old ones may be thinned later.
- **Concurrency behavior.** Two replacements of G from the same H at once:
  one stands; the other is a repeat if its H' is the same, else
  `:wrong-secret`, and that caller's old secret still stands until t2.
  Racing `end-sign-in`: either replaced and then ended, or `:not-live`.
- **Edge cases.** A repeat that arrives after a newer replacement gives
  `:wrong-secret` (Q3). H equal to H' while H is its hash: a repeat. A t2 at
  or before t: the old secret stands no more at once. A replacement of a
  sign-in whose time passed a moment before: it is taken out by this event
  and the replacement is `:not-live`.

### end-sign-in

`{:root P :sign-in G :at t}` gives `{:ended G}` or
`{:refused :no-such-sign-in}`.

- **Latency.** A sign-out or a program's end: up to a few hundred
  milliseconds; the change reaches other copies' watches quickly (Q9).
- **Throughput.** One per sign-out and per program's end. Sign-ins whose
  time passes need none: an event takes them out.
- **Consistency/correctness invariants.**
  - *(SPEC)* The sign-in ends, and every session under it is closed. Ending
    it again changes nothing.
  - *(derived)* G not under P gives `:no-such-sign-in`, and nothing changes
    under either root.
  - *(derived)* A sign-in that has ended in any way, a take-out included,
    gives `{:ended G}`.
  - *(derived)* Only open sessions change; closed ones keep their facts.
- **Data growth and scale.** The cascade is bounded by the open sessions of
  one sign-in: tens to hundreds.
- **Concurrency behavior.** Racing `open-session` under G: either opened and
  then closed, or `:not-live`; never an open session under an ended sign-in.
  Two ends at once: one end, both give `{:ended G}`.
- **Edge cases.** A G that was never begun. A G with hundreds of open
  sessions.

### make-actor

`{:root P :actor A :made-by B :label text :at t}` gives `{:made A}`,
`{:refused :no-such-maker}`, `{:refused :ended}`, `{:refused :in-use}` or
`{:refused :too-deep}`.

- **Latency.** When a person or an agent makes an agent: a few hundred
  milliseconds.
- **Throughput.** Rare per person; thousands over one person's life at most.
- **Consistency/correctness invariants.**
  - *(SPEC)* Makes the agent A under the root P, made by the live actor B of
    that root. *(Sid: entry 8)* It has an id of its own.
  - *(SPEC)* Who made whom goes 32 deep at most, the store's own bound on how
    deep a permission can be: a B with 32 makers above it gives `:too-deep`.
    No read costs more with depth.
  - *(SPEC)* A repeat is A with the same root and maker: `{:made A}`, no
    change, even if A has ended since; its label and `:at` are dropped.
  - *(derived, Q5)* The checks, after the take-out, in order: a repeat;
    `:in-use` (A is another actor under P); `:no-such-maker` (B is not an
    actor of P); `:ended` (B, or an actor above it, has ended); `:too-deep`;
    then it makes.
  - *(derived)* A new agent is live, has no sign-in, and its root is P.
  - *(derived)* `:label` is a text; nil does not fit.
- **Data growth and scale.** Actors per root: one to thousands; ended ones
  stay. A chain of 32 at most.
- **Concurrency behavior.** A make under B racing `end-actor` of B or of an
  actor above it: either made and then ended by the cascade, or `:ended`;
  never a live actor beneath an ended one.
- **Edge cases.** A = P: `:in-use`. A = B: `:in-use`. B of another root:
  `:no-such-maker`. B a waiting person: `:no-such-maker`. B at depth 31: A is
  made at depth 32. An A used by an actor of another root: not checked.

### end-actor

`{:root P :actor A :at t}` gives `{:ended A}`, `{:refused :no-such-actor}`
or `{:refused :is-a-person}`.

- **Latency.** The largest write: it reaches up to thousands of actors and
  their live sign-ins and open sessions. Seconds are acceptable (Q9); it
  returns only when all of it can be read.
- **Throughput.** Rare.
- **Consistency/correctness invariants.**
  - *(SPEC)* The agent ends, with every actor that it made, in any depth, and
    every sign-in and session of each. A person is not ended by this call:
    A = P gives `:is-a-person`.
  - *(derived)* A not an actor under P gives `:no-such-actor`.
  - *(derived)* A that has already ended gives `{:ended A}` and changes
    nothing.
  - *(SPEC: standing)* From the moment it is decided, no read shows any
    actor beneath A as live, nor any sign-in or session of theirs: `who-is`
    says `:ended? true` and `:actor-ended? true` for each such sign-in, and
    `:closed? true` for each such session.
  - *(SPEC)* The facts stay: `under` lists each ended actor.
- **Data growth and scale.** *(derived)* Its cost grows with the actors
  beneath A (at most 32 deep) and what is live beneath them, not with their
  ended sign-ins or closed sessions.
- **Concurrency behavior.** Racing `make-actor`, `begin-sign-in`,
  `open-session` or a yes for an actor beneath A: each lands either before
  (and is ended by the cascade) or after (and is refused).
- **Edge cases.** An A with nothing beneath it. A tree of thousands. An A
  whose maker has already ended.

### open-session

`{:root P :sign-in G :session S :until t1 :at t}` gives `{:open S}`,
`{:refused :not-live}` or `{:refused :in-use}`.

- **Latency.** Each opening of a connection and each program's run waits on
  it, and so does each moving on of a session's time: tens of milliseconds.
- **Throughput.** The largest volume of writes: one per sitting per person
  and one per run per agent (thousands a day for one agent), plus one each
  time the runtime moves an open session's time on (Q9).
- **Consistency/correctness invariants.**
  - *(SPEC)* A session is opened under a live sign-in, with a time
    `:until`. Its id is made by the runtime at random.
  - *(SPEC)* The same id under the same sign-in again, while the session is
    open, moves its time to the `:until` that came when that is later; else
    it changes nothing. Either way it gives `{:open S}`.
  - *(SPEC)* A session that is closed, or was taken out, is not opened
    again: the same id under the same sign-in gives `{:open S}`, the result
    of the first, and changes nothing.
  - *(SPEC)* S open under another sign-in of P gives `:in-use`. *(derived,
    Q5)* So does S closed or taken out under another sign-in of P.
  - *(derived)* G not live gives `:not-live`: G not under P, or ended (by its
    end, its actor's, or a take-out, this event's included).
  - *(derived)* The checks, after the take-out, in order: a repeat;
    `:in-use`; `:not-live`; then it opens.
  - *(SPEC)* A t1 at or before t: taken, and never live; `who-is` shows it
    closed.
  - *(Sid: entry 7)* Its id is the `:session` of every act sent in it, and
    it outlives the secret.
- **Data growth and scale.** Per person: tens to hundreds open; closed ones
  without bound, millions over years for a person with busy agents. Found by
  root and session id.
- **Concurrency behavior.** S racing `end-sign-in` of G: opened and then
  closed, or `:not-live`. The same S under two sign-ins at once: one stands,
  the other gives `:in-use`. Two moves of S's time at once: the later
  `:until` stands.
- **Edge cases.** A repeat whose `:until` is earlier than the present one:
  no change. A repeat at an `:at` past S's `:until`: S is taken out first
  and its time is not moved. A session whose `:until` is later than its
  sign-in's: live only while its sign-in is. An S used under another root:
  not checked.

### close-session

`{:root P :session S :at t}` gives `{:closed S}` or
`{:refused :no-such-session}`.

- **Latency.** When a sitting or a run ends: a few hundred milliseconds,
  often not waited on. A run that crashes never calls it; its session is
  taken out when its time passes.
- **Throughput.** Fewer than `open-session`: crashed runs never close.
- **Consistency/correctness invariants.**
  - *(SPEC)* A session is closed once; closing it again gives `{:closed S}`
    and changes nothing.
  - *(derived)* A session taken out gives `{:closed S}`.
  - *(derived)* S not under P gives `:no-such-session`.
  - *(derived)* Its sign-in does not change.
- **Data growth and scale.** As `open-session`.
- **Concurrency behavior.** Racing `end-sign-in`: closed either way.
- **Edge cases.** A session already closed by its sign-in's end.

### ask

`{:code U :hash H :asks text :ends t1 :at t}` gives `{:asked U}` or
`{:refused :in-use}`.

- **Latency.** A program waits on it before it shows the code: a few hundred
  milliseconds.
- **Throughput.** One per program's sign-in attempt, abandoned ones
  included. Without bound over time.
- **Consistency/correctness invariants.**
  - *(SPEC)* A program asks under a code, with the hash of the secret that it
    made.
  - *(SPEC)* A repeat is the same U with the same H: `{:asked U}`, no change,
    even after the ask is answered or over; its `:asks`, `:ends` and `:at`
    are dropped. A code bound otherwise gives `:in-use`.
  - *(SPEC)* A code is bound for good: an ask that is over keeps its code, so
    a program that still asks under an old code never reads another's
    answer. Codes are compared exactly.
  - *(SPEC)* An `:ends` at or before t: taken, and over from the start.
  - *(derived)* An ask is under no root and takes nothing out.
  - *(derived)* An ask cannot be withdrawn; it ends by its time or an answer.
- **Data growth and scale.** Asks: without bound; each is found by its code.
  Codes are finite (8 characters), and `:in-use` tells the runtime to pick
  another.
- **Concurrency behavior.** Two asks on one code at once with different
  hashes: one is bound, the other gives `:in-use`.
- **Edge cases.** A code that is not 8 characters: does not fit. An empty
  `:asks`. An `:ends` far ahead.

### answer-ask

`{:code U :yes? bool :root P :actor A :new-agent {:label text}-or-nil
:sign-in G :until t1 :at t}` gives `{:yes {:root P :actor A :sign-in G}}`,
`{:no U}`, `{:refused :no-such-ask}`, `{:refused :over}`,
`{:refused :no-such-actor}`, `{:refused :ended}` or `{:refused :in-use}`.

- **Latency.** A person waits on it after she says yes or no: a few hundred
  milliseconds.
- **Throughput.** At most one taken per ask; repeats and losing answers on
  top.
- **Consistency/correctness invariants.**
  - *(SPEC)* A person answers once. The first answer stands: the same answer
    again gives the same result, and another answer gives what stands. Two
    persons who answer one ask at the same moment: one answer stands, and
    both are told which; what of that the runtime shows the other person is
    the runtime's.
  - *(SPEC)* An answer whose `:at` is at or after the ask's `:ends` is
    `:over`.
  - *(SPEC)* A yes names the actor that the program becomes: the person
    herself; a new agent, made by her, with the label; or a live agent of
    hers that exists. A sign-in of kind `:program` begins for that actor,
    with the ask's hash, the end t1, and the ask's `:asks` as its label.
  - *(SPEC)* A no needs a person who has entered, and judges no other part.
    The ask is refused for good.
  - *(SPEC)* A new agent's id, or the sign-in's id, that is another's under
    P gives `:in-use`.
  - *(derived, Q5)* The checks, in order: `:no-such-ask` (U is not bound); an
    answer already stands, which gives that answer whatever this one is and
    whatever its `:at`; `:over`; then, after the take-out under P:
    `:no-such-actor` (P not an entered person; or, with no new agent, A not
    an actor under P); `:ended` (A or an actor above it has ended); `:in-use`;
    then the answer is taken.
  - *(derived)* A yes's effects (the ask answered, the agent made, the
    sign-in begun) are one effect: once `ask-of` gives `:yes` with G,
    `who-is` of P and G gives that sign-in, `under` lists it and the watch of
    P carries it (unless it has ended since).
  - *(derived)* A losing answer makes nothing: no agent and no sign-in.
  - *(derived)* The answer does not need the program's secret: the ask's hash
    becomes the sign-in's hash.
  - *(derived)* An answer is an event under P and takes out what has passed
    (Q2).
- **Data growth and scale.** One answer per ask, kept for ever.
- **Concurrency behavior.** As the SPEC says, and: a yes as an existing agent
  racing `end-actor` of that agent is decided in P's order: either the
  sign-in begins and is ended by the cascade, or the yes is `:ended` and the
  ask keeps waiting.
- **Edge cases.** An answer whose `:at` equals `:ends`: over. A first answer
  taken in time whose repeat carries a later `:at`: what stands. A no that
  carries `:new-agent`: ignored. A yes with a t1 at or before t: taken, and
  the sign-in is never live. A yes for a person whose layer is not made: not
  refused (Q1).

### who-is

`(who-is client {:root P :sign-in G :session S-or-nil})` gives
`{:sign-in {...}-or-nil :session {:sign-in G' :until t3 :closed?
bool}-or-nil :own-layer L-or-nil :own-layer-made? bool}`.

- **Latency.** Single-digit milliseconds (Q9). *(SPEC)* One round trip to
  one task, on every HTTP request and every opening of a connection.
- **Throughput.** The highest of every operation, by orders of magnitude:
  every request of every person and every agent (Q9).
- **Consistency/correctness invariants.**
  - *(SPEC)* The sign-in part, for G under P: `:actor`, `:kind`, `:hash`,
    `:until`, `:previous` (`{:hash H0 :until t2}` or nil), `:ended?`,
    `:actor-ended?`. *(derived)* Nil when G is not under P.
  - *(derived)* `:ended?` is true after `end-sign-in`, a take-out, or the end
    of its actor or of an actor above it. `:actor-ended?` is true when its
    actor or an actor above it has ended; for a person's own sign-in it is
    always false in this build.
  - *(SPEC)* The session part, for S: its sign-in G', which can differ from
    the G asked, its `:until`, and `:closed?`, true also for a session taken
    out. *(derived)* Nil when S is nil or not under P.
  - *(SPEC)* `:own-layer` is the one fixed at her first enter, and
    `:own-layer-made?` the present flag, which informs and on which nothing
    acts. *(derived)* Nil and false for a root that has not entered.
  - *(SPEC)* It judges no time and gives no `:making`.
  - *(SPEC)* It reads a bounded amount however long the person's history is.
  - *(derived)* After any write returns, every copy's `who-is` shows it.
  - *(derived, Q4)* `:previous` stays as given until a later replacement,
    also after its t2; the runtime judges t2.
- **Data growth and scale.** Three lookups by id under one root; nothing
  scanned.
- **Concurrency behavior.** Racing writes of the same root: it sees each
  write whole or not at all.
- **Edge cases.** An unknown or waiting P: all nil and false. A known G with
  S nil. S under another sign-in of P. A sign-in whose `:until` has passed
  with no event since: not ended, and the runtime sees the passed time.

### ask-of

`(ask-of client U)` gives `{:hash H :asks text :ends t1 :state
:waiting|:yes|:no :answer {:root P :actor A :sign-in G}-or-nil}`, or nil.

- **Latency.** Single-digit milliseconds. A program polls it every few
  seconds while it waits; a person reads it once when she types the code.
- **Throughput.** Polls times the asks that wait.
- **Consistency/correctness invariants.**
  - *(SPEC)* Nil for a code that is not bound.
  - *(SPEC)* It judges no time: `:waiting` for an ask that nobody answered,
    also after `:ends`; the runtime holds `:ends` against its clock.
  - *(derived)* `:answer` is given for `:yes` only, as it was given, even
    when its sign-in or actor has ended since; standing is read through
    `who-is`.
  - *(derived)* `:hash` is the ask's own; it does not follow later
    replacements of the sign-in's secret.
  - *(SPEC)* An answered ask can be read for ever.
- **Data growth and scale.** One lookup by code.
- **Concurrency behavior.** Racing an answer: `:waiting`, or the answer that
  stands; never an answer whose sign-in cannot yet be read.
- **Edge cases.** A code that is not 8 characters: does not fit. The code of
  an ask that is over. The code of an ask answered no.

### under

`(under client P)` gives `{:actors [{:actor A :made-by B :label text
:ended? bool} ...] :sign-ins [{:sign-in G :actor A :kind K :label
text-or-nil :begun t :until t1} ...]}`.

- **Latency.** Tens of milliseconds: a page of her agents and sign-ins.
- **Throughput.** Low: when a person opens that page.
- **Consistency/correctness invariants.**
  - *(SPEC)* `:actors`: the agents of the root, ended ones too, in the order
    in which they were made. P herself is not listed.
  - *(SPEC)* `:sign-ins`: its live sign-ins, in the order in which they
    began. *(SPEC)* Live as of the root's latest event: a sign-in whose time
    passed with no event since is still listed, and the runtime judges its
    `:until`.
  - *(derived)* `:begun` is the `:at` of the write that began it (the first,
    for a repeat); `:until` is its present secret's.
  - *(derived)* After any write returns, every copy's `under` shows it.
- **Data growth and scale.** It grows with the agents ever made (thousands),
  not with ended sign-ins or closed sessions. *(derived)* No paging in this
  build.
- **Concurrency behavior.** Racing writes of the same root: each write whole
  or not at all.
- **Edge cases.** An unknown or waiting root: `{:actors [] :sign-ins []}`.
  A root with no agents: `:actors []`.

### watch-root

`(watch-root client P f)` gives a function of no argument that stops the
watching. `f` is called with `{:sign-ins {G {:actor A :kind K :hash H :until
t1 :previous ...}} :sessions {S {:sign-in G :until t3}}}` or with
`:softland.auth.client/not-reached`.

- **Latency.** A change made through any copy reaches every watch of P
  within a second of the write's return (Q9). *(SPEC)* `not-reached` comes
  within seconds of the loss.
- **Throughput.** One watch per copy per root with an open connection at
  that copy. Calls per change of what is live under the root: most come from
  `open-session` (each opening and each moving on of a session's time),
  `close-session`, take-outs, and `replace-secret` (Q9).
- **Consistency/correctness invariants.**
  - *(SPEC)* `f` is called each time what is live under P changed, through
    whichever copy the change came. Its value holds the live sign-ins and the
    open sessions, each session with its sign-in and its `:until`.
  - *(derived)* Each call carries the whole present value. What changes it:
    `begin-sign-in`, `replace-secret`, `end-sign-in`, `end-actor` (when
    something beneath was live), `open-session` (an opening, or a later
    `:until`), `close-session`, a yes, and any event under the root that
    takes something out. Nothing else does: an `invite`, an `enter`, an
    `own-layer-made`, a `make-actor`, an `ask`, a no, a refusal or a repeat
    changes the value only when its event takes something out.
  - *(derived)* A call is the present value, not an event: `f` may get the
    same value twice, and two changes close together may arrive as one call
    with the later value. A call never carries an older value after a newer
    one.
  - *(SPEC)* No call is owed at the registration: a caller that must know
    how its own sign-in stands at the start registers first and then reads
    with `who-is`.
  - *(SPEC)* `not-reached` comes once, when this process can no longer say
    what is live under P; `f` is not called again after it; the caller stops
    the watch and registers anew.
  - *(SPEC)* `f` runs on a thread of Rama's client and must return at once.
    *(derived)* An `f` that throws stops neither this watch's later calls nor
    other watches.
  - *(derived)* After the stop function returns, no call of `f` starts;
    calling it twice does no harm. Several watches of one root in one process
    each get their calls.
  - *(SPEC)* Its value is bounded by what was live at the root's latest
    event, not by what has ended.
  - *(SPEC)* The watched roots of one task, over all copies together, are
    not held to Rama's default bound on live reads (seen at 1,000); the
    module sets what it needs at launch, and the build measures what the
    option bounds (Q8).
- **Data growth and scale.** The value: live sign-ins (tens) and open
  sessions (tens to hundreds) of one person.
- **Concurrency behavior.** Writes through many copies at once reach every
  watch in the order they were decided.
- **Edge cases.** A watch of a root that has nothing live: no call until
  something live appears. A sign-in whose time passes with no event: no call;
  the runtime judges its `:until`. A watch on a closed client (Q7).

## Entity State × Write Matrix

There are six entities, one for each of the SPEC's words that writes target:
the account and its link, the invitation (its claim), the person and actor,
the sign-in, the session, and the ask. Each section lists the entity's states
and its related reads, then a row for each state and each of the twelve
writes. A row that names several states gives the same outcome and the same
reads for each. A write that does not touch the entity in any state has one
row naming every state.

**Take-outs in every row.** Every write but `ask` is an event under a root
once it names an entered person (Q2). Before its own effect is judged, it
takes out every sign-in and session under that root whose time is at or
before its `:at`. The sign-in states S3 and session states N2 and N3 show
that take-out on the thing itself. In every other row, "watch: no call" means
no call for the write's own effect: a take-out of some other sign-in or
session under the root calls `f` with the value without it.

### 1. Account and its link

States:

- **L0** no link (never entered, or every enter refused).
- **L1(P)** linked to the person P.

Related reads: `enter` again with that account (the only read of an
account), `who-is` of P, `under` P, `watch-root` P.

**L0 × invite** → the account does not change.
- enter again: `:not-invited` as before, unless the invite made a claim that
  this account proves wait; then the next enter takes P.
- who-is: the account has no person; who-is of the invited P is nil, nil,
  nil, false.
- under: of the invited P, empty.
- watch-root: no call.

**L1(P) × invite** → the link does not change.
- enter again: P, with the fixed own layer and making.
- who-is P: unchanged (an invite of P under another claim is
  `:person-in-use`).
- under P: unchanged.
- watch-root P: no call.

**L0 × enter, proving a waiting code claim of P** → L1(P); the claim is used
up; P is entered, her own layer and making fixed from this offer.
- enter again: `{:person P :own-layer L :making {U M} :own-layer-made?
  false}`, this offer's L, U and M; a new offer is dropped.
- who-is {P, any G, nil}: sign-in nil, session nil, own-layer L, made false.
- under P: `{:actors [] :sign-ins []}`.
- watch-root P: no call; nothing live yet.

**L0 × enter, proving no waiting code claim but a waiting email claim of P**
→ as the row above, with the email's person P.
- enter again: P, this offer's L and making, flag false.
- who-is P: own-layer L, made false.
- under P: empty.
- watch-root P: no call.

**L0 × enter, proving a waiting code claim of P1 and a waiting email claim
of P2** → L1(P1); P2 keeps waiting.
- enter again: P1.
- who-is: P1 gives L and false; P2 gives nil and false.
- under: P1 and P2 both empty.
- watch-root: no call for either.

**L0 × enter, proving no waiting claim** (none given, unknown, or used up) →
`{:refused :not-invited}`; L0; only the event is kept.
- enter again: `:not-invited`, unless an invite has made a claim it proves
  wait since.
- who-is: every who-is unchanged.
- under: unchanged.
- watch-root: no call.

**L1(P) × enter** (any claims, any offer) → L1(P); `:says` becomes this
enter's, with its `:at`; no claim used; the event takes out what has passed
under P.
- enter again: P, the fixed own layer and making, the present flag.
- who-is P: own layer and flag unchanged; a sign-in or session whose time
  passed by this `:at` is now ended or closed.
- under P: without sign-ins taken out.
- watch-root P: a call only if something was taken out.

**L0 × own-layer-made** → the account does not change.
- enter again: as before.
- who-is: unchanged; the write is refused for a person who waits or an
  unknown id.
- under: unchanged.
- watch-root: no call.

**L1(P) × own-layer-made {P}** → the link does not change; her flag becomes
true.
- enter again: flag true.
- who-is P: `:own-layer-made? true`.
- under P: unchanged.
- watch-root P: no call.

**L0, L1 × begin-sign-in, replace-secret, end-sign-in, make-actor,
end-actor, open-session, close-session, ask, answer-ask** → the account and
its link do not change.
- enter again: the same person, own layer, making and flag as before.
- who-is P: changes only as the rows of the person, sign-in, session and ask
  say.
- under P: likewise.
- watch-root P: likewise.

### 2. Invitation (its claim)

States:

- **C0** names nobody.
- **C1(P)** waits, naming P, who has not entered.
- **C2(P)** used up: an account proved it; it names P for good.

Related reads: `invite` again (its result), `enter` by an account that
proves it (its result), `who-is` of P, `under` P, `watch-root` P.

**C0 × invite {P, C}, P with no claim** → C1(P).
- invite again: `{:invited P}` for P; `:claim-in-use` for another person.
- enter proving C by an unlinked account: takes P.
- who-is P: nil, nil, nil, false.
- under P: empty.
- watch-root P: no call.

**C0 × invite {P, C}, P with another claim** (waiting or used) →
`{:refused :person-in-use}`; C stays C0.
- invite again: `:person-in-use`.
- enter proving C: C names nobody; the enter is judged on its other claim.
- who-is P: unchanged.
- under P: unchanged.
- watch-root P: no call for this write.

**C1(P) × invite {P, C}** → no change; `{:invited P}`.
- invite again: the same.
- enter proving C: takes P.
- who-is P: unchanged.
- under P: unchanged.
- watch-root P: no call.

**C1(P), C2(P) × invite {Q, C}, Q not P** → `{:refused :claim-in-use}`; no
change.
- invite again {P, C}: `{:invited P}`.
- enter proving C: takes P (C1); refused (C2).
- who-is Q and P: unchanged.
- under Q and P: unchanged.
- watch-root: no call.

**C2(P) × invite {P, C}** → no change; `{:invited P}`; C stays used.
- invite again: the same.
- enter proving C by a new account: `:not-invited`.
- who-is P: unchanged.
- under P: unchanged.
- watch-root P: no call for this write.

**C0 × enter proving C** → C is no claim; the enter is judged on its other
claim, or refused; C stays C0.
- invite again: C names nobody.
- enter again: the same outcome.
- who-is, under: unchanged by C.
- watch-root: no call.

**C1(P) × enter by an unlinked account proving C** (the code, or the email
with no waiting code claim) → C2(P); P entered.
- invite again {P, C}: `{:invited P}`; {Q, C}: `:claim-in-use`.
- enter: the same account gives P; another unlinked account proving C:
  `:not-invited`.
- who-is P: own-layer L, made false.
- under P: empty.
- watch-root P: no call.

**C1(P) × enter by a linked account (person Q) proving C** → C stays C1(P);
the enter gives Q.
- invite again: `{:invited P}`.
- enter by another unlinked account proving C: takes P.
- who-is P and Q: unchanged.
- under: unchanged.
- watch-root: no call for C.

**C1(P) × enter by an unlinked account proving C by email while its code
proves a waiting claim of P1** → the code's claim is used; C stays C1(P).
- invite again: `{:invited P}`.
- enter by another account proving C: takes P.
- who-is P: unchanged; P1: own layer, false.
- under: P unchanged; P1 empty.
- watch-root: no call.

**C2(P) × enter proving C** → C never matches; the enter is judged on its
other claim, or refused.
- invite again: `{:invited P}`.
- enter again: the same outcome.
- who-is P: unchanged.
- under P: unchanged.
- watch-root P: no call.

**C0, C1, C2 × own-layer-made, begin-sign-in, replace-secret, end-sign-in,
make-actor, end-actor, open-session, close-session, ask, answer-ask** → the
claim does not change.
- invite again, enter proving it: the same results as before.
- who-is, under, watch-root of P: as the rows of the person, sign-in,
  session and ask say.

### 3. Person and actor

States:

- **X0** an id that names nothing under the root in question.
- **X1** a waiting person.
- **X2** an entered person, own layer not made.
- **X3** an entered person, own layer made.
- **X4** a live agent at depth 1 to 31.
- **X5** a live agent at depth 32.
- **X6** an ended agent.

Related reads: `who-is` (own layer, flag, `:actor-ended?` of its sign-ins),
`under` (the actors), `watch-root` (through its sign-ins), `ask-of` (an
answer's actor).

**X0 × invite {X, C}** → X1 (or `:claim-in-use`, X0).
- who-is X: nil, nil, nil, false.
- under X: empty.
- watch-root X: no call.
- ask-of: unchanged.

**X1 × invite {P, C}** → the same claim: no change, `{:invited P}`; another
claim: `:person-in-use`.
- who-is P: unchanged.
- under P: empty.
- watch-root P: no call.
- ask-of: unchanged.

**X2, X3 × invite {P, C}** → her claim: `{:invited P}`, no change; another:
`:person-in-use`.
- who-is P: unchanged.
- under P: unchanged.
- watch-root P: no call for this write.
- ask-of: unchanged.

**X4, X5, X6 × invite {A, C}** → A is no root, so Auth sees an unknown
person; A becomes a waiting person beside the agent of the same id under
another root. The runtime's ids never do this, and Auth does not check
across roots.
- who-is {A, ...}: nil, nil, nil, false.
- under of the agent's root: unchanged.
- watch-root: no call.
- ask-of: unchanged.

**X0, X4, X5, X6 × enter** → an enter reaches a person only through a link
or a waiting claim; it cannot name these. Unchanged.
- who-is: unchanged.
- under: unchanged.
- watch-root: no call.
- ask-of: unchanged.

**X1 × enter proving her claim** → X2; own layer and making fixed.
- who-is P: own-layer L, made false; sign-in nil.
- under P: empty.
- watch-root P: no call.
- ask-of: unchanged.

**X2 × enter by her account** → X2; `:says` latest.
- who-is P: L, false.
- under P: unchanged.
- watch-root P: no call for this write.
- ask-of: unchanged.

**X3 × enter by her account** → X3; `:says` latest.
- who-is P: L, true.
- under P: unchanged.
- watch-root P: no call for this write.
- ask-of: unchanged.

**X0, X1, X4, X5, X6 × own-layer-made** → `{:refused :no-such-person}`;
unchanged.
- who-is: unchanged.
- under: unchanged.
- watch-root: no call.
- ask-of: unchanged.

**X2 × own-layer-made** → X3.
- who-is P: made true, and so the next enter's result.
- under P: unchanged.
- watch-root P: no call for this write.
- ask-of: unchanged.

**X3 × own-layer-made** → `{:made P}`; unchanged.
- who-is P: made true.
- under P: unchanged.
- watch-root P: no call for this write.
- ask-of: unchanged.

**X0, X1 × begin-sign-in** (as the actor, or as the root) →
`:no-such-actor`; unchanged.
- who-is {P, G}: sign-in nil.
- under P: unchanged.
- watch-root P: no call.
- ask-of: unchanged.

**X2 × begin-sign-in, A = P** → a sign-in for her begins; Auth does not
refuse it for her layer not being made (Q1).
- who-is {P, G}: actor P, not ended; own layer L, made false.
- under P: G listed.
- watch-root P: a call that holds G.
- ask-of: unchanged.

**X3 × begin-sign-in, A = P** → a sign-in for her begins.
- who-is {P, G}: actor P, `:ended? false`, `:actor-ended? false`.
- under P: G listed.
- watch-root P: a call that holds G.
- ask-of: unchanged.

**X4, X5 × begin-sign-in for A under P** → a sign-in for A begins.
- who-is {P, G}: actor A, `:actor-ended? false`.
- under P: G listed.
- watch-root P: a call that holds G.
- ask-of: unchanged.

**X2, X3, X4, X5 of another root × begin-sign-in under P** →
`:no-such-actor`; unchanged.
- who-is {P, G}: nil.
- under of either root: unchanged.
- watch-root: no call for this write.
- ask-of: unchanged.

**X6 × begin-sign-in** → `:ended`; no sign-in.
- who-is {P, G}: nil.
- under P: unchanged.
- watch-root P: no call for this write.
- ask-of: unchanged.

**X0 × make-actor, as the new A, with B a live actor of P at depth 0 to 31**
→ A is made at B's depth plus one (X4, or X5 at 32).
- who-is: unchanged (no sign-in yet).
- under P: A listed with B, its label, `:ended? false`.
- watch-root P: no call for this write.
- ask-of: unchanged.

**X2, X3 (A = P), X4, X5, X6 under P × make-actor, as the new A** → a repeat
(the same root and maker as A's own make) gives `{:made A}`; otherwise
`:in-use`. Unchanged.
- who-is: unchanged.
- under P: unchanged.
- watch-root: no call for this write.
- ask-of: unchanged.

**X1, and X2 to X6 of another root × make-actor, as the new A under P** →
not checked across roots: A is made under P. The runtime's ids never do
this.
- who-is: unchanged.
- under P: A listed; the other root's under unchanged.
- watch-root: no call for this write.
- ask-of: unchanged.

**X0, X1 × make-actor, as the maker B** → `:no-such-maker`; unchanged.
- who-is: unchanged.
- under P: unchanged.
- watch-root: no call.
- ask-of: unchanged.

**X2, X3 (B = P), X4 under P × make-actor, as the maker B** → A is made
(see the X0 row above); B is unchanged.
- who-is: unchanged.
- under P: A listed with `:made-by B`.
- watch-root: no call for this write.
- ask-of: unchanged.

**X5 × make-actor, as the maker B** → `:too-deep`; unchanged.
- who-is: unchanged.
- under P: unchanged.
- watch-root: no call for this write.
- ask-of: unchanged.

**X2 to X6 of another root × make-actor, as the maker B under P** →
`:no-such-maker`; unchanged.
- who-is, under: unchanged for both roots.
- watch-root: no call for this write.
- ask-of: unchanged.

**X6 × make-actor, as the maker B** → `:ended`; unchanged.
- who-is: unchanged.
- under P: unchanged.
- watch-root: no call for this write.
- ask-of: unchanged.

**X0, X1 × end-actor** → `:no-such-actor`; unchanged.
- who-is, under: unchanged.
- watch-root: no call.
- ask-of: unchanged.

**X2, X3 × end-actor, A = P** → `:is-a-person`; unchanged.
- who-is P: unchanged.
- under P: unchanged.
- watch-root P: no call for this write.
- ask-of: unchanged.

**X2 to X6 of another root × end-actor under P** → `:no-such-actor`;
unchanged.
- who-is, under: unchanged for both roots.
- watch-root: no call for this write.
- ask-of: unchanged.

**X4, X5 × end-actor** → X6 for A and every actor beneath it; their live
sign-ins ended, their open sessions closed.
- who-is {P, G of A or beneath, S}: `:ended? true`, `:actor-ended? true`;
  session `:closed? true`; own layer unchanged.
- under P: those actors `:ended? true`; their sign-ins no longer listed.
- watch-root P: a call without those sign-ins and sessions, if any were
  live.
- ask-of: an ask answered yes for one of them still gives its answer.

**X6 × end-actor** → `{:ended A}`; unchanged.
- who-is: unchanged.
- under P: unchanged.
- watch-root P: no call for this write.
- ask-of: unchanged.

**X0, X1 (as P) × answer-ask, yes or no** → `:no-such-actor`; the ask keeps
waiting.
- ask-of: `:waiting`.
- who-is, under: unchanged.
- watch-root: no call.

**X2, X3 (as P) × answer-ask, yes, A = P, in time** → a `:program` sign-in G
for her, with the ask's hash and `:asks` as its label (for X2, not refused:
Q1).
- ask-of: `:yes`, answer {P, P, G}.
- who-is {P, G}: actor P, kind `:program`, until t1.
- under P: G listed, label the ask's `:asks`.
- watch-root P: a call that holds G.

**X4, X5 (as A, under P) × answer-ask, yes, no new agent, in time** → a
`:program` sign-in G for A.
- ask-of: `:yes`, answer {P, A, G}.
- who-is {P, G}: actor A.
- under P: G listed.
- watch-root P: a call that holds G.

**X6 (as A) × answer-ask, yes, no new agent** → `:ended`; the ask keeps
waiting.
- ask-of: `:waiting`.
- who-is, under: unchanged.
- watch-root: no call for this write.

**X0 (as A) × answer-ask, yes, no new agent** → `:no-such-actor`; the ask
keeps waiting.
- ask-of: `:waiting`.
- who-is, under: unchanged.
- watch-root: no call for this write.

**X0 (as A) × answer-ask, yes, new agent** → A made at depth 1 (X4), made by
P, with the label; G for A.
- ask-of: `:yes`, answer {P, A, G}.
- who-is {P, G}: actor A, `:actor-ended? false`.
- under P: A listed with `:made-by P`; G listed.
- watch-root P: a call that holds G.

**X2, X3 (A = P), X4, X5, X6 under P (as the new A) × answer-ask, yes, new
agent, not the answer that stands** → `:in-use`; nothing written; the ask
keeps waiting.
- ask-of: `:waiting`.
- who-is, under: unchanged.
- watch-root: no call for this write.

**X2, X3 (as P) × answer-ask, no, in time** → the ask is answered no; she is
unchanged.
- ask-of: `:no`, answer nil.
- who-is, under: unchanged.
- watch-root: no call for this write.

**X0 to X6 × replace-secret, end-sign-in, open-session, close-session, ask**
→ no actor changes.
- who-is: own layer, flag and `:actor-ended?` unchanged.
- under: actors unchanged (sign-ins as the sign-in rows say).
- watch-root: as the sign-in and session rows say.
- ask-of: as the ask rows say.

### 4. Sign-in

States, held against the event's `:at`:

- **S0** the id is not bound under P.
- **S1** live, with no previous secret.
- **S2** live, with a previous secret.
- **S3** not ended, its actor live, but its present secret's `:until` at or
  before this event's `:at`: this event takes it out first.
- **S4** ended by `end-sign-in`.
- **S5** ended because its actor or an actor above ended.
- **S6** taken out by an earlier event, its time having passed.

Related reads: `who-is` of P and G (the sign-in part, and the session part
for sessions under G), `under` P (the sign-ins), `watch-root` P, `ask-of`
(for a sign-in that a yes began).

**S0 × begin-sign-in, A live under P, t1 after t** → S1.
- who-is {P, G}: actor A, kind K, hash H, until t1, previous nil, not
  ended; session part nil for any S.
- under P: G listed, `:begun` t.
- watch-root P: a call that holds G.
- ask-of: unchanged.

**S0 × begin-sign-in, A live under P, t1 at or before t** → taken, never
live.
- who-is {P, G}: `:ended? true`, until t1.
- under P: not listed.
- watch-root P: no call for G.
- ask-of: unchanged.

**S0 × begin-sign-in, A not an actor under P, or ended** → refused; S0.
- who-is {P, G}: nil.
- under P: unchanged.
- watch-root P: no call for G.
- ask-of: unchanged.

**S1, S2, S4, S5, S6 × begin-sign-in, the same G with the same parts** →
`{:begun G}`; no change.
- who-is: as before (S4, S5, S6 still `:ended? true`).
- under P: as before (S4, S5, S6 not listed).
- watch-root P: no call for G.
- ask-of: unchanged.

**S3 × begin-sign-in, the same G with the same parts** → G is taken out
first (S6); then `{:begun G}`.
- who-is: `:ended? true`; its sessions `:closed? true`.
- under P: G no longer listed.
- watch-root P: a call without G and its sessions.
- ask-of: unchanged.

**S1, S2, S4, S5, S6 × begin-sign-in, the same G with other parts** →
`:in-use`; no change.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: unchanged.

**S3 × begin-sign-in, the same G with other parts** → G is taken out first
(S6); then `:in-use`.
- who-is: `:ended? true`.
- under P: G no longer listed.
- watch-root P: a call without G and its sessions.
- ask-of: unchanged.

**S0 × replace-secret** → `:not-live`; S0.
- who-is: nil.
- under, watch-root, ask-of: unchanged.

**S1 × replace-secret, H its hash now, t1 after t** → S2: hash H', until
t1, previous {H, t2}.
- who-is: hash H', until t1, previous {H, t2}; sessions under G unchanged.
- under P: `:until` t1; `:begun` unchanged.
- watch-root P: a call with the new hash, until and previous; sessions
  unchanged.
- ask-of: an ask that began G still gives its own hash.

**S2 × replace-secret, H its hash now, t1 after t** → S2 with previous {H,
t2}; the secret before H stands no more.
- who-is: hash H', previous {H, t2}.
- under P: `:until` t1.
- watch-root P: a call with the new values.
- ask-of: unchanged.

**S1, S2 × replace-secret, H its hash now, t1 at or before t** → replaced,
and G is no longer live: ended, with its sessions (S6).
- who-is: hash H', until t1, `:ended? true`; its sessions closed.
- under P: G no longer listed.
- watch-root P: a call without G and its sessions.
- ask-of: unchanged.

**S1, S2 × replace-secret, H not its hash now** (the previous hash
included) → `:wrong-secret`; no change.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: unchanged.

**S1, S2, S4, S5, S6 × replace-secret, a repeat** (H' its hash now, H the
previous) → `{:replaced G}`; no change.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: unchanged.

**S3 × replace-secret, a repeat or not** → G is taken out first (S6); then
`{:replaced G}` for a repeat, `:not-live` otherwise.
- who-is: `:ended? true`; its sessions closed.
- under P: G no longer listed.
- watch-root P: a call without G and its sessions.
- ask-of: unchanged.

**S4, S5, S6 × replace-secret, not a repeat** → `:not-live`; no change.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: unchanged.

**S0 × end-sign-in** → `:no-such-sign-in`; S0.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: unchanged.

**S1, S2 × end-sign-in** → S4; every open session under G closed.
- who-is: `:ended? true`, `:actor-ended? false`; each session under G
  `:closed? true`.
- under P: G no longer listed.
- watch-root P: a call without G and its sessions.
- ask-of: an ask that began G still gives its answer.

**S3 × end-sign-in** → G is taken out first (S6), with its sessions; then
`{:ended G}`.
- who-is: `:ended? true`; sessions closed.
- under P: G no longer listed.
- watch-root P: a call without G and its sessions.
- ask-of: unchanged.

**S4, S5, S6 × end-sign-in** → `{:ended G}`; no change.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: unchanged.

**S0 × end-actor** → no sign-in to change.
- who-is, under, watch-root, ask-of: unchanged by G.

**S1, S2 × end-actor of its actor or one above** → S5; its open sessions
closed.
- who-is: `:ended? true`, `:actor-ended? true`; sessions `:closed? true`.
- under P: G no longer listed; the actor `:ended? true`.
- watch-root P: a call without G and its sessions.
- ask-of: unchanged.

**S3 × end-actor of its actor or one above** → G is taken out first (S6);
then its actor ends.
- who-is: `:ended? true`, `:actor-ended? true`; sessions closed.
- under P: G no longer listed; the actor `:ended? true`.
- watch-root P: a call without G and its sessions.
- ask-of: unchanged.

**S4, S6 × end-actor of its actor or one above** → G stays ended; its actor
is now ended.
- who-is: `:ended? true`; `:actor-ended?` now true.
- under P: G not listed; the actor `:ended? true`.
- watch-root P: no call for G.
- ask-of: unchanged.

**S5 × end-actor** → no change.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: unchanged.

**S1, S2 × open-session under G** → a session opens; G does not change.
- who-is {P, G, S}: sign-in part unchanged; session {G, its until,
  `:closed? false`}.
- under P: unchanged.
- watch-root P: a call that holds S.
- ask-of: unchanged.

**S0, S4, S5, S6 × open-session under G** → `:not-live` (a repeat of a
session already under G gives `{:open S}`); G does not change.
- who-is: sign-in part unchanged; session part as before.
- under, watch-root: unchanged by G.
- ask-of: unchanged.

**S3 × open-session under G** → G is taken out first (S6), with its
sessions; then `:not-live`, or `{:open S}` for a repeat.
- who-is: `:ended? true`; sessions closed.
- under P: G no longer listed.
- watch-root P: a call without G and its sessions.
- ask-of: unchanged.

**S0 × close-session** → no session can be under an unbound sign-in; G
stays unbound.
- who-is {P, G}: sign-in nil.
- under, watch-root, ask-of: unchanged by G.

**S1, S2 × close-session of a session under G** → G does not change.
- who-is: sign-in part unchanged; that session `:closed? true`.
- under P: unchanged.
- watch-root P: a call without S, if it was open.
- ask-of: unchanged.

**S3 × close-session of a session under G** → G is taken out first (S6),
with its sessions; then `{:closed S}`.
- who-is: `:ended? true`; sessions closed.
- under P: G no longer listed.
- watch-root P: a call without G and its sessions.
- ask-of: unchanged.

**S4, S5, S6 × close-session of a session under G** → `{:closed S}`;
nothing changes.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: unchanged.

**S0 × answer-ask, yes, P and A valid, in time** → S1, kind `:program`,
hash the ask's (never live if t1 is at or before t).
- who-is {P, G}: actor A, kind `:program`, hash the ask's, until t1,
  previous nil.
- under P: G listed, label the ask's `:asks`, `:begun` the answer's `:at`.
- watch-root P: a call that holds G.
- ask-of: `:yes` with G.

**S1, S2, S4, S5, S6 × answer-ask, yes naming G, not the answer that
stands** → `:in-use`; nothing written.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: `:waiting`.

**S3 × answer-ask, yes naming G, not the answer that stands** → G is taken
out first (S6); then `:in-use`.
- who-is: `:ended? true`.
- under P: G no longer listed.
- watch-root P: a call without G and its sessions.
- ask-of: `:waiting`.

**S0, S1, S2, S4, S5, S6 × answer-ask, no** → G does not change.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: `:no`.

**S3 × answer-ask, no, by P** → G is taken out (S6); the ask is answered no.
- who-is: `:ended? true`.
- under P: G no longer listed.
- watch-root P: a call without G and its sessions.
- ask-of: `:no`.

**S0, S1, S2, S4, S5, S6 × invite, enter, own-layer-made, make-actor, ask**
→ G does not change.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: unchanged.

**S3 × invite, enter, own-layer-made, make-actor** (events under P) → G is
taken out (S6), with its sessions.
- who-is: `:ended? true`; sessions closed.
- under P: G no longer listed.
- watch-root P: a call without G and its sessions.
- ask-of: unchanged.

**S3 × ask** → an ask is under no root: G does not change.
- who-is, under: unchanged.
- watch-root: no call for G.
- ask-of: the new ask only.

### 5. Session

States, held against the event's `:at`:

- **N0** the id is not bound under P.
- **N1** open and live at this event.
- **N2** open, its own `:until` at or before this event's `:at`: this event
  takes it out first.
- **N3** open, its sign-in's time at or before this event's `:at`: this
  event takes out the sign-in, and the session with it.
- **N4** closed: by `close-session`, `end-sign-in` or `end-actor`.
- **N5** taken out by an earlier event, its time or its sign-in's having
  passed.

Related reads: `who-is` of P, G and S (the session part, and the sign-in part
of its own G), `watch-root` P (the sessions), `under` P (it lists no
session: unchanged in every row, and listed so that every row says so).

**N0 × open-session, G live under P, t1 after t** → N1.
- who-is {P, G, S}: session {G, until t1, `:closed? false`}.
- watch-root P: a call that holds S with its sign-in and until.
- under P: unchanged.

**N0 × open-session, G live under P, t1 at or before t** → taken, never
live.
- who-is: session {G, until t1, `:closed? true`}.
- watch-root P: no call for S.
- under P: unchanged.

**N0 × open-session, G not live** (not under P, or ended in any way) →
`:not-live`; N0.
- who-is: session nil.
- watch-root: no call for S.
- under: unchanged.

**N1 × open-session, the same G, a later `:until`** → N1, its time moved to
the new `:until`.
- who-is: session `:until` the new one.
- watch-root P: a call with S's new until.
- under: unchanged.

**N1 × open-session, the same G, an `:until` no later than its own** →
`{:open S}`; no change.
- who-is: unchanged.
- watch-root: no call for S.
- under: unchanged.

**N2 × open-session, the same G** → S is taken out first (N5); then
`{:open S}`; its time is not moved.
- who-is: `:closed? true`.
- watch-root P: a call without S.
- under: unchanged.

**N3 × open-session, the same G** → G is taken out first, and S with it
(N5); then `{:open S}`.
- who-is: session `:closed? true`; sign-in `:ended? true`.
- watch-root P: a call without G and S.
- under P: G no longer listed.

**N4, N5 × open-session, the same G** → `{:open S}`; it stays closed.
- who-is: `:closed? true`.
- watch-root: no call for S.
- under: unchanged.

**N1, N4, N5 × open-session, another sign-in G2 of P** → `:in-use`; no
change.
- who-is: S still under G.
- watch-root: no call for S.
- under: unchanged.

**N2, N3 × open-session, another sign-in G2 of P** → S (and for N3 its
sign-in) is taken out first (N5); then `:in-use`.
- who-is: `:closed? true`.
- watch-root P: a call without S.
- under: unchanged for N2; for N3, its sign-in no longer listed.

**N0 × close-session** → `:no-such-session`; N0.
- who-is: session nil.
- watch-root: no call for S.
- under: unchanged.

**N1 × close-session** → N4.
- who-is: `:closed? true`; sign-in part unchanged.
- watch-root P: a call without S.
- under: unchanged.

**N2, N3 × close-session** → taken out first (N5), and for N3 its sign-in
too; then `{:closed S}`.
- who-is: `:closed? true`.
- watch-root P: a call without S.
- under: unchanged for N2; for N3, its sign-in no longer listed.

**N4, N5 × close-session** → `{:closed S}`; no change.
- who-is, watch-root, under: unchanged.

**N0 × end-sign-in, end-actor** → no session to change.
- who-is, watch-root, under: unchanged by S.

**N1 × end-sign-in of its G** → N4.
- who-is: `:closed? true`; sign-in `:ended? true`.
- watch-root P: a call without S and G.
- under P: G no longer listed.

**N2 × end-sign-in of its G** → S is taken out first (N5); then G ends.
- who-is: `:closed? true`; sign-in `:ended? true`.
- watch-root P: a call without S and G.
- under P: G no longer listed.

**N3 × end-sign-in of its G** → G and S are taken out first (N5); then
`{:ended G}`.
- who-is: `:closed? true`; sign-in `:ended? true`.
- watch-root P: a call without S and G.
- under P: G no longer listed.

**N4, N5 × end-sign-in of its G** → S does not change.
- who-is: `:closed? true`; the sign-in as the sign-in rows say.
- watch-root, under: as the sign-in rows say.

**N1 × end-actor of its sign-in's actor or one above** → N4.
- who-is: `:closed? true`; sign-in `:ended? true`, `:actor-ended? true`.
- watch-root P: a call without S and its sign-in.
- under P: the sign-in no longer listed; the actor `:ended? true`.

**N2, N3 × end-actor of its sign-in's actor or one above** → taken out
first (N5); then the actor ends.
- who-is: `:closed? true`; sign-in `:ended? true`, `:actor-ended? true`.
- watch-root P: a call without S and its sign-in.
- under P: the sign-in no longer listed; the actor `:ended? true`.

**N4, N5 × end-actor** → S does not change.
- who-is: `:closed? true`.
- watch-root, under: as the actor and sign-in rows say.

**N0 × replace-secret** → no session to change.
- who-is, watch-root, under: unchanged by S.

**N1 × replace-secret of its G** → S does not change (SPEC).
- who-is: session part unchanged; sign-in part with the new hash.
- watch-root P: a call (the sign-in changed); S still held.
- under P: the sign-in's `:until` changed.

**N2 × replace-secret of its G** → S is taken out first (N5); the
replacement is then judged on G, which is live.
- who-is: `:closed? true`; the sign-in as the sign-in rows say.
- watch-root P: a call without S, with G's new values.
- under P: G's `:until` changed.

**N3 × replace-secret of its G** → G and S are taken out first (N5); then
`:not-live`, or `{:replaced G}` for a repeat.
- who-is: `:closed? true`; sign-in `:ended? true`.
- watch-root P: a call without S and G.
- under P: G no longer listed.

**N4, N5 × replace-secret of its G** → S does not change.
- who-is: `:closed? true`; the sign-in as the sign-in rows say.
- watch-root, under: as the sign-in rows say.

**N0, N1, N4, N5 × invite, enter, own-layer-made, begin-sign-in,
make-actor, answer-ask** → S does not change.
- who-is, watch-root, under: unchanged by S.

**N2, N3 × invite, enter, own-layer-made, begin-sign-in, make-actor,
answer-ask** (events under P) → S is taken out (N5), and for N3 its sign-in
too.
- who-is: `:closed? true`.
- watch-root P: a call without S.
- under: unchanged for N2; for N3, its sign-in no longer listed.

**N0 to N5 × ask** → an ask is under no root: S does not change.
- who-is, watch-root, under: unchanged by S.

### 6. Ask

States, held against the event's `:at`:

- **K0** the code is not bound.
- **K1** waiting, the event's `:at` before `:ends`.
- **K2** waiting, the event's `:at` at or after `:ends` (over).
- **K3** answered yes, with {P, A, G}.
- **K4** answered no.

An ask is never taken out: it is under no root, and `ask-of` gives
`:waiting` until an answer stands. Related reads: `ask-of` U, and for a yes,
`who-is` of P and G, `under` P, `watch-root` P.

**K0 × ask {U, H}, `:ends` after t** → K1.
- ask-of U: `{:hash H :asks ... :ends t1 :state :waiting :answer nil}`.
- who-is, under: unchanged.
- watch-root: no call.

**K0 × ask {U, H}, `:ends` at or before t** → taken, over from the start
(K2 for every answer).
- ask-of U: `:waiting`, with an `:ends` that has passed.
- who-is, under: unchanged.
- watch-root: no call.

**K1, K2, K3, K4 × ask, the same U and H** → `{:asked U}`; no change.
- ask-of: as before (K3 still `:yes` with its answer).
- who-is, under: unchanged.
- watch-root: no call.

**K1, K2, K3, K4 × ask, the same U and another hash** → `:in-use`; no
change.
- ask-of: as before.
- who-is, under: unchanged.
- watch-root: no call.

**K0 × answer-ask** → `:no-such-ask`.
- ask-of: nil.
- who-is, under: unchanged.
- watch-root: no call for this write.

**K1 × answer-ask, yes as herself, P entered** → K3 {P, P, G}.
- ask-of: `:yes`, answer {P, P, G}.
- who-is {P, G}: a live `:program` sign-in of P, with the ask's hash.
- under P: G listed, label the ask's `:asks`.
- watch-root P: a call that holds G.

**K1 × answer-ask, yes as a live agent A of hers** → K3 {P, A, G}.
- ask-of: `:yes`, answer {P, A, G}.
- who-is {P, G}: a live `:program` sign-in of A.
- under P: G listed.
- watch-root P: a call that holds G.

**K1 × answer-ask, yes as a new agent A** → K3 {P, A, G}; A made by P.
- ask-of: `:yes`, answer {P, A, G}.
- who-is {P, G}: a live sign-in of A.
- under P: A listed, `:made-by P`, its label; G listed.
- watch-root P: a call that holds G.

**K1 × answer-ask, yes, P not entered, or A not hers, or A ended** →
refused; K1.
- ask-of: `:waiting`.
- who-is, under: unchanged.
- watch-root: no call for this write.

**K1 × answer-ask, yes, the new A or G another's under P** → `:in-use`;
nothing written; K1.
- ask-of: `:waiting`.
- who-is, under: unchanged.
- watch-root: no call for this write.

**K1 × answer-ask, no, P entered** → K4.
- ask-of: `:no`, answer nil.
- who-is, under: unchanged.
- watch-root: no call for this write.

**K1 × answer-ask, no, P not entered** → `:no-such-actor`; K1.
- ask-of: `:waiting`.
- who-is, under: unchanged.
- watch-root: no call.

**K2 × answer-ask, any** → `:over`; K2.
- ask-of: `:waiting`, with an `:ends` that has passed.
- who-is, under: unchanged.
- watch-root: no call for this write.

**K3 × answer-ask, any** (the same answer, another, another person, any
`:at`) → `{:yes {:root P :actor A :sign-in G}}`, what stands.
- ask-of: unchanged.
- who-is {P, G}: as it is now (G may have ended since).
- under P: unchanged.
- watch-root: no call for this write.

**K4 × answer-ask, any** → `{:no U}`.
- ask-of: unchanged.
- who-is, under: unchanged.
- watch-root: no call for this write.

**K0 to K4 × invite, enter, own-layer-made, begin-sign-in, replace-secret,
end-sign-in, make-actor, end-actor, open-session, close-session** → the ask
does not change.
- ask-of: unchanged; for K3, the answer stays as given even when G ends, is
  taken out or has its secret replaced, or A ends.
- who-is, under, watch-root of the answer's P: as the rows of those writes
  say.

## Questions for the spec's author

What is still open, or where the SPEC pulls against itself. Each says what
this page holds meanwhile.

**Q1. Is "no sign-in of hers begins before her layer is made" a rule of
Auth?** The SPEC rests the answer that only an enter sends the making act
again on it: "no sign-in of hers begins before her layer is made, so the
flag of `who-is` informs and nothing acts on it". But no write refuses a
sign-in, a yes or an agent for a person whose flag is false, and the contract
has no word for it. If a copy begins her sign-in after `own-layer-made` fails
(a `:could-not`), she acts with a layer that may not exist, and only her next
enter sends the act again. This page holds: it is the runtime's order of
calls, and Auth does not refuse. Options: (a) as held; (b) `begin-sign-in`
and a yes whose root's flag is false are refused, with a new word; (c) Auth
refuses, and `who-is` gives `:making` again so that the copy that meets the
refusal can send the act.

**Q2. Which events take out what has passed, and does a repeat's take-out
break "leaves the same state as once"?** The SPEC says "each event under a
root" takes out what has passed, and that a repeat "leaves the same state as
once" and a refusal "binds nothing". A repeat that arrives later carries a
later `:at`, and if it is an event it takes out more than the first did; a
refusal, if it is an event, changes what is live. The SPEC does not say which
writes are events under a root (`invite` and `own-layer-made` name a
`:person`, `enter` names none until it gives one), nor whether the take-out
comes before the event's own write is judged. This page holds: every write
that names an entered person as `:root` or `:person`, or comes to one, is an
event under her root, taken, repeated or refused; an `ask` is not; the
take-out comes first; and "the same state as once" means the write's own
effect, while take-outs follow the time. Options: (a) as held; (b) only a
write that is taken (not a repeat, not a refusal) takes out, so a repeat
leaves exactly the state of once; (c) only the writes that name `:root` take
out.

**Q3. The repeat of `replace-secret`.** The SPEC's text still says "`H` is
the sign-in's hash now, or `H'` is already its hash", which takes as a repeat
any write whose H' is the hash now, whatever its H. This page holds the
narrower reading: a repeat is H' now and H the previous hash. A repeat that
arrives after a newer replacement then gives `:wrong-secret`, not "the result
of the first". Options: (a) as held, and the SPEC's text says so; (b) the
SPEC's text as written; (c) Auth recognises a repeat of any replacement of
the sign-in's past, which needs the past hashes to stay readable.

**Q4. The previous secret, and a sign-in's time.** Does an event whose `:at`
is past t2 take `:previous` out of `who-is` and the watch, or does it stay
until the next replacement? And a sign-in is taken out at its present
secret's `:until` even if the previous secret's t2 is later. This page
holds: `:previous` stays until a later replacement, and the runtime judges
t2; a sign-in's time is its present secret's alone.

**Q5. The order of the checks, and one word.** Where two refusals apply to
one write, the SPEC does not say which comes first: `invite`
(`:claim-in-use` or `:person-in-use`), `begin-sign-in`, `make-actor`
(`:ended` or `:too-deep`), `open-session`, `answer-ask` (`:no-such-actor`,
`:ended` or `:in-use`). This page holds the orders given in each operation:
a repeat first, then the refusals in the contract's order, with
`:too-deep` last before taking. The contract names `:in-use` for a session
"open under another sign-in"; for a session closed or taken out under
another sign-in, which "is not opened again", it names no word; this page
holds `:in-use`.

**Q6. Two parts with no bound.** A provider is a keyword, with no bound on
its length; `:own-layer` has no stated form or bound. This page holds that
both are bounded (a provider of at most 256 characters; `:own-layer` of the
store's layer-id form) and that a write beyond either does not fit.

**Q7. `close!` and the watches.** The SPEC says a call on a closed client
gives `{:could-not :closed}`, and that `watch-root` gives a function. It
does not say whether a watch on a closed client gives that map in place of
the function, nor whether its watches get `not-reached` once or stop
without a call. This page holds: the map in place of the function, and no
further call to any `f`.

**Q8. How many watched roots one task must hold.** A planning figure for
the plan to size: the SPEC asks only that Rama's default not bind it.

**Q9. Latency and rate targets.** Every millisecond figure on this page, the
rate of `who-is`, and how often the runtime moves an open session's time on
(which sets the volume of `open-session` and of the watch's calls) are
planning figures for the plan to size.
