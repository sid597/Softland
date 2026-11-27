# Read path — the read path's pure parts, and what a query is

[Parent: store](../README.md) · [Store source entry](../../../README.md).

On 27 November 2026 one file of this folder is in the form of the permission
fix: [lanes.clj](lanes.clj), which reads an asker's rights in a lane from the
permissions that she holds there. [query.clj](query.clj),
[sources.clj](sources.clj), [answer.clj](answer.clj) and [acts.clj](acts.clj)
are in the form before it and do not load against it; what this page says of
a lane's setting "members" or "everyone", of her permission's row and of the
rows above it is of that earlier form
([the store's page](../README.md) says which code is in which).

The read path is [read_path.clj](../read_path.clj), one folder up, beside
[door.clj](../door.clj) and [module.clj](../module.clj): the one piece of code by
which a fact comes back out of the store to a caller in the runtime's process,
once or kept live, and by which a lane's acts come back out by stamp, once. It is the only file of the read path that requires Rama, and
the only file of this tree that opens a table to read a fact. This folder holds
the rules that it stands on.

All of this folder is pure code and loads with Clojure alone.

| File | Responsibility | State and lifetime |
|---|---|---|
| [query.clj](query.clj) | A query written as data: its one form, declared as shapes and rules in `act.clj`'s language; the five words that can be asked; the check; the read path's numbers. | None. |
| [lanes.clj](lanes.clj) | The lanes of a read: which lanes, in what order, and which of them is the read's home; whether a lane is made; whether an asker may see it; what a read says of a lane that it left out; what a lane's own rows mean for one query. | None. |
| [sources.clj](sources.clj) | The sources of a read, as data: each row or run that the read path asks of Rama, with its table and its routing id, which says on which task a lane is read; which of them a read wants next; the calls of one round; the size rules of a run; which keys and places a change names. | None. |
| [answer.clj](answer.clj) | What a read gives: the lanes put together, the nearest that holds a fact first; a place-answer; and what a change tells a live asker. | None. |
| [acts.clj](acts.clj) | The read of a lane's acts by stamp: which sources it wants next, round by round; the one rule of who reads a decision whole; the page within its two bounds; and the answer. | None. |
| [words.clj](words.clj) | Everything that the read path says in place of an answer, as data: its own ten words, why a live read ended, why a lane gave nothing. | None. |

## How the files relate

[words.clj](words.clj) requires nothing. [lanes.clj](lanes.clj) reads rows of
the facts table with [rows.clj](../record/rows.clj), and asks
[first_act.clj](../gate/first_act.clj) what a lane starts with.
[sources.clj](sources.clj) requires the lanes, and takes every text of a key
from [rows.clj](../record/rows.clj): no key is made in this folder.
[query.clj](query.clj) requires [the act](../act.clj), for the engine that reads
a shape; the door's [declaration](../door/declaration.clj), for the tests of an
id and the store keys; [rows.clj](../record/rows.clj), for the order of a run's
two ends; and the sources, for the one test of a letter that a range of Rama's
does not order as the table does. [answer.clj](answer.clj) requires the lanes
and the sources. [acts.clj](acts.clj) requires the lanes and the sources, for
the rights, which it reads as a read of facts reads them; it takes the keys of
the line from [rows.clj](../record/rows.clj), and the act-id's depot from
[first_act.clj](../gate/first_act.clj).

[read_path.clj](../read_path.clj) requires the query, the sources, the answer,
the acts and the words. It holds no rule of a read. It fetches what the sources say is
missing, and hands the answer what it fetched; at a change of a push it does
what two pure functions of [answer.clj](answer.clj) say. So the
[pure proof](../../../../test-store/softland/store/README.md) of the read path
can play those two functions in every order of the callbacks, with no cluster.

A word of a query that reads facts is declared in three maps that are keyed by
the word: what it is and its shape, in [query.clj](query.clj); what it reads in
one lane, in [sources.clj](sources.clj); how its rows are put together, in
[answer.clj](answer.clj). A word that does not exist yet is one entry in each.
The word of a lane's acts, `:acts`, reads a lane's line and no facts: it is in
[query.clj](query.clj), and [acts.clj](acts.clj) says what it reads and puts it
together.

## Boundaries

Nothing in this folder calls Rama, and nothing here builds a path of Rama's: a
source is data, and [read_path.clj](../read_path.clj) turns it into one of the
forms of path that one of its functions makes. Nothing here writes: the read path appends to no depot, and no
read is kept. Nothing here holds state: who looks at what, and the pushes, are
the memory of [read_path.clj](../read_path.clj).

The read path is not sign-in, not a network edge, not a view or a screen, and
not a record of who read what. It is in-process Clojure, and the runtime asks
it ([seam.clj](../../../../src-runtime/softland/runtime/seam.clj)).

## Decisions this build stands on

Sid's decisions are in [the store source entry](../../../README.md), with his
words. The read path stands on the section of 11 and 12 October 2026, by entry:

| Entry | Decision | Where it shows in the read path |
|---|---|---|
| 1 | A lane has a setting, "members" or "everyone"; a permission says what it allows | The standing of an asker in a lane, in [lanes.clj](lanes.clj). Any permission that stands allows seeing; what it allows beyond that is not read here. Whether it stands is the gates' own rule, read from the same rows ([chain.clj](../gate/chain.clj)) |
| 2 | A lane says which lanes it builds on; a view may give another list; the nearest lane that holds a fact wins, also with "nothing here" | The order of the lanes, in [lanes.clj](lanes.clj); the walk of a place, in [answer.clj](answer.clj) |
| 3 | A read is done in the runtime, straight from the tables, with Rama's push, and the pushed value is no cache | [read_path.clj](../read_path.clj): it reads the gates' tables with a select or a push, shares one push among those who look at the same thing, and holds no value of its own |
| 4 | Only programs on machines that Sid runs may reach Rama's ports | The read path is trusted with the whole store: Rama sends it whole rows, and it hands a caller only what she may see and asked for |
| 5 | The tables keep a value plain | The read path reads a value from its row, and calls no query of the door's |
| 6 | A query is written as data and made of a few words; one more order for the keys that a lane marks | The words, in [query.clj](query.clj); "what points at a thing" reads the facts by value, in [sources.clj](sources.clj) |
| 7 | From a fact, a read can get who wrote it, and under which permission | A place-answer holds either only when the query asked, in [answer.clj](answer.clj) |
| 8 | The base's second is fine | A row of the base is read straight, as a row of a streaming lane is |
| 9 | The other sheet comes after this build | The read path keeps no record of who read what, and nothing of a read |

The rest by pointer: the store's keys and their namespace (8 and 9 October),
the depot that a lane starts with (7 October), and the form of an id and of a
value (6 October) are read through the functions that the files above require.

One decision of 13 October 2026, entry 4 of that day in the store source
entry, says where the base is read. In Sid's words: "4. The base's rows: a copy
on every task. One task decides, in the line's order." The reason that he was
given for the copy: everyone reads the base, and with all its rows on one task
every read of the base would run on one thread and share Rama's bound on the
pushes of one table on one task. So the read path reads a microbatch lane on
the task of the read's home, and [sources.clj](sources.clj) is where that
shows: "Where a microbatch lane is read", below.

## Engineering choices

Each is the builder's, with its reason. None is a decision of Sid's, and each is
provisional. The reasons in full are in the docstrings that the lines name.

**Where the read path sits.** [read_path.clj](../read_path.clj) is at the top
of the tree, as [door.clj](../door.clj) is, and its pure parts are this folder,
so the alias that holds Clojure alone stays the guard that they need nothing
else.

**Once or live is the call, not a part of the query.** One query can be asked
both ways, and two askers of one thing hold equal data.

**A source is what is shared.** One row, or one run of neighbouring rows, of one
table on one task, as a map. Two reads share a push when their sources are
equal. So two askers whose lists differ share the push of each streaming lane
that both lists hold, and of a microbatch lane when their reads have the same
home; and askers who asked for different parts of a row share too. A place is
not served from an open run that holds it: they are two sources.

**One call for each table under each routing id, asked once.** Every missing
source of one table under one routing id goes in one call. A lane's rights and
its fact are one round trip; a place through three lanes is three calls in one
round, and one round more when a permission of the asker's there was given her,
for the rows above it. What a read reads of the base is one call more, to the
task of the read's home.

**A read settles in a bounded number of rounds.** Each source is wanted on the
values of the sources before it: a lane's own rows, then her permissions, then
the rows above them, then its facts, then the checks; and the lanes of a start
lane's list only after the start lane. So a read asked once settles within 6
rounds, whatever is written meanwhile, and a live read opens within 8 on tables
that do not change. [sources.clj](sources.clj) derives the numbers, and each
loop's bound is set from them. A step of an open read that still wants more
at the live bound ends the read, with a reason of its own, and does not wait
on sources that it never joined.

**A read asked once reads a lane's fact with its rights.** So the runtime can
read one row, or the first places of a run, of a lane that the asker may then
not see. They are dropped and never handed over. It halves the round trips of
the commonest read. One predicate in [sources.clj](sources.clj) holds the
choice; the other answer is that predicate giving false.

**A lane's own rows are one run.** Its settings (`setting-keys` of
[declaration.clj](../door/declaration.clj)) sit on the lane's own entity under
the store's namespace, so one read, or one push, gives them all.

**Her permission is followed only under "members".** Under "everyone" it shapes
nothing; when the setting goes to "members" under an open read, the push on her
permission is opened then.

**The permissions above hers are followed with it.** Her permission stands only
while each permission above it stands, and the end of one above her is written
nowhere but at its own place ([chain.clj](../gate/chain.clj)). So a read holds
the row of each holder above her, one push for each, which her own row names all
at once: none when she is the lane's first holder, one when she is straight
beneath her. Each row above is read after her own row, under a source that
names her row's stamp: Rama tells each push by itself, so a read that took a
row above from a push opened before her row was written could pair her new
row with a row above as it was before the same act ended it, and see a
permission stand that stood at no stamp. A row above that is read after hers
shows its place as it was at her row's stamp or later, so a permission that
the rows say stands, stood then. Askers so share the push on a row above
only when their own rows were written by one decision.
A read that names its run holds the row of that run's permission too, on the
session, and the rows above it: she sees the lane when either stands, and a
run's permission lets her see only when it names her as its holder.
When her permission ends, or one above it, her read leaves the lane's facts at
once and her answer says that the lane is left out, as when a setting goes to
"members" for an asker with no permission. The read keeps her row and the rows
above it: they are what could let the lane in again.

**A lane's acts are read by stamp, once, in their own rounds.** The last stamp
first, then her rights, then the line, then the things; and for an asker who may
not see the lane, her rows by who and her line rows in the place of the line.
Only decisions up to that last stamp are given, so the rights that judge them
were read after each of them was decided, and a row that is not there up to it
is a fault and no race. An asker outside reads no row of the lane that is not
hers, and a holder's page of kept things is one window. A taken act is held to
the same rule as a refused one. The read is not kept live: whether it should be
is Sid's. [acts.clj](acts.clj) says each round and why.

**The three steps of a run.** The first places in the lane's own call; when
that walk is full, the size by a bounded walk on the task; then the run whole. A
run that holds more places than one read may ends in a word. No walk on a task
is longer than the build's number of places, whatever a tool asks.

**The forms of path.** One function of [read_path.clj](../read_path.clj)
makes every path that Rama is given. A path that Rama cannot serve stopped a
worker of the in-process cluster in a probe of this build's plan, so the read
path sends no other form, and no caller gives one.

**Letters for which Rama's order and the table's can differ.** The table keeps
its keys by code point. Java's order of Strings agrees with that for every key
but one that holds a letter from U+E000 on, or one outside the first plane. For
such keys three things were seen on the store's own table. One was run by
itself: the map that Rama's client makes of a run missed a key that it holds.
Two were seen and not explained: a range asked by hand gave a key that the
order by code point puts after its end; and in two runs of the proof a run
between two such ends gave fewer places than the table holds, where a probe
that asked the same ends by hand got them all. So the read path rests on none
of it. A run whose end holds such a letter has no first read. A lookup that
misses such a key goes through the run. Each key of such a run is held to the
run's two ends before it is given. And a run between two ends that a caller
names is turned away when an end holds such a letter. A window of a read of
acts whose end holds such a letter is a walk of its count from its first end,
with no cut, and each key of it is held to the two ends: a lane and a who are
each such an id. An entity whose id holds
such a letter is read as an entity, whose run cannot lose a place of its own.
[sources.clj](sources.clj) holds the test and its three uses; the rule of the
check is in [query.clj](query.clj).

**What Rama sends at a change.** For a push on a run, a diff that names the
changed key; for a push on one row, the row's new value; and a whole value
anew at the first callback of a push, and when Rama left a reader out of a
change. The read path takes the three the same way: among the keys that the
diff names, or among every key of the value before and after when it names
none, it tells the places whose rows differ. That each write is told by a diff
of its own rests on what a row holds: a push on rows that hold a byte array was
sent the whole value anew about once a second, so a fact's row holds its value
as a String ([tables.clj](../record/tables.clj) says what was run).

**No value is kept between two changes; of what was told, each lane's
standing.** At a change the read path works out what the asker is told from the
values that the pushes hold at that moment. It compares no place with what she
was told before. Three tests let a step say nothing of a place: the row is the
same; a lane's own rows mean the same for this query; a nearer lane that she
may see holds a row at the place, in a source that the read still holds.
Everything else is told, so a place can be told a second time with the answer
that it already had. A read holds one mark: true while the whole answer is owed;
else what the last whole answer said of each lane, seen or left out and why,
and the lanes that wait. It is no value of the tables. It is there for the
promise that an asker who may not see a lane is told nothing of it, not even
an answer that says what it said: a change of her rights in a lane that she
was told she may not see, and may not see after it, tells her nothing, and one
that leaves her standing not known waits for the rows that say it. Without the
mark a step cannot tell such a change from one that took the lane from her:
two changes that are taken before either is handled each read the other's new
value. The argument, and the orders of callbacks that were played for it with
no cluster, are in the namespace docstring of [answer.clj](answer.clj) and in
the proofs' map. Keeping each lane's standing is a session's choice of 22
November 2026, for Sid to turn.

**The whole answer anew when what shapes a read changes.** A setting, a
permission, the list of the start lane or a mark that changes gives the asker
a whole new answer, and not the places one by one. Such changes are rare, and
one form fewer reaches a caller.

**The asker's function runs under the read's turn.** Its first call is made by
`read-live` itself, on the caller's thread, with the whole answer. Every later
call is on a thread of Rama's client. A function that does not return at once
holds back the reads of others; this was run for the plan's check, and the
namespace docstring of [read_path.clj](../read_path.clj) says what was seen. A
read asked from inside a function is turned away with a word of its own.

**Where a microbatch lane is read.** A streaming lane has every row on its
own task. A microbatch lane, which today is the base, has every row of its
facts and of its facts by value on every task, the same rows everywhere: its
gate decides an act on the lane's task and writes the rows of an accept on
each task. So a read of it needs one task, and any one will do. The read path
takes the task of the read's home: the lane that the read starts from, which
is `:from-lane`, or the first lane of `:lanes`. Every source of a microbatch
lane in a read has the home as its routing id: the lane's own rows, a
permission, a place, a check of it, a run of one entity or across entities, and
a run of the facts by value. Each is one source, and a run comes whole from the
one task. So an asker reads the base where her own lane lives, and a read that
starts from the base reads it on the base's own task.

What follows from it. Two askers whose reads have the same home share their
pushes on the base; two with two homes do not, and each holds a push of its
own on the task of its home. The tasks show one batch of the base at instants
that differ, by Rama's own page on microbatch topologies; so two askers with
two homes can be told of one act at two instants, and a read asked once through
her own lane, straight after the maker of an act of the base has its answer,
can still give the base as it was before that act. That is read in Rama's
page; the read path's proof on the cluster counts how often it sees it, and
holds nothing to the count. The read path does not test, at a read, that the
task of the home holds the rows: it rests on the gate's copy.

**The numbers.** Seven that an opener can give, and two more, each kept and not
decided: 256 places in the first read of a run; 20,000 places in one run; 40,000
checks of nearer lanes in one read that is asked once, and 250 in one that is
kept live; 900 pushes held on one table under one routing id; 2,000 paths in one
call; a wait of 10 seconds; 65 lanes in one read; and the rounds before a read
that does not settle ends in a word: 6 for a read asked once, which the chains
of its sources give, and 16 for a live read, the 8 that they give and 8 more for
writes while it opens. None bounds the bytes of a run.

**The read path bounds its own pushes.** Rama bounds the pushes on one table on
one task, by a dynamic option of its own whose default is 1,000, and it refuses
one opening at a time: a read that asked for about 10,000 was turned away only
after seconds. So the read path counts the pushes that it holds on each table
under each routing id, and turns a live read away at once, before it opens one
of the pushes that would pass 900, with a word of its own; an open read that
comes to need such a push ends. And one live read of what points at a thing may
need 250 checks of nearer lanes, each of them a push. A read asked once opens
no push and is held to neither. The count is by routing id and not by task,
because Rama's client does not say which task a routing id is on; and it is of
one process. In a streaming lane the routing id is the lane, so one count
holds every push of every asker through that lane. In the base the routing id
is the home of the read, so one count holds the pushes on the base of the
askers of one home, and the 900 bounds those: it does not bound what all the
homes of one task hold on the base together, which is what Rama's own bound
counts there. Each home that reads the base live holds a push of its own on
the base's own rows and one at least on a fact of it, so Rama's bound holds
the homes of one task that read the base live to half its number at most
(derived, not run). What that leaves to Rama's own bound, and that the read path sets
none of Rama's options, is in the namespace docstring of
[read_path.clj](../read_path.clj). Not decided here: whether Rama's option is
raised, or the checks are made with fewer pushes.

**A read asked once reads the tables.** It makes its own calls also where a live
read holds a push open on the same source, and takes no value of a push. A push
holds nothing that the tables do not hold, and it can be behind them by as long
as the push is late; so a read asked once that took its value could give a
caller the state before her own act, straight after that act's ack. It costs the
round trip that a read asked once costs when no push is open.

## Not decided, kept for now

None of these is decided here. Each is built to the kept or the working answer,
at one function, so that the other answer is a small change.

| The thing | Built as | Where the other answer would be made |
|---|---|---|
| What a read says of a lane that it left out | The lane, and one of two words for why: she may not see it, or it is not made | One function of [lanes.clj](lanes.clj) |
| How far a read walks from a lane | The one list: the start lane, then the lanes of its own fact, and no further. A start lane that she may not see gives itself alone | One function of [lanes.clj](lanes.clj) |
| The store's own facts in a tool's read | Left out unless the query asks for them; any asker who may see the lane may ask | One function of [answer.clj](answer.clj) |
| "What points at a thing" through a list of lanes | At a place that an order by value gives, the nearest lane that holds a row wins, and the place is given when the winner's value is the thing. A place is found only through an order | One function of [sources.clj](sources.clj): left out, the runs are joined |
| One moment for all the lanes of one read | None: the lanes are read at moments that differ | Nowhere here: it would be a query of the store |
| What a lane starts with | A lane with no setting fact reads as [first_act.clj](../gate/first_act.clj) says | That function; [lanes.clj](lanes.clj) calls it here |
| One permission for each holder; who may write a setting, a list or a mark | No code here: the read path reads whether the permission at her place stands, and what it allows is no part of that | — |
| Who asks | What the caller says | The check of a query; sign-in would stand in front of the read path |

## Words a session made, Sid's to change

Each says what the thing is. None is from the rig's code, the server's or
Inland's. "The read path", "asker" and "caller" are the build contract's words;
they are a session's too.

**Namespaces.** `softland.store.read-path`, and under it `query`, `lanes`,
`sources`, `answer`, `acts` and `words`; the two proofs,
`softland.store.read-path-pure-proof` and `softland.store.read-path-proof`; the
measures, `softland.store.read-measures`; and the run of piece 3's statements
over the store, `softland.store.line-statements`.

**Calls.**

| Word | What it is |
|---|---|
| `open` | Opens the read path on a cluster handle and the module's name |
| `read-once` | Reads once: a query, and its answer |
| `read-live` | Reads, and keeps the read live: a query and a function |
| `leave` | Says that an asker left, by the handle that `read-live` gave |
| `open-count` | How many live reads are open, and how many pushes they hold. Its two numbers are `:live-reads` and `:pushes` |

**A query.**

| Word | What it is |
|---|---|
| `:asks`, with `:place`, `:entity`, `:run`, `:points-at`, `:acts` | What is asked, and its five words |
| `:first`, `:last` | The two ends of a lane, as ends of a run |
| `:asker`, `:not-signed-in` | Who asks |
| `:session` | The run that the read is asked in, as an act names its `:session`. With an asker only. The asker then sees a lane that is open to its members under her permission for the lane, or under that run's when it names her as its holder; another holder's permission for a run of that id gives her nothing, and the read reads nothing above it |
| `:lanes`, `:from-lane` | Through which lanes; the lane to take the list from |
| `:with`, with `:writer`, `:permission` and `:time` | The parts of a row that are asked for |
| `:stores-own-facts` | Whether the store's own facts are asked for |

**The words with which the check turns a thing away**, each named for what is
wrong: `:not-a-query`, `:part-not-known`, `:asks-no-word-known`,
`:place-not-named`, `:entity-not-named`, `:run-not-named`,
`:points-at-not-named`, `:asker-is-no-id`, `:no-one-asker`,
`:session-is-no-id`, `:run-with-no-asker`, `:acts-not-named`,
`:lanes-not-a-list-of-lanes`, `:from-lane-is-no-lane`, `:acts-not-of-one-lane`,
`:lanes-not-named-one-way`, `:too-many-lanes`, `:part-of-a-row-not-known`,
`:id-over-the-limit`, `:run-ends-out-of-order`, `:run-end-not-ordered`,
`:store-key-not-asked-for`. The rule names are in
[query.clj](query.clj).

**What a call gives.**

| Word | What it is |
|---|---|
| `:now`, `:leave-with` | What `read-live` gives: the answer now, and the handle |
| `:anew`, `:changed`, `:ended` | What the asker's function is given: a whole answer, the changed places, the end of the read |
| `:left`, `:not-open` | What `leave` gives |
| `:lanes`, `:left-out`, with `:lane`, `:why`, `:may-not-see`, `:not-made` | The lanes of an answer, and what it says of a lane that gave nothing |
| `:place`, `:places`, `:orders`, with `:level-since` and `:not-marked` | The parts of an answer |
| `:holds`, `:value`, `:lane`, `:fact`, `:stamp`, `:writer`, `:permission`, `:time` | The parts of a place-answer. `:no-fact`, `:nothing-here`, `:value` and `:not-a-value` under `:holds` are the record's own |
| `:could-not` | The name under which a result holds one of the read path's own words |

**The read path's ten words.** Each reads after "the read path could not":
`:find-the-module`, `:read-the-query`, `:reach-the-store`,
`:get-an-answer-within-its-wait`, `:read-inside-an-askers-function`,
`:serve-a-read-that-large`, `:keep-more-reads-live`, `:keep-the-read-live`,
`:find-the-line-whole`, `:keep-a-read-of-acts-live`. The
first is spelled as the
door's word for its own opening and is the read path's own.
[words.clj](words.clj) says what each is.

| Word | What it is |
|---|---|
| `:because`, with `:the-store-ended-a-push`, `:a-step-did-not-settle`, `:a-run-grew-too-large`, `:a-read-it-came-to-need-failed`, `:no-room-for-a-push-it-came-to-need`, `:its-function-threw` | Why a live read ended |
| `:read-after` | In a source of a row above a permission of the asker's: the stamp of her row that names it, so that the row is read after hers |
| `:why`, `:at`, `:what`, `:most`, `:waited-ms`, `:module`, `:threw`, `:said` | What a word carries |
| `:places-of-one-run`, `:checks-of-one-read`, `:pushes-under-one-routing-id` | What was too many: the first two for one read, the third for the read path |

**Words of the explanations and of the code.**

| Word | What it is |
|---|---|
| a source, with `:gate`, `:table`, `:lane`, `:routing-id`, `:path` | One value that Rama gives and can push, as data. `:facts` and `:facts-by-value` name the table; `:row`, `:own-rows` and `:run` the kind of path |
| a routing id | What Rama's client is given as its partitioning key. The record's own word, so that "key" stays a fact's key |
| the home of a read | The lane that a read starts from: `:from-lane`, or the first lane of `:lanes`. It is the routing id of each source of a microbatch lane in that read |
| the lane's own rows | The rows of a lane's own entity under the store's keys: its depot, who may see it, what it builds on, its marked keys |
| standing, with `:yes`, `:not-known-yet`, `:may-not-see`, `:not-made` | Whether an asker may see a lane, as the read path knows it now. The gates use the same word for a permission that stands; whether it stays here is Sid's |
| a check | The read of a nearer lane's row at a place that a farther lane's order by value gives |
| the first read of a run; the size of a run | The two bounded reads that come before a run is read whole |
| rank; round | The order in which sources can be wanted; one turn of fetching what is missing |
| the read's turn | What lets one call of an asker's function run at a time for one read. Never called a lock: a lock is an encryption key in this tree |
| a read's mark | What a live read holds between two changes: that the whole answer is owed; or what its asker was last told of each lane, and which lanes wait |
| a lane that waits | One that she was told she may not see, whose standing a change of her rights has left not known yet, until its rows are given |
| `:run-start-places`, `:max-places-of-one-run`, `:max-checks-of-one-read`, `:max-checks-of-one-live-read`, `:max-pushes-under-one-routing-id`, `:max-paths-in-one-call`, `:wait-ms`, `:max-acts-of-one-read`, `:max-thing-weight-of-one-read` | The nine numbers that an opener can give |
| `:acts`, `:next`, `:last-stamp`, `:thing`, `:no-thing` with `:heavier-than-the-read`; `:stamp`, `:act-id`, `:answer` | The parts of an answer of a read of acts, and of a decision given; `:too-heavy` and `:not-weighed` under `:no-thing` are the gate's |
| a holder, a visitor, outside | What an asker is in a lane, for a read of its acts: her permission there stands; she may see the lane, which is open to everyone, and is no holder; she may not see it, or it is not made |
| withheld | A decision given as its stamp alone |

**Words inside the code**, which no caller is handed. Each is a session's, as the
others are.

| Word | What it is |
|---|---|
| `:once`, `:live` | How a read is asked, as the code that fetches tells the code that says what is wanted |
| `:lanes`, `:list-known?`, `:standing`, `:by-lane`, `:wanted`, `:missing`, `:too-large`, `:digest` | What "what a read wants now" gives: the lanes; whether the list of a start lane can be said yet; each lane's standing; what is read in each lane; every source that is wanted; those that are not read yet; whether the checks are more than one read may have; the digest of the thing asked about |
| `:own`, `:permission`, `:run-permission`, `:chain`, `:facts`, `:since`, `:checks` | What is read in one lane: the source of its own rows, of the asker's permission, of the permission of the run that she asks in, of the row of each permission above those two, its fact sources, the stamp of the asked key's mark, and the check wanted at each place |
| `:join`, `:leave`; `:joined`, `:left` | The sources that a live read is to hold and to let go; and those that one step did |
| `:tell`, `:owed?` | What a step gives back: what to hand the asker's function, and the read's mark after it |
| `:told`, `:waiting` | The two parts of a read's mark when the whole answer is not owed: each lane's standing as the last whole answer said it, and the lanes that wait |
| `:walk`, `:places`, `:holds?`, `:gives`; `:given`, `:none`, `:waits` | How the rows of one word of a query are put together; and the three outcomes of the walk of one place |
| `:made?`, `:who-may-see`, `:permission` with `:stands`, `:none`, `:not-known-yet`, `:builds-on`, `:marked-since` | What a lane's own rows, an asker's permission row and the rows above it mean for one query |
| `:pushes`, with `:opening`, `:readers`, `:called` | What the read path holds for each push: the one call that opens it, the live reads that hold it, and a mark that its first callback came |
| `:counts` | How many pushes the read path holds on each table under each routing id: what its own bound is held to |
| `:joined`, `:past-the-bound` | What the joining of sources gives: the read holds them; or they would take a count past the read path's bound, and it holds none of them |
| `:reads`, with `:query`, `:tell`, `:turn`, `:sources`, `:opened?`, `:owed?` | What the read path holds for each live read: its query, its function, its turn, the sources that it holds, whether it is opened, and its mark |
| `:whole`, `:first`, `:count`, `:size` | The step of a read for which a path is made: the row or the run itself; the first read of a run; its count; its size |
| `:ok`, `:refuse` | What the check of a query gives: the query in its one form, or the word with which it is turned away, beside `:at`. The form is that of the act's check |
| `:put`, `:out`, `:all` | What a diff names: a key that was put, a key that went out, or no key |
| `:own-close`, `:ended-by-the-store`, `:anew`, `:change` | The kind of a diff: the last call of a push that the read path closed; Rama ending a push; a whole value sent anew; a change |
| `:push`, `:word`; `:late`, `:no-path`; `:returned`, `:threw` | What an opening ended in; two outcomes of a wait and of a call that was not made; and whether an asker's function returned or threw |
| `:lanes-within-the-limit`, `:names-one-asker`, `:names-its-lanes-one-way`, `:ids-within-the-limit`, `:run-ends-in-order`, `:run-ends-are-ordered`, `:points-at-names-a-thing`, `:store-key-is-asked-for` | The eight rules of a query, each named for what it holds |
| `:run-end` | The shape of one end of a run, in the query's map of shapes |
| `:from`, with `:open`, `:read-once`, `:read-live`, `:the-askers-function`; `:says` | In the table of the ten words: which call gives a word, and what it says |
| `:shape`, beside `:reason` | In the table of the five words of a query: the shape that `:asks` must have under that word, and the word when it does not fit |
| `:line`, `:things`, `:task`; `:window`, `:last-stamp`, `:by-who` | The three tables that a read of acts reads beside the facts, and the three kinds of path of its sources |
| `:holder`, `:visitor`, `:outside` | The kinds of an asker in a read of acts (`asker-kind`) |
| `:take`, `:bridge`, `:break` | What a stamp is to a run of neighbouring stamps that one window reads: a row to read, a stamp with no row that a run may go across, and one whose row must not be read |
| `:fetch?`, `:whole?`, `:fault` | Inside a read of acts: a thing to fetch; a decision given whole; the first stamp at which the line is not whole |
| `:too-many` | What the checks of nearer lanes give in the place of the checks, past the build's number |
| `:known`, `:reading` | "Known": each source that is read, with its value. "Reading": what a read wants now. Also two names in a small map inside the opening of a live read |
| `:any-lane` | A lane id that names no lane, with which the order of a run's two ends is asked: the order is the same in every lane |
