# Questions of Auth's spec

[Parent: Auth checks](README.md) · [The spec](../../../src/proposal/runtime-2026-11-21/spine/auth/SPEC.md)

The places where the spec does not say what happens, or says two things, as the
reference (`reference.clj`) met them, against the spec as of commit `37bc0e01`.
For each: the sentences, the readings, and the reading that the reference takes.
That is the reading that refuses more or changes less, unless it is said why
not. A turn changes `reference.clj` and the check that cites the question.

**22 November 2026.** Sid's calls of that day, in the prompts that he pasted to
the build of the reference, settled questions 1 to 7, 12, 20 and 23, and let
10, 17 and 24 stand as the reference took them, which stay his to turn. Each
says so under it, and the spec now says each where it belongs. The calls are
two rules: what fits, and that a repeat's result comes from what stands, never
from a memory of calls. The others are his to turn. Question 28 is new: what in
the reference's state grows with the number of calls.

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

**Settled by Sid's call of 22 November: (a).** Every part that the contract
names is there, nil where the contract says `-or-nil`, each in its stated form,
and nothing that the contract does not name. `:driven-by` alone may be left
out. The broken copy `:loose-form` takes (b).

**2. What the runtime keeps, and Auth does not check.** Under the heading "What
the runtime does, and Auth does not check": "A provider's name is at most 256
characters. `:own-layer` is a UUID." Under "Rules for every call", an input
that "does not fit gives `{:could-not :read-the-event}`". That rule's own list of
forms says "A provider is a keyword", and gives `:own-layer` no form. The same
section also holds rules that Auth does keep, such as the order of refusals.

- (a) Auth keeps them too, as form: an input that breaks them does not fit.
- (b) The runtime keeps them, and Auth does not check them.

The reference took (b), though (a) refuses more. The grounds were the heading,
and the brief, which asks the reference to hold only the order of refusals from
that section. A provider must still be a keyword, and `:own-layer` must be there
(1).

**Turned by Sid's call of 22 November, to (a).** A provider's length and
`:own-layer` being a UUID are forms, and Auth checks them: an input that breaks
either does not fit. The spec moved both, with the other sentences that Auth
keeps, out of that heading. A provider's name is counted with its namespace
when it has one, `:g/x` as the three characters `g/x`: the reference's reading
of "name", which the spec does not say. The broken copy
`:provider-and-layer-unchecked` takes (b).

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

**Stands, by Sid's call of 22 November.** The spec now says it as form. The
broken copy `:capital-email-fits` takes (b).

**4. The two ids of `:making`.** "`:making` holds its UUID and the id that it
names as its session ... that session is no session of Auth's." And "Ids of
persons, actors, sign-ins and sessions are UUIDs."

- (a) Both are UUIDs.
- (b) The session's id is the store's, of any form, since that session is no
  session of Auth's.

The reference takes (a). **Stands, by Sid's call of 22 November.**

**5. A call whose client is no client.** "No call throws for what a caller
gave", and "a call that could not be made gives `{:could-not <word> ...}`". The
contract's words are `:read-the-event`, `:reach-the-module`, `:closed` and
`:find-the-module`, and none is for this.

The reference gives `{:could-not :read-the-event}`, and `close!` of a thing that
is no client gives nil. **Stands, by Sid's call of 22 November**, and the spec
now says it.

`open` itself has the contract's form since 22 November: it takes what stands
for the cluster, a thing that `cluster` gives, and a module's name, and a name
that is not there gives `{:could-not :find-the-module}`. Before, it ignored the
name. A thing in the cluster's place that is no cluster, a client among them,
gives `{:could-not :find-the-module}` too: the reference's reading, since the
contract gives `open` that word alone.

**29. A string that UTF-8 cannot carry.** The spec named a text by its
length alone: "a string of at most 256 characters, a character being one
`char` of the JVM". A Java string can hold a surrogate `char` without its
pair, which no UTF-8 text holds; Rama keeps a string as UTF-8 and keeps such
a string changed (the `char` becomes `?`). So the module made one account of
`[:t "\uD800x"]` and `[:t "?x"]`, one claim of two emails that differ there,
and read back a changed label and `:asks`, while the reference kept them apart
(the review of 22 November ran it).

- (a) Such a string does not fit, wherever the contract takes a string: a
  text, a code, a provider's name, a key or a value of `:says`.
- (b) It fits, and the module keeps it unchanged, by encoding every key and
  value that it stores so that UTF-8 carries it.

The reference takes (a), with the module (`softland.auth.fit/carried?`), and
the spec says so in "What fits". **The build's default of 23 November, not
Sid's call: his to turn.** A surrogate pair that stands together fits.

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

The reference took (b), which held all three sentences, and neither reading
refuses more. A copy that was run again after `own-layer-made` said false, so
its runtime sent the act once more.

**Turned by Sid's call of 22 November, to (a).** A repeat's result comes from
what stands, never from a memory of calls. An enter of an account that has a
link gives its person, the fixed own layer and making, and `:own-layer-made?` as
it stands at that enter, a copy of the first enter among them. The spec's rule
for every call names it as the one part of a repeat's result that says how
things stand now. The broken copy `:enter-repeats-what-it-gave` takes (b).

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

The reference took (b), which changes less and holds both sentences. A copy was
the same input whole, its `:at` among the rest. The reading cost the link a
memory of each enter that it decided, with what it gave (`:decided`).

**Turned by Sid's call of 22 November, to (c).** `:says` is kept from the enter
with the latest `:at`, and an enter whose `:at` is not later leaves it. The link
keeps no memory of each enter: at full life that grows with every sign-in. The
reference no longer keeps `:decided`, and "the same state as once" holds of an
enter by (c), since a copy has the same `:at`. No read of the contract gives
`:says`, so a module's `:says` can be checked only by reading its tables. The
broken copy `:says-of-every-enter` takes (a).

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

**Stands as taken, by Sid's call of 22 November, and stays his to turn.** The
spec now says it at replace-secret.

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

The reference refused the first case with `:wrong-secret` (or `:not-live`, by
the order of 11), which refuses more and changes less. A sign-in kept the pairs
of hashes of the replacements that it took (`:replaced`). The reference takes
the second case, as the first sentence says, since no sentence refuses it.

**Turned by Sid's call of 22 November, under his call that a repeat's result
comes from what stands.** A sign-in keeps its present hash and the one before
it, and no history of its replacements: `:replaced` grew with every
replacement, a memory of calls. A replacement is judged by the present hash and
the one before it alone. The first case is taken, since H1 is the hash now; a
replacement that arrives after a newer one is still refused with
`:wrong-secret` whenever its old hash is no longer the hash now, which is every
case but a hash that has come back. The second case is taken, as before. The
spec says it at replace-secret, beside the sentence on a replacement that
arrives after a newer one. The broken copy `:replacements-remembered` takes the
old reading, and the generated histories now and then bring the hash before
the present one back, so that a module which remembers is caught.

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

**Stands as taken, by Sid's call of 22 November, and stays his to turn.** The
spec now says it at open-session.

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
`:waiting`, since it judges no time. Such an ask never has an answer, so
Sid's call on question 20 leaves this as it was: an answer to an ask that waits
is judged as before.

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

The reference took (b), which refuses more. The same answer was the same yes
or no and the same root; for a yes, also the same actor and sign-in, with a new
agent or with none as the one that stands, which the ask kept (`:answered`).
What followed: another answer, told what stands before `:ends`, was `:over`
when it came again after `:ends`; and when the clocks of two copies fell on
either side of `:ends`, the second person was told `:over`, not which answer
stands.

**Turned by Sid's call of 22 November, to (a).** Once an ask has an answer,
every answer that fits gives what stands, before any refusal, and changes
nothing but what its time takes out. An answer to an ask that still waits is
judged as before: `:no-such-ask`, `:over`, then the refusals of the actor and
of the ids. What follows: "both are told which" holds whatever the clocks; an
answer that was told what stands is told it again whenever it comes, so the
generated histories take it as a call that can come again; and the ask keeps
the answer that stands and nothing more, no `:answered`. The broken copy
`:answered-ask-judged-first` takes (b).

**21. A no after the ask's end.** "A no needs a person who has entered, and judges
no other part." And "An answer whose `:at` is at or after the ask's `:ends` is
refused with `:over`."

- (a) A no is an answer, and a late one is `:over`.
- (b) A no judges no time either, and is taken.

The reference takes (a), which refuses more. Since Sid's call on question 20,
this is of an ask that waits: a no to an ask that has an answer is told what
stands.

**22. A no from a root who has not entered.** "A no needs a person who has
entered." The contract's refusals of `answer-ask` have no word for it.

The reference refuses it with `:no-such-actor`, the word for "no actor of this
root". Since Sid's call on question 20, this is of an ask that waits.

**23. The other parts of a no.** "Judges no other part", and an input that "does
not fit gives `{:could-not :read-the-event}`".

- (a) The other parts must still fit (an actor's id, a sign-in's id, a time),
  and are not judged.
- (b) They may be anything.

The reference takes (a). **Stands, by Sid's call of 22 November**, which the
spec now says under "What fits". The broken copy `:no-takes-any-parts` takes
(b).

**24. "A live agent of hers".** "A yes names the actor that the program becomes:
the person herself; a new agent, which is made by her; or a live agent of hers
that exists."

- (a) Any live agent of her root, in any depth.
- (b) Only an agent that she made herself.

(b) refuses more. The reference takes (a): the spec calls all of them "the
agents of the root" (`under`), and the sentence keeps "made by her" for the new
agent alone.

**Stands as taken, by Sid's call of 22 November, and stays his to turn.** The
spec now says it at answer-ask.

**25. A yes whose sign-in id is a sign-in already, with the parts that the yes
would bind.** "`{:refused :in-use}` the new agent's id or the sign-in's id is
another's." A sign-in that `begin-sign-in` began with that id, for that actor,
of kind `:program`, with the ask's hash, holds exactly what the yes would bind.
`begin-sign-in` would take the same as a repeat.

The reference refuses it with `:in-use`, which refuses more: the id is a
sign-in already, and no answer made it. Since Sid's call on question 20, this
is of an ask that waits.

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

## What the state keeps

**28. What grows with the number of calls.** Asked in the prompt that Sid pasted
on 22 November, after his call on question 12: what in the whole state grows
with the number of calls and is not history that the spec asks to keep.

Since the calls, a repeat, a refusal and an input that does not fit add nothing
to the state, and a write that is taken adds only a thing that the spec keeps
for good: a person and her claim, a link, an agent, a sign-in, a session, an
ask. `:agent-order` is kept whole because `under` gives every agent, ended
ones too, in the order in which they were made. Kept for a rule, one each and
not growing: `:first-hash` for each sign-in (8), `:over-when-made?` for each
ask (19), and `:invited-by`, `:invited-at` and `:entered-at` for each person.
Two parts grow otherwise:

- `:sign-in-order` keeps every sign-in that a root ever began, ended ones too.
  An ended sign-in's record is history that the spec keeps; its place in the
  order is read by nothing, since `under`, the one read of the order, gives the
  live ones alone. Because of it, `under` and `live-under` walk every sign-in
  that the root ever began, and `live-under` every session, closed ones too:
  cost, which the reference does not model, and which "What must hold at full
  life" forbids a module ("bounded by what is live under her, not by what has
  ended"). Keeping the order of the live sign-ins alone would bound it, and
  would change no result and no read.
- The atom of a cluster's Auth keeps in `:closed` the id of every client that
  was ever closed. That is no part of Auth's state and no history: it is how
  the reference's clients know that they are closed, which a real client holds
  on itself. It grows with each `close!`.

The reference keeps both as they are, and neither changes a result or a read.
Each is Sid's to turn.

## Where the brief and the spec differ

The brief that asked for this reference says of `:driven-by`: "either
`{:kind :person}` or `{:kind :model :model <text>}` ... Absent means not said."
The spec, as of `37bc0e01`, says "is nil or either ...", "When `:driven-by` is
absent (or nil), it means not said", and gives `who-is` a
`:driven-by D-or-nil`. The reference follows the spec: a nil `:driven-by` fits,
and `who-is` gives `:driven-by nil` when it was not said.
