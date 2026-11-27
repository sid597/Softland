# Loop 0 Empirical Facts: Rama 1.6.0 Probe, Codebase Boundaries & Proof Scan

**Date:** 27 November 2026  
**Scope:** Empirical Rama `1.6.0` `InProcessCluster` measurements, existing codebase boundary facts (`src-store/`, `src-auth/`, `src-runtime/`, `src-command/`, `src-server/`, `src-wire/`), and pointer to the complete mechanical `test-store/` check-sentence dump. Zero target stored shapes or gate rules are prescribed here.

---

## 1. Empirical Rama `1.6.0` IPC Probe Facts (`4` tasks, `2` threads/task, `6.94s`, exit `0`)

1. **Multi-proxy callback tearing across a single-act transfer:**
   - When a live reader evaluates standing directly from `@push` across two or more independent `foreign-proxy-async` handles, a single-act multi-row transfer on one task (e.g., ending public access and granting personal access in the same topology event) fires proxy callbacks asynchronously in arbitrary order.
   - Delaying the grant callback by `40ms` relative to the end callback produced intermediate `:may-not-see` screens: `[:yes :may-not-see :may-not-see :yes]`.

2. **Two-round client `foreign-select` skew:**
   - Executing two sequential `foreign-select` calls from a foreign client (across two PStates, or Round 1 child row $\to$ Round 2 lender row inside one PState) runs as two separate task turns. Committing a single-act permission swap between Round 1 and Round 2 returned `:may-not-see`, whereas a single-turn query topology on the task returned `:yes`.

3. **Single-task query topology + non-blocking `foreign-invoke-query-async`:**
   - A `<<query-topology` starting with `(|hash *routing-id)` and ending with `(|origin)` (zero intermediate partitioners, zero yields) executes all local PState reads on that task in one atomic single-threaded turn.
   - Treating PState proxy callbacks solely as invalidation triggers that invoke `foreign-invoke-query-async` (single-flight with dirty-retry, never blocking Rama callback threads):
     - **`:streaming` (`*routing-id = lane`):** Across `30` consecutive single-act permission swaps under forced `40ms` grant-callback delay (`34` snapshots observed), mid-transfer `:may-not-see` flickers = **`0`**.
     - **In-place lender lowering & raising (zero stored ancestor list):** Lowering a lender row in place immediately capped its borrower's queried rights; raising the lender row back in place immediately restored the borrower's rights without re-granting the borrower row or storing an ancestor list on it.
     - **`:microbatch` `:base` via reader `home` routing-id on a non-deciding task:** With `:base` hashed to `task=2` and reader `:home-0` hashed to `task=3` (`distinct-tasks = true`), copying both `$$microbatch-facts` and a secondary holder-keyed PState in the same `(|all)` microbatch block yielded **`0` proxy flickers** (`11` snapshots) and **`0` poll flickers** (`293` concurrent `2ms` polls routed to `:home-0`) across `10` single-act transfers on `:base`.

4. **Measured `foreign-proxy-async` counts (when `:read-after` stamps are omitted on shared triggers):**
   - **Public lane (`200` concurrent live viewers watching one entity/key):** `2` fact-table proxies (`own-rows` + watched key) + `1` holder-index proxy = **`3` proxies total** across all `200` viewers.
   - **Members-only lane (`200` distinct borrower agents under one lender + `250` concurrent sittings of one shared agent under that lender, watching one entity/key):** `3` fact-table proxies (`own-rows` + watched key + `1` shared lender row) + `203` holder-index proxies (`2` collective prefixes + `200` distinct agents + `1` shared range for the `250`-sitting agent) = **`206` proxies total**, below `read_path/query.clj:89` (`900`) and Rama's `pstate.proxy.limit` (`1000`).

---

## 2. Existing Codebase Boundary Facts (Today's Code Only)

- **Act & ID leaves (`src-store/softland/store/act.clj:36-39, 57-106, 163, 222-227`):**
  - `:id` accepts only `readable-keyword?` (no whitespace) or `uuid?`. `:e` on `:fact` is `:id`. `max-depth = 32`.
  - `digest/hex-for-a-yes` (`door/digest.clj`) hashes the maker's act after stripping `[:act-id :depot :door-time :permission]`.
- **Declaration & limits (`src-store/softland/store/door/declaration.clj:27-77, 110-235, 343-358`):**
  - `max-facts = 256`, `max-based-on = 1024`, `max-value-bytes = 65536`, `max-act-bytes = 18874368`, `max-id-text = 256`.
  - `store-keys` currently has `8` keys: `:softland.store/depot`, `:permission`, `:run-permission`, `:act-permission`, `:who-may-see`, `:who-may-read-acts`, `:builds-on`, `:marked-keys`.
- **Row keys & whole-lane scans (`src-store/softland/store/record/rows.clj:6-13, 49-115, 496-513`):**
  - PState string keys join printed `id-text` parts with `" "` (ASCII 32).
  - Whole-lane fact runs scan `[(str lane-text " "), (str lane-text "!")]` (`[:run :first :last]`), subject to `:run-start-places` (`256`) and `:max-places-of-one-run` (`20000`) in `read_path/query.clj`.
  - Fact and by-value rows store `:who` and `:permission` as optional `String` fields (`fixed-keys-schema` omits `nil` keys). `rows/use-row` writes `:value nil` with no `:by-index`, whereas an explicit retract fact row writes `:value nil` with `:by-index`.
- **Gate PStates, non-yielding walk & microbatch copies (`record/tables.clj:37-167`, `module/walk.clj:68-95, 145-152`, `module/microbatch_gate.clj:35, 165-182`):**
  - Each gate currently owns `6` PStates (`answers`, `facts`, `facts-by-value`, `task`, `line`, `things`) and decides on the lane's hashed task.
  - `walk/decide` holds zero partitioners and zero yields between first read and last write (`walk.clj:82-95`).
  - `microbatch_gate` copies only `walk/copies-of` (`:fact` and `:by-value` today) via `(|all)` to every task; `answers`, `task`, `line`, and `things` stay on the lane's deciding task.
- **Read path (`read_path.clj`, `read_path/{query,lanes,sources,answer,acts}.clj`):**
  - Fact reads route `:streaming` sources by `lane` and `:microbatch` (`:base`) sources by the query's `home` lane (`sources.clj:41-46`).
  - Act-line reads (`read_path/acts.clj:12-16, 92-133`) route **all** sources — including `:base` — by `lane` (`:base`'s deciding task, where `$$microbatch-line`, `$$microbatch-things`, and `$$microbatch-task` live) and also check the asker's standing (`holder` / `visitor` / `outside`).
- **Auth, runtime, CLI, server & wire callers (`6` sites outside `src-store/`):**
  - `src-auth/softland/auth/schema.clj:84-85, 117, 303-334`: `Auth` stores `UUID`s internally plus `own-lane`, `making-uuid`, and `making-session` (no permission ID column).
  - `src-runtime/softland/runtime/sign_in.clj:89-107, 153-155, 214-288`: `enter!` crash-retries resubmit the personal lane's making act under `making-uuid`; `sign_in.clj:155` writes `{:e who :k :person/name ...}`; `sign_in.clj:214,258` mints random UUIDs for agents/sessions.
  - `src-runtime/softland/runtime/seam.clj:39-40, 124-167, 611-735`: `Caller` holds `root` (person maker) and authenticated caller identity; `seam.clj:39-40` cites 9 Oct Entry 1 (nothing plain in a store act derives from the person's ID).
  - `src-command/softland/command/main.clj:306-316`, `src-server/softland/server/sign_in.clj:164-169`, `src-server-browser/softland/browser/app.cljc`: construct initial/default permission arguments for lane acts.
  - `src-wire/softland/wire/words.clj:157-160`: `:end-agent` currently types `:agent` as `:uuid`.

---

## 3. Raw Mechanical `test-store/` Check-Sentence Dump

All `2,081` check sentences across the `7` proof files (`act_proof.clj`, `door_proof.clj`, `module_proof.clj`, `pure_proof.clj`, `read_path_proof.clj`, `read_path_pure_proof.clj`, `run_again_proof.clj`, `20,857` lines total) were mechanically tokenized with exact `file:line` to:

- `/mnt/data/projects/research/softland/permissions-test-store-scan/check-sentences.txt`

No editorial filtering was applied; use `check-sentences.txt` alongside `test-store/softland/store/*proof.clj` and `BUILD.md` when deriving `ALLOWLIST.md`.
