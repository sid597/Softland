# View-MVP PLAN VALIDATION — WP-B2 Phase B2-P2, Round 1

Status: validation artifact (per-round; NEVER overwrite a FAIL — this is R1).
Verdict: **PASS (with advisories A1-A8 folded into the named phases; open
doubts D1-D2 with falsifiers).** Default-fail discipline: PASS is affirmed
only against the explicit citation traces and scenario walks below.

Self-routing: this is B2-P2 (plan validation) -> required Opus 4.8 / xhigh;
ran on Opus 4.8 with effort xhigh (Sid set it in-session) -> no mis-route.
Package gate: this file is `file(1)`-checked to say "text" (verified: "UTF-8
text") with zero raw NUL. No raw control byte; any codepoint UNDER DISCUSSION
(box-drawing, arrows, emoji) is written `U+XXXX` so no tool-JSON decode can
inject bytes; ordinary section-sign / em-dash prose punctuation matches the
rest of this doc tree.

Provenance: fresh-context scenario-trace of `build/view-mvp/PLAN.md` against
`build/view-mvp/CONTRACT.md` v1.1 (BINDING) + its `IMPLICIT_SPEC.md`
(incl. the §8 addendum) + `build/trail-view/CONTRACT.md` (WP1, BINDING —
§3/§4/§6/§7/§8) + `CHARSET_AUDIT.md` + the on-disk source cited by the plan.
Method: every `file:line` citation in the plan was checked against real code
(2 Opus verifier subagents over the server import-seam and the bridge/util/
atlas breadth; ~10 direct reads by this session over the client-wiring design
core). Citations verified: **42** distinct source-fact claims.

---

## 1. Verdict basis — VERIFIED affirmatively (default-fail earned)

### 1.1 The load-bearing design (cached scene) HOLDS
The plan's whole falsifiability strategy rests on "the as-built sidebar already
does build-once -> cache atom -> flatten-and-hit-test-the-same-tree, and the
trail face is that pattern at full-screen." VERIFIED in source:
- `editor_compute.cljs:310-336` `<sidebar`: builds `raw-tree` ->
  `resolve-layout` -> `tree`; computes `struct-hash` (`:318`) +
  `structure-changed?` (`:319`); ONLY when changed does `(reset! !sidebar-scene
  tree)` (`:327-328`); returns `{:rects (tree->rects tree) :shadows
  (tree->shadows tree)}` (`:330-335`). Exactly the pattern the plan §5 mirrors.
- `combined_text.cljs:229-232` reads the cached scene as a BOUND value
  (`sidebar-scene`) via `tree->text-ops` — the trail-face text branch (plan
  §6.4) copies this shape.
- `mouse.cljs:480` sidebar hover hit-tests `@!sidebar-scene` (the CACHED
  atom), while `mouse.cljs:322-330` (chat) and `:370-374` (flow) REBUILD the
  tree at click time. The trail face follows the sidebar (hit-test the cached
  atom), not chat/flow. This is the exact as-built anti-pattern the retro found
  (trap 1 / T-6) and the plan's design avoids it. CONFIRMED.
- Gate 13 is a PURE-cljc test (build scene via `scene.cljc` -> hold the one
  tree -> two `tree->rects` flattens + `hit-test` all on the SAME object). It
  needs no atoms/browser and is runnable at P3. Sound.

### 1.2 The 7-step wiring insertion points are REAL and correctly placed
- `<mode` is a GENERIC pass-through: `editor_compute.cljs:300-303`
  `(cond (:rt-node extract-preview) :extract-preview :else (ws/local-world-mode
  local-world))`. The plan's non-obvious claim "the `<mode` selector already
  routes the new trail modes" is CORRECT — once `local-world-mode` returns
  `:trail-text`/`:trail-timeline`, `<mode` passes it through; no edit to `<mode`
  needed, only the downstream `<editor-rects` `case` (`:458-461`) and the
  `combined_text` dispatch (`:253`).
- Mode branch: `derive-effective-local-world` mode `cond` at
  `workspace_actions.cljs:145-149`; predicates `local-world-flow?` `:214-217`;
  `:split`/`:panes` `case`s `:167-206`. Present as cited.
- Recompute + add-watch: `runtime.cljs:301-314` builds the deref'd map and
  calls `derive-effective-local-world`; add-watch vector `:317-318`. Adding
  `:trail-face-state @(:!trail-face-state atoms)` to the map and
  `:!trail-face-state` to the watch vector is correct.
- Atoms: `state.cljs:192` is exactly `:!sidebar-scene (atom nil)` inside the
  aggregated `atoms` map — new `:!trail-face-state`/`:!trail-face-scene` atoms
  beside it resolve for `(:!trail-face-state atoms)`.
- render threading: `render.cljs:25-31` destructure, `:41-50`
  `<combined-text-ops` call (passes `!sidebar-scene` at `:47` + `dg/*` text fns
  at `:49`), `:55-63` `<editor-rects+sidebar` call (passes `!sidebar-scene` at
  `:60` + `dg/*` rect fns at `:62`). The plan's threading map is accurate.
- Scroll (trap 9): `scroll.cljs:48` cond order sidebar(`:50`) ->
  agent(`:59`) -> flow(`:71`, comment `:70` "must check BEFORE chat") ->
  chat(`:87-95`) -> editor `:else`(`:98`). The editor `:else` guard is `(or
  (not file-workspace?) ...)` (`:99`) — TRUE in a trail-face world -> a trail
  face WOULD steal editor scroll. CONFIRMED. Insert a `trail-face-active?`
  clause after `:85` (after flow, before chat). Correct.
- Mouse: `handle-mousedown!` inner cond `:437-465` (flow -> file-workspace ->
  `:else`); inserting a `local-world-trail-face?` clause before `:else`
  (`:462`) calling a `handle-trail-face-click!` that hit-tests `@!trail-face-
  scene` is correct — and `(+ y scroll-y)` is the right click-y (flow
  precedent `:374`).
- Entry command: `agent_flow.cljs` parse `or`-chain `:216-219` (dg/jit),
  `case` dispatch `:234-313`. Adding `trail-face/parse-trail-command` to the
  `or` and a `case` branch that `reset!`s `!trail-face-state` fits.
- Camera: `render.cljs:446` passes `0 (- scroll-y)` as camera x/y to
  `draw-frame!` -> pan-y = `-scroll-y`. Grounds §5's `!scroll-y`-reuse scroll
  design (scene coords scroll-independent, no rebuild-on-scroll).

### 1.3 Gate-17 (existing-mode byte-identical) SAFETY holds
Trail branches fire only when `trail-face-state` is non-nil (`(:face ...)`
present). In a non-trail world it is nil, so the `derive-effective-local-world`
`cond`, the `<editor-rects` `case`, and the `combined_text` dispatch all fall
through UNCHANGED -> existing-mode output is byte-identical. VERIFIED by the
nil-guard structure. (Hard requirement caught by the plan: `:panes` `case`
`:180-206` has NO default clause, so a `:trail-text`/`:trail-timeline` mode
without an added `:panes` case throws `IllegalArgumentException` — the plan
correctly requires adding those cases; see A5.)

### 1.4 The substrate edits are bounded and sound
- OP-27 (V3-5 shaper): `renderer.cljs:1147` `shape-msdf-line` /
  `:1187` `shape-slug-line` — `(swap! !x + advance)` sits INSIDE `(when-let [g
  (get glyphs code)] ...)` (`:1181` / `:1220`) -> a missing glyph gets no
  advance and no draw. CONFIRMED bug. Iteration is per-UTF-16-code-unit
  (`(.charCodeAt ch 0)` `:1165` / `:1204`). The fix (advance-always + fallback
  `U+FFFD` draw, keep `charCodeAt`, do NOT convert to codepoint iteration —
  the cljc sanitizer owns surrogate correctness) is a correct, bounded edit.
  Consistency note: the sanitizer substitutes uncovered/astral codepoints to a
  single `U+FFFD` (BMP) BEFORE shaping, so text reaching the per-code-unit
  shaper is BMP -> per-unit == per-codepoint advances; the atlas is 95 ASCII
  (no astral glyphs) so nothing regresses. Gate 4 does NOT execute the shaper
  (contract) -> OP-27 is diff-review + first-light verified.
- OP-26 (T-4 clip clamp): `rect_tree.cljs:213-251` `tree->rects`; `visible?`
  `:221-228` (culls fully-offscreen only); bg emitted at full `w`/`h` with NO
  clamp (`:231-242`). `clip-bounds` is in scope. The plan's axis-aligned
  intersection clamp (`x'=max abs-x cx`, `y'=max abs-y cy`, right/bottom = min
  against clip edges) is correct and only applies when `clip-bounds` non-nil;
  `visible?` already guarantees overlap so `w'/h'` stay positive. `child-clip`
  (`:244-246`) is a separate concern (unchanged). Gate 6 testable at P4.
- OP-35 (epoch atom): TWO-PART find CONFIRMED. `util_fns.cljc`
  `transitional-mirror-quarantine` `:mirrors` vector and the mirror `!*-atom`
  defs are colocated; the gate-17 guard test dynamically enumerates every
  public `!`-prefixed `clojure.lang.Atom` var in `app.server.rama.util-fns`
  and asserts set-equality with `:mirrors`. Adding `(defonce !ingest-epoch-atom
  (atom 0))` WITHOUT adding it to `:mirrors` FAILS that test. The plan's
  "OP-35 is TWO-PART (else gate 17 fails)" is a real, load-bearing catch the
  CONTRACT and IMPLICIT_SPEC both missed. The epoch atom fits the quarantine's
  uniform claims (`reset-on-restart? true`, not kernel truth = INV-14) — an
  honest addition.

### 1.5 The seam holds; read-only is structural; no dropped requirement
- Data surface: every op's inputs are WP1 §7 wrapper results / geometry /
  local view-state. The bridge e/defns `TrailBundle`/`TrailFeed`/
  `TrailConversation`/`TrailText` each wrap a NAMED WP1 §7 `read-*` /
  `render-bundle-text` inside `e/server` (F-3 was already swept in v1.1;
  the `Trail*` names are THIS package's e/defn names, not WP1 wrappers).
  `WatchIngestEpoch` reads the ingest-epoch atom — the ONLY out-of-§7 read,
  the S1a carve-out (a counter, not a PState, not `foreign-select`). No S1
  breach.
- Read-only (INV-10) is structural: no face path writes a kernel; view-state
  is atoms only. Consistent with WP1 gate 14 (no-depot module).
- Gate coverage: all 17 contract gates map to a named deftest/artifact + ops +
  phase (plan §13). All 10 face laws, S1-S5, and INV-11..INV-19 are carried by
  ops with gate falsifiers. No contract requirement (§1-§12) is dropped.
- Fixtures (plan §12) match WP1 §4 (six layers L0-L5, `:this`/`:in-family`,
  `:verdicts.:current` two-asserter + `:written-by`-differs, `:omissions` with
  counts, `:last-attested-ms` nil, `:dead-end`, hole-endpoint) and WP1 §6
  (arrival window, back-dated entry both stamps, `:feed/uncovered`). Two salt
  classes (covered-post-regen vs permanently-uncovered) are correct per E-8.
  Gate 16 (post-WP1-green live-capture shape-diff) is the drift backstop.

### 1.6 Cross-package findings (OI-1/2/3) CONFIRMED, correctly scoped
- OI-1: NO production OC runtime boots today; `start-object-container-runtime!`
  is called ONLY in test (`object_container_test.clj:712/761/787/820`); the
  sole production `defonce` (`util_fns.cljc:12`) boots the TEXT kernel. P3/P4/
  gate-14 unaffected (test cluster + fixtures). Conditional escalation at P5 is
  correctly named (shared with WP1 integration).
- OI-2: active font is DejaVu via the `:slug` backend (`fonts.cljs:101-104`,
  `manifest.json` `default:true` + `preferredBackend:"slug"`); the top-level
  MSDF `font_atlas.json` is NOT referenced by `manifest.json`. So the §5.3
  regen makes gate 4/5 self-consistent on `font_atlas.json` but will NOT draw
  the new glyphs in-app on the slug backend (honest fallback-tofu-with-advance
  via OP-27, never vanish). The P4 Sid decision (switch manifest default to the
  MSDF atlas) is correctly flagged (manifest.json is outside the allowlist).
- OI-3: md `object-key` is content-hash-derived
  (`markdown_adapter.clj:184/201-203`; `object-key = sha256(source-ref + sep +
  sha256(raw-text))`) -> a changed `.md` mints a NEW object; only byte-
  identical rewrites converge. Gate 14 uses an identical rewrite so it holds;
  first-light expectation note is correct.
- The md watcher's `slurp` is a NEW reader (CONFIRMED: no md file-walker
  exists anywhere; md import is in-memory `raw-text` only). Convergent-re-
  import safety rests on the watcher reusing `markdown-source-import-request` +
  `append-object-container-request!` VERBATIM (existing write seam) -> trap 2
  preserved (reader new, truth path existing). See A8.

---

## 2. Advisories (non-blocking; fold into the named phase — no plan re-run)

- **A1 (P3, gate-writing) — gate 1's P3 round-trip is PURE EDN, not a live
  `resolve-address` call.** Contract gate 1 and plan §13 both say "round-trips
  through WP1 `resolve-address`" AND place gate 1 at P3->P4. But `resolve-
  address` is a WP1 §7 server wrapper in `app.server.rama.trail-view`, which
  does not exist until WP1 is green (B2-P5), and B2's S1 seam forbids re-
  implementing it. So at P3 gate 1 MUST test the pure property: the rendered
  header line, read with `read-string`, is shape-equal to the fixture's carried
  `:bundle/address` (WP1 §3 law 1 guarantees every result carries its address).
  The live `resolve-address` round-trip is a P5 confirmation. Left implicit,
  an implementer could call a nonexistent fn or defer gate 1 to P5. This is the
  single most worthwhile plan-note to add. Implementer-fixable; self-corrects
  at test-write time (the pure form is the only P3-runnable option).
- **A2 (P5, wiring) — plan §6-step-1's `(:face @trail-face-state)` has a stray
  `@`.** Inside `derive-effective-local-world` the value is ALREADY deref'd
  (the deref happens in `recompute-local-world!`, `runtime.cljs:306-314`).
  Literal copy would error (deref of a plain map). Correct form inside the pure
  fn: `(:face trail-face-state)` (a destructured value). Self-corrects at
  compile.
- **A3 (P5, reactivity) — do NOT deref `@!trail-face-scene` inside the
  `combined_text` `m/latest` text branch.** §6.4 writes `@!trail-face-scene`,
  but the sidebar precedent it cites (`:229-232`) consumes a BOUND
  `sidebar-scene` value threaded reactively; a raw deref inside `m/latest` is a
  non-reactive snapshot (CLAUDE.md) -> the trail text would not re-render when
  the scene atom updates. Thread `!trail-face-scene` the SAME way
  `!sidebar-scene` is threaded (atom in at `render.cljs:47`, bound-or-watched
  consistently inside `<combined-text-ops`). NOTE: the mouse hit-test's
  `@!trail-face-scene` (§6.6) IS correct — it is an imperative event handler,
  matching the sidebar hover deref at `mouse.cljs:480`. The distinction is
  reactive-flow (bind) vs event-handler (deref). Self-corrects at first light
  (visible stale text), P5/P6.
- **A4 (P5, scroll) — reconcile §1c ("owning its own scroll atom") with §5
  ("reuses `!scroll-y`").** §5 is the detailed design and is right: camera
  pan-y = `-scroll-y` (`render.cljs:446`, VERIFIED), so reusing `!scroll-y`
  keeps scene coords scroll-independent (no rebuild-on-scroll). The scroll zone
  should `swap! !scroll-y` with `scene.cljc/clamp-scroll`, NOT a new atom,
  unless the named fallback (`!trail-face-scroll-y`) is taken. Also: if the
  `<trail-face-scene` flow reuses the sidebar shape literally it would WATCH
  `!scroll-y` and put it in the struct-hash (`editor_compute.cljs:318/336`,
  which the sidebar DOES) -> rebuild every scroll frame; the trail-face flow
  must DROP `scroll-y` from its watch/hash to get the intended no-rebuild
  behavior.
- **A5 (P5) — adding `:trail-text`/`:trail-timeline` `:panes` cases is a HARD
  must, not optional.** `:panes` (`workspace_actions.cljs:180-206`) is a `case`
  with NO default; an unmatched mode throws. The plan flags this (§6-step-1);
  cite it in the code with a note so it is not dropped. (`:split` at `:167-173`
  has a default, so it is safe without an explicit trail case.)
- **A6 (all phases) — file-path corrections (content correct, path wrong):**
  (i) `electric_flow.cljc` is at `src/app/electric_flow.cljc` (NOT
  `client/`); boot resets at `:472-483`. (ii) `fonts.cljs` (OI-2) is at
  `src/app/client/workspace/runtime/fonts.cljs` (NOT `substrate/webgpu/`).
  (iii) the transcript reader/watcher file is `dogfood/transcript.clj` (the ns
  `dogfood.transcript` IS cited correctly; only the bare filename misleads).
  (iv) the `0.56` legacy literal is at `rect_tree.cljs:283`, not `:284` (S3
  greps it anyway).
- **A7 (P5) — name where `parse-trail-command` lives, and thread
  `!trail-face-state` into `agent_flow.cljs` scope.** The plan references
  `trail-face/parse-trail-command` (a pure string->address fn — belongs in a
  `trail_face/*.cljc`, e.g. `text_face.cljc` or a small `commands.cljc`) and
  `reset!`s `!trail-face-state`, but does not name the file for the parser nor
  explicitly thread `!trail-face-state` into `submit-agent-run!`'s atom scope.
  Both implementer-obvious; name them so nothing is dropped.
- **A8 (P6) — record that the md watcher adds a `slurp` reader (not a new truth
  path).** Trap-2 compliance rests on the watcher calling
  `markdown-source-import-request` + `append-object-container-request!`
  VERBATIM; only the file read (`slurp`) is new. State this explicitly in the
  watcher so a reviewer does not read it as a second ingest path.

---

## 3. Open doubts (non-blocking; falsifier named)

- **D1 — the cljc-on-JVM spike is un-reverifiable here (throwaway deleted).**
  The plan §1b attests `:SPIKE-OK` (a pure client-dir `.cljc` loads + runs on
  the `:test` classpath; reader conditionals -> `:clj`; surrogate-safe
  codepoint counting). I independently confirmed the low-risk half:
  `rect_tree.cljs` requires only `clojure.string` (zero js interop, verified in
  the retro/spec), so its `.cljc` promotion is safe. The spike itself rests on
  the plan attestation. Falsifier: the P3 first compile-check (`clojure -M:test`
  requiring `trail_face/*.cljc` + `rect_tree.cljc`). If cljc-on-JVM fails
  there, that is the §12 pre-flagged POLICY FORK (ruling 2.1 is load-bearing) —
  escalate, do NOT quietly move logic to `.cljs`.
- **D2 — no B2 baseline suite run this session (correctly).** The working tree
  carries Track-A WP1 in-progress kernel edits (`M relation_kernel.clj`,
  `M relation_kernel_test.clj`), so a full-suite run now would test Track-A's
  WIP, not a clean B2 baseline. Falsifier deferred to the clean gate-17
  regression baseline at P4/P5. The one baseline that matters for the new gates
  (a pure client-dir `.cljc` runs on the JVM) is D1's spike.

---

## 4. Stop-clause / Fable re-entry status

- **NO stop-clause escalation.** The plan is buildable as specified; no
  contract requirement is dropped; no gate is uncovered.
- **NO genuine binding-doc conflict.** view-mvp CONTRACT v1.1, WP1 CONTRACT,
  and decisions.md are consistent. The seam holds (WP1 §7 wrappers + the S1a
  epoch carve-out); read-only is structural. OI-1/OI-2 are NAMED cross-package
  dependencies with conditional triggers (P5/P4), not conflicts.
- **NO Fable re-entry trigger fired** (no policy fork; this is R1, not a second
  FAIL; gates are not yet green).

## 5. Next

- **B2-P3** — pure core + fixtures + gates 1-13 written & compile-checked
  (parallel-safe with WP1 impl). Fold A1 (gate-1 pure-EDN form) and A5 (panes
  cases as a code-cited must) at write time; A6 path corrections apply
  throughout. Per the routing pattern: Opus 4.8 / high (impl), fresh session.
- Then B2-P4 (gates 1-13 green + substrate amendments §5, incl. the OI-2 Sid
  decision), B2-P5 (integration; waits WP1 green; fold A2/A3/A4/A7),
  B2-P6 (watchers + first light; fold A8), B2-P7/P8 (impl + test validation),
  Fable gate.
- Advisory to the authoring session: A1, A2, A3 are the three worth a one-line
  plan-note before P3/P5 for smoothness; none require a plan re-run (all self-
  correct at compile / test-write / first-light).
