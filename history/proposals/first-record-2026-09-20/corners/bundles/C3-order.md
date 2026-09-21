# C3 order (10): main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L41-42 · SHORT · CARRIED C3,C2**
*1. The short version*

**3. "As of" is one number, because there is one order. And that is a scope decision, not a storage decision.** Datomic: "transaction ids are a total ordering of all transactions." A reader sees "all transactions up to their time basis, in order, with no gaps." [D-MODEL], [D-ACID]. XTDB v2 enforces a single Kafka partition and refuses to start otherwise. Both admit the price in plain words: a hard ceiling on writes. Hickey: "It *is* the bottleneck… nothing is infinite." At real scale the camp does not partition the log. It makes many databases. Nubank runs more than 3000. The unit of total order is the unit of consistent reading.


---
**datalog L594-595 · ABOVE · ABOVE C3**
*7. What this camp would question above the table*

**"One store for the planet with personal layers instead of personal databases."** The camp's deepest doubt. Hickey chose the closed world to avoid "universal naming, open-world, shared semantics". He says his design is wrong for "arbitrary write scaling". Both product teams state a hard ceiling per database. The camp's lived answer at scale is many databases, split by meaning, joined at read time. But the camp has no *good* answer for the planet either. Hickey's complaint about sharding (no query, no transactions, no consistency across shards) applies to the many-databases answer just as much, and Nubank confirms it: counting customers across the estate became "increasingly difficult" and needed an ETL. So what transfers is not the advice to use many databases. It is the narrower point: *one order per unit, and choose the unit by meaning.* Sid's layer is a unit chosen by meaning. Whether a layer can also be the unit of order is a question this camp raises and cannot settle.


---
**datalog L619-619 · ABOVE · ABOVE C3,C2**
*8. Questions this camp would call the wrong question*

- **(10) "Is 'as of' one number or a position per partition?"** That is downstream. The real decision is what the unit with one timeline is. Inside it the answer is one number. Across units it is one number each. A position per physical partition means the unit was chosen by the machinery and not by meaning. Hickey's "shard below".

---
**datalog L771-774 · R2 · NEW-REASON C3,C8**
*Round two*

**One thing to hold in mind throughout.** Nobody in this camp ever lived inside one partitioned store, and nobody in it ever verified an actor. So on (10) and (8) everything below is inference from their principles, not their experience. On (9), (1), (7), (3), (11) and (17) there is direct lived evidence.

---


---
**datalog L787-787 · R2 · CARRIED C3**
*Round two › R2.1 The tailored questions › T1. The unit of saying*

| part of the saying | who writes it | note |
| layer | the offerer states it, the gate checks it | one layer per saying |

---
**datalog L814-815 · R2 · CARRIED C3**

**Layer: on the fact or on the saying?** On the saying. N: a Datomic transaction is submitted over one connection, to one database. R: Shortcut's context attribute is per transaction [SC17]. I: there are two reasons in Sid's world. The gate's policy question has the layer in it, and it is asked once per act. And facts that must land together need one order, so they need one home. An act that writes two layers is two sayings, the second because of the first. Promotion is already "an ordinary offer", so nothing in the design needs a saying that straddles layers.


---
**datalog L835-835 · R2 · CARRIED C3**

3. **Partitioning by entity.** A saying that touches 40 entities needs one order. That collides with lean (10). See T3. This is the real cost, and it is structural.

---
**datalog L881-882 · R2 · CARRIED C3,C2**
*Round two › R2.1 The tailored questions › T3. One order as a scope choice, against lean (10)*

The lean: a layer has one home store. Within a store, partition by entity. "As of" is a position per partition, a cut, never one number.


---
**datalog L883-884 · R2 · CARRIED C3**

**The camp's rule.** The unit of total order is the unit of consistent reading and the unit of landing together. R: Hickey, once data is sharded "you cannot query against shards. You cannot do transactions across shards. You cannot ensure consistency across shards" [H-WD]. So choose the unit by meaning, "shard above" [H-IONS].


---
**datalog L887-887 · R2 · CARRIED C3,E1**

- **The entity** (the lean). Every cell is ordered. Nothing larger is. A saying over 40 entities cannot land together. Any pattern read is a cut. The gate reads policy and grammar from other partitions on every admission.

---
**datalog L888-888 · R2 · CARRIED C3,C2**

- **The layer.** A saying has one home, so it lands together. A pattern over a layer is as of one number. Policy and grammar still live elsewhere, in the base, but that is one foreign position per admission, written on the verdict.

---
**datalog L889-889 · R2 · CARRIED C3**

- **The person** (all of one person's layers in one order). R: this is Nubank's lived unit. "That was the decision we had on sharding, we separated by customer." [N-POD]. And their regret says the owner is the key to split on later. Cost: the session firehose shares an order with the person's durable facts.

---
**datalog L890-890 · R2 · CARRIED C3**

- **The problem.** A shared layer for hundreds of people and their agents. It works as a layer until it is too hot. R on the ceiling: Hickey, "It *is* the bottleneck… nothing is infinite" [H-WD]. XTDB: "300k docs/second written on a single thread" locally [X-LEAD]. Hundreds of people times tens of agents at machine rate is within sight of that ceiling. When it is reached the problem splits into sub-problems, by meaning.

---
**datalog L891-892 · R2 · CARRIED C3**

- **The base cannot be any single unit.** A field of ten million papers with summary layers does not fit one order, and Datomic's own guidance starts to bite at 10 billion datoms of history [D-FAQ]. The base has to be split by meaning: a source document with everything drawn directly from it, a topic's summary layer, and so on.


---
**datalog L893-894 · R2 · CARRIED C3**

**The camp's pick.** The layer is the unit, keyed so that the owner is recoverable. The base is many units.


---
**datalog L898-899 · R2 · CARRIED C3,C2**

2. *A pattern over the shared base.* No unit of order can hold it. Either a cut, or something else. The camp's "something else" is the next point.


---
**datalog L904-905 · R2 · NEW-REASON C3**

**Resembles and differs.** Resembles: many writers, a split by owner, a lagging consistent extract for the questions that cross everything. Differs: Nubank's units are whole databases with nothing shared between them. Sid's units would share one store, one gate design and one base. Nobody in this camp has done that.


---
**datalog L978-979 · R2 · CARRIED C3**
*Round two › R2.2 Ask A: the leans, one by one*

**(10) Order.** See T3. **This is the lean the camp presses hardest.** Partition by entity makes the saying impossible, makes every pattern read a cut, and spreads each private layer across every partition.


---
**datalog L1018-1018 · R2 · ABOVE C3,C2**
*Round two › R2.4 Ask C: what they would change above the table*

4. **"One store for the planet": one order per unit, units by meaning, cuts with names, an owner on every saying.** T3, and Nubank's regret.

---
**frontiers L423-424 · ABOVE · DISAGREES C3,A3**
*9. What this camp would question above the table*

- **One writer.** Agreement that one authority must assign time. Disagreement that one authority must do all the checking on the write path. McSherry: keep heavy work off "the critical path of timestamp assignment" (REPORTED), and consider deciding commits in the data plane as a view over intents (REPORTED sketch). CALM: most keys may not need a version check at all (INFERRED).


---
**frontiers L436-437 · ABOVE · DISAGREES C3,A3**
*10. Questions this camp would call the wrong question*

- **"Can two gates ever write one layer?"** Too coarse. It depends on the key: growing set or register.


---
**frontiers L559-559 · R2 · DISAGREES C3,A3,E1**
*Round two (2026-09-20): the leans, pressed from the frontier › A. Leans this camp would reject or sharpen*

- **(7), sharpened twice.** Materialize's design doc (N): "if N txns are run concurrently, 1 will commit and N-1 will have to (usually cheaply) retry." Deciding case: thirty of one person's agents on one hot cell at machine rate is thousands of refusals a second for that person, kept for ever under (13). Goebel (R) would repair rather than refuse. CALM (I): let the grammar say when a key is a growing set, which needs no version check and produces no refusals. Second, the verdict must name the point at which the gate read grammar and policy. Deciding case: a policy is withdrawn at T1, and a gate that has seen that partition only through T0 says yes at T2. With the point on the verdict, that is explainable. Without it, it looks like a gate bug for ever.

---
**log L546-547 · ABOVE · ABOVE C3**
*For the group › 8. What this camp would question above the table*

**One store for the planet, with personal layers.** The sharpest internal dissent. Kleppmann: a total order costs "one network round-trip to the leader and/or a quorum of replicas" per append, and his local-first work makes the device the primary copy. FuzzyLog: a system-wide order is "expensive, often impossible, and typically unnecessary". Helland: "There is no simultaneity at a distance", and "you can know where you are writing or you can know when the write will complete but not both." CT chose many logs on purpose, so that no single operator is trusted, and Rescorla shows that even so, real verification came to rest on one company. Two separate points hide here. One store is not one order: with Rama's partitions Sid already has many orders, which answers the throughput half. The other half stays open. With no optimism allowed, every act waits for a round trip to wherever its partition's leader lives, and a single operator asks the planet for the trust that CT was built to avoid asking for.


---
**log L561-561 · ABOVE · ABOVE C3**
*For the group › 9. Questions this camp would call the wrong question*

4. **"Can two gates ever write one layer?" (10).** It assumes the layer is the unit of order. No system here orders a namespace. They order keyed units, and a namespace spans many of them.

---
**log L689-690 · R2 · CARRIED C3**
*Round two: the leans, pressed › T3. Keyed units, or the layer as the unit of order?*

The camps agree on the principle. Helland: "You know only when a single unique key unifies both." (R). If a layer is the unit of order, the layer is the key and all of it has one home. The disagreement is only about which key.


---
**log L691-692 · R2 · CARRIED C3**

**The deciding case is the base layer.** If layers are the unit, base is one partition: one leader, one machine's throughput, one region's round trip for the whole planet, one blast radius (R, Balakrishnan 2024). FuzzyLog calls such an order "expensive, often impossible, and typically unnecessary" (R). Any layer written by hundreds of people plus their agents fails the same way. So the layer cannot be the unit everywhere.


---
**log L693-694 · R2 · DISAGREES C3,C2**

**The Datomic camp's point survives inside lean (10).** A personal or session layer, at tens of agents, fits one partition, and there a single number is just a cut of length one. So record "as of" as a cut always. The second deciding case is the hand. Under partition-by-entity, one person's gestures on different entities have no mutual order except the gate's clock, which lean (11) forbids for order. I: give hand and crossing facts the session as their entity. Then the keyed unit is the meaningful unit, which is what both camps want. One warning stays. A personal layer that becomes a team layer must be re-homed, and "Earlier record versions were placed in their old log." (R, Helland). Cuts must name partitions by ids that are never reused.


---
**meaning L2666-2668 · ABOVE · ABOVE C3,A3**
*Part five — for the group › 8. What this camp would question above the table, ranked by *

**8. One writer.** Weak as a principle, strong as a caveat. One per unit, and the
unit small (2.2, 2.8).


---
**rama L453-454 · ABOVE · ABOVE C3**
*The group › 8. What this camp would question above the table*

**One store for the planet, with personal layers.** The reference says nothing about a worldwide cluster. RPL's model has been a cluster per customer. The task count is fixed at launch, so growth to planet scale passes through re-partition events that change every log position. A microbatch gate makes admission depend on every task group at once. And Kreps's point removes one hoped-for benefit: a partitioned log has no global order anyway. What one store really buys is one gate policy and one id space. The camp would ask whether that is worth one failure domain.


---
**rama L594-595 · R2 · CARRIED C3**
*Round two › T3. UUIDv7 against "no time inside the id"*

The hot entity under lean (10). **CHECKED** `skill/pstate-schema.md:54`: "a few keys take far more events or data than the rest; all of a hot key's writes land on its one task, making it a hotspot. Hash balances *keys*, not *load*." RPL's remedy (`:69`): "For **skewed** data, do NOT pick the cheapest-looking partitioner off this menu and settle. Derive the `f` the dominant read wants — e.g. *how many tasks should one key's data span as a function of its size?* — and implement it with `|direct`." Placement "can also be stored state, recorded at write time and read back before routing" (`:50`). Marz lived this (R, Mastodon post): "a naive implementation that handles all fanout for a single user from a single partition will lead to some partitions of a module having a lot more overall work to do than others."


---
**rama L596-597 · R2 · CARRIED C3**

The consequence. Once a hot entity's data spans several tasks, the entity no longer has one writer. Only a cell does. Sid's compare-and-set is on the cell (entity, key, layer). Lean (10) promises more than that: partition by entity.


---
**rama L598-599 · R2 · NEW-CASE C3**

The one case. Hundreds of people and their agents on one problem. The problem's root entity, or one shared canvas entity, takes machine-rate writes from thousands of actors. That is one task thread. The stream throttle trips, and then "Depot appends with AckLevel.ACK while the limit is hit will throw an exception back to clients" (**CHECKED** `docs/11-stream-topologies.md:297`), for every entity that hashes to that task, not only the hot one.


---
**rama L600-601 · R2 · CARRIED C3**

Sharpen (I). Promise order per cell. Deliver per-entity co-location as the default placement. Keep the right to spread a hot entity by cell. If tools come to rely on a whole entity committing as one, that right is gone. Note also that the depot partitioner runs on the appending client (**CHECKED** `skill/depot-design.md:132`), so the door has to know the placement table.


---
**rama L604-605 · R2 · CARRIED C3,E4**
*Round two › T4. The gate's three checks under a stream gate*

The version check is local by construction: the cell lives on the task where the event runs. Shape and policy need reads. The Rama-native options:


---
**rama L607-607 · R2 · CARRIED C3,E4**

2. **Hop and come back.** `select>` to the owning partition, then `|hash` back to the cell's task, then the compare-and-set. The version check and the write still share one event, so that part stays atomic. The grammar or policy can move in between. Two network hops per offer, and a retry redoes them. RPL's standing rule (N, `.claude/skills/rama/SKILL.md`): "Never trade I/O efficiency for code simplicity", and "Partitioner calls in topologies also add network latency."

---
**rama L608-608 · R2 · CARRIED C3,X3**

3. **Co-locate by choice of partition key**, so the policy sits where the offer lands. For a personal layer that means partitioning by layer. It conflicts with lean (10), and it turns "nearest layer wins" into a read across partitions.

---
**rama L609-609 · R2 · CARRIED C3**

4. **A microbatch gate.** Reads across partitions still hop, but the whole batch is one transaction. The costs are in round one.

---
**rama L613-614 · R2 · CARRIED C3**

Which does RPL's practice favour? Option 1 for anything small and rarely written. Co-location by partition key for anything large (N, `SKILL.md`, "Colocate related data").


---
**rama L625-625 · R2 · ABOVE C3**
*Round two › T5. One cluster worldwide*

5. One stalled task group fails appends for its partitions and halts every microbatch topology in the module (`:79`).

---
**rama L627-627 · R2 · ABOVE C3**

7. Labels can pin a module to labelled nodes (`docs/20-heterogenous-clusters.md`). That is per module, not per partition.

---
**rama L629-629 · R2 · CARRIED C3**

9. The task count is fixed at launch.

---
**rama L638-638 · R2 · ABOVE C3**

2. Can replica placement be made aware of zone or region per task group: leader here, followers there? Can leadership be pinned or preferred by label?

---
**rama L640-640 · R2 · ABOVE C3**

4. When one region is cut off, does every microbatch topology in the module stop, as the replication page implies?

---
**rama L643-643 · R2 · CARRIED C3,C2**

7. First-class task scaling: when? Will partition indexes and offsets be preserved, remapped, or translated?

---
**skeptics L279-279 · ABOVE · ABOVE C3,C5**
*6. What this camp would question above the table*

- **One store for the planet with personal layers.** The DynamoDB paper, REPORTED: operations decided adoption inside Amazon, against the better-fitting system. Stonebraker and Pavlo, REPORTED: distributed rarely beats one node. TAO, REPORTED: planet scale was reached by binding each object to a shard for life and giving up cross-shard atomicity. EDPB, REPORTED: design for erasure first. INFERRED: one planetary log holding everyone's personal values is the hardest case for erasure. Personal layers ease it only if a layer is a physical or cryptographic unit.

---
**sync L3152-3154 · ABOVE · ABOVE C3,C2**
*5. The group › 5.3 Questions this camp would call the wrong question*

3. **(10) "Can two gates ever write one layer?"** A layer is not the unit of
   order. Ask what the compare-and-set domain is and what the snapshot domain
   is.

---
**sync L3370-3374 · R2 · CARRIED C3**
*Round two: the leans, pressed › T1. "No optimism" against one worldwide gate*

**Who lives this way.** AT Protocol: one writer per personal repository, shared
views as indexes (N §2.13). Bayou: an accepting server near the user, a primary
far away (R §2.1). Differs: AT Protocol has no compare-and-set across
repositories; Bayou's near server checked no policy.


---
**sync L3534-3540 · R2 · DISAGREES C3,E4**
*Round two: the leans, pressed › T6. By whom plus the grant: what it cost Matrix*

One case returns with the hybrid placement of T1. A person's own gate admits a
fact under a grant that lives in a *shared* layer and was revoked there a
moment earlier. That is Matrix's fork in miniature. The rule that avoids it: a
gate checks only grants that live in layers it orders itself. Grants for a
personal layer live in that layer. Lean (8)'s "exists before the agent's first
write" fits. (I)


---
**sync L3597-3601 · R2 · CARRIED C3**
*Round two: the leans, pressed › A. The other leans*

**(10) Order.** Stands: Figma per file, AT Protocol per repository, Croquet per
session (R). Sharpen: an opaque token with a gate epoch (T1). What must share a
partition is decided by invariants across cells (Figma's cycle check, R §2.4),
and uniqueness invariants become registry cells, as under (2). (I)


---
## Says the same as the ledger (counted, not copied)

- C3 · frontiers · 1: L576-577
- C3 · meaning · 1: L3072-3078
- C3 · rama · 1: L683-684

