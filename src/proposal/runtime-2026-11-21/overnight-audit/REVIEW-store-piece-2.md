# Adversarial Review — Store Piece 2 (`34fdab0e^..3544d026`)

Scope: `git diff 34fdab0e^ 3544d026 -- src-store test-store`, weighed against [`BUILD-store-piece-2.md`](../BUILD-store-piece-2.md) and [`store-piece-2/SPEC.md`](../store-piece-2/SPEC.md).

Probes written for this review (read-only against `src-store` and `test-store`):
- Pure probe: [`overnight-audit/probes/break_piece_2.clj`](probes/break_piece_2.clj)
- In-process cluster probe (run behind `cluster.lock`): [`overnight-audit/probes/cluster_probe_piece_2.clj`](probes/cluster_probe_piece_2.clj)

---

## 1. Histories of Acts That the Gate Decides Wrongly (or Where R2 / R5 / Door Break)

### 1A. Re-granting an `:act-permission` after an end + refused attempt creates a dead standing permission (`SPEC.md` R5 vs `walk.clj` check `a`, diverges from `gate-histories/reference.clj`)

- **What `SPEC.md` R5 says (`store-piece-2/SPEC.md:104`):** *"After an end, the run or the act can be given a permission again."*
- **Why it breaks:** An `:act-permission` `[who layer granted-in parent :see-and-act [:act u hex]]` pins the `uuid` `u` of the act's `:act-id`. In `module/walk.clj:331` (`check (a)`), the **first** non-stray arrival under `[layer depot u]` — including a refusal while `pa1` or an ancestor permission was ended, or an early arrival before `pa1` landed — writes a permanent row at `answer-key(layer, u)` in `$$answers`. When a giver then re-grants permission `pa2` (or `pa1` again) at `{:e u :k :softland.store/act-permission :v pa2}`, the gate **accepts** the re-grant (`pa2` stands in `$$facts` forever), yet when the holder sends `(assoc x :permission pa2)` under `[layer depot u]`, `walk/decide` drops it at `check (a)` without deciding it or writing any fact. Because `:permission` is inside `digest/digest` (`$$answers`), `door/answer-of` returns `{:answer :not-mine}` when `pa2 != pa1` (or returns the stale `{:answer :refused :reason :permission-ended}` from before the re-grant when `pa2 = pa1`).
- **Input that shows it (7-act history in `break_piece_2.clj` `probe-1-re-grant-act-permission` and `cluster_probe_piece_2.clj` Receipt 1):**
  1. `:asha` makes `:c-lab`. (`:accepted`)
  2. `:asha` grants `pa1 = [:ben :c-lab :c-lab root :see-and-act [:act u hex-x]]` on `u`. (`:accepted`)
  3. `:asha` ends `u` under `:softland.store/act-permission`. (`:accepted`)
  4. `:ben` sends `(assoc x :permission pa1)` with `act-id [:c-lab :streaming u]`. (Gate: `:refused :permission-ended`, writes `answer-key(:c-lab, u)`.)
  5. `:asha` grants `:cal` layer permission `cal-perm`. (`:accepted`)
  6. `:cal` grants `pa2 = [:ben :c-lab :c-lab cal-perm :see-and-act [:act u hex-x]]` on `u`. (Gate: `:accepted`; `pa2` stands in `$$streaming-facts`.)
  7. `:ben` sends `(assoc x :permission pa2)` with `act-id [:c-lab :streaming u]`.
     - `gate-histories.reference/judge` (plain-code R1–R10): **`:accepted`**.
     - `walk/decide` (pure & in-process cluster): **dropped at check (a)**; `door/answer-of` -> **`{:answer :not-mine}`**; `:exp-1 :dose` -> **`{:holds :no-fact}`**.
- **Why `gate-histories` missed it in 20,000 runs:** Read & run. `gate-histories/reference.clj` tracks no `answers` table at all, and `gate-histories/run.clj` (`lines 119–167, 252–274, 492`) has no `:give-act` generator and increments `n` by `+2` on every `:yes-for-one-act`, so it **never** granted two permissions to the same act UUID.

### 1B. Any `:see`-only holder (or unsigned-in reader in an `:everyone` layer) can permanently burn a granted `:act-permission` (`SPEC.md` S9 / R2)

- **Why it breaks:** Every granted `:act-permission` sits in `$$facts` at entity `u` (`<act-uuid>`) under `:softland.store/act-permission`. `read_path/answer.clj:112` (`counts?`) exposes all store-key facts to any reader who can `:see` the layer when `:stores-own-facts true` is passed, and `door/additions.clj:126` (`act-id-word`) accepts any caller-supplied `[layer depot u]`. A `:see`-only holder (who cannot act, give, or end per S9) can read `u` as soon as `pa` is granted and send a dummy act under `[layer depot u]`. The gate refuses the dummy act (`:permission-does-not-allow-acting`) and writes `answer-key(layer, u)`, permanently preventing the legitimate holder's permitted act from ever being decided.
- **Input that shows it (run on in-process cluster, `cluster_probe_piece_2.clj` Receipt 2):**
  1. `:ida` makes `:c-studio`, gives `:cal` `:see`-only permission, and gives `:joe` `:act-permission` `pa` on `u` (`#uuid "00000000-0000-0000-0000-000000000063"` in pure probe).
  2. `:cal` calls `read-path/read-once` with `{:asks [:run :first :last] :asker :cal :layers [:c-studio] :stores-own-facts true}` and reads `u` from `[:places [u :softland.store/act-permission]]`.
  3. `:cal` calls `(door/send-act d dummy-act [:c-studio :streaming u])` -> `{:answer :refused, :reason :permission-does-not-allow-acting, :stamp 3}`.
  4. `:joe` calls `(door/send-act d (assoc maker-joe :permission pa) joe-act-id)` -> `door/answer-of` gives **`{:answer :not-mine}`**, and `:secret :dose` in `:c-studio` stays **`{:holds :no-fact}`**.

### 1C. `digest/hex-for-its-permission` mismatches `door/send-act` on a maker's act, and hashes pre-parse syntax rather than `act/parse`'s normal form (`SPEC.md` R2)

- **Door mismatch (run in `probe-2`):** `door.clj` was untouched in Piece 2. A maker holds a maker's act (without `:act-id` or `:depot`) and an `act-id` from `door/act-id-for`. Passing the maker's act to `digest/hex-for-its-permission` yields `a3f2ae87...`, whereas `door/send-act` adds `:act-id` and `:depot` so `decision/hex-for-its-permission` computes `f0cbfa98...` and refuses `:act-not-the-one-permitted`. Yet if the maker attaches `:act-id` and `:depot` to her act map, `door/send-act` (`additions/reading`) refuses `{:could-not :add-its-parts, :at :act-id}`! No function in `softland.store.door` builds the permission digest from `(maker-act, act-id)`.
- **Pre-parse mismatch (run in `probe-6d`):** `SPEC.md` R2 says *"the act is the act that the permission names, part for part"*. Two acts whose `act/parse` one-form is identical — `{:facts [{:e :x :k :n :v 5}]}` vs `{:subjects #{} :based-on {} :facts '({:e :x :k :n :v 5N})}` — produce different `hex-for-its-permission` digests (`false`), so the second is refused `:act-not-the-one-permitted`.

---

## 2. Read-Path Callback Behavior, Push Amplification, Leak, and Falsified Round / Call Bounds

### 2A. Another holder's `run-permission` on `:session` in a `:may-not-see` layer joins her chain pushes and fires spurious `{:anew}` callbacks (`sources.clj:580`, `answer.clj:492–516`)

- **Why it breaks:** In `layers/run-permission-state` (`layers.clj:174`), when `run-row` names another holder (`(not= asker (nth held 0))`), standing is immediately `:none` without consulting `chain-rows`. However, `sources/reading` (`sources.clj:580`) calls `(chain-sources home layer (row-of run-permission))` **without** checking that `held`'s holder is `asker`.
- **Input & callback sequence (run in `probe-4` and on in-process cluster in `cluster_probe_piece_2.clj` Receipt 3):**
  1. `:c-near` is `:members` (`:root` -> `:bea`, holding secret fact `:p :text = "secret"`); `:c-far` is `:everyone` (`:p :text = "public"`).
  2. `:ann` (who holds **no** permission in `:c-near`) opens `read-live` on `{:asks [:place :p :text] :asker :ann :session :run-7 :layers [:c-near :c-far]}`. Initial `:anew` callback #1 gives `"public"` with `:left-out [{:layer :c-near :why :may-not-see}]`; `open-count` `:pushes` = `5`.
  3. `:bea` grants **`:dov`** (not `:ann`) `[:dov :c-near :c-near bea-perm :see [:run :run-7]]` in `:c-near`.
     - `run-7` push callback runs `settle!` -> joins `:dov`'s chain (`:bea` and `:root` pushes in `:c-near`; `open-count` `:pushes` rises to `7`, and up to `+31` pushes for a depth-32 chain, counting against `:c-near`'s 900-push cap) and sets `:owed? true`.
     - When `:bea` and `:root` pushes arrive (`on-arrival`), `:ann`'s callback is called a **second time** with an identical `{:anew ...}`!
  4. `:bea` ends `:dov`'s `:run-7` permission in `:c-near`.
     - `settle!` leaves `:bea` and `:root` (`owes-for-a-move?` = `true`), calling `:ann`'s callback a **third time** with an identical `{:anew ...}`!
  - Directly violates `read_path/layers.clj:299–302`: *"not even an answer that says what it said is given, which would tell her that something of that layer was written."*

### 2B. Falsified `read-once` cost claim: *"rows more, and no call more"* (`read_path.clj:1226–1229`, `BUILD-store-piece-2.md:70–71`)

- **Why it breaks:** Every `run-permission` has depth $\ge 2$ (`parent` slot is mandatory in `act.clj:213`), so `chain-sources` is never empty once `run-row` holds a `run-permission`. Whenever `:asker` has no layer permission (`nil`, the exact primary case of `a-read-asked-in-a-run`) or holds a depth-1 first permission, `read-once` without `:session` finishes in **1 round / 1 call**, whereas `read-once` with `:session` takes **2 rounds / 2 calls** (`Round 0` reads `run-row`; `Round 1` makes a second RPC for `chain-sources`) — even when `run-row` belongs to another holder `:dov`!
- **Input (run in `probe-6a`):** On `:m-run` where `:ada` gave `:cal` a `:run-9` permission, `play-once` for `:cal` without `:session` gives `[1 [1]]` (1 call); with `:session :run-9` gives **`[2 [1 1]]` (2 calls)**. For `:ada` (first holder) with `:session :run-9` held by `:dov` beneath `:bea`, without `:session` gives `[1 [1]]`; with `:session :run-9` gives **`[2 [1 1]]`**.

### 2C. Falsified 6-round static bound (`sources.clj:83–91`, `read_path.clj:344–346`, `read_path_pure_proof.clj:1232`)

- **Why it breaks:** `sources.clj:83–91` claims sources form 6 ranks (`0..5`) so the loop *"ends within six rounds"*, and `read_path.clj:344–348` sizes `max-rounds = 9` as *"Six are derived from the six ranks... three more are for a list, a setting or a permission that is written while a live read opens."* For `:from-layer :start`, Rank 1 (`own-rows` of listed layers) actually waits on `standing(:start) = :yes`, which waits on Rank 3 (`chain` of `:start`). Because every `run-permission` has depth $\ge 2$, Ranks 2–3 execute **twice** (rounds 1–2 for `:start`, round 3 for listed `own-rows`, rounds 4–5 for listed `run-permission` + `chain`, round 6 for `facts-by-value`, round 7 for `nearer-checks`).
- **Input (run in `probe-3` and `probe-6b`):** `{:asks [:points-at :parent :canvas-1] :asker :cal :session :run-7 :from-layer :start}` across `[:start :mid :far]` takes **7 rounds** when `:far` is `:everyone` and **8 rounds** on a completely static store when `:far` is `:members` (leaving only **1** spare round before `max-rounds = 9` aborts `read-live` with `:get-an-answer-within-its-wait`).
- **What held when probing callback permutations (`probe-5`):** Exhaustive `every-order` exploration across 5 stress worlds that `read_path_pure_proof.clj` omitted (marked `:points-at` with dynamic chain joins, `:from-layer` list changes, `:stores-own-facts true` shared rights/fact source, and `:session == layer` inside `own-rows-run`) visited **16,965 states** with **0 wrong final screens**.

---

## 3. Statements of `SPEC.md` and `BUILD-store-piece-2.md` Broken or Unheld by Any Proof

1. **`SPEC.md` R5 self-end statement fails for `:act-permission` and for `:see` permissions (run in `probe-6c`):**
   - `SPEC.md:100–105` states: *"A permission for a run or for an act is ended by its giver, by anyone above the giver, and by its holder. Its holder ends it by an act under that permission itself, or under her permission for the layer... The same holds for her permission for the layer: she ends it under itself"*.
   - Sending `{:e u :k :softland.store/act-permission}` under `pa` with a fresh act-id refuses `:permission-for-another-act`; with act-id `u` refuses `:act-not-the-one-permitted` (and burns `u`!). Sending `{:e :run-1 :k :softland.store/run-permission}` under a `:see` run-permission `r-see` refuses `:permission-does-not-allow-acting`.
2. **`SPEC.md` R6, R5 re-grant, and R3 on `:act-permission` are unheld by any proof (read across all 7 proofs + `gate-histories`):**
   - **R6 on `:act-permission`** (*"an act has at most one permission that stands"*, `:holder-has-a-permission` on `:softland.store/act-permission`): **0 checks** in `pure_proof.clj`, `module_proof.clj`, `run_again_proof.clj`, and **0 generated cases** in `gate-histories/run.clj` (which has no `:give-act` op and never reuses an act UUID).
   - **R3 on `:act-permission`** (sending the permitted act after its `:act-permission` was ended -> `:permission-ended`, or after an ancestor permission was ended -> `:permission-above-ended`): **0 checks** in `pure_proof.clj`, `module_proof.clj`, and `run_again_proof.clj` (`pure_proof.clj` step 26 ended `pb` only *after* step 32 had already burned `uuid 32`, and never sent `y` afterward).
3. **`door_proof.clj` has 0 lines changed in Piece 2 (read from `git diff`):**
   - No proof sends an act under `:run-permission` or `:act-permission` through `softland.store.door` (which is why Finding 1C went unnoticed).
4. **R8 (*"A permission for an act lets nobody see"*) in `read-path` proofs, and unmarked `run-points` (`read_path_pure_proof.clj:1619`):**
   - Neither `read_path_pure_proof.clj` nor `read_path_proof.clj` ever writes a `:softland.store/act-permission` row (only `see_check.clj` on `gate-histories` had `:act-permission` rows in `facts`). And `read_path_pure_proof.clj`'s `every-small-run` tested `:points-at` only *"under a key that no layer marks"* (`line 1619`), so zero `facts-by-value` runs or `nearer-checks` were exercised with `:session`.
5. **Inaccurate proof claim in `BUILD-store-piece-2.md:98–103` (read against `module_proof.clj:3331–3360`):**
   - `BUILD-store-piece-2.md` claims `module-proof` ran *"an end by the giver and by the run itself, a second give, the permitted act and the same act-id with another value: through both gates on a cluster, in a streaming layer and in the base in one batch"*. In `a-permission-for-a-run-and-for-an-act-in-the-base` (`module_proof.clj:3347`), `(thing :base :nia pa ...)` generated a **new** random `act-id` (testing `:permission-for-another-act`, **not** `:act-not-the-one-permitted` / *"the same act-id with another value"*), and neither a self-end by the run nor a second give was run in the base.

---

## 4. Explanations That No Longer Match Their Code (Read)

1. `src-store/softland/store/gate/chain.clj:18` & `:55`: *"1. the row at its holder's place holds it"* and *"place  a layer, a permission -> the key of its holder's place"*. `place` (`lines 88–93`) returns the session's place for `:run` and the act UUID's place for `:act`, not the holder's place.
2. `src-store/softland/store/gate/decision.clj:241–243`: `:permission-does-not-stand` docstring still says *"does not hold the act's permission for its holder"*, whereas `permission-place` sits on the holder, session, or act UUID.
3. `src-store/softland/store/README.md:1219, 1359–1360, 1395–1396, 1405–1407`:
   - `line 1219` defines *"an end"* as *"A fact with no value under `:softland.store/permission`"* (omitting `:run-permission` and `:act-permission`).
   - `lines 1359, 1405–1407` still describe `:permission-fact-not-a-permission`, `:permission-fact-for-another-layer`, `:permission-fact-granted-in-another-layer`, and `:permission-fact-not-beneath-the-acts-own` as applying only to `"A :softland.store/permission fact"` rather than all three `permission-keys`.
   - `line 1396` still describes `:permission-ended` as *"The place of the act's `:who` holds an end: her permission in the layer was ended, and she was not given one again"* (false when a run's or an act's place holds an end).
4. `src-store/softland/store/record/rows.clj:376–377, 555–556`: `held-permission` still says *"given to the place's holder"*; `own-rows-run` says *"and its holder's permission, when the layer's id is also a holder's id"*, omitting `:run-permission` when `session = layer` and `:act-permission` when `act-uuid = layer`.
5. `src-store/softland/store/read_path/layers.clj:17–26, 287`: namespace docstring lists *"her permission row"* and *"the rows above it"* while omitting the run's permission row under `:softland.store/run-permission`; `meaning` (`line 287`) says *"What a layer's own rows and the asker's permission row mean"*, though `meaning` now takes `either`'s combined word.
6. `src-store/softland/store/read_path/sources.clj:155–159, 544–546`: `chain-sources` docstring says *"Takes... the asker's permission row"` (also called on `run-permission` row at `line 580`); `reading` (`lines 544–546`) says `:may-not-see` *"wants nothing but its own rows, her permission and the rows above it"*, omitting `run-permission` and its chain.
7. `src-store/softland/store/read_path/answer.clj:479–480`: `after-change` docstring says *"the source is a layer's own rows or her permission"*, omitting `:run-permission` and `:above` sources (`sources/rights-kind`).
8. `src-store/softland/store/read_path.clj:1226–1229`: *"rows more, and no call more"* (falsified in Finding 2B).

---

## Summary of What Was Run vs Only Read

- **Run:**
  - `clojure -M:store -m softland.store.act-proof` (166 checks, 0 failed)
  - `clojure -M:store -m softland.store.pure-proof` (724 checks, 0 failed)
  - `clojure -M:store -m softland.store.read-path-pure-proof` (223 checks, 0 failed)
  - `clojure -M:store -i src/proposal/runtime-2026-11-21/overnight-audit/probes/break_piece_2.clj` (Probes 1A, 1B, 2, 3, 4, 5, 6A–6D)
  - `flock .../cluster.lock clojure -M:store-module -i src/proposal/runtime-2026-11-21/overnight-audit/probes/cluster_probe_piece_2.clj -e "(cluster-probe-piece-2/-main)"` (Cluster Receipts 1–4 on 4-task `InProcessCluster`)
- **Only read:** `git diff 34fdab0e^ 3544d026 -- src-store test-store`, `BUILD-store-piece-2.md`, `store-piece-2/{SPEC.md, HANDOFF.md, see_check.clj}`, `door_proof.clj`, `module_proof.clj`, `read_path_proof.clj`, `run_again_proof.clj`, `gate-histories/src/gate_histories/{reference.clj, walk.clj, run.clj}`, and the store's READMEs and docstrings.
