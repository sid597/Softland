This is a working session on the permissions work of the store. Read nothing: no files, no code, no memtool. Write nothing to disk. Everything you need is pasted below: the page, then a review of it.

The page holds what was built, what was found wrong, each question I was asked with my reply (section 6), the solution a session drew from my replies (section 7), eight statements I have not answered (section 8), and what is open (section 9). Only section 6 is decided.

The review is from another session. I asked it one thing: what did we miss?

I want you to work its misses out with me.

In your first reply:
- For each miss, say in a line or two whether it is real: does its case really break the answers, or leave something unanswered? Say so plainly when it is not, and why.
- Sort the real ones into mine to decide and engineering. Settle the engineering ones yourself and tell me how in a line or two.
- Say which misses touch the first slice that is about to be built. I need that first, because a build is waiting on it. The slice: a give with an id of its own that an act names; any number of permissions per holder on a lane; several permission changes in one act that pass or fail as one; a give lent from another, or standing on its own; give only what you hold; a permission never given is not answered "ended", and a holder can end her own.

Then put the ones that are mine to me, numbered so I can reply by number. For each: the question, the case that raised it, what stands today (decided in section 6, a session's in section 7 or 8, or nothing), the range of answers, the point you would start with as the default and why, and what it fixes for good. Not as an either/or. Start with the ones that touch the slice. Fold a to h of section 8 into the same pass where a miss touches them, and ask me the rest of a to h at the end.

Do not ask me again what section 6 already answers. If a miss reopens one of my answers, put my reply beside it and say what the case changes.

My frame for the solution is atomic units, composition, spectrum and defaults. When a miss looks like a new kind of thing, say first whether it is an atom or made of what we have.

When I bring a case that breaks what you said, change the model and say what changed.

At the end, give me each question as you put it with my reply in full, and the text to add to or change in the page, section by section. Mark what I answered as decided and everything else as yours.

===== THE PAGE =====

# Permissions in the store: what was built, what was found, what Sid answered, what is open

As of 27 November 2026. Written by a Claude Code session with Sid (session
"permissions review"), to be handed to other sessions. It assumes nothing of
any conversation. The record's times are UTC, where this session ran on
26 November.

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
- Eight statements of a session have no reply of his (section 8). Three
  questions can wait (section 9).
- No real data is kept yet. The fixes come before pieces 5 to 8 (Sid,
  section 6, item 17).

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

## 7. The solution as discussed (A SESSION'S, built on section 6)

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

- a. "Everyone" is a holder like any other, not a setting of the lane.
- b. Several owners each act alone, and the last owner cannot leave without
  handing on.
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

- the stored record of a give, and what an act carries;
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
  check an id, not a person.

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

===== THE REVIEW (neon swarm) =====

