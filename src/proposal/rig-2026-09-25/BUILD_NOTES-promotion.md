# Build notes — stage 4, "promotion"

<!-- The rama skill's build step (phase-build.md) for PLAN-promotion.md as
validated (PLAN_VALIDATION-promotion.md, minor-fail, F1 to F9 applied in
place). Worktree /mnt/data/projects/Softland-rig-build-promotion, branch
rig-build-promotion, made off rig-2026-09-25 at 9730010c. Claude Opus 5.5,
effort max. Vocabulary: "key" is a fact's key; "lock" is an encryption
key. -->

## Prep (26 September, 05:27 IST, before wave 1 lands)

Part 1 of the build: loaded, read wave 1's code as it stands, noted what
the plan binds to, and stopped. Nothing was built and no cluster was run.

**Loaded.** The rama skill (SKILL.md, references/phases.md,
references/phase-build.md; stream.md's commit boundaries and `ack-return>`
read for the continuation's shape). SPEC.md. RIG.md: "Overnight state",
"For Sid" (5, 11, 20 to 22 are promotion's), "Defaults taken overnight"
(1 sealing, 5 permissions, 6 the base), "Rig choices so far".
PLAN-promotion.md whole; PLAN_VALIDATION-promotion.md whole. On main, read
only: PROGRESS.md "Now" (lines 9 to 230). The model's promotion code
(model.clj `exempt?` 76, `refusal`'s read-out branches 444-452,
`send-read-out` and `forward` 592-648, `promotion-status` 981-1000;
scenarios.clj `b-cases`). The plan's probe and the validation's X25519
probe (runs/phase4-*). PLAN-replays.md's table "The APIs it needs, by
stage" (phase 4's rows). Sid's build instructions in the relay folder.
PLAN-reads-rest.md says phase 4 owes it nothing (line 1422);
PLAN-tools-and-grammars.md's changes to the stream gate's event (its a2,
a3: key rows read and written in the event) matter below.

**Wave 1 as read.** `/mnt/data/projects/Softland-rig-wave1`, branch
`rig-wave1`, head `57c46159` plus uncommitted edits to `gate.clj`
(`stream-refusal`, between `refusal` and the lock reasons), `locks.clj`
(`open-row>` split into `open-row-with>` and `open-row>`, the latter's
signature unchanged), `reads.clj` and `micro_test.clj`. It may still
change before it lands; part 2 re-reads every point below after the
merge. Line numbers are the wave 1 working tree's at 05:27.

### Binding points

Marks: **as planned** (exists under the plan's name and does what the
plan needs), **differs** (exists; the build adapts as said), **to add**
(missing; the build adds it where said). Each was checked by reading the
code named, not by running it.

**Phase 1's envelope and stream gate.**

| # | Point | Where | Status |
|---|---|---|---|
| 1 | `env/crossing-name [req]` → `[L nil :crossing u]` | envelope.clj:266 | as planned |
| 2 | `env/landing-name [req target]` → `[T :by-entity :landing u]` | envelope.clj:272 | differs: 2-arity only; the plan's `(landing-name req target class)` is added as a 3-arity and the 2-arity kept (envelope_test.clj:577 calls it) |
| 3 | `:crossed` a placed key (the crossing's name carries class nil by `tag-of`); `:promote-request` and `:crossed` control keys (plaintext values) | envelope.clj:41, 51 | as planned |
| 4 | parse refuses `:crossing`/`:landing` on `:stream`, only `:crossing` on `:micro`; `:who :store` refused on both depots | envelope.clj:371, 387 | as planned |
| 5 | the parts digest `parts-fact` replaces `:sealed` by `true` and drops `:lock-id` | envelope.clj:198 | to add: `:box` must be replaced too. `canonical` prints a byte array by identity (`#object[...]`; checked: two equal arrays print different text), so a digest over box bytes would differ between two reads of the same landing and turn every resend into `:name-taken` |
| 6 | `gate/decide [offer settings rows heads clock wall digest lx]`, pure and total | gate.clj:409 | as planned; the crossing goes through it. The read-out's own reason reaches it as `(:reason (:read lx))`, which `locks/lock-refusal` places first among the lock reasons (locks.clj:730), after stage 1's list, so a re-classed source layer is `:class-mismatch` first (F8) |
| 7 | `gate/exempt-actors #{:operator}` | gate.clj:40 | differs: the crossing's `:who :store` is not exempt, so `refusal` would give it `:permission-does-not-cover-this` (permit.clj:44). Add `:store`, as the model's `exempt?` has it; safe, since both depots refuse `:who :store` on its face |
| 8 | `control-fact?`, `control-value-ok?` (a `case` with no default), `control-allowed?` | gate.clj:59, 80, 104 | to add: clauses for `:promote-request` (the request's value check, F1 and F2) and `:crossed` (the store's only); `:lease` takes `{:count 1 :landing L*}` with L* bound to the lease's own name (F1) |
| 9 | `stamp-for`, `answer-from-record` | gate.clj:256, 280 | as planned |

**Phase 2's lock road and door.**

| # | Point | Where | Status |
|---|---|---|---|
| 10 | `open-value> [layer fid T]` → `{:value v :stamp s}`, `{:erased-at d}` or `{:unreadable r}` | locks.clj:1352 | differs: it answers `{:value ..}` for a control fact and a retract too, and does not hand back the row, whose e and k the landing needs. The read-out reads the source's answer and row itself (2 seeks) and calls `open-row> [layer fid row stamp T]` (1345, unchanged by the split), telling `:source-has-no-value` by `locks/sealed?` on the row. Same 4 seeks the plan counts |
| 11 | `deliver-lock> [layer session lock-id persons]` reads `[layer :leases session lock-id]`; `unlease` hands back `:sealed` bare for `:under nil` when it is 32 bytes | locks.clj:1054, 227 | to add: the landing body. `unlease` must answer nil for a landing row (a row with `:public`), so an `:offer` citing a landing lease is `:no-such-lock` (PR10); a landing's delivery unboxes the fact's `:box` with the row's private key after checking `:for`. `deliver-all>` (1091) holds the offer, so the landing branch goes there, with `own-locks>`'s road untouched |
| 12 | `delivery` → `{:persons :delivered :missing?}`; a missing lock is a face refusal | locks.clj:617, module.clj:144 | to add: for a landing, a lock that does not deliver is the recorded `:landing-lock-gone` (PR5). On the stream gate a landing only ever comes from the store's own hop (the depot refuses `:landing` names), so no forger reaches it: a row missing under the landing's session is recorded there, where the micro gate keeps a row present under another session a face refusal (F3). Named as a rig choice at the build |
| 13 | `consume-locks> [layer session lock-ids]`, the one call site | locks.clj:1078 | as planned |
| 14 | lease row schema `{:under Keyword :sealed byte/1}` under `[layer :leases session lock-id]` | locks.clj:1009 | to add: `:public byte/1`, `:for PersistentVector` |
| 15 | `fresh-for`, `lease-writes`, `lease-under` (a person writer's rows are sealed under their lock) | locks.clj:672, 767, 591 | to add: a landing lease draws one X25519 key pair before the decision and writes one bare row `{:under nil :sealed private :public public :for L*}` whoever leases |
| 16 | `lease-locks [layer session]` → `leased-locks` `{:grain :locks {id K}}` | locks.clj:920, 1416 | differs: a landing row is left out today (its 48-byte private key is not a lock). Its `{:public :for}` goes under a new `:landings` key, not inside `:locks` as the plan's letter says, because the door pools every `:locks` entry as a sealing lock (client.clj:102-121) |
| 17 | a lease act's writer checked alive (`persons-refusal`'s `lease-who`); a value act's writer is not (`up-front-persons` reads it only to unlease rows sealed under them) | locks.clj:706, 576 | as planned (PR13 on the stream gate) |
| 18 | the person forget's fan-out writes `$$persons` and purges dying values; touches no lease row, closes no session, revokes no permission | locks.clj:1286, 1253 | as planned (F9: a bare landing row survives its leaser's forget) |
| 19 | a session close deletes every lease row of the session | locks.clj:1231 | as planned (PR9) |
| 20 | a value's yes in the base wraps its lock into the record (kind `:base` is not a row kind); the record path of a resend opens with R = O when it cites the recorded id | locks.clj:744, 1175 | as planned (the stream landing and its replays) |

**Phase 3's micro gate and door.**

| # | Point | Where | Status |
|---|---|---|---|
| 21 | `route-key`: the lease name of the first cited lock, so a landing arrives on hash(lease-name), where its row is | micro.clj:288 | as planned (§A; the forward's `(|hash lease-name)` must pick the partition `hash-by route-key` picks: phase 3's §G probe, and T7 a checks it) |
| 22 | `parse-micro` sets `:sealed` and `:lock-id` aside before `env/parse :micro` | micro.clj:255 | to add: `:box` set aside and put back on a `:landing` name only (elsewhere `:unknown-part`); a landing's face rules (F3): exactly one sealed value fact with a well-formed box, citing exactly the bound id `[[T C :offer u] 0]` (else `:malformed` or `:no-such-lock`) |
| 23 | `parts-digest`, `skeleton-offer` replace `:sealed` by `true` | micro.clj:280, 335 | to add: `:box` too (digest determinism, as 5; the leader never sees box bytes) |
| 24 | `owned-ids`, `delivered-locks`, `arrival-open`: a missing or other-session row is `:status :missing`, then the face `:no-such-lock` at the name step | micro.clj:388, 397, 413, 485 | to add: the landing body in `delivered-locks` (block 1 and block 2b both take their locks there); in `arrival-open`, for a landing, an absent row, a `:for` for another name or a box that does not open is `:status :ok` with `:value-reason :landing-lock-gone` (recorded; an owned row consumed), while a row present under another session stays the face (F3) |
| 25 | `reason-order` | micro.clj:58 | to add: `:landing-lock-gone`, at the head of the lock reasons (before `:does-not-open`), where the stream gate places it; unknown reasons rank last today |
| 26 | `lease-ok?` takes `{:count n}` only | micro.clj:320 | to add: `{:count 1 :landing [T C :landing u]}` bound to the lease act's own name (F1), else `:malformed-control` |
| 27 | the fold's mint `{:under (:who o) ...}`, `mint-rows` draws n symmetric locks | micro.clj:958, 1044 | to add: a landing lease mints one key pair into a bare row with `:public` and `:for` |
| 28 | `$$micro-names` lease row `{:under :sealed :layer :session :kind :owner :batch}` | micro.clj:170-177 | to add: `:public`, `:for` |
| 29 | `micro-lease` → `lease-result` `{:frontier :locks {i K}}` | micro.clj:1195, 1604 | differs as 16: `:landings {i {:public :for}}` beside `:locks` |
| 30 | `persons-to-check`: a value act's subject union, never its `:who` | micro.clj:440 | as planned (PR13 on the micro gate, B case 4) |
| 31 | the fold: a block-1 face never takes a name; a second passing envelope under a decided name is the face `:name-taken`; byte-identical duplicates collapse (`envelopes` keeps distinct `[name fp]`) | micro.clj:599, 983 | as planned (F3's "among passing envelopes only" holds by construction; T7 a, T12 d check it) |
| 32 | `foreign-control-keys` refuses `:promote-request`, `:crossed` | micro.clj:54 | as planned (open question 6: promotion out of a re-classed layer is not built) |
| 33 | the frontier `$$micro-task :frontier`, `visible-at?`, the name row `[name :answer]` with `:batch` | micro.clj:180, 202 | as planned (the status read's done) |
| 34 | a session close deletes its lease rows by lease name (block 2c) | micro.clj:970, 1555 | as planned (T9 c, T11) |
| 35 | the micro hold: `rtest/pause-microbatch-topology!` and `resume-` on "micro" | micro_test.clj:80 | as planned (T3, T4) |

**The doors, the read exit, the hooks.**

| # | Point | Where | Status |
|---|---|---|---|
| 36 | `c/build`, `offer-until-answered!`, `lookup`, `record`, `lease-locks`, `forget-value!`, `forget-person!`, `close-session!`, `depot-records`, `seed!` | client.clj | as planned |
| 37 | `mc/offer!`, `await-answer`, `lookup`, `take-locks`, `lease-offer` (count only), `open-act`, `record-of`, `lease-rows`, `make-base!`, `make-group!`, `seed-shared!`, `open-session!`, `person-permission`, `revoke-offer`, `forget-value!` | micro_client.clj | as planned; the landing lease's offer and its public-key take are the new door's own |
| 38 | `read-exit/read!` for one-owner layers (the source, the base's copy); `mc/open-act` for the group's copy | read_exit.clj:88 | as planned |
| 39 | `inject/point!` (crash hooks) | inject.clj:62 | as planned; no hold exists: `hold!`, `release!`, `held?` to add (a predicate the continuation checks, ending the record's processing there; it never blocks a task thread, unlike `rig.claims/hold!`) |

**The module.**

| # | Point | Where | Status |
|---|---|---|---|
| 40 | the event: `ack-return>` on the record path and on the decide path, each followed by `locks/fan-out>`, which emits nothing for an act that is no person act | module.clj:135-139, 214-220 | differs: the continuation goes before `fan-out>` on both paths (a one-line call each), as an op that passes every other act straight through; a final `ack-return>` merges `:crossing` into the request's ack (stream.md: any task, last write wins, sent when the tree completes). The plan's `{:request :crossing}` becomes the request's ack plus `:crossing`, so the door's `offer!` and `answered!` read it unchanged |
| 41 | `layers-schema` merges each stage's fields | module.clj:85-87 | to add: `:forwards` (one merge argument) |
| 42 | `*micro-offers` is declared in `micro/declare!`, after the gate topology | micro.clj:1247 | to check at the first compile: the forward's `depot-partition-append!` reaches it from an op through `this-module-pobject-task-global` under `<<with-substitutions` (the skill documents the pattern for PStates; for a depot it is assumed). Fallback: the append inline in module.clj's flow |
| 43 | the stream-gate landing (T the base while one-owner) needs the gate's whole record-or-decide path on T's home, and the crossing needs its writes block; both are inline in module.clj's `<<sources` body today (lines 122-213) | module.clj | **decision, below** |

### The one decision before building: where the gate's decide path lives

The plan decides a stream-gate landing "through the gate's ordinary
decision path" on T's home, by a hop, and the crossing is written as any
act's yes is. Both need code that today exists once, inline in the
`<<sources` body of module.clj, reachable only from a depot record.

- **(a) Move it into ops** in a new namespace (`rig.store.gate-event`):
  the record-or-decide path and the writes, verbatim, called by module.clj's
  flow (which shrinks to intake, the op, the ack, the continuation, the
  fan-out), by the crossing (the writes) and by the stream landing (the
  whole path on T's home). One decision path for every act. Cost: about 90
  lines leave module.clj; phase 6's planned edits to that body (its a2, a3:
  key rows read and written in the event) then conflict, and are ported
  into the op at the merge: the same code in one place, which the build
  notes will map line for line.
- **(b) Copy it** into promotion's namespace for the crossing and the
  stream landing, module.clj untouched. The merge stays local, but the
  gate's decision path then exists twice, and once phase 6 lands, a copy
  that does not read the key rows would wrap a promoted `:mention` in the
  base under no grammar subjects: a silent divergence of exactly the kind
  "never patch around a structural problem" forbids.

**Default: (a)**, unless the orchestrator's merge plan prefers (b); the
receipt asks. Either can change later without touching a record.

### Shared files I expect to change, and why

- `envelope.clj`: `landing-name` gains the class (3-arity; the 2-arity
  kept); `parts-fact` replaces `:box` (point 5).
- `gate.clj`: `:store` joins `exempt-actors` (7); `control-fact?`,
  `control-value-ok?`, `control-allowed?` gain `:promote-request`,
  `:crossed` and the landing lease's `:landing` (8); a promotion request
  is its act's one fact, as a lock act is.
- `locks.clj`: the lease row's two fields (14); `unlease` answers nil for
  a landing row, and the landing body in `deliver-all>` with
  `:landing-lock-gone` (11, 12); the landing lease's key pair and bare row
  in `fresh-for` and `lease-writes` (15); `:landings` in `leased-locks`
  (16).
- `micro.clj`: the lease row's two fields (28); `parse-micro`,
  `parts-digest`, `skeleton-offer` for `:box` (22, 23); the landing body
  and `:landing-lock-gone` in `delivered-locks` and `arrival-open` (24);
  `reason-order` (25); `lease-ok?` (26); the mint and `mint-rows` (27);
  `lease-result` (29).
- `inject.clj`: `hold!`, `release!`, `held?`, and `reset-all!` clearing
  the holds (39).
- `module.clj`: the requires; the `:forwards` merge argument (41); the
  continuation's call on the record path and the decide path (40); the
  status query's declaration; and, under (a), the body moved to
  `rig.store.gate-event` (43).
- Not expected: `client.clj`, `micro_client.clj`, `reads.clj`,
  `read_exit.clj` (the door's promotion functions go in a new namespace).

**New namespaces.** `rig.store.box` (the sealed box: key pair, `box`,
`unbox`, `public-key`, pure JDK, total, requires nothing of the store);
`rig.store.promote` (pure: the request's value check, the store-made
crossing and landing offers, the read-out's decision, the status over
three answers, the statements, the `:forwards` schema);
`rig.store.promote-flow` (dataflow: the continuation, the forward, the
stream landing, the `promotion-status` query); `rig.store.promote-client`
(the door: `lease-landing!`, `promote!` with a caller-given or pre-made
request name, `promotion-status`, the resend); under (a),
`rig.store.gate-event`. Tests: `rig.store.promote-unit-test` (no
cluster), `rig.store.promote-test` (cluster). Phase 8's names: all as its
table asks, the door's three in `rig.store.promote-client`.

### Order of work (part 2)

1. `git merge rig-2026-09-25` at the commit the orchestrator names; re-read
   every point above against the merged code; note what moved.
2. `rig.store.box` and its unit test U1, run without a cluster.
3. `rig.store.promote` (pure) and U2 to U4.
4. The shared-file edits: envelope, gate, locks, micro, inject; then
   module.clj (with (a) or (b)); `rig.store.promote-flow`;
   `rig.store.promote-client`. First compile checks point 42.
5. The existing suite once, under the cluster lock, to see nothing broke.
6. `IMPLEMENTATION_VALIDATION-promotion.md`, scenario-traced, default FAIL.
7. `rig.store.promote-test`: T1 to T14 and the hold hook's own test.
8. `TEST_VALIDATION-promotion.md`.
9. The whole suite to green, every namespace named, in the background
   with its log under `runs/`.
10. These notes finished: what was built, divergences, the suite run,
    "For RIG.md" (P4-1 onward, questions for Sid, first-record
    placeholders).

Commits as I go, one change each.

### What blocks the build now

- Wave 1 has not landed (expected; part 2 waits for the message).
- The decision above, (a) or (b): the orchestrator's, since it owns the
  merge. Default (a).
- Point 42 is unverified until the first compile; its fallback costs a
  few lines in module.clj, no design change.

## Build (26 September, from 05:33 IST)

Wave 1 merged at `2034cce5` (merge `4d53e643`); `module.clj` was
unchanged since the prep read it. The orchestrator chose (a).

### What was built, commit by commit

1. `1b1d416c`: the stream gate's record-or-decide path and its writes
   moved verbatim into `rig.store.gate-event` (`record-or-decide>`,
   `write-decided>`); `module.clj`'s flow calls the op and keeps the ack
   and the fan-out. stream-gate, lock and forget tests: 13 tests, 935
   assertions, 0 failures (`runs/promotion-move-tests.log`). Sent to the
   orchestrator.
2. `49706eb2`: `rig.store.box` (the sealed box) and
   `rig.store.promote-shape` (names, the request's and the landing lease's
   checks, the landing row, the landing body, a landing's faces).
3. `28a67c24`: the gates take promotion's acts (envelope, gate, locks,
   micro, inject; see "Shared-file changes").
4. `72561f2e`: `rig.store.promote` (pure), `rig.store.promote-flow` (the
   continuation, the read-out, the forward, the stream landing, the
   `promotion-status` query), `rig.store.promote-client` (the door), and
   the module's wiring. A smoke run landed a note in the group
   (`runs/promotion-smoke.log`).
5. `1a3a11e8`, `3f9a928e`, `f5e38f6b`: the implementation validation's
   three fixes (V-1 the crossing on the source's entity, answered by
   name; V-2 the read-out claims no class, as the model; V-3 the status
   read's forward summary). `7126a224`: the validation, minor-fail, fixed
   in place.
6. `444b9068`, `25811c57`, `7bf0515e`, `b0721065`: the tests; `d2e3be54`
   the test validation, minor-fail, fixed in place; `26415703` the review's
   R-1 refusals through `gate/decide`.

### How a promotion runs, as built

The door (`promote-client/promote!`) makes the request's uuid u first, so
every name is known before anything is sent: it takes a landing lease in
the target T (`[T C :offer u]`, `{:count 1 :landing [T C :landing u]}`),
which T's gate mints as one bare X25519 key pair row, and reads its
public key back (`:landings`); then it sends the request `[L :by-layer
:offer u]`, one `:promote-request` fact carrying the source, the target,
the class, the lease id, the public key (base64), the landing permission,
the head to replace and the copy's subjects. The stream gate admits it
like any act (the value checked, F1, F2). After its answer,
`promote-flow/continue>` goes on past a commit boundary: the read-out on
L's home opens the source (`locks/open-row>`), seals the copy under a
fresh lock K, boxes K to the lease's public key bound to the lease id and
the landing name, and writes the crossing (`[L nil :crossing u]`, `:who
:store`) and the stored forward in one group; past another commit
boundary the forward goes to `*micro-offers` on the lease's task, or by a
hop to T's home on the stream gate. T's gate decides the landing like any
sealed act, K coming out of the box with the lease row's private key, and
consumes the row whatever it decides. `promotion-status` reads the
request's, the crossing's and the landing's answers (the landing's only
once settled under the frontier) and says the state with its statement.

### Divergences from the plan (each in IMPLEMENTATION_VALIDATION-promotion.md)

- **V-2 / D5, for Sid and the orchestrator:** the read-out claims no
  class, as the model's `refusal` has it (model.clj 411-419: "a read-out
  is the store's own step, placed where the source's lock is; it claims
  no class"), and as IMPLICIT_SPEC OP7 draws it. The plan's validation
  fix F8 refused such a read-out `:class-mismatch`; SPEC.md makes the
  model the gates' executable spec, so the build follows the model. A
  promotion pending when its source layer is re-classed is read out on
  the stream side, where the lock is, and lands (T15).
- D2: the request's ack is its answer plus `:crossing` (and `:landing`
  for a stream target), so the door's ack path and lookup path give one
  shape.
- D3: the lease queries give a landing row's public key under
  `:landings`, beside `:locks`, which the door pools as sealing locks.
- D4: the read-out reads the source's answer and row and calls
  `open-row>`, since `open-value>` answers `{:value}` for a control fact
  or a retract and hands back no row; 8 seeks, the eighth phase 2's
  ledger read, which the plan's count of 7 left out.
- D6: on the stream gate a landing whose bound row is missing under its
  session is recorded `:landing-lock-gone`, also when a row exists under
  another session (rows are keyed by session there; the plan's face
  refusal for that case guards against forgers, who cannot reach this
  gate).
- D7: the request's check also requires a session, the source among the
  act's stood-on, and a source index a row vector holds.
- V-1: the crossing's record answers by name with no digest (its name is
  the store's alone).

### Shared-file changes (for the merge)

- `module.clj` (`1b1d416c`, `72561f2e`): the `<<sources` body after
  `(inject/point! :seen *name)`, old lines 127-220, moved verbatim to
  `rig.store.gate-event` (`record-or-decide>` holds the record lookup, the
  record path and the decide path; `write-decided>` the writes of a
  decided act): **another branch's edit to those lines is ported into
  `gate_event.clj`**, where the same lines sit, re-indented. The comment
  block above `defmodule` rewritten. New requires `gate-event`, `promote`,
  `promote-flow`; `layers-schema`'s merge gains `(promote/layer-fields)`;
  one line after `(ack-return> *ack)`: `(promote-flow/continue> *layer
  *offer *ack)`; one line after the reads' queries:
  `(promote-flow/declare-queries! topologies)`.
- `gate.clj` (`28a67c24`): require `promote-shape`; `exempt-actors` gains
  `:store`; `control-fact?` gains `:promote-request`, `:crossed`;
  `control-value-ok?`'s `:lease` clause also takes a landing lease, and
  two clauses, `:promote-request`, `:crossed`; `control-allowed?` starts
  with `(= :crossed k) (= :store who)` and admits `:promote-request`; the
  `:malformed-control` clause of `refusal` adds the request's one-fact
  rule; `intake` split into `intake-offer` and `intake` (no change in
  behavior).
- `envelope.clj`: `landing-name` gains a 3-arity with the class (the
  2-arity kept); `parts-fact` replaces `:box` by `true`.
- `locks.clj`: requires `box`, `promote-shape`; the lease row schema gains
  `:public`, `:for`; `unlease` answers nil for a landing row; `fresh-for`
  draws `:landing-pair`; `lease-writes` split into `landing-lease-writes`
  and `symmetric-lease-writes`; `leased-locks` gains `:landings`; new
  `landing-delivery` and `delivered-context` after `value-context`; new
  `deliver-landing>` before `deliver-all>`, whose body gains a landing
  branch; `decision-reads>` calls `delivered-context`.
- `micro.clj`: requires `box`, `promote-shape`; `reason-order` gains
  `:landing-lock-gone` before `:does-not-open`; the `$$micro-names` lease
  row gains `:public`, `:for`; `parse-micro` sets `:box` aside and applies
  a landing's faces; `skeleton-offer` replaces `:box`; `lease-ok?` takes a
  landing lease; `delivered-locks` gains the landing body; new
  `landing-gone?`; `arrival-open`'s `if` becomes a `cond` with the landing
  case first; the fold's mint meta gives a landing lease `:under nil` and
  `:for`; `mint-rows` mints a key pair for it; `lease-result` gains
  `:landings`.
- `inject.clj`: a `holds` atom, cleared by `reset-all!`; `hold!`,
  `release!`, `held?` at the end.
- Not changed: `client.clj`, `micro_client.clj`, `reads.clj`,
  `read_exit.clj`.

### Phase 8's names

- `rig.store.promote-client/lease-landing! [store {:who :target :class
  :session :permission :uuid}]` → `{:name :offer :lock-id :answer :public
  :for}`.
- `rig.store.promote-client/promote! [store spec]`, spec `:who :layer
  :permission :session :source :target :class :landing-permission
  :replaces :subjects :uuid`; the request's name is
  `(promote-shape/request-name L u)` for the `:uuid` given, known before
  sending. Also `request-offer`, `resend!`, `connect` (micro-client's
  handles plus the status query).
- `rig.store.promote-client/promotion-status [store layer req as-of]` →
  `{:status .. :reason .. :at .. :statement ..}`; `status-of` → the
  keyword alone (`:none` `:pending` `:crossed` `:done` `:refused`).
- `rig.store.envelope/crossing-name [req]`, `landing-name [req target]`
  and `[req target class]`.
- `rig.store.inject/hold! [point nm]`, `release!`, `held?`, points
  `:before-read-out` and `:before-forward`, keyed by the request's name;
  crash points `:before-read-out`, `:before-forward`, `:after-forward`.

### Review R-1 (the wave 1 review, 9f6ccc91)

Built and tested here: a client's `:crossed` fact is
`:control-not-allowed` at the stream gate, the operator's too (only the
store's read-out writes one, and no depot record may claim `:store`); a
`:promote-request` is a control fact the gate checks, admitted only as its
act's one fact with a well-formed value, so the only admitted request is
one whose yes goes on to its read-out
(`promote_unit_test/the-gate-admits-promotion-facts-only-on-their-own-path`,
through `gate/decide`; the micro gate already refused both,
`foreign-control-keys`). The rest of R-1 is left to its own step.

## For RIG.md

### Rig choices (not rulings), P4-1 onward

Each can change later without touching a record unless marked
first-record (those are also listed below).

- **P4-1. The stream gate's decision path is one op**, `rig.store.gate-event`
  (the orchestrator's decision (a)): the depot's records, the crossing's
  writes and a landing into a layer the stream gate orders all go through
  it.
- **P4-2. The promotion continues in the request's own record** (the
  plan's PR7): `promote-flow/continue>` after the request's answer, past
  a commit boundary; the request's ack carries the crossing's answer, and
  a stream landing's (D2).
- **P4-3. The read-out claims no class** (V-2), as the model decides it:
  a source layer re-classed since the request is read out where the
  source's lock is, on the stream side. This reverses the plan's F8.
- **P4-4. A crossing's record answers by name** (V-1): its name is under
  the store's reserved scheme, so no digest is compared on its record
  path.
- **P4-5. The request is checked whole at the gate** (D7): exactly its
  keys; the source a readable fact id in the owner's layer that the act
  stands on; the target another layer; the lease id bound to the
  request's own uuid (F1); the public key a decodable X25519 key (F2); the
  landing permission for the target; the head in the target; at most 256
  subjects; a session; and the request its act's one fact.
- **P4-6. `:crossed` is the store's alone**: anyone else writing one,
  the operator included, is `:control-not-allowed` (review R-1).
- **P4-7. A landing lease is a lease act with `:landing`**, minting one
  bare X25519 key pair row on either gate; lease queries show its public
  key under `:landings`, never its private key (D3).
- **P4-8. A landing that cannot get its lock is recorded
  `:landing-lock-gone`**, placed at the head of the lock reasons on both
  gates. On the micro gate a row present under another session stays a
  face refusal (F3); on the stream gate, where only the store's hop brings
  landings, a row missing under the landing's session is recorded (D6).
- **P4-9. The public key travels in the request as base64 text**: a
  control value is EDN, which has no bytes.
- **P4-10. The status read carries a forward's summary** (V-3), and reads
  the landing only once it is settled under the frontier.
- **P4-11. The door's rules**: no request without a landing lease; one
  session for the lease and the request; the session kept open in the
  target until the promotion ends (PR9). The store cannot check them.
- **P4-12. The hold is a predicate** (`inject/held?`): a held
  continuation ends the record's processing at its point, never blocking
  a task; the door's resend continues it (PR15). Plus a `:before-read-out`
  crash point.
- **P4-13. The sealed box** (PR6): the JDK's X25519, the wrapping lock
  HMAC-SHA256 keyed by the shared secret over `softland/landing-box/v1`
  and both public keys, AES-256-GCM with a 12-byte nonce, the associated
  data the canonical text of `[lock-id landing-name]`. A box is read only
  at a landing's decision, so it can change without touching a record.
- **P4-14. The stored forward is the envelope as sent** (PR8, F4), in
  `$$layers [L :forwards req]`, subindexed; every send is it.

### Questions for Sid

1. **A promotion pending when its source layer is re-classed** (P4-3):
   the model reads it out where the lock is, and so does the build; the
   plan's validation refused it `:class-mismatch`. The model's reading
   follows P16 (a re-classed layer's store-placed acts stay with the
   stream gate). Which do you want?
2. **A landing lease taken in another session than its request's**
   (open question 2, D6). On the micro gate the promotion stays crossed
   until a resend after that session closes; on the stream gate it ends
   refused at once. Either way that session's bare landing row stays
   until the session closes, an opener of a copy that was refused. A
   request could carry the lease's session (a first-record field), so the
   store could check it and consume the row. Should it?
3. **Promotion's forms** (RIG.md "For Sid" 5, PR1 to PR5), as built,
   below.
4. **Promotion's windows and limits** as the plan put them and the build
   kept them: For Sid 11 (between the read-out and the landing the
   landing lease is bare and opens the copy, even if a subject is
   forgotten meanwhile, closed at the landing's decision, T13's F7); 20
   (a door can forge a landing for its own promotion; signing closes it,
   T12 shows a third writer cannot); 21 (a landing lease dies with its
   session, T9 c); 22 (a target re-classed between the request and the
   landing refuses the landing, T10 c).
5. **For phase 8 to report**: the rig's stored forward lands a crossed
   promotion whose source is erased before the first send (T7 b, held),
   where the model's `forward` sends nothing; in the model the first send
   cannot be lost, so the B cases agree. `promotion-status` shows `:none`
   for a request admitted after the moment asked (F5), where the model
   shows pending. A landing lease act precedes every request, which the
   model does not have.

### First-record placeholders

- **PR1, the request**: a control fact `:promote-request` on the source's
  entity in the owner's layer, its value exactly `{:source :target :class
  :lease :public :permission :replaces :subjects}`, the public key as
  base64 text (P4-9); the act stands on the source.
- **PR2, the crossing**: named `[L nil :crossing u]`, `:who :store`, no
  permission, the class in force on the act (P4-3), standing on the source
  and the request, one `:crossed` fact `{:request req :source src}` on the
  source's entity; its recorded refusals `:source-erased`,
  `:source-has-no-value`, and `:malformed-control` when no box can be made
  (F2).
- **PR3, the landing**: named `[T C :landing u]`, C the class the request
  carries; `:who` the requester; because of the request; standing on the
  source and the crossing; one sealed value fact on the source's entity
  and key, with the store-owned fact part `:box`, accepted on a landing
  name only; version 1 unchanged.
- **PR4, the landing lease**: `[T C :offer u]` with `{:count 1 :landing
  [T C :landing u]}`; its row bare, the private key in `:sealed`, with
  `:public` and `:for`.
- **PR5, `:landing-lock-gone`**, recorded (P4-8).
