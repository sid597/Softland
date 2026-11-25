# After the cut: what is open

Written 25 November 2026 by the session that carried `STARTER.md` through, for
Sid and for the session that comes next. It assumes nothing of that session's
conversation. Paths are from the repository root; W is this folder.

## Where things stand

`STARTER.md` is done and pushed on `claude/funny-bell-qxf3pu`, seven commits
on top of 4369cfd. Not merged to `main`: that is Sid's.

- The docstrings and comments of 46 source files (store, runtime, wire, Auth,
  command line) are cut from 9,575 prose lines to 5,029. No code changed:
  `python3 -I W/same_code.py 4369cfd <file>` prints "same code" for each.
- A sentence that said whose call a thing is ("not decided", "a session's",
  "Sid's decision of …") left its docstring and became an entry in a
  `whose-call.edn` beside each tree's code. 122 entries: 52 `:not-decided`,
  31 `:a-sessions`, 39 `:decided`. The forks' own files are in `W/calls/`;
  `W/assemble_calls.py` makes the five files from them.
- `src-store/CLAUDE.md` is the draft that was in this folder.
- Every proof ran once after the cut with rc 0 and no `FAILED`
  (`W/proofs-after-the-cut.txt`).
- Two repairs beyond the starter: `same_code.py` now takes a namespace
  docstring with no requires after it; and five quoted lines that the rename
  had changed from "layer" to "lane" are back as typed.

## What Sid answered, 25 November 2026, in the session's chat

These are in a session's words; his typed lines are in the record.

- **The cache line lives with the code.** A warning that matters when the
  code is changed stands next to that code; nobody reads the data file and
  the code together. Done: his line "i also don't consider it a cache" is
  back in the namespace docstring of `src-store/softland/store/read_path.clj`,
  and its entry is out of `whose-call.edn`, so it is written once.
- **The pages are cut by the same rule as the code.** A thing is written down
  only if it is written nowhere else and is not plain to derive or to find.
  Copies in many places bloat the tree, and an agent then cannot tell which
  one is the truth. Not done yet: it is item 6 of the list below, after the
  review.

## One question that his answer on the cache opens

The cut moved every sentence that says whose call a thing is out of the
docstrings and into `whose-call.edn`, a file beside the tree. The starter
gives that as his decision ("Whose call a thing is becomes data"), in a
session's words that were not checked against the record.

His reason for the cache line reaches these entries too. "Sid decided this"
on a var tells a session that changing it is his word to give; "not decided"
tells it that the value is a placeholder. Both matter at the moment the code
is changed, and both now sit in a file that the session changing the code
has no reason to open. The sections "Engineering choices" and "Words a
session made" of the store's and the door's pages, and the tables that hold
the code against each of his decisions, are the same kind of statement, so
how the pages are cut hangs on this too.

**A.** At the code, as data on the var: the entry becomes metadata of its
`def` or `ns`. One home, next to the code, and one command still lists every
open call. The separate files go.
**B.** At the code, as a short note in the docstring, as before the cut. No
data, and no list but a search.
**C.** As it is: a file beside the tree.

Position: A. It keeps what he asked for (data, so that the open calls can be
listed) and puts it where his localization reason says a warning belongs.
Its cost: metadata is code to the checker, so `same_code.py` has to pass that
one key, and the 122 entries move once more.

## One choice this session made, his to turn

The six forks split on a number whose docstring said "A number of the build,
kept and not decided": some wrote `:a-sessions` (the builder chose the
number), some `:not-decided`. This session made them one way: a sentence that
says "not decided" is `:not-decided`; one that says only "a number of the
build" or "the builder's" is `:a-sessions`. Fifteen entries changed state. If
every number of the build should read the same, say which.

## For the next session

A fresh session on Sid's machine, not a cloud one: the record
(`softland-memory`) cannot be reached from the cloud, and three of these need
it. In this order.

1. **Review the cut, to break it.** From `git diff 4369cfd f0222ec`, by tree.
   The checker holds the code; nothing but a reader holds the prose. Look
   for a why, a trap, a limit or a finding of a run that was the one home of
   something and is gone, and for an entry of `whose-call.edn` that lost a
   fact of its sentence (the sentence is in `git show 4369cfd:<file>`). The
   session that made the cut read three forks closely and sampled the other
   three; it is the builder and does not judge its own build. Two things it
   saw and left:
   - An entry often repeats a number that the code holds ("256. A number of
     the build"). That is a second telling of the code inside the data.
   - `softland.store.read-path`: the sentence "the proof is the only asker,
     and no inhabitant uses the read path yet" was dropped as false, not
     moved, since the runtime's seam asks the read path.
2. **Check the starter against the record.** `STARTER.md`, "What Sid decided,
   25 November 2026", is in a session's words. Find the lines he typed for
   the two decisions (the lane; code, checks and prose) and their ids. If he
   typed neither, say so before anything is written as his.
3. **Two entries whose state the record can settle.** Look each up, show him
   his line, and set the state from it.
   - `softland.store.gate.decision/max-kept-weight` (32 MiB) and
     `max-kept-weight-with-no-standing` (256 KiB): how heavy a thing can be
     for a gate to keep a second copy of it for the read by stamp, for a
     sender with standing and for one without. The old docstrings said both
     "Sid's call of 22 November 2026, the default of the plan's check" and "A
     number of the build, kept and not decided". The entries hold both
     sentences and are `:not-decided`. What did he say on 22 November: these
     numbers, or "go with the plan's default"?
   - `softland.auth.schema/max-actor-depth` (32): how many makers can stand
     below a root in Auth. The store's `act.clj` allows 31 delegated makers
     in a permission, one fewer. The old docstring said "Revision 7 closed
     for this build" and named nobody;
     `src/proposal/runtime-2026-11-21/spine/auth/BUILD.md` says "Closed for
     this build per Sid's instruction". The entry is `:decided` on that
     page's word alone.
4. **Write the Decisions entry**, on his word, into `src-store/README.md`.
   The draft is below; it has places for his typed lines.
5. **One attribution on the store's page.**
   `src-store/softland/store/README.md`, "The order of a lane's acts after a
   failure", begins: Sid, 13 October 2026: "I am against the Rama setting
   that orders a layer's acts after a failure. Nobody has shown whether …".
   The Decisions of 13 October, entry 7, give his typed line as "yeah i am
   against the setting as well ... so that is sorted what next?" (‹278f6238›)
   and the long sentence as the prompt's, which a session wrote at his word
   and he sent. The run-again proof's docstring says the same. So the store's
   page gives a session's sentence as his. Mend it from the Decisions entry.
6. **Cut the pages by the same rule as the code**, after the review and
   after his answer to the question above. A page keeps what it is the one
   home of: what the gate does not check, what a run found, his decisions.
   What retells the code goes: the file maps, the "read by level" text, the
   pointers to docstrings. Before a section or a page is removed, list what
   would go and show him: moving or removing a page is his word to give.
7. **Not covered by the cut.** `src-server`, `src-server-browser` and
   `src-cli` were renamed and not cut; the checks' prose was out of scope.
   Whether they are cut is his to say.

For a cloud session to run the proofs again: the "Softland" environment now
allows the four hosts that Clojure and Rama are fetched from. Clojure's tools
are not installed by it; the session that ran them installed the tools by
hand and pointed Maven's resolver at the session's proxy in
`~/.m2/settings.xml`. A fetch can answer 429 while the proxy is busy; the
next try goes through.

## Draft of the Decisions entry (not written into the README)

    ## Decisions of 25 November 2026 — the lane, and what is written beside the code

    Sid made these on 25 November 2026. [Where he made them, and his typed
    lines with their ids: from the record.]

    1. **The store's unit is the lane.** [His line.] In a session's words: a
       lane is the acts that one gate decides one after another, shown to its
       members at birth. "Layer" stays his word for a group of acts at any
       size (entry 4 of 20 and 21 November); the store does not use it for
       its unit and defines no meaning for it. What comes with it:
       - The sections above this one are left as typed. Where an entry there
         says "layer" for what one gate decides in one line, read "lane".
       - Built: the word was changed in code, checks and pages by one map,
         with no change of behaviour (9d08cf5; `rename.py` and the records of
         the proofs are in `src/proposal/lane-2026-11-25/`). Quoted lines
         that the map reached were put back as typed (62c3656).
    2. **Code is the truth of what happens, and a check is the truth of what
       must hold. Prose beside the code stays only where it is the one home
       of something: a why, a trap, a limit, what a run found. Whose call a
       thing is becomes data.** [His line.] What comes with it:
       - Built: the docstrings and comments of 46 source files of the store,
         the runtime, the wire, Auth and the command line were cut, 9,575
         lines of prose to 5,029, with no code changed; the proofs gave rc 0
         afterwards (`src/proposal/lane-2026-11-25/proofs-after-the-cut.txt`).
       - A session's, each his to turn: the file `whose-call.edn` beside each
         tree's code, its three states (`:not-decided`, `:a-sessions`,
         `:decided`), and the rule that a sentence which says "not decided"
         is `:not-decided`; the page `src-store/CLAUDE.md`.
       - Not done: the pages of these trees still describe a reading by
         folder map and docstring, and the store's and the door's pages still
         hold "Engineering choices" and "Words a session made" beside the
         data. The checks' prose is not cut.
