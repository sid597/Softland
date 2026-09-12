# SEAM-STEP1 contract — validation round 1 (fresh context, default-fail) — VERDICT: fail

Run 2026-08-03 by a fresh-context Opus subagent over `SEAM-STEP1-CONTRACT.md`
(as first written), decisions.md "The render seam", `SEAM-STEP1.md`, and the
live source. Preserved verbatim per the never-overwrite-a-FAIL rule. The
authoring session re-ran with this artifact as input; the fix ledger is at
the bottom.

---

**VERDICT: fail** — four defects reach code or force a mid-phase stop; nine more are minor-fail fixes.

### FAIL-class

**1. §4a's central premise — "stamped ≡ refreshed" — is false in the committed tree. Proven by execution.**
`build-slot` sets `:stack-path (or stack-path [])` (`scene_store.cljc:87`). `[]` is truthy, so `slot-entry`'s chain `(or (:stack-path effective) (:stack-path slot) [[cid 0 0]])` (`scene_store.cljc:217-222`) never reaches the fallback: the 1-arg form uses `[]`, the 2-arg form uses the composed path. §4a's reasoning covers registry *mutation* (`set-transform` :143 → `:affine` only ✓, `add-container` :86 throws on re-add ✓, `remove-container` :158 refuses parented cids ✓) but never the *never-stamped* slot.

JVM probe against the live tree (two stampless slots, containers at layer 9 and layer 0):
```
stampless, layers 9/0 — paint order: [[:scene-slot [:vi :a]] [:scene-slot [:vi :b]]]
stampless, layers 9/0 — pick order : [[:scene-slot [:vi :b]] [:scene-slot [:vi :a]]]
```
Every fixture in `test/app/client/workspace/scene_store_test.clj` is stampless (`:41-42, :110-116, :290-291, :403-404, :505-506, :582-583`) — and §13 names that file as the "idiom source" for the new test ns. `workshop_playground.cljs:143` is a live stampless writer into the same store. Consequences: (a) making `pick` walk the stamped-keyed `:ordered` map is a **pick-order behavior change**, not an equivalence; (b) G1's stamped≡refreshed probe fails on the first house-idiom fixture → **§S3 fires**, and §S3's diagnosis ("the registry-immutability fact is broken") names the wrong cause. This is a policy fork (normalize `:stack-path` at the upsert edge / reject stampless slots at validate / keep pick on the refreshed batch), i.e. §S4 territory, not something an implementer may improvise.

**2. G1 — the growth law's fence — is a tautology and goes green proving nothing.**
§4b: "`scene-tape` 1-arg and `ordered-slots` 1-arg walk `(vals (:ordered store))` — no compile." §11 G1: "assert `(vec (vals (:ordered store)))` ≡ `(:entries (ss/scene-tape store))`". After §4b these are the same expression. The only surviving batch route is the arity-2 form `(scene-tape store {})`, which the contract never names as the oracle. Fix: name an explicit oracle entry point (e.g. keep `scene-tape` 1-arg as the batch compile and add `ordered-entries`/`maintained-tape` for the walk), and restate G1 as `maintained ≡ <named-oracle>` at every step.

**3. The pick point-map change breaks nine committed test call sites in two files §12 forbids editing.**
`(ss/pick store effs [x y])` at `test/app/client/workspace/scene_store_test.clj:59,60,293,297,310,327,416,417` and `test/app/client/substrate/scene_tape_test.clj:126` pass a bare point vector. §6b changes the point to `{:world … :screen …}`; T9 fixes the caller count at six and orders "a seventh caller found = artifact flag, **not** silent update"; §12 allowlists neither test file; G9 demands the full JVM suite green. Those four clauses cannot all be satisfied → **guaranteed §S6 stop in Act 3**. (T9's six also conflate direct callers with transitive ones: the only direct `ss/pick` callers in `src/` are `scene_runtime.cljs:312` and `scene_store.cljc:461`; `bundle-for-viewport` :328 and `bundle-at-world-point` :341 reach it through `context-bundle`.)

**4. §4b and §11 G2 contradict each other on the frame oracle; `compile-frame-tape` is orphaned, not demoted.**
§4b: "`compile-frame-tape` SURVIVES as the oracle behind fence G2." G2 actually drives "the cljc arrangement ops … vs the `compile-tape` oracle". `compile-frame-tape` is `(defn- …)` in a `.cljs` file (`renderer.cljs:2185`) — no JVM test can call it, and its only caller is `renderer.cljs:2277` inside `draw-frame!`, which Act 1 removes. It ends the phase unreachable and unexercised. decisions.md:409-410 ("a batch stage is never deleted when its incremental sibling arrives — it is demoted to that sibling's oracle") is not satisfied by an oracle nothing runs. The contract must name what executes it (dev-flag frame-edge comparison, or a cljs-side check) or state that the demotion is nominal.

### MINOR-FAIL — exact fixes

**5. §4b "`entry-key-compare` … REUSES `compare-order` on the token" is a substance error.** `compare-order` destructures `(:order left)` (`scene_tape.cljc:427-437`); handed a bare order token it compares nils and returns `0` for every pair — the sorted map collapses to one entry.
> Corrected: "…reuses `compare-order`, which takes ENTRIES: call it as `(compare-order {:order lt} {:order rt})`, or export a token-level `compare-order-token`. Never pass a bare order token to `compare-order`."

**6. §6b "the per-entry hit fn selects by the entry's effective `:camera`" — no such key exists.** `ctn/effective` emits `{:affine :flags :layer :stack-path :transport-slot}` (`containers.cljc:236-241`); `screen-bounds` :287 selects with `(= 1 (:flags eff))`. §6a states this correctly, §6b contradicts it, and §12 makes `containers.cljc` read-only so no `:camera` key can be added.
> Corrected: "…selects by the entry's effective screen flag — `(= 1 (:flags eff))`, the same predicate `screen-bounds` uses (`containers.cljc:287`)."

**7. §5b's memo key is unreadable at the stated key and unsound as a key.** `tl/layout` stores the revision at `[:source :revision]` (`text_layout.cljc:222` and `:470`), not top-level `:source-revision`; `electric_flow.cljc:441` is the *input* arg. Worse: `position-text-op` computes fresh layouts inline (`renderer.cljs:1467-1474`) passing **no** `:source-revision` → nil key for every such op → one cache entry shared across layouts with different origins/baselines. Nothing in G1–G10 catches this.
> Corrected: "…index memoized on the layout-result's identity (`identical?` on the map that arrives as `(:layout-result txt)`), falling back to no cache when absent. The revision, when present, is read at `[:source :revision]`; it is a hash and must never be the sole cache key."

**8. §11 G3 "its two required tokens" undercounts the scene-tape fence's pins.** The same two regions also require `execute-scene-tape!` (`verify_scene_tape_fence.mjs:71`) and `containers/inverse-point` (:83); the forbid list omits the legacy-layer forbid (:85); and `(defn- compile-frame-tape` is the **end anchor** of the `frame-family-registry` slice (:51) — moving or deleting it silently re-slices the audited region.
> Corrected: "…the required tokens in both audited regions (`compile-frame-tape` AND `execute-scene-tape!` in `draw-frame!`; `scene-tape/pick-reverse` AND `containers/inverse-point` in `pick`) re-pin to the maintained-walk names; every other `requireToken` and every `forbid` — including the legacy-layer forbid — stays; if `compile-frame-tape` moves, the `registryEnd` anchor at :51 moves with it."

**9. G6's ONE-SHOT "before" procedure destroys the tree §3 exists to protect.** "capture it from the pre-package git state at the sitting (checkout, measure, return)" — the working tree holds both the package's edits and the foreign uncommitted set; `git checkout`/`git stash` in the shared tree is exactly the write §3 and the parallel-sessions git discipline forbid. No actor is named for the measurement either.
> Corrected: "…capture it from a read-only worktree of the pre-package commit (`git worktree add <tmp> HEAD`; never `checkout`/`stash` in the shared tree, which would destroy §3's foreign work) — Sid runs it at the sitting, or waives with the known baseline; name which in the receipt."

**10. G4's ordering gate has no checkable receipt.** §10.3 forbids commits, so no temporal evidence exists; the reviewable form is an end-state invariant.
> Corrected: "…the four assertions land green and are recorded in the artifact BEFORE the unification decision. Reviewable invariant: the diff contains zero `m/signal` unless assertions (1) and (2) are recorded green; if either falsified, §S2's fallback shape is the only legal diff."

**11. A third `:line/id` rescan consumer exists and neither doc names it.** `text_layout.cljc:669` `clip-result` runs the identical `(first (filter #(= requested-line-id (:line/id %)) …))` scan and is reached from two text-fence owners (`combined_text.cljs clip-editor-text-op`, `rect_tree.cljc tree->text-ops`). SEAM-STEP1.md:44-45 says "electric_flow / renderer lookup sites"; `electric_flow.cljc:445` is an `nth`, not a scan — so the starter's second site is really this one. Name it as explicitly deferred, or the implementer either misses instance #2's real surface or edits an un-allowlisted file (§S6).

**12. G8's precondition and environment are unnamed.** `run_verifier.mjs:11,29-35` needs `puppeteer` plus `/usr/bin/google-chrome` (or `RENDER_VERIFIER_CHROME`) and a built bundle at `target/render-verifier/js/main.js`. The gate is assigned in-phase to Codex with neither the build step nor the browser requirement stated; if the environment lacks either, G8 is an unowned gate wearing an owner's name.

**13. Two small shape ambiguities.** (a) `(rseq (:ordered store))` yields `MapEntry`s, not entries — pick must take `val`; the contract's "walks `(rseq (:ordered store))` with `pick-reverse`'s exact filters" reads as if entries come out directly. (b) `ordered-slots` has zero callers anywhere in `src/` or `test/` (only its own definition, `scene_store.cljc:257-263`) — §4b spends spec on dead code. (c) The frame-arrangement keying is two-readable: "the arrangement DIFFS the produced `[entry-id order-token]` pairs" + "paint payloads rebind every frame" admits both "arrangement holds ordered entry-ids, entries looked up per frame" and "arrangement holds a sorted map of entries, each re-assoc'd per frame"; name one.

### Verified clean (no finding)

Every §13 locator checked resolves with substance intact — `scene_store` 135/161/177/194/213/240/257/265/445/527; `scene_tape` 393/399/427/439/469/493/502, first four `defn-` ✓; `scene_runtime` 33/304/328/341/384-416; `renderer` 1452/1474/1904/1916/1948/1973/1988/2007/2021/2151/2185/2194/2207; `render.cljs` 187 (`;; first-light P2b` ✓)/220/556; `editor_compute` 414/432/463/479/530/604/626/645/665 + guard 699-700; `containers` 86/143/221/236/284; `ground` 87/169/3270; `mouse` 460; fences 70/82/84 and 84-87/130; `test_runner` `pure-namespaces` :31, `app.missionary-claims-test` :43; `missionary-reference.txt:899`. Also confirmed: all store writes funnel through `upsert-slot`/`remove-slot` (`:194`); `pick` compiles the full tape every call (`:271-273`); `:stable-tie` = vi makes `compare-order` total (`compare-scalar` pr-str fallback, `:396-397`); `:scene-tape` (`scene_runtime.cljs:415`) has zero consumers, so dropping it is safe; `:world/revision` has one producer and zero consumers (`scene_tape.cljc:487`); the starter's close conditions, standing guards, prediction check and fence-never-deleted law are all carried (§7, §10.4, T7).

---

## Fix ledger (applied by the re-run of the authoring phase, 2026-08-03)

- F1 → RULED (Fable, flagged for Sid's veto at the preview): **pick-follows-paint.**
  The maintained (stamped) order is the one order truth for BOTH projections;
  the 1-arg batch compile is the named oracle (equivalence exact by the
  total-order fact); the 2-arg refreshed form is retired from the pick path.
  For registry-stamped slots (all committed production writers) nothing
  changes; for stampless slots pick order changes to AGREE with paint — which
  is `pick`'s own docstring promise ("the exact reverse of the paint tape")
  that the refreshed path was silently breaking. §4a/§4b/G1/§S3 rewritten.
- F2 → oracle named: `scene-tape` stays the batch compile (both arities,
  untouched); NEW `maintained-entries` is the walk. G1 restated over the two
  distinct code paths.
- F3 → `pick`'s point argument is backward-compatible (bare `[x y]` = world
  for both spaces, exactly today's semantics; map form adds `:screen`); the
  nine committed test call sites stay untouched and green; T9 re-enumerated
  as direct-vs-transitive.
- F4 → the frame batch stays ALIVE: dev-flagged twin-run door in the renderer
  (arrangement ≡ compile-frame-tape per frame when enabled) + JVM G2 over the
  shared cljc ops; G10's sitting runs a minute with the flag on as receipt.
- F5–F13 → applied per the validator's corrected wordings (comparator entry
  shape · `:flags` predicate · layout-result-identity memo · full fence-pin
  inventory · read-only worktree for G6-before · G4 end-state invariant ·
  `text_layout.cljc:669` scan named-deferred · G8 environment + owner-flip
  clause · MapEntry `val` · `ordered-slots` de-spec'd · frame-arrangement
  shape named as the sorted-map form).
