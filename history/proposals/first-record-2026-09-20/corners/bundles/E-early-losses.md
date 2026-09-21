# E early losses: main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L590-591 · ABOVE · ABOVE E7,P0**
*7. What this camp would question above the table*

**"An append-only store of small facts as the one substance for everything."** They agree on facts and on small. "A Datom is a minimal and sufficient representation of a fact." They reject "everything". Hickey: "Datomic is not about keeping stuff." Nubank: not firehose writes, not long strings. Yang: application state is not information. Their test for whether something belongs is whether someone will later need to reason from it. A cursor position fails. A selection that led to an act passes, as part of that act.


---
**datalog L600-601 · ABOVE · ABOVE E6**

**"Every read recorded as provenance on every fact."** They would cut this twice. Not every read: the basis and the question, because the rows are re-derivable from those. Not on every fact: on the saying. And they would hold on to the principle underneath, that perception is free. "Queries and reads not part of a transformation never create a transaction." A read is written down only when it becomes part of an act. Sid's design already has this shape, since reads appear only on facts and at crossings. The risk is volume: if every render of every screen is a crossing, the crossings become Nubank's firehose.


---
**datalog L618-618 · ABOVE · ABOVE E6**
*8. Questions this camp would call the wrong question*

- **(11) "Is every read listed?"** The question assumes a list. Theirs is: what was the basis, and what was asked? With one order per unit the list can be rebuilt from those two.

---
**datalog L790-790 · R2 · DISAGREES E8**
*Round two › R2.1 The tailored questions › T1. The unit of saying*

| part of the saying | who writes it | note |
| session | the runtime | a pointer to the session entity: runtime commit, kind of machine |

---
**datalog L791-791 · R2 · DISAGREES E6**

| part of the saying | who writes it | note |
| based on | the runtime and the tool | the basis, the questions asked, outside anchors |

---
**datalog L820-820 · R2 · NEW-REASON E1,X1**

- **(7)** One verdict per saying. Landing is all or nothing: R "If one part of a transaction fails, the entire transaction fails, and the database is left unchanged" [D-ACID]. A refusal names the cell or the check that failed.

---
**datalog L822-822 · R2 · DISAGREES E8**

- **(12)** One pointer to the session.

---
**datalog L831-833 · R2 · DISAGREES E6,E5**

**What is lost, honestly.**

1. **Per-fact based-on.** The reply summarised 30 papers. Paper 17 is later retracted. Per-fact lists would cast doubt on about 7 facts. One list per saying casts doubt on all 200. Over-doubt is also a way for the map to lie. But look at who fills the list. Lean (11) says the runtime fills in entries. The runtime knows what the *invocation* read. It cannot know which read fed which output. So in practice the 200 per-fact lists are 200 copies of one list, with the same over-doubt and 200 times the bytes. Only a tool that tracks its own dependencies can do better. With sayings it does better by splitting: 40 claims become 40 sayings, each with its own reads, all because of the same crossing. The saying gives the tool a dial between one saying of 200 and 200 sayings of one. The per-fact envelope has no dial. It is fixed at 200 of one. (I, from the leans' own wording.)

---
**datalog L923-924 · R2 · NEW-REASON E1,C5**
*Round two › R2.1 The tailored questions › T4. Reading old facts through today's schema*

**With lean (9).** After a key is destroyed, the verdict's line about the grammar version is the only surviving statement about that value's shape. That is one more reason the line belongs on the verdict, which is kept for ever.


---
**datalog L968-969 · R2 · NEW-CASE E3,C2**
*Round two › R2.2 Ask A: the leans, one by one*

**(11) When. Stands, and Datomic lives this way.** R: "you should always use the t (or related tx) value" for order [D-BEST]. *Sharpen:* keep the stamp monotonic with position inside each unit of order. R: an explicit instant must not be "older than any existing transaction" [D-TXDATA]. **Case:** *what was I looking at yesterday at 17:00* has to map a wall-clock time onto a position. If a gate's clock steps back after a time sync, two positions carry stamps in the wrong order and the mapping has no answer. The gate must never stamp earlier than its own last stamp in the same unit.


---
**datalog L970-971 · R2 · DISAGREES E6**

**(11) Based on: every read listed. Sharpen to the basis and the question, on the saying.** **Case:** the agent reads a pattern over 3,000 base facts, then one model reply, and writes 200 facts. If "listed" means the rows, that is 600,000 entries for one act. If it means the pattern read as one entry, it is still the same list 200 times over. One saying holds one entry. R: "Every query we issue to that value of the database has the same basis" [H-DD]. One caution the other way: a bare basis is enough only while T3's single-number "as of" holds. Under lean (10)'s cuts, the basis is itself the expensive part.


---
**datalog L980-981 · R2 · DISAGREES E5**

**(4) Marks on reads.** Two halves. *Depends-on or how-she-got-here:* keep it, as two named attributes (2.4). *Follow-latest or stay-on-version, fixed when written:* wrong question, see R2.3. One structural point. "The floor guesses; the tool may correct." In a store that never rewrites, a correction to provenance cannot edit the fact. It needs somewhere to go. If the saying is an entity, the correction is a new fact *about the saying*, with its own by-whom and when. R: "Transactions are ordinary entities: you can create attributes that are about transactions" [D-BEST], and the docs' `:correction/for` [D-MODEL]. With provenance inside each fact's envelope, the only way to correct a guess is to write a new version of a fact whose value did not change.


---
**datalog L988-989 · R2 · DISAGREES E1**

**(7) The verdict kept for ever; refusals in the offerer's session layer.** Stands, and XTDB v2 lives the first half [X-TXS]. Two sharpenings. One verdict per saying. And a refusal filed away from the cell it lost on must carry *the position it was judged at and the version it found there*. **Case:** hundreds of agents contend for one hot cell in a shared problem layer. Most offers lose. An agent asks what it lost to. In XTDB the refusal sits in the same order as the winner, so the answer is its position. In the offerer's session layer it has no place in the target's order unless one was written on it. The lean's choice of layer is right for volume. Refusals at machine rate would bury a shared layer. The cross-reference is what it costs.


---
**datalog L990-991 · R2 · DISAGREES E8**

**(12) Session start. Stands, and Nubank lives this way.** R: the git version of the service on every transaction [N-QCON]. Sharpen with Hickey: the commit is the name, and a build that is not a clean commit says "unreproducible" in its name [H-IONS].


---
**datalog L992-993 · R2 · DISAGREES E7**

**(14) The hand. The camp rejects pan and zoom at a tick as facts kept for ever.** R: "Datomic is not about keeping stuff" [H-DD]. No history for "high churn attributes" [D-BEST]. Nubank: "It doesn't work that well for fire hose writes" [N-QCON]. DataScript, built for long-lived browser applications, keeps no history so that its databases "operate in constant space" [T-README]. Yang: application state is not information [Y-HN]. **Case:** one person, eight hours, ten ticks a second, is 288,000 facts a day. Under lean (9) that is also 288,000 keys a day in the key store, never trimmed. They are the most revealing facts in the store about how that person works. And they are already recorded, because every "what was shown" crossing carries the viewport it was shown through. *Sharpen:* point and select become part of the saying they led to. Pan and zoom live inside the shown-crossings. "A person may turn capture down by their own fact": stands, with the note that a store which never rewrites cannot honour it backwards in time.


---
**datalog L994-995 · R2 · NEW-CASE E4**

**(15) A click.** The camp is nearly silent. One sourced caution about defaults. R: Instant chose open by default, and then, once agents arrived, built undo exactly where they were hurt: "Even if a rogue agent deletes your columns, you can undo it" [I-ARCH]. **Case:** a colleague's toolkit is copied into a person's own layer. Under the lean it is on by default, and it acts in their name at the next click. *Sharpen:* on by default only for tools that are in their layer *and* were said by them or under their grant. The by-whom on the saying that landed the tool decides it.


---
**datalog L1002-1002 · R2 · ABOVE E5**
*Round two › R2.3 Ask B: leans the camp would call the wrong question*

1. **(4) "Follow the latest, or stay on the version read", marked when the fact is written.** Whether a later change to a read matters is not a property of the read. It is a property of the question someone asks later, and in Sid's design the definition of stale is itself a fact that will improve. **Case:** summary S stood on claim C at version 3. Version 4 fixes a typo. Version 5 reverses the claim. If S was marked "stay", it is never stale, which is wrong at version 5. If it was marked "follow", it is stale at version 4, which is noise. No mark chosen at writing gets both right. A year-5 definition of stale can, provided the year-1 read was recorded plainly as *what, as of where*. R: identity and state are separate, and the state comes from the value you read through [H-AWTY]. Ask instead: *what did it read, as of where?* Leave the verdict to the definition.

---
**datalog L1006-1006 · R2 · DISAGREES E7**

5. **(14) "Which motions become facts at a tick?"** Ask instead: *which motions became the basis of an act, and which are already inside a shown-crossing?*

---
**datalog L1017-1017 · R2 · CARRIED E1,A1**
*Round two › R2.4 Ask C: what they would change above the table*

3. **"The runtime is the one thing that is not a fact": keep it, and draw the line where XTDB ended up.** R: for seven years XTDB made every node re-derive outcomes from the log. "Every transaction has to be deterministic… any kind of error had to restart all of the nodes", and every feature "had to be expressible as a pure, deterministic function of the log" [X-LEAD], [X-DBS]. Sid's runtime will be rebuilt hundreds of times. So *re-derivable* has to mean re-derivable **now**, by today's runtime. It must never mean that build 400 would reproduce what build 12 computed. Everything that must stay as it was has to be an outcome that was written down: the gate's verdict, the landed facts, the crossings. The leans already do this. The change is to say so as a rule, and to apply it to running answers too. A running answer shown in year 1 is only known through its crossing. Recomputing it under today's runtime is a new answer, not a replay of the old one.

---
**datalog L1020-1020 · R2 · DISAGREES E6**

6. **"Every read recorded on every fact": the basis and the question, once, on the saying.** Reading stays free and unrecorded except at acts and at crossings. R: [H-HN12].

---
**datalog L1021-1021 · R2 · ABOVE E7,P0**

7. **"An append-only store of small facts as the one substance": yes for information, no for the firehose.** See (14). The line is one Hickey and Nubank both draw.

---
**frontiers L418-418 · ABOVE · ABOVE E1,X2**
*9. What this camp would question above the table*

- **Tools, grammars, policies and definitions as facts in the same store.** Two cautions. Hellerstein and co-authors: a revolutionary programming model had "limited" adoption, and they now lift what they can and wrap the rest as opaque functions (REPORTED). And: once policies and grammars are facts, the gate's own decision is a non-monotone read of them. The verdict should carry the as-of of the grammar and policy it used, or verdicts cannot be explained later (INFERRED).

---
**frontiers L422-422 · ABOVE · DISAGREES E6**

- **Every read recorded as provenance on every fact.** The sharpest disagreement. McSherry: complete provenance is "Accurate, but not yet helpful"; compute explanations on demand. Brandon: at fine grain "the overhead of the graph metadata dominates the actual computation". Alvaro: fine-grained lineage was the first thing given up in production. Their split would be this. Deterministic results need no stored reads at all, only times, logic version and runtime version. Non-deterministic results (a person's act, a model's reply) need a read set, at pattern grain, pinned, with empty reads included (INFERRED synthesis).

---
**frontiers L433-433 · ABOVE · ABOVE E5**
*10. Questions this camp would call the wrong question*

- **"Follow the latest, or stay on the version read?"** Not a property of a read entry. Records pin. Views follow. Staleness is the difference between the two.

---
**frontiers L434-434 · ABOVE · DISAGREES E3**

- **"Whose clock?"** Secondary. First: which single field is the order, and who is its only author? After that the clock question mostly answers itself.

---
**frontiers L435-435 · ABOVE · ABOVE E6**

- **"Match, don't route", asked only about landings.** A standing pattern is a query over all facts as of now, not only over facts that land after it was registered. The question the design skips: what does a newly registered tool owe to history?

---
**frontiers L524-525 · R2 · DISAGREES E3**
*Round two (2026-09-20): the leans, pressed from the frontier › T2. The reclocking map as a first-record item*

**Road B, Aurora DSQL's way** (R, https://brooker.co.za/blog/2024/12/05/inside-dsql-writes.html). No map at all. Each gate stamps `when` so that it never goes backward within its own partition, and keeps announcing how far it has got even when idle. "As of T" then means, in each partition, everything stamped at or before T, and T is ready once every gate has announced past it. The map is implicit in the facts' own stamps. One more rule keeps causes before effects across different clocks: the gate stamps a fact later than every stamp named in its based-on. Dedalus shows the same repair (R): "implement Lamport clocks [15] atop Dedalus, which allows programs to ensure temporal monotonicity". With that rule a read of one fact at its version needs no waiting at all.


---
**frontiers L526-527 · R2 · DISAGREES E3**

What is recorded on road B: nothing new. Two promises on `when` (never backward within a partition; later than what the fact stood on), and the status on each crossing. That is the smallest possible first-record item. Lean (11), "never used for order", is the one lean that closes it. Facts stamped without those promises can only ever be reached by road A.


---
**frontiers L532-533 · R2 · ABOVE E5,C6**
*Round two (2026-09-20): the leans, pressed from the frontier › T3. Lean (4)'s two marks*

**Follow-latest or stay-on-version.** I agree with the Datomic camp. It is the wrong question, and round one said so ("Records pin. Views follow."). The mark is fixed at writing. What decides is a fact that does not exist yet: the one that later supersedes. Their case, at Sid's scale: a model summary stands on 200 paper facts in the ten-million-paper field. Under stay, every metadata fix marks it stale, and at that volume everything is always stale. That map cries wolf. Under follow, a retraction flows through in silence. That map hides doubt. The staleness definition can get both right, if the superseding fact says what kind of change it is. That is one more reason for lean (6): "replacing 25" is where the kind of change belongs.


---
**frontiers L534-535 · R2 · DISAGREES E5**

**Depends-on or how-she-got-here.** This one can be known at writing, but the lean's form is wrong twice (I). First, it is a counterfactual: would the fact differ without this read? For a deterministic tool that is a computation, McSherry's minimal explanation (R), not a guess. For a model reply nobody can know it, the tool included. What can be known at writing is the read's role: trigger, given to the model, matched, navigated. Record the role and let the doubt definition weigh roles. Second, a yes-or-no mark cannot say that either of two reads would have sufficed. LDFI (R) reasons about "the redundancy of support". Without support groups, doubt spreads too far.


---
**frontiers L536-537 · R2 · DISAGREES E5**

So, against the lean: reads at pattern grain, pinned, empty reads included (McSherry, R: "or failed to read, if absent"), each with a role, optionally grouped into alternative supports. No follow-or-stay mark.


---
**frontiers L540-541 · R2 · ABOVE E6,C2**
*Round two (2026-09-20): the leans, pressed from the frontier › T4. Every read, against its cost (an exchange)*

**The skeptics' best case.** Brandon (R): at fine grain "the overhead of the graph metadata dominates the actual computation". McSherry (R): complete provenance is "Accurate, but not yet helpful". Alvaro at Netflix (R): fine lineage was the first thing given up, and call graphs "sacrifice some precision". Big tech's revealed choice (R, other report): a few typed reads that the system acts on, and no more. Numbers (I; my arithmetic on the brief's scale, not anyone's measurement). A fact is perhaps 300 bytes. A tool fired by a pattern that matched 2,000 facts, listing each as id plus version, carries about 48 KB of reads, 160 times the fact. Twenty agents for one person at ten facts a second each comes near 10 MB a second for that person, never trimmed. Under lean (10) each pattern read's as-of is a vector: ten reads over 256 partitions adds 20 KB per fact. Last, "no grace period" meets opaque bodies. A model call or a host call can read around the floor. A list that claims to be complete and is not is worse than no list. It is the map lying about itself.


---
**frontiers L542-543 · R2 · DISAGREES E6,E5**

**The lineage camp's rejoinder.** The asymmetry is Sid's own: an unrecorded read is gone for every earlier fact, and a recorded one costs bytes. For anything not re-derivable the read set can never be recomputed. McSherry's own commit sketch (R) sums a transaction up as its read set and write set. The CALM manifest (R) makes based-on a safety tool: a reader can wait until it holds what a fact stood on. That cannot be retrofitted. Signatures are rules, and with rules fine lineage is "trivial to collect" (Alvaro, R). S3 (R, other report): where the store stays silent on staleness, every serious user builds a side system. And the dominance argument is about one edge per value. It does not touch reads at pattern grain.


---
**frontiers L544-545 · R2 · DISAGREES E6**

**The cheapest shape that still supports the five walks (I).**
- One read-set fact per firing, not per offer. Each offer from that firing points at it once.

---
**frontiers L546-546 · R2 · NEW-REASON E6,C2**

- Each entry holds: a pattern (or one fact id at its version), the as-of point (eight bytes, by road A or B), a role, and a digest, which is the count and a hash over the ids and versions that matched. An empty read is an entry with count zero.

---
**frontiers L547-547 · R2 · NEW-REASON E6**

- One honesty slot on the read set: complete, or partial (the body could read around the floor). This is not a grace period. It is the difference between "everything" and "everything the floor saw".

---
**frontiers L548-549 · R2 · DISAGREES E6,E2**

- A crossing points at a read set the same way, and adds what was rendered and its status.


---
**frontiers L550-551 · R2 · NEW-REASON E6**

The digest is what makes pattern grain enough. Re-running the pattern at its as-of must reproduce the digest. If it does, the full list is recovered exactly, for about 64 bytes instead of 48 KB. If it does not (an index bug, an erased value, a changed policy), the floor knows the recovered list is inexact and can say so.


---
**frontiers L568-568 · R2 · ABOVE E5,C6**
*Round two (2026-09-20): the leans, pressed from the frontier › B. Wrong questions*

- Follow or stay: ask what the superseding fact says about why it supersedes.

---
**frontiers L573-573 · R2 · ABOVE E6**
*Round two (2026-09-20): the leans, pressed from the frontier › C. Above the table: what the leans change*

- Every read recorded: with read sets per firing, pattern grain and digests, this camp's objection shrinks to one demand, the honesty slot.

---
**log L552-553 · ABOVE · ABOVE E6**
*For the group › 8. What this camp would question above the table*

**Every read recorded as provenance on every fact.** Nobody here does it, and two of them came close enough to show it is feasible. Tango and Hyder put read sets into the log for every transaction, but for validation, and they let them go when the log is trimmed. Hyder counts unrecorded reads as its source of read scale ("they are not logged or melded"). The honest statement is that Sid would be first to keep read sets forever at machine rate, and the known tricks for size are: patterns, not lists; direct reads, not the closure; one frontier; shared read records.


---
**log L562-562 · ABOVE · ABOVE E1**
*For the group › 9. Questions this camp would call the wrong question*

5. **"Is the gate's yes or no kept beside each fact?" (7).** The prior question is whether offers are logged before they are judged. Once that is settled, where verdicts live and whether refusals survive follow from it.

---
**log L563-564 · ABOVE · ABOVE E3**

6. **"Whose clock?" (11).** A fair question with a quick answer (the gate's, and never for order). The camp would add the question behind it: which time? The moment a statement is about, or the moment it was admitted.


---
**log L699-700 · R2 · NEW-CASE E1**
*Round two: the leans, pressed › T4. Lean (7) against "refusals in a separate, trimmable stre*

**Refusals forever is the part I would press.** The deciding case: thirty agents of one person keep one "current summary" cell fresh. Each landed fact makes about twenty-nine refusals. Hyder measured the mechanism: the longer the wait, "the greater the chance that each transaction aborts" (R). Under leans (7), (9) and (13), each refusal is a full offer, an encrypted value, a key in the key store, and a verdict, forever. Session layers become the largest data in the store. CT keeps no refusals at all: "Rejected logging submissions must not be issued an SCT by the CT log." (R), because "logs are useful only if their size is manageable" (R, Laurie).


---
**log L701-702 · R2 · NEW-REASON E1,C5**

**The trim rule** (I). Keep forever the refusal's envelope and verdict: offer id, actor, cell, expected version, reason, what was checked. Destroy the refused value's key after the retry window unless someone pins it. Helland bounds that window with his bank, where a check clears in "less than one year" (R). Under lean (9) this trim rewrites nothing, so it does not touch lean (0). What is lost: replaying a new gate against old refused offers beyond the window, and the content of what an agent tried to write. What stays: counts, reasons, and a commitment by which a holder can prove what they offered. Also decide when a session layer closes. Young's accountants close the year: "Nine were marked by their year and were read-only" (R). A closed session can move to cold storage, as Delos does with old segments (R), without being trimmed.


---
**log L719-719 · R2 · NEW-CASE E6,E3**
*Round two: the leans, pressed › A. The other leans*

- **(11) Sharpen "every read listed" to "every read accounted for".** Deciding case: a summary tool scans 200,000 facts to write one summary. Listed, that is megabytes of pointers on one fact. As a pattern plus a cut it is one line. Hyder's reads scale because "they are not logged or melded" (R). Kleppmann lists direct parents only, "which ensures that this set remains small" (R). When, because-of, and no grace period: stand. Make when monotone within a partition (CT's rule, R), and note that Rama already stamps an append time (R).

---
**log L724-724 · R2 · NEW-REASON E7**

- **(14) Press.** The local-first team: "CRDTs store all history, including character-by-character text edits. These pile up" (R), and they could not trim. Deciding case: pan and zoom ticks, hundreds of people, years, each tick a fact plus a verdict, never trimmed. I: put the settled viewport on the crossing fact, which needs it anyway, and record motion only at the grain of intent.

---
**log L731-731 · R2 · ABOVE E1**
*Round two: the leans, pressed › B. Wrong questions, against the leans*

3. Lean (7) asks where refusals live. Ask: when does a session layer close?

---
**log L733-734 · R2 · ABOVE E6**

5. Lean (11) asks whether every read is listed. Ask whether every read is accounted for.


---
**log L738-738 · R2 · NEW-REASON E1,A3**
*Round two: the leans, pressed › C. Above the table: only what the leans change*

- **One writer, doubled.** With (7), (11) and (14), every landed fact is two appends, every refusal two, every tick two. Hyder's sequential judge was its ceiling: "meld has been the bottleneck that limits transaction throughput" (R). I: fact and verdict should be one atomic append to one partition.

---
**log L745-746 · R2 · NEW-CASE E1,E7**
*Round two: the leans, pressed › The three I would press hardest*

3. Lean (7) with (13) and (14): refusals and ticks kept forever, at machine rate, are the largest data in the store. Case: thirty agents on one cell.


---
**meaning L2602-2610 · ABOVE · ABOVE E4**
*Part five — for the group › 8. What this camp would question above the table, ranked by *

### 8. What this camp would question above the table, ranked by force

**1. Policy keyed on who, in a store where tools are matched and not called.**
Strongest. See 2.1. Hardy's rule set grew to "fourteen boolean operators" and
still leaked. Linda's authors conceded security was never confronted. Folk leaves
the hole open on purpose because the room is the trust boundary. No system of this
kind has run among strangers. The camp's demand is small and early: an offer
selects the authority it uses; grants are facts; the record shows which grant.


---
**meaning L2634-2640 · ABOVE · DISAGREES E6**

**4. Every read recorded as provenance.** Medium to strong. Xanadu made tracking
readers "impossible" on principle (3.5). PROV declined to require it (2.6).
Wikidata's machine-filled slot became noise (2.5). Webstrates drowned in cursor
operations (3.3). For it: Moreau's "very tedious"; Folk's and Worlds' read
tracking. The challenge is about who owns the record of a read, and about signal,
not about whether it can be done.


---
**meaning L2669-2672 · ABOVE · ABOVE E4,C8**
*Part five — for the group › 9. Questions this camp would call the wrong question*

### 9. Questions this camp would call the wrong question

- **(15) "Which tools may act in a person's name?"** None. Ask what the person
  handed to this tool, and for what.

---
**meaning L2675-2676 · ABOVE · ABOVE E7**

- **(14) "Is being shown the same as looking?"** The system can only ever know
  "shown".

---
**meaning L2823-2830 · R2 · NEW-REASON E4,C8**
*Round two › T1. Closing ambient authority: the smallest first-record con*

Grants name their grantee. They are not bearer tokens, because ids get copied
into every based-on list (I; R for the failure: MyWebstrates made the id the
capability and "there is no mechanism for revoking access", §3.3). So identity
still matters exactly where lean (8) puts it: the door proves who is speaking. It
stops being what the gate looks up. This is Horton's shape: identity for blame,
capability for authority (R,
https://www.usenix.org/legacy/event/hotsec07/tech/full_papers/miller/miller.pdf).


---
**meaning L2831-2835 · R2 · NEW-REASON E1**

**What the verdict records.** The saying's id. Yes or no. The grant id and
version relied on. The grammar id and version. The cut: the position of every
partition read in order to decide, above all the one holding the grant. On a no:
what was expected and what arrived (R: Armstrong, §2.9).


---
**meaning L2845-2847 · R2 · NEW-REASON E7,E4**

The selection is not a separate fact. It is G's "covers". (N for the pattern:
Sandstorm asks "Which calendar should the app use?", never "Is it OK?", §2.2.)


---
**meaning L2862-2868 · R2 · NEW-REASON E4**

Under the convention it fails safely. T's offer must cite a grant that covers the
target. T's standing grant from A covers L, not A's personal layer. B's note
cites no grant over the target, because B holds none. The general rule (I): **a
name inside a value is only a name. A tool may act on it only under a grant the
triggering saying itself cited, or one the enabling person gave for that very
scope.**


---
**meaning L2869-2878 · R2 · NEW-REASON E4,C8**

Two sharpenings (I). A grant to a tool pins the tool's version. A grant to "T,
latest" hands A's authority to whoever can supersede T; this is where lean (4)'s
follow-latest flag turns into a security setting. And agents must be able to
write narrower sub-grants by ordinary offers. Otherwise tens of agents spawning
sub-agents at machine rate will share one actor id, and the record loses the
instrument, which is what made Wikidata's bot policy "ineffective" (R: Pintscher,
§2.5). Revoking a parent kills the subtree (R: Spritely's forwarder "will only
operate if Lauren decides not to flip the revoked? cell",
https://files.spritely.institute/papers/spritely-core.html).


---
**meaning L3023-3031 · R2 · NEW-CASE E4**
*Round two › A. Leans my camp would reject or sharpen*

### A. Leans my camp would reject or sharpen

**(15), with (8): reject the frame.** T1 has the case. A second one, for "tools
in their own layers are on by default": one of a person's tens of agents is
steered by a poisoned paper and writes a tool into that person's layer. It is on
by default, and it outlives the agent's grant. Sharpen (I): a tool with no grant
may give running answers, which need no authority. Only offers need a grant. "On
by default" is safe for the first kind and unsafe for the second.


---
**meaning L3032-3052 · R2 · DISAGREES E7**

**(14): reject the default.** Who: Webstrates made everything durable and walked
it back: "moving the cursor around for a second can easily generate 30-50
operations", then a transient element, a protected mode, and throttling that is
"almost guranteed to eventually cause inconsistencies" (N,
https://webstrates.github.io/userguide/api/throttle-and-compose.html).
Dynamicland: objects "do not remember anything", and "Objects can't see people"
(N, https://dynamicland.org/2024/FAQ/). Xanadu: it must be "impossible to [...]
track individual readers" (N, §3.5). Case: hundreds of people, a canvas open all
day, a few ticks a second. That is on the order of a hundred thousand motion
facts per person per day, each with an envelope and a verdict kept for ever, and
no trimming (13). The exhaust outweighs the knowledge, and it is a
minute-by-minute record of each person's attention, on by default. Sharpen (I):
acts that change shared state or grant something are always facts. "Shown" is a
fact in the viewer's own layer, written **when what is shown changes, not when
time passes**; a store that refuses clocks for order should not use one here.
What a model was given is always a fact. Raw motion is a statement while it lasts
(R: Folk drops intermediate states on purpose, §3.1), unless the person's own
fact turns capture *on*. Lemmer-Webber's test for consent fits: "intentional,
granted, contextual, accountable, and revocable" (R,
https://dustycloud.org/blog/re-re-bluesky-decentralization/).


---
**meaning L3079-3088 · R2 · NEW-CASE E1,P0**

**(7) and (13): "kept forever" and "never trimmed".** Case: a looping agent earns
a million refusals overnight. Sharpen (I): a refused offer never became a fact, so
trimming its body rewrites nothing. Keep the verdict; put the body under a quota
(N: the nanopublication registry added quotas, §2.6). Never-trimmed makes "what
becomes a fact" the only control on size, so (14) and (7) decide (13). Reed gave
up on keeping every version (R, §2.8). Pintscher says Wikidata's history tables
are "near the limits of their scalability" (R, §2.5). Xanadu and Hewitt both make
keeping a thing somebody's cost (N §3.5; R §2.11). With an economy in scope, each
layer needs a payer.


---
**meaning L3094-3097 · R2 · NEW-REASON E5,E4**

**(4): sharpen.** A fact's reads always pin (Part four, (4)). Keep
follow-or-stay for standing things: tool signatures, definitions, grants. In a
grant the default must be stay (T1).


---
**meaning L3098-3106 · R2 · DISAGREES E3**

**(11): I take back part of my round one.** I asked for a writer's clock. The cut
in based-on already says what the writer had seen, which is the only "time" that
matters for staleness. That is Reed's view: "Pseudotime can be thought of as a
naming mechanism for successive states of all objects in the system" (R,
https://www.cs.sfu.ca/~vaughan/teaching/431/papers/reed83.pdf). When an old thing
happened (a 2019 notebook entry ingested in 2026) is content. Give it one key in
the seed, so every lane uses the same one. The lean stands. Item 7 in my
round-one list should read that way.


---
**meaning L3114-3117 · R2 · ABOVE E4,C8**
*Round two › B. Wrong questions*

### B. Wrong questions

- (15) "Which tools may act in a person's name?" Ask what the person handed this
  tool, for what, until when.

---
**meaning L3118-3120 · R2 · ABOVE E7**

- (14) "Looking, captured separately from shown." The system can know "shown".
  "Looked" is an inference, so it is a running answer and never a fact. And not
  "at a tick": when what is shown changes.

---
**meaning L3124-3125 · R2 · DISAGREES E3**

- (11) "Whose clock?" The cut is the writer's clock.


---
**meaning L3126-3129 · R2 · ABOVE E4**
*Round two › C. Above the table, after the leans*

### C. Above the table, after the leans

1. **Ambient authority is still first.** (8) and (15) record more and decide the
   same way. T1.

---
**meaning L3130-3136 · R2 · ABOVE E6,E7,E1**

2. **New, and second: decide what deserves to be a fact.** Every read, every
   tick, every verdict, never trimmed. Together the leans turn the store into
   exhaust plus a watch kept on people, and the map stops being loud about
   anything. Wikidata's machine-filled source slot shows where that ends (N,
   §2.5). Realtalk's stance is the counterweight: "There's very little "data"."
   A fact is what someone stands behind, what conferred authority, or what was
   shown. The rest is running answers and passing statements.

---
**meaning L3153-3160 · R2 · ABOVE E4,C5**

**Resembles and differs.** The capability systems I lean on pass live references
between running objects; Sid's grants are inert facts checked at one gate. The
transfer holds because the offer cites its grant, which binds the permission to
the request. Webstrates and Realtalk are one document or one room, with no
strangers. AT Protocol has copy-holders nobody controls; Sid has one operator, so
a plan can reach every copy the store itself made, and no others. Urbit is one
person's log. None of them ran matching among strangers at planet scale. On that
point there is no precedent, only warnings.

---
**rama L459-460 · ABOVE · ABOVE E6**
*The group › 8. What this camp would question above the table*

**Every read recorded on every fact.** Neither man has written about it. Kreps's determinism principle supplies the best argument for it: if a tool's output is a deterministic function of its reads, then the reads are all you need to re-derive it or to know it is stale. They would ask about cost. Reads outnumber writes. If every fact carries its reads, provenance may become most of the store. Marz's one real system in this area keeps it in traces beside the data.


---
**rama L467-467 · ABOVE · ABOVE E3**
*The group › 9. Questions this camp would call the wrong question*

- **(11) "Whose clock?"** Wrong if the clock decides order. Right if the clock is data. Then the answer is to write down whose clock it was.

---
**rama L469-470 · ABOVE · NEW-REASON E1**

- **(7) "Are refusals kept?"** Not wrong, but they would turn it around. The offer is the raw fact. The verdict is the gate's opinion. Keep both, always, because the gate is code, and code is the thing that will be wrong.


---
**rama L532-533 · R2 · CARRIED E3,A1**
*Round two › T1. Which log › Shape (a): the offers depot is the permanent record; facts a*

What lean (11) does to it. "The gate's clock only" makes the gate non-deterministic in Kreps's exact sense (R: "a call to gettimeofday or some other non-repeatable thing"). The rows can then not be re-derived from the depot. RPL says the same in writing (N): "If your existing PStates have data that was non-deterministically generated, you might find that you need to describe your change in terms of existing views rather than in terms of your depot records."


---
**rama L534-535 · R2 · CARRIED E3,A1,X2**

Replay. Running the offers through the gate again gives the same winners and the same versions, if per-cell order holds and if the gate's reads of grammar and policy are local and ordered (T4). It gives a different "when". It can give different verdicts wherever a grammar or policy reached the gate's task by another partition's timing. So replay is not a rebuild.


---
**rama L550-551 · R2 · CARRIED E3,E1**
*Round two › T1. Which log › Shape (b): the gate decides, commits, then publishes the adm*

What lean (11) does to it. Nothing. The clock is read once, stored with the decision, then published. Logging results is exactly how a non-deterministic leader stays replayable.


---
**rama L564-565 · R2 · DISAGREES E3**
*Round two › T1. Which log › Which would Marz pick*

Neither as stated. **R:** "since PStates are not the source of truth – the depots (event logs) are – mistakes can be corrected via recompute from the source of truth." **R:** his records carry their own time, put there before the append: "The appended records would have three keys: userId, location, and timestamp." **R:** his version is the length of the cell's list (collaborative editor post). **I:** he would take (a), and move the clock read in front of the append, so the gate is a pure function of depot order and the fact rows stay views. That contradicts lean (11) unless the door counts as part of the gate. **N:** he publishes derived depots when another module needs a feed.


---
**rama L610-610 · R2 · DISAGREES E4**
*Round two › T4. The gate's three checks under a stream gate*

5. **Check policy at the door**, before the append. Marz (R): permissions are "redundant checks" put in front of the events store. The gate then checks shape and version only.

---
**rama L615-616 · R2 · DISAGREES E4,C1,C3**

What that means for the leans. Grammars, and the few policies on shared and base layers, fit option 1. Per-person and per-agent grants do not. With tens of agents per person across a planet they are neither small nor rarely written, and "every task pays every write". Under partition-by-entity, a personal layer's facts are spread over every task. So a personal layer's policy can be local to every task, or small, but not both. **I:** the gate cannot check personal grants cheaply under lean (10). They are checked at the door (option 5), or the ownership rule is made structural, so that the layer id itself says who owns it, and the rule that an owner may write their own layer needs no read. Either way the verdict should say who checked what.


---
**rama L647-647 · R2 · NEW-REASON E6**
*Round two › T5. One cluster worldwide*

11. What record size is practical in a depot? Any guidance for records of tens of kilobytes (long read lists)?

---
**rama L670-671 · R2 · DISAGREES E4,C8,A2**
*Round two › A. The leans, one by one*

**(8) By whom. STANDS, with one sharpening.** Marz (R): permissions belong in front of the events store. Rama has no authentication (NOT IN THE REFERENCE), so a door is forced anyway. Sharpen (I): what the door checked can only be known at that moment. Record the door's build and its method on the verdict. The case: in year four a door bug accepts any by-whom from agent hosts for two weeks. Which facts are suspect? That is answerable only if each verdict names the door build that vouched for it. On agents as their own actors and on grants: nothing sourced. The Rama angle is in T4: grants are many, so the door checks them.


---
**rama L673-673 · R2 · CARRIED E3,E1**

- When: STANDS. Kreps (R): position orders; a wall clock is data. Sharpen: under a stream gate a retry reads the clock again, so the first stamp must be looked up by offer id (ties to 7). Record which task stamped it (`Ops.CURRENT_TASK_ID`, **CHECKED** `docs/11-stream-topologies.md:46`) and which module instance (`ops/module-instance-info`, **CHECKED** `skill/pstate-schema.md:62`). Marz (R) also carries the source's own time in the record. It cannot be added later.

---
**rama L674-674 · R2 · DISAGREES E6**

- Every read listed, no grace: nothing sourced on the rule itself. On where reads live, Marz (R) keeps them in traces beside the data. The case: one agent fact that stands on ten thousand point reads. The offer is then mostly reads, in a system whose own rule of thumb treats about 50 KB as one fetch (**CHECKED** `docs/14-depots.md:242`). Sharpen (I): write the read list once as its own record, and let the fact point at it.

---
**rama L685-686 · R2 · NEW-REASON E5**

**(4) Depends-on marks.** Nothing sourced. One line from Kreps's determinism (R): for a deterministic tool every read is depends-on by construction, so the mark carries information only for people and models.


---
**rama L693-694 · R2 · DISAGREES E1,P0**

**(7) The gate's yes or no. SHARPEN.** The verdict index has to sit on the target cell's partition, keyed by offer id, written in the same event as the fact (**CHECKED** `docs/23-acid-semantics.md:41`, `skill/stream.md:13`). "Refusals in the offerer's session layer" can be a label. It cannot be a location. The case: an offer replayed after its commit finds its own version current and refuses itself, unless the lookup is on the same task. On "forever": Marz (R): "Garbage collection gets rid of data that is of low value." Kreps (R): event data gets a window. The case: hundreds of people and agents racing on one hot cell produce N minus one refusals for each fact, each with full based-on, kept forever. The camp would let refusals age out after a horizon. Lean (0) forbids that.


---
**rama L695-696 · R2 · NEW-CASE E8,A2**

**(12) Session start. SHARPEN.** Stamp the gate's module instance on every verdict, not only at session start. The case: a module update lands in the middle of a session (**CHECKED**, 2 to 30 seconds, `docs/19-operating-rama.md:441`). The session-start fact cannot say which gate build admitted fact number 5,001. Marz (I), from human-fault tolerance: after a bad deploy you must find what the bad code wrote.


---
**rama L697-698 · R2 · DISAGREES E7,P0**

**(14) The hand. REJECT putting hand ticks, as facts, into the never-trimmed log.** Kreps (R, *The Log*): "For event data, Kafka supports just retaining a window of data." Marz (R): garbage-collect low-value data. Rama (N): trimming and other depot options are set per depot (**CHECKED** `docs/14-depots.md:316-327`, `docs/19-operating-rama.md:354-356`). The case: 300 people on one problem, pan and zoom ticks at 10 per second, 8 hours. That is 86.4 million facts a day for one problem. Each carries because-of and based-on, is replicated three times, is never trimmed, and competes with real writes for the stream gate's per-task budget (**CHECKED** `docs/11-stream-topologies.md:295`). The camp would give the hand its own depot, its own retention, and a microbatch topology. One model, different storage promises.


---
**rama L708-708 · R2 · ABOVE E7,P0**
*Round two › B. Which leans this camp would call the wrong question*

4. **Lean (14), which motions become facts.** Ask instead: which kinds of record share the permanent log's promises?

---
**rama L709-709 · R2 · ABOVE E1,P0**

5. **Lean (7), where refusals are kept.** The intake depot already keeps them. Ask instead: is the verdict index a fact, or a view over offers?

---
**skeptics L282-282 · ABOVE · DISAGREES E6,E5**
*6. What this camp would question above the table*

- **Every read recorded as provenance on every fact.** The default records none. Big tech records a few, and only where they bear weight: the version being replaced (Dynamo), the freshness of the permission check (Zanzibar), the reader's own recent writes (FlightTracker). INFERRED: the industry's revealed choice is a small, typed set of reads that the system acts on, not a complete list that nothing reads. Group A reached the same place from theory.

---
**skeptics L293-293 · ABOVE · ABOVE E1,C1**
*7. Questions this camp would call the wrong question*

- **"Is the gate's yes/no kept?"** Featonby's version: what does the *caller* get when the same offer arrives again, late, perhaps after the thing was superseded or erased? Amazon keeps the token and the original parameters and returns the original answer. That decides what must be stored.

---
**skeptics L376-376 · R2 · ABOVE E6,C2,A1**
*Round two (2026-09-20): the leans, pressed from the skeptics › T5. Rows plus an audit log, judged fairly against the five w*

- *What did I look at yesterday?* In one respect better than Sid's store. A temporal table gives "Reconstructing the state of the data as of any time in the past" (R, Microsoft Learn), and on one primary that as-of is one number. What was on a given screen also needs the query and its time. pgAudit can log reads (R, AWS post: "You can choose what events are audited, including DDL operations, reads, writes"). So yes, if read logging is on, within log retention, and if rendering is reproducible. By default it is off, and retention is weeks.

---
**skeptics L377-377 · R2 · DISAGREES E2,P0,C5**

- *What was the model given?* Yes, and more simply: prompt and reply stored verbatim (COMMON PRACTICE). But sampled, expiring, outside the system of record, and a blob, so "which prompts contained this fact" is a text search. A verbatim prompt log is also one more copy of personal data to erase.

---
**skeptics L379-381 · R2 · NEW-REASON E5,E6**

- *What went stale?* No. The audit table knows that a row changed. Nothing knows what stood on the old value, because reads are not data. Teams patch this per feature with source-id columns. That is based-on reinvented each time, in a different shape each time.
- *What is in doubt?* No, for the same reason, and not transitively either.


---
**skeptics L382-383 · R2 · NEW-CASE E5,E6**

The deciding case: a model summary over 200 papers, and paper 117 is retracted a year later. Which summaries are now in doubt, and which claims did people later build on those summaries with other tools? Rows plus an audit log can say that 117 changed. It cannot say the rest, because no common shape links a write to what it stood on across tools built by different hands. That is the "big performance or functionality advantage" that Lesson 12 asks for (R).


---
**skeptics L384-385 · R2 · ABOVE E6,C3**

The fair limit: that is a verdict on the default as practised, not on the engine. A fact table and a read-set table in Postgres, behind a gate that refuses any write without a read set, would give all five walks. Stonebraker and Pavlo would say so at once (I). What the engine does not give cheaply is millions of standing patterns matched against landings, and a gate per partition at planet scale.


---
**skeptics L410-411 · R2 · DISAGREES E3,E1,C2**
*Round two (2026-09-20): the leans, pressed from the skeptics › T7. Aurora DSQL as Sid's shape: what it fixed at its record *

Set against the table: items 1 to 3 are leans (10) and (11) reversed. The gate's stamp is the order across partitions, and as-of is one number. DSQL pays for that with clocks of bounded error (R, Brooker's About Time) and with waiting. Item 4 is lean (7) reversed for refusals.


---
**skeptics L412-413 · R2 · DISAGREES E3,C2,C3**

**For one stream gate per partition in Rama (I).** Each gate stamps from a good clock and never backward. With a single writer per partition that is one comparison. Each gate announces its point every tenth of a second or so, as a runtime signal. Kept as facts, those announcements would swamp the log, so only an occasional durable residue is needed. The gate stamps a fact later than anything the fact stood on. Readers take the lowest announced point as "ready through T". The gate is a reader too: before it admits, it reads grammar and policy from other partitions. It can wait until those partitions have announced past its stamp, which is "no optimism" and costs up to one announcement interval per admission. Or it can admit and write on the verdict the point it saw. A chain that must land in several partitions together needs the protocol Brooker left out. TAO's answer was a repair job (R). The first-record consequence is only the two promises on `when`. They cost no bytes, and they cannot be added to facts already stamped.


---
**skeptics L418-418 · R2 · DISAGREES E3**
*Round two (2026-09-20): the leans, pressed from the skeptics › A. Leans this camp would reject or sharpen*

- **(11) `when` never used for order: upheld by the default creed, rejected by DSQL's example.** Brooker (R) calls wall-clock time for humans only "the right default position", and the same author's system orders by it once the error is bounded. Deciding case: T7.

---
**skeptics L426-426 · R2 · NEW-CASE E1,C1**

- **(7): sharpened.** Featonby (R) asks what a late retry receives, and answers: the original response. So verdicts and refusals must be findable by offer id from anywhere, not only inside a session layer that has ended. Deciding case: an agent crashes, restarts the next day in a new session, and replays its outbox.

---
**skeptics L428-428 · R2 · NEW-CASE E4**

- **(15): sharpened.** Pavlo (R): minimal privileges, above all for agents. Deciding case: a tool fact imported from a colleague is now "in their own layer", and so on by default. Make on-by-default hold only for tools the person wrote (I).

---
**skeptics L433-433 · R2 · DISAGREES E3,C2**
*Round two (2026-09-20): the leans, pressed from the skeptics › B. Wrong questions*

- (11): not "may `when` order?" but "what is the cheapest promise on `when` that keeps a one-number as-of possible later?"

---
**skeptics L435-435 · R2 · ABOVE E1**

- (7): not "where do refusals live?" but "what does a late retry get, and from where?"

---
**skeptics L443-443 · R2 · DISAGREES E3**
*Round two (2026-09-20): the leans, pressed from the skeptics › C. Above the table: what the leans change*

- The strongest change: keep `when` able to order. It costs nothing at the first record and cannot be bought later.

---
**sync L88-94 · SHORT · NEW-REASON E6**
*1. The short version*

5. **Reads should be captured by the runtime as ranges plus a position, never
   authored.** Convex records "the index range we scanned", so a read set
   covers rows that do not exist yet, and one algorithm serves both commit
   conflicts and live-query invalidation. Bayou defined the dependency set of
   a read formally in 1994, and then had to drop literal write-id sets because
   they "could get large".


---
**sync L95-101 · SHORT · NEW-CASE E5,C6**

6. **A read's role must be written at write time.** Pijul's 1.0 rewrite split a
   change's links into "strict dependencies" and "merely a set of 'known'
   changes". Automerge keeps `deps` (what I had seen) apart from `preds` (what
   I overwrite) and Kleppmann insists the second is not derivable from the
   first. Mercurial's developers wish their supersession markers had recorded
   *what kind* of rewrite happened.


---
**sync L3126-3135 · ABOVE · ABOVE E6**
*5. The group › 5.2 What this camp would question above the table*

**"Every read recorded as provenance on every fact."** Sid is alone here. No
system in this camp stores read sets durably per record. Convex holds them in
memory for live queries. Bayou gave up literal sets. The causal-graph systems
store what a record *stood on*, not what its maker *looked at*. The nearest
thing is Patchwork's Account History (2026), and it lives in the reader's own
document. That placement is the camp's privacy point: who read what is more
sensitive than who wrote what, and belongs in the reader's layer. The cost is
bytes. The payoff is large and nobody else has it: the reach of a deletion,
staleness, doubt, and "what was the model given" all fall out of it.


---
**sync L3155-3156 · ABOVE · ABOVE E6**
*5. The group › 5.3 Questions this camp would call the wrong question*

4. **(11) "Is every read listed?"** Reads are not listed by anyone. They are
   captured by the runtime, as predicates with positions.

---
**sync L3157-3159 · ABOVE · DISAGREES E7**

5. **(14) "Which motions become facts?"** The unit is not the motion. It is the
   crossing (what was shown) and the operand (what was selected when the person
   acted).

---
**sync L3162-3164 · ABOVE · ABOVE E3**

7. **(11) "Whose clock?"** With one gate this is nearly settled. The live
   question is whether a second, claimed time is allowed, and the camp says yes,
   as a value.

---
**sync L3356-3359 · R2 · NEW-REASON E2,C3**
*Round two: the leans, pressed › T1. "No optimism" against one worldwide gate*

- *Shared layers.* A promotion into a team layer or the base goes to wherever
  that entity lives and contends there with hundreds of others. Showing the
  offer *as an offer* while it waits is inside Sid's rule as the brief words it
  ("as if it were"). (I)

---
**sync L3469-3477 · R2 · ABOVE E5,C4**
*Round two: the leans, pressed › T4. A read's role now; follow-or-stay later*

I accept the split. A based-on entry is a statement about the past, so it
always stays on the version read. Whether the fact should now be judged against
the latest is the staleness definition's business, and definitions are facts
that can change. So the second half of lean (4) is the wrong question *for
reads*. It is the right question for a different thing: a *value* that refers
to another entity. p2panda made floating and pinned two field types (R §2.15);
AT Protocol has the plain URI and the strong reference (N §2.13). That belongs
in the key's grammar.


---
**sync L3478-3484 · R2 · NEW-REASON E5**

The role at write time: yes. The sources are as direct as this camp gets
(Pijul R §3.6; Automerge R §2.8; Mercurial's wish R §3.2). On "the floor
guesses; the tool may correct": a correction is a new fact beside, naming the
read it re-marks, never an edit (Fossil R §3.3). And the floor rarely has to
guess. A deterministic tool depends on all its reads by construction (Convex,
I §2.2). A model or a person was exposed to theirs.


---
**sync L3485-3491 · R2 · NEW-CASE E5**

Deciding case: a summary layer over ten million papers. A model summarises
forty papers; one is retracted. If all forty reads are "depends-on", every
retraction turns thousands of summaries loudly stale, the signal becomes
noise, and people stop looking at it. A map that cries wolf also lies. If they
are "exposed-to", the summary turns quietly doubtful, and only a paper it cites
makes it stale. The role sets the loudness. It cannot be recovered afterwards.


---
**sync L3515-3526 · R2 · NEW-CASE E4,E1**
*Round two: the leans, pressed › T6. By whom plus the grant: what it cost Matrix*

No single gate orders Matrix events. So whenever branches of the event graph
meet, every server must work out which policy state holds. The proposal lists
what that demands and where the first algorithm failed. It must be "a pure
function from sets of state to a single resolved set of state". It "should not
allow malicious servers to avoid moderation action by forking and merging the
room DAG". The first algorithm mishandled chains of grants ("where Alice gives
Bob power and then Bob gives Charlie power on one branch of a conflict, when
the latter power level event is authed against the original power level (where
Bob didn't have power), it fails"), leaned on "the deprecated and untrustable
depth parameter", and produced "state resets". And it must "Be efficient; state
resolution can happen a lot on some large rooms."


---
**sync L3527-3533 · R2 · NEW-REASON E4,E1**

All of that is the price of "which grant was current?" having no single answer.
Under one gate, grants are totally ordered with the facts they authorise. The
question becomes a lookup at a position, and a grant revoked before admission
is an ordinary refusal. What is left of the cost (I): the bytes of the
references on every fact; and keeping every cited grant version forever, which
Sid's store does anyway.


---
**sync L3607-3616 · R2 · DISAGREES E1**
*Round two: the leans, pressed › A. The other leans*

**(7) The verdict.** Press "refusals kept forever". Nobody in the camp keeps
them except did:plc, which then had to purge (R §2.13). Deciding case: thirty
agents contend for one hot cell at machine rate. Each admission refuses up to
twenty-nine others. Refusals outnumber facts twenty-nine to one, forever, under
lean (13). Two remedies: keep the refusal's envelope without its value; and cut
refusals at the source with Bayou's shape, an offer that carries its own check
and its own fallback (R §2.1). On "beside": with stable random ids, beside
cannot be orphaned (Git's notes were orphaned by an id change, R §3.1). The
only argument left for inline is write volume.


---
**sync L3617-3619 · R2 · DISAGREES E8**

**(12) Session start.** Stands: Croquet, jj (R). Add the *gate's* build to the
grounds of each verdict; see C.


---
**sync L3620-3631 · R2 · DISAGREES E7**

**(14) The hand.** Reject the default. tldraw does not save pointer or
selection (N §2.7). PushPin: "there is no reason to persist such updates" (R
§2.9). Riffle saved everything, and every change to the UI became a migration
(R §2.9). Croquet never serialises view-to-view traffic (R §2.3). Deciding
case: three hundred people on one problem, the pointer sampled ten times a
second, eight hours a day, two hundred and fifty days. That is about 21.6
billion envelopes a year, each with every part, never trimmed. And who looked
where is the most sensitive record in the store. Sharpen: sample at crossings
and at offers (the operand), not at ticks. Capture of motion is off until a
person's own fact turns it on; the lean has the default the other way round.
Patchwork keeps its view log in the reader's own document (R §2.9).


---
**sync L3643-3644 · R2 · ABOVE E5,C4**
*Round two: the leans, pressed › B. Wrong questions*

1. **(4), second half.** Follow-or-stay is not a property of a read. Ask it of
   value references, in the grammar (T4).

---
**sync L3652-3653 · R2 · DISAGREES E7**

5. **(14).** "Which motions, at what tick?" Ask what the person acted on, and
   what they were shown.

---
**sync L3682-3683 · R2 · ABOVE E6,E7,P0**
*Round two: the leans, pressed › C. Above the table: only what the leans change*

4. **"Every read recorded", with (14) as leaned and (13) never trimmed,** is the
   combination this camp would break first, on volume and on privacy.

---
## Says the same as the ledger (counted, not copied)

- E1 · datalog · 2: L335-335, L793-794
- E1 · frontiers · 2: L361-362, L558-558
- E1 · log · 1: L697-698
- E1 · meaning · 1: L3072-3078
- E1 · skeptics · 1: L223-223
- E1 · sync · 2: L1770-1774, L3541-3543
- E3 · datalog · 2: L66-66, L236-236
- E3 · log · 5: L107-107, L138-138, L219-219, L306-307, L318-318
- E3 · rama · 2: L117-118, L367-368
- E3 · sync · 3: L550-550, L815-818, L930-930
- E4 · meaning · 11: L623-629, L636-645, L657-666, L725-730, L931-936, L2806-2808, L2809-2812, L2841-2842, L2843-2844, L2848-2861, L3121-3122
- E4 · sync · 4: L1715-1716, L1770-1774, L3541-3543, L3632-3634
- E5 · sync · 2: L922-922, L1229-1230
- E6 · datalog · 1: L262-262
- E6 · sync · 3: L198-201, L224-226, L2640-2643
- E8 · log · 2: L333-333, L722-722

