# Sid's refinements of 14 October 2026: order and time apart, and time travel

Sid asked for his refinements to be noted down, so that he fixes them in a new session. Written by
the session "parallel-if-needed" (Opus 5.5) on 14 October 2026, after it finished the build "run
again" (`../run-again/RECEIPT.md`). It is not a ruling.

How to read it. A quote between double quotes with an id ‹…› is a line that Sid typed, word for
word, as the record of `memtool line <id>` gives it. An id «…» is a session's text, not his. Every
other sentence is the session's reading, and says so where it matters. Two of his lines were not
yet in the record when this page was written; they are quoted from the conversation and marked.

## 1. His words, in order

13 October, before the build:
- "why do we even need the time as a stamp? The depot is enough. It is a sequential order in which
  things are added to it." ‹7b62aa2b›
- "so the reason we need this for is time travel???" ‹62434f40›
- "so we have a clock in front of depot and the local counnter correct?" ‹a15a7249›

14 October, in this session:
- "so we also make the order and time different is that what you saying??? btw my intent was the
  same" ‹aa212e6b›
- "so if we both meant the same thing how did even the new implementation drift??" ‹8afc236d›
- "how and what did find this "The far-future time question, in plain words" maybe there is more
  of this sort to be found and not worked on ... so you say time travel is not taken into
  consideration right now?? btw what do you understand by time travel? I mean it from the product
  pov the product being the tools, layers, groups etc. they have this feature available from the
  softland system available iff neeeded be and then they can use it for their own thing based on
  whatever granularity" ‹fca62974›
- "so from what i am understanding: order and time are different and we don't use the time for
  ordering. every thing that goes in depot has a time attached to it. So by default we have
  everything granular from the pov of time?? and therefore anything that requires more coarser
  time can more can be built on top of this e.g a tool e.g editor can say "Hey user you have an
  optiont to time travel the editor (graph based so can have thousands of nodes) on the graph
  level, on the page level, on per letter typed level, etc. .. like every granular needed that can
  be imagined is given" ????" ‹2f80019d›
- "so the most atomic unit in our system is a fact correct and a bunch of facts make an act which
  in itself can have many act?? so we adding the time to the fact or the act?" (not yet in the
  record)
- "i want you to note down my refinements and then i will fix them in a new session not this one"
  (not yet in the record)

## 2. The refinements

### R1. Order and time are apart. Order never uses the time

His words: ‹aa212e6b›, ‹2f80019d›; on 13 October ‹7b62aa2b›, ‹a15a7249›.

What the store does today, read in its source:
- A stamp is milliseconds since 1970 x 65,536 + a counter (`src-store/softland/store/gate/stamp.clj`).
  So the time is inside the order.
- The 10-second rule refuses an act that carries a stamp more than 10 seconds ahead of the time on
  the act (`gate/decision.clj`, `refused-early`). So the time decides who is accepted.
- Nothing else in the store reads the time out of a stamp: the read path and the door do not
  (searched: the one caller of `stamp/time-of` is the 10-second rule).

What R1 changes, the session's reading:
- The stamp becomes a counter for each layer: one more for each decision, an accept or a refusal,
  in the depot's order. A run again gives the same numbers.
- The time on the act stays, as a label. No rule reads it.
- The 10-second rule goes. In its place, the session proposes a check that needs no clock: a stamp
  that an act carries from its own layer cannot be later than that layer's last stamp. A stamp that
  an act carries from another layer is a reference only.
- "This act came after the facts it is based on", across layers, is read from the act's
  `:based-on`, not from the size of its number.
- What it removes: the lockout that the review of 14 October found (`../run-again/RECEIPT.md`,
  section 7). One accepted act with a time far ahead, from a fast server clock or from code that
  appends without the door, makes every honest act that names a later fact be refused "carried
  stamp too far ahead" until the clock catches up. With R1 a wrong clock gives a wrong label, and
  nothing else.
- What it gives up: one number for "the whole store as of 3 pm", always consistent across layers.
  That was the reason for the time in the stamp, see "The September bet" below.

What of `0d24b03e` (the two fixes of 14 October) stays under R1: a refusal's stamp is the layer's
last plus one, which a counter does anyway. The bound of the time at 2^45 loses its reason, which was
the packing; whether a range check on the time stays is open.

Queue rows that change with R1 (`../decisions/OPEN-QUESTIONS.md`), for a new session to bring to Sid:
- V46, where a time that is far ahead is stopped: falls away. Nothing is harmed by a far time.
- V49, a time at the end of the range: falls away.
- V43, what a second send carries as its time: becomes a question of which label is shown.
- V44, a record that arrives with no time: becomes a product question. Refuse it as now, or accept
  it with no time.
- The 10-second number, `stamp/max-ahead-millis`: falls away.

The September bet. The time was put into the stamp on purpose in September:
`src/proposal/frame-2026-09-15/CORNERS.md`, section 4, "keep `when` able to order. It costs
nothing at the first record and cannot be bought later", with two promises: a layer's stamps never
go backward, and an act's stamp is later than everything that it is based on. Then "as of T" is one
number across every layer. Sid never decided it: on 13 October he asked "so the reason we need this
for is time travel???" ‹62434f40›, and a session answered that it was "a bet on a future feature"
«d953d923». The session's view, for Sid to confirm: the bet's premise no longer holds. The store
keeps every act forever (no depot is trimmed) and comes out the same when it is run again from its
depots, and every act in a depot holds its time and what it is based on. So a consistent "as of T"
across layers can be built later, as an index computed from the depots with the two promises, if
time travel needs it. In an index a bad clock spoils only the index, which can be built again with a
corrected rule; in the stamp a bad clock decides who is refused, and every run again repeats it.

### R2. Everything in the depot has a time; the finest grain is kept; coarser is built on top

His words: ‹2f80019d›.

The session's reading:
- The finest step back in time is one act. A tool chooses its grain by what it puts in one act and
  how often it sends one. An editor that sends one act per letter can go back letter by letter.
- Coarser views can always be built later, by grouping and filtering the one history: by page, by
  graph, by pause, by save, by hour. Finer views cannot: what was not written is not there.
- A time-travel view steps through positions, which are exact, and shows times as labels, which
  are as good as the server's clock.
- The time on an act is when the server sent it, not when the person typed. The act has an
  optional `:claimed-when`, the maker's own time, which is a label too.
- Open in his vision: the grain of what is written. Vision log, line 698: "should it be per letter
  (yeah need it for the snappy feedback) but the semantics and the amount of events we will be
  creating and the through put"; and a pan at 240 frames a second. `docs/carry-on.md`, line 304.

### R3. Time travel is a capability of the system

His words: ‹fca62974›; vision log, line 203 ("the normal versionoing in rama by what time something
arrives … and time travel there can a explicit commited one which we want to do e.g like git does"),
line 249, line 400 (time travel is a lens, "not a universal one .. or the default one");
`docs/carry-on.md`, lines 105 to 111, 346 and 432.

The session's reading: the system offers it, and a tool, a layer or a group uses it if it needs it,
at the grain that it chooses. As a capability it is a few verbs:
- go to a point: a layer, or a stack of layers, as it was;
- name a point: a position in a layer, a time, or a version that someone committed;
- compare two points;
- fork from a point;
- choose the grain.

What the store has and does not have for it:
- Has: every act, kept forever; the depots are never trimmed.
- Does not have: a read of a point (the tables keep only the current fact of each place, so a past
  state needs a replay of the depot); the history of an entity; named points; fork; compare; and
  rules for the past: that a forgotten value does not come back ("Forget is immediate", the queue's
  H1, answered), and who may see an earlier state.
- No row of the queue's 128 is about time travel.

### R4. The time sits on the act

His question is the line marked "not yet in the record" above. The session's answer, for him to
confirm (`src-store/softland/store/act.clj`, the shapes `:act` and `:fact`):
- A fact is the smallest unit of what is stored: an entity, a key, a value or none (a retract), and
  what it replaces. A fact's id is [act-id, its index in the act].
- An act is the smallest unit of what happens: one who, one layer, one moment, its facts, decided
  whole. An act holds no acts. It points to facts (`:based-on`) and to one act (`:because-of`). A
  save, a session or a version is a grouping on top of acts.
- The door writes the time once for each act. Each fact's row carries a copy of its act's time and
  position, beside the act's stamp, who and permission, which it carries today.

### R5. Find more questions of this sort

His words: ‹fca62974›, "maybe there is more of this sort to be found and not worked on".

The session's reading. "This sort" is an open question that is a symptom of one choice that nobody
closed. The far-future question was one: it hung on the September bet, and so do V43, V44, V46 and
V49. The way to find more, proposed: go through the queue's 128 rows, group them by the choice that
each one really hangs on, and check in the record (`memtool who`, `search`) whether Sid ever made
that choice.

### R6. How the build drifted, and the process around it

His words: ‹8afc236d›; on the review, "Check the latest comment i made like today in the last
session about review and all" ‹dfb19469› and "Start the review but we will only do one review"
‹b0588aed›; on the docs of the tests ‹d840d2a4›.

The chain, from git and the record:
1. 21 September: the frame paper's bet (above), first written in commit `d0e7c1d9`, "First record:
   the corners, eight research voices applied".
2. 26 September: the rig, commit `42da5880`, "stamps become a hybrid clock, milliseconds times
   65536 plus a counter".
3. 8 October: the store's module took that stamp (`66f22ca3`).
4. 13 October: a session put the choice to Sid, "Keep the clock in the stamp" or "A counter alone
   … the time of day becomes a separate fact", and wrote "nobody has worked it through" «01169cef».
   The choice was not closed.
5. The build's prompt said "the door writes the time on the act", "no gate reads a clock" and "a
   number counted per layer". Those are a session's phrases, which Sid pasted; `memtool who` finds
   none of them in a line that he typed. Each fits both answers.
6. The build kept the shape that the code had. On 14 October this session patched that shape
   (`0d24b03e`) before it asked whether the shape should exist.

What stops it, proposed:
- When a prompt leaves open a choice that the existing code already answers one way, it says the
  choice in plain words, such as "the stamp holds no time", or the build stops and asks.
- A phrase in a pasted prompt is not Sid's word until the record shows that he typed it. Saved as
  memory: `.claude/memory/feedback-a-pasted-prompt-is-not-sids-word.md`.
- One review, after the work, given the code and not the session's conclusions. The build's prompt
  said "no reviewers"; that was a session's proposal, which Sid had not agreed to.

Done in this session, nothing left to fix: the docs of the tests (his ‹d840d2a4›, commit
`ebe0d0e4`), the one review and its results (`d3c50994`), tests for the six gaps that it found
(`9a1f70c8`), the scratch deleted (`2c2db6eb`).

## 3. For the new session

Read first: this page; `../run-again/RECEIPT.md`, sections 4, 7 and 8; `gate/stamp.clj`;
`gate/decision.clj` (`time-on`, `refused-early`, `stamp-of`); `door/declaration.clj` (`time-part`,
`max-time`); `module/walk.clj`, step 7; `CORNERS.md`, section 4; the queue's V43, V44, V46 and V49.

A change of what a gate gives is made before the first real record, so the fixture is made again
and the receipt says so (`test-store/softland/store/run_again_fixture/README.md`).

Not decided by Sid, and to be asked:
- whether a consistent "as of T" across layers is wanted, and if so as an index built later (the
  session's view) or in the stamp (the September bet);
- V43 and V44 under R1, and whether a range check on the time stays;
- the time-travel design of R3, and whether it gets a row in the queue;
- the grain that each tool writes (R2).
