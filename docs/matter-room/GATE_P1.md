# matter-room — P1 slim gate record

2026-07-28 · Fable, fresh-context relative to the Codex implementation (this
session never wrote P1 source; P1.md used as input, not authority) · slim
tier per CONTRACT §8 and the 2026-07-27 cadence ruling · Sid delegated the
run and the commit ruling in-session ("Its your call").

## VERDICT: PASS

## Independently established (not taken from the receipt)

- **Suite on the current tree**: focused `app.material-portal-test`
  18 tests / 342 assertions / 0 failures / 0 errors; compat selection
  (reply-to-block + space-material + provenance-material) 21 / 276 / 0 / 0 —
  re-run in this session's own JVM against the exact tree that was committed.
- **Entity-mode byte pin held in the gate JVM**: sha `deb12d4d…`, 22,064
  UTF-8 bytes, and entity-plus-master equality — executable assertion, green
  here, pin value cross-attested by the contract (pre-P1 provenance).
- **Here-key sentinel enumeration is complete**: tree-wide grep for
  `:*/…here…` keywords yields exactly the 9 keys in the three sentinel lists
  (5 master, 3 deviation, 1 wearer) — closing the self-reference loophole
  where the test iterates the same lists the implementation uses.
- **Room id re-derived with zero shared code**: UUID v3 of `fm:attention`
  computed from raw `md5sum` + version/variant bit surgery =
  `a983e774-33f1-384f-aa7f-3cf319fd7c75` — matches the receipt, the served
  mapping, and this session's suite output.
- **Diff trap spot-checks** (full diff read): anchor mode only when
  `:entity-id` absent; one-pass anchor wearer filter *before* all anchored
  consumers with `wearer-snapshot?` captured pre-normalization (the
  falsifier's leak fix, its counterexamples now fixtures); `true?` PINNED
  guard; blast basis at section + per-master + rendered card; experience
  address/material-ids explicit and surviving sub-serve fallback; entity-mode
  2-arity `bindings-of` delegation behaviorally identical (and byte-proven);
  `binding-material/table-conflicts` pre-exists (`binding_material.cljc:598`,
  file untouched); CLJS `room-id` throws, tables empty, client consumes the
  served mapping; no new depot/PState/topology/module/durable owner/write
  path anywhere in the diff.
- **Fence held exactly**: `git status` = the 5 §11/PLAN-§P1 code paths + the
  2 phase docs; every other dirty path is the parked foreign write-set
  (multi-cascade R2, shared docs, probe files, late `vision/LOG.md` touch);
  index was empty before this session staged; forbidden/conditional sources
  zero-diff.
- **Cross-JVM sha behaves as the retired claim says**: this JVM's anchor
  canonical sha (`3e1ec791…`) differs from the receipt's pin (`706f9d9c…`)
  because runtime-minted revision ids differ per boot; in-JVM double-open and
  `*print-namespace-maps*` equality held in THIS run — exactly the shipped
  G1 determinism form. Not a finding.

## Residue (non-blocking)

- clj-kondo receipt ("0 warnings") reproduces only with a populated project
  analysis cache; a cache-less 5-file lint shows 58 cross-namespace
  unresolved-var warnings, all on vars that provably exist. 0 errors both
  ways. Receipt-tooling footnote, not code.
- CLJS `:dev` compile accepted on the receipt (272 files, 0 warnings at
  final source state; tree unchanged since) — not re-run to avoid touching
  the live dev build cache while Sid's `:8080` server runs unattended.
- G10 live half accepted on receipt coherence (fields match the code paths
  read; room id independently confirmed; p95 23.6ms < 52ms) — not re-driven.
  G10 headed half is Sid's at wear, contract-sanctioned non-blocking.
- No HEAD-dynamic suite reads these paths (the code-atom scanner is pinned
  per `381c445`), so the post-commit HEAD-dynamic re-run rule has no targets.

## Ruling and eligibility

P1 is eligible and the commit ruling is executed under Sid's delegation:
code commit `7a3127c` (exactly the 5 files), docs in the commit carrying
this record. P2 is opened from this boundary per P1.md's starter; veto is
one `git reset --soft` away and reaches Sid before anything is pushed
(nothing is ever pushed from this branch).
