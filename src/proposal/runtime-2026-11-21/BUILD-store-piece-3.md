# The store's line, piece 3: a layer's acts and refusals, kept and read by stamp

[The spec](store-piece-3/SPEC.md) ·
[its implicit spec](store-piece-3/IMPLICIT_SPEC.md) ·
[the decomposition](store-piece-3/DECOMPOSITION.json) ·
[the first stage's plan](store-piece-3/PLAN-gates-keep-the-line.md) and
[its check](store-piece-3/PLAN_VALIDATION-gates-keep-the-line.md) ·
[piece 2, repaired](BUILD-store-piece-2-repair.md) ·
the judges: [the statements of piece 3](store-piece-3/statements/README.md)
and [the generated histories](gate-histories/README.md)

**22 November 2026.** Two Claude sessions, one after the other. The session
named "f" built stage 1, committed it (`7f1242f3`), began stage 2, and ran out
of room in its context. The next session took the same job up on Sid's word,
"so a session named "f" ran out of tokens i want you to pick up the exact work
and take it to completion", and built stage 2, wrote stage 3, and made one
correction of stage 1. This page says what changed, each fork taken, each
command with what it printed, what is open, and each choice of a session's
for Sid to turn. A session does not judge its own build: a fresh session does
that, from the diff.

## What was asked

The job came to session f as one message that Sid pasted (`‹70bb746d›`),
which calls itself "my direct ask: this message is my go for the whole job".
By the record, a session wrote that message for him (`«fb6ef520»`, 07:09 UTC)
and he pasted it as written; none of its words is in a line that he typed. Its
ask: "build piece 3 of the store's line in `src-store`: a layer's acts and
refusals, kept by the gates and read by stamp", after piece 2's fix, in three
steps:

1. "The gates keep the line. Build the plan with every revision that its
   validation requires: one walk for the digest and the weight; arrays and
   big numbers weighed for what they hold; no unbounded work on the task for
   a refused thing; the seeks of each write and each read counted; the
   hazard of high code points in a who as in a layer."
2. "The read by stamp. No plan exists for it: build it from the spec's 'The
   read' and 'What must hold', and write its design into the build's page as
   you settle it: the tables, where each row sits, the seeks of each read."
3. "The read kept live. The spec leaves open whether it is offered. Say
   whether it should be, what it would cost, and how a caller learns of new
   decisions without it. Build none of it now: it is mine to turn."

And two "calls of 22 November", which the message names as "both defaults of
the plan's check and mine to turn". The words are the session's, and this
build takes them as his go, as the message says:

1. "A taken act is kept whole up to 32 MiB. A refused thing is kept whole up
   to 32 MiB from a sender with standing, and up to 256 KiB from a sender
   with none or refused at the declaration. The weighing stops at the bound
   plus one, and a thing that is too heavy keeps no exact weight."
2. "The row by who is kept, inside the task's table, as the validation
   proposes under c."

The judges are no session's of this build: the statements of
`store-piece-3/statements/`, written by another session from the spec alone
(`f04298ee`), and `gate-histories/`. Neither was changed, nor was the judges'
copy of the walk, `gate-histories/src/gate_histories/walk.clj`: the store's
walk adds the line through a function of its own beside `decision/writes`,
which keeps its seven arguments, so the copy follows the store with no change.

## Stage 1: the gates keep the line

Built and committed by session f (`7f1242f3`).

### What a gate keeps now, for each decision

On the layer's task, in the decision's one write, before the answer and the
last stamp, and behind check (a) as every write of a decision is
(`gate/decision.clj`, `line-writes`; `module/walk.clj`, step 7):

| Row | Table | Key | Holds |
|---|---|---|---|
| the line row | `line`, one PState of each gate | `"<layer> <stamp>"`, the stamp in 19 digits | the uuid of the act-id, the answer's word, time and digest (hex), the who that the thing names, and the thing's weight; or, for a thing that is not kept, `:not-kept` `:too-heavy` or `:not-weighed` and no weight |
| the thing | `things`, one PState of each gate | the same key | the thing as it arrived, when it is kept |
| the row by who | the field `:by-who` of the gate's table for a task, a subindexed map beside `:last-stamp` (call 2) | `"<layer> <who> <stamp>"` | the stamp; only when the thing names a usable who |

A thing is kept when its weight is a number not over its bound
(`decision/kept-bound`): 32 MiB (33,554,432) when its sender has standing,
256 KiB (262,144) otherwise. "A sender with standing", as the plan's check
defined it under its Option 2 and call 1 took it: the declaration took the
thing and the act's permission stands and is hers; so an accept, a refusal of
the places, a holder who may only see (`:permission-does-not-allow-acting`)
and a making act are kept up to 32 MiB, and every other refusal of the
standing, and every refusal of the declaration, up to 256 KiB.

The weight is the number of bytes of the UTF-8 of the thing's one text as it
arrived, the time on it too, counted in the walk that takes its digest
(`door/digest.clj`, `weighed`); for a part that the text does not write out
whole (a part that it writes as `#other` or `#too-deep`, and a decimal), what
the part holds is added (`door/weight.clj`). For a thing that the text writes
out whole, the weight is exactly the bytes of `(text/text thing)`: a pure
check holds it on nine things, and since stage 2 on four things more whose
time is no whole number.

A stray and a second thing under a used act-id write nothing of the line, as
they write no answer. The base's line, its things and its rows by who are on
the base's task alone: the walk gives the gate nothing of the line to copy.
Nothing that the line holds names a task or a position in a depot, so the
form with no task of the run-again proof holds it whole.

### Each revision of the plan's check, and how it is met

| The check asked | What was built |
|---|---|
| One walk for the digest and the weight (its items b, e; E3) | `text/feed` hands a third function each part whose text does not hold what it holds; `digest/weighed` feeds the hash and counts the bytes of each piece in the same walk. The walk takes the declaration's check first and then the one walk, so that the weighing knows its bound (`decision/weighing-bound`). `digest/digest` is unchanged, and the one walk gives its digest: a pure check holds the two equal. The time on the act, which the digest leaves out, is weighed after the walk, by its own text (stage 2's correction, below) |
| Arrays and big numbers weighed for what they hold (E1, and item e.1.6) | An array of a primitive weighs 1 and its length times the width of one item, with no walk; an array of objects, a Java collection, a map and a record weigh 1 and their parts; a big integer, a decimal and a ratio weigh their bits in whole bytes, read from `bitLength`, with no text and no strip. A run of bytes weighs its text, two hex digits for each byte (fork 2) |
| No unbounded work on the task for a refused thing (item b, E2) | The two bounds of call 1. The weighing stops as soon as the sum passes the bound: a thing that is too heavy keeps no weight. Every part weighs 1 at least, so a walk of the parts that the text does not write out ends within the bound, also for a part with no end (a record that holds an endless sequence is too heavy in 11 ms). A thing that the declaration refused is weighed up to 256 KiB. One cost that is not bounded is in the digest's own walk and older than this piece: open item 1 |
| The seeks of each write and each read counted (Partitioning) | Writes: no read is added to any kind of thing; one to three writes are (below, and `module/walk.clj`, "What one call costs"). Reads: stage 2 |
| The hazard of high code points in a who as in a layer (R20) | In the read: stage 2, where a window whose end holds a letter from U+D800 on is a walk of its count with no cut, its keys held to its ends, as for a layer; run on the cluster for a layer outside the first plane and a who from U+E000 on |
| The rows by who in the task's table (item c) | Call 2: `:by-who` in `record/tables.clj`, `task` |
| Road B and Q7: the microbatch gate could count its depot's offsets (item a) | The plan said that neither gate could; the check showed that the microbatch gate could, and the streaming gate not. The choice stands for both reasons that the check gives: a streaming gate cannot, and a client's read of a depot by position is a non-yielding read of a whole range. Said here, since this build writes no plan page |
| S4 cannot be met as written (item d) | Confirmed by the check; the rule of the new key stands right behind who may see's, and a pure check holds the one way in which S4 is broken: a thing with the key and a fault of a later rule gets that rule's word, where it was refused `:store-key-not-known` |

### What each decision costs more

Counted from the dataflow, as the walk counts; nothing measured on disk.

| What arrived | Reads, writes before | Reads, writes now |
|---|---|---|
| A stray | 0, one add to a set | the same |
| A thing under an act-id that has an answer | 1, 0 | the same |
| A thing that the declaration refuses | 3, 2 | 3, 3 to 5 |
| An act whose permission does not stand | 5 at most, 2 | 5 at most, 3 to 5 |
| A making act | 5, 4 | 5, 7 |
| One accepted act with one value under a plain key | 5, 3 | 5, 6 |
| The greatest call | 8,228, 771 | 8,228, 774 |

Beside the rows: the thing's bytes a second time when it is kept, written
and replicated in the decision's event, so a maker's `:ack` in a streaming
layer waits for them; at most 256 KiB of them for a sender with no standing.
The weight costs one sum for each piece of the digest's walk, and for a
thing that the declaration refused whose text does not write a part of it
out, a walk of that part up to 256 KiB and one. What the store keeps twice:
each thing that is kept, once in its depot and once in `things`; the answer's
fields, in the answer's row and in the line row; the who, in the line row
and in the key of its row by who.

## Stage 2: the read by stamp

### What was asked of it, and what it is

The spec's "The read": for one layer, the decisions from a stamp on, in the
layer's order, each with its stamp, its act-id, its answer as the read by
act-id gives it, and the thing whole as it arrived; bounded by count and by
bytes, saying where the next read begins; saying what it left out; with the
layer's last stamp. Built as a fifth word of the read path's query,
`[:acts start most]`, with one layer (`:from-layer`, or a list of one), read by
`read-once` (`src-store/softland/store/read_path/acts.clj`, and the dispatch
in `read_path.clj`). The spec's three questions for Phase 1, answered:

- **Where the thing whole comes from at a read:** from `things`, which stage 1
  keeps (the first of the spec's two roads). No position in a depot is read.
- **Whether the answers' table can serve the order by stamp:** no. It is keyed
  by the act's uuid, in no order of stamps; the line is keyed by stamp.
- **One more word of a query, or a call of its own:** one more word. The read
  path's check, its numbers, its words and its calls of Rama's client serve it
  unchanged, and a session's trail, the same shape of read, can be a sixth word
  with the same machinery.

### The tables, and where each row sits

Every row that the read reads is on the layer's own task, and each source has
the layer as its routing id (`acts.clj`, `source-of`).

| What the read takes | Table | Key, as `record/rows.clj` makes it | Where it sits |
|---|---|---|---|
| the layer's last stamp | the gate's table for a task, field `:last-stamp` | the layer's text | the layer's task |
| her rows by who | the same table, field `:by-who` | `"<layer> <who> <stamp>"` | the layer's task |
| a decision's line row | `line` | `"<layer> <stamp>"` | the layer's task |
| a decision's thing | `things` | the same key | the layer's task |
| the layer's own rows, her permission, her run's, the rows above them | `facts` | as a read of facts reads them (`read_path/sources.clj`) | a streaming layer's task; for the base, every task holds them, and the read takes them on the base's own task |

Why the base is read on its own task (spec, "What must hold" 5): its line, its
things, its last stamp and its rows by who are there alone, where its gate
decides, and the same task holds a copy of the base's facts that the same
batch writes. A read of the base's facts from another layer's home reads them
on the home's task, which can be a batch behind the base's line; a read of the
base's acts takes its rights from the same task as its line. The read path's
proof holds, on the cluster, that each call of a read of the base's acts goes
to the microbatch gate's tables under the base as its routing id.

### The rounds, in the order of what must hold

| Round | What is read | One call to |
|---|---|---|
| 1 | the layer's last stamp, L | the table for a task |
| 2 | the rights: the layer's own rows, her permission row, her run's permission row | the facts |
| 3 | the rows above her permission and her run's, when the rows of round 2 name them | the facts |
| 4 | for an asker who may see the layer: the window, the line from the start, `most` rows at most. For an asker outside it, signed in: her rows by who from the start, `most` at most | the line; or the table for a task |
| 5 | for an asker outside: her line rows at those stamps | the line |
| 6 | the things of the decisions given whole that fit in the bound | the things |

Only decisions up to L are given. A decision at L or before was decided before
the rights were read, so the rights that judge an answer are never older, in
the layer's line, than a decision that it gives whole on their ground: a
permission that ended at stamp s, or a setting that changed there, is read as
such by every read that gives s (the implicit spec's R12, R13). Since a line row
is written in the decision's one write with the last stamp, the window holds
a row for every stamp up to L: a row that is missing there is a fault, and the
read ends in the word `:find-the-line-whole` with that stamp, never with an
answer that stops at it. A decision that lands after L is read is left for the
next read: the pure proof plays one, landing between round 1 and round 4.

An asker outside the layer reads her rows by who only once the rights say so,
and then her line rows and her things: she reads no row of the layer that is
not hers, so what her read costs is bounded by what is hers (the spec's "Her
own things, without a walk through everyone's"). An asker who may see the
layer reads no row by who: her own are told by the line row's `:who`.

Line rows and things are fetched as runs of neighbouring stamps, one window
each (`acts.clj`, `runs`): a run goes across a stamp with no thing in the
table and stops at a stamp whose kept thing must not be read, a withheld one
or one heavier than the read, or at a stamp that the page does not hold. So a
holder's page of n kept things is one window, and a thing that is withheld is
never fetched (R16).

### The seeks of each read

Counted from the code; nothing measured on disk. A call is one round trip to
the layer's task; k is the asker's own decisions from the start, n the count
of the page, c the permissions above hers, m the runs of neighbouring stamps.

| The asker | Rounds | Seeks, and steps |
|---|---|---|
| the layer's first holder | 4: L, the rights, the window, the things | 1; 3 and a step for each own row of the layer; 1 and n steps; 1 and a step for each kept thing |
| a holder given hers beneath c others | 5: and the rows above, between the rights and the window | the same, and c |
| a visitor of a layer open to everyone | 4 | as the first holder's, the things of her own decisions in m windows |
| an asker outside, or in a layer not made | 5: L, the rights, her rows by who, her line rows, her things | 1; 3 and steps; 1 and k steps; m and k steps; at most m and k steps |
| the same, when the rows above her permission must be read to know that it ended | 6 | and c |
| an asker who may see the layer, from a stamp past L | 2: L and the rights (3 with the rows above) | about 4, and no row of the line |
| an asker outside, with no decision of hers from the start | 3: L, the rights, her rows by who | about 5, and no row of the line |

Rama's skill text counts the level of a field inside the table for a task as
one read more, for rounds 1 and 4. Six rounds is the read path's bound for a
read asked once (`max-rounds`); the pure proof holds 4, 5, 5 and 6 for the
four kinds of asker, and the cluster proof holds a first holder's read at four
calls, one in each round.

### Who is given what

`acts.clj`, `reads-whole?`, is the one rule, and `asker-kind` what the asker
is in the layer:

| The asker | The answer |
|---|---|
| a holder: her permission for the layer, or her run's, stands | every decision whole, the last stamp, `:next` the start and the count given |
| a visitor: she may see the layer, which is open to everyone, and holds no permission there; signed in or not | with the acts setting at `:everyone`, every decision whole; else her own whole and every other as `{:stamp s}` alone; the last stamp |
| outside: she may not see the layer, or it is not made | her own decisions whole and nothing else; no last stamp; the layer left out, `:may-not-see` or `:not-made`; `:next` the stamp after her last decision given, or the start |

A decision given whole is `{:stamp :act-id :answer :thing}`, or, when there is
no thing to give, `{:stamp :act-id :answer :no-thing why}`: `:too-heavy` or
`:not-weighed`, the gate's words, or `:heavier-than-the-read`, a kept thing
that alone weighs more than the read's bound. Each of these is told apart from
a withheld one, and the read goes on past it. A taken act is held to the same
rule as a refused one, the spec's reading, which `reads-whole?` would change.

Her own, in this build: a decision whose thing names her as its `:who`. The
asker is what the caller says, and a thing's who is what its sender wrote, so
a sender can put into a layer a thing that names another as its who, which
that one then reads as her own. Sign-in will hold the asker against the
caller; nothing holds a sender's who but the gate's check of the permission
that an act names.

### The bounds

Two numbers of the read path beside its others
(`read_path/query.clj`, `default-numbers`), fixed at its opening:

- `:max-acts-of-one-read`, 1,000: the `most` of a query is cut to it. A page
  of 1,000 line rows is about 250 KB.
- `:max-thing-weight-of-one-read`, 33,554,432: the weight of the things that one
  answer fetches and gives, in all. 32 MiB, the heaviest thing that a gate
  keeps, so each kept thing can be read. Things are given in the order of the
  stamps while their weights fit; the answer ends before the first that does
  not fit, and `:next` is its stamp.

The weight is the gate's measure, which each line row holds, so the read knows
what a page weighs before it fetches a thing. A weight is not the bytes that
Rama moves (open item 3).

### The words

Two words more of the read path, now ten: `:find-the-line-whole`, carrying
`:stamp`, from `read-once`; and `:keep-a-read-of-acts-live`, from `read-live`,
which turns a query of `:acts` away at once with no call to Rama. Two reasons
more of the query's check: `:acts-not-named` and `:acts-not-of-one-layer`.

### A correction of stage 1: the time's weight

A review by another session, read-only, of `7f1242f3`
(`overnight-audit/REVIEW-store-2026-11-22.md`, step 5, in `b2a95dc6`) found
that the time on a thing that is no whole number was weighed by what it holds,
where every other part is weighed by its text: a run of 200,000 bytes weighed
about 200,001 bytes where its text is about 400,000, and 30,000 longs of
nineteen digits about 240,001 where their text is about 600,000. So a
stranger's thing that the declaration refused, whose one text passes 256 KiB,
could be kept whole under the 256 KiB bound. Stage 1's fork 4 had chosen that
on purpose, to keep the weighing of any time within the bound, and it broke the
measure that fork 2 names. Stage 2 weighs the time by its own text, as the
thing's one text writes it one collection deep (`door/text.clj`, `feed-part`),
stopping as soon as the weight passes the bound (`door/digest.clj`,
`weigh-the-time!`). Run by hand: the two things above are now too heavy at
256 KiB; a time of a string, a vector, a run of 10 bytes and a map holding a set
each weighs exactly the bytes of the thing's text; a time of 30 MB of bytes is
turned away in 5 ms. Two pure checks hold both. No thing of the run-again
fixture holds a time that is no whole number (all 396 times are), so the
fixture's weights do not change and it was not made again. What stays
unbounded by the bound: a set inside the time, whose text is made whole
before it is weighed, as the digest's walk makes the text of a set anywhere
else in the thing; so the work stays of the order of the thing's own size.

The review's other preliminary finding against stage 1, that a decimal and a
part written `#other` or `#too-deep` count twice, is fork 3, by choice: it
weighs such a part more than it holds, never less. Its finding on the strip of
a decimal is open item 1. Its findings on piece 2's repair are that build's,
and this one does not touch them.

### The judge's run over the store

The statements of `store-piece-3/statements/` take any target of five calls
(`piece3_statements/runner.clj`). The target over the store is
`test-store/softland/store/line_statements.clj`: for each history, a module
of the store's own under a name of its own, four at once on one in-process
cluster of four tasks, destroyed after; the door's `act-id-for`, `send-act` and
`answer-of`; an append with no door to the depot named, waiting for the gate;
and `read-once` of `[:acts start most]`, by a read path whose bound on weight
is the judge's `:max-bytes`, its answer put in the judge's form. Session f
drafted it; this session gave it the module's name as Rama gives it (Rama puts
the declaring namespace before a module's name) and keeps a history whose play
throws as a break of its own.

## Stage 3: the read kept live

Asked: whether it should be offered, what it would cost, and how a caller
learns of new decisions without it. Nothing of it is built; `read-live` turns a
read of acts away.

**My position: offer it when the trail or an importer needs it, and in this
form: the layer's last stamp kept live, and the line read once at each
change.** Not a push on the line's rows.

What the read kept live must give is when to read again: the line only grows
at its end, in its order, so what a caller needs after stamp s is exactly what
a read once from s gives. A push on the line itself is the wrong tool for that.
A push on a run from s on grows with the layer and would pass the read path's
bound on a run's places, so it would have to slide from page to page. Its
start differs for each reader, so readers would share no push. It would send
every decision's row to every reader of the layer, things still to be fetched
apart, since a push of 32 MiB things is not to be had. And it would need its
own leaving of the layer when a permission ends.

The other form needs none of that. The read path holds one push on the layer's
last stamp, `[:last-stamp layer]` in the gate's table for a task. Every live
reader of the layer in a process shares it, as sources that are equal share a
push today, and what it carries is one number. At each change, the read path
reads once from each waiting reader's `:next`, and gives her what that read
gives. Every rule of the read asked once then holds as it is: the rights are
read after the last stamp, so a reader whose permission ended, or one above
it, is given the next answer as one outside the layer, which is the spec's "a
read that is kept live leaves the layer when her permission ends"
(What must hold, 7), with no machinery of its own. An asker outside the layer
is told no stamp of it: the read path calls her function only when a read gives
her something that is hers.

What it would cost: one push for each layer that is read live in a process,
whoever reads it; one change sent to that process at each decision of the
layer; and for each live reader at most one read once at each change, fewer
when decisions come faster than her reads, since one read covers all of them up
to the last stamp, bounded by count and weight as now. What must be run before
it is built: whether Rama's push on a key inside a subindexed map, in a table
that is a fixed-keys value, tells each write by a diff of its own, as a push
on a run of a top-level map was seen to. The facts and the line are top-level
maps; `:last-stamp` is not, and no probe has watched one.

How a caller learns of new decisions without it. Of its own act, from the
door: the answer gives its stamp. Of anything else, by reading again from the
`:next` that its last read gave. A read from a stamp past the layer's last
reads the last stamp and the rights and no row of the line: two rounds, about
four seeks, three rounds with the rows above her permission. So reading
again on a period is cheap. What it lacks is the moment, and that is all the
form above adds.

## Forks taken, each a session's

Session f's, stage 1:

1. **The line's rows come from a function of their own,
   `decision/line-writes`**, beside `decision/writes`, and the walk writes
   them first, before the record's rows. So `writes` keeps its seven
   arguments and its meaning, the judges' copy of the walk follows the store
   with no change, and a write of the line that threw would fail the event
   before its answer is written: the event runs again whole, and no answer
   stands for a decision whose line is not there.
2. **The weight is the bytes of the thing's one text**, the time on it too,
   and a run of bytes as its text writes it, two hex digits for each byte.
   The plan's check asked that a run of bytes weigh its length; the
   statements of piece 3 weigh it as its text. A weight that is the text's
   bytes is one number that any reader can make again from `text/text`, and
   it never weighs a part less than it holds. What it costs: a thing that
   holds bytes is kept up to half the bytes that the bound names. No act of
   the form holds bytes, so only a refused thing pays it.
3. **The parts that the text does not write out are weighed for what they
   hold** (`door/weight.clj`), and added to the text's bytes: a decimal
   counts twice, its text and its digits, so a decimal weighs a little more
   than it holds.
4. **The time on the thing is weighed by its own text, as the rest of it is.**
   Stage 1 weighed a time that is no whole number by what it holds; stage 2
   corrected it, above.
5. **A thing whose part is of a class that the weight does not know is not
   kept** (`:not-weighed`): its line row says so, and it is in its depot.
6. **The base's line, its things and its rows by who are on the base's task
   alone**: no copy to every task (the plan's Road I is the other way).
7. **A thing for a layer of the other gate gets a line row in the gate where
   it arrived**, under that gate's count for the layer id, as its answer
   does: every decision of a gate has a line row in that gate's record. The
   read of a layer gives the line of the layer's own gate.
8. **The digest in a line row is hex text**, so that no field of the line is
   a byte array, should a read of the line be kept live.
9. **This build reaches a store that holds decisions only by a second run of
   the gates over its depots**, never by an update in place: a module update
   does not make a PState again from a depot, so a store from before this
   build would hold no line for its decisions. No real data is kept.

This session's, stage 2:

10. **The read's rounds: the last stamp first, then the rights, then the line,
    or her rows by who for an asker outside; only decisions up to that last
    stamp are given.** Session f's draft read the window first and the rights
    after it, with the last stamp beside the rights, and a second window when
    the first held fewer rows than the last stamp said. Its order kept the
    rights newer than the window too, but an asker outside read a window of up
    to 1,000 line rows of the layer to be handed none of them, which the
    spec's line on her own things rules out, and its second window was a race
    to settle. This order reads nothing that is not given or judged, and makes
    a missing row a fault. What it costs: one round more for a holder, four
    where the draft took three.
11. **Line rows and things are read as runs of neighbouring stamps**, one
    window each, and a run never goes across a stamp whose kept thing must not
    be read. The draft's window of things went from an outsider's first stamp
    to her last, and would have fetched the kept things of others between
    them.
12. **The read is a word of a query, `:acts`**, and not a call of its own; it is
    asked once, and `read-live` turns it away with a word of its own.
13. **The bound on weight is a number of the read path**, fixed at its
    opening, as the spec has it, and not a part of a query: a caller cannot ask
    a task for more than the build allows. The judge, which gives a bound with
    each read, is played by one read path for each bound.
14. **The two numbers**: 1,000 decisions, and 32 MiB of things, the heaviest
    thing that a gate keeps.
15. **The rights of a read of acts are read on the layer's own task**, the
    base's too: the read's home is the layer itself.
16. **`:last-stamp` nil** for an asker who may see the layer, when its last
    stamp was read before the layer's first decision and her rights after it
    was made: the answer then gives nothing, and says that nothing was
    decided when the read began.
17. **`:heavier-than-the-read`**, the word of a kept thing that alone weighs
    more than the read's bound, told apart from the gate's `:too-heavy`.
18. **To an asker outside, `:next` is the stamp after her last decision
    given, or the start**, and no last stamp.
19. **The judge is run from its own folder with the store's two paths added
    by `-Sdeps`**, since its alias `:store` names them one folder short; no file
    of the judge's is changed.

## What is open

1. **The digest's walk strips a decimal's trailing zeros, and that takes time
   that grows with the square of its digits.** Run on JDK 21 on this
   machine: a decimal of 83 KB, 10^200,000 at the scale 200,000, whose text is
   `1M`, took 8,235 ms to strip; one of 830 KB had not ended after five
   minutes; a big integer of 830 KB printed in 285 ms. The walk takes the
   digest of every thing that is no stray, a stranger's too, before the
   declaration's check, and the declaration's check strips each decimal of
   an act again (`act/normal-value`, and the text of each value). So one
   thing of under a megabyte holds a task, and every layer of it, for
   minutes; through the door the runtime's thread pays it first. This piece
   adds none of it and its weighing never strips; it is older than this
   piece. Bounding it changes what a value is, or the one text of a large
   number, and so is a rule of the store: Sid's. Session f's position: a
   decimal and a big integer whose unscaled bits pass a bound, far above any
   value that `max-value-bytes` lets a fact hold, are no value of the form
   (`act/value?`), and the one text writes such a number as `#other` with no
   strip and no print; the door and the gates then agree on it, and a thing
   that holds one is refused, cheaply. Nothing is built for it.
2. **A thing of 32 MiB on a real cluster.** The module's proof keeps things of
   300 KB in process, and the judge's probe sends one of about 34 MB of text
   that is not kept; what Rama writes and replicates for a kept thing of
   32 MiB, and what reading one back costs a task, is not run (the plan's
   Q4).
3. **What the weight is in bytes that Rama writes**, on disk and on the
   wire: not compared.
4. **The judge's alias `:store`** names `../../../../src-store` and
   `../../../../test-store` from its folder, one level short of the
   repository root, so `clojure -M:store` loads no store. A finding about the
   judge; the run here adds the right paths and leaves its files as they are.
5. **The judge's K1 keeps every taken act whole**, where call 1 bounds a taken
   act at 32 MiB. An act that passes the declaration weighs far less than
   that (the pure proof holds an act at the declaration's limits under it), so
   no generated history reaches the difference.
6. **The read kept live**: stage 3's position, Sid's to turn; and before it is
   built, the probe of a push on `[:last-stamp layer]`.
7. **A set inside the time on a thing** is weighed after its whole text is
   made, as the digest's walk makes a set's anywhere else: the one weighing
   whose work the bound does not bound.
8. **A sender names any who**: a thing that names another as its who is hers
   to read until sign-in holds the asker, and nothing holds the who that a
   sender writes on a refused thing.
9. **The rest of the store review in `b2a95dc6`**, on piece 2's repair: not
   this build's, and untouched by it.

## What changed

From `git diff --stat` against `7f1242f3`, the stage-2 commit:

| File | Lines changed, by `git diff --stat` | What changed |
|---|---|---|
| `src-store/softland/store/read_path/acts.clj` | 486, new | The read of a layer's acts: its sources, its rounds, who reads a decision whole, the runs of neighbouring stamps, the page within its bounds, the answer |
| `src-store/softland/store/read_path.clj` | 179 | Ten handles at `open`; the paths of a window, a last stamp and a run of rows by who (`path-of`, `window`); `read-acts-once`, and the dispatch of `:acts` in `read-once`; `read-live` turning `:acts` away; the docstrings of the namespace, `open`, `read-once`, `read-live` and `max-rounds` |
| `src-store/softland/store/read_path/query.clj` | 61 | The word `:acts` and its shape; the rule that a read of acts names one layer; the two numbers; the docstrings that name the counts |
| `src-store/softland/store/read_path/words.clj` | 31 | The two words of a read of acts, and what a word carries, `:stamp` |
| `src-store/softland/store/read_path/layers.clj` | 19 | `who-may-read-acts`, the setting as a read of acts takes it |
| `src-store/softland/store/door/digest.clj` | 44 | The correction of stage 1: `weigh-the-time!`, and the docstring of `weighed` |
| `src-store/softland/store/door/text.clj` | 13 | `feed-part`, the walk of one part of a thing at its depth |
| `src-store/softland/store/read_path/README.md` | 73 | The folder's map: the new file, how the files relate, the choice of the read of acts, its words |
| `src-store/MODEL.md` | 45 | What can be asked: a layer's acts by stamp; what cannot be got back, written again; the two numbers |
| `src-store/softland/store/README.md` | 6 | The read path's row of the store's map: ten handles, and what a read of acts reads; two counts of words |
| `src-store/OPEN-QUESTIONS-rama.md` | 4 | "five forms of path", which is no longer true, in the words of what the read path sends |
| `test-store/softland/store/read_path_pure_proof.clj` | 327 | Three groups of checks of the read of acts, on tables played as values, round by round; the check of a query's form, the words, the numbers and the three maps of a word, brought to the new word |
| `test-store/softland/store/read_path_proof.clj` | 186 | The group `a-layers-acts-by-stamp` on the cluster, of 15 checks; the proof's note of each call's table, for the six new handles; its docstring |
| `test-store/softland/store/line_statements.clj` | 237, new | The target over the store on which piece 3's statements are played, and the run |
| `test-store/softland/store/pure_proof.clj` | 8 | Two checks of the time's weight |
| `test-store/softland/store/README.md` | 82 | The proofs' map: the new file and its section, and what the two read path proofs check and do not reach |
| `src/proposal/runtime-2026-11-21/BUILD-store-piece-3.md` | | This page |

## What was run, with its output

All on 22 November 2026, IST, on this machine; the cluster runs one after
the other behind the machine's cluster lock. The logs are in the two
sessions' scratchpads, outside the repository; the commands make them again.

### Stage 1, session f

| Command | Output | Time |
|---|---|---|
| a probe of JDK 21, outside the repository: `.stripTrailingZeros` of 10^n at the scale n, and `str` of 10^n | 8,305 bytes: 96 ms; 83,049 bytes: 8,235 ms; 830,483 bytes: not ended in 5 min. `str` of a big integer of 830,483 bytes: 285 ms | |
| `clojure -M:store -m softland.store.act-proof` | 166 checks, none failed | |
| `clojure -M:store -m softland.store.pure-proof`, before the proof's second writings of the declaration took the new key | 5 failed, each a second writing (the shapes, the rules, the reasons, the store keys, the namespace); written again | |
| the same, after them | 772, none failed (739 before this build) | 6 s |
| `clojure -M:store -m softland.store.read-path-pure-proof` | 244, none failed | 16 s |
| `flock … clojure -M:store-module -m softland.store.module-proof`, first run | 494 passed, 2 failed, both the proof's own count of stamps: two of the new group's things were sent with `send!`, which keeps no first answer; sent with `decide!` | |
| the same, second run | 496, none failed (482 before) | |
| `flock … clojure -M:store-module -m softland.store.run-again-proof`, on the sixth making of the fixture | 44 passed, 4 failed, all in the two checks of the fixture, and every row that differs a row of the three new tables, which the sixth making holds none of; checks 1 and 4, the run again and the run after failures, gave no row that differs, the line's among them: 424 answers and 424 line rows in the streaming gate, 2,101 and 2,101 in the microbatch gate | |
| `flock … clojure -M:store-module -m softland.store.door-proof` | 283, none failed | 36 s |
| `flock … clojure -M:store-module -m softland.store.read-path-proof` | 610, none failed | 75 s |
| `clojure -M:run 20000 1` to `5`, in `gate-histories/`, five at once | exit 0 each; every statement holds; "AT A YES FOR ONE ACT: 0 of the 0 kinds of break. Elsewhere: 0"; "ROWS OF A KIND THAT THE WALK DOES NOT KNOW, kept aside, by kind: none". Seed 5: 824,901 things sent, 764,726 decisions, 163,323 taken, the counts that the repair's page gives for the store before this build | 211 to 219 s |
| `clojure -M:run mutants 20000 1` | exit 0: 12 of 12 broken copies caught | 21 s |
| `flock … clojure -M:cluster 20000 5`, in `gate-histories/` | exit 0: "20000 histories, seed 5: 0 kinds of break … Every answer on the cluster was the answer in memory". With no break it sends no history | 201 s |
| `flock … clojure -M:store-module -m softland.store.run-again-proof make-fixture` | the seventh making, on purpose, for the line and the eighth key: `acts.edn`, 127,741 bytes, 401 records; `tables.edn`, 343,535 bytes, 1,706 rows, the line 174 and 201 rows, the things 174 and 201; every "held to" true, the line's and the setting's among them. Made twice: the first making's head said "four tables", and the proof's text of the head was corrected before the second | about 60 s |
| `flock … clojure -M:store-module -m softland.store.run-again-proof`, on the seventh making | 48, none failed | about 120 s |

### Stage 2, this session

`J` is `clojure -Sdeps '{:aliases {:the-store {:extra-paths ["../../../../../src-store" "../../../../../test-store"]}}}' -M:store:the-store -m softland.store.line-statements`, run in `store-piece-3/statements/` behind the lock.

| Command | Output | Time |
|---|---|---|
| `clojure -M:store-module -e "(require 'softland.store.read-path)"` | loaded | |
| `clojure -M:store -m softland.store.read-path-pure-proof`, with the new checks | 265 passed, 2 failed: the check's list of the words that it gives held no read of acts, and the numbers written out a second time were seven; both written again | 17 s |
| the same, after them | 273, none failed | 17 s |
| `clojure -M:store -m softland.store.line-statements 4 1` (the judge's alias alone) | "Could not locate softland/store/line_statements": open item 4 | 1 s |
| `J 4 1` | first "Module name cannot contain / character", then "Module not alive": the module's name as Rama gives it was not used; fixed in the adapter | |
| `J 4 1` | "PASSED the judge's probe of the bounds, over the store … PASSED the judge's statements over the store: 4 histories (seeds [1], 4 each), 0 broke a statement" | 16 s |
| `J`, before the correction of the time's weight | "PASSED the judge's probe of the bounds … PASSED the judge's statements over the store: 240 histories (seeds [1 2 3], 80 each), 0 broke a statement, in 242 s"; Rama's client printed lines marked ERROR as each module was destroyed | 249 s |
| `flock … clojure -M:store-module -m softland.store.read-path-proof`, before the correction | 640, none failed; each of the 15 new checks passed | 55 s |
| the correction, by hand in a REPL: `digest/weighed` of things whose time is a run of 200,000 bytes, 30,000 longs, a string, a vector, a whole number, a vector nested 40 deep, and 30 MB of bytes | `:too-heavy` (text 400,136 bytes) and `:too-heavy` (600,121) at 262,145; 126, 127, 133 and 201, each the bytes of the thing's text; the 30 MB time turned away in 5 ms | |
| `clojure -M:store -m softland.store.pure-proof`, after the correction | 774, none failed | 3 s |

### The final run, on the tree of the stage-2 commit

| Command | Output | Time |
|---|---|---|
| `clojure -M:store -m softland.store.act-proof` | 166, none failed | 1 s |
| `clojure -M:store -m softland.store.pure-proof` | 774, none failed: 772 after stage 1, and the two checks of the time's weight | 3 s |
| `clojure -M:store -m softland.store.read-path-pure-proof` | 273, none failed (244 after stage 1) | 17 s |
| `flock … clojure -M:store-module -m softland.store.door-proof` | 283, none failed | 33 s |
| `flock … clojure -M:store-module -m softland.store.module-proof` | 496, none failed | 53 s |
| `flock … clojure -M:store-module -m softland.store.read-path-proof` | 640, none failed (610 after stage 1) | 69 s |
| `flock … clojure -M:store-module -m softland.store.run-again-proof`, on the seventh making, not made again: stage 2 changes no table and no rule of a gate, and the correction changes the weight of no thing of the fixture | 48, none failed | 69 s |
| `clojure -M:run 20000 1` to `5`, in `gate-histories/`, five at once | exit 0 each; "HOLDS: agree, told, once, outsiders, stamps, kept, see, unseen, screen, run-again"; "AT A YES FOR ONE ACT: 0 of the 0 kinds of break. Elsewhere: 0."; "ROWS OF A KIND THAT THE WALK DOES NOT KNOW, kept aside, by kind: none". Seed 5: 824,901 things sent, 764,726 decisions, 163,323 taken, as after stage 1 | 208 to 217 s |
| `clojure -M:run mutants 20000 1`, in `gate-histories/` | exit 0: 12 of 12 broken copies caught | 21 s |
| `flock … clojure -M:cluster 20000 5`, in `gate-histories/` | exit 0: "20000 histories, seed 5: 0 kinds of break, each one's smallest history on a cluster … Every answer on the cluster was the answer in memory." | 198 s |
| `J`, the judge's statements over the store | exit 0: "PASSED the judge's probe of the bounds, over the store: 1 MiB kept whole with standing, a thing over 32 MiB too large and the line goes on, a sorted map that throws is safe"; "PASSED the judge's statements over the store: 240 histories (seeds [1 2 3], 80 each), 0 broke a statement, in 241 s". Rama's client printed lines marked ERROR as each module was destroyed | 248 s |

