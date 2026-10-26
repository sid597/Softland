# Ground check, section 3: trying to break the two claims

By the ground-break agent (Opus 5.5, xhigh effort), night of 14 to 15 October 2026, at HEAD c90c7bcc. Nothing was changed. Short paths are under `src-store/softland/store/`. "Probe line n" is line n of `runs/ground-3-probe-output.txt`, printed by `runs/ground-3-probe.clj` (pure code, `clojure -M:store`, exit 0).

**Who wrote the two claims** (`memtool who`, run). Sid typed neither phrase. A session wrote "order never uses the time" first («7accdff4», 14 Oct 07:38). A session also wrote "a refusal moves the count and no fact" («4108f1f8», 17:25). Sid pasted both, in ‹0cc1e524› and three other lines. His own line is "order and time are different and we don't use the time for ordering" (‹2f80019d›). I searched his lines for "refusal stamp" and found none that says a refusal is counted. In the README that rule sits under "What comes with it" (`src-store/README.md:999-1000`), and no record id marks it as his. Where the code states the claims: `gate/stamp.clj:5-6, 21-28`, `gate/decision.clj:85-86, 532-533`, `door/declaration.clj:170-171`, `module/walk.clj:97-101`.

## Findings, most important first

**1. "Order never uses the time" holds.** Traced from the code, and checked by probe.
- **Where the time enters and leaves.** The walk takes the time in once, at `module/walk.clj:295`. It hands it once, at `walk.clj:372`, to `writes`. From there it is only a value in a row: `record/rows.clj:192`, `:248` and `:446`, reached through `decision.clj:487, 496, 554`.
- **No key holds the time.** The keys are made at `rows.clj:171`, `:179` and `:410`. The one key that holds a stamp is `by-value-key`, and that stamp is `since` (`decision.clj:429`). `since` comes from the act's own stamp (`walk.clj:370`), which comes from the last stamp (`walk.clj:361-362`).
- **No comparison reads `:time`.** There are four comparisons in the five files:
  - `declaration.clj:175`, which checks that the time is not below zero (see finding 4);
  - `decision.clj:414`, which sorts place keys, as strings;
  - `decision.clj:457`, which compares one stamp with another stamp of the same layer;
  - `rows.clj:552-558`, which puts key strings in order.
- **By value.** Two different times give the same rows except for `:time`, and no key contains the time's digits (probe lines 30-32).
- **The second send** (V43, open). The repeat check, check (a), is keyed on the act-id (`walk.clj:285-288`). So the time that stays on the answer is the time of whichever send came first in the depot, not the earliest time (`door.clj:468-472`). The two sends have one digest (probe line 22).
- **The microbatch gate** does nothing with the time (`module/microbatch_gate.clj:191-222`).
- **Existing checks that see this:** `pure_proof.clj:2809, 2950`.

**2. "A refusal moves the count and no fact" holds.**

For a thing with no time, the walk goes like this:
1. It is no stray (`declaration.clj:611-612`; probe line 38).
2. There is no answer yet under its act-id (`walk.clj:286`).
3. The declaration refuses it `:no-door-time` (`walk.clj:294`).
4. `*refused` is set, and no row is read (`walk.clj:300-302, 317-319`).
5. Step 6 is skipped (`walk.clj:336`).
6. The walk reads the last stamp and adds one (`walk.clj:361-362`).
7. `writes` gives only the answer and the last stamp (`decision.clj:553-555`; probe line 28), and the answer has no `:time`.

- **Every refusal takes that path.** A refusal word can come from three places: the declaration (`walk.clj:294`), the standing check (`:311`) or the places check (`:331`). All three land in `*refused`, and `decision.clj:553` is the only place that makes fact rows. A refusal is never nil, because the declaration gives `:malformed` when it has no other word (`declaration.clj:688`).
- **No copies and no skipped count.** A refusal has nothing to copy (`walk.clj:244`; `microbatch_gate.clj:205`). An accept cannot skip the count, because `writes` always adds the last stamp (`decision.clj:555`).
- **Answer and count are saved together.** They are written in one loop with no partitioner in it (`walk.clj:171-186, 377-401`). A stream topology saves writes between partitioners together, and a microbatch topology saves the whole batch together (Rama skill `core-concepts.md:65`; `walk.clj:202-205`). This comes from reading the code and Rama's documents; I did not run it.
- **Existing checks that see this:** `pure_proof.clj:1828, 1841, 2816, 2979`, and on a cluster `module_proof.clj:1193`. The pure proof runs its own copy of the walk (`pure_proof.clj:2318`), not `walk.clj` itself. Only `module_proof` checks the real walk.

**3. "The count" belongs to one layer in one gate, not to the layer.** This is a wording problem.
- **One layer can have two counts.** A thing sent on the streaming depot for a microbatch layer is not a stray, because the stray test only checks that the act-id names this gate's depot (`declaration.clj:622`). The gate refuses it `:layer-belongs-to-the-other-gate` (`decision.clj:217-218`), and that refusal moves the streaming gate's count for that layer (`walk.clj:361, 397`).
- **The fixture already has such layers.** Its page says: "`:base` in the streaming gate, `:alice` in the microbatch gate … which a refusal already counted 0 and 1" (`test-store/softland/store/run_again_fixture/README.md:71-73`).
- **A layer can be counted before it exists.** Refusals with `:layer-not-made` are counted too, so the act that makes a layer gets stamp n, where n is the number of refusals before it. I found no code that expects a making act to have stamp 0 (grep).
- **Proposed fix:** in `stamp.clj:5-9`, `walk.clj:111-115` and `README.md:999`, say "the layer's count in this gate".
- **Existing check:** `pure_proof.clj:2989` sees this behaviour, but its sentence calls it "the first decision of a layer".

**4. "No rule reads it, so a wrong clock … changes … no decision" (`stamp.clj:26-28`) overstates.**
- **The time's form can refuse an act.** The declaration's shape refuses a time that is missing, is not an integer, or is below zero (`declaration.clj:330`). So a clock set before 1970 changes the decision from accept to refuse (probe line 12; `pure_proof.clj:831`). The stamp is not affected: the refused thing still takes the next one.
- **Carried stamps are the same.** No gate uses their values, but their form is checked: `:carried-stamp [:int {:min 0}]` (`act.clj:195-196`).
- **A first send with no time can block the real act.** Another program can append the act with no time. The gate refuses it, and a later real send under the same act-id is then never decided, because it has the same digest (probe line 23; `door/digest.clj:69-73` already says this).
- **Proposed fix:** "no rule reads its value; its form (present, a whole number, not below zero) is the declaration's".
- **Existing checks:** none checks this sentence.

**5. An accept with no time would crash the walk.** This is a latent risk, not a present fault.
- **The crash.** `writes` with an accept and a nil time throws a NullPointerException at `rows.clj:248` (probe line 33). Inside the walk, that crash fails the event (`walk.clj:227-228`), and the gate's worker restarts (`streaming_gate.clj:71-73`).
- **Why it cannot happen today.** What prevents it is that two separately written pieces of code agree: the declaration's shape check on the time (`declaration.clj:330`, with `act.clj:429-431`) and the function that reads the usable time (`declaration.clj:175`). They agree on 13 values in the proof (`pure_proof.clj:818-837`) and on 13 more in my probe (probe lines 9-21).
- **Proposed fix:** for an accept, take the time from the act as the declaration already checked it (`declaration.clj:711`).
- **Existing checks:** those value rows would catch a split only for the values they list.

**6. The stamp's wrap breaks the "later than" test.** This is low priority.
- **What breaks.** After 2^63 - 1 decisions in one layer, the stamp wraps to a negative number (probe line 5; `stamp.clj:54`). From then on, `decision.clj:457` keeps an old entry in the facts-by-value order that should be taken out: the probe takes it out before the wrap (line 36) and keeps it after (line 37). A maker's carried stamp after the wrap would also be refused.
- **Proposed fix:** say this under `stamp.clj:36-37`. The wrap itself is already checked (`pure_proof.clj:1148`).

**7. `next-stamp` "never throws" (`stamp.clj:50`) holds only for a Long or nil.** A string throws, `false` gives 0, and a double is cut to a whole number (probe lines 2, 6, 7). This cannot happen in use, because the table stores only Longs (`record/tables.clj:168`). **Proposed fix:** say "for a Long or nil".

## What I could not break, and what I did not read

**Could not break:**
- A refused thing never writes a fact row or a by-value row (probe line 29).
- No path moves the count twice for one thing.
- Strays get no stamp (`walk.clj:273-277`). That is right, because no answer would point to that stamp.
- `places` and `since-by-place` compare stamps only within one layer.

**Not run:** any Rama cluster. **Not read:** the read path beyond a grep, which found no sort by `:time`.

## Leads outside the five files

- **The second send** (`door.clj:468-480`): which send's time the answer shows (V43).
- **`:claimed-when`** (`act.clj:161`): the maker's own time, which is never written on any row. The declaration accepts -1 and refuses 1.5 as `:malformed` (probe lines 42-43).
- **The wrong-partition stamp** (`microbatch_gate.clj:137-147`): a thing on the wrong partition would get a stamp from that task's count.
