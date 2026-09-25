# Plan validation — tools and grammars (rig phase 6)

Validator: Claude Opus 5.5 (`claude-opus-5-5`), effort max, a fresh session,
26 September 2026 from 03:39 IST, as the rama skill's phase 2
(`references/phase-2-plan-validate.md`, template
`references/artifact-plan-validation.md`). Branch `rig-plan-tools`, worktree
`/mnt/data/projects/Softland-rig-plan-tools`. The plan validated is
`PLAN-tools-and-grammars.md` at `c161f238`. The default verdict was FAIL.

**Verdict: minor-fail.** Twelve failures, each fixable by a localized edit;
all twelve are fixed in the plan in place, numbered F1 to F12 and marked
`[V-F<n>]` where they land. None needs the architecture rethought: pick A
(a grammar in the layer it governs, read locally), the stream gate's one
event, the per-layer `:key-rows` row, the tool as one ordinary fact, and
the runner as operator code beside the door and the exit all survive the
traces below.

Tags on load-bearing claims: *checked* (read in the named source at the
named commit, or run), *derived* (reasoned from checked facts), *assumed*.
No cluster was run by this validation; no probe was needed, because every
decision-changing fact here is in committed source.

Read for this (*checked*): the plan whole; SPEC.md phase 6 (rig branch,
line 104); the brief as quoted by builder A; PROGRESS.md "Now" (main, lines
44-165); `docs/decisions.md` 159-184 (main); CONCLUSION.md R4, R7 and "The
seed" (main, 74-129); EXAMINATION-copies.md whole (main rig worktree, at
`36f27077`); RIG.md's overnight head (main rig worktree, 03:39 copy);
PLAN-locks-and-forgetting.md lines 155-215, 270-300, 398-410, 675-690, 742,
1054-1066, 1710-1722, 1786-1796, 1815-1826; PLAN-micro-store.md 180-300 and
its batch-id lines; the built code on the rig branch (the read exit's merge, `40374171`, byte-identical to `rig-build-reads` at `00fd0468`):
`client.clj`, `gate.clj` 20-190, `reads.clj` 28-72 and its hint call sites,
`read_exit.clj` 41-127; the locks build committed at `5a5de4fe`
(`client.clj` `build`, `lease-for!`, `assign!`, `lease!`; `locks.clj`
`person-owner`, `lease-writes`, `persons-refusal`, `read-values`;
`grammar.clj`). The micro build's and the read exit's committed names came
from a read-only gatherer (Sonnet) and were spot-checked where they decide
a fix. Not opened: `env.clj`, anything under `src/app`,
`test/rig/store/gate_test.clj`.

## 0. The failures and their fixes, one line each

| # | Failure | Fix in the plan |
|---|---|---|
| F1 | The grammar section still marks C and F "unsettled" copies and offers G to Sid; the caching examination has settled them | 0, 3.2, 3.3, 3.4, 12 Q1-Q2, 14: the examination's outcome recorded; A is built, C fails, G fails the yardstick, F is deferred (possible later through based-on, R4, with no new envelope part; in the examination's reading not a copy under the rule); T-FR3 stays first-record; the build builds A only |
| F2 | The cost of a key with no grammar is overstated: "a `:mention` of Bob written there is not about Bob ... his forget does not reach it" reads as a loss for every value, but in a one-owner layer an unmarked value is wrapped under the owner alone with or without a grammar (`wrap-of`, locks build) | 3.2, 4.6, Q4: narrowed to what a missing grammar changes: a `:die-with-any` mark, the act's subject union (finding), the shape check, the hints |
| F3 | A `:die-with-any` mark on a key the layer has no grammar for is admitted with no rule stated and no test: its wrap is owner ∪ carried, so the mark silently adds nothing | 4.6: stated as a rig choice (admitted, required = owner ∪ carried), T-RC12; test G7; the examination's question 5 put to Sid with Q4 |
| F4 | `:grammar-change-needs-rebuild` reads only `:used` from before the act (stream) or the batch (micro). An act holding the first `:note` grammar with `:by-value` and a `:note` value is admitted, its value indexed under the old hints, and the row then says `:by-value`: `[:kv :note v]` misses it silently. The micro batch has the same hole across acts, in both orders | 4.5, 4.7, 3.2 (micro), G4: the act's own value facts under k count as a use; in the micro store any envelope of the same batch in L offering a fact under k does, and the fold refuses the grammar |
| F5 | "Two runners at once offer the same name with the same content": false as built. `client/build` fills `:claimed-when` from the wall clock and the parts digest covers it (`client.clj` 21-37, both branches), so a second runner's offer is `:name-taken`, not answered from the record | 6.2 c, 6.3, T-FR5, 15: the run's output act carries a derived `:claimed-when` (the millisecond of the later stood-on stamp) and a fixed `:session`, so its content is a function of (L, tool fact, matched fact) |
| F6 | The possible fifth class-b step (a tool is not a person) left open. Resolved: phase 2 seals a lease row under `person-owner` of the lease act's `:who`, so a lease by the tool is refused `:no-such-person` (locks build `lease-writes`, `persons-refusal`); and read entries are sealed at the door too (LP:677-679), so every tool read needs a lock, not only its outputs | 6.2, 8 b5, 13.2: the runner leases as the operator (bare rows, L23) into the tool's own door session `(client/default-session tool-id)` and takes the locks into the door's pool, `client/stock!` (in the locks build's working copy at 04:00, uncommitted), sized to what it will offer. No store code; a runner step inside a11; if `stock!` does not land, a public pool refresh after `client/lease!` is one door step, class b; the build probes it after phase 2's merge |
| F7 | The loop check ignores the runner's own writes: every tool read writes a `:read/pattern` fact into L, so a tool matching `[:k :read/pattern]` gets a new match every pass for ever; and nothing stops `:out` from naming a store key | 6.4, 5.1, 5.2, R5, recipe-test: every tool's edges include the read-entry keys; a pattern on a store key and an `:out` holding one are refused at parse |
| F8 | The count misses steps: `client/lookup-many` is a new door function the plan lists in section 10 but not in section 8; the pattern read's "start after" is needed for any tool with more than n matches and is uncounted; the revision reader is two steps in the plan and "one capability" in RIG.md | 8: b6 `lookup-many`; b7 (deferred, not built tonight) the pattern read's start-after; the revision reader stated as one capability exposing two built-in steps |
| F9 | The micro reorder's hop is not in the cost tables, and the cheaper road (open on `hash(L)` with the lease rows carried sealed) was not constructed | 3.2, 9 "Partitioning efficiency": the micro row costed; the alternative constructed and rejected on the micro plan's own rule that a bare lock never crosses a task on the common path |
| F10 | The plan named functions of builds that had committed nothing; they have since. The hint seam is `reads/current-hints` (called at `gate.clj:281`, `reads.clj:426`, `reads.clj:975`), not only the constant `seed-hints`; `reads/read-keys` already names the read-entry keys; the micro gate's block 1 and fold are `micro/arrival-open`, `micro/layer-rows`, `micro/prepare`, and its refusal order `micro/reason-order`, where `:grammar-change-needs-rebuild` must also go | 1 (a table of what each name became), 10, 13: the names to bind after the merge |
| F11 | `:tool` is called "a store key the runner knows" in 5.1 while 4.1 excludes it from the store keys and 5.1 lets a grammar govern it | 5.1: `:tool` is an ordinary key the runner reads by name; the store keys are the control keys, `:grammar` and `reads/read-keys` |
| F12 | Section 10 misses `rig.store.read-model-test` (its histories read `[:kv ...]`), whose 0-difference result depends on the `:note` by-value hint | 10: listed; its replays write the toy grammars into each layer first |

## 1. The spec and the brief, verbatim, and where the plan answers them

SPEC.md phase 6 (rig branch, line 104, *checked*): "Tools and grammars from
inside. Keys are ids; a key's grammar is facts about it, including which
subjects its values name and whether they are opaque. A tool is facts too:
what it matches, its signature, its read entry preference; a minimal runner
finds tools by matching. Then the count: add one new tool and one new
grammar by writing facts only, and count how many new compiled steps were
needed. Zero is the target. Report the count and every step that was
needed, with why."

The brief (verbatim): "Phase 6: grammar facts checked at the gate, tool
facts, a minimal runner that finds tools by matching, and the code that
reads a file at a git revision into passages and functions, with content
and position and no store identity. Prove the runner with one test tool and
one test grammar over the model's toy keys (:note and :mention), nothing
about repo material: the reference tool, and how a passage stays the same
across revisions, are the model's third round's to design. Report the
compiled steps it needed in three classes: fixed-side steps the frame
already promised, fixed-side steps nobody anticipated, and capabilities.
Call it the machinery count. It is not the thesis count, which a fresh
session takes later on a tool nobody tonight knows."

| Clause | Plan (after fixes) | Verdict |
|---|---|---|
| "Keys are ids" | the grammar fact's entity is the key's id (4.1) | pass |
| "a key's grammar is facts about it, including which subjects its values name and whether they are opaque" | `{:e k :k :grammar :v {:shape :subjects-at :opaque :index}}` (4.1-4.5) | pass |
| "A tool is facts too: what it matches, its signature, its read entry preference" | `:matches`, `:signature {:in :out :rows?}` (5.1) | pass |
| "a minimal runner finds tools by matching" | `[:k :tool]` through the one exit (6.2 step 1); no tool known to compiled code | pass |
| "add one new tool and one new grammar by writing facts only ... Zero is the target" | R1 and G1-G6 write them as EDN facts through the door; the count's zero is after the machinery (8) | pass |
| "Report the count and every step that was needed, with why" | section 8, three classes, each with why; completed by F8 | fail → F8 |
| "grammar facts checked at the gate" | both gates (3.2, 4.7); micro completed by F4, F9 | fail → F4, F9 |
| "the code that reads a file at a git revision" | built and merged (`rig.revision`, RIG.md); the plan consumes it as a capability | pass |
| "one test tool and one test grammar over the model's toy keys (:note and :mention), nothing about repo material" | `:mention-count` and the `:mention` grammar; G3's `:blob` is a test key used only for the opaque case, stated so (7.2 G3) | pass |
| "three classes" | a, b, c (8) | pass after F8 |
| "It is not the thesis count" | 8's last paragraph says so | pass |

The rulings the brief names (PROGRESS.md "Now", main, *checked*):

- Ruling 3, "Spectrum, by tool: a deterministic tool gets the short entry
  by default and may ask for exact rows in its signature" and "trigger is
  already because-of": `:rows?` in the signature, passed to `read!`'s
  `:rows?` (built, `read_exit.clj:48-66`, `reads/reader-kinds` has `:tool`);
  the output's `:because-of` is the matched act's name. Pass.
- Ruling 5, "Keys as ids: confirmed": pass.
- Ruling 6, "A key's grammar may declare its values opaque when a tool
  needs it, at the cost of no matching, no shape check and no index on
  them, and shown as opaque": 4.4; traced in 2.6 below. Pass.
- Ruling 8, "Subject slot on the act: filled from the layer's owner, the
  key's grammar, and the tool": owner and grammar from the rows; the tool's
  share is the carried `:subjects`, `#{}` tonight (Q7). Pass.
- "Keys are ids; word and grammar are facts about them": pass.
- "Tools add facts, never envelope parts": the runner's acts use the
  envelope as built (`:who :layer :class :permission :session :stood-on
  :because-of :claimed-when :subjects :facts`, `envelope.clj:27-28`); F5
  and F6 fill two existing parts, add none. Pass.
- decisions.md, "Tools are records over a vocabulary": "named steps over a
  vocabulary of capabilities, formulas in the leaves, one executor with
  several runners"; "Nothing in a record is a program". The recipe names
  capabilities by keyword and holds formulas from a closed set; nothing
  names a var or a function. Pass, with one note for Sid (section 7): the
  decision reserves "a designed total language" in the leaves for when "a
  record provably cannot say a needed tool twice over", "asked of Sid
  then"; tonight's six formulas are the smallest set the test tool needs,
  not that language.
- CONCLUSION.md "The seed": the tool fact's four parts and the grammar
  fact's four parts match the seed's lists; the count's classes map onto
  "protocol" (a, b) and "capability" (c). Pass.

## 2. The traces

### 2.1 The stream gate checking a grammar, one event

History H1, layer `:alice` (personal, home task t = hash(:alice) mod 8):

1. Operator act A1 `{:e :mention :k :grammar :v {:shape [:map {:persons
   [:set-of [:keyword] 1 2]} {:open? true}] :subjects-at [:persons]
   :opaque false :index #{}}}`. Event on t: reads settings, `$$persons
   [:alice]`, the key rows of the act's keys (`:grammar` is a store key: no
   row) and of the governed key `:mention` (4.7 step 2): nil. Decision:
   exempt; `grammar/parse` ok; the operator may write it; not a hint change
   (the defaults are `#{}` and not opaque, 4.5), so no rebuild refusal.
   Yes. Writes in the same event: `[:alice :key-rows :mention :grammar]`.
   No partitioner (plan 3.2; the built gate has none, `module.clj`).
2. Alice act A2, a sealed `:mention` of `#{:bob :carol :dan}`. Same task:
   rows read → the `:mention` row; `read-values` opens, `shape/check` finds
   `[:persons]` has 3 elements, max 2 → `:value-shape`, recorded (phase 2's
   L27 place, after stage 1's list). Traced against the built order: stage
   1's `refusal` (`gate.clj:128-187`) runs first, then phase 2's
   `lock-refusal` (locks build, "in L27's order: the value checks' reason
   ... then `:no-such-person`"). So a malformed grammar and a bad value in
   one act answer `:malformed-control`, and a bad value from a writer with
   no permission answers `:no-permission`. Pass.
3. Alice act A3, a `:mention` of `#{:bob}` marked `:die-with-any`: shape
   passes; `subjects-of` over `grammars-of rows` gives `#{:bob}`; own
   subjects `#{:alice :bob}`; wrap `{:required [:alice :bob]}`
   (`wrap-of`, locks build 253-268: marked → required = own subjects).
   Row write: `:used true`. Pass (G2).

One event, one task, all reads local, one atomic group (*derived* from
phase 0 finding 1 as the plan cites it). Replay before commit recomputes
the same rows from the same state; after commit, the record path writes
nothing (the plan's 9 "Topologies"). Pass.

### 2.2 The micro gate: block 1 reordered, one hop per act (class b)

The micro plan's block 1 (*checked*, PLAN-micro-store.md 208-223) opens a
sealed value on the arrival task, `hash(route-key)` with route-key the
lease name, "before any hop", and computes subjects there with
`subjects-of`. The rows live on `hash(L)`. The plan's road: parse, hop to
`hash(L)` for settings, permissions and key rows, hop back to the arrival
task to open, check and drop the plaintext, then the name and entity visits
(3.2). History H2, group layer `:g` (shared), one batch:

- E1 a sealed `:mention` in `:g` arriving at task a = hash([lease-name]).
  a → hash(:g): reads settings, permission rows, `[:g :key-rows :mention]`;
  → a: unlease, open, shape check against the carried row, subjects (no
  owner in a shared layer, so grammar ∪ carried), `$$persons` per subject
  (local), plaintext dropped; → name task; → entity tasks; the fold.

The hop is real: one extra transfer per act (a → hash(L) → a instead of
a → name → hash(L)), class b4 as the plan counts it. The plan did not cost
it and did not construct the obvious cheaper road (F9):

- Road M1 (the plan's): +1 hop per act; bare locks and plaintext never
  leave task a.
- Road M2 (constructed here): keep the micro plan's order (a → name →
  hash(L)), carry the lease rows as read on a, sealed, to `hash(L)`, and
  unlease and open there with `$$persons` local. Zero extra hops. But an
  operator's lease row is bare (`{:under nil :sealed K}`, L23), so every
  operator act in a shared layer would carry a bare lock across a task on
  the common path; the micro plan allows that only on the rare resend path
  ("an operator's bare row is the one case where a bare lock crosses a
  task, inside the cluster, on this rare path", MP record path).
- Cost side by side, per act, *assumed* 1 ms a hop: M1 ≈ +1 ms latency
  inside a ≥ 300 ms microbatch cycle, +1 network transfer of the skeleton
  and the sealed bytes; M2 = 0. Throughput: M1 adds one transfer per act;
  seeks are equal (the rows are read once either way).
- Verdict: M1 kept, for the security rule M2 breaks, not for simplicity;
  the plan now says so and costs it (F9). Whether M2 with bare operator
  rows is acceptable is phase 3's and Sid's; recorded in section 7.

In-batch semantics (F4). Values in a batch are checked and indexed under
the rows read before the batch. The micro record carries `:batch`
(PLAN-micro-store.md 1150, the answer record's microbatch id), so T-FR3's
"latest grammar before its batch" is derivable at read time (*checked*;
the plan asserted it without the citation). But the rebuild check is not
safe inside a batch: history H3, one batch in `:g`, E1 a `:note` value
(the key unused before), E2 the first `:note` grammar with `:index
#{:by-value}`. E2's check reads `:used false` → admitted; E1's index writes
use the pre-batch hints (no `:by-value`); the row after the batch says
`:by-value`; `[:kv :note v]` in `:g` misses E1's value and reports the page
complete. Reversed order (E1 the grammar, E2 the value) gives the same
result, because block 2a indexes every envelope under the pre-batch hints.
Fix F4: the fold refuses a hint-changing grammar for k in L when any
envelope of the batch in L offers a fact under k (the skeleton keeps every
fact's `:k`, MP "the parsed offer minus every `:v`"), whatever the order.

### 2.3 Where the grammar check sits in the refusal order

Built stage 1 order (`gate.clj:128-187`, *checked*):
`:fact-outside-the-acts-layer`, `:no-such-layer`, `:class-mismatch`, the
four permission checks, `:malformed-control`, `:control-not-allowed`,
`:stale-replaces`, `:stale-revoke`, `:layer-already-made`,
`:unsupported-reclass`; then phase 2's value checks (`:does-not-open`,
`:malformed-value`, `:value-shape`, `:too-many-subjects`,
`:grain-mismatch`) and `:no-such-person`, `:person-forgotten` (locks build
`lock-refusal`). The plan's insertions: grammar admission inside
`:malformed-control` and `:control-not-allowed` (no new code), the new
`:grammar-change-needs-rebuild` right after `:control-not-allowed`, the
shape check inside phase 2's `:value-shape`. Consistent with both built
orders. Pass.

### 2.4 A grammar change on a key already used

History H4, `:alice`: A1 a `:mention` (the key used, `:used true`); A2 a
grammar adding `:by-value` to `:mention`: the row's `:used` is true and
`:index` changes → `:grammar-change-needs-rebuild`, recorded. A3 the same
fact in `:alice-agent`, no mention there → admitted; `[:kv :mention v]`
then answers there (G4). Pass for the cross-act case.

History H5, one act (stream): the first `:note` grammar with `:by-value`
and a `:note` value in the same act, `:note` unused. Before F4: `:used`
false → admitted; `reads/index-writes` is given the hints of the rows read
before the act (4.7 step 6), so no `:ix-kv` entry; the row now says
`:by-value`; a later `[:kv :note v]` misses the value, silently. The plan's
own rule "a value is checked under the grammar in force before its act"
(4.7) is kept for shapes and subjects, but for hints it produces an index
that disagrees with the row. FAIL → F4: the act's own value facts under k
count as a use, so H5 is refused `:grammar-change-needs-rebuild`; G4 gains
H5 as a case.

### 2.5 Subjects from a grammar reaching the wrap; the unmarked narrowing; a mark with no grammar

- Grammared, marked: H1's A3, wrap `{:required [:alice :bob]}`. Pass.
- Grammared, unmarked, one-owner layer: wrap `{:required [:alice]}`
  (`wrap-of`: "unmarked, a person owner: the owner alone required",
  locks build, *checked*); `:subjects #{:alice :bob}` on the answer record
  (for finding). Bob's forget does not close it, as the ruling says
  ("Alice's mention of Bob in her own layer dies with Alice and survives
  Bob", PROGRESS.md 140-142).
- No grammar, unmarked, one-owner layer (a session layer made tonight with
  no grammar): wrap `{:required [:alice]}`, the same as with a grammar;
  `:subjects #{:alice}`. So the only differences a missing grammar makes
  are the act's union (finding by subject), the shape check and the hints,
  and a mark (next). The plan's 3.2, 4.6 and Q4 said "a `:mention` of Bob
  written there is not about Bob ... his forget does not reach it" as if
  the forget were lost for every such value. FAIL → F2 (the examination's
  narrowing, its 3.1).
- No grammar, marked `:die-with-any`: own subjects = owner ∪ ∅ ∪ carried
  (LP 190-198; `carried` is the offer's `:subjects`). With nothing carried,
  wrap `{:required [:alice]}`, identical to unmarked: the mark is admitted
  and does nothing, and the offerer is not told. The plan states only the
  consequence in passing (4.6) and tests nothing. FAIL → F3: stated as a
  rig choice (admitted, the permissive end, since refusing would add a
  recorded reason that is Sid's), test G7 (a marked `:mention` of Bob in a
  layer with no `:mention` grammar: yes, `:subjects #{:alice}`, wrap
  `{:required [:alice]}`, still opens after Bob's forget; with `:subjects
  #{:bob}` carried, wrap `[:alice :bob]`), and the question to Sid.
- Micro: the same computation on the arrival task with the carried rows;
  no owner in a shared layer, so the unmarked wrap is any-of the own
  subjects, and there a missing grammar does change the forget: a `:mention`
  of Bob in a shared layer with no grammar is about no one, its wrap empty
  (LP 207-209; the plan's 4.6 says so for promoted copies). The narrowing
  in F2 is stated for one-owner layers only.

### 2.6 Opaque keys

History H6, `:alice`: grammar for `:blob` `{:shape [:any] :subjects-at nil
:opaque true :index #{}}` (a hint change from defaults → only on an unused
key). `:blob` values `"x"` and `{:a 1}`: no shape check (`read-values`
skips the grammar's shape; the floor's `edn-value?` still runs), no value
subjects, admitted. `(grammar/hints rows)` puts `:blob` in `:opaque`, so
`reads/by-value?` is false (`reads.clj:51-54`, "hinted so and not opaque")
and no `:ix-kv` entry is written; `[:kv :blob v]` is refused `:opaque`
(built, `reads.clj:458`); `[:k :blob]` shows both rows with `:opaque true`
(a9, not built yet). Admission refuses an opaque grammar with a shape, a
subjects path, or `:by-value` (`:malformed-control`). Pass. The lock's
meaning of opaque is Q3, correctly left to Sid.

### 2.7 The tool fact

`{:e :mention-count :k :tool :v {:matches [:k :mention] :signature {:in
:match :out #{:note} :rows? false} :permission [:mention-count :alice
:alice] :recipe [...]}}` (5.1). What it matches: a read exit pattern, one
of the six built forms. Signature: input, output keys, and the read-entry
preference `:rows?`. The pid it acts under: `[who layer in]` with `who` the
tool's id; the built gate refuses a pid whose `who` is not the act's `:who`
(`:permission-does-not-cover-this`, `gate.clj:153`), so a tool fact naming
another actor's pid runs to a refusal as data. The recipe: named steps
(`:emit`, `:revision/read-units`, `:revision/read-span`) with formulas in
the leaves from a closed set (`:lit :in :got :count :str :map`); no var,
no function, no loop, bounded at parse (16 steps, depth 8, 256 nodes).
Nothing in it is a program. Pass, after F7 (an `:out` of a store key and a
pattern on one are refused at parse) and F11 (`:tool` is an ordinary key).

### 2.8 The runner

History H7, `:alice`, with the toy grammars: operator writes the tool and
its grant; Alice writes `:mention`s on `:e1` (`#{:bob}`) and `:e2`
(`#{:alice :bob}`).

- Pass 1, step 1: `read!` `{:reader :operator :reader-kind :tool :rows?
  false :role :stood-on :for :alice :working :alice :permission nil :layer
  :alice :read [:pattern [:k :tool]] :limit n}`. Every key it passes is one
  `check-call` accepts (`read_exit.clj:48-66`: `:for` a keyword,
  `:reader-kind` in `#{:person :model :tool}`, `:permission` nil allowed,
  `:role` in the four seed roles). The entry act is built by `entry-offer`
  with `:session nil`; under phase 2's door `build` gives an offer with a
  value fact and no session the writer's `default-session`,
  `:door/operator`, and the operator's lease is bare (L23). Pass.
- Step 4a: the tool's read, as `:mention-count` under its pid. Its entry
  act holds a `:read/pattern` value fact, which phase 2 seals at the door
  (LP:677-679, "its read entries go through the door like any act, so
  their values are sealed once this stage lands"). The door's `assign!`
  leases through `lease-for!`, "a lease act by its writer" (locks build
  `client.clj`), and the gate seals a lease under `(person-owner (:who
  offer))` and refuses it `:no-such-person` when that actor has no
  `$$persons` entry (`lease-writes`, `persons-refusal`, *checked* at
  `5a5de4fe`). So the tool's first read fails before it reads anything,
  and the same holds for its outputs. The plan's b5 ("if needed") is
  needed. Resolution (F6): the runner leases as the operator into the
  tool's own door session in L and takes the locks into the door's pool,
  `(client/stock! store :operator L (client/default-session tool-id) k)`,
  for k = 1 per read entry plus the run's outputs, before the read and
  before the outputs. `stock!` is in the locks build's working copy at
  04:00, not committed ("Lease `n` locks for `session` in `layer` by `who`
  and take them into the door's pool, so the acts that follow in that
  session need no lease"). The committed door alone is not enough: after
  `client/lease!` the rows exist, but the pool is filled only by the
  private `refresh!`, which `assign!` calls when the layer's grain is not
  yet known or after its own lease succeeds; with the grain known, `take!`
  finds the pool empty and the door falls back to `lease-for!` as the
  tool, a recorded `:no-such-person` lease, and the act is refused
  `:no-such-lock` on its face (*checked*, `assign!`, `refresh!`, `take!`
  at `5a5de4fe`). So if `stock!` does not land, a public pool refresh is
  one door step, class b. The gate ties a cited lock to the offer's
  `:session` and layer only (the lease row holds `{:under :sealed}`, no
  writer), so the tool's act is admitted. No store code: a runner step
  inside a11 (given `stock!`). Rows are bare until consumed at decision,
  as the operator's own are; the runner stocks exactly what it will cite,
  so the fallback never fires. When tools run beneath a person's session (R7), the lease is
  that session owner's and sealed under her lock (L23), and this step goes.
  *Derived* from the committed code; the build's probe (13.2) confirms it
  on the merge.
- Step 4b-c: `(recipe/run tool row)` → `{:e :e1 :k :note :v {:token "1
  named"}}`; the output act `:who :mention-count :layer :alice :class
  :by-layer :permission [:mention-count :alice :alice] :stood-on {fid-e1
  s1, tool-fid s0} :because-of (first fid-e1) :subjects #{} :name (run-name
  ...)`. Based-on is R4's form (fact ids with stamps). Pass.
- Step 4d: `client/lookup-many` (one `foreign-select` with `submap` on L's
  home, probed by the plan) → none → `offer-until-answered!`. Pass 2: both
  names found → `:recorded`, nothing offered (R2). Pass.
- Two runners at once (or a runner that crashed after its offer entered
  the depot and was decided after the restarted runner's lookup): both
  offer under one name. The parts digest covers `:claimed-when` and
  `:stood-on` (`client.clj:21-37` on the rig branch and in the locks
  build: "the digest covers `:claimed-when` and `:stood-on`, so a resend is
  this map, never a rebuilt one"), and `build` fills `:claimed-when` with
  `System/currentTimeMillis`. Two builds differ → the second is
  `:name-taken`, a face; the report says the run was refused when it ran.
  Safety holds (one output act per match), the plan's claim (6.3; 15
  "Production readiness") does not. FAIL → F5: `:claimed-when` derived (the
  millisecond of the later of the two stood-on stamps, via
  `rig.store.clock`) and `:session` fixed (the tool's default session,
  F6), so the whole parts digest is a function of the triple.
- Loop check, history H8: tool X `{:matches [:k :read/pattern] :out
  #{:note}}`. Static graph: `:read/pattern → :note`, no cycle, admitted.
  Pass k writes k's read entries (`:read/pattern` facts in L, by the
  operator and by each tool); pass k+1's read of X finds them as new
  matches; X runs on each; it writes read entries again. One generation per
  pass, for ever, driven by the runner's own writes, which the check does
  not see. FAIL → F7.
- Scale: a tool with more than n matches reads the same first page each
  pass (the pattern read takes no "start after", as built:
  `(foreign-invoke-query (:read-pattern store) layer for x as-of limit)`,
  `read_exit.clj:69-76`), so matches past n never run. The plan states it
  and reports the tool `:partial` as data, never complete; but the step
  the runner needs to work past n is not in the count. FAIL → F8 (counted
  as a deferred class-b step, b7, not built tonight; tonight's layers hold
  a handful of facts).

### 2.9 The machinery count's classes

Checked row by row (plan section 8):

- a1-a9 are fixed-side steps the frame named: each cites a ruling, the seed,
  or a validated plan's promise (the read exit's "Phase 6 replaces the
  constant with the key's grammar facts", phase 2's "Stage 6 reads the same
  map from facts"). a10 and a11 are named by decisions.md and SPEC.
  Correct class.
- b1 (`:grammar-change-needs-rebuild` and `:used`), b2 (the derived name,
  now with its derived `:claimed-when`, F5), b3 (the loop check, now over
  the runner's own writes, F7), b4 (the micro reorder and its hop): nobody
  anticipated them. Correct class.
- b5: resolved by F6 to no store step, given the locks build's
  uncommitted `client/stock!`; without it, one door step (a public pool
  refresh), class b. It stays in the table as a resolved non-step with its
  why, so the receipt shows it was examined.
- Uncounted before F8: `client/lookup-many`, a new door function (section
  10 lists it; section 8 did not). Class b, nobody anticipated. And the
  start-after on pattern reads, needed past n matches: class b, deferred.
- c: `:emit`, the revision reader's two built-in steps, six formulas.
  RIG.md calls the revision reader "one capability, class (c): two built-in
  steps a tool can call"; the plan counted two. Reconciled (F8): one
  capability, two vocabulary entries; the plan's unit ("one named code
  unit added ... to the vocabulary") counts entries, and the receipt says
  both.
- Not steps (checked): the exit needs no change for tools (`:reader-kind
  :tool` and `:rows?` are built, `reads.clj:72`, `read_exit.clj:48-66`);
  the grant is phase 1's `grant-offer`; the lease road needs none (F6).

The count as the plan now predicts it: (a) 11; (b) 5 built tonight (b1
b2 b3 b4 b6) plus b7 deferred, b5 resolved to none (one more if `stock!`
does not land); (c) 9 vocabulary entries (the revision reader is one
capability as two of them). 11 + 5 + 9 = 25 built tonight, one deferred. After them, the test tool and the
test grammar are facts only: the machinery count's zero.

## 3. The caching examination's outcome (brief's request)

EXAMINATION-copies.md (main rig worktree, read whole, *checked*): "Grammar:
no copy needed. Build the plan's pick A ... Road C ... does not survive"
(section 0); "Build A as the plan has it. Build no C and no F. Keep T-FR3
... marked first-record ... Keep F possible at no cost. An adoption is an
ordinary grammar fact in the layer, whose act stands on the source through
based-on (R4). Nothing is new in the envelope" (section 5); G "fails the
yardstick" (3.6).

Does the plan build A only? Checked against the build order (13), the
tests (7) and the writes (9): the only grammar rows are `[L :key-rows k]`
written by L's own gate; no `|all`, no hop, no adoption act, no test of F
("not built tonight, and no test needs it", 3.3 F). Yes. But the text
still carried C and F as "unsettled" copies awaiting builder A's
examination, and Q1 still offered G. FAIL → F1, recorded in 0, 3.2, 3.3,
3.4, 12 and 14.

## 4. Names the build must bind to after the merge

Committed now (*checked*: rig branch at `40374171`; locks build `5a5de4fe`;
micro names per the gatherer at `48562bde`, see 4.1). The plan's own copy of
this table is in its section 1 ([V-F10]):

| The plan's name | Bind to | Where |
|---|---|---|
| `reads/seed-hints` → `store-hints` | the seam is `reads/current-hints` (a function, "so the topology code calls it rather than embedding the constant"); the constant `reads/seed-hints` | `reads.clj:34-47`; called at `gate.clj:281` (decide*), `reads.clj:426` (`parse-pattern`'s default arity), `reads.clj:975` (a query topology) |
| the three places that take hints (a8) | exactly those three call sites | as above |
| `open-row>` | `reads/open-row>` (a `deframafn` over the pure `reads/open-row`); phase 2 has its own `locks/open-row>` and `open-value>`, joined behind the read exit's seam at the merge | `reads.clj:331-346`; `locks.clj:1291, 1320` at `5a5de4fe` |
| the store's read-entry keys | `reads/read-keys` `#{:read/point :read/pattern}` (FR6) | `reads.clj:68` |
| `read-exit/read!` | as built, spec keys `:reader :for :reader-kind :rows? :working :permission :layer :read :as-of :limit :role :entry-name`; `:read [:pattern p]` | `read_exit.clj:88` |
| `reads/parse-pattern` | as built, `(parse-pattern pattern limit as-of hints)` | `reads.clj:416-426` |
| `reads/index-writes` | as built, `(index-writes hints layer name log stamp)` | `gate.clj:281`, `reads.clj:181` |
| `grammar/grammars`, `grammar/subjects-of` | as built in the locks build | `grammar.clj:12-29` at `5a5de4fe` |
| `locks/read-values` | as built | `locks.clj:418` at `5a5de4fe` |
| wrap | `locks/wrap-of` | `locks.clj:253` at `5a5de4fe` |
| leasing (F6) | `client/stock!` (working copy, uncommitted at 04:00), else `client/lease!` plus a pool refresh; `client/default-session` | locks build `client.clj` 63-67, 293-308 at `5a5de4fe`; `stock!` at 310 in the working copy |
| the micro gate (F4, F9, F10) | `micro/arrival-open`, `micro/layer-rows`, `micro/prepare`, `micro/reason-order`, `rig.store.permit/refusal` | micro build at `48562bde` (`micro.clj` 58-70, 1112-1266) |
| the clock's millisecond (F5) | `clock/ms-of` | `clock.clj:33` |
| the offer builder (F5) | `client/build` (its `:claimed-when` and `:session` defaults) | both branches |
| `client/lookup-many` | new; the batch form of `client/lookup` | rig branch `client.clj:53-66` |
| `client/seed!` | as built | rig branch `client.clj:158` |

### 4.1 The builds in flight

From a read-only gatherer (Sonnet), spot-checked where a fix rests on it:

- **Read exit** (`Softland-rig-build-reads`, `00fd0468`, clean): merged into
  the rig branch as `40374171`; every source file byte-identical to the rig
  branch's. Names as in the table above. *Spot-checked*: `read!`,
  `check-call`, `entry-offer`, `current-hints` and its three callers,
  `open-row`/`open-row>`, `read-keys`, `reader-kinds`.
- **Phase 2** (`Softland-rig-build-locks`): committed to `5a5de4fe` when I
  read it (the gatherer saw `3ddc6eab` earlier), with `client.clj`,
  `inject.clj`, `locks.clj` and tests modified and uncommitted at 04:00.
  Bind to: `grammar/grammars` and `subjects-of`; `locks/read-values`,
  `wrap-of`, `person-owner`, `lease-row`, `lease-writes` (private),
  `lock-refusal` (L27's order), `max-lease`; `client/build` (session
  default `default-session`), `lease!`, `stock!` (uncommitted),
  `lease-locks`, `offer-until-answered!`. No `:grammar-*` keyword exists
  yet anywhere (grep over all four trees).
- **Phase 3** (`Softland-rig-build-micro`): committed to `48562bde` in the
  gatherer's read and `2d0cb13c` in mine, with `micro.clj` and
  `micro_client.clj` modified and uncommitted. Block 1 as built
  (`micro.clj:1112-1266` at `48562bde`): on the arrival task (the depot is
  `hash-by route-key`) the lease rows are read from `$$micro-names`,
  `$$persons` for their `:under` persons, then `arrival-open` opens and
  computes subjects with `grammar/subjects-of` (the constant); then
  `(|hash *name)` (1160) for the record and the name step; on the fresh
  path `(|hash *layer)` (1171) reads settings, the permission chain and
  heads from `$$micro` with `$$layers` fallbacks (`layer-rows`); then
  `(|hash *e)` per entity (1212); then `(|global)` (1259), where
  `+map-agg` gathers and the pure `prepare` decides every envelope in
  batch order and emits the writes that block 2a consumes. So the rows the
  open needs are two hops away from it, which is the plan's b4, and the
  fold that must see a batch's own uses (F4) is `prepare`, which sees every
  envelope's rows on one task. The refusal order both gates share is
  `micro/reason-order` (22 reasons, 58-70); `:grammar-change-needs-rebuild`
  goes after `:control-not-allowed` there too. The permission chain is
  `rig.store.permit`.
- **Revision reader** (rig branch): `rig.revision/read-units [repo rev path]
  [repo rev path opts]` → `{:rev :commit :path :cut :units}` or an error
  map; `read-span [repo rev path first-line last-line]` (+opts) → `{:rev
  :commit :path :unit}` or an error map (`revision.clj:735, 766`). The
  plan's two steps bind to these; its `:first`/`:last` arguments are the
  reader's `first-line`/`last-line`.

## 5. The template's checks

### Query topology: `read-pattern`, `read-point` (changed)

- Input examples present: yes (plan 9, "Query Topologies").
- `[:k :mention]`: N = +1 row read, M = 1 (it decides the hints and the
  marks). N == M.
- `[:e :e1]` over a page with 2 keys: N = +2, M = 2. N == M.
- M varies with the page's distinct keys; the plan marks it variable and
  reads each distinct key's row once in a `loop<-`. Pass.

### PState schemas

- One new field, `:key-rows`, in `$$layers` (stream) and in the micro
  store's per-layer state; no new PState, so no pair to merge. Option D (a
  separate `$$grammars` under the same leading structure and partitioner)
  was rejected for exactly the template's reason. Pass.
- No `Object`: the shape is canonical EDN text (a shape nests; the skill
  forbids `Object`), probed in Rama 1.6.0 (`runs/phase6-tools-rows-probe.txt`,
  the plan's). Pass.
- `fixed-keys-schema` for the uniform row, with a nullable `:grammar`
  sub-record (a nullable field on rows of one shape, which the template
  allows). Pass.
- `:key-rows` subindexed: nothing bounds a layer's keys. Pass.

### Partitioning

- No new partitioner; every read and write is on the layer's home (stream)
  or `hash(L)` (micro). Weighted seeks recomputed from the plan's rows:
  0.80 × 1 + 0.15 × 2 + 0.049 × 0 + 0.001 × 1 = 1.101 at N = 1, 16, 128:
  totals, flat in N. Proportions sum to 1.000. Pass.
- The micro row was missing (F9): added in the plan, +1 seek per distinct
  key at `hash(L)` and +1 hop per act, flat in N.

### Topologies

- No new topology. The runner is operator code: a topology cannot block on
  an offer's answer, and the one exit (RIG.md default 4) waits for its
  entry's answer before it shows anything (built, `read_exit.clj:88-126`).
  Pass. No choice rests on test synchronization. Pass.

### Production readiness

- Concurrent clients: FAIL before F5 (two runners → `:name-taken`, 2.8);
  pass after.
- Client restart: the runner keeps no state; names and content recompute
  (after F5). Pass.
- Worker restart: the gate's replay recomputes the rows or finds the
  record. Pass.
- Scale: rows per layer and key, subindexed. The runner past n matches:
  FAIL → F8 counts the step it needs; the pass is O(T × M) seeks, stated,
  with R6's standing read or a "since" bound as the road (plan 6.5). After
  F8 this passes as a stated bound, not as scale: the runner stays correct
  (one output per match, a partial read reported partial, never complete),
  and its progress stops at n matches per tool until b7 or a standing read
  lands. The spec asks for a minimal runner and tonight's proof stays
  below n; the phase that runs a real tool over a real layer needs b7 or
  R6 first.
- Stream non-idempotent writes: none added (whole-row `termval`s). Pass.
- Multi-partition stream writes: none added. Pass.

### Internal depots, cross-topology flows, `depot-partition-append!`

None added. Pass.

### In-memory state

None. The runner holds one page per tool. Pass.

### Minimality — adversarial simplification

Simplest design sketched: the grammar as a row per (layer, key) projected
by each gate from its own facts; the check at the open; hints from the
rows; a runner that reads `[:k :tool]`, reads each tool's pattern, and
offers outputs under names fixed by the match. The plan is that design.

- `:key-rows` row. Delete it: every decision reads the grammar through
  `:heads` and the log, two or more seeks per key (plan 9). Kept.
- `:used`. Delete it: H4 changes a used key's hints and leaves wrong
  indexes. Kept. Merge into a count of facts: a count is a non-idempotent
  increment; a boolean `termval` is idempotent. Kept as is.
- `:grammar-change-needs-rebuild`. Bypass by rebuilding on change: the
  paged rebuild exists (read exit F2) but a rebuild during live writes is
  its own design (Q5). Kept.
- The loop check. Delete it: H8 and a self-matching tool run for ever.
  Kept (after F7).
- Derived names plus `lookup-many`. Replace with a cursor fact per tool:
  one extra write per pass and a doubled output when a crash falls between
  output and cursor (plan 14). Constructed there; derived names win.
  `lookup-many` versus per-name lookups: T roundtrips against T × M. Kept.
- The runner's operator lease (F6). Replace with a person lock for tools:
  makes a tool a person (a subject candidate, forgettable as one). Rejected.
- The micro reorder: constructed against M2 in 2.2. Kept for the rule it
  keeps.

### Throughput — adversarial

The dominant operation is an offer's decision: +1 seek per distinct key,
local, no hop (stream). A cheaper design would read no row: a grammar
carried on the offer (road D, a new envelope part, refused by "Tools add
facts, never envelope parts") or a key id that commits to its grammar
(road E, still carried). Neither is admissible. Micro: +1 hop per act (M1)
against M2's zero, rejected for a security rule, costed (F9). Pass.

## 6. Spec coverage, operation by operation

### Grammar fact admitted (WR-G1)
- **Source**: "a key's grammar is facts about it" (SPEC 6).
- **Trace**: H1 A1: one fact, one row write on t, same event.
- **Faults**: worker restart before commit → replay recomputes the same
  row from the same state; after commit → the record path. Retry → same.
  Multi-partition: none.
- **Races**: two grammar acts for one key in one layer serialize on the
  home task; each decision sees the other or not; the later stamp is in
  force. Out of order at a partition: `(hash-by :layer)`, one partition.
- **Flaws**: H5 and H3 (F4).
- **Verdict**: FAIL → F4, fixed.

### Value checked by its grammar (the gate)
- **Source**: "grammar facts checked at the gate" (brief); ruling 6.
- **Trace**: H1 A2 `:value-shape`; G6 checked under the grammar before its
  act.
- **Faults/races**: as above; the value's grammar is derivable (stream by
  stamp; micro by `:batch`).
- **Flaws**: none found, with reasoning: the check is a pure function of
  the rows read in the same event and the opened value.
- **Verdict**: PASS.

### Subjects from the grammar
- **Source**: "which subjects its values name" (SPEC 6); ruling 8.
- **Trace**: 2.5.
- **Flaws**: F2 (overstated cost), F3 (a mark with no grammar, unstated).
- **Verdict**: FAIL → F2, F3, fixed.

### Opaque
- **Source**: "whether they are opaque" (SPEC 6); ruling 6 verbatim.
- **Trace**: H6.
- **Flaws**: none found, with reasoning: every consequence ruling 6 names
  has a code place (4.4) and a test (G3); the lock's reading is Sid's (Q3).
- **Verdict**: PASS.

### Index hints from grammar facts
- **Source**: the read exit's "Phase 6 replaces the constant with the key's
  grammar facts" (P:915-916, as the plan cites it).
- **Trace**: G4; the three built call sites of `reads/current-hints`.
- **Flaws**: F4 (same act, same batch); F10 (the seam's name).
- **Verdict**: FAIL → F4, F10, fixed.

### Tool fact
- **Source**: "A tool is facts too: what it matches, its signature, its
  read entry preference" (SPEC 6).
- **Trace**: 2.7.
- **Flaws**: F7 (store keys in `:out` or the pattern), F11 (wording).
- **Verdict**: FAIL → F7, F11, fixed.

### The runner finds tools by matching
- **Source**: "a minimal runner finds tools by matching" (SPEC 6).
- **Trace**: H7, 2.8.
- **Faults**: runner crash mid-pass → rerun recomputes names; the door's
  `offer-until-answered!` resends under the same name. Retry → recorded
  (after F5). Multi-partition: none (every act is in L).
- **Races**: two runners → recorded (after F5; `:name-taken` before).
  Tool revoked mid-pass → the entry is refused `:permission-revoked`, the
  exit hides the answer (R4 test).
- **Flaws**: F5, F6, F7, F8.
- **Verdict**: FAIL → F5, F6, F7, F8, fixed.

### The count
- **Source**: "Report the count and every step that was needed, with why"
  (SPEC 6); the three classes (brief).
- **Trace**: 2.9.
- **Flaws**: F8.
- **Verdict**: FAIL → F8, fixed.

### The revision reader as a capability
- **Source**: "the code that reads a file at a git revision into passages
  and functions, with content and position and no store identity" (brief).
- **Trace**: built and merged, 25 tests green on the merge (RIG.md, the
  build's run `runs/revision-reader-after-merge.log`); the plan adds two
  vocabulary steps binding `rig.revision/read-units` and `read-span`
  results to a step name, no store identity added.
- **Flaws**: none found, with reasoning: the steps pass the reader's data
  and its errors as data; nothing in the store gains an identity for a
  passage.
- **Verdict**: PASS.

## 7. What should go to Sid

1. **Where a key's grammar has authority** (Q1, Q2 as revised by F1):
   per layer, with adoption later as an act standing on its source (A, F
   later), or store-wide at every gate (only as a recorded read or a copy
   whose version each decision records; T-FR3 falls). Tonight builds A.
2. **A `:die-with-any` mark on a key with no grammar** (F3, the
   examination's question 5): admitted tonight, dying only with the owner
   and whoever the offer carries; or refused?
3. **The tool's lease** (F6): tonight the runner leases for a tool as the
   operator, bare rows in the tool's door session; with R7's cascade the
   lease becomes the session owner's. The session on a tool's acts is on
   the record.
4. **The run's `:claimed-when`** (F5): a tool's output claims the
   millisecond of its later input, so a rerun is a retry. With Q6 (a
   derived name, where the rule says "random").
5. **The leaves' language**: tonight's six formulas are the smallest set
   the test tool needs; decisions.md reserves "a designed total language"
   for when a record cannot say a needed tool twice over, "asked of Sid
   then". The thesis count will show whether that point comes.
6. **The micro reorder** (F9): one hop per act, to keep bare operator
   locks and plaintext on the arrival task; M2 saves the hop by carrying
   sealed lease rows to `hash(L)`, at the price of bare operator locks on
   the wire. Phase 3's, and Sid's if the rule is to bend.

## 8. What is checked, derived, assumed

- Checked: the verbatim spec, brief-quoted rulings and decisions at their
  lines on main; the examination whole; the built gate's refusal order;
  the built client's `build` defaults and digest note; the built read
  exit's `read!`, `check-call`, `entry-offer`, `current-hints` and its call
  sites, `reader-kinds`, `read-keys`, `open-row`; the locks build's
  `person-owner`, `lease-writes`, `persons-refusal`, `wrap-of`,
  `read-values`, `lease!`, `default-session`, `assign!`, `lease-for!` at
  `5a5de4fe`; the micro plan's block 1 and the record's `:batch`.
- Derived: every trace H1-H8, F4's in-batch hole, F6's road through the
  public `lease!`, the count.
- Assumed: 1 ms a hop; the probe the build runs for F6 will agree with the
  committed code once phase 2's uncommitted work lands.

PHASE_VALIDATION:minor-fail
