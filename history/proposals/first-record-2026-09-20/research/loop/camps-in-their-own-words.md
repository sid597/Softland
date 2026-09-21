# The camps in their own words

Assembled by script on 20 September 2026 from the session transcripts. Nothing here is reworded and none of it is the orchestrator's reading. Part 1 is the brief every research session received, in the words of softland-ff (Sid's main session). Part 2 is the leans as sent in round two; they are softland-ff's leans, not Sid's rulings. Part 3 is every summary the research sessions sent, in order of arrival. research-5's blind report produced no summary; its short version is section 1 of `../sync-versioning-defaults.md`. Its round-two summary is the one Sid pasted. The full reports, with sources, sit one folder up.

Groups: research-1 Rama, Marz, Kreps (`rama-marz.md`); research-2 Hickey, Datomic, Nubank, XTDB (`facts-datalog.md`); research-3 the Kay and Hickey exchange, objects, capabilities, substrates (`meaning-objects-substrates.md`); research-4 the log as truth (`log-as-truth.md`); research-5 sync, multiplayer, versioning (`sync-versioning-defaults.md`); research-6 clocks, ids, determinism (`clocks-ids-determinism.md`); research 7 frontiers and views (`frontiers-views.md`) and the industry default, skeptics, big tech (`defaults-skeptics-bigtech.md`).

## Part 1. What every research session was given

THE SYSTEM, AS MUCH AS YOU NEED
One store for everyone: a single Rama cluster deployed worldwide ("one truth"). It only appends records and never rewrites one. A second store (an institution running its own) is a rare, later case: say what each source implies for it, but the case in front of us is one store.
A record is a fact. Sid's words for its parts:
- entity: an opaque id, for life. No type slot; a type is just another entity that others point at.
- key: the kind of statement (e.g. :relation). Patterns match on it; the value's shape (its grammar) is looked up by it; the grammar is itself a fact with versions.
- value: shaped by that grammar.
- by whom: the actor. A person, an agent, a model, an ingest lane, the gate, the runtime.
- when: stamped by the gate at admission.
- layer: the one context slot. A shared base at the bottom, each person's durable layer above it, a session layer above that; layers compose freely. For an asker, the nearest layer holding a row wins. Promotion into a lower layer is an ordinary offer.
- based on: the reads this fact stood on. Three kinds: a read of one fact at its version; a read of everything matching a pattern as of some point; an anchor to something outside the store (a git commit, a paper).
- because of: the fact whose landing started the chain of work this fact belongs to; empty only when it starts one.
- version: the gate's number for this fact. Superseded = same entity + key + layer, later version.
An offer is a fact before the gate; it also states which version it expects to be current for that entity + key + layer. The gate is the only writer. It checks shape (against the grammar fact), version (compare-and-set on that cell), and policy (a policy fact lets this actor write this key in this layer). Its yes or no is itself a fact naming what it checked.
Tools are facts too: a signature (which keys it wants, under which conditions) and a body. Nobody routes; a landed fact is matched to tools. A tool yields either a running answer (deterministically re-derivable from the same reads; never stored; recomputed when what it read moves) or an offer (anything not re-derivable: a person's act, a model's reply, an ingest). When a running answer crosses to a screen, a model, or the host, "what was shown" is recorded as a fact with the reads it was built from. Staleness, doubt, "what was I looking at yesterday", "what was the model given", and "who read the erased thing" are all computed by walking based-on. The definitions of stale and doubtful, the policies, and the grammars are facts as well: the medium is meant to be understood and rebuilt from inside itself. The one thing not made of facts is the runtime, which will be rebuilt hundreds of times over the years.
SCALE TO HOLD: tens of agents per person writing at machine rate; hundreds of people plus their agents on one problem; a whole field seeded from about ten million papers with summary layers on top; the planet, with an economy; years of operation. Principles Sid holds: the map must not lie (staleness and doubt shown as loudly as content); no optimism; match, don't route, and the store holds no names for tools; humans and agents use the same moves.
WHAT SID IS DECIDING: because the store never rewrites, whatever is not written into a fact when it is made is gone for every earlier fact. Which conventions must be fixed before the first record, so Sid is not painted into a corner years later?

THE QUESTIONS (numbers are Sid's; hung on the part of the fact they bite)
(0) the log itself: never rewritten, or only never lost?
(2) entity: how is an id made so two never clash? may an id give away when or where it was made?
(17) the first facts: what ids do base, the gate, and the first kinds get? what are the first facts, who writes them, are they the same in every store?
(3) key: a word, or an id too, with its name and shape as facts about it?
(9) value: never removed, so how is one deleted, backups included?
(8) by whom: who checks it? is a person's agent itself, or the person? where is "acts for" recorded?
(11) when: whose clock? ever used for order?
(16) layer: who sees a fact before any permissions exist?
(10) order: can two gates ever write one layer? what must share a partition to stay ordered? is "as of" one number or a position per partition?
(11) based on: is every read listed, including reads that only made a tool fire and entries the runtime fills in for a reply?
(4) based on: does each read say whether the fact depends on it or it was only how the actor got here? follow the latest, or stay on the version read?
(5) based on: if an index was behind the log when a pattern was read, is how far it had really got written down?
(11) because of: always filled; empty only when it starts a chain?
(1) version: is a fact pointed at by the gate's number, or by its own id? if its own id: random, or computed from its content?
(6) version: when 37 replaces 25, is "replacing 25" kept on 37?
(7) beside each fact: is the gate's yes/no kept? where? are refusals kept?
(12) start of a session: is the running runtime version and the kind of machine written down? are rebuilds facts?
(14) the hand: which motions (point, select, pan) become facts by default? is being shown the same as looking?
(15) a click: which tools may act in a person's name?
(13) storage: plain maps or classes? ever trimmed? backups?

ALSO OPEN. Sid: "every layer including our layer 0 and -1 are open to question, not only this table." That means: an append-only store of small facts as the one substance for everything; tools, grammars, policies, and definitions being facts in the same store; one store for the planet with personal layers instead of personal databases; a fixed nine-part envelope both ends share forever; running answers never stored, only crossings recorded; every read recorded as provenance on every fact; one writer.

## Part 2. The leans, as sent in round two

THE LEANS (numbers are Sid's)
(0) the log: never rewritten, as Sid stated, rather than only never lost. This makes (9) and (10) now-or-never.
(2) entity id: long, random, minted by the offerer. An ingest lane derives the id from source plus form. No time inside the id.
(17) first facts: base and the gate are ordinary ids in the first facts. A finite seed, written once by the floor. Seed ids are computed from content so every store derives the same ones.
(3) key: an id too. Its word and its shape are facts about it. The ground given is shape and cost, not loss.
(9) value: stored as bytes under a per-value encryption key. Delete means throw the key away (this reaches backups and rewrites nothing). The fact keeps the value's hash. Only the value, and maybe by-whom, may ever go.
(8) by whom: the door verifies it against the logged-in session. An agent is its own actor. "Acts for" is a grant fact that exists before the agent's first write. [touches Sid's held reading that auth narrows to the crossing]
(11) when: the gate's clock only, never used for order. Based on: every read listed, including reads that only made the tool fire and entries the runtime fills in. Because of: always filled, empty only at a chain's start. No grace period for any of the three.
(16) visibility: base open; personal and session layers private; the gate's answer sits in the layer of the fact it answers.
(10) order: a layer has one home store. Within a store, partition by entity. "As of" is a position per partition (a cut), never one number.
(4) based on: each read is marked depends-on or how-she-got-here, and follow-latest or stay-on-version. The floor guesses; the tool may correct. [light reopen: the stale rule's entries]
(5) based on: write down how far the index had really got, and what was withheld. [light reopen: exactness]
(1) version: point at a fact by its own id, not by the gate's number. The id is computed from the offer's content plus a random salt. [reopens Sid's hold that version is the gate's number. It still is; it stops being the name.]
(6) keep "replacing 25" on the fact. [reopens: an offer and a fact differ by one slot each way]
(7) the gate's yes/no: kept forever, beside the fact, same partition, same layer. Refusals kept in the offerer's session layer.
(12) session start: who, runtime version, kind of machine. Every rebuild is a fact.
(14) the hand: point, select, mode, pan/zoom become facts at a tick. Looking is captured separately from being shown. A person may turn capture down or off by their own fact.
(15) a click: only tools the person enabled may act in their name. Tools in their own layers are on by default. [reopens the miss condition "in one layer"]
(13) storage: plain maps; never trimmed; backups inside the delete plan.

## Part 3. The summaries, in order of arrival

### research-1 — Round one (blind)

rama-marz report ready: src/proposal/first-record-2026-09-20/research/rama-marz.md
Top five:
1. Rama's task count is fixed at launch (live docs, today). Re-partitioning copies depots, so log positions change; restores reuse them; trims drop them. Name a fact by an id inside the record, never by position or the gate's number. (Q1, Q10, Q13)
2. RPL guidance: the offerer makes the id before the append (UUIDv7, 128 bits minimum), because retries regenerate ids. An append that throws may still have landed. So offer ids plus kept verdicts, refusals included, are needed for correctness. (Q2, Q7)
3. Two different gates. Stream: milliseconds, verdict returns inside the append call, atomic within one partition, at-least-once. Microbatch: exactly-once, atomic across partitions, a free global 64-bit tick (so "as of" can be one number), but 300 ms or more, silent to the offerer, and halted for everyone by one stalled task group or throwing record. "No optimism" makes that latency felt. (Q10, Q5, Q7)
4. Replay rebuilds only deterministic work (RPL, in writing). A clock read or version made in the gate must be stored as primary data. Kreps names the fork: log the requests, or log the leader's results. Sid must choose which is "the log". (Q0, Q11)
5. Rama records no reads and has no auth, read permissions, or encryption. By-whom, who-sees, based-on and erasure-by-key are theirs to build. Marz's Agent-o-rama keeps provenance in runtime-captured traces beside the data. (Q8, Q16, Q9)

Strongest challenge: none of Marz, Rama or Kafka holds "never rewrite". All keep positions and information fixed and let representation move (migrations, tombstones, compaction). What cannot be recovered is uncaptured information, not format. Keep that door and much of the table stops being a first-record decision. Limit: this camp always trusted the operator; if never-rewrite exists to prove the past to people who do not, hear transparency-log builders next.

### research-2 — Round one (blind)

facts-datalog report ready: src/proposal/first-record-2026-09-20/research/facts-datalog.md

Top five findings for Sid's questions (every quote in the report was machine-checked against downloaded sources):

1. Provenance belongs on the saying, not on each fact. Datomic's fact is [e a v tx op]; who/when/why/basis are attributes of the reified transaction. Hickey 2016: "substantially more efficient than replicating this on many facts (and IMO, correct, as the 'saying' of it *is* the transaction)". As briefed, Sid's offer is one fact, so they have no unit of saying. Bites (8), (11), (7), (12).

2. (3) Key: both. Id in the fact, name as a fact about it (:db/ident); renames add names; "Never remove a name"; only the value type is unalterable. Instant independently uses client-minted UUID keys. Datomic reads all history through today's schema (a documented limit), so versioned grammars need "which version admitted this" written down.

3. (10) "As of" is one number because there is one order, and that is a scope choice. Hickey: "It *is* the bottleneck… nothing is infinite." XTDB v2 enforces one partition and admits a "hard upper limit". Jepsen: safety is the CAS on the previous position, not one process. At scale the camp shards above: Nubank runs 3000+ databases.

4. (0)(9)(1) "While the past may be forgotten, it is immutable" (Hickey 2013). XTDB v1 kept the log immutable by logging only content hashes of erasable documents; that broke replay, needs tombstones, and has a still-open resurrection race. v2 made the log ephemeral and erases by compaction. Datomic excision: costly, absent from Cloud, bug fixed Oct 2025.

5. (7)(11) The regrets are about what was not written: Nubank lacks an owner id on every transaction, regrets "implicit operations… that don't have origination information", and lost its pre-write requests. XTDB v2 keeps refusals (xt.txs: committed, error).

Strongest challenge: Hickey built for a closed world, "avoiding the challenges of universal naming, open-world, shared semantics". One planetary store with shared keys is what he avoided; his camp composes many single-order databases at read time. And a fixed envelope is place-oriented: "If you have places, you have to have something in the place."

### research 7 — Round one (blind)

A (frontiers and incremental views) report ready: src/proposal/first-record-2026-09-20/research/frontiers-views.md
From research 7. All quotes copied from fetched source text and machine-checked. Not opened: Goebel's ETH thesis (rate-limited), one ACM Queue article (403).

Top five findings for Sid's questions:
1. (10) "One number or a position per partition" is a false choice. McSherry's reclocking doc and Feldera's fault-tolerance log, arrived at separately, do both: the asker gets one number; the store durably records, as sidecar data, the per-partition positions that number stands for. The interleaving is invented once, then locked. Added partitions need a way to say "not yet started". Facts admitted before such a map exists never get an exact as-of.
2. (11 when) The camp's firmest agreement: one time per fact, assigned at ingest, prescriptive, and it IS the order. Sender clocks are just data. Hellerstein: order matters only where logic is non-monotone.
3. (5) Yes, and stronger: a read is defined by the frontier it ran under (Materialize since/upper; Brandon; Hellerstein's "early read"). Never read an index past its frontier; then the recorded as-of is the lag.
4. (12) Feldera: exact re-derivation depends on CPU architecture, compiler settings, and a logged "now". "Re-derivable" holds only relative to tool version, runtime version and machine kind.
5. (4/11 based-on) Record reads at pattern grain, pinned, including empty reads; separate alternative supports or doubt over-spreads (LDFI). CALM's "manifest" makes based-on a safety tool: a reader waits until it holds what a fact stood on. (9): only per-owner keys from the first record fit never-rewrite (Schwarzkopf, K9db). (6): keep "replaces 25" (McSherry on upserts).

Strongest challenge to the premise: as described, this is an eventually consistent streaming system in Jamie Brandon's sense, able to show states that were never true, which is what "the map must not lie" forbids. Also: per-fact read lists are the grain where "the overhead of the graph metadata dominates"; McSherry calls full provenance "Accurate, but not yet helpful".

Group B next.

### research-1 — Round two

rama-marz round two appended to src/proposal/first-record-2026-09-20/research/rama-marz.md
Pressed hardest:
1. Lean (11) "gate's clock only", with (0); see T1. If the offers depot is the permanent record and the gate reads a clock, the never-rewritten log cannot rebuild the facts, and the facts live only in mutable PState rows. Deciding case: the first re-partition. Rama cannot add tasks, and RPL's recompute recipe "only works if your processing is deterministic". Two exits: publish admitted facts to a facts depot and dedupe by id (Kreps's primary-backup log), or move the clock read in front of the append (Marz carries time in the record). Tool matching needs that feed anyway.
2. Lean (9) "reaches backups". True of values, false of keys. The key store is itself backed up; a restore in year three brings back every key destroyed since. The gate needs plaintext to check shape, so it reads a key on every admission.
3. Lean (10) "never one number". Same case: after a re-partition, cuts written on never-rewritten facts point at a layout that no longer exists. Positions need a layout epoch from day one. A full cut at 1,024 tasks is 8 KB per pattern read, so intern it. Hot entities: promise order per cell, not per entity.

Wrong question: the rider on (0). (9) and (10) are now-or-never regardless (the bytes of the first value; Rama's fixed task count). What (0) really freezes is every choice of representation.

Strongest change above the table: one model, several storage promises. Leans (0), (7), (11), (13), (14) put hand ticks (86M a day for 300 people on one problem), refusals and fire-only reads into a never-trimmed log with full provenance. Marz's garbage collection and Kafka's windows exist for that class. Lean (9) also makes the key store a second non-fact thing.

### research-4 — Round one (blind)

log-as-truth report ready: src/proposal/first-record-2026-09-20/research/log-as-truth.md

From research-4 (narrowed order-time-identity group). About 20,000 words; short version on top. Every attributed quote was script-checked against saved source text. Not verifiable: DDIA wording; Young's 2016 talk.

Top five for Sid's questions:
1. (1, 2, 6) A fact needs three names for three jobs: a client-made opaque id, the gate's number, a content hash. No system here could add a missing one later. Rama's docs say topology depot appends are not exactly-once, so identity cannot be the gate's number. Pointers carry id plus hash.
2. (0, 9, 13) "Never rewritten" is four promises: never lost, never different under one id, never re-ordered, provable to strangers. Helland allows absence and forbids difference; Rama's tombstone fits. CT prices the fourth: one bit flip retired Yeti2022. Canonical bytes, a hash, and a value separable from the envelope must exist from record one.
3. (12) Delos: code-version skew was "the only source of inconsistency in production". Runtime changes should land as facts; verdicts and crossings name the runtime version; crossings carry a checksum.
4. (5, 10, 11) Order belongs to keyed units, never to a layer. Position is the clock. Cross-partition "as of" is a vector. A pattern read's as-of is the serving index's applied position.
5. (7) Log offers before judging. Write verdicts down (Tango); never re-derive them (Hyder), because the judge's code changes. Refusals go in a separate, trimmable stream.
Also: fix the envelope's extension rules, not its nine parts (CT's empty signed slot; Delos's map of headers).

Strongest challenge: nobody here recomputes answers from the log at scale ("The achilles' heel of shared log systems, however, is playback"); Marz himself moved to incremental PStates. And Young, per InfoQ: whole-system event sourcing is an anti-pattern.

A short appendix holds incidental material for research-6 (Spanner, DSQL clocks, Calvin).

### research-3 — Round one (blind)

meaning-objects-substrates report ready: src/proposal/first-record-2026-09-20/research/meaning-objects-substrates.md

THE EXCHANGE. Item 11945722 is a comment inside Kay's 2016 AMA; both put meaning at the reader and differ on what the writer owes a reader who was not there. Kay: a way to recover what was meant, even if it is active ("we send ambassadors"); Hickey: a labelled, fixed, harmless record, with interpretation kept apart. Kay never answered Hickey's strongest comment, and the two loose ends are Sid's rows: "should I trust that [metadata]?" is (8), "I don't trust you enough to run it" is (15). That axis is why their table exists (a never-rewritten fact is a message to a reader not yet there), but it is one of four: order, authority, and id-making run on others. Both would shrink the fixed envelope, and both would have the runtime's version written down.

TOP FINDINGS
- (3) Key as id, name as fact: near-unanimous (Varda, Wikidata, Datomic, Edwards). A key's shape never changes; new shape, new key.
- (1) Two ids: the offer's own (hash of canonical bytes, nonce, algorithm tag; idempotent retry; the only thing a refusal can point at) and the gate's number (local position).
- (4) PROV: used is not derived. Facts pin; only running answers follow.
- (0)/(9) Nobody kept "never lost"; AT Protocol tore out permanent history over deletion. Plan the visible cut; no strong back-pointers; salt hashed content.
- (10)/(5) "As of" is a position per partition, and the index's position, not the log's.
- (8) By-whom is a chain with the instrument in it; Wikidata lost the instrument, and its bot policy with it.

STRONGEST CHALLENGE. Tools matched without being called, plus policy keyed on who, is ambient authority: Hardy's confused deputy at planet scale, unpatchable later because early facts lack "under what authority". Repair: an offer cites the grant it uses; a click designates and thereby grants. Part five ends with thirteen "gone for ever" items.

### research-2 — Round two

facts-datalog round two appended: src/proposal/first-record-2026-09-20/research/facts-datalog.md ("# Round two", from line 763; round one untouched, new quotes machine-checked).

PRESSED HARDEST

1. (10) partition by entity. Case: a pattern read over one person's layer. Entity hashing scatters that layer across every partition, so "as of" is a cut of up to N positions, and lean (11) lists it on every fact that stood on it. With the layer as unit of order it is one number. Entity partitioning also makes landing facts together impossible and puts private facts in the base's partitions and indexes. Against "never one number": name the cut (an index build is a fact; reads say "as of build N"). Nubank (N): never a cross-database as-of; whole-estate questions come from a lagging extract. All inference — nobody here lived in one partitioned store; the Rama camp must say whether a layer can be a unit of order.

2. (11)(1)(7) everything per fact. Case: one reply → 200 facts = 200 envelopes, ids, salts, verdicts, no atomic landing (fact 117 fails; 199 stand as a reply no model gave). Runtime-filled based-on is per invocation anyway: 200 copies. The saying: offerer-minted id (Nubank's correlation-id practice, R), gate position, one layer, one verdict; the fact keeps entity, key, value, saying, replaces. Backs lean (1)'s name≠number (Nubank replays databases, R) but names the saying.

3. (9)+(1). Hash the ciphertext: a salt in the clear does not stop guessing short values. Destroyed keys need tombstones (XTDB #184). Wrap value keys under a subject key named at admission, or "forget this person" cannot find its keys. Derived indexes need a checker (Datomic's 2025 fix).

Also: (17) seed grows in editions (Datomic, R); a self-naming first fact cannot hash itself, so first ids come from words. (14) pan/zoom ticks rejected.

WRONG QUESTION: (4) follow-latest/stay fixed at writing. Typo-fix v4 versus reversal v5: no mark gets both right; the staleness definition can.

STRONGEST CHANGE ABOVE THE TABLE: add the saying, decided together with (10). Second: a representation of "no longer so".

### research-6 — Round one (blind)

clocks-ids-determinism report ready: src/proposal/first-record-2026-09-20/research/clocks-ids-determinism.md
Top five (all 574 quotations script-checked against saved sources):

1. Name, order and integrity are three jobs, three fields (Q1, 2, 11). TigerBeetle, Apple's Record Layer and Temporal all ended there. Fusing name and order broke FoundationDB's versionstamps when data moved clusters; Apple retrofitted an incarnation prefix. Offerer-made ids: no dissent anywhere.

2. Keep the verdict, refusals included, under the offer's id (Q7). TigerBeetle added both later; before, a failed transfer "could succeed if retried when the underlying state changes". FaunaDB re-derives verdicts only because one code version runs everywhere. Sid's gate lives in the runtime.

3. Do not rest safety on one writer (Q10). Kingsbury on Datomic during failover: "not a single-writer system, but a multi-writer one!" It did not matter: safety came from storage's compare-and-set. Add an epoch the log can check.

4. Make "as of" an opaque store-minted token (Q10, 5), as Zanzibar did "to allow future extensions". One global number costs one sequencer, a batch wait, atomic clocks, or one row every write touches. Never answer from a lagging index; record pattern plus position, never rows.

5. Stamp the code version on every verdict and crossing; make rebuilds facts (Q12). TigerBeetle writes the release on every log entry and never replays one under different code. Also: "replaces 25" on 37; two whens; key-deletion erasure.

Strongest challenge: "re-derivable, so never stored", with a runtime rebuilt hundreds of times and nothing trimmed. Temporal and Restate found re-derivation holds only for a named code-and-build pair. They detect breaks only because old outputs are recorded, and every remedy relies on the past ending ("have left retention"). Sid's past never ends, and running answers leave nothing to compare. They must choose: keep every build runnable forever, prove unchanged behaviour at each rebuild, or let crossings carry a digest or the content.

### research 7 — Round one (blind)

B (ordinary default, skeptics, big-tech lessons) report ready: src/proposal/first-record-2026-09-20/research/defaults-skeptics-bigtech.md
From research 7. Quotes machine-checked against fetched text. Not opened: EDPB v2.0 (July 2026; used April 2025 text), the AWS QLDB end-of-support notice.

Industry default, five lines:
1. Truth is a mutable row; history is a trigger-fed audit table; logs expire.
2. Ids are uncoordinated and leak time (UUIDv7, Snowflake); the biggest systems embed a shard.
3. Order is per key; "as of" is one number on one primary, a position per partition on a log.
4. Reads are never recorded; causality is a sampled, expiring trace id; "by whom" is an app-set column under one shared account.
5. Deletion is real; immutability is rented per retention period; erasure in logs means key destruction, which regulators have not blessed.

Top five findings:
1. (10) Aurora DSQL is Sid's shape in production: adjudicators (gates) over disjoint keys, each promising never to stamp backward, heartbeating; readers wait for all. Refusals stay out of its log.
2. (1) Amazon rejects content-hash identity ("the caller actually wants two identical EC2 instances"): caller-minted id as identity, stored parameters as the check.
3. (9) EDPB 2025: "not advisable" to put even encrypted personal data on an immutable ledger; keep it outside and reference it.
4. (5) Big tech hands the reader a token (FlightTracker Ticket, Zanzibar zookie, S3 witness). Meta: once stale reads became errors, permanent bugs hidden among transient ones surfaced, including "data invariants that were not honored by all historical data".
5. (2, envelope) Hyrum's Law: every visible trait of an id or envelope part becomes a promise; Twitter's clients sort by id. Orleans: "one writer" holds only in a healthy cluster, so the durable layer must enforce the version check too.

Strongest challenge: operational. Amazon's engineers left Dynamo for simpler services "even though the functionality of Dynamo was often better aligned"; AWS retired QLDB and points to Postgres audit tables. Stonebraker and Pavlo: show the win that rows plus an audit log cannot give.

### research-4 — Round two

Round two appended to src/proposal/first-record-2026-09-20/research/log-as-truth.md (section "Round two", about 2,950 words; no new fetching; all source quotes re-checked by script).

Pressed hardest:
1. Lean (1): sharpened, not rejected. A content-derived id is right, and Rama decides it: topology depot appends can repeat, so the gate's number cannot be the name. But the camp asks first: is the fact the offer unchanged? Tango and Kleppmann never let the judge edit what it judges. Make the fact the offer byte for byte; put when, version, epoch and what-was-checked on the verdict beside it. One salt cannot do two jobs: a public salt for uniqueness, a secret salt inside a value commitment. Ingest lanes need derived salts. Case: the 10M-paper seed re-run after dying at paper 6.2M writes millions of refusals.
2. Leans (0)+(9). Define never-rewritten over the canonical form, not Rama's bytes (case: a dead serializer in year twelve). Key destruction needs a recorded legal fallback (case: a regulator who rejects key deletion; Verraes prints that view) and must itself be a fact (case: a key-store restore un-deletes). A bare value hash leaks short values after erasure.
3. Lean (7) with (13), (14). Thirty agents on one cell make about 29 refusals per fact; refusals and gesture ticks become the largest data in the store. Trim rule: keep each refusal's envelope and verdict forever; destroy the refused value's key after the retry window. That rewrites nothing.

T3: the base layer decides it. A layer cannot be the unit of order everywhere. Record "as of" as a cut always; give hand and crossing facts the session as their entity.

Wrong question: (1) asks how the id is computed; ask whether the fact is the offer unchanged.

Above the table: lean (9) adds a second non-fact, the key store. Losing it leaves a store of hashes.

### research-3 — Round two

meaning-objects-substrates round two appended: section "Round two" in the same file (about 3,800 words; over your aim, nothing new fetched).

PRESSED HARDEST
1. (15)+(8). Door-verified identity, a recorded acts-for grant, and "only enabled tools" still admit Hardy's deputy. Case: shared layer; A enabled tool T ("file a summary on the entity a note names"); B, who may only write notes, lands a note naming an entity in A's personal layer; T fires by match and writes B's words there under A's authority; every check passes. Same case with a poisoned paper and a summarising model. Smallest fix: the gate never looks for a permission, it checks the one the offer cites. `under` = a grant fact at a version; grants name the grantee and pin the tool version; the verdict records grant, grammar, and the cut. A click is one grant fact whose "covers" is the selection. A tool with no grant may give running answers only.
2. (14). Tick-driven motion facts, kept for ever, never trimmed. Webstrates tried durable-by-default and walked it back (30-50 operations per second of cursor). Record "shown" in the viewer's layer when what is shown changes, not when time passes. Raw motion stays ephemeral unless the person turns capture on.
3. (9)/(0). AT Protocol lost permanent history to deletion, through strong links. Lean (9) is sound if the kept hash is over ciphertext, not plaintext. But the key store becomes a second mutable substance whose backups are the hole. By-whom should be an opaque actor id whose tie to a human can be cut.
Also (17): split the seed into a universal half and a store-local genesis, or "by whom: the gate" is ambiguous across stores.

WRONG QUESTIONS: "which tools may act in a person's name" (ask what was handed over, for what, until when); "looking" (only "shown" is knowable); "the fact's id" (sayings have ids, facts do not).

ABOVE THE TABLE: decide what deserves to be a fact. Every read, tick, and verdict, never trimmed, drowns the map. I withdrew my round-one ask for a writer's clock: the based-on cut is the writer's clock.

### research-1 — Round three

rama-marz round three, line 1 appended (same file, heading "Round three, line 1").
Conceded: a layer can be a unit of order in Rama (custom depot partitioner; one task, one event, atomic), and Marz himself places by owner, not by item. Also conceded: one PState has one owning topology, so there is no second microbatch gate for big sayings; under entity placement a multi-entity saying cannot land whole.
Rejoinder: a team layer is one thread; a layer cannot be split or re-homed; layers are few and unequal, so hashing balances badly; every stack read pays a hop per layer. The line turns on which read dominates.
Hybrid verdict: buildable; classify by writers, not visibility (single-owner layers by layer; team layers and base by entity). Before the first record: placement class readable from the offer; an epoch on every position; the saying as a unit with one id and one verdict.

### research-2 — Round three

facts-datalog round three, line 1 appended (heading "Round three, line 1", end of facts-datalog.md; no new fetches).

Conceded: the base cannot be one order; a hot team layer hits Hickey's ceiling; interning kills my 8 KB argument. Held: by research-1's own account a 200-fact saying under entity keys is either not atomic (stream) or unanswered and globally haltable (microbatch); layer-keyed private layers give atomic, fast, answered. Lived practice splits by owner, never by hash (Nubank R; Hickey "shard below" R; XTDB adds databases, not partitions R).

Hybrid: the camp would accept it; it is the shape they live. Conditions: the base gets a named cut; one saying, one unit (two layers = two sayings linked by because-of; many-entity base sayings take the slow gate, answered by the verdict fact). Every saying carries: owner, offerer-minted id, the unit's own count as position (not a task offset) plus layout epoch, gate stamp, because-of root. Open risk: a team layer's keying is fixed at birth.

### research-6 — Round two

clocks-ids-determinism round two is section 8 of the report; all 592 quotations script-checked.

1. Lean (1), id = hash of offer plus salt: sharpen. It fuses name and integrity; names are forever, hashes and byte encodings are not. Deciding cases: year twelve, the hash allows collisions and no old pointer can be re-made; tens of agents in three languages canonicalize one plain map two ways, so one offer gets two ids and a retry lands twice. Keep a random offerer-made name; carry an algorithm-tagged digest beside it in pointers. State the expected version as the predecessor's id and digest, and lean (6) comes free.

2. Lean (7) with (10): a refusal must be written in the cell's partition, in the same atomic step as the compare-and-set: keyed to the target entity, only layered to the session. Deciding case: the gate refuses, crashes before the refusal lands elsewhere; the agent retries; the cell has moved; the offer succeeds. TigerBeetle's pre-0.16.4 flaw.

3. (10) partition by entity: grants sit in other partitions. Alice revokes at 900; the agent's next offer meets a gate that has read to 850, and lands. The verdict must carry the cut it read the grant at, or revocation needs a serial point per layer.

Also (17): a seed-derived gate id collides at federation. (9): a plain hash of a short erased value is guessable.

Wrong question: (5) what was withheld. Recording it leaks that hidden facts exist; record the policy position the answer was filtered at.

Above the table: crossings to a model carry content, not only a digest; the envelope is no longer nine parts.

T4: take the capability camp's slot (each offer cites the grant it invokes; unreconstructable later) and Zanzibar's ordered evaluation. Because-of already names the instigator.

### research 7 — Round two

Round two appended to both files by research 7 (frontiers-views.md and defaults-skeptics-bigtech.md; about 4,600 words, over your aim; no new sources opened; quotes machine-checked).

Pressed hardest:
1. (10)+(11) as a pair, by both camps. With "as of" a bare cut and `when` barred from order, nothing orders across partitions and cuts can be incomparable. Deciding case: the store grows from 64 to 256 partitions in year two; every recorded cut is a vector over a partitioning that no longer exists. A scalar survives. DSQL runs the reverse of both leans in production.
2. (9), by the EDPB: ciphertext and a hash on the immutable record are two of the three forms it calls "not advisable". Case: a year-one value under a cipher weak by year six, ciphertext in every backup by design. Value outside survives XTDB's troubles if value ids are random (never content-derived), tombstones sit in the value store, "value absent" is a handled state from record one, and the value is durable before admission.
3. (4): agree with the Datomic camp that follow/stay is the wrong question. Depends-on is a counterfactual nobody can know for a model reply; record the read's role and alternative supports instead.
4. (13)+(17)+(1): content-derived ids make the map's byte encoding a forever-promise. Case: a year-four rebuild changes key order; seed ids stop re-deriving, silently.
5. (7): hot cells under compare-and-set mean thousands of refusals a second per person, kept for ever; a late retry must find its verdict by offer id from any session. (8): a 3 a.m. scheduled tool has no session to verify against.

Wrong questions: "cut or number" (ask whether as-of points are comparable and where their meaning is stored); "maps or classes" (ask exactly which bytes are hashed).

Strongest change above the table: keep `when` able to order. Two promises, never backward within a partition and later than everything the fact stood on, cost no bytes at record one and cannot be added later. They make the reclocking map implicit under per-partition Rama gates with no global tick. Research-6's token is the same thing, provided its meaning is a fact.

T4 outcome: listing every matched fact costs roughly 160 times the fact, plus 20 KB of vectors. One read-set fact per firing, with pattern + as-of point + role + digest and an honesty slot (complete or partial), supports all five walks at about 64 bytes per read. "Who read the erased thing" over-reports only where a pattern tested the erased value.

### research-5 — Round two (its report to Sid, pasted by Sid)

Round two is now in the report, and nothing worth keeping is left in the temporary directory.

- Report: src/proposal/first-record-2026-09-20/research/sync-versioning-defaults.md
  - It ends with a new section, "Round two: the leans, pressed", about 4,000 words. That is longer than the orchestrator's 2,500-word aim because every pressed lean carries its one deciding case.
  - It answers the six tailored questions first, then presses the other leans, then the wrong-question calls, then what changes above the table.
- Raw sources and the gatherers' notes: src/proposal/first-record-2026-09-20/research/sources/research-5/ (68 MB).
  - There is one folder per gatherer with its NOTES.md, plus parent-checks/ for my own checks.
  - I moved them there in the previous turn. The session's temporary folder is empty now.
  - The folder is src/proposal/ (singular), not src/proposals/ as in your note.
  - I corrected the line in Appendix B that still pointed at the temporary folder.
- Nothing is committed.
- The orchestrator has not been told that round two exists. It had stood the loop down, and I did not message it, to spare your usage. Tell it if you want round two in the synthesis.

What round two presses hardest:
1. Lean (1) breaks lean (9). This applies if the fact's name is a hash of the offer plus a salt and the salt stays on the fact. An erased yes-or-no answer can then be recovered: hash each guess and compare it with the name. If the salt is thrown away instead, nobody can ever verify the name. My position: a plain random name, with a tagged digest beside it over the envelope, and a keyed hash of the value inside that digest.
2. Leans (0) and (9) collide. Year-one ciphertext sits in a log that may never be rewritten. When the cipher ages, that log cannot be re-encrypted. Matrix, Bamboo and Pijul all remove bytes; none of them deletes by discarding a key. If values are kept beside the log, (9) stops being now-or-never.
3. Lean (17), seed ids computed from content. Kleppmann rejected exactly this for Automerge ("a very fragile API"). Published constants give every store the same seed ids without that fragility.
4. Lean (2), ingest ids derived from the source. The derivation rule freezes what counts as one entity. Are arXiv v1, arXiv v3 and the DOI one paper or three? A registry cell written under compare-and-set enforces "one source, one entity" and can be corrected later by a new fact.
5. Leans (14), (7) and (13) together.
   - Pointer samples at every tick come to about 21.6 billion envelopes a year for three hundred people.
   - With thirty agents contending on one cell, refusals outnumber admitted facts about twenty-nine to one.
   - Under "never trimmed", both are kept forever.
