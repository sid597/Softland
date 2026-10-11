from lib import Plan
p = Plan()
p.swap("""A layer of the list that is left out keeps its S1, and her S2 while its setting is "members": those
are the pushes that say when it may come in.""",
       """A layer of the list that is left out keeps its S1, and her S2 while it is made and its setting is
"members": those are the pushes that say when it may come in.""")
p.swap("""  that broke the first design. It is derived, not run, and the check should read it closely.""",
       """  that broke the first design. It is derived, not run, and the check should read it closely. (The
  check did: for what points at a thing the argument had a hole, where a step leaves a check whose
  change it has not handled; "Live" now has the argument as it holds, and what was played.)""")
p.swap("""| S2 | The asker's permission in the layer | facts | `(keypath (rows/place-key layer asker :softland.store/permission))` | as S1 | in the layer's call | one push for an asker and a layer, only while the setting is "members" |""",
       """| S2 | The asker's permission in the layer | facts | `(keypath (rows/place-key layer asker :softland.store/permission))` | as S1 | in the layer's call | one push for an asker and a layer, only while the layer is made and its setting is "members" |""")
p.swap("""| S6 | Ruling 12's check: does a nearer layer hold a row at a place that a farther layer's S5 gave | facts of the nearer layer |""",
       """| S6 | Ruling 12's check: which row, if any, does a nearer layer hold at a place that a farther layer's S5 gave (ruling 64 reads its value) | facts of the nearer layer |""")
p.save()
