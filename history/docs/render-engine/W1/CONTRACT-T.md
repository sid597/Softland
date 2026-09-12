## 7. Contract T — one text layout result

### 7.1 Separation of shaping/layout from painting

One pure, versioned layout call consumes source text, font/shaper identity,
style runs, constraints, index-space declaration, and layout policy. It emits
one immutable result. Measure, wrap, glyph painting, caret, selection, clip, and
hit test are readers of that result; none independently re-measures text.

Illustrative shape; fields and routes are binding:

```clojure
{:text-layout/version 1
 :layout/id <hash of all semantic inputs>
 :source {:id <material-id> :revision <revision>
          :text <source text>
          :index-space <explicitly tagged index domain>
          :source-map <index/cluster mapping>}
 :font {:face-id <id> :face-revision <digest/version>
        :size <n> :variations <axes> :features <features>
        :fallback-chain <ordered revisions>}
 :shaping {:shaper-id <id> :version <version>
           :language <tag> :script <tag> :direction <dir>}
 :space {:coordinates :material-local}
 :constraints {:inline-size <width|unbounded>
               :wrap <policy> :line-height <policy>
               :alignment <policy> :tab-stops <policy>
               :clip <geometry ref>}
 :metrics {:advance [w h] :ink-bounds <rect>
           :logical-bounds <rect> :ascent <n> :descent <n> :leading <n>}
 :lines [{:line/id <stable-within-layout>
          :source-range [start end]
          :baseline [x y] :advance <n>
          :logical-bounds <rect> :ink-bounds <rect>
          :run-range [start end]} ...]
 :runs [{:source-range [start end] :direction <dir> :font-revision <id>
         :glyphs [{:glyph-id <id> :cluster <source range>
                   :position [x y] :advance [x y] :offset [x y]
                   :ink-bounds <rect>} ...]} ...]
 :clusters [{:source-range [start end]
             :caret-stops [{:index <tagged source offset>
                            :position [x y] :affinity <upstream|downstream>} ...]
             :logical-bounds <rect> :ink-bounds <rect>} ...]
 :clip-plan {:visible-lines <ids> :visible-glyph-ranges <ranges>
             :clip-geometry <shared geometry ref>}
 :receipts {:input-hash <hash> :output-hash <hash>
            :font/shaper/environment <fingerprints>}}
```

No untagged source offset is legal. T0 may adapt the current ClojureScript/JS
UTF-16 code-unit behavior behind an explicitly tagged legacy index space while
preserving output. T2 later adds Unicode-safe editing, IME, paste, and source
mapping through the same contract; it does not create a second layout seam.
Caret stops are cluster/shaper output, not assumed character boundaries.

### 7.2 The seven readers

| Consumer | Only lawful source |
|---|---|
| measure | `:metrics`, line logical/ink bounds, and declared constraint result |
| wrap | `:lines` and their source ranges; no `width / char-advance` reconstruction |
| paint | positioned glyph IDs/positions plus paint style; backend adds coverage only |
| caret | declared cluster caret stops, bidi direction, and affinity |
| selection | source range → cluster/line logical regions from the same result |
| clip | `:clip-plan` and glyph/line bounds; no character-count substring guess |
| hit test | point → line/cluster/caret stop through result geometry and inverse transform |

Object-level text picking and edit-caret hit testing may return different
semantic subroutes, but both read the same layout result and Contract-O entry.
The result is material-local and reusable across instances. Instance affine is
a shared projection input to paint/caret/selection/clip/hit, not a reason to
reshape. Road-specific hinting or pixel snapping is a versioned projection
included in receipt identity and shared by every affected reader; it cannot be
applied independently by paint or caret, and it cannot change source ranges or
semantic advances.

### 7.3 T0/T1 fence

T0 inventories owners, not literal occurrences. Its executable fence rejects an
independent text consumer that derives geometry with a private `0.56`,
`count × advance`, `width / advance`, max-character wrapping, substring clip,
or point/column division outside the layout provider/adapters. Current examples
that must route through the seam are cited in §1; W1 does not edit them.

T0 is behavior-identical: a legacy monospace provider may reproduce current
advance, wrap, caret, selection, clip, and hit outputs exactly, while making the
single result observable and testable. T1 replaces that provider with real
shaping and proportional metrics; every reader moves together. MSDF and
Slug-direct are interchangeable paint consumers of positioned glyphs. The
Slug-retirement candidate receipt may choose between them only after T1's
layout/shaper boundary is preserved.

### 7.4 Contract-T receipts

- **T0-1 identity:** all seven consumers hold the same layout ID/input hash.
- **T0-2 zero diff:** current monospace corpus preserves pixels, wraps, caret,
  selection, clip, and hit results through the legacy provider.
- **T0-3 executable fence:** seeded private-metric consumers fail CI; production
  owner inventory has no unapproved independent metric route.
- **T1-1 shaped corpus:** ligatures, kerning, combining marks, bidi, fallback,
  variable axes, tabs/newlines, and proportional text agree across all readers.
- **T1-2 backend parity:** the same positioned glyph result feeds MSDF and Slug;
  switching paint roads changes coverage quality/performance receipts, never
  wrap/caret/selection/hit identity.
- **T1-3 legal zoom:** tiny and large text is tagged by precision/backend regime
  across `[0.01,1000]`; no road uses a regime tag to exclude legal material.

