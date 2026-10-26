# Receipt: the read build

proven by a hand-played asker, used by no inhabitant yet

> **14 October 2026.** Two builds came after this receipt. The build before real data took the
> locks out (`../before-real-data/RECEIPT.md`), so a row here that names a lock names a thing that
> is gone. On 13 October Sid went through that build's receipt and made eight decisions:
> `src-store/README.md`, "Decisions of 13 October 2026 — the store comes out the same when it is run
> again". The build "run again" (`../run-again/`) followed, in the night of 13 to 14 October. What
> this receipt says happened is left as it was written. Each open row below that those decisions or
> that build closed, changed or made moot has a line that begins "**14 October:**" and says where it
> stands. Of "What is true now": the base keeps no clock of a batch any more (entry 5 of that day).

Written by the session that finished the build, "session 1" (Opus 5.5), 12 October 2026, for Sid.
It keeps apart what was run, what was modelled, and what was only read. The working behind each
line is in the papers that the line names, all in this folder.

## 1. What is true now

The way out is built: a caller in the runtime's process hands the read path a query written as
data, for a place, an entity, a run of neighbouring places, or what points at a thing, through a
list of layers, and gets the value now and each change after. The gates keep what a read needs: the
value plain, who wrote it and under which permission, who may see a layer, what it builds on, the
keys that it marks with one more order by value, and for the base a clock that is the same on every
attempt of a batch. Three things are true since this last session that were not true this morning.

- **A live read is told a change at once, and not a second late.** From a streaming depot's ack to
  the call of the asker's function, for a place of the facts table: a middle of 0.095 ms and a slowest 1 in 100 of 0.2 ms, with 500 of 500 acts told by a call of their own, where it was a
  middle of 1,000 ms with 26 of 500 acts told by a call of their own.
- **A live read that is too large is turned away at once.** A canvas of 5,000 kept live through
  three layers as what points at it: turned away in 20 ms with the read path's own word, before one check is opened, where it was turned away after 9.3 s, with about
  10,000 openings that Rama refused one by one.
- **A read asked once gives what the tables hold.** Straight after the acks of 200 acts it gives 200
  of 200 places, also where a live read holds a push open on the same run; it gave 186 to 192.

**And one thing that the first of these costs**, measured in the same final run. Each write is now
told to each push that holds the written row. With 898 different live reads on runs that all hold
the places that are being written, an act took a middle of 15.5 ms where it took 4.7 ms, and one
maker could not hold 100 acts a second. With 100 such reads it took 4.9 ms. With 898 live reads on
places that are not being written it took 3.8 ms, as with one. Section 4 has the lines.

What "used by no inhabitant yet" means here: no view, no screen, no tool and no agent calls the
read path. Its two proofs and its measures are its only callers. No code outside `src-store` and
`test-store` names the store, but the probes of the build's own papers.

## 2. The commits

All on `main`, each by named paths. None is pushed.

| Commit | What it is |
|---|---|
| `00721838` | The run of what a second attempt of a batch does on Rama |
| `dc70e98b` | The implied spec, and the rulings on its 31 points |
| `a8c1783b` | The split into three parts |
| `b378e8f0`, `2ea0fff9`, `efaf6275` | The three plans |
| `794bc2fd`, `a6857ba8`, `735e963f` | The three checks of the plans |
| `371c6d2f` | The gates keep what a read needs |
| `fd92e6e9` | The base for a reader: each batch's clock is kept across its attempts |
| `16ced230` | The read path |
| `ffc10f29`, `e5c4a800`, `b12070e5`, `beb852d5`, `818fd840` | The papers of the builds, the build's page, the starter |
| `96f7034d` | The four repairs that the rulings left to the review, with their proofs |
| `9541425f` | What the review of the whole changed, with its proofs |
| `91d21fa5` | One check more in the read path's proof, for a thing that this receipt states (section 8) |
| `aa94bc09` | Memory: running the store's proofs from this shell. Not of the build |
| the commit that holds this file | The review's paper, the final runs, the measures, the build's page, this receipt |

## 3. The proofs, before and after

Each is one command from the repository root, and each ended with exit status 0 in every run that
is named here. "Before" is the tree before the read build, run by the build session on 12 October.
"After" is the final tree, run by this session; each cut output is `runs/final-<proof>.txt`.

| Proof | Before the read build | After the three builds | After the repairs and the review |
|---|---|---|---|
| act | `{:test 8, :pass 150, :fail 0, :error 0}` | 160 | `{:test 8, :pass 160, :fail 0, :error 0}` |
| pure | `{:test 16, :pass 540, :fail 0, :error 0}` | 686 | `{:test 18, :pass 690, :fail 0, :error 0}` |
| module | `{:test 22, :pass 227, :fail 0, :error 0}` | 354 | `{:test 33, :pass 354, :fail 0, :error 0}` |
| door | `{:test 26, :pass 193, :fail 0, :error 0}` | 199 | `{:test 27, :pass 199, :fail 0, :error 0}` |
| read path, pure | not there | 149 | `{:test 12, :pass 152, :fail 0, :error 0}` |
| read path, on the cluster | not there | 445 | `{:test 27, :pass 482, :fail 0, :error 0}` |

No check was removed by any stage. The three builds wrote 43 checks again for a decision that made
them untrue (ruling 73, with the list in `TEST_VALIDATION-what-the-gates-keep.md`). This session
wrote about forty more again for the repairs; `FULL_SPEC_REVIEW.md` lists them in a table. Four of
those had passed for a wrong reason: a byte array compared with a String, a throw that was caught
and read as "no value", two lists of rows that were unequal only because two byte arrays are never
equal, and an inequality that held whichever way the code went.

## 4. The measures of C

One run on the final tree, `runs/measures-final.txt`: 558 seconds, exit status 0, 61 lines, and no
opening of a push that Rama refused. "Before the repairs" is the last build's one run,
`runs/measures-read-path.txt`. Both are Rama 1.6.0 in-process, 4 tasks, 2 threads, 2 workers, one
machine, every row in memory, no network: a time is a floor for a round trip and says little of a
seek on a disk. Each shape was run once, so a difference of a millisecond between the two columns
is inside what one run can tell. Nothing is held to a number; the contract sets no target. M1, M2
and M4 give the middle and the slowest 1 in 100; M3 gives the middle of five openings.

| What, with the middle and the slowest 1 in 100 in ms | Before the repairs | The final tree |
|---|---|---|
| M1 the streaming gate, 100 acts a second, the key not marked, from the call of `send-act` to its return | 5.42 / 29.553 | 4.505 / 26.453 |
| M1 the same, the key marked | 4.767 / 28.302 | 4.427 / 26.38 |
| M1 the base, the same acts, from the call to "accepted" | 1202.828 / 1415.44 | 1010.276 / 1412.349 |
| M2 the streaming gate with 1 live reads asked for, each on another place that is not written | 4.281 / 31.137; open 1; told 0 | 3.812 / 26.384; open 1; told 0 |
| M2 the streaming gate with 1 live reads asked for, each on another run that holds the written places | 4.274 / 28.254; open 1; told 21 | 3.945 / 30.289; open 1; told 3499 |
| M2 the streaming gate with 100 live reads asked for, each on another place that is not written | 4.387 / 34.425; open 100; told 0 | 3.809 / 28.525; open 100; told 0 |
| M2 the streaming gate with 100 live reads asked for, each on another run that holds the written places | 3.967 / 27.658; open 100; told 2100 | 4.943 / 30.148; open 100; told 350000 |
| M2 the streaming gate with 1000 live reads asked for, each on another place that is not written | 4.117 / 27.846; open 998; told 0 | 3.787 / 25.882; open 898; told 0 |
| M2 the streaming gate with 1000 live reads asked for, each on another run that holds the written places | 4.695 / 38.366; open 998; told 21998 | 15.48 / 44.797; open 898; told 3142587 |
| M3 a canvas in a streaming layer, the run across the entities, asked once, through one layer | 39.522 | 40.203 |
| M3 a canvas in a streaming layer, the run across the entities, asked once, through three layers | 48.055 | 46.557 |
| M3 a canvas in a streaming layer, the run across the entities, kept live, through one layer | 48.645 | 42.592 |
| M3 a canvas in a streaming layer, the run across the entities, kept live, through three layers | 48.017 | 46.897 |
| M3 a canvas in a streaming layer, what points at the canvas, asked once, through one layer | 47.515 | 43.632 |
| M3 a canvas in a streaming layer, what points at the canvas, asked once, through three layers | 82.874 | 79.127 |
| M3 a canvas in a streaming layer, what points at the canvas, kept live, through one layer | 36.118 | 36.769 |
| M3 a canvas in a streaming layer, what points at the canvas, kept live, through three layers | 9279.655 (a word, `:reach-the-store`) | 19.978 (a word, `:serve-a-read-that-large`) |
| M3 a canvas in the base, the run across the entities, asked once, through one layer | 43.648 | 36.214 |
| M3 a canvas in the base, the run across the entities, asked once, through three layers | 45.314 | 45.091 |
| M3 a canvas in the base, the run across the entities, kept live, through one layer | 34.715 | 34.899 |
| M3 a canvas in the base, the run across the entities, kept live, through three layers | 42.648 | 55.077 |
| M3 a canvas in the base, what points at the canvas, asked once, through one layer | 33.335 | 32.226 |
| M3 a canvas in the base, what points at the canvas, asked once, through three layers | 84.993 | 80.48 |
| M3 a canvas in the base, what points at the canvas, kept live, through one layer | 34.485 | 34.673 |
| M3 a canvas in the base, what points at the canvas, kept live, through three layers | 9272.171 (a word, `:reach-the-store`) | 19.836 (a word, `:serve-a-read-that-large`) |
| M3 a canvas of 100, what points at it, kept live, through three layers | not run | 7.059 |
| M4 how late a push is, of a place of the facts table, in a streaming layer, from the ack | 1000.321 / 2000.551; 26 of 500 told by a call of their own | 0.095 / 0.201; 500 of 500 |
| M4 how late a push is, of a place of the facts table, in the base, from the ack | 1609.443 / 2713.18; 25 of 500 told by a call of their own | 564.786 / 844.242; 129 of 500 |
| M4 how late a push is, of a place of the facts by value, in a streaming layer, from the ack | 0.151 / 0.23; 500 of 500 told by a call of their own | 0.132 / 0.234; 500 of 500 |

**What the numbers say, and what they do not.**

1. **How late a push is.** For a place of the facts table in a streaming layer, from the depot's
   ack to the call of the asker's function: a middle of 0.095 ms, where it was 1,000 ms; every act
   was told by a call of its own, where 26 of 500 were. In the base, from the append's ack: 565 ms,
   where it was 1,609 ms. What is left there is the base's own second: 129 of 500 acts were told by
   a call of their own and 371 by a later act's, since two acts of one batch at one place give one
   change. A place of the facts by value is as it was, 0.13 ms.
2. **The canvas of 5,000.** Asked once through three layers as what points at it: 79 ms, with 14
   calls and 4,950 places, as before. Kept live in one layer: 37 ms and 3 pushes. Kept live through
   three layers as a run: 47 ms and 9 pushes. Kept live through three layers as what points at it:
   it does not open; it is turned away in 20 ms, after 9 openings for the rights and the orders and
   none for a check, where it was turned away after 9.3 s and about 10,000 openings that Rama
   refused. A canvas of 100 does open that way: 7 ms, 135 places, 220 pushes.
3. **The read path's own bound.** Of 1,000 live reads asked for on one layer, 898 open, with 900
   pushes, and 102 are turned away with `:keep-more-reads-live`; asking for all 1,000 took 222 ms
   and 249 ms in the two shapes. Before, 998 opened and 2 met Rama's bound.
4. **What live reads cost a gate.** Reads on places that are not written cost nothing that one run
   can tell: 3.8 ms with 1, with 100 and with 898. Reads on runs that hold the written places do:
   3.9 ms with 1, 4.9 ms with 100, 15.5 ms with 898, and with 898 the maker fell behind its own
   pace, by a middle of 10.7 s over the run of 3,500 acts. That is about 13 millionths of a second
   for each push that holds the written row and each write, with the askers' functions running in
   the same process as the gate, which a real runtime's would not. Before the repair this shape was
   cheap, 4.7 ms with 998, because Rama sent those pushes one whole value about once a second and
   the askers were told 21,998 changes; they are now told 3,142,587. It is the same fact as point
   1, seen from the gate.
5. **Not measured**: the base on a real cluster, a network, a second node, a seek on a disk, a
   second runtime. The base's 1.0 s from the call to "accepted" is the in-process run of a
   microbatch and says nothing of a cluster.

## 5. The four repairs

Each was already ruled by the build session. `FULL_SPEC_REVIEW.md` has the working of each.

- **Ruling 86: a fact's row holds its value as a String.** Rama's push on rows that hold a byte
  array sent the whole value anew about once a second; with a String it gets one change for each
  write. The String is the text itself when the plain bytes are well-formed UTF-8 that does not
  begin with U+0000, and else U+0000 and one character for each byte, so the bytes come back
  exactly. *Run before it was rested on* (`runs/review-probe-held-value.txt`): ten runs of bytes,
  all 256 byte values and 65,508 random bytes among them, written by a stream and by a microbatch
  topology and read back the same; 20 of 20 and 100 of 100 changes on a push, where byte-array rows
  in the same run got 7 and 1. So the mark stayed U+0000. *Changed:* one pair of functions in
  `record/rows.clj`, one line of the schema, two lines of the gate. The digest of a value is still
  the digest of its bytes, so no key of the order by value moved.
- **Ruling 87: the read path bounds its own pushes.** It counts them in the same swap that joins or
  leaves them, and turns a live read away with a word of its own before it opens one past 900. One
  live read may need 250 checks of nearer layers; one asked once keeps 40,000. Not as the ruling
  words it in one thing: section 9.
- **Ruling 89: a read asked once reads the table.** The function that took an open push's value is
  taken out.
- **Ruling 84:** the door's two lines say "the two depots of acts".

## 6. The review of the whole

Verdict `pass`, in `FULL_SPEC_REVIEW.md`, with what it reached and what it did not. It made three
passes and found seventeen things beside the repairs, each fixed and held by a run or a script:
twelve in the explanations, five of them lines that one build wrote and a later build made untrue;
and five gaps in the proofs, two of which the repair of ruling 86 opened. It found no fault in the code
of the gates or of the read path beyond the four repairs.

What it ran over the whole tree, beside reading: every one of 967 definitions has a docstring;
every one of 306 links in the maps resolves; one file of the store's source opens a table (R1);
10,768 hostile things handed to the check of a query made no path that leaves one layer (R3);
every keyword that this build made is on a list of words (R6); every declared word of every class
is named by a check.

**What you should know about how it was made.** The contract and the skill ask for one fresh review
of the whole. This one was made by the session that had just made the repairs, because your line to
it was "yourself, with no subagents", and the starter's head was changed to say otherwise only
after the session had started. And 1.8 MB is more than one context holds: the review read the read
path, the record, the decision, the first act, the streaming gate and the clock's topology whole;
it read the store's page and the proofs' map in the sections of this build; and it read the
microbatch gate's pure code, the door, the act and the four older proofs through what this build
changed in them and by search. Its paper says exactly where that line runs. If you want the part that it did
not read line by line judged by fresh eyes, that is one more session, and nothing in this receipt
depends on it having passed.

## 7. Every provisional choice

None of these is a decision of yours. Each is engineering, written with its reason in a docstring
or in the ruling named, and each is yours to change.

**The form and the place of things.**

- The form of a query: a plain map of a few parts; once or live is the call and no part of the
  data, so `read-once` and `read-live` take the same query (ruling 60).
- Where the read path's code lives: `read_path.clj` at the top of the store, beside `door.clj`, with
  its pure parts in `read_path/`; its pure checks are a proof of their own (ruling 80).
- Where a permission's one part more lives: a fifth slot of the permission itself, `:see` or
  `:see-and-act`; without the slot it reads as "see and act" (ruling 2). **Flagged for you:** your
  decision 8 of 6 October names two forms of a permission, and the code now reads three. The other
  answer keeps the part beside the permission.
- "An act needs act" is checked with the standing, before any place is read; the word is
  `:permission-does-not-allow-acting`. A holder of "see" who sends a value meets it at the door's
  own request for locks (ruling 3).
- The form of the mark: one fact on the layer that holds a set of keys, 32 at most; a key in the
  store's own namespace cannot be marked (rulings 19, 40).
- The table of the facts by value: a table of its own for each gate; its key is the layer, the
  key, the stamp since which the key is marked, the SHA-256 of the value's bytes, then the entity
  (rulings 42, 43).
- Facts that were written before a key was marked are not filled in. A read of what points at a
  thing is told since which stamp each layer's order is level. A mark that is taken off and put on
  again gets a new stamp; the rows of the earlier marking stay and no read names them (ruling 20).
- In a microbatch layer the three new keys are on every task, and a mark holds from the batch
  after the one that wrote it (rulings 18, 41, 45).

  **14 October:** changed. Every row of a microbatch layer's facts and of its facts by value is on
  every task, and its answers, its last stamp and its strays are on the layer's task alone (entry
  4). A mark holds from the next act, and a newly marked key gets the stamp of the act that marks it
  (`../run-again/PART-B.md`, section 3).
- Who wrote a fact and the permission that its act named are two fields of the row, each a text
  (ruling 22). **A fact's value is a String** (ruling 86, 92).
- A retract under a store key stays refused; a list is emptied and a mark taken off by writing a
  value that says so; `:replaces` stays optional (ruling 21).

**The base.**

- A stream topology of its own keeps each batch's clock, on a third depot that takes no act, in a
  table of one row; the depot is not trimmed; Rama's phase timeout is not set (rulings 46 to 51).
- The gate throws in one place: when a clock that was acked cannot be read back. It then makes no
  stamp and Rama runs the batch again; while the cause stays, the batch fails each time and the
  worker goes down and up (ruling 59).
- The door's three reads stay held to closing, so a reader of the tables can see a fact of the base
  up to one run before its maker reads "accepted" at the door (ruling 16).

**14 October:** the three points go. No clock of a batch is kept: the door writes the time on the
act and no gate reads a clock (entry 1), so the third depot, its topology, its table and the gate's
one throw are out of the store (entry 5). A gate closes no batch, and the door's reads are held to
none: a maker of the base reads her answer when its batch is committed on the base's task
(`../run-again/PART-B.md`, sections 4 and 6). The base's gate is written again: one task decides,
in the order of the base's one partition, and the rows of an accept are copied to every task
(entry 4).

**How a read is made.**

- A read asked once reads a layer's fact in the same call as its rights, so the runtime can read up
  to 257 rows of a layer for an asker who may then not see it, and drops them (ruling 61).
- A source, one row or one run of one table under one routing id, is what two askers share. A
  layer's depot, setting, list and marks are one push. Her permission is followed only while the
  layer is "members" (ruling 23 and its readings).

  **14 October:** stands, and it means another thing for the base. A source of the base has the
  routing id of the layer that the read starts from, so two askers share a source of the base only
  when their reads start from the same layer. Before, every asker shared one (the queue's V51).
- When what shapes a read changes, the asker is given the whole answer anew (ruling 69). When a
  layer is no longer rightful for her, her read leaves that layer and says that it is left out
  (ruling 24).
- The asker's function is called on a thread of Rama's client, in the read's turn, and must return
  at once: one function that stayed 2.5 s held back other askers' reads (rulings 67, 75). Its first
  call is the whole answer, made by `read-live` itself (ruling 78). A read asked from inside it is
  turned away (ruling 77). A thing that is no function is as a function that throws at its first
  call (this session).
- A run is read in three steps, the first places, its size by a bounded walk, then whole; a run
  whose end holds a letter from U+E000 on has no first read, and a run between two such ends that a
  caller names is turned away (ruling 88).
- One task of the base is named with `com.rpl.rama.Helpers/genHashingIndexKeys`, which Rama's pages
  show for tests (ruling 62).

  **14 October:** gone. The read path names no task: it asks no task by number, asks Rama for no
  count of tasks, and uses no class of Rama's. It reads every source of the base under the routing
  id of the layer that the read starts from, `:from-layer` or the first of `:layers`, so the base
  is read on the task where the asker's own layer lives. A run across the entities of the base is
  one call or one push, where it was one for each task (`../run-again/PART-B.md`, section 7).
- A value that Rama sends anew is handled as the rows that differ (ruling 88).
- A read asked once takes no value of a push; the function that did is taken out (rulings 89, 95).
- The read path counts its pushes by table and routing id (rulings 87, 93).

  **14 October:** stands. In the base the routing id is now the layer that the read starts from.
  So one count holds the pushes on the base of the askers who start from one layer, and the 900
  bounds those. It does not bound what all such layers of one task hold on the base together, which
  is what Rama's own bound counts there (`src-store/softland/store/read_path/README.md`, "The read
  path bounds its own pushes"; the queue's V51).

**The numbers of the build, each kept and not decided.** 256 places in the first read of a run;
20,000 places in one run; 40,000 checks of nearer layers for a read asked once and 250 for one kept
live; 900 pushes on one table under one routing id; 2,000 paths in one call; a wait of 10 s; 65
layers in one read; 8 rounds; 64 layers in what a layer builds on; 32 marked keys. None bounds the
bytes of a run.

**The words.** Every word that the build made is on a list: the store's page and the read path's
page, each under "Words a session made, Sid's to change". Three of them are also names in the rig's
code, `:by-value`, `value-digest` and `read-values`; none was taken from it (ruling 54). This
session's: `:keep-more-reads-live`, `:no-room-for-a-push-it-came-to-need`,
`:pushes-under-one-routing-id`, `:max-pushes-under-one-routing-id`, `:max-checks-of-one-live-read`,
`held-value`, `plain-bytes`, `plain-bytes-of`.

**The measures' shapes** (rulings 28, 36, 96), and one shape that this session added.

## 8. Every not-decided thing, as it was built

Each is built to the kept or the working answer at one function whose docstring says "Not decided",
so that the other answer is a small change. Nobody decided any of them.

| The thing | Built as | The one function | What the kept answer lets happen |
|---|---|---|---|
| One permission for each holder in a layer | As built | `gate/decision.clj`, `permission-place`; the declaration's rule that a permission fact sits on its holder | She cannot hold "see" from one giver and "act" from another |
| Who may write a layer's setting, what it builds on, and its marks | Whoever may act in the layer | `gate/decision.clj`, `who-may-write-fault`, which gives nil | Any holder of "see and act" in a layer can set it to "everyone", and any such holder in the base can set the base to "members"; any of them can write the list and the marks |
| What a layer starts with | A new layer is "members", the base "everyone"; a layer builds on nothing and marks no key until a fact says so. Read from no fact | `gate/first_act.clj`, `starts-with` | A person who is not signed in sees the base. A later change here changes how every layer with no fact reads |
| A permission that says "see" stays "see" | A permission's place takes one write, and nothing ends a permission | `gate/decision.clj`, `store-key-fault` | It cannot be raised, and it cannot be ended |
| A read that left a layer out says so | The layer, and one of two words: she may not see it, or it is not made | `read_path/layers.clj`, `left-out` | She is told that a layer exists which she may not see, and which |
| How far a read walks from a layer | The one list: the layer, then the layers of its own fact, and no further | `read_path/layers.clj`, `in-order` | "Builds on" does not reach through a layer that a layer builds on |
| `:from-layer` when she may not see the start layer | That one layer is left out and its list is not used | the same function | She learns nothing of what that layer builds on |
| A fact under a marked key whose value is no thing's id | Every value is kept in the other order, by the digest of its bytes | `gate/decision.clj`, `by-value-writes` | No gate judges what an id is |
| The store's own facts in a tool's read | Left out unless the query asks; any asker who may see the layer may ask | `read_path/answer.clj`, `counts?` | **An asker who is not signed in can ask for the base's own facts, since the base is "everyone": each permission in the base, with its holder and its giver, and the count of each request for locks** |
| "What points at a thing" through layers | At a place that some layer's order gives, the nearest layer that holds a row wins, and the place is given when the winner's value is the thing | `read_path/sources.clj`, `nearer-checks` | One read, or one push, for each such place and each nearer layer. A place at which only a nearer layer says the thing, and which no order holds, is not in the answer |
| One moment for all the layers of one read | Nothing lines them up | no code | The layers of one answer are read at moments that differ |

**14 October:** none of these is decided, and each stands as built. Three rows carry more now.
"What a layer starts with": on 13 October he said what he means by the base ("What the base is",
before the eight entries of that day); that does
not decide the row (the queue's K14 and V25). "The store's own facts in a tool's read": the base
holds no request for locks any more, and the rest of the row stands. "One moment for all the layers
of one read": the tasks show one batch of the base at instants that differ, so a read through a
layer on another task than the base's can give the base as it was a moment before
(`src-store/softland/store/read_path/README.md`, "Where a microbatch layer is read"; the queue's
V52).

## 9. Where the contract, a ruling or the starter could not be met as written

- **A7's first sentence**, "A read shows no fact of a batch that could still be tried again", is
  read as the contract reads it when it says that the repair "gives both": a read shows no fact
  that a second try could change (ruling 16). A read does show a fact of a batch that Rama could
  still run again; every attempt makes it the same.

  **14 October:** it holds by another means. Every attempt makes a fact the same because a decision
  holds no clock and nothing of the attempt; it was the kept clock of the batch before
  (`../run-again/PART-B.md`, section 3).
- **A1**, "keeps those plain bytes in the fact's row": kept as a String that gives those bytes
  back, since a byte array in the row made every live read a second late (ruling 86).
- **R4**, "is sent nothing of it": read as "is handed nothing of it". Rama sends the runtime the
  whole row (ruling 15).
- **The contract's "one fresh review of the whole"**: section 6.
- **The contract's "The build session judges each stage's paper"**: changed by your word of the
  morning of 12 October. From the build of the base on, a build's verdict was taken from its own
  papers and final runs.
- **The skill's "each subsystem builds on the subsystems before it"**: the plans of the first two
  parts were written side by side, and the gates' part was built before the base's (rulings 38,
  53), for time.
- **The skill's rule not to put a cheaper design aside without building it**: the cheaper way of
  the checks of nearer layers was designed on paper and not built (ruling 79), because what those
  checks mean is yours and not decided.
- **Ruling 87, "on each table and task"**: built as "on each table under each routing id". Rama's
  client gives no call from a routing id to a task. What it reaches: every push of every asker
  through one streaming layer, which is the case that the measures met. What it does not: two
  streaming layers on one task, the entities of the base that share a task, a second runtime.

  **14 October:** the second of the three is another thing now. No source of the base is read
  under a routing id of an entity: each is read under the layer that the read starts from. So what
  the count does not reach in the base is the layers of one task from which the base is read live:
  each has a count of its own, and Rama's bound counts them together (the queue's V51).
- **Ruling 87, "before it opens any push"**: held for one round of a read. A live read learns what
  it needs round by round, so a read that is turned away in its third round has opened the pushes
  of its first two, unless another reader held them, and leaves them.
- **Ruling 89, "one function switches it"**: the function is taken out and no switch is left.
- **The starter's later head**, which says to give the repairs and the review to fresh agents: not
  followed, at your word to this session.
- **The measure "the opening of a canvas of 5,000 facts"**, kept live through three layers as what
  points at it: it does not open. It is turned away, now at once.
- **Where a plan could not be met**: one place in the gates' build (a request for locks reads no
  marked keys, against one line of its plan and with its cost table), six small ones in the base's
  build, four in the read path's build. Each is in its `IMPLEMENTATION_VALIDATION-*.md`, under
  "Where the plan could not be met", and each was taken as built (rulings 72, 83, 88).

## 10. Rows proposed for your queue, decided by nobody

Nothing is written into `decisions/OPEN-QUESTIONS.md`. Each is built to the answer in section 8 or
named beside it.

**14 October:** each is a row of the queue since 12 October. The first row is V24. Rows 1 to 8 are
V12, V11, V25, V10, V26, V27, V28 and V29. Row 9 is ?6. Rows 10 to 13 are V30 to V33. Rows 14 to 18
are V34. Rows 10, 11, 13 and 15 have a line below; the others stand as written, and none of them is
decided.

**The first row.** What does "what points at a thing" mean when a nearer layer holds another fact at
the place, and by which way are the nearer layers checked (P12, rulings 64, 79, 87)? As built, a
canvas of more than about 125 places cannot be kept live through three layers: it needs a push for
each place and each nearer layer, and one live read may need 250. Raising Rama's bound does not
change that shape; it moves the number. What works today for a canvas of 5,000: asked once through
three layers (section 4); kept live in one layer; kept live as a run through three layers. The
cheaper way, where a small nearer layer is read whole or held by one push and every check is taken
from it, was worked out on paper: 2 pushes where this needs 10,020 in the plan's worked case.

**From the implied spec** (P4 to P13):

1. May a holder hold more than one permission in a layer?
2. Who is the owner of a layer, and who may write its setting, its list and its marks?
3. What does a layer start with: who may see it, and what it builds on? (K14's first half.)
4. How is a permission raised from "see" to "see and act", and how does a permission end?
5. What may a read tell of a layer that the asker may not see, or that is not made? And when she
   may not see the start layer of `:from-layer` (ruling 65)?
6. Does "builds on" reach through the layers that a layer builds on?
7. What may a marked key hold, and what does a gate do with a value that is no thing's id?
8. May an asker who sees a layer read its permissions, its setting, its list and its marks? With
   the working answer a person who is not signed in can read them in the base.
9. Does one read line up the moments of the layers that it read? (The queue's ?6.)

**From the rulings:**

10. Your decision of 7 October is "Two depots, one for each gate". The module has a third, of the
    clocks of batches, which takes no act and from which no gate decides. Nobody asked you
    (ruling 34).

    **14 October:** answered. Two depots: the depot of batch clocks is gone (entry 5; the queue's
    V30).
11. Is the log of batch clocks kept for ever, as the logs of acts are (ruling 50)?

    **14 October:** moot. That log is gone (entry 5; the queue's V31).
12. Your decision 8 of 6 October names two forms of a permission; the code reads three (ruling 2).
13. A cheaper form for a streaming layer's write, found and not taken: one row for each layer that
    holds its last stamp and its marks would give an accepted act 4 reads where it now pays 5. It
    changes what a stamp is, from "increasing on its task" to "in its layer" (ruling 55).

    **14 October:** its second half is answered: a number counted per layer (entry 8). A gate keeps
    one last stamp for each layer. Its first half, one row for a layer's last stamp and its marks,
    stays open and is not built (the queue's V33). The count of reads here is of the store of 12
    October.
14. Is Rama's `pstate.proxy.limit` raised from 1,000, and the read path's 900 with it (rulings 87,
    91)? What a higher value costs a task is not known here.
15. Should the read path count its pushes by task? It needs a call of Rama's client, or a small
    query of the module, which would be the first query between the read path and the tables
    (ruling 93).

    **14 October:** open, and it bears on more. The base's rows are on every task, and each layer
    from which the base is read live has a count of its own. Derived, not run: at Rama's default
    bound that is about 500 such layers for each task, and the read path's count does not see it
    (the queue's V51).
16. Entry 4 trusts each program that can reach Rama's ports with the whole store. Such a program
    can also stop a worker with one call: a select with a path that Rama does not serve stopped a
    worker of the in-process cluster (ruling 70). The read path sends five forms of path and no
    other.
17. A second runtime: Rama's bound on pushes holds for every client together, and the read path's
    count is of one process. How many runtimes there are, and whether they share a bound, is not
    designed.
18. A place that is written often, under many different live reads. Those who look at the same
    thing share one push; those who look at overlapping things, such as runs with different ends
    over one busy entity, do not, and each of their pushes costs the gate at each write (section 4,
    point 4). Is a bound wanted on how many different live reads may hold one place, or is a view
    held to asking for a thing in the one way that is shared? Nothing is built for it.

## 11. Questions for Rama's team

1. Why is a push on rows that hold a byte array sent the whole value anew, about once a second,
   where the same rows with a String get one change for each write? Is it meant (ruling 86)?
2. Is there a call of the client that gives the task of a partitioning key (ruling 93)?
3. Is `com.rpl.rama.Helpers/genHashingIndexKeys` meant for a client outside a test (ruling 62)?
4. `pstate.proxy.limit`: what does a higher value cost a task, and is the bound for one table or
   for all the tables of a task (ruling 91)?
5. A range between two ends that hold a letter from U+E000 on, or one outside the first plane,
   gave fewer places than the table holds in two runs of the proof; a probe that asked the same
   ends by hand got them all. Which order does a range's end follow (ruling 88)? The cause was not
   found; that it is Java's order of Strings against the order of UTF-8 bytes is a guess that
   nobody verified.
6. How many threads does Rama's client have on a real cluster? It had two in-process (ruling 75).

**14 October:** the six are kept on a page of the store, `src-store/OPEN-QUESTIONS-rama.md`, with
what Rama's public sources say of each; nothing was sent to Rama's team. Question 3 fell away: the
read path names no task and uses no class of Rama's for that (`../run-again/PART-B.md`, section 7).
Questions 1, 2, 4, 5 and 6 stand. Question 4 bears on more now (the queue's V51). Two questions
that the base's kept clock raised, of an append with an ack from inside a microbatch and of the id
of a batch, are on that page and fell away with the clock (entry 5). That page also holds four new
questions from the build "run again".

## 12. What was run, what was modelled, what was only read, and what was not run

**Run on the store's own module**, Rama 1.6.0 in-process, one machine, 4 tasks, 2 threads, 2
workers, one copy of each row:

- the six proofs (section 3) and the measures (section 4);
- a real batch of 18 acts made to run a second time with no hook in the module's source: one
  clock, 52 keys compared across the two attempts, none differs; and with the gate changed for one
  run so that a later attempt reads the clock anew, 35 keys differed (ruling 82);
- the module started again under an open read, by Rama's module update with the unchanged module;
- how many pushes Rama opens on one table on one task: 1,000, then a refusal, and a closed push
  makes room (the last build's probe).

**Run on a probe's own module, not on the store's:**

- what Rama's push can watch (11 October, `push-probe/`);
- what a second attempt of a batch does, and the window in which a commit fails after some tasks
  have shown their rows (`batch-probe/`, ruling 17);
- that the bytes in a row are what makes Rama send a value anew (the last build's probe), and that
  a String with U+0000 is written, read back and pushed rightly (this session's probe,
  `runs/review-probe-held-value.txt`);
- nine things about threads, openings and callbacks of Rama's client
  (`plan-check-probes-read-path/`).

**Modelled, with no Rama:** every order of the steps of a live read, on the very functions that
`read_path.clj` calls: thirteen scenarios, 672 lists of writes on one place in two layers, 620
lists of a setting, a permission and two facts, each with the asker's screen held against a read
asked once at every end (ruling 74). That Rama's client delivers in those ways and no other is not
modelled; the probes ran it.

**Counted from the dataflow, not measured:** what one accepted act reads and writes in each gate (5
reads and 4 row writes in the streaming gate, 6 and 5 under a marked key); what one batch pays for
its kept clock (3 reads, 1 record, 1 row and the wait for the ack).

**Read, not run:** Rama's pages, in an extract that names 1.5.0 where the store runs 1.6.0; that a
push which sends no heartbeat is cleared; that a private table refuses every client; the name of
`pstate.proxy.limit`, from Rama's own message; a migration of a PState's schema.

**Not run by anyone in this build:** a cluster of several machines; a network between the runtime
and Rama; a killed process; a move of a task's leader; a second copy of a row; a commit that fails
after one task has shown its part, on the store's module; a module update between two attempts of
one batch; the gate's one throw inside the gate; a second runtime; Rama ending a push by itself or
sending a value anew by itself after the first callback; a number of tasks that changes; the base
on a real cluster; a tool, a view, an inhabitant.

**14 October:** the build "run again" ran one line of this list in part: another number of tasks,
8 beside 4, as a fresh store that is fed the kept acts
(`test-store/softland/store/run_again_proof.clj`). One line fell away: the gate's one throw is out
of the store (entry 5). That build's papers name none of the others as run on the store; its
receipt, `../run-again/RECEIPT.md`, says what it ran.

## 13. The sessions' slips

- **The build session**: its first commit took another session's staged rename with it; it undid
  its own two commits a minute later and made them again by path. And the last two builds stood
  still for about three hours, each waiting for a permission in an agent whose parent saw nothing.
  Both are on the build's page.
- **This session**: its first command "ran" six proofs with no name, because a list was not split;
  it left one file of 343 bytes and was run again rightly. And its first edit of `record/rows.clj`
  was made while that run was still going, so the last two of the six could have loaded a
  half-made change; they were stopped and run again on a clean copy of the head. No result in this
  receipt is of either.

## 14. Scratch that is not committed, and waits for your word

Nobody deletes any of it without your word.

- `runs/raw-rp-measures-full.txt`, 1.0 GB: Rama's log of 98,794 openings that it refused, from the
  last build's measures (ruling 90). The final measures of this session logged no refused opening.
  Deleted on 26 October 2026 at your word.
- The other `runs/raw-wtgk-*`, `raw-bfar-*` and `raw-rp-*` files of the three builds, and four raw
  logs under `plan-probes-read-path/logs/`.
- Seven files of this session, `runs/raw-review-before-*.txt`: the six proofs on the tree before
  the repairs, and the one of 343 bytes with no name after its last dash.
- A git worktree that this session made for its clean run, registered at
  `/tmp/claude-1000/-mnt-data-projects-Softland/c87e94e1-c1fe-4bc8-8491-5f2d66e3bfd6/scratchpad/baseline`,
  detached at `beb852d5`. It holds a checkout and nothing else; `git worktree list` shows it.
- Not this build's, and left as they were: the staged rename under `decisions/groups/`, the
  untracked `decisions/groups/g-reads.txt` and `g-check.txt`, `docs/.obsidian/`, `docs/Untitled.md`,
  and the change of `src/app/server/README.md`.

## 15. `git status --short`

As it is after the commit that holds this file. None of these lines is of that commit.

```
 M src/app/server/README.md
R  src/proposal/backend-on-rama-2026-10-02/decisions/groups/g-reads.txt -> src/proposal/backend-on-rama-2026-10-02/decisions/groups/g-reads-first.txt
?? docs/.obsidian/
?? docs/Untitled.md
?? src/proposal/backend-on-rama-2026-10-02/decisions/groups/g-check.txt
?? src/proposal/backend-on-rama-2026-10-02/decisions/groups/g-reads.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/plan-probes-read-path/logs/runs-run-1-all.log
?? src/proposal/backend-on-rama-2026-10-02/reads/plan-probes-read-path/logs/runs-run-2-all.log
?? src/proposal/backend-on-rama-2026-10-02/reads/plan-probes-read-path/logs/tasks-run-1-all.log
?? src/proposal/backend-on-rama-2026-10-02/reads/plan-probes-read-path/logs/tasks-run-2-all.log
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-act-final.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-act-final.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-door-final.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-door-final.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-lint.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-load.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-module-1.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-module-final.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-module-final.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-mutation.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-pure-1.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-pure-final.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-bfar-pure-final.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-review-before-.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-review-before-act.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-review-before-door.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-review-before-module.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-review-before-pure.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-review-before-read-path-pure.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-review-before-read-path.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-lint.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-measures-full.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-measures-full.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-measures-small.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-measures-small.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-mutation-ending.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-mutation-ending.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-probe-bytes.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-probe-bytes.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-probe-diffs.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-probe-diffs.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-probe-limit.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-probe-limit.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-probe-order.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-probe-order.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-probe-pace.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-probe-pace.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-proof-1.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-proof-2.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-proof-2.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-proof-3.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-proof-3.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-proof-4.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-proof-4.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-pure-1.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-pure-2.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-pure-3.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-pure-4.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-pure-5.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-read-path-pure.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-read-path-pure.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-read-path.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-read-path.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-rows-more.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-tree-act.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-tree-act.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-tree-door.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-tree-door.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-tree-module.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-tree-module.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-tree-pure.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-rp-tree-pure.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-act.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-act.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-door.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-door.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-head-door.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-head-mg.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-head-module.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-head-pure.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-head-sg.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-keep-batch.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-keep-decision.clj
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-module.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-module.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-mut.txt
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-pure.exit
?? src/proposal/backend-on-rama-2026-10-02/reads/runs/raw-wtgk-pure.txt
```

