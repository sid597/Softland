# Implicit Spec

<!-- Phase 0. Fill in before starting Phase 1 (Plan). After completing this, fill in PLAN.md. -->

Before designing any Rama-specific implementation, write out the implicit assumptions and expectations that a senior engineer would bring to this system based on the requirements and the domain.

For every operation the system exposes, fill in:
- **Latency**: Are the effects of this operation needed in single-digit milliseconds, or is hundreds of milliseconds / seconds acceptable?
- **Throughput**: What drives the volume of this operation and how does it scale with usage?
- **Consistency/correctness invariants**: What must always be true?
- **Data growth and scale**: Which collections are unbounded? What access patterns dominate? What needs efficient range access vs. point lookups?
- **Concurrency behavior**: What happens under concurrent writes to the same entity? What ordering guarantees matter?
- **Edge cases**: Also consider: empty inputs, boundary values, large ranges, missing keys, duplicate operations.
- **Entity state × write matrix**: For each entity that write operations target, list every state that entity can be in (e.g., "does not exist", "active", "completed"). Then for each write operation × entity state, write:

  ```
  Entity state x Write operation
    - related-read-op-1: what would it return after this and why
    - related-read-op-2: what would it return after this and why
  ```

  List every related read for EVERY row — not just the obvious or interesting ones. Without listing each read explicitly, it is easy to miss writes that lead to undesired application behavior that violates common sense. Think through how that write affects all related reads and if any inconsistency is created. ALWAYS assume all read operations can be called in every entity state — users query data in all states. Do NOT skip states because the spec is silent — the implicit spec exists to fill in what the protocol leaves unstated.

Ground these in the specific domain — don't list generic concerns. Every point must be tied to a concrete operation or behavior.

**The spec records requirements, not designs.** Do NOT prescribe implementation decisions — state representations, storage choices, data structures, or mechanisms. Anything written here is treated as a requirement by every later phase and becomes exempt from validation checks it would otherwise fail. State WHAT must be true (latency bounds, invariants, scale facts); leave HOW to the plan.

## Operations

Module: the store's module, `softland.store.module`. Stage: Phase 0 of the Rama skill.
Written by: a fresh session (Opus 5.5), 8 October 2026, for the build session "softland-11".

### How to read this paper

**Sources.** Four files, and no other, give the requirements:

- "the contract": `src/proposal/backend-on-rama-2026-10-02/BUILD-store-module.md`. Cited as `C`, with its
  item or rule: `C item 3(d)`, `C rule 6`, `C:118` for a line.
- "the README": `src-store/README.md`, Sid's decisions of 6, 7 and 8 October 2026. Cited as `R 6-Oct 4`,
  `R 7-Oct 7`, `R 8-Oct 3`, or `R:183` for a line. Where the contract and the README differ, the README
  wins; each difference is listed in the last section.
- `src-store/softland/store/act.clj`: the one form and its check. Cited as `act.clj`.
- `src-store/CLAUDE.md`: the words of the tree.

No code of the rig, the server or Inland was read. The four findings that the contract names are used as
findings, cited as `finding`.

**Three kinds of statement.** Each statement that later stages may rest on says which kind it is.

- *given*: the contract, the README or `act.clj` says it. The citation follows.
- *derived*: the sources do not say it in these words, but it follows from what they say. The reason
  follows. A later stage may challenge the reasoning.
- **OPEN POINT n**: the sources are silent, and the build must still do something. The ways it could go
  are in the section "Open points". This paper picks none of them. An open point is not a requirement.

**Numbers.** `O` is an operation, `C1`…`C10` a rule that holds for every operation, `E` an entity, `T` a
state transition, `X` an edge case, `OP` an open point.

**Words.** An *act* is what a maker sends. A *fact* is one statement of an act: entity, key, value. A
*key* is a fact's key. A *lock* is an encryption key. A *place* is one entity and one key in one layer; it
holds one current fact at most. A *thing* is whatever arrives on a depot, an act or not. *The door* is
played by the proof's hand in this build. A *store key* is one of `:class`, `:permission`,
`:lock-request` (C item 2). A *plain key* is any other key. A *value fact* is a fact under a plain key
that has a value. "Place" and "thing" and "plain key" and "value fact" are this paper's words, for this
paper only; the build does not have to keep them.

**What is not here.** No PState, no depot shape and no topology is designed here. Where the contract
itself fixes a mechanism (two depots, a stream topology and a microbatch topology, a query topology for
the answer, what each depot partitions by), this paper repeats it as *given* and adds nothing to it.

**Production.** The proof runs on Rama's in-process cluster. Each requirement here is for production:
a retry by Rama, a worker restart and a node failure are in scope (SKILL.md).

### The operations, in short

| | Operation | Kind |
|---|---|---|
| O1 | An act is sent to the streaming depot; the streaming gate decides it | write |
| O2 | An act is sent to the microbatch depot; the microbatch gate decides it | write |
| O3 | Make a layer (the first act of a layer) | write, a kind of act |
| O4 | Write value facts and retracts | write, a kind of act |
| O5 | Give a permission | write, a kind of act |
| O6 | Ask for locks | write, a kind of act |
| O7 | Send under an act-id that was sent before (a retry by Rama, a repeat, a different act) | write |
| O8 | Read the answer by act-id | read |
| O9 | Read a lock's bytes | read |
| O10 | Read a layer's class | read |

O1 and O2 are the two ways in. O3 to O6 are what an act can do; each of them goes through O1 or O2,
whichever the layer's class says. One act can do several of them at once (X89, X101); a gate accepts the
whole act or refuses the whole act (act.clj).

Not operations of this build, by decision (C "Not in this build"): a read of a fact, `resolve`, a bell,
the feed, promotion, re-class, forget's destroy step, sign-in, the ending of a permission, a grammar
check, the door proper. No later stage adds one of them.

### What the door reads from the module, and when

The proof's hand plays the door (C "The proof"). In the order of one act:

1. **Before it makes an act-id: the class of the layer (O10).** An act-id is `[layer class uuid]`
   (R 6-Oct 5), so the door needs the class first. *Derived:* for a making act there is no class to read
   yet, because the layer is not made. The door then takes the class from F4 C: microbatch for `:base`,
   streaming for any other layer.
2. **Before it seals a value: the bytes of a lock (O9).** The lock comes from an earlier request (O6).
   The door reads the answer of that request by act-id first (O8); a lock can be read only when that
   answer is visible. So one sealed value needs: a request sent, its answer read, the lock read. The
   request is sent ahead of need so that the sealing does not wait (C item 7).
3. **For `:replaces`: nothing from the module.** A fact id is `[act-id index]` (act.clj). The door knows
   the fact ids of the acts that it sent itself. *Derived limit of this build:* no read gives the current
   fact of a place, because reads are not built. So a sender cannot learn the fact id of a fact that
   another sender wrote. The proof replaces its own facts.
4. **After the append: the answer by act-id (O8).** For a streaming act: after the ack, or after the ack
   fails (R 7-Oct 6). For a microbatch act: there is no ack; the door reads until an answer is there.
5. **When there is no answer and the door is in doubt: nothing more to read.** It sends the same thing
   again under the same act-id (O7).

### Rules that hold for every operation (C1–C10)

**C1. No exception on what arrives.** *Given:* each check is "a recorded refusal word, never an
exception" (C item 3); an exception in a topology kills the worker (finding). *Derived:* this reaches
every step that touches what arrived, not only the checks: the depot's derivation of a partition from
the thing, the declaration's check, the digest, each read of the record that uses a part of the thing,
the stamp, the making of a lock. *Derived, for production:* Rama retries a thing that made a gate fail,
and it fails again. So one such thing stops its partition for every act behind it. "The proof does not
send it" is no defence: anything can append to a depot without the door (R 7-Oct 8).

**C2. One act-id, one answer, for ever.** *Given:* a gate reads its own record by act-id before it
decides, so a repeat is answered, not decided twice (R 7-Oct 7). *Derived:* once the record holds an
answer under an act-id, nothing changes it: not a repeat, not a retry by Rama, not a different act under
the same act-id. An answer is never deleted, because a repeat can come at any later time.

**C3. Whole or nothing, and visible together.** *Given:* a gate accepts the whole act or refuses the
whole act (act.clj). *Derived:* on accept, each fact of the act becomes the current fact of its place,
each lock that the act names becomes the lock of its value, each permission that the act gives stands,
and the answer says accepted. A reader sees all of these or none of them. No reader sees an accepted
answer before a fact of that act, or a fact before the answer. On refusal the record changes in one
place only: the answer (and OPEN POINT 6 for a lock that the act named).

**C4. The order of the checks is the order of the reasons.** *Given:* C item 3 gives the order (a) to
(g). *Derived:* when a thing has two faults, the check that comes first gives the recorded reason, as
`act.clj` does inside the form. *Not placed by the contract:* the two lock checks of C item 7 (a lock
that the store did not make for this layer; a lock that already seals a value), and the two refusals
"not decided" (a retract of a permission; a change of class). The plan places them and says where. That
is engineering, not a decision.

**C5. A repeat does not depend on the record of the moment.** *Derived from C2:* the answer to a repeat
is the recorded answer, even when the record has moved since. A repeat of an accepted replace is still
"accepted", although its `:replaces` would now be stale. A repeat of an accepted act with a sealed value
is still "accepted", although its lock now seals a value (its own). This is why check (a) is first.

**C6. The digest.** *Given:* each answer keeps a digest of its act; the gate tells a repeat from a
different act by it (C rule 1). *Derived:*
- The digest is of the thing as it arrived, with its sealed bytes. The gate cannot take it of the plain
  values: it does not hold them.
- The same thing gives the same digest in every process, on every node, after every restart.
- Two different things do not give the same digest in practice. A short hash is not enough: with a
  collision, a different act reads the first act's answer as its own, which is what rule 1 forbids.
- The digest is defined for every thing that has a usable act-id, also for a thing that is not an act.
  Such a thing gets a recorded refusal under its act-id, and a repeat of it must be told from a different
  thing.
- *Consequence for the door:* a repeat is the same thing, byte for byte. A door that seals the value
  again (new random bytes) and sends under the same act-id sends a different act (X106).

**C7. The stamp.** *Given:* an integer the gate assigns; strictly increasing on its task; later than
each stamp that the act carries; below 2^62; it holds the clock time and a counter (C item 6;
R 8-Oct 4). The answer holds a stamp when the act is accepted and when it is refused (C item 5). Each
current fact holds the stamp (C item 5). *Derived:*
- One decision, one stamp. The facts of an accepted act hold the stamp of the act's answer.
- "The stamps that the act carries" are the values of `:based-on` (act.clj: the shape `:carried-stamp`
  is used there and nowhere else). `:claimed-when` is the maker's claim, any integer; it is not a carried
  stamp (X24).
- "Strictly increasing on its task" holds through a worker restart, through a move of the task's leader
  to a node whose clock is behind, through a clock that steps back, and through more decisions in one
  clock tick than the counter can count. So the last stamp of a task is durable, or it can be found again
  from durable state before the first stamp after a restart.
- A retry by Rama of work that was never visible may give another stamp. A stamp that a reader could
  have seen never changes.
- The plan says how many stamps one clock tick holds, and in which year the packing reaches 2^62.

**C8. The record is durable.** *Given:* a worker restart does not replay a depot; a PState is durable
storage in its own right (SKILL.md). *Derived:* the current facts, the answers, the class, the
permissions, the locks with their bytes, and whatever keeps the stamp increasing survive a worker
restart and a node failure. Nothing that a later decision or a later read needs lives only in the
memory of one process, unless it is built again from durable state before it is used.

**C9. The acts depots are never trimmed.** *Given:* C rule 4; F2 A rests on it. *Derived:* each thing
that is appended stays for ever: an accepted act, a refused act, a thing that is not an act, and the
sealed bytes of each. So nothing secret goes on a depot (C rule 5: never a lock's bytes), and the depots
grow without limit by design.

**C10. The gate is the authority.** *Given:* anything can append to a depot without the door
(R 7-Oct 8); the door's additions are a declaration that the gate checks on arrival (R 6-Oct 2).
*Derived:* no check of a gate assumes an honest door. The gate checks the act-id, the class and the
seal again. The gate cannot check who appended a thing: `:who` is a claim, and the gate holds it only
against the holder of the act's permission (check (d)) and against the rules of a making act
(C item 9). What that leaves open is OPEN POINT 5.

### Things that can arrive on a depot (edge cases of C1, C4, C6, C10)

Anything that the cluster can serialise can be appended. For each kind: what the gate does.

- **X1.** A thing that is not a plain map: nil, a number, a string, a vector, a set, bytes. It holds no
  act-id. It is not an act. The gate does not throw and goes on to the next thing. Whether and where the
  refusal is recorded: **OPEN POINT 1**.
- **X2.** A map with no `:act-id`, or with an `:act-id` that does not have the shape `[id class uuid]`:
  two slots, four slots, a class word that is not one of the two, a last slot that is not a UUID, a layer
  slot that is not an id. No answer can be found by it. **OPEN POINT 1**.
- **X3.** A usable act-id whose layer is not the act's `:layer`. *Derived:* the declaration refuses it,
  because the act-id is there to route the act and to find its answer in the right record (act.clj,
  "Words"). A reader that goes by the act-id looks in another layer's record. **OPEN POINT 1**.
- **X4.** A usable act-id whose class slot is not the class of the gate where the thing arrived: the
  thing is on the other depot. **OPEN POINT 1**.
- **X5.** `:class` is absent, or is not one of the two class words, or is not the class slot of the
  act-id. The act-id is usable and is of this gate. Refused by the declaration; recorded under the
  act-id. The word is the builder's (C rule 8).
- **X6.** The maker's part fails the check of `act.clj`. The recorded reason is the word of `parse`, one
  of seven: `:malformed`, `:unknown-part`, `:unknown-version`, `:no-permission`, `:no-session`,
  `:empty-act`, `:permission-not-in-layer`. *Given:* the gate does not write a second check of the
  maker's form; the declaration contains `act.clj`'s (C item 2; R 6-Oct 1).
- **X7.** A part that neither the maker's ten nor the door's two name, for example `:owner`. Refused as
  an unknown part.
- **X8.** A fact under a plain key with a plain `:v`. Refused `:not-sealed`. The word is the contract's
  (C item 7; proof line "an unsealed value refused").
- **X9.** A fact with a broken seal: `:sealed` and `:v` together; `:sealed` with no `:lock-id`;
  `:lock-id` with no `:sealed`; `:sealed` that is not bytes; `:sealed` with no bytes in it. Refused by
  the declaration. The words are the builder's.
- **X10.** A fact under a store key that arrives sealed. *Derived from C item 2:* a store-key fact
  keeps a plain value, so this is refused by the declaration.
- **X11.** A fact under a plain key with no `:v`, or with `:v nil`, and with no seal. It is a retract
  (act.clj: a fact that gave no `:v`, or a nil one, is a retract). It names no lock.
- **X12.** A fact that carries a part that the form does not name: a layer, a mark. Refused as an
  unknown part (act.clj; R 6-Oct 6).
- **X13.** Facts given as a list and not as a vector. `act.clj` takes both. *Derived:* the index of a
  fact is its position in the order that arrived. A fact id `[act-id index]` rests on that order, so the
  order must be the same after the depot as before it.
- **X14.** A key that looks like a store key and is not one of the three, for example `:tool/class`. It
  is a plain key; its value must be sealed. *Derived consequence, for Sid to know:* the three words
  `:class`, `:permission` and `:lock-request` are taken out of every tool's keys in every layer. A tool's
  fact `{:e some-entity :k :class …}` is refused by F8, whatever the tool meant by "class".
- **X15.** A very large thing: an act of many thousands of facts, a `:based-on` of many thousands of
  entries, sealed bytes of many megabytes. The form bounds the subjects (256), the depth of a value (32)
  and the depth of a permission (4), and nothing else. **OPEN POINT 11**.
- **X16.** A thing that the cluster cannot serialise does not arrive: the append fails at the sender,
  and no gate sees it. *Derived, for the plan to check:* which values Rama carries unchanged (a big
  integer, a decimal, a set, a UUID, a keyword with a namespace, bytes). The digest (C6), the index of a
  fact (X13) and the plain value of a store-key fact rest on it.

### The stamp (edge cases of C7)

- **X17.** More decisions on one task in one clock tick than the counter holds. The stamp still
  increases. The plan says how. (finding: plain milliseconds ran ahead under load.)
- **X18.** The clock of a task goes back: a clock step, or the leader of the task moves to a node whose
  clock is behind. The stamp still increases.
- **X19.** A worker restart. The first stamp after it is later than the last stamp before it.
- **X20.** An act carries a stamp far ahead of the gate's clock, up to 2^62 − 1 (the form allows it).
  "Later than each stamp that the act carries" then pulls the stamps of the task ahead, for every later
  act on that task. **OPEN POINT 8**.
- **X21.** An act carries a stamp so high that no stamp below 2^62 is later. *Derived from C1:* the
  gate gives a refusal, not an exception. Which stamp the answer of that refusal holds: **OPEN POINT 8**.
- **X22.** A microbatch act whose facts are on several tasks. On which task its one stamp is "strictly
  increasing": **OPEN POINT 9**.
- **X23.** The packing of clock time and counter comes near 2^62. The plan says in which year, and shows
  that it is far.
- **X24.** `:claimed-when` is far in the future, or below zero. It is a claim (R 6-Oct 4). The gate's
  stamp does not follow it, and the gate does not refuse for it.

### O1. An act is sent to the streaming depot; the streaming gate decides it

*Given:* a stream topology; one event for each act, on the task of the act's layer; the streaming depot
partitions by the act's `:layer` (C items 1 and 3).

- **Latency.** *Derived from F4 C's heuristic* (R 7-Oct 4): a streaming layer is "work a person engages
  with directly — an editor, a canvas, 3D, live collaboration", which "must be interactive". So the
  decision and its facts are visible within single-digit milliseconds at normal load, and within a few
  tens of milliseconds under load. The door reads the answer right after the ack (O8), so that read is
  on the same path. F9 says there are three limits and that they are targets; the sources do not name
  them or give their numbers: **OPEN POINT 12**.
- **Throughput.** The people and the agents that act in streaming layers drive it: each stroke of work
  in an editor or on a canvas can be an act. It scales with the number of layers in use at one time,
  times the acts per second in each. The acts of one layer are decided one at a time on one task; many
  layers spread over the tasks by their layer id.
- **Consistency and correctness.**
  - *Given:* the checks, in this order (C item 3): (a) the record already holds an answer under this
    act-id — return it and decide nothing; (b) the door's declaration, which contains `act.clj`'s check;
    (c) the layer's class is this gate's; (d) the act's permission stands, or the act is the layer's
    first act; (e) each `:replaces` is the current fact of its place; (f) no setting key on an entity
    other than the layer; (g) a fact under `:permission` writes a permission beneath the act's own, in
    the act's layer, and nothing else. Then accept: the facts, their new versions, and the answer under
    the act-id, with a stamp. Plus the three refusals of the seal (C item 7) and the two refusals "not
    decided" (C4).
  - *Derived:* the acts of one layer are decided in the order in which the depot's partition holds
    them. F2 A's reason ("an act proposed at a version is refused the same way every time", R 7-Oct 2)
    needs that order.
  - *Derived:* each thing that the streaming gate reads or writes for one act of a layer is reached as
    one event on the layer's task, so that C3 holds without any step between tasks. (*Given* in part:
    "one event per act, on the layer's task".)
  - *Given:* the ack is a signal; the record is the truth (R 7-Oct 6, 7). The ack may carry the answer;
    it may fail for an act that the gate accepted. *Derived:* after an ack that waited for the gate, a
    read by act-id gives the answer (proof line "one act landing through the streaming gate and read
    back by act-id").
  - *Given:* either gate can tell which gate a layer belongs to (C rule 7). *Derived:* the streaming
    gate can read the class of a layer of the other class, so that it refuses with "the layer belongs
    to the other gate" and not with "the layer is not made" (X31).
- **Data growth and scale.** For each streaming layer, on that layer's task:
  - current facts: the number of entities of a layer has no limit; the keys of one entity are usually
    few, and the form gives no limit. The gate reaches a place by entity and key, once for each fact of
    an act. No operation of this build reads a range of them; the read side, not built, will.
  - answers: one for each act-id ever decided, never deleted (C2). Reached by act-id, one at a time.
  - permissions: grow with the members, the agents and (later) the sessions of the layer; none ends in
    this build. The gate reaches up to four of them for every act (the chain), by permission id.
  - locks: one for each value ever accepted, and each lock asked for and not used. Never deleted in
    this build. Reached by lock id, once for each sealed value of an act.
  - a whole streaming layer lives with one task (the depot partitions by `:layer`). A very large layer
    is one task's load. That follows from the decision; it is a known limit, not a fault to design
    round.
- **Concurrency.** Two senders into one layer: the depot's order decides, and each act sees the record
  as the act before it left it. Acts of two layers do not meet: nothing in this build is shared between
  two layers.
- **Edge cases.**
  - **X25.** The ack fails or times out for an act that the gate accepted (R 7-Oct 6). The door reads
    by act-id.
  - **X26.** The door sends the act again after a lost ack. The depot now holds the thing twice. The
    second is a repeat (O7).
  - **X27.** The worker stops in the middle of an act. Nothing of that act is visible. After the
    restart the gate goes on from where it was; it does not replay the depot from its start (SKILL.md).
    The act is decided once.
  - **X28.** The leader of the layer's task moves to another node. Each decision that was visible
    before is in the record there. An act that was acked is not lost.
  - **X29.** Rama processes a depot record a second time after a failure. *Derived from F2 A's reason:*
    an act of a layer that is not yet decided is not decided after a later act of that layer. The plan
    shows this from Rama's guarantees; it does not assume it.
  - **X30.** One layer with very many acts a second. Its rate is the rate of one task (see "Data growth
    and scale").
  - **X31.** An act names a layer of the other class: the act-id `[:base :streaming uuid]` on the
    streaming depot. Refused at check (c): the layer belongs to the other gate.

### O2. An act is sent to the microbatch depot; the microbatch gate decides it

*Given:* a microbatch topology with the same checks, over a batch, across tasks; the same record shape;
the microbatch depot partitions by one entity that the depot derives from the act's facts (C items 1
and 4; R 6-Oct 10). The entity of the first fact is the builder's choice (C item 1).

- **Latency.** *Derived from F4 C's heuristic:* "data subscribed to from another layer may arrive a
  batch later". Hundreds of milliseconds, up to a few seconds under load, are acceptable. *Given
  (finding):* the decisions of one batch are visible only when the slowest task of the batch is done.
  There is no ack that carries an answer (R 7-Oct 6); the door reads by act-id until an answer is there.
- **Throughput.** The acts into microbatch layers drive it. In this build the base is the only one
  (F4 C; re-class is not built): few senders write to it and everyone reads it. The size of a batch
  grows with the load. The gate is correct at each size of a batch.
- **Consistency and correctness.**
  - *Given:* the same checks (a) to (g), the same refusals, the same record shape as O1 (C item 4).
  - *Given:* in one batch, two acts that replace the same fact get the same winner on every attempt;
    an act that touches several tasks is accepted whole or refused whole (C rule 6).
  - *Derived, wider than the one case that rule 6 names:* the decisions of one batch are those that the
    gate would give if it decided the acts of the batch one at a time in one order, and that order is
    the same on every attempt of the batch. The cases that need it are X32 to X37. Without one order, the
    invariants of the record can break inside a batch: one current fact for a place, one answer for an
    act-id, one value for a lock, one making for a layer.
  - *Derived:* the stamps of the acts of a batch agree with that order where two acts meet: of two
    accepted facts on one place, the current one holds the later stamp.
  - *Given:* a microbatch act's answer is found from its act-id alone (C rule 7; R 7-Oct 6, 7). The
    reader has the act-id; it does not need the act's facts or its first entity to find the answer.
  - *Given:* the decisions of one batch are visible together (C item 4). *Derived:* C3 holds across
    tasks: no reader sees the accepted answer of a microbatch act while a fact of that act on another
    task is not yet visible.
  - *Given:* do not send all acts through one task unless the plan shows that it must (C rule 6; open
    item ?15).
  - *Given, and left out of the contract's F5 line:* "a gate reads on its own task" (R:129–131), with
    Sid's cost reason: "a fetch is a cross-task hop on every act". *Derived:* the facts of a microbatch
    layer are on several tasks, by entity. The class of the layer, the permissions of the layer and the
    locks of the layer are needed by an act on every one of those tasks. The plan counts the hops
    between tasks that one act makes, and says why each is needed.
  - *Given, to find out and to say in the plan* (C rule 9): whether a rebuild from the acts depots picks
    the same winner when two microbatch acts on different entities meet. See X42 and **OPEN POINT 10**.
- **Data growth and scale.** The same collections as O1, for each microbatch layer, with one
  difference: the facts of the layer are spread over the tasks by entity. The base is the layer that
  "everyone subscribes to" (R 7-Oct 4): its facts are read far more than they are written, by the read
  side that is not built.
- **Concurrency.** Everything in one batch is at the same time. The one order above settles each
  meeting. Between two batches: the later batch sees the record as the earlier one left it.
- **Edge cases.**
  - **X32.** Two acts in one batch replace the same fact. One wins; the other is refused as stale; the
    same one on every attempt (C rule 6).
  - **X33.** Two acts in one batch write one place and name nothing to replace. Both are accepted
    (C rule 2). One of them is the current fact when the batch is visible: the same one on every attempt,
    and it holds the later stamp.
  - **X34.** In one batch, one act writes a place and names nothing to replace; another replaces the
    fact that was current before the batch. The order decides whether the second is stale. The same
    order on every attempt.
  - **X35.** The same act-id twice in one batch. The same thing twice: one decision, and the other is a
    repeat. Two different things, which can be on two tasks because their first facts name different
    entities: one is decided, the other is told that the act-id is used; the same one on every attempt.
  - **X36.** An act whose facts are on entities of several tasks, and one fact is stale on one task.
    The act is refused whole: no task keeps a fact of it (C rule 6).
  - **X37.** In one batch: a making act of a layer and another act into that layer; a permission given
    and an act under it; a request for locks and a sealed value under one of those locks (only a forger
    sends the last: the door reads the bytes first). The one order decides each.
  - **X38.** An attempt of a batch fails on one task. Nothing of the attempt is visible on any task.
    The retry decides the same batch and gives each act the same answer.
  - **X39.** One slow task holds back every decision of the batch (finding). A reader by act-id sees
    nothing until then, and then all of them.
  - **X40.** A batch of nothing, of one act, of many thousands. The gate is correct at each size. A
    long batch gives the task back from time to time, so that a read is not held for the whole batch
    (SKILL.md, cooperative multitasking).
  - **X41.** An act for a streaming layer arrives on the microbatch depot, with an act-id of the class
    microbatch. Refused at check (c) and recorded under the act-id. The gate reads the layer's class
    from the record of the other gate's layer (C rule 7).
  - **X42.** Rule 9's case: two microbatch acts whose first facts name different entities meet (both
    name one lock, or both make the base, or both carry one act-id, or both replace a fact that neither
    has first). Whether a rebuild from the depots gives the same winner is to find out and to say in the
    plan. This paper does not answer it.

### O3. Make a layer (the first act of a layer)

*Given:* a layer is made by its first act; the form of that act is the builder's (C item 9; R 8-Oct 2:
"the builder's part, not his", so that the making of a layer is in the acts depot). A gate accepts an
act into a layer for which the record holds nothing when it is a making act: it writes the layer's
class and one permission, the layer's first, with nothing above it, and it names that permission as the
one it acts under.

- **Latency.** The base: once in the life of a cluster, through the microbatch gate; seconds are
  acceptable. A personal layer: on the path of a person's first sign-in, through the streaming gate; a
  few tens of milliseconds. A group's layer: when a person creates a group, a direct action; the
  streaming gate's latency.
- **Throughput.** One making act for each layer: one for each person who signs in for the first time,
  one for each group that is created, one for the base. Rare beside every other act.
- **Consistency and correctness.**
  - *Given:* the base, `:base`: class microbatch; the first permission is `[:system :base :base]`; the
    act's `:who` is `:system` (C item 9; F4 C; E1·H8 A).
  - *Given:* any other layer: class streaming; the act's `:who` is the holder of the first permission
    (a group's layer, made by the person who creates the group) or `:system` (a personal layer, made for
    its person) (C item 9; K2).
  - *Given:* any other act into a layer that is not made is refused (C item 9).
  - *Derived:* a layer is made once. After it, the layer has one class and one top permission, and no
    act of this build changes either: a change of class is refused (C item 3), and a permission with
    nothing above it is not "beneath the act's own" (check (g)).
  - *Derived:* the base goes through the microbatch gate and every other making act through the
    streaming gate, because the act-id of a making act carries the class that F4 C gives. So each layer
    is made by one gate only, and the two gates never both make one layer.
  - *Derived:* the class and the first permission are visible together with the answer (C3). A read of
    the class (O10) never gives a class for a layer whose making act has no accepted answer.
  - *Derived:* the making act is the one exception to two checks: to (d), as the contract says, and to
    (g), because the permission that it writes is its own and not beneath its own.
  - *Derived:* a making act carries no sealed value. A lock is made for a layer on a request into that
    layer (C item 7), and a request into a layer that is not made is refused. The class and the
    permission are store-key facts with plain values, so a making act needs no lock.
  - *Tension to know* (see the last section): at the first act of a personal layer the system acts
    under a permission whose holder is the person. R 8-Oct 3 says that each actor acts under its own
    permission (E4 A); the contract makes the first act the exception.
- **Data growth and scale.** One class fact and one first permission for each layer. The number of
  layers has no limit: one for each person, one for each group.
- **Concurrency.** Two making acts for one layer: one is accepted. In the streaming gate they are
  decided one after the other. In the microbatch gate (the base) one batch can hold both, on two tasks;
  one wins, the same one on every attempt (O2).
- **Edge cases.**
  - **X43.** An act into a layer that is not made, and it is not a making act. Refused (C item 9; proof
    line "an act into a layer that is not made refused").
  - **X44.** A making act for `:base` whose `:who` is not `:system`, or whose permission is not
    `[:system :base :base]`. It is not a making act of the base. Refused.
  - **X45.** A making act for `:base` on the streaming depot, or with the class streaming. Refused
    (F4 C).
  - **X46.** A making act for another layer on the microbatch depot, or with the class microbatch.
    Refused (F4 C: every other layer starts streaming).
  - **X47.** A making act for another layer whose `:who` is neither `:system` nor the holder of the
    first permission. Refused.
  - **X48.** A making act whose facts are not the two that C item 9 names: no class fact; no permission
    fact; two permission facts; a class fact on an entity that is not the layer; a permission fact that
    is not the permission that the act names; a first permission with a permission above it; more facts
    beside the two. The form is the builder's; whatever it is, each act that does not have it is "any
    other act into a layer that is not made", and is refused.
  - **X49.** A making act with a fact that names something to replace. Nothing is current in a layer
    that is not made, so the fact is stale (check (e)). Refused.
  - **X50.** A making act for a layer that is made. Refused. The contract gives no word of its own for
    it. Unless the plan gives one (C rule 8), the first check that fails in the order gives the reason:
    with `:who :system` and a person's permission it is (d); with `:who` the holder it passes (d) and
    fails (g), because a first permission is not beneath the act's own; and its class fact is a change
    of class.
  - **X51.** Two making acts for one layer at the same time. See "Concurrency".
  - **X52.** A personal layer or a group's layer is made before the base is made. The contract's checks
    of a making act name nothing in the base, so nothing is read there. Said here so that the plan does
    not add a read of another layer.
  - **X53.** After the system makes a personal layer, the system holds no permission in it: the first
    permission is the person's. A second act there by `:system` is refused at (d) (E4 A). *Derived.*
  - **X54.** A making act with `:who :system` from a sender that is not the system. The gate cannot
    tell who appended (C10). In this build any sender can make any layer that is not made, for any
    holder. **OPEN POINT 5**.
  - **X55.** A making act for a layer other than the base whose first permission is held by `:system`.
    E3·H7 B says the top of a chain is a person, "or the system for the base". **OPEN POINT 5**.
  - **X56.** A `:class` fact in a layer that is made, on the layer's own entity. A different class: a
    change of class, refused with a word that says it is not decided (C item 3). The same class, or no
    value: **OPEN POINT 4**.
  - **X57.** A `:class` fact on an entity that is not the act's layer. Refused (F8; C item 3(f); proof
    line "a setting key on another layer refused"), in each state of the layer.
  - **X58.** A `:class` fact whose value is not one of the two class words. Refused by the declaration.

### O4. Write value facts and retracts

*Given:* a fact is `{:e :k :v :replaces}`; a fact with no value is a retract (act.clj). What arrives has
each value fact's `:v` replaced by `:sealed` bytes and a `:lock-id` (C item 2).

- **Latency.** The latency of the gate of the layer's class (O1 or O2).
- **Throughput.** Most acts are of this kind. One act holds one fact or more; the form gives no limit
  to the number (X15).
- **Consistency and correctness.**
  - *Given:* a plain value under a plain key is refused `:not-sealed`; a sealed value is refused when
    the store did not make its lock for this layer; a second value under a lock that already seals one
    is refused (C item 7).
  - *Given:* each `:replaces` is the current fact of its entity and key, or the act is refused as stale
    (C item 3(e)). A fact that names nothing to replace is never refused as stale; it becomes the
    current fact of its place (C rule 2). A retract is recorded as the current fact of its place, so
    that a later fact can replace it (C rule 3).
  - *Given:* after accept, the place holds: the sealed bytes (or that it is a retract), the id of the
    fact that wrote it, and the stamp (C item 5). *Derived:* the id of a fact is `[act-id index]`, the
    index being the fact's position in the act (act.clj; X13). That id is the "version" that a later
    `:replaces` names (R 6-Oct 4).
  - *Derived:* after accept, each lock that the act names seals one fact of this act and can seal no
    other, for ever. The record can tell, for a lock, whether it seals a value and which fact; and for a
    value, which lock seals it. *Given:* a lock sits with its value in the record, so that a later
    forget destroys the lock and the value is gone (C item 7).
  - *Derived:* a fact that is replaced is no longer a current fact. Its sealed bytes stay in the depot
    (C9). Its lock stays in the record: destroying a lock is forget's step, which is not built, and "the
    lock table must make it possible" (C "Not in this build").
  - *Derived:* a place never goes back to "empty". A retract is a fact of its place.
  - *Derived:* the facts of an act are facts of the act's layer. A fact has no layer of its own
    (R 6-Oct 6). No act writes into the record of another layer.
  - *Given by silence:* the checks of this build do not read `:based-on`, `:because-of`, `:subjects`,
    `:session` or `:claimed-when` against the record (X74). They stay in the depot with the act. The
    record that C item 5 lists holds none of them.
- **Data growth and scale.** The places of a layer: no limit. The locks: one for each value fact ever
  accepted. The gate reaches a place once for each fact, and a lock once for each sealed fact, each by
  its id. An act of many facts reaches many places.
- **Concurrency.** Two acts replace the same fact: the first in the order is accepted, the second is
  stale. Two acts write one place and name nothing: both are accepted, and the later is current. In a
  streaming layer the order is the depot's; in a microbatch, the batch's one order (O2).
- **Edge cases.**
  - **X59.** A value, nothing to replace, an empty place. Accepted; it is the current fact.
  - **X60.** A value, nothing to replace, a place that holds a fact. Accepted; never stale (C rule 2).
    The earlier fact is no longer current; its lock stays.
  - **X61.** `:replaces` names the current fact of the place. Accepted (proof line, the first half of
    "a stale `replaces` refused").
  - **X62.** `:replaces` names a fact that is not the current fact of the place: an earlier fact of the
    place; a fact of another place; a fact of another layer; a fact id whose index is past the end of
    its act; a fact of an act that was refused; any fact id at all when the place is empty. Refused as
    stale. One reason for all of them: the current fact of the place is not the one named.
  - **X63.** A retract, nothing to replace, an empty place. Accepted (C rule 2). The place holds a
    retract (C rule 3).
  - **X64.** A retract of a retract. Accepted by the same two rules. The place holds the later retract.
  - **X65.** A value on a place that holds a retract, naming the retract's fact id or naming nothing.
    Accepted (C rule 3).
  - **X66.** Two facts of one act on the same place. **OPEN POINT 3**.
  - **X67.** A fact of an act names in `:replaces` a fact of the same act. **OPEN POINT 3**.
  - **X68.** A sealed value whose lock id the store does not know, or whose lock the store made for
    another layer. Refused (C item 7).
  - **X69.** A sealed value under a lock that already seals a value. Refused (C item 7). It is not
    refused when the thing is the same act again: that is a repeat, and check (a) answers it first (C5).
  - **X70.** Two facts of one act under one lock. The second is "a second value under a lock that
    already seals one". The act is refused whole.
  - **X71.** An act is refused for one fact, while another fact of it named a good lock that was not
    used. Is that lock free again? **OPEN POINT 6**.
  - **X72.** Sealed bytes that are not what the lock gives: any bytes under a real lock id. The three
    refusals of C item 7 do not include opening the value. **OPEN POINT 7**.
  - **X73.** A value fact on the layer's own entity, under a plain key. A fact like any other;
    accepted. *Given:* the record leaves room for a layer's own facts (F5 A; C:54–55).
  - **X74.** `:based-on` names a fact that the record does not hold, or gives a stamp that is not that
    fact's stamp; `:because-of` names an act-id that has no answer. No check of the contract reads
    either. Only the stamp bound of C7 uses `:based-on`. **OPEN POINT 8** for that bound.

### O5. Give a permission

*Given:* a permission is `[who layer granted-in]`, or the same with a fourth slot, the permission above
it; four deep at most (act.clj). Whoever holds a permission can give one beneath it (E2 A). A fact
under `:permission` writes a permission beneath the one that the act itself acts under, in the act's
layer, and nothing else (C item 3(g)). Permissions are kept as facts (C item 5).

- **Latency.** The latency of the gate of the layer's class. The check of a permission, (d), is on the
  path of every act of every kind, so its cost counts for every act.
- **Throughput.** To give one is rare: an invitation into a group, an agent set up beneath a person.
  Later, with sign-in, a session beneath a person (C "Not in this build"); that is one for each session
  and is not rare. To check one is as common as acts are.
- **Consistency and correctness.**
  - *Given:* the act's permission stands when the record holds it, each permission above it in its
    chain stands, and its holder is the act's `:who` (C item 3(d); E4 A).
  - *Given:* a retract of a permission is refused with a word that says it is not decided (C item 3).
    *Derived:* in this build a permission that stands, stands for ever. The check "each permission above
    it stands" cannot fail apart from "the record does not hold it" (X77). It stays, and the record
    leaves room for a permission that has ended, because who may end one is not decided, not ruled out.
  - *Derived from (g) and the form's one rule:* each permission of a layer has its whole chain in that
    layer. The first has nothing above. Each later one is beneath the permission of the act that gave
    it, and that permission is in the act's layer (act.clj, `:permission-not-in-layer`). So the check of
    a chain reads the record of one layer only.
  - *Derived from (g):* a holder gives only straight beneath its own permission. It cannot give beside
    it, beneath another's, or at the top.
  - *Derived:* a permission that an act gives stands from the moment that act's answer is visible (C3),
    not before.
  - *Derived:* the first holder of every chain is the `who` of the layer's first permission. The gate
    cannot tell a person from a session or an agent: each is an id (R 6-Oct 9). What the gate holds of
    "the top is a person … never a session" (E3·H7 B) is **OPEN POINT 5**.
- **Data growth and scale.** The permissions of a layer grow with each give and never shrink in this
  build. A chain is four long at most, so the check of one act reads four permissions at most.
- **Concurrency.** A give and the first act under it: the order decides; the act under it is accepted
  only after the give. In one microbatch, the batch's one order (X37).
- **Edge cases.**
  - **X75.** An act under a permission that the record does not hold. Refused at (d) (proof line "an
    act under a permission that does not stand refused").
  - **X76.** An act under a permission that the record holds, by a `:who` that is not its holder.
    Refused at (d) (E4 A).
  - **X77.** An act under a permission whose chain names, above it, a permission that the record does
    not hold. Refused at (d). In this build the record cannot hold the lower one without the upper
    (derived from (g)), so this is always also "the record does not hold it".
  - **X78.** An act under a permission for another layer. Refused by the form,
    `:permission-not-in-layer` (act.clj; proof line "an act with a permission for another layer
    refused"). The record is not read for it.
  - **X79.** A permission id five deep, as the act's permission or as the value of a permission fact.
    It is not a permission id (act.clj: four at most). Refused by the form or by the declaration. So the
    holder of a permission four deep cannot give one.
  - **X80.** A fact under `:permission` whose value is not a permission id. Refused by the declaration.
  - **X81.** A permission fact that is beneath the act's own permission, in the act's layer. Accepted;
    an act under the new permission by its holder is then accepted (proof line "a permission given
    beneath another by its holder, and an act under it accepted").
  - **X82.** A permission fact whose fourth slot is not exactly the act's own permission: beneath a
    permission further up the chain, beneath a permission the giver does not hold, or with nothing above
    it in a layer that is made. Refused at (g) (proof line "a permission given by one who does not hold
    the one above refused"; the other way to that line is X76).
  - **X83.** A permission fact whose layer slot is not the act's layer. Refused at (g): "in the act's
    layer".
  - **X84.** A permission fact whose `granted-in` slot is not the act's layer. **OPEN POINT 5**.
  - **X85.** A holder gives a permission to itself, beneath its own. No rule of the sources forbids it.
    Accepted if the other checks pass.
  - **X86.** The same permission is given a second time; or a second permission fact lands on the place
    of an earlier one, with `:replaces` or with none. **OPEN POINT 4**.
  - **X87.** A fact under `:permission` with no value: a retract of a permission. Refused with a word
    that says it is not decided (C item 3).
  - **X88.** One act gives a permission and names that same new permission as the one it acts under.
    The new one does not stand before the act. Refused at (d).
  - **X89.** One act gives a permission and writes values. Whole or nothing (C3): when a value fact is
    stale, the permission is not given.

### O6. Ask for locks

*Given:* the module makes locks on request. The request is an act with a fact under `:lock-request`,
because in Rama a query cannot write and both depots are for acts. A request may ask for several locks
at once, ahead of need, so that sealing a value does not wait on the store. How a lock that was asked
for ahead comes to sit beside its value is the plan's. (All C item 7.) A lock's bytes are made inside
the module and are never appended to a depot; neither a retry nor a replay makes a lock again for a
value that is already sealed (C rule 5). The working answer for the open row H1: a lock for each value
(R:196–201).

- **Latency.** The request is an act: the latency of the gate of the layer's class. For a microbatch
  layer the locks come a batch later. *Derived from "ahead of need":* a request is not on the path of
  one stroke of work; a sender that has locks in hand seals with no wait.
- **Throughput.** One lock for each value fact ever written, so locks are as many as values. Requests
  are fewer by the number of locks that one request asks for.
- **Consistency and correctness.**
  - *Derived:* a request is an act like any other: each check (a) to (g) holds for it, the permission
    first. A request that is refused makes no lock.
  - *Derived:* a request that is accepted makes as many locks as it asks for, each for the act's layer
    and for no other layer (C item 7: "a sealed value whose lock the store did not make for this
    layer").
  - *Derived from R 7-Oct 7:* the door finds the locks of its request from the request's act-id. How
    (the lock ids are in the request, or follow from its act-id, or are in its answer) is the plan's
    (C:201–203, "how locks are asked for and read").
  - *Derived from C rule 5:* which locks a request makes does not depend on the attempt. A retry and a
    replay find the locks that the first run made; they do not make others beside them.
  - *Derived:* once a reader could have read a lock's bytes, those bytes never change: a value may be
    sealed with them already. Bytes of an attempt that was never visible may differ in the retry.
  - *Derived from C8:* a lock and its bytes survive a worker restart and a node failure. The bytes are
    in no depot, so nothing can make them again; a lock that is lost is each value under it lost.
  - *Derived:* the bytes come from a source that cannot be guessed, and each lock has its own.
  - *Given:* room is left for two things that are not built: wrapping a lock under the locks of the
    people the value is about, and a switch of the grain (C:127–128). The README names the other answer
    of H1 as "one for each session and layer" (R:198); see the last section.
- **Data growth and scale.** The locks of a layer grow without limit: one for each value ever accepted,
  and each lock asked for and not used. None is deleted in this build. The number that one request can
  ask for needs a limit, so that one act cannot write without bound (phases.md, cross-phase rule on
  write volume); the number is the plan's, with its reason (**OPEN POINT 11** holds the other limits).
- **Concurrency.** Two requests at the same time make two sets of locks with no lock in both. A request
  and a first use: the use is accepted only after the request (X37 for one microbatch).
- **Edge cases.**
  - **X90.** A request for no locks, for a count below zero, for a count that is not an integer, or for
    more than the limit. Refused; no lock is made.
  - **X91.** A request into a layer that is not made. Refused like any act there (C item 9).
  - **X92.** A request under a permission that does not stand. Refused at (d); no lock is made.
  - **X93.** A request sent twice. The second is a repeat: answered from the record; the locks are the
    ones that the first made; no new lock (C rule 5).
  - **X94.** An event or an attempt of a batch that made locks fails before it is visible. No reader saw
    those bytes. The retry may make other bytes.
  - **X95.** A lock that is asked for and never used. It stays. The sources give no end to it; "lease"
    is the rig's word and is not taken (C:38–39).
  - **X96.** A lock is used by a session or a `:who` that did not ask for it, in the same layer. The
    contract binds a lock to its layer only. Accepted, if the act's other checks pass.
  - **X97.** The door reads a lock before the answer of its request is visible. Nothing yet.
  - **X98.** The door reads a lock id that the store never made. Nothing.
  - **X99.** A worker restart, or a move of the leader, between a request and the use of its lock. The
    lock and its bytes are the same.
  - **X100.** A fact under `:lock-request` with `:replaces`: check (e) holds for it as for any fact. A
    fact under `:lock-request` with no value (a retract): **OPEN POINT 4**.
  - **X101.** One act asks for locks and also carries sealed values under locks of an earlier request.
    The form allows it. Whole or nothing (C3): when a value fact is refused, no lock is made.

### O7. Send under an act-id that was sent before

Three things look alike to a gate and are told apart. *A retry by Rama:* Rama gives the gate the same
depot record again after a failure. *A repeat:* a sender appends the same thing again, so the depot
holds it twice. *A second, different act:* another thing under an act-id that is used.

- **Latency.** The latency of the gate. *Derived from check (a) being first:* a repeat costs less than
  a decision: the gate reads the answer by act-id and stops.
- **Throughput.** Low in normal work. In bursts when the network fails: a door sends each act that is
  in flight again.
- **Consistency and correctness.**
  - *Given:* a repeat is answered from the record and not decided twice (R 7-Oct 7; proof line "a
    repeat … answered from the record, not decided twice"). *Derived:* what a reader can see of "not
    decided twice": the answer of the repeat holds the stamp of the first decision, and no place, lock
    or permission has changed.
  - *Given:* a second, different act under a used act-id does not read back the first act's answer as
    its own; the gate tells the two apart by the digest that each answer keeps (C rule 1; proof line "a
    second, different act under a used act-id told so"). How the sender of the second act is told, and
    what the record keeps of that act: **OPEN POINT 2**.
  - *Derived:* a retry by Rama of work that became visible is a repeat. A retry of work that did not
    become visible is a first decision (X27, X38).
  - *Derived from C2:* a used act-id stays used. A sender that wants to try a refused act again takes a
    new act-id.
- **Data growth and scale.** A repeat and a second act add to the depot (C9). A repeat adds nothing to
  the record. What a second, different act adds: **OPEN POINT 2**.
- **Concurrency.** A repeat can arrive while the first is not yet decided. In the streaming gate the
  two are in one partition, one after the other. In one microbatch: X35.
- **Edge cases.**
  - **X102.** The same thing again, after the first was accepted. The recorded answer, with the same
    stamp. Nothing is written.
  - **X103.** The same thing again, after the first was refused. The recorded refusal, with the same
    reason, also when the record has moved so that the act would now pass (C5).
  - **X104.** A different thing under a used act-id. It is told that the act-id is used by another act.
    **OPEN POINT 2** for how.
  - **X105.** The first thing under an act-id was not an act and was refused `:malformed`; the second
    is a good act. The act-id is used; as X104.
  - **X106.** The door seals a value again and sends under the same act-id. The sealed bytes differ, so
    this is a different thing (C6). The door keeps what it sent and sends that.
  - **X107.** The same act-id on both depots. One act-id must not get two answers (C2).
    **OPEN POINT 1**.

### O8. Read the answer by act-id

*Given:* a query topology: act-id → the recorded answer, or nothing yet (C item 8). The answer is
`:accepted`, or `:refused` with a reason, and a stamp, with the digest of rule 1 (C item 5).

- **Latency.** Single-digit milliseconds. *Derived from R 7-Oct 6 and 7:* the door reads the answer
  after every act, so this read is on the path of every act.
- **Throughput.** One read at least for each act. For a microbatch act the door reads until an answer
  is there; the plan says how many reads one microbatch act costs, since each read from outside is a
  network roundtrip (SKILL.md).
- **Consistency and correctness.**
  - *Given:* it finds the answer from the act-id alone, for an act of either class (C rule 7).
  - *Derived from C2:* when a read has given an answer for an act-id, each later read gives the same
    answer, through restarts and node failures.
  - *Derived from C3:* it never gives "accepted" for an act of which a fact is not yet visible.
  - *Derived:* "nothing" means that no decision is visible under this act-id. It does not mean refused.
    The reader cannot tell "not yet" from "never arrived" (X108).
  - *Derived from C rule 1:* what it gives lets the reader see whether the answer is of the reader's
    own act (**OPEN POINT 2**).
  - *Given:* a read writes nothing (C item 7: in Rama a query cannot write).
- **Data growth and scale.** It reads one answer by its act-id. The answers have no limit (C2).
- **Concurrency.** A read at the same time as the decision gives nothing or the whole answer.
- **Edge cases.**
  - **X108.** An act-id that no gate has decided: the thing never arrived, or is not yet decided.
    Nothing. The reader sends the same thing again (O7).
  - **X109.** A read while the act's batch is not yet visible. Nothing; a later read gives the answer.
  - **X110.** A read that is given something that is not an act-id. Nothing; it does not fail the
    module.
  - **X111.** A read right after a streaming ack that waited for the gate. The answer.
  - **X112.** Two reads of one act-id, any time apart, with a restart between. The same answer.
  - **X113.** The sender of a second, different act reads by the act-id. It does not take the first
    act's answer as its own. **OPEN POINT 2**.

### O9. Read a lock's bytes

*Given:* the door reads a lock's bytes by a query (C item 7).

- **Latency.** Tens of milliseconds are acceptable, because locks are read ahead of need. *Derived from
  "sealing a value does not wait on the store" and the cost of a roundtrip (SKILL.md):* the door gets
  the locks of one request without one roundtrip for each lock.
- **Throughput.** Each lock is read once at least: as many reads of a lock as values, in groups.
- **Consistency and correctness.**
  - *Derived:* it gives the same bytes for a lock at every read (O6).
  - *Derived:* it gives the bytes in each state of the lock: not yet used; sealing a current value;
    sealing a value that is no longer current. The door needs the first. The read side, not built,
    needs the other two to open a value.
  - *Derived:* it gives nothing for a lock that the store did not make, and nothing before the answer
    of the lock's request is visible.
  - *Derived from C rule 5:* this read is the one way that a lock's bytes leave the module. They go to
    no depot.
  - Who may read a lock: the sources give no rule. **OPEN POINT 7**.
- **Data growth and scale.** It reads locks by their ids, a few at a time. The locks have no limit.
- **Concurrency.** A read at the same time as the request's decision gives nothing or the bytes.
- **Edge cases.**
  - **X114.** A read of a lock that seals a value, and of a lock whose value is no longer current. The
    bytes, the same as before.
  - **X115.** A read of a lock of a layer in which the reader holds no permission. Nothing in this
    build refuses it. **OPEN POINT 7**.

### O10. Read a layer's class

*Given:* the proof's hand "reads the class" (C "The proof"); the class is a fact on the layer's own
entity (F1 A; C item 2); either gate can tell which gate a layer belongs to (C rule 7). The contract
names this read and does not say how the module serves it; that is the plan's.

- **Latency.** Single-digit milliseconds. *Derived:* the door needs the class to make an act-id, so the
  read is on the path of every act, unless the door keeps what it read. In this build the class of a
  made layer does not change, so a door may keep it; with re-class (not built) it may not.
- **Throughput.** As many as acts, at the most.
- **Consistency and correctness.**
  - *Derived:* it gives nothing for a layer that is not made, and the class for a layer that is made:
    microbatch for `:base`, streaming for every other (F4 C).
  - *Derived from C3:* the class is visible together with the accepted answer of the making act.
  - *Derived from C rule 7:* it finds the class from the layer id alone, for a layer of either class.
- **Data growth and scale.** One class for each layer; read by layer id.
- **Concurrency.** A read at the same time as the making gives nothing or the class.
- **Edge cases.**
  - **X116.** A layer that is not made. Nothing (proof lines: the class is shown after each making).
  - **X117.** A read while the batch that makes the base is not yet visible. Nothing yet.
  - **X118.** The door reads "not made"; another sender makes the layer; the door sends a making act.
    The act is refused (X50). The door reads the class again.
  - **X119.** A read that is given something that is not an id. Nothing; it does not fail the module.

### The proof's lines, and where this paper holds each

The contract's proof shows sixteen things (C:143–152). Each is an operation or an edge case above.

| The proof shows | Here |
|---|---|
| the base made by the system, with the class microbatch | O3, O2, O10 |
| a person's layer made by the system for that person, with the class streaming | O3, O1, O10 |
| a group's layer made by the person who creates it | O3, O1 |
| an act into a layer that is not made refused | X43 |
| one act landing through the streaming gate and read back by act-id | O1, O8, X111 |
| one through the microbatch gate | O2, O8, X109 |
| a refusal recorded with its reason | C3, O8 |
| a repeat answered from the record, not decided twice | O7, X102 |
| a second, different act under a used act-id told so | X104, OPEN POINT 2 |
| an unsealed value refused | X8 |
| a stale `replaces` refused | X62 |
| a setting key on another layer refused | X57 |
| an act with a permission for another layer refused | X78 |
| an act under a permission that does not stand refused | X75 |
| a permission given beneath another by its holder, and an act under it accepted | X81 |
| a permission given by one who does not hold the one above refused | X82, X76 |

## Entity State × Write Matrix

Six entities are the targets of writes. For each: its states, its transitions, and one row for each
state and each write that can meet it.

**The reads of each row.** Three reads are open to a caller in this build, and each can be called in
each state. A fourth line says what the gate finds for the next act; no caller can read that in this
build, but it decides every later write, so each row says it.

- *answer*: O8, by the act-id of the write of the row.
- *class*: O10, for the layer of the write.
- *lock*: O9, for each lock that the write names or makes.
- *next act*: what the gate finds when the next act arrives.

In each row "refused" means: the answer holds the refusal and its reason, with a stamp; nothing else in
the record changes (C3). A row says so again only where something could be thought to change.

### E1. A layer

**States.** *Not made:* the record holds nothing for the layer. *Made, streaming.* *Made, microbatch:*
in this build `:base` only. Not built: a layer whose class was changed (re-class, ?9).

**Transitions.**
- **T1.** Not made → made, microbatch: the making act of `:base` is accepted.
- **T2.** Not made → made, streaming: the making act of a personal layer, by `:system`, is accepted.
- **T3.** Not made → made, streaming: the making act of a group's layer, by the holder of its first
  permission, is accepted.
- **T4.** Not made → not made: any act that is refused.
- **T5.** Made → made, the same class: every act. No act of this build takes a layer out of "made" or
  changes its class.

```
Not made × a making act that is right (the right gate, class, :who and first permission)
  - answer: accepted, with a stamp.
  - class: the class that F4 C gives: microbatch for :base, streaming for any other layer. Visible
    together with the answer, never before it.
  - lock: nothing, for any lock id. A making act makes no lock and names none.
  - next act: the layer is made. The first permission stands. An act under it by its holder passes
    check (d). In a personal layer an act by :system does not pass (d) (X53).

Not made × a making act that is wrong (X44 to X49)
  - answer: refused.
  - class: nothing. The layer is still not made.
  - lock: nothing.
  - next act: the layer is not made. A right making act is still accepted.

Not made × any other act (value facts, a permission given, a request for locks)
  - answer: refused: the layer is not made (X43, X91).
  - class: nothing.
  - lock: nothing. A request into a layer that is not made makes no lock.
  - next act: the layer is not made.

Made, streaming × a making act for the same layer
  - answer: refused (X50).
  - class: streaming, as before.
  - lock: each lock of the layer as before.
  - next act: the first permission is the one that the first making act wrote; nothing of the second
    making act is kept.

Made, streaming × any other act, through the streaming gate
  - answer: accepted or refused, by checks (a) to (g) and the checks of the seal.
  - class: streaming, as before.
  - lock: as the row of E4 for what the act does.
  - next act: the layer is made, the class is streaming; places, permissions and locks as the rows of
    E2, E3 and E4.

Made, streaming × any act, through the microbatch gate (the act-id's class is microbatch)
  - answer: refused at check (c): the layer belongs to the other gate (X41). Recorded under the act-id
    of class microbatch, which is where a reader with that act-id looks.
  - class: streaming, as before.
  - lock: as before; no lock is made or used.
  - next act: nothing changed in the layer.

Made, streaming × an act with a :class fact on the layer's own entity
  - answer: a different class: refused, with a word that says a change of class is not decided. The
    same class, or no value: OPEN POINT 4.
  - class: streaming, as before, for the refused case.
  - lock: as before.
  - next act: nothing changed.

Made, streaming × an act with a :class fact on an entity that is not the layer
  - answer: refused at check (f) (F8; X57).
  - class: streaming, as before. The class of the layer that the entity names (if it is a layer) is not
    changed either: no act writes into another layer.
  - lock: as before.
  - next act: nothing changed.

Made, microbatch × a making act for the same layer
  - answer: refused (X50). Visible when its batch is visible.
  - class: microbatch, as before.
  - lock: as before.
  - next act: the first permission is [:system :base :base], as before.

Made, microbatch × any other act, through the microbatch gate
  - answer: accepted or refused by the same checks; visible when its batch is visible.
  - class: microbatch, as before.
  - lock: as the row of E4 for what the act does.
  - next act: as the rows of E2, E3 and E4; an act of the same batch that is later in the batch's one
    order finds the same as an act of a later batch would.

Made, microbatch × any act, through the streaming gate (the act-id's class is streaming)
  - answer: refused at check (c): the layer belongs to the other gate (X31). Recorded under the act-id
    of class streaming.
  - class: microbatch, as before.
  - lock: as before.
  - next act: nothing changed.

Made, microbatch × an act with a :class fact on the layer's own entity
  - answer: a different class: refused, not decided. The same class, or no value: OPEN POINT 4.
  - class: microbatch, as before, for the refused case.
  - lock: as before.
  - next act: nothing changed.

Made, microbatch × an act with a :class fact on an entity that is not the layer
  - answer: refused at check (f) (F8; X57).
  - class: microbatch, as before.
  - lock: as before.
  - next act: nothing changed.
```

### E2. A place (one entity and one plain key, in one layer that is made)

**States.** *Empty:* no fact was ever accepted for the place. *Holds a value:* the current fact F1 has
sealed bytes under lock K1, with stamp s1. *Holds a retract:* the current fact F1 has no value, with
stamp s1. There is no way back to empty.

**Transitions.**
- **T6.** Empty → holds a value.
- **T7.** Empty → holds a retract.
- **T8.** Holds a value → holds a later value (naming the current fact, or naming nothing).
- **T9.** Holds a value → holds a retract.
- **T10.** Holds a retract → holds a value.
- **T11.** Holds a retract → holds a later retract.
- **T12.** Any state → the same state: the act is refused.

In the rows, the new fact is F2 = `[act-id index]` of the act of the row, with stamp s2 > s1, and a new
value is sealed under lock K2, which the store made for this layer and which seals nothing yet. Fx is a
fact id that is not the current fact of the place. Each row takes the act's other checks as passed.

```
Empty × a value, naming nothing to replace
  - answer: accepted, stamp s2.
  - class: not changed by any write to a place.
  - lock: K2 gives the same bytes as before; it now seals F2 and can seal nothing else.
  - next act: the current fact of the place is F2. A later fact that names F2 passes (e); a later fact
    that names nothing passes (C rule 2); a later fact that names any other fact is stale.

Empty × a value, with :replaces (any fact id)
  - answer: refused as stale: nothing is current in an empty place (X62).
  - class: not changed.
  - lock: K2 gives its bytes. Whether K2 can still seal a value: OPEN POINT 6.
  - next act: the place is empty.

Empty × a retract, naming nothing to replace
  - answer: accepted, stamp s2 (C rule 2; X63).
  - class: not changed.
  - lock: no lock is named; none changes.
  - next act: the current fact of the place is F2, a retract (C rule 3). The place is not empty.

Empty × a retract, with :replaces (any fact id)
  - answer: refused as stale (X62).
  - class: not changed.
  - lock: none named; none changes.
  - next act: the place is empty.

Holds a value × a value, naming nothing to replace
  - answer: accepted, stamp s2 (C rule 2; X60).
  - class: not changed.
  - lock: K2 now seals F2. K1 gives the same bytes as before; it still seals F1 and can seal nothing
    else; F1 is no longer current.
  - next act: the current fact is F2. A later fact that names F1 is stale.

Holds a value × a value, :replaces F1
  - answer: accepted, stamp s2 (X61).
  - class: not changed.
  - lock: K2 now seals F2. K1 as in the row above.
  - next act: the current fact is F2. A second act that also names F1 is stale.

Holds a value × a value, :replaces Fx
  - answer: refused as stale (X62).
  - class: not changed.
  - lock: K1 as before, sealing F1, which is current. K2: OPEN POINT 6.
  - next act: the current fact is F1, as before.

Holds a value × a retract, naming nothing to replace
  - answer: accepted, stamp s2 (C rules 2 and 3).
  - class: not changed.
  - lock: K1 gives the same bytes; it still seals F1; F1 is no longer current. No lock is destroyed:
    a retract is not a forget.
  - next act: the current fact is F2, a retract.

Holds a value × a retract, :replaces F1
  - answer: accepted, stamp s2.
  - class: not changed.
  - lock: K1 as in the row above.
  - next act: the current fact is F2, a retract.

Holds a value × a retract, :replaces Fx
  - answer: refused as stale.
  - class: not changed.
  - lock: K1 as before; none changes.
  - next act: the current fact is F1, as before.

Holds a retract × a value, naming nothing to replace
  - answer: accepted, stamp s2 (C rule 2; X65).
  - class: not changed.
  - lock: K2 now seals F2. The retract F1 had no lock.
  - next act: the current fact is F2.

Holds a retract × a value, :replaces F1
  - answer: accepted, stamp s2 (C rule 3: a later fact can replace a retract; X65).
  - class: not changed.
  - lock: K2 now seals F2.
  - next act: the current fact is F2.

Holds a retract × a value, :replaces Fx (for example the value that the retract replaced)
  - answer: refused as stale: the current fact is the retract, not the value before it.
  - class: not changed.
  - lock: K2: OPEN POINT 6.
  - next act: the current fact is the retract F1, as before.

Holds a retract × a retract, naming nothing to replace
  - answer: accepted, stamp s2 (X64).
  - class: not changed.
  - lock: none named; none changes.
  - next act: the current fact is F2, the later retract.

Holds a retract × a retract, :replaces F1
  - answer: accepted, stamp s2.
  - class: not changed.
  - lock: none named; none changes.
  - next act: the current fact is F2, the later retract.

Holds a retract × a retract, :replaces Fx
  - answer: refused as stale.
  - class: not changed.
  - lock: none named; none changes.
  - next act: the current fact is F1, as before.
```

Two facts of one act on one place, in each state of the place: **OPEN POINT 3**.

### E3. A permission (in one layer that is made, or being made)

**States.** *Not held:* the record of the layer does not hold it. *Standing:* the record holds it, and
each permission above it in its chain. Not built: *ended* (who may end one is not decided); the record
leaves room for it.

**Transitions.**
- **T13.** Not held → standing: it is the first permission that a making act writes.
- **T14.** Not held → standing: the holder of the permission above it gives it.
- **T15.** Not held → not held: a give that is refused.
- **T16.** Standing → standing: every act; a retract is refused, and nothing ends one in this build.

In the rows, P is the permission, H its holder, and A the permission above it (none when P is a first
permission).

```
Not held × the making act of the layer names P as the first permission
  - answer: accepted (the row "Not made × a making act that is right" of E1).
  - class: the class that the making act wrote.
  - lock: nothing; no lock is made.
  - next act: P stands. An act under P by H passes (d). An act under P by another :who does not.

Not held × an act under A, by A's holder, with a fact under :permission that writes P beneath A
  - answer: accepted (X81).
  - class: not changed by any write of a permission.
  - lock: none named or made by the permission fact; none changes.
  - next act: P stands, from the moment the answer is visible. An act under P by H passes (d). H can
    give a permission beneath P, unless P is four deep (X79).

Not held × a give of P that a check refuses (the giver does not hold A; P is not straight beneath the
giver's own permission; P's layer is not the act's; P has nothing above it in a made layer)
  - answer: refused, at (d) or at (g) (X76, X82, X83).
  - class: not changed.
  - lock: none changes.
  - next act: P is not held. An act under P is refused at (d).

Not held × a fact under :permission with no value, on the place where P would be
  - answer: refused: a retract of a permission is not decided (X87).
  - class: not changed.
  - lock: none changes.
  - next act: P is not held.

Not held × an act under P (by anyone)
  - answer: refused at (d): the record does not hold P (X75). When the act is a right making act that
    names P as its first permission, it is the first row.
  - class: not changed.
  - lock: none changes. A request for locks under P makes no lock (X92).
  - next act: P is not held.

Standing × an act under P by H
  - answer: check (d) passes; the other checks decide.
  - class: not changed.
  - lock: as the row of E4 for what the act does.
  - next act: P stands, as before.

Standing × an act under P by a :who that is not H
  - answer: refused at (d) (E4 A; X76).
  - class: not changed.
  - lock: none changes.
  - next act: P stands, as before.

Standing × an act under P by H, with a fact under :permission that writes a permission beneath P
  - answer: accepted, unless P is four deep (X79) or another check fails.
  - class: not changed.
  - lock: none changes.
  - next act: P stands; the new permission stands beneath it.

Standing × a give of P again, or another permission fact on the place of P's fact
  - answer: OPEN POINT 4.
  - class: not changed.
  - lock: none changes.
  - next act: OPEN POINT 4: whether P still stands when another fact is the current fact of its place.

Standing × a fact under :permission with no value, on the place of P's fact
  - answer: refused: a retract of a permission is not decided (X87).
  - class: not changed.
  - lock: none changes.
  - next act: P stands, as before. Each permission beneath P stands, as before.
```

### E4. A lock

**States.** *Not made:* the store never made a lock with this id. *Made, not used:* made for layer L on
a request; it seals nothing. *Seals a current value:* it seals fact F, and F is the current fact of its
place. *Seals a value that is no longer current:* F was replaced or retracted; the lock is kept. Not
built: *destroyed* (forget's step); the record leaves room for it.

**Transitions.**
- **T17.** Not made → made, not used: a request for locks is accepted.
- **T18.** Made, not used → seals a current value: an act with a value under it is accepted.
- **T19.** Seals a current value → seals a value that is no longer current: a later fact lands on the
  place of its value.
- **T20.** Made, not used → made, not used, or → spent: a refused act named it (OPEN POINT 6).
- **T21.** Seals a value → the same: a second use is refused; a repeat of its own act is answered.

```
Not made × a request for locks in layer L is accepted, and this lock is one that it makes
  - answer (of the request): accepted, with a stamp.
  - class: not changed by any write of a lock.
  - lock: the bytes, from the moment the answer is visible; the same bytes at each later read.
  - next act: an act in L with one value sealed under this lock passes the checks of the seal.

Not made × a request for locks that is refused (X90 to X92)
  - answer: refused.
  - class: not changed.
  - lock: nothing.
  - next act: the lock is not made; a value under its id is refused.

Not made × an act with a value sealed under this lock id
  - answer: refused: the store did not make this lock for this layer (X68).
  - class: not changed.
  - lock: nothing. The refusal does not make the lock.
  - next act: the lock is not made.

Made, not used × an act in L with one value under it, and the act is accepted
  - answer: accepted.
  - class: not changed.
  - lock: the same bytes. It now seals that fact.
  - next act: any other act with a value under this lock is refused (X69).

Made, not used × an act in L with a value under it, and the act is refused for another reason
  - answer: refused, with the reason of the check that failed.
  - class: not changed.
  - lock: the same bytes.
  - next act: OPEN POINT 6: the lock can seal a value of a later act, or it is spent.

Made, not used × an act in another layer with a value under it
  - answer: refused: the store did not make this lock for that layer (X68).
  - class: not changed.
  - lock: the same bytes; still not used. Whether a forger can spend a lock this way is the same
    question as OPEN POINT 6.
  - next act: an act in L can still use it, unless OPEN POINT 6 says spent.

Made, not used × an act in L with two values under it
  - answer: refused: a second value under one lock (X70).
  - class: not changed.
  - lock: the same bytes.
  - next act: OPEN POINT 6.

Made, not used × the request that made it arrives again (a repeat)
  - answer: the recorded answer of the request, with its first stamp.
  - class: not changed.
  - lock: the same bytes. No lock is made.
  - next act: the lock is as it was.

Seals a current value × another act with a value under it (in any layer)
  - answer: refused: the lock already seals a value (X69), or it is not this layer's (X68).
  - class: not changed.
  - lock: the same bytes; it seals the same fact.
  - next act: the place of its value is not changed.

Seals a current value × the act that used it arrives again (a repeat)
  - answer: the recorded answer, accepted, with its first stamp. Check (a) answers before any check of
    the seal (C5).
  - class: not changed.
  - lock: the same bytes; it seals the same fact.
  - next act: nothing changed.

Seals a current value × a later fact (a value or a retract) is accepted on the place of its value
  - answer (of the later act): accepted.
  - class: not changed.
  - lock: the same bytes. It still seals the earlier fact, which is no longer current. It is not
    destroyed and not free.
  - next act: an act with a value under this lock is refused (X69).

Seals a value that is no longer current × an act with a value under it
  - answer: refused: the lock already seals a value (X69).
  - class: not changed.
  - lock: the same bytes.
  - next act: nothing changed.
```

### E5. The answer under an act-id

**States.** *None:* the record holds no answer under the act-id. *Accepted,* with a stamp and the
digest of its act. *Refused,* with a reason, a stamp and the digest of its thing.

**Transitions.**
- **T22.** None → accepted.
- **T23.** None → refused, with a reason.
- **T24.** Accepted or refused → the same: a repeat, a retry by Rama, a second and different act.

```
None × a thing with this act-id arrives and passes each check
  - answer: accepted, with a stamp; visible together with each fact, permission and lock that the act
    writes.
  - class: changed only when the act is a making act (E1).
  - lock: as E4 for what the act does.
  - next act: the act-id is used.

None × a thing with this act-id arrives and fails a check
  - answer: refused, with the reason of the first check that failed, and a stamp.
  - class: not changed.
  - lock: no lock is made. A lock that the thing named: OPEN POINT 6.
  - next act: the act-id is used. The same thing again reads this refusal (X103).

None × a thing arrives whose act-id cannot be used, or does not fit the act or the gate (X1 to X4)
  - answer: OPEN POINT 1. In no case does it throw, and in no case can one act-id get two answers.
  - class: not changed.
  - lock: no lock is made or changed.
  - next act: no place, permission or lock has changed.

Accepted × the same thing arrives again
  - answer: accepted, with the first stamp (X102).
  - class: not changed.
  - lock: as before; a lock that the act used is not "used twice".
  - next act: nothing changed; no fact of the act is written a second time.

Accepted × a different thing arrives under the act-id
  - answer: a reader that sent the first act still reads accepted, with the first stamp and the first
    digest. A reader that sent the second does not take that answer as its own (C rule 1): OPEN POINT 2
    for how it is told.
  - class: not changed.
  - lock: no lock is made or changed.
  - next act: no fact of the second thing is written. The act-id stays used by the first.

Refused × the same thing arrives again
  - answer: refused, with the first reason and the first stamp, also when the act would now pass
    (X103).
  - class: not changed.
  - lock: none changes.
  - next act: nothing changed.

Refused × a different thing arrives under the act-id
  - answer: a reader that sent the first thing still reads its refusal. The second is told that the
    act-id is used: OPEN POINT 2. The second is not decided, also when it is a good act (X105).
  - class: not changed.
  - lock: none changes.
  - next act: nothing of the second thing is written.
```

### E6. The stamp of a task

**States.** *None given:* the gate has decided nothing on the task. *Last stamp s.*

**Transitions.**
- **T25.** None given → the first stamp.
- **T26.** Last stamp s → a later stamp, at each decision, accepted or refused.

```
None given × the first decision on the task
  - answer: holds a stamp that has the clock time and a counter, below 2^62, later than each stamp
    that the act carries.
  - class: holds no stamp that a caller reads; not changed.
  - lock: holds no stamp that a caller reads; not changed.
  - next act: gets a later stamp.

Last stamp s × a decision, and the clock is ahead of s
  - answer: a stamp later than s that holds the clock time.
  - class: not changed.
  - lock: not changed.
  - next act: gets a later stamp.

Last stamp s × a decision, and the clock is at s or behind it (many acts in one tick; a clock that
stepped back; a new leader with a slower clock; a restart)
  - answer: a stamp later than s all the same (X17 to X19).
  - class: not changed.
  - lock: not changed.
  - next act: gets a later stamp.

Last stamp s × a decision for an act that carries a stamp later than s and later than the clock
  - answer: OPEN POINT 8: a stamp later than the carried one, or a refusal.
  - class: not changed.
  - lock: not changed.
  - next act: OPEN POINT 8: when the gate follows the carried stamp, each later stamp of the task is
    later than it too.

Last stamp s × a microbatch act whose facts are on this task and on others
  - answer: one stamp for the act. On which tasks it is later than the last: OPEN POINT 9.
  - class: not changed.
  - lock: not changed.
  - next act: OPEN POINT 9.
```

## Open points for the build session

**Ruled.** The build session ruled on all twelve on 8 October 2026. The rulings are rules 10 to 21 of
"The builder's rules" in `../BUILD-store-module.md`, in the order of the points below: point 1 is rule
10, point 12 is rule 21. Where a point's text and its rule differ, the rule holds. The seven differences
between the contract and the README, listed after the points, are corrected in the contract.

Each is a thing that the build must handle and on which the contract and the README are silent. For
each: the cases, the ways it could go, and what each way would mean. This paper picks none. Where a
way would decide something that is Sid's, the text says so.

### OPEN POINT 1. A thing that a reader by act-id cannot find, or that could give one act-id two answers

*Cases:* a thing with no act-id or an act-id of the wrong shape (X1, X2); an act-id whose layer is not
the act's layer (X3); an act-id whose class is not the class of the gate where the thing arrived (X4);
the same act-id on both depots (X107). *What is fixed:* nothing throws (C1); one act-id never gets two
answers (C2).

- **(a) No answer under an act-id; the gate keeps a small sign of such things apart from the answers.**
  It means: each gate records answers only under act-ids of its own class whose layer is the act's
  layer, so C2 holds simply. A refusal is still data that someone can see. The sender reads "nothing"
  for ever and cannot tell it from "not yet". One more small thing in the record, with a word that the
  build makes (C rule 8).
- **(b) Nothing is recorded; the depot is the only trace.** It means: the least to build. "Each a
  recorded refusal word" (C item 3) is not kept for these things. Nobody learns of them without reading
  the depot.
- **(c) The gate where the thing arrived records a refusal under the act-id that the thing carries.**
  It means: the refusal is recorded by act-id. A reader goes by the act-id to another gate's or another
  layer's record, and does not find it unless the read looks in more than one place. A thing that is on
  both depots can get two answers under one act-id unless the two gates share their answers, and the
  plan must then show that safe when both decide at the same time. A forger who learns an act-id that a
  door has made and not yet sent can use it up from the wrong depot.

### OPEN POINT 2. How a second, different act under a used act-id is told so, and what the record keeps of it

*Cases:* X104, X105, X113. *What is fixed:* the first answer does not change (C2); the second sender
does not take it as its own (C rule 1); the gate tells the two apart by the digest.

- **(a) The record keeps the first answer only, with its digest. The read gives the digest with the
  answer, and the door compares it with the digest of what it sent.** It means: the gate writes nothing
  for the second act, so a forger adds nothing to the record. The door takes the digest in the same way
  as the gate, so the way of the digest is shared between the two. "Told so" is the door's comparison.
- **(b) The read takes the act-id and the digest of the reader's own thing, and the module answers
  "this act-id is used by another act".** It means: the same record as (a). The module tells, not the
  door. A reader that has no digest of its thing cannot ask.
- **(c) The gate also records, under the act-id, that a different thing came: its digest, or a count.**
  It means: "told so" is in the record and stays. Each forged thing adds to the record; without a
  bound, one act-id can grow without limit. The first answer stays beside it, unchanged.

In each way the streaming gate's ack may carry the same word as a signal (R 7-Oct 7); the record is the
truth.

### OPEN POINT 3. Two facts of one act on the same place

*Cases:* X66 (two facts, one entity, one key, in one act); X67 (a fact names in `:replaces` a fact of
its own act).

- **(a) The declaration refuses the act: an act names a place once.** It means: the simplest rule; one
  act gives one fact id for a place. A maker that wants "set, then change" sends two acts. One more
  refusal, with a word that the build makes.
- **(b) In order: each fact sees the facts of its own act that come before it; the last is current.**
  It means: a later fact may name an earlier fact of its own act. The earlier facts of the act are
  never current for any reader, and their locks seal values that were never current. The gate checks
  inside the act as well as against the record.
- **(c) Each fact is checked against the record as it was before the act; the last one is current.** It
  means: two facts that both replace the same current fact both pass, which reads against "each
  `:replaces` is the current fact". A fact that names an earlier fact of its own act is stale.

### OPEN POINT 4. Store-key facts in states that the contract does not name

*Cases:* (i) the same permission is given a second time (X86); (ii) a second permission fact lands on
the place of an earlier one, so the earlier is no longer the current fact of its place (X86); (iii) a
`:class` fact in a made layer that says the class the layer has, or has no value (X56); (iv) a fact
under `:lock-request` with no value (X100). *Under them, one question:* does "the record holds it"
(C item 3(d)) mean that the permission was written once, or that it is the current fact of a place?
*What is fixed:* a retract of a permission and a change of class are refused as not decided (C item 3).

- **(a) Refuse each, with the word that says it is not decided.** It means: in this build a store key
  takes one write for a place, and a permission keeps its place for ever. Nothing here decides who may
  end a permission or change a class. The form of a permission fact (which entity it sits on) then
  decides how many permissions one holder can have in one layer; the builder says it.
- **(b) Take each as a plain fact under rules 2 and 3: accepted, and the later fact is current.** It
  means: for (ii) the build must say whether the earlier permission still stands. If it does not, an
  ordinary write has ended a permission, and who may end one is Sid's and not decided. If it does,
  "the record holds it" means "was written once", and the current fact of the place does not say
  which permissions stand.
- **(c) Refuse (ii) and the two retracts; accept (i) and the same-class fact as changes of nothing.**
  It means: a careful sender that gives a permission twice does no harm and gets no refusal. More
  cases in the gate.

### OPEN POINT 5. What the gate holds of a permission id and of `:who`, beyond the contract's words

*Cases:* (i) the `granted-in` slot (X84): check (g) says "in the act's layer", and the base's first
permission is `[:system :base :base]`; the sources do not say what `granted-in` must be in the first
permission of another layer, or in a permission given beneath. (ii) `:system` as the holder of the
first permission of a layer that is not the base (X55). (iii) A session or an agent as the holder of a
first permission: each is an id, and the gate cannot tell it from a person. (iv) `:who :system` from a
sender that is not the system (X54). *What is fixed:* the layer slot is the act's layer (act.clj); the
holder is the act's `:who`, but for a first act (C item 3(d)).

- **(a) The gate checks what the contract's words say, and no more.** `granted-in` is free; any id can
  be the top of a layer that is not the base. It means: the least code. `granted-in` is a claim that
  nothing holds true. E3·H7 B ("a person, or the system for the base, and never a session") is kept
  later by sign-in and the door, not by the gate.
- **(b) The gate also holds `granted-in` to the act's layer, and refuses `:system` at the top of any
  layer but the base.** It means: two more refusals, with words that the build makes. Each permission
  id of this build then has the same layer in both slots, so the slot says nothing until a permission
  can be granted from another layer. It reads E3·H7 B onto the gate as far as the gate can see.
- **(c) The gate refuses `:system` at the top of a layer that is not the base, and leaves `granted-in`
  free.**

In each way the gate cannot check (iii) or (iv) in this build; they wait for sign-in (C "Not in this
build"). Until then any sender can make any layer that is not made, for any holder. Sid should know
that this is so, whichever way is taken.

### OPEN POINT 6. A lock that a refused act named

*Cases:* X71; the rows of E4 "Made, not used × … refused"; a forger that names another layer's lock.
*What is fixed:* a lock seals one value at most (C item 7); a repeat of an accepted act is not a second
use (C5).

- **(a) Free: the lock seals nothing, so a later act can use it.** It means: a refusal changes the
  answer and nothing else (C3 stays simple). The depot then holds two sealed values under one lock, the
  refused one and the later one; a forget of the later value loses the refused one too, which harms
  nothing. The door must not use one lock twice with the same random part (the cipher's rule), or it
  takes another lock for the next try.
- **(b) Spent: a lock that any thing named can seal nothing later.** It means: one lock, one sealed
  value in the depot, ever. A refusal now writes to a lock. Each new try of a refused act needs new
  locks. A forger who learns lock ids can spend the locks of a layer, also from another layer.
- **(c) Spent only when the refused act was in the lock's own layer and its permission stood.** It
  means: the forger's way is closed. More cases in the gate.

### OPEN POINT 7. The reach of the seal

Two questions.

*Does the gate open a sealed value, to check that the bytes are what its lock gives (X72)?*

- **(a) No. The gate checks the shape of the seal and the standing of the lock: the contract's three
  refusals.** It means: no plain value is ever in the gate's memory (R 6-Oct 3: the seal is below the
  form, and the door seals). A forger or a broken door can write a fact that nobody can open.
- **(b) Yes. The gate checks the bytes against the lock.** It means: the gate opens each value, at a
  cost for each, and plain values pass through the module's memory. A fact in the record can always be
  opened.

*Who may read a lock's bytes (X115)?*

- **(a) Any caller of the module, in this build; the door proper is the guard, later.** It means:
  nothing to build. The seal protects the depot and the disk; it does not protect against a caller of
  the cluster.
- **(b) The read needs something that only the asker has, for example the act-id of its request.** It
  means: a handle that cannot be guessed, now. It is a small rule about reads, and reads are not
  decided (C "Not in this build", group 6).

### OPEN POINT 8. An act that carries a stamp ahead of the gate's clock

*Cases:* X20, X21, X74. *What is fixed:* the gate's stamp is later than each carried stamp and below
2^62 (C item 6); nothing throws (C1). When no stamp below 2^62 is later than the carried one, the act
is refused, in each way below.

- **(a) Follow it: the gate's stamp is later than the carried one, whatever it is.** It means: what the
  contract's words say. One forged act with a stamp near 2^62 moves the stamps of its task to the end
  of the range. The task can give no stamp after that, so each later act on it is refused for ever.
  The plan says which stamp the answer of such a refusal holds.
- **(b) Refuse an act whose carried stamp is more than a bound ahead of the gate's clock.** It means: a
  number that the build makes, Sid's to change. An honest act carries stamps that a gate gave, so they
  are near some gate's clock; the clocks of two nodes differ, so the bound is wider than that
  difference.
- **(c) Refuse an act whose `:based-on` names a fact that the record does not hold with that stamp.**
  It means: the strongest. It is a read of the record for each entry of `:based-on`, which can be in
  another layer and on another task. It comes close to `resolve`, which is not in this build.

### OPEN POINT 9. The one stamp of a microbatch act that writes on several tasks

*Case:* X22. *What is fixed:* the answer holds one stamp; each current fact holds "the stamp"; a stamp
is strictly increasing on its task (C items 5 and 6).

- **(a) One stamp for the act, later than the last stamp of each task on which the act writes.** It
  means: "strictly increasing on its task" is true on each task, for each fact. The tasks of one batch
  agree on stamps.
- **(b) One stamp for the act, increasing on the one task that decides the act or keeps its answer.**
  It means: on the other tasks, two facts can hold stamps that are not in the order in which the facts
  were written.
- **(c) A stamp for each task's part of the act, and one for the answer.** It means: the order on each
  task is exact. One act has several stamps; the stamp of the answer is not the stamp of each fact;
  `:based-on` carries the fact's.

### OPEN POINT 10. What "recomputable from the acts depots" reaches

*Cases:* X42; C rules 5 and 9; F2 A (R 7-Oct 2). As the contract stands, two parts of the record cannot
come out of the depots: a lock's bytes (C rule 5: never appended) and a stamp (it holds the gate's
clock time at the decision). And rule 9's question, the winner when two microbatch acts on different
entities meet, is to find out.

- **(a) "Recomputable" reaches the decisions and the current facts.** Locks are kept through a rebuild
  as they are, with what each seals. The gate then takes "this lock seals this same fact" as the same
  act and not as a second use (C rule 5 says as much). Stamps are new at a rebuild. It means: a stamp
  that an old act carries in `:based-on` names a stamp that the rebuilt record no longer holds.
- **(b) As (a), and the stamps come out the same too.** It means: the time in a stamp cannot be read
  from the gate's clock at the decision alone; it comes from something that the depot holds. That
  changes where H2 A's "clock time" is read from, which is Sid's row.
- **(c) A rebuild is not a requirement of this build.** F2 A is the reason for having no second depot
  of decisions; the plan says what it found for rule 9 and builds nothing for a rebuild. It means:
  nothing to test for a rebuild now. The three findings (locks, stamps, the winner across entities) are
  written down for the day a rebuild is asked for.

### OPEN POINT 11. Limits that the form does not give

*Case:* X15: the number of facts of one act; the number of entries of `:based-on`; the bytes of one
sealed value. (The number of locks of one request needs a limit in each way; see O6.)

- **(a) The door's declaration gives limits; a thing over one is refused with a word.** It means:
  numbers that the build makes, kept and not decided, like the 256 and the 32 of `act.clj`. The work
  of one act has a bound.
- **(b) No limits; the gate gives the task back during a long act.** It means: nothing is refused for
  its size. One very large act holds its layer's task (streaming) or its batch (microbatch) for its
  length, and anything can append one.
- **(c) Limits in the door only.** It means: the gate is the authority (R 7-Oct 8), so a sender that
  goes round the door has no limit. For a forger this is (b).

### OPEN POINT 12. F9's three limits

R 7-Oct 9 and C:61 say that "the three limits" are targets and that the rig's measures are the
starting point. Neither file says which three they are, or gives a number. This stage did not read the
rig. The latency lines of this paper come from F4 C's heuristic alone.

- **(a) The build session gives the plan the three limits and their starting numbers, as findings.**
  It means: the plan can say what it aims at, and the check of the plan can hold it to that.
- **(b) The plan goes on without numbers and says so.** It means: the classes of this paper
  ("single-digit milliseconds", "hundreds of milliseconds") stand in, and no number binds the plan.

## Where the contract and the README differ

The README wins (the caller's rule). `C:n` is a line of
`src/proposal/backend-on-rama-2026-10-02/BUILD-store-module.md`; `R:n` is a line of
`src-store/README.md`.

1. **Three values are given as decisions; the README says they are not decided.** C:45–46 lists under
   "From the act (6 Oct)": a permission "≤4 deep", "no value = a retract", and a carried stamp "≥ 0,
   < 2^62". The README says of each "kept from today's code, not decided": the retract at R:78–79, the
   four levels at R:87, the 2^62 at R:98–100; and "not below zero" is "closed by a session …
   waiting for his word" at R:100–102. This paper uses the values and holds them as kept, not decided.
2. **"Act-id" is given as Sid's word; the README says a session wrote it.** C:77 has it under "Words
   (Sid's)". R:64–66 and R:147–150: Sid said "act-uuid", "a session wrote act-id", "the word is his to
   change" (also act.clj:60–62).
3. **"One row is open."** C:9–10: "Nothing is left to ask before the build. One row is open … H1."
   The README has two more things that wait: the carried stamp's "not below zero", "waiting for his
   word" (R:100–102), and 6c, "open only if he strikes 6b" (R:38–39). Neither changes this build.
4. **The other answer of H1.** C:118–119 and C:127–128 leave room for a switch to "one lock per act".
   R:198 names the other answer as "one for each session and layer". The room that the record leaves
   should be for the README's alternative too.
5. **The first act of a personal layer against "each actor acts under its own permission".** R:183–184
   (E4 A) has no exception. C:99–101 and C:134–137 make one: at the first act of a personal layer the
   `:who` is `:system`, and the permission that the act names is the person's. R:173–174 says only that
   the system makes the layer and the permission; it does not say under which permission the system
   acts when it does.
6. **"A gate reads on its own task" is left out of the contract's F5 line.** R:129–131 has it, with
   Sid's cost reason. C:54–55 keeps the copy of each grammar and drops the clause. It bears on the
   microbatch gate, whose layer is on several tasks (O2).
7. **A word only.** C:60 says the gate refuses a setting key "as a recorded refusal"; R:140–141 says "as
   a recorded decision". The meaning is the same.

**In the contract and not in the README** (no conflict; said so that nobody reads them as Sid's):
the code words `:system` and `:base` (C:64–65; the README has "system" and "the base" as words,
R:164–168 and R:123); the base's first permission `[:system :base :base]` (C:133–134); the word
`:not-sealed` (C:126).

**Tensions that are in both files** (not a difference between them):

- *F1 A against F4 C and the open re-class.* "The members of a layer set its class" (R:120–122; C:52).
  In this build F4 C gives the class at the making, and each later change is refused as not decided
  (C:105–106). So in this build no member sets a class; F1 A is kept only in "as a fact on the layer".
- *F2 A against C rule 5 and H2 A.* "The record is recomputable from the acts depot" (R:116–119;
  C:50–51). A lock's bytes are never on a depot (C:172–174), and a stamp holds the gate's clock time
  (R:187–188). See OPEN POINT 10.
