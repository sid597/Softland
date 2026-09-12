# A cold author says two tools, from the contract alone

Written against `../README.md` and nothing else in the repository; every number below
came out of `probe-4.mjs --record` on the records in this directory.

---

## 1. The reach tool — `reach.json`

A region of surface distance 150 mm about the seed at longitude 180°, latitude 60°,
returned so another record can take it as a clip.

```
{"out": "reach", "op": "surface-region",
 "support": "support", "seed": "tool.seed", "radius": "tool.radius", "distance": "surface"}
```

with `tool.seed = [3.141592653589793, 1.0471975511965976]` and `tool.radius = 150`, and
`"return": ["reach"]`. The program has no `each` and no `state`; the steps run once and
`return` names the step out.

Run (`--record cold/reach.json`), status `complete`, exit 0:

| | |
|---|---|
| region | `6a5dcda2c043378e…` |
| radius | 150 |
| area | 67433.94227378975 mm² |
| bounds | x [−191.2321833642485, 44.8944095894843], y [58.56835361987256, 200], z [−136.32775200466682, 136.32775200466682] |
| saturated | false |
| pieces | `["east"]` |

Cross-check on the area, from the contract's own R = 200 mm and "intrinsic, the great
circle": a spherical cap of angle θ = 150/200 = 0.75 rad has area 2πR²(1 − cos θ) =
67433.94 mm². It agrees to every printed digit, so `distance: "surface"` is the cap and
not a chart disc.

### The dab through it — `reach-dab.json` (and `reach-dab-unclipped.json`)

```
node …/probe-4.mjs --record cold/reach.json --then cold/reach-dab.json
```

`inputs: {"clip": {"kind": "surface-region"}}`; the steps are `curve-point` at t = .65,
a `surface-region` disc of radius 200 mm about that point, and `paint` of
(0, 0.5, 0, 0.5) into `painting.initial` with `clip: "inputs.clip"`.

The disc's area, 115534.63190054559 mm², is again the cap formula at θ = 1 rad. The
painting is 128 × 64 over u ∈ [308.3185307179587, 948.3185307179587), v ∈ [50, 300) —
deliberately wider than the reach, because with the README's own 80 × 30 example patch
every texel is inside 150 mm of the seed and the clip would be a no-op that proves
nothing.

| run | changed | bytes |
|---|---|---|
| through the clip | 6096 | `26da35e30f3934be…` |
| no clip (`reach-dab-unclipped.json`, standalone) | 7502 | `9791eb72f5dc66f4…` |

1406 texels are the reach doing work, not a claim about it.

---

## 2. The coating-reading pickup brush — `pickup-brush.json`

Four events on `kA/arc-AB` at t = .65, .70, .41, .65; `support.revision = 2`;
`coating.order = ["kA", "kB"]` (bottom to top, so kA under kB). State before the first
event: `carry = tool.start = [1, 0, 0, 1]`, `painting = "painting.initial"` over the
declared 64 × 32 patch u ∈ [588.3185307179587, 668.3185307179587), v ∈ [194, 224),
`nearest`, transparent. Per event:

```
at      curve-point   tool.curve, event.t
read    read-surface  support, point=at, layers=["coating", "state.painting"], order=coating.order
mixed   mix           a=state.carry, b=read.color, amount=0.25
disc    surface-region support, point=at, radius=8.75, distance="surface"
painted paint         painting=state.painting, region=disc, rgba=mixed, opacity=0.5
next    carry←mixed, painting←painted     return  state.carry, state.painting
```

### With the grant (no `--budget`): `complete`, `at` 4, exit 0

| event | t | read | contributors | carry after | painting |
|---|---|---|---|---|---|
| d1 | .65 | (0.25, 0, 0.5, 0.75) | kA, kB, pickup@0 | (0.8125, 0, 0.125, 0.9375) | rev 1, changed 401, `41131182bc961ced…` |
| d2 | .70 | (0.5390625, 0, 0.328125, 0.8671875) | kA, kB, pickup@1 | (0.744140625, 0, 0.17578125, 0.919921875) | rev 2, changed 321, `98975de5ce9a3550…` |
| d3 | .41 | (0.5, 0, 0, 0.5) | kA, pickup@2 | (0.68310546875, 0, 0.1318359375, 0.81494140625) | rev 3, changed 405, `4f5c8adec8b31fbc…` |
| d4 | .65 | (0.6631851196289062, 0, 0.2650909423828125, 0.9282760620117188) | kA, kB, pickup@3 | (0.6781253814697266, 0, 0.16514968872070312, 0.8432750701904297) | rev 4, changed 401, `30cb13f2147784bdae44b4aa303d412c8980f00c09435c0983521cd291d75579` |

The final carry and the final bytes are the composer's, digit for digit and hash for
hash. d1's read is the worked example's own (0.25, 0, 0.5, 0.75) from kA and kB, and d3
is kA alone — the contract's statement that t = .41 lies outside B's root domain, seen
from the outside.

### With no work granted (`--budget 0`): `pending`, `at` 0, exit 2

```
reason      dependency withheld: [{"binding":"binding-B",
                                   "missing":"χ: B@0 → M@2 at the demanded chart point","work":1}]
state       carry [1,0,0,1]; painting pickup rev 0, c35020473aed1b46…
history     []
continuation request 2283d5b44f453fd7…, queued ["d2","d3","d4"]
```

It stops on the very first dab's read and commits nothing: the carry is still the
authored opaque red, the painting is still revision 0 of the transparent declaration,
and the history is empty — the "before any next state" clause, visible. The suspended
request hash `2283d5b44f453fd7…` is character-identical to the request the granted run
records for d1, so the thing being asked is the same thing in both runs; the queue names
the three dabs not yet reached.

---

## What I could not say from the contract alone

**1. A bare step out as a reference.** "A reference is a string with dots" plus "a string
that starts with no root is a literal word" makes `"rgba": "mixed"` a literal word, which
would be nonsense. Only the worked example's `"point": "at"` shows that a dotless step
out resolves. I followed the example over the rule. It also means a step named `surface`,
`coating` or `nearest` would silently capture a literal word elsewhere in the same
program. *Settles it:* a reference is a string whose first dot-segment is a root or an
earlier step's `out` — plus what shadowing does.

**2. References inside arrays.** `layers` has to be able to name a painting, and the only
way to name the brush's own is `state.painting`; but the contract says "Numbers, arrays
and objects are literals" and its one example, `["coating"]`, is a literal word. I
guessed elements are resolved. The run confirms it (`contributors` carries `pickup@0` …
`pickup@3`). *Settles it:* one line saying references resolve inside arrays and objects.

**3. The input's id — the sharpest gap.** `inputs` is written
`{"kind": "surface-region", "id": "<the region's id>"}` and "the executor refuses an
input that is not the declared subject (`stale`)". A cold author cannot obtain that id.
The run summary prints `6a5dcda2c043378e…` — 16 of 64 hex; the refusal prints 8 of each
side (`not the declared subject (6a5dcda2…, got 6a5dcda2…)`), so it does not even
disambiguate itself. `--dump` and `--write` would presumably print it, but neither is
part of the `--record` form. What does work — `probe-input-noid.json` — is omitting `id`
entirely: the executor binds (`bound: clip ← the region the first record returned, id
6a5dcda2…`) and skips the check. So the only handoff a cold author can write is the one
with the contract's guarantee turned off, and nothing in the contract says `id` is
optional or what dropping it costs. *Settles it:* print region ids and painting hashes in
full in the summary, or say `id` is optional and name what the check buys.

**4. What a region can be consumed by.** The table hands a region to `paint` (as `region`
or `clip`) and to `member`. Nothing else. `read-surface` takes no clip, so the reach
cannot restrict a *read*; there is no region ∩ region, no difference, no region from a
painting, no region from a curve span. "Retained as a result another record can consume
as a clip" is exactly the edge of what is sayable — one step further and there is no
word. *Settles it:* say whether region algebra is out of scope on this bench or merely
not yet in the table.

**5. `mix` on premultiplied colour.** The table's `a·(1 − amount) + b·amount` per channel
lerps alpha too. On premultiplied values a pickup could as well have meant a source-over
at 0.25, and the two disagree. I took the table literally; the composer's carry says that
was right, but the contract never says which one "mixes the sample into the carried
pigment" is.

**6. Which painting the read sees.** "the brush's own previous painting above the
coating" I read as `state.painting`, the value before this event's `paint`, because
`next` rebinds only after the steps. That is forced by the program's mechanics, not by
the layer wording — and a record has no way to say anything else: it cannot name the
painting mid-event, nor "as of two events ago".

**7. `filter` on `read-surface`.** Listed as an argument, with no default given (unlike
`revision`, `order`, `budget`, `pending`, which all carry one). I omitted it and the read
resolved, evidently taking the painting declaration's `nearest`. Whether it is required,
defaults per layer, or defaults from the record is not stated.

**8. What one unit of work is a unit of.** "the merge step of B's binding costs one unit
of work" does not say per run, per read, or per demanded chart point. `probe-step-budget.json`
writes `budget: 1` on the read step and all four dabs complete with the same final carry
and bytes, so a single read costs at most one unit. Whether the *run* needs 1 or 4 I
could not test: `--budget 1` was refused by the harness I am running under, not by the
contract, and the contract does not answer it in words. *Settles it:* one sentence on
whether the merge is charged once per chart point and cached across reads.

**9. `contributors` is "layers consulted", not "layers that coloured".** At d3 the read is
exactly kA's (0.5, 0, 0, 0.5), yet `pickup@2` is listed — the painting was transparent
there. Useful, but never said; a cold author reading contributors as provenance for the
colour would be wrong.

**10. `pieces` describes the seed, not the region.** The 150 mm reach reports `["east"]`;
the 200 mm disc about the t = .65 point — 12 mm away, on the other side of the cut —
reports `["west"]`. Both straddle longitude ±180°. The contract is honest ("the chart
piece of the seed"), which means a record near the cut has no way to ask through the
table whether its region crosses it, and `bounds` is world millimetres, not chart.

**11. The patch is the tool author's, not the region's.** `paint` writes into a painting
whose patch was authored in the record; a region much larger than the patch simply falls
off its edges, and a clip much larger than the patch does nothing. Nothing in the record
can say "a painting that covers this region". That is why the reach dab had to author a
640 × 250 patch by hand for the clip to be visible at all.

**12. What the record could not reach for.** The reach seed had to be typed as literal
radians into `tool`, even though (180°, 60°) is a fact the host already holds — it is
A@0's own seed. The only handle a record has on the retained marks is the single name
`kA/arc-AB` at a parameter; there is no root for a mark, its seed, its radius or its
colour. A tool that means "reach out from where the arc was seeded" cannot say so; it can
only re-state the number and hope it still matches.

**13. Small silences.** `state` is listed in the program without being marked optional —
`reach.json` omits it and runs. `distance` is listed with exactly one legal value and no
default, so I always wrote it. And the summary's truncation to 16 hex applies to painting
hashes too; the brush's final bytes were only readable in full because the history rows
print them unabbreviated while the results block does not.

---

## Files

| file | what |
|---|---|
| `reach.json` | tool 1: the 150 mm reach region about the seed |
| `reach-dab.json` | tool 1, second record: one dab painted through the reach as a clip (`--then`) |
| `reach-dab-unclipped.json` | the control that makes the clip's 1406 texels a measurement |
| `pickup-brush.json` | tool 2: the four-dab coating-reading pickup brush at G@2 |
| `probe-input-noid.json` | the probe behind account item 3 |
| `probe-step-budget.json` | the probe behind account item 8 |
