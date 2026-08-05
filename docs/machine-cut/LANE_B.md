# Machine-Cut — LANE B (the serve + the face) — artifact

Status: **GREEN**. All Lane B gates (G4, G10, G12) + the §7 pairs-aware face
transcription run to green in-context, first run. Zero interpreter/grammar/
registry edits (§7 satisfied — no stop-clause). Fence held: only additive edits
to `face_projection.clj`, plus NEW test namespace, NEW face `.edn`, NEW fixture,
NEW golden. No commits.

Implements CONTRACT §§4.4 (read route), 6 (grouping laws + serve totality),
7 (the pairs-aware face). Dispatch amendments applied: `:pairs-with` used
directly (enum in tree, verified loading); machine-cut actor constant defined
LOCALLY in `face_projection.clj` with an INT re-home flag.

---

## Gate results — RUN in-context (exact final suite lines)

**Lane B namespace alone** (`app.machine-cut-serve-test`):
```
Ran 18 tests containing 115 assertions.
0 failures, 0 errors.
```

**Full `face_*` suite + Lane B, serial, ONE JVM** (the MC-T8 additive proof —
existing face suites stay green over the in-tree edit; existing goldens
untouched):
```
Ran 62 tests containing 916 assertions.
0 failures, 0 errors.
```
(namespaces: face-arsenal, face-assembly, face-gate-fixes, face-integration,
face-primitives, face-projection, face-transcription, machine-cut-serve)

**Relation-kernel suite** (the read route's dependency; `:pairs-with` enum
loads; G10 exercises it live):
```
Ran 2 tests containing 222 assertions.
0 failures, 0 errors.
```

The `[FACE] machine-cut edge read failed: …Keyword cannot be cast to
QueryTopologyClient` / `…query is null` prints during the run are the EXPECTED
honest-degradation logs from the G12 read-failure tests (the catch → `[]`,
never a throw). The `[FACE-ARSENAL] replay…` / `face-list read failed` prints
are face_arsenal_test's own pre-existing negative tests, not mine.

### Gate-by-gate (CONTRACT §9)

- **G4 grouping pure core** — GREEN. `g4-basic-pair-and-totality` (pair forms,
  every served turn exactly once), `g4-mc-t8-additive-keys-only`,
  `g4-mc-t14-both-copies-dedup` (both endpoint copies of one relation-id → one
  pair, `edges-read` 1 — the MC-T14 pin), `g4-mc-t10-foreign-counted-never-merged`
  (foreign asserter counted, pair stays machine-cut's), `g4-conflict-deterministic-pick`
  (one response two prompts → river-earliest prompt wins, +1 conflict, later
  prompt keeps empty `:responses`), `g4-until-ms-cut-prompt-strands-responses`
  + `g4-until-ms-cut-responses-empty` (MC-T11 both directions),
  `g4-composition-cut-then-group` (real order: blocks→turns → apply-until-ms →
  derive).
- **G10 foreign-asserter isolation (IPC)** — GREEN.
  `g10-foreign-asserter-isolation-ipc`: boots the real relation-kernel cluster,
  seeds a machine-cut edge + a foreign `sid` edge on the SAME events BY HAND
  (`rk/assert-request` + `append-relation-request!`), materialized-read barrier
  (`rk/await-relation` on the real read route), then asserts §4.2 colocation
  (both copies under the conversation key), the ONE conversation-key call
  returns both distinct relation-ids, and the projection serves the machine-cut
  pair ONLY with `:foreign-asserter-edges 1`, merging nothing.
- **G12 serve totality** — GREEN. `g12-absent-rk-rt-none` (absent rk-rt → `[]`
  → `:none`, every turn unpaired), `g12-read-failure-degrades-to-empty` (poisoned
  handle → catch → `[]`; MC-T12), `g12-nil-address-none`,
  `g12-mc-t8-turns-byte-identical-under-merge` (the additive merge never disturbs
  `:turns` or any existing key), `g12-none-structure-shape`.
- **§7 face transcription (consumer 1)** — GREEN.
  `paired-face-compiles-clean-and-golden` (compiles V1-V7 clean, golden-equal,
  apply-report `{:items-without-id 0 :binds-missing 0}`, positive content-h,
  flattens to rects + text-ops), `paired-face-based-on-boxes` (envelope
  `:assembly/based-on "boxes-face"`), `g4-fixture-parity` (the golden fixture's
  pair structure IS `derive-pair-structure`'s output — no drift), `paired-anatomy`
  P1-P7 (silver-mark structure-line header; one russian-doll pair frame carrying
  the prompt id; per-pair asserter named; **4-layer containment** block card ⊂
  user|response frame ⊂ pair frame; unpaired turn shown honestly; page-end
  paging answer; geometry within the pane).

---

## Files created / edited

EDITED (additive only; MC-T8):
- `src/app/server/rama/face_projection.clj` — new requires (clojure.string,
  transcript-identity, relation-kernel — READ-ONLY public query surface);
  `machine-cut-actor-v0`, `actor->structure-label`, `dedup-edges-by-relation-id`,
  `derive-pair-structure` (pure core), `none-structure`, `read-machine-cut-edges`
  (total read); `conversation-projection` merges the four additive keys after
  `shape-conversation`; the nil-address branch + `error-data-context` wrap in
  `(merge none-structure …)`. `:turns` + every existing key byte-identical.

CREATED:
- `test/app/machine_cut_serve_test.clj` — Lane B gates (18 tests).
- `resources/public/faces/boxes-paired-face.edn` — the pairs-aware face
  (`:assembly/based-on "boxes-face"`, grammar 0, existing vocabulary only).
- `test/app/fixtures/faces/machine-cut-paired-conversation.edn` — the §6/§7
  paired data-context (turns copied verbatim from plurality-conversation).
- `test/app/fixtures/faces/boxes-paired-face.golden.edn` — the rt-tree golden
  (generated via `regen-paired-golden!`, an explicit diff-reviewed act).

NOT touched (fence): `machine_cut.clj` (Lane A's), `file_viewer.cljc`, any
kernel file, `face_assembly.cljc`/`face_primitives.cljc` (zero grammar edits),
existing face `.edn` files, existing goldens (verified empty diff).

---

## Judgment calls

1. **Turn↔edge mapping by RECOMPUTE**, not string-parse: build
   `{(tid/chat-message-id address turn-id) → turn}` and look up each edge's
   `from`/`to` `:target-id`. The conversation object-key contains colons, so
   parsing the message-key out of `oc:chat-message:<ok>:<uuid>` is fragile;
   recompute reuses the canonical `tid/chat-message-id` — exact and total.
2. **Pure core dedups by relation-id itself** (MC-T14) even though the R1 query
   already dedups — so the G4 both-copies fixture (o:/i: copies) is a real pin,
   and the pure function is correct regardless of read path.
3. **`:source` = `:machine-cut` iff `pairs-count > 0`, else `:none`.** Machine-cut
   edges that all get cut/dropped → `:none` ("no machine cut"). `:edges-read` =
   distinct machine-cut relation-ids read (after dedup + actor filter).
4. **Defensive chain/totality rule:** a pair HEAD is never nested as another
   pair's response. Guarantees every served turn appears exactly once even under
   a (contract-disallowed) A→B→C chain; documented in `derive-pair-structure`.
   The realistic disjoint invariant (driver output has each event once) means
   this never fires on real data — it only backstops totality.
5. **Added an honest `:unpaired` section to the FACE** beyond §7's literal
   "bind `[:pairs]`": the structure-line already names the unpaired count, and
   the face shows the turns too (a muted frame each) so no served material is
   silently dropped (map-must-not-lie). Additive fidelity; still zero grammar
   edits.
6. **The pair nesting is fully expressible** in the assembly grammar
   (multi-segment `:each [:user-turn :blocks]` and `:bind [:user-turn :order]`
   ride `get-in` in `expand-slot`/`resolve-props`) — verified in
   `face_assembly.cljc`. Every primitive used (`:stack :box :header-band
   :text-clip :text-run`) is the existing W2 vocabulary. **No stop-clause.**

---

## INT flags (for the orchestrating Fable session)

1. **Re-home the machine-cut actor constant** (LANES INT checklist item 3): I
   defined `machine-cut-actor-v0 "llm:machine-cut/v1"` locally in
   `face_projection.clj` (dispatch amendment — machine_cut.clj was not in this
   lane's world). Lane A now has `machine_cut.clj` in the tree; INT picks the
   shared location and makes it one def + two requires. My read/derive read the
   constant only through `fp/machine-cut-actor-v0`, so the re-home is a rename.
2. **Boot attach of `rk-rt` into face-ctx** (LANES INT checklist item 2):
   `conversation-projection` reads `rk-rt` from ctx; when absent → `:none`
   (total). INT wires `file_viewer.cljc`'s boot to attach `rk-rt` + WAL replay,
   poisoned-but-total (Amendment C / MC-T12). My `read-machine-cut-edges`
   consumes a nil OR throwing handle safely (→ `[]`), so a poisoned-but-total
   boot handle degrades honestly here — proven by `g12-read-failure-degrades-to-empty`.
3. **`:edges-read` semantics** = distinct machine-cut relation-ids read
   (deduped, actor-filtered). If G13's receipt wants "rk-visible edge count ==
   plan's assert count", note that `:edges-read` counts what the SERVE read, not
   what the driver planned; they coincide when every asserted edge's endpoints
   are on the served page (the default worn window). Flagged so the receipt
   compares the right two numbers.
