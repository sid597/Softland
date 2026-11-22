# The store's line, piece 2, repaired a second time: the review of 22 November 2026

[The review](overnight-audit/REVIEW-store-2026-11-22.md) ·
[its two probes](overnight-audit/probes/) ·
[piece 2's spec](store-piece-2/SPEC.md) ·
[piece 2, repaired the first time](BUILD-store-piece-2-repair.md) ·
[piece 3](BUILD-store-piece-3.md) ·
the judges: [the generated histories](gate-histories/README.md) and
[the statements of piece 3](store-piece-3/statements/README.md)

**22 November 2026.** One Claude session. This page says what was asked,
what each break was and what now holds instead, each fork taken, what
changed, each command with what it printed, and what is open. A session does
not judge its own build: a fresh session does that, from the diff.

## What was asked

The job came as one message pasted into the session, which calls itself "my
go for the whole job"; the line typed after it was "ok complete it". The
record did not yet hold the message when this page was written (`memtool
who` on one of its runs of words: "not found"), so who wrote it is not said
here. Its ask: "Fix every Piece 2 break that REVIEW-store-2026-11-22.md found
in src-store": P1, "the unseen-layer and confidentiality leak across per-row
pushes on both :permission and :run-permission"; P2, "refusal-word
precedence against store-piece-2/SPEC.md R9, and never returning
:permission-ended for a never-given forged yes on a used yes's UUID"; and the
three P3s, "hex-for-a-yes skipping declaration/check bounds, use-of-its-yes
exposing an out-of-bounds :fact index under :stores-own-facts, and
read_path/settle! wedging live reads when hitting max-rounds". And: "Solve P1
and P3b from the store's invariants rather than patching around the probe
cases", take the sound default at a genuine fork "without weakening the
store's promises", mark it, and keep the committed probes, `gate-histories/`
and `store-piece-3/statements/` "untouched as independent judges". Leave the
strip of a decimal's trailing zeros open "unless a fix changes no store rule,
or state your view and grounds".

None of the judges' files was changed: not the two probes, not
`gate-histories/`, not `store-piece-3/statements/`.

## The five breaks, and what holds now

### P1: a permission that stood at no stamp, made of two rows from two moments

**The break.** A live read holds a push on the asker's permission row and one
on each row above it. Rama tells each push by itself, and nothing lines two
pushes up. One act can give her a permission beneath its own and end its own
(the statements take such an act). A read that held the giver's row from a
push opened before that act could then pair her new row with the giver's row
as it was before the same act ended it: by the chain's rule her permission
stood, at no stamp of the layer's line, and the read let her see the layer
and handed her a fact written after she lost her standing. The review ran it
in the judges' own live read, in the pure proof's every order, and on a
cluster.

**The invariant it is solved from.** A permission stands at a stamp when its
row holds it and each row above holds its permission with an older stamp
(`gate/chain.clj`). A row of a place changes only by a later decision, at a
later stamp. So a row above that was read at or after the stamp of the row
beneath it, and holds its permission with an older stamp, is the row that
the place held at that stamp: no decision wrote the place between. The rows
then say what was true at her row's stamp, and a permission that they say
stands, stood then.

**What was built.** The read path reads each row above an asker's permission,
or above her run's, after her own row: the source of such a row names her
row's stamp (`:read-after`, `read_path/sources.clj`, `chain-sources`). It is
wanted only once her row is known, so its push is opened after her row was
seen and holds its place as it was at her row's stamp or later. When her row
changes, the sources above name the new stamp, and the read joins them again,
afresh. Nothing else of the read changes: what a change means and what it
tells are the same functions on the same values. A read asked once already
reads the rows above in the round after her row; it now reads one place once
when two sources of it differ in that stamp alone (`read-of`,
`calls-for-once`, and `select-now` in `read_path.clj`).

### P2: the words of R9, and a yes that was never given

**The break.** The gate checked "another run" and "not the act that the yes
permits" only when the permission's place held the permission; with the place
ended or used, an act of another run, or an act with another digest, got
`:permission-ended` or `:permission-used-up`. SPEC.md's R9 refuses such a
thing "whatever else is right in it", and the statements check both words
before what the place holds. And a yes that was never given, on the id of a
used yes, was told `:permission-ended`.

**What was built** (`gate/decision.clj`, `standing`). Once the layer is made:
the holder, then another run, then another act, each read from the act alone
and the digest that the walk made of it; then what the place holds. A yes's
place takes one give, so a place that holds the use of another yes on the
same id was never given the yes that the act names: `:permission-does-not-stand`
(`use-of-another-yes?`). An end by a yes's giver, by one above, or by its
holder names a permission for the layer and not the yes that it ended, so
`:permission-ended` stays the word of a yes's place that holds such an end.

### P3: a yes for an act that no gate can take

**The break.** `digest/hex-for-a-yes`, the maker's call, gave a digest for
any act of the form, also one that the declaration refuses: 257 facts, a
value over its bound, a place named twice, a key of the store's that is no
store key, an id over its limit. A yes for it was taken and stood, and its
holder's act could never be.

**What was built** (`door/digest.clj`). The maker's call holds the act to the
declaration's check first, as the door would append it under the least
demanding yes there is: an act-id of the act's layer that no fact of the act
replaces, the time 0, and a yes of the act's who beneath the shortest first
permission that an id allows (`under-the-least-yes`). So it gives no digest
only for an act that the declaration refuses under every yes; for every
other act it gives the text that it gave before. The gate's own digest, of a
thing that its declaration took, is the same walk with no second check
(`hex-of-the-act`, which `decision/hex-for-its-permission` calls).

### P3: the use of a yes named a fact that its act does not hold

**The break.** The accept under a yes wrote the yes's use as a row whose fact
id was the act's act-id and the count of its facts, one index past its last:
a read of the place with the store's own facts gave `:fact [[layer depot
uuid] 256]` for a 256-fact act, a fact that no reader of the act finds.

**The invariant.** Every fact id that a row of the facts table names, and that
a read gives back, is the id of a fact that its act holds at that index.

**What was built.** The use names the act that used the yes and no fact of it
(`record/rows.clj`, `use-row`): a row with no value and no index, written by
the accept, in the decision's one write as before (`decision/use-of-its-yes`).
A read gives such a place as `:nothing-here`, with its layer, the stamp of
the act that used the yes, and no `:fact` (`read_path/answer.clj`,
`place-answer`). No fact id names the row (`rows/written-by?`).

### P3: a step of a live read that never settles

**The break.** A step of an open read (`settle!`) that met the live bound on
rounds while it still had sources to join returned as if settled: the read
then owed an answer that no push it held would bring, and stayed open,
silent.

**What was built** (`read_path.clj`, `settle!`). A step that still wants to
join or leave a source after `(:live max-rounds)` rounds ends the read: its
function is told `{:ended {:could-not :keep-the-read-live :because
:a-step-did-not-settle}}`, and the read and its pushes leave the read path,
as a read that does not open within the same bound is turned away. The
reason is the read path's sixth (`read_path/words.clj`).

## Forks taken, each mine, each for Sid to turn

1. **P1 is solved in the read path, not at the gate.** The review's first way
   was to forbid, at the gate, an act that gives beneath its own permission
   and ends it. I did not: the statements of the generated histories take
   such an act (`reference.clj`), so a gate that refused it would break
   `agree` in every history that holds one; a new refusal is a rule of the
   store, which is Sid's; and the stale pairing is a property of reading
   rows from pushes that Rama does not line up, which the gate cannot close
   for the read path. The read path's rule needs nothing of the gate but the
   chain's rule and the stamps.
2. **Every row above names her row's stamp, not only the one straight above
   hers.** Only the row straight above can be ended by the decision that wrote
   hers, today: a decision writes no place above its own permission but its
   own end. Naming that row alone would cost less. I did not rest the read on
   that, because it holds only while no holder has two permissions for the
   layer in one chain, which Sid's decision of 21 November leaves open (the
   store's decisions of 20 and 21 November, entry 6: "Not built: two
   permissions for the layer that stand at once for one holder"); naming
   every row rests on nothing but the stamps. What it costs, counted and not
   measured: a live read now holds a push of its own on each row above her
   permission, and above her run's, shared only with askers whose rows one
   decision wrote. An asker d beneath the first holder holds 1 + d pushes of
   her own on the layer where she held 1; under the read path's bound of 900
   pushes on one table under one routing id, that is about 450 such askers of
   a layer at d = 1 and 300 at d = 2, where it was about 900. The rows above
   are written rarely, so the pushes cost little at a write; the count is
   what they cost. The way to have the sharing back is open item 1.
3. **A read asked once reads a place once** when the sources of it differ in
   that stamp alone: one path in one call, and the value given to each.
4. **The order of the gate's words of standing**: the layer, the holder,
   another run, another act; then the place. A yes's place that holds the use
   of another yes gives `:permission-does-not-stand`; a place that holds an
   end, which does not say which permission it ended, gives
   `:permission-ended`, also for a permission that was never given there.
5. **The maker's digest is held to the declaration under the least demanding
   yes**, so it refuses only what every yes would; the gate's is not checked
   again. What it cannot hold: a giver who makes the text by another hand,
   since a yes names its act by the digest alone; and an act that fits only
   under a shorter permission than the giver's.
6. **The use of a yes holds no index.** It changes the form of a stored row:
   the row of a use lacks `:by-index`, which every other row of a fact holds.
   No real data is kept, and the run-again fixture is made again for it (an
   eighth making), on purpose.
7. **A step that does not settle ends its read**, with a reason of its own,
   `:a-step-did-not-settle`, where the other answer was to let it wait for a
   later change.
8. **The strip of a decimal stays open**, as asked. My view: a fix that
   changes no rule exists, a strip by halving powers of ten that gives the
   same text, but it only lowers the cost of the order of its digits from the
   square to a few multiplications, and the walk still pays it for a thing
   that the declaration then refuses. The bound that holds the task is on
   what a value is: a decimal whose unscaled digits pass a bound far above
   `max-value-bytes` is no value of the form, and its text is `#other`, with
   no strip (session f's position on BUILD-store-piece-3.md, open item 1). That
   is a rule of the store, and Sid's.

## What changed

From `git diff --stat` against `fe3b19a8`, the head when this was committed;
no commit of another session touched these paths since `2eff35ef`.

| File | Lines, by `git diff --stat` | What changed |
|---|---|---|
| `src-store/softland/store/read_path/sources.clj` | 71 | P1: a row above names her row's stamp (`chain-sources`, `:read-after`); `read-of`; `calls-for-once` puts the sources of one read in one call |
| `src-store/softland/store/read_path.clj` | 57 | P1: `select-now` reads each read once and gives its value to each source of it. P3c: `settle!` ends a read that does not settle; the docstrings of `max-rounds`, `read-live` and the cost of a live read |
| `src-store/softland/store/read_path/answer.clj` | 22 | P3b: a row that no fact wrote gives no `:fact` (`fact-id`, `place-answer`, the namespace docstring) |
| `src-store/softland/store/read_path/words.clj` | 6 | P3c: `:a-step-did-not-settle`, the sixth reason for which a live read ends |
| `src-store/softland/store/gate/decision.clj` | 159 | P2: the order of `standing`, `use-of-another-yes?`, the docstring of the words in their order. P3b: `use-of-its-yes` writes `use-row`. P3a: `hex-for-its-permission` calls `hex-of-the-act` |
| `src-store/softland/store/door/digest.clj` | 103 | P3a: `hex-for-a-yes` holds the act to the declaration under the least demanding yes (`under-the-least-yes`); `hex-of-the-act`, the gate's; the namespace docstring |
| `src-store/softland/store/record/rows.clj` | 19 | P3b: `use-row`; the docstring of a fact's row |
| `src-store/softland/store/record/tables.clj` | 5 | P3b: `:by-index` absent in the row of a yes's use |
| `src-store/softland/store/read_path/README.md` | 16 | The rows above hers read after hers, and why; a step that does not settle; two words |
| `src-store/softland/store/README.md` | 27 | The yes's digest and its use; R9's order; the words of a gate and of the code |
| `src-store/MODEL.md` | 4 | The maker's digest gives none for an act that every yes would see refused |
| `test-store/softland/store/pure_proof.clj` | 76 | `second-review-cases`: probe 2's five cases, probe 4's five acts and three at the limits, the yes's use; the check of the use row written again |
| `test-store/softland/store/read_path_pure_proof.clj` | 105 | `a-row-above-hers-is-read-after-her-own`: the review's road in every order, the player failing with the sources of before, a give that stands, the stamps that the sources name; the rounds counted by reads; the six reasons; a check of the meaning of a row above with the source that her row names |
| `test-store/softland/store/read_path_proof.clj` | 140 | `the-second-reviews-cluster-probe-written-again` and `a-step-that-does-not-settle-ends-its-read`; the counts of pushes of `the-base-is-read-where-the-askers-layer-lives` and `one-push-for-one-thing`, brought to a push of each asker's own on the row above hers |
| `test-store/softland/store/README.md` | 18 | What the two read path proofs do not reach |
| `test-store/softland/store/run_again_fixture/acts.edn`, `tables.edn` | 796, 2,632 | The eighth making, on purpose, for the row of a yes's use |
| `test-store/softland/store/run_again_fixture/README.md` | 26 | The eighth making |
| `src/proposal/runtime-2026-11-21/BUILD-store-piece-2-second-repair.md` | new | This page |

## What was run, with its output

All on 22 November 2026, IST, on this machine, on the working tree of this
commit; each cluster run behind the machine's lock
(`flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock`).
The logs are in the session's scratchpad, outside the repository; the
commands make them again.

### The judges

| Command | Output | Time |
|---|---|---|
| `clojure -M:run 20000 1` to `5`, in `gate-histories/`, five at once | exit 0 each; "HOLDS: agree, told, once, outsiders, stamps, kept, see, unseen, screen, run-again"; "AT A YES FOR ONE ACT: 0 of the 0 kinds of break. Elsewhere: 0."; "ROWS OF A KIND THAT THE WALK DOES NOT KNOW, kept aside, by kind: none". Seed 1: 825,699 things sent, 764,962 decisions, 164,310 taken, 600,652 refused, the review's counts | 210 to 215 s |
| `clojure -M:run mutants 20000 1`, in `gate-histories/` | exit 0: each of the 12 broken copies caught, at histories 58, 2, 4, 7, 16, 6, 1, 90, 109, 2, 14 and 14 | 20 s |
| the review's pure probe, `overnight-audit/probes/break_piece_2_yes.clj`, each probe called by itself by a runner in the scratchpad, which reads the file's forms and changes none (`-M:store-module` with test.check and `gate-histories/src` added) | 1A: "violations count: 0"; its assertion that `unseen` breaks fires. 1B: "states explored: 30 \| total tells delivered (expected 0): 0", no leak, for the permission; its assertion fires. 2: "2a … gate: :act-not-the-one-permitted \| SPEC/reference: :act-not-the-one-permitted"; its assertion of a difference fires at 2a. 3: throws `ArityException`, "Wrong number of args (8) passed to … ->ReadPath": the probe names the read path's eight fields of before piece 3's second stage, which gave it fourteen (`2eff35ef`). 4: "4a … hex?=false \| grant=permission-fact-not-a-permission"; its assertion fires. 5: "total decision writes … 771", the place-answer of the used yes holds no `:fact`, and the probe's own code throws on the missing index | 7 s |
| a second runner in the scratchpad, with the probe's own definitions, every case of probes 2, 3 and 4 printed | 2a to 2d: the gate's word is the reference's (`:act-not-the-one-permitted`, `:act-not-the-one-permitted`, `:permission-for-another-run`, `:act-not-the-one-permitted`). 2e, a yes never given with the act's digest on the used yes's id: the gate `:permission-does-not-stand`; the probe's reference `:act-not-the-one-permitted`, because the probe leaves `*act-named*` unbound and the reference then refuses every act under a yes so, the setup's own use among them; the reference told which act each yes is for: `:permission-does-not-stand`. 3, on the constructor of fourteen fields: "what the caller's function was given: [{:ended {:because :a-step-did-not-settle, :could-not :keep-the-read-live}}]", the read gone, "pushes left in the read path: 0". 4: for each of 4a to 4e "hex-for-a-yes: nil", the grant refused `:permission-fact-not-a-permission`; a good act's digest and grant as before | 7 s |
| `flock … clojure -M:store-module -i overnight-audit/probes/cluster_probe_piece_2_yes.clj -e "(cluster-probe-piece-2-yes/-main)"` | Receipt 1: "[shared ReadPath] callbacks to :ann (exp 1): 1" and "[isolated ReadPath] callbacks to :ann (exp 1): 1", each the first answer with `:c-secret` left out. Receipt 2: the 256-fact yes taken with 771 writes, its use on all four tasks, the read of its place "{:holds :nothing-here, :layer :base, :stamp 4, :writer :ben, …}" with no `:fact`. Receipt 3: as the review's. Receipt 4: "wrong-digest m2 on used pa1 … :act-not-the-one-permitted"; the probe's never-given yes names m2's digest and is sent with m1, so "… :act-not-the-one-permitted" (R9); "grant of 257-fact yes answer: … :permission-fact-not-a-permission", since the maker's digest is nil | 19 s |
| `flock … clojure -Sdeps '{:aliases {:the-store {:extra-paths ["../../../../../src-store" "../../../../../test-store"]}}}' -M:store:the-store -m softland.store.line-statements`, in `store-piece-3/statements/` | exit 0: "PASSED the judge's probe of the bounds, over the store: 1 MiB kept whole with standing, a thing over 32 MiB too large and the line goes on, a sorted map that throws is safe"; "PASSED the judge's statements over the store: 240 histories (seeds [1 2 3], 80 each), 0 broke a statement, in 253 s" | 260 s |

### The store's seven proofs, on the final tree

| Command | Output | Time |
|---|---|---|
| `clojure -M:store -m softland.store.act-proof` | 166, none failed | 1 s |
| `clojure -M:store -m softland.store.pure-proof` | 779, none failed: 774 before, and the five cases of the review | 4 s |
| `clojure -M:store -m softland.store.read-path-pure-proof` | 277, none failed: 273 before, and the four checks of the rows above hers. Its SHOWN line: "in 84 lists of up to three writes on the review's road, 37108 worlds, no step handed her anything; with sources that name no stamp, steps handed her #{the secret}" | 49 s |
| `flock … clojure -M:store-module -m softland.store.read-path-proof` | 670, none failed: 640 before. "the asker's calls, on the shared read path and on one of its own: [1 1] and in her run [1 1] \| the words through the door: [:act-not-the-one-permitted :permission-does-not-stand :permission-for-another-run]"; "a step that does not settle ends its read, after 17 readings of what it holds" | 60 s |
| `flock … clojure -M:store-module -m softland.store.module-proof` | 496, none failed | 53 s |
| `flock … clojure -M:store-module -m softland.store.door-proof` | 283, none failed | 31 s |
| `flock … clojure -M:store-module -m softland.store.run-again-proof`, on the seventh making | 42 passed, 6 failed, every one at the four rows of a yes's use, each "differs: [:by-index]"; the run again and the run after failures gave no row that differs | 69 s |
| `flock … clojure -M:store-module -m softland.store.run-again-proof make-fixture` | the eighth making, on purpose, for the row of a yes's use: `acts.edn` 127,741 bytes, 401 records; `tables.edn` 343,483 bytes, 1,706 rows, the seventh's counts in every table; every "held to" true. `:by-index` in 185 rows of the seventh making and 181 of the eighth, the four uses | 25 s |
| the same, on the eighth making | 48, none failed | 67 s |

### Runs on the way, which changed the build

| Command | Output, and what followed |
|---|---|
| `pure-proof` after the code | 773 passed, 1 failed: the check of the use row held its index at the count of the act's facts; written again for a row with no index |
| `read-path-pure-proof` after the code | 270 passed, 3 failed: the six reasons, a check that made the source of a row above by hand with no stamp, and a count of a read asked once that read one row twice in one call. The first two were written again; the third led to `read-of` and the reading of each place once (fork 3) |
| `read-path-proof` with the new checks | 665 passed, 5 failed: the five counts of pushes in which a second asker beneath one holder shared the push on that holder's row. Written again, as fork 2's cost; the new checks of the review passed in that run |

## What is open

1. **The sharing of the pushes on the rows above.** Fork 2's cost. A design
   that keeps them shared, not built: hold the shared push on each row above
   and the stamped one beside it only until the shared push gives a row as new
   as the stamped one, and read, at each place, the newer of the two rows. A
   row that was read at or after her row's stamp makes what the rows say true
   at that stamp, and a push of the same place can only give that row or a
   later one, so the standing stays one that stood. It costs a round more when
   her row changes, and the steps must take two sources of one place. Worth
   it when a layer is read live by hundreds beneath one holder; the counts
   above say when.
2. **What the read path's rule rests on in Rama.** That a push opened after a
   change of a decision was seen gives every place as it was at that decision
   or later: read from how a task serves the opening of a push, after the event
   whose change was seen, and run by no probe of its own. The cluster checks
   hold that the asker is handed nothing in the orders that Rama gave in their
   runs; the pure proof holds it in every order of its player.
3. **A fact told as late as a push is.** Untouched, and older than this build:
   a fact written after her permission ended can be told until the end's push
   reaches the read path (the read path's docstring of `read-live`). It is the
   same kind of thing as P1, a value seen at another moment than her rights,
   and P1's rule does not close it: it makes a standing that the read gives one
   that stood, and does not make the facts that it gives be of that stamp.
4. **`:permission-ended` for a permission never given at a place that holds an
   end**, of a holder or of a run: an end does not say which permission it
   ended, so the gate cannot tell a forged permission from the one that ended
   there. The statements say `:permission-does-not-stand` for both. To tell
   them apart, an end would name what it ended: a change of a stored row, and
   Sid's.
5. **The probes as judges.** Probe 3 of `break_piece_2_yes.clj` does not run on
   the store since `2eff35ef`: it names eight fields of the read path; probe 2
   compares the gate with a reference that knows no act of any yes, so its
   2e reads as a difference that it is not; probe 5 throws when the place's
   answer has no index. Each is the review's file, and stays as it is.
6. **A yes whose digest a giver makes by another hand**, for an act that the
   declaration refuses, or an act that fits only under a permission shorter
   than the giver's: the gate cannot tell either from the digest, which names
   the act and nothing about it.
7. **The strip of a decimal's trailing zeros**: open, fork 8.
8. **Not run**: `gate-histories`'s `-M:cluster` (not asked), and anything with
   more than one machine.
