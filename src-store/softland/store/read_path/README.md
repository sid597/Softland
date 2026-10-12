# Read path — the read path's pure parts, and what a query is

[Parent: store](../README.md) · [Store source entry](../../../README.md).

The read path is [read_path.clj](../read_path.clj), one folder up, beside
[door.clj](../door.clj) and [module.clj](../module.clj): the one piece of code by
which a fact comes back out of the store to a caller in the runtime's process,
once or kept live. It is the only file of the read path that requires Rama, and
the only file of this tree that opens a table to read a fact. This folder holds
the rules that it stands on.

All of this folder is pure code and loads with Clojure alone.

| File | Responsibility | State and lifetime |
|---|---|---|
| [query.clj](query.clj) | A query written as data: its one form, declared as shapes and rules in `act.clj`'s language; the four words that can be asked; the check; the read path's numbers. | None. |
| [layers.clj](layers.clj) | The layers of a read: which layers, in what order; whether a layer is made; whether an asker may see it; what a read says of a layer that it left out; what a layer's own rows mean for one query. | None. |
| [sources.clj](sources.clj) | The sources of a read, as data: each row or run that the read path asks of Rama, with its table and its routing id; which of them a read wants next; the calls of one round; the size rules of a run; which keys and places a change names. | None. |
| [answer.clj](answer.clj) | What a read gives: the layers put together, the nearest that holds a fact first; a place-answer; and what a change tells a live asker. | None. |
| [words.clj](words.clj) | Everything that the read path says in place of an answer, as data: its own seven words, why a live read ended, why a layer gave nothing. | None. |

## How the files relate

[words.clj](words.clj) requires nothing. [layers.clj](layers.clj) reads rows of
the facts table with [rows.clj](../record/rows.clj), and asks
[first_act.clj](../gate/first_act.clj) what a layer starts with.
[sources.clj](sources.clj) requires the layers, and takes every text of a key
from [rows.clj](../record/rows.clj): no key is made in this folder.
[query.clj](query.clj) requires [the act](../act.clj), for the engine that reads
a shape; the door's [declaration](../door/declaration.clj), for the tests of an
id and the store keys; [rows.clj](../record/rows.clj), for the order of a run's
two ends; and the sources, for the one test of a letter that a range of Rama's
does not order as the table does. [answer.clj](answer.clj) requires the layers
and the sources.

[read_path.clj](../read_path.clj) requires the query, the sources, the answer
and the words. It holds no rule of a read. It fetches what the sources say is
missing, and hands the answer what it fetched; at a change of a push it does
what two pure functions of [answer.clj](answer.clj) say. So the
[pure proof](../../../../test-store/softland/store/README.md) of the read path
can play those two functions in every order of the callbacks, with no cluster.

A word of a query is declared in three maps that are keyed by the word: what it
is and its shape, in [query.clj](query.clj); what it reads in one layer, in
[sources.clj](sources.clj); how its rows are put together, in
[answer.clj](answer.clj). A word that does not exist yet is one entry in each.

## Boundaries

Nothing in this folder calls Rama, and nothing here builds a path of Rama's: a
source is data, and [read_path.clj](../read_path.clj) turns it into one of five
forms of path. Nothing here writes: the read path appends to no depot, and no
read is kept. Nothing here holds state: who looks at what, and the pushes, are
the memory of [read_path.clj](../read_path.clj).

The read path is not sign-in, not a network edge, not a view or a screen, and
not a record of who read what. It is in-process Clojure, and in this build no
asker uses it but its proof.

## Decisions this build stands on

Sid's decisions are in [the store source entry](../../../README.md), with his
words. The read path stands on the section of 11 and 12 October 2026, by entry:

| Entry | Decision | Where it shows in the read path |
|---|---|---|
| 1 | A layer has a setting, "members" or "everyone"; a permission says what it allows | The standing of an asker in a layer, in [layers.clj](layers.clj). Any permission that is there allows seeing; what it allows beyond that is not read here |
| 2 | A layer says which layers it builds on; a view may give another list; the nearest layer that holds a fact wins, also with "nothing here" | The order of the layers, in [layers.clj](layers.clj); the walk of a place, in [answer.clj](answer.clj) |
| 3 | A read is done in the runtime, straight from the tables, with Rama's push, and the pushed value is no cache | [read_path.clj](../read_path.clj): it reads the gates' tables with a select or a push, shares one push among those who look at the same thing, and holds no value of its own |
| 4 | Only programs on machines that Sid runs may reach Rama's ports | The read path is trusted with the whole store: Rama sends it whole rows, and it hands a caller only what she may see and asked for |
| 6 | A query is written as data and made of a few words; one more order for the keys that a layer marks | The four words, in [query.clj](query.clj); "what points at a thing" reads the facts by value, in [sources.clj](sources.clj) |
| 7 | From a fact, a read can get who wrote it, and under which permission | A place-answer holds either only when the query asked, in [answer.clj](answer.clj) |
| 8 | The base's second is fine | A row of the base is read straight, as a row of a streaming layer is |

The rest by pointer: the store's keys and their namespace (8 and 9 October),
the depot that a layer starts with (7 October), and the form of an id and of a
value (6 October) are read through the functions that the files above require.

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
equal. So two askers whose lists differ share the push of each layer that both
lists hold, and askers who asked for different parts of a row share too. A place
is not served from an open run that holds it: they are two sources.

**One call for each task, asked once.** Every missing source of one task goes in
one call. A layer's rights and its fact are one round trip; a place through
three layers is three calls in one round.

**A read asked once reads a layer's fact with its rights.** So the runtime can
read one row, or the first places of a run, of a layer that the asker may then
not see. They are dropped and never handed over. It halves the round trips of
the commonest read. One predicate in [sources.clj](sources.clj) holds the
choice; the other answer is that predicate giving false.

**A layer's own rows are one run.** Its depot, who may see it, what it builds on
and its marked keys sit on the layer's own entity under the store's namespace,
so one read, or one push, gives the four.

**Her permission is followed only under "members".** Under "everyone" it shapes
nothing; when the setting goes to "members" under an open read, the push on her
permission is opened then.

**The three steps of a run.** The first places in the layer's own call; when
that walk is full, the size by a bounded walk on the task; then the run whole. A
run that holds more places than one read may ends in a word. No walk on a task
is longer than the build's number of places, whatever a tool asks.

**The five forms of path.** One function of [read_path.clj](../read_path.clj)
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
names is turned away when an end holds such a letter. An entity whose id holds
such a letter is read as an entity, whose run cannot lose a place of its own.
[sources.clj](sources.clj) holds the test and its three uses; the rule of the
check is in [query.clj](query.clj).

**What Rama sends at a change.** For a push on the facts by value, a diff that
names the changed key. For a push on the facts table, whose rows hold a
value's bytes, the whole value anew, at each change: so for a run of the facts
table Rama sends the whole run again when one place of it changes. Both were
run on the store's module, and a probe of this build with one thing changed
showed that the bytes in a row are what makes the difference. The read path
takes both the same way: it compares the value before with the value after,
row by row, and tells the places that differ.

**No value is kept between two changes.** At a change the read path works out
what the asker is told from the values that the pushes hold at that moment. It
does not compare with what she was told before, because it does not keep that.
Three tests let a step say nothing of a place: the row is the same; a layer's
own rows mean the same for this query; a nearer layer that she may see holds a
row at the place, in a source that the read still holds. Everything else is
told, so a place can be told a second time with the answer that it already had.
A read holds one mark, "the whole answer is owed", which is true or false and
no value of the tables. The argument, and the orders of callbacks that were
played for it with no cluster, are in the namespace docstring of
[answer.clj](answer.clj) and in the proofs' map.

**The whole answer anew when what shapes a read changes.** A setting, a
permission, the list of the start layer or a mark that changes gives the asker
a whole new answer, and not the places one by one. Such changes are rare, and
one form fewer reaches a caller.

**The asker's function runs under the read's turn.** Its first call is made by
`read-live` itself, on the caller's thread, with the whole answer. Every later
call is on a thread of Rama's client. A function that does not return at once
holds back the reads of others; this was run for the plan's check, and the
namespace docstring of [read_path.clj](../read_path.clj) says what was seen. A
read asked from inside a function is turned away with a word of its own.

**The task of a row in a microbatch layer.** A run across entities of a
microbatch layer has one part on each task. The read path names each task with
one routing id that a class of Rama's own gives. Rama's pages show that class
for tests; whether it is meant for a client is a question for Rama's team.

**The numbers.** Five that an opener can give, and two more, each kept and not
decided: 256 places in the first read of a run; 20,000 places in one run; 40,000
checks of nearer layers in one read; 2,000 paths in one call; a wait of 10
seconds; 65 layers in one read; 8 rounds before a read that does not settle
ends in a word. None bounds the bytes of a run.

**How many pushes Rama opens.** Rama bounds the pushes on one table on one task
by a setting of its own. As run on the store's module, in-process, with the
setting as Rama ships it: 1,000 opened, the next was refused, and a closed push
made room for one more. A streaming layer is whole on one task, so every live
read through it counts toward the one bound. The read path sets none of Rama's
settings and counts no push against the bound: an opening that Rama refuses
ends in `:reach-the-store`, the read gives that word, and every push that it
joined is left. So the count of pushes of a live read of what points at a thing
through layers, one for each place of a farther order and each nearer layer, is
bounded by Rama long before the read path's own number of 40,000 checks. Not
decided here: whether the bound is raised, or fewer pushes are held.

**A read asked once takes the value of an open push.** Where a live read holds a
push on a source, a read asked once is given the value that the push holds, and
makes no call. That value holds nothing that the tables do not hold, and it can
be behind them by as long as the push is late: in the proof a read asked once
straight after the acks of 200 acts gave about 190 places, where a read path that held
no push gave 200. One function of [read_path.clj](../read_path.clj) takes those
values; with it giving nothing, every read asked once reads the tables.

## Not decided, kept for now

None of these is decided here. Each is built to the kept or the working answer,
at one function whose docstring says that it is not decided, so that the other
answer is a small change.

| The thing | Built as | Where the other answer would be made |
|---|---|---|
| What a read says of a layer that it left out | The layer, and one of two words for why: she may not see it, or it is not made | One function of [layers.clj](layers.clj) |
| How far a read walks from a layer | The one list: the start layer, then the layers of its own fact, and no further. A start layer that she may not see gives itself alone | One function of [layers.clj](layers.clj) |
| The store's own facts in a tool's read | Left out unless the query asks for them; any asker who may see the layer may ask | One function of [answer.clj](answer.clj) |
| "What points at a thing" through a list of layers | At a place that an order by value gives, the nearest layer that holds a row wins, and the place is given when the winner's value is the thing. A place is found only through an order | One function of [sources.clj](sources.clj): left out, the runs are joined |
| One moment for all the layers of one read | None: the layers are read at moments that differ | Nowhere here: it would be a query of the store |
| What a layer starts with | A layer with no setting fact reads as [first_act.clj](../gate/first_act.clj) says | That function; one function of [layers.clj](layers.clj) is its one caller here |
| One permission for each holder; "see" stays "see"; who may write a setting, a list or a mark | No code here: the read path reads whether a permission row is there, and no part of it | — |
| Who asks | What the caller says | The check of a query; sign-in would stand in front of the read path |

## Words a session made, Sid's to change

Each says what the thing is. None is from the rig's code, the server's or
Inland's. "The read path", "asker" and "caller" are the build contract's words;
they are a session's too.

**Namespaces.** `softland.store.read-path`, and under it `query`, `layers`,
`sources`, `answer` and `words`; the two proofs,
`softland.store.read-path-pure-proof` and `softland.store.read-path-proof`; the
measures, `softland.store.read-measures`.

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
| `:asks`, with `:place`, `:entity`, `:run`, `:points-at` | What is asked, and its four words |
| `:first`, `:last` | The two ends of a layer, as ends of a run |
| `:asker`, `:not-signed-in` | Who asks |
| `:layers`, `:from-layer` | Through which layers; the layer to take the list from |
| `:with`, with `:writer` and `:permission` | The parts of a row that are asked for |
| `:stores-own-facts` | Whether the store's own facts are asked for |

**The words with which the check turns a thing away**, each named for what is
wrong: `:not-a-query`, `:part-not-known`, `:asks-no-word-known`,
`:place-not-named`, `:entity-not-named`, `:run-not-named`,
`:points-at-not-named`, `:asker-is-no-id`, `:no-one-asker`,
`:layers-not-a-list-of-layers`, `:from-layer-is-no-layer`,
`:layers-not-named-one-way`, `:too-many-layers`, `:part-of-a-row-not-known`,
`:id-over-the-limit`, `:run-ends-out-of-order`, `:run-end-not-ordered`,
`:store-key-not-asked-for`. The eight rule names are in
[query.clj](query.clj).

**What a call gives.**

| Word | What it is |
|---|---|
| `:now`, `:leave-with` | What `read-live` gives: the answer now, and the handle |
| `:anew`, `:changed`, `:ended` | What the asker's function is given: a whole answer, the changed places, the end of the read |
| `:left`, `:not-open` | What `leave` gives |
| `:layers`, `:left-out`, with `:layer`, `:why`, `:may-not-see`, `:not-made` | The layers of an answer, and what it says of a layer that gave nothing |
| `:place`, `:places`, `:orders`, with `:level-since` and `:not-marked` | The parts of an answer |
| `:holds`, `:value`, `:layer`, `:fact`, `:stamp`, `:writer`, `:permission` | The parts of a place-answer. `:no-fact`, `:nothing-here`, `:value` and `:not-a-value` under `:holds` are the record's own |
| `:could-not` | The name under which a result holds one of the read path's own words |

**The read path's seven words.** Each reads after "the read path could not":
`:find-the-module`, `:read-the-query`, `:reach-the-store`,
`:get-an-answer-within-its-wait`, `:read-inside-an-askers-function`,
`:serve-a-read-that-large`, `:keep-the-read-live`. The first is spelled as the
door's word for its own opening and is the read path's own.
[words.clj](words.clj) says what each is.

| Word | What it is |
|---|---|
| `:because`, with `:the-store-ended-a-push`, `:a-run-grew-too-large`, `:a-read-it-came-to-need-failed`, `:its-function-threw` | Why a live read ended |
| `:why`, `:at`, `:what`, `:most`, `:waited-ms`, `:module`, `:threw`, `:said` | What a word carries |
| `:places-of-one-run`, `:checks-of-one-read` | What was too large |

**Words of the explanations and of the code.**

| Word | What it is |
|---|---|
| a source, with `:gate`, `:table`, `:layer`, `:routing-id`, `:path`, `:every-task` | One value that Rama gives and can push, as data. `:facts` and `:facts-by-value` name the table; `:row`, `:own-rows` and `:run` the kind of path |
| a routing id | What Rama's client is given as its partitioning key. The record's own word, so that "key" stays a fact's key |
| the layer's own rows | The rows of a layer's own entity under the store's keys: its depot, who may see it, what it builds on, its marked keys |
| standing, with `:yes`, `:not-known-yet`, `:may-not-see`, `:not-made` | Whether an asker may see a layer, as the read path knows it now. The gates use the same word for a permission that stands; whether it stays here is Sid's |
| a check | The read of a nearer layer's row at a place that a farther layer's order by value gives |
| the first read of a run; the size of a run | The two bounded reads that come before a run is read whole |
| rank; round | The order in which sources can be wanted; one turn of fetching what is missing |
| the read's turn | What lets one call of an asker's function run at a time for one read. Never called a lock: a lock is an encryption key in this tree |
| the whole answer is owed | The one mark that a live read holds between two changes |
| `:run-start-places`, `:max-places-of-one-run`, `:max-checks-of-one-read`, `:max-paths-in-one-call`, `:wait-ms` | The five numbers that an opener can give |

**Words inside the code**, which no caller is handed. Each is a session's, as the
others are.

| Word | What it is |
|---|---|
| `:once`, `:live` | How a read is asked, as the code that fetches tells the code that says what is wanted |
| `:layers`, `:list-known?`, `:standing`, `:by-layer`, `:wanted`, `:missing`, `:too-large`, `:digest` | What "what a read wants now" gives: the layers; whether the list of a start layer can be said yet; each layer's standing; what is read in each layer; every source that is wanted; those that are not read yet; whether the checks are more than one read may have; the digest of the thing asked about |
| `:own`, `:permission`, `:facts`, `:since`, `:checks` | What is read in one layer: the source of its own rows, of the asker's permission, its fact sources, the stamp of the asked key's mark, and the check wanted at each place |
| `:join`, `:leave`; `:joined`, `:left` | The sources that a live read is to hold and to let go; and those that one step did |
| `:tell`, `:owed?` | What a step gives back: what to hand the asker's function, and whether the whole answer is owed after it |
| `:walk`, `:places`, `:holds?`, `:gives`; `:given`, `:none`, `:waits` | How the rows of one word of a query are put together; and the three outcomes of the walk of one place |
| `:made?`, `:who-may-see`, `:permission` with `:there`, `:none`, `:not-known-yet`, `:builds-on`, `:marked-since` | What a layer's own rows and an asker's permission row mean for one query |
| `:pushes`, with `:opening`, `:readers`, `:called` | What the read path holds for each push: the one call that opens it, the live reads that hold it, and a mark that its first callback came |
| `:reads`, with `:query`, `:tell`, `:turn`, `:sources`, `:opened?`, `:owed?`, `:task-routing-ids` | What the read path holds for each live read: its query, its function, its turn, the sources that it holds, whether it is opened, whether the whole answer is owed, and the routing ids of the tasks as they were when it opened |
| `:whole`, `:first`, `:count`, `:size` | The step of a read for which a path is made: the row or the run itself; the first read of a run; its count; its size |
| `:put`, `:out`, `:all` | What a diff names: a key that was put, a key that went out, or no key |
| `:own-close`, `:ended-by-the-store`, `:anew`, `:change` | The kind of a diff: the last call of a push that the read path closed; Rama ending a push; a whole value sent anew; a change |
| `:push`, `:word`; `:late`, `:no-path`; `:returned`, `:threw` | What an opening ended in; two outcomes of a wait and of a call that was not made; and whether an asker's function returned or threw |
| `:layers-within-the-limit`, `:names-one-asker`, `:names-its-layers-one-way`, `:ids-within-the-limit`, `:run-ends-in-order`, `:run-ends-are-ordered`, `:points-at-names-a-thing`, `:store-key-is-asked-for` | The eight rules of a query, each named for what it holds |
| `:run-end` | The shape of one end of a run, in the query's map of shapes |
| `:from`, with `:open`, `:read-once`, `:read-live`, `:the-askers-function`; `:says` | In the table of the seven words: which call gives a word, and what it says |
| `:shape`, beside `:reason` | In the table of the four words of a query: the shape that `:asks` must have under that word, and the word when it does not fit |
| `:too-many` | What the checks of nearer layers give in the place of the checks, past the build's number |
| `:known`, `:reading` | "Known": each source that is read, with its value. "Reading": what a read wants now. Also two names in a small map inside the opening of a live read |
| `:any-layer` | A layer id that names no layer, with which the order of a run's two ends is asked: the order is the same in every layer |
