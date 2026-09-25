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
seek (the frontier) on the shared path only.

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

Chosen: B, as a rig choice (RR5): indexes are not records (they are
rebuildable from the log), so moving a hot layer's indexes to C, or to a
placement by `[L k]`, later is a rebuild, not a record change. What would
force the move: a shared layer whose index outgrows one task's disk or
whose index puts exceed one task's write rate; phase 7 can measure the
latter.

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
   digest, and learns from the name task that the act was admitted with its
   stamp and batch. There `(shared-reads/index-writes hints *layer *nm *rows
   *stamp *batch :> *d)`, pure and total, computes the act's entries (the
   rows as written, with `:e`, `:fid`, `:stamp`, `:batch`), their addresses,
   the `:ix-of` sets and the `:ix-id` addresses. An `:ix-kv` address holds
   the value's canonical text, which is why it is computed here: this is
   the one place the text exists. **A change to phase 3's discipline, named
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
construction (test RT6).

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
  the row's lock and the entity task's `$$persons`; the ledger on this task
  gives the erasure date), then `(shared-reads/put-page-writes ...)`, pure:
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
frontier, one page or tail read) and no write. Named upgrade, a rig choice
that touches no record (RR9): a doorbell proxy on a per-layer long, so a
tick on a quiet layer skips its query; the lines are the same either way.
Taken if phase 7 or src-inland shows empty polls to matter.

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
delta cut by the limit is a `:partial` delivery (below), and the next
delivery starts after the last acknowledged moment, so the facts beyond the
limit are delivered by no later delta (RR10: the next delta resumes from the
last *shown* address inside the old moment first, then the new range; the
last shown address is computed from the last shown row, so nothing of the
unshown entry leaks, F3 kept). [Derived; a test drives a delta of limit + 5
facts over two deliveries and checks each is delivered once.]

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
  Steps: the delta query with `:after` the handle's moment; no match gives
  `:nothing-new`, nothing offered, nothing shown, the handle's moment
  unchanged (so a later fact admitted before the unchanged moment's clamp
  cannot be skipped: the next delta starts where the last acknowledged one
  ended); a match builds one FRR2 act, offers it until answered, and on
  `:yes` advances the handle's moment and returns the rows; on `:no`
  returns `{:refused r}` and shows nothing, the moment unchanged.
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
  `:ix-ke` range `read/standing␀`, the entities in its addresses; for each,
  one seek at `:ix-ek` prefix `ent␀read/closed␀`; ids only, a maintenance
  read like the closing query), and closes each with `:closed-by :crash`,
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
   tombstoned), from the five `read/…␀` prefixes of `:ix-ke` in turn, at
   most n (256) per page; ids and stamps only, nothing opened (a
   maintenance read, RR11).
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
