# Piece 3 — Statements, In-Memory Model, History Generator, and Runner

Built from [`../SPEC.md`](../SPEC.md) (which governs) and [`../IMPLICIT_SPEC.md`](../IMPLICIT_SPEC.md) alone, with Sid's call of 22 November 2026:

> A refused thing is kept whole up to 32 MiB from a sender with standing, and up to 256 KiB from a sender with none or refused at the declaration.

`PLAN.md` and `PLAN_VALIDATION.md` of Piece 3 were not opened.

## Files

- [`deps.edn`](deps.edn) — standalone deps (`org.clojure/clojure` + `org.clojure/test.check`, zero `src-store` dependency).
- [`src/piece3_statements/statements.clj`](src/piece3_statements/statements.clj) — plain-code statements of a layer's line (`L1..L6`), kept-whole byte bounds (`K1..K3`), the `:softland.store/who-may-read-acts` setting (`S1..S4`), and the read by stamp (`R1..R10`).
- [`src/piece3_statements/model.clj`](src/piece3_statements/model.clj) — small in-memory model of both gates (`:streaming` and `:microbatch`), declaration/standing/places refusal stages, permission chains (`layer`, `run`, `act`), setting changes, and the read by stamp.
- [`src/piece3_statements/generator.clj`](src/piece3_statements/generator.clj) — `test.check` history generator over both `:lab` (`:streaming`) and `:base` (`:microbatch`), covering pre-making refusals, layer making, taken and refused acts, senders with standing and with none, heavy things (`~468 KiB` between `256 KiB` and `32 MiB`), declaration refusals (`<= 256 KiB`, `> 256 KiB`, missing `:door-time`, missing/unusable `:who`), strays, wrong-gate arrivals, duplicate `act-id` resends, permission grants and revocations (including parent-chain revocations, `run-permission`, and `act-permission`), `:who-may-see` and `:who-may-read-acts` writes and invalid writes, and multi-asker read sweeps.
- [`src/piece3_statements/runner.clj`](src/piece3_statements/runner.clj) — generic target runner taking any map `{:act-id-for :send-act! :append-raw! :answer-of :read-line}` that exposes the store's door and the read by stamp, playing each history against both the target and a reference model, and checking single-page reads, pagination joins under wide (`64 MiB`) and tight (`64 KiB`) byte bounds, and malformed requests.
- [`src/piece3_statements/broken_copies.clj`](src/piece3_statements/broken_copies.clj) — 16 broken copies, each violating one statement (`L1..L6`, `K2..K3`, `S1..S2`, `R1..R8`).
- [`src/piece3_statements/run.clj`](src/piece3_statements/run.clj) — boundary probe (`1 MiB` vs `> 32 MiB` with standing, exact `262,144`/`262,145` and `33,554,432`/`33,554,433` thresholds, throwing `sorted-map` raw input), 240 generated histories on the model, and all 16 broken copies.

## Statements

| ID | Statement |
|---|---|
| `L1` | A stray gets no stamp, no answer, and never appears in the read. |
| `L2` | A thing for `L` sent on the other gate's depot is never in `L`'s line or `L`'s `last-stamp`. |
| `L3` | A second arrival under a used `act-id` gets no stamp and changes nothing of the first decision. |
| `L4` | First non-stray arrivals on `L`'s own depot take stamps `0, 1, 2, ...` with no gaps, refusals and pre-making refusals included. |
| `L5` | Every decision's answer `{:verdict :reason :stamp :time :digest}` agrees with the door's answer by `act-id` and with the decided thing's usable time and digest. |
| `L6` | A `:whole` decision carries the exact thing decided at that stamp with all of its parts (`:session`, `:based-on`, `:because-of`, `:subjects`, `:claimed-when`, `:door-time`, or raw unparsed keys). |
| `K1` | Every accepted act is kept whole. |
| `K2` | *(Sid's call)* A refused thing from a sender with standing (passed declaration and standing, refused at places) is kept whole up to `32 MiB` (`33,554,432` bytes) and not kept whole above `32 MiB`. |
| `K3` | *(Sid's call)* A refused thing from a sender with no standing or refused at the declaration is kept whole up to `256 KiB` (`262,144` bytes) and not kept whole above `256 KiB`. |
| `S1` | With no fact under `:softland.store/who-may-read-acts`, every layer — `:base` included — reads as `:members`. |
| `S2` | Only an act under the layer's first permission writes `:softland.store/who-may-read-acts`; a given permission is refused `:only-the-first-holder-writes-a-setting`. |
| `S3` | `:softland.store/who-may-read-acts` accepts only `:members` or `:everyone` on the layer's own entity, cannot be retracted, and cannot be marked. |
| `S4` | Changing `:softland.store/who-may-read-acts` governs the entire line from stamp `0` onward. |
| `R1` | A holder of a standing layer permission or a standing `run-permission` for the query's `:session` gets `L`'s `last-stamp` and every decision whole/`:too-large`; an `act-permission` grants no reading. |
| `R2` | Once a permission or any permission above it in its chain no longer stands, the asker gets nothing whole on a holder's ground. |
| `R3` | In an `:everyone` layer whose acts setting is `:members`, a visitor gets `last-stamp` and consecutive stamps: a signed-in visitor gets her own usable-`:who` decisions whole/`:too-large` and every other `:withheld` (`:stamp` alone — no `act-id`, no `answer`, no `thing`, no `:too-large`); an unsigned-in visitor gets every decision `:withheld`. |
| `R4` | In an `:everyone` layer whose acts setting is `:everyone`, any visitor (signed in or `:not-signed-in`) gets `last-stamp` and every decision whole/`:too-large`. |
| `R5` | In a made `:members` layer where the asker has no standing permission, `L` is left out (`:may-not-see`) with no `last-stamp`; a signed-in asker still gets her own usable-`:who` decisions whole/`:too-large` (with stamp gaps where others sit), and an unsigned-in asker gets no decisions (`SPEC.md` governs). |
| `R6` | In an unmade layer, `L` is left out (`:not-made`) with no `last-stamp`; a signed-in asker gets her own usable-`:who` refused decisions whole/`:too-large`, and nobody else gets anything (`SPEC.md` Q3 governs). |
| `R7` | Reading `:who` from a declaration-refused raw thing never throws and matches an asker iff `:who` is a usable id equal to the asker. |
| `R8` | A decision that the asker may read whose thing was not kept whole or exceeds the read's byte bound does not stop the line: it gives `{:stamp :status :too-large :act-id :answer}` with no `:thing`, distinct from `:withheld`, and `next-stamp` advances past it. |
| `R9` | For an asker who may see `L`, each page's stamps are consecutive from `start` with none above `last-stamp`; for every asker, paging from `0` via `next-stamp` terminates and joins with no duplicate and no omission. |
| `R10` | Malformed read inputs never throw and return `{:could-not ...}`. |

## Command and Output

```bash
cd src/proposal/runtime-2026-11-21/store-piece-3/statements && clojure -M:run
```

```text
PASSED Boundary probe (1 MiB kept whole with standing, >32 MiB :too-large without stopping line, exact 256 KiB & 32 MiB thresholds, throwing sorted-map safe).
PASSED Model check over 240 generated histories (seeds [1 2 3]): 0 breaks.
CAUGHT Broken copy stray-gets-a-stamp                     (L1: gives a stray a stamp in the layer's line) -> at history #0 by :L5/door-answer-agreement
CAUGHT Broken copy other-gate-mixed-in                    (L2: mixes a thing sent on the other gate's depot into L's line) -> at history #0 by :L4/consecutive-stamps-from-zero
CAUGHT Broken copy resend-overwrites-thing                (L3 / L6: a second arrival under a used act-id overwrites the kept thing) -> at history #0 by :L6/exact-decided-thing
CAUGHT Broken copy standing-refusal-capped-at-256kib      (K2 (Sid's call): caps a refused thing from a sender with standing at 256 KiB instead of 32 MiB) -> at history #0 by :K2/refused-with-standing-32mib
CAUGHT Broken copy no-standing-refusal-kept-past-256kib   (K3 (Sid's call): keeps a >256 KiB refused thing from a sender with no standing whole) -> at history #1 by :K3/refused-no-standing-or-declaration-256kib
CAUGHT Broken copy declaration-refusal-kept-past-256kib   (K3 (Sid's call): keeps a >256 KiB declaration-refused thing whole) -> at history #9 by :K3/refused-no-standing-or-declaration-256kib
CAUGHT Broken copy base-acts-open-by-default              (S1: defaults :base's :who-may-read-acts to :everyone instead of :members) -> at history #10 by :R3/open-layer-visitor-members-setting
CAUGHT Broken copy given-holder-writes-acts-setting       (S2: allows a non-first holder to write :softland.store/who-may-read-acts) -> at history #81 by :L5/door-answer-agreement
CAUGHT Broken copy visitor-reads-taken-act-whole          (R3: gives a visitor of an :everyone layer another sender's taken act whole while acts setting is :members) -> at history #0 by :R3/visitor-reads-taken-act-whole
CAUGHT Broken copy withheld-leaks-answer                  (R3: includes :act-id and :answer on a :withheld decision) -> at history #0 by :R3/withheld-leaks-fields
CAUGHT Broken copy open-layer-skips-permission-read       (R1 / R4: skips reading the asker's permission in an :everyone layer, treating holders as visitors) -> at history #0 by :R1/holder-or-open-acts-withheld
CAUGHT Broken copy act-permission-grants-line-read        (R1: treats an :act-permission holder as holding a permission to read the line) -> at history #6 by :R3/open-layer-visitor-members-setting
CAUGHT Broken copy ended-chain-still-reads-whole          (R2: checks only the asker's own permission row without checking that permissions above it still stand) -> at history #101 by :R5/closed-layer-own-things-and-left-out
CAUGHT Broken copy closed-layer-hides-own-things          (R5 / R6: omits a signed-in asker's own taken and refused things in a :may-not-see or :not-made layer) -> at history #0 by :R9/consecutive-and-pagination
CAUGHT Broken copy closed-layer-leaks-stamps              (R5: leaks :last-stamp and withheld stamps of others' decisions in a :may-not-see layer) -> at history #0 by :R5/closed-layer-leaks-last-stamp
CAUGHT Broken copy too-large-stops-line                   (R8: stops returning entries when encountering a decision whose thing is :too-large) -> at history #0 by :R8/too-large-stops-line

Summary (33006 ms): boundary=true, model-histories=240 (breaks=0), broken-copies=16/16 caught.
```
