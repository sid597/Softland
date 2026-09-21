# Rama, Nathan Marz, and the Kreps dissent: what an append-only fact store must fix before its first record

Research session: research-1. Cutter: Claude Fable 5.1, max effort. Date: 2026-09-20.
Gatherers (fetch and anchor only, no judgment): three Sonnet sub-agents. Every quote below was re-read by the main session in the saved page text before it went in.
Brief: from softland-ff, round one. Sid's current leanings were withheld on purpose, so nothing here is fitted to them.

## How to read this

Markers in Part one (what Rama's reference says):

- **CHECKED** — I read the passage. The file and line are given. Paths are from the repo root. `docs/NN-…` means `reference/rama/docs/NN-…`. `skill/…` means `.claude/skills/rama/references/…`.
- **IMPLIED** — my inference from checked passages. The reference does not say it in these words.
- **NOT IN THE REFERENCE** — I searched and found nothing.

Markers in Parts two and three (what people would say):

- **REPORTED** — they wrote or said it. A quote or close paraphrase with source is given.
- **INFERRED** — I reconstructed it from their system or principles.
- **INSTITUTIONAL** — company docs or a multi-author paper; I infer what the institution thought.

Provenance of the Rama reference. `reference/rama/docs/` is an extraction of the Red Planet Labs docs generated 2026-03-22; no Rama release is recorded for it. `.claude/skills/rama/` is vendored from `redplanetlabs/rama-ai-learn` at sha `0b46060`, dated 2026-08-05. It is RPL's own guidance for agents and is the newer of the two. On 2026-09-20 I re-fetched four live doc pages (depots, microbatch, operating-rama, downloads) and confirmed the load-bearing claims below still stand. The downloads page shows Rama 1.9.0 as current.

Numbers in brackets like (0), (2), (17) are Sid's question numbers from the brief.

Quoting. Long passages in double quotes are copied from a source, and the source is named next to them. I ran a script over the finished report that checks each quoted passage against the saved source text; all of them matched. Short phrases in double quotes ("as of", "based on", "never rewrite", "by whom") are the brief's own terms.

---

# Part one — What Rama's own reference says

## The short version

Eleven findings from the reference matter most for the first record. Each is backed in the sections below.

1. **Rama does not promise "never rewritten". It promises that nothing is lost and that positions never move.** Depots keep everything by default. But Rama ships two doors for rewriting the log: depot migrations (transform or excise any record, offsets unchanged) and depot trimming (drop old records). "Never rewrite" is Sid's rule to hold, not Rama's.
2. **The partition count is fixed at launch.** "Tasks cannot be changed after launch." Growing past it means copying every depot into a new module. Log positions change when that happens.
3. **So a log position is not a safe name for a fact.** Positions survive migrations. They do not survive a re-partition, a backup restore, or a trim. Only an id carried inside the record survives all four.
4. **Ids must be made by the offerer, before the append.** Rama's own guidance: client-side UUIDv7, 128 bits minimum, never generated inside a stream topology. The reason is retries. This is the strongest single instruction the reference gives on Sid's table.
5. **An append that throws may still have landed.** The offerer cannot tell. Only an offer id plus a kept verdict lets them find out. That makes keeping the gate's yes/no, refusals included, a structural need and not a nicety.
6. **Rama offers two different gates.** A stream gate is fast (milliseconds), can hand the verdict back inside the append call, is atomic only within one partition, and is at-least-once. A microbatch gate is exactly-once, atomic across all partitions, has a free global tick number, is slow (300 ms and up), cannot answer the offerer, and stops for everyone if any one task group stalls or any one record throws.
7. **With a microbatch gate, "as of" can be one number.** The microbatch ID is a 64-bit counter, the same on every partition, readable in topology code. With a stream gate there is no global number; "as of" is a position per partition, and the reference shows no way for topology code to see depot offsets.
8. **A topology writing to a depot is not exactly-once.** Only PState writes are. So if admitted facts are re-published to a second depot, that depot can hold duplicates, and every consumer must dedupe by fact id.
9. **Replay rebuilds only what is deterministic.** Rama's own re-partition recipe "only works if your processing is deterministic". A clock read or a generated id inside the gate is not. Whatever the gate adds (when, version) must be stored as primary data and copied, never recomputed.
10. **Rama records no reads.** There is no read log, no read provenance, no query audit. "Based on" is entirely Sid's convention. Rama neither helps nor hinders.
11. **Rama has no authentication, no authorization, and no encryption in this reference.** Any client with a cluster connection can append to any depot and read any PState. "By whom", "who may see", and "delete by destroying a key" all have to be built above or in front of Rama.

## Question by question

### (0) The log itself: never rewritten, or only never lost?

- **CHECKED** `docs/14-depots.md:316` — "By default, depots permanently store all data appended to them."
- **CHECKED** `docs/14-depots.md:288` — "A depot's records can be migrated to new values or excised as part of a module update. A migration is specified as a function of one argument that takes in a depot record and returns the new value to replace it with. If the function returns Depot.TOMBSTONE, the record is removed from the depot."
- **CHECKED** `docs/14-depots.md:310` — "Depot migrations never change the offsets of records. If a record is excised, a small tombstone value is written in its place. Topologies and foreign depot reads will exclude those tombstones when reading from the depot."
- **CHECKED** `skill/depot-migration.md:85-93` — the mechanism is two logs. Rama builds a new log beside the old one, then "atomically switches to the new log and deletes the original."
- **CHECKED** `docs/14-depots.md:306` — the migration function "must be idempotent (as of version 1.5.0)" and it also runs on new appends until the module is updated again to remove it.
- **CHECKED** `docs/14-depots.md:320-327` — trimming is opt-in through the dynamic option `depot.max.entries.per.partition`. By default a record is not trimmed while any topology in any module still needs it.
- **CHECKED** `docs/03-first-module.md:107` — the stated purpose of keeping the log: "If you deploy a bug that corrupts your views, you can always recompute those views from scratch from the raw data in the logs… Event sourcing also provides a natural audit trail."

What this adds up to. Rama's answer: never lost by default, rewritten when the operator decides, positions stable either way. A depot migration is a deploy, not a per-record call. It rewrites the whole depot in the background. **IMPLIED:** if Sid wants "never rewritten" to be true of the store, that is a policy Sid holds over the operator (never ship a depot migration, never set the trim option). Rama will not enforce it. The other reading is also open: keep "never rewrite" as the rule for meaning, and keep Rama's migration door for representation and for lawful erasure. Part two shows Marz has always taken the second reading.

### (2) Entity: how is an id made so two never clash? May an id give away when or where it was made?

- **CHECKED** `skill/unique-ids.md:9` — "If any consuming topology is stream: Generate the ID client-side with `(ops/random-uuid7)` and include it in the depot record. Do NOT generate IDs inside stream topologies — on retry, `ops/random-uuid7` or `ModuleUniqueIdPState` produce a different ID, making the entire entity creation non-idempotent."
- **CHECKED** `skill/unique-ids.md:13` — "128 bits is the minimum size needed to avoid birthday paradox collisions at scale… random 64-bit Longs have ~10% collision probability at just 2 billion IDs — unacceptable for production."
- **CHECKED** `skill/unique-ids.md:15` — "Always use UUID7 over UUID4 when generating UUIDs. UUID7 is time-ordered — IDs sort chronologically, which is useful for range queries on subindexed maps and preserves insertion order."
- **CHECKED** `skill/unique-ids.md:17` — ordering is only to the millisecond. "Don't write code or tests that assume sub-millisecond ordering of UUID7s."
- **CHECKED** `skill/unique-ids.md:44-46` — the in-cluster alternative, `ModuleUniqueIdPState`, packs "22 bits for the generating task ID and 42 bits for a monotonically increasing counter". It is "Safe to use in microbatch topologies" and "Tricky in stream topologies".
- **CHECKED** `skill/unique-ids.md:98` — composite ids are endorsed: "a 'post' can be identified by `[user-id task-unique-post-id]`".

On whether an id may give away when or where it was made: Rama's guidance does not treat this as a problem. It treats it as a feature. UUIDv7 gives away the millisecond. The module id generator gives away the task. **NOT IN THE REFERENCE:** any privacy or unlinkability concern about ids. **IMPLIED:** the reason RPL likes time-ordered ids is physical. PState maps are sorted by the serialized key (`docs/17-serialization.md:165-169`), so time-ordered ids put new entries next to each other on disk and make "latest N" a cheap range scan. Random ids scatter writes. If Sid wants ids that reveal nothing, the cost is paid in index locality, and the time order has to be carried by some other sorted key.

One more constraint from partitioning. The depot partitioner hashes something in the record, "modded by number of tasks" (`docs/16-partitioners.md:153`). **IMPLIED:** whatever the id is, the part of it that the partitioner reads decides which facts are ordered with which, for the life of the module.

### (17) The first facts: what ids, who writes them, the same in every store?

**NOT IN THE REFERENCE.** Rama has no seed-data or genesis hook. The nearest things:

- **CHECKED** `docs/12-microbatch-topologies.md:37` — a global PState can be given an `initialValue`. That is a constant in module code, not a fact.
- **CHECKED** `skill/depot-reference.md:138-146` — a new topology can start from `:beginning` of a depot, once, on first deploy.

**IMPLIED:** the first facts are ordinary appends made by some client after launch. If they sit at the head of the offers depot, and the depot is never trimmed, every topology ever added later can replay them. Whether they are "the same in every store" is a convention outside Rama. One Rama fact bears on it: the gate needs grammar and policy facts in order to admit anything, so the very first appends must be admitted by rules that are not yet facts. The reference gives no pattern for that. Module code is the only place those first rules can live.

### (3) Key: a word, or an id too?

**NOT IN THE REFERENCE** as a question. Two nearby facts:

- **CHECKED** `docs/17-serialization.md:111` — RPL's own Thrift example gives each type an explicit numeric id and keeps the name out of the wire format: "Needing to distinguish types on the wire is a common problem… Explicit type IDs are one solution."
- **CHECKED** `docs/17-serialization.md:167-169` — anything used as a PState map key must serialize to the same bytes every time, and sorts on disk by those bytes.

**IMPLIED:** Rama is indifferent. A keyword and a UUID both work as keys. The Thrift example shows the camp's habit: a stable id on the wire, the human name as something looked up.

### (9) Value: never removed, so how is one deleted, backups included?

- **CHECKED** depot excision exists (`docs/14-depots.md:288-310`, quoted under (0)). The slot stays. The content goes. Readers skip it.
- **CHECKED** `docs/15-pstates.md:181-200` — PStates are mutable, so removal there is an ordinary write (`termVoid`). One trap: "If you instead remove a parent of the subindexed structure, the reference to the subindexed structure will be deleted but the individual elements on disk will not… you should delete them explicitly before deleting the top-level key."
- **CHECKED** `docs/22-backups.md:19-34` — backups hold depots, PStates, topology progress, the module jar, and configs. They are incremental and file-level ("log segments for depots, SSTs and write-ahead logs for PStates").
- **CHECKED** `docs/22-backups.md:112-114` — old backups go only when backup GC says so: `backup.max.age.hours`, `backup.min.backups.to.keep`.
- **NOT IN THE REFERENCE:** encryption at rest, per-record keys, crypto-shredding, or any statement on how an excised record leaves old backups.

**IMPLIED:** in Rama an erasure has three parts. (a) A depot migration that tombstones the records. It is a module update and a whole-depot rewrite, so erasures would be batched, not done one by one. (b) Explicit removal from every PState that copied the value, minding the subindex trap. (c) Waiting out the backup retention window, because old backup files still hold the bytes. The erasure is complete only when the oldest backup that contains the record has been collected. If Sid wants erasure to be one small act, the usual answer (encrypt each subject's values with their own key, destroy the key) is outside anything this reference offers, and must be fixed before the first record because it changes what bytes go in the value.

### (8) By whom: who checks it? Is a person's agent itself, or the person? Where is "acts for" recorded?

**NOT IN THE REFERENCE.** I searched every page for authentication, authorization, permission, credential, access control, encryption, TLS. Nothing, apart from an app-level password hash in the tutorial.

- **CHECKED** `skill/depot-design.md:132-134` — "Depot partitioners run on the appending client." The client is trusted code.
- **CHECKED** `docs/24-rest-api.md:5-20` — the REST API takes plain HTTP POSTs for appends and reads. No credential appears in it.
- **CHECKED** `docs/18-module-dependencies.md:7-9` — any module on the cluster can mirror any other module's depots and PStates.

**IMPLIED:** Rama treats everything that can reach the cluster as one trust domain. "By whom" is a field the offerer fills in. Rama will not check it. Either a tier in front of the cluster vouches for the actor, or the offer carries a signature that the gate topology verifies. Which of the two is a first-record decision, because a signature has to be in the record from the start to be checkable later.

### (11) When: whose clock? Ever used for order?

- **CHECKED** `docs/14-depots.md:196` — "Appends also store and index in the depot partition the time of the append. This is used for 'start from' options." This is the depot leader's clock. **NOT IN THE REFERENCE:** any way for topology code to read that time, or a record's offset.
- **CHECKED** `docs/06-tying-it-together.md:404` — RPL's own tutorial stamps time inside the ETL: `.each(System::currentTimeMillis).out("*joinedAtMillis")`. That is the clock of whichever machine leads that task.
- **CHECKED** `skill/testing.md:323` — module code should call `TopologyUtils/currentTimeMillis` so tests can control time.
- **CHECKED** order never comes from time. It comes from the depot partition: "local ordering" (`docs/14-depots.md:62`).

**IMPLIED:** the brief's "stamped by the gate at admission" would mean, in Rama, the wall clock of the task leader that ran the gate for that partition. Different partitions are led by different machines. So the stamp is comparable within a partition only as far as one machine's clock is steady, and across partitions only as far as NTP holds. A leader change can step it backwards. Rama's camp would record it and never sort by it. Under a stream gate, a retry re-reads the clock, so the same offer can get a different "when" on replay unless the first verdict is looked up and reused.

### (16) Layer: who sees a fact before any permissions exist?

**NOT IN THE REFERENCE.** Rama has no read permissions. The one visibility rule it has is about commit, not people:

- **CHECKED** `skill/microbatch.md:130` — "readers outside the owning topology — query topologies, foreign reads, other topologies — see only committed state."

**IMPLIED:** inside the cluster, everyone sees everything that has committed. "Who sees" is enforced by whatever stands between people and the cluster.

### (10) Order: can two gates ever write one layer? What must share a partition? Is "as of" one number or a position per partition?

- **CHECKED** `docs/15-pstates.md:41` — "a PState can only be updated by the ETL topology that owns it."
- **CHECKED** `docs/23-acid-semantics.md:27` — "each task is single-threaded. This means all actions on a task happen in serial."
- **CHECKED** `docs/23-acid-semantics.md:15` — "stream topologies are transactions for changes on a single partition, while microbatch topologies are cross-partition transactions for every change across all partitions."
- **CHECKED** `docs/14-depots.md:62` — without a shared partition, "data on different partitions are processed in parallel and independently."
- **CHECKED** `docs/14-depots.md:121` — "Data should be appended to the same depot when related… Data is related if local ordering is important or if they affect the same conceptual entities."
- **CHECKED** `docs/14-depots.md:234` — "A record in a depot partition is identified by a 'partition index' and an 'offset'." There is no global offset.
- **CHECKED** `docs/25-integrating.md:336` — "The microbatch ID is a 64 bit value that increments by one with each successful microbatch… each microbatch ID is associated with a specific range of data on each depot partition."
- **CHECKED** `skill/microbatch.md:140` — `(ops/current-microbatch-id :> *mb-id)` makes that number readable in topology code.
- **CHECKED** `skill/microbatch.md:128` — "external readers can observe two tasks on different microbatches at the same moment… Cross-partition atomicity… is a property of the settled result, not of what a reader sees mid-commit."

Two gates, one layer: in Rama this cannot happen by accident. One topology owns a PState. One thread runs a partition. **IMPLIED:** a compare-and-set on a cell needs no lock; it is a read and a write in one event on the cell's task.

What must share a partition: under a stream gate, everything that has to change in one atomic step. That is the cell, the fact, the verdict, and any index that must never disagree with them. **IMPLIED:** also whatever the gate must read to decide. A grammar fact or policy fact on another partition costs a hop, and a hop ends the transaction (`skill/stream.md:41`). The check and the write are then two steps, and the policy can move between them. Under a microbatch gate nothing has to share a partition for atomicity, because the whole microbatch is one transaction.

"As of": under a microbatch gate, one number works, because every partition advances through the same microbatch IDs. A reader must still confirm that the partitions it touches have all committed that ID. Under a stream gate, "as of" is a position per partition. Since the reference shows no way for topology code to see offsets, the gate would have to keep its own per-partition counter.

### (11) Based on: is every read listed?

**NOT IN THE REFERENCE.** Rama keeps no record of reads. Foreign selects, query topology invokes, and reactive subscriptions leave no trace. The only read guarantee it gives is monotonic: "You'll never read an earlier version of data from a PState than you've already read" (`docs/23-acid-semantics.md:83`). Recording reads is wholly Sid's design. **IMPLIED:** at machine write rates, each fact carrying its reads multiplies record size. The reference's rule of thumb for depot reads is about 50 KB per fetch (`docs/14-depots.md:242`), which hints at the record sizes RPL has in mind: small.

### (4) Based on: depends-on versus how-I-got-here; follow latest or pin the version?

**NOT IN THE REFERENCE.**

### (5) Based on: if an index was behind the log when a pattern was read, is that written down?

- **CHECKED** the depot head is readable by clients: `foreign-depot-partition-info` returns `{:start-offset … :end-offset …}` per partition (`skill/depot-reference.md:243-244`).
- **CHECKED** topology progress is kept in internal PStates, `$$__streaming-state-<topologyId>` and `$$__microbatcher-state-<topologyId>` (`docs/11-stream-topologies.md:142`, `docs/12-microbatch-topologies.md:116`), and is shown in the Cluster UI. **NOT IN THE REFERENCE:** a client API to read it.
- **CHECKED** `skill/testing.md:157-165` — RPL's advice is to build your own: "materialize progress state as part of the design (e.g. counters of work enqueued and work completed)".

**IMPLIED:** how far the index had really got can be written on a pattern read only if the indexing topology writes its own progress mark into a PState. Under microbatch that mark is the microbatch ID. Under stream it is a counter the topology keeps per partition. This is cheap to add on day one and impossible to add for reads already recorded.

### (11) Because of: always filled?

**NOT IN THE REFERENCE.** Rama tracks a stream record's "event tree" for acking (`docs/11-stream-topologies.md:117`), but it is in-memory bookkeeping, never stored.

### (1) Version: is a fact pointed at by the gate's number, or by its own id? Random or from content?

- **CHECKED** Rama's own name for a record is (partition index, offset) (`docs/14-depots.md:234`).
- **CHECKED** that name survives depot migrations (`docs/14-depots.md:310`).
- **CHECKED** it does not survive a restore: "If Module A is restored to a prior version where the depot contained entries up to offset 107, then any new data appended after the restore will start at offset 107" (`docs/22-backups.md:106`). Old offsets get reused for different records.
- **CHECKED** it does not survive a re-partition: the documented way to get more tasks is a new module whose "processing would also make new depots that are a copy of the old depots" (`docs/19-operating-rama.md:487`).
- **CHECKED** it does not survive trimming (`docs/14-depots.md:325`).
- **CHECKED** RPL's guidance is an id carried in the record, made by the client (`skill/unique-ids.md:9`).

**IMPLIED:** point at facts by an id inside the record. Treat the gate's number as what it is: an ordering within one cell, not a name. On "random or computed from content": **NOT IN THE REFERENCE.** One consequence is worth stating. A content-derived id and Rama's migration door cannot both be used. A depot migration changes a record's content in place, so a hash of the content would stop matching. If Sid chooses content-derived ids, they are also choosing never to use depot migrations on those records.

### (6) When 37 replaces 25, is "replacing 25" kept on 37?

**NOT IN THE REFERENCE.**

### (7) Beside each fact: is the gate's yes/no kept? Where? Are refusals kept?

Rama does not answer this, but four checked facts push one way.

- **CHECKED** `docs/14-depots.md:212` — "An exception doesn't mean the append did not go through – it just means it didn't go through cleanly."
- **CHECKED** `docs/11-stream-topologies.md:297` — under throttling, "Depot appends with AckLevel.ACK while the limit is hit will throw an exception back to clients. In these cases the appends will have gone through."
- **CHECKED** `skill/stream.md:13` — "A stream topology can retry a record even after all PState writes have completed and committed… every write in a stream topology must be either naturally idempotent… or explicitly deduplicated."
- **CHECKED** `skill/microbatch.md:129` — "A record that throws deterministically retries forever… blocking the topology. Malformed input must be rejected in dataflow, not allowed to throw."

**IMPLIED:** (a) Every offer, refused or not, is already in the offers depot for good, so refusals are "kept" the moment the offer lands, unless something filters before the depot. (b) A refusal must be a value the gate writes, never an exception. (c) Under a stream gate, a replayed offer would find its own version already current and refuse itself, unless the gate first looks the offer up by id and returns the verdict it gave before. So a verdict index keyed by offer id is needed for correctness. (d) The offerer who got an exception needs the same index to learn what happened. Keeping the verdict is therefore forced. Where it lives: in a PState on the same task as the cell, written in the same event as the fact, so the two are atomic (`docs/23-acid-semantics.md:41`).

Can a fact and its yes/no land atomically? **CHECKED** yes. In a stream gate, if both writes are in one event on one task. In a microbatch gate, always.

How a refusal gets back to the offerer. **CHECKED** stream: `ack-return>` sends a value back through the `append` call (`docs/11-stream-topologies.md:200-202`). By default only the first attempt counts (`docs/14-depots.md:214`). **CHECKED** microbatch: it cannot. "Microbatch topologies do not integrate with depot appends because they run asynchronously to the appends" (`docs/12-microbatch-topologies.md:102`); `:ack` "confirms depot durability only" (`skill/microbatch.md:134`). The offerer reads the verdict PState, by polling or by a reactive `proxy` (`docs/15-pstates.md:436`).

### (12) Start of a session: is the running runtime version and the kind of machine written down? Are rebuilds facts?

- **CHECKED** `docs/07-terminology.md:39` — every deploy makes a new "module instance… a specific deployment of a module associated with a specific set of code."
- **CHECKED** `docs/25-integrating.md:191` and `skill/task-globals.md:81` — code can read its module instance id: `context.getModuleInstanceInfo().getModuleInstanceId()`, from inside a task global.
- **CHECKED** `docs/22-backups.md:19-26` — backups include the module jar.
- **NOT IN THE REFERENCE:** a runtime call for the Rama version, the jar's hash, or the machine kind.

**IMPLIED:** a gate can stamp each verdict with the module instance id that made it. Linking that id to a build is Sid's job: a constant baked into the jar, and a fact written at deploy time. Rama will not write "a rebuild happened" anywhere a topology can see.

### (14) The hand, and (15) a click

**NOT IN THE REFERENCE.** One number helps size (14): a stream gate tracks every record one at a time and is throttled per task (`topology.stream.max.executing.per.task`, `docs/11-stream-topologies.md:295`). Pointer motion as facts through a stream gate would compete with real writes for that budget.

### (13) Storage: plain maps or classes? Ever trimmed? Backups?

- **CHECKED** `docs/30-clj-serialization.md:9-13` — built in: basic types, java.util collections, Clojure data structures, and `defrecord`. "Under the hood, Rama uses Nippy."
- **CHECKED** `docs/17-serialization.md:40-45` — Java serialization is for "experiments and tests", because "you won't be able to deserialize old versions of that type". For production: "a solution for custom types with first-class support for evolving types over time", naming Thrift and Protocol Buffers. RPL's Mastodon build uses Thrift.
- **CHECKED** `docs/17-serialization.md:144` — "It's critical that all modules and clients that interact with those objects have this serialization registered."
- **CHECKED** `skill/operate.md:14` — "Client Rama version must match cluster version (same major and minor)."
- **CHECKED** trimming is off by default (above).
- **CHECKED** `docs/22-backups.md:126-143` — incremental online backup "requires a paid license". On the free version a backup means shutting the cluster down and copying the data directories and a Zookeeper snapshot.

**IMPLIED:** plain Clojure maps need no registration anywhere, which suits an envelope "both ends share forever". A custom type ties every client, for good, to a serializer jar. The camp's own preference, though, is a schema with evolution rules (Part two). The two can be reconciled: plain data on the wire, and the schema as a checked convention. That is what Sid's "grammar as a fact" already is.

## The gate: stream or microbatch

The brief asks this directly. Here is the comparison from the reference, then what it means.

| | Stream gate | Microbatch gate |
|---|---|---|
| Latency | "single-digit millisecond" (`skill/stream.md:5`) | "at least 300ms" (`skill/phase-1-plan.md:112`) |
| Delivery | at-least-once (`docs/11-stream-topologies.md:156`) | exactly-once for PStates (`docs/12-microbatch-topologies.md:126`) |
| Atomic scope | one event on one task (`docs/23-acid-semantics.md:41`) | the whole microbatch, all partitions (`docs/23-acid-semantics.md:37`) |
| Verdict to offerer | yes, `ack-return>` | no; read a PState |
| Global "as of" number | none | microbatch ID |
| In-gate id and counter generation | unsafe on retry | safe |
| One bad record | retried, then others continue | "retries forever… blocking the topology" |
| One stalled task group | that group's partitions fail | "all microbatch topologies in the module… continuously fail" (`docs/21-replication.md:79`) |
| Re-publishing to a depot | at-least-once | "do not have exactly-once semantics… this is on our roadmap" (`docs/12-microbatch-topologies.md:130`; still so in the live docs today) |

RPL's general advice: "Unless you require millisecond-level update latency for your PStates, you should generally prefer microbatch topologies" (`docs/12-microbatch-topologies.md:171`).

**IMPLIED, my reading.** Sid's "no optimism" rule changes the weight of that advice. If a not-yet-fact is never shown as a fact, then a person's own act does not appear until the gate has spoken. With a microbatch gate that is a third of a second at best, on every keystroke-level act, for everyone. With a stream gate it is milliseconds. So the latency row is not a tuning detail for Sid; it is felt in the hand. The price of the stream gate is on the rows below it: offers need client-made ids, verdicts must be kept, the compare-and-set must be written to be replay-safe, and everything that must agree must share a partition. Those are exactly first-record decisions. The microbatch gate would let Sid skip them and would give a global clock for free, but it makes admission for the whole planet depend on every task group being healthy at once. Rama modules can run both kinds of topology side by side. A stream topology for the interactive path with microbatch topologies for heavier derived indexes is a common Rama shape: RPL's Mastodon build and Multiply's backend both mix the two.

One structural point sits under all of this. In Rama the only ordered, replicated, never-trimmed log is a depot, and the only exactly-once writes are PState writes. Clients append to depots. Topologies write PStates. So the natural Rama reading of Sid's design is: **the offers depot is the log, and the facts are PState rows the gate writes.** "The gate is the only writer" is then true of the fact PStates by construction (`docs/15-pstates.md:41`). But it means the permanent record is the offers, and the facts are, in RPL's words, "essentially materialized views of depots, with depots being the source of truth" (`docs/04-depots-etls-pstates.md:120`). That is fine only if the gate's decision can be re-derived from the offers in order. A clock read breaks that. So either the stamp is carried into the offer before the append (by the front tier), or the fact PStates are declared primary data in their own right and protected as such. The Rama skill says PStates are already that in practice: "PStates are durable storage in their own right — replicated and persisted to RocksDB at write time, NOT a materialized view recomputed from depots" (`.claude/skills/rama/SKILL.md`). Sid should pick which of the two is "the log that is never rewritten", because Rama gives them different guarantees.

## Other items the brief named

**Partitioner choice and co-locating everything about one entity.** **CHECKED** `.claude/skills/rama/SKILL.md` "Colocate related data": design partitioning "around the application's core queries, not just the top-level key… `$$post->likes` might all partition by account ID (not post ID)". **CHECKED** `skill/depot-design.md:138-145`: "Match the partition key to the primary PState access pattern… Mismatched key forces `|hash` repartition (extra hop, breaks local ordering)." **IMPLIED:** for Sid the cell is entity + key + layer, but the partition key should be coarser than the cell, so that one entity's facts across keys and layers sit together and one gate event can see them all. The entity id alone is the obvious candidate. The cost: a very hot entity (a shared base-layer type that everything points at) is one partition's load. Facts that point at it live with their own entity, not with it, so reads of "everything pointing at X" are a fan-out or a second index.

**Module updates.** **CHECKED** `docs/19-operating-rama.md:441`: PState clients have zero downtime; "Depot appends may have some downtime where appends are buffered clientside… anywhere from two seconds to thirty seconds." **CHECKED** `:424`: "There's currently no way to rename a depot or PState in a module update." Removing one deletes its data and needs an explicit `--objectsToDelete`. **IMPLIED:** depot and PState names are forever. The brief says the runtime will be rebuilt hundreds of times. That is cheap in Rama terms (each rebuild is a module update), as long as names and the task count never change.

**PState lag and a readable cut.** Covered under (5) and (10). The depot head is readable. Topology progress is not, unless you materialize it. The microbatch ID is the only built-in global cut.

**Replication.** **CHECKED** `docs/21-replication.md:41`: nothing is visible until it is on disk on the leader and on all in-sync followers. `:81`: "we recommend a replication factor of three and a min-ISR of two." `:152`: cluster metadata and leader election live in Zookeeper.

**A later second store.** **CHECKED** mirrors are between modules of one cluster: "Mirrors in Rama don't store data locally – operations done on them always go through to the original" (`docs/18-module-dependencies.md:9`). Appends to a mirror depot do not wait for the other module's stream topologies (`:129`). Reads of another module's PStates are "similar… to read committed" (`docs/23-acid-semantics.md:104`). **NOT IN THE REFERENCE:** anything between clusters. No cross-cluster mirror, no federation, no geo-replication, and no guidance on regions or wide-area links. The only mentions of geography are a sample custom partitioner named `partition-by-region` and the phrase quoted in the next item. **CHECKED** the one bridge for outside logs is `ExternalDepot`: it "adapts an external partitioned log (e.g. Kafka, Kinesis, custom queue) into the Rama depot abstraction" by partition count, start offset, end offset, and fetch-by-offset (`skill/external-depots.md:3-27`). **IMPLIED:** a second store would be read as an external depot, by (partition, offset), and its facts re-admitted through the local gate as ingests. What crosses is the record with its own id. That is one more reason the id and the actor must be inside the record and meaningful without the store that made them.

**A single cluster deployed worldwide.** **NOT IN THE REFERENCE.** The reference describes clusters "in a data center, on the cloud, or on your local machine" (`docs/03-first-module.md:151`). **IMPLIED, and worth asking RPL about directly:** every append waits for its in-sync followers, and a microbatch gate waits for every task group. Spread across continents, that puts wide-area round trips inside every admission. Kept in one region, it puts every user on Earth a wide-area trip from the gate. The reference gives no guidance either way.

---

# Part two — Nathan Marz

## 1. What he built where this decision was load-bearing, and what he chose

Three systems over fifteen years. The differences between them are the lesson.

**BackType, then Twitter (2008–2013).** A master dataset of what he called DataUnits. Each one is a small typed fact: one property of one node, or one edge between two nodes. Thrift gives each a checked shape. They sit append-only in HDFS, split into folders by kind (`/data/PersonProperty/age/`). Everything the product shows is recomputed from them in batch. This is the closest published prior art to Sid's fact.

**The Lambda Architecture and the book (2011–2015).** The same design made general. Query = function(all data). The master dataset is raw, immutable, timestamped, append-only, kept forever. Batch views are recomputed from scratch. A speed layer covers the last few hours.

**Rama (started 2013, public 2023).** Depots are partitioned append-only logs. PStates are durable indexes, each owned and written by exactly one topology, kept up to date incrementally. In his words (HN, 2023-08-15, https://news.ycombinator.com/item?id=37139258): "Rama codifies and integrates the concepts I described in my book, with the high level model being: indexes = function(data) and query = function(indexes)."

## 2. His reasons, in his words

On what a fact is. "A piece of data is an indivisible unit that you hold to be true for no other reason than it exists. It is like an axiom in mathematics." And: "data is inherently time based. A piece of data is a fact that you know to be true at some moment of time." And: "data is inherently immutable. Because of its connection to a point in time, the truthfulness of a piece of data never changes… CRUD has become CR." (*How to beat the CAP theorem*, 2011, http://nathanmarz.com/blog/how-to-beat-the-cap-theorem.html)

On raw versus derived. "When you keep tracing back where information is derived from, you eventually end up at information that's not derived from anything. This is the rawest information you have: information you hold to be true simply because it exists. Let's call this information data." (Marz and Warren, *Big Data*, Manning 2015, chapter 1; free sample chapter, mirrored at http://www.odbms.org/wp-content/uploads/2014/02/BD_meap_ch01.pdf)

On why immutable. His first reason is people, not machines. "In a production system, it's inevitable that someone will make a mistake sometime, such as by deploying incorrect code that corrupts values in a database. If you build immutability and recomputation into the core of a Big Data system, the system will be innately resilient to human error." And: "That raw pageview information is never modified. So when you make a mistake, you might write bad data, but at least you won't destroy good data." (*Big Data*, chapter 1)

On small facts. "There are two major conceptual advantages to working with graph-based schemas. The first is the ability to add partial data about an entity in your system… Second, by splitting up data into a lot of small containers, it's easy to specify what data you want to work with." (*Thrift + Graphs = Strong, flexible schemas on Hadoop*, 2010, http://nathanmarz.com/blog/thrift-graphs-strong-flexible-schemas-on-hadoop.html)

On his chapter 2, summarized by him. "The core idea is that each record should be a 'fact' that stands on its own as something true at a moment in time. When you write your batch computations, you should make them work on any set of valid facts. There's nothing wrong with saying the same record twice, as logically 'A and A' is the same as 'A'." (HN, 2012-01-10, https://news.ycombinator.com/item?id=3449142)

On schemas. "JSON doesn't give you a real schema and doesn't protect against data inconsistency… A good schema protects you against these kinds of errors, keeps your data consistent, and gives you errors at the time of creating a bad object." (*Thrift + Graphs*, 2010). Fourteen years on: "Schemas themselves are extremely important, and they should be as tight as possible." (HN, 2024-01-09, https://news.ycombinator.com/item?id=38934011)

On what is wrong with databases. "There's a fundamental tension between being a source of truth versus being an indexed store that answers queries quickly. The traditional RDBMS architecture conflates these two concepts into the same datastore. The solution is to treat these two concepts separately." (*Everything wrong with databases and why their complexity is now unnecessary*, 2024, https://blog.redplanetlabs.com/2024/01/09/everything-wrong-with-databases-and-why-their-complexity-is-now-unnecessary/)

On one writer. "Rama PStates are globally readable but not globally writable. They are only writable from the topology that declares them. All code writing to PStates is thereby always in the exact same program. Additionally, since PStates are not the source of truth – the depots (event logs) are – mistakes can be corrected via recompute from the source of truth." (HN, 2024-01-10, https://news.ycombinator.com/item?id=38942073)

## 3. What he later changed, regretted, or migrated

I found no essay where Marz says "I was wrong". The gatherer searched for a written reply to Kreps and found none. The changes show in what he built next. Six of them.

**(a) From recompute-everything to incremental views.** In 2011 the enemy was incremental state: "It's a reliance on incremental algorithms and mutable state that leads to complexity in our systems." The book has a section titled "The problems with fully incremental architectures". In 2023: "Rama is not batch-based. That is, PStates are not materialized by recomputing from scratch. They're incrementally updated either with stream or microbatch processing. But PStates can be recomputed from the source data on depots if needed." (HN 37139258). He kept the log as the source of truth and the right to recompute. He dropped the second system and the constant recompute. That is the shape Kreps argued for in 2014. Rama's own recipe for re-partitioning a module (start a new module from the beginning of each depot, switch clients when caught up) is Kreps's Kappa recipe almost step for step. Marz now describes a depot as "exactly like the Apache Kafka but built into the system" (Software Engineering Daily, episode 1600, 2023-12-28, transcript at https://softwareengineeringdaily.com/wp-content/uploads/2023/12/SED1600-Rama.txt).

**(b) From a set of timestamped facts to ordered logs.** In 2010–2012 the master dataset was a set. Duplicates were harmless. Order came from the timestamp inside each fact: "you may have multiple gender DataUnits for someone… you would select the most recent gender DataUnit." (*Thrift + Graphs*). In Rama, order comes from the depot partition ("local ordering"), and the timestamp is just a field: "The appended records would have three keys: userId, location, and timestamp. The depot is partitioned by userId." (HN 38942073). He moved from time-as-order to position-as-order.

**(c) "Forever" got doors.** Already in 2011 he allowed three kinds of deletion: bad data ("delete the bad data and precompute the queries again"), low-value data ("Garbage collection is simply a function that takes in the master dataset and returns a filtered version of the master dataset… Mutability is really just an inflexible form of garbage collection"), and law ("regulations requiring you to purge data after a certain amount of time. These cases are easily supported"). Rama turned these into features: depot migrations with tombstones, and trimming. "Depots (the 'event sourcing' part of Rama) can be optionally trimmed… Some applications need this, while others don't." (HN 38934011)

**(d) RPL now says in writing where recompute fails.** From RPL's migrations post (Sam Adams, 2024, INSTITUTIONAL, https://blog.redplanetlabs.com/2024/09/30/migrating-terabytes-of-data-instantly-can-your-alter-table-do-this/): recomputing from depots may be "infeasible or impossible" when "you've enabled depot trimming", when "your existing PStates have data that was non-deterministically generated", or when "scanning millions of depot records might be egregiously inefficient". The second case is exactly a gate that stamps clocks and versions.

**(e) Ids grew up.** Rama's current guidance is 128-bit UUIDv7, made by the client, because "random 64-bit Longs have ~10% collision probability at just 2 billion IDs" (`skill/unique-ids.md`). From memory of *Big Data* chapter 2, which I could not open in this session, the book's pageview example used a random 64-bit nonce to make each fact identifiable. If my memory is right, that is a quiet reversal. Treat it as unverified.

**(f) Retry safety got stricter.** His 2025 collaborative-editor post builds a version-checked write in a stream topology and never mentions retries (I searched the post for retry, idempotent, duplicate: no hits). RPL's 2026 agent guidance says "every write in a stream topology must be either naturally idempotent… or explicitly deduplicated." INSTITUTIONAL. The rule was learned between the two.

## 4. Which questions he speaks to, and what he would say

**(0) Never rewritten, or never lost?** REPORTED: for Marz "immutable" means a new write never destroys an old one. It does not mean the bytes are never touched. He deletes bad data, collects garbage, purges for regulators, and migrates schemas: "When you change your schema, you'll have the capability to update all data to the new schema." (*Big Data*, chapter 1). INFERRED: he would tell Sid that "never rewritten" is stronger than the property that does the work. The property that does the work is: never overwritten, always re-derivable.

**(2) Entity ids.** REPORTED 2010: his nodes were named by typed, natural identifiers in a union (`union PersonID { 1: string email; }`, a project by apache name or github id or sourceforge id). The id carries the type. That is the opposite of Sid's opaque id with no type slot. INSTITUTIONAL 2026: client-made UUIDv7. INFERRED: he does not value opacity. He values an id that survives a retry and sorts well on disk. He would not worry that an id reveals its millisecond. He would ask Sid a different question: when the same paper is ingested twice, what makes the two entities one? In his model that is a computation over "these two are the same" facts, not something an id scheme solves. (The book's treatment of this is from memory, not opened.)

**(17) First facts.** He has not written about this. INFERRED from his practice: the first rules live in code, because his schemas and functions always did.

**(3) Key as word or id.** REPORTED: Thrift, his tool of choice, identifies a field by number and lets the name change. "To add a new kind of property, you just add a new field into one of the property structures." INFERRED: a key is a stable id; its name and shape are things said about it. He would agree with the second half of Sid's question.

**(9) Deleting values.** REPORTED: purge for regulation is "easily supported"; bad data is deleted and views recomputed; Rama has tombstones. I found nothing by him on GDPR by name (HN search: zero hits), nothing on erasing from backups, and nothing on encryption beyond one line: "Things like E2E encryption are pretty easy to implement on top of Rama's existing primitives" (HN, 2023-08-16, https://news.ycombinator.com/item?id=37153669). INFERRED: he treats erasure as an operator's batch job over the dataset, and sees no conflict with immutability, because deleting says nothing about whether the fact was true.

**(8) By whom.** Almost nothing. REPORTED, one sentence: "Because the events store is immutable and constantly growing, redundant checks, like permissions, can be put in to make it highly unlikely for a mistake to trample over the events store." (*Big Data*, chapter 1). Permissions, for him, protect the log from mistakes. They sit in front of it. His writers were a handful of trusted pipelines inside one company. INFERRED: he would treat the actor as a field in the record.

**(11) When.** REPORTED: every fact carries a time, and it is the time the statement was true or was made, supplied with the record. 2010–2012: that time orders facts. 2023 on: depot position orders facts and time is data. INFERRED: he would keep both a source time in the offer and the gate's stamp, and order by neither.

**(10) Order and one writer.** REPORTED: one topology writes a PState; partition by the entity; "This entire topology definition executes atomically – all the PState queries, operational transformational logic, and PState writes all happen together and nothing else can run on the task in between… Rama doesn't have explicit transactions because transactional behavior is automatic when computation is colocated with storage." (*Massively scalable collaborative text editor backend with Rama in 120 LOC*, 2025, https://blog.redplanetlabs.com/2025/04/01/massively-scalable-collaborative-text-editor-backend-with-rama-in-120-loc/). That post is the nearest thing he has written to Sid's gate. Each edit carries the version its author saw. The depot is hashed by document id. The version is simply the length of the document's edit list. One detail matters: when the version is stale, his gate does not refuse. It transforms the edit against what was missed and admits it.

**(11, 4, 5) Based on; recording reads.** He has never written about recording reads as provenance (HN search for "provenance": zero hits). Two things bear on it. First, his definition: a fact "stands on its own". In his vocabulary, something whose truth depends on what was read is not data. It is a view. Sid's split between offers and running answers is the same line. But Sid also hangs reads on raw facts (a person's act, based on what they saw). That has no counterpart in Marz's model. Second, REPORTED: his 2025 agent platform records this kind of thing, and records it in the runtime, not in the data. "A trace of every agent invoke is viewable… Every aspect of execution is captured, including node emits, node timings, model calls, token counts, database read/write latencies, tool calls, subagent invokes." (Agent-o-rama README, https://github.com/redplanetlabs/agent-o-rama). And: "model calls are automatically traced – this node didn't have to record any tracing info explicitly." (*Introducing Agent-o-rama*, 2025, https://blog.redplanetlabs.com/2025/11/03/introducing-agent-o-rama-build-trace-evaluate-and-monitor-stateful-llm-agents-in-java-or-clojure/). INFERRED: on "are entries the runtime fills in listed?", he would say the runtime should fill in nearly all of them, because authors forget. On where they live, he would keep them in a trace keyed by the run, beside the facts, not inside each fact.

**(1) Pointing at a fact.** INFERRED from RPL guidance: by an id in the record. He has not discussed content-derived ids.

**(6) "Replacing 25" on 37.** Not addressed. INFERRED: in his designs the newer record wins by position in the cell's list (Mastodon keeps "a list of status content versions… to capture the edit history"). He would call "replaces 25" a copy of something the order already says.

**(7) The gate's yes/no; refusals.** REPORTED, and it cuts against the premise: his master dataset had no gate beyond a type check. "There's nothing preventing a Person from having multiple ages… That's absolutely true… Here's the thing though: it doesn't matter… you can resolve this during a 'full recompute' when choosing the data to ship to the application layer." (*Thrift + Graphs*). INFERRED: he would say the raw fact is that this actor offered this value, expecting version 25. That is true forever. Which offer won is a view over offers. If the rule for winning has a bug, the offers are still there and the view is recomputed. So on Sid's question he lands firmly: keep every offer and every verdict, refusals included, because the gate is code, and code is where human faults come from.

**(12) Runtime version.** Not addressed directly. INFERRED from human-fault tolerance: after a bad deploy you must find what the bad code wrote. That needs the code's identity on what it wrote.

**(13) Storage.** REPORTED: tight schemas; Thrift or Protobuf for anything that must evolve; trimming optional; backups built in. "The free version of Rama can run clusters up to two nodes" (*Rama… is now free for production use*, 2025, https://blog.redplanetlabs.com/2025/03/18/rama-the-100x-developer-platform-is-now-free-for-production-use/).

(14), (15), (16): he does not speak to these.

## 5. What resembles Sid's situation, and what differs

Resembles. Small atomic facts, one statement each, as the only source of truth. Timestamped. Never overwritten. Everything else derived and re-derivable. Human error as the main threat. The same person wrote the runtime Sid runs on, and has since built agent tracing on it.

Differs, and each of these limits the transfer:

- His facts described the world (tweets, pageviews), written by a few trusted pipelines in one company. Sid's facts are acts by many writers who must be told apart, checked, and sometimes refused. Marz never had a gate, a policy, or a compare-and-set on the master dataset.
- He never recorded reads.
- His schemas and functions lived in code, deployed together, one version live at a time. Sid's grammars, policies and tools are facts in the store, with every version live forever.
- He always kept a door open to rewrite representation. Sid closes it.
- He trusted the operator. One company owned the dataset. For a planet-wide store with an economy on it, the operator's power to rewrite is itself a trust question. His comfort with migration does not carry over for free.
- His clusters are per customer, in a datacenter: "each user will just have their own cluster" (HN 37153669; "user" there means a Rama customer). Nothing he has written covers one store for everyone.

## 6. Who he disagrees with, on what

- Databases as a category: global mutable state, and one store trying to be both truth and index.
- Kreps, 2011–2014, on whether a batch layer is needed. His later practice moved to Kreps's side.
- Kreps, still, on integration. Rama is the claim that one system can be log, index and compute. Kreps calls that hubris (Part three).
- The Datomic and XTDB line, by implication. RPL's case study of Multiply is the evidence (INSTITUTIONAL, and it is RPL's own marketing, so weigh it as such): a Clojure team building "an AI-powered platform for collaboration and co-creation" with agents, first on Datomic "for its immutable data model", then XTDB, then Rama. Their stated problems: "Squeezing everything into a fixed data model was unnatural", "It often felt like we were working against the database", "We ran into bottlenecks for running deep live queries… we were unable to get fault tolerance across multiple nodes." (https://blog.redplanetlabs.com/2025/03/04/how-multiply-went-from-datomic-to-xtdb-to-rama/). Read closely, their pain was not facts as the source of truth. It was one fixed index over facts as the only way to ask. Their Rama build has 17 depots "according to the entities they affect" and 19 purpose-built PStates.

---

# Part three — The dissent: Jay Kreps

## 1. What he built, and what he chose

Kafka at LinkedIn (2010 on), then Confluent. A partitioned, replicated, append-only log as the one thing every other system reads from. His choices: order only within a partition; a record's position is its permanent name; retention is a setting; derived state belongs to consumers and must be rebuildable by deterministic code reading the log.

## 2. His reasons, in his words

On log time. "The log entry number can be thought of as the 'timestamp' of the entry. Describing this ordering as a notion of time seems a bit odd at first, but it has the convenient property that it is decoupled from any particular physical clock. This property will turn out to be essential as we get to distributed systems." (*The Log: What every software engineer should know about real-time data's unifying abstraction*, LinkedIn Engineering, 2013; the live URL now 404s, read via https://web.archive.org/web/20181229062300/https://engineering.linkedin.com/distributed-systems/log-what-every-software-engineer-should-know-about-real-time-datas-unifying)

On determinism. "If two identical, deterministic processes begin in the same state and get the same inputs in the same order, they will produce the same output and end in the same state." And: "Deterministic means that the processing isn't timing dependent and doesn't let any other 'out of band' input influence its results. For example a program whose output is influenced by the particular order of execution of threads or by a call to gettimeofday or some other non-repeatable thing is generally best considered as non-deterministic." (*The Log*)

On what to put in the log. "The 'state machine model' usually refers to an active-active model where we keep a log of the incoming requests and each replica processes each request. A slight modification of this, called the 'primary-backup model', is to elect one replica as the leader and allow this leader to process requests in the order they arrive and log out the changes to its state from processing the requests." His toy example: one log holds "+1", "*2"; the other holds "1", "3", "6". (*The Log*)

On position as state. "You can describe each replica by a single number, the timestamp for the maximum log entry it has processed. This timestamp combined with the log uniquely captures the entire state of the replica." (*The Log*)

On reading a lagging index. "The client can get read-your-write semantics from any node by providing the timestamp of a write as part of its query—a serving node receiving such a query will compare the desired timestamp to its own index point and if necessary delay the request until it has indexed up to at least that time to avoid serving stale data." (*The Log*)

On global order. "Each partition is a totally ordered log, but there is no global ordering between partitions (other than perhaps some wall-clock time you might include in your messages)… Lack of a global order across partitions is a limitation, but we have not found it to be a major one. Indeed, interaction with the log typically comes from hundreds or thousands of distinct processes so it is not meaningful to talk about a total order over their behavior." (*The Log*)

On one writer. "The processor is always the single writer for its local partition… This means that a lot of the complexity of reasoning about concurrent modifications from different processors disappears." (*Why local state is a fundamental primitive in stream processing*, O'Reilly, 2014, https://www.oreilly.com/content/why-local-state-is-a-fundamental-primitive-in-stream-processing/)

On keeping the log. 2013: "Of course, we can't hope to keep a complete log for all state changes for all time. Unless one wants to use infinite space, somehow the log must be cleaned up." (*The Log*). 2017: "if you just set the retention to 'forever' or enable log compaction on a topic, then data will be kept for all time… it's not insane, people do this all the time, and Kafka was actually designed for this type of usage." With a warning: "when a system is treated as the canonical source for data the bar for both software correctness and operational practices increases quite dramatically." (*It's Okay To Store Data In Apache Kafka*, Confluent, 2017, https://www.confluent.io/blog/okay-store-data-apache-kafka/)

## 3. Where he and Marz disagree

From *Questioning the Lambda Architecture* (O'Reilly Radar, 2014, https://www.oreilly.com/radar/questioning-the-lambda-architecture/).

He starts with what he accepts. "I like that the Lambda Architecture emphasizes retaining the input data unchanged. I think the discipline of modeling data transformation as a series of materialized stages from an original input has a lot of merit." And: "Reprocessing is one of the key challenges of stream processing but is very often ignored… Code will always change."

Then five disagreements.

1. **Two systems.** "The problem with the Lambda Architecture is that maintaining code that needs to produce the same result in two complex distributed systems is exactly as painful as it seems like it would be. I don't think this problem is fixable." His alternative: keep the log, run one stream job, and when the code changes "start a second instance of your stream processing job that starts processing from the beginning of the retained data, but direct this output data to a new output table. When the second job has caught up, switch the application to read from the new table."
2. **Is streaming second-class?** Marz in 2011 let the speed layer be approximate and had batch correct it. Kreps: "there is no reason that a stream processing system can't give as strong a semantic guarantee as a batch system."
3. **CAP.** "The CAP theorem, sadly, remains intact."
4. **Why the log exists.** For Marz it is there because people make mistakes. For Kreps it is there because code changes and many systems must read the same data. The first reason argues for keeping everything forever. The second argues for keeping as much as you may want to reprocess: "if you want to reprocess up to 30 days of data, set your retention in Kafka to 30 days."
5. **One system or many.** Kreps, 2017: "databases are mostly about queries, and I don't think Kafka really benefits from trying to add any kind of random access lookups directly against the log… Each of these systems have their own pros and cons, and I think it's hubris to think you can do better than all of them in a single system." Rama is the opposite bet.

What happened next. On points 1 and 2 Kreps was right, and Marz's own system shows it. Twitter, where Lambda was first built, followed. Summingbird, Twitter's one-program-two-systems framework, is marked "status: retired" on GitHub (https://github.com/twitter/summingbird). In 2021 Twitter wrote: "We have a lambda architecture with both batch and real-time processing pipelines, built within the Summingbird Platform… we propose to build pipelines in kappa architecture to process the events in streaming-only mode… we remove batch components." They then found the stream results more accurate than batch, because "the original TSAR batch pipelines discard late events." (*Processing billions of events in real time at Twitter*, 2021, https://blog.x.com/engineering/en_us/topics/infrastructure/2021/processing-billions-of-events-in-real-time-at-twitter-, INSTITUTIONAL). Summingbird adds nothing else for Sid; I dropped it.

## 4. Which of Sid's questions he speaks to

**(0)** REPORTED: retention is a setting. Forever is fine if you run the log like a database. INSTITUTIONAL (Kafka design docs, https://kafka.apache.org/43/design/design/#log-compaction): compaction rewrites old segments in the background, and yet "Ordering of messages is always maintained. Compaction will never re-order messages, just remove some. The offset for a message never changes. It is the permanent identifier for a position in the log." Note what this is. Kafka and Rama were built apart, and landed on the same rule: **positions are permanent; contents may be removed.** Neither camp holds "never rewritten".

**(9)** INSTITUTIONAL (same Kafka page): "A message with a key and a null payload will be treated as a delete from the log. Such a record is sometimes referred to as a tombstone… delete markers are special in that they will themselves be cleaned out of the log after a period of time." A reader that lags more than the retention of delete markers can miss the delete.

**(10)** REPORTED: order per partition only; one writer per partition; global order is not worth wanting. INFERRED: on "as of", he would say a position per partition, always. One number only when there is one partition.

**(11) When.** REPORTED: order by log position, never by wall clock. Wall-clock time is something "you might include in your messages". He notes the other school: "Spanner—Not everyone loves logical time for their logs. Google's new database tries to use physical time and models the uncertainty of clock drift directly by treating the timestamp as a range."

**(5)** REPORTED: a derived index should know its "index point" and compare it with the position a reader asks for. INFERRED: on "is how far it had really got written down?", yes. It is one number per partition, and the index already has it.

**(1)** INSTITUTIONAL: Kafka names a record by its position. One caution from general knowledge that I did not open a source for: a position is local to the log that gave it. Copying a topic to another cluster gives new offsets. For Sid's later second store this matters. What crosses must carry its own id.

**(7) and the shape of the gate.** REPORTED: the state-machine and primary-backup passage is Sid's fork, stated plainly. Log the offers and let every reader re-run the gate; then the gate must be deterministic, with no clock reads. Or let one gate decide and log its results; then the results are the truth and the gate may read a clock. Sid's design is the second. INFERRED: Kreps would say both work, and the mistake is to mix them by accident: to treat the offers as the log while the gate does things that cannot be replayed.

**ALSO OPEN.** He would push on "one substance for everything" and "one store". The log should be the simple shared thing. Asking it to also be the index for every question is what he calls hubris.

## 5. Resemblance and difference

Resembles: one log as the truth for many readers; years of running it; clear writing on order, time, position, and replay.

Differs: Kafka's records are a transport between a company's systems. Writers are trusted services. No per-record attribution, no admission control, no reads recorded. Kafka does not care what a record means.

---

# The group

## 7. The voices that matter most, who was dropped, who is missing

**Most useful, in this order.**

1. **Rama's reference together with RPL's 2026 agent guidance.** Not a person, but it is the runtime, and its limits are physical. It gives the hardest first-record constraints in this whole report: fixed task count, ids made by the offerer, positions that do not survive a re-partition, stream versus microbatch, no exactly-once depot appends, no auth.
2. **Marz, read as a before and after.** The 2010–2012 fact model is the closest prior art to Sid's fact. What he changed by 2023 (no batch recompute, order by position, doors for deletion and migration, bigger ids) is the most valuable thing in his record, because he lived with the first design at Twitter scale and then built the second.
3. **Kreps's *The Log*.** The clearest writing anywhere in this camp on the four things the table keeps circling: order, time, position, and what can be replayed.

**Dropped.** Summingbird (one line of value, used above). Marz's Storm history and his software-engineering principles post (nothing on these questions). Kleppmann's *Turning the database inside-out* (2015): it belongs with this camp, and has the line "accountants don't use erasers", but it restates Kreps and Marz for these purposes.

**Missing from this group, and worth hearing.** I did not research these; another session may have.
- Rich Hickey and Datomic. The nearest real system to Sid's fact: entity, attribute, value, transaction, added-or-retracted; a single transactor as the gate; transactions that are themselves entities and can carry "by whom" and "why"; excision added later for legal deletion. (This description is from general knowledge; I opened no Datomic source.) Multiply's exit from it is only one side of that story.
- Pat Helland (*Immutability Changes Everything*; *Data on the Outside versus Data on the Inside*). He writes about exactly what may cross a boundary, and about identity and versions.
- Greg Young on versioning event-sourced systems. It is the practitioner literature on never-rewrite with changing schemas: upcasting, weak schema, copy-and-replace.
- Transparency logs and ledgers (Certificate Transparency, Trillian). They are the people whose constraints really are never-rewrite, public audit, and many writers with attribution. This camp's were not.
- The GDPR and event sourcing literature (per-subject keys and key destruction).
- Red Planet Labs directly, on two things the reference is silent about: one cluster spanning continents, and the timing of first-class task scaling.

## 8. What this camp would question above the table

Taking the ALSO OPEN list in order.

**One append-only store of small facts as the one substance.** Marz agrees for the source of truth; it is his DataUnit. He disagrees that the same store should answer the questions: "There's a fundamental tension between being a source of truth versus being an indexed store that answers queries quickly." One substance for truth, many shapes for answers. Multiply's story is that tension lived. There is also a physical form of it in Rama: "Topologies read all data appended to a depot. So if a topology doesn't need certain data being appended, it must filter out that data at the beginning of processing" (`docs/14-depots.md:123`). One depot for everything means every tool reads every fact. Marz's own raw store was split by kind so that jobs read only what they need. The camp would say: match, don't route, is the right rule for meaning, and it still needs routing by kind at the storage level to be affordable.

**Tools, grammars, policies and definitions as facts in the same store.** This camp keeps schemas and functions in code, one version live at a time: "You'll never have to deal with situations where there are multiple versions of a schema active at the same time." They would ask three things. If a grammar is a fact and old facts are never rewritten, must every reader understand every grammar version forever? Where do grammar and policy facts sit relative to the gate's partition, given that a hop ends the transaction? And when a policy fact is wrong, which is a human fault, what is the recovery path? In their world it is: fix the code, recompute. In Sid's it is: add facts that correct, and let doubt spread along based-on. That is coherent. They would want to see it run.

**One store for the planet, with personal layers.** The reference says nothing about a worldwide cluster. RPL's model has been a cluster per customer. The task count is fixed at launch, so growth to planet scale passes through re-partition events that change every log position. A microbatch gate makes admission depend on every task group at once. And Kreps's point removes one hoped-for benefit: a partitioned log has no global order anyway. What one store really buys is one gate policy and one id space. The camp would ask whether that is worth one failure domain.

**A fixed nine-part envelope, shared forever.** Marz fixes the evolution rule, not the field list: add optional fields, never reuse an id, keep the power to migrate. He would call "forever" safe only if adding is allowed.

**Running answers never stored.** His whole career argues the other way: "The most obvious alternative approach is to precompute the query function." Views are safe to store because they can be recomputed. He would say store them, as PStates, and never let them be truth. That may already be what Sid means, since a PState is not a fact. On recording crossings he has no position beyond Agent-o-rama's traces of what went to a model.

**Every read recorded on every fact.** Neither man has written about it. Kreps's determinism principle supplies the best argument for it: if a tool's output is a deterministic function of its reads, then the reads are all you need to re-derive it or to know it is stale. They would ask about cost. Reads outnumber writes. If every fact carries its reads, provenance may become most of the store. Marz's one real system in this area keeps it in traces beside the data.

**One writer.** Full agreement from both. But both mean one writer per partition. Neither means one writer for the world.

## 9. Questions this camp would call the wrong question

- **(0) "Never rewritten, or only never lost?"** They would say neither phrase names the property that matters. The property is: a later write never destroys an earlier fact; positions never move; derived things can always be re-derived. Byte-level permanence is not on their list.
- **"Which conventions must be fixed before the first record?"** They would split it in two. Representation can be migrated later; Rama ships the tool. Information that was not captured cannot be recovered by any migration. So the question becomes: what information will Sid wish had been captured? And which physical choices can Rama not undo? By that test, (3) word-or-id, (13) maps-or-classes, the id format in (2), and the envelope's exact shape are not first-record decisions if the migration door stays open. The first-record decisions are: who, acting for whom, based on what, because of what, under which code, at what time by whose clock, under which offer id; and in Rama, the task count, the partition key, the names, and ids inside the record.
- **(11) "Whose clock?"** Wrong if the clock decides order. Right if the clock is data. Then the answer is to write down whose clock it was.
- **(6) "Is 'replacing 25' kept on 37?"** Inside one store they would call it a copy of what the cell's order already says. Here I part from the camp: across two stores, order is not shared and the explicit pointer is the only thing that survives, as parent pointers do in git. The camp never had a second store.
- **(7) "Are refusals kept?"** Not wrong, but they would turn it around. The offer is the raw fact. The verdict is the gate's opinion. Keep both, always, because the gate is code, and code is the thing that will be wrong.

## The strongest challenge this camp makes to the premise

The premise: because the store never rewrites, whatever is not written into a fact when it is made is gone for every earlier fact.

The second half is true with or without rewriting. Information nobody captured is gone. The first half is a choice that none of the three sources makes. Marz: "When you change your schema, you'll have the capability to update all data to the new schema." Rama: depot migrations, offsets unchanged. Kafka: "The offset for a message never changes", and the contents may go. All three hold positions and information fixed, and let representation move. If Sid keeps that door, much of the table stops being a first-record decision, and the list that remains is short and is about information.

The honest limit of this challenge: every one of these people trusted the operator. One company owned the log. Sid's store has many writers, attribution, and an economy. There, the operator's power to rewrite the past is a trust problem, not only an engineering convenience. This camp never faced that and has nothing to say about it. If "never rewrite" is there to make the past provable to people who do not trust the operator, then it is doing work this camp's systems never had to do, and the people to hear next are the ones who build transparency logs and ledgers.

---

# Sources

Opened and read in this session (all quotes above were checked against the saved page text):

- Rama reference, vendored: `reference/rama/docs/` (extraction dated 2026-03-22) and `.claude/skills/rama/` (rama-ai-learn @ 0b46060, 2026-08-05). Live docs re-checked 2026-09-20: depots, microbatch, operating-rama, downloads (Rama 1.9.0).
- Nathan Marz, *How to beat the CAP theorem*, 2011-10-13. http://nathanmarz.com/blog/how-to-beat-the-cap-theorem.html
- Nathan Marz, *Thrift + Graphs = Strong, flexible schemas on Hadoop*, 2010-03-10. http://nathanmarz.com/blog/thrift-graphs-strong-flexible-schemas-on-hadoop.html
- Nathan Marz and James Warren, *Big Data*, Manning 2015, chapter 1 only (Manning's free sample; MEAP v7 mirror at http://www.odbms.org/wp-content/uploads/2014/02/BD_meap_ch01.pdf).
- Nathan Marz, Hacker News comments, 270 collected through the Algolia API (user `nathanmarz`, 2009–2025). Item ids are given inline.
- Nathan Marz, *Everything wrong with databases and why their complexity is now unnecessary*, 2024-01-09. URL inline.
- Nathan Marz, *Massively scalable collaborative text editor backend with Rama in 120 LOC*, 2025-04-01. URL inline.
- Nathan Marz, *Rama, the 100x developer platform, is now free for production use*, 2025-03-18. URL inline.
- Nathan Marz, *Introducing Agent-o-rama*, 2025-11-03, and the Agent-o-rama README. URLs inline.
- Nathan Marz, Software Engineering Daily episode 1600, 2023-12-28, transcript. URL inline.
- Red Planet Labs (Sam Adams), *Migrating terabytes of data instantly*, 2024-09-30. URL inline.
- Red Planet Labs, *How Multiply went from Datomic to XTDB to Rama*, 2025-03-04. URL inline.
- Red Planet Labs, *How we reduced the cost of building Twitter at Twitter-scale by 100x*, 2023-08-15 (skimmed for ids, timestamps, retry modes only).
- Jay Kreps, *The Log*, 2013-12-16 (via Wayback). *Questioning the Lambda Architecture*, 2014-07-02. *Why local state is a fundamental primitive in stream processing*, 2014-07-31. *It's Okay To Store Data In Apache Kafka*, 2017-09-15. URLs inline.
- Apache Kafka design documentation, log compaction section (v4.3). URL inline.
- Twitter Engineering, *Processing billions of events in real time at Twitter*, 2021-10-22. twitter/summingbird README. URLs inline.
- Martin Kleppmann, *Turning the database inside-out with Apache Samza*, 2015 (opened; one line used).

Could not open, and what that means:

- ***Big Data* chapters 2 and 3.** Manning's liveBook is script-rendered, O'Reilly's copy returned 403, and the only full copies found were unauthorized mirrors, which I did not use. Chapter 2 is where the fact-based model, identifiability, and the three cases for deletion are laid out in full. For it I relied on Marz's own one-paragraph summary (HN 3449142), the 2010 post it grew from, the 2011 essay, and chapter 1. Two statements rest on my memory of chapter 2 and are marked so in the text: the 64-bit nonce, and identity resolution through "same as" facts.
- Marz's talks (Strange Loop 2012 *Runaway complexity in Big Data*, the InfoQ interview of 2014): audio and slides only, no transcript. SlideShare blocked the fetch.
- The rama-user Google Group thread on end-to-end encryption that Marz links (https://groups.google.com/u/1/g/rama-user/c/jj-ILcoMjtk): not fetched.
- A written reply by Marz to Kreps's critique: searched for, not found.

Raw page captures sit in this session's scratch folder and will not survive a restart. Every load-bearing passage is quoted above with its URL so the captures are not needed.

---

# Round two

Written 2026-09-20, after round one, which stays as it was. The orchestrator sent the current leans of softland-ff. They are leans, not Sid's rulings. My job here is to press them from this camp's side: reject, sharpen, or call the wrong question, each with the one case that decides it.

Markers as before. For the Rama reference: **CHECKED** (file and line), **IMPLIED** (my inference), **NOT IN THE REFERENCE**. For people: **R** reported with source, **I** inferred by me, **N** institutional (RPL's docs and agent guidance, Kafka's docs). Round-one quotes are reused without repeating their URLs; new ones carry theirs.

One correction to my own round one, left in place above and corrected here. Under (2) I said RPL prefers UUIDv7 for disk locality. That was my inference. The reference gives a different reason, sort order. See T3.

## T1. Which log

Two shapes. Both are buildable in Rama. They give "never rewritten" different meanings.

### Shape (a): the offers depot is the permanent record; facts are PState rows declared primary

What it is in Rama. The natural shape. Clients append offers. One gate topology owns the fact rows (**CHECKED** `docs/15-pstates.md:41`). The gate's stamp, version and verdict are data in those rows.

What "never rewritten" then covers. Only the offers depot, and only by Sid's discipline, since Rama itself offers migration and trimming (round one, (0)). That depot holds every offer: admitted, refused, malformed, and client re-sends. An append that threw and was sent again is two records. The fact rows are mutable storage. "The gate only ever adds rows" is a rule in gate code. Rama does not enforce it, and PState migrations exist. So under (a), lean (0) protects the thing that is not the facts.

What lean (11) does to it. "The gate's clock only" makes the gate non-deterministic in Kreps's exact sense (R: "a call to gettimeofday or some other non-repeatable thing"). The rows can then not be re-derived from the depot. RPL says the same in writing (N): "If your existing PStates have data that was non-deterministically generated, you might find that you need to describe your change in terms of existing views rather than in terms of your depot records."

Replay. Running the offers through the gate again gives the same winners and the same versions, if per-cell order holds and if the gate's reads of grammar and policy are local and ordered (T4). It gives a different "when". It can give different verdicts wherever a grammar or policy reached the gate's task by another partition's timing. So replay is not a rebuild.

Restore. Consistent: "backups guarantee that depots contain all entries that may have affected any PStates" (**CHECKED** `docs/22-backups.md:34`). Everything after the backup point is gone. Depot offsets are reused (**CHECKED** `:106`). **IMPLIED:** so are the gate's numbers, because its counters are PState data and go back to old values. A fact that a screen showed yesterday as version 37 may be a different fact after a restore. This is the case that decides lean (1): name a fact by its own id.

Re-partition. Rama cannot add tasks (**CHECKED**, round one). The documented way out has two branches. Recompute in a new module: "This approach only works if your processing is deterministic, which may not be the case if your processing makes use of any random numbers (such as UUIDs)." Or: "An alternative approach is to repartition the PStates directly by making a special topology in the new module to iterate through the original PStates and repartition them to the new module's PStates." (**CHECKED** `docs/19-operating-rama.md:489`). Under (a) with a clock-reading gate only the second branch is open. It works, because the rows carry their stamps as data. It is a full physical rewrite of the facts, with "significant downtime" (`:491`).

What a second store reads. Offers, by (partition, offset). Offers without verdicts. To learn what landed it must also query the fact rows, and PStates are not a feed: topologies source depots only ("All new data enters Rama via depots, and topologies source all incoming data from them", **CHECKED** `docs/14-depots.md:7`), and a reactive `proxy` follows one path, not a log (**CHECKED** `docs/15-pstates.md:436-438`). The same gap exists inside the store. "A landed fact is matched to tools" needs a feed of landed facts. Under (a) the only place that can react to a landing is the gate topology itself.

Against the leans. (7) fits: verdict and fact in one event on one task. (13) "never trimmed" makes every malformed offer permanent.

### Shape (b): the gate decides, commits, then publishes the admitted fact to a facts depot; consumers dedupe by fact id

What it is in Rama. A depot declared `:disallow`, appended from the gate with `depot-partition-append!`. N: "ALWAYS add a commit boundary before `depot-partition-append!` to an internal depot in a stream topology." (**CHECKED** `skill/stream.md:49`). The append is at-least-once (**CHECKED** `docs/12-microbatch-topologies.md:130`, `skill/depot-design.md:68`). RPL names the use: "a module may be publishing an event stream based on other depots meant for consumption by other modules" (**CHECKED** `docs/14-depots.md:39`). **IMPLIED:** the retry path must publish too. When a replayed offer is found already decided, the gate appends the stored fact again, byte for byte, because the first append may never have happened.

What "never rewritten" then covers. The facts depot: an ordered, replicated log of the gate's results. This is Kreps's primary-backup log (R): the leader decides and logs out what it decided. The offers depot becomes intake. The camp would trim it once verdicts are durable. If refusals are to be kept forever (lean 7), they must be published too.

What lean (11) does to it. Nothing. The clock is read once, stored with the decision, then published. Logging results is exactly how a non-deterministic leader stays replayable.

Replay. Every derived index can be rebuilt from the facts depot alone, with dedupe (first record per fact id; highest version per cell). So can the gate's own state. Marz's invariant holds again (R): "In Rama, a PState can always be recomputed from the depot data, which is the source of truth."

Restore. The same loss after the backup point. Ids protect names, as under (a).

Re-partition. RPL's first recipe works as documented, because a consumer of a results log is deterministic.

What a second store reads. The facts depot, through an `ExternalDepot`-shaped bridge, by (partition, offset). It dedupes by fact id and re-admits through its own gate as ingests.

Costs. Each fact is stored three times (offer, row, published record), times the replication factor. Duplicates in a never-rewritten log are permanent. They are harmless (Marz R: "'A and A' is the same as 'A'"). The published record lags the committed row by one event, so "no optimism" needs one sentence saying which of the two is "landed". Either choice is coherent.

### Which would Marz pick

Neither as stated. **R:** "since PStates are not the source of truth – the depots (event logs) are – mistakes can be corrected via recompute from the source of truth." **R:** his records carry their own time, put there before the append: "The appended records would have three keys: userId, location, and timestamp." **R:** his version is the length of the cell's list (collaborative editor post). **I:** he would take (a), and move the clock read in front of the append, so the gate is a pure function of depot order and the fact rows stay views. That contradicts lean (11) unless the door counts as part of the gate. **N:** he publishes derived depots when another module needs a feed.

Kreps (R) names both shapes as legitimate. **I:** he would say the error is mixing them by accident.

My read, marked as mine. With grammar and policy as facts that the gate reads (T4), a fully deterministic gate is hard to keep for years. Shape (b) does not need one. The shape to avoid is (a) with a clock-reading gate. There the never-rewritten log cannot rebuild the facts, and the facts live only in mutable rows.

## T2. Content ids against the migration door

Does the conflict remain if the id is computed once and then carried as a stored name that nobody re-verifies? **IMPLIED:** no. A migration may change every other byte, and the id field is copied through. It is then a name.

What is lost when it is only a name.

1. Nobody can check that the content matches the id. A second store cannot use the id to trust content or to notice tampering.
2. Lean (17) wants seed ids that "every store derives". Deriving is re-computing. So the seed facts are exactly where name-only does not work. The second store must produce the same bytes and the same hash in year five. The canonical encoding and the hash function then become the frozen thing, a choice of representation that can never migrate. Rama's own serializer cannot be that form. It is deterministic for built-in types (**CHECKED** `docs/17-serialization.md:167`), but it is internal to Rama, and clients are locked to the cluster's version (**CHECKED** `skill/operate.md:14`). Whether its bytes are stable across Rama versions: **NOT IN THE REFERENCE**.
3. With a random salt (lean 1), nobody without the salt can recompute. For ordinary facts the hash then buys one thing: someone who holds salt and content can later prove the id was minted for exactly that content. If nobody will ever ask for that proof, it is a random id with extra steps. RPL's retry reason is served equally by a random id (N).

Who in this camp lives this way: nobody. Marz used natural ids (R, 2010), then random ones (book, from memory), and RPL uses UUIDv7 (N). Kafka uses positions. Content addressing belongs to the git, IPFS and transparency-log people. They are missing from this camp.

The one case. The second institution stands up its store in year five and derives the seed ids. In between, Sid's side changed a serializer or a map-ordering rule. The ids no longer match, and nothing can be rewritten. Two ways out, both mine (I): fix a canonical encoding with test vectors before the first seed fact; or drop derivation and ship the seed ids as a list of constants with the floor, which needs no agreement on hashing at all.

A side note, mine and not from this camp. Lean (9) keeps the value's hash after the key is destroyed. For small values (a name, a yes or no, a number) an unsalted hash gives the value back to anyone who guesses.

## T3. UUIDv7 against "no time inside the id"

Why RPL picks v7. **CHECKED** `skill/unique-ids.md:15`: "UUID7 is time-ordered — IDs sort chronologically, which is useful for range queries on subindexed maps and preserves insertion order." And `:19`: "Range queries and cursor pagination over UUID7-keyed subindexed structures are therefore time-ordered." The stated reason is sort order. Disk locality is not stated anywhere. I withdraw it as RPL's reason.

Does that reason apply to an entity id that lasts for life? Only where something walks a map keyed by entity id and wants creation order. An entity id is mostly used for point lookup and as the partition key. "Newest entities first" can be served by a separate index keyed by the gate's stamp. The reason is strong for ids of things that get listed by time. Under lean (1) those ids come from content and salt, and time order within a cell comes from the gate's version. So nothing in the leans needs v7. Lean (2) stands.

What a fully random 128-bit id costs in Rama. Partition balance: nothing, since the partitioner hashes either kind (**CHECKED** `skill/pstate-schema.md:52`). Reading everything about one entity: nothing, if its facts nest under the entity key, because a subindexed structure is contiguous (**CHECKED**, subindexing). Lost: time-ordered scans over ids, so one more index and one more write per entity. Unknown: extra write work in RocksDB from random top-level keys. **NOT IN THE REFERENCE**, and I did not source it elsewhere.

The hot entity under lean (10). **CHECKED** `skill/pstate-schema.md:54`: "a few keys take far more events or data than the rest; all of a hot key's writes land on its one task, making it a hotspot. Hash balances *keys*, not *load*." RPL's remedy (`:69`): "For **skewed** data, do NOT pick the cheapest-looking partitioner off this menu and settle. Derive the `f` the dominant read wants — e.g. *how many tasks should one key's data span as a function of its size?* — and implement it with `|direct`." Placement "can also be stored state, recorded at write time and read back before routing" (`:50`). Marz lived this (R, Mastodon post): "a naive implementation that handles all fanout for a single user from a single partition will lead to some partitions of a module having a lot more overall work to do than others."

The consequence. Once a hot entity's data spans several tasks, the entity no longer has one writer. Only a cell does. Sid's compare-and-set is on the cell (entity, key, layer). Lean (10) promises more than that: partition by entity.

The one case. Hundreds of people and their agents on one problem. The problem's root entity, or one shared canvas entity, takes machine-rate writes from thousands of actors. That is one task thread. The stream throttle trips, and then "Depot appends with AckLevel.ACK while the limit is hit will throw an exception back to clients" (**CHECKED** `docs/11-stream-topologies.md:297`), for every entity that hashes to that task, not only the hot one.

Sharpen (I). Promise order per cell. Deliver per-entity co-location as the default placement. Keep the right to spread a hot entity by cell. If tools come to rely on a whole entity committing as one, that right is gone. Note also that the depot partitioner runs on the appending client (**CHECKED** `skill/depot-design.md:132`), so the door has to know the placement table.

## T4. The gate's three checks under a stream gate

The version check is local by construction: the cell lives on the task where the event runs. Shape and policy need reads. The Rama-native options:

1. **Broadcast small data to every task.** RPL's named pattern (N). "A topology uses `|all` to replicate data to every task. Every task gets a full copy — useful for lookup tables or configuration." (**CHECKED** `skill/patterns.md:163`). And: "The right `f` for small, rarely-written data read locally everywhere (e.g. config and lookup tables) — each task keeps its own copy, so reads are local with no cross-task hop. Not for large or frequently-written data — every task pays every write." (**CHECKED** `skill/pstate-schema.md:55`). The copies are stale by design. Tasks receive a new grammar at slightly different moments, and even under microbatch "external readers can observe two tasks on different microbatches at the same moment" (`skill/microbatch.md:128`). So "a stale local read whose versions the verdict records" is not a separate option. It is what this option is. The verdict must name the grammar and policy versions it checked. The brief already says the gate's yes or no names what it checked.
2. **Hop and come back.** `select>` to the owning partition, then `|hash` back to the cell's task, then the compare-and-set. The version check and the write still share one event, so that part stays atomic. The grammar or policy can move in between. Two network hops per offer, and a retry redoes them. RPL's standing rule (N, `.claude/skills/rama/SKILL.md`): "Never trade I/O efficiency for code simplicity", and "Partitioner calls in topologies also add network latency."
3. **Co-locate by choice of partition key**, so the policy sits where the offer lands. For a personal layer that means partitioning by layer. It conflicts with lean (10), and it turns "nearest layer wins" into a read across partitions.
4. **A microbatch gate.** Reads across partitions still hop, but the whole batch is one transaction. The costs are in round one.
5. **Check policy at the door**, before the append. Marz (R): permissions are "redundant checks" put in front of the events store. The gate then checks shape and version only.
6. **Keep grammars in module code.** The camp's own default (Marz R: one schema version live at a time). Not facts. It contradicts Sid's design. It is listed because it is what these people actually do.

Which does RPL's practice favour? Option 1 for anything small and rarely written. Co-location by partition key for anything large (N, `SKILL.md`, "Colocate related data").

What that means for the leans. Grammars, and the few policies on shared and base layers, fit option 1. Per-person and per-agent grants do not. With tens of agents per person across a planet they are neither small nor rarely written, and "every task pays every write". Under partition-by-entity, a personal layer's facts are spread over every task. So a personal layer's policy can be local to every task, or small, but not both. **I:** the gate cannot check personal grants cheaply under lean (10). They are checked at the door (option 5), or the ownership rule is made structural, so that the layer id itself says who owns it, and the rule that an owner may write their own layer needs no read. Either way the verdict should say who checked what.

## T5. One cluster worldwide

**Known** (all CHECKED unless marked).

1. The leader does everything, including reads: "The leader is responsible for performing all work for each of the tasks in its task group: running ETL code, updating PStates, appending to depots, and serving PState client queries." (`docs/21-replication.md:30`). Followers only apply. More replicas do not bring reads closer to anyone.
2. A write is visible only after every in-sync replica has it on disk (`:41`; `docs/23-acid-semantics.md:112`). Min-ISR defaults to the replication factor minus one (`:71`).
3. Replicas of a task group go on different nodes (`:24`). Zones or regions: NOT IN THE REFERENCE.
4. One Conductor per cluster. Zookeeper holds leader election and ISR membership (`:152-161`). The Zookeeper session timeout defaults to 5000 ms (`docs/26-all-configs.md:23`).
5. One stalled task group fails appends for its partitions and halts every microbatch topology in the module (`:79`).
6. A module update pauses appends for 2 to 30 seconds, for the whole module at once (`docs/19-operating-rama.md:441`).
7. Labels can pin a module to labelled nodes (`docs/20-heterogenous-clusters.md`). That is per module, not per partition.
8. Clients must match the cluster's major and minor version (`skill/operate.md:14`).
9. The task count is fixed at launch.
10. Online incremental backup needs a paid licence. The free version runs two nodes (R).
11. R, Marz 2023: "each user will just have their own cluster", meaning each Rama customer.

**Unknown.** Everything about wide-area behaviour. The reference never mentions it.

**The exact questions for Red Planet Labs.**

1. Has any Rama cluster run with nodes in more than one region? What round-trip time between replicas has been tested?
2. Can replica placement be made aware of zone or region per task group: leader here, followers there? Can leadership be pinned or preferred by label?
3. Can followers serve reads, stale allowed, with a stated position? If not, is it planned?
4. When one region is cut off, does every microbatch topology in the module stop, as the replication page implies?
5. Which timeouts assume a local network (the `replication.*` options, the 5-second Zookeeper session, `topology.stream.timeout.seconds`)? What values are safe at 150 to 300 ms round trip?
6. Is there, or will there be, supported replication or mirroring of a depot between clusters? Are offsets preserved?
7. First-class task scaling: when? Will partition indexes and offsets be preserved, remapped, or translated?
8. Exactly-once depot appends from microbatch are "on our roadmap". When? Anything planned for stream topologies?
9. Can topology code read a record's depot offset and append time? That would give a native position for cuts and a native "when".
10. Can a module update roll region by region, or reach zero downtime for appends?
11. What record size is practical in a depot? Any guidance for records of tens of kilobytes (long read lists)?
12. Is the built-in serialization byte-stable across Rama versions, so that it could be hashed?
13. Licence terms for one cluster of thousands of nodes run as a public service. Does the client version lock mean a front tier is assumed?

## A. The leans, one by one

Leans where this camp has nothing sourced are marked so and kept to a line.

**(0) Never rewritten. REJECT as stated.** Marz (R), Rama (N) and Kafka (N) all keep a door (round one). The deciding case: the thing that must go is not in the value slot. A token inside an external anchor in based-on. A private name inside the word of a key. A person's id in the by-whom of a refusal. Lean (9) lets only the value, and maybe by-whom, ever go. Under never-rewritten everything else stays in every replica and every backup. The camp keeps the operator's excision: the slot stays, the content goes, positions do not move. **I:** it would make each excision an audited act, itself a fact. Also sharpen: say what (0) covers. Under T1 it is a depot, and which depot matters.

**(2) Entity id. STANDS.** RPL (N): made by the offerer, 128 bits. Who already lives this way: RPL's own guidance; Marz in 2010, whose ids came from the source (R). Sharpen the ingest-derived id: it names the source record, not the thing. The case: ten million papers, and the same paper arrives as a DOI record, an arXiv record and a PubMed record. Three entities, for life. Marz's id unions listed several kinds of id for one node (R) and settled sameness by computation (from memory of the book, unverified). An id for life cannot be merged later. So "same as" has to be a key from the first day.

**(17) First facts.** See T2.

**(3) Key is an id. STANDS.** Thrift field numbers (R, 2010). RPL's explicit type ids (N). The cost: the gate resolves key id to grammar on every offer. T4, option 1.

**(9) Delete by destroying a key. SHARPEN.** Three points from the reference.
- The key store is itself stored. If it is a PState, it is in every backup until backup GC removes that backup (**CHECKED** `docs/22-backups.md:112-114`), and a restore brings PStates back as they were. "This reaches backups" is true of values and false of keys. The one case: in year three the module is restored to last night's backup, and every key destroyed since then is back. So the key store needs its own, shorter backup life, and deletions have to be re-applied after any restore. **I:** the deletion facts are what make that possible.
- The gate has to see plaintext to check shape against the grammar. Under T1(a) the offers depot is permanent, so the offer must already be encrypted when it is appended. So encryption happens at the door, and the gate needs the key on every admission. Per-value keys cannot be broadcast (T4). They have to sit with the cell.
- If the key sits in the same row as the fact, deleting it is an ordinary delete in mutable storage, which PStates allow anyway (**CHECKED** `docs/15-pstates.md:181`). The cryptography is needed for the depot copy and the backups, not for the row.

Marz (R) has said only that such encryption is "pretty easy to implement on top of Rama's existing primitives". Nobody in this camp has written about running it.

**(8) By whom. STANDS, with one sharpening.** Marz (R): permissions belong in front of the events store. Rama has no authentication (NOT IN THE REFERENCE), so a door is forced anyway. Sharpen (I): what the door checked can only be known at that moment. Record the door's build and its method on the verdict. The case: in year four a door bug accepts any by-whom from agent hosts for two weeks. Which facts are suspect? That is answerable only if each verdict names the door build that vouched for it. On agents as their own actors and on grants: nothing sourced. The Rama angle is in T4: grants are many, so the door checks them.

**(11) When, based on, because of.**
- When: STANDS. Kreps (R): position orders; a wall clock is data. Sharpen: under a stream gate a retry reads the clock again, so the first stamp must be looked up by offer id (ties to 7). Record which task stamped it (`Ops.CURRENT_TASK_ID`, **CHECKED** `docs/11-stream-topologies.md:46`) and which module instance (`ops/module-instance-info`, **CHECKED** `skill/pstate-schema.md:62`). Marz (R) also carries the source's own time in the record. It cannot be added later.
- Every read listed, no grace: nothing sourced on the rule itself. On where reads live, Marz (R) keeps them in traces beside the data. The case: one agent fact that stands on ten thousand point reads. The offer is then mostly reads, in a system whose own rule of thumb treats about 50 KB as one fetch (**CHECKED** `docs/14-depots.md:242`). Sharpen (I): write the read list once as its own record, and let the fact point at it.
- Because of: nothing sourced.

**(16) Visibility. SHARPEN.** In Rama "private" is the door's promise, not the store's. Marz (R): "Rama PStates are globally readable". **CHECKED** `docs/03a-distributed-programming.md:61`: "A running event has access to all depot and PState partitions on that task." Any module on the cluster may mirror any other's (`docs/18-module-dependencies.md:7-9`). The one case: tool bodies. Tools are facts with a body, and a landed fact is matched to them. If a body written by anyone but the floor runs inside the cluster, as topology code or as a module, it can read every private layer on its task. So such bodies run outside the cluster, behind the door, or private layers are not private. The first-record consequence: layer has to be in the key path of every index, so the door can filter reads by layer cheaply.

**(10) Order. SHARPEN.** Kreps (R) agrees that a position per partition is the honest form. Two cases press on "never one number".
- The first re-partition. A position belongs to one partition of one layout. Rama grows by copying into a new layout. Cuts already written onto never-rewritten facts then point at a layout that no longer exists. Sharpen: every position carries a layout epoch from the first day.
- Size. A pattern read over all partitions yields a cut with one entry per task. It is written on every fact it supports, with no grace. At 1,024 tasks that is about 8 KB per pattern-read entry. Sharpen (I): write the cut once as its own record and point at it.
- Under a microbatch gate one global number exists (**CHECKED**, round one) and survives a re-partition. "Never one number" gives up Rama's only native global cut. Keep the slot able to hold either form.
- The hot entity: T3.

**(4) Depends-on marks.** Nothing sourced. One line from Kreps's determinism (R): for a deterministic tool every read is depends-on by construction, so the mark carries information only for people and models.

**(5) How far the index had got. STANDS.** Kreps (R): the "index point". Kafka lives this way. In Rama the marker has to be built (**CHECKED** `skill/testing.md:157-165`).

**(1) Name a fact by its own id. STANDS.** RPL (N). The deciding case is the restore in T1. Deriving the id from content: T2.

**(6) Keep "replacing 25". STANDS at no cost.** Under either shape in T1 the offer already carries "expects 25", for good, in a depot. The camp adds nothing beyond round one, section 9.

**(7) The gate's yes or no. SHARPEN.** The verdict index has to sit on the target cell's partition, keyed by offer id, written in the same event as the fact (**CHECKED** `docs/23-acid-semantics.md:41`, `skill/stream.md:13`). "Refusals in the offerer's session layer" can be a label. It cannot be a location. The case: an offer replayed after its commit finds its own version current and refuses itself, unless the lookup is on the same task. On "forever": Marz (R): "Garbage collection gets rid of data that is of low value." Kreps (R): event data gets a window. The case: hundreds of people and agents racing on one hot cell produce N minus one refusals for each fact, each with full based-on, kept forever. The camp would let refusals age out after a horizon. Lean (0) forbids that.

**(12) Session start. SHARPEN.** Stamp the gate's module instance on every verdict, not only at session start. The case: a module update lands in the middle of a session (**CHECKED**, 2 to 30 seconds, `docs/19-operating-rama.md:441`). The session-start fact cannot say which gate build admitted fact number 5,001. Marz (I), from human-fault tolerance: after a bad deploy you must find what the bad code wrote.

**(14) The hand. REJECT putting hand ticks, as facts, into the never-trimmed log.** Kreps (R, *The Log*): "For event data, Kafka supports just retaining a window of data." Marz (R): garbage-collect low-value data. Rama (N): trimming and other depot options are set per depot (**CHECKED** `docs/14-depots.md:316-327`, `docs/19-operating-rama.md:354-356`). The case: 300 people on one problem, pan and zoom ticks at 10 per second, 8 hours. That is 86.4 million facts a day for one problem. Each carries because-of and based-on, is replicated three times, is never trimmed, and competes with real writes for the stream gate's per-task budget (**CHECKED** `docs/11-stream-topologies.md:295`). The camp would give the hand its own depot, its own retention, and a microbatch topology. One model, different storage promises.

**(15) A click.** Nothing sourced.

**(13) Storage. SHARPEN.** Plain maps stand, with Marz's caveat (R, 2010): "JSON doesn't give you a real schema… A good schema… gives you errors at the time of creating a bad object." In Rama a depot accepts any object. The partitioner runs on the client, and "A throw propagates to the caller of `foreign-append!` rather than becoming a rejection the topology can observe" (**CHECKED** `skill/depot-design.md:132-134`). The case: a buggy agent host appends a hundred million malformed offers overnight. Under never-trimmed and never-rewritten they are permanent, and every replay from the beginning reads them. So the door checks the envelope before the append, and "never trimmed" is scoped to facts, not intake (T1, shape b). Backups: online backup is a paid feature; and see (9) for keys.

## B. Which leans this camp would call the wrong question

1. **The rider on lean (0): "this makes (9) and (10) now-or-never."** They are now-or-never anyway. (10) because the task count and partition key are physical in Rama. (9) because encryption changes the bytes of the very first value. Neither depends on (0). What (0) really makes now-or-never is every choice of representation. That is the cost to weigh, and the rider hides it.
2. **Lean (10), "a position per partition, never one number."** Ask instead: what names a cut so that it survives a re-partition and is small enough to write on every fact?
3. **Leans (1) and (17), ids computed from content.** Ask instead: who will ever re-verify, and from which bytes? If nobody, use a random id. If somebody, the canonical bytes are the thing to freeze.
4. **Lean (14), which motions become facts.** Ask instead: which kinds of record share the permanent log's promises?
5. **Lean (7), where refusals are kept.** The intake depot already keeps them. Ask instead: is the verdict index a fact, or a view over offers?
6. **Lean (11), "the gate's clock only."** Ask first which log (T1). Under shape (a) this one lean decides whether the facts can ever be rebuilt.

## C. What the leans change above the table

Round one's section 8 stands. The leans add five things.

1. **One model, several storage promises.** Taken together, leans (0), (7), (11), (13) and (14) send the highest-volume, lowest-value records (hand ticks, refusals, reads that only made a tool fire) into a never-trimmed, never-rewritten log with full provenance. Marz's garbage collection (R) and Kafka's windows (R) exist for exactly that class of record. The camp would keep one kind of fact and let retention differ by kind, at the level of depots.
2. **A second thing that is not made of facts.** Lean (9) creates a key store. It is mutable. It must be deletable. It is the most sensitive state in the system, and the gate reads it on every admission. "The one thing not made of facts is the runtime" stops being true.
3. **The door is part of the runtime.** Lean (8), plus Rama having no authentication and locking clients to its version, makes the door a required tier and in practice the only client. Its build belongs on the record next to the gate's.
4. **"One writer" needs an object.** One writer of what? Under shape (b) the gate writes rows and publishes the log. Under shape (a) clients write the permanent log and the gate writes only rows.
5. **Tool bodies and privacy.** "Tools are facts" needs a sentence about where bodies run. Inside the cluster there is no private layer (lean 16).

## Resembles and differs, carried forward

Unchanged from round one. For these leans the camp's sourced material is strongest on (0), (7), (9) as it touches backups, (10), (13) and (14). It is thin or absent on (4), (15), and the acts-for half of (8). Every one of these people trusted the operator. The arguments here for keeping a rewrite door inherit that limit.

## Missing, and what I could not source in round two

- People who live with content-derived ids (git, IPFS, transparency logs). T2 is argued from Rama's side only.
- Practice on key-destruction erasure: key stores, salted hashes, restores. The reference is silent. Marz has one sentence. I opened nothing else.
- RocksDB behaviour under random keys. Not sourced.
- Anything on Rama over a wide-area network. The thirteen questions in T5 are the route.
- A written position from Marz on recording reads, on grants, or on clicks. Searched in round one. None found.

---

# Round three, line 1

The orchestrator's question: what is the unit of total order, the entity or the layer? One exchange with the Datomic camp (research-2). Written from what was already in my context, under a tight budget. No new source was opened. Markers as before.

## 1. Their best reply, made as strong as Rama allows

A layer can be a unit of order in Rama. Nothing forbids it.

- **CHECKED** `docs/14-depots.md:35-58`: a depot's partitioner is any function of the record. `Depot.hashBy` takes an extraction function. A custom `Depot.Partitioning` receives the record and the partition count. Hashing on the layer slot is one line.
- **CHECKED** `docs/23-acid-semantics.md:27,41`: a task is single-threaded, and all PState writes in one event on one task land together. With the layer as the key, a layer has one writer and one order. Its "as of" is one number. A saying of 200 facts in one layer, sent as one offer record, is checked and written in one event. It lands whole or not at all. Their case, "fact 117 fails; 199 stand", cannot happen.
- **CHECKED** `docs/14-depots.md:90`: when the depot and the PState are placed the same way, the gate needs no hop.
- It also repairs a hole in my own T4. A private layer's policy and grants would sit on the layer's task. The gate's policy check becomes a local read.
- RPL's own practice is on their side. N, `.claude/skills/rama/SKILL.md`: design placement "around the application's core queries, not just the top-level key". R, Marz's Mastodon post, about a map keyed by status id: "It is not partitioned by the status ID, which is the key of the map. It is instead partitioned by the account ID of the user who posted that status, which is not even in the map!" He places by owner, not by item. A private layer is an owner.

And one concession I owe them. I cannot answer their second case with "a microbatch gate for big sayings, a stream gate for small ones". **CHECKED** `docs/15-pstates.md:41`: a PState is written by one topology only, and a topology is either stream or microbatch. So one fact store has one kind of gate. Under entity placement and a stream gate, a saying that touches many entities cannot land atomically. They are right about that.

## 2. My rejoinder: what breaks

- **A team layer.** Hundreds of people and their agents at machine rate, all in one layer, is one task and one thread. **CHECKED** `skill/pstate-schema.md:54`: "all of a hot key's writes land on its one task, making it a hotspot. Hash balances *keys*, not *load*." When the stream throttle trips, acked appends throw for every layer that shares that task (**CHECKED** `docs/11-stream-topologies.md:297`). More workers do not help, because a task is the unit of parallelism and the task count is fixed. Under entity placement the same team's writes spread over every task, since they touch many entities.
- **A private layer that outgrows a task.** A partition lives whole on each of its replicas' disks. Picture tens of agents at machine rate for years, with reads and crossings recorded, all in one person's layer. Rama cannot split a task (**CHECKED** `docs/19-operating-rama.md:483`). RPL's remedy for one big key is to spread it over several tasks (`skill/pstate-schema.md:69`). Then the layer has several writers and the one number is gone.
- **Balance.** Hashing balances well only with many similar keys per task (**CHECKED** `skill/pstate-schema.md:53`). Entities are many and small. Layers are few and wildly unequal: a handful of huge team layers and a sea of tiny session layers.
- **Ordinary reads.** "Nearest layer wins" for one entity now reads the session layer's task, the person's, the team's and the base's. That is a hop per layer in the stack, on every render and every tool firing. Under entity placement all layers' rows for an entity sit together, and one local read resolves the stack. RPL (N) says placement is chosen so that "the dominant read's access pattern is cheap". So the real question is which read dominates: the stack read of one entity, or the pattern read over one layer. I think the first, by a wide margin. This line turns on that.
- **Promotion.** It becomes a step across partitions. That is harmless. The brief already makes promotion an ordinary offer, and based-on links the two facts.
- **A saying across two layers.** Two tasks, two transactions. Atomic landing is lost again. The gain holds only for sayings that stay inside one layer.
- **No way back.** Moving one key's data to another task while running: **NOT IN THE REFERENCE**. A layer that starts as one person's and becomes a team's cannot be re-homed without the full copy into a new module.

## 3. Verdict on the hybrid

It is buildable. For session layers and one person's layer it is the better placement. It fails if "private" is the test. A team layer is private in who may see it and base-like in load. The test has to be writers, not visibility: a layer with one owner is placed by layer; a layer with many writers, and the base, by entity.

- **CHECKED possible:** a custom depot partitioner over any function of the record (`docs/14-depots.md:41-58`); a configurable key partitioner on a PState (`docs/16-partitioners.md:158`); one event on one task is atomic; no hop when placements match.
- **IMPLIED:** one number per single-owner layer (the gate must keep the counter, since the reference shows topology code no offsets); a many-fact saying as one record in one event (record size is unexamined; RPL treats about 50 KB as one fetch); local policy reads; a hop per layer on stack reads; a one-thread ceiling per layer.
- **NOT IN THE REFERENCE:** re-homing a key online; splitting a task; any limit on record size.

What must be decided before the first record, because placement cannot change later:

1. The placement function, and the class of each layer. The partitioner runs on the client and sees only the record (**CHECKED** `skill/depot-design.md:132`). So the class has to be readable from the offer itself: from the layer id, or from a slot the gate verifies. That sits badly with a fully opaque layer id.
2. An epoch on every position, per placement unit. A layer that moves, or a store that re-partitions, ends one order and starts another. A cut is then (epoch, number). This keeps both doors open.
3. The promise made to tools. Say this much: a single-owner layer has one order while it has one home. Never promise whole-layer order for a layer with many writers.
4. The saying as a unit, with one id and one verdict, carried on each of its facts. With that, even under entity placement, readers can count a saying's facts only once its verdict says whole. Without it, no placement can repair a half-landed reply afterwards. This one is mine (I), and it does not depend on which side wins.

On "name the cut": agreed. It is the interned cut from round two. Under a microbatch gate the build number already exists.

