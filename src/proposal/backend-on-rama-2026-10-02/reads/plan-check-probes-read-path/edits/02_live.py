from lib import Plan
p = Plan()

# --- steps 5 and 6, and the argument
p.between("5. *For each read that holds the push*, under that read's lock, with `known` made of the values that",
          "**What a live read follows beside the facts (ruling 23), and what each change does.**",
"""5. *For each read that holds the push*, under that read's lock. A read that is not opened yet does
   nothing here: `read-live` reads the pushes' values itself under the same lock ("How a live read
   opens", below). For an opened read one pure function says what the step does,
   `answer/after-change`, and the code of `read_path.clj` only does it. In this order:
   - `sources/reading` again, on the values that the read's pushes hold now. A source that is now
     wanted and not held is joined; one that is held and no longer wanted is left. Then `known` is
     made again, of the values of the sources that the read holds after that and that are given. A
     source that this step left is not in it. A source that this step joined is in it only when
     its push was open already, for another read.
   - *Is the whole answer owed?* It is owed from this step on when the source is a layer's own rows
     or her permission and what it means for this query differs before and after
     (`layers/meaning`, below), and when a whole value came anew and differs. It can be owed from
     an earlier step: the read holds one mark for it. While it is owed, no single place is told.
     When nothing is missing, the function gets `{:anew (answer/whole …)}` and the mark is taken
     off. When something is missing, the mark stays, and the step in which the last missing source
     is given gives the whole answer.
   - *Else the source is a fact source.* For each place that is left from step 3, `answer/told-by?`
     says whether this change is to be told: the source's layer has the standing `:yes` for this
     asker, the row counts (ruling 11), and no nearer layer of standing `:yes` holds a counted row
     at the place in `known`: in its run, in its order or by its check. Then the place is worked out
     with `answer/of-places`; a place that waits is left out. The places of the checks that this
     step joined and whose pushes were open already are worked out with them. What is left goes to
     the function as `{:changed {place place-answer}}`.
6. *A push that an open read came to need is given.* Under the read's lock, by `answer/after-arrival`:
   step 5's first line again. Then, when the whole answer is owed, `{:anew …}` if nothing is missing
   any more. Else, when the source is a check of ruling 12, `{:changed …}` for its place, unless the
   place still waits. A read that no longer holds the source does nothing.

`layers/meaning` gives what a layer's own rows and her permission row mean for this query, as a value
that `=` compares: the layer's standing for this asker; and, only when that standing is `:yes`, the
list when it is the start layer, and the stamp of the asked key's mark. Step 5 calls it with the old
value of the push that called and with the new one, each beside what `known` holds of the other. So
a setting that is written again with the same word gives nothing. And a list or a mark that is written
in a layer that she may not see gives her nothing: not even a whole answer that says what it said,
which would tell her that something of that layer was written.

**How a live read opens, and what its asker gets first.** `read-live` makes the read's entry, not
opened, and goes round: `reading`, join what is missing, wait for the joined pushes, again. One bound
holds for the whole call, `:wait-ms`. It holds no lock while it waits: a callback that waited for the
read's lock would hold a thread of Rama's client, and the opening of a push can need that thread
(run: C4 and C5 of the check's probe). When nothing is missing it takes the read's lock, reads the
pushes' values and asks `reading` once more. If a change that came meanwhile made a source wanted, it
lets the lock go and goes round again. Else it makes `:now` with `answer/whole`, marks the read
opened, lets the lock go and returns.

A callback that comes while the read opens takes the read's lock, sees that the read is not opened,
and does nothing. Its change is not lost. A push holds its new value before its callback runs (run:
C1), and `read-live` reads the values under the same lock. So either the callback had the lock first,
and `:now` holds the change; or `read-live` had it first, and the callback then finds the read opened
and tells the change. An asker gets `:now` and then every later change. A change that fell in the
opening can come once more after `:now`, with the answer that `:now` already holds. None comes that is
older than `:now`.

One thing the read path cannot order. The function can be called, on Rama's thread, before
`read-live` has handed `:now` to its caller's thread: the lock is let go before the call returns. A
caller that keeps a screen puts `:now` in place before it applies a change; its function may wait
for that. The docstring of `read-live` says so.

**Why no put-together value is kept, and why the asker still ends at the newest state.** A step works
from the values that the pushes hold when it runs, and from the old and the new value of the one push
that called. It does not compare with what the asker was told before, because the read path does not
keep that. A push holds its new value when its callback runs, and another push of the same row can
still hold the old one at that moment (run: C1, C2). So two steps that cross each see the other's
change or do not, and each test that lets a step say nothing must be safe either way.

Three tests let a step say nothing of a place. Each is true whatever the push that called holds.

- *The row is the same* (step 3).
- *A layer's own rows mean the same* (`layers/meaning`).
- *A nearer layer that she may see holds a row at the place now*, in a source that the read still
  holds after this step. Then the answer at the place does not rest on the layer that called: the
  walk stops before it. And the nearer source's own change is told by its own step, now or later,
  because the read still holds it. A row of the facts table never goes away. An order that loses the
  place calls back. And a check is left only by a step after which the answer no longer rests on it:
  no order holds the place any more, and that step tells the place (next line); or a nearer layer's
  order has come to hold the place, and that layer wins before the check's layer is reached.

Three things are told with no such test, because a test there could hide a change for good.

- *A place whose sources this step changed.* When an order loses a place and no order holds it any
  more, the step leaves the checks at that place. A change that such a check's push has taken and not
  yet called back then never reaches the read. So the step counts only sources that it still holds,
  finds no nearer row, and tells the place: not in the answer. When an order comes to hold a place,
  the checks nearer than it are joined, and the place is told when the last of them is given.
- *A check that arrives* tells its place (step 6).
- *What shapes the read* gives the whole answer anew, from every value that the pushes hold then.
  The mark "the whole answer is owed" carries that from the step that found the change to the step in
  which the last missing source is given. It is the one thing that a read holds between two steps
  about what its asker is told. It is no value of the tables: it is true or false.

Why the last thing that is told at a place is then the answer at the end. Take a place, and the last
step that told it, with the values that it used and the layer at which its walk stopped. A later step
of a source at that layer, or nearer, cannot have said nothing: the sources nearer than it hold what
they held, so no nearer layer holds a row at the place, and a source that was joined since is told
when it arrives. So every later step at that place is of a source behind the layer that won, or of a
source that the read had left, and the answer does not rest on those. The one way in which it could
rest on a farther source is ruling 64's: a nearer check's row that holds the thing counts only while
some order holds the place. That ends with the step that leaves the check, which tells. The read's
lock puts the calls of one asker's function in the order in which they were worked out.

Derived, and played, and not run on Rama. The check's model
(`plan-check-probes-read-path/model/crossing.py`, its log beside it) plays these steps for what
points at a thing in every order in which the pushes can take their values, call back, be served and
arrive: 13 scenarios through two, three and four layers, and every list of up to three writes on one
place in two layers, 672 lists and 22,906 states. With the steps as above, the asker's screen at the
end equals what a read asked once gives, in every order. With `told-by?` counting a check that the
same step has just left, 118 of the 672 lists have an order that ends wrong: the asker keeps a place
that the tables no longer give. With `{:anew}` given by the kind of the source that arrived, and no
mark, a layer that joins over a farther order never gives its own places. With a place waiting for
every wanted source, and not only for those before the layer that wins, a case of four layers ends
wrong. A comparison of "before" and "after" with the other pushes as they are now is wrong too: when
a nearer layer gets a row at a place in the same moment in which a farther order loses that place,
each change alone looks like no change. The build's pure proof plays the same orders on the real
functions (check P9).

The cost of telling without a test: a place can be told a second time with the answer that it
already had.

""")

# --- the design in short said the comparison that "Live" rejects
p.swap("""   for the places that the diff names, what the read gives from the values that the pushes hold at
   that moment, and calls the asker's function when that differs from what the diff's old value gave.
   It keeps no value of its own.""",
       """   for the places that the diff names, what the read gives from the values that the pushes hold at
   that moment, and calls the asker's function unless one of three tests shows that nothing changed
   for her ("Live"). It keeps no value of its own.""")

# --- the calls
p.swap("""read-live   a query, and a function of one argument -> {:now an answer, :leave-with a handle},
            or a word. After it returns, the function is called with each change""",
       """read-live   a query, and a function of one argument -> {:now an answer, :leave-with a handle},
            or a word. The function is called with each change that comes after `:now` was made""")
p.swap("""- `:changed` comes for a fact that changed in a layer of the read. It holds the places that the change
  names and that the changed layer wins. A place that the read gave and no longer gives holds
  `{:holds :no-fact}`. When two changes cross, a place can come a second time with the answer that it
  already had; it never comes with an answer that is not the newest.""",
       """- `:changed` comes for a fact that changed in a layer of the read. It holds the places that the change
  names, each with what the read gives there now; a place at which a nearer layer holds a row is not
  among them. A place that the read gave and no longer gives holds `{:holds :no-fact}`. When two
  changes cross, a place can come a second time with the answer that it already had; it never comes
  with an answer that is not the newest.""")
p.swap("""- `:ended` comes when the read path can no longer keep the read right: Rama ended a push, a run grew
  past the build's number of places, a read that the open read came to need failed, or the function
  itself threw. The caller asks again; the read path opens nothing again by itself (ruling 24).""",
       """- `:ended` comes when the read path can no longer keep the read right: Rama ended a push, a run grew
  past the build's number of places or the checks past theirs, a read that the open read came to need
  failed, or the function itself threw. The caller asks again; the read path opens nothing again by
  itself (ruling 24). A throw of the function while it is given `{:ended …}` is dropped.""")

p.between("**On which thread the function is called, and what it must not do.**",
          "**R5, where a caller meets it.**",
"""**On which thread the function is called, and what it must not do.** On a thread of Rama's client: the
thread of the push whose change it is (T5), or the thread on which a push that the read came to need
was given (run: C3, a function chained to a push's future ran on that push's thread). For an
`{:ended …}` after a wait that passed, on the thread of the JVM's own timer. Never on the thread that
called `read-live`. The read path holds the read's own lock while it calls, so the calls for one read
come one at a time and in the order in which they were worked out.

The function must return soon, and the docstring says what it stops while it does not. Run, C4, on an
in-process cluster whose client had two such threads: while one callback stayed for 2.5 s, every
push whose callbacks run on that thread waited, three of the seven others, pushes of other askers
among them. A select and the opening of a push from any other thread waited as well when Rama's
client gave their answer on that thread: in one run six of six selects that block, four of six that
do not, and four of six openings; in another six, six and three. So one slow function holds back the
reads of others, `read-once` and `read-live`, for as long as it stays, up to their own bound. And a
second change for the same read, on the other thread, waits for the read's lock and so holds that
thread too. A caller that does more than hand the change on takes a thread of its own.

The function may call `leave`, for its own read or for another. `leave` takes no lock and waits for
nothing, and closing a push inside its own callback, or inside the callback of another, returned at
once and ended that push (run: C5, C6). It must not call `read-once` or `read-live`: they wait for
Rama on a thread that Rama may need for the answer. Of six selects made inside a callback, three
were given only after the callback returned; a select that blocks, made inside a callback, came back
in a millisecond in two runs and did not come back in a third (run: C5, C9, and run 1). So the read
path turns such a call away at once, with the word `:read-inside-an-askers-function` and no call to
Rama: `tell!` marks its thread for the length of the call, and `read-once` and `read-live` look at
the mark first. What the function does with a change, and on which thread of its own, is the
caller's.

In the code and its explanations this lock is called the read's **turn**, and never a lock: in this
tree a lock is an encryption key (`src-store/CLAUDE.md`, "Words"), and the read path reads none. This
paper says "the read's lock", as ruling 67 does.

""")

# --- the words: seven
p.swap("""a map that holds `:could-not`, one of the read path's own six (`softland.store.read-path.words`). No
answer holds `:could-not`.""",
       """a map that holds `:could-not`, one of the read path's own seven (`softland.store.read-path.words`). No
answer holds `:could-not`.""")
p.swap("""| `:get-an-answer-within-its-wait` | `read-once`, `read-live` | the read path's wait passed before Rama gave every value, or the read did not settle in 8 rounds | `:waited-ms` |""",
       """| `:get-an-answer-within-its-wait` | `read-once`, `read-live` | the read path's wait, one bound for the whole call, passed before Rama gave every value, or the read did not settle in 8 rounds | `:waited-ms` |
| `:read-inside-an-askers-function` | `read-once`, `read-live` | the call was made on a thread on which the read path is calling an asker's function; no call was made to Rama | — |""")
p.swap("""| `:keep-the-read-live` | the function, under `:ended` | the read ended | `:because`: `:the-store-ended-a-push`, `:a-run-grew-too-large`, `:a-read-it-came-to-need-failed` or `:its-function-threw`; and what that carries |""",
       """| `:keep-the-read-live` | the function, under `:ended` | the read ended | `:because`: `:the-store-ended-a-push`, `:a-run-grew-too-large` (with `:what` and `:most`: the places of a run, or the checks of the read), `:a-read-it-came-to-need-failed` or `:its-function-threw`; and what that carries |""")
p.swap("""6. `words`: the six, each with `:from` and `:says`.""",
       """6. `words`: the seven, each with `:from` and `:says`.""")
p.swap("""| `:could-not`; `:find-the-module`, `:read-the-query`, `:reach-the-store`, `:get-an-answer-within-its-wait`, `:serve-a-read-that-large`, `:keep-the-read-live` | The read path's six words. The first is spelled as the door's and is the read path's own |""",
       """| `:could-not`; `:find-the-module`, `:read-the-query`, `:reach-the-store`, `:get-an-answer-within-its-wait`, `:read-inside-an-askers-function`, `:serve-a-read-that-large`, `:keep-the-read-live` | The read path's seven words. The first is spelled as the door's and is the read path's own. The fifth was added by the check of this plan |
| "the read's turn" | The thing that lets one call of an asker's function run at a time for one read. Never "lock": a lock is an encryption key in this tree |""")

# --- a read that an open read came to need fails: who waits
p.swap("""**A read that an open read came to need fails.** A push that step 5 joined is not given within the
wait, or its opening throws, or its run is too large: the read cannot be kept right, so it ends with
`:keep-the-read-live` and the reason.""",
       """**A read that an open read came to need fails.** A push that step 5 joined is not given within the
wait, or its opening throws, or its run is too large: the read cannot be kept right, so it ends with
`:keep-the-read-live` and the reason. No thread waits for such a push: the wait is a bound on its
future (`CompletableFuture.orTimeout`), and when it passes the read is ended on the thread of the
JVM's timer. An opening that throws is caught where it is made, so that the entry's delay holds a
future of a word and never a thrown thing; each read of the entry ends or gives the word, and the
entry leaves the map with its last reader.""")
p.save()
