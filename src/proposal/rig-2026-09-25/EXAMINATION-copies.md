# Examination: the copies, under the project's caching rule

Cutter: Claude Opus 5.5, effort max. A new session, adversarial toward the
copies, as CLAUDE.md "Systemic repair and caching" requires. Written 26
September 2026, 03:20 to 04:00 IST, on branch `rig-exam-copies`. It
examines; it builds nothing. One probe: a JVM timing of the two wrap
schemes, `runs/ExamCopiesWrapCost.java` with its output
`runs/exam-copies-wrap-cost.txt`. No cluster was run.

Tags on load-bearing claims: *checked* (read in the named source, or run),
*derived* (reasoned from checked text), *assumed* (neither). Nothing here is
a ruling; the rulings are Sid's (PROGRESS.md "Now" on main).

Read for this (*checked* as text): PROGRESS.md "Now" whole (main,
`src/proposal/frame-2026-09-15/`); CONCLUSION.md R1 to R8 and "The seed"
(main, `src/proposal/store-next-2026-09-25/`); the formal model's README,
"Where the toy is kinder than the rule" (main); SPEC.md phase 2 and README.md
"The yardstick for the numbers" and its promise table (rig branch); RIG.md's
overnight head; PLAN-tools-and-grammars.md sections 1, 3 and 12 (in
`Softland-rig-plan-tools` at `c161f238`); PLAN-locks-and-forgetting.md
"$$persons", the shapes, L1 to L9 and step 11 (rig branch at `5aba1253`);
`locks.clj` in `Softland-rig-build-locks` at `b2c435d6` (the wrap and the
fan-out); the rama skill (`SKILL.md`, `core-concepts.md`, `stream.md`).
Not opened: `env.clj`, anything under `src/app`, `gate_test.clj`.

## 0. On one screen

- **Where the copies came from.** Both come from one rig invariant meeting
  one ruling. The invariant is phase 1's: the stream gate decides in one
  event on one task, with no partitioner and every input local. It is a rig
  choice drawn from what Rama gives, not a ruling. The ruling is placement
  by layer (ruling 2), which spreads one person's values, and one key's
  uses, over every task. Each copy then has a third ingredient. For
  grammars it is a reading: that "the key's grammar" must reach every layer
  at write. That is what the formal model's compiled grammar gives for free
  (the toy's kindness); no ruling says it. For person locks it is an
  assumption nobody made on purpose: that a person's lock is symmetric, so
  wrapping needs the secret. Sid's phase 2 words make the *value* lock
  symmetric; the person lock's form is the plan's.
- **Grammar: no copy needed.** Build the plan's pick A (a grammar lives in
  the layer it governs, read locally). A layer takes another layer's grammar
  through road F, an act in the layer that restates the grammar and stands
  on the source. F is not a copy in the rule's sense: it claims no
  currency, it is the promotion-shaped move the rulings already have, and
  its drift from the source is found through its stood-on. Store-wide
  conformance can be a read-time check against the base's grammar, with no
  copy at all. Road C, the base's current grammar on every task, does not
  survive: it copies mutable state, and to be honest every decision must
  record the version it used, which is a record change that F avoids.
- **Person locks: the copy survives, as originals replicated.** The secret
  is needed wherever a value about the person is opened. In a one-owner
  layer the owner's lock is needed at every decision, to unwrap the leased
  lock, and at every open. Values are on every task. A public-key wrap
  frees only the wrap under non-owner subjects, never the opening. It costs
  about 120 times the CPU per wrap (1.24 µs against 147.7 µs, measured) and
  changes the wrap record. One home with a hop per open concentrates a
  person's thousand agents on one task. What the `|all` copy multiplies is
  the storage residue of destroyed locks. That joins Sid's pending depot
  question.
- **Tonight.** Grammar: A as planned, no C, no F; leave F possible through
  the act's based-on (R4), with no new part. Locks: `$$persons` as built,
  plus (if still cheap) a scheme tag on the wrap record, marked
  **first-record**, and a check that a person forget's answer finds every
  task's entry destroyed.
- **To Sid.** First, whether a key's grammar is per layer with adoption, or
  store-wide enforced at every gate. Second, the person lock's scheme.
  Third, whether forget reaches lock residue, which is the depot question
  restated. Fourth, a note for model round three. Fifth, marks on a key a
  layer has no grammar for. See section 6.

## 1. The rule, and how I ran it

The rule, verbatim: "Never patch around a structural problem. If we reach
for caching, stop that implementation and use a new session for an
adversarial examination of why we would not need caching. First trace where
the proposal originated: what the current architecture exposes, and which
behavior or assumptions made caching look like the solution. Protect the
architecture we want while examining the one we have. Establish whether the
underlying design or computation should change before deciding what to do
about caching."

The examination turned on telling three kinds of "copy" apart. They behave
differently, and the rule targets only the first (*derived*):

1. **Derived mutable state kept near a reader.** An example is "the
   current grammar of `:mention`" on every task. It claims to be current,
   and it cannot keep that claim across tasks, because a cross-task read is
   a read at a moment (SPEC "What Rama showed"; PROGRESS.md 162-165). This
   is a cache.
2. **Originals replicated.** The same datum sits on several tasks, for
   example a person's lock. It is written in the original's own event tree,
   the set of replicas is known, and a forget reaches all of them. Each
   replica *is* the datum, not a view of it.
3. **An immutable fact, replicated or restated.** An example is a grammar
   version, or a first fact. Its content cannot go stale; it can only be
   absent, or superseded by a newer fact. Its restatement as a new fact
   that stands on the source (road F) claims no currency at all. Its meaning
   is "this layer's rule, taken from that fact at that stamp."

## 2. Where the proposals originated

### 2.1 The pressure both copies share

| Element | What it says | Status | Source |
|---|---|---|---|
| One event, one task, no partitioner, every input local | The stream gate reads everything its decision needs on the layer's home task, in one event | **Rig choice** (phase 1), drawn from Rama | PLAN-tools 3.1 citing SP:391-393 and phase 0 finding 1 (*checked* as quoted there) |
| Rama's atomicity | A stream event is atomic on one task. A partitioner is a commit boundary. Readers outside the owning topology see committed state. Cross-task reads are reads at a moment | **Platform** | SPEC "What Rama showed"; rama skill `stream.md:43`, `core-concepts.md:7` (*checked*) |
| Placement | One-owner layers by layer, shared layers by entity | **Ruling 2** | PROGRESS.md "The nine" (*checked*) |
| Scale | A person running 1,000 agents at full speed spans about 20 tasks; the promise "many people and agents work at once, each in their own working copy, without blocking each other", checked as each session on its own home task | **Yardstick** (arithmetic, not a measurement) | README.md 110, 142 (*checked*) |
| Order between stores | "A check one gate makes against the other store is a read, stamped in the checking store, never atomic with the other's writes, and the store promises nothing that needs such an order" | **Ruling** (Sid's rule, a sharpening) | PROGRESS.md 162-165 (*checked*) |

The trace, *derived*. The rulings do not require every input to be local.
Sid's order rule permits a decision to use something from elsewhere, as a
read stamped in the checking store. What that costs is a record that says
what was read. "Every input local" is the rig's stronger choice. It keeps
stamped reads out of the decision's record (the plan's T-FR3: the decision
carries no grammar version) and keeps the network off the decision's
critical path. It is a good choice, and it is the pressure: once every
input must be local, anything the decision needs from elsewhere becomes
"copy it here." Placement by layer ensures that almost everything
store-wide is "elsewhere" for almost every layer.

### 2.2 The grammar: the reading that made a copy look needed

- "Keys are ids; word and grammar are facts about them" (PROGRESS.md 65,
  ruling). This makes the **key** store-wide. It does not say which layer a
  grammar fact is in, or which layers it governs.
- Reading one, the plan's pick A: a grammar governs the layer it is in. It
  is *derived* by analogy from "a permission lives in the layer it governs;
  a gate checks only permissions in layers it orders" (PROGRESS.md 159-161),
  and from promotion's "the target's grammar" (156). It is a reading, not a
  ruling, and the plan says so.
- Reading two: one grammar per key, reaching every layer. I found it in no
  ruling. It is what the formal model has. There the grammar is one
  compiled branch (`fact-subjects`, model.clj:241-248, as the plan cites
  it), available on every partition at once. The model's README lists that
  kind of reach as where the toy is kinder: "The stream gate reads other
  stream partitions at once" (README 104, *checked*). **An assumption the
  toy made for free, and nobody made on purpose for the store.**
- The copy (road C) appears where reading two's reach is wanted and the
  rig's invariant is kept. The plan's own trigger is narrower. Session
  layers "made in quantity" start with no grammar under A, so a `:mention`
  of Bob in one "is not about Bob."

### 2.3 Person locks: the assumption that made a copy look needed

- L1's reason (*checked*, LP L1): "every write about a person and every open
  needs the person's lock where the value is, and a lock on one task would
  put a hop and a second atomic group inside every decision."
- The assumption nobody made on purpose: the person lock is symmetric.
  SPEC.md 74, Sid's words: "a small symmetric lock per value at write,
  wrapped under person locks as ruled." "Symmetric" is said of the **value**
  lock. The person lock's form, "32 random bytes made by the gate"
  (LP:298), is the plan's. With a symmetric person lock, wrapping needs the
  secret, so every task that wraps must hold it.
- `$$persons` was argued as placement. It is pstate-schema's case for
  `|all`: small, rarely written, read everywhere (LP "$$persons"). The
  caching note at the end of that section examined an in-memory cache
  *over* `$$persons`, not `$$persons` itself. The tools plan (3.3 C) says it
  had not checked whether `$$persons` went through the rule. As far as I
  found, this is its first examination under it.

## 3. The grammar: should the design or the computation change?

### 3.1 What a gate must hold, part by part

These are needed at write, on the layer's home (*derived* from PLAN-tools
3.1):

- **Subjects.** They decide the wrap and the act's subject slot.
- **The opaque flag and the index hints.** They decide the index writes,
  the pattern parse and rebuilds.

The shape check is needed at write to refuse a value. It could instead
become a mark at read.

Then a narrowing the plan does not draw, *checked* against the build. In
a one-owner layer, an unmarked value is wrapped under the owner alone:
`wrap-of`, `locks.clj:253-268` in `Softland-rig-build-locks`, "unmarked, a
person owner: the owner alone required." The rulings agree: "Alice's
mention of Bob in her own layer dies with Alice and survives Bob"
(PROGRESS.md 140-142). So a session layer with no `:mention` grammar
changes **no unmarked value's forget**. Bob's forget would not reach that
value with a grammar either.

What a missing grammar does change (*derived*):

- a `:die-with-any` mark finds no subject beyond the owner to die with;
- finding by subject (the act's union);
- the shape check and index hints in that layer.

Promotion is already covered. "The copy is about whoever the target's
grammar and tool name," and under A the target's grammar is on the target's
own home. So the reach problem is real but narrower than "every session
layer is ungoverned."

### 3.2 Where a key's grammar lives, by the rulings

Three rulings bear on it:

- The key is store-wide (65).
- A layer governs its own facts: the permission pattern (159-161), and
  ruling 9's visibility, under which a person's session layers are private
  to them.
- The base is open to every actor (ruling 9), so any door may read the
  base's grammar.

Put together (*derived*): a gate can enforce honestly only what is in the
layer it orders. Anything else it could know is a read at a moment, under
the order rule. So "the key's grammar" as enforced at a gate is the grammar
in force in that layer. A store-wide grammar can still exist: the base's,
readable by all and canonical by convention. It gains authority in a layer
only through an act in that layer. That act is road F.

This is my position, not a ruling. It is Sid's question 1 (section 6).

### 3.3 Standing on the grammar the offer was checked against

An offer can stand on a grammar fact in another layer. Under R4, its
based-on is fact ids with their stamps, and that is the honest record of
which grammar it relied on. The gate must then hold that version's
**content**, on the layer's home, at decision time. There are three ways
it could get there (*derived*):

1. **Carried by the offer.** This is road D. It is a new envelope part,
   refused by "Tools add facts, never envelope parts." The gate also cannot
   trust it without reading the source. That changes only if the id
   commits to the content, which is road E: the key's id derived from its
   grammar. E fixes a grammar for the key's life, which pulls against
   "grammars are facts with versions" (LEDGER.md 43, Sid, as the plan cites
   it). It is a question for round three, not for tonight.
2. **Replicated on every task.** This is road C, or the immutable-version
   index of 3.8.
3. **Restated in the layer.** This is road F.

F puts the stood-on where it belongs. It sits on the one act that adopts,
once per layer and version, not on every value's act. Values in the layer
are then checked under the layer's own grammar, derivable from the layer's
log by stamp. T-FR3 stands. The gate holds only its own layer's rows.

### 3.4 Whether the check belongs where the value is opened

At write, the gate opens the value on the layer's home with the leased
lock. Subjects, opacity and hints must be applied there, because they
decide what is written. So yes, for those.

Store-wide conformance is different: does this layer's `:mention` value
fit the base's current `:mention`? It needs no write-time reach. A reader
that reads the layer's values and the base's grammar reads two layers at a
moment. R3 and R5 record that read. The reader can mark values that do not
fit the base's current grammar (*derived*). That gives store-wide
visibility with no copy. It is the shape of the staleness index that
CONCLUSION.md 129 calls "the likely first honest protocol step."

### 3.5 What the model already assumes

The model assumes a compiled, store-wide grammar reaching every partition
at once (2.2). Round three designs the grammar fact's shape "first"
(CONCLUSION.md, "The seed"). It will not see this question unless it models
grammars as facts in layers, with adoption as an act, or states that it
assumes a store-wide constant (*derived*). That is a note for its starter;
this session writes nothing there.

### 3.6 Road G: a person's layers on one task

G fails the yardstick. A person running 1,000 agents spans about 20 tasks
(README 142). The frame promises that "many people and agents work at once,
each in their own working copy, without blocking each other", which the
rig checks as each session on its own home task (README 110). G would put
all of them on one thread.

It also partitions offers by the layer's owner, which the offer does not
carry. The name's tag or a lookup would have to supply it, which changes
the envelope or placement: record-touching. And "inherits" would be a new
rule. G is rejected on the yardstick and the platform (*derived*).

### 3.7 The base's grammar as settled history; a first fact with a constant id

- **Settled history.** After R8's re-class the base is a shared layer. A
  read of it at its settled frontier (R3) is final. Facts are never
  rewritten in meaning, and a later grammar is a new fact. So a replica of
  the base's grammar *as of a settled frontier* is the one kind of copy that
  cannot lie. The model already leans on "settled history" for the re-class
  (its README, "After a re-class"). Delivering it to every task is still
  machinery, though. A PState is written by one topology only (*checked*,
  `core-concepts.md:7`), and the base's grammar facts change gates at the
  re-class, so it needs two PStates or a forwarding depot. And a decision
  must still say which frontier it applied. F carries the same meaning as a
  fact, with none of that machinery.
- **A first fact with a constant id.** R8 keeps first facts as facts, with
  constant ids (item 59's default). It withdrew edition constants because
  ruling 9 wants "a store can differ" (*checked*, CONCLUSION.md R8). A
  constant id lets an adoption act, or a test, name the seed's `:mention`
  grammar without a read. It does not put the grammar's **content** on any
  gate; only an edition constant would, and that is withdrawn. So the
  constant id serves F; it does not replace it.

### 3.8 Road C, examined against itself

- **What it copies:** "the current grammar of `k` in the base," which is
  kind 1 in section 1, a cache.
- **Its claim of currency cannot be kept.** A decision on another task
  uses whatever copy has arrived. To be honest, the decision must record
  the version it used; that is the plan's own cost (3). At that point C is
  a stood-on on every decision, with machinery attached. It is a record
  change, and T-FR3 falls.
- **One writer.** The base's grammar facts are admitted by the stream gate
  tonight and by the micro gate after the re-class. A PState has one
  writer (*checked*), so C needs a forwarding depot or two PStates.
- **Replay.** The `|all` fan-out after the base's commit must be redone on
  the recorded path, as L9 does for persons.
- **What C buys that F does not.** When the base's grammar changes, C
  reaches every task promptly: one act, N writes. F reaches each adopting
  layer only when an act in it re-adopts, which is one act per layer, found
  by the walk. This is C's one real advantage, and it is why the question
  stays open.

The verdict: C does not survive. If Sid rules that a store-wide grammar
must reach every open layer promptly, the survivable form is not C as
written. It is a **derived index of immutable grammar versions**, keyed by
the version's fact id, or by a digest of its content. Each gate writes it
on its own write path into its own PState on every task. It is rebuilt
from the log, purgeable by fact id, and never read as "current." Grammar
facts name no person, so a forget reaches it only through an excision,
purged by fact id on every task. Each layer names the version it applies
through an adopting act's based-on, which is F by reference. Nothing then
claims currency: a version can only be absent (refused as data, resent
later) or superseded (found by the walk). That is kind 3 in section 1. It
is not needed tonight, and not before Sid answers question 1.

### 3.9 What F costs at the yardstick

A rough estimate (*assumed* sizes). An adopting act restates each adopted
key's grammar, a few hundred bytes. A session adopting 20 keys at about
300 bytes carries about 6 KB in its making act. R7 already writes
permissions into each layer when a session opens (CONCLUSION.md R7), so the
act exists. The cost grows with sessions, not with acts. If it measures too
heavy, F by reference over the index in 3.8 is the fallback.

### Verdict on the grammar

**No copy is needed.** The road instead:

- A tonight;
- F as the cross-layer road, when Sid wants a layer to take another's
  grammar;
- read-time conformance against the base, for store-wide visibility.

The plan marks C and F UNSETTLED. That resolves two ways. C does not
survive this examination. F is not a copy in the rule's sense: it is a
fact that claims no currency, standing on its source, like a promotion's
landing, and it needs no further examination under the rule.

## 4. Person locks: should the design or the computation change?

### 4.1 Where a person's secret is needed

All of these are *checked* in LP and the build:

- **At every decision in a one-owner layer.** The leased lock is kept
  "wrapped under the session owner's lock" (CONCLUSION.md R1, line 51). The
  gate therefore unwraps it with the owner's lock to open the sealed value,
  then re-wraps it under the value's subjects, the owner always required
  (`wrap-of`).
- **At the resend check,** which opens with the recorded lock (R1's riders).
- **At every open for a read,** through `open-value>`, which needs
  `$$persons` per wrap person (LP RD7). Also at the read-out for promotion
  and at index rebuilds.
- **In shared layers:** at open, some any-of subject's lock.

Every one of these runs on the value's task, because lock rows are
co-located with their values (ruling 7, PROGRESS.md 96). Values about Alice
are on every task her layers or mentions reach. Her own agents alone put
them on about 20 tasks (README 142). Shared layers, placed by entity, put
them on any task.

### 4.2 A public-key wrap: what it would change

- **Wrapping** needs only the subject's public key. A public key opens
  nothing, so its copies need no forget. A stale copy after Alice's forget
  wraps under a dead secret, so the value is born closed for her. That
  answers the open item "writes about someone already forgotten"
  (PROGRESS.md 180) by construction, where the symmetric road needs a rule
  for a destroyed lock (*derived*).
- **Opening** still needs the secret. So does every decision in a
  one-owner layer (the lease unwrap), and every open there (owner
  required). A public-key wrap therefore frees only the wraps under
  **non-owner** subjects: marked values and shared layers. The secret must
  still be wherever opens happen (*derived*).
- **Forgetting** is the same in kind under both schemes: destroying one
  secret closes every wrap under it, wherever it is. The only difference is
  how many places hold the secret, and 4.1 fixes that by where opens
  happen, not by the scheme.
- **Cost** (*checked*, `runs/exam-copies-wrap-cost.txt`, java 21, one
  thread, 10,000 iterations each):

  | Operation | Time |
  |---|---|
  | Symmetric wrap (AES-256-GCM of 32 bytes) | 1.24 µs |
  | Public-key wrap (X25519 ephemeral, agreement, GCM) | 147.7 µs |
  | Public-key open step (X25519 agreement) | 73.7 µs |

  Against the README's figure of about 5,000 small acts a second on a task
  in-process, one public-key wrap per act is about 0.74 s of every second
  (*derived*). An act could share one ephemeral key across its values, so
  the cost falls to one agreement per act and subject. It stays a
  large share of a task's budget.
- **The record changes** (**first-record**). Each wrap kept in the record
  (agent sessions, group layers and the base, under ruling 7) carries an
  ephemeral public key per act and subject. The person fact carries a
  public key. Lease rows wrap under the owner's public key.

### 4.3 Where the secret could be, against the yardstick

- **One home, a hop per open.** Every open of every value about a person
  goes to one task. A person with 1,000 agents each opening 100 values a
  second needs 100,000 unwraps a second on that task (*assumed* rate).
  Counting the hop's handling on the task, even symmetric unwraps put that
  past one task's budget. Public-key unwraps, at 73.7 µs each, need about
  7 s of work every second. Each decision would also gain the hop and the
  second atomic group that L1 names. This fails balance (the skill's
  goal 1) and the yardstick.
- **The owner's span.** A person's layers would sit on a chosen set of
  tasks, with her secret only there, and shared-layer opens would hop. The
  forget's reach falls from N tasks to about 20. The price is a placement
  change in the offer's routing, which is record-touching. Not worth it
  (*derived*).
- **Every task (`|all`), as built.** Each use is one local seek. Each person
  act costs N writes, and a person has two such acts in a life. The store
  holds about 60 bytes per person per task (LP "Disk usage"). At 10,000 to
  100,000 people that is 0.6 to 6 MB a task (*derived*).

### Verdict on person locks

**The copy survives, as originals replicated.**

What it is: the secret itself, not anything derived, on every task. It is
written in the person act's own event tree after the home's commit
boundary (L9; `locks.clj:1247-1255` in the locks build, *checked*). The ack
returns only when the whole tree completes ("`:ack` returns when the entire
tree completes", `stream.md:3`, *checked*). It is not a cache: no replica
claims to track another, and each one is the lock.

What a forget must reach:

- every task's entry. The forget's own act does this with an idempotent
  `termval`, re-run on the recorded path;
- each entry's Rama replicas;
- each replica's **RocksDB residue.** The old bytes survive in older files
  until compaction rewrites them (*assumed* from RocksDB's design; not
  probed tonight).

The copy multiplies that residue by N compared with one home. It matters
only if forget must reach the operator's copy. That is already Sid's to
rule for the depot (PROGRESS.md, "Still Sid's"), and the two questions are
one: a lock recovered from residue would open the depot's sealed values.

What can go stale: only the window between the forget's decision on
`:people`'s home and the fan-out's arrival at a task. An open on that task
in the window is a read at a moment, which the order rule allows, and
which cannot be unread (exposure). A wrap made there under the still-present
lock dies when the destroyed entry lands. After the answer, nothing is
stale ("once answered, no value ... opens anywhere", OP10 in LP).

## 5. What the rig should build tonight

**Grammar (phase 6).**

- Build A as the plan has it. Build no C and no F.
- Keep T-FR3, the decision carrying no grammar version, marked
  **first-record**. It holds under A and under F. It falls under B and C.
- Keep F possible at no cost. An adoption is an ordinary grammar fact in
  the layer, whose act stands on the source through based-on (R4). Nothing
  is new in the envelope, and the gate checks nothing against the source.
- The plan's text should record this examination's outcome on C and F.
  That is builder A's to write; this session writes only here.

**Person locks (phase 2).**

- Keep `$$persons` as built. Two small additions, both only if they are
  still cheap:
  1. **A scheme tag on the wrap record** (**first-record placeholder**). A
     later edition could then add a public-key wrap beside the symmetric
     chain without touching a kept record. It touches the record's form, so
     it needs the first-record mark. If phase 2 is past the point, it goes
     to edition one's list instead.
  2. **A check that the forget reaches every copy.** After a person
     forget's answer, every task's `$$persons` entry for that person has a
     nil lock and the same `:erased-at`. The check belongs where OP10's
     promise is tested, if the suite does not have it already. I did not
     open the tests.
- No public-key wrap tonight.

## 6. For Sid

1. **Where a key's grammar has authority** (the plan's Q1 and Q2, restated).
   There are two options:
   - per layer, with adoption as an act in the layer that stands on its
     source (A with F);
   - store-wide, enforced at every gate. The order rule allows that only as
     a recorded read (B) or a copy whose version each decision records (C).
     Either way the record changes and T-FR3 falls.

   My position is per layer with adoption. A gate can honestly enforce only
   what is in the layer it orders. The permission sharpening already has
   this pattern. And F is the binding dimension as you framed it: the
   pointer is fixed, and the change is found by the walk.

   C's one advantage is prompt reach when the base's grammar changes. If
   you want that, the form that survives is the immutable-version index in
   3.8, not C.
2. **The person lock's scheme.** Symmetric, replicated to every task: what
   tonight builds, and it survives this examination. Or public-key, which
   frees only the non-owner wraps, costs about 120 times the CPU per wrap,
   and changes the wrap record. Either way, the scheme tag keeps a later
   switch off the kept record.
3. **The depot question, widened.** "Whether forget must reach the
   operator's copy of a value in the depot" is really about lock residue.
   A destroyed lock's bytes left in RocksDB residue on N tasks, times the
   replication factor, would open the depot's sealed values. If forget
   must reach the operator, the store needs a way to make that residue
   unreadable, for example by rotating an encryption under which the lock
   store sits. That is a design, not a tweak, and it is the same design
   for one copy or N copies (*derived*).
4. **Round three.** The model's compiled grammar is the toy's kindness. Its
   seed design should model grammars as facts in layers, with adoption as
   an act, or say that it assumes a store-wide constant.
5. **Marks on a key the layer has no grammar for.** Under tonight's
   permissive default, a `:die-with-any` mark finds no subject beyond the
   owner, and the value dies only with the owner. Refuse such a mark, or
   allow it? This is the plan's Q4 seen from the mark's side.

## 7. What is checked, derived, assumed

- **Checked:**
  - the quoted rulings and their line numbers on main;
  - CONCLUSION.md R1, R7 and R8;
  - the model README's kinder-than-the-rule list;
  - SPEC 74 and README 110 and 142 on the rig branch;
  - the plans' sections named at the top;
  - `wrap-of` and the fan-out in the locks build at `b2c435d6`;
  - the rama skill's one-writer rule and the ack-on-tree rule;
  - the probe's three timings, on this machine, on one thread.
- **Derived:**
  - the three kinds of copy;
  - the narrowing in 3.1;
  - the reading in 3.2;
  - the verdicts;
  - the budget arithmetic in 4.2 and 4.3.
- **Assumed:**
  - the sizes in 3.9;
  - the per-agent open rate in 4.3;
  - RocksDB keeping old bytes until compaction (its design; not probed);
  - that the plans' line citations match their current text.
