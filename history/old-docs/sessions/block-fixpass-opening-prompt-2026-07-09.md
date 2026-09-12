# Block-distiller P1/P2 FIX-PASS — opening prompt (fresh session)

**You are the coordinated fix-pass session** for the block-kernel package. A
fresh-context falsification review of P1/P2 found the code functionally correct
(gates green) but surfaced 4 items; **Sid RULED the recommended fixes** (decisions.md,
2026-07-09). Your job: apply F1+F2+F3+N5 to the **settled tree**, re-green the FULL
suite, one code commit on Sid's word.

## DO NOT START until P3 has landed and is stable
The fixes live in `block_distiller.clj` + `block_distiller_test.clj`, which the P3
session was live-editing (edges G8/G9 + relation-kernel). **Confirm P3 is done**
(baton NOW shows a P3 close entry; `block_distiller_test` runs green including G8/G9)
BEFORE editing. Apply every fix to the CURRENT file state — re-Read each fn first; do
NOT reuse the P1/P2 line numbers below verbatim, they will have moved.

## Boot
1. `/work-package` + `/rama` (+ `/rama-pitfalls` only if you touch a foreign-append shape).
2. Read `docs/current-mental-model/build/sense-line-mvp/block-kernel/P1P2_FALSIFICATION.md`
   (the findings + dispositions in full) and the RULED entry in `decisions.md`
   (search "block-kernel P1/P2 falsification"). SPEC §6.1/§4.4, CONTRACT R3/R4/T4.
3. Read the CURRENT `block_distiller.clj` + `block_distiller_test.clj` (post-P3).

## The four fixes (all in-allowlist: block_distiller.clj + test + fixtures)

### F1 — SPEC §6.1: don't mint a sub-block coincident with the whole-message block
In `free-cut-part`, the `:human-message` branch currently does
`(into (whole-block :human-message text) (free-cut-text text {:human? true}))`. For a
**single-paragraph** human turn the lone `:human-sub` span equals the whole `[0,len)`
→ two unit-ids at one `(surface,span)` (§6.1 MUST violation). **Fix:** drop any
`free-cut-text` sub-block whose span == `[0,(count text)]`:
```
:human-message (let [whole (whole-block :human-message text)
                     len   (count text)]
                 (into whole
                       (remove #(and (= 0 (long (:start-offset %)))
                                     (= len (long (:end-offset %))))
                               (free-cut-text text {:human? true}))))
```
Multi-block human turns are unchanged (their subs are proper sub-spans). **This is a P0
`§F` edit** — the existing fixture golden is UNCHANGED (its human msg is multi-para), so
the fix would be UNTESTED unless you add a case:
- **Add event 11 to `fixture.jsonl`** (at the END, so existing indices/block-paths don't
  shift): a single-paragraph human turn, e.g.
  `{"type":"user","isMeta":false,"userType":"external","uuid":"u-solo","parentUuid":"a-sub","isSidechain":false,"timestamp":"2026-07-09T10:00:07.000Z","message":{"role":"user","content":"just one line, no structure."}}`
- **Regenerate the golden** (the `comment` block at the foot of the test ns) and EYEBALL:
  event u-solo must yield exactly ONE block (form `:human-message`, span `[0,len)`), NOT a
  whole + coincident sub.
- **Update the pure-test constants** the new event shifts: `g1-classification`'s expected
  class vector (append one `:river`) + its river count (4→5) + total (10→11); and in
  `distiller-import-gates`: `(:river summary)` 4→5, message-rows `(= 10 …)`→11, the river
  `:message-uuid` set (add `"u-solo"`), and the debris subtraction (11−5=6). Grep the test
  for `4`/`10` literals tied to river/debris counts.

### F2 — T4 routing: make the import-key recoverable by extract-object-key
`import-key` builds `"imp:sense-block:" …` which `extract-object-key` (object_container.clj
:284-339) cannot parse (falls to `:else` → whole string → foreign `read-import-completion`
mis-routes to nil). **Fix** (block_distiller.clj `§A`):
```
(str "imp:tr:" object-key ":sb:" (core/sha-256 (str event-uuid)))
```
`extract-object-key` strips `imp:tr:` → `leading-object-key` → `chat:<hex>` (routes right);
`:sb:`+hash keeps it a DISTINCT full key so R4 "second distillation dedups on its own key"
holds. Update the `import-key` docstring accordingly. **Add a gate:** a deftest asserting
`(ocr/read-import-completion oc-rt (bd/import-key object-key <river-event-uuid>))` returns a
non-nil completion after `distill-conversation!` (proves the routing fix; would fail today).
NB the `id-minters` pure test asserts `(str/starts-with? (bd/import-key …) "imp:sense-block:…")`
— update that expectation to the new prefix.

### F3 — G4 test power: distinguish clean-replay from conflict-reject
In `distiller-import-gates` G4 block, the re-run `distill-conversation!` result is discarded.
Capture it and assert the re-run's decisions are all `:accepted` AND its `:river` count ==
the expected river count (a fingerprint-conflict reject writes no rows → today G4 greens on
it). Fold in nit N1 (add a `(some? u)` guard on the before/after unit reads).

### N5 — docstring: `import-payload` says "12-key"; it builds 10
Change "The 12-key OC import payload" → "The 10-key OC import payload".

## Gate to green
Re-run the FULL block-distiller suite (pure + F2 spike + `distiller-import-gates` with G1/G2
+ P3's G8/G9 + the new F2 routing gate) AND `object_container_test` in one JVM — all green.
The F1 golden regen + count updates are the fiddly part; run pure tests first to converge the
golden before the IPC gates.

## Rules
- File allowlist: `block_distiller.clj` + `block_distiller_test.clj` + fixtures ONLY. No
  `object_container.clj` edit (F2 is solved adapter-side — do NOT add a kernel `extract-object-key`
  branch; Sid ruled the in-allowlist restructure). No `relation_kernel.clj` edit beyond P3a's.
- CODE uncommitted until Sid's word; code/docs separate commits; docs auto-commit on the local
  branch, never pushed. NEVER read `src/app/server/env.clj`.
- Append a NOW entry (≤15 lines) at session end; if F1's §6.1 realization warrants it, note the
  SPEC §6.1/§4.4 amendment (subs are minted only for proper sub-spans) in-place.
