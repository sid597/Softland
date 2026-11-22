# The store's line, piece 2, repaired: a yes for one act has an id of its own

[Piece 2 as built](BUILD-store-piece-2.md) ·
[its spec, brought to the form](store-piece-2/SPEC.md) ·
[the review that broke it](overnight-audit/REVIEW-store-piece-2.md) ·
[the judge: the generated histories](gate-histories/README.md)

**22 November 2026.** One Claude session, on the prompt that Sid pasted as his
go for the whole job. This page says what changed, each fork taken, each command
with what it printed, what is open, and each choice of the session's for him
to turn. A session does not judge its own build: a fresh session does that,
from the diff.

## What he asked for

The ask is the prompt that Sid pasted on 22 November as his go for the whole
job (‹526c4049›). By the record a session wrote it («a2cc0b7a») and he never
typed it; so its lines are quoted here as the prompt's, not as his words. It
asks to "fix piece 2 of the store's line in `src-store`, so that every break
the review found is gone, judged by checks that this session does not write."
The form in it, which it calls "a session's form, and mine to turn": a yes
for one act gets an id of its own,
`[who layer granted-in above :see-and-act [:act yes digest]]`, where `yes` is
a UUID that its giver makes and the permission sits at that entity under
`:softland.store/act-permission`; `digest` is of the act as its maker made it,
in the one form that `act/parse` gives, with its who and its session, without
its act-id, its depot, its time and its permission; the act is sent under any
act-id of its maker's; the gate uses the yes up when it takes the act; a try
that is refused uses nothing and is sent again under a new act-id; two yeses
for one act can stand at once, each used once; an act-id names one act and
gets one answer, whoever sent first.

The judge is another session's: `store-piece-2/SPEC.md` and `gate-histories/`
as committed in `25d3b5be` and `e9bc5ac1` ("Store's line: the statements for
a yes with an id of its own"). This build changed neither, nor the judge's
copy of the walk (`gate-histories/src/gate_histories/walk.clj`): the store's
walk took the form with no change that the copy could not follow, since the
use of a yes is a row of a fact and the copy writes and reads back rows of
facts.

## What changed, by part

**The yes, in the gate and the door.**

- `door/digest.clj`: `act-for-a-yes` and `hex-for-a-yes`, in the place of
  `hex-for-its-permission`. The digest that a yes names is SHA-256 over the
  one text of the act in the one form that `act/parse` gives, without its
  act-id, its depot, its time and its permission. It takes a maker's act,
  with a permission or none, or a thing as it arrives; it reads the act's
  parts with `act.clj`'s own declaration, less the permission, so `5` and
  `5N`, facts as a list or a vector, and an empty part left out or given are
  one act and one digest. It is the door's one call for a maker who asks a
  yes, and the gate's: one function (the review's 1C, both halves).
- `gate/decision.clj`: `hex-for-its-permission`, with its two arguments,
  calls it on the thing that arrived. `standing` holds no act-id's uuid
  against a yes; it refuses an act under a used yes `:permission-used-up`
  (`used-up?`). An accept under a yes writes the yes's use
  (`use-of-its-yes`): an end at the yes's place, the act's own row one index
  after its last fact, naming the yes as the permission that its act named;
  none when the act is the yes's own end, whose own row there is that end. A
  give of a yes at a place that holds any row is refused
  `:a-yes-is-given-once`, and its chain is not read (`chains-needed`).
  `:permission-for-another-act` is gone: a yes for another act is
  `:act-not-the-one-permitted`. `writes` keeps its seven arguments.
- `act.clj`, `door/declaration.clj`, `door.clj`, `gate/chain.clj`,
  `module/walk.clj`, `record/rows.clj`: no code but what is above; their
  docstrings now say the form, the yes's own id, the use, and the costs (one
  reading of the act's parts and one SHA-256, and one row more written, for
  an act under a yes; 771 writes at most where 770 were).

**The read path.**

- `read_path/sources.clj`: `askers-run-row`. The rows above the permission
  of the run that a query names are wanted only when that permission is the
  asker's (the review's 2A). The derivation of how many rounds a read takes
  is written again, in the place of the ranks that the review showed false
  (2C): asked once, a given list settles in 4 rounds and a start layer and
  its list in 6, whatever is written meanwhile; kept live, 5 and 8 on tables
  that do not change, and a list that gains a layer while it opens asks 5
  more.
- `read_path.clj`: `max-rounds` set from that derivation, `{:once 6 :live
  16}`, where it was 9 for both; the live loop now counts rounds that join,
  so a read that settles at the bound opens. The cost of a read in a run is
  said again from the derivation (2B): the rows above her run's permission
  ride the round that reads those above her own; one round more, with its
  calls, when she holds none in the layer or its first.
- `read_path/answer.clj`, `read_path/layers.clj`, `read_path.clj`: a live
  read's mark now holds what its asker was last told of each layer, seen or
  left out and why (`mark-of`), beside whether the whole answer is owed; and
  a change of her rights in a layer that she was told she may not see, and
  may not see after it, tells her nothing. A layer whose standing the change
  leaves not known yet waits in the mark until its rows are given. The
  generated histories found this road at 2,000 histories once 2A was fixed:
  a holder ends her own permission and gives the asker one beneath it in the
  same act. `read-live` marks a read when it opens.

**The pages.** The eight explanations of the review's section 4, each with
its code; `src-store/MODEL.md`; the maps of the store, `door/`, `gate/` and
`read_path/`; the proofs' map. In `src-store/README.md` only explanations
changed: the paragraph on what a permission covers and the tables of words;
no line of a "Decisions of" section.

**The proofs.** Each statement that the review's section 3 shows no proof
held now has a check, and the review's two probes are written again for the
form (below).

- `pure_proof.clj`: `yes-cases`, a line of 42 things under yeses: a try
  before the yes lands, with another value or in another run, refused and
  using nothing; the same thing again under its act-id not decided; the act
  written another way taken under any act-id; used up; nothing to end; its
  id takes no give again; two yeses for one act, each used once; ended by
  its giver, by one above the giver, by its holder under her permission for
  the layer; never under another yes; its own end its use; a yes gives
  nothing; R3 with the end above and the give again above. The digest's
  cases, and the rows of the use. `review-cases`, the probe's histories.
  `covers-cases` keeps the run's line and loses its act part.
- `module_proof.clj`: the group of a run and an act, its act part written
  again for a yes on a cluster; the base's group at line 3347 written again
  to hold what its page says (below).
- `door_proof.clj`: `a-permission-for-a-run-and-a-yes-for-one-act-through-the-door`.
- `run_again_proof.clj`: the hand's line under a run and yeses; the fixture
  made again.
- `read_path_pure_proof.clj`: the rounds of a read, 2A, an asker whose own
  permission does not stand, R8 for a yes, the probe's read histories.
- `read_path_proof.clj`: the cluster probe's four receipts.

## The review's findings, each with its fix and its check

| Finding | What fixes it | What holds it |
|---|---|---|
| 1A, a yes given again after an end, and a refused try, leave a yes that can never be used | The form: a yes on an id of its own; a try under any act-id, refused, uses nothing; a yes's place takes one give, and another yes for the act takes a new id | `yes-cases`, `review-cases` (probe 1A); the module's group; receipt 1 of the cluster probe; the judge |
| 1B, a holder who may only see, or one who holds nothing, takes the act-id first | The yes's id is no act-id: nothing sent under it touches the yes | `review-cases` (probe 1B); receipt 2; the judge (T2) |
| 1C, the maker's act against the door's text, and the act's syntax | `hex-for-a-yes`, of the act's one form, the maker's and the gate's one function | `yes-cases`, `review-cases` (probes 2, 6D); the door's proof; the module's group (written another way); the judge |
| 2A, another holder's permission for her run joins her pushes and tells her a whole answer | `askers-run-row`; and the mark: a change of her rights in a layer that she was told she may not see tells nothing | the read path's pure proof (three worlds, every order; probe 4); receipt 3 |
| 2B, "rows more, and no call more" false | The cost said again from the derivation | `the-rounds-of-a-read-are-the-chains-of-its-sources`; probe 6A |
| 2C, the bound of six rounds and nine false | The derivation written again; `max-rounds` 6 and 16 from it | the same check; probes 3 and 6B; receipt 4 |
| Section 3, 1: R5's own end of a yes and of a permission that says see | As SPEC.md now has R5: a yes ends itself only when its act is its end; a permission that says see takes no act | `yes-cases`; `review-cases` (probe 6C) |
| Section 3, 2: R6, R5 given again, and R3 for a yes, held by no proof | Checks | `yes-cases` (ended, above ended, given again above, given once); the module's group (R3 above); the run-again line |
| Section 3, 3: no send under a run's or an act's permission through the door | A check | the door's proof |
| Section 3, 4: R8 for an act's permission in no proof, and no marked key read in a run in every order | Checks | `a-yes-for-one-act-lets-nobody-see`; probe 5's worlds A to C, marked, in a run, every order |
| Section 3, 5: the base's check at `module_proof.clj:3347` does not test what its page says | Written again: in one batch of the base, a run's own end, its give again, the act under a yes under an act-id of the maker's, another thing under that act-id with another value (not decided, "not mine"), the act again (used up), a second yes, another act under it | the module's proof |
| Section 4, the eight explanations | Each changed with its code: `chain.clj` (1), `decision.clj` (2), the store's map (3), `rows.clj` (4), `layers.clj` (5), `sources.clj` (6), `answer.clj` (7), `read_path.clj` (8) | read |

## The review's two probes, written again

Both probes call `digest/hex-for-its-permission`, which the form takes away,
and pin a yes to the uuid of its act's act-id: neither loads against this
store. As the ask says, their histories are written again for the form, in
checks of this session's own, each with the probe's who, layers, acts and
order:

- `overnight-audit/probes/break_piece_2.clj`: probes 1A, 1B, 2, 6C and 6D in
  `pure_proof.clj`, `review-cases`; probes 3, 4, 5, 6A and 6B in
  `read_path_pure_proof.clj`, `the-reviews-probe-of-the-read-path-written-again`.
- `overnight-audit/probes/cluster_probe_piece_2.clj`: its four receipts in
  `read_path_proof.clj`, `the-reviews-cluster-probe-written-again`, on the
  cluster of that proof. Receipt 4 injects one write while the longest live
  read opens, as the probe did.

## The forks, and my choices, each yours to turn

1. **Where "used up" is kept: an end at the yes's place**, written by the
   accept in the decision's one write, naming the yes as the permission that
   its act named, with the act's own uuid and an index one after its last
   fact (`decision/use-of-its-yes`). So everything that reads a permission's
   place reads a used yes as one that no longer stands: no new table, no new
   kind of row, the same row in both gates and on every task of the base,
   and the same when the gates run again. The other ways: a row of a kind of
   its own beside the yes, which the judge's copy keeps aside and does not
   read back; a field in the yes's row. A cost of this way: the use's fact
   id, `[act-id n]`, names no fact of the act as it is in the log.
2. **A yes's place takes one give**, whatever it holds (`:a-yes-is-given-once`).
   SPEC.md's R6 says the same since 22 November; the gate's word is mine.
3. **The words**: `:permission-used-up`, `:a-yes-is-given-once`; and
   `:permission-for-another-act` retired, since a yes for another act is
   one whose digest is another (`:act-not-the-one-permitted`).
4. **The door's call for the digest is `softland.store.door.digest/hex-for-a-yes`**,
   one function that the gate calls too, and it takes no door. A maker who
   holds an act with no permission yet calls it as she is; one with the door's
   three parts is read without them. Not a second function in `door.clj`.
5. **The gate makes the digest of the thing as it arrived**, read again with
   `act.clj`'s declaration, not of the act that the declaration's check made,
   whose plain values are text by then: one reading of the act's parts more,
   only under a yes.
6. **The use is not counted in `max-act-bytes`**: one row more of who and the
   yes, said in the docstring, as the facts by value already are.
7. **`max-rounds` is `{:once 6 :live 16}`.** 6 and 8 are derived; the 8 more
   that a live read is given for writes while it opens are mine. The live
   loop now stops only a read that still has something to join: a read that
   settles in its last round opens.
8. **A live read's mark remembers what its asker was told of each layer.**
   The read path said that it "keeps no value between two changes" and "holds
   no answer"; to keep its promise that an asker who may not see a layer is
   told nothing of it, not even an answer that says what it said, a step
   must know what she was told of that layer. Without it, two changes taken
   before either is handled each read the other's new value, and only the
   moves of their rows told the read that something changed, which is also
   what told her answers again. So the mark holds, of the last whole
   answer, each layer's standing, seen or left out and why, and nothing of
   its facts. It is a fork against a design line of the read path, not one
   of your decisions; I took it because the other way leaves the promise
   broken. The other ways: weaken the promise to "told no fact of it"; or
   hold the rows of an ended permission's chain, which I built first and
   which the every-order proof broke.
9. **A read whose mark holds nothing**, as the judge's own live read, which
   opens with none, takes the meaning before each change as what was told.
   That is right where each change is handled before the next is taken, as
   in the judge's one order; the store's read path marks every read when it
   opens, and a check shows that a read with no mark can end wrong in some
   orders.
10. **2A is closed twice**: the rows above another holder's run permission are
    never wanted (the ask), and a change of rights in a layer she was told she
    may not see tells nothing (the general rule, which the judge needed). A
    check shows that each alone hands her nothing on 2A's own road.
11. **The review's probes written again** in checks of mine, as listed above,
    since neither loads.
12. **The fixture made again**, on purpose, for the rules above (below).

## What is open

1. **The judge's cluster replay holds nothing on this store.** It sends the
   smallest history of each kind of break through the module; with no break
   it sends none, and says "Every answer on the cluster was the answer in
   memory" of nothing. So the judge does not hold its copy of the walk
   against Rama on the new roads. The store's own proofs do, on the cluster:
   the module's two groups, the door's, the run-again line in both gates, and
   the cluster probe's receipts. A replay of a sample of the generated
   histories, and not only of broken ones, would close it; it would be the
   judge's to add.
2. **The judge's live read opens with no mark** (`gate-histories/live.clj`,
   `open`), so it holds the read path's promise only in its one order
   (choice 9). Marking it with `softland.store.read-path.answer/mark-of` of
   its first answer, as `read-live` does, would make it drive the read path
   as the store does. It is the judge's file.
3. **`BUILD-store-piece-2.md`** still says the first form, and "rows more, and
   no call more" (its lines 70 and 71); it is not this build's page. SPEC.md
   holds the form since 22 November, and this page says what the store now
   does.
4. **The name `decision/hex-for-its-permission`** stays, with its two
   arguments, as the judge asks; it is now the digest of a yes, of the act.
5. **What no run reaches.** A yes in a batch of the base, or an event, that
   fails and runs again, its use set back with it; a yes given before the
   module is started again, held after it; what the digest of a yes costs on
   a large act; a yes in the base through the door; the rounds of a live
   read under more than one write while it opens. Each is in the proofs' map.

## What was run, with its output

On the final code, 22 November 2026 from 14:12 IST; the cluster proofs one
after the other behind the machine's cluster lock, which two other sessions'
runs held first. The logs are in this session's scratchpad, outside the
repository; the commands make them again.

| Command | Output | Time |
|---|---|---|
| `clojure -M:store -m softland.store.act-proof` | 166 checks, none failed | 0.5 s |
| `clojure -M:store -m softland.store.pure-proof` | 739, none failed | 3.2 s |
| `clojure -M:store -m softland.store.read-path-pure-proof` | 244, none failed | 15.7 s |
| `flock … clojure -M:store-module -m softland.store.door-proof` | 283, none failed | 378 s, the wait for the lock in it |
| `flock … clojure -M:store-module -m softland.store.module-proof` | 482, none failed | 54 s |
| `flock … clojure -M:store-module -m softland.store.read-path-proof` | 610, none failed | 72 s |
| `flock … clojure -M:store-module -m softland.store.run-again-proof`, on the fifth making of the fixture | 42 passed, 6 failed, all in the two checks of the fixture: every row that differs, 5 of 559 with 4 tasks, is of the two acts that the fifth making's hand sent under a permission for one act of the first form, now refused `act-not-the-one-permitted`, and their facts unwritten | 73 s |
| `flock … clojure -M:store-module -m softland.store.run-again-proof make-fixture` | the sixth making: `acts.edn`, 123,504 bytes, 387 records; `tables.edn`, 106,629 bytes, 563 rows; every "held to" of the making true, the line under a run and yeses among them | 54 s |
| `flock … clojure -M:store-module -m softland.store.run-again-proof`, on the sixth making | 48, none failed | 118 s, the wait for the lock in it |
| `clojure -M:run 20000 1` to `5`, in `gate-histories/`, five at once | exit 0 each: no kind of break, none at a yes and none elsewhere; no row kept aside; 199 to 204 s of histories a run | 201 to 206 s |
| `clojure -M:run mutants 3000 1` | exit 0: 12 of 12 broken copies caught, the two at a yes at history 14 each | 14.7 s |
| `flock … clojure -M:cluster 20000 5`, in `gate-histories/` | exit 0: "20000 histories, seed 5: 0 kinds of break … Every answer on the cluster was the answer in memory". With no break it sends no history: it holds nothing here (What is open, 1) | 277 s |

What the seed-5 histories walked, as the judge prints it: 824,901 things
sent, 764,726 decisions, 163,323 taken; acts taken under a yes 16,226;
yeses given 30,855, and 13,984 more for an act that had one; 13,891 used;
5,758 acts under a used yes, each refused `permission-used-up`; 1,937
refused tries sent again under a new act-id and taken; a give at a yes's
place that held a yes, its end or its use refused `a-yes-is-given-once`,
where no word of the giver's own standing came first.

On the way, each a run whose output changed the code or a check:

- The judge's `clojure -M:run 2000 1`, after the gate took the form and 2A
  was fixed: one kind of break, `unseen`, at history 264, the road of the
  read path's mark (choice 8): a holder ends her own permission and gives
  the asker one beneath it in the same act. After the mark: exit 0.
- The read path's pure proof, with the rows of an ended chain kept instead of
  a mark: 5 checks failed, among them two worlds of every order that ended
  on a screen that the tables do not give. That way was dropped.
- The module's proof: one check of mine read the yeses' rows after the whole
  line had used them; it now reads them right after the gives. The read
  path's proof: a docstring's quotes, and one setup of mine that built a
  run's permission before its layer's; both mended before the runs above.

## Beyond the ask

Listed from `git diff --stat` of `src-store` and `test-store` (27 files,
2,815 lines in and 1,415 out, the fixture's two files 1,868 of them), not from
memory.

- **The read path's mark** (`read_path/answer.clj`, `layers.clj`,
  `read_path.clj`; choice 8): the ask was 2A, the rows above another holder's
  run permission; the judge needed the general rule behind it. With it,
  `layers/meaning-standing`, and the read path's page's choice "No value is
  kept between two changes" written again.
- **The live loop's bound** counts rounds that join (`read_path.clj`,
  `read-live`): with a bound set from a derivation, a read that settles in
  its last round opens.
- **`decision/chains-needed`** reads no chain for a give of a yes, which is
  refused whatever its place holds.
- **`door/declaration.clj`, `max-act-bytes`**: one sentence, the use of a yes
  is not counted.
- **The store's map** (`src-store/softland/store/README.md`): two words of the
  declaration and two names of its rules that piece 2 added and left out of
  the tables (`:permission-fact-under-another-key`,
  `:permission-fact-not-on-what-it-covers`, `:permission-fact-is-of-its-key`,
  `:permission-fact-sits-on-what-it-covers`).
- **In the read path's pure proof**: three writes of its stand-in gate
  (`:give-act`, `:end-and-give`, `:end-and-give-run`), two players that write
  while a read is asked or opens, and the worlds `chains`, `unseen-world` and
  `own-unseen-world`.

## The commits

Two, each when every proof passed on its tree: the read path first
(`27d883a3`, "Store's read path: an unseen layer tells nothing, and the rounds
are derived"), then the yes with this page. The read path's commit was run on its own tree, in a
detached worktree at the commit before it with only its files: the read path's
pure proof, 241 checks, and its proof on the cluster, 583 checks, the count of
piece 2, none failed. The store's other proofs do not load the read path's
files that the commit changes. After the runs above, three docstrings were
corrected, in `act.clj`, `decision.clj` and `answer.clj`, with no code; the
three pure proofs and a load of every namespace on the module's alias were
run again on the final text. The yes's commit, the last, has the subject "Store: a yes for one act
has an id of its own, and is used up".
