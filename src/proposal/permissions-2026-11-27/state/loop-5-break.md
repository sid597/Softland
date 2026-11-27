# Loop 5 Blind Cluster Breaker Report (`loop-5-break.md`)

Executed on 4-task Rama `1.6.0` `InProcessCluster`s behind `/mnt/data/projects/Softland/src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock` against `git diff baa19ffe..HEAD` (`Store` + `Auth` + `door` + `read-path` + `runtime/seam` + `sign-in` + `command` CLI over Jetty).

- **Gate, replay & fixture harness:** `break_gates.clj` (`20 / 20` probe suites, `exit 0`)
- **Read-path, `:base` skew & runtime harness:** `break_reads_runtime.clj` (`30` probes across 6 suites, `exit 0`)

---

## 1. Reproducible Implementation Breaks Found & Fixed in Loop 5

### Fixed Break 1: `read-acts-once` torn read between `own-rows` (`:who-may-read-acts`) and `"lane-rights"` (`:acts` standing)
- **Exact sites before fix:**
  - [read_path.clj](file:///mnt/data/projects/Softland/src-store/softland/store/read_path.clj#L337-L353) (`read-acts-once`) issued `select-now` (`own-rows`) and `invoke-rights-raw` (`"lane-rights"` `:acts`) as two separate asynchronous calls.
  - [acts.clj](file:///mnt/data/projects/Softland/src-store/softland/store/read_path/acts.clj#L358) read `:softland.store/who-may-read-acts` from `own-rows` while taking `:standing` (`:holder` / `:visitor` / `:outside`) from `snapshot`.
- **Reproducible failure before fix (`3e-acts-torn-own-rows-vs-lane-rights`):**
  1. Lane starts private with `{:e lane :k :softland.store/who-may-read-acts :v :everyone}` and a private act `{:e :vault :k :secret :v "PRIVATE-ERA-ACT-HISTORY"}` at stamp `1`.
  2. At stamp `2`, in **one atomic act**, the owner sets `:who-may-read-acts :members` and grants `:anyone {:see true}`. At **no** stamp in the lane's history was `(and (= :visitor standing) (= :everyone who-may-read-acts))` ever true.
  3. When an unauthenticated `read-once` (`[:acts 1 10]`) read `own-rows` at stamp `1` (`:everyone`) and `"lane-rights"` at stamp `2` (`:visitor`), `acts/reading` combined them and leaked the private-era `:thing` (`leaked-things = 1`).
- **Fix landed & verified:**
  - [lanes.clj](file:///mnt/data/projects/Softland/src-store/softland/store/read_path/lanes.clj#L176-L295): when `(= :acts question)`, `rights-begin` / `rights-resume` reads `[:fact (rows/place-key lane lane :softland.store/who-may-read-acts)]` inside the **same single-turn `"lane-rights"` execution** on the lane's task (recording its stamp in `:read-set` and `:who-may-read-acts` on the returned snapshot; `:see` queries are untouched).
  - [acts.clj](file:///mnt/data/projects/Softland/src-store/softland/store/read_path/acts.clj#L358): uses `(:who-may-read-acts snapshot)` (falling back to `(lanes/who-may-read-acts own-rows lane)` only when no snapshot setting is present).
  - **Post-fix receipt:** `3e-acts-torn-own-rows-vs-lane-rights` $\to$ `[PASS] leaked-things=0`; permanent cluster regression test added in [read_path_proof.clj](file:///mnt/data/projects/Softland/test-store/softland/store/read_path_proof.clj#L3211-L3245) (`a-read-of-acts-reads-standing-and-setting-at-one-moment`).

### Fixed Break 2: Direct `"lane-rights"` query topology accepted `:anyone`, `:anyone-signed-in`, and non-`author-party` IDs in `*asker`
- **Exact site before fix:** [lanes.clj](file:///mnt/data/projects/Softland/src-store/softland/store/read_path/lanes.clj#L95) (`valid-rights-query?`, called at [door_reads.clj](file:///mnt/data/projects/Softland/src-store/softland/store/module/door_reads.clj#L120)) checked `(or (nil? asker) (declaration/usable-id? asker))` instead of requiring `act/author-party?` per `CONTRACT.md` §2.1 & §2.4 (`*asker: author-party or nil`).
- **Reproducible failure before fix (`s2-collective-author-spoofing`):** Direct `foreign-invoke-query` on `"lane-rights"` with `*asker = :anyone` or `:anyone-signed-in` performed a depot PState read and returned `{:standing :may-not-see, :read-set {...}}` instead of `nil` (`read-path/read-once` and `read-live` were already protected by `query/check`).
- **Fix landed & verified:** Tightened [lanes.clj](file:///mnt/data/projects/Softland/src-store/softland/store/read_path/lanes.clj#L95) to `(or (nil? asker) (and (act/author-party? asker) (declaration/id-within-the-limit? asker)))`.
- **Post-fix receipt:** `s2-collective-author-spoofing` $\to$ `:lane-rights-direct-anyone nil`; permanent regression checks added in [permission_proof.clj](file:///mnt/data/projects/Softland/test-store/softland/store/permission_proof.clj#L821-L828) and [read_path_proof.clj](file:///mnt/data/projects/Softland/test-store/softland/store/read_path_proof.clj#L3222-L3240).

### Fixed Doc Hygiene: Stale Loop-1 docstring in `permission_proof.clj`
- Updated [permission_proof.clj](file:///mnt/data/projects/Softland/test-store/softland/store/permission_proof.clj#L14-L18) so it no longer claims the cluster module and live read are not yet built (`src-store/CLAUDE.md:31`).

---

## 2. Spec / Design Trade-Offs Surfaced for Sid's Morning Call (Left Aligned with Locked `gate-histories`)

### Open Call A (`[HIGH]` design question — locked `gate-histories` Reading #6 vs `PERMISSIONS.md` §6 item 20): Should `:anyone-signed-in` be capped to `:see`/`:write` (excluding `:give` and `:owner`)?
- **Where it sits:**
  - [decision.clj](file:///mnt/data/projects/Softland/src-store/softland/store/gate/decision.clj#L383-L390) (`owner-of-the-lane?`), [decision.clj](file:///mnt/data/projects/Softland/src-store/softland/store/gate/decision.clj#L491) (`give-fault`), and [decision.clj](file:///mnt/data/projects/Softland/src-store/softland/store/gate/decision.clj#L534) (`change-fault`) enforce `:anyone-only-sees` on `:anyone`, while letting `:anyone-signed-in` hold whatever `:allows` it is given.
  - This matches the frozen Loop 2A oracle (`src/proposal/runtime-2026-11-21/gate-histories/README.md:447`, Reading #6: *"Anyone at all holds see and nothing more, at a give and at a change. `:anyone-signed-in` holds what it is given"*, which actively generates `:anyone-signed-in` gives with `:member` / `:publisher` / `:owner`).
- **What `s2-anyone-signed-in-owner-probe` demonstrated on `InProcessCluster`:**
  - If an owner grants `owner-allows` (`:lent-from nil`) to `:anyone-signed-in` and ends her own `first-permission`, `owner-of-the-lane?` counts `:anyone-signed-in` in `$$task[:owners]` (`[:accepted 1]`), leaving zero named person owners. Any signed-in visitor (`:person.mallory`) can then act under that `:anyone-signed-in` owner give to grant herself personal standing `owner-allows` and end `:anyone-signed-in`'s give in one act (`[:accepted 2]`), locking out the creator (`:may-not-see`). Even without `:owner`, granting `:give` to `:anyone-signed-in` lets any visitor mint standing (`:lent-from nil`) permissions to third parties that outlive closing `:anyone-signed-in`.
- **Why left for Sid's word:** Changing this turns frozen `gate-histories` red (`README.md:428-432`: *"Where BUILD.md can be read two ways and a history walks it, the judge had to pick. A gate built on the other reading shows red here; that is a question, not a break"*). If Sid wants `:anyone-signed-in` restricted to `{:see, :write}`, it is a 4-line update in `decision.clj` + `gate-histories/reference.clj`.

### Open Call B (`[INFO]` architectural boundary of `Loop 0d` separate plain pushes — `1c`): Same-act `[write-secret, revoke-viewer]` push ordering
- **What `1c-unmodified-1act-secret-first-then-revoke` & `1c-reordered-fact-before-revoke-push` demonstrated:**
  - Under `Loop 0d` (where gathered multi-path pushes were measured at `45–50ms` at 900 viewers and rejected in favor of separate plain pushes + `lane-rights` `:read-set` validation), a plain fact push (`$$*-facts`) carries the fact's `:stamp` (`3`) without a lane-rights epoch.
  - If an author writes a confidential fact **and** revokes a viewer in the **same** act (rather than ending the viewer's give first and writing the confidential fact in the next act), and Rama delivers the already-open fact proxy callback before the `:permission` / `:holders` proxy callbacks, the viewer's held `:read-set` from stamp `1` still matches her not-yet-updated rights pushes for one moment, emitting `{:changed {[:doc :secret] ... :stamp 3}}` immediately followed by `{:anew {:left-out [{:why :may-not-see}]}}`.
  - Note: `read-once` does **not** leak (`2b` & `2c` `[PASS]`, because `read-once` asks `"lane-rights"` strictly after fact selects), and revoking in act $N$ before writing confidential data in act $N+1$ does not leak once the revoke settles (or if `read-path` also gates `:changed` on `fact-stamp <= max-rights-checked-stamp`, which would require a `lane-rights` query on ordinary fact writes). Matches `gate-histories/README.md:486` ("The two questions of the live read, for Sid").

---

## 3. `rama-pitfalls` Protocol Verdict (Post-Fix)

```text
RAMA-PITFALLS VERDICT for softland.store + runtime callers (baa19ffe..HEAD, post-Loop-5 fixes)

  1. EVENT BOUNDARY & HOP RETRY : PASS — streaming-gate and microbatch-gate run walk/decide in one zero-hop event on :lane's task; microbatch |all copies (:fact, :by-value, :holder-permission) commit atomically per batch; lane-rights runs in one turn between |hash and |origin (including :who-may-read-acts on :acts).
  2. SIDE-EFFECT RETRY TRACE    : N/A  — zero external IO, child processes, or cross-module appends inside Store topologies.
  3. PSTATE & MODULE OWNERSHIP  : PASS — 7 streaming PStates owned solely by "streaming-gate"; 7 microbatch PStates owned solely by "microbatch-gate".
  4. BACK-ARROW CHECK           : PASS — Store is pure log -> PState materialization; read_path.clj holds only ephemeral client proxies and per-read snapshots gated on live PState stamp equality.
  5. RETRY IDEMPOTENCE OF IDs   : PASS — zero random/clock calls inside topologies; deterministic UUIDv8 for initial lane permissions; stamps derive deterministically from $$task[:last-stamp].
  6. ACK & READ-AFTER-WRITE     : PASS — :ack on streaming, :append-ack + answer-by-act-id polling on microbatch.
  7. STREAM vs MICROBATCH       : PASS — streaming for low-latency lanes; microbatch for :base |all replication across all tasks.
  8. PROXYSTATE PATH SHAPE      : PASS — keypath and sorted-map-range only; open-count verified {:live-reads 0, :pushes 0} after leave and refused opens across all suites.
  9. SUBINDEXING & BOUNDS       : PASS — permissions-by-holder top-level {String Long}; $$task subindexes :last-stamp, :owners, :by-who, :strays; chain & holder-scan bounded at 32 (:too-many-permissions verified across all 4 read paths).
 10. HASH DETERMINISM & LOCALITY: PASS — both depots hash-by :lane; cross-task :base reads via home routing-id verified non-flickering across all 4 tasks.
 11. TASKGLOBAL HANDOFF         : N/A  — zero TaskGlobal objects in Store.
 12. POLICY & DURABLE OUTCOMES  : PASS — all gate checks durable in $$answers/$$line; valid-rights-query? enforces author-party on *asker; :acts snapshot reads standing + :who-may-read-acts atomically (Open Call A on :anyone-signed-in noted for Sid).
 13. LIFECYCLE & DUPLICATE IDs  : PASS — duplicate act-id idempotent, conflicting payload :not-mine, exact-act :yes single-use, ended/used permission UUIDs permanently closed (:a-permission-id-takes-one-permission).
 14. UNTRUSTED OBS & POISON REC : PASS — nil, primitives, sorted-maps, NaN/Infinity, java.io.File, java.util.Date, 257-char IDs, 33-perm floods, and supplementary Unicode (U+1D538) leave both gates healthy.
 15. RUNTIME PROBE & TEST BAR   : PASS — 20/20 gate/fixture breaker suites + 30 read/runtime breaker probes + all 8 test-store proofs + gate-histories + test-runtime/server/auth/wire 100% green; 102/102 refusal/word keywords covered; 10th run-again fixture verified 100% row-for-row.

OVERALL: PASS (2 implementation breaks fixed & proved; 2 spec/trade-off calls documented for Sid)
```

---

## 4. Summary of Executed Cluster Attack Matrix

1. **Mid-transfer callback races (`break_reads_runtime.clj` Suite 1):**
   - `1a` 1-act public $\to$ private (`end :anyone` + `grant :person.ben` + write `"v2"`) across all 4 adversarial callback delivery orders (`perm-before-holders`, `holders-before-perm`, `fact-first`, and `hold-holders-until-after-lane-rights`): `settled=true, flickered=false`.
   - `1b` 1-act 3-hop `:lent-from` lender swap for `:agent.bot`: `settled=true, flickered=false`.
   - `1c` 12 rapid back-to-back 1-act transfers cycling `:anyone` $\to$ `:anyone-signed-in` $\to$ `:person.ben` under concurrent 20-write load with per-table callback jitter: `settled=true, ended=false, flickered=false`.
   - `1d` In-place lender lower (`:see` removed $\to$ `:may-not-see`, silent while lowered) and in-place re-raise (`:see` restored $\to$ `:yes`): `PASS`.
2. **Microbatch `:base` copy skew & `read-once` ordering (`break_reads_runtime.clj` Suite 2):**
   - `2a` Cross-task `[:home :base]` across distinct Rama tasks during 1-act close-`:base` + grant-`:person.ben` + write `"base-private-v2"`: `ben-settled=true, ben-flicker=false, stranger-closed=true, stranger-leaked-v2=false`.
   - `2b` & `2c` `read-once` (`:place` and `:points-at`) concurrent revoke injected immediately after fact / `facts-by-value` select returns and before `"lane-rights"`: `PASS` (new secret withheld with `:may-not-see`).
3. **Act-line (`[:acts ...]`) standing & torn-read defense (`break_reads_runtime.clj` Suite 3):**
   - `3a`–`3d` `:holder` vs `:visitor` vs `:outside` (write-only holder, exact-act `:yes` holder, unlent agent) on both `:streaming` and `:base` under `:members` and `:everyone`, plus `:max-thing-weight-of-one-read` pagination: `PASS`.
   - `3e` Atomic `[:who-may-read-acts :members, give :anyone :see]` torn-read attack between `own-rows` and `"lane-rights"`: `PASS` (`leaked-things=0`).
4. **Deep (`32` vs `33`) & cyclic `:lent-from` (`break_gates.clj` Section 1):**
   - Depth `32` (`31` hops) accepted on `:streaming` and `:base` across all 4 tasks; depth `33` refused `:permission-lent-too-deep`; 31-hop cap/uncap and root revocation pass; self-loops, multi-fact mutual `A<->B` cycles, intra-act chains, retroactive `change` cycles, raw depot cycles, and planted cyclic rows all refused/bounded cleanly.
5. **Collective-holder spoofing, Option B `:everyone`, demoted `:given-by`, owner floor, replay, poison records & `tables.edn` (`break_gates.clj` Sections 2–5 & `break_reads_runtime.clj` Suites 4–5):**
   - `:who`/`:given-by`/`:asker`/`"lane-rights"` spoofing rejected; Option B `:everyone` matrix exact; demoted `:given-by` and promoted-target revocation blocked; all owner-floor bypasses (`nil`, lent owner, `:session` owner, `:exact-act` owner, lowering last owner, simultaneous 2-owner end) refused `:lane-would-have-no-owner`; `33`-permission holder flood returns `:too-many-permissions` across all 4 paths with `{:live-reads 0, :pushes 0}` cleanup; runtime `seam`, `sign-in`, `setup!`, and Jetty CLI verified end-to-end; `acts.edn` (`401` records) and `tables.edn` (`1,739` rows across 7 tables/gate) round-trip and replay 100% identically.
