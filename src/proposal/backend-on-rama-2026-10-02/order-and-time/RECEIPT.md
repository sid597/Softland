# Receipt: order and time apart, 14 October 2026

The change that Sid's refinements of 14 October asked for ([REFINEMENTS.md](REFINEMENTS.md), R1 and
R4), made in the session that started from that page (Opus 5.5). It is a record of what was done
and run, not a ruling.

## 1. What Sid decided

In full, with his words and their ids, in `src-store/README.md`, "Decisions of 14 October 2026 —
order and time apart". In short:

1. Order and time are apart, and order never uses the time. A stamp is the layer's count of
   decisions; the time on the act is a label. This replaces his answer of 7 October to the sheet's
   H2, "clock-time+counter", A: "the wall time and a counter, in one integer".
2. An act's stamp is not raised above the stamps that it carries ("the jump"), and no check holds a
   carried stamp against the layer's last stamp.
3. The time is on each fact's row and on the answer.
4. A thing that arrives with no time is refused.
5. Time travel is designed later.

## 2. What changed in the store

- `gate/stamp.clj`: one function, `next-stamp`: the layer's last stamp plus one, or 0. The packing,
  the 10-second rule and the jump are gone.
- `gate/decision.clj`: `refused-early` and `stamp-of` are gone; `writes` takes the time and puts it
  in the answer and in each row of a fact and of the facts by value.
- `record/rows.clj`, `record/tables.clj`: `:time` in the answer (when the thing held a usable one),
  the fact's row and the row of the facts by value.
- `door/declaration.clj`, `act.clj`: no bound above on the time (`max-time` gone) or on a carried
  stamp (`max-carried-stamp` gone); each is a whole number not below zero.
- `module/walk.clj`: step 7 counts; the time goes to `writes`.
- `door.clj`, `door/words.clj`: the door's answer carries `:time`.
- `read_path/query.clj`, `read_path/answer.clj`: a query's `:with` may ask for `:time`.
- The maps and docstrings of each, and `src-store/README.md`, with the decisions.

## 3. The proofs

The source was made by the session; the five files of proofs by four builders (Opus), one for each
file or group of small files, each from one statement of the contract. At Sid's word in the
session, a builder with a big change ran its own proof as it worked (pure, module); the others ran
none, and every proof ran once at the end. The fixture was made again just before that run.

The final run, on the working tree after the fixture was made (`runs/final-*.txt`):

| Proof | Count | Exit |
|---|---|---|
| act | 166 passed, 0 failed | 0 |
| pure | 685 passed, 0 failed | 0 |
| module | 443 passed, 0 failed | 0 |
| door | 277 passed, 0 failed | 0 |
| read path, pure | 213 passed, 0 failed | 0 |
| read path | 553 passed, 0 failed | 0 |
| run again | 46 passed, 0 failed | 0 |

The checks of the review's lockout, now that it cannot happen: an accepted act with a time far
ahead, then an honest act that carries the stamp of a fact written after it, is accepted with the
next stamp. Held in both gates by the module's proof, by the run-again proof's hand, and on the
walk in memory by the pure proof. None was run against the old source.

One check failed once in a builder's run and passed in the next and in the final run, with no
change to its file: the read path's "through three layers, a permission is given in the nearest
while the farther orders hold places", at `(every? :anew erin-later)`. A session's reading, not
verified by repeated runs: it rests on the order in which Rama's pushes arrive, since the read
path tells a place again when a push comes after a whole answer (`after-change` in
`read_path/answer.clj`); the read path compares no stamp across layers (searched).

## 4. The fixture, made again

Made on 14 October at 14:48 IST: 337 records, 488 rows (`test-store/softland/store/run_again_fixture/README.md`
says by gate and table). It was not held against the making before it; by derivation, 463 of that
making's 475 rows would differ. The fixture's page says which, and why twelve would not.

## 5. Corrections to the refinements page

- **Sid did decide the stamp's time, on 7 October.** The page says of the September bet "Sid never
  decided it", and its chain of the drift (R6, item 4) says the choice was not closed. The sheet
  of 7 October asked "Does a stamp hold the wall time?" (`../decisions/groups/g-record.txt`, H2), A
  being "the wall time and a counter, in one integer", and he answered "clock-time+counter"
  (‹f3f530b8›). His decision of 14 October replaces it.
- **R1 dropped two rules, not one.** "One more for each decision" also took away the jump above
  carried stamps, the second September promise. Sid decided it on its own (§1, 2).
- **A layer is not one partition.** A session's reply of 14 October said so («abe4782a»). A depot
  partition holds the things of every layer that hashes to it, and Rama gives a stream event no
  offset: the order is the depot's, and the count is the layer's.
- **The queue's V4 also changed:** its half on two machines' clocks fell away with the rule.
- **Words given as Sid's that he did not type.** Each was a session's text that he pasted, and is
  now said so: in `gate/stamp.clj` ("No gate reads a clock", "A number counted per layer"),
  `record/tables.clj`, `door.clj`, the store's page (twice), the 13 October table of the store's
  page, the run-again proof's quotes, and the fixture's page.

## 6. The queue

`../decisions/OPEN-QUESTIONS.md`: H2 superseded; V44 answered; V46 and V49 moot; V4 and V43 changed;
V53 new, time travel as a capability, open.

## 7. Not done, and whose

- Time travel (V53), and R5 of the refinements, the search through the queue for more questions
  that hang on one choice nobody closed: Sid's, for a later session.
- V43, which time a second send's answer shows, and V45, what the time is called: open.
- A check of `:with #{:time}` on the cluster: an idea in the read path's proof's page, not written.
- No review of this change was run. One review, given the code and not this page, is Sid's to ask
  for.
- Not committed, at nobody's word yet: the `raw-` outputs under `runs/`, and an empty file there,
  `raw-old-fixture-held.txt`, from a run that a builder stopped before it started.
