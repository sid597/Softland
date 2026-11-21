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

## What this page is

**21 November 2026.** Phase 0 for [SPEC.md](SPEC.md), piece 3 of the store's
line: a layer's decisions read by stamp, each with its answer and the thing
whole, for askers who may read them. Written by one Claude session that read
the spec, `src-store/MODEL.md`, and the code that the spec points at. Nothing
was run. It holds requirements only: no table, no depot, no topology, no query.

Each statement carries one of four marks, so that a later phase can tell what
it may turn and what it may not:

- **Sid's**: his decision, as SPEC.md or a "Decisions of …" section holds it.
- **A session's**: a choice of the session that wrote SPEC.md. His to turn.
- **Code**: a fact of the code as it stands, with the file that says it.
- **Derived**: what this page infers from the three above. A later phase may
  challenge the inference; it may not drop the case.

Words, as the store's tree has them. A **thing** is whatever arrived on a
depot. A **decision** is a thing that a gate gave a stamp and an answer: a
taken act or a refused thing. A layer's **line** is its decisions in the order
of their stamps. A **holder** is an asker whose permission stands in the layer
(`softland.store.gate.chain/stands?`). A **visitor** is an asker who may see
the layer and holds no permission that stands in it; she exists only where the
layer's `:softland.store/who-may-see` reads `:everyone`. A thing is given
**whole** when the asker gets it as it arrived; a decision is **withheld** when
she gets its stamp and nothing else. "Key" is a fact's key; this piece has no
lock.

What Phase 0 could not settle is under [Open questions](#open-questions), and
is not a requirement. Where the spec and the code differ is under
[Where SPEC.md and the code differ](#where-specmd-and-the-code-differ).

## Operations

Six operations. O1 and O2 are writes that exist today and that this piece adds
to. O3 and O4 are the new read. O5 is the read kept live, which Phase 1 may or
may not offer. O6 is the second run of the gates, which is an operation of the
store by Sid's decision of 13 October 2026. What exists and must come out
unchanged is listed last.

### O1. A thing arrives and is decided

The walk of one thing (`softland/store/module/walk.clj`, `decide`), for both
gates. This piece adds no new kind of thing, but one new store key (O2), and
whatever Phase 1 has a gate keep for the read is kept here.

**The kinds of thing, and what each is to the read.** Code, each row with its
file. The read must treat the six kinds apart.

| What arrived | A stamp | An answer | In the read (O3) | Whose `:who` can match an asker |
|---|---|---|---|---|
| A stray: no plain map, no act-id, an act-id that is not `[layer depot uuid]`, a layer slot over `max-id-text`, an act-id of the other gate, or an act-id whose layer is not the thing's `:layer` (`door/declaration.clj`, `stray-reason`) | none | none; its digest alone, under its word (`walk.clj`, step 0) | never | nobody's |
| A second thing under an act-id that has an answer, the same thing again or another (`walk.clj`, step 1, check (a)) | none of its own | none of its own: the first's stands | never; the stamp of that act-id gives the first thing | nobody's |
| A thing that the declaration refused (`door/declaration.clj`, `check`) | yes | refused, with the declaration's word; `:time` only when the thing held a usable one | yes | its `:who`, only when `usable-id?` holds for it |
| A thing that passed the declaration and a gate's check refused: its standing or its places (`gate/decision.clj`, `standing`, `places`) | yes | refused, with the gate's word | yes | its `:who`, always an id within the limit |
| A taken act | yes | accepted | yes | its `:who` |
| A thing for the layer that arrived on the other gate's depot, with that depot in its act-id | yes, in the other gate's count for that layer id | refused there: `:layer-belongs-to-the-other-gate` or a declaration's word (`gate/decision.clj`, `standing`) | not in the layer's line; see [Q2](#open-questions) | see Q2 |

Two corrections to what one might assume:

- **Code.** "A thing that is no act at all" splits in two. A thing that is no
  plain map, or holds no usable act-id, is a stray: it has no stamp and is not
  in the read at all. A plain map with a usable act-id of this gate whose
  layer slot equals its `:layer`, and with nothing else right (no `:who`, no
  facts, any other content, any size), is no stray: the declaration refuses
  it, it gets a stamp and an answer, and it is in the read. So every thing in
  the read is a plain map (no record) that holds `:act-id` and `:layer`; no
  more can be assumed of a declaration-refused one.
- **Code.** For a thing that the declaration refused, the walk has no act in
  its one form: `declaration/check` gives `{:refuse reason}` and
  `decision/writes` is handed nil for the act (`walk.clj`, steps 2 and 4;
  `gate/decision.clj`, `writes`). Whatever is taken from such a thing is
  taken from the thing as it arrived, by code that cannot throw on it.

**Latency.**
- **Code.** In a streaming layer the maker's append with the ack level `:ack`
  returns when the gate's event is done (`module/streaming_gate.clj`), so
  every read and write that this piece adds to a decision is added to the
  round trip of every write a person waits on. **Derived:** the addition is a
  small fixed number of reads and writes on the layer's task, plus at most
  work that is linear in the size of the thing. Single-digit milliseconds for
  an act of ordinary size stays the aim.
- **Code.** In the base the ack says only that the depot holds the thing; the
  answer is there when the batch commits on the base's task
  (`module/microbatch_gate.clj`). **Derived:** what this piece adds is
  visible to O3 no later than the answer is: at that commit, on the task
  where the read looks.
- **Sid's** (SPEC, "What must hold", 3): the walk never gives its task back
  between its first read and its last write, and no partitioner, yield, read
  that may yield, or append may be added (`walk.clj`, the namespace
  docstring). **Derived:** so whatever a gate does for this read in a
  decision holds the task's thread for its whole length. The walk already
  takes one digest over the whole thing before the declaration refuses it
  (`src-store/softland/store/README.md`, "How long one act can hold a
  task"); this piece may add work of that order and not of a higher one.

**Throughput.**
- **Code.** One decision for every act and for every refused thing of every
  layer. No bound on what one sender puts into a depot is built, and no way to
  block a sender (`src-store/README.md`, decisions of 12 October, entry 7).
  **Derived:** the cost that this piece adds to a refusal is paid for each
  thing of a flood, in the layer's one line; in the base, on the one task
  that decides every act of the base. The cost of a refusal today is 3 reads
  and 2 writes for a declaration's refusal and 5 reads and 2 writes at most
  for a thing with no standing (`walk.clj`, "What one call costs"). SPEC
  "What must hold", 8, asks that the new cost be said for each kind beside
  those numbers, for both gates.

**Consistency and correctness.**
- **W1. Sid's.** The store's promises stay: an act is taken or refused whole;
  a layer's stamps have no gap, with the refusals in them; a depot is never
  changed or trimmed (`MODEL.md`, "What the store promises").
- **W2. Sid's** (SPEC 1, 2). Whatever a gate comes to keep for the read is
  made of the thing, the rows that the decision read, and the stamp. No
  clock, no table of the other gate, no random number, nothing of a batch or
  of an attempt, no number of a task, and no position in a depot partition
  unless the run-again proof's form with no task can say why it is left out.
- **W3. Derived**, from `walk.clj`, "When a thing is run again". Whatever is
  kept is written behind check (a) and becomes visible together with the
  answer and the layer's last stamp, on the same task: in a streaming layer
  in the one event, in the base in the task's part of the batch. Then no
  reader ever sees a last stamp s for which O3 has no decision at s, and a
  stream event that Rama runs again after a commit writes nothing a second
  time. Each added write is the same write however often it runs.
- **W4. Derived**, from check (a). A second thing under a used act-id changes
  nothing that the read gives for the first: not its thing, not its who, not
  its time.
- **W5. Derived.** A stray adds nothing that the read can give, and no stamp.
- **W6. Code.** No function that the walk calls may throw on what arrives: a
  throw fails the event or the batch, and a thing that throws each time
  stops the partition (`walk.clj`, "What it rests on"; `microbatch_gate.clj`,
  "A throw is no quiet retry"). Whatever this piece reads of a thing in the
  walk (its `:who`, its size, anything) is read so that it cannot throw, on a
  sorted map whose keys do not compare, on a part that throws when read, on a
  thing nested past any depth.
- **W7. Derived**, from SPEC 3 and 4. What a decision adds is bounded by the
  thing. A thing has no bound of the store's when the declaration refused
  it: `:too-many-facts`, `:value-too-large`, `:act-too-large` and
  `:malformed` are the words of exactly the things that are large, and "the
  door bounds nothing: a thing of any size that Rama's client writes reaches
  the gate" (`src-store/softland/store/README.md`). So no step that this
  piece adds may assume a size.

**Data growth and scale.**
- **Code.** A layer's line is unbounded and never trimmed; its stamps run to
  2^63 − 1 (`gate/stamp.clj`). A task of the streaming gate holds every layer
  that hashes to it, "a personal layer for each person among them"
  (`record/tables.clj`), so the number of lines on one task is unbounded too.
- **Code.** A depot partition holds the things of every layer that hashes to
  it, with strays and second things among them, so a layer's decisions are
  not neighbours in it and a position in it is no stamp (`gate/stamp.clj`).
- **Derived.** If a gate keeps the thing a second time, the store holds every
  thing twice, the refused flood too; in the base, a row that lives on every
  task is held N times (`microbatch_gate.clj`, "What the copy costs"). SPEC 8
  asks that what is kept twice, and how much, be said.

**Concurrency.**
- **Code.** A layer is one partition of its depot and is decided one thing at
  a time on its task, in the depot's order; the stamps of a layer go up in
  the order of the calls (`walk.clj`). Reads of that task run between events
  and see committed state only.
- **Code.** After a failure that Rama does not answer with a restart, a later
  act of a streaming layer can in principle be decided before an earlier one
  (`streaming_gate.clj`, "The order of a layer's acts after a failure"; not
  reached in a run). **Derived:** the order of O3 is the order of the stamps,
  not of the depot, also then.

**Edge cases.** The six kinds above, and:
- **Code.** A thing refused `:no-door-time`, and the same thing with a time,
  have one digest (`door/digest.clj`, `without-the-time`). Two sends of one
  act under one act-id differ only in the time on them. See R9.
- **Code.** A string that holds half of a surrogate pair comes off the depot
  with `?` in its place (`src-store/softland/store/README.md`, "What a
  fact's row holds for a value"). "As it arrived on the depot" means as Rama
  gives it back, which is what the gate took its digest of.
- **Code.** A thing that a topology appended to another partition than its
  layer's would be decided on another task (`README.md`, "Where a thing sits
  on its depot"). No client can make one and no topology of the module
  appends. **Derived:** this piece adds no topology that appends to a depot
  of acts.

### O2. The layer's setting is written: who may read its acts whole

**Sid's:** "unless the layer turns it on". **A session's:** its form, a sixth
store key on the layer's own entity, working word
`:softland.store/who-may-read-acts`, values `:members` and `:everyone`, read
as `:members` while no fact says it, written again only by the layer's first
holder. It is an act like any other, decided by O1, and it is itself a
decision in the line that it governs.

**Latency.** That of any act of its layer. Its effect on reads begins when its
row is visible on the task where a read reads the setting: in the base that
is task by task, at instants that differ (`microbatch_gate.clj`, "What a
reader sees, and when").

**Throughput.** Rare: a few writes in the life of a layer, by one holder.

**Consistency and correctness.** Each follows from the key being one of
`store-keys` with `:setting true` and `:written-again true`
(`door/declaration.clj`); Phase 1 confirms each against the code that it
changes, since each is a rule of a gate.

- **S1. A session's.** With no fact, every layer reads as `:members`, the
  base too. **Code:** the base reads as `:everyone` under
  `:softland.store/who-may-see` with no fact
  (`gate/first_act.clj`, `starts-with`), so the base is an open layer whose
  acts are not open: every asker who is not a holder of the base is a
  visitor of it.
- **S2. A session's**, with **Code** (`gate/decision.clj`,
  `who-may-write-fault`). Only an act under the layer's first permission
  writes it; under a given permission it is refused
  `:only-the-first-holder-writes-a-setting`, whatever that permission allows.
- **S3. Code**, by the rules that hold for the other three settings. Four
  follow from the key's entry in `store-keys` with no new check. A fact
  under the key on an entity that is not the layer is refused
  `:setting-not-on-the-layer` (`gate/decision.clj`, `setting-fault`, which
  reads `setting-keys`). A fact with no value under it is refused
  `:store-key-retract-not-built` (`store-key-fault`): the setting can be
  written again and never retracted. A layer cannot mark the key
  (`:marked-key-in-the-stores-namespace`). A making act cannot carry it: its
  form is exactly two facts (`gate/first_act.clj`, `making-act?`), so it
  takes a second act, as SPEC says. One does not follow by itself and is
  **derived**: a value that is neither word is refused by the declaration,
  with a word of its own, as each of the other three settings has a rule for
  the shape of its value (`declaration/rules`).
- **S4. Derived**, from `declaration/rules`: "the order of the declarations
  is the order of the reasons". Where the new rule stands among the rules
  decides the word of a thing with two faults. The place is chosen so that no
  thing that is refused today gets another word, except a thing whose only
  fault was this key.
- **S5. Code.** Today a fact under this key is refused
  `:store-key-not-known`, since the store owns its whole namespace
  (`declaration/rules`, `:store-key-is-known`). After this piece it is taken.
  That is a rule of a gate changed on purpose (**Sid's**, 13 October, entry
  6): the fixture is made again and the receipt says so. **Derived:** a depot
  that held such a thing before this build would come out with another
  answer on a second run. The kept fixture holds no such thing (its one
  unknown key is `:softland.store/no-such-key`).
- **S6. Derived.** The setting as it reads now governs the whole line, the
  decisions from before the write too: SPEC says "With `:everyone`, whoever
  may see the layer reads its acts whole", and names no stamp from which.
  Written back to `:members`, visitors are withheld from the whole line
  again. What an asker read while it was on stays read; nothing can take it
  back.
- **S7. Derived.** The setting has an effect only in a layer that everyone may
  see. In a layer that reads `:members` under `who-may-see`, whoever may see
  it is a holder and reads every act whole either way.
- **S8. Derived.** The setting is a fact on the layer's own entity in the
  store's namespace, so it lies in the layer's own rows
  (`record/rows.clj`, `own-rows-run`) and any asker who may see the layer can
  read it as a fact, with `:stores-own-facts true`, as she reads who may see
  the layer today (`read_path/answer.clj`, `counts?`).
- **S9. Derived.** A row under the key whose value is neither word reads as
  `:members`, the answer that shows less, as `layers/who-may-see` does for
  its own key. No write of a gate gives such a row.

**Data growth.** One row for a layer; in the base, one on every task.

**Concurrency.** Writes of it are acts of one layer and have the line's order;
the last taken one stands. A read in flight: R13.

**Edge cases.** Written twice with the same word: a new decision, a new row
with a new stamp, and no change for any asker. Written by `:system` for the
base: the system is the base's first holder (`first_act.clj`), and any program
that can send can name it (`README.md`, "What the gate does not check").
Written in a layer that reads `:members` under `who-may-see`: taken, and no
effect until that setting is `:everyone`.

### O3. The read: a layer's decisions by stamp, asked once

One layer, a start stamp, a greatest count. For each decision: its stamp, its
act-id, its answer, and the thing whole; or its stamp alone when the asker may
not read it. And where the next read begins.

**Latency.** Hundreds of milliseconds are acceptable; tens are the aim for a
page of ordinary acts. It is a read that a person can wait on (an author
looking at what was refused, an owner looking at a layer's refusals) and that
an importer and the trail call in a loop (the runtime's page, pieces 5 and 8).
**Code:** every wait of the read path has one bound for the whole call,
`:wait-ms`, 10,000 (`read_path/query.clj`), and a read that passes it gives
the word `:get-an-answer-within-its-wait`. **Derived:** the number of round
trips of one read does not grow with the number of decisions that it gives;
SPEC 8 asks that seeks and round trips be said.

**Throughput.** What drives it: an importer reading a whole layer from stamp
0, page after page; the trail; a caller that polls from "where the next
begins" to learn of new decisions; an author who has her act's stamp from
the door's answer (`door/words.clj`, `answer`) and asks for that one stamp.
So two access patterns dominate: a forward run from a stamp, and one
decision by its stamp. A layer's refusals alone are not asked for; a caller
who wants them reads the line and keeps the refused ones, and the cost of
that is said (SPEC 8).

**Who may read what.** The whole rule, as one table. "Sees" is
`read_path/layers.clj`, `standing`. "Holder" is `permission-state` giving
`:stands`. The acts setting is O2.

| The layer | The asker | What she gets |
|---|---|---|
| Not made | anyone | The layer left out, `:not-made`. Nothing of its stamps, though the layer id may have refusals with stamps (E1, S1) |
| Made; she may not see it (`who-may-see` reads `:members`; she holds no permission that stands, or is not signed in) | anyone | The layer left out, `:may-not-see`. Nothing of its stamps, and nothing of her own things there. **A session's** (SPEC, "Who may read what", last line); see [Q1](#open-questions) |
| Made; she is a holder, `:see` or `:see-and-act` | holder | Every decision whole, taken and refused, from stamp 0, those from before her permission too |
| Made; everyone may see it; acts setting `:members` or no fact | a signed-in visitor | Whole: each decision whose thing names her as `:who`. Withheld: every other |
| The same | not signed in | Every decision withheld |
| Made; everyone may see it; acts setting `:everyone` | anyone, not signed in too | Every decision whole. See [Q6](#open-questions) |

- **R1. Sid's**, with the narrowing that he agreed to. A refused thing is read
  whole by its own who and by whoever holds a permission in the layer; a
  visitor of an open layer reads none but her own unless the layer turns it
  on.
- **R2. A session's reading, to be shown to Sid.** A taken act whole is held
  to the same rule as a refused one. Phase 1 builds to it and keeps the rule
  in one place, so that the other answer (a taken act whole to whoever may
  see the layer) is one change.
- **R3. Derived.** Rights are judged as they stand at the read, not as they
  stood at each decision. A holder who was given her permission at stamp 100
  reads stamps 0 to 99 whole. An asker whose permission has ended reads as a
  visitor in an open layer and is left out of a closed one, for the acts
  that were taken under her permission too.
- **R4. Code**, and it differs from a read of facts. Under `who-may-see`
  `:everyone` the read path today does not read the asker's permission at
  all: "Under 'everyone' her permission shapes nothing"
  (`read_path/sources.clj`, `reading`, lines 482–486 and 523–527). For this
  read it shapes everything: in the base, and in every open layer, whether
  she is a holder decides between whole and withheld. So this read reads her
  permission, and the rows above it, in an open layer too.
- **R5. Derived.** "Its own who" is a comparison of two things that nobody
  has checked. The asker is what the caller says (`read_path/query.clj`,
  "Not decided"); the `:who` of a thing is what its sender said
  (`MODEL.md`, "Who sent an act"). So in this build any caller that names an
  asker reads that asker's things, and any sender can put a thing into a
  layer's line that names another person as its who, which she then reads as
  her own. The read holds neither; the runtime will hold the asker against
  the caller and the who against the sender (the runtime's page, entry 2).
  The page of the read says this plainly.
- **R6. Derived.** The comparison is of ids: the asker, an id within the
  limit, equals the thing's `:who` as it arrived. For a declaration-refused
  thing the `:who` can be absent, a string, a symbol, a keyword that does
  not read back, an id over the limit, or a part that throws when read: each
  of these matches no asker, and none throws. `:system` is a who like any
  other.
- **R7. Code.** No row of the store holds the who of a refused thing today:
  an answer's row is `{:refused :stamp :time :digest}`
  (`record/tables.clj`, `answers`), and the who of a taken act is only in
  the rows of its facts while they are current. Where the who of a decision
  comes from at a read is Phase 1's to say, and its cost is in SPEC 8's
  count.

**Consistency and correctness.**
- **R8. Sid's** (SPEC 6). The answer given with a decision is the one that
  the door's read by act-id gives: the same verdict, reason, stamp, time and
  digest that `record/rows.clj`, `answer-of`, makes of the answer's row
  under that act-id. A caller who then asks the door by that act-id gets an
  equal answer.
- **R9. Derived**, from R8 and `door/digest.clj`. The thing given whole is
  the thing that the gate decided: its digest
  (`softland.store.door.digest/hex-of`) is the answer's digest, and its
  usable time (`declaration/usable-time`) is the answer's `:time`, absent
  when the answer has none. The second matters because a digest leaves the
  time out: a second send of the same act, and the same thing with and
  without a time, share a digest with the decided thing and are not it.
  A thing is never given in part as if it were whole: when it cannot be
  given whole, the entry says so (R15).
- **R10. A session's**, made exact here. The stamps that one read gives are
  consecutive from its start: each stamp from the start up to the last one
  given is in the answer once, in the numeric order of the stamps, as a
  whole decision or as a withheld one. A withheld decision is its stamp and
  nothing else of it: no act-id, no verdict, no reason, no time, no digest,
  no size. No entry is given for a stamp past the layer's last stamp: a
  withheld entry is never made up from the range that was asked.
- **R11. Derived.** Pages join. Reading from "where the next begins" gives
  the next stamp and no stamp twice, also when the layer grew between the
  two reads. A decision's stamp, act-id, answer and thing never change once
  given (`module/door_reads.clj`, "A row that a read gave does not change").
  The one thing about a stamp that can differ between two reads is whether
  this asker gets it whole.
- **R12. Sid's** (SPEC 7). Nothing is shown that the asker may not read, also
  in the moment in which a permission ends. **Derived**, to make it a
  statement that a proof can hold: one answer is judged by one reading of
  the asker's rights (her permission row, each row above it, the layer's two
  settings), and that reading is not older, in the layer's line, than the
  newest decision that the answer gives whole on its ground. Named exactly:
  if the end of her permission, or of one above it, has a stamp below the
  layer's last stamp as this answer reports it, she is given nothing whole
  as a holder.
- **R13. Derived**, the same for the setting. A read that is in flight when
  the setting is written gives either what the setting before allowed or
  what the setting after allows, by the same rule as R12. Two reads stand
  apart: a first page whole under `:everyone` and a second withheld under
  `:members` is right.
- **R14. Code**, the ground of R12 in the base. A read of facts reads the
  base on the task of the read's home, where the rows of facts are a copy
  that can be one commit behind the base's own task
  (`read_path/sources.clj`, "Where a layer is read"; `read_path.clj`, "A
  microbatch layer"). The base's answers and its last stamp are on the
  base's task alone (`record/rows.clj`, "Where a row lives"). **Sid's**
  (SPEC 5): the read of the base's acts says on which task it reads each
  thing that it needs (the decisions, the last stamp, her permission and the
  rows above it, the two settings) and why that task holds it. **Derived:**
  the place chosen for her rights must hold R12, which a copy on another
  task than the one that holds the decisions does not hold by itself.
- **R15. Derived**, from SPEC 4 and `read_path/query.clj`
  ("None bounds the bytes of a run"). A count alone bounds nothing here: an
  act that passed the declaration can write 18,874,368 bytes into rows
  (`max-act-bytes`), and a refused thing has no bound. So one read has a
  bound on the bytes of what it moves from Rama into the runtime and of what
  it hands over, a number of the build beside the count. The bound holds
  when a single thing is larger than it. Such a thing does not stop the
  line: the read still gives its stamp, its act-id and its answer where the
  asker may read it, says that the thing is too large to be given in this
  read, and "where the next begins" moves past it. What is said of it is
  distinct from "withheld", which is about rights. Whether a thing over the
  bound can be got another way is [Q4](#open-questions).
- **R16. Derived.** The bound of R15 holds over what is fetched, not only
  over what is handed. Today the read path can fetch a row that the asker
  may not see and drop it, because a row is small
  (`read_path.clj`, "What crosses from Rama to the runtime"). A visitor's
  read of an open layer withholds almost everything; the cost of that read
  is said, and it does not grow with the bytes of what is withheld.
- **R17. Code.** No call of the read path throws for what a caller gave or
  for what Rama did, and "a read that failed has never said 'no fact'"
  (`read_path.clj`). **Derived:** the read gives an answer or one of the
  read path's words; it never throws on a thing, and a failed read never
  reads as "no decisions" or as "the end of the line".
- **R18. Code.** The check of who may see is in one place, the read path
  (`MODEL.md`; `src-store/README.md`, 11 and 12 October, entry 4). **A
  session's:** this read goes through it. **Derived:** who may read a thing
  whole is decided by pure code in that one place, on rows, so that a pure
  proof can hold each row of the table above without a cluster (SPEC,
  "Proofs").
- **R19. Code.** The read path sends Rama only the five forms of path that
  `path-of` builds, because "Rama does not refuse a path that it cannot
  serve cleanly (one such select stopped a worker of the in-process
  cluster)" (`read_path.clj`). **Derived:** any new form of call that this
  read makes to Rama is shown by a run to be served cleanly before it is
  relied on, and what a caller gave is checked before any key or call is
  made of it.
- **R20. Code.** A layer id is a readable keyword or a UUID, and a readable
  keyword can hold a letter from U+E000 on or outside the first plane. For
  keys that hold such a letter Rama was seen to give fewer rows of a range
  than the table holds (`read_path/sources.clj`,
  `outside-the-agreed-range?`, `first-read-holds?`). **Derived:** the read
  gives every decision of such a layer, or turns the layer away with a word;
  it never gives fewer and says nothing.

**Data growth and scale.** A line is unbounded; one read is bounded in count
and in bytes. The order is the numeric order of stamps of any number of
digits, up to 2^63 − 1.

**Concurrency.**
- **Derived.** A read that runs while the layer's gate decides sees a prefix
  of the line: every stamp from 0 to some last one, none missing (W3).
- **Code.** A task runs one event at a time, so a read on a layer's task
  waits for a walk and a walk waits for a read. In the base every decision
  is on the one task that also decides every act of the base
  (`microbatch_gate.clj`, "What it is built for"). **Sid's**, 13 October,
  entry 4, gave the base's facts a copy on every task so that reads of the
  base do not all run on one thread. **Derived:** SPEC 5 and 8 together ask
  what reads of the base's acts cost that one task, or what it costs to keep
  them elsewhere. A read, unlike the walk, may give its task back, and one
  read of a page of large things does not hold a task's thread for the whole
  page.
- **Derived.** Many askers read one layer at once; the read keeps nothing of
  a read and writes nothing (`read_path.clj`, "State"), so they do not
  touch each other.

**Edge cases.**
- **The start is past the layer's last stamp.** An ordinary answer: no
  decisions, the last stamp, and a next start that is not below the one
  asked. A poller rests on this.
- **The start is the last stamp plus one, and a decision lands during the
  read.** Either answer is right: empty with the old last stamp, or the new
  decision with the new one. Never the new last stamp with no decision.
- **Stamp 0.** The first decision of a layer id, which need not be the
  making act: things sent before the layer was made are refused
  `:layer-not-made` and take stamps 0, 1 and on: a refusal writes its answer
  and the layer's last stamp whether or not the layer is made
  (`walk.clj`, step 7; `gate/decision.clj`, `standing`). Once the layer is
  made, its holders read those from stamp 0.
- **No decision at all under the layer id.** The layer is not made; the
  answer is "left out, `:not-made`". "No last stamp" is told from "last
  stamp 0".
- **A made layer always has a decision,** its making act, so an asker who
  may see a layer never gets an empty line from stamp 0.
- **What the caller gave is wrong:** a start that is no whole number, below
  0, or past the long range; a count that is 0, negative, no number, or over
  the bound; a layer that is no id within the limit; an asker and
  `:not-signed-in` both or neither; a list of layers where one layer is
  asked. Each is turned away by the check of the query with a word and no
  call to Rama, or served as said (a count over the bound may be cut to the
  bound, and then the answer says how many it took). None throws.
- **A layer of the other gate's depot, or a layer that is not made.** The
  read looks in the record of the depot that the layer starts with
  (`read_path/sources.clj`, `gate-of`; `gate/first_act.clj`,
  `depot-at-making`) and gives that line or "left out".
- **One layer only.** The read does not walk the layer's builds-on list and
  joins no two lines: stamps of two layers have no order between them
  (`gate/stamp.clj`).
- **The asker's own permission row is the place she asks about, or she is the
  first holder, or `:system`.** No special case: the first permission does
  not end (`gate/decision.clj`, `end-fault`).
- **A thing that is not plain data:** a record inside it, a run of bytes, a
  sorted map, a Java collection, a part nested past 64 collections, a class
  that the reading process does not have. Giving it does not throw
  (R17); in what form it is handed is [Q5](#open-questions).
- **The act that writes the setting, and the end of the asker's own
  permission, are themselves decisions in the range.** No special case: R12
  and R13 say what she gets.
- **The same read twice.** The same answer, but for decisions since and
  rights that changed.

### O4. The layer's last stamp, asked with the read

**Latency and throughput.** That of O3; it is asked with every page, and by a
poller alone.

**Consistency and correctness.**
- **L1. Code.** The last stamp of a layer is the number under the layer's
  text in the gate's table for its task, on the layer's task alone, for both
  gates (`record/tables.clj`, `task`).
- **L2. Derived.** It is given only to an asker who may see the layer; an
  asker who may not is told that the layer is left out and "nothing of its
  stamps" (SPEC). A visitor gets it: she gets every stamp anyway.
- **L3. Derived.** In one answer the last stamp is not below the highest
  stamp that the answer gives. A caller has reached the end exactly when the
  next start is the last stamp plus one.
- **L4. Derived.** A layer id with no decision has no last stamp; that is not
  0.
- **L5. Derived.** It is the count of the layer's decisions less one, and so
  tells a visitor of an open layer how many things were sent into it, the
  refused among them. The withheld stamps tell her the same. That is the
  spec's choice ("so that the stamps that a caller gets have no gap"); the
  page of the read says it.

**Edge cases.** The base read from another layer's home: the last stamp is
not on that home's task (R14). A layer id that has decisions only in the
other gate's record: no last stamp in its own line (Q2).

### O5. The same read kept live (only if Phase 1 offers it)

SPEC leaves it to Phase 1; a read asked once is the floor. If it is offered,
these hold; if it is not, Phase 1 says how a caller learns of new decisions
(the poll of O3) and what that costs.

- **V1. Derived.** Every decision is told once, in the order of the stamps,
  with none left out, or the caller can tell that some were and reads them
  with O3. **Code:** a live read of facts does not have this property: "two
  acts on one place can come as one change", Rama can leave a reader out of
  a change, and "a caller that must see every act cannot rest on this call"
  (`read_path.clj`). The trail and an importer must see every act.
- **V2. Sid's** (SPEC 7, piece 1). It leaves the layer, and says so, when
  the asker's permission ends, or one above it. **Derived:** from that
  moment she is told nothing whole on a holder's ground; in an open layer
  the read may go on as a visitor's or end, and Phase 1 says which.
- **V3. Derived.** When the acts setting goes from `:everyone` to
  `:members`, a visitor is told nothing whole from then on; when it goes the
  other way, what she is told of the decisions that she was withheld is said.
- **V4. Code.** Rama bounds the pushes on one table on one task, 1,000 by
  default, and the read path holds 900 under one routing id
  (`read_path/query.clj`). A streaming layer is whole on one task, and the
  base's decisions are on one task. **Derived:** what a live read of acts
  takes of those bounds is said, for one layer with many askers.
- **V5. Code.** An asker's function must return at once, and the size of
  what a push carries is the size of the things (`read_path.clj`, "A live
  read, and the read's turn"). R15 holds for what is pushed too.

### O6. A second run of the gates over the depots

**Sid's**, 13 October 2026: "The store has to come out the same when it is
run again from its depots", and every later build reproduces the fixture's
tables from the fixture's acts, "also with another number of tasks".

- **A1.** Whatever the gates keep for this read is in the comparison of the
  run-again proof: row for row between `Store` and `StoreAgain` on 4 tasks,
  and against the fixture on a fresh store of 4 tasks and of 8
  (`test-store/softland/store/run_again_proof.clj`, checks 1 to 3).
- **A2. Code.** The form with no task compares a row by its key alone. A
  table whose rows are on the layer's task alone is the union of the tasks'
  rows; a table of which every task holds a whole copy is that one map, and
  each task's copy is held against it; what a gate keeps for a task is in
  the form as rows under a key that names no task (`run_again_proof.clj`,
  "The form with no task"). **Derived:** what this piece keeps fits one of
  those three, with a key that is the same on a store of 4 tasks and of 8,
  or the proof's form says by name why a part is left out.
- **A3. Sid's** (SPEC 2). A position in a depot partition differs between a
  store of 4 tasks and one of 8: the proof feeds the fixture's partitions in
  another order and onto another number of partitions. So does the number of
  a task. Neither is in a kept row unless A2's exception is written.
- **A4. Code.** With 8 tasks the proof feeds the partitions in the reverse
  order and the microbatch depot before the streaming one. What is kept
  hangs on the order of one layer's things alone, never on the order of two
  partitions or of the two depots.
- **A5. Code.** `StoreAgain` calls `module/add-gates-and-reads`, the one
  function that `Store` calls. **Derived:** whatever this piece adds to the
  module is added through it, so that the second module cannot run another
  gate than the store's.
- **A6. Code.** The fixture's `tables.edn` tells a run of bytes from a string
  by its place: the one position that is bytes is `:digest` of an answer's
  row, and the making writes nothing when it finds bytes elsewhere; a value
  that EDN does not hold as itself fails the making
  (`run_again_proof.clj`, "Bytes", "Making the fixture"). **Derived:** if a
  gate comes to keep a thing, or bytes of one, the proof's rule for bytes
  and for what EDN holds is changed with it and said; the hand sends things
  that the fixture can hold, and a proof on the cluster holds the things
  that it cannot (a record, bytes, a sorted map).
- **A7. Sid's** (SPEC, "Proofs"; 13 October, entry 6). A rule of the store
  changes on purpose (S5), so the fixture is made again, the hand gains
  things that write and read the setting, and the receipt says so. It is not
  made again to make a failing check pass.
- **A8. Derived.** The same holds on a second attempt of a batch and on a
  stream event run again (W3): the proof's check 4 covers what this piece
  keeps.
- **A9. Derived**, and easy to miss. A PState is not made again from a depot
  when a module is updated: a gate resumes where it stood
  (`.claude/skills/rama/SKILL.md`, "Worker restart does NOT replay depot
  history"). So a store that held decisions before this piece is deployed
  holds nothing that this piece keeps for them. R10 says the line has no gap
  from stamp 0. Phase 1 says how the decisions from before the build are
  read: by a second run from the depots, which is the aim of 13 October and
  what the runtime's page expects before real data is kept, or another way.
  It is not left unsaid.
- **A10. A session's** (the runtime's page, "The store's line"). The
  statements of a new rule of a gate go into the reference of
  `src/proposal/runtime-2026-11-21/gate-histories/` before the rule is
  built. The new key's rules (S2, S3) are such statements.

### What exists and must come out unchanged

- **The door's read by act-id** (`module/door_reads.clj`, `answer-by-act-id`):
  the same four answers, one read of one key on the layer's task.
- **Every read of facts** through the read path, once and live, in its
  rounds, its sources and its numbers; and the rule that under `:everyone`
  a read of facts reads no permission (R4 is for this read only).
- **The strays** and their counts.
- **The cost of a decision** that this piece does not need to touch: no read
  is added to a kind of thing for which the read needs nothing.
- **The pages that say what cannot be got back** change with the build
  (`MODEL.md`, "What can be asked", "What cannot be got back"; the read
  path's count of its numbers and of its calls or words). **Derived:** after
  this piece an act by its act-id is got back in two reads, the door's
  answer for its stamp and this read for the thing, by an asker who may read
  it; "what an act carried besides its facts" is got back for a decision
  that is read whole; a layer's refusals are got by reading the line. The
  thing under a used act-id that was not the first stays out of every read,
  and so does a stray. `MODEL.md` says both.

## Entity State × Write Matrix

Five entities. The reads listed in every row:

- **range**: O3, for each kind of asker: **H** a holder; **Vo** a signed-in
  visitor whose id the thing names as `:who`; **V** any other signed-in
  visitor; **N** not signed in; **X** an asker who may not see the layer.
  V, Vo and N exist only where everyone may see the layer.
- **last**: O4.
- **door**: the door's answer by act-id, for the thing's sender.
- **facts**: a read of facts through the read path.
- **live**: O5, if it is offered.

### E1. The line of a layer id L, in the record of the depot that L starts with

States: **none** (no thing was ever decided under L); **refusals only** (L is
not made, last stamp k); **made** (last stamp k).

```
none x a right making act arrives (taken)
  - range: H (the first holder) gets stamp 0 whole, accepted. In the base,
    which everyone may see, the first holder is :system; every other asker
    is V or N and gets stamp 0 withheld. In any other layer who-may-see reads
    :members, so every asker but a holder is X: left out, :may-not-see. For
    a personal layer made by :system for its person, the asker :system holds
    no permission there and is X, though the thing names it as its who.
  - last: 0, to who may see the layer.
  - door: accepted, stamp 0.
  - facts: the layer's depot row and the first permission; in the base on
    every task.
  - live: one decision told to H.

none x a thing that is refused arrives (by the declaration, or :layer-not-made)
  - range: left out, :not-made, for every asker, the thing's own who too.
    Nothing of its stamp. State becomes "refusals only", last stamp 0.
  - last: not given: the layer is left out.
  - door: refused with the word, stamp 0; no :time when the thing held none.
  - facts: none; the layer reads as not made.
  - live: nothing; a live read of a layer that is not made holds no line.

none x a second thing under a used act-id
  - cannot be: no act-id of L has an answer.

none x a stray whose :layer is L
  - range, last, door, facts, live: nothing changes. The door says
    "nothing yet" for ever. The gate's set of strays holds its digest.

none x a thing for L arrives on the other gate's depot
  - range: unchanged, left out, :not-made: the read looks in L's own record.
  - last: not given.
  - door: refused (:layer-belongs-to-the-other-gate or a declaration's word),
    with a stamp of the other gate's count for L. That stamp is not a stamp
    of L's line (Q2).
  - facts: none.
  - live: nothing.

refusals only (last k) x a right making act arrives (taken)
  - range: H now reads stamps 0 to k+1 whole: the refused things that came
    before the layer was made, from any sender, of any size (R15), and the
    making act. In the base V and N get k+2 withheld stamps, and Vo her own
    whole.
  - last: k+1.
  - door: accepted, stamp k+1. The earlier refusals' answers are unchanged.
  - facts: the depot row and the first permission.
  - live: H is told the making act; whether she is told 0 to k is O3's
    business, not the live read's.

refusals only x a refused thing arrives
  - range: left out, :not-made, as before. last stamp becomes k+1, unseen.
  - last: not given.
  - door: refused, stamp k+1.
  - facts: none.
  - live: nothing.

refusals only x a second thing under a used act-id
  - range, last, facts, live: nothing changes.
  - door: the first thing's answer; "not mine" to the sender of another thing.

refusals only x a stray, or a thing on the other gate's depot
  - as under "none".

made (last k) x a taken act arrives
  - range: stamp k+1. H whole with "accepted". Vo whole. V and N withheld.
    X left out. With the acts setting :everyone: V and N whole too.
  - last: k+1 to H, Vo, V, N; not given to X.
  - door: accepted, stamp k+1, the time.
  - facts: the rows of its facts, to who may see the layer; a visitor reads
    the facts and not the act (R2).
  - live: told once, at k+1, whole or withheld by the same rule.

made x a thing that a gate's check refuses arrives (it passed the declaration)
  - range: stamp k+1. H whole with "refused" and the reason. Vo whole: its
    :who is a usable id. V and N withheld. X left out, her own thing too (Q1).
  - last: k+1.
  - door: refused, the word, stamp k+1, the time.
  - facts: unchanged: no fact of a refused thing is in a table.
  - live: told once.

made x a thing that the declaration refuses arrives
  - range: stamp k+1. H whole, whatever the thing is, within R15. Vo whole
    only when the thing's :who is a usable id equal to hers; a thing with no
    usable :who is read by holders alone. V and N withheld. X left out.
  - last: k+1.
  - door: refused, the declaration's word, stamp k+1; no :time when the thing
    held no usable one.
  - facts: unchanged.
  - live: told once; R15 holds for what is pushed.

made x a second thing arrives under an act-id that has an answer
  - range: unchanged. The stamp of that act-id still gives the first thing,
    with the first thing's time (R9), whether the second is the same act
    again or another. The second is in no read.
  - last: k, unchanged.
  - door: the first's answer to a sender whose digest it is; "not mine" to
    the sender of another thing.
  - facts: unchanged.
  - live: nothing told.

made x a stray whose :layer is L arrives
  - range, last, door, facts, live: nothing changes.

made x a thing for L arrives on the other gate's depot
  - range: L's line unchanged.
  - last: k, unchanged.
  - door: refused there, with a stamp of the other gate's count for L, which
    can equal a stamp of L's line and is not one (Q2).
  - facts: unchanged.
  - live: nothing told.
```

### E2. One act-id of a made layer

States: **unused**; **answered** (a first thing was decided under it).

```
unused x a thing arrives under it
  - range: a new decision at the next stamp, by E1.
  - last: raised by one.
  - door: its answer.
  - facts: its facts when taken.
  - live: told once.

answered x the same thing arrives again with another time on it
  - range: unchanged; the thing given at that stamp carries the first time.
  - last: unchanged.
  - door: the one answer, read by both sends as their own (one digest).
  - facts: unchanged.
  - live: nothing told.

answered (the first held no time, refused :no-door-time) x the same thing with a time
  - range: unchanged; the thing given is the one without a time, and the
    answer holds no :time (R9).
  - last: unchanged.
  - door: the refusal, read as her own by the second sender: one digest
    (door/digest.clj says so; not run).
  - facts: unchanged; the act is not taken.
  - live: nothing told.

answered x a different thing arrives under it
  - range: unchanged; the different thing is never given, to a holder
    neither. MODEL.md, "What cannot be got back", says so.
  - last: unchanged.
  - door: "not mine" to its sender.
  - facts: unchanged.
  - live: nothing told.
```

### E3. The acts setting of a made layer L

States: **no fact** (reads `:members`); **`:members`**; **`:everyone`**. Each
write below is an act and so also a decision of E1: a new stamp, read by the
rule of E1. Listed here is what changes beside that.

```
no fact, or :members x the first holder writes :everyone (taken)
  - range: where everyone may see L, V and N now get every decision whole,
    from stamp 0, the refused things of strangers too. H as before. Where
    who-may-see reads :members: no change for anyone (S7).
  - last: unchanged in who gets it.
  - door: accepted.
  - facts: the setting's row on L's own entity; readable as a fact by who may
    see L, with :stores-own-facts true. In the base, on every task.
  - live: V3: what a visitor is told of the decisions that she was withheld
    is said by Phase 1.

:everyone x the first holder writes :members (taken)
  - range: V and N are withheld from the whole line again, Vo keeps her own.
    A read in flight gives the before or the after by R13.
  - last: unchanged.
  - door: accepted.
  - facts: the row holds :members.
  - live: a visitor is told nothing whole from then on.

any x the first holder writes the word that it already reads as (taken)
  - range: one new decision; no change in what anyone gets.
  - last: raised by one.
  - door: accepted.
  - facts: the row again, with a new stamp.
  - live: the decision is told; nothing else.

any x a holder of a given permission writes it
  - range: a refused decision, :only-the-first-holder-writes-a-setting; the
    setting is unchanged.
  - last: raised by one.
  - door: refused with that word.
  - facts: unchanged.
  - live: the refused decision is told.

any x a fact with no value under the key, a value that is neither word, or the key on another entity
  - range: a refused decision (:store-key-retract-not-built; the
    declaration's word for the value; :setting-not-on-the-layer); the setting
    is unchanged.
  - last: raised by one.
  - door: refused with the word.
  - facts: unchanged.
  - live: the refused decision is told.

any x who-may-see is written to :everyone or to :members (see E5)
  - the acts setting's row is not touched; what it does changes with E5.
```

### E4. One asker's permission in a made layer L

States: **none**; **stands, `:see`**; **stands, `:see-and-act`**; **ended**;
**beneath an end** (her row holds a permission and one above it no longer
stands, or was given again later); **given again**. The table of O3 gives
what each state reads: "stands" is H; every other state is X where
who-may-see reads `:members`, and V or Vo where it reads `:everyone`.

```
none x she is given a permission, :see or :see-and-act (a taken give)
  - range: she becomes H: every decision whole from stamp 0, those from
    before the give too (R3). :see is enough.
  - last: given.
  - door: the giver's act accepted.
  - facts: her permission row; she now sees a :members layer.
  - live: a read that she opens now is a holder's.

stands x her permission is ended, by herself, her giver, or a holder above
  - range: in a :members layer she is X: left out, and her own taken acts and
    refused things with it (Q1). In an :everyone layer she is Vo for the
    things that name her and V for the rest. A read in flight: R12. Her next
    read from "where the next begins" gives no thing whole as a holder.
  - last: not given in a :members layer; given in an :everyone layer.
  - door: the end accepted; her own later acts refused :permission-ended.
  - facts: her row is an end; a read of facts leaves the layer out or reads
    it as a visitor.
  - live: the read leaves the layer and says so (V2).

stands x a permission above hers is ended
  - range, last: as if her own were ended; her row is unchanged and her
    permission does not stand (gate/chain.clj, stands?). The read must read
    the rows above hers to know it (R4, R12).
  - door: her later acts refused :permission-above-ended.
  - facts: her row unchanged; reads of facts treat her as holding none.
  - live: leaves the layer.

ended x she is given a permission again
  - range: H again, the whole line, the decisions made while she held none
    too.
  - last: given.
  - door: the give accepted.
  - facts: her row holds the permission with a new stamp.
  - live: a new read is a holder's; an old one that left does not come back
    by itself.

beneath an end x the holder above her is given a permission again
  - range, last: no change: she is not H until she is given one again by an
    act of its own (chain.clj: her row is older than the row above).
  - door: her acts still refused.
  - facts: unchanged.
  - live: nothing.

stands, :see x she sends an act
  - range: a refused decision (:permission-does-not-allow-acting), which she
    reads whole as H and as its own who.
  - last: raised by one.
  - door: refused with that word.
  - facts: unchanged.
  - live: told once.

none, in an :everyone layer x she sends a thing that is refused for her standing
  - range: Vo: she reads it whole as its own who; every other visitor gets
    its stamp withheld; holders read it whole.
  - last: raised by one.
  - door: refused (:permission-does-not-stand and the like).
  - facts: unchanged.
  - live: told once to each asker by the same rule.

none, in a :members layer x she sends a thing that is refused for her standing
  - range: she is X: left out. She cannot read back what she sent (Q1).
    Holders read it whole.
  - last: not given to her.
  - door: refused, with its stamp: the door tells her the stamp that the
    read will not.
  - facts: unchanged.
  - live: told to holders.
```

### E5. Who may see a made layer L

States: **`:members`** (or no fact, for any layer but the base);
**`:everyone`** (or no fact, for the base).

```
:members x the first holder writes :everyone (taken)
  - range: every asker who was X becomes V, Vo or N: she now gets the last
    stamp and every stamp, withheld but for her own, or whole if the acts
    setting already reads :everyone. So an acts setting that was written to
    :everyone while the layer was closed takes effect at this write.
  - last: now given to everyone.
  - door: accepted.
  - facts: the layer's facts are readable by everyone.
  - live: reads of facts of former X askers can now be opened; O5 by its rule.

:everyone x the first holder writes :members (taken)
  - range: V, Vo and N become X: left out, their own things too (Q1). H as
    before. A read in flight: R13.
  - last: no longer given to them.
  - door: accepted.
  - facts: left out for them.
  - live: a visitor's live read leaves the layer and says so.
```

## Open questions

Not requirements. Each is something that the spec and the code do not settle;
the first three are Sid's.

- **Q1. Her own thing, in a layer that she may not see.** SPEC's table gives
  a thing whole to "the `:who` that the thing names". Its last line under the
  table says that an asker who may not see the layer is told that the layer
  is left out, "and nothing of its stamps". This page took the last line as
  the rule (the table of O3). Then the commonest refused author, a stranger
  whose thing was refused for want of a permission in a layer that reads
  `:members`, cannot read back what she sent, and neither can a holder whose
  permission ended read her own taken acts. Sid's third use of 12 October is
  "to give an author back what was refused". The other reading gives her own
  things whole wherever they are, and then tells her their stamps, which the
  door's answer by act-id already tells her.
- **Q2. Things sent for a layer on the other gate's depot.** Code: such a
  thing is no stray when its act-id names the depot that it arrived on; it
  is refused and stamped in the other gate's record, in a count of its own
  under the same layer id (`gate/decision.clj`, `standing`; the kept fixture
  holds a last stamp of 1 for `:base` in the streaming gate beside 179 in the
  microbatch gate, and of 1 for `:alice` in the microbatch gate beside 117
  in the streaming gate:
  `test-store/softland/store/run_again_fixture/tables.edn`). So one layer id
  can have two counts of stamps,
  and a stamp can be in both. The door's read by act-id finds either. This
  page requires only that the read of a layer gives the line of the layer's
  own gate and never mixes the two. Whether the other count is readable at
  all, and by whom (the layer's holders are in the other record), is not
  said anywhere.
- **Q3. Refusals in a layer that is never made.** By the rule as written
  nobody reads them, their own who neither. Whether that is wanted for
  Sid's second use ("to see what a sender in bad faith does") is not said.
- **Q4. Rama's own bounds.** Not determined from the code: the greatest
  size of one record that a depot takes, of one value that a PState holds,
  and of one answer of a query or a select. The module sets none, and the
  store's page says only that "a thing of any size that Rama's client
  writes reaches the gate". R15 needs these numbers, and whether a thing
  over the read's bound can be got back at all depends on them.
- **Q5. The form of a thing that is not plain data.** The read path runs in
  the runtime's process, so it could hand an object as Rama gave it; the
  runtime then puts it on a wire. Whether "whole, as it arrived" is the
  object, or a text of it, and what is handed for a part that the one text
  writes as `#other`, `#too-deep` or `#unreadable`
  (`door/text.clj`), is not said. R9 holds either way.
- **Q6. `:everyone` and an asker who is not signed in.** By SPEC's sentence
  she reads every act whole, with its `:subjects`, the people that it is
  about. Stated in the table of O3 as written; flagged because it is the
  widest thing this piece can show.
- **Q7. Whether a topology is given the position of the record that it
  decides.** SPEC says it is not known. `gate/stamp.clj` holds a reading,
  not a run: "Rama gives a stream event no offset of its record (a reading
  of the names in Rama's jar)". Nothing is said there of a microbatch.
- **Q8. A read backwards, and an end stamp.** SPEC says "from one stamp to
  another" and also "where it starts and how many it takes at most". This
  page took a start and a count, ascending. Whether the trail wants the
  newest first is piece 5's and not asked here.
- **Q9. What is said of a decision that is withheld for its size and not for
  rights** (R15): this page requires that the two are told apart and that
  the stamp, act-id and answer are still given to an asker who may read it.
  The words are a session's to choose and Sid's to change.

## Where SPEC.md and the code differ

None of these overturns the spec. Each is a place where its words are looser
than the code.

- **"A thing that the declaration refused can name no usable `:who`"**
  (SPEC, "Who may read what"). Code: it can name a usable one or none. A
  thing refused `:no-door-time`, `:value-too-large`, `:act-too-large` or
  `:too-many-facts` has an ordinary `:who`
  (`door/declaration.clj`, `shapes`, `rules`, `with-texts`). The spec's
  table has it right: "when it names a usable one".
- **"The bound on an act holds only for an act that passed the declaration
  (`max-act-bytes`)"** (SPEC 4). Code: `max-act-bytes` bounds the bytes of
  text that an accept writes into the rows of its facts, counted from each
  value's text and the who and permission of each fact
  (`door/declaration.clj`, lines 92–111). It does not count `:based-on`
  (1,024 entries at most), `:subjects` (256), `:session` or `:because-of`.
  So no one number of the build bounds the thing whole, for a taken act
  either; the bound follows from several limits together and Phase 1 works
  it out.
- **"A read that is asked once reads her permission with the rows, as a read
  of facts does"** (SPEC 7). Code: a read of facts reads her own permission
  row in the same call as the fact only because both are rows of one table
  under one routing id (`read_path/sources.clj`, `before-standing?`,
  `calls-for-once`); the rows above hers come a round later; and under
  `:everyone` her permission is not read at all (`reading`, lines 482–486).
  R4 and R12 say what this read needs in its place.
- **"Its answer as the door's read by act-id gives it today (the verdict,
  the reason of a refusal, the time, the digest)"** (SPEC, "The read").
  Code: that form is the query's, made by `record/rows.clj`, `answer-of`
  (lines 199–210), with the stamp in it. The door's own `answer-of` gives a
  maker `:answer`, not `:verdict`, and no digest
  (`door/words.clj`, `answer`). And the docstring of
  `module/door_reads.clj` (lines 12–14) lists the query's answer without
  `:time`, which `answer-of` gives.
- **"A sixth key … reading as `:members` while no fact says it
  (`first_act.clj`, `starts-with`)"**. Code: `starts-with` gives the base
  `:everyone` for who may see it. The spec's `:members` for the new key is
  for every layer, the base too; S1 says what follows.
