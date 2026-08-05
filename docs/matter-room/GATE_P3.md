# matter-room — P3 slim gate

2026-07-28 · fresh Fable gate over the final uncommitted P3 tree · base
`20aeca8` (P3-F2 ruling commit; P2 code `50d2da7`).

## Verdict

**PASS — P3 is closed at the slim tier.**

G6, G7, and G10-machine are green: the focused suite was independently
re-run, the full nine-file diff was read against every contract-named trap,
and the contract-critical live receipts were independently re-driven on an
isolated current-tree server. Three non-blocking findings are recorded below
with their cheap falsifiers. P4 is not opened by this verdict. Staging and
both commit decisions remain Sid's.

## Scope and fence

- HEAD reverified at close: `20aeca8`; index empty throughout; nothing
  staged, committed, or pushed.
- Final code/test diff: exactly the amended PLAN §P3 seven-source-plus-two-
  test set (P3-F1 admitted `material_portal.cljc` for the T9 sentence;
  P3-F2 admitted exactly four inert `{:invoke (fn [_] nil)}` registrations
  in `ground.cljs` — the diff contains precisely those four and nothing
  else there). Diffstat: 9 files, 798 insertions, 21 deletions;
  `git diff --check` clean.
- Conditional P6 owners (`material_truth.clj`,
  `object_container/facet_master.clj`) and forbidden files (`cascade.clj`,
  `binding_material.cljc`) are exact zero-diff. `env.clj` was never read.
- Foreign write-set (multi-cascade docs, decisions.md, next-prompt.md,
  vision/LOG.md, probe-block-anatomy.*) untouched and unabsorbed.
- The four matter verbs enter the canonical vocabulary through
  `verb-registry/names`' derivation from the `verbs` map; the
  registry/implementation equality invariant holds with the four inert
  kernel registrations — P3-F2 executed exactly as ruled.

## Independent gate evidence

### Focused post-falsifier suite — re-run this session

Nine namespaces (the implementer's exact set, `clojure -M:test` explicit
runner form):

```text
103 tests · 1,600 assertions · 0 failures · 0 errors
```

CLJS `:dev` compile re-run: 272 files, 0 compiled (artifacts already
current), 0 warnings.

### G6 — PASS

Suite re-run covers: room-id → exactly one server-derived master;
byte-identical human/resident briefings; the POSITIVE content assertion
(briefing bytes contain the anchor's `:entity/id`); forged client
master/entity/wearer coordinates ignored (`authoritative == resident-open`
under a fully forged portal-open); non-room conversations retain P8 block
narrowing. Diff spot-check: `resident-portal-open` tries the room's
server-authoritative reverse table first and falls back to the unchanged
P8 path; `matter-room/narrowed-portal-open` reads ONLY `:conversation-id`.
`portal-briefing`'s param map gains `:master-id` (the F3 five-key-map death
point, fixed as planned).

### G7 — PASS

- Suite re-run covers: the durable cycle through the existing owners
  (`import-candidate!` / `activate!` / `deviate!` / recovery-offer
  rollback), malformed candidate → error card with activation errors +
  worn revision byte-unchanged, forged non-offer rollback → refused with
  no state movement, served active bindings matter-verb-free, endpoint
  disclosure exactness (all and only three durable routes, no
  `/api/matter-room/preview`), registry declaration honesty (required
  args, effect classes, v1-accepts/v2-refuses executable), the
  `:durable-via-request` exact-set pin re-cut naming both lanes, T9
  disclosure tokens.
- Live, independently re-driven this gate on isolated servers:
  - **Preview immobility** (:8094, real headless client): server portal
    23,826 bytes (request-token stripped) byte-identical before/during/
    after a real `__portal.preview` that visibly wore
    `preview:fm:attention:-871484168` over `code-floor:fm:attention:v3`;
    `endPreview` restored `code-floor:fm:attention:v3`. No server request
    exists on the preview path.
  - **Durable product cycle** (fresh bootstrapped isolated runtime,
    through the exact jetty functions): deviate → accepted
    (master-candidate), activate → accepted, rollback at the served
    recovery offer → accepted (`:recovery-offer` branch), final active
    returned to the pre-cycle revision.
  - **Forged rollback live** (:8094 HTTP): 400,
    `:matter/recovery-offer-not-found`, not accepted.
- Falsifier structural claims spot-checked in the diff: zero new
  import/append/depot/topology/import-request constructors in the seven
  source files; owner-call census exactly one `material-truth/deviate!`,
  one `import-candidate!`, two `activate!`; `matter_room.cljc` is a pure
  seam (normalization + reverse table only); `face_wiring` posts only the
  three disclosed routes and delegates preview to the existing
  `__bindings` membrane.

### G10-machine — PASS

Environment attestation first: temporary current-tree `:8094` server,
`LAND_CLUSTER=0 LAND_PINNED=1`, fresh-profile headless Chrome 150; WebGPU
adapter `vendor google · architecture swiftshader` (CPU rasterizer;
`isFallbackAdapter` reported false but the architecture string is
decisive). The bar was cleared under CPU rendering — the conservative
direction; the implementer's 16.3ms receipt attested a real adapter on
their run.

Portal health at the anchor with the room serves live: `errors=[]`,
`found=true`, `entity=fm:attention`, room mapping
`{a983e774-33f1-384f-aa7f-3cf319fd7c75 fm:attention}`, experience
material-ids widened to 3 (master + the fresh runtime's two residents —
matching the implementer's fresh-runtime observation).

Real ground edit echo:

```text
n=43 · p50=15.3ms · p95=18.0ms · p99=28.4ms · max=28.4ms
slow echoes >52ms=[] · inflight=0 · bar: p95 < 52ms
```

The temp server and browser were stopped; no listener remains on `:8080`
or `:809x`. The shared conductor was down throughout and was never
started.

## Findings — non-blocking, cheap falsifiers named

1. **Mute error card on decision-path rejections (P3-owned seam).**
   `completed-matter-act` reads `:reason`/`:errors` at the result's top
   level; those are populated by the owners' PRE-validation path only. A
   decision-layer rejection carries them inside `:decision` — and on a
   virgin runtime the decision itself can be nil — so the served card is
   `{:error nil :errors []}`. Reproduced live and in a controlled JVM.
   G7's letter stands (its malformed-candidate clause rides the
   pre-validation path, which cards honestly, suite-pinned). Cheap fix:
   also read `[:decision :reason]`/`[:decision :errors]` and name a nil
   decision honestly. T9's honesty family — a mute card is a map that
   says nothing.
2. **Unseeded-master wedge, exposed through the new lane (machinery
   pre-existing — P6 owners zero-diff).** On a runtime without the
   deploy-time ingest, deviate-before-bootstrap accepts the candidate but
   every subsequent activation fails (mute, per finding 1) — and
   `ensure-master!` cannot repair it afterward because a present latest
   revision skips its pointer-creating v0 import (read from source;
   consequence confirmed live: a post-hoc drill seed did not un-wedge
   `:8094`). The durable cluster is unaffected (ingest ran at deploy);
   `LAND_CLUSTER=0` dev boots do not seed masters and can reach this via
   `__portal.deviate` in one keystroke. Falsifier: fresh dev boot →
   `__portal.deviate` → `__portal.activate`. Fix candidates sit outside
   P3's fence (act-lane `ensure-master!`, or master seeding in the IPC
   boot) — the next contract's call.
3. **Vacuous negative assertion in the T9 disclosure test.** It asserts
   the absence of a string that never existed in the file; the actual
   retired sentence ("This portal is read-only. …") is not pinned absent.
   Substance verified in this gate: zero "read-only" occurrences remain
   in `material_portal.cljc`. Cheap fix: pin the real retired sentence.

Residue carried from the phase (unchanged): absolute grammar refusal
needs a v2 strict entry on `fm:space`; Sid's headed G10 half completes at
wear; kondo remains cache-starved in this environment.

## Receipt coherence

`P3.md`, this gate, the source/tests, and the live output agree on: the
seven-plus-two file set; suite 103/1,600 (exact); CLJS 272/0 warnings;
room id `a983e774-…` (P1/P2's); G10 n=43 under the bar on both rigs (16.3
theirs, 18.0 this gate); preview immobility held on both rigs (their
23,268 vs this gate's 23,826 bytes — different runtime state, the
immobility is the claim); their live durable cycle on a bootstrapped
isolated runtime reproduced here through the same jetty functions; their
disclosed `__bindings.served()` timeout was not used as evidence on
either rig. No receipt claims the durable cluster was driven; it never
was.

## Close

P3 is proven at CONTRACT §8's slim tier and may be committed only by
Sid's ruling. P4 must begin in a fresh context; this PASS does not
authorize it.
