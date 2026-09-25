# Plan validation — phase 7, "the three numbers, on the finished store"

Validated by Claude Opus 5.5 at max effort, in a fresh session on branch
`rig-plan-numbers` (worktree `Softland-rig-plan-numbers`), 26 September
2026 from 05:00 IST, against `PLAN-numbers.md` at `ae514e3e`, in the shape
of the rama skill's phase 2 (`references/phase-2-plan-validate.md`),
adapted to a measurement plan: default FAIL; the requirement quoted
verbatim; a concrete case traced for each check with the plan's lines
cited ("L…" is PLAN-numbers.md at `ae514e3e`) and the code's (a branch,
a commit, a file and a line, read with `git show`, never checked out); a
self-consistency check last. The fixes are applied in place in
PLAN-numbers.md, each marked `[F1]` … `[F12]`.

No cluster was started, no probe was run and no benchmark was run. Every
claim below is read from code or from a plan, and says which; a claim that
is only reasoned says so.

**Verdict: minor-fail.** The plan's shape holds: three harnesses on the
store's own road (door, leases, the gate's one event, the read exit), the
per-act write lists as data and a test that holds them against the store's
own growth, the lock rows picked out of a force-compacted copy with checks
that can fail, one JVM and one cluster per run under the relay's lock, the
thresholds assumed and judged at orders of magnitude. Every failure below
is a localized edit, and each is made. Four matter most:

- **The headline count depends on two later stages, and one of them is not
  in the binding points.** On the code as it stands (`rig-wave1` at
  `d83e5ff8`, phases 3 and 2 merged onto the read exit), the constant
  `reads/seed-hints` indexes `:note` by value, so the plan's primary act (a
  `:note` fact "with no grammar") writes `:ix-kv` and `:ix-of` too: 10
  writes today, 11 once `:ix-s` lands. It is the plan's 9 only after phase
  6 replaces the constant (F1).
- **T1 and T3 fail on a correct harness** once phase 6 lands: the first
  admitted `:note` fact in a fresh layer writes a `:key-rows` row, and the
  tests demand "every other field by 0" (F2).
- **Variant C's reads are refused as written:** the exit's entry offer
  carries the reader as `:who` and no session, so the session's permission
  does not cover it, and a reader that is not a person cannot lease (F3).
- **Number 3 measures latency only in closed loops** and does not say that
  such latencies hide queueing (coordinated omission), while its threshold
  is an arrival-rate statement (F5).

## Sources, read by this session

- The rama skill: `SKILL.md`, `references/phases.md`,
  `references/phase-2-plan-validate.md`, `references/artifact-plan-validation.md`;
  `references/pstate-schema.md` L128-139 (size tracking and `(view count)`).
- SPEC.md whole (phase 7 at L112-116; "Tests and evidence" L186-194).
- RIG.md "Overnight state" (L3-106), "Defaults taken overnight, not ruled"
  (L459-541; default 7 at L532-537), "Numbers so far" (L765-793).
- README.md "The yardstick for the numbers" (L126-143) and "The numbers,
  measured on slices of the stream store" (L309-403).
- PLAN-numbers.md whole, at `ae514e3e`.
- The slices: `src/rig/bench/lock_slice.clj` (its requires and schema),
  `test/rig/bench/lock_bench.clj` L1-260 (its helpers, `compact-copy`,
  `value-spec`), `test/rig/bench/stream_bench.clj` (its definitions), and
  `runs/phase7-lock-growth.txt` whole, `runs/phase7-agent-rate.txt` and
  `runs/phase7-one-thread.txt` at the lines section 10 quotes.
- The built code, by `git show`:
  - `rig-wave1` at `d83e5ff8` (the merge of phases 3 and 2 onto the read
    exit, committed after the plan was cut): `module.clj` whole, `gate.clj`
    whole, `reads.clj` L1-260;
  - `rig-build-locks` at `fd41f6d2`: `locks.clj` L150-420, L570-830,
    L890-1412; `client.clj` whole (its `locks.clj` and `client.clj` are
    byte-identical on `d83e5ff8`);
  - `rig-build-micro` at `e60c8ee1`: `permit.clj` whole,
    `micro_client.clj` L455-493;
  - `rig-2026-09-25` at `703b8e26`: `read_exit.clj` L1-127, `reads.clj`
    L755-790.
- The validated plans at the sections the plan cites:
  PLAN-locks-and-forgetting.md L1898-1902; PLAN-read-exit.md L1176-1180 and
  L1530-1536; PLAN-reads-rest.md "The delta" (L816-860) and "The mark at
  write" (L1153-1185); PLAN-micro-store.md L594-599;
  PLAN-tools-and-grammars.md 4.6 (L587-612), 4.7 (L616-680), the changes to
  earlier stages (L1376-1415) and L1416-1423; PLAN-promotion.md L34-48 and
  L505-548.
- The Rama 1.6.0 jar's class listing, for the serializer and key-encoding
  classes 6.5 names; the relay folder's listing, for the lock file.

## C1. The measures: does each number measure what Sid's text asks, on the finished store?

**Source** (SPEC.md L112-116, verbatim): "7. The three numbers, each with
its method and the machine it ran on: index writes per second with an
agent session layer writing small acts continuously; lock store growth
under hand layers, bytes per value and the curve over a hundred thousand
values; one person's layer on one thread, acts per second and latency."
RIG.md default 7 (L532-537): "Lock growth is reported for 40- and 200-byte
values."

**Trace.**

- *Agent rate.* Variant A (L390-429): one layer `{:kind :agent :owner
  :ada}` with a session `:bench-s1` opened beneath Ada's root permission
  (L365-375), K writers in closed loops with no pause, every act a
  one-fact `:note` value of 40 canonical bytes sent through `c/offer!`
  (the door seals it under a leased lock; the gate decides it in its one
  event). At K = 16 on the slice's rate (2,249 acts a second) the window
  admits about 33,700 value acts and, at 64 value acts a lease, about 527
  lease acts; index writes a second = (33,700 × 9 + 527 × 72) / 15 ≈ 22,750
  (*derived*, on the plan's counts). The rate is "continuously" (no pause),
  "an agent session layer" (kind `:agent`, written in an opened session),
  and "index writes per second" by 2.5's sum (L276-289), with the
  read-index subset beside. It runs on the store's own module and road,
  not on a slice. **Holds, subject to C2's count.**
- *Lock growth.* `h40` and `h200` (L678-679): 100,000 values each into
  `{:kind :hand :owner :alice}` at per-value grain, ten points; at each,
  the real `[:alice-hand :locks]` rows sized in Rama's serializers
  (logical) and on disk from a force-compacted copy (L691-727). "Under
  hand layers", "bytes per value", "the curve over a hundred thousand
  values", 40 and 200 bytes: each is there. **Holds, subject to C5's
  method.**
- *One person's layer.* `{:kind :personal :owner :pat}` on one task of
  four, each task on its own thread (L551-589): one sequential writer for
  6,400 measured acts, then K = 1 to 128. "Acts per second" (value and
  all acts) and "latency" (p50, p95, p99, max, mean). "On one thread":
  every offer is checked to be decided on the layer's home task (4.8's
  placement check). **The acts a second hold. The latency does not, as
  written:** every latency window is a closed loop, which C6 shows is
  subject to coordinated omission and does not measure the threshold's
  own statement (100 acts a second at p99 ≤ 20 ms). F5.
- *Method and machine.* The `META` line is recorded by the harness at every
  run (L293-315) and copied into every result file with the caveat of 3.3
  (L336-344). **Holds.**

**Flaws found:** number 3's latency (C6).
**Verdict: FAIL** on the latency part; fixed by F5.

## C2. The index-write count, write site by write site

**Source.** SPEC.md L113-114: "index writes per second with an agent
session layer writing small acts continuously". The plan's claim (L33-41,
L207-254): 9 writes for a small agent act with no grammar, 11 under a
by-value grammar, 10 for a personal or hand act, 72 for a lease act of 64
locks, 10 for a read entry act; a lease act every 64 value acts.

**What "index writes" counts.** Every `local-transform>` path write of the
gate's decision event for an admitted act, sets and deletes, on any
PState, `$$clock` included (L209-216). The slices counted the same way
("INDEX WRITES PER ADMITTED ACT: 4", runs/phase7-agent-rate.txt L54;
README L326-327 names the four, the clock among them). The narrow reading
(`:ix-*` only) is printed beside it. This keeps the numbers comparable and
shows both readings. **Holds.**

**The trace.** The merged decision event is `rig-wave1` `d83e5ff8`
`module.clj` L127-209: the name's record read (L127), then for an
undecided name the settings (L138), the delivery (L140), the clock
(L144), the permission rows (L147-153), the heads rows (L155-161), the
lock context (L162), `gate/decide` (L163), and, when it decides, the
writes (L168-204). `decide*` (`gate.clj` L335-390) computes every list.
For an admitted one-fact value act, with no replace, nothing stood on and
no control fact:

| # | Plan row (L222-234) | Site on `d83e5ff8` / `fd41f6d2` | What the code writes for the agent `:note` act | Plan says |
|---|---|---|---|---|
| 1 | answer record | module.clj L169, `(keypath L :answers name)` | 1, yes or no | 1 |
| 2 | the act's rows | L172, one whole-vector `termval` | 1 | 1 |
| — | what it stood on | L174-176, one per carried entry | 0 (`:stood-on {}`; gate.clj L367) | 0 |
| — | replaced heads | L177-179, `NONE>` per replace | 0 (gate.clj L368) | 0 |
| 3 | the fact's head | L180-182, one per fact of a yes | 1 (gate.clj L369: every fact, control facts too) | 1 |
| 4, 5 | `:ix-ek`, `:ix-ke` | L185-187 over `:index-put`; reads.clj `fact-writes` L168 | 2 | 2 |
| 7 | `:ix-kv` | the same block; reads.clj L166-169, when `by-value?` | **1 today**: `reads/seed-hints` is `{:by-value #{:note} ...}` (reads.clj L41), and `decide*` passes `(reads/current-hints)` (gate.clj L356) | 0 |
| 8 | `:ix-of` | L188-190; reads.clj L170 | **1 today**, for the same reason | 0 |
| — | index deletes | L191-193 | 0 (only a purge deletes) | 0 |
| — | settings | L196-198, when `:settings` is not nil | 0: `:settings` is nil unless the act holds a setting fact (gate.clj L349-350, L371) | not listed; 0 |
| — | permission rows | L199-201 | 0: `[]` without a grant or revoke (gate.clj L373-386) | not listed; 0 |
| 10 | the cited lease row, consumed | `locks/write-decision>` L1209 → `consume-locks>` L1050-1061, one `NONE>` per cited id | 1 (`:consume` is the cited ids, locks.clj L797) | 1 |
| 9 | lock row | `write-decision>` L1210-1212 over `:lock-rows` | 0: `row-lock?` is false for `:agent` without `:own-row` (locks.clj L364-371), so the lock goes into the row (L738) | 0 |
| 11 | lease rows | L1213-1215 | 0 (a value act mints none) | 0 |
| 12 | stamp to name | L1236-1240 | 1: `lock-effects` sets `:by-stamp [stamp name]` for every yes (locks.clj L815) | 1 |
| 13 | the task's last stamp | module.clj L204, inside the decided branch | 1, yes or no | 1 |
| 6 | `:ix-s` | not built | 0 today; 1 once the rest of phase 5 lands ("one more `termval` per fact", PLAN-reads-rest.md L853-855) | 1 |
| — | `:key-rows` | not built | 0 today; phase 6 writes a row "for a yes only ... the first admitted fact under k in L sets `:used true`. Store keys get no row" (PLAN-tools-and-grammars.md L638-642) | "written by none of the workloads' acts" (L247-248) |
| | **total** | | **10 today** (1+1+1+2+1+1+1+1+1); **11** with `:ix-s` and today's hints; **9** with `:ix-s` and phase 6's hints | **9** |

The same trace for the other columns (plan L220-237):

- **Personal or hand value act**: the agent act plus one lock row
  (`row-lock?` true for `:personal` and `:hand`, locks.clj L364; the
  record is not put in the row, L738). 11 today, 10 on the finished store
  as planned. The plan's 10 holds on the finished store.
- **Lease act** `{:e s :k :lease :v {:count 64}}`: answer 1, rows 1, head
  1 (gate.clj L369 writes a head for every fact), `:ix-ek` and `:ix-ke` 2
  (`:lease` is in no hint set, so no `:ix-kv`), `:ix-s` 1 when built, 64
  lease rows (`lease-writes` L743-753, applied at L1213-1215), `:by-stamp`
  1, clock 1, no consumption (it cites no lock). 71 today, **72** on the
  finished store. The plan's 72 holds. That index writes are computed for
  control facts too is confirmed on the merged code: `decide*` passes the
  whole `log` to `reads/index-writes` (gate.clj L355-356).
- **Read entry act** (one `:read/point` fact, `:no-copy`, marked
  `:own-row` by the rest of phase 5, PLAN-reads-rest.md L1172-1180): answer,
  rows, head, `:ix-ek`, `:ix-ke`, `:ix-s`, a lock row (the mark), the
  consumption, `:by-stamp`, clock: **10** on the finished store. The
  plan's 10 holds. Today, without the mark, an agent layer keeps the
  entry's lock in its row: 8.
- **By-value act** (A'): the agent act's 9 plus `:ix-kv` and `:ix-of`:
  **11**. Holds.
- **The lease cadence.** The door leases `(max lease-size (- n have))` =
  64 locks when its pool for [layer, session] holds fewer than the offer
  needs (client.clj L27-31, L171-179), one thread at a time
  (`:lease-mutex`, L57, L171), and takes a lease's locks into the pool
  once (`refresh!`, L102-121). With no refusal, one lease act per 64
  value acts. Holds; 10.1 and 11.1 per value act (L252-254) follow.
- **Promotion adds nothing to a value act's event**: its `:forwards` is
  written only in a crossing's yes (PLAN-promotion.md L511-542), and lease
  rows gain two nil fields for non-landing leases (L543-545). Holds.

**Flaws found.**

1. **The primary act is not the 9-write act on the code as it stands.**
   `reads/seed-hints` hints `:note` by value, on `703b8e26` and on the
   merged `d83e5ff8` alike (reads.clj L34-41). Phase 6's plan replaces the
   constant: "`seed-hints` becomes `store-hints`, the store's own keys
   only ... the three callers of `reads/current-hints` ... take the rows'
   hints" (PLAN-tools-and-grammars.md L1381-1385), and "a key with no
   grammar in the layer is ... not opaque, no index hints" (4.6, L589-591).
   So variant A's `:note` act is 9 writes only once phase 6 is merged; on a
   store without it, variant A *is* A' and T1 fails. The plan's section
   4.2 (L384-388) reads the plan of phase 6 as the code, and B9 (L1322)
   names `reads/seed-hints` without saying it names `:note`. The build
   after the merges has to know this before writing the harness, and the
   plan has to say which key the primary act uses in each case.
   **FAIL → F1.**
2. **`:key-rows` is written by the workloads' first act in each layer.**
   The plan lists `:key-rows` as written by none of the workloads' acts
   (L247-248). Under phase 6 the first admitted `:note` (or `:mention`)
   fact in a layer writes one row. Outside the measured windows this is
   harmless (the warm-up makes it), but T1 and T3 start from fresh layers
   and demand "every other field by 0" (L1046-1047): a correct harness
   fails them. **FAIL → F2** (and C4).
3. `:by-stamp` may go ("whether it stays is builder A's call",
   PLAN-reads-rest.md L859-861); the plan already carries this as B4 and
   row 12's site note. `:ix-s` is planned, not built; row 6 says so. Not
   flaws; T1 settles both.

**Verdict: FAIL**, fixed by F1 and F2. With both, the plan's counts are
the finished store's as planned, and T1 to T4 hold the harness's list to
whatever the merges built.

## C3. The reads of the decision event (2.4) and section 11's table

**Source.** The plan reports reads per act (L256-274, "reported, not a
number") and costs the dominant operation in its partitioning table
(L1247-1271).

**Trace**, on `d83e5ff8` (`module.clj`) and `fd41f6d2` (`locks.clj`):

- *Agent value act*: the name's record (module.clj L127); the settings
  (L138); the delivery's person entries, owner and writer, both `:ada`, so
  one `$$persons` read (locks.clj `up-front-persons` L572-583,
  `read-persons>` L1007-1024); the cited lease row (`deliver-lock>` L1040;
  its `:under` person is already read, L1042-1043); the clock (module.clj
  L144); two permission rows (`gate/pids-to-read` → `permit/chain` of
  `[:bench-s1 L L [:ada L L]]`, permit.clj L29-38); no heads; the wrap's
  person again, already read (`decision-reads>` L1083-1108); the key row
  (phase 6, planned). **8.** The personal act cites `[:pat L L]`, one
  permission row: **7.** Both hold.
- *Lease act with the door's query*: in its event, the record, the
  settings, the writer's person entry (`up-front-persons`: a lease reads
  its writer, L580-583), the clock and two permission rows: 6 (7 if the
  merged gate reads a key row for a store key, which phase 6's "store keys
  get no row" leaves open). Then `lease-locks` (locks.clj L1378-1392): the
  settings, one range read of `[L :leases s]`, and the `:under` person
  entry: 3 seeks and up to 64 plus in-flight rows iterated. **9 seeks**,
  not the plan's "six ... plus its `lease-locks` query (one seek)" = 7
  (L1252-1255, L1260-1264).

Weighted seeks with 9: (64 × 8 + 9) / 65 = 8.02 at N = 1, 16 and 128,
flat, because every read of an act is on the layer's home task whatever
N is. The table's conclusion stands; its number is wrong.

**Verdict: FAIL** on the lease row of the table; fixed by F10.

## C4. The harness tests: can each fail when the harness is wrong, and pass when it is right?

**Source.** SPEC.md L188-191: "Tests are `clojure.test` namespaces under
the rig folder's `test/` ... Every claim that a capability works says what
was run and what it showed." The brief: a test that cannot fail proves
nothing.

| Test (L1039-1085) | Fails when the harness is wrong? | Passes when the harness is right? | Verdict |
|---|---|---|---|
| **T1**, the write list against the store's growth, agent layer, 100 acts, 2 leases | Yes: every field's growth against the list; a field the store has and the list lacks fails; a listed write that did not happen fails. `(view count)` on a subindexed map with size tracking off is O(n) (pstate-schema.md L139), fine at 102 entries. The lease arithmetic (2 × 64 − 100 = 28) holds for a fresh door and a session no setup act leased in (client.clj L155-186) | **No**, once phase 6 lands: the first `:note` act writes a `:key-rows` row and T1 demands "every other field by 0" (C2, flaw 2). On today's code it fails for a real reason (`:ix-kv`, `:ix-of` +100 each, C2 flaw 1), and the plan must say what the build does then | FAIL → F1, F2 |
| **T2**, by-value grammar | Yes (`:ix-kv`, `:ix-of` by 100) | Yes: the grammar fact made the key row before the snapshot, so its count does not move; but the count cannot see the first `:note` act setting `:used`, a write the list should name | FAIL (incomplete) → F2 |
| **T3**, personal and hand layers | Yes (`:locks` by 100) | **No**, as T1: each fresh layer's first `:note` act writes a key row | FAIL → F2 |
| **T4**, twenty point reads in an agent layer | Yes (`:locks` by 20 is exactly the `:own-row` mark; the entries' id-index entries without value fields is `:no-copy`) | **No** with C's read spec: every read is refused (C7) | FAIL → F3 |
| **T5**, the counts the numbers are made of | Yes (value count against answers; two lease counts against each other and against `:answers`; `rate` and `percentiles` on a known list) | Yes | PASS; F5 adds the schedule accounting |
| **T6**, placement | Yes for windows (partition and clock deltas). For `layers-on-task!`, "returns only layers whose making grew the target partition" is the function's own criterion; checked with the same observation, a wrong observation (the wrong partition index, a delta read before the making act's ack) passes both | Yes | FAIL (the search half cannot fail) → F7 |
| **T7**, the lock-store measure | The pick: yes (the count, one prefix, bytes within 4 a row; the second hand layer makes the one-prefix check fail on purpose). The re-pack: "a positive size no larger than the whole store's" passes a re-pack that dropped the values or put the wrong entries | Yes | FAIL (the re-pack half is weak) → F8 |
| **T8**, the wrap | Yes (`:required` and the blob's 28-byte step, locks.clj L253-268, L290) | Yes | PASS |
| **T9**, the machine | Only on a missing field; `META` is a record, not a measure | Yes | PASS |
| **T10**, the verdict rule | Yes (pure, known cases) | Yes | PASS |
| **T11**, the values | Yes (the slice's first 100 values; exact canonical sizes) | Yes | PASS |

**Verdict: FAIL**, fixed by F1, F2, F3, F5, F7 and F8.

## C5. Lock bytes on disk

**Source.** SPEC.md L114-115: "lock store growth under hand layers, bytes
per value and the curve over a hundred thousand values". The brief: is the
method sound, including picking the lock rows out of a force-compacted
copy, and is the named fallback honest about what it can and cannot show?

**Trace.**

1. *The copy and the forced compaction* (L702-708) are the slice's
   `compact-copy` (lock_bench.clj L192-241): wait until the directory's
   listing holds still for 300 ms, copy every file but `LOCK` and the info
   `LOG`, open with `OptionsUtil/loadLatestOptions`, flush every column
   family, `compactRange` with `kForce`, measure, retry up to five times.
   It ran at every point of ten variants without error
   (runs/phase7-lock-growth.txt L308), and the writers are paused at each
   point (L671-672), so only Rama's own background work can move the
   files, which the wait covers. `compact-copy` is private and deletes its
   copy; the plan writes its own `compacted` (L1029), which must keep the
   copy open for the pick. **Sound.**
2. *The pick by value* (L737-744). The Rama 1.6.0 jar has
   `rpl/rama/util/nippy_serialization$thaw.class` (checked). Which values
   of a hand layer's `$$layers` are maps carrying `:scheme` and
   `:required` at their top? Lock records (locks.clj L953-958). Not rows
   (module.clj L23-34 with locks.clj L960-969: `:lock` is a nested field,
   nil in a hand layer), not index entries (the row's fields plus `:fid
   :stamp :erased-at :copy`, reads.clj L112-123), not answer records
   (module.clj L48-61), not lease rows (`{:under :sealed}`, with
   promotion's nil `:public :for`), not `:heads` (a long), `:by-stamp` (a
   name), `:erased`, `:permissions`, `:key-rows`, `:ix-of` or `:forwards`.
   **Sound by the schemas** (*derived*; T7 confirms). Not covered: the
   `subindexed` family also holds entries whose bytes may not be frozen
   application values (the references of nested subindexed structures:
   `:log`'s vectors, `:leases`' session maps, `:stood-on`; the size entries
   of `:log`'s vectors, whose tracking stays on, module.clj L65-67).
   Whether `thaw` accepts them is not known from reading. A thaw that
   throws must not stop the pick, and an unthawed entry must be counted,
   not dropped. → F9.
3. *The one-prefix check* (L745-752) does not define P. Taken as the
   longest common prefix of the picked keys, it needs no knowledge of
   Rama's layout (the slice showed only that a lock row's RocksDB key is 3
   bytes longer than its lock id's encoding, L286-287, which fits a prefix
   or a suffix). If the reference id comes first, only lock rows start with
   P and the check holds. If not, P is a common start of vector encodings
   that `:answers` and `:log` keys (names are `[layer class :offer uuid]`,
   envelope.clj L252-255 on `d83e5ff8`) can share with lock ids (`[name
   i]`), keys that were not picked start with P, and the check fails into
   the named fallback, never into a wrong number. → F9 writes the
   definition.
4. *The count and size checks* (L753-755) can fail and are strong: the
   logical pass's exact row count, and raw bytes within a few a row of the
   logical sizes (the slice: within 3). **Sound.**
5. *The re-pack* (L756-759): the same DB and family options, the entries
   in key order, flushed, forced. Inside the whole store the rows under
   one prefix occupy their own run of data blocks, all but the two at its
   edges, compressed per block under the same options, so the re-pack's
   SST bytes are the rows' share of the store's bytes on disk, give or take
   two 4 KB blocks and their own index and filter blocks (*derived*).
   **Sound**; its test is weak (C4, F8).
6. *The fallback* (L760-764): "the whole store's compacted bytes times the
   lock rows' share of its raw key-value bytes", labeled "estimate", naming
   the failed check. Honest in its label, silent on what it cannot show:
   it assumes the lock rows compress as the store's average does. A lock
   row carries 60 bytes of random ciphertext (a 32-byte lock sealed under
   one person: 32 + 12 + 16, locks.clj L64-67) out of about 180, while the
   rest of the store mixes random bytes (sealed values, digests) with
   repeated keywords and addresses, so the estimate's error has no known
   sign. Two bounds hold whatever the layout: at least 60 bytes a row on
   disk (random bytes do not compress), and at most the row's raw
   key-value bytes plus block overhead (the slice's raw variant: 172.2 B
   raw, 86.9 B compacted, runs/phase7-lock-growth.txt L110). → F9: print
   both bounds beside the estimate and say what it assumes.
7. *B12* names `freeze`, `thaw` and `k-ser` but not the other internals
   the method leans on: the family names `default` and `subindexed` (the
   slice's finding, runs/phase7-lock-growth.txt L47-49, L93) and
   `OptionsUtil/loadLatestOptions` over Rama's written options. → F12.

**Verdict: FAIL** on the thaw handling, P's definition and the fallback's
bounds; the method is sound. Fixed by F8, F9 and F12.

## C6. Latency: coordinated omission

**Source.** SPEC.md L115-116: "one person's layer on one thread, acts per
second and latency". RIG.md default 7 (L533-535): "at least 100 acts a
second at 20 ms or less for the slowest 1 in 100". SPEC.md L192-194: the
in-process cluster gives orders of magnitude only.

**Trace.**

- *Number 3 (a)*, one writer, one act at a time (L568-578), and *(b)*, K
  writers each in a closed loop (L580-589). In a closed loop a stall (a
  disk flush, a GC pause, a lease round trip) delays only the offers in
  flight; the offers an arrival at the same rate would have made during
  it are never sent, so none of them records the stall. The measured p99
  is of the offers that were sent, which understates the p99 a person
  writing at a given rate would see. Concretely, at one writer and about
  300 acts a second, a 20 ms stall costs the closed loop one slow offer;
  an arrival at 100 a second makes two offers during it, each waiting
  part of the 20 ms; an arrival at 1,000 a second makes twenty (*derived*).
  The plan knows the principle (14.3, L1397-1400: "a closed loop at an
  assumed agent speed hides a queue at the task, because a slow ack only
  slows the writer") and applies it to variant B, not to number 3, whose
  threshold is itself a statement about a rate. Nothing in 5 or in the
  result files says number 3's latencies are closed-loop service times.
  **FAIL → F5**: open-arrival windows at 100 and 1,000 value acts a
  second, latency from the scheduled time, sent by enough threads that a
  late ack delays no other send; the latency threshold judged on them;
  (a) and (b) kept and labeled; the schedule accounting tested in T5.
- *Number 1 A*: closed loops; its latencies are reported, not judged (7.1
  judges the rate). They need the same label. Part of F5.
- *Variant B* (L451-457): a fixed schedule with a random phase, a send that
  falls behind goes at once, latency from its scheduled time. Free of
  coordinated omission in its accounting. One writer per session keeps at
  most S acts in flight at the task; a queue beyond that is in the client
  and is still counted from the schedule. **Holds.**
- *Timing*: `System/nanoTime` around `c/offer!` only (L400-401), latencies
  in primitive arrays (L1007-1010), the leased and unleased offers split at
  one writer (L575-578). **Holds.**

**Verdict: FAIL**, fixed by F5.

## C7. The workloads' roads: the door, D1, and variant C

**Source.** The brief's "on the finished store rather than a slice", and
the plan's own promise that every workload goes "through its own road"
(L28-32).

**Trace.**

- *One door for K writers* (L394-401) is the door as built: one pool per
  [layer, session], one lease at a time, a lease of 64 when the pool is
  short (client.clj L27-31, L155-186). Consequence, *derived* from that
  code: one door holds at most about 64 value acts in flight between
  leases, and each lease act queues on the task behind the acts already in
  flight. At K ≥ 64 the door, not the task, sets A's rate; the plan names
  this as one reading (10.2, L1123-1127). So D1's condition (L506-508)
  will very likely hold, and D1 will run in every agent-rate run.
- *D1's stocking* (L509-511): "`c/stock!`, leases of 256, until the pool
  holds the window's expected acts". `stock!` is `lease!` then `refresh!`
  (client.clj L322-332); `refresh!` calls `lease-locks`, whose query reads
  and unleases every standing lease row of the session in one range read
  (locks.clj `lease-locks>` L1378-1392, `leased-locks` L896-901); a
  stocked row is not consumed before the window. So the i-th `stock!`
  reads i × 256 rows. For a window of 18 s at an assumed 5,000 acts a
  second, 90,000 locks, 352 leases: Σ i × 256 = 256 × 352 × 353 / 2 ≈ 15.9
  million row reads and AES-GCM opens on the home task's thread, and as
  many locks sent back (*derived*, not run), on the order of a minute a
  run at a few microseconds each, missing from 8.6. One `lease-locks` call
  after the last lease reads 90,000. And if the pool runs dry inside the
  window, the door leases 64 at a time again and D1 is no longer "no
  writer waits on a lease" (L511). **FAIL → F4.**
- *Variant C* (L476-483): `read!` "with reader kind `:model` reading for
  `:ada`, ... the session's permission". The exit builds the entry with
  `:who` the reader and `:session nil` (read_exit.clj `entry-offer`
  L78-86, `703b8e26`); the door then writes it in `(default-session
  reader)`, `:door/<reader>` (client.clj `build` L85). The session's
  permission `[:bench-s1 L L [:ada L L]]` is held by `:bench-s1`, and
  phase 3's check accepts a holder only when it is the offer's `:who` or
  its `:session` (permit.clj L65-69). Both fillings of `:reader` fail:
  `:reader :ada` gives holder `:bench-s1` ∉ {`:ada`, `:door/ada`}, refused
  `:permission-does-not-cover-this`; `:reader :bench-s1` makes the door
  lease as `:bench-s1`, which has no person entry, refused `:no-such-person`
  (locks.clj `persons-refusal` L682-694), and the entry, citing locks that
  were never minted, is refused `:no-such-lock` (client.clj `assign!`
  L158-161). The rest of phase 5 leaves `entry-offer` as it is (its
  sessions are standing reads' closing, PLAN-reads-rest.md L746-748,
  L1040, L1077). As written, every C read and T4 is refused. What works:
  `:reader :ada`, `:reader-kind :model`, `:for :ada`, `:permission [:ada
  L L]`; the entries lease in `:door/ada` and the value acts in
  `:bench-s1`, one lease per 64 of each, still one lease per 32
  iterations, so 4.6's per-iteration count is unchanged. **FAIL → F3.**
- *Number 2's writers* (L668-674): 64 threads through one door, paused at
  every point. The door's lease of 64 makes rounds of about 64 acts and a
  lease; at the slices' p99 of about 22 ms at 64 writers plus a lease and a
  query, about 2,400 to 4,300 acts a second (*derived*), inside the
  plan's assumed 2,000 to 5,000 (L798-799). **Holds.**
- *Number 3* writes as `:pat` citing `[:pat L L]` in `:door/pat` (client.clj
  L65-69, L85); its reads (5.5) as `:reader :pat` write entries in the same
  session under the same permission. **Holds.**
- *Variant B*: a `c/connect` per session (client.clj L42-57), so a door per
  session. *Setup*: `make-layer-offer` sets per-value grain (L507-515),
  `make-person!` (L348-353), `grant-offer` (L517-521), `open-session!`
  grants `[S L L [p L L]]` (micro_client.clj L473-493). **Hold.**

**Verdict: FAIL**, fixed by F3 and F4.

## C8. The runs: the cluster lock, the background, progress, one cluster at a time

**Source.** RIG.md L89-92: "Every in-process cluster run waits on `flock
/mnt/data/projects/rig-relay-2026-09-26/cluster.lock`." RIG.md L82-85:
"long runs in the background with progress under `runs/`". README L789-790
of RIG.md: "Only one in-process cluster can run on this machine at a time
(port 2002)".

**Trace.**

- *The lock.* Every JVM that starts a cluster, each run and the test alike,
  runs as `flock <lock> clojure -M:bench|-M:test ...` from the rig folder
  (L841-850); `flock` waits with no timeout and holds the lock for the
  JVM's life. The file exists (checked: `cluster.lock` in the relay
  folder). A `flock -n` try first writes "waiting for the cluster lock" to
  the progress file, so a wait is told from a hang (L852-855). `report`
  starts no cluster and takes no lock (L879). **Holds.**
- *The background.* `nohup test/rig/bench/phase7-final.sh <set> >
  runs/phase7-final-driver.log 2>&1 &` (L862); per step a start line and an
  end line in `runs/phase7-final-progress.log`, the step's output in
  `runs/phase7-final-<step>.log` (L864-867); `PROGRESS` lines every 5 s
  and at every window's edges (L888-892). The `.log` files are ignored by
  git (the rig folder's `.gitignore` holds `runs/*.log`, checked), and
  nothing a result needs lives only in them (L893-896). **Holds.**
- *One cluster at a time, across JVMs*: the lock. **Holds.**
- *One cluster at a time, inside one JVM*: the test namespace opens "one
  cluster for the namespace at `{:tasks 4 ...}`, and one at `{:tasks 1
  ...}` for T7" (L1035-1037). If the first is a fixture around the whole
  namespace, T7's `create-ipc` runs while it is open and fails to bind port
  2002, as the slices' first attempt did ("Address already in use",
  runs/phase7-lock-growth.txt L296-299). `flock` cannot help inside one
  JVM. The plan must say the two are never open at once. **FAIL → F6.**
- *The busy port* is retried every 30 s up to five times (L906-908);
  *the overlap monitor* lists other Java processes every 2 s (L900-905). At
  validation the only other JVM on the machine was a rig test JVM of
  another session (`-Xmx8g`, the `:test` alias), so no permanent JVM would
  mark every window (checked with `ps`). *Free space* is checked before
  each step (L1299-1300). *Failures* (L940-946): a failed `test` stops the
  driver; any other failure is logged and the driver goes on; nothing is
  rerun by the script. **Hold.**

**Verdict: FAIL** on the two clusters in one JVM; fixed by F6.

## C9. The thresholds, and conclusions at orders of magnitude

**Source.** RIG.md default 7 (L532-537): "The numbers are judged against
the thresholds assumed in this file and README.md ... which are assumed,
not Sid's." SPEC.md L192-194: "only orders of magnitude mean anything, and
the numbers say so."

**Trace.** 7.1 (L808-817) quotes the three thresholds as default 7 has
them, says "assumed and not Sid's", and every result file prints
"thresholds assumed (RIG.md default 7), not Sid's"; Q1 (L1357-1358) puts
them to Sid. 7.2 (L819-824) keeps the slices' rule: ten times or more on
one side decides, within ten times the in-process cluster cannot, each K
judged on its own with the side and the factor named. The caveat of 3.3 is
in every file. Bytes (7.3, L826-835): logical bytes are exact for Rama
1.6.0's serializers and are judged directly; compacted bytes are judged the
same way with the RocksDB-options caveat; both denominators are printed,
because the threshold does not say which bytes it means. T10 tests the
rule on the slices' own cases (10,000 far above 1,000; 2,249 near; 4.7
times over four). Section 10's readings compare with the slices and name
what would move a verdict; none states a timing finer than the rule
allows. With F5, number 3's latency verdict is taken on the open-arrival
windows under the same rule.

**Verdict: PASS.**

## C10. The binding points B1 to B13

**Source.** The brief: the build after the merges must know what to
confirm. The plan: "Each is a name this plan uses that tonight's merges
may move or rename" (L1308-1310).

**Trace.** Each of B1 to B13 (L1314-1326) names a site today and what the
harness does with it, and T1 to T11 fail if one is wrong in a way that
matters. Read against the code: B1 (`rig.store.module/Store`, module.clj
L105 on `d83e5ff8`), B3, B4, B5's named pieces, B6, B7 (micro_client.clj
L482-493), B8 (permit.clj L29), B11, B13 (`"*offers"`, `"lease-locks"`,
`"read-point"`, `"read-pattern"`: module.clj L107, locks.clj L1402,
reads.clj's queries) are where the plan says. What the list does not name,
though the plan or C1 to C8 lean on it:

1. The content of `reads/seed-hints`: `:note` by value (C2, flaw 1). B9
   names the constant, not what it says.
2. The first-use `:key-rows` write (C2, flaw 2). B2 lists `:key-rows` as
   planned, not the write the first fact under a key makes.
3. `read-exit/entry-offer`'s `:session nil` and the reader as `:who`
   (C7). B10 names `read!`, not the entry it builds.
4. `c/connect` (every `with-store`, and a door per session in B),
   `c/offer-until-answered!` (setup) and `c/lease!` (F4). B5 names
   neither.
5. The RocksDB family names `default` and `subindexed` and
   `OptionsUtil/loadLatestOptions` over Rama's written options (C5,
   item 7). B12 names the serializers only.
6. The slice bench's helpers and what loading them needs:
   `rig.bench.lock-bench` requires `rig.bench.lock-slice` and uses
   `rig.store.envelope/canonical` (lock_bench.clj L30-38, L82-94). 9.1
   names the copy fallback; no binding point names the dependency.

And one statement is stale: 2.1 (L166-168) says no merge has committed
and `rig-wave1` stands at `703b8e26`. At validation `rig-wave1` stands at
`d83e5ff8`: phase 3 merged (`f29f792d`), then phase 2 (`d83e5ff8`), with
the seams RIG.md "Unfinished" item 1 lists still to wire ("The rest of the
seams are wired in the commits that follow", its message). The build reads
the final branch anyway; the plan should record what the trace on
`d83e5ff8` showed, so the build knows where the list already agrees.

**Verdict: FAIL**, fixed by F12 (with the notes of F1 to F4).

## C11. Time

**Source.** The brief: is the run-time estimate plausible, and does a
minimum set exist if time is short?

**Trace.** Recomputed from the windows (8.6, L918-938; 4.9, 5.6, 6.7):

- *Agent rate*, a run: JVM 40 s (*assumed*) + setup 10 + idle 10 +
  warm-up 20 + A's six levels 108 + two more 36 + A' 54 + D2 18 = 296 s;
  260 s without the two extra levels. The plan's "about 4.6 minutes" holds
  without D1.
- *One person*, a run: 40 + 20 + 20 + (a) 26 + (b) 104 = 210 s. Holds.
- *Lock growth*, a 100,000-value variant: 40 s + setup + 100,000 values
  at 2,400 to 4,300 a second (C7) = 23 to 42 s + ten points, each a copy,
  a forced compaction and a scan of a store growing to an estimated 150
  to 250 MB, a few seconds each growing with the store. About 2 to 3.5
  minutes. Holds.
- *Not in the estimate*: D1, which C7 shows will likely run in every
  agent-rate run: with F4's linear stocking (352 leases of 256 at a few
  milliseconds each, then one range read of 90,000 rows, *derived*) and its
  18-s window, about a minute a run, three more minutes. As written, its
  quadratic stocking adds more. F5's open-arrival windows: 2 × 18 s a run,
  about two more minutes. So the minimum set is about 42 to 45 minutes,
  the full set about 65.
- *A minimum set exists*: 8.2's `min` (L869-879). *A shorter one does
  not*, for when less than 40 minutes remain before the numbers are due.
  One run of each timing step and the two main lock variants: `test` 3,
  `agent-rate-1` about 6 (with D1), `one-thread-1` about 4, `h40` and
  `h200` about 2.5 each, `report` 1: about 19 minutes, at the cost of the
  spread across runs, which the report must then say is unknown.

**Verdict: FAIL** (the estimate misses D1 and has no set shorter than 40
minutes); fixed by F4, F5 and F11.

## C12. The template's sections, read for a measurement (the plan's section 15, checked)

- **Query topologies, PState schemas, topologies, internal depots, stream
  correctness**: none is added; the harness drives the store's own module
  (L1189-1245). Not applicable, with that reason. The micro store's
  microbatch topology is present and idle, and its idle cost is recorded in
  a 10-s window (L329-332). PASS.
- **Partitioning**: C3; flat at N = 1, 16 and 128 with the corrected lease
  row. PASS with F10.
- **Production readiness, as it applies to a harness.** Concurrent
  clients: K writers share the door, whose pool locking is phase 2's (a
  lease's locks enter the pool once, one thread leases at a time:
  `de86fd23`, client.clj L102-121, L171-179); the harness's counters are
  per writer. A killed run leaves `RESULT` lines under its run id and no
  end line, and the summary uses complete runs only; `flock` lets go when
  the JVM exits (L1441-1443). A retried lease act (setup and the door use
  `offer-until-answered!`) appends twice: the two lease counts then
  disagree and the window is marked, which is the check doing its job. A
  worker restart is not simulated; the numbers are not about failover.
  Two clusters in one JVM: C8, F6. PASS with F6.
- **In-memory efficiency.** Latencies in growable primitive `long` arrays
  per writer (L1007-1010); A's largest window, about 150,000 offers, is
  about 1.2 MB. PASS.
- **Minimality, adversarially.** The simplest plan, rerunning the slices'
  benches on the merged store, fails twice (L1450-1455): the lock slice is
  not the store's lock rows, and the stream bench neither counts leases
  nor names or reads back the finished store's writes. Could
  `field-counts` go, with the read-back alone checking the list? No: the
  read-back shows that sampled acts' named writes exist; only a count of
  every field can show a write the list does not name. Could the re-pack
  go, with RocksDB's approximate range size instead? Only as an estimate
  over the same prefix, which the plan keeps as a cross-check. The extras
  (A', D1, D2, B, C, the per-act grain, the entry bytes, the repeat, 5.5)
  are each named with what they decide and what they cost. PASS.
- **Throughput, adversarially: does the harness load the task it
  measures?** Read-backs and placement reads at window edges, the overlap
  monitor and progress on client threads, CPU snapshots at edges
  (L1462-1467). One load the plan does not name: D1's stocking as written
  puts O(n²) reads on the home task before its window (C7). PASS with F4.

## C13. Spec coverage, clause by clause

Each clause of SPEC.md's phase 7 and "Tests and evidence", and RIG.md
default 7, traced to the plan's lines, with the fault and race checks as
they apply to a harness (a harness writes through the store's own road
and keeps no store state of its own).

| Clause (verbatim) | Traced to | Holds? |
|---|---|---|
| "The three numbers, each with its method" | 4, 5, 6; the method header of every result file (L915) | yes |
| "and the machine it ran on" | `META`, recorded by the harness at every run (L293-310) | yes |
| "index writes per second" | 2.3, 2.5, 4.3; T1 ties the list to the code | with F1, F2 |
| "with an agent session layer" | 4.2: kind `:agent`, a session opened beneath the owner's root | yes |
| "writing small acts continuously" | 4.3: one 40-byte fact an act, closed loops with no pause | yes |
| "lock store growth under hand layers" | 6: kind `:hand`, the store's own `[L :locks]` rows | yes |
| "bytes per value" | 6.4, 6.6: logical, raw, compacted and re-packed | with F8, F9 |
| "the curve over a hundred thousand values" | ten points to 100,000; each step against the mean step (L778-780) | yes |
| "one person's layer on one thread" | 5: a personal layer, every offer decided on its one task (4.8) | yes |
| "acts per second" | 5.3, 5.4 | yes |
| "and latency" | 5.3, 5.4: closed loops only | with F5 |
| "Tests are `clojure.test` namespaces under the rig folder's `test/`, run with `clojure -M:test <ns> ...`" | `rig.bench.numbers-test` (9.3), step 1 of the driver | with F6 |
| "Every claim that a capability works says what was run and what it showed. A claim that was only reasoned says so." | the plan's marks (L15-20); the result files (8.5) | yes |
| "Measurements (phase 7) state the method, the machine and the run. ... only orders of magnitude mean anything, and the numbers say so." | 3.3 in every file; 7.2 | yes |
| Default 7: "Lock growth is reported for 40- and 200-byte values." | `h40`, `h200` in the minimum set | yes |
| Default 7: "judged against the thresholds assumed ... not Sid's" | 7.1; every file's line | yes |
| RIG.md L82-85: "long runs in the background with progress under `runs/`" | 8.2, 8.3 | yes |

*Fault and race checks, as they apply.* A worker restart mid-run: not
simulated; an erring run is marked (L1443-1445). A retried offer: C12. A
run killed mid-way: partial lines the summary ignores (L1492-1493). Two
sessions' clusters: `flock` across JVMs; two clusters in one JVM: F6.
Concurrent writers into one door: the door's own locking, as built.
