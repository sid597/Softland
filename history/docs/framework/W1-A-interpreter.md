# W1-A — the interpreter + golden harness (phase artifact)

2026-07-11 · lane W1-A (Opus 4.8 subagent, dispatched by the Fable orchestrator).
Builds CONTRACT §4-§6 lane A: the two-stage interpreter, the JVM golden harness,
and the Outline face fixture. Gates G1-G6 green in-context (49 assertions, 0
fail). Fence held: new files only; `rect_tree.cljc` read-only (required, not
edited).

## Files

- `src/app/client/workspace/face_assembly.cljc` — the interpreter.
- `test/app/face_assembly_test.clj` — golden harness + stubs + G1-G6.
- `test/app/fixtures/faces/outline.edn` — the Outline assembly (grammar v0).
- `test/app/fixtures/faces/outline-conversation.edn` — committed data fixture.
- `test/app/fixtures/faces/outline.golden.edn` — committed rt-tree snapshot.

## What was built (CONTRACT §4-§6)

**`compile-assembly` (registry, assembly-edn) -> compiled** — wear-time, pure,
TOTAL (never throws). Validates V1-V7, resolves prims against the registry,
closes over the builder graph once into a data-only PLAN (builders pre-resolved,
trap T3). On any validation failure returns an error form whose apply renders
the §4 error card.

- V4 THE GUARD is a mechanical post-read walk (`scan-guard`): the first list,
  symbol, set, or char anywhere in the form rejects with a path (trap T2). No
  logic in data, ever.
- V1 envelope: `:assembly/name` (string) + `:assembly/grammar` (= 0) + `:root`
  required; unknown NAMESPACED keys tolerated (Wave-2 forward-compat), unknown
  plain keys reject.
- V2 closed node key set `#{:prim :props :children}` XOR `#{:each :template}`;
  exactly one of `:prim`/`:each`. V3 `:prim` must resolve to a **function** in
  the registry (fn? check, not mere presence — this is what makes the error
  path registry-independent). V5 props keys keyword, values literal-or-bind;
  nested binds reject. V6 children vector / each path non-empty keyword vector /
  template one node. V7 node cap 1000.

**`apply-assembly` (compiled, data, view-ctx) -> rt-tree** — data-change-time,
pure, no atoms (caller owns caching, trap T9). Post-order walk (children built
before parents, passed as already-built rt-nodes — §6 interface), then ONE
`resolve-layout` arrange pass (measure in the primitive, arrange in the engine —
trap T7), then stamps the root `:data`:

- **Δ1 carry (trap T10):** `(:view-instance, :address)` from birth — Δ3 store
  compliance becomes a rename, not a rework.
- **apply-report:** `{:items-without-id n :binds-missing n}` — index-fallback
  each items and nil-resolving binds are counted; the map must not lie about
  degradation.
- **`:assembly/content-h`:** the measured bottom-up root height (the §5 pane
  scroll contract — scroll rides the camera, tree stays scroll-independent).

**Ids (deterministic):** root = `[view-instance assembly-name]`; child ids
extend the parent by structural segment; an `:each` item's segment is its `:id`
data value when present (id travels with the item across reorders, trap T6),
else its index (legal, counted). A bound `:props {:id ..}` replaces the derived
segment.

**Error card (registry-independent, trap T4):** `error-card-node` is built into
this namespace — a corrupted registry cannot corrupt the error path. Renderable
(`tree->rects`/`tree->text-ops` succeed); carries name + grammar + address +
first <=5 errors, each with a form path. Also covers: unknown prim, malformed
assembly, `:each` over a non-sequence, an `:each` standing alone as root, and a
builder that throws (defensive try/catch) — all resolve to a visible card,
never a black screen (L13: no `try` upstream).

**Builder-fn interface (§6, fixed):** `(fn [ctx props children] -> rt-node)`,
`ctx = {:id :view-instance :address :geom :prim}`. Tested against 3 local stub
primitives (`:stack`, `:card`, `:text-run`) — the interface is contract-fixed so
these goldens stay valid against lane B's real vocabulary. Stubs follow the
measure rule and one-wrap text (no `:text-layout` — the dead hook, PROBE
Finding 1).

## Gates (G1-G6) — all PASS in-context

Run: `clj -M:test -e "(require 'app.face-assembly-test) (clojure.test/run-tests 'app.face-assembly-test)"`
→ **6 tests, 49 assertions, 0 failures, 0 errors.**

- **G1 golden walk** — Outline fixture + committed data → compile → apply == the
  committed `outline.golden.edn`; the golden flattens to rects + text ops; all
  heights positive.
- **G2 the guard bites** — one test per rejection class: (a) list, (b) symbol,
  (c) unknown node key, (d) both `:prim`+`:each`, (e) nested bind; each rejects
  at compile into an error-card builder with the expected error message.
- **G3 error-card totality** — malformed + unknown-prim + **corrupted-registry**
  assemblies each apply to a valid renderable error card carrying name +
  address + <=5 errors; nothing throws; the corrupted-registry case proves
  registry-independence.
- **G4 two-stage + Δ1 carry** — apply is pure (equal trees for equal inputs);
  one compiled builder over two data contexts yields two correct differing trees
  with no recompile; root `:data` carries `(view-instance, address)` +
  apply-report + content-h.
- **G5 `:each` id discipline** — nested each descends context; item `:id` drives
  node ids and they travel with items across a reorder; missing ids fall back to
  index AND are counted (`items-without-id`).
- **G6 `:bind` discipline** — paths resolve against the descended context; a
  missing path yields a nil prop counted in `binds-missing`; a bind ref at node
  position rejects.

## Design notes / flags for W1-INT

- **Width flow is thread-unchanged.** The interpreter threads geom down
  unmodified; a builder defaults its width to `(:content-w geom)` (or
  `:props :w`). Because children are built post-order (before parents), the
  interpreter deliberately does NOT narrow child content-w — that would couple
  it to a primitive-specific padding convention. Indentation in the stubs comes
  from `:layout :padding` x-offset, not width shrink. **Flag for lane B / INT:**
  real content-w narrowing for indentation (e.g. `:indent-rail`) needs the
  geom threaded at descent, so if a real face needs shrinking child text-wrap
  width it must be a primitive that the interpreter recurses THROUGH with a
  narrowed geom — a genuine open question the Outline face does NOT hit
  (padding-offset indentation suffices; "belonging is indentation" is realized).
  Not a stop-clause; recorded so INT can decide the mechanism when a real face
  demands it.
- **`compile-assembly` arg order is `(registry assembly)`** per CONTRACT §5 (the
  probe had them reversed; the contract wins). W1-INT registry wiring: build the
  plain map `{prim-kw -> builder-fn}` from lane B's `face_primitives.cljc` and
  pass it first.
- **`error-card-node` and the introspection helpers (`error?`,
  `compile-errors`) are public** for the INT mount + any error surfacing.

## Probe (harvest/discard)

**DISCARDED the probe code; harvested nothing verbatim.** `face_probe.cljc` used
a different builder interface `(fn [props children geom id])` and lacked the full
guard/validation/Δ1/apply-report; its ARCHITECTURE (two-stage compile/apply,
`:each`=mapv, `:bind`=get-in, error-card-not-throw, measure-in-primitive) was
already lifted into CONTRACT §5/§6/§10 via PROBE.md, and this lane reimplemented
it against the contract's fixed interface. Both probe files deleted (they were
uncommitted evidence; PROBE.md carries the record).
