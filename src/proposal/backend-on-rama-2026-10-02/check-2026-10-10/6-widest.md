# 6 — the widest view: kinds of assumption the six examples do not cover

Session 6 of 6, 2026-10-10. Written by Fable 5.1 at the effort the session ran at.

## What this is built from

I read nothing on disk: no file, no code, no record of what was said. Everything
below is derived from the brief alone (Sid's description of the store, the six
examples of today, and the passing session's note), plus what I know of Rama
and Electric from training. Every claim carries one of three marks:

- **[follows]** — it follows from what the brief says, by reasoning I show.
- **[supposition]** — my own guess about what a project like this would have
  done or not asked; it needs a check by one of the other five sessions or by Sid.
- **[Rama, unchecked]** — a fact about Rama from my training that I could not
  check against the local docs. The Rama session should confirm or kill each one.

The Rama facts I lean on, so they can be checked in one place:

- r1. A PState (table) is declared inside one topology and written only by
  that topology; a topology's input is a depot. Nothing writes a PState from
  outside the module. **[Rama, unchecked]**
- r2. A depot is an append-only, partitioned log. Its partitioning scheme is
  fixed at declaration. There is no per-record delete. Whether a depot can be
  trimmed by age or size I do not remember with confidence. **[Rama, unchecked]**
- r3. A stream topology processes records one at a time with at-least-once
  semantics and no atomicity across partitions. A microbatch topology
  processes a batch with exactly-once semantics and commits its PState writes
  atomically across partitions. **[Rama, unchecked]**
- r4. A reactive ("proxy") PState query keeps a replica of the queried path on
  the client side and pushes diffs to a callback. **[Rama, unchecked]**
- r5. A depot append can be acknowledged at different levels: durable in the
  depot, or processed by the stream topologies that consume it. **[Rama, unchecked]**
- r6. Only a Rama client (a JVM program using Rama's client library) connects
  to a cluster; the cluster asks no identity of it. This matches what the
  brief says a session found today in the local docs, so it is partly checked.
- r7. Recent Rama has cluster backups to external storage. **[Rama, unchecked]**
- r8. Tests commonly run against an in-process cluster with few tasks, often
  one partition. **[Rama, unchecked]**

Where a finding rests on one of these, I say which.

---

## Part 1 — the findings, in order of bite

The order weighs two things: how badly it bites, and how irreversibly. Anything
that fixes the shape of a record in the depot is irreversible at the first kept
record, because the log keeps what it was given. Anything that only shapes the
way out is reversible until the way out is built.

For each: what it is and where it would be written; whose it would be (Sid's, a
fact, or a session's); what is lost if it is wrong and when that lands (now,
first kept record, when reads are built, at scale); what would settle it.

### 1. Where the lock lives, and whether forgetting can work at all

**What it is.** Three things are decided or built: a lock seals one value; the
gate opens each value when it accepts it; to forget, the store destroys the
lock, which is "in another table". Put next to r1 and r2: a table is written
only by a topology, and a topology reads a depot. So for the gate (a topology)
to open a value, the lock must have reached it through a depot — either inside
the act's own record, or through a second depot that feeds the lock table.
Either way the lock has been appended to an append-only log. Destroying it in
the table leaves it in the log. The one thing the seal exists to do — make a
value unreadable — is defeated by the path the lock took to the gate.

This is a pick-two triangle nobody has laid out:
(a) the gate opens values when it accepts them; (b) a lock can be destroyed;
(c) everything the store keeps is a Rama table fed by a Rama log.
All three cannot hold. Drop (a): the gate stays blind to values, locks can live
somewhere mutable outside the log, forgetting works, but then the gate cannot
check anything about a value's content. Drop (b): forgetting is a "please
ignore" record, not an erasure. Drop (c): locks live outside Rama (a key store,
a file), the store has two homes with two durability stories, and "the runtime
is the only Rama client" gains a sibling.

A variant a session may have reached for: seal the locks themselves under a
master that lives outside Rama (envelope encryption). Then destroying one
value's lock is still impossible (it is in the log, wrapped); destroying the
master forgets everything under it. The grain of forgetting becomes the grain of
whatever lives outside the log — per store, per person, per layer. That grain
is the real decision, and "one lock seals one value" does not make it.

**Where it would be written.** The door's code: what goes into the depot record.
The lock table's declaration: which depot feeds it. The decisions page under
forgetting, if the lock's home is named at all. **[supposition]** that it is not
named.

**Whose.** "Seal before the depot", "one lock per value", "forget by destroying
the lock" are Sid's by the brief. The lock's home is a session's, or nobody's.

**What is lost, and when.** The promise of forgetting, at the first kept record:
the first act's lock is in the log from then on. Irreversible.

**Marks.** (a) and (b) **[follows]** from the brief; (c) rests on r1, r2.

**What would settle it.** Sid rules on one question: does the gate need to see
the value's content to accept or refuse? If no, locks can leave the log. Then a
fact check by the Rama session: can a depot forget a record (retention, trim),
and does a cluster backup (r7) copy depots. Then the grain of forgetting is put
to Sid as a spectrum, not a pick.

### 2. Against whom, or for what, is the seal

**What it is.** The brief gives the seal one use: forgetting. It gives it no
adversary. The runtime makes every lock; the gate opens every value; so the seal
hides nothing from the runtime or its operator. If forgetting is the whole
purpose, the seal is a forgetting mechanism and the secrecy-shaped costs (the
pushed row is sealed, every shown value costs a second read — example 3) are
paid for nothing: the table could hold the value open and the log sealed. If
secrecy from the operator was the purpose, the design does not provide it,
because the gate has the lock.

A second layer of the same thing: the words. In plain speech a "seal" is
tamper-evidence — it shows if it has been broken; that is a signature or a hash,
not encryption. A "lock" is secrecy. The brief writes "seals each value with a
lock (an encryption key)"; the parenthetical reads like a session's gloss on
Sid's word. If Sid's "seal" ever meant "you can tell if it was changed", the
whole crypto path is built on a misread word. **[supposition]** that the gloss
is a session's.

**Where it would be written.** The architecture files' account of the seal; the
decisions page; the vocabulary list if there is one.

**Whose.** "Seal" and "lock" are Sid's words by the brief; the mechanism they
were read as is a session's.

**What is lost, and when.** Now: example 3's question (must a value stay sealed
in the table or only in the log?) cannot be answered until someone says what
the seal is for. Every read-side design waits on it. If the seal is overbuilt,
the cost is read latency and a second table forever; if misbuilt, it is the
wrong mechanism.

**Marks.** The absence of an adversary **[follows]**: the brief lists forgetting
and nothing else. The word-drift is **[supposition]**.

**What would settle it.** Sid names the adversary or the goal in one line. The
shape of the seal follows from that line.

### 3. The fields of the act's record are frozen at the first kept record

**What it is.** Whatever the door writes into the depot is the shape of history.
The way out is undecided, but it will ask things of each record, and the record
either carries them or does not. Fields the way out will plausibly need and
that nobody has listed for Sid:

- an act id the maker holds, so a screen can tell "my act landed" from "this
  place changed" — with no optimistic update the screen has only the committed
  row to go on, and if the row does not carry which act put it there, the
  screen cannot show the committed result *of its own act*; it can only show
  that the place now holds something;
- who acted, as a pair when an agent acts for a person — the brief's "humans
  and agents together" has two parties with standing, and a single maker field
  loses one of them at the first record;
- when, and by whose clock (the maker's, the door's, Rama's);
- the layer's kind at the time (see 5);
- a refusal's reason, if refusals are kept as data (the rig's finding);
- the reference from a fact to its lock.

A map can gain fields later; the old records will lack them forever.

**Where it would be written.** The door's record shape; the gate's table row
shape (does a fact carry the act that made it?).

**Whose.** A session's, by default — nothing in the brief says Sid saw a field
list. **[supposition]**

**What is lost, and when.** The first kept record is thin forever. The two
specific losses: (i) "show the committed result" of one's own act is not
possible without a correlating id, which pushes a session straight toward a
client-side pending table — that is, toward the optimistic layer Sid does not
want by default; (ii) the human-and-agent pair is unrecorded in the log.

**Marks.** The need for a correlating id **[follows]** from "show the committed
result, no optimistic updates". The rest is **[supposition]** about what the
record carries.

**What would settle it.** Sid sees the record's field list and says what a
reader must be able to ask of a record. One session walks "a maker acts, the
screen shows the committed result" end to end and names the field it had to
have.

### 4. Who writes the first permission — the bypass that is the real model

**What it is.** A permission is a fact in a layer that lets its holder act
there. The first act into an empty layer — the base included — needs a
permission that does not exist. Something lets it through: a seed, a root
maker, a rule that a layer's first act creates it and grants its maker, or a
"the runtime may do anything" exception. Whichever it is, that thing is the
actual permission model; the facts in layers are its second storey.

If the exception is "the runtime may do anything": the runtime is the only
client (r6, and the brief), so every act arrives through the one program that
may do anything. The gate's check is then on a field the runtime fills in; the
permission facts constrain nothing the runtime does not choose to be
constrained by. If the rule is "first act creates and grants": two makers
claiming the same new layer race, and the loser's act is refused or, worse,
accepted into someone else's layer.

Two more unasked things sit beside it. A permission is a fact; a fact's value
can be forgotten by destroying its lock. An unopenable permission fact — does
the gate read it as refusal, as allow, as absent? And revocation: is it a new
fact at the same place, or a destroyed lock? Both exist in the design and leave
different traces.

**Where it would be written.** The gate's acceptance code; a seed step; a test
fixture that quietly grants.

**Whose.** A session's. **[supposition]**, but the bootstrap problem itself
**[follows]** from the definition of permission.

**What is lost, and when.** Now, since the way in is built: the stated model
("a permission is a fact in a layer") is not the operating model. Bites at the
first kept record of the base.

**What would settle it.** Sid says how a layer comes to be and who holds its
first permission. A session shows the actual path the current code takes for the
first act into an empty store.

### 5. "Together" and "committed" mean different things per layer kind

**What it is.** An act is "one or more facts that go in together". A layer is
streaming or microbatch. By r3, a microbatch topology commits a batch
atomically across partitions; a stream topology processes record by record,
at-least-once, with no cross-partition atomicity. So in a streaming layer an
act whose facts fall on two partitions can be half-visible to a reader; in a
microbatch layer it cannot. The same word promises two things.

"Committed" has the same split. In a streaming layer the depot has the act
before the gate decides; a refusal arrives after the append (r5). A client that
treats "appended" as "committed" shows a refused act as done. In a microbatch
layer "committed" is the batch commit, a moment later. The brief's "show the
committed result" needs the word defined per kind, and it is not.

A third consequence from r1: a PState belongs to one topology, so the streaming
gate and the microbatch gate write different tables. Two layer kinds means two
tables of "the one fact that is there now". A reader must know a layer's kind to
find its facts. So a layer registry exists or is implied, and the question
"can a layer change kind?" has an answer with a race in it: acts in flight
during the flip land in the wrong table, and both tables may hold a "now" for
one place.

**Where it would be written.** The two gate topologies; the decisions page under
streaming/microbatch; the architecture's use of "committed"; wherever a layer's
kind is recorded.

**Whose.** Two speeds may well be Sid's (it reads like a spectrum he wanted).
The one-to-one mapping of his two speeds onto Rama's two topology kinds, and
the atomicity consequence, are a session's or nobody's. **[supposition]**

**What is lost, and when.** At reads: a screen shows half an act, or a refused
act as done. Hidden until then if tests run on a one-partition in-process
cluster (r8), where a half-act cannot occur. **[supposition]** that tests do.

**What would settle it.** Sid says what a reader may see in the middle of an
act, and whether "committed" means "the gate kept it" in both kinds. The Rama
session checks r3 and whether the test cluster has more than one partition.

### 6. Entity identity across layers, and the partition key

**What it is.** A place is (layer, entity, key). So one entity id names the
same entity in the base, the team's layer and mine: the table's key presupposes
an overlay model, where reading an entity composes its layers. The way out is
"nothing decided", but this is a read decision already made by the way in. If
the intended model were forking — my copy of a page is a new entity that
remembers its parent — the shared id is the wrong primitive.

Underneath: who mints entity ids? If the maker (a screen or an agent) mints
them, a resend after a dropped connection is idempotent; if the gate mints them,
a resend makes a duplicate entity, and a maker cannot refer to a new entity
inside the act that creates it.

And the partition key (r2: fixed at declaration). By layer: the base — the
thing everyone shares — is one partition, the system's narrowest pipe. By
entity: the permission check (a fact in the layer) needs a hop to the layer's
partition before the write on the entity's partition; in a streaming topology
with at-least-once, a retry across that hop is a double write.

**Where it would be written.** The depot declaration; the PState key path; the
id-minting code.

**Whose.** A session's unless Sid drew the overlay. **[supposition]**

**What is lost, and when.** Overlay versus fork: at reads, and it reshapes the
product. Partition key: at scale, and the depot's partitioning is irreversible.
Id minting: at the first dropped connection.

**What would settle it.** Sid draws what a reader sees when two layers hold the
same place. A session names the partition key and who mints ids.

### 7. One fact per place is last-writer-wins — the concurrency model nobody chose

**What it is.** The table holds one fact per place; a new act replaces it. A
person and an agent writing the same place "together": the later in depot
order wins, the earlier is gone from "now" with no signal to anyone. How bad
depends on what a value is. If a value is a document body, every edit replaces
the body and two concurrent edits lose a whole body. If a value is a word, the
loss is a word. The brief never says what a value is — its type, its grain, its
size — and "one lock seals one value" is stable only while "value" means one
thing.

**Where it would be written.** The table write (a set, not a merge); the
definition of value, if there is one.

**Whose.** "The one fact that is there now" is Sid's by the brief. The grain of
a value and the absence of a conflict signal are a session's or nobody's.
**[supposition]**

**What is lost, and when.** Silent loss in the central use — people and agents
changing one thing at once. Lands at the first real concurrent use. Recoverable
from the log only if the lock still lives (ties to 1).

**What would settle it.** Sid says what a value is — the unit of change — and
what a maker should see when it has just overwritten someone's fresh fact.

### 8. Forgetting is partial three ways: shape, backups, read-records

**What it is.** Destroying the lock forgets the value. The place remains: the
entity id, the key name, the maker, the time, the fact that an act happened. A
key named "diagnosis" whose value is forgotten has not been forgotten. If
cluster backups exist (r7), they copy the lock table; the live destroy does not
reach them. If reads are kept — "Always" was a session's word, per example 4 —
who read a value stays after the value is gone, and the read-record is the
most sensitive data in the store.

**Where it would be written.** The forgetting decision; the backup setup; the
read-keeping rule.

**Whose.** Forgetting is Sid's; its partiality is nobody's.

**What is lost, and when.** A promise ("Softland can forget") that is false in
three ways, at the first forget. Backups make it irreversible.

**Marks.** The shape remaining **[follows]** from "seals each value" (only
values are sealed). Backups rest on r7. Read-records **[follows]** from
example 4.

**What would settle it.** Sid says what "forget" must cover: the value, the
place, the act, who saw it. A fact check on backups.

### 9. "No caching" is undefined, and the chosen tools hold copies by construction

**What it is.** A Rama proxy query keeps a client-side replica and pushes diffs
(r4). Electric holds each connected client's reactive state on the server.
Missionary flows hold a latest value. Each is a held copy. Without a
definition, "no caching" either bans the tools or bans nothing: a session can
wave a real second store through as "just a proxy", or refuse a proxy as "a
cache". Same shape as "a program" in example 1: a word doing load-bearing work
with no list of what it covers.

**Where it would be written.** The architecture's rule; the read path when
built.

**Whose.** The rule is Sid's; the definition is nobody's. **[follows]** from
the brief that no definition is given; the tools' behaviour rests on r4 and my
knowledge of Electric.

**What is lost, and when.** At reads: either the wrong thing is built in the
rule's name, or the rule is argued past.

**What would settle it.** Sid says what counts: any held copy; a copy consulted
*instead of* asking Rama; or a copy that can go stale without a push. One line.

### 10. The gap on the screen

**What it is.** In a microbatch layer the committed result arrives a moment
after the act. No optimistic update, no cache: for that interval the screen
shows nothing of the maker's own act. Something fills the gap — nothing, a
spinner, a pending mark. A pending mark is client-held state about a write; is
that the thing the no-optimism rule bans, or is it the "by default" exception?
Nobody has shown Sid the gap.

**Where it would be written.** The client's write path, unbuilt.

**Whose.** Nobody's. The gap **[follows]** from microbatch plus no-optimism.

**What is lost, and when.** At reads: either a dead-feeling medium or a
smuggled optimistic layer.

**What would settle it.** Sid sees the gap and says what the screen shows during
it, for each layer kind.

### 11. An act lives in one layer, so moving between layers is two acts

**What it is.** "Publish my draft to the team" is a write to the team's layer
and a forget in mine. Not atomic. A reader can see both or neither. The most
common collaboration motion — share, promote — has no atomic form.

**Where it would be written.** The act's shape.

**Whose.** "Into one layer" is Sid's by the brief; the consequence is nobody's.
**[follows]**

**What is lost, and when.** At reads and real use.

**What would settle it.** Sid says whether promotion is something the store does
or something a maker does in two steps.

### 12. "The runtime", singular

**What it is.** "The runtime is the only Rama client." Read literally, one
process holds every client's proxies and every Electric session. It is the
throughput ceiling and the single failure point. On restart every subscription
drops; with no cache every screen blanks and refills. Is "the runtime" one
process or a kind of process?

**Where it would be written.** The architecture file.

**Whose.** A session's phrasing, maybe Sid's intent. **[supposition]**

**What is lost, and when.** At scale; and at reads, the reconnect behaviour.

**What would settle it.** Sid says one or many. A session checks Electric's
reconnect semantics against "no cache".

### 13. "Write directly to Rama" was reinterpreted without saying so

**What it is.** Sid's line "write directly to rama and then stream from there"
cannot be literal for a browser (example 1: only our own programs connect). The
runtime mediates both ways. The intent — no middle store, no optimistic layer
— can be met; the literal cannot. Did any session say "your line cannot be
literal, here is what we take it to mean" and get a nod? The brief's example 1
suggests the opposite: the architecture carried "the runtime is the only
client" for a week without anyone noticing it reinterpreted the line.

**Where it would be written.** Wherever the line is quoted as a decision.

**Whose.** Sid's line; a session's silent reading.

**What is lost, and when.** Now: the record says one thing, the build does
another, and the next session picks whichever suits.

**Marks.** **[follows]** from example 1 and the line.

**What would settle it.** Sid confirms the reading in his words.

### 14. Who may forget, and forgetting at scale

**What it is.** Forgetting is irreversible and, in the brief, unpermissioned.
A permission to forget in the base is the power to erase the shared thing. A
team disbanding is one destroyed lock per value with no bulk form (one lock per
value makes this so). **[follows]** for bulk; **[supposition]** that
permission-to-forget is undecided.

**When.** First forget; at scale.

**What would settle it.** Sid.

### 15. Agents as makers: binary permission, no rate

**What it is.** "A permission lets its holder act there" is binary. An agent
with permission in the base can flood the base with acts. **[follows]** for
binary; **[supposition]** that rate was never raised.

**When.** The first agent in the base.

### 16. Offline shows nothing

**What it is.** No cache, streamed from Rama, no optimistic default: a person
offline sees none of their own changes and makes none. For a medium meant for
understanding and changing things together, is that intended? **[follows]**
from the rules; **[supposition]** that it was never put to Sid.

**When.** Product shape; now, if offline matters.

### 17. Keys: free or declared

Who defines keys? If makers — agents especially — invent keys freely, the base
fills with variants of one meaning. If keys are declared, where does a
declaration live — as facts? **[supposition]** that it was never asked.
**When.** First agent writes.

### 18. The test cluster hides what the real one shows

A one-partition in-process cluster (r8) cannot show a half-act, a hop, a
cross-partition retry. "Builds and passes" (example 1) is evidence about the
test cluster. **[supposition]** that tests run so.

### 19. At-least-once is survivable only because every write is an overwrite

If every write sets a value, a retry sets it again — idempotent. The moment
anyone adds a counter, a set-add, or a kept read-record, streaming layers
double-apply under failure. This invariant is load-bearing and, I suppose,
unwritten. Rests on r3. **[supposition]**

### 20. Document bodies as values

If a body is a value, each edit appends a whole body to the log, with a new
lock. The log grows by body size per keystroke. **[supposition]** that this is
the grain; the cost **[follows]** if it is.

### 21. Way in before way out — whose order

The brief states the sequence as fact. Example 3 is the first collision that
the sequence produced. If a session chose the order, every such collision is
its cost. **[supposition]** about whose.

---

## Part 2 — the shapes, beyond the six

The six examples are: an undefined term carrying load ("a program"); a
session-to-session replacement never put to Sid; two decisions fine alone that
collide later; a session's word recorded as Sid's; a pick between two ends
where he wants the spectrum; a checkable fact handed over as a decision.

Shapes the six do not name, each with where it hides:

1. **Decision by absence.** Nobody saw it as a decision, so it is on no page:
   partition key, id minting, time source, record serialization, what the
   gate checks. Hides in constants and in the first commit.
2. **Vocabulary carrying a mechanism.** "Seal", "lock", "door", "gate" each
   imply a mechanism; a session may have built the implication. Hides in the
   parenthetical glosses next to Sid's words.
3. **The pick-two triangle.** Three commitments fine in pairs, impossible
   together (1 above). Hides because each pair was checked and the third was
   checked against a different pair.
4. **Boundary words with two meanings.** "Committed", "together", "cache",
   "stream", "forget", "directly" — a plain meaning in Sid's mouth, a
   technical meaning in Rama's or Electric's. Hides wherever a session
   nodded to the plain word and built the technical one.
5. **Sequencing as a decision.** Way in before way out. Hides as "the plan".
6. **Silent reinterpretation of a literal line.** "Directly to Rama." Hides as
   a faithful quotation beside an unfaithful build.
7. **Tests as confirmation.** A suite written from the same assumption cannot
   catch it. Hides in green.
8. **Grain drift.** A decision about "a value" or "a layer" is stable only
   while the word's grain is fixed; nobody fixed it. Hides in the noun.
9. **Conditional dropped.** "By default", "could be either", "for now" fall off
   between the chat and the page. Hides in the page's crispness.
10. **Purpose unstated.** A mechanism with no named adversary or goal (the
    seal). Hides because mechanisms look like decisions.
11. **Bootstrap blindness.** The model assumes a populated store; the first
    record is the exception path. Hides in a seed or a fixture.
12. **The tool's two ends taken as the design's two ends.** Rama has stream and
    microbatch; a layer is therefore one or the other. Hides as a natural fit.

---

## Part 3 — what nobody asked at all

As far as the brief shows. Each is **[supposition]** about the record and
**[follows]** as a question the design raises:

- What is a value — its type, its grain, its size — and may it be a reference?
- What does the gate check that needs the value opened?
- Where does a lock live, and is that place append-only?
- Against whom, or for what, is the seal?
- Who writes the first permission into an empty layer?
- Who mints entity ids, and is an entity the same entity in every layer?
- What does "together" promise a reader in a streaming layer?
- What does "committed" mean in each layer kind, and does a fact carry the act
  that made it?
- What does the screen show between an act and its committed row?
- What counts as a cache?
- Who is the "who" when an agent acts for a person?
- Does the depot record carry every field the way out will ask of it?
- Which of the words in use are Sid's, and which came with a mechanism attached?
- Who may forget, and what does forgetting leave behind?
- Was "way in first" Sid's order?

---

## Part 4 — the questions I would ask Sid

In the order the answers unblock the most.

1. Does the gate need to see a value's content to accept or refuse it? If not,
   the lock need never enter the log.
2. In one line: what is the seal for? Forgetting only, secrecy from someone,
   or proof that nothing changed? Did "seal" ever mean "you can tell it was
   tampered with" rather than "you cannot read it"?
3. What must "forget" cover: the value only, or also the key, the entity, the
   act, and who saw it? Must it hold against a backup?
4. When an agent acts for a person, whose name is on the act — the agent's,
   the person's, or both?
5. When a screen shows a committed result, must it be able to say "this is
   *my* act, landed", or is "this place now holds this" enough?
6. How does a layer come to exist, and who holds its first permission?
7. When my layer and the base both hold a fact at the same place, what does a
   reader see — mine over the base, both, or is my layer a copy with a parent?
8. What is a value — the unit a maker changes in one act? A word, a field, a
   whole document?
9. In a streaming layer, may a reader ever see half an act? If never, that
   constrains how acts are partitioned; if sometimes, the screen must say so.
10. What counts as a cache: any copy held anywhere, a copy consulted instead of
    asking Rama, or a copy that can go stale without being told?
11. During the moment before a microbatch layer commits, what does the screen
    show of the maker's own act — nothing, a mark, the act as the maker sent it?
12. Was "the way in first, the way out later" your order, or a session's?
13. Which of door, gate, seal, lock, layer, act, place are your words?
