Yardstick cut by: Claude Fable 5.1, max effort. Marked by: Codex, reasoning effort high.

You are doing a marking task on ONE file. Follow these rules exactly.

FILE: src/proposal/first-record-2026-09-20/research/frontiers-views.md (578 lines)

Hard limits
- Read only that file. Do not open any other file: not the AGENTS.md reading list, not docs/,
  not loop/, not sources/, not HANDOFF.md, nothing else in the repository. Do not use the network.
- Do not create, edit, or delete any file. Do not run any command that writes. Your only
  output is your final message.
- Do not summarise or reword the file anywhere in your output, except in the short `note` column.

Background
Sid is deciding what his store must fix before its first record. The store only appends and never
rewrites, so whatever a fact does not carry when it is made is gone for every earlier fact. He wants
the conventions that would paint him into a corner. One research file has already been read and
turned into a LEDGER (below). It is one voice, not a ruling. Your file is another voice. A reader
will compare the voices corner by corner. A script will copy the line ranges you return, verbatim,
so you return line ranges and tags only, never text.

The file refers to Sid's open questions by these handle numbers:
(0) the log: never rewritten, or only never lost  (1) what a pointer to a fact uses: the gate's
number, or the fact's own id, random or from content  (2) how ids are made; may an id give away
when or where  (3) a key: a word, or an id  (4) read marks: depends-on vs how-she-got-here; follow
latest vs stay on the version read  (5) pattern reads: how far the index had got; what was withheld
(6) "replacing 25" kept on 37  (7) the gate's yes or no: kept for ever, where, refusals  (8) by-whom:
the agent or the person; acts-for  (9) deleting a value, backups included  (10) order: what shares
a partition; as-of one number or a position per part; two gates on one layer  (11) whose clock;
every read listed; because-of always filled  (12) session-start fact; every rebuild a fact
(13) plain maps or classes; log trimmed; backups  (14) which motions of the hand become facts
(15) which tools may fire on her click and write in her name  (16) who can see a fact before any
permission exists  (17) the first facts: what, by whom, ordinary ids or words in code, same in a
second store.

THE LEDGER (the yardstick). Each line has a code, the handles it touches, its CONVENTION, and the
reasons and cases it already HOLDS.

P0 PREMISE [0]. CONVENTION: never-rewritten is a promise about admitted content, not about bytes.
  Repair and re-encoding may rewrite bytes to keep content the same. The promise is checkable by a
  digest on each fact and a pointer to what it replaced. One designed exception, erasure, itself a
  fact. HOLDS: "nothing lost silently".

C1 NAME [2, 1]. CONVENTION: the offerer makes a random 128-bit id for every entity, offer and fact,
  before the first attempt, and keeps it across retries. One id space. Never worked out from
  content. Never the gate's number. No "where" or "when" inside. Never a capability. Facts point at
  each other by this id. A digest, tagged with its algorithm, may sit beside the id on pointers that
  must be commitments. On ingest the OFFER id is derived from lane + source key + source version, so
  a re-run is a recognised retry; the ENTITY id is random.
  HOLDS: every surveyed system uses offerer-made ids; FoundationDB calls lacking them the biggest
  gotcha; a slow index can be rebuilt but a leaky id can never be recalled; hash algorithms wear out
  while names are for ever (git SHA-1); a content hash freezes one canonical byte encoding in every
  runtime; with a content id "same intent, different content" stops being a visible error; ids that
  exist before admission let one batch of offers point at each other; Twitter's id widening shows
  what a wrong width costs; TigerBeetle and RFC 9562 want time in the id for index speed, Spanner
  and Firestore refuse it for hot spots; a source-derived entity id makes the source's identity
  mistakes permanent.

C2 AS-OF [10, 5]. CONVENTION: "as of" inside a based-on is an opaque, kind-tagged token that only
  the store mints and reads. A comparison may answer "incomparable" and every consumer handles that
  from day one. Later it may hold: store id and incarnation, a layout epoch for re-partitioning, a
  pointer to a stored shared cut, the index's position, the policy position. The withheld set is
  never recorded. Every answer states the cut it is complete to.
  HOLDS: Zanzibar's zookie is opaque "to allow future extensions"; a bare number in the first
  record can never be widened; a position per partition inline is tens of kilobytes per read entry,
  so shared cuts are minted on a beat as small facts; recording what was withheld tells Alice that
  Bob holds private claims there.

C3 ORDER [10]. CONVENTION: the compare-and-set cell (entity, key, layer) sits in one ordered unit.
  What else shares that unit, the entity or the layer, is OPEN. Partition identity stays out of
  names and out of as-of tokens.
  HOLDS: each ordered unit costs a steady heartbeat to tell "nothing happened" from "late", so the
  unit should be coarser than a layer; Megastore's customers took twelve years to get out of their
  entity groups; policies and grants are entities, so under partition-by-entity an offer can be
  admitted under a grant already revoked in another partition; with tens of thousands of writers on
  the same few entities, by-entity queues writes that never conflict, while by-layer spreads writes
  with the writers but the shared base cannot be one unit; id pointers + an opaque token + the
  replaces-chain are what make a wrong choice here survivable.

C4 KEY [3, 17]. CONVENTION: a key is an id; its word and its grammar are facts with versions. The
  first key ids are published constants, the same in every store. The store's id, its incarnation
  and its gate's actor id differ per store; the gate is not a seed constant.
  HOLDS: stored patterns name keys and are never rewritten, so whatever token names a key inside a
  stored pattern is pinned for good; two groups can write "dose" and mean different things; with a
  constant gate, on federation day nobody could say which gate.

C5 ERASURE [9, 1]. CONVENTION: from the first erasable value only two roads work: encrypt the value
  and delete the key, or keep the value outside the log with a pointer in the record. Digests are
  taken over ciphertext, or salted with something that dies with the key. The opaque actor id stays
  on the fact; the facts that say who that actor is can be erased. The envelope's value part admits
  clear, ciphertext-with-key-reference, and outside-with-pointer from day one. Content kept on model
  crossings takes the same road.
  HOLDS: "a value once written in the clear is in every backup"; a plain hash of a short value (a
  rating, a vote, an email) can be guessed after erasure; "who read the erased thing" must keep
  working.

C6 REPLACES [6]. CONVENTION: the new fact names the fact it replaces.
  HOLDS: without it Elle cannot tell which write replaced which; blind writes destroy history;
  FoundationDB ships the previous number with each new one; TigerBeetle chains checksums; each
  cell's history walks from the facts alone and a fork can be proved; cost is one pointer.

C7 ENVELOPE [13, 11]. CONVENTION: the envelope has a version and is closed: the gate refuses parts
  it does not know. No part uses emptiness or a magic value as a signal; a chain's start gets an
  explicit mark. The logical envelope is kept apart from its physical encoding.
  HOLDS: TigerBeetle's "reserved, must be zero"; an empty slot could mean "starts a chain", "older
  envelope" or "bug"; an envelope version turns most future corners into early losses.

C8 BY-WHOM [8]. CONVENTION: by-whom is the immediate actor as itself (the agent, not the person).
  Acts-for is a separate, scoped, short-lived statement that the gate checks and the verdict names.
  HOLDS: if an agent writes as the person, what the person did and what their agents did can never
  be told apart again.

EARLY LOSSES (cheap from the start; adopting late loses only the early period; nothing is stuck):
E1 VERDICT [7]. The gate's verdict is kept under the offer's id, atomic with the compare-and-set, in
  the cell's partition. It names the grammar version passed, the policy fact and the position it was
  read at, the expected version, and the epoch. Three outcomes: ok, refused, unknown.
E2 CROSSINGS. When a running answer is shown to a person or handed to a model or the host, the
  crossing carries a digest; crossings to a model or the host also carry the content.
E3 CLAIMED WHEN [11]. A claimed "when" from the offerer sits beside the gate's stamp. Order is never
  taken from either.
E4 GRANT INVOKED [15]. Every offer names the grant it invokes, by id and version; the gate checks
  that grant and does not search. A click designates and grants. HOLDS: a tool Alice enabled matches
  a fact Mallory wrote and then writes as Alice's agent (the confused deputy at machine rate);
  "everything done under a revoked grant is one walk".
E5 READ MARKS [4, 11]. Each based-on entry says how it was read and whether the fact depends on it,
  with a third value: shown, dependence unknown.
E6 READS ON THE CROSSING [11, 5]. Reads are kept on the crossing as pattern plus cut, never as rows;
  a fact points at the crossing instead of listing its reads. Eager matching only for tools and
  watched answers; a historical pattern read is checked lazily.
E7 THE HAND [14]. Select and point are recorded by default; pan and zoom only when a crossing's
  digest changes.
E8 SESSION [12]. The unit is the gate's epoch and the crossing, not the session. A gate's term opens
  with a fact in each ordered unit and every verdict carries its epoch.

ABOVE THE TABLE:
A1 RE-DERIVING. "Worked out again exactly from the same reads" holds only for one tool version on
  one runtime build. A rebuild moves no fact, so nothing is marked stale by it. Temporal and Restate
  can see a replay break only because old outputs are kept to compare with; a running answer keeps
  nothing. Hence digests and content on crossings.
A2 REBUILD IS A FACT [12]. Every rebuild of the runtime is a fact that running answers read.
A3 SAFETY. Safety rests on the compare-and-set plus an epoch, not on there being one writer or one
  gate (Datomic had two live transactors at failover and it did not matter).

ALSO ALREADY HELD:
X1 BATCHES. If one offer carries several facts, fix early whether each is checked against the state
  before the batch or against the facts ahead of it in the batch.
X2 Each fact says which grammar version it passed; grammar and policy changes reach several gate
  instances at different moments (F1 needed leases and in-between versions).
X3 Nearest-layer-wins is an override rule; a layer used to hide a fact becomes deny-by-position.
X4 DEFAULT VISIBILITY [16]. Visibility is inherited at creation, so no fact is ever without a rule.
X5 A small formal model of the envelope's rules, run with two gates and a failover, before building.
CAN WAIT (W): W1 index layout. W2 physical encoding. W3 one number or many, given the opaque token.
  W4 how many gates, given compare-and-set plus epoch.

CARRIED QUESTIONS (the ledger's author wanted these answered by other files; any file may answer):
Q1 What is the log physically: the depot of offers with the gate a fold over it, or a second depot
   that the gate writes?
Q2 If the gate runs inside a topology that can retry, where is the gate's "when" fixed?
Q3 Can an index (a PState or any materialised view) state the log positions it has applied?
Q4 Can the append path refuse a stale writer?
Q5 Can the partition count change, and what then happens to positions?
Q6 How does index layout behave with random ids?
Q7 What shares a partition or ordered unit: the entity, the layer, or both?
Q8 Hickey and Datomic on "sayings (transactions) have ids and facts (datoms) do not"; attributes as
   entities.
Q9 Content addressing's best case (git, Unison, IPLD, Merkle DAGs, hash-linked ops) against a
   random name with a digest beside it.
Q10 Erasure in event-sourced systems (crypto-shredding, forgettable payloads, excision,
   compaction), and verifiable or transparency logs as the strong form of "never rewritten".

The file speaks its camp's vocabulary. Map it before you judge. Examples: zookie, frontier,
timestamp, basis-t, vector clock, heads, snapshot id are forms of AS-OF. Transaction id, tx entity,
commit hash, op id, event id, UUID are forms of NAME or of a pointer. Attribute, ident, schema,
property, predicate, ontology term, type are forms of KEY. Tombstone, crypto-shredding, excision,
obliteration, compaction, retention are forms of ERASURE. Entity group, shard key, stream,
aggregate, document, partition key are forms of ORDER. Principal, delegation, impersonation,
service account, capability are forms of BY-WHOM or GRANT.

Task
Split the WHOLE file, line 1 to the last line, into consecutive ranges, and give each range one tag.
A range is a run of whole paragraphs, list items (with their continuation lines), table rows, or
headings. Blank lines belong to the range before them. Every line of the file must fall in exactly
one range. Keep a range to one point: if two neighbouring paragraphs make different points about
different ledger lines, they are two ranges. A range that gets copied should be readable alone.

Tags, in order of precedence (if two apply, use the one listed first and name the other in the note):
  OWN-LIST    The file's own list of what cannot be added later, what is gone for every earlier
              fact, or what must be fixed before the first record. Mark the whole list, including
              items that agree with the ledger.
  DISAGREES   The passage states or implies a convention incompatible with a ledger line; or argues
              the ledger's convention is wrong, unnecessary, too costly or unworkable; or sorts it
              differently (the ledger says early loss or can wait, the file says it cannot be added
              later, or the reverse). A voice in the file that disagrees counts even if the file's
              author does not side with it.
  ABOVE       The passage rejects or questions the frame rather than one line: the problem itself,
              the premise (0), one kind of record for everything, tools and policies in the same
              store as content, one gate, matching instead of routing, every read recorded, running
              answers never stored, one store for the planet, the nine parts. Also "this is the
              wrong question".
  NEW-CORNER  The passage names something that must be fixed before the first record, or that early
              facts would lack for ever, and no ledger line (P0, C, E, A, X, W) holds it.
  CARRIED     The passage answers, or bears directly on, one of Q1 to Q10.
  NEW-CASE    A concrete deciding case for a ledger line that the line does not HOLD: a scenario,
              incident, migration, outage or regret that would decide the line one way.
  NEW-REASON  A reason, mechanism, cost, number, condition ("unless", "only if", "breaks when") or
              limit bearing on a ledger line, and different from the reasons that line HOLDS. It may
              support, qualify or bound the line.
  SAME        The passage says the same as a ledger line and its reasons are among those HELD, or
              are further instances of a held reason (another system showing the same thing). These
              are counted, not copied.
  SKIP        None of the above: narrative evidence with no bearing on any line, biography, method
              notes, source lists, headings standing alone. One SKIP range may span many paragraphs.

Rules of judgment
- There is no target size. Do not try to make anything smaller or larger. It is fine if most of a
  "round two" section is marked, and it is fine if most of a biographical section is SKIP.
- Whenever you are unsure between SAME and a copied tag, use the copied tag. Whenever you are unsure
  between SKIP and any other tag, use the other tag.
- A further instance of a HELD reason is SAME. A different reason is NEW-REASON. "Another system
  also makes ids at the client" is SAME. "Client-made ids failed here, for this cause" is NEW-CASE
  or DISAGREES.
- Judge against the CONVENTION and HOLDS text above only. Do not judge whether the file is right.

Output format
Your final message must be ONLY tab-separated rows. No header, no code fence, no prose before or
after. One row per range, in file order, covering the file from line 1 to line 578:
  start<TAB>end<TAB>tag<TAB>ledger<TAB>note
- start, end: line numbers in the file, inclusive.
- tag: exactly one of OWN-LIST DISAGREES ABOVE NEW-CORNER CARRIED NEW-CASE NEW-REASON SAME SKIP.
- ledger: the ledger code or codes the range bears on, most relevant first, comma-separated with no
  spaces, from: P0 C1 C2 C3 C4 C5 C6 C7 C8 E1 E2 E3 E4 E5 E6 E7 E8 A1 A2 A3 X1 X2 X3 X4 X5 W1 W2 W3
  W4. For CARRIED put the ledger code the question belongs to, and name the Q in the note. For
  NEW-CORNER, ABOVE, OWN-LIST and SKIP use a single hyphen when no code fits.
- note: at most 16 words naming the point, and for DISAGREES which way it disagrees. No tabs.

Method
Number the lines (nl -ba or cat -n) and read the whole file first, in order, before you judge any
range. Work section by section. Before answering, check that your rows cover every line from 1 to
578 exactly once, in order, with no gaps and no overlaps.
