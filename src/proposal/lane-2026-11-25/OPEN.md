# After the cut and its review: what is open

Written 25 November 2026 by two cloud sessions, the one that carried
`STARTER.md` through and the one that reviewed its cut, for Sid and for the
session that comes next. It assumes nothing of their conversations. Paths are
from the repository root; W is this folder.

## Where things stand

- **The cut.** The docstrings and comments of 46 source files (store, runtime,
  wire, Auth, command line) went from 9,575 prose lines to 5,029. No code
  changed. `src-store/CLAUDE.md` is the draft that was in this folder.
- **The review.** A fresh session read every changed form against 4369cfd,
  by tree: six readers, one for each list of `W/fork-lists.txt`, and nets
  over dates, citations, numbers, quoted lines and the sentences that say
  whose call a thing is. No Rama fact, no finding of a run, no number and no
  quoted line of Sid's was gone, and each sentence that said whose call a
  thing is had its entry. What it found is mended:
  - that a function does not throw, put back at each of the 32 places that
    said so. The gates call the row readers in a topology, where a throw stops
    the partition, and no check holds it for them;
  - six sentences that were the one home of something, and five that the
    shortening had made wrong (the commit names each);
  - four sentences of pages that sent a reader to a docstring for what the cut
    moved, and the attribution of 13 October on the store's page, from entry 7
    of that day's Decisions.
- **Whose call a thing is stands on its var.** The cut had moved those
  sentences into a `whose-call.edn` beside each tree. Nothing read those
  files or held them true, so each of the 122 entries is now the metadata
  `:softland/whose-call` of its namespace or var. `W/calls_to_vars.py` made
  the move, and what `W/calls.py` reads back from the code equals what the
  files held. `python3 -I W/calls.py --not-decided` lists the 83 open calls;
  39 carry a decision of Sid's. The five files are in `W/as-files/`.
- **The code is the same but for that metadata.**
  `python3 -I W/same_code.py 4369cfd <file>` prints "same code" for each of
  the 46 files; it passes the one key. Every proof ran once on the tree with
  the metadata: `W/proofs-after-the-review.txt`.

## What Sid answered, 25 November 2026, in a session's chat

These are in a session's words; his typed lines are in the record.

- **The cache line lives with the code.** A warning that matters when the
  code is changed stands next to that code; nobody reads the data file and
  the code together. Done: "i also don't consider it a cache" is in the
  namespace docstring of `src-store/softland/store/read_path.clj`.
- **The pages are cut by the same rule as the code.** A thing is written down
  only if it is written nowhere else and is not plain to derive or to find.
  Not done: item 4 below.

## Taken as the default while he was away, each his to turn

He said to take the default and tell him. These three are a session's.

1. **The calls are metadata on the var**, and not a file beside the tree or a
   note in the docstring. His reason for the cache line reaches them: "Sid
   decided this" and "not decided" matter at the moment the code is changed.
   On the var a call cannot be left behind by a rename, and the list is read
   from the code. Its cost: a call is code to Clojure, so to turn one is a
   change of a source file.
2. **Two states for three.** `:not-decided`: Sid has not decided it, and what
   stands is a session's or is kept so that the build runs. `:decided`: he
   decided it, and `:source` names where. The third state, `:a-sessions`, is
   `:not-decided` now: the six forks did not tell the two apart alike ("the
   builder's, kept and not decided" went both ways), and the sentence of each
   call still says whose the thing is.
3. **Four entries mended** (`calls_to_vars.py`, `MENDED`): `standing` in
   `read_path/lanes.clj` held as decided two sentences that its source does
   not; the read path's "room that it leaves" had lost "not in this build";
   "both numbers" named a paragraph; the source of `max-actor-depth` spoke of
   a docstring.

## For the next session

A session on Sid's machine: the record (`softland-memory`) cannot be reached
from the cloud, and the first three need it.

1. **Check the starter against the record.** `STARTER.md`, "What Sid decided,
   25 November 2026", is in a session's words. Find the lines he typed for
   the two decisions (the lane; code, checks and prose) and their ids. If he
   typed neither, say so before anything is written as his.
2. **Five calls whose state the record can settle.** Look each up, show him
   his line, and set the state from it.
   - `softland.store.gate.decision/max-kept-weight` (32 MiB) and
     `max-kept-weight-with-no-standing` (256 KiB): both say "Sid's call of 22
     November 2026" and "A number of the build, kept and not decided", and
     are `:not-decided`. What did he say that day: these numbers, or "go with
     the plan's default"?
   - `softland.store.record.tables/task` (the rows by who are a field, not a
     PState of their own) and `softland.auth.fit` rest on the same calls of
     22 November and are `:decided`. The four should come out alike.
   - `softland.auth.schema/max-actor-depth` (32). The old docstring said
     "Revision 7 closed for this build" and named nobody;
     `src/proposal/runtime-2026-11-21/spine/auth/BUILD.md` says "Closed for
     this build per Sid's instruction". It is `:decided` on that page's word
     alone. The store's `act.clj` allows 31 delegated makers in a
     permission, one fewer.
3. **Write the Decisions entry**, on his word, into `src-store/README.md`.
   The draft is below; it has places for his typed lines.
4. **Cut the pages by the same rule as the code.** A page keeps what it is
   the one home of: what the gate does not check, what a run found, his
   decisions. What retells the code goes: the file maps, the "read by level"
   text, the pointers to docstrings. Before a section or a page is removed,
   list what would go and show him: moving or removing a page is his word to
   give. Known to be out of step until then:
   - `src-store/README.md`, "Follow folder map → namespace docstring →
     function docstring … What a function takes, gives and does not handle
     is in its docstring": the cut took those out of the docstrings.
   - `src-store/CLAUDE.md` says "No map of the tree is kept beside the code",
     and the maps are still there. The same page no longer holds four things
     that the page before it did: what the page of a folder of checks says
     ("what no check reaches"), "Trace the real callers and the real
     lifetime", "No namespace of this tree requires the rig", and "What
     lasts". The page is his to turn.
   - `src-store/softland/store/README.md` ("In this build its proof is its
     only asker") and `test-store/softland/store/README.md` ("No inhabitant
     uses the read path yet"): the runtime's seam asks the read path.
5. **Not covered by the cut.** `src-server`, `src-server-browser` and
   `src-cli` were renamed and not cut; the checks' prose was out of scope.
   Whether they are cut is his to say.
6. **What the review leaves.**
   - A call often tells again what its docstring, now three lines away, or
     the code says: about 33 sentences, and a number in 25 calls. A call can
     shrink to what is its own: the state, the source, and what the other
     answer would change.
   - That a row reader does not throw, and that a caller prints as a word and
     nothing else (it holds the hash of the secret), are held by a sentence
     and by no check.
   - `:softland/whose-call` is explained on the store's page and in
     `calls.py`, and nowhere for the runtime, the wire, Auth and the command
     line. `calls.py` is in this folder; where it lives for good is open.
   - `W/as-files/`, `W/calls/` and `assemble_calls.py` are superseded and
     kept. Deleting them is his word to give.

For a cloud session to run the proofs: the "Softland" environment allows the
four hosts that Clojure and Rama are fetched from. Clojure's tools are not
installed by it: unpack `clojure-tools-<version>.tar.gz` from
download.clojure.org by hand, and point Maven's resolver at the session's
proxy in `~/.m2/settings.xml`. A fetch can answer 429 while the proxy is
busy; the next try goes through.

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
         lines of prose to about 5,100, with no code changed, and a fresh
         session reviewed the cut and mended what it found. The proofs gave
         rc 0 afterwards
         (`src/proposal/lane-2026-11-25/proofs-after-the-review.txt`).
       - A session's, each his to turn: that whose call a thing is stands on
         its namespace or var, as the metadata `:softland/whose-call`; its
         two states, `:not-decided` and `:decided`; the page
         `src-store/CLAUDE.md`.
       - Not done: the pages of these trees still describe a reading by
         folder map and docstring, and the store's and the door's pages still
         hold "Engineering choices" and "Words a session made" beside the
         calls. The checks' prose is not cut.
