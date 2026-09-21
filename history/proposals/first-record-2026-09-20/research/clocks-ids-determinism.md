# Clocks, ids, determinism, and checking: what the builders would fix before the first record

Research session *research-6*, for Sid's main Softland session (softland-ff). 20 September 2026.
Group: determinism, clocks, ids, and checking, with Google's storage lineage.
Round one. I was not given Sid's current leanings, on purpose. Nothing here was read from the Softland repository; the brief was the only input about the system.

## 0. How to read this, and how far to trust it

**What this is.** A reconstruction of how eight groups of builders would reason about Sid's questions, taken from what they wrote. Section 1 is the short version. Section 2 goes team by team in the six-part form the brief asked for. Section 3 gathers it under Sid's question numbers. Sections 4 to 6 are the group-level answers. Section 7 lists sources.

**The three marks.**
- REPORTED: they wrote it or said it, and the source is named.
- INSTITUTIONAL: a paper or document with many authors. It shows what an organisation published, not what one person thought.
- INFERRED: my reconstruction from their system or principles. Every statement about what Sid's store should do is INFERRED, whether or not the word appears beside it, because none of these people has seen Sid's design.

**How the quotes were checked.**
- Every source was downloaded and saved as text (about 190 documents). Quotes were copied from the saved text, not from memory. Eight gatherer agents did the downloading and locating. I read their findings, re-read the key passages myself, and wrote everything in this report.
- I re-checked by hand 42 of the gatherers' quotes that carry the most weight. All matched. I also read a number of passages directly: RFC 9562, Twitter's Snowflake post, Google's audit-log page, Chubby's passage on ACL files, FoundationDB's snapshot reads, and TigerBeetle's pages on transient failures and the cluster id.
- Then a script compared every quotation in this report with the saved sources. It caught one quotation that a gatherer had reported wrongly, and a few slips of my own. All are fixed. The details are in section 7.
- Double quotation marks are used only for words copied from a source, and for titles. Sid's own terms and phrases from the brief are in *italics*. Inside a quote, three dots mark words I left out, and single quotes stand for the source's own inner quotation marks. Nothing else is changed.
- The saved copies are in a temporary scratch folder and will be lost on restart. The URLs in section 7 are the durable record.

**What I could not get.** Dave Rosenthal's and Dave Scherer's own words (FoundationDB's old blog is gone from the archive paths we tried). A transcript of Will Wilson's 2014 talk (we have only a blogger's notes, and I mark them as that). Kyle Kingsbury's remark about FoundationDB's testing (a tweet we could not retrieve; I do not quote it). Any Temporal text giving the design reasons for replacing its first two Worker Versioning designs. A published results report for Restate's own Jepsen tests. Anything in the Zanzibar paper on who may write the first rules. The original ACM pages for three papers returned errors; identical copies from the authors' or archive sites were used.

**Corrections to the brief's assumptions, found along the way.**
- The 2012 Calvin paper does not say that time and randomness must be frozen into the input. That rule appears only in Abadi's later writing (2017 blog, 2018 article).
- Temporal's "Designing a Workflow engine from first principles" was written by Shawn Wang as a summary of a talk by Maxim Fateev. Restate's post on the immutability problem is by Jack Kleeman, not Stephan Ewen.
- The Restate article the brief calls *The Anatomy of a Durable Execution Stack* is the same article as "Building a modern Durable Execution Engine from First Principles".

## 1. The short version

**Ten findings for Sid's questions.** Each is argued with sources in sections 2 and 3.

1. **A record's name, its order, and its integrity are three jobs. Every system that lasted gave each its own field.** TigerBeetle: a client-made id, a cluster-made timestamp, a hash-chained log number. Apple's Record Layer: its own primary key, with the store's version kept beside it. Temporal: a client-chosen id, a position, and a server-made id the docs tell you not to store. Systems that fused name and order paid for it later: FoundationDB's versionstamps broke when data moved between clusters; Twitter's ids turned into an unreliable cursor; CockroachDB once treated equal timestamps as equal identity and lost isolation. For questions (1), (2), (11).

2. **The offerer makes the id, before the first attempt, and keeps it for retries. No one in this camp disagrees.** FoundationDB calls the lack of this "probably the biggest 'gotcha'". It matches the checked Rama fact in the brief. For (2).

3. **Keep the gate's verdict, refusals included, under the offer's id.** TigerBeetle lived without both and added both. Before the change, "a transfer that previously failed could succeed if retried when the underlying state changes". The only system that re-derives verdicts instead (FaunaDB) can do so because one version of the checking code runs everywhere. Sid's gate lives in the runtime, the one thing not made of facts. For (7).

4. **Do not rest safety on *one writer*.** Kingsbury showed that Datomic, the nearest checked system to Sid's, has two live writers during failover, "not a single-writer system, but a multi-writer one!", and that it did not matter, because safety came from a sequential compare-and-set in storage. Sid's offers already carry that compare-and-set. What is missing from the brief's picture is something in each append that lets the log refuse a stale gate (Chubby's sequencer, Restate's epoch). For (10).

5. **Let *as of* be an opaque token that the store mints, as Zanzibar did "to allow future extensions".** One number for the whole world has a price, and everyone who paid it had unusual means: one sequencer in one region, a batch wait on every write, atomic clocks with a wait on every commit, or one row that every write touches. Even then, each partition must prove it has caught up, and idle ones need heartbeats. An opaque token lets Sid defer that choice without trapping the first record. For (10), (5).

6. **Never answer from an index that is behind. Record the position an answer is complete to.** Google across three generations (F1's cache, Zanzibar's index, Firestore's live queries) and Apple's Record Layer all wait, catch up, or refuse. None of them notes *my index was behind* on an answer. And record the *pattern* with its position, never the rows, because version checks on rows cannot see rows that arrive later (F1's "Insertion phantoms"). For (5), (4).

7. **When 37 replaces 25, say so on 37.** Elle, the best checker of such histories, needs exactly that: without it "we cannot tell" which write replaced which, and blind writes "destroy history". FoundationDB's own log ships the previous number with each new one. With the pointer, each cell's history can be verified from the facts alone, and a fork can be proved. For (6).

8. **Two whens, and order from neither.** TigerBeetle began with one timestamp and later had to add a special import mode that switches other features off; it now teaches "Recorded: when the fact was learned, and Effective: when the fact took place." Bigtable let clients stamp time, and Spanner took that back. Deterministic work gets time handed to it (TigerBeetle, Calvin, Temporal), so wherever a running answer depends on time passing, time has to land as a fact. For (11).

9. **Write the code version where something is derived or judged, and make rebuilds facts.** TigerBeetle writes the release into every log entry, makes the upgrade an entry, and never replays an entry under different code. Temporal writes the worker's build on every completed task. A session is too coarse a unit. A rebuild also moves no other fact, so unless it is a fact itself, nothing will ever recompute after one. For (12).

10. **In a store that never rewrites, erasure means deleting a key or keeping the value outside. Choose before the first record.** Google's other road, compaction, is rewriting. TigerBeetle keeps erasable things out of the store entirely. A value once written in the clear is in every backup. For (9).

Smaller findings are in section 3: keys as ids with their words as facts (3); the immediate actor as itself, with *acts for* as a separate short-lived statement (8); reads left unrecorded by default everywhere else, for volume (14); an explicit mark for *starts a chain* in place of an empty slot (11); constants that are the same in every store and identities that differ (17).

**What cannot be added later, and what can wait.** This is my own gathering-up of the above, so all of it is INFERRED. It is the camp's experience turned toward Sid's actual decision.

Gone for every earlier fact if it is not there from the first one:
- An id made by the offerer, on every offer and fact. Pointers made before such ids exist would have to use the gate's number, and those pointers are forever.
- Room for *which store, which incarnation* in anything that points or says *as of*. The easy way is to make *as of* an opaque token. Apple added this late and carries a special case for the old records for good.
- On each new version, the version it replaced.
- The verdict, under the offer's id, naming what it checked: grammar version, policy fact and the position it was read at, expected version, gate build.
- Two whens: the gate's, and the offerer's claim.
- On each based-on entry, how it was read (exact version, at least as fresh as, latest at a position) and whether the fact depends on it. A default can be written from day one. It cannot be reconstructed afterwards.
- The immediate actor as itself, with *acts for* as its own statement.
- An explicit mark for the start of a chain.
- A version for the envelope, spare room that must be empty, and no value that doubles as a signal.
- The erasure road. A value once written in the clear cannot be taken back out of the backups.

Can wait, because it is derived, physical, or can be switched on later without harming the past:
- How indexes are laid out, and the physical encoding of records.
- Whether *as of* is one number or a position per partition, provided the token is opaque.
- How many gates run at once, provided safety rests on the compare-and-set and an epoch.
- Which motions of the hand are recorded. Switching one on later loses only what was never meant to be kept.
- The tools for checking. But the model of the envelope's rules is cheapest before the first record, and the conventions above are what make the log checkable at all.

**The strongest challenge this camp makes to the premise.** It is aimed at *running answers are deterministically re-derivable, so never stored*, taken together with *the runtime will be rebuilt hundreds of times* and *nothing is ever trimmed*. The people who have lived by replay for a decade, Temporal and Restate, found that re-derivation holds only for a named pair, this code version on this build, and that the pair keeps changing. They can see when it breaks only because the old outputs are in the record to compare with. And every remedy they found depends on the past ending: old code is deleted once old histories "have left retention"; old builds are shut down when their pinned runs finish; long histories are cut. Sid's store is defined by the past never ending, and running answers leave no output to compare with. So a runtime rebuild could change what *yesterday's answer* re-derives to, and no one could tell. The camp would ask Sid to choose openly among three roads before the first record: keep every tool version and runtime build runnable forever; promise unchanging behaviour and prove it on every rebuild; or let each crossing carry enough to check a later re-derivation, or to skip it. The choice decides what a crossing fact must hold. Section 5 has the full argument and the smaller challenges.

# 2. Team by team

Each team is written up in the six parts the brief asked for. They are ordered by how much they matter to Sid, as argued in section 4.

## 2.1 TigerBeetle (Joran Dirk Greef and team)

**Who.** Joran Dirk Greef started TigerBeetle in 2020 as a ledger database for payment systems. The team writes a lot down: reference docs, design docs in the repository, a changelog, an API-changes file, blog posts, and long interviews. Kyle Kingsbury tested it in 2025 (Jepsen, TigerBeetle 0.16.11). Statements from the docs and source are the team's, so I mark them REPORTED when the text is theirs and unambiguous; Greef's own words come from the 2021 clock post, a 2023 QCon talk and a 2025 Changelog interview.

This is the closest system in my group to Sid's record. In January 2026 the team even described it in Sid's words: "TigerBeetle is a fact-ingesting machine", and "TigerBeetle is the recording layer, and the application interprets those facts in the reporting layer" (Lewis Daly, "One for the Treble, Two for the Time").

### 1. What they built, and what they chose

- **Records are never changed and never deleted.** "Transfers are immutable. They are never modified once they are successfully created. There is at most one Transfer with a particular id." "Transfers cannot be deleted after creation. If a transfer is made in error, its effects can be reversed using a correcting transfer." The same for accounts: "Accounts cannot be deleted after creation. This provides a strong guarantee for an audit trail – and the account record is only 128 bytes."
- **Every record has three names, each with one job.**
  1. The `id`: 128 bits, made by the client, before the first send. "The primary purpose of an id is to serve as an 'idempotency key' — to avoid executing an event twice."
  2. The `timestamp`: made only by the cluster. "All timestamps within TigerBeetle are unique, immutable and totally ordered." Inside the database this is the real key: "Every object in TigerBeetle has a globally unique u64 creation timestamp which plays the role of synthetic primary key" (ARCHITECTURE.md).
  3. The `op`: the entry's number in the replicated log, tied to the one before it: the primary appends "the prepare to the Write Ahead Log (WAL), assigning it the next sequence number and adding a checksum 'pointer' to the previous log entry".
- **Records point at each other by the client's id**, not by the cluster's number (a transfer that settles a pending one names it in `pending_id`).
- **The envelope is fixed and small.** 128 bytes, no padding, checked at compile time. It includes a field named `reserved`: "This space may be used for additional data in the future... Must be zero". Flag sets carry spare bits that must also be zero.
- **One writer, one thread, fully deterministic.** "All client requests (and all events within a client request batch) are executed with the highest level of isolation, serially through the state machine, one after another". "Determinism means that given the same input the software gives the same logical result and arrives at it using the same physical path."
- **The clock is the cluster's, never the client's**, and the writer may not read it directly (see reasons below).
- **No actor on the record, and no authentication at all.** "TigerBeetle does not support authentication. You should never allow untrusted users or services to interact with it directly." Names, descriptions and who-did-it live in another database; the record carries opaque `user_data` slots that can point there.
- **The running code version is written into every log entry** (details in 4, question 12).

### 2. Their reasons, in their words

- On client-made ids. "The client software, such as your app or web page, that the user interacts with should generate the id (not your API). This id should be persisted locally before submission, and the same id should be used for subsequent retries." And: "Avoid requiring a central oracle to generate each unique id (e.g. an auto-increment field in SQL). A central oracle may become a performance bottleneck".
- On ids that carry time. "Random identifiers are not recommended – they can't take advantage of all of the LSM optimizations. (Random identifiers have significantly lower throughput than strictly-increasing ULIDs)." Their scheme: "the high 48 bits are a millisecond timestamp the low 80 bits are random." The reason is storage speed, nothing else.
- On who stamps time. "An important invariant is that the TigerBeetle cluster assigns all timestamps... This is why the timestamp field must be set to 0 when operations are submitted by the client." "This restriction is needed to make sure that any two timestamps always refer to the same underlying clock (cluster's physical time) and are directly comparable."
- On why one machine's clock is not enough. Greef, 2021: "if NTP silently stops working because of a partial network outage, and if TigerBeetle keeps transacting, then we would be running blind, in the dark, while disconnected from true time." The fix: "combining the majority of clocks in the cluster to construct a fault-tolerant clock called 'cluster time'. We use cluster time to bring a server's system time back into line if necessary, or shut down safely if we see too many faulty clocks." They would rather stop than stamp with a bad clock.
- On why the deterministic writer cannot read the clock. "The state machine can't access OS to get time directly as that would violate determinism. Instead, the TigerBeetle primary injects a specific timestamp into the logic of the state machine when it converts a request to a prepare." Matklad (2025) gives the general rule: "instead of parametrizing the component over Clock, you can pass a now: Instant to methods that need time".
- On never deleting. "This is important because adding transfers as opposed to deleting or modifying incorrect ones adds more information to the history. The log of events includes the original error, when it took place, as well as any attempts to correct the record and when they took place. A correcting entry might even be wrong, in which case it itself can be corrected with yet another transfer." And: "it would not be possible to go back if the original record were modified or deleted."
- On what determinism buys. "It reduces the problem of synchronizing mutable state to a much simpler problem of synchronizing an immutable, append-only, hash-chained log." "Any test failure can be reliably reproduced by sharing a seed".
- On the 128 bytes. Greef, 2025: "Everything in TigerBeetle is CPU cacheline-aligned... Everything is zero copy deserialization, fixed size, very strict alignment." The fixed envelope is a speed choice first.
- On deciding before building. TIGER_STYLE: "code, like steel, is less expensive to change while it's hot. A problem solved in production is many times more expensive than a problem solved in implementation, or a problem solved in design." And: "We do it right the first time. This is important because the second time may not transpire".
- On the limits of their own testing. "With simulation testing, there is the temptation to trust the fuzzer. But a fuzzer can prove only the presence of bugs, not their absence."

### 3. What they later changed, regretted, or migrated

**a. They started keeping refusals (release 0.16.4).** Before, a transfer that was refused could succeed when retried later with the same id. Now: "Clients have the strong guarantee that a Transfer.id that has once failed due to a transient error code will never succeed again if retried." The reference page gives the reason: "Transient errors depend on the database state at a given point in time, and each attempt is uniquely associated with the corresponding Transfer.id. This behavior guarantees that retrying a transfer will not produce a different outcome (either success or failure). Without this mechanism, a transfer that previously failed could succeed if retried when the underlying state changes". The changelog adds that it "guards against surprising behavior when the client is running in a stateless API service". Note what is kept: only refusals that depended on the state of the store. A refusal about the request's own shape comes out the same every time, so it needs no memory.

**b. They changed the reply to carry a verdict for every event (0.17.0, breaking).** The old reply listed only failures. "While this approach prioritized saving network bandwidth by omitting results for the common happy path, it didn't provide enough information about the outcome. The new protocol departs from the sparse array style, returning the status of each event, including the successfully created ones, along with the timestamp when they were processed by the TigerBeetle cluster."

**c. They had to let a second clock in (0.15.7, the `imported` flag).** Customers arrive with history. So: "When set, allows importing historical Transfers with their original timestamp." The rules show how much one shared clock had been carrying: "It is not allowed to mix events with the imported flag set and not set in the same batch." "User-defined timestamps must be a past date, never ahead of the cluster clock". "Timestamps must be strictly increasing." "Imported transfers cannot have a timeout." And: "it's recommended to import events only on a fresh cluster or during a scheduled maintenance window." By 2026 the team teaches two times instead of one: "we record two timestamps: Recorded: when the fact was learned, and Effective: when the fact took place." The docs suggest a spare slot for the second: "this might store a second timestamp for 'when' the transaction originated in the real world, rather than when the transfer was timestamped by TigerBeetle."

**d. A value used as a signal had to be undone (0.16.0, breaking).** An amount of zero used to mean "as much as possible". Then real zero-amount transfers were needed: "Zero-amount transfers are now permitted. This is a breaking API change".

**e. How formats change, and a reversal on how.** Old request formats are never edited. They are frozen under their old operation number and stay callable (`deprecated_create_accounts_sparse` and others); a new format gets a new number. The source says: "Any public API changes must be introduced explicitly as a new operation number", and that the state machine "should never accept client_release as an argument". The older API-changes file says the opposite (changes "gated in the state machine by the client's release"). We found no text that tells the story of the switch; the code comments mark the remaining gates for removal.

**f. Message types can never really be removed.** From the Jepsen report: TigerBeetle fixed a crash "by adding the deprecated message types back into the switch statements, and simply ignoring them."

**g. A shipped feature was pulled.** Changelog, July 2026: "Revert unique keys, which caused compatibility issues".

**h. What Jepsen changed.** Clients used to crash the whole process when their session was evicted; fixed. A node that lost all its data had no way back; a `recover` command was added. One upgrade hazard was written into the changelog instead of patched, given "a lack of test coverage for upgrades in general". One finding is still open: "By design, client requests are retried forever, which complicates error handling."

**i. *Immutable* has a carve-out they state openly.** "Accounts are immutable. They are never modified once they are successfully created (excluding balance fields, which are modified by transfers)." The derived totals are stored and updated in place. Only the events are append-only.

### 4. Which questions they speak to

**(0) Never rewritten, or only never lost?** REPORTED: the promise is about committed records, not about the physical log. "The WAL is a ring buffer with prepares". "Uncommitted ops may be replaced by different ops if they do not survive through a view change." "Committed ops are immutable." And the promise is made checkable: "All data in TigerBeetle is immutable, checksummed, and hash-chained, providing a strong guarantee that no corruption or tampering happened." INFERRED: they would call *never rewritten* an empty phrase unless something in each record lets a reader detect a rewrite. They also repair damaged blocks from other replicas, which rewrites bytes in order to keep the record the same. So the promise that matters is about the record's content, not its bytes on disk.

**(2) Ids.** REPORTED: made by the offerer, 128 bits, saved before the first send, never minted by a central service. They may reveal *when* (recommended, for speed). They never reveal *where*. Two values are reserved: "Must not be zero or 2^128 - 1". One id space is advised even where two are allowed, "because other systems (that you may later connect to TigerBeetle) may use a single 'namespace' for all objects." A second allowed scheme matters for ingest: "reuse an identifier of a corresponding object from another database."

**(1) Pointer: the gate's number or the fact's own id?** REPORTED: both exist, and records point at each other by the client-made id. INFERRED: the reason transfers to Sid. A name the offerer knows before admission lets one batch of offers point at each other, and lets a retry be recognized. The gate's number cannot do either, because it does not exist yet.

**(11) When, and order.** REPORTED: the cluster's clock only; made "strictly monotonic"; unique per record; and yes, used as the order: "A transfer that is created before another transfer is guaranteed to have an earlier timestamp (even if they were created in the same request)." INFERRED: this is order first, dressed as time. One primary hands out strictly increasing numbers and keeps them near real time. It is safe only because there is exactly one stamping point. With one gate per partition it would hold inside a partition and mean nothing across partitions.
Also REPORTED: durations are safer than instants across a clock boundary: "The timeout is an interval in seconds rather than an absolute timestamp because this is more robust to clock skew between the cluster and the application."
INFERRED from 3c: fix two *whens* before the first record: when the gate admitted it, and when the offerer says it happened or was made. TigerBeetle started with one and paid for it with a special mode that switches off other features.

**Time as an input to deterministic work.** REPORTED: when a pending transfer expires, that is not a silent side effect of the clock. It is an operation in the log (`pulse`), so every replica sees time pass at the same point. Kingsbury still found this the soft spot: "timeouts are not exactly deterministic, which makes it difficult to model-check." INFERRED for Sid: a running answer such as *stale after seven days* changes when no fact has moved. If time does not land as a fact, such an answer is not a function of its reads, and *recomputed when what it read moves* will never fire for it.

**(7) Is the gate's yes or no kept? Are refusals kept?** REPORTED: yes to both, and both were added later because their absence hurt (3a, 3b). Replies also say *which* field differs when an id is reused with different content ("exists_with_different_amount", one code per field).

**(12) Runtime version; are rebuilds facts?** REPORTED, and stronger than any other source in my group:
- Each log entry records the code release it was prepared under ("The corresponding Request's release version", in the prepare header).
- The upgrade is itself an operation in the log.
- A change of release is forced to line up with a checkpoint, so no entry is ever replayed under different code. The source comment explains the danger: "1. Execute op=X in the state machine on version v1. 2. Upgrade, checkpoint, restart. 3. Replay op=X when recovering from checkpoint on v2. If v1 and v2 produce different results when executing op=X, then an assertion will trip".
- One binary carries several releases ("Multiversion Binaries") so a replica can always run the code an old entry needs.
- The promise they make is about storage, not the API: "data files created by a particular past version of TigerBeetle can be migrated to any future version"; but "At the moment, TigerBeetle doesn't guarantee complete API stability."
INFERRED for Sid: TigerBeetle never trusts new code to reproduce old results. Sid's running answers ask exactly that of every future runtime.

**(9) Deletion.** REPORTED: there is none. Closing is a flag that a later record can undo. Mistakes are fixed by adding records. Anything that might need erasing (names, descriptions) is kept out, in an ordinary database, and those mappings "should be immutable and append-only". INFERRED: asked how a value is deleted, they would say that such a value should never be put in this store at all, only a pointer to it.

**(8) By whom.** REPORTED: not their problem, on purpose. A stateless API tier in front does "authentication and authorization". INFERRED: checking identity depends on things outside the log (keys, tokens, revocations). For a deterministic writer, its result is an input to be recorded, not something to re-derive. Sid pulls identity into the fact; TigerBeetle would want the check itself kept outside the deterministic core and only its result recorded.

**(13) Storage.** REPORTED: fixed structs, not open maps; reserved room that must be zero; formats evolve by new operation numbers while old ones stay frozen; storage format stable since 0.15.x (March 2024).

**(6) Does the new one name the old?** REPORTED: at the log level always (each entry carries the checksum of its parent, "for hash chain verification"). At the record level, a settling transfer names the pending one.

**(10) Two writers?** REPORTED: never; one primary, one thread. The cost: 64 client sessions by default, the oldest evicted. Many clients are expected to sit behind a stateless tier and send large batches.

**Retries.** REPORTED: "the TigerBeetle client will never time out... An error would imply that a request did not execute, when that is not known." Kingsbury's objection is in 2.7. One subtle REPORTED point: the reply cache belongs to a session, and a restarted client gets a new session. "Requests retried by a different client (same request body, different session) may receive different replies." Only the record's own id survives a restart. That is why the id, not the connection, has to carry retry safety.

**The `because of` slot.** INFERRED from 3d only: an empty slot that means *starts a chain* is a value used as a signal. In a store that never rewrites, an empty slot could also mean an older envelope, or a bug. TigerBeetle had to break its API to undo one such signal. An explicit *starts here* mark is cheaper now than later.

### 5. What resembles Sid's situation, what differs

Resembles: immutable small facts; one writer that stamps time; ids made by the offerer; a fixed envelope meant to last; a recording layer kept apart from an interpreting layer; refusals and verdicts kept; years of living with never-rewrite, and a public record of what that forced them to add.
Differs: one domain and one record shape; no actor, no layers, no policy, no provenance; one cluster with one primary, not a worldwide store; a recycled log; stored running totals. The speed arguments (cache lines, LSM runs) are about their engine, not Sid's.

### 6. Who they disagree with

- With Google's docs (2.6) on ids that carry time: recommended here, an anti-pattern there. The physics differ: one shard against range-split shards.
- With Kingsbury on retrying forever. They defend it as honesty about not knowing; Kingsbury says it "complicates error handling". Still open.
- With Temporal (2.2) in practice: TigerBeetle never lets two versions of code run the same entry. Temporal's patching does exactly that, on purpose.
- With Spanner on clocks: no special hardware, but they stop the cluster rather than stamp with clocks they cannot trust.
- On lineage, Greef says their simulation testing came "from the people at Dropbox, not Foundation".

## 2.2 Temporal and Restate (durable execution)

**Who.** Maxim Fateev and Samar Abbas built Amazon's Simple Workflow and Azure's Durable Task Framework, then Cadence at Uber, then Temporal. Stephan Ewen (co-creator of Apache Flink) and colleagues (Till Rohrmann, Ahmed Farghal, Jack Kleeman, Giselle van Dongen, Francesco Guardiani) built Restate. Both products do the same thing: record a history, and rebuild the state of a piece of work by running deterministic code over that history again.

Sources: Temporal's docs (the team's words, REPORTED); Fateev speaking on SE Radio 596 (2023, transcript); Restate's blog posts, which carry their authors' names; Restate's docs. Two corrections to the brief's assumptions: the Temporal post "Designing a Workflow engine from first principles" was written by Shawn Wang as a summary of a Fateev talk, and Restate's post on the immutability problem was written by Jack Kleeman, not Ewen.

These are the people who have lived longest with the exact sentence in Sid's design: *deterministically re-derivable from the same reads, so never stored.*

### 1. What they built, and what they chose

- **State is never stored. Only what cannot be re-derived is.** Fateev: "We don't store state of the workflow directly, we only store state of the results of activities because we record events." And: "we record every event in the workflow. So we use practically event sourcing to recover state of the workflow."
- **A hard line between deterministic code and everything else.** Temporal docs: "Workflow code must be deterministic to support replay. To handle non-deterministic operations like API calls, LLM/AI invocations, database queries, and other external interactions, put them in Activities." The sharpest form: "all operations that do not purely mutate the Workflow Execution's state should occur through a Temporal SDK API." Restate, for agents: "Restate records every non-deterministic step the agent takes in a journal: LLM calls, API requests within tools, generated timestamps and IDs."
- **Time and randomness come from the record, not from the machine.** Fateev: "if you need to use random, you practically need to use deterministic random which will be random the first time you call it and then it'll return the same value you replay. And Temporal provides special APIs for all these situations, for random, for time".
- **Replay checks itself against the record.** "When the Workflow's code replays, the Commands that are emitted are compared with the existing Event History." "If a generated Command doesn't match what it needs to in the existing Event History, then the Workflow Execution returns a non-deterministic error."
- **Names.** A Workflow Id chosen by the client, often meaningful, and the handle for retry safety: "at most one Workflow Execution with a given ID running at any point in time". A Run Id made by the server, and explicitly not to be leaned on: "You shouldn't rely on storing the current Run Id". An event id that is a position in the history.
- **The code version is written into the history**, per unit of work: the `WorkflowTaskCompleted` event carries "binary_checksum | Binary Id of the Worker that completed this Task", next to the worker's `identity`.
- **Reads are not recorded.** "Queries are efficient–they never add entries to the Workflow Event History, whereas an Update would (if accepted)."
- **Restate: one log, and the log does the fencing.** "Restate's core is a distributed Raft log of events and commands". "all state of the processor is deterministically derived from the durable log and can always be rebuilt from the log during recovery."

### 2. Their reasons, in their words

- Why a journal of steps and not snapshots of state (van Dongen, 2026): "A checkpoint can only take you back to a boundary — it doesn't recover execution, it restarts it."
- Why the log must do the fencing (Ewen and Kleeman, "Every System is a Log", 2025): "The server issues a unique epoch to every invocation and retry, which the SDK attaches to every journal event that it sends, allowing the server to reject events from subsumed handler executions (the conditional append)." And: "Having the update event conditionally appended to the same event journal as the lock event replaces the need for the lock's fencing token".
- How a stale leader is stopped (Ewen, Farghal, Rohrmann, 2025): "New leaders obtain the next epoch in a strictly monotonous sequence... The old leader (who might still be following the log) will receive that epoch bump message and step down at that exact point". "Any messages carrying lower epochs than the latest epoch-bumping message will be ignored... No split brain view is possible."
- What must stay together: "Everything related to an invocation happens within a single partition: invocation, idempotency & deduplication, journal entries, state, promises/futures, avoiding the need to synchronize and coordinate with any other shards."
- On retries: Temporal on activities: "We recommend that it be idempotent, so retries can be processed without duplicate side effects." On one-shot recorded values: "Do not ever have a Side Effect that could fail, because failure could result in the Side Effect function executing more than once."

### 3. What they later changed, regretted, or migrated

The whole history of this field is one problem: **the code that replays has changed.** Kleeman names it: "The journal no longer matches the code. This is the immutability problem; the code executing a given request must never change in its behaviour, despite the potential for requests to be replayed long after they started."

**a. First answer: keep both branches in the code (patching).** A marker is written into the history the first time new code runs, and replay follows the marker. The costs, from Temporal's docs: old branches stay until every old run is gone, "After all the Workflow Executions prior to version 1 have left retention, you can remove the code for that version". Change names are used up for good: after removal, "You can no longer use Step1 for the changeId." Temporal's own blog: "it requires you to maintain both code paths until all old Workflows complete. For frequent deploys on long-running Workflows, that's a lot of branches to juggle."

**b. Second answer: pin each run to the build that started it.** Temporal built this three times. The docs say of the first: "the 2023 draft of Worker Versioning, which was deprecated", and "Support for the experimental method of Worker Versioning prior to 2025 will be removed from Temporal Server in March 2026." We found no Temporal text giving the design reasons for the replacements, only the notices. The advice then flipped: "Worker Versioning should be the default recommendation for deploying Workflow code changes in production. If you can run versioned worker deployments, prefer Worker Versioning over patching." Both kinds now exist side by side, declared by the author: "A Pinned Workflow is guaranteed to complete on a single Worker Deployment Version", while "Auto-upgrade Workflows are not restricted to a single Deployment Version and need to be kept replay-safe manually, that is with patching."

**c. Pinning fails for long-lived work; Fateev says so.** "It works very well for relatively short workflows... But if you have workflow which runs for a month ... being able to deploy and run a pool of workers for every version becomes a daunting task." Kleeman, for Restate: "For long running workflows, however, this is not a solution. Replays can happen in principle months or years after execution started", and "there is a security and reliability concern about having arbitrarily old code running".

**d. Restate's answer: versions never change, and old ones are kept.** "When you deploy a version of your code, you give it an immutable, unique endpoint and register it with Restate. Restate then makes sure that requests start and end on the same version". For agents: "Every deployment is a complete, versioned snapshot of the agent: code, prompts, tool definitions, schemas, guardrails, and model configuration. Once deployed, it never changes." In 2024 Kleeman wrote of this pattern that "Keeping around old versions costs nothing". By 2026 Restate walked that back: "keeping old versions around isn't zero-cost in every sense. You still need to think about dependency updates, security patches for long-lived versions, and cold start latency".

**e. Third answer: cut the history.** Continue-As-New starts a fresh run with a fresh history. Temporal gives two reasons: size, and "A Workflow Execution can hit Workflow Versioning problems if it started running on an older version of your code and then begins executing on a newer version."

**f. Histories are capped, and then deleted.** "The Workflow Execution's Event History is limited to 51,200 Events or 50 MB", plus per-kind caps ("more than 2000 Updates", "more than 10000 Signals"). Fateev: "if you need to pass large blobs Temporal is not the technology". Closed histories are removed after a retention period ("The minimum Retention Period is 1 day"). Restate keeps a finished journal 24 hours by default, "for debugging and auditing", and trims its log: "the RocksDB database is periodically snapshotted to the object store and the log is trimmed to the point of the snapshot."

**g. Fateev on their first try:** "I wouldn't say we've got it right because it's still not very popular service, but we actually learned a lot doing that" (on Amazon's Simple Workflow).

**h. A small trap worth knowing.** A sleeping workflow never notices that a new version exists: "An idle Workflow isn't executing anything. It doesn't know a new version exists." The fix is to send it a wake-up message after each deploy.

### 4. Which questions they speak to

**The premise: running answers are re-derivable, so never stored.** This is where my camp pushes hardest.
- REPORTED: re-derivation holds only while the code "makes the same Workflow API calls in the same sequence, given the same input." When code changes, replay breaks.
- REPORTED: they can *see* the break only because the history holds the old outputs to compare with.
- REPORTED: every remedy they found leans on histories ending. Old branches are deleted when old runs "have left retention". Old builds are shut down when their pinned runs finish. Long runs are cut with Continue-As-New.
- INFERRED for Sid: Sid's facts never leave retention. So none of the three remedies is open in its usual form. If a running answer from three years ago must still be re-derivable, then the tool version (a fact, so nameable) *and* the runtime build of that day (not a fact) must both still be runnable. Restate tried *keep every version forever* and found the cost: security patches and dependencies for code nobody may change.
- INFERRED: because a running answer is never stored, there is nothing to compare a later re-derivation with. A runtime rebuild that changes an answer would go unseen. Temporal's non-determinism error exists only because the record holds the earlier result.
- INFERRED: so the honest reading is that *re-derivable* is a claim *relative to a named tool version and a named runtime build*. *What was I looking at yesterday?* and *what was the model given?* can be answered by re-derivation only if that pair can still be run, or if the crossing fact holds enough to check the re-derivation against (a digest of what was shown) or to skip it (the content itself). For a model's input, where exact bytes matter, Restate records the step itself, not a recipe for it.

**(12) Runtime version at session start; are rebuilds facts?**
- REPORTED: they write the build into the history, and per unit of work, not per session (binary checksum on each completed task; Restate pins each invocation to a deployment).
- INFERRED: per session is too coarse if a session can outlive a deploy. The unit that needs the stamp is each crossing and each verdict.
- INFERRED from 3h: a rebuild moves no fact. So *recomputed when what it read moves* will never fire for it. If a rebuild is not itself a fact that running answers read, nothing will ever recompute them after the runtime changes.

**(4) Follow the latest, or stay on the version read?** REPORTED: both are needed, and the author must say which. Temporal ended up with exactly two declared behaviours, Pinned and Auto-Upgrade, after years with only one. INFERRED: a fact's read of another fact is pinned by nature (that is what lets staleness be computed). Following the latest is what a running answer does. The slot in based-on should be able to say which kind of reference it is, even if the first runtime only writes one kind.

**(11) When; time as an input.** REPORTED: replayed code never reads the machine clock; time comes from the record. INFERRED: a running answer that uses *now* is not a function of its reads unless *now* is one of them.

**(14) Which motions become facts; is being shown the same as looking?** REPORTED: reads leave no trace; only accepted writes do. And the same page sometimes advises turning a polled read into a recorded Update for speed. So for them the line is a cost trade, not a principle. The 51,200-event cap is what a system looks like after it learns the price of recording everything against one subject.

**(1) and (2) Names and retry.** REPORTED: a client-chosen id is the retry handle; the server-made id is unstable on purpose; position is a third name. Restate remembers an idempotency key for 24 hours by default. INFERRED: a store that trims nothing can remember an offer's id forever, which is stronger than either product offers.

**(10) Can two gates write one layer?** REPORTED: they will both try, at every failover. What stops the stale one is the log: an epoch that only moves forward, written into the log itself, with appends carrying a lower epoch ignored. Work for one subject lives in one partition. INFERRED: the question to settle is not whether two gates can exist but what in each append lets the log refuse the older one.

**(6) Does the new one name the old?** REPORTED: a reset run copies history "up to and including the reset point" into a new run; chained runs are linked. Forks are explicit, never silent.

**(7) Are failures kept?** REPORTED: failures are ordinary events in the history (a failed task records the build that failed it).

**(9) Deletion.** REPORTED: payloads can be encrypted by a codec on the user's side so that "all your sensitive data exists in its original format only on hosts that you control". INFERRED: that is key-deletion erasure, ready-made.

**(0) and (13) Kept forever?** REPORTED: no. Both treat the history as a tool for recovery with a time limit, not as an archive. This is the largest difference from Sid.

### 5. What resembles Sid's situation, what differs

Resembles: the same split between what is re-derived and what is recorded; agents and model calls as first-class citizens of that split; build identity in the record; a log that fences its own writers.
Differs: their histories are private to one piece of work, short, capped, and deleted. Replay is for recovery after a crash, not for telling the truth about the past. Sid's store is shared, forever, and re-derivation is how it answers questions about yesterday. Every escape hatch they rely on assumes the past eventually goes away.

### 6. Who they disagree with

- Restate against Temporal, in writing: patch branches "need to be removed with extreme care"; pinning to builds is "not a solution" for long work.
- Temporal's new advice against Temporal's old advice (patching first, then pinning first).
- Both against TigerBeetle's stance (2.1): they let different code versions meet the same history and manage the damage; TigerBeetle forbids the meeting.
- Both, by practice, against *never stored*: Restate snapshots derived state "To avoid arbitrarily long re-build phases", while holding that the log is the truth.

## 2.3 Zanzibar, and the people who rebuilt it

**Who.** Zanzibar is Google's one authorization system for Drive, Calendar, Cloud, YouTube and others. The 2019 paper has fourteen authors (INSTITUTIONAL), among them Mike Burrows of Chubby and Lea Kissner, who has also spoken about it in their own voice (Oso interview, 2021; REPORTED). The people who rebuilt it outside Google wrote down what they hit: AuthZed (SpiceDB: Jake Moshenko, Evan Cordell, Jimmy Zelinskie, Joey Schorr), Airbnb (Himeji: Alan Yao), and OpenFGA.

### 1. What they built, and what they chose

- **Access rules are small records in one shape**, written object#relation@user. Why: "Defining our data model around tuples, instead of per-object ACLs, allows us to unify the concepts of ACLs and groups and to support efficient reads and incremental updates".
- **Every stored rule keeps its commit timestamp, and old versions stay readable for a while.** Rows are keyed by "(shard ID, object ID, relation, user, commit timestamp). Multiple tuple versions are stored on different rows, so that we can evaluate checks and reads at any timestamp within the garbage collection window."
- **The client holds the *as of*.** It is called a zookie: "A zookie is an opaque byte sequence encoding a globally meaningful timestamp that reflects an ACL write, a client content version, or a read snapshot."
- **But not every rule is a record.** Rules about rules live in a per-namespace configuration, outside the tuples.
- **Every write also goes to a changelog in the same transaction**, which feeds indexes and watchers.

### 2. Their reasons, in their words

**The problem they refuse to have.** "'new enemy' problem, which can arise when we fail to respect the ordering between ACL updates or when we apply old ACLs to new content." Their two examples, in full:
- Example A, titled "Neglecting ACL update order": "1. Alice removes Bob from the ACL of a folder; 2. Alice then asks Charlie to move new documents to the folder, where document ACLs inherit from folder ACLs; 3. Bob should not be able to see the new documents, but may do so if the ACL check neglects the ordering between the two ACL changes."
- Example B, titled "Misapplying old ACL to new content": "1. Alice removes Bob from the ACL of a document; 2. Alice then asks Charlie to add new contents to the document; 3. Bob should not be able to see the new contents, but may do so if the ACL check is evaluated with a stale ACL from before Bob's removal."

**The rule that follows.** "to avoid applying old ACLs to new contents, the ACL check evaluation snapshot must not be staler than the causal timestamp assigned to the content update." For a content update at time Tc, reading at a snapshot no older than Tc "ensures that all ACL updates that happen causally before the content update will be observed by the ACL check." The bound belongs to each piece of content. It is not one global *now*.

**Why not always check against the very latest?** "such evaluation would require global data synchronization with high-latency round trips and limited availability."

**The protocol.** When content is about to be saved, the client asks for a zookie. "Zanzibar encodes a current global timestamp in the zookie and ensures that all prior ACL writes have lower timestamps. The client stores the zookie with the content change in an atomic write to the client storage." Later: "The client sends this zookie in subsequent ACL check requests to ensure that the check snapshot is at least as fresh as the timestamp for the content version."

**Why the token is opaque.** "We choose to use an opaque cookie instead of the actual timestamp to discourage our clients from choosing arbitrary timestamps and to allow future extensions."

**What the looseness buys.** The protocol says *at least as fresh*, so the server may choose. In their words the semantics "allow Zanzibar to choose any timestamp fresher than the one encoded in a zookie", and that freedom lets it "serve most checks at a default staleness with already replicated data". In production, requests whose zookies are more than ten seconds old outnumber recent ones by "about two orders of magnitude".

**Why stale rules are mostly harmless, and when they are not.** Moshenko (AuthZed, 2021): "It turns out that nobody should really care if someone is given access to an exact copy of something that they once had access to. There is no new information to be gained." The harm only comes with *new* content. That is why the token is tied to a content version.

**Why rules about rules are not records.** "While such relationships between relations can be represented by a relation tuple per object, storing a tuple for each object in a namespace would be wasteful and make it hard to make modifications across all objects. Instead, we let clients define object-agnostic relationships via userset rewrite rules in relation configs." Their lessons section names the payoff: clients can "change ACL inheritance rules without having to update large numbers of tuples."

**Why positive rules only.** Kissner: "when you have firewall-style ACLs that cannot be reverse indexed, everyone gets confused. We have 30 years of UX studies showing that people can't understand more than about eight firewall rules... I felt very strongly that we should skip that kind of allow/deny semantics — what we want is a canonical, positive representation of ACLs." And: "We almost entirely avoided intersections and negations in Zanzibar because they are one giant foot-gun."

### 3. What they, and their rebuilders, later changed or regretted

**a. Syntax frozen by its own success.** Kissner: "we've found that the syntax for inter-verb pointers is a lot more prone to errors than we would like — I'd like to go back in time to rewrite that. However, Zanzibar got very popular very quickly inside Google — once we knew we wanted to change the syntax, there was already a lot of it there." The format froze at first adoption, long before anyone chose to freeze it.

**b. Features were added after the fact for particular clients.** The lessons section: "Access control patterns vary widely: Over time we have added features to support specific clients".

**c. SpiceDB removed a Zanzibar construct that did two jobs at once.** "the usage of `_this` proved to be a point of major misunderstanding, as it makes a `relation` into *both* a store of relationships and a computed result. We therefore chose to remove `_this` and instead break relations into the two categories of `relation` and `permission`, with only `permission` holding computed or abstract results." Stored things and computed things got separate names.

**d. Without Google's clocks, the guarantee costs a serial point.** Cordell (AuthZed, 2021): "Cockroach does not provide external consistency for transactions involving non-overlapping keys." The documented trick was not enough: "through testing (now that we have a test!), we determined that we needed both transactions to write to the same key in order for Cockroach to assign the timestamps properly." So: "The simplest (and the default) static strategy is that all relationship writes touch the same row." One global order without special clocks was bought with one row that every write touches.

**e. Policy that lives elsewhere goes stale by itself.** Moshenko: "Even when permissions are stored directly alongside the data in a way where they can be mutated atomically, the policy that evaluates them usually lives in code or comes from a policy server. If any of the policy evaluation machinery is out of date, you can again introduce a new enemy!" Zanzibar itself keeps a second freshness rule just for configuration: "Zanzibar chooses a single snapshot timestamp for config metadata when evaluating each client request. All aclservers in a cluster use that same timestamp for the same request".

**f. Some rebuilders dropped the hard part.** Airbnb's write-up of Himeji never uses the words zookie, snapshot, timestamp, or new enemy (we searched the text). It fans out on write and invalidates caches, the opposite of Zanzibar's own rule: "To facilitate consistency, Zanzibar avoids storage denormalization and relies only on normalized data". OpenFGA's docs, as revised in September 2026, still say of zookies: "OpenFGA is considering a similar feature in future releases." AuthZed calls client-held consistency "an extremely important, but often overlooked feature of Zanzibar".

**g. Exact *as of* reads expire when history is trimmed.** SpiceDB: requests at an exact snapshot "can fail with a Snapshot Expired error", because old versions are collected.

**h. Kissner on a second copy of any rule:** "What happens when you want to remove somebody from that ACL? What you have built yourself is a giant privacy incident manufacturing system!" And on where new-enemy bugs breed: "especially when your indexing system is separate from your storage system."

### 4. Which questions they speak to

**(5) An index behind the log.** REPORTED: the answer is never taken from the stale index alone. The Leopard index is built offline, then: "Leopard servers maintain an incremental layer that indexes all updates since the offline snapshot, where each update is represented by a (T, s, e, t, d) tuple, where t is the timestamp of the update and d is a deletion marker. Updates with timestamps less than or equal to the query timestamp are merged on top of the offline index during query processing." The index learns how far it has got from the change feed: "The client can use the heartbeat zookie to resume watching where the previous watch response left off." Two conventions follow. Every derived index knows its own position, because the feed tells it. Every answer is *as of* a stated position. INFERRED for Sid: the based-on entry for a pattern read holds the position the answer was evaluated at. *How far behind was the index* is then not a separate thing to write down. An index that cannot state its position cannot be read honestly at all.

**(10) Is *as of* one number or a position per partition?** REPORTED: here it is one token, because Spanner's timestamps mean the same thing everywhere. REPORTED: the rebuild without such clocks had to funnel every write through one row. REPORTED: the token is opaque "to allow future extensions". INFERRED for Sid: this is the most portable idea in my whole group. Let *as of* inside based-on be an opaque token that the store mints and only the store interprets. Today it can hold one number. Later it can hold a position per partition, or a store id plus a position, with no change to any old fact. A bare number written into the first record can never be widened.

**(16) Who sees a fact before any permissions exist? Visibility under a lagging policy index.**
- REPORTED: visibility must be evaluated at a snapshot no older than the content being shown.
- INFERRED for Sid: every fact already carries its own admission position, so the rule is cheap to state: to show fact F, evaluate policy at a position no older than F's. It is only cheap to *do* if policy facts and the facts they govern share an order. If they sit in different partitions, this is question (10) again.
- INFERRED: the same holds for the gate. A gate that admits a write under a policy read from a lagging index is Example B on the write side. The verdict already names what it checked. If it names the policy fact *and the position at which the gate read it*, a later walk can find every fact admitted under a rule that had in truth already been withdrawn. That turns a silent hole into computable doubt.
- ABSENT: the paper never says who may write a tuple or a namespace config, and never discusses bootstrap or default-deny. We searched the full text for bootstrap, admin, trusted, authenticate, permission to write, default deny. The canonical source has no answer to *who sees or writes before any permissions exist*. We did not sweep all SpiceDB and OpenFGA docs, so this is *absent from what we pulled*, not *absent everywhere*. Chubby's inherit-on-create (2.6) is the nearest written answer in the lineage.

**Policies as facts in the same store (the ALSO OPEN list).**
- REPORTED against: Zanzibar chose not to make rules-about-rules records, for space and for bulk change.
- REPORTED for: a policy that lives outside the log has its own way of going stale (3e). Zanzibar needed a second snapshot discipline for config for exactly that reason.
- INFERRED: Sid's form (*a policy fact lets this actor write this key in this layer*) is already a rule over a whole key and layer, not one record per entity. That avoids Zanzibar's stated cost. And keeping policy in the same ordered store removes the second freshness problem. The price Zanzibar names still applies: one policy change flips many decisions at once, so one verdict must see one policy snapshot and name it.
- REPORTED: Zanzibar's own write-race guard is an ordinary record: "The lock tuple is just a regular relation tuple used by clients to detect write races." That is small precedent for keeping control information in-band.

**Layers and *nearest wins*.** INFERRED, offered as a caution and not a finding: Kissner's warning is about rule sets where a later rule overrides an earlier one. *Nearest layer holding a row wins* is an override rule. As long as layers only add or replace *content*, this is ordinary shadowing. If a layer is ever used to *hide* something (a row whose job is to mask a base fact), it becomes deny-by-position, and the reverse question "who can see this?" gets hard in the way Kissner describes.

**(3) Key: a word or an id?** REPORTED: in Zanzibar namespaces and relations are names defined in config, object ids are strings, and user ids are integers from Google's identity system. SpiceDB dropped the fixed user-id space: "only a 'userset' is supported (called a 'Subject'), as there is no predefined set of users IDs ala Google's GAIA." Little more here.

**(9) Deletion.** REPORTED only: old tuple versions are garbage-collected. Nothing on erasure.

### 5. What resembles Sid's situation, what differs

Resembles: rules as small uniform records; one store for the planet; a client-held *as of*; a change feed driving derived indexes; rules about visibility that must respect order.
Differs: Zanzibar stores only the rules. The content lives in each client's own storage, which is why the token has to travel between two systems. Sid's store holds content and policy together, so a fact's own position can serve as its token. Zanzibar trims history and leans on Spanner's clocks; Sid has neither.

### 6. Who they disagree with

- AuthZed against the other rebuilders: client-held consistency is the point, not an extra. Himeji and OpenFGA shipped without it.
- Zanzibar against Himeji on denormalizing (fan-out on write).
- Kissner against allow/deny rule lists in general.
- Moshenko, gently, against Zanzibar's own split between tuples and config: anything that evaluates policy from outside the ordered store can go stale.

## 2.4 FoundationDB and Apple's Record Layer

**Who.** Dave Rosenthal, Dave Scherer and Nick Lavezzo started FoundationDB in 2009. Will Wilson was an early engineer and later started Antithesis with Scherer. Apple bought FoundationDB in 2015 and runs CloudKit on it. I could not get Rosenthal's or Scherer's words in their own voice: the old company blog is gone from the archive paths we tried. What I have is the SIGMOD 2021 paper (twenty-one authors, Rosenthal, Scherer and Wilson among them, so INSTITUTIONAL), the official docs, the design doc for idempotency ids, forum threads where core developers answer under their own names, Will Wilson's 2024 essay, and Apple's Record Layer paper and schema guide.

### 1. What they built, and what they chose

- **One process hands out every version number.** "The Sequencer assigns a read version and a commit version to each transaction" (paper §2.2). The number is the order and the log position at once: "This commit version defines a serial history for transactions and serves as Log Sequence Number (LSN)" (§2.4.2).
- **The number is a counter dressed as time.** "The Sequencer chooses the commit version by advancing it at a rate of one million versions per second" (§2.4.1). No wall clock stamps any record in FoundationDB.
- **The log is chained, not only numbered.** "To ensure there is no gaps between LSNs, the Sequencer returns the previous commit version (i.e., previous LSN) with commit version. A Proxy sends both LSN and previous LSN to Resolvers and LogServers so that they can serially process transactions in the order of LSNs" (§2.4.2).
- **The versionstamp is an id that is also the order, with three owners.** "A versionstamp is a 10 byte, unique, monotonically (but not sequentially) increasing value for each committed transaction. The first 8 bytes are the committed version of the database... The last 2 bytes are monotonic in the serialization order for transactions" (Python API docs). The Record Layer adds 2 more bytes from the client. So: 8 bytes from the sequencer, 2 from the commit proxy's batch, 2 from the client.
- **Reads are tracked as ranges, for five seconds, then forgotten.** "FDB chooses a 5-second MVCC window to limit the memory usage of the transaction system and storage servers, because the multiversion data is stored in the memory of Resolvers and StorageServers" (§6.4).
- **The simulator came before the database.** Will Wilson: "Before we even started writing the database, we first wrote a fully-deterministic event-based network simulation that our database could plug into" ("Is something bugging you?", Antithesis blog, 2024).
- **Record Layer: records are Protocol Buffers; the schema (metadata) is versioned data kept in the store; evolution is by addition only.**

### 2. Their reasons, in their words

- On the single sequencer not being the limit: "The singletons (e.g., ClusterController and Sequencer) and Coordinators on the control plane are not performance bottlenecks, because they only perform limited metadata operations" (§2.3.3). INSTITUTIONAL. We found no independent source that checks this claim.
- On determinism: "All database code is deterministic; accordingly multithreaded concurrency is avoided (instead, one database node is deployed per core)", with "all sources of nondeterminism and communication... abstracted, including network, disk, time, and pseudo random number generator" (§4).
- On what it bought them. Wilson: "if one particular simulation run found a bug in our application logic, we could run it over and over again with the same random seed, and the exact same series of events would happen in the exact same order." And: "We deleted all of our dependencies (including Zookeeper) because they had bugs, and wrote our own Paxos implementation in very little time and it had no bugs."
- On what it cannot do: "Simulation is not able to reliably detect performance issues... It is also unable to test third-party libraries or dependencies, or even first-party code not implemented in Flow... several bugs have resulted from the true operating system contract being weaker than it was believed to be" (§4, Limitations).
- On why the core is tiny: "instead of thinking about all the features that it could have, we asked ourselves what features could we take away?" and "FoundationDB's core provides no indexing and never will" (docs, "Layer concept").
- On why schema must live in the store and not in code (Record Layer paper §10.2): "it is hard to atomically update the metadata code used by multiple Record Layer instances. For example, if one Record Layer instance runs a newer version of the code (with a newer descriptor), writes records to a record store, then an instance running the old version of the code attempts to read it, an authoritative metadata store (or communication between instances) is needed to interpret the data."

### 3. What they later changed, regretted, or migrated

This is the most useful part of the FoundationDB record for Sid.

**a. Retries: the biggest trap, admitted years later.** The design doc for idempotency ids (apple/foundationdb, `design/idempotency_ids.md`) opens: "Non-idempotent transactions is probably the biggest 'gotcha' that users need to be made aware of -- and they won't discover it organically." The developer guide's standing rule: "Avoid generating IDs within the retry loop. Instead, create them prior to the loop and pass them in." The fix (automatic idempotency ids) is still marked "experimental and not recommended for use in production", and its ids expire ("don't delete anything younger than 1 day"), so a very slow client gets "a new, non-retriable error". The same doc notes the cost of their own monotonic key: "Potential write hot spot, since all idempotency id writes are adjacent and monotonically increasing."

**b. An id that is also the order does not survive a move.** Record Layer paper (§8.1 on CloudKit sync): "versions assigned by different FoundationDB clusters are uncorrelated... CloudKit addresses this with an application-level per-user count of the number of moves, called the incarnation. Initially, the incarnation is 1, and it is incremented each time the user's data is moved to a different cluster. On every record update, we write the user's current incarnation to the record's header; these values are not modified during a move. The VERSION sync index maps (incarnation, version) pairs to changed records."
They did not rewrite old records. The older scheme's ids were mapped to sort before all new ones: "(incarnation, version) if the record was last updated with the new method and (0, update counter value) otherwise." A special case they carry forever.

**c. The same problem on restore, argued in public for six years.** Alec Grieser (Apple), forum, 2018: "If you restored data into a fresh database, then there is no guarantee that the database's version is greater than the original one... Adding a prefix indicating the 'generation' of the data (the number of times it has been moved around) is one way to get around the fact that you can't depend on the newly restored to cluster being correctly ordered." A fix was reported in 2019. Yet in 2024 a core developer posting as jzhou wrote of restoring to a new cluster: "There is no guarantee... because no such version bump exists for restore. Someone has to use `fdbcli> advanceversion`". Our gatherer flagged the contradiction and did not resolve it. GitHub issue #1073 states the workaround: "keep a counter somewhere else and prefix versionstamps with the current value of the counter."

**d. The commit version alone does not name a transaction.** The guide: the commit proxy will "assign the version it got from the master to all transactions within that batch." A community developer (KrzysFR) on the forum: "If you do some local testing, and always see the batch order equal to 0, it is probably because you are not able to generate enough concurrent load to trigger merging of transactions?" A convention that looks right at low load and breaks under load.

**e. Hand-marked read dependencies are a bug source.** Record Layer paper §10.1: "Bugs due to incorrect manual conflict ranges are very hard to find, especially when mixed with business logic. For that reason, layers should generally define abstractions, such as indexes, for such patterns rather than relying on individual client applications to relax isolation requirements."

**f. The encoding's own version trapped them.** Schema guide: "there is no clear upgrade path to begin using proto3 syntax without losing data... It is therefore advised that any existing record types continue to use proto2 syntax."

**g. A key layout fixed early had to be worked around.** "there is a single extent for all record types because CloudKit has untyped foreign-key" ... "references without a 'table' association. By default, selecting all records of a particular type requires a full scan" (§10.3; a page break splits the sentence in our copy). They later added type prefixes for new users. Note the cause: untyped references, which is also Sid's choice (*no type slot*).

### 4. Which questions they speak to

**(1) Pointer: the gate's number, or the fact's own id?**
- REPORTED (Record Layer): the record is named by its own primary key. The version sits beside it: "Since the version is only known upon commit, it is not included within the record's Protocol Buffer representation. Instead, the Record Layer writes a mapping from the primary key of each record to its associated version."
- REPORTED: a store-assigned number is not comparable across clusters, moves, or restores. It needs a prefix (incarnation), and the prefix must be written on every record at the time, because old records are never re-stamped.
- INFERRED for Sid: if facts are pointed at by the gate's number alone, three things follow. An offer cannot be pointed at until it is admitted, so offers in one batch cannot point at each other. A second store, a restore, or a move makes numbers collide. And the fix (a store or incarnation prefix) cannot be added to old facts. So the pointer format needs room for *which store, which incarnation* from the first record, even while there is one store.

**(2) Ids and retry.** REPORTED: mint ids before the retry loop, outside the store. This matches the checked Rama fact in the brief (ids minted inside a streaming topology are not retry-safe). FoundationDB learned it the slow way and says so.

**(10) Order; one number or many?**
- INSTITUTIONAL: one number is affordable when one process hands numbers out and every log server hears about every number (that is what the previous-LSN pointer is for).
- REPORTED costs: every failure of any transaction-system process ends an *epoch* and runs recovery; recovery throws away the unacknowledged tail ("a special recovery transaction that informs StorageServers the RV so that they can roll back any data larger than RV"); multi-region runs with one primary region, and losing a whole region can lose the tail ("A, C, and I of ACID but potentially exhibiting a Durability failure").
- INFERRED for Sid: a worldwide store with one sequencer means every write pays a trip to the sequencer's region. FoundationDB never tried that. If Sid wants one *as of* number, this is the price list; if not, *as of* is a position per partition and based-on must be able to hold that.

**(0) Never rewritten, or never lost?** REPORTED: even this very careful system rewrites its log tail at recovery. The promise is about acknowledged commits only. INFERRED: for Sid the promise should attach to admitted-and-acknowledged facts, and the gate's verdict is the acknowledgment. The guide also gives a guarantee worth copying for offers: on an unknown result, "the transaction either committed or not and if it didn't commit, it will never commit in the future."

**(11b) and (4) Recording reads; dependency or path?**
- REPORTED: FoundationDB records what every transaction read, as key *ranges* plus a read version. That is the same shape as Sid's pattern read. It keeps them five seconds, in memory, because of cost.
- REPORTED: it has two kinds of reads. "Snapshot reads selectively relax FoundationDB's isolation property, reducing conflicts but making it harder to reason about concurrency." An ordinary read says *if this moves, I am invalid*. A snapshot read says *I looked, but do not hold me to it.*
- REPORTED: letting each caller mark this by hand caused bugs that were "very hard to find" (3e above).
- INFERRED for Sid: yes, mark each read as depends-on or only-looked. Without the mark, every glance makes a fact look stale later and the staleness signal cries wolf. But the mark should come from the tool's definition (the abstraction), not from each actor's judgment call.

**(3) Key: word or id?** REPORTED: on the wire, Protocol Buffers use field numbers; names live in metadata. "Serialization is based on field numbers, not field names. So, for the record itself, this is invisible. However, index and primary key definitions in the meta-data itself are based on field names, so these will also need to be updated." And: "Under no circumstances should a new field be added with an existing field number." INFERRED: anything stored that refers to a key by its word pins that word forever. In Sid's store, pattern reads inside based-on refer to keys and can never be rewritten. So a key's word can never be renamed, unless keys are ids and the word is a fact about the id.

**(12) Runtime version.** INSTITUTIONAL: FoundationDB does not record the code version per record. It restarts every process at once and promises only that "on-disk data is compatible". The Record Layer keeps three counters in one store header: metadata version, storage format version, and an application version. INFERRED: this is enough for them because nothing is re-derived from old records by new code. Sid's store does re-derive, so this answer does not transfer. Temporal's does (see 2.2).

**(13) Format evolution.** REPORTED rules: add only; never reuse a number; deprecate instead of removing; "The only safe type change that does not require an index rebuild is to update a 32-bit, variable length integer to its 64-bit version"; indexes move through disabled → write-only → readable, and a query never uses an index that is not yet readable. That last rule is a no-optimism answer to (5): an index that is behind is not read at all.

**Checking.** REPORTED: build the deterministic simulator first; inject faults; tune the fault rate ("Fault injection distributions are carefully tuned to avoid driving the system into a small state-space"); measure what the simulation actually reached. A blogger's notes on Wilson's 2014 talk (a paraphrase, not Wilson's words) record a self-check: run the same seed twice and compare the random generator's final state, to catch hidden non-determinism.

### 5. What resembles Sid's situation, what differs

Resembles: one writer of order; ids that may double as order; schema kept as data in the same store; a small core with everything else layered on top; a team that wanted to be sure before building.
Differs: FoundationDB is a rewritable key-value store in one region with a five-second memory. It has no actor, no clock time, no provenance. It forgets reads after five seconds; Sid wants them kept forever. Its sequencer serves a datacenter, not a planet.

### 6. Who they disagree with

- With Spanner, on clocks: FoundationDB orders by one counter and uses no clocks; Spanner orders by clocks and has no single counter.
- With Spanner's later regret: FoundationDB says the core should have no indexes or query layer, ever. The Spanner team calls its own bolted-on query layer a mistake (see 2.6).
- With itself, on monotonic keys: versionstamps are offered as keys, while its own design doc names the "write hot spot" they cause.

## 2.5 Calvin: Daniel Abadi, Alexander Thomson, and FaunaDB

**Who.** Thomson and Abadi argued in 2010 that databases should be deterministic, and built Calvin (SIGMOD 2012, six authors) to show it. Abadi then defended the idea for years on a personal blog, in the first person (REPORTED). FaunaDB was the production descendant; Abadi was an advisor, and the blog says so ("Daniel Abadi is an advisor at FaunaDB"). Kyle Kingsbury tested FaunaDB in 2019. Fauna ended its service in May 2025.

### 1. What they built, and what they chose

- **Agree on the order of the inputs first. Then every replica executes them the same way.** "The sequencing layer (or 'sequencer') intercepts transactional inputs and places them into a global transactional input sequence—this sequence will be the order of transactions to which all replicas will ensure serial equivalence during their execution."
- **Record inputs, not effects.** "only the transactional input is logged—there is no need to pay the overhead of physical REDO logging. Replaying history of transactional input is sufficient to recover the database system to the current state."
- **Many sequencers, one order, no clock.** "Calvin divides time into 10-millisecond epochs during which every machine's sequencer component collects transaction requests from clients." Each batch carries "(1) the sequencer's unique node ID, (2) the epoch number", and every node can "piece together its own view of a global transaction order by interleaving (in a deterministic, round-robin manner) all sequencers' batches for that epoch." The global position of a transaction is (epoch, sequencer, place in batch).
- ***As of* is a place in that order**, not a time. A snapshot is taken "with respect to a virtual point of consistency, which is simply a pre-specified point in the global serial order".

### 2. Their reasons, in their words

- Why lack of determinism forces systems to agree on outcomes, which is the expensive thing: "two servers running exactly the same database software with the same initial state and receiving identical sequences of transaction requests may nonetheless yield completely divergent final database states" (2010).
- The hinge of the whole argument: "There is no fundamental reason that a transaction must abort as a result of any nondeterministic event" (2012).
- What replaces abort-and-retry: "Calvin did not pick up a transaction exactly where it left off … It accomplished this via restarting the transaction from the same original input" (Abadi, 2019).
- Why no clocks: "If every transaction goes through the same protocol, then a natural order of all transactions emerges --- the order is simply the order in which transactions were voted on during the protocol. When batches are used instead of transactions, it is the batches that are ordered during the protocol, and transactions are globally ordered by combining their batch identifier with their sequence number within the batch. There is no need for clock time to be used in order to create a notion of before or after" (Abadi, 2018).
- Why copying Spanner without its hardware is dangerous: "In light of Spanner's decision to have separate consensus protocols per shard, software-only derivatives are extremely dangerous", because "these systems lack hardware and infrastructure support for minimizing and measuring clock skew uncertainty." And: "For software-only implementations, it is hard to avoid occasionally violating the maximum clock skew bound assumption, and the violations themselves may not be discoverable. Therefore, unified consensus is the safer option."
- What goes wrong when order is per partition. Abadi names three anomalies that plain serializability allows: the immortal write, the stale read, and the causal reverse, in which "a later write which was caused by an earlier write, time-travels to a point in the serial order prior to the earlier write".

### 3. What they later changed, regretted, or conceded

**a. The rule about clocks and randomness came later than the protocol.** The 2012 paper never mentions clock reads or random numbers; every use of "nondeterministic" there is about failures and thread scheduling (we searched the text). The rule appears in 2017 on the blog and in 2018 in print: "The preprocessing layer also replaces nondeterministic code inside transaction logic with deterministic code. For example, code that makes system calls to get the current time or to generate a random number must be executed by the preprocessor and be replaced by a fixed value."

**b. The production system dropped Calvin's defining demand.** Calvin requires the full read and write sets before execution: "All transactions are therefore required to declare their full read/write sets in advance". Kingsbury on Fauna: "FaunaDB bypasses Calvin's requirement that transactions know their read & write sets before execution; instead, snapshot-isolated reads are executed by a coordinator before sequencing the transaction. An optimistic concurrency control protocol includes read timestamps in the transaction sent to the sequencer, allowing executors to identify whether objects have been modified since they were last read. Since transactions are pure, these conflicts can be transparently retried."

**c. "Without clocks" still needed clocks to keep moving.** Kingsbury: "In Calvin, clock skew has no impact on correctness." But: "FaunaDB still relies on wall clocks to decide when to seal time windows in the log, which means that clock skew can delay transaction processing... In none of our clock tests did FaunaDB exhibit new safety violations. However, clock skew can cause partial or total unavailability." The order was safe from clocks. The speed was not.

**d. Where the bugs really were.** "We found 19 issues in FaunaDB, including nontransactional schema changes, lockups removing nodes from clusters, unavailability in response to clock skew and reboots, indices which failed to return negative integer values or skipped records at the end of pages, and multiple snapshot isolation violations in temporal and indexed queries." The ordering core held. Schema change, indexes, and as-of queries did not.

**e. Claims had to be walked back, though the design held.** Kingsbury: "Fauna's approach is fundamentally sound: the bugs that we've found appear to be implementation problems". But: "Fauna's documentation for consistency properties was sparse, inconsistent, and overly optimistic: claiming, for example, that FaunaDB offered '100% ACID' transactions and strict serializability, when, in fact, users might experience only snapshot isolation."

**f. Costs the proponents state themselves.** Every write waits for a batch: "In the original Calvin paper, batch windows were 10ms (so the average latency would be 5ms); however, we have subsequently halved the batch window latency in my labs". Reads pay too: "a read-only transaction in Calvin must pay the cross-region replication latency". A stalled transaction blocks everything behind it; from their own 2010 experiment: "where transactions can deadlock or stall for any reason, execution schemes which guarantee equivalence to an a priori determined serial order prove to be a poor choice." And no back-and-forth with a client inside a transaction: "all recent implementations have limited or no support for interactive transactions, thereby preventing their use in many existing deployments."

**g. The company.** "we have made the hard decision to sunset the Fauna service... Driving broad based adoption of a new operational database that runs as a service globally is very capital intensive." The reason given was money. No technical failure was named.

### 4. Which questions they speak to

**(10) Order.** REPORTED: if one order is wanted across everything, get it from one ordered input log, batched, with no clocks. *As of* is then one position. The stated price is a batch wait on every write (about 5 to 15 ms) plus agreement across regions, paid by everyone, including writers who touch nothing in common. INFERRED for Sid: Rama depots are many separate logs, which is what Abadi calls partitioned consensus. By Abadi's argument, order across them then needs either Google-grade clocks or a single sequencing step in front. Without one of those, *as of* across partitions is a position per partition, and Abadi's three anomalies are the exact ways a cross-partition map could lie.

**(7) Is the gate's yes or no kept?** REPORTED: in FaunaDB it is not. The log entry holds the reads, and every replica works out the verdict again: "The actual log entry contains the newly determined identifier, along with a record of all the reads and buffered writes that were performed by that transaction's coordinator". "Note that the replicas perform this check independently from each other, reading from their local copy of the data. They will always come to the same conclusion about whether the reads changed or not." A refused transaction still sits in the log; only its verdict is implicit.
INFERRED for Sid: this works only while every replica, now and later, runs the same checking code. Sid's gate lives in the runtime, which is not made of facts and will be rebuilt hundreds of times. By Sid's own rule, anything that cannot be re-derived from facts alone must be recorded. A verdict depends on the gate build of that day. So by that rule the verdict is an offer-like thing and is kept, even though the closest production system to Sid's gate chose not to keep it.

**(11b) Is every read listed?** REPORTED: FaunaDB is a production precedent for writing the reads a write stood on into the log beside the write, and for using them to decide whether the write is still good at its place in the order ("to see if the values changed between the snapshot at which they were originally read and the correct snapshot as of the transaction's identifier"). That is Sid's staleness walk, done once at admission. FaunaDB refuses the stale write. Sid's gate admits it and shows it as stale. Same data, different policy.

**(11) Whose clock, and the gate's *when*.** REPORTED (2018): time and randomness are drawn once, before execution, and frozen into the input. INFERRED for Sid: the gate's *when* should be fixed once, as part of the ordered input, not read from a clock while executing. Rama can retry a streaming topology (the brief's checked fact about ids). A clock read inside the gate would give a different *when* on each retry, for the same reason an id minted there is unsafe.
REPORTED: the time shown to developers can be a courtesy only: "A 'timestamp' in FaunaDB is a logical concept and is simply the location in the distributed log", and "Rough correspondence between 'FaunaDB time' and real time is merely an affordance for the developer and not an operational constraint."

**Retry.** REPORTED: a retry is a re-run of the same recorded input. Nothing about the retry is recorded on its own.

**(13) and schema.** REPORTED: the worst Jepsen findings were outside the ordering core: schema changes that were not transactional, indexes, and as-of queries. INFERRED: the conventions around grammar facts and index positions deserve at least as much checking as the gate's ordering.

### 5. What resembles Sid's situation, what differs

Resembles, strongly: Calvin's biggest weakness is that the whole transaction must be handed over at once, with its reads declared, and no conversation in the middle. An offer in Sid's design is exactly that: one self-contained thing that names its reads and the version it expects. The restriction that kept Calvin out of "many existing deployments" costs Sid nothing. Also alike: one agreed order, checking that is deterministic, reads written next to writes.
Differs: Calvin assumes a single code version everywhere at a given moment and does not discuss what happens when the executing code changes. It stores mutable state and may cut its input log after a checkpoint. Its order costs latency for every writer in the world, which a worldwide store with session layers written at machine rate would feel.

### 6. Who they disagree with

- With Spanner and its software-only imitators, on using clocks for order across partitions.
- CockroachDB answers in two voices. Kimball and Sharif concede the anomaly by name, "While Spanner provides linearizability, CockroachDB only goes as far as to claim serializability", and argue that waiting out clock doubt is a choice about latency, not a need for hardware: "One could very well wait out the maximum clock offset in any system and achieve linearizability." Andrei Matei argues that what users need is simpler: "CockroachDB doesn't allow stale reads", while granting "CockroachDB does not offer strict serializability".
- With their own 2010 experiment, which shows fixed order losing badly when any transaction stalls.

## 2.6 Google's storage lineage: Bigtable, Chubby, Megastore, Spanner, F1, Firestore

Almost all of this is INSTITUTIONAL: papers with nine to twenty-six authors. Two single-author voices are REPORTED: Mike Burrows (Chubby, 2006) and Eric Brewer (the 2017 CAP note). The value of this lineage for Sid is that the same company wrote down what each generation got wrong, often years later and in plain words.

### 1. What they built, generation by generation

**Bigtable (2006).** Time is a dimension of every cell: "(row:string, column:string, time:int64) → string". Who stamps it? Either side: timestamps "can be assigned by Bigtable, in which case they represent 'real time' in microseconds", or they can be set by the client application. And uniqueness is not the store's problem: "Applications that need to avoid collisions must generate unique timestamps themselves." Old versions are trimmed by rule: "only the last n versions of a cell be kept, or that only new-enough versions be kept".

**Chubby (Burrows, 2006).** A small lock service used to elect one primary for hours or days. Three choices matter here:
- The fencing token. "At any time, a lock holder may request a sequencer, an opaque byte-string that describes the state of the lock immediately after acquisition. It contains the name of the lock, the mode in which it was acquired (exclusive or shared), and the lock generation number." And the check belongs to the receiver: "The recipient server is expected to test whether the sequencer is still valid and has the appropriate mode; if not, it should reject the request."
- Policy stored in the store itself. "ACLs are themselves files located in an ACL directory, which is a well-known part of the cell's local name space." And no node is ever born without one: "Unless overridden, a node inherits the ACL names of its parent directory on creation."
- Four counters and a checksum on every node: "an instance number; greater than the instance number of any previous node with the same name", "a content generation number", "a lock generation number", "an ACL generation number; this increases when the node's ACL names are written", plus "a 64-bit file-content checksum so clients may tell whether files differ."

**Megastore (2011).** The unit of order is the *entity group*: "We provide fully serializable ACID semantics within fine-grained partitions of data." Each group has its own log: "A transaction writes its mutations into the entity group's write-ahead log". *As of* exists only inside one group: "Current and snapshot reads are always done within the scope of a single entity group." Writers race for a log position by compare-and-set: "though multiple writers might be attempting to write to the same log position, only one will win. The rest will notice the victorious write, abort, and retry". The price: "Limiting that rate to a few writes per second per entity group yields insignificant conflict rates."

**Spanner (2012).** One timestamp that means the same thing everywhere, made from GPS and atomic clocks. The rule: "if a transaction T1 commits before another transaction T2 starts, then T1's commit timestamp is smaller than T2's." The clock admits its own doubt, `TT.now()` returns an interval, and the writer waits the doubt out: "the expected wait is at least 2 ∗ ε", where "ε is therefore 4 ms most of the time". Spanner took stamping back from the client: "Unlike Bigtable, Spanner assigns timestamps to data".

**F1 (2013).** Google's ad business on top of Spanner. Three choices matter: optimistic writes checked by version (the same shape as Sid's offer), change history written by the database itself, and a protocol for changing the schema while 1,500 servers hold cached copies of it.

**Firestore (2023).** A document store for app developers, rebuilt on Spanner after a decade on Megastore, with live queries that push consistent snapshots to listeners.

### 2. Their reasons, in their words

- Why Spanner, given Bigtable and Megastore: "we have also consistently received complaints from users that Bigtable can be difficult to use for some kinds of applications: those that have complex, evolving schemas, or those that want strong consistency in the presence of wide-area replication." And: "Many applications at Google have chosen to use Megastore because of its semi-relational data model and support for synchronous replication, despite its relatively poor write throughput."
- The famous sentence, exactly: "We believe it is better to have application programmers deal with performance problems due to overuse of transactions as bottlenecks arise, rather than always coding around the lack of transactions."
- On clocks, as a closing verdict: "As a community, we should no longer depend on loosely synchronized clocks and weak time APIs in designing distributed algorithms."
- Brewer on what the clock is really for: "One subtle thing about Spanner is that it gets serializability from locks, but it gets external consistency (similar to linearizability) from TrueTime." And on what it is not for: "TrueTime does not significantly help achieve CA... To the extent there is anything special, it is really Google's wide-area network, plus many years of operational improvements".
- F1 on why the database must write the change history itself: "In the MySQL system that AdWords used before F1, our Java application libraries added change history records into all transactions. This was nice, but it was inefficient and never 100% reliable. Some classes of changes would not get history records, including changes written from Python scripts and manual SQL data changes." So: "In F1, Change History is a first-class feature at the database level, where we can implement it most efficiently and can guarantee full coverage."
- F1 on why a schema cannot change everywhere at once: "there is no reliable mechanism for determining currently running F1 servers, and explicit global synchronization is not possible." Hence "multiple versions of the schema may be in use simultaneously", and hence the rule that any two neighbouring versions must be safe together (their Definition 5), with at most two alive at once: "we ensure that at any moment F1 servers use at most two schemas by writing a maximum of one schema per lease period."
- Burrows on why a lock service at all: "many programmers have come across locks before, and think they know to use them. Ironically, such programmers are usually wrong, especially when they use locks in a distributed system".

### 3. What they later changed or regretted

**a. The unit of order was too small, chosen too early, and could not be changed.** Megastore, 2011: "Nearly all applications built on Megastore have found natural ways to draw entity group boundaries." Firestore, 2023, about the same customers: "Our customers found the Megastore-based implementation restrictive for organizing their data, which had to be carefully organized into entity groups to support transactional updates and strongly-consistent queries. Furthermore, the write throughput to each entity group was limited. This forced most customers into using eventually-consistent queries". Google's own Datastore article lists as a limitation: "The entity group relationship can not be changed after entity creation." The Maps example in the Megastore paper already showed the trap: "the number of entity groups does not grow with increased usage, so enough patches must be created initially for sufficient aggregate throughput at later scale."

**b. Letting the client stamp time was taken back.** Bigtable let clients write timestamps and left collisions to them. Spanner stamps every version itself. Cloud Spanner's commit-timestamp column even enforces that it "can only contain values in the past."

**c. The correct fencing mechanism was cheap and still adopted slowly.** Burrows: "The sequencer mechanism requires only the addition of a string to affected messages, and is easily explained to our developers. Although we find sequencers simple to use, important protocols evolve slowly." So Chubby also shipped a time-based fallback, the lock-delay, and called it "imperfect".

**d. Burrows's list of misjudgments.** "Even though Chubby was designed as a lock service, we found that its most popular use was as a name server." "Chubby was never intended to be used as a storage system for large amounts of data, and so it has no storage quotas. In hindsight, this was naïve." "Developers rarely consider availability". On an API that made things worse: "We might have done better to send redundant 'file change' events instead, or even to ensure that no events were lost during a fail-over." And: "Readers will note the irony of our own failure to predict how Chubby itself would be used."

**e. Spanner's inherited storage format.** "Spanner originally used significant parts of the Bigtable code base, in particular the on-disk data format... This was a good decision at the time... It is self-describing and therefore highly redundant". Verdict: "they are ultimately a poor fit and leave a lot of performance on the table." The query layer too: "some of these database features were 'bolted on'".

**f. Spanner's "slow realization".** "Part of this long iteration phase was due to a slow realization that Spanner should do more than tackle the problem of a globally-replicated namespace, and should also focus on database features that Bigtable was missing."

**g. Bigtable's lessons.** The failures that really happened were not the textbook ones: "memory and network corruption, large clock skew, hung machines, extended and asymmetric network partitions, bugs in other systems that we are using (Chubby for example)". Also: "it is important to delay adding new features until it is clear how the new features will be used", and "The most important lesson we learned is the value of simple designs." They threw away a membership protocol that was too complex and leaned on "the behavior of Chubby features that were seldom exercised by other applications".

**h. Firestore, still open in 2023.** "mass-produced machines themselves are unreliable and may corrupt in-memory data. We are actively addressing these issues through the addition of end-to-end checksums". Also a generation change that broke a published limit: the move to Spanner "unavoidably reduced maximum key size, affecting a tiny number of documents for very few customers. We contacted these customers directly".

**i. F1's formal model paid for itself.** "Creating a formal model of the schema change process has had several positive effects on our production system. In particular, it highlighted two subtle bugs in our implementation".

### 4. Which questions they speak to

**(11) When: whose clock, and is it ever used for order?** INSTITUTIONAL: the store's clock, never the client's (3b). Time may carry order only with TrueTime-grade clocks and a commit wait. Without that hardware, the lineage's own history says do not: Bigtable's lessons list "large clock skew" as a fault they met in production.

**(10) Order, and *as of*.**
- INSTITUTIONAL: even with TrueTime, *as of* is not just one number. It is a number plus proof that each partition has caught up to it. Every replica tracks a *safe time*, the "maximum timestamp at which a replica is up-to-date. A replica can satisfy a read at a timestamp t if t <= tsafe". An idle partition cannot prove this by itself: "A leader by default advances MinNextTS() values every 8 seconds." Firestore does the same for live queries: "Changelog tasks generate a heartbeat every few milliseconds for every idle key range; this heartbeat is crucial for the Frontend tasks to know that they have received all updates when a document-name range is otherwise idle."
- INFERRED for Sid: telling *nothing happened here* from *something is late* costs a steady message per partition, even with no writes. If every personal layer and every session layer were its own ordered partition, that cost grows with the number of layers, not with the amount of writing. So the unit that carries order has to be coarser than a layer, while the compare-and-set cell (entity + key + layer) stays fine-grained.
- INFERRED from 3a: whatever Sid picks as *what must share a partition* will be fixed per fact forever (facts are never rewritten, so a fact can never move to another ordering unit). Megastore's customers got this wrong in a store where they *could* in principle rewrite. The safest reading of the lineage: do not let facts depend on which partition they landed in. Keep partition identity out of the fact's name and out of its *as of* format (see the zookie in 2.3).

**(1) and (4): the offer's expected version.** F1 is a lived precedent. "F1 returns with each row its last modification timestamp, which is stored in a hidden lock column in that row"; at commit the server "re-reads the last modification timestamps for all read rows. If any of the re-read timestamps differ from what was passed in by the client, there was a conflicting update". They made it the default: "F1 clients use optimistic transactions by default." Their stated benefits fit agents well: "Reads never hold locks", "Optimistic transactions can be arbitrarily long", "All state associated with an optimistic transaction is kept on the client", and *speculative writes*: "A client may read values outside an optimistic transaction (possibly in a MapReduce), and remember the timestamp used for that read."
Their stated blind spot is Sid's pattern read: "Insertion phantoms. Modification timestamps only exist for rows present in the table, so optimistic transactions do not prevent insertion phantoms". A version check on rows cannot see a row that was not there. INFERRED: a pattern read must be recorded as the pattern plus a position, never as the list of rows it returned, or later arrivals can never make it stale.
Their other stated limit: "in a table that maintains a counter which many clients increment concurrently, optimistic transactions lead to many failed commits". INFERRED: hundreds of people and their agents writing to one hot cell will mostly collect refusals. That is a modelling question (do not make hot cells), not an envelope question, but (7) decides whether those refusals pile up as facts.

**(5) An index that is behind.** Three generations give the same answer: do not answer from behind. F1's cache: the client "passes in the root row key and the commit timestamp of the last write that must be visible. If the cache is behind that timestamp, it reads Change History records beyond its checkpoint and applies those changes to its in-memory state to catch up." Firestore: the front end tracks "when it has received all the updates necessary to reach a consistent timestamp. Only then does it send the accumulated delta". And when it loses track it says so and starts over: "the Changelog task marks that name range as out-of-sync... The Frontend task then aborts all accumulated state for that query and redoes the steps". None of them annotates an answer with *my index was this far behind*. They wait, catch up, or refuse. INFERRED: for Sid the honest form is that every pattern read returns the position it is complete up to, and that position goes into based-on. An index that cannot state its position cannot be read honestly at all.

**(7) and change history.** INSTITUTIONAL: history kept by application code was "never 100% reliable"; only the writer itself can "guarantee full coverage". F1 writes the history "as children of each root table" in the same transaction, so it adds no extra participants. A warning from the same paper: the consumers of history became the limit on write rate: "our rate limits are usually chosen to protect downstream Change History consumers who can't process changes fast enough". INFERRED: in Sid's store, tools matched to landed facts are exactly such consumers.

**(2) Ids.** INSTITUTIONAL: ids must not carry time, for a storage reason and not a privacy reason. Spanner docs: "A common cause of hotspots is using a key that monotonically increases or decreases, such as a timestamp." "We recommend using UUID Version 4, because it uses random values in the bit sequence. We don't recommend Version 1 UUIDs because they store the timestamp in the high order bits." Firestore: "Do not use monotonically increasing document IDs"; it says of itself that it "allocates document IDs using a scatter algorithm". Even an indexed time *field* hotspots. This holds for range-partitioned stores. TigerBeetle, a single-shard store, says the opposite (2.1). Which one applies to Sid depends on how Rama lays out the indexes, which the brief does not say.

**(9) Deletion, backups included.** INSTITUTIONAL ("Data deletion on Google Cloud"): four stages, with deadlines. "Google Cloud commits to delete customer data within a maximum period of about six months (180 days)." The two-month figure for live systems is a garbage-collection number: "typically enough time to complete two major garbage collection cycles". For log-structured storage, deletion means rewriting: "Compacting existing tables to overwrite deleted data can be expensive, as it requires re-writing tables of existing (non-deleted) data". The other road is cryptographic erasure, which "renders data unreadable by deleting the encryption keys", so that "logical deletion can be completed even before all deleted blocks of that data are overwritten in Google Cloud's active and backup storage systems", and for backups: "Without the encryption key that was used to encrypt specific customer data, the customer data is unrecoverable even during its remaining lifespan on Google's backup systems." INFERRED: of Google's two roads, only key deletion works in a store that never rewrites. It must be decided before the first record, because a value once written in the clear is in every backup from then on.

**(8) and (15) Who acts, and for whom.** INSTITUTIONAL (Google infrastructure security design overview): two identities travel on one call and both are checked. The service proves it is itself ("Each service that runs on the infrastructure has an associated service account identity"). Separately it proves it acts for a person: "the infrastructure lets Gmail present an end-user permission ticket in the RPC request. This ticket proves that Gmail is making the RPC request on behalf of that particular end user." The ticket is minted centrally and is short-lived: "the identity service returns a short-lived end-user context ticket". The reason for access is also logged: enforcement "includes audit logging, justifications, and unilateral access restriction". Humans and services live in one namespace: "Google engineers who need access to services are also issued individual identities." One honest caveat from the same page: "Access management between Google Cloud services is typically done with service agents rather than using end-user context tickets." So even inside Google the model is not uniform.
INFERRED for Sid: *by whom* is the immediate actor as itself (the agent is the agent). *Acts for* is a second, separately checkable statement with a limited life. If an agent writes as the person, then in a store that never rewrites, what the person did and what their agents did can never be told apart again.

**(14) Which motions become facts.** INSTITUTIONAL (Cloud Audit Logs): writes are always recorded, reads are opt-in. "Admin Activity audit logs are always written; you can't configure, exclude, or disable them." But: "Data Access audit logs are disabled by default because they can generate large volumes of data."

**(16) and (17) Before permissions exist; the first facts.** REPORTED (Burrows): in Chubby nothing is ever created without a policy, because creation inherits one. The rules live at "a well-known part of the cell's local name space". F1 re-reads its schema "from a well-known location in the key–value store". Spanner: "Database metadata, which includes schema versions and data placement information, is managed as data". INFERRED: the lineage's pattern for bootstrap is *well-known names*, the same in every deployment, plus *inherit on create*, so the state *a thing exists and no policy covers it* cannot occur after genesis.

**(3) and (13) Schema and format change.** INSTITUTIONAL (F1): when many servers hold a cached schema, there is no moment at which it changes everywhere. So (a) each writer must be fenced by the schema version it holds ("no write operation can commit if the schema it is based on has an expired lease"), and (b) changes must go through in-between states that are safe next to both neighbours (delete-only, then write-only, then public). INFERRED for Sid: the gate will run as many instances, each holding some version of a grammar fact. Each verdict must name the grammar version it checked. A grammar change that is not safe next to its previous version needs in-between versions.
Also Spanner's PITR doc ties history to schema: a longer retention means "schema versions must be retained for longer durations". In a store that keeps everything forever, every grammar version is needed forever. Since grammars are facts, that comes for free, as long as nothing ever reads an old fact with the newest grammar by default.
Firestore's rule for a service that changes under live traffic: "The default guarantee for updating Firestore is bug-for-bug API compatibility."

### 5. What resembles Sid's situation, what differs

Resembles: one logical store for the world; optimistic version-checked writes as the default; history written by the writer itself; policy and schema kept as data at well-known names; a company that kept operating its choices for fifteen years and wrote down the results.
Differs: Google has GPS and atomic clocks in every datacenter and its own network. Every store in this lineage deletes old versions (Spanner keeps one hour by default, seven days at most). None records what a write was based on. None is append-only forever.

### 6. Who they disagree with

- With Abadi (2.5): Spanner orders by clocks across many logs; Abadi says order should come from one global log and no clocks.
- With TigerBeetle (2.1): time-ordered ids are an anti-pattern here and a recommendation there.
- With FoundationDB (2.4): Spanner's authors regret a query layer bolted on from outside; FoundationDB's founders insist on exactly that layering.
- With their own past: Megastore 2011 against Firestore 2023 on whether small, fixed units of order are liveable.

## 2.7 Checking: Kyle Kingsbury (Jepsen, Elle) and Chris Newcombe (TLA+ at AWS)

**Who.** Kyle Kingsbury has tested some forty databases since 2013 and publishes every report; these are one person's words (REPORTED). Elle is a checker Kingsbury built with Peter Alvaro (VLDB 2020). Chris Newcombe brought formal specification to AWS; the 2014 report has six authors, and a 2025 follow-up by Marc Brooker and Ankush Desai says what changed (both INSTITUTIONAL where they speak for AWS).

They matter to Sid for two reasons. They say which promises systems like this break most often. And they show that some conventions in the record decide whether the store can ever be checked at all.

### 1. What they built

- **Jepsen** runs a real cluster, injects faults, records every operation a client tried and what it heard back, and then asks whether any legal history could explain it.
- **Elle** checks transaction isolation from such a record, without knowing the database's internals.
- **At AWS**, engineers write a precise model of a design, in TLA+ and later in P, and have a machine try every interleaving, before the code exists.

### 2. Their reasons, in their words

**What testing can show.** "Because Jepsen tests are experiments, they can only prove the existence of errors, not their absence. We cannot prove correctness, only suggest that a system is less likely to fail because we have not (so far) observed a problem." The two methods see different things: "Jepsen operates on real software, not abstract models. This prevents us from seeing pathological message or thread schedules", yet "testing executables often reveals implementation errors or subsystem interactions which a model checker would miss!"

**Every operation has three outcomes, not two.** From the Jepsen tutorial: an operation completes with "`:ok` if the operation succeeded, `:fail` if it didn't take place, or `:info` if we're not sure." A timeout is the third kind: "that process' operation is converted to an `:info` message, because we can't tell if it succeeded or failed." From the CockroachDB report: "in a normal function call, we assume that an operation either succeeds (and returns) or fails (returning an error, throwing an exception, etc). Across a network, however, there is a third possibility: an indeterminate result. Known failures can simply be retried, but indeterminate results require careful handling to avoid losing or duplicating operations."

**Wall-clock time is not an order.** Kingsbury, 2013: "Timestamps, as implemented in Riak, Cassandra, et al, are fundamentally unsafe ordering constructs. In order to guarantee consistency you, the user, must ensure locally monotonic and, to some extent, globally monotonic clocks. This is a hard problem, and NTP does not solve it for you." The fix and its price: "The safest option I can think of is to use a strong coordinator for your timestamps, like an atomic incrementing counter in Zookeeper. That's slow and limits your availability". Elle says the same from theory: "We might consider deducing the version order from the real-time order in which writes or commits take place, but Adya et al explicitly rule this out, since optimistic and multi-version implementations might require the freedom to commit earlier versions later in time."

**Why write the spec first (Newcombe and colleagues).** Engineers designing the happy path stop short of rare combinations: "Almost always, the engineer stops well short of handling 'extremely rare' combinations of events, as there are too many such scenarios to imagine. In contrast, when using formal specification we begin by precisely stating 'what needs to go right?'" The result: "We have found this rigorous 'what needs to go right?' approach to be significantly less error prone than the ad hoc 'what might go wrong?' approach."
The example everyone cites: "the shortest error trace exhibiting the bug contained 35 high level steps. The improbability of such compound events is not a defense against such bugs... The bug had passed unnoticed through extensive design reviews, code reviews, and testing".
It was cheap to start: "Engineers from entry level to Principal have been able to learn TLA+ from scratch and get useful results in 2 to 3 weeks". And it gave courage: "we have been able to make innovative performance optimizations – e.g. removing or narrowing locks, or weakening constraints on message ordering – which we would not have dared to do without having model checked those changes." A side benefit: "a precise, testable, well commented description of a design is an excellent form of documentation."

### 3. What they later changed or conceded

**a. The gap between the model and the code.** AWS, 2014: "On learning about TLA+, engineers usually ask, 'How do we know that the executable code correctly implements the verified design?' The answer is that we don't." AWS, 2025: "we take structured logs from the execution of distributed systems and validate post-hoc that they match behaviors allowed by the formal P specification of the system. This allows for bridging the gap between the P specification of the system design and the production implementation". The way across the gap was the system's own record of what it did, checked afterwards against the model.

**b. Why AWS moved past TLA+ alone.** Not because it failed: "many engineers struggled to learn and become productive with TLA+." The 2025 toolbox: "model checking, fuzzing, property-based testing, fault-injection testing, deterministic simulation, event-based simulation, and runtime validation of execution traces", with "formal specifications as test oracles". They also check isolation after the fact "following approaches such as Elle".

**c. What models still miss.** 2014: "We don't yet know of a feasible way to model a real system that would enable tools to predict such emergent behavior" (retry storms, slow collapse). 2025: "metastable failures remind us that systems have a variety of behaviors that cannot be neatly categorized this way", and the trouble "can be triggered in most systems with timeout-and-retry client logic."

**d. The S3 team's lighter method, and one admitted miss.** They write a tiny executable model next to the real code: "the reference model for a log-structured merge tree implementation is a hash map", and they test that the real thing behaves like it. It "prevented 16 issues from reaching production". The miss: a cache was set very large in every test, so the miss path never ran. Their warning about test generators: "biasing introduces the risk of baking our assumptions into our tests, when our goal in adopting formal methods is to invalidate exactly such assumptions".

**e. Jepsen corrects itself.** The Bufstream report withdrew one of its own claims that had rested on a vendor's documentation. The ethics page allows for this in advance: "Jepsen's test harnesses and libraries have bugs... These could lead to false negatives and false positives."

### 4. Which questions they speak to

**The premise *one writer*, and (10) two gates.** REPORTED, and the single most useful finding here. Datomic is the nearest checked system to Sid's store: one transactor, immutable facts, as-of reads. Its documentation argued that isolation follows from having a single writer. Kingsbury: "This is wrong in two senses." During failover, "There may be times when a standby transactor believes it should take over, but another active transactor is still running. This means transactions may actually execute concurrently. During this window Datomic is not a single-writer system, but a multi-writer one!" And then: "Thankfully this doesn't matter: Datomic's safety property follows directly from the Sequential consistency of the storage system's CaS operation. Any number of concurrent transactors ought to be safe."
INFERRED for Sid: *the gate is the only writer* is a claim no one can keep at every instant, so safety should not rest on it. It should rest on the compare-and-set that every offer already carries. The design is then safe with any number of gates, and running only one is a matter of speed. What needs checking is that the compare-and-set on a cell is truly sequential under failover. In Sid's case that is a claim about Rama.
REPORTED, in support, from the etcd report: locks alone cannot protect a remote resource; "one must rely on the storage system itself to ensure transactional correctness, or, if the lock service provides one, use a fencing token of some kind, which is included with every operation a lockholder performs". With short leases and pauses Kingsbury "could reliably induce the loss of ~18% of acknowledged updates."

**(6) Does 37 say it replaces 25?** REPORTED: this convention decides whether the store can be checked from its own record. Elle's problem: "we cannot tell, because we lack a key component in Adya's formalism: the version order." Blind overwrites are the cause: "In a sense, blind writes to a register 'destroy history'. If we used a compare-and-set operation, we could tell something about the preceding version, but a blind write can succeed regardless of what value was present before." Their two cures are *recoverability*, "every version we observe can be mapped to a specific write in some observed transaction", and *traceability*, where a version carries its own past so that a read "tells us the order of all versions written prior".
INFERRED for Sid: a fact that names the version it replaced is traceable, and a fact with its own unique id is recoverable. With both, a checker can walk every cell's chain from the facts alone, trusting no index, and can prove a fork (two facts that both claim to replace 25). Without the back-pointer, a fork looks like two ordinary later versions. A real case of getting this bookkeeping wrong: PostgreSQL's serializable mode was broken for nine years because its conflict check could "incorrectly identify an updating transaction's transaction ID (XID) as responsible for both the original and updated versions of a tuple".

**(7) Verdicts and refusals.** REPORTED: the usual way systems lose or double data is by turning *I don't know* into *it failed* and trying again. CockroachDB: "a low-level RPC retry mechanism converted an indeterminate network failure to an (apparently) definite logical failure, allowing a higher-level transaction state machine to retry the entire transaction, applying it twice." MongoDB: "we repeatedly observed arrays with multiple copies of the same element." TiDB: "two auto-retry mechanisms which blindly re-applied updates when a transaction conflicted." Kingsbury asks that error lists say which kind each one is: "a table of which codes indicate determinate vs indeterminate failures".
A half-made verdict is worse than none. Bufstream: the refusal "did not set the offset for the sent record to the special value -1, which some clients relied on as a signal of an error. Consequently, the official Java client we used in our tests interpreted this error as a successful response with offset 0."
INFERRED for Sid: an offerer whose connection drops holds an indeterminate result. If every offer has an id made by the offerer, and the gate keeps one verdict per offer id, refusals included, then *what happened to my offer?* always has a definite answer. If refusals are not kept, *no fact found* can mean refused, lost, or still on its way. The verdict should be one whole thing, the yes or no together with the version given, and must never rely on an empty or special value to mean no.

**(11) When, and order.** REPORTED: never order by wall clock. A number that doubles as an identity is a further trap. CockroachDB: "when two transactions T1 and T2 have the same timestamp, and access the same key, they are considered equivalent: the transaction ID is not used to discriminate... This anomaly could occur whenever timestamps collide—for instance, due to clock offset, including those well below CockroachDB's threshold." Clock alarms cannot save a design that leans on clocks: "there will always be a few-second window during which transactional anomalies can occur."

**(5) Lagging reads.** REPORTED: stale reads from copies that are behind are among the commonest findings, sometimes with no fault injected at all. Redis-Raft served stale reads because new leaders failed "to issue a no-op operation upon coming to power". AWS RDS MySQL "routinely violated Serializability at Serializable isolation, even in healthy clusters." In Datomic the lag is documented and there is an opt-in to being current: "Calling d/sync forces the client to synchronize with the transactor, preventing stale reads." INFERRED: a derived index that lags is normal. The failure is an answer that does not say what position it reflects.

**A gate checks on the way in only.** REPORTED (Datomic): "not all schema constraints apply to extant data. In particular, attribute predicates are only enforced on newly-added datoms, not on existing datoms." INFERRED for Sid: when a grammar fact gets a new version, old facts are not re-checked, and cannot be corrected. So every fact should say which grammar version it passed, or a reader will assume the newest.

**Many checked writes are not one checked write.** REPORTED (Datomic): "A set of transaction functions might be correct when executed in separate transactions, but incorrect when executed in the same transaction!" INFERRED: if one offer ever carries several facts, whether the gate checks each against the state before the batch, or against the facts ahead of it in the same batch, is a convention to fix early. Datomic's choice surprised its users.

**Claims.** REPORTED: marketing and documentation overstate, again and again (MongoDB's "full ACID transactions" against a failure "to preserve snapshot isolation"; Fauna in 2.5; unsafe defaults). Sid's principle that the map must not lie is, in Kingsbury's terms, a claim that has to come with its own test.

**How to check Sid's conventions before building on them.** INFERRED, put together from the above:
1. Write the envelope's rules as a small formal model before the first record. State what must go right. Examples: each cell's replaces-chain is a single line with no forks; every offer id has exactly one verdict; a verdict never names a policy or grammar version newer than its own position; no based-on entry points past its own *as of*. Then run the model with two gates and a failover in the middle, and see which rules break.
2. Keep a tiny reference model of the gate (AWS's "hash map") and test every rebuilt runtime against it. With hundreds of rebuilds ahead, this is the cheapest standing proof that a rebuild did not change what the gate means.
3. Make the log checkable from itself, as Elle needs and as AWS later did with PObserve: unique ids, back-pointers, complete verdicts. A never-rewritten store built that way can be audited against its model forever, by anyone, with no access to the runtime.
4. Test the real thing under faults. The model cannot see the implementation, and the implementation test cannot see rare schedules.

### 5. What resembles Sid's situation, what differs

Resembles: Sid wants staleness and doubt to be computed from the record. These people compute correctness from the record. The needs are the same: unique names, explicit chains, complete verdicts, positions on reads.
Differs: they test other people's databases for days; Sid needs the property for decades and across runtime rebuilds. None of them addresses provenance of reads or layers.

### 6. Who they disagree with

- Kingsbury against TigerBeetle on retrying forever (2.1): hiding both kinds of error in an endless loop "complicates error handling".
- Kingsbury against vendors' claims, as a standing matter.
- Model checking against testing is not a fight: each side names what the other sees. AWS now uses both, and Elle as well.
- On FoundationDB we could not get Kingsbury's own words. The remark that FoundationDB's testing outdid Jepsen's exists only as a tweet we could not retrieve; we found it described second-hand in two places, and we do not quote it.

## 2.8 Ids as a written lineage: Twitter's Snowflake and RFC 9562

This is short. It is here because *IDS* is in my group's title, and because the reasoning is written down by people who lived with their choice. I read these sources myself.

**Twitter, 2010 (Ryan King, "Announcing Snowflake", and the project README).** Twitter needed ids made without coordination: "We needed something that could generate tens of thousands of ids per second in a highly available manner. This naturally led us to choose an uncoordinated approach." The ids had to sort roughly by time, because clients already used an id as a bookmark: "We have a number of API resources that assume an ordering (they let you look things up 'since this id')." They could only promise rough order: "although the tweets will no longer be sorted, they will be k-sorted." And they were stuck at 64 bits: "We've been through the painful process of growing the number of bits used to store tweet ids before. It's unsurprisingly hard to do when you have over 100,000 different codebases involved." With so few bits, the id had to say *where* it was made as well as *when*: "we settled on a composition of: timestamp, worker number and sequence number", and "worker numbers are chosen at startup via zookeeper". They looked at standard UUIDs and passed: "all the schemes we could find required 128 bits."
Three lessons, all REPORTED. Changing the width of an id after others depend on it is very painful. An id that reveals *where* is what you get when there are too few random bits to do without it. And once an id sorts by time, people use it as a cursor, even though it is only roughly ordered.

**RFC 9562 (Davis, Peabody, Leach; IETF, 2024), the standard that added time-ordered UUIDs.**
- Why they added time-ordered ids: "UUID versions that are not time ordered, such as UUIDv4... have poor database-index locality."
- Why ids should not say where: "Privacy and network security issues arise from using a Media Access Control (MAC) address in the node field of UUIDv1. Exposed MAC addresses can be used as an attack surface to locate network interfaces and reveal various other information about such machines". Even for clusters: "The node id SHOULD NOT be an IEEE 802 MAC address". A central registry for uniqueness is "NOT RECOMMENDED".
- On revealing when: "Timestamps embedded in the UUID do pose a very small attack surface. The timestamp in conjunction with an embedded counter does signal the order of creation for a given UUID and its corresponding data but does not define anything about the data itself or the application as a whole. If UUIDs are required for use with any security operation within an application context in any shape or form, then UUIDv4 (Section 5.4) SHOULD be utilized."
- Treat ids as opaque: "As general guidance, avoiding parsing UUID values unnecessarily is recommended; instead, treat UUIDs as opaquely as possible."
- An id is not a key to a door: ids "MUST NOT be used as security capabilities (identifiers whose mere possession grants access)."
- Against ids computed from meaningful content, for question (1): "A common issue observed in database schema design is the assumption that a particular value will never change, which later turns out to be an incorrect assumption." Hence: "The general advice is to avoid name-based UUID natural keys".
- How bad is a clash? They give two poles. Low: "A UUID collision generated a duplicate log entry". High: "A duplicate key causes an airplane to receive the wrong course... Collisions must be avoided: failure is unacceptable."
- Who makes the id: "Applications using a monolithic database may find using database-generated UUIDs (as opposed to client-generated UUIDs) provides the best UUID monotonicity." They prefer the database for the sake of order. TigerBeetle, FoundationDB and the checked Rama fact all prefer the client for the sake of safe retries. This is the same split again: the name and the order are different jobs.

**What this adds for question (2).** INFERRED: with 128 bits nobody in this lineage needs an id to say where it was made. Whether it may say when is a real trade. Speed of index inserts argues for it (RFC 9562, TigerBeetle). Hot spots on range-split stores argue against it (Spanner, Firestore). So does the small leak: an id given out to someone who may not see the fact still tells them when it was made, and in Sid's store an entity id is "for life" and travels outside the store as an anchor. One asymmetry is worth weighing. If random ids turn out to be slow, the cost sits in derived indexes, which can be rebuilt. If time-bearing ids turn out to leak, nothing can be done, because the ids are in every fact forever.

# 3. Question by question

Sid's numbers, in the brief's order. Each entry says what this camp reports, who differs, and what that implies before the first record. The evidence and sources are in section 2; this section only gathers it. Anything about Sid's store is INFERRED unless marked.

### (0) The log: never rewritten, or only never lost?

- REPORTED: the systems that promise immutability promise it for *committed records*, not for the physical log. TigerBeetle: "Committed ops are immutable", while "Uncommitted ops may be replaced", and the log itself "is a ring buffer". FoundationDB throws away the unacknowledged tail at every recovery. Restate trims its log to a snapshot. Temporal deletes histories after a retention period.
- REPORTED: TigerBeetle makes the promise checkable: "immutable, checksummed, and hash-chained". It also repairs damaged blocks from other replicas, so bytes do get rewritten in order to keep a record the same.
- REPORTED: formats change underneath. Spanner replaced its inherited storage format. The brief says a Rama depot migration can touch every record.
- INFERRED: the camp would call both halves of the question too weak. The promise worth fixing has three parts. One: once a fact's verdict is durable, its identity and content never change (its bytes may be re-encoded or repaired). Two: each fact carries what a reader needs to detect a change, meaning a checksum of its own content and a pointer to what it replaced. Three: the one planned exception, erasure, goes through a designed path and is itself a fact.
- Second store: each store has its own chain. Nothing here forces two stores to share one.

### (2) Entity: how is an id made so two never clash? May it reveal when or where?

- REPORTED, with no dissent: the offerer makes the id before the first attempt and keeps it for retries. TigerBeetle: generate it in the client, "persisted locally before submission". FoundationDB: "Avoid generating IDs within the retry loop." This agrees with the checked Rama fact in the brief.
- REPORTED: 128 bits, no central service (TigerBeetle: "Avoid requiring a central oracle"; RFC 9562: a central registry is "NOT RECOMMENDED"). Twitter's pain came from too few bits and from widening ids later.
- REPORTED: never *where*. RFC 9562 on machine addresses inside ids; Snowflake needed a worker number only because it had 64 bits.
- REPORTED, and split, on *when*. For it: RFC 9562 ("poor database-index locality" without it) and TigerBeetle ("Random identifiers are not recommended"). Against it: Spanner ("We don't recommend Version 1 UUIDs because they store the timestamp in the high order bits") and Firestore (its own allocator scatters). RFC 9562 calls the leak "a very small attack surface" and still sends security uses to random ids.
- REPORTED: reserve the edge values (TigerBeetle forbids 0 and the maximum). Use one id space for everything. Never let holding an id grant anything (RFC 9562).
- INFERRED: the asymmetry from 2.8. A slow index can be rebuilt. A leaky id cannot be recalled. Which way Rama's index layout pushes is not known from the brief.
- INFERRED, tentative, for bulk ingest: keep the *entity* id random, and make the *offer* id repeatable from (ingest lane, source key, source version). Then a re-run of the ingest is recognized as a retry, while identity stays free of natural keys, which RFC 9562 warns will one day change.
- Second store: random 128-bit ids from two stores do not clash. Gate numbers do (see 1).

### (17) The first facts

- REPORTED pattern in Google's lineage: bootstrap material lives at *well-known names that are the same in every deployment*. Chubby's ACL files sit in "a well-known part of the cell's local name space". F1 re-reads its schema "from a well-known location". Spanner: "Database metadata... is managed as data". RFC 9562 likewise publishes fixed namespace ids.
- REPORTED: each store also needs an identity of its own that differs from every other store's. TigerBeetle puts the cluster's 128-bit id in every message: "The cluster number binds intention into the header, so that a client or replica can indicate the cluster it believes it is speaking to, instead of accidentally talking to the wrong cluster (for example, staging vs production)." Apple had to add a per-move *incarnation* to every record once data moved between clusters.
- REPORTED: a new term of office for the writer starts with a record in the stream. FoundationDB's first transaction after recovery is "a special recovery transaction". Restate's new leader "appends a message to the log to signal their epoch is now active".
- REPORTED: in Chubby nothing is created without a policy, because creation inherits one.
- ABSENT: the Zanzibar paper says nothing on who writes the first rules.
- INFERRED: two kinds of first fact. *Constants*: the ids of the first kinds (the key that means *name*, the grammar of grammars, base, the actor kinds). These should be the same in every store and published, so that tools and facts can move between stores. *Identity*: this store's id and incarnation, its gate's actor id, its genesis. These must differ in every store. The genesis writer acts before any policy exists, so its verdicts should say *genesis rule*, not pretend that a policy allowed them. After genesis, inherit-on-create means no fact ever exists uncovered by a policy.

### (3) Key: a word, or an id with its name and shape as facts?

- REPORTED: Google's own wire format settled this long ago. Numbers identify fields, and names are for people. Record Layer: "Serialization is based on field numbers, not field names", and "Under no circumstances should a new field be added with an existing field number". TigerBeetle's `code` and `ledger` are numbers whose meaning lives elsewhere.
- REPORTED: a rename is only free where nothing stored uses the word. The Record Layer's index definitions used names, so a rename had to chase them.
- REPORTED: names freeze at first adoption. Kissner on Zanzibar's syntax; Temporal's change names that are used up forever.
- INFERRED: Sid's based-on entries store patterns, patterns name keys, and nothing stored is ever rewritten. So whatever names a key inside a stored pattern is pinned for good. If that is the word, the word can never change or be split. If it is an id, the word is just another fact, with versions. The price: a raw fact cannot be read without the facts about its key, and the first key ids must be well-known constants (17).

### (9) Value: never removed, so how is one deleted, backups included?

- REPORTED, two roads. TigerBeetle: there is no deletion, so whatever might need erasing stays out of the store, and the record holds a pointer. Google: a pipeline with deadlines ("about six months (180 days)") and *cryptographic erasure*, under which data in backups "is unrecoverable even during its remaining lifespan on Google's backup systems". Temporal's payload codec is the same idea run by the customer.
- REPORTED: Google's other road, compaction, means "re-writing tables of existing (non-deleted) data". A store that never rewrites cannot take it.
- INFERRED: only deleting a key, or keeping the value outside, works here. Either must be chosen before the first record, because a value once written in the clear is in every backup. The erasure is itself a fact. The erased fact's id and envelope stay, so *who read the erased thing* still works by walking based-on.
- INFERRED, my own caution, not from a source: if a fact were named by a hash of its plain content, the name would outlive the erasure, and short values can be guessed from their hash. This argues against content-hash names for anything erasable. A checksum used only for integrity can be computed over the stored (encrypted) form.

### (8) By whom: who checks it? Is an agent itself, or the person? Where is *acts for*?

- INSTITUTIONAL (Google): two identities on every call. The immediate caller proves it is itself. Separately it presents a short-lived ticket that "proves that Gmail is making the RPC request on behalf of that particular end user." Humans and services share one namespace of identities. The reason for access ("justifications") is logged too.
- REPORTED (TigerBeetle): none of this is in the record; an outer tier handles "authentication and authorization".
- REPORTED (Temporal): events record the worker's `identity`, which is the process that did the work and not the end user.
- INFERRED: *by whom* names the immediate actor as itself. *Acts for* is a second statement, with a scope and a limited life, checked by the gate and named in the verdict. The check of identity depends on things outside the log, so the gate records its result and does not try to re-derive it later.

### (11) When: whose clock? Ever used for order?

- REPORTED, no dissent on whose: the writer's clock, never the offerer's. Bigtable allowed client timestamps; Spanner took that back.
- REPORTED on order. Kingsbury: wall-clock timestamps are "fundamentally unsafe ordering constructs". Abadi: "There is no need for clock time to be used in order to create a notion of before or after." Elle: real-time order is ruled out as a source of version order. Only Spanner orders by time, with GPS and atomic clocks and a wait on every commit. TigerBeetle and FoundationDB hand out a counter from one point and keep it near real time.
- REPORTED: make *when* move forward together with order inside one ordered stream, so the two never disagree (TigerBeetle: "strictly monotonic").
- REPORTED: a second *when* becomes necessary. TigerBeetle's import mode, then its 2026 teaching: "Recorded: when the fact was learned, and Effective: when the fact took place."
- REPORTED: deterministic work must get time as an input. TigerBeetle injects it and logs a `pulse`; Calvin's preprocessor freezes it; Temporal serves it from the history.
- INFERRED: fix two whens now, the gate's admission time and the offerer's claim of when it happened or was made. Never order by either. Stamp the gate's when once, as part of the ordered input, not by reading a clock inside code that may be retried. Let the passing of time land as a fact where any running answer depends on it.

### (16) Layer: who sees a fact before any permissions exist?

- REPORTED (Zanzibar): visibility must be judged at a snapshot no older than the thing being shown. Stale rules are harmless for unchanged content and harmful for new content.
- REPORTED (Chubby): creation inherits policy, so nothing is ever without one.
- ABSENT (Zanzibar): bootstrap.
- INFERRED: after genesis, *before any permissions exist* should not be a reachable state. A layer is created with a policy naming at least its creator, and facts inherit their layer's policy. To show a fact, evaluate policy at a position no older than the fact's own. Kissner's caution about override rules applies if a layer is ever used to hide a fact rather than to add one.

### (10) Order: two gates? What shares a partition? Is *as of* one number?

- REPORTED: two gates will both try at some failover, whatever the design says (Kingsbury on Datomic). What saves the data is a sequential compare-and-set in storage, plus something in each append that lets the log refuse a stale writer (Chubby's sequencer, Restate's epoch).
- REPORTED: what shares an ordered unit is a choice made early that cannot be unmade. Megastore 2011 against Firestore 2023; Datastore: "The entity group relationship can not be changed after entity creation." Restate keeps all of one subject's work in one partition.
- REPORTED price list for *as of* as one number: one sequencer (FoundationDB, within a region); one global batched log (Calvin, a batch wait on every write, across regions); clocks with hardware and a commit wait (Spanner); or one row that every write touches (SpiceDB on CockroachDB). Even then, each partition has to prove it has caught up, and idle ones need heartbeats (Spanner every 8 seconds, Firestore every few milliseconds).
- REPORTED: Zanzibar made its *as of* an *opaque token* "to allow future extensions".
- INFERRED: the compare-and-set cell (entity + key + layer) must live in one ordered unit. That is the one hard need. Whether a whole layer does is a cost decision. It does not have to be made now if *as of* in based-on is an opaque token minted by the store, because the token can hold one number today and a position per partition, or a store id and incarnation, later. A bare number in the first record can never be widened.

### (11) Based on: is every read listed?

- REPORTED precedent for yes: FoundationDB tracks every read of every transaction, as ranges, for five seconds. FaunaDB writes each write's reads into the log.
- REPORTED precedent for no: Temporal records no reads ("never add entries") and still caps a history at 51,200 events. Google's audit logs leave reads off by default "because they can generate large volumes of data". F1 found that the consumers of history set the limit on write rate.
- REPORTED: what started a piece of work is always recorded. In Temporal that is the start event or a signal. In Sid's terms that is because-of, not based-on.
- REPORTED: whatever the platform feeds to deterministic code (time, random values, results) is recorded, because replay needs it. By that rule, whatever the runtime adds to what a model is shown must be recorded, or be re-derivable under a named build.
- INFERRED: nobody in this camp has run *every read, kept forever*. A shape that keeps it affordable: a model's reply is based on one thing, the crossing fact that says what it was shown, and that crossing fact holds the reads. Then each offer carries a pointer, not a list.

### (4) Based on: depends-on or only-how-I-got-here? Follow the latest or stay?

- REPORTED: FoundationDB has both kinds of read. Snapshot reads "selectively relax FoundationDB's isolation property, reducing conflicts but making it harder to reason about concurrency." And hand-marking them is a bug source ("very hard to find"), so it belongs in abstractions.
- REPORTED: Temporal ended with two declared behaviours, Pinned and Auto-Upgrade. SpiceDB lets each read state its mode: at least as fresh as a token, exactly at a token, or fully consistent.
- INFERRED: yes, mark each read. Without the mark every glance makes a fact look stale and the signal loses its worth. Take the mark from the tool's definition, not from each actor's judgment. A fact's read of another fact is pinned by nature; following the latest is what running answers do. The entry should be able to say how it was read (exact version, at least as fresh as, latest at a position), even if the first runtime writes only one of these.

### (5) Based on: if an index lagged, is how far it had got written down?

- REPORTED, three generations at Google plus Zanzibar and Apple: never answer from behind. Merge a timestamped delta up to the query's position (Leopard). Catch up from the change history or wait (F1). Refuse and start over when position is lost (Firestore's "out-of-sync"). Do not read an index that is not yet "readable" (Record Layer).
- REPORTED: indexes learn their position from the change feed's heartbeats.
- REPORTED: where Jepsen found trouble in FaunaDB was exactly here: "multiple snapshot isolation violations in temporal and indexed queries".
- INFERRED: the entry records the position the answer is complete up to, as the opaque token from (10). *How far behind was the index* is then not a separate thing. An index that cannot state its position cannot be read honestly at all.
- REPORTED (F1): record the pattern and the position, never the rows. Version checks on rows miss "Insertion phantoms".

### (11) Because of: always filled; empty only at the start of a chain?

- REPORTED: values that double as signals came back to hurt. TigerBeetle broke its API to stop using a zero amount as a signal. In Bufstream a refusal without its special value was read as success.
- REPORTED: Temporal starts every history with an explicit start event, and linked runs name their parent.
- INFERRED: an empty slot can mean *starts a chain*, or *older envelope*, or *bug*. An explicit start mark keeps those apart.

### (1) Version: is a fact pointed at by the gate's number, or by its own id?

- REPORTED: lasting systems have both and point by the offerer-made name. TigerBeetle: client id for pointers, cluster timestamp as the internal key. Record Layer: own primary key, with the version stored beside it because "the version is only known upon commit". Temporal: a client-chosen id, while the server-made one is not to be stored.
- REPORTED: a writer-made number breaks across stores, moves and restores (Apple's incarnation; six years of FoundationDB forum threads). A FoundationDB commit version does not even name one transaction, because a batch shares it. CockroachDB treated equal timestamps as equal identity and lost isolation.
- REPORTED on content-derived names: RFC 9562 advises against ids computed from meaningful values.
- INFERRED: three fields for three jobs. An id made by the offerer names the fact (and the offer), so it works before admission, across retries, and across stores. The gate's number gives order. A checksum gives integrity. If the gate's number appears in any pointer, give it room for a store id and incarnation from the first record. Random over content-derived, for the erasure reason in (9).

### (6) Version: when 37 replaces 25, is *replacing 25* kept on 37?

- REPORTED: FoundationDB ships the previous number with each new one so that gaps can be detected. TigerBeetle's log entries carry their parent's checksum. Temporal's forks and continuations name where they came from.
- REPORTED (Elle): without the link, a checker "cannot tell" which write replaced which. Blind writes "destroy history". A compare-and-set write reveals its predecessor, and versions that carry their past make the whole order provable.
- INFERRED: keep it. The offer already states the version it expects and the gate already checks it, so the cost is one number. It makes each cell's history a chain that can be verified from the facts alone, and it makes a fork provable, which matters the day a second store appears.

### (7) Beside each fact: is the gate's yes or no kept? Refusals?

- REPORTED for: TigerBeetle added both after living without them (3a and 3b in 2.1). Temporal keeps failures as events. F1: only the writer can "guarantee full coverage" of history. Kingsbury: indeterminate results are normal, and systems lose data by guessing; a verdict must be whole.
- REPORTED against: FaunaDB keeps the reads and works the verdict out again on every replica.
- REPORTED nuance: TigerBeetle remembers only refusals that depended on the state of the store.
- INFERRED: keep the verdict, keyed by the offer's id, refusals included. Sid's gate lives in the runtime, which is not made of facts, so by Sid's own rule a verdict is not re-derivable from facts. A verdict names the grammar version, the policy fact and the position it was read at, the expected version, and the gate's build. Where refusals live matters for visibility, and this camp has nothing on that.

### (12) Start of a session: runtime version, kind of machine? Are rebuilds facts?

- REPORTED: TigerBeetle writes the release into every log entry, makes the upgrade an entry, and never replays an entry under different code. Temporal writes the worker's build id on every completed task. Restate pins each invocation to an immutable deployment that includes "code, prompts, tool definitions, schemas, guardrails, and model configuration".
- REPORTED the other way: FoundationDB and Firestore record nothing per record and instead hold behaviour still ("on-disk data is compatible"; "bug-for-bug API compatibility").
- INFERRED: per session is too coarse. The stamp belongs on each verdict and each crossing. A rebuild must be a fact, for two reasons. It is the only way to name the build that a past answer depended on. And a rebuild moves no other fact, so nothing would ever recompute after one.

### (14) The hand: which motions become facts? Is being shown the same as looking?

- REPORTED: everywhere in this camp, reads and looks are not recorded by default, for volume. Temporal's queries; Google's Data Access logs.
- REPORTED: acts that change something always are (Google: Admin Activity logs "are always written; you can't configure, exclude, or disable them").
- INFERRED: a crossing to a model is unlike a crossing to a screen. The exact bytes matter, and the reply is an offer that stands on them. This camp has little more on the hand.

### (15) A click: which tools may act in a person's name?

- INSTITUTIONAL (Google): a service acts for a person only while holding a short-lived ticket minted from that person's live credential, and passes the ticket down the chain of calls. Google's Cloud side mostly uses service agents instead, so even Google is not uniform.
- INFERRED: a delegation is a fact with a scope and a limited life, checked by the gate, named in the verdict. This camp is thin here.

### (13) Storage: plain maps or classes? Ever trimmed? Backups?

- REPORTED: TigerBeetle uses fixed structs with reserved room that must be zero, and evolves by new operation numbers while old ones stay frozen and callable. Record Layer: add only, never reuse a number, and beware the encoding's own version ("no clear upgrade path" from proto2 to proto3).
- REPORTED: Spanner inherited a self-describing format, "It is self-describing and therefore highly redundant", and left it. The logical record and its physical form are separate decisions, and only the first is forever.
- REPORTED on trimming: everyone here trims except TigerBeetle's records.
- REPORTED on backups: a restore can break what writer-made numbers promise (FoundationDB). A restore or a move is an event that must leave a mark (incarnation).
- INFERRED: decide the logical envelope and its evolution rules now. Leave the physical encoding free to change. Give the envelope a version and spare room. Treat restore as something that mints a new incarnation.

# 4. The voices that matter most, who was dropped, who is missing

**The three that matter most for Sid.**

1. **TigerBeetle (Joran Dirk Greef and team).** It is the nearest thing to Sid's record that has been run for years under a no-rewrite rule: small immutable facts, one writer that stamps time, offerer-made ids, a fixed envelope. Its value is the public list of what that rule forced them to add later: kept refusals, a verdict for every event, a second clock for imported history, a way to close what cannot be deleted, the code release on every log entry. Each of those is one of Sid's open questions, answered by regret.

2. **Temporal and Restate (Maxim Fateev; Stephan Ewen, Jack Kleeman and colleagues).** They are the only people here who have lived with Sid's central sentence, that derived things are re-derived from the record and never stored, while the code that re-derives keeps changing. Their decade is a record of how hard that is. Every remedy they found assumes that the past eventually goes away.

3. **Kyle Kingsbury, with Elle.** Three reasons. Kingsbury showed on the system nearest to Sid's (Datomic) that *one writer* is not something safety can rest on, and what it should rest on instead. Elle shows that small conventions in the record (unique names, a pointer to the replaced version) decide whether a store can be checked from its own history at all. And the reports are the best evidence there is on which promises break.

**One idea from a fourth: Zanzibar's opaque *as of* token.** It lets Sid put off the hardest structural choice (one order or many) without painting the first record into a corner.

**Who I went lighter on, and why.**
- *Calvin.* The idea matters (order first, then deterministic checking), and Sid's offers happen to fit its one great restriction. But it says nothing about code that changes over time, and its production descendant dropped the defining demand.
- *FoundationDB's founders.* I could not get Rosenthal's or Scherer's own words. The paper, docs, forum and Will Wilson's essay had to stand in.
- *Bigtable and Megastore internals.* I used only their lessons and the reversal between generations.
- *Himeji and OpenFGA.* Their write-ups do not engage with consistency, which is a finding in itself (2.3).
- *CockroachDB.* Used only as Abadi's counterpart.

**Who is missing from the list I was given.**
- *Rich Hickey and Datomic.* The closest existing system: immutable facts, one transactor, attributes that are themselves entities, as-of reads. I assume research-4 has it. I used only Kingsbury's test of it.
- *Pat Helland.* Helland has written for twenty years on exactly (2), (7) and (0): identifiers, idempotence, and immutable data. I did not read those pieces for this report.
- *Verifiable logs (Certificate Transparency, Trillian: Ben Laurie, Al Cutter and others).* They are about proving to an outsider that a log was never rewritten. That is the strong form of (0), and nobody in my group addresses it.
- *Leslie Lamport.* The origin of both halves of this group: logical clocks for order, and TLA+ for checking.
- *Hybrid logical clocks (Sandeep Kulkarni, Murat Demirbas).* The usual answer to *whose clock* for people without Google's hardware.
- *Delos and the shared-log line (Mahesh Balakrishnan and colleagues).* Restate cites it. It is about swapping the log's implementation under a running system, which bears on a runtime that will be rebuilt many times and on a second store.
- *Bitemporal databases (Richard Snodgrass; XTDB).* Two whens, studied for decades.
- *Practitioners of key-deletion erasure in event-sourced systems.* Most relevant to (9).
- *Dominik Tornow.* TigerBeetle points to a piece by Tornow defending "requests never time out"; I did not fetch it.

# 5. What this camp would question above the table

These go at the ALSO OPEN list. The first is the strongest challenge my camp makes.

***Running answers are never stored; only crossings are recorded.***
Nobody here would accept *deterministically re-derivable* as a fixed property of a tool. They would call it a claim about a pair: this tool version on this runtime build. TigerBeetle trusts it so little that it never lets new code replay an old entry. Temporal can detect a break only because it keeps the old outputs to compare with. And every way out that Temporal and Restate have found (delete old branches once old runs "have left retention", shut old builds when their runs end, cut long histories) depends on the past ending. Sid's store is defined by the past never ending, and its runtime is the one thing not made of facts. So they would ask Sid to choose openly among three things, and would say the choice has to be made before the first record because it decides what a crossing fact must hold:
1. Keep every tool version and every runtime build runnable forever. Restate tried this and wrote down the cost: "dependency updates, security patches for long-lived versions".
2. Promise behaviour that never changes across rebuilds, as Firestore does ("bug-for-bug API compatibility"), and prove it on every rebuild against a reference model, as AWS does.
3. Accept that re-derivation is promised only for the present, and let crossings carry enough to check a later re-derivation (a digest of what was shown) or to skip it (the content). With no recorded output, a rebuild that changes an old answer cannot be seen by anyone, and the map would then lie without anyone knowing.
A fourth point: time. An answer that depends on *now* is not a function of its reads unless time enters as a fact.

***Every read recorded as provenance on every fact.*** Nobody in this camp has run it. FoundationDB keeps read sets for five seconds because of memory. Temporal records no reads and still caps a history at 51,200 events because replay and storage suffer. Google leaves read logs off by default. F1 found that those who consume history set the pace of writing. They would ask for a budget (bytes of provenance per byte of content, at machine-rate agents) and a structure (point at the crossing; do not list reads on every fact). They would also ask which later question each recorded read is meant to answer, and would record the pattern and its position, never the rows.

***One writer.*** It cannot be kept at every instant (Kingsbury on Datomic). Put safety in the compare-and-set and in an epoch the log can check. One gate is then a matter of speed, and two gates at failover are harmless.

***One store for the planet.***
- One order for the planet has a price list (section 3, question 10), and each item was paid by a company with unusual means. Brewer says plainly what makes Spanner work: "it is really Google's wide-area network, plus many years of operational improvements".
- Google's lineage says the unit of order, once chosen, cannot be changed, and that small fixed units pushed "most customers" out of consistency.
- Placement is a separate matter from order. Spanner: "A directory is the unit of data placement." A personal layer is a natural unit of placement (where a person's data physically lives, for law and for latency), even if it is not the unit of order.
- Sharing one store is its own engineering problem. Firestore: "an individual RPC is not a uniform work unit, as its cost can vary significantly—one RPC can cost a million times another", and "isolation is hard, and our techniques for maintaining isolation are not always guaranteed to work". Personal databases get isolation for free. Personal layers in one store have to earn it.

***A fixed nine-part envelope both ends share forever.*** TigerBeetle would cheer, and then add conditions from experience. Reserve room that must be zero. Version the envelope. Never let a value double as a signal. Remember that the encoding has a version of its own (Apple's proto2 trap). And Kissner's warning: the format freezes at first adoption, which comes earlier than anyone plans.

***Tools, grammars, policies and definitions are facts in the same store.*** There is precedent in favour: Chubby's ACL files, F1's schema, Spanner's metadata "managed as data", the Record Layer's warning against schema that lives in code, and Moshenko's point that policy kept outside goes stale by its own clock. Their one firm addition: such facts are held in memory by many gate instances, and they change nowhere at once. F1 needed leases, fencing of writes by schema version, and changes built from steps that are safe next to their neighbours. A compare-and-set on the grammar fact does not provide that. Every verdict must at least name the versions it used.

***An append-only store of small facts as the one substance.*** FoundationDB's founders would like the small core. Spanner's authors would warn from their own format that small self-describing records are "highly redundant" and that the physical form will need to change, so the logical fact must not be tied to it. TigerBeetle, the purist, still stores running balances in place. Restate, which says the log is the truth, still snapshots derived state "To avoid arbitrarily long re-build phases". I report that as their practice. It is not a recommendation.

# 6. Questions this camp would call the wrong question

- **(0) *Never rewritten, or only never lost?*** They would ask how anyone would know. A promise nobody can check is marketing. The useful questions are what the acknowledged unit is, and what in each record lets a reader detect a change.
- **(10) *Can two gates ever write one layer?*** They will both try, at some failover. Ask what in each append lets the log refuse the stale one.
- **(1) *The gate's number, or its own id?*** A false choice. The systems that lasted have both, plus a checksum, and each does one job.
- **(11) *Whose clock, and is it ever used for order?*** "When" and "order" are different columns. Once they are kept apart, whose clock is an easy question.
- **(5) *Is how far the index had got written down?*** This assumes that answering from behind is allowed. The lineage refuses to. Ask instead whether every index knows its position and every answer states one.
- **(13) *Plain maps or classes?*** That is a question about the physical form, which can change. The forever question is about the logical envelope and its rules for growth.
- **(12) *At the start of a session?*** Wrong unit. The build needs to be named wherever something was derived or judged: each crossing and each verdict.
- **(7) *Are refusals kept?*** Better: which refusals depend on the state of the store? Those must be kept, or a retry can flip the outcome. The others come out the same every time.

# 7. Sources

All fetched on 20 September 2026. About 190 documents were saved as text; listed here are the ones this report quotes or leans on. "Docs" means current product documentation, which changes over time.

**Verification result.** A script compared every passage in double quotation marks that is ten characters or longer (about 580 passages, titles included) with the saved source texts. It compares letters and digits only, so line breaks, hyphenation, punctuation and typographic quote marks are ignored, while a changed or added word is caught. After the fixes below, every passage matched a source. What the script caught: (1) one quotation that a gatherer had reported wrongly. Jepsen's FaunaDB report says "documentation for consistency properties was sparse"; the gatherer's version spoke of marketing material, a phrase that appears nowhere in that report. I corrected it from the source. (2) One quote of mine that was off by one word (Firestore's docs say "using a scatter algorithm"). (3) Two places where I had put my own paraphrase inside quotation marks. Both were removed. (4) Six quotes that a PDF page break or footer interrupts in our saved copy. I re-cut these so that each quoted piece is continuous in the source. What the script cannot check is whether a quote is fairly placed in its context. That rests on my reading of the passages.

### TigerBeetle
- Docs: Data Modeling, Time, Reliable Transaction Submission, System Architecture, Correcting Transfers, Upgrading, Safety; reference pages for Transfer, Account, create_transfers; and the single-page build. https://docs.tigerbeetle.com/ (for example https://docs.tigerbeetle.com/coding/time/ and https://docs.tigerbeetle.com/reference/transfer/).
- Repository, main branch, https://github.com/tigerbeetle/tigerbeetle : docs/ARCHITECTURE.md; docs/TIGER_STYLE.md; docs/internals/upgrades.md and data_file.md; docs/coding/api-changes.md; CHANGELOG.md; src/vsr/message_header.zig; src/vsr/replica.zig; src/state_machine.zig; src/tigerbeetle.zig.
- Joran Dirk Greef, "Three Clocks are Better than One", 2021. https://tigerbeetle.com/blog/2021-08-30-three-clocks-are-better-than-one
- Lewis Daly, "One for the Treble, Two for the Time", 2026. https://tigerbeetle.com/blog/2026-01-14-bitemporality
- matklad, "Tracking Time Without Clock", 2025. https://tigerbeetle.com/blog/2025-10-21-clockless-time
- "Fuzzer Blind Spots Meet Jepsen", 2025. https://tigerbeetle.com/blog/2025-06-06-fuzzer-blind-spots-meet-jepsen
- Joran Dirk Greef on The Changelog #635, 2025 (transcript). https://changelog.com/podcast/635
- Joran Dirk Greef, "A New Era for Database Design with TigerBeetle", QCon talk recorded 2023 (transcript). https://www.infoq.com/presentations/tigerbeetle/
- Kyle Kingsbury, "Jepsen: TigerBeetle 0.16.11", 2025. https://jepsen.io/analyses/tigerbeetle-0.16.11

### Temporal and Restate
- Temporal docs, https://docs.temporal.io/ : Workflow Definition (deterministic constraints); Events and Event History; Workflow Execution limits; Workflow message passing; Event reference; Workflow Id and Run Id; Patching; Versioning (Go SDK); Worker Versioning, including the legacy page; Continue-As-New; Activities; Data conversion; Namespaces (retention).
- Temporal blog, "Safe deployments with Temporal Worker Versioning on Kubernetes", 2026. https://temporal.io/blog/safe-deployments-with-temporal-worker-versioning-on-kubernetes
- Shawn Wang, "Designing a Workflow engine from first principles" (summary of a talk by Maxim Fateev), 2021. https://temporal.io/blog/workflow-engine-principles
- SE Radio 596, "Maxim Fateev on Durable Execution with Temporal", 2023 (transcript). https://se-radio.net/2023/12/se-radio-596-maxim-fateev-on-durable-execution-with-temporalse-radio-596/
- Stephan Ewen, "Why we built Restate", 2023. https://restate.dev/blog/why-we-built-restate/
- Jack Kleeman, "Solving durable execution's immutability problem", 2024. https://restate.dev/blog/solving-durable-executions-immutability-problem/
- Stephan Ewen and Jack Kleeman, "Every System is a Log: Avoiding coordination in distributed applications", 2025. https://restate.dev/blog/every-system-is-a-log-avoiding-coordination-in-distributed-applications
- Stephan Ewen, Ahmed Farghal, Till Rohrmann, "Building a modern Durable Execution Engine from First Principles", 2025. https://restate.dev/blog/building-a-modern-durable-execution-engine-from-first-principles
- Giselle van Dongen and Francesco Guardiani, "Updating AI Agents safely in production", 2026. https://restate.dev/blog/dealing-with-versioning-in-long-running-agents
- Giselle van Dongen, "Agent checkpointing is far from production-grade resiliency", 2026. https://restate.dev/blog/why-checkpointing-is-not-production-grade-durable-execution
- Restate docs: Versioning, https://docs.restate.dev/operate/versioning ; service configuration (retention), https://docs.restate.dev/services/configuration

### Zanzibar and its rebuilders
- Ruoming Pang and thirteen others, "Zanzibar: Google's Consistent, Global Authorization System", USENIX ATC 2019. https://www.usenix.org/system/files/atc19-pang.pdf
- "Developer Den with Lea Kissner", Oso, 2021. https://www.osohq.com/post/developer-den-with-lea-kissner
- Jake Moshenko, "Enforcing Causal Ordering in Distributed Systems: The Importance of Permissions Checking", AuthZed, 2021. https://authzed.com/blog/new-enemies
- Evan Cordell, "The One Crucial Difference Between Spanner and CockroachDB", AuthZed, 2021. https://authzed.com/blog/prevent-newenemy-cockroachdb
- Jimmy Zelinskie, "Zed Tokens, Zookies, Consistency for Authorization", AuthZed, 2023. https://authzed.com/blog/zedtokens
- AuthZed's annotations on the paper (source file). https://raw.githubusercontent.com/authzed/zanzibar-annotated/main/content/annotations-spicedb.yaml
- SpiceDB docs, Consistency. https://authzed.com/docs/spicedb/concepts/consistency
- Alan Yao, "Himeji: a scalable centralized system for authorization at Airbnb", 2021 (read via web.archive.org; the original is on medium.com/airbnb-engineering).
- OpenFGA docs, Query Consistency Modes (revised September 2026). https://openfga.dev/docs/interacting/consistency

### FoundationDB and the Record Layer
- Jingyu Zhou and many others, "FoundationDB: A Distributed Unbundled Transactional Key Value Store", SIGMOD 2021. https://www.foundationdb.org/files/fdb-paper.pdf
- Christos Chrysafis and others (Apple), "FoundationDB Record Layer: A Multi-Tenant Structured Datastore", SIGMOD 2019. https://www.foundationdb.org/files/record-layer-paper.pdf
- Record Layer docs, "Schema evolution". https://foundationdb.github.io/fdb-record-layer/SchemaEvolution.html
- FoundationDB docs, https://apple.github.io/foundationdb/ : Developer Guide; Python API; Automatic Idempotency; Known Limitations; Anti-Features; Testing; Layer Concept.
- Design document for idempotency ids. https://raw.githubusercontent.com/apple/foundationdb/main/design/idempotency_ids.md
- FoundationDB forum threads: "Versionstamp uniqueness and monotonicity" (602), "Versionstamp vs committedVersion" (600), "Versionstamps and DR streaming" (4564), all at https://forums.foundationdb.org/ ; GitHub issue apple/foundationdb#1073.
- Will Wilson, "Is something bugging you?", Antithesis, 2024. https://antithesis.com/blog/is_something_bugging_you/
- Notes by a third party on Will Wilson's Strange Loop 2014 talk (a paraphrase, not Wilson's words). https://alex-ii.github.io/notes/2018/04/29/distributed_systems_with_deterministic_simulation.html

### Calvin, Abadi, FaunaDB, and the reply from CockroachDB
- Alexander Thomson, Thaddeus Diamond, Shu-Chun Weng, Kun Ren, Philip Shao, Daniel Abadi, "Calvin: Fast Distributed Transactions for Partitioned Database Systems", SIGMOD 2012. http://cs.yale.edu/homes/thomson/publications/calvin-sigmod12.pdf
- Alexander Thomson and Daniel Abadi, "The Case for Determinism in Database Systems", VLDB 2010. http://www.cs.umd.edu/~abadi/papers/determinism-vldb10.pdf
- Daniel Abadi and Jose Faleiro, "An Overview of Deterministic Database Systems", CACM 2018. https://www.cs.umd.edu/~abadi/papers/abadi-cacm2018.pdf
- Daniel Abadi's blog, https://dbmsmusings.blogspot.com/ : "Distributed consistency at scale: Spanner vs. Calvin" (2017); "NewSQL database systems are failing to guarantee consistency, and I blame Spanner" (2018); "Partitioned consensus and its impact on Spanner's latency" (2018); "It's Time to Move on from Two Phase Commit" (2019); "Correctness Anomalies Under Serializable Isolation" (2019).
- Daniel Abadi and Matt Freels, "Consistency without Clocks: The FaunaDB Distributed Transaction Protocol", 2018 (read in the dev.to republication, which names only Freels; the archived fauna.com page names both). https://dev.to/fauna/consistency-without-clocks-the-faunadb-distributed-transaction-protocol-p46
- Kyle Kingsbury, "Jepsen: FaunaDB 2.5.4", 2019. https://jepsen.io/analyses/faunadb-2.5.4
- The Fauna Team, "The Future of Fauna", 2025 (via web.archive.org).
- Spencer Kimball and Irfan Sharif, "Living without atomic clocks", Cockroach Labs. https://www.cockroachlabs.com/blog/living-without-atomic-clocks/
- Andrei Matei, "CockroachDB's consistency model", 2019. https://www.cockroachlabs.com/blog/consistency-model/

### Google's lineage
- Fay Chang and others, "Bigtable: A Distributed Storage System for Structured Data", OSDI 2006. https://static.googleusercontent.com/media/research.google.com/en//archive/bigtable-osdi06.pdf
- Mike Burrows, "The Chubby Lock Service for Loosely-Coupled Distributed Systems", OSDI 2006. https://static.googleusercontent.com/media/research.google.com/en//archive/chubby-osdi06.pdf
- Jason Baker and others, "Megastore: Providing Scalable, Highly Available Storage for Interactive Services", CIDR 2011. https://www.cidrdb.org/cidr2011/Papers/CIDR11_Paper32.pdf
- James Corbett and others, "Spanner: Google's Globally-Distributed Database", OSDI 2012. https://static.googleusercontent.com/media/research.google.com/en//archive/spanner-osdi2012.pdf
- Jeff Shute and others, "F1: A Distributed SQL Database That Scales", VLDB 2013. https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/41344.pdf
- Ian Rae, Eric Rollins, Jeff Shute, Sukhdeep Sodhi, Radek Vingralek, "Online, Asynchronous Schema Change in F1", VLDB 2013. https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/41376.pdf
- Eric Brewer, "Spanner, TrueTime & The CAP Theorem", 2017. https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/45855.pdf
- David Bacon and others, "Spanner: Becoming a SQL System", SIGMOD 2017. https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/46103.pdf
- Ram Kesavan, David Gay, Daniel Thevessen, Jimit Shah, C. Mohan, "Firestore: The NoSQL Serverless Database for the Application Developer", ICDE 2023. https://storage.googleapis.com/gweb-research2023-media/pubtools/7076.pdf
- Cloud Spanner docs: commit timestamps, schema design, TrueTime and external consistency, timestamp bounds, point-in-time recovery. https://cloud.google.com/spanner/docs/schema-design and neighbouring pages.
- Firestore best practices. https://firebase.google.com/docs/firestore/best-practices
- "Balancing Strong and Eventual Consistency with Datastore", Google Cloud article. https://docs.cloud.google.com/datastore/docs/articles/balancing-strong-and-eventual-consistency-with-google-cloud-datastore
- "Data deletion on Google Cloud". https://cloud.google.com/docs/security/deletion
- "Google infrastructure security design overview". https://cloud.google.com/docs/security/infrastructure/design
- "Cloud Audit Logs overview". https://cloud.google.com/logging/docs/audit

### Checking
- Kyle Kingsbury, "The trouble with timestamps", 2013. https://aphyr.com/posts/299-the-trouble-with-timestamps
- Jepsen, "Research Ethics". https://jepsen.io/ethics
- Jepsen tutorial, chapters 3 and 6. https://github.com/jepsen-io/jepsen/tree/main/doc/tutorial
- Kyle Kingsbury, Jepsen analyses, https://jepsen.io/analyses : Datomic Pro 1.0.7075 (2024); CockroachDB beta-20160829 (2017); etcd 3.4.3 (2020); Bufstream 0.1.0 (2024); MongoDB 4.2.6 (2020); PostgreSQL 12.3 (2020); Redis-Raft 1b3fbf6 (2020); TiDB 2.1.7 (2019); MySQL 8.0.34 (2023).
- Kyle Kingsbury and Peter Alvaro, "Elle: Inferring Isolation Anomalies from Experimental Observations", VLDB 2020. https://arxiv.org/pdf/2003.10554
- Chris Newcombe, Tim Rath, Fan Zhang, Bogdan Munteanu, Marc Brooker, Michael Deardeuff, "Use of Formal Methods at Amazon Web Services", 2014 (the long version of the CACM 2015 article). https://lamport.azurewebsites.net/tla/formal-methods-amazon.pdf
- Marc Brooker and Ankush Desai, "Systems Correctness Practices at AWS", ACM Queue, 2025 (read via web.archive.org; original at https://queue.acm.org/detail.cfm?id=3712057).
- James Bornholt and others, "Using Lightweight Formal Methods to Validate a Key-Value Storage Node in Amazon S3", SOSP 2021. https://www.cs.utexas.edu/~bornholt/papers/shardstore-sosp21.pdf

### Ids
- K. Davis, B. Peabody, P. Leach, RFC 9562, "Universally Unique IDentifiers (UUIDs)", IETF, 2024. https://www.rfc-editor.org/rfc/rfc9562.txt
- Ryan King, "Announcing Snowflake", Twitter engineering blog, 1 June 2010 (via web.archive.org), and the project README. https://github.com/twitter-archive/snowflake/tree/snowflake-2010

# 8. Round two: the leans, pressed

Written 20 September 2026, after round one, in answer to the orchestrator's list of softland-ff's current leans. They are leans, not Sid's rulings. Marks: R = reported (the source is named; its URL is in section 7), N = institutional, I = my inference. The budget was tight, so this is argued from sources already gathered. Two quotes are new to the report (Fateev on recorded arguments; Zanzibar on rounded timestamps). The script checked them like the rest. Phrases taken from the leans are in italics.

## T1. Lean (1): the fact's id as a hash of the offer plus a salt

**What the lean gets right (I).** The offerer can compute the id before admission, and a retry gives the same id if the salt is kept. It also turns every pointer into a commitment, so a based-on entry proves what was read. And if the salt dies with the value's key, it answers my round-one worry about guessing erased values.

**What breaks.** Two things that a random name does not suffer.
1. *The hash wears out; the name is forever.* RFC 9562 already has to send readers to the security notes on MD5 and SHA-1 (R). Deciding case: year twelve, an economy rides on based-on chains, and the hash now allows collisions. If the name is the hash, names themselves become ambiguous, and no old pointer can be re-made. If the name is random and a digest sits beside it, only the check is weak, and later facts can re-attest old ones under a new hash. Apple shows what two id schemes cost once both are in the record: "(incarnation, version) if the record was last updated with the new method and (0, update counter value) otherwise" (R), for good.
2. *A content hash makes the byte encoding forever, in every client.* Lean (13) says plain maps. Hashing a plain map needs canonical bytes (key order, numbers, Unicode), fixed before record one and identical in every agent runtime. Deciding case: tens of agents per person, written in three languages; two libraries encode one number differently; the same offer gets two ids; a retry through the other library lands twice. Apple's Record Layer found that the encoding has a version of its own, with "no clear upgrade path" (R).

**Beside Amazon's pattern and the Datomic claim.** As research 7 reports it, Amazon names a request by a caller-minted id and checks a retry by comparing the stored parameters. TigerBeetle does exactly this, and can tell the caller which field differs ("exists_with_different_amount", R). A hash id cannot. Different bytes are simply a different offer, so *same intent, different content* stops being a visible error. On the Datomic claim that sayings have ids and facts do not: my camp agrees on *what* is named, since retry safety and verdicts attach to the offer (Kingsbury, R). It disagrees on *how*. FaunaDB named sayings by their place in the log (R), which is the store's number again.

**Sharpen, not reject.** Keep the commitment and unfuse it. A random offerer-made id is the name. A digest, tagged with its algorithm, and salted or taken over the ciphertext, is a separate field that a pointer may carry beside the id. Then lean (6) comes free. If the offer states its expected version as *the predecessor's id and digest*, the fact already says what it replaces, inside what is committed. That is TigerBeetle's parent checksum (R) and what Elle needs (R), per cell. Offer and fact then differ only by what the gate adds.

**Lean (2) stands** for entities: long, random, offerer-made, no time inside. Google's docs live this way (N). TigerBeetle dissents, for LSM speed (R). Rama's index layout decides that, and I do not know it. Press on the ingest rule. An entity id derived from *source plus form* makes the source's mistakes about identity permanent. RFC 9562: "A common issue observed in database schema design is the assumption that a particular value will never change, which later turns out to be an incorrect assumption" (R). Deciding case: one paper arrives as a preprint through one lane and under a DOI through another, or a DOI is reissued. One paper then has two ids for life. And if *form* moves when a lane's parser is fixed in year two, the re-run mints ten million new entities. The alternative (I): derive the *offer* id from lane and source key, mint the entity id at random, and let the kept verdict (lean 7) hand the first entity id back on any re-run. That needs a name that is not a hash of content, which is a second reason for the sharpening above.

## T2. The opaque token, against leans (10) and (5)

A cut per partition is the honest *as of* for partitioned logs. Abadi would agree that nothing else is on offer without one global log or Google's clocks (R). So lean (10) stands. What the token must be able to carry later (I, each with its ground):
- **Store id and incarnation.** A restore or a move must leave a mark (Apple, R; six years of FoundationDB forum threads, R).
- **A layout epoch.** A set of positions means nothing after re-partitioning unless it names the layout it was cut under. Megastore's Maps case is the warning: "the number of entity groups does not grow with increased usage, so enough patches must be created initially for sufficient aggregate throughput at later scale" (N). Deciding case: year three, the partition count quadruples, and every earlier based-on holds positions over a layout that no longer exists.
- **Sparse positions, or a pointer to a stored cut.** Deciding case: an agent does a hundred pattern reads a second across all of base. Inline positions for thousands of partitions are tens of kilobytes per read entry. Zanzibar shares snapshots on purpose: "We choose evaluation timestamps rounded up to a coarse granularity, such as one or ten seconds, while respecting staleness constraints from request zookies" (N). So let the store mint cuts as small facts on a beat, and let a token name one. Many reads then share one cut.
- **The serving index's position, in the same currency.** Idle partitions need a heartbeat, or silence cannot be told from lateness (Spanner and Firestore, N). Whether a Rama index can state the depot positions it has applied is the first thing to check.
- **The policy position used to filter the answer.**

On lean (5)'s *what was withheld*: press. Writing down what was hidden tells the asker that hidden things exist. Deciding case: hundreds of people on one problem; Alice's read over claims about one protein records two withheld facts in Bob's private layer; Alice now knows Bob holds private claims there. Kissner's warning about copied access rules fits: "a giant privacy incident manufacturing system" (R). Record *that* a filter ran, and the policy position it ran at. Never record the withheld set.

**The little to fix now (I).** The token is opaque bytes with a kind tag. Only the store mints it and reads it. Nothing compares tokens except the store. And a comparison may answer *incomparable*, which every consumer must handle from day one.

## T3. Re-derivation against rebuilds, and lean (12)

**The cheapest road is the third, split in two (I).** Put a digest on every crossing. Put the content on crossings to a receiver that cannot be re-derived: a model, or the host. Keeping hundreds of builds runnable is the dearest road, and Kleeman names its danger: "there is a security and reliability concern about having arbitrarily old code running" (R). Unchanging behaviour forbids fixing the very bugs one wants fixed (Firestore's promise, N).
Deciding case: year four. A summary layer over the ten million papers was written by a model in year one. Someone disputes one summary and asks what the model was given. Tool version one is a fact and can run. Build 37 is gone. Build 212 re-derives a prompt whose digest does not match. With a digest alone, the store can say honestly that it does not know, and it will say so forever. With the content, it can answer. Temporal records the inputs of every such step, not only the results. Fateev: "every activity in vacation you see arguments, every activity completion you see result" (R; *in vacation* is the transcript's slip for invocation). For a screen, a digest per tick is enough. Content falls under lean (9) like any value, so it can be erased.

**Does every verdict need the release stamp?** Not for replay. Lean (7) keeps verdicts, so none is ever re-derived. It needs the stamp for *scoped doubt*. Deciding case: a rolling deploy; build 118 has a wrong shape check and runs on four partitions in ten for six hours. *When* cannot separate its verdicts from a neighbour's, because builds overlap in time. The cheap form: each verdict carries its gate epoch (T5), and the epoch's opening fact carries the build and the kind of machine. That is one small field, and the build is a fact about the epoch. The verdict should also name the grammar version it checked (F1 fences writes by schema version, N).
**Lean (12), sharpened.** The unit is the epoch and the crossing, not the session. Deciding case: a session runs three days across four deploys. Temporal stamps the build on every completed task for this reason (R).

## T4. Identity-keyed policy against capabilities: an exchange

**Zanzibar's best reply (I, built from R and N).**
1. We never let a deputy spend its own authority on a user's data. The calling service must show that it acts for a named person, and the callee "only returns data for the end user named in the ticket" (N).
2. Rules kept as records give what bearer tokens never did: a reverse index, and clean removal. Kissner: "what we want is a canonical, positive representation of ACLs" (R). Sid needs *who read the erased thing*. That is a query over state with history.
3. Citing a grant does not make it fresh. A grant revoked at position 900 can be cited at a gate still reading position 850. Ordered evaluation is needed under either view.

**The capability camp's rejoinder, at full strength (I).**
1. Point one concedes the case. The ticket *is* a request citing its authority, and Google needed it because identity checks inside a deputy can be confused. Now look at Sid's store. Tools fire by matching. Nobody handed them an authority for a purpose. A tool that Alice enabled, on by default in her layers (lean 15), matches a fact that Mallory wrote, and then offers as Alice's agent. The trigger chose the target, and the enabler's standing authority did the writing. That is Hardy's deputy, with prompt injection as the modern route, at machine rate.
2. Citation makes audit better, not worse. With the grant on the offer, *under what authority did this land* is one hop, and everything done under a revoked grant is one walk. Without it, the verdict names whichever allowing rule the gate happened to find. When two grants would each have allowed the write, the actor's intent is lost, and no later envelope can recover it.
3. The reverse-index complaint is about secrets passed around out of band. In Sid's store a grant is a fact: ordered, indexable, and superseded in order to revoke. RFC 9562's rule that an id "MUST NOT be used as security capabilities" (R) is kept, because the gate still checks that the cited grant names this actor.

**What must be on every offer from record one.**
- *Identity view:* the actor as itself, *acts for*, and a verdict that names the policy facts it found and the cut it read them at.
- *Capability view:* all of that, plus **the grant this offer invokes**, by id and version. The gate checks that grant and does not search. *Because of* already says at whose instigation. The authority slot is the one that is missing.
- *My camp's call (I):* the two views differ by one id per offer. It cannot be reconstructed later, and it costs nothing if policy ignores it. By round one's test, it belongs in the first record. Take the slot from the capability camp, and the ordering discipline from Zanzibar. One press back on *a click designates and thereby grants*: such a grant should stand on the crossing's digest, so the record shows what the person was looking at when they clicked.

Deciding case: Alice's summarizer reads Mallory's fact, whose value tells the model to write into Alice's durable layer. Under identity policy, Alice's tool may write Alice's layer, so it lands. Under citation, the tool holds one grant (summaries into session S for trigger T), the write cites nothing valid, and the refusal is kept.

## T5. The epoch the log can check

**What is written (I, from Chubby R, Restate R, FoundationDB N).** An *epoch-open fact* in each ordered unit that a gate instance serves. It holds an epoch number greater than the last one there, the gate's actor id, the build, and the kind of machine. Restate's new leader "appends a message to the log to signal their epoch is now active" (R). FoundationDB opens each epoch with "a special recovery transaction" (N). On every verdict: the epoch it was judged under. The rule: in that unit, nothing from a lower epoch is accepted after a higher one opens.
**Where.** In the same partition as the cells it fences, since lean (10) gives no order across partitions.
**Who enforces.** Safety stays with the per-cell compare-and-set. Kingsbury: "Any number of concurrent transactors ought to be safe" (R). Whether Rama's append path can refuse a stale writer is a claim about Rama, and it needs its own test. Whatever Rama does, the record makes a breach *provable*. A verdict from epoch e that sits after the opening of e+1 is a split-brain trace that any checker can find from the facts alone.
**For the first record.** An epoch slot on the verdict. Genesis opens epoch zero in every partition. Offers never carry epochs.

**This presses lean (7).** A refusal must be decided and recorded in the same atomic step as the compare-and-set, under the offer's id. Deciding case: the gate refuses an offer in the cell's partition and crashes before the refusal lands in some other partition. The agent retries. The cell has moved. The offer now succeeds. That is TigerBeetle's old flaw: "a transfer that previously failed could succeed if retried when the underlying state changes" (R). The lean survives on one condition. The refusal is keyed to the *target entity*, so that it lands in the cell's partition, and it is only *layered* to the offerer's session. Where a record lives and who may see it are separate choices.

## A. The other leans

- **(0) Stands, as a promise about content.** Deciding case: one replica's disk goes bad and is repaired from another, which rewrites bytes. TigerBeetle does this as a matter of course (R). Say that identity and content never change. Add a checksum and the chain, or nobody can test the promise (Kingsbury, R).
- **(17) Reject one clause.** Constants computed from content are fine, and RFC 9562's published namespace ids live this way (N). But if *the gate* is a seed id, every store's gate has the same id. Deciding case: federation day; a fact arrives *by the gate*, and nobody can say which gate. TigerBeetle puts the cluster's id in every message for this reason (R). Constants are the same everywhere. The store, its incarnation and its gate are random per store.
- **(9) Sharpen twice.** First, *the fact keeps the value's hash*: a plain hash of a short value can be guessed after erasure. Deciding case: an erased rating, vote, or email address. Hash the ciphertext, or salt with something that dies with the key. Second, *maybe by-whom* may go: no. Keep the opaque actor id on the facts, and erase the facts that say who that actor is. TigerBeetle keeps names in the other database (R). Otherwise *who read the erased thing* breaks. Also, the key store is the one thing that must truly delete, and its own backups set the real deadline (Google's 180 days, N).
- **(11) Press the single *when*.** Deciding case: federation, or an agent's offer held offline for an hour. The other gate's time, or the offerer's, is about the saying, and it has nowhere to go. TigerBeetle lived this and now teaches "Recorded: when the fact was learned, and Effective: when the fact took place" (R). Keep a slot for a claimed *when*. And *empty only at a chain's start* is a value doubling as a signal (TigerBeetle's zero amount, R). Use an explicit start mark.
- **(11) Every read listed, no grace period.** Nobody here has run this (Temporal, R; Google's audit logs, N). Deciding case: fifty pattern reads per offer, twenty offers a second per agent, tens of agents, hundreds of people. That is about ten million read entries a second on one problem. Point at the crossing, and let the crossing hold the reads.
- **(14) Press the default.** Every lived default leaves looks unrecorded, for volume (N, R). Deciding case: an hour of panning at ten ticks a second is thirty-six thousand facts from one person. Default on for select and point, which T4's grants need. Record pan and zoom only when a crossing's digest changes.
- **(3), (4), (6) stand.** For (3) there is a stronger ground than cost: stored patterns pin a word forever (I; the Record Layer's rename, R). For (4), *the floor guesses and the tool may correct* matches FoundationDB's lesson that marking by hand breeds bugs (R).
- **(10) Partition by entity: the open interaction.** Policies and grants are entities too, so they sit in other partitions than the cells they govern. Deciding case: Alice revokes an agent's grant at position 900 of one partition. The agent's next offer lands in another partition, whose gate has read the first only up to 850. The offer is admitted under a revoked grant. That is Zanzibar's Example B on the write side (N). There are two ways out. The verdict carries the cut it read the grant at, so a walk finds the breach afterwards. Or revocation goes through one serial point per layer, which is the price SpiceDB paid without Google's clocks: "all relationship writes touch the same row" (R).

## B. Wrong questions

- (1) *Should the name be a hash?* Ask which pointers must be commitments, and put a digest beside the id there.
- (12) *What is written at session start?* Ask which unit can change build midway. The session can. The epoch and the crossing cannot.
- (5) *What was withheld?* Ask under which policy position the answer was filtered.
- (7) *Which layer holds refusals?* Ask what is atomic with the compare-and-set.
- (10) *One number or a cut?* That is settled. Ask who mints cuts and how reads share them.

## C. Above the table, where the leans change round one

- **Running answers never stored.** Amend it by Sid's own rule. A crossing to a model or to the host carries its content, because what stands on it cannot be re-derived. Every other crossing carries a digest.
- **A fixed nine-part envelope.** Under the leans it is no longer nine. An own id, a digest, the replaced version, a claimed *when*, the grant invoked, and an epoch on the verdict are all new parts. TigerBeetle would say: count them honestly now, give the envelope a version and spare room, and let no value double as a signal.
- **Policies as facts, with one writer.** Partition by entity moves the hard problem here. A policy must be ordered with the writes it governs, or every verdict must say how much of the policy it had seen. This is the strongest change I would make above the table.
- **Resembles and differs.** Every press above comes from a system with one domain, one code base, or a past that ends. Sid's store has many agent runtimes, one log shared across the planet, and a past that does not end. The deciding cases fall the way they do because of that difference. They are my inferences, not rulings from the sources.
