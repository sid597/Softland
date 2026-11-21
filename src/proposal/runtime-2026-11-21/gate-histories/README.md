# Generated histories through the store's own gate

[Parent: the runtime's page](../PROPOSAL.md).

**21 November 2026.** Written by a Claude session while another session was
building the store's permission rules (ending a permission, giving again,
root-only settings). It is a check of that build by a session that did not
write it.

Sid asked for this shape twice: on 12 November, "why do both of you think
the lean proof should come last?", and on 18 November, for formal
verification "so that we can say and more trust in what we are building".
The answer he was given that day was three steps: statements in plain words,
a model, and the model and the real gate run on the same generated acts.
This folder is the first and the third step, in Clojure. It holds no Lean.

## What it does

It makes random histories of one layer: gives, ends, gives after an end,
plain facts, settings, the same thing sent twice. Some makers name the right
permission; some name a stale one, one whose "see" they raised to "see and
act", or another holder's. Each thing is decided twice:

- by the **gate**: the store's own pure functions
  (`softland.store.gate.decision`, `softland.store.gate.chain`,
  `softland.store.door.declaration`), called in the order that the walk
  names, against a record in memory ([walk.clj](src/gate_histories/walk.clj));
- by the **statements**: the plainest code that says them, which keeps who
  was given what by whom and which gives were ended, and reads no row and no
  stamp ([reference.clj](src/gate_histories/reference.clj)).

When the two differ, test.check cuts the history down to the smallest that
still differs and the run prints it step by step.

## The statements

Ten about permissions, each in the docstring of
[reference.clj](src/gate_histories/reference.clj), S1 to S10. In short: an
act is taken only under a permission that stands; after an end the holder
can be given a permission again; those beneath do not come back by
themselves; only the giver, anyone above, or the holder ends a permission;
the first permission does not end; a holder has one permission in a layer;
a give is straight beneath the giver's own; only the first holder writes
settings; a holder who may only see cannot act; an act is taken whole or
refused whole.

Five that every history must keep ([run.clj](src/gate_histories/run.clj)):

| Word | What must hold |
|---|---|
| agree | The gate takes a thing exactly when the statements take it. |
| stamps | The decisions of the layer have the stamps 0, 1, 2 and on. |
| kept | A decision changes no row but its own: what was taken stays, and a refusal writes no fact. |
| once | A thing that is sent again under its act-id decides nothing. |
| run-again | The same things, decided again from nothing, give the same record. |

## Run it

From this folder. It needs Clojure and test.check, both in `~/.m2`.

```
clojure -M:run                  # 2000 histories, a seed of its own
clojure -M:run 20000 7          # so many histories, this seed
clojure -M:run mutants 3000 1   # the gate with one rule broken at a time
```

A run prints how many decisions it made, how often each word of a refusal
came up, and how deep the permissions were. It exits 1 when a statement
broke, with the smallest history that breaks it.

`mutants` is the check of the check. It puts a broken function in place of
one rule of the gate, for the run only, and the run must find a history that
breaks a statement. The store's files are not changed.

## What was run, and what it showed

On 21 November 2026, against the store's files as they were in the working
tree at 15:45 IST, not committed: `gate/chain.clj` (sha256 `3602a8af8313`),
`gate/decision.clj` (`b886ad8a8492`), `record/rows.clj` (`e8c701a320b8`).

- **40,000 histories, seeds 20261121 and 7: every statement held.** About
  1.55 million decisions, 317,000 of them taken; 24,000 ends and 10,600 gives
  after an end. Permissions were up to five deep.
- **The case that the gate's rule of stamps exists for came up 517 times**
  and was refused each time: an act under a permission whose ids were all in
  place, though one above it had ended and been given again.
- **Six mutants, six caught,** each within 700 histories. The smallest
  history that catches a gate which reads ids alone is four steps: Asha gives
  Cal; Cal gives Dee and ends her own; Asha gives Cal again; Dee acts. The
  mutant takes Dee's act. The store's gate refuses it.

## Sid's own record, through the same gate

[record.clj](src/gate_histories/record.clj) sends a file of things that were
said through the gate, one act for each, with the same walk. It was run on
21 November on the whole record of the project: every line that Sid typed
since March and every text that a session wrote, 38,545 things, 88 MB of
text. The file is made outside the repository and is not in it.

```
clojure -M:record /path/to/record.edn
```

- **The gate took every one:** 38,545 acts, 193,582 rows of facts, the stamps
  0 to 38,546 with no gap, in 21 seconds on one thread. That is the
  declaration's check, the digest and the gate's decision, in memory. It
  says nothing of Rama.
- **Three things an importer has to do,** each found by a refusal:
  1. An id of the store is a keyword or a UUID. A string is refused, and so
     is a keyword that does not read back: the record's ids begin with a
     digit two times in three. The importer gives each id a form that the
     store takes.
  2. One value holds 65,536 bytes at most. 69 things said are longer, the
     longest 775,000 bytes. They are cut into parts, each a fact, in the one
     act; the longest is an act of 31 facts. Where to cut is the question
     Sid raised on 9 October: a page or a block.
  3. Who said a thing is a fact, and the who of the act is the importer,
     as Sid decided for the importer and Alice.
- **The form that was sent is a bet,** made to get these answers: one layer,
  one act for one thing said, five facts. It set `:claimed-when` to the time
  when the thing was said. It did not set `:because-of`, which is the part
  of an act that names what it answers, and the record knows that for each
  line.

## What it does not reach

- **The module.** The walk here was written from the description in
  `softland.store.module.walk` and from the docstrings of the pure
  functions, not from the Rama code. It shows that the rules decide rightly
  when a gate has the rows that those docstrings name. That the module hands
  a gate those rows is the module's proof, on a cluster.
- **A read.** Whether an asker may see a layer is the read path's, which was
  being changed when this was written. The same reference can judge it: a
  holder may see while her grant stands.
- **The other gate.** Every history is one streaming layer. The base, the
  microbatch gate and an act for a layer of the other gate are not in it.
- **A fact that names something to replace,** `:based-on`, and the bounds on
  a value and an act.
- **Every history.** A run samples. A statement about all histories needs a
  proof, which is what Lean would add for the few statements worth it.

## Where it could go

Into `test-store/`, as a proof beside the store's others, if the session
that owns the store wants it there. It was kept out so that two sessions did
not write in one folder.
