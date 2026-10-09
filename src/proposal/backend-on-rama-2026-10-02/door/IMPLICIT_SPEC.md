# Implicit Spec

<!-- Phase 0. Fill in before starting Phase 1 (Plan). After completing this, fill in PLAN.md. -->

**What this paper is.** The implied spec of the door: what the door's contract implies and does not
write out. It lists each call of the door, what must hold for it, each state that a call can meet,
and each place where the contract's words cannot all hold.
**For which contract.** `src/proposal/backend-on-rama-2026-10-02/BUILD-door.md`, as amended on
9 October 2026 (head `5ebb010f`; the rename `c296a459` is in the log). "The contract" below is that
file. "Part 1" to "part 7" are the numbered parts of its "What to build". "D1" to "D8" are its
builder's rules. "Rule 5, 11, 15, 16, 20" are the store's rules in `BUILD-store-module.md`, which the
contract says hold where they touch the door.
**The stage.** The first of the Rama skill's stages, Phase 0 (`phase-0-implicit-spec.md`). It decides
nothing and designs nothing.
**Who wrote it.** One fresh session (model Opus 5.5, effort xhigh), 9 October 2026, for the build
session "door build" to judge. It wrote this one file and changed no other.

**What was read, and what was run.** Each claim about the module as built was read in the source at
the lines given. Nothing was run by this session: no proof, no probe. The three count lines (act 150,
pure 449, module 227) are the build session's run, as `door/PROGRESS.md` records it. Where a claim
rests on a check of the module's proof, the check was read and not run, and the line says "the proof's
check at". Where a claim rests on Rama's own documentation, it names the local copy under
`reference/rama/docs/`, which records its extraction on 22 March 2026 and no version; the module runs
on Rama 1.6.0.

## How the skill's template is adapted

The template is written for a Rama module. The door is not a module. It is a client of one: it has
no depot, no PState and no topology of its own, and it owns no durable state (part 1). So:

- **"Operations"** are the door's own calls, as "What to build" gives them: opening the door (part
  1); an act-id for a layer, ahead of sending (part 2); asking for locks (part 3); seal and send
  (part 4); the answer back, with its bounded wait (part 5). A sixth entry holds what the contract
  implies beside those five and names in no part of its own.
- **Each field of the template is filled for each operation**, tied to a call of the door or to what
  the module does with what the door sends. "Latency" counts the round trips to the cluster and the
  reads on a task that one call costs, since the door has no storage of its own. "Data growth" has
  two homes: the memory of the door's process, and the rows and depot records that the door's calls
  leave in the cluster for good.
- **The matrix** covers each thing whose state the door's calls change or depend on: a layer, an
  act-id, a lock, a request for locks, the door's memory of one process, and a depot's log. Its
  "writes" are the door's calls, and one event that is no call: a restart of the process. Its
  "reads" are what a maker can learn through the door, what the door itself reads, and what an
  operator or a proof reads round the door.
- **Requirements, not designs.** A line here says what must be true, with the contract's clause or
  the source line that makes it so. It names no function, data shape or thread as a choice. Two
  names are the contract's own and are used as such: `act-id-for` (part 2) and `answer-of` (part 5).
  A name in backticks with a file is a thing that exists in the tree today.
- **Open points are not requirements.** Where the contract is silent, cannot be met, or disagrees
  with itself or with the module, the point is under "Open points for the build session", marked
  `P1` to `P22`. A requirement that waits on one says so. Where a point would need a decision of
  Sid's, the point names the row of the queue and stops.

Words. "Act", "maker", "door", "gate", "depot", "act-id" as the contract defines them. "Key" is a
fact's key. "Lock" is an encryption key. "Thing" is what is appended to a depot, as the tree says it.
"The stock" is the door's memory of locks that it has read and not used (part 1, part 3).

## Operations

### What the module gives the door, as built

Each line is read in the source. The numbers `G1` to `G26` are used below.

- **G1. Two depots.** `*streaming-depot` partitions by the hash of the thing's `:layer`;
  `*microbatch-depot` by the hash of the entity of the thing's first fact
  (`src-store/softland/store/module.clj:73-74`; `gate/batch.clj:85-97`). Both partitioners run on
  the appending client (`module.clj:57-58`). Neither depot is ever trimmed, and nothing secret goes
  on one (`module.clj:33-37`).
- **G2. The two acks.** With the level `:ack`, an append to the streaming depot returns when the gate
  has processed the thing and its rows are visible. An ack can fail for an act that the gate
  accepted. An ack of the microbatch depot says that the depot holds the thing and nothing about the
  decision (`module.clj:60-65`). The gate gives nothing back in the ack
  (`module/streaming_gate.clj:99-100`). The docstring names no level for the microbatch depot; the
  proof's hand uses `:append-ack` (`test-store/softland/store/module_proof.clj:304-312`).
- **G3. What an append that throws means, by Rama's documentation.** "An exception doesn't mean the
  append did not go through – it just means it didn't go through cleanly"
  (`reference/rama/docs/14-depots.md:212`). With `:ack`, a first attempt of the stream topology that
  fails gives the client an exception also when a retry then succeeds (`14-depots.md:214`). When a
  task's limit of pending records is hit, an `:ack` append throws and "the appends will have gone
  through" (`reference/rama/docs/11-stream-topologies.md:297`). The rig ran one case of it: the
  acked append threw on the client when the worker died, and the record was processed after the
  restart (`src/proposal/rig-2026-09-25/RIG.md:2177-2180`, a finding; no proof of this tree reaches
  it, `test-store/softland/store/README.md:304`).
- **G4. Three queries.** `answer-by-act-id [routing-id act-id]` gives
  `{:verdict :reason :stamp :digest}` or nil; nil is "nothing yet" and does not mean refused.
  `locks-of-request [routing-id request-act-id from to]` gives a map from index to `{:bytes :way}`,
  empty when the request made no lock or its answer is not yet readable. `depot-of-layer [layer]`
  gives the layer's depot, or nil for a layer that is not made (`module/door_reads.clj:9-28`).
- **G5. The routing id.** The first argument of the first two queries is the layer of a streaming
  act-id and the uuid of a microbatch act-id. The one function that says so is in the record's
  rows (`record/rows.clj:312-321`). Rama's client hashes the first argument, so each query goes to
  one task and never hops (`door_reads.clj:54-62`).
- **G6. The head of each query** tests what the caller gave before any key is built: an id over the
  limit, a thing that is no act-id, or a routing id that is not the one of the act-id's depot costs
  no read and gets nil or an empty map (`door_reads.clj:64-71`; `rows.clj:273-344`). The tests run
  on the task, after the client has hashed the first argument; "a caller that may hold a thing that
  is no id tests it with the same functions before it calls" (`door_reads.clj:69-71`). The proof's
  hand does (`module_proof.clj:125-153`). A read whose first argument Rama's client cannot hash is
  reached by no proof (`store-module/FULL_SPEC_REVIEW.md:251`).
- **G7. A stray gets no answer.** A thing that is no plain map or holds no act-id; whose act-id is
  not `[layer depot uuid]`; whose act-id's layer prints to more than 256 characters; whose act-id's
  depot is not the depot where it arrived; or whose act-id's layer is not its own `:layer`, which
  covers a thing with no `:layer` (`door/declaration.clj:237-246`, `453-487`). The gate keeps the
  stray's digest under its word and ends (`streaming_gate.clj:33-36`; `microbatch_gate.clj:49-53`).
  No read by act-id ever gives anything for it.
- **G8. Check (a).** A gate reads the answer under the thing's act-id first. If one is there, it
  writes nothing, whether the thing is the same act again or a different one
  (`streaming_gate.clj:37-41`; `microbatch_gate.clj:60-61`). One act-id has one answer.
- **G9. The declaration** is the check of what arrived: the maker's ten parts in `act.clj`'s order,
  `:act-id`, `:depot`, and for each fact `:lock-id` and `:sealed`
  (`declaration.clj:183-196`, `215-235`, `526-549`). Its limits: 256 facts, 1,024 entries of
  `:based-on`, 65,536 sealed bytes, 256 locks in a request, 256 characters in an id's text
  (`declaration.clj:61-86`; rule 20).
- **G10. The store keys are three**, each in the namespace `softland.store`; a fact under one keeps a
  plain value and carries no seal; a fact under any other key in that namespace is refused
  `:store-key-not-known`, whatever it holds (`declaration.clj:122-147`, `208-214`, `372-380`).
- **G11. A value under a plain key must arrive sealed**: `:not-sealed`, `:sealed-with-a-plain-value`,
  `:seal-incomplete`, `:sealed-not-bytes`, `:sealed-value-too-large`, `:lock-named-twice`
  (`declaration.clj:411-446`).
- **G12. What the gate checks of a seal.** The lock is one that the store made for this layer and
  this depot; it seals nothing yet; its way is known; it opens the sealed bytes at the fact's place
  (`gate/decision.clj:292-309`). The gate "does not check that what a lock opens is a value of the
  form" (`decision.clj:326-327`; `src-store/softland/store/README.md:165-167`). It does not check
  who asked for the lock: the proof's check at `module_proof.clj:827-828`, `854` has another holder
  of the layer seal under a lock that Alice asked for, and it is accepted.
- **G13. A refusal writes the answer and the task's last stamp, and nothing else.** "A lock that a
  refused act named stays as it was, free" (`decision.clj:388-391`; rule 15). The proof's check at
  `module_proof.clj:807-813`, `833-837` uses one lock in three refused acts and then in an accepted
  one.
- **G14. A request for locks** is an act with one fact under `:softland.store/lock-request`, on the
  uuid of its own act-id, holding a count of 1 to 256. The id of a lock is `[request act-id, index]`.
  A gate makes the bytes at the write of an accepted request (`README.md:294-299`;
  `declaration.clj:395-409`; `decision.clj:366-368`). A request sent twice makes no lock again
  (rule 5; the proof's check at `module_proof.clj:823-826`, `846-847`).
- **G15. What the read of locks gives.** In a streaming layer a lock is given with its bytes in each
  state, so the read does not say whether a lock seals a value. In a microbatch layer the read
  gives locks only for an accepted request whose batch is closed, and a lock that seals a value
  comes with nil for its bytes (`door_reads.clj:16-26`, `113-147`; `rows.clj:236-269`).
- **G16. A layer's depot does not change in this build.** `depot-at-making` gives `:microbatch` for
  the base and `:streaming` for anything else, and it gives a depot for whatever it is handed
  (`gate/first_act.clj:49-54`). A depot fact after the making is refused (`decision.clj:231-232`).
  So "no act of this build changes a depot" (`door_reads.clj:60-62`), and when `depot-of-layer`
  gives a depot it is the one that `depot-at-making` gives.
- **G17. Closing.** A read gives a row of a microbatch layer only when its batch is closed: one
  short run of the gate after the batch (`door_reads.clj:40-52`; `README.md:374-389`). In the
  proof's runs on the in-process cluster an answer was readable 873 to 893 ms after its append
  (`store-module/PROGRESS.md:175-180`); those timings say nothing about production
  (`test-store/softland/store/README.md:306-307`).
- **G18. The seal.** `seal` takes a lock, the place `[layer entity key]` and plain bytes; it gives
  the sealed bytes, 28 more than the plain bytes, or nil when the lock's way is not known or its
  bytes are no lock. Each call takes a fresh random part (`door/seal.clj:26-36`, `118-135`).
  "Which plain bytes stand for a value: the door's. The proof's hand seals the UTF-8 of the one text
  of the value" (`seal.clj:51-52`), in its normal form (`module_proof.clj:224-228`).
- **G19. The one text and the digest.** For a value of the form the one text is EDN and reads back.
  For anything else it is a mark: `#other` and the name of a Java type, `#too-deep`, `#unreadable`,
  `#keyword`, `#double` (`door/text.clj:18-57`). The digest is SHA-256 over the one text of the
  thing as it arrived, with its sealed bytes; a caller gets it as 64 hex characters
  (`door/digest.clj:18-24`, `55-68`).
- **G20. The maker's form.** `parse` refuses an act that carries an act-id or a depot, and a fact
  that carries a seal, a lock id, a layer or a mark, with `:unknown-part`; a value that `value?`
  does not take, with `:malformed` (`act.clj:244-269`, `476-509`).
- **G21. The order of a layer's acts.** In a streaming layer the depot's order, without a failure
  (`streaming_gate.clj:86-90`). In a microbatch layer two acts on two depot partitions have no
  order in the depot; the gate gives each batch one, the turn, "a fixed rule with no other meaning"
  (`module.clj:48-55`; `README.md:359-365`).
- **G22. What one act costs a gate.** Streaming: one accepted act with one sealed value is 4 reads
  and 4 row writes in one event on the layer's task (`README.md:276-278`). Microbatch: 3 reads, 4
  row writes and 6 messages between tasks (`README.md:404-410`). The aims, none measured: 1,000
  acts a second on one task; 20 ms from the door's append to the answer in the door's hand; for the
  microbatch gate an answer within two runs of the gate (`README.md:493-504`).
- **G23. What a lock costs.** About 140 bytes of rows for each value in a streaming layer and about
  245 in the base, by arithmetic and not by a measure (`store-module/PROGRESS.md:119-126`, `169-172`;
  `decisions/groups/g-door.txt:93-95`).
- **G24. The PStates are not private.** "Any caller of the module can read a lock's bytes in this
  build; the door proper is the guard, later" (`door_reads.clj:73-76`).
- **G25. A start of the module again keeps the record.** The proof's check at
  `module_proof.clj:1478-1501`: after Rama's module update the same handles read the same answer
  and the same lock bytes, and a lock that was asked for before seals a value after.
- **G26. Every task has one thread.** A gate's event, a query and a select on one task run one after
  another (`.claude/skills/rama/SKILL.md`, "Every task is single-threaded"). The streaming gate's
  event does not give the task back (`streaming_gate.clj:68-73`).

### O1. Opening the door

Part 1: "The door is opened on a cluster handle and the module's name, and holds the two depots'
handles and the three queries' handles. It owns no durable state. Its one memory is a stock of locks
(part 3), the memory of one process." D8: the door is the module's only client in the runtime.

- **Latency.** Seconds are acceptable. Opening is once for a process and is on no act's way.
  Rama's guidance is to make each handle once and use it again
  (`.claude/skills/rama/references/foreign-client.md:243`).
- **Throughput.** One opening for each process of the runtime (D8). Nothing in the contract drives
  a second.
- **Invariants.**
  - O1.1 An opened door holds the two depots and the three queries of G1 and G4, and no other handle
    of the module. It holds no handle of a PState, although the module leaves them readable (G24):
    "it uses the module's surface only, two depots and three queries" ("What to build").
  - O1.2 The door owns no durable state: nothing that it knows outlives its process (part 1; D3).
  - O1.3 The door's one memory is the stock of locks. Whatever else the door must remember to work
    the stock (the act-id of a request that it sent and whose locks it has not read) is part of that
    memory and of nothing else; it is not a second memory of what a maker sent (part 5: "The door
    keeps nothing of what it sent once the call returns").
  - O1.4 A door that is opened holds an empty stock, whatever the record holds from a process
    before (D3: "a restart asks again").
  - O1.5 The module's name is the one that Rama gives the module (`module.clj:25`). A door opened on
    a name under which no module runs tells its opener so in the door's own words (O6.1), and no
    call is made on it.
- **Data growth and scale.** The memory of the process only. The stock holds at most the plan's
  number of unused locks for each layer and who (part 3), 32 bytes and an id each. The number of
  pairs of a layer and a who that one process meets has no bound in the contract, so the door's
  memory grows with the pairs that hold locks; what bounds it is the plan's to say, with its cost
  in rows that seal nothing (O3).
- **Concurrency.** Many makers use one opened door at once (part 1: "any maker in the runtime's
  process"). The contract does not say what holds when a process opens the door twice. What follows
  from the parts: two opened doors are two stocks, each with its own loss at a restart, and neither
  uses the other's locks. No row is wrong for it; rows are wasted. Whether the handles of Rama's
  client can be used from many threads at once was not settled by reading (see "What could not be
  settled by reading").
- **Edge cases.** The module is not running, or the name is wrong: O1.5. The cluster handle is
  closed later: each call then fails as "the door could not" (O6.1), never as one of the four
  answers. The module is started again under an opened door: the handles and the stock stay good
  (G25). The contract names no closing of the door: P22.

### O2. An act-id for a layer, ahead of sending

Part 2: "`act-id-for` a layer gives `[layer depot uuid]`: the layer's depot as the module gives it
(`depot-of-layer`), or the depot that the layer starts with when it is not made (`first_act.clj`),
and a random UUID. A maker may ask for the ids of a series before it sends the first, as `act.clj`
allows; the door keeps none of them. An act-id is made once and never reused." D2.

- **Latency.** One query for one call, to the layer's task: one seek for a streaming layer; for the
  base one or two, the depot fact's row and then `:closed-through` (`door_reads.clj:149-170`).
  Single-digit milliseconds are needed: a send that brings no act-id asks for one first, so this
  read is on the way of each such act.
- **Throughput.** As many as the acts that makers send, or more when a maker asks for ids that it
  never uses. Each read lands on the layer's own task, which has one thread that the streaming gate
  also needs (G26). By the cost of a cold read the gate's 4 reads give 480 to 770 acts a second on
  one task (`README.md:493-498`); a fifth read for each act, for its act-id, and a sixth, for its
  answer, are on the same thread. No number is measured. The plan says what one act costs the
  layer's task through the door, in reads, and holds it against the aim of 1,000.
- **Invariants.**
  - O2.1 The id is `[layer depot uuid]` in the shape that `act.clj` declares (`act.clj:162-164`):
    the layer as the maker gave it, one of the two depot words, a UUID.
  - O2.2 The depot is the one that `depot-of-layer` gives for the layer; when the read gives nil, it
    is the depot that the layer starts with (G16). The read is made: part 2 names it, and in this
    build the two always agree (G16), so the read is what leaves room for a layer whose depot can
    change (?9, not decided). The door's docstring says that room (the contract, "Not in this
    build": "Where the door's code must leave room for one, say so in the docstring").
  - O2.3 The uuid is random. It is made from nothing of the act, the maker, the person or the time
    (act 5: "the last a random UUID"). So an act-id written plain in a depot says nothing of who
    acted (the session's reading under ?4 A in `src-store/README.md:242-247`; K10 is open and is
    not decided here).
  - O2.4 The door never gives one act-id twice of its own making (D2), and keeps none (part 2; D2).
  - O2.5 An id that the door gave is usable by a maker before anything is sent: as `:because-of`,
    and as the act-id of a fact id `[act-id index]` in `:replaces` and `:based-on` of a later act of
    the series (`act.clj:147-148`, `160`, `166-168`). The index is the position of the fact in
    `:facts` as the maker gave them. So the door keeps each fact at its position (part 4: "never
    reorders facts"): the gate writes each fact with its index in the arrived order
    (`decision.clj:358-362`).
  - O2.6 The door remembers no layer's depot between two calls. Its one memory is the stock
    (part 1). A remembered depot would be a cache, which the project's rule sends to its own
    examination first (`CLAUDE.md`, "Systemic repair and caching").
- **Data growth and scale.** Nothing is kept: no memory, no row, no depot record. An id that is
  never sent leaves nothing anywhere.
- **Concurrency.** Any number of calls at once, for one layer or many. No two calls give one id.
  No call waits on another.
- **Edge cases.**
  - The layer is not made, or its making is in flight, or it is the base before its making is
    closed: the read gives nil and the id carries the depot that the layer starts with. The making
    act itself needs such an id.
  - The base, in the few milliseconds when its making act's answer is readable and its depot row is
    not yet (`README.md:179-184`): the same id, by G16.
  - The layer is nil, is no id (a string, a number, a keyword that does not read back), or prints
    to more than 256 characters. `depot-of-layer` gives nil for each (the proof's check at
    `module_proof.clj:942-943`), and `depot-at-making` would still give `:streaming` (G16). An
    act-id made of such a layer makes a stray of whatever is sent under it (G7): P2.
  - The read fails: the query throws or times out. "When it is not made" is not "when the read
    failed": P21.
  - A series of many ids: the contract does not say whether a series is one call. What a series
    costs the layer's task in reads is the plan's to say (Throughput).
  - A maker asks for an id and never sends: nothing is left (Data growth).
  - A maker asks for an id for one layer and gives it back with an act of another: O4, P2.

### O3. Asking for locks

Part 3: "When the door needs locks for a maker's acts in a layer, it sends a lock-request act whose
`:who` and `:permission` are the maker's, whose `:layer` is the act's, and whose one fact is
`{:e <the uuid of the request's own act-id> :k :softland.store/lock-request :v n}`; it reads the
request's answer by act-id and then its locks with `locks-of-request`." D3; D6; rules 5, 15, 20.
This is the door's own act. No maker calls it.

- **Latency.** A request is a whole act: one append, one read of its answer, one read of its locks.
  In a streaming layer that is three round trips, and the gate's event for a request of n locks
  writes n lock rows beside its fact, its answer and the last stamp (G14). In the base the answer
  is readable only when the request's batch is closed (G17): about a second on the in-process
  cluster. So a send that finds the stock empty waits that long before it can seal. The reason for
  the stock is that "sealing a value does not wait on the store" (`BUILD-store-module.md`, item 7).
  What must hold: in a streaming layer a send that finds locks in the stock makes no request on its
  way; how often a send does find them is the plan's to say and to hold against the 20 ms aim (G22).
- **Throughput.** One request for each n values that makers write in one layer under one who, plus
  the requests whose locks are never used. Each request is an act in the layer's depot for good,
  one fact row, one answer row and n lock rows (G14, G23).
- **Invariants.**
  - O3.1 A request is an act in the form that the gate checks (G9, G14): the maker's `:who` and
    `:permission`, the act's `:layer`, one fact under `:softland.store/lock-request` on the uuid of
    the request's own act-id, with a count. Which `:version` and `:session` it carries the contract
    does not say: P4.
  - O3.2 The count is 1 to 256: "the declaration's limit is the ceiling" (part 3;
    `declaration.clj:76-79`; rule 20). The door sends no request that the declaration refuses for
    its count.
  - O3.3 The request's act-id is made as any act-id is (O2.1 to O2.4), in the depot of the act's
    layer. So a request for the base goes to the microbatch depot, and its first fact's entity is
    its own uuid: it arrives on the task where its answer and its locks will be
    (`module.clj:48-52`).
  - O3.4 The door reads the request's answer before its locks (part 3). In a streaming layer the
    read of locks alone cannot tell "refused" from "not yet processed": both give an empty map
    (G4, G15).
  - O3.5 A lock is used only for values of acts that carry the same `:who` and `:layer` as the
    request that made it (part 3, the cutter's rule; D3). The gate does not hold this (G12); the
    door alone does, and its proof shows it (part 7: "that a lock never crosses a who").
  - O3.6 A lock is used for one value. It is never named by two facts of one act
    (`:lock-named-twice`, G11) and never by two acts that the door sends (H1 A; G12:
    `:lock-already-seals-a-value`). Whether a lock that a refused act named may be used again is
    P7.
  - O3.7 A lock's bytes never go into a depot (part 3; rule 5). A request carries a count. Nothing
    that the door appends, and nothing that it gives a maker, holds a lock's bytes. That the same
    holds for the door's own words and for what the door prints the contract does not say: P18.
  - O3.8 The door reads locks only of the requests that it made itself (D6), in this process.
  - O3.9 "A refused request is handed to the maker as the gate's refusal; the door retries nothing
    on its own" (part 3). The maker can tell it from a refusal of its own act: P5.
  - O3.10 The door never destroys a lock (part 3: "forget is not its work").
  - O3.11 A maker is handed the refusal only of a request that carried its own act's `:who`,
    `:permission` and `:layer`. A request under another permission says nothing of this act: the
    gate holds a request to the permission that it names (`decision.clj:136-144`).
- **Data growth and scale.** Unbounded and for good: each request leaves a depot record, a fact row,
  an answer row and n lock rows. A lock that is never used is a row that seals nothing, for good
  (part 3). The sources of such rows, as the contract and the module give them:
  1. a stock that a restart loses (part 3 names this one);
  2. a request that is accepted and whose locks the door never reads: its answer did not come
     within the door's wait, the read of locks failed, or the process stopped first;
  3. a lock that was taken for an act that the gate then refused, or that arrived second under a
     used act-id, or that never arrived (P7);
  4. a request that a maker sends as its own act through the door: nothing stops it (D1), and no
     door reads its locks (O3.8).
  The plan gives the number that bounds source 1 for one layer and who, and says what bounds the
  others or that nothing does. "The receipt counts the rows it left" (part 3): P22 asks who counts.
- **Concurrency.** Two makers that need locks for one layer and who at once: no lock goes to both
  (O3.6). Whether the second waits on the first's request or sends its own is the plan's; either
  way a maker never waits without a bound on a request that is not its own, and O3.11 holds. Two
  requests of one layer are two acts; in a streaming layer they are decided in the depot's order,
  and each is whole (G21). A request and the first use of one of its locks are in order because
  the door reads the request's answer before it seals (O3.4).
- **Edge cases.**
  - The request is refused. The words that a request can earn where an act of the same maker would
    earn the same: `:layer-not-made`, `:permission-does-not-stand`, `:who-not-the-permissions-holder`,
    `:no-permission`, `:no-session`, `:unknown-version`, `:malformed`, `:permission-not-in-layer`,
    `:id-too-long`. It makes no lock (the proof's checks at `module_proof.clj:558-559`,
    `843-845`). The maker's act is not appended (part 4). So one faulty act has two outcomes: with
    locks in the stock the act itself goes in and its refusal is recorded under its act-id; with
    an empty stock the request is refused and nothing is ever recorded under the act's act-id.
  - The request's answer is not readable yet. In the base that is each request, for about one run
    of the gate (G17). The contract gives no bound for this wait: the maker's bound is the bound of
    `answer-of` (part 5), and seal and send takes none. P8.
  - The request is accepted and its locks cannot be read: the read of locks throws. The locks are
    made and free. The door knows the request's act-id and may read again (O3.8); what it tells the
    maker meanwhile is one of the door's words (O6.1).
  - The request's append threw. The depot may hold it (G3). "The door retries nothing on its own."
    Nothing is appended for the maker's act. If the request went in, its locks are source 2 above
    unless the door reads them later.
  - The request is for the first value act of a layer whose making act is in flight. In a
    streaming layer the making is decided first when it was appended first (G21). In the base the
    two are on two depot partitions and have no order (G21): the request can be refused
    `:layer-not-made` in the same batch that makes the layer. A maker that reads the making's
    answer before it writes does not meet this.
  - The act needs more locks than the stock holds: the door asks for more. An act within the
    declaration's limit of 256 facts needs 256 locks at most, which one request can make. An act
    over that limit: P9.
  - A lock that the read gives with a way that this code does not know, or with nil for its bytes:
    `seal` gives nil for it (G18). No such lock goes into the stock as one that can seal, and no
    fact is ever sent with a `:sealed` that is nil.
  - A request that is sent ahead of need, outside any maker's call, and refused: there is no maker
    to hand it to. P16.

### O4. Seal and send

Part 4: "Given a maker's act in its one form, the door reads `:layer` and `:facts` and no more. For
each fact that carries a value and is not under a store key, it takes a lock from the stock and seals
the value to its place with `seal.clj`, giving `:sealed` bytes and a `:lock-id`; a fact under a store
key keeps its plain value; a retract has nothing to seal. It adds `:act-id` and `:depot`, appends the
thing to the depot the act-id names [...] and gives the maker the act-id, the digest of what it sent
(`digest.clj`), and the ack as a signal, never as the answer (F7 A)." D1, D3, D4, D6; part 6;
rules 5, 15, 16, 20.

- **Latency.** For a streaming layer the effect is needed in tens of milliseconds at most: a
  streaming layer is for "work a person engages with directly" (`src-store/README.md:147-152`), and
  the aim is 20 ms from the door's append to the answer in the door's hand (G22). With an act-id in
  hand and locks in the stock, one send is one append with `:ack`: the gate's event, 4 reads and 4
  row writes for one sealed value (G22), and the replication of its rows. Before the append the
  door's own work is, for each value, its normal form, its one text and one seal, and for the
  thing one digest, which walks the whole thing once (`digest.clj:40-44`). Without an act-id in
  hand, one read more (O2). With an empty stock, a whole request more (O3). For the base, seconds
  are acceptable for the decision; the append itself returns when the depot holds the thing (G2).
- **Throughput.** The rate at which makers write. The store's targets are at least 100 acts a second
  for one person and at least 1,000 a second on one task at the rate of an agent
  (`BUILD-store-module.md`, rule 21). The door is on the way of each act, so it must not be the
  narrow place: two sends wait on each other only where they take locks from one stock, and that
  taking is short. (A finding of the rig, not a form: at 64 writers its own handling of locks at
  the door, not its gate, set the rate; `src/proposal/rig-2026-09-25/README.md:390`.)
- **Invariants.**
  - O4.1 What the door appends for an act is the maker's act with three additions and no other
    change: `:act-id`; `:depot`; and, for each value fact, `:v` replaced by `:sealed` and `:lock-id`
    (part 4; act 2; `declaration.clj:5-10`). The door fills no missing part, reorders no fact,
    names no permission, and carries `:who`, `:session` and `:permission` as given (part 4; part 6).
  - O4.2 A value fact is a fact that carries a value and whose key is not one of the three store
    keys (part 4; G10). A fact with no `:v`, or with a nil one, is a retract and has nothing to seal
    (`act.clj:493-495`). A fact under a key that is in the store's namespace and is no store key is
    not under a store key: its value is sealed; a gate refuses the fact either way (G10).
  - O4.3 A value is sealed to the place `[the act's layer, the fact's entity, the fact's key]`
    (`seal.clj:34-36`), with a lock of its own (O3.5, O3.6), with a fresh random part (rule 15;
    `seal.clj:123-124`). The plain bytes are the UTF-8 of the one text of the value in its normal
    form: part 7 holds the door to "what the hand sends for the same act, but for the random parts",
    and that is what the hand seals (G18).
  - O4.4 `:depot` is the depot slot of the act-id, and the thing is appended to that depot (part 4;
    `declaration.clj:361-362`).
  - O4.5 The streaming depot is appended to with the level `:ack` (part 4). The microbatch depot
    with a level that says that the depot holds the thing (G2): P17.
  - O4.6 When the append returned, the maker gets three things: the act-id; the digest of the thing
    that was appended, as the 64 hex characters that the read by act-id gives (G19); and the ack as
    a signal that says which ack it is, since the two mean different things to a maker (G2). The
    ack is never given as the answer (D4; F7 A).
  - O4.7 The digest that the maker gets is the digest that the gate will keep: it is taken of the
    very thing that is appended, and that thing has the same one text after Rama has carried it
    (rule 11). The module's proof shows this for each kind of value, for a vector and for a list
    (the proof's checks at `module_proof.clj:506-537`, `597`, `619-623`); for facts given as a lazy
    sequence it is reached by no proof (`store-module/FULL_SPEC_REVIEW.md:255-256`): P20.
  - O4.8 No value under a plain key ever goes plain into a depot through the door. When the door
    cannot seal a value, it appends nothing for the act (part 4). The depots are never trimmed
    (G1), so a plain value there can never be forgotten (a finding of the rig:
    `src/proposal/rig-2026-09-25/README.md:363-370`). What else of a thing goes plain: P19.
  - O4.9 The door checks nothing of the act's form (D1; part 4). It reads `:layer` and `:facts`
    (part 4), and what it needs of `:who` and `:permission` for the locks (part 3): P4.
  - O4.10 When the door cannot do a step, it tells the maker in the door's own words (O6.1), it
    appends nothing for that act, and its words are kept in no record (part 4). One step is not
    like the others: an append that threw (P1).
  - O4.11 The door's calls give the maker a result and do not throw for what a maker gave: a thing
    that throws when it is read is one more thing that the door could not do (part 4: "it tells the
    maker in the door's own words").
  - O4.12 Once the call returns, the door keeps nothing of what it sent (part 5).
  - O4.13 When the call returns, the append is over: it was acked or it threw. The door starts no
    append that goes on after its call. So two sends that one maker makes one after the other reach
    the depot in that order (derived from part 4, "gives the maker [...] the ack"). What order the
    gate then decides them in is G21.
  - O4.14 The locks that one act names are neighbours: indexes next to each other, of as few
    requests as the stock allows. The streaming gate reads one range of lock rows for each request
    that an act names, from the least index to the greatest (`decision.clj:274-290`); "a door that
    uses a request's locks in order reads exactly the locks that it names"
    (`store-module/FULL_SPEC_REVIEW.md:124-132`). This is what the module's map expects of the
    door (`README.md:197-207`); the contract leaves the stock to the plan (part 3).
- **Data growth and scale.** In the door's process, nothing after the call (O4.12); during the call,
  the thing and its sealed copy. In the cluster, for good: one depot record for each send, also for
  a refused act, a repeat and a stray (G1); the rows of an accept; one answer row for each act-id
  that is decided. Within the declaration's limits one thing holds at most 256 sealed values of
  65,536 bytes (G9). The door checks nothing, so nothing bounds what it is given: P9.
- **Concurrency.** Many makers send at once through one door. No lock is taken twice (O3.6). The
  locks of one act are taken together, so that O4.14 holds when other sends take from the same
  stock. Two sends in flight at once have no order that the door promises. Two sends that one
  maker makes one after the other are in the depot in that order (O4.13); in a streaming layer
  that is the order of the decisions (G21). In the base it is not when the two acts have two
  first entities: an act that replaces a fact of the act before it can be decided first and be
  refused `:replaced-fact-not-current` (G21). A maker that needs the order reads the first answer
  before it sends the second; the door's docstring says so.
- **Edge cases.** The things that are no act are in the table that follows. Beside them:
  - An empty `:facts`, and an act of retracts and store-key facts only: nothing to seal, no lock
    is taken, and the stock is not read.
  - A making act: two facts under store keys (`first_act.clj:68-71`). It needs no lock. Its act-id
    carries the depot that the layer starts with (O2).
  - One act with a store-key fact, a retract and a value fact: one lock, for the one value.
  - Two facts on one place, each with a value: two locks; the gate refuses `:place-named-twice`
    (`declaration.clj:437-438`).
  - A value whose sealed bytes are more than 65,536, which is a plain text of more than 65,508
    bytes: sealed and sent, and refused `:sealed-value-too-large`; the depot keeps it (P9).
  - A fact under a store key with any value at all: it goes plain (O4.2; P19).
  - An act-id given back with the act. Of the act's layer: it is used as given. Of another layer:
    the thing would be a stray, `:act-id-of-another-layer` (G7): P2. With a depot slot that is not
    the layer's depot: the thing goes to the depot that the act-id names (O4.4) and comes back as a
    recorded refusal, `:layer-belongs-to-the-other-gate` or `:layer-not-made`
    (`decision.clj:146-153`; the module proof's checks of the seam,
    `store-module/FULL_SPEC_REVIEW.md:389-394`), read with the uuid or the layer as the act-id's
    depot slot says (G5). With a depot slot that is neither depot word: no depot is named; the door
    could not append. Not of the shape of an act-id: P2.
  - The same act given twice with one act-id; two acts given with one act-id: O5 and the matrix, B.
  - The act's `:who` holds locks in the stock that were asked for under a permission that the act
    does not name: the locks are used (O3.5 names `:who` and `:layer` only), and the gate judges
    the act's permission.
  - The seal gives nil (G18): the door could not seal; nothing is appended (O4.8).
  - The append throws before anything left the process: a partitioner runs on the appending
    client (G1), and Rama's client must first turn the thing into bytes. The door cannot tell this
    from a throw after the depot took the thing (P1).

#### Things given to the door that are not an act

"Literal" is what the door does when it reads part 4 to the letter: it reads `:layer` and `:facts`,
seals what it finds, adds its parts and appends. "The module" is what the gates as built do with
that thing.

| # | What is given | What the contract says | Literal: what would be appended | The module: what comes back |
|---|---|---|---|---|
| 1 | No map: nil, a number, a string, a vector | Part 4: "no `:layer`": the door's own words; nothing appended | Nothing | Nothing. Sent without the door it is the stray `:no-act-id` (`declaration.clj:479-481`) |
| 2 | A map with no `:layer`, or a nil one | The same | Nothing | Nothing |
| 3 | A thing whose `:layer` cannot be read without a throw (a sorted map with string keys) | Not named. O4.11: the door's own words | Nothing | Nothing. Without the door the depot's partitioner throws on the client (`FULL_SPEC_REVIEW.md:252-254`) |
| 4 | A record, or another map that is no plain map, with a `:layer` | "The door checks nothing of the act's form; a malformed act goes in and comes back as the gate's refusal" | The thing with the door's parts, still no plain map | A stray, `:no-act-id` (`declaration.clj:295-298`, `479`). No answer, ever. **P2** |
| 5 | `:layer` is no id: a string, a number, a keyword that does not read back | The same sentence | A thing whose act-id is `[that layer, :streaming, uuid]` (G16) | A stray, `:act-id-malformed` (`declaration.clj:482`). No answer, ever. **P2** |
| 6 | `:layer` prints to more than 256 characters | The same sentence; rule 20 | The same | A stray, `:id-too-long` (`declaration.clj:483`). No answer, ever. **P2** |
| 7 | `:facts` absent or nil | The same sentence | The thing as given, with `:act-id` and `:depot` | A recorded refusal, `:malformed` (`act.clj:151`, `343-345`) |
| 8 | `:facts` is no sequence: one fact map, a set of facts, a string | The same sentence | The thing as given: the door finds no fact, so it seals nothing. A value inside goes plain into the depot | A recorded refusal, `:malformed` (`act.clj:398-401`). The depot keeps the plain value. **P19** |
| 9 | `:facts` has no end | Not named | The call does not come back (`act.clj:505-506` says the same of the form's own check) | Nothing. **P9** |
| 10 | A fact that is no map: a string, a number, a vector | The same sentence | The fact as given, in its place | A recorded refusal, `:malformed` (`act.clj:332`). What the fact held is plain in the depot. **P19** |
| 11 | A fact that is a record, with a value | The same sentence | Either the record with its plain value, or a plain map built from it with the value sealed | Plain: `:malformed`, and a plain value in the depot. Rebuilt: the gate can accept what the form refuses. **P3** |
| 12 | A fact that already carries `:sealed` and `:lock-id`, and no `:v` | Act 3: "the door seals, the maker never does". Part 4 names no such fact | The fact as given: the door finds no value | The gate judges the maker's seal: accepted when the lock is a free lock of the layer and opens the bytes (G12), else a recorded refusal. The form refuses this fact, `:unknown-part` (G20). **P3** |
| 13 | A fact that carries `:v` and also `:sealed` or `:lock-id` | Part 4: the door "changes the act only by the three additions" | The door's seal in place of the maker's parts | Accepted, when all else is right: the fault is gone. **P3** |
| 14 | An act that already carries `:act-id` or `:depot` | The same | The door's parts in place of the maker's | Accepted, when all else is right: the fault is gone. The form refuses it, `:unknown-part` (G20). **P3** |
| 15 | A value that is no value of the form: a ratio, a list, a record, a number that is not finite, a value inside more than 32 collections | "A malformed act [...] comes back as the gate's refusal" | The value's one text, sealed. For a ratio or a record the text is `#other` and the name of its Java type: what the value held is gone (G19) | Accepted: the gate does not check what a lock opens (G12). The form refuses it, `:malformed` (G20). **P3** |
| 16 | A fact with a value whose `:e` or `:k` is no id or no key | The same sentence | The value sealed to the place as given; any thing has a one text (`text.clj:219-224`) | A recorded refusal, `:malformed`. One lock was taken. **P7** |
| 17 | A part that the form does not know, on the act or on a fact | The same sentence | As given | A recorded refusal, `:unknown-part` (`act.clj:333`). What the part held is plain in the depot. **P19** |
| 18 | More than 256 facts; more than 1,024 entries of `:based-on`; an id of more than 256 characters that is not the layer | The same sentence; rule 20 | The whole thing, each value sealed | A recorded refusal: `:too-many-facts`, `:too-many-based-on`, `:id-too-long`. Each lock was taken. **P7**, **P9** |

What the table shows. Rows 1 to 3 end in the door's own words, as part 4 says. The contract's
sentence, "goes in and comes back as the gate's refusal", holds for the faults of rows 7, 8, 10, 16,
17 and 18; in rows 8, 10 and 17 the refused thing leaves plain content in the depot (P19). It does
not hold for rows 4, 5 and 6, where the thing is a stray, and not for rows 11 to 15, where the
door's own additions take the fault away before a gate can see it, or, in row 12, where a gate
judges a seal that the maker made. Row 9 does not come back at all.

### O5. The answer back, with its bounded wait

Part 5: "`answer-of` an act-id and the digest the door gave on sending reads `answer-by-act-id` and
gives one of four: accepted, refused with the gate's reason, nothing yet, or not mine — an answer
under this act-id whose digest is not of what was sent (the store's builder's rule 11). For a
microbatch act, the door waits as the maker asks, in a bound the maker gives, reading by act-id until
an answer is there." D4, D5, D6; rule 11.

- **Latency.** One read is one query to one task (G5): for a streaming act-id one seek on the layer's
  task; for a microbatch act-id one seek, and a second for `:closed-through` when the row is there
  (`door_reads.clj:87-111`). After a streaming append that was acked, the first read has the answer:
  single-digit milliseconds, inside the 20 ms aim (G22). For a microbatch act the answer is there
  within two runs of the gate after the append (G22): hundreds of milliseconds to seconds.
- **Throughput.** One read for each streaming act. For an act that a maker waits on, as many reads
  as the wait makes before the answer is there. A microbatch act's reads go to the task of its
  uuid, so the waits of many acts spread over the tasks. At the microbatch gate's aim of 1,000 acts
  a second arriving on one depot partition, and about a second to an answer, about 1,000 waits run
  at once; the reads that they cost each second are that number times the rate at which one wait
  reads. The plan gives that rate and its cost in reads on a task (G26). The door may not replace
  the reads by a subscription (D5).
- **Invariants.**
  - O5.1 The call gives one of four answers (part 5), and one of the door's own words when it could
    not read (P10). It gives no other thing.
  - O5.2 "Accepted" and "refused, with the gate's reason word" are given only when the record holds
    an answer under the act-id whose digest is the digest that the maker gave (D4; rule 11). The
    door compares the two hex texts; it holds no thing to take a digest of (part 5).
  - O5.3 "Not mine" is given when the record holds an answer under the act-id with another digest
    (part 5). It says nothing more: not whose the answer is, and not what it says. Whether the
    answer's verdict is given beside it the contract does not say; P11 holds what a maker's own
    second send reads.
  - O5.4 "Nothing yet" is given when the read gave nil. It does not mean refused
    (`door_reads.clj:12-13`), and it does not mean that nothing is on a depot. It is the read's
    answer in each of these states, and the door cannot tell them apart: nothing was ever sent
    under the act-id; a thing is on the depot and not decided; a microbatch act is decided and its
    batch is not closed (G17); the thing is a stray (G7); the act-id is not one that the read's
    head takes (G6).
  - O5.5 An ack is never given as the answer, and no answer is made from one (D4; F7 A).
  - O5.6 The wait is one question about one act-id. It reads by act-id until a read gives an answer
    or the maker's bound has passed, and then gives what the last read gave. It gives no
    subscription and no stream of changes, and it decides nothing of W4 (part 5; D5).
  - O5.7 How far the bound reaches: the door starts no read after the bound has passed. A read
    that is on its way then takes its own time, up to the time limit of a query in Rama
    (`reference/rama/docs/13-query-topologies.md:342`). The door's docstring says how far the
    bound reaches (`src-store/CLAUDE.md`, "say how far the claim reaches").
  - O5.8 A wait with no bound does not exist (D5: "bounded by the maker's word").
  - O5.9 The call reads and does nothing else: it appends nothing, takes no lock, and changes
    nothing in the door's memory (part 5; part 1).
  - O5.10 The door reads no fact of the record for a maker (D6). An answer is no fact.
  - O5.11 The call needs nothing that the door remembers (part 5: "the maker holds the act-id and
    the digest"). So it gives the same for an act-id and a digest that a maker kept from a process
    before.
- **Data growth and scale.** None: no memory, no row, no depot record.
- **Concurrency.** Any number of calls at once, also about one act-id. Each compares the record's
  one answer (G8) with its own digest. Calls do not wait on each other.
- **Edge cases.**
  - A digest that is nil or is no digest: "not mine" when an answer is there, "nothing yet" when
    none is.
  - A bound of zero, or none given: one read. A bound below zero: one read. A very long bound for
    a thing that is a stray: the wait runs to its bound and gives "nothing yet" (O5.4).
  - A wait for a streaming act-id. Part 5 names the wait "for a microbatch act". After a streaming
    append that threw, the answer can come later (G3): P10.
  - The read throws: none of the four is true. P10.
  - The answer's stamp: P10.
  - An act-id that is no act-id, or whose layer is no usable id: the read's head gives nil (G6);
    a first argument that Rama's client cannot hash is reached by no proof (G6). P2.
  - The maker sent the same act twice under one act-id: P11.
  - The answer of the base's making act is readable a few milliseconds before the base's depot row
    (`README.md:179-184`): `act-id-for` still gives the same id (O2), so nothing follows.

### O6. What the contract implies beside the five

- **O6.1 The door's own words** (part 4; D1; D7). "The words are the builder's, plain, listed for Sid."
  What must hold of the set:
  - Each word names what the door could not do, never a judgement of the act (D1). It is not a
    gate's reason word and can never be read as one.
  - The set covers each way in which a call of the door ends without one of its named results.
    The contract names four: no `:layer`; a layer not made and no depot to start with (P14); a stock
    empty and the request refused (P5); an append that threw (P1). Reading the module finds these
    beside them: a thing that cannot be read (row 3 of the table); a layer of which no act-id can
    be made (P2); an act-id given back that names no depot, or is not of the act's layer (P2); the
    read of the layer's depot failed (P21); the request's append threw; the request has no answer
    within the door's wait (P8); the request was accepted and its locks could not be read; the seal
    gave nothing (G18); the read of an answer failed (P10); the door could not be opened (O1.5).
  - Each word says which of two things is true: nothing was appended for this act; or the door
    does not know whether the depot holds the thing (P1). A maker decides what to do next by it.
  - A word about a request says what became of the request, since a request is an act in the depot
    and in the record whatever becomes of the maker's act (O3; P5).
  - Each word has a case that the door's proof can reach on the in-process cluster (part 7: "each
    of the door's own words"): P13.
  - Latency, throughput, growth: a word costs no round trip of its own and leaves nothing
    anywhere ("kept in no record", part 4). Concurrency: none.
- **O6.2 The sender, until sign-in** (part 6). The door takes `:who`, `:session` and `:permission` as
  the maker gives them and checks none. It treats `:system` as any who (K3 A; K9 is open and is not
  decided here). It makes no person and no id for one (?4 A). Its docstring says in plain words that
  it cannot check that the sender is the who that the act names (part 6). The fields of the
  template do not apply further: this is a thing that the door does not do.
- **O6.3 What the door's proof must be able to see** (part 7). To show its own cases the proof needs:
  the thing that the door appended for an act, to hold it against what the hand sends; each lock id
  that the door used and the `:who` of the act that carries it; the bytes of each lock that the
  door read, to show that none is in a depot's log; an append that throws; a restart of the door's
  memory. The door gives a maker none of the first three (part 5; O3.7). How the proof sees them
  without a second client of the module, against D8, is P13.
- **O6.4 The end of a process.** The contract names no closing of the door. A process that ends
  loses the stock (part 3; D3), each request that it had not read, and nothing else, since the door
  owns nothing durable (O1.2). An act that is on a depot is decided whether the process lives or
  not; its maker can read the answer later only if it kept the act-id and the digest itself
  (O5.11). The door promises nothing across a restart: a maker that sends an act again after one,
  under a new act-id, has sent a second act. P22.
- **O6.5 The pure parts** ("What to build": the making of an act-id, and telling a value fact from a
  store-key fact and a retract, "may be split into a pure namespace"). If they are split, they load
  with Clojure alone: the alias `:store` is the guard that pure code needs nothing else
  (`src-store/README.md:33-38`). The namespace that requires Rama's client loads under
  `:store-module` only.
- **O6.6 The explanations that the door changes** (the contract, "Explanations"). The sentences of
  the tree that say that the door is not built, or that expect something of it, are listed under
  "What the module's explanations expect of the door". Which of them the build must change is P12
  and P15.

## Entity State × Write Matrix

**The reads.** A row lists each of these that is related to its entity.

- `id`: `act-id-for` the layer, and inside it the read `depot-of-layer` (O2). A maker's read.
- `answer`: `answer-of` the act-id with the digest that the send gave (O5). A maker's read.
- `send gives`: what the call that made the write gives back to the maker (O4.6, O6.1).
- `locks`: `locks-of-request` for a request that this door made (O3.8). The door's read; no maker
  has it.
- `stock`: what the door's memory holds for the layer and who. The door's own; no maker has it.
- `log`: what the depot's log holds, and the gate's count of strays. An operator's read, and a
  proof's; never the door's (`module.clj:16-23`; `module_proof.clj:278-285`).

**The writes.** `act-id-for`; the door's own request for locks; seal and send; `answer-of` with its
wait, which writes nothing and is listed where a state changes what it gives; and a restart of the
process, which is no call.

A row marked with a point (P1 to P22) says what the literal reading gives; the point holds the
other answers.

### A. A layer

States: **A0** its id is no usable id (no id, or over 256 characters); **A1** not made, and it starts
streaming; **A2** the base, not made; **A3** made, streaming; **A4** the base, made, its making's
batch closed; **A5** the base, its making decided in a batch that is not closed yet. A layer whose
depot has changed is no state of this build (?9; G16).

```
A0 x act-id-for
  - id: [the thing given, :streaming, a uuid]: depot-of-layer gives nil for what is no usable id
    (G6), and the starting depot of anything that is not the base is :streaming (G16). P2.
  - answer: nothing yet, for ever: the read's head takes no such act-id (G6).
  - locks: no request exists; nothing to read.
  - stock: unchanged.  - log: unchanged.
A0 x request for locks (a value act in A0 with an empty stock)
  - send gives: under the literal reading, nothing in time: the request is a stray (G7), so its
    answer never comes and the door's wait for it runs to its bound (P8). P2.
  - answer (of the request): nothing yet, for ever.   - locks: {} (G4).   - stock: unchanged.
  - id: as A0 x act-id-for.   - log: the request is in the streaming depot; one stray more.
A0 x seal and send (an act with no value fact)
  - send gives: the act-id, a digest, :acked.  The thing is a stray (G7). P2.
  - answer: nothing yet, for ever.   - id: as above.   - locks, stock: untouched.
  - log: the thing is in the streaming depot; the gate keeps its digest under :act-id-malformed
    or :id-too-long.
A1 x act-id-for
  - id: [layer :streaming uuid]; the read gave nil, so the depot is the starting one (O2.2).
  - answer: nothing yet (nothing was sent).   - locks, stock, log: unchanged.
A1 x seal and send, a right making act
  - send gives: the act-id, the digest, :acked.   - answer: accepted.  The layer is now A3.
  - id (after): [layer :streaming uuid], now from the module's own read.
  - locks, stock: untouched: a making act has two store-key facts and no value (O4 edge cases).
  - log: the thing, with its two plain store-key values.
A1 x seal and send, an act with no value fact that is no right making act
  - send gives: act-id, digest, :acked.   - answer: refused :layer-not-made (decision.clj:152-153).
  - id: unchanged.   - locks, stock: untouched.   - log: the thing.  The layer stays A1.
A1 x request for locks; A1 x seal and send, an act with value facts
  - The stock holds nothing for a layer that is not made: a lock comes only from an accepted
    request, and a request needs a made layer.  So the send asks first.
  - answer (of the request): refused :layer-not-made.   - locks: {} (module_proof.clj:558-559).
  - send gives: the door's own word, with the request's refusal (P5).  Nothing is appended for
    the act.
  - answer (of the act's act-id, when the maker made one ahead): nothing yet, for ever.
  - id: unchanged.   - stock: unchanged.   - log: the request, not the act.
A2 x act-id-for
  - id: [:base :microbatch uuid]: nil from the read, and the base starts microbatch (G16).
  - answer: nothing yet.   - locks, stock, log: unchanged.
A2 x seal and send, the base's making act
  - send gives: act-id, digest, the microbatch depot's ack: the depot holds the thing (G2).
  - answer: nothing yet until the batch is closed (the layer is A5 meanwhile); then accepted, and
    the layer is A4.   - id: [:base :microbatch uuid] before and after (G16).
  - locks, stock: untouched.   - log: the thing, in the microbatch depot.
A2 x seal and send, another act with no value fact
  - send gives: as above.   - answer: nothing yet, then refused :layer-not-made.
  - id: unchanged.   - locks, stock: untouched.   - log: the thing.
A2 x request for locks; A2 x seal and send, an act with value facts
  - answer (of the request): nothing yet until its batch is closed; then refused :layer-not-made.
    The door waits for it: P8.   - locks: {}.
  - send gives: the door's own word (P5), or its word for a request with no answer yet (P8).
  - answer (of the act's act-id): nothing yet, for ever.   - id, stock: unchanged.
  - log: the request.
A3 x act-id-for
  - id: [layer :streaming uuid], from the module's read.   - answer: nothing yet.
  - locks, stock, log: unchanged.
A3 x request for locks, under a permission that stands
  - answer (of the request): accepted, at once after the ack.
  - locks: n locks, indexes 0 to n-1, each with its bytes and its way (G14, G15).
  - stock: n more for this layer and who, once the door has read them.
  - id: unchanged.   - log: the request, with its count and no bytes.
A3 x request for locks, under a permission that does not stand, or by a who that is not its holder
  - answer (of the request): refused, with the gate's word.   - locks: {} (module_proof.clj:843-845).
  - stock: unchanged.   - id: unchanged.   - log: the request.
A3 x seal and send, an act with no value fact
  - send gives: act-id, digest, :acked.   - answer: accepted, or refused with the gate's word.
  - id: unchanged.   - locks, stock: untouched.   - log: the thing.
A3 x seal and send, an act with value facts, locks in the stock
  - send gives: act-id, digest, :acked.   - answer: accepted, or refused with the gate's word.
  - stock: fewer by one for each value, whatever the answer (P7).
  - locks: the same locks with the same bytes, whether they now seal or not (G15).
  - id: unchanged.   - log: the thing, each value sealed, each lock id plain, no lock's bytes.
A3 x seal and send, a making act again
  - answer: refused, :who-not-the-permissions-holder or :permission-fact-not-beneath-the-acts-own
    (module_proof.clj:574-577).   - id, locks, stock: unchanged.   - log: the thing.
A4 x act-id-for
  - id: [:base :microbatch uuid], from the module's read.   - the rest: unchanged.
A4 x request for locks, under a permission that stands
  - answer (of the request): nothing yet until its batch is closed; then accepted.
  - locks: {} until then; then n locks with their bytes (G15, G17).
  - stock: n more, once read.   - id: unchanged.   - log: the request, in the microbatch depot.
A4 x seal and send, an act with no value fact; with value facts and locks in the stock
  - send gives: act-id, digest, the depot's ack.
  - answer: nothing yet until the batch is closed; then accepted or refused.
  - stock: fewer by one for each value.
  - locks: a lock that now seals comes with nil for its bytes, from the moment when the act's
    batch is visible on the lock's home task (README.md:173-178).
  - id: unchanged.   - log: the thing.
A5 x act-id-for
  - id: [:base :microbatch uuid]: the read gives nil for a row of a batch that is not closed
    (door_reads.clj:156-165), and the starting depot is the same (G16).
A5 x request for locks; A5 x seal and send
  - As A4, when the thing falls into a batch after the making's.  In the making's own batch the
    two are on two depot partitions and have no order (G21): the request or the act can be
    refused :layer-not-made.
  - answer: nothing yet until its own batch is closed.   - locks, stock, id, log: as A4 or A2.
```

### B. An act-id

States: **B0** not made by the door; **B1** made, held by a maker, nothing sent under it; **B2** a
thing under it is on its depot, and no answer is readable; **B3** answered, accepted, and the answer
is of this maker's thing; **B4** answered, refused, of this maker's thing; **B5** answered for
another thing; **B6** the thing under it is a stray; **B7** the append threw. `d1` is the digest of
the first send, `d2` of a second.

```
B0 x answer-of
  - answer: nothing yet.  The door keeps no id (D2), so it cannot tell an id that it never made.
  - id: never gives this id by its own making, but for the chance of a random uuid.   - log: nothing.
B0 x seal and send, with an act-id that a maker made itself
  - The door cannot tell it from its own (D2).  Of the shape, of the act's layer: as B1.  Else P2.
B1 x seal and send, act X
  - send gives: the act-id, d1, the ack.  The state is B2, then B3 or B4; or B6; or B7.
  - answer(d1): by the state.   - id: unaffected.   - log: X as sent.
B1 x answer-of, with or without a wait
  - answer: nothing yet; a wait runs to its bound.   - log: nothing.
B1 x restart
  - Nothing is anywhere: the door kept no id (O2.4).  The maker may still use the id.
B2 x answer-of
  - answer(d1): nothing yet; with a wait, the answer when it comes within the bound.
B2 x seal and send, act X again, under the same act-id
  - A second thing is on the depot.  The gate decides the one that arrived first, or in one batch
    of the base the one with the earlier turn, and writes nothing for the other (G8; G21).  When
    X has a value fact the two things differ: other locks, other random parts, so d2 is not d1.
    When X has none they are one text and d2 = d1.
  - answer(d1): the first thing's answer.   - answer(d2): not mine, unless d2 = d1.  P11.
  - stock: the locks of the second send are taken and never seal (P7).   - log: both things.
B2 x seal and send, another act Y, under the same act-id
  - The same: one is decided, by arrival; the other reads not mine.   - log: both.
B2 x restart
  - The thing is decided all the same (G3).  answer(d1) gives it, to a maker that kept the id and
    d1 itself (O5.11).
B3 x answer-of
  - answer(d1): accepted, the same at each call: an answer that a read gave never changes
    (door_reads.clj:44-45).
B3 x seal and send, X again; Y
  - The gate writes nothing (G8).   - answer(d1): accepted.   - answer(d2): not mine.
  - Y is in the log and not in the record.  Its maker sends it under a new act-id.
  - stock: the second send's locks are taken and never seal.
B3 x restart
  - answer(d1): accepted: the record is durable (G25).
B4 x answer-of
  - answer(d1): refused, with the gate's word, the same at each call.
B4 x seal and send, X corrected, under the same act-id
  - The gate writes nothing: the refusal is the one answer of this act-id (G8).
  - answer(d2): not mine.  A corrected act needs a new act-id.
  - stock: the locks of the refused first send are free in the record (G13) and out of the stock
    (P7); the second send's too.
B5 x answer-of
  - answer(this maker's digest): not mine.  A wait gives it at the first read.
B5 x seal and send, under it again
  - Nothing is written.  Not mine again.
B6 x answer-of
  - answer: nothing yet, for ever; a wait runs to its bound (O5.4).  P2.
B6 x seal and send, the same act again
  - Another stray.  The gate keeps the digest of each different stray once (G7).
  - log: one record more for each send.
B7 x answer-of
  - answer(d1): nothing yet, when the depot does not hold the thing or the gate has not decided
    it; accepted or refused, when it went in; not mine, when another thing holds the act-id.
    The maker has d1 only if the door's word for the thrown append carries it.  P1.
B7 x seal and send, X again, under the same act-id
  - At most one of the two is decided (G8).  The maker holds d1 and d2: one reads the answer, the
    other reads not mine.  P11.
B7 x seal and send, X again, under a new act-id
  - Both can be accepted: X is twice in the record.  The door does not know that they are one act.
B7 x restart
  - As B2.
```

### C. A lock

States: **C0** not made; **C1** made and free, not read by this door; **C2** free, in the stock of its
layer and who; **C3** taken: it sealed a value of a thing that was sent and is not answered; **C4** it
seals an accepted value; **C5** free in the record and in no stock.

```
C0 x request for n locks, accepted
  - answer (of the request): accepted.
  - locks: streaming, n locks with their bytes at once; the base, {} until the request's batch is
    closed, then n locks.   - stock: empty until the door has read them (C1 -> C2).
  - log: the request, with a count.  No bytes: the gate makes them at the write (G14; rule 5).
C0 x request for n locks, refused
  - answer (of the request): refused.   - locks: {}.   - stock: unchanged.   - log: the request.
C0 x seal and send, a value act
  - Not reached: the door seals with a lock from the stock or not at all (O4.8).
C1 x the door reads the request's locks
  - locks: the n locks; the same bytes at each read (README.md:178; rule 5).   - stock: holds them.
C1 x restart
  - stock: empty.   - locks: the rows are there, and no door knows the request (O3.8).  C5.
C2 x seal and send, a value act of the same layer and who
  - stock: the lock is gone from it, and is given to no other fact and no other act (O3.6).
  - locks: unchanged.   - answer (of the act): by the gate.   - log: the lock's id, plain, on its fact.
C2 x seal and send, by another who of the layer, or by this who in another layer
  - stock: the lock stays; it is not given (O3.5).  That send asks for its own.
C2 x restart
  - stock: empty.  The lock is a row that seals nothing, for good (part 3).  C5.
C3 x the gate accepts the act
  - answer: accepted.   - locks: streaming, the lock with its bytes as before (G15); the base, nil
    for its bytes.   - stock: not there.  A later act that named it would be refused
    :lock-already-seals-a-value (G12).  C4.
C3 x the gate refuses the act, or the thing ends at check (a)
  - answer: refused; or not mine.   - locks: the lock, with its bytes: it is free (G13).
  - stock: not there, unless P7 puts it back.  C5.
C3 x the append threw
  - answer: P1.  The lock seals, or it is free; the door does not know.  If it were given to
    another act and the first went in, that act would be refused :lock-already-seals-a-value for
    no fault of its maker.   - stock: not there.
C3 x restart
  - The gate decides the thing all the same: C4 or C5.   - stock: empty.
C4 x any write
  - Never taken again.  The door never opens what it seals (D6).  locks, in a streaming layer,
    still gives its bytes to any caller (G24).
C5 x any write
  - A row that seals nothing.  The door never destroys it (O3.10).  The receipt counts it (P22).
```

### D. A request for locks

States: **D0** not sent; **D1** on its depot, no answer readable; **D2** accepted, its locks not read;
**D3** accepted, its locks in the stock; **D4** refused; **D5** its append threw.

```
D0 x the door sends it
  - answer (of the request): by the layer's state (A).   - log: the request.
D1 x the door reads its answer
  - answer: nothing yet.  In a streaming layer after an ack this is not expected, since the ack
    waits for the gate (G2).  In the base it is each request, until its batch is closed (G17).
  - locks: {}.   - stock: unchanged.
D1 x seal and send, by a maker that needs these locks
  - send gives: the answer in time, and the send goes on; or the door's word (P8).
D1 x restart
  - The request is decided all the same.  Its locks are C5.
D2 x the door reads its locks
  - locks: the n locks.   - stock: n more.  D3.
D2 x the read of its locks fails
  - send gives: the door's word.   - stock: unchanged.  The locks stay C1 until a later read.
D3 x seal and send
  - stock: gives the locks, each once (C2).
D3 x the same request sent again
  - The door does not send it again (part 3).  If it did, no lock would be made again (rule 5).
D4 x seal and send, by the maker whose act caused it
  - send gives: the door's word, with the gate's word for the request (P5).  Nothing is appended
    for the act.   - locks: {}.   - stock: unchanged.
D4 x seal and send, the next act that needs locks for this layer and who
  - A new request, under that act's `:who` and `:permission` (O3.11).  When nothing has changed it
    is refused again: one more record in the depot and one more answer row for each try (P9).
D5 x seal and send, by the maker whose act caused it
  - send gives: the door's word.  Nothing is appended for the act.  The depot may hold the request
    (G3); if it is accepted, its locks are C1.
```

### E. The door's memory of one process

States: **E0** not opened; **E1** opened, no lock for this layer and who; **E2** at least as many
locks for them as the act needs; **E3** some, and fewer than the act needs; **E4** locks for another
who of the layer only; **E5** a request for this layer and who is on its way; **E6** opened again
after a restart.

```
E0 x opening
  - stock: empty (O1.4).  E1.
E1 x act-id-for; E1 x answer-of
  - stock: untouched: neither call reads or changes the memory (O2.6, O5.9).
  - id, answer: as their own rows say.
E1 x seal and send, an act with no value fact
  - stock: untouched, and no request is sent (O4 edge cases).
E1 x seal and send, an act with m value facts
  - A request first (D).  stock, after: what the request gave, less m.
E2 x seal and send, an act with m value facts
  - stock: m fewer, taken together, neighbours (O4.14).  No request on the way of this send.
  - send gives: act-id, digest, ack.
E3 x seal and send, an act with m value facts
  - The door asks for more.  Whether the act then names locks of two requests is the plan's;
    each request that an act names is one range for the streaming gate to read (O4.14).
E4 x seal and send, by a who that has no lock
  - stock: the other who's locks are untouched (O3.5).  A request under this act's who.
E5 x seal and send, a second act of this layer and who
  - It waits on that request, within a bound, or sends its own (O3, Concurrency).  No lock goes
    to both acts.
E5 x restart
  - The request is decided all the same; its locks are C5.
E2, E3, E4 x restart
  - stock: empty.  Each lock that it held is C5.  E6.
E6 x act-id-for; answer-of
  - As E1.  answer-of gives the record's answer for an act-id and a digest from before (O5.11).
E6 x seal and send, an act with value facts
  - As E1: a new request.  The door reads no lock of a request from before (O3.8).
```

### F. A depot's log

States: **F0** it holds nothing of the act; **F1** it holds the thing.

```
F0 x seal and send that the door completes
  - log: the thing: the maker's parts as given; `:act-id`; `:depot`; each value under a plain key
    as sealed bytes with its lock id; each value under a store key plain.  No lock's bytes (rule 5).
    Plain and for good: `:who`, `:layer`, `:session`, `:permission`, each entity, each key, each
    act-id and lock id, and whatever the door did not seal (P19).  F1.
F0 x seal and send that ends in one of the door's words
  - log: nothing of the act (part 4), but for an append that threw (P1).  A request that the
    send caused may be there (D4, D5).
F0 x request for locks
  - log: the request: a count, no bytes.
F1 x seal and send, the same act-id again
  - log: one record more.  The log keeps each thing as appended; a repeat and a stray too (G1).
F1 x restart
  - log: unchanged.
```

## Open points for the build session

Each point says what is unsettled; the passage of the contract and the source lines that meet there;
the answers that are possible; and its kind, one of four:
**silent** (the contract is silent), **cannot be met** (the contract cannot be met as written),
**two clauses** (two clauses of the contract disagree), **the module expects** (the module's
explanations expect something that the contract does not give). Where the pick is plain engineering
the writer says which it would take and why. Where it would be a decision of Sid's, it says so and
picks nothing.

### P1. An append that threw may have appended. *Cannot be met.*

Part 4 lists "an append that threw" among the steps that the door cannot do, and says of all of
them: "it appends nothing for that act". By G3 an append that throws can have gone through: the
module's own docstring says that an ack can fail for an accepted act (`module.clj:60-65`), Rama's
documentation says the same of any exception from a depot client (`14-depots.md:212`, `214`;
`11-stream-topologies.md:297`), and the rig ran one case (`RIG.md:2177-2180`). A throw can also
come before anything left the process, since the depot's partitioner runs on the client (G1). The
door cannot tell the two.

- (a) The door's word for a thrown append says that the door does not know whether the depot holds
  the thing. It carries the act-id and the digest, so that the maker reads the record (F7 A; D4).
  The contract's sentence is then true of each other word and untrue of this one; the receipt says
  so.
- (b) The door reads the answer itself before it returns. The send then waits, and "nothing yet"
  still does not tell whether the thing is on the depot.
- (c) The door sends the thing again itself. It must then keep the thing (against part 5), and
  part 3 says of a request that "the door retries nothing on its own".

The writer would take (a). It is what F7 A already says: what leaves a decision is a signal, and the
truth is the record.

### P2. A thing that would be a stray has no answer, ever. *Cannot be met.*

Part 4: "The door checks nothing of the act's form; a malformed act goes in and comes back as the
gate's refusal." For rows 4, 5 and 6 of the table, and for an act-id given back that is not of the
act's layer or not of the shape, what goes in is a stray (G7; `declaration.clj:453-487`). A stray
gets no answer, so `answer-of` gives "nothing yet" for ever, and the maker cannot tell it from an
act that is on its way (O5.4). If the act has a value fact and the stock is empty, the door's own
request is a stray first (matrix, A0).

- (a) The door appends a thing only when a gate can answer it under its act-id. It tests what its
  own addition needs and nothing else: the thing is a plain map; its `:layer` is an id that an
  act-id can hold (`declaration.clj:276-282`); an act-id given back has the shape, is of that layer,
  and names a depot (`declaration.clj:284-291`). When one fails, the door says in its own words that
  it could not make an act-id for the thing, and appends nothing.
- (b) Literal. The thing goes in; the docstring says that such a thing reads "nothing yet" for ever.
- (c) A fifth answer, "no answer will come". The door cannot know it from the read (G4).

The writer would take (a), and reads it as inside D1: the act-id is the door's own addition (act 2),
and these three tests are what the door needs to make it, "what the door could not do". The build
session holds it against D1.

### P3. A fault of the maker's form that the door's own additions take away. *Cannot be met.*

The same sentence of part 4, and D1. For rows 11 to 15 of the table the fault is gone before a gate
can see it, so the act comes back accepted and not refused:

1. a value that is no value of the form. The door seals its one text. The gate "does not check that
   what a lock opens is a value of the form" (`decision.clj:326-327`). For a ratio or a record the
   text is a mark and the name of a Java type, and what the value held is gone (`text.clj:39-44`).
   Who can still tell: nobody before a read opens the bytes, and no read of facts is built (group
   6); for those marks, nobody after either;
2. a fact that already carries `:sealed` or `:lock-id`;
3. an act that already carries `:act-id` or `:depot`;
4. a fact that is a record.

`act.clj` refuses each (G20). Act 3 put the seal below the form (`src-store/README.md:66-69`), so
after the seal only the door, before it, or a gate that reads what a lock opens, can hold the form
of a value.

- (a) Literal. The gate accepts what the form refuses. For case 1 the record then holds bytes that
  do not read back as what the maker gave, and the maker reads "accepted".
- (b) The door says in its own words that it could not seal. "Which plain bytes stand for a value"
  is the door's (`seal.clj:51-52`), so "no plain bytes stand for this" is a thing that the door
  could not do; and likewise "this already holds the part that the door adds". The door is then a
  check of one corner of the form: the value (`act.clj:244-269`), and its own five words.
- (c) A gate checks what a lock opens. That is a change of the gates, which the contract puts
  outside this build and calls "a finding for the receipt"; rule 16 says that "the grammar check has
  its place" there.

Not picked. D1's source is act 2, and Sid's F8 C says "The door may warn early; the gate is the
authority" (`src-store/README.md:164-167`): whether the door may refuse a value is a line between
the door and the gate that he drew. What each answer costs: (a) loses a maker's value without a
word; (b) narrows D1 by one corner; (c) is not this build.

### P4. What the door reads of an act. *Two clauses; and silent.*

Part 4: "the door reads `:layer` and `:facts` and no more." Part 3: the request's "`:who` and
`:permission` are the maker's", and "a lock is used only for values of acts that carry the same
`:who` and `:layer`"; D3. The door cannot do the second without reading `:who` and `:permission`.
And a request without `:version` and `:session` is refused `:unknown-version` or `:no-session`
(`act.clj:142`, `146`); the contract does not say which the request carries.

- (a) The door also reads `:who`, `:permission`, `:version` and `:session`, to make the request and
  to choose the stock, and copies them as given. It checks none.
- (b) The request carries a session of the door's own. The door would then make an id that the
  record keeps, and sessions are open rows (E5, E6, Q19).
- (c) The maker gives who and permission beside the act. One fact would then be in two places.

The writer would take (a). It decides nothing of E5, E6 or Q19. One thing follows that those rows
may later touch: a lock asked for in one session can seal a value of the same who in another, since
the cutter's rule names who and layer only.

### P5. A refused request: the gate's refusal, or the door's own words. *Two clauses.*

Part 3: "A refused request is handed to the maker as the gate's refusal." Part 4: for "a stock empty
and the request refused" the door "tells the maker in the door's own words, which are not a gate's
refusal and are kept in no record". The request's refusal is kept in the record, under the request's
act-id, and the request is in the depot (G1, G13).

- (a) One word of the door's, "could not get locks: the request was refused", which carries the
  gate's reason word for the request and the request's act-id. Nothing is recorded under the act's
  own act-id, and the word says so.
- (b) The gate's refusal as if it were the act's. The maker then reads `answer-of` its act-id and
  finds nothing.

The writer would take (a). With it the docstring says one more thing: one faulty act has two
outcomes by the state of the stock (O3, Edge cases).

### P6. "Nothing of the gates' or the record's internals", and three rules that live there. *Two clauses.*

"What to build": the door requires "Rama's client API, and nothing of the gates' or the record's
internals: it uses the module's surface only, two depots and three queries." Part 2 itself names
`first_act.clj`, under `gate/`, for the depot that a layer starts with. The module's surface needs
two things more of a caller: the routing id, whose one function is `record/rows.clj:312-321`, and
the tests of what is passed, which `door_reads.clj:69-71` tells a caller to make "with the same
functions" (`rows.clj:273-344`; "the proof's hand calls `routing-id` and the `-read` functions",
`rows.clj:83-85`).

- (a) "Internals" are the PStates, the rows and the topologies. The door may require the pure
  functions that each hold one rule, so that each rule stays in one place. Its docstring names each
  and why.
- (b) The door writes the routing rule and the starting depot a second time. Two places can drift.
- (c) The rules move under `door/`. That changes files of `gate/` and `record/`, which the contract
  puts outside this build.

The writer would take (a).

### P7. A lock that was taken for an act that is not accepted. *Silent.*

Part 5: "The door keeps nothing of what it sent once the call returns." Part 3 names one source of
rows that seal nothing, a restart. Rule 15: "A lock that a refused act named is free [...] 'A lock
seals one value' counts accepted values." By G13 such a lock is free in the record; by G15 the read
of locks in a streaming layer does not say whether a lock seals.

- (a) A taken lock never comes back to the stock. Each value of each act that is refused, that
  arrives second under its act-id, or that never arrives, leaves one row that seals nothing. The
  receipt counts them with the rows of a restart.
- (b) A lock comes back when `answer-of` reads "refused" or "not mine" for its act. The door must
  then remember which locks an act-id took, past the call. It must never come back on "nothing yet"
  or after a thrown append: if the first act went in, the next act under that lock is refused
  `:lock-already-seals-a-value` for no fault of its maker. Under (b) two sealed values in the log
  open with one lock, the refused one and the later one; rule 15 allows it, and both carry one who
  and layer by the cutter's rule, so the room for forget (K11, H10, H11) is the same.

The writer would take (a) for this build: (b) needs a memory that part 5 rules out, and a wrong
return costs a good act. The cost of (a) is in P9.

### P8. The door's wait for its own request. *Silent.*

Part 3: the door "reads the request's answer by act-id and then its locks". Part 5 gives the maker's
bound to `answer-of`; seal and send takes none. In the base each request's answer waits for its
batch to close (G17).

- (a) A bound of the door's own, a number with its reason in the docstring. When it passes: a word
  of the door's, and nothing is appended. The door remembers the request and reads its locks at
  the next need (O3.8).
- (b) The send takes a bound from the maker.
- (c) The send never waits: it ends at once with the door's word, and the maker comes back.

The writer would take (a).

### P9. What a thing can cost when the door checks nothing. *Silent; and the module expects.*

D1; part 3: "the declaration's limit is the ceiling", said of one request. The store's map says
twice of a cost that the gates do not bound: "The door proper is the guard, later"
(`README.md:192-196`, `197-207`). The cases through the door:

- an act of more than 256 value facts: two requests or more, each lock a row for good, and then
  `:too-many-facts`;
- a value of any size: sealed, appended, kept by the depot for good, and refused
  `:sealed-value-too-large`;
- facts with no end: the call does not come back;
- a maker that sends a refused act in a loop: each try leaves a depot record, and with an empty
  stock a refused request too (matrix, D4).

- (a) Literal. The cost is said in the door's docstring and in the receipt.
- (b) The door stops in its own words where it would have to ask for more locks than the
  declaration lets one act use, or seal more than the declaration's sealed size. A limit of the
  declaration is then used by the door before a gate uses it.

Not picked where (b) reaches into D1. One part the writer holds to be engineering under part 3's
"ceiling": the door asks for no more than 256 locks for one act, since a lock past that number
cannot seal an accepted value (G9).

### P10. `answer-of`: a read that failed, the stamp, and a wait for a streaming act-id. *Silent.*

1. A read that throws is none of the four answers. "Nothing yet" would claim that the record was
   read. The writer would take a word of the door's.
2. `answer-by-act-id` gives the stamp (G4). Part 5 names four answers and no stamp. A maker needs
   the stamp to write `:based-on`, a map from a fact id to a carried stamp (act 11;
   `act.clj:147`, `175-176`), and no read of facts is built. The writer would have "accepted" and
   "refused" carry the stamp: it is part of the record's answer and no fact (D6).
3. Part 5 names the wait "for a microbatch act"; D5 is said of any wait. After a streaming append
   that threw, the answer can come later (G3). The writer would give the same bounded wait for any
   act-id.

### P11. A maker's own second send reads "not mine". *Silent.*

Part 5: "not mine — an answer under this act-id whose digest is not of what was sent." D2: "An act-id
is made once and never reused." Through the door a second send of one act with a value fact is
another thing: other locks and other random parts (G18; O3.6), and the door keeps nothing (part 5).
The proof's hand "keeps each thing that it sent, so that a repeat is the same thing byte for byte"
(`module_proof.clj:18-20`). A finding of the rig says why a sender sends again: after an error it
sends under the same id and reads the record (`src/proposal/rig-2026-09-25/README.md:350-352`).

- (a) The docstring says it. A maker that sends again under one act-id holds each digest that it
  was given; "not mine" for one of them says only that this thing was not the one decided. D2 says
  that the door never makes one id twice; it does not refuse a second send.
- (b) The door refuses an act-id that it has seen. It keeps none (D2), so it cannot.
- (c) The door keeps the sealed thing until its answer is read. Against part 5.

The writer would take (a).

### P12. The line on who reads a lock, and "the guard". *Cannot be met; and the module expects.*

"Explanations": "`module/README.md`'s line that any caller can read a lock's bytes stays true and now
names the door as the caller it meant." `src-store/softland/store/module/README.md` holds no such
line. The line is in `module/door_reads.clj:73-76` and in the store's map, `README.md:162-164`; it
says "the door proper is the guard, later": a guard, not a caller. The contract's door is the only
caller in the runtime (D8), reads only its own requests (D6), and gives a maker no lock (O3.7). It
stops no other client of the cluster: the PStates are readable and the queries are open (G24; ?11 is
open).

- (a) Both sentences are changed to say that, and that a guard of the record is still not built.
- (b) Both are left.

The writer would take (a), by the tree's rule: "Does the explanation still explain the code?"

### P13. What the door's proof must read round the door. *Two clauses.*

D8: "the door is the module's only client in the runtime [...] The module proof's hand is the one
exception." Part 7 asks the door's proof for "that a lock never crosses a who; that no lock's bytes
are ever appended; and that what the door sends for an act is what the hand sends for the same act,
but for the random parts". The door gives a maker none of what those need (O6.3). The module's proof
shows the like by reading the depots' logs and the PStates (`module_proof.clj:242-285`, `463-470`).

- (a) The door's proof reads the logs and the record as the module's proof does, and appends only
  through the door. The proofs' README names it as a second exception to D8.
- (b) The door shows a proof what it sent and which locks it holds. A maker could use the same way.

The writer would take (a). Beside it: part 7 asks for "each of the door's own words", and two words
need a case that the in-process cluster can reach: an append that throws, and the word of P14.

### P14. "A layer not made and no depot to start with" has no case. *Cannot be met.*

Part 4 names it among the steps that the door cannot do. `depot-at-making` gives a depot for
whatever it is handed (`first_act.clj:49-54`).

- (a) The word is dropped.
- (b) The word is given the case of P2: a layer of which no act-id can be made.

The writer would take (b), if P2 is ruled as (a); else (a).

### P15. Explanations that become untrue, and that the contract does not name. *Two clauses; and silent.*

Part 7: "The module proof stays as it is." "Explanations" names `door/README.md`, the tree's map, the
line of P12 and the proofs' README. `src-store/CLAUDE.md` binds every file. The sentences that the
door's build makes untrue: "The door is not built" in `act.clj:59`, `README.md:17-18` and `:25` (the
store's map), `door/README.md:7-9`, `test-store/README.md:37`, and `module_proof.clj:12`; and the
comparison that takes "the thing that the door sent" in `door/digest.clj:62-68` and
`door_reads.clj:13-15`, where the contract's door holds a digest and no thing (part 5).

- (a) Each sentence is changed in the commit that makes it untrue. In `module_proof.clj` that is one
  sentence of the docstring and no check, so its count stays 227.
- (b) Only the files that the contract names.

The writer would take (a).

### P16. A request that is sent ahead, outside any maker's call. *Silent.*

Part 3: "how far ahead it asks [...] is engineering", and "A refused request is handed to the maker".
The door's sheet: the door "asks for more before the stock runs out" (`decisions/groups/g-door.txt:101`).
A request that no call waits on has no maker to hand a refusal to.

- (a) Each request is sent inside the call of a maker whose act needs locks, and may ask for more
  than that act needs.
- (b) The door also asks between calls, with the parts of the last act of that layer and who. A
  refusal is then dropped, or held for a later maker, whose act may name another permission
  (O3.11).

The writer would take (a).

### P17. The ack level of the microbatch depot. *Silent.*

Part 4: "the microbatch depot with the ack the module's docstring describes". The docstring says
what the ack says and names no level (`module.clj:63-65`). The hand uses `:append-ack`
(`module_proof.clj:310`); Rama's documentation says that `:ack` is the same where no stream
topology reads the depot (`14-depots.md:206`). The writer would take `:append-ack`.

### P18. A lock's bytes outside a depot. *Silent.*

Part 3 and rule 5 keep a lock's bytes out of a depot. The contract does not say that they stay out
of the door's own words, of what the door prints or throws, and out of a maker's reach. The writer
would hold all three (O3.7). It is care and no guard: any client of the cluster can read a lock
(G24; rule 16).

### P19. What goes plain into a depot through the door. *Silent; for Sid to know.*

The door seals the `:v` of a fact under a plain key, when the fact is a plain map in a sequence
under `:facts`. All else that a maker gives is appended as given, and the depot keeps it for good
(G1): a value under a store key, whatever it is (O4.2); a value inside `:facts` that is no sequence
(row 8); what a fact that is no map holds (row 10); what a part that the form does not know holds
(row 17); and each id. The gate refuses the three rows; the log keeps them all the same.

- (a) Literal, and said in the door's docstring and in the receipt.
- (b) For row 8 alone: the door says that it could not find the facts, and appends nothing.

The writer picks nothing here: (b) is one more test of form at the door (D1), and the rest cannot be
closed by the door at all.

### P20. The sequence of facts that the door sends. *Silent.*

Part 4: the door "never reorders facts" and "changes the act only by the three additions". To seal,
the door builds the facts anew. A vector and any other sequence have two texts (`text.clj:31-32`),
so two digests. The hand sends a vector (`module_proof.clj:170`). A lazy sequence through Rama is
reached by no proof (`FULL_SPEC_REVIEW.md:255-256`).

- (a) The door sends the facts as a vector, whatever sequence the maker gave. A gate's one form
  holds them as a vector too (`act.clj:311-320`).
- (b) The door keeps the maker's kind of sequence, and makes a lazy one whole before the digest.

Either meets O4.7. The writer would take (a), as the hand does.

### P21. `act-id-for` when the read of the layer's depot fails. *Silent.*

Part 2 gives the starting depot "when it is not made". A read that threw has not said that.

- (a) A word of the door's, and no id.
- (b) The starting depot. Right in this build (G16); wrong on the day that a depot can change (?9).

The writer would take (a).

### P22. No closing, two opened doors, and who counts the rows left. *Silent.*

Part 1 opens the door and nothing closes it. Part 3: "the receipt counts the rows it left."

- Who counts: (a) the door's proof, from the record, round the door (P13); (b) the door says how
  many locks it holds unused: a count, never bytes.
- Two opened doors in one process are two stocks (O1, Concurrency). The docstring says that a
  process opens one.
- The end of a process is a restart for the stock (O6.4).

Either way of counting meets part 3; it is the plan's.

## What the module's explanations expect of the door

Each sentence of the tree that says that the door is not built, or that leaves something to it,
against what the contract says the door is and is not.

| The sentence | Where | What it expects | What the contract gives |
|---|---|---|---|
| "Any caller of the module can read a lock's bytes in this build; the door proper is the guard, later." | `module/door_reads.clj:73-76`; `README.md:162-164`; `BUILD-store-module.md`, rule 16 | A door that stands between a lock's bytes and every other caller | No guard. The door is the one caller in the runtime (D8) and reads only its own requests (D6). **P12** |
| "What an act with no permission costs the microbatch gate [...] The door proper is the guard, later." | `README.md:192-196` | A door that keeps an act without standing from the microbatch gate | Nothing: D1; and D6, since a permission is a fact of the record and the three queries read none. As it falls out: a value act of such a who stops at its request when the stock is empty; an act with no value fact goes through. **P9** |
| "How long one act can hold a task of the streaming gate [...] The door proper is the guard, later." | `README.md:197-207` | Acts whose locks are few requests in tight runs, or a bound | The stock is the plan's (part 3). O4.14 carries the expectation for the door's own acts. What appends without the door is not held by it. **P9** |
| "A door that uses a request's locks in order reads exactly the locks that it names." "My pick: (b), in a later change, with the door proper." | `store-module/FULL_SPEC_REVIEW.md:124-132` (a paper of the module's build) | A change of the gate's read of locks to come with the door | "Any change to the gates" is outside this build. A finding for the receipt |
| "The door is not built." | `act.clj:59`; `README.md:17-18`, `:25`; `door/README.md:7-9`; `test-store/README.md:37`; `module_proof.clj:12` | Nothing | Untrue after the build. The contract names one of the six. **P15** |
| "The sender compares the digest with the digest of what it sent"; "The door's comparison. Takes [...] the thing that the door sent." | `door_reads.clj:13-15`; `door/digest.clj:62-68` | A door that holds the thing that it sent | The door keeps nothing; the maker gives the digest back (part 5). **P15** |
| "It keeps each thing that it sent, so that a repeat is the same thing byte for byte." | `module_proof.clj:18-20` (the hand) | A repeat that is the same thing | Through the door a repeat is another thing. **P11** |
| "When there is no answer and the door is in doubt [...] It sends the same thing again under the same act-id." | `store-module/IMPLICIT_SPEC.md:115-116` (a paper of the module's build) | A door that sends again by itself | "The door retries nothing on its own" (part 3), and it keeps nothing. **P1**, **P11** |
| "Which plain bytes stand for a value: the door's." | `door/seal.clj:51-52` | The door says which | Part 7 holds it to the hand's: O4.3. The form of a value: **P3** |
| "The door may warn early; the gate is the authority." | `src-store/README.md:164-167` (Sid's F8 C) | A door that may refuse before the gate | D1: the door checks nothing. **P3**, **P9** |
| "a caller that may hold a thing that is no id tests it with the same functions before it calls" | `door_reads.clj:69-71` | A caller that requires the record's rows | "Nothing of the gates' or the record's internals." **P6** |
| "20 ms from the door's append to the answer in the door's hand" | `README.md:493-494` | An aim that counts the door's read of the answer | No number in the contract. O4 and O5, Latency |
| "Anything can arrive: the door is not the only sender." "The door seals and sends; it is the gate that checks." "reads the answer by act-id in a loop with a time limit, as a door would" | `streaming_gate.clj:10`; `declaration.clj:52`; `module_proof.clj:24-26` | What the contract also says | The same: "What the door is not"; D1; part 5 |

## The open rows that this paper stops at

None is decided here. Each is named where a requirement would need it.

- **K9**, who may act as the system: O6.2. **K7, K8, K10, Q16**, persons and what is made from a
  person: O2.3. **E5, E6, Q19**, sessions: P4.
- **W3**, where a read's steps run, and **W4**, what tells a screen that a fact changed; group 6:
  O5.6, O5.10.
- **K11, H10, H11**, forget; the switch of a layer to one lock for each act: O3.5, O3.10, P7. The
  door's docstring leaves the room, as the contract says.
- **?9**, a layer whose depot changes: O2.2, P21. **?11**, a record that only the gates can read:
  P12. **K5**, what a key is, and **K15**, a namespace under the store's: the door tells a store
  key by its being one of the three (G10), as the gates do; O4.2.
- **F9**'s targets are aims and are not measured (G22). No requirement here turns an aim into a
  number that the door must meet.
- A network edge (group 7), a first maker, and promotion (?10): nothing here needs them.

## What could not be settled by reading

1. Whether one handle of Rama's client, of a depot or of a query, can be used by many threads at
   once. The local documentation speaks of it for serialisers and for clients of other systems only
   (`reference/rama/docs/17-serialization.md:160`; `25-integrating.md:22`). The proof's hand is one
   thread.
2. What Rama's client does with a thing that it cannot turn into bytes, and what a worker does with
   bytes of a Java type that it does not have. The door seals `:v` only (P19), so any other part of
   a maker's thing reaches Rama's serialiser as given. If a worker throws while it reads such a
   record, no gate code has run, and the rig's finding is that a throw in a topology stops the worker
   (`src/proposal/rig-2026-09-25/README.md:343-346`). The module's review traced one kind only, a
   map whose keys cannot be compared (`FULL_SPEC_REVIEW.md:252-254`). On the in-process cluster the
   door and the module share one JVM, so a run there cannot show the second half.
3. Whether an append that throws can be made on the in-process cluster without a fault of the
   cluster, for the proof of that word (P13).
4. How long a query waits before it throws here, and so how far the bound of a wait reaches in
   seconds (O5.7). The rig met a read that stopped at 10 seconds (`RIG.md:819-823`).
5. Whether the digest that the door takes of a thing whose `:facts` is a lazy sequence is the
   gate's (P20).
6. What Rama's client does with a first argument of a query that it cannot hash (G6).
7. Whether a module that is started again in a real cluster leaves a client's handles good, as the
   in-process cluster does (G25). The rig's finding: a module update drains and is no failover
   (`src/proposal/rig-2026-09-25/README.md:371-372`).
8. The row A5 of the matrix, an act of the base in a batch after its making's and before that
   batch is closed: derived from the gate's walk (`microbatch_gate.clj:98-100`), not read in a
   check.

## What was read, and what was run

Read whole, in the contract's order: the contract; `src-store/CLAUDE.md`; `src-store/README.md`
and `decisions/groups/g-door.txt`; `act.clj`; `door/README.md` and its four files; `module.clj`,
`module/door_reads.clj` and `module/README.md`; the hand, `module_proof.clj:1-503`;
`decisions/OPEN-QUESTIONS.md`; `store-module/PROGRESS.md` and `store-module/FULL_SPEC_REVIEW.md`; the
Rama skill, its Phase 0 page, its template and `foreign-client.md`.

Read beside the list, because a claim needed the lines: the store's map
(`src-store/softland/store/README.md`), whole; `gate/first_act.clj`, `gate/decision.clj` and
`record/rows.clj`, whole; the namespace docstrings of the two gates; `first-entity` in
`gate/batch.clj`; rules 1 to 21 of `BUILD-store-module.md`; the proofs' README from "The module's
proof" on; seven checks of the module's proof (`module_proof.clj:504-630`, `800-950`, `1478-1501`);
`door/PROGRESS.md`; two passages of `store-module/IMPLICIT_SPEC.md` (`97-117`, `1479-1495`); and five
pages of the local copy of Rama's documentation, by search (`11`, `13`, `14`, `17`, `25`).

Read last, by search and as findings only: the rig's `README.md` (`313-378`, `390`) and `RIG.md`
(`711-830`, `2165-2180`). No form, name, call shape or default of the rig's code, the server's or
Inland's is in this paper, and none of their code was read.

Not read: `gate/batch.clj` beyond `first-entity`, `gate/stamp.clj`, `record/tables.clj`, the two
gates' dataflow below their docstrings, the plans and checks of the module's build, the act proof,
the pure proof, and the module proof's checks other than the seven.

Run: nothing. No proof, no probe, no build. The count lines in this paper are the build session's.


## Rulings of the build session on P1 to P22

By the build session "door build" (Opus 5.5, effort xhigh), 9 October 2026. It read this paper's
operations, the table of things that are no act, and the open points, whole. It checked in the source
the facts that a ruling rests on: that a gate does not check what a lock opens, and does not hold a
lock to who asked for it (`gate/decision.clj`, `seal-fault` and `seals`; the module proof's check "a
lock is bound to its layer, not to who asked for it"); that a stray is told before anything else and
gets no answer (`door/declaration.clj`, `stray-reason`); what the one text gives for a thing that is
no value (`door/text.clj`); and Rama's own words on an append that throws
(`reference/rama/docs/14-depots.md:212-214`). None was false.

Each ruling is a requirement for the plan, the build and the review, beside the contract. A ruling is
the build session's. It is never a decision of Sid's, and each is his to strike. "For the receipt"
marks a place where the contract is not met as written.

Two rules hold most of the rulings.

**Rule A. The door makes each of its own additions right, or it says that it could not and appends
nothing.** The door's additions are three (act 2): the act-id, the depot, and the seal of each value.
An addition is right when the door's own declaration takes it (`door/declaration.clj`), and when it
wrote over nothing that the maker gave. This is what D1 names: "what the door could not do". It is no
check of the act: under rule A the door tests nothing that a gate can see and judge.

**Rule B. A value that the door finds under `:v` is sealed so that it reads back, or the act is not
appended.** The door finds a value in a fact that is a map, in `:facts` that is a sequence. The one
text reads back for a value of the form and for nothing else (`door/text.clj`), and after the seal no
gate can tell (`gate/decision.clj`, `seals`). So the door seals a value of the form, by
`softland.store.act/value?`, the one function that says what a value is. Rule B narrows D1 by this
one corner. It is the build session's working answer, and the first row under "Rows that the build
proposes" is the question behind it.

The rulings, by point:

- **P1: (a).** The door's word for an append that threw says that the door does not know whether the
  depot holds the thing. It carries the act-id and the digest, so that the maker reads the record
  (F7 A; D4). The same holds for a request's append that threw: the word says so of the request, and
  nothing is appended for the maker's act. For the receipt: "it appends nothing for that act" cannot
  be true of this one word.
- **P2: (a), by rule A.** The door appends a thing only when a gate can answer it under its act-id.
  It tests what its own addition needs: the thing is a plain map; its `:layer` is an id that an
  act-id can hold; an act-id given back has the shape of one and is of that layer. An act-id given
  back whose depot slot is not the layer's depot goes to the depot that it names, and a gate answers
  it. For the receipt: "a malformed act goes in and comes back as the gate's refusal" cannot hold for
  a thing that would be a stray.
- **P3: by rules A and B.** By A: an act that already carries `:act-id` or `:depot`, and a fact that
  carries a value beside `:sealed` or `:lock-id`, are things to which the door could not add its
  part. By B: a value that is no value of the form, and a value in a fact that is a map and no plain
  map, are values that the door could not seal. Each ends in the door's own words, and nothing is
  appended. A fact that carries `:sealed` or `:lock-id` and no value goes as given: the door adds
  nothing to it, and the gate judges it. For the receipt: the same sentence of part 4, and D1.
- **P4: (a).** The door also reads `:who`, `:permission`, `:version` and `:session`, to make the
  request and to choose the stock. It copies them as given and checks none.
- **P5: (a).** One word of the door's, which carries the gate's reason word for the request and the
  request's act-id. Nothing is recorded under the act's own act-id, and the word says so. The
  docstring says that one faulty act has two outcomes by the state of the stock.
- **P6: (a).** "Internals" are the PStates, the rows and the topologies. The door opens no PState,
  makes no key of a row and no row. It may require the pure functions that each hold one rule that
  the door needs: of `act.clj` and `door/`; `depot-at-making` of `gate/first_act.clj`, which part 2
  names; and from `record/rows.clj` the routing id and the three tests of what a caller passes to a
  read, which `module/door_reads.clj` tells a caller to use. The door's docstring names each and
  why. No rule is written a second time.
- **P7: (a).** A lock that was taken never comes back to the stock. The receipt counts such rows
  with the rows of a restart, and says what would change it.
- **P8: (a).** The door's wait for its own request has a bound of the door's own, a number with its
  reason. When it passes: a word of the door's, and nothing is appended. The door remembers the
  request and reads its locks at a later need. The plan may let the opener of the door give the
  number.
- **P9: literal, with two bounds, each of a step of the door's own and each read from the
  declaration in one place.** (1) The door takes for one act no more locks than the declaration lets
  one act hold facts: a lock past that number cannot seal an accepted value. An act with more value
  facts ends in the door's own words; no lock is taken and nothing is appended. (2) By rule A the
  door appends no `:sealed` that is over the declaration's limit; such a value is one that the door
  could not seal. All else is as given, and the docstring says what it can cost: facts with no end
  (the plan says whether the first bound ends that walk), and a refused act sent in a loop. The two
  sentences of the store's map that end "The door proper is the guard, later" are made true: the
  door as built is no guard of a gate. For the receipt.
- **P10: the three picks.** A read that failed is a word of the door's, and never "nothing yet".
  "Accepted" and "refused" carry the stamp of the record's answer. "Not mine" carries nothing more.
  The bounded wait is the same for any act-id.
- **P11: (a).** The docstring says it, and says how a maker that sends one act again under one
  act-id reads the record: it holds each digest that it was given; at most one of them reads an
  answer that is its own, and each other reads "not mine". D2 means that the door never makes one id
  twice.
- **P12: (a).** Both sentences are made true: any client of the cluster can read a lock's bytes; in
  the runtime the door is the one client, reads only its own requests and gives a maker no lock; a
  guard of the record is not built (?11 is open).
- **P13: (a).** The door's proof reads the depots' logs and the record round the door, as the
  module's proof does, and the proofs' README names it as a second exception to D8. It sends through
  the door for everything that the door covers. Each of the door's own words has a case on the
  in-process cluster. A real throw of Rama's client is wanted for the word of P1; when none can be
  made there, the proof makes the append throw itself and its line says so.
- **P14: (b).** The word has the case of P2: a layer of which no act-id can be made. For the
  receipt: the contract's case, as worded, cannot come about.
- **P15: (a).** Each sentence that the door makes untrue is changed in the commit that makes it
  untrue, in `src-store/` and in `test-store/`. In `module_proof.clj` that is its docstring and no
  check; its count stays 227. No code of the gates, the record or the declaration changes.
- **P16: (a).** Each request is sent inside the call of a maker whose act needs locks, and may ask
  for more than that act needs. The door has no thread of its own.
- **P17:** `:append-ack`.
- **P18:** all three hold. A lock's bytes are in no word of the door's, in nothing that the door
  prints or throws, and in nothing that a maker gets.
- **P19: (a), literal.** What a thing holds outside a `:v` that the door finds goes as given, and
  the depot keeps it. The docstring says it. For the receipt, as the second row below.
- **P20: (a).** The door sends the facts as a vector, each fact at its position. The door's proof
  shows that the digest given to the maker is the gate's, for facts given as a vector, as a list and
  as a lazy sequence.
- **P21: (a).** A word of the door's, and no id.
- **P22.** No closing in this build: the door borrows its handles, and the opener owns the cluster
  handle. The docstring says that a process opens one door. The rows left are counted by the door's
  proof from the record, round the door. The door may say how many locks it holds, as a count and
  never as bytes; that is the plan's.

On what could not be settled by reading:

- Whether one handle of Rama's client can be used by many threads: the plan finds it in Rama's
  documentation, or the build probes it. The door's proof sends from many threads at once.
- What Rama's client does with a thing that it cannot turn into bytes: the build probes it on the
  in-process cluster. What a worker does with bytes of a Java type that it does not have cannot be
  shown there. For the receipt, with the second row below.
- How long a query waits before it throws: the plan names the setting, and the docstring of the wait
  says how far the maker's bound reaches.

### Rows that the build proposes for the queue, decided by nobody

1. **Who holds the form of a value once it is sealed.** Act 3 put the seal below the form. A gate
   opens each sealed value once (rule 16) and does not read what it opens. The answers: the gate
   reads what a lock opens, for which the plain bytes of a value become a thing that the door and
   the gates agree on; the door, before it seals (rule B, as built here); both; nobody.
2. **Whether the door runs the maker's check (`softland.store.act/parse`) before it seals and
   appends.** Sid's F8 C says "The door may warn early; the gate is the authority". The contract's
   door checks nothing (D1). What the check would close: plain content of a refused thing that the
   depot keeps for good (P19); a thing of a type that a worker does not have; what a thing can cost
   before a gate refuses it (P9). What it costs: a fault of form is then the door's word, kept in
   no record, and each act is walked once more.
3. **Whether a lock of an act that was not accepted comes back** (P7). As built it never does, and
   each such value leaves a row that seals nothing. To give it back the door must remember which
   locks an act took until its answer is read.
4. **What guards a gate from an act that costs much** (P9): the two costs that the store's map left
   to "the door proper".
