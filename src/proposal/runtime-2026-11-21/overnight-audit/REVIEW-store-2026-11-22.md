# Adversarial Review — Store (`2026-11-22`)

## Verdict

- **Step 4 (`1a2e9811..d3290f37` — `27d883a3` + `d3290f37`): FAIL (P1 read-path confidentiality & unseen-layer breach in `27d883a3`; P2 refusal-word precedence & forged-yes misclassification in `d3290f37`; three P3 boundary/wedge defects).**
  - The yes-with-own-id gate rules themselves (`d3290f37`) hold across `100,000` `gate-histories` (`20,000` × seeds `1..5`, `0` random breaks, `12/12` mutants caught), across `:base` (`:microbatch`) 4-task replication at the `771`-write maximum, and across `Store` module restarts.
  - However, `27d883a3`'s headline guarantee (*"an unseen layer tells nothing"*) fails when an asker already holds an ended-above permission chain through a giver who receives a fresh permission and then atomically gives beneath it + ends her own permission in one act: per-row `ProxyState` delivery pairs the asker's new row with the giver's stale row, flipping `standing` transiently to `:yes` and **handing confidential facts from the unseen layer directly to an asker who was `:may-not-see` at every stamp** (reproduced in `gate-histories.run/play` itself, in `read_path_pure_proof`'s `every-order`, and naturally on a 4-task Rama `InProcessCluster` with zero mocks).
- **Step 5 (`Store: a layer's acts and refusals, kept and read by stamp`): PENDING BUILDER COMMIT (Stage 1 `7f1242f3` landed at `15:10`; Stage 2 `read_path/acts.clj` left uncommitted in the working tree since `15:11`).**
  - Preliminary review of committed Stage 1 (`7f1242f3`, read-only) already identified a **256 KiB refusal-cap bypass in `digest/weighed`** (`src-store/softland/store/door/digest.clj:527–531`) where non-`int?` `:door-time` values (such as a `200 KB` `byte[]` or `[Long ...]` vector) are weighed by `weight/weight-of` instead of `text/feed`, undercounting `one-text` UTF-8 bytes by `2x–2.5x` so a stranger's declaration-refused thing up to `~600 KiB` of `one-text` is kept whole in `$$things`, plus double-counting of `BigDecimal` / `#other` / `#too-deep` in `text/feed*` (`text.clj:674–675`) and the $O(n^2)$ `BigDecimal.stripTrailingZeros` task-thread stall (`8.2 s` at `83 KB`, scaling quadratically on strays and refusals alike). Full Step 5 statement runs, heavy-refusal task-sharing measurements, and read-seek counts will run as soon as `Store: a layer's acts and refusals, kept and read by stamp` is committed.

---

## Step 4 — Store's Repair and Yes (`1a2e9811..d3290f37`)

Scope: `git diff 1a2e9811..d3290f37 -- src-store test-store`, covering:
- `27d883a3` (`Store's read path: an unseen layer tells nothing, and the rounds are derived`),
- `d3290f37` (`Store: a yes for one act has an id of its own, and is used up`),
weighed against [`BUILD-store-piece-2-repair.md`](../BUILD-store-piece-2-repair.md), [`store-piece-2/SPEC.md`](../store-piece-2/SPEC.md), and `gate-histories` (`e9bc5ac1`).

Probes written for Step 4:
- Pure probe: [`overnight-audit/probes/break_piece_2_yes.clj`](probes/break_piece_2_yes.clj)
- 4-task `InProcessCluster` probe (run behind `cluster.lock`): [`overnight-audit/probes/cluster_probe_piece_2_yes.clj`](probes/cluster_probe_piece_2_yes.clj)

### 4.1 What `27d883a3` + `d3290f37` Repaired From `REVIEW-store-piece-2.md`

Every defect of the old act-permission form (`1A`, `1B`, `1C`, `3.1`, `3.2`, `3.3`, `3.5`), the read-path cost/round claims (`2A`, `2B`, `2C`), and the stale docstrings (`4.1`–`4.8`) in [`REVIEW-store-piece-2.md`](REVIEW-store-piece-2.md) was addressed:
- **Yes on its own UUID, normalized maker digest, single-use write:** A yes `[who layer granted-in parent :see-and-act [:act yes hex]]` sits on `yes` under `:softland.store/act-permission`; `digest/hex-for-a-yes` hashes `act/parse`'s normalized maker form without `:act-id`, `:depot`, `:door-time`, or `:permission`; `decision/use-of-its-yes` writes an end naming `yes` at the yes's place on accept; `store-key-fault` refuses any second give at a yes's place with `:a-yes-is-given-once`; outsiders can neither burn an act-id nor write/use a yes (`T2`).
- **Non-holder `run-permission` chain filter (`sources.clj:201–213, 632–633`):** `askers-run-row` stops joining chain pushes when `run-row` names another holder, fixing `REVIEW-store-piece-2.md` `2A` and `2B`'s `:dov` round-trip.
- **Derived round bounds (`sources.clj:83–120`, `read_path.clj:344–356`):** `max-rounds` is `{:once 6 :live 16}`, derived from the source dependency DAG (up to 8 static rounds for `:live`, 6 for `:once`).

### 4.2 Faults Found in `1a2e9811..d3290f37`

#### P1 — Confidential data leaked to a `:may-not-see` asker via phantom standing chains across stale per-row pushes (`27d883a3`)

- **Evidence:**
  - `src-store/softland/store/read_path/answer.clj:508–526, 612–626` (`unseen-layers`, `after-change`)
  - `src-store/softland/store/read_path/sources.clj:592–595, 629–635` (`reading`)
  - `src-store/softland/store/gate/decision.clj:588–626` (`places`, allowing atomic give-beneath-own + end-own in either fact order)
  - `test-store/softland/store/read_path_pure_proof.clj:979–1014` (`own-unseen-world`)
- **Why it breaks:**
  A live read subscribes to each row of an asker's permission chain via separate per-key `ProxyState`s (`permission-source`, `run-permission-source`, `chain-sources`), which have no cross-key snapshot isolation in Rama (`rama-pitfalls` #1 / #8).
  In `27d883a3`, `unseen-layers` (`answer.clj:508–526`) suppresses updates only when `(get-in reading [:standing layer])` is `:may-not-see` or `:not-known-yet`. `own-unseen-world` (`read_path_pure_proof.clj:979`) tested `:bea` ending her own permission and giving `:ann` one beneath it in the same act **only** when `:ann` initially held `nil` (so `:bea` was not in `held`, landing in `:not-known-yet` -> `waiting`).
  When `:ann` **already** holds an ended-above permission chain through `:bea` (`:root -> :dave -> :bea -> :ann`, with `:dave` ended), `:ann` is `:may-not-see` and her live read **already holds open pushes** on `:ann`, `:bea`, `:dave`, and `:root`:
  1. **After `:ann` lost standing**, `:root` writes confidential `{:e :rx :k :dose :v "TOP-SECRET-99mg"}` in `:secret` (`:ann` never had standing for this value at any stamp).
  2. `:root` gives `:bea` a fresh `bea-p2` beneath `:root`. `:bea`'s open push updates to `bea-p2` (stamp $s_1$); `:ann` still holds `ann-p1` (parent `bea-p1`), so `:ann`'s standing remains `:may-not-see`.
  3. `:bea` sends **one atomic act** giving `:ann` `ann-p2` beneath `bea-p2` and ending `bea-p2` (accepted by `decision/places` in either fact order). At **every** stamp before and after this act, `ann-p2` is dead from birth (`bea-p2` ended at the same stamp $s_2$), so `:ann` is **`:may-not-see` at every stamp in the store's history**.
  4. When `:ann`'s `ProxyState` callback runs before `:bea`'s `ProxyState` callback (which happens **naturally without any fault injection** on Rama when `:ann`'s fact precedes `:bea`'s fact in `:facts`, or whenever the holder's key sorts before the giver's key as in `:dee` < `:eve`):
     - `known` holds `:ann = ann-p2` (stamp $s_2$), `:bea = bea-p2` (**stale stamp $s_1 < s_2$**), `:root = root-p` (stamp $s_0 < s_1$).
     - `chain/stands?` checks $s_0 < s_1 < s_2$ -> **`true`**.
     - `standing(:secret)` flips transiently to **`:yes`**, bypassing `unseen-layers`.
  5. **Two concrete consequences — both proven in pure code and on a live 4-task Rama `InProcessCluster`:**
     - **(a) Direct confidentiality leak (`"TOP-SECRET-99mg"` handed to `:ann`):** Because a runtime copy shares one `ReadPath` across all callers (`read_path.clj:1143`), whenever another reader (such as `:root`) holds a push open on `:rx :dose` (or whenever `on-arrival` completes before `:bea`'s `EmptyPlace` diff arrives), `settle!` immediately populates `known` with `:secret`'s confidential row and `after-change` calls `:ann`'s callback with **`{:anew {:layers [:secret :pub], :left-out [], :place {:value "TOP-SECRET-99mg", :layer :secret}}}`** — exposing a secret `:ann` was never authorized to see at any stamp.
     - **(b) Spurious `{:anew}` leak even on an isolated `ReadPath`:** Even when no other reader holds `:rx :dose` open and `:bea`'s `EmptyPlace` diff arrives before `:rx :dose` finishes opening, `:ann`'s diff already opened an unauthorized `ProxyState` on `:secret`'s fact and overwrote `:owed?` to `true`, so `:bea`'s diff delivers a duplicate `{:anew ...}` callback — falsifying `27d883a3`'s guarantee (*"an unseen layer tells nothing"*).
     - Exact same bug triggers via **`:run-permission`** (`:end-and-give-run` when `:run-7` previously held `:ann`'s ended-above run-permission through `:bea`).
- **Proven in three independent runners:**
  1. **`gate-histories.run/play` itself (`break_piece_2_yes.clj` Probe 1A):** A 7-step history in `gate-histories.run`'s own `gen-op` grammar (`:asha -> :ben -> :eve -> :dee`, end `:ben`, write confidential `[:thing-1 :colour] = "said at 10"`, give `:eve` beneath `:asha`, `:eve` atomically ends `:eve` + gives `:dee`) breaks `gate-histories`'s own `:unseen` invariant (`BROKEN at step 7 - unseen`), handing `:dee` `{:anew {:layers [:lab], :left-out [], :place {:holds :value, :layer :lab, ..., :stamp 5, :value "said at 10"}}}`! (`20,000 × 5` random runs missed it only because the joint generator probability of that 4-deep chain + end + re-grant + 2-fact `:eve -> :dee` act is $\approx 10^{-6}$ per history.)
  2. **`read_path_pure_proof`'s `every-order` (`break_piece_2_yes.clj` Probe 1B):** Explores all 16 callback states for both `:permission` and `:run-permission`; delivers `5` tells (expected `0`), leaking both `"TOP-SECRET-99mg"` and `"RUN-SECRET-42mg"`.
  3. **Live 4-task Rama `InProcessCluster` with zero `with-redefs` (`cluster_probe_piece_2_yes.clj` Receipt 1):**
     - Shared `ReadPath`: `:ann` receives `3` callbacks (expected `1`), with `callback 1 -> value="TOP-SECRET-99mg" left-out=[]`.
     - Isolated `ReadPath`: `:ann` receives `2` callbacks (expected `1`).

#### P2 — Refusal-word precedence & forged-yes misclassification in `decision/standing` vs `SPEC.md` R9 and `gate-histories/reference.clj`

- **Evidence:**
  - `src-store/softland/store/gate/decision.clj:155–165, 237–256, 320–345` (`used-up?`, `standing`)
  - `src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md:175–178` (R9)
  - `src/proposal/runtime-2026-11-21/gate-histories/src/gate_histories/reference.clj:311–321` (`judge`)
  - `src/proposal/runtime-2026-11-21/gate-histories/src/gate_histories/run.clj:759–765` (`agree-violation`)
- **Why it breaks:**
  `SPEC.md` R9 states unconditionally: *"A thing that names a permission for another run than its own is refused, whatever else is right in it; and so is a thing that names a yes for another act, an act whose text is not the yes's digest, whatever its act-id."* `gate-histories/reference.clj:313–320` checks `:permission-for-another-run` and `:act-not-the-one-permitted` **before** checking whether the permission stands or whether the yes is used up.
  In `decision.clj:321–345`, `standing` checks `:permission-for-another-run` (`:326`) and `:act-not-the-one-permitted` (`:329`) **only** inside `(when (record/holds-value? permission-row permission) ...)`. Once a yes is **used up** or **ended** (or a `run-permission` is **ended**), `permission-row` holds no `:value` (`EmptyPlace`), so execution jumps to lines `339–344`:
  1. **Wrong-digest act under a used yes (`Probe 2a`, `Cluster Receipt 4`):** `SPEC`/`reference` -> `:act-not-the-one-permitted`; `gate` -> `:permission-used-up`.
  2. **Wrong-digest act under an ended yes (`Probe 2b`):** `SPEC`/`reference` -> `:act-not-the-one-permitted`; `gate` -> `:permission-ended`.
  3. **Wrong-session act (`:run-2`) under an ended `:run-1` permission (`Probe 2c`):** `SPEC`/`reference` -> `:permission-for-another-run`; `gate` -> `:permission-ended`.
  4. **Forged, never-given yes on a *used* yes's UUID (`Probe 2d/2e`, `Cluster Receipt 4`):** After `pa1` on `y1` is **used up** (never ended!), if `:ben` sends an act naming a forged, never-granted permission `pa-forged = [:ben layer layer cal-p :see-and-act [:act y1 hex]]` on `y1`, `(used-up? permission-row pa-forged)` (`decision.clj:342`) is `false` (`(:permission row)` is `(text pa1) != (text pa-forged)`), and line `343` `(record/ended? permission-row)` is **`true`** (because `use-of-its-yes` wrote an `EmptyPlace`), so the gate tells `:ben` **`:permission-ended`** for a never-given permission on a *used-up* yes.
  - **Why `gate-histories` missed it:** `agree-violation` (`run.clj:764–765`) checks only boolean `(= taken? (boolean (:accept? ruled)))`, never comparing the gate's refusal reason with `(:why ruled)`.

#### P3 — `digest/hex-for-a-yes` skips `declaration/check` rules & bounds, allowing permanently unredeemable standing `yes` grants in `$$facts`

- **Evidence:** `src-store/softland/store/door/digest.clj:122–181` vs `src-store/softland/store/door/declaration.clj:74–136, 557–654, 696–746`.
- **Why it breaks:**
  `act-for-a-yes` validates only `act.clj`'s structural `:act` shape (minus `:permission`) and runs none of `declaration/check`'s rules or size bounds. As proven in `Probe 4` and `Cluster Receipt 4`, `hex-for-a-yes` hashes a maker's act with `257` facts (`> max-facts = 256`), a `70 KiB` plain value (`> max-value-bytes = 65,536`), duplicate `[:e :k]` places, unknown `:softland.store/*` keys, or `260`-char keyword IDs; the giver's grant `{:e y :k :softland.store/act-permission :v pa}` is **`:accepted`** into `$$facts`, creating a standing `yes` that the holder can never redeem.

#### P3 — `use-of-its-yes` writes an out-of-bounds `:fact` index (`[[layer depot uuid] count(facts)]`, up to `256`) exposed by public reads

- **Evidence:** `src-store/softland/store/gate/decision.clj:729–762` (`use-of-its-yes`) and `src-store/softland/store/read_path/answer.clj:53, 133–152` (`fact-id`, `place-answer`).
- **Why it breaks:**
  When an act `A` with `k` facts (`0..k-1`) is accepted under `yes`, `use-of-its-yes` writes an `EmptyPlace` row at `(layer, yes, :softland.store/act-permission)` with `:by-act = act-uuid` and **`:by-index = k`**. Querying `read-once` or `read-live` with `:stores-own-facts true` (proven in `Probe 5` and on `:base` in `Cluster Receipt 2` for a `256`-fact `771`-write `yes`) returns `{:holds :nothing-here, :layer :base, :fact [[:base :microbatch act-uuid] 256], ...}`, whose `:fact` index `256` corresponds to no element of `(:facts A)` (`0..255`).

#### P3 — `read_path/settle!` hitting `(:live max-rounds)` (`16`) drops unjoined sources without calling `end-read!`, permanently wedging the live read

- **Evidence:** `src-store/softland/store/read_path.clj:952–968` (`settle!`) vs `1362–1364` (`read-live*`).
- **Why it breaks:**
  Unlike `read-live*` (`:1362–1364`), when `settle!` (`:961–962`) reaches `(<= (:live max-rounds) round)` while `join` is non-empty, it returns `{:reading reading :known known :joined joined :left left}` without joining the final `join` set and without calling `end-read!`. As proven in `Probe 3`, `(:missing reading)` then contains unjoined sources not in `(:sources read)`, setting `:owed? = true` with `:tell = nil` and permanently wedging the live read.

---

## Step 5 — Piece 3 (`Store: a layer's acts and refusals, kept and read by stamp`)

**Status:** Waiting on the Stage 2 commit (`Store: a layer's acts and refusals, kept and read by stamp`). Stage 1 (`7f1242f3` — `Store: each gate keeps a layer's line, and the act again within its bound`) was committed at `15:10:14`, and uncommitted Stage 2 edits in `src-store/softland/store/read_path/` have been idle since `15:11`.

### Preliminary Stage 1 Findings Already Derived From `7f1242f3` (Read-Only Inspection)

1. **256 KiB refusal-cap bypass via non-`int?` `:door-time` in `digest/weighed` (`src-store/softland/store/door/digest.clj:527–531`):**
   - `without-the-time` strips top-level `:door-time` before `text/feed`. For a raw arrival whose `:door-time` is not `int?` (refused `:no-door-time` at declaration, so governed by the `256 KiB` cap), line `531` calls `(held! t)` (`weight/weight-of`) instead of `text/feed`:
     - If `:door-time` is `(byte-array 200000)`, `(text/utf8 thing)` is **`~400,050` bytes (`> 256 KiB`)**, whereas `weight/weight-of` counts `1 + 200,000 = 200,001 <= 262,144` — **keeping the ~400 KiB text whole in `$$things`** and contradicting `digest/weighed`'s own docstring (`lines 480–483`: *"a run of bytes counts as its text does, two hex digits for each byte and two quotes"*).
     - If `:door-time` is `(vec (repeat 30000 1000000000000000000))` (`30,000` 19-digit `Long`s), `(text/utf8 thing)` is **`~600,050` bytes (`~586 KiB`, >2.2x over 256 KiB)**, whereas `weight/weight-of` counts `1 + 30,000 * 8 = 240,001 <= 262,144` — keeping it in `$$things` with `:weight ~240,050`, which will also cause a stamp read with `:max-bytes 300000` to return `~600 KB` of `one-text`!
     - Even for `:door-time nil` or `:door-time false`, `weighed` undercounts `(count (text/utf8 thing))` (`"nil"` = 3 bytes vs `weight-of` = 1; `"false"` = 5 bytes vs `weight-of` = 1).
2. **Double-counting of `BigDecimal` (and `#other` / `#too-deep`) in `text/feed*` (`src-store/softland/store/door/text.clj:674–675`):**
   - When `x` is a valid `BigDecimal` (e.g. `1.5M`), `(written-whole? x)` is `false`, so `feed*` calls `(take-held x)` (`weight-of` = `5`) **and** `(take-text (scalar-text x))` (`"1.5M"` = `4`), giving `:weight 14` for `{:a 1.5M}` whose `one-text` is `9` bytes.
3. **Unbounded $O(n^2)$ `BigDecimal.stripTrailingZeros` stall on the shared task thread (`src-store/softland/store/door/text.clj:150`, acknowledged in `src-store/softland/store/README.md:153–158`):**
   - Every non-stray arrival (`digest/weighed`) and every stray (`digest/hex-of`) runs `scalar-text` -> `.stripTrailingZeros` before any bound stops the digest walk (`8.2 s` at `83 KB` on JDK 21, blocking every layer sharing that Rama task).

*(Full Step 5 execution — running `store-piece-3/statements/` over the landed module, measuring heavy-refusal cost on a shared multi-layer task, and counting/measuring seeks of each read class — is ready to run as soon as Stage 2 is committed.)*

---

## What Was Run, What Was Only Read, What Is Open, and My Choices

### 1. What was run, with its output

- **`gate-histories` (`20,000` histories × seeds `1..5` + `mutants`) on detached worktree of `d3290f37`:**
  ```sh
  cd src/proposal/runtime-2026-11-21/gate-histories
  for s in 1 2 3 4 5; do clojure -M:run 20000 "$s"; done
  clojure -M:run mutants
  ```
  Output summary (all exit `0`):
  - Seed `1` (`175.3 s`): `825,699` things sent, `764,962` decisions (`164,310` taken: `{:act 16227, :layer 126216, :run 1867}`; `600,652` refused), `HOLDS: agree, told, once, outsiders, stamps, kept, see, unseen, screen, run-again`, `0` breaks.
  - Seed `2` (`182.1 s`): `829,310` things sent, `768,072` decisions (`165,096` taken: `{:act 16360, :layer 126988, :run 1748}`; `602,976` refused), `HOLDS`, `0` breaks.
  - Seed `3` (`183.6 s`): `827,762` things sent, `766,791` decisions (`164,400` taken: `{:act 16236, :layer 126390, :run 1774}`; `602,391` refused), `HOLDS`, `0` breaks.
  - Seed `4` (`191.0 s`): `830,205` things sent, `768,927` decisions (`165,270` taken: `{:act 16472, :layer 126984, :run 1814}`; `603,657` refused), `HOLDS`, `0` breaks.
  - Seed `5` (`178.0 s`): `824,901` things sent, `764,726` decisions (`163,323` taken: `{:act 16226, :layer 125310, :run 1787}`; `601,403` refused), `HOLDS`, `0` breaks.
  - `mutants`: `12/12` mutants caught (`S3` at history 12, `S4/S5` at 1, `S6` at 1, `S7` at 2, `S8` at 5, `S9` at 9, `S11` at 7, `S12` at 4, `R1/R9` at 1419, `R8` at 1, `R2 any-act` at 1, `R2 used-more-than-once` at 2).

- **Step 4 pure adversarial probe (`break_piece_2_yes.clj`) on detached worktree of `d3290f37` (exit `0`):**
  ```text
  === PROBE 1A: gate-histories.run/play breaks :unseen AND leaks confidential data ===
    violations count: 1
    BROKEN at step 7 - unseen : dee, asking in run-2, may not see the layer before and after, and was told 2 times: {:anew {:layers [:lab], :left-out [], :place {:holds :value, :layer :lab, :fact [[:lab :streaming #uuid "00000000-0000-0000-0000-00000000000a"] 0], :stamp 5, :value "said at 10"}}}

  === PROBE 1B: Phantom standing chain across stale per-row pushes (pure read-path) ===
    [:permission] states explored: 16 | total tells delivered (expected 0): 5 | leaked 'TOP-SECRET-99mg' to :ann? true
    [:run-permission] states explored: 16 | total tells delivered (expected 0): 5 | leaked 'RUN-SECRET-42mg' to :ann? true

  === PROBE 2: Refusal-word divergences (decision/standing vs SPEC.md R9 & reference.clj) ===
    2a: wrong-digest act-B under USED yes y-used            -> gate: :permission-used-up | SPEC/reference: :act-not-the-one-permitted
    2b: wrong-digest act-B under ENDED yes y-ended          -> gate: :permission-ended   | SPEC/reference: :act-not-the-one-permitted
    2c: wrong-session (:run-2) under ENDED :run-1 perm      -> gate: :permission-ended   | SPEC/reference: :permission-for-another-run
    2d: NEVER-GIVEN forged yes (wrong hex) on USED y-used   -> gate: :permission-ended   | SPEC/reference: :act-not-the-one-permitted
    2e: NEVER-GIVEN forged yes (right hex) on USED y-used   -> gate: :permission-ended   | SPEC/reference: :act-not-the-one-permitted

  === PROBE 3: settle! hitting (:live max-rounds) drops unjoined sources & wedges read ===
    read still in :reads? true | callbacks: [] | :owed?: true | final wanted source in :sources? false

  === PROBE 4: hex-for-a-yes hashes declaration-invalid acts -> unredeemable standing yeses ===
    4a..4e (257 facts, 70 KB value, duplicate place, bad store key, 260-char id): all hex?=true, grant=accepted, redeem=refused, stuck standing?=true

  === PROBE 5: use-of-its-yes phantom fact-id [[layer depot uuid] 256] & 771-write max ===
    256-fact marked yes verdict: :accepted | total decision writes: 771 | returned :fact index: 256 (out-of-bounds? true)
  ```

- **Step 4 4-task `InProcessCluster` probe (`cluster_probe_piece_2_yes.clj`) behind `cluster.lock` on detached worktree of `d3290f37` (exit `0`):**
  ```text
  === CLUSTER RECEIPT 1: Confidential data leaked to :may-not-see asker on live 4-task cluster ===
    atomic end-and-give door answer:            {:answer :accepted, :stamp 7, :time 1795339885565}
    read-once for :ann after act (:left-out):   [{:layer :c-secret, :why :may-not-see}] | value: aspirin-81mg
    [shared ReadPath] callbacks to :ann (exp 1): 3
      callback 0 -> value="aspirin-81mg"     left-out=[{:layer :c-secret, :why :may-not-see}]
      callback 1 -> value="TOP-SECRET-99mg"  left-out=[]
      callback 2 -> value="aspirin-81mg"     left-out=[{:layer :c-secret, :why :may-not-see}]
    [isolated ReadPath] callbacks to :ann (exp 1): 2
      callback 0 -> value="aspirin-81mg"     left-out=[{:layer :c-secret, :why :may-not-see}]
      callback 1 -> value="aspirin-81mg"     left-out=[{:layer :c-secret, :why :may-not-see}]

  === CLUSTER RECEIPT 2: :base (:microbatch) yes through door + 4-task replication + 256-fact 771-write yes ===
    :base yes grant answer:                     {:answer :accepted, :stamp 3, :time 1795339886580}
    :base 256-fact yes redeem answer (771 wr):  {:answer :accepted, :stamp 4, :time 1795339886791}
    :base second send under new act-id answer:  {:answer :refused, :reason :permission-used-up, :stamp 5, :time 1795339887025}
    use-of-its-yes replicated on all 4 tasks?   [true true true true]
    read-once :stores-own-facts on used yes:    {:holds :nothing-here, :layer :base, :fact [[:base :microbatch #uuid "cf0751c4-ddb1-4d3d-a244-0a72ab925699"] 256], :stamp 4, :writer :ben, :permission [...]}

  === CLUSTER RECEIPT 3: Module update (restart) + standing & used yeses across restart ===
    y-used before update:         {:answer :accepted, :stamp 2}
    y-used retried after update:  {:answer :refused, :reason :permission-used-up, :stamp 3}
    y-stand used after update:    {:answer :accepted, :stamp 4}
    y-stand 2nd try after update: {:answer :refused, :reason :permission-used-up, :stamp 5}
    :after fact in store:         {:holds :value, :layer :c-restart, :stamp 4, :value "redeemed-after-update"}

  === CLUSTER RECEIPT 4: Forged yes on used UUID & unredeemable 257-fact yes through door ===
    wrong-digest m2 on used pa1:            {:answer :refused, :reason :permission-used-up, :stamp 4}
    never-given forged yes on used y1:      {:answer :refused, :reason :permission-ended, :stamp 5}
    grant of 257-fact yes answer:           {:answer :accepted, :stamp 6}
    holder redeem of 257-fact yes answer:   {:answer :refused, :reason :too-many-facts, :stamp 7}
  ```

- **Earlier steps run in this job (for completeness):**
  - Step 1 (`402efee6` -> `d70161a9`): All 7 runtime attack namespaces failed (`exit 1`) on `d8ee975c` and passed (`7/7`, `exit 0`) on `f7163f93` (with `BUILD.md` overrides) and on `d70161a9` (plain `:runtime` against real `src-auth` `7a381f3a` and `src-store` `d3290f37`); recorded in [`test-runtime/softland/runtime/attacks/README.md`](../../../../test-runtime/softland/runtime/attacks/README.md).
  - Step 2 (`f04298ee`): `clojure -M:run` in `store-piece-3/statements/` (`53,895 ms`, `exit 0`): `boundary=true, model-histories=240 (breaks=0), broken-copies=16/16 caught`.
  - Step 3 (`15a90b0f`): 1-fault (`128` runs, `97,307` states, `0` breaks) and 2-faults-in-one-history (`136` runs, `1,188,828` states, `TF-01` caught 2 cross-depot `*enters`/`*root-events` replay flaws + `replace-secret` transient cycle); recorded in [`spine/auth/PLAN_VALIDATION.md`](../spine/auth/PLAN_VALIDATION.md).

### 2. What was only read

- `git diff 1a2e9811..d3290f37 -- src-store test-store`, `BUILD-store-piece-2-repair.md`, `store-piece-2/SPEC.md`, `gate-histories/src/gate_histories/{reference.clj, walk.clj, live.clj, run.clj, cluster.clj}`, `REVIEW-store-piece-2.md`, and `git diff d3290f37..7f1242f3 -- src-store` (Piece 3 Stage 1).
- Uncommitted files in `src-store/softland/store/read_path/` were **not** read while the builder's edit is in flight, and `store-piece-3`'s `PLAN*` / `PLAN_VALIDATION*` were **never** opened.

### 3. What is open

1. **Step 5 (`Store: a layer's acts and refusals, kept and read by stamp`):** Ready to run the moment Opus commits Stage 2 of Piece 3 (`src-store/softland/store/read_path/acts.clj` et al., untouched on disk since `15:11`).
2. **Repair of the P1 phantom-standing leak (`answer.clj` / `sources.clj` / `decision.clj`):** Either forbid giving a permission beneath `own` in the same act that ends `own` (so no dead-on-arrival permission with stamp $> \text{parent}$ is ever written), or avoid trusting a newly arrived child permission row against a held parent row until a fresh read/barrier confirms the parent was not ended at the child's stamp.
3. **Refusal-word precedence (`decision.clj:321–345` vs `SPEC.md` R9):** Decide whether `SPEC.md` R9's unconditional wording (`:act-not-the-one-permitted` / `:permission-for-another-run` before ended/used) governs or `decision.clj`'s row-first order governs, and avoid returning `:permission-ended` for a never-given permission on a *used-up* yes's UUID.

### 4. My choices, each yours to turn

1. **Step 1 guard keys & defaults (`test-runtime/softland/runtime/attacks/numbers.clj`):** `:sign-in-window-ms 10000`, `:max-sign-in-posts-per-address 30`, `:max-watches-per-root 256`, `:max-visitor-watches 64`.
2. **Step 2 Piece 3 statement choices (`src/proposal/runtime-2026-11-21/store-piece-3/statements/`):**
   - Counted a thing's byte size as the UTF-8 byte length of `softland.store.door.text/text` of the arrived thing (`door-time` included), matching `max-value-bytes` / `max-act-bytes`.
   - Treated `"a sender with standing"` for the `32 MiB` refusal bound as a thing that passed `declaration` and whose named `:permission` stands in the layer and belongs to `:who` (`:see` holders included; wrong-run, wrong-yes-digest, ended, used, or forged permissions get the `256 KiB` no-standing bound).
   - Named the layer acts-reading setting `:softland.store/who-may-read-acts` (`:members` default even on `:base`, `:everyone` to open; written by first holder only, retroactive, non-retractable). *(Matches what Stage 1 `7f1242f3` independently chose.)*
   - Kept the runner's `:read-line` adapter generic over the exact shape of the stamp-read query/answer so Step 5 maps the store's public `read-path` call straight onto the statements without editing `statements.clj`.
3. **Step 3 two-fault scope (`overnight-audit/auth-concurrent/`):** Extended `model_check.clj` with `:fault-budget 2` and a `:mixed` cross-mechanism fault mode combining depot-append replay with PState/worker stale-read and partition retry faults in one history (`TF-01..TF-04` alongside all 32 `agreement` scenarios).
