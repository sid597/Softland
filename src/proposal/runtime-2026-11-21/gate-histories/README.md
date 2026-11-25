# Generated histories through the store's own door and gate

[Parent: the runtime's page](../PROPOSAL.md).

A check of the store by sessions that did not build it: the judge of the
store's line. Written on 21 November 2026 while piece 1 was built; rebuilt on
the morning of 22 November so that the histories found piece 2's breaks by
themselves; brought that afternoon to the form of a yes for one act with an id
of its own ([store-piece-2/SPEC.md](../store-piece-2/SPEC.md), 22 November).
It is only the judge: nothing in `src-store/` or `test-store/` was changed for
it. Another session brings the store to the form against what is committed
here.

Sid asked for this shape twice: on 12 November, "why do both of you think
the lean proof should come last?", and on 18 November, for formal
verification "so that we can say and more trust in what we are building".
The answer he was given that day was three steps: statements in plain
words, a model, and the model and the real gate run on the same generated
acts. This folder is the first and the third step, in Clojure. It holds no
Lean.

## What it does

It makes random histories of one layer and plays each one through the
store's own code and through the statements, thing by thing.

- **A history.** Gives, ends, gives after an end, plain facts, settings; a
  permission for a run, and acts in it and in another run; a yes for one
  act, with an id that its giver makes, ended by its giver, by the one above
  the giver or by its holder, given a second time for the same act under a
  new id, and given again at its own place; the act that a yes names, sent
  by its holder under an act-id of her own before its yes lands, while it
  stands, after it was used, with a part changed, written another way, in
  another run, under the yes's own id, twice, under each of two yeses, and
  as the yes's own end; a refused try sent again under a new act-id; the
  same thing again under its act-id; another thing under an act-id that was
  used, by the same who and by another; and a thing under the act-id whose
  uuid is a yes's id, by a holder who may only see, by one who holds
  nothing, or by anyone. Some makers name the right permission, some a
  stale one, one raised from "see", another holder's, or another's yes.
- **The store's way.** Each thing is made as a maker makes it and handed to
  the door's own pure code, which adds the act-id, the depot and the time
  ([walk.clj](src/gate_histories/walk.clj), `through-the-door`); the gate
  decides it as the module's walk does, check (a) and its answers among the
  steps (`decide`); and the door's own answer function says what its sender
  is told (`told`). Two askers keep a read open through the whole history,
  through the read path's own pure steps
  ([live.clj](src/gate_histories/live.clj)). A yes's text is made as a maker
  makes it, of the act that she hands the door
  ([run.clj](src/gate_histories/run.clj), `hex-for-a-yes`).
- **The statements' way.** The plainest code that says the statements
  ([reference.clj](src/gate_histories/reference.clj)): who was given what
  by whom, which gives were ended, which yes was used, and the one answer of
  each act-id. It reads no row, no stamp and no digest.

Where the two differ, that is a break. The thing is noted and taken out of
the history for both, and the rest of the history is still played, so one
history can show more than one break. Each break is named by its kind: the
statement that broke, and the road to it. For each kind, the first history
that broke so is cut down by test.check's own walk of its rose tree, then
further by taking out each step that the kind does not need, and printed
step by step ([run.clj](src/gate_histories/run.clj)). A kind is marked when
it is at a yes for one act: its thing names a yes, gives or ends one, or goes
under a yes's id, or what broke is T2.

## The form it holds the store to

From [SPEC.md](../store-piece-2/SPEC.md), 22 November; in the prompt that
Sid pasted that day, which a session wrote; a session's form, and his to
turn.

- **A yes for one act** is `[who layer granted-in above :see-and-act [:act
  yes digest]]`. `yes` is a UUID that its giver makes, and the yes sits on it,
  under `:softland.store/act-permission`.
- **Its digest** is of the act as its maker made it, in the one form that
  `softland.store.act/parse` gives, with its who and its session, without
  its act-id, its depot, its time and its permission: SHA-256 of that one
  form's text, as 64 hex characters.
- **The act goes under any act-id of its maker's.** The act-id plays no part
  in a yes.
- **Used up.** The gate uses the yes up when it takes the act. A try that is
  refused uses nothing, and is sent again under a new act-id.
- **Two yeses for one act** can stand at once, each used once. A yes's place
  takes one give. "An act has at most one permission that stands" no longer
  holds for acts, so nobody can stand in a giver's place.
- **One answer for an act-id.** An act-id names one act and gets one answer,
  whoever sent first.

## The statements

As [SPEC.md](../store-piece-2/SPEC.md) has them since 22 November: S1 to S12,
R1 to R10, and one answer for an act-id, each in the docstring of
[reference.clj](src/gate_histories/reference.clj). What changed in the plain
code, and the two beside them:

- **R2 without the act-id.** An act is taken under a yes only when its who is
  the yes's holder, it is the act that the yes names part for part, the yes
  stands, and it is not used; the act that is taken uses it up. "Part for
  part" is said in plain code (`the-act`): the history writes down which act
  each yes was for, and the statements compare the act as its maker made
  it, each part but the act-id, the depot, the time and the permission, a
  part left out read as empty. So where the gate's digest and the act part
  for part come apart, the gate and the statements differ.
- **R5** as SPEC.md now has it: a holder ends her yes, or her permission for
  a run, under her permission for the layer, or under the permission itself
  where it takes the end. A yes takes no act but its one act, so it ends
  itself only when that act is its end; a permission that says see takes
  none. A used yes has nothing left to end.
- **R6.** A yes's place takes one give: a give there that finds a yes, its
  end or its use is refused (`:a-yes-is-given-once`). A run has at most one
  permission that stands.
- **R9.** A thing under a yes for another act, an act that is not the one
  named, is refused, whatever its act-id.
- **One answer for an act-id** (`once`): the first thing decided under an
  act-id gives its answer; the same thing again is told it; another thing is
  not decided and is told "not mine". It replaces the morning's "another
  thing under an act-id that was used is decided as any thing is", which
  broke against the gate's check (a) under every seed.
- **T1.** An act that the statements take is taken by the gate, and its
  sender is told so.
- **T2, narrowed** as the morning's report proposed: nothing that an
  outsider of a yes sends writes at the yes's place or uses it up. An
  outsider of a yes is one who is not its holder, its giver or one above the
  giver. The morning's T2 asked more, that nothing an outsider sends changes
  whether the holder's act is taken: that broke in the statements too, through
  an insider's act that an outsider's thing made the gate take or refuse
  whole, and is no longer asked.

What every history is held to, in the run's words:

| Word | What must hold |
|---|---|
| agree | The gate takes a thing exactly when the statements take it. |
| told | The door tells each sender what the statements say she is told: "accepted" when they take her act (T1), "refused" when they refuse it, the first answer for the same thing sent again, "not mine" for another thing under an act-id that has an answer. |
| once | Under an act-id that has an answer nothing is decided and nothing is written, for the same thing and for another. |
| outsiders | T2. At each decision of a thing that the gate or the statements took, and for each yes of which its sender is an outsider: no row at the yes's place among what the gate wrote, nor a fact on it that the statements took; and the yes's own act, sent by its holder under a fresh act-id, taken before the thing, is taken after it, by the gate and by the statements. |
| stamps | The decisions of the layer have the stamps 0, 1, 2 and on. |
| kept | A decision changes no row but its own: what was taken stays, and a refusal writes no fact. |
| see | After each decision, the read path lets an asker see the layer exactly when the statements do: each holder and one who holds nothing, in no run and in each run, signed in and not. |
| unseen | An asker who keeps a read open on a layer that she may not see, before a decision and after it, is told nothing at it: not even an answer that says what it said (the read path's own promise, `softland.store.read-path.layers/meaning`). |
| screen | What such an asker was shown, after each decision, is what a read asked once gives. |
| run-again | The same things, decided again from nothing, give the same record. |

## Run it

From this folder. It needs Clojure and test.check, both in `~/.m2`; the
cluster replay needs Rama 1.6.0, which the store's module alias uses.

```
clojure -M:run                         # 2000 histories, a seed of its own
clojure -M:run 20000 3                 # so many histories, this seed
clojure -M:run mutants 3000 1          # the gate with one rule broken at a time
clojure -M:run sketch 20000 3          # against the sketch of a gate of the form
clojure -M:run sketch mutants 3000 1   # the broken copies, around the sketch
flock ../../backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock clojure -M:cluster 3000 1
```

`run` prints what the histories walked, then each kind of break with how
many histories showed it, the first, and whether it is at a yes for one act;
then each kind's smallest history: one thing a line, with who sent it, in
which run, under which act-id and permission, what it says, what the gate
did, what the door tells its sender, and what the statements say. `#n` is
the act-id with the uuid n, `#n.j` the j-th more of a step, `#yn` the act-id
whose uuid is the yes yn's id; `ben<asha yes y4` is the yes y4 that asha gave
ben. Its last lines say which kinds are at a yes and which are elsewhere, and
how many rows of a kind that the walk does not know were kept aside, by kind.
It exits 1 when a statement broke.

`mutants`, the broken copies, is the check of the check. It puts a broken
function in place of one rule of the gate or the read path, for the run
only, around the function in place, and the run must find a history that
breaks a statement under it that the gate without it does not break there.
The store's files are not changed. The last two touch a yes: one lets any act be taken under a yes,
one drops what an accept under a yes writes at its place, so that the yes
is used more than once. That last one assumes that a gate writes the use of
a yes at its place, the row that `writes` is handed for it; a gate that
writes it elsewhere needs the copy brought to it.

`sketch` puts a sketch of a gate that takes the form in place of four
functions of the store's gate, for the run only
([sketch.clj](src/gate_histories/sketch.clj)): the digest of the act as its
maker made it; the standing with the act-id playing no part; one end more at
the yes's place for an accept under it, which uses it up; a give at a yes's
place that holds a row refused. It is the judge's check of its own
statements and steps, made from the spec's words, before the store takes the
form. It is not the store's fix and binds nobody: where a gate writes the use
of a yes, and its words, are the store's builders'.

`cluster` plays the same run, takes each kind's smallest history, and sends
it through the store's module on a fresh in-process cluster: the door's
`send-act` and `answer-of`, the module's walk in Rama, and `read-live`. It
prints the door's answer to each sender beside the answer in memory, and
how often each live read's function was called. One in-process cluster at a
time runs on this machine, so it runs behind the cluster lock.

While another session changes `src-store` in this tree, a run reads that
session's files as they stand. To hold the committed store, export it and
put it on the path in its place, as this build did:

```
git archive 3544d026 src-store | tar -x -C /somewhere/outside
clojure -Sdeps '{:paths ["src" "/somewhere/outside/src-store"]}' -M:run 20000 1
```

## The copy of the walk, and the store's functions that it calls

[walk.clj](src/gate_histories/walk.clj) is the module's walk said again in
memory, from its description and the pure functions that it calls. Two
things keep it from breaking when the store changes under it:

- **A row of a kind that it does not know is kept aside.** It wrote four
  kinds, a fact, an entry of the facts by value, the answer and the layer's
  last stamp, and threw on any other; piece 3 will write new kinds. Such a
  row now goes under the record's `:aside`, in the order written, and
  nothing reads it back; the run counts such rows by kind in its last lines,
  and so does the record's run. So a rule of the store that rests on a new
  kind of row is not held by this copy until the copy writes that kind.
- **`writes` is handed every row that the walk read for the act**, as the
  module's walk hands it (`module/walk.clj`, steps 4 to 7, traced in the
  code): the row at the place of the act's permission and the rows of its
  chain, the rows of the places of its facts and of the chains that those
  need, and the marked keys' rows. Before, the copy handed it the marked
  keys' rows alone. So the store can use a yes up at its place with no new
  argument: the row there is in `:place-rows`. The module needs no change
  for it.

The store's builders are told to keep two of the functions that the copy
calls as they are: **`decision/hex-for-its-permission`, with the thing and
the act**, and **`decision/writes`, with seven arguments**. The copy also
calls `decision/permission-place`, `depot-place`, `chain-needed`,
`standing` (six arguments), `makes-the-layer?`, `places-needed`,
`chains-needed`, `places`, `marked-keys-place`, `reads-marked-keys?`,
`since-by-place`, `rows-before-needed` and `time-on`;
`declaration/stray-reason` and `check`; `digest/digest` and
`readable-hex-of`; `stamp/next-stamp`; `record/answer-key` and `answer-of`;
`words/answer`; and `additions/reading` and `thing-to-append`. A change to
one of these breaks the copy, and the copy is then brought to it.

## What waits for the store's fix

Against the store as committed (`3544d026`), every act under a yes of the new
form is refused: its gate holds the act-id's uuid against the yes
(`:permission-for-another-act`), and its digest is of the thing as it
arrives (`:act-not-the-one-permitted`). So until the store takes the form:

- **The run with no break.** It needs the store to take the form, and the
  read path's 2A fixed: 2A is not at a yes, and broke before this build too.
- **The broken copies that touch a yes.** "A yes is used more than once" is
  not caught against the store as committed, which never takes an act under
  a yes. "Any act is taken under a yes" is caught there, but only by a road
  through the use: with it, that gate takes an act sent under the yes's own
  id, and then takes the yes's end, which the statements refuse, since for
  them the yes was used.
- **The cluster replay** of the store that takes the form: whether the
  module's walk in Rama decides as the walk in memory does, on the new
  roads.

What can be run before, and was: the runs against the store as committed,
the same runs against the sketch, and the cluster replay of the store as
committed (below).

## What the run finds, 22 November 2026, afternoon

With this folder's code as committed with this page, `clojure -M:run 20000
<seed>` under seeds 1 to 5. Five runs were against the store as committed at
`3544d026`, from an export of it, and five against the sketch, all ten at once
on this machine. The histories took 234 to 250 seconds a run.

**Against the store as committed, every break is at a yes for one act but
one: 2A**, the live read that tells an asker who may not see. It is not at a
yes, and it broke at `1a2e9811` too.

| | seed 1 | seed 2 | seed 3 | seed 4 | seed 5 |
|---|---|---|---|---|---|
| kinds of break | 11 | 12 | 12 | 11 | 11 |
| at a yes for one act | 10 | 11 | 11 | 10 | 10 |
| elsewhere | 2A | 2A | 2A | 2A | 2A |

Each kind, and the history at which it first came up under each seed:

| Kind | seed 1 | seed 2 | seed 3 | seed 4 | seed 5 |
|---|---|---|---|---|---|
| the act of a yes, sent by its holder as permitted, refused `permission-for-another-act` | 1 | 1 | 8 | 1 | 3 |
| the same, when the act is the yes's own end | 15 | 20 | 6 | 2 | 3 |
| the same, sent after a refused try under a new act-id | 2 | 20 | 2 | 14 | 22 |
| the same, under each of two yeses for one act | 42 | 22 | 21 | 6 | 23 |
| the same, sent a second time under a new act-id, after the first was taken by the statements | 17 | 2 | 44 | 28 | 40 |
| the same, written another way | 12 | 20 | 13 | 42 | 6 |
| the same, in a plain step that names the yes and sends its act part for part | – | 3,169 | 498 | – | – |
| the act sent under the act-id whose uuid is the yes's own id, refused `act-not-the-one-permitted` | 7 | 10 | 36 | 28 | 26 |
| a give at a yes's place after its end or an end above it, taken by the gate; the statements refuse it, a yes's place takes one give | 10 | 67 | 9 | 4 | 40 |
| T2: an outsider writes at a yes's place after its end, in the gate alone | 108 | 337 | 10 | 201 | 40 |
| T2: the same, after the end of a permission above the yes | 1,428 | 679 | 1,638 | 284 | 349 |
| 2A: an asker who may not see is told a whole answer again | 2 | 2 | 2 | 1 | 2 |

Each kind at a yes comes from the store not taking the form yet: its gate
holds the act-id's uuid against the yes, its text is of the thing as it
arrives, and a yes's place takes a give after an end. The smallest history of
the first is three lines: asha makes the layer, gives herself a yes y2 for a
plain fact, and sends the fact under an act-id of her own, `#4`; the gate
refuses it `permission-for-another-act`, and the statements take it.

**Nothing else breaks that passed at `1a2e9811`.** The morning's outputs, kept
in that session's scratchpad with the sha256 of each source file, equal to
the files at `1a2e9811`, give under each seed five kinds that were not at a
yes: the four of "another thing under a used act-id, for the layer", and 2A.
Now there is one, 2A; the four hold by one answer for an act-id. Under no
seed did `told`, `once`, `stamps`, `kept`, `see`, `screen` or `run-again`
break, and at `1a2e9811` none of them did either. (`told` is new: it holds
what the morning's `taken` held and adds "refused" and "not mine".)

**Against the sketch, only 2A breaks**, under each seed, and nothing was taken
out of a history. What seed 5's 20,000 histories walked there: 824,901 things
sent, 764,726 decisions; 30,855 yeses given, and 13,984 more for an act that
had one; 13,891 used, 2,335 of them after another yes for the same act was
used; 16,226 acts taken under a yes; 5,758 acts sent under a used yes, each
refused; 1,937 refused tries sent again under a new act-id and taken; a give
at a yes's place again refused `a-yes-is-given-once` 6,316 times (after its
end 1,017, after an end above 37, while it stood 3,325, after its use 1,937),
and the same gives taken 1,213 times by the store as committed (1,134 after
the yes's end, 79 after an end above it). Under the other seeds the counts
of things, decisions, yeses given, used and acted under, acts under a used
yes and tries sent again are each within 5 per cent of seed 5's.

**The broken copies** (`mutants`), 3,000 histories, seed 1. Against the store
as committed, eleven of twelve were caught, exit 1, in 75 seconds. "A yes is
used more than once" was not caught, since no act under a yes is ever taken
there; "any act is taken under a yes" was caught at history 119, by the road
through the use above. Against the sketch, twelve of twelve were caught, exit
0, in 17 seconds, the two that touch a yes at history 14 each.

**T2's two roads, each made to break once.** A history of four steps was run
under the sketch with a broken `writes`, in the scratchpad: asha gives ben and
eve a permission each, ben gives asha a yes y6, and eve, an outsider of y6,
sends a plain fact. When eve's accept writes an end at y6's place, the run
says `[:outsiders :the-gate-alone :wrote-at-its-place :stands]`; when it ends
ben's permission instead and leaves y6's place alone, it says
`[:outsiders :the-gate-alone :used-it-up :stands]`. Without the broken
`writes`, no break.

**On a cluster.** `flock … clojure -M:cluster 20000 5`, against the store as
committed, 222 seconds, exit 0: the 11 kinds of the seed-5 run, each one's
smallest history sent through the store's module on a fresh in-process
cluster, up to the step at which it broke. 40 things; for each, the answer
that the door gave its sender on the cluster was the answer that the walk in
memory gave, the word of each refusal too. None was passed over. So the
copy of the walk, with this build's two changes, still decides as the
module's walk in Rama does on these histories, and the breaks at a yes are
the module's, not the copy's. For 2A, zed's function was called at the
read's opening and again at step 1, `[1 2]`, and dee's once, `[1 1]`, as in
the morning.

## Sid's own record, through the same gate

[record.clj](src/gate_histories/record.clj) sends a file of things that were
said through the gate, one act for each, with the same walk. It was run on
21 November on the whole record of the project: every line that Sid typed
since March and every text that a session wrote, 38,545 things, 88 MB of
text. The file is made outside the repository and is not in it. It was not
run again on 22 November; it loads against this walk.

```
clojure -M:record /somewhere/outside/record.edn
```

The file holds one EDN map a line for each thing said, in the order said. The
script that wrote it from the record, `make_record_edn.py`, was removed on 25
November 2026 and is in git's history.

- **The gate took every one:** 38,545 acts, 193,582 rows of facts, the stamps
  0 to 38,546 with no gap, in 21 seconds on one thread. That is the
  declaration's check, the digest and the gate's decision, in memory. It
  says nothing of Rama.
- **Three things an importer has to do,** each found by a refusal: an id of
  the store is a keyword that reads back, or a UUID, and the record's ids
  begin with a digit two times in three; one value holds 65,536 bytes at
  most, and 69 things said are longer, the longest 775,000 bytes, so they are
  cut into parts in the one act (where to cut is the question Sid raised on
  9 October: a page or a block); and who said a thing is a fact, the who of
  the act the importer, as Sid decided for the importer and Alice.
- **The form that was sent is a bet,** made to get these answers: one layer,
  one act for one thing said, five facts. It did not set `:because-of`,
  which names what an act answers, and the record knows that for each line.

## Runs before this build

- **21 November,** against piece 1 before and after it was committed
  (`2882b55b`): 40,000 histories under seeds 20261121 and 7, every statement
  held; eight mutants, eight caught. Then piece 2 was held against these
  histories by the session that built it, with R1 to R10 and 20,000
  histories, and every statement held: no history walked the roads that
  broke later.
- **22 November, morning** (`1a2e9811`), against piece 2 as committed, with
  the first form of a permission for one act: 20,000 histories under seeds 1
  to 5 found 51, 46, 50, 44 and 52 kinds of break, every one of the review's
  breaks that the model reaches and two more: a yes's act-id taken by an
  earlier thing (1A, and 1B by a sender who may only see or holds nothing),
  the text against the act (1C), an outsider's yes at the act's place
  first, another thing under a used act-id (four kinds), and a live read that
  tells an asker who may not see (2A). Eleven mutants, eleven caught; on a
  cluster, every answer the answer in memory. That run's report, with each
  smallest history, is this file at `1a2e9811`. Its open point 1, that a yes
  and an act-id meet badly, is what the form of 22 November answers.

## What it does not reach

- **Every random history on a cluster.** The walk here is the module's,
  said again in memory and checked against `module/walk.clj` step by step;
  a cluster runs only each kind's smallest history (`cluster`).
- **The store that takes the form, on a cluster.** It waits for the store.
- **What a read costs.** The review's 2B and 2C, the rounds and calls of a
  read and the bound of nine rounds, are claims of cost, not of what is
  told. The live read here is one order of the pushes; every order is the
  read path's own pure proof, on small worlds.
- **The review's sections 3 and 4**, the proofs that do not hold a statement
  and the explanations that no longer match their code: read, not run. R5's
  words on a holder's own end (section 3, point 1) are in SPEC.md now.
- **The base and the microbatch gate,** an act for a layer of the other
  gate, two layers, a layer's list and the reads through it, what points at
  a thing, a fact that names something to replace, and the bounds on a
  value and an act. Every history is one streaming layer.
- **T2 and a fact to replace.** No history writes a fact that names one to
  replace. An act that names one can be stopped by another's write at that
  fact's place; T2's second half would read that as a use of the yes.
- **T1 on a repeat written another way.** The same act written another way
  under the act-id of an act that was taken would be a repeat to the
  statements and "not mine" to the door; no step sends it.
- **A row of a kind that the copy does not know** is kept, not read.
- **After the first break, the store's own history.** A thing that breaks a
  statement is taken out of its history for both sides. The rest of the
  history is judged as if it had not been sent; the store would hold it.
- **Every history.** A run samples. A statement about all histories needs a
  proof, which is what Lean would add for the few statements worth it.

## Where it could go

Into `test-store/`, as a proof beside the store's others, if the session
that owns the store wants it there. It was kept out so that two sessions did
not write in one folder.

## This build, 22 November 2026, afternoon

The ask, in the prompt that Sid pasted as his go for the whole job: bring
the judge of the store's piece 2 to the form of a yes with an id of its own,
change nothing in `src-store` or `test-store`, and commit under the subject
"Store's line: the statements for a yes with an id of its own", which the
session that fixes the store waits for. The record shows that a session
wrote that prompt («a2cc0b7a») and the form in it (above, and SPEC.md).

### What changed

`git diff --stat 1a2e9811` of this folder and of SPEC.md, with the new file:

```
 README.md                          |  this page
 src/gate_histories/reference.clj   |  242 ++++---
 src/gate_histories/run.clj         | 1048 +++++++++++------
 src/gate_histories/walk.clj        |   88 ++-
 src/gate_histories/record.clj      |    7 +-
 src/gate_histories/sketch.clj      |   86, new
 ../store-piece-2/SPEC.md           |  209 ++++--
 6 files, this page aside: 1090 insertions, 590 deletions
```

- `reference.clj`: the statements of 22 November: R2 without the act-id and
  with the use of a yes (`:used`, `used?`); R5's words; R6, a yes's place
  takes one give (`:a-yes-is-given-once`); R9; one answer for an act-id
  (`judge`: the first answer, and "not mine" for another thing); `the-act`
  without the act-id and the depot; T2 narrowed; `place-state` says
  `:used`.
- `run.clj`: a yes with an id of its own and its text as a maker makes it
  (`yes-id-of`, `made-yes`, `hex-for-a-yes`); its steps (`yes-for-one-act`,
  `on-an-act`, `:under-its-id`), its act under any act-id of the maker's;
  `told` and `once` for one answer; T2 narrowed (`outsiders-violation`);
  each kind marked when it is at a yes, and the run's last lines; the
  `sketch` mode; the mutants put around the function in place, and a
  twelfth; a yes's act mostly a plain fact.
- `walk.clj`: rows of a kind that it does not know kept aside; `writes`
  handed every row that the walk read, as the module hands it; the list of
  the store's functions that it calls.
- `sketch.clj`, new: the sketch of a gate of the form, for a run only.
- `record.clj`: prints the rows kept aside.
- `store-piece-2/SPEC.md`: the form of 22 November; R2, R5, R6 and R9 again;
  what "used up" means; one answer for an act-id; T1 and T2; what the form
  reaches in the store, and the two functions to keep.
- This page.

Nothing in `src-store/`, `test-store/` or `deps.edn` changed.

**Where the change is in git.** The five namespaces and SPEC.md went into
`25d3b5be`, "Auth: slice b", which another session made at 13:46 while they
were staged here for this build's commit: the index is shared by the
sessions in this tree. Their bytes there are the files that ran (sha256 of
each held against the run's); nothing was lost. The commit "Store's line: the
statements for a yes with an id of its own" holds this page, and the two
commits together are the change. History was not rewritten.

### What was run, with its output

Each against an export of the store at `3544d026` put on the path by
`-Sdeps` (above), since another session works in `src-store` today.

| Command | Output | Time |
|---|---|---|
| `clojure -M:run 20000 1` to `5`, the store as committed | exit 1 each: 11, 12, 12, 11 and 11 kinds; each at a yes but 2A; the tables above | 234 to 245 s of histories; 242 to 255 s a run |
| `clojure -M:run sketch 20000 1` to `5` | exit 1 each: one kind, 2A | 239 to 250 s; 243 to 254 s |
| `clojure -M:run mutants 3000 1`, the store as committed | exit 1: 11 of 12 caught; "a yes is used more than once" not caught | 75 s |
| `clojure -M:run sketch mutants 3000 1` | exit 0: 12 of 12 caught | 17 s |
| the T2 check, a script in the scratchpad | each of T2's two roads breaks once; no break without the broken `writes` | |
| `flock … clojure -M:cluster 20000 5`, the store as committed | exit 0: 11 kinds, 40 things, every answer on the cluster the answer in memory, none passed over | 222 s |
| after one docstring fix in `walk.clj`: `run 2000 1`, `sketch 2000 1`, `mutants 3000 1` and `sketch mutants 3000 1` again | the same outputs as before it, line for line, but the seconds | |
| while building: `clojure -M:run 300 1`, `2000 1` to `5`, and `sketch 300 1`, `2000 1` | the same shapes, at fewer histories | 3 to 21 s of histories |

A run is the same for the same seed: the histories, the kinds and their
smallest histories come out again; the seconds do not. The outputs were
kept in this session's scratchpad, outside the repository; the commands make
them again.

### What is open

1. **What waits for the store's fix:** the run with no break, the broken
   copies that touch a yes, and the cluster replay of the store that takes
   the form (above).
2. **2A** is not at a yes and is not this build's: the read path joins the
   rows above a run's permission whoever holds it, and owes a whole answer
   at that step (the review's 2A; the morning's open point 4, unchanged).
3. **Where a gate writes the use of a yes.** The twelfth mutant drops what
   an accept under a yes writes at the yes's place. If the store writes the
   use elsewhere, the mutant must follow it, or it is not caught after the
   fix for a reason that is not the store's.
4. **A yes's id before its give lands.** Whoever gives first at an id is the
   yes's giver, in the statements as in the gate: an id says nothing of who
   made it. So "nobody can stand in a giver's place" holds while nobody else
   knows the id, which a refused give does not write. A giver whose give was
   refused should make a new id. The run lets another give first at such an
   id, and both sides take it, as they take any give at an empty place.
5. **T2's second half and a fact to replace.** No history writes one; an
   act that names a fact to replace can be stopped by another's write at that
   fact's place, and T2 would read it as a use.
6. **A repeat written another way** (T1 and `told` on it): no step sends it.
7. **A maker's way to make the text of a yes** is the store's: SPEC.md asks
   for one; `hex-for-a-yes` here is the judge's.
8. **The morning's open points.** 1, the act-id of a yes, and 2, an
   outsider's yes at the act's place first, are answered by the form; 3, 1C,
   by the text of the act in its one form; 4, 2A, stays; 5, the repeat
   beside R5, is gone, since a refused try is sent again under a new act-id;
   6, T2 as worded, is narrowed; 7, R5 against R2, is R5's new words; 8, the
   run's line now says "by the step that sends one".
9. **"Any act is taken under a yes"** is caught against the store as
   committed only by the road through the use, so before the fix it says
   nothing of R2's own check.

### My choices, each yours to turn

1. **A yes's place takes one give.** Once it holds a yes, its end or its use,
   nothing is given there again; another yes for the act is a new yes with a
   new id. Else whoever may act could write at an ended yes's place, which T2
   forbids.
2. **A used yes has nothing left to end:** its end is refused.
3. **The text of a yes** is SHA-256 of the one text of the one form that
   `parse` gives, without its permission. For the parse, which wants a
   permission, a first permission of the act's who in its layer is put in
   and taken out; the one form of every other part is the same whichever
   permission is there.
4. **`told` and `once`** hold one answer: the door's answer to each sender is
   held, not only "accepted"; the morning's `taken` is in `told`.
5. **T2 is held at each decision of a thing that was taken**, for each yes of
   whom its sender is an outsider. Its second half is read by the yes's own
   act sent under a fresh act-id, before the thing and after, so that it
   does not rest on where a gate writes a use.
6. **A kind is at a yes** when its thing names a yes, gives or ends one, or
   goes under a yes's id, or when it is T2.
7. **The sketch**, its four functions, and its shape: a used yes reads as
   ended. A shape for the judge's check, not for the store.
8. **The mutants are put around the function in place**, so that they work
   around the sketch too; the twelfth is new.
9. **The steps.** A yes's act is a plain fact two times in three. The roads
   are those listed under "What it does". Yes ids are `yN`, a uuid with 89
   as its high half; a step's second act-id is `#N.1`. Gone: the maker who
   made the text of her own act (there is one way to make it now), and a yes
   given again with the same id (now another yes with a new id, or a give
   at its place, which is refused).
10. **The runs held an export of `3544d026`**, not the working tree.
11. **SPEC.md:** the note of 22 November at its top, and the first form left
    to git at `8b11300c`; "What changes in the store" says what the form
    reaches and not how; T1 and T2 in a paragraph of their own.
12. **R5's words:** a permission for a run that allows acting takes an end of
    itself in its run; a yes ends itself only when its one act is its own
    end; a permission that says see takes no act.
13. **The order of the statements' checks for a yes:** its holder, its act,
    its standing, its use. Their words are the statements' own and are not
    compared with the gate's.
