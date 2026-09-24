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

---

Derived 25 September 2026 by Claude Opus 5.5 (1M context), effort max, in a
fresh-context Phase 0 session. Requirements only. No PStates, depots,
topologies, partitioning or schemas are chosen here. A mechanism appears only
where a source rules it, and the source is named beside it.

## 0. Scope, sources, and how each line is marked

**Scope.** The whole store the rig builds: Sid's phases 1 to 6 in SPEC.md
(stream store; locks and forgetting; micro store; promotion; reads and read
entries; tools and grammars from inside). Phases 7 and 8 enter only as the
operations and scale facts they imply (the section "Measurement and replay").

**Sources.** SPEC.md, and the sources it makes part of the spec: PROGRESS.md
"Now" (lines 9 to 195); the formal model README, lines 42 to 152; model.clj
in full, whose `baseline` configuration (`ruled` with every entry of
`readings` turned) is what the rig implements; scenarios.clj (the A, B and D
cases); SPEC.md's "What Rama showed" (binding on the design); RIG.md's rig
choices. Where SPEC.md summarises a source, the source wins (section 2 lists
every difference found).

**Citation tags.** `[P n]` PROGRESS.md line n. `[R n]` model README line n.
`[M fn]` the model.clj function fn. `[S A1..A8, B1..B4, D1..D2]` the cases in
scenarios.clj, in file order. `[SP n]` Sid's phase n in SPEC.md. `[RQ n]` item
n of SPEC.md's "What Rama showed". `[RIG Rn]` a rig choice.

**Marks.** *ruled*: a source states it. *model*: model.clj's baseline decides
it this way, and SPEC.md says the rig's gates must decide the same way.
*derived*: follows from ruled or model lines; the derivation is given.
*assumed*: a senior engineer's expectation the sources do not state (most
latency and volume figures). *open*: the sources are silent; the plan picks,
and writes the pick in RIG.md as a rig choice, not a ruling. Open items are
numbered O1, O2, ... and collected at the end.

**Vocabulary.** "Key" is a fact's key, the "in this respect" part. "Lock" is
an encryption key. The sources' "keyed fingerprint" means a fingerprint taken
under a secret; this file calls that the fingerprint secret, never a key.

## 1. The nouns, as the requirements use them

- **Offer.** What an offerer sends to a gate: one act under one name, naming
  the act's layer, the class it expects, the permission it acts under (none
  for the operator and the store's own steps), what it stood on, what it is
  because of, and its facts. [P 48-58] [M op-offer]
- **Name.** Random, made by the offerer before the gate, tagged with the
  layer and class it was made for, so it reaches one gate only. Two schemes
  are reserved to the store: the read-out's name and the landing's name, both
  derived from the promotion request's name (the model writes them
  `crossing:<request>` and `landing:<request>`). [P 58, 129-135]
  [M crossing-name, landing-name]
- **Answer.** Yes or no, the reason for a no, the stamp, the digest of the
  offer it was given for, and what the offer was because of. Recorded where
  the offer was decided; found by name plus layer. [M decide, lookup]
- **Act.** Admitted whole or not at all. Act id and index on every fact; who,
  when and the reads on the act once; the layer on the act. Any size, one fact
  included. [P 53-55, 127-128] [SP 1]
- **Fact.** A positional core (entity, key, value slot, act id and index,
  replaces) plus named parts the store owns (by whom, layer, based on, because
  of, permission, session, claimed when, expected versions, subjects) and a
  version marker. [P 48-52]
- **Chain.** The facts of one layer, entity and key, linked by `replaces`;
  its head is the latest fact nothing replaces. [M chain-head, replaceable?]
- **Key.** An id. Its word and its grammar are facts about it: which subjects
  its values name, and whether its values are opaque. [P 65, 87-90] [SP 6]
- **Layer.** Four kinds: base, group, personal, session; a session is hand or
  agent, set by its session-start fact. One-owner layers: personal and both
  sessions. Shared: group and base. Settings are facts on the layer: class (by
  layer, by entity) and lock grain (per value, per act). [P 71-74, 96-100,
  109-112]
- **Permission.** A fact in the layer it governs. The session is the root;
  narrower permissions are ordinary facts under it. A group layer's root
  permission is made with the layer, naming the group. A revocation is a fact
  in the permission's layer, standing on the permission. [P 56-57, 110-112,
  159-161] [R 72-76]
- **Value lock.** Each value's own small symmetric lock, made at write ("real
  encryption, kept simple" [SP 2]), wrapped under the person locks of the
  people the value is about. Per value by default; per act under the layer's
  grain setting. [P 91-100]
- **Lock row.** A wrapped value lock kept as its own row in the lock store:
  personal and hand layers, and any value whose mark asks for it; on the same
  task as its value. Otherwise the wrapped lock is kept in the record: agent
  sessions, group layers, the base. [P 92-96] [SP 2, 3] [M lock-for]
- **Person lock.** One per person. Destroying it is a person forget. [SP 2]
- **Wrap.** Which person locks a value lock opens under, as required people
  and any-of people, fixed at write. [M wrap] (rules in I-L1 below)
- **Marks.** Set on a value at write: die-with-any (7b) and own-row (the lock
  placement override). The model has exactly these two. [P 95, 101-102]
  [M lock-for, wrap]
- **Subjects.** The people a value is about, from three sources: the layer's
  owner, the key's grammar, the tool. The act's subject slot is the union, for
  finding. [P 103-104, 138-141] [M fact-subjects, act-subjects]
- **Forget.** Value forget: deletes the lock row, or, for a lock kept in the
  record, the operator excises it. Person forget: destroys the person lock.
  Every forget is a fact and names a permission. [P 60-62, 97, 161] [SP 2]
- **Retract.** A new fact, undoable; time travel shows it. [P 60-61]
- **Erasure.** A value that can no longer be opened. Reads show "erased on
  <date>" and nothing else about it from after the read's moment. [P 61-62,
  146-148]
- **Promotion.** A request act in the owner's layer; the read-out, the store's
  own step, which opens the value through its lock on the owner's task and
  writes the crossing fact there; the landing offer into the shared layer.
  Statuses: pending (not read out, still forgettable), crossed (read out, not
  landed), done, refused. [P 114-116, 150-158] [SP 4] [M promotion-status]
- **Stamp, moment.** The store's time on an act, under the clock promises. A
  read as of a moment T. [P 85-86]
- **Read entry.** Rows for point reads; one line per pattern read carrying
  the pattern, the moment, the role and a fingerprint of what matched, with a
  complete-or-partial mark. [P 75-84] [SP 5]
- **Tool.** Facts: what it matches, its signature, its read entry preference.
  A minimal runner finds tools by matching. [SP 6]
- **Operator.** Trusted at launch, no signing. Acts at the root, as the
  store's own steps do; names no permission. [P 63-64] [M exempt?]

## 2. Where SPEC.md and its sources differ, or the sources differ among themselves

Each is carried into the requirements below as noted. None is resolved here;
the ones marked "for Sid" change what a ruling says and are his to settle.

- **D1.** SPEC.md lists R7, R8 and R9 under "Rig choices already made (from
  RIG.md)". RIG.md as it stands lists R1 to R6. R7 to R9 exist only in
  SPEC.md. Taken as binding, because SPEC.md states them.
- **D2 (for Sid).** Ruling 7 says every value gets its own lock [P 91]. The
  model gives no lock to the facts the store acts on (forget, lock grain,
  class, promote request, crossed, permission, revoke) [M control-keys]. The
  rig follows the model: those facts are never erased by any forget, which the
  gates need (a permission erased with a person would stop the gate from
  checking it). The ruling's wording and the model differ.
- **D3.** Phase 2 and the rig constraints say every forget is a fact [SP 2]
  [P 169]. In the model a person forget is a lock-store step with one global
  stamp, not an offer, in no layer, naming no permission [M op-forget-person]
  [R 106]. The rig must write a fact; where it lives is open (O12).
- **D4.** The model replays only offers begun and not finished [M failover].
  Rama replays records that had already completed since the last checkpoint,
  at least once and possibly more than once [RQ 2]. The answer record must
  therefore cover finished offers, by name (RIG.md's finding 2 says the same).
- **D5.** The model's micro gate commits a batch in one step [R 48]. Rama
  makes each task's part visible when that task's commit finishes [RQ 5]. The
  rig reads the micro store through a settled frontier [RIG R5].
- **D6.** In the model the stream gate reads other stream partitions at once,
  and both gates read one settings map at once [R 101-106]. In the rig each is
  a read at a moment. The README reasons that a stale class view only refuses;
  that reasoning does not cover a stale lock-grain view, which admits at the
  wrong grain instead of refusing (see OP8).
- **D7.** The model has leaders, epochs, fencing, deposed-leader steps and a
  failover clock skew of up to 3 [M failover, zombie]. Rama has no leader epoch
  the rig controls, and the in-process cluster runs replication factor 1
  [RIG.md claim 2]. Those model ops have no direct analog; phase 8 reports
  each as a difference, never a silent skip.
- **D8.** The model's offer digest is a plain hash over the offer, values
  included [M digest-of]. The rig constraint says fingerprints over values are
  keyed [P 170]. The digest kept in every answer record is a fingerprint over
  values, so the constraint covers it.
- **D9.** SPEC.md phase 2 places lock rows for personal and hand layers and
  "in the record for agent layers" without the mark override. Ruling 7 says a
  mark on any value overrides [P 95], and phase 2 says "marks honoured". Not a
  conflict: the override applies in every layer.
- **D10.** SPEC.md phase 5 names the pattern-read line but not ruling 3's
  "rows for point reads" [P 75]. The ruling binds: point reads leave rows.
- **D11.** In the model a lock-grain setting cites the layer owner's
  permission, so on group and base (no owner) it is refused for want of a
  permission [M op-layer-setting]. Ruling 7 calls grain "a setting on the
  layer, itself a fact the person can change" [P 98], silent on shared layers.
  The rig follows the model unless a rig choice says otherwise (O7).
- **D12.** Under per-act grain the model keeps one lock per act per partition
  (`[:act name p]`) and computes each fact's wrap from that fact's own mark
  [M lock-for]. Ruling 7 says one lock "shared by all values in an act,
  forgettable only as a whole" [P 98-100]. The two part where an act spans
  tasks (only after a re-class) and where one act mixes marked and unmarked
  values. See I-L6.

## 3. Requirements on every operation

Gates, answers and names

- **I-G1 (ruled).** A gate never throws on an offer. Every refusal is data
  with a reason. An exception in topology code restarts the worker [RQ 3].
- **I-G2 (ruled).** Every gate write is idempotent under replay. A replayed
  offer, finished or not, gets the same answer, the same stamp and the same
  admitted facts as its first decision; nothing is admitted twice, no stamp is
  given twice. Tests assert "at least once", never an exact replay count.
  [RQ 2] [RIG R4]
- **I-G3 (ruled).** An offerer who gets an error for an offer resends under
  the same name and takes the answer from the record. [RQ 4]
- **I-G4 (ruled, model).** An answer is found by name plus layer: on the
  layer's home task when the layer is placed by layer; through a name row the
  micro commit writes in the same batch when placed by entity. One read when
  the name's tag gives layer and class; two (the layer's home, then the name
  row) for an offer the store places itself in a one-owner layer (a forget, a
  setting, a read-out), whose name carries a layer and no class. Never a scan.
  [P 133-135] [R 68-70] [M lookup, tag-of]
- **I-G5 (model).** Order of a gate's checks. First, the name's tag against
  the offer's layer and class: a mismatch is refused on its face, reading
  nothing. Then a record under this name with another digest: refused, the
  name is taken. Then a record with the same digest: the recorded answer, and
  nothing is decided again. Otherwise the gate decides, and the first failing
  check gives the reason, in this order: a fact outside the act's layer; a
  class that differs from the layer's class fact (not checked for a
  read-out); a permission that does not cover the write (its person and layer
  against the offer's); a permission kept in another layer; a permission that
  does not exist; a revoked permission; a stale or doubled replace; a read-out
  whose source has no value; a read-out whose source is erased. The operator
  and the store's own steps skip the four permission checks. [M stream-step,
  micro-decision, refusal]. The rig's own further refusals (unknown envelope
  part, unknown version marker, a reserved scheme used by an offerer, a value
  failing its key's shape, an empty act) are data too; where they sit in this
  order is open (O1).
- **I-G6 (ruled).** Acts are admitted whole or not at all, on both gates; the
  act's facts share one stamp. [SP 1, 3] [P 53] [M decide]
- **I-G7 (ruled).** Names: random, made by the offerer, tagged with the layer
  and class they were made for; a reuse with different content is refused by
  a digest of the offer; an act that causes further offers names each of them
  before the first gate; a promotion's landing is named from the request's
  name under a scheme reserved to the store. [P 58, 129-135] [SP 1]
- **I-G8 (ruled).** The envelope: small positional core, the named parts the
  store owns, a version marker; the gate refuses parts it does not know. Tools
  add facts, never envelope parts. [P 48-52] [SP 1]
- **I-G9 (ruled).** Per stream task: the log, answers by name, the task's
  stamp. [SP 1]

Placement and order

- **I-O1 (ruled).** Stream gate for one-owner layers, microbatch gate for
  shared ones; two fact stores, each with its own gate. One-owner layers are
  placed by layer, shared layers by entity. N, the task count, is fixed at
  launch and chosen for two years. The class is on the offer; the gate checks
  it against the layer's class fact. A hot layer can be re-classed; a
  re-classed one-owner layer moves to the microbatch gate, and its
  single-owner order promise ends at the re-class. [P 68-74, 136-137] [SP 1, 3]
- **I-O2 (ruled).** Clock promises: a stamp never goes backward within a unit
  and is never earlier than anything the fact stood on. The model reads "unit"
  as the partition and also keeps each stamp at or after the gate's wall clock
  [M stamp-for]. What an act stood on includes its reads, the fact it
  replaces, a promotion's source and, for a landing, the crossing fact.
  [P 85-86] [R 59, 93]
- **I-O3 (derived).** Stamps from the two stores are comparable on one scale:
  a read as of T covers both stores [M read-as-of], and a landing's stamp must
  be later than its crossing fact's stamp from the other store [R 93].
- **I-O4 (ruled).** Order between the two stores exists only through
  stood-on. A check one gate makes against the other store is a read, stamped
  in the checking store, never atomic with the other's writes; the store
  promises nothing that needs such an order. [P 162-165] [R 80-86]
- **I-O5 (ruled).** Nothing may rely on seeing a microbatch on every task at
  once; the micro store is read through a settled frontier, so no reader sees
  half a batch. The next microbatch starts only after every task committed the
  current one. [RQ 5, 7] [RIG R5]
- **I-O6 (ruled).** No optimism: a person's view shows only what a gate
  admitted; a promotion shows as pending until it lands. [P 115-116]

Locks and forgetting

- **I-L1 (ruled, model).** The wrap, fixed at write, per value from that
  value's own subjects [P 138-141] [M wrap, baseline `:wrap :owner-required`,
  `:lock-subjects :fact`]:
  - one-owner layer, unmarked: requires the owner's person lock only; the
    value dies with the owner and survives every other subject (7b among the
    others; Sid, 25 September) [R 144-147] [S A1, A8];
  - one-owner layer, marked die-with-any: requires the owner and every other
    subject;
  - shared layer, unmarked, about one person: dies with that person [S A2];
    about several: opens while any one of them is not forgotten, dies when all
    are [S A4-A6];
  - shared layer, marked: dies with any subject [S A7];
  - about no one (shared layer, no subject): no person forget closes it; only
    a value forget or an excision erases it [S A3].
- **I-L2 (ruled).** Lock rows sit on the same task as their values. [P 96,
  168] [SP 2]
- **I-L3 (ruled).** One log. Erasure is by destroying a lock, never by values
  kept beside the log. Locks are for forgetting, not hiding from the
  operator; the gate reads every value it matches on. [P 45-47]
- **I-L4 (derived).** After a value's lock is gone, nothing the store keeps
  holds that value's plaintext: not the log, not an index, not the queue of
  offers a gate reads from, not a follow-on offer carrying a copy. From "one
  log, erasure by destroying a lock, not values beside the log" [P 45] and
  "gone for everyone including the past" [P 61]. The model does not count its
  queues and sent-offer maps as the store; in the rig the offers a gate reads
  are part of the store. How is open (O2).
- **I-L5 (ruled).** Every index over values is rebuildable from the log or
  purgeable by value id, so a forget reaches it. Fingerprints over values are
  keyed. Every forget and every restore is a fact; forget facts are replayed
  after any restore. [P 167-170] [SP 5]
- **I-L6 (ruled, with D12).** Lock grain is per value by default; a layer
  setting switches to per act, one lock for all values in an act, forgettable
  only as a whole; it affects only values written after the switch. A per-act
  lock is wrapped under the act's subject union. [P 96-100, 144-145] Where an
  act's values sit on several tasks, or its values carry different marks, the
  model and the ruling part (D12); the rig matches the model or reports the
  difference (O8).
- **I-L7 (ruled).** Time travel: a read as of a moment shows nothing admitted
  after it, except an erasure, which shows only its date. [P 146-148] [SP 2]
  [M read-as-of, `:p4-erasure-exempt true`]
- **I-L8 (ruled).** "Never rewritten" is about meaning, not bytes:
  re-encoding is allowed; removal is allowed when the removal is a fact;
  nothing comes back different. [P 43-44]

Permissions, visibility, ownership

- **I-P1 (ruled).** Every offer by a person names the permission it acts
  under; the operator and the store's own steps act at the root and name none.
  A permission lives in the layer it governs; a gate checks only permissions
  in layers it orders; an offer citing a permission kept in another layer is
  refused. A forget names a permission too. [P 56-57, 159-161] [R 73-75]
- **I-P2 (ruled).** Default visibility: the base to any authenticated actor;
  a person's own and session layers to that person; group layers to the
  group's members. Seeded as policy facts, so a store can differ. Every read
  below applies it. [P 105-107]
- **I-P3 (ruled).** Owner is derivable at read time from author, layer and
  permission; every act carries those three. [P 59]
- **I-P4 (ruled).** The operator is trusted at launch; no signing, and no
  slot for one. [P 63-64]
- **I-P5 (ruled).** A pick that decides where or how something is written
  sticks for what was written under it; a pick applied at read can change.
  [P 38-40] It governs lock grain, wraps and subjects (fixed at write), class
  (earlier facts stay where they were placed), and read-time picks such as a
  tool's read entry preference.

## Operations

Latency lines marked *assumed* are the expectations the choice of gate
implies; no source gives a number. Phase 7 measures on the in-process cluster,
where only orders of magnitude mean anything [SPEC "Tests and evidence"].

### Writes

#### OP1. Offer an act into a one-owner layer (the stream gate) [SP 1]

- **Latency (assumed).** A person's own write is answered, and visible to
  that person's next read, at interactive latency: single-digit to low tens of
  milliseconds on real hardware. No optimism (I-O6) means the person sees only
  admitted facts, so the gate's latency is the person's write-to-see latency.
  Phase 7's third number measures acts per second and latency of one person's
  layer on one thread.
- **Throughput.** Driven by people's own writing and, above all, by agent
  session layers: "a stream is many small acts" [P 54-55]. One one-owner
  layer's rate is bounded by one task's thread, because a layer placed by
  layer is ordered on one home task (derived from I-O1); a layer that outgrows
  it is re-classed [P 73-74]. Per admitted act: a log entry per fact, one
  answer, one stamp, one value lock per value (a lock row in personal and hand
  layers), the index writes for its values, and eager read entries in personal
  layers (RD3).
- **Invariants.**
  - Admitted whole on the layer's home task, all facts at one stamp (I-G6).
  - Every fact names the act's layer, else the act is refused, fact outside
    the act's layer [P 127-128].
  - The offer's class equals the layer's class fact, else refused [P 72-73].
  - The cited permission covers this person and this layer, lives in this
    layer, exists and is not revoked (I-G5, I-P1).
  - A replace names an unreplaced fact of the same layer, entity and key; no
    two facts of one act replace the same fact; else refused, stale replace
    [M replaceable?, refusal].
  - Every value gets its own lock at write (D2 for the facts the store acts
    on), wrapped per I-L1; a lock row on the same task for personal and hand
    layers or an own-row mark; in the record for agent sessions [P 91-96].
  - Subjects per value from owner, grammar and tool; the act's subject slot
    is their union [P 103-104, 138-141].
  - The stamp keeps I-O2. The answer, with digest and stamp, is kept on the
    home task (I-G9).
- **Data growth and scale.** The log per layer is unbounded and append-only.
  Answers, one per name, are kept with no ruled expiry, since a finished
  record can replay after a crash (D4): unbounded. Dominant access: a point
  lookup by name (answer); the chain head by layer, entity and key; a range by
  stamp within a layer (reads as of T).
- **Concurrency.** Every offer into one layer is decided in its home task's
  order. Two offers replacing the same head: the first decided is admitted, the
  second refused stale. The owner's hand and agent sessions are other layers
  with their own homes, with no order between them except stood-on (I-O4). A
  resend racing its original lands on the same task after it and finds the
  record.
- **Edge cases.**
  - An empty act. The model never decides one (its facts give it no
    partition), so its offerer never hears. The rig answers it as data (O1).
  - A very large act. "Acts of any size": admitted whole in one decision. No
    limit is ruled; a limit is a rig choice with its own refusal reason (O1).
  - An act mixing a store-placed fact (a forget, a setting) with ordinary
    facts: silent (O1).
  - A value that fails its key's grammar: refused (derived from ruling 6: a
    value that is not opaque gets a shape check) (OP16).
  - Stood-on naming a fact the gate cannot see (not yet visible in the other
    store, or no such id). The model skips unknown ids when stamping
    [M stamp-for], so the clock promise goes unchecked for them (O3, R6).
  - A write about a person already forgotten: open in PROGRESS [P 179-180]
    (O11).
  - A layer that does not exist or has no class fact: refused as data (O1).
  - A closed session layer: open, "when a session layer closes (82)"
    [P 186] (O10).
  - A value about no one in a one-owner layer: its subjects are the owner, so
    it dies with the owner.

#### OP2. Resend under the same name; look up an answer [RQ 4] [M lookup]

- **Latency (assumed).** One point read, two for a store-placed offer in a
  one-owner layer (I-G4): single-digit milliseconds.
- **Throughput.** One per client error or timeout. After a crash every record
  since the checkpoint replays [RQ 2], so the gate's own look at the record is
  on the hot path of every replay.
- **Invariants.**
  - Same content, same name: the recorded answer and stamp, nothing decided
    again, even when the reason for a no has since gone away. A refused name
    stays refused; the offerer makes a new name to try again
    [M stream-step "already decided here"].
  - Other content, same name: refused, name taken; the first record is
    untouched [M lookup with `:digest-check`].
  - Layer or class other than the name's tag: refused on its face, reading
    nothing [M mis-tagged?].
  - Neither a name-taken nor a face refusal is written as the name's answer
    (that would overwrite the first use), yet the offerer must learn it as
    data. The model only notes them; how the rig returns them is open (O4).
  - "No answer yet" is distinguishable from every refusal. An offer in flight
    and a lost offer look the same, and the offerer resends [M drain].
  - A yes and a read by the same offerer never contradict each other (derived
    from I-O6 with I-O5): if an answer is visible before the admitted facts are
    visible to a frontier read, an offerer sees yes and cannot read its own
    act. Which reads go through the frontier is phase 3's; the requirement is
    that the two agree. Not ruled.
- **Data growth and scale.** Nothing beyond the answers already kept.
- **Concurrency.** Stream: the resend and the original are on one task, in
  order. Micro: in one batch the later is skipped as decided; in a later batch
  it finds the name row [M micro-decision, micro-prepare]. Two contents under
  one name at once: whichever the gate decides first keeps the name, the other
  is refused by digest, and every later lookup agrees.
- **Edge cases.**
  - An honest collision of two random names is indistinguishable from a reuse:
    refused by digest.
  - A person or the operator offering under a scheme reserved to the store:
    refused (derived from "reserved to the store" [P 131-132]; the model never
    generates one) (O1).
  - A resend after a re-class: decided before, the record answers; never
    decided, the stream gate now refuses class mismatch and the offerer makes a
    name tagged for the new class.
  - A resend of an offer admitted before its value was forgotten: the record
    answers yes, the value stays erased, and answering needs no opening.

#### OP3. Offer an act into a shared layer (the microbatch gate) [SP 3]

- **Latency (assumed).** Hundreds of milliseconds to a few seconds: an answer
  exists once the batch commits on every task it touches and the settled
  frontier passes it (I-O5).
- **Throughput.** Group and base writes; promotion landings; grants and
  revokes in shared layers; re-classed hot one-owner layers, which bring an
  agent-rate stream to this gate [P 136-137].
- **Invariants.** Those of OP1, and:
  - An act placed by entity can span tasks. It is admitted whole across them
    in one batch; its answer is kept on every task it touches and on the name
    row's task, written in the same batch [P 133-135] [M micro-decision].
  - Wrapped locks in the record for group and base [SP 3], unless the value
    is marked own-row: then a lock row on the value's task.
  - Within a batch, offers are decided in one order in which each sees what
    was decided before it: a replace, a revoke, a name's first use, a class
    change earlier in the batch [M micro-prepare]. Answers, stamps and every
    later read agree with that order.
  - A retried batch leaves no partial state [RQ 5], but a read of the other
    store inside a batch can differ between attempts (RIG.md, claim table,
    last row). Only the committed attempt counts; nothing outside the store
    acts on an attempt that did not commit.
  - An act refused on any one fact is refused whole: no task admits its share
    of an act whose other share failed.
  - A landing is stamped later than its crossing fact (I-O3); whether the gate
    reads that stamp or the offer carries it is R6, phase 3's pick (O3).
- **Data growth and scale.** The log per shared layer, spread by entity;
  answers on each task touched, plus a name row per decision: unbounded.
- **Concurrency.** Two acts replacing one head in a batch: the earlier in the
  batch's order is admitted, the later refused stale. A revoke and a write
  under it: OP6. Across batches: commit order, and the next batch starts only
  after every task committed [RQ 7].
- **Edge cases.**
  - An act touching many entities: many tasks, still whole.
  - A group write citing the writer's session permission: refused, permission
    from another layer [S D1].
  - An offer racing a re-class of its layer: the micro gate learns the class
    by a read; a stale view refuses (class mismatch) and never admits into the
    wrong store [R 103].
  - A batch holding an offer and its resend: the second is skipped as decided.
  - A reuse in the same batch as the first use: refused by digest.

#### OP4. Replace and retract within a chain [P 48-50, 60-61] [M replaceable?, chain-head]

- **Latency, throughput.** Those of OP1 or OP3, by the layer's gate.
- **Invariants.**
  - A replace names the unreplaced fact of the same layer, entity and key,
    and a fact is replaced once. The model allows replacing a fact whose value
    is erased: the check is on the chain, not on whether the value opens
    [M replaceable?].
  - Several unreplaced facts can stand on one layer, entity and key: the model
    admits a fact without `replaces` beside the head, and its chain head is
    the latest by stamp [M chain-head]. What a point read returns then is the
    model's (the latest) unless a rig choice says otherwise (O5).
  - A retract is a new fact, undoable by another new fact; time travel shows
    it [P 60-61]. The model has none, so its form is open (O5). Whatever the
    form, it passes the gate like any act and never erases a value.
- **Data growth and scale.** Chains of unbounded length; the head is a point
  lookup; history by stamp is a range.
- **Concurrency.** The first replace decided wins (OP1, OP3).
- **Edge cases.** A replace naming a fact in another layer, or no fact: refused
  stale. Two facts of one act replacing one fact: refused (doubled). After a
  re-class, a replace in the micro store of a head admitted in the stream store
  before it: the micro gate must see that head as settled history [R 105]
  (O3).

#### OP5. Make a layer; start and close a session [P 109-112, 105-107]

- **Latency (assumed).** Rare; not latency-bound, except that the first offer
  into a new layer needs its class fact and root permission already admitted.
- **Throughput (assumed).** One per session; agent sessions may be many per
  person per day. Groups are rare.
- **Invariants.**
  - A layer's first facts give its kind; its owner (one-owner) or members
    (group); its class fact (by layer for one-owner, by entity for shared)
    [M init]; its grain (per value); and its root permission, which for a
    group is made with the layer and names the group [P 110-112].
  - A session's kind, hand or agent, comes from its session-start fact and
    decides where its values' locks live (row for hand, record for agent)
    [P 93-95, 109-110].
  - Default visibility comes from seed policy facts [P 105-107].
  - Layers, people, entities, keys and tools are data, never code [SPEC "What
    the rig is not"].
  - While placed by layer, a layer's home task does not change (derived:
    placement by layer with N fixed).
- **Data growth and scale.** Layers are unbounded (sessions accumulate); with
  N fixed for two years, each task is home to ever more layers.
- **Concurrency.** An offer racing the layer's making: refused as data (no
  class fact yet) (O1).
- **Edge cases.** Closing a session: open (82); offers after the close, and
  agent-session read entries "added on promotion", wait on it [P 82-83, 186]
  (O10). A person with no session (the model's Bob) cites own-layer
  permissions only [M permissions].

#### OP6. Grant and revoke a permission [P 56-57, 159-161] [R 72-76] [SP 3]

- **Latency.** A revoke must bind the next decision of the gate that orders
  its layer (derived: one gate orders both, I-P1). Grants are rare.
- **Throughput (assumed).** Low.
- **Invariants.**
  - A permission is a fact in the layer it governs; permissions are first
    facts in every run [R 73]. In the model it covers one person and one layer;
    narrower cover (keys, entities, tools) is not modelled (O6).
  - A revoke is a fact in the permission's layer, standing on the permission
    fact [R 76]. The model's revoker is the operator [M op-revoke]; whether a
    person may revoke is open (O6).
  - Once admitted, every later decision of that gate citing the permission is
    refused, revoked. Decisions before it stand; nothing is undone [M refusal].
  - A revocation and the writes it governs are ordered by one gate: the
    layer's home task, or the micro store's commit order [R 98].
  - Revoking a session root does not revoke permissions under it in the model
    (no cascade) (O6).
  - A revoked permission refuses the forgets that cite it too; whether a
    person can always forget their own values after a revoke is open in
    PROGRESS [P 178-179] (O13).
- **Data growth and scale.** Permissions per person per layer, and at most a
  few revocations each; small.
- **Concurrency. The revocation race** [SP 3]: a write and the revocation of
  its permission in one shared layer. Whichever the micro gate orders first
  decides: a write ordered after the revoke is refused; one ordered before is
  admitted and stays; every read agrees with that order. A revoke in the other
  store never races a group write, because under the baseline the write
  cannot cite a permission from another layer [S D1, D2].
- **Edge cases.** A re-grant after a revoke (same permission id or a new one):
  open (O6). A revoke of an unknown permission, or a second revoke: silent; the
  model only revokes live ones (O6). A revoke of the permission a crossed
  promotion's landing cites: the landing is refused and the promotion ends
  refused after crossed (OP15).

#### OP7. Re-class a layer [P 72-74, 136-137] [M op-layer-setting, gate-of]

- **Latency (assumed).** Rare and operational; not latency-bound.
- **Throughput.** Rare; driven by a layer growing hot, typically an agent
  session writing continuously.
- **Invariants.**
  - The re-class is a fact placed on the layer's home task and ordered there
    with the layer's writes [M op-layer-setting]. Every write decided before it
    stays in the stream store; every stream offer decided after it is refused,
    class mismatch.
  - A re-classed one-owner layer moves to the microbatch gate, placed by
    entity; its single-owner order promise ends at the re-class [P 136-137].
  - Its earlier values, their lock rows and its permission facts stay in the
    stream store, and the micro gate reads them as settled history, which rests
    on the re-class fact standing after them on the same task [R 105]. How the
    micro gate reads them is R6, phase 3's (O3).
  - Values written after it keep their layer kind's lock placement (a row for
    personal and hand, now on the value's micro task; in the record for agent)
    (derived from ruling 7 with I-L2).
  - Later names of the layer are tagged by entity; their answers are found by
    the name row (I-G4).
  - The model's re-classer is the operator [M op-layer-setting]; whether a
    person may re-class is open (O9).
- **Data growth and scale.** Nothing new; the layer's log is split across the
  two stores at the re-class.
- **Concurrency.** Offers tagged by layer and decided after the re-class on
  the home task: refused, class mismatch. Offers tagged by entity that reach
  the micro gate before it sees the new class: refused, class mismatch; a
  stale view only refuses [R 103]. A forget of a value whose lock row is in the
  stream store is placed where the lock is and decided there
  [M op-forget-value].
- **Edge cases.** Back to by layer, or a shared layer re-classed: neither the
  rulings nor the model go that way (O9). A promotion pending at the re-class:
  its read-out is placed where the source's lock is, in the stream store, and
  decided there (derived from [M send-read-out]). Per-act grain in a
  re-classed layer: acts now span tasks (D12, O8). A re-class and a grain
  switch in flight together: both are facts on the home task, ordered there.

#### OP8. Switch lock grain [P 96-100] [M op-layer-setting, lock-for]

- **Latency, throughput (assumed).** Rare.
- **Invariants.**
  - A setting on the layer, a fact the person can change [P 98]. The model's
    setter is the layer's owner under an own-layer permission; on group and
    base it is refused for want of one (D11, O7).
  - It affects only values written after it: values admitted before keep
    their locks; values admitted after, in the order of the gate that admits
    them, get the act's lock [P 99-100].
  - A per-act lock is one lock for all the act's values, forgettable only as
    a whole, wrapped under the act's subject union [P 144-145], with the
    limits of D12.
  - Switching back to per value is the person changing the setting (derived
    from "can change"); it too affects only later values.
  - For a layer placed by layer the switch sits on the home task with the
    layer's writes, so every write is decided with the setting in force. On
    the micro gate (shared or re-classed layers) a stale view of the grain does
    not refuse, it locks at the wrong grain (D6); the requirement is that
    "after the switch" means after in the admitting gate's order (derived).
- **Edge cases.** A switch inside an act that also writes values: silent
  (O7). Two switches in flight: ordered on the home task.

#### OP9. Forget a value [P 60-62, 97, 117-118, 161] [SP 2] [M op-forget-value, apply-control]

- **Latency (assumed).** A gate write, so OP1's or OP3's latency. Once the
  forget is answered yes, the value opens nowhere: the effect is complete at
  the answer, never eventual (derived from "gone for everyone including the
  past" [P 61]).
- **Throughput (assumed).** Low against writes. Row deletions show in phase
  7's lock-store curve.
- **Invariants.**
  - The forget is a fact. It names the value and a permission and stands on
    the value [M op-forget-value]. The store places it on the task that holds
    the value's lock; its name carries the layer and no class [M tag-of].
  - A lock row (personal, hand, or an own-row mark) is deleted [SP 2]. A lock
    kept in the record (agent, group, base, unmarked) is excised: the
    operator's fallback for unmarked record-default values [P 97]. In the model
    an agent-session value's owner orders the forget and the effect is an
    excision [M op-forget-value].
  - In a group layer a member cannot forget one value themselves unless the
    group's rule requires the mark on write; otherwise it is an excision
    [P 117-118] (OP12).
  - It reaches every index over values and every copy of the plaintext the
    store keeps (I-L4, I-L5).
  - The erasure's date is the forget fact's stamp [M apply-control]. Reads at
    every moment, including moments before the forget, show "erased on <date>"
    (I-L7).
  - Under per-act grain it erases every value sharing the act's lock [P 99].
  - Ordered before a promotion's read-out on the same task, it makes the
    read-out refuse; after it, the copy is not recalled [P 153-156] [S B1, B3].
  - Replayed after any restore (OP11).
- **Data growth and scale.** One forget fact each; the lock store loses the
  row (or the act's row).
- **Concurrency.** A forget and a read-out on one task are ordered by that
  task [R 94]. A forget against a landing is ordered by nothing, and nothing
  is promised [R 96]. Two forgets of one value: the first erases, the second
  is admitted and changes nothing [M apply-control]. A forget stands on its
  value, so it cannot be decided before the value exists.
- **Edge cases.**
  - A value already closed by a person forget: the model admits the value
    forget and records a lock erasure, and its reads then show the value
    forget's date rather than the person forget's [M erasure]. Which date a
    doubly erased value shows is not ruled (O14).
  - A fact with no lock (D2): the model never forgets one, since a forget is
    placed where the value's lock is and such a fact has none; the outcome
    is O1, and the fact must stay readable either way.
  - An unknown id, or a value not yet visible to the deciding gate: silent
    (O1).
  - An opaque value: its lock is destroyed like any other; opacity removes
    matching, shape check and index, not the lock (derived from [P 88-90]).
  - A value admitted before its layer's re-class: decided where its lock is
    (OP7).
  - The forgetter's permission revoked: refused in the model (O13).
  - A deterministic tool's short read entry whose fingerprint covered the
    value cannot re-run to that fingerprint: expected [P 148-149] (RD6).

#### OP10. Forget a person [SP 2] [P 60-62, 177-178] [R 56, 95, 106] [M op-forget-person, wrap-closed]

- **Latency (assumed).** Seconds are acceptable. Once answered, no value whose
  wrap needs that person opens anywhere.
- **Throughput.** Rare.
- **Invariants.**
  - It destroys the person lock [SP 2]. Every value whose wrap requires that
    person, or whose any-of people are now all forgotten, can no longer be
    opened (I-L1) [M wrap-closed].
  - It is a fact (D3); its layer, name, permission and ordering gate are open
    (O12).
  - Its date shows as "erased on <date>" on every value it closes. The model
    uses one global stamp later than everything so far [R 106]; the rig's date
    must be later than every stamp either store gave before it (derived from
    the model's reading); how, without a global stamp, is open (O12).
  - What it does not close stays open: a mention of Bob in Alice's own layer
    survives Bob [S A8]; a group value about Alice and Bob survives either
    alone [S A4, A5]; a group value about no one survives everyone [S A3]; a
    promoted copy is about whom the target's grammar and tool name, not its
    former owner, so it survives the former owner unless they are named
    [P 156-157] [S B4].
  - Before a pending promotion's read-out, it makes the read-out refuse [S B2];
    after, the copy stays [S B4]. The crossing fact is written only if the
    opening succeeded, so the copy's exemption rests on the crossing fact
    existing, never on comparing stamps across stores [R 95].
  - Its order against a read-out inside the lock store is "reasoned, not
    shown" [P 177-178]; the rig shows it or reports that it could not (O15).
- **Data growth and scale.** One person lock per person; a destroyed one
  still yields its date (derived: reads show it).
- **Concurrency.** Against a read-out on another task it is a read at a moment
  (I-O4); either way, the crossing fact exists only if the opening succeeded.
- **Edge cases.** Forgetting a person twice: nothing changes, the first date
  stays [M op-forget-person]. Writes about a forgotten person, and the
  forgotten person's own later offers and permissions: open (O11). A per-act
  lock whose union holds the person closes by the wrap rule.

#### OP11. Restore [P 169]

Every restore is a fact, and forget facts are replayed after any restore
[P 169]. The rig is not durable [SPEC "What the rig is not"], so whether it
has a restore at all is open (O16). If it has one: after it, every value a
forget erased stays erased with its original date, and nothing comes back
different [P 44].

#### OP12. A group's rule that requires the mark on write [P 101-102, 117-118]

A value about two people survives one's forget by default and dies by a mark
at write or by a group's rule (7b); a member can forget one group value
themselves only where the group's rule requires the mark on write. The model
has no group rule. Open (O17): whether the rule makes the gate refuse unmarked
writes or apply the mark itself; where it lives (the recurring shape puts a
pick in a fact on the layer [P 38-39]); and, as a pick that decides how values
are written, it affects only later writes (I-P5).

#### OP13. Promotion request [P 114-116, 150-158] [SP 4] [M op-promote]

- **Latency (assumed).** The request's own answer is a stream write (OP1).
  The whole promotion takes up to seconds; meanwhile the person sees pending
  or crossed, never done (I-O6).
- **Throughput (assumed).** People moving their own values into groups and
  the base; low to moderate.
- **Invariants.**
  - An act in the owner's layer. It names the source value, the target layer,
    the target chain head the copy will replace (if any) and the permission the
    landing will cite [M op-promote]. The cross-layer move is two acts
    [P 128].
  - It stands on the source, so it is stamped after it.
  - The read-out's and the landing's names are fixed before the first gate,
    derived from the request's name under schemes reserved to the store
    [P 129-132].
  - In the model the source is an openable value in the person's own layer
    [M op-promote]. Sources in hand or agent sessions, and targets that are
    not group or base, are open (O18).
  - "Say all of this at the point of promotion" [P 158]: the request's answer
    or the status read carries what a surface needs to say it: two steps;
    pending is still forgettable, crossed is not recallable; a forget before
    the read-out refuses, after it the copy stays; the copy is about the
    target's subjects; crossed does not promise done (derived; the words are
    not the rig's).
  - A yes queues the read-out at least once, and again on every replay that
    finds the request decided yes; the read-out's derived name makes the
    extras duplicates [M stream-step, continue-promotion].
- **Data growth and scale.** One request fact per promotion.
- **Concurrency.** Two requests promoting one value are two promotions; both
  can land, unless the first landing moved the head the second names, which
  refuses the second as stale (derived from [M forward, refusal]).
- **Edge cases.**
  - A retried request lands once [SP 4]: resends hit the recorded answer, and
    the derived names collapse repeated read-outs and landings.
  - A refused request has no read-out and no status; the model reports status
    only for admitted requests [M read-as-of].
  - A request for a value already erased is admitted (the request checks
    nothing about the source); its read-out refuses, source erased, and the
    promotion is refused (derived from [M refusal]).
  - A request for a fact with no value (D2): the read-out refuses, source has
    no value.

#### OP14. Read-out, the crossing fact, and sending the landing [P 150-158] [R 60-65, 93-95] [M send-read-out, forward]

- **Latency (assumed).** The store's next step after the request, tens of
  milliseconds; the promotion shows pending until the crossing fact exists.
- **Throughput.** One per admitted request, plus duplicates on replay.
- **Invariants.**
  - The store's own step: named from the request, acting at the root (no
    permission), claiming no class, placed on the task that holds the source's
    lock [M send-read-out].
  - It opens the value through its lock (the value lock under its wrap) on
    that task. Only if the opening succeeds does it write the crossing fact
    there, in the source's layer, standing on the source and the request
    [R 95] [M send-read-out].
  - It refuses, and the promotion is refused, when the source has no value or
    is erased (row deleted, excised, or wrap closed) [M refusal] [S B1, B2].
  - A forget ordered before it on that task refuses it. After it the copy is
    not recalled: the read-out is a read that happened [P 153-156].
  - After a yes it sends the landing offer: the copy, the target layer, class
    by entity, the landing permission and the replace from the request,
    standing on the source and the crossing fact, because of the request
    [M forward]. A replay that finds the read-out decided sends the landing
    again while the source can still be opened; if the source is erased by
    then, nothing is sent [M forward].
  - Consequence (derived from the model): if a read-out is decided yes and the
    landing offer is not durably sent before the source is erased, the
    promotion stays crossed for ever, neither done nor refused. "Crossed does
    not promise done" [P 157] covers a refused landing, not a landing never
    sent. The rig decides as the model does or reports the difference.
- **Data growth and scale.** One crossing fact per read-out.
- **Concurrency.** With a value forget on the same task: ordered there
  [R 94]. With a person forget: a read at a moment (I-O4) [R 95]. A replayed
  read-out finds its record under the derived name.
- **Edge cases.** The source's lock is in the stream store of a re-classed
  layer (OP7). The source is under per-act grain: the act's lock is opened.
  An opaque source: its lock opens as any other, and the copy is opaque in the
  target because its key, and so its grammar, is the same (derived). Whether
  the read-out leaves a read entry: silent (O19).

#### OP15. Landing [SP 4] [P 150-158] [M forward, commit-decision, promotion-status]

- **Latency (assumed).** A microbatch write (OP3).
- **Invariants.**
  - Named from the request under the store's scheme; the name row makes any
    second landing a duplicate. A retried request lands once; a failover
    between forward and landing lands once [SP 4].
  - Offered as the requester, citing the landing permission in the target
    layer, which the micro gate orders [M forward].
  - Stamped later than the crossing fact and the source (I-O3).
  - The copy has the source's entity, key and value; the target layer;
    replaces the target head named in the request; carries no marks
    [M forward]. It gets a new value lock by the target's rule (in the record
    for group and base), wrapped under its own subjects from the target's
    grammar and tool, never the former owner as owner [P 156-157].
  - It can be refused: stale replace (the target head moved), permission
    revoked or missing, class mismatch. The promotion then ends refused after
    crossed [P 157-158].
  - Under the baseline the landing does not check its source again
    (`:landing-checks-source false`): a forget after the read-out does not
    stop it [R 96] [S B3, B4].
- **Data growth and scale.** One fact per landed promotion in the target.
- **Concurrency.** Against a forget of the source: ordered by nothing, and
  nothing is promised [R 96]. Against other writes to the target chain: the
  batch order.
- **Edge cases.** A landing after the source was forgotten: done, the copy
  open [S B3, B4]. The target head named in the request since erased: the
  replace is still allowed (OP4).

#### OP16. Write a key's grammar [SP 6] [P 65, 87-90, 103-104]

- **Latency, throughput (assumed).** Rare.
- **Invariants.**
  - Keys are ids; a key's word and grammar are facts about it [P 65].
  - The grammar says which subjects its values name (the second source of
    subjects, ruling 8) and whether its values are opaque [SP 6].
  - A value under a key that is not opaque is opened by the gate, matched,
    checked for shape and indexable. Under an opaque key: no matching, no
    shape check, no index, and reads show it as opaque [P 88-90].
  - Adding a grammar takes facts only; zero new compiled steps is the target
    (OP18).
  - A grammar change is a pick that decides how values are written (subjects
    decide wraps; opacity decides indexing), so it applies to values written
    after it; values already written keep their wraps (I-P5, derived).
- **Edge cases.** A key with no grammar facts: its values name no subjects
  beyond owner and tool and are not opaque (derived; the model's `:note`).
  Where grammar facts live, how a gate in the other store sees a change (a
  read at a moment, I-O4), a key made opaque after its values were indexed,
  competing grammar facts, and where an opaque value's subjects come from when
  the grammar cannot read the value: open (O20).

#### OP17. Write a tool [SP 6] [P 52, 80-81, 103-104]

- **Invariants.** A tool is facts: what it matches, its signature, its read
  entry preference [SP 6]. A deterministic tool gets the short read entry by
  default and may ask for exact rows in its signature [P 80-81]. A tool is the
  third source of subjects; how it names them is not given, and the model's
  tool names none (O21). Tools add facts, never envelope parts [P 52]. Adding
  one takes facts only; zero new compiled steps is the target.
- **Edge cases.** A tool that seems to need a new envelope part cannot have
  it; the gate refuses unknown parts (I-G8). A tool changed after it ran: a
  read-time pick can change (I-P5). A tool that is not deterministic: the
  ruling covers models, people and deterministic tools only (O21).

#### OP18. Run tools, and the count [SP 6] [P 84, 129-130]

- **Invariants.** The minimal runner finds tools by matching. What a tool's
  match facts are matched against (an admitted fact, a read) is not given
  (O21). A tool's outputs are offers through the gates like any other, because
  of their trigger ("trigger is already because-of" [P 84]), and named before
  the first gate (an act that causes further offers names each of them
  [P 129-130]). A runner re-run on a replay must not double its outputs; the
  names fixed in advance make repeats duplicates (derived from I-G2, I-G7).
- **The count.** Add one new tool and one new grammar by writing facts only;
  count the new compiled steps needed; zero is the target; report the count
  and every step, with why [SP 6]. What counts as a compiled step is fixed by
  the plan and written down with the count (open how it is defined, O21).

### Reads

Every read applies visibility (I-P2), shows only admitted facts (I-O6), reads
the micro store through the settled frontier (I-O5), derives the owner at read
time (I-P3), and never opens an erased value.

#### RD1. The answer to an offer, by name plus layer

OP2 holds its requirements. Also: an answer tells whether an offer into a
private layer was admitted, so who may look one up is a visibility question
the sources do not answer (O22).

#### RD2. Point read: a layer, an entity, a key; now or as of a moment [P 75] [SP 5]

- **Latency (assumed).** Single-digit milliseconds; point lookups.
- **Throughput (assumed).** The most frequent read: surfaces render values.
  Under ruling 3 a point read in a personal or shared layer also leaves read
  entry rows by default, so read volume becomes write volume (derived).
- **Invariants.** It returns the chain's head as of the read's moment (OP4,
  O5), with its value if it opens, "erased on <date>" if not, or "opaque" for
  an opaque key, and who, when, layer, stamp and act. It leaves rows: "rows for
  point reads" [P 75]; eager by default in personal and shared layers; agent
  session layers may default to none [P 81-83]; a model's or a person's reads
  are always exact [P 78-80].
- **Edge cases.** A key never written on that entity: an empty answer, never
  an error. Whether an empty point read leaves a row: silent (ruling 3 says
  "empty pattern reads included" only) (O19). A layer the reader cannot see:
  whether that differs from empty is open (O22).

#### RD3. Pattern read, and its read entry line [P 75-84] [SP 5]

- **Latency (assumed).** Tens of milliseconds for small matches; it grows with
  what matches. A read that stops short is marked partial.
- **Throughput.** Tools and people querying. Each pattern read in a personal
  or shared layer writes one line by default.
- **Invariants.**
  - It matches only values the gate can open: never an erased value, gone for
    everyone including the past [P 61], and never an opaque one, which allows
    no matching [P 88-90] (derived).
  - One read entry line per pattern read: the pattern, the moment, the role
    (seeded: stood on, shown, matched, passed through; a trigger is already
    because-of), a keyed fingerprint of what matched, a complete-or-partial
    mark; empty pattern reads included [P 75-84] [SP 5].
  - By kind of reader: a model's or a person's reads are the crossing's exact
    list, always, because there is no re-run. A deterministic tool gets the
    short entry by default and may ask for exact rows in its signature
    [P 78-81].
  - Eager by default in personal and shared layers; agent session layers may
    default to none, the line added on promotion, which waits on 82
    [P 81-83]. What "eager" means beyond the word (written when the read is
    taken, or with the act that follows) is not given (O19).
  - Every index it uses is rebuildable from the log or purgeable by value id
    (I-L5).
  - Across the two stores a read is final only for what each held when taken
    [P 173-177] [R 97].
- **Data growth and scale.** Read entry lines are unbounded, one per pattern
  read in the eager layers: plausibly more rows than facts (assumed).
- **Concurrency.** Against a forget: either the read opened the value (a read
  that happened, recorded) or it did not; never a match on a value erased
  before the read.
- **Edge cases.** An empty match still leaves its line. A huge match: when a
  read counts as partial is not given (O19). A pattern over both stores: one
  moment per store, or one moment closed in every store, is open [P 173-177]
  (O23). A pattern over entity and key alone can list erased facts with their
  date (derived).

#### RD4. Read as of a moment (time travel) [P 146-148] [SP 2] [M read-as-of]

- **Latency (assumed).** A range by stamp over a layer; tens of milliseconds.
- **Invariants.** Every admitted fact stamped at or before T, with its value
  or the date it was erased, even an erasure after T; nothing else from after
  T (no later fact, no forget fact, no replacement); each promotion's status
  as of T [M read-as-of]. In SPEC's words, "erased on this date" and nothing
  else from after its moment [SP 2]. A read as of T is final only for what the
  store held when taken: the other store can still admit something stamped at
  or before T; the model's x2 fails in every configuration and is not promised
  [R 97].
- **Edge cases.** T before any fact: empty. T later than any stamp given: open
  (O23). An erased fact shows, in the model, its id, act, layer, stamp and
  erasure date, never its value [M read-as-of]; what else a forget must hide is
  forget's full reach, open (77) [P 185].

#### RD5. A promotion's status as of a moment [M promotion-status] [P 150-158]

- **Invariants.** Done if the landing is at or before T; else refused if the
  read-out or the landing said no at or before T; else crossed if the crossing
  fact is at or before T; else pending [M promotion-status]. Pending until it
  lands [P 115-116]. It spans both stores: request and crossing fact in the
  stream store, landing in the micro store, the latter through the frontier.
- **Successive reads never go back** (pending, crossed, done or refused; or
  pending, refused): derived from time running one way [P 24-25] and the
  sequences the B cases expect [S B1-B4]. Not ruled in these words.
- **Edge cases.** A promotion that can stay crossed for ever (OP14); a landing
  refused after crossed; a refused request, which has no status.

#### RD6. Read entries, and a short entry's re-run [P 75-84, 148-149] [SP 5]

- The reads of an act are found with the act ("reads on the act once"
  [P 53-54]). Where the entries of an eager read that leads to no act live is
  not given (O19).
- A short entry re-runs to its fingerprint while nothing it matched is
  forgotten; once a matched value is forgotten it cannot, and that is expected
  [P 148-149]. A re-run can also match more, since the other store may since
  have admitted something dated at or before the moment [P 173-177].
- Exact rows name the matched facts; after a forget each shows its erasure
  date.
- The fingerprint is taken under a secret (I-L5), so a fingerprint does not
  let anyone confirm a guess at a forgotten value without it (derived from the
  purpose of "keyed"). The exact bytes it covers are open (56) [P 180-181].

#### RD7. Whether a value opens; the lock store's size [SP 2, 7]

Whether a value opens and, if not, since when. Per layer, the count and bytes
of lock rows: one row per value in personal and hand layers under per-value
grain, one per act under per-act grain (derived); deleted rows leave it
(OP9). Phase 7 measures this (M2).

#### RD8. A key's grammar; RD9. tools by matching; RD10. settings and permissions

Every gate decision reads the layer's class and grain, the cited permission
and its revocation, the replaced chain head, and the key's grammar for
subjects and shape (derived from I-G5, OP1, OP16), so these reads are on the
hot path of OP1 and OP3. The runner reads tools by matching (OP18). People
read which permissions they hold and their layers' settings, as facts, with
visibility (I-P2).

### Measurement and replay (phases 7 and 8)

- **M1. Index writes per second** with an agent session layer writing small
  acts continuously [SP 7]: one agent session placed by layer on one task,
  writing a stream of small acts on a thread entity, with a status fact on it
  (streaming, complete, stopped) [P 54-55]; the indexes over values updated
  per act; read entries possibly off by default [P 82].
- **M2. Lock store growth under hand layers** [SP 7]: bytes per value and the
  curve up to 100,000 values; hand layers hold lock rows, one per value under
  per-value grain; the lock store's bytes are measurable per layer.
- **M3. One person's layer on one thread** [SP 7]: acts per second and
  latency; one layer ordered on one task.
- Each number states its method and machine, and says the in-process cluster
  is not a proxy for production; only orders of magnitude mean anything
  [SPEC "Tests and evidence"].
- **M4. Replay of the model's fixed histories** [SP 8], where practical. The
  rig can drive the model's op kinds: offer, retry, reuse, revoke, promote;
  step and work (let one task's queue advance); prepare, commit and batch
  (hold a microbatch between its decision and its commit, which [S D1]
  needs); failover (a crash and restart); forget-value, forget-person,
  set-grain, reclass, read. Deposed-leader steps and clock skew have no analog
  (D7). The comparison is the model's: at the end, which named values are
  open, erased or missing; and each read's promotion statuses [S play]. Every
  difference is reported as a finding. The model's partition numbers (three
  per store, toy homes) do not map onto the rig's N; compare answers and reads,
  not placements (derived). Failures are injected through global atoms
  [RIG R3]; tests assert at least once [RIG R4].
- **M5. Tests the spec names**, by phase: phase 1, R7's stream cases (admit
  whole on the home; refuse on its face; refuse a reuse by digest; answer a
  retry from the record; refuse a fact outside the act's layer, a class
  mismatch, a permission that does not cover, lives elsewhere, does not exist
  or is revoked, a stale or doubled replace; stamp by the clock promises; the
  same answer after a crash mid-offer). Phase 2, R8: every A case both ways,
  one-owner and shared [S A1-A8]. Phase 3: the micro and permission cases and
  the revocation race [S D1, D2]. Phase 4: both sides of the read-out line
  [S B1-B4], a retried request landing once, a failover between forward and
  landing.

## Entity State × Write Matrix

Each entity lists its states, its writes, and the reads related to it; every
row then lists every one of those reads. "gate" is the view the next offer
meets (the checks of I-G5). "RD1x" is a lookup under the same name by someone
holding other content. A row marked (O*n*) has an outcome the plan picks.

### E1. An offer's name and its answer record

States: **N0** unused; **N1** in flight (sent, not decided); **N2** decided
yes, digest d, stamp s; **N3** decided no, digest d, reason r.
Writes: first offer; resend of the same content; other content under the name
(tag matching); an offer whose layer or class differ from the name's tag; a
crash and replay (Rama replays finished records too, D4).
Related reads: RD1, RD1x, RD2, RD3, RD4, RD5, RD6, gate.

**N0 × first offer, admitted**
- RD1: yes, stamp s, for digest d.
- RD1x: no, name taken.
- RD2: the act's facts.
- RD3: the new values match patterns that cover them; the read leaves its line.
- RD4: nothing as of T < s; the facts as of T ≥ s.
- RD5: pending, if the act is a promotion request.
- RD6: the act's reads, recorded once, on the act.
- gate: the task's stamp is at least s; later replaces see the new heads.

**N0 × first offer, refused**
- RD1: no, reason r, with a stamp (the model stamps refusals too [M decide]).
- RD1x: no, name taken: a refused first use still holds the name.
- RD2: nothing new.
- RD3: nothing new.
- RD4: nothing new at any moment.
- RD5: no status; a refused request is not a promotion.
- RD6: no act, so no reads recorded on an act (eager entries taken before
  the offer stay; O19).
- gate: the refusal consumed a stamp; nothing else changed.

**N0 × offer whose layer or class differ from its own name's tag**
- RD1: nothing recorded; the refusal reaches the offerer as data (O4).
- RD1x: nothing recorded under the name.
- RD2: nothing. RD3: nothing. RD4: nothing. RD5: nothing. RD6: nothing.
- gate: unchanged; nothing was read.

**N1 × resend of the same content**
- RD1: no answer yet, then the one answer the first decision gives.
- RD1x: name taken, once decided.
- RD2: the act at most once.
- RD3: at most one match per value.
- RD4: the act at one stamp.
- RD5: at most one promotion.
- RD6: the reads recorded once.
- gate: one stamp given, not two.

**N1 × other content under the same name, both in flight**
- RD1: whichever content the gate decides first holds the name; the first
  offerer gets its answer if that was theirs, else name taken.
- RD1x: the converse.
- RD2: at most one of the two contents admitted.
- RD3: matches only the admitted content.
- RD4: the same at every moment.
- RD5: at most one promotion.
- RD6: one act's reads.
- gate: one decision under the name.

**N1 × tag mismatch while the original is in flight**
- RD1: the original is unaffected.
- RD1x: refused on its face, not recorded (O4).
- RD2, RD3, RD4, RD5, RD6: unaffected.
- gate: unchanged.

**N1 × crash mid-decision, replay**
- RD1: no answer until decided; then one answer, the same on every replay.
- RD1x: name taken once decided.
- RD2: the act once; the failed attempt's writes were discarded [RQ 1].
- RD3: once.
- RD4: one stamp.
- RD5: one promotion; its read-out queued at least once, duplicates collapse.
- RD6: once.
- gate: no stamp given twice.

**N2 × resend of the same content**
- RD1: the same yes and stamp.
- RD1x: name taken.
- RD2: unchanged, no second admission.
- RD3: unchanged.
- RD4: unchanged.
- RD5: unchanged; the read-out is queued again and collapses [M stream-step].
- RD6: unchanged.
- gate: no new stamp.

**N2 × other content under the name**
- RD1: the first yes, unchanged.
- RD1x: no, name taken, returned as data, not recorded (O4).
- RD2, RD3, RD4, RD5, RD6: unchanged.
- gate: unchanged.

**N2 × tag mismatch**
- RD1: unchanged.
- RD1x: refused on its face; a lookup follows the tag to the first record and
  says name taken [M lookup].
- RD2, RD3, RD4, RD5, RD6: unchanged.
- gate: unchanged; nothing was read.

**N2 × crash, replay of the finished record**
- RD1: the same yes and stamp.
- RD1x: name taken.
- RD2: the act once.
- RD3: once.
- RD4: unchanged stamp.
- RD5: follow-on read-out re-queued and collapsed.
- RD6: once.
- gate: the replay gives no stamp.

**N3 × resend of the same content**
- RD1: the same no and reason, even if the cause has gone (say the permission
  was granted since).
- RD1x: name taken.
- RD2, RD3, RD4: nothing.
- RD5: no status.
- RD6: nothing.
- gate: no new decision.

**N3 × other content under the name**
- RD1: the no, unchanged.
- RD1x: name taken.
- RD2, RD3, RD4, RD5, RD6: nothing.
- gate: unchanged.

**N3 × tag mismatch**
- RD1: unchanged. RD1x: refused on its face.
- RD2, RD3, RD4, RD5, RD6: nothing.
- gate: unchanged.

**N3 × crash, replay**
- RD1: the same no. RD1x: name taken.
- RD2, RD3, RD4, RD5, RD6: nothing.
- gate: no stamp.

### E2. A chain and its value (one layer, entity and key)

States: **C0** no fact; **C1** a head whose value opens; **C2** a head whose
value is erased; **C3** a fact already replaced; **C4** several unreplaced
facts; **C5** a head under an opaque key.
Writes: a new fact without replace; a replace of the head; a replace of a
replaced fact; two replaces of one fact in one act; a retract; a value forget;
a person forget (owner, another subject, the last of several); a promotion
request of the head; a landing into the chain.
Related reads: RD2, RD3 (over values), RD3e (over entity and key alone), RD4,
RD5, RD6, RD7, gate.

**C0 × new fact**
- RD2: the value. RD3: matches. RD3e: lists it.
- RD4: nothing before its stamp s, the value from s.
- RD5: none.
- RD6: earlier short entries can match more if re-run (derived).
- RD7: opens.
- gate: a later replace must name it.

**C0 × replace naming a fact that is not there**
- RD1 of the offer: no, stale replace.
- RD2: nothing. RD3: nothing. RD3e: nothing. RD4: nothing. RD5: none.
- RD6: unchanged. RD7: nothing to open.
- gate: unchanged.

**C0 × value forget naming a value not there**
- Outcome is O1. RD2, RD3, RD3e, RD4, RD5, RD6, RD7: unchanged either way.
- gate: unchanged.

**C1 × new fact without replace**
- RD2: the model's chain head, the latest by stamp (O5).
- RD3: both values match. RD3e: both listed.
- RD4: the first between the two stamps; then per O5.
- RD5: unchanged. RD6: re-runs can match more.
- RD7: both open.
- gate: each can be replaced once (state C4).

**C1 × replace of the head**
- RD2: the new value.
- RD3: now matches the new value; as of an earlier moment, the old.
- RD3e: both in history.
- RD4: the old value before the new stamp s', the new from s'.
- RD5: a pending promotion of the old head still reads out the old value.
- RD6: short entries at earlier moments still re-run.
- RD7: both open; a replace erases nothing.
- gate: the old fact can no longer be replaced (stale).

**C3 × replace of the replaced fact**
- RD1 of the offer: no, stale replace.
- RD2, RD3, RD3e, RD4, RD5, RD6, RD7: unchanged.
- gate: unchanged.

**C1 × two facts of one act replacing the head**
- RD1: no, stale replace (doubled). RD2, RD3, RD3e, RD4, RD5, RD6, RD7:
  unchanged. gate: unchanged.

**C1 × retract**
- RD2: shows it retracted, in a form O5 fixes.
- RD3: per O5. RD3e: the retract listed.
- RD4: the value before the retract, the retract after.
- RD5: unchanged. RD6: unchanged.
- RD7: the value still opens; a retract erases nothing.
- gate: an undo is another new fact.

**C1 × value forget**
- RD2: "erased on F" in place of the value.
- RD3: never matches it again, at any moment.
- RD3e: lists the fact with its erasure date.
- RD4: erased on F at every moment from the fact's stamp, even before F;
  nothing else from after the moment.
- RD5: a pending promotion whose read-out comes after is refused; a crossed
  or done one keeps its copy.
- RD6: short entries that matched it cannot re-run to their fingerprint;
  exact rows show it erased.
- RD7: closed since F.
- gate: it can still be replaced (OP4); a second forget changes nothing.

**C2 × replace of the erased head**
- RD2: the new value. RD3: matches the new. RD3e: both.
- RD4: erased on F for the old at every moment; the new from its stamp.
- RD5: unchanged. RD6: unchanged. RD7: the new opens, the old stays closed.
- gate: the new head is replaceable.

**C2 × value forget again**
- If the first erasure was a value forget: nothing changes, the first date
  stays. If it was a person forget: the model records a lock erasure, and
  RD2, RD3e and RD4 then show the value forget's date (O14).
- RD3: no match either way. RD5: unchanged. RD6: unchanged.
- RD7: closed. gate: unchanged.

**C1 in a one-owner layer, unmarked × person forget of the owner**
- RD2: erased on P. RD3: no match. RD3e: listed with P.
- RD4: erased on P at every moment.
- RD5: pending, then refused at its read-out; crossed or done keeps its copy.
- RD6: short entries cannot re-run. RD7: closed since P.
- gate: replace still allowed; new writes by the forgotten person are O11.

**C1 in a one-owner layer, a mention of Bob, unmarked × person forget of Bob**
- RD2, RD3, RD3e, RD4, RD5, RD6: unchanged; RD7: still opens [S A8].
- gate: unchanged.

**C1 in a shared layer, about Alice and Bob, unmarked × forget Alice**
- RD2, RD3, RD3e, RD4, RD5, RD6: unchanged; RD7: opens [S A4].
- gate: unchanged.

**... then × forget Bob (the last of them)**
- RD2: erased on the later forget's date [M wrap-closed] [S A6].
- RD3: no match. RD3e: listed with that date. RD4: same at every moment.
- RD5: pending ones refused at read-out. RD6: cannot re-run.
- RD7: closed. gate: unchanged.

**C1 in a shared layer, marked × forget one subject**
- RD2: erased on P [S A7]. RD3: no match. RD3e: listed with P. RD4: same.
- RD5, RD6: as above. RD7: closed. gate: unchanged.

**C1 in a shared layer, about no one × any person forget**
- RD2, RD3, RD3e, RD4, RD5, RD6: unchanged; RD7: opens [S A3]; only a value
  forget or excision erases it. gate: unchanged.

**C1 × promotion request of the head**
- RD2: unchanged; the request is a fact on another key.
- RD3: unchanged. RD3e: unchanged for this chain.
- RD4: unchanged. RD5: pending.
- RD6: unchanged. RD7: opens; pending is still forgettable.
- gate: a forget is still allowed.

**C1 (the target chain) × landing**
- RD2: the copy.
- RD3: matches the copy.
- RD3e: the landed fact listed.
- RD4: the copy from the landing stamp.
- RD5: done.
- RD6: re-runs can match more.
- RD7: the copy opens under its own wrap.
- gate: the copy is the head; another landing that named the old head is
  refused stale.

**C5 × any read-affecting write** (new fact, replace, forget)
- RD2: "opaque" while its lock opens; erased on F once forgotten.
- RD3: never matches. RD3e: listed.
- RD4: as RD2 at each moment.
- RD5: a promotion reads it out through its lock; the copy is opaque too.
- RD6: nothing matched it, so no fingerprint covers it.
- RD7: opens until forgotten.
- gate: no shape check.

### E3. A layer

States: **L0** not made (no class fact); **L1** one-owner, by layer, per
value; **L2** one-owner, by layer, per act; **L3** one-owner, re-classed to by
entity; **L4** shared (group or base); **L5** a closed session (82).
Writes: make; an offer into it; grain switch; grain switch back; re-class;
re-class back; close a session.
Related reads: RD1, RD2, RD4, RD5, RD6, RD7, RD10 (settings as facts), gate.

**L0 × offer**
- RD1: no, as data (O1). RD2: nothing. RD4: nothing. RD5: none.
- RD6: nothing. RD7: no rows. RD10: no such layer.
- gate: unchanged.

**L0 × make**
- RD1: yes for its first facts.
- RD2: empty. RD4: empty before, first facts after.
- RD5: none. RD6: none. RD7: no rows.
- RD10: kind, owner or members, class, per-value grain, root permission.
- gate: offers citing its root permission can now be decided.

**L1 × offer admitted**
- RD1: yes. RD2: the facts. RD4: from the stamp.
- RD5: pending for a request. RD6: eager lines in a personal layer; none by
  default in an agent session.
- RD7: one row per value in personal and hand; none added in agent (lock in
  the record).
- RD10: unchanged. gate: the home task's stamp moves forward.

**L1 × grain switch to per act**
- RD1: yes. RD2: unchanged. RD4: the setting fact from its stamp.
- RD5: unchanged. RD6: unchanged.
- RD7: earlier values keep their rows; later acts add one row per act.
- RD10: per act.
- gate: later values get the act's lock.

**L2 × grain switch back to per value**
- RD1: yes. RD2: unchanged. RD4: the setting from its stamp.
- RD5: unchanged. RD6: unchanged.
- RD7: earlier per-act rows stay; later values one row each.
- RD10: per value. gate: later values get their own locks.

**L2 × value forget of one value**
- RD1: yes. RD2: every value of that act shows erased on F.
- RD4: the same at every moment.
- RD5: pending promotions of any of those values are refused at read-out.
- RD6: short entries covering any of them cannot re-run.
- RD7: the act's row is gone.
- RD10: unchanged. gate: unchanged.

**L1 × re-class**
- RD1: earlier answers stay at the home; later names are tagged by entity and
  found by the name row (I-G4).
- RD2: continuous history across both stores, the micro side through the
  frontier.
- RD4: earlier facts from the stream store, later from the micro store.
- RD5: pending promotions still read out on the stream task holding the
  source's lock.
- RD6: unchanged.
- RD7: earlier rows stay in the stream store; later rows sit on their values'
  micro tasks.
- RD10: by entity.
- gate: stream offers tagged by layer refused, class mismatch; micro offers
  admitted once the micro gate reads the new class.

**L3 × offer**
- RD1: found by the name row. RD2: in the micro gate's order: two acts from
  the one owner may now be ordered by batch, not arrival (the single-owner
  order promise ended [P 137]).
- RD4: stamps per the micro gate. RD5: unchanged. RD6: as the layer's kind.
- RD7: rows or record per the layer's kind. RD10: unchanged.
- gate: as OP3.

**L3 × re-class back to by layer**
- Not ruled, not modelled (O9). RD1, RD2, RD4, RD5, RD6, RD7, RD10, gate:
  per the rig choice; until one is made, the offer is refused as data.

**L4 × grain switch**
- RD1: no, no permission, in the model (D11, O7). RD2, RD4, RD5, RD6, RD7,
  RD10: unchanged. gate: unchanged.

**L4 × re-class**
- Not ruled (O9). RD1: per the rig choice. RD2, RD4, RD5, RD6, RD7, RD10,
  gate: unchanged until one is made.

**L1 (a session) × close; L5 × offer**
- Not ruled (82, O10). RD1: per the rig choice. RD2 and RD4: facts written
  before the close stay. RD5: promotions from the closed session per O18.
  RD6: the line "added on promotion" waits on 82. RD7: rows stay. RD10: the
  close as a fact. gate: per the rig choice.

### E4. A permission

States: **P0** does not exist; **P1** granted and live; **P2** revoked.
Writes: grant; revoke; revoke again; a write citing it from its own layer; a
write into another layer citing it; a forget citing it; a re-grant.
Related reads: RD1 (offers citing it), RD2 and RD4 (the permission and
revocation facts), RD5 (promotions whose landing cites it), RD10 (a person's
view of what they hold), gate.

**P0 × write citing it**
- RD1: no, no such permission. RD2: nothing. RD4: nothing. RD5: n/a.
- RD10: not held. gate: unchanged.

**P0 × grant**
- RD1: yes. RD2: the permission fact. RD4: from its stamp.
- RD5: landings citing it can now land. RD10: held.
- gate: writes citing it are decided on their other checks.

**P0 × revoke**
- Not ruled (O6). RD1: per the rig choice. RD2, RD4, RD5, RD10: unchanged.
  gate: unchanged.

**P1 × write citing it in its own layer**
- RD1: yes, if the other checks pass. RD2: the facts. RD4: from the stamp.
- RD5: unchanged. RD10: unchanged. gate: unchanged.

**P1 × write into another layer citing it**
- RD1: no, permission from another layer (or does not cover, when person or
  layer differ). RD2, RD4, RD5, RD10: unchanged. gate: unchanged.

**P1 × revoke**
- RD1: every later decision citing it: no, revoked; earlier answers never
  change.
- RD2: the revocation fact; facts admitted under it before stay.
- RD4: live before the revocation's stamp, revoked from it.
- RD5: a crossed promotion whose landing cites it and has not landed ends
  refused; done ones are unchanged.
- RD10: revoked. gate: refuses it from now.

**P1 × revoke racing a write, same shared layer, same batch**
- RD1: yes if the gate ordered the write first; no, revoked, if after.
- RD2: agrees with that order. RD4: agrees with the stamps.
- RD5: a landing in the race follows the same rule. RD10: revoked.
- gate: one order, which every read agrees with.

**P2 × write citing it**
- RD1: no, revoked. RD2, RD4, RD5, RD10: unchanged. gate: unchanged.

**P2 × revoke again**
- The model revokes only live ones (O6). RD1: per the rig choice. RD2 and
  RD4: revoked since the first revocation. RD5, RD10: unchanged. gate:
  unchanged.

**P2 × forget citing it**
- RD1: no, revoked, in the model (O13). RD2: the value stays open. RD4:
  unchanged. RD5: unchanged. RD10: unchanged. gate: unchanged.

**P2 × re-grant**
- O6: a new fact under the same permission id or a new id. RD1, RD2, RD4,
  RD5, RD10, gate: per the rig choice.

**P1 (a session root) × revoke**
- RD1: offers citing the root refused; offers citing permissions under it
  still decided on their own (no cascade in the model, O6).
- RD2, RD4: the revocation. RD5: landings cite target-layer permissions, so
  unchanged. RD10: root revoked, others live. gate: as RD1.

### E5. A value lock, and its lock row

States: **K0** none (a fact the store acts on, D2); **K1** a live lock row
(personal, hand, or own-row mark); **K2** a live lock in the record (agent,
group, base); **K3** row deleted; **K4** excised; **K5** wrap closed by
person forget; **K6** wrap partly closed (some any-of people forgotten, still
opens).
Writes: value forget; person forget (a required person; one of several any-of;
the last any-of); read-out; restore; re-class of its layer; grain switch.
Related reads: RD2, RD3, RD4, RD5, RD6, RD7 (opens, since when), RD7s (the
lock store's size), gate.

**K0 × value forget**
- RD1: per O1; the model cannot place such a forget (it goes where the
  lock is). RD2: the fact stays readable either way.
- RD3, RD4, RD5, RD6: unchanged. RD7: nothing to close. RD7s: unchanged.
- gate: the fact keeps serving the gate (a permission, a setting).

**K1 × value forget**
- RD2: erased on F. RD3: no match. RD4: erased on F at every moment.
- RD5: a later read-out refuses. RD6: short entries cannot re-run.
- RD7: closed since F (row deleted). RD7s: one row fewer (or the act's).
- gate: unchanged.

**K2 × value forget**
- Excised by the operator; in a group, a member's own forget is O17 (OP12).
- RD2: erased on F. RD3: no match. RD4: erased on F. RD5: read-out refuses.
- RD6: cannot re-run. RD7: closed since F (excised). RD7s: unchanged.
- gate: unchanged.

**K1 or K2 × person forget of a required person**
- RD2: erased on P. RD3: no match. RD4: erased on P.
- RD5: pending refused at read-out; crossed and done keep their copy.
- RD6: cannot re-run. RD7: closed since P.
- RD7s: in the model the row stays, closed; a row that can no longer open
  still counts in the lock store's size (derived; matters for M2).
- gate: unchanged.

**K1 or K2 with any-of Alice and Bob × forget Alice (to K6)**
- RD2, RD3, RD4, RD5, RD6: unchanged. RD7: opens. RD7s: unchanged. gate:
  unchanged [S A4].

**K6 × forget of the last any-of person (to K5)**
- RD2: erased on that forget's date. RD3: no match. RD4: same.
- RD5: read-out refuses. RD6: cannot re-run. RD7: closed [S A6].
- RD7s: unchanged. gate: unchanged.

**K1 × read-out**
- RD2, RD3, RD4: unchanged. RD5: crossed. RD6: unchanged (O19).
- RD7: still opens. RD7s: unchanged. gate: unchanged.

**K3, K4 or K5 × read-out**
- RD5: refused, source erased [S B1, B2]. RD2, RD3, RD4, RD6, RD7, RD7s:
  unchanged. gate: unchanged.

**K3 or K4 × value forget again**
- RD2, RD4: the first date stays. RD3, RD5, RD6: unchanged. RD7: closed.
  RD7s: unchanged. gate: unchanged.

**K5 × value forget**
- The model records a lock erasure; RD2 and RD4 then show the value forget's
  date (O14). RD3, RD5, RD6: unchanged. RD7: closed. RD7s: the row goes.
  gate: unchanged.

**K3, K4 or K5 × restore**
- If the rig restores at all (O16): RD2, RD4: still erased, same date. RD3:
  no match. RD5: unchanged. RD6: cannot re-run. RD7: closed. RD7s: no row
  comes back. gate: unchanged.

**K1 × re-class of its layer**
- RD2, RD3, RD4, RD5, RD6: unchanged. RD7: opens; the row stays on its
  stream task. RD7s: unchanged. gate: a later forget is placed where the
  row is.

**K1 or K2 × grain switch**
- RD2, RD3, RD4, RD5, RD6, RD7, RD7s: unchanged; the switch touches only
  later values. gate: later values get the new grain.

### E6. A person lock

States: **Q0** live; **Q1** destroyed, on date P.
Writes: person forget; person forget again; a write by the person; a write
naming the person as a subject; a read-out of a value wrapped under them.
Related reads: RD1 (answers to their offers), RD2, RD3, RD4, RD5, RD6, RD7,
RD10 (their permissions), gate.

**Q0 × person forget**
- RD1: earlier answers unchanged.
- RD2: values that required them show erased on P; the rest open (the A
  cases: [S A1, A2, A6, A7] close; [S A3, A4, A5, A8] stay open).
- RD3: closed values never match. RD4: erased on P at every moment for them.
- RD5: pending promotions of closed values refused at read-out; crossed and
  done keep their copy unless the copy itself names them [S B2, B4].
- RD6: short entries over closed values cannot re-run.
- RD7: closed since P for those values.
- RD10: their permission facts stay readable (D2); whether usable is O11.
- gate: per O11 for their later offers.

**Q1 × person forget again**
- RD1, RD2, RD3, RD4, RD5, RD6, RD7, RD10: unchanged; the date stays P
  [M op-forget-person]. gate: unchanged.

**Q1 × a write by the forgotten person, or naming them as a subject**
- Not ruled [P 179-180] (O11). A new value cannot be wrapped under a
  destroyed lock. RD1, RD2, RD3, RD4, RD5, RD6, RD7, RD10 and gate follow the
  rig choice; until one is made the offer is refused as data, never thrown.

**Q1 × read-out of a value that required them**
- RD5: refused, source erased [S B2]. RD1: the crossing name answers no.
- RD2, RD3, RD4, RD6, RD7, RD10: unchanged. gate: unchanged.

### E7. A promotion, found by its request's name

States: **X0** none; **X1** request refused; **X2** pending; **X3** crossed;
**X4** done; **X5** refused at the read-out; **X6** refused at the landing;
**X7** crossed with its source erased before the landing offer was durably
sent (OP14).
Writes: request; request resent; value forget of the source; person forget
of the owner; the read-out; the landing; a failover between forward and
landing; the target head moved by another write; a revoke of the landing
permission; a forget of the source after done; a forget of the copy.
Related reads: RD1 (request, read-out and landing names), RD2 (source), RD2t
(target chain), RD4, RD5, RD6, RD7 (source), RD7c (copy).

**X0 × request admitted**
- RD1: request yes; read-out and landing names: no answer yet.
- RD2: source unchanged. RD2t: unchanged. RD4: the request fact from its stamp.
- RD5: pending. RD6: the request's reads on the act. RD7: opens. RD7c: none.

**X0 × request refused**
- RD1: request no. RD2, RD2t, RD4: unchanged. RD5: no status. RD6: none.
  RD7: unchanged. RD7c: none.

**X2 × request resent**
- RD1: the same yes; the read-out is queued again and collapses.
- RD2, RD2t, RD4: unchanged. RD5: pending, or later if it moved.
- RD6: unchanged. RD7: opens. RD7c: none yet.

**X2 × value forget ordered before the read-out**
- RD1: read-out name: no, source erased; landing name: never answered, since
  none is sent.
- RD2: erased on F. RD2t: unchanged. RD4: erased on F at every moment.
- RD5: pending until the read-out decides, then refused [S B1].
- RD6: unchanged. RD7: closed. RD7c: none.

**X2 × person forget of the owner before the read-out**
- RD1: read-out: no, source erased. RD2: erased on P. RD2t: unchanged.
- RD4: erased on P. RD5: pending, then refused [S B2]. RD6: unchanged.
- RD7: closed. RD7c: none.

**X2 × read-out, the source opens**
- RD1: read-out yes; landing: no answer yet.
- RD2: unchanged. RD2t: unchanged (no optimism).
- RD4: the crossing fact in the owner's layer from its stamp.
- RD5: crossed. RD6: O19. RD7: still opens. RD7c: none yet.

**X3 × value forget after the read-out**
- RD1: the landing still decided on its own checks.
- RD2: erased on F. RD2t: the copy once landed. RD4: both, by stamp.
- RD5: crossed, then done [S B3]. RD6: unchanged.
- RD7: closed. RD7c: opens.

**X3 × person forget of the owner after the read-out**
- RD2: erased on P. RD2t: the copy once landed. RD4: both.
- RD5: crossed, then done [S B4]. RD7: closed. RD7c: opens, unless the copy's
  own subjects name the owner. RD1, RD6: unchanged.

**X3 × landing admitted**
- RD1: landing yes. RD2: unchanged. RD2t: the copy is the head.
- RD4: the copy from the landing stamp, later than the crossing fact.
- RD5: done. RD6: unchanged. RD7: unchanged. RD7c: opens under its own wrap.

**X3 × landing refused** (stale head, revoked or missing permission, class)
- RD1: landing no, with its reason. RD2: unchanged. RD2t: unchanged.
- RD4: unchanged. RD5: refused after crossed [P 157-158]. RD6: unchanged.
- RD7: unchanged. RD7c: none.

**X3 × failover between forward and landing**
- RD1: one landing answer. RD2t: one copy [SP 4]. RD4: one landing stamp.
- RD5: crossed until the landing is decided, then done. RD2, RD6, RD7:
  unchanged. RD7c: opens once landed.

**X3 × target head moved by another write before the landing**
- RD1: landing no, stale replace. RD2t: the other write's value.
- RD5: refused after crossed. RD2, RD4, RD6, RD7: unchanged. RD7c: none.

**X3 × revoke of the landing permission before the landing**
- RD1: landing no, revoked. RD5: refused after crossed. RD2, RD2t, RD4, RD6,
  RD7: unchanged. RD7c: none.

**X3 × source erased, then a replay that finds the read-out decided (to X7)**
- RD1: landing name never answered. RD5: crossed, for ever, in the model
  [M forward]. RD2: erased. RD2t: unchanged. RD4: the crossing fact stays.
  RD6: unchanged. RD7: closed. RD7c: none. The rig matches or reports it.

**X4 × request resent**
- RD1: the same answers throughout. RD2t: one copy. RD5: done. RD2, RD4,
  RD6, RD7, RD7c: unchanged.

**X4 × forget of the source**
- RD2: erased. RD4: erased at every moment. RD7: closed. RD7c: opens.
- RD5: done. RD2t, RD1, RD6: unchanged.

**X4 × forget of the copy** (an excision in a group or the base unless marked
own-row, OP9, OP12)
- RD2t: erased on F. RD4: the copy erased at every moment. RD7c: closed.
- RD5: done; the status is about the landing. RD1, RD2, RD6, RD7: unchanged.

**X5 or X6 × request resent**
- RD1: the same answers. RD5: refused. RD2, RD2t, RD4, RD6, RD7, RD7c:
  unchanged.

### E8. A read entry

States: **R0** none; **R1** a short entry, complete; **R2** marked partial;
**R3** exact rows; **R4** a matched value since forgotten.
Writes that make or touch entries: a pattern read in an eager layer; a point
read; a read in an agent session; the promotion of an agent session's act; a
forget of a matched value; the other store admitting a fact dated at or
before the entry's moment.
Related reads: RD6 (find, and re-run), RD3 (the pattern now), RD2 (matched
facts now), RD4 (as of the entry's moment), RD7 (matched values open).

**R0 × pattern read by a deterministic tool, personal or shared layer**
- RD6: one line: pattern, moment, role, fingerprint, complete; a re-run
  reaches the same fingerprint while nothing matched is forgotten.
- RD3: the same match. RD2: the matched facts. RD4: the same at the moment.
- RD7: all matched values open.

**R0 × pattern read by a person or a model**
- RD6: the exact list, always; nobody re-runs it [P 78-80].
- RD3, RD2, RD4: as above. RD7: open.

**R0 × empty pattern read**
- RD6: a line with an empty match [P 76-77]; a re-run is empty unless the
  other store later admitted something dated at or before the moment.
- RD3: empty. RD2: nothing. RD4: nothing. RD7: nothing matched.

**R0 × point read in an eager layer**
- RD6: rows [P 75]. RD3: n/a. RD2: the value read. RD4: the same at the
  moment. RD7: opens.

**R0 × read in an agent session layer**
- RD6: nothing by default [P 82]; the line is added on promotion, which
  waits on 82 (O10, O19). RD3, RD2, RD4, RD7: the read's own result.

**R1 × forget of a matched value (to R4)**
- RD6: the entry is unchanged (never rewritten); a re-run cannot reach its
  fingerprint, as expected [P 148-149].
- RD3: no longer matches it. RD2: erased on F. RD4: erased on F.
- RD7: closed.

**R3 × forget of a matched value**
- RD6: the rows are unchanged; the forgotten one reads erased on F.
- RD3: no longer matches. RD2: erased on F. RD4: erased on F. RD7: closed.

**R1 or R2 × the other store admits a fact dated at or before the moment**
- RD6: a re-run can match more; the store does not promise otherwise (x2)
  [R 97]. RD3: matches it. RD2: shows it. RD4: shows it at the moment now.
  RD7: opens.

**R2 × re-run**
- RD6: may match more or other; the mark says the first was partial; when a
  read is partial is O19. RD3, RD2, RD4, RD7: the current result.

### E9. A key's grammar

States: **G0** no grammar facts; **G1** a grammar naming subjects; **G2**
opaque; **G3** changed after values were written.
Writes: write a grammar; change it; make the key opaque; write a value under
the key; forget a value under the key.
Related reads: RD1 (answers, shape refusals), RD2, RD3, RD7 (wraps), RD8
(the grammar), RD9 (tools that match the key), gate.

**G0 × value write**
- RD1: yes; there is no shape to fail. RD2: the value. RD3: matches.
- RD7: wrapped under the owner in a one-owner layer; in a shared layer about
  no one, so only a value forget or excision closes it.
- RD8: nothing. RD9: tools matching the key find it. gate: unchanged.

**G0 × write a grammar naming subjects (to G1)**
- RD1: yes. RD2, RD3: unchanged. RD7: earlier wraps unchanged (I-P5).
- RD8: the grammar. RD9: unchanged.
- gate: later values get these subjects and a shape check.

**G1 × value write naming Bob**
- RD1: yes. RD2: the value. RD3: matches.
- RD7: wrapped per I-L1 with Bob among the subjects. RD8, RD9: unchanged.
- gate: unchanged.

**G1 × value write failing the shape**
- RD1: no, the value's shape (O1, O20). RD2, RD3, RD7, RD8, RD9: unchanged.
  gate: unchanged.

**G0 or G1 × make opaque (to G2 or G3)**
- RD1: yes. RD2: later values show as opaque. RD3: later values never match;
  earlier indexed values are O20. RD7: unchanged. RD8: opaque.
- RD9: per O21. gate: later values get no shape check and no index.

**G2 × value write**
- RD1: yes; no shape check. RD2: "opaque". RD3: never matches.
- RD7: a lock like any other; where its subjects come from is O20.
- RD8, RD9: unchanged. gate: unchanged.

**G2 × value forget**
- RD1: yes. RD2: erased on F. RD3: never matched. RD7: closed. RD8, RD9:
  unchanged. gate: unchanged.

**G1 × change of the grammar (to G3)**
- RD1: yes. RD2, RD3: earlier values unchanged. RD7: earlier wraps
  unchanged. RD8: the new grammar. RD9: unchanged.
- gate: later values follow the new grammar; a gate in the other store sees
  the change by a read (I-O4, O20).

### E10. A tool

States: **T0** none; **T1** defined; **T2** changed.
Writes: define it; change it; a fact it matches is admitted (the runner runs
it); a replay of the runner.
Related reads: RD1 (answers to its outputs), RD2 (its outputs), RD6 (its
read entries), RD8 (subjects it contributes), RD9 (tools by matching), gate.

**T0 × define**
- RD1: yes. RD2: the tool's facts. RD6: its later reads leave a short entry
  by default, or exact rows if its signature asks [P 80-81].
- RD8: its subject contribution per O21. RD9: found by matching.
- gate: no new compiled step, the target (OP18).

**T1 × a matching fact admitted**
- RD1: an answer per output offer, each named before the first gate and
  because of the trigger. RD2: its admitted outputs.
- RD6: its reads per its preference. RD8: per O21. RD9: unchanged.
- gate: its outputs pass the gates like any offer.

**T1 × runner replay**
- RD1: the same answers. RD2: each output once. RD6: once. RD8, RD9:
  unchanged. gate: repeats collapse by name.

**T1 × change (to T2)**
- RD1: yes. RD2: earlier outputs unchanged. RD6: earlier entries unchanged;
  a read-time preference may change (I-P5). RD8: per O21.
- RD9: matches by the new facts from now. gate: unchanged.

## Open where the rulings are silent

For the plan to pick, each as a rig choice written in RIG.md, never as a
ruling. The simplest pick that keeps every requirement above is SPEC.md's
instruction.

- **O1.** The rig's own refusals, and where each sits in I-G5's order: an
  unknown envelope part or version marker; a missing name or layer; a
  person's offer with no permission (the model refuses it as not covering); a
  person or operator using a store-reserved scheme; a value failing its key's
  shape; an empty act; an act size limit, if any; a layer not made or with no
  class fact; an act mixing store-placed facts with ordinary ones; a forget
  naming an unknown or unseen value.
- **O2.** How no plaintext outlives its lock: the offers a gate reads, a
  landing offer carrying a copy, anything replay keeps (I-L4).
- **O3.** R6: how a gate learns the stamp of what an offer stood on in the
  other store (a read, or carried on the offer); how the micro gate reads a
  re-classed layer's earlier heads, lock rows and permissions as settled.
- **O4.** How a face refusal or a name-taken refusal reaches its offerer as
  data without being recorded as the name's answer.
- **O5.** What a point read returns for several unreplaced facts (the model:
  the latest by stamp); a retract's form and its undo.
- **O6.** A permission's cover beyond person and layer; who may grant and
  revoke; revocation cascading from a session root; re-grant after a revoke;
  a revoke of an unknown or already revoked permission.
- **O7.** Lock grain on shared layers (refused in the model, D11); a grain
  switch inside an act that also writes values.
- **O8.** Per-act locks where an act spans tasks after a re-class, and where
  one act mixes marks (D12).
- **O9.** Re-class back to by layer; re-class of a shared layer; who may
  re-class (the model: the operator).
- **O10.** A session's close (82): offers after it; the agent-session read
  entry line added on promotion.
- **O11.** Writes about someone already forgotten [P 179-180]; a forgotten
  person's own later offers; whether their permissions stay usable.
- **O12.** The person forget as a fact: its layer, name, permission and
  ordering gate; its date without a global stamp (D3).
- **O13.** Whether a person can always forget their own values once their
  write permission is revoked [P 178-179]; the model refuses.
- **O14.** The date a value erased twice shows (the model: a value forget
  after a person forget shows the value forget's date).
- **O15.** Showing, not reasoning, the order of a person's forget against a
  read-out inside the lock store [P 177-178].
- **O16.** Whether the rig has a restore at all (OP11).
- **O17.** A group's rule requiring the mark: refuse unmarked writes or apply
  the mark; where it lives (OP12).
- **O18.** Promotion sources other than the person's own layer, and targets
  other than group and base.
- **O19.** Read entries: what "eager" means (written at the read, or with the
  act); where entries of a read that leads to no act live, above all reads of
  shared layers; when a read is partial; whether an empty point read leaves
  rows; whether a read-out leaves an entry.
- **O20.** Grammars: where their facts live and how the other store's gate
  sees them; a key made opaque after its values were indexed; competing
  grammar facts; an opaque value's subjects; the shape language.
- **O21.** Tools: how a tool names subjects; what matching runs against;
  tools that are not deterministic; what counts as a compiled step in the
  count.
- **O22.** Visibility of answers; whether a layer the reader cannot see reads
  as empty or as refused; the operator's reads.
- **O23.** A read's moment across the two stores: one moment per store, or
  one closed in every store [P 173-177]; a moment later than any stamp given.

PROGRESS.md's other open items (56 fingerprint bytes and the record's written
form, 59, 41, 17 and 60, 3, 44, 83, 77, 82, 84, 85) "change what a kept record
carries, not how the rig is built" [P 172-186]. The rig keeps no records, so
it takes the simplest reading of each where it must touch one, and names it as
a rig choice.
