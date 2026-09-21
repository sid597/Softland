# X also held, and can wait: main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L604-606 · ABOVE · ABOVE X1,C7**
*7. What this camp would question above the table*

**What they would add that is not on the table.**

1. **The unit of saying.** The nine parts have no transaction. As the brief describes it, an offer is one fact. Then there is no way to land several facts together, and no single place for their shared provenance. Both product teams treat a set of facts at one point in time as basic. Halloway: "intermediate database states are inexpressible." If Sid's design already has this unit, the camp's main objection to the envelope goes away with it.

---
**datalog L825-830 · R2 · ABOVE X1,C7**
*Round two › R2.1 The tailored questions › T1. The unit of saying*

**The deciding case: one agent turns one model reply into 200 facts.**

Under the leans as written: 200 envelopes. 200 copies of by-whom, when, because-of and the based-on list. 200 content ids, 200 salts, 200 verdicts. And no way to land them together. If fact 117 fails its grammar, 199 facts of a reply stand in the store with a hole in them. That is a state nobody said. R: Halloway, "With this information model intermediate database states are inexpressible" [S-HN24]. In Sid's terms the map would show a reply that no model gave.

Under the saying: 200 small facts, one saying, one verdict, one landing.


---
**datalog L843-844 · R2 · NEW-REASON X1**

- **Not found:** any written regret that a transaction bundled unrelated causes. I looked in Waeselynck's event-sourcing essay [VAL-ES], the Shortcut post, and the Nubank talks. The mechanism that would produce it is documented (batching for throughput). The regret is not. Treat item 2 above as a risk I infer, not a lesson they report.


---
**datalog L976-977 · R2 · NEW-CASE X4,C3**
*Round two › R2.2 Ask A: the leans, one by one*

**(16) Visibility. One sharpening, on where private facts physically sit.** R: Datomic's filter is a view that trusted code applies at read time [D-FILTER]. Instant applies view rules as a filter after the query: "every object that satisfies a query will run through the `view` rule before being passed back" [I-PERMS]. R: Prokopov draws two filters on the push path, security and then interest [T-WEB]. **Case:** with partition by entity, a person's private fact about entity E sits in the same partition, and the same index, as the base's facts about E. Privacy then depends on every read path and every match path applying the filter, for ever, across hundreds of rebuilds of the runtime. A filter applied after the fact also leaks through counts and timing. If the layer is the unit of partition, private layers are physically separate, and a forgotten filter cannot leak across. N: Nubank's isolation is physical, a database per service. *The gate's answer in the fact's layer:* stands. R: XTDB keeps `xt.txs` in the same database [X-TXS].


---
**datalog L1019-1019 · R2 · NEW-REASON X2,C4**
*Round two › R2.4 Ask C: what they would change above the table*

5. **"Tools, grammars, policies as facts in the same store": yes, and the verdict must carry the versions.** T4. The seed grows in editions, and its first ids come from words.

---
**rama L449-450 · ABOVE · ABOVE W1**
*The group › 8. What this camp would question above the table*

**One append-only store of small facts as the one substance.** Marz agrees for the source of truth; it is his DataUnit. He disagrees that the same store should answer the questions: "There's a fundamental tension between being a source of truth versus being an indexed store that answers queries quickly." One substance for truth, many shapes for answers. Multiply's story is that tension lived. There is also a physical form of it in Rama: "Topologies read all data appended to a depot. So if a topology doesn't need certain data being appended, it must filter out that data at the beginning of processing" (`docs/14-depots.md:123`). One depot for everything means every tool reads every fact. Marz's own raw store was split by kind so that jobs read only what they need. The camp would say: match, don't route, is the right rule for meaning, and it still needs routing by kind at the storage level to be affordable.


---
**rama L606-606 · R2 · NEW-REASON X2,E1**
*Round two › T4. The gate's three checks under a stream gate*

1. **Broadcast small data to every task.** RPL's named pattern (N). "A topology uses `|all` to replicate data to every task. Every task gets a full copy — useful for lookup tables or configuration." (**CHECKED** `skill/patterns.md:163`). And: "The right `f` for small, rarely-written data read locally everywhere (e.g. config and lookup tables) — each task keeps its own copy, so reads are local with no cross-task hop. Not for large or frequently-written data — every task pays every write." (**CHECKED** `skill/pstate-schema.md:55`). The copies are stale by design. Tasks receive a new grammar at slightly different moments, and even under microbatch "external readers can observe two tasks on different microbatches at the same moment" (`skill/microbatch.md:128`). So "a stale local read whose versions the verdict records" is not a separate option. It is what this option is. The verdict must name the grammar and policy versions it checked. The brief already says the gate's yes or no names what it checked.

---
**rama L628-628 · R2 · NEW-REASON W2**
*Round two › T5. One cluster worldwide*

8. Clients must match the cluster's major and minor version (`skill/operate.md:14`).

---
**rama L677-678 · R2 · DISAGREES W1,X4**
*Round two › A. The leans, one by one*

**(16) Visibility. SHARPEN.** In Rama "private" is the door's promise, not the store's. Marz (R): "Rama PStates are globally readable". **CHECKED** `docs/03a-distributed-programming.md:61`: "A running event has access to all depot and PState partitions on that task." Any module on the cluster may mirror any other's (`docs/18-module-dependencies.md:7-9`). The one case: tool bodies. Tools are facts with a body, and a landed fact is matched to them. If a body written by anyone but the floor runs inside the cluster, as topology code or as a module, it can read every private layer on its task. So such bodies run outside the cluster, behind the door, or private layers are not private. The first-record consequence: layer has to be in the key path of every index, so the door can filter reads by layer cheaply.


---
**rama L720-721 · R2 · ABOVE X4**
*Round two › C. What the leans change above the table*

5. **Tool bodies and privacy.** "Tools are facts" needs a sentence about where bodies run. Inside the cluster there is no private layer (lean 16).


---
**skeptics L278-278 · ABOVE · NEW-REASON X2,E1**
*6. What this camp would question above the table*

- **Tools, grammars, policies and definitions as facts in the same store.** FlightTracker's lesson applies, REPORTED: systems end up "relying on data invariants that were not honored by all historical data". INFERRED: when rules are facts with versions and no fact is ever rewritten, every reader must know which rule held when each fact landed. The store can answer that, since the rule is a fact too, but only if each fact or its verdict names the rule version it passed.

---
**skeptics L280-280 · ABOVE · DISAGREES W2,C7**

- **A fixed nine-part envelope both ends share forever.** Hyrum's Law: every observable trait of those nine parts becomes a promise. Twitter: widening a field later is "painful". Hamilton: old formats stay until rollback is impossible, which here means forever. INFERRED: fix the widths and encodings generously, and randomize or hide whatever is not promised.

---
## Says the same as the ledger (counted, not copied)

- W1 · sync · 1: L275-276
- W3 · frontiers · 3: L231-232, L263-264, L317-317
- W4 · log · 1: L108-108
- W4 · sync · 3: L150-152, L457-458, L1300-1301
- X2 · log · 2: L88-88, L718-718
- X2 · sync · 1: L968-971

