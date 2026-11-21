# Questions of Auth's spec

[Parent: Auth checks](README.md) · [The spec](../../../src/proposal/runtime-2026-11-21/spine/auth/SPEC.md)

The places where the spec does not say what happens, or says two things, as the
reference (`reference.clj`) met them, against the spec as of commit `37bc0e01`.
For each: the sentences, the readings, and the reading that the reference takes.
That is the reading that refuses more or changes less, unless it is said why
not. None is settled here: each is Sid's to turn, and a turn changes
`reference.clj` and the check that cites the question.

## Forms

**1. A part that is left out, and a part that the contract does not name.**
"A write or a read whose input does not fit gives `{:could-not :read-the-event}`,
and a write then writes nothing." The contract writes each map whole. Some of
its parts are written `text-or-nil`, `H-or-nil`, `S-or-nil` or
`{:label text}-or-nil`. Of `:driven-by` alone it says "optional".

- (a) Every part that the contract names must be there, a part written
  `-or-nil` as nil when it says nothing; a part that the contract does not
  name does not fit.
- (b) A part left out reads as nil, and a part more is dropped.

The reference takes (a), with `:driven-by` free to be left out. So `who-is`
needs `:session nil` when no session is asked for, an `enter` needs
`:email nil` and `:code-hash nil`, and an `answer-ask` needs `:new-agent nil`.

**2. What the runtime keeps, and Auth does not check.** Under the heading "What
the runtime does, and Auth does not check": "A provider's name is at most 256
characters. `:own-layer` is a UUID." Under "Rules for every call", an input
that "does not fit gives `{:could-not :read-the-event}`". That rule's own list of
forms says "A provider is a keyword", and gives `:own-layer` no form. The same
section also holds rules that Auth does keep, such as the order of refusals.

- (a) Auth keeps them too, as form: an input that breaks them does not fit.
- (b) The runtime keeps them, and Auth does not check them.

The reference takes (b), though (a) refuses more. The grounds are the heading,
and the brief, which asks the reference to hold only the order of refusals from
that section. A provider must still be a keyword, and `:own-layer` must be there
(1).

**3. An email that is not in lower case.** "Two emails are the same when their
texts are the same. The runtime gives every email in lower case."

- (a) An email with a capital letter does not fit.
- (b) It fits and is compared as given, so it never proves an invitation that
  was made in lower case.

The reference takes (a), for a claim's email and an enter's alike. Lower case is
Java's `toLowerCase` in the root locale. The form is read before anything else,
so an enter of an account that has a link, with such an email, is not let in,
though "an account that has a link gives its person, whatever claim came with
it".

**4. The two ids of `:making`.** "`:making` holds its UUID and the id that it
names as its session ... that session is no session of Auth's." And "Ids of
persons, actors, sign-ins and sessions are UUIDs."

- (a) Both are UUIDs.
- (b) The session's id is the store's, of any form, since that session is no
  session of Auth's.

The reference takes (a).

**5. A call whose client is no client.** "No call throws for what a caller
gave", and "a call that could not be made gives `{:could-not <word> ...}`". The
contract's words are `:read-the-event`, `:reach-the-module`, `:closed` and
`:find-the-module`, and none is for this.

The reference gives `{:could-not :read-the-event}`, and `close!` of a thing that
is no client gives nil.

## invite, enter, own-layer-made

**6. What an enter that is run again says of the own layer.** Three sentences
bear on it:

- "An enter that is run again after a failure gives what it gave."
- "Until that call every enter says `:own-layer-made? false`, and the copy that
  made that enter sends the act again."
- Of every repeat: "The second call gives the result of the first, which says
  that the write was taken; it does not say how the thing stands now, which a
  read says."

The readings:

- (a) `:own-layer-made?` says how it stands at each enter: true once
  `own-layer-made` went through, also for a copy of the first enter.
- (b) A copy of an enter that the link decided gives what that enter gave, flag
  and all. An enter that is no copy is decided anew, and says how the flag
  stands.

The reference takes (b), which holds all three sentences, and neither reading
refuses more. A copy that is run again after `own-layer-made` says false, so
its runtime sends the act once more. That is the same act, with the same digest,
since it is built from nothing but what the enter gave; and a repeat of
`own-layer-made` changes nothing.

**7. The `:says` of an enter that comes again late, and what makes a copy.**
"`:says` is kept with the link as what the provider said at the enter that was
decided last, with its `:at`." And "A write that arrives twice ... leaves the
same state as once." The two meet when an enter arrives a second time after
another enter of the same account.

- (a) Every enter of an account that has a link is decided, and its `:says`
  and `:at` are kept: the late copy puts back its own, and the state is not as
  once.
- (b) A copy of an enter that the link decided is a repeat, no new decision,
  and changes nothing. "Decided last" is said of enters that are no copies.
- (c) The `:says` of an enter is kept only when its `:at` is later than the
  kept one's: a rule that the spec does not say.

The reference takes (b), which changes less and holds both sentences. A copy is
the same input whole, its `:at` among the rest. An enter that is run again with
a fresh `:at` is therefore a new decision, and the spec does not say whether a
run again keeps its `:at`. The reading costs the link a memory of each enter
that it decided, with what it gave (`:decided`). The module would need the
same memory, or its events. No read of the contract gives `:says`.

## Sign-ins

**8. A repeat of begin-sign-in after the sign-in's secret was replaced.** "Two
writes are the same write when the parts that say what is bound are the same:
... for `begin-sign-in`, the root, the actor, the kind and the hash." "A
sign-in id is bound once: the same id with the same parts again changes
nothing; with other parts it is refused."

- (a) The hash is the one with which the sign-in began: the repeat gives
  `{:begun G}` and changes nothing.
- (b) The hash is the sign-in's hash now: after a replacement, the same begin
  is refused with `:in-use`.

(b) refuses more. The reference takes (a), because (b) breaks the rule that
"the second call gives the result of the first", which the generated histories
hold. "Bound once" reads as the binding that the first begin made.

**9. A sign-in or a session whose end is at or before the event that makes it.**
"A sign-in, a session or an ask whose end is at or before the `:at` of the
event that makes it is taken and is never live." It does not say what the
reads then give.

- (a) It is taken out at once, as by its own event: `who-is` says the sign-in
  has ended, or the session is closed.
- (b) It is kept as not ended, and is only left out of what is live.

The reference takes (a).

**10. A replacement whose new end is at or before its own `:at`.** These bear on
it:

- "No session changes." (replace-secret)
- "It outlives the secret: replacing a sign-in's secret changes no session."
  (session)
- "A sign-in's time is its present secret's `:until` alone."
- The take-out comes "before the write itself is judged".
- "What `watch-root` and `under` give is therefore bounded by what was live at
  the root's latest event."

The readings:

- (a) It is taken, and its own event takes the sign-in out at once: ended, as
  by `end-sign-in`, and so with its sessions.
- (b) It is taken, nothing more changes, and the sign-in stays in what is live
  until the next write under its root takes it out.
- (c) It is refused. No word of the contract says why: the sign-in is live,
  and its secret is right.

The reference takes (a), and that is neither the reading that changes less
(b) nor the one that refuses more (c). (b) leaves what is live holding a sign-in
that was not live at its root's latest event, against the "bounded" sentence
and against the rule of the generated histories that what is live holds nothing
whose time is at or before its root's latest write. (c) would give a word that
says something false. Under (a), "no session changes" holds of the
replacement: the sessions close because their sign-in's time passed, as by
`end-sign-in`, and not because its secret was replaced. The checks of both
sentences say so.

**11. A replacement that arrives after a newer one, when its sign-in is no longer
live.** "A replacement that arrives after a newer one is no repeat, and is
refused with `:wrong-secret`." And the refusals come "in the order in which the
contract lists them", `:not-live` before `:wrong-secret`.

The reference keeps the order: `:not-live` when the sign-in is not live,
`:wrong-secret` when it is.

**12. A hash that comes back.** "`H` is the sign-in's hash now; or it is the same
replacement again, which is so when `H'` is the sign-in's hash now and `H` is
the one before it. A replacement that arrives after a newer one is no repeat,
and is refused with `:wrong-secret`." Two cases show where these part:

- H1 to H2, then H2 to H1, then the first again. H1 is the hash now, which
  the first sentence takes; but the replacement arrives after a newer one,
  which the second refuses.
- A replacement whose old hash and new hash are one.

The reference refuses the first case with `:wrong-secret` (or `:not-live`, by
the order of 11), which refuses more and changes less. A sign-in keeps the pairs
of hashes of the replacements that it took (`:replaced`). The reference takes
the second case, as the first sentence says, since no sentence refuses it.

**13. A replacement's `:previous-until` later than its `:until`.** "The old secret
keeps standing until `t2` and no longer." "A sign-in's time is its present
secret's `:until` alone." Nothing says whether `t2` may be later than the new
`:until`.

The reference keeps `t2` as given. The sign-in ends at its own time all the
same, and the old secret with it. To refuse it would need a word that the
contract does not have.

**14. Two sign-ins of one root with one hash.** Nothing is said. The reference
takes both: the runtime makes hashes of random secrets.

## Actors and sessions

**15. The depth of `:too-deep`.** "`{:refused :too-deep}` B is 32 makers below its
root." "Who made whom goes 32 deep at most."

- (a) The person is 0 below her root, an agent that she made is 1 below, and a
  maker 32 below is refused: the deepest agent is 32 below.
- (b) The person is the first of the 32: the deepest agent is 31 below.

The reference takes (a).

**16. end-actor of an agent that has ended already.** "Ending it again changes
nothing" is said of `end-sign-in` alone. An agent that ended with an actor above
it was never the subject of its own `end-actor`, so a call to end it is no
repeat.

The reference gives `{:ended A}` and changes nothing. No word of the contract
fits a refusal: the agent is there, and it is no person.

**17. open-session of a session that is closed.** "A session that is closed, or
was taken out because its time passed, is not opened again." And "a repeat is
recognised first".

- (a) The repeat gives `{:open S}`, the first result, and the session stays
  closed.
- (b) It is refused with `:not-live`.

(b) refuses more. The reference takes (a), because (b) breaks the rule that
"the second call gives the result of the first", and because `:not-live` is
said of the sign-in, which may still be live. What follows for the runtime: one
that keeps a session in use by calling `open-session` again reads `{:open S}`
for a session that has closed. Only `who-is` tells it so.

**18. `under` and `live-under` of a root that is no person.** Nothing is said. The
reference gives no agents, no sign-ins and no sessions.

## Asks

**19. An ask that is over when it is made, and an answer from a slower clock.** "A
sign-in, a session or an ask whose end is at or before the `:at` of the event
that makes it is taken and is never live." And "An answer whose `:at` is at or
after the ask's `:ends` is refused with `:over`." An answer whose `:at` is
before `:ends`, from a copy whose clock is behind, would be taken by the second
sentence. The first says that the ask is never live.

The reference refuses every answer to such an ask with `:over`. `ask-of` says
`:waiting`, since it judges no time.

**20. Where "another answer gives what stands" falls, and what is "the same
answer".** "An answer whose `:at` is at or after the ask's `:ends` is refused
with `:over`. The first answer stands: the same answer again gives the same
result, and another answer gives what stands. Two persons who answer one ask at
the same moment: one answer stands, and both are told which." And "a repeat is
recognised first, then the refusals in the order in which the contract lists
them". The spec does not say whether another answer is judged by the refusals,
nor which parts make two answers the same.

- (a) Once an ask is answered, every answer gives what stands, before any
  refusal. An answer after `:ends` is then not `:over` when the ask has an
  answer.
- (b) The same answer is recognised first. Then the refusals come in their
  order, `:over` among them. Then another answer that passes them gives what
  stands, and changes nothing.

The reference takes (b), which refuses more. The same answer is the same yes or
no and the same root; for a yes, also the same actor and sign-in, with a new
agent or with none as the one that stands. Its `:until` and a new agent's label
are dropped. What follows:

- Another answer binds nothing, as a refusal binds nothing. Told what stands
  before `:ends`, it is `:over` when it comes again after `:ends`, so its second
  coming does not give the result of its first. The generated histories count
  an answer as taken only when it is the one that stands.
- "Both are told which" holds only for an answer that passes the refusals.
  When the clocks of the two copies fall on either side of `:ends`, the second
  person is told `:over`, not which answer stands.
- The brief's line "an answer at or after `:ends` is refused with `:over`"
  holds for every answer but the one that stands, given again: a repeat is
  recognised first. The histories check it so.

(a) would be simpler for the runtime: an answered ask is decided, and every
answer learns what stands. But it refuses less.

**21. A no after the ask's end.** "A no needs a person who has entered, and judges
no other part." And "An answer whose `:at` is at or after the ask's `:ends` is
refused with `:over`."

- (a) A no is an answer, and a late one is `:over`.
- (b) A no judges no time either, and is taken.

The reference takes (a), which refuses more.

**22. A no from a root who has not entered.** "A no needs a person who has
entered." The contract's refusals of `answer-ask` have no word for it.

The reference refuses it with `:no-such-actor`, the word for "no actor of this
root".

**23. The other parts of a no.** "Judges no other part", and an input that "does
not fit gives `{:could-not :read-the-event}`".

- (a) The other parts must still fit (an actor's id, a sign-in's id, a time),
  and are not judged.
- (b) They may be anything.

The reference takes (a).

**24. "A live agent of hers".** "A yes names the actor that the program becomes:
the person herself; a new agent, which is made by her; or a live agent of hers
that exists."

- (a) Any live agent of her root, in any depth.
- (b) Only an agent that she made herself.

(b) refuses more. The reference takes (a): the spec calls all of them "the
agents of the root" (`under`), and the sentence keeps "made by her" for the new
agent alone. Sid may read it otherwise.

**25. A yes whose sign-in id is a sign-in already, with the parts that the yes
would bind.** "`{:refused :in-use}` the new agent's id or the sign-in's id is
another's." A sign-in that `begin-sign-in` began with that id, for that actor,
of kind `:program`, with the ask's hash, holds exactly what the yes would bind.
`begin-sign-in` would take the same as a repeat.

The reference refuses it with `:in-use`, which refuses more: the id is a
sign-in already, and no answer made it.

**26. `ask-of`'s `:answer` for a no.** "`:answer {:root P :actor A :sign-in G}-or-nil`".

The reference gives nil for a no and for a waiting ask, since a no begins no
sign-in and names no actor.

## The client

**27. A watching function: what it is, and what it does.** "`f` runs on a thread
of Rama's client and must return at once." "No call throws." "`close!` calls no
`f` again." The stop function "stops the watching".

The reference reads these as follows:

- `f` must be a function (`fn?`). A keyword, a map or a var in its place does
  not fit, which refuses more than taking whatever can be called.
- What `f` throws is dropped. The write gives its result, and the watch stays.
- `f` runs on the thread that made the write, before the write returns, in the
  order of the changes.
- A watch is looked up just before each call. So one that an earlier call of
  the same change stopped, or whose client it closed, is not called.
- A write that `f` makes, which "must return at once" forbids, puts its calls
  after the ones still owed. Each watch is handed the changes in their order.

## Where the brief and the spec differ

The brief that asked for this reference says of `:driven-by`: "either
`{:kind :person}` or `{:kind :model :model <text>}` ... Absent means not said."
The spec, as of `37bc0e01`, says "is nil or either ...", "When `:driven-by` is
absent (or nil), it means not said", and gives `who-is` a
`:driven-by D-or-nil`. The reference follows the spec: a nil `:driven-by` fits,
and `who-is` gives `:driven-by nil` when it was not said.
