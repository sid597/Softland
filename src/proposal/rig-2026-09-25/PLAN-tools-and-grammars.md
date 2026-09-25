# Plan — tools and grammars from inside (rig phase 6)

Planned by Claude Opus 5.5 (`claude-opus-5-5`), effort max, in a fresh
session on 26 September 2026 from 02:41 IST, as the rama skill's plan step
(`references/phase-1-plan.md`, `references/artifact-plan.md`). Not yet
validated; a fresh session validates it next. Branch `rig-plan-tools`,
worktree `/mnt/data/projects/Softland-rig-plan-tools`. No code was written
and no probe was run: every Rama fact below rests on phase 0's receipts
(SPEC.md "What Rama showed") and the validated plans it cites; section 13
lists what the build must confirm first.

Words. "Key" is a fact's key and nothing else; "lock" is an encryption key.
A PState map's entries are "under" or "indexed by" something, never "keyed",
so "key" keeps one sense. Each load-bearing claim says what it rests on:
*checked* (read in the named source, or run), *derived* (reasoned from
checked facts), *assumed* (neither; the build or Sid confirms it).

Scope, from Sid's overnight brief (verbatim): "Phase 6: grammar facts
checked at the gate, tool facts, a minimal runner that finds tools by
matching, and the code that reads a file at a git revision into passages and
functions, with content and position and no store identity. Prove the runner
with one test tool and one test grammar over the model's toy keys (:note and
:mention), nothing about repo material". The revision reader is planned
separately (`PLAN-revision-reader.md`, worktree `Softland-rig-reader`); this
plan consumes it as a capability and designs nothing about passages. The
reference tool, and how a passage stays the same across revisions, are the
model's third round's.

## 0. The plan on one screen

- **A grammar lives in the layer it governs.** It is one control fact,
  `{:e <the key's id> :k :grammar :v {:shape s :subjects-at p :opaque b
  :index #{h}}}`, admitted by that layer's gate and projected by it into a
  per-key row beside the layer's settings and permission rows. Each gate
  reads the rows of an act's keys inside its own decision: the stream gate
  in its one event on the layer's home task, with no partitioner; the micro
  gate in the layer visit of its batch, where it already reads the layer's
  settings and permissions. No copy of any layer's facts is kept on another
  task by this plan's build. The store-wide alternatives (the base's grammar
  read by a hop, or copied to every task) are costed in section 3; the copy
  is marked unsettled there. A layer that wants another layer's grammar
  adopts it by an act in itself standing on the source: that is a copy too,
  designed and marked unsettled, not built tonight.
- **The grammar checks at the gate** where phase 2 already refuses one
  shape: `:value-shape`, after the value is opened, now from the grammar's
  shape. Subjects come from `:subjects-at` through phase 2's `subjects-of`,
  unchanged. An opaque key's values are neither shape-checked, nor matched
  by value, nor indexed by value, and every read shows them marked opaque.
  Index hints come from the rows, in place of the read exit's constant.
- **A tool is one ordinary fact** in the layer it runs in: what it matches
  (a read exit pattern), its signature (inputs, output keys, read-entry
  preference), the actor and permission it acts under, and a recipe of named
  steps over tonight's vocabulary with formulas in the leaves.
- **The minimal runner is operator code beside the door and the read
  exit**, not a topology. It finds tools by a pattern read, `[:k :tool]`,
  reads each tool's matches through the one exit as that tool (reader kind
  `:tool`, its preference, role `:matched`), and offers each output act
  through the door under the tool's permission, standing on the matched
  fact and the tool fact with their stamps, because of the matched act,
  under a name derived from the tool and the match, so a rerun is a retry
  and each match runs once. A tool whose outputs could match its own
  pattern, directly or through the layer's other tools, is refused as data.
- **The proof** is one test namespace, `rig.store.tools-test`: the toy
  grammars and one test tool written as facts only, the gate refusing and
  admitting by them, the runner finding and running the tool once per match.
- **The machinery count, predicted**: 11 fixed-side steps the frame
  promised, 4 nobody anticipated, 9 capabilities (section 8). The build
  takes the real count.

## 1. What binds this plan

Primary sources, read for this plan (*checked*):

- `src/proposal/frame-2026-09-15/PROGRESS.md` on main, "Now": line 65 "Keys
  are ids; word and grammar are facts about them"; lines 48-52, the envelope
  and "Tools add facts, never envelope parts"; line 56, every offer names the
  permission it acts under; ruling 3 (lines 76-83), "Spectrum, by tool: a
  deterministic tool gets the short entry by default and may ask for exact
  rows in its signature", and "trigger is already because-of"; ruling 5
  (line 87); ruling 6 (lines 88-90), "A key's grammar may declare its values
  opaque when a tool needs it, at the cost of no matching, no shape check
  and no index on them, and shown as opaque"; ruling 8 (lines 103-104), the
  subject slot "filled from the layer's owner, the key's grammar, and the
  tool"; the sharpening on subjects (lines 138-145); promotion's "The copy is
  about whoever the target's grammar and tool name" (line 156); permissions
  (lines 159-161), "a permission lives in the layer it governs; a gate checks
  only permissions in layers it orders"; and lines 162-165, "order between
  the two stores exists only through stood-on".
- `docs/decisions.md` lines 159-184, "Tools are records over a vocabulary":
  a recipe is "named steps over a vocabulary of capabilities, formulas in the
  leaves, one executor with several runners"; "A capability is code below
  the waist, added once, with receipts"; "Nothing in a record is a program".
- `src/proposal/store-next-2026-09-25/CONCLUSION.md`, "The seed: model round
  three" (lines 110-129): the tool fact (lines 114), the grammar fact (115),
  the count's classes and "A staleness index is the likely first honest
  protocol step" (129); R4 (line 78), based-on as fact ids with their stamps;
  R7 (line 93), permissions reaching a layer.
- The formal model's README, "What is in the toy" (lines 42-76): `:note`, a
  plain value, and `:mention`, "a value that names one or two people, so the
  key's grammar makes them subjects" (line 46); subjects (line 58); the tool
  contributes none. `model.clj:241-248`, `fact-subjects`, is the toy's
  grammar as one compiled branch.
- The rig folder: SPEC.md phase 6 and "What Rama showed"; RIG.md's defaults
  3 (read entries), 4 (one exit) and 5 (permissions: "agents and tools
  narrower still"), and its "Anything else" rule; `IMPLICIT_SPEC.md` OP16 to
  OP18, O20, O21 and E9 (lines 866-915, 1786-1796, 1915-1925).
- The validated plans this one plugs into: `PLAN-stream-store.md` (the one
  event, no partitioner, SP:391-465); `PLAN-locks-and-forgetting.md` (the
  grammar constant LP:178-189, "Grammar as data now, facts later"
  LP:1499-1504, the refusal order L27 LP:1821-1823, `:value-shape` L12
  LP:1725-1732, subjects LP:191-198); `PLAN-read-exit.md` (hints P:900-919,
  "Phase 6 replaces the constant with the key's grammar facts" P:915-916,
  what phase 6 feeds the exit P:1054-1056, `:ix-ke` kept for the runner
  P:441 and P:1186-1187); `PLAN-micro-store.md` at `848020f0` in
  `Softland-rig-plan-micro` (block 1 MP:208-223 and MP:1539-1578, the
  layer's projections on `hash(L)` MP:1290-1292); `PLAN-revision-reader.md`
  in `Softland-rig-reader` (operations lines 196-238, its place in the count
  lines 841-875).
- Prior thinking in the frame folder, not rulings (*checked* as text, weight
  as proposals): FALSIFICATION.md line 84 ("A claim into alice's layer is
  checked against her grammar and a claim into base against base's"), line
  235 ("The grammar revision a fact was checked against is recorded in its
  decision", held), line 266 (drift by recorded grammar revision); CORNERS.md
  lines 345-352 (C4.3, "A grammar version that requires more, or provides
  less, is a new key"; C4.4, "A grammar is a predicate over plain data. It
  never decides the bytes"); LEDGER.md line 43 (Sid: "values have shapes;
  grammars are facts with versions").

The code as it stands, on this branch at `b31ad57e` (*checked*): phase 1 and
the hybrid clock only. Phase 2 and the read exit are validated plans whose
builds had committed nothing when this plan was written (their worktrees
were clean at `e3989912` and `b31ad57e`). So this plan names the functions
those plans name (`locks/read-values`, `grammar/subjects-of`,
`reads/index-writes`, `reads/parse-pattern`, `read-exit/read!`, `open-row>`)
and the build binds to whatever they became; section 13 says what to check.

## 2. Today's compiled knowledge of `:note` and `:mention`, and what becomes a fact

| Where (*checked*) | What it knows | Becomes |
|---|---|---|
| `model.clj:241-248` `fact-subjects` | `(= :mention (:k f))` → the value's `:persons` are subjects | the `:mention` grammar fact's `:subjects-at [:persons]` (the model itself is round three's to extend) |
| Phase 1, `gate.clj:255` | nothing per key: the act's subjects are the carried `:subjects` plus the owner; test E1 (`stream_gate_test.clj:180-197`) checks that Bob is not added from a `:mention` | unchanged in phase 1; phase 2 adds the grammar's |
| Phase 2 plan, `rig.store.grammar/grammars` (LP:178-189) | the constant `{:mention {:subjects-at [:persons]}}`; `subjects-of` refuses `:value-shape` for another shape under `:mention` (L12) | the layer's `:keys` rows; `subjects-of` takes the rows' grammars in place of the constant and does not change (LP:1501-1503 anticipated exactly this) |
| Read exit plan, `reads/seed-hints` (P:903-906) | `{:by-value #{:note} :opaque #{} :no-copy #{:read/point :read/pattern}}` | `:by-value` and `:opaque` from the rows' `:index` and `:opaque`; `:no-copy` for the store's own read-entry keys stays compiled (`reads/store-hints`), because store keys are the floor (open item 3) |
| Read exit plan, "shown as opaque" (P:913-915) | not built | a row mark from the key's grammar (section 4.5) |
| `src/rig/bench/lock_slice.clj:95-106` | a bench's own copy of the `:mention` rule | untouched: a bench, not the store; phase 7 rebuilds benches on the finished store |
| `gate.clj:61-74` `control-value-ok?` and the envelope's store keys | shapes of the store's own control values | stay compiled: the store's keys are the protocol; `:grammar` joins them (section 4.1) |

So the store's compiled knowledge of the two toy keys, once phases 2 and 5
are built, is two constants: phase 2's grammar map and the read exit's
by-value hint. Phase 6 deletes both and writes two grammar facts in their
place (*derived*).

## 3. Where grammar facts live, and how each gate reads them (question 1)

### 3.1 What a gate needs from a grammar, and where it needs it

Every use of a grammar falls inside one layer's decision or one layer's
read (*derived* from the rulings and the plans above):

- the shape check (ruling 6) and the subjects (ruling 8) are needed when
  the gate opens a value: on the stream store, phase 2's step 5,
  `read-values`, on the layer's home task (LP:1060-1063); on the micro store,
  block 1 on the arrival task, before any hop (MP:208-223);
- the opaque flag and the index hints are needed where index entries are
  written (the gate's decision event, `reads/index-writes`, P:521-547),
  where a pattern is parsed (`read-pattern` on the layer's home, P:715),
  where rows are shown (`open-row>`), and where a layer's indexes are rebuilt
  (P:582-646);
- the runner needs the index hints only through the exit.

The stream gate decides in one event on one task with no partitioner, so
that every read sees the task's state and every write commits as one group
(SP:391-393; phase 0 finding 1). Whatever a stream decision reads must be on
the layer's home task.

### 3.2 The pick: the grammar a gate checks lives in the layer the gate orders

A grammar fact is a fact in a layer, and it governs that layer: values
written in layer L under key k are checked against the grammar for k in L.
The layer's gate projects its grammar facts into a row per key, in the
layer's own state, as phase 1 projects settings and permission rows
(`module.clj:131-138`, *checked*):

- **Stream store:** `$$layers [L :keys k]`, on L's home task. The gate
  reads the rows of the act's distinct keys in the same event, in step 4 of
  phase 2's order beside `:settings` and before `read-values` (LP:1058,
  *checked* as the plan's order), in a `loop<-` like the permission rows
  (`module.clj:101-108`). No partitioner, no hop.
- **Micro store:** `$$micro [L :keys k]`, on `hash(L)`, beside the layer's
  other projections there (MP:1290-1292, MP:1745-1748). The micro gate reads
  the rows in the `(|hash *layer)` visit where it reads the layer's settings
  and permission rows (MP:1550-1557). One consequence for phase 3's build
  (*derived*): its block 1 opens a sealed value and computes subjects on the
  arrival task before any hop (MP:208-223), and that step now needs the
  rows. The simplest road that keeps plaintext off the wire is to move the
  layer visit before the open: parse, `(|hash *layer)` for settings,
  permissions and key rows, then `(|hash *route-key)` back to the arrival
  task to open, check and drop the plaintext, then the name and entity
  visits as planned. It costs one hop per act in the batch, inside a
  microbatch whose cycle is already at least 300 ms (the skill's figure).
  How to fold it into block 1 is phase 3's; the requirement is that the rows
  reach the open.
- **Read exit:** `read-point` and `read-pattern` already run on the read
  layer's home (P:660-663); they read the row of the pattern's key before
  `parse-pattern`, and the rows of the keys they show (section 9).

Which grammar a value is checked under: the one in force before its act on
the stream store, and before its batch on the micro store. So it is
derivable at read time from the layer's own log: the latest grammar fact for
k in L admitted before the value's act (before its batch). Nothing new is
recorded on the decision (*derived*; the FALSIFICATION line 235 proposal to
record it is met by derivation, because the grammar is in the same layer
and its stamps order it). First-record: that the decision carries no
grammar version (T-FR3).

Why this pick (*derived*, each from a ruling or a plan):

1. It is the ruled pattern for the other thing a gate checks. "A permission
   lives in the layer it governs; a gate checks only permissions in layers
   it orders" (PROGRESS.md 159-161), and R7 writes a session's narrower
   permissions into each layer through that layer's own gate. A grammar is
   the other rule a gate enforces on an offer; it lives the same way.
2. It keeps every decision local: no partitioner in the stream event, no
   copy, no cross-store read, balanced across tasks by the layers' own
   placement.
3. The things a grammar drives are per layer already: the wraps (per value,
   in the layer), the four index fields (`$$layers [L]`, P:366-368), the
   pattern reads (a pattern matches "in layer L", P:145). Hints that decide
   L's indexes belong with L.
4. It reads the rulings' "the key's grammar" as "the key's grammar where the
   fact is written", which the promotion sharpening already uses: "The copy
   is about whoever the target's grammar and tool name" (line 156).
5. "From inside": a person can make a tool and its grammar in their own
   layer with the permission they hold there, and promote them later
   (promotion is the ruled cross-layer move), without write access to the
   base.

What it gives up (*derived*): one key can carry different grammars in
different layers. CORNERS C4.3's rule ("a grammar version that requires
more, or provides less, is a new key") says such divergence should be a new
key; nothing enforces it tonight. A layer that wants the base's grammar must
state it itself (3.4). Both are questions for Sid (section 12, Q1 and Q2).

### 3.3 The alternatives, and what each costs

Costs use the skill's model: a seek about 0.5 ms, an iterated entry about
5 µs, a network hop about 1 ms in-cluster (*assumed*, the order of
magnitude only). "Offer rate" is the cluster's admitted acts a second; the
rig's assumed threshold is 1,000 a second a task (RIG.md default 7).

**A. In the layer it governs (chosen).** Per act: one seek per distinct key
on the home task (usually one), no hop. Per grammar change: one fact, one
row write. Storage: one row per (layer, key used or grammared). Balanced by
layer placement. Divergence across layers possible (3.2).

**B. Store-wide, in the base, read at decision by a hop.** No copy. The
stream event would hop to the base's home task to read, then back to the
layer's home to decide and write (the writes stay in the last segment, so
they still commit together, *derived* from phase 0 finding 1). Costs: two
network hops on every offer's critical path (about 2 ms against a decision
of well under 1 ms today, *assumed*); and every offer in the cluster reads
on the base's one task, so that task serves the whole cluster's offer rate:
at N = 128 and 1,000 acts a second a task, about 128,000 grammar reads a
second on one task, against about 2,000 seeks a second one task can do at
0.5 ms each. Weighted seeks per operation stay flat, but the load
concentrates on one task as N grows, which fails "balanced computation
across tasks" (the skill's goal 1). Order: decisions would no longer follow
depot order on a layer's home (an offer returning from its hop can pass one
that left earlier); each is still one atomic group. After R8's re-class the
base is in the micro store, so the stream gate's read becomes a read of the
other store: allowed only as "a read, stamped in the checking store, never
atomic with the other's writes" (PROGRESS.md 162-165), so the decision must
record the grammar fact and stamp it read, a record change. Rejected: fails
balance; costs every offer two hops.

**C. Store-wide, in the base, with a copy on every task (`|all`).** This is
**a copy of one layer's facts on other tasks, kept near the reader:
unsettled.** CLAUDE.md "Systemic repair and caching" requires a fresh
adversarial examination of why it would not be needed before it is built;
builder A runs it. Costs: every read local (one seek per key); every grammar
change writes N rows (small and rare, which the skill's `|all` criterion
accepts). What the copy brings with it (*derived*): (1) one writer: a PState
has one owning topology, but the base's grammar facts are admitted by the
stream gate tonight and by the micro gate after R8's re-class, so one gate
would have to forward to the other through an internal depot, machinery
nobody planned; (2) replay: the `|all` hop runs after the base's record
commits, and a crash between them leaves some tasks without the new row,
because phase 1's record path writes nothing (`module.clj:93-96`), so the
record path would have to redo the fan-out, with monotone sets so an old
version never overwrites a newer one; (3) lag: a decision on another task
can use the older version until its copy is written, so the decision must
say which version it used, a record change. Phase 2 already keeps one copy
of this kind, `$$persons` on every task by `|all` (LP:945-952); whether it
went through the caching rule's examination I have not checked. Not taken:
A gives the same local reads with no copy, one writer per layer, and no
record change.

**D. Carried by the offer.** The door reads the grammar through the exit
and the offer carries it. The gate cannot trust an offerer's copy of the
rule it enforces without reading the source, which is B again; and a
grammar carried on the offer is a new envelope part ("Tools add facts, never
envelope parts", line 52; the gate refuses unknown parts). Rejected.

**E. A key whose id names its grammar** (the key's id derived from its
grammar's content, so the grammar is fixed for the key's life, CORNERS
C4.3's discipline built in). The gate could check a carried grammar against
the key's id with no read, but the grammar still has to be carried (D's
envelope part) or read (B). Rejected for tonight; noted because it makes
C4.3 structural, a question for round three.

**F. A layer adopts another layer's grammar, standing on it.** The grammar
row in L is written by an act in L, through L's gate, carrying the grammar
and standing on the source grammar fact (in the base, or a group) with its
stamp. The gate still reads only L's rows (A's cost). The dependence is a
recorded stood-on, "the only order" between layers or stores; a later change
at the source is found through it, which is where the staleness index
CONCLUSION line 129 calls "the likely first honest protocol step" would
come in. This is **a copy of one layer's facts in another layer, on another
task: unsettled**, under the same rule as C. It differs from C in being a
fact (attributed, visible, time-travelled, retractable) rather than
machinery, and in costing one act per adopting layer per change. Designed
here so the pick does not close it; not built tonight, and no test needs it.

### 3.4 Where "only through stood-on" puts a grammar the offer relied on

- Under the pick, the grammar an offer is checked under is its own layer's,
  read in the same event (stream) or the same batch (micro), so it is not a
  check against the other store and needs no order (*derived*).
- A layer relying on another layer's grammar does so only through an act in
  itself standing on the other's grammar fact with its stamp (road F). That
  stood-on is the whole of the order between them. The gate never checks
  the source; a change at the source leaves the adopted row as it was until
  a new act in the layer changes it, and the stood-on is how that is found.
- Under B or C the grammar would be a read of another layer at a moment,
  and after R8's re-class a read of the other store: "stamped in the checking
  store, never atomic with the other's writes, and the store promises nothing
  that needs such an order" (PROGRESS.md 162-165). The decision would then
  have to record what it read.

## 4. The grammar fact (question 2)

### 4.1 Its form (first-record, T-FR1 and T-FR2)

```clojure
{:e :mention                 ; the key's id: "keys are ids", so the key is the entity
 :k :grammar                 ; a store key
 :v {:shape       [:map {:persons [:set-of [:keyword] 1 2]} {:open? true}]
     :subjects-at [:persons] ; or nil: which subjects the key's values name
     :opaque      false
     :index       #{}}}      ; a subset of #{:by-value :no-copy}
```

- One fact per key per layer; the latest admitted grammar fact for a key in
  a layer is the one in force (as phase 1's settings merge,
  `gate.clj:275-278`). An act holding two grammar facts for one key is
  refused `:malformed-control`.
- `:grammar` joins the control keys (phase 1's `control-fact?`, phase 2's
  unsealed list LP:165-169): unsealed, no lock, projected by the gate, never
  forgotten by a value forget; a later grammar fact takes its place. The
  model's rule is the same for its control facts ("Store-control facts have
  no lock", README line 301). The gate must read a grammar on every later
  decision, and a sealed grammar would make every decision depend on a lock
  a forget can destroy.
- The fact's entity must not be a store key (the control keys, `:grammar`,
  `:read/point`, `:read/pattern`): a store key's grammar is compiled, and
  making it facts is open item 3. Refused `:malformed-control`.
- Who may write one: the operator, or the layer's owner (the owner's own
  permission covers it, as it covers a grain switch, `gate.clj:76-84`).
  Anyone else: `:control-not-allowed`. A rig choice (T-RC4); "from inside"
  may later want any writer holding a permission in the layer.

### 4.2 The value shape: a small total language of shapes

Every shape is a vector, so a row can hold it under a concrete schema type
(`clojure.lang.PersistentVector`), and a grammar is data, never code:

| Shape | Matches |
|---|---|
| `[:any]` | any value the floor admits (`env/edn-value?`, as today) |
| `[:nil]`, `[:boolean]`, `[:int]`, `[:uuid]` | that scalar (`:int` a long after `env/normalize-value`) |
| `[:keyword]` | a readable keyword (`env/readable-keyword?`) |
| `[:string]`, `[:string max]` | a string, at most `max` characters (max ≤ 1,048,576) |
| `[:enum v ...]` | one of up to 64 literal plain values, compared after normalisation |
| `[:vector-of s min max]`, `[:set-of s min max]` | a vector or set of `min` to `max` elements, each matching `s` (max ≤ 65,536) |
| `[:map {k s ...} opts]` | a map whose keyword keys each match their shape; `opts` `{:optional #{k ...} :open? bool}`: optional keys may be absent; an open map admits other keys unchecked |
| `[:or s ...]` | the first of up to 8 shapes that matches |

Totality (*derived*): a shape is finite data with no names and no
references, so no shape refers to itself and checking is structural
recursion over the shape and the value, both finite; the envelope bounds a
value's depth at 32 (`envelope.clj:55-58`). A shape is bounded at admission
(depth ≤ 8, at most 256 nodes, `[:or]` at most 8 branches). Because `[:or]`
can multiply work, the check counts the (shape, value) pairs it visits and
stops at 65,536, which refuses the value `:value-shape` like any mismatch
(the budget case is a test, section 7). The checker is one pure function,
`(shape/check s v)` → nil or the path of the first mismatch (or
`:budget`), never a throw (every function the gate calls catches
`Throwable`, as phase 2's `rig.store.locks` does, LP:1156-1157). The path is
for the tests and the build notes; the record keeps only `:value-shape`.

The shape checks meaning, never bytes: the floor's one encoding (canonical
EDN, `env/canonical`) is the same for every key, whatever its grammar
(CORNERS C4.4, *checked* as text).

A grammar's own value is checked at admission by `(grammar/parse v)`,
total: a map with exactly `:shape`, `:subjects-at`, `:opaque`, `:index`;
the shape within the language and its bounds; the rules of 4.3 to 4.5. A
failure refuses the act `:malformed-control`, at R13's place in the order
(`gate.clj:163-164`), so a malformed grammar needs no new refusal code.

### 4.3 Subjects: which subjects a key's values name

`:subjects-at` is nil or one path, a vector of map keys, into the value, to
a collection of person ids: phase 2's form exactly (LP:183, "the value's
`:persons` collection names people"), so phase 2's `(subjects-of grammars k
v)` does not change; it takes a map from key to grammar, which the rows
give. At admission `grammar/parse` checks that the path walks required
(not optional) entries of `[:map]` shapes and ends on a `[:set-of
[:keyword] min max]` or `[:vector-of [:keyword] min max]` whose `max` is
at most 256 (phase 2's cap, L13). So a value that passes its shape always
yields its subjects, and phase 2's own `:value-shape` refusal inside
`subjects-of` can no longer fire for a grammared key (*derived*).

The `:mention` grammar above says "one or two people" (the model, README
line 46): `[:set-of [:keyword] 1 2]`. A `:mention` of Bob in Alice's
layer: subjects `#{:alice :bob}` on the answer record (owner ∪ grammar ∪
carried, LP:191-198); its wrap `{:required [:alice]}`, or `[:alice :bob]`
required when marked `:die-with-any` (LP:203-209).

### 4.4 The opaque flag

`:opaque true` says the store never interprets the key's values in this
layer (ruling 6). At admission an opaque grammar must have shape `[:any]`,
no `:subjects-at`, and no `:by-value` in `:index` (each a contradiction:
no shape check, no subjects read from inside, no index); otherwise
`:malformed-control`. What follows, each a place in the code (section 10):

- no shape check: `read-values` skips the grammar's shape for the key; the
  floor's own checks (`edn-value?` after opening, phase 2's
  `:malformed-value`) still apply, because they are the encoding's, not the
  grammar's;
- no subjects from the value: owner and carried only (IMPLICIT_SPEC O20
  leaves an opaque value's subjects open; this is the smallest reading);
- no index on values: the key is in the hints' `:opaque` set, so
  `reads/index-writes` writes no `:ix-kv` entry for it (P:913-915);
- no matching: `[:kv k v]` on it is refused `:opaque` by the exit
  (P:157-158); `[:k k]`, `[:ek e k]`, `[:latest e k]` still find facts by
  key and entity, because they match on no value;
- shown as opaque: every row the exit shows for the key carries `:opaque
  true` beside its value (T-RC9).

What "opaque" means for the lock is a reading, not a ruling: tonight the
gate still unseals an opaque value mechanically, for the reused-name digest
and the re-wrap (LP:1073-1092), and never interprets it. The other reading,
that the gate never unseals it (a lock the store does not hold), would
change the digest, the forget and the door; it is Sid's (section 12, Q3).

### 4.5 Index hints

`:index` is a subset of `#{:by-value :no-copy}`, the read exit's own hint
names (P:902-906). `(grammar/hints rows)` builds the exit's `hints` map from
a layer's rows, `{:by-value #{k ...} :opaque #{k ...} :no-copy #{k ...}}`,
merged with `reads/store-hints`, the compiled hints of the store's own keys
(`:no-copy #{:read/point :read/pattern}`). Every exit function that takes
`hints` today takes this map instead: zero change inside the index kinds,
which is what the read exit designed them for (P:915-916).

A grammar that would change a key's `:index` or `:opaque` in a layer that
already holds a fact under that key is refused
**`:grammar-change-needs-rebuild`**, a new code, placed right after
`:control-not-allowed`. The layer's indexes for that key were written under
the old hints, and a changed hint would leave them wrong without a rebuild
(P:916-918). The first grammar for a key counts as a change from the
defaults (`#{}`, not opaque). The row says whether the layer holds a fact
under the key (`:used`, 4.7), so the check costs no read beyond the row. The
road that admits such a change and rebuilds the key's entries is later work
(section 12, Q5).

### 4.6 A key with no grammar

A key with no grammar in the layer is permissive: any value the floor
admits, no subjects beyond owner and tool, not opaque, no index hints.
That is IMPLICIT_SPEC OP16's derived edge case ("its values name no
subjects beyond owner and tool and are not opaque ... the model's `:note`")
and today's behaviour, so every earlier test that writes no grammar keeps
its answers (*derived*; the build confirms by running the suites). It has a
cost, stated plainly: a value about Bob under a key with no grammar in the
layer is not recorded as about Bob, and Bob's forget does not reach it even
when it is marked `:die-with-any`. Strict (refuse a key with no grammar) is
the other end; it would make every layer state its keys before use. A rig
choice (T-RC2); Sid's question Q4.

### 4.7 The row, and what the gate does with each part

The row under `$$layers [L :keys k]` (stream) and `$$micro [L :keys k]`
(micro):

```clojure
(fixed-keys-schema
 {:used    Boolean                         ; the layer holds a fact under k
  :grammar (fixed-keys-schema
            {:fid         clojure.lang.PersistentVector   ; the grammar fact [name idx]
             :stamp       Long                             ; its act's stamp
             :shape       clojure.lang.PersistentVector
             :subjects-at clojure.lang.PersistentVector   ; nil when none
             :opaque      Boolean
             :index       (set-schema clojure.lang.Keyword)})})  ; nil :grammar: no grammar
```

Written in the decision event (stream) or batch (micro) for a yes only,
each a whole-row set of a value computed from the row read in the same
event, so a replay writes the same row (phase 1's I-G2, *derived*): a
grammar fact for k sets `:grammar`; the first admitted fact under k in L
sets `:used true`. Store keys get no row.

The gate's steps with grammar, in phase 2's order (L27, LP:1821-1823), new
parts in bold:

1. Faces, unrecorded: stage 1's structural faces and `:not-sealed`; the
   record path; `:no-such-lock` at the delivery.
2. Reads before the decision, on the home task: settings, `$$persons
   [owner]`, **the `:keys` rows of the act's distinct keys, and of the key
   each grammar fact governs**, the delivery.
3. `read-values`, per value fact: open, `edn-value?`; **if the key's row has
   a grammar and it is not opaque, `(shape/check shape v)`, failing →
   `:value-shape`**; subjects by `subjects-of` over **the rows' grammars**
   in place of the constant; the reasons are recorded at step 5.
4. Stage 1's reads (permission rows, heads), the wrap persons.
5. `decide`, recorded reasons in order: `:fact-outside-the-acts-layer`,
   `:no-such-layer`, `:class-mismatch`, the four permission checks,
   `:malformed-control` (**now also a grammar fact failing `grammar/parse`,
   a store key's grammar, or two grammar facts for one key**),
   `:control-not-allowed` (**also a grammar fact by neither the operator nor
   the owner**), **`:grammar-change-needs-rebuild`**, `:stale-replaces`,
   `:stale-revoke`, `:layer-already-made`, `:unsupported-reclass`; then
   phase 2's `:does-not-open`, `:malformed-value`, `:value-shape`,
   `:too-many-subjects`, `:grain-mismatch`, `:no-such-person`,
   `:person-forgotten`, `:person-already-made`, `:no-such-value`.
6. Writes for a yes: phase 2's and the read exit's, with
   `reads/index-writes` given **`(grammar/hints rows)`**, and **the changed
   `:keys` rows**.

Every step is total; a refusal is an answer, recorded or on the face, never
a throw (rule 9; phase 0 finding 3).

A value is checked under the grammar in force before its act: an act that
writes a grammar for k and a value under k checks the value under the
previous grammar (or none). The simplest rule, and the one that keeps the
grammar a value was checked under derivable (T-FR3).
