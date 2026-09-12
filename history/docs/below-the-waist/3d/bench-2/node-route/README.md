# The Node route of the Seam Bench, and the record contract of its executor

The Seam Bench (`../seam-bench.html`) is one file. Its pure functions for the sphere G, the pickup replay and the executor live here as plain scripts the bench embeds verbatim at two-space indent (`sweep-and-sphere.js`, `pickup-replay.js`, `executor.js`, `records.js`), so that every number the bench prints can be produced in Node without a browser, checked against the definer's attacks, and the bench then checked against the same suite. Run everything from the repository root.

```
node docs/below-the-waist/3d/bench-2/node-route/probe-2-3.mjs [--bench <path|ref>]   # attacks 2 and 3: 55 checks (+ custody when the bench embeds the block)
node docs/below-the-waist/3d/bench-2/node-route/probe-4.mjs   [--bench <path|ref>]   # attack 4: the brush through the executor, the reach region, the sequence, the contact ray, the coverage lane's envelope, custody
node docs/below-the-waist/3d/bench-2/node-route/probe-4.mjs --record <file.json> [--then <file.json>] [--budget <n>]   # run one record (a pasted one, a cold author's), print its summary, exit 0 when complete; --then runs a second record with the first's returned region or painting as the inputs it declares
node docs/below-the-waist/3d/bench-2/node-route/probe-4.mjs --cold < continuation-4.json           # resume a suspended brush in a new process
```

Without `--bench` a probe runs the sidecars over the committed bench's own functions (the route before the bench takes a change); with `--bench` it executes the block the bench embeds, from the bench's bytes, and checks every declaration unit of every sidecar against the embed (custody: a bench whose embedded function diverges from the sidecar fails before any number is trusted). `load.mjs` is the shared loader. `--dump` prints every number; `--write` lands the receipts beside the probe.

---

## The contract: what a record is, and what the executor runs

This is the language the executor (`executor.js`, embedded in the bench, `xRun`) accepts. It is what the composer intends to support on this bench and nothing more: the support is the sphere G alone, the capabilities are the ones in the table below, and an operation, a support, a distance or a filter the table does not declare is refused with the table printed, never guessed.

### The host: the sphere G and what is retained on it

G is a sphere of radius **R = 200 mm** (world units are metres; every length in a record is millimetres on the surface). Its chart S0 is u = R·longitude, v = R·latitude, with the cut at longitude ±180°; the **authored branch** of u runs around πR = 628.3185307179587 mm (longitude in [0, 2π)·R), which is where the retained records live. Two saved chart parameters re-describe the same sphere at later **revisions**: G@1 (S1, a = (u − 200)/2) and G@2 (S2, sheared, the merge of the two records into one domain M). A point on G is given as a **unit vector** [x, y, z] (world direction from G's centre) or, for a seed, as **[longitude, latitude] in radians**.

Two records are retained on G, bound to every revision through their own chains:

| record | mark | what | root domain | colour (premultiplied, linear light) |
|---|---|---|---|---|
| A@0 | kA | a great-circle arc 80 mm long through the seed (180°, 60°), **radius 16 mm** (the nib's radius, not its width) | u ∈ [πR − 160, πR + 160], v ∈ [120, 290] | (0.5, 0, 0, 0.5) |
| B@0 | kB | a tap, a disc of radius 24 mm centred on the arc at parameter .65, authored in its own coordinates (ξ, η) | ξ ∈ [−10, 200], η ∈ [−80, 90] | (0, 0, 0.5, 0.5) |

Their composition where both cover a point is by a saved **order**, bottom to top (`["kA", "kB"]` is kB over kA); with no order saved the composition is `needs-policy`. At G@2 the merge step of B's binding costs one unit of **work** per read (each read is charged on its own; nothing is cached across reads, so a run of four dabs at G@2 needs a grant of 1 on every read, which is what the run's grant supplies); a read that grants no work there is `pending` with the dependency named (`χ: B@0 → M@2 at the demanded chart point`). Attack 2 and 3 put these on the bench; the executor reads them through `gLocate`.

The only curve a record may address by name is **`kA/arc-AB`**, at a parameter t ∈ [0, 1] (t = .65 is the tap's centre; at t = .41 the point is inside kA and 19.2 mm from the tap's centre, but outside B's root domain, so B does not contribute there). What the host already holds is readable through the root **`host`**, so a record refers to it instead of retyping a number: `host.R` (200), `host.piR`, `host.A.seed` ([lon, lat] of the arc's seed, (180°, 60°)), `host.A.radius` (16), `host.A.rgba`, `host.A.curve` (`kA/arc-AB`), `host.A.domain`, `host.B.at` (.65), `host.B.point` (the tap's centre as a unit vector), `host.B.radius` (24), `host.B.rgba`, `host.B.domain`, `host.revisions`, `host.charts`.

### A record

A record is JSON. Its top-level keys are the roots a reference may start from, plus the program:

```
{
  "id": "<name>@<revision>",            a name for the record; not part of the recipe hash
  "what": "…",                          prose, ignored by the executor
  "support":  {"id": "G", "revision": 0 | 1 | 2},
  "coating":  {"order": ["kA", "kB"] | ["kB", "kA"] | null, …anything else is carried, not read},
  "painting": {"id": "…", "grid": [nx, ny], "domain": [u0, v0, w, h], "chart": "S0", "filter": "nearest", "initial": [r, g, b, a]},
  "tool":     {…any numbers and arrays the program refers to…},
  "events":   [{"id": "…", …any fields the program refers to…}, …],
  "inputs":   {"<name>": {"kind": "surface-region", "id": "<the region's id>"} | {"kind": "painting", "sha256": "<its bytes' hash>"}},
  "program":  {…below…}
}
```

`state` and `inputs` are optional (a record without `each` needs no state). `painting` declares a **logical painting**: a Float32 RGBA grid of `grid` texels over the patch `domain` = [u0, v0, width, height] in millimetres on S0's authored branch (u around πR), premultiplied, linear light, read by the declared `filter` (`nearest` only); `painting.initial` in a program is a fresh painting of that declaration filled with `initial`. `inputs` declares values another record returned, handed in by whoever runs this one (`--record a.json --then b.json` hands b the region or painting a returned; the bench's card hands in the last region it kept). The subject is the region's `id` or the painting's `sha256`, printed in full by every run so it can be copied into the record; the executor refuses an input that is not the declared subject (`stale`). An `id` left out, or written as a placeholder in angle brackets, is bound by `--then` to what the first record returned and printed as `bound: …`, which runs the record but turns the subject check off for that run: write the id back to keep it.

### The program

```
"program": {
  "each":   "events",                            optional: run the steps once per event, in order
  "state":  {"<field>": <reference or literal>}, the state before the first event
  "steps":  [{"out": "<name>", "op": "<operation>", "<arg>": <reference or literal>, …}, …],
  "next":   {"<field>": <reference>},            after the steps of an event: the state for the next one
  "return": ["state.<field>", "<step out>", …]   what the run hands back
}
```

A **reference** is a string whose first dot-segment (or the whole string, when it has no dot) is a root (`state`, `event`, `events`, `tool`, `support`, `coating`, `painting`, `inputs`, `host`) or the `out` of an earlier step of the same event; the segments after it walk into the value (`state.carry`, `event.t`, `tool.radius`, `picked.color`, `inputs.clip`, and a bare `at` for the step named `at`). Any other string is a literal word (`"surface"`, `"nearest"`; the word `"coating"` inside `layers`). A step's `out` shadows a root of the same name for the steps after it, so do not name a step `state`, `tool`, `coating` or another root. Numbers and objects are literals; arrays are literals too, except the `layers` argument of `read-surface`, whose elements are resolved one by one (`["coating", "state.painting"]` names the coating and the state's painting). Without `each` the steps run once and `return` names step outputs; with `each` the steps run per event, `next` rebinds state fields from the outputs, and `return` names state fields (`state.painting`). Every value keeps its subject: a region owns a copy of the record it was built from and an `id`; a painting carries its declaration, its revision (one more per paint) and its bytes; a read carries the id of the snapshot it pinned.

### The capability table

Each operation consumes exactly the arguments listed; another key on a step is reported as `not consumed`. Every heavy thing happens inside an operation; the executor only moves values and keeps the order.

| op | arguments | returns |
|---|---|---|
| `curve-point` | `curve` (`"kA/arc-AB"`), `t` | a unit vector: the point of the arc at t |
| `surface-region` | `support`, `seed` ([lon, lat] rad) **or** `point` (unit vector), `radius` (mm), `distance` (`"surface"`, the default and the only value: intrinsic, the great circle) | a **surface region**: membership by d(seed, q) ≤ radius on the surface; with `record` (the copy it was built from), `id` (64 hex, the hash of that record), `area` (mm²), `bounds` (world mm about G's centre, [min, max] per axis), `pieces` (the chart pieces the region meets: `west` and `east` by the longitudes it spans, `north` above latitude 70°; a region across the cut lists both), `saturated` (a radius ≥ πR is the whole sphere); a negative radius is refused |
| `member` | `region`, `point` | `{status: "resolved", inside, distance, radius}` |
| `read-surface` | `support`, `revision` (default `support.revision`), `point`, `layers` (bottom to top: the word `"coating"` and/or paintings), `order` (default `coating.order`), `budget` (default: the run's grant), `filter` (default: each painting's own declared filter), `pending` (`"wait"` default, or `"provisional"`) | a **read**: `{status: "resolved", color, contributors, snapshot}`, or `pending` (a binding withheld; `missing` names it; `known` lists what resolved), or `needs-policy` (no order; `candidates`), or `unsupported` (a read outside the painting's patch, a filter not declared). `contributors` lists the layers consulted (a mark whose region holds the point; every painting layer, transparent or not), not the layers that changed the colour |
| `mix` | `a`, `b`, `amount` | a·(1 − amount) + b·amount, per channel including alpha (a linear blend of premultiplied values, not a source-over) |
| `paint` | `painting`, `region`, `rgba`, `opacity` (default 1), `clip` (a region, optional) | a **new painting**, revision + 1: rgba × opacity source-over at every texel whose centre the region (and the clip) holds; `changed` counts them; the input painting is not changed |
| `sample` | `painting`, `point`, `filter` | `{status: "resolved", color, texel}` or `unsupported` outside the patch |

`read-surface` is the compositor's sample through the bindings: for the layer `"coating"` it inverts each retained binding at the support's revision, applies the root restriction, composes by the order; for a painting layer it samples the texel; the layers are composed bottom to top, premultiplied source-over. Its **snapshot** pins the support revision, the retained records' hash, the order and the chains, each painting layer's (id, revision, bytes hash), the filter and the point; the read's `snapshot` is that snapshot's hash.

### Pending, the continuation, and what commits

A step consumes only a resolved read. When a read returns `pending` or `needs-policy` the run stops **before any next state**: the state, the history and the position stay what they were, and the result carries a **continuation** (JSON: the record, the recipe hash, the request, the input state with every painting as Float32LE base64 bytes, the events still queued, the missing dependency). The **request** names the event, the position, the step, the snapshot, the input state's hash and the recipe's hash. Resuming (`xResume(continuation, grant)`) restores the state, checks it hashes to the request's, re-demands the read under the grant and continues; the eager run, the delayed run and a run resumed in a new process reach the same carry, the same painting bytes and the same history. An answer delivered for a request (`xRun(record, {answer: {request, value}})`) commits only if it belongs to the request the run reaches: a changed order, recipe or input state, another event or snapshot, or a request already consumed, is `stale` and commits nothing. `pending: "provisional"` on a read step is the declared exception: the known layers are consumed as if the missing ones were transparent, and the choice is retained in the history row with its interpretation.

The **recipe** is the record without `id` and `events`; its hash is what a continuation and a request carry.

### Running

In Node: `node docs/below-the-waist/3d/bench-2/node-route/probe-4.mjs --record <file.json> [--then <file.json>] [--budget 0]` prints the run's summary: `status`, `at`, the state (a painting as its id, revision and bytes hash), the history (per event: the read's colour, contributors and snapshot, the carry, the painting's revision and changed count), the results, and, when suspended, the continuation's request and queue; exit 0 when complete, 2 otherwise; steps with `not consumed` keys or errors are printed after. On the bench: the executor's card (paste a record, or load one of the four, choose the grant, run, resume).

### A worked example: the two-record sequence

One authored point of kA read through the retained bindings at the three revisions; the same colour at each, because the bindings are retained through the chart change and the merge. With `--budget 0` the third event is pending at B's merge step.

```
{
  "id": "sequence@0",
  "support": {"id": "G"},
  "coating": {"order": ["kA", "kB"]},
  "tool": {"curve": "kA/arc-AB", "t": 0.65},
  "events": [{"id": "G@0", "revision": 0}, {"id": "G@1", "revision": 1}, {"id": "G@2", "revision": 2}],
  "program": {
    "each": "events",
    "state": {"last": null},
    "steps": [
      {"out": "at",   "op": "curve-point",  "curve": "tool.curve", "t": "tool.t"},
      {"out": "read", "op": "read-surface", "support": "support", "revision": "event.revision", "point": "at", "layers": ["coating"], "order": "coating.order"}
    ],
    "next": {"last": "read"},
    "return": ["state.last"]
  }
}
```

Its history: `G@0: read (0.25, 0, 0.5, 0.75) from kA, kB`, the same at G@1 and G@2 (blue over red, each at half opacity).

### Values in one line each

- a unit vector: `[-0.4991, 0.8645, -0.0600]` (the arc at t = .65)
- a seed: `[3.141592653589793, 1.0471975511965976]` (180°, 60°)
- a colour: `[0.25, 0, 0.5, 0.75]` (premultiplied RGBA)
- a painting declaration: `{"id": "p", "grid": [64, 32], "domain": [588.3185307179587, 194, 80, 30], "chart": "S0", "filter": "nearest", "initial": [0, 0, 0, 0]}` (64 × 32 texels over u ∈ [πR − 40, πR + 40), v ∈ [194, 224) mm)
- a region's id and a painting's bytes hash: 64 hex characters, SHA-256, printed in full by every run
- what a record cannot say on this bench (the cold-mind attack's findings, kept as production work): a region as a clip on a read; region algebra (intersection, difference, a region from a curve span); a painting declared to cover a region rather than an authored patch; a painting named mid-event other than a step's own output
