from lib import Plan
p = Plan()

# --- what a read gives for "what points at"
p.between("**What points at a thing, through layers (rulings 12, 20, 41, 43).**",
          "**One moment for all the layers (ruling 13).**",
"""**What points at a thing, through layers (rulings 12, 20, 41, 43, 64).**

- A layer of the list marks the key or it does not. If it does, with the stamp S, the answer says
  `{:layer l :level-since S}`: that layer's order holds every fact under the key whose stamp is later
  than S, and no other (the part before's one sentence for both gates). If it does not, the answer
  says `{:layer l :not-marked true}`, and that layer's order gives no place. The read never reads a
  layer whole for it.
- A place is found through an order: the read looks at a place when the order of some layer that she
  may see holds it. At such a place the nearest layer that she may see and that holds a counted row
  wins, as for every word (B3). A layer holds a row there when its own order holds the place, or when
  the check of ruling 12 finds a row in its facts table. The check reads the facts table of each
  layer that is nearer than the nearest order which holds the place, marked or not.
- The place is given when the winner's value is the thing that was asked for (ruling 64). When the
  winner's own order holds the place, it is given from that order's row. When the winner holds the
  place by its check, it is given from the check's row if `rows/holds-this-value?` says that the
  row's value is the thing, and it is not given if the row says "nothing here" or holds another
  value. So a nearer fact that moves the block, or takes it away, hides it. A nearer fact that says
  the same thing, written before that layer's mark or in a layer that does not mark the key, does
  not hide it: the place comes from the nearer layer, with that layer's `:layer`, `:fact` and
  `:stamp`. It costs one digest of the row's bytes for each check that finds a row.
- What an asker must know of this, and the docstring says it. A place can come from a layer whose
  entry in `:orders` says `:not-marked`, or with a stamp that is not later than that layer's
  `:level-since`. And a place is found only through an order: a place at which a nearer layer says
  the thing, and which no order of a layer that she may see holds, is not in the answer;
  `:level-since` and `:not-marked` say for each layer what its order leaves out. So when the farther
  layer's fact at such a place moves away, the place leaves the answer, though the nearer fact still
  says the thing.

""")

# --- the one function of ruling 12
p.between("*Ruling 12's check: `sources/nearer-checks` (the one function).*",
          "*The loop ends, and its rounds are bounded.*",
"""*Ruling 12's check: `sources/nearer-checks` (the one function).* For each place that the order of
some layer of the list gives: one source S6 in each layer of standing `:yes` that is nearer than the
nearest layer whose order holds that place. A layer behind that one cannot win the place, so nothing
is read there. It is the only place where a source depends on the rows of a run. Left out, it gives
the cheap answer, the runs joined: `answer` then finds no nearer row and shows the place from its
nearest order.

""")

# --- what is given
p.between("**(4) What is given: `answer/whole` and `answer/of-places`.**",
          "## Live",
"""**(4) What is given: `answer/whole` and `answer/of-places`.** Both take the query, what `reading`
gave, and `known`. `whole` gives the answer of the section above. `of-places` gives the place-answers
of a set of places, or nil for a place that still waits. Each works out a place by one walk from the
nearest layer, over the rows that `known` holds at that moment for the sources that `reading` wants.
For a place or a run: the first layer in order, of standing `:yes`, whose value holds a counted row at
the place's key. For what points at a thing: the first such layer that holds any row at the place, by
its own order or by its check; the place is then given when that layer's order has it, or when the
check's row holds the thing (ruling 64). The walk stops at the layer that wins. A place waits when
the walk meets, before it stops, a layer whose standing is `:not-known-yet`, or a wanted source that
`known` does not hold; a source behind the layer that wins is not waited for. Neither function keeps
anything, and neither is handed anything that was put together before (ruling 26, R2).

""")

# --- trace 4
p.swap("""`multi-path` of keypaths. The answer: each place whose nearest row is in an order; `:orders""",
       """`multi-path` of keypaths. The answer: each place whose nearest row is in an order, or is a check's
row that holds `:canvas-1` (ruling 64); `:orders""")

# --- trace 11
p.between("**11. A thing that a farther layer points at and a nearer layer moves.**",
          "**12. Two askers on the same thing; one leaves, the other stays, the last leaves.**",
"""**11. A thing that a farther layer points at and a nearer layer moves, or says again.** The base:
`:block-7 :parent` is `:canvas-1`. `:team` writes `:block-7 :parent` as `:canvas-2`. Both mark
`:parent`. `[:points-at :parent :canvas-1]` through `[:team :base]`, once: `:team`'s order for
`:canvas-1` does not hold `:block-7`; the base's does; the check `(keypath ":team :block-7 :parent")`
gives a row; its value is not `:canvas-1`; `:block-7` is not in `:places`. The same read for
`:canvas-2` gives `:block-7` from `:team`. *Live, opened before the move:* the check's push holds nil,
and `:block-7` is given from the base. The move: the check's push calls back, nil to a row;
`told-by?` is true, `:team` is the nearest; `of-places` finds `:team` holding a row whose value is
another thing: `{:changed {[:block-7 :parent] {:holds :no-fact}}}`. The base's order did not change.
`:team` moves it back to `:canvas-1`. In a streaming layer the row and the order change in one event
and call back as two pushes, in no order. The check first: its row now holds `:canvas-1`, so the
place is told from `:team`'s row; then the order, which is told again from the order's row, the same
fact; the check is left, since `:team`'s own order now holds the place. The order first: the same
place-answer, the check is left, and its callback reaches no read.

*Ruling 64, the case that it changes.* `:alice-notes` does not mark `:parent`. Alice wrote
`:block-7 :parent` as `:canvas-1` there, and the base says the same. Through `[:alice-notes :base]`:
the base's order gives the place; the check in `:alice-notes` finds a row; `holds-this-value?` is
true; the place is given, `{:holds :value :value :canvas-1 :layer :alice-notes :fact … :stamp …}`,
and `:orders` says `{:layer :alice-notes :not-marked true}`. *Live:* the base then moves `:block-7`
to `:canvas-2`. The base's order calls back with the removal. `reading`: no order holds the place
any more, so the check in `:alice-notes` is left. `told-by?` looks only at the sources that the read
still holds, finds no nearer row, and the place is told: `{:changed {[:block-7 :parent] {:holds
:no-fact}}}`. That is what a read asked once gives at that moment: the place is found only through
an order. The step must tell here whatever the check held: a change that the check's push had taken
and not yet called back never reaches the read once the check is left ("Live", "Why no put-together
value is kept").

""")

# --- the pure proof and the proof against the module
p.swap("""| P6 | `sources/nearer-checks` on the case of trace 11 and on a place that a nearer order holds; then with the function put aside | Ruling 12's one function wants a check exactly where a nearer layer could hide a place. Without it the answer is the runs joined |""",
       """| P6 | `sources/nearer-checks` on the case of trace 11, on a place that a nearer order holds, and on a place that two orders hold with a layer between them; then with the function put aside. `answer/of-places` on a check's row that holds another thing, that says "nothing here", and that holds the asked thing; `rows/holds-this-value?` on a UUID, a string with spaces, a decimal with trailing zeros, bytes that are no value | Ruling 12's one function wants a check in each layer nearer than the nearest order that holds the place, and in no other. Without it the answer is the runs joined. Ruling 64: the nearest row wins, and the place is given from it exactly when it holds the thing, by the test with which a gate puts a fact in that thing's order |""")
p.swap("""| 10 | Trace 11, once: a farther layer points, a nearer moves, takes away, moves back. A nearer layer that does not mark the key. Facts written before the mark. The mark off and on | Ruling 12 as built: hidden, hidden, shown from the nearer. `:not-marked`. `:level-since` equals the stamp that the marks row holds, and no fact with an earlier stamp is given |""",
       """| 10 | Trace 11, once: a farther layer points, a nearer moves, takes away, moves back. A nearer layer that does not mark the key and writes another thing, then the same thing. A nearer fact that says the same thing and was written before that layer's mark. Then the farther layer moves the place away. Facts written before the mark with no farther order. The mark off and on | Rulings 12 and 64 as built: hidden, hidden, shown from the nearer layer's order. Hidden; then shown from the nearer layer's row, with its `:layer`, `:fact` and `:stamp`, under `:not-marked` or a `:level-since` that is not earlier than its stamp. Not in the answer once no order holds the place. `:level-since` equals the stamp that the marks row holds, and no fact with an earlier stamp is given through that layer's own order |""")

# --- not decided: the row of ruling 12
p.swap("""| "What points at a thing" through a list of layers (ruling 12, provisional, for Sid) | B3 holds: a nearer row at the place hides it | `sources/nearer-checks`: left out, the runs are joined |""",
       """| "What points at a thing" through a list of layers (rulings 12 and 64, provisional, for Sid) | B3 holds: at a place that an order gives, the nearest layer that holds a row wins, and the place is given when the winner's value is the thing | `sources/nearer-checks`: left out, the runs are joined |""")

# --- the open point becomes a settled line
p.between("5. **Ruling 12 as built: one edge, and its count of pushes.**",
          "6. **`:from-layer` when the asker may not see the start layer.**",
"""5. **Settled by ruling 64, against this point's first recommendation.** The edge was: a nearer layer
   whose own fact at a place points at the same thing, written before that layer's mark or under a
   key that it does not mark, hid the farther place and was not shown itself. It is closed: the
   nearest layer that holds a counted row wins, and the place is given when the winner's value is the
   thing; the read gives it from the nearer layer's row, which the check has in hand ("What a read
   gives"). What stays, and goes to the row for the queue (P12) with the measures' numbers: a place
   is found only through an order, so a place at which a nearer layer says the thing and which no
   order holds is not in the answer; and the count is one push for each place and nearer layer,
   10,020 in the worked case, 40,000 at the limit. Ways to hold fewer, none planned, since rulings 12
   and 64 fix the count: one push on a nearer layer whole while it is small (the check of this plan
   costed it, its paper's points); the checks of one task in a few pushes on lists of places
   (`submap` is not such a path: T9); ruling 12's other answer.
""")
p.swap("""numbers. Can it be less? The one large item is ruling 12's checks, one push for each; open point 5
names three ways to hold fewer.""",
       """numbers. Can it be less? The one large item is ruling 12's checks, one push for each; the settled
line 5 under "Open points" names three ways to hold fewer.""")

# --- the receipt
p.swap("""  a layer when it was the start layer (open point 6, with P8); the edge of open point 5 (with P12);
  open point 11.""",
       """  a layer when it was the start layer (open point 6, with P8); with P12, what ruling 64 leaves: a
  place is found only through an order; open point 11.""")
p.swap("""  (ruling 8). A live read through three layers under ruling 12 holds one push for each place and
  nearer layer.""",
       """  (ruling 8). A live read through three layers under ruling 12 holds one push for each place and
  nearer layer. Under ruling 64 a place can come from a layer that does not mark the key, when that
  layer's own fact says the thing and a farther layer's order holds the place; and it leaves the
  answer when the farther fact moves away, though the nearer fact still says the thing.""")

# --- the functions: one more in rows.clj, and the place-answer of a check's row
p.swap("""**`record/rows.clj`** — five functions that only a read needs, beside the part before's (open point 7).
Key texts are made in this file and nowhere else.""",
       """**`record/rows.clj`** — six functions that only a read needs, beside the part before's (open point 7;
ruling 66 counted five, and ruling 64 needs the sixth). Key texts are made in this file and nowhere
else.""")
p.swap("""5. `same-row?` [row-a row-b] → true when both are nil, or each field of one equals the same field of
   the other, a byte array by its content. Never throws.""",
       """5. `same-row?` [row-a row-b] → true when both are nil, or each field of one equals the same field of
   the other, a byte array by its content. Never throws.
5b. `holds-this-value?` [row value] → true when the row holds bytes and `value-digest` of them is
   `value-digest-of` of the value: the test by which a gate puts a fact in that value's order. False
   for no row and for a retract. Must hold: the value passes `act/value?`. Ruling 64's comparison.""")
p.swap("""27. `place-answer` [query layer row] and `order-place-answer` [query layer thing row] → a place-answer.
    The one place where `:writer` and `:permission` are put in.""",
       """27. `place-answer` [query layer row] and `order-place-answer` [query layer thing row] → a place-answer.
    The one place where `:writer` and `:permission` are put in. `order-place-answer` takes a row of an
    order, or a check's row that holds the thing (ruling 64); its `:value` is the thing as the asker
    gave it.""")
p.swap("""| `softland/store/record/rows.clj`, the namespace docstring | "Inputs and results, by group" gains the five functions; "Callers" gains the read path's pure namespaces |""",
       """| `softland/store/record/rows.clj`, the namespace docstring | "Inputs and results, by group" gains the six functions; "Callers" gains the read path's pure namespaces |""")
p.swap("""7. **Five functions more in `record/rows.clj`**, a file that the part before changes.""",
       """7. **Six functions more in `record/rows.clj`** (five when ruling 66 was made; ruling 64 adds
   `holds-this-value?`), a file that the part before changes.""")
p.swap("""   this part's build start on files of its own only. (a) This part's build adds the five as its last""",
       """   this part's build start on files of its own only. (a) This part's build adds the six as its last""")
p.save()
