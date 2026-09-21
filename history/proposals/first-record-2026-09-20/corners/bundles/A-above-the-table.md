# A above the table: main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L598-599 · ABOVE · NEW-REASON A1,E2**
*7. What this camp would question above the table*

**"Running answers never stored, only crossings recorded."** Strong agreement, in Hickey's own words. A derivation that updates live is not data: "'date-of-birth' is data and 'age' (unless temporally-qualified, 'as-of') is not." A crossing is a derivation pinned to a basis: *this was shown, built from these reads.* That is an event that happened, so it is a fact. It is also Datomic's rule about caching: cache the sources of answers, never the answers. This project's standing rule against reaching for caches has the same root.


---
**datalog L1022-1023 · R2 · NEW-REASON A1,E2**
*Round two › R2.4 Ask C: what they would change above the table*

8. **"Running answers never stored, only crossings recorded": unchanged, and strongly held.** R: "'age' (unless temporally-qualified, 'as-of') is not" data [H-HN16]. A crossing is the temporally qualified form.


---
**frontiers L421-421 · ABOVE · ABOVE A1,E2**
*9. What this camp would question above the table*

- **Running answers never stored; only crossings recorded.** Agreement that derived state is never the truth. Disagreement if "never stored" means "never held". This camp's whole craft is holding indexed state so that recomputation is cheap, and evicting it safely (Noria). Crossing records need an as-of, the tool version, and the runtime version to be re-derivable (REPORTED for Feldera; INFERRED for Sid).

---
**log L44-44 · SHORT · ABOVE A1**
*The short version*

- *Nobody here recomputes derived answers from the log at scale.* vCorfu: "The achilles' heel of shared log systems, however, is playback." Ten of twenty-five event-sourcing engineers in the Overeem study complained that rebuilding is slow. Kleppmann: "In systems with a high event rate such replay may not be feasible." Marz himself moved from Lambda's recompute-from-scratch to Rama's incrementally maintained PStates. The camp stores derived state everywhere, never trusts it as truth, and keeps it honest by stamping it with the log position it reflects. Sid's rule that running answers are never stored is stricter than any system in this camp. This touches Sid's standing rule on caching, so I flag it for their own adversarial look rather than recommend anything.

---
**log L45-46 · SHORT · ABOVE -**

- *The practitioners warn against one substance for everything.* InfoQ's report of Greg Young's ten-year retrospective: "CQRS and Event sourcing are not top-level architectures and normally they should be applied selectively just in few places." Kleppmann in 2021: "there is no one true way". The transfer is not clean (for Sid the audit trail is the product, not an add-on), but the costs they name are real and permanent: versioning forever, rebuild time, names that can never be renamed.


---
**log L51-52 · SHORT · NEW-REASON A1,P0**
*The camp in one picture*

**What they share.** The log is the truth and everything readable is derived from it. Helland: "The truth is the log. The database is a cache of a subset of the log." Kleppmann: "A materialized view is just a cached subset of the log, and you could rebuild it from the log at any time." Marz: "PStates are essentially materialized views of depots, with depots being the source of truth." Records are not edited; a correction is a new record. Helland: "Accountants don't use erasers". Whoever replays the same log must reach the same state, so anything non-deterministic is captured as data before it enters the log.


---
**log L542-543 · ABOVE · ABOVE -**
*For the group › 8. What this camp would question above the table*

**An append-only store of small facts as the one substance for everything.** Split verdict. For: Helland's outside data is exactly this, Marz builds whole backends this way, and Kleppmann in 2026 wants "the database as common ground" for humans and agents. Against: the InfoQ report of Young's retrospective calls a whole system on event sourcing an "anti-pattern"; Kleppmann says "there is no one true way"; Kreps calls it "hubris to think you can do better than all of them in a single system". Helland's own split is between inside data (private, mutable, schema'd by one owner) and outside data (immutable, identified, shared). Sid's design makes all stored data outside data and leaves the inside as the unstored running answer. That is coherent, and nobody in this camp has run it. The transfer from Young is imperfect: his teams paid event sourcing's costs in places that did not need an audit trail, and for Sid the trail is the product. The costs are still real.


---
**log L550-551 · ABOVE · ABOVE A1**

**Running answers never stored; only crossings recorded.** The camp's strongest pushback, given in the short version. They would protect two things inside Sid's rule: derived data must never become a source, and it must always be disposable. They would drop the word "never". Their replacement principle is that derived state is honest if it carries the position it reflects: Aurora's pages, Tango's views, a CT tree head, a Kafka consumer's offset. A cache hides how old it is; a view that names its position cannot. Whether that distinction survives Sid's own examination of caching is for that examination. I only report that every system here, including Rama itself, keeps derived state, and that the two that tried to live on playback (Tango's clients, Lambda's batch layer) were both corrected by their own authors' next systems.


---
**log L554-555 · ABOVE · DISAGREES A3,W4**

**One writer.** The camp agrees on one writer per ordered unit and rejects one writer per system. CORFU's sequencer is "merely an optimization". Aurora's single writer was a "simplifying assumption" that its successor dropped. DSQL gives each key one adjudicator and has as many as it needs. For Sid the phrase should mean: for any cell, at any moment, exactly one gate, fenced by an epoch.


---
**log L713-714 · R2 · ABOVE A1**
*Round two: the leans, pressed › T5. What must exist from record one so that nothing needs a *

The premise is more survivable than my round one made it sound. Summaries are offers, so the largest derived things are already facts, and Rama's indexes are stored. The heel moves to index rebuilds.


---
**meaning L2641-2650 · ABOVE · ABOVE -**
*Part five — for the group › 8. What this camp would question above the table, ranked by *

**5. Small plain facts as the one substance.** Medium. Kay: "I wanted to get rid
of data"; "the last thing you wanted any programmer to do is mess with internal
state" (2.7). The point under the rhetoric: a gate that checks one fact's shape
cannot protect a rule that spans many facts. Objects protect such rules by hiding
state. Facts cannot hide. Webstrates names the two failure modes of any single
substance (3.3). Wikidata put functions in another wiki and took six years to add a
new kind of entity (2.5). Hewitt: arrival order is not deducible from facts
(2.11). Against the challenge: Kay's side has not built the alternative at scale,
by its own account (2.7).


---
**meaning L2651-2659 · ABOVE · ABOVE -**

**6. Tools, grammars, policies, and definitions as facts in the same store.**
Medium to weak. This camp mostly loves it: Smalltalk, STEPS, Realtalk in Realtalk,
Wikidata's constraints, the nanopublication trust root. Their cautions: meta-level
change needs fences, and Sid's layers are a good fence (2.7). Policy in the medium
opens a time-travel hole (3.3). Definitions as facts can swell (2.5). In practice
few people edit the kernel (3.1), and STEPS never finished the bottom (2.7).
O'Keefe's question to Armstrong stands: how much of a shared sea of functions can
you change before you need the backups (2.9)?


---
**meaning L2660-2665 · ABOVE · ABOVE A1,C2**

**7. Running answers never stored.** Weak as a challenge; the camp agrees (Croquet,
Folk, Realtalk, Hickey). Conditions they would attach: determinism has to be
enforced, not assumed (2.8); the interpreter's version has to be among the reads
(1.7, 2.3); and an index *is* a stored running answer, which is fine if it is
disposable and its lag is written down (5).


---
**meaning L2681-2682 · ABOVE · ABOVE -**
*Part five — for the group › 9. Questions this camp would call the wrong question*

- **"One truth."** One record of who said what and in what order. Beliefs stay
  plural (Hewitt, Wikidata, Xanadu's "There are no official truths").

---
**meaning L2683-2684 · ABOVE · ABOVE -**

- **"Where does meaning live?"** Kay and Hickey both answer: at the reader. The
  live question is what the writer owes a reader who was not there.

---
**meaning L2952-2959 · R2 · NEW-REASON A2,A1**
*Round two › T4. The runtime outside the substance*

### T4. The runtime outside the substance

**Is a recorded version name enough for Kay? No (I).** A version name points at
something outside the store that will not exist in year eight. That is Kay's own
objection: "How can you find it?" (R, §1.2). The 1978 image came back because its
machine was small and the image carried the rest (R, §1.2). Lean (12) is Hickey's
half: an as-of for the interpreter. It is needed. It is not Kay's half.


---
**meaning L2975-2986 · R2 · NEW-CASE A1,A2**

**The case.** Year eight. The runtime is rebuilt in another language. The
definition of "stale", a fact from year one, now answers differently on the same
reads, because text ordering or number handling differs slightly. Staleness
shifts silently under every fact ever written. Nobody can say which build is
right, because "right" only ever meant "what build one did". Unison lived this
when its inference changed: "there seems to be no way to replace the 'variant'
associate with a hash" (R, §2.3). Croquet had to add runtime checks for code that
reads a clock (N, §2.8). With the definition in the store, the reference
interpreter's answer is the answer of record, and the new build's failure is a
fact. The second institution's store makes this unavoidable. It will run other
code, and can share meaning only through a definition that travels with the seed.


---
**meaning L3139-3145 · R2 · ABOVE -**
*Round two › C. Above the table, after the leans*

4. **The pile of things that are not facts is growing.** Round one had one: the
   runtime. The leans add the door's session check, the key store, and the
   indexes. Ingalls: "An operating system is a collection of things that don't
   fit into a language. There shouldn't be one." (R, §2.7). Keep the list short
   and visible: one fact per such part, saying what it is, its version, and what
   it may do. Put the floor's meaning in the seed (T4). The gate's walk along a
   grant chain is itself a program, and its language needs the same treatment.

---
**meaning L3150-3152 · R2 · ABOVE -**

6. Unchanged: rules that span many facts, which no per-fact gate can protect
   (Part five, 8.5).


---
**rama L457-458 · ABOVE · ABOVE A1**
*The group › 8. What this camp would question above the table*

**Running answers never stored.** His whole career argues the other way: "The most obvious alternative approach is to precompute the query function." Views are safe to store because they can be recomputed. He would say store them, as PStates, and never let them be truth. That may already be what Sid means, since a PState is not a fact. On recording crossings he has no position beyond Agent-o-rama's traces of what went to a model.


---
**rama L461-462 · ABOVE · ABOVE A3**

**One writer.** Full agreement from both. But both mean one writer per partition. Neither means one writer for the world.


---
**rama L621-621 · R2 · ABOVE -**
*Round two › T5. One cluster worldwide*

1. The leader does everything, including reads: "The leader is responsible for performing all work for each of the tasks in its task group: running ETL code, updating PStates, appending to depots, and serving PState client queries." (`docs/21-replication.md:30`). Followers only apply. More replicas do not bring reads closer to anyone.

---
**rama L623-623 · R2 · ABOVE -**

3. Replicas of a task group go on different nodes (`:24`). Zones or regions: NOT IN THE REFERENCE.

---
**rama L624-624 · R2 · ABOVE -**

4. One Conductor per cluster. Zookeeper holds leader election and ISR membership (`:152-161`). The Zookeeper session timeout defaults to 5000 ms (`docs/26-all-configs.md:23`).

---
**rama L626-626 · R2 · ABOVE A2**

6. A module update pauses appends for 2 to 30 seconds, for the whole module at once (`docs/19-operating-rama.md:441`).

---
**rama L631-634 · R2 · ABOVE -**

11. R, Marz 2023: "each user will just have their own cluster", meaning each Rama customer.

**Unknown.** Everything about wide-area behaviour. The reference never mentions it.


---
**rama L637-637 · R2 · ABOVE -**

1. Has any Rama cluster run with nodes in more than one region? What round-trip time between replicas has been tested?

---
**rama L641-641 · R2 · ABOVE -**

5. Which timeouts assume a local network (the `replication.*` options, the 5-second Zookeeper session, `topology.stream.timeout.seconds`)? What values are safe at 150 to 300 ms round trip?

---
**rama L646-646 · R2 · ABOVE A2**

10. Can a module update roll region by region, or reach zero downtime for appends?

---
**rama L649-650 · R2 · ABOVE -**

13. Licence terms for one cluster of thousands of nodes run as a public service. Does the client version lock mean a front tier is assumed?


---
**rama L718-718 · R2 · NEW-REASON A2,C8**
*Round two › C. What the leans change above the table*

3. **The door is part of the runtime.** Lean (8), plus Rama having no authentication and locking clients to its version, makes the door a required tier and in practice the only client. Its build belongs on the record next to the gate's.

---
**rama L719-719 · R2 · ABOVE A3,P0**

4. **"One writer" needs an object.** One writer of what? Under shape (b) the gate writes rows and publishes the log. Under shape (a) clients write the permanent log and the gate writes only rows.

---
**skeptics L281-281 · ABOVE · ABOVE A1,E6**
*6. What this camp would question above the table*

- **Running answers never stored; only crossings recorded.** MacCárthaigh, REPORTED: "most caches have modes". The DynamoDB paper, REPORTED: do not let caches "hide the work that would be performed in their absence". INFERRED: this camp agrees with not trusting derived state. It would add: provision for the day everything must be recomputed at once.

---
**skeptics L283-284 · ABOVE · CARRIED A3,C3**

- **One writer.** Amazon moved *to* this (DynamoDB). Orleans warns that it holds only when the cluster is healthy, so the durable layer must enforce the check too. DSQL shows the scaled form: several adjudicators over disjoint key ranges.


---
**skeptics L285-286 · ABOVE · ABOVE -**

**The strongest challenge this camp makes to the premise.** It is about operations, not correctness. The better-fitting system loses to the one people can run. Amazon's engineers left Dynamo for simpler services "even though the functionality of Dynamo was often better aligned with their applications' needs". AWS retired its ledger database and pointed customers to Postgres with audit tables. Stonebraker and Pavlo's test is whether the new construct gives a large win that rows plus an audit log cannot. For Sid the honest candidates are three: reads as data, staleness shown as loudly as content, and tools that live in the store. The ordinary default truly does not provide these. The S3 and FlightTracker stories show what their absence cost. Everything in the design that is *not* one of those three is where a skeptic would say: use the boring thing.


---
**skeptics L442-442 · R2 · CARRIED A3**
*Round two (2026-09-20): the leans, pressed from the skeptics › C. Above the table: what the leans change*

- One writer: Orleans (R) still applies. The version check must hold at the point of durability, because during a failover two gates can each believe they are alone.

---
**sync L123-130 · SHORT · ABOVE -**
*1. The short version*

10. **The camp's strongest challenge is to "no optimism" with one worldwide
    gate.** Bayou built a commit-only view and found that applications "never
    select the commit-only option". Boodman calls local-first response "just a
    matter of physics". Weidner shows strict waiting can starve a client. The
    camp's own resolution is honest tentativeness: show the unconfirmed thing,
    marked, and carry the mark through every query (Bayou's two-bit tags).

---

---
**sync L3056-3069 · ABOVE · ABOVE -**
*5. The group › 5.2 What this camp would question above the table*

**"An append-only store of small facts as the one substance for everything."**
Figma found that one conflict rule did not fit all kinds of value: design
properties got last-writer-wins, and code text got a causal event graph, inside
one product. REPORTED. A cell under compare-and-set is a poor home for text
that two people and an agent are typing into; every keystroke contends for one
cell. The camp would ask what the fact-shape of that paragraph is. INFERRED.
Figma's server also enforces an invariant across many records (no cycles),
which the brief's gate has no slot for. REPORTED fact, INFERRED gap. tldraw
refuses the ambition outright: "not a general purpose real-time data solution".
Riffle tried one substance down to the scroll position and reported the
migrations. The Irmin retrospective: "tries to do too many things". On the
other side, Gentle's whole argument supports the substance: store what happened
and what it stood on, and nothing an algorithm made of it.


---
**sync L3082-3095 · ABOVE · ABOVE -**

**"One store for the planet with personal layers instead of personal
databases."** The local-first camp objects on ownership, offline work, and
longevity. Staltz: "I fundamentally don't trust any system that has an admin
with sudo powers." AT Protocol's middle road is personal signed repositories
that can change hosts, plus shared indexes. A layer inside one store has no
exit unless export is defined from the start, and an export is a small second
store. INFERRED. Frazee also gives the counter-argument, from having built the
alternative twice: "People won't sacrifice features for hypothetical
improvements." On operations: Figma's one ordered stream coupled everyone's
availability; Dolt says a single primary is the ceiling; Boodman says the
round trip is physics. One thing I did not find anywhere in AT Protocol's paper
or core specs is erasure law. A planet store meets differing national rules on
day one. INFERRED.


---
**sync L3111-3125 · ABOVE · ABOVE A1,E2**

**"Running answers never stored, only crossings recorded."** For: Gentle ("We
never write the CRDT state to disk"); Figma's LiveGraph, which dropped
incremental maintenance once it saw that "most query results never change after
initial load". Against: Croquet snapshots because replay is too slow to join;
Automerge had documents that "hadn't loaded after 17 hours" before its runtime
changed; Convex lists full re-execution as a limit. I report these without
recommending stored derived state; this project treats that road as a reason to
stop and examine, and I agree. The precise challenge is different.
"Re-derivable" is a claim about reads, tool version, runtime version, and
resource limits together (Bayou, Convex, Croquet all say so in their own
terms). After many rebuilds the old runtime may no longer run. Then yesterday's
crossing can be *described* by its reads but not *re-rendered*. A digest on the
crossing makes the drift detectable. Nothing makes it re-renderable except
keeping old runtimes runnable. INFERRED.


---
**sync L3136-3143 · ABOVE · ABOVE A3**

**"One writer."** Nearly everyone with a server agrees: Wallace, Boodman,
Weidner, Jayakar, Linear as observed; Upwelling wished for one; Keyhive
describes what a centre gives while declining to have one. The costs are all
REPORTED: a serialisation ceiling (Replicache), coupled availability (Figma), a
loss window that is never zero (Figma), a single-primary ceiling (Dolt), trust
in the operator (Staltz; did:plc's answer is replicas as witnesses). The camp's
consistent refinement: one writer per *ordering domain*, never one per planet.


---
**sync L3339-3344 · R2 · ABOVE -**
*Round two: the leans, pressed › T1. "No optimism" against one worldwide gate*

**Does a person's own gate, near the person, change the physics?** Yes, for the
person's own work. A gate a frame away makes committed-only display feasible;
Riffle's target was exactly "within a single frame after a write" (R §2.9).
Bayou's finding that nobody chose commit-only reads (R §2.1) loses its force
there. Three things stay slow.


---
**sync L3364-3369 · R2 · DISAGREES A3,C3**

**One new duty.** A person changes device or region, and the layer's gate
moves. Two gates must never both believe they own a layer. SSB's fork kills a
feed (R §2.15); Figma routes every client of a file to one process to avoid
"split brain" (R §2.4). So a position is (layer, gate epoch, number), inside the
opaque token. (I)


---
## Says the same as the ledger (counted, not copied)

- A1 · frontiers · 3: L23-24, L159-159, L277-278
- A1 · meaning · 1: L1597-1604
- A1 · sync · 5: L285-287, L1221-1225, L1236-1238, L1266-1269, L2284-2286
- A2 · frontiers · 2: L95-95, L159-159
- A2 · log · 1: L722-722
- A2 · meaning · 1: L1597-1604
- A2 · sync · 1: L1020-1025
- A3 · datalog · 2: L173-174, L602-603
- A3 · frontiers · 1: L576-577
- A3 · log · 3: L108-108, L333-333, L722-722
- A3 · sync · 1: L1000-1002

