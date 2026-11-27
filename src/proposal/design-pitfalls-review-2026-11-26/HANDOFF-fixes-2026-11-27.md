# The fixes of 27 November: what was built, and what is left

Written 27 November 2026 by the cloud session that built nine fixes of
`DECISIONS-AND-FIXES.md`, section 7.1, for Sid and for the session that
comes next. It assumes nothing of that conversation. Paths are from the
repository root. Nothing here is a decision of Sid's: he gave a go on the
nine fixes, and every choice inside them is a session's and his to turn.

## 1. What was built

Eleven commits on `main`'s head of that day (`25cafbf`), in three pull
requests, each stacked on the one before.

| Fix | Commit | Pull request |
|---|---|---|
| F3: Auth's spec says `:own-lane` | `0deb00f` | #41 |
| F4: three docstrings cite an entry only for what it holds | `edbbfe2` | #41 |
| F5: five places of the read path's page | `7701b0c` | #41 |
| F6: the docstrings say that the secret is on the wire before her yes | `ff9c637` | #41 |
| F7: a command with a missing argument is not run | `47cff77` | #42 |
| F8: `send` keeps a UUID with its act | `b3de805` | #42 |
| F9: the exit status, and a `watch` that could not be sent | `04f7858` | #42 |
| F10: the command's own word, `:find-a-credential` | `52d3b41` | #42 |
| F10, F11: what the line answers | `1ab68b9` | #43 |
| F10, F11: the library hands on the runtime's own word | `e4e58a2` | #43 |
| F10: the command decides on that word, not on 401 | `85a435c` | #43 |

What ran, on the last commit, on Rama's in-process cluster in the cloud
container: `softland.runtime.proof` 49 passed; the seven attacks, each
passed; `command-proof` 16; `open-session-proof` 5; `review-faults-proof` 6;
`two-lines-proof` 3; the wire's pure proof 38; the runtime's pure proof 22;
and the three new proofs, `command-pure-proof` 22, `command-review-proof` 9,
`line-review-proof` 11. Each new sentence failed before its fix. The cluster
proof and the worker-kill proof were not run.

Not done by this session: a review of the build. A fresh session does that,
from the three pull requests or from `25cafbf..85a435c`.

## 2. The choices a session made, for Sid to turn

- **F7.** Every command takes an exact number of plain arguments; with
  another number it ends with 2 before anything is connected. A retract is
  typed `write e k nil`.
- **F8.** What makes an act of a file the same act is who sends it and the
  act as the file holds it. The `:session` is not part of it.
- **F9.** 2: the command could not be read. 1: a "could not", also in a
  result of `send`, at the end of a `watch`, and for an ask that ran out.
  3: a refusal, a gate's or her no at `sign-in`. A `send` that holds both
  gives 1. `:nothing-yet` and `:not-mine` stay at 0.
- **F10.** One new word of the command's own: `:find-a-credential`.
- **F10, F11.** A word that the line does not take gets a result,
  `{:id n :gives word}`, a `:watch` too; `{:id n :ended word}` is only the
  end of a watch that the line opened. A number in use is said as
  `:read-the-word` with `:why :number-in-use`. Every limit is `:take-more`;
  two new `:why` values, `:too-many-watches-on-a-connection` and
  `:too-many-waiting`.
- **F4.** In `seam.clj` the sentence "nothing that is written plain in an
  act is made from the person" is gone; the docstring says "A person's id
  says nothing … and neither does an id made from it", which is what entry 1
  of 9 October holds.

## 3. What is left

### 3.1 Three questions for Sid

- **A value that does not read as EDN (F7, second half).** Example:
  `write :note :body {:a 1`, with the brace left open, is stored as the text
  `{:a 1` and ends with 0. Stored as text, or refused? A bare word (`oak`)
  and plain text (`hello world`) are stored as text today, and the command's
  page says so. The building session says: plain words stay text; a value
  that begins as EDN data and does not read is refused.
- **A file sent again in another session (F8, second half).** Example: she
  sends a file, types `new-session`, and sends it again. Today each act
  answers `:not-mine`, nothing is written twice, and the command ends with
  0; a sentence of `command_review_proof.clj` holds that. Leave it, say it
  plainly with a status that is not 0, or send the acts as new acts? The
  building session says: leave it; the case goes when `:session` leaves the
  act (F28).
- **The secret before her yes (F6).** Keeping it off the wire needs a second
  secret for the polls and its hash in Auth's ask: a stored form of Auth.
  The building session says yes, when Auth's forms are next changed.

F14, F15 and the permission page's question 13 are as
`DECISIONS-AND-FIXES.md` has them.

### 3.2 Waits for Loop 1 of the permissions rebuild to land

Loop 1 edits `test-runtime/` and `src-command/softland/command/main.clj`
where a permission or a who is built. These touch the same files:

- **An answer with no number is never read (F11, one part).** A word whose
  frame the runtime could not read waits and ends as `:hear-the-runtime`.
  The fix the building session thinks is right: the library reads its own
  frame with `frame/read-one` before it sends, and gives `:read-the-frame`
  at once. `checks.clj:455-464` ("the bounds") tests the runtime by sending
  exactly such a frame through `client/say`, so those two sentences have to
  send the raw frame another way, in the same change.
- **`typed` and the `case` of `run` are two lists** kept in step by hand in
  `main.clj`. One table that holds the count, the text and the function of
  each command would remove it; it re-indents the whole `case`.

### 3.3 Waits for the rebuild itself

- **F12.** The building session holds it: the fix it thinks is right has the
  seam read the table's `:who`, so that the rule is said once, and that is
  in `seam.clj:677-756`.
- **F13, F33, F34.** Store code that Loop 2B rewrites, and that Loop 2A's
  judge is checked against untouched.

### 3.4 Waits for a session on Sid's machine

- **F1.** Decided, two sentences. It needs the id of his "yse" from the
  record, which cannot be reached from the cloud, and it edits the section
  whose second entry is still to be written (`lane-2026-11-25/OPEN.md`,
  item 2).

### 3.5 Free to build: no question, and no file of a running loop

- **A flag with its value left out, and a flag that is not known.**
  `write e k v --lane` writes into her own lane without a word; `--lain x`
  is ignored. The same kind of slip as F7, in `parsed` of `main.clj`.
  `new-session --model` with no name sends `""` today, which the check
  refuses.
- **The result of a word that ends its own connection can be lost.** The
  end closes the line before the worker writes the result, and `say!`
  writes nothing on a closed connection (`line.clj`; read `seam.clj` before
  touching it). `sign-out` then ends with 1 though it worked.
  `checks.clj:268` and `command_proof.clj`'s `sign-out` rest on the result
  winning.

### 3.6 Seen and not changed

- `http.clj:542` says `:read-the-frame` with `:why :too-large` and a `:most`
  that the table's entry does not name.
- `gone!` of `client.clj` can run twice, on a close and then on an error.
- `read_path/README.md`: row 329 ("Who asks") and "ten words" on line 388
  were not checked against the code.
- `.github/workflows/deploy.yml` runs "Deploy to Fly.io" on every push to
  `main` and every pull request. It is from the Electric starter app that
  the repository began as, has no token, and fails at once.

## 4. Pages that are out of step

Not edited, since other sessions own them:

- `FIRE.edn` beside this page still says that F6 to F12 are safe to start.
  F3 to F11 are built, but for the one part of F11 above.
- `DECISIONS-AND-FIXES.md`, section 7.1, lists the built fixes as to do. Its
  line numbers are those of `e7f5ca33`; `main.clj` and `client.clj` have
  moved.

## 5. For a session that builds in the cloud

- The record cannot be reached. Nothing here was checked against it.
- The proofs run there: install the Clojure command line, and Rama's
  repository answers. The command line's proof takes about 90 seconds.
- The lane cut's checker of "same code" is in history:
  `git show df2a29f^:src/proposal/lane-2026-11-25/same_code.py`.
