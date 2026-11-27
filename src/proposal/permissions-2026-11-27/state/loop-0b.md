status:     DONE
commit:     e4bda697+loop0b
verified:   nothing run. 2,081 check sentences read once, 411 listed; CONTRACT.md held against design-pitfalls (2 findings turned in the page, 3 stand) and rama-pitfalls (11 PASS, 2 N/A, section 4 OPEN, section 15 FAIL: no run)
next:       Window A: 'continue' for Loop 0c, to break CONTRACT.md and ALLOWLIST.md. Sid: S1 to S7 of CONTRACT.md; S1, S2 and S5 before code rests on it

# Loop 0b: what Window B wrote, and the two audits

Window B (Fable, max), 27 November 2026, from the prompt that Sid pasted.
Read: the two pitfalls skills, `PERMISSIONS.md` whole, `BUILD.md`,
`LOOP0-EMPIRICAL-FACTS.md`, the review page's sections 3, 6 and 7, the
store's act, declaration, gate, rows, tables, walk, both gates, the door's
reads and digest, the read path's lanes, sources, query and acts, Auth's
records, the runtime's sign-in and seam, and all 2,081 lines of the dump.
Not read: `PLAN.md`, `HANDOFF-implementer.md`, `read_path/answer.clj`,
`door.clj`, and the proofs' code but for three checks. No file under
`src-store/` or `test-store/` was touched.

- `CONTRACT.md`: 126 lines. It is one page on a screen and about two on
  paper: the seven calls with their trade-offs take 50 of them.
- `ALLOWLIST.md`: 411 of 2,081 sentences, each with its reason. 279 carry a
  reason of `BUILD.md` alone; 132 carry a default of the contract, and 88 of
  those nothing else.
- "Auth maker custody" in the prompt was read two ways, and both are in S2:
  the values of a personal lane's making act, and who made an agent.

## design-pitfalls, held against CONTRACT.md

Inventory: the party; a give's id; its three places; what it allows and
covers; "lent from"; the sitting; `:under` on an act and in a row; the table
by holder and its key; `:owners`; `making-give`; the query.

Turned in the page:

1. Questions 1 and 2. A give's own place also said "ended". Who holds a give
   is written once and never changes; whether it stands changes once, by
   another party's act. The page now gives the end a place of its own. Cost
   now: one key, and nine sentences under `end` in the allowlist. Cost had
   it stayed: the form of every end that acts keep.
2. Question 8. The cover's tag `:act` said "an act"; the thing is one act to
   come, named by its digest. Sid's "permission over" each act (‹45c7127b›)
   is about acts that exist. The tag is `:exact-act`, and `:act` stays free.

Standing, each with its cost:

3. Question 8. One id under two words: `:session` on an act, `:sitting` on a
   give. One job. Now: pick one word (S4, S7). Later: every give that acts
   keep holds the word.
4. Question 8. `:with :permission` in a query hands what a row calls
   `:under`. Now: a word of the wire. Left.
5. Questions 1 and 3. `:everything` is one word for every right and, with no
   "lent from", for what counts as an owner. `BUILD.md` B3 defines owning so,
   which makes it one job today. A part given alone later is a key of the
   map, and old gives keep the word. By the brake, no split.

Asked, and nothing found: questions 1 and 2 of "lent from" beside a row's
`:who` and `:under`, and of `:under` in a row read two ways (which give the
act named; at an end place, a use or an end: drawn at read from one field);
question 2 of the terms (one lifecycle, one writer); question 3 of the kind
of a party and of what a give allows and covers (values of one field);
question 4 (acts fix `:who` as a party, `:under`, three keys and their
values, none for good while no real data is kept); question 5 ("lent from"
and the holder's kind are needed at the act's own moment; the table by
holder and `:owners` are drawn from acts and can be made again); question 6
(one exact act is the finest for writing; for seeing it is the lane's
default group, as in `BUILD.md`; P section 9 item 13 is open); question 7
(S1 to S5 each name the points of the range and what is not offered);
question 10 (nothing beyond the batches: the query is P section 12 point
16's "read again from one snapshot", and `making-give` is forced by B1).

## rama-pitfalls, held against CONTRACT.md

```text
depots       *streaming-depot, *microbatch-depot -> hash-by :lane -> an act as it
             arrived; of it :who and :under change form
topologies   streaming-gate (stream, all-after), microbatch-gate (microbatch): one
             source each -> walk/decide. Queries: answer-by-act-id, depot-of-lane,
             and gives-that-stand, which is new and only reads
PStates      each gate's six + gives-by-holder + task :owners -> owner: the gate
             -> writers: walk/decide, and on the base the copy behind |all
             -> readers: the gate, the queries, the read path's selects and pushes
partitioners none in walk/decide; |all behind it in the microbatch gate;
             |hash routing-id and |origin in each query
actors       the door (appends), the read path in the runtime, Auth (S2)
clients      the door's appends as today; a call of gives-that-stand; pushes on
             one row or one run

RAMA-PITFALLS VERDICT for CONTRACT.md

  1. EVENT BOUNDARY & HOP RETRY : PASS - each write of a decision (a give's three
     places, its row by holder, :owners, answer, line) is inside walk/decide, which
     holds no partitioner (walk.clj:82-95). The one hop is the base's copy, in a
     microbatch that commits or is tried again whole. :owners stays on the deciding
     task and is read only there
  2. SIDE-EFFECT RETRY TRACE    : N/A - no shape adds an effect outside Rama, and no
     topology appends (module.clj:25-26)
  3. PSTATE & MODULE OWNERSHIP  : PASS - the new table and :owners are written by
     the gate that owns the lane's six; the query reads. making-give (S2a) is
     Auth's alone and reaches the store inside an act
  4. BACK-ARROW CHECK           : PASS for the gate, which reads its own rows and
     never the query's answer. OPEN for the read path under S5(a): the runtime
     holds the query's last answer between two pushes, and Rama does not keep it
     level. S5 marks it for the stop on caching
  5. RETRY IDEMPOTENCE OF IDs   : PASS - a give's id is its giver's and is in the
     act; no topology mints one; a personal lane's first is Auth's (S2a) or a
     function of making-uuid (S2b)
  6. ACK & READ-AFTER-WRITE     : PASS for a streaming lane, as today. For the
     base: the answer on the base's task and the copy on a reader's home commit
     apart (microbatch_gate.clj, "What a reader sees"), so the query under a home
     can be one batch behind an answer, both ways. The base's facts lag so today
  7. STREAM vs MICROBATCH       : PASS - unchanged. The copy of the new table needs
     the microbatch's whole commit, which walk.clj's limit for a gate that copies
     already names
  8. PROXYSTATE PATH SHAPE      : PASS - each watch is one row or one run of keys of
     a top-level map, never a table. A viewer adds one run of her own; the lane's
     rows, the run of everyone and each lender's places are shared. Not measured
     for these shapes: L 4's 206 is of the probe's own
  9. SUBINDEXING & BOUNDS       : PASS - the new table is a flat top-level map;
     :owners is subindexed as :last-stamp is; :gives is cut at a bound. Carried:
     the query holds its task for (gives read) x (1 + chain) small runs with no
     yield, and its two bounds are not on the page
 10. HASH DETERMINISM & LOCALITY: PASS - depots hash by :lane as today; the query
     routes by the lane or the read's home, as sources.clj does
 11. TASKGLOBAL HANDOFF         : N/A - none
 12. POLICY & DURABLE OUTCOMES  : PASS - every decision stays in the gate, on the
     lane's rows; a refusal stays an answer and a line row. S3(b) would put one
     check at the door, and is not taken
 13. LIFECYCLE & DUPLICATE IDs  : PASS by shape - a give's place and its end's
     place are each written once and never over; only the terms take a second
     write (B3). That a second write is refused is the gate's, and no check holds
     it yet
 14. UNTRUSTED OBS & POISON REC : PASS by shape - each part of a key (kind, id,
     sitting, give) is a leaf that the declaration checks before a key is made, and
     the query tests its arguments at its head. As today a gate checks no sender:
     the kind in :who is what the sender said (README, 11 and 12 October entry 4)
 15. RUNTIME PROBE & TEST BAR   : FAIL - nothing of the page was loaded or run. L's
     probe ran the mechanism (one query on the task, pushes as signals, a table by
     holder copied behind |all), not these schemas, keys or watches

OVERALL: NEEDS-REVISION - no section asks for another design. 15 asks for a run
before code rests on the page, and S1, S2 and S5 are Sid's first.
RESIDUAL RISKS: 6, the base's lag: B4's proof of a transfer must hold it on a
home task. 9, the query's two bounds: B1, when the read is built. 4, the kept
answer: its own session, by the root page, before B4's read lands. 15, a probe
of these schemas: Loop 0c, or the first loop that touches the proofs.
```
