# Questions of Auth's spec

[Parent: Auth checks](README.md) · [The spec](../../../src/proposal/runtime-2026-11-21/spine/auth/SPEC.md)

The places where the spec does not say what happens, or says two things, as the
reference (`reference.clj`) met them, against the spec as of commit `37bc0e01`.
For each: the sentences, the readings, and the reading that the reference takes.
That is the reading that refuses more or changes less, unless it is said why
not. A turn changes `reference.clj` and the check that cites the question.

**22 November 2026.** A session's defaults of that day, Sid's to turn, settled
questions 1 to 7, 12, 20 and 23, and let 10, 17 and 24 stand as the reference
took them. Until 28 November this page called them "Sid's calls": a session
proposed them and wrote them into prompts in his first person, which he pasted
to the build of the reference, and he typed no line on them (the review of 26
November, run 5, break 2). Each says so under it, and the spec now says each
where it belongs. The defaults are two rules: what fits, and that a repeat's
result comes from what stands, never from a memory of calls. They stay as they
are built. Question 28 is new: what in
the reference's state grows with the number of calls.

**28 November 2026.** Sid's yes to three changes of what Auth stores (the
spec's note of that day) turns questions 9, 10 and 17, each of which says so
under it, and adds 30 to 34, which are a session's readings and his to turn.

**28 November 2026, later.** Sid's yes to three more changes (the spec's
second note of that day: several accounts for a person, an email that is
free again once it is erased or unlinked, an invitation that ends and can be
taken back) turns question 3, and adds 35 to 40, which are a session's
readings and his to turn. The rules that came with them, the steps of the
module and every name are a session's too (the spec says each). A review of
that build added 41 and 42, a session's readings too.

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

**Settled by a session's default of 22 November, Sid's to turn: (a).** Every part that the contract
names is there, nil where the contract says `-or-nil`, each in its stated form,
and nothing that the contract does not name. `:driven-by` alone may be left
out. The broken copy `:loose-form` takes (b).

**2. What the runtime keeps, and Auth does not check.** Under the heading "What
the runtime does, and Auth does not check": "A provider's name is at most 256
characters. `:own-lane` is a UUID." Under "Rules for every call", an input
that "does not fit gives `{:could-not :read-the-event}`". That rule's own list of
forms says "A provider is a keyword", and gives `:own-lane` no form. The same
section also holds rules that Auth does keep, such as the order of refusals.

- (a) Auth keeps them too, as form: an input that breaks them does not fit.
- (b) The runtime keeps them, and Auth does not check them.

The reference took (b), though (a) refuses more. The grounds were the heading,
and the brief, which asks the reference to hold only the order of refusals from
that section. A provider must still be a keyword, and `:own-lane` must be there
(1).

**Turned by a session's default of 22 November, Sid's to turn, to (a).** A provider's length and
`:own-lane` being a UUID are forms, and Auth checks them: an input that breaks
either does not fit. The spec moved both, with the other sentences that Auth
keeps, out of that heading. A provider's name is counted with its namespace
when it has one, `:g/x` as the three characters `g/x`: the reference's reading
of "name", which the spec does not say. The broken copy
`:provider-and-lane-unchecked` takes (b).

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

**Stands, by a session's default of 22 November, Sid's to turn.** The spec now says it as form. The
broken copy `:capital-email-fits` takes (b).

**Turned on 28 November, later, by the design that came with Sid's yes.**
Auth is given no address: a claim is `{:by :email :hash H}` and an enter
carries `:email-hash`, so that an invitation holds no address anywhere in
Auth, its events too. The runtime lower-cases the address and hashes it,
and the question of case is its own. A claim or an enter that holds an
address does not fit. The broken copy `:capital-email-fits` is gone with
the rule it broke.

**4. The two ids of `:making`.** "`:making` holds its UUID and the id that it
names as its session ... that session is no session of Auth's." And "Ids of
persons, actors, sign-ins and sessions are UUIDs."

- (a) Both are UUIDs.
- (b) The session's id is the store's, of any form, since that session is no
  session of Auth's.

The reference takes (a). **Stands, by a session's default of 22 November, Sid's to turn.**

**5. A call whose client is no client.** "No call throws for what a caller
gave", and "a call that could not be made gives `{:could-not <word> ...}`". The
contract's words are `:read-the-event`, `:reach-the-module`, `:closed` and
`:find-the-module`, and none is for this.

The reference gives `{:could-not :read-the-event}`, and `close!` of a thing that
is no client gives nil. **Stands, by a session's default of 22 November, Sid's to turn**, and the spec
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

## invite, enter, own-lane-made

**6. What an enter that is run again says of the own lane.** Three sentences
bear on it:

- "An enter that is run again after a failure gives what it gave."
- "Until that call every enter says `:own-lane-made? false`, and the copy that
  made that enter sends the act again."
- Of every repeat: "The second call gives the result of the first, which says
  that the write was taken; it does not say how the thing stands now, which a
  read says."

The readings:

- (a) `:own-lane-made?` says how it stands at each enter: true once
  `own-lane-made` went through, also for a copy of the first enter.
- (b) A copy of an enter that the link decided gives what that enter gave, flag
  and all. An enter that is no copy is decided anew, and says how the flag
  stands.

The reference took (b), which held all three sentences, and neither reading
refuses more. A copy that was run again after `own-lane-made` said false, so
its runtime sent the act once more.

**Turned by a session's default of 22 November, Sid's to turn, to (a).** A repeat's result comes from
what stands, never from a memory of calls. An enter of an account that has a
link gives its person, the fixed own lane and making, and `:own-lane-made?` as
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

**Turned by a session's default of 22 November, Sid's to turn, to (c).** `:says` is kept from the enter
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

The reference took (a).

**Turned on 28 November, to (b),** by Sid's yes that a time which passes ends
nothing: the thing is taken with its time, is not watched, and is not ended
or closed. An ask so made is still over from its making (19): an ask is not
part of that change.

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

**Stood as taken, by a session's default of 22 November, Sid's to turn.** The spec said it at
replace-secret.

**Turned on 28 November,** by Sid's yes that a time which passes ends
nothing. The replacement is taken; the sign-in has that `:until`, is not
watched, and is not ended; its sessions are not closed, and the standing of
each follows the sign-in's at each read. It differs from (b) in one thing:
the sign-in leaves what is watched at once, since its time is at or before
the `:at` of its root's latest write, so the "bounded" sentence and the rule
of the generated histories still hold. A later replacement whose `:at` is
before that `:until`, from a copy whose clock is behind, is taken. "No
session changes" now holds of every replacement.

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

**Turned by a session's default of 22 November, Sid's to turn, under its default that a repeat's result comes from what stands.** A sign-in keeps its present hash and the one before
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

**Stood as taken, by a session's default of 22 November, Sid's to turn.** The spec said it at
open-session.

**Turned in part on 28 November.** (a) stands for a session that someone
closed: the repeat gives `{:open S}` and the session stays closed. A session
whose time passed is no longer closed for that, so the question's second
half has a new reading: the repeat gives `{:open S}`; it does not move the
session on when the session's time, or its sign-in's, has passed at the
repeat's own `:at`, and it closes nothing; the same repeat with an `:at`
before both times moves the session on, and the session is then watched
again. What followed for the runtime still follows: only `who-is` tells it
how the session stands.

**18. `under` and `live-under` of a root that is no person.** Nothing is said. The
reference gives no agents, no sign-ins and no sessions.

**30. Who made an agent, and whose ending ends it.** Until 28 November one
part, `:made-by`, said who made an agent, whose ending ends it and, in the
runtime, who may end it. Sid's yes of 28 November stores the first two apart:
`:made-by`, and `:ends-with`, which starts as the maker. `end-actor` follows
`:ends-with` and reads `:made-by` for nothing, "an actor above" in the standing
of a sign-in is the one whose ending ends it, and `under` gives both.

What is not decided, and is Sid's: who may point an agent at another actor.
The reference takes no part that sets `:ends-with`, in `make-actor` or in any
other write, so no history holds an agent whose two parts differ; the check of
`end-actor` that tells them apart makes its state by hand. The bound of 32
stays on who made whom (15). Nothing in the reference takes the two to be
equal.

**32. What an end does to what is not watched.** Since 28 November a sign-in
or a session that nobody ended can be out of what is watched. "The sign-in
ends, and every session under it is closed." "The agent ends ... and every
sign-in and session of each." Neither says whether that holds of a thing
that is not watched.

- (a) It holds of every one: `who-is` says `:ended?` of each sign-in of an
  ended actor and `:closed?` of each session of an ended sign-in; a write
  that needs such a sign-in live is `:not-live`; ending or closing it again
  changes nothing.
- (b) It holds of the watched ones, and the others stay as they were.

The reference takes (a): under (b) an end would be taken back by a clock.
What it costs a module that finds the things of an actor in what is watched
is its own; the spec says what this build's module does.

**33. What `:ends` counts.** "A count of the ends written under the root."

- (a) The writes that ended or closed something: an `end-actor` that ends
  three agents, five sign-ins and nine sessions counts one.
- (b) The things that ended.

The reference takes (a). The count is there so that every end is a call of a
watch's function, and one write is one call. A repeat of an end, and an end
of a thing that has ended with one above it, count nothing.

**34. Whether a session leaves what is watched with its sign-in.** Until 28
November a sign-in whose time passed took its sessions out with it, whatever
their own time.

- (a) Each thing leaves by its own time: a session stays watched while its
  sign-in is not.
- (b) A session leaves with its sign-in, and is watched only while its
  sign-in is.

The reference takes (a), which changes less at a write: what is watched says
nothing of standing any more, so nothing rests on the two agreeing, and a
watcher that holds a thing which the value does not hold reads `who-is`
either way.

## Asks

**19. An ask that is over when it is made, and an answer from a slower clock.** "A
sign-in, a session or an ask whose end is at or before the `:at` of the event
that makes it is taken and is never live." And "An answer whose `:at` is at or
after the ask's `:ends` is refused with `:over`." An answer whose `:at` is
before `:ends`, from a copy whose clock is behind, would be taken by the second
sentence. The first says that the ask is never live.

The reference refuses every answer to such an ask with `:over`. `ask-of` says
`:waiting`, since it judges no time. Such an ask never has an answer, so
the default of 22 November on question 20 leaves this as it was: an answer to an ask that waits
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

**Turned by a session's default of 22 November, Sid's to turn, to (a).** Once an ask has an answer,
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

The reference takes (a), which refuses more. Since the default of 22 November on question 20,
this is of an ask that waits: a no to an ask that has an answer is told what
stands.

**22. A no from a root who has not entered.** "A no needs a person who has
entered." The contract's refusals of `answer-ask` have no word for it.

The reference refuses it with `:no-such-actor`, the word for "no actor of this
root". Since the default of 22 November on question 20, this is of an ask that waits.

**23. The other parts of a no.** "Judges no other part", and an input that "does
not fit gives `{:could-not :read-the-event}`".

- (a) The other parts must still fit (an actor's id, a sign-in's id, a time),
  and are not judged.
- (b) They may be anything.

The reference takes (a). **Stands, by a session's default of 22 November, Sid's to turn**, which the
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

**Stands as taken, by a session's default of 22 November, Sid's to turn.** The
spec now says it at answer-ask.

Since Sid's yes of 28 November (F15: "only you at a browser are you"), the
first of the three is gone: a yes that names the person herself is refused
with `:is-a-person`, and so is `begin-sign-in` of kind `:program` for her. A
yes names a new agent or a live agent of hers, and (a) and (b) are still the
reading of the second. The broken copies `:yes-names-the-person` and
`:person-begins-a-program` take each refusal away.

**25. A yes whose sign-in id is a sign-in already, with the parts that the yes
would bind.** "`{:refused :in-use}` the new agent's id or the sign-in's id is
another's." A sign-in that `begin-sign-in` began with that id, for that actor,
of kind `:program`, with the ask's hash, holds exactly what the yes would bind.
`begin-sign-in` would take the same as a repeat.

The reference refuses it with `:in-use`, which refuses more: the id is a
sign-in already, and no answer made it. Since the default of 22 November on question 20, this
is of an ask that waits.

**26. `ask-of`'s `:answer` for a no.** "`:answer {:root P :actor A :sign-in G}-or-nil`".

The reference gives nil for a no and for a waiting ask, since a no begins no
sign-in and names no actor.

**31. Two asks that are the same write.** Until 28 November: "for `ask`, the
hash". An ask now holds two hashes, `:hash` and `:asking-hash`.

- (a) The same write when both hashes are the same; the same `:hash` with
  another `:asking-hash`, under a code that is bound, is `:in-use`.
- (b) The same write when `:hash` is the same; the other hash of a repeat is
  dropped, as its `:asks` and `:ends` are.

The reference takes (a), which refuses more: both hashes say what is bound,
and under (b) a second program that knew the first hash could ask under the
code with a secret for asking of its own and be told `{:asked U}` while its
polls are refused. The broken copy `:asking-hash-not-bound` takes (b). Auth
does not compare the two hashes of one ask with each other: that they differ
is the runtime's to hold. A session's choice of 28 November, Sid's to turn.

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

## Invitations and accounts

**35. An invite that runs again, and a result that comes from a memory.**
a session's default of 22 November, Sid's to turn: a repeat's result "comes from what stands, never
from a memory of calls". The spec of 28 November, later: an invite carries
an op-id, "an invite whose op has a record changes nothing and gives the
answer it gave", so that an invite that the module runs again, after its
invitation was taken back or after the link that it let in was removed,
cannot bring it back.

- (a) The op's answer is kept, and an invite of that op gives it again,
  whatever stands: a memory of one call, for one write.
- (b) No answer is kept, and an invite that runs again is judged afresh.

The reference takes (a), with the spec, and keeps the answer under the op
alone: an op given again with another person or another claim gives the
answer of the first (the module keeps it at the claim's task, so there an
op given again under another claim of another task is judged afresh). The
spec says that such an op is not defined: the runtime makes an op fresh, at
random, for each word that reaches it, and the setup for each run, so
neither happens. The answer is a retry's memory and nothing else: no rule
leans on it (that nobody gives a person who has entered another account is
41's, held where the claim's links are). It is the one
place where a result comes from a memory of a call, and it grows by one
answer for each invite (28). **A session's reading, for Sid to see beside
his call of 22 November.** The broken copy `:invite-op-forgotten` takes
(b).

**36. An invitation whose end is at or before its own time.** "One whose
`:ends` is at or before its own `:at` is taken and never waits."

The reference takes it, and writes it: it lets nobody in, names nobody, and
takes the place of an invitation of the same claim that was over. An invite
of the same person while it stands is no repeat, since it is over: it
writes the new one.

**37. The order of a person's accounts.** "In the order in which they were
linked." Two accounts can be linked at one `:at`, from two copies of the
runtime.

The reference orders them by the `:at` of the enter that linked each, then
by the provider's name, its namespace counted, then by the subject
(`account-order`), which the module can give without a counter. An `:at`
from a clock that went back puts a later link before an earlier one.

**38. The person of a take-back.** "`take-back {:person P :claim C :invite I
:at t}` gives `{:taken-back P}` always."

The reference takes the invitation back by its op alone and gives the `P`
that it was given, without holding it against the invitation's person: the
op names one invitation, and who may take it back is the runtime's.

**39. An unlink of a link that never stood.** "`{:unlinked P}` whenever,
after it, link `L` does not stand on the account."

The reference gives `{:unlinked P}` for a link that never stood, for an
account that has no link, and for a link of another person that stands no
more, and changes nothing. Only a link that stands for another person is
refused. `:linking` is never given by the reference, in which an enter is
one step.

**40. An invitation that is over, as read.** "`invitation-of` ... says
nothing of "over": the caller holds `:ends` against its own time."

The reference gives an invitation that is over as it gives one that waits,
with its `:ends`, until it is taken back or the claim is invited again. Its
person is the claim's `:person` though the claim names nobody.

**41. An invitation beside her links.** The spec of 28 November, later, wrote
an invitation of `P` "beside her links if she has some", whoever made it.
Then a person who has entered could be given another account by whoever
invited her, or by a setup run again, until the invitation's end, and
nobody could close it.

- (a) Beside her links, only her own invite is written: one whose
  `:invited-by` is `P`. Another's gives `{:invited P}` and changes nothing.
- (b) Any invite is written beside her links, and who may make one is the
  runtime's alone.

The reference takes (a), and so does the spec: the rule sits where the fact
is, the claim's links, and leans on no memory of answers. "Her own" is read
from what the event holds: the runtime's `:invited-by` for her own word is
her person id. After she unlinked every account, an invite of her by anyone
is written again, which is the owner's way back in through the setup.
**A session's reading, for Sid to see**, with the question it leaves:
whether the first inviter may bring back a person who unlinked everything.
The broken copy `:anyone-invites-beside-links` takes (b).

**42. An enter that runs again after its account was unlinked.** The spec
said "no step reads an old event again", and that an enter that runs again
after its account was unlinked cannot prove an invitation made after it.
Rama runs a record again from the log after a failure, so an enter whose
account was linked and then unlinked can run again with nothing of its link
left. A entered as P by a code; her email's claim holds an invitation of Q
made before that enter; P unlinks A; the enter, run again, finds no record,
proves Q's invitation and links A to Q with its `:says`, and nobody signed
in.

- (a) It is judged as a new enter at its old `:at`: only an invitation made
  at or before it, that still waits, lets it in. What it does is what A's
  next sign-in would do.
- (b) It is refused. That takes a record for every sign-in, a mark that
  stays under the account's key after an unlink, or Auth's writes on a
  topology whose updates are exactly-once, with results read by op.

The reference takes (a), as the module does, and so does the spec, which
says it as the rule that holds and as not closed: (b) is each of three
things that are Sid's. A failed mark under the op of the enter that linked
closes one of the two cases and is not built, for the reason that a memory
of what an enter gave was not (a session's default of 22 November, Sid's to turn). **A session's
reading, for Sid to see.**

## What the state keeps

**28. What grows with the number of calls.** Asked in the prompt that Sid pasted
on 22 November, after his call on question 12: what in the whole state grows
with the number of calls and is not history that the spec asks to keep.

Since the calls, a repeat, a refusal and an input that does not fit add nothing
to the state, and a write that is taken adds only a thing that the spec keeps
for good: a person, a link, an agent, a sign-in, a session, an ask; and
since 28 November, later, a claim's record, which goes when nothing waits
under it and no link stands, and the answer of each invite's op (35), which
stays. `:agent-order` is kept whole because `under` gives every agent, ended
ones too, in the order in which they were made. Kept for a rule, one each and
not growing: `:first-hash` for each sign-in (8), `:over-when-made?` for each
ask (19), and `:invited-by`, `:invited-at` and `:entered-at` for each person;
and `:next-link`, one number, from which the reference makes a link's id.
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
