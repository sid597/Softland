# Probes — the text tool as records on the current waist (2026-09-08)

Exploration, not materialization: four scripts that try to express the
"one vantage · one subject · the text tool through the compositor · a
pointer" state as records over the engine as it is (`engine/executor.cljc`,
`engine/surface.cljc`, the path kind's capability table). Nothing under
`src/` was changed. Every capability a probe adds is marked `CODE #n` in
the source; everything else is data.

Run from the repository root (the JVM reference runner, no browser):

```
clojure -M probes/text-tool-on-the-waist/probe.clj
clojure -M probes/text-tool-on-the-waist/probe2.clj
clojure -M probes/text-tool-on-the-waist/probe3.clj
clojure -M probes/text-tool-on-the-waist/probe4.clj
```

Recorded outputs of the runs that produced the numbers quoted in the
session are beside the scripts (`run-1.txt` … `run-4.txt`), with the
pictures (`*.png`, zoom 8 of a 34 × 22 local surface).

| Probe | What it shows |
|---|---|
| 0a–0d | Leaf-language limits: a string cannot be loop items; `:get` takes no computed key; no `:and` `:conj` `:count` `:str` `:let`; loop items are projected strictly to the declared fields. |
| 2a | A run of text drawn through the CPU compositor with the existing path table only: glyphs as rect sources, pen and wrap rule as expressions, caret position in state. `text-2a-zoom8.png`. |
| 2b | The shaper as a capability (`:text/shape`, CODE #1); the keystroke stream as its own root, reached only by `:items`, so it is not in the recipe. |
| 3a | Sampling the painting: contributors name `surface@revision` of the whole painting; a transparent texel inside the surface reads `covered? true`. |
| 3b | Pointing as a hit record over the same items (boxes, zero code): what · glyph · index · where · by · drawn-by. The layout is re-derived by the hit: two sources. |
| 4 | The vantage's subject as a query over a store of records: a count needs no code; collecting the matches needs `:collect` (CODE #2). A where-clause must carry every field the match reads. |
| 5 | Typing = append a key and resume with `:until`: per-keystroke time vs a full rerun, continuation bytes, at zoom 1 and zoom 8. |
| 5b | Append resumes; editing a consumed key refuses `:consumed-items-differ`; moving the run refuses `:recipe-differs`. |
| 6, 6' | Point at the tool, change one field (width, wrap rule): the text redraws, the cursor record is unchanged, the caret is where the offset is, the run's kind is intact. Resuming typing under the edited tool refuses `:recipe-differs`. |
| 7 | A foreign paste draws at half size: a tool rule over the asserter, zero code. `text-7-foreign-zoom8.png`. |
| 8 | Two vantages on one subject differ only in the painting root; returning to a vantage is byte-identical; a continuation keeps every record's kind. |
| 9, 9b, 9c, 13 | One source: the layout record emits placements (through `:collect`); paint, hit and caret consume them through `:inputs`. A retained output must be a map to carry its `:subject`. `text-13-chain-caret-zoom8.png`. |
| 10, 11 | Where a keystroke's time goes at zoom 8, and the kill-probe: `surface/new` costs 1.5 s unhinted and 6.7 ms hinted on the JVM (reflection on the float array). |
| 12 | `check-inputs!` accepts a painting produced from a five-event stream as the subject of the four-event record (the repo's own sphere fixtures); an edited tool root is refused. |
| 14 | Continuation bytes grow quadratically when the loop collects (state-after per history row). |
| 15 | A complete run keeps no continuation; suspension needs an unconsumed item. |
