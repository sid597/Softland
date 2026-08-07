# T2 INPUT-FLOOR — one-pass contract (Unicode-safe editing · IME composition · paste semantics · source-index mapping · the caret/selection/clip bounds ruling)

Cut 2026-08-07 by Fable — **cutter model `claude-fable-5`, effort `max`** (the
model-routing law, CLAUDE.md; Sid's two-second header check). One-pass law:
`.claude/skills/work-package/SKILL.md` + decisions.md "How we work". Two
Sid-touches: this cut · the accept. The cut closes through ONE bounded
fresh-eyes falsification round — ONE Codex-class arm at Sid's hand (the
standing round shape since W4; findings evidence-cited, claim→source, no
verdict authority, no recut; author repairs in-session; expiry fires if the
round returns ≤1 decision-changing finding). THE ROUND RAN 2026-08-07:
ten findings; the cutter (Fable, max) adjudicated eight decision-changing
and repaired them IN THIS TEXT — every repair carries its finding number
inline; the expiry does NOT fire, the next contract still owes its round. The four-lens author pass ran
BEFORE the round (lens 1/4 Opus sweep of the finished draft; lens 2/3 author
judgment). Structural precedents AT THE SOURCE LEVEL: the chrome atom
(`86918e9` — session truth, flag lane, nil-default seams in shared files),
W4 frame-runtime (`143497a` — scheduler deadlines, clip roads, fixture
shapes, verifier append discipline), and the connector atom's `layout-label`
(the one precedent for NEW text entering the world through the layout seam).
Do not imitate IMAGE-ATOM/SEAM-STEP1 docs; they predate the law.

## Scope — what this atom is

Package 2's final atom (ENGINE.md §0, the T2 row): **"input floor
completion: Unicode-safe editing · IME composition · paste semantics ·
source-index mapping."** The full-breadth law governs: those four
obligations are the FLOOR, not a menu — "minimal" may describe this atom's
seams (a flag-lane editing session over fixtures), never the obligations.
This atom also OWNS the ruling W4 carried forward: the correct relationship
among object-selection bounds, text-editing bounds, caret/range geometry,
clip bounds, and visible pixels (the "slightly offset blue box",
`W4-FRAME-RUNTIME-NOW.md` close entry).

Everything visible rides `?live-atoms=1`; flag absent → byte-identical
product. NO durable/Rama writes: the editing session is client-session
truth (the chrome selection precedent — reload → gone). The durable custody
act (the PRODUCT's editors riding this floor: ground blocks, the file
editor, cmd panel) is a staged activation at Sid's line, after the
Package-2 seam courtroom — the two-dials law: this atom builds ahead on
reversibility; contact waits for Sid.

**The editing session (the model).** One session at a time, held by the
editing runtime as session truth: target identity `{:vi :address}` (what
`ss/pick` returns), the session DOCUMENT (text + revision counter), caret
(a tagged offset PLUS `:affinity` — W1 §7.2's caret row makes affinity
part of caret truth; one source offset holds two distinct visual stops at
bidi and wrap boundaries and the session stores WHICH — round finding 3)
+ selection anchor (a tagged offset only; anchors carry no affinity),
vertical desired-x memory, composition state (preedit string +
caret-in-preedit + the preedit's source range), blink visibility, and the
segmentation provider handles. Motion decides affinity transitions in the
kernel (End on a wrapped visual line yields `:upstream`, Home
`:downstream`; clicks take the hit-test stop's affinity). The session document feeds the ONE layout
seam — `text-layout/layout`, the same entry every reader already consumes
(W1 Contract T; `text_layout.cljc:966`) — at origin `[0 0]`, zoom 1, the
ground cache-edge discipline (`ground.cljs:354` precedent; zoom is camera,
never a layout input — shaping-correction T7). Every applied edit re-lays
the edited material once; construction stays proportional (shaping I4).

**The editable population v1 = this atom's OWN live-atoms text fixtures**
(two, pinned in Entry points: a proportional multi-line paragraph and a
GPU-clipped card). Real product blocks and every legacy surface REFUSE the
entry gesture v1 (a dblclick on them is a no-op through this lane and
whatever it already was through legacy lanes — nothing changes unflagged or
on non-fixture targets). The product flip is the custody act above.

### The bounds ruling (the five truths, decided — one Sid line reverses any)

1. **Object-selection bounds (chrome outline) = full semantic bounds,
   permanently — ruled CORRECT, not carried as debt.** Chrome's outline
   anchors the target row's max-area addressed rect verbatim
   (`chrome_derive.cljc:96-102`, `scene_store.cljc:526-555`), clip-blind
   (`:clip :none`, `chrome_gpu.cljs:302`; executor scissor reset,
   `renderer.cljs:3424-3428`). This matches Figma's own semantics: a
   clipped child's selection outline shows the child's FULL extent. W4's
   accepted "slightly offset blue box" is correct object-selection
   behavior; the baton's "carry exact visible-bound chrome to T2 if
   desired" is DISCHARGED BY RULING here, not by code. The only repayment
   that remains routed is the pre-existing together-law road (clip-aware
   pick + marquee/snap population, one road, LATER — untouched by this
   atom).
2. **Text-editing bounds = the layout result's logical bounds in material
   space** (measure truth: `:metrics`/line `:logical-bounds`), distinct
   from the node's padded semantic bounds (`ground.cljs:1661-1663` shows
   the pad). v1 paints NO edit-mode outline (refusal, routed) — the ruling
   defines the vocabulary; the caret/selection themselves mark the mode.
3. **Caret / range / preedit geometry = layout-result truth, rendered as
   WORLD-stratum rect nodes INSIDE the edited node's subtree** — the
   legacy precedent made law (`face_primitives.cljc:741-793`: ground's
   caret and selection wash are ordinary rect nodes in the block's tree,
   and therefore already clip/transform/effect-participating). Editing
   visuals inherit the edited node's clip (CPU road or W4 GPU scissor
   road), its transforms, and its effects BY CONSTRUCTION — zero new clip
   consumers, zero chrome edits, zero pick interference (subtree nodes
   inherit the addressed ancestor's address — `stamp-block-addresses`,
   `scene_store.cljc:415-446` — so picking a highlight picks the fixture).
4. **Clip bounds stay where W4 put them.** The composed clip rides the
   tree walks (`intersect-clip`, `rect_tree.cljc:186-202`; `:gpu/clip`
   stamping on the GPU road) and the store-frame lanes
   (`:rect-clips-by-vi`/`:text-clips-by-vi`, `scene_store.cljc:335-343`).
   T2 adds NO new consumer of any of them — truth 3's subtree placement
   does all the work.
5. **Pick and visible pixels: ZERO pick changes** (W4's law, upheld
   verbatim — `ss/pick` `scene_store.cljc:386-409`, `rect_tree` hit-test
   `:613-637`, marquee, snap all untouched). Caret placement is the
   two-subroute law W1 §7.2 already states: the ONE pick road resolves the
   OBJECT (`scene-runtime/pick-world`, `scene_runtime.cljs:319-325`); the
   editing runtime then inverse-transforms the point into material space
   and calls `tl/hit-test-result` (`text_layout.cljc:1418`) — a
   Contract-T reader, not a pick road. A click in a clipped-away region
   resolves exactly as today (clip-blind, ancestor-bounds-gated) — no new
   clip semantics anywhere in pick.

**One declared deviation (named, receipted, repaid).** Because editing
visuals are `:world`-stratum subtree nodes, a raster export
(`export-viewport!`, `:world`-stratum-only) taken MID-SESSION includes
them — a deviation from the W0-C P2.10 default ("authoring chrome does not
export"). Declared exactly as W4 declared effect-blind pick: honest,
receipted (S3 counts the session-visual nodes present at export), tiny
surface (export chord + live session simultaneously; Escape first avoids
it), and repaid on ONE named road — the editing-chrome expansion (editing
visuals move to a pick-none overlay home when edit-outline/screen-constant
metrics land there). The fork was weighed in writing: the overlay/chrome
placement was rejected v1 because overlay rect slots are pick bodies (slot
entries hard-code `:pick`, `scene_store.cljc:272-274`) and chrome-form
placement costs clip re-derivation plus prior-atom grammar edits — truth
3's subtree road is the only v1 shape with exact clipping, zero pick
changes, and zero prior-atom edits.

### The four obligations, pinned

**1. Source-index mapping (the index model).**
- The index space stays T0's declared adapter boundary: tagged
  `{:index-space {:domain :utf-16-code-unit :version 1} :offset j}`
  (`text_layout.cljc:15-20` — its docstring names T2 as the one who may
  replace the source map; T2 RETAINS utf-16 and adds SAFETY. A domain
  change is a routed LATER road via a source-map version bump through the
  same seam).
- Every kernel operation takes and returns TAGGED offsets (W1's
  no-untagged-offsets law). Raw integers do not cross the kernel boundary.
- **The surrogate law:** no operation ever produces an offset inside a
  surrogate pair — a kernel invariant, property-asserted in S1.
- **Editing units:** caret motion and backspace/forward-delete operate on
  GRAPHEME boundaries; word motion (Ctrl+Arrow) on WORD boundaries — both
  from an injected segmentation provider (below). Vertical motion moves
  over VISUAL lines (the layout result's `:lines`) with desired-x memory
  (the remembered value is the caret's material-space x, not a column).
  Home/End go to the visual line's painted ends. The consumed-whitespace
  reader law (shaping-correction §5.4) is upheld verbatim: every source
  offset keeps a caret stop; consumed offsets display at the preceding
  line's painted end; selection contributes zero width across consumed
  ranges; copy INCLUDES consumed characters.
- **Visual caret position** maps through the layout result's cluster caret
  stops (`text_layout.cljc:824-829` — two stops per cluster with
  affinity). When a grapheme boundary falls INSIDE a shaped cluster
  (ligature: one cluster, two graphemes), the caret x interpolates within
  the cluster by advance fraction. THE ROAD IS PINNED (round finding 2):
  today's shaped readers SNAP to declared stops (`shaped-caret-result`'s
  exact→nearest chain, `text_layout.cljc:1190-1204`;
  `shaped-hit-test-result` likewise, `:1393-1402`), so the interpolation
  lands INSIDE the seam's readers — `tl/caret-result` and
  `tl/hit-test-result` gain an ADDITIVE injected-boundaries option
  (grapheme boundaries as DATA) and return interior stops in the same
  `{:index :position :affinity}` vocabulary, derived from declared stops
  + glyph advances. The session NEVER computes interior x itself — a
  session-side interpolation helper is an independent metric route,
  exactly what the T0-3 fence bans (W1 §7.3). The exact fraction formula
  stays the implementer's recorded default. WRONG builds named: motion by
  cluster (caret cannot enter "fi"), deletion by cluster (backspace eats
  both ligature letters), and a test-only interpolation helper that
  leaves the live `caret-result`/`hit-test-result` road snapping — all
  three FAIL S1/S2.
- **Insertion admits any Unicode string** — the `(= 1 (count key))` BMP
  gate (`events.cljs:158-162`) does not exist in this lane. No
  normalization transform v1 (inserted text lands verbatim; NFC policy is
  a routed refusal).
- **Segmentation providers:** the kernel is segmentation-agnostic —
  grapheme/word boundary functions are DATA inputs. Browser provider =
  `Intl.Segmenter` (granularity `grapheme` and `word`; a platform
  built-in — zero new dependencies), identity pinned in receipts. JVM
  tests inject explicit fixture boundaries (deterministic, no host
  variance). Header-domain offsets (`[:header h j]`) are read-only to this
  atom — header editing is a routed refusal (the fixtures carry no
  headers).

**2. Unicode-safe editing (the kernel).**
A pure `.cljc` kernel: session-state value in, `(state', ops)` out. Edit
operations are DATA — `{:op :insert/:delete-range/:replace-range}` over
tagged offsets (already the shape undo and the wire will want —
decisions.md minted-diffs; undo itself stays routed). Dispatch is a pure
decision function: (parsed key event × session state × boundaries) → kernel
op | `:pass-through`. Enumerated pass-throughs are pinned in S1 (browser
zoom chords, F-keys, the OS shortcuts the session must not eat). Selection
is caret+anchor; typing/paste/IME-commit over a non-empty selection
replaces it (one op). Shift+arrows extend; plain arrows collapse to the
motion side. Dblclick inside the session selects the word at the point.

**3. IME composition (the lifecycle).**
- **State machine** (pure, in the kernel): `:idle` →
  (`compositionstart`) → `:composing` → n × (`compositionupdate`/input:
  preedit string + caret-in-preedit) → `:idle` via COMMIT
  (`compositionend` with data → exactly ONE semantic insert of the
  composed string) or CANCEL (`compositionend` empty → view restored,
  zero ops). A session exit or target loss during composition CANCELS
  first — committed session text never contains preedit bytes.
- **Keydown during composition** (`isComposing` / keyCode 229) never
  reaches kernel dispatch. Dead keys ride the same composition machinery
  (browsers emit composition events for dead-key sequences); a platform
  that emits none makes the dead key a declared no-op, never a silent
  splice.
- **Preedit rendering rides THE seam:** the edited node's layout input
  during composition is committed-text ⊕ preedit spliced at the caret —
  ONE `tl/layout` call over the session view-document (W1 §7.1: T2 "does
  not create a second layout seam"). The preedit range is session
  knowledge; its underline derives from `tl/selection-result` over that
  range (thin world rects at baseline + offset — recorded default for
  thickness/offset). The committed document and its revision are untouched
  until commit.
- **The IME host (ruled default):** ONE hidden `<textarea>`, a composition
  SINK only — never document truth: empty outside composition, focused
  while the session is active, repositioned to the caret's screen point
  whenever the caret moves (candidate-window anchoring; world→screen
  through the live camera + container-transform providers, the chrome
  provider-install precedent), value cleared at commit/cancel. EditContext
  is the routed LATER host swap behind the same kernel seam.

**4. Paste semantics (and the rest of the clipboard).**
- Paste (session active): read `text/plain`, normalize EXACTLY — CRLF and
  bare CR → LF; strip C0 controls except `\n` and `\t`; nothing else —
  then ONE semantic insert op (one layout, one future undo unit).
- Over-budget paste refuses BY NAME (receipt), never silent truncation;
  the budget is a recorded default ≥ 100k code units. (The ground's paste
  policy — `paste-decision`, `ground_edit.cljc:79` — FOLDS presentation
  over a threshold while keeping every byte of the durable text; it has
  no clamp constants, is a different lane, is untouched, and is NOT a
  precedent for this refusal: the session budget is a NEW session-lane
  policy — round finding 10.)
- Copy = the selection's SOURCE-range text (consumed whitespace INCLUDED —
  §5.4) via `navigator.clipboard.writeText` (the ground road,
  `ground.cljs:2914`). Cut = copy + delete-range. Empty selection → no-op
  v1. Rich clipboard (`text/html`), files/images → routed refusals.

### Session lifecycle, dispatch precedence, and the two shared hooks

- **Entry:** a flag-only `dblclick` listener owned by the editing runtime
  (its own listener — the W4 export-chord precedent; zero shared-file
  listeners). On dblclick: `pick-world` through the ONE road → if the hit
  is a T2-editable fixture → session opens with the caret at the
  `hit-test-result` stop for the click point; anything else → no-op (the
  event otherwise proceeds untouched).
- **Exit:** Escape (consumed by the session — chrome's Escape/clear runs
  only when NO session is live; precedence pinned: session > chrome lane >
  legacy); pointerdown outside the edited node (capture-phase check: end
  session, then let the event proceed to its normal meaning); flag-lane
  teardown. Exit during composition cancels composition first.
- **While a session is live:** the keydown intercept consumes key events
  WHOLESALE (the legacy `>keyboard` router sees nothing — no
  preventDefault fights with the IME host); UNMODIFIED pointer events
  over the edited node are session input, consumed wholesale — the legacy
  tap/press machinery sees nothing (click = caret move; drag = range
  selection from hit-test stops; dblclick = word-select). SHIFT-modified
  pointer gestures over the edited node PASS THROUGH untouched to the
  legacy road — shift-tap is chrome's selection door wholesale
  (`ground.cljs:3898-3906`; chrome contract: toggle through the one pick
  road), so chrome selection toggles mid-session and S3's coexistence
  golden is reachable by real gestures; shift+click TEXT-range extension
  is therefore a routed refusal (shift+arrows and drag cover extension
  v1 — round finding 4). Pointer events outside =
  exit + normal routing. The camera reservation is honored: no session ⇒
  zero change anywhere, and ground-level naked drag still pans always —
  the session captures pointer meaning only over the edited node's picked
  region, only after an explicit dblclick entry.
- **The ONLY shared-input-file edits are two nil-default hooks** (the
  chrome clear-seam precedent, flag-inert): (1) `events.cljs` — the
  keydown handler head consults an installable intercept predicate; nil
  (unflagged, or flag-on with no session) → byte-identical behavior; (2)
  `runtime/mouse.cljs` — the same shape at the paste listener head, before
  its `case`. Nothing else in the legacy input roads changes. (A third
  nil-default hook — the due-deadline consumer at the frame sink,
  `runtime/render.cljs` — is NOT an input road; see Caret blink + Entry
  points.)
- **Keyboard nudge of selected objects** (chrome's routed item "coarse-
  pointer/touch hit variants + keyboard nudge → input floor T2") is
  RE-ROUTED onward WITH CAUSE: nudge is arrangement manipulation over the
  settle artery, not text input; what T2 owes it — and lands — is the
  dispatch seam it will ride (the intercept predicate + the precedence
  model). The nudge verb itself belongs to the manipulation expansion.
  Coarse-pointer/touch caret variants ride the modality expansion.

### Caret blink — the clock's second consumer

Blink is a scheduler deadline driving a WRITER (decisions.md: "frame
counters and wall clocks may drive writers — a tween writes through the
event door like anyone else"; the clock never enters derivation):
`frame-scheduler/register-deadline!` with id `:t2/caret-blink`,
`{:next-deadline … :cadence 530.0 :stop-predicate (no live session)}`
(mandatory stop-predicate — `frame_scheduler.cljc:47-58`), armed/retired by
the `sync-pulse-deadline!` bridge shape (`frame_runtime.cljs:285-304` is
the exact precedent). THE DUE-TICK ROAD IS NAMED (round finding 1):
registration stores scheduling data only, and `decide-at!` RETURNS the
due ids in `:due-deadlines` (`frame_scheduler.cljc:157-163`) which today
NO caller consumes — so this atom adds a THIRD nil-default hook at the
frame sink: `runtime/render.cljs` (not on the ZERO list) gains an
installable due-deadline consumer invoked with
`(:due-deadlines scheduler-step)` between the decide/publish pair
(`render.cljs:310-314`) and the encode branch; nil (unflagged, or no
session) → byte-identical. The editing runtime installs it at flag boot;
on a due `:t2/caret-blink` it toggles the caret node's presence in the
session slot — an ordinary store write through the event door (the
deadline-driven-writer law); the toggle triggers ZERO layout calls; the
caret's bytes when visible are constant (no phase
uniform, no shader edits). Typing/motion resets blink to visible and
re-arms the deadline. Encodes tick at the blink cadence ONLY while a
session lives, and stop when it ends (W0-C §5.7 rules 2–3; the pulse's
stop-on-empty precedent). The LEGACY 530ms blink/shimmer machinery
(`events.cljs:191`, `runtime.cljs:425-428`, the `editor_compute.cljs:798`
ticker chain) is UNTOUCHED, and W4's routed "migrate caret blink onto the
scheduler at the clock's second consumer" is discharged WITH CAUSE
re-recorded: T2's caret is scheduler-native from birth; the legacy timers
drive legacy-only surfaces and die with the legacy road at the product
flip — migrating them now would touch unflagged product behavior for zero
product gain.

### Rendering — session text and editing furniture through existing lanes

The editing runtime holds ONE layout result per session-document state
(the session's view-document through `tl/layout`, block-greedy at the
fixture's declared wrap) and projects everything from it:

- the fixture slot's text nodes. Fixture REGISTRATION happens once, at
  flag boot, through `register-face-instance!`; per-edit re-upserts NEVER
  re-call it — it allocates a fresh container id every call (`alloc-cid!`
  + `ctn/add-container`, `scene_runtime.cljs:143-152`), so per-keystroke
  re-registration churns cids and leaks registry entries (round finding
  9). The re-upsert road is the CHROME UPDATE SHAPE: `editing_runtime`
  swaps the PUBLIC store atom cross-namespace —
  `(swap! scene-runtime/!scene-store ss/upsert-slot vi {…})` — preserving
  the slot's existing `:container`/`:container-slot`/`:stack-path`/
  `:stratum` read from the live slot (`update-registration-tree!`,
  `chrome_runtime.cljs:56-64`, is the exact precedent — private there, so
  the editing runtime carries its own few-line equivalent;
  `scene_runtime.cljs`/`scene_store.cljc` stay ZERO; note `upsert-slot`
  replaces the slot wholesale and does NOT inherit `:container` when
  omitted — `scene_store.cljc:163-181` — so the read-modify-write is
  mandatory). Receipt: the fixture's container id is captured at session
  open and asserted UNCHANGED across every edit (rides `:census` in the
  receipt global — the churn wrong build fails);
- the caret node (the `ground-caret` shape, `face_primitives.cljc:786-793`:
  a world rect from `tl/caret-result`, min-width 2.0 world units — the
  recorded default; width scales with zoom, which is the Figma-correct
  behavior for content-anchored editing furniture; a screen-constant caret
  regime is a routed refusal);
- range-highlight nodes per visual line (`tl/selection-result` rects — the
  selection-wash shape, `:741-751`; consumed ranges contribute zero width
  by construction);
- preedit underline rects (thin world rects under the preedit span).

**The one-result identity obligation:** every painted glyph position and
every caret/selection/preedit rect for the edited node derive from ONE
layout result per (session text × provider × metrics) state. `:layout/id`
is a CONTENT HASH of the semantic input (`input-id`,
`text_layout.cljc:145-148`; shaped road `:757`) — equal ids prove equal
INPUT, never one call, so id equality alone cannot catch a duplicated
layout (round finding 5). The one-call truth is a COUNTER: the receipt
global carries the editing runtime's own seam-call count (`:seam-calls`),
and S2 asserts EXACTLY ONE `tl/layout` call per session-document state
transition — typing, paste, IME update each exactly one; blink toggles
ZERO — on every road, not just composition. On top of the counter: where
the paint road carries the result, the receipt asserts `:layout/id`
equality (the W1 T0-1 shape); where the implementer's road emits per-line
nodes, the receipt asserts value-equality of painted line geometry
against the session result. The checked claims below resolve which road is real;
neither road may re-derive geometry outside the seam (the T-fence gains
the editing runtime's layout-calling owner rows — the connector
`layout-label` precedent, `verify_text_layout_fence.mjs`).

### Build order (serial internal cuts, one owner)

1. The kernel (`text_editing.cljc`): index model, edit ops, motion,
   selection, IME state machine, paste normalization, dispatch decisions.
2. Segmentation (`editing_segmentation.cljs` + the injected seam).
3. The runtime (`editing_runtime.cljs`): session lifecycle, IME host, the
   two nil-default hooks, clipboard bridges.
4. Geometry/paint: slot upserts + caret/range/preedit nodes + the
   one-result identity receipt.
5. Blink via the scheduler deadline + the frame-sink due-consumer hook.
6. Fixtures (paragraph + clipped card), verifier receipts, goldens, the
   five-truth receipts.

## Ruled at cut — defaults with Sid veto slots (one line reverses any)

1. **The five-truth bounds ruling above** — headline: object selection
   keeps full semantic bounds PERMANENTLY (Figma semantics; the W4
   observation is correct behavior, now declared); editing geometry is
   world-subtree truth that clips with the text.
2. **Index domain stays tagged UTF-16 v1**; grapheme motion/deletion via
   injected `Intl.Segmenter`; no normalization transforms.
3. **IME host = one hidden textarea, composition sink only** (never
   document truth); EditContext routed.
4. **Entry = dblclick on a T2 fixture; exit = Escape / outside click**;
   precedence session > chrome > legacy; real blocks refuse v1.
5. **Blink = 530ms scheduler deadline driving a presence writer**,
   stop-on-session-end; legacy timers untouched (re-route recorded).
6. **Paste = plain text, exact normalization set, named budget refusal**
   (default budget ≥100k code units, implementer-recorded).
7. **Caret min-width 2.0 world units; underline thickness/offset
   implementer-recorded** — world-scaling editing furniture v1.
8. **Editing visuals export mid-session** — the declared P2.10 deviation,
   receipted, repaid at the editing-chrome expansion.
9. **The editable set v1 = the two T2 fixtures** (proportional paragraph ·
   GPU-clipped card); session truth only, reload → gone (M8 absence
   receipts).
10. **Word-select on dblclick-in-session; triple-click routed.**

## Refusals — deliberately not in this atom (each routed, never a void)

- Durable text-write custody — the product's editors (ground blocks, file
  editor, cmd panel) riding this floor, the `:object/edit` envelope road,
  and any echo-model change → the staged product flip, Sid's line, after
  the seam courtroom (the write path stays exactly as settled).
- Undo/redo → the minted-diffs/ledger road (the kernel's data ops are its
  ready input; inverses arrive there).
- Rich text: style-run editing, styled paste (`text/html`), font/size
  authoring UI → style-run grammar + the authoring slice.
- Edit-mode outline chrome · screen-constant caret/handle metrics ·
  low-zoom editing chrome policies (CH-20) → the editing-chrome expansion
  (also the export-deviation repayment home).
- Text-box resize/overflow handles + auto-height policies (CH-09's tail) →
  authoring/chrome expansion.
- Keyboard nudge of selected objects → manipulation expansion (re-routed
  with cause above; the dispatch seam it needs lands here).
- Grapheme/codepoint index DOMAINS (source-map version bump) → LATER road
  through the same seam.
- Unicode normalization policy (NFC/NFD) → its own road.
- Multi-caret · column selection · collaborative carets → ledger/
  multiplayer roads.
- Triple-click line/paragraph select · select-all chord in-session ·
  shift+click text-range extension (the gesture is chrome's selection
  door — the coexistence law; shift+arrows and drag extend v1) →
  selection-gesture expansion.
- Touch/pen text input · coarse-pointer caret targets → modality
  expansion.
- IME reconversion/surrounding-context APIs · EditContext host swap → IME
  expansion.
- Header-domain (`[:header h j]`) editing → fold/anatomy authoring road.
- Caret scroll-into-view / viewport follow → viewport road (fixtures sit
  in-view).
- Spellcheck · autocomplete · find/replace · accessibility mirrors
  (W0-C P2.12) → LATER.
- Vertical text · ruby → typography expansion.
- Session editing of connector labels (`layout-label` material) → the
  label expansion (its contract already routes label editing).

## Laws — pointers, not restatements (each with its operationalizing scenario)

- **W1 Contract T §7.1–7.4** — the one layout seam; T2's named obligations
  ("Unicode-safe editing, IME, paste, and source mapping through the same
  contract; it does not create a second layout seam"); caret stops are
  cluster/shaper output; the seven readers; no untagged offsets; the
  two-subroute pick sentence. → S1, S2.
- **W1 Contract O §3.2 + W4's pick law** — pick filters the tape, reverse;
  ZERO pick changes; the together-routed aware road untouched. → S3.
- **W1 Contract G** — §4.9 text row: "object/cluster/caret pick route
  declared" (this atom DECLARES the caret subroute as truth 5); §4.7 slop
  untouched. → S2, S3.
- **W1 Contract M** — ephemeral citizenship (the chrome pin): session
  grammar fail-closed in the kernel; M8 persistence-ABSENCE receipts
  (reload → no session, no visuals); M10 N/A by grammar (no GPU resources
  minted — editing visuals are ordinary nodes). → S4.
- **W1 Contract C** — editing visuals are ordinary rect nodes through the
  tagged color road; no new paint classes. → S3.
- **shaping-correction CONTRACT** — §4 I1/I2/I4/I5 (one carried authority,
  declared key, linear construction, span helpers); §5.1–5.3 wrap
  semantics; **§5.4 the reader law across consumed ranges** (this atom's
  motion/selection/copy make it executable); §6 key discipline (layout at
  origin 0/zoom 1; zoom never a layout input). → S1, S2.
- **decisions.md "The render seam"** — change minted at write sites
  (keystrokes are writes; ops are values); no execution clock as
  derivation ancestor (blink is a deadline-driven writer; the static token
  check makes it fail-able); the five declarations on the editing derive
  (keyed inputs = session state × layout result; door = input events +
  the blink deadline; ownership = session truth, one generation; ­
  projections = slot nodes + receipts; oracle = the one-result identity
  receipt). → S4.
- **ENGINE.md §0** — the T2 row (scope) + the full-breadth law (the
  obligations are the floor) + the gate sentence (no central branches —
  this atom registers NOTHING new in the frame body; it emits ordinary
  nodes). → scope, S3.
- **W0-C §2.2 CH-09 + §2.3 P0.5** — caret, range highlight, composition
  underline from one layout result; "clock only for caret blink". → S2,
  S4.
- **W0-C §5.7 rules 2–3** — sleep on stop; registered deadlines with stop
  predicates. → S4.
- **The chrome contract** — selection ≠ focus (plain click keeps its
  first-light meaning; dblclick is NEW and flag-only); chrome namespaces
  untouched; Escape precedence composes with chrome's clear. → S3, S4.
- **Dark-lane / flag / golden laws** (decisions.md; the shared-file law) —
  new namespaces load pure; flag absent byte-identical; goldens
  append-only via this atom's OWN one-shot; `--update-goldens` is
  forbidden in the harness itself (`run_verifier.mjs:624-627`); prior
  atoms' input digests that shift ride their existing
  `--append-*-input-amendment` roads. → S3, S4.

## Exact entry points

New namespaces (ALL new code lives here):

- `src/app/client/workspace/text_editing.cljc` — the pure kernel: session
  state value + fail-closed constructors (tagged offsets only) · edit ops
  as data (`:insert`/`:delete-range`/`:replace-range` + caret/selection
  transitions) · motion (grapheme/word over injected boundaries ·
  visual-line vertical with desired-x · home/end · the §5.4 consumed-range
  law) · selection model (caret+anchor, replace-on-input) · the IME state
  machine (events in → state + ops out; the one-commit-one-op law;
  keydown-during-composition guard) · paste normalization (the exact
  set + budget refusal) · the dispatch decision fn (event map × state ×
  boundaries → op | pass-through; the pinned pass-through list) · surrogate
  invariant asserted at every op boundary. JVM-tested with injected
  fixture boundaries and the shaping-correction mock provider.
- `src/app/client/workspace/editing_segmentation.cljs` — the
  `Intl.Segmenter` providers (grapheme + word), identity surfaced for
  receipts; nothing else.
- `src/app/client/workspace/editing_runtime.cljs` — flag-boot wiring: the
  dblclick entry listener (pick-world → fixture gate → session open at the
  hit-test stop) · exit roads (Escape · capture-phase outside-pointer ·
  teardown; cancel-composition-first) · the IME host textarea (create/
  focus/position-at-caret-screen-point/clear; composition + input + keydown
  listeners driving the kernel) · the two intercept-predicate installs +
  the frame-sink due-consumer install ·
  clipboard bridges (paste normalize+insert; copy/cut via
  `navigator.clipboard`) · session slot upserts (per-edit text nodes +
  caret/highlight/underline nodes from the ONE session layout result) ·
  blink deadline registration (`:t2/caret-blink`, 530ms, stop-predicate;
  the `sync-pulse-deadline!` bridge shape) · the TWO fixtures (proportional
  paragraph: multi-line, block-greedy wrap, a ligature word, an RTL span,
  an "é"; the clipped card: the W4 `clip-card` shape with `:gpu-clip?` and
  the −6px overhang, same text family) · provider installs (camera +
  effective-transforms accessors at boot — the chrome precedent — PLUS
  the session LAYOUT PROVIDER, round finding 6: captured at flag boot
  from the same live font-assets road the paint threads —
  `(:layout-provider active-font)`, `ground.cljs:503` /
  `scene_runtime.cljs:109`; `tl/layout` falls back to the LEGACY road
  SILENTLY when `:provider` is absent or lacks `:shape-line`
  (`text_layout.cljc:966-972` + `:286-287`), so on the browser road a
  session layout whose `:receipts :font-shaper-environment` is the
  legacy environment (`:shaper-id :legacy/code-unit-grid`) is a NAMED
  VERIFIER FAILURE, never a fallback — the ligature/bidi obligations are
  vacuous on the legacy road; JVM tests ride the shaping-correction mock
  provider, shaped by construction) ·
  `globalThis.__softlandEditingReceipt` {:session-target :layout-id
  :seam-calls :font-shaper-environment :blink-armed :blink-toggles
  :due-consumer-installed :intercepts-installed :census} (the established
  receipt-global pattern).

Thin hooks only (few lines each, enumerated):

- `src/app/client/workspace/events.cljs` — ONE nil-default intercept
  predicate consulted at the keydown handler head (`:164-174`); nil →
  byte-identical. Installed only at flag boot; returns true only while a
  session is live.
- `src/app/client/workspace/runtime/mouse.cljs` — the same shape at the
  paste listener head (`:40-80`), before its focus `case`.
- `src/app/client/workspace/runtime/render.cljs` — ONE nil-default
  due-deadline consumer at the frame sink, invoked with
  `(:due-deadlines scheduler-step)` between the decide/publish pair
  (`:310-314`) and the encode branch; nil → byte-identical (the blink
  road — round finding 1; render.cljs was already outside the ZERO list,
  and its digest already sits in the path/connector/chrome/w4 input
  sets, which this atom's live_atoms/verifier edits shift anyway — zero
  additional amendment sets).
- `src/app/client/workspace/live_atoms.cljs` — editing_runtime boot hook +
  fixture container registration (the named-hook exception); boots AFTER
  chrome_runtime and frame_runtime (order pinned — the receipt-global
  chain the pulse already rides).
- `test/app/test_runner.clj` — register `text-editing-test` (+ the
  geometry test ns if split) in the pure tier.
- Verifier lane (append-only, this atom's OWN roads — the established
  shape): `verifier.cljs` `run-t2-input-floor!` wired into the top-level
  `Promise.all` (the S2/S3/S4 machine receipts + 3 goldens + the identity
  receipt + the export-deviation count); `run_verifier.mjs` t2 rows +
  `t2InputFloorInputs` digest set + one-shot `--append-t2-goldens`
  (preflight: prior banks byte-identical + no existing t2 cases) +
  `--append-t2-input-amendment`; `manifest.json` gains the t2 sets;
  `verify_text_layout_fence.mjs` GAINS the editing namespaces'
  layout-calling owner rows (the connector precedent) — the fence's
  banned-pattern sweep then polices them forever. SHARED-FILE LAW (membership corrected by round finding 7): this atom
  edits `live_atoms.cljs` · `events.cljs` · `mouse.cljs` ·
  `runtime/render.cljs` · `verifier.cljs` · `run_verifier.mjs`. The
  digest-shifting members are live_atoms (in the path/connector/chrome/w4
  input sets), runtime/render.cljs (the same four), verifier.cljs (ALL
  five, incl. image), and run_verifier.mjs (chrome + w4);
  `events.cljs`/`mouse.cljs` are hashed in NO input set
  (`run_verifier.mjs:298-460`). So ALL FIVE prior atoms' input-set
  fingerprints move and ride their existing `--append-*-input-amendment`
  one-shots (each gated on a full-green preflight,
  `run_verifier.mjs:1701-1731`), never a golden edit.
- ZERO edits (named, so the round can check them): `ground.cljs` ·
  `renderer.cljs` · `scene_store.cljc` · `rect_tree.cljc` ·
  `scene_tape.cljc` · `containers.cljc` · `scene_runtime.cljs` ·
  `frame_graph/frame_effects/frame_scheduler/compositor_gpu/
  frame_runtime` · all chrome namespaces · path/connector/image
  namespaces · the legacy input models (`text_input.cljs` ·
  `block_edit.cljc` · `block_edit_wiring.cljs` · `ground_edit.cljc` ·
  `cmd_panel.cljs` · `runtime/keyboard.cljs`) · `electric_flow.cljc`.
  PLANNED ADDITIVE EDIT (upgraded from contingency — the round PROVED the
  reader gap, finding 2: the shaped `caret-result`/`hit-test-result` snap
  to declared stops, `text_layout.cljc:1190-1204`/`:1393-1402`):
  `text_layout.cljc` gains ADDITIVE reader capability only — the
  injected-boundaries interior-caret option on `caret-result`/
  `hit-test-result` (plus an affinity-aware bidi caret-x helper if S2
  needs it) — riding the T-fence owner rows + the shaping/T2
  input-amendment roads, recorded in the baton, never silent. Everything
  else in `text_layout.cljc` is untouched.

CHECKED CLAIMS for the implementer (verify FIRST, minutes not hours):
1. A hidden focused textarea receives composition events while the canvas
   page has no other focusable surface (G1 ground truth: no DOM text
   surface exists — nothing competes), and synthesized
   `CompositionEvent`s drive the same listener path for machine receipts
   (real-IME behavior is the felt line, not a machine claim).
2. The `(= 1 (count key))` admission plus the seven `!focus` routers are
   the ONLY legacy keydown consumers (`events.cljs`/`runtime.cljs`) — the
   intercept predicate therefore silences the legacy lane completely.
3. Where fixture text layout actually happens on the paint road
   (`rect_tree.cljc:145` `resolve-text-layout` vs carried results), and
   whether the one-result identity receipt lands by `:layout/id` equality
   or per-line value equality — record the road taken.
4. Slot re-upsert per keystroke keeps entry identity stable in the
   maintained arrangement (no churn/flicker — the anchor-pattern law).
5. `Intl.Segmenter` exists in the pinned verifier Chrome; if absent
   there, verifier rows inject fixture boundaries through the kernel seam
   and the Segmenter identity receipt moves to the felt lane (declared).
6. The capture-phase outside-pointer check coexists with ground's pointer
   machinery (the click proceeds to its legacy meaning after session
   close — no double-handling).
7. The due-consumer hook's placement (post-decide, pre-encode-branch)
   lets the due frame paint the toggled caret WITHOUT an echo encode next
   frame (the store write may re-fire world-changed?). Kill-probe:
   scheduler counters over ~5s of idle live session — ~9–10 encodes
   expected at 530ms; ~19 = echo; if the echo is real, the damping is the
   implementer's recorded default in NOW (S4's cadence receipt is the
   truth either way).
8. The live font-assets accessor the paint road threads
   (`(:layout-provider active-font)` — `ground.cljs:503`,
   `scene_runtime.cljs:109`) is reachable at editing-runtime boot and
   yields a SHAPED provider in the verifier environment (else finding 6's
   named failure fires — record the accessor taken).

## Decisive scenarios (frozen as tripwires + goldens at close)

1. **Kernel truth** [JVM, pure — injected boundaries, mock provider]:
   the Unicode table — "é" (e + U+0301): one backspace deletes the whole
   grapheme; an emoji ZWJ family: arrows skip it whole, backspace deletes
   it whole; a surrogate-pair fuzz set: NO op sequence ever yields an
   offset inside a pair (property assertion); the ligature "fi" (one
   cluster, two graphemes): caret ENTERS between f and i (interpolated x),
   backspace after "fi" deletes "i" only — the motion-by-cluster and
   delete-by-cluster wrong builds FAIL; word motion lands on injected word
   boundaries; vertical motion over the WRAPPED block-greedy fixture
   ("abc   def" at the §5.3 budget) round-trips with desired-x; home/end =
   painted ends; the §5.4 law executable — caret stops exist at consumed
   offsets and display per law, selection across a consumed range adds
   zero width, copy INCLUDES the consumed spaces; IME machine —
   start → n×update → commit yields exactly ONE insert op of the composed
   string (the per-update-commit wrong build fails), cancel yields zero
   ops, keydown-during-composition is dropped, exit-during-composition
   cancels first; paste rows — CRLF/CR→LF, C0 stripped except \n \t, tab
   and \n preserved, over-budget refuses by name; dispatch — printable/
   nav/edit consumed, the pinned pass-through list passes; every kernel
   boundary rejects untagged offsets (fail-closed constructor).
2. **One-seam identity + geometry** [JVM mock-provider + browser
   verifier]: the one-result identity receipt — painted glyph geometry and
   caret/selection/preedit rects for the edited node derive from ONE
   layout result per state (`:layout/id` equality or per-line value
   equality per checked claim 3; asserted BOTH JVM and live via
   `__softlandEditingReceipt`); caret round-trip — offset → `caret-result`
   rect → `hit-test-result` at its center → the same stop (± declared
   affinity) across: an RTL/LTR run boundary (both stops honored THROUGH
   SESSION STATE — store the caret at each branch, round-trip it, the
   two states stay distinct: offset + affinity; finding 3), a wrapped
   line boundary (upstream end vs downstream start, the same
   through-state law), a LIGATURE-INTERIOR grapheme boundary ("fi"'s
   interior offset: `caret-result` x strictly between the cluster's two
   declared stops and `hit-test-result` at that x returns the interior
   offset — the LIVE readers, never a test-only helper; finding 2),
   consumed offsets, and a preedit-active layout; the `:seam-calls`
   counter asserts EXACTLY ONE `tl/layout` call per session-document
   state transition on EVERY road — typing, paste, IME update each
   exactly one; blink toggles ZERO (finding 5); preedit is ONE
   `tl/layout` call over committed ⊕ preedit (receipt: exactly one seam
   call per composition update; the T-fence's new owner rows are green and the
   banned-pattern sweep finds no independent metric in the new
   namespaces).
3. **The five-truth receipts + goldens** [verifier]: 3 appended goldens —
   (a) a live session in the proportional paragraph at zoom 1: caret +
   a multi-line range highlight spanning the ligature and the RTL span;
   (b) the CLIPPED card on the GPU road: the caret placed at an offset
   whose rect falls partly in the −6px overhang region, and a range
   highlight crossing the clip edge — probe-pixel assertions beside the
   golden: a caret/highlight pixel INSIDE the clip is painted, the same
   geometry OUTSIDE the clip reads BACKGROUND (the semantic-bounds-caret
   wrong build fails by probe), and the glyphs at the edge clip
   IDENTICALLY to the furniture; (c) a composition state: preedit span
   underlined, caret inside preedit. Blink is pinned VISIBLE for capture
   (injected clock/state); determinism ×2. Chrome truth — the coexistence
   SEQUENCE is pinned (finding 4): open the session FIRST (the entry
   dblclick's constituent taps run the untouched legacy tap road and
   clear any prior chrome selection — expected), THEN shift-tap the
   edited fixture; the tap PASSES THROUGH the session's shift carve-out
   to chrome's toggle door, and the object outline appears at FULL
   semantic bounds while caret/highlight stay clipped — coexistence
   captured from real gestures (truth 1 executable). Every chrome OUTPUT
   golden/receipt is byte-identical and the six chrome NAMESPACE digest
   entries inside `chromeAtomInputs` are unchanged; the chrome INPUT-SET
   fingerprint itself MOVES (it hashes live_atoms/render.cljs/verifier/
   runner — finding 7) and rides the chrome input-amendment road. Pick truth — the
   scene_store/rect_tree digest rows in every prior input set are
   UNCHANGED (zero pick-road edits, machine-checked), and a click into
   the clipped-away overhang resolves exactly as today (case receipt).
   The export-deviation receipt — a mid-session `:world` export CONTAINS
   the session visuals, counted and declared with the repayment road
   named. ALL existing goldens byte-identical; the MSDF counterexample
   stays RED; prior-atom input amendments recorded for the four shifting
   shared files (live_atoms · runtime/render.cljs · verifier.cljs ·
   run_verifier.mjs) — all five prior atoms' sets move (finding 7).
4. **Runtime / dispatch / blink truth** [browser verifier + unflagged
   tripwire]: flag-off — hooks nil, zero listeners, zero fixtures, all new
   namespaces load pure, byte-identical product (extends the established
   flag-off tripwire); flag-on, NO session — keydown and paste flow to the
   legacy lane byte-identically (intercept passes through; dblclick on
   empty ground/blocks is a no-op); session open — keydown consumed
   wholesale (the legacy `!caret-visible` and focus routers see nothing —
   receipt), paste intercepted and normalized, copy/cut round-trip through
   the system clipboard; blink — encodes tick at the 530ms cadence ONLY
   while the session lives (scheduler receipt counters, the W4 S4 shape),
   the toggle rides the due-consumer hook (receipt:
   `:due-consumer-installed` true; `:blink-toggles` counts while live and
   freezes after exit — finding 1), the deadline retires on exit and
   encodes return to idle, typing resets
   the phase to visible; IME host truth — the hidden textarea HOLDS
   FOCUS while the session lives, and its screen position tracks the
   caret's world→screen point after motion (candidate-window anchoring,
   machine-asserted — finding 8); the clock stays out of derivation
   EXECUTABLY —
   the W4 JVM tripwire pattern extended: session-state derive fns are pure
   of clock reads, and the static token check over the new namespaces
   finds no `js/Date`/`performance.now`/rAF/`setInterval`/`setTimeout`
   (the COMPLETE timer ban — a recursive-setTimeout blink beside an inert
   deadline is finding 1's named wrong build); precedence —
   session Escape closes the session ONLY (chrome selection intact),
   Escape with no session behaves exactly as before; entry — dblclick at
   a glyph opens with the caret at that cluster's stop (receipt);
   outside-click ends the session and the click's legacy meaning happens
   once (no double-handling).
5. **The felt receipt** [Sid's eyes, `?live-atoms=1`]: dblclick into the
   paragraph — the caret lands where you pointed and blinks; type
   "office" — the ligature forms live and the arrows walk INTO it; arrow
   across "é" in one press; Ctrl+arrow word-jumps; drag and shift+arrow —
   the highlight hugs glyphs and wraps lines; dblclick a word — it
   selects; paste a paragraph — it lands as one act, proportionally
   wrapped; edit the CLIPPED card — caret and highlight stay inside the
   window exactly like the glyphs, while shift-tap's blue outline still
   frames the full semantic box — acquired mid-session through the shift
   pass-through (the ruled relationship, felt in one
   glance); compose through the OS IME — a NAMED acceptance criterion,
   not an if-installed aside (finding 8): preedit
   rides inline underlined and commits as typed (the machine half stays
   synthesized-event truth per checked claim 1 — headless CI cannot
   drive a real OS IME; S4's focus/anchoring receipts close the
   mechanical gap); Escape — the blink stops
   costing frames (`__softlandFrameSchedulerReceipt` visible); flag off —
   nothing anywhere.

## MUST-NOTs

Never read `src/app/server/env.clj` · NO durable/Rama writes — the session
is client truth; the `:object/edit` envelope/outbox/echo road is UNTOUCHED
(no echo-model change, no optimistic-echo change, nothing) · no second
layout seam — every editing/preedit layout rides `tl/layout` (W1 §7.1) ·
no second pick road and ZERO pick changes — `ss/pick`, `rect_tree`
hit-test, marquee, and snap are untouched; the together-routed
clip-aware road is not partially delivered · the legacy input models are
untouched (`text_input.cljs` · `block_edit.cljc` · `block_edit_wiring` ·
`ground_edit.cljc` · `cmd_panel` · `runtime/keyboard.cljs`); the ONLY
shared-input edits are the TWO enumerated nil-default hooks
(`events.cljs` keydown head · `mouse.cljs` paste head), each flag-inert ·
`ground.cljs` ZERO edits · `renderer.cljs` ZERO edits ·
`electric_flow.cljc` ZERO edits · no execution clock in any derive path —
blink is a deadline-driven writer riding the due-consumer hook; no
`js/Date`/`performance.now`/rAF/`setInterval`/`setTimeout` in the new
namespaces (static check — the COMPLETE timer ban, finding 1) · never
re-call `register-face-instance!` for a live vi (container churn +
registry leak, finding 9 — re-upserts preserve the slot's container) · goldens append-only via
this atom's own one-shot; never `--update-goldens`; shared-source digest
shifts ride the input-amendment roads · no new JS/npm dependency
(`Intl.Segmenter` is platform) · no chrome grammar/namespace edits · no
normalization transforms on input v1 · commits only at Sid's word — code
and docs separate, both on `docs/current-mental-model-local`, exact-path
staged, never push.

## Close

Scenario 1–2 JVM halves freeze as ~5 tripwires in the new pure test
namespace(s) (`text_editing_test`, splitting a geometry ns if it earns
it); scenario 3's goldens + scenario 2/4's verifier receipts join the
permanent bank via `--append-t2-goldens`; focused suite = the new JVM
namespaces GREEN + `npm run verify:render-engine` whose ONLY red is the
preserved MSDF counterexample (W1's canonical exit — never "fixed" by this
atom). Foreign failures are board debt, never stops — two pre-existing
finds from this cut's gathering are ALREADY board debt, not T2 scope:
legacy `selection-result` returns `:rect` while `editor_compute.cljs:276`
reads `:rects` (nil on the legacy road), and `shell.cljs:216-220` computes
caret-x from a private `count × char-advance` outside the T-fence owner
inventory. One NOW entry (≤15 lines, self-audit included) in
`T2-INPUT-FLOOR-NOW.md` + the board line flip (NEXT after T2: the
Package-2 seam courtroom over the landed atom set). Acceptance = Sid's
word.

## Codex opening prompt (implementation — after the round closes)

```
You are implementing the T2 INPUT-FLOOR atom for Softland's render engine —
one pass, the whole atom, one session, serial internal cuts in the
contract's order (kernel → segmentation → runtime → geometry/paint →
blink → fixtures/receipts).

Read first, nothing else needed (byte-budgeted; scoped where marked):
1. docs/render-engine/T2-INPUT-FLOOR-CONTRACT.md  (~50KB, whole — this contract, the build law)
2. docs/render-engine/W1/CONTRACT-T.md            (5.7KB, whole — Contract T; W1.md is only a locator spine now)
3. docs/shaping-correction/CONTRACT.md §5–§6 ONLY (of 98KB — wrap semantics, the §5.4 reader law, the key discipline; never the whole file)
4. .claude/skills/work-package/SKILL.md           (8.9KB — the one-pass law: build, close, when to ask)

Your structural precedents AT THE SOURCE LEVEL: text_layout.cljc (the seam:
layout/caret-result/selection-result/hit-test-result, tagged indices),
face_primitives.cljc:741-793 (caret/wash as world rect nodes),
chrome_runtime.cljs (session truth, provider installs, nil-default seams,
receipt global), frame_runtime.cljs (flag boot, fixtures, the
sync-pulse-deadline! bridge, its own listeners), frame_scheduler.cljc
(register-deadline!), connector_route.cljc layout-label (a new
layout-calling owner joining the T-fence), and the verifier append roads in
run_verifier.mjs. Consume them as precedents; edit ONLY what the contract's
Entry points enumerate. Do not read IMAGE-ATOM-*/SEAM-STEP1 docs (pre-law).

This contract has been through its four-lens author pass and its bounded
falsification round — build what is WRITTEN; the repairs are law.

Build the WHOLE atom straight through. Keep your own falsification pass and
fix what it surfaces in-session. Ambiguity → strongest default + a note in
docs/render-engine/T2-INPUT-FLOOR-NOW.md. A genuine fork (two readings that
cannot both hold) is ONE question in that file — route around it and keep
building; never stop. Foreign test failures are board debt, never stops.
The checked claims (contract, Entry points) come FIRST — minutes, not
hours, before cut 1.

Close per SKILL.md: tripwires + goldens frozen from the contract's
scenarios; focused suite: the new pure JVM namespaces GREEN, and
verify:render-engine red ONLY on the preserved MSDF counterexample (its
canonical state — do not fix it). NOW entry ≤15 lines, board flip. Commits
only at Sid's word. Acceptance is Sid's word — you never wait on a review
round.
```
