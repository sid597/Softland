# Plan validation — stage 5b, "reads, the rest" (PLAN-reads-rest.md)

<!-- Phase 2 of the rama skill (phase-2-plan-validate.md, template
artifact-plan-validation.md), 26 September 2026, 03:22 to 04:35 IST, by a
fresh-context validator session (Claude Opus 5.5, effort max) in worktree
/mnt/data/projects/Softland-rig-plan-reads-rest, branch rig-plan-reads-rest.
Validated: PLAN-reads-rest.md at 0f63dbe5. Read in full: SPEC.md; the plan.
Read in part, with line numbers cited: PROGRESS.md "Now" on main (rulings 2,
3, 6, 7; the forget and time-travel sharpening, l.146; the rig constraints,
l.167; "Open while the rig runs", l.172); CONCLUSION.md R3, R5, R6 (l.73 to
92); the main rig worktree's RIG.md defaults 1 to 4 and 6 and "For Sid";
README.md "The yardstick for the numbers" (l.126); PLAN-read-exit.md (the
address forms l.406 to 411, P13's head rule l.162 to 166, F2's page bounds
l.580 to 600); PLAN-locks-and-forgetting.md (OP9 l.747, $$persons l.715,
OP10); PLAN-micro-store.md, both this branch's revision (4f81068c, "not yet
validated") and the validated one in Softland-rig-plan-micro (848020f0: block
2b l.307 to 330, "never the plaintext"; the stream side frozen from the
re-class but for the grain switch, l.767 to 774 of this branch's copy); the
skill's dataflow.md "Yielding" and paths.md `set-elem`. No probe was run
(nothing here needed the cluster). Verbatim passages were gathered by a
read-only sonnet gatherer into this session's scratchpad and spot-checked
against the files. -->

## Verdict

**minor-fail.** Eighteen failures, each fixable by a localized edit; all
eighteen are fixed in the plan in place (commits 5f8ef235, 5e983412,
124bfe60, 0359c2c2 and 235ea524), numbered F1 to F18 and marked
`[F1]` ... in the text. No failure needs a new architecture: the one-task read, block 2d,
the poll, the drop through OP9 and the purge invariant all stand. One check
fails and is **not** fixed by an edit: the `|hash` indicator for a hot shared
layer (the placement question below). It is accepted for tonight as the named
rig choice RR5 under SPEC.md's rule for what can change without touching a
record, and it goes to Sid; F5 and F6 make that acceptance honest.

| fix | what failed | the edit |
|---|---|---|
| F1 | the re-classed layer's two eras are merged by raw address, and a stream-era address has `hex(stamp)` where a micro-era one has `hex(batch)`: `[:latest]` answers the replaced stream-era head after a micro-era replace | merge by `[pattern part, :stamp, fid]` from the entries; `[:kv]` by era, stream first |
| F2 | a delta cut by the scan budget with no match answers `:nothing-new` and advances `:scan` to the delta's moment: the facts past the cut are never delivered | the delta answers `:scanned-to` (a time part only); the next delta starts there |
| F3 | the closing query reads every delivery line's row: about 2.5 seeks per line at every close | each line carries the running closing fingerprint, count and mark; the close reads the last line |
| F4 | a drop page of 256 targets is a non-yielding stream event of about 1,300 seeks (0.4 to 0.65 s) on the agent layer's home at every dropping close | 64 targets per act (about 330 seeks) |
| F5 | Option D's bucket count is "a setting" of the layer, a fact: the named upgrade would change a record's shape | the bucket count is index maintenance state, never a fact |
| F6 | the placement section never measures Option B against the yardstick, and presents D as the road for a hot layer, which it is not for key-scoped reads | the yardstick arithmetic, the plain statement, the question for Sid |
| F7 | membership by `(view contains? *for)` over a subindexed set, fallback `(set-elem *for)` "with a nil test": `set-elem` stops on an absent element (paths.md l.317), so the branch never emits | `(subselect (set-elem *for))`, one seek, `[]` for a non-member |
| F8 | the partitioning table lists only pattern reads; the delta tick, the most frequent operation, and the point read are missing; the quiet shared tick is counted 4 where the plan's own steps give 5 | every read category, proportions summing to 1, recounted |
| F9 | RR6's index error leaves an admitted act unindexed while reads of its layer still say `:complete` | a per-layer `:ix-error` flag; a shared read of a flagged layer is `:partial` until the rebuild |
| F10 | "time travel on a shared layer is a read at an older F" is untrue for a re-classed layer: F bounds no stream-era fact | stated in step 4, step 6 and "What stays open" |
| F11 | a drop interrupted by the closer's crash is "repaired by running it again", with nothing that tells anyone to | the recovery finds pending drops from the record |
| F12 | the micro `:ix-kv` address carries the value's full canonical text from the arrival task to hash(L), breaking phase 3's validated "never the plaintext" onward from block 2b, where a keyed digest does the same work | the micro value index is keyed by an HMAC of the canonical text, computed on the arrival task; no plaintext leaves it |
| F13 | the micro rebuild's put page reads each entity's `:answers` and `:log` whole (`subselect ALL`) before its 4,096-row cap applies: a page's work is unbounded for a long-lived entity | two bounded ranges from the page's offset, walked together by name |
| F14 | the progress row types `:cursor` as a String, while a sweep page's cursor is `[L a]` and a put page's an entity with a row offset | the cursor is a vector |
| F15 | the restore replays a shared layer's forgets as `:replay-forget` "on the layer's ops depot", but `*micro-index-ops` defines no such op | the micro op, its route (hash(L), hash(e) for the ledger's date, back) and its idempotence |
| F16 | the validated micro plan's M25 sends a forget of a re-classed layer's stream-era value to the stream gate; the plan's five paths never name it, the one-owner person purge's seam covers "one-owner layers" only, and the carried moment question names P16's settings but not these forgets | path 1 names the stream-era target; the person purge covers every layer in `$$layers`; the carried item names M25's forgets |
| F17 | the person purge is "called by phase 2's `forget-person!`", a change to phase 2's client that no list of changes names | a wrapper of this stage calls phase 2's `forget-person!`, then `purge-person!`; phase 2 untouched |
| F18 | a standing read opened on a one-owner layer and held across its re-class (the base's, at the first group, default 6) meets the shared path's refusal of a stamp moment (RR1) at every later tick: `{:refused :moment-kind}` for ever, nothing shown, silently | the query answers `:reclassed`; `deliver!` closes the entry `:closed-by :reclass` and reopens it on a frontier moment |

## The placement question (builder A's first trace)

**What the plan chose.** Every index of a shared layer on `hash(L)` (Option
B, RR5): `:ix-ek`, `:ix-ke`, `:ix-kv`, `:ix-s`, `:ix-of`, `:ix-id` as
fields of `$$micro [L]`, one hop per admitted act in block 2d, no seek at
write, every read on one task.

**What one task carries for a large layer, at the yardstick** (README.md
l.126: "tens of thousands of people and their agents on one problem; a field
of about ten million papers"). Arithmetic under stated assumptions, not a
measurement:

- *Bytes.* The plan's own figure is about four times a fact's bytes on the
  layer's task, six for a value-indexed one ("Resource usage", shared). A
  micro fact's row is its sealed value plus the wrapped lock record (169 B
  raw for one person, 233 B for two, BENCH_NOTES-locks.md l.50) plus a
  32-byte digest and its fields: about 400 to 500 B for a short value. The
  base with ten million papers at ten facts each is 10^8 facts: the log is
  spread by entity (about 50 GB over N tasks), and its index, about 2 KB a
  fact, is **about 200 GB on one task**, growing for years.
- *Reads.* A shared read costs 6 to 9 seeks on that task (F8's table; 13
  for a five-id point read); one
  task thread serves about 2,000 to 3,300 uncached seeks a second (0.3 to
  0.5 ms each, the skill's figure). Ten thousand members' renderers each
  holding one standing read at one delivery a second is 10^4 × 6 = 6 × 10^4
  seeks a second on **one thread**: twenty to thirty times what it can serve
  uncached, and still about three seconds of work a second at 50 µs a cached
  seek, before opening a single value (an unwrap and an AES open per shown
  entry, on the same thread).
- *Writes.* Ten thousand people's acts at one every ten seconds is 1,000
  acts a second into the layer: 1,000 partitioner messages and about 10,000
  puts a second on one task, and one RocksDB instance compacting a
  multi-hundred-gigabyte index.

**Does it honour ruling 2's intent?** Ruling 2 (PROGRESS.md l.73): "Placement:
by layer for one-owner layers, by entity for shared ones ... a hot layer can
be re-classed." Its letter is kept: rows, log, heads and answers stay by
entity. Its intent, that a shared layer's load spreads, and that a hot layer
is moved to where it spreads, is **not** kept for the index and for every
read: they re-concentrate on `hash(L)` exactly the load the re-class was for.
The plan's difficulty log (item 1) sees this ("looks like undoing that") and
settles it by the partitioning rule for key-scoped reads; that settles the
choice among A, B and C at N = 128, not whether B holds at the yardstick.

**Is Option D the road for a hot layer?** Only for its writes and its
entity-scoped reads. Every key-scoped read, `[:all]`, and every standing
read's delta over `:ix-s` still visits every bucket, so each bucket serves
the whole key-scoped and delta read load of the layer: the read concentration
above is unchanged by D. The store core's placement needs more than D: for
example entity-scoped indexes beside the rows on `hash([L e])` (one more task
per read, for the settings and members), value lookups on `hash([L k
digest])`, key-scoped on `hash([L k])`, and the doorbell (RR9) so that quiet
deltas do not reach the index at all. That is a design, not a fix, and it is
Sid's to open.

**Can the upgrade come later without touching a record?** Yes, once F5
lands, for four reasons checked against the plan: (1) every index is
rebuildable from the log (the micro pass, "Rebuild from the log, shared"),
so a different placement is a rebuild; (2) a recorded `{:frontier F}` is
valid on any task, because F is committed on every task (a batch starts only
after every task committed the one before, "What Rama showed" 7), so a
fan-out read at F is still one moment; (3) the recorded `:exact` order and
the fingerprints are functions of the entries' fields (entity, key, batch,
stamp, fact id), not of where the entries sit, and a merge of sorted buckets
by address reproduces one list's order; (4) complete reads reproduce
exactly, and a partial read's shown subset may differ for a *new* read under
a new placement while the records of old reads stay true. The one thing that
would have touched a record was D's bucket count as "a setting" of the layer,
a fact whose shape would change: F5 moves it to maintenance state.

**My call.** Acceptable **for tonight**, as the named rig choice RR5, and
**not** as the store core's placement. For: SPEC.md's rule, "pick the
simplest thing ... applies only to what can change without touching a
record", which the placement meets after F5; the in-process cluster cannot
exercise the yardstick; and nothing the rig tests (the frontier, the
entries, forgets reaching every index) depends on where the index sits.
Against, and said plainly in the plan by F6: at Sid's scale Option B does not
honour ruling 2's intent, D does not fix it for reads, and the rig must not
be read as having settled index placement. The `|hash` check below stays
FAIL for that reason; it is accepted, not passed.

## The other traces builder A named

### The frontier: F = min(asked, the frontier on that task)

Concrete history, N = 4, group layer `:g` whose home is task 2. Act A, two
facts, one about `e1` (task 1) and one about `e3` (task 3), is decided in
batch 10. Block 0 of batch 10 writes `:frontier 9` on every task (micro plan
l.160); block 2d writes A's entries on task 2 with `:batch 10`.

- *One task.* Task 2 commits batch 10 at t1, task 3 at t2 > t1. A read on
  task 2 at t1 < t < t2 finds Ft = 9, so F = 9: both of A's entries are
  hidden (the batch filter, step 4), though they are on disk there. After
  every task commits 10, batch 11 starts; task 2's commit of 11 writes
  `:frontier 10` and a read then shows both facts. The price is one batch of
  lag (the micro plan's M7). Asked `{:frontier 10^9}`: clamped to Ft, as
  RT2 checks. Asked `{:frontier 5}`: every entry with batch ≤ 5, every
  tombstone committed on task 2 (erasures cross the frontier: a tombstone
  keeps its act's batch, so it is shown exactly when its fact would be, with
  only its date), and, for a re-classed layer, the whole stream era (F10).
- *Two tasks.* A value forget of A's `e1` fact in batch 20 writes the lock
  effect and the ledger on task 1 and the tombstone on task 2. If task 1
  commits first, `micro-act` on task 1 shows the lock excised while the exit
  on task 2 (F = 19) still opens the index copy: two reads, two moments, each
  consistent with its own; neither is half of batch 20. The forgetter sees
  its `:yes` at a frontier of at least 20 (phase 3 hides answers above F), by
  which time every task has committed 20, so no exit read after the answer
  can open the copy. Nothing relies on seeing batch 20 everywhere at once.
- *Cannot see.* Anything whose batch is above F; a member removal (none
  exists, RR2). *Can see and should not be relied on:* a re-classed layer's
  stream-side facts after the re-class (the grain switch, micro plan l.773,
  "the grain switch is the named exception"), bounded by neither F nor a
  recorded stamp: carried, correctly, as PROGRESS l.172's open item.
- *The re-classed base merged by address.* **Fails (F1).** A stream-era
  address is `e ␀ k ␀ hex(stamp) ␀ fid` (read exit l.406); a micro-era one
  `e ␀ k ␀ hex(batch) ␀ hex(stamp) ␀ fid`. Stamps are ms × 65536 (default
  2), about `0191…` in hex; batch ids are small, `000000000000002a` for
  batch 42. A stream-era head h0 of `(e7, :title)` and its micro-era replace
  h1 (batch 42, larger stamp by the clock promise): the string comparison
  meets `0191…` against `0000…` at the third part, so h0's address is the
  larger, and "the merge keeps the one entry with the larger address" returns
  h0, the replaced title. `[:ek e7 :title]` lists h1 before h0. F1 merges by
  the entries' `[pattern part, :stamp, fid]` for `[:e]`, `[:ek]`,
  `[:latest]`, `[:k]` and `[:all]` (each era's list is already in that order
  inside its pattern part, since inside one entity batch order is stamp
  order), which is P13's head rule across both eras; and for `[:kv]`, whose
  micro order across entities is by batch (FRR8), the stream era then the
  micro era inside the one `(k, v)` group.

### Plaintext carried to hash(L)

The plan (step 1 of "Written in which batch") takes `k ␀ len ␀ vtext ␀` on
the arrival task and carries it, inside the batch, to `hash(L)`, where it is
stored in `:ix-kv` addresses. Against the sources:

- *Default 1* (RIG.md l.184: "nothing that could open a value sits in the
  depot"): not broken. A partitioner hop is not a depot, and no depot
  record gains a value.
- *Phase 3's discipline* (validated micro plan l.314 to 318: after the name
  task's check "the sealed bytes, the wrapped lock record and the value
  digest go on to the entity task (never the plaintext ...)"): broken, by the
  plan's own naming. In the micro store the index would then be the only
  plaintext at rest, and the only plaintext that crosses a task (in
  production, a worker boundary).
- *What a forget must then reach:* the `:ix-kv` addresses on `hash(L)`
  (value purge in the forget's batch; person purge in pages), and RocksDB's
  deleted bytes until compaction (named for a kept store). Nothing in
  transit persists: a failed batch is recomputed from the depot.
- *The alternative, constructed.* The only form that reads `:ix-kv` is
  `[:kv k v]`, an exact match; F5 already verifies every candidate by
  opening it. A keyed digest of the canonical text, `hex(HMAC(index secret,
  canonical v))`, computed on the arrival task in block 2b and carried in
  place of the text, serves the same read (the query computes the digest of
  v in the module), with the same seeks, fewer bytes (64 characters in place
  of the text), no change to phase 3's discipline and no plaintext anywhere
  in the micro store. The purge invariant still requires the person purge to
  delete it (a rebuild cannot compute the digest of a value that no longer
  opens), so "nothing retained confirms a forgotten value" holds as before.
  The plan did not construct it: FAIL under "no dismissal without
  construction", fixed by F12. The one-owner store keeps the read exit's
  text form (it never crosses a task); the re-classed `[:kv]` read computes
  each era's prefix with its own function. F12 also removes a second
  crossing the draft had: the micro sweep page sends each entry to its
  entity task to check it against the log, and an `:ix-kv` entry's address
  would have carried the text there and back.
- *The stream era of a re-classed layer* keeps the read exit's text form in
  `$$layers`; its forgets go through the stream gate (M25) and its person
  purge through the one-owner pages, which F16 extends to it.

### Standing reads

Each clause of R6 (CONCLUSION l.85 to 92), traced:

- "one entry, opened with its pattern, role and frontier": FRR1, one act;
  the moment is the initial read's. PASS.
- "a stamped line with the delivered fact ids ... exact for a person or a
  model, and a fingerprint for a deterministic tool": FRR2's `:exact` and
  `:fingerprint`, ruling 3's split. PASS.
- "at the rate the person is shown things": the poll is the delivery; the
  store does not pace it. The proxy was constructed and costed (four
  reasons; the doorbell's numbers). PASS.
- "A delivery of nothing new adds no line": `:nothing-new`, nothing offered.
  The line rule holds; **the delta that feeds it FAILED, losing facts (F2,
  fixed)**: Alice's tool holds
  `[:k :note]` on her layer with limit 10, so the scan budget is 176;
  between two ticks her agent admits 500 `:status` facts and then one
  `:note`. The delta over `:ix-s` scans 176 `:status` entries, matches
  nothing, is cut by the budget; `deliver!` answers `:nothing-new` and
  advances `:scan` to the delta's moment, above the `:note` fact's stamp. No
  later delta starts below it. RT9's "the union of the delivered sets equals
  the final full read's matched set" would fail. RR10's resume covers a cut
  with a shown row, not a cut with none.
- "closes at unsubscribe or session close ... with a fingerprint over
  everything delivered and a mark": FRR3. A fingerprint over the lines'
  `[moment fingerprint]` is a keyed commitment to everything delivered, in
  order, and recomputable from the record for every reader kind; it is a
  reading of R6's words (first-record, for Sid), and a sound one. **Its cost
  fails (F3)**: `standing-close` reads every delivery line's row (`:no-copy`,
  1 to 2 seeks) and its lock row (own-row, 1): an agent's standing read over
  an hour at one delivery every two seconds is 1,800 lines, about 4,500
  seeks, 1.4 to 2.3 s of one query on the working layer's home at every
  close. The delta query already runs in the module once per delivery; given
  the handle's previous running value it returns the next, `{:fp (HMAC
  secret "softland.standing-fp/1\n" ‖ prev-fp ‖ canonical [moment
  fingerprint]) :n (inc n) :partial? (or p this-partial)}`, and the line
  carries it. The close then reads the last line only (a tail read, its row,
  its lock row: about 4 seeks whatever the number of lines). Per delivery:
  one HMAC and about 100 bytes, no seek. Less total work at every size:
  adopted.
- *Held across a re-class* (**FAILED, F18, fixed**): Alice's renderer holds
  a standing `[:k :title]` on the base, opened while the base is one-owner
  (moments `{:stamp s}`). The operator makes the first group; default 6
  re-classes the base. The next `deliver!` sends `:after {:stamp s0}`; the
  base's class in force is now `:by-entity`, the query takes the shared
  branch, and RR1 refuses a stamp moment: `{:refused :moment-kind}`,
  nothing shown, both moments kept, the same at every tick. RR12 covers a
  standing read *opened* on a re-classed layer, not one held across the
  re-class.
- "An entry left open by a crash is closed when the session closes":
  `standing-open`'s set difference and `close-session!`. PASS. Who runs it
  after a crash is the session's next door or the operator: named (F11
  extends the same recovery to a pending drop).
- "Nothing shown before its line is acknowledged" (R5): `subscribe!` and
  `deliver!` return rows only on `:yes`; a `:no` shows nothing and keeps both
  moments. PASS. One caller per handle: a handle used from two threads could
  offer two lines for one delta; the handle is the caller's, named in F2's
  edit.

### The close act

- Keep or drop, keep when the act says nothing: FRR5, a part of phase 2's
  `:session-closed` value, read by the closer (RR13). PASS; the parser change
  is phase 2's and small.
- A drop as phase 2's value forgets: OP9 acts (locks plan l.747), one fact
  per target, `:because-of` the close act, answered by name, resumable
  because `entry-ids` lists only what is not yet erased and a second forget
  "changes nothing". The mechanism holds. **The page size FAILED (F4,
  fixed)**: 256
  targets × (the row, the answer's stamp, the ledger, the lock row, `:ix-of`,
  five purge writes) is the plan's own "about 1,300 seeks ... about 0.7 s"
  in one stream event. OP9 reads and writes the ledger per target, which is
  the kind of path dataflow.md says not to yield on ("Do NOT yield on a path
  where correctness depends on same-key events processing in order"), so the
  event holds the agent layer's home for its whole length; that task also
  hosts other layers, people's among them, and a dropping close is routine
  (README l.126: hundreds to thousands of agents per person). At 64 targets
  the event is about 330 seeks (0.1 to 0.16 s), the order of the read exit's
  accepted 256-act rebuild page (read exit l.593); the forget facts are the
  same in number, the acts four times as many.
- Who closes: an agent cannot forget in its own layer (OP9's `:who`); the
  drop is the owner's door's or the operator's. **Recovery fails (F11)**: a
  closer that crashes mid-loop is "repaired by running it again", but
  nothing makes anyone run it. The record has what recovery needs: the
  `:session-closed` fact with `:reads :drop` (found by the `:ix-ke` prefix
  `session-closed␀`) and `entry-ids` with n = 1 (anything left?).
- Entries openable until the loop ends: RR15, for Sid. PASS as named.
- Every entry marked `:own-row` at write: ruling 7 (PROGRESS l.91 to 100, "A
  mark on any value overrides. Excision is the operator's fallback for
  unmarked record-default values") supports the mark and the plan's reason.
  What it costs, per read: one lock row per `:read/*` fact (a point or
  pattern read: one; a standing read: 2 + d), 169 B each, moved from the
  row's `:lock`, not added; one more put at write, one more seek at every
  open of an entry (the close query, a later audit, a drop's OP9), one
  delete at a drop. At the README's 50 full-speed agents per task, each
  reading once per model call, about 50 lock rows a second per task. PASS;
  phase 7 is told (M1's "agent layers write no lock rows" no longer holds).

### Purge and rebuild

Traced in the micro store with one value-indexed fact f in `:g`, admitted in
batch 10 at stamp s1, whose wrap requires Bob:

- *Person forget, then value forget.* Bob forgotten at s2 on `:people`'s
  home; the person purge's page on task 2 opens f's `:ix-kv` entry, finds
  the wrap closed, deletes f's `:ix-kv` and `:ix-of` entries, leaves the id
  indexes. Then the value forget in batch 20 at s3: block 1 finds no ledger
  entry on hash(e); the lock effect and `:erased {s3 :excised}` on hash(e);
  block 2d tombstones f's `:ix-ek`, `:ix-ke`, `:ix-s` entries with
  `:erased-at s3` and finds no `:ix-kv` left. A rebuild then: the ledger
  says s3, so a tombstone dated s3 and no `:ix-kv`. Equal.
- *Value forget, then person forget.* Tombstones dated s3 and `:ix-kv`
  deleted in batch 20; the person purge finds nothing (a tombstone has no
  `:ix-kv` entry). Rebuild: s3. Equal.
- *Restore, rebuild, replay.* The restore fact in `:people` (FRR7); the
  indexes emptied; the put pages write from the log, the lock store and the
  ledger as they stand; the replay of every `:forget` and `:forget-person`
  fact in stamp order purges again anything a restored lock reopened. By the
  invariant the fields equal those of a store that never restored (RT8).
  PASS.
- *A put page and a forget in one batch:* the put page never overwrites a
  tombstone and every micro rebuild ends with the forget replay. PASS.
- *Each stage's index:* the one-owner five (`:ix-s` new), the micro six,
  phase 2's `:by-stamp` (ids only, kept), the ledger, the heads. PASS.

Second pass, three histories the first pass had not run:

- *The base's stream era and a person forget (F16).* The base is
  re-classed; a stream-era fact f0 about paper p7, key `:abstract`
  (value-indexed), sits in `$$layers [base :ix-kv]` with its text in the
  address, its wrap requiring Bob (7b). Bob is forgotten. The one-owner
  person purge asks phase 2's `dying-with>` for "every value on this task in
  a one-owner layer": the base is now by-entity, so f0 is not listed, and
  the micro person purge reads only `$$micro`. f0's text stays in the index
  for good, against "so a forget reaches it". A value forget of f0 is
  routed by the validated micro plan's M25 to the stream gate, so path 1
  reaches it, but the plan never said so. F16 names both, and names M25's
  forget facts as stream-side facts after the re-class.
- *A restore with a shared forget (F15).* A value v in `:g` forgotten, then
  a restore: the replay sends `{:op :replay-forget ...}` to
  `*micro-index-ops`, whose parse knows only put, sweep and person-purge
  pages, so the record is dropped as data. In the rig the rebuild already
  tombstones v from the ledger, so RT8 would still pass; in a kept store
  where a restore brings back v's lock, v would be indexed again and never
  purged. F15 defines the op.
- *A long-lived entity in a rebuild (F13).* A paper of the base touched by
  years of acts has 100,000 rows; each put page that reaches it reads all
  100,000 (`subselect ALL`) and takes 4,096: 25 pages, about 2.5 million
  row reads for one entity, against 100,000 for bounded ranges from the
  page's offset. F13 reads the ranges; F14 types the cursor that carries
  the offset.

### The changes asked of the other builds

Each checked against its plan:

- *Read exit* (items 1 to 9, plus F2's `:scanned-to` and F3's running
  closing value on `read-pattern`'s answer): each adds a branch, a field, an
  input or a list entry to functions the read exit already has; item 9
  changes one rule of the rebuild's put page (one local ledger seek). Small
  and possible.
- *Phase 2:* `:reads` in the `:session-closed` value parser; the purge call
  site now also tombstones `:ix-s` (the read exit's function, called as
  before); the `dying-with>` seam with a named fallback, whose contract F16
  widens to every layer in `$$layers`; the replay seam, which in the rig
  changes nothing but the purge (L17). The first draft also had phase 2's
  `forget-person!` call the purge, unnamed: F17 moves that call into a
  wrapper of this stage. Small and possible.
- *Phase 3:* block 2d after block 2b's name-task check (the validated block
  2b already routes, after that check, from the arrival task to the entity
  tasks; block 2d is one more route, to hash(L)); the schema merge;
  `$$micro-task :rebuild` and `:layers`; the third source; the forget's
  fact ids and date; the `:replay-forget` op on `*micro-index-ops` (F15);
  and, after F12, **no** change to its plaintext discipline (the digest is
  computed where the plaintext already is). F16 takes M25's routing as the
  validated micro plan has it. Small and possible. Note for builder A: the plan read the validated micro plan
  at 6b12638c in Softland-rig-plan-micro; this branch carries 4f81068c, "not
  yet validated"; the build must follow the validated revision.

## Template checks

### Query topologies

- `read-pattern`, shared branch. Examples in "Query Topologies": `[:e e]`
  with 12 facts, 5 seeks before F9 and 6 after (stream settings, frontier,
  micro settings, members, `:ix-error`, one page), M = N: every read decides
  the branch, the moment, visibility, honesty or the rows. Variable, marked,
  with the read exit's doubling pages. PASS.
- `read-point`, shared branch: 5 fids, 2 never indexed: 5 + 5 + 3 = 13
  after F9; the `:ix-id` hop is meaningful (it finds the entity). A copy of
  the entry in `:ix-id` would save one seek per indexed id at the cost of a
  fourth full copy of every fact on the concentrated task (about 500 B a
  fact, about 50 GB for the base at 10^8 facts): rejected with those
  numbers, given RR5's concentration. PASS.
- `standing-close` after F3: 4 seeks whatever d. PASS (FAIL before F3).
- `standing-open`, `entry-ids`: one or two ranges in pages, ids only. PASS.

### PState schemas

- Groups by key and partitioner: every new field joins an existing PState
  with the same key and partitioner (`$$layers [L]`, `$$micro [L]`,
  `$$micro-task`). No new PState. PASS.
- Object types: none (addresses are Strings, `:e` a Keyword, `:fid` a
  vector, as the read exit's). PASS.
- Fixed-keys records for uniform values: `index-entry` is the micro row's
  fixed keys plus six. PASS.
- Subindexing: every per-layer map subindexed; `:ix-of` sets bounded by the
  value-indexed kinds (one tonight, enforced by the hints, a finite seed);
  `$$micro-task :layers` subindexed. PASS.

### Partitioning

- `|hash` on L for every index write: the keyspace is large (many layers)
  but a key can be hot. **FAIL**, accepted as RR5 (above), not passed.
- `(|direct *task)` for maintenance pages after a total parse: PASS.
- The table: after F8, every read category with proportions summing to 1,
  weighted totals recomputed, flat from N = 1 to N = 128 (every read on one
  task). PASS after F8.
- Placement schemes that store state (Option D) were considered and costed
  on total I/O. PASS; F5 moves D's state out of the record.

### Topologies

- Microbatch for block 2d (atomic with the rows across tasks): PASS.
- Stream only for what must be visible with the act's answer (the one-owner
  `:ix-s`); the person-purge and replay pages ride the gate topology because
  only it may write `$$layers`: justified by ownership, as the read exit's
  Option E. PASS.
- No choice made on test or observability grounds. PASS.

### Production readiness

- Concurrent clients: acts ordered by the gate and the batch; standing
  handles are per caller (one caller per handle, F2's note). PASS.
- Client restart: handles are not durable; the record closes the entry
  (`standing-open`, `standing-close`) and, after F11, resumes a drop. PASS.
- Worker restart: queries fail and are retried by the caller, nothing shown;
  batches retry whole; stream pages replay idempotently. PASS.
- Large scale: every collection subindexed; the hot-layer concentration is
  the accepted RR5. **FAIL at the yardstick, accepted for tonight as RR5**
  (the placement question), not passed.
- Non-idempotent stream writes: none (termvals and deletes at computed
  addresses). PASS.

### Internal depots, cross-topology, stream commit boundaries

- `*micro-index-ops` is operator-appended, not internal: no `:disallow`
  justification needed. No stream topology appends to a depot (RR18 would,
  with the commit boundary named). PASS.

### In-memory state

- No TaskGlobal; the standing handle lives in the caller. PASS.

### Minimality

The simplest design I can construct: the read exit's queries branch to a
shared path on the layer's task; block 2d writes index copies there; deltas
over a per-layer order index; lines as ordinary acts; the drop as OP9 acts;
purges and rebuilds as the read exit's, twinned. Every mechanism in the plan
is in that sketch or justified:

- `:ix-s` (both stores): deleting it makes `[:e]`, `[:k]`, `[:all]` deltas
  rescan their prefix every tick and go partial on a 10,000-fact layer (the
  plan's numbers). Kept.
- `:ix-id`: without it a shared point read must hop to the name task and
  the entity task. Kept.
- `$$micro-task :layers`: without it a person purge or a sweep cannot find
  a task's shared layers. Kept.
- `$$micro-task :rebuild`: a microbatch cannot `ack-return>`; the loop
  needs progress. Kept.
- `*micro-index-ops`: the rebuild and person purge must write `$$micro`,
  which only the micro topology may write. Kept.
- The three maintenance queries: the fingerprint secret never leaves the
  module; a crash close has only the record. Kept.
- The poll over a proxy: constructed and costed. Kept.
- The plaintext text in the micro value index: **removed by F12** (a keyed
  digest does the same work).

### Throughput

- The closing fingerprint: **FAIL, fixed by F3** (constructed above).
- The drop page: F4 lowers an event's length, not its total work.
- The delta: over `:ix-s` one seek plus one iteration per new fact. PASS.
- The doorbell: constructed, with numbers; the poll is cheaper for busy
  layers, the doorbell for quiet ones with many readers; the poll is the
  pick and RR9 the named change. PASS.

## Spec coverage

### "Point reads and pattern reads" on shared layers (phase 5)
- Source: SPEC.md phase 5, "Point reads and pattern reads".
- Trace: RS1 and RS2 above (5 fids, 2 unindexed: three rows, two
  `:absent`).
- Fault: a worker restart fails the query, nothing shown or recorded; a
  batch retry leaves no entry of an uncommitted batch below F.
- Race: two offerers in one batch are ordered by the fold; a batch
  committing during a page yield is above F and hidden.
- Flaws: F1 (the merge), F7 (membership), F9 (index error).
- Verdict: FAIL before the fixes; PASS after F1, F7, F9.

### "one read entry line per pattern read carrying pattern, moment, role and a keyed fingerprint ... complete-or-partial mark"
- Source: SPEC.md phase 5; ruling 3 (PROGRESS l.76).
- Trace: a shared read's line is the read exit's with `{:frontier F}` and
  `:max-stamp`; the entry is stamped above `:max-stamp` (RT2, on another
  task than `:g`'s).
- Fault and race: the read exit's (offer until answered, by name).
- Flaws: the complete mark can lie after an index error (F9).
- Verdict: PASS after F9.

### "eager by default in personal and shared layers"
- Source: SPEC.md phase 5; ruling 3.
- Trace: every read through the exit is recorded; standing reads record
  every delivery that shows something new.
- Verdict: PASS.

### "Every index over values rebuildable from the log or purgeable by value id, so a forget reaches it"
- Source: SPEC.md phase 5; the rig constraint (PROGRESS l.167).
- Trace: the purge histories above; the restore history.
- Fault: pages idempotent, resumable; a batch retries whole.
- Race: a put page and a forget in one batch (the tombstone guard and the
  closing replay).
- Flaws: none in the invariant itself; around it, F13 (an unbounded put
  page), F15 (a replay op never defined on the micro side), F16 (a
  re-classed layer's stream era outside both purges' named reach), and
  F12, which removes the plaintext the forget had to chase between tasks.
- Verdict: PASS after F12, F13, F15 and F16.

### "A microbatch is not visible on every task at one instant" ("What Rama showed" 5)
- Trace: the frontier history above.
- Flaws: none on one task; across two, two reads at two moments, named.
- Verdict: PASS.

### "a read as of a moment shows nothing admitted after it, except an erasure, which shows only its date" (PROGRESS l.146)
- Trace: tombstones keep their act's batch and carry only the date; a read
  at an older F hides every later batch.
- Flaws: F10, a re-classed layer's stream era is not bounded by F at all.
  Carried, as PROGRESS l.172 leaves it open; F10 states it.
- Verdict: PASS with the open item stated after F10.

### R6 standing reads (CONCLUSION l.85)
- Trace and flaws: above (F2, F3).
- Verdict: PASS after F2 and F3.

### R5 and default 4, agent-session reads kept or dropped at close
- Trace and flaws: above (F4, F11).
- Verdict: PASS after F4 and F11.

### "Every forget and every restore is a fact; forget facts are replayed after any restore" (PROGRESS l.169)
- Trace: FRR7's restore act; the replay in stamp order across stores.
- Verdict: PASS.

### "A gate never throws on an offer; every refusal is data" ("What Rama showed" 3)
- Trace: every new pure function total; the ops parsers total before
  `|direct`; RT13.
- Flaws: RR6's catch keeps the gate alive but hid the gap from reads (F9).
- Verdict: PASS after F9.

## For Sid (each touches a record or a ruling's intent)

1. **Index placement for shared layers at the yardstick** (F6): ruling 2
   places shared layers by entity; the rig places their indexes by layer.
   Tonight that is a rig choice; for the store core it re-concentrates a hot
   layer's index (about 200 GB for the base at ten million papers) and every
   read of it on one task thread. Which placement should the store core
   have?
2. The plan's own list, unchanged: FRR1 to FRR9 (with F3's running value in
   FRR2 and F4's 64 in FRR9); maintenance reads without an entry (RR11); a
   drop effective at the close act or per forget (RR15); delivering a forget
   of a delivered fact; the closing fingerprint's reading; what a restore
   records.
3. The two-store moment (PROGRESS l.172), now stated whole (F10, F16).
4. A read that pages across a commit on its task can show one erasure of a
   batch and not another of the same batch (each page is its own select;
   `:allow-yield?` keeps one select on a stable snapshot, not a loop of
   them). It is a case of RIG.md "For Sid" 10, "a value forgotten between
   the query and the entry's acknowledgement is still shown tonight", and
   is left there, not fixed here: one select over the whole budget would
   read up to sixteen times more than the doubling pages when matches are
   dense.
