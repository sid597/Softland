# Plan — stage 1, "stream-store"

<!-- Phase 1 Step 5 of the rama skill, for the first entry of DECOMPOSITION.json.
Written 25 September 2026 by a fresh-context session (Claude Fable 5.1, effort
max). Plan only: no module or test code. Sources: SPEC.md and everything it
names; IMPLICIT_SPEC.md (cited as I-xx, OPn, RDn, En, On); DECOMPOSITION.json
("stream-store" scope); RIG.md phase 0; the rama skill's references. Where a
line here summarises a source, the source wins. Vocabulary: "key" is a fact's
key; "lock" is an encryption key. -->

## Scope of this stage, in one paragraph

The stream store end to end for one-owner layers placed by layer: the offer
depot and its partitioning, the stream gate, and per task the log, the answers
by name, the task's stamp, the layer's settings and its permissions as facts.
Every decision branch that rig choice R7 lists, deciding as `model.clj`'s
`baseline` does (`stream-step`, `refusal`, `stamp-for`, `lookup`,
`mis-tagged?`, `digest-of`, `tag-of`), and seeding the first facts the model's
`seed-permissions` seeds on the one-owner side. Values are stored as offered;
locks are stage 2's; the micro gate is stage 3's; promotion is stage 4's;
reads as of a moment are stage 5's. Later stages are black boxes: this plan
says what state it leaves and where, not how they will use it.

The rulings this plan implements are I-G1 to I-G9, I-O1, I-O2, I-O4 (the
stream side), I-P1, I-P3, I-P4, I-P5, and the operations OP1, OP2, OP4, OP5,
OP6, OP7 and OP8 on their stream side. The open items that fall to this stage
are O1, O3 (stream side), O4, O5 and O6; each pick is under "Rig choices
proposed", never as a ruling.

## The shapes every section below uses

These are the data the gate and the offerer share. They are stated once here
and referred to by name.

**Ids.** Layer ids, entity ids, fact keys and person ids are keywords in the
rig (P3). A fact id is `[name idx]`: the act's name and the fact's position in
the act. "Value id" means fact id throughout.

**Name** (I-G7, `tag-of`, `mis-tagged?`): a vector
`[layer class scheme id]` (P4), made by the offerer before the gate.

- `layer`: the layer the act goes into.
- `class`: `:by-layer` or `:by-entity`, the class the name was made for; `nil`
  for an act the store places itself, exactly as `tag-of` reads it: every
  fact's key is one of the placed keys `#{:class :lock-grain :forget
  :crossed}`.
- `scheme`: `:offer` for every offerer (people and the operator); `:crossing`
  and `:landing` are reserved to the store (stage 4 derives them from a
  request's name, keeping the request's `id`; an offerer using them is refused
  on its face).
- `id`: a UUID7 from `ops/random-uuid7`, client-side, so a resend carries the
  same name (unique-ids.md).

The tag of a name is its first two elements. The tag of an offer is
`[(:layer offer) (if store-placed? nil (:class offer))]`. The gate compares the
two, reading nothing (I-G5's first check).

**Offer** (the envelope, I-G8, P5): one act under one name.

```
{:version     1                       ; the version marker; only 1 is known
 :name        [layer class scheme id]
 :who         person | :operator | :store
 :layer       layer                   ; the act's layer (I-G6 sharpening: layer on the act)
 :class       :by-layer | :by-entity  ; what the offerer expects the layer's class fact to say
 :permission  [who layer in] | nil    ; nil only when :who is :operator or :store
 :session     layer | nil             ; carried, stored on the act, not checked in this stage
 :stood-on    {fid stamp, ...}        ; what the act stood on, with the stamps as the offerer read them (P9)
 :because-of  name | nil              ; the act that caused this one
 :claimed-when long | nil             ; the offerer's own clock, carried, not checked
 :subjects    #{person ...}           ; what the tool named; the gate unions the owner in (P15)
 :facts       [fact ...]}             ; one or more
```

A fact (the positional core, plus the mark):

```
{:e entity  :k key  :v value | nil  :replaces fid | nil  :mark #{:die-with-any :own-row}
 :layer layer}                       ; optional; when present it must be the act's (the :per-act reading)
```

`:v` is EDN data (maps, vectors, sets, keywords, strings, numbers, booleans,
nil, uuids); `:v nil` on a fact that replaces another is a retract (P13).
Act id and index are not carried: they are the name and the position. A fact
may name its layer; the gate refuses the act when it is not the act's
(`:fact-outside-the-acts-layer`, the sharpening "layer belongs on the act").
Any key outside these two sets, a missing required part, a version other than
1, a name that is not a well-formed vector, or a value outside EDN data is
refused as malformed, unrecorded (P7). Tools add facts, never parts.

**Permission id** (P8): `[who layer in]`, the model's `[who layer where]` with
`where` resolved to the layer the permission's fact lives in: the model's
`[:alice :group :own]` is `[:alice :group :group]`; `[:alice :group :session]`
is `[:alice :group :alice-hand]`. The gate's cover check (`who`, `layer`
against the offer's) and its home check (`in` against the offer's layer) read
nothing, as the model's do; the exists and revoked checks read this layer's
permission index.

**Digest** (I-G7, `digest-of`, D8, P6): HMAC-SHA256 under the rig's
fingerprint secret over the canonical EDN of the offer minus its name (map
entries sorted by their printed key, set elements sorted, recursively, so two
`=` offers print the same), kept as a hex string. Derived by the gate at
decision time and by the client for a lookup; the offer does not carry it.

**Answer record** (the act record; `decide`, I-G4, P7): kept under the name
on the layer's home task, for a yes and for a recorded no alike.

```
{:answer :yes | :no   :reason keyword | nil   :stamp long   :digest string
 :who :class :permission :session :stood-on :because-of :claimed-when   ; the act's parts, once
 :subjects #{person ...}}                                               ; owner ∪ carried
```

**Log entry**: under the name, the act's facts as a vector of rows
`{:e :k :v :replaces :mark}` in offer order, with `:v` as canonical EDN text
(P12). Written only for a yes. A row is addressed by its value id `[name idx]`.

**Stamp** (I-O2, `stamp-for`, P9): a long,
`max(wall, clock + 1, max(stood-on stamps) + 1, max(replaced facts' stamps) + 1)`,
with `wall` from `TopologyUtils/currentTimeMillis` and `clock` this task's
last stamp. Strictly increasing per task; one per decided offer, yes or no;
all facts of an act share it.

## Reads

Every read in this stage is one point read on one partition, routed by the
layer id as the first key, so every read is a `foreign-select-one` and no
query topology exists (Step 1's first question answers yes for each).

| read | who | path on `$$layers` | seeks | note |
|---|---|---|---|---|
| RD1 the answer by name plus layer | the offerer, tests | `[(keypath layer :answers name)]` | 2 (the layer's entry, then the name) | `layer` is the name's first element; the client compares the record's digest with the digest of what it sent: equal → the answer; different → name taken (`lookup`); nil → no answer yet, resend (I-G3). An answer for a store-placed act (tag class nil) in this stage is on the home too; the second read I-G4 describes (the name row) exists only once the micro store does (stage 3). |
| RD10 a layer's settings | the gate (locally), tests | `[(keypath layer :settings)]` | 1 | `{:kind :owner :class :grain}`, projected from the layer's setting facts |
| RD10 a permission's state | the gate (locally), tests | `[(keypath layer :permissions pid)]` | 2 | `{:granted fid :revoked fid-or-nil}` |
| the admitted facts of an act | tests, stage 5 | `[(keypath layer :log name)]` | 2 | the vector of rows; the value slot decoded by the client |
| whether a fact heads its chain | the gate (locally), stage 5 | `[(keypath layer :heads [e k fid])]` | 2 | its stamp when unreplaced, nil otherwise; the latest head of `(e k)` (O5, P13) is the max stamp over the keys with prefix `[e k]` — a range scan for stage 5, not read in this stage |
| the task's stamp | the gate (locally), tests | `STAY` on `$$clock` | 1 | one long per task |

The gate's own reads per offer, all local to the home task and inside the
decision event: the layer's entry (settings come with it), the name's answer
record, the cited permission's row, and one heads row per replacing fact. The
counts are in "Partitioning efficiency".

Point reads and pattern reads as of a moment (RD2, RD4) are stage 5's; what
this stage leaves for them is under "What later stages consume".

## Writes

One depot, `*offers`, every write an offer as shaped above (app-design: a
single append encodes all side-effects of an event). By operation:

| op | offer | facts | decided by |
|---|---|---|---|
| OP1 offer an act | `:who` the person, `:permission` a pid in this layer, `:class` the layer's class | ordinary facts, any number ≥ 1 | the gate, on the layer's home |
| OP2 resend | the same offer map, byte for byte (the digest covers `:claimed-when` and `:stood-on`, so the client resends what it built, not a rebuilt offer) | — | answered from the record; nothing written |
| OP4 replace / retract | as OP1 | a fact with `:replaces fid`; a retract is such a fact with `:v nil` (P13); its undo is a fact replacing the retract with the value again | the gate; a stale or doubled replace refuses the whole act |
| OP5 make a layer | `:who :operator`, `:permission nil`, `:class` = what its own class fact declares | `{:e layer :k :kind :v :personal|:hand|:agent}`, `{:e layer :k :owner :v person}`, `{:e layer :k :class :v :by-layer}`, `{:e layer :k :lock-grain :v :per-value}` (P10); any of them may be in one act with the layer's grants | the gate; the class check uses the act's own class fact when the layer has none (P10) |
| OP6 grant | `:who :operator` | `{:e (perm-entity who) :k :permission :v {:id pid}}` in the layer `in` of the pid | the gate; projected into `:permissions` |
| OP6 revoke | `:who :operator`, `:stood-on {grant-fid stamp}` | `{:e (perm-entity who) :k :revoke :v {:permission pid}}` in the permission's layer | the gate; refused `:stale-revoke` when the pid is not granted and unrevoked in this layer (P8) |
| OP7 re-class | `:who :operator`, `:class` the current class | `{:e layer :k :class :v :by-entity}`; tag class nil (store-placed) | the gate; from then on an offer tagged `:by-layer` into this layer is refused `:class-mismatch`; the move to the micro gate is stage 3's |
| OP8 grain switch | `:who` the owner, `:permission` the owner's own | `{:e layer :k :lock-grain :v :per-act|:per-value}`; tag class nil | the gate; projected into `:settings`; its effect on locks is stage 2's |
| seed | the operator's OP5 and OP6 acts for the one-owner layers and the permissions that live in them | as `seed-permissions`: for the model's world, layers `:alice` (personal), `:alice-hand` (hand), `:alice-agent` (agent); own permissions `[:alice L L]` for those three layers; session permissions `[:alice L :alice-hand]` for L in `:alice :alice-agent :group :base`, all facts in `:alice-hand` | the gate, before any history |

The gate's writes on a yes, all on the home task in the one decision event
(I-G6): the answer record (`termval`), the log entry (`termval`), for each fact
a heads write (`[e k fid]` set to the stamp; `[e k replaced]` deleted with
`NONE>`), for each setting fact one `:settings` field (`termval`), for a
permission fact its row (`termval`), for a revoke its row's `:revoked`
(`termval`), and `$$clock` (`termval`). On a recorded no: the answer record
and `$$clock`. Every one is a set of a value computed before any write, keyed
by name, fact id, pid or layer, so a replay that reaches the writes (only
possible when nothing was committed) produces the same rows, and a replay that
finds the record writes nothing (I-G2).

## PState Design

Two PStates, both owned by the stream gate.

### `$$layers` — everything keyed by a layer, on the layer's home task

The candidates, costed for the dominant read (the gate's decision: answer
record, settings, permission, heads on one task) and for write volume:

- **Option A, one PState per piece**: `$$answers {layer {name ...}}`,
  `$$log {layer {name ...}}`, `$$settings {layer ...}`, `$$permissions
  {layer {pid ...}}`, `$$heads {layer {[e k fid] ...}}`. Every one shares the
  key type (layer) and the partitioner (hash of the layer). Reads per offer:
  each PState's top-level entry for the layer is its own seek: 4 layer
  entries + 3 element seeks = 7 seeks. Five partitions' memory overhead per
  task for the same key.
- **Option B, one PState keyed by layer**, value a fixed-keys record with the
  settings as a plain field and four subindexed sub-maps: answers by name,
  log by name, heads by `[e k fid]`, permissions by pid. Reads per offer: the
  layer's entry once (settings ride in it; block-cached after the first offer
  into a hot layer) + 3 element seeks = 4 seeks. One partition per task.
- **Option C, answers and log as one map** (the act record holds its facts):
  one fewer write per act, but every answer read on the hot path (every
  resend, every replay, every lookup) reads the act's facts too; a large act
  ("acts of any size") makes its own answer expensive to re-read. Rejected on
  throughput of the replay path, which RQ 2 puts on every record since the
  checkpoint.

Chosen: B (pstate-schema.md's rule that data sharing a key and a partitioner
is one PState with a field per piece). Schema:

```clojure
(declare-pstate s $$layers
  {clojure.lang.Keyword                                     ; layer id
   (fixed-keys-schema
     {:settings    (fixed-keys-schema {:kind  clojure.lang.Keyword    ; :personal :hand :agent (shared kinds: stage 3)
                                       :owner clojure.lang.Keyword
                                       :class clojure.lang.Keyword    ; :by-layer :by-entity
                                       :grain clojure.lang.Keyword})  ; :per-value :per-act
      :answers     (map-schema clojure.lang.PersistentVector          ; name
                               (fixed-keys-schema
                                 {:answer       clojure.lang.Keyword
                                  :reason       clojure.lang.Keyword
                                  :stamp        Long
                                  :digest       String
                                  :who          clojure.lang.Keyword
                                  :class        clojure.lang.Keyword
                                  :permission   clojure.lang.PersistentVector
                                  :session      clojure.lang.Keyword
                                  :stood-on     (map-schema clojure.lang.PersistentVector Long)
                                  :because-of   clojure.lang.PersistentVector
                                  :claimed-when Long
                                  :subjects     (set-schema clojure.lang.Keyword)})
                               {:subindex-options {:track-size? false}})
      :log         (map-schema clojure.lang.PersistentVector          ; name
                               (vector-schema
                                 (fixed-keys-schema {:e        clojure.lang.Keyword
                                                     :k        clojure.lang.Keyword
                                                     :v        String              ; canonical EDN; nil for a retract
                                                     :replaces clojure.lang.PersistentVector
                                                     :mark     (set-schema clojure.lang.Keyword)}))
                               {:subindex-options {:track-size? false}})
      :heads       (map-schema clojure.lang.PersistentVector Long     ; [e k fid] -> the fact's stamp, while unreplaced
                               {:subindex-options {:track-size? false}})
      :permissions (map-schema clojure.lang.PersistentVector          ; pid [who layer in]
                               (fixed-keys-schema {:granted clojure.lang.PersistentVector
                                                   :revoked clojure.lang.PersistentVector})
                               {:subindex-options {:track-size? false}})})})
```

Why each part is shaped so:

- `:answers` keyed by name: the ruled read is by name plus layer (I-G4), a
  point read. Unbounded (one per name, never expired: D4), so subindexed. A
  refused offer's record keeps the act's parts too, so a refused request is
  still found by what it was because-of (E1 N3 rows).
- `:log` keyed by name, value the act's rows as one vector: an act is admitted
  whole and read whole; one write per act instead of one per fact (M1 counts
  index writes per act); a row is addressed by `[name idx]` as `(keypath layer
  :log name)` then `nth`. The inner vector is not subindexed: it is one act,
  and an act's size is the offerer's, read and written as a unit ("acts of any
  size" means the unit is the act). The alternative, rows keyed by `[name
  idx]` in one subindexed map, costs one write per fact and gives nothing this
  stage or the stated needs of later stages read by fact alone.
- `:heads` keyed by `[e k fid]`: the stale-replace check is one point read
  (`[e k r]` present means r is unreplaced, in this layer, on this entity and
  key: `replaceable?` in one seek), and admission is two no-read writes per
  replacing fact (`NONE>` on `[e k r]`, `termval` on `[e k fid]`). It holds
  ids and stamps, no value, so a forget never has to reach it (I-L5).
  Unbounded (one per unreplaced fact), subindexed.
- `:permissions` keyed by pid: exists and revoked are one point read; a hand
  session holds a person's session permissions for every layer, unbounded, so
  subindexed.
- `:settings` as a plain field: four keywords, read with the layer's entry at
  no extra seek.
- Size tracking off on all four: nothing queries a count; tracking costs a
  read per write.
- No `Object`: ids are keywords (P3), names and fact ids and pids are
  `PersistentVector` (the envelope parser rebuilds every vector with `vec` so a
  `subvec` never reaches a write), values are canonical EDN text (P12). The
  parser guarantees the exact classes, because a schema violation would throw
  inside the topology (I-G1); see "Topologies", no-throw.

### `$$clock` — the task's stamp

```clojure
(declare-pstate s $$clock Long {:initial-value 0})
```

One long per task (I-G9, ruling 4 read as "the partition"), read with `STAY`,
written with a root `termval` (valid for a class-reference schema). A separate
PState because its key structure is none: it is not per layer. Obvious: the
only alternative is a sentinel key inside `$$layers`, which would make a
per-task value look like a layer.

### What is not a PState

No index over values beyond `:heads` (ids and stamps only). No name row (the
micro store's, stage 3). No lock store (stage 2; see "What later stages
consume" for the slot it can take). No TaskGlobal: nothing here is derived
cache; every read is a point read on durable state.

## Depots

- **`*offers`** — `(declare-depot setup *offers (hash-by :layer))`. Client
  appends only (people, the operator, and the seed). Every stream-store write
  is one record of this depot: an offer as shaped above. Partitioned by the
  act's layer, so a record starts on the layer's home task, `hash(layer) mod
  N`, which is also where `$$layers` keeps that layer (the built-in `hash-by`
  and the PState's default key partitioner agree; depot-design.md
  "Colocation"). One depot, not one per operation: every operation is an
  offer, the operations on one layer are order-dependent (a replace and the
  head it names, a revoke and the writes it governs, a re-class and the
  offers after it), and one depot keyed by layer is the layer's order
  (OP1 "Concurrency", OP6 "a revocation and the writes it governs are
  ordered by one gate"). Retry mode `:all-after` (P11): the stream is
  order-sensitive per layer; since every decided offer is answered from its
  record on replay, replaying "all after" costs reads, not double writes.
  `:start-from :beginning`? Not needed: the module is launched once per rig
  run before any append; the default `:end` at first deploy sees every record
  appended after launch.
- The partitioner is `hash-by :layer`, a keyword lookup, so no client-side
  throw for any record: `(:layer x)` is nil for a non-map, and nil hashes to
  some task, where the gate refuses the record as malformed (I-G1's client
  side, depot-design.md "Depot partitioners run on the appending client").
- No internal depot in this stage. The landing handoff to the micro store
  (stage 4 sends it; stage 3 declares the micro depot) is described under
  "What later stages consume".
- The stream store's own placed home is never stored: `f(layer) = hash(layer)
  mod N` is computed on the client by the depot partitioner and on the server
  by the PState's key partitioner (P2). A stored placement table cannot serve
  the depot partitioner (a pure function of the record), so it would add a
  read per offer and a second mechanism for the same `f`; rejected on that
  total cost, not on complexity.

## Topologies and PStates

One topology in this stage, the stream gate, named `"gate"`. It owns
`$$layers` and `$$clock`. Stage 3 adds the micro gate as a microbatch
topology in the same module (P1) with its own depot and PStates; this module
will then have exactly one stream topology and one microbatch topology.

### `gate` — stream

**Why stream** (both reasons of artifact-plan.md's rule apply, and ruling 1
names the type): (a) a person's own write must be visible to their next read
with no optimism (I-O6, OP1 latency: single-digit to low tens of
milliseconds); (b) the offerer takes the answer from the depot append itself
(`ack-return>`), and the append with `:ack` must not return before the
decision is visible, so a resend or a lookup after an error reads the record
(I-G3, RQ 4). Microbatch could give neither: at least 300 ms, and an ack that
says nothing about the decision.

**Every concern in it needs stream.** The topology does exactly one thing per
record: decide the offer on its home task and return the answer. Settings and
permission projection, heads maintenance and the log write are parts of that
one decision (they must be visible when the answer is), not separate
concerns; none can move to a microbatch without breaking I-G6's "whole or not
at all" against the answer.

**The event, on the home task, with no partitioner** (so the whole decision
is one atomic group, RQ 1; every read sees this task's committed and
uncommitted state; nothing hops):

1. `(source> *offers {:retry-mode :all-after} :> *raw)` — bind the whole
   record; no destructuring at the source, so a non-map cannot throw.
2. `(envelope/parse *raw :> *p)` — pure, total: returns
   `{:ok offer}` with every part validated and every class normalised
   (`vec` on vectors, keywords checked, longs checked, EDN whitelist walked on
   values), or `{:refuse reason}` for a malformed envelope, unknown part,
   unknown version, bad name, reserved scheme, empty act, a name tagged for
   the other gate (`:wrong-gate`, tag class `:by-entity`), or tag mismatch
   (`mis-tagged?`). A refusal here is answered by `ack-return>` and nothing
   else happens (P7).
3. Reads, in dataflow, all local: the layer's entry
   `(local-select> [(keypath *layer)] $$layers :> *entry)` (nil when the layer
   is not made; settings come with it), the answer record
   `(local-select> [(keypath *layer :answers *name)] $$layers :> *rec)`, the
   task's clock `(local-select> STAY $$clock :> *clock)`, the wall
   `(gate/wall-now :> *wall)`, and, only when the offer is not already
   decided: the cited permission's row `(local-select> [(keypath *layer
   :permissions *pid)] $$layers :> *perm)` (skipped for the operator and the
   store), and for each fact with `:replaces`, the heads row
   `(local-select> [(keypath *layer :heads [*e *k *r])] $$layers :> *head)`
   (via `ops/explode` over the replacing facts, collected into a map with an
   aggregator-free local accumulation: the facts are a small vector, so a
   `loop<-` over them binding a map of `r → stamp-or-nil` is the shape).
   Reads are fixed in count except the heads reads, which are one per
   replacing fact — issued only for those facts (none for an act with no
   replaces).
4. `(gate/decide offer entry rec perm heads clock wall :> *d)` — pure, total,
   the executable spec of `stream-step` + `refusal` + `stamp-for` under
   `baseline`, returning one of:
   - `{:kind :recorded :answer rec}` — a record exists with the same digest:
     the model's "already decided here"; nothing is written (I-G2, E1 N2/N3
     × resend, × replay).
   - `{:kind :taken}` — a record exists with another digest: name taken;
     nothing is written, the offerer hears it through the ack (P7, O4).
   - `{:kind :decide :answer {...} :stamp s :log rows-or-nil :heads-del [..]
     :heads-put [..] :settings {..} :permissions {..}}` — a fresh decision,
     yes or no, with every write precomputed.
   The reason order for a fresh decision, as I-G5 with the rig's own reasons
   placed (P7): `:fact-outside-the-acts-layer`; `:no-such-layer` (the layer
   has no class fact and the act carries none); `:class-mismatch` (the
   offer's `:class` against the class in force: the layer's class fact, else
   the act's own `:class` fact, P10); `:permission-does-not-cover-this`
   (pid's who and layer against the offer's, or no pid for a person);
   `:permission-from-another-layer` (pid's `in` ≠ the offer's layer);
   `:no-permission` (no row); `:permission-revoked` (row has `:revoked`);
   `:stale-replaces` (a replacing fact whose `[e k r]` heads row is absent,
   or two facts of the act replacing one fact); `:stale-revoke` (a revoke
   fact whose target pid has no unrevoked row in this layer, P8). The
   operator and `:store` skip the four permission checks (`exempt?`). A yes
   is the absence of every reason. The stamp is computed for a yes and a no
   alike (`decide` in the model stamps refusals).
5. Writes, only for `:kind :decide`, all `termval`/`NONE>` sets as listed
   under "Writes": answer record, log entry (yes only), heads deletes and
   puts (yes only), settings fields for `:kind`/`:owner`/`:class`/`:lock-grain`
   facts (yes only), permission rows for `:permission`/`:revoke` facts (yes
   only), `$$clock`. A test-only crash hook (`inject/maybe-fail!`, R3, P14)
   sits between the reads and the writes and again after the writes, so a
   crash mid-offer can be forced on either side of the decision.
6. `(ack-return> answer)` — the answer map for every kind:
   `{:answer :yes|:no :reason r :stamp s :name name}` for a fresh or recorded
   decision, `{:answer :no :reason :name-taken :name name}` for a taken name,
   `{:answer :no :reason r :name name-or-nil}` for a face refusal. The client
   reads it under `"gate"` in the ack map.

**Idempotency of every write, traced.** `answers[name]`: set once per name
(a replay finds it and skips). `log[name]`: same. `heads[[e k fid]]`: set to
the act's stamp; a replay that re-reaches the write (nothing committed) sets
the same value. `heads[[e k r]]` deleted: deleting twice is the same. Settings
and permission rows: sets of values derived from the act. `$$clock`: set to
the stamp, which is `max(...)` over state the replay sees identically when
nothing committed; when the first attempt committed, the replay never reaches
the write. No increments, no appends to lists: no non-idempotent write exists.
A streaming batch that fails is discarded whole (RQ 1), and the gate never
throws (below), so the only failure is a worker crash, after which the record
replays from the source and takes the recorded path or a fresh decision on
unchanged state.

**Same answer after a crash mid-offer** (R7's last case, E1 N1 × crash): a
crash before the writes commit leaves no record; the replay decides on the
same state, so the same reason; the stamp may differ from what the lost
attempt would have given, but no attempt gave one (nothing committed), so
there is one stamp. A crash after the commit: the replay finds the record and
returns it, writing nothing; a completed record replayed after a later crash
(RQ 2, D4) takes the same path. Tests assert "at least once" (R4).

**No input can make topology code throw** (I-G1, RQ 3): (1) the source binds
the raw record; (2) `parse` is total and wraps its body in `try` as a last
line, turning any exception into `{:refuse :malformed}`; (3) `decide` is
total on parsed data and likewise wrapped; (4) every value written has the
class the schema names, because `parse` normalised it, and every path key is
a keyword or a `vec`'d vector; (5) `ack-return>` sends a map of keywords,
longs, strings and vectors. A property test drives `parse` and `decide` with
generated garbage and asserts no exception; an IPC test appends non-maps,
maps with unknown parts, a version 2, a name that is a string, and an empty
act, and asserts the ack carries a refusal and the worker did not restart.
This is the load-bearing invariant of the build: a schema violation or a
destructuring failure inside the topology is a worker restart loop on a
poison record.

**Cooperative multitasking.** No loop runs over PState contents; the only
loop is over the act's facts, bounded by the offer. No `:allow-yield?` read
exists. An enormous act blocks its task for its own decision, which is the
ruled unit ("acts of any size", admitted whole); no limit (P7).

**PStates owned:** `$$layers`, `$$clock`, schemas above.

## Query Topologies

None in this stage. Every read is a single `foreign-select-one` on one
partition (the table under "Reads"). The reads that would need one, a point
read as of a moment across chain history or a pattern read across layers,
are stage 5's; this stage leaves the log and the heads keyed so that a per
layer range scan answers them (see "What later stages consume").

## Partitioning efficiency

**Optimal placement, derived first.** The dominant read is the gate's own
decision, run once per offer; its second is the offerer's lookup (RD1). Both
want every datum of one layer on one task: `f(layer) → one task`, the same
for the answers, the log, the heads, the permissions and the settings of that
layer, so that a decision and a lookup are point reads with no hop. Ruling 2
fixes this placement ("by layer for one-owner layers") and fixes N. Load
balance across layers is what a hash gives when there are many layers per
task (sessions accumulate: OP5 "each task is home to ever more layers");
per-layer skew (a hot agent session) is by ruling a hotspot on one task,
remedied by re-class, not by placement. So `f(layer) = hash(layer) mod N`,
which `hash-by :layer` on the depot and the default key partitioner of
`$$layers` both implement (P2). The alternative, a stored placement per
layer, is rejected on total cost above ("Depots").

**Validation.** Seeks/op are totals across the cluster: every read below
touches exactly one task, so the totals do not change with N. The block cache
makes the layer's entry a memory hit for a hot layer; it is counted as a seek
regardless.

Data categories for the gate's decision (one offer):

- (a) an ordinary act with no replacing fact, a person citing a permission:
  layer entry + answer record + permission row = 3 seeks.
- (b) an act with r replacing facts (r = 1 typical, counted as 1): 3 + r = 4.
- (c) a resend or a replay of a decided offer: layer entry + answer record =
  2 (the digest is computed, nothing else read).
- (d) an operator act (make, grant, revoke, re-class): layer entry + answer
  record + (revoke only) permission row = 2 to 3, counted 3.
- (e) a face refusal (malformed, mis-tagged): 0 seeks, nothing read.

Frequencies: agent session layers write "many small acts" (P 54-55) mostly
without replaces; replaces are the person's edits; resends are per client
error (RQ 4) plus replays after a crash; operator acts are rare.

### N = 1 task (single-task baseline)
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| (a) ordinary act, no replace | 0.60 | 3 | 0 |
| (b) act with a replace | 0.25 | 4 | 0 |
| (c) resend / replay, decided | 0.10 | 2 | 0 |
| (d) operator act | 0.04 | 3 | 0 |
| (e) face refusal | 0.01 | 0 | 0 |
Weighted seeks = 3.12   |   Weighted iterator reads = 0

### N = 16 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| (a) ordinary act, no replace | 0.60 | 3 | 0 |
| (b) act with a replace | 0.25 | 4 | 0 |
| (c) resend / replay, decided | 0.10 | 2 | 0 |
| (d) operator act | 0.04 | 3 | 0 |
| (e) face refusal | 0.01 | 0 | 0 |
Weighted seeks = 3.12   |   Weighted iterator reads = 0

### N = 128 tasks
| Data category | Frequency proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| (a) ordinary act, no replace | 0.60 | 3 | 0 |
| (b) act with a replace | 0.25 | 4 | 0 |
| (c) resend / replay, decided | 0.10 | 2 | 0 |
| (d) operator act | 0.04 | 3 | 0 |
| (e) face refusal | 0.01 | 0 | 0 |
Weighted seeks = 3.12   |   Weighted iterator reads = 0

Flat in N: every offer is decided on one task with point reads. The offerer's
lookup (RD1) is 2 seeks at every N for the same reason. Writes per offer are
likewise on one task: 2 to 2 + 2r + (setting and permission facts) sets and
one clock set, flat in N.

Where the design is known not to be optimal and accepts it by ruling: one
layer's throughput is one task's thread (OP1 "Throughput"); a hot layer is
the ruled case for re-class (ruling 2), and phase 7's M1 and M3 measure
exactly that bound.

## Design Decisions

- **Subindexing.** In `$$layers`, the four per-layer maps are subindexed
  because each grows without bound: `:answers` (one per name, kept forever,
  D4), `:log` (one per admitted act), `:heads` (one per unreplaced fact),
  `:permissions` (one per permission a layer holds; a hand session holds a
  person's session permissions for every layer). Size tracking is off on all
  four. Not subindexed: `:settings` (four fields); the log entry's inner
  vector (one act, the ruled unit of admission and reading, sized by its
  offerer); `:mark` (at most the two marks the parser knows); `:subjects` and
  `:stood-on` on the answer record (sized by the offer, the input the
  offerer controls, like the act itself).
- **Colocation.** The depot's `hash-by :layer` and `$$layers`'s default key
  partitioner put an offer and every datum of its layer on one task, so the
  gate reads and writes locally with no partitioner in the event, and the
  offerer's lookup routes by the layer id. `$$clock` is per task and read on
  whatever task the event is on. Nothing in this stage hops.
- **One event, no hops.** The decision reads before it writes, computes every
  write in one pure function, then writes. The atomic group is the event
  (RQ 1); the answer and the admitted facts become visible together (I-G6).
- **Refusals as data, two classes.** Refusals before the name is trusted are
  not recorded (P7); refusals after it are the name's answer for ever, with a
  stamp, as the model records them. The offerer learns both through the ack,
  and the recorded class also through a lookup.
- **Settings and permissions are facts first.** A setting is an admitted fact
  (`:kind`, `:owner`, `:class`, `:lock-grain`) in the log; a permission and a
  revocation likewise. `:settings` and `:permissions` are projections the
  gate keeps for its checks, rebuildable from the log; they hold no value
  plaintext beyond the setting keywords and the pid, which are the store's
  own control data (D2: the facts the store acts on carry no lock).
- **The class in force** for the class check is the layer's class fact, else
  the act's own `:class` fact when it carries one (the making act, P10).
  This one rule covers making, ordinary offers and re-class.
- **Names route, tags refuse.** The depot routes by the act's layer; the tag
  check compares the name with the act reading nothing; a name reaches one
  gate only because its tag says which class it was made for: the client
  picks the depot by the tag once the micro store exists (stage 3), and each
  gate refuses on its face a name tagged for the other (`:wrong-gate`, P7),
  so a misrouted name is never decided by the wrong gate.

## State primitive selection

- `$$layers` (PState): durable, partitioned by layer. Per admitted act the
  write volume is 2 + 2r + s + p sets, where r is the act's replacing facts,
  s its setting facts and p its permission facts, all bounded by the act the
  offerer sent. Per refused (recorded) offer: 1 set. Source of truth for the
  store: the log and the answers; `:heads`, `:settings` and `:permissions`
  are derived views whose volume is bounded by the same act.
- `$$clock` (PState): durable, one long per task, 1 set per decided offer.
- No TaskGlobal: nothing is cached; every read is a point read on the
  durable state the decision needs, and a cache of it would be one more
  thing to keep right across restarts for no seek saved (the layer's entry
  is block-cached by RocksDB already).
- No external system. The fingerprint secret is a constant in code (P6).

## Resource usage analysis

Ids are keywords of about 10 bytes serialized; a name is about 60 bytes
(two keywords, a keyword, a 16-byte UUID, vector framing); a fact id about
70; a pid about 40; a stamp 8.

### Disk usage (PStates and the depot), per task

- `:answers`: one record per name. Fixed parts (answer, reason, stamp,
  digest as 64 hex chars, who, class, permission, session, claimed-when)
  about 250 bytes; `:stood-on` about 80 bytes per stood-on fact; `:subjects`
  about 10 per person; `:because-of` 60. Typical record 350 to 500 bytes plus
  the 60-byte key. Growth: one per offer decided (yes or recorded no); never
  expired (D4).
- `:log`: one entry per admitted act; per row about 60 bytes of framing and
  ids, plus the value's EDN text (the toy's values are about 40 characters),
  plus 70 for `:replaces` when present. A three-fact act is about 400 bytes.
  Growth: one per admitted act.
- `:heads`: about 110 bytes per unreplaced fact (key 100, stamp 8). Net
  growth per act: facts admitted minus facts replaced.
- `:permissions`: about 200 bytes per permission. Growth: per grant; rare.
- `:settings`: about 80 bytes per layer.
- `$$clock`: 8 bytes.
- `*offers` depot: the raw offer, about the answer record plus the facts,
  500 bytes to 1 KB per offer; retained (no trimming policy in the rig).

At 100,000 acts of three facts on one task (M1, M2's scale): answers about
45 MB, log about 40 MB, heads about 33 MB, depot about 80 MB. With N tasks
and layers spread by hash, each task holds its layers' share.

### Memory usage (TaskGlobals)

None.

### Minimization

- The digest is stored once (64 hex characters); a binary form would halve
  it. Kept as text for readability in the rig; a kept store could store
  bytes.
- `:stood-on` is stored on the act record; the read entry (stage 5) may keep
  its own line, so stage 5 decides whether this copy stays. The stamp per
  stood-on fact is what the clock promise used; it is not derivable later
  from the ids alone without a read per id.
- `:subjects` duplicates the owner (from settings) into every act record
  because the ruled slot is on the act (ruling 8) and stage 2's wrap reads it
  there; ten bytes per act.
- The answer record and the log entry share the name as key twice (once in
  each map); the alternative (Option C) was rejected on the replay path's
  cost, not on size.
- Nothing is duplicated across storage locations beyond the projections
  named under "Design Decisions", each a few bytes per fact and needed by
  every decision.

## Rig choices proposed

Each is a pick where the rulings are silent, one line of what and one of why.
None changes PROGRESS.md. The build session copies the ones it keeps into
RIG.md with the next free numbers.

- **P1. One module, `rig.store.module/Store`, holds both stores** (this
  stage's stream gate and depot; stage 3's micro gate and depot). Why: the
  micro gate must read the stream store's settled state and the stream gate
  must hand landings to the micro depot; inside one module both are plain
  same-module operations with one task count and no mirrors, and the rig's
  tests launch one module; "two fact stores, each with its own gate" holds as
  two depots, two topologies, two partitionings.
- **P2. A one-owner layer's home task is `hash(layer) mod N`**, computed by
  `hash-by :layer` on the depot and by the default key partitioner of
  `$$layers`; no placement table. Why: the depot partitioner is a pure
  function of the record, so a stored home could not route appends without a
  read per offer and a second mechanism for the same function.
- **P3. Ids (layer, entity, fact key, person) are keywords in the rig.**
  Why: the model's world and its expected answers are keyworded, tests read
  them directly, and phase 8's replay compares by these ids; a kept store's
  random ids are PROGRESS.md's open items 41, 59 and 17.
- **P4. A name is `[layer class scheme uuid7]`**; class is nil for an act the
  store places (every fact key in `#{:class :lock-grain :forget :crossed}`);
  schemes `:crossing` and `:landing` are reserved to the store, `:offer` is
  everyone else's. Why: the tag is read off the name with no parsing, the
  UUID7 is time-ordered within a layer's answers, and the derived names of
  stage 4 keep the request's UUID under another scheme.
- **P5. The envelope's parts are the fixed sets above, version 1, values
  restricted to EDN data.** Why: "the gate refusing parts it does not know"
  needs a closed set, and a value outside EDN data could not be stored in a
  typed slot or digested canonically.
- **P6. The digest is derived, not carried**: HMAC-SHA256 under a rig
  constant secret over the canonical EDN of the offer minus its name,
  computed by the gate when it records and by the client when it looks up.
  Why: a carried digest is either recomputed (redundant) or trusted (a
  client bug would refuse its own resend as name taken); keyed because D8
  says every fingerprint over values is; the secret lives in code only
  because the rig keeps no records. A consequence the client owns: the
  digest covers `:claimed-when` and `:stood-on`, so only the identical map
  answers from the record; a client keeps what it sent until it has an
  answer, and one that lost it makes a new name.
- **P7. Refusals before the name is trusted are answered through the ack
  only and never recorded**: malformed envelope, unknown part, unknown
  version, bad name, reserved scheme, empty act, tag mismatch, a name tagged
  for the other gate (`:wrong-gate`: tag class `:by-entity` at the stream
  gate, so a name reaches one gate only even if a client misroutes it), and
  name taken. Everything from `:fact-outside-the-acts-layer` on is recorded
  under the name with a stamp, in I-G5's order, with the rig's own reasons
  placed: `:no-such-layer` just before `:class-mismatch`, `:stale-revoke`
  after `:stale-replaces`; no act size limit; no mixed-act refusal (the model
  places per fact; in this stage every fact of a one-owner layer is on the
  home). A recorded no keeps the act's parts (who, class, permission,
  session, stood-on, because-of, claimed-when, subjects) like a yes, so a
  refused request is still findable by what it was because-of (E1 N3). Why:
  a refusal before the name is trusted cannot be recorded under a name (O4),
  and it is deterministic, so a resend gets the same answer without a
  record; recording every later refusal keeps "a refused name stays refused"
  (OP2); one record shape. (O1, O4)
- **P8. A permission id is `[who layer in]`** and its checks run as the
  model's: cover and home from the id, exists and revoked from this layer's
  index; only the operator grants and revokes; a revoke of an unknown,
  elsewhere-kept or already revoked permission is refused `:stale-revoke`;
  no cascade from a session root; no re-grant (a second grant fact under the
  same id is admitted as a fact and leaves the index's first grant and its
  revocation state as they are). Why: it is the model's triple with the
  session resolved to the layer the fact lives in, so a gate reads nothing
  for the first two checks, and a revoke standing on its grant mirrors a
  replace standing on its head. (O6)
- **P9. A stamp is a long, `max(wall ms, clock+1, stood-on+1,
  replaced+1)`**, with the wall from `TopologyUtils/currentTimeMillis`, and
  **the offer carries the stamps of what it stood on** (`:stood-on {fid
  stamp}`): the gate keeps the clock promise against the carried stamps and
  the replaced facts' recorded stamps, and re-reads nothing. Why: one scale
  comparable across tasks and stores (I-O3) that tests can drive with
  simulated time; the offerer read what it stood on and holds those stamps
  (the envelope's "based on" and "expected versions"), re-reading them would
  mean a hop for every stood-on fact on another task or in the other store,
  and the ruled order between stores is stood-on itself, a read stamped in
  the checking store. (O3, stream side)
- **P10. A layer is made by an operator act carrying its `:kind`, `:owner`,
  `:class` and `:lock-grain` facts**; the class in force for an act into a
  layer with no class fact is the act's own `:class` fact; who may change a
  setting later is the permission check's business (the operator is exempt;
  the owner's own permission covers a grain switch). Why: "layers are data"
  needs a first act that passes the gate, and one rule for the class in force
  covers making, ordinary offers and re-class.
- **P11. The source's retry mode is `:all-after`.** Why: the stream is
  order-sensitive per layer, and since every decided offer is answered from
  its record, replaying the records after a failed one costs reads only.
- **P12. The value slot is stored as canonical EDN text in this stage.** Why:
  a fully typed slot for open data (no `Object` in a schema), stored as
  offered and decoded exactly; stage 2 will put ciphertext in the same slot.
- **P13. A retract is a replacing fact with `:v nil`; its undo is a fact
  replacing the retract and carrying the value again; a chain's latest head
  is the maximum stamp among its unreplaced facts.** Why: it needs no new
  part and no new key, time travel shows it as a fact, and the head rule is
  the model's. (O5)
- **P14. A test-only crash hook (`rig.store.inject`) sits before and after
  the gate's writes**, armed by name through a global atom. Why: R3 (failure
  injection through global atoms, in-process cluster only); R7's last case
  needs a crash on either side of the decision.
- **P15. The act's subject slot is the layer's owner unioned with what the
  offer carries.** Why: the ruled three sources are owner, grammar and tool;
  the owner is a setting the gate already has, the tool's are carried, and
  the grammar's are stage 2's until stage 6 makes grammars facts.

## What later stages consume, and where it is

Stated as what this stage leaves, not as their design.

- **A value's row and its task.** Value id `[name idx]`; row at
  `$$layers [layer :log name]`, position `idx`, on task `hash(layer) mod N`
  while the layer is by layer. The row carries `:mark`, so a lock's placement
  override and the die-with-any mark are readable where the value is.
- **For locks and forgetting (stage 2).** Anything keyed by a value id and
  wanted on the value's task shares `$$layers`'s key and partitioner, so by
  the merge rule it is a field of `$$layers`'s value (a lock-row map keyed
  by fact id, subindexed) or a field of the row itself (a lock kept in the
  record). The act's subject slot is on the answer record; the layer's owner
  and grain are in `:settings`; a forget is an act whose name's tag has class
  nil (P4) and reaches the stream gate on the layer's home, which is the
  value's task. The value slot is text now; ciphertext can take the same
  slot. The module is redeclared at each stage and the rig keeps no records,
  so a schema that grows needs no migration.
- **For the micro store (stage 3).** Same module (P1). A second depot placed
  by entity, a microbatch topology and its own PStates, all theirs. The
  client picks the depot by the name's tag: class `:by-layer` and nil go to
  `*offers`, `:by-entity` to theirs, so "a name reaches one gate only" is the
  client's routing plus each gate's tag check. The stream gate keeps deciding
  every act whose tag class is nil for a one-owner layer, before and after a
  re-class, on the home (in the model the micro gate decides a setting act
  for a re-classed layer; the checks are the same `refusal`, the stamp comes
  from the stream task's clock, and the answer is found by the lookup's
  first read; phase 8 reports it if a history shows a difference), refuses
  `:by-layer`-tagged acts after the re-class (`:class-mismatch`), and refuses
  `:by-entity`-tagged names on their face (`:wrong-gate`, P7); a re-classed layer's settled heads, permissions and
  settings are readable in `$$layers` as committed state from any topology in
  the module (R6, O3's other half, theirs to use). The settled frontier (R5)
  and the name row are theirs.
- **The landing handoff (stage 4 sends it, stage 3 receives it).** A
  promotion request is an ordinary act in the owner's layer; the read-out is
  an act named `[layer nil :crossing id]` (derived from the request's name)
  decided by the stream gate on the layer's home, which holds the value and
  (stage 2) its lock, writing the `:crossed` fact there. The landing is then
  handed over from inside the same event: a commit boundary
  `(|direct (ops/current-task-id))` so the crossing fact is durable first,
  the micro depot's partitioner to the landing's task, and
  `depot-partition-append!` of the landing offer (named `[target :by-entity
  :landing id]`, standing on the crossing fact with its stamp carried, P9)
  with `:append-ack`. A replay re-appends the same landing; the micro gate
  answers the second from its record because the name and the digest are the
  same, which is what "a retried request landing once" needs.
- **For reads (stage 5).** `:log` per layer holds every admitted act's rows;
  `:answers` holds each act's stamp and who; `:heads` holds the current
  heads with their stamps, keyed so a range over `[e k]` gives a chain's
  heads. A by-stamp index (stamp → name per layer) is rebuildable from
  `:answers` and belongs in `$$layers` if kept. Every index over values they
  add must be keyed by, or purgeable by, `[name idx]`.
- **For tools and grammars (stage 6).** The envelope's `:subjects` part is
  where a tool's subjects arrive; the grammar's subjects (the `:mention`
  rule stage 2 keeps as a constant) move into facts there.

## The client side (`rig.store.client`)

The offerer's library, plain Clojure over the foreign API, in the same
process in the rig.

- `(connect ipc)` → `{:module-name :depot :layers :clock}` from
  `foreign-depot` / `foreign-pstate` once (foreign-client.md: handles once).
- `(make-name layer class)` → `[layer class :offer (ops/random-uuid7)]`;
  `(name-for layer class facts)` applies the store-placed rule (class nil
  when every fact key is a placed key).
- `(build {:who :layer :class :permission :facts ...})` → the offer with
  `:version 1`, its name, and defaults (`:stood-on {}`, `:subjects #{}`,
  `:because-of nil`, `:session nil`, `:claimed-when (System/currentTimeMillis)`).
  Pure; the map it returns is what is sent and what is resent.
- `(digest offer)` from `rig.store.envelope` (the same function the gate
  uses; P6).
- `(offer! store offer)` → `(foreign-append! depot offer :ack)` and the
  answer under `"gate"` in the ack map. Blocks until the decision is visible.
- `(offer-until-answered! store offer)` → `offer!`; on an exception (RQ 4)
  `lookup`; an answer ends it, otherwise the same map is sent again, up to
  a bound, then the last error.
- `(lookup store name digest)` → `foreign-select-one [(keypath layer :answers
  name)]` with `layer` the name's first element; nil → `:no-answer`; the
  record's digest ≠ `digest` → `{:answer :no :reason :name-taken}`; else the
  record (RD1, `lookup` in the model).
- `(facts store layer name)` → the log entry with each `:v` decoded from
  EDN; `(settings store layer)`, `(permission store layer pid)`, `(head?
  store layer e k fid)`, and `(clock store layer)` → `foreign-select-one STAY
  $$clock {:pkey layer}`, which routes to the layer's home task.
- `(seed! store world)` → the operator's making and granting acts for a
  world map `{:layers {id {:kind :owner}} :permissions [pid ...]}`; the
  default world is the model's one-owner side (the "Writes" table's seed
  row). Returns the answers so a test can assert every seed was admitted.
- `(crossing-name req-name)`, `(landing-name req-name)` in
  `rig.store.envelope`, defined now so the reserved-scheme refusal has a
  concrete shape; used by stage 4.

## Namespaces and tests

- `src/rig/store/envelope.clj` — the shapes: `parse` (total), `known-parts`,
  `placed-keys`, `store-placed?`, `tag-of`, `mis-tagged?`, `canonical`,
  `digest`, `fingerprint-secret`, name construction and derivation. Pure,
  shared by module and client.
- `src/rig/store/gate.clj` — `refusal`, `stamp-for`, `decide`, `wall-now`,
  the reason order; pure and total, the rig's executable counterpart of
  `stream-step` + `refusal` + `stamp-for`.
- `src/rig/store/inject.clj` — the test-only crash hook (P14).
- `src/rig/store/module.clj` — `defmodule Store`: `*offers`, the `gate`
  topology, `$$layers`, `$$clock`, wiring the two pure namespaces into
  dataflow.
- `src/rig/store/client.clj` — the offerer's side above.
- `test/rig/store/envelope_test.clj` — property tests (test.check is on the
  test classpath): `parse` and `decide` never throw on generated input;
  `digest` is the same for `=` offers built in different orders and differs
  when any part differs; `tag-of` matches the model's `tag-of` on the
  model's own offer shapes translated to the rig's.
- `test/rig/store/gate_test.clj` — one IPC, `{:tasks (rand-nth [2 4 8])
  :threads 2 :workers 1}`, seeded with the model's world, then `testing`
  blocks for every branch R7 lists, with the acts on `:alice`,
  `:alice-hand` and `:alice-agent`: admit an act whole on its home (answer,
  log, heads, stamp, clock); refuse on its face a name made for another layer
  and one made for another class, and a reserved scheme; refuse a reuse with
  other content by digest and leave the first record untouched; answer a
  retry from the record with the same stamp and no second log entry; refuse
  a fact naming another layer, a class mismatch (before and after a
  re-class), a permission that does not cover (Bob's and a nil one), one
  kept in another layer (`[:alice :alice-agent :alice-hand]` cited on
  `:alice-agent`), one that does not exist, one revoked; a stale replace
  (already replaced, in another layer, unknown) and a doubled replace; an
  act with one good and one stale fact refused whole; the stamp never
  backward per task, above every carried stood-on stamp and above the
  replaced fact's stamp, at or above the wall under simulated time; the same
  answer after a crash mid-offer, injected before the writes and after them
  (one log entry, one stamp, the worker restarted at least once, R4); a
  retract and its undo; the malformed inputs of "no throw". Run with
  `clojure -M:test rig.store.envelope-test rig.store.gate-test` from the rig
  folder.
- Where cheap, a branch's expected answer is also asked of the model: the
  model rides on the test classpath, and `formal.model/refusal` is private,
  so parity is checked through `formal.model/run` on a one-offer history
  where the branch exists there (class mismatch, permissions, stale
  replaces); the branches the model cannot express (malformed, reserved
  scheme, no such layer) are asserted against this plan's reason names.

## Design difficulty log

Written while designing, first person.

- **Where the digest is computed.** The model has the offer carry it and the
  gate compare. I first kept that; then asked what the gate would do with a
  carried digest it could not trust (recompute, so redundant; trust, so a
  client that rebuilt its offer with a fresh timestamp refuses itself as
  name taken). Deriving it at the gate and at the lookup from one function
  was forced once I saw the lookup is where the client's copy matters
  (`lookup` takes the asker's digest). Close only until that was seen.
- **Stood-on stamps: carry or read.** Reading them is one seek on the same
  task and a hop pair for another task or the other store; the hop breaks
  the no-partitioner event and makes the decision order in a layer depend on
  hop latency. Carrying them uses a ruled envelope part. Genuinely
  competitive for same-task facts (one cheap seek would verify a client's
  claim); settled by the ruled shape of the order between stores (a read
  stamped in the checking store) and by keeping the event hop-free, which
  the atomicity requirement wants. The trust boundary is written down as
  P9.
- **Log keyed by act or by fact.** By fact (`[name idx]` rows) is the
  "obvious" event log and lets one row be rewritten alone; by act is one
  write per act and matches "admitted whole, read whole". Nothing this stage
  reads or that later stages are stated to need reads a single row without
  its act, and M1 counts index writes per act, so by act won on write cost;
  a row is still addressed by its id. I would have chosen differently if the
  spec had a per-row read on a hot path.
- **One PState or five.** Forced by the merge rule once I noticed every
  piece shares the layer key and partitioner; the only doubt was whether a
  fixed-keys value with four subindexed children and a plain field is
  created cleanly on the first nested write, which the build checks first
  (the fallback is the five-PState shape with the same paths).
- **The value slot's type.** `Object` is banned; a closed set of records is
  wrong for open data; bytes (nippy) or text (EDN) both fit. Text won on
  debuggability and on not depending on an array class in a schema; stage 2
  will put ciphertext there either way. A real choice, not a hard one.
- **The name's form.** A string with a printed tag needs parsing at the gate
  and on the client; a vector needs neither and sorts by layer, class and
  time. Vector, quickly; the one cost is that every vector reaching a write
  must be a `PersistentVector`, which the parser guarantees.
- **One module or two.** Two modules is the shape phase 0 tested for the
  mirror read. One module was forced by the landing handoff and the settled
  reads, which need no mirror inside a module, and by test launch cost.
  Not close once the handoff was traced.
- **What is recorded.** Whether a face refusal or a taken name should be
  recorded somewhere was the one place I went back and forth: recording it
  would overwrite or shadow the first use; not recording it means a lookup
  cannot tell "never sent" from "refused on its face". The model records
  neither and the offerer hears it through the ack; a resend gets the same
  face answer deterministically. Settled by O4's wording and by the fact
  that no read in the spec asks for face refusals later.
- **The permission id.** A fact id (`[name idx]`) as the permission's id is
  the "facts all the way" answer, but the model's first two checks read
  nothing because the id is self-describing, and their order in I-G5 puts
  cover before home. Keeping the triple keeps the order and the zero reads;
  a fact-id form would reorder two reasons in one corner. Settled by "decide
  the same way".

## Self-validation against `artifact-plan-validation.md`

Run against this plan before finishing; the Phase 2 artifact is not written
here.

- Query topologies: none; every read is one point read on one partition.
- PState schemas: `$$layers` and `$$clock` differ in key structure (layer
  key vs one value per task), so the split is justified; no `Object`; record
  shapes use `fixed-keys-schema`; no polymorphic position (the value slot is
  text by P12; `:reason` is a keyword or nil); inner collections that can
  exceed 100 elements are the four subindexed maps; the unsubindexed
  collections are bounded by the act or by the parser (`:mark`), or are the
  act's own carried sets (`:stood-on`, `:subjects`), which are the offerer's
  input like the act itself.
- Partitioning: `|hash` on the layer has many keys per task (sessions
  accumulate) and the hot-key case is the ruled re-class case; the table is
  filled for N = 1, 16, 128 with proportions summing to 1, seeks as totals
  across tasks, flat in N; a stored placement was considered and rejected on
  total cost; no justification rests on a later stage's mechanism (each
  hand-off is stated as what is left, and the two design consequences that
  do rest on a later stage, the depot choice by tag and the landing append,
  are described as theirs).
- Topologies: one stream topology, both stream reasons cited; every concern
  in it is the one decision; no test-synchronization argument was used.
- Production readiness: concurrent clients (two contents under one name: the
  first decided keeps it, the other hears name taken; a resend racing its
  original finds the record, even in one streaming batch, since the event
  sees uncommitted writes); a client restart (P6); a worker restart at any
  point (traced under "Topologies"); scale (subindexed maps, no scans); no
  non-idempotent write; no multi-partition write.
- Internal depots: none. Cross-topology flows: none in this stage. Stream
  `depot-partition-append!`: none in this stage (stage 4's is described with
  its commit boundary).
- In-memory state: none.
- Minimality, the simplest sketch: one depot by layer, one stream topology,
  one PState of answers-and-facts by layer, a clock. The plan adds `:heads`
  (delete it: the stale-replace check would scan the layer's log per
  replacing fact), `:permissions` and `:settings` (delete them: the
  permission and class checks would scan the log for control facts), the
  answers/log split (Option C, costed), and the digest (delete it: a reuse
  with other content would be answered from the record, violating I-G7).
  Each is required by a named check or ruling.
- Throughput: the cheapest design meeting every check is the one costed
  (3 to 4 point reads and a handful of no-read sets per offer on one task);
  the only lower-cost variant, Option C, trades the replay path for one
  write and was rejected with its cost.
- Spec coverage: OP1, OP2, OP4 to OP8 (stream side), RD1, RD10, E1 rows N0
  to N3 with every write, E3 rows L0 to L2 and L1 × re-class on the stream
  side, E4 rows P0 to P2 are each traced in the "Writes", "Topologies" and
  "Rig choices" sections; E2's chain rows on the gate's side (C0, C1, C3,
  C1 × two replaces, C1 × retract) in "Writes" and P13; the lock, forget and
  promotion rows are later stages'.

## What this plan could not settle

- Whether Rama accepts `clojure.lang.PersistentVector` as a subindexed map
  key class and creates a top-level fixed-keys value with four subindexed
  children on the first nested write; the build checks both in its first
  compile and, if not, falls back to five PStates keyed by layer with the
  same paths (and to `clojure.lang.IPersistentVector` for the key class).
- Whether vector keys in a subindexed map sort with prefix contiguity, which
  stage 5's range over `[e k]` in `:heads` wants; not needed by this stage's
  point reads.
- How a person starts or closes a session layer (O10, PROGRESS 82): in this
  stage only the operator makes layers (P10).
- `:session` on the offer is carried and stored, never checked; what a gate
  should check it against is not ruled.
