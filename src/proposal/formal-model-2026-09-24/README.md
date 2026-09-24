# A small executable model of the store's rules

Written 24 September 2026, before any Rama code, to find out whether the
rulings in `../frame-2026-09-15/PROGRESS.md` contradict each other. It encodes
those rulings (the "Ruled in chat" lines, the nine, the four layer kinds and
the two consequences) as plain data and pure functions. test.check generates
random histories of offers, retries, promotions, failovers, forgets, name
reuses and revocations. After each history the eight properties Sid named are
checked, with a ninth for revocation (`rv`) and three extra checks.

Two rounds, both on 24 September. The first found six places where
PROGRESS.md was silent or pulled against one of the eight properties. Sid
confirmed all six other readings as his, so they are the baseline now. The
second round, from `STARTER-round-2.md`, added changes A to E and ran again.
On 25 September Sid made the name tag his and ruled the one case A left open.
Neither changed a result: the baseline already read them that way.

No Rama, no network, no disk. PROGRESS.md was the only design source read.

## Run it

```
cd src/proposal/formal-model-2026-09-24
clojure -M:run 1000                 # fixed histories, coverage, every configuration
clojure -M:run 10000 baseline 7     # one configuration, another seed
clojure -M:run 10 scenarios         # the fixed histories alone
```

It needs Clojure 1.12.0 and test.check 1.1.0, both already in `~/.m2`. The
seed is fixed (20260924), so a run repeats exactly. At 10000 histories one
configuration takes one to three minutes, and the whole matrix about half an
hour in one JVM. Since the runner takes a configuration name, configurations
can run side by side: twelve at a time, the matrix took about five minutes.
For each check that fails, the output gives the smallest failing history
test.check shrank to, a trace of what happened in it, and the violation.

- `src/formal/model.clj`: the store. Two fact stores with a gate each, placement, locks, forgetting, promotion, names, permissions, failover, reads.
- `src/formal/properties.clj`: the eight properties, `rv`, and three extra checks (x1 to x3).
- `src/formal/scenarios.clj`: fixed histories for the cases Sid named in change A, and for B and D.
- `src/formal/run.clj`: generators, the configurations, coverage counts, the runner.

## What is in the toy

- **People.** Alice and Bob.
- **Entities.** e0 to e3, and one per person for their permission facts.
- **Fact keys.** Two: `:note`, a plain value, and `:mention`, a value that names one or two people, so the key's grammar makes them subjects.
- **Layers.** Five: Alice's personal layer, her hand session, her agent session, one group (Alice and Bob), and the base.
- **Two stores, each with its own gate (ruling 1).** The stream gate serves the one-owner layers. It handles one event on one partition at a time and replays an unfinished offer from its start after a failover (at least once). The micro gate serves the shared layers. It decides a batch across partitions at prepare and commits it in one step.
- **Partitions and placement (ruling 2).** Three partitions per store. One-owner layers are placed by layer, shared layers by entity. The class on the offer is checked against the layer's class fact. A layer can be re-classed, and a re-classed one-owner layer moves to the micro gate.
- **Leaders and epochs.** Each gate has a leader with an epoch. A failover makes a new leader with the next epoch and a clock that may be off by up to 3. The deposed leader can still try to act on what it held; with fencing, its epoch is refused.
- **Locks (ruling 7, change A).**
  - Every value gets its own lock at write, wrapped under the person locks of the people it is about. A wrap names people whose locks are all required, and people of whom any one is enough.
  - In a one-owner layer the owner's lock is always required. Another subject's lock matters only when the value is marked to die with any of them.
  - In a shared layer there is no person owner and 7b applies as written. A value about one person dies with them. A value about two survives one of them. A marked value dies with any.
  - In personal and hand layers the lock is a row in the lock store; in agent, group and base layers it is kept in the record. A mark on the value overrides this.
  - Forgetting one value deletes its lock row, or, for a lock kept in the record, the operator excises it. Forgetting a person destroys their person lock.
  - A layer setting switches lock grain to one lock per act.
- **Subjects (ruling 8).** Each value's lock is wrapped under its own subjects: the layer's owner and the key's grammar applied to that value. The act's subject slot is the union, for finding. The tool contributes none in this toy.
- **Clock promises (ruling 4).** A stamp is never behind the leader's wall clock, never backward on a partition, and always later than anything the act stood on.
- **Promotion, in two steps (reading 6, change B).**
  - Alice's request to promote a value is admitted in her layer.
  - The gate's next step is the read-out, on the partition that holds the value's lock. It opens the value through the lock and writes the crossing fact there. Then it sends the landing offer, which stands on the crossing fact.
  - A forget ordered before the read-out refuses the promotion. A forget after it does not recall the copy.
  - A read shows the promotion as pending (not yet read out, still forgettable), crossed (read out, not landed), done, or refused.
  - The landing's name is derived from the request's under a scheme (`landing:o5`). So is the read-out's (`crossing:o5`).
- **Names (changes C and E).**
  - Every offer carries a digest of what it says, and every answer is recorded with the digest it was given for.
  - A layer placed by layer keeps its answers on its home partition. For every micro decision the micro gate also writes a name row, on the partition the name picks, in the same commit.
  - The lookup takes a name and a layer and reads one partition. It reads two for an offer the store places itself in a one-owner layer (a forget, a setting, a read-out): the layer's home, then the name row.
  - A name's scheme tag carries the layer and the class it was made for, so the name reaches one gate only. Round 2 proposed this reading; Sid made it his on 25 September.
  - A gate refuses, on its face, an offer whose layer or class differs from its name's tag. It refuses, by the digest, an offer under a name it has decided for other content.
- **Permissions (change D).**
  - Permissions are first facts in every run. Each is a fact in some layer: in the layer it grants writes into (own), or in Alice's hand session (session).
  - Every offer by a person names one. The operator and the store's own steps act at the root and name none.
  - A gate refuses a permission that does not cover the write, one kept in another layer than the write's, one that does not exist, and one that is revoked.
  - A revocation is a fact in the permission's layer, standing on the permission.

## Order between the two stores

Sid's rule, which sits under reading 6 and changes B and D: order between the
two stores exists only through stood-on. A landing stands on its forward, so
landing-after-forward is defined. A forget and a forward on the same
partition are ordered. A forget against a landing is ordered by nothing. So
any check one gate makes against the other store is a read, stamped in the
checking store, never atomic with the other store's writes, and the model
must never promise anything that needs such an order.

The properties are the ruler, not the store: they may use the true order of
every effect (`:order` in the model). The gates never do.

Where the model relies on the rule:

- **Landing after read-out.** The landing stands on the crossing fact, so the micro gate stamps it later (P5 checks this). The micro gate is assumed to learn the stamp of what an offer stood on, even in the other store.
- **A value's forget against its read-out.** Both are decided on the partition that holds the value's lock row, so one of them is first. A forget ordered first makes the read-out refuse. P6's copy exemption rests on this: a copy is exempt when its crossing fact came before the forget.
- **A person's forget against a read-out.** The forget destroys the person lock; the read-out opens the value through it. The crossing fact is written only if the opening succeeded, so the exemption rests on the crossing fact existing, not on comparing stamps across stores. The order itself lives inside the lock store (a lock opened, then destroyed), which the toy runs one step at a time. Reasoned, not shown.
- **A forget against a landing.** Nothing orders them, and the model promises nothing about it: a forget after the read-out does not recall the copy. The configuration where the landing checks its source shows that no check closes it (trace 16).
- **Reads across the stores (B).** A read as of moment T reads both stores, and a landing admitted after the read can be stamped at or before T. So P7 judges a read by what the store held when it was taken. x2 judges in hindsight and fails in every configuration; the model does not promise x2 (trace 1). What that means for read entries is open; see Open below.
- **Permissions (D).** Under D a permission, its revocation and the writes it governs are ordered by one gate: one stream partition for a layer placed by layer, the micro store's commit order for a shared layer. Under the literal reading the micro gate checks a permission kept in the stream store, as a read at prepare, and misses a revocation admitted before the commit (trace 14).
- **Name reuse (E).** A gate can refuse a reuse only when it holds the name's first use. The name's tag keeps each name to one gate, so no gate needs to look in the other store (trace 13).

Where the toy is kinder than the rule, and would not show a failure a real store could have:

- **Layer settings.** Class and lock grain sit in one map that both gates read at once, so a re-class admitted in one store is known in the other instantly. A real micro gate learns it by a read. A stale view only refuses (class mismatch), so no property should turn. Reasoned, not shown.
- **Stream partitions.** The stream gate reads other stream partitions at once: a chain head, a value's lock, a permission kept in Alice's session. Partitions of one store have no order between them either, so a real check against another partition is a read too. The toy cannot show a race there.
- **After a re-class.** A one-owner layer's earlier values, their locks and its permission facts stay in the stream store, and the micro gate reads them as settled history. That rests on the re-class fact standing after them on the same partition.
- **A person's forget is stamped after everything so far.** One global stamp, used only for the "erased on" date a read shows.

## The readings

Where PROGRESS.md is silent, the model takes a reading, and each reading is an
entry in the config. `ruled` holds the literal readings; `baseline`, the
default, turns every entry in `readings`. The promotion's two steps with the
crossing fact are not an entry: Sid made them a uniformity rule, chosen once.

| config entry | `ruled` (literal) | `baseline` | whose | what in PROGRESS.md it rests on |
|---|---|---|---|---|
| `:act-layer` | `:per-fact`: layer is a named part of each fact | `:per-act`: the gate refuses an act whose facts name another layer | Sid, round 1 | the envelope lists "layer" among the named parts without saying fact or act |
| `:reclass-moves-gate` | `false` | `true`: a re-classed one-owner layer moves to the micro gate | Sid, round 1 | ruling 2, "a hot layer can be re-classed"; ruling 1 picks the gate by ownership |
| `:forward-name` | `:minted`: the store names the landing as it sends it | `:derived`: named from the request's name under a scheme | Sid, round 1 | "Names: random, made by the offerer before the gate, scheme-tagged" |
| `:lock-subjects` | `:act`: per-value locks under the act's subjects | `:fact`: under the value's own subjects | Sid, round 1 | ruling 7, "Per-value locks inherit the act's subject list"; ruling 8 |
| `:p4-erasure-exempt` | `false` | `true`: an erasure shows only its date | Sid, round 1 | Forget: "time travel shows 'erased on this date'" |
| `:p6-copies` | `:none`: no copy may return a forgotten value | `:crossed`: a copy whose crossing fact came before the forget is exempt | Sid, round 1; read from the crossing fact since B | the second consequence; Sid's exposure dimension |
| `:wrap` | `:any-subject`: the owner is one subject among others | `:owner-required` | Sid, change A | rulings 7, 7b and 8 |
| `:name-row` | `false` | `true`: the micro commit writes a name row | Sid, change C | P1, "findable by its name"; ruling 2's placement |
| `:digest-check` | `false` | `true`: a reuse is refused by its digest | Sid, change E | "Names: random, made by the offerer before the gate" |
| `:name-tag` | `:none` | `:layer-and-class`: a name reaches one gate only | Sid, 25 September (proposed in round 2, to make E hold) | "Names: ... scheme-tagged" |
| `:permission-home` | `:anywhere`: a gate takes a cited permission wherever it lives | `:in-their-layer`: only one kept in the layer the write goes into | Sid, change D | "Every offer names the permission it acts under. The session is the root; narrower permissions are ordinary facts under it." |

Other values kept runnable: `:forward-name :carried`, a second random name the
person makes up front (reading 3 asked to keep it, to compare);
`:wrap :owner-and-an-other` and `:wrap :owner-and-marked` (below);
`:p6-copies :landed-before`. Two entries are used only by demonstration
configurations: `:landing-checks-source` (the micro gate checks the source
again while landing) and `:fencing` (on in `ruled` and `baseline`).

**Why the name tag.** As Sid wrote it, E fails: a reuse that reaches the other
store is invisible to the digest, it lands, and two facts share one id
(traces 13 and 11). A tag that says which layer and class a name was made for
keeps every name to one gate. A gate refuses a mismatched offer on its face,
reading nothing, and the lookup follows the tag. Sid made the reading his on
25 September and it stays in the baseline; its own row shows E's failure
without it.

**Two readings of A.** A mention of Bob in Alice's own layer, when Bob is
forgotten: Sid ruled on 25 September to keep 7b as ruled, so it survives. In a
one-owner layer, then, only the owner's lock and marks count, and the third
reading of A differs from A only in shared layers. The other reading,
`:owner-and-an-other` (the owner's lock, and one of the other subjects'), stays
runnable: under it the value dies with Bob. The P6 (b) standard and x1 are
fixed at A as ruled, so there x1 objects (trace 10). The third reading,
`:owner-and-marked`, gives an unmarked subject in a shared layer no wrap at all.

## Results

Seed 20260924, 10000 histories per check per configuration, histories up to
60 ops. "holds" means test.check found no counterexample; that is not a
proof. The number is the history where the first counterexample came. Three
histories in five draw from every op, one in five from ops around promotions,
and one in five from ops around permissions, so that the paths of B and D
are reached.

| configuration | P1 one answer | P2 whole acts | P3 no forks | P4 no read past | P5 clocks | P6 forget | P7 pending | P8 not twice | rv revoked | x1 | x2 | x3 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ruled (every literal reading) | fails (1) | fails (46) | holds | fails (55) | fails (48) | fails (31) | holds | fails (102) | fails (170) | fails (40) | fails (1075) | fails (24) |
| **baseline** (every reading turned) | holds | holds | holds | holds | holds | holds | holds | holds | holds | holds | fails (660) | holds |
| baseline, layer on each fact (1 back) | fails (94) | fails (46) | holds | holds | fails (1779) | holds | holds | holds | holds | holds | fails (660) | fails (1779) |
| baseline, re-class keeps the stream gate (2 back) | fails (222) | fails (222) | holds | holds | holds | holds | holds | holds | holds | holds | fails (660) | holds |
| baseline, the store names the landing (3 back) | holds | holds | holds | holds | holds | holds | holds | fails (102) | holds | holds | fails (660) | holds |
| baseline, locks under the act's subjects (4 back) | holds | holds | holds | holds | holds | fails (217) | holds | holds | holds | fails (47) | fails (660) | holds |
| baseline, P4 literal (5 back) | holds | holds | holds | fails (55) | holds | holds | holds | holds | holds | holds | fails (660) | holds |
| baseline, P6 literal (6 back) | holds | holds | holds | holds | holds | fails (359) | holds | holds | holds | holds | fails (660) | holds |
| baseline, owner one subject among others (A back) | holds | holds | holds | holds | holds | fails (31) | holds | holds | holds | fails (40) | fails (660) | holds |
| baseline, no name row (C back) | fails (1) | holds | holds | holds | fails (6404) | holds | holds | holds | holds | holds | fails (660) | fails (92) |
| baseline, no digest (E back) | fails (24) | holds | holds | holds | holds | holds | holds | holds | holds | holds | fails (660) | holds |
| baseline, names carry no tag (proposed tag back) | fails (1734) | holds | holds | holds | fails (771) | fails (3558) | holds | holds | holds | holds | fails (660) | fails (39) |
| baseline, permissions taken from any layer (D back) | holds | holds | holds | holds | holds | holds | holds | holds | fails (170) | holds | fails (1075) | holds |
| baseline, landing name carried (3, other form) | holds | holds | holds | holds | holds | holds | holds | holds | holds | holds | fails (660) | holds |
| baseline, A read as owner and one other | holds | holds | holds | holds | holds | holds | holds | holds | holds | fails (163) | fails (660) | holds |
| baseline, third reading of A | holds | holds | holds | holds | holds | fails (47) | holds | holds | holds | holds | fails (660) | holds |
| baseline, copies exempt only if landed before | holds | holds | holds | holds | holds | fails (359) | holds | holds | holds | holds | fails (660) | holds |
| same, and the landing checks its source | holds | holds | holds | holds | holds | fails (476) | holds | holds | holds | holds | fails (660) | holds |
| baseline, no fencing | holds | fails (1720) | holds | holds | fails (823) | holds | holds | fails (1720) | holds | holds | fails (660) | holds |

The extra checks: x1, a person's forget takes nothing it should not (the
mirror of P6 (b)); x2, P7's crossed state judged in hindsight; x3, one id
names one fact.

Seeds 1 and 2 give the same verdict in every cell but three, all on rare
paths: P5 with the layer on each fact (fails on the fixed seed only), P5
without the name row (holds on seed 1), and P6 with the landing checking its
source (holds on seed 2). The baseline held under all three seeds, 30000
histories per check, except x2, which fails under every seed. Every reading
flipped back alone brings back its own failure under every seed. The carried
landing name behaves as the derived one everywhere.

### The fixed histories

`clojure -M:run 10 scenarios` plays them. Under the baseline each does what
was said:

- A, as Sid named them: in Alice's own layer a note and a mention of Bob both die with Alice; in the group a mention of Bob alone dies with Bob; a plain note about no one survives both forgets; a value about Alice and Bob survives either one alone, and dies when both are gone; marked, it dies with Bob. And the case Sid ruled on 25 September: in Alice's own layer a mention of Bob survives Bob.
- B: a value forget queued before the read-out, and Alice forgotten before it, each show pending, then refused. A value forget after the read-out, and Alice forgotten after it, each show crossed, then done, and the copy stays.
- D: a group write under Alice's session permission, revoked in her session between the micro gate's prepare and commit, is refused. So is a group write under her group permission, revoked in the group first.

The other configurations differ where they should. With A flipped back, the
mention of Bob in Alice's layer survives her forget (P6 objects). Under the
third reading, Bob's group mention of himself survives Bob's forget, as Sid
expected, and so does a group value about Alice and Bob after both forgets;
P6 objects to both. Read as the owner and one other, the mention of Bob in
Alice's layer dies with Bob (x1 objects). With P6 literal, both copies that
outlived their source draw P6. With D flipped back, the session-kept
permission's write is admitted after its revocation (`rv` objects).

### The smallest failing histories

Each is the history test.check shrank to on the fixed seed, told in words.
The run output has the traces.

1. **A read that hindsight contradicts (baseline, x2).** Nine ops. Alice promotes a note: the request is in at stamp 4, the read-out writes the crossing fact at stamp 5 and sends the landing, and the micro leader prepares it. Alice writes another note at stamp 7. A read as of 7 shows the promotion crossed. Then the micro gate commits the landing at stamp 6, at or before the read's moment. When the read was taken the landing was not there, so P7 holds; judged in hindsight the read should have said done. Nothing orders a micro stamp against a moment read on the stream side.
2. **The layer on each fact (1 back): two moments (P2).** One op. Alice offers one act with a note in her hand session and a note in her personal layer. Placed by layer, they land on stream partitions 1 and 0 as two events, at stamps 6 and 2. In P1's one-op history the act's answer is on the hand session's partition, but the act names her personal layer, so the lookup finds nothing.
3. **A re-classed layer kept on the stream gate (2 back): two moments (P2).** Three ops. The operator re-classes Alice's personal layer to by-entity. She offers an act on e0 and e1, which the stream gate places by entity and lands at stamps 5 and 6. In P1's three ops she offers one note after the re-class. The stream gate decides it on partition 0, but its name's tag says by-entity, so the lookup reads the micro name row, which the stream gate never writes: it has one partition per event.
4. **The store names the landing (3 back): two landings (P8).** Three ops. Alice writes a note and asks to promote it, and her client sends the request twice. The gate answers the second from its record, and the read-out, already decided, sends the landing again under a fresh name. The micro gate cannot tell the two apart, and both land.
5. **Locks under the act's subjects (4 back) (P6, x1).** Three ops each. In the group Alice writes one act with a mention of Bob and a mention of herself, and she is forgotten. Her mention survives, because its lock is wrapped under the act's subjects, Alice and Bob. The mirror: Bob writes a group act with a plain note and a mention of himself, and he is forgotten. The note, about no one, is erased because it inherited Bob.
6. **P4 literal (5 back).** Six ops. A group value is about Alice, and Alice is forgotten at stamp 7. A read as of 6 shows it "erased on 7".
7. **P6 literal (6 back).** Five ops. Alice promotes a note: the read-out crosses at stamp 5 and sends the landing. She forgets the note (stamp 6). The copy lands at stamp 8 and returns her value. Its crossing fact came before the forget, so the baseline exempts it.
8. **The owner one subject among others (A back) (P6).** Three ops. Alice writes a mention of Bob in her own layer and is forgotten. The value survives through Bob's lock. In x1's four ops Alice is forgotten first and then writes a mention of Bob, which only Bob's lock still opens; Bob's forget takes it, which A never lets a non-owner's forget do.
9. **The third reading of A (P6).** Three ops. Bob writes a group mention of himself and is forgotten. The value survives: it has no wrap.
10. **A read as the owner and one other (x1).** Three ops. Alice writes a mention of Bob in her agent session, and Bob is forgotten. The value dies with Bob; 7b as ruled keeps it.
11. **No name row (C back) (P1, x3, P5).** Zero ops: the first facts themselves. The grant of Alice's group permission lands on micro partition 1, the partition its entity picks. Its name picks another, and the lookup finds nothing. Without the name row the micro gate also checks a reuse only on the partitions its facts land on. So a reuse that lands elsewhere is admitted, two facts share one id (x3), and a forget standing on that id reads as standing on itself (P5).
12. **No digest (E back) (P1).** Two ops. Alice writes a note as o0 and reuses o0 for another note. The stream gate answers the reuse from its record, yes, though nothing of it landed.
13. **Names that carry no tag (P1, x3, P6, P5).** In x3's two ops Alice's o0 lands on the stream, Bob reuses o0 in the group on the micro gate, neither gate can see the other's record, and both land: two facts with one id. In P6's five ops Bob's reused o0 is about Bob, and Bob is forgotten. P6 then reads Alice's promoted copy of her o0 as a copy of an erased value: a forget of his value looks like a forget of hers. In P1's three ops the operator re-classes Alice's layer as o0. Alice reuses o0 for a note in her layer, which now goes to the micro gate, and it lands. The lookup reads the stream first and answers "name taken" for content that did land.
14. **Permissions taken from any layer (D back) (rv).** Five ops. Alice writes into the base under the permission kept in her session. The micro gate prepares the write. The operator revokes that permission in her session (stream partition 1, stamp 6). The micro gate commits the write at stamp 3. It was admitted after the revocation and stamped before it; nothing orders the two.
15. **Copies exempt only if landed before (P6).** The history of trace 7: the copy landed after the forget.
16. **The landing checks its source (P6).** Eight ops. The micro gate prepares the landing and finds the source readable. The stream gate admits Alice's forget (stamp 7). The micro gate commits the copy (stamp 6). A check one gate makes against the other store is a read, and this one is stale by the commit. Round 1 found the same race in nine ops.
17. **No fencing (P2, P5, P8).** Five ops (six for P2 and P8). Alice writes a group note, and the micro leader prepares it. The micro store fails over, and the new leader prepares and commits the note at stamp 4. Then the deposed leader commits its old batch, and the same note lands again at stamp 3. It is admitted twice, at two moments, and partition 0's stamps go back.

### How much of the space the histories reached

Of the histories under the baseline, how many reached each path: 1500 and
10000 on the fixed seed, and 10000 on seeds 1 and 2.

| 1500 | 10000 | seed 1 | seed 2 | path |
|---:|---:|---:|---:|---|
| 655 | 4441 | 4407 | 4438 | a retry under the same name |
| 448 | 3073 | 3074 | 3055 | the stream gate met a name it had already decided |
| 76 | 522 | 471 | 501 | the micro gate met a name it had already decided |
| 247 | 1690 | 1718 | 1721 | a name was reused for other content |
| 70 | 488 | 514 | 532 | a reuse was refused by its digest |
| 207 | 1411 | 1442 | 1444 | a reuse was refused on its face, made for another layer or class |
| 0 | 0 | 0 | 0 | two offers under one name both landed |
| 75 | 530 | 554 | 561 | a stream failover replayed an unfinished offer |
| 18 | 132 | 147 | 143 | a micro failover lost a prepared batch |
| 29 | 201 | 217 | 208 | a deposed leader's write was refused by its epoch |
| 75 | 541 | 525 | 540 | a promotion landed |
| 11 | 104 | 83 | 81 | a read showed a promotion pending, not yet read out |
| 6 | 52 | 39 | 43 | a read showed a promotion crossed, not yet landed |
| 51 | 417 | 416 | 406 | a promotion was refused because its source was erased |
| 11 | 71 | 47 | 51 | a promotion landed and its source was later forgotten |
| 109 | 879 | 878 | 876 | a value forget deleted a lock row |
| 101 | 749 | 722 | 720 | a value forget was an operator excision |
| 77 | 466 | 489 | 462 | a person forget erased a value that existed before it |
| 93 | 620 | 685 | 662 | a read as of an earlier moment showed an erased value |
| 175 | 1285 | 1268 | 1265 | a layer was re-classed |
| 313 | 2209 | 2172 | 2172 | one act landed on two partitions of the micro store |
| 47 | 318 | 346 | 351 | a per-act lock was used |
| 12 | 99 | 107 | 102 | an offer was refused: stale replaces |
| 45 | 327 | 321 | 317 | an offer was refused: class mismatch |
| 225 | 1637 | 1615 | 1612 | an offer was refused: fact outside the act's layer |
| 460 | 2978 | 3020 | 3028 | a permission was revoked |
| 100 | 635 | 629 | 609 | an offer was refused: its permission revoked |
| 329 | 2281 | 2265 | 2257 | an offer was refused: its permission from another layer |
| 0 | 0 | 0 | 0 | an offer was admitted after its permission's revocation |

The two paths at zero are violations the baseline should never reach. Every
other path was reached by 1500 histories. What needed more than 1500 were
failures, not paths. On the fixed seed: the double commit without fencing
(1720), the layer on each fact colliding ids (1779), the missing name row
seen by P5 (6404), and the untagged reuse after a re-class (1734). On seed 1:
x2 under the baseline (3596), and the landing check race (1906). B's paths
are the thinnest: 6 histories showed a promotion crossed at 1500, and 52 at
10000.

## Open

- **A read's moment across the two stores (x2).** A read as of moment T is final only for what the store held when it was taken, because the other store can still admit something dated at or before T. So a deterministic tool's short read entry (pattern, moment, fingerprint, ruling 3) re-run later as of the same moment can match more. There are two roads. A read closes its moment in every store it read, or the entry carries one moment per store. Not ruled yet; the rig will help decide.

## What was assumed, not modeled, or only reasoned

These readings are fixed in the model rather than turned into config entries:

- **Chains stay within a layer.** A fact replaces only a fact in its own layer with the same entity and fact key.
- **Retries are answered from the record.** The gate looks up the offer's name before any other check; with the digest, only an offer with the same digest gets the recorded answer.
- **Answers are durable.** An answer is recorded where it was decided, and in the name row for the micro gate, durable before it is given, so a failover loses nothing that was answered.
- **Rama's gate behaviour.**
  - The stream gate's event on one partition is atomic, and it is at least once across a failover.
  - The micro gate's batch is atomic across partitions, decided at prepare and made visible at commit.
  - These are the Rama claims PROGRESS.md says the rama skill verifies before code. They are assumed here, not verified.
- **Stood-on stamps are knowable.** A gate can learn the stamp of anything an offer stood on, in either store.
- **Person locks.** They sit in a lock store outside the partitions. A person's forget is stamped after everything so far.
- **Owner subjects.** A group or the base contributes no owner subject, so a promoted copy's subjects come from the target layer and the key's grammar only.
- **Store-control facts have no lock.** These are forgets, settings, promotion requests, crossing facts, permissions and revocations.
- **The root.** The operator and the store's own steps (read-outs) name no permission. Forgets and settings are placed with what they act on, so their names' tags carry no class.
- **A forget is an offer.** It names a permission, so a person whose write permission to a layer is revoked cannot forget in it either. Whether a person can always forget their own values is not ruled.

Not modeled:

- Visibility, read entries and fingerprints, and opaque values.
- Retract, restore and replay of forgets, and excision reaching replicas.
- Tool-declared subjects and group rules.
- Permissions granted after the first facts; only revocation is modeled.
- A client sending an offer under a derived name (`landing:o5`).

Found only by reasoning, not by running:

- **Derived names are predictable.** A client that sent an offer named `landing:o5` first would take the name, and the real landing would be refused by its digest. So the landing scheme has to be the store's alone: a gate refuses a client offer under it. The carried name is random, but the person who made it knows it too.
- **Crossed is not a promise of done.** A crossed promotion can still be refused at the landing: by a revocation of Alice's permission for the target, or by a stale replaces there. The model allows it, and so does P7.
- **Stream checks against other stream partitions** are reads without order, as across the stores; the toy's instant reads hide this.
- **After a re-class** the old store keeps the layer's earlier values, their locks, its permission facts and its answers. Forgets follow the values, and the lookup takes two reads for offers the store places itself.
- **Grain switch across partitions.** "Affects only values written after the switch" needs an order between the setting fact and acts on other partitions. The model switched grain only in layers placed by layer, where that order exists.
- **Promotion drops the layer owner as a subject.** Alice's note in her own layer is about Alice, but its copy in the group is not, so her forget after the read-out does not reach it. Reading 6 says that is right; the copy crossed.
- **Short read entries cannot be re-run after a forget.** A deterministic tool's short read entry can no longer be re-run to its fingerprint once a value it matched is forgotten.
- **Writes about someone already forgotten.** The model wraps them under a destroyed lock. What the store should do is not ruled; x1 met one such write in trace 8.
- **With the layer on each fact (reading 1 back), even the name tag does not keep a name to one partition.** The tag carries the act's layer, and the facts may be elsewhere, so x3 comes back there.
