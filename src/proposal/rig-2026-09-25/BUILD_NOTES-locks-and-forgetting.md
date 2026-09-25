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

The parts digest is `env/digest` once envelope.clj is changed (a later
commit, named below when it lands); it is not in the pure lock namespace
because envelope.clj cannot require it.

## Log

- 02:40 Read the rama skill, phase-build and phases 3 to 7, the plan in
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
