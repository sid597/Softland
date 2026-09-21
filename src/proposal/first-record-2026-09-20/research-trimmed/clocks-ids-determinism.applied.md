# File one applied: `research/clocks-ids-determinism.md`

Written 21 September 2026 by Claude Fable 5.1 at max effort, in Sid's session, from the chat.
**Status: one voice from one file. Nothing here is a lean of Sid's or a ruling.** The sort into
corners and early losses (section 3) is the session's own and unchecked. L-numbers are line
numbers in `research/clocks-ids-determinism.md`.

## 0. What happened with Codex

- **Run:** Codex (gpt-6-astra, effort high, read-only sandbox) marked the 295 lines I would have
  skipped, in 194 blocks. The plan passed the every-line-once check on the first try.
- **Result:** it promoted 82 of the 100 substantive blocks. It deferred 18 as already said in the
  kept text. It also deferred the 95 source-list entries.
- **Check:** I checked all 18 deferrals against the text, and all are fair.
- **Saving:** the faithful trim is 94% of the original, so this file has almost no fat.
  - The section I planned to skip is each team's "which questions they speak to", 23% of the file.
  - It carries reasons and cases that the by-question synthesis drops.
- **What I read:** all of it except the source list. For files built like this one, trimming saves
  almost nothing. Choosing the reading order matters more.
- **Artifacts** in this folder: `assemble.py`, `clocks-ids-determinism.prompt.md`,
  `clocks-ids-determinism.plan.tsv`, `clocks-ids-determinism.trim.md`,
  `clocks-ids-determinism.deferred.md`.

## 1. What this file is, and how far I trust it

- **The camp:** builders who lived with their choices: TigerBeetle; Temporal and Restate; Zanzibar
  and its rebuilders; FoundationDB and Apple's Record Layer; Calvin and Fauna; Google's storage
  lineage; Jepsen, Elle and TLA+; Snowflake and RFC 9562.
- **The rounds:** round one was written blind to Sid's leans, on purpose (L5). Round two (§8)
  presses the leans as the orchestrator listed them: "leans, not Sid's rulings" (L1017).
- **Its marks:** REPORTED, INSTITUTIONAL, INFERRED. Every statement about Sid's store is INFERRED
  (L14).
- **Its quotes:** about 580 quotes were script-checked against saved sources. The saved copies were
  in a scratch folder that is gone, and only the URLs remain (L21). The quotes were checked by the
  author, not by me.
- **My own knowledge:** where I know these systems, the account matches what I know and I found
  nothing to dispute. That is not verification. The cases I know: TigerBeetle's three names;
  versionstamps and CloudKit's incarnation; zookies and the new-enemy problem; Spanner's commit
  wait; Temporal's versioning history; Jepsen on Datomic; RFC 9562.
- **Limit one, scale:** its deciding cases assume "tens of agents per person" and "hundreds of
  people on one problem" (L1025, L1042, L1088). Sid now holds 10³ to 10⁴ times that. Every cost
  argument in it gets stronger and none gets weaker. Its own "ten million read entries a second"
  becomes 10¹⁰ or more.
- **Limit two, Rama:** it never saw Rama. Several of its points stop at "a claim about Rama, and it
  needs its own test". Those are listed in §9.

## 2. Does it accept the premise?

**The premise (0).** Yes, restated as a promise about admitted content, not about bytes (L713,
L1084, L913).
- Repair and re-encoding rewrite bytes in order to keep content the same.
- The promise is made checkable by a digest on each fact and a pointer to what it replaced.
- There is one designed exception, erasure, and it is itself a fact.
- This is the same place I reached from Sid's text alone: "nothing lost silently". That is
  convergence, not independent evidence.

**The problem.** It doesn't challenge the problem. It challenges four things above the table. The
first is the strong one (§4): running answers never stored; every read recorded; one writer; one
store for the planet.

**Its organizing idea.** A record's name, its order and its integrity are three jobs. Systems that
lasted gave each its own field, and systems that fused two paid later (L34).

## 3. The conventions, sorted by how they corner

The file gives one list of what is "gone for every earlier fact" (L58–68). Sid's words separate two
things: "gone for every earlier fact" and "paint me in a corner".

I sorted the list using Sid's forest answers. Keys, grammars and policies are live. Verdicts,
crossings and session facts are facts whose value shapes can change with a build. That gives two
kinds:
- **Corner:** the choice sits inside every later fact too, because later facts must work with early
  ones. Getting it wrong means two regimes for ever, or a rewrite.
- **Early loss:** early facts lack something. The loss is bounded by when you add it, and nothing is
  structurally stuck.

This sort is mine. The file didn't have the forest answers in enough detail to make it.

### Eight corners

**C1. What a name is. (2), (1)**
- **Convention:** the id is made by the offerer before the first attempt and kept across retries.
  128 bits, random, one id space, edge values reserved. Never "where". Never the gate's number.
  Never worked out from content. Never a capability.
- **Evidence:** every system there agrees on offerer-made ids (L36, L718). FoundationDB calls the
  lack "probably the biggest 'gotcha'" (L394).
- **The split on "when" inside an id:** TigerBeetle and RFC 9562 are for it, for index speed.
  Spanner and Firestore are against it, for hot spots. The asymmetry decides it for me: a slow index
  can be rebuilt, and a leaky id can never be recalled (L702).
- **The pointer rule:** facts point at each other by the offerer-made id. Such an id works before
  admission, so one batch of offers can point at each other. It also works across retries, stores
  and restores (L142, L414).
- **Against lean (1), hash of offer plus salt as the id (L1019–1031):** the hash wears out while the
  name is for ever (I know git's SHA-1 transition as a live case). A content hash freezes a
  canonical byte encoding in every agent runtime, which collides with the plain-maps lean. "Same
  intent, different content" stops being a visible error. The sharpening: a random name, with an
  algorithm-tagged digest beside it, carried only by pointers that must be commitments.
- **Carried up:** ids sit inside values and cross every who-may-see line. Twitter's widening of its
  ids across "100,000 different codebases" (L689) shows what a wrong width costs.
- **Ingest:** derive the offer id from lane, source key and source version. Mint the entity id at
  random. A re-run is then a recognised retry, and the kept verdict hands back the first entity id
  (L1031). An entity id derived from the source makes the source's identity mistakes permanent. Ten
  million papers is where that bites.

**C2. What "as of" is. (10), (5)**
- **Convention:** an opaque, kind-tagged token that only the store mints and reads. A comparison may
  answer "incomparable", and every consumer must handle that from day one (L1044).
- **Zanzibar's reason:** "to allow future extensions" (L303).
- **Why it corners:** "A bare number in the first record can never be widened" (L779).
- **What the token may need to hold later (L1036–1040):** store id and incarnation; a layout epoch,
  for re-partitioning; a pointer to a stored cut, so many reads share one; the index's position; the
  policy position the answer was filtered under.
- **Carried up:** an inline position for every partition is tens of kilobytes per read entry
  (L1038). Shared cuts, minted on a beat as small facts, are what make pattern reads affordable.
- **On "what was withheld":** never record the withheld set, because it tells Alice that Bob holds
  private claims there (L1042). My addition: if every read carries its policy position
  unconditionally, "a filter ran" tells nobody anything.

**C3. What shares an order. (10)**
- **The file's view:** the hardest corner and the least settled. The compare-and-set cell must sit
  in one ordered unit, and that is the one hard need (L779). The unit must be coarser than a layer:
  telling "nothing happened" from "late" costs a steady heartbeat per unit (L570). The choice is
  fixed per fact for ever, so keep partition identity out of names and out of as-of (L571).
  Megastore's customers took twelve years to get out of this one (L546).
- **Open interaction (L1091):** policies and grants are entities. Under partition-by-entity they sit
  in other partitions than the cells they govern. So an offer can be admitted under a grant already
  revoked elsewhere.
- **My pressure on partition-by-entity at Sid's scale:** tens of thousands of people and their
  agents on one problem write about the same few entities. By entity, all those writes queue at
  those partitions, though they are in different layers and never conflict. By layer, with many
  layers hashed to one partition, writes spread with the writers; policy then sits with the layer it
  governs, and a personal layer becomes the unit of placement. The base can't be one unit, so it
  would split by entity: two regimes by design. I hold this as a question for `rama-marz.md`, not a
  lean.
- **Why C1, C2 and C6 matter most here:** with id pointers, an opaque token that names its layout,
  and per-cell order from the chain, a re-partition changes no fact. Those three are what make C3
  survivable if it turns out wrong.

**C4. A key is an id. (3), (17)**
- **Reason (L741, L431):** based-on entries store patterns, patterns name keys, and nothing stored
  is rewritten. Whatever token names a key inside a stored pattern is pinned for good. If that token
  is the word, the word can never change or split. If it is an id, the word is a fact with versions.
- **Price:** the first key ids must be published constants, the same in every store.
- **Other first facts:** the store's id, its incarnation and its gate's actor id must differ per
  store. Round two rejects one clause of lean (17): the gate is not a constant. If it were, on
  federation day nobody could say which gate (L1085).

**C5. The erasure road. (9), (1)**
- **Convention:** in a never-rewrite store only two roads work: delete a key, or keep the value
  outside with a pointer in the record. "A value once written in the clear is in every backup"
  (L747).
- **Digests:** take them over the ciphertext, or salt them with something that dies with the key. A
  plain hash of a short value (a rating, a vote, an email) can be guessed after erasure (L1086).
- **By-whom:** keep the opaque actor id on the fact and erase the facts that say who that actor is.
  Otherwise "who read the erased thing" breaks.
- **Carried up:** the envelope's value part must admit clear, ciphertext-with-key-reference, and
  outside-with-pointer from day one. Content kept on model crossings falls under the same road.

**C6. The new fact names the one it replaces. (6)**
- **Evidence:** without it Elle "cannot tell" which write replaced which, and blind writes "destroy
  history" (L650). FoundationDB ships the previous number with each new one. TigerBeetle chains
  checksums.
- **With it:** each cell's history can be walked from the facts alone, and a fork can be proved.
- **Cost:** one pointer. The file, the lean and my first-turn note all agree here.

**C7. Envelope discipline. (13), (11 because-of)**
- **Convention:** the envelope has a version. It is closed: the gate refuses parts it doesn't know
  (TigerBeetle's "reserved, must be zero"). No part uses emptiness or a magic value as a signal, so
  a chain's start gets an explicit mark (L807); an empty slot could otherwise mean "starts a chain",
  "older envelope", or "bug". The logical envelope is kept apart from its physical encoding (L849,
  L918).
- **Why it matters most overall:** an envelope version turns most future corners into early losses.
  "Facts of envelope two carry X" is a clean regime. A regime inferred from absence is not.

**C8. By-whom is the immediate actor, as itself. (8)**
- **Reason:** "If an agent writes as the person, then in a store that never rewrites, what the
  person did and what their agents did can never be told apart again" (L586).
- **Acts-for:** a separate, scoped, short-lived statement. The gate checks it and the verdict names
  it.

### Early losses

Cheap to have from the start; late adoption loses only the early period. No dead end.

- **Verdict contents:** the grammar version, the policy fact and the position it was read at, the
  expected version, and the epoch. The verdict's existence under the offer's id matters on day one,
  though: it must be atomic with the compare-and-set and sit in the cell's partition (L1080). Ingest
  retries depend on it.
- **Crossing digests, and content on model crossings.**
- **A claimed "when" beside the gate's.** Order is never taken from either (L764).
- **The grant invoked** (§5).
- **Read marks:** how each based-on entry was read, and whether the fact depends on it.
- **The store's own id and incarnation.** C1 and C2 defuse the rest of this.
- **The hand's defaults.**

### Can wait

Per L70–75: index layout; physical encoding; one number or many, given the token; how many gates,
given compare-and-set plus epoch.

> Note. The envelope version and the opaque token buy the ability to change later. This is the same
> move as Sid's fixed/live line, applied inside the fact: fix very little, chosen so that everything
> else can move.

## 4. Its strongest challenge: re-deriving against rebuilds

**The argument (L77, L889–893):** "worked out again exactly from the same reads" is a claim about a
pair: this tool version on this runtime build. Temporal and Restate lived a decade by replay. They
can see a break only because old outputs are in the record to compare with. Every remedy they found
depends on the past ending. Sid's past never ends, and a running answer leaves nothing to compare
with. A rebuild could therefore change what yesterday's answer re-derives to, and nobody could tell.

**A sharper form in Sid's own terms (L247):** a rebuild moves no fact, so "redone when what it read
changes" never fires for it. That holds unless the rebuild is itself a fact that running answers
read.

**The three roads:** keep every build runnable for ever; promise unchanging behaviour and prove it
on each rebuild; or let crossings carry enough to check a later re-derivation (a digest) or to skip
it (the content).

**Round two picks the third road, split in two (L1048–1049):** a digest on every crossing; content
on crossings to a model or the host, because what stands on them cannot be worked out again.

**The year-four case:** someone disputes a summary over the papers. Build 37 is gone, and build 212
re-derives a prompt whose digest doesn't match. With a digest the place says honestly that it does
not know, for ever. With content it can answer.

**My addition:** leaves could be append-only like facts. A fix is a new leaf, and tools name leaves
by id. That gets road one at leaf size. The gate, the matching and layer resolution also shape
answers, so crossings still need the digest.

**Status:** early loss, not a corner. The first model call should still carry content, because the
earliest summaries are what the layers over ten million papers stand on.

## 5. What it raises that no handle holds

**The authority slot (T4, L1054–1071).**
- **The problem:** tools fire by matching, and nobody handed them authority for a purpose. A tool
  Alice enabled matches a fact Mallory wrote, then offers as Alice's agent. "The trigger chose the
  target, and the enabler's standing authority did the writing" (L1062). That is prompt injection at
  machine rate.
- **The fix:** every offer names the grant it invokes, by id and version. "The gate checks that
  grant and does not search" (L1068).
- **On a click:** a click designates and grants. The grant stands on the crossing's digest, so the
  record shows what she was looking at when she clicked (L1069).
- **Why I rate it highest:** it is the most valuable idea in the file for (15).
- **Cost, which the file doesn't price:** it is also an affordability mechanism. The gate's third
  check reads one cited fact at a stated position. It no longer searches policies.

> Note. This gives three walks of one shape. Based-on finds what a fact stood on, and that walk finds
> stale. Because-of finds what started it, and that walk finds the chain. The grant finds what
> allowed it: "everything done under a revoked grant is one walk" (L1063). Sid's text has the first
> two walks. The third is missing from the nine parts.

**The epoch (T5).** A gate's term of office opens with a fact in each ordered unit, and every
verdict carries its epoch. A verdict from epoch e that sits after the opening of e+1 is a
split-brain trace any checker can find. Safety rests on the compare-and-set, not on there being one
gate. Datomic had two live transactors at failover and it didn't matter (L646).

**Three outcomes, not two.** Ok, refused and unknown (L624). An offerer-made id plus one kept
verdict per id means "what happened to my offer?" always has a definite answer (L655). That is the
mechanism under Sid's no-optimism rule.

**Batches (L663).** If one offer ever carries several facts, a convention must be fixed early:
either each fact is checked against the state before the batch, or against the facts ahead of it in
the batch.

**Grammar and policy facts change nowhere at once across gate instances.** F1 needed leases and
in-between versions. Each fact should say which grammar version it passed, or a reader will assume
the newest (L592, L661).

**Nearest-wins is an override rule (L349).** Shadowing content is fine. A layer used to hide a fact
becomes deny-by-position, and "who can see this?" gets hard.

**Consumers of history set the write rate (L579).** "Tools matched to landed facts are exactly such
consumers." That supports my first-turn inference that matching is the affordability crux.

**Placement is not order (L902, L903).** A personal layer is a natural unit of placement, for law
and latency. Isolation in a shared store has to be earned.

**Check before building (L667–671).** Write a small formal model of the envelope's rules, with
invariants such as: every offer id has exactly one verdict; no chain forks; no based-on points past
its own as-of. Run the model with two gates and a failover. Test a tiny reference model of the gate
against every rebuild. This answers Sid's open "what to build first to test the line".

## 6. Against my first-turn trace

**The sixth line (affordable).** The file gives the nearest thing yet to a mechanism: a provenance
budget; "point at the crossing; do not list reads on every fact"; pattern plus position, never rows;
shared cuts; cited grants.

My refinement on matching: eager matching should run only for tools and for watched answers. A
historical pattern read should be checked lazily from its pattern and cut when someone looks.
Otherwise every pattern read ever made becomes a standing subscription for ever.

**The unwritten line about what stays a person's.** Handle-level answers only, so still open. (9) is
the erasure road. (16) becomes unreachable after genesis, by inherit-on-create (L771).

**Repair from inside:** nothing on it. Still open.

**Distance:** partly met by placement.

## 7. Where I push back

1. **"Never answer from an index that is behind" is too strong.** What the lineage refuses is an
   answer without a position. Zanzibar serves most checks at a default staleness (L305). The rule
   would be: every answer states the cut it is complete to.
2. **TigerBeetle's "only state-dependent refusals need keeping" doesn't transfer.** All three of
   Sid's gate checks read live facts, so every refusal depends on the store's state.
3. **Refusal volume is not priced.** Hot cells "will mostly collect refusals" (L575), but the file
   doesn't price that at Sid's scale. It also doesn't say whether a refused offer's content is kept
   or only its verdict.
4. **Read marks need a third value.** "Take the mark from the tool's definition" fails for a model's
   reply. The mark needs a value for "shown, dependence unknown".
5. **The opaque token is a new fixed thing.** It is a black box inside every based-on, so compare
   and read-as-of become built-in steps.
6. **Sid's ruled "a version on every fact" does three jobs in his text:** pointer target, cell
   order, and the gate's count. The file says never point by the gate's number. The ruling holds
   either way. What "version" means inside a pointer changes. Surfaced, not resolved.

## 8. The leans, as this file judges them

I know the leans only through this file's paraphrase.

- **Stand:** (0), as a promise about content; (2); (3); (4); (6); (10), as a cut per partition.
- **Sharpen:** (1): unfuse the name from the hash. (9): digest the ciphertext, and keep the actor
  id. (12): the unit is the epoch and the crossing, not the session.
- **Press:** (5): never record the withheld set. (7): the refusal is atomic with the
  compare-and-set, located in the cell's partition, and only layered to the session. (11): add a
  claimed "when", add a start mark, and point at the crossing. (14): select and point on; record pan
  and zoom only when a crossing's digest changes. (15): cited grants.
- **Reject one clause:** (17), the gate as a seed constant.

## 9. Carried to the other files

**`rama-marz.md`:** What is the log physically: the depot of offers with the gate a fold over it, or
a second depot the gate writes? If the gate is a fold, its "when" must be fixed at append, not read
inside a retryable topology (L495). Can an index state the depot positions it has applied? Can the
append path refuse a stale writer? Can the partition count change? How does index layout behave with
random ids? What shares a partition (C3)?

**`facts-datalog.md`:** Hickey's side of "sayings have ids and facts do not" (L1027). Attributes as
entities, for C4.

**`sync-versioning-defaults.md`:** content addressing's best case, set against C1.

**`log-as-truth.md`:** erasure in event-sourced systems; verifiable logs, for the strong form of
(0). The file names both as missing voices (L876, L881).

## 10. Status

One voice, and nothing here is a lean. C3 and the authority slot most need another voice before they
move.
