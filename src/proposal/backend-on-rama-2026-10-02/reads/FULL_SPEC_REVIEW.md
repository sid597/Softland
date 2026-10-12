# Full-Spec Review: the read build, whole, against `BUILD-reads.md`

Verdict: pass
Summary: This session made the four repairs that rulings 84, 86, 87 and 89 left to the review, with
their proofs and explanations, and then audited the whole read build against `BUILD-reads.md` in
three passes. It read whole the read path and its five pure namespaces, the record's two files, the
decision, the first act, the module, the streaming gate, the topology of the batch's clock, and the
maps of the read path, the record, the gates, the module and the two trees' entries; it read the
store's page and the proofs' map in the sections that this build wrote or changed, and the
microbatch gate, its pure code, the door and the act through what this build changed in them; and
it searched the seven files of proofs rather than reading each line. Beside the repairs it found seventeen things and fixed each: twelve in the
explanations, five of them lines that an earlier build wrote and a later one made untrue (rows 4,
5, 9, 12 and 13); and five gaps in the proofs (rows 10, 11, 14, 15 and 17), of which two were
opened by the repair of ruling 86 itself. It found no
fault in the code of the gates or of the read path beyond what the four rulings name. The six
proofs pass on the final tree, each with exit status 0. A third pass found nothing new; one check
more, row 17, was added after it, when the receipt stated a thing that no run held. The verdict
holds for what the review reached; "What this review did not reach" says where that ends.

Cut by: the session "session 1" (Opus 5.5), 12 October 2026, started by Sid by hand on
`STARTER-review.md`. It made the four repairs, audited, fixed and audited again itself. It started no
agent: Sid's line to this session was "do everything it lists, in its order, yourself, with no
subagents". The starter's head was changed twice after this session had read it (commits `beb852d5`
and `818fd840`); the second says to give the repairs and the review each to a fresh agent. This
session kept to Sid's line, and says so here so that the difference is seen. So this review is not
the fresh, separate session that the skill and the contract ask for: the session that made the
repairs also judged them. What stands against that: each repair has a proof that can fail, the
proofs of the three builds were not written by this session, and each fix of the review is held by
a run or a script that this paper names.

## A ruling that could not hold as worded

**Ruling 87 says that the read path counts its pushes "on each table and task". It counts them on
each table under each routing id.** The evidence: Rama bounds the pushes on one table on one task,
and its client gives no call that turns a routing id into a task. Looked for with `javap` in the
jar of Rama 1.6.0 (`com.rpl.rama.Helpers`, `PState`, `ProxyState`, `SelectOptions`,
`PartitionedObject`) and in the public vars of `com.rpl.rama`, `com.rpl.rama.ops` and
`com.rpl.rama.test`: `foreign-object-info` gives the number of partitions; `Helpers/genHashingIndexKeys`
gives a key for each task and does not go back; a PState can declare a key partitioner of its own,
and the store's do not. The ruling is not undone: the count, the bound of 900, the turning away at
once and the word are built. What changes is what one count holds; the repair's section below says
what that reaches and what it does not.

No other ruling of 1 to 91 failed to hold. Two are built in a way that the session chose and names:
ruling 89 by taking its function out and not by switching it off, and "before it opens any push"
of ruling 87 for a round of a read and not for the read.

## The four repairs, and what each changed

### Ruling 86: a fact's row holds its value as a String

**What was run before the form was rested on.** `runs/review-probe-held-value.clj`, on Rama 1.6.0
in-process, 4 tasks, 2 threads, a module of the probe's own whose rows have the store's field
`:value String`; the Strings are made by the store's own pair of functions. What it printed is kept in
`runs/review-probe-held-value.txt`.

- Ten runs of bytes, each written twice by a stream topology and twice by a microbatch topology, and
  read back by a select: all 256 byte values; text that begins with U+0000; one zero byte; a lead
  byte with no tail (C3 28); a surrogate in UTF-8 (ED A0 80); an overlong slash (C0 AF); 65,508 random
  bytes; text of one, two, three and four bytes a letter; text with U+0000 inside; no bytes. For each
  of the ten, in both topologies: the String read back is the String written, the bytes that the
  pair gives back are the bytes given, and the topology's own `local-select>` of the row before a
  second write found the same String. So U+0000 as the mark holds, and no other mark was needed.
- A push on one such row and on a run of five such rows, 20 writes of one row 100 ms apart by the
  stream topology: the run got 20 `KeyDiff`, the one row 20 `NewValueDiff`. The same rows with a byte
  array, in the same run: 7 `ResyncDiff` each. By the microbatch topology, each write a batch of its
  own: 20 and 20. 100 writes one straight after the other's ack: 100 and 100; the byte-array rows 1
  `ResyncDiff` each. One new row in the run: one `KeyDiff` for each topology.

**What changed in the code.** `record/rows.clj`: `held-value` (bytes to the String), `plain-bytes`
(back), `plain-bytes-of` (a row's bytes); `fact-row` makes `:value` with the first; `holds-value?`,
`value-of`, `read-value` and `holds-this-value?` go through the pair; `same-row?` is the equality of
two maps, since no field of a fact's row is a byte array any more. `record/tables.clj`: `:value` is
`String` in the one schema of the facts of both gates. `gate/decision.clj`: the two places that took a
row's bytes for the digest of the order by value take `plain-bytes-of`. Nothing else knew the form,
and nothing else does. The digest of a value is still the digest of its bytes, so no key of the facts
by value changed. The rows of the answers and of the locks keep their byte arrays.

**The form.** The text itself when the bytes are well-formed UTF-8 (decoded with malformed input
reported, not replaced) whose text does not begin with U+0000; else U+0000 and one character for each
byte (ISO-8859-1). The two never meet, and the bytes come back exactly: proven in the pure proof for
eleven runs of bytes, and on the cluster through the real gate for 65,508 zero bytes, which begin
with U+0000 and so take the second form (the module's proof, group 7).

**No migration.** The class of a field of a table changed. No cluster holds data of the store: it has
run in-process only. A cluster that held rows with a byte array would need Rama's migration of a
PState's schema; none is written, and the receipt says so.

**M4, before and after.** From a streaming depot's ack to the call of the asker's function, for one
live read of one place of the facts table:

| | middle | slowest 1 in 100 | told by a call of their own |
|---|---|---|---|
| before, the last build's full run (`runs/measures-read-path.txt`) | 1,000.3 ms | 2,000.6 ms | 26 of 500 |
| after, this session's small run, 40 acts (not kept as a file) | 0.094 ms | 0.199 ms | 40 of 40 |
| after, the final full run (`runs/measures-final.txt`) | 0.095 ms | 0.201 ms | 500 of 500 |

**What the repair costs, as the same final run measured it.** Each write is now told to each push
that holds the written row, where before a push on such rows was sent a whole value about once a
second. With 898 live reads open on one layer, each on another run that holds the written places,
the askers were told 3,142,587 changes over 3,500 acts, where the run before the repair told 21,998;
and an act took a middle of 15.5 ms from the call of `send-act` to its return, where it took 4.7 ms,
so the maker could no longer hold 100 acts a second in that one process. With 100 such reads the
middle was 4.9 ms, and with 898 reads on places that are not written it was 3.8 ms, as with one.
So the cost is of the pushes that hold the row that is written, and it is paid on the gate's task.
The receipt puts this beside the gain, and proposes a row for the queue.

### Ruling 87: the read path bounds its own pushes

**What changed.** `read_path.clj`: `held` has a third map, `:counts`, from a gate, a table and a
routing id to how many entries of `:pushes` are under them; `join!` counts the entries that it would
put in, inside the same swap that puts them in, and when one count would pass
`:max-pushes-under-one-routing-id` (900) the swap changes nothing and `join!` gives
`:past-the-bound`; `leave-sources!` counts down in the swap that takes an entry out. `read-live`
turns such a read away with a word of the read path's own, `:keep-more-reads-live`, which carries
`:what :pushes-under-one-routing-id` and `:most`; an open read that comes to need such a push ends
with `:no-room-for-a-push-it-came-to-need`. `read_path/query.clj`: two numbers more, so seven.
`read_path/sources.clj`: the checks of one read have a number for a read asked once (40,000, as
before) and one for a read kept live (250), by `most-checks`. `read_path/words.clj`: the eighth word
and the fifth reason.

**Where the ruling could not be met as written.** The ruling says "on each table and task". Rama
bounds the pushes on one table on one task, and its client gives no call that turns a routing id
into a task: looked for in `com.rpl.rama`, `PState`, `ProxyState`, `SelectOptions` and `Helpers` of
Rama 1.6.0 (`javap` on the jar, and the public vars of the Clojure namespaces);
`Helpers/genHashingIndexKeys` goes from a task to a key, and not back. So the count is by table and
routing id. What that reaches: every push of every asker through one streaming layer is one count,
since the routing id of a streaming layer is the layer; that is the case that the measures met. What
it does not reach: two streaming layers that Rama puts on one task, and the entities of the base
that share a task, are counted apart; and the count is of one process, where Rama's bound holds for
every client together. Past either, Rama refuses the opening as before, and the read gets
`:reach-the-store`. With the checks of one live read bounded by 250, one read can no longer ask
Rama for thousands of openings that it refuses one by one. A bound on the whole of one table would
never pass Rama's, and would hold one runtime to 900 pushes on a table whatever the number of tasks:
on a cluster of 64 tasks that is 900 where Rama allows 64,000. It was not taken. A row proposed for
the queue, and a question for Rama's team.

**"Before it opens any push."** A live read opens in rounds: the layers' own rows, then the asker's
permission, then the facts, then the checks. The bound is tested for a whole round before one push
of that round is opened; the pushes that the read joined in earlier rounds are left when it is
turned away. So a read that is turned away in its third round has opened pushes in the first two.
The proof counts the openings of the read that is turned away: none, because the pushes of its
earlier rounds were open already for another reader.

**Rama's option is not set** (ruling 91). The docstring of `softland.store.read-path` says that 900
stands under the default of `pstate.proxy.limit`, and that to raise the option and the number with
it is a setting for Sid to weigh.

### Ruling 89: a read asked once reads the table

**What changed.** `read_path.clj`: the function that took the value of an open push for a read
asked once is taken out, with its one use; `read-once` selects every source. The ruling says "one
function switches it; the review of the whole switches it off". This session took the function out
and did not leave one that gives nothing: a read asked once that answers from a value kept in the
memory of the process is what the starter's rule names ("a kept copy of what the cluster holds in
the memory of a process"), and a switch that is always off is code for a behaviour that is not
wanted. The other answer is no longer one function away. Listed as this session's choice.

### Ruling 84: two lines of `door.clj`

Both now say "the two depots of acts", and that the module's third depot, of the clocks of batches,
takes no act and is not opened by the door.

## Checks of the proofs that the repairs made untrue, written again (ruling 1)

None was removed.

| Proof | The check, as it was | Why it was untrue or could pass falsely | As written again |
|---|---|---|---|
| pure, the record's rows | the row of a sealed fact holds the plain bytes, class `byte[]` | the field is a String | the row holds the text itself as a String, of class `String`, and gives the bytes back |
| pure, the record's rows | the row of a fact under a store key holds the bytes of its one text | the same | it holds the one text as a String, whose bytes it gives back |
| pure, the record's rows | bytes that are no text are told so: a row made by hand with a byte array | a byte array in the row threw inside `read-value`, which gives `[:not-a-value]` for any throw: the check passed for the wrong reason | the row is made with `held-value` of those bytes |
| pure, the record's rows | the row of the marked keys, class `byte[]` | the field is a String | class `String` |
| pure, the decision | an accept of one sealed value writes the fact with the plain bytes | the field is a String | with the text of the plain bytes |
| pure, the decision | "the row holds exactly what the lock opened": `(= (vec (:value row)) (vec sealed))` is false | with a String the two vectors hold characters and bytes and are never equal: it would pass whatever the row held | the row's plain bytes are the bytes of the value's one text, and are not the sealed bytes |
| pure, the decision | a request for locks, a making act, a retract with a give: rows with bytes | the field is a String | rows with the one text |
| pure, the decision | a give with the slot: `(String. bytes)` | the field is a String | the row's value itself |
| pure, the order by value | a fact that is replaced, naming what it replaces or not: the two lists of writes are not equal | they were unequal only because two byte arrays are never `=`; the rows are the same | the rows written are the same either way |
| read path, pure | `same-row?`: equal bytes that are not one object | no row holds bytes | an equal String that is not one object; no field of a row is a byte array |
| read path, pure | a row of bytes that are no value, by hand with a byte array (two places) | passed by a throw that was caught | made with `held-value` |
| module | about twenty checks that read `(vec (:value row))` | the field is a String | they read the row's plain bytes through one helper; two of them also hold the String itself |
| module | 65,536 sealed bytes are kept as 65,508 plain bytes | the field is a String | the row's String begins with U+0000 and has 65,509 characters, and gives 65,508 bytes back |
| read path, cluster | "through the read path that holds the open push it is given the push's value": `(<= n 200)` | ruling 89; and the inequality passed either way | 200 of 200 through both read paths |
| read path, cluster | "no live read is open, so no value comes from an open push" | ruling 89: none ever does | the same place while a live read holds a push open on each of its sources: the same calls and the same answer |
| read path, cluster | when every asker has left the read path's two maps are empty | `held` has three | three maps, the counts among them |
| read path, pure | the read path's own seven words; the four reasons | ruling 87 | eight; five |
| read path, pure | the checks of one read are bounded | ruling 87: one number for once, one for live | each bounds its own kind of read and not the other |

## The review of the whole

### Pass 1: what was found, and its fix

Each row: where, the clause that it bears on, what was wrong, the fix, and the evidence that it
holds. Rows 14 to 16 were found in the second pass, and row 17 after the third, while the receipt
was written; they stand here so that the faults are one list.

| # | Where | The clause | What was wrong | Fix | Evidence |
|---|---|---|---|---|---|
| 1 | `test-store/softland/store/*.clj`, 21 definitions that hold the proofs' kept state (`shown-lines` in six files; `round`, `permissions`, `sent-things`, `live-reads`; `things-sent`, `first-answers`, `locks-in-hand`, `kept`, `base-things-sent`, `base-answers`, `lock-bytes-read`; `results`, `kept`, `outside-the-stocks`; the measures' `round`) | "`src-store/CLAUDE.md` binds every file. Each namespace and each function has a docstring" | None had a docstring. They are no functions, so the letter of the rule was kept; but the same rules ask that the state a file owns is said, and one such definition of the same file (`attempts`) had one | A docstring on each, by the form that `attempts` uses | A script of this review read every top-level definition of both trees with Clojure's reader: 967 definitions, none without a docstring. Before the fix: 21 |
| 2 | `src-store/softland/store/README.md`, "How the parts relate", step 4; and "Engineering choices", the paragraph on a used lock | "Replace old wording; do not add a history of corrections" (`src-store/CLAUDE.md`) | "which now also asks what the act's permission allows"; "The fact's row no longer gives the key of its lock" | "which also asks"; "The fact's row gives no key of its lock" | A search of both trees for wording that tells a history ("now also", "no longer", "than before", "this build changes", and others) finds no other |
| 3 | `module/streaming_gate.clj`, the namespace docstring; `gate/decision.clj`, `reads-marked-keys?` | the same | "That is one read more than before the facts by value were kept"; "which read nothing more than before" | Each says what is read, with no "before" | the same search |
| 4 | `door/declaration.clj`, the docstring of the rules' code | "look for a line that one build wrote and a later one made untrue" (the starter) | "`act.clj`, which this build changes in one place only": written by the door's build; the read build changed `act.clj` again, for what a permission allows | The clause is taken out | read |
| 5 | `record/tables.clj`, the namespace docstring, "Why flat maps" | the same clause of the starter | "the decision of one common act would cost 6 to 8 seeks, against 4 here": the count of before the gates kept the order by value; one accepted act now reads 5 rows, as the streaming gate's docstring says | The sentence says the constraint and no count: with the layer as the top-level key each row would cost two seeks to read and one more to write, where each costs one here | read against `module/streaming_gate.clj`, "One accepted act with one sealed value costs 5 reads" |
| 6 | `test-store/softland/store/README.md`, the row of `read_path_proof.clj`; the proof's namespace docstring | "The map of a check says ... what state it changes outside the code that it checks" (`src-store/CLAUDE.md`) | "Seven atoms": the proof has eight, the one that holds the cluster's handles was not counted. And "four read paths": the new group of the read path's own bound opens two more | Both say eight atoms and name the two read paths more | counted in the file: `shown-lines`, `round`, `permissions`, `sent-things`, `live-reads`, `attempts`, `throw-asked`, `throws-made` |
| 7 | `read_path/README.md`, "Decisions this build stands on" | "Its own explanation holds 'Decisions this build stands on' (the entries of 11 and 12 October by number, the rest by pointer)" | The table had entries 1, 2, 3, 4, 6, 7 and 8. Entry 5 (a value is kept plain) is why the read path gives a value and opens no lock, and entry 9 (the other sheet comes after) is why it keeps no read; neither had a row | A row for each | read |
| 8 | `read_path/README.md`, "Words inside the code" | R6: "Every word that the build makes is ... listed for Sid under 'Words a session made, Sid's to change'" | `:ok` and `:refuse`, what the check of a query gives, were on no list. Found by a script of this review that reads every keyword of the read path's and the record's code and looks for it on the lists | A row for the two | The script again: the keywords that are on no list are Rama's own option names (`:pkey`, `:callback-fn`, `:max-amt`, `:num-partitions`, `:subindex?`) and the words of the act's own engine and shape language (`:tuple`, `:seq-of`, `:parts`, `:optional`, `:malformed`, `:unknown-part` and the like), which are the act's build's and not this one's |
| 9 | `act.clj`, the docstring of `allows`, and the namespace docstring's list of callers | "the code against its own explanations, in both directions"; "a line that one build wrote and a later one made untrue" | "a read asks this of the permission of whoever asks": written by the build of what the gates keep, before the read path. The read path reads no part of a permission: any permission that is there allows seeing (`read_path/layers.clj`, `standing`). And "the gates' checks (`decision`, `batch`) ask `allows`": one function asks it, `decision/standing`, which both gates call | Both lines say what the code does | a search of the source for `act/allows`: one call, `gate/decision.clj`, in `standing` |
| 10 | `test-store/softland/store/read_path_proof.clj`, the group of reads that fail | "Fault-tolerance ... the read path under ... a value sent anew, a callback with the same value" (the starter); "closing a coverage gap means testing the uncovered case" (the skill) | A gap that ruling 86's repair opened. Before it every change of a fact reached the read path as a whole value sent anew, so every live check ran that path. Now a change comes as a diff of its own, and no check met a value sent anew after the first callback, which is what Rama sends a reader that it left out of a change | A check: the proof makes Rama's own `ResyncDiff` and hands it to the callback of a run's push that holds two places, once with the rows as they are and once with one row that differs | Run: with the same rows the asker was told nothing; with one row that differs, one change that names that one place. The read path's proof on the cluster, exit 0 |
| 11 | the same group; `read_path.clj`, the docstrings of the namespace and of `read-live` | "Boundary and malformed inputs on every operation: a suite that enumerates a class of invalid inputs and omits one member of that class is a gap" (the skill) | Two members of a class had no check and no sentence. `read-live` was held to a function that throws, and not to a thing that is no function. `leave` says that it gives `:not-open` "for a thing that is no handle of this read path", and no check handed it one | The checks: `read-live` with nil, a number and a string in the place of the function; `leave` with nil, a number, a string, a map, and the handle of a read of another read path. The docstring of `read-live` says that a thing that is no function is as a function that throws at its first call. The namespace docstring says for what no call throws, and what must hold of a call's first argument | Run: each of the three things in the place of a function gives the answer and a handle that is not open, and nothing is held; each of the five things in the place of a handle gives `:not-open`, and the other read path's read stays open until it is left there. The read path's proof on the cluster, exit 0 |
| 12 | `src-store/softland/store/README.md`, "Engineering choices" | "a line that one build wrote and a later one made untrue"; "Replace old wording" | Four lines. "**The one change of `act.clj`**": the read build changed `act.clj` a second time. "check (d) stays one read and a comparison of bytes": the row holds a String since ruling 86. "reads what it read before", "reads nothing more", "decided in batches, as before", "where the module had one stream topology": each tells a history | The heading says what the choice is, that `act.clj` gives its engine to other namespaces, and names the check of a query as its second user; check (d) is "one read and one comparison", of two texts; the others say what is read and what is, with no "before" | read against `act.clj`, `read_path/query.clj` (`act/as-shape`), `record/rows.clj` (`holds-value?`) |
| 13 | `src-store/softland/store/README.md`, rule 21 | "Does the explanation still explain the code?" (`src-store/CLAUDE.md`) | "The aims ... None is measured": the read build's measures time a gate under 100 acts a second, in-process | The line says that none is measured on a cluster with a disk and a network, and points at the measures | read against `test-store/softland/store/read_measures.clj`, M1 |
| 14 | `test-store/softland/store/read_path_proof.clj`, the groups of a very large run and of the read path's own bound | "a suite that enumerates a class ... and omits one member of that class is a gap" (the skill) | Of the five reasons for which a live read ends, one, `:a-run-grew-too-large`, was given by no check on the cluster: found by counting, for each reason, the checks that name it. Three places of `read_path.clj` give it | Two checks: a run of 8 places kept live that an act takes to 11, where one read may hold 10; and a read of what points at a thing that opens with one check and comes to need two, where one live read may need 1 | Run: each read ended with `{:ended {:could-not :keep-the-read-live :because :a-run-grew-too-large ...}}`, the first with `:what :places-of-one-run :most 10`, the second with `:what :checks-of-one-read :most 1`, and held nothing after. The read path's proof on the cluster, exit 0 |
| 15 | `test-store/softland/store/read_path_proof.clj`, the group of the four things that a place can hold | the same clause; and ruling 86's second form | The group held "bytes that are no value" with bytes that are text and no form, which a row holds as the text itself. Bytes that are no text, which a row holds as U+0000 and a character for each byte, came back out through the read path in no check: the module's proof read such a row with the record's function, and the pure proof read one that it made itself | A second place in that group: the proof's hand seals four bytes that are no text and appends them; the place is asked once and kept live | Run: both give `{:holds :not-a-value}` with the layer and the fact, and no `:value`; no read threw. The read path's proof on the cluster, exit 0 |
| 16 | `test-store/softland/store/README.md`, the two sections of the read path's proofs | "Prefer a constraint and its reason to ... a guess of what a run gives" (`src-store/CLAUDE.md`) | "It takes about a minute" of the pure proof and "about four minutes" of the proof on the cluster: in every run of this session they took 4 seconds and 79 to 100 seconds | "a few seconds", "about a minute and a half" | the run times of this session's runs, each printed with its exit status |
| 17 | `test-store/softland/store/read_path_proof.clj`, the group of who asks | "When claiming that a capability works, show the meaningful action it enables and the evidence for that claim" (`CLAUDE.md`); and R8, for the working answer of ruling 11 | Found while the receipt was written, after the third pass. The receipt tells Sid what the working answer for the store's own facts lets happen: a person who is not signed in can ask for the base's own facts, since the base is "everyone". That followed from two functions as read (`layers/standing`, `answer/counts?`), and no check asked for the store's own facts as a person who is not signed in | A check: such a person asks for a holder's permission in the base, and for one in a members layer | Run: she is given the system's permission in the base, `[:system :base :base]`, and for the members layer `{:holds :no-fact}` with the layer left out as `:may-not-see`. The read path's proof on the cluster, exit 0, 482 |

**What the pass ran over the whole tree, beside reading.** The scripts are kept in
`runs/review-scripts/`, with a page that says how to run each.

- Every top-level definition of both trees has a docstring: 967 of 967, by a script that reads each
  file with Clojure's reader.
- Every folder of both trees has a `README.md` with a link to its parent; the tree's own entry has
  none above it. Every link of every map resolves: 305 links, none leads nowhere. The last build's
  paper named this as not verified.
- `clj-kondo` 2026.05.25 over both trees: no error and no warning in any file but the four dataflow
  files of `module/`, where it reports what it cannot read of Rama's dataflow, an output binding
  counted as an argument and a `*var` as unresolved, as it did for the builds before.
- R1 by a search: in `src-store` the calls of Rama's client that open a table, select or open a
  push (`foreign-pstate`, `foreign-select-async`, `foreign-proxy-async`, `foreign-object-info`) are
  in `read_path.clj` and nowhere else; the door has `foreign-depot`, `foreign-query`,
  `foreign-append!` and `foreign-invoke-query`. No code file outside `src-store`, `test-store` and
  the papers' own probes names a namespace or a table of the store. A search shows the tree, not a
  process; nothing in Rama holds R1.
- R3 by a trial: 10,768 things handed to the check of a query, made of ids with a space, an
  exclamation mark, U+0000, no character at all, letters outside the first plane, a store key, 300
  characters; ends `:first` and `:last` in every slot; and things that are no query, `[:whole-table]`
  and `[:table "$$streaming-facts"]` among them. 360 were taken as a query. Every source that those
  could want in any layer was made, 330 of them, and every path that the read path's one function
  builds for each step: 987 paths. Each is a key, or a range whose two ends begin with the text of
  one layer and a space or an exclamation mark, the first before the second. A source of a kind
  that is not known, and a step that is not known, give no path.
- R6 by the lists: row 8 above.

### Pass 2: the classes of words, member by member

After the first pass's fixes the hunt began again from the contract, as the clause-by-clause tables
below record, and with one sweep that the first pass had not made: for each class of words that the
code declares, how many checks name each member. A class with a member that no check names is a
gap. The sweep is a script of this review that reads the declarations themselves and searches the
proofs' text.

| The class | Members | Named by no proof | Named by the pure proof only |
|---|---|---|---|
| The read path's own words | 8 | none | none |
| The reasons for which a live read ends | 5 | none | none, after row 14. Before it: `:a-run-grew-too-large` |
| The two words for a layer that is left out | 2 | none | none |
| The words with which the check of a query turns a thing away | 18 | none | 7: the check is pure code, and the pure proof gives every word once; the proof on the cluster shows ten faults reaching the word `:read-the-query` |
| The read path's numbers | 7 | none | 1, `:max-paths-in-one-call`: the cut of a call is pure code |
| The act's reasons, the declaration's reasons, the words of a stray | 7, 34, 5 | none | those that the declaration gives before a gate reads a row |
| The refusal words of the gates' checks | 19 | none | 1, `:lock-way-not-known`, which no lock of this build can earn, as its docstring says |

The sweep found rows 14 and 15. After them it finds nothing.

### The contract, clause by clause

Each clause is quoted from `BUILD-reads.md`. "Module's proof, group n" and "read path's proof, group
n" are the groups as `test-store/softland/store/README.md` numbers them. A clause with no fault has
no row in the tables of faults; it is here so that the pass can be checked.

**What to build, A: what the gates keep.**

| Clause | Where it is code | The checks that hold it |
|---|---|---|
| A1 "A gate opens each sealed value when it checks it. It now keeps those plain bytes in the fact's row, and the row names no lock. The act in the log is not changed ... a gate still refuses an unsealed value that arrives." | `gate/decision.clj`: `opened` gives the plain bytes, `accept-writes` hands them to `record/rows.clj`, `fact-row`, which keeps them as `held-value`'s String. `record/tables.clj`, `facts`: no field names a lock. The refusal `:not-sealed` is the declaration's | Pure proof, groups 10 and 11. Module's proof, groups 3, 4, 9 (streaming) and 16, 18, 24 (the base): the row's plain bytes, no lock, the log still sealed, the lock's row names the fact, an unsealed value refused, an act whose second value does not open leaves no row |
| A2 "A fact's row also holds who wrote it, and the permission that the act named. A read that does not ask for them is sent nothing of them." | `fact-row` and `by-value-row`: `:who`, `:permission`. `read_path/answer.clj`, `with-asked-parts`: the one place where either is put into what a caller gets | Module's proof, groups 3, 9, 24. Read path's proof, group 5. "Is sent" is read as "is handed" (ruling 15): Rama sends the runtime the whole row |
| A3 "One more store key: a fact on the layer whose value is 'members' or 'everyone'. It takes a second write." | `door/declaration.clj`: `store-keys`, the rule `:who-may-see-fact-holds-its-word`. `gate/decision.clj`: `setting-fault`, `store-key-fault` (a second-write key earns no word) | Pure proof. Module's proof, groups 10 and 25: written, written again, a third word, another entity, sealed, a retract |
| A4 "A permission gets one part more: see, or see and act. A permission that was kept without the part reads as 'see and act' ... A gate refuses an act under a permission that says 'see'" | `act.clj`: the third form of `:permission-id`, and `allows`. `gate/decision.clj`, `standing`: `:permission-does-not-allow-acting`. `gate/batch.clj`, `standing-on-arrival`: the same word, final on arrival | Pure proof. Module's proof, groups 11 and 26. The door's proof: a holder of "see" meets the word at the door's request for locks |
| A5 "One more store key: a fact on the layer that holds an ordered list of layers. It takes a second write." | `store-keys`, the rules `:builds-on-fact-holds-layers` and `:builds-on-within-the-limit`. For a read: `read_path/layers.clj`, `builds-on` and `in-order` | Module's proof, groups 10 and 25, at 64 layers and over. Read path's proof, group 7: from a layer, with the newest list |
| A6 "A fact on the layer marks a key. For a marked key the gates keep each fact in one more order, by value ... The mark takes a second write" | `store-keys` and three rules. `record/tables.clj`, `facts-by-value`. `record/rows.clj`: `by-value-key`, `by-value-run`, `value-digest`, `with-level-since`, `marked-keys-of`. `gate/decision.clj`: `since-by-place`, `level-since-after`, `by-value-writes`. `gate/batch.clj`: `marks-places-asked`, `batch-end` | Pure proof: the order by value one act at a time, and a batch under marked keys. Module's proof, groups 12, 27, 28. Read path's proof, groups 3, 10, 15 |
| A7 "A read shows no fact of a batch that could still be tried again, and a place that holds a fact never gives nothing ... keep each batch's clock ... It is tried first, with a run." | `module.clj`: the depot `*batch-clock-depot`. `module/batch_clock.clj`. `module/microbatch_gate.clj`: the head of step 2, and `clock-must-be-kept`. `gate/batch.clj`: `clock-record`, `kept-clock`, `clock-row-to-keep` | Pure proof: a batch run twice with one clock. Module's proof, group 30 (a replaced place never gives nothing) and group 32 (a real batch made to run a second time: every row the same). Read path's proof, group 27. The first sentence is read as the contract reads it (ruling 16 (1)): no fact that a second try could change. The run held, so "the row before" is not built (ruling 82) |

**What to build, B: the read path.**

| Clause | Where it is code | The checks that hold it |
|---|---|---|
| B1 "A query written as data ... a place; an entity, or any run of neighbouring places; what points at a thing. Each is asked once, or kept live." | `read_path/query.clj`: `words`, `shapes`, `rules`, `check`. `read_path.clj`: `read-once`, `read-live` | Read path's pure proof, groups 1 and 2. Read path's proof, groups 2, 3 and 14, for each gate |
| B2 "The read path reads the layer's setting and the asker's permission. Until sign-in is built, who asks is what the caller says." | `read_path/layers.clj`, `standing`. `read_path/sources.clj`: `own-rows-source`, `permission-source` | Pure proof, group 3: the seven lines of the table. Read path's proof, groups 8 and 9 |
| B3 "The list that the layer gives, or the view's own. A layer of the list that the asker may not see is left out, and the read says that it left one out. For each place the nearest layer that holds a fact wins, also when that fact says 'nothing here'." | `layers/in-order`, `layers/left-out`. `read_path/answer.clj`: `walk-facts`, `walk-orders`, `whole` | Pure proof, groups 6 and 7. Read path's proof, groups 7, 8, 10 |
| B4 "A live read is Rama's push, one for each layer of the list ... The read path keeps a live read on each, and ends the reads that are no longer rightful." | `read_path.clj`: `open-push`, `join!`, `on-callback`, `on-change`, `on-arrival`, `settle!`. `answer/after-change`, `answer/after-arrival`, `layers/meaning` | Pure proof, groups 8, 9 and 10: every order of the callbacks. Read path's proof, groups 14, 15, 18, 19 |
| B5 "Those who look at the same thing share one push. It ends when the last one leaves." | `read_path.clj`: `join!`, `leave-sources!`, `close-when-given!` | Read path's proof, group 16: the shared pushes, 50 races of two openings, 50 of a join against the last leave; group 17 for the counts |
| C "a gate's slowest 1 in 100 at 100 acts a second; the same with live reads open on the layer; the opening of a canvas of 5,000 facts; how late a push is. The base on a real cluster cannot be measured here" | `test-store/softland/store/read_measures.clj`: M1 to M5 | A run, and no check: `runs/measures-final.txt` |

**The builder's rules.**

| Rule | How it is held | Evidence |
|---|---|---|
| R1 "No tool reads Rama by itself. The read path is the one reader of facts in the runtime" | One namespace opens a table | The search above. Read path's proof, group 21, makes the same search as a check |
| R2 "Nothing is shown before it is in the tables. No optimistic update, and no store of our own that we keep fresh beside Rama's push" | See "What the read path holds between two calls", below | Read path's proof, groups 16 and 18 |
| R3 "The read path never opens a push on a whole table or on a whole collection that keeps its elements apart" | `read_path.clj`, `path-of`: the one maker of a path | The trial above. Read path's proof, group 12: a whole layer ends in a word within two calls, and an unknown kind of source makes no call |
| R4 "A read that did not ask for a part of a row is sent nothing of it" | `answer/with-asked-parts` | Read path's proof, group 5 |
| R5 "A screen is shown the newest state. A caller that must see every act cannot rest on a push" | The namespace docstring of `read_path.clj` says the limit; ruling 78: the function's first call is the whole answer | Read path's proof, groups 14, 24, 25: twenty acts end at the last value, and no count of changes is asserted |
| R6 "Every word that the build makes is plain ... and is listed for Sid" | The two lists | Row 8 of the faults, and its script |
| R7 "The store's rules 1 to 21 hold where they touch. This build changes two." | Rules 13 and 16 of the store's page say the new truth | Module's proof, groups 10, 25 (rule 13) and 3, 9, 16, 24 (rule 16). The four proofs of before this build pass |
| R8 "What is marked not decided below is built so that the other answer is a small change, and its docstring says that it is not decided" | See the next table | Read |

**Not decided, kept only so that the build can run.** Each is built as kept, at one function whose
docstring says "Not decided". None is decided here.

| The thing | The one function, and its docstring |
|---|---|
| "One permission for each holder in a layer" | `gate/decision.clj`, `permission-place`: "kept as built and not decided". The declaration's rule `:permission-fact-sits-on-its-holder` is the other of the two places |
| "Who may write a layer's setting, what it builds on, and the mark of a key" | `gate/decision.clj`, `who-may-write-fault`: gives nil; "Not decided. Sid's word ... is 'the owner'" |
| "What a layer starts with" | `gate/first_act.clj`, `starts-with`: "Not decided ... kept only so that the build can run" |
| "A permission that says 'see' stays 'see'" | `gate/decision.clj`, `store-key-fault`: "to raise one, or to end one, is not built and not decided" |
| "A read that left a layer out says so" | `read_path/layers.clj`, `left-out`: "Not decided ... a working answer, for Sid" |

The working answers that the build session ruled beside these (rulings 9 to 13 and 65) each have
their one function too: `layers/in-order` (how far a read walks; a start layer that she may not
see), `decision/by-value-writes` (what a marked key may hold), `answer/counts?` (the store's own
facts in a tool's read), `sources/nearer-checks` (what points at a thing through layers). "One
moment for all the layers" has no code: nothing lines the layers up.

**Not in this build, by decision.** Nothing of it is built: the read path appends to no depot and
keeps no read (no `foreign-append!` in `read_path.clj`); the door seals for the log as it did; a
query has four words and a row two parts that can be asked for, so there is no looking back, no
finding by words, no "what stands on a fact", no "everything one person wrote" and no whole act
from a fact; no code ends a permission, changes a depot, publishes or promotes; no view, screen,
tool or agent calls the read path, by the search of the tree; nothing signs anybody in.

**"When this build passes".** "a caller in the runtime's process asks for a place, an entity, a run
of places, or what points at a thing, through a list of layers, and gets the value now and each
change after": read path's proof, groups 3, 7, 10, 14. "The read path has a proof of its own
against the module": `read_path_proof.clj`, with its row in the proofs' map. "The four proofs of
today stay and pass": they pass, with the counts in the section of the final runs.

**Explanations, as the tree's rules say.** Every namespace and every definition has a docstring,
each folder a map with a link to its parent, and every link resolves (the whole-tree checks above).
The read path is in the tree's entry, with its question in the table at the top. Its own page holds
the four sections that the contract names. The three lines of the store's page that the contract
names as untrue are replaced: that a gate keeps nothing of the plain bytes, that a store key takes
one write, and that nothing reads a fact for a caller. Sid's answers are in `src-store/README.md`
as his; this session did not edit that file. The faults that the pass found in the explanations
are rows 1 to 9 and 12 above.

### What the read path holds between two calls (R2)

Read from `read_path.clj`, and held by the proof for the fields of `held` (group 16).

- The read path itself, a value of its own type: the module's name; four handles of Rama's client,
  borrowed; its seven numbers; a counter for the ids of its live reads.
- `held`, one atom of three maps. `:pushes`: for each source, the delay around the one call that
  opens its push, the ids of the reads that hold it, and a mark that its first callback came.
  `:counts`: for each gate, table and routing id, how many entries of `:pushes` are under it.
  `:reads`: for each live read its query, its function, its turn, the set of its sources, whether
  it is opened, whether the whole answer is owed, and the routing ids of the tasks.
- One mark on a thread, for the length of a call of an asker's function.

That is all. No row, no value, no answer and no place-answer is among it. The values are in Rama's
pushes: each holds the current value of its path, and Rama keeps it level (Sid, 11 October: "i also
don't consider it a cache"). A read asked once holds what it fetched until it returns, and since
ruling 89 it takes nothing from a push. `:counts` is new in this session: it is a number about the
read path's own pushes and no value of the tables, and it changes only in the two swaps that put an
entry into `:pushes` or take one out, so it cannot differ from the map; the proof holds that at
eight moments and after 20 races.

### Fault tolerance, case by case

| The case | What the code does | How far it is shown |
|---|---|---|
| An event of the streaming gate that Rama runs again, also after its writes were committed | Every write of a decision is behind check (a), and the answer commits with the rest; the stray's write is an add to a set (`module/streaming_gate.clj`, "When Rama runs an event again") | Read. A repeat of an act is run (module's proof, group 4); Rama running an event again by itself is not forced by a check |
| A record of the clock's depot that is handed over again, or late | First write wins: a record of the row's own batch or of an older one writes nothing (`gate/batch.clj`, `clock-row-to-keep`) | Pure proof; module's proof, group 32 |
| A second attempt of a batch | Each attempt takes the batch's kept clock, so each fact's row, each answer's stamp and verdict, and each row of the facts by value are the same; a new lock's bytes and one word differ, and the door's three reads are held to closing for them | Run on the store's own module: module's proof, group 32, 52 keys compared; read path's proof, group 27: the asker's function is called once. Since ruling 86 a row's value is a String that is made from its bytes alone, so it is the same on a second attempt as every other field |
| A worker that starts again | PStates are durable; the gates keep nothing in the memory of a process; the kept clock is in a PState of a topology of its own | Module's proof, groups 13 and 31; read path's proof, group 26, under an open read. By Rama's module update with the unchanged module, in-process |
| The read path under a push that Rama ends | Each read on it ends with `:the-store-ended-a-push`; nothing is opened again by the read path | Read path's proof, group 19: Rama's own ending diff, made by the proof and handed to the callback; 40 crossings with an opening |
| A value sent anew after the first callback | Handled as the keys whose rows differ; nothing when no row differs | Read path's proof, group 19, new in this review (row 10) |
| A callback with the same value | Nothing is told: `sources/named` drops a key whose row is the same before and after | Read path's pure proof, group 11; read path's proof, groups 19 and 27 |
| A read that leaves inside its own function | `leave` takes no turn and waits for nothing | Read path's proof, group 23 |
| A live read that would take the read path past its own bound on pushes | Turned away in the swap that would join, before one push of that round is opened; an open read ends | Read path's proof, group 17, new in this review |
| A killed process, a move of a task's leader, a commit that fails after one task has shown its part, a module update between two attempts, a real cluster | Not reached by any check. The batch probe saw the window of a failed commit on a module of its own (ruling 17) | Not run on the store's module |

### Pass 3: the last pass

After rows 14 to 16 the hunt began again from the contract. What was run again on the tree as it
then stood: the script of the docstrings (967 definitions, none without one); the links of every
map (306, none leads nowhere); the keywords of the read path's, the record's, the gates' and the
act's code against the lists of words; the classes of words member by member; the R3 trial (the
same counts); the R1 search (one file). And each change that this session made to the source was
read once more as a diff, as new surface: the pair of functions and their four callers; the swap
of `join!` and of `leave-sources!`; `read-once` with no read of the read path's memory; every
docstring that was touched.

It found nothing new. One thing was seen and is not a fault of this build, and was left: 20
keywords inside the microbatch gate's pure code that are on no list of words (`:about`, `:parcels`,
`:read?`, `:meets?`, `:asks-rows?` and others). Each was there before the read build
(`git show c3e65c06`), so they are the store module's build's words and not this one's; this
build's own words in that file are on the store's page. To say what each is would take a reading of
that file line by line, which this review did not make (see below). It is named for whoever tidies
that list.

## Proposed for Sid's queue, decided by nobody

These are what this session found that only Sid can decide, or that Rama's team must be asked.
Nothing is written into `decisions/OPEN-QUESTIONS.md`. The rows that the builds before proposed are
in `RECEIPT.md` with these.

| # | The question | What is built meanwhile |
|---|---|---|
| 1 | The first row, with P12 and ruling 79: what "what points at a thing" means through layers, and by which way the nearer layers are checked. The way that is built needs one push for each place and each nearer layer, so a canvas of more than about 125 places cannot be kept live through three layers, whatever Rama's bound is set to | One live read may need 250 checks; past that it is turned away at once. Asked once the same canvas of 5,000 is answered whole. The cheaper way, the small nearer layer read whole or held by one push, is designed on paper and not built |
| 2 | Is Rama's option `pstate.proxy.limit` raised from its default of 1,000 pushes on one table on one task, and the read path's 900 with it? What a higher value costs a task is not known here | The option is not set. 900 stands under the default |
| 3 | Should the read path's count be by task, as Rama's bound is? It needs either a call of Rama's client that gives the task of a partitioning key, which is a question for Rama's team, or a small query of the module that says it, which would be the first query between the read path and the tables | The count is by table and routing id |
| 4 | The words of this session: `:keep-more-reads-live`, `:no-room-for-a-push-it-came-to-need`, `:pushes-under-one-routing-id`, `:max-pushes-under-one-routing-id`, `:max-checks-of-one-live-read`, `held-value`, `plain-bytes`, `plain-bytes-of` | Each is on a list of words, and Sid's to change |
| 5 | A question for Rama's team, from ruling 86: why a push on rows that hold a byte array is sent the whole value anew about once a second, and whether that is meant | A fact's row holds no byte array. The answers and the locks do; no read is kept live on them |
| 6 | From the final measures (ruling 100): a place that is written often, under many different live reads. Those who look at the same thing share one push; those who look at overlapping things do not, and each of their pushes costs the gate at each write: 898 different runs over the places that were being written took an act from 4.7 ms to 15.5 ms, in-process. Is a bound wanted on how many different live reads may hold one place, or is a view held to asking for a thing in the one way that is shared? | Nothing is built for it. The contract sets no target for a measure |

## What this review did not reach

- **Not read whole:** the store's page, `src-store/softland/store/README.md`, outside the sections
  that this build wrote or changed (read: how the parts relate, the decisions' rows of 11 and 12
  October, the engineering choices, the rules 9, 13 and 16 to 21; searched: the rest, the lists of
  words among it); the proofs' map outside the read path's two sections, the measures and the lines
  on a fact's row; the door's page; the decisions of 6 to 9 October in the tree's entry.
- **Not read line by line:** `gate/batch.clj` beyond the functions of the batch's clock, of the
  marked keys and of the standing on arrival, and beyond what this build changed in it, which was
  read as a diff; steps 3 and 4 of the dataflow of `module/microbatch_gate.clj`; `door.clj` and the
  seven files of `door/` beyond what this build changed in them; `act.clj` beyond its diff. For
  those this review rests on the builds' own validations, on the door's and the store module's own
  reviews of the whole, and on the proofs that pass.
- **The proofs were searched, not read whole.** 881 KB in seven files. Read: the helpers and the
  groups of the two proofs of the read path that this session touched, and each check of the four
  older proofs that reads a fact's row. Searched over all seven: comparisons that can hold either
  way, a class of words with a member that no check names, and every read of a row's value. A check
  that passes falsely for a reason which those three searches do not look for was not looked for.
- **A fresh session did not judge this one.** See the head of this paper.
- **Rama outside one process:** a cluster of several machines, a network, a killed process, a move
  of a task's leader, more than one copy of a row, a second runtime that opens pushes on the same
  tables. No check and no measure of this build reaches any of them.
- **Two routing ids on one task**, and so the case in which Rama's bound is met although each count
  of the read path's is under its own: not played by a check.
- **A value that Rama itself sends anew** after the first callback of a push, and Rama itself
  ending a push: the proof hands the read path Rama's own diff for each.
- **A migration** of a table whose rows hold a byte array to rows that hold a String: not written,
  since no cluster holds data.

## The final runs

The six proofs on the final tree, each with exit status 0. Each cut output is
`runs/final-<proof>.txt`.

| Proof | Command | Count line | Exit |
|---|---|---|---|
| act | `clojure -M:store -m softland.store.act-proof` | `{:test 8, :pass 160, :fail 0, :error 0}` | 0 |
| pure | `clojure -M:store -m softland.store.pure-proof` | `{:test 18, :pass 690, :fail 0, :error 0}` | 0 |
| module | `clojure -M:store-module -m softland.store.module-proof` | `{:test 33, :pass 354, :fail 0, :error 0}` | 0 |
| door | `clojure -M:store-module -m softland.store.door-proof` | `{:test 27, :pass 199, :fail 0, :error 0}` | 0 |
| read path, pure | `clojure -M:store -m softland.store.read-path-pure-proof` | `{:test 12, :pass 152, :fail 0, :error 0}` | 0 |
| read path, on the cluster | `clojure -M:store-module -m softland.store.read-path-proof` | `{:test 27, :pass 482, :fail 0, :error 0}` | 0 |

Against the last build's own final runs: pure 686 to 690, the four cases of the pair of functions;
read path pure 149 to 152, the two numbers and the word; read path on the cluster 445 to 482, the
group of the read path's own bound, the checks that the review added, and the acts that set them
up; act, module and door as they were, with checks written again and none added. The read path's
proof on the cluster was run once more after row 17's check was added, and its kept output is of
that run; the other five outputs are of the run before it, on the same source.

The measures of the contract's section C on the final tree are `runs/measures-final.txt`: one run,
558 seconds, exit status 0, 61 lines, and no opening of a push that Rama refused. The receipt puts
them beside the run of before the repairs, line by line.

PHASE_VALIDATION:pass
