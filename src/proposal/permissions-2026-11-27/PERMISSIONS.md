# Permissions in the store: what was built, what was found, what Sid answered, what is open

As of 27 November 2026. Written by a Claude Code session with Sid (session
"permissions review"), to be handed to other sessions. It assumes nothing of
any conversation. The record's times are UTC, where this session ran on
26 November.

Second pass, 27 November 2026 (26 November from 20:13 UTC in the record).
A review of this page by another session was held against it in a working
session with Sid (the "permissions working session"). What that added: a
line in sections 1, 9 and 11; section 6, items 18 to 21 (decided); a note
at the head of section 7; marks on (a) and (b) in section 8; and section 12
(the review, the order, the first slice). Everything else is as the first
session wrote it, and that text is the commit before this one.

Four kinds of statement are kept apart:

- IN THE CODE: what the page of 26 November reported, from read-only
  gatherers that day. The session that wrote this page read no code.
- SID'S: words he typed, word for word, with the id of his line in the
  record (`memtool line <id>`). "Long prompt" means the record cannot tell
  typed from pasted.
- DECIDED: a question put to Sid and his explicit reply to it. By his rule
  (section 6, item 15) a page is approved only where he answered explicitly.
- A SESSION'S: a reading or a proposal that he has not answered. It is his
  to turn. It is not a decision of his.

Nothing was run for this page. Store paths are under
`src-store/softland/store/`.

## 1. In short

- The permissions part of the store was built on 21 and 22 November 2026.
  It works and its proofs pass (last run 25 November).
- It has four bugs. In four places a session's reading was built that is
  not what Sid meant. Two sessions proposed a fix of the stored record;
  nothing of it is built.
- This session held the work against the root page's rule "Defaults, not
  conflations" (section 2). Its finding: almost every rule of the built
  permissions was never chosen. It follows from one fact: a permission is
  written out whole, and that writing is at once its id, its place in the
  store and its history (section 5).
- Sid then answered eleven questions (section 6). His answers replace the
  built form with: two atoms (see and write), composition over acts, one
  record per give, authority that is lent to agents and runs, an owner who
  is a right and not the maker, several changes in one act, and a run as a
  party of its own.
- Eight statements of a session had no reply of his (section 8). Since
  27 November (a) and (b) are decided. Three questions can wait
  (section 9).
- No real data is kept yet. The fixes come before pieces 5 to 8 (Sid,
  section 6, item 17).
- Second pass, 27 November: a review by another session listed 24 misses.
  Sid answered four more questions (section 6, items 18 to 21). A session
  settled sixteen points for the first slice, and eleven questions put to
  him are open, with part of a twelfth (section 12). By his answer of the
  same night the fixes of the review of 26 November come first, and this
  design after, as its own build (section 12, "The order").

## 2. What it is held against

The root page of the repository (`CLAUDE.md`) holds the rule:

> Defaults, not conflations: when starting with a simple first slice, keep
> independent jobs apart in IDs, keys and stored shapes, and let the simple
> day-1 behaviour be a default value rather than a fused structure. Concerns
> that merely coincide in the first use case (write ordering vs. read
> grouping, provenance vs. cascade, identity vs. mutable state) should not
> share one identity or storage slot, and a floor mechanism should be named
> only for what it actually enforces — not for the richer product concept
> built on top of it. Keep rules and features lean: only split jobs with
> genuinely different lifecycles or cardinalities, and wire only the default
> behaviour until more is needed.

Sid's words on the model (SID'S). This session opened the first eight.

- ‹66e7b9cf› 24 Nov: "so question why is having way to do 1. "stamp all
  acts with author" 2. "be able to give read write access to each act"
  3. "acts can grouped together either form the start or at some point of
  time" 4. "anyone with permission for this act's read and write can take
  action" (what else?) not enough for arbitrary composition because to do 3
  we would make another act which can say the properties of a group, and we
  can add properties to mark any X no. of acts with any property that binds
  it to a any N number of groups. What other properties the system should
  provide (I might already be missing what it already provides and what
  should provide) this is a system design and architecture problem right?
  maybe there are also limits that i have not include as tradeoffs or
  constraints"
- ‹80b8d52d› 24 Nov: "to me this all work seems like the granularity is
  already decided and its set to either layer or 1-act but why? this is like
  breaking the most significant rule of softland which is being provider of
  spectrum [...] getting to the read side its more brutal like only read the
  whole layer or make a new one what? why?" and "a single act is a single
  act, a layer is a named group in wich there can be X no. of acts (but in a
  layer we don't talk using the acts because its too granular, its
  composition of smaller layer, the smallest layer is a collection of acts
  type (I have yet to refine my own mental model and terminology for this
  though) now giving read and write access means for both "all layers,acts
  that exist at any arbitrary granularity" "all layer and acts that *can*
  exist in future" we tackle both the cases right"
- ‹9e06f4fe› 25 Nov, long prompt: "we have two different problems that we
  are trying to solve. One is how to order different acts because in Rama we
  need to partition the events according to some kind of key [...] The other
  problem is we need to have some kind of infinite composition of different
  kind of acts that so that we can give them different permissions and use
  them as we like. As a context bundle of some sort." Also: "ordering is for
  providing the throughput, efficiency from the system point of view, and
  grouping is more about a product kind of thing that we are providing on
  top. And it seems to me they are a different problems." Also: "if an act
  is already committed in the name of some layer, we cannot further we
  cannot provide granularity after the fact that it is committed." Also:
  "One heuristic could be it is possible to define infinite compositions,
  but for starters we can put anything on a broader layer that is already
  defined." Also: "I am mixing how do we query and read that data versus how
  we are writing it down."
- ‹68b2b592› 25 Nov: "the deciding factor is what does an act need to decide
  while it passes at "one present moment" through the system."
- ‹e36008e1› 23 Sep: "we can combine these to make a bigger one but can't
  decompose it to atomic units later on becuaes the additional data that is
  attached is not there"
- ‹23f4a742› 24 Nov: "why do we need to answer and discuss this at this
  stage? we have the stamps for each act right?"
- ‹6db69c3b› 24 Nov: "why is it being presented as a binary decision? we are
  a provider of spectrum" and "so how can millions of people watch
  something"
- ‹d5172c3f› 25 Nov: "yes lane is the name". The lane is the store's unit
  of order. "Layer" stays his word for a group of acts of any size.
- From this session: ‹5e1eac98›: "my headspace is at the atomic units,
  composition, spectrum, defaults". ‹68b918be›: "the system should be fool
  proof, it should be provable that anything can't be automatically allowed
  to anyone".
- As the page of 26 November quotes them, not opened by this session:
  ‹45c7127b› 21 Nov, long prompt: "each act is the smallest atomic unit
  right and we can have permission over that"; "permissions is also like
  someone is asking for read write access and that has the spectrum of
  everytime to always allow and that can be per run of some
  task/query/agent"; "iff the readers have same level of clearence they will
  all see the same thing ... we only need to match the top ones or no???".
  ‹95754ddf› 21 Nov: "I think yes it can have more than one permission
  because bio-agent is not a single thing we can run 10k instances of the
  same thing unloke a human". ‹61d83548› 18 Nov: "later on I want to add
  multiple other users who have root access. How would that work?".
  ‹d05a12e6› 7 Oct: "permisison is like a hierarchy ... it ties back to a
  single individual". ‹56a55527› 2 Oct: "an individual is always a part of a
  groups, a group is part of another group ... world is the open group".
  ‹e2275efa› 13 Oct, on merge conflicts: "this is a product kind of
  decision. That uh, we are providing only a spectrum uh, and what uh, like
  give our basic defaults."

## 3. The problem and its shape (A SESSION'S frame)

Sid, after reading it (‹bb35585c›): "now this reply makes me understand the
problem". His answers in section 6 are built on it.

Softland is a shared notebook that never erases. People write lines in it,
and so do their agents, many at once. Each line is an act. The store has to
answer two questions all day:

1. Someone hands in a new act. Is it taken?
2. Someone reads. Which acts are they shown?

Behind both is a third: who set the rule that answers them, and how is that
rule changed? The standard name is access control.

Four facts make it hard here. They are not choices.

- Taking is once, showing is every time. A taken act stays for good, so
  question 1 is answered once and the answer is kept. Question 2 is answered
  fresh at each read.
- The rules change while people write. A change needs a clear before and
  after against the acts.
- A writer is often not a person. It is an agent acting for a person, in
  many runs at once.
- The thing has no fixed size. It can be one act, a group of acts, or acts
  not written yet.

Every rule is one sentence with blanks: [who] may [do what] to [which
things], [for how long], [on whose say-so]. A sixth question is about the
sentence itself: who may change it. Each blank is a range, and the product
picks one default.

## 4. What is built today (IN THE CODE, as of 26 November)

The form. Every act names one permission, written out whole
(`act.clj:88-100`), nested up to 32 deep (`act.clj:36`):

    [who lane granted-in]                        a lane's first permission
    [who lane granted-in parent]                 a given one; parent is the
                                                 giver's permission, whole
    [... parent allows]                          allows = :see | :see-and-act
    [... parent allows [:run session]]           for one run
    [... parent :see-and-act [:act yes digest]]  a yes: one exact act, once

Where one sits (`gate/chain.clj:39-49`): a lane permission at (lane,
holder); a run permission at (lane, session); a yes at (lane, the yes's own
uuid).

The rules (`gate/decision.clj` unless another file is named):

- One standing permission per holder per lane, and one per run. A second
  give is refused.
- What a permission allows is never changed in place. "See" becomes "see
  and act" by an end and then a give, and the two cannot go in one act.
- An end is a fact with no value at the place. It does not say which
  permission was ended (`record/rows.clj`).
- A permission that was never given, at a place that holds an end, is
  refused as ended.
- Given again, a permission has the same id, because the id is its parts.
  Old and new are told apart by the stamps of their rows (`gate/chain.clj`).
- Ending writes nothing for those beneath. The rows above an act's
  permission are read at each decision. Who gave a permission and what it
  ends with are one slot: the parent.
- Only a lane's first holder writes its settings. Not built: a lane that is
  handed to another holder.
- A holder of `:see` cannot act, give or end, her own neither.
- A run permission or a yes gives nothing.
- No gate rule reads a clock (`gate/stamp.clj`).
- An act's digest holds its permission (`door/digest.clj`). A new form
  changes every digest.

Seeing (`read_path/`). A query carries the asker and the run, and no
permission. A lane is seen by everyone when its setting says so; otherwise
by whoever holds any permission that stands in it. A yes lets nobody see.
So seeing is the whole lane or none of it.

Live views (`read_path/sources.clj`, `read_path/query.clj`). Each live
viewer holds pushes of her own: on her permission's row, on her run's row,
and on each row above. The read path holds itself to 900 pushes on one
table under one routing id, under Rama's default of 1,000. "About 450
viewers" of a members-only lane with one giver between her and the first
holder, and "about 300" with two, are counted and not measured. A fact
written just after her permission ended can still reach her. The rule that
mended live views rests on a property of Rama that no probe ran. The read
path keeps nothing durable.

Above the store, a permission is written out by hand in
`src-runtime/softland/runtime/sign_in.clj` and
`src-command/softland/command/main.clj`, and in the trees of 20 November
(`src-server`, `src-server-browser`, `src-cli`).

Never tested: a yes in a batch that fails and runs again; a yes given
before a restart and used after; a yes on a very large act; anything on
more than one machine; what a view over many small lanes costs.

## 5. What was found wrong

Bugs (the page of 26 November):

1. A holder has one permission per lane. Sid said more than one
   (‹95754ddf›).
2. To raise or lower someone is an end and then a give. She is out in
   between, and the end also ends everything she gave.
3. A permission that never existed is answered "ended". Answers are kept
   for good and can be read.
4. A see-only holder cannot leave a lane.

Not what he meant (the page of 26 November):

5. The sizes are three kinds, each a form of its own: the lane, one run,
   one exact act. He described a range.
6. "One act" was built for approving one write of an agent. His two
   examples were about who may see.
7. Seeing is the whole lane or none (‹80b8d52d›).
8. Every holder, a person too, falls when her giver falls. His answer that
   day was about agents.

This session's finding (A SESSION'S): the rules follow from the form.

- One permission per holder per lane, because its place is (lane, holder).
- Raising or lowering is an end and a give, because what it allows is part
  of its id.
- An end cannot say what it ended, and a permission never given reads as
  ended, because the end sits at the place and a give has no id of its own.
- Everyone beneath falls, because who gave it and what it falls with are
  one slot.
- Only the first holder writes settings, because "owner" is not a right. It
  is the permission that has no parent.
- A see-only holder cannot leave, and a run or a yes cannot give, because
  rights hang on one word and on the kind of form.
- Seeing is the whole lane or none, because a read asks only whether some
  permission stands in the lane.

What each shape locks or costs (A SESSION'S):

| | How the store holds it | What that locks or costs |
|---|---|---|
| a | A permission is written out whole, and that writing is its id. | Nothing in it can change. |
| b | It sits at one place per (lane, holder). | One permission per holder. An end cannot say which permission ended. |
| c | Who gave it and what it falls with are one slot. | A person cannot stay when her inviter leaves. No handing on. |
| d | Lane, run and one act are three separate forms. | The sizes cannot be mixed, and each new size is another build. |
| e | The lane is the unit for four things: the order of acts, what a permission covers, what is seen, whose settings. | What is seen together is fixed when an act is written, for good. |
| f | Every act carries its permission whole, inside its digest. | Changing the form changes every act: cheap today, frozen once real data is kept. |
| g | Each live viewer watches her own row and each row above. | About 450 viewers for a members-only lane. |

The fix two sessions proposed (the page of 26 November): each give is a
record with an id of its own, cited by acts; separate fields for holder,
who gave it, what it ends with (with the list of those above kept on the
row), rights as a set, what it covers, status; only today's behaviour
wired. This session's findings against it: it mends bugs 1 to 4 and does
not say it mends 5 to 7; the chain is still fixed at the give; "ends with"
is weaker than "never exceeds" once someone can be lowered without being
ended; "what it covers" is one field for who, what and how long. Section 7
is what replaces it.

Four things in the built work hold: gives and ends are acts in the same
order as the writes they govern; no gate rule reads a clock; nothing is
erased; seeing is worked out at each read and not stored.

## 6. Decided: each question and Sid's reply

Sid's replies are word for word. ‹5e1eac98› and ‹68b918be› are long prompts
in the record; in the session each arrived as his own message, with no
paste. "Stands as" is a session's one-line reading.

After the first seven he wrote (‹4fede476›): "yeah we on the same page for
all 7 .. so what are the next pending ones?"

**1. Who holds it.**
Asked: "Range: one run, an agent (all its runs), a person, a group of
people, everyone. Decision 1: are these five sizes of one thing, with
"everyone" a holder and not a lane setting? I say yes."
Sid (‹5e1eac98›): "1. there should not be only 1 or somethign on per lane
for permission, there is only 1 who initially made the lane and keeping
that is choice not a softland made fact, there can be as many as entities
(people, group, agent ids etc.) on each lane as that lane wants. if i
understand correctly that is your choice as well."
Stands as: any number of parties on a lane, any number of permissions each.
The maker is a fact; being the only owner is a default the lane can change.
His reply does not mention "everyone" (section 8, a).

**2. Does what.**
First asked: "Decision 2: four separate words on day one, and write
possible without see? I say yes." The four were see, write, give, end.
Sid (‹5e1eac98›): "2. what is the full range? you said this is a problem
studied as access control so i think this should already have a well
defined set of everything right? is it an open problem because if it is
then we would be enumerating and then figuring out what to do."
Asked again as: "the two atoms are see and write, one per moment. The kinds
of act (replace, refer, give, end, mark) are the first level of
composition, each a name and not a new mechanism." The two moments are when
an act arrives and when someone reads.
Sid: the "same page for all 7" line. And on the rule "you can give only
what you hold" (‹e14a5bd4›): "i think so because X with read rights should
not send another request to himself on id Y and grant it write permission."
Stands as: two atoms, see and write. Everything else is composition over
acts. You can give only what you hold.

**3. To which things.**
Asked: "Decision 3: is a lane only the order of its acts, and what a
permission covers a group of acts? I say yes."
Sid (‹5e1eac98›): "3. is this not already answered in the lane talk i had ?
is it not everything that is composable over acts? that is the whole range
and completness composition gives? does it not? [...]"
Stands as: already his from the lane talk (section 2). The range is
everything composable over acts. A lane is the order.

**4. For how long.**
First asked: "Your sentence then reads: every time is one use, per run is
held by the run, always is held by the agent. Decision 4: is that reading
of your sentence right?"
Sid (‹5e1eac98›): "4. maybe not a product question but more like a security
question??? like how certificates get auto renewed or smth? because i think
the user is not thinking like that the mental model i have right now is
heigher level like subscriptions per month ... but see i think this is
dependent on the context we are working in .. so here i referenced the
example of paid subscriptions but this also applies to something like an
agent that is supposed to be active for something or a timeout of some sort
.... hmmm is there even a one answer fits all? what would the defulat even
be why?"
Asked again as: "One mechanism fits all, and no one policy does. The atom
is that an end is an act. Every "how long" is who writes the end, and
when." The writers named: the person taking it back; the gate when the uses
run out; the runtime when the run finishes; a timer; whoever watches
payment. "The default is no limit, until ended."
Sid (‹e14a5bd4›): "4. agreed"
Stands as: an end is an act; "how long" is who writes the end; the default
is no limit.

**5. On whose say-so.**
Asked: two fields, "given by" (history) and "lent from" (optional; when
set, the permission falls with the lender's and can never allow or cover
more than the lender's does at that moment). "Decision 5: (a) may an agent
or a run ever hold authority that is not lent from a person? I say never.
(b) Does a person let in by another stay when that one leaves? I say yes by
default."
Sid (‹5e1eac98›): "5. i think you are right but i am getting nervous that
each act should contain this or you saying its optional ... either way we
are paying the cost of storage and then performance right????"
Answered: none of it is on each act. It is on the give, written once.
"Given by" is the give's author. "Lent from" is one id, only on gives to
agents and runs. An ordinary act carries one id, the give it acts under.
Sid (‹e14a5bd4›): "5. yeah"
Stands as: as asked, both parts.

**6. Who may change it.**
Asked: "Decision 6: is the owner a right apart from the maker, with several
owners each acting alone and nothing left without an owner? I say yes."
Sid (‹5e1eac98›): "6. yes owner is different from maker both can be same
but also one is subset of another."
Stands as: the owner is a right apart from the maker. No reply on "several
owners each acting alone" or "nothing left without an owner" (section 8,
b).

**7. Several changes in one act.**
Sid's question before it (‹bb35585c›, pasted by him): "is there no concept
of inheretence or transfer? or these are not exactly atomic units of
permission because we can acheive transfer using other atomic operations
like revoke, assign new but then who is doing these 2 changes under whose
permission? does it matter if its an atomic unit or we make the transfer as
atomic as well? there is a usecase for it specifically for e.g i makign a
new skill and then selling its copies or instances to other people but each
one points to me or sellign the whole ownership of the skill such that the
new owner holds the rights to sell any more of the copies or not. so there
can be infinite such combos how do we enable them, will our current or the
new fixed system handle such cases?"
Asked: "Decision 7: may one act carry several permission changes that pass
or fail together? I say yes."
Sid (‹5e1eac98›): "7. i was reading the part before this so when i got to
this i thought it should ablso be idempotent that is passed and failed as
one  so yes i think so too."
Stands as: yes. Two properties, both wanted: several changes pass or fail
as one, and the same act sent twice counts once.

**8. What a run is to the store.**
First asked with the range: only an id on each act; a record of its own,
with an act that starts it and an act that ends it; or everything about it
repeated beside each act.
Sid (‹68b918be›): "can you explain more 8 semantically, product pov,
engineering pov"
Asked again as: "Decision 8, restated: is a run a party of its own,
declared by a start act and closed by an end act, with whom it acts for
answered per give? I say yes."
Sid (‹d0f62e81›): "yes to 8 as you say the decision."
Stands as: as restated.

**9. Whose act stands in his lane after he approves an agent's work.**
Asked: "Range: the agent's own act lands in your lane under your once-only
yes; or the agent's act stays in its own lane and an act of yours refers to
it; or both are offered." The session said the second as the default.
Sid (‹68b918be›): "9. i think its should be bidirectional, so we want to
preserve the individual atomics and the realtion between them as well. so
its bidirectional like an agent can point to mine and my act can refer to
it. i tink its also useful to have because even though the agent is
attached to mine it can still be started automatically triggered based on
some outside or inside event and i want it to run like that in a system
that i have created."
Stands as: both are offered. Each act stays its own atom, the relation is
kept both ways, and an agent can be started by an event and run on its own.

**10. Two acts written from the same one.**
First asked with the page's word "place". Sid (‹68b918be›): "10. what
place???"
Asked again as: "Your note says X. An agent starts from X and writes X2.
Meanwhile you changed the note to Y. X2 was made from X, and X is no longer
the newest. What happens?" Proposed: every act that replaces another names
the act it was written from; the store takes both, nothing is refused and
nothing is lost; which one shows as newest is worked out at read, so it is
a default that can change.
Sid (‹d0f62e81›): "10. yeah its a question of permission conflict or
permission leading to conflict these cases will be there and then i think
they should be raiesed and decisions lay on that group or lane or whatever
but we are not conflating in the system and making decision for them ofc we
can have a defult ones."
Stands as: the store takes both and raises the conflict. That group or lane
decides. The system decides nothing for them beyond a default.

**11. Who may see what a run did.**
First asked as "Who may read a run's trail?", the session saying "the
person it acts for".
Sid (‹68b918be›): "11. i think i don't understand why you mean by run's
trail,  its not this easy to say because imagine i have a agent that i can
run, this agent is being used to provide some service to some things and
thereby can be used in some other person's tool, the tool is owned by some
one else but you have to give permission to the tool for it to be useful
... so i think what i am saying and thinking that the system should be fool
proof, it should be provable that anything can't be automatically allowed
to anyone ... I think by defult we can have it per person right? because
that gives the option to decide and be conservative about it."
Asked again as: "Each step of a run is done under one give, and that give
traces back to one person. That person sees that step and can give it on.
Nobody sees another person's part without a give."
Sid (‹d0f62e81›): "11. yeah and its like you can give access to parts of
the run as well it does not have to be all or nothing but it has to be from
the user and the permissions they give, its like licences as well for what
it means to give access to a person can they use it for derative work, give
further persmission etc. but all these are product questions and can be
compsed with what we have right"
Stands as: per person by default, part by part, and only from the person
and the permissions they give. Nothing is allowed automatically. Licence
terms are product questions composed from the same parts.

**15. Landing.** Asked: whether a page would be written for him to read
before the store's Decisions are touched.
Sid (‹68b918be›): "15. i will most probably not read it and is auto
approved iff i have answered explicitly already."

**16. Reading today's code.** Asked: one read-only gatherer with the eight
questions of section 9.
Sid (‹68b918be›): "16. yes but only once we done will all this back and
forth and nothing else is left on your side."

**17. The order.** Asked: the stored shapes before pieces 5 to 8.
Sid (‹68b918be›): "17. we are not starting pieces 5 to 8 until the review
fixes are done"

Second pass, in the permissions working session, 27 November for Sid
(26 November from 20:13 UTC in the record). The numbers 18 to 21 are this
page's; in that session they were questions 2, 1, 3 and 4. His replies are
word for word. His first reply to all four is ‹b09854a6›; the id comes
from a gatherer's search of the record, and the working session opened no
line. His second reply, to 19, 20 and 21, was not yet in the record when
the gatherer looked. Three of the four were first put in a short form he
found cryptic, and then again in plain words; both forms are given.

The same night, at 19:46 UTC, he answered six questions in the session that
ran the review of 26 November (‹e17932a7›; they are in section 3.1 of
`src/proposal/design-pitfalls-review-2026-11-26/DECISIONS-AND-FIXES.md`).
Two of them are (a) and (b) of section 8, and are quoted under 20 and 21.

**18. Who may end a give that is not their own.**
Asked: "A yes means these four may end a give: its holder; whoever gave it,
while she still holds what she gave it under; whoever holds a give it is
lent from, directly or further up; whoever holds everything over what it
covers."
Sid (‹b09854a6›): "2 yes,"
Said to him afterwards in plain words, with no objection of his: "It covers
four parties: the person herself; whoever gave it, while she still has her
own access; the person an agent borrowed it from; and an owner." And: "One
consequence: a new owner could take back a copy the old owner sold "for
good". Protecting sold copies is a mark on the permission, added when
selling is built."
Stands as: those four may end a give. Nothing falls because the one who
gave it left; what falls is what was lent (item 5).

**19. What holding a right lets you pass on.**
First asked: "A yes means holding a right is using it, lending it to your
own agents and runs, and ending it. Giving or lending to another person
needs the right to give."
Sid (‹b09854a6›): "1 sorry don't understand fully, but its also confusing"
Asked again with the case. Alice lets Bob read her note and does not let
him share it. Bob asks his own agent to summarise it. By item 5 an agent
only borrows a person's access, and in the code a read-only holder can hand
her access to nobody, so the agent is refused. "I recommend treating them
as two things: Your own agent or run may use any access you have, with no
extra permission. Sharing with another person needs the right to share,
from whoever gave you access." And: "A yes means: "Whoever has read or
write access may lend it to her own agents and runs. Passing it to another
person needs the right to share.""
Sid: "1. agree with your 2 split"
Stands as: holding see or write includes lending it to one's own agents
and runs. Passing it to another person needs the right to give. This
narrows the second sentence of rule 2 in section 7, which was a session's:
giving is something you must hold when it reaches another person.

**20. "Seen by everyone" is a give to everyone.** (Section 8, a.)
Asked in the review session: "Is 'everyone' a holder like any other? Making
a lane public would be giving 'see' to everyone, as an act in that lane.
Today it is a setting of the lane, and for the base it is only in code. I
say yes."
Sid (‹e17932a7›): "3. yes"
First asked in the working session: "A yes means a give written in the
lane's order, to "anyone" for seeing and "anyone signed in" for writing,
never to an agent or run directly."
Sid (‹b09854a6›): "3. yes i think the message is cryptic and i worry what i
am sayign yes to"
The working session did not count that as a yes and asked again: "A yes
means: "Public is a permission given to everyone, written in the log when a
lane is made or opened. There are two everyones: anyone at all, for
reading, and anyone signed in, for writing, because an act needs an author.
An agent is never 'everyone' on its own; it comes through its person.""
Sid: "3 yes"
Stands as: as asked, both times.

**21. Owners: each acts alone, a lane always has one, and a lane can be
handed over in one act.** (Section 8, b.)
Asked in the review session: "4a. With several owners, may each act alone,
without the others agreeing? I say yes. 4b. Can the last owner leave only
by handing ownership on? I say yes."
Sid (‹e17932a7›): "4. both yes"
First asked in the working session: "A yes means an act is refused if it
would leave no owner give that stands on its own." And: "It unblocks
handing over a whole lane in one act; today the first give can never end."
Sid (‹b09854a6›): "4. again a bit cryptic but reading the second bullet i
say yes, we want it to be able to hand over whole lane in one act. don
we???"
Asked again: "I recommend replacing it with a guard that does the same job
without blocking the hand-over: "a lane must always have at least one
owner; an act that would leave none is refused". A hand-over passes,
because the same act makes the new owner and ends the old one." And: "A yes
means that one quoted sentence."
Sid: "4. your recommendation"
Stands as: several owners each act alone. A whole lane can be handed over
in one act. A lane must always have at least one owner, and an act that
would leave none is refused. Not decided: whether acting alone reaches
ending another owner's give, which 4a does not name, and what happens when
the only owner is gone (section 12).

## 7. The solution as discussed (A SESSION'S, built on section 6)

Second pass: for the first slice, section 12 replaces three things below:
the record of a give (no "uses"; what it covers is a group, not a lane),
the statement to prove, and the first default (an act is born into its
lane's default group). Rule 2's second sentence is narrowed by item 19.
"No gate rule reads a clock" in rule 8 is a property of the built work and
was not decided under item 4. "The gives it begins with", in the paragraph
on a run, was not in what Sid said yes to under item 8. In the sale rows,
"the buyer holds use" is loose: "use" is not a third right. The buyer may
ask for a run, which is a write, and the run sees the skill under a give
lent from the seller. The rest of this section stands as a session's, and
section 12 says which parts of it Sid has been asked about.

Atoms:

- An act, stamped with its author. Nothing is changed or erased.
- Two rights, one per moment at which the store checks anything. See is
  checked at each read. Write is checked once, when an act arrives, and the
  answer is kept.
- A give and an end are acts. A give hands see or write, over an act or a
  group, to a party. An end takes it back.
- A group is declared by an act. Marks put acts in or take them out. An act
  can be in any number of groups, and groups can be in groups. The same
  holds for parties: people in groups, a run in its agent.
- A pointer is data in an act: this act refers to that one, with a kind.

The record of a give:

    id          its own
    holder      a party: one run, an agent, a person, a group, everyone
    allows      see, write; and which kinds of act
    covers      a group of acts; on day one, a whole lane
    uses        a count, or no limit
    given by    the author of the give act
    lent from   optional: the give this one is lent from

An ordinary act carries its author (a person or a run) and the id of the
give it acts under. An act that replaces another also names the act it was
written from. Today an act carries the whole permission, inside its digest.

A run is a party of its own, declared by a start act and closed by an end
act. The start says which agent, what started it (a person's act, a tool's
run or an event) and the gives it begins with. Whom it acts for is answered
per give. An agent is the group of its runs.

Rules, with what each rests on:

1. The author of an act holds everything over it when it is born, and
   whoever declares a group holds the group. (Section 8, c.)
2. You can give only what you hold. Giving is itself something you must
   hold, and what you give cannot exceed what you hold. (Decided, 2.)
3. What is lent never allows or covers more than its lender does at that
   moment, and falls with it. Agents and runs hold only lent authority,
   ending at a person. People stand on their own by default. (Decided, 5.)
4. Putting an act into a group, as far as access goes, is a give.
   (Section 8, d.)
5. One act may carry several permission changes that pass or fail as one.
   Transfer is an end and a give in one act. (Decided, 7. "Hand on" as a
   kind of give: section 8, f.)
6. Refer: anyone may write a pointer; it never carries see; whether it
   shows at the target is the owner's give, worked out at each read.
   (Section 8, e.)
7. Two acts written from the same one: the store takes both and raises the
   conflict. (Decided, 10.)
8. An end is an act. No gate rule reads a clock. (Decided, 4.)
9. The owner is a right apart from the maker. (Decided, 6.)
10. A give to an agent reaches only the runs started by the giver or the
    giver's triggers, unless the giver says wider. (Section 8, g.)

The statement to prove, from Sid's "it should be provable that anything
can't be automatically allowed to anyone": no party can see or write over
an act unless a chain of gives leads from the act's author to that party,
and each giver held what they gave at that moment. It rests on rules 1 to
4. It is proposed as one of the store's proofs.

Defaults for day one:

- An act is born into its lane's group, the broad one that already exists.
- Access is given over that group, to see or to see and write.
- The lane's maker holds everything over the lane's group, until the lane
  says otherwise.
- A give lasts until ended. A run's give ends with the run.
- What a run did is seen per person.

Limits, which are not choices:

- Seeing composes anything at any time. Writing is decided in one lane at
  one moment, so every give, end and mark that a write depends on must be
  in that lane's order by then. A group or a run that spans lanes needs its
  changes delivered to each lane it touches. This is the one place where
  ordering and grouping meet.
- Cost is paid per group, not per act. Readers with the same access share
  one view. Thin groups cost more.
- With seeing per act, two readers can see different newest acts.
- If giving is unrestricted, nobody can say in general who can end up with
  what (Harrison, Ruzzo and Ullman 1976, from memory). Rule 2 keeps it
  answerable.
- The store can record a use of what someone may see, and refuse to attach
  it. It cannot stop them retyping it.

The case Sid brought, as rows of the record:

- A copy of a skill that points to him: the buyer holds use, covering the
  skill, given by him, lent from nothing. It outlives any change to his.
- A copy the buyer may resell: the same, with "hand on" in what it allows.
- The whole ownership: one act ends his owner permission and gives the
  buyer one that stands. Copies sold before keep standing. He stays the
  maker.
- A sale the buyer must accept: he gives the buyer a once-only permission
  to perform that hand-over, and the buyer's one act does it.

Cases already run against the answers, and what each one changed. Every
change to the model in this session came from a case, so a case that is not
on this list is the likeliest place for a miss.

- Raising or lowering a holder: became one act that changes what a give
  allows (rule 5).
- Handing over or selling the whole of a thing: became an end and a give in
  one act, with the new give standing on its own (rules 3, 5 and 9).
- Selling copies that outlive the seller's own permission: became a give
  lent from nothing (rule 3).
- Pointing at someone else's act, to cite it, reply to it or build on it:
  became refer (rule 6).
- An agent of one person running inside another person's tool for a third:
  became "whom a run acts for is answered per give", seeing what a run did
  per person, and rule 10.
- Two acts written from the same one: became rule 7.
- Ten thousand runs of one agent: became the run as a party of its own.
- A holder with read giving write to a second id of his own: rule 2.
- A subscription, a timeout, an agent active for one task: became "an end
  is an act, and how long is who writes the end" (rule 8).
- Writing without seeing everything, as with exam answers or sign-in acts:
  see and write came apart. Seeing less than a lane is still open
  (section 9, item 13).

## 8. Stated by a session, no reply of Sid's

By his rule in item 15 these are not decided. (a) and (b) are inside the
questions of 1 and 6 as asked, so his "same page for all 7" may cover them.
Since 27 November (a) and (b) are decided. The other six were put to him
again that night, each as a range with a default, and are open
(section 12).

- a. "Everyone" is a holder like any other, not a setting of the lane.
  Decided: section 6, item 20.
- b. Several owners each act alone, and the last owner cannot leave without
  handing on. Decided: section 6, item 21.
- c. The author of an act holds everything over it when it is born.
- d. Putting an act into a group, as far as access goes, is a give and
  needs the right to give that act.
- e. Refer: anyone may write a pointer, it never carries see, and whether
  it shows at the target is the owner's give.
- f. "Hand on" is a kind of give: it ends mine and gives the same to you in
  one act.
- g. A give to an agent reaches only the runs started by the giver or the
  giver's triggers, unless the giver says wider.
- h. An agent's work is taken in by an act of his, unless he sets that
  agent to write on its own.

## 9. Open

Can wait, and block nothing:

- 12. Which references show at a target. The range: none; those from
  parties the owner names; those from anyone who may write in the target's
  group; any that the reader can see.
- 13. What a reader gets when the newest act is one she may not see.
- 14. Words: his word for a named group of acts, "end" or "revoke", and the
  names of the day-one bundles.

Not placed: which day real data starts, and which lane his own work goes
into.

Engineering, a session's to work out:

- the stored record of a give, and what an act carries (for the first
  slice: section 12);
- the stored form of a group, by rule and by list;
- how a group or a run that spans lanes reaches each lane;
- the table of what points at an act;
- whether his approving act carries the content or points at it;
- live views where the number of viewers stops mattering;
- time limits where late is not acceptable: an end written by a timer is
  late if the timer is down;
- who vouches that an act comes from a run, when the run is hosted by
  someone else's tool;
- the plan for pieces 5 to 8, corrected toward section 6;
- where each default lives. This page says "a default that can change" of
  which act shows as newest, of which references show, and of the day-one
  defaults, and never says where the default is kept. A default drawn at
  read has to come from an act that is kept, not from a value in code;
- what the store checks when a rule says "a person". Rule 3 ends lent
  authority "at a person" and seeing is "per person", and the store can
  check an id, not a person. (For the first slice: section 12, point 4.)

Not read from today's code (the eight facts of item 16):

- Can a row above a permission sit in another lane?
- Is an act's author kept apart from the holder of its permission?
- Who can give a lane's first permission again?
- What settings does a lane have besides "seen by everyone"?
- What is a stamp?
- Why can an end and a give not go in one act?
- What does the store do when an act is run again?
- Does an act name the fact it was written against?

## 10. What changed since the text of 26 November 18:40 UTC

At that time Sid pasted into an Antigravity session (‹a16b151d›, long
prompt) the first forms of the six questions, the transfer answer and
questions 7 to 9. Five of them changed after his replies:

- 2: "four separate words" became two atoms, see and write, with the kinds
  of act as composition.
- 8: "for which person", as one fact of a run, became: answered per give.
- 9: "the second as the default" became: both offered, the relation kept
  both ways, and an agent can be started by an event.
- 10: "refuse and show both" became: the store takes both and raises the
  conflict.
- 11: "the person it acts for" became: per person, by whose give each step
  was done under.

Added after that text: refer; the author as the root; putting an act into
a group is a give; a give to an agent reaches only the giver's runs; the
statement to prove.

## 11. Not checked

- No code was read by this session. Every code fact is the page of
  26 November's, which rests on gatherers' quotes of that day.
- The three design-pitfalls reviews of the whole store of 26 November were
  not read: the one in `src/proposal/design-pitfalls-review-2026-11-26/`,
  the Antigravity session's runs 1 to 5, and the third. By ‹1b4e544b› (a
  long prompt holding two sessions' replies) they raise calls that cut
  through this page: what `:session` or a run is on an act; whether a
  program acting as a person writes that person's id into acts; whether a
  lane is ordering alone or also visibility; what a fact with no value
  means; "its time passed" stored as "ended". Nothing here was held against
  them.
- The store's Decisions sections in `src-store/README.md` were not read,
  nor the plan for pieces 5 to 8, nor Sid's notes of 24 November that are
  images.
- The names from the theory of access control are from memory.
- Nothing is measured: no cost of a check, no number of viewers, no size of
  the fix.
- Second pass: the permissions working session read no code and opened no
  line of the record. It worked from this page and the review as Sid pasted
  them, read parts of the review page of 26 November (its sections 1, 3, 7
  and 8), and opened this file only to land its text. Every code fact it
  leans on is the review's and is not checked. Ids marked "gatherer" in
  section 12 are a subagent's and were not opened by the session.

## 12. Second pass, 27 November 2026: the review, the order, the first slice

A SESSION'S throughout, except where an item of section 6 or a line of
Sid's is named. Written by the permissions working session. "Gatherer"
marks an id or a quote that comes from one read-only search of the record
by a subagent of that session, up to 26 November 20:20 UTC.

### The review

Another session (neon) reviewed this page on one question of Sid's: what
did we miss? It listed 24 misses: 15 that change what is stored, 3 that do
not, and 6 statements of section 4 that it says no longer match the code.
The review is not on disk. Sid pasted it into the working session, so it is
in the record as a paste of his; it opens "Part 1 — Misses That Change What
Is Stored". Its receipts name files and lines of the code.

The working session's reading, miss by miss, is in the record as that
session's replies. In short:

- 15 of the 24 are real: 1 to 4, 6 to 13, 16 to 18.
- 3 are not real as misses of Sid's answers. Miss 5 says the two atoms
  break; they hold, and each of its cases is made of what we have. Misses
  14 and 15 are about Auth and the whole store's act form: places where
  what is built would contradict the answers, not gaps in the answers.
- 6 are corrections against the code (19 to 24), not checked.
- No miss needs a new atom.

The review marks nearly every miss as Sid's because it changes a stored
form. The working session sorted on a narrower line. A call is Sid's when
the answers differ in what a person can do, see or lose, or when it changes
a reply of his. It is engineering when every answer behaves the same on day
one and one of them keeps the whole range open; the session took that one.

### The order

DECIDED, in the session that ran the review of 26 November (its page,
`src/proposal/design-pitfalls-review-2026-11-26/DECISIONS-AND-FIXES.md`,
section 3.1, item 6). Asked: "Fixes first, and the new permission design
after as its own build? [...] One fix would wait for the new design: taking
`:session` off the act needs the run." Sid (‹e17932a7›): "6. yes".

SID'S, on the size of a fix (‹f07c97d1›, 25 November, gatherer): "this is
only related to what we already did and fixing that mistake anything we
need to build on top of it is different you are again making acope creep
which bit us before this need to be absolutely constrained please which
does not mean lacking do you understand?"

SID'S, later the same night in the working session (ids not looked up): he
asked for the implementation prompt of this page's slice to be sent to a
session of his named "permission-fix-implementer", and said: "if the work
is going to be too big then we can do it in batches so that the first batch
only does 1/4th of the total fixes needed to be done". The working session
put his "fixes first" answer beside that and sent the first batch. Which
goes first is his.

How this page's slice meets the review page's list of fixes (its section
7). Its group 7.3 waits for this design: F29 (everyone is a holder) is
item 20 here; F31 (the yes keeps "this exact act") is point 10 below; F28
(`:session` leaves the act) is the note on per-run permissions below; F32
and F25 are not in the slice. Its groups 7.1 and 7.2 are not this page's.
That page says every change of the act's form remakes the fixture and the
digests, and that a session may hold F22 to F24 to change the form once.
The slice changes the act's form too. On F22 and F26 Sid has doubts
(‹996c8c18›, 26 November, gatherer): of the depot word in an act's id,
"this is afair needed for us to hash a act by if it goes in the fast place
streaming or the slow microbatch? don't we?"; of the version, "why will it
be forever on version 1 if we find the v1 on old one bad we can update it
as a system security update".

### The first slice

The slice, in the words of the prompt Sid pasted into the working session
(a pasted prompt; who typed it was not checked): "a give with an id of its
own that an act names; any number of permissions per holder on a lane;
several permission changes in one act that pass or fail as one; a give lent
from another, or standing on its own; give only what you hold; a permission
never given is not answered "ended", and a holder can end her own."

Decided, and built by the slice: items 2, 5, 6 and 7, and of the second
pass items 18 to 21.

Sixteen points settled by the working session for the slice. They are a
session's, made without reading code; "per the review" marks a code fact
that is the review's. The review's misses each one answers are in
brackets.

1. The id of a give is made from the act that makes it: that act's id and
   the give's place in the act. Replay gives the same id, no check for
   uniqueness is needed, and the id carries its lane. Whether the depot
   word stays in an act's id is F22 of the review page; the give's id takes
   the act's id as it is. [3, 10, 15]
   Replaced on 27 November by the implementer's reading of the code
   («678be1d3»): the giver's program makes the id, and the give is a fact
   on that id, as a yes is today. An id made from an act is not something
   a fact can sit on in this store. A re-sent act still gives the same id,
   because the id is in the act, and a place takes one give for good.
2. Three acts name a give: give, change, end. A change keeps the id and
   replaces what the give allows or covers; never the holder, the lender or
   who gave it. It is not a new atom: it is "replace", the act that changes
   a note, applied to a give. Without it, lowering someone who holds see
   and write under one give is an end and a give, and everything she lent
   falls: the second half of bug 2. Each change is an act with its own
   author, so "given by" stays true and the lane's order says which terms
   an act was taken under. Raising needs what giving needs; lowering needs
   what ending needs (item 18). By item 5 ("at that moment"), lowering a
   lender caps what she lent, and raising her again uncaps it. An end is
   final: given again is a new give with a new id. [2]
3. What is made under a lent give is lent from it. Only a give that stands
   on its own can make another that stands on its own. It follows from
   items 2 and 5. [2]
4. "Lent from" may be set on a give to anyone, and must be set when the
   holder is not a person. Item 5 lets people stand on their own "by
   default", so the other end of the range stays; the narrowing to agents
   and runs was in the answer about cost, and the cost is the same. For
   this the gate has to tell a person from an agent or a run: a party's id
   says which, and the door writes the author's on each act, not the
   sender. [2]
5. An act names a list of the gives it acts under. Day one takes none or
   one. None is a lane's making act, or the end of a give of one's own.
   More than one is left for later: an act that needs two gives at once, a
   vote of owners. A give is an act too, so the give it was made under is
   on its record. "An ordinary act carries one id" was in the answer Sid
   said "5. yeah" to, and an ordinary act still carries one. [3]
6. One act is one moment. Each change in it is checked against what its
   author held just before the act. Changes may name what the same act
   makes. Then the result is checked as a whole: no give left lent from one
   the same act ends, no give changed twice, and the lane still has an
   owner (item 21). This answers Sid's question before item 7, "who is
   doing these 2 changes under whose permission?": the author, under what
   she held coming in. Per the review today's gate already checks against
   the state before the act. [3, 21]
7. Inside one lane. All changes of one act, and a give and the give it is
   lent from, are in one lane, because a gate decides from its own lane's
   order. Item 7 then reads "pass or fail as one, inside one lane". [10]
8. "A holder can end her own" means the party that is the holder. A member
   of a group, or of everyone, cannot end the group's give. Today's guard,
   that a lane's first give never ends (per the review), becomes item 21's:
   it may end in an act that leaves the lane an owner. [5, 6, 20]
9. What a give covers is a group, not a lane. Day one's value is "the
   default group of lane L", named apart from the lane, and an act that
   says nothing is born into it. This keeps seeing less than a lane
   possible later without re-forming every give. [4, 11]
10. Today's yes is carried as a give that covers one exact act, named by
    its digest. There is no "uses" field in the slice: an exact act can be
    taken once only, by itself, and counts come later. [6, 7]
11. The gate writes no act. A yes that has been used is worked out from the
    act taken under it. So one writer named under item 4, "the gate when
    the uses run out", is not a writer; the others are outside the store
    and stay. The review calls "no topology appends to a depot" a rule of
    the store; per the gatherer it is a session's derivation («5d9592be»),
    not a line of Sid's. This point stands on its own ground. [6, 22]
12. The right to give says what may be given on. A bare "may give" cannot
    say "may give see, and the receiver may not give further". Day one
    keeps today's two values: "see", which by item 19 includes lending it
    to one's own agents and runs, and "see, write and give all of it". [5]
13. One form. The old whole-permission form is refused for new acts, not
    kept beside the new one under a version the sender picks. The
    permissions written by hand above the store (section 4) go through one
    builder. How a rule's edition relates to `:version` is F26 of the
    review page. [15, 18]
14. Rows carry the give's id, not its text. [24]
15. A lane's making act names the holder of its first give (item 6).
    Whoever makes a lane for someone else holds nothing over it afterwards.
    By item 20 a lane that is open gets its give to everyone in the same
    act, or in a later one. Who owns the base is not touched: his lines
    differ (‹e5a2200e›, 15 October: "I (sid) has the "full" access to
    softland base"; ‹f9cfe0c5›, 24 November: "agree all ok here" to a list
    of a session's defaults that held "the base belongs to the system";
    both gatherer). The slice leaves the base's owner as it is today.
    [1, 11]
16. Live views. When any row a viewer depends on changes, her standing is
    read again from one snapshot. The slice adds a proof that a transfer in
    one act never shows a state in between. [18]

The record the slice then stores:

    give      id         the act that made it, and its place in that act
              holder     a party; its id says person, agent or run; or
                         everyone ("anyone", "anyone signed in")
              allows     see; write, by kind of act; give, with what may
                         be given on
              covers     a group: the default group of a lane, or one
                         exact act
              given by   the author of the give act
              under      the give that act named
              lent from  none, or a give in the same lane
    change    names a give; the new allows or covers
    end       names a give
    an act    its author; the gives it acts under (none or one today)

The statement to prove. As section 7 words it, it is false in every shared
lane: Carol sees what Bob wrote there with no give from Bob to her. For the
slice it reads:

> An act is taken only if it names a give that stood just before it, held
> by its author and allowing that kind of act where it lands; or it is a
> lane's making act; or it ends a give its author holds. Nobody is shown an
> act unless she holds a give that stands and allows see over the group it
> was born into, or she wrote it. Every give leads back, give by give, to
> the making act of its lane. Each giver held what she gave when she gave
> it, and what is lent also needs every give above it to stand still.

It speaks of what the store answers for acts it has taken, given that the
door vouches for each act's author; whoever can write to Rama directly is
outside it. "Held by its author" includes holding through everyone
(item 20). "Or she wrote it" is today's behaviour per the review, and is
open (question 5 below). Two things in today's code stay outside the
statement until Sid answers: refused acts read by a lane's members
(question 12) and the open read of the store's own facts (question 11).

Per-run permissions. Per the review, today's per-run permission is keyed by
the login sitting (`:session` on an act), and the sitting is inside what
makes an act the same act. A run is not a sitting (item 8), and a run that
outlives its sitting has its re-sent acts refused as different acts,
against item 7. Taking `:session` off the act is F28 of the review page and
needs the run. Until then a per-run permission is carried as what it
enforces, a give good for one sitting, and is not renamed into a run.

Owner over owner. Items 18 and 21 read together let one owner end another
owner's give, as long as the lane keeps an owner. Sid was not shown that
case. The slice follows the two items; question 4 below can turn it.

### What the implementer read in the code

IN THE CODE, per the session "permission-fix-implementer" («678be1d3»,
26 November 20:41 UTC), which read the code the first batch changes. Not
checked by the working session.

- A yes already sits on an id its giver makes, takes its place for good,
  and has "never given" and "ended" answered apart.
- A read finds a holder's permission because it sits at (lane, holder).
  With any number of gives per holder, a read needs her gives listed by
  holder, written in the same decision as the give. Each live viewer then
  holds more pushes than today.
- The nested form is what caps a chain at 32 today. With ids, the gate has
  to refuse a give that is too deep.
- What kept an end and a give out of one act is that an act names a place
  once, and both sat at (lane, holder). With each give at its own place
  they can ride in one act, already after batch 1.
- Stamps are compared today only because a give made again has the same
  id.
- "One per run" is one per sitting id per lane, with the holder checked
  afterwards.
- Of the eight facts of section 9, four: a row above a permission cannot
  sit in another lane; an act's author must be the holder of its
  permission, except the system making a personal lane; nobody can give a
  lane's first permission again; an act names what it was written against
  per fact (`:replaces`), plus a `:based-on` label no gate reads.

### How it is built: four batches

SID'S, 27 November, in the working session (ids not looked up): "if the
work is going to be too big then we can do it in batches so that the first
batch only does 1/4th of the total fixes needed to be done". And of the
lane work: "it was proposed as 8-10 steps which i don't like at all and
should all be done in an intelligent manner not some "slow is better"
philosophy."

A session's cut. Each batch leaves the store in a state it can rest in,
with its proofs. A batch is given as what must be true at its end and what
evidence to bring back, not as steps.

1. The give gets an id. A permission becomes a record with an id; an act
   names the give it acts under; an end names the give it ends. Behaviour
   stays as today but for what falls out of the id: any number of gives per
   holder (bug 1), a give never given answered apart from one that ended
   (bug 3), and a holder ending her own (bug 4). The record keeps apart
   from the start what today's form fuses, with today's behaviour as the
   value: every give is lent from the one above it. It uses points 1, 5, 9,
   10, 13, 14 and 15, the last sentence of point 2 and the first of
   point 8.
2. Lent or standing (item 5, points 3 and 4); give only what you hold,
   with a right to give that says what may be given on (point 12); who may
   end (item 18); lending to one's own agents and runs (item 19).
3. One act as one moment (points 6 and 7); the change that keeps a give's
   id (point 2, bug 2); the owner guard and the hand-over of a lane
   (item 21, point 8).
4. Public as a give to everyone (item 20); the statement to prove as a
   proof over the whole; the live-view proof (point 16).

The prompts for all four are in `BUILD.md`, beside this page. Under each
batch it says which lines are decided and which are a session's. A batch's
text is corrected when the report of the one before it shows the page
wrong.

### Readings in sections 6 and 7 that are a session's

- Item 9, "both are offered": his words keep each act its own atom with the
  relation both ways; they do not say the agent's act also lands in his
  lane (question 10 below).
- Item 7, "the same act sent twice counts once": added by a session. The
  store already works that way, and the review page says the same.
- Item 1, "any number of permissions each": his line for it is ‹95754ddf›,
  which is about an agent's instances.
- Rule 2, "giving is itself something you must hold": a session's, narrowed
  by item 19.
- Rule 8, "no gate rule reads a clock": a session wrote it first
  («f2eb3f50», 13 October). His own lines are ‹f8a673a6›, "it is the clock
  comes before the depot.", and ‹2f80019d›, "order and time are different
  and we don't use the time for ordering. every thing that goes in depot
  has a time attached to it." (gatherer)
- The paragraph on a run in section 7, "the gives it begins with": not in
  what he said yes to under item 8.

### Put to Sid on 27 November and open

Each was put as a range with a default. The full text is in the record, in
the working session's replies (under "Yours, by number"); the numbers are
that session's. Under each: what the gatherer found of his in the record.
None of those lines answers the question as put.

- 4. Owners, the parts still open: does "each acts alone" (item 21) reach
  ending another owner's give; what happens when the only owner is gone.
  Record: nothing on the second.
- 5. What a writer holds over her own act (section 8, c). Default: an act
  belongs to the group it is born into, its writer can always see it, and
  only makers of lanes and groups are roots. Record: ‹071290dc›
  (12 October), on a refused writer, "if they have it in the log, we can
  send them back, hey, this was rejected"; ‹6ce6e4dd› (21 November),
  "having who did what where etc. stamped is quite esseential data".
- 6. Putting an act into a group (section 8, d). Default: a mark is a
  pointer, and access goes with it only when the marker may give that act
  and says so. Record: ‹af5a942e› (17 September), his own question whether
  groups "can be infinitely compositional or is the definition more strict
  here".
- 7. Refer (section 8, e; section 9, item 12). Default: each group decides
  which pointers show inside it; a hidden target reads "something you may
  not see". Record: ‹e14a5bd4›, "ok there is a permission type "refer" how
  does it compose? its not specifically write access, its an access to what
  can be done over it right so is it atomic and a new type/". The session's
  answer to it left which references show for later, so "same page for all
  7" does not settle that.
- 8. Hand on (section 8, f). Default: it needs "hand on" or the right to
  give, and the new give stands as the old one stood. Record: nothing
  beyond item 21 and ‹bb35585c›.
- 9. Which runs a give to an agent reaches (section 8, g). Default: the
  runs I start and the runs my triggers start, read from the give each
  run's start act was written under. Record: ‹95754ddf› (21 November),
  "each instance can have different permisiion based on different uses it
  has or work so yes".
- 10. How an agent's work enters his lane (section 8, h). Default: the
  agent writes in its own lane and an act of his takes the work in, as a
  pointer plus his give of see. Record: ‹93a88844› item 4 (20 November),
  "This is a tough one i see both uses for different cases [...] what is
  the one that gives the most spectrum hmmm".
- 11. Who sees the gives, the ends and who is in a group. Default: the
  giver, the holder and whoever holds everything over what it covers.
  Record: nothing.
- 12. Who sees a refused act. Default: its writer whole; the lane's owners
  see that it happened and not what it said. Record: that it is kept and
  can be read back is decided (‹071290dc›, ‹fad2158b›, 12 October; "1,
  keep", ‹e17932a7›). Who sees it is not. A session read his lean of
  20 November (‹93a88844›, "5.2 B") as "any reader of the layer", which is
  wider than this default.
- 13. May a program write under a person's own id. Default: only what she
  drives by hand; anything that acts on its own gets an id of its own and
  lent gives. Record: ‹7a626d9d› (26 November), "any token we all know is
  always "on behalf of" this is how its read"; ‹e5a2200e› item 3
  (15 October), "best is to have each agent their own identity". The
  review page carries it as fix F15, reported by carbon as answered "no".
  There is no typed "no" to the question as put.
- 14. Where an agent's authority may end (reopens item 5, a). Default: at a
  give that stands on its own, held by a person or a group of people.
  Record, two lines that differ: ‹ddf13d17› (6 October), "a group can have
  its own agents"; ‹f3f530b8› (7 October), "it ties back to a single
  individual". The company agent that outlives an employee was not put to
  him before.
- 15. What has to be done before pieces 5 to 8 (item 17). Record: "The
  order" above.

Earlier lines of his that bear on items 18 to 20 (gatherer):

- 18: ‹e5a2200e› item 5 (15 October), "for malicious users we should be
  able to block as the system and then the user when giving permissions to
  different agents, tools,layers, groputs etc. should be able to do the
  same. this is one end of the spectrum they should also be able to block
  "type" of actions". The four of item 18 do not name the system. The
  working session reads a block by the system as a block at the door, not
  as the end of a give; that reading is a session's.
- 19: ‹b6b0f700› (18 November), "I will have my own agents, which might
  have like almost all of the permissions that I have, because I want them
  to work through things, but still I want them to not be able to read my
  environment variable keys or some other type of data." So a lend to one's
  own agent can be narrower than what one holds. And ‹f9cfe0c5›
  (24 November), "agree all ok here" to a list of a session's defaults that
  held "any signed-in person may invite, and an agent may not".
- 20: ‹e5a2200e› item 6 (15 October), "why are we not saying "signed in"
  "not signed in" why are there 3 categories??"; ‹564310b8›, "A. Yes" to
  people who are not signed in reading the base. An earlier yes of his
  (‹5dcbfd73›, 11 October) was to a lane setting, "members or everyone";
  item 20 replaces it.

Not asked on 27 November, because the review says Sid had answered. What
the gatherer found:

- Recorded reads are kept, by his words (‹5ff89890›, 12 October: "we take
  the side of spectrum that captures more"; ‹45c7127b›, 21 November: "yes
  to the general idea of reads are writtien down as well"). Who may see
  them: no line but item 11.
- Time: ‹2f80019d› and ‹296a2a7b› (14 October): order and time are apart,
  everything in a depot has a time, and a thing with no time is refused. On
  limits there is only item 4.
- The sentence of 25 November: he approved losing "and shows to its
  members" (‹e17932a7›, "5.yse"; fix F1 of the review page).
