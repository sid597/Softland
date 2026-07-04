# View-MVP PLAN — WP-B2 Phase B2-P1 (implementation plan)

Status: derived artifact (NOT binding). On any conflict with
`build/view-mvp/CONTRACT.md` (v1.1 BINDING), `build/trail-view/CONTRACT.md`
(WP1, BINDING), or `decisions.md`, those govern; a genuine
two-readings-cannot-both-hold conflict is a stop-clause event recorded in §15,
never silently resolved here.

Self-routing: this is B2-P1 (plan) — required Opus 4.8 / high; ran on Opus 4.8
at effort max (>= high, Sid set it in-session) — no mis-route. Package gate:
every doc write is `file(1)`-checked to say "text", never "data" (standing NUL
trap). No raw non-ASCII glyph appears in this file; codepoints are written
`U+XXXX` (IMPLICIT_SPEC discipline), so no decode surprise can inject bytes.

Provenance: written against `build/view-mvp/CONTRACT.md` v1.1 + its
`IMPLICIT_SPEC.md` (incl. §8 addendum), `build/render-substrate-retro/{RETRO,
PRIMITIVES}.md`, `build/view-mvp/CHARSET_AUDIT.md`, `build/trail-view/
CONTRACT.md` (§n citations), CLAUDE.md (Missionary/Electric law + font recipe),
`memory/implementation-quirks.md`, and direct reads of the on-disk source
cited inline. All `file:line` citations below were read THIS session unless
marked "(agent)"; the agent-sourced object-container facts (§1a) were gathered
by one Explore pass over the 2,648-line OC module and its adapters.

---

## 1. Verification duties (CONTRACT §12 "BINDING pre-code") — RESULTS

All four settled with evidence before any plan design rests on them.

### 1a. (a) Import entry points + observable completion (READ, not memory)

The object-container (OC) import seam is **append-a-request-map-to-a-depot**;
the adapters build request maps (pure data) and a separate call appends them.
Completion is **poll/latch-based**, not a blocking "batch done" ack.

- **Markdown request builder:** `object-container.markdown-adapter/
  markdown-source-import-request [raw-text source-ref (opts)]`
  (`markdown_adapter.clj:462`; alias `source-ingest-request` `:516`). Returns an
  `:object-container/import-material` request keyed by `object-key`, with
  `:import/key`, `:idempotency/key`, `:material/fingerprint`. **There is NO
  path-taking markdown fn and NO markdown file walker anywhere** (agent, repo
  search) — md import is driven only from in-memory `raw-text`.
- **Transcript request builder:** `object-container.transcript-adapter/
  transcript-observation-import-request [obs (opts)]`
  (`transcript_adapter.clj:221`) — one observation -> one request. The per-file
  driver already exists: `dogfood.transcript/read-jsonl-observations`
  (`transcript.clj:557`) + `import-observations-into-object-container!`
  (`:1072`), and a live watcher `start-transcript-watch!` (`:1416`).
- **The append (the actual kernel write):** `object-container.runtime/
  append-object-container-request! [runtime request (ack-level)]`
  (`runtime.clj:90`); default `ack-level` `:append-ack` (`:91-92`) returns when
  durable, NOT when processed.
- **Observable completion (poll/latch, no push):**
  - Deterministic per-request latch: `await-object-container-decision`
    (`runtime.clj:314`, loops `read-decision` `:125` to timeout). Passing
    explicit `:ack` to the append makes `foreign-append!` block and return the
    decision — but does NOT cover transcript source-line completion (a separate
    downstream depot, `object_container.clj:1859-1862`).
  - Markdown completion PStates: `$$import-completions-by-key` (read via
    `read-import-completion` `runtime.clj:149`), `$$source-ingest-completions-
    by-ref`, `$$source-latest-by-ref` (`runtime.clj:31-33`).
  - Transcript file-level (reliable "file done"): `$$transcript-file-offsets`
    (`read-transcript-file-offset` `runtime.clj:264`) — done when
    `:last-byte-offset == (.length file)` (asserted `object_container_test.clj:
    749,812`), gated per-line by `$$transcript-source-lines-by-file` status in
    `#{:import-complete :parse-error-complete}` (`transcript_identity.clj:4`).
  - **KEY for gate 14 / trap 11:** the watcher latches on `await-...-decision`
    (or `:ack`) — a **bounded deterministic barrier**, NOT an fs poll-loop and
    NOT poll-as-proof. This is the discipline the quirks file demands.
- **Idempotency / convergent re-import:** deterministic content-addressed ids
  (`import-key = imp:md:<object-key>:<sha256(...)>` `markdown_adapter.clj:440-
  442`; `idempotency/key` defaults to it `:475-477`) + journal PStates
  (`$$decisions-by-idempotency` decl `object_container.clj:1693`, checked
  `:1786-1789`; `$$import-completions-by-key` checked `:1822-1828`). A
  byte-identical re-import **replays the accepted decision, writes no new rows**
  (proof: `object_container_test.clj:201-248` — re-ingest yields 1
  source-version, outline count unchanged). Backs gate 14 / E-12.
  - **CAVEAT (feeds §15 OI-3):** md `object-key` derives from `source-ref` **+
    source-hash**, so a *changed* .md mints a **new object** (not a revision).
    Only byte-identical rewrites converge. Transcripts converge per-line under a
    stable conversation object-key.
- **Runtime handle:** `start-object-container-runtime!` (`runtime.clj:7`) builds
  an **in-process `create-ipc` test cluster**; only test code holds one
  (`object_container_test.clj:712,761,787,820`). **No production/long-lived OC
  runtime is booted in the running server today** (feeds §15 OI-1).
- **Watch roots:** transcripts named as `~/.claude/projects/**/*.jsonl`
  (`transcript.clj:76-81`) but NOT repo-scoped; **no `docs/`/`vision/`/`.md`
  root constant exists** (feeds §11 config).

### 1b. (b) cljc-on-JVM spike — PASS (ruling 2.1 is buildable)

Wrote a throwaway pure `.cljc` under `src/app/client/workspace/`, required it
under `clojure -M:test`, deleted it (working tree clean). Result:

```
:SPIKE-OK {:utf16-len 4, :cp-count 3, :sanitized "a?b"}
```

Proves, on the JVM where the gates run: (1) a pure client-dir `.cljc` loads AND
executes via the `:test` classpath (`deps.edn` `:paths ["src" "resources"]` +
`:test {:extra-paths ["test"]}`); (2) reader conditionals resolve to `:clj`;
(3) surrogate-safe codepoint counting works (UTF-16 len 4 vs codepoint count 3
for a `U+1F534`-bearing string = edge case E-1), and sanitize substitutes
uncovered->fallback with count preserved (OP-2/OP-3/gate-4 core). The §12
pre-flagged policy fork (cljc-on-JVM classpath failure) **does not fire**.
`rect_tree.cljs` requires only `clojure.string` (`rect_tree.cljs:5`), zero js
interop -> its `.cljc` promotion loads cleanly.

### 1c. (c) Wheel-cascade order (trap 9) — mapped

`scroll.cljs:48` `cond`: `in-sidebar?`(`:50`) -> `in-agent?`(`:59`) ->
`flow-active?`(`:71`, comment `:70` "must check BEFORE chat") -> chat(`:87-95`)
-> `:else` editor(`:98`). **Trap 9 CONFIRMED concretely:** the editor `:else`
fires on `(or (not file-workspace?) ...)` (`:99`), which is TRUE in a trail-face
mode (a distinct local-world where `file-workspace?` is false) -> a trail face
falling through would silently steal editor scroll. **Insertion point:** a new
`trail-face-active?` clause **after `:85`** (after the `flow-active?` block,
before the chat clause), owning its own scroll atom.

### 1d. (d) shadow-cljs resolves the cljc rename — SETTLED

12 files require the ns `app.client.workspace.rect-tree` (grep) — addressing is
by **namespace**, which the `.cljs -> .cljc` extension change does not alter, so
none of the 12 requires change. No `.edn`/build file references the file path
(grep). shadow-cljs resolves builds by ns entry (`shadow-cljs.edn`), and `.cljc`
is a first-class source extension. The rename is transparent to every consumer
and to both compilers.

---

## 2. File-level change map (allowlist annotated with sites + op ids)

NEW (pure `.cljc` unless noted):
- `src/app/client/workspace/trail_face/sanitize.cljc` — OP-1..OP-3.
- `src/app/client/workspace/trail_face/text_face.cljc` — OP-4..OP-8.
- `src/app/client/workspace/trail_face/cards.cljc` — OP-9..OP-17.
- `src/app/client/workspace/trail_face/lanes.cljc` — OP-18..OP-20.
- `src/app/client/workspace/trail_face/scene.cljc` — OP-21..OP-24.
- `src/app/client/workspace/trail_face/wiring.cljs` — OP-29..OP-31 (ONLY cljs).
- `src/app/server/ingest_watchers.clj` — OP-36..OP-39 (outside all Rama
  modules).
- `test/app/client/workspace/trail_face_test.clj` — gates 1-13 (+4/5 assertions).
- `test/app/server/ingest_watchers_test.clj` — gate 14.
- `test/resources/trail_face/*.edn` — fixtures (§12).

RENAMED: `rect_tree.cljs -> rect_tree.cljc` (+ T-4 clamp only; §7).

AMENDED (bounded to the named edit):
- `combined_text.cljs:253` — trail-face text-ops branch (OP-41; §6.4).
- `editor_compute.cljs:300-303,456-464` — `<mode` selector already routes trail
  modes via `local-world-mode`; add `<editor-rects` cases + a `<trail-face-
  scene` flow mirroring `<sidebar` `:306-337` (OP-41; §5).
- `runtime/scroll.cljs:after 85` — trail-face scroll zone (OP-42; §1c).
- `runtime/mouse.cljs:437-465` — trail-face click routing + new
  `handle-trail-face-click!` (OP-43; §6.6).
- `runtime/workspace_actions.cljs:145-149,after 217` — mode branch + predicates
  (OP-40; §6.1).
- `runtime/render.cljs:23-36,41-63` — render-consumer atoms + compute-fn
  threading (OP-40/OP-41; §6.5).
- `runtime.cljs:306-321` — recompute-local-world! input + add-watch (OP-40;
  §6.2).
- `runtime/state.cljs:192` — new view-state atoms (OP-29; §6.2).
- `runtime/agent_flow.cljs:214-220,~304` — entry command (OP-44; §6.7).
- `src/app/file_viewer.cljc:6-9,after 133` — pull e/defns + WatchIngestEpoch
  (OP-32/OP-33; §10).
- `electric_flow.cljc:472-483` — thread new client atoms (OP-34; §10).
- `src/app/server/rama/util_fns.cljc:112-128` — `!ingest-epoch-atom` +
  `:mirrors` (OP-35; §10 — TWO-PART).
- `substrate/webgpu/renderer.cljs:1147,1187` — the two shape fns only (OP-27;
  §8).
- `resources/public/font_atlas.{json,png}` — regen (OP-28; §9).

UNTOUCHED (gate 17 / INV-16): `trail.cljs`, `dg_flow.cljs`, `sidebar.cljs`,
`settings_view.cljs`, `cmd_panel.cljs`, `draw-frame!` order, camera/zoom,
`use-persistent-render-target?` (`render.cljs:16`, stays false), buffer pools,
font backends.

---

## 3. Phase plan (what lands where; gate coverage)

- **B2-P3 (pure core + fixtures + gates 1-13, parallel-safe with WP1 impl):**
  all `trail_face/*.cljc` + `rect_tree.cljc` (rename + T-4 clamp) + fixtures +
  `trail_face_test.clj` gates 1-13, written & compile-checked. No WP1 dep
  (fixtures are contract-shaped). No wiring, no e/defns, no runtime.
- **B2-P4 (gates 1-13 green + substrate amendments §5):** run gates to green;
  land OP-27 (renderer shape fns) with diff-review notes; land OP-28 (atlas
  regen) -> gate 5; resolve §15 OI-2 (active atlas) with Sid before relying on
  first-light rendering. `rect_tree.cljc` clamp -> gate 6.
- **B2-P5 (integration; starts only after WP1 gates green):** Electric bridge
  e/defns (needs live WP1 wrappers), the 7-step wiring, the `<trail-face-scene`
  cached flow, scroll/mouse zones, entry command; gates 16, 17. Requires an OC
  runtime handle (§15 OI-1).
- **B2-P6 (watchers + epoch push + first light):** `ingest_watchers.clj` +
  `WatchIngestEpoch` + `!ingest-epoch-atom`; gate 14; then first light with Sid
  (gate 15 `MEASUREMENT_RAF.md`, `FIRST_LIGHT.md`, H1 clock).
- **B2-P7/P8:** impl + test validation (fresh, default-fail, line-cited), then
  Fable gate.

---

## 4. Pure-cljc face core (module boundaries; data-in/data-out)

Every fn is `(data, geometry) -> data`; no atoms, no js interop (INV-S2); the
one platform shim allowed is codepoint iteration (spike-proven, §1b).

- **`sanitize.cljc`**
  - `coverage-set [atlas-json] -> #{codepoint}` (OP-1; reads `glyphs[].unicode`,
    verified shape `font_atlas.json:1` — flat array of `{"unicode" N ...}`).
    Loading of the JSON is done in `wiring.cljs` and passed in (INV-S2).
  - `codepoints [s] -> (seq int)` and `codepoint-count [s] -> int` — the shim
    (spike form: `.codePoints`/`.codePointCount` on `:clj`; `.codePointAt` loop
    on `:cljs`).
  - `sanitize-op [coverage fallback-cp op] -> op'` (OP-2) — codepoint-wise;
    covered pass through, uncovered -> `U+FFFD`, count + positions preserved.
  - `op-advance-count [op] -> int` (OP-3) — codepoint count (== rendered
    advances), used by `rect_tree.cljc`/`scene.cljc` clip/wrap/hit math.
- **`text_face.cljc`** (View-3)
  - `project->line-ops [text] -> [line-op]` (OP-4) — split WP1
    `render-bundle-text` output line-for-line; no re-wrap (INV-2/trap 3).
  - `marker->style` table keyed to `;; trail-text v0`; `classify-line
    [line] -> style-kw` (OP-5), unknown -> `:normal`.
  - `version-guard [lines] -> {:ok? bool :notice ...}` (OP-6) — header mismatch
    -> whole projection `:normal` + one notice line.
  - `address-header-op` (OP-7) and `style->rgba` per-run coloring (OP-8).
- **`cards.cljc`** (kernel-material builders; SEPARATE from trail.cljs kinds)
  - `feed-entry-card [entry geom] -> rt-node` (OP-9) — kind glyph, display name,
    two-clock stamps, actor badges, address line.
  - `two-clock-stamp [entry]` (OP-10, renders both when diverge; nil claimed is
    honest, WP1 §6), `staleness-treatment [target]` (OP-11, 3-state triad; nil
    `last-attested-ms` -> `walked unknown`, must not read fresh),
    `provenance-badges [row]` (OP-12, `:asserted-by` always; `:written-by` only
    when differs; disagreeing asserters both, unmerged), `verdict-fold-card`
    (OP-13), `omissions-block [caps]` (OP-14, every cap -> visible count),
    `hole-endpoint-card` (OP-15, explicit hole style, never crash),
    `material-preview [bundle]` (OP-16, mechanical truncation only),
    `expanded-address-op` (OP-17).
  - Collapse-ids namespaced `:trail-face/*` (OP-24 / INV-S5 / trap 10).
- **`lanes.cljc`**
  - `assign-lanes [entries] -> {entry-id lane}` (OP-18) — DETERMINISTIC (same
    fixture -> identical twice; gate 7).
  - `connectors [edges lanes geom] -> [thin-rect]` (OP-19) — Manhattan only
    (vertical spines + elbows + junction dots), thin rects (INV-17/trap 6); each
    endpoint touches its card bounds; `:retracted` -> struck/dim.
  - `dead-end-terminal? [target]` (OP-20) — no forward segment past a
    `:dead-end` (gate 7).
- **`scene.cljc`**
  - `build-text-face-scene [bundle-text geom view-state] -> rt-node`,
    `build-timeline-scene [feed bundles geom view-state] -> rt-node` (OP-21) —
    assemble ONE rt-node tree; the cached-scene contract (§5). Windowing NOT
    built (OP-23/INV-18); v0 builds all cards.
  - `clamp-scroll [offset content-h viewport] -> offset'` (OP-22) — codepoint/
    line-based height, chat-pane idiom; clamp on next wheel (RETRO §3.8).

---

## 5. The cached-scene architecture (the sidebar pattern) — load-bearing

INV-11 / gate 13 / trap 1 / T-6 say: exactly ONE scene tree per data/viewport
change; render-flatten and click-hit-test share the SAME object. The as-built
codebase already implements this for the sidebar; the trail face is **that
pattern applied to a full-screen mode**:

- **Build once + cache:** add a `<trail-face-scene` `m/latest` in
  `editor_compute.cljs` mirroring `<sidebar` (`editor_compute.cljs:310-336`) —
  it calls `scene.cljc/build-*-scene`, `reset!`s `!trail-face-scene` (like
  `!sidebar-scene` at `:328`), and returns `{:rects (tree->rects tree)
  :shadows (tree->shadows tree)}` for the editor rect pool. Use the same
  `struct-hash` gate (`:318-320`) so the scene rebuilds only on data/viewport
  change (satisfies "built ONCE per change").
- **Text ops read the cached scene:** `combined_text.cljs` already reads
  `!sidebar-scene` for `tree->text-ops` (`combined_text.cljs:229-232`); the
  trail-face text branch (§6.4) does the same on `@!trail-face-scene`.
- **Hit-test reads the cached scene:** `handle-trail-face-click!` (new, §6.6)
  hit-tests `@!trail-face-scene` — **NOT a rebuilt tree**. This is the explicit
  fix for the as-built anti-pattern: chat/flow REBUILD the tree at click time
  (`mouse.cljs:322-330,370-374`); the sidebar does not (`mouse.cljs:480`). The
  trail face follows the sidebar.
- **Scroll rides the camera:** the global camera pan-y is `-scroll-y`
  (`render.cljs:446`; PRIMITIVES §camera). A full-screen trail face reuses
  `!scroll-y` (editor/flow-left precedent) so the cached scene coords are
  scroll-independent (no rebuild on scroll -> best for INV-11); hit-test adds
  `scroll-y` to the click y (flow precedent `mouse.cljs:374`). Clamp via
  `scene.cljc/clamp-scroll` in the scroll zone (§1c). (Alternative — a dedicated
  `!trail-face-scroll-y` subtracted at build, chat precedent — is available if
  camera reuse jars at first light; not recommended, it forces rebuild-on-
  scroll.)

Gate 13 asserts two `tree->rects`/`tree->text-ops` flattens from one
`!trail-face-scene` value are `identical?`-stable inputs and a fixture click
hit-tests the same object.

---

## 6. The 7-step wiring (exact insertion points; dg_flow precedent)

Two faces = two modes from ONE atom (the `!flow-state`->`:flow-intake`/
`:flow-run` precedent): `!trail-face-state {:face :text|:timeline :address
<addr> :order :arrival|:claimed}` -> modes `:trail-text` / `:trail-timeline`.

1. **Mode branch + predicates** (`workspace_actions.cljs`). Add a `cond` branch
   in `derive-effective-local-world` (`:145-149`) keyed on `(:face
   @trail-face-state)` present -> `:trail-text`/`:trail-timeline`; destructure
   `trail-face-state` in the arg map (`:137-138`). Add after `local-world-flow?`
   (`:214-217`): `local-world-trail-text?`, `local-world-trail-timeline?`,
   `local-world-trail-face?` (either). Add `:panes`/`:split` cases (single pane,
   width 1.0, like `:flow-*` at `:193-201`). (OP-40; gate 17.)
2. **Atom + recompute** (`state.cljs`, `runtime.cljs`). `state.cljs:192` (beside
   `!sidebar-scene`): `!trail-face-state (atom nil)`, `!trail-face-scene (atom
   nil)`. `runtime.cljs`: add `:trail-face-state @(:!trail-face-state atoms)` to
   the `recompute-local-world!` map (`:306-314`) and `:!trail-face-state` to the
   add-watch vector (`:317`). (OP-29/OP-40.)
3. **Build/compute fns**: the `<trail-face-scene` flow (§5) + `wiring.cljs`
   glue; the pure builders live in `scene.cljc`.
4. **Compute branches.** `combined_text.cljs`: the `<content-text` dispatch is
   `(if (:rt-node extract-preview) ... (if flow-mode? ... editor))` at `:236/
   :253`; bind `trail-mode? (local-world-trail-face? local-world)` beside
   `flow-mode?` (`:226`) and add a branch that returns
   `[(tree->text-ops @!trail-face-scene) (vec (range (count lines))) []]`
   (mirrors the flow branch `:253-257` and the sidebar cached read `:229-232`).
   `editor_compute.cljs`: the `<mode` selector (`:300-303`) already yields the
   trail modes via `local-world-mode`; add `:trail-text`/`:trail-timeline` cases
   to the `<editor-rects` `case` (`:458-461`) returning the `<trail-face-scene`
   rects. (OP-41; gate 17 byte-identical for non-trail worlds.)
5. **render-consumer threading** (`render.cljs`). Add `!trail-face-state
   !trail-face-scene` (and, at P6, `!ingest-epoch`) to the destructure
   (`:25-31`); thread into the `<combined-text-ops` call (`:41-50`) and the
   `<editor-rects+sidebar` call (`:55-63`); pass `trail-face/compute-*` fns
   alongside the `dg/*` fns (`:49-50,:62-63`). (OP-40/OP-41.)
6. **Scroll + mouse.** Scroll zone: new `trail-face-active?` clause in
   `scroll.cljs` after `:85` (§1c). Mouse: in `handle-mousedown!` add a branch
   to the inner `cond` (`mouse.cljs:437-465`) before `:else`, keyed on
   `local-world-trail-face?`, calling `handle-trail-face-click!` which hit-tests
   `@!trail-face-scene` (add `scroll-y` to y) and dispatches card
   expand/collapse via `:trail-face/*` actions — **no tree rebuild** (§5). (OP-
   42/OP-43; traps 1, 9; gates 13.)
7. **Entry command** (`agent_flow.cljs`). In `submit-agent-run!` add
   `trail-face/parse-trail-command` beside `dg`/`jit` (`:215-217`) and a `case`
   branch (`~:304`) that `reset!`s `!trail-face-state` (setting `:face` +
   `:address` + `:order`) — the derive picks up the mode. (OP-44.)

---

## 7. rect_tree promotion + T-4 clamp (gate 6)

- **Rename** `rect_tree.cljs -> rect_tree.cljc`; ns `app.client.workspace.rect-
  tree` unchanged (§1d). Puts `resolve-layout`/`tree->rects`/`tree->text-ops`/
  `hit-test` on the JVM.
- **T-4 clamp** in `tree->rects` (`rect_tree.cljc:229-251`): today the `bg` map
  is built at `:x abs-x :y abs-y :w w :h h` (`:232`) with no clamp; `visible?`
  (`:221-228`) only culls fully-offscreen. Edit: when `clip-bounds` is non-nil,
  clamp the emitted bg rect to the axis-aligned intersection
  (`x' = max abs-x cx`, `y' = max abs-y cy`, right/bottom = min against clip
  right/bottom, `w'/h'` from those) BEFORE the `cond->`. Radii degrade at
  clamped corners (accepted, §5.2). Scope: `tree->rects` bg only — text is
  already clipped (`:279-318`); **leave the `0.56` literal at `:284`**
  (INV-S3, legacy). Gate 6 asserts every emitted bg rect is within clip bounds
  at 3 scroll offsets.

## 8. renderer.cljs V3-5 shape fix (OP-27; §5.1)

In `shape-msdf-line` (`renderer.cljs:1147`) and `shape-slug-line` (`:1187`),
the `cond` over each char (`:1166-1184` / `:1205-1232`) puts `(swap! !x +
advance)` INSIDE `(when-let [g (get glyphs code)] ...)` — a missing glyph gets
no advance and no draw (the V3-5 bug). Edit, bounded to these two fns: in the
`:else` branch, ALWAYS `(swap! !x + advance)`, then draw the glyph if present
ELSE draw the atlas fallback glyph if `(get glyphs 0xFFFD)` exists (else emit
nothing but keep the advance). This is defense-in-depth behind the cljc
sanitizer (OP-2, which upstream substitutes uncovered -> `U+FFFD`); gate 4 does
NOT execute the shaper, so this fix is verified by diff review + first light.
Do NOT convert the shaper to codepoint iteration (the sanitizer owns surrogate
correctness); keep `.charCodeAt` per code unit.

## 9. Atlas regen (OP-28; gate 5) + the active-atlas finding

- Toolchain present: `msdf-atlas-gen` at `/usr/local/bin`; the Ubuntu varfont
  the CLAUDE.md recipe names is at `/usr/share/fonts/truetype/ubuntu/
  UbuntuSansMono[wght].ttf`. Regen `resources/public/font_atlas.{json,png}` with
  the CHARSET_AUDIT missing set (box-drawing `U+2500` class, arrows, curly
  punctuation, `U+2713`/`U+2605`/`U+26A0` class) **+ `U+FFFD` itself**; re-run
  the audit green. Coverage source for OP-1 is this same `font_atlas.json`
  (gate 4). `font_atlas.json` is MSDF, `glyphs[].unicode` flat (verified
  `font_atlas.json:1`).
- **FINDING (feeds §15 OI-2):** the running app selects the manifest
  `default:true` font = **DejaVu Sans Mono**, `active-backend :slug`
  (`fonts.cljs:101-104,124-141`; `manifest.json:36-56`), which renders from
  `fonts/dejavu_sans_mono_slug_meta.json`, **not** the MSDF `font_atlas.json`
  the contract regenerates. Today harmless (audit: all 7 atlases share the
  identical 95-glyph ASCII set). Post-regen, widening only `font_atlas.json`
  will NOT render `U+2500`/`U+2192`/`U+2014` in-app on the slug backend — they
  stay honest fallback-tofu-with-advance (OP-27), never vanish, but do not draw.
  Gate 4/5 remain self-consistent on `font_atlas.json`. Resolution flagged to
  Sid at P4 (options in §15 OI-2); recommend switching the manifest default to
  the MSDF atlas (outside the allowlist -> Sid decision) so the regen is
  effective at zero extra toolchain cost.

## 10. Electric bridge + epoch atom (OP-32/33/34/35)

- **`file_viewer.cljc`** (the x5 `Watch*` precedent `:105-133`; the pull
  precedent `FileContent` `:102`). Add pull e/defns `TrailBundle`/`TrailFeed`/
  `TrailConversation`/`TrailText` — each `(e/server (trail-view/read-* ...))`
  over a WP1 §7 wrapper (`read-context-bundle`/`read-recent-activity`/
  `read-conversation-trail`/`render-bundle-text`), address-shaped args. Add
  `WatchIngestEpoch []` = `(e/server (e/watch util-fns/!ingest-epoch-atom))`
  (exactly `WatchSidebarTruth` `:105-111`). Add the require
  `#?(:clj [app.server.rama.trail-view :as trail-view])` beside the guarded
  `util-fns` require (`:6-9`) — lands at **P5** (trail_view.clj is WP1's,
  parallel; absent now, expected). `WatchIngestEpoch` needs only `util-fns`
  (already required) -> can land at P6. No `foreign-select`, no PState names
  (INV-S1); `WatchIngestEpoch` is the S1a carve-out.
- **`electric_flow.cljc`** (`:472-483`): add `!ingest-epoch (atom 0)` and (P5)
  the trail result atoms to the boot `let`, and `(reset! !ingest-epoch
  (fv/WatchIngestEpoch))` beside the other `reset!`s (`:479-483`). (OP-34.)
- **`util_fns.cljc` — OP-35 is TWO-PART (else gate 17 fails):**
  1. `(defonce !ingest-epoch-atom (atom 0))` beside the mirror atoms
     (`:123-128`).
  2. **Append `!ingest-epoch-atom` to the `:mirrors` vector**
     (`transitional-mirror-quarantine`, `:112-119`). The guard test
     `mirror-quarantine-covers-every-atom-test` (`text_kernel_probe_test.clj:
     257-273`) asserts `(= declared actual)` where `actual` = every public var
     named `!*` whose value is a `clojure.lang.Atom`; omitting the declaration
     fails it. The epoch atom fits the quarantine's uniform claims exactly
     (`:reset-on-restart? true`, `:rebuild-path :none`, not kernel truth =
     INV-14) — an honest addition, not a hack. Neither CONTRACT nor
     IMPLICIT_SPEC names this; genuine P1 find.

## 11. Watcher design — `ingest_watchers.clj` (OP-36..OP-39)

Grounded in §1a. **Event-driven, not poll-loop** (INV-19): a `java.nio.file.
WatchService` (inotify on Linux) over the configured roots, debounced `>= 500ms`
per path, serialized import per file, failures logged + retried on next event,
loop never crashes (OP-39). Config constants (new, §1a shows none exist):
`~/.claude/projects/-mnt-data-projects-Softland/` (repo-scoped, NOT the global
`**` glob), `docs/`, `vision/`.

On a settled change event, per file type (OP-37, EXISTING seam only):
- **Transcript `.jsonl`:** reuse `dogfood.transcript/read-jsonl-observations`
  (`:557`) + `import-observations-into-object-container!` (`:1072`) for that one
  file (their internal loop already appends via
  `append-object-container-request!` and awaits the decision).
- **Markdown `.md`:** NEW plumbing only — `slurp` the file -> `markdown-adapter/
  markdown-source-import-request [raw-text source-ref]` (`:462`) ->
  `append-object-container-request! runtime request :ack` (or default append +
  `await-object-container-decision`, `runtime.clj:314`). The `slurp` is a new
  READER; the kernel write seam is existing -> NOT a new ingest path (trap 2).

On the deterministic decision latch (NOT an fs poll; §1a "KEY"), bump
`!ingest-epoch-atom` by +1 (OP-38). `WatchIngestEpoch` (§10) pushes it; the
client re-pulls the feed on epoch change, debounced `>= 1s` (INV-19; no polling
loop). Epoch is a monotonic per-server counter, carries no data, resets on
restart (INV-14) — every rendered stamp still comes from WP1's two clocks.

**Gate 14** (`ingest_watchers_test.clj`, JVM integration): boot an OC runtime
via `start-object-container-runtime!` (the test-cluster path the existing OC
tests use, `object_container_test.clj:712`), write a fixture `.md` under a temp
watch root -> import fires (latched on `await-...-decision`, no poll) -> epoch
`+1`; identical rewrite -> convergent re-import (`$$decisions-by-idempotency`)
-> feed entry count unchanged, epoch still `+1` (E-12). Trap 11 satisfied: the
assertion latches on the decision, never polls to "prove" the no-op.

**Runtime dependency (feeds §15 OI-1):** gate 14 supplies its own test cluster,
but first light (P6) and the Electric bridge (P5) need a runtime in the RUNNING
server, which does not exist today. Named, not hidden.

## 12. Fixtures (`test/resources/trail_face/*.edn`; §8, gate 16)

Derive EXACTLY from WP1 CONTRACT §4/§6/§8 shapes:
- **bundle.edn** — all six layers (L0-L5); `:relations` with `:this`+`:in-
  family`; `:verdicts.:current` with two disagreeing asserters + a `:written-by`
  != `:asserted-by` row; `:omissions` with counts; L5 `:last-attested-ms` nil
  target (walked-unknown); a `:dead-end` target; a hole-endpoint row (§9.3).
- **feed.edn** — arrival window with a back-dated entry (`:time/claimed-ms`
  ~60d, `:time/arrival-ms` now), a `:feed/uncovered` declaration, both clock
  stamps on every entry.
- **projection.txt** — a `;; trail-text v0` sample incl. `walked unknown`, all
  §8 markers; plus a mutated-header and mutated-marker variant for gate 3.
- **Two salt classes** (E-8 / IMPLICIT_SPEC F-4): (i) audit top-missing
  `U+2500`/`U+2192`/`U+2014` — COVERED after the §9 regen -> gate 2/4 verbatim +
  advance==codepoint assertions; (ii) permanently-uncovered `U+1F534`/`U+1F7E2`
  + surrogate pairs — never added -> gate 4 fallback assertions. Never salt the
  fallback path with (i) (regen makes them covered -> vacuous/failing).
- Re-derive in the same session if WP1 impl amends §4/§6/§8 (implementer-
  fixable, §8). Gate 16: one live WP1-wrapper capture diffs shape-equal
  (keys/nesting) against these hand fixtures, post-WP1-green.

## 13. Per-gate deftest map

`clojure -M:test`, ns `app.client.workspace.trail-face-test` over §12 fixtures.

| Gate | deftest | Fixture | Assertion / falsifier | Phase | Ops |
|---|---|---|---|---|---|
| 1 | `address-law-test` | bundle/feed/expanded | one header address line; round-trips WP1 `resolve-address` shape-equal | P3->P4 | OP-7/17 |
| 2 | `view3-verbatim-test` | projection.txt | line count == projection; per-line op text == line after sanitize; no drop/reorder/re-wrap | P3->P4 | OP-4/2 |
| 3 | `marker-totality-test` | projection + mutated | every line -> style; unknown -> `:normal`; bad header -> plain + one notice | P3->P4 | OP-5/6 |
| 4 | `sanitize-correctness-test` | salted (both classes); coverage from REAL `font_atlas.json` | covered pass verbatim advance==cp; uncovered -> `U+FFFD`; surrogate pair == 1 advance | P3->P4 | OP-2/3 |
| 5 | `atlas-coverage-regression-test` | regenerated `font_atlas.json` | must-have set + `U+FFFD` present; falsifier: narrows or ships without fallback | **P4** | OP-28 |
| 6 | `clip-containment-test` | straddling cards @ 3 offsets | every bg rect within clip bounds | **P4** | OP-26 |
| 7 | `lane-connector-geometry-test` | feed w/ dead-end + relations | deterministic (twice-equal); endpoints touch bounds; no forward past dead-end | P3->P4 | OP-18/19/20 |
| 8 | `two-clocks-test` | back-dated entry | in arrival-today window; `:order :claimed` sorts by claimed stamp; both stamps; order param changes rendered address; a claimed WINDOW is never built (WP1 §9.10) | P3->P4 | OP-10 |
| 9 | `staleness-triad-test` | recent/stale/never | 3 pairwise-distinct; nil != fresh | P3->P4 | OP-11 |
| 10 | `badges-test` | 2 asserters + written-by-differs | `:asserted-by` always; `:written-by` iff differs; disagreeing unmerged | P3->P4 | OP-12/13 |
| 11 | `omissions-are-pixels-test` | caps + `:feed/uncovered` | every omission -> visible count; forced cap changes render | P3->P4 | OP-14 |
| 12 | `hole-endpoint-test` | hole row | renders hole style; no exception/skip | P3->P4 | OP-15 |
| 13 | `cached-scene-identity-test` | any scene + a click | two flattens `identical?`-stable; hit-test resolves same object | P3->P4 | OP-21/43 |
| 14 | `watcher-loop-test` | temp root + fixture md | import latched (no poll); epoch +1; identical rewrite -> count unchanged | **P6** | OP-36..39 |
| 15 | `MEASUREMENT_RAF.md` (artifact) | real day's material | artifact exists + states ruling either way | **P6** | OP-23 |
| 16 | `fixture-fidelity-test` | live WP1 capture | shape-equal (keys/nesting) vs hand fixtures | **P5** | OP-45/46 |
| 17 | `regression-test` | non-trail world | kernel+WP1 suites green; shadow compiles; existing-mode ops byte-identical | **P5** | OP-40..44 |

Gate 5 is P4 (needs OP-28). Gate 14 is P6-only (F-2). Gate 4 fallback assertion
uses class-(ii) salt (regen-independent). Gate 13 stops at structural identity;
real mouse routing is first light.

## 14. Style gates (S1-S5) — compliance plan, each with its stop

- **S1 Seam:** e/defns + faces call only WP1 §7 wrappers; `WatchIngestEpoch`
  reads the epoch atom (S1a carve-out). Enforce by source scan; no
  `foreign-select`/PState name/kernel-write under `trail_face/` or the new
  e/defns. *Stops at source scan (gate 14 covers runtime).*
- **S2 Purity:** `trail_face/*.cljc` = no atoms, no js interop; the ONE shim is
  `sanitize.cljc` codepoint iteration (named, spike-proven). *Stops at the file
  boundary; `wiring.cljs` exempt.*
- **S3 char-advance:** face code takes `char-advance` as an argument; literal
  `0.56` appears nowhere under `trail_face/`. `rect_tree.cljc:284` keeps its
  `0.56` (legacy, out of scope). *Stops at the new dir.*
- **S4 control bytes:** NUL/control only as `\uXXXX` in source AND fixtures;
  emoji/box-drawing salts written as `\uXXXX` escapes, never raw glyphs. *Stops
  at source/fixture files.*
- **S5 collapse-id:** every interactive id under `trail_face/` is
  `:trail-face/*`. *Stops at the new dir.*

## 15. Open items + conditional escalation triggers (the map must not lie)

- **OI-1 — No production OC runtime (shared with WP1).** `start-object-
  container-runtime!` builds a test cluster; nothing boots the OC/relation/
  trail-view modules in the running server. P3/P4/gate-14 are unaffected
  (fixtures + test cluster). **Conditional escalation:** if, at P5, no party
  (likely WP1's integration) owns a server-boot that launches these modules and
  holds the handle, escalate to `decisions.md` Open Questions as a cross-package
  integration fork. Recommended shape: a `defonce` boot mirroring
  `util_fns.cljc:12-17` (text-kernel delay).
- **OI-2 — Active atlas != regen target (§9).** App renders DejaVu/slug; contract
  regenerates MSDF `font_atlas.json`. Not a gate blocker (gate 4/5 self-
  consistent). Options for P4, **flag to Sid** (manifest.json is outside the
  allowlist): (1) switch manifest default to the MSDF Ubuntu atlas [recommended,
  zero toolchain cost, makes the regen render]; (2) regen the DejaVu slug atlas
  [needs the slug toolchain the contract does not name]; (3) accept v0 renders
  structural glyphs as honest fallback-tofu-with-advance (OP-27) and defer.
- **OI-3 — Markdown new-object-per-edit (OC property, not a B2 defect).** A
  changed `.md` mints a new object-key (content-hash-derived), so the feed shows
  a new `:source-ingested` entry per real edit; byte-identical rewrites still
  no-op (gate 14 uses identical rewrite, so it holds). Note for first-light
  expectations only.
- **OI-4 — WatchService vs the existing transcript poll-watcher.** The plan uses
  an event-driven `WatchService` (INV-19), reusing only the transcript IMPORT
  driver, not `start-transcript-watch!`'s poll-loop. If `WatchService` proves
  unreliable for the transcript dir at first light, the poll-watcher is a named
  fallback (bounded interval, still latch-not-prove) — not a fork.

## 16. Baseline note

No "B2 baseline suite" run this session: the working tree carries Track-A WP1
in-progress kernel edits (`git status`: `M relation_kernel.clj`,
`M relation_kernel_test.clj`), so a full-suite run now would test Track-A's WIP,
not a clean B2 baseline. The clean gate-17 regression baseline is taken at P4/P5
on the integration point. The one baseline that matters for the new gates — that
a pure client-dir `.cljc` loads and runs on the JVM — is the §1b spike (PASS).

## 17. Stop-clause status

No stop-clause escalation fires at P1. The contract is buildable as specified;
the two cross-package items (OI-1 runtime, OI-2 atlas) are NAMED dependencies
with conditional triggers, not P1/P3 blockers. No genuine two-readings conflict
between view-mvp CONTRACT, WP1 CONTRACT, and decisions.md was found — the seam
holds (WP1 §7 wrappers are the only truth surface; the epoch atom is the S1a
carve-out).
