# Index of copied ranges, in reading order

| bundle | part | tag | zone | file | lines | bytes | codes | note |
|---|---|---|---|---|---|---|---|---|
| P0 | main | CARRIED | R2 | datalog | L950-951 | 532 | P0,C5 | Q10: immutable logs can survive erasure, but unbounded replay caused XTDB's retreat. |
| P0 | main | CARRIED | R2 | datalog | L996-999 | 873 | P0,W1,C5 | Q10: snapshots avoid planetary replay but introduce plaintext copies requiring erasure. |
| P0 | main | NEW-REASON | SHORT | frontiers | L19-19 | 225 | P0,C2 | Timestamped differences reconstruct collection contents by summation. |
| P0 | main | DISAGREES | R2 | frontiers | L561-561 | 298 | P0,E7 | Rejects untrimmed hand history; records demand changes instead of periodic movement ticks. |
| P0 | main | DISAGREES | R2 | frontiers | L578-578 | 276 | P0 | Surveyed systems allow trimming; proposed read-set design remains unimplemented. |
| P0 | main | ABOVE | ABOVE | log | L560-560 | 173 | P0 | Reframes immutability as separate meaning, identity, order, and provability promises. |
| P0 | main | CARRIED | R2 | log | L647-648 | 251 | P0,C5 | Q10: encryption shifts deletion into the key store; public provability is a separate promise. |
| P0 | main | NEW-CASE | R2 | log | L649-650 | 528 | P0,W2 | Obsolete or unsafe serializers require content-preserving re-encoding years later. |
| P0 | main | ABOVE | R2 | log | L729-729 | 101 | P0 | Reframes immutability around canonical form versus stored bytes. |
| P0 | main | ABOVE | ABOVE | meaning | L2679-2680 | 142 | P0 | Reframes permanence as fixed meaning plus visible removal. |
| P0 | main | DISAGREES | R2 | meaning | L2987-2997 | 627 | P0,C6 | CARRIED Q10: AT Protocol removed enumerable history and strong back-pointers. |
| P0 | main | ABOVE | ABOVE | rama | L465-465 | 282 | P0 | Reframes permanence around preserved facts, stable positions, and re-derivation. |
| P0 | main | ABOVE | ABOVE | rama | L475-476 | 590 | P0 | Representation migration narrows what genuinely must precede the first record. |
| P0 | main | CARRIED | ABOVE | rama | L477-478 | 572 | P0 | Q10; untrusted operators motivate transparency logs beyond this camp's experience. |
| P0 | main | CARRIED | R2 | rama | L526-529 | 298 | P0 | Q1; offers form the depot log while facts occupy primary PState rows. |
| P0 | main | ABOVE | R2 | rama | L530-531 | 503 | P0 | Offers-only permanence fails to cover the admitted facts themselves. |
| P0 | main | CARRIED | R2 | rama | L540-541 | 606 | P0 | Q1; offers omit verdicts and PState rows do not provide an admitted-fact feed. |
| P0 | main | CARRIED | R2 | rama | L542-543 | 133 | P0,E1 | Q1; atomic verdict rows coexist with permanently retained malformed intake. |
| P0 | main | CARRIED | R2 | rama | L544-547 | 846 | P0,C1 | Q1; result publication requires a commit boundary and retry-safe republishing of stored facts. |
| P0 | main | CARRIED | R2 | rama | L548-549 | 349 | P0,E1 | Q1; a results depot permits trimming intake but must also publish permanent refusals. |
| P0 | main | CARRIED | R2 | rama | L552-553 | 297 | P0,A1 | Q1; deduplicated result logs can rebuild both indexes and gate state. |
| P0 | main | CARRIED | R2 | rama | L558-559 | 180 | P0,C1 | Q1; a second store consumes the admitted-facts depot and deduplicates before ingestion. |
| P0 | main | CARRIED | R2 | rama | L560-561 | 371 | P0,E1 | Q1; publication triples logical storage and separates committed rows from visible log records. |
| P0 | main | CARRIED | R2 | rama | L566-567 | 102 | P0 | Q1; request-log and result-log shapes are both legitimate if kept distinct. |
| P0 | main | CARRIED | R2 | rama | L568-569 | 318 | P0,E3,X2 | Q1,Q2; logged decisions avoid requiring a permanently deterministic policy-reading gate. |
| P0 | main | NEW-REASON | R2 | rama | L622-622 | 174 | P0 | Write visibility waits for every in-sync replica. |
| P0 | main | NEW-REASON | R2 | rama | L630-630 | 89 | P0 | Online backup licensing and free-node limits constrain operational recovery. |
| P0 | main | CARRIED | R2 | rama | L644-644 | 114 | P0,C1 | Q1; asks about exactly-once internal depot publication. |
| P0 | main | DISAGREES | R2 | rama | L705-705 | 379 | P0,C3 | Attributes irreversible representation choices to permanence; placement and encryption remain first-record obligations independently. |
| P0 | main | CARRIED | R2 | rama | L710-711 | 147 | P0,E3 | Q1,Q2; log choice determines whether gate-clock stamping prevents reconstruction. |
| P0 | main | DISAGREES | R2 | rama | L716-716 | 452 | P0,E1,E6,E7 | Retention should vary by record kind instead of preserving refusals, hand ticks, and provenance forever. |
| P0 | main | CARRIED | R2 | rama | L724-725 | 324 | P0 | Q10; rewrite arguments inherit trusted-operator assumptions. |
| P0 | main | ABOVE | ABOVE | skeptics | L277-277 | 535 | P0,C2 | Challenges one fact-store substance while identifying concrete costs of absent freshness guarantees. |
| P0 | main | ABOVE | ABOVE | skeptics | L291-291 | 298 | P0 | Reframes immutability as a recurring obligation to detect loss. |
| P0 | main | DISAGREES | R2 | skeptics | L378-378 | 264 | P0,C5 | Allows rewriting audit history for ordinary deletion rather than preserving admitted content. |
| P0 | main | DISAGREES | R2 | skeptics | L414-415 | 265 | P0,E6,E1 | Trims historical versions, omits reads, and keeps no refusals. |
| P0 | main | DISAGREES | R2 | skeptics | L427-427 | 195 | P0,E7 | Permits sampled, expiring hand records instead of permanent default capture. |
| P0 | main | ABOVE | ABOVE | sync | L3146-3149 | 272 | P0 | Logical versus physical integrity is the underlying question. |
| P0 | main | DISAGREES | R2 | sync | L3431-3435 | 355 | P0,C5 | Deletion deliberately leaves no trace. |
| P0 | main | NEW-CASE | R2 | sync | L3436-3437 | 116 | P0 | Saving dropped dangling dependencies. |
| P0 | main | DISAGREES | R2 | sync | L3635-3640 | 322 | P0,E1,E7 | Untrimmed retention deemed unsafe with hand and refusal defaults. |
| P0 | main | ABOVE | R2 | sync | L3647-3649 | 180 | P0,C5 | Define log as envelopes and external value IDs. |
| P0 | body | DISAGREES | BODY | datalog | L190-191 | 438 | P0 | Forbids repair and reshaping migrations; permits only explicit tombstones. |
| P0 | body | CARRIED | BODY | datalog | L327-328 | 389 | P0,C5 | Q10: deliberate retention exceptions differ from garbage collection of derived indexes. |
| P0 | body | DISAGREES | BODY | datalog | L354-354 | 121 | P0 | Datalevin rejects universal database immutability; also ABOVE. |
| P0 | body | CARRIED | BODY | datalog | L469-470 | 652 | P0,W1 | Q10: weeks-long replay drove abandonment of the permanent log. |
| P0 | body | CARRIED | BODY | datalog | L502-502 | 71 | P0 | Q10: XTDB abandoned its permanently retained log. |
| P0 | body | ABOVE | BODY | datalog | L504-505 | 194 | P0 | Questions treating event sourcing as a universal remedy for discarded history. |
| P0 | body | DISAGREES | BODY | datalog | L522-523 | 703 | P0 | Triples mutate without history; only deleted attributes gained undo. |
| P0 | body | DISAGREES | BODY | datalog | L550-551 | 146 | P0 | Deleted data is physically gone. |
| P0 | body | DISAGREES | BODY | datalog | L552-553 | 208 | P0 | Mutable application state challenges universal immutability; also ABOVE. |
| P0 | body | DISAGREES | BODY | datalog | L554-555 | 265 | P0,C1 | No history or database values; transaction identities are not stored. |
| P0 | body | ABOVE | BODY | datalog | L558-561 | 217 | P0 | Distinguishes mutable application state from durable information. |
| P0 | body | DISAGREES | BODY | frontiers | L93-93 | 355 | P0 | Correct retained-window reads replace the never-rewritten-content invariant. |
| P0 | body | DISAGREES | BODY | frontiers | L96-96 | 228 | P0,E7 | Recommends aggressive compaction of recorded attention inputs. |
| P0 | body | DISAGREES | BODY | frontiers | L103-104 | 507 | P0 | Materialize deliberately compacts history; also contrasts opaque tools and planetary scope. |
| P0 | body | NEW-REASON | BODY | frontiers | L141-142 | 563 | P0 | Logged state supports causal debugging and historical invariant checks. |
| P0 | body | DISAGREES | BODY | frontiers | L351-352 | 586 | P0 | Permits trimming with explicit read-retention frontiers instead of preserving admitted content. |
| P0 | body | DISAGREES | BODY | frontiers | L385-386 | 250 | P0,E7 | Recorded screen demand is the first history to trim. |
| P0 | body | DISAGREES | BODY | frontiers | L389-390 | 293 | P0 | Allows trimming through since; backups additionally require authoritative sidecar maps. |
| P0 | body | NEW-REASON | BODY | log | L80-81 | 119 | P0 | More faithful retained history costs more. |
| P0 | body | DISAGREES | BODY | log | L128-128 | 533 | P0 | Permits altering admitted entries to recover from poison records. |
| P0 | body | CARRIED | BODY | log | L129-129 | 153 | P0,C5 | Q10: old log segments move to cold storage for point-in-time restoration. |
| P0 | body | DISAGREES | BODY | log | L142-142 | 209 | P0 | Allows historical editing and adds poison-record neutralization beyond erasure. |
| P0 | body | CARRIED | BODY | log | L145-146 | 436 | P0,C5 | Q10: redo-log entries are disposable commands, unlike permanent facts. |
| P0 | body | NEW-REASON | BODY | log | L154-154 | 436 | P0 | Editable history undermines audit trust; audit evidence must reconstruct present state. |
| P0 | body | DISAGREES | BODY | log | L165-165 | 439 | P0 | Practitioners routinely copy and rewrite historical content despite immutability promises. |
| P0 | body | NEW-CASE | BODY | log | L167-167 | 305 | P0 | Yearly read-only accounting stores make cross-year projections painful. |
| P0 | body | DISAGREES | BODY | log | L173-173 | 197 | P0 | Without affordable evolution, users rewrite whole stores despite immutability promises. |
| P0 | body | CARRIED | BODY | log | L196-196 | 151 | P0,C5 | Q10: the log definition permits discarding old events. |
| P0 | body | ABOVE | BODY | log | L225-226 | 274 | P0 | Local-first primary copies challenge the single-store premise. |
| P0 | body | DISAGREES | BODY | log | L232-232 | 235 | P0,C5 | Accepted entries can never be removed, excluding the ledger's erasure exception. |
| P0 | body | CARRIED | BODY | log | L233-233 | 183 | P0 | Q10: issuing a receipt creates an unconditional retention obligation. |
| P0 | body | CARRIED | BODY | log | L239-240 | 143 | P0 | Q10: external observation makes record removal detectable. |
| P0 | body | CARRIED | BODY | log | L242-242 | 579 | P0 | Q10: one corrupted leaf hash made Yeti2022 irreparable. |
| P0 | body | CARRIED | BODY | log | L243-243 | 210 | P0 | Q10: restoring an older backup forked a transparency log. |
| P0 | body | CARRIED | BODY | log | L249-249 | 229 | P0,C1 | Q10: checkpoints must never roll back while duplicate caches may lose data. |
| P0 | body | CARRIED | BODY | log | L254-255 | 574 | P0 | Q10: missing gossip concentrates trust; witness signatures address costly, brittle public verification. |
| P0 | body | CARRIED | BODY | log | L257-257 | 169 | P0 | Q10: transparency incidents expose the cost of externally checkable append-only promises. |
| P0 | body | CARRIED | BODY | log | L263-263 | 212 | P0,C7 | Q10: immutable encoding, signed extensions, retiring shards, and protected heads constrain restoration. |
| P0 | body | CARRIED | BODY | log | L269-270 | 322 | P0,E1 | Q10: transparency costs, rejected redaction, and persistence-before-receipt lessons. |
| P0 | body | CARRIED | BODY | log | L308-309 | 308 | P0,C5 | Q10: Hyder garbage-collects its redo log rather than preserving permanent factual history. |
| P0 | body | CARRIED | BODY | log | L314-315 | 255 | P0,C5 | Q10: Aurora garbage-collects redo history behind readers while retaining derived pages. |
| P0 | body | CARRIED | BODY | log | L331-331 | 168 | P0,C5 | Q10: redo history is collected; tail annulment itself is an epoch-stamped append. |
| P0 | body | CARRIED | BODY | log | L336-337 | 297 | P0,C5 | Q10: redo storage has short historical horizons rather than permanent semantic records. |
| P0 | body | CARRIED | BODY | log | L347-347 | 91 | P0,C5 | Q10: depots retain all appends by default but permit trimming. |
| P0 | body | ABOVE | BODY | log | L363-364 | 106 | P0 | Splits immutability into four independently chosen promises. |
| P0 | body | CARRIED | BODY | log | L365-365 | 209 | P0 | Q10: durable inclusion promises impose operational and storage costs. |
| P0 | body | CARRIED | BODY | log | L366-366 | 259 | P0,C5 | Q10: absence is allowed but substituted content destroys immutability. |
| P0 | body | NEW-CASE | BODY | log | L367-367 | 286 | P0,C2 | Copy-and-replace can rebuild a different history at the same recorded position. |
| P0 | body | DISAGREES | BODY | log | L368-369 | 203 | P0,C5 | External provability forbids repair, restoration, and removal. |
| P0 | body | CARRIED | BODY | log | L370-371 | 308 | P0,C5 | Q10: redo logs trim while record logs preserve factual history. |
| P0 | body | DISAGREES | BODY | log | L372-373 | 473 | P0 | Includes poison-entry editing and wholesale historical rewriting beyond content-preserving repair. |
| P0 | body | DISAGREES | BODY | log | L374-375 | 627 | P0,W2 | Requires canonical bytes immediately and adds poison-record exceptions beyond erasure. |
| P0 | body | CARRIED | BODY | log | L514-515 | 453 | P0,C5 | Q10: trimming, tiering, and whole-log retirement differ in historical information loss. |
| P0 | body | CARRIED | BODY | log | L516-517 | 392 | P0 | Q10: protected heads prevent restore rollback; tiered segments support historical recovery. |
| P0 | body | DISAGREES | BODY | log | L518-519 | 798 | P0,E1 | Accepts acknowledged facts disappearing after restore and permits refusal trimming. |
| P0 | body | DISAGREES | BODY | log | L526-527 | 605 | P0 | Includes surgical poison-entry editing among transferable corrections. |
| P0 | body | CARRIED | BODY | log | L530-531 | 355 | P0,C7 | Q10: externally verified permanence has operational costs; reserved extensions preserve evolution. |
| P0 | body | DISAGREES | BODY | log | L573-573 | 132 | P0,C1 | Copy-and-transform reuses identities across different content. |
| P0 | body | NEW-REASON | BODY | meaning | L100-106 | 308 | P0 | Records preserve utterances at a point in time. |
| P0 | body | NEW-REASON | BODY | meaning | L258-267 | 704 | P0 | Recorded past is immutable and inert. |
| P0 | body | NEW-REASON | BODY | meaning | L377-378 | 133 | P0 | Recorded past must remain inert and unchanged. |
| P0 | body | NEW-REASON | BODY | meaning | L410-417 | 478 | P0,C7 | Persistent records must serve absent future readers. |
| P0 | body | DISAGREES | BODY | meaning | L1103-1115 | 841 | P0,C6 | CARRIED Q10: AT Protocol truncated permanent history and replaced strong back-pointers. |
| P0 | body | DISAGREES | BODY | meaning | L1116-1119 | 241 | P0,C5 | CARRIED Q10: Deletion leaves no tombstone; old imports can resurrect removed records. |
| P0 | body | CARRIED | BODY | meaning | L1120-1120 | 67 | P0,C5 | Q10: Relays became non-archival. |
| P0 | body | DISAGREES | BODY | meaning | L1138-1144 | 449 | P0,C5 | CARRIED Q10: Permanent objects conflict with deliberate removal of history. |
| P0 | body | ABOVE | BODY | meaning | L1223-1228 | 444 | P0 | History growth threatens shared infrastructure and overwhelms human oversight. |
| P0 | body | CARRIED | BODY | meaning | L1567-1571 | 233 | P0 | Q10: Read-mark mutation was redesigned for write-once media. |
| P0 | body | DISAGREES | BODY | meaning | L1572-1575 | 288 | P0,C2 | CARRIED Q10: Retention windows reject old reads and writes instead of keeping every version. |
| P0 | body | DISAGREES | BODY | meaning | L2060-2067 | 487 | P0 | Unsponsored content may be discarded rather than retained permanently. |
| P0 | body | DISAGREES | BODY | meaning | L2138-2142 | 319 | P0 | Continuity breaches reboot the universe despite a frozen foundation. |
| P0 | body | DISAGREES | BODY | meaning | L2163-2172 | 639 | P0 | CARRIED Q10: Several systems abandon permanent history or retain only public unerasable material. |
| P0 | body | ABOVE | BODY | meaning | L2176-2182 | 410 | P0,C5 | Replaces the premise with fixed meaning and visible, unerasable erasure records. |
| P0 | body | DISAGREES | BODY | meaning | L2255-2266 | 734 | P0,C5 | CARRIED Q10: Trace-free deletion competes with visible retraction and suppression. |
| P0 | body | DISAGREES | BODY | meaning | L2529-2540 | 707 | P0,C7 | Open maps and trimmable histories replace fixed closed records kept forever. |
| P0 | body | CARRIED | BODY | meaning | L2592-2601 | 648 | P0,C5 | Q10: Verifiable logs and retention practices identified as missing evidence. |
| P0 | body | CARRIED | BODY | rama | L56-56 | 211 | P0,C5 | Q10; trimming respects outstanding topology consumption by default. |
| P0 | body | NEW-REASON | BODY | rama | L57-58 | 271 | P0 | Retaining raw inputs permits recovery from code-corrupted views. |
| P0 | body | CARRIED | BODY | rama | L59-60 | 636 | P0,C5 | Q10; operator policy controls whole-depot rewriting and erasure. |
| P0 | body | CARRIED | BODY | rama | L216-216 | 50 | P0 | Q10; depot trimming is disabled by default. |
| P0 | body | NEW-REASON | BODY | rama | L217-218 | 221 | P0 | Online backups require licensing; free backups require cluster shutdown. |
| P0 | body | CARRIED | BODY | rama | L235-236 | 185 | P0,C1 | Q1; neither topology supplies exactly-once publication to another depot. |
| P0 | body | CARRIED | BODY | rama | L241-242 | 1302 | P0,E3,A1 | Q1,Q2; offers-as-log and primary fact rows require different replay guarantees. |
| P0 | body | NEW-REASON | BODY | rama | L251-252 | 280 | P0 | Visibility waits for leader and in-sync follower disk persistence. |
| P0 | body | CARRIED | BODY | rama | L269-270 | 423 | P0 | Q1; Rama distinguishes depot inputs from incrementally maintained indexes. |
| P0 | body | NEW-REASON | BODY | rama | L273-274 | 526 | P0 | A statement's truth at its original time motivates immutable facts. |
| P0 | body | NEW-REASON | BODY | rama | L275-276 | 446 | P0 | Raw observations terminate derivation and anchor the source of truth. |
| P0 | body | NEW-REASON | BODY | rama | L277-278 | 528 | P0 | Append-only inputs protect good data from buggy deployments. |
| P0 | body | CARRIED | BODY | rama | L287-288 | 436 | P0,A3 | Q1; exclusive PState ownership coexists with depots as recomputation truth. |
| P0 | body | DISAGREES | BODY | rama | L297-298 | 701 | P0,C5 | Bad or low-value data may be removed, beyond preserving admitted content. |
| P0 | body | DISAGREES | BODY | rama | L307-308 | 545 | P0 | Re-derivability and deletable bad data replace immutable admitted content as the governing promise. |
| P0 | body | DISAGREES | BODY | rama | L346-346 | 71 | P0 | Attributes a ban on representation rewriting to Sid, unlike the ledger. |
| P0 | body | CARRIED | BODY | rama | L347-347 | 222 | P0 | Q10; operator-controlled rewriting becomes a trust problem in a public economy. |
| P0 | body | CARRIED | BODY | rama | L371-372 | 507 | P0,E3 | Q1,Q2; request logs and result logs permit different leader behavior. |
| P0 | body | DISAGREES | BODY | rama | L381-382 | 723 | P0 | Complete permanent history is challenged as unbounded; retention and compaction are alternatives. |
| P0 | body | NEW-REASON | BODY | rama | L387-388 | 375 | P0,A1 | Changing code motivates retained inputs and reprocessing. |
| P0 | body | DISAGREES | BODY | rama | L394-394 | 377 | P0 | Retention need cover only the desired reprocessing window. |
| P0 | body | CARRIED | BODY | rama | L413-414 | 531 | P0,E3 | Q1,Q2; log decisions when admission cannot be replayed deterministically. |
| P0 | body | DISAGREES | BODY | skeptics | L30-31 | 2167 | P0 | Mutable rows and bounded history replace permanent content; also ABOVE and CARRIED Q10. |
| P0 | body | DISAGREES | BODY | skeptics | L68-69 | 309 | P0 | Log retention and TTLs discard history rather than preserve admitted content. |
| P0 | body | ABOVE | BODY | skeptics | L98-99 | 723 | P0 | Questions ledger necessity and unique audit benefits; also CARRIED Q10. |
| P0 | body | ABOVE | BODY | skeptics | L102-103 | 721 | P0 | QLDB retirement challenges demand for a dedicated immutable ledger. |
| P0 | body | DISAGREES | BODY | skeptics | L107-107 | 250 | P0,E1 | Treats immutable-ledger audit as unnecessary; also ABOVE. |
| P0 | body | NEW-REASON | BODY | skeptics | L141-142 | 614 | P0 | Continuous verification and transfer checks make durability an ongoing activity. |
| P0 | body | NEW-REASON | BODY | skeptics | L143-144 | 582 | P0,A3 | Operational simplicity motivates single writers; checksums enable later integrity verification. |
| P0 | body | CARRIED | BODY | skeptics | L155-156 | 1103 | P0,E1,C3 | Q1: adjudication precedes journal admission, allowing storage replicas to consume only committed work. |
| P0 | body | DISAGREES | BODY | skeptics | L194-195 | 239 | P0 | TAO keeps mutable rows without history and repairs data through migrations. |
| P0 | body | DISAGREES | BODY | skeptics | L232-232 | 344 | P0 | Mutable rows and expiring logs replace permanent content; audit is said not to require a ledger. |
| P0 | body | DISAGREES | BODY | skeptics | L251-252 | 199 | P0 | Storage history is trimmed despite checksum and compatibility safeguards. |
| P0 | body | ABOVE | BODY | skeptics | L260-261 | 278 | P0 | Relational audit alternatives and ledger-product retirement challenge the need for the proposed store. |
| P0 | body | CARRIED | BODY | skeptics | L267-267 | 341 | P0 | Q10: externally audited transparency logs offer a strong append-only comparison. |
| P0 | body | CARRIED | BODY | skeptics | L268-268 | 148 | P0 | Q10: regulated write-once practice couples immutability with retention periods. |
| P0 | body | CARRIED | BODY | skeptics | L270-270 | 137 | P0,C6 | Q10: financial ledgers preserve append-only history through correction by reversal. |
| P0 | body | DISAGREES | BODY | sync | L202-206 | 388 | P0 | Stable writes are discarded; omitted history requires full transfers. |
| P0 | body | DISAGREES | BODY | sync | L228-229 | 107 | P0 | Committed history is truncated with an omission vector. |
| P0 | body | DISAGREES | BODY | sync | L246-252 | 412 | P0 | Bayou truncates its log; disconnected and planetary premises also differ. |
| P0 | body | DISAGREES | BODY | sync | L503-507 | 265 | P0,C5 | Deletion removes server properties; undo content remains with client. |
| P0 | body | DISAGREES | BODY | sync | L510-523 | 1081 | P0 | Admitted changes can be lost; journal targets nonzero loss. |
| P0 | body | NEW-REASON | BODY | sync | L564-566 | 207 | P0,A1 | Replay equality validates journal; recovery relies on snapshots. |
| P0 | body | DISAGREES | BODY | sync | L571-572 | 129 | P0,C5 | Deletion accepts history loss to bound storage. |
| P0 | body | NEW-CASE | BODY | sync | L872-874 | 179 | P0 | Saving dropped changes with missing dependencies. |
| P0 | body | NEW-REASON | BODY | sync | L1201-1203 | 137 | P0 | Snapshot restoration is possible but discouraged. |
| P0 | body | DISAGREES | BODY | sync | L1239-1241 | 160 | P0 | History may disappear once future concurrency is impossible. |
| P0 | body | DISAGREES | BODY | sync | L1274-1276 | 231 | P0 | Claims single-gate history is technically discardable. |
| P0 | body | NEW-REASON | BODY | sync | L1348-1351 | 211 | P0,W2 | Indefinite history can use compression or cold storage. |
| P0 | body | DISAGREES | BODY | sync | L1370-1374 | 398 | P0,C5 | Deletion must look like nonexistence. |
| P0 | body | NEW-REASON | BODY | sync | L1411-1414 | 311 | P0 | Retraction does not assert the opposite. |
| P0 | body | DISAGREES | BODY | sync | L1469-1475 | 544 | P0 | Append-only identity service removed invalid operations. |
| P0 | body | DISAGREES | BODY | sync | L1510-1511 | 122 | P0,E1 | Retained operations include an exception for purged rows. |
| P0 | body | DISAGREES | BODY | sync | L1514-1515 | 92 | P0,C5 | Deletion leaves no trace. |
| P0 | body | ABOVE | BODY | sync | L1649-1655 | 372 | P0 | Designer withdraws append-only terminology. |
| P0 | body | DISAGREES | BODY | sync | L1678-1685 | 595 | P0 | Rejects completeness proofs for traceless removal. |
| P0 | body | ABOVE | BODY | sync | L1707-1707 | 76 | P0 | Append-only becomes append-or-delete. |
| P0 | body | NEW-REASON | BODY | sync | L1817-1824 | 525 | P0,C1,C8 | Later annotations require stable target names. |
| P0 | body | NEW-REASON | BODY | sync | L1975-1981 | 538 | P0,C8,E3 | Supplemental facts correct authors and dates. |
| P0 | body | NEW-REASON | BODY | sync | L2001-2005 | 360 | P0 | Disk owners can edit bytes despite integrity conventions. |
| P0 | body | NEW-REASON | BODY | sync | L2035-2045 | 705 | P0 | Preserving abandoned work distinguishes actual history from reconstructed intent. |
| P0 | body | DISAGREES | BODY | sync | L2096-2101 | 356 | P0,C5 | Erasure rewrites historical commits before collection. |
| P0 | body | DISAGREES | BODY | sync | L2144-2149 | 435 | P0 | Late garbage collection discards old history. |
| P0 | body | DISAGREES | BODY | sync | L2168-2171 | 213 | P0 | Deployed nodes prune history by design. |
| P0 | body | NEW-REASON | BODY | sync | L2259-2262 | 293 | P0 | Disk ownership bounds enforceable immutability. |
| P0 | body | ABOVE | BODY | sync | L2263-2265 | 158 | P0 | Withdraws append-only premise because growth backfires. |
| P0 | body | DISAGREES | BODY | sync | L2266-2271 | 460 | P0,C5 | Excision should make history appear never written. |
| P0 | body | ABOVE | BODY | sync | L2272-2273 | 100 | P0 | Full history is excessive retention. |
| P0 | body | DISAGREES | BODY | sync | L2274-2275 | 133 | P0 | Committed log truncates with omission evidence. |
| P0 | body | DISAGREES | BODY | sync | L2276-2277 | 103 | P0 | Collection replaces indefinite retention. |
| P0 | body | DISAGREES | BODY | sync | L2280-2281 | 135 | P0,C6 | Commit chains and identity operations were removed. |
| P0 | body | DISAGREES | BODY | sync | L2282-2283 | 88 | P0 | Never-lost allows nonzero loss. |
| P0 | body | DISAGREES | BODY | sync | L2287-2290 | 214 | P0,C5 | Voices advocate traceless removal or no removal. |
| P0 | body | DISAGREES | BODY | sync | L2831-2832 | 127 | P0,E1 | Retained rejected operations were purged. |
| P0 | body | NEW-CASE | BODY | sync | L2842-2844 | 98 | P0 | Complete journaling required one state-owning type. |
| P0 | body | DISAGREES | BODY | sync | L2955-2956 | 146 | P0 | History is trimmed. |
| P0 | body | NEW-REASON | BODY | sync | L2957-2959 | 162 | P0 | Recovery and re-encoding need equality verification. |
| P0 | body | DISAGREES | BODY | sync | L2993-2999 | 436 | P0 | Signed-log camp rejects completeness chains. |
| P0 | body | DISAGREES | BODY | sync | L3009-3010 | 152 | P0,C5 | Permanent history competes with traceless removal. |
| P0 | body | CARRIED | BODY | sync | L3040-3042 | 196 | P0,E1 | Q10: witnesses and signed checkpoints strengthen append-only promises. |
| P0 | body | CARRIED | BODY | sync | L3194-3196 | 228 | P0,C8 | Q10: witnessed attestations address aging signatures. |
| C1 | main | DISAGREES | ABOVE | datalog | L617-617 | 314 | C1 | Names belong to entities and sayings; facts have no independent id; also ABOVE. |
| C1 | main | DISAGREES | R2 | datalog | L786-786 | 118 | C1 | Gate position serves as every fact's version. |
| C1 | main | CARRIED | R2 | datalog | L788-788 | 76 | C1,C8,E4 | Q8: actor and invoked grant are attributes of the saying. |
| C1 | main | CARRIED | R2 | datalog | L789-789 | 35 | C1,E3 | Q8: the gate records one stamp per saying. |
| C1 | main | DISAGREES | R2 | datalog | L800-800 | 88 | C1 | Saying plus ordinal replaces an independent fact id. |
| C1 | main | DISAGREES | R2 | datalog | L801-802 | 101 | C1,C7,C6 | Replacement uses a position; missing replacement denotes a new cell. |
| C1 | main | NEW-REASON | R2 | datalog | L810-810 | 324 | C1 | Unique-value constraints reject duplicate temporary identities. |
| C1 | main | DISAGREES | R2 | datalog | L812-813 | 466 | C1 | Preserves replay-stable names only for sayings, not individual facts. |
| C1 | main | DISAGREES | R2 | datalog | L818-818 | 153 | C1 | Fact addresses use saying-plus-place or cell-plus-position, with one saying digest. |
| C1 | main | DISAGREES | R2 | datalog | L823-824 | 221 | C1 | Cell versions are saying positions rather than independent fact identities. |
| C1 | main | DISAGREES | R2 | datalog | L836-837 | 333 | C1 | Reify an entity or use cell-plus-version instead of naming individual facts. |
| C1 | main | CARRIED | R2 | datalog | L838-840 | 201 | C1 | Q8: transaction provenance has transaction-level granularity. |
| C1 | main | CARRIED | R2 | datalog | L929-929 | 222 | C1,E3 | Q8: facts carry a transaction reference as their path to time. |
| C1 | main | CARRIED | R2 | datalog | L955-956 | 481 | C1,W1,C3 | Q6: random-id locality costs depend on whether indexes lead with id or layer. |
| C1 | main | DISAGREES | R2 | datalog | L984-985 | 629 | C1 | Name the saying rather than the fact; attach position to avoid ordering lookups. |
| C1 | main | DISAGREES | R2 | datalog | L1004-1004 | 111 | C1 | Name the saying and fact ordinal instead of an independent fact; also ABOVE. |
| C1 | main | DISAGREES | R2 | frontiers | L562-562 | 427 | C1,E1 | Uses content-plus-salt identities; NEW-REASON: pre-admission references may name refused facts. |
| C1 | main | ABOVE | ABOVE | log | L558-558 | 187 | C1 | Rejects choosing one name; identity, position, and hash have distinct jobs. |
| C1 | main | DISAGREES | R2 | log | L671-672 | 380 | C1 | Content-derived fact identity handles repeated appends; also Q9. |
| C1 | main | DISAGREES | R2 | log | L674-674 | 285 | C1,C5 | Proposes public offer salts for content-derived identity plus secret value-commitment salts. |
| C1 | main | DISAGREES | R2 | log | L675-675 | 516 | C1 | Uses derived ingest salts for content ids rather than independently random fact ids. |
| C1 | main | DISAGREES | R2 | log | L676-676 | 325 | C1 | Includes lossy CT deduplication; recommends exact cell-partition checks for Sid. |
| C1 | main | DISAGREES | R2 | log | L677-678 | 350 | C1 | Hash migration is not decisive against content names; algorithm-tagged ids and later attestations suffice. |
| C1 | main | DISAGREES | R2 | log | L679-680 | 642 | C1 | Content hash names the unchanged offer; gate stamps live solely on the verdict. |
| C1 | main | DISAGREES | R2 | log | L681-682 | 462 | C1,X1 | Names facts by saying-id plus index, not independent random ids; also Q8. |
| C1 | main | DISAGREES | R2 | log | L683-684 | 402 | C1 | Retains source-derived entity ids and versions their derivation instead of requiring random entity ids. |
| C1 | main | DISAGREES | R2 | log | L717-717 | 402 | C1,C4 | Universal seed kinds use content-derived ids; store and gate identities remain random. |
| C1 | main | ABOVE | R2 | log | L730-730 | 85 | C1 | Asks whether the fact is the unchanged offer before choosing its identity. |
| C1 | main | ABOVE | R2 | log | L743-743 | 134 | C1 | Proposes unchanged offers as facts with all gate stamps on separate verdicts. |
| C1 | main | DISAGREES | R2 | meaning | L2813-2822 | 672 | C1,E4 | Grantee identity uses a tool-body hash; NEW-REASON: scope, lifetime, delegation, and self-governing roots. |
| C1 | main | DISAGREES | R2 | meaning | L2836-2840 | 303 | C1,E4 | Tool-body hashes identify grantees; selection grants scope for one chain. |
| C1 | main | DISAGREES | R2 | meaning | L2884-2894 | 646 | C1 | Only sayings have ids; batch facts use saying-id and index pairs. |
| C1 | main | DISAGREES | R2 | meaning | L2895-2902 | 512 | C1 | CARRIED Q9: Sayings use content hashes; random identity is reserved for entities. |
| C1 | main | CARRIED | R2 | meaning | L2909-2920 | 805 | C1,W1 | Q9: Tag encoding and hash together; biased prefix bytes preclude raw-prefix sharding. |
| C1 | main | DISAGREES | R2 | meaning | L3123-3123 | 60 | C1 | Facts have no ids; sayings do. |
| C1 | main | DISAGREES | R2 | meaning | L3146-3149 | 295 | C1 | CARRIED Q9: Hashed sayings and content-derived seeds support exit; unauthenticated portable authorship remains unresolved. |
| C1 | main | CARRIED | R2 | rama | L520-521 | 215 | C1,W1 | Q6; withdraws disk-locality attribution and distinguishes documented sort-order motivation. |
| C1 | main | NEW-CASE | R2 | rama | L536-537 | 502 | C1,C2 | Backup restore reuses gate versions as well as offsets for different facts. |
| C1 | main | NEW-CASE | R2 | rama | L554-555 | 81 | C1 | Independent ids preserve names despite post-backup loss. |
| C1 | main | DISAGREES | R2 | rama | L572-573 | 225 | C1 | CARRIED Q9; content-derived ids remain usable as unchecked names after migration. |
| C1 | main | CARRIED | R2 | rama | L574-576 | 165 | C1 | Q9; unchecked content-derived names lose integrity verification. |
| C1 | main | CARRIED | R2 | rama | L577-577 | 664 | C1,C4 | Q9; reproducible seed hashes freeze canonical encoding across runtime versions. |
| C1 | main | CARRIED | R2 | rama | L578-579 | 349 | C1 | Q9; salted hashes provide commitments only to salt holders, without improving retry identity. |
| C1 | main | DISAGREES | R2 | rama | L580-581 | 266 | C1 | Camp practices include natural ids, time-bearing UUIDs, and position identities. |
| C1 | main | DISAGREES | R2 | rama | L588-589 | 413 | C1 | CARRIED Q6; UUIDv7 is favored for chronological scans, not documented disk locality. |
| C1 | main | CARRIED | R2 | rama | L590-591 | 523 | C1,W1 | Q6; lifelong random entity ids need a separate index only for creation-order scans. |
| C1 | main | CARRIED | R2 | rama | L592-593 | 528 | C1,W1 | Q6; random ids preserve partition balance and nested locality but require another chronological index. |
| C1 | main | CARRIED | R2 | rama | L642-642 | 118 | C1,C2 | Q5; asks whether cross-cluster copying preserves offsets. |
| C1 | main | CARRIED | R2 | rama | L648-648 | 96 | C1,W2 | Q9; asks whether built-in serialization remains hash-stable across versions. |
| C1 | main | NEW-CASE | R2 | rama | L689-690 | 127 | C1 | Restore-driven identity reuse motivates independent fact names. |
| C1 | main | CARRIED | R2 | rama | L707-707 | 200 | C1 | Q9; content-derived identity needs a verifier and frozen canonical bytes. |
| C1 | main | ABOVE | ABOVE | skeptics | L292-292 | 234 | C1 | Reframes id leakage as a decision about permanent public promises. |
| C1 | main | ABOVE | ABOVE | skeptics | L294-294 | 180 | C1 | Rejects the random-versus-content dichotomy; uses identity and digest together. |
| C1 | main | DISAGREES | R2 | skeptics | L421-421 | 421 | C1,C4,W2,C7 | Accepts content-derived seed ids and fixes canonical encoding now; also CARRIED Q9. |
| C1 | main | DISAGREES | R2 | skeptics | L422-422 | 404 | C1,P0 | Endorses content-derived fact identity instead of separate random names; also CARRIED Q9. |
| C1 | main | DISAGREES | R2 | skeptics | L423-423 | 391 | C1 | Derives shared entity ids from public sources; ledger derives only offer ids. |
| C1 | main | NEW-REASON | SHORT | sync | L57-65 | 552 | C1,E1 | Refusals lack admission numbers; cells, versions, and offers need distinct names. |
| C1 | main | ABOVE | ABOVE | sync | L3150-3151 | 113 | C1 | Cell name, fact name, and position are complementary. |
| C1 | main | ABOVE | R2 | sync | L3345-3355 | 800 | C1 | Distant agents need tentative dependency chains. |
| C1 | main | DISAGREES | R2 | sync | L3377-3382 | 374 | C1 | Matrix replaced assigned IDs with hashes. |
| C1 | main | CARRIED | R2 | sync | L3383-3393 | 755 | C1 | Q9: bare-reference verification and cross-store convergence favor hashing. |
| C1 | main | CARRIED | R2 | sync | L3400-3401 | 146 | C1 | Q9: salt removes convergence; separate digest retains commitment. |
| C1 | main | DISAGREES | R2 | sync | L3418-3423 | 434 | C1 | Matrix hash ID survives redaction. |
| C1 | main | NEW-REASON | R2 | sync | L3557-3570 | 940 | C1,C4 | Correctable source registry separates identity from ingestion uniqueness. |
| C1 | main | ABOVE | R2 | sync | L3645-3646 | 131 | C1,C5 | Hash coverage replaces name-versus-number question. |
| C1 | main | ABOVE | R2 | sync | L3654-3657 | 186 | C1 | Source identity rule belongs in correctable registry. |
| C1 | body | CARRIED | BODY | datalog | L55-58 | 447 | C1 | Q8: datoms contain transaction references rather than independent fact identities. |
| C1 | body | CARRIED | BODY | datalog | L65-65 | 168 | C1 | Q8: transactions are entities carrying shared metadata. |
| C1 | body | CARRIED | BODY | datalog | L79-82 | 376 | C1 | Q8: the transaction reference provides time, provenance, and causality. |
| C1 | body | DISAGREES | BODY | datalog | L194-194 | 260 | C1 | Transactor mints entity ids; clients supply temporary names. |
| C1 | body | DISAGREES | BODY | datalog | L195-195 | 481 | C1,C3 | Ids encode partition location for index locality; also bears on Q6. |
| C1 | body | DISAGREES | BODY | datalog | L196-196 | 419 | C1 | Prefers time-leading UUIDs despite their creation-time leak; also bears on Q6. |
| C1 | body | CARRIED | BODY | datalog | L198-199 | 375 | C1,W1 | Q6: random ids scatter lookups; time-leading ids trade privacy for locality. |
| C1 | body | CARRIED | BODY | datalog | L240-241 | 196 | C1,E3 | Q8: each saying adds a distinct time fact even when its content repeats. |
| C1 | body | CARRIED | BODY | datalog | L282-282 | 261 | C1,C6 | Q8: transaction entities can explicitly reference transactions they correct. |
| C1 | body | CARRIED | BODY | datalog | L283-284 | 223 | C1 | Q8: causality belongs to the saying referenced by each datom. |
| C1 | body | DISAGREES | BODY | datalog | L287-287 | 392 | C1 | Facts have no independent ids; entity, transaction, or tuple identifies them; Q8. |
| C1 | body | DISAGREES | BODY | datalog | L288-288 | 272 | C1 | Use the gate's number to identify the saying. |
| C1 | body | CARRIED | BODY | datalog | L289-289 | 441 | C1 | Q9: hashes provide universal commitments but do not reveal order or causality. |
| C1 | body | DISAGREES | BODY | datalog | L290-291 | 390 | C1 | Prefers gate numbers locally and store-number pairs or content hashes across stores. |
| C1 | body | DISAGREES | BODY | datalog | L359-362 | 695 | C1,C4 | Datomic entity ids and bases are local; names carry cross-store meaning. |
| C1 | body | CARRIED | BODY | datalog | L371-372 | 277 | C1,C8,A2 | Q8: reified transactions hold service commits and user credentials. |
| C1 | body | CARRIED | BODY | datalog | L467-468 | 524 | C1,C5 | Q9 and Q10: returning identical content races with content-addressed eviction. |
| C1 | body | CARRIED | BODY | datalog | L486-486 | 828 | C1,C5 | Q9 and Q10: content naming benefits meet eviction costs and guessable hashes. |
| C1 | body | CARRIED | BODY | frontiers | L313-314 | 210 | C1 | Q8: five-part datoms carry a transaction slot, without a separate fact-id slot. |
| C1 | body | DISAGREES | BODY | frontiers | L315-315 | 649 | C1 | Uses internal integer entity ids and UUIDs only across domains, rather than one random-id space. |
| C1 | body | DISAGREES | BODY | frontiers | L330-330 | 296 | C1 | Reported voice reserves UUIDs for cross-domain use despite the author's random-id recommendation. |
| C1 | body | NEW-REASON | BODY | frontiers | L353-354 | 184 | C1 | Random client ids avoid the coordination required by sequential assignment. |
| C1 | body | DISAGREES | BODY | frontiers | L377-378 | 422 | C1 | Derives fact identity from content and time for idempotent exchange. |
| C1 | body | NEW-CASE | BODY | log | L73-73 | 464 | C1,X2 | Undated newspaper reference fails to identify immutable content; schemas also need pinned references. |
| C1 | body | DISAGREES | BODY | log | L74-74 | 246 | C1 | Names immutable data by key and version rather than an independent random id. |
| C1 | body | NEW-REASON | BODY | log | L79-79 | 384 | C1 | Identity scope, nonreuse, and central allocation scalability. |
| C1 | body | NEW-REASON | BODY | log | L87-87 | 160 | C1,E5 | Stored pointers pin versions; following latest is computed by readers. |
| C1 | body | DISAGREES | BODY | log | L89-89 | 196 | C1,C6 | Immutable names are key-version pairs rather than independent random ids. |
| C1 | body | NEW-REASON | BODY | log | L95-95 | 192 | C1 | Identifier uniqueness must cover all users and prohibit reuse. |
| C1 | body | DISAGREES | BODY | log | L116-116 | 176 | C1 | Stable preassigned positions provide retry identity instead of independent random ids. |
| C1 | body | DISAGREES | BODY | log | L133-133 | 309 | C1 | Position alone suffices inside one log; virtualization becomes necessary across log implementations. |
| C1 | body | NEW-CASE | BODY | log | L166-166 | 195 | C1 | Copy-and-replace retains identifiers while changing event content, undermining idempotency. |
| C1 | body | NEW-REASON | BODY | log | L176-176 | 137 | C1 | Retry ids are stream-scoped while revisions separately supply order. |
| C1 | body | DISAGREES | BODY | log | L200-200 | 657 | C1 | Content hashes name updates and prevent Byzantine duplicate-id forgery; also Q9. |
| C1 | body | CARRIED | BODY | log | L209-209 | 254 | C1 | Q9: cross-repository pointers pair names with hashes; stable account ids survive renames. |
| C1 | body | DISAGREES | BODY | log | L210-210 | 323 | C1 | Operations use Lamport ids and changes use hashes rather than one random id space. |
| C1 | body | DISAGREES | BODY | log | L214-214 | 294 | C1,C6 | Versions and changes use content hashes rather than random names. |
| C1 | body | DISAGREES | BODY | log | L229-230 | 336 | C1 | Entries use hashes and indexes rather than random fact ids. |
| C1 | body | NEW-REASON | BODY | log | L237-237 | 121 | C1,E1 | Duplicate submissions may reuse an earlier receipt. |
| C1 | body | DISAGREES | BODY | log | L238-238 | 247 | C1,C4 | Log identity derives from its public key; the trust list remains external. |
| C1 | body | DISAGREES | BODY | log | L250-250 | 158 | C1 | Best-effort duplicate suppression permits repeated entries. |
| C1 | body | DISAGREES | BODY | log | L258-258 | 155 | C1 | Names are content hash and index; no independent random fact id. |
| C1 | body | DISAGREES | BODY | log | L259-259 | 117 | C1 | Exact duplicate suppression is explicitly unnecessary for correctness. |
| C1 | body | DISAGREES | BODY | log | L264-264 | 110 | C1,C4 | Log identity is a key rather than random; bootstrap trust remains outside. |
| C1 | body | NEW-REASON | BODY | log | L290-290 | 168 | C1 | Atomic idempotent appends permit retries after missing replies. |
| C1 | body | NEW-REASON | BODY | log | L351-352 | 263 | C1 | Topology depot appends can repeat under failures and retries. |
| C1 | body | NEW-REASON | BODY | log | L378-379 | 543 | C1 | Counter ids depend on trusted nodes; uniqueness and retry checks have explicit scopes. |
| C1 | body | NEW-REASON | BODY | log | L382-383 | 258 | C1 | ExpectedVersion.Any defeats Event Store's otherwise scoped duplicate check. |
| C1 | body | CARRIED | BODY | log | L452-453 | 373 | C1,E5 | Q9: immutable references and name-plus-hash commitments pin exactly what was read. |
| C1 | body | NEW-REASON | BODY | log | L471-471 | 120 | C1 | Event identity, stream revision, and global position perform different jobs. |
| C1 | body | DISAGREES | BODY | log | L472-472 | 142 | C1 | Content hash and tree index serve as the two names. |
| C1 | body | CARRIED | BODY | log | L473-473 | 77 | C1 | Q9: record references include both name and content hash. |
| C1 | body | DISAGREES | BODY | log | L474-474 | 121 | C1 | Operation ids are Lamport timestamps; change ids are content hashes. |
| C1 | body | DISAGREES | BODY | log | L475-475 | 139 | C1 | Position-only naming works within one log and needs virtualization across log chains. |
| C1 | body | CARRIED | BODY | log | L476-476 | 157 | C1 | Q9: content names resist forgery but require completed encoding and more space. |
| C1 | body | NEW-CASE | BODY | log | L477-477 | 84 | C1 | Copying changed events under unchanged ids breaks identity-based retry semantics. |
| C1 | body | NEW-REASON | BODY | log | L478-478 | 81 | C1 | Assigning positions before durable persistence creates holes. |
| C1 | body | NEW-REASON | BODY | log | L479-480 | 64 | C1 | Failed topology appends can produce duplicate deliveries. |
| C1 | body | DISAGREES | BODY | log | L481-482 | 1050 | C1 | Requires hashes on every pointer and favors content names for versions; also Q9. |
| C1 | body | DISAGREES | BODY | log | L534-535 | 211 | C1 | Long-lived identities are random but versions are named by content hashes. |
| C1 | body | NEW-REASON | BODY | log | L569-569 | 353 | C1 | Own ids and content commitments survive store boundaries; local gate numbers do not. |
| C1 | body | DISAGREES | BODY | log | L571-571 | 241 | C1 | Content hashes name updates for untrusted merging; also Q9. |
| C1 | body | DISAGREES | BODY | meaning | L546-554 | 651 | C1,C4 | Qualified identity names; attribute value types cannot change under the same key. |
| C1 | body | ABOVE | BODY | meaning | L585-590 | 373 | C1 | Second-store interoperability challenges a central-store design. |
| C1 | body | DISAGREES | BODY | meaning | L646-651 | 436 | C1,E4 | Capability reference combines naming and authority, avoiding a shared namespace. |
| C1 | body | NEW-CASE | BODY | meaning | L764-768 | 295 | C1,E4 | MyWebstrates could not revoke access when document ids were capabilities. |
| C1 | body | ABOVE | BODY | meaning | L806-813 | 534 | C1,C8 | Single heaps concentrate power; cross-store delivery needs independently meaningful records. |
| C1 | body | DISAGREES | BODY | meaning | L831-844 | 996 | C1,C4 | 64-bit and parent-derived identities diverge from one random 128-bit id space. |
| C1 | body | DISAGREES | BODY | meaning | L923-930 | 479 | C1 | Jurisdiction deliberately enters ids; old alarms permanently lack names. |
| C1 | body | DISAGREES | BODY | meaning | L952-958 | 350 | C1 | CARRIED Q9: Content hashes identify definitions. |
| C1 | body | DISAGREES | BODY | meaning | L959-964 | 382 | C1,C4 | CARRIED Q9: Content hashes name immutable definitions while human names remain metadata. |
| C1 | body | DISAGREES | BODY | meaning | L965-976 | 749 | C1 | CARRIED Q9: Structural and random-mixed type identities coexist; default shifted toward unique types. |
| C1 | body | NEW-CASE | BODY | meaning | L977-981 | 380 | C1 | Random type ids caused retry churn and required stable re-derivation. |
| C1 | body | DISAGREES | BODY | meaning | L982-991 | 650 | C1 | CARRIED Q9: Use hashes for content and offers; random identities alleged to break retries. |
| C1 | body | DISAGREES | BODY | meaning | L992-998 | 470 | C1 | CARRIED Q9: Portable hashes coexist with local integer object references. |
| C1 | body | CARRIED | BODY | meaning | L999-1005 | 398 | C1,A1 | Q9: Changed inference exposed meaning omitted from existing definition hashes. |
| C1 | body | CARRIED | BODY | meaning | L1023-1027 | 262 | C1 | Q9: Missing algorithm tags led to rehashing and translation tables. |
| C1 | body | CARRIED | BODY | meaning | L1028-1038 | 715 | C1 | Q9: Hash migration retains old defects and accumulates permanent compatibility names. |
| C1 | body | CARRIED | BODY | meaning | L1039-1046 | 508 | C1 | Q9: Portable identity differs from cheap local numbering; code lacks erasure pressures. |
| C1 | body | DISAGREES | BODY | meaning | L1047-1053 | 352 | C1 | CARRIED Q9: Content hashes serve as object identities. |
| C1 | body | CARRIED | BODY | meaning | L1054-1063 | 634 | C1 | Q9: Self-description preserves algorithm agility but biases bytes and retains obsolete algorithms. |
| C1 | body | CARRIED | BODY | meaning | L1064-1071 | 497 | C1 | Q9: CID migration burns version numbers; old identifier formats persist indefinitely. |
| C1 | body | CARRIED | BODY | meaning | L1072-1082 | 720 | C1,W2 | Q9: Canonical encodings face historical nonconforming encoders. |
| C1 | body | DISAGREES | BODY | meaning | L1121-1124 | 299 | C1,C6 | Replacement should use a local number rather than a hash reference. |
| C1 | body | DISAGREES | BODY | meaning | L1127-1137 | 724 | C1 | Time-bearing ids and audit timestamps expose identity creation and correlation. |
| C1 | body | DISAGREES | BODY | meaning | L1154-1160 | 379 | C1 | Sequential identities deliberately preserve language independence while leaking creation order. |
| C1 | body | DISAGREES | BODY | meaning | L1166-1172 | 394 | C1 | Statement ids embed entity identity rather than being independently random. |
| C1 | body | DISAGREES | BODY | meaning | L1268-1276 | 494 | C1 | CARRIED Q9: Assertion identities are content hashes in the nanopublication network. |
| C1 | body | DISAGREES | BODY | meaning | L1333-1341 | 562 | C1 | CARRIED Q9: Canonicalised assertion content supplies its own tagged hash identity. |
| C1 | body | DISAGREES | BODY | meaning | L1342-1347 | 461 | C1 | CARRIED Q9: Content identities remove issuer control and identifier management. |
| C1 | body | CARRIED | BODY | meaning | L1348-1351 | 226 | C1,P0 | Q9: Hash verification provides no persistence without retained copies. |
| C1 | body | NEW-REASON | BODY | meaning | L1387-1389 | 186 | C1 | Mutable information embedded in names causes identifier changes. |
| C1 | body | NEW-REASON | BODY | meaning | L1394-1400 | 524 | C1 | Attribution must identify asserting acts rather than shared assertion content. |
| C1 | body | CARRIED | BODY | meaning | L1401-1405 | 284 | C1 | Q9: Nonportable blank nodes forced a separate graph-canonicalisation standard. |
| C1 | body | DISAGREES | BODY | meaning | L1514-1515 | 108 | C1 | Unique local integers supply object references. |
| C1 | body | DISAGREES | BODY | meaning | L1549-1552 | 283 | C1,C2 | Object name plus pseudotime identifies versions. |
| C1 | body | DISAGREES | BODY | meaning | L1553-1557 | 368 | C1,E3 | Writer clocks and site identifiers determine order. |
| C1 | body | DISAGREES | BODY | meaning | L1634-1637 | 252 | C1 | CARRIED Q9: Hashes identify immutable content; UUIDs identify changing files. |
| C1 | body | DISAGREES | BODY | meaning | L1641-1644 | 243 | C1 | CARRIED Q9: Separate name, UUID, and content-hash identity webs. |
| C1 | body | DISAGREES | BODY | meaning | L1645-1653 | 596 | C1 | CARRIED Q9: Immutable content should have hash identities. |
| C1 | body | DISAGREES | BODY | meaning | L1964-1973 | 665 | C1 | Decentralisation replaced local versions with hash identities that also granted irrevocable access. |
| C1 | body | DISAGREES | BODY | meaning | L2028-2038 | 738 | C1 | Hierarchical content addresses encode ancestry and order. |
| C1 | body | DISAGREES | BODY | meaning | L2039-2043 | 287 | C1,C6 | Ids expose place and authorship; replacement is encoded structurally rather than by pointer. |
| C1 | body | ABOVE | BODY | meaning | L2109-2115 | 465 | C1,C8 | Operator boundaries limit enforceable invariants to independently verifiable records. |
| C1 | body | DISAGREES | BODY | meaning | L2183-2196 | 894 | C1 | Survey includes 64-bit, sequential, hierarchical, and jurisdiction-bearing identities. |
| C1 | body | DISAGREES | BODY | meaning | L2197-2206 | 717 | C1 | Jurisdiction may need permanent placement in ids; width may exceed 128 bits. |
| C1 | body | DISAGREES | BODY | meaning | L2426-2439 | 966 | C1 | CARRIED Q9: Portable hashes and compound version identities compete with random fact names. |
| C1 | body | DISAGREES | BODY | meaning | L2440-2450 | 766 | C1 | CARRIED Q9: Offer identity is a canonical-content hash with nonce and algorithm tag. |
| C1 | body | DISAGREES | BODY | meaning | L2460-2464 | 318 | C1,C6 | Predecessor pointer is a local number; replacement must include a reason. |
| C1 | body | DISAGREES | BODY | meaning | L2562-2565 | 306 | C1,C7 | Jurisdiction-bearing ids and open fields accompany small ordering units. |
| C1 | body | DISAGREES | BODY | rama | L63-63 | 349 | C1 | UUIDv7 instead of opaque random ids; retries explain client-side generation. |
| C1 | body | NEW-REASON | BODY | rama | L64-64 | 237 | C1 | Random 64-bit ids reach roughly ten percent collision probability at two billion. |
| C1 | body | DISAGREES | BODY | rama | L65-65 | 232 | C1 | CARRIED Q6; time-ordered ids are preferred for sorted scans. |
| C1 | body | NEW-REASON | BODY | rama | L66-66 | 156 | C1 | UUIDv7 supplies only millisecond ordering. |
| C1 | body | DISAGREES | BODY | rama | L67-67 | 273 | C1 | Task-bearing, topology-generated ids are permitted under microbatch. |
| C1 | body | DISAGREES | BODY | rama | L68-69 | 137 | C1 | Composite user-and-task ids are endorsed instead of one opaque id space. |
| C1 | body | DISAGREES | BODY | rama | L70-71 | 704 | C1 | CARRIED Q6; leaking time or task is valued for physical access. |
| C1 | body | CARRIED | BODY | rama | L88-89 | 167 | C1,W1 | Q6; map keys require deterministic serialization and sort by serialized bytes. |
| C1 | body | DISAGREES | BODY | rama | L169-169 | 99 | C1 | Rama names records by partition and offset. |
| C1 | body | CARRIED | BODY | rama | L170-170 | 77 | C1,P0 | Q10; depot migration preserves record positions. |
| C1 | body | NEW-CASE | BODY | rama | L171-171 | 286 | C1 | Restoring a backup reuses old offsets for different records. |
| C1 | body | CARRIED | BODY | rama | L172-172 | 218 | C1,C2 | Q5; repartition creates copied depots in another module. |
| C1 | body | NEW-REASON | BODY | rama | L173-173 | 70 | C1 | Trimming invalidates position-based references. |
| C1 | body | CARRIED | BODY | rama | L176-177 | 511 | C1,P0 | Q9; content-derived identity conflicts with in-place content migration. |
| C1 | body | NEW-REASON | BODY | rama | L188-188 | 239 | C1,E1 | Stream retry can follow completed, committed writes. |
| C1 | body | DISAGREES | BODY | rama | L232-232 | 63 | C1 | Microbatch permits generating ids inside the gate. |
| C1 | body | NEW-REASON | BODY | rama | L253-254 | 1296 | C1,C8 | Cross-cluster ingestion requires self-contained fact and actor identities. |
| C1 | body | NEW-REASON | BODY | rama | L281-282 | 411 | C1 | Logical repetition of one fact is idempotent. |
| C1 | body | DISAGREES | BODY | rama | L301-302 | 431 | C1 | Time-bearing UUIDv7 replaces an unverified earlier 64-bit nonce practice. |
| C1 | body | DISAGREES | BODY | rama | L309-310 | 751 | C1 | Natural, typed, and time-bearing identities are favored over opacity. |
| C1 | body | DISAGREES | BODY | rama | L363-364 | 346 | C1,P0 | Positions permanently name records; retention may discard history. |
| C1 | body | DISAGREES | BODY | rama | L401-402 | 624 | C1,P0 | CARRIED Q10; positions are permanent identifiers while contents may be compacted away. |
| C1 | body | DISAGREES | BODY | rama | L411-412 | 309 | C1 | Kafka identifies records by positions, despite cross-cluster portability concerns. |
| C1 | body | DISAGREES | BODY | skeptics | L32-33 | 1931 | C1 | Time and shard leakage are accepted; also CARRIED Q6 on random-id index locality. |
| C1 | body | DISAGREES | BODY | skeptics | L56-57 | 1185 | C1,E1 | Stream/sequence pointers and expiring retry keys conflict with permanent ids and verdicts. |
| C1 | body | CARRIED | BODY | skeptics | L165-166 | 1682 | C1,E1 | Q9: parameter hashes collapse distinct requests; caller tokens preserve intent and late-retry answers. |
| C1 | body | DISAGREES | BODY | skeptics | L177-178 | 1034 | C1,C3 | Embeds immutable shard placement in ids; also CARRIED Q7 on association locality and cross-shard writes. |
| C1 | body | DISAGREES | BODY | skeptics | L179-180 | 347 | C1,C3 | Accepts permanent placement in ids to obtain single-server association reads. |
| C1 | body | DISAGREES | BODY | skeptics | L188-188 | 270 | C1,C3 | Meta judges opaque-id routing lookups too costly and chooses permanent shard-bearing identities. |
| C1 | body | DISAGREES | BODY | skeptics | L198-199 | 861 | C1 | Orleans makes type part of identity, unlike the single opaque id space. |
| C1 | body | DISAGREES | BODY | skeptics | L205-205 | 379 | C1 | Type-bearing Orleans identities prohibit type changes; ledger ids carry no type. |
| C1 | body | DISAGREES | BODY | skeptics | L213-214 | 452 | C1 | Requires time-sortable ids, opposing random names; widening-cost example is SAME. |
| C1 | body | NEW-REASON | BODY | skeptics | L217-217 | 185 | C1 | Declared opacity cannot prevent dependencies on visible timestamp bits. |
| C1 | body | NEW-REASON | BODY | skeptics | L221-221 | 330 | C1 | Stored tools permanently depend on observable id properties, freezing both sides of the interface. |
| C1 | body | DISAGREES | BODY | skeptics | L233-233 | 281 | C1 | Default ids expose time and shard placement rather than remaining random and opaque. |
| C1 | body | CARRIED | BODY | skeptics | L245-245 | 228 | C1 | Q9: caller ids and stored parameters distinguish retries from requests with identical content. |
| C1 | body | DISAGREES | BODY | skeptics | L259-259 | 323 | C1,C3 | Meta embeds shards in ids; also NEW-CASE on consistency retrofits and historical invariant failures. |
| C1 | body | NEW-REASON | BODY | skeptics | L303-303 | 255 | C1 | Random ids avoid the cross-store worker registry required by Snowflake-style minting. |
| C1 | body | NEW-REASON | BODY | skeptics | L304-304 | 117 | C1,C3 | Home-bearing names prevent migration to another store without renaming. |
| C1 | body | DISAGREES | BODY | sync | L143-146 | 312 | C1 | Accepting server assigns identity instead of offerer before attempting. |
| C1 | body | DISAGREES | BODY | sync | L183-189 | 439 | C1 | Server identities derive from creator identity and stamp. |
| C1 | body | DISAGREES | BODY | sync | L216-219 | 266 | C1,C7 | Server assigns identity on acceptance; infinity denotes pending admission. |
| C1 | body | DISAGREES | BODY | sync | L297-302 | 432 | C1 | IDs embed table and time; time supports reuse prevention. |
| C1 | body | NEW-CASE | BODY | sync | L315-320 | 472 | C1 | Encoded table identity blocked imports and client-generated IDs. |
| C1 | body | DISAGREES | BODY | sync | L329-336 | 586 | C1 | Server replaces temporary client IDs; optimism duplicates logic and flickers. |
| C1 | body | DISAGREES | BODY | sync | L353-356 | 243 | C1 | ID embeds day, table, and format version. |
| C1 | body | NEW-CASE | BODY | sync | L357-358 | 125 | C1 | Server-only IDs blocked offline creation and imports. |
| C1 | body | DISAGREES | BODY | sync | L365-367 | 152 | C1 | ID embeds day to prevent reuse after deletion. |
| C1 | body | DISAGREES | BODY | sync | L491-496 | 390 | C1 | Object IDs embed client identity. |
| C1 | body | DISAGREES | BODY | sync | L551-554 | 291 | C1 | IDs embed server-issued client identity. |
| C1 | body | DISAGREES | BODY | sync | L664-668 | 337 | C1 | Offer identity uses per-client counters. |
| C1 | body | NEW-CASE | BODY | sync | L701-703 | 229 | C1,X2 | Schema migrations changed mutation identity scope. |
| C1 | body | DISAGREES | BODY | sync | L717-720 | 247 | C1 | Client-group counter names offers. |
| C1 | body | DISAGREES | BODY | sync | L755-757 | 254 | C1 | ID encodes record type. |
| C1 | body | DISAGREES | BODY | sync | L791-798 | 477 | C1 | Operations, changes, and documents use different identity schemes. |
| C1 | body | DISAGREES | BODY | sync | L801-807 | 559 | C1,C7 | Hashes and actor counters name records; null components identify root. |
| C1 | body | DISAGREES | BODY | sync | L819-828 | 717 | C1 | Hash identities defend against equivocation. |
| C1 | body | DISAGREES | BODY | sync | L829-836 | 619 | C1 | Endorses hash chaining and actor IDs despite storage costs. |
| C1 | body | DISAGREES | BODY | sync | L848-853 | 400 | C1 | Migrated dependency identities from actor sequences to hashes. |
| C1 | body | NEW-CASE | BODY | sync | L854-858 | 402 | C1 | Backend hashing prevents frontend naming without a round trip. |
| C1 | body | CARRIED | BODY | sync | L865-871 | 480 | C1,C7 | Q9: hash identities require canonical parsing; compression may differ. |
| C1 | body | DISAGREES | BODY | sync | L910-915 | 415 | C1 | Recommends actor counters and conditional hash names. |
| C1 | body | DISAGREES | BODY | sync | L1043-1045 | 210 | C1,X4 | Document ID knowledge grants write capability. |
| C1 | body | NEW-REASON | BODY | sync | L1090-1092 | 206 | C1 | Content-derived document identity changes on every edit. |
| C1 | body | DISAGREES | BODY | sync | L1096-1100 | 252 | C1,E4 | Document URL is an irrevocable bearer capability. |
| C1 | body | DISAGREES | BODY | sync | L1109-1110 | 92 | C1,X4 | Secret URLs remain the access mechanism. |
| C1 | body | DISAGREES | BODY | sync | L1131-1132 | 150 | C1,X4 | Pre-permission system treats IDs as capabilities. |
| C1 | body | DISAGREES | BODY | sync | L1154-1159 | 266 | C1,C8 | Yjs uses session-counter identities and unattributed deletes. |
| C1 | body | DISAGREES | BODY | sync | L1162-1164 | 204 | C1 | ID width follows JavaScript numeric limits. |
| C1 | body | DISAGREES | BODY | sync | L1165-1169 | 367 | C1 | Argues less entropy suffices for replica IDs. |
| C1 | body | NEW-CASE | BODY | sync | L1178-1180 | 237 | C1 | Compatibility constrained ID-width expansion. |
| C1 | body | NEW-CASE | BODY | sync | L1181-1185 | 359 | C1 | Duplicate client IDs permanently corrupt documents. |
| C1 | body | DISAGREES | BODY | sync | L1242-1250 | 649 | C1 | Shared IDs use agent sequences; positions have distinct scopes. |
| C1 | body | DISAGREES | BODY | sync | L1311-1316 | 403 | C1 | Replica IDs use smaller widths than uniform per-record names. |
| C1 | body | NEW-REASON | BODY | sync | L1342-1343 | 93 | C1 | Concurrent creation motivates random session-scoped identities. |
| C1 | body | CARRIED | BODY | sync | L1375-1379 | 345 | C1 | Q9: strong references combine URI and content commitment. |
| C1 | body | DISAGREES | BODY | sync | L1380-1385 | 445 | C1 | Time-ordered IDs improve locality but allow chosen timestamps. |
| C1 | body | CARRIED | BODY | sync | L1445-1448 | 307 | C1 | Q9: pinned references force recursive migrations or broken commitments. |
| C1 | body | DISAGREES | BODY | sync | L1492-1495 | 248 | C1 | Time-bearing names buy locality. |
| C1 | body | DISAGREES | BODY | sync | L1540-1545 | 263 | C1,E3 | Events use content hashes and author clocks. |
| C1 | body | DISAGREES | BODY | sync | L1548-1552 | 334 | C1 | Event name hashes canonical content. |
| C1 | body | DISAGREES | BODY | sync | L1601-1602 | 136 | C1 | Endorses content-hash names. |
| C1 | body | DISAGREES | BODY | sync | L1628-1633 | 357 | C1 | Message identity hashes content and signature. |
| C1 | body | DISAGREES | BODY | sync | L1636-1636 | 63 | C1 | Message hash includes signature. |
| C1 | body | DISAGREES | BODY | sync | L1656-1660 | 361 | C1,C5 | Hash IDs cover metadata and unkeyed payload hashes. |
| C1 | body | DISAGREES | BODY | sync | L1672-1674 | 168 | C1 | Genesis identity derives from empty content. |
| C1 | body | DISAGREES | BODY | sync | L1708-1710 | 204 | C1,C5 | Recommends envelope hash identity with unkeyed value hash. |
| C1 | body | DISAGREES | BODY | sync | L1721-1722 | 113 | C1,C4 | Predictable empty first message determines its ID. |
| C1 | body | DISAGREES | BODY | sync | L1755-1762 | 555 | C1,C5 | Hash ID covers redacted envelope and unkeyed content hash. |
| C1 | body | DISAGREES | BODY | sync | L1763-1769 | 541 | C1 | Assigned IDs replaced with hashes to resolve clashes. |
| C1 | body | DISAGREES | BODY | sync | L1775-1782 | 490 | C1 | Accepts content-derived identity over redacted canonical form. |
| C1 | body | DISAGREES | BODY | sync | L1789-1795 | 424 | C1 | History and content are named by hashes. |
| C1 | body | DISAGREES | BODY | sync | L1798-1805 | 633 | C1 | Hash serves as name, checksum, and signing-chain component. |
| C1 | body | CARRIED | BODY | sync | L1839-1847 | 698 | C1 | Q9: migration orphans notes, textual references, and untagged signatures. |
| C1 | body | CARRIED | BODY | sync | L1848-1851 | 255 | C1,C7 | Q9: single-scheme conversion retains old defects for round trips. |
| C1 | body | NEW-REASON | BODY | sync | L1876-1877 | 127 | C1,E1 | Sidecar knowledge depends on stable names. |
| C1 | body | CARRIED | BODY | sync | L1882-1884 | 97 | C1 | Q9: Git rejects mixed algorithms; Fossil permits them. |
| C1 | body | DISAGREES | BODY | sync | L1898-1902 | 377 | C1 | Immutable commits retain content-derived IDs. |
| C1 | body | NEW-CASE | BODY | sync | L1927-1930 | 311 | C1 | Unhashed metadata makes distinct commits share identity. |
| C1 | body | DISAGREES | BODY | sync | L1971-1974 | 252 | C1 | Artifacts use untagged content-hash identity. |
| C1 | body | DISAGREES | BODY | sync | L2009-2015 | 496 | C1 | Mixes old SHA-1 and new SHA3 names. |
| C1 | body | DISAGREES | BODY | sync | L2020-2034 | 1038 | C1 | Mixed content-hash names persist; permissions remain outside history. |
| C1 | body | DISAGREES | BODY | sync | L2046-2051 | 367 | C1 | Database commits use hash identity. |
| C1 | body | CARRIED | BODY | sync | L2064-2079 | 1217 | C1,P0 | Q9: migration renamed commits, severed links, and required mappings. |
| C1 | body | NEW-CASE | BODY | sync | L2091-2095 | 356 | C1 | Auto-increment fails across clones. |
| C1 | body | DISAGREES | BODY | sync | L2114-2118 | 288 | C1 | Fact-like database derives names from content. |
| C1 | body | DISAGREES | BODY | sync | L2121-2131 | 779 | C1 | Values determine identity; cheap storage justifies growth. |
| C1 | body | CARRIED | BODY | sync | L2139-2143 | 374 | C1 | Q9: non-hash addresses shrank index 360-fold. |
| C1 | body | DISAGREES | BODY | sync | L2174-2177 | 234 | C1 | Change name hashes content and dependencies. |
| C1 | body | DISAGREES | BODY | sync | L2180-2185 | 450 | C1 | Entity name derives from creating hash plus position. |
| C1 | body | DISAGREES | BODY | sync | L2226-2238 | 900 | C1,C5 | Proposes offer-plus-index IDs and unkeyed content hashes. |
| C1 | body | DISAGREES | BODY | sync | L2239-2244 | 341 | C1 | Change identity should survive cherry-picking into new context. |
| C1 | body | DISAGREES | BODY | sync | L2314-2316 | 229 | C1 | Defends smaller replica widths despite destructive clashes. |
| C1 | body | DISAGREES | BODY | sync | L2317-2319 | 199 | C1 | Entity identity derives from creating event. |
| C1 | body | DISAGREES | BODY | sync | L2320-2323 | 264 | C1 | IDs intentionally embed time, table, or type. |
| C1 | body | DISAGREES | BODY | sync | L2324-2327 | 291 | C1 | Cited system treats IDs as permission. |
| C1 | body | NEW-REASON | BODY | sync | L2328-2330 | 134 | C1 | Distinct alphabets distinguish identities from hashes. |
| C1 | body | DISAGREES | BODY | sync | L2331-2335 | 303 | C1 | Time-bearing locality schemes compete with opaque identities. |
| C1 | body | CARRIED | BODY | sync | L2352-2354 | 151 | C1 | Q6: random IDs trade locality and debuggability for opacity. |
| C1 | body | DISAGREES | BODY | sync | L2454-2458 | 353 | C1,C5 | Hash IDs cover redacted envelopes and unkeyed content hashes. |
| C1 | body | DISAGREES | BODY | sync | L2468-2470 | 167 | C1 | Time-bearing IDs replace permanent deleted-ID registries. |
| C1 | body | DISAGREES | BODY | sync | L2571-2581 | 744 | C1,X4 | Secret IDs grant permission; public systems retrofit privacy. |
| C1 | body | DISAGREES | BODY | sync | L2747-2753 | 497 | C1,C7 | Examples use hashes and infinity markers. |
| C1 | body | DISAGREES | BODY | sync | L2757-2759 | 223 | C1 | Hash identity defended against untrusted peers. |
| C1 | body | CARRIED | BODY | sync | L2760-2769 | 715 | C1 | Q9: hashing costs space and complicates naming, migration, and metadata. |
| C1 | body | CARRIED | BODY | sync | L2770-2772 | 101 | C1 | Q9: plain internal pointers reserve hashes for external verification. |
| C1 | body | NEW-REASON | BODY | sync | L2840-2841 | 126 | C1,E1 | Stable IDs prevent sidecar orphaning. |
| C1 | body | CARRIED | BODY | sync | L3013-3013 | 77 | C1 | Q9: competing policies on mixed hash schemes. |
| C1 | body | DISAGREES | BODY | sync | L3018-3019 | 86 | C1 | tldraw puts type in identity. |
| C2 | main | CARRIED | R2 | datalog | L900-901 | 1167 | C2 | Q3: named index builds describe applied cuts, with explicit lag. |
| C2 | main | NEW-CASE | R2 | datalog | L902-903 | 735 | C2 | Nubank replaced cross-database analytics with a lagging named extract. |
| C2 | main | NEW-REASON | R2 | datalog | L982-983 | 407 | C2 | An accurate read basis includes its layer stack and policy-filtered view. |
| C2 | main | ABOVE | R2 | datalog | L1003-1003 | 147 | C2 | Ask what names a cut rather than choosing number versus vector. |
| C2 | main | NEW-REASON | SHORT | frontiers | L20-20 | 161 | C2,W3 | Time domains require a partial order and least upper bounds. |
| C2 | main | NEW-REASON | SHORT | frontiers | L21-21 | 284 | C2 | Antichain frontiers certify that earlier updates cannot arrive. |
| C2 | main | NEW-REASON | SHORT | frontiers | L22-22 | 252 | C2 | Reads wait for completion rather than emit partial answers. |
| C2 | main | ABOVE | ABOVE | frontiers | L431-431 | 190 | C2,W3 | Rejects scalar-versus-vector framing; asks who records the interleaving. |
| C2 | main | ABOVE | ABOVE | frontiers | L432-432 | 312 | C2 | Rejects lag as supplementary metadata; the frontier defines the read. |
| C2 | main | DISAGREES | R2 | frontiers | L516-517 | 271 | C2 | Requires comparability before first record rather than handling incomparable points. |
| C2 | main | CARRIED | R2 | frontiers | L522-523 | 973 | C2,C3 | Q3,Q5: shared cuts and partition starts; exposed microbatch offsets need checking; own writes wait. |
| C2 | main | NEW-REASON | R2 | frontiers | L528-529 | 530 | C2 | Opaque token meaning must survive runtime rebuilds in facts or a computation over facts. |
| C2 | main | DISAGREES | R2 | frontiers | L556-556 | 513 | C2,E3 | Rejects incomparable vectors and non-ordering when; NEW-CASE: growth from 64 to 256 partitions. |
| C2 | main | DISAGREES | R2 | frontiers | L567-567 | 127 | C2 | Requires comparable as-of points; ABOVE rejects the scalar-versus-cut question. |
| C2 | main | ABOVE | R2 | frontiers | L569-570 | 95 | C2 | Replaces lag measurement with whether reads may pass index progress. |
| C2 | main | NEW-REASON | R2 | frontiers | L574-574 | 157 | C2,A2 | Runtime progress must leave durable facts or historical cuts die at rebuild. |
| C2 | main | ABOVE | ABOVE | log | L559-559 | 126 | C2 | Reframes recording index progress as the definition of as-of. |
| C2 | main | DISAGREES | R2 | log | L721-721 | 155 | C2 | Endorses recording withheld facts, which the ledger prohibits. |
| C2 | main | ABOVE | R2 | log | L732-732 | 102 | C2,C3 | Reframes cut representation around partition naming and re-homing. |
| C2 | main | CARRIED | R2 | rama | L538-539 | 747 | C2,C3 | Q5; nondeterministic facts require direct PState copying with significant repartition downtime. |
| C2 | main | CARRIED | R2 | rama | L556-557 | 109 | C2,C3 | Q5; deterministic result-log consumers permit replay-based repartition. |
| C2 | main | CARRIED | R2 | rama | L639-639 | 92 | C2 | Q3; asks whether follower reads can state their applied position. |
| C2 | main | CARRIED | R2 | rama | L645-645 | 131 | C2,E3 | Q2,Q3; asks whether topology code can read depot offsets and append time. |
| C2 | main | DISAGREES | R2 | rama | L679-680 | 410 | C2 | Layout epochs must accompany positions from the first day, rather than remain a later extension. |
| C2 | main | CARRIED | R2 | rama | L682-682 | 204 | C2,W3 | Q5; claims microbatch global cuts survive repartition and supports both token forms. |
| C2 | main | CARRIED | R2 | rama | L687-688 | 173 | C2 | Q3; index progress must be explicitly materialized in Rama. |
| C2 | main | ABOVE | ABOVE | skeptics | L295-295 | 179 | C2,W4 | Reframes gate count around a shared lower bound on future stamps. |
| C2 | main | CARRIED | R2 | skeptics | L419-419 | 367 | C2,C3 | Q5: repartitioning invalidates recorded cuts unless historical layouts remain interpretable; scalar cuts remain permissible. |
| C2 | main | NEW-CASE | SHORT | sync | L102-108 | 459 | C2,C3 | Global counters couple availability and expose other tenants' activity. |
| C2 | main | CARRIED | SHORT | sync | L109-115 | 436 | C2 | Q3: checkpoint positions, backlog, unstamped reads, and index lag. |
| C2 | main | CARRIED | R2 | sync | L3360-3363 | 233 | C2 | Q3: distant reads need truthful lag reporting. |
| C2 | main | NEW-REASON | R2 | sync | L3438-3440 | 118 | C2,P0 | Pruning requires formerly derivable metadata. |
| C2 | main | DISAGREES | R2 | sync | L3441-3445 | 260 | C2 | Readers must distinguish withheld existence from nonexistence. |
| C2 | main | DISAGREES | R2 | sync | L3591-3596 | 370 | C2 | Private authorization references expose withheld markers. |
| C2 | main | DISAGREES | R2 | sync | L3602-3604 | 144 | C2 | Explicitly retains withheld set. |
| C2 | body | NEW-REASON | BODY | datalog | L63-63 | 173 | C2 | Immutable database values let readers avoid coordination. |
| C2 | body | NEW-REASON | BODY | datalog | L129-134 | 537 | C2 | A shareable basis enables historical permalinks without enclosing reads in transactions. |
| C2 | body | NEW-REASON | BODY | datalog | L261-261 | 282 | C2 | One database value preserves consistency across a unit of work. |
| C2 | body | CARRIED | BODY | datalog | L275-275 | 410 | C2 | Q3: merging indexed data with novelty gives a gap-free basis; sync waits. |
| C2 | body | CARRIED | BODY | datalog | L276-276 | 364 | C2 | Q3: the read basis must identify actual index progress rather than log head. |
| C2 | body | CARRIED | BODY | datalog | L277-278 | 191 | C2 | Q3: synchronous and asynchronous commits expose indexing lag explicitly. |
| C2 | body | CARRIED | BODY | datalog | L481-482 | 261 | C2 | Q5: forward-only log epochs distinguish recovery generations. |
| C2 | body | CARRIED | BODY | datalog | L491-491 | 191 | C2 | Q3: commit modes distinguish log submission from completed indexing. |
| C2 | body | NEW-REASON | BODY | frontiers | L33-33 | 788 | C2 | Nested-loop timestamps and precursor counts determine completion. |
| C2 | body | NEW-REASON | BODY | frontiers | L43-44 | 450 | C2 | Explicit histories turn coordination problems into timestamped computation. |
| C2 | body | NEW-REASON | BODY | frontiers | L45-46 | 976 | C2,W3 | Chosen interleaving aligns consumers without making independent sources mutually consistent. |
| C2 | body | NEW-REASON | BODY | frontiers | L47-48 | 1385 | C2,W3 | Durable sidecar remapping preserves source data and enables consistent recovery. |
| C2 | body | CARRIED | BODY | frontiers | L49-50 | 416 | C2 | Q5: dynamically arriving partitions require representable not-yet-started positions. |
| C2 | body | CARRIED | BODY | frontiers | L51-52 | 841 | C2 | Q3: collection read and write frontiers bound correctly readable positions. |
| C2 | body | NEW-REASON | BODY | frontiers | L53-54 | 707 | C2 | Read-time selection trades responsiveness, freshness, and monotonic observations. |
| C2 | body | NEW-REASON | BODY | frontiers | L55-56 | 442 | C2 | Capture upstream position, then wait for replication to reach it. |
| C2 | body | NEW-REASON | BODY | frontiers | L57-58 | 774 | C2 | Eventually consistent streaming may remain systematically wrong and trigger irreversible actions. |
| C2 | body | CARRIED | BODY | frontiers | L59-60 | 888 | C2 | Q3: progress counts certify completeness despite duplicated or reordered updates. |
| C2 | body | NEW-REASON | BODY | frontiers | L75-75 | 615 | C2 | Held timestamp tokens represent outstanding rights to write and constrain progress. |
| C2 | body | NEW-CASE | BODY | frontiers | L77-77 | 357 | C2,A1 | Materialize added durable reclocking because original sources were not exactly replayable. |
| C2 | body | CARRIED | BODY | frontiers | L88-88 | 446 | C2 | Q3: index upper frontier defines valid reads; readers must stay behind it or wait. |
| C2 | body | NEW-REASON | BODY | frontiers | L107-108 | 858 | C2,W3 | Keyed queries may converge when relevant updates stop; simpler time and baseline costs matter. |
| C2 | body | NEW-CASE | BODY | frontiers | L115-116 | 715 | C2 | Bank-transfer tests exposed impossible totals in Flink and ksqlDB. |
| C2 | body | NEW-REASON | BODY | frontiers | L119-120 | 315 | C2 | Internal consistency requires each output to match some supplied input subset. |
| C2 | body | NEW-REASON | BODY | frontiers | L121-122 | 323 | C2 | Streaming inconsistency can produce impossible values indefinitely, unlike merely stale key-value reads. |
| C2 | body | NEW-CASE | BODY | frontiers | L123-124 | 307 | C2 | Unsynchronized credit and debit streams manufacture money. |
| C2 | body | NEW-CASE | BODY | frontiers | L125-125 | 297 | C2,X1 | Emitting after each related update makes three of four aggregate results wrong. |
| C2 | body | NEW-REASON | BODY | frontiers | L127-128 | 219 | C2,E1 | Reject inputs only at the edge so downstream operators share the same input set. |
| C2 | body | NEW-CASE | BODY | frontiers | L129-130 | 563 | C2,E2 | Transient inconsistency can trigger a false overdraft warning. |
| C2 | body | NEW-REASON | BODY | frontiers | L131-132 | 533 | C2 | Internal consistency removes the user's obligation to reason about event interleavings. |
| C2 | body | NEW-REASON | BODY | frontiers | L133-134 | 337 | C2 | Completeness metadata amortizes over batches; correctness primarily trades against latency. |
| C2 | body | CARRIED | BODY | frontiers | L135-136 | 1017 | C2 | Q3: output frontiers let downstream consumers detect complete timestamped results. |
| C2 | body | NEW-REASON | BODY | frontiers | L148-148 | 265 | C2 | Multiple watermark waves permit early outputs and late inputs while preserving consistency. |
| C2 | body | NEW-REASON | BODY | frontiers | L153-153 | 276 | C2 | Impossible outputs are a stronger failure than stale outputs. |
| C2 | body | NEW-CASE | BODY | frontiers | L154-154 | 529 | C2,E2 | Lagging reads and branching work can mix moments or conceal partial-answer corrections. |
| C2 | body | CARRIED | BODY | frontiers | L155-155 | 201 | C2 | Q3: record index progress and forbid answering beyond it. |
| C2 | body | NEW-REASON | BODY | frontiers | L168-169 | 456 | C2,W3 | Weak consistency imposes reasoning costs; simpler time reduces complexity. |
| C2 | body | NEW-CASE | BODY | frontiers | L184-185 | 587 | C2,E5 | Shopping-cart manifests delay checkout until every named update arrives. |
| C2 | body | NEW-CASE | BODY | frontiers | L186-187 | 802 | C2 | Early checkout reads a Ferrari before its removal reaches the replica. |
| C2 | body | NEW-REASON | BODY | frontiers | L200-200 | 160 | C2 | CRDT update guarantees leave observations unsafe. |
| C2 | body | DISAGREES | BODY | frontiers | L201-202 | 139 | C2 | Eventual consistency becomes the default per-handler choice instead of universally complete answers. |
| C2 | body | NEW-CASE | BODY | frontiers | L211-211 | 172 | C2 | Lagging pattern reads reproduce the early-checkout failure. |
| C2 | body | NEW-REASON | BODY | frontiers | L251-251 | 240 | C2,W3 | Independent implementations durably bind scalar steps to partition offsets. |
| C2 | body | DISAGREES | BODY | frontiers | L271-272 | 301 | C2 | Noria deliberately permits eventual consistency while maintaining partial views. |
| C2 | body | DISAGREES | BODY | frontiers | L275-276 | 1097 | C2 | Noria sacrifices complete, correct reads for disconnected high-throughput read paths. |
| C2 | body | NEW-REASON | BODY | frontiers | L281-281 | 297 | C2,W1 | Partial materialization does not inherently require weak consistency. |
| C2 | body | NEW-CASE | BODY | frontiers | L282-282 | 315 | C2 | Noria's successors added tickets and waiting to recover read-your-writes. |
| C2 | body | DISAGREES | BODY | frontiers | L283-283 | 158 | C2 | ReadySet retains eventual consistency. |
| C2 | body | DISAGREES | BODY | frontiers | L297-298 | 252 | C2 | Presents Noria's deliberately incorrect intermediate views as an opposing convention. |
| C2 | body | DISAGREES | BODY | frontiers | L301-302 | 213 | C2 | Defends eventual consistency for keyed web reads to gain throughput. |
| C2 | body | CARRIED | BODY | frontiers | L373-374 | 221 | C2 | Q3: a pattern read is defined by the index frontier it used. |
| C2 | body | NEW-REASON | BODY | frontiers | L375-376 | 466 | C2,E6 | Causal epochs track completion of all work from an input before showing results. |
| C2 | body | NEW-REASON | BODY | frontiers | L397-397 | 382 | C2,W3 | Durable interleaving maps and coordinated frontier movement carry recovery and contention costs. |
| C2 | body | NEW-REASON | BODY | frontiers | L398-398 | 295 | C2,E6 | Intermediate-output failures and provenance granularity bear on safe displayed answers. |
| C2 | body | DISAGREES | BODY | frontiers | L403-404 | 250 | C2 | Preserves Noria's permission for briefly wrong answers as an opposing voice. |
| C2 | body | DISAGREES | BODY | frontiers | L408-408 | 194 | C2 | Estimated watermarks and later corrections oppose certified complete cuts. |
| C2 | body | NEW-REASON | BODY | frontiers | L442-442 | 338 | C2,W3 | Federated timelines need durable receiver-chosen remapping. |
| C2 | body | NEW-REASON | BODY | frontiers | L443-443 | 481 | C2 | Composable stores must exchange frontiers as well as timestamped changes. |
| C2 | body | NEW-REASON | BODY | frontiers | L444-444 | 173 | C2 | Completeness statements make exchanged updates safe under duplication and reordering. |
| C2 | body | CARRIED | BODY | log | L91-91 | 243 | C2 | Q3: alternate indexes may lag, with no reported applied-position marker. |
| C2 | body | NEW-REASON | BODY | log | L114-114 | 354 | C2,E5 | Snapshots are vectors; records carry the writer's causal frontier. |
| C2 | body | CARRIED | BODY | log | L118-118 | 288 | C2 | Q3: outgoing proposals carry local playback positions and support deterministic trimming. |
| C2 | body | NEW-REASON | BODY | log | L136-136 | 236 | C2,E5 | One offer frontier bounds everything its actor could have known. |
| C2 | body | CARRIED | BODY | log | L137-137 | 192 | C2,C3 | Q7: vector cuts preserve partial order; one global log couples failure domains. |
| C2 | body | CARRIED | BODY | log | L147-148 | 868 | C2,C3 | Q5: persist-before-order avoids holes; shared cuts order shards only without trimming. |
| C2 | body | CARRIED | BODY | log | L217-217 | 244 | C2 | Q3: subscribers checkpoint applied LSNs; direct lagging-store reads lack isolation. |
| C2 | body | NEW-REASON | BODY | log | L275-275 | 101 | C2 | Happened-before determines only a partial order. |
| C2 | body | NEW-REASON | BODY | log | L276-276 | 132 | C2,W3 | Any total order extending causal order adds an arbitrary choice. |
| C2 | body | NEW-CASE | BODY | log | L277-278 | 541 | C2,E5 | Phone-mediated causality can be reversed unless clients carry the earlier request's timestamp. |
| C2 | body | NEW-REASON | BODY | log | L288-288 | 212 | C2,E6 | One committed snapshot position names the state a transaction read. |
| C2 | body | CARRIED | BODY | log | L320-320 | 189 | C2 | Q3: read replicas lag and anchor views behind writer durability points. |
| C2 | body | CARRIED | BODY | log | L330-330 | 212 | C2 | Q3: report the applied position or wait until a requested cut is reached. |
| C2 | body | CARRIED | BODY | log | L342-343 | 1109 | C2,C5,P0 | Q3,Q10: applied positions describe replicas; compaction destroys earlier reconstructible states. |
| C2 | body | CARRIED | BODY | log | L435-436 | 503 | C2,C3 | Q7: global ordering combines shard vectors and couples failures across readers. |
| C2 | body | NEW-REASON | BODY | log | L443-443 | 201 | C2,E5 | Writer knowledge can be represented by one causal or playback frontier. |
| C2 | body | CARRIED | BODY | log | L458-459 | 684 | C2 | Q3: applied-position markers or wait-for-cut reads account for lagging indexes. |
| C2 | body | CARRIED | BODY | log | L460-461 | 790 | C2,E6,E2 | Q3: as-of must match applied state; historical replay may require separately preserved crossing evidence. |
| C2 | body | ABOVE | BODY | log | L572-572 | 190 | C2 | Continuing a store elsewhere requires an external successor pointer. |
| C2 | body | NEW-REASON | BODY | log | L574-574 | 158 | C2 | Cross-log positions are incomparable; partition tie-breaking creates only an arbitrary total order. |
| C2 | body | NEW-CASE | BODY | meaning | L278-290 | 873 | C2,A1 | Repeated age calculations diverge unless temporally qualified. |
| C2 | body | NEW-REASON | BODY | meaning | L328-329 | 122 | C2 | Functional computation needs an explicit temporal model. |
| C2 | body | NEW-REASON | BODY | meaning | L386-388 | 218 | C2,A1 | Dynamic interpretation requires temporal qualification. |
| C2 | body | NEW-REASON | BODY | meaning | L447-451 | 371 | C2 | Immutable temporal versions connect both camps. |
| C2 | body | ABOVE | BODY | meaning | L1760-1770 | 720 | C2,C3 | Actor computation lacks a single well-defined global state. |
| C2 | body | NEW-REASON | BODY | meaning | L1771-1775 | 305 | C2,E3 | Arrival order must be recorded; separate gates supply incomparable orders. |
| C2 | body | NEW-CASE | BODY | meaning | L2400-2407 | 384 | C2,W1 | Index rebuild, stale permissions, and convergence expose distinct completeness limits. |
| C2 | body | NEW-REASON | BODY | meaning | L2408-2412 | 266 | C2 | A log cut overstates evidence when the index has not applied it. |
| C2 | body | NEW-REASON | BODY | rama | L136-136 | 155 | C2 | Native positions contain partition index and offset; no global offset exists. |
| C2 | body | CARRIED | BODY | rama | L137-138 | 360 | C2 | Q3; topology code can obtain a global microbatch counter tied to input ranges. |
| C2 | body | NEW-REASON | BODY | rama | L139-140 | 238 | C2 | Readers may see partitions at different microbatches during commit. |
| C2 | body | CARRIED | BODY | rama | L145-146 | 390 | C2 | Q3; cuts need committed-partition checks or application-maintained stream counters. |
| C2 | body | CARRIED | BODY | rama | L157-157 | 182 | C2 | Q3; clients can obtain each depot partition's start and end offsets. |
| C2 | body | CARRIED | BODY | rama | L158-158 | 299 | C2 | Q3; progress exists internally, but no client API is documented. |
| C2 | body | CARRIED | BODY | rama | L159-160 | 185 | C2 | Q3; reference recommends materializing application progress counters. |
| C2 | body | CARRIED | BODY | rama | L161-162 | 348 | C2 | Q3; index topology must publish progress before historical reads can retain it. |
| C2 | body | CARRIED | BODY | rama | L231-231 | 49 | C2 | Q3; only microbatch supplies a native global progress number. |
| C2 | body | CARRIED | BODY | rama | L249-250 | 196 | C2 | Q3; readable progress requires application materialization except for microbatch ids. |
| C2 | body | CARRIED | BODY | rama | L373-374 | 233 | C2 | Q3; processed position plus log identifies replica state. |
| C2 | body | CARRIED | BODY | rama | L375-376 | 369 | C2 | Q3; serving nodes can wait for a requested index position. |
| C2 | body | CARRIED | BODY | rama | L409-410 | 243 | C2 | Q3; index points determine whether a requested read position has been reached. |
| C2 | body | DISAGREES | BODY | skeptics | L52-53 | 233 | C2,E6 | Lag remains an external metric rather than a recorded answer-completeness cut. |
| C2 | body | NEW-CASE | BODY | skeptics | L147-148 | 1164 | C2 | S3 customers built separate consistency systems before per-object witnesses supplied read barriers. |
| C2 | body | NEW-CASE | BODY | skeptics | L151-152 | 516 | C2,P0 | S3's missing freshness guarantees caused customer side systems; durability also requires operational review. |
| C2 | body | NEW-CASE | BODY | skeptics | L183-184 | 1421 | C2,C7,X2 | FlightTracker exposed permanent consistency bugs and historical records violating assumed invariants. |
| C2 | body | DISAGREES | BODY | skeptics | L189-189 | 270 | C2,E6 | Records reader freshness demands instead of each read's achieved index cut. |
| C2 | body | NEW-REASON | BODY | skeptics | L190-190 | 321 | C2 | Accepted transient staleness conceals permanent consistency bugs. |
| C2 | body | NEW-REASON | BODY | skeptics | L218-219 | 768 | C2,E1 | Opaque policy tokens preserve causality while allowing latency and availability choices. |
| C2 | body | DISAGREES | BODY | skeptics | L243-243 | 231 | C2,E6 | Lag is a metric rather than recorded read data; alternative mechanisms constrain freshness. |
| C2 | body | NEW-REASON | BODY | skeptics | L306-306 | 286 | C2,C3 | Independent institutions cannot inherit one operator's ordering promises across their store boundary. |
| C2 | body | CARRIED | BODY | sync | L227-227 | 82 | C2 | Q3: checkpoint records last reflected write. |
| C2 | body | NEW-REASON | BODY | sync | L291-292 | 150 | C2 | Whole-screen queries share one cut. |
| C2 | body | CARRIED | BODY | sync | L347-348 | 138 | C2 | Q3: derived-index reads carry known timestamps. |
| C2 | body | NEW-REASON | BODY | sync | L349-350 | 110 | C2 | All client queries use one committer timestamp. |
| C2 | body | CARRIED | BODY | sync | L407-411 | 330 | C2 | Q3: model exposes backlog relative to external progress. |
| C2 | body | CARRIED | BODY | sync | L449-449 | 80 | C2 | Q3: lag is a first-class visible value. |
| C2 | body | NEW-CASE | BODY | sync | L524-534 | 843 | C2,C3 | One unavailable shard stalled all comments. |
| C2 | body | NEW-CASE | BODY | sync | L535-541 | 518 | C2 | Unstamped reads force refetch during invalidation races. |
| C2 | body | NEW-REASON | BODY | sync | L559-560 | 98 | C2 | Missing read position forces redundant refetch. |
| C2 | body | NEW-REASON | BODY | sync | L601-605 | 317 | C2 | Successful transactions advance one database version. |
| C2 | body | NEW-REASON | BODY | sync | L606-609 | 311 | C2 | Global counter exposes other workspaces' activity. |
| C2 | body | NEW-REASON | BODY | sync | L721-724 | 248 | C2,C3 | Row versions trade read cost for concurrency and permission flexibility. |
| C2 | body | NEW-REASON | BODY | sync | L928-929 | 138 | C2 | Graph versions need heads; scalar labeling changes with graph structure. |
| C2 | body | NEW-REASON | BODY | sync | L1078-1080 | 155 | C2 | Query convergence must fit a frame. |
| C2 | body | NEW-REASON | BODY | sync | L1302-1310 | 652 | C2 | Tentative versions require richer tokens. |
| C2 | body | NEW-REASON | BODY | sync | L1322-1325 | 230 | C2,C3 | Position libraries irrevocably assign order on typing. |
| C2 | body | NEW-REASON | BODY | sync | L1339-1341 | 172 | C2 | Committed-only versions permit one integer per domain. |
| C2 | body | CARRIED | BODY | sync | L1407-1410 | 291 | C2,E3 | Q3: synchronization status travels in response headers. |
| C2 | body | CARRIED | BODY | sync | L1504-1506 | 185 | C2 | Q3: expected identity versions address read lag. |
| C2 | body | NEW-REASON | BODY | sync | L1675-1677 | 202 | C2,P0 | Pruning requires a formerly derivable distance field. |
| C2 | body | NEW-REASON | BODY | sync | L1723-1724 | 103 | C2,P0 | Pruning changes what metadata remains derivable. |
| C2 | body | NEW-REASON | BODY | sync | L2192-2195 | 253 | C2,C4 | Version-set identity is order-independent. |
| C2 | body | NEW-CASE | BODY | sync | L2598-2599 | 132 | C2,C3 | Global order encountered availability and serialization ceilings. |
| C2 | body | NEW-REASON | BODY | sync | L2601-2602 | 156 | C2 | Frontiers need shared history; vectors cost space. |
| C2 | body | NEW-REASON | BODY | sync | L2603-2604 | 132 | C2 | Tentative states need richer coordinates. |
| C2 | body | NEW-REASON | BODY | sync | L2605-2605 | 58 | C2 | Entire screen shares one cut. |
| C2 | body | CARRIED | BODY | sync | L2705-2717 | 928 | C2 | Q3: checkpoint, lag, generations, and session progress qualify reads. |
| C2 | body | CARRIED | BODY | sync | L3043-3044 | 143 | C2 | Q3: frontier progress relates to recomputation. |
| C2 | body | NEW-REASON | BODY | sync | L3045-3046 | 90 | C2,C3 | Per-cell SQL versions provide client positions. |
| C2 | body | NEW-REASON | BODY | sync | L3203-3205 | 219 | C2,E3 | Cross-store cuts map positions; clocks cannot establish universal order. |
| C3 | main | CARRIED | SHORT | datalog | L41-42 | 641 | C3,C2 | Q7: database scope determines total order and its write ceiling. |
| C3 | main | ABOVE | ABOVE | datalog | L594-595 | 991 | C3 | Planetary scope exceeds lived database limits; meaningful ordering units remain unresolved. |
| C3 | main | ABOVE | ABOVE | datalog | L619-619 | 334 | C3,C2 | Choose the meaningful timeline unit before choosing as-of representation. |
| C3 | main | NEW-REASON | R2 | datalog | L771-774 | 313 | C3,C8 | Partitioned-store and verified-actor proposals lack this camp's lived evidence. |
| C3 | main | CARRIED | R2 | datalog | L787-787 | 77 | C3 | Q7: one saying has one checked layer. |
| C3 | main | CARRIED | R2 | datalog | L814-815 | 568 | C3 | Q7: atomic sayings occupy one layer; cross-layer acts split into causally linked sayings. |
| C3 | main | CARRIED | R2 | datalog | L835-835 | 165 | C3 | Q7: multi-entity sayings require an atomic ordering unit larger than one entity. |
| C3 | main | CARRIED | R2 | datalog | L881-882 | 138 | C3,C2 | Q7: entity partitioning makes pattern-read bases multi-position cuts. |
| C3 | main | CARRIED | R2 | datalog | L883-884 | 330 | C3 | Q7: total-order scope determines atomic admission and consistent reading. |
| C3 | main | CARRIED | R2 | datalog | L887-887 | 222 | C3,E1 | Q7: entity ordering loses multi-entity atomicity and imports policy dependencies. |
| C3 | main | CARRIED | R2 | datalog | L888-888 | 232 | C3,C2 | Q7: layer ordering gives local atomicity and one foreign policy position. |
| C3 | main | CARRIED | R2 | datalog | L889-889 | 309 | C3 | Q7: owner ordering combines personal layers but mixes durable and firehose traffic. |
| C3 | main | CARRIED | R2 | datalog | L890-890 | 426 | C3 | Q7: hot shared problems hit throughput ceilings and require semantic subdivision. |
| C3 | main | CARRIED | R2 | datalog | L891-892 | 331 | C3 | Q7: a shared base exceeds one ordered unit and requires finer divisions. |
| C3 | main | CARRIED | R2 | datalog | L893-894 | 109 | C3 | Q7: proposes owner-recoverable layer units and multiple base units. |
| C3 | main | CARRIED | R2 | datalog | L898-899 | 148 | C3,C2 | Q7: base-wide reads require cuts spanning multiple units. |
| C3 | main | NEW-REASON | R2 | datalog | L904-905 | 319 | C3 | Nubank's isolated databases do not establish shared-base behavior. |
| C3 | main | CARRIED | R2 | datalog | L978-979 | 208 | C3 | Q7: entity partitioning spreads private layers and prevents local atomic sayings. |
| C3 | main | ABOVE | R2 | datalog | L1018-1018 | 141 | C3,C2 | Planetary ordering uses meaningful units, named cuts, and recorded ownership. |
| C3 | main | DISAGREES | ABOVE | frontiers | L423-424 | 382 | C3,A3 | Drops version checks for monotone keys; ABOVE separates timestamp authority from checking. |
| C3 | main | DISAGREES | ABOVE | frontiers | L436-437 | 105 | C3,A3 | Makes coordination depend on growing-set versus register semantics; ABOVE rejects layer-level framing. |
| C3 | main | DISAGREES | R2 | frontiers | L559-559 | 762 | C3,A3,E1 | Replaces refusal-based CAS with repair or accumulating keys; NEW-CASE: hot-cell refusal volume. |
| C3 | main | ABOVE | ABOVE | log | L546-547 | 1011 | C3 | Planetary store faces remote-write latency and concentrated operator trust despite partitioned ordering. |
| C3 | main | ABOVE | ABOVE | log | L561-561 | 189 | C3 | Rejects treating a layer namespace as the ordered unit. |
| C3 | main | CARRIED | R2 | log | L689-690 | 228 | C3 | Q7: ordering by layer makes the layer itself the uniquely homed key. |
| C3 | main | CARRIED | R2 | log | L691-692 | 427 | C3 | Q7: base-layer partitioning centralizes throughput, latency, and failure exposure. |
| C3 | main | DISAGREES | R2 | log | L693-694 | 751 | C3,C2 | Requires partition ids in cuts; also Q7 on session entities for ordered gestures. |
| C3 | main | ABOVE | ABOVE | meaning | L2666-2668 | 106 | C3,A3 | One writer is only a bounded per-unit caveat. |
| C3 | main | ABOVE | ABOVE | rama | L453-454 | 570 | C3 | Planetary unity creates a shared failure domain without global order. |
| C3 | main | CARRIED | R2 | rama | L594-595 | 823 | C3 | Q7; skewed keys require spreading work and possibly stored placement state. |
| C3 | main | CARRIED | R2 | rama | L596-597 | 234 | C3 | Q7; spreading hot entities retains cell order while giving up whole-entity order. |
| C3 | main | NEW-CASE | R2 | rama | L598-599 | 448 | C3 | A hot shared canvas throttles unrelated entities sharing its task. |
| C3 | main | CARRIED | R2 | rama | L600-601 | 375 | C3 | Q7; preserve cell-level guarantees and ensure appenders know changing placement. |
| C3 | main | CARRIED | R2 | rama | L604-605 | 148 | C3,E4 | Q7; version checks are local, while grammar and policy placement remain unresolved. |
| C3 | main | CARRIED | R2 | rama | L607-607 | 474 | C3,E4 | Q7; remote checks require two hops and may become stale before the local write. |
| C3 | main | CARRIED | R2 | rama | L608-608 | 236 | C3,X3 | Q7; layer-local policies make overlay resolution cross partitions. |
| C3 | main | CARRIED | R2 | rama | L609-609 | 129 | C3 | Q7; microbatch supplies cross-partition transaction scope despite read hops. |
| C3 | main | CARRIED | R2 | rama | L613-614 | 173 | C3 | Q7; replicate small metadata and colocate larger data. |
| C3 | main | ABOVE | R2 | rama | L625-625 | 118 | C3 | One stalled task group halts all module microbatch topologies. |
| C3 | main | ABOVE | R2 | rama | L627-627 | 122 | C3 | Placement labels target modules rather than individual partitions. |
| C3 | main | CARRIED | R2 | rama | L629-629 | 38 | C3 | Q5; task count is fixed at launch. |
| C3 | main | ABOVE | R2 | rama | L638-638 | 151 | C3 | Questions regional replica placement and preferred leaders. |
| C3 | main | ABOVE | R2 | rama | L640-640 | 115 | C3 | Questions whether a regional outage stops every microbatch topology. |
| C3 | main | CARRIED | R2 | rama | L643-643 | 109 | C3,C2 | Q5; asks how task scaling preserves, remaps, or translates positions. |
| C3 | main | ABOVE | ABOVE | skeptics | L279-279 | 564 | C3,C5 | Challenges planetary scope through operations, locality, cross-shard atomicity, and erasure boundaries. |
| C3 | main | ABOVE | ABOVE | sync | L3152-3154 | 162 | C3,C2 | Ask for compare-and-set and snapshot domains. |
| C3 | main | CARRIED | R2 | sync | L3370-3374 | 286 | C3 | Q7: personal repositories separate local and shared authority. |
| C3 | main | DISAGREES | R2 | sync | L3534-3540 | 404 | C3,E4 | Gate may check only grants in layers it orders. |
| C3 | main | CARRIED | R2 | sync | L3597-3601 | 308 | C3 | Q7: cross-cell invariants and registry cells determine partition sharing. |
| C3 | body | CARRIED | BODY | datalog | L62-62 | 166 | C3,C2 | Q7: one database is one serialized order and basis. |
| C3 | body | CARRIED | BODY | datalog | L105-108 | 300 | C3 | Q7: serialization can avoid overlap-detection costs. |
| C3 | body | ABOVE | BODY | datalog | L109-114 | 630 | C3 | Rejects unlimited write scalability as Datomic's scope. |
| C3 | body | CARRIED | BODY | datalog | L115-118 | 295 | C3 | Q7: independent shards lose cross-shard query, transactions, and consistency. |
| C3 | body | CARRIED | BODY | datalog | L252-252 | 285 | C3,C2 | Q7: total database order makes a gap-free numeric basis possible. |
| C3 | body | CARRIED | BODY | datalog | L255-255 | 552 | C3,E1,X2 | Q7: admission dependencies need a shared order or recorded foreign positions. |
| C3 | body | CARRIED | BODY | datalog | L256-256 | 767 | C3,C2 | Q7: choose meaningful ordered units before choosing the shape of as-of. |
| C3 | body | CARRIED | BODY | datalog | L257-258 | 632 | C3,C2 | Q7: layer ordering shortens cuts but leaves shared-base capacity unresolved. |
| C3 | body | ABOVE | BODY | datalog | L343-343 | 189 | C3 | Machine-rate agents exceed the single-box scope assumption. |
| C3 | body | NEW-REASON | BODY | datalog | L344-344 | 167 | C3,C2 | Ten-billion-datom guidance constrains large shared fields and crossing histories. |
| C3 | body | CARRIED | BODY | datalog | L353-353 | 142 | C3 | Q7: partitioned logs lose guarantees available inside a database. |
| C3 | body | CARRIED | BODY | datalog | L367-370 | 559 | C3 | Q7: services and customer slices become separate databases at large scale. |
| C3 | body | CARRIED | BODY | datalog | L377-380 | 422 | C3 | Q7: customer partitions become complete infrastructure copies. |
| C3 | body | CARRIED | BODY | datalog | L381-382 | 247 | C3 | Q7: transactors, services, and microservice clusters shard together. |
| C3 | body | CARRIED | BODY | datalog | L383-384 | 221 | C3 | Q7: operational guidance limits each transactor to one primary database. |
| C3 | body | CARRIED | BODY | datalog | L397-398 | 468 | C3 | Q7: customer counts across fragmented databases required separate analytical ETL. |
| C3 | body | CARRIED | BODY | datalog | L401-401 | 195 | C3 | Q7: database order follows service and customer boundaries. |
| C3 | body | CARRIED | BODY | datalog | L413-418 | 274 | C3 | Q7: single-box saturation and cross-database analytics constrain shard-above scaling. |
| C3 | body | CARRIED | BODY | datalog | L451-456 | 286 | C3 | Q7: strict database ordering requires exactly one Kafka partition. |
| C3 | body | CARRIED | BODY | datalog | L457-458 | 310 | C3 | Q7: bounded write throughput buys precise database-wide change guarantees. |
| C3 | body | NEW-REASON | BODY | datalog | L459-460 | 186 | C3 | Reported local single-thread throughput reaches 300,000 documents per second. |
| C3 | body | CARRIED | BODY | datalog | L488-488 | 117 | C3 | Q7: each database enforces one partition and a write ceiling. |
| C3 | body | CARRIED | BODY | datalog | L524-525 | 211 | C3 | Q7: one Postgres order feeds reactive invalidation. |
| C3 | body | CARRIED | BODY | frontiers | L78-78 | 882 | C3,A3 | Q7: atomic multi-shard writes use one coordinating shard, imposing contention and scale limits. |
| C3 | body | CARRIED | BODY | frontiers | L86-86 | 255 | C3 | Q7: sharing a time source, rather than physical co-location, supplies cross-partition order. |
| C3 | body | NEW-REASON | BODY | frontiers | L176-177 | 514 | C3,A3 | Coordinate only non-monotone logic, with time and order represented as data. |
| C3 | body | NEW-REASON | BODY | frontiers | L180-181 | 726 | C3,A3,C2 | Monotonicity determines whether missing information requires coordination. |
| C3 | body | CARRIED | BODY | frontiers | L188-189 | 723 | C3 | Q7: per-partition sealing permits more asynchrony than a shared total order. |
| C3 | body | NEW-REASON | BODY | frontiers | L192-193 | 541 | C3 | Causal ordering is required specifically for non-monotone derivations. |
| C3 | body | NEW-REASON | BODY | frontiers | L205-205 | 544 | C3,C2,X3 | Current values, nearest layers, and revocable policies require coordinated absence reads. |
| C3 | body | DISAGREES | BODY | frontiers | L206-206 | 445 | C3,A3 | Rejects universal cell compare-and-set as unnecessary for accumulating keys. |
| C3 | body | CARRIED | BODY | frontiers | L207-207 | 229 | C3 | Q7: co-locate the cell and everything its non-monotone admission decision reads. |
| C3 | body | NEW-REASON | BODY | frontiers | L223-224 | 381 | C3,A3 | Coordination cost motivates selective guarantees rather than strong consistency everywhere. |
| C3 | body | DISAGREES | BODY | frontiers | L322-323 | 504 | C3,A3 | Prefers minimizing conflict avoidance and repairing conflicts after changes. |
| C3 | body | DISAGREES | BODY | frontiers | L333-333 | 279 | C3,A3 | Proposes repairing machine-rate conflicts instead of compare-and-set refusals and retries. |
| C3 | body | DISAGREES | BODY | frontiers | L343-344 | 109 | C3,A3 | Opposes centering transaction processing on conflict avoidance. |
| C3 | body | DISAGREES | BODY | frontiers | L399-400 | 306 | C3,A3 | Calls gate coordination waste where monotonicity permits avoiding it. |
| C3 | body | DISAGREES | BODY | frontiers | L446-446 | 150 | C3 | Accumulating cells merge without coordination; only current-value cells require a single owner. |
| C3 | body | CARRIED | BODY | log | L67-68 | 397 | C3,C1 | Q7: consistency belongs to a uniquely keyed entity. |
| C3 | body | CARRIED | BODY | log | L76-76 | 389 | C3 | Q7: only a shared unique key guarantees transactional scope. |
| C3 | body | CARRIED | BODY | log | L82-83 | 1143 | C3,C2 | Q5,Q7: repartitioning moves new versions while old versions remain in their original logs. |
| C3 | body | CARRIED | BODY | log | L90-90 | 116 | C3 | Q7: only one key guarantees shared ordering and placement. |
| C3 | body | NEW-REASON | BODY | log | L113-113 | 305 | C3,W3 | Regional partitions can make a system-wide total order unavailable. |
| C3 | body | NEW-REASON | BODY | log | L123-123 | 113 | C3,W3 | CORFU's author later judged global total ordering typically unnecessary. |
| C3 | body | NEW-CASE | BODY | log | L125-125 | 327 | C3 | One inaccessible log slot blocks learners across unrelated shards. |
| C3 | body | CARRIED | BODY | log | L159-159 | 150 | C3,A3 | Q7: stream ordering and expected-version checks jointly control concurrency. |
| C3 | body | CARRIED | BODY | log | L177-177 | 135 | C3 | Q7: mutual ordering requires one stream or application merging. |
| C3 | body | CARRIED | BODY | log | L184-185 | 471 | C3 | Q7: business-event streams are substantially coarser ordering units than cells. |
| C3 | body | NEW-REASON | BODY | log | L198-198 | 286 | C3,W3 | Total ordering requires network coordination and cannot serve disconnected writers. |
| C3 | body | CARRIED | BODY | log | L216-216 | 377 | C3 | Q7: shared claims require colocated ordering; multi-entity events break per-partition processing. |
| C3 | body | NEW-REASON | BODY | log | L296-296 | 208 | C3,A3 | Longer certification delays enlarge conflict zones and increase abort probability. |
| C3 | body | CARRIED | BODY | log | L299-300 | 632 | C3,C2 | Q7: proposed multiple logs share ordering only for overlapping partitions; cross-log positions are incomparable. |
| C3 | body | CARRIED | BODY | log | L303-303 | 234 | C3,C2 | Q7: multiple certifiers require shared ordering for overlapping work and vector cuts elsewhere. |
| C3 | body | NEW-CASE | BODY | log | L305-305 | 514 | C3 | Remote agents repeatedly collide on hot cells; finer semantic cells reduce contention. |
| C3 | body | NEW-CASE | BODY | log | L325-325 | 452 | C3 | Multi-master page-level conflicts coupled unrelated rows; retirement cause remains unspecified. |
| C3 | body | CARRIED | BODY | log | L350-350 | 107 | C3 | Q7: Rama guarantees only local ordering, with none across depots. |
| C3 | body | CARRIED | BODY | log | L429-430 | 391 | C3 | Q7: keys, streams, and accessed partitions determine ordering scope. |
| C3 | body | CARRIED | BODY | log | L433-434 | 444 | C3 | Q7,Q5: CAS scope and gesture ordering constrain placement; repartitioning leaves history behind. |
| C3 | body | CARRIED | BODY | log | L532-533 | 256 | C3,C5 | Q7,Q10: keyed order, immutable pointers, and deletion without substituted content. |
| C3 | body | CARRIED | BODY | meaning | L898-905 | 528 | C3 | Q7: Single-threaded object throughput forces a hot counter into multiple objects. |
| C3 | body | CARRIED | BODY | meaning | L906-908 | 168 | C3 | Q7: Cross-object queries and transactions require additional coordination. |
| C3 | body | DISAGREES | BODY | meaning | L915-922 | 500 | C3,C2 | Requires small ordering units and explicit per-unit positions absent global sequencing. |
| C3 | body | NEW-REASON | BODY | meaning | L1579-1591 | 903 | C3,E3 | Croquet converged on per-session ordering and simulation timestamps. |
| C3 | body | ABOVE | BODY | meaning | L1614-1621 | 518 | C3 | Rejects a universal total order for open systems. |
| C3 | body | NEW-CASE | BODY | meaning | L1856-1859 | 258 | C3 | Parallel execution removed ordering guarantees previously supplied by one thread. |
| C3 | body | NEW-REASON | BODY | meaning | L2010-2015 | 376 | C3 | Operation-based schema migration requires an agreed conflict-resolution order. |
| C3 | body | CARRIED | BODY | meaning | L2341-2351 | 709 | C3 | Q7: Ordered units trade local throughput against cross-unit coordination. |
| C3 | body | CARRIED | BODY | rama | L72-73 | 302 | C3 | Q7; partitioner-selected record component fixes ordering companions. |
| C3 | body | CARRIED | BODY | rama | L132-132 | 132 | C3 | Q7; a task executes actions serially. |
| C3 | body | CARRIED | BODY | rama | L133-133 | 222 | C3,E1 | Q7; stream atomicity is local, microbatch atomicity spans partitions. |
| C3 | body | CARRIED | BODY | rama | L134-134 | 145 | C3 | Q7; separate partitions process independently. |
| C3 | body | CARRIED | BODY | rama | L135-135 | 197 | C3 | Q7; shared ordering or entities motivates one depot. |
| C3 | body | CARRIED | BODY | rama | L143-144 | 571 | C3,E1,E4 | Q7; atomic stream admission requires colocated writes and decision inputs. |
| C3 | body | CARRIED | BODY | rama | L229-229 | 146 | C3,E1 | Q7; atomic scope differs between one task event and all partitions. |
| C3 | body | NEW-REASON | BODY | rama | L234-234 | 153 | C3 | One stalled task group stops all microbatch topologies in its module. |
| C3 | body | CARRIED | BODY | rama | L245-246 | 980 | C3,W1 | Q7; colocating by entity trades local checks against hot entities and reverse-query fanout. |
| C3 | body | CARRIED | BODY | rama | L293-294 | 1135 | C3,W1 | Q5; repartition follows replay-into-new-module and client-switch migration. |
| C3 | body | NEW-REASON | BODY | rama | L377-378 | 477 | C3,W3 | Independent writers weaken the meaning of a desired global order. |
| C3 | body | NEW-REASON | BODY | rama | L392-392 | 229 | C3 | Streaming can provide semantic guarantees comparable to batch processing. |
| C3 | body | CARRIED | BODY | skeptics | L46-47 | 1453 | C3,C2,P0 | Q7,Q1: entity partitions, consumer offsets, and the database-to-log atomicity seam. |
| C3 | body | ABOVE | BODY | skeptics | L109-110 | 128 | C3 | Challenges planetary distribution with scale-up-first and later personal sharding. |
| C3 | body | NEW-CASE | BODY | skeptics | L185-186 | 425 | C3,X1 | One in 1,500 batched reads exposed partial transactional updates before atomic visibility was added. |
| C3 | body | CARRIED | BODY | skeptics | L192-193 | 313 | C3,X1 | Q7: cross-partition fact chains can land partially and require detection or repair. |
| C3 | body | NEW-REASON | BODY | skeptics | L305-305 | 176 | C3,A3 | Independent stores writing one cell reintroduce conflict reconciliation and version-vector costs. |
| C3 | body | CARRIED | BODY | sync | L555-558 | 269 | C3,C2 | Q7: file ordering domain; global order couples availability. |
| C3 | body | NEW-REASON | BODY | sync | L676-685 | 750 | C3,C2,C5 | Row versions ease authorization and deletion but increase read cost. |
| C3 | body | CARRIED | BODY | sync | L771-774 | 217 | C3,X4 | Q7: separately permissioned comments use another partition. |
| C3 | body | NEW-REASON | BODY | sync | L1058-1062 | 319 | C3,E3 | Canonical order need not reflect objective temporal precedence. |
| C3 | body | CARRIED | BODY | sync | L1415-1422 | 600 | C3 | Q7: owner-separated records avoid cross-user write coordination. |
| C3 | body | CARRIED | BODY | sync | L1516-1518 | 176 | C3 | Q7: per-repository writers require global indexing. |
| C3 | body | CARRIED | BODY | sync | L2052-2061 | 648 | C3,C2 | Q7: single-primary ceiling pushes sharding to applications. |
| C3 | body | CARRIED | BODY | sync | L2595-2597 | 225 | C3 | Q7: ordering domains range from files to databases. |
| C3 | body | CARRIED | BODY | sync | L2608-2610 | 107 | C3 | Q7: owner partitions avoid shared primary writes. |
| C4 | main | DISAGREES | SHORT | datalog | L39-40 | 446 | C4 | DataScript uses words directly; incompatible value types require new keys. |
| C4 | main | CARRIED | ABOVE | datalog | L623-624 | 146 | C4 | Q8: id in the attribute slot, word as a fact about it. |
| C4 | main | NEW-CASE | R2 | datalog | L906-909 | 645 | C4,X2 | Current cardinality can hide historical affiliations in as-of reads. |
| C4 | main | NEW-CASE | R2 | datalog | L921-922 | 465 | C4,X2 | Historical multi-affiliation views are corrupted by later single-valued grammar. |
| C4 | main | NEW-CASE | R2 | datalog | L957-959 | 828 | C4 | Bootstrap schema grew through explicit idempotent upgrades rather than a one-time seed. |
| C4 | main | NEW-REASON | R2 | datalog | L960-961 | 554 | C4,C1 | Self-naming bootstrap facts prevent hashing their own complete content; namespaced words avoid the cycle. |
| C4 | main | NEW-REASON | R2 | datalog | L962-963 | 937 | C4 | Key indirection permits renames, but readers need an available name table. |
| C4 | main | ABOVE | R2 | datalog | L1005-1005 | 153 | C4 | Ask how the seed grows through compatible editions. |
| C4 | main | NEW-CASE | R2 | frontiers | L563-564 | 279 | C4 | Repointing relation silently changes every standing pattern that stores the word. |
| C4 | main | ABOVE | ABOVE | log | L544-545 | 525 | C4 | Trust anchor, successor pointer, and encoding must remain outside the shared fact store. |
| C4 | main | DISAGREES | ABOVE | meaning | L2621-2633 | 948 | C4 | All first facts must match across stores; also challenges central power. |
| C4 | main | ABOVE | ABOVE | meaning | L2677-2678 | 149 | C4 | Word-versus-id framing omits three distinct naming jobs. |
| C4 | main | DISAGREES | R2 | meaning | L3107-3113 | 404 | C4,C1,C6 | Value kinds never change, sayings use hashes, and predecessor references stay numeric. |
| C4 | main | ABOVE | ABOVE | rama | L451-452 | 780 | C4,X2,C3 | Stored grammars and policies raise permanent-reader, colocation, and correction questions. |
| C4 | main | DISAGREES | R2 | rama | L611-612 | 204 | C4,X2 | Grammars live in module code rather than versioned facts; also ABOVE. |
| C4 | main | NEW-REASON | R2 | rama | L661-662 | 168 | C4 | Stable key ids require grammar resolution on every offer. |
| C4 | main | NEW-REASON | R2 | skeptics | L424-424 | 337 | C4 | Scoped word uniqueness preserves informative collisions that opaque key ids otherwise hide. |
| C4 | main | NEW-REASON | R2 | sync | L3494-3508 | 1100 | C4 | Opaque-key exports need definitions and cross-store naming alignment. |
| C4 | main | DISAGREES | R2 | sync | L3571-3581 | 722 | C4 | Assumes gate seed shared across stores. |
| C4 | main | ABOVE | R2 | sync | L3650-3651 | 109 | C4 | Shared genesis needs constants rather than computation. |
| C4 | body | CARRIED | BODY | datalog | L64-64 | 166 | C4 | Q8: attributes are entities with naming facts. |
| C4 | body | NEW-REASON | BODY | datalog | L135-144 | 793 | C4 | Names retain established meanings; incompatible variants receive new names. |
| C4 | body | NEW-CASE | BODY | datalog | L169-170 | 562 | C4 | Schema alteration became necessary; value types remained unalterable. |
| C4 | body | NEW-REASON | BODY | datalog | L171-172 | 490 | C4,X2 | Historical reads use current schema because physical indexes retain only one schema. |
| C4 | body | DISAGREES | BODY | datalog | L204-204 | 517 | C4,C7 | Gate identity is constant across stores; bootstrap stamps use a sentinel. |
| C4 | body | CARRIED | BODY | datalog | L209-209 | 333 | C4 | Q8: idents resolve to attribute entities whose schema can be annotated. |
| C4 | body | NEW-REASON | BODY | datalog | L210-210 | 291 | C4 | Always-resident ident tables constrain names to schema and enums. |
| C4 | body | NEW-REASON | BODY | datalog | L211-211 | 240 | C4 | Old names remain aliases after renaming. |
| C4 | body | NEW-REASON | BODY | datalog | L212-212 | 177 | C4 | Namespaced names are global; changed meanings require new names. |
| C4 | body | NEW-REASON | BODY | datalog | L213-213 | 451 | C4 | Grammar versioning remains safe only when new versions preserve earlier contracts. |
| C4 | body | DISAGREES | BODY | datalog | L215-215 | 162 | C4 | DataScript stores plain words directly as attributes. |
| C4 | body | DISAGREES | BODY | datalog | L216-217 | 276 | C4 | Datomic attribute ids are local; names cross stores instead. |
| C4 | body | CARRIED | BODY | datalog | L334-334 | 61 | C4 | Q8: schema and stored code are ordinary facts. |
| C4 | body | ABOVE | BODY | datalog | L341-341 | 197 | C4 | Closed-world design explicitly excludes planetary shared semantics. |
| C4 | body | DISAGREES | BODY | datalog | L355-355 | 75 | C4,P0 | DataScript chooses literal attribute words and discards history. |
| C4 | body | CARRIED | BODY | datalog | L516-517 | 625 | C4 | Q8: attribute UUIDs and separate ident records support renaming. |
| C4 | body | NEW-REASON | BODY | datalog | L518-519 | 321 | C4,C1 | Client-generated attribute ids allow offline schema creation. |
| C4 | body | DISAGREES | BODY | datalog | L530-531 | 509 | C4,P0 | Attributes are literal words; retractions erase history to maintain constant space. |
| C4 | body | DISAGREES | BODY | datalog | L532-533 | 207 | C4 | Defends word keys for small single-process stores without renames. |
| C4 | body | NEW-REASON | BODY | datalog | L544-545 | 551 | C4,E6 | Restricting subscription patterns enables reverse matching and requires indexable keys. |
| C4 | body | CARRIED | BODY | datalog | L556-557 | 136 | C4 | Q8: Datalevin stores attributes as integer ids. |
| C4 | body | DISAGREES | BODY | frontiers | L316-316 | 187 | C4 | Prefers globally qualified words as keys. |
| C4 | body | DISAGREES | BODY | frontiers | L331-331 | 406 | C4,E6 | Prefers word keys; opaque ids introduce time-varying name lookups requiring recorded reads. |
| C4 | body | DISAGREES | BODY | frontiers | L357-358 | 236 | C4 | Preserves a vote for word keys while requiring recorded name resolution at registration. |
| C4 | body | DISAGREES | BODY | frontiers | L447-448 | 179 | C4 | Assigns the same gate id in every store. |
| C4 | body | ABOVE | BODY | log | L141-141 | 132 | C4 | Bootstrap needs both a fixed object id and an external current-log register. |
| C4 | body | DISAGREES | BODY | log | L151-152 | 428 | C4 | Event types are names rather than entity ids. |
| C4 | body | NEW-REASON | BODY | log | L155-155 | 214 | C4,C7 | A new version must be convertible from the old; otherwise it is a new event. |
| C4 | body | DISAGREES | BODY | log | L156-156 | 314 | C4 | Word keys cannot be renamed; compatibility requires retaining old names indefinitely. |
| C4 | body | NEW-REASON | BODY | log | L157-157 | 213 | C4 | Downstream consumers cannot safely interpret changed semantic meanings. |
| C4 | body | DISAGREES | BODY | log | L162-163 | 433 | C4,C1 | Type is a friendly word; retry identity is stream-scoped and fails under unrestricted expected versions. |
| C4 | body | NEW-REASON | BODY | log | L174-174 | 258 | C4 | Changed meaning requires a new key; stable ids free display names from permanent spelling. |
| C4 | body | ABOVE | BODY | log | L390-391 | 573 | C4 | Bootstrap requires external encoding, hash rules, trust anchor, and successor pointer. |
| C4 | body | DISAGREES | BODY | log | L394-395 | 842 | C4 | Includes friendly-name type keys rather than ids; versioned-schema pointers offer an alternative. |
| C4 | body | NEW-REASON | BODY | log | L396-397 | 640 | C4,X2 | Id-keyed records need portable dictionaries; semantic changes require new keys. |
| C4 | body | NEW-REASON | BODY | log | L575-576 | 226 | C4 | Identical genesis content prevents inter-store translation, beyond merely sharing ids. |
| C4 | body | NEW-REASON | BODY | meaning | L179-185 | 196 | C4,C7 | Numbers require representational context. |
| C4 | body | NEW-REASON | BODY | meaning | L757-763 | 487 | C4,X3 | Layered names distinguish public nicknames from private petnames. |
| C4 | body | NEW-CASE | BODY | meaning | L823-830 | 581 | C4 | Reused protobuf tags can leak private data or corrupt persisted messages. |
| C4 | body | NEW-REASON | BODY | meaning | L1197-1202 | 365 | C4 | Value-kind changes require fresh keys; grammar versioning is bounded by compatibility. |
| C4 | body | ABOVE | BODY | meaning | L1203-1212 | 687 | C4,X2 | Constraints as facts swelled until checks crashed; validation occurred after admission. |
| C4 | body | ABOVE | BODY | meaning | L1240-1249 | 640 | C4 | Open type statements produced widespread classification errors. |
| C4 | body | NEW-REASON | BODY | meaning | L1368-1373 | 367 | C4,X4 | A setting publication supplies trust roots and controls which records load. |
| C4 | body | NEW-REASON | BODY | meaning | L1374-1380 | 421 | C4 | English-like predicate names invite unintended meanings. |
| C4 | body | NEW-CASE | BODY | meaning | L1381-1386 | 433 | C4,X3 | Untrusted same-as links propagate assertions; merge authority needs layer scope. |
| C4 | body | NEW-CASE | BODY | meaning | L1390-1393 | 252 | C4 | Schema.org retained both HTTP and HTTPS predicate namespaces indefinitely. |
| C4 | body | NEW-REASON | BODY | meaning | L1463-1473 | 780 | C4,X4 | Self-description bootstraps through one explicit hand-cut circle. |
| C4 | body | NEW-REASON | BODY | meaning | L1622-1633 | 596 | C4 | Names lose meaning when their context is removed. |
| C4 | body | NEW-REASON | BODY | meaning | L1985-1993 | 613 | C4 | Stable field and element ids extend naming beneath the outer key. |
| C4 | body | NEW-CASE | BODY | meaning | L1994-2003 | 641 | C4 | Copying schema states cannot distinguish move-and-rename from delete-and-insert. |
| C4 | body | NEW-REASON | BODY | meaning | L2207-2217 | 637 | C4,X4 | Self-description and self-governing roots supply bootstrap precedents. |
| C4 | body | CARRIED | BODY | meaning | L2229-2239 | 644 | C4 | Q8: Attributes are entities with renameable idents; names remain separate from identity. |
| C4 | body | DISAGREES | BODY | meaning | L2240-2245 | 388 | C4 | Qualified words compete with ids; opaque names impose usability and drift costs. |
| C4 | body | NEW-REASON | BODY | rama | L76-78 | 237 | C4 | Initial PState values are code constants; no genesis hook exists. |
| C4 | body | NEW-REASON | BODY | rama | L79-80 | 132 | C4 | New topologies may replay initial appends from the beginning. |
| C4 | body | NEW-REASON | BODY | rama | L311-312 | 158 | C4 | Initial rules live in code in this camp's practice. |
| C4 | body | ABOVE | BODY | rama | L345-345 | 182 | C4,X2 | Code-deployed schemas differ from permanently coexisting grammar, policy, and tool facts. |
| C4 | body | DISAGREES | BODY | skeptics | L34-35 | 320 | C4 | Seed identities may vary between environments instead of using universal first-key constants. |
| C4 | body | DISAGREES | BODY | skeptics | L36-37 | 925 | C4,X2 | Words remain key identities; also NEW-REASON on non-transitive schema compatibility. |
| C4 | body | ABOVE | BODY | skeptics | L92-93 | 1154 | C4 | Questions schema-last and new constructs; also NEW-REASON on semantic heterogeneity. |
| C4 | body | NEW-REASON | BODY | skeptics | L106-106 | 398 | C4,X2 | Grammar checks satisfy schema-first; independently coined keys still create semantic heterogeneity. |
| C4 | body | DISAGREES | BODY | skeptics | L234-234 | 90 | C4 | Seed ids vary by environment instead of using shared first-key constants. |
| C4 | body | DISAGREES | BODY | skeptics | L235-235 | 169 | C4 | Words serve as key identities; also NEW-REASON on local semantic drift. |
| C4 | body | NEW-REASON | BODY | sync | L243-245 | 103 | C4,E8 | Server creation is an ordinary write with bootstrap exception. |
| C4 | body | DISAGREES | BODY | sync | L324-328 | 320 | C4,X2 | One current schema validates historical records retroactively. |
| C4 | body | DISAGREES | BODY | sync | L359-361 | 158 | C4,X2 | Single-schema enforcement retroactively constrains immutable facts. |
| C4 | body | NEW-CASE | BODY | sync | L887-891 | 330 | C4,C1 | Code changes alter content-derived genesis identity. |
| C4 | body | NEW-CASE | BODY | sync | L964-967 | 313 | C4,X2 | Write-time translation failed when future schemas appeared. |
| C4 | body | NEW-REASON | BODY | sync | L972-974 | 189 | C4,X2 | Read-time translation supports later-invented schemas. |
| C4 | body | NEW-REASON | BODY | sync | L976-980 | 348 | C4,X2 | Schema reconciliation cannot preserve every desired property. |
| C4 | body | NEW-REASON | BODY | sync | L983-988 | 338 | C4 | Deep engine integration blocked production support. |
| C4 | body | NEW-REASON | BODY | sync | L1093-1095 | 204 | C4,C1 | Reference-specific types permit different renderings of identical content. |
| C4 | body | NEW-REASON | BODY | sync | L1117-1120 | 302 | C4,X2 | Translators need engine support; some grammars cannot reconcile. |
| C4 | body | DISAGREES | BODY | sync | L1357-1364 | 509 | C4 | Schema identity uses words. |
| C4 | body | DISAGREES | BODY | sync | L1392-1400 | 721 | C4,X2 | Breaking changes require new schema words. |
| C4 | body | DISAGREES | BODY | sync | L1459-1464 | 444 | C4 | Word-key authority caused field collisions and software-dependent meanings. |
| C4 | body | DISAGREES | BODY | sync | L1496-1501 | 425 | C4,X2 | Word schemas lack pinned versions. |
| C4 | body | NEW-CASE | BODY | sync | L1522-1525 | 285 | C4 | Temporary genesis infrastructure became permanent. |
| C4 | body | NEW-CASE | BODY | sync | L1587-1593 | 508 | C4 | Registry accumulates permanent deprecated kind numbers. |
| C4 | body | NEW-REASON | BODY | sync | L1603-1607 | 326 | C4 | Kind ranges couple identity and retention. |
| C4 | body | DISAGREES | BODY | sync | L1641-1642 | 105 | C4 | Kinds are bare words. |
| C4 | body | DISAGREES | BODY | sync | L1689-1694 | 444 | C4 | Schema identity includes name and definition version. |
| C4 | body | DISAGREES | BODY | sync | L1711-1712 | 99 | C4 | Key combines name and grammar-version identity. |
| C4 | body | DISAGREES | BODY | sync | L2080-2090 | 825 | C4 | Column IDs regretted because independent minting breaks merges. |
| C4 | body | DISAGREES | BODY | sync | L2102-2113 | 779 | C4,P0 | Regrets column IDs and uses history-rewrite excision. |
| C4 | body | NEW-REASON | BODY | sync | L2153-2158 | 353 | C4,C7 | Single type forces blobs or giant unions. |
| C4 | body | NEW-CASE | BODY | sync | L2365-2369 | 335 | C4,C1 | Content-derived genesis freezes formats; code identity orphans state. |
| C4 | body | ABOVE | BODY | sync | L2370-2373 | 245 | C4 | Grammar language outside data avoids self-description regress. |
| C4 | body | NEW-REASON | BODY | sync | L2374-2375 | 106 | C4,E8 | Server creation has a bootstrap exception. |
| C4 | body | NEW-CASE | BODY | sync | L2376-2377 | 74 | C4 | Placeholder infrastructure becomes permanent. |
| C4 | body | DISAGREES | BODY | sync | L2398-2401 | 303 | C4,X2 | Word keys lack pinned grammar versions. |
| C4 | body | NEW-REASON | BODY | sync | L2402-2404 | 178 | C4 | Numeric kinds need permanent registry and encode retention. |
| C4 | body | DISAGREES | BODY | sync | L2405-2405 | 78 | C4 | Keys are bare words. |
| C4 | body | DISAGREES | BODY | sync | L2406-2408 | 162 | C4 | Column IDs regretted because independent branches must converge. |
| C4 | body | DISAGREES | BODY | sync | L2409-2409 | 79 | C4 | Schema identity combines word and definition version. |
| C4 | body | NEW-REASON | BODY | sync | L2410-2411 | 150 | C4,X2 | Read-time translation needs writer schema and translators. |
| C4 | body | DISAGREES | BODY | sync | L2414-2415 | 112 | C4,X2 | Current schema validates historical records. |
| C4 | body | NEW-REASON | BODY | sync | L2419-2421 | 98 | C4 | Single value type forces blobs or unions. |
| C4 | body | DISAGREES | BODY | sync | L2422-2424 | 117 | C4 | Word-key practice and ID regret oppose ledger choice. |
| C4 | body | NEW-REASON | BODY | sync | L2680-2682 | 221 | C4,E5 | Grammar distinguishes floating and pinned references. |
| C4 | body | NEW-REASON | BODY | sync | L2960-2961 | 74 | C4 | Retention behavior can belong to kind definitions. |
| C4 | body | CARRIED | BODY | sync | L3032-3035 | 260 | C4,C1,C5 | Q8: attributes and transactions as entities; excision. |
| C4 | body | NEW-REASON | BODY | sync | L3197-3199 | 208 | C4 | Cross-store key equivalence needs reconciliation facts. |
| C4 | body | NEW-REASON | BODY | sync | L3200-3202 | 208 | C4,X2 | Some grammar pairs cannot reconcile. |
| C4 | body | NEW-REASON | BODY | sync | L3212-3213 | 155 | C4,E8 | One store records another's birth. |
| C5 | main | CARRIED | SHORT | datalog | L43-44 | 692 | C5,P0 | Q10: excision failures, external payloads, replay costs, and an ephemeral log. |
| C5 | main | CARRIED | ABOVE | datalog | L609-609 | 255 | C5 | Q10: derived indexes need a checker proving erasure completed. |
| C5 | main | ABOVE | ABOVE | datalog | L620-620 | 235 | C5 | Distinguish retraction, forgetting, and never admitting the value. |
| C5 | main | CARRIED | R2 | datalog | L845-848 | 593 | C5 | Q10: key deletion replaces a large erasable payload store with a small mutable key store. |
| C5 | main | CARRIED | R2 | datalog | L853-853 | 437 | C5,C2 | Q10: erasure changes historical reads while the encrypted envelope remains. |
| C5 | main | CARRIED | R2 | datalog | L854-854 | 338 | C5 | Q10: destroyed keys need tombstones distinguishable from unreachable keys. |
| C5 | main | CARRIED | R2 | datalog | L855-855 | 433 | C5,C1 | Q10 and Q9: forgetting salts prevents both guessing and recognition of forbidden resubmission. |
| C5 | main | CARRIED | R2 | datalog | L856-856 | 414 | C5 | Q10: plaintext derived indexes still require verifiable purging. |
| C5 | main | CARRIED | R2 | datalog | L857-858 | 402 | C5 | Q10: crypto-shredding reaches log backups only if key-store backups are erased too. |
| C5 | main | NEW-REASON | R2 | datalog | L865-866 | 391 | C5,E1 | After erasure, admission verdicts alone attest the value's accepted shape. |
| C5 | main | CARRIED | R2 | datalog | L869-869 | 520 | C5 | Q10: key-store outages must not be mistaken for intentional erasure during rebuilding. |
| C5 | main | CARRIED | R2 | datalog | L870-870 | 512 | C5 | Q10: per-value keys form another system of record and must be durable before admission. |
| C5 | main | CARRIED | R2 | datalog | L873-874 | 432 | C5,E6 | Q10: downstream quotations retain separate keys; dependency walks locate copied private content. |
| C5 | main | CARRIED | R2 | datalog | L875-876 | 646 | C5,P0 | Q10: eviction, replication, and replay costs explain the move toward compaction. |
| C5 | main | CARRIED | R2 | datalog | L877-878 | 573 | C5,P0 | Q10: resolvable erasure markers do not solve unbounded full-log rebuilds. |
| C5 | main | ABOVE | ABOVE | frontiers | L419-419 | 531 | C5,W1,C3 | Questions planetary scope through linearization limits, mixed ownership, and per-person view costs. |
| C5 | main | NEW-REASON | R2 | frontiers | L552-553 | 620 | C5,E6 | Erased predicates prevent exact pattern-reader recovery; audits over-report possible readers. |
| C5 | main | NEW-REASON | R2 | frontiers | L560-560 | 366 | C5 | Owner-wrapped value keys avoid million-key erasure walks; plaintext indexes require revocation. |
| C5 | main | DISAGREES | R2 | log | L651-652 | 673 | C5 | A regulator may require ciphertext excision beyond encryption-key destruction. |
| C5 | main | NEW-REASON | R2 | log | L657-658 | 509 | C5,C8 | Opaque actors preserve attribution, but surviving associations may still identify people. |
| C5 | main | ABOVE | R2 | log | L737-737 | 492 | C5 | Key store adds mutable external authority alongside runtime, encoding, and trust anchors. |
| C5 | main | DISAGREES | R2 | log | L744-744 | 251 | C5 | Allows ciphertext excision beyond the ledger's two erasure roads; also key-restore protection. |
| C5 | main | CARRIED | R2 | meaning | L2998-3004 | 444 | C5,C6 | Q10: Ciphertext commitments avoid plaintext-linked deletion cascades. |
| C5 | main | CARRIED | R2 | meaning | L3005-3008 | 172 | C5 | Q10: Crypto-shredding remains unproven by this camp; Datomic used physical excision. |
| C5 | main | ABOVE | R2 | meaning | L3009-3012 | 308 | C5,P0 | Mutable key storage adds a second substance with incompatible archival obligations. |
| C5 | main | CARRIED | R2 | meaning | L3013-3018 | 406 | C5 | Q10: Erasure cannot recall exported plaintext; future cryptanalysis limits exported ciphertext safety. |
| C5 | main | DISAGREES | R2 | rama | L655-656 | 689 | C5,P0 | NEW-CASE: sensitive metadata requires operator excision beyond encrypted values and identity facts. |
| C5 | main | NEW-CASE | R2 | rama | L663-664 | 623 | C5 | CARRIED Q10; restoring backups resurrects destroyed encryption keys unless deletions are reapplied. |
| C5 | main | NEW-REASON | R2 | rama | L665-665 | 326 | C5 | Permanent intake must already be encrypted; grammar checks need colocated decryption keys. |
| C5 | main | NEW-REASON | R2 | rama | L666-667 | 249 | C5 | Cryptography protects depot and backup copies; primary row deletion is already mutable. |
| C5 | main | CARRIED | R2 | rama | L668-669 | 163 | C5 | Q10; camp offers implementation optimism but no operational key-erasure experience. |
| C5 | main | ABOVE | R2 | rama | L717-717 | 271 | C5 | Deletable key storage adds sensitive mutable state outside an all-facts model. |
| C5 | main | DISAGREES | R2 | skeptics | L388-389 | 547 | C5 | Rejects immutable personal ciphertext and hashes; also ABOVE and CARRIED Q10. |
| C5 | main | DISAGREES | R2 | skeptics | L390-391 | 829 | C5 | Later cipher breaks defeat key-deletion erasure; also NEW-CASE and CARRIED Q10. |
| C5 | main | DISAGREES | R2 | skeptics | L392-393 | 332 | C5 | Requires personal values outside the log rather than allowing ciphertext alone; also CARRIED Q10. |
| C5 | main | DISAGREES | R2 | skeptics | L420-420 | 39 | C5 | Rejects the erasure lean through T6's objection to personal ciphertext on the log. |
| C5 | main | ABOVE | R2 | skeptics | L434-434 | 109 | C5 | Reframes erasure around whether immutable records should contain personal data in any form. |
| C5 | main | ABOVE | R2 | skeptics | L440-440 | 120 | C5 | External values challenge one substance by restoring the dual-write seam; also CARRIED Q10. |
| C5 | main | DISAGREES | SHORT | sync | L66-74 | 609 | C5,C1 | Recommends unkeyed value hash; otherwise supports separate names and digests. |
| C5 | main | CARRIED | SHORT | sync | L75-80 | 381 | C5,P0 | Q10: separable payloads preserve verification during deletion. |
| C5 | main | CARRIED | R2 | sync | L3424-3426 | 181 | C5 | Q10: fixed-shape payload hole preserves envelope structure. |
| C5 | main | CARRIED | R2 | sync | L3427-3430 | 293 | C5 | Q10: omitted content remains verifiable; unrecording removes dependents. |
| C5 | main | DISAGREES | R2 | sync | L3446-3456 | 774 | C5 | Rejects key destruction as deletion; requires removable external bytes. |
| C5 | main | NEW-REASON | R2 | sync | L3457-3466 | 639 | C5,C8 | Provenance and cell coordinates can identify people after erasure. |
| C5 | main | DISAGREES | R2 | sync | L3548-3556 | 557 | C5 | External values allegedly make erasure deferrable. |
| C5 | body | CARRIED | BODY | datalog | L67-68 | 73 | C5 | Q10: forgetting was added later as excision. |
| C5 | body | CARRIED | BODY | datalog | L163-164 | 668 | C5,P0 | Q10: legal retention requirements drove explicit forgetting. |
| C5 | body | CARRIED | BODY | datalog | L165-166 | 738 | C5 | Q10: excised entities remained readable; repair required an index checker. |
| C5 | body | CARRIED | BODY | datalog | L167-168 | 649 | C5 | Q10: Cloud lacks excision; disposable data belongs in separately deletable storage. |
| C5 | body | CARRIED | BODY | datalog | L188-188 | 316 | C5,P0 | Q10: excision requests are permanently protected from excision. |
| C5 | body | CARRIED | BODY | datalog | L189-189 | 318 | C5 | Q10: transactional erasure requests trigger nontransactional removal across history. |
| C5 | body | CARRIED | BODY | datalog | L220-220 | 605 | C5 | Q10: excision can require database-sized indexing work and throttle writes. |
| C5 | body | CARRIED | BODY | datalog | L221-221 | 411 | C5,P0 | Q10: excision is exceptional; ordinary mistakes should remain in history. |
| C5 | body | CARRIED | BODY | datalog | L222-222 | 326 | C5 | Q10: no-history attributes prevent guaranteed complete excision. |
| C5 | body | CARRIED | BODY | datalog | L223-223 | 139 | C5 | Q10: backup erasure remains unestablished. |
| C5 | body | CARRIED | BODY | datalog | L224-224 | 552 | C5 | Q10: practitioners use external payloads or per-subject crypto-shredding. |
| C5 | body | CARRIED | BODY | datalog | L225-226 | 409 | C5 | Q10: early cleartext admission makes later erasure expensive and unreliable. |
| C5 | body | CARRIED | BODY | datalog | L352-352 | 94 | C5 | Q10: erasure was retrofitted in Datomic and designed into XTDB. |
| C5 | body | CARRIED | BODY | datalog | L427-427 | 163 | C5,C3 | Q10: immutable single-partition log replaces documents with hashes. |
| C5 | body | CARRIED | BODY | datalog | L428-428 | 92 | C5,C1 | Q10 and Q9: content-addressed documents occupy a forgettable store. |
| C5 | body | CARRIED | BODY | datalog | L433-434 | 393 | C5,P0 | Q10: rewritten architecture uses compaction and an ephemeral log. |
| C5 | body | CARRIED | BODY | datalog | L437-440 | 581 | C5,C1 | Q10: content hashes separate an immutable log from erasable documents. |
| C5 | body | CARRIED | BODY | datalog | L441-442 | 543 | C5,C1 | Q10 and Q9: separate content topics enable erasure and deduplication. |
| C5 | body | CARRIED | BODY | datalog | L443-444 | 167 | C5 | Q10: erasure should avoid rebuilding entire topics or indexes. |
| C5 | body | CARRIED | BODY | datalog | L463-464 | 256 | C5,C2 | Q10: erasure changes historical query results. |
| C5 | body | CARRIED | BODY | datalog | L465-466 | 416 | C5 | Q10: missing erased documents block replay unless pointers resolve to tombstones. |
| C5 | body | CARRIED | BODY | datalog | L479-480 | 744 | C5 | Q10: erasure becomes physically complete through background compaction. |
| C5 | body | CARRIED | BODY | datalog | L485-485 | 916 | C5,P0 | Q10: erasable payloads require tombstones, replay tolerance, and explicit historical-loss reporting. |
| C5 | body | CARRIED | BODY | datalog | L567-567 | 407 | C5,E1 | Q10 and Q1: erasure experience and append-before-judgment make XTDB relevant. |
| C5 | body | DISAGREES | BODY | frontiers | L94-94 | 262 | C5,P0 | Offers retraction plus compaction as physical erasure, beyond the ledger's two roads. |
| C5 | body | CARRIED | BODY | frontiers | L182-183 | 681 | C5 | Q10: tombstones preserve monotone histories while masking immutable values. |
| C5 | body | CARRIED | BODY | frontiers | L212-212 | 146 | C5 | Q10: tombstones hide values without providing erasure. |
| C5 | body | CARRIED | BODY | frontiers | L285-286 | 1042 | C5 | Q10: owner shards, encryption keys, and revocation streams cover stored, backup, and derived data. |
| C5 | body | NEW-REASON | BODY | frontiers | L293-294 | 307 | C5,E6 | Erasure requires a policy for stored model summaries and other derived facts. |
| C5 | body | DISAGREES | BODY | frontiers | L359-360 | 434 | C5,P0 | Treats retraction plus compaction as real erasure; permits loss of historical reads. |
| C5 | body | CARRIED | BODY | log | L72-72 | 304 | C5,P0 | Q10: deletion may yield absence but never substituted content. |
| C5 | body | CARRIED | BODY | log | L86-86 | 126 | C5,P0 | Q10: tombstones preserve absence without replacing content. |
| C5 | body | CARRIED | BODY | log | L112-112 | 238 | C5,P0 | Q10: checkpoint trimming trades away rollback and historical indexing. |
| C5 | body | CARRIED | BODY | log | L161-161 | 310 | C5 | Q10: whole-stream deletion and key destruction are offered as safe erasure paths. |
| C5 | body | CARRIED | BODY | log | L169-169 | 1030 | C5,P0 | Q10: production privacy erasure used rewriting or separate stores; immutable vendors refused in-place edits. |
| C5 | body | DISAGREES | BODY | log | L170-171 | 607 | C5 | Crypto-shredding is legally disputed; side stores weaken single-source truth and retain relational identity. |
| C5 | body | CARRIED | BODY | log | L179-179 | 288 | C5 | Q10: stream erasure leaves an existence marker that may remain sensitive. |
| C5 | body | DISAGREES | BODY | log | L186-187 | 305 | C5,P0 | Includes retroactive rewriting and legal objections to treating key destruction as deletion. |
| C5 | body | CARRIED | BODY | log | L201-202 | 378 | C5,P0 | Q10: privacy deletion may rewrite logs or destroy per-user encryption keys. |
| C5 | body | CARRIED | BODY | log | L207-207 | 304 | C5,P0 | Q10: disconnected replicas prevent safely truncating accumulated edit history. |
| C5 | body | CARRIED | BODY | log | L208-208 | 250 | C5,P0 | Q10: Bluesky explicitly deletes records; permanent undeletability is criticized. |
| C5 | body | CARRIED | BODY | log | L218-218 | 152 | C5,P0 | Q10: rewriting and key destruction support deletion; downstream cleanup uses replay. |
| C5 | body | CARRIED | BODY | log | L248-248 | 124 | C5,P0 | Q10: mandatory temporal shards allow whole logs to retire. |
| C5 | body | DISAGREES | BODY | log | L251-251 | 468 | C5 | Q10: redaction rejected because it conceals misissuance; permanent logs exclude personal data. |
| C5 | body | DISAGREES | BODY | log | L261-261 | 177 | C5 | Removal is prohibited; sensitive content must stay out of the log. |
| C5 | body | CARRIED | BODY | log | L298-298 | 192 | C5,P0 | Q10: copying live nodes reclaims old log segments; garbage collection hurt performance. |
| C5 | body | CARRIED | BODY | log | L321-321 | 261 | C5,P0 | Q10: materialized pages replace old redo records; backups preserve separate redo streams. |
| C5 | body | CARRIED | BODY | log | L344-345 | 754 | C5,P0 | Q10: immutable history protects against bad writes; compaction and regulatory deletion remove old values. |
| C5 | body | CARRIED | BODY | log | L348-348 | 225 | C5,P0 | Q10: excision replaces content with tombstones while preserving offsets. |
| C5 | body | DISAGREES | BODY | log | L402-402 | 525 | C5 | Erasure may also use excision or whole-log rewriting, beyond the ledger's two roads. |
| C5 | body | CARRIED | BODY | log | L403-404 | 57 | C5,P0 | Q10: space-driven trimming is distinct from erasure. |
| C5 | body | DISAGREES | BODY | log | L405-406 | 492 | C5 | Key destruction may not satisfy law; backup cleaning and relational residue complicate the two roads. |
| C5 | body | CARRIED | BODY | meaning | L555-561 | 461 | C5,P0 | Q10: Datomic excision leaves permanent evidence of removal. |
| C5 | body | CARRIED | BODY | meaning | L1083-1089 | 377 | C5,C1 | Q10: Hash addressing makes surviving external copies automatically discoverable and verifiable. |
| C5 | body | CARRIED | BODY | meaning | L1090-1093 | 258 | C5 | Q10: Retrieval metadata is public; future cryptanalysis threatens retained ciphertext. |
| C5 | body | CARRIED | BODY | meaning | L1192-1196 | 264 | C5,P0 | Q10: Suppression preserves restricted content and privately logs removals. |
| C5 | body | CARRIED | BODY | meaning | L1320-1321 | 116 | C5 | Q10: Invalidation represents destruction, cessation, or expiry. |
| C5 | body | DISAGREES | BODY | meaning | L1352-1362 | 757 | C5,P0 | CARRIED Q10: Publication cannot be erased; signed retraction validity is decided by readers. |
| C5 | body | CARRIED | BODY | meaning | L1974-1978 | 275 | C5,P0 | Q10: Restore appends, but deletion destroys an entire document and history. |
| C5 | body | DISAGREES | BODY | meaning | L2021-2027 | 359 | C5 | CARRIED Q10: Xanadu promises permanent content with no deletion. |
| C5 | body | CARRIED | BODY | meaning | L2173-2175 | 119 | C5,P0 | Q10: Planned excision preserved history; retrofitted deletion destroyed it. |
| C5 | body | CARRIED | BODY | meaning | L2267-2277 | 760 | C5,C6 | Q10: Excision leaves backups; hash dependencies and exported copies constrain deletion. |
| C5 | body | CARRIED | BODY | rama | L52-53 | 605 | C5,P0 | Q10; migration replaces content or leaves an offset-preserving tombstone. |
| C5 | body | CARRIED | BODY | rama | L54-54 | 188 | C5,P0 | Q10; migration creates another log, switches, then deletes the original. |
| C5 | body | CARRIED | BODY | rama | L55-55 | 188 | C5,P0 | Q10; migration must be idempotent and also transforms subsequent appends. |
| C5 | body | CARRIED | BODY | rama | L94-94 | 136 | C5,P0 | Q10; excision preserves slots while readers skip removed contents. |
| C5 | body | CARRIED | BODY | rama | L95-95 | 367 | C5 | Q10; deleting a subindex parent leaves child values on disk. |
| C5 | body | CARRIED | BODY | rama | L96-97 | 372 | C5 | Q10; incremental backups retain depot and PState bytes until backup garbage collection. |
| C5 | body | CARRIED | BODY | rama | L98-99 | 146 | C5 | Q10; reference supplies no encryption or backup-excision guarantee. |
| C5 | body | DISAGREES | BODY | rama | L100-101 | 738 | C5 | CARRIED Q10; tombstoning, explicit index deletion, and backup expiry offer a third erasure route. |
| C5 | body | DISAGREES | BODY | rama | L315-316 | 618 | C5 | CARRIED Q10; erasure is operator purging rather than mandatory encryption or external payloads. |
| C5 | body | CARRIED | BODY | skeptics | L38-39 | 956 | C5 | Q10: crypto-shredding and forgettable payloads cover event histories and backups. |
| C5 | body | DISAGREES | BODY | skeptics | L74-74 | 586 | C5 | Crypto-shredding may fail cryptographically, operationally, or legally; also CARRIED Q10. |
| C5 | body | DISAGREES | BODY | skeptics | L75-75 | 529 | C5 | Rejects key deletion as personal-data erasure and prefers external payloads; also CARRIED Q10. |
| C5 | body | DISAGREES | BODY | skeptics | L76-76 | 608 | C5 | Inaccessibility only approaches erasure, rather than accomplishing it; also CARRIED Q10. |
| C5 | body | DISAGREES | BODY | skeptics | L77-77 | 1071 | C5 | Rejects on-log personal ciphertext; also ABOVE and CARRIED Q10. |
| C5 | body | CARRIED | BODY | skeptics | L78-79 | 173 | C5 | Q10: academic countervoice treats crypto-shredding as compliant erasure. |
| C5 | body | DISAGREES | BODY | skeptics | L80-81 | 394 | C5 | Requires external personal values, with encryption only supplementary; also CARRIED Q10. |
| C5 | body | DISAGREES | BODY | skeptics | L236-236 | 371 | C5 | Rejects personal ciphertext on immutable logs; also CARRIED Q10. |
| C5 | body | CARRIED | BODY | skeptics | L309-310 | 257 | C5 | Q10: copied immutable payloads multiply erasure obligations across controllers. |
| C5 | body | NEW-REASON | BODY | sync | L725-726 | 153 | C5,C2 | Global versions require soft deletes. |
| C5 | body | DISAGREES | BODY | sync | L841-845 | 271 | C5 | Prefers permanent history over removal. |
| C5 | body | DISAGREES | BODY | sync | L875-886 | 928 | C5 | Integrity chains prevent deletion; replacement document loses attribution. |
| C5 | body | DISAGREES | BODY | sync | L926-927 | 101 | C5 | Content integrity prevents deletion. |
| C5 | body | DISAGREES | BODY | sync | L940-945 | 315 | C5 | Automerge cannot truncate; authority changes that constraint. |
| C5 | body | DISAGREES | BODY | sync | L946-950 | 265 | C5 | Rejects deleting history; convergence does not guarantee correctness. |
| C5 | body | DISAGREES | BODY | sync | L1003-1010 | 530 | C5 | Full history prevents excising names and draft information. |
| C5 | body | NEW-REASON | BODY | sync | L1055-1057 | 166 | C5 | Held ciphertext and key defeat later revocation. |
| C5 | body | NEW-REASON | BODY | sync | L1107-1108 | 148 | C5,P0 | Unknown reconnect times obstruct safe truncation. |
| C5 | body | DISAGREES | BODY | sync | L1124-1126 | 162 | C5 | Full history prevents erasure; recipients cannot unlearn content. |
| C5 | body | NEW-REASON | BODY | sync | L1173-1175 | 116 | C5 | Ordering requires tombstones after content removal. |
| C5 | body | NEW-REASON | BODY | sync | L1198-1200 | 235 | C5 | Demand erasure requires internal document knowledge. |
| C5 | body | NEW-REASON | BODY | sync | L1476-1478 | 197 | C5,C8 | Public identity history survives deactivation. |
| C5 | body | NEW-REASON | BODY | sync | L1560-1568 | 614 | C5 | Deletion requests outlive targets and prevent rebroadcast. |
| C5 | body | NEW-REASON | BODY | sync | L1614-1615 | 129 | C5 | Deletion needs permanent readmission prevention. |
| C5 | body | CARRIED | BODY | sync | L1717-1718 | 85 | C5 | Q10: missing payload preserves log validity. |
| C5 | body | CARRIED | BODY | sync | L1745-1754 | 736 | C5 | Q10: fixed versioned envelope survives redaction. |
| C5 | body | CARRIED | BODY | sync | L1857-1865 | 577 | C5 | Q10: excision exposes secrets, strips signatures, and cannot reach clones. |
| C5 | body | CARRIED | BODY | sync | L1880-1881 | 106 | C5 | Q10: rotation precedes rewriting; copies remain reachable. |
| C5 | body | CARRIED | BODY | sync | L1990-2000 | 837 | C5 | Q10: nonpropagating shun lists prevent reintroduction and destructive spread. |
| C5 | body | DISAGREES | BODY | sync | L2006-2008 | 233 | C5 | Human name remains after account scrubbing. |
| C5 | body | NEW-REASON | BODY | sync | L2186-2187 | 152 | C5 | Logical deletion labels edges without removing content. |
| C5 | body | CARRIED | BODY | sync | L2210-2214 | 368 | C5 | Q10: separate contents support omission and later verification. |
| C5 | body | CARRIED | BODY | sync | L2223-2225 | 156 | C5 | Q10: removal also requires removing dependents. |
| C5 | body | NEW-REASON | BODY | sync | L2459-2462 | 249 | C5 | Readmission prevention and local deletion limit destructive spread. |
| C5 | body | NEW-REASON | BODY | sync | L2463-2463 | 76 | C5 | Recipients cannot unlearn data. |
| C5 | body | NEW-REASON | BODY | sync | L2466-2467 | 105 | C5,C2 | Version scheme affects physical deletion. |
| C5 | body | CARRIED | BODY | sync | L2471-2472 | 90 | C5 | Q10: rotation precedes rewriting; clones remain uncontrolled. |
| C5 | body | CARRIED | BODY | sync | L2473-2474 | 141 | C5 | Q10: immutable backups limit byte destruction. |
| C5 | body | DISAGREES | BODY | sync | L2475-2476 | 63 | C5 | History-retaining systems cannot erase. |
| C5 | body | NEW-REASON | BODY | sync | L3014-3015 | 100 | C5,C6 | Delete propagation has disputed safety consequences. |
| C5 | body | DISAGREES | BODY | sync | L3190-3193 | 297 | C5 | Unkeyed value hash remains in commitment. |
| C5 | body | NEW-REASON | BODY | sync | L3206-3209 | 232 | C5 | Deletion requires requests, acknowledgements, and recorded silence. |
| C6 | main | NEW-REASON | R2 | datalog | L986-987 | 403 | C6 | Replacement links avoid rerunning newer admission logic while rebuilding historical chains. |
| C6 | main | NEW-REASON | ABOVE | frontiers | L417-417 | 410 | C6 | Upsert-style supersession hides retractions and shifts state requirements onto consumers. |
| C6 | main | NEW-REASON | R2 | log | L685-686 | 148 | C6 | Parent lists preserve multiple predecessors rather than assuming a single replacement. |
| C6 | main | DISAGREES | ABOVE | rama | L468-468 | 313 | C6 | Camp calls replacement pointers redundant locally; author defends them across stores. |
| C6 | main | NEW-REASON | R2 | rama | L691-692 | 187 | C6 | Permanent offers already retain the expected predecessor. |
| C6 | body | NEW-REASON | BODY | datalog | L294-294 | 333 | C6 | Paired assertion and retraction datoms explicitly retain the replaced value. |
| C6 | body | DISAGREES | BODY | datalog | L295-295 | 280 | C6,E1 | CAS checks expected value instead of the expected fact version. |
| C6 | body | NEW-REASON | BODY | datalog | L296-297 | 460 | C6 | Explicit replacement links avoid costly index-dependent reconstruction. |
| C6 | body | NEW-REASON | BODY | frontiers | L61-62 | 499 | C6 | Upserts force consumers to retain previous values just to interpret changes. |
| C6 | body | NEW-CASE | BODY | frontiers | L79-79 | 154 | C6 | Supporting incoming upserts required retaining the entire keyed collection. |
| C6 | body | NEW-REASON | BODY | frontiers | L91-91 | 226 | C6 | Explicit retraction targets allow stateless change consumers. |
| C6 | body | NEW-REASON | BODY | frontiers | L318-318 | 117 | C6 | Multiplicity differences generalize binary assertion and retraction. |
| C6 | body | NEW-REASON | BODY | frontiers | L379-380 | 161 | C6 | Replacement pointers eliminate consumers' need to retain prior state. |
| C6 | body | NEW-REASON | BODY | log | L75-75 | 412 | C6 | Linear versus branching version histories require different parent cardinalities. |
| C6 | body | DISAGREES | BODY | log | L485-486 | 382 | C6 | Event Store leaves predecessors implicit rather than recording a replacement pointer. |
| C6 | body | NEW-REASON | BODY | log | L487-488 | 558 | C6 | Sparse cell positions require explicit predecessors; parent lists support merging stores or layers. |
| C6 | body | DISAGREES | BODY | meaning | L1018-1022 | 268 | C6 | Replacement relation lives in a separate patch rather than on the successor. |
| C6 | body | NEW-REASON | BODY | meaning | L1173-1180 | 468 | C6 | Correction differs from expiry; deprecation reasons prevent reintroduction. |
| C6 | body | DISAGREES | BODY | meaning | L1363-1366 | 169 | C6 | Supersession conventions were added after five years of records. |
| C6 | body | NEW-REASON | BODY | meaning | L1836-1841 | 339 | C6 | Transient systems needed keyed durable state with supersession. |
| C6 | body | NEW-CASE | BODY | meaning | L1842-1846 | 321 | C6 | Retraction before replacement caused flicker and loss of retained consequences. |
| C6 | body | DISAGREES | BODY | meaning | L2451-2459 | 516 | C6 | Replacement can live outside successors or be removed to enable deletion. |
| C6 | body | DISAGREES | BODY | rama | L327-328 | 281 | C6 | An explicit replacement pointer is considered redundant with cell order. |
| C6 | body | DISAGREES | BODY | skeptics | L58-59 | 208 | C6 | New records omit replacement pointers and discard checked versions. |
| C6 | body | DISAGREES | BODY | skeptics | L246-246 | 50 | C6 | Replacement pointers are not retained. |
| C6 | body | DISAGREES | BODY | sync | L576-581 | 317 | C6,P0 | Losing values and provenance disappear under overwrite. |
| C6 | body | DISAGREES | BODY | sync | L619-620 | 128 | C6 | Rebase rewrites originally expected value. |
| C6 | body | NEW-REASON | BODY | sync | L837-840 | 297 | C6,E5 | Replacement intent cannot be derived from previously seen changes. |
| C6 | body | DISAGREES | BODY | sync | L1430-1441 | 895 | C6,P0,C7 | Signed predecessor chain removed; null placeholder remains. |
| C6 | body | NEW-REASON | BODY | sync | L1465-1468 | 264 | C6,C5 | Context-free deletion forces consumers to retain prior state. |
| C6 | body | DISAGREES | BODY | sync | L1507-1509 | 173 | C6 | Signed replacement pointers were removed. |
| C6 | body | DISAGREES | BODY | sync | L1608-1609 | 160 | C6,E3,P0 | No predecessors; author clocks order disposable versions. |
| C6 | body | NEW-REASON | BODY | sync | L1917-1924 | 511 | C6 | Portable supersession markers preserve actor, time, and possible successors. |
| C6 | body | NEW-REASON | BODY | sync | L2464-2465 | 153 | C6,C5 | Context-free deletes require retained prior state. |
| C6 | body | DISAGREES | BODY | sync | L2803-2812 | 657 | C6 | Some systems omit predecessors or rewrite replacement context. |
| C7 | main | ABOVE | SHORT | datalog | L37-38 | 720 | C7,C8,E3,E6 | Move shared provenance from individual facts to the saying. |
| C7 | main | ABOVE | SHORT | datalog | L47-50 | 821 | C7,C3 | Challenges planetary scope and the fixed nine-part envelope. |
| C7 | main | ABOVE | ABOVE | datalog | L592-593 | 823 | C7,X2,A2 | Stored schema and code need admission versions, runtime identities, and ordered policy dependencies. |
| C7 | main | ABOVE | ABOVE | datalog | L596-597 | 935 | C7,C8,E3 | Reduce the fixed envelope and move shared provenance into open saying attributes. |
| C7 | main | DISAGREES | ABOVE | datalog | L621-621 | 192 | C7 | Absent attributes replace required causal slots; also ABOVE. |
| C7 | main | ABOVE | ABOVE | datalog | L622-622 | 122 | C7 | Rejects maps-versus-classes framing in favor of plain data. |
| C7 | main | ABOVE | R2 | datalog | L781-784 | 117 | C7 | Introduces the saying as a separate entity and provenance container. |
| C7 | main | DISAGREES | R2 | datalog | L792-792 | 69 | C7 | Absence of because-of denotes chain start. |
| C7 | main | DISAGREES | R2 | datalog | L803-804 | 182 | C7 | Core permits an absent replacement field rather than explicit chain-start marking. |
| C7 | main | ABOVE | R2 | datalog | L805-806 | 716 | C7,C8 | Shared provenance belongs on sayings, supported by Datomic and transaction-auditing practice. |
| C7 | main | ABOVE | R2 | datalog | L821-821 | 57 | C7,C8,E3 | When, reads, and cause are shared once per saying. |
| C7 | main | ABOVE | R2 | datalog | L925-928 | 433 | C7 | Reopens minimal data sufficiency and the fixed-envelope framing. |
| C7 | main | ABOVE | R2 | datalog | L930-930 | 528 | C7,C4 | Named attributes separate labels from definitions; positional envelopes require permanent external agreement. |
| C7 | main | ABOVE | R2 | datalog | L931-931 | 331 | C7 | Provenance belongs to the shared dataset or stream. |
| C7 | main | ABOVE | R2 | datalog | L932-933 | 117 | C7 | Use an open, growing set of saying attributes. |
| C7 | main | DISAGREES | R2 | datalog | L942-945 | 720 | C7 | Envelope versioning is too costly; open vocabulary accommodates economic and cross-store provenance. |
| C7 | main | NEW-CASE | R2 | datalog | L972-973 | 491 | C7 | One-off repair scripts without causal provenance become unexplained historical operations. |
| C7 | main | DISAGREES | R2 | datalog | L974-975 | 514 | C7 | Absence means unknown cause; chain start is explicit, but missing fields still carry meaning. |
| C7 | main | ABOVE | R2 | datalog | L1015-1015 | 462 | C7,X1,C3 | An atomic saying joins envelope design, provenance precision, and partition choice. |
| C7 | main | ABOVE | ABOVE | frontiers | L420-420 | 443 | C7 | Questions nine fixed envelope parts versus system-understood fields and ordinary payload. |
| C7 | main | ABOVE | R2 | frontiers | L575-575 | 166 | C7,C4 | Questions the fixed envelope through floor-only verdict, read-set, and cut seed kinds. |
| C7 | main | ABOVE | ABOVE | log | L548-549 | 621 | C7 | Challenges fixing nine parts; advocates named maps, versions, hashed extensions, and tolerant readers. |
| C7 | main | NEW-REASON | R2 | log | L723-723 | 145 | C7 | Versioned named maps need an extension slot; refusal volume remains a concern. |
| C7 | main | DISAGREES | ABOVE | meaning | L2611-2620 | 735 | C7 | Replace a closed fixed envelope with a tiny core and open policy-controlled extensions. |
| C7 | main | ABOVE | ABOVE | meaning | L2685-2693 | 573 | C7 | Prioritise offer-verdict communication over a fixed internal record theory. |
| C7 | main | ABOVE | R2 | meaning | L2921-2931 | 625 | C7 | Cut core to six parts needed by handlers that do not interpret content. |
| C7 | main | ABOVE | R2 | meaning | L2932-2935 | 223 | C7,C6 | Move provenance, authority, and expected version onto extensible saying attributes. |
| C7 | main | NEW-REASON | R2 | meaning | L2936-2943 | 558 | C7 | Separate gate requiredness from bytes; retain unknown tags and distinct presence states. |
| C7 | main | ABOVE | R2 | meaning | L2944-2951 | 499 | C7 | Federation requires room for missing provenance without invented values. |
| C7 | main | ABOVE | R2 | meaning | L3137-3138 | 88 | C7 | Six-part core plus extension rules. |
| C7 | main | ABOVE | ABOVE | rama | L455-456 | 223 | C7 | A forever-fixed envelope is challenged in favor of explicit evolution rules. |
| C7 | main | NEW-CASE | R2 | rama | L701-702 | 798 | C7,P0 | A malformed-offer flood makes permanent intake expensive; validate before append and retain facts separately. |
| C7 | main | ABOVE | ABOVE | skeptics | L296-297 | 159 | C7,W2 | Reframes representation choice around rollback compatibility under never-rewrite. |
| C7 | main | NEW-REASON | R2 | skeptics | L429-430 | 236 | C7,P0,X2 | Permanent history imposes indefinite format support and tolerance of earlier invariant violations. |
| C7 | main | ABOVE | R2 | skeptics | L436-437 | 70 | C7,C1,W2 | Reframes maps versus classes around the exact bytes committed by hashing. |
| C7 | main | DISAGREES | ABOVE | sync | L3070-3081 | 827 | C7 | Unknown schemas admitted; also questions self-hosted policy repair. |
| C7 | main | DISAGREES | ABOVE | sync | L3096-3110 | 1043 | C7 | Unknown fields retained; also rejects fixed nine-part envelope. |
| C7 | main | ABOVE | ABOVE | sync | L3160-3161 | 129 | C7 | Logical independence precedes maps-versus-classes. |
| C7 | main | DISAGREES | R2 | sync | L3582-3590 | 598 | C7,E8 | Chain starts point at sessions instead of explicit origin marks. |
| C7 | main | ABOVE | R2 | sync | L3670-3673 | 311 | C7 | Expanded envelope needs versioning rather than nine fixed parts. |
| C7 | body | NEW-REASON | BODY | datalog | L83-84 | 256 | C7 | Additional structural components introduce application rigidity. |
| C7 | body | NEW-REASON | BODY | datalog | L85-86 | 177 | C7 | Atomic facts keep transaction size proportional to novelty. |
| C7 | body | ABOVE | BODY | datalog | L87-96 | 1305 | C7,C8,E3 | Transaction-level provenance avoids enormous datoms and repeated metadata. |
| C7 | body | ABOVE | BODY | datalog | L97-100 | 365 | C7 | Questions minimal data sufficiency and provenance granularity. |
| C7 | body | ABOVE | BODY | datalog | L151-158 | 404 | C7 | Fixed places conflate meaning with position and force placeholder values. |
| C7 | body | DISAGREES | BODY | datalog | L202-202 | 734 | C7,C4 | Bootstrap uses a magic epoch timestamp; also adds self-naming and reserved-range mechanisms. |
| C7 | body | DISAGREES | BODY | datalog | L281-281 | 349 | C7 | An absent causal attribute denotes chain start instead of an explicit mark. |
| C7 | body | NEW-REASON | BODY | datalog | L325-326 | 758 | C7,W2 | Plain named data survives runtime rebuilds without requiring historical classes. |
| C7 | body | ABOVE | BODY | datalog | L356-356 | 121 | C7 | Questions whether data should exist independently of interpreters. |
| C7 | body | NEW-CASE | BODY | datalog | L391-392 | 413 | C7,C8 | Unrecorded origins left Nubank unable to explain operations. |
| C7 | body | NEW-CASE | BODY | datalog | L404-404 | 159 | C7,C8 | Missing operation origins were a major early mistake. |
| C7 | body | NEW-CASE | BODY | datalog | L473-474 | 290 | C7,W2 | Arbitrary serialized values were replaced with stricter portable types. |
| C7 | body | NEW-REASON | BODY | datalog | L475-476 | 353 | C7 | Whole-document retransmission amplifies single-field changes. |
| C7 | body | NEW-CASE | BODY | datalog | L492-492 | 132 | C7,W2 | Restricting serialized value types improved portability. |
| C7 | body | NEW-REASON | BODY | datalog | L538-539 | 228 | C7,X4 | Atomic datoms provide fine-grained security and synchronization filters. |
| C7 | body | ABOVE | BODY | datalog | L566-566 | 410 | C7,C3 | Provenance belongs on sayings; closed-world scope limits planetary transfer. |
| C7 | body | DISAGREES | BODY | frontiers | L213-213 | 166 | C7 | Allows emptiness to mark chain starts instead of requiring an explicit mark. |
| C7 | body | ABOVE | BODY | frontiers | L320-321 | 133 | C7 | Different communication contexts call for different record shapes. |
| C7 | body | ABOVE | BODY | frontiers | L334-334 | 78 | C7 | Questions a fixed envelope across communication contexts. |
| C7 | body | NEW-CASE | BODY | log | L126-126 | 401 | C7 | Positional headers broke under upgrades; named header maps survived. |
| C7 | body | NEW-REASON | BODY | log | L130-131 | 246 | C7 | Survey found almost no reserved-field or compatibility provisions. |
| C7 | body | NEW-CASE | BODY | log | L140-140 | 90 | C7 | Named maps survived version skew where positional fields failed. |
| C7 | body | NEW-REASON | BODY | log | L175-175 | 86 | C7 | Weak-schema maps are preferred over typed classes. |
| C7 | body | NEW-REASON | BODY | log | L206-206 | 381 | C7 | Bidirectional format compatibility and accumulated optional fields constrain envelope evolution. |
| C7 | body | NEW-CASE | BODY | log | L245-245 | 550 | C7,C1 | Signed extension space enabled receipt indexes and removed the hash-lookup database. |
| C7 | body | NEW-CASE | BODY | log | L246-246 | 179 | C7 | A clean second CT format had no planned adoption. |
| C7 | body | NEW-CASE | BODY | log | L512-513 | 602 | C7 | Header upgrades, upcaster stacks, optional fields, and CT extensions expose format-evolution costs. |
| C7 | body | NEW-REASON | BODY | meaning | L186-197 | 644 | C7 | Minimal formatting suffices without a universal bootstrap. |
| C7 | body | ABOVE | BODY | meaning | L217-219 | 160 | C7 | Smallest universal core. |
| C7 | body | ABOVE | BODY | meaning | L226-237 | 621 | C7 | Minimal envelope with a seam avoids one mandatory theory. |
| C7 | body | NEW-REASON | BODY | meaning | L268-277 | 626 | C7,E3,C8 | Minimal sufficiency includes labels, temporal granularity, and provenance. |
| C7 | body | ABOVE | BODY | meaning | L361-364 | 265 | C7 | Minimal envelope and seam answer interpreter regress. |
| C7 | body | NEW-REASON | BODY | meaning | L372-376 | 211 | C7 | Recordhood requires sufficient labels. |
| C7 | body | NEW-REASON | BODY | meaning | L391-394 | 304 | C7,E3,C8 | Temporal and provenance granularity remain design choices. |
| C7 | body | ABOVE | BODY | meaning | L440-444 | 360 | C7 | Neither camp establishes the minimal envelope. |
| C7 | body | NEW-REASON | BODY | meaning | L470-478 | 590 | C7 | Immutable records address future runtimes and stores. |
| C7 | body | ABOVE | BODY | meaning | L526-534 | 653 | C7 | Move provenance onto admission; retain a smaller datom core. |
| C7 | body | DISAGREES | BODY | meaning | L535-539 | 336 | C7 | Open optional extensions and unknown parts replace a closed envelope. |
| C7 | body | NEW-REASON | BODY | meaning | L540-545 | 400 | C7,W2 | Parsing must work without fetching validation grammar. |
| C7 | body | ABOVE | BODY | meaning | L578-584 | 499 | C7 | Nine parts already impose one provenance theory. |
| C7 | body | ABOVE | BODY | meaning | L591-599 | 602 | C7,A2 | Shrink the immutable core and record interpreter versions. |
| C7 | body | NEW-CASE | BODY | meaning | L845-851 | 322 | C7 | Removing unknown-field preservation broke essential forwarding behaviour. |
| C7 | body | NEW-REASON | BODY | meaning | L852-855 | 242 | C7 | Closed shapes motivated removal of unknown-field preservation. |
| C7 | body | NEW-CASE | BODY | meaning | L856-859 | 312 | C7 | Proto3 restored preservation; JSON conversion and field copying still lose unknown data. |
| C7 | body | NEW-REASON | BODY | meaning | L860-864 | 295 | C7 | Forwarders must preserve unfamiliar parts across runtime versions. |
| C7 | body | NEW-CASE | BODY | meaning | L865-872 | 442 | C7 | Parser-level required fields repeatedly broke infrastructure. |
| C7 | body | NEW-REASON | BODY | meaning | L873-875 | 240 | C7 | Validation belongs at consumers rather than pass-through intermediaries. |
| C7 | body | NEW-REASON | BODY | meaning | L876-877 | 156 | C7 | Required fields are nearly impossible to make optional safely. |
| C7 | body | ABOVE | BODY | meaning | L878-884 | 470 | C7 | Required container metadata harms composition and adds redundancy. |
| C7 | body | NEW-REASON | BODY | meaning | L885-889 | 330 | C7,W2 | Storage and live wire representations evolve under different pressures. |
| C7 | body | NEW-REASON | BODY | meaning | L890-897 | 484 | C7 | Requiredness belongs in gate policy rather than physical parsing. |
| C7 | body | ABOVE | BODY | meaning | L937-944 | 576 | C7 | Successful systems evolve; perfect upfront design is rejected. |
| C7 | body | ABOVE | BODY | meaning | L945-951 | 414 | C7,A3 | Unknown-field evolution and small writers challenge fixed shapes and one gate. |
| C7 | body | DISAGREES | BODY | meaning | L1125-1126 | 64 | C7 | Retired back-pointer remains a required null slot. |
| C7 | body | NEW-CASE | BODY | meaning | L1416-1427 | 742 | C7,A1 | An early tape bundled executable access procedures and standard entry pointers. |
| C7 | body | ABOVE | BODY | meaning | L1428-1433 | 326 | C7 | Standard initial pointers followed by open extensions. |
| C7 | body | ABOVE | BODY | meaning | L1434-1439 | 353 | C7 | A short open envelope carries interpretation instead of assuming an external browser. |
| C7 | body | ABOVE | BODY | meaning | L1451-1462 | 822 | C7 | Freezing Smalltalk blocked evolution; meta-level changes need explicit fences. |
| C7 | body | ABOVE | BODY | meaning | L1511-1513 | 201 | C7 | Minimise immutable parts and keep a uniform framework. |
| C7 | body | ABOVE | BODY | meaning | L1521-1527 | 412 | C7,A2 | Minimise the envelope and runtime outside the medium. |
| C7 | body | NEW-REASON | BODY | meaning | L1698-1705 | 529 | C7 | Contracts mediate separately built programs beyond language-local typing. |
| C7 | body | ABOVE | BODY | meaning | L1741-1748 | 526 | C7 | Compelling examples produced excessive semantic rules. |
| C7 | body | ABOVE | BODY | meaning | L1749-1753 | 245 | C7 | Nine envelope parts risk the same example-driven overdesign. |
| C7 | body | NEW-CASE | BODY | meaning | L2413-2419 | 300 | C7,E2 | Folk added trigger propagation late; requiredness limits future evolution. |
| C7 | body | DISAGREES | BODY | meaning | L2420-2425 | 369 | C7 | Empty is assigned unknown meaning rather than requiring an explicit signal. |
| C7 | body | DISAGREES | BODY | meaning | L2541-2548 | 343 | C7,P0 | Open records permit unknown parts; motion and refusal bodies may be trimmed. |
| C7 | body | NEW-REASON | BODY | rama | L213-213 | 343 | C7,W2 | Java serialization cannot reliably deserialize earlier type versions. |
| C7 | body | NEW-REASON | BODY | rama | L219-220 | 401 | C7,W2 | Custom types bind clients to serializer jars; plain data can retain checked grammars. |
| C7 | body | NEW-REASON | BODY | rama | L283-284 | 443 | C7 | Tight schemas detect inconsistent objects at creation. |
| C7 | body | NEW-REASON | BODY | rama | L333-334 | 349 | C7,W2 | Evolving custom schemas motivate Thrift or Protobuf. |
| C7 | body | DISAGREES | BODY | rama | L403-404 | 371 | C7,C5 | Null is a deletion signal; expiring tombstones let lagging readers miss erasure. CARRIED Q10. |
| C7 | body | ABOVE | BODY | skeptics | L108-108 | 197 | C7,C4 | Questions indirect versioned value grammars as a complication of the storage model. |
| C7 | body | NEW-REASON | BODY | skeptics | L173-174 | 444 | C7,P0 | Never-rewrite extends old-format readability obligations indefinitely. |
| C7 | body | NEW-REASON | BODY | skeptics | L191-191 | 261 | C7,X2 | Historical facts require readers to retain old shapes or identify the rules originally applied. |
| C7 | body | NEW-REASON | BODY | skeptics | L211-212 | 898 | C7,C1,C2 | Observable implementation habits become dependencies even without contractual promises. |
| C7 | body | NEW-REASON | BODY | skeptics | L216-216 | 225 | C7,C1 | Randomizing unspecified behavior prevents consumers from relying on accidental stability. |
| C7 | body | NEW-REASON | BODY | skeptics | L302-302 | 272 | C7,C1,E3 | A second store exposes accidental first-store behaviors as an incompatible de facto protocol. |
| C7 | body | DISAGREES | BODY | sync | L153-157 | 371 | C7 | Infinity marks pending admission instead of an explicit variant. |
| C7 | body | NEW-REASON | BODY | sync | L321-323 | 194 | C7 | Plain string IDs simplify cross-service serialization. |
| C7 | body | NEW-REASON | BODY | sync | L374-384 | 751 | C7 | Cross-runtime numeric types can silently corrupt values. |
| C7 | body | DISAGREES | BODY | sync | L808-811 | 297 | C7 | Unknown metadata must survive instead of being refused. |
| C7 | body | NEW-CASE | BODY | sync | L899-902 | 290 | C7 | Storage engine types leaked through public API. |
| C7 | body | DISAGREES | BODY | sync | L920-921 | 127 | C7,C4 | Recommends null actor and counter as origin identity. |
| C7 | body | DISAGREES | BODY | sync | L931-933 | 169 | C7 | Requires preserving unknown fields. |
| C7 | body | NEW-REASON | BODY | sync | L981-982 | 111 | C7,C4 | Translation metadata itself needs versioning. |
| C7 | body | DISAGREES | BODY | sync | L1401-1406 | 472 | C7,X2 | Unknown grammars fail open. |
| C7 | body | NEW-CASE | BODY | sync | L1454-1458 | 380 | C7,C4 | Existing colon-bearing keys forced specification relaxation. |
| C7 | body | NEW-REASON | BODY | sync | L1479-1482 | 267 | C7,C8 | Float encoding diverges; key rotation complicates old signatures. |
| C7 | body | NEW-CASE | BODY | sync | L1526-1528 | 109 | C7 | Interop failures forced float, ordering, and null rules. |
| C7 | body | NEW-REASON | BODY | sync | L1580-1586 | 491 | C7,C6 | Reader-visible optional edits become mandatory. |
| C7 | body | NEW-REASON | BODY | sync | L1618-1621 | 205 | C7 | Optional interpretation features become mandatory. |
| C7 | body | DISAGREES | BODY | sync | L1814-1816 | 202 | C7 | Commits preserve unknown headers. |
| C7 | body | DISAGREES | BODY | sync | L1914-1916 | 186 | C7 | Magic all-zero and all-z IDs designate origin. |
| C7 | body | NEW-CASE | BODY | sync | L1931-1933 | 231 | C7 | Rebase silently drops unknown change-ID metadata. |
| C7 | body | NEW-REASON | BODY | sync | L1967-1970 | 283 | C7 | Simple formats target interpreters centuries later. |
| C7 | body | NEW-CASE | BODY | sync | L2150-2152 | 167 | C7,W2 | Physical formats require incompatible migrations. |
| C7 | body | NEW-CASE | BODY | sync | L2215-2217 | 177 | C7 | New patch format rapidly obsoleted repositories. |
| C7 | body | DISAGREES | BODY | sync | L2359-2364 | 405 | C7,C4 | Origins use magic values or empty authorization lists. |
| C7 | body | DISAGREES | BODY | sync | L2416-2418 | 181 | C7,X2 | Gate accepts unknown grammars. |
| C7 | body | DISAGREES | BODY | sync | L2727-2733 | 435 | C7 | Origins use empty lists or magic root parents. |
| C7 | body | NEW-REASON | BODY | sync | L2941-2942 | 108 | C7 | Future readers require enduring format interpretation. |
| C7 | body | DISAGREES | BODY | sync | L2943-2945 | 159 | C7 | Unknown fields must be preserved. |
| C7 | body | NEW-REASON | BODY | sync | L2946-2949 | 308 | C7 | Runtime quirks constrain format portability. |
| C7 | body | NEW-REASON | BODY | sync | L2953-2954 | 131 | C7 | Cross-runtime integers can lose precision. |
| C8 | main | NEW-REASON | R2 | datalog | L966-967 | 466 | C8 | A local actor check becomes an unverified claim when facts cross stores. |
| C8 | main | ABOVE | ABOVE | meaning | L2673-2674 | 131 | C8 | Agent-versus-person framing omits the separate grant. |
| C8 | main | NEW-REASON | R2 | meaning | L2879-2883 | 306 | C8 | Only the chain head is authenticated; later links remain claims. |
| C8 | main | DISAGREES | SHORT | sync | L116-122 | 451 | C8 | Nostr signs as the person; replica provenance omits authorship. |
| C8 | body | DISAGREES | BODY | datalog | L203-203 | 458 | C8,C7 | Bootstrap facts lack an actor and carry an artificial time. |
| C8 | body | NEW-REASON | BODY | datalog | L229-229 | 444 | C8,A2 | Applications attach actor and runtime metadata to transactions. |
| C8 | body | DISAGREES | BODY | datalog | L230-230 | 208 | C8,E4 | Trusted peers can transact without actor or delegation verification. |
| C8 | body | ABOVE | BODY | datalog | L231-231 | 652 | C8,C7,E4 | Open transaction attributes separate actor, principal, and grant; trusted-writer assumptions limit transfer. |
| C8 | body | NEW-REASON | BODY | datalog | L232-233 | 105 | C8 | Transaction signatures provide cryptographic actor verification. |
| C8 | body | DISAGREES | BODY | datalog | L342-342 | 73 | C8 | Datomic trusts writers and never verifies actors. |
| C8 | body | NEW-REASON | BODY | datalog | L402-402 | 218 | C8,A2 | Actor credentials and runtime commits are routinely recorded together. |
| C8 | body | ABOVE | BODY | datalog | L411-412 | 247 | C8,C3 | Closed ownership and trusted writers limit transfer to a shared planetary store. |
| C8 | body | NEW-REASON | BODY | datalog | L576-576 | 612 | C8,X4 | Cryptographic actor verification and fail-closed policy are available precedents. |
| C8 | body | DISAGREES | BODY | log | L411-412 | 544 | C8 | CT records no submitter, contrary to preserving each fact's immediate actor. |
| C8 | body | NEW-REASON | BODY | meaning | L112-128 | 952 | C8,C4 | Attribution requires trust; labels do not determine interpretation. |
| C8 | body | NEW-REASON | BODY | meaning | L492-498 | 417 | C8,E4 | Authenticating attribution differs from safely executing meaning. |
| C8 | body | NEW-REASON | BODY | meaning | L667-675 | 517 | C8 | Anonymous bearer rights complicate accountability. |
| C8 | body | NEW-REASON | BODY | meaning | L676-680 | 378 | C8 | Delegation passes responsibility alongside authority. |
| C8 | body | NEW-REASON | BODY | meaning | L681-684 | 254 | C8 | Attribution is local belief rather than nonrepudiable proof. |
| C8 | body | DISAGREES | BODY | meaning | L685-693 | 614 | C8 | By-whom is an upstream responsibility chain rather than solely the immediate actor. |
| C8 | body | NEW-REASON | BODY | meaning | L699-700 | 103 | C8 | Capability accountability remained unresolved for decades. |
| C8 | body | DISAGREES | BODY | meaning | L715-724 | 615 | C8 | By-whom becomes a chain authenticated only at its head. |
| C8 | body | NEW-REASON | BODY | meaning | L1304-1312 | 588 | C8 | Delegation assigns activity-scoped responsibility without specifying its degree. |
| C8 | body | NEW-REASON | BODY | meaning | L1367-1367 | 68 | C8 | Signing does not solve key loss or compromise. |
| C8 | body | ABOVE | BODY | meaning | L1406-1415 | 684 | C8,E5 | No gate, private-key identity, and uncosted provenance limit transfer. |
| C8 | body | DISAGREES | BODY | meaning | L1817-1827 | 718 | C8,X4 | Attribution is optional and any program can overwrite another within the room. |
| C8 | body | DISAGREES | BODY | meaning | L2278-2291 | 849 | C8 | By-whom is a chain vouched for link by link. |
| C8 | body | NEW-REASON | BODY | meaning | L2292-2297 | 327 | C8 | Signatures enable portable authorship but introduce key-loss and compromise limits. |
| C8 | body | NEW-REASON | BODY | rama | L104-105 | 205 | C8,E4 | Reference supplies no authentication or authorization mechanism. |
| C8 | body | NEW-REASON | BODY | rama | L106-106 | 128 | C8 | Client-executed partitioners assume trusted appenders. |
| C8 | body | NEW-REASON | BODY | rama | L107-107 | 133 | C8 | REST append and read examples supply no credentials. |
| C8 | body | NEW-REASON | BODY | rama | L317-318 | 479 | C8,E4 | Trusted pipelines use permissions chiefly to protect the log from mistakes. |
| C8 | body | NEW-REASON | BODY | rama | L343-343 | 274 | C8,E1 | Trusted descriptive pipelines do not establish admission safety for many independent actors. |
| C8 | body | NEW-REASON | BODY | rama | L421-422 | 210 | C8,E1,E6 | Trusted transport records lack attribution, admission checks, and read history. |
| C8 | body | DISAGREES | BODY | skeptics | L40-41 | 1268 | C8,E4 | Shared-account attribution omits verified actors and delegation; also NEW-CASE on ambiguous request identities. |
| C8 | body | DISAGREES | BODY | skeptics | L237-237 | 215 | C8 | Shared database accounts and application-set attribution replace verified immediate actors. |
| C8 | body | DISAGREES | BODY | sync | L179-182 | 263 | C8 | Programs act through users' credentials. |
| C8 | body | DISAGREES | BODY | sync | L233-235 | 211 | C8 | User credentials erase the acting program's identity. |
| C8 | body | NEW-CASE | BODY | sync | L859-864 | 447 | C8 | Fresh actors per run inflate long-lived documents. |
| C8 | body | NEW-CASE | BODY | sync | L892-898 | 449 | C8 | Authorship absent for eight years. |
| C8 | body | NEW-REASON | BODY | sync | L916-919 | 259 | C8 | Standing actors avoid per-run identity growth. |
| C8 | body | NEW-CASE | BODY | sync | L934-934 | 80 | C8 | Replica identity left early authorship unavailable. |
| C8 | body | NEW-REASON | BODY | sync | L1046-1048 | 174 | C8 | Authorization principals do not establish human identity. |
| C8 | body | NEW-REASON | BODY | sync | L1049-1054 | 394 | C8 | Authority groups separate people from device keys. |
| C8 | body | NEW-REASON | BODY | sync | L1127-1130 | 298 | C8,E4 | Agent attribution and device authority have different identity requirements. |
| C8 | body | DISAGREES | BODY | sync | L1170-1172 | 170 | C8,E3 | Deletes omit actor and time. |
| C8 | body | NEW-CASE | BODY | sync | L1186-1197 | 824 | C8,E3 | Late provenance needs side maps; early attribution remains missing. |
| C8 | body | NEW-CASE | BODY | sync | L1204-1213 | 625 | C8,E3,C1 | Missing birth metadata stays missing; gate could prevent collisions. |
| C8 | body | NEW-REASON | BODY | sync | L1317-1321 | 306 | C8 | Replica identity cannot safely span tabs or crashes. |
| C8 | body | NEW-REASON | BODY | sync | L1344-1344 | 37 | C8 | Replica identity differs from user identity. |
| C8 | body | DISAGREES | BODY | sync | L1423-1427 | 270 | C8 | Host exercises the person's signing keys. |
| C8 | body | DISAGREES | BODY | sync | L1512-1513 | 114 | C8 | Host acts under person's identity. |
| C8 | body | NEW-REASON | BODY | sync | L1534-1539 | 382 | C8,C5 | Recovery, key rotation, and governance constrain decentralization. |
| C8 | body | DISAGREES | BODY | sync | L1571-1579 | 632 | C8,E4 | Delegation burden led to impersonation and off-record scopes. |
| C8 | body | DISAGREES | BODY | sync | L1610-1613 | 290 | C8,E4 | Delegation rejected as reader burden; replacement hides agents. |
| C8 | body | DISAGREES | BODY | sync | L1622-1627 | 368 | C8,E4 | Rejects delegation schemes. |
| C8 | body | NEW-CASE | BODY | sync | L1663-1671 | 614 | C8,E4 | Versioned delegation addresses multi-device identity limits. |
| C8 | body | NEW-REASON | BODY | sync | L1878-1879 | 147 | C8 | Author and admitter differ; display mappings correct identity. |
| C8 | body | NEW-REASON | BODY | sync | L2196-2201 | 366 | C8,C5 | Key signatures resist spoofing; external names remain changeable. |
| C8 | body | NEW-CASE | BODY | sync | L2218-2222 | 321 | C8 | Late attribution redesign added keys and identity mappings. |
| C8 | body | NEW-CASE | BODY | sync | L2503-2506 | 244 | C8 | Replica-only records required late author side maps. |
| C8 | body | NEW-REASON | BODY | sync | L2507-2508 | 147 | C8 | Maker and accepting actor differ. |
| C8 | body | DISAGREES | BODY | sync | L2509-2511 | 155 | C8 | Programs sign as people. |
| C8 | body | DISAGREES | BODY | sync | L2512-2515 | 255 | C8,E4 | Nostr rejects on-record delegation burden. |
| C8 | body | NEW-REASON | BODY | sync | L2516-2517 | 99 | C8 | Authority groups hide replaceable devices. |
| C8 | body | DISAGREES | BODY | sync | L2518-2520 | 205 | C8 | Public keys serve as author identities. |
| C8 | body | NEW-REASON | BODY | sync | L2523-2525 | 213 | C8,E4 | Key rotation complicates historical verification. |
| C8 | body | DISAGREES | BODY | sync | L2917-2927 | 745 | C8 | Cited hosts act as people. |
| C8 | body | NEW-REASON | BODY | sync | L3210-3211 | 144 | C8 | Grouped keys preserve cross-store person identity. |
| E | main | ABOVE | ABOVE | datalog | L590-591 | 521 | E7,P0 | Rejects one fact substance for firehose data and incidental application state. |
| E | main | ABOVE | ABOVE | datalog | L600-601 | 616 | E6 | Record patterns at acts; per-render crossings risk firehose volume. |
| E | main | ABOVE | ABOVE | datalog | L618-618 | 179 | E6 | Replace read-list framing with basis and question. |
| E | main | DISAGREES | R2 | datalog | L790-790 | 93 | E8 | Session identity carries runtime provenance instead of epoch and crossing. |
| E | main | DISAGREES | R2 | datalog | L791-791 | 90 | E6 | Read patterns, bases, and anchors live on the saying rather than the crossing. |
| E | main | NEW-REASON | R2 | datalog | L820-820 | 227 | E1,X1 | One atomic verdict admits or rejects the entire saying. |
| E | main | DISAGREES | R2 | datalog | L822-822 | 39 | E8 | Session pointer remains the provenance unit. |
| E | main | DISAGREES | R2 | datalog | L831-833 | 907 | E6,E5 | Reads move onto sayings; coarse provenance trades dependency precision for shared storage. |
| E | main | NEW-REASON | R2 | datalog | L923-924 | 230 | E1,C5 | An erased value's surviving shape evidence is its admission verdict. |
| E | main | NEW-CASE | R2 | datalog | L968-969 | 589 | E3,C2 | Clock rollback breaks historical wall-clock lookup unless stamps remain monotonic per unit. |
| E | main | DISAGREES | R2 | datalog | L970-971 | 616 | E6 | Stores reads once on the saying rather than on a referenced crossing. |
| E | main | DISAGREES | R2 | datalog | L980-981 | 762 | E5 | Read meanings become separate attributes; provenance corrections attach to sayings instead of fact envelopes. |
| E | main | DISAGREES | R2 | datalog | L988-989 | 749 | E1 | Refusals belong in offerer session layers, outside the contested cell's partition. |
| E | main | DISAGREES | R2 | datalog | L990-991 | 253 | E8 | Session-start identity remains central rather than gate epoch and crossing. |
| E | main | DISAGREES | R2 | datalog | L992-993 | 1039 | E7 | Point and select are recorded only through resulting sayings, not by default. |
| E | main | NEW-CASE | R2 | datalog | L994-995 | 579 | E4 | Copied toolkits can act without the owner's authorship or grant. |
| E | main | ABOVE | R2 | datalog | L1002-1002 | 873 | E5 | Staleness depends on later meaning, not a follow-or-stay choice fixed at admission. |
| E | main | DISAGREES | R2 | datalog | L1006-1006 | 156 | E7 | Motions enter records through resulting acts or crossings; also ABOVE. |
| E | main | CARRIED | R2 | datalog | L1017-1017 | 965 | E1,A1 | Q1: XTDB's deterministic replay restrictions support preserving outcomes across runtime rebuilds. |
| E | main | DISAGREES | R2 | datalog | L1020-1020 | 173 | E6 | Reads reside once on the saying rather than on a referenced crossing; also ABOVE. |
| E | main | ABOVE | R2 | datalog | L1021-1021 | 164 | E7,P0 | One append-only substance excludes incidental firehose state. |
| E | main | ABOVE | ABOVE | frontiers | L418-418 | 477 | E1,X2 | Questions the programming model and consequences of storing the gate's rules as facts. |
| E | main | DISAGREES | ABOVE | frontiers | L422-422 | 623 | E6 | Omits deterministic read sets; ABOVE challenges universal provenance. |
| E | main | ABOVE | ABOVE | frontiers | L433-433 | 160 | E5 | Rejects follow-or-pin as a read-entry choice. |
| E | main | DISAGREES | ABOVE | frontiers | L434-434 | 153 | E3 | Makes clock ownership secondary to one authoritative ordering field; ABOVE questions the framing. |
| E | main | ABOVE | ABOVE | frontiers | L435-435 | 247 | E6 | Questions matching only new landings instead of defining obligations to history. |
| E | main | DISAGREES | R2 | frontiers | L524-525 | 773 | E3 | Uses gate stamps for partition and causal order without a recorded remap. |
| E | main | DISAGREES | R2 | frontiers | L526-527 | 359 | E3 | Requires monotone, causally ordered when stamps from the first record. |
| E | main | ABOVE | R2 | frontiers | L532-533 | 742 | E5,C6 | Rejects follow-or-stay choice; metadata fixes and retractions need different supersession meanings. |
| E | main | DISAGREES | R2 | frontiers | L534-535 | 674 | E5 | Replaces dependence marks with observed roles and later weighting; adds alternative supports. |
| E | main | DISAGREES | R2 | frontiers | L536-537 | 211 | E5 | Records roles and optional support groups instead of dependence marks. |
| E | main | ABOVE | R2 | frontiers | L540-541 | 1071 | E6,C2 | Questions universal provenance: per-row costs, vector costs, and unobservable reads through opaque bodies. |
| E | main | DISAGREES | R2 | frontiers | L542-543 | 726 | E6,E5 | Treats read recording and manifest safety as unretrofittable, rather than early losses. |
| E | main | DISAGREES | R2 | frontiers | L544-545 | 157 | E6 | Canonical read sets belong to firings, with offers pointing there instead of crossings. |
| E | main | NEW-REASON | R2 | frontiers | L546-546 | 245 | E6,C2 | Read entries add matched-count and id-version digests, including explicit empty results. |
| E | main | NEW-REASON | R2 | frontiers | L547-547 | 199 | E6 | Read-set completeness marks distinguish captured reads from all actual reads. |
| E | main | DISAGREES | R2 | frontiers | L548-549 | 92 | E6,E2 | Crossings reference separate read sets instead of owning the recorded reads. |
| E | main | NEW-REASON | R2 | frontiers | L550-551 | 324 | E6 | Digests validate exact pattern replay and expose index, erasure, or policy mismatches. |
| E | main | ABOVE | R2 | frontiers | L568-568 | 78 | E5,C6 | Replaces follow-or-stay framing with the superseding fact's reason for change. |
| E | main | ABOVE | R2 | frontiers | L573-573 | 140 | E6 | Narrows the universal-read objection to an explicit capture-completeness mark. |
| E | main | ABOVE | ABOVE | log | L552-553 | 571 | E6 | Questions retaining every read forever at machine rate; offers bounded provenance representations. |
| E | main | ABOVE | ABOVE | log | L562-562 | 220 | E1 | Logging offers before judgment precedes decisions about verdict placement and retention. |
| E | main | ABOVE | ABOVE | log | L563-564 | 219 | E3 | Reframes whose clock around statement time versus admission time. |
| E | main | NEW-CASE | R2 | log | L699-700 | 656 | E1 | Thirty agents on one summary cell can generate twenty-nine permanent refusals per accepted write. |
| E | main | NEW-REASON | R2 | log | L701-702 | 845 | E1,C5 | Destroying refused payload keys preserves verdicts but loses future gate replay; session closure permits tiering. |
| E | main | NEW-CASE | R2 | log | L719-719 | 538 | E6,E3 | A 200,000-fact summary makes row provenance enormous; patterns and cuts bound it. |
| E | main | NEW-REASON | R2 | log | L724-724 | 386 | E7 | Settled viewport on crossings preserves intent while years of motion ticks accumulate. |
| E | main | ABOVE | R2 | log | L731-731 | 76 | E1 | Reframes refusal retention around session-layer closure. |
| E | main | ABOVE | R2 | log | L733-734 | 90 | E6 | Reframes listing every read as accounting for every read. |
| E | main | NEW-REASON | R2 | log | L738-738 | 300 | E1,A3 | Two appends per action amplify sequential admission costs; atomic combined append avoids this. |
| E | main | NEW-CASE | R2 | log | L745-746 | 151 | E1,E7 | Thirty agents contending on one cell amplify permanent refusal and gesture storage. |
| E | main | ABOVE | ABOVE | meaning | L2602-2610 | 550 | E4 | Actor-based policy combined with matching creates ambient authority. |
| E | main | DISAGREES | ABOVE | meaning | L2634-2640 | 429 | E6 | Universal read provenance conflicts with privacy and useful signal. |
| E | main | ABOVE | ABOVE | meaning | L2669-2672 | 174 | E4,C8 | Replace acting in a person's name with purpose-specific delegation. |
| E | main | ABOVE | ABOVE | meaning | L2675-2676 | 90 | E7 | The system can attest shown, not looked. |
| E | main | NEW-REASON | R2 | meaning | L2823-2830 | 509 | E4,C8 | Named grantees preserve accountability while preventing ids from acting as bearer tokens. |
| E | main | NEW-REASON | R2 | meaning | L2831-2835 | 293 | E1 | Refusals record expected and received values alongside decision cuts. |
| E | main | NEW-REASON | R2 | meaning | L2845-2847 | 159 | E7,E4 | Grant coverage can carry selection without an additional selection fact. |
| E | main | NEW-REASON | R2 | meaning | L2862-2868 | 407 | E4 | Targets in values require authority from the triggering or enabling grant. |
| E | main | NEW-REASON | R2 | meaning | L2869-2878 | 676 | E4,C8 | Pin tool versions; delegate narrower sub-grants; revoke descendants with parents. |
| E | main | NEW-CASE | R2 | meaning | L3023-3031 | 512 | E4 | Poisoned input installs a persistent tool that outlives its creator's grant. |
| E | main | DISAGREES | R2 | meaning | L3032-3052 | 1504 | E7 | Raw motion capture must be opt-in; only changed shown content is recorded by default. |
| E | main | NEW-CASE | R2 | meaning | L3079-3088 | 678 | E1,P0 | Looping agents generate refusal floods; quotas and payers control retained bodies. |
| E | main | NEW-REASON | R2 | meaning | L3094-3097 | 184 | E5,E4 | Historical reads pin; grants must default to pinned versions. |
| E | main | DISAGREES | R2 | meaning | L3098-3106 | 589 | E3 | Rejects writer-clock metadata; occurrence time becomes ordinary content under a seed key. |
| E | main | ABOVE | R2 | meaning | L3114-3117 | 135 | E4,C8 | Reframes tools acting as people into bounded delegation. |
| E | main | ABOVE | R2 | meaning | L3118-3120 | 202 | E7 | Questions recording inferred looking; shown records should follow content changes. |
| E | main | DISAGREES | R2 | meaning | L3124-3125 | 54 | E3 | Writer time is the read cut rather than a claimed clock. |
| E | main | ABOVE | R2 | meaning | L3126-3129 | 139 | E4 | Attribution alone leaves ambient authority intact. |
| E | main | ABOVE | R2 | meaning | L3130-3136 | 529 | E6,E7,E1 | Exhaust and surveillance challenge universal recording; narrow what deserves persistence. |
| E | main | ABOVE | R2 | meaning | L3153-3160 | 601 | E4,C5 | Matching among strangers at planetary scale has no demonstrated precedent. |
| E | main | ABOVE | ABOVE | rama | L459-460 | 469 | E6 | Recording every read on facts risks provenance dominating storage. |
| E | main | ABOVE | ABOVE | rama | L467-467 | 141 | E3 | Clock choice is reframed as recorded provenance rather than ordering. |
| E | main | NEW-REASON | ABOVE | rama | L469-470 | 223 | E1 | Keeping offers and verdicts permits auditing a fallible gate. |
| E | main | CARRIED | R2 | rama | L532-533 | 463 | E3,A1 | Q2; gate-clock data prevents reconstructing fact rows from offers. |
| E | main | CARRIED | R2 | rama | L534-535 | 367 | E3,A1,X2 | Q2; replay clocks and cross-partition policy timing can change admission results. |
| E | main | CARRIED | R2 | rama | L550-551 | 179 | E3,E1 | Q2; read the clock once, retain the decision, then publish it. |
| E | main | DISAGREES | R2 | rama | L564-565 | 693 | E3 | CARRIED Q1,Q2; moves stamping before append instead of retaining a separate gate admission stamp. |
| E | main | DISAGREES | R2 | rama | L610-610 | 176 | E4 | The door checks policy; the gate checks only shape and version. |
| E | main | DISAGREES | R2 | rama | L615-616 | 749 | E4,C1,C3 | Check grants at the door or encode ownership in layer ids; CARRIED Q7. |
| E | main | NEW-REASON | R2 | rama | L647-647 | 111 | E6 | Practical depot record-size limits may constrain long read lists. |
| E | main | DISAGREES | R2 | rama | L670-671 | 631 | E4,C8,A2 | Door checks grants instead of gate; NEW-CORNER: preserve door build and verification method. |
| E | main | CARRIED | R2 | rama | L673-673 | 470 | E3,E1 | Q2; reuse the first stamp and identify its task and module instance. |
| E | main | DISAGREES | R2 | rama | L674-674 | 427 | E6 | Store full read lists in separate records instead of pattern-plus-cut crossing records. |
| E | main | NEW-REASON | R2 | rama | L685-686 | 210 | E5 | Deterministic tools purportedly make all reads dependencies; distinctions remain for people and models. |
| E | main | DISAGREES | R2 | rama | L693-694 | 786 | E1,P0 | Refusals should expire after a horizon; NEW-CASE: hot-cell contention multiplies retained refusals. |
| E | main | NEW-CASE | R2 | rama | L695-696 | 411 | E8,A2 | Mid-session deployment makes session-start build attribution insufficient. |
| E | main | DISAGREES | R2 | rama | L697-698 | 819 | E7,P0 | Hand records should use a separate, expiring log; NEW-CASE: millions of daily motion facts. |
| E | main | ABOVE | R2 | rama | L708-708 | 117 | E7,P0 | Reframes hand recording as a question about differing permanence promises. |
| E | main | ABOVE | R2 | rama | L709-709 | 144 | E1,P0 | Reframes refusal placement around whether verdicts are facts or views. |
| E | main | DISAGREES | ABOVE | skeptics | L282-282 | 454 | E6,E5 | Prefers a small actionable read subset over complete crossing provenance; also ABOVE. |
| E | main | ABOVE | ABOVE | skeptics | L293-293 | 291 | E1,C1 | Reframes verdict retention around late retries and the caller's original response. |
| E | main | ABOVE | R2 | skeptics | L376-376 | 545 | E6,C2,A1 | Tests historical-screen reconstruction through temporal tables, logged queries, retention, and reproducible rendering. |
| E | main | DISAGREES | R2 | skeptics | L377-377 | 297 | E2,P0,C5 | Model inputs and replies are sampled and expire; retained copies also enlarge erasure obligations. |
| E | main | NEW-REASON | R2 | skeptics | L379-381 | 339 | E5,E6 | Absent common read provenance, change history cannot establish staleness or transitive doubt. |
| E | main | NEW-CASE | R2 | skeptics | L382-383 | 461 | E5,E6 | A retracted paper must invalidate summaries and downstream claims across independently built tools. |
| E | main | ABOVE | R2 | skeptics | L384-385 | 401 | E6,C3 | Separates provenance capability from engine choice; questions matcher and planetary admission costs. |
| E | main | DISAGREES | R2 | skeptics | L410-411 | 279 | E3,E1,C2 | Orders by gate timestamps and excludes refusals, unlike E3 and E1. |
| E | main | DISAGREES | R2 | skeptics | L412-413 | 1039 | E3,C2,C3 | Requires clock-order promises at record one; also NEW-REASON on heartbeat and admission waiting costs. |
| E | main | DISAGREES | R2 | skeptics | L418-418 | 265 | E3 | Permits wall-clock ordering once clock error is bounded. |
| E | main | NEW-CASE | R2 | skeptics | L426-426 | 318 | E1,C1 | An agent restarting in another session must retrieve original verdicts while replaying its outbox. |
| E | main | NEW-CASE | R2 | skeptics | L428-428 | 241 | E4 | Imported tools must not acquire default authority merely by entering a person's layer. |
| E | main | DISAGREES | R2 | skeptics | L433-433 | 123 | E3,C2 | Reserves future clock ordering instead of prohibiting it; also ABOVE. |
| E | main | ABOVE | R2 | skeptics | L435-435 | 87 | E1 | Reframes refusal storage around the response and lookup path for late retries. |
| E | main | DISAGREES | R2 | skeptics | L443-443 | 116 | E3 | Requires clock-order capability from the first record instead of permanently excluding it. |
| E | main | NEW-REASON | SHORT | sync | L88-94 | 412 | E6 | Runtime ranges capture absent rows and serve conflicts and invalidation. |
| E | main | NEW-CASE | SHORT | sync | L95-101 | 423 | E5,C6 | Pijul split read roles; Mercurial omitted rewrite kinds. |
| E | main | ABOVE | ABOVE | sync | L3126-3135 | 687 | E6 | Durable read provenance creates privacy and storage burdens. |
| E | main | ABOVE | ABOVE | sync | L3155-3156 | 135 | E6 | Ask how runtime captures predicate reads. |
| E | main | DISAGREES | ABOVE | sync | L3157-3159 | 171 | E7 | Crossings and operands replace default hand facts. |
| E | main | ABOVE | ABOVE | sync | L3162-3164 | 170 | E3 | Separate claimed time from admission clock. |
| E | main | NEW-REASON | R2 | sync | L3356-3359 | 259 | E2,C3 | Shared-layer contention permits visibly pending offers. |
| E | main | ABOVE | R2 | sync | L3469-3477 | 567 | E5,C4 | Follow-or-stay applies to value references, not historical reads. |
| E | main | NEW-REASON | R2 | sync | L3478-3484 | 437 | E5 | Role corrections append; actor kind guides interpretation. |
| E | main | NEW-CASE | R2 | sync | L3485-3491 | 462 | E5 | Paper retractions create stale-alert noise without exposure distinction. |
| E | main | NEW-CASE | R2 | sync | L3515-3526 | 831 | E4,E1 | Matrix merges mischecked delegated grants and reset state. |
| E | main | NEW-REASON | R2 | sync | L3527-3533 | 412 | E4,E1 | Ordered grants avoid resolution but require retained versions. |
| E | main | DISAGREES | R2 | sync | L3607-3616 | 674 | E1 | Challenges permanent refusals using 29-to-one amplification. |
| E | main | DISAGREES | R2 | sync | L3617-3619 | 112 | E8 | Session-start remains required; adds gate build to verdict. |
| E | main | DISAGREES | R2 | sync | L3620-3631 | 833 | E7 | Rejects default hand capture; records crossings and operands. |
| E | main | ABOVE | R2 | sync | L3643-3644 | 121 | E5,C4 | Follow-or-stay belongs to value-reference grammar. |
| E | main | DISAGREES | R2 | sync | L3652-3653 | 103 | E7 | Actions and shown context replace motion records. |
| E | main | ABOVE | R2 | sync | L3682-3683 | 151 | E6,E7,P0 | Untrimmed hand and read histories fail on volume and privacy. |
| E | body | ABOVE | BODY | datalog | L123-128 | 431 | E6 | Perception remains free; ordinary reads never create transactions. |
| E | body | NEW-CASE | BODY | datalog | L175-176 | 547 | E1 | Jepsen prompted separate terminology for submitted requests and admitted datoms. |
| E | body | NEW-REASON | BODY | datalog | L179-180 | 685 | E3 | Recording time cannot represent revisable domain event time. |
| E | body | NEW-REASON | BODY | datalog | L237-237 | 485 | E3 | Millisecond stamp collisions prevent precise transaction ordering. |
| E | body | NEW-REASON | BODY | datalog | L238-238 | 392 | E3,C2 | Monotonic stamps permit wall-clock queries to map onto ordered positions. |
| E | body | NEW-CASE | BODY | datalog | L239-239 | 317 | E3 | Paper publication dates cannot serve as admission stamps. |
| E | body | NEW-REASON | BODY | datalog | L245-245 | 202 | E5 | Trust views derive from metadata about sayings. |
| E | body | NEW-REASON | BODY | datalog | L263-263 | 333 | E6 | Single-fact reads reduce to narrow patterns; outside anchors cannot be reconstructed. |
| E | body | NEW-REASON | BODY | datalog | L264-264 | 249 | E5 | Triggering reads and content dependencies belong to different transaction attributes. |
| E | body | ABOVE | BODY | datalog | L265-266 | 477 | E6 | Recording all perception would undo reader independence; acts and crossings delimit recording. |
| E | body | DISAGREES | BODY | datalog | L269-269 | 329 | E5 | Uses separate attributes instead of a dependence flag within each read entry. |
| E | body | NEW-REASON | BODY | datalog | L270-270 | 691 | E5,C2 | Identity plus recorded basis supports both historical and current reference resolution. |
| E | body | DISAGREES | BODY | datalog | L300-300 | 254 | E1 | Successful transactions serve as verdicts; refusals leave no durable trace. |
| E | body | NEW-REASON | BODY | datalog | L301-301 | 295 | E1 | Success responses expose before-state, after-state, and expanded datoms. |
| E | body | DISAGREES | BODY | datalog | L302-302 | 614 | E1 | Deterministic verdicts need no storage when all dependencies share one order. |
| E | body | DISAGREES | BODY | datalog | L303-304 | 377 | E1 | Datomic drops refusals; XTDB and Nubank experience support retaining them. |
| E | body | DISAGREES | BODY | datalog | L309-310 | 370 | E8,A2 | A session entity, renewed on rebuild, is the runtime provenance unit. |
| E | body | NEW-REASON | BODY | datalog | L313-313 | 446 | E7 | High-churn histories increase storage and indexing costs. |
| E | body | NEW-CASE | BODY | datalog | L314-314 | 252 | E7 | Nubank regretted using its default fact store for firehose writes. |
| E | body | DISAGREES | BODY | datalog | L315-315 | 284 | E7 | Select and point become facts only when they contribute to an act. |
| E | body | NEW-REASON | BODY | datalog | L316-317 | 371 | E5,E2 | Showing is attestable; looking is only inferred. |
| E | body | DISAGREES | BODY | datalog | L320-320 | 169 | E4,C8 | Transaction functions trust any writer without a checked invocation grant. |
| E | body | NEW-REASON | BODY | datalog | L321-322 | 261 | E4 | Grant narrowing requires a new name rather than silently changing relied-upon meaning. |
| E | body | NEW-REASON | BODY | datalog | L337-338 | 165 | E2,C2 | Recording-time history answers what someone was shown. |
| E | body | DISAGREES | BODY | datalog | L345-345 | 93 | E6 | Datomic keeps no read provenance. |
| E | body | NEW-REASON | BODY | datalog | L351-351 | 142 | E3 | Valid time can be a first-class axis rather than an ordinary attribute. |
| E | body | NEW-CASE | BODY | datalog | L393-394 | 309 | E1 | Nubank lost requests preceding writes and regretted downstream-only event sourcing. |
| E | body | ABOVE | BODY | datalog | L395-396 | 367 | E7 | Firehose writes and long strings do not belong in the universal fact store. |
| E | body | NEW-CASE | BODY | datalog | L403-403 | 144 | E1 | Nubank wanted the complete upstream offer stream, including refusals. |
| E | body | ABOVE | BODY | datalog | L406-406 | 89 | E7 | Rejects firehose data as fact-store content. |
| E | body | NEW-REASON | BODY | datalog | L431-432 | 71 | E3 | Records carry both transaction time and valid time. |
| E | body | NEW-REASON | BODY | datalog | L445-448 | 257 | E3 | External corrections make transaction-time history inadequate for domain history. |
| E | body | NEW-REASON | BODY | datalog | L449-450 | 247 | E3 | Valid time addresses lag, corrections, and effective-date queries. |
| E | body | CARRIED | BODY | datalog | L477-478 | 1050 | E1,E3,A1 | Q1 and Q2: resolved-result logs relax deterministic replay and external metadata constraints. |
| E | body | CARRIED | BODY | datalog | L489-489 | 900 | E1 | Q1: offers enter the log before judgment; retained results include refusals and metadata. |
| E | body | CARRIED | BODY | datalog | L490-490 | 634 | E1,A1 | Q1: resolved outcomes avoid requiring future runtimes to reproduce all historical verdicts. |
| E | body | CARRIED | BODY | datalog | L497-498 | 350 | E1,C5 | Q1 and Q10: appended offers, retained refusals, and erasure survived a runtime rewrite. |
| E | body | CARRIED | BODY | datalog | L501-501 | 125 | E1,E3 | Q1: passive transaction logs differ from active transactors. |
| E | body | CARRIED | BODY | datalog | L506-509 | 328 | E1,A3 | Q1: secondary stores follow one writer's resolved log. |
| E | body | NEW-REASON | BODY | datalog | L534-537 | 343 | E6 | Subscription matching reverses the direction of query execution. |
| E | body | ABOVE | BODY | datalog | L540-543 | 426 | E6 | General Datalog cannot efficiently reverse high-volume changes into affected subscriptions. |
| E | body | ABOVE | BODY | datalog | L570-571 | 224 | E6 | Matching requires a weaker language than unrestricted queries. |
| E | body | DISAGREES | BODY | frontiers | L41-42 | 847 | E3 | Gate-assigned time prescribes order rather than merely recording when. |
| E | body | ABOVE | BODY | frontiers | L63-64 | 705 | E6,A1 | Questions exhaustive provenance; compute useful explanations on demand. |
| E | body | DISAGREES | BODY | frontiers | L67-68 | 900 | E1,P0 | Verdicts become views; failed intents and unobserved writes may disappear. CARRIED Q1. |
| E | body | DISAGREES | BODY | frontiers | L87-87 | 474 | E3 | Uses the gate's clock for order; rejects a separate decorative timestamp. |
| E | body | NEW-REASON | BODY | frontiers | L89-89 | 277 | E5 | Recorded reads pin versions; following and staleness are derived views. |
| E | body | ABOVE | BODY | frontiers | L90-90 | 431 | E6,A1 | Questions per-output provenance for deterministic results; non-rederivable results retain empty reads too. |
| E | body | DISAGREES | BODY | frontiers | L92-92 | 248 | E1 | Offers and verdicts may have shorter retention instead of permanent verdict storage. |
| E | body | NEW-REASON | BODY | frontiers | L126-126 | 154 | E2,C2 | Output corrections must be distinguishable from changes in the world. |
| E | body | NEW-REASON | BODY | frontiers | L137-138 | 507 | E6 | Dependency granularity trades redundant recomputation against graph-metadata overhead. |
| E | body | NEW-REASON | BODY | frontiers | L139-140 | 378 | E6 | Lazy computation cannot notify changes; wanted-output inputs approximate selective eagerness. |
| E | body | NEW-REASON | BODY | frontiers | L156-156 | 284 | E6 | Pattern-grain reads avoid per-value dependency metadata dominating computation. |
| E | body | NEW-REASON | BODY | frontiers | L158-158 | 247 | E2,C2 | Crossings distinguish partial answers from answers complete at their cut. |
| E | body | NEW-REASON | BODY | frontiers | L160-161 | 78 | E6 | Wanted outputs become maintained input data. |
| E | body | NEW-REASON | BODY | frontiers | L190-191 | 968 | E3 | Receiver time and carried premise time expose temporal causality as data. |
| E | body | NEW-REASON | BODY | frontiers | L194-195 | 597 | E5 | Alternative lineage supports enable solver-based failure analysis. |
| E | body | NEW-CASE | BODY | frontiers | L199-199 | 1041 | E6 | Netflix replaced fine-grained lineage with request traces because porting applications was unacceptable. |
| E | body | DISAGREES | BODY | frontiers | L208-208 | 282 | E3 | Gate stamps supply order at supersession and absence checks. |
| E | body | NEW-REASON | BODY | frontiers | L209-209 | 386 | E5,C2 | Based-on manifests let consumers wait for causal prerequisites, beyond auditing. |
| E | body | NEW-REASON | BODY | frontiers | L210-210 | 360 | E5 | Alternative support groups prevent doubt from spreading through redundant dependencies. |
| E | body | NEW-REASON | BODY | frontiers | L214-214 | 208 | E1 | Refusals provide lineage explaining absent outcomes. |
| E | body | NEW-REASON | BODY | frontiers | L215-216 | 339 | E6 | Opaque-body reads are capturable only when the runtime controls every read path. |
| E | body | NEW-REASON | BODY | frontiers | L219-220 | 475 | E6,A1 | Rule-based protocol lineage does not directly cover opaque tools, models, or permanent provenance. |
| E | body | DISAGREES | BODY | frontiers | L252-252 | 512 | E8,A1 | Requires session-start runtime and machine metadata as first-record convention; ledger uses epochs and crossings. |
| E | body | ABOVE | BODY | frontiers | L254-254 | 330 | E6 | Questions landing-only matching: newly registered tools may owe work to historical facts. |
| E | body | NEW-CASE | BODY | frontiers | L326-327 | 726 | E6,W1 | Goebel moved from eager fact maintenance to demand-driven maintenance at thousands of views. |
| E | body | NEW-REASON | BODY | frontiers | L335-336 | 100 | E6,W1 | Demand-driven maintenance addresses many thousands of views. |
| E | body | DISAGREES | BODY | frontiers | L363-364 | 655 | E3 | Ingest-assigned time is the ordering field; sender time is separate data. |
| E | body | NEW-REASON | BODY | frontiers | L369-370 | 445 | E6,E5 | Read records include empty matches, firing triggers, and explicit coarse-lineage kinds. |
| E | body | NEW-REASON | BODY | frontiers | L371-372 | 383 | E5 | Pinned reads and alternative supports prevent excessive doubt propagation. |
| E | body | DISAGREES | BODY | frontiers | L381-382 | 318 | E1 | Verdicts may be computed from offers and refusals trimmed sooner than facts. |
| E | body | NEW-REASON | BODY | frontiers | L407-407 | 382 | E5,C1 | Build systems use recorded input hashes to decide what requires recomputation. |
| E | body | NEW-REASON | BODY | frontiers | L445-445 | 101 | E1,A3 | Exchanging interpreted facts avoids duplicate interpretation of offers across stores. |
| E | body | NEW-REASON | BODY | log | L77-77 | 154 | E3 | Messages contain past observations; distant services share no simultaneous present. |
| E | body | NEW-REASON | BODY | log | L78-78 | 244 | E1 | Durable processing transitions must preserve identical replies on retries. |
| E | body | NEW-REASON | BODY | log | L92-92 | 147 | E1 | Durable retry decisions need not be records in the same log. |
| E | body | NEW-REASON | BODY | log | L93-93 | 106 | E3 | Each service has its own present; received data describes the past. |
| E | body | NEW-REASON | BODY | log | L94-94 | 252 | E6 | Read logging can remain idempotent without changing substantive entity behavior. |
| E | body | CARRIED | BODY | log | L104-105 | 525 | E1,C3 | Q1,Q7: shared-log systems evolved from ordered transaction records to partial ordering and replaceable logs. |
| E | body | DISAGREES | BODY | log | L109-109 | 442 | E6,E5 | Every transaction logs object-version read rows and validates all of them. |
| E | body | NEW-REASON | BODY | log | L110-110 | 281 | E1 | Decision records are required when a reader lacks enough objects to derive the verdict. |
| E | body | NEW-REASON | BODY | log | L119-119 | 174 | E3 | Time proposals make retention robust to skew and drift. |
| E | body | NEW-REASON | BODY | log | L134-134 | 257 | E1,A1 | Code changes prevent future verdict derivation, requiring recorded decisions. |
| E | body | DISAGREES | BODY | log | L181-181 | 204 | E1 | Rejected expected-version checks leave no durable verdict. |
| E | body | CARRIED | BODY | log | L194-194 | 174 | E1 | Q1: a leader may validate before appending to the log. |
| E | body | CARRIED | BODY | log | L195-195 | 760 | E1,C3 | Q1,Q7: ordered offers precede validation; outcomes go to a separate stream. |
| E | body | NEW-REASON | BODY | log | L199-199 | 192 | E3,P0 | Timestamp ordering can insert events before the end, violating append order. |
| E | body | CARRIED | BODY | log | L215-215 | 62 | E1 | Q1: both admission outcomes are recorded in a separate stream. |
| E | body | NEW-REASON | BODY | log | L221-222 | 283 | E6 | Unverified read-as-event precedent names storage cost for tracing decisions. |
| E | body | NEW-CASE | BODY | log | L234-234 | 247 | E1 | Waiting for inclusion delayed certificate issuance, motivating promise-only receipts. |
| E | body | NEW-REASON | BODY | log | L235-235 | 215 | E1 | Admission restrictions bound spam and permanent log growth. |
| E | body | NEW-REASON | BODY | log | L236-236 | 280 | E3 | Log timestamps must progress monotonically and cannot appear in the client's future. |
| E | body | NEW-CASE | BODY | log | L244-244 | 397 | E1 | Merge-delay breaches led Sunlight to sequence entries before acknowledging them. |
| E | body | DISAGREES | BODY | log | L252-252 | 169 | E1 | Refusals leave only a returned error, without durable verdicts. |
| E | body | DISAGREES | BODY | log | L260-260 | 83 | E1 | Refusals are deliberately unrecorded to control spam and growth. |
| E | body | NEW-REASON | BODY | log | L262-262 | 87 | E3 | Gate time must be monotone and cannot be trusted when future-dated. |
| E | body | NEW-REASON | BODY | log | L279-280 | 270 | E5,C2 | Named reads establish cross-partition causal precedence as well as provenance. |
| E | body | DISAGREES | BODY | log | L283-284 | 487 | E1 | Verdicts are derived by all readers rather than durably written; also ABOVE and Q1. |
| E | body | CARRIED | BODY | log | L286-286 | 73 | E1 | Q1: the database consists of the intention log. |
| E | body | CARRIED | BODY | log | L287-287 | 226 | E1 | Q1: appending an intention precedes the commit decision. |
| E | body | DISAGREES | BODY | log | L289-289 | 239 | E1 | Atomic append arbitrates; identical log replay derives every verdict without recording it. |
| E | body | CARRIED | BODY | log | L291-291 | 124 | E1 | Q1: both committed and aborted transaction updates remain in the intention log. |
| E | body | DISAGREES | BODY | log | L292-293 | 175 | E6 | Queries are never logged, enabling linear read scaling. |
| E | body | DISAGREES | BODY | log | L302-302 | 443 | E1 | Hyder never writes verdicts; identical-code replay is contrasted with recorded Tango decisions. |
| E | body | NEW-REASON | BODY | log | L304-304 | 205 | E6,C2 | A snapshot position compactly names the state underlying a read set. |
| E | body | DISAGREES | BODY | log | L310-311 | 265 | E1 | Hyder and Tango disagree over whether admission decisions must be written. |
| E | body | DISAGREES | BODY | log | L326-327 | 1045 | E1,E3 | Aurora DSQL omits refusals and uses physical-time ordering; also Q3,Q7,Q10. |
| E | body | DISAGREES | BODY | log | L332-332 | 122 | E1 | Aurora and DSQL retain no refusal verdicts. |
| E | body | NEW-REASON | BODY | log | L334-335 | 516 | E6,C2 | Historical pattern results require retained index history; later staleness checks can be cheaper. |
| E | body | CARRIED | BODY | log | L349-349 | 216 | E3 | Q2: Rama records append time; a separate gate stamp creates a second clock. |
| E | body | DISAGREES | BODY | log | L417-418 | 871 | E3 | Includes physical-clock ordering in DSQL, contrary to ordering by neither stamp. |
| E | body | NEW-REASON | BODY | log | L419-420 | 285 | E3 | Statement time can predate admission by decades during large historical ingestion. |
| E | body | CARRIED | BODY | log | L421-422 | 341 | E3 | Q2: choose append time or a separate gate stamp; enforce per-partition monotonicity. |
| E | body | DISAGREES | BODY | log | L442-442 | 294 | E6,E5 | Every object's version is logged and all reads are validated before accepting writes. |
| E | body | NEW-REASON | BODY | log | L446-447 | 502 | E6,E2 | Direct parents and shared snapshots bound provenance; external results must survive source changes. |
| E | body | NEW-REASON | BODY | log | L454-455 | 530 | E5,C1 | Pinned versions preserve recoverable history; dependency judgments are available only when writing. |
| E | body | DISAGREES | BODY | log | L492-492 | 115 | E1 | Logs offers but never records derived verdicts. |
| E | body | CARRIED | BODY | log | L493-493 | 163 | E1 | Q1: offers and durable outcomes use separate records or streams. |
| E | body | DISAGREES | BODY | log | L494-495 | 293 | E1 | Judges before logging and preserves no refusal verdict. |
| E | body | NEW-REASON | BODY | log | L496-497 | 306 | E1 | Stable retry replies need retained decisions only through the bounded retry lifetime. |
| E | body | DISAGREES | BODY | log | L498-499 | 901 | E1 | Refusal retention may expire; verdict stream may trim independently of facts. |
| E | body | NEW-REASON | BODY | log | L508-509 | 940 | E7,E5 | Intent-grain recording limits motion growth; shown and looked require different witnesses. |
| E | body | DISAGREES | BODY | log | L635-635 | 305 | E3 | Spanner uses synchronized physical clocks for order. |
| E | body | DISAGREES | BODY | log | L636-636 | 418 | E3,C2 | DSQL uses physical-time cuts; excessive skew loses linearizability. |
| E | body | NEW-REASON | BODY | meaning | L244-248 | 247 | E4 | Receiving executable meaning needs safety assumptions. |
| E | body | NEW-REASON | BODY | meaning | L249-257 | 488 | E4 | An interpreter may be refused because its sender is untrusted. |
| E | body | NEW-REASON | BODY | meaning | L291-295 | 306 | E4,A1 | Interpretation with effects threatens reproducibility and safety. |
| E | body | NEW-REASON | BODY | meaning | L365-367 | 175 | E4 | Executable meaning requires recipient safety. |
| E | body | NEW-REASON | BODY | meaning | L389-390 | 124 | E4 | Questions to interpreters may have dangerous effects. |
| E | body | NEW-REASON | BODY | meaning | L435-439 | 209 | E4 | Transmitted meaning safety remains unanswered. |
| E | body | DISAGREES | BODY | meaning | L514-516 | 98 | E5,E6,X2 | Interpreter pointers on every fact are classified as required from inception. |
| E | body | NEW-REASON | BODY | meaning | L517-525 | 473 | E2,C2 | Crossings temporally qualify derivations. |
| E | body | NEW-CASE | BODY | meaning | L630-635 | 455 | E4 | ACL patches alternately introduced vulnerabilities and broke legitimate programs. |
| E | body | NEW-REASON | BODY | meaning | L652-656 | 309 | E4 | Least authority changes during execution and must arrive just in time. |
| E | body | NEW-CASE | BODY | meaning | L694-698 | 236 | E4 | Switching hats failed when more than two authorities were needed. |
| E | body | DISAGREES | BODY | meaning | L731-746 | 1169 | E4 | Grant provenance is classified as impossible to add later. |
| E | body | NEW-REASON | BODY | meaning | L769-778 | 674 | E4,C8 | Grant citation binds requests; local accountability does not prove authorship globally. |
| E | body | ABOVE | BODY | meaning | L779-788 | 658 | E4 | Capability purism and direct exposure in user interfaces can fail. |
| E | body | NEW-CASE | BODY | meaning | L1006-1017 | 867 | E5 | Explicit dependency upgrades proved difficult; Unison retired its replacement patches. |
| E | body | NEW-CASE | BODY | meaning | L1234-1239 | 393 | E5 | Machine-filled import provenance became noise and was disqualified as sourcing. |
| E | body | NEW-REASON | BODY | meaning | L1277-1283 | 384 | E5 | Usage and derivation have different semantics. |
| E | body | NEW-CASE | BODY | meaning | L1284-1292 | 689 | E5 | Unused mixed paint demonstrates that usage and generation do not establish derivation. |
| E | body | NEW-REASON | BODY | meaning | L1293-1294 | 138 | E5 | Generic influence should yield to more specific relations. |
| E | body | NEW-REASON | BODY | meaning | L1295-1303 | 593 | E5 | Mechanical usage is reliable but noisy; declared human or model dependence can be wrong. |
| E | body | NEW-REASON | BODY | meaning | L1318-1319 | 142 | E1 | Provenance bundles themselves support provenance. |
| E | body | DISAGREES | BODY | meaning | L1324-1332 | 580 | E6 | PROV permits omitted reads and leaves provenance granularity to applications. |
| E | body | NEW-REASON | BODY | meaning | L1482-1488 | 447 | E5 | Worlds pins touched slots while unread slots continue following parents. |
| E | body | NEW-REASON | BODY | meaning | L1498-1506 | 582 | E1,E5 | Admission positions let readers distinguish already-stale inputs from later changes. |
| E | body | DISAGREES | BODY | meaning | L1611-1613 | 128 | E2,E6 | Views read without recording crossings. |
| E | body | NEW-CASE | BODY | meaning | L1666-1678 | 941 | E5 | Erlang binds per call; loading a third version kills processes on the first. |
| E | body | NEW-CASE | BODY | meaning | L1679-1693 | 1104 | E1 | Contract-checker refusals repeatedly contradicted mistaken programmer expectations. |
| E | body | NEW-REASON | BODY | meaning | L1694-1697 | 239 | E1 | Refusals preserve unexpected reality, including expected and received values. |
| E | body | NEW-REASON | BODY | meaning | L1828-1835 | 533 | E5 | Reactive reads and snapshot reads require distinct operations. |
| E | body | NEW-CASE | BODY | meaning | L1847-1855 | 632 | E2,C2 | Convergence versions prevent unsettled computation from crossing into external effects. |
| E | body | ABOVE | BODY | meaning | L1860-1865 | 362 | E7 | State convergence deliberately omits intermediate events. |
| E | body | DISAGREES | BODY | meaning | L1866-1875 | 640 | E7,E2 | Hand motions vanish by default and shown content is unrecorded. |
| E | body | ABOVE | BODY | meaning | L1904-1915 | 798 | E4 | Linda deliberately omitted security and encapsulation for performance. |
| E | body | DISAGREES | BODY | meaning | L1940-1951 | 771 | E7 | Durable hand motions were replaced with transience and inconsistent throttling. |
| E | body | NEW-CASE | BODY | meaning | L1952-1963 | 811 | E1,C2,X4 | Historical restore bypassed current permissions; policy expiry created unrecorded enforcement lag. |
| E | body | DISAGREES | BODY | meaning | L2044-2051 | 552 | E6,C5 | Owners may withdraw documents; individual reader tracking must be impossible. |
| E | body | DISAGREES | BODY | meaning | L2052-2059 | 541 | E6,C5 | Reader tracking conflicts with consent and personal ownership of viewing records. |
| E | body | DISAGREES | BODY | meaning | L2306-2315 | 560 | E3 | Reed derives order from writer clocks and site ids. |
| E | body | DISAGREES | BODY | meaning | L2316-2324 | 581 | E3 | Claimed occurrence time is classified as a first-record requirement rather than an early loss. |
| E | body | DISAGREES | BODY | meaning | L2363-2373 | 610 | E6 | Several camps omit read records or forbid reader tracking. |
| E | body | NEW-REASON | BODY | meaning | L2383-2391 | 472 | E5 | Pinning and following are selected explicitly at each use. |
| E | body | NEW-REASON | BODY | meaning | L2392-2399 | 500 | E5 | Historical facts pin; actors declare dependence and may be mistaken. |
| E | body | NEW-REASON | BODY | meaning | L2465-2472 | 425 | E1 | Refusals expose expectation errors; policy versions explain decisions under lag. |
| E | body | NEW-REASON | BODY | meaning | L2473-2480 | 512 | E1 | Refusal bodies permit unauthorised storage growth and need offerer-owned quotas. |
| E | body | DISAGREES | BODY | meaning | L2491-2496 | 391 | E8,A2 | Requires session-start facts in addition to build facts. |
| E | body | DISAGREES | BODY | meaning | L2497-2507 | 598 | E7,E2 | Substrates omit motion and view history; Xanadu forbids reader tracking. |
| E | body | DISAGREES | BODY | meaning | L2508-2515 | 492 | E7 | Pointing is transient or trimmable; shown cannot establish looked. |
| E | body | ABOVE | BODY | meaning | L2516-2522 | 275 | E4,C8 | Reframes acting in a person's name as scoped delegation. |
| E | body | DISAGREES | BODY | meaning | L2523-2528 | 330 | E4 | Invoked grants are classified as mandatory from the first fact. |
| E | body | DISAGREES | BODY | meaning | L2555-2561 | 547 | E4 | Authority provenance classified as impossible to retrofit. |
| E | body | CARRIED | BODY | rama | L114-114 | 286 | E3,C2 | Q2; depot leader stamps append time, but topology access is undocumented. |
| E | body | CARRIED | BODY | rama | L115-115 | 215 | E3 | Q2; tutorial obtains time inside task execution. |
| E | body | CARRIED | BODY | rama | L116-116 | 126 | E3 | Q2; topology clock wrapper permits controlled test time. |
| E | body | CARRIED | BODY | rama | L119-120 | 589 | E3,E1 | Q2; leader changes and retries alter clocks unless the first verdict is reused. |
| E | body | NEW-REASON | BODY | rama | L149-150 | 593 | E6 | Unlogged reads multiply fact size; depot fetch guidance assumes roughly fifty kilobytes. |
| E | body | NEW-REASON | BODY | rama | L186-186 | 148 | E1 | Append exceptions leave successful admission uncertain. |
| E | body | NEW-CASE | BODY | rama | L187-187 | 220 | E1 | Throttle exceptions occur after appends have landed. |
| E | body | NEW-REASON | BODY | rama | L189-190 | 196 | E1 | Deterministically malformed records otherwise block microbatch processing indefinitely. |
| E | body | DISAGREES | BODY | rama | L191-192 | 780 | E1 | Kept verdicts are required for retry correctness, rather than optional early history. |
| E | body | NEW-REASON | BODY | rama | L193-194 | 154 | E1 | Fact and verdict atomicity depends on event locality or microbatch scope. |
| E | body | NEW-REASON | BODY | rama | L195-196 | 585 | E1 | Microbatch verdicts require polling or subscriptions; append acknowledgements establish durability only. |
| E | body | NEW-REASON | BODY | rama | L208-209 | 303 | E7 | Pointer facts consume the same per-task stream execution budget as other writes. |
| E | body | NEW-REASON | BODY | rama | L227-227 | 112 | E1,E7 | Interactive gate latency differs by hundreds of milliseconds. |
| E | body | NEW-REASON | BODY | rama | L228-228 | 134 | E1,C1 | Only microbatch PState writes provide exactly-once delivery. |
| E | body | NEW-REASON | BODY | rama | L230-230 | 64 | E1 | Stream returns verdicts directly; microbatch requires separate reads. |
| E | body | NEW-REASON | BODY | rama | L233-233 | 96 | E1 | A malformed record can block the entire microbatch topology. |
| E | body | NEW-REASON | BODY | rama | L237-238 | 187 | E1,C3 | Microbatch preference is conditional on accepting greater update latency. |
| E | body | DISAGREES | BODY | rama | L239-240 | 1093 | E1,C1 | Retained verdicts become first-record requirements under stream, while microbatch allegedly permits skipping them; also ABOVE. |
| E | body | DISAGREES | BODY | rama | L295-296 | 585 | E3 | Earlier designs order facts by timestamps; later designs use partition positions. |
| E | body | CARRIED | BODY | rama | L299-300 | 551 | E3,A1 | Q2; nondeterministic PState data prevents reconstruction from depot inputs. |
| E | body | NEW-CASE | BODY | rama | L303-304 | 401 | E1,A3 | Collaborative-editor version checks omitted retry handling later required by guidance. |
| E | body | DISAGREES | BODY | rama | L319-320 | 326 | E3 | Earlier timestamp-based ordering conflicts; later source and gate timestamps agree. |
| E | body | DISAGREES | BODY | rama | L323-324 | 1427 | E6 | Reads belong in execution traces keyed by run, not crossing-pattern records; also ABOVE. |
| E | body | NEW-REASON | BODY | rama | L344-344 | 27 | E6 | Marz's earlier practice supplies no read-provenance experience. |
| E | body | DISAGREES | BODY | rama | L407-408 | 362 | E3 | Includes Spanner's physical-time alternative to logical ordering. |
| E | body | DISAGREES | BODY | skeptics | L42-43 | 846 | E3 | Reported clock-ordering practice conflicts with E3; the quoted default creed agrees. |
| E | body | DISAGREES | BODY | skeptics | L48-49 | 293 | E6,E5 | Read provenance is absent or kept in separate, expiring audit logs. |
| E | body | DISAGREES | BODY | skeptics | L50-51 | 99 | E5 | No read recording means no dependence or path distinctions. |
| E | body | DISAGREES | BODY | skeptics | L54-55 | 462 | E5,E6 | Causal read links are sampled and expire instead of being kept. |
| E | body | DISAGREES | BODY | skeptics | L60-61 | 432 | E1 | Refusals expire and the journal retains only accepted work; also CARRIED Q1. |
| E | body | DISAGREES | BODY | skeptics | L64-65 | 149 | E7 | Hand events are sampled outside the system of record. |
| E | body | DISAGREES | BODY | skeptics | L66-67 | 349 | E4 | Broad, once-granted OAuth scopes replace authority designated by each click. |
| E | body | NEW-REASON | BODY | skeptics | L117-118 | 725 | E4 | Agent database branching and account restrictions provide practical safety mechanisms. |
| E | body | ABOVE | BODY | skeptics | L119-120 | 286 | E4 | Questions needing a new store for agent isolation and least privilege. |
| E | body | DISAGREES | BODY | skeptics | L157-158 | 824 | E3,C2,C3 | Commit timestamps order writes across gates; also CARRIED Q3 on completeness frontiers. |
| E | body | DISAGREES | BODY | skeptics | L159-160 | 696 | E3,C2 | Physical-time ordering simplifies consistent reads but makes correctness depend on clock accuracy. |
| E | body | DISAGREES | BODY | skeptics | L161-162 | 672 | E3,E1,C3,C2 | Uses gate clocks for order and omits refusals; also NEW-REASON on hot-cell retries. |
| E | body | NEW-REASON | BODY | skeptics | L169-170 | 690 | E6,A1 | Constant work avoids load-sensitive modes and cache-empty instability. |
| E | body | ABOVE | BODY | skeptics | L171-172 | 885 | E6,C1 | Questions change-driven recomputation under ingestion bursts; also CARRIED Q9 on separating identity from hashes. |
| E | body | NEW-REASON | BODY | skeptics | L224-225 | 332 | E3,C2 | Clock uncertainty directly increases waiting costs. |
| E | body | DISAGREES | BODY | skeptics | L238-238 | 216 | E3 | Allows bounded-clock ordering, which E3 prohibits. |
| E | body | DISAGREES | BODY | skeptics | L240-240 | 281 | E3,C2,C3 | DSQL uses monotone clock order; also CARRIED Q7 on partitioning and cross-shard atomicity. |
| E | body | DISAGREES | BODY | skeptics | L241-241 | 188 | E6,E5 | The default omits reads, while cited systems preserve only selected freshness dependencies. |
| E | body | DISAGREES | BODY | skeptics | L244-244 | 128 | E5,E6 | Causal trace links are sampled and expire. |
| E | body | DISAGREES | BODY | skeptics | L247-247 | 213 | E1 | Refusals expire and journals retain committed work only; also CARRIED Q1. |
| E | body | DISAGREES | BODY | skeptics | L249-249 | 66 | E7 | Hand events remain in a separate analytics pipeline. |
| E | body | DISAGREES | BODY | skeptics | L250-250 | 82 | E4 | Broad OAuth scopes replace specifically invoked click authority. |
| E | body | DISAGREES | BODY | skeptics | L258-258 | 299 | E3,C2 | DSQL demonstrates trusted wall-clock ordering instead of excluding clock-derived order. |
| E | body | NEW-REASON | BODY | sync | L147-149 | 178 | E1,A3 | Expected query results generalize version checks. |
| E | body | NEW-REASON | BODY | sync | L158-160 | 211 | E3 | Portable machines make synchronized clocks impractical. |
| E | body | DISAGREES | BODY | sync | L171-178 | 639 | E6,E8 | Session maintains literal write-id sets instead of crossing patterns. |
| E | body | DISAGREES | BODY | sync | L207-209 | 223 | E1 | Refusal history belongs to application convention. |
| E | body | NEW-REASON | BODY | sync | L220-223 | 245 | E1,E6 | Server-computed dependency queries generalize expected-version checks. |
| E | body | DISAGREES | BODY | sync | L230-232 | 219 | E1 | Application error convention replaces mandatory stored verdicts. |
| E | body | NEW-REASON | BODY | sync | L237-237 | 74 | E3 | Server-local monotonic stamps need no clock agreement. |
| E | body | NEW-REASON | BODY | sync | L267-271 | 290 | E6 | Runtime read sets serve commit conflicts and live-query reruns. |
| E | body | NEW-REASON | BODY | sync | L277-279 | 177 | E6 | Runtime records scanned index ranges. |
| E | body | NEW-REASON | BODY | sync | L280-284 | 351 | E6 | One overlap algorithm handles admission conflicts and subscriptions. |
| E | body | NEW-REASON | BODY | sync | L288-290 | 229 | E3 | Committer assigns monotonic admission timestamps. |
| E | body | NEW-REASON | BODY | sync | L303-305 | 219 | E3 | Commit and display clocks carry different guarantees. |
| E | body | NEW-REASON | BODY | sync | L306-312 | 478 | E1,A3 | Commit validates every read version. |
| E | body | NEW-REASON | BODY | sync | L341-346 | 440 | E5,E6 | Actor determinism distinguishes dependency from unprovable human or model dependence. |
| E | body | NEW-REASON | BODY | sync | L351-352 | 128 | E3 | Ordering and display clocks have distinct guarantees. |
| E | body | DISAGREES | BODY | sync | L368-373 | 384 | E6 | Read sets remain in memory instead of durable crossing history. |
| E | body | NEW-REASON | BODY | sync | L395-400 | 469 | E3,A1 | External inputs coordinate deterministic simulation time. |
| E | body | DISAGREES | BODY | sync | L412-417 | 451 | E7 | View-only hand events remain unrecorded. |
| E | body | DISAGREES | BODY | sync | L441-448 | 595 | E8 | Session-start runtime record replaces epoch-and-crossing unit. |
| E | body | DISAGREES | BODY | sync | L450-451 | 144 | E7 | Only shared model changes persist. |
| E | body | NEW-REASON | BODY | sync | L452-453 | 116 | E3,A1 | Model execution uses only reflector time. |
| E | body | NEW-REASON | BODY | sync | L500-502 | 222 | E1 | Admission must enforce cross-record acyclicity. |
| E | body | ABOVE | BODY | sync | L567-570 | 254 | E1 | Three admission checks omit cross-cell invariants. |
| E | body | DISAGREES | BODY | sync | L621-622 | 146 | E1 | Rejected transactions are removed. |
| E | body | DISAGREES | BODY | sync | L628-636 | 606 | E1,C6 | Refusals vanish; rebasing loses original replacement intent. |
| E | body | NEW-REASON | BODY | sync | L669-675 | 512 | E4,E7 | Mutator arguments preserve intent while speculative outputs disappear. |
| E | body | DISAGREES | BODY | sync | L686-689 | 274 | E2 | Client-view history may be discarded and rebuilt. |
| E | body | NEW-REASON | BODY | sync | L690-693 | 300 | E4,A3 | Central rejection provides fine-grained authorization. |
| E | body | DISAGREES | BODY | sync | L708-712 | 319 | E1 | Failure advances counter without retained refusal data. |
| E | body | NEW-REASON | BODY | sync | L731-735 | 375 | E4,E7 | Click intent preserves action, arguments, and displayed context. |
| E | body | DISAGREES | BODY | sync | L739-743 | 252 | E2,E1 | View records are throwaway; mutation log is not audit history. |
| E | body | DISAGREES | BODY | sync | L758-763 | 426 | E7 | Selection is transient presence; camera state remains local. |
| E | body | DISAGREES | BODY | sync | L778-785 | 509 | E7,C1,C4 | Selection is unsaved; IDs encode types; kinds use words. |
| E | body | NEW-REASON | BODY | sync | L923-925 | 213 | E5,P0 | Missing dependencies defer delivery and must survive persistence. |
| E | body | NEW-CASE | BODY | sync | L935-939 | 305 | E4,E2 | Content hash prevents publishing after approval becomes stale. |
| E | body | NEW-CASE | BODY | sync | L1029-1039 | 770 | E2,E5,A1 | View history pins document heads but misses tool heads. |
| E | body | NEW-REASON | BODY | sync | L1063-1066 | 186 | E1 | Refusal returns timestamp needed for repair. |
| E | body | NEW-REASON | BODY | sync | L1070-1071 | 106 | E7 | Persistent UI state improved user experience. |
| E | body | NEW-CASE | BODY | sync | L1072-1077 | 415 | E7,C4 | Persisting UI state forced layout migrations. |
| E | body | DISAGREES | BODY | sync | L1084-1089 | 414 | E7 | Hand state remains ephemeral without document-version association. |
| E | body | DISAGREES | BODY | sync | L1133-1135 | 215 | E7 | Recommends not persisting the hand. |
| E | body | NEW-REASON | BODY | sync | L1136-1137 | 141 | E5,A1 | Pinned display history also needs tool versions. |
| E | body | NEW-REASON | BODY | sync | L1138-1139 | 98 | E3,C3 | Canonical order lacks objective temporal precedence. |
| E | body | NEW-REASON | BODY | sync | L1140-1142 | 136 | E1 | Refusal should include repair information. |
| E | body | NEW-REASON | BODY | sync | L1231-1234 | 301 | E5,E6 | Captured parents determine an operation's interpretation version. |
| E | body | NEW-REASON | BODY | sync | L1251-1254 | 226 | E6 | Implicit common parents compress provenance. |
| E | body | NEW-REASON | BODY | sync | L1270-1271 | 106 | E5,E6 | Capture each action's interpretation context mechanically. |
| E | body | NEW-REASON | BODY | sync | L1277-1279 | 113 | E6,C7 | Common parent links can be implicit in encoding. |
| E | body | NEW-CASE | BODY | sync | L1449-1453 | 351 | E3 | Combining claimed and indexed times causes ordering confusion. |
| E | body | NEW-REASON | BODY | sync | L1502-1503 | 105 | E5,C1 | Reference authors choose floating or pinned targets. |
| E | body | NEW-CASE | BODY | sync | L1519-1520 | 107 | E3 | Combining clocks causes display regret. |
| E | body | DISAGREES | BODY | sync | L1553-1557 | 348 | E3,P0 | Author clocks choose versions; old versions may disappear. |
| E | body | DISAGREES | BODY | sync | L1558-1559 | 107 | E1 | Refusal is a transient response. |
| E | body | DISAGREES | BODY | sync | L1616-1617 | 91 | E3 | Author clock determines winning version. |
| E | body | DISAGREES | BODY | sync | L1699-1704 | 355 | E3 | Author timestamps choose overwrites within subspaces. |
| E | body | NEW-REASON | BODY | sync | L1713-1714 | 98 | E5,C4 | Pinning policy belongs to field grammar. |
| E | body | NEW-REASON | BODY | sync | L1725-1727 | 149 | E3 | Central clocks still need bounded damage. |
| E | body | NEW-REASON | BODY | sync | L1808-1813 | 414 | E3 | Corrected dates follow causal parents. |
| E | body | NEW-CASE | BODY | sync | L1852-1856 | 376 | E3,C2 | Missing generation metadata required a versioned side index. |
| E | body | NEW-REASON | BODY | sync | L1872-1875 | 274 | E3 | Gate time can be monotone along read dependencies. |
| E | body | NEW-REASON | BODY | sync | L1906-1909 | 258 | E3,C8 | Operation history includes host and predecessors. |
| E | body | DISAGREES | BODY | sync | L2016-2019 | 189 | E3 | Writer date determines winning tag. |
| E | body | NEW-REASON | BODY | sync | L2188-2191 | 275 | E5 | Tools may add semantic dependencies. |
| E | body | NEW-CASE | BODY | sync | L2205-2209 | 325 | E5 | Split strict dependencies from known context after shipping. |
| E | body | DISAGREES | BODY | sync | L2549-2560 | 767 | E3 | Nostr orders winners by author clock. |
| E | body | NEW-REASON | BODY | sync | L2635-2637 | 217 | E6 | Runtime capture makes provenance mechanically attributable. |
| E | body | NEW-REASON | BODY | sync | L2638-2639 | 117 | E6 | Ranges include future insertions. |
| E | body | NEW-REASON | BODY | sync | L2644-2645 | 145 | E6 | Frontiers and implicit parents compress provenance. |
| E | body | DISAGREES | BODY | sync | L2646-2649 | 186 | E2 | Rebuildable view histories may be nondurable. |
| E | body | NEW-REASON | BODY | sync | L2665-2668 | 197 | E6,C2 | Pattern-cut honesty depends on truthful index position. |
| E | body | NEW-CASE | BODY | sync | L2673-2674 | 134 | E5 | Pijul split dependencies after shipping. |
| E | body | NEW-REASON | BODY | sync | L2675-2677 | 153 | E5,C6 | Exposure and replacement contain different information. |
| E | body | NEW-REASON | BODY | sync | L2678-2679 | 127 | E5 | Deterministic tools depend on captured reads. |
| E | body | NEW-REASON | BODY | sync | L2683-2684 | 125 | E5 | Pinned history coexists with latest-version reopening. |
| E | body | NEW-REASON | BODY | sync | L2827-2830 | 235 | E1,E4 | Authorization can replay from recorded causal past. |
| E | body | DISAGREES | BODY | sync | L2833-2835 | 215 | E1 | Refusals discarded or left to applications. |
| E | body | NEW-REASON | BODY | sync | L2836-2837 | 105 | E1 | Refusal includes repair information. |
| E | body | NEW-CASE | BODY | sync | L2838-2839 | 125 | E4 | Hash approval blocks changed content. |
| E | body | DISAGREES | BODY | sync | L2891-2903 | 898 | E7 | Camp keeps selection and pointer ephemeral. |
| E | body | NEW-REASON | BODY | sync | L2987-2992 | 404 | E6,A1,C1 | Runtime ranges unite invalidation and conflicts; determinism and encoded IDs impose costs. |
| E | body | NEW-REASON | BODY | sync | L3036-3039 | 300 | E5,C8 | Provenance vocabulary distinguishes derivation, exposure, and delegation. |
| A | main | NEW-REASON | ABOVE | datalog | L598-599 | 533 | A1,E2 | Temporally pinned crossings turn changing derivations into attestable events. |
| A | main | NEW-REASON | R2 | datalog | L1022-1023 | 212 | A1,E2 | Crossings make live derivations temporally qualified facts. |
| A | main | ABOVE | ABOVE | frontiers | L421-421 | 409 | A1,E2 | Rejects never holding derived state; crossings also need versions for reproducibility. |
| A | main | ABOVE | SHORT | log | L44-44 | 771 | A1 | Challenges never storing running answers through playback costs. |
| A | main | ABOVE | SHORT | log | L45-46 | 492 | - | Challenges event sourcing as the substance of the entire system. |
| A | main | NEW-REASON | SHORT | log | L51-52 | 617 | A1,P0 | Capture nondeterministic inputs before logging to make replay deterministic. |
| A | main | ABOVE | ABOVE | log | L542-543 | 973 | - | Challenges immutable small facts as one substance for the entire system. |
| A | main | ABOVE | ABOVE | log | L550-551 | 837 | A1 | Rejects never storing running answers; proposes disposable derived state stamped with applied positions. |
| A | main | DISAGREES | ABOVE | log | L554-555 | 392 | A3,W4 | Requires exactly one active gate per cell; also challenges one writer for the whole system. |
| A | main | ABOVE | R2 | log | L713-714 | 201 | A1 | Reconsiders playback objection because summaries are already facts and indexes are stored. |
| A | main | ABOVE | ABOVE | meaning | L2641-2650 | 671 | - | One-fact gates cannot protect invariants spanning facts; single-substance limits remain. |
| A | main | ABOVE | ABOVE | meaning | L2651-2659 | 615 | - | Internal tools and policies need meta-level fences and protection from historical policy bypass. |
| A | main | ABOVE | ABOVE | meaning | L2660-2665 | 365 | A1,C2 | Unstored answers require enforced determinism; indexes are already stored derived answers. |
| A | main | ABOVE | ABOVE | meaning | L2681-2682 | 149 | - | One record can contain plural beliefs. |
| A | main | ABOVE | ABOVE | meaning | L2683-2684 | 148 | - | Reframes meaning around obligations to absent readers. |
| A | main | NEW-REASON | R2 | meaning | L2952-2959 | 443 | A2,A1 | A recorded build name cannot recover an unavailable interpreter. |
| A | main | NEW-CASE | R2 | meaning | L2975-2986 | 864 | A1,A2 | Changed ordering or numbers silently alter historical staleness without reference semantics. |
| A | main | ABOVE | R2 | meaning | L3139-3145 | 555 | - | External substrate proliferation challenges one substance; NEW-CORNER: record each part's version and authority. |
| A | main | ABOVE | R2 | meaning | L3150-3152 | 99 | - | Per-fact gates cannot protect rules spanning facts. |
| A | main | ABOVE | ABOVE | rama | L457-458 | 435 | A1 | Running answers may be stored safely as recomputable views. |
| A | main | ABOVE | ABOVE | rama | L461-462 | 123 | A3 | One writer is understood per partition rather than globally. |
| A | main | ABOVE | R2 | rama | L621-621 | 337 | - | Leader-only reads prevent replicas from bringing service closer to distant users. |
| A | main | ABOVE | R2 | rama | L623-623 | 99 | - | Node separation supplies no documented regional placement guarantee. |
| A | main | ABOVE | R2 | rama | L624-624 | 176 | - | Cluster-wide coordination includes a five-second leader-election session timeout. |
| A | main | ABOVE | R2 | rama | L626-626 | 119 | A2 | Module-wide updates pause everyone's appends. |
| A | main | ABOVE | R2 | rama | L631-634 | 178 | - | Per-customer deployments leave worldwide behavior unestablished. |
| A | main | ABOVE | R2 | rama | L637-637 | 119 | - | Requests evidence for tested cross-region round trips. |
| A | main | ABOVE | R2 | rama | L641-641 | 189 | - | Questions network timeout assumptions at worldwide latencies. |
| A | main | ABOVE | R2 | rama | L646-646 | 83 | A2 | Questions whether worldwide module updates can roll region by region. |
| A | main | ABOVE | R2 | rama | L649-650 | 141 | - | Questions public-service licensing and a mandatory front tier. |
| A | main | NEW-REASON | R2 | rama | L718-718 | 237 | A2,C8 | Authentication tier is runtime code whose build must be attributable. |
| A | main | ABOVE | R2 | rama | L719-719 | 196 | A3,P0 | Writer uniqueness depends on whether the permanent object is intake, rows, or results. |
| A | main | ABOVE | ABOVE | skeptics | L281-281 | 353 | A1,E6 | Questions the operational cost of having to recompute all unstored answers simultaneously. |
| A | main | CARRIED | ABOVE | skeptics | L283-284 | 240 | A3,C3 | Q4: durable checks must survive overlapping writers during failover. |
| A | main | ABOVE | ABOVE | skeptics | L285-286 | 861 | - | Requires demonstrated gains sufficient to justify operating a new store. |
| A | main | CARRIED | R2 | skeptics | L442-442 | 166 | A3 | Q4: durable version checks must reject stale gates during overlapping failover. |
| A | main | ABOVE | SHORT | sync | L123-130 | 470 | - | Challenges committed-only display through latency, starvation, and unused commit-only views. |
| A | main | ABOVE | ABOVE | sync | L3056-3069 | 983 | - | One substance faces text contention, invariants, and UI migrations. |
| A | main | ABOVE | ABOVE | sync | L3082-3095 | 929 | - | Planetary store raises exit, trust, availability, and jurisdiction questions. |
| A | main | ABOVE | ABOVE | sync | L3111-3125 | 1032 | A1,E2 | Retained reads and digest cannot ensure future historical rendering. |
| A | main | ABOVE | ABOVE | sync | L3136-3143 | 542 | A3 | One writer belongs to an ordering domain, not planet. |
| A | main | ABOVE | R2 | sync | L3339-3344 | 345 | - | Nearby gate weakens latency objection. |
| A | main | DISAGREES | R2 | sync | L3364-3369 | 330 | A3,C3 | Requires exclusive layer-gate ownership and layer-bearing positions. |
| A | body | NEW-REASON | BODY | datalog | L101-104 | 485 | A3 | Acquiring novelty requires coordination and admission rules. |
| A | body | NEW-REASON | BODY | datalog | L145-148 | 546 | A1 | Repeated live derivations can produce internally inconsistent calculations. |
| A | body | NEW-REASON | BODY | datalog | L149-150 | 369 | A1,W1 | Immutable source segments support caching without caching changing answers. |
| A | body | ABOVE | BODY | datalog | L177-178 | 611 | A2 | Code moved from database facts toward external commit-identified builds. |
| A | body | CARRIED | BODY | datalog | L253-253 | 220 | A3 | Q4: concurrent gates remain safe through previous-position CAS. |
| A | body | NEW-REASON | BODY | datalog | L254-254 | 561 | A3,X1 | Cell-local CAS cannot enforce atomic invariants spanning multiple cells. |
| A | body | NEW-REASON | BODY | datalog | L307-307 | 249 | A2 | Nubank records the service commit on every transaction. |
| A | body | NEW-REASON | BODY | datalog | L308-308 | 336 | A2 | Commit identity and explicit unreproducibility distinguish runtime builds. |
| A | body | NEW-REASON | BODY | frontiers | L69-70 | 495 | A1 | Clock-dependent views require explicit future validity boundaries. |
| A | body | NEW-CASE | BODY | frontiers | L76-76 | 392 | A2,C2 | Virtual-time configuration cutovers originated in live reconfiguration. |
| A | body | NEW-REASON | BODY | frontiers | L85-85 | 340 | A3,W4 | Multiple checking gates require a separate lightweight time-assignment authority. |
| A | body | NEW-REASON | BODY | frontiers | L164-165 | 368 | A1,E2 | Model-shaped answers may lack the batch oracle used to check relational outputs. |
| A | body | NEW-REASON | BODY | frontiers | L237-238 | 864 | A1,C1 | Replay logs offsets and checksums for rereadable inputs, but full data for transient inputs. |
| A | body | NEW-REASON | BODY | frontiers | L239-240 | 714 | A1 | Reproducibility additionally depends on logged now, processor architecture, and compiler optimizations. |
| A | body | NEW-REASON | BODY | frontiers | L241-242 | 246 | A1,X1 | Replay must preserve batch cuts to preserve the sequence of output changes. |
| A | body | ABOVE | BODY | frontiers | L289-289 | 301 | A1 | Challenges never-held running answers: discarding all maintained state requires rescanning history. |
| A | body | NEW-REASON | BODY | frontiers | L319-319 | 480 | A3,E1 | Replicating interpreted facts avoids distributing authority over the same intent. |
| A | body | ABOVE | BODY | frontiers | L332-332 | 310 | A3,E1 | Questions multiple authorities interpreting identical offers; federation should carry interpreted facts. |
| A | body | DISAGREES | BODY | frontiers | L367-368 | 450 | A3,C3,C2 | Forbids two gates on register cells; CARRIED Q7 co-location and Q5 partition starts. |
| A | body | NEW-REASON | BODY | frontiers | L383-384 | 200 | A1,A2 | Bit-exact replay depends on machine architecture and compiler as well as runtime events. |
| A | body | NEW-REASON | BODY | frontiers | L401-402 | 271 | A1 | Re-derivation depends on machine, compiler, logged now, and batch cuts. |
| A | body | ABOVE | BODY | log | L70-70 | 181 | A1 | Readable state stored as a disposable log-derived cache. |
| A | body | ABOVE | BODY | log | L98-99 | 456 | A1 | Challenges transferring private mutable inside-state designs to one shared immutable store. |
| A | body | ABOVE | BODY | log | L115-115 | 404 | - | Log reconfiguration requires consensus metadata outside the log to survive log unavailability. |
| A | body | NEW-REASON | BODY | log | L117-117 | 349 | A2 | Logged activation synchronizes new runtime behavior at one position. |
| A | body | NEW-REASON | BODY | log | L120-121 | 288 | A1 | Stored inputs versus outputs trade bandwidth against CPU; intervening writes can invalidate outputs. |
| A | body | ABOVE | BODY | log | L124-124 | 348 | A1 | Playback limited shared-log applicability to few clients or small state. |
| A | body | NEW-CASE | BODY | log | L127-127 | 470 | A2,A1 | Production inconsistencies came from engine rollout; two-phase activation and checksums addressed them. |
| A | body | NEW-REASON | BODY | log | L139-139 | 178 | A2,A1 | Logged code changes plus derived-state checksums make runtime transitions checkable. |
| A | body | NEW-REASON | BODY | log | L158-158 | 222 | A1,E2 | External-call results must be captured when events are made for deterministic replay. |
| A | body | ABOVE | BODY | log | L168-168 | 440 | - | Event sourcing across an entire system is called an architectural anti-pattern. |
| A | body | NEW-REASON | BODY | log | L180-180 | 104 | A1,E2 | Recording computed outputs is an alternative to relying on software-version replay. |
| A | body | ABOVE | BODY | log | L193-193 | 175 | A1 | Materialized views store derived state and remain rebuildable. |
| A | body | ABOVE | BODY | log | L205-205 | 130 | - | Rejects a single universally correct architecture. |
| A | body | ABOVE | BODY | log | L223-224 | 288 | - | Local primary copies and merging challenge one shared truth governed by a gate. |
| A | body | ABOVE | BODY | log | L265-266 | 244 | A3 | Independent stores and client receipt quorums replace one globally trusted store. |
| A | body | ABOVE | BODY | log | L267-268 | 388 | - | CT's public, independent stores differ from one shared store with private layers. |
| A | body | NEW-REASON | BODY | log | L295-295 | 158 | A3,W4 | Sequential certification limits parallelism even when parts can run concurrently. |
| A | body | NEW-CASE | BODY | log | L297-297 | 187 | A3,W4 | Heavily optimized Hyder certification remained the transaction-throughput bottleneck. |
| A | body | ABOVE | BODY | log | L317-317 | 295 | A1 | Storage retains derived page images built from redo logs. |
| A | body | DISAGREES | BODY | log | L319-319 | 456 | A3,W4 | One active writer is required to simplify isolation, ordering, and atomicity. |
| A | body | CARRIED | BODY | log | L322-323 | 377 | A3,E8,P0 | Q4: durable epoch-stamped truncation records fence deposed writers. |
| A | body | DISAGREES | BODY | log | L329-329 | 333 | A3,W4 | Permits multiple gates only across distinct keys, never concurrently on one cell. |
| A | body | ABOVE | BODY | log | L353-354 | 315 | A1 | Incrementally stored PStates replaced routine full recomputation. |
| A | body | ABOVE | BODY | log | L388-389 | 274 | - | Current-log pointers and trust lists must exist outside the log. |
| A | body | DISAGREES | BODY | log | L431-432 | 327 | A3,W4 | Forbids simultaneous deciders on one key even with fencing. |
| A | body | DISAGREES | BODY | log | L437-438 | 537 | A3,W4 | Requires exactly one gate per partition; also Q5 about position-layout dependence. |
| A | body | NEW-CASE | BODY | log | L502-503 | 739 | A2,A1 | Engine rollout caused production divergence; logged activation and incremental checksums addressed it. |
| A | body | NEW-REASON | BODY | log | L504-505 | 763 | A1,A2,E2 | Runtime versions and answer-affecting machine properties bound reproducible crossings. |
| A | body | ABOVE | BODY | log | L528-529 | 280 | - | Practitioners' architectural regrets challenge the overall event-sourcing premise. |
| A | body | ABOVE | BODY | meaning | L96-99 | 85 | - | Questions data as the foundation. |
| A | body | ABOVE | BODY | meaning | L129-146 | 862 | - | Active interpreters must accompany messages. |
| A | body | ABOVE | BODY | meaning | L147-178 | 1880 | - | Interpretation remains separate and plural; richer messages still bottom out in data. |
| A | body | ABOVE | BODY | meaning | L201-203 | 222 | - | Autonomous ambassadors negotiate with unknown recipients. |
| A | body | ABOVE | BODY | meaning | L212-216 | 354 | - | Interpreter regress remains unresolved. |
| A | body | NEW-CASE | BODY | meaning | L220-223 | 311 | A1,A2 | Reviving the 1978 image depended on its virtual machine. |
| A | body | ABOVE | BODY | meaning | L296-304 | 619 | - | Dynamic ambassadors cannot substitute for recorded facts. |
| A | body | ABOVE | BODY | meaning | L305-310 | 325 | - | Object and place orientation blamed for deficient recordkeeping. |
| A | body | ABOVE | BODY | meaning | L316-322 | 524 | - | Meaning requires processes beyond programmer assumptions. |
| A | body | ABOVE | BODY | meaning | L323-324 | 93 | - | Data alleged to scale terribly. |
| A | body | ABOVE | BODY | meaning | L325-327 | 185 | - | Abstract data types and assignment alleged to scale poorly. |
| A | body | ABOVE | BODY | meaning | L333-337 | 386 | - | Meaning negotiation proposed beyond message sending. |
| A | body | ABOVE | BODY | meaning | L342-357 | 797 | - | Absent writers motivate travelling interpreters without excluding alternate readings. |
| A | body | NEW-CASE | BODY | meaning | L368-371 | 168 | A1,A2 | Media can carry a small recoverable machine. |
| A | body | ABOVE | BODY | meaning | L379-385 | 484 | - | Interpretation is separate; interpreter code is itself data. |
| A | body | ABOVE | BODY | meaning | L395-397 | 95 | - | Object and place orientation blamed for poor recordkeeping. |
| A | body | ABOVE | BODY | meaning | L418-426 | 588 | - | Labels and interpreters expose competing bootstrap assumptions. |
| A | body | ABOVE | BODY | meaning | L427-434 | 467 | - | Ambassadors risk substituting dynamic objects for facts. |
| A | body | ABOVE | BODY | meaning | L445-446 | 148 | - | Interpreter bootstrap remains unresolved. |
| A | body | ABOVE | BODY | meaning | L452-456 | 275 | - | Writers cannot fix future interpretations. |
| A | body | ABOVE | BODY | meaning | L457-469 | 690 | - | Reframes the question around obligations to absent readers. |
| A | body | ABOVE | BODY | meaning | L562-569 | 540 | - | Questions data scaling while recognising matching and internal rebuilding. |
| A | body | ABOVE | BODY | meaning | L701-706 | 423 | - | Shared inbox routing broke actor boundaries; authority retrofit did not land. |
| A | body | ABOVE | BODY | meaning | L789-805 | 1034 | - | Shared heaps require global knowledge and expensive federation fan-out. |
| A | body | NEW-CASE | BODY | meaning | L909-914 | 399 | A3 | Single-threaded races required delaying outbound messages until durable writes completed. |
| A | body | ABOVE | BODY | meaning | L1161-1165 | 375 | - | Multiple attributed claims replace one truth; provenance is optional. |
| A | body | ABOVE | BODY | meaning | L1440-1450 | 777 | - | Scalable autonomous intermodule meaning negotiation remains unbuilt. |
| A | body | NEW-REASON | BODY | meaning | L1474-1481 | 490 | A1,A2 | Optimisation displaced completion of the underlying runtime. |
| A | body | NEW-REASON | BODY | meaning | L1489-1493 | 374 | A3,E5 | Commit validates every read to prevent publishing work based on changed inputs. |
| A | body | ABOVE | BODY | meaning | L1518-1520 | 127 | - | External operating-system substance should disappear. |
| A | body | ABOVE | BODY | meaning | L1528-1534 | 397 | - | Single-substance systems leave scale, attribution, erasure, and meta-level fences unresolved. |
| A | body | NEW-REASON | BODY | meaning | L1558-1561 | 312 | A3,E5 | Read timestamps reject retroactive writes that would invalidate returned values. |
| A | body | ABOVE | BODY | meaning | L1564-1566 | 139 | A3 | Mutual exclusion requires prior knowledge or central registration. |
| A | body | NEW-REASON | BODY | meaning | L1576-1578 | 97 | A3 | Commit-record ownership creates a critical resource. |
| A | body | NEW-REASON | BODY | meaning | L1592-1596 | 270 | A1,C3 | Ordered input plus deterministic computation supports unstored derived state. |
| A | body | NEW-REASON | BODY | meaning | L1605-1610 | 358 | A1 | Clock reads and accidental model writes require runtime detection. |
| A | body | ABOVE | BODY | meaning | L1654-1665 | 770 | - | Shared function stores raise distributed update and binding questions. |
| A | body | ABOVE | BODY | meaning | L1713-1719 | 483 | - | Prototypes avoid the class meta-regress. |
| A | body | ABOVE | BODY | meaning | L1776-1784 | 554 | - | Plural information has no central truth arbiter and requires sponsors. |
| A | body | ABOVE | BODY | meaning | L1785-1791 | 451 | - | One truth can mean a shared attribution record, not consistent beliefs. |
| A | body | ABOVE | BODY | meaning | L1792-1797 | 352 | - | Arrival decisions cannot be deduced from facts alone. |
| A | body | ABOVE | BODY | meaning | L1876-1880 | 269 | - | Independent local communities replace a central planetary store. |
| A | body | NEW-CASE | BODY | meaning | L1881-1889 | 593 | A1,A2 | Repeated incompatible runtime rewrites were affordable only because little persisted. |
| A | body | ABOVE | BODY | meaning | L1890-1894 | 239 | - | Matching among mutually untrusted parties lacks precedent. |
| A | body | ABOVE | BODY | meaning | L1895-1903 | 508 | - | Destructive, nondeterministically matched tuples differ from persistent attributed facts. |
| A | body | ABOVE | BODY | meaning | L2091-2099 | 591 | - | Xanadu engineers sought multiple operators to prevent central control. |
| A | body | ABOVE | BODY | meaning | L2100-2108 | 587 | - | Unidirectional links trade guaranteed integrity for coordination-free scale. |
| A | body | NEW-CASE | BODY | meaning | L2124-2130 | 489 | A1,C4 | Bootstrap events carry the operating system above a frozen instruction set. |
| A | body | NEW-REASON | BODY | meaning | L2131-2137 | 497 | A1,X2 | Layer compatibility requires declared versions above a tiny frozen semantics. |
| A | body | NEW-REASON | BODY | meaning | L2143-2152 | 639 | A1,A2 | Explicit language semantics constrain future runtimes but do not guarantee continuity. |
| A | body | DISAGREES | BODY | meaning | L2352-2362 | 776 | A3,C2,C3 | Two gates never share a cell; as-of must be an explicit partition-position list. |
| A | body | NEW-REASON | BODY | meaning | L2481-2490 | 560 | A1,A2 | Rebuild identity cannot substitute for language compatibility and determinism enforcement. |
| A | body | NEW-REASON | BODY | rama | L131-131 | 107 | A3 | One topology exclusively owns each PState's writes. |
| A | body | NEW-REASON | BODY | rama | L141-142 | 234 | A3,C3 | Task serialization makes a local compare-and-set lock-free. |
| A | body | NEW-REASON | BODY | rama | L199-200 | 366 | A2,E8 | Each deployment has a runtime-readable module instance identity. |
| A | body | NEW-REASON | BODY | rama | L201-201 | 77 | A2 | Backups preserve the deployed module jar. |
| A | body | NEW-REASON | BODY | rama | L202-203 | 103 | A2 | Runtime version, jar hash, and machine kind lack documented accessors. |
| A | body | NEW-REASON | BODY | rama | L204-205 | 263 | A2,E8 | Verdicts can identify module instances; applications must connect them to build facts. |
| A | body | ABOVE | BODY | rama | L255-256 | 545 | - | A worldwide cluster places wide-area latency inside admission or between users and the gate. |
| A | body | DISAGREES | BODY | rama | L321-322 | 975 | A3,E1 | CARRIED Q4,Q7; stale edits are transformed and admitted instead of failing compare-and-set. |
| A | body | DISAGREES | BODY | rama | L329-330 | 806 | A3,P0,E1 | Admission winners are recomputable views rather than fixed gate decisions; also ABOVE. |
| A | body | NEW-REASON | BODY | rama | L331-332 | 195 | A2,E8 | Code identity enables finding facts written by a faulty deployment. |
| A | body | ABOVE | BODY | rama | L348-349 | 201 | - | Per-customer clusters supply no evidence for one planetary store. |
| A | body | ABOVE | BODY | rama | L354-354 | 135 | - | Integrated log, index, and compute confront the multiple-systems dissent. |
| A | body | NEW-REASON | BODY | rama | L369-370 | 552 | A1 | Clock reads and thread scheduling invalidate deterministic replay. |
| A | body | NEW-REASON | BODY | rama | L379-380 | 391 | A3 | Single partition writers simplify reasoning about concurrent modification. |
| A | body | NEW-REASON | BODY | rama | L391-391 | 586 | A1,W1 | Two implementations must agree; parallel replay and table switching avoid maintaining both. |
| A | body | ABOVE | BODY | rama | L395-396 | 366 | - | One system cannot presumptively outperform specialized query systems. |
| A | body | NEW-CASE | BODY | rama | L397-398 | 930 | A1 | Twitter's batch pipeline discarded late events that streaming retained. |
| A | body | ABOVE | BODY | rama | L415-416 | 199 | - | Challenges one substance and one store serving every query. |
| A | body | DISAGREES | BODY | skeptics | L62-63 | 239 | A2 | Runtime versions remain deployment metadata instead of facts read by running answers. |
| A | body | ABOVE | BODY | skeptics | L94-95 | 327 | - | Questions whether schema-later document stores remain distinct from relational systems. |
| A | body | ABOVE | BODY | skeptics | L96-97 | 654 | - | Questions graph-store necessity and distributed performance advantages. |
| A | body | ABOVE | BODY | skeptics | L100-101 | 727 | - | Questions replacing relational models and rebuilding established database infrastructure. |
| A | body | ABOVE | BODY | skeptics | L105-105 | 373 | - | Demands a functional or performance advantage over relational rows and audit tables. |
| A | body | ABOVE | BODY | skeptics | L111-112 | 453 | - | Limits the skeptics' applicability by distinguishing business workloads, adoption, and avoiding irreversible choices. |
| A | body | ABOVE | BODY | skeptics | L123-124 | 914 | - | Challenges custom infrastructure through operational costs and unknown failure modes. |
| A | body | ABOVE | BODY | skeptics | L125-126 | 418 | - | Applies innovation costs while qualifying the objection for a medium-building product. |
| A | body | ABOVE | BODY | skeptics | L127-128 | 174 | - | Limits the boring-technology objection when infrastructure itself is the product. |
| A | body | DISAGREES | BODY | skeptics | L137-138 | 1048 | A3,C3,C2 | Always-writable conflict merging replaces compare-and-set; also NEW-REASON on truncated vector clocks. |
| A | body | ABOVE | BODY | skeptics | L139-140 | 1282 | - | Operational manageability displaced a better-fitting distributed design inside Amazon. |
| A | body | CARRIED | BODY | skeptics | L200-201 | 1127 | A3 | Q4: durable storage rejects stale ETags despite simultaneous actor activations. |
| A | body | CARRIED | BODY | skeptics | L206-206 | 505 | A3 | Q4: compare-and-set must hold at durability, not merely in gate memory. |
| A | body | DISAGREES | BODY | skeptics | L248-248 | 149 | A2 | Runtime versions remain deploy logs and trace attributes rather than runtime facts. |
| A | body | ABOVE | BODY | skeptics | L307-307 | 230 | - | Challenges creating a second store when trusted institutions could share the first. |
| A | body | ABOVE | BODY | sync | L136-140 | 309 | - | Tentative admission and per-write repair challenge the gate model. |
| A | body | NEW-REASON | BODY | sync | L161-165 | 320 | A1 | Deterministic failure requires identical resource limits. |
| A | body | ABOVE | BODY | sync | L166-170 | 388 | - | Tentativeness propagates through queries and presentation. |
| A | body | ABOVE | BODY | sync | L192-197 | 462 | - | Applications uniformly rejected commit-only reads. |
| A | body | ABOVE | BODY | sync | L210-213 | 227 | - | Applications choose whether tentative state is visibly distinguished. |
| A | body | ABOVE | BODY | sync | L236-236 | 67 | - | One primary commits; other accepted writes remain tentative. |
| A | body | NEW-REASON | BODY | sync | L238-240 | 216 | A1 | Re-derivation depends on runtime resource limits. |
| A | body | ABOVE | BODY | sync | L241-242 | 161 | - | Marked tentative display challenges no-optimism. |
| A | body | ABOVE | BODY | sync | L253-261 | 570 | - | Rejects both hidden tentativeness and hiding tentative results. |
| A | body | NEW-REASON | BODY | sync | L293-296 | 251 | A1 | Promise scheduling must be deterministic. |
| A | body | NEW-REASON | BODY | sync | L337-338 | 80 | A1 | Full query re-execution is an admitted limitation. |
| A | body | NEW-REASON | BODY | sync | L362-364 | 173 | A1,A2 | Scheduling determinism motivates runtime-build references on crossings. |
| A | body | NEW-REASON | BODY | sync | L387-392 | 335 | A1,E3 | Replicated deterministic models depend on externally ordered inputs. |
| A | body | NEW-REASON | BODY | sync | L401-406 | 453 | A1,A2 | Session code hashes enforce identical programs. |
| A | body | NEW-REASON | BODY | sync | L418-423 | 282 | A1 | Snapshots avoid complete replay for new participants. |
| A | body | NEW-CASE | BODY | sync | L430-438 | 577 | A1,C1 | Code-derived session identity orphaned data. |
| A | body | ABOVE | BODY | sync | L454-456 | 222 | A1 | Replay latency motivates snapshots of derived state. |
| A | body | ABOVE | BODY | sync | L459-463 | 256 | - | Identity and permissions sit outside the session record. |
| A | body | DISAGREES | BODY | sync | L473-477 | 247 | A3,C6 | File authority applies last-writer-wins without compare-and-set. |
| A | body | NEW-REASON | BODY | sync | L482-485 | 252 | A3 | Central authority avoids decentralized CRDT overhead. |
| A | body | DISAGREES | BODY | sync | L486-490 | 345 | A3,C6 | Blind last-arrival overwrite replaces compare-and-set. |
| A | body | ABOVE | BODY | sync | L497-499 | 166 | - | Unacknowledged local changes override incoming authoritative changes on screen. |
| A | body | ABOVE | BODY | sync | L542-547 | 410 | - | Different value kinds required different conflict algorithms. |
| A | body | ABOVE | BODY | sync | L573-575 | 116 | - | One conflict rule does not fit every value kind. |
| A | body | ABOVE | BODY | sync | L582-585 | 237 | - | Questions CRDT overhead and globally ordered streams. |
| A | body | ABOVE | BODY | sync | L610-617 | 572 | - | Confirmed durable state coexists with optimistic presentation. |
| A | body | ABOVE | BODY | sync | L637-641 | 246 | - | Screen displays unconfirmed changes despite confirmed-only durable storage. |
| A | body | ABOVE | BODY | sync | L653-655 | 246 | - | Speculative local changes are immediately visible. |
| A | body | ABOVE | BODY | sync | L656-658 | 178 | A1 | Server mutator deliberately may produce different results. |
| A | body | ABOVE | BODY | sync | L659-663 | 331 | - | Reconciliation rewinds and replays speculative state. |
| A | body | ABOVE | BODY | sync | L694-698 | 267 | - | Network latency challenges waiting for admission before display. |
| A | body | ABOVE | BODY | sync | L736-738 | 131 | - | Speculative output cannot establish server admission. |
| A | body | ABOVE | BODY | sync | L744-747 | 208 | - | Physics and conflict-policy placement challenge the frame. |
| A | body | ABOVE | BODY | sync | L786-790 | 294 | - | Rejects one general-purpose data substance. |
| A | body | NEW-REASON | BODY | sync | L1026-1028 | 170 | A1 | Diffs must distinguish assertions from recalculated results. |
| A | body | ABOVE | BODY | sync | L1104-1106 | 205 | - | Server supports replicas rather than owning truth. |
| A | body | ABOVE | BODY | sync | L1143-1153 | 624 | - | Rejects central truth despite permission and deletion costs. |
| A | body | ABOVE | BODY | sync | L1214-1218 | 288 | - | Compactness-first design omits the envelope. |
| A | body | NEW-CASE | BODY | sync | L1255-1263 | 582 | A1 | Persisted merge structures increased size and slowed ordinary work. |
| A | body | NEW-REASON | BODY | sync | L1280-1283 | 232 | A1 | Rejects persisted merge state; authority simplifies concurrency. |
| A | body | NEW-REASON | BODY | sync | L1291-1294 | 320 | A3 | Central reconciliation makes CRDTs optional optimizations. |
| A | body | ABOVE | BODY | sync | L1328-1336 | 693 | - | Input faster than acknowledgement can indefinitely block updates. |
| A | body | ABOVE | BODY | sync | L1594-1598 | 280 | - | Remote signing latency pressures optimistic display. |
| A | body | ABOVE | BODY | sync | L1686-1688 | 211 | - | Rejects append-only logs for editable multi-device data. |
| A | body | NEW-CASE | BODY | sync | L1695-1698 | 293 | A3,C6 | Forked sequences kill feeds unless forks become evidence. |
| A | body | NEW-REASON | BODY | sync | L1719-1720 | 113 | A3,C6 | Multiple writers require representable fork evidence. |
| A | body | ABOVE | BODY | sync | L1728-1736 | 589 | - | Rejects administrator trust; deletion survives centralization. |
| A | body | DISAGREES | BODY | sync | L1910-1913 | 278 | A3 | Concurrent writes both succeed; readers resolve divergence. |
| A | body | DISAGREES | BODY | sync | L1941-1954 | 934 | A3,C1,C7 | Accepts conflicting writes and requires unknown-field preservation. |
| A | body | ABOVE | BODY | sync | L1982-1984 | 221 | - | Users and permissions sit outside enduring history. |
| A | body | DISAGREES | BODY | sync | L2606-2607 | 150 | A3 | Jujutsu accepts divergence instead of refusing conflicts. |
| A | body | NEW-REASON | BODY | sync | L2863-2876 | 937 | A1,A2 | Limits, scheduling, mixed builds, and code identity constrain reproducibility. |
| A | body | ABOVE | BODY | sync | L2981-2986 | 443 | - | Unused commit-only option challenges display premise. |
| A | body | ABOVE | BODY | sync | L3011-3012 | 157 | - | Server authority competes with local-first ownership. |
| A | body | DISAGREES | BODY | sync | L3016-3017 | 98 | A3 | Accepts all writes and records divergence. |
| A | body | ABOVE | BODY | sync | L3020-3021 | 75 | - | Marked tentative display challenges no-optimism. |
| A | body | ABOVE | BODY | sync | L3214-3217 | 228 | - | Credible exit requires personal-layer export. |
| X | main | ABOVE | ABOVE | datalog | L604-606 | 496 | X1,C7 | Add an atomic unit of saying for multi-fact admission and shared provenance. |
| X | main | ABOVE | R2 | datalog | L825-830 | 612 | X1,C7 | A 200-fact reply requires one atomic saying to avoid partial invented states. |
| X | main | NEW-REASON | R2 | datalog | L843-844 | 339 | X1 | Unrelated-cause batching risk is inferred, not a documented practitioner regret. |
| X | main | NEW-CASE | R2 | datalog | L976-977 | 1046 | X4,C3 | Entity partitioning makes private-data isolation depend on every filter path across rebuilds. |
| X | main | NEW-REASON | R2 | datalog | L1019-1019 | 176 | X2,C4 | Keep checked versions while growing bootstrap editions from stable word-derived ids. |
| X | main | ABOVE | ABOVE | rama | L449-450 | 919 | W1 | One substance still requires multiple indexes and physical routing to avoid every tool reading everything. |
| X | main | NEW-REASON | R2 | rama | L606-606 | 1019 | X2,E1 | Broadcast metadata incurs every-task write cost and inherently stale local checks. |
| X | main | NEW-REASON | R2 | rama | L628-628 | 85 | W2 | Clients must match the cluster's major and minor versions. |
| X | main | DISAGREES | R2 | rama | L677-678 | 814 | W1,X4 | Index paths must contain layers from day one; ABOVE: cluster-executed tools bypass private-layer promises. |
| X | main | ABOVE | R2 | rama | L720-721 | 148 | X4 | Tool execution location determines whether private layers have meaning. |
| X | main | NEW-REASON | ABOVE | skeptics | L278-278 | 474 | X2,E1 | Versioned rules must remain attributable because historical facts can violate later invariants. |
| X | main | DISAGREES | ABOVE | skeptics | L280-280 | 361 | W2,C7 | Fixes encodings now rather than deferring W2; also ABOVE on permanent envelope observables. |
| X | body | NEW-REASON | BODY | datalog | L119-122 | 444 | X1 | Atomic transactions make intermediate database states inexpressible. |
| X | body | NEW-REASON | BODY | datalog | L214-214 | 328 | X2,E1 | Record admission grammar versions to avoid current-schema reinterpretation. |
| X | body | DISAGREES | BODY | datalog | L244-244 | 554 | X4 | Visibility depends on trusted read-time filtering rather than a creation-inherited rule. |
| X | body | ABOVE | BODY | datalog | L246-246 | 672 | X3,C3 | Questions combining speculative state, private space, trust, and admission in one layer. |
| X | body | NEW-REASON | BODY | datalog | L247-247 | 363 | X3 | Historical filtering and speculation do not provide durable branches. |
| X | body | NEW-REASON | BODY | datalog | L248-249 | 154 | X4 | Default-open and default-closed systems provide opposing visibility precedents. |
| X | body | ABOVE | BODY | datalog | L346-346 | 322 | X1,C7 | Single-fact offers lack atomic multi-fact admission and shared provenance. |
| X | body | NEW-REASON | BODY | datalog | L347-348 | 52 | X3 | Datomic supplies no durable layers or branches. |
| X | body | NEW-CASE | BODY | datalog | L357-358 | 111 | X1 | Jepsen's set-versus-sequence dispute required transaction documentation changes. |
| X | body | NEW-REASON | BODY | datalog | L429-430 | 73 | W1 | Each node rebuilds its index from the entire log. |
| X | body | NEW-REASON | BODY | datalog | L471-472 | 224 | W1 | Full per-node replicas motivated shared object storage. |
| X | body | DISAGREES | BODY | datalog | L520-521 | 536 | X4 | Absent policies allow access; visibility is applied after querying. |
| X | body | NEW-REASON | BODY | datalog | L577-577 | 234 | X3 | Durable branching requires mechanisms beyond historical database filters. |
| X | body | NEW-REASON | BODY | datalog | L578-578 | 586 | X3,E5 | Named contexts support provenance but lack historical retraction without additional structure. |
| X | body | NEW-REASON | BODY | frontiers | L65-66 | 1192 | W1,E6 | Shared indexes and parameter collections reduce standing-query overhead. |
| X | body | DISAGREES | BODY | frontiers | L84-84 | 602 | W3,C2 | Requires a store-wide scalar timeline and durable partition map before first record. |
| X | body | NEW-REASON | BODY | frontiers | L97-98 | 246 | W1,E6 | Index patterns together and join landings rather than maintaining millions of subscriptions. |
| X | body | NEW-CASE | BODY | frontiers | L145-145 | 375 | W3 | Brandon points from his differential-dataflow implementation toward simpler totally ordered DBSP. |
| X | body | NEW-CASE | BODY | frontiers | L146-146 | 432 | X5,C2 | Settled-output tests missed intermediate-output consistency failures. |
| X | body | NEW-REASON | BODY | frontiers | L147-147 | 246 | X5,C2 | Failure dynamics proved easier to trigger and harder to predict than expected. |
| X | body | NEW-REASON | BODY | frontiers | L149-150 | 417 | W1 | Hidden operator state and unclear memory costs hindered differential-dataflow adoption. |
| X | body | NEW-REASON | BODY | frontiers | L157-157 | 212 | W3 | Pay partial-order complexity only for loops or independently advancing inputs. |
| X | body | NEW-REASON | BODY | frontiers | L235-236 | 994 | W3,E3 | Transaction order supplies time; ordered inputs simplify the computational model. |
| X | body | NEW-REASON | BODY | frontiers | L245-245 | 140 | W3 | DBSP simplifies differential dataflow by dropping partial time and past updates. |
| X | body | NEW-REASON | BODY | frontiers | L246-246 | 334 | W1 | Incremental engines must efficiently process historical backfill before live updates. |
| X | body | NEW-REASON | BODY | frontiers | L247-248 | 174 | W1 | Streaming workloads also require batch processing. |
| X | body | NEW-REASON | BODY | frontiers | L255-256 | 146 | X1,W1 | Bulk seeding must preserve the semantics of individual landings. |
| X | body | NEW-REASON | BODY | frontiers | L259-260 | 274 | W3,C3 | A partitioned source requires constructing, rather than assuming, the scalar admission order. |
| X | body | NEW-REASON | BODY | frontiers | L284-284 | 1079 | X4,W1 | Per-user policy views require shared computation to avoid excessive read or storage costs. |
| X | body | NEW-REASON | BODY | frontiers | L290-290 | 174 | X4 | Without policy, only the trusted base may see a fact. |
| X | body | NEW-REASON | BODY | frontiers | L291-291 | 295 | X3,W1 | Writable layers differ from policy-derived universes; per-person computation still incurs their costs. |
| X | body | NEW-REASON | BODY | frontiers | L365-366 | 273 | X3,C2,X4 | Nearest-layer reads require a frontier for every searched layer. |
| X | body | NEW-CASE | BODY | log | L247-247 | 378 | W1,W2 | Relational storage approached seven-figure costs and hit a 16 TiB limit. |
| X | body | NEW-REASON | BODY | log | L425-426 | 471 | X4,C4 | Before policy facts exist, runtime default-denial and genesis policies must bootstrap visibility. |
| X | body | NEW-REASON | BODY | meaning | L338-341 | 206 | X1 | World rollback provides a fence for meta-level changes. |
| X | body | NEW-REASON | BODY | meaning | L499-509 | 753 | X2,A1 | Immutable interpreter references require continued resolvability. |
| X | body | NEW-CASE | BODY | meaning | L707-714 | 526 | X4 | Public block lists emerged from early public architecture. |
| X | body | NEW-REASON | BODY | meaning | L747-756 | 718 | X4 | Reachability limits initial visibility; global pattern reads expose a public-default assumption. |
| X | body | NEW-CASE | BODY | meaning | L1213-1222 | 592 | W1 | Cheaply used scholarly data dominated Wikidata's index and forced a graph split. |
| X | body | ABOVE | BODY | meaning | L1250-1259 | 696 | X2 | Flexible schemas reduced coherence; functions moved outside the shared substance. |
| X | body | NEW-REASON | BODY | meaning | L1260-1267 | 523 | X2 | Post-admission checking leaves incoherence that admission grammars could prevent. |
| X | body | NEW-CASE | BODY | meaning | L1494-1497 | 272 | X3 | Deep world lookup required runtime primitives; persistence and external effects remained outside scope. |
| X | body | NEW-REASON | BODY | meaning | L1562-1563 | 111 | X1 | An atomic action groups tentative versions for joint commitment. |
| X | body | NEW-CASE | BODY | meaning | L1731-1736 | 384 | W2,C4 | Hidden implementation classes save space; bootstrap map describes itself. |
| X | body | NEW-REASON | BODY | meaning | L1737-1740 | 215 | W2,C4 | Physical shape optimisation can stay invisible beneath plain maps. |
| X | body | NEW-REASON | BODY | meaning | L1916-1924 | 634 | X4 | Shared pattern reads expose the entire tuple space without additional boundaries. |
| X | body | ABOVE | BODY | meaning | L1932-1939 | 476 | X3 | One substance over-shares personal material and excludes state outside its representation. |
| X | body | NEW-REASON | BODY | meaning | L2068-2079 | 853 | X4,C4,C8 | Self-governing clubs terminate policy regress and distinguish individual from delegated signatures. |
| X | body | NEW-REASON | BODY | meaning | L2325-2335 | 676 | X4 | Private reachability contrasts with trusted-room and public-heap assumptions. |
| X | body | NEW-REASON | BODY | meaning | L2336-2340 | 313 | X4,C2 | Historical reads must be authorised by current policy. |
| X | body | NEW-CASE | BODY | meaning | L2566-2572 | 465 | W1,E5 | Corpus skew, history limits, and noisy provenance constrain the shared base. |
| X | body | NEW-REASON | BODY | rama | L108-109 | 132 | X4 | Any cluster module can mirror another module's data. |
| X | body | NEW-REASON | BODY | rama | L123-126 | 284 | X4 | Committed-state visibility supplies no permission boundary. |
| X | body | NEW-REASON | BODY | rama | L127-128 | 153 | X4 | People's visibility depends on an external access tier. |
| X | body | NEW-REASON | BODY | rama | L212-212 | 177 | W2 | Built-in serialization already supports maps, collections, and records. |
| X | body | NEW-REASON | BODY | rama | L214-214 | 163 | W2 | Custom serializers must be registered in every interacting client and module. |
| X | body | NEW-REASON | BODY | rama | L215-215 | 113 | W2 | Client and cluster major and minor versions must match. |
| X | body | ABOVE | BODY | rama | L285-286 | 509 | W1 | Separating truth storage from query indexes challenges one-store conflation. |
| X | body | ABOVE | BODY | rama | L352-352 | 98 | W1 | One datastore serving truth and indexing is challenged. |
| X | body | NEW-CASE | BODY | rama | L355-356 | 906 | W1 | Multiply migrated after fixed indexes constrained queries and distributed operation. |
| X | body | DISAGREES | BODY | rama | L405-406 | 221 | W3,C2 | Requires positions per partition whenever several partitions exist. |
| X | body | DISAGREES | BODY | skeptics | L44-45 | 566 | X4 | Visibility defaults open internally and depends on manually remembered predicates. |
| X | body | NEW-REASON | BODY | skeptics | L149-150 | 1040 | X5,P0 | Small executable models, durability reviews, and observed bit errors justify continuous protection. |
| X | body | DISAGREES | BODY | skeptics | L239-239 | 183 | X4 | Internal visibility defaults open and relies on manual predicates. |
| X | body | NEW-REASON | BODY | sync | L727-728 | 134 | X4,C2 | Per-client snapshots simplify read authorization. |
| X | body | NEW-REASON | BODY | sync | L729-730 | 119 | X2 | Two client schema generations coexist during migration. |
| X | body | NEW-REASON | BODY | sync | L766-770 | 376 | X2,C4 | Bidirectional migrations support mixed client versions. |
| X | body | NEW-REASON | BODY | sync | L775-777 | 157 | X2 | Version skew is the difficult integration boundary. |
| X | body | NEW-REASON | BODY | sync | L903-907 | 278 | W2 | Runtime representation reduced memory without changing file format. |
| X | body | NEW-CASE | BODY | sync | L992-996 | 330 | X3 | Dependent drafts added complexity without observed value. |
| X | body | NEW-REASON | BODY | sync | L997-999 | 208 | X3 | Base changes propagate into drafts before merge. |
| X | body | NEW-REASON | BODY | sync | L1015-1019 | 370 | X3 | Two-level branching already encounters insufficient depth. |
| X | body | NEW-REASON | BODY | sync | L1111-1114 | 214 | X3 | Private changes and edit rejection need explicit support. |
| X | body | NEW-REASON | BODY | sync | L1121-1123 | 205 | X3 | Independent layers need re-examination after base changes. |
| X | body | DISAGREES | BODY | sync | L1483-1487 | 261 | X4 | Public-first system retrofits private permissions. |
| X | body | DISAGREES | BODY | sync | L1521-1521 | 80 | X4 | Permissions arrive after years of public records. |
| X | body | DISAGREES | BODY | sync | L1529-1533 | 295 | X4 | Public-default records omit provenance. |
| X | body | NEW-REASON | BODY | sync | L2412-2413 | 150 | X2 | Migrations support concurrent client generations. |
| N | main | NEW-CORNER | SHORT | datalog | L45-46 | 696 | C3,C5 | Missing customer ownership obstructs later splitting; unrecorded origins cannot be recovered. |
| N | main | NEW-CORNER | ABOVE | datalog | L607-607 | 235 | C1 | Unique external identities resolve semantic sameness beyond random-id uniqueness. |
| N | main | NEW-CORNER | ABOVE | datalog | L611-614 | 310 | E3 | Choose whether valid time is a universal axis or ordinary data. |
| N | main | NEW-CORNER | R2 | datalog | L834-834 | 417 | - | Require one cause per saying; throughput batching can silently combine unrelated acts. |
| N | main | NEW-CORNER | R2 | datalog | L841-841 | 430 | - | Name the act explicitly; actor and context alone cannot reconstruct it. |
| N | main | NEW-CORNER | R2 | datalog | L842-842 | 254 | C3,C5 | Nubank's missing owner and origin metadata cannot be reconstructed later. |
| N | main | NEW-CORNER | R2 | datalog | L871-872 | 603 | C5 | Subject ownership must be marked at admission to locate or collectively destroy value keys. |
| N | main | NEW-CORNER | R2 | datalog | L964-965 | 371 | C5 | Admission must identify subject ownership; erasure also needs key tombstones and complete-copy checking. |
| N | main | NEW-CORNER | R2 | datalog | L1016-1016 | 929 | - | Fix uniform logical retraction before the first record; empty values constrain every grammar. |
| N | main | NEW-CORNER | R2 | log | L653-654 | 370 | C5 | Key-store restoration can resurrect erased keys; destruction facts must be replayed afterward. |
| N | main | NEW-CORNER | R2 | log | L667-668 | 345 | P0 | Early external checkpoints are needed to prove early history against later tampering. |
| N | main | NEW-CORNER | R2 | log | L720-720 | 372 | E5 | Trigger marks preserve immediate causation; runtime guesses need explicit attribution. |
| N | main | NEW-CORNER | R2 | log | L739-740 | 161 | E1,C4 | Multiple home stores require store identity on every verdict from record one. |
| N | main | NEW-CORNER | R2 | meaning | L3062-3071 | 647 | C1,C4 | Entity merges require permanent resolution; personal source-derived names need secrecy. |
| N | main | NEW-CORNER | R2 | rama | L657-658 | 601 | C1,C4 | A same-as key is required from day one for independently ingested identities. |
| N | main | NEW-CORNER | R2 | skeptics | L425-425 | 364 | C8,E4 | Noninteractive actors need a credential path independent of logged-in sessions from the first record. |
| N | main | NEW-CORNER | R2 | sync | L3674-3681 | 579 | E1,A2 | Gate build must accompany verdict to explain differing admission behavior. |
| N | body | NEW-CORNER | BODY | datalog | L197-197 | 650 | C1 | Fix external unique-identity keys early so independent ingests converge on one entity. |
| N | body | NEW-CORNER | BODY | datalog | L389-390 | 531 | C3 | Customer ownership missing from transactions makes later database splitting harder. |
| N | body | NEW-CORNER | BODY | datalog | L405-405 | 456 | C5,C3 | Shared-base facts need subject ownership from creation for splitting and erasure. |
| N | body | NEW-CORNER | BODY | datalog | L487-487 | 589 | E3 | Valid time requires a separate decision; offerer time does not cover effective dates. |
| N | body | NEW-CORNER | BODY | datalog | L568-569 | 279 | C3,C5 | Owner omissions join irrecoverable origin and refusal losses. |
| N | body | NEW-CORNER | BODY | frontiers | L292-292 | 637 | C5,C8 | Every fact must resolve to an erasure owner through actor and layer from creation. |
| N | body | NEW-CORNER | BODY | frontiers | L355-356 | 451 | C4 | Bootstrap grammar and gate-authorizing policy must exist at the least time. |
| N | body | NEW-CORNER | BODY | log | L100-101 | 501 | - | Records must preserve intended operations, not merely resulting values. |
| N | body | NEW-CORNER | BODY | log | L135-135 | 339 | E5 | Read marks must distinguish gate-validated reads from actor-declared provenance. |
| N | body | NEW-CORNER | BODY | log | L160-160 | 415 | - | Preserve both conversation root and immediate cause with separate propagated identifiers. |
| N | body | NEW-CORNER | BODY | log | L178-178 | 316 | - | Conversation root omits immediate causation; infrastructure must preserve both. |
| N | body | NEW-CORNER | BODY | log | L197-197 | 126 | - | Generated events preserve the original event id for causal tracing. |
| N | body | NEW-CORNER | BODY | log | L413-414 | 444 | C8 | Gate-authenticated attribution and portable actor signatures require explicit provenance. |
| N | body | NEW-CORNER | BODY | log | L444-445 | 163 | - | Generated events retain an origin identifier for causal tracing. |
| N | body | NEW-CORNER | BODY | log | L448-449 | 920 | E5,E6 | Read provenance needs attesting authority; external anchors need durable content commitments. |
| N | body | NEW-CORNER | BODY | log | L464-465 | 392 | - | Infrastructure propagates both correlation and immediate-causation ids. |
| N | body | NEW-CORNER | BODY | log | L466-467 | 468 | - | Preserve chain root and immediate parent; runtime fills both. |
| N | body | NEW-CORNER | BODY | log | L570-570 | 456 | E1,C4 | Store identity must appear on every verdict from the first record. |
| N | body | NEW-CORNER | BODY | meaning | L107-111 | 165 | - | Preserved interpretation across transmission. |
| N | body | NEW-CORNER | BODY | meaning | L204-210 | 485 | - | Recoverable association between records and interpreters. |
| N | body | NEW-CORNER | BODY | meaning | L358-360 | 171 | - | Records need a recoverable interpreter association. |
| N | body | NEW-CORNER | BODY | meaning | L512-513 | 130 | - | Future runtimes must execute referenced interpreters. |
| N | body | NEW-CORNER | BODY | meaning | L570-577 | 582 | - | Seed must anchor body-language semantics for future execution. |
| N | body | NEW-CORNER | BODY | meaning | L1181-1185 | 339 | C6 | Successors must retain why they replace predecessors. |
| N | body | NEW-CORNER | BODY | meaning | L1186-1191 | 453 | - | Entity merge representation and permanent redirect resolution must be fixed early. |
| N | body | NEW-CORNER | BODY | meaning | L2004-2009 | 395 | C4 | Grammar changes must retain editing operations to preserve migration intent. |
| N | body | NEW-CORNER | BODY | meaning | L2246-2254 | 620 | C4 | Value-field ids and grammar-edit history must exist early; compatible growth bounds key reuse. |
| N | body | NEW-CORNER | BODY | meaning | L2298-2305 | 534 | C8 | Unsigned early offers can never acquire independent evidence of original authorship. |
| N | body | NEW-CORNER | BODY | meaning | L2374-2382 | 586 | E5,E6 | Read entries distinguish recorder; triggers and private usage histories need separate representation. |
| N | body | NEW-CORNER | BODY | rama | L81-82 | 554 | C4 | Bootstrap admission requires rules that precede grammar and policy facts. |
| N | body | NEW-CORNER | BODY | rama | L110-111 | 406 | C8 | First-record choice between front-tier attestation and preserved offer signatures. |
| N | body | NEW-CORNER | BODY | rama | L247-248 | 618 | - | Depot and PState names cannot be renamed through module updates. |
| N | body | NEW-CORNER | BODY | skeptics | L222-222 | 599 | C7 | Unpromised observable field behaviors must be hidden before tools make them permanent contracts. |
| N | body | NEW-CORNER | BODY | sync | L561-563 | 224 | - | Fix admission acknowledgement's physical durability threshold before record one. |
| N | body | NEW-CORNER | BODY | sync | L1934-1940 | 444 | - | Omitted rewrite kind is irrecoverable for earlier supersessions. |
| N | body | NEW-CORNER | BODY | sync | L2159-2167 | 616 | P0 | Requires trim boundary before record one. |
| N | body | NEW-CORNER | BODY | sync | L2685-2687 | 103 | - | Omitted historical rewrite kind cannot be recovered. |
| O | main | OWN-LIST | R2 | datalog | L910-912 | 420 | E1,X2 | Admission verdict records checked grammar and policy versions; rejects per-fact placement. |
| O | main | OWN-LIST | R2 | datalog | L913-913 | 510 | E1,C3 | Cross-order admission dependencies cannot be reconstructed unless the gate recorded them. |
| O | main | OWN-LIST | R2 | datalog | L914-915 | 156 | E1 | Optional offered grammar version explains refusal under a newer grammar. |
| O | main | OWN-LIST | R2 | datalog | L916-918 | 908 | W2,C7 | Disagrees: one permanent value encoding is fixed before admission. |
| O | main | OWN-LIST | R2 | datalog | L919-920 | 360 | C4 | Require new keys for incompatible grammar changes; renames add aliases. |
| O | main | OWN-LIST | R2 | datalog | L934-936 | 259 | C7 | Small positional core contains entity, key, value, saying, and replaces. |
| O | main | OWN-LIST | R2 | datalog | L937-937 | 54 | C1 | Saying is an entity with an id and an ordered position. |
| O | main | OWN-LIST | R2 | datalog | L938-938 | 260 | C4,C7 | Fix initial provenance vocabulary as ordinary named keys rather than envelope places. |
| O | main | OWN-LIST | R2 | datalog | L939-939 | 40 | W2 | Disagrees: fix the floor's single value encoding now. |
| O | main | OWN-LIST | R2 | datalog | L940-941 | 108 | C4 | Incompatible shapes require new keys; names grow only by aliases. |
| O | main | OWN-LIST | SHORT | frontiers | L25-26 | 496 | C2,E3,W3 | DISAGREES: authoritative ordering stamps and durable scalar mappings required before first record. |
| O | main | OWN-LIST | ABOVE | frontiers | L425-426 | 585 | C2,W3 | DISAGREES: ordering time, frontiers, and durable partition mappings required at first record; ABOVE challenges matching. |
| O | main | OWN-LIST | R2 | frontiers | L511-512 | 204 | C2 | Every index answer carries its complete-through point. |
| O | main | OWN-LIST | R2 | frontiers | L513-513 | 416 | C2 | DISAGREES: requires comparable points; permits explicitly marked mixed-moment answers. |
| O | main | OWN-LIST | R2 | frontiers | L514-515 | 57 | E2,C2 | Crossings store the read point and completeness status. |
| O | main | OWN-LIST | SHORT | log | L30-31 | 758 | C1,P0 | Three names; DISAGREES by requiring hashes on every pointer. |
| O | main | OWN-LIST | SHORT | log | L32-33 | 853 | P0,C5,W2 | Four promises, canonical form, separable values, and impersonal surviving metadata. |
| O | main | OWN-LIST | SHORT | log | L34-35 | 623 | A2,A1,E2,E1 | Runtime versions, logged upgrades, and crossing checksums. |
| O | main | OWN-LIST | SHORT | log | L36-37 | 638 | C3,C2,E3 | Keyed order, index frontier, and writer knowledge. |
| O | main | OWN-LIST | SHORT | log | L38-39 | 579 | E1 | Log offers first; DISAGREES by allowing verdict trimming. |
| O | main | OWN-LIST | SHORT | log | L40-41 | 572 | C7 | Extension rules and named fields before fixed parts. |
| O | main | OWN-LIST | SHORT | log | L53-54 | 91 | - | Introduces three choices required before the first record. |
| O | main | OWN-LIST | SHORT | log | L55-56 | 653 | C3,W3 | Ordering scope; DISAGREES by requiring an immediate choice. |
| O | main | OWN-LIST | SHORT | log | L57-58 | 513 | P0 | Retention depends on whether the log contains records or redo instructions. |
| O | main | OWN-LIST | SHORT | log | L59-60 | 203 | P0 | External verifiability versus operator trust. |
| O | main | OWN-LIST | OWN | log | L579-582 | 195 | - | Introduces conventions whose absence cannot be repaired for earlier facts. |
| O | main | OWN-LIST | OWN | log | L583-583 | 147 | C1 | Three names; DISAGREES by requiring hashes on every pointer. |
| O | main | OWN-LIST | OWN | log | L584-584 | 96 | P0,C1 | Canonical form and named hash function specified outside the store. |
| O | main | OWN-LIST | OWN | log | L585-585 | 127 | C7 | Versioned named-field envelope with a hashed extension slot. |
| O | main | OWN-LIST | OWN | log | L586-586 | 149 | C5 | Separable values, impersonal metadata, and salted erasable-value hashes. |
| O | main | OWN-LIST | OWN | log | L587-587 | 49 | C6 | Replacement history uses a parent list. |
| O | main | OWN-LIST | OWN | log | L588-588 | 253 | E1 | Additional verdict metadata; DISAGREES by permitting refusal trimming and classifying verdicts as foundational. |
| O | main | OWN-LIST | OWN | log | L589-589 | 184 | C2 | Applied index cuts, cross-partition vectors, and writer frontiers. |
| O | main | OWN-LIST | OWN | log | L590-590 | 142 | E5 | Provenance includes attesting authority; DISAGREES by classifying read marks as foundational. |
| O | main | OWN-LIST | OWN | log | L591-591 | 95 | C4,X2 | Stable key meaning and pinned checked grammar versions. |
| O | main | OWN-LIST | OWN | log | L592-592 | 118 | A2,E2 | Runtime changes and crossing evidence; DISAGREES by treating crossing checksums as foundational. |
| O | main | OWN-LIST | OWN | log | L593-593 | 182 | C4 | Shared genesis and required external bootstrap rules. |
| O | main | OWN-LIST | OWN | log | L594-594 | 92 | - | Both causal root and immediate cause recorded by the runtime. |
| O | main | OWN-LIST | OWN | log | L595-595 | 114 | E3 | Monotone admission clock; statement time stays in the value. |
| O | main | OWN-LIST | OWN | log | L596-596 | 80 | P0,C5 | Recorded erasure and poison-record neutralization. |
| O | main | OWN-LIST | OWN | log | L597-598 | 132 | C1,C3 | Fact pointers remain independent of partition positions. |
| O | main | OWN-LIST | R2 | log | L659-659 | 68 | P0 | Introduces the minimum required now for future public provability. |
| O | main | OWN-LIST | R2 | log | L660-660 | 67 | P0,W2 | Canonical fact encoding specified outside the store. |
| O | main | OWN-LIST | R2 | log | L661-661 | 70 | C7,C1 | Envelope version and named hash function. |
| O | main | OWN-LIST | R2 | log | L662-662 | 162 | P0,E1 | Duplicate admission hashes in fact and verdict permit corruption repair. |
| O | main | OWN-LIST | R2 | log | L663-663 | 93 | C5,P0 | Hiding value commitments let erasure preserve hash chains. |
| O | main | OWN-LIST | R2 | log | L664-664 | 46 | C7 | Extension slot included in the hashed form. |
| O | main | OWN-LIST | R2 | log | L665-666 | 58 | C3,P0 | Partition order never changes. |
| O | main | OWN-LIST | R2 | log | L703-706 | 312 | - | Introduces first-record requirements to avoid repeated full playback. |
| O | main | OWN-LIST | R2 | log | L707-707 | 270 | W2,P0,C5 | DISAGREES: physical envelope-value separation must be fixed immediately because later layout changes are forbidden. |
| O | main | OWN-LIST | R2 | log | L708-708 | 198 | C2 | Derived readers preserve applied cuts and resume from checkpoints. |
| O | main | OWN-LIST | R2 | log | L709-709 | 320 | A1,A2 | DISAGREES: runtime rebuilds selectively invalidate derived state. |
| O | main | OWN-LIST | R2 | log | L710-710 | 382 | E6,E2 | DISAGREES: crossings list result ids and hashes rather than only patterns and cuts. |
| O | main | OWN-LIST | R2 | log | L711-712 | 181 | W1,C5 | DISAGREES: reverse provenance index required before first erasure to avoid scanning. |
| O | main | OWN-LIST | R2 | meaning | L2694-2698 | 177 | - | Introduction to first-record losses. |
| O | main | OWN-LIST | R2 | meaning | L2699-2699 | 83 | E4 | Invoked authority on every fact; DISAGREES with early-loss classification. |
| O | main | OWN-LIST | R2 | meaning | L2700-2700 | 82 | C8 | DISAGREES: attribution chain; NEW-CORNER: offer signing. |
| O | main | OWN-LIST | R2 | meaning | L2701-2702 | 104 | C1 | DISAGREES: canonical hash identity with nonce and algorithm tag. |
| O | main | OWN-LIST | R2 | meaning | L2703-2703 | 81 | C2 | DISAGREES: explicit partition positions; index progress belongs in the read cut. |
| O | main | OWN-LIST | R2 | meaning | L2704-2704 | 81 | E1,A2,X2 | Verdict grammar, policy, and runtime versions. |
| O | main | OWN-LIST | R2 | meaning | L2705-2705 | 80 | E5 | Usage versus dependence; NEW-CORNER: actor or runtime recorder. |
| O | main | OWN-LIST | R2 | meaning | L2706-2706 | 69 | E3 | Occurrence time distinct from admission; DISAGREES with early-loss classification. |
| O | main | OWN-LIST | R2 | meaning | L2707-2707 | 42 | C6,C1 | DISAGREES: numeric predecessor; NEW-CORNER: replacement reason. |
| O | main | OWN-LIST | R2 | meaning | L2708-2709 | 90 | C4 | NEW-CORNER: grammar editing operations and identities for value fields. |
| O | main | OWN-LIST | R2 | meaning | L2710-2710 | 63 | C7 | Explicit chain start; DISAGREES: empty means unknown. |
| O | main | OWN-LIST | R2 | meaning | L2711-2711 | 77 | - | NEW-CORNER: entity merge convention and reader resolution. |
| O | main | OWN-LIST | R2 | meaning | L2712-2712 | 54 | C5 | Removable value with retained envelope. |
| O | main | OWN-LIST | R2 | meaning | L2713-2714 | 56 | C4 | DISAGREES: all first-fact ids universal, including the earlier gate inventory. |
| O | main | OWN-LIST | R2 | meaning | L2715-2719 | 159 | C7 | ABOVE: minimise immutable core; DISAGREES: open additive extensions. |
| O | main | OWN-LIST | R2 | meaning | L2960-2974 | 1031 | A1,A2,C4 | NEW-CORNER: seed language semantics, reference interpreter, and conformance cases. |
| O | main | OWN-LIST | SHORT | rama | L31-34 | 130 | - | Introduction to the file's first-record findings. |
| O | main | OWN-LIST | SHORT | rama | L35-35 | 354 | P0 | CARRIED Q10; defaults preserve records but operators may migrate or trim. |
| O | main | OWN-LIST | SHORT | rama | L36-36 | 188 | C3,C2 | CARRIED Q5; fixed task count forces copying and changed positions. |
| O | main | OWN-LIST | SHORT | rama | L37-37 | 207 | C1 | NEW-REASON: carried ids survive restore, trimming, and repartition. |
| O | main | OWN-LIST | SHORT | rama | L38-38 | 260 | C1 | DISAGREES: recommends time-bearing UUIDv7; client generation protects retries. |
| O | main | OWN-LIST | SHORT | rama | L39-39 | 227 | E1 | DISAGREES: verdict retention is required for correctness, not merely an early loss. |
| O | main | OWN-LIST | SHORT | rama | L40-40 | 413 | C3,E1 | CARRIED Q7; gate modes trade atomic scope, latency, and failure isolation. |
| O | main | OWN-LIST | SHORT | rama | L41-41 | 307 | C2,W3 | CARRIED Q3; microbatch exposes global progress, stream offsets remain unavailable. |
| O | main | OWN-LIST | SHORT | rama | L42-42 | 214 | C1,P0 | CARRIED Q1; publishing admitted facts produces duplicates requiring deduplication. |
| O | main | OWN-LIST | SHORT | rama | L43-43 | 287 | E3,A1 | CARRIED Q2; gate-generated stamps must be preserved rather than recomputed. |
| O | main | OWN-LIST | SHORT | rama | L44-44 | 162 | E5,E6 | Read provenance requires application conventions. |
| O | main | OWN-LIST | SHORT | rama | L45-46 | 281 | C8,X4,C5 | Authentication, authorization, and encryption require additional mechanisms. |
| O | main | OWN-LIST | ABOVE | rama | L466-466 | 756 | C4,C1,C7,C3,C8,E3,E5 | DISAGREES: key and id representation may migrate; captured information and physical placement must be fixed. |
| O | main | OWN-LIST | R2 | skeptics | L394-395 | 423 | C5 | Replay must tolerate erased values from record one; also CARRIED Q10. |
| O | main | OWN-LIST | R2 | skeptics | L396-396 | 151 | C5,P0 | Distinguishes erased from missing and keeps attributable erasure facts; also CARRIED Q10. |
| O | main | OWN-LIST | R2 | skeptics | L397-397 | 349 | C1,C5 | Random value ids and tombstone write refusal prevent resurrection; also CARRIED Q9,Q10. |
| O | main | OWN-LIST | R2 | skeptics | L398-399 | 285 | C5 | Requires durable values before admission; also ABOVE on dual-write seam and CARRIED Q10. |
| O | main | OWN-LIST | R2 | skeptics | L400-403 | 301 | E3,C2 | First-record list: monotone commit timestamps; also DISAGREES with E3's prohibition on clock ordering. |
| O | main | OWN-LIST | R2 | skeptics | L404-404 | 292 | C3,C2,E3 | Idle gates advance clock frontiers; also DISAGREES E3; heartbeat requirement is SAME. |
| O | main | OWN-LIST | R2 | skeptics | L405-405 | 97 | E3,C2 | Readers wait past one shared time at every gate; also DISAGREES E3. |
| O | main | OWN-LIST | R2 | skeptics | L406-406 | 159 | E1,P0 | Journal contains only committed work; also DISAGREES E1 on refusal retention; CARRIED Q1. |
| O | main | OWN-LIST | R2 | skeptics | L407-407 | 184 | C3,A3 | Commit-time conflict checks require bounded heat on individual keys; also NEW-REASON. |
| O | main | OWN-LIST | R2 | skeptics | L408-409 | 120 | C3,X1 | Cross-gate writes require coordination; also CARRIED Q7. |
| O | main | OWN-LIST | ABOVE | sync | L3165-3176 | 825 | E1,E5,E8 | Also ABOVE; DISAGREES: verdict grounds and sessions become must-capture-at-birth. |
| O | main | OWN-LIST | R2 | sync | L3318-3324 | 435 | C7 | Admission slots explicitly represent pending offers. |
| O | main | OWN-LIST | R2 | sync | L3325-3331 | 479 | E2 | Record-one marker preserves whether crossings included tentative input. |
| O | main | OWN-LIST | R2 | sync | L3332-3335 | 303 | C2 | Opaque cut can include pending offers and epoch. |
| O | main | OWN-LIST | R2 | sync | L3336-3338 | 100 | C1 | Offerer-made IDs support tentative references. |
| O | main | OWN-LIST | R2 | sync | L3660-3669 | 735 | C1,C2,C3,E8 | Also ABOVE; personal gates require second-store conventions at record one. |
| O | body | OWN-LIST | BODY | log | L407-408 | 968 | C5 | Separable values, impersonal envelopes, hiding hashes; DISAGREES by permitting expiring-backup erasure. |
| O | body | OWN-LIST | BODY | meaning | L2218-2228 | 744 | C4,C1 | First-fact inventory; DISAGREES: universal gate identity and optional content-derived ids. |
| O | body | OWN-LIST | BODY | sync | L2291-2295 | 218 | P0 | Logical permanence permits recorded erasure and re-encoding. |
| O | body | OWN-LIST | BODY | sync | L2296-2296 | 77 | C1,C7 | Envelope identity must not depend on physical encoding. |
| O | body | OWN-LIST | BODY | sync | L2297-2299 | 188 | - | Fix acknowledgement's physical durability threshold. |
| O | body | OWN-LIST | BODY | sync | L2300-2302 | 121 | P0 | Trimming must explicitly record omissions. |
| O | body | OWN-LIST | BODY | sync | L2336-2340 | 201 | C1 | Gate refuses collisions in offerer-minted IDs. |
| O | body | OWN-LIST | BODY | sync | L2341-2343 | 198 | C1 | DISAGREES: permits creating-offer-plus-index identities. |
| O | body | OWN-LIST | BODY | sync | L2344-2345 | 152 | C1 | Keep time, place, type, and shard outside identity. |
| O | body | OWN-LIST | BODY | sync | L2346-2349 | 243 | C1 | Written identity carries a scheme marker. |
| O | body | OWN-LIST | BODY | sync | L2350-2351 | 45 | C1 | Identity knowledge grants no authority. |
| O | body | OWN-LIST | BODY | sync | L2378-2383 | 281 | C4 | DISAGREES: gate actor is shared across stores. |
| O | body | OWN-LIST | BODY | sync | L2384-2386 | 219 | C4,C2 | Unique store identity disambiguates local admission numbers. |
| O | body | OWN-LIST | BODY | sync | L2387-2389 | 202 | C7 | DISAGREES: empty authorization list marks genesis axioms. |
| O | body | OWN-LIST | BODY | sync | L2390-2391 | 151 | C4,C7 | Runtime bootstraps tiny versioned grammar-of-grammars. |
| O | body | OWN-LIST | BODY | sync | L2392-2393 | 73 | C4 | Assume genesis placeholders are permanent. |
| O | body | OWN-LIST | BODY | sync | L2425-2429 | 215 | X2,E1 | Record exact admitted grammar version. |
| O | body | OWN-LIST | BODY | sync | L2430-2432 | 211 | C4 | DISAGREES: breaking grammar change requires new key. |
| O | body | OWN-LIST | BODY | sync | L2433-2437 | 376 | C4 | Key is ID; base-name registration uses compare-and-set. |
| O | body | OWN-LIST | BODY | sync | L2438-2443 | 455 | C4,E6 | Name lookup is a read; independent keys need reconciliation. |
| O | body | OWN-LIST | BODY | sync | L2444-2446 | 197 | P0,C4 | DISAGREES: kind retention permits replacement and nonstorage. |
| O | body | OWN-LIST | BODY | sync | L2447-2449 | 149 | E1,X2 | Admission reads grammar and policy facts. |
| O | body | OWN-LIST | BODY | sync | L2477-2481 | 224 | C5 | Separate envelope and value physically. |
| O | body | OWN-LIST | BODY | sync | L2482-2485 | 302 | C5,P0 | Erasure records authority, cause, and readmission prevention. |
| O | body | OWN-LIST | BODY | sync | L2486-2488 | 178 | E2,C5 | DISAGREES: crossings keep reads instead of rendered content. |
| O | body | OWN-LIST | BODY | sync | L2489-2492 | 274 | C5,E6 | Provenance locates assertions affected by erasure. |
| O | body | OWN-LIST | BODY | sync | L2493-2495 | 224 | C5 | DISAGREES: expiring backups may substitute for destroying access. |
| O | body | OWN-LIST | BODY | sync | L2496-2498 | 89 | C5 | Cross-store erasure is an audited request. |
| O | body | OWN-LIST | BODY | sync | L2528-2531 | 162 | C8 | Immediate actor never becomes represented person. |
| O | body | OWN-LIST | BODY | sync | L2532-2535 | 260 | C8,E4,E1 | Checked verdict names versioned delegation grant. |
| O | body | OWN-LIST | BODY | sync | L2536-2537 | 93 | A1,A2 | Actor definition must be recoverable at recorded version. |
| O | body | OWN-LIST | BODY | sync | L2538-2540 | 158 | C8 | Standing actor spans runs. |
| O | body | OWN-LIST | BODY | sync | L2541-2542 | 85 | C5,C8 | Human names are separately erasable. |
| O | body | OWN-LIST | BODY | sync | L2543-2546 | 166 | C8 | Gate checks credentials; external signatures age. |
| O | body | OWN-LIST | BODY | sync | L2561-2568 | 512 | E3 | Claimed time is keyed value; admission time follows read causality. |
| O | body | OWN-LIST | BODY | sync | L2582-2590 | 581 | X4,C2 | DISAGREES: exposes hidden-reference existence; layers determine visibility. |
| O | body | OWN-LIST | BODY | sync | L2611-2614 | 165 | C3,C2 | DISAGREES: cell stays in one partition for life. |
| O | body | OWN-LIST | BODY | sync | L2615-2618 | 286 | C3,E1 | Enumerate cross-cell invariants before choosing partitions. |
| O | body | OWN-LIST | BODY | sync | L2619-2621 | 227 | C2 | Opaque token forbids external arithmetic. |
| O | body | OWN-LIST | BODY | sync | L2622-2622 | 71 | E2,C2 | Crossing records screen or prompt cut. |
| O | body | OWN-LIST | BODY | sync | L2623-2625 | 203 | C3,A3 | Gates can own different cells in one layer. |
| O | body | OWN-LIST | BODY | sync | L2626-2630 | 241 | E1,A3 | Consider query checks and fallback under contention. |
| O | body | OWN-LIST | BODY | sync | L2650-2654 | 209 | E6 | Runtime capture differs from actor claims. |
| O | body | OWN-LIST | BODY | sync | L2655-2658 | 239 | E6,C2 | Pattern read needs evaluated layer stack. |
| O | body | OWN-LIST | BODY | sync | L2659-2659 | 80 | E6 | Trigger belongs in because-of without duplication. |
| O | body | OWN-LIST | BODY | sync | L2660-2661 | 135 | E6 | Model reply points to crossing. |
| O | body | OWN-LIST | BODY | sync | L2662-2664 | 145 | E6 | Compress provenance and reuse crossing references. |
| O | body | OWN-LIST | BODY | sync | L2688-2691 | 189 | E5 | Provenance pins reads; staleness is derived. |
| O | body | OWN-LIST | BODY | sync | L2692-2694 | 232 | E5 | Capture dependence and exposure roles at creation. |
| O | body | OWN-LIST | BODY | sync | L2695-2697 | 215 | E5 | Actor class and citation guide roles. |
| O | body | OWN-LIST | BODY | sync | L2698-2699 | 124 | C4,E5 | Value-reference pinning belongs to grammar. |
| O | body | OWN-LIST | BODY | sync | L2700-2702 | 119 | C6,E5 | Replacement and trigger stay separate from reads. |
| O | body | OWN-LIST | BODY | sync | L2718-2724 | 447 | C2 | Record index cut; enforce high-water token and display lag. |
| O | body | OWN-LIST | BODY | sync | L2734-2742 | 581 | C7,E8 | DISAGREES: origin uses emptiness or session parent. |
| O | body | OWN-LIST | BODY | sync | L2773-2779 | 339 | C1 | DISAGREES: permits actor-plus-counter names. |
| O | body | OWN-LIST | BODY | sync | L2780-2784 | 356 | C1 | Gate-only fields prevent offerer hashing final fact. |
| O | body | OWN-LIST | BODY | sync | L2785-2787 | 166 | C1,E1 | Refused offers need independent identities. |
| O | body | OWN-LIST | BODY | sync | L2788-2793 | 393 | C1,C5 | DISAGREES: unkeyed value hash inside optional integrity digest. |
| O | body | OWN-LIST | BODY | sync | L2794-2800 | 449 | C1 | Early dependencies require waiting offers or serialized work. |
| O | body | OWN-LIST | BODY | sync | L2813-2822 | 635 | C6,C7,X3 | Keep replacement and origin; shadowing pins lower-layer base. |
| O | body | OWN-LIST | BODY | sync | L2845-2850 | 297 | E1 | DISAGREES: mandates inline verdict grounds. |
| O | body | OWN-LIST | BODY | sync | L2851-2852 | 146 | E1 | Refusal records failed check and repair conditions. |
| O | body | OWN-LIST | BODY | sync | L2853-2855 | 182 | E1,C5 | Refused payload may be omitted. |
| O | body | OWN-LIST | BODY | sync | L2856-2858 | 204 | E1 | DISAGREES: retention must be fixed now, rather than early loss. |
| O | body | OWN-LIST | BODY | sync | L2859-2860 | 65 | E1 | Verdict must be re-checkable from records. |
| O | body | OWN-LIST | BODY | sync | L2877-2888 | 787 | E8,A2,E2 | DISAGREES: session anchors crossings; digest is optional. |
| O | body | OWN-LIST | BODY | sync | L2904-2914 | 749 | E7,E2 | DISAGREES: point off by default; select requires action or changed display. |
| O | body | OWN-LIST | BODY | sync | L2928-2936 | 559 | E4,E7,E2 | Click approves shown operand; admission compares content or cut. |
| O | body | OWN-LIST | BODY | sync | L2962-2972 | 766 | C7,P0,E1 | DISAGREES: preserves unknown fields and permits class-based trimming. |
| P0 | read-whole | DISAGREES | SHORT | skeptics | L16-16 | 170 | P0 | Mutable truth and expiring logs reject permanent admitted content; also ABOVE. |
| C1 | read-whole | DISAGREES | SHORT | skeptics | L17-17 | 197 | C1 | Time-bearing and shard-bearing ids oppose random, location-free names. |
| E | read-whole | DISAGREES | SHORT | skeptics | L18-18 | 269 | E3,C3,C2 | Permits wall-clock ordering, which E3 excludes. |
| E | read-whole | DISAGREES | SHORT | skeptics | L19-19 | 298 | E6,E5,C8,E4 | Omits read provenance and uses shared accounts with application-level authority. |
| P0 | read-whole | DISAGREES | SHORT | skeptics | L20-21 | 294 | P0,C5 | Rents immutability and questions key destruction as erasure; also CARRIED Q10. |
| A | read-whole | ABOVE | SHORT | skeptics | L22-23 | 214 | - | Operational familiarity explains preference for the ordinary default. |
| C3 | read-whole | CARRIED | R3 | datalog | L1051-1051 | 479 | C3 | Q7 and Q5: semantic partition boundaries require foresight and costly later movement. |
| C3 | read-whole | CARRIED | R3 | datalog | L1052-1052 | 295 | C3 | Q7: hot shared layers exceed single-thread write capacity. |
| C3 | read-whole | CARRIED | R3 | datalog | L1053-1053 | 91 | C3 | Q7: layer keys concentrate load; entity keys distribute it. |
| X | read-whole | NEW-REASON | R3 | datalog | L1055-1055 | 60 | X1 | A microbatch gate supplies cross-partition atomic admission. |
| C3 | read-whole | CARRIED | R3 | datalog | L1056-1057 | 80 | C3,C2 | Q5: repartitioning costs remain under either key. |
| C3 | read-whole | CARRIED | R3 | datalog | L1058-1061 | 189 | C3,C2 | Q7: base and team-layer ceilings remain after eliminating cut byte costs. |
| C3 | read-whole | CARRIED | R3 | datalog | L1062-1062 | 530 | C3,X1 | Q7: cross-partition atomic admission adds latency and shared stalls. |
| C3 | read-whole | CARRIED | R3 | datalog | L1063-1063 | 638 | C3 | Q7: lived systems use owner units or bounded databases rather than hashed global units. |
| C3 | read-whole | CARRIED | R3 | datalog | L1064-1064 | 387 | C3 | Q7: small personal units preserve ordering benefits within a thread's capacity. |
| C2 | read-whole | CARRIED | R3 | datalog | L1065-1066 | 498 | C2,C3 | Q5: unit-relative counts survive movement; task offsets require layout epochs. |
| C3 | read-whole | CARRIED | R3 | datalog | L1067-1068 | 170 | C3 | Q7: shared-base and hot-team workloads lack this camp's lived ordering solution. |
| C3 | read-whole | CARRIED | R3 | datalog | L1073-1073 | 674 | C3,C2 | Q7: private layers become ordered values; entity-sharded base requires named cuts. |
| C3 | read-whole | CARRIED | R3 | datalog | L1074-1074 | 282 | C3,C2 | Q7 and Q3: private ordered logs coexist with named base cuts and epochs. |
| C3 | read-whole | CARRIED | R3 | datalog | L1075-1076 | 321 | C3,C2 | Q7: owner units need lagging derived builds for cross-unit aggregation. |
| C3 | read-whole | CARRIED | R3 | datalog | L1077-1078 | 493 | C3,X1,E1 | Q7: one saying occupies one unit; multi-entity base sayings use asynchronous atomic admission. |
| O | read-whole | OWN-LIST | R3 | datalog | L1079-1081 | 212 | C3,C5 | Record owner or subject from admission to support later recutting. |
| O | read-whole | OWN-LIST | R3 | datalog | L1082-1082 | 54 | C1 | Offerer-generated identity survives recutting. |
| O | read-whole | OWN-LIST | R3 | datalog | L1083-1083 | 178 | C2,C3 | Use unit-relative positions and layout epochs wherever task offsets appear. |
| O | read-whole | OWN-LIST | R3 | datalog | L1084-1084 | 97 | E3 | Gate stamps remain monotonic within each ordered unit. |
| O | read-whole | OWN-LIST | R3 | datalog | L1085-1086 | 196 | - | Record causal-chain root early to identify future problem-based splits. |
| C3 | read-whole | CARRIED | R3 | datalog | L1087-1088 | 167 | C3 | Q7 and Q5: choose team-layer ordering at creation; preserved metadata enables later changes. |
| C3 | read-whole | NEW-REASON | R3 | datalog | L1089-1090 | 242 | C3 | Hybrid shared-base and live-recutting claims remain inferred rather than demonstrated. |
| C8 | read-whole | NEW-REASON | SHORT | meaning | L29-37 | 608 | C8,E4 | Trust in attribution and transmitted interpreters. |
| C4 | read-whole | NEW-REASON | SHORT | meaning | L38-43 | 309 | C4,A2,C7 | Future readers make interpretation conventions durable. |
| N | read-whole | NEW-CORNER | SHORT | meaning | L44-50 | 407 | - | Interpreter references require portable identity and executable semantics from the first fact. |
| C7 | read-whole | ABOVE | SHORT | meaning | L51-56 | 326 | C7,A2 | Both voices shrink the fixed envelope. |
| E | read-whole | DISAGREES | SHORT | meaning | L57-64 | 522 | E4,C8 | Grant provenance classified as irreversible rather than an early loss. |
| C3 | read-whole | CARRIED | R3 | rama | L742-744 | 314 | C3 | Q7; custom partitioning can place all records by layer. |
| C3 | read-whole | CARRIED | R3 | rama | L745-745 | 415 | C3,X1 | Q7; one layer-local offer can atomically admit a many-fact saying. |
| C3 | read-whole | CARRIED | R3 | rama | L746-746 | 116 | C3 | Q7; matching depot and PState placement eliminates hops. |
| C3 | read-whole | CARRIED | R3 | rama | L747-747 | 152 | C3,E4 | Q7; layer placement makes private policy and grant checks local. |
| C3 | read-whole | CARRIED | R3 | rama | L748-749 | 461 | C3 | Q7; owner-based placement follows the dominant access pattern rather than map keys. |
| C3 | read-whole | CARRIED | R3 | rama | L750-751 | 433 | C3,X1 | Q7; exclusive topology ownership prevents mixing gate modes for one fact PState. |
| C3 | read-whole | NEW-CASE | R3 | rama | L754-754 | 604 | C3 | A hot team layer throttles unrelated layers sharing its task. |
| C3 | read-whole | CARRIED | R3 | rama | L755-755 | 442 | C3,C2 | Q5,Q7; a layer outgrowing one task loses single-writer order when split. |
| C3 | read-whole | CARRIED | R3 | rama | L756-756 | 245 | C3 | Q7; few unequal layers balance worse than numerous small entities. |
| C3 | read-whole | CARRIED | R3 | rama | L757-757 | 581 | C3,X3 | Q7; layer partitioning makes every entity-stack read hop across layers. |
| C3 | read-whole | NEW-REASON | R3 | rama | L758-758 | 159 | C3 | Cross-partition promotion remains an ordinary offer linked by provenance. |
| C3 | read-whole | CARRIED | R3 | rama | L759-759 | 153 | C3,X1 | Q7; a saying spanning two layers loses the promised local atomicity. |
| C3 | read-whole | CARRIED | R3 | rama | L760-761 | 216 | C3,C2 | Q5; no documented online rehoming avoids a full module copy. |
| C3 | read-whole | CARRIED | R3 | rama | L764-765 | 327 | C3 | Q7; hybrid placement distinguishes single-owner and many-writer layers rather than privacy. |
| C3 | read-whole | CARRIED | R3 | rama | L766-766 | 251 | C3 | Q7; configurable partitioners support the hybrid's local atomicity. |
| C3 | read-whole | CARRIED | R3 | rama | L767-767 | 331 | C3,C2,X1 | Q7; layer-local counters and batches trade local policy checks against stack hops and throughput limits. |
| C3 | read-whole | CARRIED | R3 | rama | L768-769 | 97 | C3,C2 | Q5; online rehoming and task splitting remain undocumented. |
| O | read-whole | OWN-LIST | R3 | rama | L770-771 | 86 | - | Introduction to decisions required before the first record. |
| O | read-whole | OWN-LIST | R3 | rama | L772-772 | 313 | C3,C1 | Placement and layer class must be fixed; offers must reveal a verifiable class. |
| O | read-whole | OWN-LIST | R3 | rama | L773-773 | 196 | C2 | DISAGREES: position epochs are mandatory immediately rather than available as later token extensions. |
| O | read-whole | OWN-LIST | R3 | rama | L774-774 | 166 | C3 | Order promises depend on single-owner placement remaining fixed. |
| O | read-whole | OWN-LIST | R3 | rama | L775-776 | 327 | X1,E1 | NEW-CORNER: every constituent fact carries saying identity and a whole-saying verdict. |
| C2 | read-whole | CARRIED | R3 | rama | L777-778 | 124 | C2 | Q3; interned cuts agree, with a native global number under microbatch. |
