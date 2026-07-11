# editor-loop — ROAD (direction-grade, NOT binding)

**Status:** direction analysis, 2026-07-11 Fable session. Input to the
`block-write` contract (and context for `editor-feel` / `write-echo`, threads
9–10, whose opening prompts carry the actionable slices). decisions.md wins on
any conflict. Sid's verbatim ask: `vision/LOG.md` 2026-07-11 ("write directly
to rama and then stream from there... only and only and only if that is not
feasable... optimistic update route but never by default").

---

## 1. Verified truth: the keystroke path and the write side (2026-07-11)

Keystroke → paint is ALL CLIENT-LOCAL: DOM keydown → `editor-keys-consumer`
(`keyboard.cljs:135`) → pure `text_input.cljs` transforms on `!editor-doc`
(client atom, `state.cljs:68`) → derived flows → RAF sample → GPU.

**"Is it connected to Rama?" — write-only, as telemetry; the loop never
closes:**
- Every keystroke POSTs the ENTIRE document (`pr-str` all lines) to
  `/api/editor/save-doc` (`keyboard.cljs:154-158`, also cut/undo/redo sites
  ~199–227; `sidebar_io.cljs:18-38`) → jetty (`server_jetty.clj:1190`) →
  `util-fns/save-editor-doc!` → `:compat/record` event (allow-listed
  transitional adapter, `core.clj:416`) + server atom mirror
  (`util_fns.cljc:351-364`). Labeled "Phase 4B measurement" — source of the
  recorded 7.5ms round-trip.
- `get-editor-doc` has ZERO callers. Files load from DISK (`/api/read-file`).
  The editor's "save" key creates a browser Blob DOWNLOAD named `code.clj`
  (`keyboard.cljs:79-88`) — the source file on disk is never written.
- **Block containers have NO edit path at all** — faces are read-only
  projections. Sid's question "if I have a block container and I edit it,
  how is it edited" — today's answer: it isn't.
- **The lawful write substrate EXISTS server-side, unwired to any UI:**
  `text_kernel.clj` request depot (`*text-requests-depot`, hash-by routing
  key, per-key serialization in depot order, `:append-ack`, status
  materialization; `foreign-append!` at :562). Read-stream machinery exists
  as the faces pull / S40 server-atom+e/watch pattern. W2 shipped the first
  WRITE e/defn precedent (`RecordFaceWear` + outbox).

Sid's target = the countersigned substrate invariant (UI reads Rama, Rama is
truth, back-arrow) applied to the LAST exempt surface. Composing four
existing pieces + one missing: a designed EDIT EVENT (operation-shaped, not
full-doc snapshot) + a live per-block subscription.

## 2. Typing-lag causes, ranked (evidence in editor-feel prompt, thread 9)

1. Phase-4B mirror on the hot path (full-doc pr-str + fetch + console.log
   per keystroke). 2. O(file) per key event: `(mapv count lines)` every event
   (`keyboard.cljs:146`); whole-file `detect-folds-fn` per text change
   (`editor_compute.cljs:130`); `<bracket-match` O(file) incl. cursor moves
   (`:140`). 3. Monolithic visible-text reshape + full-viewport redraw per
   keystroke (`renderer.cljs:1348`, `render.cljs:374`). 4. RAF alignment
   (≤16.6ms, fine). Diagnosis is near-free: `[RAF]` breakdown log
   (`render.cljs:484-487`) + `[EDITOR-RAMA]` already instrument the path.

## 3. Direct-write echo budget + the caret law

Echo = keydown → request → depot append ACK → materialize → stream →
client signal → RAF → paint. Realistic p50 ~20–40ms localhost (7.5ms
measured for the committed HTTP path; Electric + RAF on top). Perception:
<~50–70ms reads instant; THE RISK IS THE TAIL (one 150ms GC/microbatch stall
mid-word = swallowed keys). Hence `write-echo` (thread 10) with the
PRE-REGISTERED criterion: direct stands iff p95 ≤ 50ms AND ≤1 stall
(>100ms)/min sustained; Sid's fingers final in both directions; thresholds
fixed before data.

**Co-variance law (L8):** if text streams from the server, the cursor must
ride the SAME signal or text/caret tear at burst — so strict no-echo means
the CARET waits too. Pre-registered fallback ladder: if text passes but
caret feel fails, concede a LOCAL CARET AFFORDANCE only (text truth stays
streamed). Full optimistic text stays off the table unless direct FAILS
outright (Sid: "never by default").

## 4. Rope trees — NO (settled in-chat 2026-07-11)

A rope solves whole-document-as-one-buffer. Softland's document is a tree of
blocks: the block IS the rope leaf, the kernel IS the tree. Edit cost
O(current line); render windowed to visible ±5 (`combined_text.cljs:317`).
Every O(file) offender found is incidental (cacheable/debounceable/
removable), not structural. Someday-policy: block size cap/split for pasted
megabyte blobs. Not now.

## 5. Event grain

Keystroke-grain writes are ON-thesis: history-is-terrain at typing
resolution; the trail view wants semantic edit spans — Rama compacts
downstream (stream topology → coarser PState). Do not pre-batch keystrokes
to flatter echo numbers (write-echo prompt pins this). Volume is trivial for
Rama; single writer today means no reconciliation problem; multi-writer
later reintroduces it (CRDT/OT questions deferred to that form-break).

## 6. Sequencing

`editor-feel` (repair, thread 9) ∥ `write-echo` (spike, thread 10) NOW →
echo verdict → Fable drafts the decisions.md PROPOSED route ruling → Sid
countersigns → `block-write` CONTRACT (package-scale: first WRITE face —
editing blocks in the reader face; the reader face landed with W2). The
64-editor/plurality scaling of editing rides `container-transforms`
(`build/spatial/ROAD.md` §2) — block-write must not preclude it: per-block
events, per-block subscriptions, no O(all-blocks) invalidation.
