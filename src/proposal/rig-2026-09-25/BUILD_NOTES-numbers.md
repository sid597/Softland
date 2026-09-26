# Build notes — phase 7, the three numbers, on the finished store

*Builder: Claude Opus 5.5, reporting to builder C. Worktree
`Softland-rig-build-numbers`, branch `rig-build-numbers`, made off
`rig-2026-09-25` at `c9684356` (26 September, 07:40 IST). Built by
`PLAN-numbers.md` as validated (`PLAN_VALIDATION-numbers.md`, F1 to F12
applied in place).*

**Status.** Prep. The binding check below is read from wave 2's three
branches, before their merge; no cluster has been started on this branch.
The harnesses are built in two steps: now, the parts that do not depend on
wave 2's entry points; after wave 2 lands here, the rest, each tested with
one short run. The minimum set runs only when builder C says the store is
finished.

Marks as in the plan: *code* (branch, file and line), *plan*, *derived*,
*assumed*. Paths are relative to the rig folder.

## Binding check (26 September, 07:15 to 07:50 IST)

**What was read, and how.** Wave 2's three branches, read-only, with `git
show <branch>:<path>` from this worktree and the Read tool in their own
worktrees, each diffed against `c9684356` (this branch's base, wave 1 and
the review merged):

- `rig-build-promotion` at `5303418f` (phase 4),
- `rig-build-reads-rest` at `33817364` (the rest of phase 5),
- `rig-build-tools` at `2d2f666a` (phase 6, stream side).

Their merge, `rig-wave2`, had no commit of its own at the time of reading.
So each row says what the branch that touches the point has; where two
branches touch the same point, what the merge must reconcile is named, and
the harness confirms it on the merged tree (tests T1 to T4 count the store's
own growth, so a merge that changed a count fails before any number runs).

| # | Binding point | Plan | Wave 2's code | What the harness does |
|---|---|---|---|---|
| B1 | the store module | `rig.store.module/Store` | unchanged name on all three (`defmodule Store`: promotion module.clj L107, reads-rest L106, tools L106); `*offers` still `(hash-by :layer)` | `rtest/launch-module!` of `m/Store` |
| B2 | `$$layers` fields, `$$clock`, `$$persons` | stage 1, 2 and 5a fields; `:ix-s` and `:key-rows` planned | **built**: `:ix-s` (reads-rest reads.clj `layer-fields` L136-148), `:key-rows` (tools grammar.clj `layer-fields` L135, row `{:used :grammar}` L119), and one field the plan did not name, `:forwards` (promotion promote.clj `layer-fields` L233, written only in a crossing's yes). `$$clock` and `$$persons` unchanged | `field-counts` enumerates the fields from the store's own field functions, so `:forwards` is counted and must grow by 0 |
| B3 | the decision event's writes | module.clj's decision block | **moved** into `rig.store.gate-event` (`write-decided>`, promotion and tools gate_event.clj L34; `record-or-decide>` L93/L97), verbatim, plus tools' key-row block (L84); `locks/write-decision>` unchanged; `reads/fact-writes` writes `:ix-s` for every fact (reads-rest reads.clj L165-184) | the write list's sites name `gate_event.clj`; counts unchanged (below) |
| B4 | `:by-stamp` still written | whether the merge keeps it | yes: `lock-effects` sets `:by-stamp [stamp name]` for every yes, unchanged on all three (promotion locks.clj L887) | row 12 of 2.3 stays |
| B5 | the door | client.clj `offer!`, `build`, `default-session`, `lease-size`, `:lease-mutex`, `:known`, `:pool`, `stock!`, `lease-locks`, `connect`, `offer-until-answered!`, `lease!` | unchanged; tools adds `lookup-many` (client.clj L268) and nothing else. Promotion: `lease-locks`' answer gains `:landings` (locks.clj `leased-locks`), which the door does not read | as planned |
| B6 | setup acts | `make-layer-offer`, `make-person!`, `grant-offer`, `:people` | unchanged | as planned |
| B7 | opening a session | `micro_client.clj` `open-session!` | unchanged, `rig.store.micro-client/open-session!` (L513 on `c9684356`): for a layer placed by layer it sends `c/grant-offer` of `[S L L [p L L]]` through `c/offer-until-answered!`; it needs `mc/connect`'s handles (its `settings-of` reads `$$micro`) | setup opens `:bench-s1` with `mc/open-session!` on an `mc/connect` handle |
| B8 | the permission walk | permit.clj `chain` | unchanged; `[:bench-s1 L L [:ada L L]]` walks 2 rows, `[:ada L L]` 1 (permit.clj L29-38, L44-78) | D2 as planned |
| B9 | grammar facts and hints | a fact per key; `seed-hints` hints `:note` by value until phase 6 | **built by tools**: `{:e <key> :k :grammar :v {:shape s :subjects-at p :opaque b :index #{h}}}` with exactly those four parts (grammar.clj L57, `refusal` L86); `:grammar` is a control key, so the door leaves it plaintext (envelope.clj L57); the operator or the layer's owner may write it (gate.clj L133); the stream gate takes hints from the layer's key rows only, `(reads/hints-of key-rows)` (gate.clj L410; reads.clj `hints-of` L54), and `seed-hints`' `:by-value #{:note}` is no longer read there. Keys stay keywords (`:e :note`). The first fact under a key that is not a store key writes that key's row (`key-row-writes`, grammar.clj L187; store keys: control keys, `:grammar` and the read keys, gate.clj `store-key?` L60) | **F1's first case**: the primary act's key is `:note`, 9 writes, provided the merged `decide*` keeps tools' `hints-of` (reads-rest's `decide*` still passes `(reads/current-hints)`, which the merge must replace). A' writes the `:note` grammar `{:shape [:any] :subjects-at nil :opaque false :index #{:by-value}}` (tools' toy grammar, test/rig/store/toy_grammars.clj) |
| B10 | the read exit | `read!`, `connect`, `entry-offer` (reader as `:who`, `:session nil`), `entry-facts` with `:own-row`, `reads/address` | reads-rest: **the entry now carries the reader's session** (read_exit.clj `entry-offer` L91-103, FRR10, first-record; `check-call` accepts `:session`, L64-76); every entry fact is marked `#{:own-row}` (reads.clj `entry-facts` L986-1014); `(reads/address :ix-s {:stamp s :fid f})` (L104-116); `read-exit/connect` also opens reads-rest's six new queries and two depots (L29-47) | **F3's second case**: variant C and T4 read as `:reader :ada`, `:reader-kind :model`, `:for :ada`, `:session :bench-s1`, citing the session's permission `[:bench-s1 L L [:ada L L]]`. Entries and value acts then lease from one pool, `[L :bench-s1]`: two locks an iteration, one lease per 32 iterations, as planned |
| B11 | the lock row and the wrap | locks.clj `lock-record-schema`, lock ids, `row-kinds`, `wrap-of`, `wrap`, `unwrap`, `open`, `canonical-bytes` | unchanged; promotion's `unlease` answers nil for a landing lease's row only, and its lease rows gain two fields, `:public` and `:for`, nil for every lease here (promotion locks.clj L1063) | as planned |
| B12 | Rama internals for sizing | `freeze`, `thaw`, `k-ser`; the families `default` and `subindexed`; `OptionsUtil/loadLatestOptions` | **confirmed** for the functions by a JVM with no cluster (07:48): `rpl.rama.util.nippy-serialization/freeze [bb v]`, `thaw [bb]`, `rpl.rama.api.durable.rocksdb.key-encoding/k-ser [bb v]`. The families and the options load are the slice's finding; T7 checks them on the merged store | `pick-lock-rows` thaws each value from a `ByteBuffer` |
| B13 | query and depot names | `"*offers"`, `"lease-locks"`, `"read-point"`, `"read-pattern"` | unchanged; wave 2 adds queries (`read-delta`, `standing-close`, `standing-open`, `entry-ids`, `micro-index-progress`, `task-layers`, `promotion-status`) | as planned |
| B14 | the slice bench's helpers | `rig.bench.lock-bench`, which requires `rig.bench.lock-slice` and `rig.store.envelope/canonical` | loads: lock_slice.clj requires only Rama and `rig.store.envelope` (L33-35), and `env/canonical` is unchanged on all three | reused as planned; the copy fallback is not needed |

**The per-act writes on wave 2's code** (*derived* from the three branches
read together; T1 to T4 count them on the merged store):

| Act | Writes | Plan |
|---|---|---|
| agent value act, key with no grammar | 9: answer, rows, head, `:ix-ek`, `:ix-ke`, `:ix-s`, the cited lease row consumed, `:by-stamp`, clock | 9 |
| the same under a by-value grammar | 11: the 9 and `:ix-kv`, `:ix-of` | 11 |
| personal or hand value act | 10: the 9 and the lock row | 10 |
| lease act of 64 | 72: answer, rows, head, three id indexes, 64 lease rows, `:by-stamp`, clock (`:lease` is a store key: no key row) | 72 |
| read entry act (`:read/point`, `:own-row`, no copy) | 10: answer, rows, head, three id indexes, the lock row, the consumed lease row, `:by-stamp`, clock (`:read/point` is a store key: no key row) | 10 |
| first act under a key that is not a store key, per layer | one more, the `:key-rows` row (in setup or warm-up, never in a window) | F2 |

Promotion adds no write to these acts: `write-decided>` is the same body as
tools' without the key-row block, and `promote-flow/continue>` (promote_flow.clj
L138-160) emits once and does nothing for an act that is not a promotion
request. Its reads per act are 2.4's (the key row read is tools' `:keys`,
gate.clj L341).

**What changes in the harness from the plan's text:**

1. Code sites: the decision's writes are in `rig.store.gate-event`, not
   module.clj; the write list names them there (B3).
2. The primary act's key is `:note` (F1's first case), to be confirmed on the
   merged tree by reading its `decide*` and by T1 (B9). If the merge left
   `current-hints` in the stream gate, the primary key becomes `:memo` and
   A' keeps `:note`, as F1 says.
3. Variant C and T4 cite the session's permission, with the session named in
   the read spec (F3's second case, B10).
4. Grammar facts carry exactly `:shape`, `:subjects-at`, `:opaque` and
   `:index`. Number 2's `:mention` grammar must admit four persons for
   `h40-p5`, so the harness writes its own: `{:shape [:map {:persons [:set-of
   [:keyword] 1 8]} {:open? true}] :subjects-at [:persons] :opaque false
   :index #{}}` (tools' toy `:mention` admits one or two, toy_grammars.clj
   L18-25). With the grammar a fact of the layer, the plan's fallback (the
   persons carried in `:subjects`) is not needed.
5. `field-counts` counts `:forwards` too, which must grow by 0 (B2).
6. Nothing else: the door, setup, session opening, permission walk, lock
   row, wrap and depot and query names hold as the plan has them.

**Still to confirm on the merged tree**, because it is the merge's choice,
not any one branch's: that `decide*` takes tools' `(reads/hints-of
key-rows)` and reads-rest's `:ix-s` together; that `write-decided>` keeps the
key-row block; and that step R and step 6b, which follow the merge, leave
the counts as they are (step R refuses control-key facts the gate does not
act on and changes pattern entries, neither of which these workloads make;
6b is the micro gate's). T1 to T4 are the check.
