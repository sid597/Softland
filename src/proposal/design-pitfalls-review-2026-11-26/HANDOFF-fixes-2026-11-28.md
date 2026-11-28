# The fixes of 28 November: what was built, and what is left

Written 28 November 2026 by the Claude Code session on Sid's machine that
built the fixes of `DECISIONS-AND-FIXES.md`, section 7.1, that needed nothing
from him, for Sid and for the session that comes next. It follows
`HANDOFF-fixes-2026-11-27.md` and assumes nothing of that conversation. Paths
are from the repository root. Two things here are Sid's: F1, his yes of
‹e17932a7›, and the second half of F7, which he gave during the build, in
this session: "plain words stay text; a value that begins as data and does
not read is refused." Every other choice is this session's and his to turn;
section 3 lists them.

## 1. What was built

On `main`, from `90cea545`. Nothing is pushed.

| What | Commit |
|---|---|
| F1: the sentence reads "What one gate puts in order is a lane.", and `src-store/CLAUDE.md` follows | `533d6b73` |
| F13: an act of another version is refused `:unknown-version`, also when it holds a part that the form does not name | `27ae8327`, `d0e6baa4` |
| F33: an answer says `:no-fact-given` where the read gives no fact. F34: the query word `:run` is `:range` | `d1e90878`, `b89195cf` |
| F12: the seam reads who may say a word from the wire's table | `d7357045` |
| The rest of F11: the library sends no frame that the runtime could not read | `e74375e0` |
| The result of a word that ends its own connection is written before the end | `b07443f1` |
| The two lists of `main.clj` are one table; a flag with its value left out, and a flag that is not known, are not run | `67244602` |
| F7, the second half: a value that begins as data and does not read is not stored as text | `3ea0316f` |
| `FIRE.edn` and `scripts/fire` | `fa5c0ed0` |

What changed beyond the list that Sid gave, from `git diff --stat`:

- `src-server/softland/server/watching.clj` and its pure proof: the server
  of 20 November is compiled against the store's source and reads the
  answer's word that F33 renamed (`b89195cf`).
- `src-wire/softland/wire/words.clj`: two new columns of the table of
  words, `:in-a-session` (F12) and `:can-end-its-connection`.
- `src-command/softland/command/client.clj`: a new function, `said`.
- `test-store/softland/store/read_measures.clj`: its three queries of a
  range say `:range`.
- `test-runtime/softland/runtime/README.md`: the rows of the four proofs
  that got sentences.
- The sentences of the store's proofs that held the old order of F13, and
  the two sentences of the bounds in `checks.clj`, say what holds now.

Not done by this session: a review of the build. A fresh session does that,
from the diff `90cea545..HEAD`.

## 2. What ran

On this machine, each cluster proof on Rama's in-process cluster behind the
machine's one cluster lock. The store's proofs but the module proof ran on
the tree as `d1e90878` has it, the cluster ones on a detached copy of that
commit; nothing of `src-store` changed after it. The
proofs that load the command line ran again on `3ea0316f`, the last commit
of code; the others of the runtime ran on the tree as `67244602` has it.

| Proof | Result |
|---|---|
| The store: `act-proof`, `pure-proof`, `read-path-pure-proof`, `permission-proof` | 166, 781, 283 and 72 passed, none failed |
| The store on the cluster: `door-proof`, `module-proof`, `run-again-proof` | 283, 510 and 48 passed. The run-again fixture's tables stand on 4 tasks and on 8: F13 changes no answer of a kept thing |
| `read-path-proof` | Three runs: 745 of 745 twice, and once 744 with one sentence failed. See below |
| The judge, `clojure -M:run fix 2000 1` in `gate-histories/` | Green in 50 seconds: agree, told, once, apart, proved and see |
| `read-measures small` | Ran to its end; its three queries of a range gave their 600 places |
| The wire's `pure-proof`; the server of 20 November's `pure-proof` | 38 passed; 68 passed |
| The runtime: `pure-proof`, `proof`, `two-lines-proof`, `open-session-proof`, `review-faults-proof` | 25, 50, 3, 5 and 6 passed |
| The command line: `command-pure-proof`, `command-proof`, `command-review-proof`, `line-review-proof` | 34, 16, 11 and 14 passed |
| The seven attacks | Each passed |

The sentence that failed once, in the first of the three runs of
`read-path-proof`: "through three lanes, a permission is given in the nearest
while the farther orders hold places: the lane that joins gives a whole answer
anew …". Of what it compares, one thing differed: one of the asker's later
messages was a change and not a whole answer anew. The places were the same.
F33 and F34 rename words in that path and change no step of a live read, so
this session takes it for an order of pushes that the sentence does not
allow for, and did not show that: the proof was not run on `90cea545` to see
whether it fails there too.

Two new sentences were held against a broken copy, since "it failed before
its fix" is otherwise only that the function did not exist:

- F12: with `:under` changed to `:an-actor` in the table, the sentence of
  the runtime's pure proof failed and showed the one word that differed.
- The end during a word: with `:sign-out` unmarked in the table, the line's
  review proof failed two sentences, and what the connection was handed was
  the answer anew, the last word and no result, which is the fault.

What a run shows of each fix, beyond that the code is there:

- **The answer with no number, the flags, F7's second half:** held through
  the library on a real line, and through the command line run as a
  process (`checks.clj`, `command-review-proof`).
- **F12:** held against callers that the pure proof makes, for each of the
  sixteen words. On a real line no new sentence says it: the sentences that
  were there hold a visitor's `:send` and an agent's words, and every proof
  of the runtime passes through `word-fault`.
- **The end during a word:** the order is staged, with the seam's
  `standing`, `watch` and `call` replaced. On a real line it is held by
  `:close-session` and `:sign-out` giving their result in `checks.clj` and
  in the line's review proof, in each run that was made; a real line does
  not choose the order.
- **F13, F33, F34:** held by the store's proofs. No client asks for a range
  yet, and the command line reads one place.

## 3. The choices a session made, for Sid to turn

Each is small to turn; where turning one is more than a rename, it says so.

- **F1.** The entry keeps the sentence as it was first approved, the
  question as the review's session put it, and his answer, beside the new
  sentence. Nothing else of the section is touched.
- **F13.** The part that names the edition is marked in the declared shape
  (`:edition`), and the engine reads it only before it would say
  `:unknown-part`. The other order was weighed and left: reading the version
  before every part would also put `:unknown-version` in front of the door's
  own two parts, so a thing with no time and a wrong version would stop
  saying `:no-door-time`. A thing that names no version at all counts as
  another edition. This changes what a gate answers for old things, which is
  E5's matter: a thing with an unknown part and a version that is not 1, or
  none, was `:unknown-part` and is `:unknown-version`. No kept thing is of
  that kind: the run-again fixture's tables stand (section 2).
- **F33.** The answer's word is `:no-fact-given`. It says that the read
  gives no fact at the place and nothing of whether one was ever written;
  the record keeps `:no-fact` for a place where none was ever accepted. No
  second word was added to tell the two apart: that would tell her of a
  lane that she may not see.
- **F34.** The query word is `:range`, the standard word for it, with
  `:range-end`, `:range-not-named`, `:range-ends-out-of-order`,
  `:range-end-not-ordered` and the two rule names. The reason that said
  "run" for the login sitting is `:session-with-no-asker`. Inside the read
  path "a run" stays the neighbouring rows of one table that a source
  reads: the path kind `[:run from to]`, `entity-run` and the like, the two
  numbers `:run-start-places` and `:max-places-of-one-run`, and two words
  that a caller can meet, `:a-run-grew-too-large` and `:places-of-one-run`.
  Renaming those too is a change of names only, across `rows.clj`,
  `sources.clj`, `read_path.clj`, the module's `door_reads.clj` and their
  proofs.
- **F12.** The table got a second column, `:in-a-session`, for the one
  requirement that `:who` had no value for. The seam's one function is
  `word-fault`. The line asks it for `:watch` and `:leave`, which it does
  itself, so a `:watch` that its caller may not say would get a result,
  `{:id n :gives word}`, as F11 set for a word that the line does not take.
  Today every caller may say both.
- **F11, the answer with no number.** The library says the runtime's own
  word, `:read-the-frame`, for a frame that it did not send: the same
  function gives the same verdict at either end, and a caller then handles
  one word. A word of the library's own would be the other choice. `said`
  is new, so that a word with no number can be read at all. The library
  does not hold a frame's bytes to the runtime's bound: a frame over it
  still closes the connection.
- **A word that ends its own connection.** The table marks the three words,
  `:can-end-its-connection`. The mark is on the word, so an `:end-sign-in`
  of another sign-in is held the same way, to no effect. While the end
  waits, the watches are left at once and nothing bounds the wait but the
  word's own call of Auth.
- **The command line's table.** One vector, `commands`: for each command
  its plain arguments by name, its flags with the name of each value, a
  note, what it does, and its function. How a command is typed is made from
  the entry, and the namespace docstring no longer lists the commands.
  `agent new` and `agent end` are two entries.
- **Flags.** A command takes only the flags of its entry, and `--as`. So a
  flag that another command takes is refused too: `who --lane l`,
  `read e k --permission p`, `give … --lane l`. A value that begins with
  `--` is read as a flag, so `--lane --as f` is a lane left out. A flag
  given twice is refused. `new-session --model` with no name ends with 2
  and is not sent. `:read-the-command` now carries `:why` and, for a flag
  or an argument, `:at`.
- **F7, the second half.** His sentence left one thing to a session: what
  "begins as data" is. Built: what is typed begins as data when its first
  character is one with which EDN writes a thing that is no bare word: `{`,
  `[`, `(`, `#`, `"`, `:`, `\`, a digit, or a sign before a digit. So a
  number counts. `3 chairs`, `1.2.3` and `2026-11-28`, typed bare, are
  refused and are typed in quotes, `"3 chairs"`; the ground is that `12O`,
  a number with a slip in it, would otherwise still be stored as a text.
  The narrower reading leaves numbers out and refuses only what begins with
  one of the first seven. It is one line of `begins-as-data?` to turn. The
  values are read before anything is connected, so the refusal ends with 2
  like every command that could not be read; for that the table says which
  arguments and flags are values (`value`) and which are texts (`text`). It
  holds for `e`, `k`, `v`, a who, a lane, a permission and an id alike.
- **`scripts/fire`.** It prints each track's own `:waiting-on` and `:next`.
  The two lines that named fixes and loops are gone, and so is the entry it
  made up when this folder had no `FIRE.edn`.

## 4. What is left

- **Waits for Sid, as before:** F2, F14 to F18, F22 to F26, F28, F30, F32,
  and two of the three questions of `HANDOFF-fixes-2026-11-27.md`, section
  3.1: a file sent again in another session, and the secret before her yes.
  None of the fixes above needed one of them, and none changes a stored
  form of the store or of Auth.
- **F29 and F31** are on neither list. This session did not look at what
  the permissions rebuild built of them.
- **Not run:** the cluster proof and the worker-kill proof, Auth's proofs
  (nothing of `src-auth` changed), the cluster proofs of the server of 20
  November, the two measures in full, and the judge's other modes.
- **A sentence of `read-path-proof` that failed once of three runs.** See
  section 2. Whether it also fails on `90cea545` is one run to find out.
- **The read path's "run" inside the code**, if Sid wants it gone too
  (section 3, F34).

## 5. Seen and not changed

- `src-store/softland/store/read_path/README.md`: the row of `:session`
  still says "the run that the read is asked in" and tells of a run's
  permission, and the page says "the eight rules of a query" where
  `query.clj` holds ten. `src-store/softland/store/README.md` (lines 736 and
  1269) and `src-store/MODEL.md` (line 192) still tell of `[:run session]`
  in a permission, which the rebuild removed.
- The read path's proofs name a login sitting `:run-7` and say "a read
  asked in a run" for it.
- `src/proposal/runtime-2026-11-21/FIRE.edn` still says that it waits on
  the permissions rebuild and that a window owns `gate-histories/`.
- `DECISIONS-AND-FIXES.md`, section 7.1, still lists the built fixes as to
  do. It is the review session's page.
- The four points of `HANDOFF-fixes-2026-11-27.md`, section 3.6, stand as
  they were.
