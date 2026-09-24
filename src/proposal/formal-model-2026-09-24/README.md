# A small executable model of the store's rules

Written 24 September 2026, before any Rama code, to find out whether the
rulings in `../frame-2026-09-15/PROGRESS.md` contradict each other. It encodes
those rulings (the "Ruled in chat" lines, the nine, the four layer kinds and
the two consequences) as plain data and pure functions. test.check generates
random histories of offers, retries, promotions, failovers and forgets. After
each history, the eight properties Sid named are checked.

No Rama, no network, no disk. PROGRESS.md was the only design source read.

## Run it

```
cd src/proposal/formal-model-2026-09-24
clojure -M:run 10000              # every configuration, 10000 histories per property (about 2 min)
clojure -M:run 10000 amended 7    # one configuration, another seed
```

It needs Clojure 1.12.0 and test.check 1.1.0, both already in `~/.m2`. The
seed is fixed (20260924), so a run repeats exactly. For each property that
fails, the output gives the smallest failing history test.check shrank to, a
trace of what happened in it, and the violation.

- `src/formal/model.clj`: the store. Two fact stores with a gate each, placement, locks, forgetting, promotion, failover, reads.
- `src/formal/properties.clj`: the eight properties, plus one extra check (x1).
- `src/formal/run.clj`: generators, the configurations, coverage counts, the runner.

## What is in the toy

- **People.** Alice and Bob.
- **Entities.** e0 to e3.
- **Fact keys.** Two: `:note`, a plain value, and `:mention`, a value that names a person, so the key's grammar makes that person a subject.
- **Layers.** Five: Alice's personal layer, her hand session, her agent session, one group (Alice and Bob), and the base.
- **Two stores, each with its own gate (ruling 1).** The stream gate serves the one-owner layers. It handles one event on one partition at a time and replays an unfinished offer from its start after a failover (at least once). The micro gate serves the shared layers. It decides a batch across partitions and commits it in one step.
- **Partitions and placement (ruling 2).** Three partitions per store. One-owner layers are placed by layer, shared layers by entity. The class on the offer is checked against the layer's class fact, and a layer can be re-classed.
- **Leaders and epochs.** Each gate has a leader with an epoch. A failover makes a new leader with the next epoch and a clock that may be off by up to 3. The deposed leader can still try to act on what it held; with fencing, its epoch is refused.
- **The lock store (ruling 7).**
  - Every value gets its own lock at write, wrapped under the person locks of the people it is about.
  - In personal and hand layers the lock is a row in the lock store; in agent, group and base layers it is kept in the record. A mark on the value overrides this.
  - Forgetting one value deletes its lock row, or, for a lock kept in the record, the operator excises it.
  - Forgetting a person destroys their person lock.
  - A value about two people survives when one of them is forgotten, unless it was marked to die with either (7b).
  - A layer setting switches lock grain to one lock per act.
- **Subjects (ruling 8).** The act's subject list is filled from the layer's owner and the key's grammar. The tool contributes none in this toy.
- **Clock promises (ruling 4).** A stamp is never behind the leader's wall clock, never backward on a partition, and always later than anything the act stood on.
- **Promotion.** A promotion from Alice's personal layer into the group or the base is a request admitted by the stream gate. The request then sends a landing offer to the micro gate. Her view says pending until the landing is in (no optimism).

## The readings

Where PROGRESS.md is silent, the model takes a reading, and each reading is an
entry in the config. `ruled` holds the literal readings. `amended` flips all
of them. Each amendment below is a question for Sid, not a ruling.

| config entry | literal reading (`ruled`) | other reading (`amended`) | what in PROGRESS.md it rests on |
|---|---|---|---|
| `:act-layer` | `:per-fact`: layer is a named part of each fact, so one act can span layers | `:per-act`: the gate refuses an act whose facts name another layer | the envelope lists "layer" among the named parts without saying fact or act |
| `:reclass-moves-gate` | `false`: a re-classed one-owner layer is placed by entity but stays on the stream gate | `true`: it moves to the micro gate | ruling 2 "a hot layer can be re-classed"; ruling 1 picks the gate by ownership |
| `:forward-name` | `:minted`: the store names a promotion's landing offer when it sends it | `:carried`: the request carries the landing's name, made by the person up front | "Names: random, made by the offerer before the gate"; the second leg's offerer is not named |
| `:lock-subjects` | `:act`: per-value locks wrap under the act's subject list | `:fact`: under the value's own subjects (the three sources applied to that fact) | ruling 7 "Per-value locks inherit the act's subject list" |
| `:p4-erasure-exempt` | `false`: P4 read literally | `true`: a read may show an erasure dated after its moment | Forget: "time travel shows 'erased on this date'" |
| `:p6-copies` | `:none`: after a forget, no copy anywhere may return the value | `:released-before`: a copy whose value was read out before the forget is exempt | the second consequence (a group member cannot forget one group value alone); Sid's exposure dimension |

Two more entries are used only by the demonstration configurations:
`:landing-checks-source` (the micro gate checks the source again while
landing) and `:fencing` (on in both `ruled` and `amended`).

## Results

Seed 20260924, 10000 histories per property per configuration, histories up
to 60 ops. "holds" means test.check found no counterexample; that is not a
proof.

| configuration | P1 one answer | P2 whole acts | P3 no forks | P4 no read past | P5 clocks | P6 forget | P7 pending | P8 not twice | x1 |
|---|---|---|---|---|---|---|---|---|---|
| ruled (literal) | fails | fails | holds | fails | holds | fails | holds | fails | fails |
| amended (all flipped) | holds | holds | holds | holds | holds | holds | holds | holds | holds |
| amended, act-layer literal | fails | fails | holds | holds | holds | holds | holds | holds | holds |
| amended, re-class keeps the stream gate | fails | fails | holds | holds | holds | holds | holds | holds | holds |
| amended, store names the landing | holds | holds | holds | holds | holds | holds | holds | fails | holds |
| amended, locks inherit act subjects | holds | holds | holds | holds | holds | fails | holds | holds | fails |
| amended, P4 literal | holds | holds | holds | fails | holds | holds | holds | holds | holds |
| amended, P6 literal | holds | holds | holds | holds | holds | fails | holds | holds | holds |
| amended, P6 exempts copies landed before | holds | holds | holds | holds | holds | fails | holds | holds | holds |
| same, plus the landing checks its source | holds | holds | holds | holds | holds | fails | holds | holds | holds |
| amended, no fencing | holds | fails | holds | holds | fails | holds | holds | fails | holds |

The amended configuration also held under seeds 1 to 4, which makes 50000
histories per property.

### The smallest failing histories

Each is the history test.check shrank to, told in words. The traces are in
the run output.

1. **An act across two layers: two answers (P1).** One op. Alice offers one
   act with a note in the group and a note in her personal layer. The stream
   gate says yes to her part. The micro gate says no to the group part, because
   the act carries one class (by layer) and the group's class is by entity.
   One name gets two answers, and half the act lands.
2. **An act across two layers: two moments (P2).** One op. Alice offers one
   act with a note in her personal layer and one in her hand session.
   Placement by layer puts them on partitions 0 and 1. The stream gate lands
   them as two events, at stamps 2 and 3, and a read between the two sees half
   the act.
3. **A re-classed layer that stays on the stream gate (P2, P1).** Three ops.
   The operator re-classes Alice's agent layer to by-entity. She offers an act
   on e0 and e1, which lands on two partitions at stamps 4 and 5. In a
   six-op variant, one half gets yes and the other gets no (stale replaces),
   because another offer replaced the same fact on the second partition first.
4. **An erasure date after the read's moment (P4).** A value is written and
   then forgotten. A read as of a moment before the forget shows "erased on"
   the forget's later stamp. This conflicts with P4 by construction: the
   forget ruling asks the past to show a later date.
5. **A forgotten person's note survives (P6).** Three ops. In her personal
   layer, Alice writes one act holding a note and a mention of Bob. Alice is
   forgotten. The note survives, because its lock was wrapped under the act's
   subject list, Alice and Bob, and Bob's lock still opens it.
6. **The mirror: a note about no one dies with Bob (x1).** Three ops. Bob
   writes one group act holding a mention of himself and a plain note. Bob is
   forgotten. The plain note is erased, because it inherited Bob as a subject.
7. **A promotion lands twice (P8).** Three ops. Alice writes a note and asks
   to promote it into the group, and her client sends the request twice under
   the same name. The stream gate answers the second send from its record, but
   sends the landing offer again. Because the store names each landing offer
   afresh, the micro gate cannot tell they are the same, and both land. A
   failover between the forward and the acknowledgement does the same.
8. **A promotion in flight outlives the forget (P6).** Four ops. Alice writes
   v2, asks to promote it, and then forgets it. The stream gate takes the
   promotion first: it reads v2 out through its lock and sends the copy (event
   4). Then it takes the forget, and the lock row is deleted (event 6). The
   copy lands in the group (event 7) and returns v2 after the forget. This
   fails the literal reading, and the reading that exempts only copies landed
   before the forget.
9. **A landing check does not close it (P6).** Nine ops. With the micro gate
   checking the source while it lands the copy: it prepares the batch and
   finds v1 readable (t7), the stream gate admits Alice's forget (t8), and the
   micro gate commits the copy (t9). A check in one gate cannot be atomic with
   a forget admitted by the other.
10. **Without the epoch (P2, P5, P8).** Five ops. Bob writes a group note, and
    the micro leader prepares a batch with it. The micro store fails over. The
    new leader prepares and commits the note at stamp 4. Then the deposed
    leader commits its old batch, and the same note lands again at stamp 2. The
    note is admitted twice, at two moments, and partition 0's stamps go back
    from 4 to 2.

### How much of the space the histories reached

Of 10000 histories under `amended`, these numbers reached each path. Other
seeds give similar counts.

| histories | path |
|---:|---|
| 4976 | a retry under the same name |
| 3403 | the stream gate met a name it had already decided |
| 719 | the micro gate met a name it had already decided |
| 687 | a stream failover replayed an unfinished offer |
| 112 | a micro failover lost a prepared batch |
| 217 | a deposed leader's write was refused by its epoch |
| 813 | a promotion landed |
| 311 | a promotion was refused because its source was erased |
| 179 | a promotion landed and its source was later forgotten |
| 1035 / 1292 | a value forget deleted a lock row / was an operator excision |
| 640 | a person forget erased a value that existed before it |
| 938 | a read as of an earlier moment showed an erased value |
| 2253 | a layer was re-classed |
| 3760 | one act landed on two partitions of the micro store |
| 584 | a per-act lock was used |
| 207 | an offer was refused for stale replaces |

## What was assumed, not modeled, or only reasoned

These readings are fixed in the model rather than turned into config entries:

- **Chains stay within a layer.** A fact replaces only a fact in its own layer with the same entity and fact key.
- **Retries are answered from the record.** The gate looks up the offer's name before any other check, so a retry gets the recorded answer.
- **Answers are durable.** An answer is recorded on the partition(s) that decided it, and it is durable before it is given, so a failover loses nothing that was answered.
- **Rama's gate behaviour.**
  - The stream gate's event on one partition is atomic, and it is at least once across a failover.
  - The micro gate's batch is atomic across partitions.
  - These are the Rama claims PROGRESS.md says the rama skill verifies before code. They are assumed here, not verified.
- **Stood-on stamps are knowable.** The gate can learn the stamp of anything an offer stood on, in either store.
- **Person locks.** They sit in a lock store outside the partitions. A person's forget is stamped after everything so far.
- **Owner subjects.** A group or the base contributes no owner subject, so a promoted copy's subjects come from the target layer and the key's grammar only.
- **Store-control facts have no lock.** These are forget facts, settings and promotion requests.
- **Not modeled.**
  - Permissions: offers name one, but it is not checked.
  - Visibility, read entries and fingerprints, and opaque values.
  - Retract, restore and replay of forgets, and excision reaching replicas.
  - Tool-declared subjects and group rules.

Found only by reasoning, not by running:

- **P1's "findable by the offer's name" was checked only as "exactly one answer exists under that name".** Where a lookup by the name alone would go is not modeled. The answer lives on the partition that decided the act, and a random name does not say which partition that is. Either the name carries its layer (the scheme tag could) or there is a name index, and the stream gate cannot write such an index in the same step as the admission.
- **A name reused for a different offer gets the first offer's answer.** Keeping a fingerprint of the offer with its answer would turn that into "no: name taken".
- **Revocation across gates.** Permissions for shared layers are facts in session layers, which live in the stream store. So the micro gate can admit an offer under a permission already revoked on the stream side.
- **Grain switch across partitions.** "Affects only values written after the switch" needs an order between the setting fact and acts on other partitions. The model switched grain only in layers placed by layer, where that order exists.
- **Promotion drops the layer owner as a subject.** Alice's note in her own layer is about Alice, but its copy in the group is not about her, so her forget does not reach it. The reverse also happens: her mention of Bob is about Alice and Bob in her layer, but only about Bob in the group. Forgetting Bob then erases the group's copy and not her original. The model produced that second trace while being built; it is not a property failure.
- **Short read entries cannot be re-run after a forget.** A deterministic tool's short read entry (pattern, moment, fingerprint) can no longer be re-run to its fingerprint once a value it matched is forgotten.
- **Writes about someone already forgotten.** The model wraps them under a destroyed lock. What the store should do is not ruled.
