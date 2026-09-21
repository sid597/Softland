# P0 premise (0): main

Copied by line number by bundle.py, never retyped. Tags and notes are Codex's, under
yardstick.prompt.md. L-numbers are lines in the named research file. Zones: round three, round two, short version, above the table, own list.

---
**datalog L950-951 · R2 · CARRIED P0,C5**
*Round two › R2.2 Ask A: the leans, one by one*

**(0) Never rewritten. Stands, and XTDB v1 lived this way.** R: "the transaction log can stay immutable" [X-DOC]. It agrees with Hickey's "while the past may be forgotten, it is immutable" [H-IM], given lean (9). One thing to notice: Datomic's excision does rewrite its log, "those same datoms will be removed from the transaction log" [D-EXC], so the lean is stricter than Datomic. *Sharpen:* never rewritten, plus never trimmed, plus indexes rebuilt from the log is exactly the combination XTDB v1 had and left. Case under (13).


---
**datalog L996-999 · R2 · CARRIED P0,W1,C5**

**(13) Storage.** *Plain maps:* stands [H-WD], [X-DD7]. *Backups inside the delete plan:* stands, and it is better than Datomic's own advice, see T2. *Never trimmed:* pressed. R: XTDB v1 held the same position, "Kafka's infinite retention capability" [X-INTRO]. They left it over one operation: a reindex of "about 600GB of data at ~100 million transactions… taking days to weeks" [X-HN23]. **Case:** in year 4 a runtime rebuild changes an index's layout. A log that is never trimmed and is the only source means a replay of the planet. The camp would accept a log that is never trimmed *only if no operation ever needs a full replay*. That means snapshots of the indexes. R: v1 added checkpoints, and "The default lifecycle of checkpoints is unbounded" [X-CKPT]. And a snapshot of an index is a copy of plaintext values, so it goes inside the delete plan as well.

---


---
**frontiers L19-19 · SHORT · NEW-REASON P0,C2**
*0. The camp's shared model, in one page*

2. **What is stored is changes, each stamped with a time.** `(data, time, diff)`. The contents at time T are the sum of all changes with time ≤ T. This is an append-only log of small facts. It is very close to Sid's store.

---
**frontiers L561-561 · R2 · DISAGREES P0,E7**
*Round two (2026-09-20): the leans, pressed from the frontier › A. Leans this camp would reject or sharpen*

- **(14), rejected on "never trimmed".** McSherry (R): find "the fewer, discrete moments" at which the answer changes. Deciding case: pan and zoom at ten ticks a second, eight hours a day, is about 290,000 facts per person per day. Record the hand when it changes what is demanded, not on a clock.

---
**frontiers L578-578 · R2 · DISAGREES P0**
*Round two (2026-09-20): the leans, pressed from the frontier › C. Above the table: what the leans change*

**Resembles, differs.** Materialize, Feldera and DSQL each own the clock of every input. Sid's gates do too, which is why road B is open. None of them keeps provenance per fact or forbids trimming. The read-set shape is my construction from their parts. None of them runs it.

---
**log L560-560 · ABOVE · ABOVE P0**
*For the group › 9. Questions this camp would call the wrong question*

3. **"Never rewritten, or only never lost?" (0).** Four promises. Bytes may change. Meaning, order and identity may not. Provability is a fourth decision with its own bill.

---
**log L647-648 · R2 · CARRIED P0,C5**
*Round two: the leans, pressed › T1. The four promises against leans (0) and (9)*

**What each lean needs.** Lean (0) needs promises 2 and 3 (never different, never re-ordered) and takes 1 for granted. Lean (9) needs 1 and 2 for the ciphertext and the hash, and moves all real deletion into a key store. Neither needs promise 4 now.


---
**log L649-650 · R2 · NEW-CASE P0,W2**

**Sharpen (0): say what is never rewritten.** Helland would reject a byte-level reading: "semantically immutable but can be physically changed" (R). The deciding case: year twelve, the serializer that wrote the ten million papers' facts is dead or unsafe, and the runtime has been rebuilt two hundred times. If the promise covers Rama's stored bytes, re-encoding is forbidden forever. If it covers the canonical form that the hash covers, re-encoding is free and still checkable. I: define the promise over the canonical form.


---
**log L729-729 · R2 · ABOVE P0**
*Round two: the leans, pressed › B. Wrong questions, against the leans*

1. Lean (0) asks rewritten or lost. Ask: never rewritten *what*, the canonical form or Rama's bytes?

---
**meaning L2679-2680 · ABOVE · ABOVE P0**
*Part five — for the group › 9. Questions this camp would call the wrong question*

- **(0) "Never rewritten, or only never lost?"** Neither of those pairs with the
  other. Meaning never changes; content can be cut, visibly.

---
**meaning L2987-2997 · R2 · DISAGREES P0,C6**
*Round two › T5. AT Protocol: the one lesson for (0) and (9)*

### T5. AT Protocol: the one lesson for (0) and (9)

R (Bryan Newbold, https://github.com/bluesky-social/atproto/discussions/1410):
purging a deleted record meant rebasing the repository; rebases were "'expensive'
for all the downstream services", "which results in deleted content being
available publicly via specially crafted API calls, which breaks human intents
and expectations." The change: "There would no longer be a public, enumerable
commit history." The back-pointer became a clock value, "intentionally not a
strong reference". October 2023: "We truncated history for all repos (eg,
prev=null) during migration."


---
**rama L465-465 · ABOVE · ABOVE P0**
*The group › 9. Questions this camp would call the wrong question*

- **(0) "Never rewritten, or only never lost?"** They would say neither phrase names the property that matters. The property is: a later write never destroys an earlier fact; positions never move; derived things can always be re-derived. Byte-level permanence is not on their list.

---
**rama L475-476 · ABOVE · ABOVE P0**
*The group › The strongest challenge this camp makes to the premise*

The second half is true with or without rewriting. Information nobody captured is gone. The first half is a choice that none of the three sources makes. Marz: "When you change your schema, you'll have the capability to update all data to the new schema." Rama: depot migrations, offsets unchanged. Kafka: "The offset for a message never changes", and the contents may go. All three hold positions and information fixed, and let representation move. If Sid keeps that door, much of the table stops being a first-record decision, and the list that remains is short and is about information.


---
**rama L477-478 · ABOVE · CARRIED P0**

The honest limit of this challenge: every one of these people trusted the operator. One company owned the log. Sid's store has many writers, attribution, and an economy. There, the operator's power to rewrite the past is a trust problem, not only an engineering convenience. This camp never faced that and has nothing to say about it. If "never rewrite" is there to make the past provable to people who do not trust the operator, then it is doing work this camp's systems never had to do, and the people to hear next are the ones who build transparency logs and ledgers.


---
**rama L526-529 · R2 · CARRIED P0**
*Round two › T1. Which log › Shape (a): the offers depot is the permanent record; facts a*

### Shape (a): the offers depot is the permanent record; facts are PState rows declared primary

What it is in Rama. The natural shape. Clients append offers. One gate topology owns the fact rows (**CHECKED** `docs/15-pstates.md:41`). The gate's stamp, version and verdict are data in those rows.


---
**rama L530-531 · R2 · ABOVE P0**

What "never rewritten" then covers. Only the offers depot, and only by Sid's discipline, since Rama itself offers migration and trimming (round one, (0)). That depot holds every offer: admitted, refused, malformed, and client re-sends. An append that threw and was sent again is two records. The fact rows are mutable storage. "The gate only ever adds rows" is a rule in gate code. Rama does not enforce it, and PState migrations exist. So under (a), lean (0) protects the thing that is not the facts.


---
**rama L540-541 · R2 · CARRIED P0**

What a second store reads. Offers, by (partition, offset). Offers without verdicts. To learn what landed it must also query the fact rows, and PStates are not a feed: topologies source depots only ("All new data enters Rama via depots, and topologies source all incoming data from them", **CHECKED** `docs/14-depots.md:7`), and a reactive `proxy` follows one path, not a log (**CHECKED** `docs/15-pstates.md:436-438`). The same gap exists inside the store. "A landed fact is matched to tools" needs a feed of landed facts. Under (a) the only place that can react to a landing is the gate topology itself.


---
**rama L542-543 · R2 · CARRIED P0,E1**

Against the leans. (7) fits: verdict and fact in one event on one task. (13) "never trimmed" makes every malformed offer permanent.


---
**rama L544-547 · R2 · CARRIED P0,C1**
*Round two › T1. Which log › Shape (b): the gate decides, commits, then publishes the adm*

### Shape (b): the gate decides, commits, then publishes the admitted fact to a facts depot; consumers dedupe by fact id

What it is in Rama. A depot declared `:disallow`, appended from the gate with `depot-partition-append!`. N: "ALWAYS add a commit boundary before `depot-partition-append!` to an internal depot in a stream topology." (**CHECKED** `skill/stream.md:49`). The append is at-least-once (**CHECKED** `docs/12-microbatch-topologies.md:130`, `skill/depot-design.md:68`). RPL names the use: "a module may be publishing an event stream based on other depots meant for consumption by other modules" (**CHECKED** `docs/14-depots.md:39`). **IMPLIED:** the retry path must publish too. When a replayed offer is found already decided, the gate appends the stored fact again, byte for byte, because the first append may never have happened.


---
**rama L548-549 · R2 · CARRIED P0,E1**

What "never rewritten" then covers. The facts depot: an ordered, replicated log of the gate's results. This is Kreps's primary-backup log (R): the leader decides and logs out what it decided. The offers depot becomes intake. The camp would trim it once verdicts are durable. If refusals are to be kept forever (lean 7), they must be published too.


---
**rama L552-553 · R2 · CARRIED P0,A1**

Replay. Every derived index can be rebuilt from the facts depot alone, with dedupe (first record per fact id; highest version per cell). So can the gate's own state. Marz's invariant holds again (R): "In Rama, a PState can always be recomputed from the depot data, which is the source of truth."


---
**rama L558-559 · R2 · CARRIED P0,C1**

What a second store reads. The facts depot, through an `ExternalDepot`-shaped bridge, by (partition, offset). It dedupes by fact id and re-admits through its own gate as ingests.


---
**rama L560-561 · R2 · CARRIED P0,E1**

Costs. Each fact is stored three times (offer, row, published record), times the replication factor. Duplicates in a never-rewritten log are permanent. They are harmless (Marz R: "'A and A' is the same as 'A'"). The published record lags the committed row by one event, so "no optimism" needs one sentence saying which of the two is "landed". Either choice is coherent.


---
**rama L566-567 · R2 · CARRIED P0**
*Round two › T1. Which log › Which would Marz pick*

Kreps (R) names both shapes as legitimate. **I:** he would say the error is mixing them by accident.


---
**rama L568-569 · R2 · CARRIED P0,E3,X2**

My read, marked as mine. With grammar and policy as facts that the gate reads (T4), a fully deterministic gate is hard to keep for years. Shape (b) does not need one. The shape to avoid is (a) with a clock-reading gate. There the never-rewritten log cannot rebuild the facts, and the facts live only in mutable rows.


---
**rama L622-622 · R2 · NEW-REASON P0**
*Round two › T5. One cluster worldwide*

2. A write is visible only after every in-sync replica has it on disk (`:41`; `docs/23-acid-semantics.md:112`). Min-ISR defaults to the replication factor minus one (`:71`).

---
**rama L630-630 · R2 · NEW-REASON P0**

10. Online incremental backup needs a paid licence. The free version runs two nodes (R).

---
**rama L644-644 · R2 · CARRIED P0,C1**

8. Exactly-once depot appends from microbatch are "on our roadmap". When? Anything planned for stream topologies?

---
**rama L705-705 · R2 · DISAGREES P0,C3**
*Round two › B. Which leans this camp would call the wrong question*

1. **The rider on lean (0): "this makes (9) and (10) now-or-never."** They are now-or-never anyway. (10) because the task count and partition key are physical in Rama. (9) because encryption changes the bytes of the very first value. Neither depends on (0). What (0) really makes now-or-never is every choice of representation. That is the cost to weigh, and the rider hides it.

---
**rama L710-711 · R2 · CARRIED P0,E3**

6. **Lean (11), "the gate's clock only."** Ask first which log (T1). Under shape (a) this one lean decides whether the facts can ever be rebuilt.


---
**rama L716-716 · R2 · DISAGREES P0,E1,E6,E7**
*Round two › C. What the leans change above the table*

1. **One model, several storage promises.** Taken together, leans (0), (7), (11), (13) and (14) send the highest-volume, lowest-value records (hand ticks, refusals, reads that only made a tool fire) into a never-trimmed, never-rewritten log with full provenance. Marz's garbage collection (R) and Kafka's windows (R) exist for exactly that class of record. The camp would keep one kind of fact and let retention differ by kind, at the level of depots.

---
**rama L724-725 · R2 · CARRIED P0**
*Round two › Resembles and differs, carried forward*

Unchanged from round one. For these leans the camp's sourced material is strongest on (0), (7), (9) as it touches backups, (10), (13) and (14). It is thin or absent on (4), (15), and the acts-for half of (8). Every one of these people trusted the operator. The arguments here for keeping a rewrite door inherit that limit.


---
**skeptics L277-277 · ABOVE · ABOVE P0,C2**
*6. What this camp would question above the table*

- **An append-only store of small facts as the one substance.** Stonebraker and Pavlo, INFERRED: it is a table. Show the large win over rows plus audit tables, or "new constructs will go nowhere". McKinley, INFERRED: it is several innovation tokens at once. Against them, REPORTED: S3's customers built their own consistency trackers when the store would not say what was current, and Meta found permanent bugs only once stale reads became errors. The default's silence about staleness has real costs. The people who paid them say so.

---
**skeptics L291-291 · ABOVE · ABOVE P0**
*7. Questions this camp would call the wrong question*

- **"Never rewritten, or only never lost?"** Warfield and the DynamoDB authors would say the working question is: how do you *know*, this week, that nothing was lost? Append-only does not answer that. Checksums in each record and scheduled re-reading do. That decision belongs to the first record.

---
**skeptics L378-378 · R2 · DISAGREES P0,C5**
*Round two (2026-09-20): the leans, pressed from the skeptics › T5. Rows plus an audit log, judged fairly against the five w*

- *Who read the erased thing?* Roughly, from statement logs, within retention. Those logs record queries, not the rows returned. The default's real advantage shows here: it may rewrite history, so erasure is an ordinary delete in the table and in its audit table.

---
**skeptics L414-415 · R2 · DISAGREES P0,E6,E1**
*Round two (2026-09-20): the leans, pressed from the skeptics › T7. Aurora DSQL as Sid's shape: what it fixed at its record *

**Resembles, differs.** Resembles: a gate, an ordered log of accepted work, derived storage, one operator. Differs: DSQL trims old versions, records no reads, and keeps no refusals. Its clocks are AWS's. A second institution's store stands outside those promises.


---
**skeptics L427-427 · R2 · DISAGREES P0,E7**
*Round two (2026-09-20): the leans, pressed from the skeptics › A. Leans this camp would reject or sharpen*

- **(14): rejected on "never trimmed".** The default keeps hand data outside the record, sampled and expiring. Capture that a person can turn off by their own fact is the EDPB's "by design" (R).

---
**sync L3146-3149 · ABOVE · ABOVE P0**
*5. The group › 5.3 Questions this camp would call the wrong question*

1. **(0) "Never rewritten, or only never lost?"** The real axes are logical
   against physical, and whether integrity is chained through the bytes. It is
   the chaining that made deletion and migration impossible for SSB, Automerge
   and Git, not immutability as such.

---
**sync L3431-3435 · R2 · DISAGREES P0,C5**
*Round two: the leans, pressed › T3. A separable value, against lean (9)*

- *Willow chose the opposite:* no proofs of completeness, so that removal
  leaves no trace (R §2.15). AT Protocol too: services "are expected not to
  differentiate between content which has never existed and content which has
  been entirely deleted" (N §2.13), after which consumers could not tell what a
  delete had removed (R §2.13, issue #927).

---
**sync L3436-3437 · R2 · NEW-CASE P0**

- *Dangling references wait; they are never dropped.* Automerge's save dropped
  them and that was a bug (R §2.8).

---
**sync L3635-3640 · R2 · DISAGREES P0,E1,E7**
*Round two: the leans, pressed › A. The other leans*

**(13) Storage.** "Never trimmed" is safe only if (14) and (7) change. As
leaned, it is the lean most likely to be broken by year three; Irmin is the
precedent (R §3.5). "Plain maps": add an envelope-format version, and a
canonical form defined apart from any runtime's maps (SSB; AT Protocol and Go:
R §2.15, §2.13).


---
**sync L3647-3649 · R2 · ABOVE P0,C5**
*Round two: the leans, pressed › B. Wrong questions*

3. **(9).** "How do we delete from a log that is never rewritten?" Ask what the
   log is a log *of*. Envelopes and value ids. Then nothing in the log ever
   needs deleting (T3).

---
## Says the same as the ledger (counted, not copied)

- P0 · datalog · 6: L61-61, L69-78, L187-187, L333-333, L336-336, L373-376
- P0 · frontiers · 2: L34-34, L101-102
- P0 · log · 3: L71-71, L85-85, L401-401
- P0 · rama · 6: L51-51, L265-266, L267-268, L339-340, L419-420, L473-474
- P0 · sync · 10: L81-87, L275-276, L623-625, L815-818, L1235-1235, L1903-1905, L1961-1963, L1985-1987, L2278-2279, L2303-2305

