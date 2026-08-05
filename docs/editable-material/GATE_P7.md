# GATE P7 — the portal proper: PASS

Gate session 2026-07-25 (fresh Fable context), gate object `04ae2df` + gate
fix `c0838c1` (this session). HEAD's code tree at boot was byte-identical to
the gate object (`c4ae58c` touched docs only). Authority: DIRECTION.md +
CAMPAIGN.md + PROMPTS.md §P7; NOW.md tail read as input only. Everything
below was re-derived by this session; nothing is carried on the build
session's word alone.

## Receipts (independently re-derived)

- **Full suite**: `clj -X:test` on the gate object — **444t / 6,225a / 0 / 0**,
  exact match with the banked numbers. All three registered flakes clean on
  attempt 1; registry unchanged. Portal namespace alone: 16t / 284a / 0 / 0,
  with the G1 receipt printed — **17/17 questions ANSWERED, each with its
  replayable JVM + console call**.
- **cljs COLD compile** (the receipt the build session could not produce):
  cache dir moved aside (the sandbox refused `rm -rf`; `mv` is equivalent and
  reversible), then **311 files / 310 compiled / 0 warnings / 114.3s** through
  the dev app's own watch lane. The watch lane counts ~40 shadow-devtools
  namespaces the bare-compile "270 files" figure never included; 0 warnings
  over a fully cold cache is the receipt. See §Ops for why the bare
  `shadow-cljs compile` lane can never produce this receipt on this machine.
- **LIVE open, durable cluster, fresh JVM, read-only**: 1,243ms · 0 errors ·
  0 unanswered · six masters · **`fm:text-body` serves
  `wrap-fallback-columns 64`** — Sid's Gate-3 activation read back through
  the portal with its full trace: actor `fable-p6-gate`/`:agent`, grounds
  `exp:reply-width` `:grounded-in`/`:experience` labeled `:grounds/declared`,
  scope `[:scope/all-unpinned]`, all three announcement scales present
  (breath / trace / weather), and **the rollback path to the 80-revision
  named in the trace** (Sid's reversal word stays executable). latest ≠
  active live on `fm:provenance` + `fm:threaded`; `fm:threaded`'s newest
  change-kind is `:change/recovery` (the land HAS re-worn a previous
  revision). 26 nameable cuts across six masters · six recovery offers ·
  lint clean · truncation complete · 196 settled cells.

## The five falsification targets (this gate's own probes, beyond the suite)

1. **"Answered one by one" cannot go vacuous.** The transport outer-catch
   (forced with unseqable wearers) reports honestly; no question answers at
   `:portal/questions` or `:portal/errors` (the annotation cannot make
   itself true); all 17 answer paths are distinct. HOLDS.
2. **Determinism, cross-JVM.** Two fresh JVMs against the live cluster
   produced byte-identical canonical EDN — sha256
   `7f91998f3c87f13d95487dbec4bc914dfe66874ca524e48e7b939a4b6778f29a` both
   runs. Stronger than G2's same-JVM assertion: it kills hash-iteration and
   identity leaks a single process can hide. Also byte-equal ×2 on the modes
   the suite never hashed (drill / why / scope / widen-to-registry). HOLDS.
3. **The fixed FIVE (the N+1 falsifier).** Nine modes — plain, empty→widen,
   nil entity, 40-wearer page, drill, why, scope, cut, all-masters — each
   produced EXACTLY five sub-serves with exactly the declared face set.
   Pricing widened (1 → 6 masters) without a sixth serve. The STOP cannot
   fire. HOLDS.
4. **Floor card-set invariance.** Nastier garbage than G9's: per-section
   pr-str bombs (values whose printing throws), hostile `ILookup`s NESTED
   inside every section, collections of bombs, a metadata bomb. The 17-card
   set, order, and total rows held through all of it. HOLDS (R1 below for
   the briefing's own boundary).
5. **Read-only byte-identity, live.** All seven portal modes opened against
   the durable cluster between two canonical master snapshots:
   **BYTE-IDENTICAL** (sha `2a4d26f4…`, 19,903 bytes, unchanged) — and the
   same sha held across the ENTIRE headed sitting below, dev-app boot
   included. HOLDS.
6. *(Bonus falsifier that did not fire)*: `history/standable?` looked
   vacuous-true-able for a cut naming a master outside the priced set —
   probed; the truth serve's history covers ALL masters regardless of
   pricing, so the bogus cut resolves `found? false` → `standable? false`.
   Honest.

## The `__portal` sitting (mechanical half, through the real client artery)

Dev app booted by this gate; puppeteer drove the DOCUMENTED console calls
verbatim (headless; WebGPU dead, which is itself part of the receipt — the
seam needs no pixels).

- **One real defect found and gate-fixed (`c0838c1`)**: plain `clj->js`
  strips keyword namespaces, so every documented keyed access —
  `(await __portal.open())['portal/recipe']` — read **undefined**. The
  replayable-call promise is P7's required output form, so this was a G1
  defect in the console half. Fix: the portal seam (only) converts through
  `clj->js :keyword-fn (str (symbol %))`; the earlier `__material`/
  `__bindings` wearers never promised namespaced keys and keep their
  convention (second-wearer law applied to client conventions). No cljs test
  lane exists in the runner; the regression pin is the sitting receipt + the
  seam comment.
- **After the fix, all documented calls replayed**: `open` 1,534ms through
  the artery — 0 errors, entity found, six masters, plan 1 roundtrip /
  0 client joins · `questions` 17/17 answered · `unanswered` `[]` · `render`
  17 floor cards in question order · `briefing` verbatim with
  `<projection>` · `why` with an intentionally stale revision →
  `stale: true` (the detection works) · `blast` with the namespaced scope
  intact in the VALUE (`"scope/all-unpinned"` — the fix visible) ·
  `at` stood a real cut, `standable: true` · self-editing false.
- **Recipe on the GPU-dead scene**: `single` / `recipe:none` / name null —
  the headless page renders no blocks, so the pick stamps nothing and the
  recipe honestly reports nothing recurred HERE. Describe-never-gate
  degrading correctly, not a defect. The `block`/`:derived-uniform` reading
  off a real scene stays with Sid's headed wear.
- **Briefing at a real sitting: 955,495 bytes (~240k tokens)** — the
  wearer-laden projection is ~9× the empty-wearers JVM run (108,493 bytes).
  This sharpens adjudication 3: for P8's summon, narrowing the OPEN is not
  optional.

## Adjudications (delegated to this gate by the mode line; Sid's veto live)

1. **Recipe NAME — ACCEPT "block" as working scaffolding.** The P7
   condition ("iff the worn-five composition has recurred") was met, both
   halves enforced in code; the value is honest that the recurrence class is
   `:derived-uniform` — code attaching uniformly, NOT DIRECTION's deep
   proof; `:recipe/gates? false` permanently; `:recipe/name-authority :sid`.
   Nothing finalizes: names finalize by recurrence and Sid names
   (§Only-Sid). "block" is the word Sid already uses. One line renames or
   withholds it at any time.
2. **The deferred VISUAL face — PASS-compatible deferral.** The render
   MODEL is the floor and is JVM-proven (G9/G9b + this gate's bombs);
   drawing it needs a headed browser with real WebGPU, and shipping
   unverifiable UI would break the wearing law. The visual face folds into
   **Sid's return-wear headed sitting** (with `picked()`, ⌁/≈, the 64 feel);
   no P7b package. The `__portal` console lane is the interim face — now
   proven through the artery.
3. **The briefing — stays the FULL projection, verbatim; narrowing is the
   OPEN's job, and for P8's summon it is REQUIRED.** P7's law is "receives
   EXACTLY this projection"; a briefing-side filter would break byte
   identity with what the human sees. The narrowing knob already exists
   in-law: `:master-ids` / scope / cut are declared, deterministic
   parameters of the projection itself — a summon verb that wants a smaller
   briefing opens a narrower portal and briefs THAT, still verbatim. The
   real-sitting measurement (~240k tokens un-narrowed) makes this a P8
   requirement, not a preference. No new machinery.
4. **`facet-material/resolved-wear` floor honesty (P1 shape)** — the
   `unavailable` shape substitutes floor material AND the floor revision id,
   then passes `valid-material?`, reporting `:floor? false` for a master the
   serve could not read. RULING: the shape SHOULD carry floor honesty at
   source; P7's read-through-the-serve workaround is correct and stays. The
   P1 fix is queued as a LATER item for the next natural P1 touchpoint — not
   a gate fix (the portal already tells the truth).
5. **Echo bar — not gated for P7.** The package adds no work to the
   keystroke path: the portal watch is nil until console-armed, install is
   one-time, zero render/edit-lane files touched. Watch-item at the headed
   sitting: a large portal result streaming while Sid types could
   momentarily contend on the transport — observe once, file if felt.

## Residue (owners named, non-blocking)

- **R1** `portal/briefing` throws on values whose `pr-str` throws (hostile
  JVM objects). Unreachable from any data revision (durable material is
  EDN; EDN values always print), and the transport outer-catch covers the
  reachable space — the render FLOOR survives the same bombs. Owner: P8, if
  the briefing ever wraps runtime values that did not come from EDN.
- **R2** `app.shared.material-portal`'s cljs half has NO client consumer
  yet (the client receives render/briefing precomputed on the envelope), so
  no build witnesses it. First consumer — the visual face — inherits the
  witness duty.
- **R3** Transport-contention watch-item at the headed sitting
  (adjudication 5).
- **R4** The server ns docstring's "no private read path into the land" is
  a shade stronger than literal: identity/placement/trails read through the
  layer's own public read functions (`ocr/read-unit`,
  `episode/read-geometry-cells`, `facet-master/activation-trail`) rather
  than through `serve`. Same machinery one level down, and the fence says
  "wherever possible" — recorded so nobody later reads the docstring as
  "every byte flows through serve".
- Carried from P6, unchanged owners: F3 dead client weather lane · F4
  escape detector `:ambiguous` on the durable cluster · text-body instance
  deviations not felt in the wrap pass.

## Ops (both divergences corrected + one environment law learned)

- The mode line said cluster UP; it was DOWN at boot — the machine rebooted
  7 minutes before the session, killing the daemons the build session had
  seen RUNNING. This gate booted it (`bin/land up`, GATE_P4 precedent: ops
  are the gate session's act); five modules recovered from durable state;
  license active; UI 200. Dev app also booted by this gate (port 8080).
- **The Electric auth trap (cost this session ~1h; now law):** the Electric
  compiler's auth token lives INSIDE the shadow build cache —
  `.shadow-cljs/builds/dev/hyperfiddle.electric.token` — so clearing that
  cache for a cold compile EVICTS it, and every subsequent compile parks
  SILENTLY on a login deref (`hooks3.clj:145`, confirmed by jstack: 0% CPU,
  main parked on a CountDownLatch). The bare `clj -M:dev -m
  shadow.cljs.devtools.cli compile dev` lane shows no prompt at all — it
  just hangs; only the dev-app lane prints the login URL to its log. Sid's
  one click on `hyperfiddle-auth.fly.dev/login?redirect-uri=localhost:8081`
  re-authed and the cold compile completed immediately. Recorded in
  implementation-quirks memory. (This also retro-explains the build
  session's luck: their `rm -rf` of the cache was permission-blocked, so
  they never lost the token.)

## Verdict

**PASS** — gate object `04ae2df` + gate fix `c0838c1`. The portal is
deterministic (cross-JVM bytes), total (17 cards from code under every bomb
tried), batched (five sub-serves, pinned, every mode), honest (unknown
masters named, truncation declared, floor reasons stated, v0 rows
`:grounds/unknown`, a GPU-dead scene described rather than refused),
read-only (live byte-identity across every mode AND the whole headed
sitting), and model-free (in the value, by source scan, by G7). Neither
STOP fired. The one defect the sitting found is fixed and pinned. **P8 is
sendable.**
