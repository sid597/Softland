# One fact, and the questions that hang on it

Written by the orchestrator session (Claude Fable 5.1) on 20 September 2026, first in chat, then copied here by script. Nothing here is a ruling. Sid rules.

**The problem.** Alice draws a relation. The store writes one fact. The store only appends, so it never goes back and changes that fact. Anything the fact does not carry at that moment can never be added to it. In ten years there will be billions of facts like it. So the question is which habits must be right from the very first fact.

## How to read the picture

The left column is one fact, in Sid's words for its parts, with alice's relation as the example. Each `←` is a question, hung on the part of the fact where it bites. The number is Sid's number for the question. A `+` line is a question the research says is missing from the table.

Under each question:

- `now`   the current answer. It is softland-ff's (Sid's main session), not Sid's ruling.
- `says`  what the research says about that question, and who says it.
- `case`  the one example that decides it, where there is one.
- `so`    where that leaves the current answer. STANDS: the research supports it. SHARPENED: keep it, and add something. CHALLENGED: there is a concrete case where it breaks. YOURS: it needs Sid's decision.

Marks at the end of a line: `[R✓]` the orchestrator opened the source itself. `[R]` a researcher reported it with a source. `[I]` an inference, a researcher's or the orchestrator's. `[N]` reconstructed from what an institution did. `r1` to `r7` name the research camp; `the-camps.md` beside this file says who each camp is. The full reports are in this folder; `loop/verified.md` lists the sources the orchestrator opened.

## The picture

```
entity      rel1               ← how is an id made so two never clash?                 (2)
                                 now    long, random, made by whoever offers it; ingest computes it
                                        from the source; no time hidden inside
                                 says   nobody disagrees that the offerer makes it. Rama's reason: if
                                        the store makes it, a retry makes a second one          [R r1]
                                        an id that shows its time becomes a promise people lean on:
                                        Twitter's clients sort by id                            [R r7]
                                        ingest: "are arXiv v1, arXiv v3 and the DOI one paper or
                                        three?" a formula freezes that answer for ever. a small
                                        lookup fact (source name → entity) can be corrected     [I r5]
                                 so     random half STANDS. computed-ingest half CHALLENGED.
                               ← what ids do the first entities get: base, gate?       (17)
                                 now    ordinary ids, written in the first facts, not words in code
                                 says   if "the gate" gets the same id in every store, two stores
                                        have the same gate, and "by whom: gate" is unclear   [I r3 r6]
                                 so     SHARPENED: shared kinds from a published list; this store's
                                        own gate and base get local ids.

key         :relation          ← a word, or an id too?                                 (3)
                                 now    an id; its word and its shape are facts on it
                                 says   Datomic does exactly this: the slot holds an id, the word is
                                        a fact about it, "Never remove a name". protobuf tags and
                                        Wikidata's P-numbers: same. nobody argued for words  [R r2 r3]
                                        a key's shape never changes; new shape, new key         [R r3]
                                        Datomic reads old facts through today's schema and lists it
                                        as a limit: write which grammar version let a value in  [R r2]
                                 so     STANDS. add the grammar version to the gate's yes.

value       {from f1, to c1…}  ← never removed, so how is one deleted?                 (9)
                                 now    bytes locked under their own key; delete = throw the key;
                                        the fact keeps the value's fingerprint
                                 says   XTDB built this shape in 2019. replay got stuck on erased
                                        documents; a "comes back to life" race is still open    [R✓]
                                        a plain fingerprint of a short value can be guessed:
                                        hash "yes", hash "no", compare                   [I four camps]
                                        locks age. a year-one value sits in every backup; a log that
                                        is never rewritten can never be re-locked            [I r7 r5]
                                        the key box is a second store; restore its old backup and
                                        the deleted comes back                          [I three camps]
                                        Europe's privacy board, on ledgers that cannot erase: even
                                        locked or fingerprinted, "should be stored off-chain"
                                                          [R✓; written about blockchains, fits by likeness]
                                        "forget alice" must find alice's values, so whose it is gets
                                        written when the fact is made. Nubank: "one thing … I would
                                        have liked to add, a customer identifier"               [R r2]
                                        what others do: erasable values live beside the log under a
                                        random name; "value is gone" is a normal state from day one
                                                                  [R: Matrix, Pijul, SSB's successors]
                                 case   the ageing lock.
                                 so     CHALLENGED. YOURS: a store beside the log is a second substance.

by whom     alice              ← who checks it? is her agent itself, or alice?         (8)
                                 now    the door checks it against the logged-in session; the agent
                                        is itself; "acts for alice" is a fact before its first write
                                 says   agent-as-itself is backed by regret: Yjs and Automerge
                                        recorded machines, not authors, and are adding authors in
                                        2026. Nostr's new scheme has the agent sign as the person,
                                        "which erases the agent from the record"                [R r5]
                                        a tool that runs at 3 a.m. has no session to check      [I r7]
                                        missing: under WHICH permission. see "a click".
                                 so     SHARPENED.

when        t25                ← whose clock? ever used for order?                     (11)
                                 now    the gate's only; never order
                                 says   two promises about t25, free today, impossible to add later:
                                        never earlier than the gate's last stamp; always later than
                                        everything in "based on"    [I r7; Amazon's DSQL runs this, N]
                                        then "as of t38" is a clean cut of the whole store, and still
                                        means something after the machines are re-divided
                                        a clock reading cannot be re-made by replay: keep t25 as
                                        data, never recompute it                           [R✓ Rama]
                                 so     SHARPENED, not reversed. the wall clock is still not trusted;
                                        the stamp is made orderable by rule.

layer       A                  ← who sees it before any permissions exist?             (16)
                                 now    base open to all; her layers private
                                 says   nothing against it. Rama has no read permissions at all, so
                                        the check where reads leave the store is all there is [R r1]
                                 so     STANDS.
                               ← can two gates ever write one layer?                   (10)
                                 now    never; a layer has one home store
                                 says   "one writer" is not what keeps it safe. Datomic, mid-failover,
                                        briefly had two and stayed right, because storage asked "is
                                        25 still current?"                          [R r6, Kingsbury]
                                 so     STANDS. add a number that goes up when the gate changes hands.

based on    [what she read]    ← is every read listed, even tiny ones?                 (11)
                                 now    every one, including reads that only made the tool fire
                                 says   listing every matched fact costs about 160× the fact  [I r7]
                                        Bayou tried lists in 1994; dropped: "could get large" [R r5]
                                        what works: the floor writes, once per act: the question
                                        asked, as of when, what the read was for, a fingerprint of
                                        the answer, "complete or partial". about 64 bytes. stale,
                                        doubt, journey, what the model was given, who read the
                                        erased thing: all five walks still work                 [I r7]
                                        Convex records "the index range we scanned", so rows that
                                        did not exist yet are covered too                       [R r5]
                                 so     SHARPENED: every read, yes. as questions, not lists of rows.
                                        written by the floor, never by the tool.
                               ← depends on it, or just how she got here?              (4)
                                 now    marked on each read; the floor guesses, the tool may correct
                                 says   what a read was FOR is known only in the moment. Pijul split
                                        "strict dependencies" from things merely "known"; Mercurial's
                                        developers wish they had recorded the kind of change    [R r5]
                                 so     STANDS.
                               ← follow the latest, or stay on the version she read?   (4)
                                 now    marked on each read too
                                 says   two camps: wrong question. v4 fixes a typo, v5 reverses the
                                        claim. no mark made in advance gets both right. the
                                        definition of stale, already a fact, can decide     [I r2 r7]
                                 so     CHALLENGED: drop this mark.
                               ← how far had the index really got?                     (5)
                                 now    written down, with what was withheld
                                 says   first half: all agree. never answer from beyond where the
                                        index has got; then you just write its position
                                                                    [R: Materialize, Bayou, Croquet]
                                        "what was withheld" tells her hidden facts exist. write the
                                        permission rules the answer was filtered under          [I r6]
                                 so     first half STANDS. second half SHARPENED.
                               ← "as of 38": one number, or a position per partition?  (10)
                                 now    per partition, never one number
                                 says   six camps: neither. a ticket the store hands out and keeps
                                        the meaning of. "A token can be one number today and a
                                        vector later."            [R: Zanzibar, Meta, Replicache, Figma]
                                        a bare list of positions dies the day 64 machines become
                                        256: it points at a layout that is gone. every position
                                        needs a layout number                               [I r1 r7]
                                 so     CHALLENGED on "never one number". with the two promises on
                                        "when", a plain stamp can be the ticket.

because of  fact 24            ← always filled? empty only if it starts a chain?       (11)
                                 now    yes to both, from the first fact
                                 says   Nubank's biggest regret: "implicit operations … that don't
                                        have origination information". nobody regrets it.      [R r2]
                                 so     STANDS.

version     25                 ← point at a fact by this number, or its own id?        (1)
                                 now    its own id; the number only orders
                                 says   agreed everywhere. a log position survives a Rama migration,
                                        not a re-division, a restore or a trim                  [R✓]
                                        three jobs, three things: a name (the offerer's), an order
                                        (25, the gate's), a check (a fingerprint)
                                                                [R r6: TigerBeetle, Apple, Temporal]
                                 so     STANDS. your hold stands too: 25 is the order, not the name.
                               ← that id: random, or computed from what the fact says? (1)
                                 now    computed, plus a random salt
                                 says   everyone who named by fingerprint froze a byte format for
                                        ever. SSB froze one JavaScript engine's JSON printing. Git is
                                        nine years into leaving SHA-1                           [R r5]
                                        alice's agents, in three languages, print one map two ways:
                                        one offer, two ids, and the retry lands twice           [I r6]
                                        salt kept: an erased yes/no is found by guessing. salt thrown
                                        away: nobody can ever check the name                    [I r5]
                                 so     CHALLENGED. random name; the fingerprint rides beside it,
                                        tagged with its method.
                               ← 37 replaces 25: keep "replacing 25" on it?            (6)
                                 now    keep it
                                 says   keep, and it comes free: the offer already says "I expect
                                        25". keep the offer exactly as offered               [I r6 r4]
                                 so     STANDS. the reopen goes away (see "one act").
                               ← what must share a partition to stay ordered?          (10)
                                 now    everything about one entity
                                 says   the one real argument, Rama camp against Datomic camp.
                                        scattered by entity, every read of alice's own layer touches
                                        every machine, and her agent's 200 facts cannot land together
                                        both accepted: a layer with one owner lives in one place with
                                        one order; team layers and the base spread by entity.
                                        "classify by writers, not visibility"
                                                                    [I; the idea was mine, weigh that]
                                        Rama can do it [R✓]. partition count is fixed today; Rama
                                        calls lifting that "high priority"                       [R✓]
                                        day-one cost: every offer says which kind of layer it is for
                                 so     CHALLENGED → the hybrid. YOURS.

the log itself      storage             ← never rewritten, or only never lost?              (0)
                                          now    never rewritten, as you said
                                          says   nobody holds it, not even Rama: migrations keep
                                                 every position and rewrite the bytes            [R✓]
                                                 it is four promises: never lost · never different
                                                 under one name · never re-ordered · provable to a
                                                 stranger                                      [I r4]
                                                 "a promise about logic, not bytes": Dolt rewrote
                                                 every commit and proved the rows unchanged    [R r5]
                                                 hidden question: WHICH log? in Rama people append
                                                 offers; the gate writes facts into tables     [I r1]
                                          so     YOURS. "the map must not lie" needs the first three,
                                                 plus "never changed silently". not the same bytes.
                                        ← plain maps? never trimmed? backups?               (13)
                                          now    yes, yes, and backups inside the delete plan
                                          says   maps: fine. never trimmed: see the hand.
                                                 a way that rewrites nothing: keep every envelope and
                                                 every yes/no for ever; let the refused or sampled
                                                 VALUE expire                                  [I r4]
                                          so     maps STAND. "never trimmed" YOURS.
before anyone       the first facts     ← what are they, who writes them, same everywhere?  (17)
                                          now    finite, written once by the floor, ids computed from
                                                 content
                                          says   Kleppmann refused computed ids for Automerge: "a very
                                                 fragile API" [R r5]. a year-four rebuild changes key
                                                 order and the ids quietly stop matching       [I r7]
                                          so     CHALLENGED on "computed": publish them as a list.
beside each fact    the gate's yes/no   ← kept? where? refusals too?                        (7)
                                          now    kept for ever, next to the fact; refusals in the
                                                 offerer's own session layer
                                          says   needed to be correct, not only for audit. Rama: "An
                                                 exception doesn't mean the append did not go
                                                 through" [R✓]. her agent retries and must find its
                                                 old answer by the offer's id
                                                 never work the answer out again: the gate's code
                                                 will have changed                             [R r4]
                                                 where: in the same step as "is 25 still current?",
                                                 where rel1 lives. TigerBeetle once got it wrong:
                                                 refuse, crash, retry, the cell has moved, the offer
                                                 now succeeds                                  [R r6]
                                                 volume: 30 agents on one cell = 29 refusals per fact
                                          so     STANDS. the yes also says: code version, grammar
                                                 version, which grant. refused VALUES for ever: YOURS.
start of a session  who, which runtime  ← is the running version written down?              (12)
                                          now    yes, with the kind of machine; every rebuild too
                                          says   Facebook's Delos: code-version mismatch was "the
                                                 only source of inconsistency in production"   [R r4]
                                                 the same tool on another chip or compiler can give
                                                 another answer: "kind of machine" is right    [R r7]
                                          so     STANDS. also on every yes/no and every "what was
                                                 shown", not only at session start.
the hand            point, select, pan  ← which motions become facts by default?            (14)
                                          now    point, select, mode, pan and zoom, at the tick
                                          says   Webstrates kept cursors by default (30–50 a second)
                                                 and took it back [R r3]. at every tick: 21.6 billion
                                                 records a year for 300 people [I r5]. three camps say
                                                 no to ticks kept for ever.
                                                 against them, your words: "we cannot predict this
                                                 without … storing these signals somehow"
                                                 nobody says don't capture. they say: record "shown"
                                                 when it changes, not when time passes; raw motion
                                                 under a stated keep-time
                                          so     YOURS.
                                        ← is being shown the same as looking?               (14)
                                          now    no; looking is captured as its own signal
                                          says   only "shown" can be known. "looking" is a guess [I r3]
                                          so     SHARPENED: "shown" is the fact; "looking" is at most
                                                 an offer from a tool that guesses.
a click             tools that match    ← which tools may act in her name?                  (15)
                                          now    only ones she enabled; her own layers' on by default
                                          case   alice enabled tool T: "file a summary on the thing a
                                                 note names". bob may only write notes. bob writes a
                                                 note naming something in alice's layer. T fires by
                                                 match and writes bob's words there, as alice. every
                                                 check passes. same hole: a poisoned paper read by
                                                 her summarising model      [I r3; Hardy's deputy, 1988]
                                          says   the fix: the offer says which grant it uses; the gate
                                                 checks only that one. a click IS a grant: this
                                                 selection, this tool, now. a tool with no grant can
                                                 only give running answers. Matrix already writes the
                                                 permitting events into every event            [R r5]
                                                 cannot be added to old facts.
                                          so     CHALLENGED. not "which tools" but "what was handed
                                                 over, for what, until when".

+ one act           the saying          ← her agent turns one reply into 200 facts. one act, or 200? (new)
                                          says   five camps, separately: the offer IS the act. keep it
                                                 exactly as offered, under the offerer's id. it may
                                                 hold many facts. who, grant, reads, because-of: once,
                                                 on the act. what the gate adds goes on its yes/no.
                                                 Hickey: "the 'saying' of it *is* the transaction" [R✓]
                                          case   "fact 117 fails; 199 stand as a reply no model gave"
                                          so     the biggest finding. it moves (1) (6) (7) (11) and
                                                 because-of onto the act, and shrinks what every
                                                 fact must carry for ever. YOURS.
+ a running answer  never stored        ← two reads at different moments: can the screen show a
                                          state that never was?                              (new)
                                          says   yes [R r7, Brandon]. by your own rule: compute at
                                                 one "as of", or paint it inexact.
                                        ← the floor was rebuilt 200 times. can "what was shown"
                                          still be re-made?                                  (new)
                                          says   not reliably [R r6: Temporal, Restate]. so "what was
                                                 shown" carries a fingerprint of it; what went to a
                                                 model carries the content.
+ value                                 ← when was it true, not when was it written?         (new)
                                          says   a field built from papers has "true from" in its
                                                 subject. Datomic: an ordinary key. XTDB: on every
                                                 record. not choosing is choosing Datomic's     [R r2]
```

Reply on any line. Agree is a hold; say ruled where it is meant. Where a line reads thin, ask for that one in full.
