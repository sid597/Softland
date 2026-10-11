from lib import Plan
p = Plan()
p.swap("""   - *Is the whole answer owed?* It is owed from this step on when the source is a layer's own rows
     or her permission and what it means for this query differs before and after
     (`layers/meaning`, below), and when a whole value came anew and differs. It can be owed from
     an earlier step: the read holds one mark for it.""",
       """   - *Is the whole answer owed?* It is owed from this step on in three cases. The source is a
     layer's own rows or her permission, and what it means for this query differs before and after
     (`layers/meaning`, below). A whole value came anew and differs. Or this step joined or left a
     source that is no check of ruling 12, whatever source called: such a source is joined or left
     only when what shapes the read has changed in the pushes' values, and the step that sees that
     first need not be the step of the push that changed. It can be owed from an earlier step: the
     read holds one mark for it.""")
p.swap("""The setting itself is in the meaning, also for an asker whose standing it does not change, and it
must be. When a setting goes to "members", a fact's step that runs before the setting's own step
already finds the new setting in the push, joins her permission, finds her standing not known yet,
and tells nothing. If she holds a permission, her standing is `:yes` before and after. Only the
setting's own step, which finds that the setting differs, owes her the whole answer, and with it
the fact that was not told.""",
       """Why a step that joins or leaves a source owes the whole answer, and not only the step of the push
that changed. When a setting goes to "members", a fact's step that runs before the setting's own step
already finds the new setting in the push, joins her permission, finds her standing not known yet,
and tells nothing of its place. If she holds a permission, her standing is `:yes` before and after.
The fact's step owes: the whole answer comes when her permission is given, with the fact in it. The
setting's own step then finds that the setting differs and gives the whole answer once more. So no
step that tells nothing rests on a later step of another push.""")
p.swap("""30b. `after-change` [query reading known source places meant-before meant-after owed?] and
    `after-arrival` [query reading known source owed?] → `{:tell nil, {:changed …} or {:anew …}
    :owed? true or false}`: what steps 5 and 6 of "Live" do for one read, once the joining and the
    leaving of that step are done and `known` is made again.""",
       """30b. `after-change` [query reading known source places meant-before meant-after joined left owed?]
    and `after-arrival` [query reading known source joined left owed?] → `{:tell nil, {:changed …}
    or {:anew …} :owed? true or false}`: what steps 5 and 6 of "Live" do for one read, once the
    joining and the leaving of that step are done and `known` is made again.""")
p.swap("""6. *A push that an open read came to need is given.* Under the read's lock, by `answer/after-arrival`:
   step 5's first line again. Then, when the whole answer is owed, `{:anew …}` if nothing is missing
   any more.""",
       """6. *A push that an open read came to need is given.* Under the read's lock, by `answer/after-arrival`:
   step 5's first line again, which owes the whole answer if it joins or leaves a source that is no
   check. Then, when the whole answer is owed, `{:anew …}` if nothing is missing any more.""")
p.swap("""- *What shapes the read* gives the whole answer anew, from every value that the pushes hold then.
  The mark "the whole answer is owed" carries that from the step that found the change to the step in
  which the last missing source is given.""",
       """- *What shapes the read* gives the whole answer anew, from every value that the pushes hold then: when
  the push that changed calls back with another meaning, and when any step finds the change first and
  joins or leaves a source for it. The mark "the whole answer is owed" carries that from the step
  that found the change to the step in which the last missing source is given.""")
p.swap("""on a setting that goes to "members" for an asker who holds a permission |""",
       """on a setting that goes to "members" for an asker who holds a permission. `answer/after-change` on a fact's step that joins her permission, and on one that leaves an order whose mark was taken off |""")
p.swap("""That a setting which changes means another thing also when her standing stays `:yes` |""",
       """That a setting which changes means another thing also when her standing stays `:yes`. That a step which joins or leaves a source that is no check owes the whole answer |""")
p.save()
