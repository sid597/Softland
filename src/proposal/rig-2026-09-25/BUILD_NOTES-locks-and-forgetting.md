# Build notes — locks-and-forgetting (phase 2 build, 26 September)

Lock primitives committed at 1febfa3da1b210a40f9fc53b4d654de4e5528a22

Running log of the rama skill's build phase for stage 2, from
PLAN-locks-and-forgetting.md (revised 26 September, validated minor-fail,
[V-F1] to [V-F4] applied). Branch `rig-build-locks`, worktree
`/mnt/data/projects/Softland-rig-build-locks`. Newest entries at the bottom
of "Log".

## Lock primitives (for the micro store's build)

The commit above holds `rig.store.locks` (pure part) and `rig.store.grammar`,
new files only. Signatures there:

- `(seal K plain)`, `(seal-with K plain nonce)`, `(open K sealed)` → bytes or nil;
  `(value-digest K plain)` → 32 bytes; `(same-bytes? a b)`;
  `(canonical-bytes v)`; `(decode-plain bytes)` → `{:value v :plain bytes}` or
  `{:refuse :malformed-value}`.
- `(lease-ids lease-name n)`; `(lock-id? x)`; `(lease-row K under entry nonce)`;
  `(unlease row entry)` → K or nil.
- `root-actor` (`:operator`); `(person-owner owner)`; `(wrap-of owner subjects marked?)`;
  `(wrap K w persons nonces)` → lock record or nil; `(unwrap record persons)`;
  `(wrap-persons w)`; `(seals-needed w)`; `(wrap-closed w persons)` → stamp or nil;
  `(erasure ledger-entry record persons)`; `(row-lock? kind marks)`.
- `(sealed? f)`, `(cited-ids facts)`; the value checks, plain data in, a
  refusal or nil out: `(does-not-open K sealed)`, `(shape-refusal grammars k v)`,
  `(subjects-refusal union)`, `(grain-refusal grain facts)`, and together, in
  L27's order, `(read-values facts delivered {:owner :carried :grain})` and
  `(value-refusal facts delivered ctx)`.
- `(check-resend facts rows R O)` → `:name-taken` or nil (the record path, V-F1);
  `(open-with row stamp T ledger-entry record persons)`, the pure half of
  `open-value>`.
- `(fresh n-locks n-nonces)`, `fresh-lock`, `fresh-nonce`, `fresh-bytes`: the only impure ones.

The parts digest is `env/digest` as of b0f025d5 (envelope.clj: every
`:sealed` replaced by `true`, every `:lock-id` removed); it is not in the lock
namespace because envelope.clj cannot require it. The sealed fact's parse
(`:not-sealed`, the control keys) is in the same commit.

Changed since the primitives commit:
- 3ddc6eab: every lock record carries `:scheme :aes-gcm-1` (`locks/wrap-scheme`,
  builder A from the copies examination; first-record placeholder), and the
  lock record's schema has `:scheme clojure.lang.Keyword`. A micro-side
  schema for lock records needs that field. `unwrap` opens an untagged or an
  `:aes-gcm-1` record and no other.
- 33b357a4: `rig.store.locks` now also requires `com.rpl.rama` and
  `rig.store.inject` (the dataflow ops); the pure functions' signatures are
  unchanged. Added pure helpers phase 3 may use: `lock-plan`, `lock-refusal`,
  `lock-effects`, `value-context`, `fresh-for`, `record-answer`, `row-at`.

## Log

- 02:15 to 02:45 Read the rama skill, phase-build and phases 3 to 7, the plan in
  full, its validation, SPEC.md, RIG.md's top sections, IMPLICIT_SPEC's lock
  sections, model.clj's locks, forgets and reads, scenarios.clj, phase 1's
  code and tests, the read exit plan's `open-value` and purge sections, the
  micro plan's use of the lock primitives.
- Builder A's additions taken: (1) a person forget's fan-out enumerates the
  values that die with the person on each task and passes them to the purge
  seam; (2) `read-as-of` and `:by-stamp` built as planned, the merge makes
  the read exit the one way to read; (3) the pure lock namespace committed
  first, new files only (above).
- Pure part written and checked in a scratch REPL script (seal/open, tamper,
  truncation, wrong lock, digests, the four wrap rows against A1 to A8 by
  hand, unlease, the value checks), then committed.
- 02:57 Primitives committed (1febfa3d), the line above (baff1330).
- Phase 3 (implement): envelope.clj (sealed facts, `:not-sealed`, the parts
  digest, the control keys; b0f025d5); the decision's lock part, the install
  functions and the dataflow ops in `rig.store.locks`, gate.clj's call sites
  and control-fact extensions, module.clj's schema merge and six call sites
  (33b357a4); the door in client.clj (b2c435d6). Checks before committing:
  every namespace loads with reflection warnings on (none); clj-kondo with
  the Rama hooks clean (targeted ignores where the hook does not read
  `declare-pstate` or `<<query-topology` inside a function, and on two
  interface parameters the bodies do not use); a create-test-pstate probe of
  the exact `$$layers` schema (31 checks: bytes in the nested row, the lock
  record in a row, `subselect ALL`, a point read past the end is nil,
  per-index termval on an existing row, the lease deletes, a session's map
  deleted whole, by-stamp ranges inclusive, MAP-KEYS on the top level); a
  first launch on the in-process cluster (seed 13 yes, a sealed act, its
  resend answered from the record, a reuse refused `:name-taken`, values
  opened through `read-as-of`, a value forget `:row-deleted` with its
  ledger date, Bob's forget leaving the mention of Bob open, Alice's forget
  closing it, the face refusals).
- Phase 3's self-check found F-SV1 and F-SV2 (reads before the decision could
  take a malformed key or index and throw); fixed in cc506303.
- Builder A, the copies examination: `:scheme :aes-gcm-1` on lock records
  (3ddc6eab); the per-task check of a forgotten person's entry is the A1 test
  in forget_test.
- Phase 4 (IMPLEMENTATION_VALIDATION-locks-and-forgetting.md): minor-fail,
  one new finding (F-IV1, `:allow-yield?` on two act-row reads in yielding
  ops), fixed in 3ddc6eab. Verdict routing: minor-fail goes on to tests.

- Phase 5 (tests, 6db2627a, dce34804): rig.store.lock-test (pure) and
  rig.store.forget-test (one cluster) new; phase 1's envelope_test and
  stream_gate_test adapted to sealing, each change marked at its place.
  For them: `inject/record-purges!` records the purge seam's calls when a
  test turns it on; `c/stock!` fills a session's pool ahead of acts whose
  stamps a test predicts.
- Phase 6 (TEST_VALIDATION-locks-and-forgetting.md): minor-fail, four
  missing cases (T-F1 to T-F4), added in phase 7's pre-loop (2d530361).
- Phase 7, the runs (each under the cluster lock):
  - lock-test + envelope-test (04:05): 727 of 728; the one failure a wrong
    expectation of mine (an AtomicLong prints "1", so the door's canonical
    text of it is a valid long).
  - forget-test alone, 4 tasks (04:05): 339 of 343; four test expectations
    (the store layer has no `:owner` key; a restarted door may take the lock
    another left unconsumed, so four copies cite the ids; byte-array maps
    compare by identity; an answer found by lookup carries no `:how`).
    Then 8 tasks (04:12): 344 of 344.
  - stream-gate-test alone, 2 tasks (04:12): 238 of 245 and an exception.
    A writer who holds a lease in the layer has the act's own permission
    checked and recorded, as phase 1 and the model answer; only a writer
    who cannot lease is refused `:no-such-lock` on its face, so three of my
    [V-F4] rewrites were wrong and went back to phase 1's expectation, with
    the pool stocked so the case is explicit. The exception was a real bug
    in the door: `refresh!` pruned the handed-out ids against a lease-locks
    result older than another thread's newer lease, so a lock in flight
    could be pooled again and cited twice (the second `:no-such-lock`,
    stampless, broke the burst's stamp arithmetic). Fixed in de86fd23: a
    lease's locks enter the pool once; one thread leases at a time. The
    burst went from 66 to about 1,000 acts a second. Then 4 tasks: 349 of
    349.
  - The whole suite (04:18 to 04:28): a phase 1 uuid7 test that made its
    uuids lazily after its window (a flake, fixed with doall); my
    schema-fit property checking what lease rows remained, which its own
    session closes delete (fixed to record what it wrote, and its tracking
    then called `seq` on a keyword, fixed); the fan-out crash fired twice
    because `inject/point!`'s countdown read then decremented while two task
    threads reached the point (made one swap). Then green three times in a
    row: 04:27 (8 and 2 tasks), 04:28 (4 and 4), 04:29 (4 and 2), 30 tests,
    1454 assertions, 0 failures, 0 errors, about 72 s each. The last run is
    runs/phase2-suite.txt, its full output runs/phase2-suite.log.

## Differences from the model's answers (findings, for phase 8)

- A value act by a writer who cannot lease in the layer (no permission, a
  permission for another layer or kept elsewhere, an unknown layer) is
  refused `:no-such-lock` on its face and recorded nowhere, where the model
  records the permission reason under the act's name; the rig records that
  reason under the lease act's name. A name so refused is decided fresh
  once the writer can lease (the model keeps it refused). A writer who
  holds a lease there gets the model's answer. (stream_gate_test, the
  refusals block.)
- The lease acts, the delivery, `:no-such-lock` and the session close have
  no counterpart in the model (the plan's revision item 20).
- The eight A cases through the module (A1 and A8 in personal layers, A2 to
  A7 in the base as the model's group cases): the same open or erased
  answer for every value, dated by the forget the model says closed it; no
  difference.
- By construction, as the plan lists: the `:people` layer and person acts
  (the model's person forget is not a fact); the person forget's date is a
  stamp on `:people`'s home; retracts get no lock (the model locks every
  non-control fact); per-act acts mixing marks die with any subject of the
  union (L6; not exercised by the A cases).

## Rig choices (can change without touching a record)

- An act carrying a lock control fact (`:forget`, `:lease`,
  `:session-closed`, `:person`, `:forget-person`) carries that fact alone;
  otherwise `:malformed-control`. Why: each such act's effect is one thing,
  and no act mixes a lease or a forget with values.
- Person facts are the operator's, and only in a layer of kind `:store`
  (the store layer `:people`); otherwise `:control-not-allowed`. Why: every
  person act is then ordered on one task, which L8's dates rest on.
- A making act whose owner's person lock is destroyed is refused
  `:person-forgotten` (the plan names `:no-such-person` for an owner with no
  entry). Why: the same reading as for a wrap person.
- An opened value's text must be the canonical EDN text of the value it
  reads as, else `:malformed-value`. Why: one plaintext per value, so the
  value digest has one input and a resend's check compares like with like.
- `:how` of a forget's answer on the record path comes from the ledger: the
  forget's own erasure when the ledger's date is its stamp, else nil. Why:
  a replayed forget answers as the first attempt did.
- The door: a value offer with no session is built in `:door/<who>`; it
  leases 64 locks at a time (more, up to 256 a lease, for a larger act),
  one thread at a time; a lease's locks enter its pool once (so no lock is
  handed out twice; a restarted door takes every unconsumed lock of the
  session); a lease refused leaves it no lock, so it cites the ids the
  refused lease would have minted, sealed under locks it throws away, and
  the gate answers `:no-such-lock` on its face; an offer keeps its locks
  until answered, and a resend after an answer takes new ones; a grain
  switch it sends makes it take the grain again.
- A value's domain is its canonical text's: the door seals the canonical
  EDN of the value it is given, so a Java float, an AtomicLong or a HashMap
  offered at the door is stored as the double, long or map its text reads
  as (the gate never sees the Java object); a text outside the domain is
  `:malformed-value`.

## First-record picks (placeholders; for the receipt and RIG.md)

- The plan's own: L3's sealed layout and plaintext (nonce ++ ciphertext ++
  tag over the canonical EDN's UTF-8), L4 raw bytes, L20 the lease fact,
  L21 lock ids `[lease-name i]`, L24 the sealed fact and the row's
  `:sealed`/`:lock-id`/`:lock`/`:digest`, L26 the parts digest without lock
  ids and the value digest keyed by the lock, L27 the reasons' names, L28
  the session-close fact; the root actor and the operator as one principal
  `:operator` (builder A).
- Added tonight: the lock record's `:scheme :aes-gcm-1` (builder A); the
  store layer's id `:people` and kind `:store` (L7's, named here as the
  build wrote them).

## Seams and stubs

- `locks/purge-read-indexes>` `[*layer *erased *forget-stamp]`, a no-op:
  the read exit's purge by value id, wired at the merge. `*erased` is
  `[{:fid [name idx] :row row} ...]`, the rows as they stood before the
  forget. A value forget calls it once, after the ledger write, with every
  value its lock erased (under per-act grain, every value of the act). A
  person forget calls it in each task's fan-out child, once per value that
  dies with the person there (`purge-dying>`). The read exit's
  `purge-writes` also needs each act's stamp and the RE4 `:ix-kv` read;
  the merged body reads both itself.
- **The person forget's enumeration is over any per-event budget at
  scale** (builder A asked to say so): on each task it iterates every act of
  every layer homed there (yielding), and for each act naming the person
  reads its rows, their lock records and persons: O(acts on the task)
  iterations plus a few seeks per value that dies. For a person with
  100,000 values on a task that is minutes inside one fan-out child. A kept
  store needs an index from person to the locks wrapped under them (one
  write per wrap person per value at admission), or a read exit whose
  index purges by person, or the read exit's paged rebuild per affected
  layer run outside the event. Tonight the seam is a no-op, so the scan
  costs I/O and changes nothing.
- `:by-stamp` holds stamp to name only, no value and nothing that confirms
  one, so no forget needs to reach it. `read-as-of` and `:by-stamp`
  duplicate the read exit's `[:all]` read: the merge makes the read exit the
  one way to read and keeps these only as an internal body or a test helper
  (builder A).
- The holder road's body of `deliver-lock>` is described in the plan, not
  built (L25).
