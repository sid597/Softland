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

**22 November 2026.** Sid's calls on the contract, in the prompt that he
pasted to the build of the reference, are written below where each belongs,
each marked "Sid's call of 22 November". Two of them are rules: what fits,
and that a repeat's result comes from what stands, never from a memory of
calls. With them, questions 1 to 7, 12, 20 and 23 of the reference's
questions (`test-auth/softland/auth/QUESTIONS.md`) are settled. Questions
10, 17 and 24 stand as the reference took them, and stay his to turn.

**28 November 2026.** Sid said yes to three changes of what Auth stores, as a
session put them to him (the review of 26 November, rows F6, F17 and F30:
`src/proposal/design-pitfalls-review-2026-11-26/DECISIONS-AND-FIXES.md`). His
yes covers each change; the names and shapes are a session's, and his to
turn. Each is written below where it belongs, marked "28 November".

- Who made an agent and whose ending ends it are stored apart (F30). The
  second starts as the maker. Who may point an agent at another actor is not
  decided, and no write does it.
- A program that waits for her yes makes a second secret only for asking,
  and an ask stores its hash beside the hash of the secret that becomes the
  credential (F6). The runtime answers a program that waits against the
  second alone, so the secret of the credential crosses no wire before her
  yes.
- A time that passes ends nothing (F17). Auth stores a sign-in's and a
  session's "good until T" and never writes "ended" because T passed;
  "ended" and "closed" are written only when someone ends the thing. The
  section "What is watched, and what a time that passes does" says what
  replaces the taking out at a write, which he asked to see. With it,
  questions 9, 10 and 17 of the reference's questions have new readings.

**28 November 2026, later.** Sid said yes to three more changes, as a
session put them to him: "several accounts per person? An erased email
becomes free again? Invitations end and can be taken back?" His answer: "Yes
to all that i dont explicitly comment below", with no comment on these. So a
person can have several accounts; an erased or unlinked email is free again;
an invitation has an end and can be taken back. The third goes with the
second: otherwise whoever later holds a freed address enters as the person
who was invited. His decision of 9 October, entry 1 (`src-store/README.md`),
stands under this: an account "is added, replaced or removed by its link".
What it takes for an email to be erasable from what Auth keeps was a
session's to work out, and is in "What can be erased later". Every rule,
shape and name below that came with these is a session's, and his to turn;
each is marked "28 November, later". Not part of it: `:driven-by` leaving
Auth, whether a program may act under a person's own id, a block on a
person, a person's name.

## What Auth is

Auth is a Rama module of its own beside the module `Store`. It holds what
sign-in keeps: which account is which person, who may become a person, which
actors a person has made, which secret stands for which actor, and which
sessions are open. The runtime is its one client: many copies of one JVM
program, each of which holds no state of its own. Nothing true about any of
this lives outside Auth's log.

Auth is apart from the store because it makes another promise about time. The
store's two depots of acts are never trimmed and nothing in them is erased. In
Auth, what names a human being (her account, her email, what her provider
said of her) must be erasable, and what has ended may one day be thinned.
What it takes is in "What can be erased later": what Auth keeps to be read
is removed by its writes, and what its events hold by an operator's step
that is not built; nothing here makes either impossible.

Auth never reads the store, and the store never reads Auth.

## Words

- **Account.** A proof of a human being by a provider: `[provider subject]`,
  as `[:google "1082…"]`. The subject is the provider's own text, kept as
  given.
- **Person.** A human being in Softland: a random UUID that says nothing.
- **Link.** Account to person. An account has one person at most; a person
  can have several accounts (28 November, later). What the provider said at
  her sign-ins is kept with the link. A link has an id, the id of the enter
  that made it, which `accounts-of` gives and by which `unlink` names it.
- **Claim.** What an invitation is made with: `{:by :email :hash H}`, the
  hash of an email address, or `{:by :code :hash H}`, the hash of a code. Two
  kinds of one form. Auth only matches a claim and never reads one, so it is
  given the hash, never the address (28 November, later): the runtime
  lower-cases the address, in the root locale, and hashes it. A claim
  **names** one person at a time: its person while a link that was made by
  proving it stands, or while an invitation of hers waits under it that is
  not over. It is **free** otherwise.
- **Invitation.** A person who waits for an account, under a claim, until an
  end (28 November, later). It has an id, the op of the invite that made it.
  It **lets in** an enter whose `:at` is at or after the invitation's own
  `:at` and before its `:ends`: the first account that proves the claim so
  becomes that person's account, and the invitation waits no longer. It is
  **over** at or after its `:ends`. It can be taken back.
- **Actor.** A who: something that sends acts. A person is an actor. An
  **agent** is an actor that another actor made; it has an id of its own, a
  random UUID. The **root** of an actor is the person at the top of who made
  whom. A person is her own root. An agent's record says two things apart
  (28 November): who made it (`:made-by`), which is a fact of the past, and
  whose ending ends it (`:ends-with`). The second starts as the maker, and
  no write sets it. The actor **above** an agent is the one whose ending
  ends it.
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
  a person has not yet said yes or no. It is found by a short **code**. It
  holds two hashes that the program sent (28 November): `:hash`, of the
  secret that becomes the sign-in's at a yes, and `:asking-hash`, of a
  secret that the program made only to ask how its ask stands.
- **Standing.** Whether a thing is live now. A sign-in is live when nobody
  has ended it, its secret's time has not passed, its actor has not ended,
  and no actor above that one has ended. A session is live when nobody has
  closed it, its time has not passed, and its sign-in is live. These are
  two kinds of thing (28 November). That someone ended a sign-in, an actor
  or a session is a fact, which Auth stores and never takes back. That a
  time has passed is no fact of Auth's: Auth reads no clock, stores the
  time, and writes "ended" for no time that passed. "Has not passed" is
  judged in two places, and kept in neither: by the runtime, which holds
  each time that a read gives against its own clock, at each read; and by
  Auth at a write that needs the thing live, against that write's own `:at`
  ("What is watched, and what a time that passes does", below).

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
  leaves the same state as once. Its result comes from what stands, never
  from a memory of calls (Sid's call of 22 November). The second call gives
  the result of the first, which says that the write was taken; it does not
  say how the thing stands now, which a read says. One part is the
  exception: an `enter` gives `:own-lane-made?` as it stands at that
  enter. Two writes are the same write when the
  parts that say what is bound are the same: for `invite`, the person and
  the claim, while her invitation waits under it and is not over; for
  `begin-sign-in`, the root, the actor, the kind and the
  hash; for `make-actor`, the root and the maker; for `open-session`, the
  root and the sign-in; for `ask`, the two hashes; for `take-back`, the
  claim and the invite; for `unlink`, the account and the link. The other
  parts of a repeat are dropped, but for `open-session`, whose repeat moves
  the session's time on. `invite` is the exception to "never from a memory
  of calls" (28 November, later): it carries an op-id, fresh for each call
  and the same for each try of one call, and an invite whose op was
  answered gives that answer again and changes nothing, whatever stands
  now, so that an invite that runs again after its invitation was taken
  back, or after the link that it let in was removed, cannot bring it back.
  The same op given again with another person or another claim is not
  defined: the runtime makes the op where it takes the word and cannot
  send it so. A refusal binds nothing: the same write after the state changed
  can be taken, and so can the same write with another `:at`, when it was
  refused for a time that had passed at the first one.
- Where two refusals apply to one write, a repeat is recognised first, then
  the refusals in the order in which the contract lists them.
- A write returns when its effect can be read by any copy of the runtime.
- No write reads a clock. The runtime puts its own clock's time on each
  write as `:at`, milliseconds since 1970. Where a rule below depends on a
  time, it names the time in the event that it is held against.
- Times are fixed-width integers. Ids of persons, actors, sign-ins and
  sessions are UUIDs, and so are `:own-lane` and the two ids of a
  `:making`, its `:uuid` and its `:session`. A hash is 64 lower-case hex
  characters. A code is a string of 8 characters, compared exactly. A text
  (`:label`, `:asks`, a provider's subject, a `:driven-by`
  `:model`) is a string of at most 256 characters, a character being one
  `char` of the JVM. A provider is a keyword whose name is at most 256
  characters. A `:driven-by`, optional on `open-session`, is nil or either
  `{:kind :person}` or `{:kind :model :model text}` (with `:model` a text of
  at most 256 characters). A `:says` map holds at most 32 entries, each
  from a string of at most 256 characters to a string of at most 2,048.
- What fits (Sid's call of 22 November): every part that the contract
  names is there, a part written `-or-nil` as nil when it holds nothing,
  each in its stated form, and nothing that the contract does not name.
  `:driven-by` alone may be left out. A provider's name and `:own-lane`
  are forms like the others, and Auth checks them. The parts of a write
  that it does not judge, as those of a no, must fit all the same. A write
  or a read whose input does not fit gives `{:could-not :read-the-event}`,
  and a write then writes nothing.
- Auth is given no email (28 November, later): a claim and an enter hold
  the hash of one, 64 lower-case hex characters, and a claim or an enter
  that holds an address does not fit. Two emails are the same when their
  hashes are. The runtime owns the address's form: it lower-cases the
  address in the root locale and hashes it, so that every copy of it makes
  the same hash (suggested: SHA-256 of the text `softland/email/` followed
  by the address).
- A string that UTF-8 cannot carry, one that holds a surrogate `char`
  without its pair, does not fit wherever the contract takes a string: a
  text, a code, a provider's name, a key or a value of `:says`. (The
  build's default of 23 November, for Sid to turn: Rama keeps a string as
  UTF-8, so such a string would be kept changed, and two accounts whose
  subjects differ only there would be one.)
- The other words of `:could-not`: `:find-the-module`, when the cluster
  that `open` is given holds no module of that name, or what it is given
  stands for no cluster; `:reach-the-module`, when Rama threw or gave no
  answer in time, with what it threw; and `:closed`, for a call on a client
  that was closed. `watch-root` on a closed client gives
  `{:could-not :closed}` in place of the function, and `close!` calls no
  `f` again. A call whose client is no client at all gives
  `{:could-not :read-the-event}`, and `close!` of it gives nil. A
  `:could-not` from a write says nothing of whether the write was taken.

```text
(open cluster module-name)        -> a client, or {:could-not :find-the-module ...}
(close! client)                   -> nil

writes: each takes the client and one map, and gives a map

invite            {:person P :claim C :invited-by B :at t :ends t1 :op-id I}
                    C is {:by :email :hash H} or {:by :code :hash H}
                    B is an actor's id, or :system
                    I is a UUID, fresh for each call
                  -> {:invited P}
                     {:refused :claim-in-use}       the claim names another person

take-back         {:person P :claim C :invite I :at t}
                  -> {:taken-back P}                the invitation of the op I
                                                    does not wait under C

enter             {:account [provider subject] :says {...} :email-hash H-or-nil
                   :code-hash H-or-nil :own-lane L :making {:uuid U :session M}
                   :at t}
                  -> {:person P :own-lane L' :making {:uuid U' :session M'}
                      :own-lane-made? bool}
                     {:refused :not-invited}

unlink            {:account [provider subject] :link L :person P :at t}
                  -> {:unlinked P}                  the link L does not stand
                                                    on the account
                     {:refused :another-persons}    L stands there for another
                     {:refused :linking}            the enter L is making it

own-lane-made     {:person P :at t}
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

open-session      {:root P :sign-in G :session S :until t1 :at t
                   :driven-by D}                 :driven-by optional;
                                                 {:kind :person} or
                                                 {:kind :model :model text}
                  -> {:open S}
                     {:refused :not-live}
                     {:refused :in-use}          S is open under another sign-in

close-session     {:root P :session S :at t}
                  -> {:closed S}   {:refused :no-such-session}

ask               {:code U :hash H :asking-hash H2 :asks text :ends t1 :at t}
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
                      :session {:sign-in G' :until t3 :closed? bool
                                :driven-by D-or-nil}-or-nil
                      :own-lane L-or-nil :own-lane-made? bool}
                     :ended? and :closed? say that someone ended the
                     thing, never that its time has passed: the caller
                     holds t1 and t3 against its own clock

(ask-of client U) -> {:hash H :asking-hash H2 :asks text :ends t1
                      :state :waiting|:yes|:no
                      :answer {:root P :actor A :sign-in G}-or-nil}, or nil

(invitation-of client C)
                  -> {:person P
                      :invitation {:invite I :invited-by B :invited-at t0
                                   :ends t1}-or-nil
                      :links n}, or nil
                     whom the claim names, the invitation that waits under
                     it, and how many links stand under it; it says nothing
                     of "over": the caller holds t1 against its own clock

(accounts-of client P)
                  -> {:accounts [{:account [provider subject] :link L
                                  :linked-at t} ...]}
                     her accounts, in the order of the :at of the enter that
                     linked each (then by provider and subject)

(under client P)  -> {:actors [{:actor A :made-by B :ends-with B' :label text
                                :ended? bool} ...]
                      :sign-ins [{:sign-in G :actor A :kind K :label text-or-nil
                                  :begun t :until t1} ...]}
                     the agents of the root, ended ones too, in the order
                     in which they were made, each with who made it and
                     whose ending ends it; and its watched sign-ins, in the
                     order in which they began. A sign-in that is not among
                     them has not ended for that

(watch-root client P f)
                  -> a function of no argument that stops the watching
```

`watch-root` calls `f` with one argument each time what is watched under
the root `P` changed, through whichever copy of the runtime the change came:

```text
{:sign-ins {G {:actor A :kind K :hash H :until t1 :previous ...}}   the watched ones
 :sessions {S {:sign-in G :until t3}}                                the watched ones
 :ends n}                                 how many writes under P ended something
```

What is watched is not what stands: a sign-in or a session that nobody
ended can be missing from it ("What is watched, and what a time that passes
does"), so a caller that holds a thing which the value does not hold reads
`who-is` before it ends anything. `:ends` goes up by one at each
`end-sign-in`, `close-session` and `end-actor` that ended or closed
something, whether or not the value held that thing: every end under the
root is a call of `f`. The value does not carry `:driven-by`.

In place of that value, `f` is called with
`:softland.auth.client/not-reached` when this process can no longer say what
is watched under `P`: within seconds of the loss, once, and `f` is not
called again after it. The caller then stops the watch and registers anew.
No call is owed at the registration: a caller that must know how its own
sign-in stands at the start registers first and then reads with `who-is`.
`f` runs on a thread of Rama's client and must return at once.

## What each write does

**invite.** Makes the person `P` wait under the claim `C` until `:ends`
(28 November, later). An invite whose op was answered gives that answer and
changes nothing. Else a claim that names another person at the invite's
`:at`, by a link or by an invitation that is not over, is `:claim-in-use`.
An invitation of `P` that waits under `C` and is not over makes the invite a
repeat, which changes nothing; its `:invited-by` and `:ends` are dropped.
When links of `P` stand under `C` and no invitation of hers waits that is
not over, an invite that is not her own (its `:invited-by` is not `P`)
changes nothing and gives `{:invited P}`: she is in by this claim, and
adding an account to a person who has entered is her act alone (a
session's reading, QUESTIONS.md, 41). Otherwise her invitation is written,
with this `:at` and `:ends`, in the place of any invitation that waited and
is over, and beside her links under the claim when she has some, which is
only when she invites herself. An invitation whose `:ends` is at or before
its own `:at` is taken and lets nobody in. A person may wait under several
claims, and may be invited after she has entered: that is how she adds an
account, an invitation that names herself, proven by her other account.
Under a claim of hers, Auth holds that rule itself; under another claim,
the runtime does: only the person herself, at a browser, may name herself,
and the id of a person is made from the inviter's root, a UUID and the
claim, so that no caller can form the id of a person whom another invited,
or of one under another claim. So the same invite made again, with a fresh
op: while her invitation waits, nothing changes; after it is over or was
taken back, she is invited anew under the same id; after she entered by
it, nothing changes; after she unlinked every account, she is invited anew
(whether the first inviter may bring back a person who unlinked everything
is Sid's).
A person who waits has no sign-in and is no actor yet. `:invited-by` is
kept as given and not checked: who may invite is the runtime's. Nothing is
written when a time passes: an invitation that is over keeps its record,
names nobody and lets nobody in, until the claim is invited again or the
invitation is taken back, or an operator's sweep that is not built drops
it. A clock far ahead at an `invite` finds a waiting invitation over and
replaces it; the invitation that was replaced is gone, and its inviter
invites again.

**take-back.** The invitation that the op `I` made, when it waits under
`C`, waits no longer, whether or not its time has passed; the claim is free
when no link stands under it (28 November, later). It touches no link and
no other invitation, so that a take-back that runs again cannot take back a
later invitation. It gives `{:taken-back P}` always: after it, the
invitation of `I` does not wait under `C`. Who may take an invitation back
is the runtime's: it reads `invitation-of` and takes back the invitation
that it read only for the actor who invited.

**enter.** An account that its provider has proven comes in. The runtime has
checked the provider's token before it calls; Auth takes the account as
proven.

- An account that has a link gives its person, whatever claim came with it,
  with the own lane and the `:making` that were fixed, and
  `:own-lane-made?` as it stands at that enter (Sid's call of 22
  November). An enter that is sent again while its link stands is
  answered so like any other: Auth keeps no memory of what an enter gave.
  An enter that is sent again after its link was removed is judged afresh,
  as any enter of an account that has no link (28 November, later).
- An account that has none takes the person of the claim that it proves: the
  code's, when `:code-hash` names an invitation that lets it in; else the
  email's, when `:email-hash` names one. An invitation lets in an enter
  whose `:at` is at or after the invitation's own `:at` and before its
  `:ends` (28 November, later). The link is written, the invitation waits
  no longer, and the link stands under the claim. A person who has not
  entered becomes an actor, her own root; one who has entered is left as
  she is, and the account becomes one more of hers.
- An account that has none and proves no claim is refused, and nothing of it
  is kept but the event.
- `:own-lane` and `:making` are what the runtime offers for a person who
  has none yet. The first enter that makes her an actor fixes them; every
  later enter gives the ones that were fixed, its own offer dropped, an
  enter of another of her accounts among them.
- `:says` is kept with the link from the enter with the latest `:at`, with
  that `:at`: an enter whose `:at` is not later than the kept one's leaves
  it (Sid's call of 22 November). What other enters said is in the events
  alone. The link keeps no memory of each enter: at full life that would
  grow with every sign-in.

What must hold: two enters of one new account at the same moment, through
two copies of the runtime, give one person, one own lane and one `:making`.
Two accounts that prove one invitation at the same moment: one becomes the
person and the other is refused. An enter that is run again after a failure,
while its link stands, gives the person, the own lane and the `:making` that
it gave, and `:own-lane-made?` as it stands. An enter that the module runs
again, after its account was unlinked and its claim invited for another
person, cannot prove that later invitation: that rests on the rule that an
invitation lets in no enter whose `:at` is before its own, and so on the
clocks of the runtime's copies, as an invitation's end does. An honest
enter from a copy whose clock is behind the inviter's is refused
`:not-invited` until its clock passes the invitation's `:at`, and an
invitation made by a clock far ahead lets nobody in until then; its inviter
takes it back and invites again. When the module has used an invitation
for an enter and cannot then link the account, as when another write
removed what the enter was making, it takes the link out of the claim and
the account out of her accounts; the invitation that the enter used is
spent and is not put back, since putting it back could bring back one that
was taken back meanwhile.

**unlink.** The link `L` of the account, when it stands there for `P`,
goes, with its `:says`; the claim that it was made by proving loses it, and
is free when nothing else stands under it; her accounts lose it (28
November, later). It gives `{:unlinked P}` whenever, after it, the link `L`
does not stand on the account, also when it never did, or when another link
stands there now: so a repeat gives the same, and an unlink that runs again
after the account was linked anew removes nothing. It is refused with
`:another-persons` when `L` stands there for another person, and with
`:linking` while the enter `L` is still making it, which `accounts-of` can
show a moment before the link stands: the caller says it again. It ends no
sign-in and no session: those are the person's, not the account's. A person
whose last account is unlinked keeps what she has until it ends, and cannot
enter again until an invitation names her; her own lane and `:making` stay,
and her next enter gives them.

`:own-lane` and `:making` are how a break between Auth and the store is
met. The runtime writes Auth first. Then it sends the act that makes her own
lane. That act is the system's: its who is the store's `system`, and
`:making` holds its UUID and the id that it names as its session. Auth keeps
the two and gives them back, and nothing more: that session is no session of
Auth's. The act is built from nothing but what the enter gave, so that any
copy sends the same act with the same digest. Then the runtime calls
`own-lane-made`. Until that call every enter says `:own-lane-made? false`,
and the copy that made that enter sends the act again. From that call on,
every enter says true, one that is sent again among them. Only an enter does:
no sign-in of hers begins before her lane is made, so the flag of `who-is`
informs and nothing acts on it. `own-lane-made` of a person who waits is
refused with `:no-such-person`: she has no lane yet.

**begin-sign-in.** From now on the secret whose hash is `H` stands for the
actor `A`, until `t1`. `A` is under the root `P` and is live. A sign-in id is
bound once: the same id with the same parts again changes nothing; with
other parts it is refused. A sign-in whose `t1` is at or before the begin's
own `:at` is taken, with that time: it is not watched, and nobody has ended
it (question 9, as turned on 28 November).

**replace-secret.** The sign-in's secret becomes the one whose hash is `H'`,
with a new end `t1`. `H` is the sign-in's hash now; or it is the same
replacement again, which is so when `H'` is the sign-in's hash now and `H`
is the one before it. A replacement that arrives after a newer one is no
repeat. Like every replacement, it is judged by the present hash and the one
before it alone, for a sign-in keeps no history of its replacements, which
would grow with every one (Sid's call of 22 November): it is refused with
`:wrong-secret` when its old hash is no longer the hash now, and a hash that
has come back is taken when the replacement's old hash is the hash now. The
old secret keeps standing until `t2` and no longer. No session changes. A
sign-in that is not live at the replacement's own `:at` is `:not-live`:
someone ended it or its actor, or its `:until` is at or before that `:at`.
A replacement whose new `:until` is at or before its own `:at` is taken:
the sign-in then has that `:until`, is not watched, and is not ended, and
its sessions are not closed; the standing of each follows the sign-in's at
each read. (Question 10. On 22 November Sid let the reference's reading
stand, in which such a replacement ended the sign-in with its sessions; his
yes of 28 November, that a time which passes ends nothing, turns it.)

**end-sign-in.** The sign-in ends, and every session under it is closed,
watched or not. Ending it again changes nothing. A sign-in whose time has
passed is ended by this like any other.

**make-actor.** Makes the agent `A` under the root `P`, made by the live
actor `B` of that root. Who made whom goes 32 deep at most, the store's own
bound on how deep a permission can be; no read costs more with the depth.
The agent ends with its maker: its `:ends-with` starts as `B`, and
`make-actor` takes no part that sets it (28 November).

**end-actor.** The agent ends, with every actor that ends with it, in any
depth, and every sign-in and session of each, watched or not. It follows `:ends-with`, and
reads who made an agent for nothing (28 November). A person is not ended by
this call.

**open-session, close-session.** A session is opened under a live sign-in,
with a time `:until` and an optional `:driven-by` (`{:kind :person}` or
`{:kind :model :model text}` where `:model` is at most 256 characters), and
closed once. Its id is made by the runtime at random. `:driven-by` is kept
with the session and can be read through `who-is` after the session closes.
A repeat of `open-session` does not change it. When `:driven-by` is absent
(or nil), it means not said, and the runtime then keeps that session's
reads. This addition is a session's default and Sid's to turn; it rests on
Sid's decision of 12 October, entry 4, in `src-store/README.md` (a model's
reads are kept always, a person's until she turns it off) and on entry 8 of
the runtime's page. The same id under the same sign-in again gives `{:open S}`, the result of
the first, whatever it changes. It moves the session's time to the `:until`
that came, when that is later, and the session is then watched: that is how
the runtime keeps a session that is in use. It does so only while nobody
has closed the session, the session's time has not passed at the repeat's
own `:at`, and its sign-in is live at that `:at`. A session that someone
closed stays closed. One whose time has passed at the repeat's `:at` is not
moved on, and is not closed for that: the same repeat with an `:at` before
the session's `:until`, from a copy whose clock is right, moves it on. Only
`who-is` says how the session stands. (Question 17. On 22 November Sid let
the reference's reading stand, in which a session whose time had passed was
closed for good; his yes of 28 November turns that part.) A session whose
`:until` is at or before the `:at` of its opening is taken, not watched and
not closed. A session's id that is another sign-in's, open or not, is
`:in-use`.

**ask, answer-ask.** A program asks under a code, with the hashes of two
secrets that it made (28 November): `:hash`, of the secret that a yes makes
the sign-in's, and `:asking-hash`, of the secret with which it asks how its
ask stands. Auth keeps the second with the ask in every state, gives it in
`ask-of`, and makes it no sign-in's hash; it does not compare the two. A
person answers once:

- yes, as herself: a sign-in of kind `:program` begins for her, with the
  ask's hash;
- yes, as a new agent: the agent `A` is made under her, with the label, and
  the sign-in begins for it;
- no: the ask is refused for good.

The first answer stands. Once an ask has an answer, every answer that fits
gives what stands, before any refusal, and changes nothing but what its time
thins from what is watched (below): the same answer again, another answer, and one at or
after the ask's `:ends` alike (Sid's call of 22 November). An answer to an ask that still waits is judged: one
whose `:at` is at or after the ask's `:ends` is refused with `:over`, and
then come the refusals of the actor and of the ids, in the contract's order.
Two persons who answer one ask at the same moment: one answer stands, and
both are told which; what of that the runtime shows the other person is the
runtime's.

A yes names the actor that the program becomes: the person herself; a new
agent, which is made by her and ends with her; or a live agent of hers that
exists, which is
any live agent of her root, in any depth. (Question 24: Sid let the
reference's reading stand on 22 November, and it stays his to turn.) The
sign-in that a yes begins has the ask's `:asks` as its label. A no needs a
person who has entered, and judges no other part; its other parts must fit
all the same (Sid's call of 22 November). `ask-of` judges no time:
it says `:waiting` for an ask that nobody answered, and the runtime holds
`:ends` against its clock. A code is bound for good: an ask that is over
keeps its code, so that a program that still asks under an old code never
reads another program's answer.

## What is watched, and what a time that passes does

Auth reads no clock, and three things have a time: a sign-in's secret, a
session, and an ask.

Until 28 November this section was "What takes a thing out of what is
live". A sign-in or a session whose time was at or before the `:at` of a
write under its root was then written as ended, as by `end-sign-in`, and
never taken back; so a copy of the runtime whose clock was far ahead ended,
for good, the sign-ins of every root that it wrote under. One mechanism did
two jobs: it kept what is watched from growing for ever, and it stated that
a thing had ended. The first job is kept. The second is taken away.

- **A time that passes ends nothing.** A sign-in and a session keep their
  `:until`. That a sign-in has ended or a session is closed is written by
  `end-sign-in`, `close-session` and `end-actor` alone, never because a
  time passed, in any write. `who-is` says `:ended?` only of a sign-in that
  someone ended, itself or with its actor, and `:closed?` only of a session
  that someone closed, itself or with its sign-in; it gives the `:until` of
  each, and the runtime holds that against its own clock at each read.
- **A write is judged against its own `:at`, and nothing of the judgment is
  kept.** A write that needs a live sign-in, `replace-secret` and
  `open-session`, is refused `:not-live` when the sign-in's `:until` is at
  or before that write's `:at`. The same write with an `:at` before that
  `:until` is judged afresh and taken: a refusal binds nothing. A repeat of
  `open-session` moves a session on only when the session's time and its
  sign-in's have not passed at the repeat's `:at`.
- **What replaces the taking out: what is watched.** A sign-in whose time
  passes with no `end-sign-in`, and a session that nobody closes because
  its program crashed, would stay in what `watch-root` pushes for ever. So
  what `watch-root` and `under` give is a thinner thing than "what nobody
  ended". A sign-in or a session is watched from a write that gave it a
  time after that write's own `:at`: its begin or its opening, a
  replacement of its secret, a moving on of its time. It leaves what is
  watched at the first write that names its root and whose `:at` its time
  is at or before, or when someone ends it. Each write that names a `:root`
  thins what is watched under that root so, before the write itself is
  judged, and whether the write is then taken, is a repeat, or is refused.
  `invite`, `enter`, `own-lane-made` and `ask` name no root and thin
  nothing. Each thing leaves by its own time: a session stays while its
  sign-in leaves. Leaving is no end, and writes nothing on the thing: it
  bounds what a watcher holds, and that is all it means. A thing that left
  comes back when a later write gives it a time that is after that write's
  own `:at`. "The same state as once" is said of a write's own effect: what
  a repeat thins follows its time.
- **Every end is pushed.** Since leaving what is watched is no end, a copy
  of the runtime can rightly hold a connection for a sign-in that the
  watched value does not hold. When that sign-in is then ended, nothing
  else in the value would change, and nobody would be told. So the value
  carries `:ends`, the number of writes under the root that ended or closed
  something. A copy that holds a thing which the value does not hold reads
  `who-is` at each push of its root.
- **An end reaches what is not watched.** `end-sign-in` closes every
  session of the sign-in, and `end-actor` ends every sign-in and session of
  each actor that it ends, watched or not: `who-is` says `:ended?` and
  `:closed?` of each, no session is opened under such a sign-in, its secret
  is not replaced, its sessions are not moved on, and ending or closing it
  again changes nothing and is not counted. How the module does it is the
  module's (`softland.auth.schema`): it writes the end on the records of
  what is watched, and reads the standing of the rest from the record of
  the sign-in and of its actor. That costs `who-is` one seek more for an
  agent's sign-in than for a person's.
- A sign-in's time is its present secret's `:until` alone. The secret
  before it stays with the sign-in until the next replacing; the runtime
  holds its `t2` against the clock.
- What `watch-root` and `under` give is therefore bounded by what was
  watched at the root's latest write. A root with no new write shows what
  was watched then; the runtime holds each time against its clock when it
  reads.
- **What a clock far ahead does, and what it does not.** A copy of the
  runtime whose clock is far ahead takes, by its own clock, each sign-in
  that it reads to be over, and each write that it sends under a root thins
  what is watched there, to nothing when the clock is far enough ahead. It
  ends nothing. `who-is` goes on saying of each sign-in and session that
  nobody ended it, with its `:until`; the copies whose clocks are right go
  on holding what they hold, and a write of theirs is judged against its
  own `:at`. What is left over: a sign-in that stands is missing from the
  watched value and from `under` until its secret is next replaced, and a
  session until its time is next moved on. A person who lists her sign-ins
  does not see it there, though `end-sign-in` of it is taken as of any
  other. Healing that at the next write would need an index of every
  sign-in and session that nobody ended, which grows for ever; it was
  weighed and is not built. The runtime's clock must still be kept: a wrong
  one refuses what it should take, and writes a wrong label.
- A time's edge: a thing is live while the time that it is held against is
  before its end. A sign-in or a session whose end is at or before the
  `:at` of the event that makes it is taken, and is not watched; an ask so
  made is over from its making. One secret before the present one stands,
  until its `t2`; a later replacing ends it whatever its `t2`.

## What the runtime does, and Auth does not check

- Who may end an agent. `end-actor` checks no caller. The runtime lets an
  actor end an agent when that actor's own ending would end it, in any
  depth: it reads `:ends-with` from `under`, and not who made the agent.
  While the two are the same, that is the maker and those above the maker.

- A program that waits is answered against its secret for asking. The
  runtime holds what a waiting program sends against `:asking-hash` alone,
  and never against `:hash`: the secret of the credential sent in that
  place is a wrong secret, told as an unknown code is. It takes no ask
  whose two hashes are one. The page on which she answers shows and checks
  neither hash.
- No sign-in of a person begins before her own lane is made. That is the
  runtime's order of calls: an enter whose `own-lane-made` did not go
  through ends with no sign-in. Auth refuses nothing for a person whose
  lane is not marked as made.
- Holding a time against a clock. Auth gives each `:until` and judges none
  at a read: the runtime holds a sign-in's and a session's `:until`, and an
  ask's `:ends`, against its own clock at each read. What `watch-root`
  pushes is a signal to it and no judge: a thing that the runtime holds and
  that the value does not hold is read with `who-is`, and is over when
  someone ended it or when its time has passed by this copy's clock.
- How often the runtime calls. `who-is` is made for each request and each
  opening of a connection. `open-session` is made once for a new session,
  and then at most once in half of a session's span for each session that
  is held; the runtime's default span is a day. Auth counts no calls, and
  the plan sizes the module for these numbers.

## What can be erased later

What names a human being is her account, her email, and what her provider
said. All of it must be erasable from everything that Auth keeps, its
events too. What it takes (28 November, later):

- **Auth only ever matches an email; it never reads one. So it is given a
  hash, never the address.** A claim is `{:by :email :hash H}` or
  `{:by :code :hash H}`, and an enter carries `:email-hash`. So an
  invitation holds no address anywhere in Auth, its events too: a person who
  is invited and never comes leaves none. What is left is the hash.
- **What Auth keeps to be read is removed by its writes, at once.** An
  account's `[provider subject]` and its `:says` sit in the account's link,
  and the account's key in her list of accounts; the hash of a claim sits in
  the claim's record and in the link. `unlink` removes the link with its
  `:says`, the link under the claim, and the account from her list;
  `take-back` removes an invitation, and the claim's record goes when
  nothing waits under it and no link stands. An answer that an invite keeps
  under its op names nobody.
- **Erasing her is not one write.** It is `unlink` of each link that
  `accounts-of` gives, and `take-back` of each invitation whose claim
  someone names. No list of the invitations that name a person is kept, and
  none is built (it would bring back a hop to the person's task at each
  invite: Sid's call, listed for him). So an invitation that names her and
  that nobody names lets its account in until it ends; the record of an
  invitation that is over stays until its claim is invited again or it is
  taken back, or an operator's sweep that is not built drops it.
  `:invited-by` is the inviter's id: it is not what names her, and stays
  where it is written. After those writes, no key and no value of the
  claims, the accounts, the invites' answers, the enters' records or her
  list of accounts holds her hash, her subject or her `:says`, and her
  address can be invited as another person; her own record stays, with her
  own lane, who first invited her and when, and holds none of the three.
  No word of the runtime for "erase her" is built: who may say it is not
  decided.
- **From the events, it takes a depot migration.** The events of enters and
  of invites hold the account, its `:says` and the hashes, and a depot is a
  log: an ordinary write removes nothing from it. A module update whose
  function clears the `:says`, the subject and the hashes of the records of
  accounts that were unlinked does, which Rama applies on read at once and
  rewrites in the background, keeping offsets. That is an operator's step
  and is not built. What the module keeps true so that it stays possible: no
  step reads an old event of enters or of invites again after that event is
  done, and an event that a migration cleared or replaced by a tombstone
  breaks nothing.

Left for Sid: the hash is plain, so whoever holds the log can test a guessed
address against it; a hash keyed by a secret that the runtime holds would
close that, at the price of a secret whose loss frees every bound email. And
`[provider subject]` could be given as a hash too, since Auth only matches
it; then `:says` would be the one readable thing. Neither is built.

## What must hold at full life

Judge each of these against many writers, many readers and years of data.

- **No one task that everything passes through.** No write and no read of
  one person goes through a task that every person's goes through. On 20
  November every beginning and end of a session went through one task; that
  is what this module replaces.
- **`who-is` is the read of every request.** It is made for each HTTP
  request and each opening of a connection. It is one round trip to one
  task, and it reads a bounded amount however long the person's history is:
  the sign-in, the session, and for an agent's sign-in the agent.
- **What a runtime watches for one root stays small.** `watch-root` is held
  open for each person who has an open connection at a copy of the runtime.
  Its value is bounded by what is watched under her, not by what has ended
  and not by what nobody ended: an agent that opens a session for each run
  closes thousands a day, and a program that crashes closes none.
- **How many roots can be watched.** Rama bounds the live reads of one table
  on one task (`pstate.proxy.limit`; seen at 1,000 on Rama 1.6.0, with no
  option set: `src-store/OPEN-QUESTIONS-rama.md`, section 4). The watched
  roots of one task, over all copies of the runtime together, must not be
  held to that default. The module sets what it needs at launch: it lets
  20,000 roots be watched on one task, the figure that Red Planet Labs run
  their own TPC-C module with (the same section), and the build measures
  what the option bounds.
- **Nothing is deleted but what names a human being.** An ended sign-in, a
  closed session, an ended actor and an answered ask can still be read: the
  facts of them stay. What is watched and what is read on each request is
  kept apart from that history. A link, an invitation and a claim's record
  are removed by `unlink` and `take-back`, since what names a human being
  must be erasable (28 November, later).
- **Scale.** Persons: millions. Actors under one person: from one to
  thousands. Live sign-ins under one person: tens; ended ones: without
  bound. Open sessions under one person: tens to hundreds; closed ones:
  without bound. Asks: short-lived, without bound over time. Accounts of
  one person: few, each added by an invitation that she makes herself;
  nothing in Auth bounds their number, and `accounts-of` reads them all,
  with one seek and a scan. A claim's record is one entry, with at most one
  invitation and the few links of its person. Each invite keeps one answer
  under its op, as each refused enter keeps one record; each is read only
  by its own write, when that write runs again, and both can be swept. Nothing of an
  invitation or a link is watched.
- **Order.** Events of one person are decided in one order. An end is never
  taken back: no later event makes an ended sign-in, a closed session or an
  ended actor live again. That is said of an end that someone wrote, and
  there is no other: a time that passed is no end.
- **Who appends.** Rama asks no name. Only programs on machines that Sid
  runs may reach Rama's ports (`src-store/README.md`, 11 and 12 October,
  entry 4), and the runtime is the one program that is meant to append here.

## Not in this build

A word of the runtime for erasing a person, and a list of the invitations
that name a person. A sweep of the invitations that are over and of the
answers that invites keep. The migration of the depots that clears what an
unlinked account's events hold. A block on a person. Counts for pacing. A person's name, which is a
fact in the store. Permissions, which are the store's.

## Where it goes

- Source: `src-auth/softland/auth/`, with the module in `module.clj` and the
  client in `client.clj`. Checks: `test-auth/softland/auth/`.
- The explanations follow the store's rules (`src-store/CLAUDE.md`): a
  README for each folder, a docstring for each namespace and each function,
  and checks that say what they check in one sentence each, in the form of
  `test-server/softland/server/proof.clj`.
- The artifacts of the skill's phases stay in this folder.
