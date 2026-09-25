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

## 5. The tool fact and tonight's vocabulary (question 3)

### 5.1 Its form (first-record, T-FR4)

```clojure
{:e :mention-count                         ; the tool's id, and the actor it acts as
 :k :tool                                  ; a store key the runner knows
 :v {:matches    [:k :mention]             ; a read exit pattern, one of its six forms (P:145-152)
     :signature  {:in    :match            ; tonight's one input: one matched fact per run
                  :out   #{:note}          ; the keys its outputs may write
                  :rows? false}            ; read-entry preference: the short line (default) or exact rows
     :permission [:mention-count :alice :alice]   ; the pid it acts under, a grant in this layer
     :recipe     [{:name :count-note
                   :do   :emit
                   :e    [:in :e]
                   :k    :note
                   :v    [:map {:token [:str [:count [:in :v :persons]] " named"]}]}]}}
```

- **What it matches**: a pattern the read exit already reads, validated by
  the exit's own parser at the tool's first match read; a pattern the exit
  refuses (`:bad-pattern`, `:not-indexed`, `:opaque`) comes back in the
  runner's report as data. Tonight a tool matches in the layer it lives in.
- **Its signature**: `:in :match` (the recipe sees one matched fact, its
  `:e :k :v :fid :stamp`); `:out`, the keys its outputs may write, which the
  executor enforces (an `:emit` of another key refuses the run) and the loop
  check reads (6.4); `:rows?`, the read-entry preference of ruling 3 ("a
  deterministic tool gets the short entry by default and may ask for exact
  rows in its signature", lines 76-83), which the runner passes to the exit
  as its `:rows?` parameter (P:243-245, "phase 6 makes it a fact on the
  tool, tonight a parameter").
- **The actor and the permission it acts under**: the tool acts as its own
  id, under `:permission`, a pid `[who layer in]` (`envelope.clj:265-268`)
  whose `who` is the tool's id and whose `layer` and `in` are the tool's
  layer. The grant is a fact in that layer, as every permission is
  (PROGRESS.md 159-161). Default 5 puts tools "narrower still", beneath the
  session's permission, and a revoke cuts everything below it; the
  permission cascade that gives a grant a parent is phase 3's (MP:444-448,
  MP:508-518). Tonight the grant is a plain operator grant (phase 1's
  `grant-offer`, P8); when the cascade lands, the tool's grant names the
  session's pid in the layer as its parent, and the tool fact does not
  change (T-RC7).
- **Its recipe**: a vector of at most 16 named steps, run in order, once
  each, per match. A step is `{:name n :do capability ...arguments}`; every
  argument is a formula (5.2) or a literal where the capability says so.
- A tool fact is an ordinary value fact, not a control fact: the gate never
  reads it (only the runner does, through the exit), so it is sealed at the
  door like any value and forgettable like any value (phase 2). Its shape
  can be given by a grammar fact for `:tool` in the layer like any key's;
  the runner's own parser, `recipe/parse-tool`, is the authority on whether
  a tool can run, and a tool it cannot parse is skipped as data.

"Nothing in a record is a program" (decisions.md 172): a recipe names
capabilities and holds formulas in a closed total language. No step runs
code the record supplies; no formula refers to a var, a namespace or a
function outside the vocabulary.

### 5.2 Tonight's vocabulary (the seed's list for tonight)

Steps, each a capability (class c of the count):

| Step `:do` | Arguments | Gives | Why tonight |
|---|---|---|---|
| `:emit` | `:e` formula, `:k` a literal key in `:out`, `:v` formula | one output fact, collected into the run's one output act | the test tool writes a `:note` |
| `:revision/read-units` | `:repo`, `:rev`, `:path` formulas, `:cut` literal (`:blocks`, `:forms` or absent) | `rig.revision/read-units`'s result: `{:units [...]}` or an error as data, bound to the step's `:name` | the revision reader, consumed as a capability (its plan, lines 196-238); no test tool calls it tonight |
| `:revision/read-span` | `:repo`, `:rev`, `:path`, `:first`, `:last` formulas | `rig.revision/read-span`'s result, bound to the step's `:name` | as above |

Formulas, in the leaves (class c):

| Formula | Value |
|---|---|
| `[:lit x]` | the plain value x |
| `[:in part & path]` | a part of the matched fact (`:e`, `:k`, `:v`, `:fid`, `:stamp`); with a path, into `:v` |
| `[:got step-name & path]` | an earlier step's result, and a path into it |
| `[:count f]` | the element count of a collection; nil otherwise |
| `[:str f ...]` | the concatenated string forms (`str` of each value) |
| `[:map {k f ...}]` | a map from literal keywords to formula values |

Totality (*derived*): at most 16 steps, each run once; formulas at most 8
deep and 256 nodes; no loop, no recursion, no reference but `[:in]` and
`[:got]` (to earlier steps only, checked at parse); every function total
(a path into a missing place is nil; `[:count]` of a non-collection is nil).
A capability returns data or an error as data. A failed step stops the run:
the report says `{:refused :step-failed :step n :error e}` and nothing is
offered. Every output still meets the gate: a formula that yields a value
its key's grammar refuses gets `:value-shape`, as any offer would.

Not in tonight's vocabulary, named so the count can see them come: a step
that maps over a collection (one output per unit a revision read returns),
a step that reads the store (a second pattern read, role `:stood-on`), a
comparison or a conditional. The reference tool is likely to need the
first two (*assumed*); round three's contract says.

## 6. The minimal runner (question 4)

### 6.1 Where it runs, and why not a topology

`rig.store.runner`, plain Clojure beside `rig.store.client` (the door) and
`rig.store.read-exit` (the exit), operator code as the exit is (P:800-802);
a kept store moves it behind the server with them. Not a topology, because
(*derived*): (1) its reads must go through the one exit (RIG.md default 4,
"one exit for every read"), and the exit queries, offers an entry and waits
for its answer before it shows anything, which topology code cannot do
(a topology cannot block on an offer's answer); (2) its outputs are offers
through the door, which seals them under leased locks at the edge (phase
2, default 1), and the door is client code; (3) a tool runs in its layer,
so a topology would have to route to that layer anyway. It is the executor's
first runner, of the several decisions.md 166-169 expects.

### 6.2 One pass: `(run-pass! store {:layer L :limit n})`

1. **Find the tools by matching.** `read!` the pattern `[:k :tool]` in L
   (served by `:ix-ke`, which the read exit kept for this, P:1186-1187), as
   the operator: `:reader :operator`, `:reader-kind :tool`, `:rows? false`,
   `:role :stood-on` (the runner stands on the tool facts to run them),
   `:for` L's owner (from `client/settings`), `:working L`, `:permission`
   nil (the operator acts at the root, `gate.clj:27-32`), `:limit n`. No
   tool is known to compiled code: every tool the runner runs is found here.
2. **Parse each tool** with `recipe/parse-tool`, total; a tool it cannot
   parse is `{:tool fid :refused :malformed-tool}` in the report.
3. **Refuse loops** (6.4): the tools that would feed themselves are
   `{:tool fid :refused :tool-loop}`, and do not run.
4. **For each remaining tool, in the order of its fact's stamp:**
   a. `read!` its `:matches` in L as the tool: `:reader` the tool's id,
      `:reader-kind :tool`, `:rows?` from its signature, `:role :matched`,
      `:for` L's owner, `:working L`, `:permission` the tool's pid, `:limit
      n`. The exit records one line: pattern, moment, role, fingerprint,
      mark; the exact list only when the signature asks (P:272-277). A
      refused read is data in the report.
   b. For each matched row that shows a value (an erased or unreadable row
      is skipped: a forgotten value is not run on), evaluate the recipe
      over it, `(recipe/run tool row)`: output facts, or a refusal as data.
   c. Build the output act: `:who` the tool's id, `:layer` L, `:class
      :by-layer`, `:permission` the tool's pid, `:session` nil, `:stood-on`
      `{matched-fid matched-stamp, tool-fid tool-stamp}` (based-on as the
      fact ids it stood on with their stamps, CONCLUSION R4), `:because-of`
      the matched fact's act name (ruling 3: "trigger is already
      because-of"; the envelope's `:because-of` is a name,
      `envelope.clj:317`), `:subjects #{}` (the tool names none, as in the
      model), `:facts` the outputs, and the name `(run-name L tool-fid
      matched-fid)` (6.3).
   d. Ask the door for that name's answer first (`client/lookup store name
      nil`, the answer lookup by name plus layer that resends use,
      `client.clj:53-66`): an answer means this match already ran, and the
      report says `:recorded`. Otherwise offer it with
      `client/offer-until-answered!` (sealed at the door once phase 2 is
      merged) and report the answer, yes or a refusal, as data.
5. Return the report: for each tool, its read entry's name, its matches,
   and each run's answer.

The runner holds no state between passes. It never throws on a tool, a
match, a refusal or a capability's error; each is a line in the report
(rule 9 applied to the runner; *derived*).

### 6.3 Once per match: the run's name

`(run-name L tool-fid matched-fid)` is `[L :by-layer :offer id]` where id
is a version 8 UUID made from the first 128 bits of SHA-256 over the
canonical text of `[L tool-fid matched-fid]` (`env/canonical`). A second
pass, a runner restarted mid-pass, or two runners at once all offer the
same name with the same content for the same match, so the gate answers
the later ones from the record (phase 1's record path; phase 2 keeps an
honest resend's answer, its F1), and one output act exists per tool fact
and match, for ever (*derived*; IMPLICIT_SPEC OP18 derived the same: "the
names fixed in advance make repeats duplicates"). A tool fact replaced by a
new one has a new fid, so the new version runs over the layer's existing
matches too: what a new tool owes history is open item 85, and this is the
simplest behaviour, not a ruling. The name is deterministic, not random;
"names: random, made by the offerer before the gate" (PROGRESS.md line 57)
and the landing precedent (names derived "from the request's name under a
scheme reserved to the store", lines 128-130) are both near it, and it is
Sid's question Q6. A writer in L who computes a run's name could offer
other content under it first, so the run is refused `:name-taken`; tonight
the writers in a one-owner layer are its owner and her sessions, and a
store-held secret in the derivation would close it (T-RC6).

### 6.4 What stops a tool from triggering itself for ever

A static check over the layer's tools, before any runs, in
`recipe/loop-free`, pure and total (*derived* as sufficient within one
layer):

- A tool whose pattern matches every key (`[:all]`, `[:e e]`) is refused
  `:tool-loop`: it would match its own outputs.
- A tool whose pattern's key is one of its own `:out` keys is refused.
- Tools are added to a graph from matched key to output keys in the order
  of their facts' stamps; a tool whose edges would close a cycle is
  refused, and the tools before it keep running. So adding a tool never
  stops one that ran before.

Within one layer, with every tool's outputs in its own layer, an acyclic
graph means every chain of runs is at most as long as the number of tools,
and each (tool, match) runs once (6.3), so a sequence of passes over a
fixed set of facts ends (*derived*). A pass is bounded besides: one read
per tool, at most `n` matches, at most 16 outputs per run. The key-level
graph is coarser than it could be (a tool matching `[:kv :note x]` and
writing `:note y` does not loop, and is refused); a finer check is later
work. A tool writing into another layer, where the loop could close across
layers, is not possible tonight (the executor writes into the tool's own
layer only).

### 6.5 What a pass costs

For a layer with T tools and M matches per tool, one pass: T + 1 pattern
reads through the exit, each a query on L's home (one seek plus the
entries it iterates, P:770-782) and one entry act through the gate; then
T × M name lookups (one seek each) and one offer per new match. The
lookups make an old match cost one seek rather than an offer. The pass is
O(T × M) in lookups because the pattern language has no lower bound on the
stamp: the fix, when a real layer needs it, is a "since" bound on a pattern
or a standing read per tool (R6, not built), each a later fixed-side step.
Tonight's layers hold a handful of facts.

## 7. The proof (question 5): tests

All in the rig's test folder, run by `clojure -M:test <ns> ...` from the
rig folder, the cluster ones under `flock
/mnt/data/projects/rig-relay-2026-09-26/cluster.lock`. No test reads or
loads `test/rig/store/gate_test.clj`.

### 7.1 Pure tests, no cluster

`rig.store.shape-test`: every shape form accepts and refuses what the table
in 4.2 says; bounds at admission (depth 9, 257 nodes, 9 `[:or]` branches,
65 enum values refused); the budget (nested `[:or]` over a wide value stops
at 65,536 visits with `:budget`); property tests (test.check, already in
`:test`) that `grammar/parse` and `shape/check` never throw on any value the
envelope's generator makes (`envelope_test.clj:128` has the generator).

`rig.store.grammar-test`: `grammar/parse` refuses an opaque grammar with a
shape other than `[:any]`, with `:subjects-at`, with `:by-value`; a
`:subjects-at` path through an optional entry, or ending on a scalar, or on
a collection with `max` above 256; a grammar on a store key. `grammar/hints`
builds the exit's map from rows and merges `reads/store-hints`.

`rig.store.recipe-test`: `parse-tool` refuses unknown steps, unknown
formulas, forward `[:got]`, an `:emit` of a key outside `:out`, 17 steps,
a formula 9 deep; `run` evaluates the test tool over a matched row to
`{:e :e1 :k :note :v {:token "1 named"}}`; a capability error stops the run
as data; `loop-free` refuses self-matching, match-all and cycle-closing
tools in stamp order; `run-name` is the same for the same triple and
differs when any part differs. When the revision reader is merged: a step
`:revision/read-units` over the reader's own fixture repository binds its
units, and an error from the reader comes back as data.

### 7.2 Cluster tests: `rig.store.tools-test`

Fixture: a module on 4 tasks (the suites' usual size, *assumed*), the
model's world by `client/seed!`, then one operator act per one-owner layer
writing the **toy grammars** as facts:
`{:e :mention :k :grammar :v {:shape [:map {:persons [:set-of [:keyword] 1
2]} {:open? true}] :subjects-at [:persons] :opaque false :index #{}}}` and
`{:e :note :k :grammar :v {:shape [:any] :subjects-at nil :opaque false
:index #{:by-value}}}`. The `:mention` grammar is the proof's test grammar;
the pair is what today's compiled knowledge becomes (section 2).

Gate:

- **G1, a value refused by its key's grammar, as data.** In `:alice`, a
  `:mention` of three people, and one whose `:persons` is a string: each
  answered `{:answer :no :reason :value-shape}`, recorded (the lookup by
  name finds the record); then a well-formed `:mention` is admitted, so the
  worker lived. The same values in a layer with no grammar are admitted
  (the permissive default, 4.6).
- **G2, subjects taken from the grammar reaching the value's wrap.** In
  `:alice`, a `:mention` of Bob marked `:die-with-any`: the answer record's
  `:subjects` is `#{:alice :bob}` and the lock record's wrap is `{:required
  [:alice :bob]}`. Then a grammar fact for `:mention` with no
  `:subjects-at` (same shape, index and opaque, so admitted though the key
  is used), and a second marked `:mention` of Bob: `:subjects #{:alice}`,
  wrap `{:required [:alice]}`. After Bob's forget, the first no longer
  opens and the second still does. The subjects came from facts.
- **G3, an opaque key.** In `:alice`, a grammar for `:blob`, `:opaque true`:
  a `:blob` whose value is a string, and one that is a map, are both
  admitted (no shape check); `[:kv :blob v]` is refused `:opaque`; no
  `:ix-kv` entry exists under the `:blob` prefix (read from `$$layers`);
  `[:k :blob]` shows both rows with `:opaque true`; grammars for `:blob2`
  that are opaque with `:subjects-at`, or with `:by-value`, are refused
  `:malformed-control`.
- **G4, index hints read from grammar facts.** In `:alice`, `[:kv :note
  {:token "x"}]` answers the `:note` written with that value; `[:kv :mention
  ...]` is refused `:not-indexed`. In `:alice-hand`, whose `:note` grammar
  is rewritten before any `:note` without `:by-value`, `[:kv :note ...]` is
  refused `:not-indexed`. A grammar adding `:by-value` to `:mention` in
  `:alice`, which holds mentions, is refused `:grammar-change-needs-rebuild`;
  the same fact in `:alice-agent`, which holds none, is admitted, and
  `[:kv :mention v]` then answers there.
- **G5, grammar admission.** A shape outside the language, a grammar on
  `:permission`, and two grammar facts for `:note` in one act: each refused
  `:malformed-control`. A grammar offered by the test tool's actor, which
  holds a permission in `:alice` but is neither the operator nor the owner:
  `:control-not-allowed`.
- **G6, checked under the grammar before its act.** One act holding a
  stricter `:mention` grammar (`[:set-of [:keyword] 1 1]`) and a `:mention`
  of two people is admitted; the next act's `:mention` of two people is
  refused `:value-shape`.

Runner:

- **R1, the runner finds the test tool by matching and runs it once per
  match.** The operator writes the test tool (5.1) and its grant in
  `:alice`: facts only. Alice writes `:mention`s of Bob on `:e1` and of
  Alice and Bob on `:e2`. `run-pass!` reports the tool found by the `[:k
  :tool]` read and two runs answered yes; `[:k :note]` in `:alice` then
  shows `{:token "1 named"}` on `:e1` and `{:token "2 named"}` on `:e2`, each
  by `:mention-count`.
- **R2, once per match.** A second `run-pass!` reports both runs
  `:recorded` and adds no fact. After a third `:mention`, the next pass runs
  exactly one.
- **R3, its reads recorded with its preference.** R1's pass left, in
  `:alice`, a `:read/pattern` line by `:mention-count` with `:role
  :matched`, `:pattern [:k :mention]`, `:count 2` and no `:exact`; and the
  operator's line for `[:k :tool]` with `:role :stood-on`. A second tool
  whose signature says `:rows? true` leaves a line whose `:exact` holds the
  matched `[fid stamp]` pairs.
- **R4, its outputs admitted under its permission with based-on.** Each
  output's answer record has `:who :mention-count`, `:permission
  [:mention-count :alice :alice]` and `:because-of` the matched act's name;
  `$$layers [:alice :stood-on name]` holds the matched fid and the tool's
  fid with their stamps; the output's stamp is later than both. After the
  operator revokes the tool's grant, a new `:mention`'s run is refused
  `:permission-revoked`, recorded, and the pass returns it as data.
- **R5, a loop refused or bounded.** A tool matching `[:k :note]` that
  writes `:note` is refused `:tool-loop` and leaves no output and no read
  entry; with tool A (`:mention` to `:note`) written before tool B (`:note`
  to `:mention`), A runs and B is refused; a tool matching `[:all]` is
  refused.

The count's receipt is the build's, not a test: the test tool and grammar
are EDN data in `tools_test.clj`, written through `client/offer-until-
answered!`, and adding them needed no change under `src/` beyond the steps
section 8 lists.

## 8. The machinery count, predicted (question 6)

What counts as one compiled step here: one named code unit added to the
fixed side or to the vocabulary, or one existing unit whose behaviour
changes (*assumed* definition; IMPLICIT_SPEC O21 leaves it open, and the
build records it with the count). Predicted: 11 + 4 + 9 = 24, and one more
in class b if phase 2's leases need a tool road.

**(a) Fixed-side steps the frame already promised (11).**

| # | Step | Where | Promised by |
|---|---|---|---|
| a1 | `:grammar` a control key: `control-fact?`, `control-value-ok?` through `grammar/parse`, `control-allowed?` for the owner, phase 2's unsealed list | `gate.clj`, `locks.clj` | the grammar fact in the seed (CONCLUSION 115); "grammars at the gate" (PROGRESS "Next") |
| a2 | the `:keys` rows written in the decision event | `module.clj`, `gate/decide*` | the same |
| a3 | the stream gate reads the act's key rows in its one event | `module.clj` step 4 | the same |
| a4 | the micro gate reads and writes the rows in its layer visit | the micro module | the same, for the second gate (ruling 1) |
| a5 | the shape language and its checker | `rig.store.shape`, `grammar/parse` | ruling 6 "shape check"; the seed's "value shape" |
| a6 | `read-values` checks the shape and takes subjects from the rows | `locks.clj` | ruling 8; LP:1499-1504 ("Stage 6 reads the same map from facts") |
| a7 | opaque: no shape, no value subjects, `:opaque` in the hints | `locks.clj`, `grammar.clj` | ruling 6 |
| a8 | index hints from the rows, in place of `reads/seed-hints`, at the three places that take hints | `gate.clj`, `reads.clj` | the read exit (P:915-916); CONCLUSION lane B ("Index kinds take the grammar's index hints as parameters") |
| a9 | "shown as opaque": the exit reads the rows of the keys it shows and marks rows | `reads.clj` (`read-point`, `read-pattern`), `open-row>` | ruling 6; P:913-915 |
| a10 | the recipe executor: `parse-tool`, `run`, the vocabulary table | `rig.store.recipe` | decisions.md 166-169, "one executor with several runners" |
| a11 | the minimal runner, `run-pass!` | `rig.store.runner` | SPEC phase 6, "a minimal runner finds tools by matching" |

The exit itself does not change for tools: the runner passes the tool's
`:reader-kind`, `:rows?` and role as the parameters the exit already takes
(P:242-257, P:1054-1056). Zero steps there.

**(b) Fixed-side steps nobody anticipated (4, perhaps 5).**

| # | Step | Why it was needed |
|---|---|---|
| b1 | `:grammar-change-needs-rebuild`, and the `:used` flag on a key's row | a grammar changing a used key's hints would leave its indexes wrong without a rebuild (P:916-918); nobody planned the refusal or the flag |
| b2 | the run's derived name | once per match with no runner state; the rulings cover acts that name what they cause and derived landing names, not a tool's runs |
| b3 | the loop check | nothing in the frame says what stops a tool feeding itself |
| b4 | the micro gate's block 1 reads the rows before it opens values | phase 3 opens values on the arrival task before any hop (MP:208-223), and the rows live on `hash(L)` |
| b5 (if needed) | a lease road for a tool actor | phase 2 seals a lease row under the lease act's writer, a person, and stores the operator's bare (LP:278-283); a tool is neither. If the runner can lease as the operator for the tools' acts under session nil, this step is not needed; the build finds out when phase 2 is merged |

**(c) Capabilities (9).** `:emit`; `:revision/read-units` and
`:revision/read-span` (the revision reader's two steps, its plan section
12, anticipated since 13 September); and the formula functions `:lit`,
`:in`, `:got`, `:count`, `:str`, `:map`. Each is a built-in a recipe calls.
`:count` and `:str` are here because the test tool uses them; the thesis
count, on a tool nobody tonight knows, will show which of these a second
tool reuses and what it adds.

After these, writing the test tool and the test grammar needs no compiled
step: they are facts (R1, G1 to G6). That is the machinery count's zero,
not the thesis count's.
