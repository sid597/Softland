# smalltalk-ui-vm — default-fail contract validation

**Verdict: MINOR-FAIL — eight enumerated fixes (F1–F8), applied to
CONTRACT.md in this same session per the minor-fail law; the round is NOT
re-run.** No finding invalidates the architecture, the two-phase split, a
fence, a gate's substance, or any §0 ruling; every fix is a precision
amendment the existing allowlists and gates already accommodate. The
judgment call (minor vs substantive) is argued at F1 below so the
implementer and the gate can re-litigate it cheaply.

Validated 2026-07-30 against:

- branch `docs/current-mental-model-local`, HEAD `88c7b96` (docs); the code
  tree is byte-identical to the manifest's cut point — `git diff
  db8e21d..HEAD -- src/ test/ bin/` is empty (verified this session)
- the binding package document `build/smalltalk-ui-vm/CONTRACT.md`
- `DIRECTION.md` (settled 2026-07-30, same directory), `NOW.md` STANDING
- format exemplar `build/halo/{CONTRACT,VALIDATION}.md`
- source and test files as read-only evidence (receipts at the bottom)

Starting foreign state preserved exactly:

```text
?? docs/current-mental-model/build/smalltalk-ui-vm/PODCAST_SCRIPT.md
```

No source or test file was changed. No suite, compile, runtime, or push was
opened. `env.clj` was never read.

## Findings — F1..F8 (severity · claim · disk evidence · smallest amendment)

- **F1 · the biggest · W1's prop vocabulary has no route for instance data.**
  W1 pins `:part/props {slot → literal | [:wear facet key]}` — wear policy
  only. But under the registry conventions, data reaches a builder ONLY
  through props (`face_assembly.cljc:359-372 resolve-props` — `{:bind
  [path]}` against the apply data context) and `ctx` `{:id :view-instance
  :address :geom}` (`:465-467`); builders never see the data context
  directly. The block's parts need INSTANCE data — text/lines, caret,
  selection, msel, machine?, hover?, notice, refusal, headers, wrap-col,
  boundary?, gsel?, gold/silver marks (`block-tree`'s full signature,
  `ground.cljs:606-608`). As written, a faithful implementer either invents
  an unsanctioned bind form or stops on §9(a) — a scheduled
  contract-authoring-gap stop (the multi-cascade class). *Amendment (F1)*:
  W1 gains a third prop form `[:view key]`, resolving against the block's
  derived view-model at apply time, with the CLOSED view-key vocabulary
  owned by `anatomy_material.cljc` beside the grammar — the contract's own
  data-resolution law applied to the view side. Why minor, not substantive:
  it changes no file set, no gate, no phase, no §0 ruling; it completes the
  schema exactly the way W2's "apply per instance data change" already
  requires, and the full view-key census was traced this round (below).
- **F2 · G2's "structurally exact (data)" is unsatisfiable through the
  required machinery as written.** `apply-assembly` stamps EVERY built node
  with `:assembly/src-path` (`face_assembly.cljc:395-412 stamp-src-path`)
  and the root with `:view-instance`, `:address`, `:assembly/content-h`,
  `:assembly/apply-report` (`:511-524 stamp-root`); `block-tree`'s output
  carries none of these. A literal data-exact compare fails on every node —
  the trap is that an implementer "fixes" it by NOT using the machinery (a
  second interpreter, T2). *Amendment (F2)*: G2/W3 name the equality
  relation — exact over ops, bounds, styles, node ids, claims, stamps, and
  every block-tree-authored `:data` key; the interpreter's own provenance
  keys (that exact five-key set) are the EXPECTED delta, normalized out by
  the comparator (and asserted present separately). Primitives emit
  block-tree's exact node ids (builders choose their own ids — verified:
  `ui-panel-group`/`header-band` already do; `rect_tree.cljc:40`).
- **F3 · §0/W4 count the non-generic facet surfaces as three; disk shows
  four.** The halo census does NOT derive from `wears-for` keys — it reads
  the hardcoded `block-wear-census` vector
  `[:provenance :attention :foldable :positioned :threaded :text-body]`
  (`ground.cljs:344-347`, consumed at `:360`), which is ALSO pinned as a
  string literal in `material_portal_test.clj:1191-1193`. For `:anatomy` to
  appear in the halo (the Workshop's door, §0), this vector must gain
  `:anatomy`. The test-side re-cut was already named in §6 P1; the
  source-side edit and the count were not. *Amendment (F3)*: §0 says four
  and names the vector; W4's "(census = wears-for keys)" corrected; §6 P1's
  `ground.cljs` entry names the edit.
- **F4 · W3 calls `:render-sig` "15-element"; disk has 16**
  (`ground.cljs:1208-1210`: view · machine? · hover? · notice · wrap-col ·
  headers · msel · bnd? · gsel? · placement-derived? · wears · gold-marks ·
  silver-marks · font-size · char-advance · line-h). The load-bearing claim
  survives — `wears` rides the sig, so worn-anatomy identity keys rebuilds.
  *Amendment (F4)*: count corrected.
- **F5 · V2's live-corpus enumeration names no concrete read surface.** G2
  says "ENUMERATES the real corpus (every block the live land renders)" —
  executable only if the surface is named. The rendered set is
  `(:blocks @!world)` after reconcile — post-merge (merged run blocks under
  first-member identity, `ground.cljs:1640-1710`), deduped, boundary-marked
  — exposed on the rig as `window.__ground.blocks()`
  (`ground.cljs:3701-3705`). *Amendment (F5)*: G2 names it, with counts
  asserted against the served post-merge set; fixtures = committed EDN
  generated from `block-tree` pre-deletion; the committed harness asserts
  interpreter-vs-goldens on the JVM (the interpreter is `.cljc` and already
  loads JVM-side — `assembly_adapter.clj:35,145` is the standing proof).
- **F6 · the recipe census will silently un-name the "block" recipe.** W5
  rides the composition/recipe helpers (`material_portal.cljc:371-401`,
  pins exact) — but `recipe` gates its naming on the hardcoded `worn-five`
  set `#{:attention :foldable :positioned :threaded :text-body}`
  (`:397-403`). Anatomy-stamped contributions join the observed composition
  (the why-this-pixel inverse requires them), so block compositions become
  six facets ≠ worn-five and the named recipe stops matching. *Amendment
  (F6)*: §6 P2's `material_portal.cljc` entry names the worn-five ruling
  (widen the named-recipe set as the pressure demands; disclosed either
  way).
- **F7 · stop-clause (c) resolves positively — record it.** The installed
  CLI (`claude` 2.1.220, `/home/sid/.local/bin/claude`) carries BOTH
  `--model <model>` and `--effort <level>` (`low, medium, high, xhigh,
  max`) — verified this session via `--help`. The honest subset is model +
  effort, both wireable; precontext rides the prompt narrowing (no flag
  needed, by design). *Amendment (F7)*: W7 carries the receipt
  parenthetical; the implementer still re-verifies at wiring time (V6
  stands).
- **F8 · "grammar-v2" naming.** `threaded_material.cljc` has exactly one
  grammar, version `0` (`:8,26-33`) — the widening mints the SECOND version
  (the next key), not a key named 2. *Amendment (F8)*: one clarifying
  parenthetical in §6 P1.

## V1 — assembly capacity: PASS after F1/F2 (no §9(a) stop finding)

Read IN FULL: `ground.cljs` `block-tree` `:599-896` (plus every pinned
region of the file), `face_assembly.cljc` (569 lines),
`face_primitives.cljc` (959 lines), `rect_tree.cljc` (426 lines).

### The conventions, as they actually are on disk

- Two-stage law: `compile-assembly :330` (pure, total, V1–V7 validation,
  error-card on failure, builder graph closed once) → `apply-assembly :526`
  (pure; props resolved per node via `{:bind [path]}` against the data
  context; ONE `resolve-layout` at the root; root stamped with
  identity + apply-report). Grammar v0 = `:prim`/`:each`/`{:bind}`; NO
  conditional-presence form — presence must arrive as data (an `:each` over
  a 0-or-1-element seq) or inside a primitive.
- Builders: `(fn [ctx props children] → rt-node)`; measure in the
  primitive, arrange in the engine; builders may return ANY node ids and
  explicit child geometry.
- **The load-bearing mechanism for the block:** `rect_tree/layout-children`
  passes nodes WITHOUT `:layout` through unchanged (`rect_tree.cljc:91-92`)
  — explicitly-positioned children, including negative offsets (rail/box at
  `-pad`, boundary at `-1.6·line-h`, marks above), survive `resolve-layout`
  exactly as `block-tree` itself relies on today.
- Totality layering is coherent: a malformed WORN anatomy falls to the code
  floor at wear resolution (`facet_material.cljc:175 resolved-wear` — the
  floor law W2 cites, verified verbatim), so the interpreter compiles the
  floor constant; the assembly error card remains the backstop for a
  floor-constant bug only.

### The behavior census walk (every `block-tree` behavior → its landing)

| block-tree behavior (disk) | lands as |
|---|---|
| lines: split + machine wrap at wrap-col (`wrap-lines :556-572`, greedy, hard-break) + header prepend (`:635-638`) | char-grid body primitive (named in W2); carries block-tree's OWN `wrap-lines` moved to `.cljc` — NOT `rt/wrap-line`, a DIFFERENT algorithm (word-boundary); byte-identity would catch the swap, this note saves the cycle |
| geometry: w = maxlen·advance + 2·pad, h = n·line-h + 2·pad (`:640-642`), root min-clamps (`:862`) | root primitive measures; `pad` binds `[:wear :attention :attention/hit-padding]` |
| per-line ops + color policy (header→tint / machine→dim / fg) + header provenance/foldable stamps + body text-body stamp (`:643-677`) | inside char-grid; stamps mint from `:part/stamp` × bound wear × `(:address ctx)` (unit-id rides `view-ctx :address` — the existing convention, `apply-assembly` docstring) |
| selection wash: focused selection OR msel → per-line rects (`:681-706`) | selection-wash primitive (named); offsets from `[:view …]` binds (F1) |
| decorations: machine rail (`:708-722`) + attention box on focused?/hover? (`:723-741`) composed via `facet-material/compose :495` (same-slot `:append`+priority vocabulary — the rail/box pair in `:block/decorations` exercises it TODAY) | rail + box primitives; the anatomy APPLY step reproduces the compose call over same-slot part contributions |
| conflict lint (`composition-lint-nodes :534-553`) | conflict-lint primitive (named); consumes compose's `:conflicts` |
| caret (`:785-790`, `ge/caret->line-col` — `.cljc` ✓) | caret primitive (named) |
| refusal line at y=n·line-h (`:791-797`); notice below it (`:798-803`, offset depends on refusal presence) | refusal/notice primitives; cross-part offsets ride view-derived props or a marks-stack — both expressible |
| boundary line at negative y with provenance stamp (`:804-815`) | boundary primitive (named) |
| gold/silver mark lines, y stacked over boundary?/gold presence (`:823-856`) | mark-line primitives (named); say-never-wish text composition rides view data |
| fold-header hit-only nodes with `:block/fold-header` claims, appended LAST for reverse hit-test (`:754-772`, comment `:747-753`) | fold-header-hit primitive (named); `:part/order` reproduces both paint and hit order — order as material is the point |
| gsel wash (`:775-780`), msel (via selection wash) | parts with `:part/when` presence entries |
| root data: address + attention hit stamp + `:material/claim` (site by machine?, facets `[:attention :positioned]`) + placement-derived?→positioned stamp + (not machine?)→threaded send-adoption stamp (`:860-895`) | root part's stamp/claim rows; claim maps are V4-guard-legal literals (maps/keywords only) |
| `rt/resolve-layout` at root (`:860`) | `apply-assembly` does exactly this (`:554`) |

**Presence census** (the closed `:part/when` vocabulary the validator must
enumerate — W1's "…" made concrete): `:always :machine :user :header
:focused :hover :has-selection` (focused+selection ∪ msel) `:has-refusal
:has-notice :boundary :has-group-sel :has-gold-marks :has-silver-marks
:has-conflicts`, plus root-stamp conditionals `placement-derived?` /
`(not machine?)`. Presence is instance data ⇒ `:part/when` evaluates at
APPLY time; compile stays per (master, revision) — cache-coherent with W3.

**Reachability**: `block-tree` has exactly ONE caller — `rebuild-block!`
(`ground.cljs:1214`); the cutover is a single-call-site swap and G2's
grep-negative criterion is cleanly executable. "Private helpers DELETED"
scope note: `wrap-lines`/`text-op`/`run-view`/`fold-header-line`/
`lines-offset` have OTHER callers (`machine-visual-lines :1112-1124`,
`render-provisional-slot! :1286`, halo/provisional composers) and survive
where still used — after the body primitive absorbs the wrap truth, the
copy path (`machine-visual-lines`) must call the SAME moved fn so
copy-what-you-see stays one truth.

**Keying (T11)**: `wears` is element 11 of the 16-element sig
(`:1208-1210`); the serve watch keys on `material-render-state` diff
(`:3663-3674`), the preview overlay is a fresh object breaking `identical?`
(`:230-246`, `:3254-3310`), and `wears-for` merges instance tiers
generically via `spec-for-facet` (`facet_masters.cljc:32`) — a worn or
deviant anatomy rides the existing invalidation with zero new trigger
machinery, exactly as W3 claims.

## V2 — corpus + verb: PASS after F5

- Live corpus surface: named by F5 (`(:blocks @!world)` post-reconcile /
  `__ground.blocks()`); the rendered set is post-merge — the corpus is what
  the land RENDERS, not raw served units (merge law `:1640-1710`, dedupe
  `:1720-1738`, boundary stamps `:1740-1766`).
- Verb: render-and-compare each (both paths callable in one browser context
  pre-deletion), counts asserted. `block-tree` is CLJS-only, so the
  live comparison runs in the browser (the G3 rig exists for exactly this);
  golden fixtures are banked as EDN from `block-tree` before deletion.
- ONE-SHOT law form confirmed: the committed harness
  (`test/app/anatomy_test.clj`, JVM) asserts interpreter-output ==
  committed goldens — feasible because the whole interpreter vocabulary is
  `.cljc` (`assembly_adapter.clj` already compiles assemblies server-side;
  `face_assembly_test.clj`/`face_primitives_test.clj` are the standing
  JVM-golden precedent). Inputs the fixtures must carry: view, wears,
  metrics, headers, marks — all EDN data.

## V3 — wear generality: PASS after F3/F6 (full census)

Generic over the spec registry (a new entry propagates with NO edit):
`cluster.clj:362` (ensure-master! over specs) · `face_projection.clj:1010,
1042,1056,1143` (serve joins automatically — §0's Workshop-door claim
verified) · `matter_room.cljc:57-80` (room minted from `master-ids` — §0
verified) · `material_truth.clj:86,161,167,424` ·
`material_portal.clj:725,832,1029` · `ground.cljs:2441`
(floor-binding-rows) · `:3565` (drillAll) · `:367` (halo spec lookup via
`by-facet`) · `:927` (`drill-master-id` fallback `(first master-ids)` —
specs are order-bearing; APPEND `fm:anatomy`, never prepend) ·
`material_inspector.cljc:23,70` (prefix-generic).

Hardcoded (the true non-generic surfaces):
1. `facet_masters.cljc:14` specs — named touch ✓
2. `ground.cljs:206-228` `resolve-material-wears` (seven hand-wired
   branches) — named touch ✓
3. `cluster.clj:340-420` explicit grammar migrations (base ensure is
   generic; activations are per-master, the `:378/:408` pattern) — named
   touch ✓
4. `ground.cljs:344-347` `block-wear-census` + its test literal
   `material_portal_test.clj:1191-1193` — **F3, the fourth touch**
5. `material_portal.cljc:397-403` `worn-five` (recipe naming) — **F6**
6. `material_portal.clj:542-556` `chrome-facets` `[:attention :foldable
   :text-body]` — checked and CLEARED: the portal chrome is a pre-named
   wall (§4), not a block; excluding-by-naming stays honest.

## V4 — exact pins, per phase: PASS (all re-derived on disk)

| pin | disk | phase duty |
|---|---|---|
| four act endpoints exact, no preview endpoint | `material_portal_test.clj:1123-1137` ✓ | stays green (W4) |
| episode + relation-kernel SHA-256 | `:1203-1208`; recomputed this session: `e1f1836f…` / `ab283b47…` — both match | P2 re-cuts episode's in the same change as the `summon-argv` touch |
| wear-census string literal | `:1191-1193` ✓ | P1 re-cut with F3's vector edit |
| eight import owners (fs scan) | `:483-502` ✓ | stays exactly eight — anatomy rides `imp:fm:` (`object_container.clj:326-328`) through `facet_master.clj`'s existing builder |
| 17 questions | `:263-264,1278-1282` ✓ | untouched (W5 section ≠ question) |
| portal no-write scan (call forms) | `:2147-2162` ✓ | W5's section composes reads only |
| registry↔ground verb equality | `binding_dispatch_test.clj:997-1001` ✓ | zero `register-verb!` changes |
| frozen gesture grammar | `:915-929` ✓ | untouched |
| floor-reserved + durable exact sets | `:148-161` ✓ | untouched |
| 16 probe receipts | `:632-672` ✓ | untouched |
| one-place censuses + branch budget | `:975-995`, `:933-938` ✓ | part↔pixel highlight rides `register-action!` (`ground.cljs:532` precedent), never a dispatch branch |
| 21 table rows | `material_truth_test.clj:461-485` ✓ | anatomy/invocation carry no binding rows ⇒ count holds |
| deterministic-time census | `:612-628` ✓ | honest clocks only (cluster migrations take `core/now-ms` — `cluster.clj:408-420` precedent) |
| reservation one-place censuses | `space_material_test.clj:134-193` ✓ | untouched |
| G21 exact read surfaces | `face_arsenal_test.clj:194-213` ✓ | §6 P2's face_projection constraint matches it |
| fail-closed inventory + receipt floor `{:test 397 :assertions 5416}` | `test_runner.clj:226-250`, `:156-160` ✓ | `anatomy_test` must be classified or the runner throws |
| versioned seam scan | `reply_to_block_test.clj:208-221` ✓ | P2 keeps the literals present |
| HEAD-dynamic suites | `test/app/server/rama/{git_spine_gate_test,code_atoms_test}.clj` ✓ (path hint drift) | re-run at committed HEAD |

## V5 — §0/§3 claims vs disk: PASS

Every §0 ruling re-derived: interpreter exists and is total/pure/registry-
validated (`face_assembly.cljc:330,526` read whole; error path
registry-independent `:266-300`); registry at `face_primitives.cljc:934`
EXACT; adapter generic + P6 precedent verbatim
(`CONTRACT_P6.md:39-52` ✓); deviate master branch →
`import-candidate!` (`server_jetty.clj:1357` ✓, generic over registered
masters); membrane previews exactly shared-master candidates
(`ground.cljs:3254-3310` — overlay replaces ONE `by-id` entry; instances
untouched; stale-base falls to truth `:248-261`); room auto-mint + serve
auto-join verified (V3); no model flag in `summon-argv`
(`episode.clj:871-884` read verbatim — `--session-id|--resume`, `-p`,
`--output-format stream-json`, `--include-partial-messages` and nothing
else); consult seams exist exactly where named (`run-episode-turn :876`,
prompt composition `:1036-1045`, `narrowed-portal-open
reply_to_block.cljc:16`, request body fields `:48-61` carry no
model/effort today; paste verbatim-insert `ground_edit.cljc:67-71`;
`install-paste-handler! mouse.cljs:39`, ground branch `:54`). W1's import
claim verified (`imp:fm:` routing + eight-owner census). W8's continuity
targets exist (`ge` unit-id-keyed state; `!preview` end-before-flip is new
work, correctly assigned to ground.cljs). W9's instrument verified
(`:1819-1872`, bar 52 literal at `:1861,1870`). Locator drift logged (all
substance intact): `rect_tree.cljc` lives under `client/workspace/`;
block-tree sub-pins drift ±10–20 lines (w/h at `:640-642`, box at
`:723-741`, root at `:860`); `material-request!` at `:630`; conflict
predicates `:537-561`; routes at `:2035/:2048/:2061` inside the pinned
range. No unresolved contract/disk conflict remains.

## V6 — the CLI seam: PASS (receipt in F7)

`claude` 2.1.220: `--model <model>` and `--effort <level>` (`low, medium,
high, xhigh, max`) both exist. Nothing fabricated; the wiring re-verifies
on the implementer's box per V6 as written.

## Coherence audit: PASS

- **Gate partition sums**: P1 = G1–G5, P2 = G6–G10 — exactly G1–G10, no
  orphan (the space-as-entity G7 class cannot recur here). Environment/actor
  owners: G3/G4/G7/G9 name the isolated rig + headed browser; owner = the
  implementing context per the phase cadence (halo P1's G2 precedent,
  environment attested first); Sid's wear is explicitly NOT a gate ✓.
- **Traps↔falsifiers**: G5 aims T1/T2/T3/T4/T11 + W8; G10 aims T5–T10 —
  all eleven covered.
- **Laws↔fences**: every W-law's files sit in the right phase allowlist
  (W1→anatomy_material+facet_masters P1; W2→face_primitives(+face_assembly
  conditional) P1; W3→ground+anatomy_test+cluster P1; W4→no new surface;
  W5→material_portal.cljc/.clj+face_projection P2; W6→ground+face_wiring
  P2; W7→threaded P1 · foldable/invocation/ground_edit/jetty/episode/
  reply_to_block P2; W8/W9→ground). P1 needs ZERO server_jetty edits and
  the console-lane loop works against the existing generic
  deviate/activate/rollback routes — verified generic.
- **Specimen-in-room render path executable**: the room is the shipped
  `?drill=` lane (`drill-conversation-id :67-72`, install params
  `:3645-3647`); post-cutover every block in the room renders through the
  interpreter; a synthetic specimen slot can also ride
  `register-face-instance!` (the halo-vi pattern `:432-488`); preview flips
  the shared master through the membrane, so the specimen previews the
  candidate by construction; part↔pixel highlight rides scene-descriptor
  actions (`register-action! :532`).
- **Stop clauses**: (a)/(b) remain live and correctly shaped; (c) resolved
  positively by F7 (duty unchanged).
- **§10 residue routing**: consistent; the armed matter-room falsifier is
  untouched by this package's own drives.

## Receipts

- **Read in full**: `face_assembly.cljc` (569) · `face_primitives.cljc`
  (959) · `rect_tree.cljc` (426) · `facet_masters.cljc` (58) ·
  `facet_material.cljc` (554) · `threaded_material.cljc` (52) ·
  `reply_to_block.cljc` (68) · `ground.cljs` `:1-2620` + `:3200-3755`
  (every contract-pinned region incl. block-tree whole; the unread stretch
  `:2620-3200` is the dispatch/pointer machinery, covered by its own
  standing suite pins re-verified above).
- **Targeted reads with line receipts**: scene_store, face_wiring,
  server_jetty, episode, cluster, facet_master.clj, object_container.clj,
  face_projection.clj, material_portal.clj/.cljc, matter_room.cljc,
  positioned/foldable materials, mouse.cljs, ground_edit.cljc,
  block_edit(.cljc/_wiring), bin/land, CONTRACT_P6.md, and the six pinned
  test files.
- **Greps run**: `block-tree` callers (ONE: `rebuild-block! :1214`) ·
  compile/apply-assembly wearers (assembly_adapter · scene_store ·
  face_wiring) · facet-set hardcodings (V3 census) · `"fm:` literals ·
  census literal in tests · SHA-256 recomputation of both pinned files ·
  `claude --help`.
- **Taken on faith** (each with its owner): the live 52ms p95 (G4/G9's own
  duty; instrument verified) · shadow-compile 0-warnings (G1's duty) · the
  halo GATE_P1 five-findings content behind §10's routing (file exists;
  say-lane tenancy not re-read) · Sid's verbatim LOG strands behind
  DIRECTION (capture law trusted).

## P1 implementer starter

Use this as the whole next-session prompt:

> smalltalk-ui-vm P1 implementation. Fresh context, one implementer context
> for the whole phase. Read the repository CLAUDE.md, then
> `docs/current-mental-model/build/smalltalk-ui-vm/CONTRACT.md` (binding;
> amended post-validation 2026-07-30 — F1–F8 are folded in),
> `DIRECTION.md` beside it, `NOW.md` STANDING, and
> `docs/current-mental-model/decisions.md`. There is deliberately no
> PLAN.md; `VALIDATION.md` (same directory) carries the behavior-census
> walk and the presence/view-key censuses — use them. Build all of P1 under
> CONTRACT §6 in this one context: `anatomy_material.cljc` (grammar +
> validators + seed + floor), the block primitives cut from `block-tree`'s
> own code (carry its OWN `wrap-lines` into the `.cljc` vocabulary — not
> `rt/wrap-line`; keep ONE wrap truth with the copy path), the
> compile-per-(master,revision) cache, `resolve-material-wears` +
> `block-wear-census` gain `:anatomy`, the fm:threaded second grammar
> version + cluster migrations, byte-identity per amended G2 (normalization
> named there), then DELETE `block-tree` (sole caller `rebuild-block!
> :1214`), goldens banked first. Treat W1–W9, T1–T11, the §6 fence, the
> zero-diff pins, and §9 stops as binding; manifests bind on substance.
> Run G1–G5 including the fresh in-phase falsifier and the isolated-rig
> console-lane drive (worktree, `LAND_CLUSTER=0 LAND_PINNED=1`, own port
> per the :8093–:8098 ladder, `env.clj` symlinked blind, never read). Write
> `build/smalltalk-ui-vm/P1.md` receipts (diff-derived file list,
> sum-checked against §6) + a newest-first NOW entry. Do not commit or
> push. A genuine policy conflict stops for a Sid/Fable ruling.

Package state after this validation: **CONTRACT amended in place (F1–F8),
OPEN for P1 implementation.** This verdict authorizes that one implementer
context; it claims nothing about G1–G5, closure, or commit authority.
