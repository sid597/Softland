status:     DONE
commit:     recorded in git log on main (this commit)
verified:   permission-proof 71 of 71; :store and :store-module namespace loads clean; serialized 4-task InProcessCluster probe suite 251 of 251 checks green (streaming + microbatch :base via distinct-task home routing-id, :acts on :base and streaming, 35 single-act transfers with 40ms callback skew = 0 flickers, in-place lender lower/raise, >32 holder-scan bound -> :too-many-permissions, read-once post-fact lane-rights ordering, zero leaked pushes)
next:       Loop 4 (migrate remaining test-store/ Rama and read-path proof suites once Loop 2A Part 2 and Loop 3 both land)

# Loop 3: Ring 2 Rama gates, `lane-rights`, and the live read path

Executed locally on `main` from `67376767` in parallel with Loop 2A Part 2 (`gate-histories/` untouched; `test-store/` untouched except `permission_proof.clj`).

## What was built

### 1. Post-2B pure gate kernel updates (`CONTRACT.md` §2.3–§2.4, `BUILD.md`)
- `src-store/softland/store/door/declaration.clj`: added `:everyone` to `:give` in `shapes`; updated `owner-allows` to `{:see true :write :all :give #{:see :write :give :owner :everyone} :owner true}`.
- `src-store/softland/store/gate/first_act.clj`: tightened `first-permission?` to check `(= declaration/owner-allows (:allows v))` on the normalized `declaration/permission-value`.
- `src-store/softland/store/gate/decision.clj`:
  - `may-pass?` requires `(contains? may-give :everyone)` whenever `(contains? act/everyone to)` (`:anyone` or `:anyone-signed-in`), both on initial grant and on in-place raise (`allows-added`), while `:everyone` in `:give` is passed on only when the grantor's `:give` holds both `:give` and `:everyone`.
  - `ends-with-no-chain?` (`:given-by` branch) requires `(may-pass? (:allows acts-own) (:allows target) (:holder target))`, so a giver who was lowered in place cannot end or lower permissions holding rights she can no longer pass on.
  - `permission-writes` writes `[:holder-permission (by-holder is) (long stamp)]` on **both** grant and in-place change (and `nil` on end).
- `test-store/softland/store/permission_proof.clj`: updated owner shapes and added explicit checks for `:everyone` in `:give`, demoted `:given-by` ending/lowering, in-place `:holder-permission` stamp updates, and `lane-rights` snapshots / read-set gating (`71/71` assertions).

### 2. Pure `read_path/` namespaces (`CONTRACT.md` §2.4, `loop-0d.md` Invariants 5–19)
- `src-store/softland/store/read_path/words.clj`: added `:too-many-permissions` to `words` and `because`, and `:lane` to `carried`.
- `src-store/softland/store/read_path/query.clj`: tightened `:asker` in `shapes` from `:id` to `:author-party` (`:asker-not-a-party`), rejecting `:anyone`, `:anyone-signed-in`, and unprefixed keywords.
- `src-store/softland/store/read_path/lanes.clj`:
  - Bounded holder runs and candidate permission walks at `max-holder-permissions` (`32`) and `:lent-from` chains at `chain/max-lent-depth` (`32`), returning `:standing :too-many-permissions` when cut at either bound (never a false `:may-not-see`).
  - Added 4-arity `(rights-begin lane question asker session)` supporting `:see` (`:anyone` first so open-lane viewers share pushes) and `:acts` (`asker` first for `:holder`, then `:anyone`/`:anyone-signed-in` for `:visitor`, else `:outside`), while keeping 3-arity `(rights-begin lane asker session)` compatible with `record/in_memory.clj` and `gate-histories`.
  - Added `rights-snapshot` returning `{:standing ... :read-set {read-key expected-stamp-or-run-stamps}}`, `valid-rights-query?`, `holder-range-agreed?`, and `clean-holder-run`.
- `src-store/softland/store/read_path/sources.clj`: derived live watch pushes directly from `(keys (:read-set snapshot))` (`holders-source` + `permission-row-source`, reusing `own-rows-source` for the depot row), gated live standing on entry-for-entry stamp equality (`snapshot-matches?`), added `source-matches-snapshot?` for the `[t_query, t_open_watch]` arrival gap, and preserved held fact pushes while standing is `:not-known-yet`.
- `src-store/softland/store/read_path/answer.clj`: updated `meanings`, `only-rights-of?` (`#{:holders :permission}`), `whole-if-nothing-is-missing`, `told-or-waiting`, and added `after-rights`.
- `src-store/softland/store/read_path/acts.clj`: updated `asker-kind` and `reading` to consume `:acts` `lane-rights` snapshots (`:holder`, `:visitor`, `:outside`, `:too-many-permissions`).

### 3. Ring 2 Rama tables, module topologies, and `read_path.clj`
- `src-store/softland/store/record/tables.clj`: added `permissions-by-holder` (`{String Long}`) and subindexed `:owners` (`String -> subindexed UUID set`) in `task`.
- `src-store/softland/store/module/walk.clj`: rewrote `decide` (`deframafn`) to drive `softland.store.gate.walk` (`begin`, `reads`, `resume`, `outcome`) in one partitioner-free, yield-free turn on the deciding task, and updated `copies-of` to include `:holder-permission`.
- `src-store/softland/store/module/streaming_gate.clj`: declared `$$streaming-permissions-by-holder` and wired it into `walk/decide`.
- `src-store/softland/store/module/microbatch_gate.clj`: declared `$$microbatch-permissions-by-holder`, wired it into `walk/decide`, and replicated `:holder-permission` (`NONE>` on nil, `termval` on stamp) behind `(|all)` to every non-deciding task.
- `src-store/softland/store/module/door_reads.clj`: added `read-lane-rights` and the single-turn `"lane-rights"` query topology (`[*routing-id *lane *question *asker *session :> *snapshot]`) with `:max-amt 33` holder scans and zero intermediate partitioners or yields.
- `src-store/softland/store/module.clj`: updated `Store` documentation for the three query topologies; `door.clj` required no code changes (verified end-to-end against `Store`).
- `src-store/softland/store/read_path.clj`:
  - Wired 13 foreign handles (adding `$$*-permissions-by-holder` and `"lane-rights"`).
  - `read-once*`: tracks per-lane `fact-epochs` so `lane-rights` is always asked strictly after the lane's last fact select returns (including multi-round `:run` / `:entity` `run-now`, `:builds-on`, and `:points-at` `nearer-checks`).
  - `read-acts-once`: queries `lane-rights` (`:acts`) on the lane's own task alongside `own-rows`.
  - `open-push` & `on-callback`: enforces `max-holder-permissions` (`32`) on `:holders` pushes, ending or refusing with `:too-many-permissions`.
  - `read-live*`, `settle!`, `ensure-rights-queries!`, `on-rights`, `on-arrival`, `on-change`: non-blocking single-flight `foreign-invoke-query-async` per `(read, lane)` gated by `push-seq` vs `asked-seq` and `same-snap-retries` (advancing freely across arbitrary bursts of legitimate transfers while terminating non-progressing loops).

## Verification

| Check | Result |
|---|---|
| `clojure -M:store -m softland.store.permission-proof` | `14` tests, `71` assertions, `0` failures, `0` errors |
| `clojure -M:store-module` Ring 2 namespace loads | Clean (`:all-ring2-loaded`) |
| Serialized 4-task `InProcessCluster` probes (`flock .../runs/cluster.lock`) | `251` checks, `0` failures, exit `0` |

### `InProcessCluster` probes executed
1. **Streaming gate + post-2B kernel rules**: `:everyone` in `:give` (member vs publisher granting/raising `:anyone` and `:anyone-signed-in`, non-propagation without `:give`), demoted `:given-by` blocked from ending/lowering higher rights and restored upon re-raise, 1-act owner hand-over + `:lane-would-have-no-owner` + `$$streaming-task` `:owners` verification, exact-act `:yes` single-use, duplicate `act-id` idempotence vs conflict `:not-mine`, poison/stray record handling (`:no-act-id`), and `:asker-not-a-party` query rejection.
2. **Microbatch `:base` via distinct-task `home` routing-id + public push sharing**: empirically verified `home-lane` and `:base` hash to different tasks (`$$streaming-facts` cross-key probe), verified closed-by-default `:base`, live opening via `|all` copy of `$$microbatch-facts` and `$$microbatch-permissions-by-holder` to `home-lane`'s task, verified zero extra pushes across 4 additional signed-in + anonymous `read-live` viewers on open `:base`, and ran 10 single-act permission swaps on `:base` read via `home-lane` with `0` flickers on both `read-once` and `read-live`.
3. **Act-line (`:acts`) on `:base` and `:streaming`**: verified `:holder` (member on open `:base` sees all acts whole even under `:who-may-read-acts :members`), `:visitor` (withheld stamps except own `:who` acts under `:members`, all whole under `:everyone` for both signed-in and anonymous), `:outside` (only own `:who` acts + `:left-out`, no `:last-stamp`), and `:keep-a-read-of-acts-live` on `read-live`.
4. **35 single-act transfers with `40ms` callback skew**: injected `40ms` scheduled delays on `:holders` callbacks (transfers 1–18) and `:permission` row callbacks (transfers 19–35) during an open `read-live`; verified `0` `:ended` events, `0` `:may-not-see` / `:left-out` flickers across all 35 transfers, and exact agreement with final `read-once`.
5. **In-place lender lower/raise**: verified `$$streaming-permissions-by-holder` stamp update on in-place change, immediate write refusal on `:agent.bot`'s borrow when lender `:person.ben` loses `:write`, live transition to `:may-not-see` when lender loses `:see`, and immediate live recovery + write restoration when lender is raised back in place without touching `:agent.bot`'s borrow.
6. **Holder-scan bound (`> 32`)**: planted 32 write-only borrows before a valid `:see` borrow (`33` total); verified already-open `read-live`, `read-once` (`:place`), `read-once` (`:acts`), and fresh `read-live` all report `:too-many-permissions` (never `:may-not-see`).
7. **`read-once` post-fact `lane-rights` ordering & resource cleanup**: traced `select-async` vs `invoke-rights-raw` across `:place`, multi-round `:entity` (`run-now`), and multi-lane `:points-at` (`:builds-on` + `nearer-checks`), confirming final `lane-rights` runs strictly after each lane's last fact select; verified `open-count` is `{:live-reads 0 :pushes 0}` after `leave`.

## `rama-pitfalls` Verdict

```text
RAMA-PITFALLS VERDICT for Permissions Loop 3 (Ring 2 Gates, lane-rights, and Live Read Path)

  1. EVENT BOUNDARY & HOP RETRY : PASS — module/walk.clj `decide` has zero partitioners/yields; all 7 PStates commit in one task event; microbatch `|all` copy runs post-decide inside atomic batch retry; `lane-rights` runs in one turn between `|hash` and `|origin`.
  2. SIDE-EFFECT RETRY TRACE    : N/A  — Zero external I/O, process spawns, HTTP calls, or cross-module appends in any topology.
  3. PSTATE & MODULE OWNERSHIP  : PASS — All 14 PStates are owned and written exclusively by their single declaring gate topology (`streaming-gate` or `microbatch-gate`).
  4. BACK-ARROW CHECK           : PASS — Depots and gate PStates are sole truth; `read_path.clj` holds only ephemeral client proxies and per-read `lane-rights` snapshots gated on live PState stamp equality.
  5. RETRY IDEMPOTENCE OF IDs   : PASS — Zero UUID/clock generation inside topologies; act-ids, permission UUIDs, and `:door-time` arrive on the depot record; stamps derive deterministically from `$$*-task` `:last-stamp`.
  6. ACK & READ-AFTER-WRITE     : PASS — `door.clj` uses `:ack` for `*streaming-depot` and `:append-ack` for `*microbatch-depot`, treating acks as signals and reading authoritative outcomes via `"answer-by-act-id"`.
  7. STREAM vs MICROBATCH       : PASS — Streaming lanes use `stream-topology` for low-latency single-task writes; `:base` uses `microbatch-topology` with `|all` replication of `:fact`, `:by-value`, and `:holder-permission`.
  8. PROXYSTATE PATH SHAPE      : PASS — All `foreign-proxy-async` paths use `keypath` or `sorted-map-range` (never `[]` or raw subindexed sets) and close via `rama/close!` when the last reader leaves (`open-count` verified `{0, 0}`).
  9. SUBINDEXING & BOUNDS       : PASS — `permissions-by-holder` is top-level `{String Long}`; `task` subindexes `:last-stamp`, `:by-who`, `:owners` inner UUID set, and `:strays` inner String set; holder scans cap at `33` (`max-holder-permissions + 1`).
 10. HASH DETERMINISM & LOCALITY: PASS — Both depots partition by `(hash-by :lane)`, co-locating lane ordering, `:owners`, `:last-stamp`, `:answers`, and `:line` on the lane's task.
 11. TASKGLOBAL HANDOFF         : N/A  — Zero `TaskGlobalObject`s or executors in `Store`.
 12. POLICY & DURABLE OUTCOMES  : PASS — All permission standing, chain-cap, `:everyone`, demoted-giver, and owner checks execute post-depot inside `gate/walk` with durable answers/line rows; `lane-rights` runs on-task and preserves `:left-out` / `:too-many-permissions` words.
 13. LIFECYCLE & DUPLICATE IDs  : PASS — `gate/walk` checks `[:answer act-key]` first (idempotent replay on same digest, `:not-mine` on conflict); permission UUIDs never reopen after end or exact-act use.
 14. UNTRUSTED OBS & POISON REC : PASS — Non-maps and malformed records are isolated by `declaration/stray-reason` before domain logic; `"lane-rights"` guards entry with non-throwing `lanes/valid-rights-query?`.
 15. RUNTIME PROBE & TEST BAR   : PASS — Verified `:store` + `:store-module` loads, `permission-proof` (71/71), and 251 checks on 4-task `InProcessCluster` including adversarial skew, flood bounds, replay, and strays.

OVERALL: READY-TO-CODE (IMPLEMENTED & VERIFIED)
RESIDUAL RISKS (acknowledged, deferred): Existing test-store/ Rama and read-path proof suites outside permission_proof.clj remain on the pre-Loop-2B permission shape and belong to Loop 4 per BUILD.md / prompt scope.
```
