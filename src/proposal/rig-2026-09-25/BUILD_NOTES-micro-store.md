# Build notes — micro store (phase 3 build, 26 September)

Running log of the rama skill's build phase for stage 3 (phase-build.md:
implement, validate the implementation, write tests, validate the tests,
run to green), from PLAN-micro-store.md (revised 26 September, validated
minor-fail, [PV-F1] to [PV-F13] in place), with builder A's decisions: R19
(the permission walk, not the cascade) and the root actor `:operator`.
Branch `rig-build-micro`, worktree `/mnt/data/projects/Softland-rig-build-micro`.
Newest entries at the bottom.

## Log

- 02:53 Read the rama skill, phase-build and phases 3 to 7, the plan in full
  with its validation, SPEC.md, RIG.md's top sections and R19, phase 2's
  plan (revision, shapes, the door and the lease road, the gate event, L20
  to L30), the model's micro gate, permissions and scenarios, phase 1's
  code and its stream gate test's patterns, the micro probe.
- 02:59 Phase 2's line had already appeared ("Lock primitives committed at
  1febfa3da1b210a40f9fc53b4d654de4e5528a22"); merged exactly that commit
  (5afe3c7d): `rig.store.locks` (pure) and `rig.store.grammar`, new files.
- 03:30 Lock-independent parts and the topology written: `rig.store.permit`
  (chain, refusal); `env/pid?` four deep and the offer's pid rebuilt
  deeply; `gate/refusal`'s four permission clauses one `permit/refusal`
  call, `gate/pids-to-read` the chain; `rig.store.micro` (schemas, intake,
  the arrival's lock work, the name, layer and entity steps, the record
  path's check, the fold, blocks 0, 1, 2a, 2b, 2c, four queries);
  `module.clj` one require and one line. The module launched on an
  in-process cluster at the first try; the frontier advanced (6 after 2 s).

## Build-level choices (not in the plan, or adapted), with why

- **The offerer's side is `rig.store.micro-client`, not `rig.store.micro`.**
  `module.clj` requires `rig.store.micro` for `declare!`, and the door needs
  the module's name and the stream side's client, so a door inside
  `rig.store.micro` would be a require cycle. The dispatch by tag (M13) and
  M25's routing live there too, so `client.clj` is not changed.
- **`$$persons` placeholder.** Only phase 2's pure namespace was merged, so
  its stream gate (which declares and writes `$$persons`) is not in this
  branch. `rig.store.micro/persons-placeholder?` declares the same PState
  with phase 2's schema on a placeholder stream topology fed by a test
  depot, the one seam; the merge turns it off and phase 2's `gate` owns it.
- **Lease rows carry the layer's kind and person owner** (beside M16's
  `:layer` and `:session` marks). Block 1 and 2b need them on the arrival
  task, before the layer task, to wrap under the owner and to place a lock
  in a row or the record. Lease rows are consumed at decision; first-record
  as M16 already is.
- **Minting happens in block 2a from the fold's output**, not in 2b from
  the offers: the fold holds who, session, layer, count and the settings in
  force; the rows are written on the lease name's task, where `$$persons`
  is local too. Same rows, same batch, one hop fewer.
- **The person checks are computed on the arrival task and carried in the
  skeleton** (`:person-reason`), not as `[[:person p] entry]` rows: an
  entry read on two tasks during a forget's fan-out can differ, and a
  merged row would decide one offer on another's read.
- **The record path's check rows are one per value**, `[[:resend name fp
  i] check]`: `+map-agg`'s combine keeps one value per key, so one row per
  envelope could drop a `:name-taken`.
- **`:members` is a map person → batch**, not a set: one write shape for
  every `$$micro` projection, and M7's batch stamp on every row.
- **A setting fact for a layer whose settings the stream gate keeps is
  refused on its face `:wrong-gate`** at this gate (P16, M25: settings stay
  with the stream gate), rather than written as a micro version that would
  shadow `$$layers`.
- **The fold coalesces its writes by location** (last write in fold order
  wins): two acts in one batch touching one location (a head made then
  replaced, a permission granted then revoked, two settings versions)
  would otherwise be two termvals of different values in one batch, in no
  fixed order.

## Log, continued

- 03:25 to 03:40 Self-validation (phase 3 step 6) and phase 4's adversarial
  pass: IMPLEMENTATION_VALIDATION-micro-store.md, minor-fail, fixed in
  place (a bare lock shipped at the name hop on the fresh path; `:landing`
  at this gate; a yield on micro-act's rows; the walk computed twice; an
  unguarded row computation; a stream-era forget target). Phase 1's suite
  on the shared-file changes: 19 tests, 785 assertions, 0 failures.
- 03:40 Phase 2's notes name later commits (b0f025d5 the sealed parse and
  the parts digest in `env/digest`; 3ddc6eab `:scheme :aes-gcm-1` on lock
  records; 33b357a4 more requires and helpers). Not merged (the brief: that
  one commit); the micro lock-record schema takes an optional `:scheme` so
  a merged `locks/wrap` fits it. For the merge: phase 2's `env/control-keys`
  lacks `:members`, so its parse would refuse the group's making act
  `:not-sealed`.
- 03:43 to 03:55 Tests written (phase 5), validated (phase 6, minor-fail:
  one synchronization fault, reading an act through a lagging entity
  task's own frontier, and fourteen matrix rows), fixes applied.
- 03:57 First run of the pure namespace: 226 of 227; the error was real,
  `prepare`'s envelope order indexing a malformed row outside its guard;
  fixed; 14 tests, 227 assertions, 0 failures.
