from lib import Plan
p = Plan()

# --- the header
p.swap("""**Blocked.** Nothing.

**A ruling that cannot hold.** None. Three rulings are read""",
       """**Blocked.** Nothing.

**Changed by the check of this plan.** The check (`PLAN_VALIDATION-read-path.md`, 12 October 2026)
first applied ruling 64, which closes this plan's open point 5 the other way. It then found fifteen
faults and fixed each here; its paper gives the lines before and after. The largest: the argument
that no put-together value need be kept did not hold as "Live" worded its step 5 for what points at a
thing. It holds once two tests are made exact and a read holds one mark; no value is kept. The check
ran two things of its own (`plan-check-probes-read-path/`): "C1" to "C9" below are the cases of its
run on Rama's client, and "the model" is its play of every order of the callbacks.

**A ruling that cannot hold.** None. Three rulings are read""")

# --- which sources are wanted
p.swap("""| 2 | S2, her permission in a layer | she is signed in, and: once, as soon as the layer is in the list; live, when the layer's setting is known to be `:members` |""",
       """| 2 | S2, her permission in a layer | she is signed in, and: once, as soon as the layer is in the list; live, when the layer is known to be made and its setting to be `:members` |""")
p.swap("""| 3 | S5: the run of the facts by value in a layer | the layer's own rows are known and mark the key; and, live, its standing is as for S4 |""",
       """| 3 | S5: the run of the facts by value in a layer | the layer's own rows are known and mark the key, and its standing is `:yes`. Asked once, the standing is known by then: her permission came in the call that brought the own rows |""")
p.swap("""A layer whose standing is `:not-made` or `:may-not-see` wants nothing but its S1 and, when its
setting is `:members`, her S2: those are what could let it in later (ruling 23).""",
       """A layer whose standing is `:may-not-see` wants nothing but its S1 and her S2: those are what could
let it in later (ruling 23). A layer that is `:not-made` wants its S1 alone: a layer that is not made
holds no permission, and its making shows in its own rows.

The start layer of `:from-layer` is in the list from the first round, before its own rows are known:
its S2 and its fact source are wanted with its S1, as for a layer of a given list. So a place from a
layer whose list names two more is one call, and then two.""")
p.swap("""- While `s`'s own rows are not known, it gives nil. When she may not see `s`, or `s` is not made, it
  gives `[s]`: the list is a fact of a layer that she may not see, so the read does not use it (open
  point 6).""",
       """- While `s`'s own rows are not known, it gives nil, and `reading` takes `[s]` for what is wanted in
  that round; no answer is made from it. When she may not see `s`, or `s` is not made, it gives
  `[s]`: the list is a fact of a layer that she may not see, so the read does not use it (open
  point 6).""")
p.swap("""**(3) Which sources are wanted now: `sources/reading`.** It takes the query, `known`, the keys of the
tasks, and whether the read is asked once or kept live.""",
       """**(3) Which sources are wanted now: `sources/reading`.** It takes the query, `known`, the routing ids
of the tasks, and whether the read is asked once or kept live.""")

# --- the numbers
p.swap("""`max-checks-of-one-read`, it wants none of them, and the read gives `:serve-a-read-that-large` with
`{:what :checks-of-one-read :most 10000}`.""",
       """`max-checks-of-one-read`, it wants none of them, and the read gives `:serve-a-read-that-large` with
`{:what :checks-of-one-read :most 40000}`. Under an open read, a change that makes the checks more
than the number ends the read: `:a-run-grew-too-large`, with the same `:what` and `:most`.""")
p.swap("""After the push is open, each change is counted (step 4 of "Live"). The word is given only for a layer""",
       """After the push is open, each change is counted (step 4 of "Live"). The value that the push gives at
its opening is counted too: a run that grew between its sizing and its opening ends the opening with
the same word. The word is given only for a layer""")

# --- what is held
p.swap("""2. `:reads`: a map from a read's id to its query in the one form, its function, its lock, the sources
   that it holds, and whether it is opened. A source in a read's set is the same object as its key in
   `:pushes`, so a source is held once however many reads hold it.""",
       """2. `:reads`: a map from a read's id to its query in the one form, its function, its lock, the sources
   that it holds, whether it is opened, and whether the whole answer is owed ("Live", step 5). A
   source in a read's set is the same object as its key in `:pushes`, so a source is held once however
   many reads hold it. A source of a run across entities in a microbatch layer holds the routing id
   of its task, as it was read when the read opened.""")
p.swap("""session found no need for a kept value, and so did not stop: the one place where one seemed needed, to
tell "changed for her" from "not changed", is met by the tests of "Live", step 5.""",
       """session found no need for a kept value, and so did not stop: the one place where one seemed needed, to
tell "changed for her" from "not changed", is met by the tests of "Live", step 5.

Two held things are nearest to that line, and the check of this plan weighed both. The mark "the
whole answer is owed" is about what the asker is told; it holds no value of the tables and is never
kept fresh: it is set by one step and taken off by another. The set of a read's sources follows the
tables: for what points at a thing it holds one check for each place of a farther order and each
nearer layer. It is made again from the pushes' values at each step, by `reading`, and no answer is
worked out from it: an answer comes from `known`.""")
p.swap("""- *Leave a source, or leave whole.* The swap takes the read out of each entry; an entry with no reader
  left is taken out of the map; `leave` also takes the read out of `:reads`. After the swap, the
  leaver closes the push of each entry that it took out: `close!` when the future is given, or when
  it is given later.""",
       """- *Leave a source, or leave whole.* The swap takes the read out of each entry; an entry with no reader
  left is taken out of the map; `leave` also takes the read out of `:reads`. After the swap, the
  leaver closes the push of each entry that its own swap took out: `close!` when the future is
  given, or when it is given later. It forces the entry's delay to do so. A delay that its joiner
  had not forced yet then opens a push that is closed as soon as it is given: that is the price of
  never leaving a push open that no entry holds. `close!` returns at once and waits for nothing,
  also inside a callback of that push (run: C5, C6).
- *After a leave.* `tell!` looks, under the read's lock and just before it calls, whether `:reads`
  still holds the read; a read that was left is given nothing. A step that reads the value of a push
  which another thread's `leave` closed in that moment gets a throw from Rama's client ("Tried to
  fetch from closed ProxyState", T6); `known-of` catches it, and the step stops for that read.""")

# --- the base: the number of tasks
p.swap("""of a run on each task; a push of a run on each task of a table that a microbatch topology writes. The
number is read at each read that needs it and is not remembered between two reads, as the door
remembers no layer's depot. What was not run:""",
       """of a run on each task; a push of a run on each task of a table that a microbatch topology writes. The
number costs no call: `foreign-object-info` answers from what the client holds (run: C8, 200 calls in
0.1 ms). A read asked once reads it at each call and remembers nothing, as the door remembers no
layer's depot. A live read with a run across entities reads it when it opens, and its sources hold
the routing ids for as long as it is open. If the number of tasks changed under such a read, it would
go on watching the tasks of before, and nothing would tell its caller: the docstring says that such a
read must be opened again, and the receipt says that nothing notices it. What was not run:""")
p.swap("""3. `before?` [a b] → true when the text a is before the text b in the order in which Rama keeps a
   table's String keys. By code point, which is the order of the bytes of UTF-8. Read, not run, for
   letters outside ASCII; check 23 runs it.""",
       """3. `before?` [a b] → true when the text a is before the text b in the order in which Rama keeps a
   table's String keys. By code point, which is the order of the bytes of UTF-8, and not by Java's
   `compareTo`, which puts a letter outside the first plane before U+E000. Run on a probe's table
   (C7: a space, `!`, `z`, `ä`, U+E000, U+FFFF, U+1F600, in that order); check 23 runs it on the
   store's.""")

# --- the functions
p.swap("""17. `meaning` [query layer source-kind value] → what a layer's own rows, or a permission row, mean for
    this query, as a value that `=` compares.""",
       """17. `meaning` [query layer own-rows permission-row signed-in?] → what a layer's own rows and her
    permission row mean for this query, as a value that `=` compares: the standing; and, only when it
    is `:yes`, the list of a start layer and the stamp of the asked key's mark.""")
p.swap("""19. `of-word`: a map from each word to a function [query layer own-rows task-keys] → its fact sources
    in that layer.""",
       """19. `of-word`: a map from each word to a function [query layer own-rows task-routing-ids] → its fact
    sources in that layer.""")
p.swap("""22. `reading` [query known task-keys mode numbers] → `{:layers :standing :wanted :missing :too-large}`.""",
       """22. `reading` [query known task-routing-ids mode numbers] → `{:layers :standing :wanted :missing :too-large}`.""")
p.swap("""30. `told-by?` [query reading known source place] → true when a change of that source at that place is
    to be told to this asker.""",
       """30. `told-by?` [query reading known source place] → true when a change of that source at that place is
    to be told to this asker. It counts a nearer layer's row only in a source that `reading` wants and
    `known` holds.
30b. `after-change` [query reading known source places meant-before meant-after owed?] and
    `after-arrival` [query reading known source owed?] → `{:tell nil, {:changed …} or {:anew …}
    :owed? true or false}`: what steps 5 and 6 of "Live" do for one read, once the joining and the
    leaving of that step are done and `known` is made again. Pure, so that the pure proof can play
    every order of the callbacks on them (check P9).""")
p.swap("""33. `task-keys` [read-path] → the keys of the microbatch tables' tasks, read now.""",
       """33. `task-routing-ids` [read-path] → one routing id for each task of the microbatch tables, read now.
    Not `task-keys`: a key is the key of a fact.""")
p.swap("""38. `known-of` [read-path read] → a map from each source that the read holds, and that is given, to
    the value that its push holds now.
39. `tell!` [read change]: calls the function under the read's lock; a throw of the function ends the
    read. `end-read!` [read-path read because carries].
40. `on-callback` [read-path source new diff old]; `on-arrival` [read-path source]: the six steps of
    "Live".""",
       """38. `known-of` [read-path read] → a map from each source that the read holds, and that is given, to
    the value that its push holds now; nil when a push throws as it is read, which stops the step.
39. `tell!` [read change]: calls the function under the read's lock, if `:reads` still holds the read,
    with the thread marked for the length of the call; a throw of the function ends the read.
    `end-read!` [read-path read because carries].
40. `on-callback` [read-path source new diff old]; `on-arrival` [read-path source]: the six steps of
    "Live". They join, leave and tell as `answer/after-change` and `answer/after-arrival` say, and
    hold no rule of their own. `on-arrival` runs for each source that a step joined, when its push is
    given.""")
p.swap("""41. `read-live`. `leave`. `open-count`.""",
       """41. `read-live`, by "How a live read opens". `leave`. `open-count`.""")
p.save()
