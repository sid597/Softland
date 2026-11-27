status:     DONE
commit:     recorded in git log on main (this commit)
verified:   all 8 test-store proofs green (permission-proof 71, act-proof 164, pure-proof 780, read-path-pure-proof 283, door-proof 283, module-proof 510, read-path-proof 741, run-again-proof 48 after 10th making); gate-histories (2000 1) green; test-wire (38), test-auth (191+3), test-server (pure+seam+http), src-server-browser load, and all test-runtime proofs + 7 attacks 100% green; read-measures & gate-measures small green; 102/102 refusal keywords hit (0 zero-hits)
next:       Loop 5 (fresh uncontaminated Breaker subagent on InProcessCluster behind cluster.lock, draft src-store/README.md Decisions entry in state/loop-5.md, update FIRE.edn)

# Loop 4: Callers, Store Proof Migration, Measures, `run_again_fixture` (10th Making), and Whole-Store Proofs

## 1. Summary of Changes (`git diff --stat`)

Zero changes were needed in `src-store/` or `src/proposal/runtime-2026-11-21/gate-histories/` during Loop 4; Ring 1 and Ring 2 held cleanly across every migrated caller and proof.

```text
 src-command/softland/command/main.clj              |   55 +-
 src-runtime/softland/runtime/seam.clj              |   36 +-
 src-runtime/softland/runtime/sign_in.clj           |   97 +-
 src-server/softland/server/sign_in.clj             |   27 +-
 test-runtime/softland/runtime/attacks/lost_replacement.clj |    4 +-
 test-runtime/softland/runtime/checks.clj           |   12 +-
 test-runtime/softland/runtime/command_proof.clj    |   13 +-
 test-runtime/softland/runtime/command_review_proof.clj |    7 +-
 test-runtime/softland/runtime/line_review_proof.clj |    4 +-
 test-runtime/softland/runtime/permissions.clj      |  155 +-
 test-runtime/softland/runtime/review_faults_proof.clj |    2 +-
 test-runtime/softland/runtime/two_lines_proof.clj  |   13 +-
 test-server/softland/server/http_proof.clj         |    4 +-
 test-server/softland/server/pure_proof.clj         |    2 +-
 test-server/softland/server/seam_proof.clj         |   10 +-
 test-store/softland/store/act_proof.clj            |  206 +-
 test-store/softland/store/door_proof.clj           |   20 +-
 test-store/softland/store/gate_measures.clj        |   51 +-
 test-store/softland/store/line_statements.clj      |    6 +-
 test-store/softland/store/module_proof.clj         | 1458 ++++++---
 test-store/softland/store/permissions.clj          |  275 +-
 test-store/softland/store/pure_proof.clj           | 1792 ++++++-----
 test-store/softland/store/read_measures.clj        |   66 +-
 test-store/softland/store/read_path_proof.clj      | 1155 +++++--
 test-store/softland/store/read_path_pure_proof.clj | 2106 +++++++-----
 test-store/softland/store/run_again_fixture/README.md |   39 +-
 test-store/softland/store/run_again_fixture/acts.edn |  798 ++---
 test-store/softland/store/run_again_fixture/tables.edn | 3353 ++++++++++----------
 test-store/softland/store/run_again_proof.clj      |   89 +-
 test-wire/softland/wire/pure_proof.clj             |    4 +-
 30 files changed, 6964 insertions(+), 4895 deletions(-)
```

- **Helpers flipped (`CONTRACT.md` §2.1–2.3):** `test-store/softland/store/permissions.clj` and `test-runtime/softland/runtime/permissions.clj` now mint `:person.<id>`, `:agent.<id>`, and `:run.<id>` party keywords (leaving Auth plain UUIDs untouched), UUID-keyed `:softland.store/permission` maps (`:holder`, `:allows`, `:covers`, `:given-by`, optional `:session`, `:under`, `:lent-from`), deterministic v8 UUIDs (`first-act/first-permission-id`, `first-act/first-anyone-id`), and `:permission`-less making acts (with optional `{:open? true}` for `:anyone` `:see` in the log).
- **Runtime, server, command, and browser callers updated:**
  - `src-runtime/softland/runtime/seam.clj`: translates Auth actor kinds (`:person` / `:agent`) to `:person.<uuid>` / `:agent.<uuid>` at the store boundary (`party-of`), strips `:permission` on making acts only when missing (`dissoc` when absent, preserving UUID `:permission` on regular acts), and formats `[:exact-act digest]` yes facts on UUID entities under `:softland.store/permission` lent from `(first-act/first-permission-id own-lane)` for agents and standing for people.
  - `src-runtime/softland/runtime/sign_in.clj` & `src-server/softland/server/sign_in.clj`: make `:base` with `{:open? true}` (`first-act/first-permission-id :base` + `first-act/first-anyone-id :base` in `:base`'s making act) and personal lanes with `(first-act/first-permission-id lane)` held by `:person.<uuid>` (`:given-by :system`).
  - `src-command/softland/command/main.clj`: passes UUID `:permission` (defaulting to `(first-act/first-permission-id own-lane)` for a person in her own lane) and builds UUID-keyed `:softland.store/permission` grant facts for `--allow`.
  - `src-server-browser/softland/browser/app.cljc`: verified compiling cleanly (`clojure -M:server-browser -e "(require 'softland.browser.app) :ok"` — reads server-supplied `:lane` and `:permission` UUID from session state).

## 2. Proofs Run & Results

| Suite | Command | Result |
|---|---|---|
| `store.permission-proof` | `clojure -M:store -m softland.store.permission-proof` | `{:test 14, :pass 71, :fail 0, :error 0}` |
| `store.act-proof` | `clojure -M:store -m softland.store.act-proof` | `{:test 8, :pass 164, :fail 0, :error 0}` |
| `store.pure-proof` | `clojure -M:store -m softland.store.pure-proof` | `{:test 21, :pass 780, :fail 0, :error 0}` |
| `store.read-path-pure-proof` | `clojure -M:store -m softland.store.read-path-pure-proof` | `{:test 21, :pass 283, :fail 0, :error 0}` |
| `store.door-proof` | `flock cluster.lock clojure -M:store-module -m softland.store.door-proof` | `{:test 25, :pass 283, :fail 0, :error 0}` |
| `store.module-proof` | `flock cluster.lock clojure -M:store-module -m softland.store.module-proof` | `{:test 49, :pass 510, :fail 0, :error 0}` |
| `store.read-path-proof` | `flock cluster.lock clojure -M:store-module -m softland.store.read-path-proof` | `{:test 37, :pass 741, :fail 0, :error 0}` |
| `store.run-again-proof` | Pre-regen Checks 1 & 4 (`0` differing rows across all 7 tables) $\to$ `make-fixture` (10th making) $\to$ full suite (4 & 8 tasks) | `{:test 5, :pass 48, :fail 0, :error 0}` |
| `gate-histories` | `(cd src/proposal/runtime-2026-11-21/gate-histories && clojure -M:run 2000 1)` | `100% GREEN` (`rules`, `mutants`, `fix`, `fix-mutants`, `live_read`) |
| `wire.pure-proof` | `clojure -M:wire -m softland.wire.pure-proof` | `38 passed, 0 failed` |
| `auth` | `reference-proof` + `module-check 1 10` | `191 passed, 0 failed` + `3 passed, 0 failed` |
| `server` | `pure-proof`, `seam-proof`, `http-proof` | `100% GREEN` (`43/43` seam + pure + http) |
| `runtime` proofs | `pure-proof`, `command-pure-proof`, `proof`, `command-proof`, `command-review-proof`, `line-review-proof`, `open-session-proof`, `two-lines-proof`, `review-faults-proof` | `100% GREEN` |
| `runtime.attacks/*` | `slow-readers`, `yes-page`, `closed-session`, `unwatched-standing`, `lost-replacement`, `sign-in-pace`, `watch-caps` | `7/7 GREEN` (`0` failed) |

### New `BUILD.md` Batches 1–4 Cluster & Whole-Store Proofs Added
- **Batch 1–4 cluster gate & `:base` replication (`module_proof.clj`):** `permissions-of-the-four-batches` (`:streaming`) and `permissions-of-the-four-batches-in-the-base` (`:base` microbatch across all 4 tasks), covering multiple gives per holder, never-given vs ended vs used-up, see-only self-end, standing person vs lent agent/run, read-only holder lending `:see` to an agent, 4-enders rule + lowered-giver revocation guard, multi-change atomic acts, owner hand-over, one owner ending another's give while an owner remains, closed-by-default `:base`, `:anyone` / `:anyone-signed-in`, and Option B `:everyone` in `:give`.
- **Live viewer push count (`read_path_proof.clj`, `the-pushes-a-live-viewer-holds`, `BUILD.md:79-80`):** Measured and asserted on an isolated read path.
- **1-act non-flicker transfer (`read_path_proof.clj`, `a-transfer-in-one-act-never-shows-a-state-in-between`, `BUILD.md:169`):** Proved 0 intermediate `:may-not-see` / empty emissions across 1-act person swaps, lent-agent swaps, and whole-lane owner hand-over on `:streaming`, AND on `:base` (`:microbatch`) read both directly on `:base`'s task (`2`) and cross-task via `:viewer-home-0` on task `3` (`!= 2`).
- **Statement to prove (`read_path_proof.clj`, `the-statement-to-prove-with-its-named-exceptions`, `BUILD.md:167-168`, `PERMISSIONS.md` §12):** Proved Clauses 1–3 of `PERMISSIONS.md` §12 with its two named exceptions (`:softland.store/who-may-read-acts` for refused acts under `:members`/`:everyone`, and open reads of the store's own facts under lane `:see` rules) across 27 decisions in `:the-statement` and 60 decisions in `:base`.

## 3. Measured Live-Viewer Push Counts & `read-measures` / `gate-measures`

### Live Viewer Push Counts (`BUILD.md:79-80`, `read-path/open-count`)
- **Open lane (`:anyone` holds `:see`, 1 place read):** **`4` pushes** (`3` on `$$facts`: `own-rows`, `:anyone` permission row, fact place; `1` on `$$permissions-by-holder`: `:anyone` `*` range). Additional viewers on the same open lane + place add **`0` pushes** (`{:live-reads 2, :pushes 4}`), because `:anyone` short-circuits before `:anyone-signed-in` and the viewer's own holder run.
- **Private lane, lent agent (`:agent.bot` lent 1 hop from owner, `:session nil`, 1 place read):** **`5` pushes** (`4` on `$$facts`: `own-rows`, agent's borrow row, lender's row, fact place; `1` on `$$permissions-by-holder`: `:agent.bot` `*` range — agents never watch `:anyone` or `:anyone-signed-in`).
- **Private lane, standing person (`:person.bob` with `:lent-from nil`, `:session nil`, 1 place read):** **`6` pushes** (`3` on `$$facts`: `own-rows`, `:person.bob`'s permission row, fact place; `3` on `$$permissions-by-holder`: `:anyone`, `:anyone-signed-in`, `:person.bob`).
- **Private lane, 1-hop lent person (`read-measures` `M2`, `:person.asker` lent from `maker`, `:session nil`, `n` places/runs):** **`n + 6` pushes** (`1` read $\to$ `7` pushes; `10` reads $\to$ `16` pushes; `50` reads $\to$ `56` pushes).
- **After `(read-path/leave ...)`:** `{:live-reads 0, :pushes 0}`.

### `read-measures small` & `gate-measures small` Highlights
- **`M1` / `D1` streaming gate (100 acts/s, 100-byte fact):** call-to-return median `7.308 ms` (`M1`) / `7.117 ms` (`D1`) unmarked, `6.61 ms` (`M1`) / `6.459 ms` (`D1`) marked; `:base` (`200 ms` wait) median `206.966 ms` (`M1`) / `206.228 ms` (`D1`), `:base` (`5 ms` wait) median `194.58 ms` (`0` unaccepted acts).
- **`M4` live push latency from `send-act` return:** `:streaming` `:facts` median `0.137 ms`, `:streaming` `:facts-by-value` median `0.167 ms`, `:base` direct median `98.393 ms`, `:base` cross-task via home lane median `96.486 ms`.
- **`D2` duplicate `act-id` replay:** first send median `4.969 ms` vs second send `1.797 ms`.
- **`D3` throughput:** Rama default held all `[200 400 800 1600]` acts/s within 1 s (`163.5` acts/s sequential one-by-one); `StoreAtOne` (`max.executing.per.task = 1`) held `[200]` of `[200 400 800 1600]` (`85.9` acts/s sequential).

## 4. Check-Sentence & Refusal-Keyword Audit

- **Refusal-keyword parity:** `102/102` active reason/word keywords across `act.clj`, `door/declaration.clj`, `door/words.clj`, `gate/decision.clj`, `read_path/query.clj`, and `read_path/words.clj` hit across `test-store/*proof.clj` (**`0` zero-hit keywords**).
- **Check-sentence audit against `check-sentences.txt` (`2,081` baseline sentences) and `ALLOWLIST.md` (`405` listed):**
  - `act_proof.clj`, `door_proof.clj`, `module_proof.clj`, `read_path_proof.clj`, `run_again_proof.clj`: **`0` unlisted changes**.
  - **`3` purely mechanical count / byte-size sentences missed by `ALLOWLIST.md` (recorded for morning review; zero behavioral rules altered):**
    1. `test-store/softland/store/pure_proof.clj:1171` (baseline `pure_proof.clj:1120`) — missed `B1 rows` byte-sum sibling of allowlisted lines `600`, `1023`, `1027` (an act of 256 max-size 65,536-byte values under a 256-char `:who` and a 36-char UUID `:permission` writes `256 * (65,536 + 256 + 36) = 16,851,968` bytes instead of `18,874,368` with the old 24,735-byte 32-deep vector; `18,874,368` / `18,874,369` `:act-too-large` edge remains tested directly in `check-bound-cases` and `refusals-of-the-declaration`):
       - **BEFORE:** `"an act whose accept would write 18,874,368 bytes of text into the rows of its facts"`
       - **AFTER:** `"an act of 256 full values whose accept would write 16,851,968 bytes of text into the rows of its facts"`
    2. `test-store/softland/store/read_path_pure_proof.clj:2688` (baseline `read_path_pure_proof.clj:2283`) — word count in `deftest the-words` after Loop 0d/Loop 3 added `:too-many-permissions` to `read_path/words.clj`:
       - **BEFORE:** `"the read path's own ten words"`
       - **AFTER:** `"the read path's own eleven words"`
    3. `test-store/softland/store/read_path_pure_proof.clj:2708` (baseline `read_path_pure_proof.clj:2299`) — live-end reason count in `deftest the-words` after `:too-many-permissions` joined `words/because`:
       - **BEFORE:** `"what a word can carry is declared, and so are the six reasons for which a live read ends and the two words for a lane that is left out"`
       - **AFTER:** `"what a word can carry is declared, and so are the seven reasons for which a live read ends and the two words for a lane that is left out"`

## 5. Explicit Notes Required by `BUILD.md`

### A. One Owner Ending Another Owner's Give While an Owner Remains (`BUILD.md:149-150`)
Per `PERMISSIONS.md` §6 items 18 ("whoever holds everything over what it covers") and 21 ("several owners each act alone; a lane must always have at least one owner"), the store lets one standing owner end or lower another owner's permission — including the lane's `first-permission` — so long as at least one standing-on-its-own owner permission remains on the lane after the act (`permission_proof.clj:537`, `module_proof.clj`, `read_path_proof.clj`). As `BUILD.md:149-150` notes, Sid was not shown this combined owner-over-owner case directly (`PERMISSIONS.md` §12 open question 4 can turn it if desired).

### B. What the Code Showed That `PERMISSIONS.md` §12 Had Wrong (`BUILD.md:52-53`)
1. **Point 1 (give ID derived from `[act-id, index]`):** In this store a fact sits on an entity `:e` (`:id`, keyword or UUID), not on a tuple `[act-id, index]`. As amended on 27 November, the giver's program mints a UUID for `:e` (matching exact-act yeses), while a lane's initial owner give and initial `:anyone` give use deterministic v8 UUIDs derived from the lane ID (`first-act/first-permission-id`, `first-act/first-anyone-id`) so replay and callers agree without coordination.
2. **Point 2 & Item 18 (`:given-by` ending/lowering after in-place lower, and holder-index stamp on change):** With in-place `change` keeping `:given-by` and `:under`, a giver lowered from member/owner to `:see` would otherwise retain the power to end or lower writers/owners she granted earlier; `ends-with-no-chain?` therefore re-checks `may-pass?` against what her `:under` give currently allows (`PERMISSIONS.md` §6 item 26). Likewise, an in-place `change` updates the permission's stamp in `$$*-permissions-by-holder` so live viewers watching a holder's run are notified when an existing give is raised or lowered (`Loop 0d` Invariant 1).
3. **Point 12 (`:give` vs publishing to `:anyone`):** Inviting a named person (`:give #{:see ...}`) and opening a lane to `:anyone` / `:anyone-signed-in` require distinct authority under `PERMISSIONS.md` §6 item 26 (`Option B`): granting or raising for `:anyone` or `:anyone-signed-in` requires `:everyone` inside `:give` (included in `owner-allows`).
4. **Point 16 / Implementer note ("That a live viewer then holds more pushes is not traced"):** Traced end-to-end and measured above (`4` shared pushes on an open `:anyone` lane, `5` for a 1-hop lent agent, `6` for a standing private member), with live standing gated on entry-for-entry `read-set` stamp equality so 1-act transfers never flicker even under arbitrary multi-proxy callback skew.
