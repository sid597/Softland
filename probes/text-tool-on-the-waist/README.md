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

## The view — the same records in the browser (`view/`)

`src/app/client/view/` is the first client built out of the tools: the
records, the store, the runner and the browser entry (its README maps them).
`view/index.html` is the page, `view/drive_view.cjs` drives it in headless
Chromium through Playwright and writes `view/receipts/` (screenshots and
`receipts.edn`).

Build and drive, from the repository root:

```
# the repository's toolchain (untested in the session that added the build entry)
clj -M:dev -m shadow.cljs.devtools.cli release view
# the route that session used, Maven being unreachable there: the standalone
# ClojureScript jar from https://github.com/clojure/clojurescript/releases (r1.11.132)
java -cp cljs.jar:src cljs.main -O simple -d target/view/out -o target/view/main.js -c app.client.view.core
cp probes/text-tool-on-the-waist/view/index.html target/view/
node probes/text-tool-on-the-waist/view/drive_view.cjs target/view probes/text-tool-on-the-waist/view/receipts
```

What the receipts show, one run in this session's headless Chromium (no
GPU; `--use-angle=swiftshader`; a 480 × 270 surface at zoom 4; the numbers
move by tens of percent between runs):

| Checkpoint | Receipt |
|---|---|
| The view drawn by the CPU runner on a 2D canvas | `01-initial.png`; every tool `:complete`; 47 placements, 42 rings |
| The pointer names what it is on | `02-pointer-on-t.png`; hit = run-1, "t", index 0, at [4 4], by sid, drawn by paint@1; on the paste: run-2, "a", by clipboard; on nothing: nil |
| Typing, Backspace, Enter as keystroke records | `03-typed.png`, `04-enter-line.png`; the cursor moves with the text |
| Point at the tool, change one field in the page's editor | `05-width-60.png` (wrap at 60), `06-foreign-full-size.png` (foreign scale 1); status "applied layout@1" |
| Move the cursor record; the caret follows | `07-cursor-at-3.png` |
| The store survives a reload | `08-after-reload.png` |

| Measured | ms |
|---|---:|
| First frame: query · layout · paint (47 items, 42 rings) | 12 · 183 · 304 |
| Present (surface value to the canvas) | 9 to 17 |
| One keystroke (61 to 70 items, whole frame rerun) | 420 to 520 |
| One pointer move (the hit tool over 47 to 70 placements) | 10 to 15 |
| One ring's paint, direct / through the executor | 7.7 / 6.5 |
| Copying the surface once (518,400 floats) | 1.0 |
| The same copy in node on this machine | 1.5 |
| Paint as ONE region over the union box (the first form tried) | about 1050 |
| Layout before / after the `:value` word | 426 / 179 |

Where the time goes, attributed: painting is per ring (one coverage pass over
the ring's box, one copy of the surface) and a ring costs about 7 ms of which
the copy is 1 ms; one region for all the text costs a coverage pass over the
union box with every segment of the line in its band. The layout's cost is
the executor's interpretation: every step's expression is compiled at every
evaluation, and without a `:let` the pen expression was embedded in every
place that read it until the `:value` word named it once per item.

