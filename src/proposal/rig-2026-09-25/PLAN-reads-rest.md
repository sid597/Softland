# Plan — stage 5b, "reads, the rest": frontier reads, standing reads, the close act, indexes and forgets whole

<!-- Phase 1 of the rama skill for the rest of Sid's phase 5, written
26 September 2026, 02:40 to 04:10 IST, by a fresh-context session (Claude
Opus 5.5, effort max) in worktree /mnt/data/projects/Softland-rig-plan-reads-rest,
branch rig-plan-reads-rest. Plan only; no code, no probe was needed (see
"Probes"). Sources read: SPEC.md (phase 5, "What Rama showed"); RIG.md on
the main rig worktree ("Overnight state", "For Sid", "Defaults taken
overnight"); PLAN-read-exit.md (whole, with its F1 to F12);
PLAN-locks-and-forgetting.md ("Interfaces tonight's parallel stages build
against", "Reads", "Writes", "PState Design", "What later stages consume",
L16 to L18, the shapes of the lock row, ledger and person lock);
PLAN-micro-store.md at 6b12638c in /mnt/data/projects/Softland-rig-plan-micro
(the committed revision: §A block 1 and 2b, §D, "The shapes", "Reads",
"Writes", "PState Design", "Depots", "Query Topologies", "The settled
frontier, stated whole") and PLAN_VALIDATION-micro-store.md at the same
commit (verdict, T6, "For builder A and Sid"); IMPLICIT_SPEC.md RD3 to RD7;
on main PROGRESS.md "Now" (lines 9 to 231) and
store-next-2026-09-25/CONCLUSION.md R1 to R8 and its open questions; the
rama skill's phase-1-plan.md, artifact-plan.md, depot-design.md (internal
depots, ack levels), the reference snapshot's 15-pstates.md "Reactive
queries" and "Fault handling". The micro plan's worktree had uncommitted
edits by its validator when this was written; this plan reads its commit
6b12638c, not the working copy. Where a line here summarises a source, the
source wins.

Vocabulary. "Key" is a fact's key only. "Lock" is an encryption key only.
The HMAC input is "the fingerprint secret" (the read exit's word). The sorted
String under which an index entry sits is its **address**. A **line** is one
`:read/*` fact of a read entry.

Status words, as the read exit uses them. **Ruling**: Sid's, PROGRESS.md
"Now". **Default**: one of CONCLUSION's R1 to R8 as tonight's brief takes
it, or a default numbered in RIG.md "Defaults taken overnight"; none is
ruled. **Rig choice**: a pick that can change without touching a record
(numbered RR1 ...). **First-record**: a pick that touches what a record
carries; a placeholder for edition one (numbered FRR1 ..., continuing the
read exit's FR1 to FR14).

Checked, derived, assumed. **[docs]** the rama skill's references or the
reference snapshot say so, not run here; **[phase-1 ran]**, **[probed]**
as the read exit uses them, citing its runs; **[build checks]** unverified,
the build runs a check first and the fallback is named. Everything else is
derived in this plan from the sources named. -->

## Scope of this stage

The read exit (PLAN-read-exit.md, validated, being built in
`Softland-rig-build-reads`) is the one way to read one-owner layers. This
plan adds, on top of it and of phases 2 and 3 as their plans stand:

1. **Shared-layer reads through the settled frontier.** Point and pattern
   reads on group layers, and on the base after its re-class (default 6),
   through the same exit, as of a settled frontier F, so no reader sees half
   a batch (RIG.md R5; "What Rama showed" 5). The entry records
   `{:frontier F}` in the moment slot (CONCLUSION R3, FR1). The indexes the
   micro store keeps for pattern reads across entities: what, where, written
   in which batch, purgeable by value id, rebuildable from the log.
2. **Standing reads** (CONCLUSION R6 as written, a default), for one-owner
   and shared layers, through the one exit, nothing shown before its line is
   acknowledged, on a poll at the delivery rate (why a poll and not a proxy:
   "Standing reads", Mechanism).
3. **The agent session's close act** (CONCLUSION R5, default 4): kept or
   dropped; kept when the act says nothing; dropped means forgotten through
   phase 2's value forget. The read exit marks every entry `:own-row` at
   write, so a drop is an ordinary row-deleting forget (why: "The close
   act").
4. **Indexes and forgets, whole**: a value forget purges by value id in
   both stores; a person forget purges every value that dies with the
   person in both stores; a restore is a fact, rebuilds from the log in
   pages, then replays the forget facts after it. The one-owner indexes, the
   micro store's, and phase 2's `:by-stamp`.

Not this stage: grammar facts as index hints and tools' signatures (phase
6); the numbers (phase 7); replays of scenarios.clj (phase 8); a stamp
moment on a shared layer (see "What stays open"). Later stages are black
boxes. Phase 2's and phase 3's plans are interfaces: this plan names the
smallest contract it takes from each, and where its plugs sit in their
events; it never relies on how their unbuilt parts work.

## Probes

None run. Every Rama behaviour this plan leans on is either probed or run
by earlier plans (String addresses and their ranges, the tail read with
`{:max-amt 1}`, vector-keyed page walks: the read exit's three probes; a
Long frontier id, stable across a retry: the micro plan's §G), or
documented and marked [docs] (two sources in one microbatch topology, which
phase 3 already uses for `*micro-tick`; internal depot appends and their
ack levels; proxies' restrictions), or left as a [build checks] with a
fallback named. The one behaviour a probe would have had to settle, a
proxy's delivery semantics, is not used (see "Standing reads").

## The shapes every section below uses

Everything the read exit fixed is reused unchanged: ids (a fact id is
`[name idx]`, FR13), addresses (parts joined by U+0000, stamps as 16 hex
digits, RC4), the six pattern forms and their parser (FR9), the answer row
and read answer shapes, the complete-or-partial rule (limit, scan budget,
F3, F8), the fingerprint and its secret (FR11, FR12), the reader's fields
(`:reader :for :reader-kind :rows? :working :permission :role`), the entry
as an ordinary act in the reader's working layer (FR3 to FR8), and F1's
rule that an entry is stamped after what it read. Only what differs is
stated here.

**The frontier.** Phase 3's (micro plan §D, "stated whole"): `$$micro-task
:frontier` on every task, the id of the last microbatch every task has
committed, a Long, written by block 0 of each batch as the previous batch's
id. Every micro row, record, head change, settings version and permission
change carries the `:batch` that wrote it; a reader at F hides everything
whose batch is above F; erasures cross the frontier (a forget's effect is
not hidden by F; the forget fact is). This plan adds: **every micro index
entry carries the `:batch` of the act it indexes** (the batch that decided
it), so the same rule hides it.

**A shared read's moment** (first-record, FR1 as the read exit left it):
`{:frontier F}`, one part, where F = `min(asked, the frontier on the task
the read runs on)`, `asked` being the reader's `:as-of {:frontier F'}` or,
absent, that task's frontier. Why the clamp, the same argument as FR2:
every batch above the task's frontier is either uncommitted somewhere or not
yet known settled, so a read at a larger F could grow; at F it cannot,
except for erasures, which show only their date. A shared read asked as of a
stamp (`{:stamp s}`) is refused as data, `{:refused :moment-kind}` (RR1):
"as of T stays one number" (CONCLUSION R3), and for a shared layer that
number is F. Carried, not closed: a read's moment across the two stores
("Open while the rig runs"); see "What stays open".

**Which layers are shared.** A layer is read on the shared path when its
class in force is `:by-entity`: a group layer (made on the micro gate; its
settings are in `$$micro [L :settings]` as batch-keyed versions, none in
`$$layers`), or a re-classed one-owner layer, the base above all (its
settings are in `$$layers [L :settings]` with `:class :by-entity`; the micro
gate reads them so, "`$$micro` first, `$$layers` on a miss"). Both stores
keep a layer's per-layer data on `hash(L) mod N`, the layer's task: the
stream store by P2, the micro store because `$$micro`'s key is the layer id
used as an entity and its default partitioner hashes it (micro plan,
"`$$layers` — read, never written": "on hash(L), the task `$$micro [L]` is
on too"). **That co-location is what this plan builds on: every read of a
layer, of either kind, runs on one task, the layer's.**

**A shared answer** adds to the read exit's read answer one field,
`:max-stamp`, the largest stamp among the matched (pattern) or read (point)
facts, 0 when none: the stamp the entry must stand on (F1, see "Shared
reads", step 7). Rows of a shared layer carry `:batch` beside `:stamp`.

## Shared reads: how the one exit reads a shared layer

### What changes in the read exit's two queries (one branch each)

The exit stays the one function, `read-exit/read!`, and its two query
topologies stay the entry points: both lead with `(|hash *layer)`, which
lands on the layer's task whatever its kind. After their step 3 reads the
layer's stream settings (`$$layers [L :settings]`), each gains one branch
(**a change to the read exit's build**, the only one in its query steps):

- the stream settings exist and say `:class :by-layer`: the read exit's own
  path, unchanged;
- otherwise: `(local-select> [(keypath :frontier)] $$micro-task :> *Ft)`,
  then this stage's path below, on the same task. A layer with no stream
  settings and no micro settings at or below F answers `{:refused
  :not-visible}`, the same answer a private layer gives (F4 kept: a reader
  cannot tell a group layer it is not in from a layer that does not exist).

Why one query per read kind and not a third query topology the exit picks:
the exit cannot tell a group layer from a missing one without asking (F4
forbids a query that says "shared"), so a separate query costs a second
round trip on every group-layer read or a kind cached in the client (the
CLAUDE.md rule on caching: nothing here needs one, because both kinds'
data sit on the task the first query already reached). The branch costs one
seek (the frontier) on the shared path only. On a group layer the read
exit's stream-settings read is a miss: that miss is the kind test (the
question "is this layer ordered by the stream gate?", answered no), the
same standing as the micro plan's faces read in `micro-lookup`. The upgrade
that removes it, a rig choice touching no record (RR19): an optional
`:hint :group` in the exit's call, from a caller that knows it reads a group
layer (a member holds the group's making fact), which makes the query read
`$$micro [L :settings]` first and `$$layers` only on a miss; it saves one of
about five seeks per group-layer read, and a wrong hint costs the same miss
the other way. Not taken tonight by rule 10 of the brief (the simplest pick
that changes no record); the table below counts the miss.

The shared path's pure functions and fragments live in
`rig.store.shared-reads`; the branch calls them. [build checks: a
`<<if` in a query topology whose shared branch calls a `deframaop` or
`deframafn` of another namespace that `local-select>`s `$$micro` and
`$$micro-task`; the read exit already relies on the same form for
phase 2's `open-row>`. Fallback: the branch's dataflow inline in
`rig.store.reads`' query definitions, calling the same pure functions.]

### The shared pattern read, step by step (`read-pattern` on a by-entity layer)

On the layer's task, after the branch:

1. **Moment.** `(shared-reads/moment *as-of *Ft :> *F)`: `min(F', Ft)`
   for `{:frontier F'}`, `Ft` when absent, `{:refused :moment-kind}` for a
   stamp moment. Total, as data.
2. **Settings as of F and visibility.** For a group layer,
   `(local-select> [(keypath *layer :settings) (sorted-map-range-to (inc *F) {:max-amt 1})] $$micro :> *sv)`
   (the micro plan's "a reader at F takes the last at or below F", one
   seek); for a re-classed layer, the stream settings already read.
   `(shared-reads/visible? *settings *for)`: the base is visible to any
   authenticated actor; a group layer to its members, read as
   `(local-select> [(keypath *layer :members) (view contains? *for)] $$micro :> *member?)`
   [build checks: `view` over a subindexed set; fallback `(set-elem *for)`
   with a nil test] (ruling 9's default; one seek). Membership is read as
   it stands, not as of F: the micro plan writes `:members` without a batch
   and has no removal (RR2); a removal, when it exists, needs a batch on the
   member row so that a read at F sees the members at F.
3. **Pattern.** The read exit's `parse-pattern`, the same six forms, the
   same hints (`reads/seed-hints`), the same refusals as data.
4. **The range read** over the micro index on this task (next section),
   with the read exit's page loop and its pure step, one change: the
   visibility test of an entry is `(<= (:batch entry) F)` in place of
   `(<= (:stamp entry) m)` (the rule of the frontier; an entry's stamp still
   orders the chain inside an address). `[:ek e k]` and `[:kv k v]` have no
   stamp bound in the address (their end is the prefix's end): the page
   step filters by batch. The limit, the doubling pages (RC3), the scan
   budget of 16 × (limit + 1) (F8) and the `:partial` mark are the read
   exit's.
   - `[:latest e k]`: the read exit's tail read `(sorted-map-range-to *end
     {:max-amt 1})` [probed] returns the entry with the largest address
     under `e␀k␀`; while that entry's batch is above F, the next tail read
     is bounded by that entry's address, in a `loop<-`, at most 16 times
     (then `:partial`, RR3). Why this is short: the entries above F are
     those of at most the batches not yet settled, about two batch cycles'
     worth (micro plan: the frontier lags "about one tick plus one batch"),
     and within one entity a later batch always gives a larger stamp (the
     micro stamp is at least the entity task's clock + 1, and every fact
     about `e` touches `e`'s task), so the hidden entries are exactly the
     top of the chain.
   - `[:kv k v]`: F5 holds: a kept entry is a candidate; it counts only
     when it opens (step 5) to v.
5. **Open.** Each kept entry goes through `shared-reads/open-entry>`, the
   shared twin of `open-row>`, on this task (next-but-one section).
6. **Re-classed layer, the stream era.** For a layer with stream settings
   (class `:by-entity`), the same pattern is also read with the read exit's
   own loop over `$$layers [L ix]`, bounded by limit + 1 matches, every
   stream-era entry counted (a stream event is atomic on its task, so no
   stream-era entry is half of anything; "shows nothing admitted after it"
   is F's to bound and F does not bound a stream fact, see "What stays
   open"), opened by `locks/open-row>`. The two sorted lists, each at most
   limit + 1 matches, are merged by address in memory
   (`shared-reads/merge-eras`, pure); the first `limit` are shown; the read
   is `:partial` when either loop was, or the merge had more than `limit`.
   For `[:latest e k]` the merge keeps the one entry with the larger
   address, which inside `e␀k␀` is the larger stamp (the head by P13's rule
   across both eras).
   Why a merge of two bounded lists and not one index: the stream era's
   entries are in `$$layers`, which only the stream gate may write, and the
   micro era's in `$$micro`, which only the micro topology may write (PState
   ownership); copying the stream era into `$$micro` at the re-class is a
   rebuild of a whole layer inside one batch or a paged copy with a window
   in which the index is incomplete. The merge costs at most one extra loop
   for re-classed layers only.
7. **Answer.** `(shared-reads/pattern-answer ...)`: the read exit's
   `pattern-answer` with `:moment {:frontier F}` and `:max-stamp`. The
   fingerprint is the read exit's function over the set of matched
   `[fid stamp]` pairs, unchanged (a micro fact's stamp is a
   `rig.store.clock` hybrid long like any other, default 2, so pairs from
   both stores compare and print alike).

### The shared point read (`read-point` on a by-entity layer)

A point read names fact ids. A shared fact's row is on its entity's task,
and the fact id `[name idx]` does not name the entity; the micro plan keeps
no name-to-entities index. So the micro index keeps one (below, `:ix-id`),
on the layer's task, and the point read never leaves it. Per fact id, in
the read exit's `loop<-`:

- a name tagged `:by-layer` (a re-classed layer's stream-era fact): the
  read exit's own steps (`$$layers [L :answers name]`, the row, `open-row>`);
- a name tagged `:by-entity`: `$$micro [L :ix-id fid]` (1 seek) → the fact's
  `:ix-ek` address, then `$$micro [L :ix-ek address]` (1 seek) → the entry;
  absent, or its batch above F, gives `{:fid fid :absent true}` (the three
  causes not told apart, as the read exit's point read); else
  `open-entry>` gives the row.

Variable reads, handled by the loop: one seek per id for an id never
indexed, two for one that is. Rows carry `:batch`. The answer is the read
exit's `point-answer` with `:moment {:frontier F}` and `:max-stamp`.

### `open-entry>`: opening a shared entry on the layer's task

The micro row holds the sealed bytes, the lock id, the wrapped lock record
and the value digest (micro plan §A, block 2b: "in the record", ruling 7),
and every index entry is a copy of its row (the read exit's F6 applied to
the micro row's fields). So a shared value opens on the layer's task from
the entry alone plus `$$persons`, which phase 2 keeps on every task:
`(shared-reads/open-entry> *entry *F :> *r)`, a `deframafn`:

1. an entry with `:erased-at` (a purge's tombstone, below) gives
   `{:erased-at s}`, and nothing is opened;
2. an entry with `:copy false` (a `:no-copy` key, F7) first reads its row
   on its entity's task: **this is the one hop a shared read can make**;
   it happens only for the store's own `:read/*` keys, which never live in
   shared layers (entries are written into the reader's working layer,
   always one-owner, FR3), so it does not occur tonight, and the function
   answers `{:unreadable :no-copy}` rather than hop (RR4);
3. else phase 2's pure steps over the entry's `:lock` and the wrap's person
   entries read from `$$persons` on this task (`[(keypath p)]`, one local
   seek per wrap person): `locks/unwrap` then `locks/open`; a wrap closed by
   a person forget gives `{:erased-at date}` with phase 2's person-forget
   date (`wrap-closed`); a lock that fails on the bytes gives
   `{:unreadable :does-not-open}`. Returns exactly the shapes of
   `open-row>` (the read exit's "open-value" section); never throws.

The entry's `:lock` holds the value's lock record wherever the row keeps
it: the row's own `:lock` (in the record, the shared default), or, for a
value marked `:own-row` in a shared layer, a copy of its lock row
(`$$micro [e :locks lid]`), which block 2b has in hand when it writes the
row. So every shared entry opens on the layer's task alone. A reading of
the rig constraint "lock rows sit on the same task as their values", named
for Sid: the index keeps, on the layer's task, a copy of the sealed value
and a copy of its wrapped lock together, and the forget's batch nils both
copies with the original (atomic across tasks), so a forget still reaches
every copy at once; the alternative, a hop to the entity task per opened
own-row entry, costs one network round trip and one seek per such entry on
every read.

Why the erasure ledger is not read here: the ledger for a shared value sits
on the value's entity task (`$$micro [e :erased lock-id]`, micro plan), not
the layer's; this plan's purge writes the forget's date into every index
entry of the value in the forget's own batch (below), so the entry carries
the ledger's date and the read never needs the ledger. A value forget and
its purge commit in one batch (a microbatch is atomic across tasks, "What
Rama showed" 5), so no reader sees the lock excised and the entry not yet
tombstoned, at any frontier: erasures cross the frontier, and both writes
are the same batch's.

## The micro store's indexes

### Placement: derived from the dominant reads, three options costed

The reads: the six pattern forms and point reads on one shared layer, as of
F. Entity-scoped forms (`[:e]`, `[:ek]`, `[:latest]`) read one entity's
facts in L; key-scoped forms (`[:k]`, `[:kv]`) and `[:all]` read across
entities. The write: every admitted act in L, whatever entities it touches.
The micro gate already visits the layer's task for every offer: block 1's
gather reads the settings and the permission row(s) on hash(L) (micro plan,
"Reads": "on hash(layer) the settings ..., the permission row(s)").

- **Option A: indexes beside the rows, per entity task.** Entity-scoped
  forms are local to hash(e); key-scoped forms and `[:all]` fan out to all N
  tasks: N seeks per read, growing with N. Rejected by the partitioning rule
  (weighted seeks must stay flat).
- **Option B, chosen: every index of a shared layer on the layer's task,
  `$$micro [L]`.** Every read is one task, a few seeks and one iteration per
  entry, flat in N; point reads through `:ix-id` on the same task; the
  stream era of a re-classed layer is on the same task (P2), so its merge
  is local; the micro gate already sends every offer of L to this task in
  block 1, so the index writes add one partition hop per admitted act in
  block 2 (not per fact), and no seek: every index write is a `termval` at
  a fresh address. The cost: a shared layer's index sits on one task; a hot
  layer's index bytes (about three times its facts' bytes, the read exit's
  arithmetic) and its index puts land there. Puts are memtable appends; the
  scarce resource is seeks, and the seeks this adds on the layer's task are
  zero at write and the reads' own at read.
- **Option C: split by pattern family.** Entity-scoped indexes on hash(e)
  (local to block 2b's row writes, balanced like the log), key-scoped ones
  on hash(L). The same hop count as B at write; entity-scoped reads pay a
  second task for visibility (settings and members are on hash(L)); the
  key-scoped part concentrates on hash(L) as in B, so it halves B's
  concentration at best, and it splits a re-classed layer's merge across
  two tasks. Not taken tonight.

- **Option D, stored placement: B cut into buckets.** A layer's settings
  carry a bucket count `B` (a setting, 1 by default); an entry lives on
  `hash([L (mod (hash e) B)])`. Entity-scoped reads go to one
  bucket: the same seeks as B, plus one hop for the settings when the
  bucket is not the layer's task. Key-scoped reads and `[:all]` fan out to
  B buckets: seeks 4 + B × pages instead of 4 + pages, and a merge of B
  sorted lists; writes spread over B tasks. Total I/O, the placement state
  included (B is read with the settings already read, so it costs no seek):
  for B = 8, a key-scoped read of 60 matches costs about 4 + 8 × 1 = 12
  seeks against B's 7, weighted seeks about 9.3 against 6.1 (0.5 × 5 + 0.3 ×
  12 + 0.1 × 20 + 0.1 × 12), and each of
  the 8 bucket tasks takes one eighth of the layer's index puts and bytes.
  Flat in N (B is the layer's, not the cluster's). It is the road for a hot
  layer, paid only by that layer; with B = 1 it is Option B exactly.

Chosen: B, as a rig choice (RR5), that is D with B = 1 for every layer,
because it is the simplest pick that can change later without touching a
record (the brief's rule 10): indexes are rebuildable from the log, so
moving a hot layer's indexes to D with B > 1, or to C, later is a rebuild,
not a record change. What would force the move: a shared layer whose index
outgrows one task's disk or whose index puts exceed one task's write rate;
phase 7 can measure the latter. The `|hash` indicator is met only in the
common case (many shared layers, none hot); a hot layer is exactly the
case D exists for.

### The fields, and what each serves

Added to `$$micro`'s fixed-keys value (the layer-as-entity's fields, beside
`:settings`, `:members`, `:permissions`, `:replaced`), by one form in
`micro.clj`'s schema: `(merge micro-fields (shared-reads/layer-fields
micro-row-fields))`. Owned by the micro topology, written only in its
batches (PState ownership: `$$micro` is declared by `micro`).

```clojure
;; the entry: the micro row whole (its :layer :k :v :replaces :mark :lock-id
;; :lock :digest, whatever phase 3 and 2 put in it, F6's rule) plus its own
(defn index-entry [micro-row-fields]
  (fixed-keys-schema
   (merge micro-row-fields
          {:e         clojure.lang.Keyword                   ; the fact's entity (the row's key, not in the row)
           :fid       clojure.lang.PersistentVector          ; [name idx]
           :stamp     Long                                   ; the act's stamp
           :batch     Long                                   ; the batch that decided the act (the frontier rule)
           :erased-at Long                                   ; set by a purge: the erasure's date
           :copy      Boolean})))                            ; false for a :no-copy key (F7)

(defn layer-fields [micro-row-fields]
  (let [entry (index-entry micro-row-fields)
        sub   {:subindex-options {:track-size? false}}]
    {:ix-ek (map-schema String entry sub)                    ; e ␀ k ␀ hex(stamp) ␀ fid
     :ix-ke (map-schema String entry sub)                    ; k ␀ e ␀ hex(stamp) ␀ fid
     :ix-kv (map-schema String entry sub)                    ; k ␀ len ␀ vtext ␀ hex(stamp) ␀ fid
     :ix-s  (map-schema String entry sub)                    ; hex(batch) ␀ hex(stamp) ␀ fid   (standing deltas)
     :ix-of (map-schema clojure.lang.PersistentVector (set-schema String) sub) ; fid -> its :ix-kv addresses
     :ix-id (map-schema clojure.lang.PersistentVector String sub)}))           ; fid -> its :ix-ek address
```

- `:ix-ek`, `:ix-ke`, `:ix-kv`, `:ix-of`: the read exit's four, the same
  addresses built by the same `reads/address` (RC4), the same forms served.
- `:ix-s`, new: every fact by batch, then stamp. A standing read's delta
  "what became visible since F0" is one range, `hex(F0 + 1)` to `hex(F +
  1)`, whatever the pattern ("Standing reads"). Ordered by batch first
  because the frontier, not the stamp, decides visibility here.
- `:ix-id`, new: the point read's entry point (above). Holds ids only (the
  address is entity, key, stamp and fact id), so a purge keeps it.
- No `Object`; addresses are Strings; size tracking off everywhere (a
  count costs a read per write); every map subindexed (unbounded per
  layer); an `:ix-of` set is bounded by the value-indexed kinds (one
  tonight), not subindexed.

### Written in which batch, and how

In the batch that decides the act, from the offer as block 2b has it, in a
new **block 2d** after block 2b's name-task check (the check that decides
"rows are written: decided yes in this batch, with this parts digest"):

1. Block 2b, on the arrival task, has each value's plaintext (opened from
   its lease), its sealed bytes, its re-wrapped lock record and its value
   digest. Before it drops the plaintext, `(shared-reads/kv-prefixes hints
   *layer *facts :> *kvp)`, pure, computes for each value-indexed fact the
   text-bearing part of its `:ix-kv` address, `k ␀ len ␀ vtext ␀`, which
   travels on with the sealed bytes. After the name task's check gives the
   act's stamp and batch (the record written in block 2a),
   `(shared-reads/index-writes hints *layer *nm *rows *kvp *stamp *batch :>
   *d)`, pure and total, computes the act's entries (the rows as written,
   with `:e`, `:fid`, `:stamp`, `:batch`), their addresses (each `:ix-kv`
   address completed with `hex(stamp) ␀ fid`), the `:ix-of` sets and the
   `:ix-id` addresses. An `:ix-kv` address holds the value's canonical
   text, which is why its prefix is taken on the arrival task: that is the
   one place the text exists. **A change to phase 3's discipline, named
   for the merge:** block 2b's "never the plaintext" onward holds for the
   rows; the `:ix-kv` addresses of values whose key is hinted `:by-value`
   (and not `:opaque`) carry the canonical text to the layer's task inside
   the batch, in the module, never in a depot, exactly as the stream gate's
   `:ix-kv` address carries it into `$$layers` (the read exit's design: a
   value index is over plaintext by definition, ruling 6's "no index on"
   opaque values being the one exemption).
2. `(|hash *layer)`, then the read exit's three write blocks over `$$micro
   [L]` (puts, `:ix-of` sets, deletes), plus one for `:ix-id`: all
   `termval`s at computed addresses, no read. One hop per admitted act.
3. A forget fact admitted in the same act (a nil-tagged operator forget in
   a shared layer, micro plan "Writes") adds, on the same task, the purge
   below, after its reads.

Exactly once: microbatch writes are applied exactly once per batch, and a
retried attempt computes the same entries from the same offers and the same
fold (microbatch.md "Guarantees" [docs]; every value is computed before any
write). No increment, no append. A gate error inside `index-writes` (it
catches `Throwable`) gives empty lists and a flag the batch turns into
nothing written for that act's indexes and a counted `:index-error` on the
task (RR6): the act is already decided in block 2a, so the micro gate cannot
refuse it here as the stream gate does (`:gate-error`); the missing entries
are then what the next rebuild's put pass restores, and a test asserts that
no generated offer sets the flag. Never a throw (rule 9).

### Purge by value id, shared

`(shared-reads/purge-writes entry kv-addresses erased-at)`, pure and total,
the micro twin of `reads/purge-writes`: tombstones (every value field nil:
`:v`, `:lock`, `:digest`, any sealed field; `:erased-at` the date) at the
entry's `:ix-ek`, `:ix-ke` and `:ix-s` addresses, all computed from the
entry (`:e :k :stamp :batch :fid`), no value needed; deletes of every
`:ix-kv` address in the fact's `:ix-of` set and of the `:ix-of` entry;
`:ix-id` kept (ids only). Its reads, all on the layer's task: `:ix-id
[fid]` → `:ix-ek [address]` → the entry, and `:ix-of [fid]`: three seeks.

**The invariant every purge keeps, both stores: a purge writes exactly what
a rebuild would write for that fact at that moment.** The date in a
tombstone is the date the open function gives for the fact at the purge's
moment (the ledger's date, else the person-forget date, L16's order), never
"the stamp of whatever triggered the purge". So a value forget after a
person forget writes the value forget's date (the ledger is read first), a
person forget after a value forget leaves the value forget's date, and a
rebuild after either produces the same tombstone. This is what makes
"restore, rebuild, forget replay reproduces the indexes exactly" hold by
construction (test RT8).

### Rebuild from the log, shared

The micro store's log is by entity (`$$micro [e :log name idx]`, with the
act's record under each entity it touched, `$$micro [e :answers name]`).
No per-layer list of acts exists, and the indexes being rebuilt cannot be
trusted to list them, so a rebuild of the micro indexes is a pass over the
whole log, every task's entities, in pages; it rebuilds every shared layer
at once (a restore rebuilds everything anyway; a single layer's rebuild,
after a hint change, is the same pass filtered by layer).

A new depot, `*micro-index-ops`, `(declare-depot setup *micro-index-ops
:random)`, appended by the operator, a second data source of the micro
topology beside `*micro-offers` (plus `*micro-tick`; phase 3 already
declares two sources [docs: a microbatch topology may consume several
depots]) [build checks: a third `source>` in the micro topology from a
function of `rig.store.shared-reads`; fallback inline in `micro.clj`]. A
record names its task and the batch routes to it with `(|direct *task)`:

- **Put page** `{:op :rebuild-put :task t :after e-or-nil :entities n}`
  (1 ≤ n ≤ 64): on task t, the next n entity keys of `$$micro` after `e`
  (`(sorted-map-range-from e {:max-amt n :inclusive? false})` over the
  top-level map, [build checks: a range over a PState's top-level keyword
  keys; the read exit probed page walks over vector keys in a subindexed
  map; fallback, a per-task `$$micro-task :entities` subindexed set written
  by block 2b, one no-read put per new entity]), each entity's `:answers`
  and `:log` read whole (`subselect ALL`, `{:allow-yield? true}`), each row
  of a yes act opened by the same open step (`locks/unwrap` and `open` with
  the row's lock, or its lock row under an `:own-row` mark, and the entity
  task's `$$persons`; the ledger on this task gives the erasure date; the
  `:ix-kv` prefix is taken here, on the entity task, as block 2b takes it
  on the arrival task), then `(shared-reads/put-page-writes ...)`, pure:
  the entries, tombstones and sets they imply, grouped by layer; `(|hash
  L)` per layer group; the write blocks. The batch's work is bounded by n
  entities and a row cap of 4,096 (the rest of an entity's rows, if more,
  wait for the next page, which starts at that entity again with a row
  offset: RR7). One record per batch per task at most (the operator's loop
  appends the next page after it sees the previous page's progress row,
  below).
- **Sweep page** `{:op :rebuild-sweep :task t :field f :after a :entries n}`
  (1 ≤ n ≤ 512): on task t, for each layer whose home is t (`$$micro`'s
  layer-as-entity keys; which keys are layers is known from their
  `:settings`), the next n entries of field f after address a; for each
  entry, `(|hash (:e entry))`, its act's record and row on the entity task,
  back with `(|hash L)`; `(shared-reads/sweep-page-writes ...)`, pure: the
  delete of every entry the log does not imply (the read exit's rule), and
  for `:ix-kv` its removal from `:ix-of`, for `:ix-id` a mismatch rewritten.
  Two hops per entry, batched by the microbatch.
- **Progress row.** A microbatch cannot `ack-return>`; the operator's loop
  learns where a page stopped from `$$micro-task :rebuild` on task t, a
  small fixed-keys field `{:op :cursor :done? :batch}` written by each page
  (a `termval`, exactly once with the batch), polled by the loop (testing.md
  "Synchronizing Any Design" [docs]). **A schema addition to phase 3's
  `$$micro-task`**, named for the merge.

Consistent without a snapshot, for the same reasons as the read exit's F2:
puts only add, a sweep deletes only what the log read in its own batch does
not imply, and an act admitted or a forget decided between two pages writes
or purges its own entries in its own batch. `$$micro-task :rebuild` and the
depot are maintenance: no act, nothing in the record (RR8), as the read
exit's `*index-ops`.

## Standing reads (CONCLUSION R6, a default)

R6 as written: one entry per standing read, opened with its pattern, role
and frontier; each delivery that shows something new adds a stamped line
with the delivered fact ids, exact for a person or a model, a fingerprint
for a deterministic tool; deliveries recorded at the rate the person is
shown things, for an agent its model-call rate; a delivery of nothing new
adds no line; the entry closes at unsubscribe or session close, with a
fingerprint over everything delivered and a complete-or-partial mark; an
entry left open by a crash is closed when the session closes.

### Mechanism: a poll at the delivery rate, through the one exit

Rama holds a read open in one way of its own, the reactive proxy
(`foreign-proxy` on a PState path, pushing fine-grained diffs to a
`ProxyState` in the client, heartbeating, resyncing on a CRC mismatch)
[docs: reference/rama/docs/15-pstates.md, "Reactive queries", "Fault
handling"]. The other way is the client asking again. This plan takes the
poll, for four reasons, in order of weight:

1. **A proxy on the data cannot show a value.** A proxy delivers the
   PState's raw contents to the client: an index entry's sealed bytes and
   wrapped lock, never an opened value. Values open only inside the module
   (`open-row>`, `open-entry>`, with `$$persons` read on the task), because
   nothing that opens a value may leave it (default 1, R1). A proxy on the
   data would therefore either show nothing useful or need the person locks
   outside the module. And it would put matched facts in the client before
   any line exists, so "nothing shown before its line is acknowledged"
   would rest on the client's discipline rather than on the exit's order.
2. **A proxy can only be a doorbell, and a doorbell costs a new write and a
   subscription.** Proxies cannot target a subindexed structure or a
   top-level map [docs: 15-pstates.md, "you cannot proxy objects which index
   their elements separately"], and every index here is subindexed; a
   doorbell would be a new per-layer field written by every admitted act
   (`$$layers [L :last-stamp]`, `$$micro [L :last-batch]`), plus a
   server-side subscription per standing read (memory and a diff per
   change, heartbeats), ringing at the rate the layer changes.
3. **R6 fixes the rate at the delivery rate, not the change rate.** The
   query rate the rule needs is exactly one query per delivery: the person's
   refresh, the agent's model call. A poll at that rate is the rule itself;
   a doorbell would still have to wait for the next delivery to query.
4. **A poll is crash-simple.** No state lives in the store for a standing
   read but its entry; a door that dies leaves an open entry and nothing
   else, which the session close closes (below). A proxy leaves a
   subscription for the server to time out.

The cost the poll pays: one query per delivery tick even when nothing
changed, about three seeks on the layer's task (settings, clock or
frontier, one page or tail read) and no write.

The alternative constructed, with numbers: **a doorbell.** One proxy per
client process and layer on a per-layer long that every admitted act
writes (`$$layers [L :last-stamp]` in the gate's decision event,
`$$micro [L :last-batch]` in block 2d; one `termval` each, no read), which
`deliver!` consults before querying: unchanged since the handle's last
moment gives `:nothing-new` with no query; changed, the delta query runs
as below (on a shared layer, possibly one or two ticks early, until the
frontier reaches the rung batch). Costs, for a screen holding 20 standing
reads over 5 layers at 2 deliveries a second, each layer changing once in
10 s: the poll, 20 × 2 × 3 = 120 seeks a second; the doorbell, 0.5 changes
a second × 4 reads × 3 = 6 seeks a second, plus 0.5 puts and 0.5 diffs a
second, plus 5 subscriptions held on the server. For an agent session
layer admitting 100 acts a second with one standing read at a model call a
second: the poll, 3 seeks a second; the doorbell, the same 3 seeks plus 100
puts and 100 diffs a second. So the doorbell does less aggregate work for
quiet layers with many standing reads, and more for busy layers with few;
Softland has both (renderers on people's layers; agents writing
continuously). Taken tonight: the poll, by the brief's rule 10 (the
simplest pick that can change without touching a record); the doorbell is
the named change (RR9), which touches no record (the lines are the same
either way) and adds one put per admitted act to phase 7's first number.
Its trigger: src-inland's renderers or phase 7 showing empty polls to
matter. [docs: a proxy may target a value inside a top-level map's value,
15-pstates.md; not run.]

### The delta: "something new" since the last delivery, in both stores

`read-pattern` gains one input, `:after`, a moment of the read's kind
(**a change to the read exit's build**): the delta is every match admitted
after `:after` and at or before the read's moment. "New" is exact in both
stores:

- **One-owner.** `:after {:stamp s0}`, where s0 is the moment of the last
  acknowledged delivery. The moment of a read is `min(asked, clock)`
  (FR2), and every fact admitted on the home task after that read gets a
  stamp above the clock it read, so "stamp > s0 and ≤ m" names exactly the
  facts admitted between the two reads: nothing missed, nothing twice.
- **Shared.** `:after {:frontier F0}`: "batch > F0 and ≤ F" names exactly
  the facts that became visible between the two frontiers.

Which index serves a delta, `(reads/delta-plan pp after)`, pure:

- `[:ek e k]` and `[:kv k v]` (one-owner): their own index, from the
  prefix plus `hex(s0 + 1)` to `hex(m + 1)`: the address orders them by
  stamp inside the prefix, so the delta reads only new matches.
- `[:latest e k]`: the tail read (one-owner; the tail loop at F, shared);
  new when the head's stamp is above s0 (its batch above F0): then the one
  new head is delivered, else nothing.
- `[:all]`, `[:e e]`, `[:k k]` (one-owner), and every form on a shared
  layer: **a new index, `:ix-s`**, every fact of the layer in stamp order
  (`hex(stamp) ␀ fid` → the entry, one-owner) or in batch order (`hex(batch)
  ␀ hex(stamp) ␀ fid`, shared, above), read from `hex(s0 + 1)` (or
  `hex(F0 + 1)`) and filtered by the pattern's pure predicate
  `(reads/matches? pp entry)`. Why a new index: in `:ix-ek` and `:ix-ke`,
  `[:e]`, `[:k]` and `[:all]` are not stamp-ordered inside their prefix, so
  their delta would rescan the whole prefix on every tick (a standing `[:all]`
  over a 10,000-fact layer would hit F8's scan budget every tick and be
  partial for ever); over `:ix-s` a delta costs one seek plus one iteration
  per fact admitted since the last delivery. For a shared layer `:ix-ek` and
  `:ix-kv` are stamp-ordered, not batch-ordered, and the first stamp of a
  batch is not known, so `:ix-s` serves every shared form.

The one-owner `:ix-s` is a fifth field of the read exit's `layer-fields`,
written in the gate's decision event with the other four (one more
`termval` per fact), tombstoned by `purge-writes`, rebuilt by the put pages
and swept by the sweep pages: every read exit function that lists the index
kinds gains one kind (**a change to the read exit's build**, named with the
others below). It serves phase 2's `read-as-of` range too (every fact at or
before T, with its row), so phase 2's `:by-stamp` (stamp → name, then a row
read per act) is no longer needed by any read of this plan; whether it stays
is builder A's call, and nothing here relies on its absence or presence.

The limit and the scan budget hold for a delta as for any pattern read; a
delta cut by the limit or the budget is a `:partial` delivery (below). The
facts beyond the cut are not lost (RR10): every index a delta reads orders
its range by stamp (one-owner: `:ix-s`, and the stamp part of an `[:ek]` or
`[:kv]` address) or by batch then stamp (shared `:ix-s`), so the handle
keeps the address of the last *shown* row, and the next delta is one range
from just above that address (the address followed by U+0000, the read
exit's page rule) to the new moment's bound. The address is computed from
the shown row itself, so nothing of the unshown limit + 1st entry reaches
the client (F3 kept). A delta that was not cut resumes from `:after` the
last acknowledged moment, as above. [Derived; test RT9 drives a delta of
limit + 5 facts over two deliveries and checks each fact is delivered
exactly once.]

### The entry's lines (first-record, FRR1 to FRR4)

One entity per standing read, `:read-<uuid>` from the opening act's name
(FR5's rule). Every line is a fact about it, in the reader's working layer,
through the ordinary offer path, marked `:own-row` (see "The close act"):

- **FRR1, the opening** (one act, one or two facts):
  `{:e ent :k :read/standing :v {:layer L :pattern p :role r :moment m0}}`,
  m0 the initial read's moment (`{:stamp s}` or `{:frontier F}`); and, when
  the initial read matched anything, a first delivery line in the same act.
  An empty initial read opens the entry with no delivery line.
- **FRR2, a delivery** (one act per delivery that shows something new):
  `{:e ent :k :read/delivery :v {:layer L :moment m :after m-prev :role r
  :count n :mark :complete|:partial :fingerprint hex :fp-secret :read-fp/1
  :exact [[fid stamp] ...] :max-stamp s}}`. `:exact` for a person or a model
  always, for a tool only with `:rows? true` (ruling 3's split, R6's "exact
  for a person or a model, a fingerprint for a deterministic tool");
  `:fingerprint` on every line, over the set of that delivery's
  `[fid stamp]` pairs (FR11's function), because the closing fingerprint is
  taken over the lines' fingerprints; `:max-stamp` on a shared layer's line
  only (F1, below). The line is stamped by its act: R6's "stamped line".
- **FRR3, the closing**: `{:e ent :k :read/closed :v {:layer L :moment
  m-last :deliveries d :fingerprint hex :fp-secret :read-fp/1 :mark
  :complete|:partial :closed-by :unsubscribe|:session-close|:crash}}`.
  `:fingerprint` is HMAC-SHA256 under the fingerprint secret over
  `"softland.standing-fp/1\n"` + the canonical text of the vector of
  `[line-stamp line-fingerprint]` of the entry's delivery lines in stamp
  order: "a fingerprint over everything delivered", computable from the
  record alone, for every reader kind (a tool's lines hold no ids, so a
  fingerprint over the delivered pairs could not be recomputed after a
  crash; one over the lines can, and a re-run of a deterministic tool's
  standing read over the same history gives the same lines and the same
  closing fingerprint). `:mark` is `:partial` when any delivery line is
  `:partial`, else `:complete`: the mark says whether the record names
  everything the read matched; how the entry closed is `:closed-by`'s.
- **FRR4, the keys** `:read/standing`, `:read/delivery`, `:read/closed`,
  store-owned constants beside `:read/point` and `:read/pattern` (FR6);
  hinted `:no-copy` (F7), so their id-index entries carry no value copy.

The read exit's `entry-moments` (F1) counts the moment of every
`:read/standing`, `:read/delivery` and `:read/closed` fact, as it counts
`:read/pattern`'s, so every line is stamped after what it names.

### The functions (`rig.store.standing`, client side beside the exit)

A standing read's handle lives in the caller's process: `{:ent :layer
:pattern :reader-fields :moment :lines-stamps}`; it is not durable, and
nothing depends on it surviving (the record has everything a close needs).

- **`(subscribe! store spec)`** → `{:handle h :rows [...]}` or `{:refused
  r}`. The exit's steps with one act of FRR1: query (`read-pattern`, no
  `:after`), build the opening act (and the first delivery line when the
  read matched), offer until answered, and only on `:yes` return the rows.
  A refused opening shows nothing and opens nothing.
- **`(deliver! store h)`** → `{:rows [...]}`, `:nothing-new`, or `{:refused
  r}`. Called at the delivery rate by its caller (the renderer's refresh, the
  agent's model call); the store does not pace it, and a line exists
  exactly when a delivery is shown, which is R6's rate by construction.
  The handle keeps two moments: `:scan`, where the next delta starts, and
  `:line`, the moment of the last recorded line (the next line's recorded
  `:after`, so the lines chain). Steps: the delta query with `:after` the
  handle's `:scan`; no match gives `:nothing-new`, nothing offered, nothing
  shown, and `:scan` advances to the delta's moment (safe: nothing matched
  between, by the argument of "The delta", and a busy layer is then not
  rescanned from the last line on every quiet tick); a match builds one
  FRR2 act with `:after` the handle's `:line`, offers it until answered,
  and on `:yes` sets both moments to the delta's and returns the rows; on
  `:no` returns `{:refused r}`, shows nothing, and leaves both moments, so
  the same facts are new again at the next delivery.
- **`(unsubscribe! store h)`** → the closing act, FRR3 with `:closed-by
  :unsubscribe`, its fingerprint and mark from the query below.
- **The closing query `standing-close [*layer *ent :> *c]`** (a third
  query topology of this stage, on the working layer's home, `(|hash
  *layer)`): the entry's delivery lines by `:ix-ek` prefix `ent␀read/delivery␀`
  (one page), each line's row (`:no-copy`, one to two seeks each) opened by
  `open-row>` (the lines are sealed values of the working layer, opened on
  its home like any value), then the closing fingerprint under the
  fingerprint secret, which exists only in the module (FR12), and the mark;
  returns `{:fingerprint :deliveries :mark :moment}`, never a line's
  contents. Why a query and not the client: the secret never leaves the
  module, and a crash close has only the record to work from. It is not
  itself recorded as a read (RR11): it reads the entry's own lines, whose
  contents were each recorded when delivered, and shows no fact of the read
  layer; its output is written into the closing line, which is its record.
  For Sid, with the other maintenance reads (below).

A standing read of a re-classed layer: its deltas read the micro era (batch
above F0); a stream-side fact of the layer admitted after its re-class
(P16's settings) is not delivered by a standing read (RR12), which is the
two-store moment carried (see "What stays open").

What a standing read does not deliver (for Sid, touches a line): a forget of
a fact it already delivered. The fact is not new; the next full read shows
it erased; the renderer holding the shown value is outside the store. A
delivery of erasures would need a line naming erased ids, and a delta over
erasures by date (phase 2's ledger is keyed by lock id, not by date).

### Crash, and the close at session close

- **Orderly session close** (the session's door is alive): the door closes
  every live handle with `:closed-by :session-close`, then does the rest of
  "The close act" below.
- **After a crash** (the door died with handles open): the entries stay open
  in the working layer. `(standing/close-session! store layer who)`, run by
  the session's next door or by the operator, finds them with the query
  **`standing-open [*layer :> *ents]`** (on the working layer's home: the
  `:ix-ke` range `read/standing␀` and, only when it is not empty, the range
  `read/closed␀`, each in doubling pages; the open entries are the
  entities of the first not in the second, a set difference in memory,
  since `:ix-ke`'s address names the entity; ids only, a maintenance read
  like the closing query), and closes each with `:closed-by :crash`,
  its fingerprint and mark from `standing-close`. The closer writes under
  its own permission in the working layer: the session's, or the operator's
  (R7's root permission, default 5; For Sid 2: the operator as the root
  actor).
- Nothing is lost that was shown: no delivery is shown before its line is
  acknowledged, so a crash can leave a line for a delivery not shown (the
  over-recording the read exit already names), never a showing without a
  line.

### Standing reads on shared layers

The same functions: the handle's moment is `{:frontier F}`, the delta runs
over the micro `:ix-s` from `hex(F0 + 1)`, the lines carry `:moment
{:frontier F}` and `:max-stamp`, and every line still lives in the reader's
working layer, a one-owner layer on the stream gate. A shared layer's
frontier moves about every tick (250 ms) whether or not the layer changed,
so a delivery tick on a shared layer costs a delta query as on a one-owner
layer; there is no extra cost for being shared.

## The close act (CONCLUSION R5, default 4)

### What the act says

Phase 2's session close is an act into the session's layer, one control
fact `{:e s :k :session-closed :v {:session s}}`, decided by the stream
gate on the layer's home, which deletes the session's unconsumed lease rows
(PLAN-locks-and-forgetting.md, "Writes", session close). This stage adds
one optional part to its value: **`:reads :keep | :drop`** (FRR5,
first-record); absent means `:keep`, tonight's default when the act says
nothing (the brief). Phase 2's value parser for `:session-closed` accepts
the part and refuses anything else as data (`:malformed-value`, phase 2's
recorded reason). The decision's writes are phase 2's, unchanged; the part
is read by the closer's procedure, not by the gate (RR13: the gate needs no
branch).

### Kept

Nothing happens to the entries. They stay values of the agent layer, their
locks in the layer's lock store, openable through the exit by whoever may
see the layer (ruling 9's default: its owner).

### Dropped: phase 2's value forget, one ordinary forget act per page

The closer (the session's door, or the operator after a crash) runs
`(read-exit/drop-reads! store layer close-name)` after the close act's
`:yes`:

1. **Enumerate** with the query **`entry-ids [*layer *before *after *n :>
   *page]`** (on the layer's home, `(|hash *layer)`): the ids of the
   `:read/*` facts of the layer admitted at or before `*before` (the close
   act's stamp) and not yet erased (their `:ix-ke` entries not
   tombstoned), from one range of `:ix-ke`, the prefix `read/` (a keyword
   prints without its colon, so the five store keys share it), keeping only
   the five keys by the page step (a hand-written `:read/other` fact is
   skipped, not forgotten), at most n (256) per page; ids and stamps only,
   nothing opened (a maintenance read, RR11).
2. **Forget** each page with one ordinary value-forget act, phase 2's OP9:
   `:who` the layer's owner or `:operator`, `:layer` the agent layer,
   `:stood-on` each target's `[fid stamp]`, `:because-of` the close act's
   name (ruling 3: "trigger is already because-of"), one fact `{:e e :k
   :forget :v {:target fid}}` per target (acts of any size). Each is its own
   fact, named by the closer before it is offered, answered by name,
   retried from the record: every forget is a fact (the rig constraint),
   and nothing in the gate is new.
3. **Repeat** until a page is empty. A crash of the closer is repaired by
   running it again: the query returns only what is not yet erased, and a
   forget of a value already erased is admitted and changes nothing (phase
   2's OP9 row).

Each forget act's event is bounded: 256 targets × (the target row, the
answer's stamp, the ledger, the lock row delete, `:ix-of`, the purge's
five tombstones and deletes) is about 1,300 seeks and 2,500 writes, about
0.7 s, under the stream timeout (F2's arithmetic; RR14, the page size).

### The mark at write: every read entry is `:own-row`

**Yes, the read exit must mark its entries at write.** An agent layer
keeps a value's lock in the record by default (ruling 7: "in the record
for agent sessions"); the forget of such a value is an excision, "the
operator's fallback for unmarked record-default values" (ruling 7), which
phase 2 builds as `termval nil` on the row's `:lock` with `:how :excised`.
So without a mark a drop would still erase the entries mechanically, but
only by running the operator's fallback at every agent session close:
routine use of a fallback, rewriting log rows' bytes where the ordinary
forget deletes a lock store row. Ruling 7 gives the value's writer the
means for exactly this, "a mark on any value overrides": a value marked
`:own-row` gets its own row in the lock store in any layer (phase 2's D9),
and its forget deletes the row (`:how :row-deleted`). The choice between
keep and drop is made at close, after the entries were written, so every
entry must be written forgettable the ordinary way: marked at write.

What it changes in the read exit's build:

- `reads/entry-facts` puts `:mark #{:own-row}` on every `:read/*` fact it
  builds, in every working layer (FRR6, first-record: where each entry's
  lock lives, a pick that sticks per value). In personal and hand layers the
  mark agrees with the default and changes nothing; marking everywhere
  means the exit needs no knowledge of the working layer's kind.
- Through phase 2's gate, unchanged: each entry value's lock becomes a row
  in the working layer's `:locks`. The bytes move from the row's `:lock` to
  a lock row; they do not grow. Phase 7 is told: agent layers now write lock
  rows, one per read-entry value (phase 2's M1 says "agent layers write no
  lock rows"), and M2's count of `:locks` in hand layers includes entries.
- Nothing else: the entry path, its permission, its stamp rule are the
  read exit's.

### The tests' reading of "cannot be opened afterwards"

After `drop-reads!` ends: every `:read/*` fact of the layer admitted before
the close act has no lock row, has a ledger entry (`:row-deleted`, dated by
its forget act), opens through `open-value>` as `{:erased-at s}`, shows
through the exit as an erased row with its date and no value, and matches
no `[:kv]` read; its index entries are tombstones. Between the close act
and the end of the loop the remaining entries still open (RR15; For Sid:
whether a drop must be effective at the close act's commit, which would
need the gate's open to honour a layer-level "reads dropped before s" mark,
a change to phase 2's open functions).

## Indexes and forgets, whole

### Every index, what it holds, and how a forget reaches it

| index | where, written by | holds | on a value forget | on a person forget | rebuilt by |
|---|---|---|---|---|---|
| `:ix-ek`, `:ix-ke`, `:ix-s` (one-owner) | `$$layers [L]`, the stream gate, in the admitting event | the row copied (sealed bytes, the lock record where the layer keeps it in the record, the digest) | tombstone: value fields nil, `:erased-at` | tombstone with the date open gives (below) | the read exit's put and sweep pages |
| `:ix-kv`, `:ix-of` (one-owner) | same | the value's canonical text in the address | delete | delete | same |
| `:ix-ek`, `:ix-ke`, `:ix-s` (shared) | `$$micro [L]`, the micro topology, block 2d of the deciding batch | the micro row copied, with `:e` and `:batch` | tombstone, in the forget's batch | tombstone | the micro pass (above) |
| `:ix-kv`, `:ix-of` (shared) | same | the value's text in the address | delete, in the forget's batch | delete | same |
| `:ix-id` (shared) | same | fid → its `:ix-ek` address: ids only | kept | kept | same |
| `:by-stamp` (phase 2) | `$$layers [L]`, the stream gate | stamp → name: ids only | kept | kept | phase 2's (from `:answers`) |
| `:heads` (phase 1, phase 3) | both stores | ids, stamps, batches | kept | kept | not an index over values |
| `:erased` (phase 2, both stores) | the ledger | lock id → date and how | written by the forget | not written (phase 2's `wrap-closed` gives the date) | from the log's `:forget` facts (phase 2's) |

"Holds" is what a forget must not leave openable or confirmable (P6
widened: "nothing retained opens or confirms a forgotten value"). A
tombstone keeps the id, entity, key, stamp and batch, so a read as of any
moment still shows the fact with its erasure date (the sharpening "Forget,
time travel"); a copy of sealed bytes whose lock is gone opens nothing, but
it is nilled anyway, so the purge and the rebuild write the same thing
(the invariant, "Purge by value id, shared").

### The five paths

1. **A value forget in a one-owner layer.** Phase 2's OP9 event on the
   layer's home, unchanged: its call to `reads/purge-writes` (the read
   exit's) now also tombstones `:ix-s`. The date is the forget's stamp,
   which is the ledger's date the event writes (a second forget of the same
   value: "admitted, nothing changes", no purge, the first date stays).
2. **A value forget in a shared layer.** Phase 3's nil-tagged operator act
   `{:e e :k :forget :v {:target fid}}` in the value's layer, arriving on
   hash(e) (the micro depot routes an act with no sealed value by its first
   entity). Block 1 reads the target's ledger entry on hash(e) (phase 2's
   and 3's gather; local). Its lock effect on hash(e) is phase 2's and 3's
   (the row's `:lock` excised, or a row lock deleted under an `:own-row`
   mark; the ledger written). This stage adds, in block 2d, on hash(L), the
   shared purge of the target (three seeks, then writes), carrying from
   hash(e) the date open will give after the forget: the ledger's date (this
   forget's stamp if it is the first, else the date already in the ledger).
   Under per-act grain the forget erases the act, and phase 3's row seam
   names every fact id whose lock it erased; the purge runs for each
   (**the one contract taken from phase 3's forget: the list of fact ids
   erased, with the date**).
3. **A person forget.** Phase 2's operator act on `:people`'s home, which
   destroys the person lock and fans out to write `$$persons [p]` on every
   task. A person's values can be many, and one event must stay well under
   the stream timeout (F2), so the purge is paged, driven by
   `(read-exit/purge-person! store p)`, which phase 2's `forget-person!`
   calls right after the forget's `:yes` (and which the restore replays):
   - **One-owner, per task t:** pages `{:op :person-purge :task t :person
     p :after cursor :n 256}` on the read exit's `*index-ops`, routed to t
     by `(|direct *task)` after the source (the depot's `hash-by :layer`
     sees a nil layer; RR16). On t, **phase 2's enumeration seam** gives the
     next page of `[layer fid]` on this task whose value died with p; per
     fact, the row and its act's stamp, `open-row>` for the date (the
     ledger's, else p's: L16's order), `:ix-of [fid]`, and
     `reads/purge-writes` with that date; `ack-return> {:next :done?}`.
     The contract taken from phase 2 (a black box: its body is phase 2's):
     `(locks/dying-with> *person *after *n :> *page)` on a task, `*page` =
     `{:fids [[layer fid] ...] :next cursor :done? bool}`, every value on
     this task in a one-owner layer whose wrap closed with p's lock,
     total, never throwing, bounded by n. **Fallback if the seam is not
     there at the merge:** the page sweeps the `:ix-kv` entries of the
     layers on task t (the read exit's sweep over one field, `open-row>`
     on each; an entry that no longer opens is purged): the same result, at
     the cost of every value-indexed entry on the task instead of only the
     dying ones.
   - **Shared, per task t:** pages `{:op :person-purge :task t :person p
     :after [L address] :n 256}` on `*micro-index-ops`; on t, for each
     shared layer whose home is t, in order, its `:ix-kv` entries after the
     cursor whose `:lock` names p (in `:required` or `:any-of`, read from
     the entry itself) and that are not tombstones are opened by
     `open-entry>`; each whose wrap is now closed is purged with p's date.
     An entry already a tombstone was purged by a value forget in that
     forget's batch, so its date is the ledger's and stays (L16's order,
     kept without reading the ledger on hash(e)). The shared layers of a
     task are listed in a small subindexed set `$$micro-task :layers`,
     written by a layer's making act in block 2a on hash(L) (**a schema
     addition to phase 3**, beside `:rebuild`). No enumeration seam is
     assumed on the micro side: phase 2's is for its layers, and the micro
     indexes are this stage's. The cost is every value-indexed entry of the
     task's shared layers per person forget (a rare operator act); the
     named upgrade, which changes no record, is a subject index `p ␀ L ␀
     fid` placed by hash(p), written at admission for value-indexed facts,
     so a person purge reads only the person's candidates (RR17).
   - **Between the forget and the purge,** reads never match or show a
     value that no longer opens (the read exit's F5 for `[:kv]`, and every
     shown row goes through the open step), so the window is one of
     storage, not of showing: the value's text sits in `:ix-kv` addresses
     until the pages pass. The rig constraint is "so a forget reaches it";
     it reaches it when `purge-person!` returns, and a crash of the
     operator's loop is repaired by running it again (every page is
     idempotent, and a purged entry is not a candidate again). A
     store-driven road, the fan-out child appending the first page itself
     (`depot-partition-append!` into the two ops depots from phase 2's
     fan-out event, at-least-once, harmless twice), is the named
     alternative (RR18): it moves one line into phase 2's event, which
     tonight's parallel builds would have to merge.
4. **A drop at session close** (above): phase 2's OP9 acts, so path 1.
5. **A restore.** `(read-exit/restore! store)`, the operator's:
   1. **The restore is a fact:** an operator act into the store layer
      `:people`, `{:e :store :k :restore :v {:indexes :rebuilt}}`, answered
      by name (FRR7, first-record: where a restore is recorded and its
      form; `:people` is the one layer of kind `:store`, phase 2's seed).
   2. **Rebuild from the log, in pages:** every one-owner layer by the read
      exit's `rebuild!` (put pages, then sweep pages over the five fields,
      F2's bounds), and the micro store by its pass (put pages over every
      task's entities, then sweep pages over every shared layer's six
      fields). Layers are rebuilt before they are opened to reads (the read
      exit's "for a kept store").
   3. **Then replay the forget facts after it,** the rig constraint: the
      admitted forget facts of both stores, found through the rebuilt
      `:ix-ke` ranges `forget␀` of every layer (they are control facts,
      plaintext, indexed like any fact), and the `:forget-person` facts of
      `:people`, all in stamp order (stamps compare across the stores,
      default 2). A value forget replays as `{:op :replay-forget :layer L
      :fid target :stamp s}` on the layer's ops depot: phase 2's (or 3's)
      forget effect for that target at the fact's own stamp, idempotent (a
      lock already in the ledger: nothing changes), then the purge at the
      date open gives. A person forget replays as `purge-person!`, after
      phase 2's own replay of the person lock's destruction. **The contract
      taken from phase 2:** its forget effects callable at a replay seam
      with the fact's target and stamp, apart from a fresh offer; in the
      rig, where no lock is ever restored (phase 2's L17), the replay's
      lock step changes nothing and only the purge runs.
   Why forgets come after the rebuild: a rebuild opens values as the log
   and the lock store stand; if a restore brought back a lock a later forget
   had destroyed, the rebuild would index its value again, and only the
   forget replayed after it takes it back out. With the purge invariant,
   the indexes after the replay are exactly those of a store that never
   restored.

What stays for a kept store, named: where the forget facts a restore
replays are kept when the restore also restores the log (the depot `*offers`
keeps every offer for ever, phase 2's O2, but a restored module's depots and
PStates may come from one backup); RocksDB keeps a deleted address's bytes
until compaction, like the depot's copy of a value (the read exit's "for a
kept store").

## What the read exit's build must change

The read exit's build is in flight in `Softland-rig-build-reads`; it goes on
as planned, and these land on top of it, at the merge of wave 1 or after
(none changes a line it builds; each adds to a function or a list):

1. **Shared branch** in `read-point` and `read-pattern` after the settings
   read: by-entity layers go to `rig.store.shared-reads` on the same task
   ("What changes in the read exit's two queries").
2. **A fifth index kind, `:ix-s`**, in `layer-fields`, `index-writes`,
   `purge-writes`, `put-page-writes`, `sweep-page-writes`, `implied`, and
   the rebuild loop's field list ("The delta").
3. **`:after`** on `read-pattern` and `(reads/delta-plan ...)`, and
   `reads/matches?` for filtering `:ix-s` ("The delta").
4. **`reads/entry-facts`**: `:mark #{:own-row}` on every `:read/*` fact
   (FRR6); the shared line's `:moment {:frontier F}` and `:max-stamp`
   (FRR8); the standing lines (FRR1 to FRR3).
5. **`reads/entry-moments`**: a line whose moment is `{:frontier F}` gives
   its `:max-stamp` (when a long in range); the three standing keys count
   like `:read/pattern`.
6. **`reads/seed-hints`**: `:no-copy` gains `:read/standing`,
   `:read/delivery`, `:read/closed`.
7. **`reads/index-op`**: the ops `:person-purge` (routed by `:task`),
   `:replay-forget`, and nothing else; the parse stays total.
8. **The exit's call check**: `:as-of {:frontier F}` accepted (the query
   refuses it on a one-owner layer, `:moment-kind`, and a stamp moment on a
   shared one).

## Interfaces, as this stage builds against them

- **The read exit** (`rig.store.reads`, `rig.store.read-exit`): its
  exports as its plan lists them, with the eight changes above; the
  address, page step, fingerprint, parse and answer functions reused by the
  shared path unchanged.
- **Phase 2** (`rig.store.locks`): `open-value>`, `open-row>` (one-owner);
  the pure `unwrap`, `open`, `wrap-closed` (the shared `open-entry>`);
  OP9 as built (the drop); the session close act with its value parser
  taking `:reads`; the purge call site in the value forget's event (the
  read exit's), which now tombstones `:ix-s` too; the enumeration seam
  `dying-with>` (contract above, fallback named); its forget effects at a
  replay seam (contract above). Where phase 2's names differ at the merge,
  phase 2's win and this plan's calls follow them.
- **Phase 3** (`rig.store.micro`): the frontier at `$$micro-task
  :frontier` and the reader's rule; `$$micro`'s schema takes `(merge
  micro-fields (shared-reads/layer-fields micro-row-fields))`; block 2d
  after block 2b's name-task check; the forget's row seam gives the fact
  ids erased and the date; `$$micro-task` gains `:rebuild` and `:layers`;
  the micro topology gains the `*micro-index-ops` source. `micro-lookup`
  and `micro-act` stay the micro store's own reads for the offerer and
  tests; a person's read of a shared layer goes through the exit.
- **Phase 4**: nothing. A landing is an admitted act in a shared layer and
  is indexed like any other; its status reads are phase 4's.
- **Phase 6**: `seed-hints` becomes the keys' grammar facts, for both
  stores' indexes at once, and a tool's `:rows?` and kind its signature.

## The lines in module.clj and micro.clj, and where Rama does not allow one

Where one form serves, in module.clj: `(shared-reads/declare-queries!
topologies)` for `standing-close`, `standing-open` and `entry-ids`
[build checks, the read exit's same check]; `(shared-reads/declare-depots!
setup)` for `*micro-index-ops` [build checks]. In `micro.clj`, the schema
merge (one form). Where Rama does not allow one form: block 2d and the
`*micro-index-ops` source are dataflow inside the micro topology (only the
topology that declares `$$micro` may write it), so `micro.clj` carries the
block (a partitioner and four write blocks, about ten lines, calling
`shared-reads` functions) and the source; the read exit's query topologies
carry the shared branch (about four lines each, calling `shared-reads`).

## Reads

Every reader-facing read goes through the exit and its query topologies on
one task, the read layer's; the maintenance reads are queries on the
working layer's home. Each needs more than one PState read, so each is a
query topology (phase-1-plan Step 1).

| read | who | method | reads, on the layer's task |
|---|---|---|---|
| RS1 shared pattern read `[L for p as-of limit]` | the exit | `read-pattern`'s shared branch | stream settings (1), frontier (1), micro settings at F (1, group), membership (1, group), the page loop over one `$$micro [L]` index (1 seek per page, one iteration per entry, batch filter); re-classed: plus the read exit's loop over `$$layers [L]`; `$$persons` per wrap person (local) per opened entry |
| RS2 shared point read `[L for fids as-of]` | the exit | `read-point`'s shared branch | settings as RS1; per fid `:ix-id` (1) and `:ix-ek` (1) when indexed; stream-era fids as the read exit |
| RS3 a delta `[... :after m0]` | `standing/deliver!` | `read-pattern` with `:after` | as RS1 or the read exit's RE2, over `:ix-s` or the pattern's own stamp-ordered index from the bound |
| RS4 closing fingerprint `[L ent]` | `standing/unsubscribe!`, `close-session!` | query `standing-close` on the working layer's home | the entry's delivery lines (1 page of `:ix-ek`), each line's row (1 to 2) and `open-row>` |
| RS5 open standing entries `[L]` | `close-session!` | query `standing-open` | the `:ix-ke` ranges `read/standing␀` and, when not empty, `read/closed␀`, in pages; a set difference |
| RS6 entry ids `[L before after n]` | `drop-reads!` | query `entry-ids` | one `:ix-ke` range, prefix `read/`, in pages, ids only |
| RS7 a micro rebuild page's progress | the operator's loop | `foreign-select-one [(keypath :rebuild)] $$micro-task {:pkey t}` | 1 |

## Writes

| op | how | decided by |
|---|---|---|
| WS1 a shared read's entry | the exit's ordinary offer into the working layer (the read exit's W1) | the stream gate |
| WS2 a standing read's opening, deliveries, closing | ordinary offers into the working layer, one act each | the stream gate |
| WS3 shared index entries | block 2d of the deciding batch, from the offer and the fold's answer | the micro topology |
| WS4 shared purge by value id | block 2d, in the forget's batch | the micro topology |
| WS5 the close act's `:reads` | a part of phase 2's session close act | the stream gate (phase 2's decision, unchanged) |
| WS6 a drop | phase 2's OP9 acts, 256 targets each, `:because-of` the close act | the stream gate |
| WS7 a person purge page | `*index-ops` (one-owner) and `*micro-index-ops` (shared) records, one bounded page each | the gate topology's index-ops source; the micro topology |
| WS8 a rebuild page | the read exit's `*index-ops` pages; `*micro-index-ops` put and sweep pages | as WS7 |
| WS9 the restore fact | an operator offer into `:people` | the stream gate |
| WS10 a forget replay | `:replay-forget` records on the layer's ops depot | the layer's gate, calling phase 2's replay seam and the purge |
| WS11 the one-owner `:ix-s` | the read exit's `index-writes`, in the admitting event | the stream gate |

## PState Design

No new PState. Fields added to three existing ones, by the merge rule
(same key and partitioner as the data they index, so fields of its value):

- **`$$layers [L]`** (stream gate): one field, `:ix-s` (`hex(stamp) ␀ fid`
  → the read exit's entry), subindexed, size tracking off. Options for the
  delta costed in "The delta": A, prefix rescans (no new field; a delta
  costs the whole prefix, and `[:all]` on a big layer is partial every
  tick); B, phase 2's `:by-stamp` plus a row read per act (no new field;
  one seek per new act per standing read per tick, multiplied by every
  standing read a renderer holds); C, chosen, `:ix-s` carrying the row (one
  put per fact at write; a delta is one seek plus the new facts' iterations
  whatever the number of standing reads). The skill's rule decides: write
  once, read on every tick.
- **`$$micro [L]`** (micro topology): six fields, `:ix-ek`, `:ix-ke`,
  `:ix-kv`, `:ix-of`, `:ix-s`, `:ix-id` (schema in "The fields"); options A,
  B, C in "Placement"; B chosen.
- **`$$micro-task`** (micro topology): `:rebuild` (`(fixed-keys-schema {:op
  Keyword :cursor String :done? Boolean :batch Long})`, a rebuild's
  progress) and `:layers` (`(set-schema Keyword {:subindex? true})`, the
  shared layers whose home is this task; subindexed because a task can hold
  many layers).

## Depots

- `*offers` (phase 1's): every entry, line, close act and forget act, as
  ordinary offers.
- `*index-ops` (the read exit's, `hash-by :layer`): gains the ops
  `:person-purge` (routed to its `:task` by `(|direct)` after the source)
  and `:replay-forget`.
- `*micro-index-ops`, new: `(declare-depot setup *micro-index-ops
  :random)`, operator appends, a third source of the micro topology; every
  record names its task and is routed by `(|direct *task)`. `:random`
  because a record's task is in the record, not in its hash. Not an act and
  not in the record (maintenance, RR8).
- `*micro-offers` and `*micro-tick` (phase 3's): unchanged.

## Topologies and PStates

No new topology. The stream gate topology gains nothing but what the read
exit's changes bring (the fifth index field, two ops in its index-ops
source); the micro topology gains block 2d and the `*micro-index-ops`
source. Both remain the only writers of their PStates, which is why no
third topology can hold these writes. The micro topology stays microbatch:
its index writes must be atomic with the rows they index across tasks, which
only the batch gives, and exactly-once with the batch (block 2d's writes are
`termval`s and `NONE>`s, applied once per batch). The stream gate stays
stream: the one-owner `:ix-s` entry must be visible with the act's answer,
as the read exit's other entries are. Its concerns that do not need stream
latency, the person-purge and forget-replay pages on `*index-ops`, stay on
it for one reason, the one the read exit gave for its rebuild pages: they
write `$$layers`, which only the gate topology may write (PState
ownership), and moving the one-owner indexes to another topology would
split what the admitting event now keeps atomic (the read exit's Option E,
rejected there). Each page is one bounded event (well under the 5 s event
timeout), so it delays the task's other events by at most its own length.
No stream topology in this plan appends to a depot; the named alternative
RR18 would, and would then put `(|direct (ops/current-task-id))` before
each `depot-partition-append!` (stream.md, the commit boundary), with the
receiving pages idempotent against the stream event's replays.

## Query Topologies

The read exit's `read-point` and `read-pattern` gain the shared branch and
`:after`; three new ones, `standing-close`, `standing-open`, `entry-ids`,
each on one task with a leading `(|hash *layer)` and a closing `(|origin)`,
emitting once.

- **`read-pattern`, shared branch.** A group layer's `[:e e]` with 12
  facts → 4 seeks (stream settings, frontier, micro settings, members) + 1
  page (16 iterated, 12 kept). `[:latest e k]` with nothing hidden → 4 + 1;
  with one hidden entry above F → 4 + 2. `[:k :note]` over 1,000 matches →
  4 + 6 pages (16 to 512 entries, 1,008 in all, cut at the 1,001st match),
  marked partial. The base's `[:k k]` after its re-class, 40 stream-era and 60
  micro-era matches → 2 (stream settings, frontier) + 2 pages (stream
  era) + 3 pages (micro era).
  An empty `[:e unknown]` → 4 + 1 (recorded, ruling 3). **Variable**,
  handled by the read exit's doubling page loop and the tail loop.
- **`read-point`, shared branch.** 5 fids of which 2 never indexed → 4 + 5
  `:ix-id` + 3 `:ix-ek`. **Variable**, the read exit's `loop<-`.
- **`read-pattern` with `:after`.** A quiet tick → 3 seeks (one-owner:
  settings, clock, one range seek that finds nothing) or 4 (shared). A tick
  with 3 new facts of 40 new in the layer (`[:k]` over `:ix-s`) → 3 + 1
  page of 40 iterated. `[:ek]` with 2 new → 3 + 1 page of 3. **Variable**,
  the same loop from a bound.
- **`standing-close`.** d delivery lines → 1 + 1 page + d rows (1 to 2
  each). **Variable**, a `loop<-` over the page.
- **`standing-open`.** A layer with no standing read → 1 seek (the empty
  `read/standing␀` range, the answer "none"), the closed range not read. A
  layer with 30 standing reads of which 28 closed → 2 range seeks + about
  60 iterations. **Variable**, conditional second range.
- **`entry-ids`.** One `read/` range in doubling pages until n ids or the
  range's end; a session with 1,000 entry facts → 7 page seeks for all of
  them over four calls of 256. **Variable**, the page loop, stopping at n.

## Partitioning efficiency

**Optimal placement first.** The dominant read of this stage is a shared
pattern read (RS1). It wants every entry it scans on one task and the
layer's settings, members and frontier on the same task; the micro store
already keeps a layer's settings, members and permissions on `hash(L)`,
and the frontier is on every task. So `f(index entries of L) = hash(L) mod
N`, the same `f` as the layer's per-layer data, implemented by fields of
`$$micro [L]` (Option B). Entity-scoped entries could also sit on `hash(e)`
(Option C) at no read cost for the scan but one more task per read for the
settings; key-scoped and whole-layer forms want `hash(L)` whatever else
holds. The table is for B; the categories are the pattern families over
shared layers, the base after re-class among them.

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| entity-scoped form, group layer, about 12 matches | 0.5 | 5 | 16 |
| key-scoped form, group layer, about 60 matches | 0.3 | 7 | 61 |
| `[:all]`, small group layer, about 200 facts | 0.1 | 8 | 201 |
| key-scoped form on the re-classed base, 40 stream-era and 60 micro-era matches | 0.1 | 7 | 102 |
Weighted seeks = 6.1   |   Weighted iterator reads = 56.6

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| entity-scoped form, group layer, about 12 matches | 0.5 | 5 | 16 |
| key-scoped form, group layer, about 60 matches | 0.3 | 7 | 61 |
| `[:all]`, small group layer, about 200 facts | 0.1 | 8 | 201 |
| key-scoped form on the re-classed base, 40 stream-era and 60 micro-era matches | 0.1 | 7 | 102 |
Weighted seeks = 6.1   |   Weighted iterator reads = 56.6

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| entity-scoped form, group layer, about 12 matches | 0.5 | 5 | 16 |
| key-scoped form, group layer, about 60 matches | 0.3 | 7 | 61 |
| `[:all]`, small group layer, about 200 facts | 0.1 | 8 | 201 |
| key-scoped form on the re-classed base, 40 stream-era and 60 micro-era matches | 0.1 | 7 | 102 |
Weighted seeks = 6.1   |   Weighted iterator reads = 56.6

Flat, because every read touches one task whatever N. (The seeks: group
layers pay 4 before the first page, the base 2; pages double from 16, so
60 matches take pages of 16, 32 and 64, and 200 take four pages.
Opening a shown value adds one local `$$persons` seek per wrap person,
block-cached for a hot person, the same at every N and left out of both
columns, as the read exit's table leaves out `open-row>`.) Option A at N =
128 would pay 128 seeks for every key-scoped and whole-layer read (weighted
seeks 0.5 × 5 + 0.3 × 132 + 0.1 × 132 + 0.1 × 132, about 68, against 6.1
at N = 1), which is why it was rejected. The write side, per admitted
act in a shared layer: one hop to `hash(L)` and 3 puts per fact (plus 2 for
a value-indexed fact, plus 1 `:ix-id`), no seek, at every N.

The standing read's quiet tick: 3 seeks (one-owner) or 4 (shared) on one
task at every N; its busy tick adds one iteration per new fact of the
layer.

## Design Decisions

- **One task per read, both kinds.** Both stores keep per-layer data on
  `hash(L)`; the shared indexes go there too, so the exit's first query
  already stands where every answer is.
- **Subindexing.** All six micro fields and the one-owner `:ix-s` are
  subindexed maps (unbounded per layer); `:ix-of` sets are plain (bounded
  by the value-indexed kinds); `$$micro-task :layers` is a subindexed set.
  Size tracking off everywhere.
- **The frontier rule on entries**, not on addresses: an entry's `:batch`
  hides it; the address orders by entity, key, stamp as the read exit's,
  so the same address function and page loop serve both stores.
- **Erasures cross the frontier**: a purge overwrites entries in place
  (tombstones), the one overwrite in the micro index, by the rule phase 3
  states for erasures.
- **A poll at the delivery rate** holds a standing read open; a proxy
  cannot show a value.
- **The close act's drop is phase 2's OP9**, paged by act size, with the
  mark `:own-row` making it the ordinary road.
- **Person forgets are paged** in both stores, because one person's values
  can exceed one event's bound; their pages are idempotent and resumable.
- **A purge writes what a rebuild would**, so restore, rebuild and replay
  reproduce the indexes exactly.

## State primitive selection

- `$$layers [L :ix-s]`, `$$micro [L :ix-*]` (PState fields): durable,
  co-located with the data they index, written in the admitting event or
  batch; per source event, 3 to 6 puts per fact.
- `$$micro-task :rebuild`, `:layers` (PState fields): durable, tiny.
- The standing read's handle (client memory): not durable by design; the
  record has everything a close needs.
- No TaskGlobal, no external system, nothing cached.

## Resource usage analysis

### Disk usage (PStates), per fact

- **One-owner:** the read exit's arithmetic (about three times a plain
  fact's bytes for `:ix-ek` and `:ix-ke`) plus `:ix-s`, one more copy of
  the row with a shorter address (about 90 characters): about four times a
  plain fact's bytes, about six for a value-indexed one. The `:read/*`
  facts stay `:no-copy` (F7) in all three id indexes: about 200 bytes each.
- **Shared:** `:ix-ek`, `:ix-ke`, `:ix-s` copies of the micro row (sealed
  bytes, the wrapped lock record of about 169 bytes, BENCH_NOTES-locks.md,
  the digest), `:ix-id` about 100 bytes: about four times a fact's bytes on
  the layer's task, six for a value-indexed one. Growth: every admitted
  fact of every shared layer, for ever (the log's growth times four).
- **Lock rows** for read entries in agent layers (the mark): one per
  entry value, about 169 bytes, moved from the row, not added.
- **`$$micro-task`**: a few hundred bytes per task.

### Memory usage (TaskGlobals)

None.

### Minimization

- `:ix-s` could hold only the fact id and its `:ix-ek` address, making
  every delta pay a seek per new fact; not taken (reads on every tick
  against one write).
- The shared id indexes could shorten the fact id text in addresses, as
  the read exit's minimization notes; not taken tonight, rebuildable later.
- A hot shared layer's index could move to Option C or a `[L k]` placement
  to spread its bytes; not taken until measured (RR5).
- Data duplicated across locations: each index is a copy of the row by
  design (reads without a seek per fact); the rows themselves are not
  duplicated elsewhere.

## First-record picks (placeholders for edition one; none is a ruling)

Continuing the read exit's FR1 to FR14, which stand.

- **FRR1 to FRR4.** A standing read's lines: the opening fact
  `:read/standing {:layer :pattern :role :moment}`, with the first delivery
  in the same act when the initial read matched; a delivery fact
  `:read/delivery {:layer :moment :after :role :count :mark :fingerprint
  :fp-secret :exact :max-stamp}`, one act per delivery that shows something
  new; the closing fact `:read/closed {:layer :moment :deliveries
  :fingerprint :fp-secret :mark :closed-by}` with the closing fingerprint
  over the vector of the lines' `[stamp fingerprint]` under the fingerprint
  secret, prefix `"softland.standing-fp/1\n"`; the mark `:partial` when any
  delivery was; the three keys as store-owned constants, one entity per
  standing read.
- **FRR5.** The close act's part `:reads :keep | :drop` in phase 2's
  `:session-closed` value; absent means keep.
- **FRR6.** Every read entry fact is marked `:own-row` at write, in every
  working layer, so its lock is a lock row (ruling 7's override).
- **FRR7.** A restore is an operator act in the store layer `:people`,
  `{:e :store :k :restore :v {:indexes :rebuilt}}`.
- **FRR8.** A shared read's recorded moment is `{:frontier F}` with F =
  `min(asked, the frontier on the layer's task)`; a shared line carries
  `:max-stamp`, the largest matched stamp, which the gate's stamp rule
  counts (F1 extended); a shared row carries `:batch`.
- **FRR9.** A drop is recorded as phase 2's ordinary forget acts, 256
  targets each, `:because-of` the close act; each dropped entry's erasure
  date is its forget act's stamp.

## Rig choices (change without touching a record)

- RR1. A stamp moment on a shared layer is refused `:moment-kind`.
- RR2. Group membership is read as it stands, not as of F (no removal
  exists yet).
- RR3. `[:latest]` at F takes at most 16 tail steps, then `:partial`.
- RR4. `open-entry>` does not hop for a `:no-copy` entry; it answers
  `{:unreadable :no-copy}` (no `:read/*` fact lives in a shared layer).
- RR5. Every index of a shared layer on the layer's task (Option B); C or
  a `[L k]` placement is a rebuild away.
- RR6. An index error in block 2d writes nothing for that act's indexes and
  counts `:index-error` on the task; the next rebuild restores them.
- RR7. Micro put pages of at most 64 entities and 4,096 rows; sweep pages
  of at most 512 entries.
- RR8. `*micro-index-ops` and `$$micro-task :rebuild` are maintenance, not
  acts, as the read exit's `*index-ops`.
- RR9. Standing reads poll at the delivery rate; a doorbell proxy is the
  named upgrade.
- RR10. A delta cut by the limit resumes after the last shown address.
- RR11. The closing, open-entries and entry-ids queries are maintenance
  reads, ids and stamps only, not recorded (For Sid); tonight their
  callers are trusted (the rig's clients); a kept store's gateway allows
  them to the session's owner and the operator only.
- RR12. A standing read of a re-classed layer delivers its micro era only.
- RR13. The gate needs no branch for the close act's `:reads`; the closer's
  procedure reads it.
- RR14. Drop pages of 256 targets.
- RR15. A drop takes effect on each entry when its forget act is decided,
  not at the close act.
- RR16. `:person-purge` pages route by `:task` with `(|direct)` on the read
  exit's `*index-ops`.
- RR17. The micro person purge sweeps the task's value-indexed entries; a
  subject index by person is the named upgrade.
- RR18. Person purges are driven by the operator's loop; the store-driven
  first page from phase 2's fan-out is the named alternative.
- RR19. No class hint on the exit's call; a group-layer read pays the
  stream-settings miss as its kind test; `:hint :group` is the named
  upgrade.

## Namespaces and tests

Code: `src/rig/store/shared_reads.clj` (the shared path's pure functions,
fragments and `open-entry>`, the micro index fields and writes, the micro
purge and rebuild pages, the three maintenance queries, the install
functions), `src/rig/store/standing.clj` (client side: `subscribe!`,
`deliver!`, `unsubscribe!`, `close-session!`), and in `read_exit.clj`,
`drop-reads!`, `purge-person!`, `restore!` (client side, beside
`rebuild!`); the edits named in "What the read exit's build must change" to
`reads.clj`, and the lines in `module.clj` and `micro.clj`. Tests:
`test/rig/store/shared_reads_test.clj` (pure: moment, visibility, merge,
index and purge writes, the purge-equals-rebuild property, delta plans,
closing fingerprint; no cluster), `test/rig/store/reads_rest_test.clj` (the
in-process cluster, 4 tasks, under the machine-wide lock), run from the rig
folder with
`flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:test rig.store.shared-reads-test rig.store.reads-rest-test`,
then phases 1, 2, 3 and the read exit's suites again, by namespace, so
Sid's `gate_test.clj` never loads.

The tests the brief names, each with its setup and what it asserts:

- **RT1. A shared-layer read never shows half a batch.** A group layer
  `:g` with members `:alice` and `:bob`; acts of two facts each, one on an
  entity of task 1 and one on an entity of task 3 (computed from the
  partitioner), 200 of them from two offerers, while a reader loops
  `read!` `[:k :note]` and `[:all]` on `:g` 500 times. Every answer: no
  row's `:batch` is above the answer's recorded F; for every act, both of
  its facts are shown or neither (trivial while both entries sit on `:g`'s
  task, and asserted so that a later placement, Option C, is held to it);
  for every shown fact, `micro-lookup` of its act's name at the same F
  answers `:yes` (an answer and a read at one frontier never contradict).
  Deterministic part: an act admitted in batch b; `read!` with `:as-of
  {:frontier (dec b)}` shows neither fact, with `{:frontier b}` both.
  [build checks: a hook (R3's global atoms) holding block 2b on task 3 for
  one batch, so the read runs while batch b is committed on `:g`'s task and
  not on task 3; the read shows neither fact. Fallback: the race version
  and the explicit frontiers only.]
- **RT2. Its entry carries the frontier id.** After RT1's reads, each
  entry's line has `:moment {:frontier F}` equal to the F the answer
  reports; F is at most the frontier read on any task afterwards; asked
  `{:frontier 10^9}`, the line records the task's frontier (the clamp);
  `:max-stamp` equals the largest stamp in `:exact`; the entry's stamp is
  above `:max-stamp` (F1 extended), checked with the reader's working
  layer on another task than `:g`'s.
- **RT3. A standing read: one entry, a line only for something new, a
  closing at unsubscribe.** Alice subscribes `[:e :e0]` on her layer
  (one-owner) and a tool subscribes `[:k :note]` on `:g` (shared). The
  opening: one act, one `:read/standing` fact, plus one `:read/delivery`
  line when the initial read matched. Ten `deliver!` calls with nothing
  admitted: `:nothing-new` each, and the entry's facts unchanged (counted by
  `entry-ids`); one new matching fact, then `deliver!`: one delivery line,
  whose `:exact` for Alice is exactly `[[fid stamp]]` of the new fact and
  whose `:fingerprint` for the tool equals `reads/fingerprint` of that one
  pair, with no `:exact`; an unmatched new fact: `:nothing-new`. Nothing
  shown before its line: the hooks of the read exit's T2 on each delivery's
  name (`:after-writes` before `:exit-shown`). `unsubscribe!`: one
  `:read/closed` fact, `:closed-by :unsubscribe`, `:deliveries` the number
  of lines, `:fingerprint` equal to the closing fingerprint computed in the
  test from the lines' stamps and fingerprints (the pure function), `:mark
  :complete`; with a limit of 2 and 3 new facts, the delivery is `:partial`
  and the closing mark is `:partial`.
- **RT4. Closing at session close, and after a crash.** An agent session
  (`:alice-agent`) holds two standing reads; its door's orderly
  `close-session!` closes both with `:closed-by :session-close`, then offers
  the close act. A second session holds two standing reads and its door
  "dies" (the handles are dropped; `inject/point! :exit-shown` armed on a
  delivery, so its line is recorded and the delivery not shown); the
  operator's `close-session!` finds both open entries through
  `standing-open`, closes each with `:closed-by :crash`, and each closing
  fingerprint equals the one the live handle would have produced (the test
  computes it from the recorded lines); the line recorded for the unshown
  delivery is counted (the over-recording named). After both closes,
  `standing-open` returns nothing.
- **RT5. A dropped agent session's entries cannot be opened afterwards;
  a kept one's can.** Two agent sessions of Alice with point, pattern and
  standing reads. Session one closes with `:reads :drop` and
  `drop-reads!` runs, interrupted once by a thrown hook between two pages
  and run again; session two closes with no `:reads` part (kept). For
  session one, every `:read/*` fact admitted before its close act: no lock
  row at its lock id, a ledger entry `:how :row-deleted`, `open-value>`
  gives `{:erased-at s}` with s its forget act's stamp, a read through the
  exit shows the row erased with its date and no value, its index entries
  are tombstones, and each forget act's `:because-of` is the close act's
  name. For session two, every entry opens to its value through the exit
  (Alice as `:for`), and its lock rows exist. The mark: every entry fact in
  both sessions carries `:own-row`, and none of their locks is in a row's
  `:lock` field.
- **RT6. A value forget leaves no index entry that opens or confirms the
  value, in both stores.** A value-indexed fact f with a distinctive text in
  Alice's layer and one in `:g`; each forgotten (Alice's OP9; the
  operator's nil-tagged forget in `:g`). Then, over every index field of
  both stores (`:ix-ek`, `:ix-ke`, `:ix-s`, `:ix-kv`, `:ix-of`, and the
  shared `:ix-id`) read whole by `foreign-select`: no address and no entry
  contains the text; every entry for f has nil value fields (`:v`, the
  sealed bytes, `:lock`, `:digest`) and `:erased-at` equal to the ledger's
  date; no `:ix-kv` or `:ix-of` entry for f exists; `[:kv k text]` matches
  nothing and its fingerprint is the empty set's; `[:e e]` shows f with its
  date only; the shared tombstone and the ledger were written in one batch
  (no read at any frontier sees the lock excised and the entry not
  tombstoned: RT1's loop runs across the forget).
- **RT7. A person forget leaves no index entry that opens or confirms any
  value that dies with the person, in both stores, and keeps the rest.**
  Values about Bob alone, about Bob and Carol, and marked to die with any
  subject, in Bob's layer (one-owner, the owner's lock required), in
  Alice's layer mentioning Bob (dies with Alice, survives Bob), and in
  `:g`. The operator forgets Bob, then `purge-person!` runs (once
  interrupted between pages and resumed). Every value that no longer opens
  (checked by `open-value>` and `open-entry>`) has no `:ix-kv` or `:ix-of`
  entry, tombstones in the id indexes with Bob's date (or its earlier value
  forget's date, for one value forgotten before Bob: L16's order), and no
  text anywhere in the indexes; every value that still opens (7b's
  survivors, Alice's mention of Bob) keeps its entries and still matches its
  `[:kv]` read. With phase 2's seam absent, the fallback sweep gives the
  same fields.
- **RT8. A restore then rebuild then forget replay reproduces the indexes
  exactly.** After a history on both stores with offers, replaces,
  retracts, control acts, read entries, standing reads, value forgets in
  both stores, a person forget, a dropped session and one value forgotten
  after the person forget: snapshot every index field of both stores.
  `restore!`: the restore fact is admitted in `:people`; every index field
  of both stores emptied by the test-only `:drop` ops; the rebuilds run
  (both stores, small pages, one page resent after a forced append error);
  the forget replay runs. Every field equals the snapshot, entry for entry
  (the purge invariant), and so does it after the replay a second time.
  The pure half: for every row, `put-page-writes` over the row's open
  result equals `purge-writes` at the date open gives, in both stores
  (property test).

Tests the design adds:

- **RT9. Deltas are exact.** One-owner and shared: 30 deliveries while
  two offerers write matching and unmatching facts; the union of the
  delivered `[fid stamp]` sets equals the final full read's matched set, and
  no fact is delivered twice; a delta of limit + 5 facts is delivered over
  two deliveries, the first `:partial`, each fact once.
- **RT10. Shared visibility and moments.** Bob, a member, reads `:g`; Carol,
  not a member, gets `:not-visible`, the same answer as for a layer never
  made; Bob reads the base after its re-class; `:as-of {:stamp s}` on `:g`
  is refused `:moment-kind` and nothing is recorded; a point read of five
  fids on `:g`, two never admitted, gives three rows and two `:absent`.
- **RT11. The re-classed base reads both eras.** Facts on the base before
  and after its re-class; `[:k k]` and `[:all]` show both eras in address
  order, the limit applied across both; a stream-era fid by point read.
- **RT12. `[:latest]` at a frontier.** A chain replaced in batch b; a read
  at F = b - 1 gives the old head, at F = b the new one.
- **RT13. No throw.** Property tests over the new pure functions (moment,
  visibility, merge, index writes, purge writes, delta plan, matches,
  closing fingerprint, the ops' parsers) with generated garbage; malformed
  `*micro-index-ops` and `*index-ops` records and malformed `:after`,
  `:as-of` and `:reads` parts answered as data, with no worker restart.
- **RT14. The earlier suites still pass**: phase 1's, phase 2's, phase 3's
  and the read exit's, by namespace.

## Spec coverage, self-check

- **"Point reads and pattern reads" on shared layers.** RS1, RS2, through
  the exit's two queries; RT1, RT10, RT11. Fault: a worker restart during a
  query fails it; nothing shown, nothing recorded (the read exit's rule).
  Race: a batch committing on the layer's task during a yield between pages
  has batch F + 1 or above, hidden. PASS.
- **"As of a settled frontier, so no reader sees half a batch."** The batch
  filter; F read on the task the read runs on; RT1, RT2. PASS.
- **"The entry records the frontier's id in the moment slot."** FRR8;
  RT2. PASS.
- **"How a pattern read across entities stays affordable: which indexes,
  where, written in which batch, purgeable by value id, rebuildable from the
  log."** "The micro store's indexes"; the table; RT6, RT8. PASS.
- **Standing reads, R6 clause by clause.** One entry opened with pattern,
  role and moment (FRR1); a stamped line per delivery showing something new
  with delivered ids, exact or fingerprint by kind (FRR2, RT3); recorded at
  the delivery rate (the poll is the delivery); nothing new, no line (RT3);
  closed at unsubscribe or session close with a fingerprint over everything
  delivered and a mark (FRR3, RT3, RT4); left open by a crash, closed at
  session close (RT4). Rama's means named and chosen with reasons. Nothing
  shown before its line is acknowledged: `deliver!`'s order (RT3). PASS.
- **The close act.** Keep, drop, default keep (FRR5); drop through phase
  2's value forget (FRR9, RT5); the mark answered: yes, and what it changes
  (FRR6). PASS, with RR15's window for Sid.
- **Indexes and forgets whole.** Value forget in both stores (RT6); person
  forget in both stores (RT7); restore as a fact, rebuild in pages, then the
  forget replay (RT8); phase 2's `:by-stamp` and the ledger named (ids,
  kept). PASS, with the contracts taken from phases 2 and 3 named.
- **"A gate never throws on an offer; every refusal is data."** Every new
  step total; the micro index error counted, not thrown (RR6); RT13. PASS.
- **Rule 8, key and lock.** Checked through the text: "key" is a fact's
  key; "lock" an encryption key; the HMAC input is the fingerprint secret.
  PASS.

## Design difficulty log

1. **Where the micro store's indexes live.** Genuinely contested. The
   micro store places by entity to spread a shared layer's load; putting
   the layer's whole index on its one task looks like undoing that. What
   settled it: the micro gate already visits the layer's task for every
   offer (settings, permissions), index writes are puts without seeks, and
   any placement by entity makes key-scoped and whole-layer reads fan out
   to N tasks, which the partitioning rule forbids. Option C spreads only
   half the bytes and splits every entity read across two tasks. The
   concentration of a hot layer's index bytes is real and is the reason
   RR5 is a rig choice with its trigger named.
2. **One query entry or two.** I first drew a third query topology for
   shared layers; F4 (a missing layer and a private one answer alike) means
   the exit cannot learn a layer's kind without a round trip, and the
   co-location of both stores' per-layer data on `hash(L)` makes the
   branch free. Forced once F4 was read against it.
3. **Proxy or poll.** A proxy is Rama's own and looked like the obvious
   "holding a read open". Two facts decided it: values open only in the
   module, so a proxy on data can show nothing; and R6 sets the rate at the
   delivery rate, which is the poll's rate by definition. Not close once
   default 1 was applied.
4. **Deltas over which index.** The first delta I wrote filtered the
   pattern's own range by stamp; tracing a standing `[:all]` on a large
   layer showed it rescanning the prefix every tick and hitting F8's
   budget. `:ix-s` costs a put per fact in every one-owner layer, which
   phase 7's first number will see; phase 2's `:by-stamp` would avoid the
   put and pay a seek per new act per standing read per tick. Close; the
   skill's write-once rule decided.
5. **The closing fingerprint.** R6 says "a fingerprint over everything
   delivered". Over the delivered pairs is the literal reading, but a
   tool's lines hold no pairs, so after a crash nothing could recompute it;
   and the fingerprint secret never leaves the module, so the client could
   not compute it either. Over the lines' fingerprints, in the module, from
   the record, works for every reader kind and every way of closing. A
   reading, first-record, surfaced for Sid.
6. **The drop.** Three roads: the close act's decision forgetting every
   entry in its own event (bounded by nothing), a new paged gate op
   destroying locks under the close act as the one forget fact, and
   phase 2's OP9 acts in pages. The brief says "through phase 2's value
   forget", and "every forget is a fact"; OP9 acts are both, need no gate
   change, and are resumable. The price is the window between the close
   act and the last page (RR15), surfaced rather than closed by a change to
   phase 2's open.
7. **The mark.** Whether to mark entries at all: phase 2's forget reaches a
   record lock by excision, so a drop works unmarked. Ruling 7 calls
   excision the operator's fallback; using it at every close is the wrong
   road even though it runs. Settled by the ruling's words.
8. **The person purge on the micro side.** An enumeration seam like phase
   2's would be cheaper; designing it here would be designing phase 2's
   and 3's black box. The sweep is correct and bounded per page, rare, and
   the subject index is named as the upgrade.
9. **What a purge dates.** I first wrote the forget's own stamp into every
   tombstone; tracing L16 (a value forget after a person forget shows the
   value forget's date) and a rebuild after both showed the purge and the
   rebuild disagreeing. The invariant "a purge writes what a rebuild would"
   fixed both, and it is what makes RT8 exact.

## What stays open

- **Carried, not closed: a read's moment across the two stores**
  (PROGRESS "Open while the rig runs"; the model's x2). A shared read
  records `{:frontier F}`, a one-owner read `{:stamp s}`; a read of a
  re-classed layer shows its stream-era facts, and a stream-side fact of
  that layer admitted after the re-class (P16's settings) is bounded by
  neither F nor any stamp the entry records, so a re-run at F can show one
  more such fact (the micro plan's §D open edge, now located here: the
  merge step). A standing read of a re-classed layer does not deliver them
  (RR12). A stamp moment on a shared layer is refused (RR1). Either a read
  closes its moment in every store it read, or the entry carries one
  moment per store; neither is chosen.
- **For Sid** (each touches a record): FRR1 to FRR9; whether store
  maintenance may read ids and stamps without an entry (RR11: the closing
  fingerprint, the open-entries and entry-ids queries); whether a drop must
  take effect at the close act (RR15); whether a standing read must deliver
  a forget of a fact it delivered (not tonight); the closing fingerprint's
  reading (over the lines, difficulty 5); what a restore records (FRR7).
- **For phase 2** (black box): the enumeration seam `dying-with>` (contract
  above; fallback named); its forget effects at a replay seam; `:reads` in
  the session close's value parser; whether `:by-stamp` stays now that
  `:ix-s` serves `read-as-of`'s range.
- **For phase 3** (black box): block 2d after block 2b's name-task check,
  and the plaintext of value-indexed keys reaching the layer's task inside
  the batch; the forget's row seam giving the fact ids erased and the date;
  `$$micro-task :rebuild` and `:layers`; a third source; membership rows
  with a batch once a removal exists.
- **For the build** [build checks]: a `view` over a subindexed set; a range
  over a PState's top-level keyword keys (fallback, a per-task entity set);
  a third `source>` in the micro topology from a function; the block 2b
  hook for RT1; the read exit's own checks, which this plan's branch shares.
- **For a kept store**: where the forget facts a restore replays live when
  the log is restored too; a hot shared layer's index concentration (RR5);
  RocksDB keeping deleted addresses' bytes until compaction.
