# Implementation validation — wave 1, the merge of phases 2 and 3 onto the read exit

<!-- Phase 4 of the rama skill (phase-4-impl-validate.md, template
artifact-impl-validation.md), done inside the build session (phase-build.md
step 2) by the builder, Claude Opus 5.5, 26 September 2026, in worktree
/mnt/data/projects/Softland-rig-wave1, branch rig-wave1. Default verdict
major-fail. What is validated: the merge resolution and every seam wired
tonight, not the two builds' own code, which their sessions validated
(IMPLEMENTATION_VALIDATION-locks-and-forgetting.md,
IMPLEMENTATION_VALIDATION-micro-store.md; the read exit's is
IMPLEMENTATION_VALIDATION-read-exit.md). Read for it: SPEC.md (phases 2, 3
and 5; "What Rama showed"); PROGRESS.md "Now" (the forget ruling, ruling 7,
7b, the sharpenings, the rig constraints) on main; RIG.md "Overnight state",
"For Sid", "Defaults taken overnight" and R1 to R19; the three builds' notes
and validations; PLAN-read-exit.md ("Purge and rebuild", the open seam, F5,
F6, F2); PLAN-locks-and-forgetting.md (the lease road, L23, the forget
effects); PLAN-micro-store.md (M3, §A, the fold); PLAN-reads-rest.md where it
names wave 1's seams. Line numbers are the files' at the head this artifact
is committed with. -->

## What wave 1 changed, in one place

| Seam | Where | What |
|---|---|---|
| S1 the open step | reads.clj 410 to 440, locks.clj 1312 to 1349 | `reads/open-row>` is phase 2's `open-row>`; the pass-through and `open-double` are gone; a person cache form, `open-row-with>`, carried by every loop that opens rows (its calls at reads.clj 1092, 1124, 1216, 1272, 1298 and 1302) |
| S2 a value forget's purge | module.clj 206 to 211, reads.clj 276 to 340 | in the decision's group, after `locks/write-decision>`: `reads/purge>` over `:purge` of the lock effects, dated by the forget's stamp |
| S3 a person forget's purge | locks.clj 1253 to 1310, module.clj 138 to 139 and 219 to 220 | the fan-out child emits each value that died with the person (`locks/dying>`), dated by its wrap's close; the module purges each in that child; the record path purges again |
| S4 the value index's text | gate.clj 237 to 255, 360, 373 | `index-writes`' sixth argument from the values the gate opened (`plain-texts`) |
| S5 a sealed entry's moment | gate.clj 237 to 272, 361 | `stamp-for` counts a read entry's moment from its opened value (`with-opened`) |
| S6 the exit's entries at the door | client.clj (unchanged), locks.clj 576 to 606 | entries go through `offer-until-answered!`, which seals; an agent's or tool's lease is sealed under the layer's person owner (`lease-under`) |
| S7 `read-as-of` internal | locks.clj 1369 to 1380, 1421 to 1430; client.clj 391 to 400 | kept as the store's own view for tests and the operator, marked never a reader's path |
| S8 the put page's cap | test/rig/bench/put_page_bench.clj; reads.clj 889 to 896 | re-measured with real opens: a 4,096-row page of sealed values held its task about 0.47 s (0.16 s for rows that open nothing), over the read exit plan's 0.1 to 0.3 s for a put page, so the cap is 2,048 (runs/wave1-put-page-bench.txt; BUILD_NOTES-wave1.md) |
| S9 `:members` | envelope.clj 43 to 52; gate.clj 219 to 235, 357 | a control key, so the sealed parse takes a group's making act; refused at the stream gate (`stream-refusal`) |
| S10 persons | micro.clj (placeholder removed), micro_client.clj 147 to 162 | `$$persons` is the gate topology's; the micro door's person acts are phase 2's |
| S11 R19 | gate.clj 142 to 211 | the permission walk wins in `refusal` and `pids-to-read` (the merge kept it) |
| S12 a shared layer's value forget | micro.clj 169, 628, 730 to 830, 885 to 990, 1360 to 1402, 1455 to 1480 | the lock effect at the micro gate: excision or lock-row deletion and the ledger, on every entity the target's lock spans |
| S13 the micro fold and phase 2's `decide` | micro.clj `micro-decision` (unchanged) | reconciled: see "Plan conformance", item 8 |

## Findings (failures found by this validation, fixed in the source)

| # | check | failure | fix |
|---|---|---|---|
| W-V1 | idempotency / I/O | a sweep page compared an implied entry with the stored one by `=`, and a byte array compares by identity: after sealing every copy entry looked drifted and was rewritten on every sweep (T10 saw 8 rewrites of an intact layer) | `same-entry?` compares bytes by content (reads.clj 353 to 364, used at 389 and 394) |
| W-V2 | spec (the forget ruling, default 4) | an agent (a writer with a permission and no person entry) could not lease, so could not write a value or record a read: phase 2 sealed lease rows under the writer and refused a writer with no person entry `:no-such-person`; the read exit's agent readers were all refused | `lease-under` (locks.clj 591 to 606): a non-person writer's rows are sealed under the layer's person owner, default 1's "session owner"; W1-1 |
| W-V3 | spec (the envelope: no plaintext value outside the control keys the gate acts on) | adding `:members` to the control keys made the stream gate keep a `:members` fact's value as plaintext in a one-owner layer | `stream-refusal` (gate.clj 219 to 235): `:control-not-allowed` at the stream gate |
| W-V4 | minimise storage I/O (SKILL.md) | every row a read opened re-read its wrap's person entries, the owner's included: one seek of about three per sealed row, repeated for every row of the same person | `open-row-with>` (locks.clj 1312, reads.clj 428) and a person map carried by each loop (reads.clj 1092, 1124, 1216, 1272, 1298, 1302) |
| W-V5 | plan conformance (the micro plan's M3) | the first draft of the micro forget's gather carried the target rows whole to the leader: sealed bytes, digests, and lock records, one of which (an empty wrap's) holds its lock bare | `target-view` (micro.clj 767): ids, the layer and whether the lock is in the record; the excision is a path write on the entity task (`:lock-excise`, micro.clj 1462) |
| W-V6 | test / plan conformance | `rig.store.micro-test` compared a stream-era row "byte-identical" with `=` and expected the stream gate's permission reason on a value act whose lease the chain refuses | the row compared by its bytes; the chain asserted on the lease act (For Sid 14), each change marked in the test |

W-V1 and W-V2 were found by the suite's first runs after the merge; W-V3, W-V4 and W-V5 by the traces below; W-V6 by the first whole-suite run. Each was a localised edit.

## Redundant conditionals

- module.clj 206 to 211: `(<<if (seq *purge) (reads/purge> ...))`, one branch: no work when the decision erased nothing. Not redundant.
- reads.clj `purge>` 311 to 329: `(<<if (nil? *fid) (continue> ...) (else>) ...)` skips a malformed item; the `<<cond` over the act's stamp takes it carried, from this read's map, or reads it: three different sources, the third a seek. Not redundant.
- reads.clj 1200 to 1225 (the point read): `(<<if (nil? *row) (absent-row ...) (identity *qpc :> *qpc2) (else>) (open-row-with> ...))`: the branches do different work and both bind `*qpc2` so the attach point unifies. Not redundant.
- reads.clj 1284 to 1310 (showing kept entries): three branches, a row read and open, an entry open, a tombstone with no open. Not redundant.
- reads.clj 1153 to 1158 (the test-only `:put`): reads the row only with `:copy?`. Not redundant.
- locks.clj `open-row-with>` 1320 to 1342: ledger-closed (no further reads), record lock (persons), row lock (lock row, persons): three different read sets. Not redundant.
- locks.clj `lease-under` (pure): four outcomes, each a different person or none.
- micro.clj 1367 to 1402: `(<<if (some? *ftarget) (ops/explode [:own :target]) (else>) (identity :own))` then `(<<if (= :own *which) ...)`: the own-entity gather and the target gather read different PStates on different tasks. Not redundant.
- micro.clj 1455 to 1470 (block 2a, entity writes): `:lock-excise` (a `NONE>` into a row), `:locks-del` (a `NONE>` of a lock row), the default `termval`: three different paths. Not redundant.
- gate.clj `decide*` 355 to 358: `(or (refusal ...) (stream-refusal ...) (locks/lock-refusal ...))`, three different checks in order.

**PASS.**

## Consecutive keypath

Every path added tonight is one multi-arity `keypath`: `(keypath *layer :answers *nm :stamp)` and `(keypath *layer :ix-of *fid)` (reads.clj 322, 324); `(keypath *layer *ix *ia)`, `(keypath *layer :ix-of *ofid)`, `(keypath *layer *dx *da)` (reads.clj 331 to 339); `(keypath *layer :log (first *cfid) (second *cfid))` (reads.clj 1155); `(keypath *tname :entities)` (micro.clj 1393), `(keypath *te :log *tname)` (1397), `(keypath *te :erased *l8-lid)` (1401); `(keypath *route2 :log (first *k2) (second *k2) :lock)` (1463), `(keypath *route2 :locks *k2)` (1466), `(keypath *route2 :entities *k2)` (1479). A search for `(keypath [^)]*) (keypath` over the changed files finds nothing. **PASS.**

## Select-compute-transform

- `reads/purge>`: reads the act's stamp and the fact's `:ix-of` set, computes `purge-writes`, then `termval`s tombstones and `NONE>`s the value entries. Not an accumulation: each write sets a whole entry computed from what was read (the read exit's own `:purge` op does the same, validated there). No aggregator applies.
- micro.clj's forget: the leader computes the ledger entry and `termval`s it; the excision is a `NONE>` on the row's `:lock` field in place, with no read-then-write in the fold (W-V5). **PASS.**

## Unnecessary nil->val

None added (`grep nil->val` over the changed files: none). An absent `:ix-of` set reads as nil and `purge-writes` takes nil; an absent `:entities` map reads as an empty subselect. **PASS.**

## :allow-yield?

- `reads/purge>`: two point reads per value, no range. It runs inside a forget's event (the value forget's decision group; the fan-out child), after that event's writes: a yield there would let another event see the forget half applied. No yield, correctly; bounded by the act (a value forget's `:purge` is the target act's rows sharing one lock) or by one value (a person forget calls it once per value).
- `locks/dying>` (locks.clj 1269 to 1272): three range reads with `{:allow-yield? true}`, as phase 2 built them: the enumeration can be long (every act on the task, BUILD_NOTES-locks-and-forgetting.md). Traced for a yield falling between its ledger read and its purge: there is none: after `(local-select> (keypath *layer :erased *rlid))` come only `read-persons>` (point reads), `closes-with?` (pure), the emit and `purge>` (point reads, writes), none of which yields. A value forget landing in an earlier yield is seen by the ledger read, and the value is skipped (that forget purged it with its own date); one landing after the purge re-tombstones with its own date, which is also what the open step gives (the ledger first, L16). So the index's date and the open step's agree in every interleaving.
- The read loops' `yield-if-overtime` (reads.clj 1201, 1255, 1265, 1285) are the read exit's, unchanged. The person map now carried across them means a read that yields judges every row by the person entries as it first read them. Traced: a person forget landing in such a yield makes the rows opened later in the read still open where a fresh read would show them erased; before W-V4 the same read showed a mix (rows before the forget open, rows after it erased). Either way the read raced a forget, and its entry records what it showed; RIG.md For Sid 19 and 30 name this edge. One read, one view: named as rig choice W1-4, not a failure.
- micro.clj 1397: `[te :log tname] (subselect ALL)` with `{:allow-yield? true}`: an act's rows on one entity, unbounded in principle; micro-act does the same (1597).
- The ops source's pages stay unyielding (the read exit's D3), W-V4 adds no yield there.

**PASS.**

## Non-subindexed collections without size limits

- `$$micro-names [nm :entities]` (micro.clj 169): subindexed, size tracking off (`sub`).
- `purge>`'s `*stamps` map and every loop's person map live for one event or one query, never written.
- A forget's `:writes` in the fold: bounded by the target act's rows.
**PASS.**

## Stream topology idempotency

Every write wave 1 adds, traced for a replay before its event committed and after (SPEC "What Rama showed" 1, 2):

- **S2, the value forget's purge** (module.clj 206 to 211). Before the commit: the whole event is discarded and replays; the decision is made again (possibly at another stamp), and `purge>` writes that decision's tombstones; only the committed attempt's exist. Tested: `rig.store.wave1-test`, "the purge is the forget's own event" (a crash at `:after-writes` after the purge: the recorder saw the purge at least twice; the ledger and every tombstone carry the committed stamp). After the commit: the record path answers and writes nothing for a value forget (`record-answer` gives no `:fan-out`); the purge is not repeated, and needs not be.
- **S3, the person forget's purge** (module.clj 138 to 139, 219 to 220). The child replays whole (`:all-after`): `dying>` enumerates again and `purge>` writes the same tombstones, dated by `wrap-closed` over the same entries, and deletes what is already deleted. A recorded person forget resent: `record-answer` sets `:fan-out`, the fan-out runs again and purges again, idempotently.
- **`termval`s and `NONE>`s only**: tombstones at computed addresses, deletes of `:ix-kv` addresses and `:ix-of` entries. No id generated, no depot append, no counter.
- **Never throws**: `purge-fid` (reads.clj 276) is total; `purge-writes` is total (the read exit's); `lease-under`, `with-opened`, `plain-texts`, `stream-refusal` run inside `decide`'s guard; `open-row-with>` calls the same total primitives as `open-row>`. A read exit query calls `open-row-with>`, whose every step is total (`opens-at?`, `open-with`, point reads on validated keys), which matters because a query topology exception is fatal to the worker too (the read exit's probe).

**PASS.**

## Partial failure in stream topologies

The person forget writes across partitions: the home's entry, then `(|all)`, then each child's entry and purges. A child that fails replays the record from its start (`:all-after`); the home finds its record and takes the record path, which fans out again, and every child writes its entry and purges again (S3). The ack returns once the whole tree has run (phase 2's A1 test reads every task right after the answer; `rig.store.wave1-test` reads every purged field right after the answer). No write is left unexecuted. **PASS.**

## Single depot append per client operation

- `micro-client/forget-value!` (micro_client.clj 362): one act, one append (through `offer!`, which resends the same map after an error).
- The exit's `read!`: one entry act; the lease before it is the door's own act, answered by name, as for any value act (the lease road, phase 2's validation, "Single depot append").
**PASS.**

## Application-state caches survive restart

The person map in the read loops lives for one query or one page and is rebuilt from `$$persons` by the next; nothing survives an event. No TaskGlobal added. **PASS.**

## No reimplementation of built-in operations

`same-entry?` walks for byte arrays (no Rama built-in compares bytes by content); `spread-entities`, `forget-entities`, `lock-ids-of` are pure grouping. **PASS.**

## Plan conformance

The plans here are the three builds' (each validated) and the brief's list of seams (RIG.md "Overnight state", item 1). Every place wave 1 differs from what a plan or a builder's note said, with its reason:

1. **The purge lives in rig.store.reads and is wired in module.clj, not inside phase 2's `purge-read-indexes>`.** Phase 2's note: "The merge replaces this body with the purge". `rig.store.reads` must require `rig.store.locks` for the open step (S1), so `rig.store.locks` cannot require `rig.store.reads` back: a cycle does not compile. So `write-decision>` no longer calls a seam and leaves `:purge` of the lock effects to the module, which calls `reads/purge>` in the same group right after it; and `fan-out>` emits each dying value, which the module purges in the same child. The same writes, in the same events, at the same point. Correctness (it compiles). PASS.
2. **A person forget's purge is dated by the day the value's wrap closed, not by the person's `:erased-at`.** Phase 2's seam passed the person's date. For a value that needs two persons, one already forgotten, the open step (and a rebuild) gives the earlier date (`wrap-closed`: the earliest dead required person), and a purge dated by the second forget would move an erasure date that must not move ("erased on this date"). `dying>` emits `wrap-closed` over the entries as they stand on the task. Correctness. PASS.
3. **A person forget's purge writes tombstones in the id indexes** (the read exit's `purge-writes`), which is what the read exit's rebuild writes today for any row the open step reports erased. PLAN-reads-rest.md (wave 2) changes both together: a person purge deletes only the value-bearing entries, and the rebuild tombstones only a ledger erasure ("the invariant every purge keeps"). Wave 1 keeps today's invariant, under which purge and rebuild agree (T10: 0 deleted, 0 rewritten after a rebuild; `rig.store.wave1-test`'s dates). Conforms to the read exit's plan; wave 2's change is wave 2's. PASS.
4. **The open step carries a person map (W-V4)**; the brief named phase 2's `open-row>` `[layer fid row stamp T]`, which stays (reads.clj 410; locks.clj 1345) and which every read loop now calls in its cached form `open-row-with>`. The contract (three return shapes) is the same. Performance: one seek of about three per sealed row. PASS.
5. **An agent's lease is sealed under its layer's person owner (W-V2)**, a change to phase 2's lease road (L23 sealed under the lease act's writer, F2). Default 1 says the gate mints lease rows "wrapped under the session owner's person lock"; for a person writer that is the writer, which stays. The read exit's plan records agents' reads in their person's session layer (default 4, T15), which needs a lease there. Rig choice W1-1, no record's form changes. PASS.
6. **`:members` is refused at the stream gate (W-V3)**, an addition to phase 1's refusal list at one gate: the micro build's note asked for `:members` in the control keys, which alone would have let the stream gate keep a `:members` value as plaintext. Mirrors the micro gate's `foreign-control-keys`. PASS.
7. **The `$$persons` placeholder is removed, not set false.** The brief said "set `persons-placeholder?` to false". Turned back on, the placeholder would declare `$$persons` beside phase 2's and the module would not launch (the first compile of the merge showed it: "Shadowing not allowed of declared pstate vars"), so the flag had no state it could safely take but false; the declaration and its depot go. The divergence from the letter is named in the receipt. PASS.
8. **The micro fold and phase 2's `decide`, reconciled (S13).** The micro fold keeps `gate/refusal` and `gate/stamp-for` (its [PV-F13] adapter): phase 2's `decide` opens values, which the micro leader never holds (M3). Traced after the merge: `gate/refusal` now carries phase 2's control rules (a lock control fact alone; person acts only the operator's in a `:store` layer; the lease's and the close's shapes and authors), and `micro-extras` agrees with each (`lease-ok?` is `control-value-ok?`'s `:lease`; `close-ok?` its `:session-closed`; a forget at this gate is the operator's, stricter than phase 2's owner-or-operator, M14; person acts refused as another gate's). Phase 2's sealing and opening reach shared layers through its primitives: the door seals (`:not-sealed` on the face otherwise); the arrival task opens and checks by `locks/read-values`; block 2b wraps by `locks/wrap-of`/`locks/wrap` with ruling 7's placement (`row-lock?`: a personal or hand layer's kind, or `:own-row`, gives a lock row; a group's and the base's lock stays in the record) and default 6 (`person-owner` is nil for the root actor, so the base wraps by 7b as written, any-of, from day one, on the stream gate as on the micro gate); the reads open by `locks/open-with`. Evidence: the micro suite's six group A cases match the model; phase 2's base cases (forget_test, A2 to A7 in the base) and the base's re-wrap; `rig.store.wave1-test`'s group forgets. One gap for wave 2, named, not wave 1's: `gate/stamp-for`'s 4-arity at the micro leader holds no opened value, so a read entry written into a shared working layer would not have its moment counted; wave 2's shared reads carry the moment from the arrival task. PASS for wave 1's scope.
9. **The micro value forget (S12)** is not in any plan's text (the micro plan left it to "stage 2 through the row seam"); built to ruling 7 and phase 2's stream forget: the same outcomes (`:no-such-value`, `{:how nil}` on a second forget, `:excised` or `:row-deleted`, the ledger dated by the forget's stamp). Two choices it makes, each a rig choice: the forget fact's `:e` names the target's entity, as the model's forget does (W1-5); and an act whose one lock spans entities keeps them on its name row (`:entities`, micro.clj 169), a field the micro plan's schema lacks, so a per-act forget reaches every entity (ruling 7: "forgettable only as a whole") (W1-6). The micro answer carries no `:how` (the stream ack does); the ledger says it. PASS.
10. **`read-as-of` stays, internal by contract (S7).** Builder A: "kept as an internal body or a test helper". Kept as the query (the replays and the promotion plans use `c/read-as-of` and `c/opens?` as the store's own view); every docstring says it records nothing, checks no visibility and is never a reader's path. Enforcement would need a caller identity the rig does not have. `:by-stamp` stays (one write per admitted act): PLAN-reads-rest.md's `:ix-s` serves the same range and leaves "whether `:by-stamp` stays" to that build. PASS.

## Carried from phase 2, not wave 1's to fix

**The person forget's enumeration is over any per-event budget at scale** (BUILD_NOTES-locks-and-forgetting.md): `dying>` scans every act of every layer homed on the task, inside the fan-out child. The coordinator's instruction: record it, do not build the fix tonight unless small. The fix (an index from person to the locks wrapped under them, one write per wrap person per value at admission; or PLAN-reads-rest.md's paged `dying-with>` driven by the operator's `purge-person!`) is not small. Recorded in BUILD_NOTES-wave1.md for Sid and wave 2.

## Spec coverage (SPEC.md, the rulings, the defaults)

- **Phase 2, "value forget deletes the row; person forget destroys the person lock; every forget is a fact; time travel shows 'erased on this date' and nothing else from after its moment"**, now through the read exit: S1, S2, S3; `rig.store.wave1-test` ("the exit opens sealed values ...", the value and person forget blocks); the read exit's T8 and T19 with real opens.
- **Phase 3, "wrapped locks in the record"**, and the lock effect of a forget there: S12; `rig.store.wave1-test`, the group forget blocks.
- **Phase 5, "every index over values rebuildable from the log or purgeable by value id, so a forget reaches it"**: S2, S3 (purged in the forget's own event, before its answer); T10 (the rebuild reproduces the indexes, 0 rewrites, W-V1); the rig constraint "so forget reaches it".
- **The forget ruling, "gone for everyone including the past"**: after either forget, no index field holds the value's text or anything derived from it (the four fields scanned), the depot holds only sealed bytes (default 1), and a read as of any moment shows the date.
- **"What Rama showed" 3, a gate never throws**: every function wave 1 adds to a topology is total (traced under "Stream topology idempotency" and in the micro section below).
- **Default 1** (sealed at the door, leases): the exit's entries lease and seal like any offer (S6).
- **Default 4** (one exit; agent reads recorded): S6 with W-V2.
- **Default 6** (the base's lock rules from day one): item 8.

### The micro seam's totality (What Rama showed 3; a microbatch that throws retries for ever)

`forget-target-of`, `lock-ids-of`, `forget-entities`, `target-rows` catch every throwable; `assoc-some` and `target-view` take maps the schema made; `forget-effect` and `spread-entities` run inside `decide-envelope`, under `fold-envelope`'s guard per envelope, which answers `:gate-error` on its face. The new 2a writes take locations the fold computed from gathered keys (a normalised name, a long, a lock id read from a row). PASS.

## Verdict

**minor-fail**: six localized failures (W-V1 to W-V6), each fixed by editing specific lines of the existing namespaces or tests, with no restructuring; with the fixes in, every check above passes.

PHASE_VALIDATION:minor-fail
