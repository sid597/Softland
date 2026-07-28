# halo v0 — default-fail contract validation

**Final verdict: PASS after substantive amendment and full rerun.**

Validated 2026-07-29 against:

- branch `docs/current-mental-model-local`
- HEAD `f84e250624d8d9cae9552e63c20dba9bcf91d0cb`
- the only binding package document, `build/halo/CONTRACT.md`
- `build/halo/NOW.md` STANDING
- `docs/current-mental-model/decisions.md`
- current source and test files as read-only composition evidence

Starting foreign state was preserved exactly:

```text
?? probe-block-anatomy.html
?? probe-block-anatomy.png
```

No source or test file was changed. No implementation, test suite, compile,
runtime, commit, or push was opened by this validation round.

## Round history

The first V1–V5 pass was a substantive **FAIL**. The contract was amended
in place, then the whole round was restarted. The restarted composition
pass found one further substantive retry-identity hole; H5 was amended and
the whole round was restarted again. The final rerun below is the verdict.

Seven gaps were closed:

1. `matter-actor` does not carry the capability required by
   `utterance-import-request`; Jetty must enrich only the request-envelope
   actor while preserving the honest actor id/type/role in provenance.
2. The existing circulation wrapper and experience projection are
   wish-specific. Halo needs a generic `bank-reference!` over the same
   private relation request path and an honest `:halo/say` projection label;
   it must not make a Halo say look like a wish.
3. `#webgpu-canvas` is the full-screen physical canvas for sidebar, panels,
   editor, and ground. Native-menu suppression must therefore be scoped by
   synchronous `ground-active?`, not by node membership, and button 2 must
   be removed from `>mouse` before the ordinary pointer path sees it.
4. Copying the camera reservation only at its three current call sites
   would leave non-space master grammars open. Meta reservation must also
   live in material `valid-row?`, while still riding all three existing
   camera-reservation lanes.
5. Adding Halo floor rows only to `ground.cljs` would make the served
   interaction table lie. One shared floor augmenter must feed both the
   client tiers and `face_projection.clj`.
6. `:claim/facets` is an interaction claim, not the wear census. A block's
   actual v0 census is the six non-space wears returned by per-subject
   `wears-for`; the space census is its one space wear.
7. `say-id` alone did not protect the second half of a composite write
   against divergent retry after import-before-mark. The server-authoritative
   target now rides `:receipt/picked-at` in the existing import payload
   fingerprint. Same-id/same-body repairs the partial; divergent same-id
   reuse conflicts before relation append.

The correction from “exactly one derived unit” to “exactly one root
utterance unit, with existing structural subunits left intact” was swept
through H5, T5, and G1. This preserves the established
`free-cut-part :human-message` law rather than promising a false total-unit
count.

## V1 — say composition: PASS

### Existing import artery

- `src/app/server/episode.clj:654` defines
  `utterance-import-request` with one map arity at `:669`.
- The request already accepts `:actor`; `utterance-rows` separately accepts
  `actor-id`, `actor-role`, and `part-type`. The contract now pins all four
  so envelope authority and recorded custody cannot be accidentally
  conflated.
- `utterance-actor` returns a supplied actor unchanged. Its default carries
  `:object-container/import-material`; `matter-actor` is honestly only
  `{:actor/id "sid" :actor/type :human}`. The necessary capability is
  therefore a Jetty envelope concern, not a mutation of room identity.
- `server_jetty.clj:1162-1181` proves the direct non-spawning composition:
  pure `utterance-import-request`, `append-object-container-request!`, then
  `await-object-container-decision`.
- `server_jetty.clj:873-1060` is the distinct spawn law. It contains
  `current-episode!`, `record-turn!`, `cascade/react!`, `summon-argv`, and
  `note-episode-turn!`. H5 explicitly forbids all of those from say.

### Relation and custody

- `relation_kernel.clj:58-83` already registers `:references`; no relation
  enum edit is needed or allowed.
- `material_circulation.clj:179-203` is the single existing private
  asserted-edge composer and append/await path.
- `bank-gold! :205-238` is specifically wish-shaped and writes
  `{:mark/type :wish}`. Reusing it would violate label honesty.
- Human `:references` is gold by `gold-edge? :87-92`; machine/import custody
  remains non-gold by `silver-edge? :82-85`.
- The current gold-mark projection is wish-only at `:959-972`, so H5
  correctly fences the required generic `:source-unit-id` / `:mark/type`
  widening while retaining the legacy `:wish-unit-id`.

### Composite retry law

- The route must preflight both runtimes, import + await first, relation +
  await second, and report success only when both truths are queryable.
- The authoritative `subject-uid` rides
  `{:receipt/picked-at {:address subject-uid}}`. `utterance-rows` places
  the receipt in the fingerprinted source/unit payload, alongside text,
  time, and normalized actor identity/role.
- The existing object-container kernel has explicit
  `:idempotency/material-fingerprint-conflict` and
  `:import/material-fingerprint-conflict` decisions
  (`object_container.clj:537-561,1851-1962`). No new journal or write owner
  is needed.
- Therefore same-id/same-body replay repairs import-without-mark, while
  divergent same-id reuse in the room is refused before a second relation
  can be written.

### Import-owner census

The exact request-type census remains eight owners:

```text
src/app/server/episode.clj
src/app/server/rama/material_circulation.clj
src/app/server/rama/object_container/assembly_adapter.clj
src/app/server/rama/object_container/block_distiller.clj
src/app/server/rama/object_container/clojure_adapter.clj
src/app/server/rama/object_container/facet_master.clj
src/app/server/rama/object_container/markdown_adapter.clj
src/app/server/rama/object_container/transcript_adapter.clj
```

`episode.clj` and `relation_kernel.clj` remain zero-diff requirements.

## V2 — actual canvas and contextmenu: PASS

- `electric_flow.cljc:745-751` creates one `100vw × 100vh`
  `#webgpu-canvas`; the source comment records that the sidebar is GPU
  rendered and has no DOM node.
- `runtime.cljs:393-400` gives the same `node` to resize, wheel, and mouse;
  keyboard is listened for on `window`.
- `events.cljs:55-74` currently sends node `mousedown` plus window
  `mouseup`/`mousemove` without a button field or filter. The new flow must
  filter button 2 before those events reach either ground or non-ground
  pointer consumers.
- `runtime/mouse.cljs:683-698` confirms that ordinary pointer events route
  according to `ground/ground-active?`; the contextmenu observer needs the
  same live boundary.
- There is no `contextmenu`, `textarea`, or `contenteditable` occurrence in
  the client path. Focused editor blocks are canvas pixels, so the actual
  focused-text case is the ordinary picked block on this canvas, not a DOM
  textarea branch.
- H1 now pins synchronous `ground-active?` scoping: prevent default and
  emit `[:pointer/meta :complete]` only on the active open ground. The
  native menu survives elsewhere on the same canvas and outside it.
- The keyboard Context Menu key is explicitly outside v0 rather than
  accidentally claimed without a focus target or coordinates.

## V3 — reservation lanes: PASS

The current “one var, three reads” camera fence is exact:

```text
src/app/shared/space_material.cljc:68
src/app/client/workspace/ground.cljs:2261
src/app/client/workspace/ground.cljs:2327
```

The definition is `binding_material.cljc:128`; there are no other
production reads.

The amended contract requires `meta-gesture-reserved?` in all three lanes
and in material `valid-row? :182`. That fourth, universal gate is necessary
because the three camera readers alone cover the space master and instance
paths, not every non-space master grammar. Code-floor validation passes
`bindable? false`, so the four new floor rows remain legal while material
rows naming `:pointer/meta` remain refused under both frozen and strict
grammar paths.

The shared augmenter is also now a binding law:

- four sites: user hit, machine hit, fold header, space ground
- existing floor identities: attention, attention, foldable, space
- same rows into `ground.cljs:2222-2235` and
  `face_projection.clj:1018-1068`
- meta probes added to `floor-drill-probes :455`
- served rows flattened only through existing `table-rows :555`

## V4 — affected exact pins: PASS

All existing pins were found and the required same-commit re-cuts are
enumerated:

| Existing exact pin | Current location | P1 re-cut |
|---|---:|---|
| floor-reserved verb vector | `binding_dispatch_test.clj:148-152` | add only `:halo/condense` |
| durable effect set | `binding_dispatch_test.clj:153-160` | add only `:matter/say` |
| matter names/args/effects | `binding_dispatch_test.clj:162-195` | fifth name; say args `#{:master-id :subject-uid}`; durable |
| registry ↔ ground vocabulary equality | `binding_dispatch_test.clj:955-959` | both new registry names registered |
| required args for every registry name | `material_truth_test.clj:455-459` | continues to quantify over all names |
| active floor matter-free set | `material_portal_test.clj:969-975` | include say in the forbidden matter set |
| disclosed durable endpoint set | `material_portal_test.clj:979-990` | exactly deviate/activate/rollback/say; preview absent |
| briefing vocabulary | `material_portal_test.clj:997-1004` | all five matter verbs and both effect classes |
| import-owner set | `material_portal_test.clj:466-499` | remains exactly the eight owners above |

Additional focused assertions required by the amended laws are:

- all four meta floor rows appear in both the drill and served table
- material `:pointer/meta` is refused at every site/tier and in frozen and
  strict grammar paths
- right-button down/up cannot reach either pointer path
- contextmenu suppression is active-ground-only on the shared canvas
- say-request rejects blank/multiline/unknown-master/malformed-actor input
- same-id/same-body is one root + one edge
- new id is a second root + edge
- divergent same-id body/target conflicts before relation append
- import-without-mark is repaired by exact replay
- Halo mark labels are `say`, never `wish`
- episode and relation kernel files stay byte-unchanged

The current baseline counts were rechecked: 17 portal questions, seven
facet masters, three durable matter endpoints, zero preview endpoint, zero
say endpoint, and the eight import owners.

## V5 — memory/platform claims: PASS

No platform claim was accepted from prior memory alone:

- the one-phase/Fable role ruling was rechecked at
  `decisions.md:47-52` and NOW STANDING
- Rama as the only durable truth was rechecked at `decisions.md:155-170`
- custody, closed typed relations, and one-line reviewed kind growth were
  rechecked at `decisions.md:181-188` and in the relation/circulation code
- one canvas with addressable conversation material was rechecked at
  `decisions.md:224-241`, `electric_flow.cljc:745-751`, and the room-id /
  episode-object-key composition
- at-least-once deterministic writes and replay tests were rechecked at
  `decisions.md:264-269` and in the object-container conflict machinery
- seven registered masters were rechecked from
  `facet_masters.cljc:14-21` and `matter_room.cljc:57-81`
- seventeen portal questions were re-counted from
  `material_portal.cljc:149-292`

## Coherence audit: PASS

| Law | Trap coverage | Gate coverage | Manifest / fence coverage |
|---|---|---|---|
| H1 gesture and native-menu scope | T1, T6, T10 | G2, G4 | canvas, runtime, events, ground, binding grammar |
| H2 inviolable reservation and shared rows | T1, T9, T10 | G1, G2, G4 | binding, space, ground, face projection, exact pins |
| H3 derived plurality and honest census | T2, T4, T9 | G2, G4 | wears-for, facet registry, ground render |
| H4 four handle kinds and requirements | T4, T7 | G2, G4 | portal and face-wiring seams |
| H5 one durable say artery | T3, T5, T9 | G1, G2, G4 | matter builder, Jetty, episode zero-diff, circulation, tests |
| H6 heavy verbs remain room-only | T7 | G1, G4 | exact endpoint and active-floor pins |
| H7 universal honest answer / space miss | T4, T8 | G2, G4 | claim chain, `fm:space`, room registry |
| H8 pure projection except say | T3, T7, T9 | G1, G2, G4 | registry effects, kernel registrations, endpoint disclosure |

G3 is deliberately orthogonal: it guards the latency cost of the open
halo, with environment attestation before timing. Sid's first unscaffolded
wear remains a product instrument, not a build gate.

Every §11 locator was re-found on the current tree. One cut-time path had
drifted by location, not substance:
`src/app/server/server_jetty.clj` → `src/app/server_jetty.clj`. The amended
manifest records the actual path. No unresolved locator or contract/disk
conflict remains.

## P1 implementer starter

Use this as the whole next-session prompt:

> Halo v0 P1 implementation. Fresh context; use one 5.6sol-xhigh
> implementer context for the whole phase. Read the repository AGENTS.md,
> then `docs/current-mental-model/build/halo/CONTRACT.md`,
> `docs/current-mental-model/build/halo/NOW.md` STANDING, and
> `docs/current-mental-model/decisions.md`. CONTRACT is the package's only
> binding build document; there is deliberately no PLAN.md. Load `/rama`
> and `/rama-retro-lens` before touching the Rama composition. Capture HEAD
> and the exact modified/cached/untracked baseline before edits. Build all
> of P1 under CONTRACT §6 in this one context; do not split or re-plan the
> phase. Treat H1–H8, T1–T10, the exact file fence, the zero-diff episode /
> relation-kernel pins, and §9 stop clauses as binding. Run G1–G4, including
> the fresh in-phase falsifier and live isolated drive; record
> `build/halo/P1.md` receipts and a newest-first NOW entry. Do not commit or
> push. A genuine policy conflict stops for a Fable/Sid ruling; locator
> drift relocates and is logged. Fable orchestrates and independently gates
> the completed phase.

Package state after this validation: **OPEN for P1 implementation**. PASS
authorizes that one implementation context; it does not claim G1–G4,
closure, or commit authority.
