# Implementation Quirks — Learned Through Pain

Operational wisdom that prevents wasted time. These are patterns discovered by hitting errors, not from reading docs.

## A nil is as likely to be a wrong key as an absent fact (2026-08-21, the R5 round)
- Rama foreign reads return **empty/nil for a wrong key, never an error**. A point read with
  a bad key is indistinguishable from real absence — which reads as data loss.
- Fired three times in one round. `read-transcript-conversation-projection` takes a
  *conversation-container-id* (`oc:chat-conversation:<address>`), not the address; passing
  the address returned `[]` while 43 turns and 406 geometry cells sat there, and the run was
  read as "the restart lost everything". Separately `$$wear-counts-by-face` is **String**-keyed;
  a spec that read it with the keyword `:outline-face` got `nil` and wrote that nil down as
  the expected baseline. Real value: 641.
- **Rule: never re-derive a key — call the accessor that owns it.** `episode/read-turn-records`,
  `read-geometry-cells`, `UnitReadResult`'s `:graduation` all derive their own keys. A probe
  that re-implements key derivation will drift from the writer sooner or later.
- Before reporting absence, prove the key: read something you KNOW exists through the same
  path first. See [[investigation-fence]] — absence is a magnitude claim, not a structure one.

## Ephemeral cluster × durable side-state (caught 2026-07-05, git-spine cursor)
- The app's Rama cluster is `com.rpl.rama.test/create-ipc` — in-memory, EPHEMERAL per JVM; ALL land state rebuilds from re-ingest at boot. Any DURABLE file keyed to cluster contents (cursor, cache, watermark) silently poisons the NEXT boot: fresh empty cluster + "unchanged, skip" cursor = edges never re-asserted.
- Rule: durable side-state must carry the cluster instance's identity (mint a run-id in the same form that creates the cluster; ignore foreign-id state = treat as deleted). The assert-log pattern (replay re-appends into the fresh cluster) is the SAFE shape; a skip-list is the unsafe one.
- Falsify by class in reviews: "stale durable X × fresh ephemeral Y" for every X the boot writes. Same-cluster tests (delete/corrupt/re-run) CANNOT catch this.

## rect_tree clip semantics (fixed 2026-07-05 at git-spine gate)
- A `clip?` node's child-clip INTERSECTS the ancestor clip (post-fix). A node's OWN text clips only to the INCOMING clip — to truncate a node's own label, put the text in a CHILD node under a `clip?` parent (the cards.cljc idiom).

## Edit Tool on large CLJS files
- The Edit tool often fails with "String not found" due to deep indentation (30+ spaces)
- **Workaround**: Use python3 string replacement via Bash when Edit fails on deeply-indented code
- Always re-read the file after modifications before attempting new edits
- After S37 runtime split, no single file exceeds 508 LOC; this is less of an issue now

## Missionary / Reactive Flows
- **m/latest arg counts MUST match fn params** — a mismatch throws a LOUD `clojure.lang.ArityException` on first emission (NOT silent corruption; corrected 2026-07-05, VERDICTS.md Claim 6). The long-arity blocks below are safe because arity MATCHES, not because mismatch would be quiet
  - `<editor-rects`: split into <layout(4), <mode(3), <sidebar(4), <intake-content(7), <run-content(7), <editor-content(17), combiner(6)
  - `<combined-text-ops`: split into <layout(4), <intake-text(7), <run-text(7), main(21)
  - `<cmd-panel-rects` = 11 args
  - These counts change when new atoms are threaded through — always verify after signature changes
- The m/ap + m/?< crash pattern and m/eduction fix are in CLAUDE.md — don't duplicate here

## Reactive Architecture: Coarse Invalidation + Impurity (S38)
- **Problem**: `<editor-rects` and `<combined-text-ops` are single `m/latest` blocks with 25+ watched atoms. When ANY atom changes (e.g. caret blink every 530ms), the ENTIRE function re-runs — including code paths that don't use the changed atom. This is coarse invalidation, not a bug — the reactive graph's invalidation domain is too wide.
- **Impurity compounds it**: `build-right-detail` (dg_flow.cljs:492) looks like a pure builder but secretly mutates `!detail-max-scroll` via `reset!` at lines 502 and 565. Side effects inside derivations are un-Electric — they create hidden edges Missionary can't see.
- **The Electric fix** (three parts):
  1. **Scope** — split each `m/latest` so each branch watches only the atoms it needs
  2. **Purity** — derivations compute and return data, they don't mutate atoms
  3. **Ownership** — each atom has one writer, or better, the value stays derived (a flow, not an atom)
- The unused-branch caveat: even with split flows, all branches stay live unless you use dynamic subscription (second step)

## Rama DSL
- No Java interop inside `<<cond`/`<<sources` — wrap in plain fn
- `local-transform>` = 2 args: `[path-with-termval] pstate`
- Both give cryptic "Unable to resolve symbol: `.`" error

## CLI Integration
- `--max-turns` doesn't exist as a Claude CLI flag — use `--max-budget-usd` instead
- Invalid flags cause **silent exit** (no error, process just dies)
- Agent CWD priority chain: sidebar project root → file parent dir → `"."`
- Ring streaming: Use `StreamableResponseBody` reify (not `piped-input-stream` — Jetty won't flush)

## WebGPU Rendering
- **No GPU scissor rects** — all clipping is software-only (filter render ops outside visible bounds)
- **Clip on text baseline, not bounding box** — `(>= y (+ panel-top padding))` not `(>= (+ y line-step) panel-top)`. Loose checks cause glyph bleed
- **Font atlas only covers ASCII 32-126** — any Unicode renders as blank. Use `> v < ..` not arrows/Unicode
- **Char-width `0.56`** (Ubuntu Sans Mono glyph advance) must be consistent across EVERY file computing character positions — `substrate/webgpu/renderer.cljs`, `workspace/text_input.cljs`, `workspace/settings_view.cljs`, `electric_flow.cljc` (`char-advance`), `workspace/shell.cljs`, `workspace/sidebar.cljs`, `workflows/jit.cljs`, `workspace/face_primitives.cljc` (the ONE named constant `fallback-char-width`) — any mismatch = cursor drift. (Relocated from CLAUDE.md at the 2026-07-16 curation.)
- **Font config**: Ubuntu Sans Mono, size 14 (kitty match); MSDF atlas at `resources/public/font_atlas.{png,json}`; regenerate: `msdf-atlas-gen -varfont "/usr/share/fonts/truetype/ubuntu/UbuntuSansMono[wght].ttf?wght=400" -type msdf -size 64 -pxrange 8 -pots -format png -imageout font_atlas.png -json font_atlas.json` (multi-font merge gotcha below)
- **Horizontal scroll is per-element, not camera**: GPU camera only does vertical (`pan-y = -scroll-y`). H-scroll via `editor-lx = layout-x - scroll-x`. Gutter uses unscrolled `layout-x`
- **Never redefine a shared coordinate variable** for a per-element transform — create a NEW variable (the h-scroll contamination lesson from S30)

## Electric 3
- `try/catch` NOT supported inside `e/defn` — causes "try is TODO" error
- RETRACTED 2026-07-05 (VERDICTS.md Claim 13): `e/watch` = `e/input` over `m/watch` and DOES transfer server→client — `(e/server (e/watch !atom))` is the shipped Rama-truth bridge (file_viewer.cljc:107-190 → electric_flow.cljc:484-489). What cannot cross peers is a RAW Missionary flow; Electric-managed transfer of an e/watch value can and does. The old "HTTP for data transfer, Electric for bootstrap only" pattern is obsolete

## Missionary + Raw DOM: Never Share Mutable State (S35)
- `m/observe` without `m/relieve` **buffers one event**. When `!` is called, the previously buffered value may drain first — a raw mousedown can trigger processing of a stale Missionary mouseup in the same synchronous dispatch
- Symptom: atom flips `true→false` within 1ms, no intermediate events fire
- Rule: if you move state ownership to raw DOM handlers, **remove ALL Missionary writes** to that state
- `add-watch` + `js/console.trace` on the atom is the best diagnostic for identifying the culprit

## Selection / Rect Clipping (S35)
- Any rect computed from character positions must clamp to pane boundary: `(min raw-w (max 0 (- viewport-w x)))`
- 3-pane widths (`0.4 / 0.55 / 0.05`) appear in `shell.cljs`, `editor_compute.cljs`, and `runtime.cljs` — must match everywhere

## Paren Bugs in ClojureScript (S35, S37)
- A missing `)` can push an `:else` cond clause inside a preceding `let` body — syntactically valid, semantically wrong
- `:else` becomes a dead keyword expression; the "else" code runs unconditionally for every match
- Paren checking tools must strip `;` comments and strings before counting
- **S37 "Can't call nil" cascade**: A missing `)` on an inner `let` inside a `cond` branch absorbed the next `:else` branch, making the parent `if` have too many args. The CLJS analyzer reported "Can't call nil" at the outermost `->>` form — 300 lines away from the actual bug. Binary search by stubbing `case` branches with `nil` is the fastest diagnostic.

## set-selection Contract (S37)
- Never use raw `swap! !flow-state assoc :selected new-sel` — always use `swap! !flow-state set-selection new-sel`
- `set-selection` (dg_flow.cljs:62) syncs three coupled fields: `:selected`, `[:batch :lanes]`, `:active-lane-idx` (with clamping)
- Bypassing it causes silent lane drift that only manifests during multi-ticket batch review
- Found in S37 during runtime split: mouse.cljs and keyboard.cljs had copied raw assoc from the monolith

## Shadow-cljs Server Classpath
- Must start with `clj -A:dev -M -m shadow.cljs.devtools.cli server` (not `npx shadow-cljs server`)
- Without `:dev` alias, `src-dev/` is missing from classpath → "The required namespace 'dev' is not available"

## Rama IPC Port Conflicts
- `util_fns.cljc` has a top-level `def ipc` that eagerly creates a Rama InProcessCluster on ns load
- If a previous JVM is still running (`ps aux | grep java`), the new one fails with "System map startup failed"
- Kill stale processes before starting server

## mapv/mapcat Arity with Index (S36)
- `(mapv (fn [idx item] ...) coll)` silently makes `idx=item, item=nil` — NO error in ClojureScript
- Fix: `(mapv (fn [idx item] ...) (range) coll)` to zip index + items
- Same for `mapcat` — found 3 instances of this in trail.cljs table rendering
- Symptom: allocated space (correct height) but empty content (nil cells)

## Rect vs Text Pipeline Consistency (S36, S39)
- `compute-ticket-list-rects` used to pass `font-size=0, char-advance=0` because "rects don't need fonts"
- BUT `build-right-detail` wraps description text to compute heights — different font-size = different wrapping = different heights
- When rect and text pipelines disagree on tree geometry, `m/latest` sees alternating states → infinite re-render oscillation
- Rule: if ANY tree builder uses font metrics for layout (not just text rendering), ALL callers must pass real font-size
- **S39 recurrence**: `handle-mousemove!` and `handle-flow-canvas-click!` in mouse.cljs both called `build-intake-tree` with `font-size=0, char-advance=0` for hit-testing. The rendered tree used real font metrics → row heights differed → hover flickered between adjacent items. Same root cause as S36, different call site.

## Hit-Test Child Stealing Hover (S39)
- **Pattern**: A hovered item adds a `:highlight` child rect. On the next mousemove, `hit-test` returns that child as the deepest node. The hover check (`(= :sidebar-entry (:type target))`) fails because the target is `:highlight`, not `:sidebar-entry`. Hover clears → highlight disappears → next frame the entry is deepest again → hover sets → cycle repeats endlessly.
- **Affected**: sidebar file entries, intake ticket rows — any node that conditionally adds children based on hover state
- **Fix**: Don't use `(peek path)` (deepest only). Walk the hit path with `(some (fn [node] (when (= :target-type (:type node)) ...)) (rseq path))` to find the nearest ancestor of the desired type. This way highlight children, accent bars, indent guides, etc. all resolve to their parent entry.
- **General rule**: If an interactive node can gain/lose children based on hover, the hit-test lookup must walk ancestors, not check only the leaf.

## Layered Backgrounds in Rect Tree (S39)
- `ui-panel`, `ui-panel-header`, and `ui-panel-footer` (ui_primitives.cljs) each paint their own `:bg` from design tokens, on top of any parent background rect.
- Setting a parent `left-bg` to black has no visible effect if `ui-panel` paints `(:bg (:colors dt))` over it.
- Override via the `:style` kwarg: `(ui-panel :id bounds :style {:bg [0.0 0.0 0.0 1.0]})` — the style map is `merge`d over defaults.
- Must override all three layers (panel + header + footer) for a fully consistent background.

## RandomAccessFile.readLine Corrupts UTF-8 + Byte Offsets (F4, 2026-06-07)
- **The footgun**: `RandomAccessFile.readLine()` decodes each byte by zero-extending it into a char (Latin-1-ish), NOT by decoding UTF-8. `é` (bytes `C3 A9`) becomes two chars `Ã©`. JavaDoc even warns "does not support the full Unicode character set."
- **Where it bit us**: `transcript.clj/read-jsonl-observations` (the shared JSONL reader for both transcript modules + the watch path). It then did `(.getBytes line UTF-8)`→`(String. bytes UTF-8)`, a no-op round-trip on the already-corrupted string. Corrupted THREE facets of source identity at once: stored content (`hÃ©`), `byte-length` (6 vs 4 — cascades into every downstream offset), and `line-hash` (hashed over mojibake).
- **Fix**: read raw bytes (`FileInputStream` + `(.position (.getChannel fis) start-offset)` + `BufferedInputStream`), split on byte `0x0A`, decode each line's content slice with `(String. content-bytes UTF_8)`, compute `line-hash` from the exact bytes via `core/sha-256-bytes` (new), and set `byte-length = content + (1 if \n present else 0)` — so an unterminated final line no longer adds a phantom +1.
- **Why splitting on 0x0A is safe**: UTF-8 continuation bytes are `10xxxxxx` and leads are `11xxxxxx`; `0x0A = 00001010` can never appear inside a multi-byte sequence. So a raw `0x0A` is always a real line terminator.
- **Hash basis change is intentional, not a migration bug**: old hashes were over mojibake (corrupt). For valid UTF-8, `sha256(decoded-string) == sha256(raw-bytes)`; they only diverge for invalid UTF-8 (which now yields a parse-error with a byte-correct hash). Don't "preserve compatibility" with the old hashes — that would preserve the bug.
- **Rule**: never use `readLine()` (RandomAccessFile or DataInputStream) when byte offsets or non-ASCII content matter. Read bytes, decode explicitly, compute identity from the byte slice.

## Rama Dataflow + Microbatch Gotchas (compute fix session 1, 2026-06-11)
- **Keyword-as-function does not normalize in dataflow**: `(filter> (= :proceed (:gate/status *gate)))` fails Rama compilation with an opaque "Could not fully normalize ParsedSegment" error (the report shows `:op :gate/status`). Bind through `get` first: `(get *gate :gate/status :> *gate-status)` then `(filter> (= :proceed *gate-status))`. Plain fn calls nest fine; keyword lookups don't.
- **Stream → microbatch conversion silently breaks every `:ack`-then-read test pattern**: stream's `:ack` = processing complete + PState visible; microbatch's `:ack` = depot durable ONLY. Every `append → immediately read` must become `append → await-*/poll` (or `wait-for-microbatch-processed-count`). Strongest settle for adversary probes whose pass condition is negative ("truth must NOT change"): a *fence* — append a throwaway valid record and await its visible effect; everything appended before the fence is then consumed (per-partition depot order + batch snapshot covers cross-depot too, same topology).
- **Drain loops bypass per-record guards**: a guard chain checked per incoming observation (auth → terminal → seq) does NOT protect writes made by a buffer-drain loop. The compute drain needed its own terminal fence (buffered `:started` after buffered `:exit` would reopen a terminal run). Rule: any loop that applies *stored* records must re-check the same invariants the ingest path checks.

## Rama Dataflow Branching + Module-Graph Gotchas (llm fix session 2, 2026-06-11)
- **A second `(%microbatch :> *var)` statement CHAINS, it does not branch**: in a `<<sources` block, every statement attaches to the previous one's emissions. A "second tap" of the batch placed after the main chain only runs when the main chain's last statement emits — records dropped by an upstream `filter>` never reach it (our unknown-run dead-letter branch silently never ran; probes caught it). For an independent branch over the same batch, put `(anchor> <batch-root>)` right after `source>` and start each branch with `(hook> <batch-root>)`.
- **`anchor>`/`hook>` INSIDE `<<if` branches blew up Rama's compiler** (StackOverflowError in `unification/routes-until` at module load). Top-level anchors right after `source>` compile fine. If branches inside conditionals are needed, restructure to top-level hooks + `filter>` instead.
- **Big module graphs overflow the default JVM thread stack at COMPILE time**: Rama's topology analysis is recursive; loading the (grown) llm module and the 1,700-line space module in one JVM threw StackOverflowError in `rpl.rama.asg.core.RawASGRichNav/select_STAR_` during `require`. Fixed with `:jvm-opts ["-Xss16m"]` on the `:test` alias. Any future JVM that loads both modules (server runtime included) needs the same flag.
- **Explode-emptiness starves downstream chain**: `(explode *empty-vec :> *x)` emits nothing, so code after it in the same chain never runs for that record. Guard with `(<<if (non-empty-coll? *v) (explode ...) ...)` — when false the chain continues past the `<<if`; when true, downstream-of-`<<if` runs once per exploded element, so everything after must be an idempotent keyed write.
- **Microbatch removes Rama-retry duplicates, NOT client duplicates**: exactly-once covers replays inside Rama; a client re-appending the same record still double-folds anything unkeyed (`conj` onto `:steers`/`:compactions`). Every fold still needs an id-based first-delivery-wins guard.
- **Cross-task visibility inside one microbatch is unordered**: awaiting a write that happens LATER in the event chain (e.g. `$$llm-control-by-id` after the run-row write) does NOT imply the earlier same-batch write on another task is visible — stream topologies gave that implication (per-hop commit order), microbatch doesn't. Tests must await the row they assert on, not a sibling index.
- **Fn literals and macros do not compile inside dataflow paths/ops**: `(local-transform> [(keypath *k) (term #(core/write-if-absent % *row))] $$p)` fails at module load ("Unable to resolve symbol: fn*"), even though core.clj's `write-if-absent` docstring shows exactly that pattern — it was never exercised in a real topology before session 5. Same for `(filter> (and ...))` ("Unable to resolve symbol: let*") — `and`/`or` are macros, not ops. Fix: select → pure fold fn in operation position → `(<<if (not (identical? folded existing)) (local-transform> ... (termval folded)))`, all on one task between partitioners (atomic anyway). Use `<<if`, NOT `filter>`, when downstream hops must still run for no-op rows (transcript obs event, session 5).

## clj-kondo Cannot Model Rama Macros — Baseline "Errors" Are False Positives (relation-kernel Phase 3, 2026-07-03)
- **Every Rama kernel lints with phantom clj-kondo `error: Unresolved symbol`** — the linter does not understand Rama's dataflow/module macros. Confirmed baseline on committed, hardened code: `object_container.clj` = 4 errors (dataflow `*logvars`) + 26 warnings; `compute.clj` = 1 error (`current-task-id` builtin) + 4 warnings. So a fresh Rama module showing a handful of "unresolved symbol" errors is EXPECTED, not a defect.
- **The specific pattern for query topologies**: `$$pstate` symbols are declared via `declare-pstate` inside the `(let [mb (microbatch-topology ...)] ...)`, but the `<<query-topology` forms sit at the `defmodule` body level *outside* that `let`. PState symbols are module-scoped (legal — query topos can read any PState), but clj-kondo only sees the lexical `let`, so it reports each `$$…` ref in a query topology as unresolved. `relation_kernel.clj` = 4 such errors, one per PState referenced across its two query topologies, and **0 warnings**.
- **Authoritative check = Rama's own compiler, not clj-kondo**: `clojure -M -e "(require 'app.server.rama.<mod>)"` macroexpands `defmodule` and validates partition alignment, cross-branch logvar unification, batch-block `|origin` structure — everything clj-kondo can't. Treat `:COMPILE-OK` + zero clj-kondo *warnings* (unused vars, arity, deprecations) as the real Phase-3 lint gate; the `$$`/logvar/builtin "errors" are the known false-positive class. Do NOT edit shared `.clj-kondo/config.edn` to suppress them — the codebase tolerates them and a config change is out of scope.
- clj-kondo is not on PATH; run it isolated: `clojure -Sdeps '{:deps {clj-kondo/clj-kondo {:mvn/version "2024.11.14"}}}' -M -m clj-kondo.main --lint <file>` (or via the bundled copy in `clojure-lsp`).

## NUL bytes in source: git-binary trap + the tool-JSON trap (relation-kernel, 2026-07-03)
- **Never put raw NUL bytes (`\x00`) in a source literal — spell them as the `\\u0000` string escape** (Clojure strings support Java unicode escapes; runtime value is byte-identical, provable via `(= sep (str (char 0)))`). `relation_kernel.clj` originally held two raw NULs (`id-part-separator`, the `missing-idempotency-key` sentinel prefix — the collision-proof "byte that can't appear in user data" idiom). Consequences of the raw bytes, all hit in one day: `file(1)` says "data"; plain `grep` silently returns nothing (NUL = binary marker, reads as "no matches"); **git classified the file as binary at commit (`Bin 0 -> 43218 bytes`), killing every future diff**. Re-spelled as escapes at commit time; suite re-run green before amending. The separator VALUE is still NUL and must never change (that rewrites every deterministic relation-id).
- **Claude tool-call JSON trap (hit 3× in one session)**: writing `\u0000` inside Write/Edit content is JSON-decoded into a RAW NUL byte in the file — the fix reintroduces the disease. To write the literal 6-char text, double-escape in the tool call, or safer: write a placeholder and fix with `perl -pi -e 's/\x00/\\u0000/g' <file>`. Always `file(1)`-check any file where you mentioned `\u0000` in an edit. (Hit 2 more times on 2026-07-03 in the retro-recheck session, both while writing DOCS about the escape -- markdown is as vulnerable as source.)
- **`grep -c $'\x00' file` is meaningless**: the shell passes the NUL, C truncates the pattern to empty string, which matches every line — it returns the file's line count, not a NUL count. Use `file(1)` or `cat -A | grep '\^@'` instead.

## Microbatch test barrier: cumulative processed-count, not polling (relation-kernel, 2026-07-03)
- **To prove NEGATIVE invariants ("the replay wrote nothing", "no duplicate copy") a poll barrier is useless** — polling proves something appeared, never that a no-op ran. The pattern that works (reuse for every future kernel test suite): one harness closure per launch with `submit!` = `foreign-append!` + counter increment, `drain!` = `wait-for-microbatch-processed-count ipc module topo @counter 30000`. The count tracks depot records CONSUMED (replays and rejections included), so submit!==+1 always holds — provided every request passes the topology's ingress filter (all routing keys present; client-side throw BEFORE append keeps the counter honest).
- Semantics verified against the references: count is cumulative per launch; the 5-arity throws on timeout (stuck record fails loudly); a microbatch is one cross-partition transaction, so at drain-return every endpoint-copy write from those records' batches is visible. `pause!`/`resume!` around two appends deterministically forces them into ONE batch (next batch contains everything appended while paused) — the only way to prove intra-batch read-your-writes.
- **Isolate any pause!/resume!-wielding test in its own deftest/launch**: a failure between pause and resume starves every subsequent drain! in that launch (cascading 30s timeouts that mask the real failure).
- **Physical (V1) reads vs public queries in assertions**: public read surfaces mask their own bug classes (R1 dedups by relation-id → hides duplicate copies; R2 returns empty history for a missing row → vacuous "no stray write" checks). Copy-count / provenance / leak assertions must read the PState directly; product-surface assertions go through the queries.

## Anchor offsets are NOT one unit across adapters (trail-view contract recon, 2026-07-04)
- **Markdown anchors are CHARACTER offsets; transcript anchors are BYTE offsets.** `markdown_adapter.clj` builds spans with `(count …)` on strings (doc anchor `0…(count raw-text)`, block spans from char positions), while `transcript_adapter.clj` anchors carry the JSONL line's `byte-offset`/`byte-length` into the on-disk file. Same `SourceAnchorRow` record, two incompatible unit semantics — any multi-byte character (emoji, curly quotes) makes a byte-assuming resolver misread md evidence spans. Every anchor consumer must branch on source-format (`:offset-unit :chars|:bytes` — trail-view CONTRACT §4 encodes this; gate 5 tests one of each).
- Related: `SourceArtifactRow.content-byte-count` is UTF-8 bytes even for md whose anchors are chars — don't reconcile counts across the two.

## Mirror PStates route ONLY via the mirror partitioner (trail-view plan spike, 2026-07-04)
- **`local-select>` on a mirror PState works only after the MIRROR partitioner `(|hash$$ $$mirror *k)`** — a plain `|hash` partitions by the current module's task count and silently mis-routes (reads return nil for data that exists; no error). Proven live in a two-module create-ipc spike, not from docs.
- One `|hash$$` sets the mirror-partition index for subsequent sibling PStates of the SAME source module partitioned the same way — so a whole object-container key-family (containers/revisions/anchors/projections) reads in one partition hop.
- For mirrored PStates with a custom `:key-partitioner` (OC's `partition-by-object-key`), pass the EXTRACTED object-key to the partitioner call, not the full prefixed id.
- Query topology → mirror QUERY via `invoke-query` works (cross-module query composition is real); module-name format for mirror declarations is `<ns>/<module-var>`.

## Wall-clock stamps inside topologies break replay determinism (2026-07-04)
- **`object_container.clj` decision rows stamp `decided-at-ms = (core/now-ms)` in-topology (lines 428/448/467)** — divergent from core.clj's own stated discipline (decisions copy the request's clock so replays re-derive byte-identical rows). OC survives it only because a prior-decision short-circuit means replays never rewrite the row. Do NOT copy the pattern into anything without such a guard: a microbatch crash-replay re-stamps a different time (worst case: a time-bucketed key lands the same event in TWO buckets). Rule: topologies never read the clock; time comes from the record (client-stamped envelope/payload fields). The trail-view activity projection is specified this way (CONTRACT §5.3 / trap 4b).

## Mirror local-select> REJECTS :allow-yield? (Rama 1.6.0; WP1 Phase B, 2026-07-05)
- **`local-select>` on a MIRROR PState raises ":allow-yield? cannot be specified for mirror selects" at runtime.** Any style rule mandating allow-yield on unbounded range reads cannot apply to mirror reads — trail_view.clj reads family collections via `(subselect MAP-VALS)` with no yield; bounded only by corpus scale (promotion criteria named in the contract). Found live in WP1 Phase B; the contract's gate text assumed otherwise.

## msdf-atlas-gen multi-font + font coverage gaps (view-mvp P4, 2026-07-05)
- **Ubuntu Sans Mono (the varfont) has NO arrows (U+2190-21FF), NO geometric shapes (U+25A0-25FF), no star/warning glyphs** — 340 of the audit's needed codepoints simply aren't in the font. Box drawing, curly quotes, dashes, U+2713 and U+FFFD ARE present. DejaVu Sans Mono fills all the gaps except U+2913 and dot leaders.
- **`msdf-atlas-gen -font A -charset a.txt -and -font B -charset b.txt` merges two fonts into one atlas, BUT the JSON output switches to a `{"atlas":…,"variants":[{metrics,glyphs,kerning}…]}` schema** — the renderer and any `glyphs[]` reader expect the flat single-font shape. Flatten: take variant-0 metrics, concat glyph arrays (dedupe by unicode), force fallback-font advances to the primary's (0.56) to keep the monospace grid; box glyphs drawn slightly wider than the cell still connect.
- The repo's `:prod` shadow build is pre-broken (prod.cljc requires Electric v2 `hyperfiddle.electric`); compile-check the `:dev` build in-process via shadow api instead.

## perl one-liners on UTF-8 files MUST use -CSD -Mutf8 (NUL-trap family, 2026-07-05)
- A byte-wise `perl -i -pe 's/\x{00B7}/…/'` matches the TRAILING byte of a multi-byte char and corrupts the file (leaves stray lead bytes; file(1) flips to "Non-ISO extended-ASCII"). Always `perl -CSD -Mutf8 -i -pe`. The standing file(1)-must-say-text gate catches it — keep running it after every mechanical re-spelling.

## Background-agent ops, two lessons (Trunk-4 marathon, 2026-07-06)
- **Killing a parent background agent does NOT cascade to its child subagents.** t4-bench was TaskStop'd; its three grader children kept running and spawning batches until stopped individually from the agent panel. On "stop everything": kill the parent, then sweep the panel for children, then check OS processes (llama-server survived too) and disk mtimes for quiescence.
- **Zero-work agent glitch, now 5 firings total (3 in one wave):** a FRESH background agent may return boilerplate with 0 tool calls. Fix is proven: resume it (SendMessage) with an explicit "your FIRST action right now: <specific Read>" line. Prevention: put an explicit first action in every background-agent prompt from the start.

## Rama probe tests flake on await timing (known-flake registry)
Three namespaces have produced intermittent failures that are
contention/timing artifacts, not code bugs — all proven by standalone
rerun green: `dogfood-space` (under live dev-server load, known since
wave-1); `dogfood-llm-probe` `llm-adversary-probe-matrix-test`
(2026-07-06, Trunk-5: failed once in-suite with a LeaderNotFoundException
and once standalone, then 67/67 green; probe-await race); and
`dogfood-llm-test` `stale-approval-on-executor-death-test` (2026-07-24, P1
gate: 6 failures in one full-suite run, then standalone green 12t/114a
TWICE independently AND in-suite green on the gate's full rerun —
LeaderNotFoundException contention visible in the log while the ns still
passed). Protocol: a single failure in these nss → rerun the ns standalone
BEFORE diagnosing; two consecutive standalone failures = real.

## OC and RK validate actor-type ASYMMETRICALLY (caught 2026-07-09, block-kernel P1+P3)
- **object-container import** REJECTS an actor whose `:actor/type` is not in
  `#{:human :agent :system :bot}` (core.clj `authorized-request?`). `:machine` is
  NOT in that set → a `:machine` importer is rejected. (block-kernel P1 hit this;
  the driver's OC importer uses `:actor/type :system`.)
- **relation-kernel** does NOT validate `:asserter-type` at all —
  `request-shape-errors` (relation_kernel.clj:~309-335) checks only `:actor/id`
  present, kind registered, and target well-formedness. Any keyword asserter-type
  is accepted: block-kernel edges assert `:machine`, code-atom asserts `:import`,
  both green.
- **Gotcha:** do NOT assume the two kernels share an actor-type allowlist. A
  `:machine` (or any non-`#{:human :agent :system :bot}`) actor works as an RK
  asserter but is rejected as an OC importer. Verify-first when wiring an actor
  into either kernel; the OC set is the constraint, RK is open.

## Git history holds committed-BROKEN blobs — a whole-history parser isolates + counts, never aborts (code-atom G-F1, 2026-07-09)
- Git history is NOT all-parseable: some blobs were committed BROKEN (unbalanced
  parens, reader-hostile tokens). In THIS repo, 5 of 895 `.clj/.cljc/.cljs` blobs
  (4 old `electric_flow.cljc` states, 1 old `ttf.clj`) fail rewrite-clj.
- **Gotcha:** an unguarded `parse-string-all` in a whole-history sync throws on the
  FIRST broken blob and kills the entire run. A full-corpus processor MUST isolate
  each blob: wrap the parse step, degrade a failure to a 0-unit cut carrying an error
  marker (`:parse-error`), STILL store the raw surface (atoms honestly absent), and
  COUNT it (`:blobs-unparseable`) — never abort, never a silent empty. Same shape as
  the git-exit isolation (`:git/exit` → `:git-failures`): per-item try/catch, count,
  continue. The unparseable set is DISJOINT from the git-failure set (a parse-failed
  blob still ingests; a git-failed blob never enters ingest).
- **Meta-gotcha:** a "N/N blobs" claim from a spike names its VERB — ENUMERATE ≠ PARSE
  ≠ INGEST. "895/895 enumerated" says nothing about parse-fidelity; the 5 broken blobs
  hid in that gap, invisible to every pinned-specimen/HEAD-files test. Gate a full-corpus
  RECEIPT (not just pinned-specimen tests) for any whole-history processor.

## Green-pre-commit / red-post-commit: a HEAD-reading test goes stale when a commit moves HEAD (code-atom close, 2026-07-09)
- A test that resolves the analyzed blob via `resolve-head-sha` (git HEAD) but pins its
  ground truth (row numbers, kondo stats, census) from a FIXED specimen blob is coupled
  to "HEAD == specimen." That holds until ANY commit re-touches the file — including the
  package's OWN code commit. `analyzer-gates` was green against the working tree
  (uncommitted → HEAD still the old blob) then RED at committed HEAD once the registry
  commit moved HEAD's `relation_kernel.clj` blob off the ingested specimen (nil source →
  NPE). Deterministic, not a flake.
- **Gotcha:** (a) re-run every HEAD-reading suite AFTER the commits that move HEAD, not
  only before — a working-tree green is not a committed-HEAD green; (b) a test that pins
  ground truth to a blob analyzes THAT blob explicitly (`:head-override <specimen
  commit>`), like the lineage gates pin fixed commits — never the moving HEAD (T3
  "analyze the blob, not the checkout"). Caught by the close-session adversarial recheck.

## OC import: a class marker / durable row with NO material (block-kernel F3, 2026-07-10)
- `import-request-validation-errors` (object_container.clj) rejects an empty-source payload
  (`:source-artifacts/missing`) by default — so you CANNOT write a projection-hint-only import
  (a durable class/marker row that has no surface, unit, or anchor).
- **Fix pattern (additive, revert-cheap):** relax the guard to
  `(and (empty? source-rows) (empty? projection-hint-rows))` — reject only a TRULY empty
  payload. The import topology's projection-hint loop already runs independent of the material
  loops, so a hint-only payload writes cleanly once validation admits it. Purely additive: any
  import WITH sources is unaffected (same class as code-atom's `imp:clj:` branch — a kernel
  edit, so a stop-clause → Sid). Used to give every ingested event a durable versioned class
  row (SPEC §3.1) even when it produces no surface (debris + surfaceless-river events).

## OC anchors: by-source stores refs, by-target stores spans (block-kernel F4, 2026-07-10)
- `$$source-anchors-by-source` stores span-LESS `SourceMaterialRefRow`s (target-id = the
  anchor-id; order-key = anchor-id) — NOT full anchor rows. `read-common-material-for-source`
  `:anchors` therefore gives you the SET of anchors on a surface but no offsets.
- To resolve `(surface, span) → unit-id` (e.g. refine! identity resolution, SPEC §6.1):
  enumerate the surface's `:derived-units` refs (target-id = unit-id, distiller-filterable via
  the id substring), then read each candidate's anchor via `$$source-anchors-by-target`
  (`read-source-anchors` by unit-id → FULL rows with start/end). By-source = the set; by-target
  = the spans. Cheap per-surface (one part = few units); not a page hot-path.

## Driving the dev app headless (learned 2026-07-11, framework W1)
- `window.__softland_atoms` (runtime.cljs) exposes the runtime atoms map — read/drive app state from puppeteer via `cljs.core.deref/get/keyword`. The repo's own puppeteer (devDependency) + system Chrome works; `--headless=new --enable-unsafe-webgpu --enable-features=Vulkan` gives a real WebGPU adapter (must navigate to the app origin first — `about:blank` has no `navigator.gpu`).
- Keyboard: dispatch synthetic `KeyboardEvent`s on `window` (the app's one keydown listener lives there); CDP `page.keyboard` also works. **The cmd panel boots VISIBLE** — ctrl+k first CLOSES it; normalize to visible+`:command-panel` focus before typing commands.
- **WebGPU canvas pixels never reach CDP/X screenshots on this box** (headful+xvfb, headless=new, swiftshader — all black; the Vulkan swapchain bypasses compositor readback). For visual evidence, rasterize the live scene atom (`tree->rects`/`tree->text-ops` onto a 2D canvas in-page) and label it CPU raster.

## Parallel sessions share ONE git index (2026-07-12)
Concurrent sessions in the same working tree cross-commit each other's STAGED files: bare `git commit` sweeps the whole index (write-echo's `311fcbb` swept box3d-spike's staged REPORT.md; benign that time, docs-only). Rule for any session in a shared tree: never stage-now-commit-later — commit with EXPLICIT paths (`git commit -m "..." -- <paths>`), never `git add -A`, and expect other sessions' uncommitted files in `git status` (flag, don't clean).

## Headless WebGPU is dead on this box (2026-07-12) — OUTDATED for boot as of Chrome 150
**2026-07-26 update (P8 gate):** system Chrome 150 `--headless=new
--enable-unsafe-webgpu --enable-features=Vulkan --no-sandbox` DOES create a
real WebGPU device now — the app boots fully headless (atoms, portal,
canvas, blocks render; a `[WEBGPU/DEVICE-LOST]` may fire mid-session but
state-side driving keeps working). Puppeteer's BUNDLED Chromium has no
`navigator.gpu` at all and the boot dies at LoadWebGPU — always pass
`executablePath: '/usr/bin/google-chrome'`. GPU PERF receipts still need
Sid's headed browser (SwiftShader-fallback + swapchain-capture caveats
below stand); but functional headless drives (real taps, typing,
Ctrl+Enter, `__portal` calls) are fully viable now.
Original finding (pre-150): headless could not create a device — "Failed
to initialize vulkan surface" (Radeon 7900 XTX).

## await-relation RETURNS on deadline — a barrier you don't read is not a barrier (machine-cut A-F4, 2026-07-13)
- `await-relation` (and helpers of its shape) returns the LAST READ VALUE on
  deadline instead of throwing. Machine-cut's driver awaited every edge write
  and DISCARDED the result — counts and the epoch decision then ran on the
  PLAN, not on materialized state; a shortfall was silent success.
- **Gotcha:** any await/retry/barrier helper that returns-without-throwing
  must have its result ASSERTED at every call site (machine_cut.clj's
  `materialized-to?` pattern: check every transition materialized; shortfall
  → an honest `:incomplete-writes`, never silence). When writing a contract,
  name what the caller must CHECK, not just what the helper does.

## TWO in-process Rama clusters booting concurrently collide in the worker registry (machine-cut first wearing, 2026-07-12)
- Booting a fresh InProcessCluster while another cluster's module launch is
  in flight (face OC boot × fresh rk cluster) produces a chronic
  worker-registry collision — transcript-ops workers fail to launch,
  repeatedly, until process restart. Invisible to every suite (no test boots
  both concurrently); found live at the first machine-cut wearing.
- **Rule: one in-process cluster per land.** New kernels attach to the
  EXISTING cluster (machine-cut reuses the trail cluster's relation kernel;
  deref-first sequences the boots) and export ONE lawful accessor for live
  handles (`machine-cut-ctx`) so REPL/live callers can't mint a second
  cluster nothing serves.

## Chrome WebGPU on Linux silently falls back to SwiftShader (scene-substrate G4, 2026-07-13)
- With `--enable-unsafe-webgpu` but NO Vulkan feature enabled, Dawn creates a
  SwiftShader (CPU raster) device on Linux — everything renders, just ~20×
  slower (flat ~91-106ms full-viewport frames, quiet JS, saturated main
  thread). Invisible unless you ask: normal identical?-skip use feels fine.
- **Check FIRST on any GPU perf anomaly:**
  `(await navigator.gpu.requestAdapter()).info` → `isFallbackAdapter: true` /
  `description: "SwiftShader Device (Subzero)"` = CPU. Fix: chrome://flags
  `#enable-vulkan` → Enabled, relaunch. Any GPU perf gate receipt must carry
  adapter identity as its first field (work-package skill rule, 2026-07-13).
- Related: headless Chrome on this box cannot create a WebGPU device AT ALL
  ("Failed to initialize vulkan surface") — GPU receipts come from Sid's
  headed browser; don't burn time on headless.

## Dev-app server half goes STALE across commits (P8 gate, 2026-07-26)
The dev app's shadow WATCH recompiles the CLIENT on every commit, but the
SERVER half is frozen at the JVM's boot — a session driving "the live app"
after new commits gets new client + OLD server silently (P8 gate: the
:episode-durable event lacked the new keys; root cause was boot-time 46 min
before the commits). **Check FIRST when driving the dev app after any
commit:** `ps -o lstart -p <dev pid>` vs the gate object's commit time.
Fix without touching the shadow cache (Electric token!): boot a SECOND
bare jetty at HEAD on another port — requires only
`(jetty/start-server! (fn [req] (e/boot-server {} app.electric-flow/main req)) {:port 8091 :resources-path "public/" :manifest-path "public/js/manifest.edn"})`
— it serves the watch's already-compiled client with HEAD server code.
Complement since `ecbd572` (rung 3): `LAND_PINNED=1 clj -M:dev -m dev`
boots the dev app with NO shadow watch, serving the already-compiled
client — a fully pinned boot (HEAD server + frozen client) that concurrent
sessions editing the tree cannot hot-swap. Compile the client first
(`clj -M:dev -m shadow.cljs.devtools.cli compile dev`).

## Resident CLI refuses non-UUID session ids (P8 gate, 2026-07-26)
`decide-episode` uses the LANE-ID as the claude CLI `--session-id` on a
virgin lane; the CLI hard-requires a UUID. Genesis conversation-id and
thread ids are UUIDs by design, so normal lanes work — but a thread-less
`?drill=<non-uuid-string>` lane can never spawn its first resident
("Invalid session ID", after the durable turn lands). Use UUID-shaped
drill ids whenever a drill resident is wanted.

## Verifying a cljs hot-fix compiled when the dev watch owns the terminal
- The `clj -A:dev -X dev/-main` watch prints to its own pts; `.shadow-cljs/`
  logs may belong to a STALE standalone server (check version/mtime).
  Deterministic receipt instead: the watch only writes build output on
  success — check `resources/public/js/cljs-runtime/<ns>.js` mtime is newer
  than the edit AND grep the compiled file for a new fn name.

## Facet-master activation history is causally ordered, NOT clock ordered (editable-material P4, 2026-07-25)
- Deploy-time migrations mint deterministic `time-ms` values (P1 v0 = 0, P3
  v1 = 1) while live activations carry wall-clock ms — so the activation
  pointer history is NON-MONOTONE in timestamps (a later activation can
  carry `time-ms 1`). Any as-of / history reconstruction that sorts by
  timestamp silently builds the WRONG world.
- Rule: reconstruct activation history by the pointer revisions' causal
  parent chain, never by time sorting; expose clock regressions instead of
  hiding them (P4's `:ambiguous-activation-history` pattern). P6's
  activation events must carry honest request times.

## Real Rama cluster ops — the shutdown state machine bites (durable-ground P5b/P6, 2026-07-17)
- **An INTERRUPTED `rama shutdownCluster` persists its intent in ZK; the next
  conductor boot RESUMES it** — killing every worker into terminal
  `CLUSTER-SHUTDOWN-COMPLETE` ("Awaiting exit"). Looks like a mystery
  mass-worker-death after a restart. Recovery: one more plain
  kill-and-restart — a COMPLETED shutdown clears on the next fresh boot.
- **A COMPLETED shutdownCluster makes every daemon EXIT ITSELF** (docs say
  "stops workers but not Conductor/Supervisor" — observed otherwise on the
  single-node dev cluster), so the CLI's own completion-poll dies with a
  thrift connect error → nonzero exit. Never gate an ops script on
  shutdownCluster's exit code; wait on the conductor-log terminal marker or
  all-daemons-gone (bin/land does both).
- **`set -e` + wait-loops**: `grep -q X && break` on a no-match iteration
  fails the whole statement and aborts the script silently — use
  `if grep -q X; then break; fi` in any `set -e` ops script.
- `rama destroy` takes the module name POSITIONALLY (no --module) and asks
  for stdin confirmation (pipe the name in). `rama deploy` takes --module.
- **pkill self-match**: any pkill/pgrep pattern used from a tool-invoked
  shell must bracket a char (`'rpl.rama.distribute[d]'`) — the invoking
  shell's own cmdline contains the pattern (bit THRICE across three
  sessions; 2026-07-24 firing killed the invoking shell mid-recovery,
  exit 144).
- **The half-cancelled shutdown wedge + the znode fix (proven 2026-07-24).**
  Running `forceClusterOpen` against a DRAINING cluster clears the T11
  resume-intent but NOT `/rama/version/1.6/conductor/state` (nippy
  `[:cluster-draining]`, 24 bytes). Result: every boot puts workers into
  LEADER-FALLING (`:shutdown? false`, never rises), conductorReady=false,
  and `shutdownCluster`/`forceClusterOpen` become SILENT no-ops — no
  supported exit exists (operating-rama docs checked: nothing documented;
  drain-stuck recovery is absent). Recovery that worked, fully
  rollback-protected: stop daemons → `cp -a` the whole data dir →
  devZookeeper alone → `./rama repl`: verify nippy round-trip is
  byte-identical (`REFREEZE-EQUAL true`), then setData the state znode to
  `(nippy/freeze [:cluster-shutdown-complete])` → stop ZK → normal boot
  (identical to the proven post-backup boot): ready=true, LEADER-OPEN.
  Also: drain time GROWS with cluster age (07-17: ~60s; 07-24: >120s) —
  bin/land waits 900s and fails closed since `cd4fcb4`; never gate on a
  bounded wait falling through.
- **Failure-drill receipts must be SERVER-READ**: a client-side snapshot
  captured after a kill drill can be the frozen state of a session the drill
  itself killed (P5a: the "restored" snapshot was a dead Electric session's
  cache; the cluster read told the truth).
- Foreign clients ride a ~30s pong timeout before abandoning a killed
  worker's channel; an in-flight foreign call against a dying worker throws
  `CallbackException :connection-closed` — if that lands inside an Electric
  server branch it kills the websocket session (reload heals; availability
  residue routed to first-light).

## Launching a remote-drivable Claude session FROM another session (2026-07-25)
- The three flags are real and compose: `claude --model fable --effort xhigh
  --remote-control <name>`. `--effort` takes low|medium|high|xhigh|max;
  `--model` takes an alias (`fable`/`opus`/`sonnet`) or a full id.
- **This box has NO tmux and NO screen** — the usual "detach it in a
  multiplexer" reflex fails. `DISPLAY=:0` is live and `kitty` is installed, so
  the working recipe is a real terminal window:
  `setsid kitty --title <name> -e claude --model fable --effort xhigh --remote-control <name> &`
  Better than a PTY-less background process (Ink needs a TTY) AND better than
  `script -qc` (no window to return to): Sid gets a session he can drive from
  his phone now and sit down at later.
- kitty prints `[glfw error 65544] ... org.freedesktop.portal.Settings` and
  `[PARSE ERROR] CSI code m has 2 > 1 parameters` on this box — cosmetic, the
  session is fine. Verify with `pgrep -af` + `ps -o stat` (want `...+`, a
  foreground process group with a terminal), not by the absence of warnings.
- Pairing state is only visible INSIDE that window, so a launching session can
  confirm the process is healthy but never that remote control connected.
  Say so rather than implying it worked.
- Don't seed a first prompt into a session that will do live/irreversible ops
  unattended. Put the prompt on the board instead and hand over a one-line
  phone message ("Read docs/next-prompt.md and run X").

## Electric compiler auth lives inside the shadow build cache (P7 gate, cost ~1h)

- The Electric v3 SNAPSHOT compiler's auth token is
  `.shadow-cljs/builds/dev/hyperfiddle.electric.token` — INSIDE the build
  cache. Clearing that cache for a cold-compile receipt evicts the token, and
  every subsequent compile parks SILENTLY on a login deref
  (`hyperfiddle.electric.shadow_cljs.hooks3` line ~145; jstack shows main
  parked on a CountDownLatch at 0% CPU — it looks like a hang, it is a
  prompt).
- The bare lane (`clj -M:dev -m shadow.cljs.devtools.cli compile dev`) never
  prints the prompt anywhere visible. Only the dev-app lane
  (`clj -A:dev -X dev/-main`) writes "[Electric compiler] Please login…" with
  the `hyperfiddle-auth.fly.dev/login?redirect-uri=http://localhost:8081` URL
  to its log. Sid clicking it once (any email, on this machine — the compiler
  listens on :8081 for the callback) unparks the compile instantly.
- So the working cold-compile recipe is: `mv` the cache dir aside (the
  sandbox refuses `rm -rf` there; `mv` to scratchpad is equivalent and
  reversible) → boot the dev app → its watch build IS the cold compile →
  read "Build completed … 0 warnings" from its log. Expect ~311 files via
  the watch lane vs ~270 via bare compile (devtools namespaces).
- `npx shadow-cljs` standalone is the WRONG lane entirely: shadow-cljs.edn
  has no :source-paths/:deps for it, so it can't find the `dev` entry ns —
  and it leaves a lingering server JVM that wedges later CLI compiles.

## OC bootstrap vs normal import: same source, same import key, DIFFERENT fingerprint (space-as-entity rung 3, 2026-07-26)
- A BOOTSTRAP import (the first-ever, which carries its active pointer) and
  a later NORMAL import of the SAME source under the SAME import key compute
  DIFFERENT material fingerprints — the normal re-import is REJECTED with
  `:idempotency/material-fingerprint-conflict` instead of replaying. The
  content-addressed revision id stays byte-identical and a fresh activation
  still lands, so the effect is a scary-looking rejection on a semantically
  idempotent re-import — NOT data loss or a new-package bug.
- Pre-existing Object Container adapter asymmetry (read-only code; surfaced
  at rung-3 G3, judgment CONFIRMED at gate). Before diagnosing any package
  code on this rejection: check `:reason` + revision-id equality first.
- Falsifier staged for whenever an OC package next opens (rung-3 RETRO
  residue): one OC test asserting a normal re-import of a
  bootstrap-imported source REPLAYS instead of conflicting.
