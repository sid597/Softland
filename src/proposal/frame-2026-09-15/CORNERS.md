# First record: the corners. Eight research voices applied, 21 September 2026

Written by Claude Fable 5.1 at max effort, in Sid's session of 21 September 2026, from the chat.
This is the one document passed on from the first-record research. A new session reads this and
nothing else from that work. Every part carries its status. **Nothing here is a ruling of Sid's
unless it says so.**

## 0. Where the work stands

- **Sid's question, in his words:** "i am deciding what my store must fix before its first record.
  the store only appends and never rewrites, so whatever a fact does not carry when it is made is
  gone for every earlier fact. what i want first: the conventions that would paint me in a corner."
- **Sid's own text is not copied here.** The problem, the forced questions and working answers, one
  fact part by part, and the open questions with handles (0) to (17): he hands these to a session
  himself. Section 12 has a paraphrase of the handles, for orientation only.
- **What was done.** Seven research sessions wrote eight files, in three rounds, about 310k tokens.
  An earlier session read file one whole and drew a ledger of eight corners from it (one voice).
  This session ran the other seven files against that ledger with Codex as a line-range finder,
  read the result corner by corner, and wrote this.
- **What Sid has ruled** (his prompt of 21 September): one store for the whole world; an entity is
  an id, for life; values have shapes, and the descriptions of shapes are facts with versions; a
  version on every fact. "The rest i agreed to as it reads now. All of it is open to question, the
  problem included." Also his: "That a fact carries exactly these nine parts is only a lean of
  mine." And: "leave the economy, it is out of scope as of now."
- **The scale he holds:** hundreds to thousands of agents per person, writing at machine speed;
  tens of thousands of people and their agents on one problem; a field of about ten million papers
  with layers of summaries; the planet; years of running.
- **Status of this document.** One session's synthesis of eight voices. The redrawn ledger in
  section 9 is a candidate. Sid has read the chat version and asked for it to be written down; he
  has not ruled on any line of it.
- **Where the evidence lives.** The research files, the pilot, the first handoff, and this
  session's Codex plans and reading bundles were retired to
  `history/proposals/first-record-2026-09-20/` on 21 September 2026, at Sid's word: "I will not
  tell any session to read them." They are evidence, not reading. Open one only to check a quote.
  The 363 MB of pages and papers the research sessions fetched are not in the repository; they sit
  in a local depot, `/mnt/data/projects/research/softland/first-record-2026-09-20/sources/`.
- **Reading fence Sid set for this work:** never `research/loop/` or `research/sources/`; never
  `src/app/server/env.clj`. This session read no code, no `docs/`, no `vision/LOG.md`.

**What I would put to Sid first.** Whether the unit of admission is one fact or a *saying* (section
2, finding 1). The name, the order, the envelope, the verdict and the reads all hang from it. The
datalog file is right that "(10) and the saying have to be decided together" (datalog L1015).

### How to read the anchors

An anchor like (rama L772) is a line number in a research file. Every anchored claim was read by
this session in the text itself. The files carry their own marks: R or REPORTED (someone wrote or
said it), I or INFERRED (the research author's reconstruction), N or INSTITUTIONAL, and in the rama
file CHECKED (checked by that session against Rama's docs). **This session verified none of their
sources.** "Mine:" marks this session's own derivation. "Lean (n)" is Sid's tentative position on
handle (n) as the research orchestrator listed it for round two; this session knows the leans only
through the files' paraphrase (section 13).

| alias | file, under `history/proposals/first-record-2026-09-20/research/` | camp |
|---|---|---|
| clocks | `clocks-ids-determinism.md` (file one) | TigerBeetle, Temporal, Zanzibar, FoundationDB, Spanner lineage, Jepsen |
| skeptics | `defaults-skeptics-bigtech.md` | the ordinary default, the skeptics, big-tech operations |
| datalog | `facts-datalog.md` | Hickey and Datomic, Nubank, XTDB |
| frontiers | `frontiers-views.md` | McSherry, Brandon, Alvaro and Hellerstein, DBSP, Noria |
| log | `log-as-truth.md` | Helland, Kleppmann, Balakrishnan, Certificate Transparency, event sourcing |
| meaning | `meaning-objects-substrates.md` | Kay and Hickey, capability systems, Wikidata, Webstrates, Unison |
| rama | `rama-marz.md` | Rama's reference, Nathan Marz, Jay Kreps |
| sync | `sync-versioning-defaults.md` | local-first and sync engines, Matrix, AT Protocol, version control |

### Words used here

- **Offer, fact, gate, layer, base, grammar, running answer:** Sid's words, from his text.
- **Saying:** the research's word for one act of offering. It may carry several facts that land
  together. It has one id and one verdict. (Datomic's transaction.)
- **Verdict:** the gate's yes or no, as a record of its own.
- **Door:** where the outside enters. It authenticates the head of by-whom.
- **Floor:** the research files' word for the fixed side: the runtime, the gate and the door.
- **Cell:** (entity, key, layer). The slot that compare-and-set guards.
- **Ordered unit:** the scope inside which the store keeps one order. In Rama, a partition.
- **Cut:** a consistent "as of" across ordered units. A *named* cut is a fact that holds the
  positions.
- **Crossing:** the moment a running answer goes to a person, a model or the host, written as a
  fact ("what was shown").
- **Seed:** the first facts.
- **Pressed zones:** in a research file, round two and three (where the leans are pressed), the
  short version, "above the table", and the file's own list of what cannot be added later.

## 1. The picture

Status: given to Sid in chat on 21 September at his request ("see if you have the picture from
planet to forest to tree level… each 'to' in there is an architecture"). His next message asked
for this write-up. He did not correct the picture and did not confirm it in words.

- **Planet: the problem.** A place changed from the inside, tools included. Every piece is fixed
  (changes only by a rebuild from outside) or live. The problem is where that line goes and what
  the fixed side must guarantee so everything live stays coherent. Coherent is Sid's six lines.
  The scale ladder is part of the problem: it turns every "always" and "every" into a rate that
  must be paid. Four things hold throughout: the map must not lie, no optimism, tools matched and
  never named in the store, same moves for people and agents.
- **Forest: what the problem forces.** Eight forced questions with working answers that interlock
  into one loop: a fact lands, the tools it matches fire, they read, they return a running answer
  or an offer, the one gate checks shape, expected version and policy, it writes or refuses and its
  verdict is a fact, a fact lands. Around the loop: reads leave entries; layers in one store; the
  runtime is the one non-fact; the outside enters through the door; the log only appends.
- **Tree: one fact.** Nine parts, six things around it, handles (0) to (17), one test: gone for
  good, or can a later fact put it right?
- **Planet to forest is the architecture of the place.** The questions are forced; the answers are
  chosen. That choice can fail by itself: a forest can be sound and leave a planet line with no
  mechanism (the earlier session found "affordable" has none), or deliver a line at a rate the
  scale makes unpayable. Its test is Sid's: the rate at which real use forces a new built-in step.
- **Forest to tree is the architecture of the record.** Every forest mechanism needs a handhold on
  the fact: matching holds the key; the gate's checks hold the key's grammar, the version, by-whom
  and layer; honesty holds by-whom, when and based-on; the chain holds because-of. A mechanism that
  finds no handhold on early facts can never get one. Its test is also Sid's: gone for good, or
  fixable.
- **Each "to" is a part, not only a derivation.** The correspondence between levels has to be
  built, held and checked, and it is where things break. The reading confirmed this: nearly every
  place a camp disagreed with the ledger was a forest decision showing up as a part of the tree
  (section 2). So each corner below is judged by walking both "to"s, up to the mechanism it serves
  and the line that mechanism protects, down to its cost on the scale ladder. Camps are not counted.
- **The same "to" inside the place** (the earlier session's reading; not checked against the vision
  documents): a summary over papers is a forest over trees; what makes them views of one place is
  based-on, with stale and doubt climbing through it. So the inside "to" rides on the most
  expensive mechanism in the design.
- **One thing this session holds that the first handoff did not:** no handle asks whether the unit
  that lands is one fact or several. After round three that handle is missing.

## 2. What the reading changed: six findings the corners hang from

1. **The unit that lands is the saying, not the fact.** Five camps, independently. Datalog calls
   it their strongest change: "Make the unit of admission a set of facts said together, with one
   home, one position, one verdict and an open set of provenance on it. It takes the envelope from
   nine parts to five… It costs partition-by-entity" (L1015). In Sid's terms: one model reply
   becomes 200 facts; fact 117 fails its grammar; "199 facts of a reply stand in the store with a
   hole in them… the map would show a reply that no model gave" (datalog L825).
2. **Three authors, three records.** The offerer writes the saying. The gate writes the verdict.
   The door vouches for the head of by-whom. "The judge never edits what it judges… let the fact
   be the offer, byte for byte, and put when, version, epoch and what-was-checked on the verdict
   beside it" (log L679).
3. **The log is a log of envelopes. Values live beside it.** This removes a collision between
   leans (0) and (9) that file one's ledger did not see (sync L3446).
4. **"As of" is the name of a cut, and the cut is a fact.** The per-partition vector exists. It is
   written once, on the cut, never on a fact (datalog L900; frontiers L522).
5. **A personal layer is a small store.** Both round-three exchanges land on placement by writers.
   Sync draws the consequence: "the second store stops being the rare later case. Its conventions
   are needed at record one" (L3660).
6. **Authority is cited, never searched for.** Three camps reach the cited grant from three sides:
   file one's Zanzibar lineage, the capability camp, and Matrix.

Mine, on why the saying keeps returning: it is the unit at which authorship is uniform. What the
offerer knows is true of the whole act. What the gate knows is true of the whole judgment. Copying
both onto every fact is what made nine parts look necessary. This is Sid's fixed/live line applied
inside the record: a tiny core that machinery handles without understanding it, and provenance as
a vocabulary of seed keys that grows the way everything else grows.

## 3. The premise and the eight corners

### P0. The premise: never rewritten. Handle (0)

**Where the voices stand.** All seven restate (0) as a logical promise; none accepts the physical
reading. Log splits it in four: never lost, never a different answer under the same id, never
re-ordered, provable to strangers. "Lean (0) needs promises 2 and 3… Neither needs promise 4 now"
(L647). Helland allows absence and forbids difference: "it will never return data other than the
original contents." Sync: "It is the chaining that made deletion and migration impossible for SSB,
Automerge and Git, not immutability as such" (L3146). Rama: "Rama does not promise 'never
rewritten'. It promises that nothing is lost and that positions never move… 'Never rewrite' is
Sid's rule to hold, not Rama's" (L35).

**What the ledger lacked.**
1. *Which log.* In Rama's natural shape clients append offers and the facts are PState rows. Then
   "lean (0) protects the thing that is not the facts" (rama L530), and the permanent depot holds
   every malformed offer and every re-send. The other shape: the gate publishes what it decided to
   a facts depot. That append is at-least-once, so "every consumer must dedupe by fact id" (L42),
   and "'no optimism' needs one sentence saying which of the two is 'landed'" (L560). "The shape to
   avoid is (a) with a clock-reading gate. There the never-rewritten log cannot rebuild the facts"
   (L568).
2. *Never trimmed has a lived failure.* XTDB v1 held exactly this position and left it after a
   reindex of about 600 GB "taking days to weeks"; the camp would accept never-trimmed "only if no
   operation ever needs a full replay" (datalog L996). Log's T5 says what that needs from record
   one: "make every future scan an envelope scan. Store envelopes apart from value bytes from day
   one… Under lean (0) this layout cannot be changed later" (L707); every derived thing names the
   cut it reflects (L708); each rebuild fact says which keys' interpretation it changes (L709).
3. *One model, several storage promises.* Hand ticks, refusals and reads that only made a tool
   fire are "the highest-volume, lowest-value records" sent into a never-trimmed log with full
   provenance (rama L716). Sync: never-trimmed "is the lean most likely to be broken by year
   three" (L3635), and "plan the trim boundary before the first record" (L2159).
4. *Knowing.* "How do you *know*, this week, that nothing was lost? Append-only does not answer
   that. Checksums in each record and scheduled re-reading do" (skeptics L291). And decide "what
   the gate's yes means physically (acknowledged after how many durable copies)" (sync L561).

**Position (mine).** P0 stands as file one had it, a promise about admitted content, and gets four
sharpenings. Define it over a canonical logical form specified outside any runtime: in year twelve
the serializer is dead, and "if the promise covers Rama's stored bytes, re-encoding is forbidden
forever" (log L649). Say which record it covers: what the gate decided; intake is trimmable once
verdicts are durable. Make the promise per kind, declared in the key's grammar at birth and never
weakened, so a pointer into a windowed kind is known to be allowed to dangle. Allow absence, never
difference (C5). One honest limit from the rama file: "every one of these people trusted the
operator" (L477). If never-rewrite must one day convince strangers, the cheap insurance is log's:
"anchor a hash of the partition heads somewhere outside, early and cheaply" (L667).

### C1. The name. Handles (2), (1)

**Agreed by all.** The offerer makes the name before the first attempt; the gate's number is never
the name. Rama's case, new to the ledger: after a restore, depot offsets are reused and so are the
gate's counters; "a fact that a screen showed yesterday as version 37 may be a different fact after
a restore" (L536). Nubank's replays "change every position" (datalog L812). Log: "None of them
could add the missing one later" (L30).

**What is named.** Datalog, meaning, log and sync: the saying. "Facts have no ids. Entities have
ids and sayings have numbers" (datalog L617). "Reserve the form 'saying id plus index' now" (log
L681). Sync names three things: the cell (a mutable slot), the version in it, and the offer, which
"exists before the gate and may be refused… A refusal never gets a gate number, so if refusals are
kept, offers need their own id" (L57).

**Random, or computed from content.** The one live dispute with lean (1). For hash-as-name: log,
meaning, frontiers. Against: sync, rama, skeptics. Sync decides it. A hash name buys two things
nothing else does: a bare reference that certifies itself, and convergence between stores. "With a
salt there is no convergence, so the hash buys only a commitment of name to content. A digest
beside a random name gives the same" (L3400). And a salt kept on the fact leaks a short value after
erasure; a salt thrown away means "the hashing bought nothing" (L3402). So lean (1) and file one's
random name do the same job. The difference is that the first freezes a byte encoding into
identity: "Every team that did so froze a byte encoding forever, or paid to change it" (SSB, Tezos,
did:plc, Dolt, Git's ninth year off SHA-1; L66). Rama's test: "who will ever re-verify, and from
which bytes? If nobody, use a random id. If somebody, the canonical bytes are the thing to freeze"
(L707). Retry is served equally by a random id (L578). The gate must still refuse the same id with
different content (log L677), which needs the digest kept with the verdict.

**Ingest.** File one split it: the offer id derived from lane, source key and source version, so a
re-run is a recognised retry; the entity id random. Log backs the first half with a case: the seed
"dies at paper 6,200,000 and is re-run from the start" (L675). Skeptics press the second: a second
institution ingests the same ten million papers and, with derived ids, both agree with no
coordination (L423). Sync's registry cell answers both: "the entity id stays random. The lane
writes a *registry cell* whose entity id is derived from the source identifier and whose value is
the entity. Compare-and-set on that cell gives exactly-once. A wrong merge or split is repaired by
a new fact. The same device enforces any uniqueness rule" (L3557). By Sid's own test a derived
entity id fails: "the derivation rule fixes entity grain forever" (log L683); arXiv v1, v3 and the
DOI, "one entity or three?" And sameness must be sayable: "An id for life cannot be merged later.
So 'same as' has to be a key from the first day" (rama L657); "Under no circumstances should
redirects be deleted or repurposed" (Wikidata, via meaning L3062). A derived id is also a guessable
id: "Fine for a DOI. A leak for a person."

**Time in the id.** The rama file withdrew disk locality as RPL's reason; the stated reason is sort
order, and "nothing in the leans needs v7. Lean (2) stands" (L590). Cost of random in Rama: one
more index for time-ordered scans. Unknown: RocksDB write work under random top-level keys, "NOT IN
THE REFERENCE" (L592). If indexes lead with the layer, "random ids cost nothing" (datalog L955).

**Position (mine).** File one's C1 stands with four changes. The name belongs to the saying; a fact
is saying plus index. The id is opaque and carries a scheme tag (sync L3557), so width and scheme
can change later. Beside it sits a tagged digest over the canonical envelope with a keyed
value-commitment inside, kept on the saying and on the verdict, "so that a flipped bit is a repair
and not a Yeti2022" (log L662). Any reference that crosses a store carries name plus digest (sync
L3383). Ingest: random entity id, registry cell, same-as in the seed. One consequence for the gate:
an offer may cite an offer not yet admitted, and such a record "must wait, never be dropped" (sync
L3345); a based-on can name an offer that was later refused (frontiers L562).

### C2. As-of. Handles (10), (5)

**Where the voices stand.** Lean (10), "a cut, never one number", is rejected by datalog,
frontiers and skeptics, sharpened by rama, backed by log and meaning. Everyone gives the same case:
"the store grows from 64 to 256 partitions in year two. Every recorded as-of is a vector over a
partitioning that no longer exists. A scalar survives untouched" (frontiers L556). The dispute
dissolves once the cut has a name: "The camp would say *always one number*, by making the cut a
thing with a name. An index build is a fact: *build 8812 covers these positions in these units*…
The vector is written once, with the build. It is never written on a fact" (datalog L900).
Frontiers' road A is the same thing: a separate slow minter, cuts that form a chain, a read names
the cut in eight bytes, "gates never wait for the minter" (L522).

**What file one's opaque token lacked.** "What the token means must be a fact, or be computable
from facts. A token whose meaning lives only in the running runtime dies at the first rebuild, and
every as-of recorded before that becomes unreadable" (frontiers L528). That answers the earlier
session's own pushback that the token is a new fixed thing. What cannot be added later is
comparability: "If early reads name points that cannot be compared, no later rule can tell whether
two of them were the same moment" (L516).

**Position (mine).** As-of is the name of a cut. The cut fact holds a position per ordered unit,
each position being unit, epoch and count (C3), and the view: "the basis is positions plus view"
(datalog L982). Named cuts chain, so they compare; across stores the honest answer stays
"incomparable". Road A's price is that "an agent cannot stand on its own last write until the next
cut"; so a read names the cut plus its own unit's count. The second road stays open only if the two
promises on `when` are kept from record one (section 4). Withheld things are never listed (four
camps agree with file one); record the policy version. Sync adds that a reader must tell three
states apart: "never existed; erased, with a pointer to the erasure fact; exists but withheld from
you" (L3441). Every answer states the point it is complete through, and its status: "ready,
refreshing, pending" (frontiers L513).

### C3. Order. Handle (10)

**The round-three verdict** (both exchanges read whole). The same hybrid: "The test has to be
writers, not visibility: a layer with one owner is placed by layer; a layer with many writers, and
the base, by entity" (rama L764). Against by-layer everywhere: the base and any team layer would be
one Rama task, one thread, and "Rama cannot split a task" (L755). Against by-entity everywhere: the
saying cannot land; every pattern read is a cut; and a private fact "sits in the same partition,
and the same index, as the base's facts about E", so privacy rests on every read path filtering for
ever, across hundreds of rebuilds (datalog L976). The read that decides between them: "the stack
read of one entity, or the pattern read over one layer" (rama L757).

**What corners here is the promise, not the placement.** "Promise order per cell. Deliver
per-entity co-location as the default placement. Keep the right to spread a hot entity by cell. If
tools come to rely on a whole entity committing as one, that right is gone" (rama L600). And: "a
single-owner layer has one order while it has one home. Never promise whole-layer order for a layer
with many writers" (L774).

**What must exist at record one.**
1. The placement class, readable from the offer, because the partitioner runs on the client and
   sees only the record; that "sits badly with a fully opaque layer id" (rama L772). Mine: a slot
   the gate verifies against the layer's class fact, never inside the id.
2. Positions as the unit's own count with an epoch, never a task offset (datalog L1083). "No stored
   pointer depends on a partition position" (log L597).
3. The owner on every saying. Nubank's regret: "if every transaction had an identifier that could
   point to the actual customer that owns that data, things like splitting databases for sharding
   would have been much, much easier" (datalog L45).
4. Grants live in the layer they govern: "a gate checks only grants that live in layers it orders
   itself" (sync L3534). This closes file one's open case, an offer admitted under a grant already
   revoked in another partition. For the base, policy is small and rarely written, so it is
   broadcast to every task; the copies are "stale by design", and the verdict names the versions it
   checked (rama L606).
5. One saying, one layer: "An act that writes two layers is two sayings, the second because of the
   first" (datalog L814).

**A constraint the datalog file's base plan misses.** In Rama "a PState is written by one topology
only, and a topology is either stream or microbatch. So one fact store has one kind of gate" (rama
L750). Under a stream gate a saying over many entities in the base cannot be atomic. It can be
countable: "readers can count a saying's facts only once its verdict says whole" (L775). Mine: this
fits Sid's flow. The 200 facts land whole in the agent's own layer; promotion into the base goes
entity by entity.

**Two smaller points.** Give hand and crossing facts "the session as their entity", so that one
person's gestures have a mutual order (log L693). And hot cells can often be declared away: "let
the grammar say when a key is a growing set, which needs no version check and produces no
refusals" (frontiers L559). What must share a unit "is decided by invariants across cells", and
uniqueness invariants become registry cells (sync L3597).

### C4. The key. Handles (3), (17)

**Where the voices stand.** Unanimous; "the one where the camp is most settled" (datalog L623).
Sync adds AT Protocol's regrets with words as keys: fields "can not be renamed", and a "V2" suffix
is "a version number smuggled into a name" (L3494).

**What the ledger lacked.**
1. *Seed ids are constants, not derived.* Lean (17) is rejected by sync: "year three, a typo in the
   seed grammar is fixed, or the canonical encoding gets a second version. New stores now derive
   different seed ids from old stores… Federation is broken at the root" (L3571). Rama agrees:
   constants need "no agreement on hashing at all" (L582). The first fact cannot hash itself, "the
   attribute that names things is named by itself" (datalog L960), so a constant may be *chosen* as
   the hash of its namespaced word. It is a constant after that, never re-derived.
2. *The seed grows by editions.* Datomic's base schema grew seven years in. "Say *editions*: each
   one a saying by the floor, each safe to apply twice, none changing an earlier one" (datalog
   L957). Split the seed: kinds and grammars are universal; "the store's own id and its gate's
   actor id must be random per store" (log L717).
3. *Growth only under one key.* "A grammar version that requires more, or provides less, is a new
   key… A rename is a second word on the same key" (datalog L919). This answers rama's question of
   whether every reader must understand every grammar version for ever: inside a key, versions only
   accrete. The case is Datomic's own: after a cardinality change, the past is "shown through
   today's rule, and it shows one affiliation where there were three" (L906). So each fact is read
   under the grammar version its verdict names.
4. *The value's encoding belongs to the floor, not the grammar.* "One self-describing plain-data
   encoding, for ever. A grammar is a predicate over plain data. It never decides the bytes… If a
   grammar can decide the encoding, then every decoder must be kept alive across hundreds of
   rebuilds of the runtime, and one lost decoder loses every value written under it" (datalog
   L916). Mine: a genuine new corner.
5. Tool signatures hold key ids, and the word lookup is a recorded read; otherwise re-pointing the
   word "relation" changes every standing pattern "with nothing landing on any tool" (frontiers
   L563). With ids, two labs can each coin "supports" and nothing collides; "that collision is
   information", so let the gate check a word is unique within a scope (skeptics L424).
6. "An export carries the definitions of every key it uses" (sync L3494).

Mine: the saying shrinks a recursion C4 would otherwise have. In `{from f1, to c1}` the words
"from" and "to" are pinned inside a value for ever; the meaning file asks for ids on fields inside
values (L3107). If several facts can land together, a relation is several small facts whose fields
are keys, and most of that problem goes.

### C5. Erasure. Handles (9), (1)

**This is where the ledger moves most.** File one named two roads and leaned to the first: encrypt
in the log, destroy the key. Five voices press it.
- Sync: "Nobody in my camp deletes by discarding a key. All of them remove bytes… year-one
  ciphertext sits in a log that is never rewritten (lean 0) and in every backup. In year twelve the
  cipher, or a key-handling bug, fails. The log cannot be re-encrypted, because it cannot be
  rewritten. Leans (0) and (9) collide" (L3446).
- Skeptics, quoting the EDPB: it is "not advisable to register personal data" on an immutable
  record as "clear text, encrypted or hashed data". Lean (9) "keeps two of those three forms on the
  record" (L388).
- Log: "the law does not consider deleting the encryption key equal to actually deleting the data
  itself… one regulator, somewhere on the planet, in some year, orders the ciphertext itself
  removed. Under strict never-rewritten Sid breaks the promise for everyone" (L651).
- Rama: "the thing that must go is not in the value slot. A token inside an external anchor in
  based-on. A private name inside the word of a key. A person's id in the by-whom of a refusal"
  (L655).
- Both rama and log: lean (9) makes a key store, a second thing not made of facts, mutable, "the
  most sensitive state in the system" (rama L717).

**Position (mine): switch the default road.** The log holds the envelope, a random value id and a
hiding commitment. Values live beside the log, in a store that may be re-encrypted and erased.
Encryption per value becomes defence in depth, not the deletion mechanism. "Ask what the log is a
log *of*. Envelopes and value ids. Then nothing in the log ever needs deleting" (sync L3647), and
"keeping values beside the log (T3) is exactly what makes (9) *not* now-or-never" (L3548). Every
SSB successor made this one change; Aljoscha Meyer "now calls his own design an 'append-or-delete
log'" (sync L75).

**The price is real.** "The dual write returns" (skeptics L440). XTDB v1 lived this road and says
what must hold from record one (skeptics L394–398; datalog L845–878): "value absent" is a state
every index and tool handles; tombstones, so that "missing never means unknown"; value ids random,
never from content, because "the resurrection race comes from content addressing"; the value
durable before the offer is admitted. Keep a rare recorded excision as the legal fallback, "the
slot stays, the content goes, positions do not move", each use a fact (rama L655; log L651).
Base-layer values stay inline: "promotion to base is a deliberate act of publication" (skeptics
L392).

**Marks that must exist at admission.** "A person asks to be forgotten after three years. There are
40,000 values about them, in the base and in other people's layers, written by hundreds of agents…
Nothing can tell you later unless it was marked at admission… wrap each value's key under a key for
the subject or owner, named when the fact is admitted. Then forgetting a person is one key"
(datalog L871). Mine: subject and owner are two marks; a fact in Bob's layer about Alice has owner
Bob and subject Alice.

**What has to work besides.** Every erasure is a fact, because "a key-store restore un-deletes"
(log L653; rama L663); mine: a value-store restore does too, and the erasure facts are what get
replayed. "A checker for forgetting": Datomic found excised data left in history indexes for years
(datalog L609). The reverse index for "who read the erased thing" must exist "before the first
erasure, or the first erasure is a full scan" (log L711). The honest reach of an erasure is the
forward walk along based-on: a private note quoted by a model, 200 facts after it; "an argument
*for* recording reads… 'delete means throw the key away' is where erasure starts, not where it
ends" (datalog L873). Nothing personal outside the value slot; by-whom stays an opaque actor id and
what is erased is the fact that binds it to a person (log L657, as file one had it). Even "an
entity and key alone" can identify, so "erasure may have to reach a cell's visibility, not only a
value" (sync L3457). Shredding never reaches "plaintext that already crossed to a screen, a model,
or a second store" (meaning L3013).

### C6. Replaces. Handle (6)

**Where the voices stand.** Stands everywhere, and "at no cost" in Rama, since "the offer already
carries 'expects 25', for good, in a depot" (rama L691).

**New reasons.** *Incremental views:* "Supersession by 'same cell, later version' is an upsert, and
McSherry's critique of upserts applies" (frontiers L417). With an explicit replaces, a running
answer can take a change as minus-old, plus-new, with no lookup. Mine: this is the affordability
line reaching into the fact. *Rebuild:* "With 'replaces' on each fact it is one pass. Without it
the gate's logic must be run again, by a runtime that may be 300 rebuilds newer" (datalog L986).
*Across stores:* "order is not shared and the explicit pointer is the only thing that survives"
(rama L468).

**Form.** By name. Not the gate's number, which a restore reuses. Not a content hash: AT Protocol's
deletion broke "through the links, not the content", and its back-pointer became "intentionally not
a strong reference" (meaning L2987). The position may ride along as a hint. Reserve a list of
parents (log L685).

**Two additions belong here.**
1. *The kind of change.* "'Replacing 25' is where the kind of change belongs" (frontiers L532).
   Without it no definition of stale can tell a typo fix from a reversal. Mercurial's developers
   "wish their supersession markers had recorded *what kind* of rewrite happened" (sync L95). It
   cannot be recovered for early supersessions.
2. *A representation of "no longer so".* "A person un-says a relation because it was wrong. What is
   written? If the answer is *a later version with an empty value*, then every grammar must admit
   empty for ever… If it is a part of the fact, it is uniform and needs no grammar to agree. Either
   can work. It has to be chosen before the first record" (datalog L1016). Mine: make "retracted" a
   form of the value slot, beside clear, reference and absent.

### C7. The envelope. Handles (13), (11)

**Here this session parts from file one's ledger**, which said versioned and closed.
- Datalog: "Put Sid's nine through that test. Entity, key, value, version and layer pass. By-whom
  and when are always present but are the same for everything said in one act. Based-on and
  because-of are sometimes empty… 'Forever' then has to hold for five things, not nine" (L596).
- Log: "fixing the parts is the wrong thing to fix… What they would fix forever instead: that the
  envelope is a map of named parts; that it has a version; that there is one extension slot and it
  is inside the hash; that a reader ignores what it does not know; and that a changed meaning gets
  a new name" (L548). CT's one opaque extension slot sat empty for eleven years and then carried
  the 2024 redesign: "The extension costs just 8 bytes" (L40).
- Meaning, Kay's test: "a part is core only if the things that handle a record *without
  understanding it* need it to do their job" (L2921). Varda: "required is forever"; "Absent and
  empty are different things" (L2936).
- Rama: "Marz fixes the evolution rule, not the field list" (L455).
- Sync warns the other way: "optional parts become mandatory", and Nostr's open tags gave "the
  exponential state space of arbitrary combinations" (L3096). Its advice: "a version marker, a rule
  for unknown parts, and very few versions, because every reader must read all of them forever."

**The deciding case: the tenth part.** "*The planet, with an economy*: every act will one day need
a price, a payer, a budget. *A second institution's store*: every saying that crosses will need to
say where it came from and who vouches for it… With nine places, a tenth means a new envelope
version, a runtime that reads both for ever, and every earlier fact silently lacking the part. With
a vocabulary, it is one new key in the next edition of the first facts" (datalog L942). Sync counts
what the leans already add (an offer id, replaces, a verdict beside, roles on reads, a value id, a
session) and concludes "nine is already not the number" (L3670).

**Position (mine): the two sides reconcile by who is acting.** A small positional core on each
fact: entity, key, value slot, saying plus index, replaces. Everything else is a part of the
saying, tagged by a key id from the seed: layer, owner, by whom, the grant, based on, because of,
claimed when, expected versions, session. The gate refuses any part whose key it cannot resolve,
which keeps file one's protection against smuggled meaning and against a regime inferred from
absence. Copiers and readers carry and ignore what they do not know, and the digest covers every
part, so "a dropped part breaks the saying's id" (meaning L2936). "Required" is a policy fact,
never a byte rule: in year eight a second institution's facts arrive with no because-of; "Refuse
them all, and there is no federation. Invent a value, and the map lies. 'Unknown' has to be
sayable, and only bytes that do not demand the part can say it" (meaning L2944). "No grace period"
stands as that first policy; it is "the most strongly sourced lean on the table" (datalog L972;
Nubank: "what I regret the most, is to have implicit operations that are not stored").

**Also.** "Starts a chain" is said outright; four voices agree, and sync's form costs nothing:
"pointing a chain's start at the session fact" (L3582). Absent because-of then means unknown: the
cause never came from the second store, sits in a layer this reader may not see, or was erased
(datalog L974). The offer is never edited and the gate's stamps go on the verdict, "fact and
verdict should be one atomic append to one partition" (log L738). The door checks the envelope
before the append: "a buggy agent host appends a hundred million malformed offers overnight" (rama
L701). And the question under plain-maps-or-classes is "exactly which bytes are hashed?" (skeptics
L436), "whether the logical fact is defined apart from every runtime" (sync L3160).

### C8. By-whom, with the grant beside it. Handles (8), (15)

**Where the voices stand.** File one's C8 stands in every voice: "It is itself, under a grant from
the person. Record both" (meaning L2673). Nostr's replacement delegation "has the agent sign *as*
the person, which erases the agent from the record"; Yjs and Automerge "recorded replicas, not
authors, and are both retrofitting authorship in 2026" (sync L116).

**The cited grant moves up beside it.** File one sorted it as an early loss. Three camps reach it
independently: file one (every offer names the grant it invokes; "the gate checks that grant and
does not search"), the capability camp, whose strongest challenge it is, and Matrix, which "writes
the policy events that authorised an event into that event." Meaning: tools matched with nobody
choosing them, plus policy keyed on who the actor is, "is ambient authority… Hardy's confused
deputy at planet scale. It cannot be patched later, because 'under what authority was this written'
is not on the early facts" (L57). And the grant chain "ends at the layer's root grant, made with
the layer, which cites itself" (L2813). Mine: that makes it a structure of the first facts, which
is why it belongs with the corners.

**The convention** (meaning T1, L2813–2878). A grant is an ordinary fact: the grantee (for a tool,
its pinned version), what it covers, for which chain of work or standing, until when, and whether
narrower sub-grants are allowed. Grants name their grantee; they are not bearer tokens, "because
ids get copied into every based-on list": identity for blame, capability for authority. The click
"is one fact by the person: a new grant", and "the selection is not a separate fact. It is G's
'covers'." "A name inside a value is only a name. A tool may act on it only under a grant the
triggering saying itself cited." "A grant to 'T, latest' hands A's authority to whoever can
supersede T." Agents write narrower sub-grants by ordinary offers, or "the record loses the
instrument"; "revoking a parent kills the subtree." "A tool with no grant may give running answers,
which need no authority. Only offers need a grant" (L3023); so "on by default" holds only for tools
the person wrote or granted (datalog L994 and skeptics L428 agree).

**It is also an affordability mechanism.** "The gate cannot check personal grants cheaply under
lean (10)" (rama L615); a cited grant that lives in the layer's own ordered unit is one local read.
Under one gate "which grant was current?… becomes a lookup at a position, and a grant revoked
before admission is an ordinary refusal", the question Matrix pays state resolution for (sync
L3527). "Everything done under a revoked grant is one walk" (file one).

**Three first-record items the ledger lacked.**
1. *Signing.* "If offers are not signed when made, the authorship of every early fact rests on the
   gate's word for ever, and no later change can fix that" (meaning L2298). Rama agrees it is a
   first-record decision (L110); log: a signature is "the only one that survives a move to a second
   store, but it is heavier" (L413). Mine: reserve the part; have people's keys sign *grants* from
   day one, since grants are rare and are the roots of every authority chain; let machine-rate
   offers ride the door's attestation. This is Sid's to rule. It is a bet on whether the past must
   one day convince people who do not trust the operator.
2. *The door's build and method on every verdict.* "In year four a door bug accepts any by-whom
   from agent hosts for two weeks. Which facts are suspect?" (rama L670). "The door is part of the
   runtime" (L718).
3. *A credential path for actors with no session.* "A scheduled tool fires at 3 a.m. for a person
   who is logged out" (skeptics L425).

## 4. `when`: two promises. Handle (11). Promoted from the early losses

File one sorted the clock as an early loss: a claimed "when" beside the gate's, order never taken
from either. Two camps independently call one addition their strongest change: "keep `when` able to
order. It costs nothing at the first record and cannot be bought later" (skeptics L443).

The two promises: the gate never stamps backward within a partition, and it stamps a fact later
than everything in that fact's based-on. With them, "as of T" can mean everything stamped at or
before T in every unit, and T is ready once every gate has announced past it (Aurora DSQL's way;
frontiers L524, skeptics L412). "What is recorded on road B: nothing new… That is the smallest
possible first-record item. Lean (11), 'never used for order', is the one lean that closes it.
Facts stamped without those promises can only ever be reached by road A" (frontiers L526).

Mine: Sid need not order by `when` today. The promises only keep a one-number as-of possible.
Per-cell order still comes from the replaces chain. Datalog gives an everyday reason for the first
promise: "*what was I looking at yesterday at 17:00* has to map a wall-clock time onto a position.
If a gate's clock steps back after a time sync… the mapping has no answer" (L968).

From Rama: under a stream gate "a retry reads the clock again, so the first stamp must be looked up
by offer id"; record which task stamped it and which module instance (L673). Marz would move the
clock read in front of the append so the gate is a pure function of depot order; that "contradicts
lean (11) unless the door counts as part of the gate" (L577). World-time, when an old thing
happened, is content: "give it one key in the seed, so every lane uses the same one" (meaning
L3098).

## 5. The early losses, after seven voices

Cheap to have from the start. Adopting late loses only the early period.

**The verdict. Handle (7).** One per saying; "landing is all or nothing" (datalog L820). Its place
is the cell's partition: "'Refusals in the offerer's session layer' can be a label. It cannot be a
location… an offer replayed after its commit finds its own version current and refuses itself,
unless the lookup is on the same task" (rama L693). Findable by offer id from anywhere: "an agent
crashes, restarts the next day in a new session, and replays its outbox" (skeptics L426); what a
late retry gets is the original answer. Contents: the grant id and version; the grammar version for
each key used ("not on the fact, which would be 200 copies. Not on the saying, because the offerer
does not know what the gate will check against", datalog L910); the position of every partition
read to decide (meaning L2831; "a policy is withdrawn at T1, and a gate that has seen that
partition only through T0 says yes at T2… Without it, it looks like a gate bug for ever",
frontiers L559); on a no, what was expected and what was found; the gate's build and module
instance, because "a module update lands in the middle of a session" (rama L695) and "a device gate
on an old build admits a fact… that the base's newer gate would refuse" (sync L3674); the door's
build; the store id, which "stops being insurance and becomes necessary" (log L739); the epoch.
After an erasure the verdict's grammar line "is the only thing that still speaks about what the
value was" (datalog L865).

**Refusals are the volume problem.** "Thirty agents of one person keep one 'current summary' cell
fresh. Each landed fact makes about twenty-nine refusals… Session layers become the largest data in
the store" (log L699). CT keeps none. Keep the refusal's envelope and verdict for ever; let the
refused value go after a retry window: "a refused offer never became a fact, so trimming its body
rewrites nothing" (meaning L3079; log L701). Cut refusals at the source with growing-set keys (C3)
and with "an offer that carries its own check and its own fallback" (Bayou; sync L3607). Also
decide "when does a session layer close?" (log L731).

**Reads. Handles (4), (11).** Four camps drop follow-or-stay for reads: "Records pin. Views follow.
Staleness is the difference between the two" (frontiers L433). "Summary S stood on claim C at
version 3. Version 4 fixes a typo. Version 5 reverses the claim… No mark chosen at writing gets
both right" (datalog L1002). Follow-or-stay belongs to value references, in the key's grammar (sync
L3469), and to standing things: tool signatures, definitions, grants; "in a grant the default must
be stay" (meaning L3094). What is written at birth is the read's role: trigger, given to a model,
matched, navigated (frontiers L534). "A deterministic tool depends on all its reads by
construction… A model or a person was exposed to theirs" (sync L3478). "A model summarises forty
papers; one is retracted. If all forty reads are 'depends-on', every retraction turns thousands of
summaries loudly stale… A map that cries wolf also lies… The role sets the loudness. It cannot be
recovered afterwards" (sync L3485). Log adds who vouches for each read: gate, runtime or actor
(L590). A correction to provenance is "a new fact *about the saying*" (datalog L980).

**The read set. Handle (11).** The cost first: "A fact is perhaps 300 bytes. A tool fired by a
pattern that matched 2,000 facts, listing each as id plus version, carries about 48 KB of reads,
160 times the fact. Twenty agents for one person at ten facts a second each comes near 10 MB a
second for that person, never trimmed" (frontiers L540). The cheapest shape that still supports the
five walks (frontiers L544–550): "One read-set fact per firing, not per offer"; each entry a
pattern or one fact id at its version, the as-of point, a role, "and a digest, which is the count
and a hash over the ids and versions that matched"; empty reads included; one honesty slot,
"complete, or partial (the body could read around the floor)", because "a list that claims to be
complete and is not is worse than no list." "The digest is what makes pattern grain enough… about
64 bytes instead of 48 KB." Sync: reads are "captured by the runtime as ranges plus a position,
never authored" (L88). What is lost, honestly: one list per saying "casts doubt on all 200.
Over-doubt is also a way for the map to lie"; the remedy is a dial, "40 claims become 40 sayings,
each with its own reads, all because of the same crossing" (datalog L831). Placement: "who read
what is more sensitive than who wrote what, and belongs in the reader's layer" (sync L3126). The
case for the whole mechanism, from the camp least inclined to give it: "a model summary over 200
papers, and paper 117 is retracted a year later. Which summaries are now in doubt…? Rows plus an
audit log can say that 117 changed. It cannot say the rest, because no common shape links a write
to what it stood on across tools built by different hands" (skeptics L382).

**Crossings.** They carry the cut, a status, the tool version, the runtime version and a digest,
plus the content when the crossing is to a model or the host (file one). Against file one's "never
as rows": for a crossing to a person or a model, list "what crossed, as ids and hashes, not only a
pattern and a cut. A screen or a model's context is finite, so that list is always affordable…
Then 'what was I looking at yesterday' is a fetch by id", because indexes forget fast (log L710).
One bit from day one: an "includes unadmitted input" mark in what a running answer returns and what
a crossing records. Under Sid's no-optimism rule it is always "no". "If the mark is not part of the
contract from record one, the second door cannot be opened later without touching every tool" (sync
L3325). This keeps a door open; it decides nothing.

**The hand. Handle (14).** Every camp rejects ticks as facts kept for ever. "One person, eight
hours, ten ticks a second, is 288,000 facts a day… They are the most revealing facts in the store
about how that person works. And they are already recorded, because every 'what was shown' crossing
carries the viewport" (datalog L992). For 300 people, "about 21.6 billion envelopes a year" (sync
L3620). Shown "is a fact in the viewer's own layer, written **when what is shown changes, not when
time passes**; a store that refuses clocks for order should not use one here" (meaning L3032).
"'Looked' is an inference, so it is a running answer and never a fact" (L3118). Point and select
become part of the saying they led to. Capture of motion is off until a person's own fact turns it
on; "the lean has the default the other way round" (sync L3620). "A store which never rewrites
cannot honour it backwards in time" (datalog L992).

**Session. Handle (12).** One pointer on the saying to the session entity: runtime commit, kind of
machine. "The commit is the name, and a build that is not a clean commit says 'unreproducible' in
its name" (datalog L990). The unit that matters for blame is the gate's epoch and build on each
verdict, not the session.

## 6. New corners no ledger line held

1. **The saying.** Section 2. With one convention: "a saying has one cause, because the gate cannot
   detect a saying that has two" (datalog L834; the risk is a lane that flushes unrelated items as
   one saying for speed; the research author marks it as inferred, not a reported regret). File
   one's batch question now lives here: inside a saying, is each fact checked against the state
   before the saying, or against the facts ahead of it?
2. **The floor's meaning in the seed.** "Year eight. The runtime is rebuilt in another language.
   The definition of 'stale', a fact from year one, now answers differently on the same reads,
   because text ordering or number handling differs slightly. Staleness shifts silently under every
   fact ever written. Nobody can say which build is right, because 'right' only ever meant 'what
   build one did'" (meaning L2975). Their remedy, three things in the seed (L2960): the written
   semantics of the body language in which tool bodies, grammars, grant checks and definitions are
   written; a reference interpreter as a fact ("It may be slow. It is the dictionary, not the
   engine"); conformance cases as facts, so "each rebuild is then a fact that cites its conformance
   run, and 'the runtime is not a fact' shrinks to 'the fast implementation is not a fact; its
   meaning is'." This lifts file one's "small formal model before building" into the seed.
3. **The list of non-facts, short and visible.** It has grown past the runtime: the door, the
   value or key store, the indexes, the encoding and hash rules, the trust anchor, the successor
   pointer (log L737, L544; meaning L3139). "One fact per such part, saying what it is, its
   version, and what it may do."
4. **The bootstrap.** "The gate needs grammar and policy facts in order to admit anything, so the
   very first appends must be admitted by rules that are not yet facts… Module code is the only
   place those first rules can live" (rama L81). If a second store starts with the same first facts
   under the same ids, "joining two stores later is a reclocking problem and not also an identity
   problem" (frontiers L355).
5. **Repair lockout.** "A bad policy fact could stop the gate from admitting the fact that fixes
   it" (sync L3070). The earlier session found this at no level of the picture. Sync names it;
   nobody gives a mechanism. Mine: a break-glass road shaped like excision, rare and recorded.
   Open.
6. **Owner and subject marks, same-as, the retraction form, the floor's one value encoding:** under
   C5, C1, C6, C4.
7. **Valid time.** Datalog says choose: "Datomic: an attribute. XTDB: an axis on every record…
   Not choosing means Datomic's by default" (L611). Meaning and log choose content: one seed key.
8. **Rama's physical corners.** Section 10.

## 7. Above the table

**What the six lines are missing.** The voices confirm what the earlier session traced: nothing at
the planet says what stays a person's. Meaning: together the leans "turn the store into exhaust
plus a watch kept on people, and the map stops being loud about anything… A fact is what someone
stands behind, what conferred authority, or what was shown. The rest is running answers and passing
statements" (L3130). A second absence is exit: "'one store' must not leak into the record. No id
that only this gate can mint. No authorship that only this gate can vouch for. No first facts that
differ from store to store. Then the second store, and leaving, stay possible" (meaning L2621).

**Running answers never stored.** Agreement on the principle: "'date-of-birth' is data and 'age'
(unless temporally-qualified, 'as-of') is not… cache the sources of answers, never the answers"
(datalog L598). Pushback on the word "never" from log, frontiers, rama and skeptics: "Nobody here
recomputes derived answers from the log at scale" (log L44); "derived state is honest if it carries
the position it reflects… A cache hides how old it is; a view that names its position cannot"
(L550). **Log and sync each flag that this touches the project's standing rule on caching and
decline to recommend; this session does the same.** It belongs to the fresh adversarial session
`CLAUDE.md` describes, not here. Log's own second look softens it: "Summaries are offers, so the
largest derived things are already facts, and Rama's indexes are stored. The heel moves to index
rebuilds" (L713). One thing is first-record either way: "*re-derivable* has to mean re-derivable
**now**, by today's runtime. It must never mean that build 400 would reproduce what build 12
computed… A running answer shown in year 1 is only known through its crossing. Recomputing it under
today's runtime is a new answer, not a replay of the old one" (datalog L1017).

**One store for the planet.** Sid's ruling; surfaced, not re-argued. It is every camp's deepest
doubt, and the objections separate. Throughput: answered, "one store is not one order" (log L546).
Distance with no optimism: partly answered by gates near owners; sync's case is "a person in
Bangalore, thirty agents in a Virginia data centre, all writing the person's layer" (L3345).
Operator trust and exit: open. Operations: unknown; "Everything about wide-area behaviour. The
reference never mentions it" (rama L631). The skeptics' test of the whole design: the large wins
are "reads as data, staleness shown as loudly as content, and tools that live in the store…
Everything in the design that is *not* one of those three is where a skeptic would say: use the
boring thing" (L285).

**One writer** means, in every voice, one gate per ordered unit, fenced by an epoch. "The version
check must hold at the point of durability, because during a failover two gates can each believe
they are alone" (skeptics L442). When a layer's gate moves with its person, "a position is (layer,
gate epoch, number), inside the opaque token" (sync L3364). "'One writer' needs an object. One
writer of what?" (rama L719).

**Rules that span many facts.** "A gate that checks one fact's shape cannot protect a rule that
spans many facts. Objects protect such rules by hiding state. Facts cannot hide" (meaning L2641).
Figma's server enforces no-cycles across records, "which the brief's gate has no slot for" (sync
L3056). Mine: the saying gives the gate a unit to check a rule over; across sayings the rule needs
a registry cell or a shared ordered unit.

**Tool bodies and privacy.** "In Rama 'private' is the door's promise, not the store's… If a body
written by anyone but the floor runs inside the cluster… it can read every private layer on its
task. So such bodies run outside the cluster, behind the door, or private layers are not private.
The first-record consequence: layer has to be in the key path of every index" (rama L677). Mine:
bodies made only of built-in steps whose reads the runtime mediates are a third option; arbitrary
code is not.

**A forest question nobody had asked.** "A standing pattern is a query over all facts as of now,
not only over facts that land after it was registered. The question the design skips: what does a
newly registered tool owe to history?" (frontiers L435).

**The premise of the table itself.** Sync half agrees with it: knowledge can be added *beside* old
records later by new records that name them. "What is truly gone is only what no one knew to
capture at the moment… **Must be captured at birth:** the offer's own id; the actor itself;
based-on with roles, positions and viewpoint; because-of; what it replaces; the grounds of the
verdict; the session. **Can be added beside, later:** names, types, digests, corrections to time
and author, labels, translations between grammars. The first pile is where a mistake is permanent.
It is also short" (L3165). Rama splits it the same way: "Representation can be migrated later…
Information that was not captured cannot be recovered by any migration" (L466).

## 8. Carried from file one, unchanged by the seven

File one (`clocks`) was applied by the earlier session; its write-up is retired with the rest.
These points still stand and no other file repeats them.

- **Three outcomes, not two:** ok, refused, unknown. An offerer-made id plus one kept verdict per
  id means "what happened to my offer?" always has a definite answer. That is the mechanism under
  no optimism.
- **The epoch.** A gate's term opens with a fact in each ordered unit, and every verdict carries
  its epoch. A verdict from epoch e after the opening of e+1 is a split-brain trace any checker can
  find. Safety rests on compare-and-set plus the epoch, not on there being one gate (Datomic had
  two live transactors at failover and it did not matter).
- **Re-deriving against rebuilds.** "Worked out again exactly from the same reads" is a claim about
  one tool version on one runtime build. A rebuild moves no fact, so nothing is marked stale by it,
  unless the rebuild is itself a fact that running answers read. Three roads: keep every build
  runnable for ever; promise unchanging behaviour and prove it on each rebuild; or let crossings
  carry a digest, and the content when the crossing is to a model or the host. File one's round two
  takes the third. The earlier session's addition: leaves could be append-only like facts, a fix is
  a new leaf, tools name leaves by id.
- **Nearest-layer-wins is an override rule.** A layer used to hide a fact becomes deny-by-position,
  and "who can see this?" gets hard.
- **Default visibility, handle (16):** inherited at creation, so no fact is ever without a rule.
  Sync adds: "'Base open'… stands if 'open' is a policy fact in the seed and not a property of the
  store" (L3591).
- **Check before building:** a small formal model of the envelope's rules, with invariants such as:
  every offer id has exactly one verdict; no chain forks; no based-on points past its own as-of.
  Run it with two gates and a failover. Test a tiny reference model of the gate against every
  rebuild. (Now section 6, item 2.)
- **Limits of file one:** its cost cases assume tens of agents per person and hundreds of people on
  a problem. Sid holds a thousand to ten thousand times that. It never saw Rama.

## 9. The ledger, redrawn

Status: a candidate from this session. Not a lean of Sid's. Not ruled. "Changed" is against file
one's ledger (section 14).

**Corners**
- **K0 The promise** (changed). Over a canonical logical form; says which log it covers; declared
  per kind and never weakened; absence allowed, difference not.
- **K1 The name** (changed). The saying has a random, opaque, scheme-tagged id made by the offerer.
  A fact is saying plus index. A tagged digest sits beside, over the envelope with a keyed
  value-commitment inside. Entity ids random; registry cells; same-as in the seed. References that
  cross a store carry name plus digest.
- **K2 As-of** (sharpened). The name of a cut; the cut is a fact. A position is unit, epoch,
  count. The view is recorded; the withheld set never. Three kinds of absence can be told apart.
- **K3 Order** (its open part closed). Promise order per cell, and one order for a single-owner
  layer while it has one home. Placement by writers. The class in a slot the gate verifies. An
  owner on every saying. Grants live in the layer they govern. One saying, one layer.
- **K4 The key** (extended). An id. Seed ids are constants; the seed grows by editions; an
  incompatible shape is a new key; the value's encoding belongs to the floor.
- **K5 Erasure** (reversed). Values beside the log; envelopes in it. Random value ids, tombstones,
  "absent" handled everywhere. Subject and owner marked at admission. Every erasure a fact. A rare
  recorded excision.
- **K6 Replaces** (extended). By name. With the kind of change. With a form for "no longer so".
- **K7 The envelope** (changed). A small positional core; a vocabulary of seed keys on the saying;
  a version marker; the gate closed, copiers open; unknown is sayable; a chain's start said
  outright.
- **K8 Authority** (the grant promoted). By-whom is the immediate actor. Every offer cites its
  grant. A root grant is made with each layer. A slot for signatures.
- **K9 `when`** (promoted). Never backward within a unit; later than everything the fact stood on.
- **K10 The floor's meaning in the seed** (new). Written semantics, a reference interpreter,
  conformance cases; the list of non-facts kept short and visible.

**Early losses.** What the verdict carries; the read-set shape and roles; what a crossing carries,
including the unadmitted-input bit; the session pointer; the hand's defaults.

**Can wait,** given K1 to K3: index layout; physical encoding; how many gates; which road mints
cuts.

## 10. Rama: what the reference fixes, per the rama file

Status: every line is that file's claim, marked CHECKED by its author against Rama's docs unless
noted. **This session opened no Rama documentation.** Load the project's `rama` skill and verify
before building on any of these.

1. Rama promises nothing is lost and positions never move. It ships depot migrations (transform or
   excise, offsets unchanged) and trimming (L35).
2. The task count is fixed at launch. Growing means copying every depot into a new module, and log
   positions change (L36). Rama cannot split a task; re-homing one key online is NOT IN THE
   REFERENCE (L755, L760).
3. A log position is not a safe name: it survives a migration, not a re-partition, a restore or a
   trim (L37). After a restore, depot offsets are reused (L536).
4. Ids are made by the offerer before the append; RPL's guidance is client-side, 128 bits minimum,
   never generated inside a stream topology, because of retries (L38). RPL's stated reason for
   UUIDv7 is sort order (L588).
5. An append that throws may still have landed (L39).
6. Two gates. Stream: milliseconds, can hand the verdict back inside the append call, atomic inside
   one partition only, at-least-once. Microbatch: exactly-once, atomic across partitions, a free
   global tick, 300 ms and up, cannot answer the offerer, and stops for everyone if one task group
   stalls or one record throws (L40). A PState is written by one topology only, so one fact store
   has one kind of gate (L750).
7. The microbatch id is a 64-bit counter, the same on every partition, readable in topology code.
   Under a stream gate there is no global number, and the reference shows no way for topology code
   to see depot offsets (L41).
8. A topology writing to a depot is not exactly-once; only PState writes are. Consumers dedupe by
   fact id (L42).
9. Replay rebuilds only what is deterministic. Whatever the gate adds must be stored as primary
   data and copied, never recomputed (L43).
10. Rama records no reads (L44).
11. No authentication, authorization or encryption. Any client with a cluster connection can append
    to any depot and read any PState (L45). A running event has access to all partitions on its
    task (L677).
12. The depot partitioner runs on the appending client and sees only the record (L772). A depot
    accepts any object; a throw goes to the caller, not to the topology (L701).
13. Depot and PState names cannot be renamed in a module update. A module update pauses appends for
    2 to 30 seconds, for the whole module (L247, L626).
14. The leader does all work, reads included; followers only apply. Zones and regions: NOT IN THE
    REFERENCE (L621–623).
15. Small, rarely written data can be broadcast to every task; the copies are stale by design
    (L606).
16. PStates sit in backups until backup GC; online incremental backup needs a paid licence (L663,
    L630). RPL treats about 50 KB as one fetch (L674).

**Unknowns to test or ask RPL** (the rama file lists thirteen questions at L637–650): can topology
code read a record's depot offset and append time; does a microbatch expose and keep the ranges it
consumed (a natural cut minter, frontiers L522); RocksDB write work under random top-level keys;
practical record size for long read lists; exactly-once depot appends ("on our roadmap"); is the
built-in serialization byte-stable across versions; anything wide-area: more than one region, safe
timeouts at 150 to 300 ms, follower reads, region-aware leaders, rolling module updates; licence
terms for one public cluster of thousands of nodes.

## 11. How this was made, and what was not read

- **Method.** The ledger from file one was embedded in one prompt as the yardstick. Codex
  (gpt-6-astra, effort high, read-only sandbox) read each of the seven files whole and returned
  tagged line ranges only: DISAGREES, ABOVE, NEW-CORNER, CARRIED, NEW-CASE, NEW-REASON, OWN-LIST,
  SAME, SKIP. A script copied the ranges by line number into one bundle per corner, so no model
  retyped anything.
- **Result.** 2,974 ranges; every plan tiled its file with zero gaps and zero overlaps. Codex
  marked 81 to 86% of every file as bearing on the ledger, so the yardstick did not make the load
  small. Splitting by zone did: the pressed zones came to 279 KB, about 27%, matching the pilot's
  "about a quarter".
- **Read by this session:** all pressed-zone bundles, whole, all seven voices; the tops of skeptics
  (L1–25) and meaning (L1–69); both round-three sections whole (datalog L1045–1090, rama
  L736–778); nine small body ranges Codex flagged as new corners; the first handoff and the
  write-up of file one.
- **Not read:** the body zone, about 73% of the seven files (team-by-team evidence), except those
  nine ranges; `research/loop/` and `research/sources/`; the orchestrator's own merge
  (`synthesis.md`, `the-camps.md`, `the-fact-and-its-questions.md`, `orchestrator-notes.md`), which
  neither applying session opened; any source behind any quote.
- **Check on Codex:** of six sampled SAME calls, five were fair and one lenient (datalog L952–954
  held a new mechanism, which also surfaced in copied ranges). DISAGREES was applied loosely: it
  often meant "disagrees with the lean, agrees with the ledger". That cost reading time only.
- **Where it all is:** `history/proposals/first-record-2026-09-20/`, with a README. `corners/plans/`
  holds Codex's rows. In `corners/bundles/`, `<corner>.md` is what was read, `<corner>.body.md` is
  the unread body zone for the same corner, `INDEX.md` lists every copied range with Codex's note
  (the instrument for picking from the body zone), and `REPORT.md` has the counts.

## 12. The handles, paraphrased

For orientation only. Sid's own text is the source and he hands it to a session himself.

(0) the log: never rewritten, or only never lost. (1) what a pointer to a fact uses: the gate's
number, or the fact's own id, random or from content. (2) how ids are made; may an id give away
when or where. (3) a key: a word, or an id. (4) read marks: depends-on against how-she-got-here;
follow the latest against stay on the version read. (5) pattern reads: how far the index had got;
what was withheld. (6) "replacing 25" kept on 37. (7) the gate's yes or no: kept for ever, where,
refusals. (8) by-whom: the agent or the person; acts-for. (9) deleting a value, backups included.
(10) order: what shares a partition; as-of one number or a position per part; two gates on one
layer. (11) whose clock; every read listed; because-of always filled. (12) a session-start fact;
every rebuild a fact. (13) plain maps or classes; is the log trimmed; backups. (14) which motions
of the hand become facts. (15) which tools may fire on her click and write in her name. (16) who
can see a fact before any permission exists. (17) the first facts: what, by whom, ordinary ids or
words in code, the same in a second store.

## 13. The leans, as the research files paraphrase them

Second-hand. This session never saw Sid's own list; these are the orchestrator's round-two leans as
quoted or paraphrased inside the files. Sid called them leans, not rulings.

(0) never rewritten; with the rider that this makes (9) and (10) now-or-never. (1) point at a fact
by its own id, a hash of content plus salt. (2) entity id random, 128 bits, made by the offerer, no
time inside; ingest ids derived from source plus form. (3) a key is an id; its word and shape are
facts. (4) two marks on each read, fixed when written: depends-on or how-she-got-here, follow or
stay; "the floor guesses; the tool may correct". (5) record how far the index had got and what was
withheld. (6) keep "replacing 25". (7) the verdict kept for ever, in the fact's layer; refusals in
the offerer's session layer. (8) by-whom is the agent as itself, verified at the door; acts-for
exists before the agent's first write. (9) each value under its own key; delete by destroying the
key; the fact keeps the value's hash; perhaps by-whom may go too. (10) a layer has one home store;
inside a store, partition by entity; as-of is a cut, never one number. (11) the gate's clock only,
never used for order; every read listed; no grace period; because-of empty only at a chain's start.
(12) a session-start fact; every rebuild a fact. (13) plain maps; never trimmed; backups inside the
delete plan. (14) hand motions as facts at a tick; a person may turn capture down by their own
fact; looking captured apart from shown. (15) tools in a person's own layer are on by default. (16)
the base open; visibility inherited. (17) a finite seed written once, its ids computed from content
so every store derives the same ones.

Which of these the voices press: (1), (9), (10), (11) on order, (14), (17), (4), (7), (13), and (0)
as a physical claim. Which stand in every voice: (2) apart from ingest, (3), (5), (6), (8), (12),
and "no grace period".

Where the redrawn ledger touches Sid's rulings. *"A version on every fact":* under the saying, a
fact's version is its saying's position in its unit, "the per-fact 'version' slot disappears"
(datalog L823). The ruling holds in effect; the earlier session had already noted that "version"
does three jobs in Sid's text. Surfaced, not resolved. *"An entity is an id, for life":* holds, and
is the reason same-as must exist from the first day. *"One store":* holds as one logical store, one
gate policy and one id space; from inside, placement by writers gives it "AT Protocol's shape,
personal repositories plus shared indexes" (sync L3660). *"Descriptions of shapes are facts with
versions":* holds; K4 constrains how versions under one key may differ.

## 14. File one's ledger, as it stood

The yardstick this session measured the seven files against. From the first handoff; one voice from
one file; not a lean of Sid's, not ruled.

- **L1 name.** The offerer makes a random 128-bit id for every entity, offer and fact, before the
  first attempt. Never from content, never the gate's number, no "where" or "when" inside. Facts
  point by it. A digest, tagged with its algorithm, may sit beside it.
- **L2 as-of.** An opaque, kind-tagged token minted by the store. A comparison may answer
  "incomparable".
- **L3 order.** The cell (entity, key, layer) sits in one ordered unit. What else shares that unit
  is open: entity or layer. Partition identity stays out of names and out of as-of.
- **L4 key.** A key is an id; its word and grammar are facts. The first key ids are published
  constants. The store id, incarnation and gate actor differ per store.
- **L5 erasure.** From the first erasable value: delete a key, or keep the value outside. Digests
  over ciphertext. The actor id stays; who it is can be erased.
- **L6.** The new fact names the fact it replaces.
- **L7 envelope.** Versioned and closed. No empty or magic value serves as a signal. The logical
  envelope is kept apart from the physical encoding.
- **L8.** By-whom is the immediate actor. Acts-for is separate, scoped and short-lived.
- **E, early losses.** The verdict kept under the offer id, atomic with the compare-and-set, naming
  grammar version, policy position and epoch. Crossings carry a digest, plus content for a model or
  the host. A claimed "when" beside the gate's. The grant invoked, named on every offer. Read marks
  with a third value: shown, dependence unknown. Reads live on the crossing as pattern plus cut,
  never as rows.
- **A, above the table.** Re-deriving holds only for a tool version on a build. A rebuild is a
  fact. Safety rests on the compare-and-set plus an epoch, not on there being one writer.
