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
