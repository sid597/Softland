# Generated histories through the store's own door and gate

[Parent: the runtime's page](../PROPOSAL.md).

A check of the store by sessions that did not build it. Written on 21
November 2026 while piece 1 of the store's line was built; rebuilt on 22
November, at Sid's ask, so that the histories find piece 2's breaks
themselves, before anyone changes `src-store`: the breaks that the overnight
review found by hand
([REVIEW-store-piece-2.md](../overnight-audit/REVIEW-store-piece-2.md)), and
any other. Nothing in `src-store/` or `test-store/` was changed for it.

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
  act, ended by its giver, by the one above the giver or by its holder,
  and given again by its giver, by the one above the giver or by another;
  the permitted act sent before its yes lands, while it is ended and after
  it is given again, as permitted, with a part changed, written another
  way, in another run or under another act-id; the same thing again under
  its act-id; another thing under an act-id that was used, by the same who
  and by another; and a thing under the uuid that a yes for one act names,
  by a holder who may only see, by one who holds nothing, or by anyone.
  Some makers name the right permission, some a stale one, one raised from
  "see", or another holder's.
- **The store's way.** Each thing is made as a maker makes it and handed to
  the door's own pure code, which adds the act-id, the depot and the time
  ([walk.clj](src/gate_histories/walk.clj), `through-the-door`); the gate
  decides it as the module's walk does, check (a) and its answers among the
  steps (`decide`); and the door's own answer function says what its sender
  is told (`told`). Two askers keep a read open through the whole history,
  through the read path's own pure steps
  ([live.clj](src/gate_histories/live.clj)).
- **The statements' way.** The plainest code that says the statements
  ([reference.clj](src/gate_histories/reference.clj)): who was given what
  by whom, which gives were ended, and the first answer of each thing. It
  reads no row, no stamp and no digest.

Where the two differ, that is a break. The thing is noted and taken out of
the history for both, and the rest of the history is still played, so one
history can show more than one break. Each break is named by its kind: the
statement that broke, and the road to it (what the act's permission covers,
what took its act-id first, whether its yes was given again). For each kind,
the first history that broke so is cut down by test.check's own walk of its
rose tree, then further by taking out each step that the kind does not need,
and printed step by step ([run.clj](src/gate_histories/run.clj)).

## The statements

They stay as [store-piece-2/SPEC.md](../store-piece-2/SPEC.md) has them:
S1 to S12 and R1 to R10, each in the docstring of
[reference.clj](src/gate_histories/reference.clj). One change to how the
plain code says one of them, and three statements beside them:

- **R2's "part for part" is said in plain code.** Before, the plain code
  held an act to its yes by the store's own digest, so it could never differ
  from the gate there. Now the history writes down which act each yes was
  for, and the statements compare the act part for part: each part but the
  time and the permission, a part left out read as empty.
- **A repeat,** the one thing added, as the store's decisions of 7 October
  have it ("a repeat is answered, not decided twice"): the same thing under
  the same act-id is decided once and keeps its first answer. Nothing is
  added of another thing under an act-id that was used: it is judged as any
  thing is.
- **Two statements that the review shows are missing,** held against every
  history:
  - **T1.** An act that the statements take is taken by the gate, and its
    sender is told so.
  - **T2.** Nothing that is sent by a who other than the holder, the giver
    and those above the giver changes whether the holder's permitted act is
    taken. For each act that its holder sends under a yes for one act, the
    history before it is played again without the things of everyone else,
    through the gate and through the statements, and the act must come out
    the same.

What every history is held to, in the run's words:

| Word | What must hold |
|---|---|
| agree | The gate takes a thing exactly when the statements take it. |
| taken | T1: what the statements take, the gate takes, and the door tells its sender "accepted"; a repeat of a taken act too. |
| outsiders | T2, as above. |
| once | The same thing sent again under its act-id decides nothing and keeps its first answer. |
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
clojure -M:run                  # 2000 histories, a seed of its own
clojure -M:run 20000 3          # so many histories, this seed
clojure -M:run mutants 3000 1   # the gate with one rule broken at a time
flock ../../backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock clojure -M:cluster 3000 1
```

`run` prints what the histories walked, then each kind of break with how
many histories showed it and the first, then each kind's smallest history:
one thing a line, with who sent it, in which run, under which act-id and
permission, what the gate did, what the door tells its sender, and what the
statements say. It exits 1 when a statement broke.

`mutants` is the check of the check. It puts a broken function in place of
one rule of the gate or the read path, for the run only, and the run must
find a history that breaks a statement under it that the real code does not
break there. The store's files are not changed.

`cluster` plays the same run, takes each kind's smallest history, and sends
it through the store's module on a fresh in-process cluster: the door's
`send-act` and `answer-of`, the module's walk in Rama, and `read-live`. It
prints the door's answer to each sender beside the answer in memory, and
how often each live read's function was called. One in-process cluster at a
time runs on this machine, so it runs behind the cluster lock.

## What the run finds, 22 November 2026

Against the store as committed at `3544d026` (piece 2), with this folder's
code as committed with this page. `clojure -M:run 20000 <seed>` under five
seeds, each from nothing and without the review's histories in hand. Each
found every one of the review's breaks that the model reaches, and two more.
The first history at which each came up:

| | seed 1 | seed 2 | seed 3 | seed 4 | seed 5 |
|---|---|---|---|---|---|
| kinds of break | 51 | 46 | 50 | 44 | 52 |
| **1A**: a yes given again after an end, dead | 900 | 2,231 | 2,085 | 2,676 | 6,409 |
| **1B**: a sender who may only see takes the act-id | 4,702 | 13,500 | 6,937 | 3,204 | 1,836 |
| **1B**: a sender who holds nothing takes the act-id | 32 | 113 | 53 | 227 | 192 |
| **1C**: the act written another way | 66 | 30 | 21 | 97 | 90 |
| **1C**: the text made of the act that the maker hands the door | 221 | 246 | 82 | 226 | 121 |
| **2A**: a live read tells an asker who may not see | 2 | 2 | 2 | 1 | 2 |
| an outsider's yes at the act's place first | 287 | 187 | 543 | 380 | 649 |
| another thing under a used act-id, for the layer | 7 | 7 | 7 | 7 | 7 |

Under no seed did `see`, `once`, `stamps`, `kept`, `screen`, `run-again` or
T1's "told so" break. Each seed took between 272 and 400 seconds, the five
run at once on this machine; the sampling of the 20,000 histories is about
217 of them, the rest is cutting each kind's history down. 2,000 histories,
the default, find most of these and not reliably the rarest: the see-only
1B came up first anywhere from history 1,836 to 13,500.

The kinds come in five families. One road is split by the gate's word for
the thing that took the act-id first and by whether the yes was given
again, so the count of kinds is large and the families are few:

- **A yes's act-id was taken by an earlier thing** (24 to 28 kinds a seed):
  the act that a yes for one act names is not decided, because something
  was decided under its act-id before it: the holder's own try, its
  giver's, or an outsider's when T2 did not break with it. 1A is one.
- **The text against the act** (2 kinds): 1C.
- **Outsiders** (13 to 20 kinds): T2. 1B, the outsider's yes at the act's
  place, and two roads that come from T2's own words (below).
- **Another thing under a used act-id, for the layer** (4 kinds).
- **Unseen** (1 kind): 2A.

### The smallest histories

Each is the one the seed-5 run printed; the other seeds print the same
shapes with other names. A step is one thing that a maker sent: who sent
it, the act-id it went under (`#n` is the act-id with the uuid n), the
permission it names (`ben<asha` is a permission that asha gave ben), what
it says; what the gate did, what the door tells its sender, and what the
statements say. Step 0 is always asha making the layer, `#0`, taken.

**1A, a yes for one act given again after an end is dead.** As the review
has it: the act-id's first answer was the holder's try while the yes was
ended, and a yes given again by one above the giver, under another id, can
never be used.

| step | who | act-id | under | what | gate | door | statements |
|---|---|---|---|---|---|---|---|
| 1 | asha | #2 | asha | gives ben a permission | accepted | accepted | taken |
| 2 | ben | #4 | ben<asha | gives asha a yes for act #5 | accepted | accepted | taken |
| 3 | ben | #6 | ben<asha | ends it | accepted | accepted | taken |
| 4 | asha | #5 | the yes, asha<ben<asha | the act | refused, permission-ended | refused | refused |
| 5 | asha | #10 | asha | gives asha a yes for act #5 again, asha<asha | accepted | accepted | taken |
| 6 | asha | #5 | the yes, asha<asha | the act | not decided: the act-id has an answer | **not mine** | **taken** |

Its siblings, each its own kind: the act-id was taken first by the
holder's try before the yes landed, by the act with a part changed, by the
act sent and taken under an earlier yes, by a give or an end that she sent
under it; or by the giver's own thing.

**1B, a sender who may only see takes the act-id.** T2 breaks in the gate
alone: without ben's thing, the gate takes asha's act; the statements take
it either way.

| step | who | act-id | under | what | gate | door | statements |
|---|---|---|---|---|---|---|---|
| 1 | asha | #2 | asha | gives asha a yes for act #3 | accepted | accepted | taken |
| 2 | asha | #4 | asha | gives ben a permission that says see | accepted | accepted | taken |
| 3 | ben | #3 | ben<asha (see) | gives asha a permission | refused, permission-does-not-allow-acting | refused | refused |
| 4 | asha | #3 | the yes | the act | not decided: the act-id has an answer | **not mine** | **taken** |

**1B, a sender who holds nothing.** The same in four steps: zed, who was
never given anything, sends a thing under `#3`; it is refused
permission-does-not-stand; asha's act is not decided. The uuid is in the
yes's own id, a fact of the layer.

**1C, the act written another way.** asha gives herself a yes for act #3,
then sends the act with its facts as a list and `:subjects #{}` and
`:based-on {}` given empty: the act part for part. The gate refuses it
act-not-the-one-permitted, since its text of the act is of the act as
written; the statements take it.

**1C, the text that a maker can make.** The same three steps, the act as
the yes named it, the yes's text made of the act that the maker hands the
door, which holds no act-id and no depot. The gate makes the text of the
thing as it arrives, with the act-id and the depot that the door added, and
refuses the act act-not-the-one-permitted. A maker can make the right text
only by building, beside the act she sends, the map that the door will
append (`digest/hex-for-its-permission` says so); the door refuses that map
if she sends it (`:add-its-parts`), and has no function that makes the text
of an act and its act-id.

**2A, a live read tells an asker who may not see.** One step: asha gives
herself a permission for run-1 that says see. Zed asks in run-1 and may not
see the layer before or after; her read is given a whole answer again,
`{:anew {:layers [:lab], :left-out [{:layer :lab, :why :may-not-see}],
:place {:holds :no-fact}}}`, which tells her that something of the layer
was written. The read path joins the rows above that run's permission
without asking whose it is, and a step that joins a source owes the whole
answer. On a cluster, her function is called a second time at that step.

**Another thing under a used act-id.** One step: asha sends a plain fact
under `#0`, the act-id of the act that made the layer. The gate does not
decide it, the door tells her "not mine", and the statements take it,
since nothing in them says what becomes of another thing under an act-id
that was used. The same under a permission for the layer by another who,
and after a refusal.

**An outsider's yes at the act's place first** (not in the review). eve,
given a permission by asha, gives asha a yes for act #5 beneath her own;
asha's own yes for act #5 is then refused holder-has-a-permission (R6: an
act has at most one permission that stands, whoever holds it), and asha's
act under her own yes is refused. Without eve's things it is taken. T2
breaks in the gate and in the statements alike: the road is the spec's, a
holder who may act can give a permission at any act's uuid. Here the yes
that came first was for the same holder; it can be for anyone.

**Two roads that come from T2's own words.** In the gate and in the
statements: ben ends his own permission; asha's act that gives ben one
again and cal one is taken whole (S6, S10), and so cal can give asha the
yes. Without ben's end, asha's act is refused whole and no yes stands. So an
outsider's thing changed whether the act is taken, through an insider's
act. And in the statements alone, when an outsider's yes for the same act
and the same holder stood at the place first, or when the history without
the outsiders holds a break of its own.

### The mutants

`clojure -M:run mutants 3000 1`, 12.6 seconds, exit 0: each of the eleven
broken rules was caught by a break that the real code does not have.

| Broken on purpose | Caught at history | By |
|---|---|---|
| S3: a permission stands when its ids are in place | 58 | see |
| S4, S5: whoever may act ends any permission | 2 | agree |
| S6: a holder whose permission stands is given another | 1 | agree |
| S7: a permission is given beneath any permission | 1 | agree |
| S8: whoever may act writes a setting | 21 | agree |
| S9: a holder who may only see acts | 6 | agree |
| S11: a read lets a holder see after her permission ended | 1 | see |
| S12: a read lets nobody but members see an open layer | 90 | see |
| R1, R9: an act is taken under a run's permission in any run | 109 | agree |
| R2: any act is taken under an act's permission | 18 | agree |
| R8: a read sees under any run's permission, whoever holds it | 2 | see |

The eight that the run caught before this build are among them. S11 and
S12 had gone uncaught since piece 2 changed the read path's `standing`:
the statement `see` was switched off then. It is asked again, in each run.
R8 is new.

### On a cluster

`flock … clojure -M:cluster 20000 5`, 409 seconds, exit 0: the 52 kinds of
the seed-5 run, each one's smallest history sent through the store's module
on a fresh in-process cluster, up to the step at which it broke. 270
things; for each, the answer that the door gave its sender on the cluster
was the answer that the walk in memory gave, the word of each refusal too.
None was passed over. So on these histories the walk in memory decides as
the module's walk in Rama does, check (a) and its answers among it, and 1A,
1B and 1C are the module's breaks, not the model's.

For 2A, zed's function was called at the read's opening and a second time
at step 1, when asha gave herself a permission for run-1: `[1 2]`; dee's,
asking in run-2, once: `[1 1]`. The calls of the live reads are printed
beside each history and not held against the model: Rama's pushes come as
they come.

### Why 20,000 runs found nothing before

The review says why: no history used an act's id twice, none gave a
permission for one act more than once, and none held R3, R5's "given again"
or R6 for an act. And the plain code held an act to its yes by the gate's
own digest, so 1C could not differ, and nothing asked what a live read
tells. What the 20,000 histories of seed 5 walked:

- 795,838 things sent; 696,877 decisions of the gate, 144,568 taken;
  11,654 things taken out of their history for a break.
- A yes for one act taken 33,273 times; ended 11,857 times; given again by
  its giver 2,479 times, by one above the giver 1,439, by another 494.
- The permitted act sent by its holder before its yes landed 34,741 times,
  while it was ended 2,287, after it was given again 897, while it stood
  18,843.
- A thing under the uuid that a yes names, by the step that sends one (a
  holder who may only see, one who holds nothing, or anyone): decided 6,879
  times, 170 of them refused permission-does-not-allow-acting (a sender who
  may only see), 5,110 permission-does-not-stand (one who holds nothing, or
  a stale permission), 741 taken; 27,519 more came after the act-id had an
  answer. The run prints this line as "by another"; anyone is nearer.
- The same thing again under its act-id 31,356 times; another thing under
  a used act-id 29,263 times by the same who and 38,342 by another.

## Sid's own record, through the same gate

[record.clj](src/gate_histories/record.clj) sends a file of things that were
said through the gate, one act for each, with the same walk. It was run on
21 November on the whole record of the project: every line that Sid typed
since March and every text that a session wrote, 38,545 things, 88 MB of
text. The file is made outside the repository and is not in it. It was not
run again on 22 November; it loads against this walk.

```
python3 make_record_edn.py /mnt/data/projects/softland-memory/base /somewhere/outside/record.edn
clojure -M:record /somewhere/outside/record.edn
```

- **The gate took every one:** 38,545 acts, 193,582 rows of facts, the stamps
  0 to 38,546 with no gap, in 21 seconds on one thread. That is the
  declaration's check, the digest and the gate's decision, in memory. It
  says nothing of Rama.
- **Three things an importer has to do,** each found by a refusal:
  1. An id of the store is a keyword or a UUID. A string is refused, and so
     is a keyword that does not read back: the record's ids begin with a
     digit two times in three. The importer gives each id a form that the
     store takes.
  2. One value holds 65,536 bytes at most. 69 things said are longer, the
     longest 775,000 bytes. They are cut into parts, each a fact, in the one
     act; the longest is an act of 31 facts. Where to cut is the question
     Sid raised on 9 October: a page or a block.
  3. Who said a thing is a fact, and the who of the act is the importer,
     as Sid decided for the importer and Alice.
- **The form that was sent is a bet,** made to get these answers: one layer,
  one act for one thing said, five facts. It set `:claimed-when` to the time
  when the thing was said. It did not set `:because-of`, which is the part
  of an act that names what it answers, and the record knows that for each
  line.

## Runs before piece 2

On 21 November, against the gate of piece 1 before and after it was
committed (`2882b55b`): 40,000 histories under seeds 20261121 and 7, every
statement held; the case that the gate's rule of stamps exists for (an act
under a permission whose ids were all in place, though one above it had
ended and been given again) came up 517 times and was refused each time;
eight mutants, eight caught. Then piece 2 was held against these histories
by the session that built it, with R1 to R10 and 20,000 histories, and every
statement held: no history walked the roads above.

## What it does not reach

- **Every random history on a cluster.** The walk here is the module's,
  said again in memory and checked against `module/walk.clj` step by step;
  a cluster runs only each kind's smallest history (`cluster`).
- **What a read costs.** The review's 2B and 2C, the rounds and calls of a
  read and the bound of nine rounds, are claims of cost, not of what is
  told. The live read here is one order of the pushes; every order is the
  read path's own pure proof, on small worlds.
- **The review's sections 3 and 4**, the proofs that do not hold a statement
  and the explanations that no longer match their code: read, not run.
- **The base and the microbatch gate,** an act for a layer of the other
  gate, two layers, a layer's list and the reads through it, what points at
  a thing, a fact that names something to replace, and the bounds on a
  value and an act. Every history is one streaming layer.
- **T1 on a repeat written another way.** The same act written another way
  under the act-id of an act that was taken would be a repeat to the
  statements and "not mine" to the door; no step sends it.
- **After the first break, the store's own history.** A thing that breaks a
  statement is taken out of its history for both sides. The rest of the
  history is judged as if it had not been sent; the store would hold it.
- **Every history.** A run samples. A statement about all histories needs a
  proof, which is what Lean would add for the few statements worth it.

## Where it could go

Into `test-store/`, as a proof beside the store's others, if the session
that owns the store wants it there. It was kept out so that two sessions did
not write in one folder.

## This build, 22 November 2026

Sid's ask, as a build and with his go for the whole of it: make the store's
generated histories find piece 2's breaks themselves, before anyone changes
`src-store`, and fix nothing in the store. Done in this folder alone.

### What changed

Step 0 committed the 583 lines that the session that built piece 2 left
uncommitted on 21 November, unchanged (`fc43d248`). Since then,
`git diff --stat fc43d248` of this folder, with the two new files:

```
 README.md                          |  this page
 deps.edn                           |    8 +-
 src/gate_histories/reference.clj   |  211 ++-
 src/gate_histories/run.clj         | 1364 ++++++++++++++------
 src/gate_histories/walk.clj        |   88 +-
 src/gate_histories/live.clj        |  183, new
 src/gate_histories/cluster.clj     |  147, new
```

- `reference.clj`: R2's part for part in plain code (`the-act`, and
  `*act-named*`, what each yes was for); the repeat, `once`; the answer
  that each thing's sender is to be told; `inside`, for T2; `given-again?`
  and `place-state`. The docstring holds T1, T2 and `once` beside S1 to S12
  and R1 to R10, which stay as SPEC.md has them.
- `walk.clj`: the door's own additions on the way in, and the door's own
  answer on the way back. The code for a store without piece 2 is gone.
- `live.clj`, new: a live read through the read path's own pure steps.
- `run.clj`: the steps of STEP 1; the statements `taken`, `outsiders`,
  `unseen` and `screen`, and `see` asked again, in each run; a history
  played to its end, a thing that breaks a statement taken out of it; kinds
  of break, and the smallest history of each; the mutants counted against
  the real code, and an eleventh mutant.
- `cluster.clj`, new, and the `:cluster` alias in `deps.edn`: each kind's
  smallest history through the store's module on an in-process cluster.

Nothing in `src-store/` or `test-store/` changed.

### What was run, with its output

| Command | Output | Time |
|---|---|---|
| `clojure -M:run 2000 20261121`, the code of 21 November, before this build | every statement held; "the statement `see` was not asked" | 6.9 s |
| `clojure -M:run mutants 3000 1`, the same | 8 of 10 caught; S11 and S12 not caught | |
| `clojure -M:run 20000 1` to `5` | exit 1 each: 51, 46, 50, 44 and 52 kinds; the table above | 400, 272, 304, 312, 288 s |
| `clojure -M:run mutants 3000 1` | exit 0: 11 of 11 caught | 12.6 s |
| `flock … clojure -M:cluster 300 5` | exit 0: 14 kinds, every answer on the cluster the answer in memory | 50 s |
| `flock … clojure -M:cluster 20000 5` | exit 0: 52 kinds, 270 things, every answer on the cluster the answer in memory | 409 s |

A run is the same for the same seed: the histories, the kinds and their
smallest histories come out again; the seconds do not. The outputs were
kept in the session's scratchpad, outside the repository; the commands make
them again.

### What is open

1. **The act-id of a yes for one act: 1A, 1B and another thing under a used
   act-id are one question.** The gate gives an act-id one answer, that of
   whatever it decides first under it, a refusal too, and tells every other
   sender under it "not mine" (check (a)). A yes for one act names the uuid
   of its act's act-id, so the uuid is fixed before the act is sent, and it
   is in a fact of the layer. Together: whatever is decided first under that
   uuid, the holder's own early or wrong try or anyone's thing, takes the act
   for good. My position. For an act under a permission for the layer or a
   run the gate is right and the spec is silent: an act-id names one act
   (each fact's row holds its act's uuid), and "another thing under an
   act-id that has an answer is not decided, and its sender is told so"
   belongs in the statements; it is not added here, by the ask, and added as
   it stands it would make 1A and 1B agree with the gate. For a yes for one
   act the two rules meet, and I would take the uuid out of the yes: a yes
   names its act by the text of the act alone, in its one form, and an
   accept under it ends it, so that it is used once. The holder then sends
   under an act-id of her own, as for any act: an early or wrong try costs
   her nothing, nobody can take her act-id first, and the text is one that
   she can make of what she hands the door (1C's first half) and that
   leaves the act's syntax out (1C's second half). What it costs: one row
   more written for each accept under a yes, the yes's end; and the spec's
   form of a yes, the store's place for it and R2 change. It is a form of a
   stored record and a rule of the store: yours to decide.
2. **An outsider's yes at the act's place first.** R6 with a yes that sits on
   the act's uuid alone: whoever may act can give a yes for any act whose
   uuid she knows, and so block its own giver, or keep a yes that was ended
   from being given again. It is in the statements, not only the gate. Where
   a yes sits is the same question as 1: a yes that sits on its holder and
   its act's text can be given first by another, and the holder can still
   act under it.
3. **1C,** under 1 if 1 is taken; else two changes of the door and the gate:
   a function of the door that makes the text of an act and its act-id, and
   the text taken of the act in its one form.
4. **2A.** The read path's `reading` wants the rows above the run's
   permission whoever holds it (`sources.clj`, `rights`, `:chain`), and a
   step that joins a source owes the whole answer. Wanting that chain only
   when the row's permission is the asker's would close it; the read path's
   own pure proof would need a world with another holder's run.
5. **The repeat beside R5.** By the repeat statement, a permitted act that
   was sent and refused before its yes stood, before it landed or while it
   was ended, keeps that refusal; a yes given again with the same id can
   never be used. The statements agree with the gate there, so those
   histories do not come out broken, though the review counts them (1A, "or
   pa1 again", "an early arrival before pa1 landed"). I added no statement to
   break them, as asked. Under 1 the question goes away.
6. **T2 as worded is stronger than the spec holds.** It breaks through an
   insider's act that an outsider's thing made the gate take or refuse whole
   (S6, S10), and when an outsider's yes for the same act and the same
   holder stood first. A narrower T2 keeps what it is for: nothing that an
   outsider sends takes the act-id of a holder's permitted act, or writes at
   its place. The run holds T2 as you worded it.
7. **R5's "under that permission itself" against R2,** for a yes for one act
   (the review, section 3): both cannot hold. The plain code checks R2 first,
   so no history breaks here.
8. The run prints "by another" for the step that sends under a yes's uuid,
   which can be sent by anyone.

### My choices, each yours to turn

1. **"The store's own walk (`walk/decide`) with its answers"** I read as
   `gate-histories.walk/decide`, the module's walk said again in memory with
   check (a) and the answers table, which I checked step by step against
   `module/walk.clj`; and I added the cluster replay of each kind's
   smallest history through the module's real walk. If you meant every
   random history on a cluster: the replay here took 50 seconds for 14
   histories, a fresh cluster's launch in each, so thousands of histories
   would want one cluster and a layer of their own for each, one cluster at
   a time on this machine.
2. **R2's "part for part"** is Clojure's `=` on the act's parts, a part left
   out read as empty: a list and a vector of the same facts are one act, and
   5 and 5N one value. SPEC.md also defines a yes's text over "the thing as
   it arrives", which keeps the maker's syntax; its two sentences disagree,
   and the plain code takes R2's.
3. **"The same thing"** of the repeat is the same act part for part under
   the same permission.
4. **What each yes was for** is written down by the history that made it
   (`*act-named*`, by the act's uuid and the text).
5. **Two makers of a yes's text:** of the act as it will arrive, which is
   what `digest/hex-for-its-permission` asks for, nine times in ten; of the
   act that she hands the door, one in ten.
6. **`unseen` and `screen`** are statements of the read path's own, taken
   from its docstrings so that the model reaches 2A: SPEC.md says who may
   see, not what a read tells.
7. **A thing on which the gate and the statements differ is taken out** of
   its history for both, and the history goes on, so that one history shows
   more than one break. What comes after is judged on a history that the
   store would not hold.
8. **A kind of break** is its statement and its road: what the act's
   permission covers, who took its act-id first and the gate's word for that
   thing, whether its yes was given again. That makes 44 to 52 kinds a seed;
   fewer words would make fewer kinds.
9. **T2's history without the outsiders** takes out every thing of whoever
   is not inside the permission, and in it, too, a thing on which the gate
   and the statements differ.
10. **The steps' weights and two leanings:** a step names mostly the last
    yes or thing or the one before; a yes's giver is often one who was given
    a permission; a sender under a yes's uuid names a yes that the gate
    took. Tuned until the review's roads came up within 20,000 histories
    under five seeds; the review's histories were never handed in.
11. **`see`** is asked again, in no run and in each run.
12. **An eleventh mutant,** R8.
13. **The code for a store without piece 2** is gone: piece 2 is committed.
14. **The live askers:** zed in run-1, who holds nothing, and dee in run-2;
    each asks one place.
15. **This page stands on 20,000 histories a seed;** 2,000 stays the
    default, and does not find the rarest roads under every seed.
