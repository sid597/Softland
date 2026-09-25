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
- **open-value**, the one function every shown value passes through; a
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
still grow. Rig choice for the clamp; the moment's form is first-record.

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
| `[:kv k v]` | every fact with key `k` whose value was `v` when admitted | `:ix-kv`, prefix `k␀len(v)v␀`, bound `stamp ≤ T` | stamp, fact id |

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
 :mark :complete | :partial              ; pattern reads
 :resume address-or-nil                  ; the next address, when partial
 :fingerprint hex :fp-secret :read-fp/1} ; pattern reads
| {:refused reason}                      ; a bad pattern, not indexed, opaque, no such layer: nothing to record
```

**Complete or partial.** A pattern read takes a limit (default 1,000, a rig
choice). The query reads at most limit + 1 entries; if a limit + 1st matching
entry exists, the answer holds the first `limit`, is marked `:partial`, and
carries `:resume`, the address of the first entry not shown; else
`:complete`. A partial read's line records what was matched and shown, not
the whole match. This is the only reason for `:partial` tonight; phase 2's
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
`:passed-through`; default `:shown`).

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
| RE1 point read `[L fids as-of]` | the exit | query topology `read-point`, leading `(|hash *layer)` | `$$clock` STAY (1); per fid, `[(keypath L :answers name)]` (1 seek, the name's record) and `[(keypath L :log name idx)]` (1 to 2 seeks, the row) |
| RE2 pattern read `[L pattern as-of limit]` | the exit | query topology `read-pattern`, leading `(|hash *layer)` | `$$clock` STAY (1); one range read of at most limit + 1 entries of one index, `{:allow-yield? true}` (1 seek + up to limit + 1 iterations); no per-fact seek (the entry carries the row) |
| RE3 an entry's answer | the exit, tests | phase 1's `client/lookup` (RD1) | unchanged |
| RE4 the index entries of one fact | purge (the gate, in a forget event), tests | `[(keypath L :ix-of fid)]` | 1 seek |
| RE5 a layer's acts and rows, whole | rebuild (the gate, in a rebuild event), tests | `[(keypath L :answers) ALL]`, `[(keypath L :log) ALL ALL]` hmm see below | 2 seeks + one iteration per act and per row |
| RE6 a layer's index entries, whole | rebuild, tests | `[(keypath L :ix-ek) ALL]` and likewise `:ix-ke`, `:ix-kv`, `:ix-of` | 4 seeks + one iteration per entry |

RE5 precisely: `[(keypath L :answers) ALL]` gives every name's record in name
order; the rows of a yes act are `[(keypath L :log name) ALL]`, which is one
seek per act (the act's subindexed row vector is its own structure). A
rebuild therefore costs 2 seeks per yes act plus one iteration per row; it
runs only at a restore or a test, never on the read path.

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
| W4 rebuild one layer's indexes | an operator record `{:rebuild layer}` on a new depot `*index-ops` (hash-by `:rebuild`... see Depots) consumed by the gate topology | the gate topology on the layer's home, one event |

Why the entry goes through the ordinary path (the brief's placement,
first-record): the entry is then a fact like any other, decided by the gate
that orders the working layer, answered by name, retried from the record,
permission-checked in the layer it lands in, indexed like any fact, and
forgettable by value id like any value when phase 2 arrives. No second write
path exists for the store to keep consistent.
