# W1 G16 — end-gate artifact (faces-as-assemblies, Wave 1)

2026-07-11 · Fable gate, orchestrating session. Shape per the 2026-07-05
delivery-mode ruling: ONE serial suite + ONE batched falsification-by-class
(5 fresh Opus subagents, class-hunting) + this Fable pass. Fixes applied at
gate per current practice; suite re-run green after every fix.

## Verdict: **PASS** — wave closes; Wave 2 does NOT start.

Suite after all gate fixes: **22 tests / 281 assertions / 0 failures**
(+ the new root-`:each` regression → 23/287 in the final run of the
integration namespace). Shadow build of every client fix: 0 warnings.

## Falsification-by-class — findings + disposition

Five classes, five reviewers. 4 MAJOR (all fixed), 2 findings-as-designed
confirmations, and recorded MINORs. Nothing vacuous: every class but one
returned real substance.

| # | Class | Finding | Disposition |
|---|---|---|---|
| F1 | projection | `apply-until-ms` prefix-consistency was by-DATA not by-construction — a missing/zero timestamp block is included by EVERY cut while earlier real-time blocks are excluded (non-prefix served as replay truth). Corpus verified clean (309 events, 0 bad stamps) so nothing fired live. | **FIXED**: running-max effective-time floor over river order — prefix by construction, identical on clean data. |
| F2 | projection | `serve` was not total: `river-page`'s corrupt-state throws (scan guard, missing surface) would propagate through FacePull into the render loop (no `try` there, L13). | **FIXED**: `try/catch Throwable` in `serve` → `:projection-read-failed` error data-context. |
| F3 | projection | First-light pending / failed / blank-request collapsed into one `:missing-address` error; a FAILED distill never bumped the epoch → permanently empty face with a lying error. | **FIXED**: `!first-light-failed` flag (set on distill failure AND off-box no-transcript), epoch bump on failure, `resolve-request` stamps `:face/first-light` → the error names `:first-light-pending` / `:first-light-failed` distinctly. |
| F4 | state lifecycle | `wear-face!` fetch race: a stale resolution (typo'd face's 404 landing AFTER the corrected face's 200) overwrote `!face-compiled` with a wrong-face error card — persistent wrong render until the next `/face`. | **FIXED**: `publish!` guard — only the still-current face writes `!face-compiled`. |
| F5 | render-path | `install-drag-select!` (sibling raw-DOM listener) was not face-aware: clicks/drags in a full-screen face mutated the HIDDEN editor doc (cursor/selection) and stole `:focus` — typing then edited the invisible file. Trail faces shared the leak. | **FIXED**: `in-normal-editor?` excludes trail + face-assembly modes (click space = render space, R-1 debt 1). |
| F6 | render-path | `/face until` with a shrinking cut stranded the camera past the new content-h — blank pane until the next wheel clamp. | **FIXED**: `:until` resets `!scroll-y 0` (matches `:set`). |
| F7 | render-path | Leftover `!agent-output` rendered the agent panel over the face and stole the wheel in its band (three `(not file-workspace?)`-only gates). | **FIXED for the face** (scroll agent-h, combined_text overlay, cmd_panel bg each exclude face-assembly). Trail parity left as-is: pre-existing, its own wave's debt — recorded. |
| F8 | parity | `hash` leaked into a node id (`hole-endpoint-card`'s `(hash row)` fallback, reachable via `:hole-card` when a row lacks `:relation-id`) — JVM and cljs hash differently → cross-platform id divergence. | **FIXED in the wrapper** (G7 forbids editing the copy): inject the interpreter's structural id as `:relation-id` when absent — deterministic, parity-stable. |
| F9 | contract | An `:each`-at-root assembly compiled clean and error-carded only at APPLY — structural rejection at the wrong layer (trap T3). | **FIXED**: compile-time root-must-be-buildable check + permanent regression test. |
| F10 | contract | New builders re-introduced literal `0.56` fallbacks — the exact drift class CLAUDE.md's sync list tracks. | **FIXED**: ONE named `fallback-char-width` constant; file registered in CLAUDE.md's sync list. |

**Recorded, no code change (accepted at gate):**
- Ratio-vs-double divergence in `empty-state` bounds for odd dims (parity):
  numerically identical; bites only if goldens ever cross the JVM→cljs
  boundary — goldens are JVM-scoped by design; revisit if that changes.
- Error-card TEXT differs across platforms (guard reason string, ex-message
  vs str) and error ORDERING follows map/set iteration — error-path-only;
  the never-throw totality holds on both platforms (verified). Normalize if
  a golden ever pins exact error text.
- First FacePull forces the first-light delay synchronously on the Electric
  server path (seconds, once per JVM) — known first-light scaffold cost.
- First-light runtime is an in-memory IPC: re-harvest+re-distill every JVM
  boot (~3 min empty window); "durable" within one JVM lifetime. Named W1
  limit; real persistence is W2+ kernel territory.
- Electric remount drops the face request (no stale serve, no auto-restore)
  — matches the trail-face atom lifecycle.
- `!face-context` is monotonic (never cleared on `/face off`; unread in
  other modes) — latent W2 note if a projection ever returns nil.
- No face rim in the status strip (chrome shows editor strip) — conscious W1
  deferral; the rim is design-round material.
- Mode-vs-content diamond (`<mode` from local-world vs `<face-assembly` from
  face-state) can pair for one transient frame on face exit — structurally
  identical to the shipped trail face; self-healing; accepted shared shape.

**Clean sweeps (verified, no finding):** V1–V7 at compile · guard mechanics ·
error-card totality + registry independence · apply purity · Δ1 stamp ·
one-wrap `:text-run` law (`:text-layout` nowhere) · request shape · artery
genericity (no face dispatch in Electric) · T9/T13 flow discipline ·
m/latest arity parity on every touched flow (4/4, 7/7, 20/20, 11/11, 5/5) ·
mode completeness across all dispatch sites (`:panes` throw-site covered;
single local-world builder flows face-state) · Electric request teardown
(racing pulls resolve to the latest request only) · epoch-debounce lifecycle ·
bounds clamping for all request shapes · read-only-by-construction (call
graph = the four named query APIs).

## Fable pass (CLAUDE.md falsification protocol)

**Architecture.** Pure interpreter over rect-tree; Electric = transport;
one generic artery; scene-cache flow per the trail shape. The seam law held
everywhere the reviewers pushed. The one architectural surprise of the wave:
the sidebar predates the extracted vocabulary (G14(b)) — recorded as a
vocabulary gap, not a framework failure; equality at assembly grain is
proven where the vocabulary is the source (G14(a), registry smoke).

**Failure modes attempted.** Double-wear race (F4) · scrub-shrink strand
(F6) · hidden-doc mutation (F5) · corrupt-state throw (F2) · dirty-timestamp
non-prefix (F1) · stuck-empty first light, pending vs failed (F3, plus the
INT-found epoch-bump absence, fixed and re-verified on a fresh JVM) ·
platform-divergent ids (F8) · wrong-layer validation (F9) · /face off →
re-enter identical (clean: sentinel reset) · epoch push after face exit
(clean: `(when (:face s))` guard) · unknown face / nil address / limit 0 /
negative until-ms (all total).

**Writers / readers / clearers.** `!face-state`: writer = /face arms only;
readers = wiring request!, flow, local-world; cleared by `/face off` (nil).
`!face-compiled`: writer = wear-face! publish! (current-face-guarded);
reader = flow; cleared only by next wear (acceptable: unread without
face-state). `!face-context`: writer = face_wiring mirror (truthy-guarded);
reader = flow; never cleared — recorded latent note. `!face-scene` +
`!last-face-struct`: writer = flow (T9-clean: watched by nothing that feeds
the flow); readers = combined_text/mouse-hit(D-008 later)/scroll; cleared on
face exit (nil + `::none` — re-entry rebuild verified). `!face-request`:
writer = request! only; reader = Electric watch; cleared via `(when (:face
s))` → nil. `!face-data`: writer = the ONE pull; reader = mirror.
`!first-light-failed`/`!default-address`: writers = the delay's future;
readers = resolve-request; JVM-lifetime by design.

**Async ordering risks.** wear-face! fetch races → closed by publish! guard.
FacePull races → Electric teardown serializes to the latest request.
Distill-vs-epoch → bump strictly after awaited durability (reviewer-verified
against the await chain). Debounce → one outstanding timer, re-read state at
fire.

**Error-path cleanup.** Fetch fail → error-card compile via publish!
(current-face-guarded) → visible card. Distill fail/off-box → failed flag +
epoch push → honest error rendered. Projection throw → error data-context.
Guard's disallowed-value branch → platform-split, never throws.

**Open doubts.** (1) The transient mode/content diamond frame — accepted on
trail precedent, but if a worn face ever flashes visibly on exit, it becomes
the L8 fence's first live case; pre-registered here. (2) The wearing's
visual evidence is CPU-rasterized (GPU capture unavailable headless on this
box) — a native-session screenshot when Sid next runs the app is the honest
completion of G15's screenshot clause; the scene data driving both is
identical and suite-pinned. (3) The running dev instance hot-reloaded the
cljs fixes but not the JVM-side gate fixes (file_viewer/face_projection) —
those are suite-verified; next `clj -A:dev -X dev/-main` boot carries them.

## SLOT numbers (final record)

- SLOT-A: JVM 5.0/12.1/24.4 ms · browser 2.6/6.0/11.8 ms @ ~200/500/1000
  nodes — linear, inside budget, no activation.
- SLOT-B: one-wrap `:text-run` law shipped + gate-verified nowhere-rides-
  the-dead-hook. No cache activation.
- SLOT-C: camera convention wired; content-h declared + clamped; scroll
  never baked into the tree.
