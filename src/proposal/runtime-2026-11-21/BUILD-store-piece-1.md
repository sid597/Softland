# The store's line, piece 1: what was built, and what was run

[The runtime's page](PROPOSAL.md), "The store's line, to start from".

**21 November 2026.** Written by the Claude session that built it. A session
does not judge its own build: this page says what was built and what a run
printed, for a session that reviews it from the diff.

## What Sid asked for, and what was built

His decisions, on the runtime's page, entry 6: a permission is ended by its
giver or by anyone above, and what was given beneath it ends with it; after an
end the holder can be given a permission again, and those beneath do not come
back by themselves; only the layer's root holder writes its settings.

Built in `src-store`, with their proofs in `test-store`:

- **An end.** A permission fact with no value, at a place that holds a
  permission. It is one row, with a stamp in the layer's order.
- **What is beneath ends with it.** A gate reads the row of each permission
  above an act's own, at each decision. The read path reads the same rows by
  the same rule, so a live read leaves a layer when the asker's permission
  ends, or one above it.
- **A give after an end.** A give is taken at a place that is empty, that holds
  an end, or whose permission no longer stands.
- **Root-only settings.** The holder of the layer's first permission alone
  writes who may see it, what it builds on and the keys it marks.

The rule of when a permission stands is in one file,
`src-store/softland/store/gate/chain.clj`, which the gates and the read path
both call.

## A session's choices, each his to turn

He asked why the work was not in the shape of implementation when a sheet of
calls was put to him. So these were taken as defaults and built, and each is
marked in the code as a session's:

| Choice | Where it is made | The other way |
|---|---|---|
| A second give is told from the first by the stamp of its row; the id does not change | `chain.clj`, `stands?` | One more part of the id that names the give |
| What is beneath an end is read at each decision, and written nowhere | `chain.clj`; `decision.clj`, `chain-needed` | An end writes an end on each place beneath, with a table of who gave to whom |
| A holder ends her own permission | `decision.clj`, `end-fault` | Only the giver and those above |
| The layer's first permission does not end | the same | A layer that can be closed, or handed to another holder |
| The root holder is the holder of the layer's first permission | `decision.clj`, `who-may-write-fault` | An owner named by a fact |
| The word is "end", as the store's pages had it | the refusal words | "revoke", which he said in passing; a rename |
| How deep a permission can be stays 32 | `act.clj`, `nesting` | A smaller number: each level is one read at each act |

## What it costs, read from the code and not measured

- An act under a given permission reads one row more for each permission above
  its own. An act of a layer's first holder reads what it read before.
- A live read in a layer that is open to its members holds one push more for
  each permission above the asker's.
- The greatest call of the walk is now 8,228 reads, from 261: an act of 256
  gives over 256 places that each hold a permission 32 deep. A refused act
  leaves the record as it was, so a holder who may act can send it again. The
  bound comes down with the depth of a permission. No run measured it.

## What was run

All seven proofs of the store, on the final code, one after the other behind
the machine's cluster lock, on 21 November 2026:

| Proof | Result |
|---|---|
| `act-proof` | 166 checks, none failed |
| `pure-proof` | 711, none failed |
| `module-proof`, in-process cluster | 463, none failed |
| `door-proof`, in-process cluster | 277, none failed |
| `read-path-pure-proof` | 218, none failed |
| `read-path-proof`, in-process cluster | 573, none failed |
| `run-again-proof`, in-process cluster | 47, none failed |

What they hold of this piece, each by its own sentences in a run:

- the gate's rules on rows that the proof builds (`pure-proof`);
- an end, the refusals beneath it, the second give and a holder's own end,
  through both gates on a cluster, in a streaming layer with a chain of four
  and in the base in one batch (`module-proof`, the groups `a-permission-ends`
  and `a-permission-ends-in-the-base`);
- a live read that leaves the layer when the permission above its holder ends,
  is given nothing while it is out, and takes the layer back when its holder
  is given a permission again: on a cluster (`read-path-proof`, the group
  `a-permission-ends-under-a-live-read`), and in every order of the callbacks
  for 342 lists of writes (`read-path-pure-proof`);
- a line with an end and a second give in each gate comes out the same when
  the gates are run again over the depots, and from kept acts on 4 tasks and
  on 8 (`run-again-proof`).

Another session's check, which this session ran and did not write
(`gate-histories/`): 20,000 generated histories through the gate's pure code
agree with ten statements of the rules in plain code.

**The fixture was made again**, on purpose, for the three rules above. The
store with the new rules gave the third making's tables from the third
making's records, since those records held no end, no second give and no
setting by a holder of a given permission; it was made again so that the kept
acts hold an end. Its page says the rest
(`test-store/softland/store/run_again_fixture/README.md`).

## What no run reached

- A cluster of several machines, and a killed process: each cluster was Rama's
  in-process one.
- An end made before the module is started again, held after it; an end in a
  batch or an event that fails and runs again.
- An end under a live read of the base.
- What an act costs for each permission above its own, and the walk's
  greatest call.

`test-store/softland/store/README.md` says the same, proof by proof.

## Not done, and waiting on Sid

- **The store's "Decisions of ..." sections were not changed.** They are his
  pages. His decisions of 20 and 21 November are on the runtime's page and
  cited from the code. Two entries of 11 and 12 October now say what the code
  no longer does: that who writes a setting is not said, and that "see" stays
  "see" because nothing ends a permission (`src-store/README.md`, entry 1 of
  that day). They need a line that says so, and a section for 20 and 21
  November, on his word.
- **Piece 2 cannot be built as the runtime's page says.** The page keeps a
  holder at one permission in a layer. A holder who may always see, and gets a
  yes for one run, holds two. The position put to him: a permission sits on
  what it covers, the holder, a run or an act, and says which in its id; a yes
  for one act is held to the act's digest. Both are forms of a stored record,
  and his.
- **Piece 3** needs nothing from him: a layer's acts and refusals, read by
  stamp. It is a new table of the module, built by the Rama skill's phases.

## Beyond the ask

- `softland.store.record.rows` gained `held-permission` and `ended?`.
- The read path's bound on rounds went from 8 to 9, for the one rank more.
- The pure proof's hand-built base record gives the system's own permission
  the stamp 0, where every row had one stamp: a given permission's row must be
  younger than its giver's.
- The read path's pure proof gained a gate for a give and an end in its world.
- Two counts of open pushes in `read-path-proof` are one higher: a reader
  holds the row of her giver.
- The store's pages were brought up to the code: `MODEL.md`, the maps of
  `softland/store/`, `gate/`, `read_path/` and of the proofs.
