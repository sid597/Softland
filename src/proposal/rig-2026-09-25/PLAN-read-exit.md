# Plan — stage 5a, "read exit" (one-owner layers)

<!-- Phase 1 of the rama skill for the first part of Sid's phase 5, written
26 September 2026, 01:48 to 02:50 IST, by a fresh-context session (Claude
Opus 5.5, effort max) in worktree /mnt/data/projects/Softland-rig-plan-reads,
branch rig-plan-reads. Plan only; the only code is two probes under runs/.
Sources read: SPEC.md (phase 5, "What Rama showed"), RIG.md, STARTER-next.md,
PLAN-stream-store.md (PStates, stamp, "Rig choices proposed", "What later
stages consume"), src/rig/store/*.clj, formal-model-2026-09-24/README.md
"The readings" and model.clj's reads (`read-as-of`, `erasure`,
`chain-head`, `op-read`), and on main PROGRESS.md "Now" and
store-next-2026-09-25/CONCLUSION.md R1 to R6. Where a line here summarises a
source, the source wins.

Vocabulary. "Key" is a fact's key only. "Lock" is an encryption key only.
The HMAC input the brief calls "a store key" is called **the fingerprint
secret** here, so "key" keeps one meaning. The sorted String under which an
index entry sits is its **address**, never its key.

Status words. **Ruling**: Sid's, in PROGRESS.md "Now". **Default**: one of
CONCLUSION.md's R1 to R8, which tonight's brief takes as a default; none is
ruled. **Rig choice**: a pick of this plan that can change without touching
a record. **First-record**: a pick that touches what a record carries (its
bytes, a stamp, an id, what an entry holds); a placeholder for edition one
to review, listed in "First-record picks".

Checked, derived, assumed. Every Rama behaviour this plan leans on is marked
**[probed]** (ran tonight, output cited), **[phase-1 ran]** (the phase 1
suite or its probes showed it), **[docs]** (the rama skill's references say
so, not run here) or **[build checks]** (unverified; the build runs a check
first and the fallback is named). -->

**Validated, 26 September, 02:13 to 03:00 IST (phase 2 of the rama skill):
minor-fail, fixed in place by the validator. Every fix is marked `[F1]` to
`[F12]` where it lands; PLAN_VALIDATION-read-exit.md has the traces. Where
text below a fix still says otherwise, the fix wins.**

## Scope of this stage

One exit for reads on one-owner layers (personal, hand session, agent
session, and the base while it is one-owner, CONCLUSION R8 as a default),
built on the stream store as phase 1 left it:

- **Point reads** (by fact id) and **pattern reads** (by entity, entity and
  key, latest of an entity and key, key, key and value for keys hinted to be
  indexed by value, the whole layer) on the layer's home task, **as of a
  moment**. A read as of a moment shows nothing admitted after it, except an
  erasure, which shows only its date (the sharpening "Forget, time travel").
- **One exit** (CONCLUSION R5, a default): query, append the read entry,
  then answer. Nothing is shown before the entry is acknowledged; a refused
  entry hides the answer. Agent session reads are recorded too.
- **The read entry** (ruling 3; CONCLUSION R3 and R4, defaults): a row per
  point read; one line per pattern read with pattern, moment, role, a keyed
  fingerprint of what matched and a complete-or-partial mark; empty pattern
  reads included; a person's or a model's pattern line also carries the
  exact list; a deterministic tool gets the short line unless it asks for
  rows. The entry is an ordinary act in the reader's working layer, offered
  through the ordinary client path.
- **open-value** (phase 2's `open-value>` and its twin `open-row>`, F6), the one function every shown value passes through; a
  pass-through tonight; phase 2 supplies its body.
- **Indexes** only for what the pattern reads need, each over values
  purgeable by value id and all of them rebuildable from the log, with the
  two functions phase 2's forget and a restore will call.

Not this stage, each with where it plugs in (section "Later stages and where
they plug in"): standing reads (CONCLUSION R6); shared-layer reads through
phase 3's settled frontier; the agent session's close act that keeps or
drops its read entries (after phase 2's forget); grammar facts as index hints
(phase 6); the sealed values and the real open step (phase 2).

Later stages are black boxes: this plan says what it leaves and what it
consumes, and never relies on how they will work. Two interfaces tonight are
consumed from parallel builds, `rig.store.clock` and `rig.store.locks`; this
plan names the smallest contract it needs from each.

## What this plan found before designing (two probes)

The phase 1 plan says a chain's latest head "is the max stamp over the keys
with prefix `[e k]` — a range scan for stage 5" (PLAN-stream-store.md, RD
table, `:heads` row). **That range scan does not exist in Rama 1.6.0.**
[probed] `runs/probes/rig/probe/read_keys_probe.clj`, output
`runs/phase5-read-keys-probe.txt`: nine PersistentVector addresses
`[e k stamp fid]` in one subindexed map came back ordered neither by element
nor by value: `[:e0 :note 117309440000000003 ...]` before `[:e0 :note 5 ...]`,
`[:e0 :note ...]` before `[:e0 :mention ...]`, `[:e1 ...]` before
`[:e0a ...]`; and every range bounded by a shorter vector
(`[:e0 :note]` to `[:e0 :note 13]`, `[:e0]` to `[:e0 :zzzzzz]`, and
`sorted-map-range-from [:e0 :note]`) selected nothing. The skill's rule
"sorting is lexicographic based on the serialized form of the key" holds;
the serialized form of a vector is not its elements in order.

[probed] `runs/probes/rig/probe/read_keys2_probe.clj`, output
`runs/phase5-read-keys2-probe.txt`: standalone **String** addresses sort as
Java strings (`"ab" "abc" "b" "e|note|..." "e0|mention|..."`, with `|`
standing for U+0000); a range from `"e0␀note␀"` to `"e0␀note␀<hex 13>"`
selected exactly the two entries with stamps 5 and 12; a range from
`"e0␀"` to `"e0\u0001"` selected exactly the four `e0` entries and not
`e0a`; `sorted-map-range-to <address> {:max-amt 1}` returned the one entry
just below the bound (a tail read). Standalone **Long** addresses sort
numerically, negatives first.

Consequences. Every index in this plan is addressed by a String built from
parts separated by U+0000, with stamps as 16 lowercase hex digits, so a
prefix is a range and "as of T" is a range bound. Phase 1's `:heads` is
unaffected (it is read only by exact address); its note about a range scan
is wrong and is reported in the receipt, not changed (not this worktree's
file to rewrite beyond the plan).

## The shapes every section below uses

**Ids, from phase 1, unchanged.** Layer, entity, fact key and person ids are
keywords (P3). A **fact id** is `[name idx]`: the act's name
`[layer class scheme uuid7]` (P4) and the fact's position in the act. It is
the identity the phase 1 record already carries; this stage makes no new id
for facts. "Value id" means fact id.

**A fact's stamp** is its act's stamp: the `:stamp` of the answer record
under the fact id's name, for an act answered `:yes`. Stamps are longs from
`rig.store.clock` (CONCLUSION R2 as a default, ms × 65536 + counter, being
built tonight); this stage needs only that stamps are positive longs,
strictly increasing per task, and that the home task's last stamp is the
long at `$$clock` (STAY) on that task, as in phase 1. Nothing here depends
on the split into milliseconds and counter, except that a shown erasure date
is a stamp and its calendar date is `rig.store.clock`'s to derive for display.

**Moment.** A read's moment is a map with exactly one part:
`{:stamp s}` for a one-owner layer, inline (CONCLUSION R3). A shared layer
will put `{:frontier id}` in its place (phase 3's settled-frontier id); the
reader of an entry dispatches on the one part. Nothing else about frontiers
is built. **First-record.** The moment of a read is
`min(asked, clock)` where `clock` is the home task's `$$clock` read in the
same query and `asked` the reader's `:as-of` (absent means `clock`). Why the
clamp: every fact admitted on that task after the read gets a stamp above
`clock` (stamps strictly increase per task), so a read as of `min(asked,
clock)` is final: re-running it later shows the same facts, and differs only
where an erasure has since happened, which shows only its date. A read asked
for a moment past the clock would otherwise record a moment whose answer can
still grow. The clamp decides which stamp an entry records, so it is
first-record too (FR2), with the moment's form (FR1).

**Pattern** (tonight's language; data, validated by a total parser; a
pattern outside it is refused as data, `{:refused :bad-pattern}`, before
anything is recorded):

| pattern | matches, in layer L, admitted at or before the moment | index | order of the answer |
|---|---|---|---|
| `[:all]` | every fact | `:ix-ek`, prefix "" | entity, key, stamp, fact id |
| `[:e e]` | every fact about entity `e` | `:ix-ek`, prefix `e␀` | key, stamp, fact id |
| `[:ek e k]` | every fact about `e` with key `k` (the chain's history) | `:ix-ek`, prefix `e␀k␀`, bound `stamp ≤ T` | stamp, fact id |
| `[:latest e k]` | the latest fact about `e` with key `k`, which heads its chain as of T | `:ix-ek`, tail read below `e␀k␀hex(T+1)`, one entry | — |
| `[:k k]` | every fact with key `k` | `:ix-ke`, prefix `k␀` | entity, stamp, fact id |
| `[:kv k v]` | every fact with key `k` whose value was `v` when admitted | `:ix-kv`, prefix `k␀len␀v␀`, bound `stamp ≤ T` | stamp, fact id |

`[:kv k v]` is answered only when the hints mark `k` as indexed by value;
otherwise it is refused as data, `:not-indexed` (a scan of `[:k k]` filtered
by value is the reader's to do, as a `[:k k]` read, recorded as such). A key
the hints mark opaque (ruling 6) is never indexed by value, and `[:kv k v]`
on it is refused `:opaque`. Rig choices, all of them: the pattern language
changes with no record touched, except that a recorded line carries its
pattern as data, so the vector forms above are **first-record**.

Why `[:latest e k]` is exact: a replacing fact has the same entity and key
as what it replaces and a later stamp (the clock promise), so the fact with
the largest stamp at or before T in `(e, k)` cannot have been replaced by a
fact at or before T; it is the latest head of the chain as of T (P13's head
rule), found by one tail read.

**Answer row** (what a read shows for one fact):

```
{:fid [name idx] :stamp s :e e :k k :replaces fid-or-nil :mark #{...}
 :value v}                       ; opened: the value, nil for a retract
| {... :erased-at s2}            ; erased: its forget's stamp, and no value
| {... :unreadable reason}       ; open-value could not open it (phase 2)
```

Point reads also answer `{:fid fid :absent true}` for a fact id with no
admitted fact at or before the moment (never admitted, refused, or admitted
after the moment; the three are not told apart, which is what "shows nothing
admitted after it" requires).

**Read answer** (what the query returns to the exit; the exit shows `:rows`
only after the entry is acknowledged):

```
{:layer L :moment {:stamp m} :kind :point|:pattern :pattern p-or-nil
 :rows [answer-row ...]                  ; in the pattern's order, or the fids' order
 :matched [[fid stamp] ...]              ; ids and stamps of what matched (pattern); what was read (point)
 :mark :complete | :partial              ; pattern reads; [F3] no resume address
 :fingerprint hex :fp-secret :read-fp/1} ; pattern reads
| {:refused reason}                      ; a bad pattern, not indexed, opaque, not visible: nothing to record
```

**Complete or partial.** A pattern read takes a limit (default 1,000, a rig
choice). The query stops once it has seen limit + 1 matching entries; if a
limit + 1st matching entry exists, the answer holds the first `limit` and is
marked `:partial`; else `:complete`. **[F3]** The limit + 1st entry only
decides the mark: nothing of it (address, fact id, stamp, key) is returned,
because a person's or a model's reads are the crossing's exact list (ruling
3) and the line records only the shown rows. (The query stops reading once it has seen limit + 1 matches;
entries it skips because they are stamped after the moment do not count.)
**[F8]** It also stops once it has scanned a budget of 16 × (limit + 1)
entries in the range (skipped ones included; 16,016 at the default limit);
a read stopped by the budget is marked `:partial` too, so a read as of an
early moment over a large prefix does bounded work. A partial read's line records what was matched and shown, not
the whole match. These are the only reasons for `:partial` tonight; phase 2's
unreadable values are shown as unreadable rows and leave the read complete
(rig choice).

**Fingerprint.** HMAC-SHA256, under the fingerprint secret, over the UTF-8
bytes of `"softland.read-fp/1\n"` followed by `env/canonical` of the **set**
of `[fid stamp]` pairs that matched (phase 1's canonical text: sorted,
recursive, so equal sets print the same), as lowercase hex. Ids and stamps
only; never a value, never a value's digest; an erased fact that still
matches contributes its `[fid stamp]` like any other. It is one swappable
function, `rig.store.reads/fingerprint`. Its inputs and bytes are
**first-record** (PROGRESS "open": "the exact bytes the fingerprint covers";
CONCLUSION R1's rider "read-entry fingerprints cover the ids of the matched
facts, not their values", a default). Why a set, not the answer's order: the
same matched facts give the same fingerprint whatever index served them.
Consequences, derived: the fingerprint changes when a matched fact changes
(a replace adds a new `[fid stamp]` to a pattern that covers the chain) and
not when an unmatched fact changes; a `[:kv]` read re-run after a forget no
longer matches the purged entry, so its fingerprint differs, which the
sharpening names as expected.

**The fingerprint secret** (**first-record**, where it lives and how it is
made). It is derived, never stored: `HMAC-SHA256(root, "softland/read-fingerprint/1")`,
32 bytes, where `root` is the store's root secret. Tonight `root` is a
constant in `rig.store.reads`, beside phase 1's digest constant and exactly
as throwaway (the rig keeps no records). It is computed and used only inside
the module (the query topology), never in the client, so unlike phase 1's
digest secret (F11) no client holds a copy. Each pattern line names the
secret's id, `:read-fp/1`, a constant, so a rotation later is a new id and
old lines stay checkable under the old one. Why derived from a root and not
the digest's secret: the digest secret has a client copy (F11), and a read
fingerprint that anyone holding it could recompute would be a confirmation
oracle over what a person read. Where a kept store keeps `root` is not this
stage's; it belongs with phase 2's lock store or the operator's secret store,
and edition one decides.

**Reader.** The exit's caller names: `:reader` (the person, or the agent's
actor id), `:reader-kind` (`:person`, `:model`, `:tool`), `:rows?` (a tool's
signature asking for exact rows; phase 6 makes it a fact on the tool,
tonight a parameter), `:working` (the reader's working layer: where its acts
land, its session layer or its own layer), `:permission` (the reader's
permission in the working layer, a pid `[who working working]`), and
`:role` (one of the placeholders `:stood-on`, `:shown`, `:matched`,
`:passed-through`; default `:shown`), and **[F12]** `:for`, the person the
reader reads for: the reader itself for `:person`, the session's person for
`:model` and `:tool` (tonight a parameter; a kept store derives it from
the session-start fact, "owner is derivable at read time from author,
layer and permission"). Visibility is decided for `:for`, never for an
agent's or a tool's own actor id: an agent in Alice's session reading her
personal layer, or its own session layer `:alice-agent` (owned by Alice),
reads what Alice may see, and its entry's `:who` stays the agent, under its
own permission in the working layer (R7: agents narrower still).

**The read entry** (ruling 3 as written; **first-record** in every part
below). One act per exit call, in the working layer, offered as an ordinary
offer: `:who` the reader, `:layer` the working layer, `:class :by-layer`,
`:permission` the reader's pid there, `:stood-on {}`, `:because-of nil`,
`:subjects #{}`, a fresh `:offer`-scheme name (made by the exit, before the
gate). Its facts, all about one fresh entity per entry, `:read-<uuid>` from
the act's name (the read is the entity, as the thread is an entity):

- a point read: one fact per fact id read,
  `{:e ent :k :read/point :v {:layer L :moment {:stamp m} :role r
  :fid fid :stamp s-or-nil :shown :value|:erased|:unreadable|:absent}}`.
  A row names what was read and how it came out; it never carries the value.
- a pattern read: one fact,
  `{:e ent :k :read/pattern :v {:layer L :moment {:stamp m} :role r
  :pattern p :mark :complete|:partial :count n :fingerprint hex
  :fp-secret :read-fp/1 :exact [[fid stamp] ...]}}`, with `:exact` present
  for a person's or a model's read always (ruling 3: no re-run), and for a
  tool only when it asked for rows; empty pattern reads are recorded with
  `:count 0` and the fingerprint of the empty set.

The exact list uses based-on's form, fact ids with their stamps (CONCLUSION
R4, a default), so a reader's next act can carry what it read in `:stood-on`
unchanged. The entry act's own `:stood-on` is empty: the entry records a
reading; it is not based on it; its moment is inline (a first-record pick).

**[F1] The entry is stamped after its moment.** The entry is decided on the
working layer's home, which can be another task than the read layer's. With
`:stood-on {}` its stamp there can be below its moment: task 1 at ms 1,000
has given 500 stamps (clock 65,536,500, the stamp of a fact f just read),
task 3 has given none in that ms, so the entry gets 65,536,000; a read of
the store as of any T between the two then shows the entry naming
`[f 65,536,500]` and a moment after T, which breaks "shows nothing admitted
after it" for the store as a whole and orders the record of a reading
before what it read (a hybrid clock's receive rule is the opposite). So the
entry stands on its moment: `gate/stamp-for` counts, beside the carried
stood-on stamps and the replaced heads, the moment of every `:read/point`
and `:read/pattern` fact of the act, through one pure total function,
`(reads/entry-moments facts)`: the `[:v :moment :stamp]` of each such fact
when it is a long in [0, `env/max-carried-stamp`), else nothing. The moment
is at least every matched stamp (the query drops anything later), so the
entry is then after everything it names, an empty read's entry included,
at no cost in bytes. A hand-written `:read/*` fact can push a task's clock
no further than a carried stood-on stamp already can (R16). Weighed and
not taken: R4's based-on, every matched pair in `:stood-on` (phase 1's gate
already counts carried stamps): it stores a person's exact list twice,
turns a tool's short line into a long one, and leaves an empty read
unconstrained. First-record (FR14). With phase 2's sealing at the door,
`entry-moments` reads the value the gate opens at decision (phase 2 wires
where the plaintext comes from).
The fact keys `:read/point` and `:read/pattern` are store-owned key ids,
constants (**first-record**); tonight nothing stops a person writing a fact
under them by hand (see "Open questions").

## Reads

Every read of this stage is on one task, the read layer's home, and needs
more than one PState read (the clock and an index, or the answer record and
the rows), so every read is a query topology (Step 1's second question); the
exit then appends through the depot.

| read | who | method | reads on the home task |
|---|---|---|---|
| RE1 point read `[L for fids as-of]` [F12] | the exit | query topology `read-point`, leading `(|hash *layer)` | the layer's settings (1, visibility); `$$clock` STAY (1); per fid, `[(keypath L :answers name)]` (1 seek, the name's record) and, only when the record is a yes at or before the moment, `[(keypath L :log name idx)]` (1 to 2 seeks, the row) |
| RE2 pattern read `[L for pattern as-of limit]` [F12] | the exit | query topology `read-pattern`, leading `(|hash *layer)` | the layer's settings (1); `$$clock` STAY (1); a loop of doubling pages over one index, `{:allow-yield? true}` (1 seek per page, one iteration per entry, stopping at the range's end or at limit + 1 matches); no per-fact seek (the entry carries the row) |
| RE3 an entry's answer | the exit, tests | phase 1's `client/lookup` (RD1) | unchanged |
| RE4 the index entries of one fact | purge (the gate, in a forget event), tests | `[(keypath L :ix-of fid)]` | 1 seek |
| RE5 a page of a layer's acts and their rows **[F2]** | a rebuild put page (the gate, one event per page), tests | `[(keypath L :answers) (sorted-map-range-from nm {:max-amt n :inclusive? false})]` (from the start when `nm` is nil), then per yes act `[(keypath L :log name) (subselect ALL)]` | 1 seek + n iterations, then 1 seek per yes act |
| RE6 a page of one index field **[F2]** | a rebuild sweep page, tests | `[(keypath L f) (sorted-map-range-from a {:max-amt n :inclusive? false})]`, then per entry its act's record and row | 1 seek + n iterations, then 2 to 3 seeks per entry |

RE5 precisely: a page of at most 256 names' records in name order (a
vector-keyed range from an exact existing name needs no prefix range, so
the probe's finding about vector ranges does not bear on it: [probed]
`runs/probes/rig/probe/read_pages_probe.clj`, output
`runs/phase5-read-pages-probe.txt`: 23 names of the form `[layer class
scheme uuid]` in a subindexed map, walked by `sorted-map-range-from-start
4` then `sorted-map-range-from last {:max-amt 4 :inclusive? false}`, came
back in 6 pages, every name once, in the map's own order); the rows of a yes act are `[(keypath L :log name) (subselect ALL)]`, one
seek per act (the act's subindexed row vector is its own structure). A put
page therefore costs at most 256 + 1 seeks plus one iteration per row, and
stops early once 4,096 rows are gathered; it runs only at a restore or a
test, never on the read path. Tests read whole fields by `foreign-select`
outside the module.

The gate's own reads in its decision event gain nothing for an ordinary
offer: the index entries of a yes are computed from the offer and written
without reading (they are sets at fresh addresses). Phase 2's forget event
gains RE4, one seek.

## Writes

No new depot for ordinary use; one small operator depot for rebuilds.

| op | how | decided by |
|---|---|---|
| W1 a read entry | the exit's ordinary offer into the working layer through `*offers` (`client/offer-until-answered!`) | the stream gate, as any act: permission in the working layer, class, digest, retry from the record |
| W2 index entries of an admitted act | computed in `decide` for a yes and written in the same event as the log rows | the stream gate (PState ownership, PLAN-stream-store.md F12) |
| W3 purge by value id | a function phase 2's forget calls; its writes join that forget's precomputed writes in the forget's decision event | the stream gate, in phase 2's forget |
| W4 rebuild one layer's indexes **[F2]** | operator records on a new depot `*index-ops`, `(hash-by :layer)`, consumed by a second source of the gate topology: put pages `{:layer L :op :rebuild-put :after nm :acts n}` then sweep pages `{:layer L :op :rebuild-sweep :field f :after a :entries n}`, appended one at a time by the operator's loop | the gate topology on the layer's home, one bounded event per page |

Why the entry goes through the ordinary path (the brief's placement,
first-record): the entry is then a fact like any other, decided by the gate
that orders the working layer, answered by name, retried from the record,
permission-checked in the layer it lands in, indexed like any fact, and
forgettable by value id like any value when phase 2 arrives. No second write
path exists for the store to keep consistent.

## PState Design

No new PState. Four fields are added to the value of phase 1's `$$layers`
(everything keyed by a layer, on the layer's home task, owned by the gate).
The skill's merge rule decides where: the indexes share `$$layers`'s key
(the layer) and partitioner, so they are fields of its value, not PStates of
their own; and PLAN-stream-store.md F12 decides who writes them: `$$layers`
has one owner, the gate topology, so every index write happens in a gate
event, in the same atomic group as what it indexes.

### The index entry and its address

An **index entry** is a copy of one fact's row with its id and stamp, so a
pattern read needs no seek per fact:

```clojure
;; [F6] the entry is the log row, whatever fields the row has (phase 1's
;; :e :k :v :replaces :mark; phase 2 adds :sealed :lock-id :lock :digest),
;; plus its own fields; module.clj passes its row field map in, so a field
;; phase 2 adds to a row rides in every entry and phase 2's open-row> can
;; open an entry without reading the row (reads.clj never requires
;; module.clj: module -> gate -> reads would be a cycle)
(defn index-entry [row-fields]
  (fixed-keys-schema
   (merge row-fields
          {:fid       clojure.lang.PersistentVector   ; [name idx]
           :stamp     Long                            ; the act's stamp
           :erased-at Long                            ; set by a purge: the forget's stamp
           :copy      Boolean})))                     ; [F7] false: the value fields are not copied (hint :no-copy)
;; the value fields (:v tonight; phase 2's sealed value and lock fields) are
;; nil for a retract, nil once purged, and nil when :copy is false
```

(module.clj names its row's field map once, `row-fields`, uses it for
`:log`'s rows, and calls `(reads/layer-fields row-fields)`, which builds
the four fields below around `(index-entry row-fields)`.)

Addresses, built by one pure function `rig.store.reads/address`, parts joined
by U+0000 (written ␀), stamps as `(format "%016x" stamp)`, a keyword as its
printed text without the colon, a fact id as `env/canonical` of it:

- `:ix-ek`: `e ␀ k ␀ stamp ␀ fid`
- `:ix-ke`: `k ␀ e ␀ stamp ␀ fid`
- `:ix-kv`: `k ␀ len ␀ vtext ␀ stamp ␀ fid`, where `vtext` is the canonical
  text of the value (phase 1's value slot) and `len` its length in chars as
  8 hex digits, so a value's text containing U+0000 cannot make one value's
  prefix another's.

A readable keyword (the envelope's `readable-keyword?`) cannot contain
U+0000, so `e ␀` is a prefix of exactly the entries about `e` [probed: the
`e0␀` range excluded `e0a`]. The fact id at the end makes every address
unique: two facts of one act share entity, key and stamp but not index; a
crossing name shares its request's UUID but not its scheme, and the whole
name is in the text.

### The fields

```clojure
;; merged into phase 1's fixed-keys-schema for a layer's value (module.clj),
;; by one form: (fixed-keys-schema (merge phase-1-fields (reads/layer-fields row-fields))) [F6]
(defn layer-fields [row-fields]                                ; [F6] a function of the row's fields
  (let [entry (index-entry row-fields)]
    {:ix-ek (map-schema String entry {:subindex-options {:track-size? false}})
     :ix-ke (map-schema String entry {:subindex-options {:track-size? false}})
     :ix-kv (map-schema String entry {:subindex-options {:track-size? false}})
     :ix-of (map-schema clojure.lang.PersistentVector            ; fid -> its :ix-kv addresses
                        (set-schema String)
                        {:subindex-options {:track-size? false}})}))
```

Why each:

- `:ix-ek` serves `[:all]`, `[:e e]`, `[:ek e k]` and `[:latest e k]`:
  entity first, then key, then stamp, so a chain's history is one range and
  "as of T" is its end bound, and the latest fact at or before T is one tail
  read. Unbounded (every admitted fact of the layer), subindexed.
- `:ix-ke` serves `[:k k]` (the runner of phase 6 finds tools by key).
  Unbounded, subindexed.
- `:ix-kv` serves `[:kv k v]` for keys the hints mark as indexed by value.
  It holds values in its addresses, so it is the one index purged by
  deleting. Unbounded, subindexed.
- `:ix-of` maps a fact id to its `:ix-kv` addresses, so a purge by value id
  needs no value (after phase 2 seals values, the gate may no longer be able
  to rebuild an address from a value whose lock is gone). Keyed by the fact
  id vector: read only by exact address, which vector addresses serve
  [phase-1 ran: `:answers` and `:heads` are read so]. The set per fact is
  bounded by the number of value-indexed kinds (one tonight), so the set is
  not subindexed; the map is (one per value-indexed fact).
- Size tracking off everywhere: nothing counts; tracking costs a read per
  write.
- No `Object`: addresses are Strings, ids keywords and vectors, the value
  slot text, as phase 1.

### Options weighed (for the dominant read, a pattern read of about 20 facts, and for writes)

- **A, chosen: String-addressed flat maps whose entries carry the row.** A
  pattern read: 1 seek for the clock, 1 for the layer's settings, then one
  seek per page of the range and one iteration per entry (about 3 seeks and
  20 iterations, ≈ 1.6 ms by the skill's arithmetic). Writes per admitted
  fact: 2 entries, plus 2 (the `:ix-kv` entry and the `:ix-of` set) for a
  value-indexed key, all no-read `termval`s in the decision event.
- **B: entries carry only fact id and stamp; the read fetches each row from
  `:log`.** The same read: 3 seeks + 20 × (1 answer + 1 to 2 row) seeks ≈ 43
  to 63 seeks ≈ 20 to 30 ms. Writes smaller by the value's bytes. Rejected:
  per-read seeks multiplied by every read, against a one-time write cost
  (SKILL.md: never trade I/O for simplicity; write-path work is amortized).
- **C: nested subindexed maps keyed by keywords and longs,
  `{e {k {stamp {fid entry}}}}`.** No string building, and "as of T" is a
  Long range. `[:ek e k]`: 3 seeks + entries; but `[:e e]` needs a seek per
  key under the entity and `[:all]` a seek per entity and per chain (a layer
  of 10,000 facts over 2,000 chains ≈ 4,000 seeks). Rejected on seeks.
- **D: vector addresses `[e k stamp fid]`.** The obvious form. Rejected by
  the probe: no prefix range exists.
- **E: the indexes in their own PState, written by a separate microbatch
  topology from the log.** Rejected: a person's write must be visible to
  their next read with no optimism (the stream gate's reason, phase 1), so a
  fact must be in the indexes when its answer is; and a second writer of the
  layer's state splits what one event now keeps atomic.

The phase 2 plan written earlier proposed a by-stamp index (stamp → name).
This plan does not need it: `[:all]` is served by `:ix-ek`, and a read as of
T filters by the stamp every entry carries.

## Depots

- **`*offers`**, phase 1's, unchanged. Read entries enter here as ordinary
  offers (W1).
- **`*index-ops`**, new: `(declare-depot setup *index-ops (hash-by :layer))`,
  appended by the operator (tests, and later a restore). **[F2]** A record
  `{:layer L :op :rebuild-put ...}` or `{:layer L :op :rebuild-sweep ...}`
  asks the gate topology for one bounded page of a rebuild of one layer's
  indexes on its home task. Placed by the same function as the layer's
  offers, so its event runs on the task that holds the layer. Not an act and
  not recorded in the log: a rebuild changes no meaning (the rebuilt
  indexes equal what the log implies), and "re-encoding is allowed" (the
  store-level ruling on "never rewritten"). Rig choice. A kept store's
  restore replays forget facts before any rebuild (the rig constraint); the
  rebuild then sees every value a forget erased as erased, through
  open-value.
- Why not a store-placed act on `*offers` for the rebuild: it would put a
  maintenance step into the record (first-record) and through the envelope's
  closed parts, for no meaning.

## Topologies and PStates

No new topology. The gate topology (stream, phase 1) gains index writes in
its decision event and one source; the reads are two query topologies. The
skill's rule "at most one stream topology" holds, and a new topology would
split write access to `$$layers`, which F12 forbids.

### `gate` — stream (phase 1's), what this stage adds

**Why still stream**, unchanged: the index entries must be visible when the
act's answer is (a person's next read after an ack sees their write), so
they belong to the same event as the log rows.

**1. Index writes for every admitted act.** `gate/decide*` adds, for a yes
only, three precomputed write lists from one pure call,
`(reads/index-writes hints (:layer offer) nm rows stamp)`:

- `:index-put` — `[[field address entry] ...]`: an `:ix-ek` and an `:ix-ke`
  entry per fact (the row whole, F6; for a key the hints mark `:no-copy`,
  the row without its value fields and `:copy false`, F7); an `:ix-kv`
  entry per fact whose key the hints mark as
  indexed by value and whose value is not nil (a retract has no value to
  index).
- `:index-of` — `[[fid #{kv-address}] ...]` for those facts.
- `:index-del` — `[]` for an admitted act (only a purge or a rebuild
  deletes).

`hints` is `reads/seed-hints` tonight, a constant
`{:by-value #{...} :opaque #{...}}` (a rig choice; its contents are a
parameter, see "Hints"). Module.clj applies the lists after the heads
writes, in the same `<<if` for a yes, three blocks shaped like phase 1's:

```clojure
(<<atomic (ops/explode (get *d :index-put) :> [*ix *ia *ie])
          (local-transform> [(keypath *layer *ix *ia) (termval *ie)] $$layers))
(<<atomic (ops/explode (get *d :index-of) :> [*ofid *ias])
          (local-transform> [(keypath *layer :ix-of *ofid) (termval *ias)] $$layers))
(<<atomic (ops/explode (get *d :index-del) :> [*dx *da])
          (local-transform> [(keypath *layer *dx *da) NONE>] $$layers))
```

The same three blocks apply a purge's and a rebuild's lists; phase 2's
forget merges `reads/purge-writes` into its own decision's lists. The field
is data (`*ix`), one of the four keywords; `keypath` with a var navigates to
it [build checks: a fixed-keys field chosen by a var at runtime; fallback,
one block per field with a constant keypath, which phase 1's code already
uses for `:heads`].

**Idempotency, traced.** Every entry is a `termval` at an address computed
from the act (name, index, stamp, entity, key, value text), so a replay that
reaches the writes (possible only when nothing committed, phase 0 finding 2)
writes the same entries; a replay that finds the record writes nothing
(phase 1's recorded path). A purge's tombstone is a `termval` of a value
computed from the row and the forget's stamp; a delete twice is one delete.
No increment, no append.

**No throw.** `reads/index-writes` is pure and total over parsed offers:
every part it reads was normalised by `env/parse`, the address functions
format keywords, longs and canonical text, and it catches `Throwable`
around itself by returning empty lists plus a flag `:index-error`, which
`decide*` turns into the unrecorded face refusal `:gate-error` (phase 1's
existing road, F6), so a bug in indexing refuses the offer as data and
never restarts the worker. The cost of that road: a gate error refuses an
act that phase 1 would have admitted; the property test drives
`index-writes` with generated offers and asserts no error.

**1b. The entry's moment in the stamp rule [F1].** `gate/stamp-for` gains
one input, `(reads/entry-moments (:facts offer))`, concatenated with the
carried stood-on stamps and the replaced heads it already passes to
`rig.store.clock/next-stamp`. Pure and total (a value that is not an entry
line, or a moment outside [0, `env/max-carried-stamp`), gives nothing), so
it cannot throw or overflow; an act with no `:read/*` fact is stamped as in
phase 1. Replay computes the same stamp from the same offer.

**2. The rebuild source, paged [F2].** A second `source>` in the gate
topology's `<<sources` block, on `*index-ops`, `{:retry-mode :all-after}`.

Why paged. A rebuild as one event of a layer of 100,000 facts reads 100,000
act rows at a seek each (30 to 50 s) and the four fields whole. Rama's
stream event-tree timeout is 5 s; a slower event is retried while it still
runs, and a batch stalled past 30 s is force-failed and replays
(rama-check-2026-09-25/RESULTS.md, line 45, and its consequence "Gate work
per event must stay well under the stream timeout, or the event runs twice
at once"). Such a rebuild never finishes and holds the home task, every
layer on it, for as long as it retries. So every rebuild step is one
bounded event, and the operator's client loop drives the pages.

1. `(reads/index-op *raw :> *op)` — total; it accepts a put page
   `{:layer L :op :rebuild-put :after nm-or-nil :acts n}` (1 ≤ n ≤ 256), a
   sweep page `{:layer L :op :rebuild-sweep :field f :after a-or-nil
   :entries n}` (f one of the four fields, 1 ≤ n ≤ 512), and two test-only
   ops (RC8): `{:layer L :op :purge :fid fid :forget-stamp s}`, which reads
   the fact's row, its act's record and its `:ix-of` entry (RE4) and applies
   `reads/purge-writes`, exactly the call phase 2's forget will make, and
   `{:layer L :op :drop :field f :entries n}`, which deletes the first n
   entries of one field so a rebuild can be seen to restore them. Anything
   else is `{:refuse r}`, answered with `ack-return>` and nothing else.
2. **A put page**: RE5 (at most 256 records after `nm`, stopping once the
   yes acts read hold 4,096 rows); per row `open-row>` over the row and its
   act's stamp (F6); `(reads/put-page-writes hints *layer *acts :> *d)`,
   pure: every entry and `:ix-of` set the page's acts imply (a tombstone
   with its date and no `:ix-kv` entry for a row `open-row>` says erased; the
   row's entry otherwise); the three write blocks (puts only);
   `(ack-return> {:next last-nm :done? (< read n)})`. Work: at most 257
   seeks, 4,096 opens and about 10,000 `termval`s, about 0.1 to 0.3 s.
3. **A sweep page**: RE6 (at most 512 entries of field f after `a`); per
   entry its act's record `[(keypath *layer :answers nm)]` and row
   `[(keypath *layer :log nm idx)]` and `open-row>`, in a `loop<-` with
   `(yield-if-overtime)`; `(reads/sweep-page-writes hints *layer *field
   *entries *found :> *d)`, pure: the delete of every entry the log does not
   imply (no yes act under its name, another stamp, no such row, another
   entity or key, hints that route it to no index or another, another
   address or other content), and for an `:ix-kv` address its removal from
   the fact's `:ix-of` set; for `:ix-of` itself, a set that differs from
   the implied one is rewritten or deleted; the write blocks; `(ack-return>
   {:next last-address :done? (< read n) :deleted m})`. Work: at most 1,537
   seeks, about 0.5 to 0.8 s.
4. The operator's loop, `(read-exit/rebuild! store L)`, client side: put
   pages from the start until `:done?`, then sweep pages over `:ix-ek`,
   `:ix-ke`, `:ix-kv`, `:ix-of` in turn until each is `:done?`, resending a
   page on an append error (phase 0 finding 4). A layer of 100,000 facts
   takes about 400 put pages and 600 to 800 sweep pages: minutes, no event
   near the timeout.

Idempotent: a replayed or resent put page `termval`s the same computed
entries; a sweep page deletes only entries the log does not imply, and its
second run finds none. Consistent without a snapshot: each page is one
event on the home task [phase-1 ran: RQ 1], reading the log and writing in
the same atomic group. An offer admitted during a rebuild writes its own
entries in its own event; put pages never delete; a sweep page reads the
record and row in its own event, so the entries of any act admitted before
it are implied and kept. A forget during a rebuild purges in its own event;
a later put page sees `open-row>` erased and writes the same tombstone; a
later sweep sees the tombstone implied. When the loop ends, the fields hold
exactly what the log implies at that point. Reads during a rebuild see
every implied entry once the put pass has passed its act (puts only add),
and stale entries until the sweep passes them; after a test `:drop` the
dropped entries are missing until the put pass reaches them. A kept store's
restore runs the rebuild before it opens the layer to reads.

Whether a second `source>` can be added from a function in
`rig.store.reads` rather than inline in module.clj's `<<sources` block is
[build checks]; the fallback is the source inline in module.clj calling the
reads functions, which is where "one function called by one line" does not
hold (see "The one line in module.clj").

## Query Topologies

Both are declared by `(reads/declare-queries! topologies)`, one line in
module.clj's `defmodule` body [build checks: a `<<query-topology` form
inside a function called from the module body resolves `$$layers` and
`$$clock`; fallback, the two forms inline in module.clj]. Both start with
`(|hash *layer)`, a built-in partitioner on a topology input that targets
one task, so the client routes the query straight to the layer's home
(query-topologies.md, "Leading partitioner"); both end with `(|origin)` and
emit once, with no aggregator (one task, one emit).

Every step is total: the parsers refuse as data, the pure functions catch
`Throwable` and return `{:refused :read-error}`, and `open-value` never
throws. A query topology exception is not known here to be fatal to the
worker as a stream one is, but this plan does not find out by accident
[phase-1 ran for stream topologies only].

### `read-point` `[*layer *for *fids *as-of :> *answer]` [F12: `*for`, the person the read is for]

1. `(|hash *layer)`.
2. `(reads/parse-point *for *fids *as-of :> *p)` — total; refuses a
   non-vector list, more than 1,000 fact ids (a rig choice), a malformed
   fact id, or a malformed moment, as data; a refusal skips to step 7 with
   `*answer` bound to it (both branches unify on `*answer`).
3. `(local-select> [(keypath *layer :settings)] $$layers :> *settings)`;
   `(reads/visible? *settings *for :> *ok)` (**[F12]**: for the person the
   read is for, so an agent's read is not refused for its own id) — ruling 9's default:
   personal, hand and agent layers are visible to their owner; the base to
   any authenticated actor; else `{:refused :not-visible}`, **[F4]** and
   the same `:not-visible` when settings are nil, so a reader cannot tell a
   private layer that exists from one that does not (ruling 9). Nothing
   recorded for a refusal: nothing was read.
4. `(local-select> STAY $$clock :> *clock)`;
   `(reads/moment *as-of *clock :> *m)` → `min(asked, clock)`.
5. `loop<-` over the fact ids, in order, accumulating rows:
   `(local-select> [(keypath *layer :answers *nm)] $$layers :> *rec)`; when
   `*rec` is a yes stamped at or before `*m`,
   `(local-select> [(keypath *layer :log *nm *idx)] $$layers :> *row)` (phase 1's
   plan names this path for one row of the subindexed vector [build checks:
   `keypath` with an index into a subindexed vector; fallback `(nthpath *idx)`]) and
   **[F6]** `(locks/open-row> *layer *fid *row s *m :> *opened)` (phase 2's
   twin of `open-value>`, given the row and its act's stamp, so nothing is
   read twice);
   else the row `{:fid fid :absent true}`. `(yield-if-overtime)` in the
   loop body.
6. `(reads/point-answer *layer *m *rows :> *answer)` — `:matched` is
   `[fid stamp]` for the rows that were not absent.
7. `(|origin)`.

Input examples: one fact id admitted before the moment → 1 (settings) + 1
(clock) + 1 (record) + 1 to 2 (row) seeks, all meaningful. Five fact ids of
which two are absent → 2 + 5 records + 3 rows; each record read is
meaningful (it decides absent or not), no row is read for an absent one.
Zero fact ids → 2 seeks, rows empty (a point read of nothing is recorded as
an entry with no rows; see "The exit"). **Variable**: the count follows
the input; handled by `loop<-`, one record seek per fact id and one row
seek only when the record says the fact is there.

### `read-pattern` `[*layer *for *pattern *as-of *limit :> *answer]` [F12]

1. `(|hash *layer)`.
2. `(reads/parse-pattern *pattern *limit reads/seed-hints :> *pp)` — total:
   it normalises a `[:kv k v]` value with `env/normalize-value` (so the
   address matches the admitted text) and refuses a value nested deeper than
   28 levels, so the recorded line (a map holding the pattern) stays within
   the envelope's 32 (a rig choice); it returns
   `{:refused :bad-pattern | :not-indexed | :opaque}` or
   `{:ix field :from address :end address-or-nil :tail? bool :limit n}`.
3. Settings and visibility, as `read-point` step 3.
4. Clock and moment, as `read-point` step 4;
   `(reads/bounds *pp *m :> *from *end)` folds the moment into the end
   bound for `[:ek]` and `[:kv]` (end = prefix + hex(m + 1)) and into the
   tail bound for `[:latest]`.
5. The range read, by kind:
   - `[:latest e k]`: one
     `(local-select> [(keypath *layer :ix-ek) (sorted-map-range-to *end {:max-amt 1})] $$layers :> *sub)`
     [probed: the tail read returned the entry just below the bound];
     the entry counts only if its address has the prefix `e␀k␀`.
   - every other kind: a `loop<-` of pages,
     `(local-select> [(keypath *layer *ix) (sorted-map-range-from *from *page)] $$layers {:allow-yield? true} :> *sub)`
     (the bare-count form of `:max-amt`, paths.md),
     with the first page `min(16, limit + 1)` entries and each next page
     twice the last (a rig choice), each next page starting at the last address read followed
     by U+0000, which is the least String above it (addresses are unique, so
     nothing is read twice and nothing skipped), until
     an entry reaches `*end` (or the map ends), `limit + 1` entries have
     matched, or **[F8]** 16 × (limit + 1) entries have been scanned.
     `(reads/page-step ...)` is the pure step: it keeps entries
     below `*end` whose stamp is at or before `*m`, counts them and what it
     scanned, and says continue or stop.
   - **[F5]** For `[:kv k v]` only, a kept entry is a candidate, not yet a
     match: `open-row>` runs on it inside the page loop, and it counts as
     matched only when it opens to `{:value v'}` with v' equal to the
     pattern's value. An `:ix-kv` entry whose value no longer opens (a
     person forget that has not yet reached this index, phase 2) is
     neither shown nor counted nor fingerprinted, so no read confirms a
     guess at an erased value (IMPLICIT_SPEC RD3: "never a match on a
     value erased before the read").
6. `loop<-` over the kept entries (at most `limit`): an entry with
   `:erased-at` gives `{... :erased-at s}` (a purged fact shows only its
   date); **[F7]** an entry with `:copy false` first reads its row,
   `[(keypath *layer :log nm idx)]` (1 to 2 seeks; only `:read/*` facts,
   read rarely); then **[F6]** `(locks/open-row> *layer (:fid e) row (:stamp e) *m :> *opened)`
   (the entry is the row, so no row is read for a copied entry) gives
   the value, the erasure date, or unreadable (a `[:kv]` match was opened in
   step 5 and is not opened again). `(yield-if-overtime)` in the loop.
7. `(reads/pattern-answer *layer *m *pp *rows *more? :> *answer)` — the
   rows in address order, `:matched` the `[fid stamp]` pairs, `:mark`
   `:partial` when a limit + 1st match was seen or the scan budget ran out,
   else `:complete` (**[F3]** no resume address: the entry past the limit
   is never returned); `:fingerprint` from `reads/fingerprint` over the set
   of matched pairs, under the fingerprint secret derived in the module;
   the secret never leaves this function.
8. `(|origin)`.

Input examples (a layer of 10,000 facts over 2,000 chains; entity `e`
with 12 facts over 3 keys; key `:note` on 4,000 facts):

- `[:latest e :note]` as of now → settings 1 + clock 1 + 1 tail seek, 1
  iteration. Fixed.
- `[:ek e :note]` as of now, 4 facts → 3 seeks + one page of 16 iterated,
  of which 4 match and the 5th ends the range. Meaningful.
- `[:e e]` → 3 seeks + 13 iterations (one page), 12 meaningful.
- `[:k :note]` with limit 1,000 → 2 + 6 page seeks = 8 (pages of 16, 32,
  64, 128, 256 and 512 entries, 1,008 in all, the last cut at the 1,001st
  match) + about 1,001 iterations; marked partial [F3: no resume address]. Variable,
  handled by the page loop: a small match reads one small page, a large one
  grows its pages, and no read is issued past the end or past limit + 1.
- `[:kv :note "x"]` with two matches → 3 seeks + one page, 2 meaningful.
- `[:e unknown]` → 3 seeks + one page whose first entry is past the prefix:
  an empty read, recorded (ruling 3: empty pattern reads included); its one
  range seek is the cost of knowing it is empty.
- `[:kv :mention ...]` when `:mention` is not hinted → refused at step 2,
  settings and clock not read.

Why a page loop and not one range read: `sorted-map-range` over a large
prefix materialises the whole range [docs: pstate-schema.md, the range
navigators select a submap], so `[:all]` on a big layer would load it
whole; `sorted-map-range-from` with `:max-amt` bounds each read, but a
single read of `limit + 1` would iterate up to 1,001 entries past a
three-fact entity. Doubling pages cost one extra seek per doubling for a
large read and nothing extra for a small one.

## The exit (`rig.store.read-exit`, client side in the rig)

CONCLUSION R5 (a default) puts the exit in a read gateway on the server. In
the rig the store's clients run in the test's process, so the exit is plain
Clojure over the foreign API beside `rig.store.client`, and a kept store
moves the same function behind the server. Its handles are taken once, `(read-exit/connect cluster)`: phase 1's
`client/connect` map plus `foreign-query` handles for `read-point` and
`read-pattern` and the `*index-ops` depot (client.clj is not changed). One
function:

```
(read! store {:reader :alice :for :alice :reader-kind :person|:model|:tool :rows? false
              :working :alice-hand :permission [:alice :alice-hand :alice-hand]
              :layer :alice :read [:point [fid ...]] | [:pattern p]
              :as-of nil :limit 1000 :role :shown :entry-name nil})
→ {:rows [...] :moment {:stamp m} :mark .. :fingerprint .. :entry name :entry-stamp s}
| {:refused reason}                 ; nothing shown
| {:refused reason :entry name}     ; the entry was refused: nothing shown
```

Steps, in this order, and nothing returned to the caller before step 5:

1. **Check the call** (total, data): the reader, kind, role, working layer,
   permission and read form are well formed; else `{:refused :bad-read}`.
2. **Query**: `foreign-invoke-query` of `read-point` or `read-pattern` on
   the read layer. A `{:refused r}` answer returns `{:refused r}`: nothing
   was read, nothing is recorded.
3. **Build the entry**: `client/build` with the entry's facts from
   `(reads/entry-facts answer spec)`: rows for a point read, one line for a
   pattern read, the exact list when the reader is a person or a model, or
   a tool with `:rows? true`. The name is `:entry-name` when given (tests
   arm the gate's crash hook on it), else fresh. The built map is kept for
   resends (phase 1: the digest covers `:claimed-when`).
4. **Append the entry and wait for its answer**:
   `client/offer-until-answered!`, which resends the same map under the same
   name after an error and takes the answer from the record (phase 0
   finding 4). If it gives up, the exit throws; nothing was shown.
5. **Answer**: on `:yes`, return the rows with the entry's name and stamp;
   on `:no`, return `{:refused reason :entry name}` and no rows.

Test hooks (R3, global atoms, the in-process cluster only). The exit makes
the entry's name before step 1 (or takes `:entry-name`), so every hook names
the read by it: `(inject/point! :exit-after-query nm)` between steps 2 and 3,
`(inject/point! :exit-after-entry nm)` between steps 4 and 5, and
`(inject/point! :exit-shown nm)` just before the return. An armed point
throws, which here stands for the exit's process dying at that point.

What this guarantees, derived: every answer shown has an entry the gate
acknowledged, because the only road to a return with rows passes step 4's
`:yes`. The converse is not promised: an entry can be recorded for an answer
never shown (a crash between 4 and 5), which records a reading that did not
reach the reader; ruling 3's exposure dimension is about reads that
happened, and a recorded read that was not shown over-records rather than
under-records. Named in "Open questions" for Sid.

Agent session reads use the same exit with `:working :alice-agent`: eager,
recorded in the agent session layer (the brief; CONCLUSION R5 reopens
ruling 3's "may default to none"; this is a default, not a ruling).

A reader's next act that stands on what it read carries `:stood-on` from
the answer's `:matched` pairs, which are exactly R4's form; the exit does
not write it.

## `open-value` (phase 2's, consumed here)

The one function every shown value passes through, in `rig.store.locks`
(phase 2's lock namespace). **[F6]** Phase 2's plan, validated in parallel
(PLAN-locks-and-forgetting.md at cbd2bb16 on rig-plan-locks, lines 545-562
and 599), fixes the names, arities and returns, and where the two plans
differ phase 2's wins; this stage calls what it defines:

- `(locks/open-value> *layer *fid *T :> *r)`, which reads the act's stamp,
  the row, the erasure ledger, the lock row and the wrap's persons itself,
  and its twin `(locks/open-row> *layer *fid *row *stamp *T :> *r)` (the
  argument order is phase 2's; the build takes it from phase 2's code),
  given a row and its act's stamp already read, so no row is read twice.
  This stage calls `open-row>` everywhere: an index entry is the row plus
  its own three fields (F6's schema), so it passes as the row; a point read
  and a rebuild page pass the row they read. Called on the layer's home
  task, inside a query topology or a gate rebuild page.
- Returns exactly one of `{:value v :stamp s}` (v nil for a retract),
  `{:erased-at s}` (the ledger's date, else the person-forget date),
  `{:unreadable reason}` (`:no-such-fact`, `:after-moment`,
  `:does-not-open`). Never throws. This stage filters by stamp before it
  opens, so `:after-moment` is never the reason a row it shows is
  unreadable.
- It may read PStates on the same task (phase 2's lock rows sit with their
  values, ruling 7), so it can be a `deframafn` whose reads the call site
  does not see [docs: dataflow.md, a `deframafn` may `local-select>`]; it
  must not repartition (a suspend on the path to its emit is not allowed in
  a `deframafn`, and a partitioner would move the query off the home).
- Tonight's body passes values through:
  `{:value (env/decode-value (:v row)) :stamp stamp}`, with a failure to
  decode caught and returned as `{:unreadable :does-not-open}`.

The merge. If phase 2's build has merged when this stage builds,
`rig.store.locks` exists and this stage only calls it. If not, this stage's
build writes `src/rig/store/locks.clj` with `open-value>` and `open-row>`
under phase 2's names, arities and return shapes and nothing else, and the
merge keeps phase 2's file whole **[F6]**. That is the one place tonight's
parallel builds can collide on a file; the resolution is fixed here so the
merge is mechanical.

## Hints (index kinds take hints as parameters)

`hints` is a map passed to every function that decides what is indexed:
`{:by-value #{fact-key ...} :opaque #{fact-key ...} :no-copy #{fact-key ...}}`.
Tonight it is one
constant, `reads/seed-hints`, `{:by-value #{:note} :opaque #{} :no-copy
#{:read/point :read/pattern}}` for the
model's world (`:note` holds a plain value; `:mention` names people and is
matched by key). **[F7]** A key in `:no-copy` gets id-index entries without
its value fields (`:copy false`): the store's read lines are written on
every read (a 1,000-pair exact list is about 80 KB) and read rarely (the
session close act, audits), so copying them into `:ix-ek` and `:ix-ke`
would add about 160 KB of writes per large read to save 1 to 2 seeks per
entry on the rare read that shows them. A key in both `:by-value` and `:opaque` is opaque (ruling 6: no matching, no
index on its values, shown as opaque; the opaque showing itself needs
phase 6's grammar and is not built). Phase 6 replaces the constant with the
key's grammar facts. Changing the hints for an existing layer needs a
rebuild of its indexes (the rebuild takes the same hints), because entries
were written under the old ones; tonight's hints never change while a
module runs. A rig choice.

## Purge and rebuild (the two tools a forget and a restore call)

**Purge by value id**, `(reads/purge-writes fid row fact-stamp kv-addresses forget-stamp)`,
pure and total, returning write lists for the three blocks:
- `:index-put`: the fact's `:ix-ek` and `:ix-ke` entries as tombstones
  (the value fields nil, F6: `:v`, and phase 2's `:sealed`, `:lock`,
  `:digest`, since a digest is derived from the value; `:erased-at forget-stamp`), at addresses computed from the row's
  entity and key, the act's stamp and the fact id: no value is needed.
- `:index-del`: every `:ix-kv` address in `kv-addresses`, and `[:ix-of fid]`
  (the delete block's `keypath` takes the fact id as the address in that
  field, so no fourth block is needed).

Its inputs are what phase 2's forget event reads anyway (the row and the
act's record, to find the lock) plus RE4, one seek for `kv-addresses`.
Phase 2's forget merges these lists into its decision's writes, in the same
event, so the forget and the purge are one atomic group (phase 1: F12).
After a purge no index holds the value or anything derived from it; the id,
entity, key and stamp remain, so a read as of any moment that matched the
fact shows it with its erasure date, as the model's `read-as-of` does
(`:erased-at`), and a `[:kv]` read no longer matches it.

A person's forget erases every value it closes at once; it reaches the
indexes either by a purge per erased value, or by a rebuild of each layer it
touches (the sweep pages see each such value erased through `open-row>`
and delete its `:ix-kv` entry). **[F5, F10]** Phase 2's plan (cbd2bb16,
line 649) purges on a value forget and names nothing for a person forget,
so until one of the two is wired a person-forgotten value's text stays in
`:ix-kv` addresses: reads never match it (F5), but the store still holds
it, which the rig constraint ("so forget reaches it") does not allow. This
is phase 2's obligation, for builder A to place.

**Rebuild one layer from its log [F2]**, by pages, two pure functions:
`(reads/put-page-writes hints layer acts)`, where `acts` is one page of yes
acts with their stamps, rows and each row's `open-row>` result, returns the
entries and `:ix-of` sets they imply, as `:index-put` and `:index-of`; and
`(reads/sweep-page-writes hints layer field entries found)`, where
`entries` is one page of a field and `found` each entry's act record, row
and open result, returns as `:index-del` (and `:index-of` rewrites) every
entry of the page the log does not imply. Both are applied by the gate's
rebuild pages (Topologies, 2), and `(reads/implied hints layer acts)`, the
same entries for a whole log, is what T10 compares against. After the
operator's loop ends, the four fields hold exactly the entries the log
implies under `hints`.

## The one line in module.clj, and where Rama does not allow one

Where it can be one form:
- the schema: `(fixed-keys-schema (merge phase-1-fields (reads/layer-fields row-fields)))` [F6];
- the query topologies: `(reads/declare-queries! topologies)` [build checks];
- the depot: `(reads/declare-depots! setup)`, declaring `*index-ops`
  [build checks: a `declare-depot` from a function called in the module body].

Where it cannot: the gate event's index writes and the rebuild source are
dataflow inside the gate topology's `<<sources` block, because a PState is
written only by the topology that declares it, and this plan keeps
`$$layers` on the gate (F12). So module.clj gets the three write blocks
(about six lines) after the heads writes, and the second `source>` inline
unless a second `<<sources` call on the same topology works [build checks].
`gate/decide*` gets one call, `(reads/index-writes ...)`, merged into its
precomputed writes, and **[F1]** `gate/stamp-for` one more input,
`(reads/entry-moments (:facts offer))`. Those two lines are all this stage
changes in gate.clj.

## Interfaces, as this stage builds against them tonight

**[F10] Shared files, and what parallel builds must keep.** gate.clj: the
two call sites above, nothing else; phase 2's revised gate also changes
`decide*` (sealing, lease consumption), so the merge keeps both, and the
index writes take the rows as phase 2 writes them (the entry is the row,
F6). module.clj: the schema merge (`reads/layer-fields`, with the row's
field map `row-fields` exposed for F6), the three index write blocks after
the heads writes in the yes branch, the `*index-ops` source, the two
install lines. Phase 2 adds its own fields and install lines to the same
two files; neither plan removes or reorders the other's lines.

- **`rig.store.clock`** (tonight's clock build, merged at 0bc0cd7f): stamps
  are positive longs, strictly increasing per task, below 2^62 (the
  envelope's `max-carried-stamp`; hex16 needs no sign), and the
  home task's last stamp is readable with `(local-select> STAY $$clock)`,
  written for a yes and a no alike (module.clj). Nothing else is used; F1
  feeds `next-stamp` one more seq of stamps and changes nothing in it.
- **`rig.store.locks`** (phase 2): `open-value>` and `open-row>` as above
  (F6); its value forget calls `reads/purge-writes` and reads RE4 (its plan,
  line 649, has the call site); **its person forget must reach `:ix-kv`**,
  by a purge per erased value or the paged rebuild of each affected layer,
  and its plan does not yet say which (F5, for builder A). Its plan also
  builds its own `read-as-of` query and a `:by-stamp` index, which serve the
  same read as this stage's `[:all]`; which one stays is builder A's call,
  and this stage does not rely on either being absent.
- **Phase 3's micro gate** (its own namespace): nothing tonight. Its
  settled-frontier id (its M23, a microbatch id, a Long) fills the moment's
  `{:frontier id}` slot later.
- **The reader's kind, `:for` and the working layer.** Tonight parameters
  of the exit's caller. A kept
  store's read gateway (R5) takes them from the actor (the session-start fact
  and phase 6's tool signature), never from the call, since a model called a
  `:tool` would otherwise leave a short line (ruling 3: a model's reads are
  the exact list, always), a reader naming another person as `:for` would
  read that person's private layers (F12), and an entry sent to a working
  layer others can see (a layer where someone granted the reader write
  permission) would show them the ids of what the reader read.
- **What this stage exports**, in `rig.store.reads`: `layer-fields`,
  `seed-hints`, `declare-queries!`, `declare-depots!`, `address`,
  `index-writes`, `purge-writes`, `put-page-writes`, `sweep-page-writes`,
  `implied`, `entry-moments`, `index-op`,
  `parse-pattern`, `parse-point`, `visible?`, `moment`, `fingerprint`,
  `entry-facts`; in `rig.store.read-exit`, `read!` and `rebuild!`. `rig.store.reads`
  requires `rig.store.envelope` and `rig.store.locks`, never
  `rig.store.client` (the client requires the module, which requires the
  gate, which requires reads: a cycle otherwise).

## Later stages and where they plug in

- **Standing reads** (CONCLUSION R6, a default; not built). One entry opened
  with pattern, role and moment; a stamped line per delivery that shows
  something new, with delivered ids (exact for a person or a model, a
  fingerprint for a tool); closed at unsubscribe or session close with a
  fingerprint over everything delivered and a complete-or-partial mark.
  Plugs in: `read-pattern` gains an `:after` bound (every entry carries its
  stamp, so "new since s" is a filter on the same ranges); `read-exit`
  gains `subscribe!`, `deliver!`, `close!`; the entry gains keys for the
  opening, delivery and closing lines, which are first-record then.
- **Shared-layer reads** (after phase 3). A shared layer is placed by
  entity, so its pattern reads fan out over its entity partitions and read
  through phase 3's settled frontier (R5 of the rig: never half a batch);
  its indexes are the micro gate's to write in its own PStates. Plugs in: a
  third query topology for shared layers, the exit choosing by the layer's
  kind, and the moment's `{:frontier id}` part. The entry format is
  unchanged otherwise.
- **The agent session's close act** (after phase 2's forget). Keeping or
  dropping the session's read entries: the entries are found by `[:k
  :read/pattern]` and `[:k :read/point]` in the agent layer; dropping them
  is phase 2's forget of each entry act's values, which purges them here.
- **Grammars and tools** (phase 6). Hints from a key's grammar facts; a
  tool's `:rows?` and its reader kind from its signature facts; "shown as
  opaque" from the grammar.
- **Sealed values** (phase 2). The index entry's `:v` then holds what the
  row holds (sealed), so the id indexes carry no plaintext; an `:ix-kv`
  address still needs the plaintext at admission, which the gate has through
  `open-value` in the decision event (phase 2's to wire).

## Partitioning efficiency

**Optimal placement first.** The dominant read is a pattern read of one
one-owner layer. Its data is one layer's facts, which phase 1 already places
on one task, `f(layer) = hash(layer) mod N` (P2); the read wants every entry
it scans on that task and nowhere else, so `f` for the index fields is the
same `f`, implemented by keeping them inside `$$layers`'s value, and the
query is routed by its leading `(|hash *layer)`. No other placement reads
fewer tasks than one. The entry's write lands on the working layer's home,
`hash(working) mod N`, one task too.

Categories (frequencies are assumptions for weighting, not measurements;
the count's tool and the src-inland renderers are expected to be dominated
by small entity and latest reads): latest 0.25 (3 seeks, 1 iteration); a
small entity or chain read of about 12 facts 0.50 (3 seeks, 16 iterations,
one page); a key read at the 1,000 limit 0.10 (settings 1 + clock 1 + 6 pages
= 8 seeks, 1,001 iterations); an empty read 0.05 (3 seeks, 1 iteration);
**[F11]** a point read of one fact id 0.10 (settings 1 + clock 1 + record 1
+ row 1 to 2 = 4.5 seeks, 0 iterations). The weights sum to 1.
Seeks count every task touched; each read touches one.

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| latest `[:latest e k]` | 0.25 | 3 | 1 |
| small `[:e e]` / `[:ek e k]` | 0.50 | 3 | 16 |
| point, one fact id [F11] | 0.10 | 4.5 | 0 |
| large `[:k k]`, limit 1,000 | 0.10 | 8 | 1,001 |
| empty | 0.05 | 3 | 1 |
Weighted seeks = 3.65   |   Weighted iterator reads = 108.4

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| latest | 0.25 | 3 | 1 |
| small | 0.50 | 3 | 16 |
| point [F11] | 0.10 | 4.5 | 0 |
| large | 0.10 | 8 | 1,001 |
| empty | 0.05 | 3 | 1 |
Weighted seeks = 3.65   |   Weighted iterator reads = 108.4

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| latest | 0.25 | 3 | 1 |
| small | 0.50 | 3 | 16 |
| point [F11] | 0.10 | 4.5 | 0 |
| large | 0.10 | 8 | 1,001 |
| empty | 0.05 | 3 | 1 |
Weighted seeks = 3.65   |   Weighted iterator reads = 108.4

Flat in N, because a one-owner layer is on one task. The exit adds the
entry's decision on the working layer's task: phase 1's 4 reads (record,
settings, clock, permission row) and the writes (record, log rows, 2 index
entries per entry fact), also flat in N. Tonight `open-value` adds nothing;
phase 2's will add its lock reads per shown value, on the same task.

The hot-layer case, named: a layer whose reads are heavy loads one task,
the same limit phase 1's placement by layer has for writes; ruling 2's
re-class to by-entity is the ruled answer, and its reads are phase 3's.

## Design Decisions

- **Subindexing.** `:ix-ek`, `:ix-ke`, `:ix-kv` and `:ix-of` are subindexed
  maps (unbounded per layer); an `:ix-of` value is a plain set (bounded by
  the number of value-indexed kinds). Size tracking off.
- **Colocation.** The indexes live in the layer's value on its home task,
  written in the event that admits what they index; every read of one layer
  is one task. The entry is written on the working layer's home through the
  depot, placed by the depot's `hash-by :layer`.
- **Which patterns.** STARTER-next calls this build "the read exit the count
  needs". The count (Sid's phase 6) has "a minimal runner finds tools by
  matching", which reads facts by key and by key and value; the reference
  tool of 13 September shows a passage or function at a revision "or shows
  that it is stale", which reads an entity's facts and a chain's latest
  fact; the model's reads are a whole layer as of a moment. The six forms
  are those needs and no more.
- **Addresses are Strings** because vector addresses have no prefix ranges
  in Rama 1.6.0 [probed].
- **Entries carry the row** so a pattern read costs no seek per fact.
- **As of T is a stamp filter** on every entry (and a range bound where the
  address orders by stamp), with the moment read before any page, so a read
  that yields between pages stays exact: anything admitted meanwhile has a
  stamp above the moment.
- **Purge writes tombstones in the id indexes and deletes the value index**,
  so time travel still shows the fact with only its erasure date.
- **The exit is outside the module**: a query topology may append to a
  depot, but it cannot wait for the gate's decision on that append (an
  append's ack from inside a topology is the depot's, not the stream
  topology's [docs: query-topologies.md, depot appends from queries;
  core-concepts ack levels]), and "nothing shown before its entry is
  acknowledged" needs the gate's answer.

## State primitive selection

- The four index fields of `$$layers` (PState): durable, co-located with the
  log, written in the admitting event. Per source event: 2 entries per
  fact, plus 2 writes per value-indexed fact.
- No TaskGlobal. The fingerprint secret is a derived constant (a pure HMAC
  of a constant), computed once per namespace load, not state.
- No external system.

## Resource usage analysis

### Disk usage (PStates), per fact
- The log row (phase 1): about 50 bytes + the value's canonical text `|v|`.
- `:ix-ek` and `:ix-ke`: an address of about 100 bytes (entity and key
  names, 16 hex, the fact id's canonical text of about 70 characters) and an
  entry of about 100 bytes + `|v|`, so about 400 bytes + 2|v| for both.
- A value-indexed fact adds an `:ix-kv` entry (about 200 bytes + 2|v|: the
  value is in the address and in the entry) and an `:ix-of` set (about 70 +
  110 + |v|).
- So indexes multiply a plain fact's bytes by about three, and a
  value-indexed fact's by about five. A read entry line with an exact list of
  1,000 pairs is about 80 KB, stored once in its row; **[F7]** its two
  id-index entries carry no copy (`:no-copy`), about 200 bytes each, so
  about 80.4 KB per such read rather than 240 KB. At a model's call rate in
  an agent session it is still the number to watch (phase 7 can measure it
  on the finished stage).

### Memory usage (TaskGlobals)
None.

### Minimization
- `:ix-ke` could carry only id and stamp, making `[:k k]` pay a row seek per
  fact; not taken, because phase 6's runner finds tools by key.
- Entries of the store's own read keys (`:read/*`) carry no value copy
  (**[F7]**, hint `:no-copy`), so a line's exact list is stored once; taken
  tonight on the arithmetic in "Hints" (a rig choice; no record changes).
- The fact id's canonical text in every address could be shortened to the
  name's scheme, UUID and index (the layer is the map's own key); not taken
  tonight, since addresses are rebuildable and the full text is unambiguous.

## First-record picks (placeholders for edition one; none is a ruling)

- FR1. The moment's form: `{:stamp s}` inline for a one-owner layer, a
  `{:frontier id}` part in its place for a shared layer later, nothing else.
- FR2. What moment a read records: `min(asked, the home task's clock)`.
- FR3. Where an entry lives: an ordinary act in the reader's working layer
  (its session layer or its own), through `*offers`, named by the exit with a
  fresh `:offer`-scheme name, `:who` the reader, under the reader's
  permission in that layer.
- FR4. The entry's parts: `:stood-on {}`, `:because-of nil`, `:subjects #{}`,
  `:session nil`.
- FR5. The entry's entity: one fresh `:read-<uuid>` per entry, from its name.
- FR6. The entry's fact keys: `:read/point` and `:read/pattern`,
  store-owned constants.
- FR7. A point row's value:
  `{:layer :moment :role :fid :stamp :shown}`, `:shown` one of `:value`,
  `:erased`, `:unreadable`, `:absent`; never the value.
- FR8. A pattern line's value:
  `{:layer :moment :role :pattern :mark :count :fingerprint :fp-secret :exact}`,
  `:exact` a vector of `[fid stamp]` in the answer's order, present for a
  person or a model always and for a tool that asks.
- FR9. The recorded pattern forms: `[:all]`, `[:e e]`, `[:ek e k]`,
  `[:latest e k]`, `[:k k]`, `[:kv k v]`. **[F9]** This makes the read of a
  cell (layer, entity, key: the chain's head as of the moment) a pattern
  read, `[:latest e k]`, recorded as one line with its exact list and
  fingerprint, and makes a point read a read by fact id, recorded as rows.
  IMPLICIT_SPEC RD2 reads it the other way: its point read is "a layer, an
  entity, a key", returning the chain's head, recorded as rows. Ruling 3
  says "rows for point reads" without saying what a point is. For Sid: is
  a cell read a point read (rows) or a pattern read (a line)? Either is a
  change of `reads/entry-facts` and FR7/FR8 only; nothing else moves.
- FR10. The roles as keywords: `:stood-on`, `:shown`, `:matched`,
  `:passed-through`.
- FR11. The fingerprint's bytes: HMAC-SHA256 over
  `"softland.read-fp/1\n"` + the canonical text of the set of `[fid stamp]`
  pairs matched, as hex; ids and stamps only.
- FR12. The fingerprint secret: derived as `HMAC-SHA256(root,
  "softland/read-fingerprint/1")`, named `:read-fp/1` in every line; `root` a
  constant in code tonight; kept only in the module, never in a client.
- FR13. A fact's id is phase 1's `[name idx]`; no new id is made.
- FR14. **[F1]** A read entry is stamped after its moment: the gate's stamp
  rule counts each `:read/*` fact's `[:moment :stamp]` as a stamp the act
  stood on, so an entry is never earlier than what it read (ruling 4's
  promise applied to the read the entry records). The alternative, R4's
  based-on carrying every matched pair in `:stood-on`, was weighed in "The
  read entry" and not taken.

## Rig choices (change without touching a record)

- RC1. Pattern parsing, the refusals before a read (`:bad-pattern`,
  `:not-indexed`, `:opaque`, `:not-visible`, `:bad-read`), none recorded;
  **[F4]** a layer that does not exist is `:not-visible` on the read path
  (the gate's `:no-such-layer` for an entry's working layer, the reader's
  own, is phase 1's and unchanged).
- RC2. Limits: 1,000 rows per pattern read by default, a limit outside 1 to
  10,000 refused `:bad-pattern`, 1,000 fact ids per point read.
- RC3. Pages of 16 entries, doubling.
- RC4. String addresses joined by U+0000, stamps as 16 hex digits, a value
  length-prefixed in `:ix-kv`.
- RC5. Index entries carry the row.
- RC6. Visibility by ruling 9's default as a constant: personal, hand and
  agent layers to their owner, the base to anyone; seed policy facts later.
- RC7. Hints as a constant, `{:by-value #{:note} :opaque #{} :no-copy
  #{:read/point :read/pattern}}` (**[F7]**).
- RC8. **[F2]** Rebuild by pages on `*index-ops`: put pages of at most 256
  acts (and 4,096 rows), then sweep pages of at most 512 entries per field,
  each one bounded event, driven one at a time by the operator's loop
  `read-exit/rebuild!`; two test-only operator ops on the same depot,
  `:purge` (runs `purge-writes` for one fact id with a given forget stamp,
  standing in for phase 2's forget, which becomes its only caller) and
  `:drop` (deletes up to n entries of one field, so a rebuild can be seen to
  restore them).
- RC9. Tombstones in the id indexes after a purge; deletion in the value
  index.
- RC10. Partial by the limit or **[F8]** by the scan budget, 16 × (limit
  + 1) entries; unreadable rows leave a read complete. **[F3]** No resume
  address is returned.
- RC11. A retract is not indexed by value.
- RC12. The exit in the client's process in the rig, with hooks through
  `rig.store.inject`.

## Namespaces and tests

Code: `src/rig/store/reads.clj` (pure functions and the module fragments),
`src/rig/store/read_exit.clj` (the exit), the stub `src/rig/store/locks.clj`
only if phase 2's has not merged, and the edits named in "The one line in
module.clj" to `module.clj` and `gate.clj`. Tests: `test/rig/store/reads_test.clj`
(pure: parsers, addresses, index, purge and rebuild writes, fingerprint,
entry facts, property tests), `test/rig/store/read_exit_test.clj` (the
in-process cluster, 4 tasks, under the machine-wide lock), and
`test/rig/store/read_model_test.clj` (against the model). Run from the rig
folder with
`flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:test rig.store.reads-test rig.store.read-exit-test rig.store.read-model-test`,
and phase 1's suite again, since the gate changed:
`rig.smoke-test rig.store.envelope-test rig.store.stream-gate-test`.

The tests the brief names, each with its setup and what it asserts:

- **T1. Answers equal the model's for the same history.** The model has no
  pattern reads; it has `read-as-of` (every admitted fact at or before T, in
  every layer, with its value or `:erased-at`). The test writes histories in
  the model's own op vocabulary restricted to what the rig has tonight:
  `:offer` on `:alice`, `:alice-hand` and `:alice-agent` (with replaces and
  retracts), `:retry`, `:reuse`, `[:read :now]` and `[:read [:at i]]`; plays
  each through `formal.model/run` under `baseline` and through the rig as the
  same offers in the same order; maps the model's act names to the rig's
  names by position; and for every model read compares, per layer, the
  model's facts filtered to that layer with the rig's `[:all]` read as of the
  matching moment (the rig stamp of the model's chosen fact), and the
  model's facts filtered by each pattern (`[:e]`, `[:ek]`, `[:latest]`,
  `[:k]`, `[:kv :note ..]`) with the rig's pattern reads, and each fact by id
  with a point read. Compared: which facts, their order by stamp, value,
  replaces. Not compared: stamp values (the clocks differ) and the rig's
  layer-making facts (`:kind`, `:owner`, `:class`, `:lock-grain`, P10), which
  the model keeps as settings, not facts; both exclusions are stated in the
  test. T1 calls the two query topologies directly, not the exit, because
  the exit's own entries are facts the model does not have and would change
  the layers being compared; the exit is tested by T2 to T5, T11, T15 and
  T16. scenarios.clj's fixed histories use forgets, promotions and the group
  layer, which the rig does not have until phases 2 to 4; the test lists
  each fixed history and says why it is not replayed yet, and every
  difference found is reported, not filtered.
- **T2. The entry is acknowledged before the answer returns.** Watch the
  entry's name (`inject/watch!`); after `read!` returns rows, the recorded
  points hold `[:after-writes nm]` (the gate committed the entry) before
  `[:exit-shown nm]`, and `client/lookup` of the entry is `:yes`.
- **T3. A refused entry hides the answer.** The operator revokes
  `[:alice :alice-hand :alice-hand]`; `read!` with that permission returns
  `{:refused :permission-revoked :entry nm}` and no `:rows`; with a working
  layer never made, `{:refused :no-such-layer ...}`.
- **T4. An empty pattern read is recorded.** `[:e :nobody]` returns no
  rows; the entry's line has `:count 0`, `:mark :complete`, and the
  fingerprint of the empty set, equal to `(reads/fingerprint #{})`.
- **T5. Complete and partial marks.** Five facts on one entity: limit 5 is
  complete; limit 3 is partial with three rows, the first three in address
  order, and **[F3]** nothing of the fourth anywhere in `read!`'s return
  (no resume address; its fact id and stamp appear in no field); limit 4 is
  partial; the entry line carries each mark and count. **[F8]** A layer of
  500 facts on `:e1` stamped after s0 and one on `:e0` at s0: `[:all]` as of
  s0 with limit 10 scans at most 176 entries and is marked `:partial`.
- **T6. The fingerprint changes with a matched fact, not with an unmatched
  one.** `[:ek :e0 :note]` gives fp1; replacing its head and reading again
  gives fp2 ≠ fp1; offering a fact on `:e1`, and replacing an unmatched
  chain, and reading again gives fp2 both times; reading as of the first
  moment again gives fp1.
- **T7. The fingerprint's inputs hold no value.** Pure: `fingerprint` takes
  only `[fid stamp]` pairs (its signature cannot take a value); the bytes it
  hashes, exposed by `reads/fingerprint-bytes`, contain none of a set of
  distinctive value texts offered for the matched facts; two layers with the
  same ids and stamps and different values give the same fingerprint (built
  from entries directly).
- **T8. As-of reads show nothing after the moment.** f1 admitted at s1; f2
  replacing it at s2. As of s1: `[:ek]` shows f1 only, `[:latest]` gives f1,
  a point read of f2 is `:absent`, `[:all]` has nothing stamped after s1. As
  of now: both, `[:latest]` gives f2. A read asked for a moment past the
  clock records the clock (FR2). The erasure exception: the test-only
  `:purge` of f1 with forget stamp s3 > s2, then a read as of s1 shows f1
  with `:erased-at s3` and no value, and nothing else from after s1.
- **T9. A purge by value id removes it from every index.** A value-indexed
  fact f, then `:purge`: no `:ix-kv` address matches f, no `:ix-of` entry
  for f, the `:ix-ek` and `:ix-ke` entries are tombstones with `:v nil`;
  `[:kv :note v]` no longer matches f; `[:e e]` shows f with its erasure date
  only; a whole scan of the four fields finds the value's canonical text in
  no address and no entry. (What "removes it from every index" is read as:
  the value and everything derived from it; the id, entity, key and stamp
  stay, so time travel can show "erased on this date". See "Open
  questions".)
- **T10. A rebuild from the log reproduces the indexes exactly.** After a
  history with offers, replaces, retracts, control acts and read entries:
  snapshot the four fields whole; `(reads/implied hints L acts)` over the
  log read by `foreign-select` gives exactly the snapshot; **[F2]**
  `read-exit/rebuild!` with pages of 2 acts and 3 entries (so a small
  history spans many pages) deletes nothing and leaves the snapshot; `:drop`
  ops that empty the four fields, then `rebuild!`, leave the fields equal
  to the snapshot, entry for entry; a stale entry written into `:ix-ek` by a
  test-only put (an address no act implies) is deleted by the sweep; an
  offer admitted between two pages keeps its entries; a page resent after
  a forced append error changes nothing. And purge agrees with rebuild: for
  one row whose `open-row>` result is
  `{:erased-at s}`, `put-page-writes` gives the same tombstone and the same
  absent `:ix-kv` entry as `purge-writes` with forget stamp s (pure). A
  rebuild after the test-only `:purge` is not asserted equal, because
  tonight no forget fact or destroyed lock tells the log the value is
  erased; with phase 2 that case joins this test.
- **T11. A crash between the query and the entry, and between the entry and
  the answer, leaves a recorded entry for every answer shown.**
  (a) `:exit-after-query` armed: `read!` throws, returns nothing, and the
  entry's name has no answer (`:no-answer`) and no line in the working
  layer's `[:k :read/pattern]`. (b) `:exit-after-entry` armed: `read!`
  throws, returns no rows, and the entry is recorded (`:yes`). (c) the
  gate's `:before-writes` armed on the entry's name: the worker restarts,
  the exit resends, `read!` returns rows, the entry is recorded once (one
  answer under its name, one line). (d) the gate's `:after-writes` armed:
  the resend is answered from the record, `read!` returns rows. Across all
  four and every other test, a global check: every `read!` that returned
  rows has its entry's answer `:yes`.

Tests the design adds:

- **T12. Index writes replay to the same entries.** `:after-writes` armed on
  an ordinary offer: after the replay, the four fields equal those of the
  same history run without the crash.
- **T13. Visibility.** Bob reading Alice's personal layer is refused
  `:not-visible` and nothing is recorded; a read of the base (made one-owner,
  owned by the root actor, R8 as a default) by Bob is answered. **[F12]** A
  model reader `:agent-a` with `:for :alice` reads `:alice` and
  `:alice-agent` and is answered, its entry in `:alice-agent` with `:who
  :agent-a`; the same reader with `:for :bob` reading `:alice` is refused
  `:not-visible`.
- **T14. No throw.** Property tests drive `parse-pattern`, `parse-point`,
  `index-writes`, `purge-writes`, `put-page-writes`, `sweep-page-writes`, `entry-moments`, `entry-facts` and
  `fingerprint` with generated garbage and generated offers, and assert
  no exception; the in-process cluster gets malformed patterns, fact ids,
  moments and `*index-ops` records and answers each as data with no worker
  restart (phase 1's no-restart check).
- **T15. Agent session reads are recorded in the agent session layer**, with
  a model reader's line carrying the exact list.
- **T16. Readers' kinds.** A person's and a model's lines carry the exact
  list; a tool's line does not; a tool with `:rows? true` gets it. A point
  read of three fact ids records three rows.
- **T17. Phase 1 still passes** with the gate's additions (its suite, as
  above).
- **T18. [F1] An entry is stamped after its moment.** Pure: `stamp-for` for
  an act with one `:read/pattern` fact whose moment is above the task's
  clock and wall gives moment + 1; a `:read/*` value that is not an entry
  line, or a moment at `max-carried-stamp`, adds nothing. In-process:
  choose a read layer and a working layer with different home tasks (the
  test computes both homes from the depot's partitioner); push the read
  layer's task clock far ahead with an offer there carrying a stood-on stamp
  of clock + 2^40; read that layer as of now through `read!`; the entry's
  stamp is above the recorded moment and above every matched stamp.
- **T19. [F4, F5, F7]** Bob reading a layer never made gets `:not-visible`,
  the same answer as for Alice's private layer. A `[:kv :note v]` candidate
  whose `open-row>` is stubbed to `{:erased-at s}` (a test double of phase
  2's person forget) is not shown, not counted and not in the fingerprint.
  A `:read/pattern` line's id-index entries carry `:copy false` and no
  value, and `[:k :read/pattern]` still shows the line's value, read from
  its row.

## Spec coverage, self-check (the validator does the full trace)

(The validator's trace, PLAN_VALIDATION-read-exit.md, found twelve
failures under these PASS lines, F1 to F12; the lines below are the plan
author's and stand as corrected by those fixes.)

- **"Point reads and pattern reads on a layer's home task, as of a
  moment."** RE1 and RE2, routed by `(|hash *layer)`. Fault: no in-memory
  state; a worker restart during a query fails that query (the exit then
  shows nothing, nothing is recorded, and the reader reads again). Retry of
  a query: read-only, same answer at the same moment. Race: a write on the
  same task during a yield between pages is stamped above the moment and
  filtered out. PASS.
- **"Shows nothing admitted after it, except an erasure, which shows only
  its date."** Every entry is filtered by its stamp against the moment; a
  tombstone or an `open-value` erasure shows only `:erased-at`. Race: a
  purge between pages shows either the value (read before) or the date (read
  after); both are linearizations of one task's order. PASS.
- **"One exit: query, append the read entry, then answer."** The exit's
  step order; the only return with rows follows the gate's `:yes`. Fault: a
  crash at any point before the `:yes` shows nothing; the gate's own crashes
  are phase 1's replay road (the name, the record). Race: two readers' entries
  are separate names; two entries into one working layer are ordered by its
  gate. PASS.
- **"If the entry is refused, the answer is not shown."** Step 5. PASS.
- **"Agent session reads are recorded there too."** The same exit with the
  agent layer as the working layer. PASS.
- **The entry's content** (rows; one line per pattern read with pattern,
  moment, role, fingerprint, mark; empty reads; exact lists by reader kind).
  FR7, FR8, T4, T15, T16. PASS.
- **"A one-owner layer's moment is its stamp, inline; able to carry a
  shared layer's settled-frontier id in its place, and nothing more."** FR1.
  PASS.
- **"The fingerprint is an HMAC under a store key over the ids and stamps
  of what matched, never values."** FR11, FR12, T7. PASS.
- **"Values are opened through one function, open-value."** Every shown
  value passes through it; tombstones show a date without opening, which
  opens nothing. PASS.
- **"Indexes: only what pattern reads need; hints as parameters; purgeable
  by value id; rebuildable from the log; with tests."** Three indexes and a
  reverse map, one per pattern family; `purge-writes`, `put-page-writes` and `sweep-page-writes` (paged, F2);
  T9, T10. PASS, with the reading of "removes it from every index" surfaced.
- **"Mind the costs: seeks, subindexing, yielding on large reads."**
  Costed per input; every unbounded map subindexed; `:allow-yield? true` on
  every page read and `yield-if-overtime` in every per-fact loop of a
  query; **[F2]** the rebuild runs in bounded pages, each well under the
  stream timeout, and **[F8]** a query's scan is bounded by its budget.
  PASS.
- **"A gate never throws on an offer; every refusal is data."** Index
  writes total, with `:gate-error` as phase 1's road; the index-ops source
  total. PASS.

## Design difficulty log

1. **The index address.** I began with vector addresses `[e k stamp fid]`,
   because phase 1's plan names a prefix range over `[e k]` for stage 5.
   Before writing a schema I probed it: vector addresses have no prefix
   ranges in Rama 1.6.0. That forced Strings (or nested maps, which cost a
   seek per key under an entity). Not close once probed.
2. **Rows in the entries or seeks per fact.** Genuinely contested: copying
   the row triples a plain fact's bytes, and the read entries of a model
   reader are the largest facts. The skill's cost rule settled it (per-read
   seeks against a one-time write), with the cost of `:read/*` copies named
   and a no-copy hint left as a rig choice for after measurement.
3. **What a purge removes.** The brief's test says a purge "removes it from
   every index". Deleting the fact from the id indexes too would make time
   travel lose the fact, while the sharpening says a read shows "erased on
   this date" and the model's `read-as-of` shows erased facts with
   `:erased-at`. I chose tombstones in the id indexes and deletion in the
   value index, and surfaced the reading rather than silently narrowing the
   test.
4. **The rebuild's atomicity.** A yielding rebuild could race a forget and
   put a purged value back; a non-yielding one blocks the task for a whole
   layer. For the rig, correctness wins; the paged form is named as the kept
   store's need. **[F2, the validator's]** Reversed: a one-event rebuild of a
   large layer cannot finish inside Rama's stream timeout (it is retried
   while it runs and force-failed at 30 s), so it is not correct either. The
   paged form keeps correctness without a snapshot: each page reads the log
   and writes in one event, puts only add, and a sweep deletes only what the
   log read in its own event does not imply, so a forget or an offer between
   pages cannot put a purged value back or lose a new entry.
5. **Where the exit runs.** A query topology can append to a depot, which
   looked like a way to keep the exit inside the module; but it cannot wait
   for the gate's decision, and "nothing shown before its entry is
   acknowledged" needs that decision. Forced once traced.
6. **The recorded moment.** Recording the asked moment is simpler; recording
   `min(asked, clock)` makes every recorded one-owner read final. I took the
   latter as a first-record pick.
7. **The partial mark's meaning.** Only the limit is a reason tonight; I
   weighed marking a read with unreadable rows partial and did not, because
   the rows say so themselves and phase 2 decides what unreadable means.
8. **Two round trips per read.** The exit costs a query and an acked
   append. A one-trip design exists: a read-request source on the gate
   topology reads on the read layer's home, hops to the working layer's home,
   decides the entry there and returns the rows by `ack-return>`, so the
   rows reach the client only after the entry commits. I did not take it:
   the brief places the entry on the ordinary client offer path, and a
   request retried after an ack error would have to return rows as of the
   entry's recorded moment rather than re-read, which re-derives phase 1's
   name, digest and retry semantics for a second kind of event. It halves
   the exit's round trips; it is the first thing to measure if phase 7 finds
   the exit slow, and it would change where an entry is made, which is
   first-record.

## Open questions

- **For Sid** (touch records): all of FR1 to FR14; whether an entry recorded
  for an answer that was never shown (a crash between entry and answer) is
  acceptable over-recording, or the exit must mark such an entry; whether a
  person may write facts under the store's `:read/*` keys by hand (tonight
  the gate does not stop it; a reserved-key refusal is a gate change and
  first-record; **[F1]** since the gate now reads a `:read/*` fact's moment
  for the stamp, a hand-written line can push a task's clock as far as a
  carried stood-on stamp can, no further, and can forge an exposure record
  in a layer its writer may write); FR14, the entry stamped after its
  moment (**[F1]**: the validator found that without it an entry decided on
  another task can be stamped below its moment and below what it read, and
  a read as of a T between them shows the entry naming facts admitted after
  T); FR9's reading of a cell read as a pattern read (**[F9]**); whether a
  `[:kv k v]` line may hold v in its recorded pattern (the reader typed it,
  so it is the reader's utterance, but a forget of the matched value does
  not reach it); whether a value opened by the query and forgotten before
  the entry's acknowledgement may still be shown (tonight it is: the read
  happened and is recorded, IMPLICIT_SPEC RD3, as the promotion ruling's
  read-out); what "removes it from every index" should mean (T9's
  reading).
- **For phase 2** (black box to this plan): `open-value>`'s and
  `open-row>`'s bodies and their lock reads; **[F5, F10]** a person's forget
  must reach `:ix-kv`, by a purge per value or the paged rebuild, and phase
  2's plan names neither yet (for builder A); how the gate gets plaintext
  for an `:ix-kv` address and for `entry-moments` once values are sealed at
  the door; which of phase 2's `read-as-of` and this stage's `[:all]` stays.
- **For the build** [build checks]: a runtime field in `keypath` inside a
  fixed-keys value; `<<query-topology` and `declare-depot` from functions
  called in the module body; a second `<<sources` call on one topology;
  whether a query topology exception is fatal to the worker.
- **For a kept store**: reads during a rebuild (tonight they see stale
  entries until the sweep, and dropped ones are missing until the put pass;
  a restore rebuilds before it opens a layer to reads); RocksDB keeps a
  deleted `:ix-kv` address in its files until compaction, like the depot's
  copy of a value (default 1); where the root secret lives;
  whether the id indexes should shorten the fact id text in addresses.
- **Found and reported, not changed**: PLAN-stream-store.md's note that a
  chain's latest head is a range over `[e k]` addresses in `:heads` is
  wrong for Rama 1.6.0 (vector addresses have no prefix range, probed); no
  phase 1 code relies on it.
