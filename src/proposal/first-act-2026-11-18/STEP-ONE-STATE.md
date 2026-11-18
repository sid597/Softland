# Step one of the store's first who: state of the build, 18 November 2026

This page passes the build of step one on to a fresh session. The building
session started from [HANDOFF.md](HANDOFF.md) in this folder. It is working
material and no decision page. As in the handoff, three kinds of line are kept
apart: **Sid's words**, **facts** (with how each was checked), and the
**builder's choices**, which Sid has not ruled on and can turn.

The code, its checks and its pages are committed on `main` in one commit, and
this page in a second. Nothing is pushed. No fresh session has reviewed the
build yet.

## What Sid said in the building session

- "Note that for code that you or any subagent you give to write should also
  write the hierarchical docs."
- "i prefer to do the testing in subagent and optimise tests for throughput
  instead of per 2 line fix and run a test."
- "if its only test running then we should start a new session this is too
  big session as of now."
- "you do the code part for the work that we require". The session read this
  as: finish the code and its pages here, and leave the running of tests to a
  subagent and the review to a new session.
- On the form of a who he asked questions and gave no ruling. The session told
  him twice that it was building "B" (below) and that he can turn it.

## What is built

| File | What changed |
|---|---|
| `src-store/softland/store/act.clj` | A second edition of the list of parts, `:signed-act`, version 2: the parts of an act, a `:who` that is a signing who, and `:signature`. `shape-of` chooses the edition. One new rule for unsigned acts. |
| `src-store/softland/store/door/signature.clj` (new) | What is signed, the making of a signature, and the test that one fits its who. Pure; the JVM's Ed25519. |
| `src-store/softland/store/door/declaration.clj` | `:arrived-signed-act`; the check's third step, the signature; one new rule for a signed give; `what-is-signed`. |
| `src-store/softland/store/gate/first_act.clj` | A signed making act names no `system`: its who is its holder. `signed-making-act` builds one. |
| `src-store/softland/store/setup.clj` (new) | `new-pair`, `send-signed!`, `make-the-base!`, `keep-first-act!`, `kept-first-act`, `send-again!`. It requires the door. |
| `test-store/softland/store/act_proof.clj`, `pure_proof.clj` | The second writings of the declarations, and the new cases. |
| `test-store/softland/store/setup_proof.clj` (new) | The setup on two in-process clusters, through the door alone. |
| `src-store/softland/store/door.clj` | Two paragraphs of its docstring, on the sender. No code. |
| Pages | `src-store/MODEL.md`; the `README.md` of `src-store/`, `softland/`, `store/`, `door/`, `gate/`; the three `README.md` of `test-store/`. No "Decisions of …" section is changed. |

The gate's walk (`module/walk.clj`) is not touched: the signature is checked
inside the declaration's `check`, which the walk already calls.

## The builder's choices, each Sid's to turn

1. **The who of a signed act is its public half** ("B"): the id
   `:who/ed25519.<64 hex>`. The handoff had a name with the public half stored
   as a third fact under a new store key ("A"). With B there is no third fact,
   no new store key and no extra row read, and a signature is held against the
   id in the act itself.
2. **An act's edition is chosen by its version and its who together.** A thing
   is a signed act when it says version 2 and names a who in the namespace
   `who`; anything else goes the way that every thing went before. See "The
   fixture" below for what forced this.
3. **One change to version 1:** an unsigned act that names a who in the
   namespace `who` is refused `:who-needs-a-signature`. Without it an unsigned
   act could name a signing who and be taken on the permission's row alone.
4. **A signed act gives permissions to signing whos only**
   (`:gives-to-a-who-with-no-public-half`). With it, in a layer that a signed
   act made, each act that a gate accepts is signed.
5. **What is signed:** a fixed line of purpose, and the SHA-256 of the thing's
   one text, taken of the thing as it arrives without its signature and its
   time. The act-id is inside it.
6. **The private half is kept in the memory of the process and nowhere else.**
   The handoff had it written to a file. No file holds a private half.
7. **Words:** "signing who", "public half", "private half", "pair", "keeper";
   the five refusal words; the namespaces `softland.store.door.signature` and
   `softland.store.setup`.

## The fixture: what in the handoff did not hold

The handoff says that acts of version 1 keep their meaning, so the kept
fixture still gives its tables. The fixture
(`test-store/softland/store/run_again_fixture/acts.edn`) holds two acts that
say `:version 2`, each sent on purpose as "a version that the code does not
know" and refused `:unknown-version`.

- Fact, from a run: with the edition chosen by the version alone, the
  run-again proof gave 42 passes and 4 failures. Exactly two rows differed, in
  one field: the refusal word of those two acts.
- The session then made choice 2 above. Fact, from a second run: the run-again
  proof gives 46 passes, no failure and no row that differs. The fixture's two
  files and `run_again_proof.clj` are unchanged.

What stays open behind it, for a store that is kept: an act that was sent
before the code knew its edition is decided anew when the tables are made
again by code that does know it. Choice 2 narrows this to things that name a
who in the namespace `who`. It does not close it.

## What the checks showed

Each is one run on the tree as committed, unless it says otherwise.

| Proof | Result |
|---|---|
| `clojure -M:store -m softland.store.act-proof` | 205 pass, 0 fail (166 before the build) |
| `clojure -M:store -m softland.store.pure-proof` | 766 pass, 0 fail (685 before) |
| `clojure -M:store-module -m softland.store.setup-proof` | 23 pass, 0 fail |
| `clojure -M:store-module -m softland.store.run-again-proof` | 46 pass, 0 fail, no row that differs from the fixture |
| `clojure -M:store -m softland.store.read-path-pure-proof` | 213 pass, 0 fail |
| `clojure -M:store-module -m softland.store.door-proof` | 277 pass, 0 fail |
| `clojure -M:store-module -m softland.store.module-proof` | 443 pass, 0 fail |
| `clojure -M:store-module -m softland.store.read-path-proof` | 553 pass, 0 fail |

The last five ran before two paragraphs of the docstring of `door.clj` were
changed, and before the pages. No code changed after them. A subagent ran the
last four; the session read the count line and the exit line of each from its
output file.

The new tests of the pure proof are `the-signature`,
`the-signed-act-in-the-check` and `the-first-who-of-a-store`; the last decides
lines of signed and unsigned things through the walk in memory. The setup's
proof gave on a cluster, through the door, the words that those lines give.

A rough number, one machine, one JVM, 2,000 runs, no benchmark: the check of a
signed first act took about 0.44 ms, and of an unsigned one about 0.016 ms. It
runs on the layer's task.

## What is left

1. A fresh session reviews the build from the diff: its job is to break it.
2. What is open below, when the work reaches it.

## What no check reaches

- A keeper outside the process, and a private half that lasts.
- A restart on the same data. No store of this build is kept.
- A public half that is a weak point of the curve: `signature.clj` says what
  such a who is.
- How long a signed act takes on a cluster under load.

## Open, and Sid's

- Choice 1, the form of a who.
- Before a store is kept: how a new edition begins to count in a store that
  already holds acts; and whether version 1, which still makes unsigned
  layers, goes into such a store at all.
- Which keeper holds the first who's private half for a store that is kept.
