# Build notes — stream-store (phase 1 build session, second run)

Running log for the `build` phase of stage "stream-store". A fresh session
picks up from "Next". Newest entries at the bottom of "Log".

## State

- Step: done. Phases 3 to 7 of the build are complete; the suite passes.
- Last suite result (09:25 IST, 4 tasks): `clojure -M:test rig.smoke-test
  rig.store.envelope-test rig.store.stream-gate-test`: 13 tests, 728
  assertions, 0 failures, 0 errors, exit 0. Output in runs/phase1-suite.log,
  summary in runs/phase1-suite.txt. Green on 2, 4 and 8 tasks across this
  session's runs 2 to 8.

## Next

Nothing in this build. For the main session: the proposed rig choices
below (not in RIG.md); the untouched partial `test/rig/store/gate_test.clj`
(Sid's to decide); clojure-lsp left caches in the rig folder (`.clj-kondo/`,
`.lsp/`, about 7 MB; git does not list them); RIG.md's rewrite and the
commit are the main session's.

## Fixes applied (phase 3)

- F1: `:log` row vector subindexed. Rama 1.6.0 takes only `{:subindex?
  true}` on a vector-schema, so size tracking stays on (probe). Written by
  one whole-vector termval (the probe accepted it). Client `facts` reads
  `[(keypath layer :log name) ALL]` by foreign-select.
- F2: `:stood-on` removed from the answer record; its own subindexed map by
  name, inner `{fid stamp}` map subindexed (track-size off); one termval per
  carried entry, for a yes (plan step 5, "Writes"); client `stood-on`.
- F3: parser refuses more than 256 carried subjects as `:malformed`.
- F4: already held (record read first; settings by `[(keypath layer
  :settings)]`); kept.
- F5: nothing to code (the wall is `TopologyUtils/currentTimeMillis`).
- F6: values nest in at most 32 collections (deeper is `:malformed`);
  `parse`, `gate/intake` and `gate/decide` catch Throwable; every vector
  rebuilt with `(into [] ...)` (name, permission, fids, because-of, and
  vectors inside values via `normalize-value`); `long` in Long positions;
  `:who :store` refused on its face as `:reserved-who`; a failure in the gate
  is the unrecorded face refusal `:gate-error` (intake and decide); the
  exempt set is the operator only. Beyond the letter of F6, so that the
  digest respects `=` and never confuses two contents: keywords must read
  back as themselves; numbers normalised to one form per `=` class (ints to
  long, BigInt/BigInteger to long in range, -0.0 to 0.0, BigDecimal without
  trailing zeros); lists, symbols, characters, records, Java floats (since
  run 1) and non-EDN numbers (e.g. AtomicLong) refused as malformed (P5's
  list).
- F7: already held (heads and permission rows read by loop<-, which emits
  its accumulator, possibly empty, on every path; no explode on the read
  path). The explodes left are inside `<<atomic` on the write path, where
  zero emits skip nothing after the block (the smoke run's acts with no
  replaces, grants or stood-on reached the ack).
- F8: `:layer-already-made` (a `:kind` or `:owner` setting fact when the
  settings hold that field) and `:unsupported-reclass` (a `:class
  :by-layer` fact while the class in force is `:by-entity`), after
  `:stale-revoke`.
- F9: nothing to change: the stream gate decides tag-class-nil acts of a
  one-owner layer before and after a re-class (the class check is against
  the class in force; a re-classed layer's grain switch carries `:class
  :by-entity`). One depot in this stage, so the client's routing by tag is
  stage 3's.
- F10, F11, F12: plan text; the code is consistent (carried stood-on stamps
  and the heads' recorded stamps feed the stamp; the client's secret only
  serves `lookup`; nothing but this gate's event writes `$$layers`).
- F13 (code part): `lookup` takes a name and a digest; a nil digest returns
  the record as data. A 2-arity `(lookup store offer)` stays for the smoke
  test. The tests F13 names are phase 5's.
- F14: the probe ran first (results below).
- R13 kept, both reasons, placed after the permission checks and before
  `:stale-replaces`. No plan fix covers the same ground: F6 checks the
  envelope's classes, not a setting fact's value (a string `:kind` would
  still reach a Keyword slot; the probe showed that write throws); F8
  refuses a second `:kind`/`:owner` and a re-class back, not a person
  writing a grant, a revocation, a re-class or an owner; P8's "only the
  operator grants and revokes" is enforced by none of the plan's reasons.
  F8 and R13 overlap only where the operator re-makes a layer (R13 lets the
  operator through, F8 refuses).
- Also, within P8's reading: two revoke facts for one permission in one
  act are `:stale-revoke` (as a doubled replace is stale); two grants of
  one permission in one act keep the first in the index.
- Also, per the skill's no-read rule: settings and permission rows are
  written whole from the values read in this event (the plan's per-field
  termvals would each read the row first); the resulting rows are the ones
  the plan names.
- inject: besides the crash points, `:seen` and `:recorded` points and a
  watch trace for named offers, so a test can observe a replay (P14).

## F14 probe (create-test-pstate on the exact $$layers schema), run 1

Ran `f14_probe.clj` (scratch; its checks move into stream_gate_test.clj as
the `layers-schema-probe` deftest). Rama 1.6.0 observations:

- `vector-schema` takes only `{:subindex? true}`; `{:subindex-options
  {:track-size? false}}` is refused at declaration ("Invalid option", valid
  #{:subindex?}). So F1's row vector is subindexed with size tracking on:
  a Rama constraint, not a choice.
- Answer record under a vector name key on an empty PState: created, read
  back equal. Heads row under `[e k [name idx]]`: written, read, deleted by
  NONE>.
- Whole-vector `termval` into the subindexed row vector: ACCEPTED; `ALL`
  reads the rows in order; one row by `(keypath layer :log name idx)`.
  Per-index `termval` on an absent vector: REFUSED (ValueSchemaMismatch:
  keypath on nil makes a map). END and AFTER-ELEM also work. The module
  uses the one whole-vector termval (F1's first option).
- Stood-on: per-entry termval works; whole-map termval also works.
- `:answers` ALL order: bytewise by the serialized key; for names of one
  layer/class/scheme that is UUID byte order, i.e. time order for UUID7.
- `:heads` keys iterate with prefix contiguity: all `[:e0 :note _]` then
  `[:e0 :notes _]` then `[:e1 :note _]` (stage 5's range over `[e k]`).
- A `subvec` as a PersistentVector key, an Integer in a Long slot, a string
  in a Keyword settings slot: each REFUSED with ValueSchemaMismatch on
  write. So F6's `(into [] ...)` and R13's `:malformed-control` are
  load-bearing (a schema violation in topology code kills the worker).
  Reads by a subvec key succeed.
- Settings field writes on a fresh layer create the fixed-keys map; the
  settings or an answer of an absent layer read nil.

## Proposed rig choices (for RIG.md; not edited there by this session)

- **A carried stood-on stamp must be below 2^62; otherwise the record is
  refused on its face as malformed.** Why: the stamp is at least a carried
  stamp + 1 and the task's clock follows it; a carried Long/MAX_VALUE would
  overflow, and every later offer on that task would be a gate error. P9
  trusts carried stamps; this bounds only the arithmetic.
- **The value domain is P5's list, normalised to one form per `=` class.**
  Maps, vectors, sets, keywords that read back as themselves, strings,
  numbers (integers of any width, doubles, big integers and decimals; not
  ratios, not Java floats), booleans, nil, UUIDs; lists, symbols,
  characters, floats and records are malformed. Why: the digest is HMAC
  over canonical text, and it must be equal for `=` offers and different
  otherwise (I-G7); `(= [1] '(1))`, `(= 1 1N)`, `(= 1.0M 1.00M)` and
  `(= 0.0 -0.0)` are true while their printed forms differ, and a set
  holding a Java float is not `=` to the same set holding the double.
- **Two revoke facts for one permission in one act are refused
  `:stale-revoke`.** Why: P8 reads a revoke as standing on its grant the way
  a replace stands on its head, and a doubled replace is stale.

## Log

- Session start: read the rama skill (SKILL.md, phases.md, phase-build.md,
  phase-3..7 docs, both validation templates), RIG.md, SPEC.md.
- Read PLAN, PLAN_VALIDATION, IMPLICIT_SPEC 1-343 + OP1/2/4-8, RD1, E1-E4,
  PROGRESS "Now", model.clj, the five source files and the smoke test.
- module.clj: `layers-schema` updated for F1 (subindexed row vector) and
  F2 (`:stood-on` map by name; removed from the answer record). Topology
  body not yet updated.
- F14 probe run (results above).
- envelope.clj, gate.clj, module.clj, inject.clj, client.clj rewritten for
  the fixes above; all five load; clojure-lsp diagnostics (clj-kondo
  2025.01.17) shows no error or warning, only unused-public-var infos for
  functions tests and later stages use; smoke test passes.
- Phase 3 self-check found: `owner-in-force` took an act's own `:owner`
  fact value unchecked, and the answer record (written for a refused act
  too) puts it in the Keyword `:subjects` set; an operator making act with
  `:owner "alice"` (refused :malformed-control) would then throw on the
  record write, a worker restart loop. Fixed: only a readable keyword is
  taken (gate.clj `owner-in-force`).
- 08:52:49 IST: gate.clj was rewritten on disk by something other than
  this session, restoring my own earlier text without the owner-in-force
  fix (other agent processes are running in this worktree). Re-applied the
  fix; from here each step checks the sources against a scratch backup
  (`check.sh`) before building on them.
- Phase 4: IMPLEMENTATION_VALIDATION-stream-store.md written. One finding
  (the owner-in-force one above, minor, fixed); 13 plan divergences listed,
  each justified (Rama fact, correctness, the plan's own no-read cost
  model) or naming. Verdict pass after the fix.
- Checked the model's answers for every history the IPC test compares
  against (scratch run of formal.model/run under baseline): yes,
  fact-outside, class-mismatch after a re-class, from-another-layer
  (:cite :session), no-permission (Bob into :alice), permission-revoked,
  stale and doubled replaces, failover, retry, reuse, reuse under another
  layer: all as expected.
- Phase 5: wrote test/rig/store/envelope_test.clj (pure: parse totality on
  the unguarded parse*, parsed classes incl. subvecs, bounds, face reasons,
  value round trip, readable keywords, digest respects = and separates,
  names/uuid7/tag-of parity with the model's offers from its fixed
  histories, reason order table, stamp-for property, decide* total with
  every write applied to a create-test-pstate of the real schema) and
  test/rig/store/stream_gate_test.clj (the F14 probe as a deftest, and one
  IPC deftest: seed, E1 sequence, face refusals, recorded refusals, OP5/F8/
  R13/OP8, re-class/P16, grant/revoke, chains, stamps incl. sim time, a
  600-fact act, malformed records through the cluster, concurrent same-name
  offers, crash before writes, crash after writes, completed-record replay).
  Both load; clojure-lsp shows no errors or warnings.
- Phase 6: TEST_VALIDATION-stream-store.md. The walk found seven implicit
  spec rows untested (concurrent replace race; refused first use holds its
  name + N3 × other content/tag; N1 × tag mismatch in flight; N3 × crash;
  session root revoke, no cascade; offer racing its layer's making;
  operator under a reserved scheme); all added to the existing deftests;
  both namespaces reload and lint clean. Verdict pass after the additions.
- Phase 7, run 1 (`clojure -M:test rig.smoke-test rig.store.envelope-test
  rig.store.stream-gate-test`, tasks 2): 13 tests, 720 assertions, 2
  failures, 2 errors; every cluster scenario passed but one assertion, and
  all three crash tests passed (append threw; the crashed record replayed
  and was answered without a resend; the completed record replayed from its
  record on the first attempt). The four:
  (a) value round trip: `#{#{(float -3.0)}}` is not = to its normalised
  `#{#{-3.0}}`, since Clojure hashes a Float apart from the equal Double.
  Fix in the envelope: Java floats are refused as malformed (EDN has no
  single-precision float) instead of normalised; the proposed value-domain
  rig choice now says so.
  (b, c) two errors: generator mutations throwing on already mutated records
  (assoc-in into a symbol, a char); fixed in the test (a mutation that does
  not apply leaves the record).
  (d) the retract's head was asserted after its undo, bound in the same let,
  had replaced it; fixed in the test (assert before sending the undo).
- Run 2: 13 tests, 728 assertions, 0 failures, 0 errors (4 tasks).
- Measured the property generators: only 8% of decision inputs were
  admitted; biased them (matching settings, granted rows, present heads,
  own permission, mostly good control values, facts sometimes naming a
  layer): 29% admitted, 62 settings writes and 18 permission-row writes in
  930 inputs, every recorded reason but :unsupported-reclass drawn.
- Runs 3 to 8 after that: all 0 failures, 0 errors; tasks drawn 4, 4, 4,
  2, 4, 8. The crash tests passed every time; the completed-record replay
  was observed on the first attempt every time. In every run's log the only
  fatal throwables are the 3 injected crashes, each followed by a worker
  shutdown and restart.
- Final run (09:25 IST, 4 tasks): 13 tests, 728 assertions, 0 failures, 0
  errors; runs/phase1-suite.log and runs/phase1-suite.txt written.
  `test/rig/store/gate_test.clj` was not read, loaded or changed; only its
  listing was seen (11,883 bytes, 03:00:35, as RIG.md gives it) and its
  checksum taken to confirm that.
