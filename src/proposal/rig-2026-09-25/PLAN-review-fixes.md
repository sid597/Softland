# Plan — step R, the fixes from the review of wave 1

Builder R (Claude Opus 5.5), reporting to builder C. Branch
`rig-build-fixes`, worktree `/mnt/data/projects/Softland-rig-build-fixes`,
cut from `c9684356`. The build sits on wave 2's merged tree (phases 4, 5's
rest and 6 on `rig-2026-09-25`), which had not landed when this plan was
written: every binding point below names the branch it comes from, and is
checked again on the merged tree before any code is written (see "Prep").

Sources, read whole: `REVIEW-wave1.md`; RIG.md's "Overnight state" and For
Sid 4 and 37; C's brief for step R. Read by window: the rama skill (SKILL.md,
phases.md, phase-build.md, phase-2-plan-validate.md, the plan and validation
templates); the code at `c9684356` (gate.clj whole; envelope.clj's control
keys, value slot and parse; micro.clj's control keys, parse, `micro-extras`,
`forget-effect`, `micro-decision`, `decide-envelope`; locks.clj's `seal`,
`lock-refusal`, `forget-writes`; client.clj's door; micro_client.clj's door;
reads.clj's fingerprint and entry; read_exit.clj's `entry-offer` and `read!`;
`review_wave1_test.clj` whole); the three wave 2 branches' notes ("Shared-file
changes", R-1 and R-2 passages) and their code at the same seams by `git
show`; the formal model's control keys (model.clj 54 to 57), `apply-control`
and `admit` (467 to 506) and `op-layer-setting` (943 to 953).

## What step R is

C's brief, four items:

- **R-1.** Refuse control-key facts the gate does not act on, at both
  gates (stream and micro), extending W1-7, which refuses `:members` at the
  stream gate.
- **R-2.** Store a `[:kv]` pattern's value keyed in the old entry form too.
  The seam is `reads/recorded-pattern`; `rig-build-reads-rest` already does
  it for new entries.
- **F-1's last edge.** A lease answered yes whose `take-locks` times out
  still seals under no lock. Fix it so the door never seals under no lock.
- **The review's pending tests** (`RIG_PENDING=1`) become ordinary
  assertions.

The roads are C's (For Sid 37: "The build will refuse such facts at both
gates in step R, after wave 2's merge, extending W1-7"; For Sid 4: "The
build will store the pattern's value keyed, like the fingerprint"). Both
questions stay Sid's; this step builds the default each names, and each
change narrows what a record may carry without adding a record form, except
R-2's line form, which is reads-rest's placeholder carried to one more line.

## Prep

**Where prep stopped (26 September, 08:10 IST).** The plan is written
(07:30) and validated by a fresh session, the rama skill's phase 2
(`PLAN_VALIDATION-review-fixes.md`: minor-fail, sixteen localized edits
made in place, V1 to V8; this builder then corrected the `:grammar` row
from BUILD_NOTES-wave2.md and wrote this paragraph). No code is written.
Drafts of the tests sit outside the repository until the merge. Wave 2
landed on `rig-2026-09-25` at `27543fd7` (C, 07:50: one suite, 148 tests,
7,699 assertions, 0 failures); its notes confirm the R-1 set this plan
names (the four setting keys about another entity at the stream gate,
`:owner` about another entity at the micro gate), that `entry-facts`
still records a plain `[:kv]` pattern for one-owner and shared entries,
and that F-1's edge is unchanged. Next: the three steps below, then the
build.

The steps before any code (from the plan validation):

1. **The merged tree.** Take wave 2's merged tree into `rig-build-fixes`
   by a merge commit before any code (`git merge --no-edit
   rig-2026-09-25` once C's merge of wave 2 has landed there; no reset,
   checkout, restore or stash). At the plan validation (26 September) the
   merge stood on branch `rig-wave2` at `0edf1dac` ("Wave 2 merge, 3 of
   3"), its suite in flight; the binding points below were checked there
   and hold, except where this plan now says otherwise.
2. **The binding points, again on that tree, before any code:** gate.clj
   `setting-fact?`, `refusal-with-rows`' `:malformed-control` clause and
   `refusal`'s two arities, `decide`/`decide*`; gate_event.clj's one call
   to `gate/decide`; micro.clj `control-keys`, `foreign-control-keys`,
   `micro-extras`, `micro-decision`'s call to `gate/refusal`; client.clj
   `offer!`, `offer-until-answered!`, `sealed`; micro_client.clj `offer!`,
   `write!`, `seal`, `take-locks`; reads.clj `recorded-pattern`,
   `pattern-answer`, `entry-facts`; shared_reads.clj `shared-pattern>`
   (through `pattern-answer`); read_exit.clj `entry-offer`, `read!`;
   `review_wave1_test.clj` as merged (W2-2's toy grammars) and
   `reads_test.clj`'s `entry-facts` test. If one differs from what this
   plan says, stop and tell C before writing code.
3. **The setting-fact search again** on that tree (`src/` and `test/`,
   never `gate_test.clj`): every map with `:k` one of `:kind :owner :class
   :lock-grain`, and every generator of those keys, its entity against its
   act's layer (R-1, "Why the doors as well as the gates").

## The merged tree at these seams (what wave 2 brings)

From each branch's "Shared-file changes" and its code, read by `git show`:

- **envelope.clj.** Phase 6 (`rig-build-tools` `2d2f666a`) adds `:grammar`
  to `control-keys`. Phase 4 (`rig-build-promotion` `5303418f`) changes
  `parts-fact` (a landing's `:box`) and `landing-name` (a 3-arity). Nothing
  of step R's.
- **gate.clj.** Phase 4: `exempt-actors` gains `:store`; `control-fact?`
  gains `:promote-request` and `:crossed`, with value checks
  (`promote-shape/request-value-ok?`, `crossed-value-ok?`); a `:crossed`
  fact is the store's own step's alone (`control-allowed?`); a promotion
  request is its act's one fact. Phase 6: `refusal` becomes a 4- and
  5-arity over a private `refusal-with-rows` (the 4-arity is the micro
  fold's), `:grammar` joins `control-fact?`, `store-key?` is new. Phase 5's
  rest: `:session-closed` takes an optional `:reads`. So on the merged tree
  `:promote-request` and `:crossed`, two of the six keys the review found
  admitted at the stream gate, are already refused unless well formed and
  in place; step R does not touch them.
- **gate_event.clj** (new, `1b1d416c`, phases 4 and 6): the gate's
  record-or-decide path, moved verbatim out of module.clj. It calls
  `gate/decide`, so a clause added to `gate/refusal` reaches it unchanged.
  Step R does not edit it.
- **micro.clj.** Phase 4 and phase 5's rest change the fold and the lease
  road; neither changes `control-keys`, `foreign-control-keys` or
  `micro-extras`' control clauses. The micro fold decides stage 1's list by
  `gate/refusal` (`micro-decision`), so the same clause reaches it.
- **reads.clj.** Phase 5's rest adds `recorded-pattern` (a `[:kv k v]`
  pattern as `[:kv k {:keyed hex}]`, an HMAC under the fingerprint secret,
  label `softland.read-pattern/1`), puts it on every pattern answer as
  `:recorded-pattern` (`pattern-answer`, which the shared read also calls),
  and records it in a standing read's opening line (standing.clj). Its
  `entry-facts` still records `(:pattern answer)`. Phase 6 changes the
  query topologies and hints, not `entry-facts` or `pattern-answer`.
- **client.clj.** Phase 6 adds `lookup-many`; the door (`offer!`,
  `offer-until-answered!`, `assign!`, `sealed`) is unchanged.
- **micro_client.clj.** Unchanged by all three (each branch's notes say so).
- **Tests.** `reads_test.clj` is changed by phases 5 and 6 (counts, hints),
  not in its `entry-facts` test; `review_wave1_test.clj` by none of the
  three branches, but by the merge (`rig-wave2`, W2-2): its seed writes the
  toy grammars into `:alice :alice-hand :alice-agent :base` (`tg/write!`,
  which R-2's `[:kv :note ..]` read of the base needs, `:note` indexed by
  value), and the O11 probe into `:rv-pl`. Step R's edits keep those lines.

## R-1: a control-key fact no gate acts on

### What each gate acts on, per control key (the merged tree)

The envelope's `control-keys` decide which facts keep a plaintext value and
carry no lock. A control-key fact's value is kept as plaintext wherever it
is admitted, so every admitted one must be a fact a gate acts on, whose
value has the shape the gate checks ("their values are ids and settings",
model.clj 55).

| key | stream gate | micro gate |
|---|---|---|
| `:kind` `:owner` `:class` `:lock-grain` on the layer's own entity | acts (P10: making, owner, re-class, grain) or refuses (`:layer-already-made`, `:unsupported-reclass`, `:control-not-allowed`) | the same, through `gate/refusal`, plus `:unsupported-reclass` for a made layer (O9) |
| the same four on any other entity | **admits, plaintext, acts on nothing** | **admits, plaintext, acts on nothing** |
| `:permission` `:revoke` | acts (grant or revoke by the value's id); value shape checked | the same (`micro-decision`) |
| `:forget` `:lease` `:session-closed` | acts; shape checked, and for a lease or a session close its entity too (`control-value-ok?`; a forget's entity is not checked, and its value is a fact id) | acts; `micro-extras` and `gate/refusal` |
| `:person` `:forget-person` | acts in the store layer only; elsewhere refused | refused (`foreign-control-keys`) |
| `:members` | refused `:control-not-allowed` (W1-7) | acts on the layer's entity in its making act; else refused |
| `:promote-request` `:crossed` | acts (phase 4); shape checked; `:crossed` the store's alone | refused (`foreign-control-keys`) |
| `:grammar` | acts (phase 6) | refused until 6b (micro.clj keeps its own `control-keys` without it): the micro door seals it as a value and the gate refuses it `:malformed-control` (phase 6's `grammar-test`, BUILD_NOTES-wave2.md); sent plaintext, `:not-sealed` on its face |

After wave 2, the one row still admitted and never acted on is the second:
a setting key about another entity. It is the same row at both gates,
because both take their settings rule from `gate/setting-fact?` (P10: "a
fact on the layer's own entity with a setting key").

The model's reading differs, and neither gate follows it. The model has
seven control keys and no `:kind` or `:owner` key at all, so there a
`:kind` or `:owner` fact about another entity is a sealed value; and its
`apply-control` acts on `:class` and `:lock-grain` whatever the fact's
entity (model.clj 467 to 470). The model's own acts put every setting fact
on the layer's entity (`op-layer-setting`, 943 to 953), so no fixed
history writes one elsewhere, and phase 8's replays cannot see the
difference. It goes to RIG.md as a place the rig differs from the model.

### The change

1. **gate.clj, beside `setting-fact?`: `misplaced-setting?`** `[offer f]`,
   true for a fact under a setting key (`env/setting-keys`) whose entity is
   not the act's layer. Pure and total (facts are parsed maps; it reads
   `:k`, `:e` and the offer's `:layer`).
2. **gate.clj, `refusal-with-rows` (the merged `refusal`'s body):** the
   `:malformed-control` clause gains `(some #(misplaced-setting? offer %)
   facts)`. Both gates call `refusal`: the stream gate through `decide`
   (called from gate_event.clj's `record-or-decide>`), the micro gate
   through `micro-decision`'s 4-arity call. One clause, one reason, both
   gates, recorded like every other `:malformed-control` (the name's answer
   record, reason `:malformed-control`; no row, no index entry, no heads, no
   settings). The reason matches how both gates already answer a control
   fact in the wrong place: a lease not about its session, a person fact
   not about its person, `:members` off the group's entity at the micro
   gate. W1-7's `:control-not-allowed` stays for `:members` at the stream
   gate: that is a key this gate never acts on, not a key out of place.
3. **The doors refuse to send one** (client.clj, used by both doors):
   `refuse-misplaced!` `[offer]` throws an `ex-info` whose data is `{:door
   :refused :reason :malformed-control :name nm :misplaced [[e k] ...]}`
   before anything is sealed, leased or appended. Called first in
   `c/offer!` and `c/offer-until-answered!` (the stream door: the second
   retries every exception as an append error, so it must throw before its
   loop), and in `mc/offer!` and `mc/write!` (the micro door: `write!`
   right after it builds the offer, before it reads the grain or leases).
   It is total over whatever a caller hands a door: it looks only when
   `:facts` is sequential, reads each fact by keyword (nil for anything
   that is not a map), and leaves everything else to the gate's parse, as
   `value-indices` does. The raw roads stay open for tests that play a client
   that is not the door: `foreign-append!` on the stream depot, `mc/send!`
   then `mc/await-answer` on the micro depot.

Why the doors as well as the gates: the review's micro block asserts "no
plaintext value in the micro depot", and C's brief makes it an ordinary
assertion. A depot keeps what is appended; the gate refuses after the
append. So "no plaintext of such a fact in a depot" is a property only the
sender can give, and the honest door is the sender. A setting key about
another entity is out of place for every gate, so no door has a reason to
send one; refusing it at the door needs no knowledge of which gate orders
the layer and changes nothing any caller relies on (checked on the merged
tree `rig-wave2` `0edf1dac` and the three branches: every setting fact
built in `src/`, and every one a test sends through a door, is about its
layer's own entity; the others are the review's own R-1 blocks, which this
step rewrites, and `micro_prepare_test.clj`'s generators (`gen-e` ×
`gen-k`, 704 to 733), which build setting facts about other entities but
feed only `micro/intake`, `prepare` and `parse-micro` under totality
properties (`nothing-throws`), never a door, and assert no decision).

The door throws rather than answering, as the stream door does when it
cannot lease (`assign!`: "the door could not lease enough locks"). An
answer shaped like the gate's would be the door speaking for the gate; a
throw says nothing was offered, and the name stays unused.

### Scenarios

- **Stream, the honest door.** Alice, holding `[:alice :alice :alice]`,
  calls `c/offer-until-answered!` with `{:e :rv1 :k :owner :v "rv1 owner
  text"}` in `:alice`. `refuse-misplaced!` throws `{:door :refused :reason
  :malformed-control :misplaced [[:rv1 :owner]]}`. Nothing is appended:
  `*offers` holds no record under the name, and no record anywhere holds
  the text.
- **Stream, a client that is not the door.** The same map appended raw to
  `*offers`. `env/parse` takes it (a control key, plaintext `:v`, well
  formed). `decide` → `refusal`: not outside the layer, the layer is made,
  the class matches, Alice's permission covers it, then
  `misplaced-setting?` → `:malformed-control`. The record is written as a
  no; no log row, no `:ix-ek`/`:ix-ke`/`:ix-kv`/`:ix-of`/`:ix-s` entry (the
  index writes are made only for a yes, `decide*`). The exit's `[:e :rv1]`
  shows nothing. The owner's forget of `[nm 0]` answers `:no-such-value`
  (`lock-refusal`: no row of the layer's log at the target). The depot
  keeps the client's own record, plaintext: see "Not done".
- **Micro, the honest door.** Bob in session `:rvs` of `:group` calls
  `mc/write!` with `{:e :rv1m :k :owner :v "..."}`. `refuse-misplaced!`
  throws before the lease. `*micro-offers` holds nothing under the name.
- **Micro, a client that is not the door.** The same envelope by
  `mc/send!`. `parse-micro` takes it (micro's own `control-keys` hold the
  four setting keys). The fold: `micro-decision` → `gate/refusal` →
  `:malformed-control`; `first-in-order` keeps it (no earlier reason:
  `micro-extras` has nothing for this key). The name row and the answer are
  written (a no), no row on `:rv1m`, no index entry (block 2d writes only
  for a yes), no lease consumed (none cited). `mc/await-answer` returns
  `{:answer :no :reason :malformed-control}`.
- **Unchanged.** `{:e :alice :k :lock-grain :v :per-act}` by Alice in
  `:alice` is still admitted (a setting fact); `{:e :alice :k :owner :v
  :bob}` by Alice still `:control-not-allowed`; by the operator still
  `:layer-already-made`. The four keys on a new layer's own entity in its
  making act still make it.

### What stays as it is

- W1-7 (`:members` at the stream gate, `:control-not-allowed`), phase 4's
  `:crossed` and `:promote-request` rules, the micro gate's
  `foreign-control-keys`: all unchanged; the tests assert each once more.
- **O1** (the review's sub-question under R-1): a forget of a fact with no
  lock (a control fact the gate acted on, or a retract) still answers yes
  with `:how nil` and erases nothing. After R-1 every admitted control fact
  is one a gate acts on, in its place; that is not the same as holding no
  text. A `:grammar` value's shape may carry strings (`[:enum v ...]` takes
  up to 64 EDN values each, `shape.clj` 12 and 79 to 80), written by the
  layer's owner or the operator, and a promotion request's `:public` is 32
  bytes the requester chooses; a forget of either answers yes and erases
  nothing. This goes to the notes under For Sid 37's sub-question. Whether
  such a forget should be refused, and whether a grammar may carry text,
  stay Sid's (OP9, O1).

### Not done, and why (for "For RIG.md")

- **The honest door still appends a control fact whose value is out of
  shape.** Free text under `:forget`, `:permission`, `:promote-request`,
  `:crossed`, or `:members` sent to the stream gate, is refused by the gate
  (`:malformed-control` or `:control-not-allowed`), but the door has
  already appended it, so the text sits in the depot beyond every forget.
  Closing it means the door checks every control fact with the gate's shape
  rules before it sends, which changes the door's contract for every control
  fact, and the tests that check the gates' refusals of malformed control
  facts send them through the door (stream_gate_test, forget_test,
  micro_test, promote_test, tools_test). A question for Sid, not built.
- **A client that is not the door can put its own text in a depot.** The
  gate refuses it and keeps no row, but the depot keeps the record, and the
  record's parts digest (a keyed digest over the act, control values
  included) stays in the refused name's answer record. The same holds today
  for a plaintext value fact from such a client (refused `:not-sealed` on
  its face). This is the depot's nature (every append is kept until a
  trim); the forget ruling reaches what the store admitted. Named, not
  built.

## R-2: a `[:kv]` read entry keeps its value keyed

### The change

**reads.clj `entry-facts`, its `:pattern` case:** the line's `:pattern` is
the answer's `:recorded-pattern`, the pattern as the module keyed it
(`[:kv k {:keyed hex}]` for a `[:kv k v]` read; every other form as it
is). A pattern answer that carries no `:recorded-pattern` gives no entry
facts (nil, as `entry-facts` already gives for anything it cannot make).
Nothing else in the line changes: moment, role, mark, count, fingerprint,
the secret's id, `:max-stamp`, the exact list, `:own-row`.

- Every pattern answer the exit can hold carries it: the one-owner
  `read-pattern` query ends in `pattern-answer`, and the shared read
  (`shared_reads.clj`, `shared-pattern>`) calls the same `pattern-answer`
  (reads-rest, `33817364`).
- `entry-facts` runs in the exit (`read_exit.clj` `entry-offer`), the
  client's side. The keyed form is computed in the module, where the secret
  is, and the exit takes it from the answer. The exit never calls
  `recorded-pattern` itself: in the rig the function is in the same JVM,
  but by the rig's own rule the fingerprint secret "never leaves" the
  module's functions, and a store core would have no secret in the client.
- Strict rather than a fallback to `(:pattern answer)`: a fallback would
  write a `[:kv]` value's text into a line whenever an answer lacked the
  key, which is the failure R-2 closes. With nil facts the exit's entry
  offer is an empty act, refused on its face (`:empty-act`): nothing
  recorded, nothing shown, `read!` answers `{:refused :empty-act :entry
  nm}`. No module answer takes that road; a hand-made answer in a unit test
  does.

### Scenario

The review's block: `:rv-b` writes `"rv2 a value about rv-b alone"` into
the base (one-owner on the stream gate, default 6). Alice reads it with
`[:kv :note "rv2 ..."]`: the answer's rows match; its `:recorded-pattern`
is `[:kv :note {:keyed h}]`, `h` the HMAC-SHA256 under the fingerprint
secret of `"softland.read-pattern/1\n"` plus the value's canonical text.
The exit writes Alice's entry with that line, sealed under her working
layer. `:rv-b` is forgotten. Alice's entry read back holds `{:pattern [:kv
:note {:keyed h}] :exact [[fid stamp]]}`: the fact id beside a keyed digest,
no text. The forget reaches nothing more; whether anything more must be
reached is the caveat below (For Sid 4).

### First-record, and a caveat for Sid (For Sid 4)

The line form `[:kv k {:keyed hex}]` is reads-rest's placeholder (its label
and its HMAC), now in every `:read/pattern` line as well as the standing
read's opening line. For Sid 4 stays open, and the notes will add one fact
to it: a keyed value stops a reader reading the value off a line, not
confirming a guess. The module keys any `[:kv]` read's value the same way,
so a reader who can read an old line can issue `[:kv k guess]` (against any
layer she can read where the key is indexed by value; the answer need not
match anything) and compare her new
line's digest with the old one. For a short or guessable value that
confirms the forgotten value. The forms that confirm nothing: keep nothing
of the value (a line that cannot re-run), or key it under a salt the reader
cannot choose again (the entry's own name, if the module mints it). Built
as C's brief says, keyed; the caveat goes to Sid.

## F-1's last edge: the micro door never seals under no lock

### The change (micro_client.clj)

**`seal`**, the door's one sealing function: when `locks/seal` gives nil
for a value fact (no lock for the id it cites, or not a lock), `seal`
throws `{:door :no-lock :name nm :lock-id lid}` (the lock id's first part
names the lease). So no caller of `seal` can build an envelope with
`:sealed nil`, whatever path it took, and `write!`, after a lease answered
yes whose locks it could not take (all of them or some), throws from
`seal` and sends nothing, as the stream door throws after its tries
(`assign!`). `write!` gets no lock check of its own (R-1's door check is
another matter): a second one would guard the same condition twice (plan
validation, minimality). The lease stays
answered yes; its unconsumed rows go when the session closes (default 1's
rule), as a stream door's unused pool does. Every caller in `src/` and
`test/` passes a lock for every id it assigns (checked: micro_test's
missing-lock cases seal under `{never fresh-lock}` and under `(:locks r)`,
which holds the id; every other call in micro_test and micro_prepare_test
seals under the locks of the lease whose ids it cites, and each expects an
answer other than `:not-sealed`, which a nil seal would give).

A refused lease keeps F-1's fix (throwaway locks citing the ids the refused
lease would have minted, so the gate answers `:no-such-lock`); only a lease
answered yes whose locks the door cannot take changes.

### Scenario

Bob writes one value in `:group`. The lease is answered yes; the micro
store is slow and `micro-lease` shows no locks within 30 s, so `take-locks`
gives `{}`. Before: `seal` sealed under `(get {} lid)`, `locks/seal nil ..`
gave nil, the envelope carried `:sealed nil`, the gate refused it
`:not-sealed` on its face. Now: `seal`, called by `write!`, throws
`{:door :no-lock ...}`; `*micro-offers` holds nothing under the act's name
and no answer is recorded; the caller may write again (a new lease). With two values under
per-value grain and one lock taken, the same.

## Tests

**`test/rig/store/review_wave1_test.clj`** (the review's namespace, one
cluster). The `pending?` switch and the OBSERVED-only branches of the two
first-record blocks go; the ns doc says why. Every assertion below is
ordinary:

- **R-2**: `pairs?` false; the line's pattern is `[:kv :note {:keyed
  hex}]` and equals `(reads/recorded-pattern [:kv :note text])`; the text
  is nowhere in the entry (`text-anywhere?`); the two "holds" assertions
  stay.
- **R-1 at the stream gate**, three parts. (a) The honest door: the four
  setting keys about `:rv1` in `:alice` each throw `:malformed-control` at
  `c/offer-until-answered!`, and no record in `*offers` under its name (the
  ex-data's `:name`) holds its text. (b) A client that is not the door (raw
  `foreign-append!`, under fresh names, since these records carry the same
  texts): each of the four is answered `[:no :malformed-control]`, no log row, no index
  entry holds its text, the exit's `[:e :rv1]` shows none of them, and the
  owner's forget of one answers `[:no :no-such-value]`. (c) Through the
  door, `:promote-request` and `:crossed` (phase 4) are refused
  `:malformed-control` and `:members` (W1-7) `:control-not-allowed`, none
  kept in a row or an index entry; their depot copies are printed as
  OBSERVED (the question in "Not done").
- **R-1 at the micro gate**: the honest door's `mc/write!` of `:owner`
  about `:rv1m` in `:group` throws and `*micro-offers` holds no record under
  its name (the ex-data's `:name`) with its text (the review's "no
  plaintext value in the micro depot", for the door's own act: the raw
  sends after it put their own records there, "Not done"); each of
  the four setting keys sent raw (`mc/send!`, `mc/await-answer`) is
  answered `[:no :malformed-control]` with no row on `:rv1m`.
- **F-1's last edge** (new, micro side, after F-1): with `mc/take-locks`
  redefined to give `{}` (its timeout's result), `mc/write!` of one value
  throws `:no-lock`; the lease its lock id names is answered yes
  (`mc/lookup`); no record in `*micro-offers` under the act's name (the
  ex-data's `:name`); no answer under it. With
  `take-locks` giving one lock of two under per-value grain, the same.
  `with-redefs` on a client function stands in for the timeout: the door's
  answer to "no locks after the deadline" is what is tested, not the 30 s.

**`test/rig/store/review_fixes_test.clj`** (new, pure, no cluster):
`gate/misplaced-setting?` over the four keys on and off the layer's entity
and over other keys; `gate/refusal` (4-arity, the micro fold's) and
`gate/decide` (the stream gate's, empty lock context) give
`:malformed-control` and no log for each misplaced key, and still admit
the owner's own grain switch; `micro/micro-decision` gives
`:malformed-control`; `refuse-misplaced!` through `c/offer!`,
`c/offer-until-answered!`, `mc/offer!` and `mc/write!` with a nil store
throws the door's `ex-info` (`:door :refused`) before any store access;
the stream door's second call takes one try (`(c/offer-until-answered!
nil o 1)`) and `mc/offer!` a short timeout (`(mc/offer! nil o 1)`),
because without the check the first retries for 15 s and the second
polls to its timeout rather than throwing; `mc/seal` throws with no lock
for a cited id and seals with one; `mc/write!` with `lease!`,
`take-locks` and `offer!` redefined and `:grain` given (else it reads the
grain from the store): a yes lease with none or too few locks throws
`:no-lock` and never calls `offer!`, a refused lease still
calls `offer!` with every value sealed (F-1's fix); `reads/entry-facts`
over `reads/pattern-answer` for a `[:kv]` read records the keyed pattern
and no text, and over an answer with no `:recorded-pattern` gives nil.

**`test/rig/store/reads_test.clj`**, its `entry-facts` test: the
hand-made answer `pans` gains `:recorded-pattern [:e :e0]`, as the module's
answers carry it, and the `[:kv ...]` case its keyed form. No other test
file changes.

**Runs** (Sid's rule: each once; after a fix, only what it touches). One
run under the cluster lock, from the rig folder:

```
flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:test rig.store.review-fixes-test rig.store.review-wave1-test rig.store.envelope-test rig.store.grammar-test rig.store.reads-test rig.store.micro-prepare-test rig.store.stream-gate-test rig.store.read-exit-test rig.store.micro-test
```

Why these: the two namespaces whose assertions this step writes or
changes; the tests of the four source namespaces it changes (gate.clj and
envelope's shared decision: envelope-test, stream-gate-test, and
grammar-test, the one direct test of phase 6's conditions in the same
`:malformed-control` clause the step extends, pure and seconds long; reads.clj's
entry: reads-test, read-exit-test; the micro door: micro-prepare-test,
micro-test; the stream door: stream-gate-test). The other namespaces load
these files but never send a setting key about another entity, never write
a `[:kv]` read entry that a test reads back, and never reach a lease whose
locks cannot be taken (checked by search), so their outcome cannot change.
The full suite is the orchestrator's, at the end of the night.

## The module, by the plan template's sections

- **Reads, writes, depots, PStates, topologies, query topologies:**
  unchanged. No schema, depot, partitioner or topology changes; no new read
  or write in any event.
- **Topology code touched:** one pure clause inside `gate/refusal`, which
  runs inside the stream gate's event (`decide`, on the layer's home task,
  after the reads it already does) and inside the micro fold (`prepare` on
  task 0, a pure fold). It reads only the offer already in hand: O(facts)
  comparisons, no seek, no iteration of a PState, no yield needed. It is
  total (a refusal is data), so neither the stream event nor the
  microbatch can throw on it; a microbatch that throws deterministically
  retries for ever, so totality is the property that matters here.
- **Partitioning, state primitives, resources:** unchanged. A refused act
  writes less than an admitted one (no rows, no index entries).
- **Retry and restart:** the clause is a pure function of the offer, so a
  replayed event decides the same; the doors' checks are client-side and
  stateless (a restarted door refuses the same offer the same way).
- **Concurrency:** no shared state is added. Two clients sending misplaced
  settings concurrently are each refused on their own record.

## Design difficulty log

- **Door or gate only (R-1).** Contested. Gate only is smaller and matches
  W1-7, but leaves the review's micro assertion failing, since the depot
  keeps the append. Settled by where the property can live: only the
  sender can keep a depot clean, and a setting key about another entity is
  out of place at every gate, so the door check is exact, needs no gate
  knowledge and breaks no caller. The wider door check (every control
  value's shape) was weighed and not built: it changes every control
  fact's road and the tests of the gates' refusals; it goes to Sid.
- **The reason (R-1).** `:malformed-control` over `:control-not-allowed`:
  the key is the store's, its position is wrong, which is what the gates
  already call malformed for leases, persons and members. Not close.
- **Road 1 against road 2 (R-1).** Not reopened: C's brief and For Sid 37
  name road 1; road 2 (seal it as a value, the model's reading for `:kind`
  and `:owner`) stays Sid's alternative and is written down again in the
  notes.
- **Strict or fallback (R-2).** Close. The fallback keeps hand-made test
  answers working unchanged; strict keeps the secret on the module's side
  and cannot write text into a line. Strict, and one unit test's answer
  gains the key the module's answers carry.
- **Throw, or seal under throwaway locks (F-1's edge).** Throwaway locks
  citing a yes lease's real ids would reach the gate and be refused
  `:does-not-open` and recorded, taking the act's name for a door-side
  failure, and consuming the lease. Throwing sends nothing and leaves the
  name free, as the stream door does. Not close. The throw sits in `seal`
  alone (plan validation): a check in `write!` as well would guard one
  condition twice, under two names for one failure.
