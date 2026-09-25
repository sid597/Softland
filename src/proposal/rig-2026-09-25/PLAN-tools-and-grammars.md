# Plan — tools and grammars from inside (rig phase 6)

Planned by Claude Opus 5.5 (`claude-opus-5-5`), effort max, in a fresh
session on 26 September 2026 from 02:41 IST, as the rama skill's plan step
(`references/phase-1-plan.md`, `references/artifact-plan.md`). Not yet
validated; a fresh session validates it next. Branch `rig-plan-tools`,
worktree `/mnt/data/projects/Softland-rig-plan-tools`. No store code was
written. One short probe was run, on the row's schema and a batched answer
lookup (`runs/probes/rig/probe/tools_rows_probe.clj`, output
`runs/phase6-tools-rows-probe.txt`); every other Rama fact below rests on
phase 0's receipts (SPEC.md "What Rama showed") and the validated plans it
cites, and section 13 lists what the build must confirm first.

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
  task by this plan's build. **[V-F1]** The caching examination
  (EXAMINATION-copies.md, 26 September, a fresh adversarial session) settled
  the roads: A is built; the base's grammar copied to every task (C) fails;
  a hop per offer (B) and a person's layers on one task (G) fail; a layer
  adopting another layer's grammar (F), an ordinary grammar fact in the
  layer whose act stands on the source through based-on (R4), is deferred,
  possible later with no new envelope part, and in the examination's
  reading not a copy under the rule. T-FR3 stays first-record. The cost of
  A: every layer states its own grammars, and session layers start with
  none; in a layer with a person owner that changes no unmarked value's forget (4.6,
  [V-F2]). Where a key's grammar has authority is Sid's (Q1).
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
  promised, 5 nobody anticipated built tonight and 1 deferred, 9
  capabilities (section 8, [V-F8]). The build takes the real count.

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
  three" (lines 110-129): the tool fact (line 114), the grammar fact (115),
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

**[V-F10] What was built since** (*checked* by the validation, 26
September 03:40-04:10): the read exit is merged on the rig branch
(`40374171`; byte-identical to `rig-build-reads` at `00fd0468`); phase 2's
build is committed to `5a5de4fe` in `Softland-rig-build-locks`; phase 3's
to `48562bde` and later in `Softland-rig-build-micro`, with work
uncommitted. The names to bind after the merge:

| This plan's name | Built as | Where |
|---|---|---|
| the hint constant and its seam | `reads/seed-hints` (the constant) and `reads/current-hints` (the function "the topology code calls ... rather than embedding the constant"); phase 6 replaces the seam's callers, not only the constant | `reads.clj:34-47`; called at `gate.clj:281` (`decide*`), `reads.clj:426` (`parse-pattern`'s default arity), `reads.clj:975` (a query topology): the three places a8 names |
| the store's read-entry keys | `reads/read-keys`, `#{:read/point :read/pattern}` (FR6) | `reads.clj:68` |
| `open-row>` | `reads/open-row>` (a `deframafn` over the pure `reads/open-row`); phase 2 has its own `locks/open-row>` and `open-value>`, joined behind the read exit's seam at the merge | `reads.clj:331-346`; `locks.clj:1291, 1320` at `5a5de4fe` |
| `read-exit/read!` | as planned; spec keys `:reader :for :reader-kind :rows? :working :permission :layer :read :as-of :limit :role :entry-name`, `:read [:pattern p]`; `reads/reader-kinds` holds `:tool` | `read_exit.clj:48-126`, `reads.clj:72` |
| `reads/parse-pattern`, `reads/index-writes` | as planned: `(parse-pattern pattern limit as-of hints)`, `(index-writes hints layer name log stamp)` | `reads.clj:416-426, 181` |
| `grammar/grammars`, `grammar/subjects-of` | as planned, `(subjects-of grammars k v)` | `grammar.clj:12-29` at `5a5de4fe` (the same file in the micro build) |
| `locks/read-values`, the wrap | `locks/read-values`, `locks/wrap-of` | `locks.clj:418, 253` at `5a5de4fe` |
| the lease road ([V-F6]) | `client/stock!` (uncommitted at 04:00), else `client/lease!` with a pool refresh; `client/default-session`, `locks/person-owner`, `locks/max-lease` | locks build `client.clj:63-67, 293-308` at `5a5de4fe`, `stock!` at 310 in its working copy; `locks.clj:68, 247` |
| the offer builder ([V-F5]) | `client/build`, its `:claimed-when` and `:session` defaults | `client.clj:21-37` on both branches |
| the micro gate's block 1 and fold | `micro/arrival-open` (the open on the arrival task, before the `(|hash *name)` hop at `micro.clj:1160`), `micro/layer-rows` (the `(|hash *layer)` visit's reads, 1171-1204), `micro/prepare` (the pure fold on `(|global)`, 1259-1263), `micro/reason-order` (the refusal order both gates share, 58-70), `rig.store.permit/refusal` (the permission chain) | micro build at `48562bde` |
| the clock's millisecond ([V-F5]) | `clock/ms-of` | `clock.clj:33` |

## 2. Today's compiled knowledge of `:note` and `:mention`, and what becomes a fact

| Where (*checked*) | What it knows | Becomes |
|---|---|---|
| `model.clj:241-248` `fact-subjects` | `(= :mention (:k f))` → the value's `:persons` are subjects | the `:mention` grammar fact's `:subjects-at [:persons]` (the model itself is round three's to extend) |
| Phase 1, `gate.clj:255` | nothing per key: the act's subjects are the carried `:subjects` plus the owner; test E1 (`stream_gate_test.clj:180-197`) checks that Bob is not added from a `:mention` | unchanged in phase 1; phase 2 adds the grammar's |
| Phase 2 plan, `rig.store.grammar/grammars` (LP:178-189) | the constant `{:mention {:subjects-at [:persons]}}`; `subjects-of` refuses `:value-shape` for another shape under `:mention` (L12) | the layer's `:key-rows` rows; `subjects-of` takes the rows' grammars in place of the constant and does not change (LP:1501-1503 anticipated exactly this) |
| Read exit plan, `reads/seed-hints` (P:903-906) | `{:by-value #{:note} :opaque #{} :no-copy #{:read/point :read/pattern}}` | `:by-value` and `:opaque` from the rows' `:index` and `:opaque`; `:no-copy` for the store's own read-entry keys stays compiled (`reads/store-hints`), because store keys are the floor (open item 3) |
| Read exit plan, "shown as opaque" (P:913-915) | not built | a row mark from the key's grammar (section 4.4) |
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
(`module.clj:139-147`, *checked*):

- **Stream store:** `$$layers [L :key-rows k]`, on L's home task. The gate
  reads the rows of the act's distinct keys in the same event, in step 4 of
  phase 2's order beside `:settings` and before `read-values` (LP:1058,
  *checked* as the plan's order), in a `loop<-` like the permission rows
  (`module.clj:103-110`). No partitioner, no hop.
- **Micro store:** `$$micro [L :key-rows k]`, on `hash(L)`, beside the layer's
  other projections there (MP:1290-1292, MP:1745-1748). The micro gate reads
  the rows in the `(|hash *layer)` visit where it reads the layer's settings
  and permission rows (MP:1550-1557), and for a re-classed layer the same
  way it reads settings there, the micro rows first and `$$layers` on a
  miss. One consequence for phase 3's build
  (*derived*): its block 1 opens a sealed value and computes subjects on the
  arrival task before any hop (MP:208-223), and that step now needs the
  rows. The simplest road that keeps plaintext off the wire is to move the
  layer visit before the open: parse, `(|hash *layer)` for settings,
  permissions and key rows, then `(|hash *route-key)` back to the arrival
  task to open, check and drop the plaintext, then the name and entity
  visits as planned. It costs one hop per act in the batch, inside a
  microbatch whose cycle is already at least 300 ms (the skill's figure).
  How to fold it into block 1 is phase 3's; the requirement is that the rows
  reach the open, and that the hints reach block 2a's index writes
  (MP:1580) the same way. **[V-F9]** The road that saves the hop,
  constructed: keep the micro plan's order (arrival, name, `hash(L)`),
  carry the lease rows as read on the arrival task, sealed, to `hash(L)`,
  and unlease and open there with `$$persons` local. Seeks are the same; it
  saves one transfer per act (about 1 ms, *assumed*). It is rejected
  because an operator's lease row is bare (`{:under nil :sealed K}`, L23),
  so every operator act in a shared layer would carry a bare lock across a
  task on the common path, which the micro plan allows only on the rare
  resend path; the choice is phase 3's and, if the rule is to bend, Sid's.
  **[V-F4]** Inside a batch every envelope is checked and indexed under the
  rows read before the batch, so the rebuild check (4.5) must also count
  uses by the batch's other envelopes in L, in either order: the fold
  refuses a hint-changing grammar for k when any envelope of the batch in L
  offers a fact under k (the skeleton keeps every fact's `:k`). The record
  carries `:batch`, the microbatch id that decided it (MP:1150), so "the
  grammar before its batch" is derivable at read time.
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
key; nothing enforces it tonight. And every layer must state its own
grammars. That is cheap for a person's layer, made once, and not for the
layers made in quantity: a hand or agent session layer is made per session,
and under the pick it starts with no grammar, so until its making act (or a
later act) states grammars, a `:mention` of Bob written there does not put
Bob in the act's subjects, a `:die-with-any` mark on it dies only with the
owner and whoever the offer carries, and it gets no shape check and no
hints (the permissive default, 4.6). **[V-F2]** Its unmarked wrap is the
owner alone either way, as with a grammar (`locks/wrap-of`: "unmarked, a
person owner: the owner alone required"; the ruling, "Alice's mention of
Bob in her own layer dies with Alice and survives Bob", PROGRESS.md
140-142), so no unmarked value's forget changes. Giving a new layer its
grammars from another layer is road F below. **[V-F1]** The caching
examination settled the rest: C, B and G fail, F is deferred (3.3). So the
pick is copy-free, tonight and after; the store-wide question left is where
a key's grammar has authority (section 12, Q1 and Q2).

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
second on one single-threaded task that also decides its own layers; even
with every row in the block cache that is more than the task's whole
budget, and at 0.5 ms a seek it could do about 2,000. Weighted seeks per operation stay flat, but the load
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
because phase 1's record path writes nothing (`module.clj:93-97`), so the
record path would have to redo the fan-out, with monotone sets so an old
version never overwrites a newer one; (3) lag: a decision on another task
can use the older version until its copy is written, so the decision must
say which version it used, a record change. Phase 2 already keeps one copy
of this kind, `$$persons` on every task by `|all` (LP:945-952); whether it
went through the caching rule's examination I have not checked. Not taken:
A gives the same local reads with no copy, one writer per layer, and no
record change. **[V-F1] Examined** (EXAMINATION-copies.md 3.8): C does not
survive. It copies mutable current state, needs one writer across two
gates, and an honest decision would record the version it used, so T-FR3
falls. If Sid rules that a store-wide grammar must reach every layer
promptly, the form that survives is the examination's derived index of
immutable grammar versions, each layer naming the version it applies
through an adopting act, not C. (`$$persons` was examined there too: it
survives, as originals replicated.)

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
come in. It differs from C in being a fact (attributed, visible,
time-travelled, retractable) rather than machinery, and in costing one act
per adopting layer per change. **[V-F1] Examined** (EXAMINATION-copies.md
3.3, 3.9, verdict): in the examination's reading F is not a copy under the
rule; it restates an immutable fact, claims no currency, and its drift from
the source is found through its stood-on, like a promotion's landing.
Deferred: not built tonight, and no test needs it; possible later as an
ordinary grammar fact in the layer whose act stands on the source through
based-on (R4), with no new envelope part and nothing the gate checks
against the source.

**G. A person's layers placed on one task, the session layers reading
their owner's grammar there.** If a session layer were placed on its
owner's personal layer's task (the task chosen by owner, each layer still
whole on one task), its gate could read the personal layer's rows locally,
with no copy and no hop, and a session would inherit its owner's grammars.
Costs (*derived*): all of a person's layers share one single-threaded task,
so one person's total write rate, agents included, is bounded by one task
(a hot layer is remedied by re-class, SP:536-537, which moves it off);
`*offers` would be partitioned by the layer's owner, which the offer does
not carry, so the name's tag or a lookup would have to give it (an envelope
or placement change); and "inherits" is itself a new rule. **[V-F1]
Rejected** (EXAMINATION-copies.md 3.6): G fails the yardstick. A person
running 1,000 agents spans about 20 tasks, and the frame promises "many
people and agents work at once, each in their own working copy, without
blocking each other" (README 110, 142); G puts them on one thread, and it
changes the offer's routing, which touches the record.

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
  `gate.clj:273-285`). An act holding two grammar facts for one key is
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
  permission covers it, as it covers a grain switch, `gate.clj:76-84`); in a
  shared layer, which has no person owner, the operator only. Anyone else:
  `:control-not-allowed`. A rig choice (T-RC4); "from inside"
  may later want any writer holding a permission in the layer.

### 4.2 The value shape: a small total language of shapes

Every shape is a vector whose head names its form, and a grammar is data,
never code. A row holds a shape as canonical EDN text, as the log holds
values (`module.clj:46`), because a shape nests and a PState schema has no
recursive type (the skill forbids `Object`):

| Shape | Matches |
|---|---|
| `[:any]` | any value the floor admits (`env/edn-value?`, as today) |
| `[:nil]`, `[:boolean]`, `[:int]`, `[:uuid]` | that scalar (`:int` a long after `env/normalize-value`) |
| `[:keyword]` | a readable keyword (`env/readable-keyword?`) |
| `[:string]`, `[:string max]` | a string, at most `max` characters (max ≤ 1,048,576) |
| `[:enum v ...]` | one of up to 64 literal plain values, compared after normalisation |
| `[:vector-of s min max]`, `[:set-of s min max]` | a vector or set of `min` to `max` elements, each matching `s` (max ≤ 65,536) |
| `[:map {k s ...} opts]` | a map whose entries, each named by a keyword, match their shapes; `opts` `{:optional #{k ...} :open? bool}`: optional entries may be absent; an open map admits other entries unchecked |
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

`:subjects-at` is nil or one path, a vector of entry names (keywords), into the value, to
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
`:control-not-allowed` (in the stream gate's `refusal` and in the micro
gate's `micro/reason-order`). The layer's indexes for that key were written under
the old hints, and a changed hint would leave them wrong without a rebuild
(P:916-918). The first grammar for a key counts as a change from the
defaults (`#{}`, not opaque). The row says whether the layer holds a fact
under the key (`:used`, 4.7), so the check costs no read beyond the row. The
road that admits such a change and rebuilds the key's entries is later work
(section 12, Q5).

**[V-F4]** A use counts from the row and also from the act itself: an act
holding a hint-changing grammar for k and a fact under k is refused,
because the act's index writes take the hints read before it (4.7 step 6)
and the row would then disagree with that fact's entries (an act holding
the first `:note` grammar with `:by-value` and a `:note` value would leave
the value out of `:ix-kv` while the row says `:by-value`, and `[:kv :note
v]` would miss it and call the page complete). In the micro store, where
every envelope of a batch is checked and indexed under the rows read before
the batch, any envelope of the same batch in L offering a fact under k
counts as a use, in either order: `micro/prepare`, the batch's pure fold on
`(|global)`, sees every envelope's fact keys and refuses the grammar.

### 4.6 A key with no grammar

A key with no grammar in the layer is permissive: any value the floor
admits, no subjects beyond owner and tool, not opaque, no index hints.
That is IMPLICIT_SPEC OP16's derived edge case ("its values name no
subjects beyond owner and tool and are not opaque ... the model's `:note`")
and today's behaviour, so every earlier test that writes no grammar keeps
its answers (*derived*; the build confirms by running the suites).
**[V-F2]** What it costs, narrowed by the caching examination (its 3.1): in
a layer with a person owner (a personal or session layer) an unmarked value is wrapped under the owner alone with
or without a grammar (`locks/wrap-of`), so a missing grammar changes no
unmarked value's forget there. It changes the act's subject union (a
`:mention` of Bob is not found by Bob), the shape check, the hints, and a
mark (next paragraph). In a shared layer or the base, with no person owner, it does
change the forget: a promoted copy or a value that lands in a shared layer
with no grammar for its key is about no one and its wrap is empty
(LP:480-481). Strict (refuse a key with no grammar) is the other end; it
would make every layer state its keys before use. A rig choice (T-RC2);
Sid's question Q4.

**[V-F3] A `:die-with-any` mark on a key with no grammar in the layer** is
admitted. Own subjects are owner ∪ carried (phase 2's formula with no
grammar term, LP:190-198), so the wrap's required list is the owner and
whatever the offer's `:subjects` carries; with nothing carried the mark
changes nothing, the value dies only with the owner, and the offerer is
not told. Refusing such a mark would add a recorded reason, which is Sid's
(Q4; the caching examination's question 5). A rig choice (T-RC12); test
G7.

### 4.7 The row, and what the gate does with each part

The row under `$$layers [L :key-rows k]` (stream) and `$$micro [L :key-rows k]`
(micro):

```clojure
(fixed-keys-schema
 {:used    Boolean                         ; the layer holds a fact under k
  :grammar (fixed-keys-schema
            {:fid         clojure.lang.PersistentVector   ; the grammar fact [name idx]
             :stamp       Long                             ; its act's stamp
             :shape       String                           ; canonical EDN, parsed where used
             :subjects-at (vector-schema clojure.lang.Keyword) ; nil when none
             :opaque      Boolean
             :index       (set-schema clojure.lang.Keyword)})})  ; nil :grammar: no grammar
```

[probed] Rama 1.6.0 takes this row under a subindexed map in a per-layer
`fixed-keys-schema`, with `:grammar` nil, `:subjects-at` nil, a vector of
keywords and a set of keywords, and reads each back as written
(`runs/phase6-tools-rows-probe.txt`).

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
   [owner]`, **the `:key-rows` rows of the act's distinct keys, and of the key
   each grammar fact governs**, the delivery.
3. `read-values`, per value fact: open, `edn-value?`; **if the key's row has
   a grammar and it is not opaque, `(shape/check shape v)`, failing →
   `:value-shape`** (a retract, `:v nil`, asserts no value and is not
   checked; phase 2 leaves retracts unsealed, its L14); subjects by `subjects-of` over **the rows' grammars**
   in place of the constant; the reasons are recorded at step 5.
4. Stage 1's reads (permission rows, heads), the wrap persons.
5. `decide`, recorded reasons in order: `:fact-outside-the-acts-layer`,
   `:no-such-layer`, `:class-mismatch`, the four permission checks,
   `:malformed-control` (**now also a grammar fact failing `grammar/parse`,
   a store key's grammar, or two grammar facts for one key**),
   `:control-not-allowed` (**also a grammar fact by neither the operator nor
   the owner**), **`:grammar-change-needs-rebuild`** (the row's `:used`, or a
   fact under the key in the same act, [V-F4]), `:stale-replaces`,
   `:stale-revoke`, `:layer-already-made`, `:unsupported-reclass`; then
   phase 2's `:does-not-open`, `:malformed-value`, `:value-shape`,
   `:too-many-subjects`, `:grain-mismatch`, `:no-such-person`,
   `:person-forgotten`, `:person-already-made`, `:no-such-value`.
6. Writes for a yes: phase 2's and the read exit's, with
   `reads/index-writes` given **`(grammar/hints rows)`**, and **the changed
   `:key-rows` rows**.

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
 :k :tool                                  ; an ordinary key the runner reads by name [V-F11]
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
  **[V-F11]** `:tool` is not a store key: the store keys are the control
  keys, `:grammar` and the read-entry keys (`reads/read-keys`,
  `#{:read/point :read/pattern}`). **[V-F7]** `parse-tool` refuses
  (`:malformed-tool`) an `:out` holding a store key or `:tool`: tonight no
  tool writes control facts, read entries or tools.

"Nothing in a record is a program" (decisions.md 172): a recipe names
capabilities and holds formulas in a closed total language. No step runs
code the record supplies; no formula refers to a var, a namespace or a
function outside the vocabulary.

### 5.2 Tonight's vocabulary (the seed's list for tonight)

Steps, each a capability (class c of the count):

| Step `:do` | Arguments | Gives | Why tonight |
|---|---|---|---|
| `:emit` | `:e` formula, `:k` a literal key in `:out`, `:v` formula | one output fact, collected into the run's one output act | the test tool writes a `:note` |
| `:revision/read-units` | `:repo` a literal repository id **[V-F13]**, `:rev`, `:path` formulas, `:cut` literal (`:blocks`, `:forms` or absent) | `rig.revision/read-units`'s result: `{:units [...]}` or an error as data, bound to the step's `:name` | the revision reader, consumed as a capability (its plan, lines 196-238); no test tool calls it tonight |
| `:revision/read-span` | `:repo` a literal repository id **[V-F13]**, `:rev`, `:path`, `:first`, `:last` formulas | `rig.revision/read-span`'s result, bound to the step's `:name` | as above |

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

**[V-F13] What a capability may reach.** The revision reader runs git
plumbing on the host the runner runs on, and the runner is operator code.
With `:repo` a formula, any writer of a tool fact in any layer could have
the runner read any file at any revision of any repository the process can
open, and `:emit` its content into the store: a record choosing what the
operator's machine reads. So `:repo` is a literal id, and the runner is
started with the map from id to path it may read (`{:repos {id path}}`,
operator configuration, not a fact; empty by default, so tonight's runner
reads no repository unless a test gives it the reader's fixture). `:rev`
and `:path` stay formulas, bounded by the reader's own checks (its plan's
hardening against `^HEAD`, an inherited `GIT_DIR`, non-ASCII paths). Which
repositories a layer's tools may read is a policy question for Sid (a fact
on the layer, or the operator's list): section 12, Q10.

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
   nil (the operator acts at the root, `gate.clj:28-32`), `:limit n`. No
   tool is known to compiled code: every tool the runner runs is found here.
2. **Parse each tool** with `recipe/parse-tool`, total; a tool it cannot
   parse is `{:tool fid :refused :malformed-tool}` in the report.
3. **Refuse loops** (6.4): the tools that would feed themselves are
   `{:tool fid :refused :tool-loop}`, and do not run.
4. **For each remaining tool, in the order of its fact's stamp:**
   0. **[V-F6] Lease for the tool, as the operator.** Once phase 2 is
      merged every value fact is sealed at the door, read entries included
      (LP:677-679), and the door leases through `lease-for!`, "a lease act
      by its writer"; the gate seals a lease row under `(locks/person-owner
      (:who offer))` and refuses a lease by an actor with no `$$persons`
      entry `:no-such-person` (`lease-writes`, `persons-refusal`, locks
      build at `5a5de4fe`). A tool is not a person, so its own lease is
      refused. The runner therefore leases as the operator into the tool's
      own door session and takes the locks into the door's pool:
      `(client/stock! store :operator L (client/default-session tool-id)
      k)`, with k = 1 for the tool's read entry plus the run outputs it
      will offer (at most 256 a lease act, `locks/max-lease`; more acts
      when more). `stock!` is "Lease `n` locks for `session` in `layer` by
      `who` and take them into the door's pool, so the acts that follow in
      that session need no lease (... a caller that leases ahead)": in the
      locks build's working copy at 04:00, not yet committed. If it does
      not land, `client/lease!` (committed) makes the rows but the door
      takes them into its pool only through the private `refresh!`, which
      `assign!` calls only when the layer's grain is not yet known; then a
      public refresh is one door step, class b. The lease rows are bare, as
      every operator lease is (L23), and consumed at decision; the gate
      matches a cited lock to the offer's `:session` and layer only (the
      lease row holds `{:under :sealed}`, no writer). The tool's reads and
      outputs carry that session. If the pool runs short the door falls
      back to `lease-for!` as the tool, which is recorded as a refused
      lease (`:no-such-person`) and leaves the act refused `:no-such-lock`
      on its face, so the runner stocks exactly what it will cite. No
      store code; a step of the runner (a11). When tools run beneath a
      person's session (R7), the session owner leases and her lock seals
      the rows (L23), and this step goes. The build confirms it by a probe
      on the merge (13.2).
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
      :by-layer`, `:permission` the tool's pid, `:session` the tool's door
      session `(client/default-session tool-id)` ([V-F6]; `client/build`
      would give it anyway to an offer with value facts and no session),
      `:stood-on`
      `{matched-fid matched-stamp, tool-fid tool-stamp}` (based-on as the
      fact ids it stood on with their stamps, CONCLUSION R4), `:because-of`
      the matched fact's act name (ruling 3: "trigger is already
      because-of"; the envelope's `:because-of` is a name,
      `envelope.clj:317`), `:subjects #{}` (the tool names none, as in the
      model), **[V-F5]** `:claimed-when` the millisecond of the later of
      the two stood-on stamps (`clock/ms-of`), passed to `client/build`
      because its default is the wall clock and the parts digest covers it
      ("the digest covers `:claimed-when` and `:stood-on`",
      `client.clj:21-37`), `:facts` the outputs, and the name `(run-name L
      tool-fid matched-fid)` (6.3). Every part is then a function of (L,
      tool fact, matched fact).
   d. Ask the door for the answers under all of this tool's run names at
      once (`client/lookup-many store L names`, one `foreign-select` on
      L's home, the batch form of the answer lookup by name plus layer that
      resends use, `client.clj:53-66`): an answer means that match already
      ran, and the report says `:recorded`. Offer each of the others with
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
same name with the same content for the same match (the same content
because every part is derived, `:claimed-when` and `:session` included,
[V-F5]; with the door's wall-clock default the second of two runners
would be refused `:name-taken` on its face), so the gate answers
the later ones from the record (phase 1's record path; phase 2 keeps an
honest resend's answer, its F1), and one output act exists per tool fact
and match, for ever (*derived*; IMPLICIT_SPEC OP18 derived the same: "the
names fixed in advance make repeats duplicates"). A tool fact replaced by a
new one has a new fid, so the new version runs over the layer's existing
matches too: what a new tool owes history is open item 85, and this is the
simplest behaviour, not a ruling. The name is deterministic, not random;
"names: random, made by the offerer before the gate" (PROGRESS.md line 58)
and the landing precedent (names derived "from the request's name under a
scheme reserved to the store", lines 130-131) are both near it, and it is
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
- **[V-F7]** A tool whose pattern names a store key (the control keys,
  `:grammar`, `reads/read-keys`) is refused `:tool-loop`. The runner writes
  into L on every pass itself: one `:read/pattern` fact for its `[:k
  :tool]` read and one for each tool's match read, and a `:lease` act for
  each tool it runs ([V-F6]). A tool matching
  `[:k :read/pattern]` and writing `:note` passes the key graph, yet each
  pass finds the previous pass's entries as new matches and runs on them,
  one generation a pass for ever. So every tool's edges include the
  read-entry keys, which the refusal expresses.
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

For a layer with T tools and M matches per tool, one pass: T operator
lease acts for the tools that run ([V-F6]; more when a tool cites more
than 256 locks), T + 1 pattern reads through the exit, each a query on L's home (one seek plus the
entries it iterates, P:770-782) and one entry act through the gate; then
T lookups, each one roundtrip for a tool's M names (one seek a name), and
one offer per new match. The lookups make an old match cost one seek
rather than an offer. A tool with more than `n` matches gets a read marked
`:partial` and runs on the first `n` only, and every later pass reads the
same first page: the pattern read takes no "start after" (its inputs,
P:712), so the report marks the tool partial and the rest waits for
paging, a later fixed-side step. The pass is O(T × M) in seeks because the
pattern language has no lower bound on the stamp: the fix, when a real
layer needs it, is a "since" bound on a pattern or a standing read per tool
(R6, not built), each a later fixed-side step.
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
a formula 9 deep, an `:out` holding `:grammar`, `:read/pattern` or `:tool`
([V-F7]); `run` evaluates the test tool over a matched row to
`{:e :e1 :k :note :v {:token "1 named"}}`; a capability error stops the run
as data; `loop-free` refuses self-matching, match-all and cycle-closing
tools in stamp order; `run-name` is the same for the same triple and
differs when any part differs. When the revision reader is merged: a step
`:revision/read-units` over the reader's own fixture repository binds its
units, and an error from the reader comes back as data. **[V-F13]** A step
naming a repository id the runner was not given (`{:repos {id path}}`) is
`{:refused :step-failed ...}` as data and reads nothing; `parse-tool`
refuses a `:repo` that is not a literal keyword.

### 7.2 Cluster tests: `rig.store.tools-test`

Fixture: a module on the suites' task count (phase 1's ran on 8), the
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
- **G3, an opaque key.** A test key, not a toy key ([V-F11]: G2, G4 and G6
  do change a toy key's grammar, each in one layer and on purpose), so each toy key keeps
  one grammar in every layer (CORNERS C4.3). In `:alice`, a grammar for
  `:blob`, `:opaque true`:
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
  `[:kv :mention v]` then answers there. **[V-F4]** In `:alice-hand`, which
  holds no mention, one act holding a grammar adding `:by-value` to
  `:mention` and a `:mention` value is refused
  `:grammar-change-needs-rebuild`; the grammar alone, then the value in a
  later act, is admitted and `[:kv :mention v]` answers it. When phase 3 is
  built, the same pair as two envelopes of one micro batch, in each order,
  refuses the grammar.
- **G5, grammar admission.** A shape outside the language, a grammar on
  `:permission`, and two grammar facts for `:note` in one act: each refused
  `:malformed-control`. A grammar offered by the test tool's actor, which
  holds a permission in `:alice` but is neither the operator nor the owner:
  `:control-not-allowed`.
- **G6, checked under the grammar before its act.** One act holding a
  stricter `:mention` grammar (`[:set-of [:keyword] 1 1]`) and a `:mention`
  of two people is admitted; the next act's `:mention` of two people is
  refused `:value-shape`.
- **G7, a `:die-with-any` mark on a key with no grammar ([V-F3]).** In a
  one-owner test layer made with no `:mention` grammar, a `:mention` of Bob
  marked `:die-with-any` is admitted with `:subjects #{:alice}` and wrap
  `{:required [:alice]}`, and still opens after Bob's forget; the same with
  `:subjects #{:bob}` carried has wrap `{:required [:alice :bob]}` and no
  longer opens after Bob's forget. An unmarked `:mention` of Bob there has
  wrap `{:required [:alice]}`, as it has in `:alice` with the grammar
  ([V-F2]).

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
  operator revokes the tool's grant, the tool's next match read is refused:
  its entry act, offered under the revoked pid, is refused
  `:permission-revoked`, and a refused entry hides the answer (the exit's
  T3, P:1321-1324), so nothing runs and the pass returns the refusal as
  data. An output offered
  under the revoked pid directly (the test builds one by hand) is refused
  `:permission-revoked` and recorded.
- **R5, a loop refused or bounded.** A tool matching `[:k :note]` that
  writes `:note` is refused `:tool-loop` and leaves no output and no read
  entry; with tool A (`:mention` to `:note`) written before tool B (`:note`
  to `:mention`), A runs and B is refused; a tool matching `[:all]` is
  refused. **[V-F7]** A tool matching `[:k :read/pattern]` and writing
  `:note` is refused `:tool-loop`, and two passes leave no output of it.

The count's receipt is the build's, not a test: the test tool and grammar
are EDN data in `tools_test.clj`, written through `client/offer-until-
answered!`, and adding them needed no change under `src/` beyond the steps
section 8 lists.

## 8. The machinery count, predicted (question 6)

What counts as one compiled step here: one named code unit added to the
fixed side or to the vocabulary, or one existing unit whose behaviour
changes (*assumed* definition; IMPLICIT_SPEC O21 leaves it open, and the
build records it with the count). Predicted **[V-F8]**: 11 + 5 + 9 = 25
built tonight, one more in class b deferred (b7), and b5 examined and
resolved to no step.

**(a) Fixed-side steps the frame already promised (11).**

| # | Step | Where | Promised by |
|---|---|---|---|
| a1 | `:grammar` a control key: `control-fact?`, `control-value-ok?` through `grammar/parse`, `control-allowed?` for the owner, phase 2's unsealed list | `gate.clj`, `locks.clj` | the grammar fact in the seed (CONCLUSION 115); "grammars at the gate" (PROGRESS "Next") |
| a2 | the `:key-rows` rows written in the decision event | `module.clj`, `gate/decide*` | the same |
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

**(b) Fixed-side steps nobody anticipated (5 built tonight, 1 deferred, 1 resolved to none; [V-F8]).**

| # | Step | Why it was needed |
|---|---|---|
| b1 | `:grammar-change-needs-rebuild`, and the `:used` flag on a key's row | a grammar changing a used key's hints would leave its indexes wrong without a rebuild (P:916-918); nobody planned the refusal or the flag; a use in the same act or batch counts ([V-F4]) |
| b2 | the run's derived name and derived content | once per match with no runner state; the rulings cover acts that name what they cause and derived landing names, not a tool's runs; the content must be derived too, `:claimed-when` included, or a second runner is `:name-taken` ([V-F5]) |
| b3 | the loop check | nothing in the frame says what stops a tool feeding itself; it covers the runner's own read-entry writes ([V-F7]) |
| b4 | the micro gate's block 1 reads the rows before it opens values | phase 3 opens values on the arrival task before any hop (MP:208-223; built, `micro/arrival-open` before the `(|hash *name)` and `(|hash *layer)` hops), and the rows live on `hash(L)`: one hop per act ([V-F9]) |
| b5 (resolved: no step) | a lease road for a tool actor | examined ([V-F6]): phase 2 refuses a lease by an actor with no person lock, and read entries are sealed too; the runner leases for the tool as the operator into the tool's door session and takes the locks into the door's pool (`client/stock!`, in the locks build's working copy at 04:00), so no store code changes; if `stock!` does not land, a public pool refresh after `client/lease!` is one door step, class b. Kept here so the receipt shows it was examined |
| b6 [V-F8] | `client/lookup-many`, the batch form of the answer lookup by name plus layer | a pass must find which matches already ran in one roundtrip per tool, not one per match; the door had only the single lookup (`client.clj:53-66`) |
| b7 [V-F8] (deferred) | a "start after" bound on the pattern read | a tool with more than n matches reads the same first page every pass (the built `read-pattern` takes `layer for pattern as-of limit`, `read_exit.clj:69-76`), so matches past n never run; not needed by tonight's proof, whose layers hold a handful of facts; the runner reports such a tool `:partial`, never complete. With R6's standing read it may not be needed at all |

**(c) Capabilities (9 vocabulary entries).** `:emit`; `:revision/read-units` and
`:revision/read-span` (the revision reader's two steps, its plan section
12, anticipated since 13 September; **[V-F8]** one capability exposing two
built-in steps, as RIG.md counts it, and two entries in this count's
unit); and the formula functions `:lit`,
`:in`, `:got`, `:count`, `:str`, `:map`. Each is a built-in a recipe calls.
`:count` and `:str` are here because the test tool uses them; the thesis
count, on a tool nobody tonight knows, will show which of these a second
tool reuses and what it adds.

After these, writing the test tool and the test grammar needs no compiled
step: they are facts (R1, G1 to G7). That is the machinery count's zero,
not the thesis count's.

## 9. The plan template's sections

### Reads

| Read | Access | Path | Where | New or changed |
|---|---|---|---|---|
| RD-G1 the stream gate's key rows | `local-select>` in a `loop<-` over the act's distinct keys and the keys its grammar facts govern | `(keypath *layer :key-rows *k)` on `$$layers` | the layer's home, inside the gate's one event | new |
| RD-G2 the micro gate's key rows | `local-select>` per distinct key in the `(|hash *layer)` visit | `(keypath *layer :key-rows *k)` on `$$micro` | `hash(L)`, inside the batch | new |
| RD-G3 `read-pattern` | in the existing query topology: the pattern's key row before `parse-pattern`; then the rows of the distinct keys the page shows | `(keypath *layer :key-rows *k)` | the read layer's home | changed |
| RD-G4 `read-point` | the rows of the distinct keys of the rows shown | the same | the same | changed |
| RD-G5 rebuild pages | the rows of the page's distinct keys | the same | the same | changed |
| RD-T1 the runner's tools | `read!` of `[:k :tool]`: `read-pattern` over `:ix-ke` under `:tool␀` | the exit's | L's home | new use of the exit |
| RD-T2 a tool's matches | `read!` of the tool's pattern, as the tool | the exit's | L's home | new use |
| RD-T3 a tool's run names | one `foreign-select` per tool, `[(keypath L :answers) (submap names)]`, in a new `client/lookup-many` | `$$layers` | L's home, one roundtrip | new |

RD-T3 is one roundtrip per tool, not one per match: client-side point reads
one by one would be the skill's anti-pattern (N roundtrips for one answer).
[probed] One `foreign-select-one` with `submap` over a subindexed map of
answers, asked for three names of which two exist, returned exactly the two
(`runs/phase6-tools-rows-probe.txt`). That it costs one seek per named
entry is *assumed*.

### Writes

| Write | Depot | What it is |
|---|---|---|
| WR-G1 a grammar fact | `*offers` (stream), `*micro-offers` (micro) | a control fact; admitted, it sets the row `[L :key-rows k :grammar]` |
| WR-G2 the first fact under a key in a layer | the same, any act | the row's `:used` set true, in the same event or batch |
| WR-T1 a tool fact | the same | an ordinary value fact, sealed at the door |
| WR-T2 a tool's grant | `*offers` | phase 1's `grant-offer`, the operator's |
| WR-T3 a run's output act | `*offers` | an ordinary offer through the door, under the tool's pid |
| WR-T4 the runner's read entries | `*offers` | the exit's entry acts (P:259-277) |
| WR-T5 the runner's leases for a tool ([V-F6]) | `*offers` | an operator lease act into the tool's door session, sized to what the tool will cite, bare rows consumed at decision |

### PState Design

One new field in the per-layer state of each store; no new PState.

- **Option A (chosen): `:key-rows`, a subindexed map under the layer, one row
  per key** (the schema in 4.7). An act costs one seek per distinct key
  (usually one, 0.5 ms), no iteration; a grammar write or a first use costs
  one row write. The gate parses the row's shape text when it checks a
  value: CPU only, well under a millisecond for a shape of 256 nodes
  (*assumed*), no I/O.
- **Option B: one plain map of every row under the layer.** One seek per
  act whatever its keys, but every decision deserializes every row (at 100
  keys and about 500 bytes a row, 50 KB a decision), and every grammar write
  rewrites the whole map. Rejected: per-decision cost grows with the layer's
  keys.
- **Option C: the rows inside `:settings`**, which every event reads
  already. B's cost plus a settings schema change. Rejected.
- **Option D: a separate PState `$$grammars` indexed by layer then key.** Same
  leading structure and same partitioner as `$$layers`; the skill says one
  PState. Rejected.
- **Store-wide rows** (indexed by key alone): section 3's B and C. Rejected
  there.

Subindexing: nothing bounds the keys a layer uses, so `:key-rows` is
subindexed (`{:subindex-options {:track-size? false}}`), as phase 1's other
per-layer maps are.

### Depots

None new. Grammar and tool facts are offers like any other, on `*offers`
`(hash-by :layer)` and, for shared layers, `*micro-offers` (MP:1451-1452).
The runner's outputs and read entries ride `*offers`: tonight the runner
serves one-owner layers only, because the exit does (P:39-43).

### Topologies and PStates

No new topology.

- **`gate`, stream, extended** (phase 1's, `module.clj:73-148`). Why stream:
  unchanged (ack coordination; SP). New writes: `[L :key-rows k]`, a `termval`
  of a row computed in the event from the row read in the same event: a
  set, idempotent. A replay after the event committed finds the answer
  record and writes nothing (the record path); a replay before the commit
  recomputes the same rows from the same state (*derived*; phase 0 finding
  1: the event's writes commit together). No non-idempotent write is added.
  No partitioner is added to the event.
  - `$$layers`: phase 1's schema, plus phase 2's and the read exit's fields,
    plus `:key-rows (map-schema clojure.lang.Keyword <row> {:subindex-options
    {:track-size? false}})`, `<row>` as in 4.7.
- **`micro`, microbatch, extended** (phase 3's, once built): `$$micro [L
  :key-rows k]`, written in the batch where the grammar fact or the first use is
  admitted, exactly once by the microbatch.
- **No runner topology** (6.1).

### Query Topologies

`read-point` and `read-pattern` (P:671, P:712) gain row reads. The number
of meaningful reads varies with the input, so it is handled dynamically:
the query computes the distinct keys of what it will show and reads each
row once in a `loop<-` (*derived*).

- `[:k :mention]`, `[:ek e k]`, `[:latest e k]`, `[:kv k v]`: one key, one
  row read, always meaningful (it decides the hints and the marks): +1 seek.
- `[:e :e1]` over a page with 2 keys: +2 seeks.
- `[:all]` over a page with 5 keys: +5 seeks, bounded by the keys the layer
  uses.
- A point read of 3 facts under one key: +1.

### Partitioning efficiency

Optimal placement first: every read a grammar adds belongs to one layer's
decision or one layer's read, so the placement that minimizes seeks is
f(layer, key) = the layer's home task, `hash(L) mod N`, the same place as
the layer's facts, settings and indexes; `(hash-by :layer)` on `*offers`
and `(|hash *layer)` in the queries implement it (*derived*). The dominant
operation is an offer's decision (every write, and every read's entry, is
one); the table counts only the seeks this phase adds to it. The
proportions are *assumed*; a workload with more reads has more store-only
acts, which only lowers the weighted sum.

**N = 1**

| Data category | Proportion | Seeks/op | Iterator reads/op |
|---|---|---|---|
| act under one user key (a note, a mention) | 0.80 | 1 | 0 |
| act under two or three user keys | 0.15 | 2 | 0 |
| store-only act (read entries, grants, leases, layer making) | 0.049 | 0 | 0 |
| act holding a grammar fact | 0.001 | 1 | 0 |

Weighted seeks = 0.80 + 0.30 + 0 + 0.001 = 1.101; weighted iterator reads = 0.

**N = 16**: the same rows, every read on the layer's home task. Weighted
seeks = 1.101; iterator reads = 0.

**N = 128**: the same. Weighted seeks = 1.101; iterator reads = 0.

Flat in N. For comparison, section 3's B has the same seeks per operation
but puts them all on the base's one task and adds two hops to each, so
that task's load grows with N; C has the same reads and N row writes per
grammar change (rare, and `|all`'s accepted cost), plus the copy.

The same holds for `read-pattern` (+1 seek for patterns that name a key, which are
most of them; + the page's distinct keys for `[:e]` and `[:all]`), all on
the read layer's home.

**[V-F9] The micro store's decision**, the same categories in a shared
layer: +1 seek per distinct key at `hash(L)` (the same weighted 1.101 at N
= 1, 16 and 128, totals, one task per read) and +1 network transfer per
act for the reorder (arrival, `hash(L)`, back to arrival, then the name
task, against the built arrival, name, `hash(L)`), about 1 ms each
(*assumed*) inside a cycle of at least 300 ms, flat in N. The road that
saves the transfer and why it is not taken are in 3.2.

### Design Decisions

- Subindexing: `:key-rows` (unbounded keys per layer). Nothing else new is a
  collection.
- Colocation: a layer's key rows sit with its facts, settings, permission
  rows and indexes on its home (stream) or with its projections on
  `hash(L)` (micro); the depot's partitioner already routes every offer
  there.

### State primitive selection

- `:key-rows` rows (PState field): per source event at most one row write per
  distinct key of the act; bounded by the act. Durable. Written only by the
  layer's gate from admitted facts, so a layer's log holds everything they
  say (the grammar facts and the keys used), as it does for settings and
  permission rows.
- No TaskGlobal, no in-memory cache, no external system.
- The runner keeps no state: what it has done is in the store, under names
  it can recompute.

### Resource usage analysis

Disk, per row (*assumed* sizes): the key 16 B; `:used` 1 B; the grammar's
fid about 60 B, stamp 8 B, shape text 50 to 500 B, subjects path about
20 B, flags and hints about 20 B; RocksDB and Rama overhead about 50 B.
About 250 to 700 B a row. A layer using 10 keys: about 5 KB; 100,000
layers: about 500 MB across the cluster, about 4 MB a task at N = 128.
Tool facts are ordinary log rows (about 0.5 to 2 KB each). A pass adds T +
1 read-entry lines to the layer (about 300 B each short; a tool asking for
rows can write up to about 80 KB for 1,000 matched pairs, P:909-913).

Memory: none held. A pass holds one page of matches (at most `n` rows,
1,000 by default, about 500 KB) for one tool at a time.

Minimization: the rows hold only what the gate reads on every decision
(shape, subjects path, flags, hints, and the fid and stamp that say which
fact is in force). They duplicate the grammar fact's value, which the log
also holds sealed or plain; the row is the projection the gate reads
without opening the log, as settings and permission rows are
(*derived*: without it every decision would read the layer's grammar facts
through `:heads` and the log, two or more seeks per key).

## 10. What changes in earlier stages' code and tests

Code, by namespace (the names are the plans'; the build binds to what was
built):

- `rig.store.gate`: `control-fact?` and `control-value-ok?` know
  `:grammar`; `control-allowed?` lets the owner write it; `refusal` gains
  the grammar cases of `:malformed-control` and `:control-not-allowed` and
  `:grammar-change-needs-rebuild`; `intake` returns the keys whose rows the
  event reads; `decide*` returns the changed rows and gives
  `reads/index-writes` the rows' hints.
- `rig.store.module`: `:key-rows` in the schema; the `loop<-` that reads the
  rows (step 4); the row writes.
- `rig.store.locks` (phase 2): `read-values` takes the rows' grammars,
  checks shapes, skips opaque keys.
- `rig.store.grammar` (phase 2's): the constant `grammars` goes; `subjects-of`
  stays; `parse`, `hints`, `row` (from a grammar fact to a row) and
  `grammars-of` (from rows to the map `subjects-of` takes) come.
- `rig.store.shape`, new: the shape language's parser and checker.
- `rig.store.reads` (phase 5): `seed-hints` becomes `store-hints`, the
  store's own keys only; `read-point`, `read-pattern` and the rebuild read
  rows; `open-row>` marks opaque rows. **[V-F10]** The three callers of
  `reads/current-hints` (`gate.clj:281`, `reads.clj:426`, `reads.clj:975`)
  take the rows' hints; `current-hints` goes or returns `store-hints` only.
- The micro module (phase 3), [V-F10]: `micro/layer-rows` reads the key
  rows; block 1 carries them to `micro/arrival-open`; `micro/prepare`
  applies the rebuild check with the batch's own uses ([V-F4]);
  `:grammar-change-needs-rebuild` joins `micro/reason-order` after
  `:control-not-allowed`.
- `rig.store.recipe` and `rig.store.runner`, new; `rig.store.client` gains
  `lookup-many`.
- The micro module (phase 3), when built: `:key-rows` in its layer projections,
  block 1's order, grammar facts projected as permission rows are.

Tests whose expectations change (the build lists each by name after running
the suites):

- Phase 2's cases that rely on `:mention` naming its people (the A cases
  with a mention of Bob): they write the toy grammars first, through a test
  helper beside `client/seed!`; without them the permissive default leaves
  Bob out.
- The read exit's `[:kv :note ...]` cases: they write the `:note` grammar
  with `:by-value` first.
- **[V-F12]** `rig.store.read-model-test` (0 differences from the model
  over five histories, 73 answers, RIG.md): its histories read `[:kv ...]`
  patterns, so its replays write the toy grammars into each layer before
  that layer's first `:note`, or its value reads are refused `:not-indexed`
  and the comparison changes.
- Phase 1's E1 (`stream_gate_test.clj:180-197`, Bob not added from a
  mention): with no grammar written, it holds as phase 1 wrote it. Phase 2's
  constant grammar would add Bob; its F4 lists seven phase 1 expectations
  that change, and I have not checked whether E1 is one. Under phase 6 it
  depends on the layer's facts again. The build checks which version
  stands.
- Phase 8's replays of the model's histories write the toy grammars into
  each layer first, so the rig's subjects match `fact-subjects`
  (`model.clj:241-248`); every value the model makes passes the toy shapes
  (its generator names one or two people, `run.clj:18-19`, and its values
  are `{:token t}` plus `:persons`, `model.clj:830`).
- Benches: when phase 7 measures on the finished store (the lock bench
  runs on its own slice today, `lock_slice.clj`, with its own copy of the
  rule), its two-person `:mention` variant (`lock_bench.clj:83-91`) needs
  the toy grammar written, or Bob is not in its wraps and the lock-growth
  numbers for that variant change.

## 11. Picks

First-record (placeholders that touch a record; each is in the receipt):

- **T-FR1.** A grammar is one fact `{:e <key id> :k :grammar :v {:shape
  :subjects-at :opaque :index}}`, one per key per layer, the latest in force;
  the shape language's vector forms.
- **T-FR2.** `:grammar` is a control key: unsealed, no lock, not forgotten
  by a value forget.
- **T-FR3.** No grammar version is recorded on a decision; a value's
  grammar is the latest grammar fact for its key admitted in its layer
  before its act (its batch, in the micro store).
- **T-FR4.** A tool is one fact `{:e <tool id> :k :tool :v {:matches
  :signature :permission :recipe}}` in the layer it runs in; it acts as its
  own id.
- **T-FR5.** A run's output act: `:who` the tool, `:permission` its pid,
  `:stood-on` the matched fact and the tool fact with their stamps,
  `:because-of` the matched act's name, `:subjects #{}`, and a name derived
  from the layer, the tool fact and the matched fact; **[V-F5, V-F6]**
  `:claimed-when` the millisecond of the later stood-on stamp and
  `:session` the tool's door session, so its whole content is derived.
- **T-FR6.** The runner's read entries: its tools read by the operator with
  role `:stood-on`; each match read by the tool with role `:matched`.
- **T-FR7.** Where grammar facts live: in the layer they govern (3.2).

Rig choices (each can change without touching a record):

- **T-RC1.** Rows read per distinct key in the event or the layer visit;
  `:key-rows` subindexed.
- **T-RC2.** A key with no grammar is permissive (4.6).
- **T-RC3.** `:grammar-change-needs-rebuild`, and the `:used` flag.
- **T-RC4.** Grammar writers: the operator and the layer's owner.
- **T-RC5.** The shape language's bounds (depth 8, 256 nodes, 8 `[:or]`
  branches, 64 enum values) and its budget (65,536 visits).
- **T-RC6.** The run name's derivation: SHA-256 over the canonical triple,
  a version 8 UUID, no store-held secret.
- **T-RC7.** A tool's grant is a plain operator grant tonight.
- **T-RC8.** Opaque means never interpreted; the gate still unseals it for
  the digest and the re-wrap.
- **T-RC9.** A shown row of an opaque key carries `:opaque true`.
- **T-RC10.** The loop check: a key-level graph at the runner, tools added
  in stamp order.
- **T-RC11.** The runner is operator code, run by passes, looking names up
  before offering; **[V-F6]** it leases for each tool as the operator, bare
  rows, into the tool's door session.
- **T-RC12 [V-F3].** A `:die-with-any` mark on a key with no grammar in the
  layer is admitted; its wrap requires the owner and whoever the offer
  carries.

## 12. Open questions for Sid

- **Q1. Where a key's grammar has authority** ([V-F1], restated after the
  caching examination, its section 6 item 1). Per layer, with adoption
  later as an act in the layer standing on its source (A tonight, F
  later); or store-wide, enforced at every gate, which the order rule
  allows only as a recorded read (B) or a copy whose version each decision
  records (C), so T-FR3 falls either way. The examination's position is
  per layer with adoption: a gate can honestly enforce only what is in the
  layer it orders, as with permissions. C's one advantage, prompt reach
  when the base's grammar changes, survives only as the examination's index
  of immutable grammar versions (its 3.8), not as C. G fails the yardstick.
- **Q2. One key, two grammars.** May a key carry different grammars in
  different layers, or must a grammar that requires more or provides less
  be a new key (CORNERS C4.3)? And is a layer's reliance on another layer's
  grammar an act in the layer standing on it (3.3 F: deferred; in the
  examination's reading not a copy under the rule)?
- **Q3. Opaque and the lock.** Does "opaque" mean the store never
  interprets the value (tonight), or never unseals it, under a lock the
  store does not hold?
- **Q4. A key with no grammar.** Permissive, or refused until the layer
  states a grammar? Tonight, permissive: **[V-F2]** in a layer with a person owner a
  `:mention` of Bob under such a key is not in the act's subjects and gets
  no shape check or hints, and its unmarked wrap is the owner's with or
  without a grammar; in a shared layer it is about no one. **[V-F3]** And a
  `:die-with-any` mark on such a key: admitted (tonight; it then dies only
  with the owner and whoever the offer carries, and the offerer is not
  told), or refused?
- **Q5. Changing a used key's index or opacity.** Refused (tonight), or
  admitted with a rebuild during which that key's reads are marked
  partial?
- **Q6. A run's name derived from the tool and the match.** Within "names:
  random, made by the offerer before the gate", as the landing's name is?
- **Q7. The tool as a source of subjects** (ruling 8's third source): none
  tonight, as in the model. Should a tool's outputs be about the people its
  matched facts were about?
- **Q8. Who writes grammars and tools from inside, and who grants a tool
  its permission**: the owner and the operator (tonight), any writer with a
  permission in the layer, or the session holder beneath whom the tool
  acts (R7)?
- **Q9. A replaced tool and history** (open item 85): tonight a new version
  of a tool runs over every existing match, and the old version's outputs
  stay.
- **Q10. What a capability may reach ([V-F13]).** Tonight the operator's
  runner configuration names the repositories a tool may read, by id, and
  a record names only the id. Should that list be a fact on the layer (so
  "from inside" covers it, with a permission to write it), or stay the
  operator's?

## 13. Build order, and what to check first

1. After phase 2 and the read exit are merged: confirm the names this plan
   uses (`locks/read-values`, `grammar/subjects-of`, `reads/index-writes`,
   `reads/parse-pattern`, `read-exit/read!`, `open-row>`) and phase 2's
   refusal order, and bind to what was built, keeping this plan's rules.
   **[V-F10]** The table in section 1 lists what they became as of 04:00
   on 26 September; check it against the merge, the micro names above
   all, since that build had uncommitted work.
2. Short checks, probes under the flock with output in `runs/`: the exit
   taking the operator as a reader with `:permission` nil; and, once phase 2
   is merged, **[V-F6]** that an operator lease into the tool's door
   session (`client/stock!`, or `client/lease!` and a pool refresh) lets the tool's read entry and output, `:who`
   the tool under its pid, be sealed and admitted with no lease act by the
   tool, and that the door never falls back to `lease-for!` when the
   runner has leased enough. The row's schema and the batched lookup are
   probed already (`runs/phase6-tools-rows-probe.txt`).
3. The pure namespaces and their tests: `shape`, `grammar`, `recipe`.
4. The stream gate: the control key, the rows, their reads and writes, the
   refusals, the hints; then phase 1's, phase 2's and the read exit's suites
   green, each changed expectation listed with its reason.
5. The exit's row reads and opaque marks.
6. `client/lookup-many` and the runner.
7. `rig.store.tools-test`.
8. The micro gate's part (a4, b4) when phase 3 is built; until then the
   micro store's grammar is planned, not built, and the receipt says so.
9. The machinery count: every step with its class, its file and why, and
   any step this plan did not predict.

## 14. Design difficulty log

**Where grammar facts live** was the hard call, and it was close. Section
3's C, the store-wide grammar with a copy on every task, is what the skill's
`|all` rule would pick at first sight (small, rarely written, read on every
offer), and it keeps "the key's grammar" literally one thing. I weighed it
against A for a long time. Three things settled it. First, the copy has no
single writer: the base's grammar facts are admitted by the stream gate
tonight and by the micro gate after R8's re-class, and a PState has one
owning topology, so C needs a forward between the gates that nobody
planned. Second, the ruled pattern for the other thing a gate checks is
A's: permissions live in the layer they govern, and R7 copies a session's
permissions into each layer through its own gate rather than reading them
across. Third, the brief's rule on copies: A builds tonight with no copy,
so builder A's examination is not on the critical path. What A gives up
(one key, two grammars; and session layers, made in quantity, that start
with no grammar) I have put to Sid as Q1 and Q2 rather than settled. The
session-layer cost I saw late, on a read-through: it is the strongest point
for C, because under C a new layer has every grammar the moment it is made.
It did not change the pick for tonight, since tonight's layers are the
model's five and the fixture states their grammars, but it means the pick
is copy-free only for what is built tonight, and I have said so in 3.2.
**[V-F1]** The examination has since run (EXAMINATION-copies.md): A is
built, C and G fail, F is deferred and in its reading not a copy under the
rule, T-FR3 stays first-record; and in a one-owner layer the session-layer
cost is narrower than I wrote, since an unmarked value's wrap is the
owner's with or without a grammar (4.6, [V-F2]).
I also noticed my own pull toward C as the "Rama-shaped" answer, which is
the failure mode CLAUDE.md names (a fluent pattern beating project truth);
the permission sharpening is the project's truth here.

**Where the runner runs** was forced once the one exit was taken seriously:
a topology cannot wait for an entry's answer, and the door seals at the
edge. Not contested.

**How to run each match once.** A cursor fact per tool was the other
candidate; it needs a write per pass and still doubles an output when a
crash falls between the output and the cursor. Derived names need no state
and make a rerun a retry, and IMPLICIT_SPEC OP18 had derived the same. The
cost is a ruling's word ("random"), which I have put to Sid (Q6).

**The loop rule.** A depth counter carried along because-of chains would
bound any loop but needs a new record field and reads along the chain. The
static key graph needs neither, and within one layer it is enough. Its
coarseness (it refuses some tools that would not loop) is the price; not
contested beyond that.

**Opaque and the lock.** I could not settle what ruling 6's "who opens"
means for phase 2's locks from the rulings alone. Tonight's reading changes
nothing in phase 2; the other one changes the digest and the forget. Put to
Sid (Q3).

**Checking a value under the grammar before its act, or its own act's.**
Phase 1 lets an act's own class fact make its layer; by analogy an act's own
grammar could govern its values. I took the grammar before the act because
it keeps the grammar a value was checked under derivable without recording
it (T-FR3). Close, and cheap to change before edition one.

## 15. Self-validation (`references/artifact-plan-validation.md`)

- **PState schemas.** One new field, `:key-rows`, in each store's per-layer
  state; no new PState, so no pair to merge. No `Object`: the shape is
  canonical EDN text, the subjects path a vector of keywords, the hints a
  set of keywords. Uniform rows use `fixed-keys-schema`. `:key-rows` is
  subindexed; nothing else new is a collection that can grow.
- **Partitioning.** No new partitioner; every read and write is on the
  layer's home (stream) or `hash(L)` (micro), where `(hash-by :layer)` and
  `(|hash *layer)` already route. Tables at N = 1, 16 and 128 above;
  weighted seeks 1.101 at each; flat.
- **Topologies.** No new topology; the stream gate's new writes are
  idempotent sets inside its one event, with no partitioner added; the
  micro gate's are exactly once. No choice rests on test synchronization.
- **Production readiness.** Concurrent clients: two runners on one layer
  offer the same names with the same content ([V-F5]: once `:claimed-when`
  and `:session` are derived), so the later answers come
  from the record; a writer and a grammar change race in one layer's
  single-threaded home, each decision seeing the other or not, and the
  grammar used is derivable either way. Client restart: the runner keeps no
  state and recomputes names. Worker restart: the gate's replay recomputes
  the same rows or finds the record; a runner's offer in flight is resent
  under its name by `offer-until-answered!`. Scale: rows are per layer and
  key, subindexed. No non-idempotent stream write. No stream write to
  several partitions is added.
- **Internal depots, cross-topology flows, `depot-partition-append!`.**
  None added.
- **In-memory state.** None.
- **Minimality.** Delete the rows: every decision would read the layer's
  grammar facts through the log (two or more seeks per key), and the read
  exit would need the same. Delete `:used`: a hint change on a used key
  would leave its indexes wrong (P:916-918). Delete the loop check: a
  self-matching tool adds one generation per pass for ever. Delete derived
  names: a rerun doubles outputs. Delete `lookup-many`: a pass offers every
  old match again (an append per match per pass). Each mechanism is needed
  by a named test (G4, R5, R2).
- **Throughput.** The added cost on the dominant operation is about one
  seek per act on its own task; no hop, no fan-out. A cheaper design would
  have to read no row, which only a grammar carried on the offer can do,
  and that is an envelope part (3.3 D).
- **Spec coverage.** SPEC phase 6: keys as ids (grammar facts on the key's
  id); a key's grammar as facts including subjects and opacity (4.1 to 4.4);
  a tool as facts with what it matches, its signature, its read-entry
  preference (5.1); a minimal runner finding tools by matching (6.2 step 1);
  the count (section 8). The brief: grammar facts checked at the gate (4.7),
  tool facts, the runner, the revision reader as a capability (5.2), the
  proof over `:note` and `:mention` only (7.2), the count in three classes
  (8). Ruling 3's spectrum by tool (the runner passes `:rows?`); ruling 6
  (4.4); ruling 8 (4.3; the tool's part none, Q7); "Tools add facts, never
  envelope parts" (no part added: the runner's acts use the envelope as it
  is); "order only through stood-on" (3.4); default 5 (5.1, T-RC7); R4
  (6.2 c). The brief's eight named tests are G1, G2, G3, G4, R1 with R2,
  R3, R4, R5.
- **Knock-on effects checked.** Moving the micro gate's open after its layer
  visit (b4) touches phase 3's lease consumption order; phase 3's plan
  decides it, and this plan states only that the rows must reach the open.
  The permissive default keeps every earlier suite's answers except where
  the constant grammar or the constant hints were relied on, which section
  10 lists. The per-layer pick leaves new layers, session layers above all,
  without grammars until a fact states them; 3.2 says so, and the roads
  that close it are Sid's (Q1).
