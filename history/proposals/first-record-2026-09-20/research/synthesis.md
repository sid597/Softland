# Synthesis — what an append-only fact store must fix before its first record

Written by the orchestrator session (Claude Fable 5.1, max effort), 20 September
2026, from seven blind reports, six round-two returns and one round-three
exchange. Research rules nothing. Sid rules. This file says where the evidence
presses on the current leans, which are softland-ff's leans and not Sid's
rulings.

Written early, on Sid's word, because the usage limit was close. What that
cost is listed at the end under "What was not done".

## How to read the marks

- **R✓** reported, and I opened the source myself (list in `loop/verified.md`).
- **R** reported by a worker with a source, script-checked by that worker
  against saved text, not opened by me.
- **I** inferred. I say whose inference: a worker's, or mine.
- **N** institutional: reconstructed from papers or talks about what an
  organisation did.
- Effect on the lean: **stands**, **sharpened**, **challenged** (with the one
  case), or **Sid must choose**.

Reports: `rama-marz.md` (research-1), `facts-datalog.md` (research-2),
`meaning-objects-substrates.md` (research-3), `log-as-truth.md` (research-4),
`sync-versioning-defaults.md` (research-5), `clocks-ids-determinism.md`
(research-6), `frontiers-views.md` and `defaults-skeptics-bigtech.md`
(research 7). Round-two and round-three returns are digested in
`loop/round-2-digest.md`.

---

## 1. The one finding that reorders the table

Camps that disagree with each other arrived, separately, at the same missing
part: **the saying**.

- Hickey, 2016 (R✓): provenance goes "on the transaction (which can have an
  open set of attributes) … substantially more efficient than replicating this
  on many facts (and IMO, correct, as the 'saying' of it *is* the transaction)."
- The log camp (R, research-4): Tango and Kleppmann never let the judge edit
  what it judges. Make the fact the offer, byte for byte. Put what the gate adds
  on the verdict beside it.
- Amazon (N, research 7): the caller mints the id; the stored parameters are the
  check. TigerBeetle, Apple's Record Layer, Temporal (R, research-6): name,
  order and integrity are three fields.
- Rama (R✓): "An exception doesn't mean the append did not go through." So the
  offerer must mint the id, and the verdict must be findable by it.
- The deciding case (I, research-2): one model reply becomes 200 facts. As the
  table stands that is 200 envelopes, ids and verdicts, with no way to land them
  together. "Fact 117 fails; 199 stand as a reply no model gave."

The shape this points at:

- **The offer is the saying.** Written by the offerer, with an offerer-made id,
  kept unchanged. It may hold many facts. It carries what only the offerer
  knows: who, under which grant, the reads it stood on (once, not per fact),
  because of what, the version it expects to replace, and its owner.
- **The verdict sits beside it.** Written by the gate in the same step as the
  compare-and-set. It carries what only the gate knows: yes or no, the stamp,
  the position, the layout epoch, the release of the gate's code, the grammar
  version that admitted each value, the grant's version and the cut it was read
  at.
- **The fact keeps the small core**: entity, key, value, layer, the saying it
  belongs to.

What this does to the table: (1), (6), (7), both halves of (11) and the
emptiness rule of because-of become questions about the saying and the verdict.
Sid's held line "an offer and a fact differ by one slot each way" changes form:
they do not differ; the verdict carries the rest. Kreps's fork (log the requests
or log the results) gets the answer "both, written, neither re-derived".

The ledger records the envelope as Sid's **lean**, not a ruling ("this seems
right as of now, but I cannot say for future"). So this presses a lean, not a
ruling.

---

## 2. The table, hung on the parts of a fact

### Entity — (2) how is an id made

Lean: long, random, minted by the offerer; an ingest lane derives it from source
plus form; no time inside.

- RPL's guidance (R, research-1): client-made, 128 bits at least, never minted
  inside a stream topology, because retries re-mint. Offerer-made ids: "no
  dissent anywhere" (R, research-6).
- Hyrum's Law (R, research 7): every visible trait of an id becomes a promise;
  Twitter's clients sort by id. Supports "no time inside", against RPL's UUIDv7.
- Sameness, not only uniqueness (I, research-2): random ids stop clashes and
  also make the same paper, ingested twice, two entities. Datomic checks
  unique-identity attributes at the gate.
- The ingest case (I, research-4): the ten-million-paper seed dies at paper
  6.2M and is re-run. Derived ids make the re-run idempotent; random ids make
  millions of duplicates or refusals.
- Industry default (N): uncoordinated ids that leak time (UUIDv7, Snowflake);
  the largest systems embed a shard.
- **Effect: stands.** Sharpened on two points: derived ingest ids must be
  deterministic across re-runs; "is this the same thing" needs a gate-side check
  the table does not yet have. Open: research-1 answered v7-against-random in
  its round two; I have not read that passage.

### Key — (3) a word, or an id

Lean: an id; its word and its shape are facts about it.

- Datomic (R, research-2): the attribute slot holds an id; the name is a fact
  (`:db/ident`); renames add names; "Never remove a name"; only the value type
  can never change. Instant chose UUID keys independently. DataScript chose
  words and says so.
- Varda's protobuf tags, Wikidata's P-ids, Edwards (R, research-3):
  "near-unanimous". "A key's shape never changes; new shape, new key."
- Datomic reads all history through today's schema, a documented limit
  (R, research-2). With versioned grammars, something must say which version
  admitted each value.
- Industry default (N): column names and JSON keys as words; migrate in place.
- **Effect: stands.** Sharpened: the verdict names the grammar version that
  admitted the value; a key's shape never changes. Fault line 2 (names against
  ids) closed without an exchange. AT Protocol's NSIDs were the one possible
  dissent; research-5 was stood down before answering.

### Value — (9) never removed, so how is one deleted

Lean: bytes under a per-value key; delete by destroying the key; the fact keeps
the value's hash.

- XTDB version 1 lived this shape: hashes in the log, erasable documents
  beside it (R✓). Replay blocked on evicted documents (issue 184). A
  resurrection race is still open since 2019 (issue 432), because identical
  content is the same id.
- Hickey, 2013 (R, research-2): "while the past may be forgotten, it is
  immutable." Datomic's excision is costly, absent from Cloud, and had a bug
  fixed in October 2025 that left excised data readable in history indexes.
- The EDPB, 2025 (R✓): "it is not advisable to store personal data on the
  blockchain"; clear text, encrypted and hashed forms "should be stored
  off-chain". Differs: the text is about blockchains, not a one-operator log.
  Resembles: neither can erase.
- Cipher ageing (I, research 7): a year-one ciphertext, under a cipher weak by
  year six, sits in every backup by design, and a never-rewritten log cannot be
  re-encrypted. I find this decisive on its own.
- Three camps (I, research-1, -3, -4): the key store is a second thing that is
  not a fact; its backups are the hole; a restore un-deletes. Destruction must
  itself be a fact.
- Four camps (I): a plain hash of a short value can be guessed. Hash the
  ciphertext, or use a commitment with a secret salt.
- "Forget this person" cannot find its keys unless the owner is named at
  admission (I, research-2). Nubank's regret is the same one (R): "one thing
  that we do not add and I would have liked to add, a customer identifier."
- Every Secure Scuttlebutt successor made the value separable from the envelope
  (R, research-5). AT Protocol tore out permanent history over deletion
  (R, research-3).
- Industry default (N): deletion is real; immutability is rented per retention
  period; key destruction is used and regulators have not blessed it.
- **Effect: challenged.** The one case: cipher ageing in a log that is never
  rewritten. The alternative the camps converge on: erasable values live outside
  the log under a random value id; the fact keeps a salted commitment; "value
  absent" is a handled state from record one; tombstones sit in the value store;
  the value is durable before admission; the owner is named at admission; the
  grammar says whether a key's values sit inline or outside. This adds a second
  store, so it touches "one substance". **Sid must choose.**

### By whom — (8) who checks it; is an agent itself

Lean: the door verifies against the logged-in session; an agent is its own
actor; "acts for" is a grant fact before the first write.

- Agent as its own actor is backed by regret (R, research-5): Yjs and Automerge
  recorded replicas, not authors, and are retrofitting authorship in 2026.
  Nostr's replacement delegation has the agent sign *as* the person, "which
  erases the agent from the record". Wikidata lost the instrument, and its bot
  policy with it (R, research-3).
- The deputy case (I, research-3, from Hardy and Miller): a shared layer; A has
  enabled tool T; B, who may only write notes, lands a note naming an entity in
  A's layer; T fires by match and writes B's words there under A's authority.
  Every check in leans (8) and (15) passes. The same case covers a poisoned
  paper and a summarising model.
- The repair (I, research-3; research-6 concurs): the gate never looks for a
  permission; it checks the one the offer cites. Matrix already writes the
  authorising events into each event (R, research-5).
- Revocation (I, research-6): Alice revokes at 900; the gate has read grants to
  850; the agent's offer lands. The verdict must carry the cut at which it read
  the grant.
- A scheduled tool at 3 a.m. has no session (I, research 7).
- By-whom as an opaque actor id whose tie to a human can be cut (I, research-3).
- Rama has no authentication or authorisation at all in the reference
  (R, research-1).
- Industry default (N): "by whom" is a column the application sets, under one
  shared database account.
- **Effect: sharpened, with one addition that is a loss item.** Every offer
  cites the grant it acts under. It cannot be reconstructed later. Fault line 3
  closed with both: the capability camp's slot and Zanzibar's ordered
  evaluation.

### When — (11) whose clock; ever used for order

Lean: the gate's clock only, never order.

- The cheapest first-record item in the loop (I, research 7): two promises on
  the stamp. Never backward within a unit. Later than everything the fact stood
  on. No bytes, no new slot, impossible to add later. "As of T" then names a
  consistent cut that survives a re-partition. Aurora DSQL runs this in
  production (N): adjudicators never stamp backward, heartbeat, and readers wait
  for all.
- A clock read is not replayable (R✓): RPL's recompute recipe "only works if
  your processing is deterministic". The stamp is primary data.
- "Position is the clock" (R, research-4). "The based-on cut is the writer's
  clock" (I, research-3, withdrawing its ask for a writer's clock).
- Industry default (N): wall-clock timestamps, often used for order.
- **Effect: sharpened, not reversed.** The wall clock is still never trusted.
  The stamp is made orderable by rule.

### Layer — (16) who sees a fact; (10) where a layer lives

Lean: base open; personal and session layers private; the gate's answer sits in
its fact's layer; a layer has one home store.

- Entity partitioning puts private facts in the base's partitions and indexes
  (I, research-2).
- A refusal is layered to the session but must be written where the target cell
  lives, in the same step as the compare-and-set (I, research-6; TigerBeetle's
  flaw before 0.16.4, R).
- "What was withheld" leaks that hidden facts exist; record the policy position
  the answer was filtered at (I, research-6).
- Round three, line 1 (I, both sides): single-owner layers become units of
  order; team layers and the base are placed by entity. See "The log itself".
- Rama has no read permissions (R, research-1), which fits the frame: visibility
  is enforced where reads leave the store.
- Industry default (N): a tenant column with row-level security; a database per
  tenant at scale.
- **Effect: stands for visibility; sharpened for placement.**

### Based on — (11) is every read listed; (4) role and follow; (5) the index's lag

Leans: every read listed, no grace period; each read marked depends-on or
how-she-got-here, and follow-latest or stay; write how far the index had got and
what was withheld.

- Captured by the runtime, never authored (R, research-5): Convex records "the
  index range we scanned", so a read covers rows that do not exist yet. Bayou
  dropped literal write-id sets because they "could get large".
- Cost (I, research 7): listing every matched fact costs about 160 times the
  fact, plus 20 KB of vectors. One read-set per firing, with pattern, as-of
  point, role, digest and a complete-or-partial mark, supports all five walks at
  about 64 bytes per read. "Who read the erased thing" over-reports only where a
  pattern tested the erased value. McSherry on full provenance: "Accurate, but
  not yet helpful" (R).
- Role at write time (R, research-5): Pijul 1.0 split "strict dependencies" from
  "known" changes; Automerge keeps `deps` apart from `preds`; Mercurial's
  developers wish their markers had recorded what kind of rewrite happened.
- Follow-or-stay is the wrong question (I, research-2 and research 7): a typo
  fix and a reversal need different answers, and no mark written in advance gets
  both right. The staleness definition, already a fact in the frame, can.
  Depends-on is a counterfactual nobody can know for a model reply.
- "As of" is an opaque token the store mints (six camps: Zanzibar's zookie,
  Meta's FlightTracker ticket, Replicache's cookie, Materialize's since and
  upper, McSherry's reclocking, Feldera's log). "A token can be one number today
  and a vector later." A read is defined by the frontier it ran under; never
  answer from an index past its frontier.
- Internal consistency (R, research 7, from Brandon): a running answer that
  reads two indexes at different frontiers can show a state that never existed.
- Industry default (N): reads are never recorded; causality is a sampled,
  expiring trace id.
- **Effect: (11) sharpened** to one runtime-captured read-set per saying, at
  pattern grain, empty reads included. **(4) split**: keep the role, because the
  actor's intent is known only at the moment; drop follow-or-stay. **(5)
  sharpened**: the as-of is an opaque token naming the serving index's position;
  replace "what was withheld" with the policy position. Fault line 6 closed.

### Because of — always filled

Lean: always filled; empty only at a chain's start.

- Nubank, Lucas Cavalcanti (R): "what I regret the most, is to have implicit
  operations … that don't have origination information."
- "Only a slot can be empty" (I, research-2). On a saying, an absent attribute
  is just absent.
- A saying that spans two layers is two sayings linked by because-of
  (I, research-2).
- Industry default (N): correlation ids, sampled and expiring.
- **Effect: stands** as information. Its home moves to the saying.

### Version — (1) the name of a fact; (6) "replacing 25"

Leans: name a fact by its own id, a hash of the offer's content plus a salt;
keep "replacing 25".

- A log position is not a name (R✓ in part): it survives a migration, not a
  re-partition, a restore or a trim.
- Do not make a content hash the name (R, research-5): SSB froze one JavaScript
  engine's JSON printing; Git's move off SHA-1 is in its ninth year; Dolt's
  format migration "changes all of the commit hashes"; did:plc's legacy ids
  "will unfortunately be around forever". Fusing name and order broke
  FoundationDB's versionstamps when data moved clusters (R, research-6).
- The deciding case (I, research-6): agents in three languages canonicalize one
  plain map two ways; one offer gets two ids; a retry lands twice. That breaks
  the retry safety which is the reason for offerer-made ids.
- Facts have no ids; sayings do (I, research-2 and research-3).
- (6): state the expected version as the predecessor's id and digest, and
  "replacing 25" comes free (I, research-6). McSherry on upserts (R, research 7).
- Industry default (N): an auto-increment and a row-version integer.
- **Effect: (1) sharpened against its content-hash half.** Three names for three
  jobs: a random offerer-made name for the saying; the gate's number for order
  within the cell, which is Sid's hold and it stands; an algorithm-tagged digest
  carried beside the name in pointers. **(6) stands** and comes free once the
  offer is kept unchanged. Fault line 5 closed; research-3's hash-with-nonce was
  the last dissent and the canonicalization case answers it.

### Beside each fact — (7) the gate's yes or no

Lean: kept for ever, beside the fact; refusals in the offerer's session layer.

- Needed for correctness, not only audit (R✓): an append that throws may have
  landed, so a retry must find its earlier verdict by offer id. TigerBeetle added
  this later; before, a failed transfer "could succeed if retried when the
  underlying state changes" (R, research-6).
- Write verdicts, never re-derive them, "because the judge's code changes"
  (R, research-4: Tango writes, Hyder re-derives). Sid's gate lives in a runtime
  rebuilt hundreds of times.
- Volume (I): thirty agents on one cell make about 29 refusals per fact
  (research-4); thousands a second per person on a hot cell (research 7). DSQL
  keeps refusals out of its log (N).
- A trim rule that rewrites nothing (I, research-4): keep every refusal's
  envelope and verdict for ever; let the refused value's key expire after the
  retry window.
- Nubank lost its pre-write requests and regrets it (R). XTDB 2 keeps refusals.
- Industry default (N): a refusal is an error returned and a log line that
  expires.
- **Effect: stands, as a necessity.** Sharpened: written at the target's unit in
  the same step; carries the release, the grammar version, the grant and its
  cut. Whether refused *values* are kept for ever: **Sid must choose.**

### Start of a session — (12) runtime version, machine kind, rebuilds

Lean: who, runtime version, kind of machine; every rebuild a fact.

- Delos (R, research-4): code-version skew was "the only source of
  inconsistency in production". TigerBeetle writes the release on every log
  entry and never replays under different code (R, research-6).
- Feldera (R, research 7): exact re-derivation depends on CPU architecture,
  compiler settings and a logged "now". Temporal and Restate (R, research-6):
  re-derivation holds only for a named code-and-build pair.
- Kay (R✓): the 1978 image came back because its interpreter travelled with it.
  A version name points outside the store.
- **Effect: stands, sharpened.** Stamp the release on every verdict and every
  crossing, not only at session start. Crossings carry a digest; crossings to a
  model carry the content.

### The hand — (14) which motions become facts

Lean: point, select, mode, pan/zoom at a tick; looking apart from shown; a
person may turn capture down or off.

- Webstrates tried durable-by-default for cursors, 30 to 50 operations a
  second, and walked it back (R, research-3). 86M ticks a day for 300 people on
  one problem (I, research-1). Nubank: the fact store "doesn't work that well
  for fire hose writes" (R). Hickey: "Datomic is not about keeping stuff" (R).
- Only "shown" is knowable; "looking" is not (I, research-3). Record "shown"
  when what is shown changes, not when time passes. Give hand and crossing facts
  the session as their entity (I, research-4).
- Against all of this stands Sid's own sharpening (ledger, line 755): "we cannot
  predict this without … storing these signals somehow", and "the fixed part
  never drops a crossing silently".
- Industry default (N): product analytics, sampled, in a separate pipeline with
  a retention window.
- **Effect: Sid must choose.** Three camps reject tick-driven facts kept for
  ever. None says "do not capture". The middle: capture under a stated promise,
  envelope kept, value key allowed to expire, and the promise itself a fact.

### A click — (15) which tools may act in a person's name

Lean: only tools the person enabled; tools in their own layers on by default.

- The deputy case above passes this lean as written.
- Wrong question (I, research-3): ask what was handed over, for what, until
  when. A click is one grant fact whose "covers" is the selection. A tool with
  no grant may give running answers only.
- Both men in the HN thread name this problem (R✓). Hickey: "I don't trust you
  enough to run it." Kay: "what assumptions need to be made on the receiving end
  to guarantee the safety of a transmitted meaning?"
- Industry default (N): scopes granted once, ambient afterwards.
- **Effect: challenged.** The one case is the deputy. Not covered by anyone in
  this loop: the current writing on prompt injection in agents, which is this
  case.

### The log itself — (0) never rewritten; (10) order; (13) storage

**(0).** Lean: never rewritten, as Sid stated.

- None of Marz, Rama or Kafka holds it (R✓ for Rama): "Depot migrations never
  change the offsets of records." Positions and information stay; representation
  moves.
- It is four promises (I, research-4): never lost; never different under one id;
  never re-ordered; provable to strangers. Helland allows absence and forbids
  difference. Certificate Transparency prices the fourth: one bit flip retired
  the Yeti2022 log (R).
- "A promise about logic, not bytes" (R, research-5): Fossil has shunning; Dolt
  re-encoded every commit and checked the logical rows were unchanged; Irmin
  wrote that unbounded growth "is not an issue" and retrofitted garbage
  collection in 2022. The promise that survives: never silently changed; every
  correction is a new record.
- Define it over a canonical form, not Rama's bytes (I, research-4): "a dead
  serializer in year twelve".
- **Which log?** (I, research-1.) In Rama clients append to depots and only
  PState writes are exactly-once; re-publishing from a topology "currently do[es]
  not have exactly-once semantics" (R✓). The table never asks which of the two is
  the permanent record.
- Playback (R, research-4): "The achilles' heel of shared log systems, however,
  is playback." XTDB 1 users saw reindexing take days to weeks; version 2 made
  the log ephemeral (R, research-2). Nothing should ever need a full playback.
- **Effect: Sid must choose which promises.** My reading (I): "the map must not
  lie" needs the first three plus "never silently changed". It does not need
  byte permanence. The fourth can be kept open cheaply by fixing a canonical
  form and a digest now. Sid's own words ground append-only in "rama's append
  only log" for fine-grained versioning (vision log, line 249); I found no
  stated reason for never-rewrite, by keyword search only.

**(10).** Lean: partition by entity; "as of" is a position per partition, never
one number.

- The task count is fixed today (R✓, with a nuance the summaries dropped:
  "Currently Rama does not support changing the number of tasks … adding support
  for this is high priority"). After a re-partition every recorded vector points
  at a layout that no longer exists (I, research-1 and research 7). Positions
  need a layout epoch from day one; XTDB's epochs are the same idea (R).
- Two gates (R, research-1): stream is milliseconds, answers the offerer, atomic
  in one partition, at-least-once. Microbatch is exactly-once and atomic across
  partitions with a global tick, but 300 ms or more, silent to the offerer, and
  halted for everyone by one stalled task group. "No optimism" makes that
  latency felt in the hand.
- Do not rest safety on one writer (R, research-6, Kingsbury on Datomic in
  failover: "not a single-writer system, but a multi-writer one!"). Safety is the
  compare-and-set in storage, plus an epoch the log can check. Orleans says the
  same (R, research 7).
- Round three, line 1. research-1 conceded a layer can be a unit of order in
  Rama (custom depot partitioner, R✓) and that Marz places by owner. research-2
  conceded the base cannot be one order. Both accepted a hybrid I put to them:
  single-owner layers placed by layer, team layers and base by entity,
  "classify by writers, not visibility". Before the first record: the placement
  class readable from the offer; one saying, one unit; the unit's own count as
  position, plus layout epoch; a named cut for the base; an owner on every
  saying. Open risk: a team layer's keying is fixed at birth. The hybrid is my
  own, so weigh it with that in mind.
- **Effect: sharpened on both halves.** Fault line 1 converged.

**(13).** Lean: plain maps; never trimmed; backups inside the delete plan.

- Plain maps stand. The real question is "exactly which bytes are hashed"
  (I, research 7).
- "One model, several storage promises" (I, research-1): Marz's garbage
  collection and Kafka's retention windows exist for ticks, refusals and
  fire-only reads.
- **Effect: maps stand; "never trimmed" is Sid's to choose**, with the hand.

### Before anyone — (17) the first facts

Lean: base and gate are ordinary ids; a finite seed written once by the floor;
ids computed from content.

- The seed grows in editions (R, research-2). A first fact that names itself
  cannot hash itself, so first ids come from words.
- Content-derived seed ids make the byte encoding a forever-promise
  (I, research 7): a year-four rebuild changes key order and seed ids stop
  re-deriving, silently.
- A seed-derived gate id collides at federation (I, research-3 and research-6).
  Split the seed: a universal half, and a store-local genesis.
- **Effect: challenged on "computed from content".** Publish seed ids as fixed
  constants. Make this store's gate and base store-local.

---

## 3. The strongest challenges above the table, ranked by force

1. **There is no unit of saying.** Hickey (R✓), the log camp, Amazon,
   TigerBeetle, Rama's retry rule. Case: 200 facts from one reply. Presses the
   nine-part envelope and layer 0's "one kind of record".
2. **Fix the envelope's extension rule, not its nine parts.** Kay (R✓): TCP/IP
   worked because it "doesn't try to define structures on the actual messages,
   but only minimal ones on the 'envelopes'". Hickey (R): "If you have places,
   you have to have something in the place." research-2's test: a slot belongs
   only if every fact always has it and it is never empty; five of nine pass.
   CT's empty signed slot, Delos's map of headers, Marz: "add optional fields,
   never reuse an id". The frame defers "migration when the envelope changes" to
   "the first one"; in a never-rewritten log that is a first-record question.
3. **Ambient authority.** Hardy, Miller (research-3); Matrix lives the repair;
   research-6 concurs. "Match, don't route" plus policy keyed on who is a
   confused deputy at planet scale, and early facts that lack "under what
   authority" can never get it.
4. **"Re-derivable" holds only for one build, and Sid's past never ends.**
   Temporal, Restate, TigerBeetle, Feldera, Delos. Other systems detect drift
   because old outputs are recorded and the past eventually leaves retention.
   Running answers leave nothing to compare. Choose: keep every build runnable,
   prove sameness at each rebuild, or let crossings carry a digest or content.
   Presses "running answers never stored" and "only the runtime is not a fact".
5. **The map can lie with no stale read in it.** Brandon, via research 7:
   reads at different frontiers compose into a state that never existed. By the
   frame's own rule this must be prevented (one cut per answer) or painted.
6. **Not everything deserves the same promise.** Marz, Kafka, Nubank, Hickey,
   Webstrates, against Sid's "we cannot predict what will matter".
7. **Every second store is a second substance.** A key store or an outside
   value store. Presses "only the runtime is not".
8. **One store for the planet.** Hickey chose a closed world, "avoiding the
   challenges of universal naming, open-world, shared semantics"; his camp runs
   many databases (Nubank: more than 3000) and cannot count its customers without
   an extract. Rama's reference says nothing about a worldwide cluster. Bayou
   built a commit-only view and applications "never select the commit-only
   option" (R, research-5); Boodman calls local response "just a matter of
   physics". Sid's position keeps the door open: "pursuing optimistic updates
   only if the preferred path proves infeasible". A person's own gate near the
   person softens this (I, mine). Promotion into the base stays slow.
9. **Rows plus an audit log.** Stonebraker, Pavlo; AWS retired QLDB and points
   to Postgres audit tables; Amazon's engineers left Dynamo for simpler services.
   "Show the win." My answer (I): the walks over based-on, tools as facts, and
   editing from inside by the same moves are what rows cannot give. It is a fair
   bar for the first workpiece.
10. **Playback.** Nobody recomputes from the log at scale; Marz himself moved to
    incremental views.

---

## 4. The Kay / Hickey exchange: my reading

Full text: `sources/hn-11945722-kay-hickey.md`. My blind reading, written before
any report arrived: `orchestrator-notes.md`, section 1.

- They are not arguing about the same thing. Hickey defends data as the record.
  Kay attacks data as the message to a stranger.
- Their **agreement** is Sid's question. Hickey: "What constitutes minimal
  sufficiency of 'data' … in- or out-of-band, per datom or dataset, how to handle
  provenance." Kay: "what is the smallest thing that could be universal …
  minimal [structures] on the 'envelopes' … does not force a single theory."
- The frame already answers Hickey's sharpest line. "'date-of-birth' is data and
  'age' (unless temporally-qualified, 'as-of') is not" is the split between a
  fact and a running answer, and "what was shown, with its reads" is the as-of.
- The frame answers Kay halfway. Every interpreter is a fact at a version. What
  is left is the envelope, the matching rule, the gate's procedure and the leaf
  set.
- Trust joins the meaning axis to the authority axis, at the tool.
- research-3, independently: both put meaning at the reader; Kay never answered
  Hickey's strongest comment; the loose ends are rows (8) and (15); "a
  never-rewritten fact is a message to a reader not yet there"; one axis of four
  (meaning, order, authority, id-making); both would shrink the envelope. I
  agree, and add that in its agreement the exchange sits under the whole
  first-record question.
- softland-ff's from-memory reading holds. It missed Hickey's
  fact-against-derivation line, his trust line, his "per datom or dataset" fork,
  and Kay's envelope test.

---

## 5. Who mattered, who is missing, what could not be sourced

**Mattered most**: Hickey; the Rama reference; Råberg and XTDB; Nubank's
engineers; Hardy and Miller; McSherry, Feldera, Brandon; TigerBeetle, Temporal,
Restate; the Secure Scuttlebutt successors, Git, Dolt, Matrix; Bayou; the EDPB;
Aurora DSQL. Each report has its own section on who was dropped and why.

**Missing, in my view**: Red Planet Labs asked directly about one cluster
worldwide (research-1's round two lists the questions). Builders of transparency
logs beyond Certificate Transparency, if Sid wants the fourth promise. The
current work on prompt injection and agent authority. Anyone on consent and the
capture of attention. The database provenance literature (why- and
where-provenance).

**Could not be sourced**: Goebel's ETH thesis and one ACM Queue article; the
EDPB's July 2026 version and the AWS QLDB notice; the wording of Kleppmann's book
and Young's 2016 talk; anything from Rama on a worldwide cluster or on
federation. research-5's Appendix A has its own list.

**What was not done**, because the usage limit was close: research-5's round
two; round-three exchanges beyond line 1 (two were folded into round two, three
had converged); my own opening of sources beyond the eleven in
`loop/verified.md`; a reading of any report in full. I read each report's short
version and the sections that bore on a lean.

---

## 6. What is Sid's to choose

1. **The saying.** Add it, with the verdict beside it, and shrink the envelope
   to an always-present core plus an extension rule. Or keep nine parts on every
   fact.
2. **What never-rewrite is for.** Which of the four promises.
3. **Where erasable values live.** In the log as ciphertext, or outside it.
4. **What deserves "for ever".** The hand, refused values, fire-only reads.
5. **Whether an offer cites its grant.** Nobody argued against it.
6. **The unit of order.** The hybrid both camps accepted, or entity everywhere.
7. **No optimism, against physics.** What to fix now so both doors stay open:
   offerer-made ids, offers kept as first-class things, a mark that can be
   carried through a query.

Items that cost almost nothing at record one and cannot be added later: the two
promises on `when`; the owner on every saying; the grant cited by every offer;
the read's role; the release on every verdict and crossing; the layout epoch on
every position; "value absent" as a handled state; an algorithm tag on every
digest.

---

## 7. Addendum — research-5's round two (arrived after the synthesis)

research-5 wrote its round two after being stood down; Sid passed me its
summary. The text is the last section of `sync-versioning-defaults.md`, "Round
two: the leans, pressed". I have not read it; what follows is from its summary
to Sid. Two points are new; three reinforce.

- **New: lean (1) breaks lean (9).** If the fact's name is a hash of the offer
  plus a salt, and the salt stays on the fact, an erased yes-or-no answer can be
  recovered: hash each guess and compare with the name. If the salt is thrown
  away instead, nobody can ever verify the name. Its position: a plain random
  name, a tagged digest beside it over the envelope, and a keyed hash of the
  value inside that digest. This is a sixth camp against hash-as-name, with the
  sharpest case yet. Effect on (1): unchanged in direction, stronger in force.
- **New: lean (2), ingest ids derived from the source.** The derivation rule
  freezes what counts as one entity. "Are arXiv v1, arXiv v3 and the DOI one
  paper or three?" A registry cell written under compare-and-set enforces "one
  source, one entity" and can be corrected later by a new fact. This answers the
  gap I marked under Entity (sameness, not only uniqueness) in the frame's own
  material: the mapping from a source's name to an entity is a fact, not a
  formula. Effect on (2): the random, offerer-made half stands; the
  derived-ingest half is **challenged**.
- Reinforces (9) against (0): Matrix, Bamboo and Pijul all remove bytes; none
  deletes by discarding a key. "If values are kept beside the log, (9) stops
  being now-or-never."
- Reinforces (17): Kleppmann rejected content-computed ids for Automerge ("a very
  fragile API"). Published constants give every store the same seed ids.
- Reinforces (14), (7), (13): pointer samples at every tick come to about 21.6
  billion envelopes a year for three hundred people; with thirty agents on one
  cell, refusals outnumber admitted facts about twenty-nine to one.

Its raw sources and gatherers' notes are in `sources/research-5/` (68 MB, not
committed). `loop/camps-in-their-own-words.md` holds every camp's summary from
every round, verbatim, with none of my reading in it, for a session that should
form its own.
