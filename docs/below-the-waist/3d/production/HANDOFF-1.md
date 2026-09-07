# 3D production — handoff 1

2026-09-07. DESIGN-1 slice B, on `main`. Implementation source: `3a2e239`.

**The repository client pure surface is RED:** 138 tests, 1,166 assertions,
zero failures and one error in
`app.client.region3d.on-plane-test/s5-placed-layout-keeps-the-one-glyph-accessor-seam`:
`:text/layout-provider-required` ("Text layout requires a shaped provider").
[Executed log](receipts/client-pure.log). This is the same test/error recorded
by [slice A HANDOFF-2](../../path-kind/production/HANDOFF-2.md); it remains
unrepaired. The passing results below do not turn that surface green.

The scoped implementation is landed, including the shared executor extension,
all five sphere/coating namespaces, tests, one browser leg, the text golden,
READMEs and docstrings. This is the CPU construction slice defined by
[DESIGN-1](../../production/DESIGN-1.md), not an acceptance claim for a live
product or a general 3D host. Those surfaces are untested.

## Commits and verification

| Milestone | Landed change | Evidence |
|---|---|---|
| `bac507d` | Pending-read transactions, phase-addressed requests, answer belonging, pre-loop suspension, provisional reads and `:from` admission. | [executor_pending_test.clj](../../../../test/app/client/engine/executor_pending_test.clj), included in the final focused run below. |
| `3a2e239` | Sphere support, retained coating reads, intrinsic caps, capability/record data, chart and partial-layer compositor extension, subject/order repairs, cold runner, browser pixels and README hierarchy. | [focused-jvm.log](receipts/focused-jvm.log), [repo-verifier.json](receipts/repo-verifier.json), [cold-resume.edn](receipts/cold-resume.edn). |
| This handoff commit | Executed receipts, the exported pictures, departures and remaining boundary. | [provenance.json](receipts/provenance.json) records source and extraction. |

The focused JVM suite passes **79 tests / 706 assertions**, zero failures or
errors. It includes the path pickup and the engine/path tests as well as the
new 3D tests ([runner](../../../../test/app/client/region3d/run_pure.clj),
[log](receipts/focused-jvm.log)). The browser build passes with zero warnings
([build.log](receipts/build.log)). The repository verifier passes all six
guards, seven DejaVu stations, all representative image goldens, the path
pickup golden and the new coating golden ([repo-verifier.json](receipts/repo-verifier.json)).
All **67 source-input digests** in that receipt match `3a2e239`
([provenance.json](receipts/provenance.json)); hashes here record evidence,
never computation identity or a cache key.

Slice A's pickup and physical push checks still pass. The saved dump includes
their complete `pickup` and `push` subtrees: [path-pickup-push.json](receipts/path-pickup-push.json).
Those checks run in [harness/pickup.cljs](../../../../src/app/client/harness/pickup.cljs)
and [harness/path_push.cljs](../../../../src/app/client/harness/path_push.cljs).
Existing GPU goldens were not edited; the repository verifier compares them
with actual readback. The added golden is
[region3d-coating.json](../../../../test/app/fixtures/render_engine/region3d-coating.json),
recorded from the JVM `brush-wire save` result, independently of the browser.

## The hardest case through the landed code

The four events are the definer's `.65, .7, .41, .65` on G at revision 2.
The table binds `read-surface` and `paint` to the shared CPU compositor; each
read sees the retained coating and the painting produced so far. The third
event is inside B's metric disc but outside its retained root restriction.
It therefore reads A and its own painting. Tests:
`brush-test/the-definers-four-dabs-through-the-shared-executor-and-compositor`
and `coating-test/inverse-demands-use-retained-chains-and-root-restrictions`.

| Dab | Read color (linear premultiplied RGBA) | Carry after pickup | Changed texels |
|---|---|---|---:|
| 0 | `[.25 0 .5 .75]` | `[.8125 0 .125 .9375]` | 401 |
| 1 | `[.5390625 0 .328125 .8671875]` | `[.744140625 0 .17578125 .919921875]` | 321 |
| 2 | `[.5 0 0 .5]` | `[.68310546875 0 .1318359375 .81494140625]` | 405 |
| 3 | `[.6631851196289062 0 .2650909423828125 .9282760620117188]` | `[.6781253814697266 0 .16514968872070312 .8432750701904297]` | 401 |

These values are asserted by that brush test and compared exactly by the
browser golden. The final 8,192 float32 components have SHA-256
`30cb13f2147784bdae44b4aa303d412c8980f00c09435c0983521cd291d75579`.
[Browser values](receipts/browser-coating.json), [JVM values](receipts/cold-save.edn).

Grant 0 suspends at item 0 before mixing or painting: no history row, carry
`[1 0 0 1]`, painting revision 0, and the missing B@0 → M@2 dependency named.
A repeated withheld run yields an equal continuation. Grant 1 resumes to
the complete eager state, history, results and subjects. A checkpoint after
two dabs does the same. Tests:
`brush-test/eager-delayed-and-encoded-continuations-keep-the-same-history`.

The cold receipt is **two separate JVM processes**, not an in-process reset.
The first wrote a 142,802-byte withheld continuation and the full eager
values. The second loaded those files, resumed using the capability table,
and compared **state, history, results and subjects by full value**: all true
([save](receipts/cold-save.edn), [resume](receipts/cold-resume.edn),
[runner](../../../../test/app/client/region3d/brush_wire.clj)). Browser delayed
and encoded continuations also match those four complete values
([browser-coating.json](receipts/browser-coating.json)). The browser's encoding
is 142,770 bytes; identical wire spelling across runtimes is not claimed.
Cross-runtime exchange of a continuation is **untested**.

The two provisional schedules remain distinct: only the first read withheld
then granted gives `c1cf8170…` and marks `[true false false false]`; all four
withheld gives `dd7d3355…` and marks all four rows. Reversing coating order or
changing pickup to `.5` refuses the old answer before a row commits. A
duplicate answer at the completed state is stale with four rows intact.
Tests: `brush-test/provisional-schedules-are-declared-and-distinct` and
`answers-refuse-edited-inputs-and-are-consumed-once`. The toy read additionally
routes an answer past an earlier read and changes every request part in
`executor-pending-test/answers-route-past-an-earlier-read-and-compare-every-input`.

The exported raw PNG is 64×32. The JVM decodes it with ImageIO; the browser
decodes it through an image and reads all 2,048 pixels with `getImageData`.
Both count 847 painted pixels. Tests: `brush-wire/golden`,
`harness/coating/run-check!` and the repo golden comparison. The raw picture
and the browser's integer 8x preview were also opened for inspection:

![The four dabs on the chart patch, at 8x](receipts/png/cpu-region3d-coating-8x.png)

[Raw PNG](receipts/png/cpu-region3d-coating.png). The preview is an 8-bit canvas
export; the float32 hash above is checked before picture conversion. These
are chart-patch pictures. Showing this painting on a rendered sphere is
**unimplemented and untested**.

Reach and reuse also run through the executor: q1's distance is
144.54684956268315 mm, the 150 mm cap's area is 67433.94227378976 mm²;
628 mm excludes the antipode, pi R and 629 mm saturate, and 2 pi R cannot
cycle back to a small cap. Tests: `surface-region-test/reach-is-intrinsic-and-retained-as-data`
and `radius-saturates-before-trigonometry-can-cycle`. The retained reach clips
the definer's dab to **34** texels and the cold author's dab to **6,096**
(`26da35e3…`). The original JSON files are copied unchanged under
`test/app/fixtures/region3d/`, their authored fields are compared to the
translated records, and both constructions execute in
`brush-test/cold-authors-fixtures-keep-their-authored-inputs` and
`returned-regions-and-paintings-are-reusable-under-their-subjects`.

## Where the design hid work, and the departures

1. **Phase is an eighth request part.** Before-loop and loop reads may both
   be at 0 and use the same output name. Seven parts do not identify that
   distinction; `:phase` does (`executor-pending-test/read-position-includes-the-phase`).
   It stays necessary while both phases can demand reads. Removing a phase
   or changing addressing changes request equality and the wire vocabulary.

2. **A replayed pre-loop suspension must preserve an entered loop.** The
   continuation carries `:loop-entered?` so its phase can say where it is
   blocked without resetting committed state. Test:
   `executor-pending-test/replayed-pre-loop-pending-keeps-the-committed-prefix`.
   This remains necessary while resume reruns pre-loop work. Retaining those
   outputs instead would change the continuation and replay semantics.
   The continuation also carries caller-resolved `:records`; otherwise cold
   `:from` admission loses its producer input. The retained-painting test
   encodes and resumes a consumer with those records.

3. **The host has to be an input the program reaches.** The design's sample
   recipe omitted it from capability arguments, and its context supplied no
   caller-root access. Hiding a host in a table closure would miss recipe
   comparison and cold ownership. The landed vocabulary requires `:host`
   explicitly wherever consumed. A region retains its geometric support
   and original unit point, so membership needs no live registry or rebuilt
   point. Tests: the brush's host-edit refusal, cold resume and retained reach.
   This construction holds for the sphere; another host changes the support
   bodies, retained geometric fields, table and vocabulary. It does not
   establish a complete abstract host protocol.

4. **The reuse row omitted pickup `.5`.** `probe-4.mjs` creates reuse from its
   already edited variant. Before changing any algorithm, the client ran
   both: `.5` reproduces `a5aba6ed…`; `.25` reproduces `b39a8b14…`. Both full
   hashes are asserted in `brush-test/returned-regions-and-paintings-are-reusable-under-their-subjects`.
   This finding holds for that probe's construction; a different producer or
   pickup value requires its own result, not a loosened hash tolerance.

5. **The stack's known list includes the consulted transparent painting.**
   The coating layer knows `["kA"]`, but the whole withheld stack knows
   `["kA" "pickup-G@0"]`. This follows the compositor's contributor reading
   and is asserted by the withheld brush test. Excluding zero-color layers
   would change the meaning of contributors; it would not justify omitting
   those layers from snapshots or recipe inputs.

6. **Chart pieces are conservative bounds.** The zero-radius cap on the cut
   names west and east, while its point has one owner and membership accepts
   only the seed (`surface-region-test/chart-piece-bounds-are-conservative-at-a-cut`).
   This closes a boundary omission without painting chart copies. If a caller
   needs exact piece enumeration, it needs a different specified meaning;
   this list is not that authority.

7. **Subjects need the reads that initialized state.** The shared executor
   now includes reachable pre-loop reads in a loop result's subject and
   excludes unused pre-loop reads
   (`executor-pending-test/a-loop-subject-includes-the-read-that-initialized-state`).
   The coating reader also preserves the supplied order in its snapshot,
   independent of grant, and refuses an explicit order that omits the sole
   visible mark (`coating-test/an-explicit-order-cannot-omit-the-only-visible-mark`).
   These are repairs to the specified input/subject meaning, not extra caches.

8. **Subject admission follows §3.5, not payload authentication.** It checks
   the producer program, required record roots and output before work. A
   painting from the `.5` producer, a missing subject and a wrong output
   refuse when `.25` is declared (the retained-input brush test). Arbitrary
   edited bytes bearing a copied subject are **not authenticated and that
   integrity guarantee is untested**. If the reuse row meant that stronger
   guarantee, its check is underspecified: it would need a trusted retained
   value or producer re-execution and would change admission's inputs/work.

The retained chains are executed as data, in reverse with continuity checks,
before the root restriction and metric classification. Removing an interior
relation on a cold host stays pending; widening B's root changes the unseen
`.41` read (`coating-test/inverse-demands-use-retained-chains-and-root-restrictions`).
The compositor still owns `over`: there is one composition calculation,
including partial layers and the layers above them
(`surface-test/a-pending-layer-keeps-known-color-and-composes-the-layers-above`).
No memo, bucket or atlas was added or credited. Timings in the dump are
single-run measurements; performance or scaling conclusions are **untested**.

## Replay and what remains

From the repository root:

```sh
clj -M:test -i test/app/client/region3d/run_pure.clj
clj -M:test -m app.client.region3d.brush-wire save /tmp/softland-coating
clj -M:test -m app.client.region3d.brush-wire resume /tmp/softland-coating
clj -Sdeps '{:deps {thheller/shadow-cljs {:mvn/version "2.28.23"}}}' -M -m shadow.cljs.devtools.cli release render-verifier
node test/render_engine/run_verifier.mjs
node test/render_engine/dump_result.mjs /tmp/softland-coating-browser
```

The broader red run read `pure-namespaces` from `test/app/test_runner.clj` as
data, selected its `app.client.*` namespaces, required those 31 namespaces and
passed them to `clojure.test/run-tests`; it did not load the server runner.
The exact runner is retained at the start of [client-pure.log](receipts/client-pure.log)
for replay with `clj -M:test -i <saved-runner.clj>`.

The [Region3D README](../../../../src/app/client/region3d/README.md) maps the new
namespaces and the existing scene tree. Existing scene/placement renderers
remain the next integration boundary. General smooth or trimmed support,
domains requiring periodic branches beyond -1/0/1, a region as a clip on a
read, region algebra, a foreign distance-field slot, material, a GPU brush
runner, hardware performance, live editing and reactive store wiring are
**unimplemented or untested here**. The remaining four vocabulary slots are
the explicit omission in DESIGN-1 §9; no general support claim is made from
the sphere receipts. The contact event and older correspondence/parity
questions remain outside the design's slice B; this landing supplies no new
receipt for them.

The executor accepts a per-read grant and returns a continuation value.
`brush-wire` and the browser harness grant manually, as their executed tests
show. No session/store owner or grant scheduler is implemented. When that
owner lands, the reactive laws remain Missionary's; Electric is not a
dependency, and reactive wiring is untested in this slice.

## Ownership question closed — Sid's ruling, 2026-09-07

The saved job is the **read derivation**. The paused brush continuation
stays in the session's uncommitted store, keyed by the read it waits on,
and never becomes a row. The stroke record belongs at the edit boundary,
uncommitted before settle. The read has a request row under an idempotency
key equal to its full declared inputs as a value; the store's own runner
produces a derived row under that key for later dabs and other tools. The
frame caller grants work per frame under a budget, without a grant row.
Ownership follows the key held by a live view or a record naming it.

This supersedes both the open question at the original close and the
proposal that an expensive operation would turn the brush continuation
into a saved job. Reproduction of the tested brush state is already shown
by the eager/delayed/cold tests and receipts above. The derivation receives
custody under its own inputs; the executor's larger consumer request still
checks whether an answer belongs to a particular step.

[DESIGN-1 §12](../../production/DESIGN-1.md#12-read-derivation-custody--ruled-by-sid-2026-09-07)
records the complete adopted boundary. Session/store/frame integration is
**unimplemented and untested** in this slice; this ruling changes ownership
direction, not the recorded execution results. No ownership decision is
waiting on Sid before that integration can be designed and landed.
