# C4 key (3)(17): main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L39-40 · SHORT · DISAGREES C4**
*1. The short version*

**2. Key: word or id? Both, and the split is exact.** In storage the attribute slot holds an id: `[42 1007 1124]`. The name is a fact about that id: `:db/ident`. Renaming adds a name and the old name keeps working. "Never remove a name." The one thing that can never change is the value's type. Instant made the same choice independently with UUIDs. DataScript chose plain words and says so. [D-IDENT], [D-CHANGE], [D-BEST], [I-SQL], [T-README]


---
**datalog L623-624 · ABOVE · CARRIED C4**
*8. Questions this camp would call the wrong question*

- **(3) "Key: a word, or an id?"** The *right* question, and the one where the camp is most settled: both, split exactly as Datomic splits them.


---
**datalog L906-909 · R2 · NEW-CASE C4,X2**
*Round two › R2.1 The tailored questions › T4. Reading old facts through today's schema*

### T4. Reading old facts through today's schema

**Datomic's limit, exactly.** R: "a database value utilizes the single schema associated with its current basis. Thus traveling back in time does not take the working schema back in time" [D-CHANGE]. And the concrete way it misleads: "After changing the cardinality of an attribute… An entity from a d/as-of, d/since, or d/history database that has an attribute with multiple values will return a single one of those values if the schema attribute has been changed to be single-valued." [D-CHANGE]. The past is shown through today's rule, and it shows one affiliation where there were three.


---
**datalog L921-922 · R2 · NEW-CASE C4,X2**

**The deciding case.** Year 1: the key for affiliation takes many values. Year 3: grammar version 5 makes it single-valued. Someone asks what they were looking at in year 1. The honest answer shows three affiliations. It does so only if the reader applies the version recorded at admission, and if rule 5 stopped version 5 from being a break under the same key in the first place. Datomic today shows one. The camp's own rules would have made version 5 a new key.


---
**datalog L957-959 · R2 · NEW-CASE C4**
*Round two › R2.2 Ask A: the leans, one by one*

**(17) First facts. Sharpen twice.**

- *"A finite seed, written once."* R: Datomic's did not stay finite. "When the Datomic team enhances the base schema, all databases created with new versions of Datomic get the enhancements automatically. Existing databases do not automatically incorporate enhancements to the base schema." Tuples entered the base schema on 27 June 2019, seven years in. Older databases need an explicit step, which they made "an idempotent operation" [D-HOWTO]. **Case:** in year 3 the floor needs a new kind of system fact, such as an erasure request or a grant. The seed needs a second edition. Say *editions*: each one a saying by the floor, each safe to apply twice, none changing an earlier one. That is accretion, and the lean's content-computed ids suit it better than Datomic's small integers do.

---
**datalog L960-961 · R2 · NEW-REASON C4,C1**

- *"Seed ids computed from content."* **Case: the very first fact.** R: in Datomic it is `[10 10 :db/ident …]`. The attribute that names things is named by itself [AVILA]. A fact that mentions its own id cannot have an id computed from its own content. The hash would have to contain itself. So the first ids must come from something that does not loop: the *word*. The id of a seed key is a hash of its namespaced word. Every store derives the same id. It is also what Hickey says a name is for: "nice hopefully globally unique names" [H-SPEC]. (I.)


---
**datalog L962-963 · R2 · NEW-REASON C4**

**(3) Key is an id; word and shape are facts about it. Stands. Datomic and Instant live this way** [D-IDENT], [I-SQL]. Datalevin also keeps attribute ids, not words, in its indexes [Y-ALTER]. *Sharpen the ground.* The lean says the ground is "shape and cost, not loss". In a store that never rewrites, it is also loss. R: Datomic's "most requested enhancement" was altering schema, and renaming comes first in its list [D-BLOG13]. **Case:** in year 2 a word turns out to be wrong, or a second institution uses the same word for something else. With words in every fact of a never-rewritten log, the mistake is permanent and the clash cannot be fixed. With ids, a rename is one new fact, and two institutions' keys are two ids that happen to share a word. One cost to plan for: R, "All idents associated with a database are stored in memory in every Datomic transactor and peer" [D-IDENT]. Every reader needs the table of words at hand.


---
**datalog L1005-1005 · R2 · ABOVE C4**
*Round two › R2.3 Ask B: leans the camp would call the wrong question*

4. **(17) "A finite seed, written once."** Ask instead: *how does the seed grow without breaking any store that already has it?* Editions. R: [D-HOWTO].

---
**frontiers L563-564 · R2 · NEW-CASE C4**
*Round two (2026-09-20): the leans, pressed from the frontier › A. Leans this camp would reject or sharpen*

- **(3), stands with one rule.** Signatures must hold key ids and record the word lookup as a read. Deciding case: the word "relation" is re-pointed from one key to another. If standing patterns hold the word, every one of them changes meaning with nothing landing on any tool.


---
**log L544-545 · ABOVE · ABOVE C4**
*For the group › 8. What this camp would question above the table*

**Tools, grammars, policies and definitions as facts in the same store.** The camp would mostly agree. Tango keeps its directory as a Tango object. Helland points at schemas with "another arc in the DAG". They would insist that something always sits outside: Delos's MetaStore, CT's log list, the encoding rules. The brief says the one thing not made of facts is the runtime. This camp would add the trust anchor, the successor pointer, and the byte-level encoding, because all three are needed before any fact can be read.


---
**meaning L2621-2633 · ABOVE · DISAGREES C4**
*Part five — for the group › 8. What this camp would question above the table, ranked by *

**3. One store for the planet.** Strong, and of a different kind: not a flaw in
the logic, a bet about power. Lemmer-Webber: "god's-eye", no credible exit (2.1).
MyWebstrates left its server over "digital sovereignty" (3.3). Dynamicland: "not a
database serving millions of customers" (3.1). Xanadu's own engineers wanted "open
entry of server providers [...] to make centralized control impossible", and never
got past islands (3.5). Reed and Hewitt reject one order for everything (2.8,
2.11). In fairness, the camp also supplies the case *for*: a shared heap only works
with one heap; Croquet, Edwards, and Varda all choose one orderer per unit;
Wikidata is one store and works. What the camp would insist on is that "one store"
must not leak into the record. No id that only this gate can mint. No authorship
that only this gate can vouch for. No first facts that differ from store to store.
Then the second store, and leaving, stay possible.


---
**meaning L2677-2678 · ABOVE · ABOVE C4**
*Part five — for the group › 9. Questions this camp would call the wrong question*

- **(3) "A word, or an id?"** Three kinds of name, all needed. The real question
  is which one is written into the fact, and where the others live.

---
**meaning L3107-3113 · R2 · DISAGREES C4,C1,C6**
*Round two › A. Leans my camp would reject or sharpen*

**Stand, and who lives this way.** (3): Varda, Wikidata, Datomic, Edwards; add
numbered tags for fields inside values, and a key's kind of value never changes.
(1): nanopublications, Unison. (6): PROV, Git; keep it a number, and say why.
(16): Worlds; add that a read of the past is judged by today's grants (§3.3).
(12): Croquet, Urbit; necessary, not sufficient (T4). (10): Durable Objects,
Croquet.


---
**rama L451-452 · ABOVE · ABOVE C4,X2,C3**
*The group › 8. What this camp would question above the table*

**Tools, grammars, policies and definitions as facts in the same store.** This camp keeps schemas and functions in code, one version live at a time: "You'll never have to deal with situations where there are multiple versions of a schema active at the same time." They would ask three things. If a grammar is a fact and old facts are never rewritten, must every reader understand every grammar version forever? Where do grammar and policy facts sit relative to the gate's partition, given that a hop ends the transaction? And when a policy fact is wrong, which is a human fault, what is the recovery path? In their world it is: fix the code, recompute. In Sid's it is: add facts that correct, and let doubt spread along based-on. That is coherent. They would want to see it run.


---
**rama L611-612 · R2 · DISAGREES C4,X2**
*Round two › T4. The gate's three checks under a stream gate*

6. **Keep grammars in module code.** The camp's own default (Marz R: one schema version live at a time). Not facts. It contradicts Sid's design. It is listed because it is what these people actually do.


---
**rama L661-662 · R2 · NEW-REASON C4**
*Round two › A. The leans, one by one*

**(3) Key is an id. STANDS.** Thrift field numbers (R, 2010). RPL's explicit type ids (N). The cost: the gate resolves key id to grammar on every offer. T4, option 1.


---
**skeptics L424-424 · R2 · NEW-REASON C4**
*Round two (2026-09-20): the leans, pressed from the skeptics › A. Leans this camp would reject or sharpen*

- **(3): sharpened.** The 2005 paper (R): locally coined keys drift in meaning "record-by-record". With keys as ids, two labs can each coin "supports" with different shapes and nothing collides. With words, the gate would refuse the second, and that collision is information. Let the gate check that a word is unique within a scope (I).

---
**sync L3494-3508 · R2 · NEW-REASON C4**
*Round two: the leans, pressed › T5. Names or ids for keys, from AT Protocol's side*

The lived regrets with names as keys (§2.13): fields "can not be renamed" (N);
a breaking change needs a new name, and the convention became a "V2" suffix,
which is a version number smuggled into a name (N); a record has no way to pin
a schema version (Newbold, R); authority rides on a DNS domain, so losing the
domain loses the schema (I); the name means what the dominant software means,
and control of names is a lever of authority (Frazee, R). Every one of these
argues *for* an id with the word as a fact about it. What names buy AT Protocol
are two things that one gate does not need: minting by many parties with no
coordinator, and records a stranger can interpret by looking the name up. The
second returns at export and with a second store: a fact whose key is an opaque
id means nothing to a stranger unless the key's defining facts travel with it.
So this does not reopen the closed line. It adds a rule: an export carries the
definitions of every key it uses, and owner-scoped words, held as facts, are
how two stores line their keys up (p2panda's name plus definition id, R §2.15).


---
**sync L3571-3581 · R2 · DISAGREES C4**
*Round two: the leans, pressed › A. The other leans*

**(17) First facts.** Reject "computed from content". Kleppmann's objection to
exactly this in Automerge: "if the developer ever changes the initialisation
code, it will produce a different change with a different hash… it would be a
very fragile API that is easy to use incorrectly" (R §2.8). did:plc's legacy
ids are "around forever" (R §2.13). The goal, the same seed ids in every store,
is met by published constants: jj, Automerge's root, Pijul, PPPPP's moot (R).
Deciding case: year three, a typo in the seed grammar is fixed, or the
canonical encoding gets a second version. New stores now derive different seed
ids from old stores. "Base" and "the gate" differ between them. Federation is
broken at the root.


---
**sync L3650-3651 · R2 · ABOVE C4**
*Round two: the leans, pressed › B. Wrong questions*

4. **(17).** "How do all stores compute the same seed ids?" Ask how they *have*
   the same ones. Constants.

---
## Says the same as the ledger (counted, not copied)

- C4 · datalog · 2: L205-206, L799-799
- C4 · log · 5: L88-88, L111-111, L220-220, L386-387, L718-718
- C4 · meaning · 4: L510-511, L1145-1153, L1727-1730, L3053-3061
- C4 · rama · 4: L87-87, L90-91, L313-314, L582-583
- C4 · sync · 2: L975-975, L3185-3186

