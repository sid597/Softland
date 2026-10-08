# Test validation

Part: `microbatch-gate`. Stage: the build, step "validate the tests" (the Rama skill's
`phase-6-test-validate.md`). Written by the build's own session (Opus 5.5), 8 October 2026, for the
build session "softland-11".

Checked: the additions to `test-store/softland/store/pure_proof.clj` and `module_proof.clj`, against
the contract's proof lines, the plan's list "What the proof adds for this part", and the implied spec
(O2, O3 to O10 for the class microbatch, C1 to C10, X31 to X42, X107).

## The template's checks

- **Few cluster launches.** One cluster for the whole module proof, as before: four tasks, two
  workers. The base's checks run on it after the streaming gate's.
- **Synchronisation.** No ack of the microbatch depot carries an answer. The hand waits with Rama's
  test tool until the gate has processed each thing appended, then reads the answer by act-id in a
  loop with a time limit of 30 s. A row is selected from a PState only after that read gave the
  answer, which is after the batch is closed and so after every task has made it visible. One check
  looks earlier on purpose (closing). A check that expects nothing to be written waits for the
  processed count first.
- **The namespaces load.** Both proofs ran.
- **The first part's checks.** Each of the 343 pure checks and the 117 module checks is there with
  what it holds, and passes. Two things of the first part's proof changed, neither a check's
  content: the hand's `answer-of` and `locks-of` pass the routing id, which is the layer for a
  streaming act-id; and the label of one check, "an act-id of the class microbatch has no answer in
  this build", now says why it gets nothing (it is asked for with its layer, not its uuid), as the
  plan says. The base is made after the first part's check that sends its making act to the
  streaming gate.

## The contract's lines and the plan's, and the check that shows each

Each is on a `SHOWN` line. Names are the `deftest`s of `module_proof.clj`.

| Line | Where |
|---|---|
| The base made by the system, with the class microbatch | `the-base-is-made` |
| One act landing through the microbatch gate, read back by act-id | `an-act-lands-in-the-base` |
| A person acting in the base under a permission that the system gave | `a-person-acts-in-the-base` |
| A refusal through the microbatch gate with its reason | `refusals-and-repeats-in-the-base` |
| A repeat answered from the record | the same |
| Two acts of one batch that replace the same fact, one winner | `one-batch-one-order`; `a-batch-is-decided-in-one-order` (pure) |
| An act on several tasks accepted whole; one refused whole, no row on any task | `an-act-on-several-tasks` |
| The one stamp of rule 18 | the same; `the-stamps-and-the-depot-of-the-base` |
| Rule 7 in both directions; one act-id on both depots | `each-gate-tells-the-other-gates-layers`; the word before the base is made in `the-base-is-made` |
| A sealed value opened by its lock, one copy of the bytes | `an-act-lands-in-the-base`; when the two tasks are one, `the-path-of-a-lock-in-the-base` |
| An answer is not readable before its batch is closed, and is after | `an-answer-is-readable-when-its-batch-is-closed` |

Beyond those: an act into the base before it is made; two making acts in one batch; one act-id under
two things in one batch on two tasks; a different thing under a used act-id; a give and an act under
it in both orders; two acts under one lock; a request and a use of its lock in one batch; a second
value under a used lock; a request sent twice; strays of the microbatch gate; one write for a store
key in the base; a carried stamp too far ahead under a permission that does not stand; a batch of 400
acts; the base after the module is started again; no lock's bytes on the microbatch depot; each
answer that a read gave is of a closed batch.

## The implied spec's cases

- **X32, X33, X35, X36, X37, X41, X107, X31.** On the cluster and in the pure proof.
- **X34.** In the pure proof's batch (acts 25 and 26).
- **"One order, the same on every attempt" (O2; rule 6).** The pure proof runs two whole batches in
  memory over `softland.store.batch`, in 200 orders of arrival each, and holds each record to the
  record of deciding the same things one at a time by the first part's checks. That is a second
  path to the decisions that shares only the turn and the base with the code under check.
- **Who goes to the meeting task.** One pure case for each of the four rules, and for the acts that
  stay.
- **C1.** `first-entity` on ten things that are no acts; strays and a refusal of the declaration
  through the gate.
- **X40.** A run of nothing (each closing), of one act, of 400.

## Found while the proofs were written, and fixed

- The read of a request's locks gave a used lock's bytes when the lock's home was its value's task.
  The check wanted "no bytes" on every run; the code now gives that.
- The check of closing first watched an answer only. It now watches a request for a lock, so it also
  shows that `locks-of-request` gives no lock while the lock's row is already in the table (X97).
- Nothing in the gate failed a proof: the module's proof passed at its first run with the base.

## What no proof reaches

It is in `test-store/softland/store/README.md`. For this part: a second attempt of a batch of the
gate, a commit that fails after one task showed its part, and the moment between two tasks' commits
(X38, X39; the probe of the plan's "check first" list ran a second attempt of a probe topology, not of
the gate); the class of a layer and a refusal's word read between a batch and its closing (X117; the
same test as the answer's); a pass that is known to have given the task back; a rebuild (X42, rules
9 and 19); the three targets of speed and size. Which of two acts wins is not fixed by the proof on
the cluster: it holds that one wins, that the winner has the earlier stamp, and that the place is the
winner's.

Verdict of this step: pass.
