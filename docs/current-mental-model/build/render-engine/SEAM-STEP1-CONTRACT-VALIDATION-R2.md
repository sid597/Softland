# SEAM-STEP1 contract — validation round 2 (scoped, fresh context, default-fail) — VERDICT: fail → repairs applied

Run 2026-08-03 by a second fresh-context Opus subagent, scoped to the four
FAIL-class resolutions from R1 (`SEAM-STEP1-CONTRACT-VALIDATION-R1.md`).
Preserved verbatim per the never-overwrite rule. Fix ledger at bottom.

---

## VERDICT: **fail** — the F1 amendment (RULING R1) introduces a new FAIL-class defect that falsifies the F3 resolution's central guarantee. F2 passes clean. F4 passes with two enumerated gaps.

### FAIL-class (new, introduced by the amendment)

**N1. RULING R1 turns a committed test RED in a file §12 forbids editing — proven by execution.**

`test/app/client/workspace/scene_store_test.clj:280` `pick-layer-and-miss` is a **stampless** fixture with **overlapping** containers whose entire point is order:

```clojure
(ctn/add-container 10 {:x 0 :y 0 :scale 1 :layer 1})
(ctn/add-container 20 {:x 0 :y 0 :scale 1 :layer 5})
(ss/upsert-slot vi-lo {:tree tree-lo :container 10})   ; no :stack-path
(ss/upsert-slot vi-hi {:tree tree-hi :container 20})   ; no :stack-path
;; :293  (ss/pick store effs [50 50])
;; :294  (is (= "addr/hi" (:address p)))
;; :295  (is (= vi-hi (:vi p)))
```

JVM probe against the live tree (fixture copied verbatim; ruled pick = `pick-reverse` over the 1-arg/stamped entries, hit fn unchanged):

```
slot stack-paths (stampless): [[] []]
REFRESHED (today's pick) forward order: [[:scene-slot [:vi :lo 1]] [:scene-slot [:vi :hi 1]]]
STAMPED / maintained forward order    : [[:scene-slot [:vi :hi 1]] [:scene-slot [:vi :lo 1]]]

TODAY   pick [50 50] -> {:address addr/hi, :vi [:vi :hi 1]}
RULED   pick [50 50] -> {:address addr/lo, :vi [:vi :lo 1]}
```

Mechanism: both stamped paths are `[]` (`scene_store.cljc:87`), so `compare-order` falls through to `:stable-tie` → `compare-scalar` → `(compare (pr-str [:vi :hi 1]) (pr-str [:vi :lo 1]))` → `hi` sorts *first*, reverse walk hits `lo`.

Four contract clauses cannot all hold: §4a RULING R1 mandates the change and G1 probe (b) requires the divergence observable; T2 + §4b close every dodge; §13 lists :293 as stay-green-untouched; G9 demands the full suite green with the file un-allowlisted → **§S6 stop in Act 1**. The stampless divergence still trips a WRONG stop clause — not §S3 (correctly carved out) but §S6/G9.

Scope check — all nine sites plus transitive ones traced: :59/:60 (non-overlapping), :297 (miss), :310/:327 (single container), :416/:417 (disjoint x-ranges), `scene_tape_test.clj:126` (fixture IS stamped → stays green), `two-scale-fixture` bundles :608/:629 (non-overlapping). **Exactly one break — :293–295 — but one is enough.**

Minimal repair, in the contract's own register (no ruling reversal): add `scene_store_test.clj` to §12 as *the single named exception*, with the exact edit fixed in the contract text — `pick-layer-and-miss` gains `:stack-path (:stack-path (get effs 10))` / `(get effs 20)` at :290–291 (making the fixture registry-stamped, which is what every production writer does), assertions unchanged. §13's stay-green list drops :293 and gains: "one committed fixture is stampless-and-overlapping; RULING R1 changes its result — it is re-stamped, not re-expected." Alternative (zero test edits): reopen the fork — §S4 territory.

### F2 — PASS

Oracle genuinely distinct (`maintained-entries` vs `scene-tape`/`compile-tape`); the tautology-recreating reading is closed by "BOTH arities — stays the batch compile, byte-untouched" + T6. Residuals (not defects): G1's pick-maintained assertion now fences the filter path, not order; `validate-entry!` is `[registry entry]` — the store must pass `default-family-registry` (`scene_tape.cljc:384`), unnamed in §4b.

### F3 — mechanism correct, guarantee falsified by N1

Backward-compat normalization sound (bare vector → both spaces → identical `inverse-point` input; no committed fixture uses `:camera :screen`). No sentence still mandates the map form. T9 substantively right, hints off: `context-bundle`'s direct call is `scene_store.cljc:459` not :461; §5b rescan is `renderer.cljs:1476` not :1474; `run_verifier.mjs` puppeteer import :12 not :11. Verified in passing: `:ordered` (PersistentTreeMap, custom comparator) survives `g2-edn-roundtrip-and-no-fns` — `(= store' (edn/read-string (pr-str store')))` → true; `store-fns-free?` → true (rehydrates as PersistentArrayMap — exactly T3's case).

### F4 — alive, with two gaps

Twin-run placement legal (renderer.cljs allowlisted); G2 oracle JVM-callable; G3 pin story matches the fence file exactly (registryEnd :51 · :70/:71 · :82/:83 · sort-by :84 · legacy-layer :85; text fence :84/:129).

**Gap 4a — G10's flag-on minute is defeated by G10's own inheritance clause.** Real use runs flag-off; the twin-run receipt evaporates under inherited wearing.
> Corrected G10: the twin-run receipt is NOT inheritable — a ≥1-minute flag-on run with zero logged divergences is a separate, mandatory line in the G10 receipt.

**Gap 4b — the twin-run door has no named home and the new fence assertion has no anchor.**
> Corrected G3/§4b: name the arrangement-update fn `update-frame-arrangement` and the door `frame-tape-twin-check!`; the no-`frame-idx` assertion slices `findForm(renderer, "update-frame-arrangement")`; `draw-frame`'s :70 requireToken re-pins to `frame-tape-twin-check!` AND a new requireToken pins `compile-frame-tape` inside `frame-tape-twin-check!`, so the demoted oracle cannot be orphaned by a later edit.

### Minor R1 wordings — landed and source-true

F5–F13 all verified landed with citations (comparator-takes-entries · `:flags` predicate · layout-result-identity memo · pin inventory · worktree · G4 invariant · `text_layout.cljc:669` · G8 environment · MapEntry `val` · `ordered-slots` de-spec'd · sorted-map arrangement). §4a re-verified in full: `set-transform` affine-only, `add-container` re-add throws, `remove-container` refuses children, `effective` composes from immutable fields only; all nine `ground.cljs` upserts + `scene_runtime.cljs` :228/:261 are inherit-path updates; the create path is always `register-face-instance!` :153–159.

---

## Fix ledger (applied by the authoring session, 2026-08-03)

- N1 → APPLIED per R2's minimal repair (ruling intact): `scene_store_test.clj`
  enters §12 as the single named exception with the exact re-stamp edit
  pinned in §4a's ruling text; §13 stay-green list drops :293 with the
  reason line. The zero-test-edits alternative (reopen the fork) is noted
  for Sid's veto at the preview.
- Gap 4a → G10 amended: twin-run receipt not inheritable, mandatory line.
- Gap 4b → §4b/G3 amended: `update-frame-arrangement` +
  `frame-tape-twin-check!` named; fence re-pin story per the corrected
  wording.
- F2 residual → §4b names `default-family-registry` as the registry passed
  at the insert edge.
- Hints → :459, :1476, :12 corrected in T9/§5b/§13.
