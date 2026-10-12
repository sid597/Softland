# Review: the pages, the read path, and the tree as a whole

Reviewer: a fresh session (Opus 5.5), 13 October 2026. Three read-only checkers held the proofs' map
against the proof files; each of their findings below was read again in the source before it was
changed. "The map" is `test-store/softland/store/README.md`; "the store's page" is
`src-store/softland/store/README.md`. Nothing is staged or committed.

## Faults found and fixed

| # | Where | What it said | What the code does | What was changed |
|---|---|---|---|---|
| 1 | The store's page, "How the parts relate", streaming steps 1, 2 and 7 | The event goes to step 7 "with the stray's entry", and "with a copy of that answer's entry" | The event carries the stray's word, digest and task, or the row that it read; the entry is made behind the commit (`module/streaming_gate.clj`, `add-to`, steps 0, 1, 8 and 9) | Steps 1 and 2 say what the event carries; step 7 says that the entry is made there, of the row that step 6 wrote or step 2 read |
| 2 | The store's page, "Decisions this module stands on", row F2 A | "That the acts depots are the replay log is a session's sentence, and no decision of his" | No page and no docstring of either tree says it; the contract lists the line among those that are replaced | The sentence is out; the row says what is in the logs and what they give |
| 3 | The store's page, same section, row K1 A | The request for locks "left the store's own keys with entry 2" | `store-keys` in `door/declaration.clj` holds five keys and no request | Said as what is: it is none of the store's keys, by entry 2 of 12 October |
| 4 | The store's page, "Names in Rama", `Placing` | "The module of one query that the rebuild launches" | `rebuild.clj` launches nothing; `rebuild/placing.clj`, "Callers. The rebuild's proof" | "that a caller of the rebuild launches" |
| 5 | The store's page, "Words", the logs of answers | `:stray-sent`, `:sent`, and `:layer` and `:uuid` of a kept answer were on no list of words (R8) | `gate/batch.clj`: `stray-sends`, `kept-answer`, `rows-to-keep`; `record/tables.clj`, `microbatch-task` | Two rows more in that table |
| 6 | `src-store/README.md`, the two aliases | "The pure code and its three proofs"; the list of `:store-module` ended at the read path | Four proofs run on `:store` (`rebuild-pure-proof` is the fourth); the rebuild's reader, its placing, their proof and `gate-measures` run on `:store-module` (`test-store/README.md`) | Both sentences say so |
| 7 | `src-store/OPEN-QUESTIONS-rama.md`, question 1, "Meanwhile" | "The rows of the answers and of the locks keep their byte arrays" | No table of locks; the byte arrays are an answer's `:digest` and the digests in a value of `:to-keep` (`record/tables.clj`) | Says those two |
| 8 | `door/README.md`, row H1 A | "the source entry says what became of its code" | The source entry says where the code of the locks is (commit `13a1d65b`) | The store's page's wording |
| 9 | `rebuild/README.md`, "Words" | What a named thing carries, `:table` and `:part` of `left-out`, `:depot` and `:at-once` of `depots` were on no list (R8) | `rebuild.clj`: `named-entry`, `taken-entries`, `batch-facts`, `kept-clock`, `left-out`; `rebuild/reader.clj`, `depots` | One row more, and two rows say their parts |
| 10 | `read_path/answer.clj`, namespace docstring | `:not-a-value`: "the nearest fact's bytes are no value of the form" | A row holds a String, and no write of a gate gives one that is no value's text (`record/rows.clj`, `read-value`; ruling 5) | "the nearest row's String is the text of no value of the form", and that no gate's write gives it |
| 11 | `gate_measures.clj`, namespace docstring | "behind the build's lock" | In this tree a lock is an encryption key (`src-store/CLAUDE.md`, "Words") | "while no proof and no other measure runs on the machine" |
| 12 | `gate_measures.clj`, `-main`, the second line that it says | "those sends took their locks from a stock" | The M1 lines are of another tree, the read build's; this tree's door keeps nothing | The line names that tree |
| 13 | `test-store/README.md`, the fourth proof | "The proof is the door's one maker; no other maker uses the door yet" | The read path's proof, the rebuild's proof and both measures call `door/send-act` | "beside the proofs and the measures of this tree, no maker uses the door yet" |
| 14 | `test-store/README.md`, the measures | "a gate under 100 acts a second, the same with live reads open" | M2 is the streaming gate alone (`read_measures.clj`, `m2`) | "the streaming gate again" |
| 15 | The map, the table of files, `gate_measures.clj` | "It runs on Rama's in-process cluster" | Two clusters, the second with `StoreAtOne`; one handle on `$$microbatch-answers` (`-main`) | The row says so |
| 16 | The map, the act's proof, group 1 | "the version and the four limits" | Three: subjects, collections, a carried stamp (`act_proof.clj:190`) | "the three limits", named |
| 17 | The map, the act's proof, group 6 | The three forms of a permission as something checked there | A `shown` line with no check (`the-limits`); group 2's cases hold them | "it prints ...; group 2 holds each as a case" |
| 18 | The map, the pure proof, group 2 | "a bare word that the store's keys were (`:class`, `:permission`, `:lock-request`)" | The cases say "the bare word" and hold that each passes (`pure_proof.clj:644` to `652`) | "one of the bare words ..." |
| 19 | The map, the pure proof, group 11 | For each of the three keys "one that names the fact there" | Only who may see has that case (`pure_proof.clj:1447`) | Said of who may see alone |
| 20 | The map, the pure proof, group 17 | "the count of the rows that were compared is held" | It holds that more than 20 are rows that the batch wrote (`pure_proof.clj:4153`) | Says that |
| 21 | The map, the pure proof, group 18 | "Each batch leaves its id and its clock" | A batch of strays alone leaves its id and no clock; and the group also holds a batch of repeats alone, a run that closes once, a second attempt and 50 orders (`pure_proof.clj:4248` to `4320`) | Each is said |
| 22 | The map, the pure proof, group 22 | No name "whose name holds lock, seal or stock" | A word of the name or of its namespace that begins with lock, seal, stock, unlock or unseal; `clock` passes (`names-a-lock?`) | The rule as the function has it |
| 23 | The map, "What the pure proof does not reach", last line | The search "reads no page of explanation" | It reads the `.clj` files under `src-store/` alone: no proof, no name made at a run (`source-files`, `lock-name-cases`) | Says each |
| 24 | The map, the module's proof, head | Seven stand-ins "note it, and throw once" | Four of the seven throw (`module_proof.clj`, namespace docstring; `put-noting-functions!`) | "four of the seven throw once" |
| 25 | The map, before group 15 | "group 33 runs after it, and group 14 after that" | 13, the first part of 33, 14, the rest of 33 (`the-checks-on-the-store-as-defined`) | The order as run |
| 26 | The map, group 33 | "For the third, the hand selects `:closed-through`" | The select serves the second of the three, that each answer read was of a closed batch | "For the second of those three" |
| 27 | The map, the paragraph on selects of the base | "but for group 17" | Group 32 looks before the closing too (`a-place-of-the-base-never-gives-nothing`) | "groups 17 and 32" |
| 28 | The map, the hand as a reader | "since no reader is built yet" | `read_path.clj` is built | "so that the gates stay proven without the read path" |
| 29 | The map, "What in the module's proof rests on a time" | The 60 s and the 1.5 s as group 34's alone | Groups 35 to 37 are held to the first, group 33 to the second | One sentence more |
| 30 | The map, "does not reach", first line | An act appended a second time "is a second thing under a used act-id" | It is the same thing: a repeat (`module_proof.clj:1716`) | "that is a repeat" |
| 31 | The map, "does not reach", a reader through layers | "no read path is built in this part" | The read path's two proofs show it | Points to them |
| 32 | The map, "does not reach", a second forced failure | "the group forces one" | Each of the four groups of the second cluster forces one, of its own kind | Says so |
| 33 | The map, "does not reach", an answer with no entry yet | "no read of the proof meets it" | Group 37 reads in pairs that can meet it, and prints whether one did; no check rests on it (`send-and-watch!`) | Says so |
| 34 | The map, "does not reach", the aims of speed | "The measures of the read build" | Two runs of measures | "The two runs of measures" |
| 35 | The map, the door's proof, head | "The proof is the door's one maker" | As row 13 | "The proof plays the door's maker" |
| 36 | The map, the door's proof, group 6 | "the log holds a vector, a list and a sequence" | `:facts` is equal to what was given, a vector only where one was given, sequential in each case (`door_proof.clj:617` to `638`) | Says that |
| 37 | The map, the door's proof, group 7 | "Those are" four kinds; "A value of 65,507 characters is accepted" as the edge | Twenty-one kinds go in, `:sealed` with `:lock-id` and a fact under `:softland.store/lock-request` among them; the edge is 65,536 bytes of text, held on both sides (`door_proof.clj:684` to `744`) | "Among them", and the edge as checked |
| 38 | The map, the door's proof, "The checks that rest on a time" | One check | Also the wait of 600 ms held under 5 s, and a call held under 300 ms (`door_proof.clj:503`, `509`) | One line more |
| 39 | The map, "The module's two clients, and four proofs" | The rebuild's proof: the door, its hand, the reader, selects | It also reads an answer by the module's query (`rebuild_proof.clj:171`, `246`) | Said |
| 40 | The map, the read path's proof, "What it changes" | It replaces three functions "as the module's proof does" | The module's proof replaces seven | "in the way in which the module's proof replaces seven" |
| 41 | The map, the rebuild's pure proof, group 4 | A thing that the log says was accepted "is made again as accepted" | Its answer is; no row of its fact is made (`rebuild_pure_proof.clj:375`, `376`) | Says both |
| 42 | The map, the rebuild's pure proof, group 5 | "every partition of every log turned back to front" | The logs of acts and of answers; the log of clocks is not turned (`rebuild_pure_proof.clj:394` to `397`) | Names the logs |
| 43 | The map, the rebuild's proof, "What it does" | On each cluster through the door and by hand | The second cluster sends by hand alone (`send-and-read-for!`) | Says each cluster |
| 44 | The map, the rebuild's proof, at rest, and "What it changes" | "where a worker is started again"; "a worker is started again" | The proof makes two throws and pauses the microbatch gate once; no form of it starts or holds a worker | Says what the proof does |
| 45 | The map, the rebuild's proof, group 2 | No kind of thing named | The first cluster sends each kind of B4, on both depots (`rebuild_proof.clj:285` to `378`, by its comments) | The kinds are listed |
| 46 | The map, the rebuild's proof, group 3 | "before that cluster's comparison of group 2"; the logs "hold copies ... and no entry of an attempt that did not count" | Group 2 does not run there; group 3 compares itself; the counts of entries are printed and no check holds them (`test-ns-hook`; `rebuild_proof.clj:715` to `718`) | Says each |
| 47 | The map, "The measures", the read build's | "the same with 1, 100 and 1,000 live reads"; a push's lateness "in a streaming layer and in the base" for both tables | M2 is M1's first shape; the facts by value are measured in a streaming layer alone (`read_measures.clj`, `m2`, `m4`) | Says each |
| 48 | The map, "The measures", the gates' | No word of how a set of times is printed, of the second cluster, or of D3's two readings | Count, least, middle, 9 of 10, most; `StoreAtOne`; the rates held and one maker one by one (`gate_measures.clj`, `times`, `d3-most`) | Said |

Left as it is, with its reason. `read_path.clj:132` and `read_path/README.md:309`, "never a lock": the
tree's rule for the word. `act.clj:90`, the same rule. Each `:sealed`, `:lock-id` and
`:softland.store/lock-request` in the four proofs and in the map: a check that holds the part refused.
`door/README.md`, rule 3, and the store's page, rules 5 and 21 and rows H1 A and K1 A: a rule of the
contract or a decision named by its name. `pure_proof.clj`, group 22: the search for the names.
Each "no longer" and "any more" in the read path and its proofs: of a read's sources at a run, no
history of the code.

Run after the change to `read_path/answer.clj`: `read-path-pure-proof`
`{:test 12, :pass 152, :fail 0, :error 0}`; `read-path-proof`
`{:test 27, :pass 480, :fail 0, :error 0}` (`runs/try-review-read-path.txt`).

## "When this build passes": each clause and its check

| Clause | The check on disk |
|---|---|
| A maker's value is in the log as she gave it | `door_proof.clj:542`, "the value in the log is as the maker gave it and not in its normal form"; `:849`, `:858`, `:882`, of acts that are refused; `:634`, facts as a vector, a list and a lazy sequence. `module_proof.clj`, groups 14 and 33, "the depot holds each thing sent, as it was sent" |
| No code of the store makes, asks for, keeps or uses a lock | `pure_proof.clj`, `no-code-of-the-store-names-a-lock`: the symbols and keywords of `src-store/**/*.clj`. `module_proof.clj:2726`: no table of locks and no query of a request's locks on the cluster. `door_proof.clj:429`: a door fetches four handles and no more. It reaches no docstring, string or comment; a search of this review found none in `src-store` but the rule for the word |
| Each answer that a gate gave is in a log | `module_proof.clj:2863` and `:2865` (streaming), `:3005` (the base), `:3834` (after the failures of the second cluster). Named on the store's page and not held by a check: an answer can be read before its entry is appended, and in a store that never runs again it has none |
| A rebuild makes the tables again from the logs alone, and a proof shows them equal, with each exception named and its reason | `rebuild_proof.clj:613` and `:616`, nothing named and no row apart, on a store at rest; `:717` and `:718` after a failed attempt of each gate. The exceptions: `rebuild/left-out` in `rebuild.clj`, three parts each with `:why`. Handed in and in no log: the number of tasks and the task of a route (`rebuild/README.md`) |
| A permission can be deeper than four | `module_proof.clj:1228`, `:1299` to `:1313` (depth 2 to 32 accepted, 33 refused), `:2142` (the base); `act_proof.clj`, `the-limits`; `pure_proof.clj`, group 3 |
| The proofs of today stay and pass | The build's own runs, `runs/after-the-log-and-the-rebuild-*.txt`: act 162, pure 712, module 398, door 239, read path pure 152, read path 480, rebuild pure 24, rebuild 22, each with no fail and no error. This review ran the two of the read path again |
| The checks of the locks and of the seal go with what they checked | **No check.** The search of group 22 reads the source alone. A search of this review over `test-store` found a lock, a seal or a stock only where a check holds its part refused, and in the two lines of rows 11 and 12 |

## What the four scripts printed

- `docstrings.clj`: 1,112 definitions, 27 with no docstring, all in `test-store/softland/store/rebuild_pure_proof.clj` (25 at the first run; the file was being changed).
- `links.py`: "links in the maps of both trees: 379 | that lead nowhere: 0".
- `words.clj`: after rows 5 and 9, no keyword that this build made in the gates, the record or the act is on no list; what stays listed is Rama's option names, the act's shape language and the words of a batch from the store module's build.
- `members.clj`: every declared word of each of the eight classes is named by a check; none under "named by none".

## For another reviewer's file

The way in:

- `src-store/softland/store/door.clj`, namespace docstring, "State", and `open`: "The module has a third depot, of the clocks of batches" and "The module's third depot". The module has five. Right: the two depots of answers and the depot of batch clocks take no act, and the door opens none.
- `test-store/softland/store/pure_proof.clj`, `tools-own-keys`: "the bare words that the store's keys were before they took the namespace", with `:lock-request` among them. Right: bare words that look like the store's.
- `pure_proof.clj:626` and `:636`: "the three new keys". Right: the three keys of the read build, or their names.
- `pure_proof.clj`, `the-doors-wait`: `:request-wait-ms` stands for "any other name"; it was the name of a number of the stock.

The gates and the logs:

- `src-store/softland/store/module.clj`, namespace docstring: "whether a log of the store is ever trimmed is Sid's". He decided it: entry 3 of 12 October, "forever never trim". Right: name that entry, or leave the clause out. The sentence "From a depot of acts and its gate's depot of answers the tables can be made again" is said as a fact and is true as far as the rebuild's proof holds it; it does not say what is handed in (the number of tasks, the task of a route) or the three parts that are not made.
- `test-store/softland/store/module_proof.clj`, namespace docstring: "since no reader is built in this part" and "since no rebuild is built in this part". Both are built. Right: so that the gates stay proven without the read path, and the logs without the rebuild's reader.
- The same docstring: "the one throw that the proof makes", while four stand-ins throw once each.
- `pure_proof.clj:53`, `:1634`, `:3960`: "steps 6b and 7 of the streaming gate". The gate's walk has steps 6 and 7 and no 6b.
- From a checker's reading, not read again here: `module_proof.clj:1298`, the half "its first permission has depth 1" compares a literal with itself; `:1680`, "each of those refusals got a stamp of its own" holds one refusal's stamp; `:2271`, `:2515`, `:2632`, a name says where an act was decided or that a row came in a reply, and the forms hold words and rows only; `pure_proof.clj:4053` and `:4110`, a name says which of two wins and the form takes either; `:4097` and `:4293`, a comparison of the proof's own constants.

The rebuild:

- `test-store/softland/store/rebuild_pure_proof.clj`, the hand-made things `a-marks` to `b-stray`: no docstring (27 by `docstrings.clj`).
- `rebuild_pure_proof.clj:349`, `(is (= rebuild/left-out (:left-out result)))`: it can only pass, since `from-logs` gives that constant.
- `rebuild_pure_proof.clj:401`, the line shown: "every partition of every log turned back to front", while the log of clocks is not turned.
- `test-store/softland/store/rebuild_proof.clj`, namespace docstring, "What it does, on each of two clusters": the second cluster sends by hand alone.
- `rebuild_proof.clj:715`: "a second run leaves copies, and no entry of an attempt that did not count" is a line shown beside two counts; no check holds it.
- From a checker's reading, not read again here: `rebuild_proof.clj:587`, `:answered? true` can only pass on the first cluster, where nothing is waited for.

## For Sid

- `src-store/CLAUDE.md` still names the seal, in "Words" and in the line on the rig's findings. No session edits that file.
- `gate_measures.clj` says that a maker "holds" a rate when the last act of the rate was answered within a second of its due moment. The run fails on nothing, and the second is a session's reading of "holds".
- Decision 2 of 7 October points to "the store's page, rule 19" for what a second run of a gate does not give. Rule 19 now says it as what the logs of answers hold for a rebuild; the pointer still lands on it.
