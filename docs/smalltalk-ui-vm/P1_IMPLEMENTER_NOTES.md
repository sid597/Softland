# P1 implementer notes — terrain digest + settled design decisions

Banked 2026-07-30 by the first P1 implementer context (Fable), handed off at
~270k context on Sid's call BEFORE any code was written. The tree is clean —
no source or test file changed; only this file + a NOW entry landed. A fresh
implementer context boots from CONTRACT.md + VALIDATION.md + THIS file and
starts writing code immediately; the decisions below were derived against
disk this session and do not need re-derivation (spot-check pins as you touch
them; manifests bind on substance).

## A. Terrain digest — verified on disk this session (HEAD c9a18ef, code db8e21d)

**The engine** (`face_assembly.cljc`, read whole):
- `compile-assembly registry assembly` → `{::status :ok ::name ::plan}` or
  `{::status :error ::errors}`; validates V1–V7 (envelope; node = :prim XOR
  :each; :prim must be a fn in the registry; props map, bind refs top-position
  only; V4 guard = NO lists/symbols/sets/chars anywhere; grammar must be `0`;
  root must be a :prim; ≤1000 nodes). Closes builder fns into the plan once.
- `apply-assembly compiled data view-ctx` → resolves props (`{:bind [path]}`
  via `get-in` against the CURRENT data context; missing → nil + counted),
  builds post-order (children handed to the builder), stamps EVERY node
  `[:data :assembly/src-path]` (skips nodes already carrying one), does ONE
  `rt/resolve-layout` at root, then `stamp-root` MERGES into root `:data`:
  `{:view-instance … :address … :assembly/content-h … :assembly/apply-report
  {:items-without-id :binds-missing}}`.
- `:each` expands to a FLAT vector of SIBLINGS (`expand-slot`) — this is the
  mechanism for both presence-gating (0-or-1-item seqs) and multi-node parts.
  Context DESCENDS to the item: binds inside a template resolve against the
  ITEM, so items must carry whatever the template binds. Item `:id` → id
  segment, else index (counted in apply-report; harmless — report is
  normalized at root).
- Builder interface `(fn [ctx props children] → rt-node)`; ctx =
  `{:id :prim :view-instance :address :geom}`. **Builders choose their own
  node ids** (ui-panel-group precedent) — our block primitives emit
  block-tree's EXACT ids and ignore ctx id.
- Interpreter-read props: `:id` overrides the derived id segment; `:child-w`/
  `:child-inset` narrow subtree content-w. Harmless to us.
- `error-card-node` is registry-independent; `invoke-builder` try/catches.

**rect_tree.cljc**: `layout-children` passes `:layout`-less nodes through
UNCHANGED (negative offsets survive). `rt-node` defaults: `:data nil`,
`:actions {}`, `:children []`, `:text []`, `:clip? false`, `:style {}`.
`resolve-layout` recurses via mapv — structurally identity for layout-less
trees. `rt/wrap-line` is word-boundary wrap — a DIFFERENT algorithm from
ground's `wrap-lines` (greedy, hard-break, trims). NEVER swap them.

**block-tree** (`ground.cljs:599-896`, read whole; sole caller inside
`rebuild-block!` at `:1214`):
- Signature: `[unit-id {:keys [text caret focused? refusal selection]}
  machine? hover? notice {:keys [font-size char-advance line-h]} wrap-col
  headers msel boundary? gsel? placement-derived? wears gold-marks
  silver-marks]`. PURE — reads no atoms.
- lines = split text on `#"\n" -1`; machine? ∧ wrap-col → `wrap-lines`
  (ground's own greedy wrap, `:555-572`); headers PREpended. n = count;
  max-len = `(reduce max 1 (map count lines))`; w = max-len·advance + 2·pad;
  h = n·line-h + 2·pad; pad = `:attention/hit-padding`.
- ops: per line `text-op` (y = i·line-h + fs BASELINE); color header→tint
  (`:provenance/tint`) / machine→`dim` / else `fg` (ground consts
  `fg=[0.92 0.92 0.94 1.0]` `dim=[0.55 0.58 0.62 1.0]`
  `err-col=[0.95 0.45 0.40 1.0]` `amber=[0.92 0.75 0.35 1.0]`, `:179-183`).
  Header ops MERGE p-stamp (provenance :fold-header :text-color
  :block/content) and get `:material/contributions [p-stamp f-stamp]`
  (f-stamp = foldable, site `:noise-header` when i=0 else `:prose-header`,
  role :header-copy, slot :block/fold-header-text). Body ops (machine ∧
  ¬header) MERGE text-body stamp (:wrapped-body :wrap-policy
  :block/content-flow).
- Children (`extra`), IN ORDER: ① sel wash — per-line rects id
  `ground-sel-<line-i>` `:rect`, x=(s'-start)·advance, y=i·line-h,
  w=max(2.0,(e'-s')·advance), h=line-h, bg [0.35 0.5 0.8 0.3]; range =
  (focused? ∧ selection) ∪ msel (msel via `lines-offset` over RENDERED
  lines). ② gsel `:ground-gsel` :rect (-pad,-pad,w,h) border 1.5
  [0.55 0.65 0.9 0.8] bg [0.35 0.5 0.8 0.10]. ③ decorations via
  `facet-material/compose` of [rail-when-machine?, box-when(focused?∨hover?)]
  — rail `:ground-mark` :rect (-pad,-pad,2.5,h) bg tint, :data = its stamp
  (provenance :machine-rail :provenance-marker :block/decorations); box
  `:ground-box` :rect (-pad,-pad,w,h) style {border-width/border-color/bg
  from attention wear}, :data = its stamp (:attention-box :attention-border
  :block/decorations). Compose sorts by wear priority — provenance=10,
  attention=20 (verified) → [rail, box]. mapv :value of :contributions.
  ④ caret `:ground-caret` :rect (col·advance, line·line-h, 2, line-h) bg
  [0.95 0.95 0.95 1.0], when focused? ∧ caret-lc (`ge/caret->line-col`).
  ⑤ refusal `:ground-refusal` :text-run (0, n·line-h, w, line-h), text-op
  "⟂ edit refused: <name-or-str>" err-col. ⑥ notice `:ground-notice`
  :text-run (0, (n + refusal?1:0)·line-h …) amber. ⑦ boundary
  `:ground-episode-boundary` :text-run (0, −1.6·line-h …) "— fresh session —"
  tint, :data = provenance stamp (:episode-boundary :boundary-label
  :block/prelude). ⑧ conflict lint (`composition-lint-nodes :534`, sole
  other caller = block-tree) — per conflict i: `ground-material-conflict-<i>`
  :error-card (0, h+i·line-h, w, line-h) bg [0.24 0.07 0.08 0.98] border 1.0
  [0.9 0.3 0.3 1.0], text-op "material conflict · <type-name>" err-col,
  :data = the conflict map. ⑨ gold mark `:ground-wish-mark` :text-run
  (0, −(boundary? 2.8 : 1.4)·line-h, max(w,42·advance), line-h) — text-op
  "⌁ <text>[  +n-1]" amber from FIRST gold-mark. ⑩ silver
  `:ground-silver-mark` :text-run (0, −(bnd?1.4:0 + gold?1.4:0 + 1.4)·line-h,
  max(w,48·advance)…) "≈ machine guess · <text>[  +n-1]" dim. ⑪ fold-header
  hits (when nh>0), per `fold-sections` (`:185-191`:
  [{:section :noise :fold-key :noise?} {:section :prose :fold-key :prose?}])
  × i: `ground-fold-header-hit-<section>` :hit-area (0, i·line-h, w, line-h),
  :data = foldable stamp (:fold-header-hit :fold-toggle :block/hit-area) +
  {:address uid :material/claim {:claim/subject uid :claim/site
  :block/fold-header :claim/facets [:foldable] :claim/args {:section
  :fold-key}}}. Appended LAST (reverse hit-test).
- Root: `rt/resolve-layout` of `rt-node :ground-block :text-run
  {:x 0 :y 0 :w (max w char-advance) :h (max h line-h)} :text ops :data (…)
  :children extra`. Root data = {:address uid} + attention stamp (:hit-box
  :hit-target :block/hit-area) + :material/claim {subject uid, site
  machine?→:block/machine-hit-area else :block/user-hit-area, facets
  [:attention :positioned], args {}} + when placement-derived?
  :material/positioned = positioned stamp (:derived-placement
  :placement-default :block/placement) + when ¬machine? :material/threaded =
  threaded stamp (:send-adoption :column-adoption-policy
  :block/thread-adoption).

**rebuild-block!** (`:1142-1222`): inputs census = view (`ge/block-view` st
uid truth :optimistic → {:unit-id :text :caret :selection :focused?
[:refusal]}; display override from `run-view` for machine run blocks),
headers (run-view), notice, hover?, msel, bnd?, gsel?, gold-marks (built
from experience marks + `mark-preview`), silver-marks, metrics
(`{:font-size :char-advance :line-h :viewport}`, `:163-170`), wears
(`wears-for uid`). 16-el `sig` `:1208-1210`; skip when = ∧ slot exists;
else build + `upsert-block-slot!` + store :w/:h/:render-sig.

**Wear machinery**: `resolve-material-wears :206` (seven hand-wired
branches — gains `:anatomy`); `current-material-wears` (identity-keyed
cache `!wears-cache`); `wears-for :289` (instance tier, identity-stable for
non-deviant subjects); `served-material-value :248` (ONE door; preview
overlay when `identical?` base). Preview membrane: `preview-candidate!
:3254` (compile-source against real spec → refuse or overlay ONE master,
revision-id `preview:<master>:<hash>`, `:facet-master/preview? true`),
`end-preview! :3312` (reset + `re-derive-material!`), `preview-state :3323`
(:applied? = base identical). Material watch `:3663-3674`: on
`material-render-state` diff → reconcile!/rebuild-material-sites! +
refresh-material-error! + recompute-binding-conflicts!. `re-derive-material!
:3238` is the callable form.

**Pointer machine** (`:3090-3186`): phases `:idle`/`:pending`/`:active`;
`pointer-up!` resets to :idle then dispatches tap or verb :end.

**Copy path**: `machine-visual-lines :1112` (headers + folded display +
wrap-lines) → `machine-sel-text` — MUST call the same moved wrap fn.
`run-view :1079` (fold-derived display+headers) stays in ground.
`render-provisional-slot!` also calls `wrap-lines` + `text-op`; halo render
and `refresh-material-error!` call `text-op`; `lines-offset` used by
machine-sel + block-tree. These three helpers MOVE to face_primitives.cljc;
all ground call sites switch to the fp/ alias (no cycle: fp requires only
rect-tree + design-tokens).

**Materials**: provenance floor = composition-form (grammar 1, merge
:append, priority 10, tint [0.62 0.66 0.76 0.6]). attention floor =
reply-bindings-form (grammar 3; merge :append, priority 20, hit-padding
8.0, border-width 1.0, border-color [0.45 0.52 0.66 0.55], background
[0 0 0 0]). Grammar-version precedent: each version keeps its own
declaration forever; floor-form points at the NEWEST form (attention
pattern). threaded today: ONE grammar `0`, ONE key
`:threaded/column-adoption-reach-lines 3.0`.

**Cluster** (`cluster.clj:330-439`): `facet-materials-ingest!` iterates
`facet-masters/specs` generically via `ensure-master!` (fm:anatomy joins
with ZERO edits — seed = default-form = floor-form, v0 import + activate);
explicit grammar migrations ride `facet-master/ensure-active-source!`
(`facet_master.clj:374` — import exact bytes + activate) with slug'd
request-ids and HONEST `core/now-ms` (the R3 law — never a new constant).
Add ONE migration entry for threaded's second grammar version beside the
strict-bindings-migrations block.

**Test pins verified**: `material_portal_test.clj:1191-1193` pins the
census literal via `str/includes?` on
`"[:provenance :attention :foldable :positioned :threaded :text-body]"` —
re-cut to the `:anatomy`-appended literal in the SAME change as the source
edit. `:1203-1208` = episode + relation-kernel SHA pins (both ZERO-DIFF in
P1 — do not touch those files). `:1123-1137` four endpoints — untouched.
`test_runner.clj`: `assert-inventory!` is fail-closed — `app.anatomy-test`
must join `pure-namespaces` (it is pure: no runtime, goldens only);
`full-receipt-floor {:test 397 :assertions 5416}` may grow, never shrink.

**Rig mechanics**: `dev.cljc` hardcodes port 8080 — the halo rig ran
`:8097` (see `build/halo/P1.md` for the exact recipe: separate worktree,
`LAND_CLUSTER=0` in-memory IPC boot (`cluster.clj:50` reads it),
`LAND_PINNED=1` = no shadow watch, serve pre-compiled assets → run
`clj -M:dev -m shadow.cljs.devtools.cli compile dev` in the worktree first;
env.clj symlinked blind, removed at teardown; fresh headless Chrome with
remote-debug port). Read halo `P1.md`/`GATE_P1.md` for the port-override
mechanism before staging G3 — this session did not reach it. G3's "restart
restores the active pointer" on an IPC rig most plausibly = page-reload
re-serve while the rig JVM stays up; verify against the halo precedent and
disclose the reading in the receipt.

## B. Settled design decisions — do not re-derive

1. **Presence gating through `:each`.** Grammar v0 has NO conditional form;
   every part gates as `{:each [:parts <part-id>] :template {:prim … :props
   compiled-binds}}` over a 0-or-1-item (or N-item) seq the apply step
   builds. `:always` parts still ride the same shape (uniform compiler).
2. **The root part IS the char-grid.** block-tree's root carries the text
   ops directly, so a separate body child would break structure. The
   `:block-root` primitive does: split → machine wrap (fp/wrap-lines, the
   MOVED greedy fn — never rt/wrap-line) → header prepend → per-line ops +
   color policy + the three stamp families → root w/h law + min-clamps →
   root :data (address/claim/conditional stamps from bound view flags) →
   `:children` = the already-built child nodes handed in by the engine.
   Props: text/wrap-col/headers/machine?/placement-derived? via [:view …];
   tint via [:wear :provenance :provenance/tint]; pad via
   [:wear :attention :attention/hit-padding]; stamps via the minted-stamp
   engine-internal bind (below). fg/dim constants live in the primitive
   (cut from ground; ground's copies stay for halo/provisional use until P2
   — or move to fp and alias, implementer's choice, ONE definition).
3. **Anatomy row shape** (`anatomy_material.cljc` owns it):
   `{:part/id kw, :part/prim kw, :part/when kw, :part/props {slot →
   literal | [:wear facet key] | [:view key]}, :part/order n,
   :part/stamp stamp-row | {name → stamp-row}}` where stamp-row =
   `{:stamp/facet :stamp/site :stamp/role :stamp/slot}`. The multi-stamp
   MAP shape exists for the root/char-grid (header provenance + fold-noise
   + fold-prose + body text-body + root hit-box/positioned/threaded) —
   a disclosed judgment call (contract letter shows the single row).
   `:anatomy/defs {name → [part…]}` + prim `:sub-anatomy` with
   `:part/def name` — compile-time inline expansion, defs may not contain
   :sub-anatomy (one level, validated).
4. **Prop compilation**: `[:wear f k]` → `{:bind [:wear f k]}`, `[:view k]`
   → `{:bind [:view k]}`; minted stamps ride an engine-internal
   `{:bind [:stamp]}` / `{:bind [:stamps]}` the COMPILER emits (sanctioned:
   the assembly bind stays an engine internal the anatomy compiler
   targets). Items carry `{:wear wears-map :view view-model :stamp/:stamps
   minted :id part-id}`; multi-node part items carry their per-element
   fields instead (below).
5. **apply-data** (pure, in anatomy_material.cljc): `(rows, wears, view,
   address) → data-context`. Evaluates the presence vocab against view;
   mints stamps via `facet-material/contribution-stamp` (wear = wears at
   the stamp row's facet, subject = address); REPRODUCES
   `facet-material/compose` over same-slot PRESENT contributions (today:
   :block/decorations rail+box) — its `:conflicts` feed the lint part's
   items; slot ORDER stays `:part/order` (compose order == material order
   in the seed — "order as material is the point" per VALIDATION; the
   seed's rail(order)<box matches priorities 10<20 — goldens enforce).
6. **Presence vocabulary** = the contract's 14 keys PLUS
   `:focused-or-hover` (the attention box's `(or focused? hover?)` — the
   enumerated 14 cannot express block-tree's own condition; the census
   authority is "block-tree's own"; F-class precision gap, DISCLOSE
   prominently in P1.md + NOW for the gate). Glosses (documented in the
   vocab): `:focused` ≝ focused? ∧ caret-lc resolvable (the caret
   condition); `:header` ≝ header-count > 0; `:has-selection` ≝ unified
   sel-range present ((focused?∧selection) ∪ msel, offsets via
   fp/lines-offset over RENDERED lines); `:user` ≝ ¬machine?; `:machine`;
   `:boundary`; `:has-refusal`; `:has-notice`; `:has-group-sel`;
   `:has-gold-marks`; `:has-silver-marks`; `:has-conflicts` (compose
   output non-empty); `:always`.
7. **Multi-node parts** ride `:each` over item seqs the VIEW-MODEL carries
   (closed view keys): sel wash over `:sel-spans`
   [{:line :col-start :col-len :id}] (id = line index → engine segment);
   fold hits over `:fold-headers` [{:i :section :fold-key :id}]; conflict
   lint over apply-step-computed conflict items [{:i :conflict :id}].
   Their primitives emit block-tree's exact per-item ids.
8. **View-model** built in ground.cljs (cut from block-tree/rebuild code);
   the CLOSED view-key vocabulary lives in anatomy_material.cljc beside
   the grammar. Keys (final list cut while writing; every rebuild census
   member reachable): :text :wrap-col :headers :header-count :machine?
   :user? :hover? :focused? :caret-line :caret-col :notice :refusal
   :boundary? :gsel? :sel-spans :fold-headers :gold-mark-text
   :silver-mark-text(+counts) :line-count :max-len (if precomputing
   avoids double work — implementer's judgment; wrap truth stays the ONE
   fp fn wherever called). Note: sel-spans/lines-offset need the RENDERED
   lines — computing them calls fp/wrap-lines again on selection-carrying
   blocks only; acceptable, G4 verifies.
9. **Equality relation (G2 comparator)**: strip `:assembly/src-path` from
   every node's :data; on the ROOT also strip `:view-instance`,
   `:assembly/content-h`, `:assembly/apply-report` (assert all present
   first). `:address` is NOT stripped — set view-ctx :address = unit-id so
   it EQUALS block-tree's root :address byte-for-byte. Normalize
   empty-after-strip :data {} → nil (rt-node default is nil). JVM golden
   compare additionally normalizes ALL numbers to double (CLJS pr-str
   drops ".0" → Longs on read; Long ≠ Double under =). IEEE arithmetic is
   identical across JVM/JS so values agree once coerced.
10. **rebuild-block! cutover**: view-ctx `{:view-instance (block-vi uid)
    :address uid :geom {:font-size :char-advance :line-height (:line-h m)
    …}}`. Compile cache atom `{[master-id revision-id] → compiled}` (prune
    to a handful; preview mints its own revision-id key and the membrane
    just works — preview-candidate! is fully generic over specs, verified).
    Anatomy wear = `(:anatomy (wears-for uid))`; malformed/absent falls to
    the FLOOR at `resolved-wear` (spec floor-form = the seed) — the engine
    error card remains only for floor-constant bugs. render-sig UNCHANGED
    (wears already element 11 — T11 satisfied).
11. **W8 wiring**: the material watch defers its re-derive while
    `(:phase @!pointer)` ≠ :idle (set a pending flag; `pointer-up!` runs
    the deferred re-derive AFTER gesture end); the watch also explicitly
    ENDS a preview whose base is no longer the served value (reset
    `!preview` + console note) before re-deriving. Caret/selection/focus
    are already unit-id-keyed atoms — they survive rebuilds by
    construction; assert in anatomy_test via two applies under different
    revisions with the same view.
12. **threaded second grammar** (key `1`): material-keys = v0's key +
    `:threaded/edge-rail-width` (non-negative) + `:threaded/edge-rail-color`
    (rgba) + `:threaded/edge-indent` (non-negative). New
    `thread-edge-form`/`thread-edge-source` consts (defaults e.g. width
    2.0, color [0.45 0.62 0.85 0.7], indent 10.0); v0 default-form +
    grammar entry BYTE-STABLE; floor-form flips to the new form (the
    attention precedent). Cluster: one `ensure-active-source!` entry,
    slug "threaded", honest now-ms.
13. **Thread-edge primitives** (for the G3 CANDIDATE, not the seed):
    `:thread-edge-rail` (vertical rect at x = −indent, y = −pad or 0 —
    implementer cuts the exact geometry, it is NEW pixels; w/color bound
    to the threaded keys) + `:thread-indent` (the horizontal connector
    tick). Candidate = seed + two rows `:part/when :user` binding
    [:wear :threaded …]. Driven console-lane through deviate (master
    branch of /api/matter-room/deviate or the __portal deviate hand —
    read face_wiring.cljs :386-393 before staging) → membrane preview →
    activate → rollback.
14. **facet_masters.cljc**: APPEND anatomy spec LAST in `specs` (order-
    bearing; `drill-master-id` falls back to `(first master-ids)`).
15. **Census edits**: `resolve-material-wears` gains an `:anatomy` branch;
    `block-wear-census` vector gains `:anatomy` at the END; the
    material_portal_test literal re-cut in the same change.
16. **Goldens**: fixture inputs = the FULL block-tree argument census
    (view map, machine?, hover?, notice, metrics, wrap-col, headers, msel,
    boundary?, gsel?, placement-derived?, wears (REAL floor wears —
    build them JVM/CLJS-side from the specs' code-floor, they are .cljc),
    gold/silver marks). Coverage: plain user block · focused+caret ·
    selection · machine+wrap+headers+folds · msel · hover box · gsel ·
    refusal · notice · boundary · gold+silver marks · compose conflict
    (synthetic priority-tie wears) · empty-text min-clamp · multi-line
    wrap hard-break. Generation: temporary browser hook (rig console)
    dumps `{:inputs … :tree (block-tree …)}` EDN per fixture; files
    committed under `test/app/fixtures/anatomy/*.edn` (contract-named
    deliverable class, DISCLOSE in P1.md); the committed JVM harness
    (anatomy_test) runs compile+apply over :inputs and compares per (9).
    The temporary hook + block-tree die together pre-receipt
    (grep-negative covers both).
17. **G2 live corpus**: on the rig, `window.__ground.blocks()` post-merge
    keys, count asserted; a temporary `__ground.compare()` dev hook runs
    both paths per uid in ONE browser context and reports per-uid
    diff/match; receipt banked in P1.md; hook deleted with block-tree.
18. **Prim vocabulary ownership**: anatomy_material does NOT require
    face-primitives (shared/ stays clean of client-workspace deps);
    it owns a closed `part-prim-vocabulary` set; anatomy_test asserts
    vocabulary ⊆ registry keys. Refusals enumerate the legal set
    (teaching). DISCLOSE as a judgment call.
19. **Trap comments**: cite T-numbers in code comments at the load-bearing
    spots (T1 deletion, T2 no-dispatch, T3 keyword-only, T4 corpus verb,
    T5 preview, T6/W8 continuity, T7 material-not-code, T11 keying).

## C. The P1 implementer starter (context #2) — paste whole

> smalltalk-ui-vm P1 implementation, context #2. A prior implementer context
> read the full terrain and banked every settled design decision at
> `docs/current-mental-model/build/smalltalk-ui-vm/P1_IMPLEMENTER_NOTES.md` —
> read the repository CLAUDE.md, then CONTRACT.md (binding, F1–F8 folded in),
> the NOTES file (§B decisions are settled — do not re-derive; §A digest
> carries verified pins; §D lists the few unread spots), VALIDATION.md's
> behavior-census walk, and NOW.md STANDING. Skip broad re-reading: read
> `ground.cljs:599-896` (block-tree, the transcription source) plus targeted
> pins as you touch them. Build all of P1 under CONTRACT §6 in this one
> context: anatomy_material.cljc (grammar+validators+seed+floor+compiler+
> apply-data per notes §B), block primitives cut into face_primitives.cljc
> (block-tree's OWN wrap-lines moved, never rt/wrap-line), ground.cljs
> cutover (census gains :anatomy, compile cache, W8 wiring, block-tree
> deleted, goldens banked first), threaded second grammar version + cluster
> migration, anatomy_test.clj + runner inventory + the material_portal_test
> census-literal re-cut. Treat W1–W9, T1–T11, the §6 fence, zero-diff pins,
> and §9 stops as binding; manifests bind on substance. Run G1–G5 including
> the fresh in-phase falsifier and the isolated-rig console-lane drive
> (worktree, LAND_CLUSTER=0 LAND_PINNED=1, own port per the :8093–:8098
> ladder — read build/halo/P1.md for the rig recipe first; env.clj symlinked
> blind, NEVER read). Disclose the pre-flagged judgment calls (notes
> §B.3/6/16/18) in `P1.md` receipts (diff-derived file list, sum-checked
> against §6) + a newest-first NOW entry. Do not commit or push. A genuine
> policy conflict stops for a Sid/Fable ruling.
>
> Context-budget directive, binding for this phase: build P1 whole in this
> context, and keep it lean — run compiles/suites/live drives so that raw
> output lands in files (read tails and verdicts only, never full logs into
> context); never re-read files you just wrote; bank raw G2/G3 evidence
> (byte-identity dumps, rig/browser logs) directly into P1.md/scratch files,
> not the conversation. Checkpoint your (estimated) token position at each
> task boundary. Note G2 is the one gate that CANNOT leave this context —
> both paths must be alive in one browser before block-tree dies, so goldens
> + byte-identity + deletion complete HERE. If this context passes ~200k
> before starting G3: STOP at the clean seam — G1+G2 green (byte-identity
> receipt banked, goldens committed, block-tree grep-negative), write P1.md
> through that point plus a NOW entry naming the seam — and G3/G4 run in a
> fresh context booting from CONTRACT §8, P1.md's receipts, notes §A's rig
> pointers, and build/halo/P1.md. G5's falsifier is a fresh context
> regardless (launch it as a fresh subagent from whichever context is live;
> smallest in-fence repair lands in that same live context). A receipt-clean
> seam beats a degraded finish; never grind past quality.

## D. What was NOT reached this session

- face_wiring.cljs `__portal`/deviate hands (read :386-393, :632 before G3).
- halo `P1.md`/`GATE_P1.md` rig recipe (port override, chrome flags,
  teardown) — read before staging the rig.
- foldable/positioned/text_body material floors (needed only for fixture
  wears; they are code — read at fixture-writing time).
- binding_dispatch_test one-place censuses — we add NO dispatch branches and
  NO verbs, so they should stay green untouched; verify at G1.
- No code, no tests, no rig, no gates were run. Task list (12 items) died
  with the session; CONTRACT §6/§8 carries the same structure.
