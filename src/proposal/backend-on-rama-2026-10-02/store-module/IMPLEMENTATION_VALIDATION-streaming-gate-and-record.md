# Implementation validation

Part: `streaming-gate-and-record`. Stage: the build, step "validate the implementation" (the Rama
skill's `phase-4-impl-validate.md`). Written by the build's own session (Opus 5.5), 8 October 2026,
for the build session "softland-11". It read no code of the rig, the server or Inland.

Checked: the code under `src-store/softland/store/` against `PLAN-streaming-gate-and-record.md`, the
contract with its rules 1 to 21, and the implied spec. Three kinds of statement: *traced* (I read the
code line by line for the case), *run* (a proof or a probe printed it), *not verified*.

## The template's checks

- **Redundant conditionals.** None. The five branches of the write step write to four PStates, and
  one of them makes a lock. *Traced*, `streaming_gate.clj`, step 7.
- **Consecutive keypath.** None; `(keypath :strays *stray)` is one navigator.
- **Select, compute, transform.** One place: the task's last stamp is read, the stamp is computed,
  and the stamp is written. The stamp is needed for the other rows of the decision, so the read
  cannot fold into the write. Every other write is a whole row at a key, with no read.
- **Unnecessary `nil->val`.** None.
- **`:allow-yield?`.** The one range read is of a request's locks, 256 rows at most. It has no
  yield, as the plan says: in the gate a yield would let another act of the layer in between this
  act's reads and its writes; in the query the read is about 1.3 ms.
- **Collections with no limit.** No row holds a collection. The outer map of the strays has five
  keys, the words of `declaration/stray-reasons`, and only `stray-reason` gives a key. *Traced.*
- **Idempotency of the stream topology.** Each write of a decision is behind check (a), and the
  answer's row commits with the rest. The stray's write is an add to a set. No id is made in the
  topology: a lock's id follows from its request's act-id. No depot append. *Traced*; the repeats of
  the proof are *run* (a repeat leaves the task's last stamp and each lock's bytes as they were).
- **Partial failure.** One event, one task, no partitioner.
- **One append for each client operation.** Yes; the door is the proof's hand.
- **Caches.** None. The last stamp is read from the PState at each decision.
- **Built-ins written again.** None.

## Against the plan

The depot, the topology, the four PStates, the three queries, the names, the source's options, the
order of the checks and each word are the plan's. The counts hold, *traced*: 4 reads and 4 writes for
one accepted act with one sealed value; 1 and 0 for a repeat; 3 and n + 3 for a request; 4 and 4 for
a making act; 2 and 2 for a refusal by the declaration.

Where the code is not the plan's words:

1. **The rows of the places are read one by one in a loop, not "in one select".** The count is the
   plan's, one seek for each place. Rama's references give no navigator for several top-level keys
   at a known cost; a `keypath` read is one seek by the skill's model.
2. **`:lock-request-sits-on-its-act` holds for a fact with no value too.** The plan is silent. It
   keeps "its place is new by its form" true for each fact under the key.
3. **A keyword with no namespace that does not read back** is written `#keyword nil "name"`, not
   with two strings: `""` would give it the text of a keyword whose namespace is the empty string.
4. **Inside one step, the facts are examined in the act's order**, each with its tests in the
   plan's order. The plan gives the order of the words and not this.
5. **The stamp of a thing that the declaration refused** takes no carried stamp: the thing is no
   act.
6. **The printed text of an id** and the two tests of what a read was given are in `declaration`;
   the keys to read are in `record`. The plan names no home for them.
7. **`act.clj`'s namespace docstring**: "Callers" and "Neither the door nor a gate is built" are
   corrected, since the change made them untrue. The new function is `as-shape`.
8. **`src-store/README.md`**: beside the classpath paragraph, its first paragraph and its table of
   questions say that the tree now holds the module. Its decisions sections are untouched.

No divergence changes a key, a row, a read count or a recorded word.

## What the plan marked "not verified", as a run found it

| Item | Found |
|---|---|
| A byte array as the class of a schema position | *Run*: Rama takes `(Class/forName "[B")`. Rows with bytes are written and read back, with two workers. |
| A record read back from the depot's log has been through serialisation | *Run*: it is another object; a subvector comes back a vector, a list a list, bytes the same bytes; its digest is the digest taken before the append. |
| The largest record that the depot takes | *Run*, the in-process cluster only: a record of 17 MB was taken. No limit was found. Production: *not verified*. |
| RocksDB shares the fronts of neighbouring keys | *Not verified*. The in-process cluster does not show it. The aim of 110 bytes of lock is not measured. |
| A query topology reads a private PState | *Run*: it does, on the in-process cluster. The locks PState is left as the plan has it, not private. One option would close the second way to a lock's bytes; the build session's to rule. |
| What Rama's client does with a first argument that is nil | *Run*: the query came back with nil. The hand tests what it passes all the same. |
| A module whose topologies are added by functions of other namespaces | *Run*: it works; the depot's and the PStates' symbols resolve there. |

## Against the spec

*Traced*, for each function that an event calls: nothing throws on what arrived (IS C1). The pure
proof gives each of them things that are no acts. Each row that is written is made by `record.clj`
with the declared classes. A refusal writes the answer and the last stamp only (C rule 15). The
event has no partitioner and no yield (IS C3).

## Found and fixed

- An unused private helper in `text.clj`, found by clj-kondo, is removed. That was its one finding
  in the pure code and the proofs. The linter cannot read Rama's dataflow without Rama's own
  configuration, so `streaming_gate.clj` and `door_reads.clj` were checked by loading them, with no
  warning and no reflection, and by the proof.
- Nothing in the gate, the reads or the pure checks failed a proof. The module's proof passed at its
  first run on the cluster; its later changes added cases.

Verdict of this step: pass, with the eight points above for the build session to read.
