VERDICT: major-fail

# Plan Validation — `gates-keep-the-line` (stage 1 of piece 3)

Adversarial Phase 2 validation of `src/proposal/runtime-2026-11-21/store-piece-3/PLAN-gates-keep-the-line.md` against `SPEC.md`, `IMPLICIT_SPEC.md`, `DECOMPOSITION.json`, `src-store/CLAUDE.md`, `src-store/README.md` (decisions of 12 October entry 7, 13 October, and 20–21 November), the Rama 1.6.0 documentation under `/mnt/data/projects/research/softland/rama-docs-for-1.6.0-2026-10-12/`, and `.agents/skills/rama/references/`.

**Summary of verdict (`major-fail`):**
- **What holds:** The plan's core storage direction — keeping decided things in a separate PState (`things`) alongside small metadata rows (`line`), rather than reading records back from the depot by offset — **survives** our attempt to break it in Rama 1.6.0 (Item `a`), and the plan's claim that `IMPLICIT_SPEC.md` S4 is logically unsatisfiable as written **holds** on trace (Item `d`).
- **Why the plan fails Phase 2 (`major-fail`):**
  1. **Unbounded task-thread stall, RocksDB write/compaction amplification, and two bypasses in `weight` on refused things (`Items b, e`; `Spec coverage` O1/W6/W7):**
     - Any sender with **no standing** in a layer can append 32 MiB refused maps that `walk/decide` serializes, writes to RocksDB, and replicates 3× synchronously on a task shared with hundreds of other layers (~424–600+ ms per event in-process before replication; ~15–20 sends exceed `read_path`'s 10,000 ms `:wait-ms` bound for unrelated layers on that task). The plan never weighs a smaller bound for non-standing / declaration refusals.
     - Line 373 requires `:too-heavy` rows in `line` to record the refused thing's exact `:weight`, preventing `weight` from stopping at `max-kept-weight + 1` and forcing `walk/decide` to traverse the entirety of an arbitrarily large (e.g. 250 MiB) refused map on the task thread (`W7` FAIL).
     - Lines 326–328 misclassify Java arrays (`int[]`, `long[]`, `double[]`, `Object[]`): arrays are neither records, maps, nor collections, so falling into *"the others as their printed text"* weighs a 500 MiB `int[]` or `Object[]` as ~35 bytes (`"#object[\"[I\" ...]"`), bypassing `max-kept-weight` completely and risking a partition-stopping write throw (`W6`, `W7` FAIL).
     - Similarly, `text/text` calls `BigDecimal.stripTrailingZeros` (`text.clj:142`) and `BigInteger.toString` (`text.clj:139–140`): a `BigDecimal` such as `(BigDecimal. (.pow BigInteger/TEN 2000000) 2000000)` weighs 2 bytes (`"1M"`) in `text/text` while carrying ~830 KB in Nippy and burning >10 seconds of CPU in `.stripTrailingZeros` inside `walk/decide`.
  2. **Redundant full traversal on 100% of valid and ordinary refused acts (`Item e`; `Throughput` FAIL):** Step 2 (`walk.clj:338`) already walks every non-stray thing via `digest/digest` (`text/feed`). Running a second full walk for `weight` on acts that hold no `#other` and no `#too-deep` doubles traversal work unnecessarily instead of accumulating byte count in the `digest` pass and falling back to the explicit-stack walker only on `#other` / `#too-deep`.
  3. **Partitioning efficiency table omits RocksDB seeks and iterator-read sums (`Partitioning` FAIL):** Lines 477–484 substitute `"Tasks read"` (`1, 1, 1`) for total RocksDB seeks/op (concealing 6+ seeks on normal pages and **$1 + 2k$ random point seeks** — 201 seeks for $k = 100$ — when reading an outside asker's rows via `line-by-who`) and omit numeric iterator-read sums `Σ(proportion × iterator-reads)` across $N = 1, 16, 128$.
  4. **Inaccurate Rama claim on `microbatch-gate` in Road B / Q7 (`Item a`):** Lines 134–150 and 508–510 state that neither topology can learn or count its record's depot offset. That is true for `streaming-gate`, **false for `microbatch-gate`**, where sequential partition emission plus exact-once PState rollback on batch retry allows an exact 0-based partition offset counter in a PState (though `foreign-depot-read`'s non-yielding synchronous fetch still rules Road B out).
  5. **Incomplete Unicode range-hazard hand-off (`Spec coverage` R20):** Hand-off item 12 (`PLAN:672–674`) warns Stage 2 about `U+E000+` / supplementary code points in `<layer>` of `line` keys, omitting `<who>` in `line-by-who` (`"<layer> <who> <stamp>"`), which suffers the identical Rama range-truncation hazard when an outside asker's keyword id contains such a character.

---

## Five Focused Checks (`a` through `e`, in order)

### a. Can a gate keep a depot position and read the act back from its depot in Rama 1.6.0?

**Checked against:**
- `/mnt/data/projects/research/softland/rama-docs-for-1.6.0-2026-10-12/text/live/` (`depots.html.txt`, `stream.html.txt`, `microbatch.html.txt`, `pstates.html.txt`, `clj-defining-modules.html.txt`, `clj-dataflow-lang.html.txt`, `integrating.html.txt`, `all-configs.html.txt`)
- `.agents/skills/rama/references/` (`depot-reference.md`, `stream.md`, `microbatch.md`, `foreign-client.md`, `external-depots.md`, `task-globals.md`, `syntax.md`)
- Public API inspection of `com.rpl/rama` `1.6.0` on `:store-module` (`com.rpl.rama`, `com.rpl.rama.ops`, `com.rpl.rama.module.StreamTopology`, `MicrobatchTopology`, `StreamSourceOptions`, `MicrobatchSourceOptions`, `com.rpl.rama.Block`, `com.rpl.rama.Depot`, `com.rpl.rama.integration.TaskGlobalContext`).

**Findings:**

1. **Does Rama 1.6.0 expose a record's depot offset to a topology consuming a native depot? No.**
   - `StreamTopology.source` (`stream.html.txt:26`, `StreamSourceOptions`) and `MicrobatchTopology.source` + `explodeMicrobatch` (`microbatch.html.txt:27–28, 72`, `MicrobatchSourceOptions`) bind only the record value `*thing`. Neither `com.rpl.rama` nor `com.rpl.rama.ops` (`current-task-id`, `current-microbatch-id`, `current-topology-type`, `module-instance-info`) exposes the current record's offset.
   - Only `com.rpl.rama.integration.ExternalDepot` emits `[offset, key, data]` (`integrating.html.txt:255`, `external-depots.md:60`), and `ExternalDepot` is read-only from Rama (`external-depots.md:128`).
   - Internal progress PStates (`$$__streaming-state-<topologyId>`, `stream.html.txt:109`) are checkpointed only periodically, not per record. Calling `foreign-depot-partition-info` via `TaskGlobalContext.getClusterRetriever()` returns `{:start-offset :end-offset}` of the partition *now* (including all records queued behind the current one), requires an async hop (`completable-future>`, violating `SPEC.md` "What must hold" 3), and is non-deterministic on replay (violating `SPEC.md` "What must hold" 1 and 2).

2. **Can a gate compute and keep its own 0-based partition offset in a PState? YES in `microbatch-gate` (breaking the plan's blanket claim in Road B / Q7), NO in `streaming-gate`.**
   - **`microbatch-gate` CAN keep the exact offset:** `IMPLICIT_SPEC.md` Q7 (`line 1050`) noted *"Nothing is said there of a microbatch"*, and `PLAN` Road B (`lines 134–150`) failed to analyze microbatch counting separately. In `microbatch-gate`, `(%things :> *thing)` emits partition $p$'s records on task $p$ in exact depot append order (`microbatch.md:13`). Microbatch PState writes have **exact-once** semantics: on any failure, the priming phase resets every PState partition to the previous `microbatchId` and replays the exact same `[startOffset, endOffset)` slice (`microbatch.html.txt:89–94`). Because `*microbatch-depot` is never trimmed (`module.clj:58`) and uses no `DEPOT-TOMBSTONE` migration (`depots.html.txt:222`), incrementing a per-task PState counter `(local-transform> [(keypath :next-offset) (nil->val 0) (term inc)] $$microbatch-task)` on **every** emitted record (strays, duplicate `act-id`s, and decisions alike) yields the exact 0-based depot offset `offset` for every decision on that partition, at a cost of **1 local PState read + 1 write per record**. Moreover, `SPEC.md` "What must hold" 2 (`lines 159–161`) explicitly allows a kept depot position *if* `run_again_proof.clj`'s `task-free` form states why it is excluded or dereferenced across 4 vs 8 tasks.
   - **`streaming-gate` CANNOT keep an exact offset:** In a stream topology (`:retry-mode :all-after`), if a worker restarts or loses leadership after PState writes commit on disk/followers but before `$$__streaming-state-streaming-gate` checkpoints progress, already-committed records are replayed from the last checkpoint (`stream.html.txt:109, 123`). Decided acts sit behind check (a) (`$$streaming-answers` keyed by `act-id`), so their writes do not double-count. **Strays and second sends under an already-answered `act-id` have no unique per-arrival key**: two identical strays (e.g. `5` then `5`, as `run_again_proof.clj:615, 617` sends!) or two identical resends cannot be distinguished from a post-commit replay of one record. One post-commit retry permanently shifts the partition's offset counter, causing every later stamp in every layer on that partition to read back an **earlier, wrong record**!
   - *(Why an internal `:disallow` decisions-only depot cannot fix `streaming-gate`: appending only decided things to a second depot breaks Sid's decision 5 of 13 October "Two depots stay", breaks `walk.clj`'s "no append" atomicity/ordering contract, re-appends duplicates on stream/microbatch retry (`microbatch.html.txt:96`, `stream.md:49`), and still stores every decided thing a second time on disk.)*

3. **What reading from the depot by `[partition, offset]` would cost even where exact (`microbatch-gate`):**
   - `Depot.read` / `foreign-depot-read` (`depots.html.txt:170–175`, `depot-reference.md:250–255`) exists **only on the foreign client** — no topology or query topology can call it.
   - **Single-stamp read:** 1 PState read (for `line` + permissions/settings) + 1 `foreign-depot-read(depot, p, o, o+1)` = **2 network round trips** minimum.
   - **Page of $n$ stamps (e.g. $n = 100$):** Because all layers hashing to partition $p$ plus strays and duplicate `act-id`s share one depot partition (`module.clj:64`, `gate/stamp.clj`), a layer's $n$ stamps sit at non-contiguous offsets $o_0 < o_1 < \dots < o_{n-1}$. A reader faces a dilemma:
     - **$n$ point calls** `foreign-depot-read(depot, p, o_i, o_i+1)`: **$n$ RPCs + $n$ disk seeks + $n$ synchronous events** on the task thread (violating `IMPLICIT_SPEC.md` O3 `line 308`: *"the number of round trips of one read does not grow with the number of decisions that it gives"*).
     - **1 range call** `foreign-depot-read(depot, p, o_0, o_{n-1}+1)`: reads and transfers **every interleaved record of every other layer, stray, and unbounded refused blob** between $o_0$ and $o_{n-1}$ as **one non-yielding synchronous event** on the task thread. `depots.html.txt:175` warns: *"The fetch is done as a single synchronous event, so to avoid locking up the task thread for too long it's recommended to limit the number of records fetched per call. As a rule of thumb you shouldn't fetch more than 50kb worth of data at a time."* By contrast, `foreign-select` on a PState **yields implicitly** (`pstates.html.txt:223`, `foreign-client.md:118`) and skips oversized entries before loading `things`.

**Ruling on `a`:** Road A's choice to keep decided things in `things` rather than reading from the depot **stands** for `streaming-gate` (correctness wall) and for `microbatch-gate` (non-yielding `foreign-depot-read` wall). Phase 1 must, however, correct Road B and Q7 (`PLAN:134–150, 508–510`) so they do not falsely claim `microbatch-gate` cannot count partition offsets.

---

### b. Weighing a smaller bound for a refused thing against Sid's two lines (not decided; options, costs, and our default marked as Sid's to turn)

**The problem on a shared task:**
In `streaming-gate`, one task thread serves every layer whose `:layer` hashes to that partition — personal layers, group layers, and agent branch layers alike (`record/tables.clj:47–49`). Under `walk.clj`'s contract (`lines 215–230`), `decide` never yields between its first read and its last write, and a streaming batch flushes and 3×-replicates all PState writes before external readers see them or `:ack` returns (`stream.html.txt:107–108`).
- **Today:** A sender with **no permission** in layer `L` who sends a 32 MiB map (e.g. failing `declaration/check` at `:unknown-part` or `:value-too-large`, or failing `decision/standing` at `:permission-does-not-stand` / `:layer-not-made`) causes **0 bytes of PState payload writes** — only ~100 bytes (`answer-row` + `:last-stamp`).
- **Under `PLAN-gates-keep-the-line.md` (`max-kept-weight = 32 MiB` for all decisions):** That same unpermissioned sender forces the shared task thread to serialize and write **32 MiB into RocksDB (`$$streaming-things`)**, replicate 32 MiB across 3 nodes (`src-store/README.md`, 12 October entry 5), and later rewrite it ~4–10× across RocksDB LSM compactions (~128–320 MiB disk writes per refused send). In the plan's own single-JVM probe with **zero** network replication (`PLAN:521`, `keeps_a_thing.out:21`), one 24 MB PState write took **424–438 ms** (~580–650 ms scaled to 32 MiB, and >1 s over a replicated network). Fewer than 20 refused sends from a stranger stall unrelated layers on that task past `read_path`'s 10,000 ms `:wait-ms` ceiling (`IMPLICIT_SPEC.md:307`).

**Held against Sid's two lines:**
1. `PROPOSAL.md`, entry 9 (`lines 188–191`): *"The store gains a read of a layer's acts by stamp, each whole and with its answer. A refused act can be read by its own who and by whoever holds a permission in the layer."*
2. `src-store/README.md`, 12 October 2026, entry 7 (`lines 825–840`): *"why don't we store both? Like the data is useful."* — naming three uses: *(i)* raising a good-faith refused attempt (such as a `:see` holder's) to the place it was for; *(ii)* seeing what a bad-faith sender tries to do; *(iii)* giving an author back what was refused — alongside his words in the same entry (`lines 829–830`): *"this data would not be written down to our actual data, because it is denied."*

**The options and what each costs (no extra PState reads in any option):**

| Option | Kept in `things` | Task stall & RocksDB amplification per stranger's refused send | What it serves and what it gives up against Sid's two lines |
|---|---|---|---|
| **Option 1 (Plan's choice): Single 32 MiB bound for every decision** | Every taken act, gate refusal, and declaration refusal $\le 32\text{ MiB}$ | Up to **32 MiB** PState write + 3× replication + ~128–320 MiB compaction; **~600–1,500+ ms** task stall per send | Serves 100% of acts and refusals $\le 32\text{ MiB}$ via stamp read. Leaves every shared task open to multi-second stalls and gigabyte RocksDB churn from unpermissioned senders. |
| **Option 2: Split by standing — 32 MiB for taken acts and standing members' refusals; 256 KiB (`262,144`) for non-standing and declaration refusals** | **32 MiB** when accepted, or refused in `decision/places` (e.g. `:replaced-fact-not-current`), or refused `:permission-does-not-allow-acting` (a standing `:see` member!). **256 KiB** when refused by `declaration/check` or earlier in `standing` (`:permission-does-not-stand`, `:permission-ended`, `:permission-above-ended`, `:layer-not-made`, `:who-not-the-permissions-holder`, `:layer-belongs-to-the-other-gate`). | At most **256 KiB** ($128\times$ smaller: **~3–5 ms** vs ~600+ ms; ~1–2.5 MiB compacted vs ~250 MiB) for any sender without standing in the layer. **0 extra PState reads** (`standing` already distinguished these cases!). | **100%** of taken acts kept whole; **100%** of standing `:see` members' proposals-to-raise and `:act` members' stale-replace conflicts kept whole up to 32 MiB; **all ordinary non-standing / declaration refusals $\le 256\text{ KiB}$** (~100 pages of text, $4\times$ `max-value-bytes`) kept whole in `things` for author recovery and bad-faith inspection. Only a $>256\text{ KiB}$ refusal from a non-standing sender or malformed envelope gets `:not-kept :too-heavy` in `line` (while **still stored whole forever in the depot log**, honouring *"store both"* and *"not written down to our actual data, because it is denied"*). |
| **Option 3: Split by verdict — 32 MiB for taken acts; 256 KiB (or 64 KiB) for *all* refusals** | **32 MiB** for accepted acts; **256 KiB** (or **64 KiB**) for every refusal | At most **256 KiB** (or **64 KiB**) on any refusal, even from a compromised `:see` member | Protects the task even against a standing `:see` member flooding 32 MiB refused proposals, **but** sacrifices stamp-read recovery of a good-faith member's $>256\text{ KiB}$ multi-fact act that hit `:replaced-fact-not-current` or `:permission-does-not-allow-acting`. |
| **Option 4: Line row only (`0` bytes in `things`) for non-standing refusals** | **32 MiB** for taken + standing-member refusals; **none** (`:not-kept`) for non-standing / declaration refusals | **0 bytes** in `things` for strangers (same ~250 B write as today's `answer-row`) | Breaks `PROPOSAL.md` entry 9 (*"A refused act can be read by its own who..."*) and 12 October entry 7 uses *(ii)* and *(iii)* for strangers/expired holders even on a 500-byte act. |

**Default we would take (ours, marked as Sid's to turn):**
**Option 2** — `max-kept-weight = 33,554,432` (**32 MiB**) for taken acts and standing-member refusals (`places` faults and `:permission-does-not-allow-acting`), and `max-refused-weight = 262,144` (**256 KiB**) for declaration refusals and non-standing refusals, paired with short-circuiting the weight check at `bound + 1` bytes. *(Marked as Sid's to turn: he may turn `max-refused-weight` to 32 MiB [Option 1], 1 MiB, 64 KiB, or 0, or move standing `:see` refusals to the smaller bound [Option 3].)*

---

### c. `line-by-who`: one row by who for each decision, for ever, in every layer

**Trace of who actually reads `line-by-who`:**
- **Holders (`H`):** Never read `line-by-who`; they scan `line` (`"<layer> <stamp>"`).
- **Visitors (`V` / `Vo`) in an `:everyone` layer:** Never read `line-by-who`! Under `SPEC.md` (`line 67`, *"It says what it left out"*) and `IMPLICIT_SPEC.md` R10 (`line 395`), a visitor's read must return **every consecutive stamp** from `start` on (withholding stamps not hers as `{:stamp s}` and giving hers whole), so it already scans `line` sequentially and checks `:who` on each `line` row (`PLAN:481`).
- **Outside askers (`X`) in a `:members` (or unmade) layer:** **The ONLY reader of `line-by-who`** under `SPEC.md`'s read shape! When `X` may not see layer `L`, `SPEC.md` (`lines 120–128, 273–278`) gives `X` her own things in `L` (including acts taken *before* her permission in `L` ended!) with gaps between stamps and at a cost *"bounded by what is hers and not by what the layer holds"*.
  - Note why `line-by-who` cannot simply be skipped for current holders at write time: if Ben writes 500 acts while holding permission in `asha-draft` and Asha later ends Ben's permission, Ben becomes `X` in `asha-draft` and `SPEC.md:122` promises him his own things *"after her permission there ended"*. (Only the layer's first holder under `:first-permission-does-not-end` can never become `X`, and omitting first-holder rows would bake `:first-permission-does-not-end` into stored index state if layer transfer [`MODEL.md:358`] is ever built.)
  - Second ground omitted by `PLAN` (`HANDOFF.md:66`): `line-by-who` also enables $O(\text{sender's acts})$ lookup for Sid's second use of 12 October (*"to see what a sender in bad faith does"*), **if** a future query exposes filtering by `:who`.

**Concrete cost vs constructed alternatives (side-by-side):**

| Design | Write & storage cost (forever) | PState partitions / task | Outside asker `X` read cost ($k$ own decisions) | What changes in the contract |
|---|---|---|---|---|
| **Plan (`line-by-who` `{String Long}`, own PState)** | **+1 RocksDB write** (~60–120 B × 3 replicas + compaction) on **100% of decisions** with usable `:who` across every layer (~31.5B rows / ~2.5–3.8 TB/yr at 1,000 acts/s) | **+2** (`$$streaming-line-by-who`, `$$microbatch-line-by-who`) | 1 range seek + $k$ iter-reads on `line-by-who`, **+ $k$ random point seeks on `line` + up to $k$ random point seeks on `things` = $1 + 2k$ seeks** (~100 ms at $k = 100$!) | Meets `SPEC.md` "from a stamp on" for `X` without external state. |
| **Alt C1 (Subindex inside `$$task`: Road K applied to `:by-who` only)** | Same +1 RocksDB write per decision (`{:subindex-options {:track-size? false}}`) | **0 new PStates** (reuses `$$streaming-task`, `$$microbatch-task`, saving 2 RocksDB PState partitions per task!) | **2 seeks** + $k$ iter-reads on `$$task` `:by-who` (only **+1 seek = +0.5 ms** on a rare non-live read!) + $2k$ seeks on `line`/`things`. Note: `X` never opens a live read on a closed layer (`SPEC.md:185`), so top-level `foreign-proxy` is NEVER needed on `line-by-who`! | Zero contract change! Strictly saves 2 PState partitions per task for +0.5 ms on a rare read. |
| **Alt C2 (Road J: No `line-by-who`; outside asker names stamps from door answers / `Trails`)** | **0 writes, 0 bytes** | **0 new PStates** | **$2k$ point seeks** (`k` on `line` checking `(:who row) == X`, up to `k` on `things`) — actually **1 fewer seek** than the plan when stamps are known! | Requires Sid to turn Q1's access pattern so an outside asker queries named stamp(s) (which the door's answer [`SPEC.md:277`] and runtime `Trails` [`PROPOSAL.md` entry 3] already record) rather than scanning from stamp 0 inside `Store`. |

**Ruling on `c`:**
1. The plan's rejection of Road K for `line-by-who` (`PLAN:251–252`, *"Close for the rows by who; a session's choice"*) **fails** `artifact-plan-validation.md` Minimality & Partitioning rules: `line-by-who` is never watched live (`O5` leaves a layer when permission ends) and is read only by outside askers (`5%` in the plan's table, $<0.1\%$ in practice), so paying 2 dedicated PState partitions on every task to save 1 seek (0.5 ms) on that rare read is unjustified overhead unless folded into `$$task` (Alt C1), AND the plan's Partitioning table hid the **$1 + 2k$ random point seeks** that `line-by-who` incurs.
2. Whether to keep `by-who` (folded into `$$task`, Alt C1) vs drop it so outside askers read by named stamps from door/Trails (Alt C2 / Road J) remains **a session's default, marked for Sid to turn** (default: keep `:by-who` as a subindexed map in `$$task` with `:track-size? false` so `Store` meets `SPEC.md` standalone without coupling to `Trails` thinning; turn to Road J if Sid prefers zero per-decision write overhead).

---

### d. Trace of the plan's claim that `IMPLICIT_SPEC.md` S4 cannot be met as written

**Verbatim quote (`IMPLICIT_SPEC.md` S4, `lines 253–257`):**
> "- **S4. Derived**, from `declaration/rules`: 'the order of the declarations is the order of the reasons'. Where the new rule stands among the rules decides the word of a thing with two faults. The place is chosen so that no thing that is refused today gets another word, except a thing whose only fault was this key."

**End-to-end trace through `src-store/softland/store/door/declaration.clj` and `gate/decision.clj`:**
In `declaration/check*` (`declaration.clj:748–754`), checks execute in strict order:
1. `act/as-shape shapes :arrived-act thing` (`declaration.clj:362–366`, `act.clj:171–191`). `:softland.store/who-may-read-acts` is a valid `:key` (readable keyword), so any fact `{:e e :k :softland.store/who-may-read-acts :v v}` with valid `:id` `e` and `act/value?` `v` passes shape checking.
2. `declaration/rules` (`declaration.clj:397–415`), evaluated in vector order:
   - `[0]` `:permission-is-in-the-acts-layer` $\to$ `:permission-not-in-layer`
   - `[1]` `:depot-is-the-act-ids-depot` $\to$ `:depot-not-the-act-ids-depot`
   - `[2]` `:facts-within-the-limit` $\to$ `:too-many-facts`
   - `[3]` `:based-on-within-the-limit` $\to$ `:too-many-based-on`
   - **`[4]` `:store-key-is-known` $\to$ `:store-key-not-known`** (`declaration.clj:401, 576–579`): tests `(or (not (in-store-namespace? (:k fact))) (store-key? (:k fact)))`.
   - `[5..18]` value-shape rules for `:depot`, permissions, `:who-may-see`, `:builds-on`, `:marked-keys`, `:place-named-once`, `:no-fact-replaces-a-fact-of-its-act`, `:ids-within-the-limit`.
3. `with-texts` (`declaration.clj:720–742`): `:value-too-large`, `:act-too-large`.
4. `decision/standing` and `decision/places` (`decision.clj:294–332, 580–589`): `:layer-not-made`, `:permission-does-not-stand`, `:setting-not-on-the-layer`, `:only-the-first-holder-writes-a-setting`, `:store-key-retract-not-built`, etc.

**Concrete counterexample proving S4 is unsatisfiable under ANY placement of the new rule:**
Consider arrived thing `T` in made layer `:alice`:
```clojure
{:act-id [:alice :streaming #uuid "00000000-0000-0000-0000-000000000001"]
 :depot :streaming  :door-time 1000  :version 1
 :who :alice  :layer :alice  :permission [:alice :alice :alice]  :session :s1
 :facts [{:e :alice :k :softland.store/who-may-read-acts :v :everyone}
         {:e :other :k :softland.store/who-may-see :v :invalid}]}
```
- **Today (Piece 2):** `T` passes shape and rules `[0..3]`. At rule `[4]` (`:store-key-is-known`), Fact 0 has namespace `"softland.store"` and is not in `store-keys`, so **`T` is refused `:store-key-not-known`**. Yet `:who-may-read-acts` is **not** `T`'s only fault (Fact 1 has both `:invalid` value and wrong entity `:other`).
- **After O2/S5 (`:softland.store/who-may-read-acts` added to `store-keys`):** Rule `[4]` (`:store-key-is-known`) **passes** on Fact 0, AND because Fact 0's value `:everyone` is valid, the new rule `:who-may-read-acts-fact-holds-its-word` **also passes on Fact 0 regardless of where in `rules` it is placed**! `T` therefore proceeds to rule `[10]` (`:who-may-see-fact-holds-its-word`) on Fact 1 and is refused **`:who-may-see-fact-not-members-or-everyone`** instead of `:store-key-not-known`.
- Even simpler: take `T'` with a single fact `{:e :other-entity :k :softland.store/who-may-read-acts :v :everyone}`. Today `T'` is refused `:store-key-not-known` (rule `[4]`); after Piece 3, `IMPLICIT_SPEC.md` **S3 itself** (`lines 243–245`) requires `T'` to be refused **`:setting-not-on-the-layer`** (at `decision/places` check `(f)`), contradicting S4's literal text!

**What DOES hold (verified across all callers of `store-keys`):**
1. For **every** thing that does **not** contain a fact with `:k :softland.store/who-may-read-acts`, zero verdicts and zero refusal words change anywhere in `declaration.clj` or `decision.clj` (even `:marked-keys-are-plain-keys` checks `in-store-namespace?`, not `store-keys`, so `{:k :softland.store/marked-keys :v #{:softland.store/who-may-read-acts}}` keeps `:marked-key-in-the-stores-namespace`).
2. For any thing that **does** contain `:softland.store/who-may-read-acts` and also violates `shapes` or rules `[0..3]`, its refusal word is unchanged.
3. Placing `:who-may-read-acts-fact-holds-its-word` right behind `:who-may-see-fact-holds-its-word` (`PLAN:550–552`) matches the existing setting-rule order in `declaration/rules`.

**Ruling on `d`:** **Confirmed.** The plan's proof that IMPLICIT S4 cannot be met literally is exact.

---

### e. Is the new `weight` measure needed, or can the existing `door/text.clj` count serve?

**1. Can `door/text.clj` (`text/utf8-count` of `text/text`) serve alone? NO — broken by 6 concrete inputs:**
`text/feed` (`src-store/softland/store/door/text.clj:191–207`) intentionally collapses non-value types and deep nesting because no valid act contains them. A declaration-refused thing *can* contain them (`SPEC.md` "What must hold" 4: *"A thing can be anything"*). Under `text/text` alone, each of the following 500 MiB maps has a `text/utf8-count` under **250 bytes**, so the gate would treat it as tiny, write 500 MiB into `$$things` (risking a partition-stopping throw, `W6`), and Stage 2 would fetch 500 MiB believing it weighs < 250 bytes (`R15`, `R16` broken):
1. **Java `List`/`Set`/`Map`:** `{:act-id [:a :streaming u] :layer :a :x (java.util.ArrayList. [<500 MiB String>])}` — `(sequential? ArrayList)` is `false` in Clojure; `text/text` emits `"#other java.util.ArrayList"` (26 B).
2. **`defrecord`:** `{:act-id [:a :streaming u] :layer :a :x (->MyRec <500 MiB String>)}` — `feed*` line 201 emits `"#other ...MyRec"` (~20 B).
3. **Non-`byte[]` arrays:** `{:act-id [:a :streaming u] :layer :a :x (int-array 134217728)}` — `(bytes? int[])` is `false`; `text/text` emits `"#other [I"` (9 B) for a **512 MiB** array.
4. **Depth $> 64$:** `(reduce (fn [v _] [v]) <500 MiB String> (range 65))` — `feed*` line 197 stops at `max-depth` (`64`) and emits `"#too-deep"` (9 B).
5. **Large `Symbol` / `Ratio`:** `(symbol <500 MiB String>)` — `scalar-text` emits `"#other clojure.lang.Symbol"` (26 B).
6. **`BigDecimal` trailing-zero compression + CPU bomb:** `(BigDecimal. (.pow BigInteger/TEN 2000000) 2000000)` — `text.clj:142` calls `.stripTrailingZeros`, emitting `"1M"` (**2 bytes**) for an ~830 KB `BigInteger` payload while burning **>10 seconds of CPU** on the task thread (reproduced locally on `:store-module`!). Even worse, `act/normal-value` (`act.clj:350`) calls `.stripTrailingZeros` too!

**2. However, the plan's new `weight` specification (`PLAN:321–338, 373`) has three defects of its own:**
- **Defect E1 — Arrays bypass `weight` via line 328:** `PLAN:326–328` specifies: *"Where the text stops (a record, a Java map or collection, an array, a symbol, a ratio, a float, a character, a part more than 64 collections deep), the weight goes on: a record as the map of its fields and its class's name, a map or a collection as its parts, **the others as their printed text**."* Java arrays (`int[]`, `long[]`, `double[]`, `Object[]`) are neither records, maps, nor collections; in Clojure `(str (int-array 134217728))` / `(pr-str ...)` is ~38 bytes (`"#object[\"[I\" ...]"`), **not** its elements! Primitive arrays (`byte[]`, `int[]`, `long[]`, `double[]`, `float[]`, `short[]`, `char[]`, `boolean[]`) must add `alength × element-bytes` in $O(1)$ (and `byte[]` must count `alength`, not `2 × alength` hex text), and `Object[]` must push its elements onto the explicit stack.
- **Defect E2 — Unbounded traversal on `:too-heavy` inputs (`PLAN:373`):** Line 373 states that for a thing over `max-kept-weight`, *"Its line row says `:too-heavy` and holds its weight."* Computing exact `:weight` on a 300 MiB refused thing forces `weight` to keep walking all 300 MiB on the non-yielding task thread after crossing 32 MiB at byte `33,554,433`. Neither `SPEC.md` nor `IMPLICIT_SPEC.md` (`R10`, `R15`) requires `:weight` on a `:too-heavy` row. `weight` must accept `max-weight` and abort immediately when `running > max-weight`, leaving `:weight` absent when `:not-kept` is `:too-heavy` (just as when `:not-kept` is `:not-weighed`).
- **Defect E3 — Unnecessary second full traversal on 100% of ordinary acts (`PLAN:336–337, 625`):** In `walk.clj:338`, `digest/digest` already walks the entire arrived `*thing` via `text/feed` before `declaration/check`. For **100% of taken acts, 100% of gate-refused acts, and every declaration-refused act that contains no `#other`, no `#too-deep`, and no unstripped `BigDecimal`/`BigInteger`**, `text/feed` already visits every piece! Counting UTF-8 bytes + `(alength ^bytes b)` inside that single `digest` pass yields the exact weight in **zero additional walks**, invoking the explicit-stack `weight` walker **only** when `text/feed` hits `#other` or `depth > max-depth` (and bounding `BigInteger`/`BigDecimal` by `(.bitLength unscaled) / 8` BEFORE calling `.stripTrailingZeros` or `.toString`!).

---

## Full Phase 2 Checklist (`artifact-plan-validation.md`)

### Query topology: none in this stage
- Input examples present: N/A (`DECOMPOSITION.json` assigns O3/O4 reads to stage 2 `read-the-line-by-stamp`; stage 1 adds no query topology and leaves `answer-by-act-id` and `depot-of-layer` untouched, `PLAN:459–462`).
- **Verdict**: PASS

### PState schemas
- **Group by `(key type, partitioner)`:**
  Each gate (`streaming-gate` and `microbatch-gate`) owns six `{String ...}` top-level map PStates colocated on the layer's task (`streaming`: all six on layer's task; `microbatch`: `answers`, `line`, `things`, `line-by-who` on base's task alone, `facts` and `facts-by-value` copied to `|all`), plus one `fixed-keys-schema` PState (`task`) on the layer's task:
  1. `answers` (`"<layer> <act-uuid>"` $\to$ `AnswerRow`), `facts` (`"<layer> <entity> <key>"` $\to$ `FactRow`), `facts-by-value` (`"<layer> <key> <since> <val-digest> <entity>"` $\to$ `ByValueRow`), `line` (`"<layer> <19-digit-stamp>"` $\to$ `LineRow`), `line-by-who` (`"<layer> <who> <19-digit-stamp>"` $\to$ `Long`): these five have **distinct key structures** (different arities and sort orders so range scans do not interleave).
  2. **`line` and `things` (`"<layer> <19-digit-stamp>"` $\to$ `LineRow` vs `IPersistentMap`):** share the **exact same key type, key string, and partitioner** (`PLAN:303–310, 838–839`). Merging them into `{String (fixed-keys-schema {:row LineRow :thing IPersistentMap})}` (Road E) would serialize `:row` and `:thing` as a single RocksDB value (`pstate-schema.md:46, 172`), forcing every withheld visitor scan and byte-bounded scan to load up to $100 \times 32\text{ MiB}$ from disk and violating `IMPLICIT_SPEC.md` R16. That physical I/O separation is sound.
  3. **`line-by-who` vs `$$task` (`PLAN:245–252`):** `$$task` already groups per-task structures (`:last-stamp`, `:strays`) into one `fixed-keys-schema` PState explicitly *"so that they cost one partition and not two"* (`record/tables.clj:37–38`). Declaring `$$streaming-line-by-who` and `$$microbatch-line-by-who` as separate PStates adds 2 PState partitions per worker/task solely to save 1 RocksDB seek on a rare outside-asker read that is never watched live (see Item `c`). **FAIL** — fold `:by-who` into `$$task` as `(map-schema String Long {:subindex-options {:track-size? false}})` (or justify on weighted throughput arithmetic why 2 extra PState partitions across all tasks beat +1 seek on 5% / <0.1% of reads; `PLAN:251` merely says *"Close for the rows by who; a session's choice"* without weighted seek math).
- **Any `Object` type?** Top-level value of `things` is `clojure.lang.IPersistentMap` (`PLAN:437`), whose inner entries are arbitrary untyped objects (`PLAN:316–319, 834–836`). Literally, unparameterized `IPersistentMap` bypasses inner schema typing just as `{Object Object}` does; however, `SPEC.md` "What must hold" 4 mandates keeping raw refused maps (*"A thing can be anything... a refused thing is kept as it arrived"*). If Road A is used, `IPersistentMap` is the narrowest JVM interface matching non-stray things (`plain-map?`). (Note: `pstate.maximal.schema.validations` is `true` by default in dev/IPC [`pstate-schema.md:200`]; because `IPersistentMap` is a leaf class reference, Rama validates `instance? IPersistentMap` in $O(1)$ without walking inner elements.)
- **Uniform record-like values use `fixed-keys-schema`?** Yes (`line` uses `fixed-keys-schema`).
- **Polymorphic positions use `definterface + defrecord`?** `line` represents a single decision summary with nullable `:refused`, `:time`, `:who`, `:weight`, `:not-kept` fields (same pattern as existing `answers` and `facts` in `tables.clj`).
- **Inner collections > 100 elements subindexed?** Inside `things`, an arrived map can hold 1,024 `:based-on` entries, 256 `:facts`, or arbitrary collections in a refused thing without subindexing, because `things` is always read and written as a **whole map** (`SPEC.md`: *"the thing whole, as it arrived"*), never navigated into on disk.
- **Section Verdict**: **FAIL** (unjustified separate PState partitions for `line-by-who` vs subindexed `:by-who` in `$$task`).

### Partitioning
- **Write partitioners:** Every write in `walk/decide` executes locally on the depot partition's task (`(hash-by :layer)` on `*streaming-depot` and `*microbatch-depot`, `module.clj:112–113`), with no `|all` copy added for `line`, `things`, or `line-by-who`.
- **`|hash` hot-key skew check (`artifact-plan-validation.md:24`):** Can a single `:layer` key take a disproportionate share of events or storage? **Yes** — `:base` puts 100% of microbatch decisions and up to 32 MiB blobs on 1 task (`PLAN:485`), and a busy shared streaming layer puts its entire unbounded history of 32 MiB `things` on 1 task (`PLAN:487–495, 841–844`). While per-layer decision ordering is fixed by Sid's decision 3 of 13 October (`"Every act is placed by its layer key, the base too"`), storing 32 MiB **unpermissioned refused blobs** on that single hashed task aggravates hot-key storage and stall skew unnecessarily (Item `b`).
- **`## Partitioning efficiency` table check (`PLAN:477–484` vs `artifact-plan-validation.md:28–33`):**
  - Filled for $N = 1, 16, 128$? Columns exist, **but report `"Tasks read"` (`1, 1, 1`), NOT RocksDB `seeks/op`!**
  - Proportions sum to 1? `0.60 + 0.25 + 0.10 + 0.05 = 1.00` (yes).
  - Weighted sums `Σ(proportion × seeks)` and `Σ(proportion × iterator-reads)` computed for each $N$? **NO — FAIL.** `"Tasks read"` is not RocksDB seeks, and `"Steps"` contains prose (`"100 rows and up to 100 things"`, `"her rows by who, then each row and thing"`) with **no numeric `iterator-reads/op` and no weighted sum**.
  - **Actual RocksDB seeks/op when recounted across all tasks** (for a streaming layer with a depth-2 permission chain, page size 100, $m = 5$ own things for a visitor, $k = 20$ own things for an outside asker):
    - Row 1 (0.60, holder page of 100): `own-rows` (1 seek) + `permission` + 1 parent (2 seeks) + `:last-stamp` (2 seeks) + `line` range (1 seek, 100 iter) + `things` range (1 seek, $\le 100$ iter) = **7 seeks, 200 iter-reads** (flat across $N = 1, 16, 128$).
    - Row 2 (0.25, 1 decision by stamp): `own-rows` (1) + `permission` + parent (2) + `:last-stamp` (2) + `line` point (1) + `things` point (1) = **7 seeks, 0 iter-reads** (flat across $N$).
    - Row 3 (0.10, visitor page of 100, $m = 5$ hers): `own-rows` (1) + `permission` (1) + `:last-stamp` (2) + `line` range (1 seek, 100 iter) + $m = 5$ point seeks on `things` = **10 seeks, 100 iter-reads** (flat across $N$).
    - Row 4 (0.05, outside asker's own things, $k = 20$ hers): `own-rows` (1) + `permission` (1) + `line-by-who` range (1 seek, 20 iter) + **20 point seeks on `line` + 20 point seeks on `things`** = **43 seeks, 20 iter-reads** (or **203 seeks** at $k = 100$!).
    - Recounted weighted totals (flat across $N = 1, 16, 128$): $\Sigma(\text{prop} \times \text{seeks}) = 0.60(7) + 0.25(7) + 0.10(10) + 0.05(43) = \mathbf{9.10\text{ seeks/op}}$; $\Sigma(\text{prop} \times \text{iter}) = 0.60(200) + 0.25(0) + 0.10(100) + 0.05(20) = \mathbf{131.0\text{ iter-reads/op}}$.
- **Section Verdict**: **FAIL** (`PLAN:477–484` omits RocksDB seek counts and iterator-read sums, hiding Row 4's $1 + 2k$ point-seek cost).

### Topologies
- **Microbatch unless justified?** Existing `microbatch-gate` (for `:base`) and `streaming-gate` (for interactive `:ack`-coordinated streaming layers, `streaming_gate.clj:51–55`). No topology added (`PLAN:403`).
- **Low-latency writes in stream?** Streaming layer decisions require `:ack` visibility with `answer-row` (`IMPLICIT_SPEC.md` O1 Latency, W3); handled in `streaming-gate`.
- **Does `streaming-gate` handle any concern not requiring stream?** Wait: `IMPLICIT_SPEC.md` W3 (`lines 146–150`) explicitly requires `line` and `things` to become visible **atomically with the answer and `:last-stamp`** so no reader ever observes `:last-stamp = s` with no decision at `s` (`Road D` in `PLAN:178–187` traces why an async microbatch/second topology violates W3 and cannot disambiguate duplicate-digest arrivals). Thus all three writes require colocation in the deciding event.
- **Any choice made on test-synchronization grounds?** No.
- **Section Verdict**: PASS

### Production readiness
- **Multiple concurrent clients?** Yes — per-layer depot partitioning serializes decisions per layer on its task (`module.clj:64–70`).
- **Client process restart?** Yes — stateless client appends with client-generated `act-id`; repeats hit check (a).
- **Worker process restart at any point?** Yes — all PStates are durable RocksDB; single-task writes commit atomically per streaming batch or microbatch attempt (`streaming_gate.clj:44–49`, `microbatch_gate.clj:92–99`).
- **Large scale (millions of entities, unbounded growth) & bounded task-thread execution?** **FAIL.**
  1. Unpermissioned senders can write unbounded sequences of 32 MiB refused blobs into `things` on shared tasks (`Item b`).
  2. `weight` does not short-circuit at `max-kept-weight + 1` (`PLAN:373`), allowing a single 300 MiB refused thing to hold the task thread for hundreds of milliseconds in `weight` (`Item e`).
  3. `weight` misclassifies Java arrays as `"printed text"` (`PLAN:328`), allowing a 500 MiB `int[]`/`Object[]` to pass `weight <= 32 MiB` and either blow worker memory or fail PState serialization/replication in a fatal retry loop (`Item e`, `W6`).
  4. Unstripped `BigDecimal` / huge `BigInteger` values can stall `text/text` / `act/normal-value` in base-10 conversion / `.stripTrailingZeros` for >10 seconds on a single ~1 MB payload (`Item e`).
- **Stream topology non-idempotent writes:** Every added write in `streaming-gate` uses `[(keypath *k) (termval *v)]` behind check (a) (`$$streaming-answers`). If retried after commit, check (a) short-circuits (`walk.clj:331–333`); if retried after discard, `termval` writes the identical deterministic key/value.
- **Multi-partition stream writes:** None (`streaming-gate` never hops tasks; `microbatch-gate` hops `|all` only for `:fact` and `:by-value`, which are unchanged and atomic per microbatch).
- **Section Verdict**: **FAIL** (due to unbounded refused-blob write cost and `weight` array/cutoff/BigDecimal hazards on shared single-threaded tasks).

### Internal depot usage
- None declared or used (`PLAN:398`).
- **Verdict**: PASS

### Cross-topology correctness
- No internal depot flows between topologies (`PLAN:398–400`).
- **Verdict**: PASS

### Stream topology correctness
- No `depot-partition-append!` used anywhere (`PLAN:399`).
- **Verdict**: PASS

### In-memory state efficiency
- No `TaskGlobal` or in-memory cache added (`PLAN:504, 680`). (Temporary stack in `weight` is per-call garbage, which is another reason `weight` MUST cap traversal at `max-kept-weight + 1` so an explicit stack on a 10M-element structure cannot spike GC pauses on the task thread!)
- **Verdict**: PASS (conditional on capping `weight`'s stack/traversal at `max-kept-weight + 1`).

---

## Minimality — adversarial simplification

**Simplest plausible design sketch (3 sentences):**
Keep `answers`, `facts`, `facts-by-value`, and `task` as today, add one top-level PState `line` (`{String LineRow}`) and one top-level PState `things` (`{String IPersistentMap}`) per gate on the layer's task, and fold `:by-who` (`"<layer> <who> <19-digit-stamp>"` $\to$ `Long`) as a subindexed map with `:track-size? false` into `$$task` alongside `:last-stamp`. Compute byte weight *during* Step 2's existing `digest/digest` (`text/feed`) walk with a hard cutoff at `bound + 1` (256 KiB for non-standing/declaration refusals, 32 MiB for accepted/standing-member acts), falling back to an array-aware iterative walker only when `text/feed` encounters `#other` or `#too-deep`. Add `:softland.store/who-may-read-acts` to `store-keys` and `starts-with`.

Diffing `PLAN-gates-keep-the-line.md` against this sketch, mechanism by mechanism:

### `$$streaming-line` & `$$microbatch-line` (PStates)
- **Delete it**: Breaks `SPEC.md` "The read" (*"For one layer, the decisions from a stamp on, in the layer's order"*) and `IMPLICIT_SPEC.md` R16 (*"the cost of that read is said, and it does not grow with the bytes of what is withheld"*): `answers` is keyed by random UUID (`"<layer> <uuid>"`), and `things` holds multi-megabyte maps that cannot be scanned just to emit withheld stamps.
- **Merge or bypass it**: Cannot merge with `things` (Road E loads up to 32 MiB per withheld row, breaking `R16`). Putting `line` inside `$$task` (Road K) adds 1 extra RocksDB seek on 95%+ of reads and prevents top-level `sorted-map-range` live proxy if Stage 3 offers `O5` (`tables.clj:27–29`). **PASS.**

### `$$streaming-things` & `$$microbatch-things` (PStates)
- **Delete it**: Breaks `SPEC.md` "The read" (*"and the thing whole, as it arrived on the depot: the arrival that was decided"*), because `streaming-gate` cannot maintain a retry-safe depot offset across strays/repeats and `foreign-depot-read` is non-yielding (Item `a`).
- **Merge or bypass it**: Cannot merge into `line` without breaking `R16`. **PASS.**

### `$$streaming-line-by-who` & `$$microbatch-line-by-who` as dedicated PStates
- **Delete it**: Deleting the by-who index entirely (Road J) breaks `SPEC.md` ("After piece 2", `lines 273–278`: *"An asker who may not see a layer is given her own things there. What that read costs must be bounded by what is hers and not by what the layer holds"*) when an outside asker queries "from stamp 0 on" without already holding her stamps.
- **Merge or bypass it**: **CAN be merged into `$$streaming-task` and `$$microbatch-task`** as `:by-who (map-schema String Long {:subindex-options {:track-size? false}})`, just like `:last-stamp` (`tables.clj:37–40, 168`)! Constructed side-by-side comparison:
  - *Plan (2 dedicated PStates):* +2 PState partitions on every task (permanent RocksDB/memory overhead on all $N$ tasks); write cost = 1 RocksDB write; outside-asker read cost (5% share, $k = 20$ rows) = $1 + 20 + 20 + 2 = 43$ seeks ($0.05 \times 43 = 2.15$ weighted seeks).
  - *Merged into `$$task` (`:by-who` subindexed, `:track-size? false`):* **0 new PStates**; write cost = 1 cached `$$task` root read + 1 RocksDB write; outside-asker read cost = $2 + 20 + 20 + 2 = 44$ seeks ($0.05 \times 44 = 2.20$ weighted seeks, a +0.05 weighted-seek difference — 25 microseconds — on a non-live read, since `O5` never runs for an outside asker who cannot see the layer!).
  - **Verdict**: **FAIL** — fold `:by-who` into `$$task` (or prove with memory vs seek numbers why 2 extra PStates per task are warranted).

### Standalone `weight` namespace + second full walk (`PLAN:321–338, 789`) + `:weight` on `:too-heavy` rows (`PLAN:373`)
- **Delete it**: Cannot delete byte-bounding completely without breaking `SPEC.md` "What must hold" 4 (*"What a read gives back of it must not throw, and must be bounded"*) and `IMPLICIT_SPEC.md` W6/R15/R16 (Item `e`).
- **Merge or bypass it**:
  1. **Exact `:weight` on `:too-heavy` rows (`PLAN:373`):** Deleting `:weight` when `:not-kept` is `:too-heavy` loses **zero** spec properties (`R10` and `R15` ask only that `:too-heavy` be told apart from withheld and give stamp, `act-id`, and answer) while bounding `weight`'s CPU/stack work at `max-kept-weight + 1` instead of $O(\text{unbounded thing})$! **FAIL — remove exact `:weight` for `:too-heavy` and short-circuit at `bound + 1`.**
  2. **Separate full walk on ordinary acts (`PLAN:336–337, 625`):** Merging byte-accumulation into Step 2's `digest/digest` (`text/feed`) eliminates an entire object-graph walk on 100% of taken acts and gate-refused acts, invoking the deep iterative walker only if `text/feed` hits `#other` or `#too-deep`. **FAIL — fuse common-case counting with `digest` instead of walking twice.**

---

## Throughput — adversarial

**Frequent operation:** `O1` (`walk/decide` on every arrived thing in `streaming-gate` and `microbatch-gate`), target 1,000 acts/sec per task (`src-store/README.md`, 7 October entry 9).

- **Plan's write-path CPU & I/O per accepted 1 KB act:**
  1. `digest/digest`: 1 full `text/feed` traversal of `*thing` (allocating UTF-8 byte chunks per token, `digest.clj:96`).
  2. `declaration/check`: `conform` + `with-texts` (`text/text` + `utf8-count` on every fact value).
  3. **`weight` (added by plan):** a **second full traversal** of `*thing` on an explicit stack (`PLAN:332–337`).
  4. `usable-who`: map lookup + `usable-id?`.
  5. PState reads: 5; PState writes: 6 (`fact`, `line`, `things`, `line-by-who`, `answer`, `last-stamp`).
- **Lower-aggregate-cost design meeting the exact same latency & spec targets:**
  1. **Single-pass `digest-and-weight` in `door/digest.clj`:** During `digest`'s `text/feed` over `(without-the-time thing)`, accumulate `(+ utf8-bytes)` on `take-text` (using `text/utf8-count` or byte length already produced for `.update md`!), `(+ (alength b))` on `take-bytes`, plus the UTF-8 byte count of `:door-time` if present. Set a boolean flag `needs-deep-weight?` only if `feed*` hits `#other` or `depth > max-depth` (or `BigDecimal` before `.stripTrailingZeros`).
     - **Side-by-side cost:** For 100% of valid acts and 99.9% of refusals (`needs-deep-weight? == false`), extra object-graph traversals drop from **1 full walk (100% overhead on traversal) to 0 walks (3 integer additions per token)**! On a 20 MiB valid act, this saves tens of milliseconds of task-thread CPU and heap allocation per act.
  2. **Short-circuit `deep-weight` at `bound + 1` + $O(1)$ primitive-array sizing (`alength * width`):** Drops worst-case refusal CPU walk from unbounded (hundreds of MB / seconds) to strictly $\le \text{bound} + 1$ bytes (~256 KiB under Option 2 = <1 ms!).
  3. **In Stage 2 preview (`Partitioning efficiency` Row 4):** Because `line-by-who` stores only `Long` (`stamp`), reading $k = 100$ decisions for an outside asker costs **201 seeks** ($1$ range + $100$ `line` + $100$ `things`). Note that storing `{:act UUID :digest String :refused Keyword :time Long :weight Long :not-kept Keyword}` in `line-by-who` would cut Row 4 from 201 seeks to $1 + 100 = 101$ seeks, **at the cost of +100 bytes written on 100% of decisions**. At realistic call rates ($<0.1\%$ outside-asker scans vs $100\%$ writes), keeping `:by-who` values as `Long` (8 bytes) wins on aggregate throughput — **provided** the Partitioning table states the 43–201 seeks honestly instead of reporting `"1"`.
- **Verdict**: **FAIL** (redundant second full walk on every act when `digest` already walks `*thing`, plus missing early cutoff at `bound + 1`).

---

## Spec Coverage — Scenario-by-Scenario Trace

Stage 1 (`gates-keep-the-line` in `DECOMPOSITION.json`) owns: `O1` (`W1`–`W7`), `O2` (`S1`–`S5`), `O6` (`A1`–`A10`), `SPEC.md` "What must hold" 1–6 and 8, "What exists and must come out unchanged" (door read by act-id, strays, decision cost), and the `door` lines of `E1`–`E5`. Every item is traced below.

### 1. `O1` / `SPEC` "The read" & "What must hold" 1 (`W1`, `W2`): Deterministic decision state from thing, rows read, and stamp
- **Source** (`SPEC.md:152–156`, `IMPLICIT_SPEC.md:137–145`): *"Whatever a gate comes to keep for this read is made of the thing, the rows read and the stamp, and of nothing else."*
- **Trace through the plan**: Layer `:asha`, current `:last-stamp` = `41`. Act `[:asha :streaming u42]` arrives with `:who :asha`, `:door-time 1700000000123`, 1 fact of 120 bytes. `walk/decide` passes `stray-reason` and check (a), computes `digest` (32 B), `check`, `weight` (e.g. 312 B), `usable-who` (`":asha"`), `standing`, `places`, reads `:last-stamp` (`41` $\to$ `stamp = 42`), and writes:
  - `line` at `":asha 0000000000000000042"` $\to$ `{:act u42 :digest "<64-hex>" :time 1700000000123 :who ":asha" :weight 312}`
  - `things` at `":asha 0000000000000000042"` $\to$ the arrived map `*thing`
  - `line-by-who` at `":asha :asha 0000000000000000042"` $\to$ `42`
  - `facts`, `answers`, `:last-stamp` (`42`).
  No clock, random source, task ID, batch ID, or depot offset is read (`PLAN:440–447`).
- **Fault-tolerance check**:
  - *Worker restart*: All PStates are RocksDB-backed; no in-memory state exists (`PLAN:680`). `:last-stamp` = `42` and all rows survive intact.
  - *Topology retry*: In `streaming-gate`, post-commit retry hits check (a) (`$$streaming-answers` holds `":asha <u42>"`) and writes nothing (`walk.clj:331–333`). Pre-commit retry or `microbatch-gate` attempt rollback recomputes identical keys and `termval` values.
  - *Multi-partition partial failure*: All writes of `O1` happen on the layer's single task (`PLAN:418, 425`); `microbatch-gate`'s `|all` copy only copies `:fact` and `:by-value` (`PLAN:427–428`), which commit atomically per microbatch.
- **Race-condition check**:
  - *Two concurrent clients writing `:asha`*: Both append to `*streaming-depot` partition `hash(:asha) mod N`; single-threaded task processes them sequentially without yielding (`walk.clj:215`).
  - *Out-of-order partitioner hops*: Zero partitioners before or inside `walk/decide`.
- **Flaws found**: none found, with reasoning: every field of `line`, `things`, and `line-by-who` is a pure function of `*thing` and `*stamp`, committed in the same single-task batch as `answers`.
- **Verdict**: PASS

### 2. `W3` (`IMPLICIT_SPEC.md:146–152`): Atomic visibility with answer and `:last-stamp`
- **Source**: *"Whatever is kept is written behind check (a) and becomes visible together with the answer and the layer's last stamp, on the same task... Then no reader ever sees a last stamp s for which O3 has no decision at s, and a stream event that Rama runs again after a commit writes nothing a second time."*
- **Trace through the plan**: On task 2, `decide` runs in one synchronous stretch (`PLAN:440–446`), writing `:fact`, `:by-value`, `:line`, `:thing`, `:by-who`, `:answer`, and `:last-stamp` on task 2 with no partitioner or yield between them. Both `streaming-gate` (per-event/batch commit on task 2, `stream.html.txt:108`) and `microbatch-gate` (per-task commit phase, `microbatch.html.txt:93`) expose all PState updates on task 2 atomically.
- **Fault-tolerance check**: Worker restart discards uncommitted batch writes across all PStates on task 2 together; retry after commit short-circuits at check (a); no multi-partition write is involved for the new tables.
- **Race-condition check**: Concurrent foreign reader on task 2 runs between events (`pstates.html.txt:192`) and sees either pre-event state (`:last-stamp` $s-1$, no stamp $s$) or post-commit state (`:last-stamp` $s$, `line` and `things` at $s$ present).
- **Flaws found**: none found, with reasoning: Rama commits all `local-transform>` writes within a non-partitioning event on one task atomically.
- **Verdict**: PASS

### 3. `W4` & `SPEC` "What must hold" 6 & `R9` (`SPEC.md:178–181`, `IMPLICIT_SPEC.md:153–155, 382–390`): Duplicate `act-id` keeps first arrival and first `:door-time`
- **Source**: *"One act-id has one answer, that of the first thing decided under it. A second thing under a used act-id is in the depot and has no stamp of its own: it is not in this read, as a stray is not."*
- **Trace through the plan**: Arrival 1 under `[:alice :streaming u1]` has `:door-time 1000` (decided at stamp `5`; `things` at `":alice 0000000000000000005"` stores Arrival 1 with `:door-time 1000`). Arrival 2 under `[:alice :streaming u1]` has `:door-time 2000` (same digest) or different facts. At step 1 (`walk.clj:331`), `(local-select> (keypath ":alice <u1>") $$answers)` is non-nil, so `decide` emits `[]` immediately (`PLAN:444–446`), before `weight`, `usable-who`, or any `local-transform>`. Stamp `5` still holds Arrival 1 with `:door-time 1000`.
- **Fault-tolerance check**: Restart/retry preserves `$$answers` at `":alice <u1>"`, blocking Arrival 2 on every replay; single-task execution has no partial-write window.
- **Race-condition check**: Concurrent resends of `u1` queue in the same depot partition for `:alice`; the first in depot order wins check (a) and commits in memory visible to the immediately following event (`stream.html.txt:106`).
- **Flaws found**: none found, with reasoning: placing all three new writes behind check (a) preserves first-arrival wins identically to `answers`.
- **Verdict**: PASS

### 4. `W5` (`SPEC.md:81–82`, `IMPLICIT_SPEC.md:156`): Strays excluded
- **Source**: *"A stray is not in it. A stray has no answer and no stamp: a gate keeps its digest alone (`softland/store/module/walk.clj`, step 0)."*
- **Trace through the plan**: Thing `5` or `{:act-id [:alice :microbatch u] :layer :alice}` arrives on `*streaming-depot`. Step 0 (`walk.clj:318–322`) sees `*stray` non-nil, adds its digest to `$$streaming-task` `:strays`, and exits before check (a) or step 7 (`PLAN:444–446`). None of `line`, `things`, or `line-by-who` is written.
- **Fault-tolerance check**: `NONE-ELEM` set addition on `:strays` is idempotent across retries and restarts; single-task.
- **Race-condition check**: Sequential per partition; no cross-partition hops.
- **Flaws found**: none found, with reasoning: step 0 terminates the `<<if` branch before any new call.
- **Verdict**: PASS

### 5. `W6`, `W7`, & `SPEC` "What must hold" 3 & 4 (`SPEC.md:163–170`, `IMPLICIT_SPEC.md:157–170`): Walk never yields, never throws, and bounds work/writes on arbitrary arrived things
- **Source**: *"3. The walk never gives its task back between its first read and its last write... What this piece adds to a decision is bounded by the thing. 4. A thing can be anything. The door is not the only sender, and a refused thing is kept as it arrived. What a read gives back of it must not throw, and must be bounded"* / *"W6. No function that the walk calls may throw on what arrives... W7. no step that this piece adds may assume a size."*
- **Trace through the plan**:
  - *Scenario A (512 MiB `int[]` inside refused map)*: Sender appends `{:act-id [:alice :streaming u9] :layer :alice :blob (int-array 134217728)}` (~512 MiB). `stray-reason` = `nil`. `digest/digest` emits `"#other [I"` (9 B). `declaration/check` refuses `:unknown-part`. `PLAN:326–328` weighs arrays as *"the others as their printed text"* (~38 bytes $\le 33,554,432$). `walk/decide` attempts `(local-transform> [(keypath ":alice 000...009") (termval *thing)] $$streaming-things)` with a **512 MiB** value — bypassing `max-kept-weight`, stalling or OOMing/failing replication on every retry and wedging the partition!
  - *Scenario B (300 MiB string inside refused map)*: Sender appends `{:act-id [:alice :streaming u10] :layer :alice :blob <300 MiB String>}`. Because `PLAN:373` requires `:too-heavy` line rows to store exact `:weight`, `weight` walks all 300 MiB instead of stopping at `33,554,433` bytes.
  - *Scenario C (`BigDecimal` CPU bomb)*: Sender appends `{:act-id [:alice :streaming u11] :layer :alice :depot :streaming :door-time 1 :version 1 :who :alice :permission [:alice :alice :alice] :session :s :facts [{:e :x :k :y :v (BigDecimal. (.pow BigInteger/TEN 2000000) 2000000)}]}`. `text/text` and `act/normal-value` call `.stripTrailingZeros`, burning >10 seconds of CPU on the non-yielding task thread while reporting a 2-byte text `"1M"`.
  - *Scenario D (Stranger flooding 31.9 MiB refused maps)*: Unpermissioned sender appends twenty 31.9 MiB maps to `:alice`. Each passes `max-kept-weight`, stalling every other layer hashed to `:alice`'s task for ~12–30 seconds total (> `read_path` 10,000 ms `:wait-ms`) and writing ~640 MiB to RocksDB (2–6 GiB compacted).
- **Fault-tolerance check**: In Scenario A or C, if serialization/replication exceeds worker direct memory (`worker.max.direct.memory.size = 500m`, `all-configs.html.txt:37`) or `topology.stream.timeout.seconds` / `topology.microbatch.phase.timeout.seconds`, the worker restarts/retries the exact same record forever (`microbatch.html.txt:115`, `microbatch.md:129`).
- **Race-condition check**: Because tasks are single-threaded (`SKILL.md:24`), Scenario B/C/D stalls concurrent clients across **all** unrelated layers colocated on that task.
- **Flaws found**:
  1. Array misclassification in `weight` (`PLAN:326–328`) lets 500+ MiB primitive/Object arrays bypass `max-kept-weight`.
  2. Storing exact `:weight` on `:too-heavy` rows (`PLAN:373`) prevents `weight` from aborting at `bound + 1`.
  3. `BigDecimal` / `BigInteger` bit-length is not checked before `.stripTrailingZeros` / `.toString` in `weight` (and `text`).
  4. Uniform 32 MiB PState write bound for unpermissioned/declaration refusals was not weighed against shared-task latency (`Item b`).
- **Verdict**: **FAIL**
- **If FAIL — specific plan changes required**:
  1. Size primitive arrays (`byte[]`, `int[]`, `long[]`, `double[]`, `float[]`, `short[]`, `char[]`, `boolean[]`) in $O(1)$ as `alength × byte-width`, and push `Object[]` elements onto the iterative stack.
  2. Check `BigInteger` / `BigDecimal` `.unscaledValue().bitLength()` in $O(1)$ before any decimal string conversion or `.stripTrailingZeros`.
  3. Pass the applicable byte bound into `weight`, abort the instant accumulated weight exceeds `bound`, and omit `:weight` when `:not-kept` is `:too-heavy`.
  4. Weigh and set a separate smaller bound (default 256 KiB, Sid's to turn; `Item b`) for declaration refusals and non-standing refusals.

### 6. `SPEC` "What must hold" 5 (`SPEC.md:171–176`): Both gates, and where `:base`'s line lives
- **Source**: *"A streaming layer has every row on its own task. The base has its facts on every task and its answers on the base's task alone... The read of the base's acts must say on which task it reads, and why that task holds what it needs."*
- **Trace through the plan**: `:base` hashes to `task_base = hash(:base) mod N`. In `microbatch-gate`, `decide` writes `$$microbatch-line`, `$$microbatch-things`, and `:by-who` on `task_base` (`PLAN:424–429`), and `copies-of` (`walk.clj:288`) filters only `:fact` and `:by-value` for `|all`. `task_base` already holds `:base`'s `:last-stamp`, `$$microbatch-answers`, and `task_base`'s own synchronous local write of `$$microbatch-facts` (permissions + both settings, written in `decide` before `|all`, `microbatch_gate.clj:69`), so Stage 2 reading `:base`'s acts on `task_base` sees decisions, `:last-stamp`, permissions, and settings from the **same** microbatch commit (`IMPLICIT_SPEC.md` R12, R14)!
- **Fault-tolerance check**: Microbatch prime/commit resets and commits all PStates on `task_base` together across restarts and retries (`microbatch.html.txt:91–94`).
- **Race-condition check**: Unlike `$$microbatch-facts` copies on other tasks (which commit asynchronously task-by-task, `microbatch_gate.clj:122–125`), reading permissions + settings + `line` on `task_base` itself has zero cross-task commit skew (`R12`, `R14`).
- **Flaws found**: none found, with reasoning: colocating `:base`'s `line` and `things` on `task_base` avoids $N\times$ blob replication while guaranteeing same-task commit consistency with `:base`'s permissions and settings.
- **Verdict**: PASS

### 7. `O2` (`S1`, `S2`, `S3`, `S4`, `S5`): `:softland.store/who-may-read-acts` as a gate rule
- **Source** (`SPEC.md:136–148`, `IMPLICIT_SPEC.md:230–265`):
  - *S1*: *"With no fact, every layer reads as `:members`, the base too."*
  - *S2*: *"Only an act under the layer's first permission writes it; under a given permission it is refused `:only-the-first-holder-writes-a-setting`, whatever that permission allows."*
  - *S3*: *"A fact under the key on an entity that is not the layer is refused `:setting-not-on-the-layer`... A fact with no value under it is refused `:store-key-retract-not-built`... A layer cannot mark the key (`:marked-key-in-the-stores-namespace`). A making act cannot carry it... a value that is neither word is refused by the declaration, with a word of its own"*
  - *S4*: *"The place is chosen so that no thing that is refused today gets another word, except a thing whose only fault was this key."*
  - *S5*: *"Today a fact under this key is refused `:store-key-not-known`... After this piece it is taken... the fixture is made again and the receipt says so."*
- **Trace through the plan** (`PLAN:533–566`):
  - `store-keys` gains `:softland.store/who-may-read-acts {:value :who-may-read-acts-word :sits-on :the-layer :setting true :written-again true}`, `shapes` gains `:who-may-read-acts-word [:enum :members :everyone]`, `rules` gains `[:who-may-read-acts-fact-holds-its-word :who-may-read-acts-fact-not-members-or-everyone]`, and `first-act/starts-with` returns `:members` for all layers including `:base`.
  - Concrete trace of 6 acts on `:alice` (first holder `:alice`, given `:see-and-act` holder `:ben` with 4-slot permission `[:ben :alice :alice [:alice :alice :alice]]`):
    1. Making act with 3rd fact `{:e :alice :k :softland.store/who-may-read-acts :v :everyone}` $\to$ `making-act?` requires `(= 2 (count (:facts act)))` (`first_act.clj:125`) $\to$ refused `:layer-not-made`.
    2. `:alice` sends `{:e :alice :k :softland.store/who-may-read-acts :v :public}` $\to$ declaration rule refuses `:who-may-read-acts-fact-not-members-or-everyone`.
    3. `:alice` sends `{:e :chair-7 :k :softland.store/who-may-read-acts :v :everyone}` $\to$ `setting-keys` includes it $\to$ `setting-fault` refuses `:setting-not-on-the-layer`.
    4. `:ben` sends `{:e :alice :k :softland.store/who-may-read-acts :v :everyone}` $\to$ `second-write-keys` includes it and `(count (:permission act)) == 4 != 3` $\to$ `who-may-write-fault` refuses `:only-the-first-holder-writes-a-setting`.
    5. `:alice` sends `{:e :alice :k :softland.store/who-may-read-acts}` (no `:v`) $\to$ `store-key-fault` refuses `:store-key-retract-not-built`.
    6. `:alice` sends `{:e :alice :k :softland.store/who-may-read-acts :v :everyone}` $\to$ accepted; in `:base` (`:system` under `[:system :base :base]`), `copies-of` includes `:fact`, so `microbatch-gate` copies the setting to all $N$ tasks (`PLAN:562–563`).
- **Fault-tolerance & race-condition check**: Identical to existing `:softland.store/who-may-see`; deterministic pure rules, `termval` PState writes, serialized per layer.
- **Flaws found**:
  1. As traced in Item `d`, `IMPLICIT_SPEC.md` S4 is logically unsatisfiable as written (acknowledged at `PLAN:828–833`).
  2. `A10` (`IMPLICIT_SPEC.md:650–653`, `HANDOFF.md:74–78`): the statements of the new key's rules must be added to `gate-histories` reference before the rule is built; `PLAN:564–566` defers coordinating with uncommitted `gate-histories/` edits to the build step.
- **Verdict**: PASS on `S1`, `S2`, `S3`, `S5`; noted spec-erratum on `S4` (traced in Item `d`).

### 8. `O6` (`A1`–`A9`, `SPEC` "What must hold" 2): Run-again determinism across 4 and 8 tasks, failures, fixture, and pre-existing stores
- **Source** (`SPEC.md:157–162`, `IMPLICIT_SPEC.md:596–649`): *"A second run of the gates over the depots makes the same tables, on a store with another number of tasks too"*, `A1`–`A9`.
- **Trace through the plan** (`PLAN:568–600`):
  - `StoreAgain` calls `module/add-gates-and-reads`, which registers all 6 new PStates via `streaming-gate/add-to` and `microbatch-gate/add-to` (`A5`).
  - Keys in `line`, `things`, and `line-by-who` contain only `<layer>`, `<who>`, and 19-digit per-layer `<stamp>` — zero task IDs and zero depot offsets (`A2`, `A3`). Feeding 4-task fixture partitions in reverse order onto an 8-task cluster preserves per-layer order, producing identical `task-free` unions (`A4`).
  - `A6` (`bytes-outside-the-digests` in `run_again_proof.clj:1442–1455`): `:digest` in `line` is hex `String` (not `byte[]`), and the fixture hand sends only EDN-roundtrippable maps with no `byte[]`, so `bytes-outside-the-digests` still finds bytes only at `[:answers :digest]`, while `module_proof.clj` tests non-EDN things (`record`, `byte[]`, `sorted-map`, `PLAN:580–584, 757–758`).
  - **WAIT — subtle `run_again_proof.clj` placement check (`A1`/`A2`, `run_again_proof.clj:70–73`)!** Look at `run_again_proof.clj:70–73`: Check 2 and Check 3 verify placement per layer (`for each layer of the fixture, a select with that layer as the partitioning key...`). Remember `PLAN:96–102` (Q2): a thing sent for `:base` on `*streaming-depot` gets a `line` row, `things` row, and `line-by-who` row in `streaming-gate` under `:base`! Does `run_again_proof`'s placement check handle a layer id (`:base` on streaming, `:alice` on microbatch) that has `answers` and `:last-stamp` on the other gate? Yes, `run_again_proof.clj:1018–1020` already holds `:base` in `streaming` answers/last-stamp and `:alice` in `microbatch` answers/last-stamp (`PLAN:774–776` extends placement to `line`, `things`, and `:by-who`).
  - `A9`: Pre-build stores must be rebuilt via second run over depots (`PLAN:590–599`), consistent with Sid's 13 October decision 6 before real data is kept, and Stage 2 treats a missing `line` row below `:last-stamp` as a reported fault rather than end-of-line.
- **Fault-tolerance & race-condition check**: Check 4 of `run_again_proof` injects throws and stalls into `walk` (`A8`); because the new writes sit in the same `writes` loop behind check (a), retries converge identically to `answers`.
- **Flaws found**: none found on `A1`–`A9` once `:by-who` is included in `task-rows-held` (if folded into `$$task`) or `table-order`.
- **Verdict**: PASS

### 9. `SPEC` "What must hold" 8 (`SPEC.md:187–189`) & "What exists and must come out unchanged" (`IMPLICIT_SPEC.md:654–663`) & Matrix `door` lines (`E1`–`E5`)
- **Source**: *"8. Cost is said. What each decision costs more, in reads and writes, for both gates; what one read of n decisions costs, in seeks and in round trips; what the store keeps twice, if anything, and how much."* / *"What exists and must come out unchanged: The door's read by act-id... Every read of facts... The strays... The cost of a decision that this piece does not need to touch"*.
- **Trace through the plan**:
  - `door_reads.clj` (`answer-by-act-id`, `depot-of-layer`), `$$answers`, strays, and `read_path` fact queries are untouched (`PLAN:806–807`), so every `door` line in `E1`–`E5` (except the intentional `S5` change on `:softland.store/who-may-read-acts`) is bit-for-bit unchanged.
  - Per-decision read/write counts are tabulated in `PLAN:609–620`.
  - **However**, read seeks in `PLAN:477–484` are reported as `"Tasks read"` (`1`) rather than RocksDB seeks (hiding `6–7` seeks on normal reads and `1 + 2k` seeks on outside-asker reads), and `O1` decision CPU cost adds an avoidable second full traversal of every act (`Item e`).
- **Fault-tolerance & race-condition check**: Unchanged paths inherit existing proofs.
- **Flaws found**: Read seek cost in `PLAN:477–484` is miscounted as tasks-read instead of RocksDB seeks; `O1` adds an unnecessary second walk on valid acts.
- **Verdict**: **FAIL** (fix seek counts in `Partitioning efficiency` and fuse common-case weight counting into `digest`).

### 10. `R20` Hand-off (`IMPLICIT_SPEC.md:466–472`, `PLAN:672–674`): Keys containing code points $\ge \text{U+E000}$ or $> \text{U+FFFF}$
- **Source**: *"A layer id is a readable keyword or a UUID, and a readable keyword can hold a letter from U+E000 on or outside the first plane. For keys that hold such a letter Rama was seen to give fewer rows of a range than the table holds (`read_path/sources.clj`, `outside-the-agreed-range?`)"*.
- **Trace through the plan**: Suppose outside asker `:alice-𝐀` (`\uD835\uDC00`, outside BMP, valid `readable-keyword?`) sends 3 refused acts into closed layer `:secret`. Stage 1 writes `:by-who` keys `":secret :alice-𝐀 000...000"`, `...001`, `...002`. `PLAN:672–674` (Hand-off #12) warns Stage 2 only about *"A layer whose id holds a letter from U+E000 on"*, forgetting that `<who>` in `":secret :alice-𝐀 <stamp>"` is **also** a readable keyword prefix in a sorted range scan!
- **Fault-tolerance & race-condition check**: N/A (key-encoding / range-navigator boundary contract between Stage 1 and Stage 2).
- **Flaws found**: Hand-off #12 (`PLAN:672–674`) omits `<who>` in `:by-who` keys from the `outside-the-agreed-range?` warning to Stage 2.
- **Verdict**: **FAIL**
- **If FAIL — specific plan change required**: Extend Hand-off #12 to state that **both `<layer>` (in `line`, `things`, `:by-who`) and `<who>` (in `:by-who`)** can hold code points $\ge \text{U+E000}$ or outside the BMP, so Stage 2 must apply `outside-the-agreed-range?` to both.

---

## Summary of Required Revisions for Phase 1

1. **Bound `weight` and fix its type coverage (`Items b, e`, `W6`, `W7`):**
   - Handle Java primitive arrays in $O(1)$ (`alength × width`, including `byte[]` as `alength` without hex doubling) and `Object[]` by pushing elements onto the explicit stack (`PLAN:326–328`).
   - Bound `BigInteger` / `BigDecimal` by `.bitLength()` in $O(1)$ before `.stripTrailingZeros` or `.toString`.
   - Pass the byte limit into `weight`, short-circuit immediately at `limit + 1`, and omit `:weight` on `:too-heavy` rows (`PLAN:297–299, 373`).
   - Fuse common-case byte counting into Step 2's `digest/digest` (`text/feed`) walk so valid acts and ordinary refusals are walked once, invoking the iterative fallback walker only when `text/feed` hits `#other` or `#too-deep`.
   - Set and document the refusal bound option (recommended default: 32 MiB for accepted acts and standing-member refusals, 256 KiB for non-standing and declaration refusals, marked as Sid's to turn; `Item b`).
2. **Fold `:by-who` into `$$task` (or justify on weighted cost) and fix `Partitioning efficiency` (`Item c`):**
   - Replace `$$streaming-line-by-who` / `$$microbatch-line-by-who` with `:by-who (map-schema String Long {:subindex-options {:track-size? false}})` inside `$$task` (saving 2 PState partitions per task at +1 seek on rare non-live outside-asker reads), and keep Road J marked for Sid to turn.
   - Replace `"Tasks read"` and prose `"Steps"` in `PLAN:477–484` with total RocksDB `seeks/op`, numeric `iterator-reads/op`, and `Σ(proportion × seeks)` / `Σ(proportion × iterator-reads)` at $N = 1, 16, 128$ (including the $1 + 2k$ point seeks of `:by-who`).
3. **Correct Road B / Q7 and Hand-off #12 (`Item a`, `R20`):**
   - State accurately in Road B / Q7 (`PLAN:134–150, 508–510`) that `microbatch-gate` *can* maintain an exact 0-based partition offset in a PState, whereas `streaming-gate` cannot across post-commit retries of strays/repeats, and `foreign-depot-read` remains unsuitable in both due to non-yielding synchronous reads.
   - Include `<who>` alongside `<layer>` in Hand-off #12 (`PLAN:672–674`).

PHASE_VALIDATION:major-fail
