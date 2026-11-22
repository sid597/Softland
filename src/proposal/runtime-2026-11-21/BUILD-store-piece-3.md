# The store's line, piece 3: a layer's acts and refusals, kept and read by stamp

[The spec](store-piece-3/SPEC.md) ·
[its implicit spec](store-piece-3/IMPLICIT_SPEC.md) ·
[the decomposition](store-piece-3/DECOMPOSITION.json) ·
[the first stage's plan](store-piece-3/PLAN-gates-keep-the-line.md) and
[its check](store-piece-3/PLAN_VALIDATION-gates-keep-the-line.md) ·
[piece 2, repaired](BUILD-store-piece-2-repair.md) ·
the judges: [the statements of piece 3](store-piece-3/statements/README.md)
and [the generated histories](gate-histories/README.md)

**22 November 2026.** One Claude session, on Sid's message in this session,
which he called his direct ask and his go for the whole job. This page says
what changed, each fork taken, each command with what it printed, what is
open, and each choice of the session's for him to turn. A session does not
judge its own build: a fresh session does that, from the diff.

## What he asked for

His words, from the message: "build piece 3 of the store's line in
`src-store`: a layer's acts and refusals, kept by the gates and read by
stamp", after piece 2's fix, in three steps:

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

His two calls of 22 November, "both defaults of the plan's check and mine to
turn":

1. "A taken act is kept whole up to 32 MiB. A refused thing is kept whole up
   to 32 MiB from a sender with standing, and up to 256 KiB from a sender
   with none or refused at the declaration. The weighing stops at the bound
   plus one, and a thing that is too heavy keeps no exact weight."
2. "The row by who is kept, inside the task's table, as the validation
   proposes under c."

The judges are not this session's: the statements of
`store-piece-3/statements/`, written by another session from the spec alone
(`f04298ee`), and `gate-histories/`. Neither was changed; nor was the
judges' copy of the walk, `gate-histories/src/gate_histories/walk.clj`: the
store's walk adds the line through a function of its own beside
`decision/writes`, which keeps its seven arguments, so the copy follows the
store with no change.

## Stage 1: the gates keep the line

### What a gate keeps now, for each decision

On the layer's task, in the decision's one write, before the answer and the
last stamp, and behind check (a) as every write of a decision is
(`gate/decision.clj`, `line-writes`; `module/walk.clj`, step 7):

| Row | Table | Key | Holds |
|---|---|---|---|
| the line row | `line`, one PState of each gate | `"<layer> <stamp>"`, the stamp in 19 digits | the uuid of the act-id, the answer's word, time and digest (hex), the who that the thing names, and the thing's weight; or, for a thing that is not kept, `:not-kept` `:too-heavy` or `:not-weighed` and no weight |
| the thing | `things`, one PState of each gate | the same key | the thing as it arrived, when it is kept |
| the row by who | the field `:by-who` of the gate's table for a task, a subindexed map beside `:last-stamp` (his call 2) | `"<layer> <who> <stamp>"` | the stamp; only when the thing names a usable who |

A thing is kept when its weight is a number not over its bound
(`decision/kept-bound`): 32 MiB (33,554,432) when its sender has standing,
256 KiB (262,144) otherwise. "A sender with standing", as the plan's check
defined it under its Option 2 and his call took it: the declaration took the
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
check holds it on nine things.

A stray and a second thing under a used act-id write nothing of the line, as
they write no answer. The base's line, its things and its rows by who are on
the base's task alone: the walk gives the gate nothing of the line to copy.
Nothing that the line holds names a task or a position in a depot, so the
form with no task of the run-again proof holds it whole.

### Each revision of the plan's check, and how it is met

| The check asked | What was built |
|---|---|
| One walk for the digest and the weight (its items b, e; E3) | `text/feed` hands a third function each part whose text does not hold what it holds; `digest/weighed` feeds the hash and counts the bytes of each piece in the same walk. The walk takes the declaration's check first and then the one walk, so that the weighing knows its bound (`decision/weighing-bound`). `digest/digest` is unchanged, and the one walk gives its digest: a pure check holds the two equal |
| Arrays and big numbers weighed for what they hold (E1, and item e.1.6) | An array of a primitive weighs 1 and its length times the width of one item, with no walk; an array of objects, a Java collection, a map and a record weigh 1 and their parts; a big integer, a decimal and a ratio weigh their bits in whole bytes, read from `bitLength`, with no text and no strip. A run of bytes weighs its text, two hex digits for each byte (a fork, below) |
| No unbounded work on the task for a refused thing (item b, E2) | The two bounds of his call. The weighing stops as soon as the sum passes the bound: a thing that is too heavy keeps no weight. Every part weighs 1 at least, so a walk of the parts that the text does not write out ends within the bound, also for a part with no end (a record that holds an endless sequence is too heavy in 11 ms). A thing that the declaration refused is weighed up to 256 KiB. What the work beyond the digest's own walk is, is said in the walk's docstring. One cost that is not bounded is in the digest's own walk and older than this piece: open item 1 |
| The seeks of each write and each read counted (Partitioning) | Writes: no read is added to any kind of thing; one to three writes are (below, and `module/walk.clj`, "What one call costs"). Reads: stage 2 |
| The hazard of high code points in a who as in a layer (R20) | In the read: stage 2 |
| The rows by who in the task's table (item c) | His call 2: `:by-who` in `record/tables.clj`, `task` |
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

Built after stage 1's commit; this section is written with it.

## Stage 3: the read kept live

Written with stage 2.

## Forks taken, each the session's

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
4. **The time on the thing is weighed as its text when it is a whole
   number**, and as what it holds otherwise, so that a refused thing's time
   of any size is weighed within the bound.
5. **A thing whose part is of a class that the weight does not know is not
   kept** (`:not-weighed`): its line row says so, and it is in its depot.
6. **The base's line, its things and its rows by who are on the base's task
   alone**: no copy to every task (the plan's Road I is the other way).
7. **A thing for a layer of the other gate gets a line row in the gate where
   it arrived**, under that gate's count for the layer id, as its answer
   does: every decision of a gate has a line row in that gate's record. The
   read of a layer gives the line of the layer's own gate (stage 2).
8. **The digest in a line row is hex text**, so that no field of the line is
   a byte array, should a read of the line be kept live.
9. **This build reaches a store that holds decisions only by a second run of
   the gates over its depots**, never by an update in place: a module update
   does not make a PState again from a depot, so a store from before this
   build would hold no line for its decisions. No real data is kept.

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
   number, and so is a rule of the store: Sid's. My position: a decimal and
   a big integer whose unscaled bits pass a bound, far above any value that
   `max-value-bytes` lets a fact hold, are no value of the form
   (`act/value?`), and the one text writes such a number as `#other` with
   no strip and no print; the door and the gates then agree on it, and a
   thing that holds one is refused, cheaply. Nothing is built for it.
2. **A thing of 32 MiB on a real cluster.** The module's proof keeps things
   of 300 KB in process; what Rama writes and replicates for a thing of
   32 MiB, and what that costs a task, is not run (the plan's Q4).
3. **What the weight is in bytes that Rama writes**, on disk and on the
   wire: not compared.

## What was run, with its output

All on 22 November 2026, IST, on this machine; the cluster runs one after
the other behind the machine's cluster lock. The logs are in this session's
scratchpad, outside the repository; the commands make them again.

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
