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
