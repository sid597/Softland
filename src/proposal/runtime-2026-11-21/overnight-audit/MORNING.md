# Morning Brief — Overnight Audit (21–22 November 2026)

Four pages left, zero source files touched:
1. [`../store-piece-3/PLAN_VALIDATION-gates-keep-the-line.md`](../store-piece-3/PLAN_VALIDATION-gates-keep-the-line.md)
2. [`REVIEW-store-piece-2.md`](REVIEW-store-piece-2.md) (probes in [`probes/`](probes/))
3. [`auth-blind/README.md`](auth-blind/README.md) (committed at `d325e482` before opening `runtime-oracle/` or `test-auth/`; post-commit adapter in [`auth-adapters/`](auth-adapters/))
4. This page.

---

## 1. Piece 3 Stage-1 Plan — `VERDICT: major-fail`

Road A's core choice (keeping decided things in `things` rather than reading from the depot) **survives** for Rama 1.6.0 (`streaming-gate` cannot count duplicate strays/resends across post-commit replay; `foreign-depot-read` is foreign-client-only and non-yielding, though Road B falsely claims `microbatch-gate` cannot count partition offsets), and `IMPLICIT_SPEC` S4's trace **holds**. Three heaviest findings blocking build:

1. **Unpermissioned 32 MiB writes + unbounded `:too-heavy` walk + two `weight` bypasses (`Items b, e`):** Any sender with **no permission** in a layer can send 32 MiB maps that `walk/decide` writes to RocksDB (`$$streaming-things`) and 3×-replicates synchronously on a task shared by many layers (~424–600+ ms/send before network replication; ~15–20 sends push unrelated layers past `read_path`'s 10 s `:wait-ms`). Recording exact `:weight` on `:too-heavy` rows (`PLAN:373`) prevents stopping at `bound + 1` on a 250 MiB refused map; Java arrays (`int[]`, `Object[]`, `PLAN:326–328`) weigh ~35 bytes as printed text while carrying hundreds of MiB; `BigDecimal.stripTrailingZeros` (`text.clj:142`) stalls CPU >10 s while weighing `(BigDecimal. (.pow BigInteger/TEN 2000000) 2000000)` as 2-byte `"1M"`.
2. **Redundant full walk on 100% of acts + hidden RocksDB seek counts (`Item e`, `Partitioning`):** Step 2 (`walk.clj:338`) already walks every non-stray thing for `digest/digest` (`text/feed`); running a second full walk for `weight` doubles traversal on every clean act. `PLAN:477–484` reports `"Tasks read"` (`1`) in place of RocksDB seeks, hiding `6+` seeks on normal pages and **$1 + 2k$ random point seeks** (201 seeks at $k = 100$) on `line-by-who` reads.
3. **`line-by-who` forever-write cost + `<who>` Unicode range hazard (`Item c`, `R20`):** Only outside askers (`X`) in `:members`/unmade layers ever read `line-by-who` (visitors in `:everyone` scan `line`). Folding `:by-who` into `$$task` (`:track-size? false`) saves 2 PStates/task, or Road J drops `line-by-who` if `X` names stamps. Hand-off #12 (`PLAN:672`) warns about `U+E000+` in `<layer>` of `line` while missing `<who>` in `"<layer> <who> <stamp>"`.

---

## 2. Store Piece 2 (`34fdab0e^..3544d026`) — What Broke

- **1A. Re-granting `:act-permission` (`SPEC` R5) breaks against `walk.clj` check `a` (run, pure + cluster):** After `pa1` on `u` is ended and an attempt under `u` is refused (`:permission-ended`, writing `answer-key(layer, u)`), re-granting `pa2` on `u` is **accepted** by the gate, yet sending `(assoc x :permission pa2)` is silently dropped at `walk.clj:331`: `door/answer-of` returns `{:answer :not-mine}` and no fact lands, while `gate-histories.reference/judge` returns `:accepted`. (`gate-histories` missed it because `reference.clj` has no `answers` table and `run.clj` never reuses an act UUID.)
- **1B. Any `:see`-only holder (or `:everyone` visitor) can permanently burn a granted `:act-permission` (run, pure + cluster):** `:stores-own-facts true` exposes entity `u` of `:softland.store/act-permission` to every reader; sending a dummy act under `[layer :streaming u]` refuses `:permission-does-not-allow-acting` and writes `answer-key(layer, u)`, locking out the legitimate holder (`{:answer :not-mine}`).
- **1C. `door` + `hex-for-its-permission` mismatch & pre-parse hashing (run):** `hex-for-its-permission(maker-act)` omits `:act-id`/`:depot` (`a3f2ae87...` vs gate's `f0cbfa98...`), yet supplying `:act-id` to `door/send-act` fails `{:could-not :add-its-parts :at :act-id}` (`door.clj` & `door_proof.clj` had 0 lines changed). Two acts with identical `act/parse` form (`5` vs `5N`, vector vs list `:facts`) hash differently.
- **2A. Spurious `{:anew}` leak + push amplification on `:session` (run, pure + cluster):** `sources.clj:580` joins `(chain-sources home layer (row-of run-permission))` without checking `(= asker (nth held 0))`. Granting and ending *another* holder `:dov`'s `:run-7` permission in a `:may-not-see` layer joins `:dov`'s chain (`5 -> 7 -> 5` pushes on cluster, up to `+31`) and fires **2 extra identical `{:anew}` callbacks** to `:ann`, violating `layers.clj:299–302`.
- **2B & 2C. Falsified read bounds (run):** `read-once` with `:session` takes **`[2 [1 1]]` (2 calls)** whenever `:asker` has no layer permission or holds depth-1 root permission (falsifying `"no call more"`, `read_path.clj:1228`, `BUILD:70`). `{:asks [:points-at ...] :session :run-7 :from-layer :start}` takes **8 rounds** on a static store (falsifying `"within six rounds"`, `sources.clj:90`, leaving 1 spare round before `max-rounds = 9`).
- **3 & 4. Unheld spec lines & stale docs (read + run):** R5 self-end fails on `:act-permission` and `:see` `run-permission`; 0 proofs test R6 or R3 on `:act-permission`; `BUILD:98–103` overstates `module_proof`'s base test; 8 files hold stale docstrings/README explanations.

---

## 3. Blind Auth Suite (`d325e482`) & Post-Commit Adapter Runs

- **Suite (`auth-blind/`):** 23 properties named with `spine/auth/SPEC.md` sentences (including `:driven-by` on `open-session`) + 27 single-rule broken variants. Caught **27/27** own mutants and, untouched, **17/20** of `test-auth/reference/breaks` (missing `:bound-code-asks-again`, `:time-of-the-older-secret`, `:ended-sign-in-begins-again`).
- **Unmodified suite vs implementations (`auth-adapters/`):**
  - `spine/runtime-oracle/src/softland/auth/client.clj`: **23/23 passed**.
  - `test-auth/softland/auth/reference.clj`: **22/23 passed, 1 failed** — `:no-call-throws` shrunk to `["missing-auth"]` because `(reference/open valid-client _module-name)` (`reference.clj:906`) ignores `_module-name` and returns a live `Client` instead of `{:could-not :find-the-module}`.
- **9 three-way divergences exposed (`blind-model` vs `runtime-oracle` vs `test-auth/reference`):**
  1. **Omitted `-or-nil` keys (`Probe D`):** `runtime-oracle` & `blind-model` accept omitted `:code-hash`, `:email`, `:label`, `:session`, `:new-agent` as `nil`; `test-auth/reference` rejects them with `:read-the-event`. **Live integration hazard between `runtime-oracle` and `src-auth`!**
  2. **Subsequent *different* answer to an answered ask (`Probe L`):** `runtime-oracle` & `blind-model` return what stands immediately; `test-auth/reference` checks `:over` and `:no-such-actor`/`:ended`/`:in-use` first, so a late second answer returns `{:refused :over}` instead of `{:yes ...}`/`{:no ...}`.
  3. **Ask born over (`ends <= at`) answered by slower clock (`at' < ends`, `Probe K`):** `test-auth/reference` refuses `:over`; `runtime-oracle` & `blind-model` accept `{:yes ...}` (breaking *"taken and is never live"*).
  4. **Extra keys on input maps & `:new-agent` (`Probes C, R`):** `runtime-oracle` silently accepts extra keys (`{:extra 1}`); `blind-model` & `test-auth/reference` reject with `:read-the-event`.
  5. **Non-client handle (`Probe B`):** `runtime-oracle` returns `{:could-not :closed}` for `nil`/`:not-a-client`; `blind-model` & `test-auth/reference` return `{:could-not :read-the-event}`.
  6. **`defrecord` input (`Probe G`):** `test-auth/reference` (`map?`) accepts records; `runtime-oracle` & `blind-model` (`plain-map?`) return `:read-the-event`.
  7. **Uppercase email (`Probe E`):** `test-auth/reference` returns `:read-the-event`; `runtime-oracle` & `blind-model` accept (`SPEC`: *"Auth does not check"*).
  8. **`open` module name (`Probe A`):** `test-auth/reference` ignores `_module-name`; `runtime-oracle` & `blind-model` return `:find-the-module`.
  9. **`answer-ask` `no` with `nil` yes-parts (`Probe M`):** `blind-model` accepted `nil` `:actor`/`:sign-in`/`:until`; both `runtime-oracle` and `test-auth/reference` require UUID/int (`:read-the-event`).

---

## 4. What Was Run, With Its Output

- `clojure -M:store -m softland.store.act-proof` -> `166 checks, 0 failed`
- `clojure -M:store -m softland.store.pure-proof` -> `724 checks, 0 failed`
- `clojure -M:store -m softland.store.read-path-pure-proof` -> `223 checks, 0 failed`
- `clojure -M:store -i src/proposal/runtime-2026-11-21/overnight-audit/probes/break_piece_2.clj` -> Probes `1A` (`:accepted` in ref vs check-`a` drop), `1B` (`:see` burns `u`), `2` (`a3f2ae87...` vs `f0cbfa98...`), `3` (`7` rounds), `4` (`+2` spurious `:anew`), `5` (`16,965` callback-order states, `0` wrong screens), `6A` (`[2 [1 1]]`), `6B` (`8` rounds), `6C` (self-end refusals), `6D` (`5` vs `5N` digest `false`)
- `flock .../cluster.lock clojure -M:store-module -i .../probes/cluster_probe_piece_2.clj -e "(cluster-probe-piece-2/-main)"` -> Cluster Receipts `1`–`4` (`{:answer :not-mine}` on re-grant & `:see` griefing; `3` `:anew` callbacks & `5 -> 7 -> 5` pushes on `:dov` grant/end)
- `cd .../overnight-audit/auth-blind && clojure -M:run` -> `23/23 properties passed`, `27/27 broken variants caught` (exit `0`)
- `cd .../overnight-audit/auth-adapters && clojure -M:run` -> `runtime-oracle`: `23/23`; `test-auth/reference`: `22/23` (`:no-call-throws` failed on `["missing-auth"]`); `10/18` 3-way probes diverged (`A,B,C,D,E,G,K,L,M,R`); `17/20` `reference/breaks` caught

---

## 5. What Is Open

1. **Piece 2 `:act-permission` identity (`1A, 1B`):** Keying `:act-permission` and `$$answers` by the same caller-chosen `act-uuid` `u` makes `u` single-shot across refusals and lets any `:see` reader burn `u`. Needs either a grant-id separate from `act-uuid` or keying `$$answers` so non-holder/pre-regrant refusals cannot poison `pa`.
2. **Piece 2 `door` helper (`1C`):** How a caller computes `hex-for-its-permission` from `(maker-act, act-id)` without assembling envelope keys that `door/send-act` rejects.
3. **Auth alignment (`Probes D, K, L, C, B`):** `runtime-oracle/client.clj` and `test-auth/reference.clj` disagree today on omitted `-or-nil` keys, late/invalid second answers to answered asks, asks born with `ends <= at`, extra map keys, and non-client handles.

---

## 6. Every Choice of Ours That Is Yours to Turn

1. **Piece 3 refused-thing weight bound (`PLAN_VALIDATION` Item `b`):** Default taken: **Option 2** — `32 MiB` for taken acts and standing-member refusals (`places` faults + `:permission-does-not-allow-acting`), **`256 KiB`** for non-standing and declaration refusals, stopping `weight` at `bound + 1` (dropping exact `:weight` from `:too-heavy` rows). *Turn to:* Option 1 (`32 MiB` for all), Option 3 (`256 KiB` for all refusals), or another byte bound.
2. **Piece 3 `line-by-who` index (`PLAN_VALIDATION` Item `c`):** Default taken: keep `:by-who` inside `$$task` (`:track-size? false`, saving 2 PStates/task) so outside askers `X` keep gap-free discovery without knowing stamps. *Turn to:* Road J (drop `:by-who` altogether; `X` names stamps from door answers / `Trails`).
3. **Blind Auth suite (`auth-blind` choices where `SPEC.md` was silent):**
   - **Omitted `-or-nil` keys (`Probe D`):** Treated omitted `:email`/`:code-hash`/`:label`/`:session`/`:new-agent` as `nil` (matching `runtime-oracle`; `test-auth/reference` requires explicit keys).
   - **Second answer on decided ask (`Probe L`):** Returned what stands before `:over`/actor refusals (matching `runtime-oracle`; `test-auth/reference` refuses first).
   - **`answer-ask` `no` unused fields (`Probe M`):** Allowed `nil` `:actor`/`:sign-in`/`:until` on `no` (both `runtime-oracle` and `test-auth/reference` require UUID/int).
   - **Plain maps & un-checked email case (`Probes E, G`):** Rejected `defrecord`s and extra keys; accepted any string $\le 256$ chars for email as given.
