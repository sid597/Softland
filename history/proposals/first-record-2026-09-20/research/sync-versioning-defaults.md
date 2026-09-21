# Sync, multiplayer, versioning: what they would fix before the first record

research-5 · 2026-09-20 · for Sid, via the orchestrator session

Cutter: Claude Fable 5.1, max effort. All reconstruction, every INFERRED mark,
and all judgments are this session's. Nine read-only gatherers (Opus) collected
passages only; they made no judgments.

---

## 0. How to read this

**Scope.** Sections one and two of the brief: sync and multiplayer builders
(including the signed append-only logs among strangers), and versioning
builders. Section three (the ordinary industry default, the skeptics, big-tech
lessons) moved to "research 7" by softland-ff's scope change. Nothing had been
gathered for it, so there is no appendix of moved material.

**Marks.** Every claim about what a person or team thinks carries one of:

- **REPORTED** — they wrote or said it. A quote or close paraphrase and a source follow.
- **INFERRED** — I reconstructed it from their system or their stated principles. It is my reading, not their words.
- **INSTITUTIONAL** — taken from a multi-author paper, a spec, or product docs. It shows what the organisation shipped or published, not what one person believes.

**Quotes.** Nothing in quotation marks was written from memory. Each quote was
found by `grep` in a locally downloaded copy of the named source (PDFs through
`pdftotext`). I then re-checked 147 of the most decision-changing quotes myself
against the same raw files. All 147 were found. Seven needed a second look
because markup, tabs, or two-column PDF layout split the phrase; all seven were
then found verbatim (details in Appendix B). Where a quote has "…", the cut is
mine or the gatherer's; the words on both sides are the source's.

**What I did not read.** No code or docs in this repository, by the brief's
instruction, so the reconstruction is not anchored on Sid's current leanings.
That departs from the project rule to start from `docs/carry-on.md`; I followed
the brief because un-anchoring was its stated purpose. I used Sid's system only
as the brief describes it.

**Added voices.** The brief invited "who is missing". I added four groups
because they fit Sid's two added heuristics (lived with never-rewrite for years; wrote down
regrets): Fossil (D. Richard Hipp), Jujutsu and Mercurial's changeset
evolution, the Secure Scuttlebutt successor specs (Bamboo, Gabby Grove, PPPPP,
Willow, Earthstar, p2panda), and Matrix (a short entry, checked by me directly).

**Order.** Section 1 is the short version. Sections 2 and 3 go person by person
in the brief's six-part shape. Section 4 goes question by question, in Sid's
numbering and order. Section 5 is the group view: who matters most, what the
camp would question above the table, which questions it would call wrong.
Section 6 collects what each source implies for a second store.

---

## 1. The short version

Ten findings. Each is argued with sources further down.

1. **Three things need names, and most regrets come from giving them one.** The
   *cell* (entity + key + layer) is a mutable slot. The *version* is an
   immutable thing in that slot. The *offer* exists before the gate and may be
   refused. jj keeps two ids on purpose. AT Protocol separates the slot
   (AT URI) from the pinned version (URI + CID). The did:plc spec calls the
   server's sequence number "an annotation … and not an intrinsic property".
   A refusal never gets a gate number, so if refusals are kept, offers need
   their own id.

2. **Do not make a content hash the name of a fact.** Every team that did so
   froze a byte encoding forever, or paid to change it. SSB froze one
   JavaScript engine's JSON printing. Tezos says its context hash function
   "cannot be changed". did:plc has ids built on a legacy format that "will
   unfortunately be around forever". Dolt's format migration "changes all of
   the commit hashes". Git's move off SHA-1 is in its ninth year. If integrity
   is wanted, carry a tagged digest as an attribute, computed over the envelope
   plus the *hash of the value*. Matrix has done exactly this since 2019.

3. **Make the value separable from the envelope on day one.** This is the one
   thing every SSB successor changed (Bamboo, Gabby Grove, PPPPP, Willow), and
   Pijul and Matrix do the same. It is what makes deletion possible without
   breaking the log. Teams that promised "append-only forever" took it back:
   Aljoscha Meyer now calls his own design an "append-or-delete log".

4. **"Never rewritten" is a promise about logic, not bytes.** Fossil never
   rewrites history and still has "shunning". Dolt re-encoded every commit and
   validated that the logical rows were unchanged. Irmin wrote in 2015 that
   unbounded growth "is not an issue" and had to retrofit garbage collection in
   2022. The promise that survives is: never *silently* changed, and every
   correction is a new record (Fossil, Mercurial, jj).

5. **Reads should be captured by the runtime as ranges plus a position, never
   authored.** Convex records "the index range we scanned", so a read set
   covers rows that do not exist yet, and one algorithm serves both commit
   conflicts and live-query invalidation. Bayou defined the dependency set of
   a read formally in 1994, and then had to drop literal write-id sets because
   they "could get large".

6. **A read's role must be written at write time.** Pijul's 1.0 rewrite split a
   change's links into "strict dependencies" and "merely a set of 'known'
   changes". Automerge keeps `deps` (what I had seen) apart from `preds` (what
   I overwrite) and Kleppmann insists the second is not derivable from the
   first. Mercurial's developers wish their supersession markers had recorded
   *what kind* of rewrite happened.

7. **"As of" should be an opaque position token, not an integer.** Figma's
   LiveGraph assumed one global order, called the assumption "deeply baked",
   and watched one unavailable shard stall every optimistic update. Replicache
   documents the ceiling of a single global version. Linear's one counter for
   all workspaces leaks everyone's write rate. Replicache's cookie is "opaque
   to the client". A token can be one number today and a vector later.

8. **Record how far the index had got.** Bayou "stably records the unique
   identifier of the last Write reflected in the Tuple Store checkpoint".
   Croquet exposes "backlog" and shows an overlay when the model is behind.
   Figma's LiveGraph does not stamp reads and pays by re-fetching
   unconditionally. AT Protocol reports index lag in an HTTP header, outside
   any record, and still debates whether a server is "read-sticky".

9. **By whom: the acting thing itself, plus the grant it acted under.** Yjs and
   Automerge recorded replicas, not authors, and are both retrofitting
   authorship in 2026. Nostr deprecated its on-record delegation tag; its
   replacement has the agent sign *as* the person, which erases the agent from
   the record. PPPPP writes the identity version into every message. Matrix
   writes the policy events that authorised an event into that event.

10. **The camp's strongest challenge is to "no optimism" with one worldwide
    gate.** Bayou built a commit-only view and found that applications "never
    select the commit-only option". Boodman calls local-first response "just a
    matter of physics". Weidner shows strict waiting can starve a client. The
    camp's own resolution is honest tentativeness: show the unconfirmed thing,
    marked, and carry the mark through every query (Bayou's two-bit tags).

---

## 2. Section one: sync and multiplayer

### 2.1 Bayou (Doug Terry, with Theimer, Petersen, Demers, Spreitzer, Hauser, Welch; Xerox PARC, 1994–1998)

**1. What they built and chose.** A replicated database for laptops that
connect rarely. Any server accepts a write at once as *tentative*. One server,
the *primary*, fixes the final order and makes writes *committed*. Every write
carries its own conflict test and its own repair. Sessions carry guarantees.

**2. Their reasons.**

- A write names itself at first acceptance, not at commit: "Each Bayou Write
  also contains a globally unique WriteID assigned by the server that first
  accepted the Write." (Terry et al., "Managing Update Conflicts in Bayou",
  SOSP 1995, https://people.eecs.berkeley.edu/~brewer/cs262b/update-conflicts.pdf)
- The expected state is a query, not a number: "Each Write operation includes a
  dependency check consisting of an application-supplied query and its expected
  result." (same)
- One orderer: "we use a primary commit scheme. That is, one server designated
  as the primary takes responsibility for committing updates… In all other
  respects, the primary behaves exactly like any other server." (same)
- The version slot exists from birth: "The CSN is the most significant factor
  used to determine a write's position in the log; uncommitted or tentative
  writes have a commit sequence number of infinity." (Petersen et al.,
  "Flexible Update Propagation for Weakly Consistent Replication", SOSP 1997,
  https://www.cs.cornell.edu/courses/cs614/2003sp/papers/PST97.pdf)
- No synchronized clocks: "There is no requirement that servers have
  synchronized clocks, which is crucial since trying to ensure clock
  synchronization across portable computers is problematic." (SOSP 1995)
- Determinism must cover limits, not only code: "a merge procedure cannot
  access time-varying or server-specific 'environment' information such as the
  current system clock or server's name. Moreover, merge procedures that fail
  due to exceeding their limits on resource usage must fail deterministically."
  (same)
- Tentative is shown, marked: "Tentative reservations are indicated as such on
  the display (by showing them grayed)." And the mark travels through queries:
  "Each tuple is tagged with a 2-bit characteristic vector identifying the set
  of views that contain it… Our query processor respects and propagates these
  bits, so that in the result of a query each tuple is tagged". (same)
- A session is two sets: "read-set = set of WIDs for the Writes that are
  relevant to session Reads write-set = set of WIDs for those Writes performed
  in the session". The dependency set of a read is defined: "RelevantWrites(S,t,R)
  is a smallest set that is 'enough' to completely determine the result of R."
  The server returns it: "This presumes that the server can compute the
  relevant Writes and return this information along with the Read result."
  (Terry et al., "Session Guarantees for Weakly Consistent Replicated Data",
  PDIS 1994, https://www.cs.utexas.edu/users/dahlin/Classes/GradOS/papers/SessionGuaranteesPDIS.pdf)
- "Acts for" is explicit: "Client applications and Bayou servers operate on
  behalf of users and obtain the key pair and access control certificates from
  the corresponding user at start-up time." Certificates "grant, delegate and
  revoke access". (SOSP 1995)
- A server's birth is a record, and its id is made from its maker's: "A Bayou
  server … creates itself by sending a [creation write] to another server…
  The creation write is handled … just as a write from a client." The new id
  is the pair of the creator's stamp and the creator's id, and they note the
  cost: "if replicas are created linearly, one from the next, server
  identifiers will be increasingly longer". (SOSP 1997)

**3. What they changed or regretted.**

- The unused option: "Interestingly, the Bayou applications that have been
  built to date never select the commit-only option when reading data. This is
  because users always want to see updates that they have made, even if the
  update has not yet been committed." (Terry et al., "The Case for
  Non-transparent Replication: Examples from Bayou", IEEE Data Engineering
  Bulletin, 1998, http://csis.pace.edu/~marchese/CS865/Papers/terry_the-case-for-non.pdf)
- Literal read sets did not scale. The paper lists the "practical problems":
  "The session state, i.e. the set of WIDs maintained for a session, could get
  large. The set of relevant WIDs returned from a Read operation could get
  large." They moved to version vectors. (PDIS 1994)
- "Never rewritten" was conceptual only: "The Write Log conceptually contains
  all Writes ever received by the server… In practice, a server can discard a
  Write from the Write Log once it becomes stable". An "O vector" records "the
  'omitted' prefix of committed Writes". The price of truncation is that a far-
  behind peer needs "a full database transfer". (SOSP 1995; SOSP 1997)
- Refusals sit outside the store: "By convention, most Bayou data collections
  include an error log for unresolvable conflicts. Such conventions, however,
  are outside the domain of the Bayou storage system." (SOSP 1995)
- Two applications, opposite choices on showing tentativeness: the calendar
  showed "tentatively scheduled meetings in a different color"; the mail reader
  "does not distinguish between tentative and committed data". (1998)

**4. Which questions, and what they would say.**

- (1) A write gets its own id where it is first accepted, and gets the
  orderer's number later. The number slot is there from the start, filled with
  infinity. REPORTED. For Sid: an offer has an id before the gate; the version
  is a second, later name. INFERRED.
- (4) The "expected version" generalises to "a query and its expected result".
  The dependency set of a read is "the smallest set … enough to completely
  determine the result", computed by the server, not declared by the client.
  REPORTED.
- (11 reads) Listing every read literally gets too large; they compressed to
  vectors. REPORTED. For Sid: list a pattern read as pattern + position, not as
  the matched facts. INFERRED.
- (5) Yes: the checkpoint records the id of the last write it reflects. REPORTED.
- (0) Conceptually all writes; practically a truncated prefix with a recorded
  omission vector. REPORTED.
- (7) Conflicts that cannot be resolved go to an in-band error log by
  convention, not by the store. REPORTED. They would likely say the same of
  refusals: keep them as data, in the application's own terms. INFERRED.
- (8), (15) Delegation is a signed certificate; software acts "on behalf of"
  the user with the user's credentials. REPORTED. Note this is impersonation:
  the record shows the user, not the program. INFERRED.
- (10) One primary orders; everything else is tentative. REPORTED.
- (11 when) Per-server monotonic stamps; clocks need not agree. REPORTED.
- (12) Determinism needs uniform resource bounds. REPORTED. For Sid: "re-derivable
  from the same reads" is only true relative to a tool version, a runtime
  version, and the limits that runtime enforces. INFERRED.
- (14) and the no-optimism principle: show tentative state, marked; carry the
  mark through queries into results. REPORTED. Nobody chose commit-only. REPORTED.
- (17) A server's existence is a write in the log; the first server is a fixed
  base case. REPORTED.

**5. Resemblance and difference.** Close: a single orderer, offers with an
expected state, a session that knows what it has read, a wish that the display
not lie. Different: Bayou's whole point was disconnected work, so it *had* to
show tentative state. Sid's store is always reachable in principle. Bayou's
log was truncated on purpose; Sid's is the record. Bayou's scale was dozens of
replicas, not a planet.

**6. Who they disagree with.** With replication transparency: "'replication
transparency', while a laudable goal for supporting legacy applications, is not
appropriate for a replicated storage system", and "Many of these systems
started with the goal of replication transparency but gradually ended up
adding hooks for applications to give input to the replication process."
(1998). Read for Sid: they disagree with any design that hides the difference
between confirmed and unconfirmed from the person. They would also disagree
with hiding unconfirmed state entirely.

Could not open: Terry's "Replicated Data Consistency Explained Through
Baseball" (Microsoft serves a block page; three mirrors 404).

### 2.2 Convex (Sujay Jayakar, James Cowling, Jamie Turner; 2021–now)

**1. What they built and chose.** A hosted reactive database. Transactions are
deterministic JavaScript functions. One committer writes an append-only
transaction log. Every query's read set is recorded and used twice: to detect
commit conflicts, and to know when a live query must rerun.

**2. Their reasons.** All from Jayakar, "How Convex Works" (2024),
https://stack.convex.dev/how-convex-works unless noted.

- "The log is the immutable source of truth; the index is derived data that we
  modify over time."
- "After querying the index, we record the index range we scanned in the
  transaction's read set. The read set precisely records all of the data that a
  transaction queried."
- One mechanism, two uses: "we detect whether the query's result would have
  changed using the exact same algorithm the committer uses for detecting
  serializability conflicts: Walk the log after the query's begin timestamp and
  see if any entry overlaps… the sync worker reruns the function and pushes its
  updated return value to the client."
- A running answer is not a log entry: "Queries don't go through the commit
  protocol, since they don't have any writes, but we can use their read sets
  for implementing subscriptions."
- One writer: "The committer in our system is the sole writer to the
  transaction log… The committer starts by first assigning a commit timestamp
  to the transaction that's larger than all previously committed transactions."
- One "as of" for the whole screen: "The sync worker additionally guarantees
  that all queries in the client's query set are at the same timestamp."
- Determinism reaches the scheduler: "determinism also requires that our
  runtime is deterministic. For example, if a query function issues two
  db.get() calls concurrently, we need to ensure that Promise.race returns the
  same result every time".
- The id: "Each ID contains a varint encoded table number, 14 bytes of
  randomness, a 2 byte timestamp, and a two byte version number and checksum.
  The randomness goes before the timestamp so writes are scattered in ID space,
  utilizing all shards' write throughput under range partitioning. The creation
  timestamp has day granularity and lets us efficiently ban ID reuse without
  having to keep deleted IDs around forever."
- Two times on one record: commit timestamps are "Hybrid Logical Clocks of
  nanoseconds since the Unix epoch"; the visible `_creationTime` is separate
  "and has different guarantees compared to the commit timestamp."
- The offer, in their words: "treating the transaction as a declarative
  proposal to write records on the basis of any read record versions (the 'read
  set'). At the end of the transaction, the writes all commit if every version
  in the read set is still the latest version of that record." They add: "This
  is akin to being unable to push your Git repository because you're not at
  HEAD." (Convex docs, "OCC and Atomicity", https://docs.convex.dev/database/advanced/occ)

**3. What they changed or regretted.**

- December 2025, reversing "the id knows its own kind": "Before we can support
  custom IDs, we need to stop relying on our special current ID encoding… It is
  a special format that currently encodes the table it belongs to". The driver:
  custom ids are "useful when migrating data from other databases or
  optimistically generating IDs on clients, for apps that work offline".
  (Convex, "Why ctx.db is changing", 2025-12-10, https://news.convex.dev/db-table-name/)
- June 2023: the `Id` class became plain strings, which "are much easier to
  pass to other services or across JSON-serialized boundaries."
  (https://news.convex.dev/announcing-convex-0-17-0/)
- The schema is not versioned. "The first push after a schema is added or
  modified will validate that all existing documents match the schema. If there
  are documents that fail validation, the push will fail."
  (https://docs.convex.dev/database/schemas) There is one current schema,
  enforced over the whole store.
- Optimism costs double work and shows as flicker: "Developers implement each
  logical mutation… twice: once for the authoritative server change and once
  for an optimistic update to the local store." (Convex, "An Object Sync Engine
  for Local-first Apps", https://stack.convex.dev/object-sync-engine) The
  optimistic-updates docs create "a temporary Id" that "will also be rolled
  back and replaced with the true ID once the server assigns it", and invite
  the reader to insert a mistake: "You should see a flicker".
  (https://docs.convex.dev/client/react/optimistic-updates)
- An admitted limit: "Queries currently fully reexecute". (object-sync-engine)

**4. Which questions, and what they would say.**

- (11 reads), (4) Reads are recorded by the runtime, as index *ranges*, so a
  later insert into the range counts as a change. No one declares reads by
  hand. REPORTED. Every recorded read is a real dependency, because the
  function is deterministic. REPORTED in effect; the generalisation to Sid's
  "role of a read" is INFERRED: for a deterministic tool, all reads are
  dependencies; for a model or a person, none can be proven to be.
- (5) Reads happen at a timestamp; the index is derived and served at that
  timestamp. The position of a read is always known. REPORTED.
- (10) One committer; "as of" is one timestamp; the whole client view sits at
  one timestamp. INSTITUTIONAL.
- (11 when) Two clocks with different guarantees: an ordering clock (HLC, set
  by the committer) and a display time. REPORTED.
- (2) The id deliberately leaks a little: the day, the table number (hidden
  name), and *the version of the id format*. They now regret the table part.
  REPORTED. The format-version field inside the id is the part worth copying.
  INFERRED.
- (1) Client-made ids are wanted for offline work and for import, and the
  server-only id blocked both for years. REPORTED.
- (3) One schema, enforced retroactively. REPORTED. This cannot transfer: a
  store that never rewrites cannot make old facts pass a new grammar.
  INFERRED.
- (12) Determinism is a property of the runtime, down to promise scheduling.
  REPORTED. So a crossing that claims "re-derivable" should name the runtime
  build. INFERRED.
- (9) "ban ID reuse without having to keep deleted IDs around forever":
  deletion is real, and the id's day-stamp exists to make that safe. REPORTED.

**5. Resemblance and difference.** Closest living system to Sid's based-on for
running answers, and to the gate as sole writer. Different: Convex documents
are mutable rows with no provenance kept on them; read sets live in memory for
live queries and are not stored as history. Convex offers optimistic updates;
Sid refuses them. Convex is per-customer, not one store for everyone.

**6. Who they disagree with.** With CRDT libraries on where conflict policy
lives: "conflict resolution may need to be handled at the application layer as
a product decision, not at the framework layer", and CRDTs "often try to
resolve as many conflicts as possible automatically, embedding many product
decisions within the library". (Jayakar, "A Map of Sync",
https://stack.convex.dev/a-map-of-sync) This is Bayou's position, thirty years
on. With loose value grammars: "A fifth of the datatypes in MongoDB's BSON are
deprecated", and a large integer written from another language "will silently
lose precision when read in JavaScript, potentially corrupting the row if
JavaScript writes it back out." (Jayakar, "How Convex Works"; bears on (13)).

### 2.3 Croquet and TeaTime (David A. Smith, David P. Reed, Alan Kay, Andreas Raab; later Vanessa Freudenberg; 2003–now)

**1. What they built and chose.** Replicated deterministic computation. Every
participant runs the same model code. Only events from outside (a person's
input) travel. A reflector stamps and orders them. Everything else is
recomputed identically everywhere. This is Sid's split between running answers
and offers, built as a product.

**2. Their reasons.**

- "Croquet's collaboration architecture is based upon the concept of replicated
  versioned objects coordinated by a universal timebase embedded in the
  communications protocol." And: "I/O events exist in real time, and provide
  the coordination between real time and 'pseudo-time'". (Smith, Kay, Raab,
  Reed, "Croquet — A Collaboration System Architecture", C5 2003,
  https://worrydream.com/refs/Smith_DA_2003_-_Croquet,_A_Collaboration_System_Architecture.pdf)
- The session is bound to the code: "A session id is created from the given
  session `name` and `options`, and a hash of all the registered Model classes
  and Constants. This ensures that only users running the exact same source
  code end up in the same session, which is a prerequisite for perfectly
  synchronized computation." (Croquet client library source, `session.js`,
  github.com/croquet/croquet, read 2026-09-20; JSDoc link markup removed)
- Lag is a value, and it is shown: "Usually `now == externalNow`, but if the
  model has not caught up yet, then `now < externalNow`. We call the difference
  'backlog'. If the backlog is too large, Croquet will put an overlay on the
  scene, and remove it once the model simulation has caught up." (`view.js`,
  same repository)
- The hand stays out of the record: "For events published by a view and
  received by a model, the data needs to be serializable, because it will be
  sent via the reflector to all users. For view-to-view events it can be any
  value or object." (same) And smoothness is a view trick: "we do automatic
  in-betweening in the view by decoupling the rendering position from the model
  position". (croquet/multiblaster-tutorial README, read 2026-09-20)
- Multisynq docs (the current successor,
  https://docs.multisynq.io/tutorials/model-view-synchronizer and the snapshots
  tutorial): "The view can read from the model, but can't write to it
  directly." Snapshots let new users join "without replaying the entire event
  history".

**3. What they changed or regretted.**

- The 2003 design was peer-to-peer with "a coordinated 'distributed two-phase
  commit'". The shipped system replaced it with a stateless cloud reflector.
  INSTITUTIONAL. I found no first-person account of why (the most likely one,
  a David A. Smith post, was blocked).
- Code as identity has a price: "whenever we change the model code, a new
  session is created… The old session state becomes inaccessible, because for
  one we cannot know if the new code will work with the old state, but more
  importantly, every client in the session needs to execute exactly the same
  code to ensure determinism." The remedy they had to add: "we need to use
  Croquet's explicit persistence. An app can call `persistSession()` with some
  JSON data". (multiblaster README) The session object now carries three ids:
  `id`, `persistentId`, `versionId`.

**4. Which questions, and what they would say.**

- (12) The running code version is part of the world's identity. REPORTED.
  For Sid: write the runtime build and the tool-body versions down at session
  start, because "deterministically re-derivable" is only true per version.
  INFERRED. Croquet's regret is the warning on the other side: do not let the
  code version become part of the *data's* identity, or a rebuild orphans the
  data. They needed a hand-written, version-free projection to survive
  rebuilds. For Sid this is already the design (facts outlive runtimes); the
  lesson is for running answers and crossings only. INFERRED.
- (5) How far behind am I is a first-class value and is shown loudly. REPORTED.
- (14) Pointer smoothing and hover are view-to-view and never become records.
  Only what must change the shared model is serialised. REPORTED.
- (11 when) The reflector's time is the only time inside the model; "Never use
  `Date.now()` in models". REPORTED.
- (13) Pure recomputation from the event log is too slow to join; they
  snapshot. REPORTED. I report this without endorsing it for Sid: the project
  treats stored derived state as a red flag to be examined, not adopted.
- (10) One reflector per session is the orderer. REPORTED.

**5. Resemblance and difference.** Same split of "re-derivable" from "not
re-derivable". Same single stamping authority. Different: a Croquet session is
small and short-lived; its event log is not an audit record; identity and
permissions are outside it.

**6. Who they disagree with.** With the state-variable view of shared worlds:
"The standard view of a networked virtual environment implementation describes
the system as a set of state variables that represent instantaneous system
state." (C5 2003). Could not open: Reed's OOPSLA 2005 TeaTime paper (paywall),
his 1978 thesis, croquet.io docs (404; the library's own doc comments carry the
same text).

### 2.4 Figma (Evan Wallace; later the multiplayer and LiveGraph teams; 2016–now)

**1. What they built and chose.** A multiplayer design tool. One server process
per open file is the authority. Conflicts resolve by last writer wins, per
property, per object. Not operational transform, and by their own account not a
true CRDT.

**2. Their reasons.** From Evan Wallace, "How Figma's multiplayer technology
works" (2019), https://madebyevan.com/figma/how-figmas-multiplayer-technology-works/
(his mirror of the Figma blog post).

- "Figma isn't using true CRDTs though. CRDTs are designed for decentralized
  systems where there is no single central authority to decide what the final
  state should be. There is some unavoidable performance and memory overhead
  with doing this."
- "A conflict happens when two clients change the same property on the same
  object, in which case the document will just end up with the last value that
  was sent to the server. This approach is similar to a last-writer-wins
  register in CRDT literature except we don't need a timestamp because the
  server can define the order of events."
- Ids are made by clients: "This can be easily accomplished by assigning every
  client a unique client ID and including that client ID as part of
  newly-created object IDs. That way no two clients will ever generate the same
  object ID. Note that we can't solve this by having the server assign IDs to
  newly-created objects because object creation needs to be able to work
  offline."
- Unconfirmed local changes win on the local screen: "So we want to discard
  incoming changes from the server that conflict with unacknowledged property
  changes."
- The server checks more than shape: "Figma's multiplayer servers reject parent
  property updates that would cause a cycle … Clients can't reject changes from
  the server because the server is the ultimate authority."
- Deletion really deletes: "Figma doesn't store any properties of deleted
  objects on the server. That data is instead stored in the undo buffer of the
  client that performed the delete… This helps keep long-lived documents from
  continuing to grow in size."

**3. What they changed or regretted.**

- *The journal (2024).* The server held a file in memory and wrote a checkpoint
  "every 30 to 60 seconds". The admission: "Since checkpoints were created only
  every ~60 seconds, we could lose up to 60 seconds of work on the server-side
  if multiplayer crashes." They added a write-ahead journal: "Each change is
  assigned a sequence number, which is an incrementing integer associated with
  the file." The new target is still not zero: "(Our goal was <1s of data
  loss.)" To retrofit "every write is logged" they had to funnel all writes
  through one type: "By refactoring the code to isolate ownership of the file
  into an encapsulated type, it was easy to audit all cases where the file gets
  updated". They proved the log rebuilds state by replay: a "byte-by-byte
  identical file blob", and "After ~400K consecutive successful validations, we
  started rolling out." The journal "isn't cross-region replicated"; the
  checkpoints are. (Figma Engineering, "Making multiplayer more reliable",
  2024-04-29, https://www.figma.com/blog/making-multiplayer-more-reliable/)
- *The global order (2024).* "LiveGraph's implementation relied on a globally
  ordered stream of updates, a reasonable assumption to make when there's one
  primary database without replicas… As our lone database splintered into a
  collection of vertical shards, global ordering was no longer guaranteed."
  The assumption was "deeply baked into LiveGraph", so the stopgap was
  "artificially combining all replication streams into one." The cost: "The
  global stream could move forward only if each shard was producing updates.
  This meant that transient blips had outsized effects on users—if one shard
  was unavailable, all optimistic updates stalled (no comments could be
  made!)". (Figma Engineering, "Keeping It 100(x) With Real-time Data At
  Scale", 2024-05-17, https://www.figma.com/blog/livegraph-real-time-data-at-scale/)
- *Recompute instead of maintain (2024).* "most query results never change
  after initial load", so "our cache could be invalidation-based". The older
  machinery had one reason: "The historical motive for a mutation-based cache
  was load". And the price of not knowing a read's position: "If an
  invalidation arrives during an in-progress read, there is no way to tell
  whether the result is from before or after the invalidation. To ensure
  eventual consistency, LiveGraph must re-fetch in either case." (same)
- *A second algorithm for a second kind of value (2025).* For code layers they
  did not reuse last-writer-wins: "we used the Eg-walker algorithm to build the
  multiplayer collaboration service for code layers … The server reconciles
  simultaneous edits from all active clients using Eg-walker." (Burke and Kern,
  "Canvas, meet code", 2025-06-25, https://www.figma.com/blog/building-figmas-code-layers/)

**4. Which questions, and what they would say.**

- (11 when) With one orderer, no timestamp is needed for order. REPORTED.
- (2) Ids are made by the client from a server-issued client id plus a local
  part, because creation cannot wait for the server. REPORTED. For Sid the
  same reason holds for a different cause: not offline work, but anything that
  must point at an offer before the gate answers. INFERRED.
- (10) The ordering unit is the file; one process owns it; routing prevents
  "split brain". REPORTED. A global order, once assumed, gets "deeply baked"
  and couples everyone's availability. REPORTED. So: never let code do
  arithmetic on a global position. INFERRED.
- (5) If reads carry no position, every invalidation during a read forces a
  re-fetch. REPORTED.
- (0) "Never lost" is a number. Theirs went from sixty seconds to under one.
  REPORTED. For Sid: decide what the gate's yes means physically (acknowledged
  after how many durable copies) before the first record. INFERRED.
- (12), (13) The proof that a log is the truth is a replay that gives a
  byte-identical state, run hundreds of thousands of times. The disaster-
  recovery artifact was the snapshot, not the log. REPORTED.
- (7) The gate must sometimes check an invariant across many records (no
  cycles in the tree), not only shape, version and policy of one record.
  REPORTED for Figma. For Sid: the brief's three checks have no home for
  cross-cell invariants. INFERRED.
- (9) Deleting really removes; undo data lives with the deleter. REPORTED. They
  accept lost history to bound growth. INFERRED.
- Above the table: one conflict rule did not fit all kinds of value; text
  needed a causal event graph. REPORTED.

**5. Resemblance and difference.** "One live value per object property" is
Sid's "one live row per entity + key + layer". A single authority orders.
Different: Figma keeps no history of losing values and no provenance; it shows
unconfirmed local changes; its partition is a file that one process can hold in
memory.

**6. Who they disagree with.** With decentralised CRDTs as overhead without
benefit once a server exists. In 2025 the stated objection to CRDTs for text is
memory, not correctness. With their own earlier assumption of one global order.

### 2.5 Linear (Tuomas Artman)

**Source warning.** Linear has published almost no technical prose. "Scaling
the Linear Sync Engine" (2023, https://linear.app/blog/scaling-the-linear-sync-engine)
is a video with one paragraph. The localfirst.fm episode with Artman was
retrieved and is career narrative; the words "optimistic", "transaction" and
"schema" do not occur in it. The detailed record is a third-party
reverse-engineering: Wang Zhu, "Reverse Engineering Linear's Sync Engine",
https://github.com/wzhudev/reverse-linear-sync-engine. Its README quotes Artman
calling it "probably the best documentation that exists - internally or
externally"; I did not open the original of that endorsement. Everything below
is REPORTED by Wang Zhu about Linear, not by Linear.

**1–2. What they built, and the reasons as recorded.**

- A total order, one number: "all transactions sent by clients follow a total
  order, whereas CRDTs typically require only a partial order … When a
  transaction is successfully executed by the server, the global `lastSyncId`
  increments by 1. This ID effectively serves as the version number of the
  database."
- One number for every customer: "unlike a file revision number that typically
  applies to a single file, `lastSyncId` spans the entire database, regardless
  of which workspace the changes occur in… even if a single transaction happens
  in your workspace, the `lastSyncId` often increments significantly".
- The local database holds only confirmed state: "transactions will never
  directly modify the tables in the local database! Instead, they only alter
  in-memory models … only after receiving the corresponding delta packages from
  the server does the local models get updated." The reason: "the local
  database is a subset of the server database (the SSOT), and it cannot contain
  changes that have not been approved by the server. If the server rejects the
  transaction, modifying the model tables prematurely could make it difficult
  and error-prone to revert."
- Ids: "the UUID is generated on the client side".
- Rebase rewrites the base: "The `original` value of each transaction is
  updated to reflect the value from the delta packet".
- Refusals vanish: a rejected transaction will "undo any changes made on the
  client side and be removed from the `executingTransaction` queue."
- Undo appends: "when a transaction executes its undo logic, a new transaction
  is created and added to the `queuedTransactions`."

**3. Changed or regretted.** Nothing written that I could find.

**4. Which questions.** (10) One global number works at Linear's size, and it
leaks: any customer can watch the whole system's write rate in the counter.
REPORTED fact, INFERRED reading. (1) The server's number is the database
version; entity ids are client-made UUIDs. REPORTED. (7) Refusals are not kept.
REPORTED. (6) The base value of a pending change is rewritten on rebase, so the
original "what I thought I was replacing" is lost. REPORTED. No-optimism:
Linear is the nearest product to Sid's rule. The durable local copy never
holds an unconfirmed change; only the in-memory model does. REPORTED.

**5–6. Resemblance, difference, disagreement.** Same single orderer and same
refusal to let unconfirmed state into the durable copy. Different: the screen
still shows the unconfirmed change. Disagrees with CRDTs on partial versus
total order.

### 2.6 Replicache and Zero (Aaron Boodman, Rocicorp)

**1. What they built and chose.** A client-side sync library (Replicache), a
hosted server built on it (Reflect), and their successor (Zero). The model is
server reconciliation from game networking: clients run named mutators
speculatively; the server runs the authoritative version; clients rewind and
replay. Boodman earlier co-built Noms, the content-addressed database that
became Dolt's first storage layer.

**2. Their reasons.**

- Two states, named: "local changes to the space in a client are immediately
  (optimistically) visible … We call these changes speculative, as opposed to
  canonical." ("How Replicache Works", https://doc.replicache.dev/concepts/how-it-works)
- The server may decide differently: "the push endpoint is not necessarily
  expected to compute the same result that the mutator on the client did. This
  is a feature." (same)
- The reconcile: "hidden from the application's view, it rewinds the state of
  the Client View to the last version it got from the server, applies the patch
  … and then replays any pending mutations on top. It then atomically reveals
  this new state to the app". And: "Replicache is modeled under the hood like
  git." (same)
- The offer's identity is a per-client counter: the push carries "the pending
  last mutation id for the client that is pushing. This is the high water mark".
  The position is opaque: "The cookie is a value opaque to the client". A poke
  carries no data: "All the poke does is tell the client that it should pull
  again soon." (same)
- What is recorded is the intent: "Zero then sends a mutation (a record of the
  mutator having run with certain arguments) to your server's push endpoint",
  which ends by "recording the fact that the mutation ran". And: "The result
  from the client mutator is considered speculative and is discarded as soon as
  the result from the server mutator is known." (Zero docs, "Custom Mutators",
  https://zero.rocicorp.dev/docs/custom-mutators, read through web.archive.org
  because the live page would not open)
- The documented menu of version schemes. Global version: "A single global
  version is stored in the database and incremented on each push … While
  simple, the Global Version Strategy does have concurrency limits because all
  pushes server-wide are serialized, and it doesn't support advanced features
  like incremental sync and read authorization as easily as row versioning."
  (https://doc.replicache.dev/strategies/global-version) Row version: "It does
  not require global locks or the concept of spaces. It does not require a soft
  deletes. Entities can be fully deleted. The disadvantage is that it pays for
  this flexibility in increased implementation complexity and read cost."
  (https://doc.replicache.dev/strategies/row-version)
- A record of what was shown, deliberately not durable: "A Client View Record
  (CVR) is a minimal representation of a Client View snapshot", and "The storage
  doesn't need to be durable — if the CVR is lost, the server can just send a
  reset patch." (row-version page)
- Why a gate at all: "you get fine-grained authorization for free …
  Implementing this would be quite difficult with a CRDT, because there is no
  place to put the logic that rejects an unauthorized change." (Rocicorp,
  "Ready Player Two", 2023-10-18, https://rocicorp.dev/blog/ready-player-two)
- The premise Sid denies, in their words: "This is just a matter of physics –
  information can only travel so fast. If you want instantaneously responsive
  UI, this means you can't wait for the server – changes have to happen
  locally, on the client." (same)

**3. What they changed or regretted.**

- Mutation ids moved from per client to per client *group*. The stated cause is
  schema change: "For brief periods during schema migrations, two client groups
  can coexist in the same browser profile." ("How Replicache Works")
- Reflect was retired in 2024 (announced 2024-06-19,
  https://rocicorp.dev/blog/retiring-reflect). Replicache went to maintenance
  while Zero was built. Zero's CRUD mutators were deprecated in favour of named
  custom mutators. INSTITUTIONAL.
- A failing mutation is not kept as data. The documented push handler logs the
  error, aborts, and retries in an "errorMode" that skips the business logic
  but still advances the client's last mutation id, so one bad mutation cannot
  block the queue. (global-version and row-version pages; paraphrase from
  source)
- I found no written retrospective by Boodman on Noms.

**4. Which questions, and what they would say.**

- (1) An offer has its own identity: client group id plus a counter. It exists
  to make retries safe and to tell the client which of its pending changes the
  server has seen. REPORTED. For Sid: this is the id a refusal can point at.
  INFERRED.
- (10) One global version is simple, serialises every write, and makes
  per-reader permissions and deletes harder. Row versions remove the global
  lock and cost more to read. REPORTED. The position handed to clients should
  be opaque. REPORTED.
- (9) A global version forces soft deletes (a client that is behind must be
  told that a row went away); per-row versions allow real deletes. REPORTED.
- (16) Read permission is hard under one global version and natural under
  per-row versions plus a per-client view record. REPORTED.
- (3) The client states its schema version in both directions; two schema
  generations coexist for a while. REPORTED.
- (7), (15) The server is the only place a rejection can live. REPORTED. What
  is recorded is the named mutator and its arguments, which is the record of a
  person's act; the server's effects follow from it. REPORTED. For Sid: a click
  becomes "this named action with these arguments, on what I was shown", and
  tools act *because of* that fact, as themselves. INFERRED.
- (4) The server need not reach the client's result. REPORTED. So a speculative
  result is never evidence of anything. INFERRED.

**5. Resemblance and difference.** Same authority, same compare against a base,
same view that the offer is an intent. Different: speculative results are shown
unmarked by default; the server's mutation log is not an audit record; CVRs
are throwaway.

**6. Who they disagree with.** With CRDTs (no place for rejection; conflict
policy belongs in application code). Inside Rocicorp: global version versus row
version. With Sid's premise, directly, on physics.

### 2.7 tldraw sync (Steve Ruiz)

**1–2. What they built and why.** A canvas SDK with its own sync. The server
room "stores the authoritative copy of the document". They built it "after
struggling to find an existing solution that could keep up with the canvas".
(Ruiz, "Announcing tldraw sync", 2024-08-05, https://tldraw.substack.com/p/announcing-tldraw-sync)

- Ids carry their type: "The `id` is a branded string that includes the type
  prefix (`shape:`, `page:`, `binding:`). This prevents accidentally mixing up
  IDs from different record types." (tldraw docs, "Store", https://tldraw.dev/sdk-features/store)
- Three scopes, given in the docs as a table (paraphrased here, not quoted):
  *document* records are persisted and synced (shapes, pages, bindings);
  *session* records are optionally persisted and not synced (current page,
  camera position); *presence* records are synced and not persisted (cursor
  positions, user selection). In their words: "Presence records sync to other
  users in real time but aren't saved." (same)
- Every write is validated: the validator "Validates the full record (including
  id and typeName) on every write." (same)
- Migrations run both ways, with named versions, so old and new clients can
  share a room: the server "runs migrations to make sure clients of different
  versions can collaborate without issue." The warning: "If you omit
  `migrations`, clients on different versions won't be able to collaborate
  without errors." (tldraw docs, "tldraw sync", https://tldraw.dev/docs/sync)
- A second lane: "A room can serve some record types through a second
  partition, the object-store lane, which is persisted and permissioned
  separately from the document. Comments are the main use for it." (same)

**3. Regrets.** None written that I found. The announcement names the hard
part: "migrations and version skew, which are best handled from within the SDK".

**4. Which questions.** (14) The clearest answer in the camp: pointer and
selection are *presence* (shared live, never saved); camera and current page
are *session* (kept locally, never shared); only shapes are *document*.
REPORTED. (2), (3) They put the type in the id on purpose, for safety.
REPORTED. Convex did the same and reversed in 2025 (2.2). (3) Kind names are
words in one flat namespace that "must not collide". REPORTED. (16), (10)
Comments needed a separately permissioned partition. REPORTED.

**5–6.** Resembles Sid's gate (server authority, validation on every write).
Differs on purpose: "tldraw sync is not a general purpose real-time data
solution … we expect tldraw sync to work alongside your existing data layers."
That is a direct refusal of "one substance for everything".

### 2.8 Automerge (Martin Kleppmann, Peter van Hardenberg, Orion Henry, Alex Good, Andrew Jeffery; 2017–now)

**1. What they built and chose.** A CRDT library for JSON-like documents that
keeps all history. Three naming schemes live side by side. Operations are named
(counter, actor). Changes are named by the SHA-256 of their bytes and list the
hashes of the changes they depend on. Documents are named by a random UUID. A
version of a document is its set of head hashes.

**2. Their reasons.**

- "A change is identified by its change hash which is the SHA256 hash of the
  binary representation of the change." "An operation ID is a pair of (actor
  ID, counter), where the counter is a unique always-incrementing value per
  actor." "The object ID is the operation ID of the operation that created the
  object." And the root: "Each document has a root `map` which is identified by
  the object ID with a `null` actor id and `null` counter." (Good and Jeffery,
  "Automerge Binary Document Format", https://automerge.org/automerge-binary-format-spec/)
- Forward compatibility is a duty of every implementation: "implementations
  must preserve columns that they do not understand", and for a change's extra
  bytes: "If future versions of automerge add new metadata to changes, this
  will allow old clients to collaborate with new clients". (same)
- "Replaces" is stored both ways: "Operations are stored with their
  predecessors in change chunks and with successors in document chunks." A
  delete is only a pointer added to the thing it deletes. (same)
- Order never uses the wall clock: "'last writer wins' here is based on the
  internal ID of the operation, not a wall clock time", and losers are kept:
  "the other values are not lost. They are merely relegated to a conflicts
  object." (Automerge docs, "Conflicts", https://automerge.org/docs/reference/documents/conflicts/)
- Why hashes, when peers are strangers: a bad peer "may generate several
  distinct updates with the same sequence number, and send them to different
  nodes (this failure mode is known as equivocation)". Counter-and-actor ids
  "only works with trusted nodes". The cost is stated too: "The downside of
  using hashes as IDs is that they require more space than other schemes."
  Dependencies are a frontier, not a closure, and a record is judged only from
  its own recorded past: "we can safely decide whether update u is valid by
  basing the decision only on the updates in before(u)". (Kleppmann, "Making
  CRDTs Byzantine Fault Tolerant", PaPoC 2022,
  https://martin.kleppmann.com/papers/bft-crdt-papoc22.pdf)
- Why they keep both: "I think it was right to ship the first version of the
  binary encoding with both hash chaining and actor IDs, even though they are
  essentially two different ways of expressing the same thing." The measured
  cost of hashes: "For the paper editing trace with 260k changes, the hashes
  alone add up to more than 8MB". And deletion is where hash ids hurt: "with
  hashes, the size of this deletion change would be >32 bytes times the number
  of deleted chars." (Kleppmann in GitHub Discussion #546, "Automerge without
  actor IDs", 2023, https://github.com/automerge/automerge/discussions/546)
- Why "replaces" cannot be derived from "had seen": "The intention in the
  design of preds was that they are not redundant, because there are situations
  in which we don't want them to simply mirror the deps graph." (Kleppmann in
  issue #588, https://github.com/automerge/automerge/issues/588)
- History is wanted: "I think it's actually a desirable feature to keep as much
  history as possible… So I would rather put effort into making it efficient to
  store the whole history than into clearing the history." (Kleppmann,
  automerge-classic issue #51, 2018)

**3. What they changed or regretted.**

- *From vector clocks to hashes (0.x to 1.0).* "Dependencies between changes
  are now expressed by referencing the hashes of dependencies, rather than
  their actorId and sequence number". `getChangesForActor()` was removed "since
  it does not fit with a hash chaining approach." Declared breaking; an upgrade
  tool was promised and I found no evidence it shipped. (automerge-classic
  CHANGELOG)
- *A writer cannot name its own record.* "the frontend does not know the hash
  of a change until it has done a round-trip through the backend. Omitting the
  hash of the local actor's most recent change allows the frontend to generate
  several changes in quick succession without waiting". (BINARY_FORMAT.md,
  automerge 1.0.1-preview.6, https://unpkg.com/automerge@1.0.1-preview.6/BINARY_FORMAT.md)
- *Actor ids are a footgun at scale.* Alex Good: "I would like to get rid of
  actor IDs, I think they are currently a bit of a footgun… in practice users
  generate a new actor ID for each run of a program. The problem with this is
  that for long lived documents (or documents with many editors) this can lead
  to an unexpected explosion in the size of the encoded document". (#546) This
  is Sid's "tens of agents per person", already felt.
- *Hash ids are fragile in practice.* Conrad Irwin: "Currently we use the hash
  of the serialized chunk to identify the change; this seems like a sensible
  approach, but it's fragile in practice." Kleppmann's answer: "Postel's law
  does not hold here. The parser should accept only what the encoder produces,
  and nothing more." (#588) Also: "DEFLATE compression is not guaranteed to be
  deterministic", and saved files are not byte-identical across replicas.
  (issue #595)
- *Records with unresolved dependencies were dropped on save.* "when you save a
  doc it does not persist changes unless all their dependent changes are
  present." (Irwin, #595)
- *Deletion never shipped.* Asked since 2018. "Discarding the history is not
  currently possible without replacing the document: if you don't have the same
  history, you can't merge the same changes. That's because we use a git-like
  commit hash for each change to ensure integrity." (van Hardenberg, issue #799,
  2023) Why it is hard without a centre: "that requires knowing what all your
  devices are, and what state they are in… sometimes devices suddenly disappear
  and never come back (e.g. because their owner dropped it in the toilet)."
  (Kleppmann, automerge-classic #259, 2020) Orion Henry's truncate proposal
  began from the cost of the only workaround, a fresh document, which "also
  removes any record of what actors are responsible for the current state of
  the document" (gist, about 2021–22); the proposal never shipped. The 2026
  closure of the request says compression made history cheap enough.
- *A content-named first record is fragile.* On making every client produce the
  same first change: "if the developer ever changes the initialisation code, it
  will produce a different change with a different hash… it would be a very
  fragile API that is easy to use incorrectly." (Kleppmann, automerge-classic
  #374, 2021)
- *Authorship is only now arriving.* "Alex Good has also begun initial work
  adding author provenance to Automerge. Automerge will be able to tell you who
  wrote which commit… which has historically been a surprisingly thorny issue
  for CRDTs." ("This Month in Automerge: July '26",
  https://automerge.org/blog/2026-july/) A 2025 user question on how to tie
  actor ids to users got the answer every such system gives: keep a table
  outside.
- *The storage engine leaked into the API.* "Previously, Hexane's types leaked
  through Automerge's surface, which meant any change to the storage engine was
  potentially a breaking change to Automerge. Now the boundary is sealed."
  (August '26, https://automerge.org/blog/2026-august/)
- *What held.* "Automerge 3.0 uses the same file format as Automerge 2". The
  gains came from a new in-memory form: "pasting Moby Dick into an Automerge 2
  document consumes 700Mb of memory, in Automerge 3 it only consumes 1.3Mb".
  (https://automerge.org/blog/automerge-3/)

**4. Which questions, and what they would say.**

- (1) Name operations cheaply (counter + actor) and name batches by content
  hash *when peers cannot be trusted*. REPORTED. With one trusted gate,
  equivocation cannot happen inside the store, so the main reason for hash
  names is gone; it returns between stores and for outside audit. INFERRED.
  Hash names cost space, cost on deletion, and cannot be known by the writer
  until the bytes are final. REPORTED.
- (2) A fresh random actor id per process is cheap to make and expensive to
  keep, once actors number in the thousands. REPORTED. For Sid: an agent run
  should not mint a new long-lived actor each time; the run is a session of a
  standing actor. INFERRED.
- (17) Give the first thing a fixed, defined id (null actor, null counter). Do
  not derive it from content or code. REPORTED.
- (6) Yes, keep "replaces", and keep it apart from "had seen". REPORTED.
- (4) List the frontier, not the closure. Judge a record from its recorded past
  only. A missing dependency makes a record undeliverable, not invalid; do not
  drop it. REPORTED, the last with a regret attached.
- (9) They cannot delete, and they say why: integrity is chained through
  content hashes. REPORTED.
- (10) A version is a set of heads. A single number over a graph means
  relabelling when the graph changes (Good, issue #763). REPORTED.
- (11 when) Wall time is optional metadata. Never order. REPORTED.
- (13) Preserve what you do not understand. Be strict about one canonical
  encoding if anything is hashed. Seal storage types away from the public
  surface. REPORTED.
- (8) Actor is not author. Eight years in, authorship is being added. REPORTED.
- (7) A 2026 pattern worth noting: "an intent record (with a hash of the
  authorized content) is written into the document itself… If the document
  changes between intent and publish, the hash mismatch blocks the publish".
  REPORTED. It is a gate that checks a person approved *this exact content*.

**5. Resemblance and difference.** Automerge is the nearest thing to "every
fact carries its provenance graph", and it never deletes. Different: no
authority, so it needs hashes, tolerates dangling links, and cannot truncate.
Sid has an authority, so can use cheaper names and can know what every reader
has seen.

**6. Who they disagree with.** Kleppmann with his own users on deleting
history. With Postel's law. With Irwin on whether predecessors are redundant.
Conceded to a 2026 Ink & Switch critique: "convergence is not the same as
correctness. This is a good criticism!"

### 2.9 Ink & Switch essays and lab notebooks (van Hardenberg, Litt, Kleppmann, Wiggins, McGranaghan, Henry, Zelenka, Good, and others; 2019–2026)

**1. What they built.** Research prototypes, each closed with candid findings.
The ones that bear on Sid: the local-first essay (2019), PushPin (2020),
Cambria (2020), Riffle (2022; by Schiefer, Litt, Schickling and Jackson, not an
Ink & Switch piece as such, and van Hardenberg is not an author), Upwelling
(2023), Patchwork (2024–26), Keyhive (2024–25), Malleable Software (2025).

**2 and 3. Reasons, and what they changed, project by project.**

*Cambria — schemas evolve by lenses* (Litt, van Hardenberg, Henry, 2020,
https://www.inkandswitch.com/cambria/).

- The reversal: "Some of our early prototypes for storing documents that were
  compatible with multiple schemas performed data translations at write time.
  ... We eventually realized this was a flawed strategy. It struggled to handle
  new schemas getting added later on, after the write had already happened."
- What replaced it: "store a log of raw writes in the form of the writer
  schema, and translate between versions at read time." Each write is tagged:
  "the new operation in the log is tagged with the writer schema—the schema
  that was used to make the change."
- The pay-off for a store that never rewrites: "Since no evolution is done on
  write, old changes can be evolved using lenses that didn't even exist at the
  time of the original change."
- The grammar travels with the data: "lenses are stored in the document itself."
- The limit: they name three properties (consistency, conservation,
  predictability; "neither side operates on data they can't observe") and show
  a case where one must break. "Lens evolutions cannot magically make two
  incompatible pieces of software work perfectly together". And: "Data schemas
  aren't linear, even in centralized software."
- The grammar of the grammar: "we'll need to consider how Cambria's own data
  might be versioned and lensed."
- Five years on: "we have not yet built a production-ready version of this
  system that integrates with Automerge; one challenge is that it may require
  deep integration with the underlying data engine." (Litt, Horowitz, van
  Hardenberg, Matthews, "Malleable software", 2025,
  https://www.inkandswitch.com/essay/malleable-software/)

*Upwelling — private drafts over a shared document* (McKelvey, Jenson, Wagner,
Cook, Kleppmann, 2023, https://www.inkandswitch.com/upwelling/).

- Layers should not depend on each other: "We experimented with 'Git-like'
  dependent drafts in Upwelling and found that it made the model more difficult
  for users to understand and did not add meaningful value. ... A key insight
  of Upwelling is therefore that there should be no dependence between one
  draft and another".
- Layers float: "when a draft is merged onto the stack, it is also merged into
  all existing drafts. Any conflicts are therefore surfaced and resolved in the
  drafts before they are merged onto the stack."
- They wanted a gate: "One way of enforcing 'one merge at a time' would be to
  keep track of the latest stack on a server, and to allow writers to merge
  onto the stack only if approved by the server".
- The admission on deletion: "Because Upwelling currently records and shares
  the full history of a document, there is no way to excise names or
  information that may have appeared in earlier drafts. This is a problem
  caused by keeping too much history, a consequence of changing from a design
  that keeps much less." And the open problem: "CRDTs are currently designed to
  guarantee that users will see the same results from the same inputs. In this
  case, users will want to see the same results from different inputs."

*Patchwork — version control for everything* (2024 notebook,
https://www.inkandswitch.com/patchwork/notebook/2024-version-control/, and
2026 entries).

- Two levels only: "There's no branching from branches. You can only create
  branches from main, and merge back to main. (This seems sufficient for most
  writing use cases, although we've already encountered occasional cases where
  it's not enough.)" A branch can be made after the fact: "You can also create
  a branch retroactively with your current edit session".
- Agents write as themselves, on their own branch: "an AI bot for a style guide
  makes changes as another collaborator in the document. It puts changes on a
  branch, which you can choose to partially or completely merge". After a
  merge, "the history timeline also shows which edits came from the bot." The
  bot's own definition is a versioned record: "Bots can be shared, edited, and
  versioned just like regular documents".
- Derived versus asserted shows up in diffs: "One challenge we encountered was
  diff that distinguish manually-edited cells versus recalculated formula
  result cells."
- A record of what was shown, built in 2026: "One patchwork tool listens to all
  of the 'open document' events in the system and logs them into an 'Account
  History' document. For each entry, it keeps track of the timestamp, the
  document url and the heads at the time it was opened, and the tool it was
  opened with. This minimal set lets you see exactly what was viewed, when, and
  how." A colleague's margin note: "You're saving the heads (to see it as it
  was). Might you also save the tool heads?" And on reopening: "it opens the
  most recent version of the document (even though we saved the heads),
  because that is the expected user experience". (grjte, "Account History",
  2026-03-17, https://www.inkandswitch.com/patchwork/notebook/account-history/)

*Keyhive — who may do what, without a server* (Good, Mumm, Zelenka and others,
2024–25, https://www.inkandswitch.com/keyhive/notebook/).

- What holds before permissions exist: "by default you can write into any
  Automerge document that you know the document ID for. This style is sometimes
  called 'Swiss number' or 'Rumpelstiltskin' security."
- Identity is left out on purpose: "Keyhive deliberately excludes user identity
  (i.e. the binding of a human identity to an application's identifier like a
  public key)."
- A person is a group: "An individual is identified by a single Ed25519 public
  key - which is immutable - whilst a group is a collection of other principals
  … One way we intend to use this is to represent a person (or more
  specifically their authority) as a group". Devices sit "behind a proxy
  ('Alice'). Documents in this scenario only need to know about Alice, not
  every device."
- What cannot be revoked: "While we can revoke future write access, if someone
  has the data and the symmetric key, then they have the ability to read that
  data."
- What a centre gives: "One of the primary functions of a central server is
  that it serializes the operations performed by group members, meaning that it
  imposes a canonical ordering of events." With the caveat that "There is not
  necessarily an underlying objective truth as to which operation occurs
  'first'".
- A refusal that carries its own repair: "when a peer rejects a message due to
  an old timestamp, the rejecting peer sends their current timestamp along with
  the rejection message."

*Riffle — all state in one reactive database, including the hand* (Schiefer,
Litt, Schickling, Jackson, 2022, https://riffle.systems/essays/prelude/).

- The delight: "We were frequently (and unexpectedly) delighted by the
  persistent-by-default UI state."
- The cost: "Our prototype stores all state, including ephemeral UI state that
  would normally live exclusively in the main object graph, in the database, so
  any change to the layout of that ephemeral state forced a migration… In most
  cases, we chose to simply delete the relevant tables and recreate them while
  in development, which essentially recreates the traditional workflow with
  ephemeral state."
- A budget for showing only consistent state: "The goal … should be to converge
  all queries to their new result within a single frame after a write".

*PushPin* (van Hardenberg and Kleppmann, PaPoC 2020,
https://www.inkandswitch.com/pushpin/).

- On the hand: "some fast-changing data (e.g. mouse position or animation
  timers) could swamp the CRDT with low-value information; there is no reason
  to persist such updates. We therefore divide the application state into two
  parts: persistent state that is replicated, and ephemeral state that exists
  only locally." With an admitted gap: "ephemeral data is not associated with a
  particular CRDT state".
- On content-derived names for things that change: "this approach is not
  suitable for collaboration software, where the shared data changes
  frequently, and the hash would change on every modification."
- The type lives in the reference, not the thing: the content type "is part of
  the URL, not the document content, because the same document content may be
  rendered differently in different contexts."
- No gate, and they miss it: "The PushPin implementation currently has no
  mechanism for determining which users' writes to a document should be
  accepted." The URL "acts as a bearer token", and "there is no real way of
  revoking a user's access".

*Local-first essay* (Kleppmann, Wiggins, van Hardenberg, McGranaghan, 2019,
https://www.inkandswitch.com/essay/local-first/).

- "The key difference between traditional systems and local-first systems is
  not an absence of servers, but a change in their responsibilities: they are
  in a supporting role, not the source of truth."
- History "can't easily be truncated because it's impossible to know when
  someone might reconnect to your shared document after six months away".
- "Access permissions for documents beyond secret URLs remain an open research
  question."
- The layer wish, stated as an open problem: "users must have the freedom to
  reject edits made by another collaborator, or to make private changes to a
  version of the document that is not shared with others".

**4. Which questions, and what they would say.**

- (3) Tag every write with the grammar it was written under. Translate when
  reading, never when writing. Keep translators as data beside the facts.
  REPORTED. Expect that some pairs of grammars cannot be reconciled. REPORTED.
  Expect the engine to need deep support, or it will not ship. REPORTED.
- Layers. Keep layers independent of each other. When the base moves, every
  open layer must be re-examined. Two levels were nearly enough. A layer can be
  cut out of a session after the fact. REPORTED.
- (9) Full shared history makes names impossible to remove. They call it a
  problem, not a virtue. REPORTED. What someone has read cannot be unread.
  REPORTED.
- (8), (15) An agent is its own collaborator with its own branch; a person
  accepts or rejects; the timeline shows which edits were the agent's; the
  agent's prompt is a versioned record. REPORTED. A person's authority is a
  group of keys, so records name "Alice" and not each device. REPORTED.
- (16) Before permissions exist, knowing the id is the permission. REPORTED.
  So ids must be unguessable and must never be the only guard. INFERRED.
- (14) Do not persist the hand. If you do, tie each sample to the version of
  what was on screen, which they did not. Persisting all UI state is delightful
  and makes every UI change a schema migration. REPORTED.
- (4), (12) Record the pinned version *and* the tool; reopen the latest by
  default; someone will ask for the tool's version too. REPORTED.
- (10), (11 when) A centre exists to impose one order. There is no objective
  "first". REPORTED.
- (7) A refusal should carry what the refused party needs to fix itself.
  REPORTED (for clocks). Generalised to Sid's gate: INFERRED.

**5. Resemblance and difference.** They want for people what Sid wants: history
as material, private layers over a shared base, agents as visible
collaborators. They refuse Sid's centre on principle, and they have spent years
paying for that refusal in exactly the places Sid's gate is cheap: ordering,
permission, rejection, revocation, deletion.

**6. Who they disagree with.** With the cloud as source of truth. With Git's
model for non-programmers. With consensus as a fix for backdating: "fairly
counter to the local-first ethos … we currently consider it a last resort."
With content addressing for live documents.

### 2.10 Yjs (Kevin Jahns; 2015–now)

**1. What he built and chose.** The most deployed CRDT library. An item's id is
(client id, clock). Client ids are random integers made per session. Deletes
carry no metadata. Deleted content is garbage-collected by default.

**2. His reasons.**

- The id width is an artefact: "This is a random 53-bit integer (53 bits
  because that fits in the javascript safe integer range". (Yjs INTERNALS.md,
  https://github.com/yjs/yjs/blob/main/INTERNALS.md)
- Less entropy is enough when only clients, not items, are random: "Database
  systems often like to use UUIDv4 which allow 122 bits of entropy. But keep in
  mind that they assign a random GUID to every single item. Yjs only generates
  random ids for clients, so we don't need as much entropy." (Jahns,
  discuss.yjs.dev thread 312, 2020, read via web.archive.org)
- What a delete forgets: "Yjs does not record metadata about a deletion: — No
  data is kept on *when* an item was deleted, or which user deleted it."
  (INTERNALS.md)
- "We can't garbage collect deleted structs (tombstones) while ensuring a
  unique order of the structs." (README)

**3. What he changed or regretted.**

- The id width was held back for compatibility: "we currently only generate 32
  random bits. This has historical reasons because I want to keep compatibility
  to the 32 bit approach." (thread 312) The current source generates 53 bits.
- A clash can only be noticed afterwards. The FAQ: "When two Y.Doc instances
  with the same ClientID exist, the document might get permanently corrupted
  without a way to recover." The source now rotates the id at runtime and
  prints "Changed the client-id because another client seems to be using it."
  (docs.yjs.dev/api/faq; `src/utils/Transaction.js`)
- Who and when are being retrofitted. The v14 design note: "In order to
  implement a Google Docs-like versioning feature, we want to be able to
  attribute content with additional information (who created the change, when
  was this change created, ..)." The mechanism is a side map keyed by id
  ranges, because the item id is the only hook left. The worked example needs
  `gc: false`, and the application must supply the author as a literal. The
  docs' own example of authors: `'Bob'` and `'OpenAI o3'`. The earlier add-on,
  `PermanentUserData`, kept a second per-user delete log and answered "who
  deleted this" by scanning every user's log; it is gone from v14. v14 has been
  in prerelease since 2022. (attributing-content.md,
  https://github.com/yjs/yjs/blob/main/attributing-content.md; FOSDEM 2026
  abstract)
- On erasure for compliance, his answer to a user: "What you are trying to do
  makes sense, but it is very complicated and requires deep understanding of
  the internal structure of a Yjs document". (discuss.yjs.dev thread 537, 2021)
- "It is not recommended to restore an old document state using snapshots,
  although that would certainly be possible." (INTERNALS.md)

**4. Which questions.** (8), (11 when) If who and when are not in the record at
birth, they come back years later as a side table that works only for records
made after the add-on, and only with garbage collection off. REPORTED. (2)
Random ids without an authority can clash, and a clash is found only after
damage. REPORTED. With a gate, a clash can be refused at admission. INFERRED.
(9) Dropping deleted content is easy; knowing who deleted what, or erasing on
demand, is not. REPORTED. (6), (7) A delete that records nothing about itself
is the cheapest design and the one he is now undoing. INFERRED from the v14
work.

**5–6.** Different from Sid in every premise (no centre, small documents, size
above all). The transfer is the regret: compactness was bought by leaving out
the envelope. He disagrees with central-server designs on viability without a
centre, and with Automerge on keeping everything.

### 2.11 Seph Gentle (ShareDB, Google Wave, diamond-types, Eg-walker)

**1. What he built and chose.** A decade of operational transform with a
central server, then CRDTs, then Eg-walker (with Kleppmann): keep the original
events and their parents; build any merge structure only in memory, when
needed; throw it away.

**2. His reasons.** From Gentle and Kleppmann, "Collaborative Text Editing with
Eg-walker", EuroSys 2025, https://arxiv.org/abs/2409.14252.

- "every node is an event consisting of an operation (insert/delete a
  character), a unique ID, and the set of IDs of its parent events."
- Parents are captured, not declared: "the previous frontier in the replica's
  local copy of the graph becomes the new event's parents." And they are the
  context of meaning: "The set of parents of an event in the graph is the
  version of the document in which that operation must be interpreted."
- They never change: "we never change the parents of an existing event".
- Derived state is never stored: "we discard its state as soon as the merge is
  complete. We never write the CRDT state to disk and never send it over the
  network."
- When history may go: "the event graph can be discarded if we know that no
  event we may receive in the future will be concurrent with any existing
  event."
- Shared ids and local numbers are different things: "Semantically we use
  attributed IDs: `(agent ID, seq)`. But internally all operations are locally
  linearized in time… They're also local only - other peers will end up with
  different id-to-order mappings." And three ways to name a moment: a full
  vector clock "can be interpreted by any peer at any time"; a frontier set is
  "much smaller" but "if a peer is missing the latest changes, the frontier set
  will be incomprehensible"; the local next-order number is "a local only
  number". (diamond-types INTERNALS.md,
  https://github.com/josephg/diamond-types/blob/master/INTERNALS.md)
- The common case is compressed: "By default we assume that every event has
  exactly one parent, namely its predecessor in the topological sort. Any
  events for which this is not true are listed explicitly." (paper, §3.8)

**3. What he changed or regretted.** The header of his own internals document:
"This was written for an earlier version of diamond types when I persisted the
merge structure like yjs and automerge do. This has much worse performance when
there are no concurrent changes, and a bigger file size." On Google Wave's
federation over OT: "We got it working, kinda, but it was complex and buggy…
But it never really worked." On unbounded growth: "Can you ever delete that
data? Probably not." ("I was wrong. CRDTs are the future", 2020,
https://josephg.com/blog/crdts-are-the-future/)

**4. Which questions.**

- The premise of Sid's whole question, in his terms: store what happened and
  what it stood on; never store what an algorithm made of it. Then the
  algorithm can be replaced later. REPORTED for text. INFERRED for facts: this
  is the strongest support in the camp for "running answers are never stored".
- (4), (11 reads) The context an act was made in is part of the act. Capture it
  mechanically. REPORTED.
- (1), (10) A shared id and a local position are two names. A single integer
  position is meaningful only inside one store. REPORTED.
- (0), (13) With one gate nothing old is ever concurrent with anything new, so
  by his own rule the log is *technically* discardable below the head. Whether
  to keep it is then a question of provenance, not of merging. INFERRED.
- (11 because-of) Compress the common case: assume the obvious single parent,
  list only exceptions. REPORTED.

**5–6.** His events are keystrokes, not facts with authors; he has no gate. He
disagrees with Yjs and Automerge on persisting merge state, with his own OT
past on needing a centre, and would accept Sid's centre as the easy case.

### 2.12 Matthew Weidner (CRDT survey, Fugue, Collabs, list-positions)

**1. What he built and chose.** CRDT libraries and the clearest recent writing
on what a central server makes unnecessary.

**2. His reasons.**

- "it is ironic to see that, in the centralized model used by this blog post,
  CRDTs and OT are merely optimizations over server reconciliation, which is
  straightforward and completely flexible." ("Architectures for Central Server
  Collaboration", 2024, https://mattweidner.com/2024/06/04/server-architectures.html)
- Why the record is named by its maker and not by the server: "(Having Bob
  generate the IDs, instead of waiting for the server to assign them, lets him
  reference those IDs in subsequent 'insert after' operations before receiving
  a response to his first operation.)" ("Collaborative Text Editing without
  CRDTs or OT", 2025, https://mattweidner.com/2025/05/21/text-without-crdts.html)
- Order is the server's: "a central server, which assigns a total order to
  operations (namely, the order that the server receives them)." (same)
- With one writer, a version is one number: "A typical version corresponds to a
  point in time on the homeserver: the state resulting from the first 𝑛 updates
  in the homeserver's log, for some 𝑛. In principle, one could also expose
  versions that include tentative local updates, but these would require more
  complicated versionIDs (e.g., encoding a vector clock)." And: "A version is
  immutable, and it is unambiguously and globally identified by a version
  string of the form `<docID>@<versionID>`." ("Proposal: Versioned
  Collaborative Documents", PLF 2023,
  https://mattweidner.com/assets/pdf/versioned_collaborative_documents.pdf)
- On ids: replica ids "are usually random instead of 'the highest replica ID so
  far plus 1'", because replicas are created concurrently. Sizes he lists: "a
  UUID v4 is 122 random bits, a Collabs replicaID is 60 random bits (10 base64
  chars), and a Yjs clientID is 32 random bits". (CRDT Survey, Part 3, 2023,
  https://mattweidner.com/2023/09/26/crdt-survey-3.html; the Yjs figure was
  true then)
- "a replica is not synonymous with a device or a user… In previous posts, I
  often said 'user' out of laziness". And: "Avoid the temptation to reuse a
  replica ID across replicas on the same device… That can cause problems if the
  user opens multiple tabs, or if there is a crash failure". (same)

**3. What he changed or regretted.** He reversed on needing CRDTs or OT under a
server. He named the limit of his own position libraries: they "fix each
character's position in a global total order as soon as the user types it".

**4. Which questions.**

- The cost of strict no-optimism, stated exactly. One strategy: "if a client
  possesses pending local operations, it refuses to process remote operations.
  Instead, the client waits until the server has acknowledged all of its
  pending local operations". Its failure: "this strategy can block updates
  indefinitely: if a client creates new operations faster than the server
  acknowledges them—e.g., by typing rapidly on a high-latency connection—then
  that client will never display updates from the server." REPORTED. Sid's rule
  is the mirror image (wait for the gate before showing one's own act), and the
  same arithmetic applies to an agent writing at machine rate. INFERRED.
- (1) The maker names the record so that later work can point at it before the
  server answers. REPORTED.
- (10) One number per ordering domain, as long as nothing tentative is ever
  part of a version. REPORTED. Sid's no-optimism rule is what keeps "as of"
  simple. INFERRED.
- (2) Random, because creation is concurrent. Session-scoped, not device-scoped.
  REPORTED.
- (8) Replica is not user. REPORTED.
- (9) The server "can work around this by storing IDs in its internal list even
  after the corresponding characters are deleted". REPORTED. The id outlives
  the value.
- (13) "clients need to store the entire operation log indefinitely—not just
  the current state. You can mitigate this cost by using fancy compression
  and/or by putting the log in cold storage." REPORTED.

**5–6.** His setting is one document and one server, so the transfer to a gate
is direct. His definition of a CRDT excludes Sid's system on purpose: "you're
not allowed to put a central server in charge of the state". He disagrees with
the CRDT orthodoxy he came from.

### 2.13 AT Protocol and Bluesky (Paul Frazee, Bryan Newbold, Daniel Holmgren, Devin Ivy; Martin Kleppmann as co-author; 2022–now)

**1. What they built and chose.** A signed record system at planet scale, built
by people who had built Secure Scuttlebutt and Beaker. Each account has one
signed repository. Records live at (account, schema name, key). Accounts are
named by permanent ids (DIDs), apart from changeable handles. Schemas are named
by words (NSIDs). Indexing is done by big separate services.

Specs and the paper below are INSTITUTIONAL. Newbold's and Frazee's posts are
REPORTED in the first person.

**2. Their reasons.**

- Deletion is a first-class right of the record: "Record deletion is supported
  without leaving a trace or 'tombstone' of previous contents." And downstream:
  "services are expected not to differentiate between content which has never
  existed and content which has been entirely deleted." (specs, Repository;
  Accounts, https://atproto.com/specs/repository, https://atproto.com/specs/account)
- A name for the slot, a second name for the version: "An AT URI pointing to a
  specific record in a repository is not a strong reference, in that it is not
  content-addressed." "When a strong reference to another record is required,
  best practice is to use a CID hash in addition to the AT URI."
  (https://atproto.com/specs/at-uri-scheme)
- Time-ordered keys are for the tree, not for meaning: the TID scheme "was
  intentionally selected to provide chronological sorting of MST keys… Appends
  are more efficient than random insertions". And the warning: "these keys can
  be specified by the end user and could have any value, so they should not be
  trusted." "because atproto is an open network, uniqueness of TIDs can not be
  guaranteed." (specs, Repository; Record Key; TID)
- The server's number is not the record's name: "These sequence numbers should
  be seen as an annotation of the authenticated data (similarly to createdAt
  timestamps), and not an intrinsic property of the operations themselves… A
  PLC mirror or replica service could plausibly assign different sequence
  numbers to the same operations". (did:plc spec v0.3,
  https://github.com/did-method-plc/did-method-plc)
- Schema evolution rules: "Any new fields must be optional / Non-optional
  fields can not be removed… / Types can not change / Fields can not be renamed
  / If larger breaking changes are necessary, a new Lexicon name must be used."
  When is a schema fixed? "public adoption and implementation by a third party,
  even without explicit permission, indicates that the Lexicon has been
  released and should not break compatibility." Schemas are records: "Lexicon
  schemas are published publicly as records in atproto repositories". But the
  schema language is not written in itself: "It is an intentional decision to
  not express the Lexicon schema language itself recursively". (https://atproto.com/specs/lexicon)
- The gate does not always know the grammar: "if the PDS knows the record
  Lexicon, it validates. If the PDS does not know it… the record is allowed to
  be created. This is also referred to as 'Fail-Open'." (same) Newbold: "we
  probably don't want PDS instances to have to do 'live' Lexicion resolution in
  the middle of processing record creation", and "We don't currently have a
  strong conception of 'recent' / 'latest' Lexicon schema". (Discussion #2940, 2024)
- Clocks: a label's timestamp carries the note "timestamps in a distributed
  system are not trustworthy or verified by default." Index lag is reported
  outside records: "Services can indicate synchronization status using the
  Atproto-Repo-Rev HTTP response header". (specs, Labels; Sync)
- Metadata beside records: labels are separately signed statements about a
  record, optionally pinned to one version. A retraction is a new label with
  `neg` set, and "does not mean that the inverse of the label is 'true', only
  that the previous label has been retracted." (https://atproto.com/specs/label)
- Why no peer-to-peer, from a builder of two such systems: "We never solved
  multi-device syncronization in a way that preserved the convenience of the
  technology. Same for key backup/sync." "People won't sacrifice features for
  hypothetical improvements." And the partition rule: "You don't have to
  coordinate permissions between users because they never transact on the same
  primary records; instead you treat each user as the sole owner of their
  dataset". (Frazee, "Why isn't Bluesky a peer-to-peer network?", 2024-01-21,
  https://www.pfrazee.com/blog/why-not-p2p; the spelling is his)
- Keys are held for people: "manual key management is not appropriate for most
  users… The Bluesky PDSes therefore hold these signing keys custodially on
  behalf of users". (Kleppmann et al., "Bluesky and the AT Protocol", 2024,
  https://arxiv.org/abs/2402.03239)

**3. What they changed or regretted.** This team publishes its debts.

- *The chain of previous commits was dropped.* "We mostly removed the concept
  of prev pointers (as CID Links) to previous commits, forming a chain of
  history." The field remains in the format, "virtually always null", kept
  "for v2 backwards compatibility", and its nullability was settled by a Go
  serialiser: "the idiomatic way to serialize data structures in golang works
  only one way or the other… Our current plan is to bend the rules of protocol
  stability… We don't love updating the spec to match implementation".
  (Newbold, Discussion #2181, 2024; spec, Repository) The design reason is in
  the sync proposal: verifying a chain "requires a local copy of the complete
  repository tree", which "is expensive"; they replaced it with an unsigned
  hint and per-commit checks, after which relays became "non-archival".
  (proposals/0006; "Relay Updates for Sync v1.1", 2025)
- *Ids built on the first record's bytes are forever.* "there exist many
  did:plc identifiers where the DID identifier itself is based on the hash of
  the old format, so they will unfortunately be around forever." (did:plc spec)
- *Content-addressed references make migration recursive.* On removing a legacy
  blob format: "If we only update the records, then 'strong' references (by
  CID) will throw warnings (eventually), but that is way easier than recursive
  migrations". (Newbold, "Protocol Tech Debt (2024)", Discussion #2128)
- *Timestamps stay "Unfortunate"* (his section heading). "createdAt and
  indexedAt can be combined in to a sortAt for use in display and ordering
  posts in feeds. This continues to cause confusion and consternation, and some
  folks would think that global reliable timestamps are important enough to
  warrant an extension to the protocol." (same)
- *The spec lost to the data.* Record keys could not contain colons; popular
  records already did; "we decided we'll allow colons in record keys". (same)
  And the general menu when old rows are wrong: "a bulk graph update (eg,
  re-writing records); provide some other migration path; slightly loosen the
  specification; or just accept some small breakage". (Discussion #1910)
- *Field names collide.* "Bluesky added 'pinned posts' to profiles and
  unintentionally clobbered unspecced uses of that field." Frazee's conclusion:
  "Schemas are only interpretable in the context of working software", and
  "who controls a lexicon definition, and who has the most users for a given
  lexicon. Those are levers of authority." (Frazee, "Guidance on Authoring
  Lexicons", 2025, https://www.pfrazee.com/blog/lexicon-guidance)
- *A delete carries no context.* A request that the delete event include the
  deleted record was closed as not planned: "the firehose event for the
  deletion references only the rkey". (indigo issue #927, 2025) Every consumer
  must hold the prior state itself.
- *The append-only identity log was edited.* "in late October, a number of
  invalid test operations were removed from the PLC directory, to bring it in
  compliance with the written specification." (Roadmap, 2026-03-24) Rejected
  operations are otherwise kept: "the set of all operations for all identifiers
  (even 'nullified' operations) can be enumerated and audited." Replicas now
  act as witnesses: if the primary dropped an operation, "the replicas would
  still have a copy of the deleted data." (Buchanan, "PLC Read Replicas", 2026)
- *The identity log leaks.* "The full history of DID operations and updates,
  including timestamps, is permanently publicly accessible. This is true even
  after DID deactivation". (did:plc spec)
- *Encodings moved.* The data model now names "DRISL (which is successor to
  DAG-CBOR)", and floats are banned because re-encoding "is not always
  consistent". Old signatures age: "With key rotation, verification of older
  commit signatures can become ambiguous."
- *Permissions came last.* "The original design focus of the protocol was to
  support public conversations in a global context"; private records are 2026
  roadmap work. The word "GDPR" does not occur in the paper or the repository,
  account, or blob specs.

**4. Which questions, and what they would say.**

- (1) Slot, version, and server number are three names. The number is an
  annotation a replica may assign differently. INSTITUTIONAL.
- (2) Time-ordered ids buy storage locality and nothing else; never trust the
  time in them; expect adversaries to choose them. INSTITUTIONAL. An id that
  hashes the first record binds you to that record's format forever. REPORTED
  as a regret.
- (3) Words, with owner authority through DNS, and strict additive rules.
  Breaking change means a new name. A schema is a record, but the language of
  schemas is outside the system. INSTITUTIONAL. The name is weaker than the
  software that writes it. REPORTED (Frazee). There is no schema version for a
  record to pin. REPORTED (Newbold). That last gap is the one Sid's "grammar is
  a versioned fact" closes. INFERRED.
- (4) A reference either floats or pins, and the record's author chooses per
  reference. INSTITUTIONAL.
- (5) Lag is a header, not a record, and they still argue about stickiness.
  REPORTED. The fix they propose for identity reads is to tell the client the
  version to expect. REPORTED.
- (6) They removed "replaces" from the signed record and keep an unsigned hint
  in the stream. INSTITUTIONAL. Consumers then cannot know what a delete
  deleted. REPORTED.
- (7) Keep rejected operations and let replicas witness the log. REPORTED. Even
  so they purged bad rows once. REPORTED.
- (8), (15) Custodial keys; the host acts for the person. INSTITUTIONAL. The
  record shows the person. INFERRED.
- (9) Delete without trace, by design, and they cannot make others comply.
  INSTITUTIONAL.
- (10) One writer per repository; everything cross-cutting is an index. "To
  find all followers of user B requires indexing the content of all
  repositories." INSTITUTIONAL.
- (11 when) Client time and indexer time are both kept, and combined for
  display. REPORTED, with regret.
- (16) Public first; permissions are being retrofitted four years in. REPORTED.
- (17) "PLC stands for 'Placeholder' because we're not in love with a single
  service model." (Frazee, 2024.) The spec now reads "Public Ledger of
  Credentials". Governance moved to a Swiss association in 2025. REPORTED. A
  placeholder chosen for the first record became permanent.
- (13) Floats, map order, null versus absent: every one became a rule after an
  interop failure. REPORTED.

**5. Resemblance and difference.** Same scale ambition. Same records with
schema names, authors, and versions. Different: many writers who do not trust
each other; public by default; deletion treated as a right; no provenance on
records; no central gate, so no compare-and-set across accounts.

**6. Who they disagree with.** With Secure Scuttlebutt (no deletion, no
multi-device, unrecoverable keys). With Nostr (manual keys, no rotation). With
blockchains (cost per user). With pure peer-to-peer: "it has no answer for the
governance of shared resources" (Frazee, "Practical Decentralization", 2026).
Inside the team: whether the spec follows the code or the code the spec.

### 2.14 Nostr (fiatjaf; also hodlbod, and the NIP authors; 2020–now)

**1. What they built and chose.** Signed events sent to dumb relays. The event
id is the hash of a canonical serialisation. Kinds are integers. Time is the
author's. Deletion is a request.

**2. Their reasons.** From NIP-01, https://github.com/nostr-protocol/nips/blob/master/01.md.

- The id: "To obtain the `event.id`, we `sha256` the serialized event", a fixed
  array of pubkey, created_at, kind, tags, content. The canonical form is
  written into the spec: "To prevent implementation differences from creating a
  different event ID for the same event, the following rules MUST be followed
  while serializing".
- Storage behaviour is encoded in the kind number. For replaceable kinds, "only
  the latest event MUST be stored by relays, older versions MAY be discarded."
  Ephemeral kinds are not stored. The winner between versions is chosen by the
  author's clock, then by hash: "the event with the lowest id (first in lexical
  order) should be retained".
- A refusal is a wire message to one client (`OK … false` with a reason
  prefix), never a stored thing.
- On deletion (NIP-09, "Event Deletion Request"): "Clients MAY choose to inform
  the user that their request for deletion does not guarantee deletion because
  it is impossible to delete events from all relays and clients." And the
  request must outlive its target: "Relays SHOULD continue to publish/share the
  deletion request events indefinitely". NIP-62 adds the legal case: "This
  procedure is legally binding in some jurisdictions, and thus, supporters of
  this NIP should truly delete events from their database", and "Relays MUST
  ensure the deleted events cannot be re-broadcasted into the relay."

**3. What they changed or regretted.**

- *Delegation on every record was tried and withdrawn.* NIP-26 now carries the
  banner "`unrecommended`: adds unnecessary burden for little gain". fiatjaf's
  reason: "in a world in which most Nostr users are using NIP-26 for
  everything, clients that do not implement NIP-26 become completely useless,
  as all they will see is a constant stream of random keys." (2023,
  https://fiatjaf.com/4c79fd7b.html) What won is remote signing (NIP-46), where
  the signer "generally has control over these keys" and signs as the user. The
  record is then identical whoever acted. Scope lives in a connection string,
  never in a record.
- *Edits.* "Direct edits are a centralizing force on Nostr", and the general
  law: "No, they are not optional. If edits become widespread they necessarily
  become mandatory. Any client that doesn't implement edits will be displaying
  false information to its users". His alternative is a delay before
  publishing: "giving the user the opportunity to cancel and edit it again
  before it is actually posted." (fiatjaf, "The case against edits", 2024,
  https://fiatjaf.com/ad84e3b3.html)
- *The registry of kinds.* "Most current NIPs are actually just schema
  descriptions of what tag is what and what is the shape of the events. It's
  kind of a waste of numbers and human memory". He proposes a bare registry
  "only for reserving kind numbers". (fiatjaf, "The end of NIPs", 2025,
  https://fiatjaf.com/311b999e.html) The index of NIPs keeps its dead: a dozen
  entries struck through with reasons, and deprecated kind numbers listed
  forever. NIP-41 (key invalidation) was removed outright.
- *Key handling still blocks.* "Clients don't currently deal well with latency.
  In order for NIP 46 to work smoothly, clients will have to implement better
  loading, debouncing, optimistic updates, publish status, and 'undo'."
  (hodlbod, "Key Management is a Blocker", 2024)

**4. Which questions.**

- (1) Content-hash ids work if the canonical form is in the spec from day one.
  REPORTED. They have no server number at all. REPORTED.
- (3) Numbers need a registry and carry no meaning; the ranges that encode
  storage behaviour are irregular because kinds were handed out before the
  ranges existed. REPORTED facts; the reading is INFERRED. The idea worth
  keeping: *how a kind is stored* (kept, replaced, not stored) is a property of
  the kind. INFERRED.
- (6) No pointer to what was replaced; older versions may be dropped; order by
  the author's clock. REPORTED. fiatjaf regrets replaceability itself. REPORTED.
- (8), (15) On-record delegation failed because every reader had to understand
  it. REPORTED. That burden is lighter with one store, where the gate resolves
  delegation once at admission. INFERRED. The replacement erased the agent from
  the record. REPORTED. Do not copy that. INFERRED.
- (9) A deletion request lives forever and cannot be enforced; a legal delete
  needs a permanent do-not-readmit list. REPORTED.
- (11 when) The author's clock decides which version wins, so it can be gamed.
  REPORTED.
- Above the table, fiatjaf's law in my paraphrase: any optional feature that
  changes what readers see becomes mandatory. REPORTED in substance (the edits
  quote above). It argues for a small envelope.

**5–6.** Like Sid's: small records, kinds, an id per record, one envelope for
everything. Unlike: no authority, no order, no provenance. fiatjaf disagrees
with delegation schemes, with edits, with "self-sovereign" slogans ("these keys
don't do anything without the means of actual action in the world"), and with
p2panda-style designs that lean on one strong peer.

### 2.15 Secure Scuttlebutt and its successors (Dominic Tarr, André Staltz, cryptix, Aljoscha Meyer, Sam Gwilym, the p2panda team; 2014–now)

**1. What SSB chose.** One append-only log per key pair. Every message names the
previous message's hash and its sequence number. The message id is the hash of
the whole message, content and signature included.

**2. Reasons, from the protocol guide** (https://ssbc.github.io/scuttlebutt-protocol-guide/).

- "A message ID is a hash of the message including signature."
- The canonical form is one engine's output: "The canonical format is defined
  by the ECMA-262 6th Edition section JSON.stringify", two-space indentation
  included. And key order matters forever: "Fields within content can appear in
  any order but the order must be remembered for later."
- The kind is a bare word: `content.type` "must be a Unicode string between 3
  and 52 code units long".
- Why ids are opaque keys: "There are no unique usernames, because you can't
  guarantee two people in separate places from choosing the same username".
  (Staltz, "An off-grid social network", 2017)

**3. What the successors changed.** Every successor is a written critique.

- *The name of the design was withdrawn.* Aljoscha Meyer, in the disclaimer now
  at the top of the Bamboo README
  (https://github.com/AljoschaMeyer/bamboo):

  > Some years after authoring this specification, I do not consider the
  > "append-only log" terminology to be appropriate anymore… This makes the log
  > an "append-or-delete log", not an "append-only log".
- *The id stopped covering the content.* Bamboo's entry holds "the hash of the
  actual _payload_" and its size, so the payload can go. Gabby Grove: "the feed
  entry only references content by hash to enable content deletion without
  breaking verification of the feed". PPPPP: "**Msg ID** = `hash(msg.metadata)`".
  All four successors made this same change.
- *The serialisation left the JavaScript engine.* PPPPP: "we follow the "JSON
  Canonicalization Scheme" (JSC) defined by RFC 8785". Gabby Grove uses CBOR.
- *One identity, many devices, recorded on the record.* PPPPP messages carry
  `accountTips`, the tips of the account's own history, so each record states
  which version of the identity it was signed under. Delegation is recorded
  with scoped powers (`'add' | 'del' | 'internal-encryption' |
  'external-encryption'`) and a consent signature. SSB's own fix came late and
  partial: in "fusion identity", "Only new members can be added to a fusion,
  there is no removal of members only tombstoning", and the spec's list headed
  "Out of scope for v1" includes "Use of fusion identity for authorisation
  logic".
- *A fixed first record.* PPPPP: "**Moot** = the root of a feed, a msg that is
  deterministically predictable and empty, so to allow others to pre-know its
  msg ID".
- *A derived number had to be stored once pruning existed.* "we NEED this field
  because it is the most reliable way of calculating lipmaa distances between
  msgs, in the face of sliced replication."
- *Completeness proofs were abandoned on purpose.* Willow: "The need to verify
  the hash chain also makes it difficult to edit or delete (meta-)data…
  designing systems for unbounded growth tends to backfire sooner or later.
  Willow deliberately avoids any cryptographic proofs of completeness, allowing
  for traceless data removal when the need arises." And on content addressing:
  "For every new piece of data, I must eventually leave the system and transmit
  a new hash out-of-band." (Meyer and Gwilym, "Willow Compared",
  https://willowprotocol.org/more/willow_compared/index.html)
- *Earthstar's list of SSB's faults:* "Does not allow editing or deletion… /
  Cannot reuse the same identity between many devices… / Uses an append only
  log, whereas Earthstar uses a key-value database."
- *p2panda's schema ids and two kinds of reference.* "Application schema ids
  are constructed from the schema's name and document view id". And as field
  types: "Relations represent the whole referenced document through their
  document id… Pinned relations point at immutable versions of documents
  through their document view id". In 2024 they removed Bamboo: "we realised
  that we weren't making full use of the features it provides."
- *Forks.* When one key signs two different messages with the same number, SSB
  treats the feed as dead. Lavoie's analysis: such logs "do not provide
  eventual consistency when malicious participants fork their logs". The
  remedies record the fork with proof instead of rejecting the feed.
- *Clocks, argued in full.* "any system that *relies* on accurate timestamps is
  a centralised system in disguise." Willow accepts arbitrary author timestamps
  and bounds the damage: "An Entry with a high timestamp only overwrites
  Entries in the same subspace." ("Timestamps, Really?",
  https://willowprotocol.org/more/timestamps_really/index.html)

**4. Which questions.**

- (0) The authors of append-only logs now say "append-or-delete". REPORTED.
- (1) Do not let the id cover the value. Hash the envelope, and inside it the
  hash of the value. REPORTED, by four independent teams. Put the canonical
  form in the spec, never in a runtime. REPORTED.
- (3) A key can be a readable name plus the id of the exact grammar version
  (p2panda). REPORTED.
- (4) Whether a reference floats or pins is a *type of field*, chosen in the
  grammar. REPORTED.
- (8), (15) Write the identity version and the scoped grant into what the
  record stands on. REPORTED (PPPPP).
- (9) A deleted value leaves a fixed-shape hole and the log stays valid.
  REPORTED.
- (10) Two writers on one log is a fork, and a fork kills the log unless forks
  are a recorded state. REPORTED.
- (17) The first record should be predictable and empty so that everyone can
  know its id in advance. REPORTED.
- (5), (13) Once anything can be pruned, numbers that used to be derivable must
  be stored. REPORTED.
- (11 when) Sid's store is centralised openly, not "in disguise", so the
  objection does not bite; the bound on blast radius still does. INFERRED.

**5–6.** SSB is the nearest thing to Sid's never-rewrite rule that lived for
years among real users, and its builders are the most direct witnesses to what
that rule costs when ids, order and integrity are all tied to the bytes. Sid's
gate removes their hardest problems (forks, multi-device, onboarding by full
replay). It does not remove deletion or format ageing. Tarr, quoted in Gabby
Grove, insisted the author stay on every entry: "It should always be known
which key-pair created a signature". Staltz: "I fundamentally don't trust any
system that has an admin with sudo powers."

Not found: a written retrospective by Tarr; a single post by Staltz announcing
his move from SSB to PPPPP (the PPPPP spec itself carries the critique).

### 2.16 Matrix (added; checked by me against spec.matrix.org on 2026-09-20)

A federated event graph with a production answer to three of Sid's questions.
All INSTITUTIONAL.

- *Redaction keeps the envelope and drops the content.* "Since some events
  cannot be simply deleted, e.g. membership events, we instead 'redact' events.
  This involves removing all keys from an event that are not required by the
  protocol. This stripped down event is thereafter returned anytime a client or
  remote server requests it. Redacting an event cannot be undone, allowing
  server owners to delete the offending content from the databases."
  (Client-Server API, Redactions) The list of surviving keys is fixed per room
  version and is the envelope: `event_id`, `type`, `room_id`, `sender`,
  `state_key`, `content`, `hashes`, `signatures`, `depth`, `prev_events`,
  `auth_events`, `origin_server_ts`. (Room version 11)
- *The id is a hash that survives redaction.* "The reference hash of an event
  covers the essential fields of an event, including content hashes… The event
  is put through the redaction algorithm. The signatures and unsigned
  properties are removed from the event, if present. The event is converted
  into Canonical JSON. A sha256 hash is calculated". A separate "content hash
  … covers the complete event including the unredacted contents." (Server-Server
  API) This is the two-hash design of 2.15, in production since room version 3
  (2019).
- *They moved from assigned ids to hash ids, and said why.* With a separate id
  field, "servers receive multiple events with the same ID in either the same
  or different rooms where the server cannot easily keep track of which event
  it should be using." (Room version 3) The proposal adds that clients "should
  already be treating event IDs as opaque strings", and that hash agility now
  rides on the container's version: "now that room versions exist, changing
  hash functions can be achieved by bumping the room version." (MSC1659)
- *Each record names the policy records that allowed it.* "The auth_events
  field of a PDU identifies the set of events which give the sender permission
  to send the event. The auth_events for the m.room.create event in a room is
  empty". (Server-Server API)

For Sid: (9) a worked, deployed shape for deleting a value and keeping the
fact; (1) a content hash can be an id *if* it is defined over the redacted form
and the canonical encoding is in the spec, and note their reason for hash ids
(many servers assigning clashing ids) does not exist under one gate; (7) a
precedent for writing "what the gate checked" onto the record; (17) the first
record is the one whose authorisation list is empty. All INFERRED applications
of INSTITUTIONAL facts.

---

## 3. Section two: versioning

### 3.1 Git (Linus Torvalds, Junio Hamano, Jeff King, brian m. carlson, Derrick Stolee; 2005–now)

**1. What they built and chose.** History named by content. "It uses the SHA-1
hash function to name content. For example, files, directories, and revisions
are referred to by hash values unlike in other traditional version control
systems where files or versions are referred to via sequential numbers." (Git,
"hash-function-transition", git.git, first dated 2017-03-03,
https://git-scm.com/docs/hash-function-transition)

**2. Their reasons.**

- The hash is a name and a checksum first: "There's a big difference between
  using a cryptographic hash for things like security signing, and using one
  for generating a 'content identifier' for a content-addressable system like
  git." "Our trust is in people". "Think of it like 'parity on steroids'."
  With a concession: "in git we also end up using the SHA1 when we use 'real'
  cryptography for signing the resulting trees, so the hash does end up being
  part of a certain chain of trust." (Torvalds, public post, 2017-02-25,
  archived at web.archive.org/web/2017/https://plus.google.com/+LinusTorvalds/posts/7tp2gYWQugL)
- A hash is chosen for a decade, not forever: the replacement should be
  "trustworthy and useful in practice for at least 10 years." (transition doc)
- Order is the graph, never the clock. Where a clock is used, it is repaired
  against the graph: a commit's "corrected committer date" is "the maximum of
  its committer date and one more than the largest corrected committer date
  among its parents." The raw date is a heuristic that "is not used when the
  topological order is required (such as merge base calculations)." (Git,
  "commit-graph" technical doc)
- A commit carries two actors and two clocks (author and committer, each with
  a time), and unknown headers are kept. This line is my background knowledge
  of the commit format, not a gathered quote.
- Things known later go beside the record: notes "add annotations with
  information that was not available at the time a commit was written",
  "without touching the objects themselves." Identity is corrected outside
  history: `.mailmap` maps "author and committer names and email addresses to
  canonical real names". Replacement refs redirect one object to another, and
  are honoured by everything "except those doing reachability traversal (prune,
  pack transfer and fsck)". (git-notes, gitmailmap, git-replace docs)

**3. What they changed or regretted.**

- *The hash transition is the long lesson.* SHAttered was February 2017. The
  plan dates from March 2017. SHA-256 repositories became possible in 2020. In
  October 2025, brian m. carlson: "The SHA-256 interoperability work is not
  done yet. My estimate of this work is 200–400 patches, of which about 100 are
  done… which is unrealistic without additional contributors." (quoted in
  Corbet, "Git considers SHA-256, Rust, LLMs, and more", LWN, 2025-10-21,
  https://lwn.net/Articles/1042172/) On 2026-09-20 the breaking-changes
  document still says of Git 3.0: "There is no planned release date for this
  breaking version yet." SHA-256 will be the default only "for new
  repositories", and "There is no plan to deprecate the "sha1" object format at
  this point in time." The stated blocker is other people's software: "An
  important requirement for this change is that the ecosystem is ready".
- *What the plan could not carry over.* Sidecar data keyed by the old id: "The
  `git notes` tool annotates objects using their SHA-1 name as key. This design
  does not describe a way to migrate notes trees to use SHA-256 names." Ids in
  free text: after a rewrite, "if some of your commit messages refer to prior
  commits by (abbreviated) sha1, after the rewrite those messages will now
  refer to commits that are no longer part of the history." (git-filter-repo
  README) Signatures that do not say which scheme they mean: "The signed
  payload for signed commits and tags does not explicitly name the hash used to
  identify objects… Fortunately SHA-256 and SHA-1 have different lengths."
- *Two non-goals they chose.* "Intermixing objects using multiple hash
  functions in a single repository", and "Taking the opportunity to fix other
  bugs in Git's formats and protocols." Old brokenness is kept on purpose so
  conversion can round-trip.
- *A number that should have been there.* Generation numbers had to be added in
  a side file because commit dates skew. Two generations of that side file now
  coexist, and readers must detect a mix. (I could not open the mailing-list
  thread where maintainers are said to wish the number had been in the commit;
  lore.kernel.org blocks fetches. I do not claim the quote.)
- *Deletion.* GitHub's guidance begins: "as a first step you need to revoke
  and/or rotate that secret… that may be sufficient to solve your problem."
  Rewriting advertises what was removed: "clueful users with an existing clone
  will notice the history divergence and can use it to quickly and easily find
  the sensitive data still in their clone". It also strips signatures "for
  commits that pre-date the sensitive data removal as well", and "You cannot
  remove sensitive data from other users' clones". (GitHub Docs, "Removing
  sensitive data from a repository")

**4. Which questions.**

- (1) A content id is a fine checksum and a poor forever-name. Its scheme must
  be named inside anything that quotes it. Migrating it takes a decade because
  every outside tool holds the old ids. REPORTED facts; the summary is
  INFERRED.
- (11 when) Two clocks on a record; neither orders; when a usable clock is
  needed, make it monotone along the causal links. REPORTED. For Sid: the gate
  can stamp "when" as the later of its wall clock and one tick past the latest
  "when" among the facts read. INFERRED.
- (7), (6) Later knowledge goes beside the record, keyed by the record's id.
  That makes id stability load-bearing. REPORTED.
- (8) Two actors per record (who made it, who admitted it). Identity fixes are
  applied at display time from a separate, versioned map. REPORTED.
- (9) Rotate first. Rewriting is loud, breaks every downstream name, and cannot
  reach copies. REPORTED.
- (13) Never mix two id schemes in one store (Git). Fossil says the opposite
  (3.3). REPORTED.

**5–6.** Git's history is small, its writers are trusted colleagues, and its
"store" is copied everywhere. The transfer is about ageing: what two decades do
to an id scheme and an envelope. Git disagrees with Fossil on rewriting and on
mixing hashes.

### 3.2 Jujutsu, and Mercurial's changeset evolution (Martin von Zweigbergk; Pierre-Yves David)

**1. What they built and chose.** Two ids on purpose, a pointer from each
rewrite to what it replaced, and a log of every operation on the repository.

**2. Their reasons.** From the jj docs (github.com/jj-vcs/jj, `docs/`, read
2026-09-20).

- The stable name is random: "A change ID is a unique identifier for a change.
  They are typically 16 bytes long and are often randomly generated." It is
  shown in its own alphabet (letters k–z) so it cannot be mistaken for a
  content hash. "Rewriting a commit results in a new commit, and thus a new
  commit ID, but the change ID generally remains the same." (glossary)
- Superseded is hidden, not gone: "A commit that is abandoned or rewritten
  stops being visible and is labeled as 'hidden'. Such a commit is still
  accessible by its commit ID". (glossary)
- Every operation is a record with its actor, place and time: the operation
  object holds "pointers to the operation(s) immediately before it, as well as
  metadata about the operation, such as timestamps, username, hostname,
  description." (architecture)
- Writes never fail; divergence is a value: "The operation cannot fail to
  commit (except for disk failures and such). It is left for the next command
  to notice if there were divergent operations." A contested pointer is
  recorded as "moved from A to B or C". (concurrency)
- A fixed origin: "The root commit is a virtual commit at the root of every
  repository. It has a commit ID consisting of all '0's and a change ID
  consisting of all 'z's." (glossary)
- Mercurial keeps the supersession record beside history, and shares it:
  "Unlike the previous way of handling such changes (which stripped the old
  changesets from the repository), obsolescence markers can be propagated
  between repositories." A marker holds "potential successors for a given
  changeset, the moment the changeset was marked as obsolete, and the user who
  performed the rewriting operation." (Mercurial wiki, "ChangesetEvolution",
  https://www.mercurial-scm.org/wiki/ChangesetEvolution)

**3. What they changed or regretted.**

- When part of a record sits outside the hashed content, two different records
  clash: "Because we use the Git Object ID as commit ID, two commits that
  differ only in their change ID, for example, will get the same commit ID, so
  we error out when trying to write the second one of them." (jj architecture)
- A field the old tools do not know is dropped silently: the change id header
  "is not preserved by all `git` tooling… preserved by a `git commit --amend`,
  but is not preserved through a rebase operation". (git-compatibility)
- Mercurial's wish list names what the markers should have carried: "Storing
  more information about what the type of rewrite in obsolescence markers would
  be useful." The feature is still off by default after about fourteen years,
  out of commitment "to backward compatibility". Three kinds of divergence had
  to be named and then renamed after real use. Marker exchange "can be very
  slow". (ChangesetEvolution and its developer page)

**4. Which questions.** (1) Keep a stable random id for the thing that evolves
and a separate id for each immutable state of it. REPORTED. If a content hash
is used, *everything* that distinguishes two records must be inside it.
REPORTED as a bug they live with. (6) Write "replaces" down, with who, when,
and what kind of replacement. REPORTED, the last as a wish. (12) Log each
operation with user, host, time and command. REPORTED. (17) Give the origin a
fixed, recognisable id that is the same in every store and is never stored.
REPORTED. (10) jj takes the opposite road to compare-and-set: accept both,
record the divergence, let the next reader resolve. REPORTED. It suits a
single person's repository; under machine-rate agents it would turn every
contended cell into a standing conflict. INFERRED. (13) An envelope field that
some writers do not understand gets lost unless preservation is a rule for all
writers. REPORTED.

**5–6.** Small, personal, offline. They matter for the shape of identity, not
for scale. jj disagrees with everyone on locking. Mercurial disagrees with
Fossil on whether supersession records should travel between stores.

### 3.3 Fossil (D. Richard Hipp; 2006–now)

**1. What he built and chose.** A version control system whose stated policy is
to keep everything and rewrite nothing, with one narrow, written exception.

**2. His reasons.** All from fossil-scm.org/home/doc/trunk/www/ (pages dated
2026-08-28).

- The format is meant to outlive its makers: "The global state of a fossil
  repository is kept simple so that it can endure in useful form for decades or
  centuries. A fossil repository is intended to be readable, searchable, and
  extensible by people not yet born." (fileformat)
- No sequence anywhere: "The global state of a fossil repository is an
  unordered set of artifacts." Each is "named by a hash of its content. No
  prefixes, suffixes, or other information is added to an artifact before the
  hash is computed." (same)
- Corrections are additions: "Fossil allows all of this not by removing or
  modifying existing repository entries, but rather by adding new supplemental
  records. Fossil keeps the original incorrect or unclear inputs and makes them
  readily accessible". Even who and when can be corrected this way: "The 'user'
  tag overrides the name of the check-in user. The 'date' tag overrides the
  check-in date." The list of uses includes "Fix faulty check-in date/times
  resulting from misconfigured system clocks." (rebaseharm; fileformat)
- Users and permissions are outside the permanent record: local state,
  including "authorized users", "is not versioned and is not synchronized with
  the global state" and is "not intended to be enduring." (fileformat)
- On rewriting: "Rebasing is an anti-pattern. It is dishonest. It deliberately
  omits historical information." (rebaseharm)

**3. What he changed or conceded.**

- *Shunning.* "Fossil is designed to keep all historical content forever.
  Fossil purposely makes it difficult for users to delete content …
  Nevertheless, there may occasionally arise legitimate reasons for deleting
  content." The mechanism: "Every Fossil repository maintains a list of the
  hash names of 'shunned' artifacts. Fossil will refuse to push or pull any
  shunned artifact." The order to delete does not travel, on purpose: "The fact
  that the shunning list does not propagate is a security feature. If the
  shunning list propagated then a malicious user (or a bug in the fossil code)
  might introduce a shun record that would propagate through all repositories
  in a network and permanently destroy vital information." The list outlives
  the content "to prevent the artifact from being reintroduced". (shunning)
- *The honest limit.* "if shunning and purging were removed from Fossil, you
  could still remove artifacts from the repository with SQL DELETE statements;
  the repository database file is, after all, directly modifiable… Where the
  Fossil philosophy really takes hold is in making it difficult to violate the
  integrity of the hash tree." (fossil-v-git)
- *Deleting a person, not their record.* Scrubbing removes a user and password,
  "However, in the DAG, commits by 'bertina' will continue to be visible
  unchanged even though there is no longer any such user in Fossil." (shunning)
- *The hash change that took days.* "Historical check-ins will keep their same
  historical SHA1 names. New check-ins will get more secure SHA3-256 hash
  names." "Fossil (version 2.0 and later) allows the SHA1 and SHA3 hashes to be
  mixed within the same repository… There is no need to 'convert' a
  repository". The stated motive: "it is a public relations problem. So the
  decision was made to migrate Fossil away from SHA1." Fossil 2.0 shipped eight
  days after SHAttered. (hashpolicy)
- The one place a writer's clock decides: "When two or more tags with the same
  name are applied to the same artifact, the tag with the latest (most recent)
  date is used." (fileformat)

**4. Which questions.** (0) Never rewritten is a property of the format and the
tools; the bytes can always be edited by whoever holds the disk. The promise is
to make integrity hard to break and every correction visible. REPORTED. (9)
Delete rarely, locally, by a kept list that also blocks re-entry; never let a
delete order spread by itself. REPORTED. (6), (7) Corrections are new records
that name their target; the wrong original stays readable. REPORTED. (1) Old
names keep their old scheme forever; new names may use a new scheme; one store
holds both. REPORTED. For Sid: if ids are random and scheme-free this whole
problem disappears; if a digest is carried, tag it with its algorithm and allow
more than one. INFERRED. (8), (16) Users and permissions are deliberately not
part of the enduring record. REPORTED. Sid makes the opposite choice (policies
are facts); Hipp's reason is that the list of users changes and should be
erasable. INFERRED. (13) Simple, self-describing text meant to be read without
the program. REPORTED.

**5–6.** The closest philosophy to Sid's never-rewrite rule, held for twenty
years by one careful person, with its exception written down. Small scale, and
everyone has a full copy. He disagrees with Git: "Git strives to record what
the development of a project should have looked like had there been no
mistakes. Fossil, in contrast, puts more emphasis on recording exactly what
happened, including all of the messy errors, dead-ends, experimental branches,
and so forth." He cites a commentator who "characterized Git as recording
history according to the victors, whereas Fossil records history as it actually
happened." (fossil-v-git) He disagrees with Mercurial on spreading
supersession records.

### 3.4 Dolt, and Noms before it (Tim Sehn, Aaron Son, Andy Arthur, Dhruv Sringari, Jason Fulghum; Noms by Aaron Boodman and Rafael Weinstein)

**1. What they built and chose.** A SQL database with Git's model. Rows live in
content-addressed trees whose shape does not depend on edit order. Commits are
named by hash. Branch, merge, diff and `AS OF` work on tables.

**2. Their reasons.** Row identity is the primary key: "Primary key values are
used to identify rows across versions for the purpose of diff and merge."
(docs, Conflicts) A point in time is named three ways: "The AS OF expression
must name a valid Dolt reference, such as a commit hash, branch name, or other
reference. Timestamp / date values are also supported." (docs, Querying
history) One writer at a time: "It is meant to be run on a single primary
server with multiple replicas for read scaling. If you need a primary bigger
than a single server, you must shard the data at the application layer."
(Sehn, "Why People Don't Use Dolt", 2024)

**3. What they changed or regretted.**

- *The format migration (2022).* They inherited Noms and its choices: "Building
  on top of Noms allowed us to get a head start on early versions of Dolt, but
  it also meant that we inherited its design decisions." (Arthur, "Migrating
  Dolt's Binary Format", 2022) Moving off it changed every name: "the migration
  changes all of the commit hashes because the storage format is different.
  Commit hashes are the output of a cryptographic hash function, so if a single
  bit changes, the commit hash will change." "During the job, we build a
  mapping of old commit hashes to new commit hashes." Forks were cut off; open
  pull requests were deleted; clones had to migrate too; a 5.7 GB database took
  about a hundred minutes. (Sringari, "Migrate your Dolt database to the new
  format on DoltHub", 2022-11-01) The method was a rebase of all history, with
  a check: "At a logical level, the data in each table is unchanged", and each
  table is "validated to ensure that they contain the same rows pre- and
  post-migration." Their own lesson: "It was pretty painful to build these
  mappings… A possible improvement here is to automatically encode this mapping
  during the migration." (Sringari, 2022-09-19)
- *Column tags: an id for a name.* The idea: "generate and use a unique
  identifier to represent that column. If the column is renamed, no big deal,
  the identifier stays the same." The verdict: "column tags seem like a classic
  premature optimization." Users did not understand them, so they were hidden
  and made random. Random broke merging, because "if two branches add the same
  column, they must receive the same column tag". So they became deterministic,
  which is still wrong: "it's not truly history independent… it can cause a
  problem if two columns, even in different tables, try to use the same column
  tag." The post ends: "The next time we talk about column tags will be when we
  deprecate them." (Sehn and Fulghum, "Column Tags", 2025-05-15,
  https://www.dolthub.com/blog/2025-05-15-column-tags/)
- *Counters do not survive copies.* Shared auto-increment across branches was
  added in 2022, "But, Dolt is a decentralized version control system so each
  individual instance of Dolt knows nothing of its clones." The advice became:
  "UUID keys produce conflict-free merges on branches and clones." (Sehn,
  "AUTO_INCREMENT vs UUID Primary Keys", 2023)
- *Deletion means rewriting.* "Deleting rows or dropping tables isn't
  sufficient, because the sensitive data will still be reachable from previous
  commits. What we need is a way to rewrite history and excise the data from
  all the commits." Then garbage-collect. (Arthur, "Filter-Branch in Dolt",
  2020) I found no Dolt post on erasure law as such.

**4. Which questions.** (1) If names come from bytes, a new byte format renames
everything, and every outside reference breaks unless a mapping is kept.
REPORTED. (13) Separate the logical fact from its encoding, and be able to
prove they match after a re-encode. REPORTED as practice. (3) An id for a
"kind of column" was their most regretted feature. REPORTED. The cause was
independent minting on branches that must later agree. One gate removes that
cause inside one store; layers and a second store bring it back. INFERRED. (2)
Counters fail wherever there is more than one place that can create; random ids
do not. REPORTED. (10) One primary, and sharding is the application's problem.
REPORTED. (9) Excision is a history rewrite followed by garbage collection.
REPORTED.

**5–6.** Dolt's rows are mutable, its history is for audit and merge, and its
users run their own copies. The resemblance is a fact-like store with names
derived from content and a single writer. They disagree with Git's partial
clone design, and with their own inheritance from Noms.

### 3.5 Irmin (Thomas Gazagnaire, Anil Madhavapeddy, the Tarides team; 2013–now)

**1–2. What they built and why.** A Git-like store as an OCaml library:
content-addressed values, commits with parents, branches as the only mutable
part, and merge functions supplied by the user. "keys are deterministically
computed from the values", and "The second backend store is the tag store: this
is the only mutable part of the system." The founding assumption on growth: "As
the store is append-only, there is no remove function. The store is expected to
grow forever, but garbage-collection and compression techniques can be used to
manage its growth. This is not an issue as commodity storage steadily becomes
more and more inexpensive." (Farinier, Gazagnaire, Madhavapeddy, "Mergeable
persistent data structures", JFLA 2015, https://gazagnaire.org/pub/FGM15.pdf)

**3. What they changed or regretted.** Irmin became the storage under Tezos.

- *The encoding froze.* "The Tezos context comes with a specific context hash
  function that cannot be changed. Otherwise, the replicated consistency would
  not be maintained." (Octez docs, "The storage layer",
  https://octez.tezos.com/docs/shell/storage.html) The encoding became its own
  published spec, marked stable.
- *Hash addresses were too slow.* "content-addressing bottlenecks transaction
  throughput… each read requires consulting the index, and each write requires
  adding a new entry to it." So they added "object addresses that are not
  hashes", after which "the index can be shrunk by a factor of 360 (from 21G to
  59MB)". (Tarides, "Lightning Fast with Irmin", 2022-04-26)
- *Growth was an issue after all.* "As rolling nodes execute, the pack file
  grows larger and larger, and no old data is discarded." "it is impossible to
  'delete' regions corresponding to dead objects and reclaim the space."
  Operators worked around it by exporting a snapshot, deleting everything, and
  importing it back. Garbage collection arrived in 2022. (Tarides, "Towards
  Minimal Disk-Usage for Tezos Bakers", 2022-11-10)
- *The on-disk format is at version five.* One step "is not
  backwards-compatible with existing stores… It is not forwards compatible."
  (mirage/irmin CHANGES.md)
- *An outside retrospective:* "it tries to do too many things and comes up
  short on some of them in ways that really matter." And on types: "you can
  only store values of a single type", with two workarounds: serialise
  everything, or grow one giant variant. (Patrick Ferris, "Irmin
  Retrospective", 2025, https://patrick.sirref.org/irmin-retro/)

**4. Which questions.** (1) A hash that others verify can never change, and the
encoding under it becomes a frozen public spec. REPORTED. Hash addressing costs
an index on every read and write; they kept hashes for the few things outsiders
must verify and used plain pointers inside. REPORTED. (13), (0) "Storage is
cheap" lasted seven years. Plan the trim boundary before the first record.
REPORTED fact, INFERRED advice. (3) One value type per store forces either
opaque blobs or one giant union. REPORTED. Sid's "grammar looked up by key" is
the third way; the retrospective suggests the need is real. INFERRED.

**5–6.** Irmin under Tezos is a never-rewrite store with many verifiers, run
for years. It differs in that its history is pruned by design in most nodes. I
found no retrospective by Gazagnaire or Madhavapeddy.

### 3.6 Pijul (Pierre-Étienne Meunier, Florent Becker; 2015–now)

**1. What they built and chose.** Version control on a theory of patches. A
change is named by the hash of its content, which includes its dependencies.
Independent changes commute. A repository state is an unordered set of changes.

**2. Their reasons.**

- A thing is named by the event that made it: "vertices are uniquely
  identified, by the hash of the change that introduced them, along with a
  position in that change. This means that two lines of text with the same
  content, introduced by different changes, will be different. It also means
  that a line keeps its identity, even if the change is applied in a totally
  different context." (manual, Theory, https://pijul.org/manual/theory.html)
- Deleting is labelling: "this system is append-only, in the sense that
  deletions are handled by a more sophisticated labelling of the edges". (same)
- Dependencies are the minimum needed to make sense of the change, and tools
  may add more: "this is just the minimal set of dependencies needed to make
  sense of the text edits. Hooks and scripts may add extra language-dependent
  dependencies based on semantics." (same)
- A version has no order: "versions are essentially unordered sets of patches."
  Its id is order-independent, and the empty repository has a fixed one: "The
  version identifier of an empty repository is always the identity element 1".
  (FAQ; Theory)
- Authors are keys, names are elsewhere: identities give "greater security and
  control than simply mapping authors to a name and email address. The
  inclusion of a name and email address can be spoofed in a way that a key
  signature cannot." And names may change later, because they are "no longer
  tied to their submitted patches". (manual, Pijul identities)

**3. What they changed or regretted.** The 1.0 rewrite (Meunier, "Towards 1.0",
2020-11-07, https://pijul.org/posts/2020-11-07-towards-1.0).

- *Two kinds of link.* "Changes now have two different sets of dependencies:
  one is the set of strict dependencies, which are change we require in order
  to apply the current change, while the other one is merely a set of 'known'
  changes, which the apply algorithm checks to decide whether to mark a
  conflict or not."
- *Header apart from contents.* "one contains a header with metadata, the
  dependencies, known patches, edits, and a cryptographic hash of the contents.
  The other section contains the contents… This division allow a server and a
  client to skip contents that isn't alive anymore, while allowing the client
  to retrieve and verify the contents at a later time".
- *The break.* "Certainly the most breaking change of them all is the new patch
  format… any repository started before now would have become obsolete in a
  matter of days."
- *Authors.* "The 'author' field in patches now gives you the choice between a
  simple free-format string, or your public key, and a mapping between public
  keys and identities is stored in repositories". He adds that author names
  "weren't initially the main focus of the project". ("Two changes to changes",
  2021)
- *Removal is local.* "A change can only be unrecorded if all changes that
  depend on it are also unrecorded in the same operation." (unrecord reference)

**4. Which questions.** (4) The direct answer in this camp: split what a record
*requires* from what it merely *knew of*, and they learned to do so after
shipping one undivided set. REPORTED. (2) Name a new thing by the record that
introduced it, plus a position. REPORTED. For Sid: an entity id could be "the
id of the offer that first mentioned it, plus an index", which makes clashes
impossible without randomness and leaks nothing by inspection. INFERRED.
(1), (9) Hash the header; put the hash of the contents in the header; let the
contents be absent. REPORTED. (8) The actor on the record is a key; the name
is a changeable mapping outside the hashed record. REPORTED. (10) A version is
a set, and its id must not depend on order. REPORTED. Under one gate this is
unnecessary inside the store and becomes relevant only between stores.
INFERRED. (17) The empty state has a fixed id. REPORTED.

**5–6.** Pijul's unit is a text edit by a trusted author. Its value for Sid is
the clean split of envelope from contents and of "requires" from "knew". It
disagrees with Git on merge ("Git doesn't guarantee the associative change
property") and on identity (cherry-picking "losing their identity"), and with
Darcs on commuting conflicts.

---

## 4. Question by question

Sid's numbers, in Sid's order. Each entry gives what the camp says (with who,
and the mark), where they split, and then **"Before the first record"**: what I
think follows for Sid. Everything under that heading is INFERRED and is my
position, with its tradeoffs. Quotes here are short pointers; the full passages
and sources are in sections 2 and 3.

### (0) The log: never rewritten, or only never lost?

**What the camp says.**

- Hipp (Fossil) holds never-rewrite as policy for twenty years, and writes down
  its limit: the bytes are always editable by whoever holds the disk, so the
  real promise is "making it difficult to violate the integrity of the hash
  tree". He keeps one narrow exception, shunning. REPORTED.
- Meyer (Bamboo) withdrew the term: "append-or-delete log". Willow:
  "designing systems for unbounded growth tends to backfire sooner or later."
  REPORTED.
- Kleppmann, in *Designing Data-Intensive Applications* (2017, ch. 11): for
  some data "it's not sufficient to just append another event to the log to
  indicate that the prior data should be considered deleted — you actually want
  to rewrite history and pretend that the data was never written in the first
  place. For example, Datomic calls this feature excision and the Fossil
  version control system has a similar concept called shunning." REPORTED.
- Upwelling's authors call full history "a problem caused by keeping too much
  history". REPORTED.
- Bayou's log held all writes "conceptually" and was truncated in practice,
  with a vector that records what was omitted. REPORTED.
- Irmin assumed growth "is not an issue" (2015) and retrofitted garbage
  collection (2022). REPORTED.
- Dolt physically rewrote every commit and proved the logical rows unchanged.
  REPORTED.
- AT Protocol dropped its commit chain, made relays non-archival, and once
  removed rows from its append-only identity log. REPORTED.
- Figma: "never lost" is a measured number (sixty seconds, then under one).
  REPORTED.
- Gentle: keep the original events and what they stood on; never persist what
  an algorithm derived from them. REPORTED.

**Where they split.** Kleppmann and Automerge: keep everything, make it cheap.
Willow and AT Protocol: deletion is a right, so do not build proofs of
completeness. Hipp: keep everything, with a written exception.

**Before the first record.**

1. State the promise in logical terms. An admitted fact is never changed and
   never silently absent. Its value may be removed by a recorded erasure. Its
   physical encoding may change.
2. So nothing in the envelope may be computed from physical bytes (see (1)).
3. Decide what the gate's "yes" means physically: durable on how many machines
   before the answer is given. Figma's history shows that this number exists
   whether or not it is chosen.
4. If anything is ever trimmed, the store must say what is missing (Bayou's
   omitted vector). Otherwise absence lies.

The tradeoff: a logical promise is weaker than "these bytes are forever". It is
the only one anyone in this camp kept.

### (2) Entity: how is an id made so two never clash? May it give away when or where?

**What the camp says.**

- The maker mints the id, because creation is concurrent and later work must
  point at the new thing before the server answers. Figma, Weidner, Linear,
  Dolt: REPORTED. Convex is moving there after years of server-only ids:
  REPORTED.
- How many bits: Jahns argued fewer suffice when only clients are random, kept
  32 bits for compatibility, moved to 53. A clash without an authority means a
  document "permanently corrupted without a way to recover". REPORTED.
- A thing can be named by the event that made it, plus a position: Automerge
  object ids, Pijul vertices. REPORTED. Bayou's variant (creator's id plus
  stamp) grows with each generation. REPORTED.
- Leaking on purpose: AT Protocol's time-ordered keys exist for tree locality
  and must "not be trusted"; adversaries can choose them. Convex leaks the day,
  a format version, and (now regretted) the table. tldraw puts the type in the
  id for safety. REPORTED.
- Against leaking: PushPin keeps the type in the reference, "because the same
  document content may be rendered differently in different contexts". Keyhive:
  by default, knowing an id is permission to write. Linear's one counter shows
  every tenant the global write rate. REPORTED facts.
- jj prints its random ids in a different alphabet from its hashes so that one
  kind of id is never mistaken for another. REPORTED.

**Where they split.** Locality in the id (AT Protocol puts time first so
inserts append; Convex puts randomness first so writes spread across shards)
versus opaque ids (Keyhive, PushPin, Weidner). Note that the two "locality"
teams want opposite layouts. That is the sign that it is a storage concern.

**Before the first record.**

1. The maker mints; the gate refuses a clash at admission. No peer-to-peer
   system can do the second half. It turns Yjs's silent corruption into an
   ordinary refusal.
2. Either at least 122 random bits, or "id of the creating offer plus an
   index" (the Pijul and Automerge shape). The second gives uniqueness by
   construction and reveals nothing by inspection.
3. No time, no place, no type, no shard in the id. Whatever can be read from an
   id will be relied on. If storage wants order, solve it below the id.
4. Do put a *scheme marker* in the written form of an id (Convex's format
   version; Git's lesson that an id quoted in free text does not say which
   scheme it belongs to; jj's alphabet). A scheme marker says nothing about
   when or where.
5. Knowing an id must never grant anything.

The tradeoff: opaque random ids cost storage locality and debuggability.
Rama can partition by hash of id; people can be given names (facts) to read.

### (17) The first facts

**What the camp says.**

- Give the origin a fixed, defined id. jj: all zeros and all z's, a "virtual
  commit". Automerge: the root map has "a `null` actor id and `null` counter".
  Pijul: "the identity element 1". PPPPP: a first message "deterministically
  predictable and empty, so to allow others to pre-know its msg ID". Matrix:
  the create event is the one whose authorisation list is empty. REPORTED and
  INSTITUTIONAL.
- Do not derive the first record's id from code or content. Kleppmann: "it
  would be a very fragile API that is easy to use incorrectly." did:plc: ids
  that hash a legacy first operation "will unfortunately be around forever".
  Croquet: tying identity to a code hash made old state "inaccessible" after
  any code change. REPORTED.
- The language of grammars sits outside the system. AT Protocol: "an
  intentional decision to not express the Lexicon schema language itself
  recursively". Cambria names the same hole ("how Cambria's own data might be
  versioned"). REPORTED.
- Bayou: a server's creation is an ordinary write, and the first server is a
  fixed base case. REPORTED.
- Frazee: "PLC stands for 'Placeholder'". It is now permanent. REPORTED.

**Before the first record.**

1. Fixed, published ids for the base layer, the gate as actor, the bootstrap
   actor, the key that means "this is a key", the key that holds a grammar, and
   the first policy. The same in every store, so two stores share a vocabulary
   from birth.
2. One fact that is *different* in every store: the store's own id. A second
   store's gate numbers are then never confused with the first's (did:plc:
   a replica "could plausibly assign different sequence numbers").
3. The first facts are written by the bootstrap actor and their "checked
   against" list is empty, as in Matrix. They are axioms. The record should
   say so rather than pretend the gate checked them.
4. The grammar of grammars is the one thing the runtime must know without
   looking it up. Keep it tiny, and version it (Cambria's "lens inception").
5. Anything named "placeholder" at genesis should be assumed permanent.

### (3) Key: a word, or an id with its name and shape as facts?

**What the camp says.**

- Words with an owner and additive-only evolution: AT Protocol. Names collide
  ("unintentionally clobbered unspecced uses of that field"). Fields "can not
  be renamed". Frazee: the name means what the software that writes it means.
  Newbold: there is no schema version for a record to pin. REPORTED.
- Numbers with a registry: Nostr. Storage behaviour is encoded in number
  ranges; dead numbers are listed forever; fiatjaf wants the registry to be
  only a registry. REPORTED.
- Bare words: SSB; tldraw's flat namespace that "must not collide". REPORTED.
- An id standing for a name: Dolt's column tags, "a classic premature
  optimization", because two branches must independently arrive at the same
  id. REPORTED.
- A readable name plus the id of the exact grammar version: p2panda. REPORTED.
- Tag each write with the grammar it was written under; translate on read;
  keep translators as data: Cambria. Unshipped after five years. REPORTED.
- The client states its schema version on every exchange: Replicache. Up and
  down migrations so mixed versions can work together: tldraw. REPORTED.
- One schema enforced over all existing data: Convex. REPORTED. Not available
  to a store that never rewrites.
- A gate that does not know a grammar lets the record through ("Fail-Open"),
  and should not fetch grammars "in the middle of processing record creation":
  AT Protocol. REPORTED.
- One value type per store forces blobs or one giant union: the Irmin
  retrospective. REPORTED.

**Where they split.** Words (the majority practice) against ids (Dolt tried and
regrets it; p2panda combines both).

**Before the first record.**

1. The non-negotiable part: each fact, or the gate's verdict on it, names the
   exact grammar version it was admitted under. Every system here either has
   this or says it misses it.
2. Within a key, only additive, optional change. Anything else is a new key.
   AT Protocol's rules are not a style choice for Sid; they are forced, because
   old facts can never be made to pass a new grammar.
3. My position on word or id: an id, with the name as a fact. Reason: under
   never-rewrite a word in stored facts can never be renamed, and AT Protocol
   shows that names do need to change. Dolt's failure had one cause,
   independent minting that must later converge, and one gate removes it inside
   one store: registering a name in the base layer is a compare-and-set.
4. The costs of that position. A pattern written with a name must resolve it at
   some position, so name lookup is itself a read and belongs in based-on. Two
   people defining "deadline" in their own layers make two keys until someone
   states they are the same; promotion to base must handle that. A second store
   brings Dolt's problem back in full; p2panda's "name plus definition id" is
   the known answer there. Debugging raw facts gets harder.
5. How a kind is kept (forever, replaced, not stored) is a property of the kind
   (Nostr's ranges, tldraw's scopes). Put it in the grammar fact now, because
   it decides what the store promises.
6. The gate reads grammar and policy facts on every admission. That is a hot
   path through the thing it guards. AT Protocol chose not to do that.

### (9) Value: never removed, so how is one deleted, backups included?

**What the camp says.**

- Separate the value from the envelope. Matrix redaction (in production since
  2019) strips everything except a fixed list of envelope keys, and the event
  id is a hash of that stripped form, so it survives. Bamboo, Gabby Grove,
  PPPPP and Pijul each hash the envelope and put only the *hash of the
  contents* inside it. REPORTED and INSTITUTIONAL.
- Delete rarely, locally, by a kept list that also blocks re-entry, and never
  let the order spread by itself: Fossil. Nostr agrees on the list ("MUST
  ensure the deleted events cannot be re-broadcasted") and spreads requests
  forever. REPORTED.
- What was read cannot be unread: Keyhive, the local-first essay. REPORTED.
- A delete event that does not say what it deleted forces every consumer to
  hold the prior state: AT Protocol (issue closed as not planned). REPORTED.
- A global version number forces soft deletes; per-row versions allow real
  ones: Replicache. REPORTED.
- The id can outlive the value: Weidner. Convex stamps the day into ids so that
  reuse can be banned "without having to keep deleted IDs around forever".
  REPORTED.
- Rotate first; rewriting is loud and cannot reach copies: GitHub's guidance.
  REPORTED.
- Backups "are often deliberately immutable"; deletion "is more a matter of
  'making it harder to retrieve the data'": Kleppmann. REPORTED.
- Automerge and Upwelling cannot delete and say so. REPORTED.

**Before the first record.**

1. Envelope and value are separate physical things from the first fact. The
   envelope may hold a tagged digest of the value, never the value's bytes in
   a form that a hash chain depends on.
2. An erasure is itself a fact: by whom, under which policy, because of what.
   It destroys the value, keeps the envelope, and leaves a do-not-readmit mark
   keyed by the fact's id. (A mark keyed by the hash of a short value, such as
   a name, can be reversed by guessing. My note, not a source's.)
3. Crossings should record reads, not rendered copies. Patchwork's "minimal
   set" does exactly this. Then an erasure does not have to chase copies
   through "what was shown".
4. Facts that are not re-derivable and stood on the erased value (a model's
   reply, a summary) are found by walking based-on. What to do with them is
   policy. They are findable only because reads were recorded. This is where
   Sid's design beats everyone in this camp.
5. Backups hold copies of values. Either they expire on a known horizon, or the
   value is stored so that destroying one thing destroys every copy. The second
   is the crypto-shredding question, which moved to research 7.
6. Between stores a delete is a request plus an audit trail. No one here found
   more.

### (8) By whom: who checks it? Is an agent itself, or the person? Where is "acts for"?

**What the camp says.**

- A replica is not an author. Weidner said so of his own earlier posts. Yjs and
  Automerge recorded replicas only and are both adding authorship in 2026; Yjs's
  first bolt-on kept a parallel log per user and answered by scanning.
  REPORTED.
- Two actors on a record is normal: Git's author and committer; Bayou's
  accepting server and user. REPORTED (Bayou); background knowledge (Git).
- Acting *as* the person loses the actor: Nostr remote signing, Bayou's
  applications holding the user's keys, AT Protocol's custodial hosts.
  REPORTED.
- Delegation written on every record failed in Nostr because every reader had
  to understand it. REPORTED. PPPPP writes the identity version and scoped
  powers instead. Matrix writes the authorising events onto each event.
  REPORTED and INSTITUTIONAL.
- A person is a group of keys, so records name the person and not the device:
  Keyhive. REPORTED.
- The author on the record is a key; the name is a changeable mapping kept
  outside: Pijul. Fossil lets a later record override the user, and lets a user
  be erased while their commits remain. REPORTED.
- An agent is its own collaborator; its edits show as its own; its definition
  is a versioned record: Patchwork. REPORTED.
- The server authenticates; that is where rejection can live: Boodman.
  REPORTED. Signatures age: "With key rotation, verification of older commit
  signatures can become ambiguous" (AT Protocol). INSTITUTIONAL.
- Tarr: the author belongs on every entry. REPORTED.

**Before the first record.**

1. By-whom is the thing that acted: this agent, this model, this tool, this
   lane, this person. Never the person on whose behalf.
2. "Acts for" is a grant fact. The acting fact stands on the grant at its
   version (a based-on entry), and the gate's verdict names the grant it
   checked. One store makes Nostr's reader burden disappear: the gate resolves
   delegation once, at admission.
3. The actor's definition (model, prompt, tool body) must be findable as of
   that version.
4. Do not mint a new long-lived actor per agent run. Automerge shows what
   thousands of short-lived actors cost. A run is a session of a standing
   actor.
5. A human name is a fact about an actor entity. It can change and can be
   erased.
6. Who checks: the gate, against the session's credential. Signatures matter
   only when facts leave the store's trust, and they do not stay verifiable
   forever.

### (11) When: whose clock? Ever used for order?

**What the camp says.** With one orderer, order needs no clock: Figma, Convex,
Weidner, Linear. REPORTED. Keep two times when two exist: Git (author,
committer), Convex (a commit clock, and a display time that "has different
guarantees compared to the commit timestamp"), AT Protocol (client and
indexer), SSB (claimed and received).
REPORTED. A usable wall clock is repaired against causality: Git's corrected
commit date; Convex's hybrid logical clock. REPORTED. Author clocks that decide
winners can be gamed: Nostr. REPORTED. "There is not necessarily an underlying
objective truth as to which operation occurs 'first'": Keyhive. REPORTED.
Fossil lets a later record correct a date. REPORTED. Newbold files timestamps
under "Unfortunate", permanently. REPORTED.

**Before the first record.** "When" is the gate's admission time. It is never
used for order; the position is. Make it monotone along reads (the later of
the wall clock and one tick past the latest "when" among the facts read), so
it can never contradict based-on. A claimed time of the act (published in
1998; on screen at 10:03:07 by the person's machine) is a value under its own
key, not part of the envelope. If several gate instances ever exist, their
clocks agree only roughly, which is one more reason.

### (16) Layer: who sees a fact before any permissions exist?

**What the camp says.** Without a server, knowing the id is the permission
(Keyhive, PushPin), and better than that is "an open research question" (the
local-first essay). REPORTED. AT Protocol began public and is adding private
data four years in. REPORTED. Things an enforcer needs must be visible to it:
blocks are public "because every protocol-conforming App View needs to know who
is blocking who". INSTITUTIONAL. A single global version makes per-reader
permission hard; per-row versions make it natural: Replicache. REPORTED.
Upwelling's drafts are private to one writer or open to all. REPORTED. tldraw
gave comments their own permissioned partition. REPORTED. Fossil keeps users
and permissions out of the enduring record. REPORTED.

**Before the first record.** The layer is the first permission. Visibility is
structural and default-deny: a session layer is seen by that session, a
personal layer by that person and the agents they grant, the base by all. The
first policy facts are part of genesis (17). One global position counter leaks
activity across layers (Linear), so positions should be per partition. A base
fact whose based-on points into a private layer must show, to a reader without
rights, that something is hidden and not what. The gate sees everything; a
matched tool sees what its grant allows.

### (10) Order: can two gates write one layer? What must share a partition? Is "as of" one number?

**What the camp says.**

- One sequencer per ordering domain. The domain is the natural unit of joint
  work: a file (Figma), a document (Weidner), a repository (AT Protocol), a
  session (Croquet), a whole database (Linear, Convex, Dolt). REPORTED.
- Those who assumed one global order regret it or document its ceiling:
  Figma's LiveGraph; Replicache's global version. REPORTED.
- The position handed out should be opaque: Replicache's cookie. REPORTED.
- A moment can be named three ways: a vector (large, universal), a frontier
  (small, needs shared history), a local number (local only): Gentle. REPORTED.
- With one writer and no tentative state, a version is one integer per domain;
  tentative state forces vectors: Weidner. REPORTED.
- Build a whole screen at one position: Convex. REPORTED.
- Two writers on one log is a fork: SSB. jj accepts divergence and records it.
  Upwelling wanted a server to approve merges one at a time. REPORTED.
- "they never transact on the same primary records": Frazee's rule for
  partitioning by owner. REPORTED.

**Before the first record.**

1. The unit of compare-and-set is the cell. All versions of a cell come from
   one sequencer. A cell lives in one partition for life.
2. What else must share that partition is whatever must be checked together
   with it: invariants across cells (Figma's no-cycles check; a name unique in
   base). List them before the first record. They decide partitioning, and
   the brief's three checks have no place for them yet.
3. "As of" is an opaque token that only the store can compare. It may hold one
   number today and many later. Nothing outside the store does arithmetic on
   it. This is the cheapest protection against Figma's "deeply baked".
4. A crossing records the one token its screen or prompt was built at.
5. A layer is not a partition. Two gate instances may write one layer if they
   own different cells. Personal and session layers are natural single-owner
   partitions (Frazee's rule). The base is not.
6. Bayou's alternative to bare compare-and-set deserves a look under
   machine-rate contention: the offer carries a check (a "query and its
   expected result") and its own fallback. Sid's expected version is the
   simplest case of that.

### (11) Based on: is every read listed, including reads that only made a tool fire, and entries the runtime fills in for a reply?

**What the camp says.**

- Reads are captured by the runtime, never declared. Convex: "we record the
  index range we scanned in the transaction's read set." Eg-walker: "the
  previous frontier … becomes the new event's parents." REPORTED.
- Record the *range*, not the rows, so that a later arrival inside the range
  counts as a change: Convex. REPORTED.
- The exact dependency set of a read can be defined ("a smallest set that is
  'enough' to completely determine the result") and returned by the server,
  and literal sets of ids "could get large", so Bayou compressed them.
  REPORTED.
- List the frontier, not everything reachable from it: Kleppmann. Assume the
  obvious single parent and list only exceptions: Gentle. REPORTED.
- A record of what a client was sent can be throwaway if it can be rebuilt
  (Replicache's CVR), or durable if it is the person's own history
  (Patchwork's Account History). REPORTED.

**Before the first record.**

1. The runtime fills in based-on. An actor's own list of what it read is a
   claim. A list the runtime captured is a record. Only the second can carry
   "the map must not lie".
2. Sid's three kinds of read are enough. The pattern read must also carry the
   position token (see (5)) and the *viewpoint*: the stack of layers it was
   evaluated under. The same pattern under a different stack is a different
   read.
3. The read that only made a tool fire is the because-of. Do not list it twice.
4. For a model's reply, based-on is one pointer to the crossing that fed the
   model. The crossing lists the reads. Do not copy them.
5. At machine rate, based-on is most of the bytes. Compress the common case the
   way Eg-walker does, and point at crossings the way (4) says.

The tradeoff the camp names: a complete literal read list is the honest record
and does not scale (Bayou). A predicate plus a position scales and is honest
only if the index position is truthful.

### (4) Based on: does each read say whether the fact depends on it? Follow the latest, or stay on the version read?

**What the camp says.**

- Pijul split one set into "strict dependencies" and "merely a set of 'known'
  changes", after shipping the undivided set. REPORTED.
- Automerge keeps what I had seen (`deps`) apart from what I overwrite
  (`preds`), and Kleppmann holds that the second "are not redundant".
  REPORTED.
- For a deterministic function every recorded read is a real dependency by
  construction: Convex. INFERRED from their design.
- Floating or pinned is a property of the *kind of reference*: p2panda's
  "Relations" and "Pinned relations" are field types; AT Protocol's plain URI
  floats and its "strong reference" pins. REPORTED and INSTITUTIONAL.
- Patchwork saves the pinned heads and still reopens the latest, "because that
  is the expected user experience". REPORTED.
- Mercurial's developers wish their markers had recorded what kind of rewrite
  took place. REPORTED.

**Before the first record.**

1. A provenance read always stays on the version read. It is a statement about
   the past. "Stale" is then computed: the pinned version is no longer current.
2. Each read needs a role written when the fact is made, because it cannot be
   worked out later. Two roles carry most of the weight. *Requires*: staleness
   flows through it. *Was exposed to*: doubt may flow; staleness need not.
3. The role often follows from the actor. A deterministic tool requires all its
   reads. A model or a person was exposed to theirs, unless they say more (a
   person citing a source makes that read a requirement).
4. Whether a *value* that points at another entity floats or pins is set by the
   key's grammar, not decided read by read.
5. Two more relations stay separate from both: what this fact replaces (6) and
   what started the work (because-of).

### (5) Based on: if an index was behind the log, is how far it had got written down?

**What the camp says.** Bayou "stably records the unique identifier of the last
Write reflected in the Tuple Store checkpoint". REPORTED. Croquet names the gap
("backlog") and covers the scene with an overlay when it is large. REPORTED.
Figma's LiveGraph does not stamp reads, so "there is no way to tell whether the
result is from before or after the invalidation", and it re-fetches every time.
REPORTED. AT Protocol reports lag in an HTTP header and still debates whether a
server is "read-sticky"; its fix for identity reads is that "clients know what
version of the DID document to expect". REPORTED. Bayou's session guarantees
are the general form: a session remembers what it has read and written and
refuses a server that is behind it. REPORTED. Git's side index now exists in
two generations that readers must tell apart. REPORTED. PPPPP had to store a
number that used to be derivable once pruning existed. REPORTED.

**Before the first record.** Yes. The position on a pattern read is the
*index's* position, not the log's head. Otherwise a fact that was in the log
but not yet in the index will be counted as seen, and staleness will be
computed against a moment the reader never saw. A session also carries its own
high-water token, so it never reads from an index that is behind its own last
admitted fact. And lag that is large is shown, as Croquet shows it.

### (11) Because of: always filled; empty only when it starts a chain?

**What the camp says.** jj makes the origin a real, fixed thing, so every
commit has a parent and no code handles "none". REPORTED. Matrix's create event
is the one record with an empty authorisation list. INSTITUTIONAL. Croquet
records only what came from outside; everything else follows from it. REPORTED.
jj's operation log keeps the command and its arguments for every operation;
Mercurial wishes it had kept the kind. REPORTED.

**Before the first record.** Two honest options. *Empty means a chain starts.*
Simple, and "find all chain starts" is a scan for emptiness. *Never empty.* A
chain that starts from a person's hand points at the session fact (12). Then
every chain is tied to an actor's session and a runtime build for free, and
there is no null to handle. The cost is a small stretch of meaning: the session
did not cause the click. I lean to never empty, for the same reason jj does.
Either way, what a person was looking at when they acted is a based-on entry
(the crossing), not the because-of.

### (1) Version: is a fact pointed at by the gate's number, or by its own id? If its own: random, or from content?

**What the camp says.**

- Both exist in every system that lasted, and they name different things. jj:
  a random change id and a content commit id. AT Protocol: a slot name and a
  pinned version. did:plc: the server's number is "an annotation … and not an
  intrinsic property". diamond-types: a shared (agent, seq) id and a local
  order number that "other peers will end up with different" values for.
  Bayou: an id at first acceptance, a commit number later, with the slot
  holding infinity until then. REPORTED.
- The maker's id exists so that later work can point at a record before the
  server answers: Weidner. And so that a retry is safe and a client can tell
  which of its pending changes the server has seen: Replicache. REPORTED.
- Content-derived names, the case for: they need no authority and they expose
  a lying peer (Kleppmann on equivocation); Matrix moved to them because many
  servers were assigning clashing ids. REPORTED and INSTITUTIONAL.
- Content-derived names, the costs, all REPORTED: the encoding under them is
  frozen (SSB's JavaScript printing; Tezos "cannot be changed"; did:plc
  "around forever"; Dolt "changes all of the commit hashes"). They cost space
  (8 MB per 260k changes) and cost most on deletion. The writer cannot know its
  own record's name until the bytes are final (Automerge's frontend). Anything
  left outside the hash makes two records clash (jj). Every outside reference
  breaks on migration, and sidecar data keyed by the old name is orphaned
  (Git notes). Changing the algorithm takes a decade unless mixing was designed
  in (Git against Fossil). Parsers must be strict forever ("Postel's law does
  not hold here").
- Irmin kept hashes only for what outsiders must verify and used plain pointers
  inside. REPORTED.

**Before the first record.**

1. Both. The fact's *name* is its own id, minted by the offerer when the offer
   is made (random, or actor plus counter). The gate's number is its
   *position*. Inside the store, position is how you order and say "as of".
   Across stores and in anything written down outside, the name is how you
   point.
2. A structural reason the name cannot be a content hash of the whole fact: the
   fact contains "when" and "version", which only the gate knows. A hash the
   offerer can compute names the offer, not the admitted fact. A hash the gate
   computes cannot be cited before admission. Automerge lived exactly this
   problem between its frontend and backend.
3. Refusals settle it. A refused offer never gets a gate number. If refusals
   are kept (7), something else must name them. That something is the offer's
   own id.
4. If integrity is wanted, carry a digest as an *attribute*: tagged with its
   algorithm, computed over a specified canonical form of the logical envelope,
   with the hash of the value inside it and not the value (Matrix, Bamboo,
   Pijul). More than one algorithm may coexist (Fossil). Inside one trusted
   gate nobody needs it; it earns its place between stores and for outside
   audit.
5. The question this opens, which the camp cannot answer for Sid: may offer B
   cite offer A before A is admitted? If no, a chain of agent work serialises
   through the gate, one round trip per step. If yes, B names A's offer id, and
   the gate admits B only once A is admitted (Kleppmann: a missing dependency
   "simply results in the update … never being delivered"). Automerge's regret
   applies: such a record must wait, not be dropped.

### (6) Version: when 37 replaces 25, is "replacing 25" kept on 37?

**What the camp says.** Yes, from everyone who kept history. Automerge:
"Operations are stored with their predecessors in change chunks and with
successors in document chunks." p2panda: every operation has a `previous`.
Mercurial: a marker says which changeset, which successors, who, when, and they
wish it said what kind. jj keeps predecessors. Fossil's corrections name their
target. REPORTED. Those who did not keep it paid: Dolt rebuilt an old-to-new
mapping by hand ("pretty painful"); AT Protocol consumers cannot know what a
delete removed; Nostr "older versions MAY be discarded"; Linear rewrites the
base of a pending change on rebase. REPORTED.

**Before the first record.** Keep it. It is free: the offer already states the
version it expects to be current, and on admission that *is* the version
replaced. Write "none" explicitly for the first version in a cell; Bayou's
slot-with-infinity shows the value of a field that is always present. Keep
*superseding* (same cell) apart from *shadowing* (same entity and key, higher
layer). A shadowing fact should stand on the lower fact at its version, as a
based-on read. Upwelling found that when the base moves, every open layer must
be re-examined. That is only computable if each override recorded which base
version it overrode.

### (7) Beside each fact: is the gate's yes or no kept? Where? Are refusals kept?

**What the camp says.**

- What authorised a record can be written on the record: Matrix's
  `auth_events`. INSTITUTIONAL. A verdict should be decidable "only on the
  updates in before(u)", that is, from the record's own recorded past:
  Kleppmann. REPORTED.
- did:plc keeps rejected operations and lets replicas act as witnesses, and
  still had to remove invalid rows once. REPORTED.
- Nobody else keeps refusals. Nostr's is a wire message. Linear drops the
  transaction. Replicache logs an error and moves the counter on. Bayou leaves
  it to an application's "error log" by convention. REPORTED.
- A refusal should carry what the refused party needs to repair itself:
  Keyhive, for clocks. REPORTED.
- "an intent record (with a hash of the authorized content)" that blocks a
  publish on mismatch: Automerge, 2026. REPORTED.
- Later knowledge kept beside a record is keyed by the record's id, and is
  orphaned if the id changes: Git notes. REPORTED.
- Making sure every write is logged needed a single type that owns the state:
  Figma. REPORTED.

**Before the first record.**

1. A yes is the fact plus what was checked: the grammar version, the policy or
   grant version, the expected cell version. Three references. Put them in the
   envelope. Inline cannot be orphaned and does not double the write count; a
   sibling fact would do both.
2. A no is a refusal record named by the offer's id: who, which cell, which
   check failed, against which versions, and what would make it pass.
3. The offered *value* in a refusal need not be kept. It may be exactly what
   policy forbade, or a secret, or junk; did:plc shows that kept rejects
   sometimes have to be purged.
4. Decide the retention class of refusals now. With machine-rate agents
   contending on cells they can outnumber facts. The camp's default is not to
   keep them at all; only systems built for audit do.
5. A verdict should be re-checkable from recorded things alone.

### (12) Start of a session: is the runtime version and the kind of machine written down? Are rebuilds facts?

**What the camp says.** Croquet puts a hash of the code into the session id,
"a prerequisite for perfectly synchronized computation", and had to add a
code-free `persistentId` and a hand-written projection to survive code changes.
REPORTED. Bayou: determinism needs uniform resource limits. Convex: determinism
reaches promise scheduling. REPORTED. Figma proved its log by byte-identical
replay across hundreds of thousands of files. REPORTED. Patchwork logs the tool
with each open, and a colleague asks for the tool's version too. REPORTED. jj
logs user, host, time and command per operation. REPORTED. Replicache sends the
schema version both ways, and two generations coexist during migration.
REPORTED. Weidner: a replica identity belongs to one session and must not be
reused after a crash. REPORTED. Git's side index has two live generations.
REPORTED. Automerge: a first record tied to initialisation code is fragile.
REPORTED.

**Before the first record.** Yes. A session fact: the actor, the runtime build
(as an anchor outside the store, a git commit, which Sid's third kind of read
already allows), the kind of machine or client, the stack of layers, and the
envelope format it speaks. A rebuild is a fact. A crossing names its session,
so "re-derivable" always has a referent. One cheap addition deserves thought: a
crossing can carry a digest of what was rendered. Then, after the fiftieth
rebuild, "yesterday's view no longer re-derives to the same thing" is a
detectable fact and not a silent lie. Figma and Automerge both reach for a
digest at exactly this kind of boundary. The lesson from Croquet's regret runs
the other way and Sid already has it: never let the code version into the
*data's* identity.

### (14) The hand: which motions become facts by default? Is being shown the same as looking?

**What the camp says.** tldraw: pointer and selection are presence (shared
live, never saved); camera and current page are session state (kept locally,
not shared); only shapes are the document. REPORTED. PushPin: fast-changing
data "could swamp the CRDT with low-value information; there is no reason to
persist such updates", and its gap: "ephemeral data is not associated with a
particular CRDT state". REPORTED. Riffle stored all UI state, was "delighted",
and found that "any change to the layout of that ephemeral state forced a
migration". REPORTED. Croquet: view-to-view traffic is never serialised; smooth
motion is made up in the view. REPORTED. Nostr has kinds that relays do not
store. REPORTED. Patchwork logs what was *opened*, at which version, with which
tool. It does not log motion. REPORTED. Bayou marks the unconfirmed on screen
and carries the mark through queries. REPORTED.

**Before the first record.** Nobody in this camp persists pointer motion, and
the one team that persisted everything says why not. Pointing: not a fact by
default. Selecting: a fact when it is the operand of an offer (what was
selected when the person acted), or when it changes what is shown. Panning: a
fact only through the crossing it causes; a new viewport is a new "what was
shown". Being shown is not looking. Shown is something the runtime did, and the
runtime is its actor. Looking is a claim about a person; the hand is weak
evidence for it. Give them different keys and different actors. Every sample of
the hand must name the crossing it was made against; that is PushPin's admitted
gap. Hand facts need their own retention class (13).

### (15) A click: which tools may act in a person's name?

**What the camp says.** The record of a person's act is the named action and
its arguments; the server produces the effects: Zero ("a record of the mutator
having run with certain arguments"). REPORTED. An approval should bind to
exact content: Automerge's intent record. REPORTED. Agents propose on their own
branch; the person merges: Patchwork. REPORTED. Scopes people reached for
first: kind and time window (Nostr's delegation conditions; remote-signer
permissions per kind). REPORTED. Scoped powers with consent, recorded: PPPPP.
Attenuated delegation through groups: Keyhive. Signed delegation certificates:
Bayou. REPORTED. Acting as the person erases the tool from the record: Nostr
remote signing, Bayou, AT Protocol hosts. REPORTED.

**Before the first record.** A click is a fact by the person: the named action,
its operand, and the crossing it was made on. Tools matched by that fact act as
themselves, because of the click, under a grant scoped at least by key, layer
and time. Only two things are ever in the person's name: the click, and a
promotion the person makes. A click approves what was shown, not whatever is
current, so the gate should compare the crossing's position or digest at
admission. That is compare-and-set for intent, and it is the same move as the
expected version.

### (13) Storage: plain maps or classes? Ever trimmed? Backups?

**What the camp says.**

- Build for readers not yet born: Hipp. Keep even old brokenness so that data
  round-trips: Git. REPORTED.
- Every writer must keep what it does not understand: Automerge's unknown
  columns; jj's header lost through `git rebase` is the counter-example.
  REPORTED.
- One runtime's serialisation habits must not define the format. AT Protocol's
  nullable-and-optional rule was settled by "the idiomatic way to serialize
  data structures in golang". SSB's ids depend on one engine's JSON printing.
  Automerge's storage types "leaked through" its public surface. REPORTED.
- If anything is hashed, one canonical encoding, written in the spec, and a
  strict parser: Kleppmann; Nostr; PPPPP's move to RFC 8785; AT Protocol's
  move to a re-specified CBOR and its ban on floats. REPORTED.
- Value types differ across runtimes: a large integer "will silently lose
  precision when read in JavaScript". Jayakar. REPORTED.
- Trimming was always retrofitted by those who said never: Irmin. Bayou planned
  it from the start, with a record of what was omitted. REPORTED.
- The disaster-recovery artifact was the snapshot, not the log: Figma. A
  restored or re-encoded store is checked for logical equality: Figma, Dolt.
  REPORTED.
- How a kind is kept is a property of the kind: Nostr, tldraw. REPORTED.

**Before the first record.** Plain, self-describing, language-neutral data for
the logical fact. No class or struct of any one runtime defines it; the runtime
will be rebuilt hundreds of times and the facts must not notice. An explicit
envelope-format version on every fact. Every writer keeps fields it does not
understand; there is one gate, which makes this easy, but re-encoders and
exporters are writers too. Value types limited to what every runtime reads the
same way, with explicit rules for big integers and floats. Retention classes
per key, fixed in the grammar: facts forever; refusals; hand samples; perhaps
session layers. Whatever is trimmed leaves a record of the omission. A backup
is good only if a replay from it can be shown equal to the store.

---

## 5. The group

### 5.1 The voices that matter most, who I dropped, who is missing

**Three voices.**

1. **Doug Terry and the Bayou papers.** The nearest ancestor of Sid's whole
   shape: offers that carry an expected state, one orderer, a session that
   knows what it has read, and tentativeness that travels through queries into
   every result row. They also give a rare kind of evidence. They built the
   principled option, a commit-only view, and then reported that no application
   ever chose it. If Sid reads one set of papers, these.
2. **Sujay Jayakar (Convex).** The nearest living system to "based-on makes
   running answers recompute", under a sole writer. It shows that recorded
   reads can be ranges, captured by the runtime, and that one mechanism can
   serve both commit conflicts and live invalidation. It shows how far
   determinism has to reach. And it has a fresh reversal on what an id should
   know about its own kind.
3. **The signed-log veterans, read together: Bryan Newbold and Paul Frazee (AT
   Protocol) with Aljoscha Meyer (Bamboo, then Willow).** They lived with
   never-rewrite among strangers, at scale, and they publish their debts. They
   converge on four moves: the value apart from the envelope; no chain that
   proves completeness; the server's number as an annotation; and the hard
   truth that the first record's format is forever.

**Next.** Martin Kleppmann is the thread through six of these sources
(Automerge, the Byzantine-tolerance paper, the deletion passage in his book,
Upwelling, Eg-walker, the AT Protocol paper). On (1), (6), (9) and (13) his
reasoning is the most reconstructable of anyone's. D. Richard Hipp is the one
person who has kept a never-rewrite store for twenty years and written down its
exception. Pierre-Étienne Meunier gives the only direct answer to (4).

**Disagreements worth staging in round two.**

- Kleppmann (keep all history, make it cheap) against Meyer and Willow (no
  unbounded growth, traceless removal) against Hipp (keep all, shun rarely).
- Wallace, Boodman, Weidner (the server decides; CRDTs are overhead) against
  Ink & Switch (the server is "in a supporting role, not the source of truth").
- Git (never mix id schemes in one store) against Fossil (mix them forever).
- Mercurial (supersession records should travel) against Fossil (a delete
  order must not travel).
- jj (a write never fails; divergence is a stored value) against everyone with
  compare-and-set.
- tldraw (put the type in the id) against Convex in 2025 and PushPin (keep it
  out).
- Terry (show the unconfirmed, marked) against Sid's rule read literally.

**Dropped.** Linear as a voice (almost no primary prose; kept as evidence
through the endorsed reverse-engineering). Croquet's theory papers and Terry's
"Baseball" paper (could not open). Irmin as a design voice (kept as evidence on
frozen hashes and late garbage collection). tldraw beyond its scopes and
migrations. Noms (no retrospective found). Peritext and performance work on
Yjs.

**Missing, for the orchestrator to place.** These are from my background
knowledge; none was gathered or verified here.

- **Datomic (Rich Hickey).** Attributes are entities whose names are facts
  about them; transactions are entities; "as of"; excision. It is the
  production precedent for the id side of (3), and the counterweight to Dolt's
  regret. Probably another group's.
- **The provenance literature (W3C PROV).** It already distinguishes kinds of
  link between a thing and what it came from ("used", "was derived from", "was
  informed by") and has a relation for acting on behalf of another agent. That
  is (4) and (8) with a vocabulary. Nobody in my camp cites it.
- **Certificate Transparency, Trillian, Sigstore.** Append-only logs with
  witnesses and signed checkpoints: the model did:plc says it is moving
  toward. Bears on (0), (7) and the second store.
- **Differential dataflow and Materialize (Frank McSherry).** Positions as
  frontiers; recomputation when inputs move. Bears on (5) and (10).
- **cr-sqlite and ElectricSQL.** Per-cell versions in SQL; positions handed to
  clients.
- Rama itself (Nathan Marz); XTDB (two time axes); event-sourcing
  practitioners on versioning events; Pat Helland on immutability; hybrid
  logical clocks (Kulkarni and others); Hypercore and Dat; Git maintainers'
  first-person regrets (the list archive blocked my fetches).

### 5.2 What this camp would question above the table

Sid's list of what is open, item by item.

**"An append-only store of small facts as the one substance for everything."**
Figma found that one conflict rule did not fit all kinds of value: design
properties got last-writer-wins, and code text got a causal event graph, inside
one product. REPORTED. A cell under compare-and-set is a poor home for text
that two people and an agent are typing into; every keystroke contends for one
cell. The camp would ask what the fact-shape of that paragraph is. INFERRED.
Figma's server also enforces an invariant across many records (no cycles),
which the brief's gate has no slot for. REPORTED fact, INFERRED gap. tldraw
refuses the ambition outright: "not a general purpose real-time data solution".
Riffle tried one substance down to the scroll position and reported the
migrations. The Irmin retrospective: "tries to do too many things". On the
other side, Gentle's whole argument supports the substance: store what happened
and what it stood on, and nothing an algorithm made of it.

**"Tools, grammars, policies, and definitions being facts in the same store."**
Patchwork does it for agents ("Bots can be shared, edited, and versioned just
like regular documents") and likes it. REPORTED. AT Protocol keeps the language
of schemas outside on purpose, lets an unknown schema through, and will not
resolve schemas in the write path. REPORTED. Cambria names the regress. Frazee:
"Schemas are only interpretable in the context of working software", so a
tool's body as a fact says what the tool does only relative to a runtime. The
sharpest worry is one Hipp states for deletes and that applies to policy: a
mechanism that travels through the store can "permanently destroy vital
information", including the means of repair. A bad policy fact could stop the
gate from admitting the fact that fixes it. INFERRED.

**"One store for the planet with personal layers instead of personal
databases."** The local-first camp objects on ownership, offline work, and
longevity. Staltz: "I fundamentally don't trust any system that has an admin
with sudo powers." AT Protocol's middle road is personal signed repositories
that can change hosts, plus shared indexes. A layer inside one store has no
exit unless export is defined from the start, and an export is a small second
store. INFERRED. Frazee also gives the counter-argument, from having built the
alternative twice: "People won't sacrifice features for hypothetical
improvements." On operations: Figma's one ordered stream coupled everyone's
availability; Dolt says a single primary is the ceiling; Boodman says the
round trip is physics. One thing I did not find anywhere in AT Protocol's paper
or core specs is erasure law. A planet store meets differing national rules on
day one. INFERRED.

**"A fixed nine-part envelope both ends share forever."** Nobody's envelope
held. Git survived by keeping headers it does not understand; jj's new header
is already being dropped by older tools. Automerge requires unknown bytes to be
retained. AT Protocol has said it will "bend the rules of protocol stability",
and that one core stream will change "even if this breaks lexicon evolution
rules". fiatjaf's law says optional parts become mandatory, and Willow
faults Nostr's open tag list for "the exponential state space of arbitrary
combinations of tags". So the camp's advice is a version marker, a rule for
unknown parts, and very few versions, because every reader must read all of
them forever (Git, on hash schemes: "they will never go away, so they
accumulate"). Note also what this report's own section 4 asks the envelope to
hold beyond the nine: the offer's id, what it replaces, the grounds of the
verdict, the session, the envelope version, perhaps a digest. The camp would
predict that nine is already not the number. INFERRED.

**"Running answers never stored, only crossings recorded."** For: Gentle ("We
never write the CRDT state to disk"); Figma's LiveGraph, which dropped
incremental maintenance once it saw that "most query results never change after
initial load". Against: Croquet snapshots because replay is too slow to join;
Automerge had documents that "hadn't loaded after 17 hours" before its runtime
changed; Convex lists full re-execution as a limit. I report these without
recommending stored derived state; this project treats that road as a reason to
stop and examine, and I agree. The precise challenge is different.
"Re-derivable" is a claim about reads, tool version, runtime version, and
resource limits together (Bayou, Convex, Croquet all say so in their own
terms). After many rebuilds the old runtime may no longer run. Then yesterday's
crossing can be *described* by its reads but not *re-rendered*. A digest on the
crossing makes the drift detectable. Nothing makes it re-renderable except
keeping old runtimes runnable. INFERRED.

**"Every read recorded as provenance on every fact."** Sid is alone here. No
system in this camp stores read sets durably per record. Convex holds them in
memory for live queries. Bayou gave up literal sets. The causal-graph systems
store what a record *stood on*, not what its maker *looked at*. The nearest
thing is Patchwork's Account History (2026), and it lives in the reader's own
document. That placement is the camp's privacy point: who read what is more
sensitive than who wrote what, and belongs in the reader's layer. The cost is
bytes. The payoff is large and nobody else has it: the reach of a deletion,
staleness, doubt, and "what was the model given" all fall out of it.

**"One writer."** Nearly everyone with a server agrees: Wallace, Boodman,
Weidner, Jayakar, Linear as observed; Upwelling wished for one; Keyhive
describes what a centre gives while declining to have one. The costs are all
REPORTED: a serialisation ceiling (Replicache), coupled availability (Figma), a
loss window that is never zero (Figma), a single-primary ceiling (Dolt), trust
in the operator (Staltz; did:plc's answer is replicas as witnesses). The camp's
consistent refinement: one writer per *ordering domain*, never one per planet.

### 5.3 Questions this camp would call the wrong question

1. **(0) "Never rewritten, or only never lost?"** The real axes are logical
   against physical, and whether integrity is chained through the bytes. It is
   the chaining that made deletion and migration impossible for SSB, Automerge
   and Git, not immutability as such.
2. **(1) "The gate's number or its own id?"** Both, and a third name for the
   cell. They are not alternatives.
3. **(10) "Can two gates ever write one layer?"** A layer is not the unit of
   order. Ask what the compare-and-set domain is and what the snapshot domain
   is.
4. **(11) "Is every read listed?"** Reads are not listed by anyone. They are
   captured by the runtime, as predicates with positions.
5. **(14) "Which motions become facts?"** The unit is not the motion. It is the
   crossing (what was shown) and the operand (what was selected when the person
   acted).
6. **(13) "Plain maps or classes?"** The question underneath is whether the
   logical fact is defined apart from every runtime.
7. **(11) "Whose clock?"** With one gate this is nearly settled. The live
   question is whether a second, claimed time is allowed, and the camp says yes,
   as a value.
8. **The premise of the table itself.** "Whatever is not written into a fact
   when it is made is gone for every earlier fact." The camp half agrees.
   Fossil's control artifacts, Git's notes, Mercurial's markers and AT
   Protocol's labels all add knowledge *beside* old records later, by new
   records that name them. What is truly gone is only what no one knew to
   capture at the moment. So the table sorts into two piles. **Must be captured
   at birth:** the offer's own id; the actor itself; based-on with roles,
   positions and viewpoint; because-of; what it replaces; the grounds of the
   verdict; the session. **Can be added beside, later:** names, types, digests,
   corrections to time and author, labels, translations between grammars. The
   first pile is where a mistake is permanent. It is also short.

---

## 6. The second store: what each source implies

Collected from sections 2 to 4. All INFERRED applications unless marked.

- **Gate numbers do not travel.** did:plc says so of its own sequence numbers
  (REPORTED). Anything that points across stores uses the fact's own id.
- **A store needs its own id, and shares its first vocabulary.** Fixed genesis
  ids for the first kinds, the same everywhere; one fact unique to each store.
- **A pointer into another store is Sid's third kind of read.** An anchor
  outside the store: store id, fact id, and, to pin it, a digest. That is AT
  Protocol's strong reference.
- **Digests earn their place here.** Tagged with their algorithm, over a
  specified canonical form of the logical envelope, with the hash of the value
  inside. Allow several algorithms at once (Fossil) or bump a container version
  (Matrix). Do not tie them to storage bytes (Dolt, Tezos, SSB).
- **Attestation, not just signatures.** Signatures age with key rotation (AT
  Protocol, REPORTED). The pattern that is emerging is a gate's attestation
  plus independent witnesses who keep copies (did:plc replicas, REPORTED).
- **Keys between stores.** Dolt's problem returns in full: two stores mint
  different ids for "the same" key. p2panda's readable name plus definition id
  is the known shape; a "same as" fact is the repair.
- **Grammars will diverge.** "Data schemas aren't linear, even in centralized
  software." Cambria's answer is translators as data, applied on read, and an
  accepted list of pairs that cannot be reconciled.
- **"As of" becomes a map** from store to position token (Gentle's frontier).
  Never compare "when" across stores; there is "not necessarily an underlying
  objective truth" about which came first (Keyhive, REPORTED).
- **Deletion is a request.** Fossil refuses to let delete orders spread;
  Mercurial and Nostr let them spread; nobody can enforce them. What can be
  done: send the erasure fact, ask for an erasure fact back, and record
  silence.
- **People.** A permanent actor id apart from any name, with authority as a
  group of keys (Keyhive), so a person is one actor in both stores.
- **Birth.** Bayou made a server's creation an ordinary write in an existing
  server's log (REPORTED). A second store's birth can be a fact in the first.
- **Exit.** A person's layer, exported, is a small second store. If that export
  is defined early, "one store for the planet" has the answer AT Protocol calls
  credible exit. If it is not, the camp will not believe the rest.

---

## Appendix A. Sources that could not be opened or found

Stated so that no one assumes they were read.

- Terry, "Replicated Data Consistency Explained Through Baseball": Microsoft
  serves a block page; three mirrors returned 404.
- Reed, "Designing Croquet's TeaTime" (OOPSLA 2005 companion): paywall. Reed's
  1978 thesis: not reached. croquet.io docs: 404 (the shipped library's doc
  comments were used). A likely first-person Croquet retrospective by David A.
  Smith on Medium: blocked.
- Linear: Artman's talks are video only, no transcript found. The original of
  his endorsement of the reverse-engineering was not opened; it is quoted in
  that repository's README.
- Zero docs (live site): would not open to fetch tools; read through
  web.archive.org. Boodman's devtools.fm episode: not retrieved. No Noms
  retrospective found.
- Kleppmann's talk "CRDTs: The Hard Parts": no transcript. *Designing
  Data-Intensive Applications*, second edition: not retrieved; the quotes are
  from the 2017 first edition.
- discuss.yjs.dev: unreachable; two threads read through web.archive.org;
  several others not retrieved. Yjs's compatibility promise for its update
  encoding: not verified, so not claimed.
- Seph Gentle's blog has no CRDT posts after 2021; the later work is in the
  EuroSys paper and repository docs.
- AT Protocol: `atproto.com/specs/permissions` returned 404; one Newbold blog
  URL returned 404. The persistence of deleted posts in third-party mirrors is
  known to me only from a secondary source and is not used as a claim here.
- Nostr: NIP-41 has been removed from the repository. No written outbox-model
  essay by Mike Dilger was found.
- SSB: no written retrospective by Dominic Tarr found (his view appears
  second-hand in the Gabby Grove draft). No single post by André Staltz
  announcing his move to PPPPP was found; the PPPPP spec carries the critique.
- Git: lore.kernel.org and public-inbox.org blocked fetches. So the 2011
  generation-numbers thread, and the 2025 thread on a shared change-id header,
  were not read, and no maintainer "we wish we had" quote is claimed.
  Mercurial's on-disk marker layout was not verified; only the wiki's prose
  list of fields.
- Irmin: no retrospective by Gazagnaire or Madhavapeddy found; the 2025 one is
  by Patrick Ferris. Two Tezos issue links on context flattening were not
  fetched. No Dolt post on erasure law was found.
- Cambria's PaPoC 2021 paper was not fetched; the essay was.

## Appendix B. How the quotes were checked

- Nine gatherers each downloaded their sources with `curl` (PDFs through
  `pdftotext`) and confirmed every reported quote with `grep` against the raw
  file. Fetch-tool summaries were never accepted as quotes.
- I then re-checked 147 quotes across the nine sets against the same raw files,
  choosing the ones most likely to change a decision. All were found. Seven
  first looked absent and were then found verbatim: two NIP-01 sentences (my
  tag-stripping had eaten text between `<` and `>` in the markdown); one
  Automerge spec sentence (link markup split it); the carlson quote (tab
  characters in the LWN text); one Eg-walker sentence (two-column PDF text
  interleaved; confirmed in the arXiv HTML); one Croquet sentence (JSDoc link
  markup); one Keyhive sentence (it is on the main notebook page, not the entry
  I first searched).
- Matrix was fetched and checked by me directly from spec.matrix.org and the
  MSC1659 proposal text.
- Places where I rendered a table as prose (tldraw's scopes), or summarised
  source pseudo-code (Replicache's push handler), are marked as paraphrase in
  the text and are not in quotation marks.
- Three statements rest on my background knowledge and say so where they
  appear: the two-actor shape of a Git commit header, and the two "missing
  voices" descriptions of Datomic and W3C PROV.
- Emphasis inside quotes was removed where a gatherer had added it. Spelling
  inside quotes is the source's.
- Raw files and the gatherers' full notes (about 4,000 lines, with many more
  passages than are used here) are in
  `src/proposal/first-record-2026-09-20/research/sources/research-5/` (68 MB,
  moved out of the session's temporary directory on 2026-09-20, not
  committed). One folder per gatherer, each with its `NOTES.md`; my own checks
  are in `parent-checks/`.

## Appendix C. Material on moved items

None. Section three was reassigned before any of it was gathered. Two passages
here touch its ground and belong to my camp's own writing: Kleppmann on
excision and immutable backups (section 4, question (9)), and Jayakar on value
types across runtimes (section 2.2).

---

## Round two: the leans, pressed

Written 2026-09-20, after the blind report above, at Sid's request. The
orchestrator had stood the loop down before this was written, so it has not
seen this section.

**Whose leans.** These are the current leans of softland-ff, Sid's main
session. They are not Sid's rulings. Sid rules; this section presses.

**Marks.** R = reported, I = inferred (mine), N = institutional. To save words,
an R or N mark points to the round-one section that holds the quote and its
URL (for example "R §2.1"). One new source was fetched for T6 and is cited in
full there. Everything else is answered from round one.

### T1. "No optimism" against one worldwide gate

**What to fix at record one so that both doors stay open.**

1. *The admission slot exists from birth.* Bayou gave every write its commit
   number slot at once, holding infinity until the primary filled it (R §2.1).
   Lean (6) already sees that an offer and a fact differ by one slot each way.
   So: one envelope for offer and fact, with the gate's slots explicitly "not
   yet". A tentative thing then needs no second format. (I)
2. *An "includes unadmitted input" mark, in what a running answer returns and in
   what a crossing records.* Bayou's query processor carried two bits from
   tuples into every result row (R §2.1). If the mark is not part of the
   contract from record one, the second door cannot be opened later without
   touching every tool, and old crossings can never say whether what was shown
   was all admitted. In committed-only mode the mark is always "no". It costs
   one bit. (I)
3. *The cut is an opaque token.* Weidner: versions that include tentative state
   "would require more complicated versionIDs" (R §2.12). A token that only the
   store compares can later hold "plus these unadmitted offers", and a gate
   epoch (below). Lean (10) says a cut; sharpen it to opaque. (I)
4. *Offerer-made ids.* Leans (1) and (2) have this. It is Weidner's reason for
   them (R §2.12).

**Does a person's own gate, near the person, change the physics?** Yes, for the
person's own work. A gate a frame away makes committed-only display feasible;
Riffle's target was exactly "within a single frame after a write" (R §2.9).
Bayou's finding that nobody chose commit-only reads (R §2.1) loses its force
there. Three things stay slow.

- *The person's agents.* One gate per layer can sit near one party. Deciding
  case: a person in Bangalore, thirty agents in a Virginia data centre, all
  writing the person's layer. Gate near the person: every agent step is a long
  round trip, and a chain whose steps each wait for admission runs at four or
  five steps a second. Gate near the agents: the person's hand waits. The known
  way out is to let an offer cite an earlier *unadmitted* offer by its id and
  admit them in order. Kleppmann: a missing dependency "simply results in the
  update … never being delivered" (R §2.8); Automerge's regret adds that such a
  record must wait, never be dropped (R §2.8). That is tentativeness between
  agents. So honest tentativeness is needed for agents before it is needed for
  people. (I)
- *Shared layers.* A promotion into a team layer or the base goes to wherever
  that entity lives and contends there with hundreds of others. Showing the
  offer *as an offer* while it waits is inside Sid's rule as the brief words it
  ("as if it were"). (I)
- *Reads of the base from far away.* A running answer near the person stands on
  base facts that arrive late. That raises the stakes of (5), not of optimism.
  Croquet's overlay for a large backlog is the honest display (R §2.3).

**One new duty.** A person changes device or region, and the layer's gate
moves. Two gates must never both believe they own a layer. SSB's fork kills a
feed (R §2.15); Figma routes every client of a file to one process to avoid
"split brain" (R §2.4). So a position is (layer, gate epoch, number), inside the
opaque token. (I)

**Who lives this way.** AT Protocol: one writer per personal repository, shared
views as indexes (N §2.13). Bayou: an accepting server near the user, a primary
far away (R §2.1). Differs: AT Protocol has no compare-and-set across
repositories; Bayou's near server checked no policy.

### T2. Content hash as the name

**A correction first.** Matrix does not use a random name with a digest beside
it. Room versions 1 and 2 did (an assigned id, plus hashes). Matrix abandoned
that in version 3 because "servers receive multiple events with the same ID"
(N §2.16): many minters, no gate. Inside one store the gate refuses a duplicate
name, so that cost is gone. Between stores it comes back.

**The one remaining case for hash-as-name.** A *bare reference* that certifies
itself. A stranger holding only the name can check that what they were handed
is the thing named, trusting no gate. With a random name and a digest beside
it, the fact is checkable but a bare reference is not: a lying store can answer
a name with other content. The repair is known: a reference that crosses
stores carries name plus digest, which is AT Protocol's strong reference
(N §2.13). A second thing is lost: convergence. Hash names give two stores the
same name for the same content with no coordination (Irmin R §3.5; Dolt R
§3.4). Random names do not. The leans keep hash names in the two places where
that pays, ingest (2) and seeds (17). I press both under A.

**Lean (1) is itself a hash name**, not a random one: "computed from the
offer's content plus a random salt". Three presses.

- It inherits the encoding freeze in full. The id depends forever on a
  byte-exact canonical form of an offer (SSB, Tezos, did:plc, Dolt: R §2.15,
  §3.5, §2.13, §3.4).
- With a salt there is no convergence, so the hash buys only a commitment of
  name to content. A digest beside a random name gives the same. (I)
- Deciding case: it breaks lean (9). A person's answer to a sensitive yes-or-no
  question is erased. The fact keeps its id. If the salt is kept on the fact,
  anyone with the envelope computes the id for "yes" and for "no" and compares:
  the name has leaked the erased value. If the salt is thrown away, nobody can
  ever verify the id, and the hashing bought nothing. Matrix, Bamboo and Pijul
  avoid this by hashing the envelope with only the *hash of the value* inside
  (N §2.16, R §2.15, R §3.6). For short values even that inner hash must be
  keyed, so that it dies with the key. (I)

My position: a plain random name; beside it a digest, tagged with its
algorithm, over the envelope with a keyed value-hash inside.

### T3. A separable value, against lean (9)

**What they learned about "value absent".**

- *Matrix: absence is a first-class, served state.* The stripped event "is
  thereafter returned anytime a client or remote server requests it", and the
  spec tells servers to attach a copy of the redaction when serving it (N
  §2.16). The id still verifies, because it was computed over the stripped
  form. The list of keys that survive is fixed per room version. In effect the
  envelope is *defined* as what survives redaction.
- *Gabby Grove: the hole has a fixed shape*, "so that the array the field is
  contained in has the same size in both cases". Bamboo keeps the payload's
  hash and size (R §2.15).
- *Pijul: header and contents are separate sections.* Contents may be absent,
  and fetched and verified later (R §3.6). Its rule for un-applying shows how
  it treats dependents: "A change can only be unrecorded if all changes that
  depend on it are also unrecorded in the same operation."
- *Willow chose the opposite:* no proofs of completeness, so that removal
  leaves no trace (R §2.15). AT Protocol too: services "are expected not to
  differentiate between content which has never existed and content which has
  been entirely deleted" (N §2.13), after which consumers could not tell what a
  delete had removed (R §2.13, issue #927).
- *Dangling references wait; they are never dropped.* Automerge's save dropped
  them and that was a bug (R §2.8).
- *Once things can be absent, numbers that were derivable must be stored.*
  PPPPP: "we NEED this field" (R §2.15).

**For Sid (I).** "The map must not lie" picks Matrix's side over Willow's. A
reader must be able to tell three states apart: never existed; erased, with a
pointer to the erasure fact; exists but withheld from you. Lean (5)'s "what was
withheld" is the third.

**Against lean (9) as worded.** Nobody in my camp deletes by discarding a key.
All of them remove bytes. Keyhive states the limit of keys: "if someone has the
data and the symmetric key, then they have the ability to read that data" (R
§2.9). Deciding case: year-one ciphertext sits in a log that is never rewritten
(lean 0) and in every backup. In year twelve the cipher, or a key-handling bug,
fails. The log cannot be re-encrypted, because it cannot be rewritten. Leans
(0) and (9) collide. The camp's shape removes the collision: the log holds the
envelope and a random value id, with a keyed digest; values live *beside* the
log in a store that may be rewritten, re-encrypted and erased. Encryption per
value is then defence in depth, not the deletion mechanism. (I)

Two smaller presses. "The fact keeps the value's hash": for a short value a
plain hash *is* the value; keyed, or gone. (I) "Maybe by-whom may go": if
by-whom is an opaque actor id and the name is a separate fact, by-whom never
needs to go; erase the name fact's value. Pijul is built this way (R §3.6), and
Fossil's 'bertina' shows what happens when the name is inside the record (R
§3.3). What the lean leaves out: a based-on list can identify a person, and so
can an entity and key alone ("this person has a fact under :diagnosis" says
enough with the value gone). Erasure may have to reach a cell's visibility, not
only a value. (I)

### T4. A read's role now; follow-or-stay later

I accept the split. A based-on entry is a statement about the past, so it
always stays on the version read. Whether the fact should now be judged against
the latest is the staleness definition's business, and definitions are facts
that can change. So the second half of lean (4) is the wrong question *for
reads*. It is the right question for a different thing: a *value* that refers
to another entity. p2panda made floating and pinned two field types (R §2.15);
AT Protocol has the plain URI and the strong reference (N §2.13). That belongs
in the key's grammar.

The role at write time: yes. The sources are as direct as this camp gets
(Pijul R §3.6; Automerge R §2.8; Mercurial's wish R §3.2). On "the floor
guesses; the tool may correct": a correction is a new fact beside, naming the
read it re-marks, never an edit (Fossil R §3.3). And the floor rarely has to
guess. A deterministic tool depends on all its reads by construction (Convex,
I §2.2). A model or a person was exposed to theirs.

Deciding case: a summary layer over ten million papers. A model summarises
forty papers; one is retracted. If all forty reads are "depends-on", every
retraction turns thousands of summaries loudly stale, the signal becomes
noise, and people stop looking at it. A map that cries wolf also lies. If they
are "exposed-to", the summary turns quietly doubtful, and only a paper it cites
makes it stale. The role sets the loudness. It cannot be recovered afterwards.

### T5. Names or ids for keys, from AT Protocol's side

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

### T6. By whom plus the grant: what it cost Matrix

New source: Matrix, MSC1442, "State Resolution: Reloaded" (2018),
https://github.com/matrix-org/matrix-spec-proposals/blob/main/proposals/1442-state-resolution.md
(fetched and checked 2026-09-20; N).

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

All of that is the price of "which grant was current?" having no single answer.
Under one gate, grants are totally ordered with the facts they authorise. The
question becomes a lookup at a position, and a grant revoked before admission
is an ordinary refusal. What is left of the cost (I): the bytes of the
references on every fact; and keeping every cited grant version forever, which
Sid's store does anyway.

One case returns with the hybrid placement of T1. A person's own gate admits a
fact under a grant that lives in a *shared* layer and was revoked there a
moment earlier. That is Matrix's fork in miniature. The rule that avoids it: a
gate checks only grants that live in layers it orders itself. Grants for a
personal layer live in that layer. Lean (8)'s "exists before the agent's first
write" fits. (I)

Resembles: each record cites what authorised it. Differs: Matrix re-derives
authorisation at every merge; Sid's gate decides once and records its grounds.

### A. The other leans

(1), (9), (4), (3) and (8) are covered by T2, T3, T4, T5 and T6.

**(0) Never rewritten.** Reject as a physical claim; keep as a logical one.
Hipp lives by never-rewrite and says the bytes are always editable (R §3.3).
Meyer withdrew "append-only" (R §2.15). Deciding case: year six, the storage
encoding or the cipher must change under ten million papers and years of
facts. Dolt did it by re-encoding everything and proving the logical rows
unchanged (R §3.4). Tezos cannot (N §3.5). The lean says (0) makes (9) and (10)
now-or-never. Keeping values beside the log (T3) is exactly what makes (9) *not*
now-or-never.

**(2) Entity id.** Random, offerer-minted, no time: stands. Weidner, Figma,
Linear and Dolt live this way (R). Sharpen: the gate refuses a clash (I); the
written form carries a scheme marker (Convex, R §2.2). Press the ingest clause.
An id derived from "source plus form" freezes the identity rule forever, as
did:plc's first-record hash did (R §2.13). Deciding case: arXiv 2409.14252v1,
its v3, and the EuroSys DOI. One entity or three? Whatever the rule says on day
one can never be revised, and every normalisation slip (DOI case, URL forms)
splits an entity for good. Alternative (I): the entity id stays random. The
lane writes a *registry cell* whose entity id is derived from the source
identifier and whose value is the entity. Compare-and-set on that cell gives
exactly-once. A wrong merge or split is repaired by a new fact. The same device
enforces any uniqueness rule, such as a key's word in the base, which lean (3)
needs.

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

**(11) When, based on, because of.** The gate's clock, never for order: stands
(Figma, Convex, Weidner, R). "Every read listed": Bayou gave up literal lists
because they "could get large" (R §2.1). Deciding case: a tool reads every
abstract in the base as of a cut. Listed literally, that is ten million entries
on one fact. "Listed" has to mean pattern plus cut plus viewpoint (Convex's
ranges, R §2.2). "No grace period": stands, and Yjs shows why (R §2.10).
"Empty only at a chain's start": jj never has an empty parent (R §3.2);
pointing a chain's start at the session fact costs nothing.

**(16) Visibility.** "Base open": AT Protocol lived public-first and is adding
private data four years in (R §2.13). Stands if "open" is a policy fact in the
seed and not a property of the store. Sharpen: a verdict that sits in the
fact's own layer shows every reader which grant authorised it. If that grant
lives in a private layer, the display says "withheld". (I)

**(10) Order.** Stands: Figma per file, AT Protocol per repository, Croquet per
session (R). Sharpen: an opaque token with a gate epoch (T1). What must share a
partition is decided by invariants across cells (Figma's cycle check, R §2.4),
and uniqueness invariants become registry cells, as under (2). (I)

**(5) Index position.** Stands: Bayou's checkpoint id, Croquet's backlog (R).
"What was withheld" is a piece nobody in this camp has. Keep it.

**(6) Replaces.** Stands: Automerge, p2panda, Mercurial, jj (R).

**(7) The verdict.** Press "refusals kept forever". Nobody in the camp keeps
them except did:plc, which then had to purge (R §2.13). Deciding case: thirty
agents contend for one hot cell at machine rate. Each admission refuses up to
twenty-nine others. Refusals outnumber facts twenty-nine to one, forever, under
lean (13). Two remedies: keep the refusal's envelope without its value; and cut
refusals at the source with Bayou's shape, an offer that carries its own check
and its own fallback (R §2.1). On "beside": with stable random ids, beside
cannot be orphaned (Git's notes were orphaned by an id change, R §3.1). The
only argument left for inline is write volume.

**(12) Session start.** Stands: Croquet, jj (R). Add the *gate's* build to the
grounds of each verdict; see C.

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

**(15) A click.** Stands: Patchwork's bots on their own branch (R §2.9); Zero's
named mutator as the record of the act (N §2.6).

**(13) Storage.** "Never trimmed" is safe only if (14) and (7) change. As
leaned, it is the lean most likely to be broken by year three; Irmin is the
precedent (R §3.5). "Plain maps": add an envelope-format version, and a
canonical form defined apart from any runtime's maps (SSB; AT Protocol and Go:
R §2.15, §2.13).

### B. Wrong questions

1. **(4), second half.** Follow-or-stay is not a property of a read. Ask it of
   value references, in the grammar (T4).
2. **(1).** Name against number is settled. The live question is what any hash
   covers, and whether it survives an erasure (T2).
3. **(9).** "How do we delete from a log that is never rewritten?" Ask what the
   log is a log *of*. Envelopes and value ids. Then nothing in the log ever
   needs deleting (T3).
4. **(17).** "How do all stores compute the same seed ids?" Ask how they *have*
   the same ones. Constants.
5. **(14).** "Which motions, at what tick?" Ask what the person acted on, and
   what they were shown.
6. **(2), ingest.** "How is the id derived from the source?" Ask where the rule
   "one source, one entity" is enforced. In a cell, under compare-and-set,
   where it can be corrected.

### C. Above the table: only what the leans change

1. **Every personal layer is now a small store.** The hybrid placement turns
   "one writer" into "one writer per ordering domain", which is what this camp
   said in round one. It also changes "one store for the planet": from inside,
   it now has AT Protocol's shape, personal repositories plus shared indexes.
   So the second store stops being the rare later case. Its conventions are
   needed at record one: facts named by their own ids; gate numbers as
   per-layer annotations; references across layers that carry a digest or a
   cut; "as of" as a map; a gate epoch. The good news is that exit comes nearly
   free. A personal layer with its own gate is already exportable. This is my
   strongest change above the table. (I)
2. **The envelope is no longer nine parts.** The leans add an offer id, what it
   replaces, a verdict beside, roles on reads, a value id or keyed hash, and a
   session. That is fine. It means the forever-promise should be about a
   version marker and a rule for unknown parts, not about the number nine. (I)
3. **"The runtime as the one non-fact" now meets many gates.** With gates near
   people, several builds of the runtime admit facts at the same moment. Bayou
   required every server to enforce the same resource bounds so that checks
   fail the same way everywhere (R §2.1). Deciding case: a device gate on an
   old build admits a fact into a personal layer that the base's newer gate
   would refuse on shape. The fact is in the store forever, and its promotion
   fails later for a reason nobody recorded. So the gate's build belongs in
   the grounds of every verdict. (I)
4. **"Every read recorded", with (14) as leaned and (13) never trimmed,** is the
   combination this camp would break first, on volume and on privacy.
