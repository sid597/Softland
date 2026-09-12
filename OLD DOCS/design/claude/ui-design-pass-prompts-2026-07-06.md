# UI Design Pass — Top-Layer Design Briefs · 2026-07-06 · v3

References below to `BETS.md` are historical; that document is retired.
For vision orientation, use [carry-on](../../../docs/carry-on.md) as a reference summary
and [the vision log](../../../vision/LOG.md) as the primary source.

**Standing:** Fable-authored briefs for the design harness (claude.ai), sitting 3.
v3 after Sid's override: **problem-first**. v2 led with mood and framed System 1
as "the wall made alive" — Sid killed that on sight ("the goal of code is not to
make a reality emulator"). The wall panel is provenance of the *questions* the
view answers, never a material target. Every brief now opens with the actual
trail-view problem; each system is a distinct **bet** on solving it; materials
serve legibility, not nostalgia.

**How to drive:** three fresh chats, one brief each, single paste. Iterate by
talking to the render's chat naturally. Bring renders back for adjudication
against the fork map (which stays home).

---

## BRIEF 1 — THE CROSSROADS (bet: the answer is a shape)

```
You're designing one screen for Softland, a personal knowledge system its
builder is constructing for himself. Read the problem first — the design
exists to solve it, and every choice should be argued from it.

THE PROBLEM

One person is building a large software project by running many AI work
sessions in parallel, alongside daily commits, documents, and decisions
scattered across disconnected tools. The history that would explain the
project — where the work is right now, how it got here, which paths were
tried and abandoned, what is still unresolved — is fully CAPTURED: every
commit, every AI conversation, every asserted relation between them is in
the system. But captured is not legible. The current screen renders this
history as a uniform terminal feed: hundreds of entries at identical
visual weight, each wearing all its metadata all the time, parallel
efforts interleaved into noise, dead ends indistinguishable from live
paths, and 161 documents connected to nothing — with nothing marking them
as different. So the builder still rebuilds his orientation from memory
every morning. That is the failure you are designing against.

THE SCREEN'S JOB

- In one glance: where the work is now, and what recently changed.
- In one minute: how it got here — the few real paths, where they forked,
  which ones died.
- At all times, honestly: what is asserted structure vs not-yet-placed;
  who/what asserted each connection; what remains unresolved. This system
  never hides its own gaps — that honesty is a feature, not a compromise.

THIS SYSTEM'S BET

The answer is a SHAPE. If the paths of work are drawn AS paths — time
flowing left to right, each effort a line you can follow, forks visible as
forks, a pivot visible as a bend, an abandoned path visibly ending — then
"how did we get here" is answered before a single word is read. Words
confirm what geometry already said. This is the system where the
fork/pivot/dead-end structure is the hero; everything else supports its
legibility. Choose materials and palette purely for that: a calm ground,
strong path ink, and exactly one reserved accent (a warm kraft amber) for
relation marks — the statements ABOUT things — so structure and material
never blur. No texture, no skeuomorphism, no decoration that doesn't
disambiguate.

THE SCENE

Time runs left to right under a thin ruler, 2022 to 2026, spaced
honestly — the long empty stretch stays empty; February 2024's frantic
fortnight crowds, because that is the truth.

What's in the world:
- A thread of thirteen commits from 2024: an engine struggle, then a pivot
  ("now moving back to electric"), then a breakthrough. One line of work,
  with one visible bend and one small ✗ where the old direction ended.
- A thread of five commits from yesterday — which happen to be the commits
  that built this very view.
- One conversation that all thirteen 2024 commits point to ("produced").
  It has no name yet — only a long raw address. Don't invent a name;
  design the honest fallback well. Thirteen separate connector lines would
  be noise: bundle them into one trunk with a count, labeled once.
- Relation marks in the reserved amber: small labels like "based-on",
  "produced", "new-direction", each carrying its asserter in fine print.
  A relation is a line with a label, never a box.
- Along the bottom, one clearly-differentiated region: the 161 unplaced
  documents, labeled "161 unthreaded · no asserted relations yet". This is
  the frontier of what's been structured — it must read as different in
  kind from the threads, never hidden, never dressed up as organized.

Each node is a mark first — a small glyph for its kind (commit / document /
conversation) with its name as a label beside it. From a distance marks
persist and labels fold into counted clusters; up close labels carry. Kind
and status are always readable from shape or word, never from color alone.

THE THREE BOARDS (same world, three moments)

1. "At a glance" — everything closed. The whole journey's shape legible in
   seconds: two threads, the bend, the ✗, the trunk to the conversation,
   the unthreaded region.
2. "Reading one thing" — commit 2232fbb opened in place: a surface pinned
   at its position carrying its full record (dates, author, relations,
   address); the conversation open too. Everything else HOLDS ITS
   POSITION — opening never bends the timeline; detail grows into the
   row's own space.
3. "Everything open" — every 2024 commit showing its reading form (glyph +
   title + dates line). The dense fortnight gets crowded; design the
   crowding. This board is the stress test.

MANNERS (non-negotiable behaviors of this screen)

- Time never bends: nothing that opens or closes moves anyone else's
  place.
- Closed things are typography; a box exists only around an open thing.
- Two inks: terrain ink for things, amber for statements about things.
- Gaps, dead ends, the unnamed, the unplaced — all shown, plainly.
- No meaning by color alone.
- Real text only: set the strings below verbatim; ellipsis over invention.

EXPLORE (where I want your judgment pushed)

- How far shape alone can carry orientation before words are needed —
  test your board 1 by squinting.
- How labels recede with distance: the typography of a cluster of eight
  commits in three days.
- How thirteen same-kind relations bundle into one legible trunk.
- The open record: how full detail can appear in place and feel calm.
- On the open record: a small affordance meaning "step into this commit"
  (it opens elsewhere in the OS of this land), and an honest empty slot
  reading "· no authored line yet ·" where the builder's own one-line
  summary will someday live.

THE ACTUAL MATERIAL (all real; each piece carries two dates — when it
happened, and when the system received it: all arrived 07-06)

The 2024 thread, oldest first (name · title · happened):
3bb81ac · "Squashed initial commit" · 2024-01-18 (authored back in
  2022-05 — a two-year drift you may surface honestly)
2232fbb · "reduced the delta by which we zoom in or out now it feels more
  controlled" · 2024-01-18 (authored 2023-09)
dcc1c40 · "now moving back to electric" · 2024-01-26   ← the pivot
b316bae · "fixed bugs after updating to IC version of electric" · 2024-02-01
9c23442 · "finnnaalllyy after a long day electric and rama are working, I
  can do reactive and non-reactive quries both, send new events from the
  reply VERY NOICE" · 2024-02-01
7fede85 · "fix the pstate schema based on how the client passes the data,
  added a few more helper functions, more queries…" · 2024-02-03
463fc28 · "now nodes are queried from rama, each of the node specific data
  is queried as needed it is surgically reactive very very fine grained" ·
  2024-02-03
7b9077c · "Merge pull request #4 from sid597/rama" · 2024-02-03
d5ecafc · "fix electric db queries based on the rama changes, use keypath
  as the first path so pstate automatically use the right partition" ·
  2024-02-05
82ff930 · "updated electric" · 2024-04-20
72cfaec · "performant panning and zooming after using missionary, still a
  few things that I don't understand" · 2024-06-18
0c4e314 · "finally learned to get started with missionary, also maybe
  found the solution for my problem" · 2024-07-01
9d310fd · "mount items based on the diff over the visible rects, fix non
  reactive dependency, use old array for unmounting from spine…" ·
  2024-10-01

Yesterday's thread (all happened 2026-07-06), oldest first, each based-on
the previous:
4d543a2 · "feat(trail-room): R-2 bands + lanes-from-edges + move chips"
f6257a9 · "feat(git-spine): claimed-ms two-clock + alias rebase + truthful
  stats + run-level dedup"
8549a69 · "chore(fonts): slug glyph set 95 -> 591"
39b93ec · "docs: Trunk-5 wave boundary - 3 gate reviews all PASS"
fb1b30c · "docs: face-2 v1.1 COUNTERSIGNED - handed to product-side session"

The nameless conversation (all thirteen 2024 commits "produced" it):
oc:chat-conversation:chat:d7a6024bddebc679b8e3959d12d63954d45b9a59450f519d…

The open record for 2232fbb (board 2):
sha 2232fbb4b2cf0f6d7c49d6b331a2675f839575a3 · author sid597
<siddharthdv77@gmail.com> · authored 2023-09-25 · committed 2024-01-18 ·
attested 2026-07-06 · relations: ← based-on oc:doc:d6432f75… ·
→ based-on oc:doc:a0229146… · ← produced oc:chat-conversation:chat:affac7f3…
(each "by import:git-spine")

A few of the 161 unthreaded documents:
decisions.md · BETS.md · FIRST_LIGHT_R2.md · vision/LOG.md ·
next-prompt.md · room-card-lane-2026-07-05.md · …

DELIVERABLE

One self-contained HTML page (inline CSS, system fonts, no external
resources, static). Three art-boards stacked, each 1400px wide, each
titled, one quiet caption line under each.
```

---

## BRIEF 2 — THE CHRONICLE (bet: the answer is a page)

```
You're designing one screen for Softland, a personal knowledge system its
builder is constructing for himself. Read the problem first — the design
exists to solve it, and every choice should be argued from it.

THE PROBLEM

One person is building a large software project by running many AI work
sessions in parallel, alongside daily commits, documents, and decisions
scattered across disconnected tools. The history that would explain the
project — where the work is right now, how it got here, which paths were
tried and abandoned, what is still unresolved — is fully CAPTURED: every
commit, every AI conversation, every asserted relation between them is in
the system. But captured is not legible. The current screen renders this
history as a uniform terminal feed: hundreds of entries at identical
visual weight, each wearing all its metadata all the time, parallel
efforts interleaved into noise, dead ends indistinguishable from live
paths, and 161 documents connected to nothing — with nothing marking them
as different. The builder still rebuilds his orientation from memory
every morning. That is the failure you are designing against.

THE SCREEN'S JOB

- In one glance: where the work is now, and what recently changed.
- In one minute: how it got here — the few real paths, their forks, their
  dead ends.
- At all times, honestly: asserted structure vs not-yet-placed; who/what
  asserted each connection; what remains unresolved.

THIS SYSTEM'S BET

The answer is a PAGE. Orientation by reading — but reading a RECORD that
someone designed, not a log that accumulated. Time flows down; scrolling
is reading; hierarchy comes entirely from typography: what matters is
larger, what's metadata is quieter, what's a statement-about-things is set
in a distinct second ink. The bet: for one person's history, a superbly
set chronicle beats any diagram — IF the typography does the work the
uniform feed refused to do: differentiate, subordinate, declare gaps. The
discipline of a fine reference book, in service of scanning speed.

THE SCENE

A tall page. Entries hang in time order down the left ~55% of the width;
the right half is quiet space that opened records grow into. Threads of
work are NOT columns — membership shows as a thin spine in the left
margin, one spine per thread, entries hanging from theirs.

What's in the world:
- The 2024 thread: thirteen commits — struggle, pivot ("now moving back to
  electric"), breakthrough ("finnnaalllyy…"). Its pivot and its dead end
  (✗) are recorded as statements, set so a scanner catches them.
- Yesterday's thread: five commits — which happen to be the commits that
  built this very view.
- One conversation all thirteen 2024 commits point to — nameless, only a
  long raw address. Set the ugliness honestly and well.
- Relation statements in the second ink (a warm amber), each a LINE IN THE
  RECORD at the date it was asserted: "⊢ 2232fbb based-on d6432f75… ·
  import:git-spine". The thirteen "produced" links collapse to one summary
  line with a count. Relations are sentences in the record, not arrows.
- Time is honest about gaps: where months pass, a quiet full-width rule
  says so — "— 2 months pass —" — a jump never masquerades as a heartbeat.
- The final section: the 161 unplaced documents, visibly different in
  kind, labeled "161 unthreaded · no asserted relations yet".

An entry is a set line: small kind-glyph, then title, room to breathe. Its
second line, when shown, is quieter: "claimed 2024-02-01 · arrived 07-06 ·
import:git-spine". Hierarchy from scale and tone, never boxes.

THE THREE BOARDS (same world, three moments)

1. "At a glance" — the whole record closed: spines, amber statements, gap
   rules, the unthreaded section. A scanner finds the pivot, the ✗, and
   the breakthrough inside ten seconds — that is the test.
2. "Reading one thing" — 2232fbb opened: a surface grows RIGHTWARD into
   the free half, anchored to its line, carrying its full record; the
   conversation open too. The left page's rhythm gains no inserted rows.
3. "Everything open" — every 2024 entry showing both its lines, three
   records open at right. The rhythm must hold under weight.

MANNERS (non-negotiable behaviors of this screen)

- The time column's rhythm is sacred: opening spends the right half,
  never inserts rows.
- Closed things are typography; surfaces belong to open records only.
- Two inks: terrain for things, amber for statements about things.
- Gaps, dead ends, the unnamed, the unplaced — recorded, plainly.
- No meaning by color alone.
- Real text only: the strings below, verbatim; ellipsis over invention.

EXPLORE (where I want your judgment pushed)

- The scale ladder: thread name / entry title / dates line / amber
  statement — four voices a scanner never confuses.
- Gap rules as part of the record's fabric.
- The rightward surface: full detail without the page losing its calm.
- On the open record: a small affordance meaning "step into this commit",
  and an honest empty slot — "· no authored line yet ·" — where the
  builder's own one-line summary will someday live.

THE ACTUAL MATERIAL (all real; each piece carries two dates — when it
happened, and when the system received it: all arrived 07-06)

The 2024 thread, oldest first (name · title · happened):
3bb81ac · "Squashed initial commit" · 2024-01-18 (authored back in
  2022-05 — a two-year drift you may surface honestly)
2232fbb · "reduced the delta by which we zoom in or out now it feels more
  controlled" · 2024-01-18 (authored 2023-09)
dcc1c40 · "now moving back to electric" · 2024-01-26   ← the pivot
b316bae · "fixed bugs after updating to IC version of electric" · 2024-02-01
9c23442 · "finnnaalllyy after a long day electric and rama are working, I
  can do reactive and non-reactive quries both, send new events from the
  reply VERY NOICE" · 2024-02-01
7fede85 · "fix the pstate schema based on how the client passes the data,
  added a few more helper functions, more queries…" · 2024-02-03
463fc28 · "now nodes are queried from rama, each of the node specific data
  is queried as needed it is surgically reactive very very fine grained" ·
  2024-02-03
7b9077c · "Merge pull request #4 from sid597/rama" · 2024-02-03
d5ecafc · "fix electric db queries based on the rama changes, use keypath
  as the first path so pstate automatically use the right partition" ·
  2024-02-05
82ff930 · "updated electric" · 2024-04-20
72cfaec · "performant panning and zooming after using missionary, still a
  few things that I don't understand" · 2024-06-18
0c4e314 · "finally learned to get started with missionary, also maybe
  found the solution for my problem" · 2024-07-01
9d310fd · "mount items based on the diff over the visible rects, fix non
  reactive dependency, use old array for unmounting from spine…" ·
  2024-10-01

Yesterday's thread (all happened 2026-07-06), oldest first, each based-on
the previous:
4d543a2 · "feat(trail-room): R-2 bands + lanes-from-edges + move chips"
f6257a9 · "feat(git-spine): claimed-ms two-clock + alias rebase + truthful
  stats + run-level dedup"
8549a69 · "chore(fonts): slug glyph set 95 -> 591"
39b93ec · "docs: Trunk-5 wave boundary - 3 gate reviews all PASS"
fb1b30c · "docs: face-2 v1.1 COUNTERSIGNED - handed to product-side session"

The nameless conversation (all thirteen 2024 commits "produced" it):
oc:chat-conversation:chat:d7a6024bddebc679b8e3959d12d63954d45b9a59450f519d…

The open record for 2232fbb (board 2):
sha 2232fbb4b2cf0f6d7c49d6b331a2675f839575a3 · author sid597
<siddharthdv77@gmail.com> · authored 2023-09-25 · committed 2024-01-18 ·
attested 2026-07-06 · relations: ← based-on oc:doc:d6432f75… ·
→ based-on oc:doc:a0229146… · ← produced oc:chat-conversation:chat:affac7f3…
(each "by import:git-spine")

A few of the 161 unthreaded documents:
decisions.md · BETS.md · FIRST_LIGHT_R2.md · vision/LOG.md ·
next-prompt.md · room-card-lane-2026-07-05.md · …

DELIVERABLE

One self-contained HTML page (inline CSS, system fonts, no external
resources, static). Three art-boards stacked, each 1400px wide, each
titled, one quiet caption line under each.
```

---

## BRIEF 3 — THE SURVEY (bet: the answer is altitude)

```
You're designing one screen for Softland, a personal knowledge system its
builder is constructing for himself. Read the problem first — the design
exists to solve it, and every choice should be argued from it.

THE PROBLEM

One person is building a large software project by running many AI work
sessions in parallel, alongside daily commits, documents, and decisions
scattered across disconnected tools. The history that would explain the
project — where the work is right now, how it got here, which paths were
tried and abandoned, what is still unresolved — is fully CAPTURED. But
captured is not legible: the current screen is a uniform terminal feed
where hundreds of entries carry identical weight, all metadata all the
time, dead ends indistinguishable from live paths, and 161 unconnected
documents undifferentiated. The builder rebuilds his orientation from
memory every morning. And the problem scales: this history will be
thousands of entries soon. A design that only works at 20 items is not a
design.

THE SCREEN'S JOB

- In one glance: where the work is now, and what recently changed.
- In one minute: how it got here — the few real paths, their forks, their
  dead ends.
- At all times, honestly: asserted structure vs not-yet-placed; who/what
  asserted each connection; what remains unresolved.

THIS SYSTEM'S BET

The answer is ALTITUDE. One continuous surface, time left to right — and
the ONLY control is how high you stand. High up, four years of work
compress to a few labeled paths and their landmark moments (the pivot, the
dead end, the breakthrough); descend and clusters resolve into titled
entries; on the ground everything is readable. The bet: orientation-then-
detail is a single continuous act, not a mode switch — and legibility at
EVERY height is the designed thing. What survives at altitude is chosen,
never accidental: landmarks stay, noise folds into counts. Style the
surface as a precise instrument — restrained palette, fine hairlines,
small exact labels, a fog treatment for the uncharted — every visual
device justified by what it disambiguates.

THE SCENE

The land's features:
- A dense cluster in early 2024: thirteen commits in one path — struggle,
  pivot ("now moving back to electric"), breakthrough — most crowded into
  one frantic fortnight. Density is real geography: busy where history was
  busy, honestly empty through the quiet of 2025.
- A young cluster dated yesterday: five commits — the commits that built
  this very view.
- One conversation all thirteen 2024 commits point to — nameless, one long
  raw address, labeled honestly.
- Relations in one reserved amber: solid line = asserted, dashed =
  proposed — a drawn convention, declared in a small key. The thirteen
  "produced" links bundle into one trunk with a count. The pivot and the
  small ✗ are landmark-grade: they survive at every altitude.
- FOG along the bottom edge: the 161 unplaced documents — "161 unthreaded ·
  no asserted relations yet". Fog is the honest rendering of what hasn't
  been structured yet: visible, labeled, obviously different in kind.
- A small legend in a corner — kind glyphs, line conventions, fog —
  designed as a quiet, precise part of the instrument.

Everything is a mark first: a small symbol for its kind, name as label.
Labels live and die by altitude; marks and landmarks persist.

THE THREE BOARDS (one surface, three heights — not three modes)

1. "High" — the whole range, 2022–2026. Paths, landmarks (pivot, ✗,
   breakthrough), year marks, cluster counts, the fog. Ten seconds of
   looking should answer "how did this project get here" in outline.
2. "Mid, two pins" — the camera over the 2024 cluster, titles legible.
   Two records PINNED at full detail — 2232fbb and the conversation — each
   a calm pinned surface holding ground-level detail while the map around
   stays at mid height. Neighbors keep their places.
3. "Ground" — the 2024 stretch up close: every record readable, relations
   labeled with their asserters. The two-clock drift shows here: 3bb81ac
   and 2232fbb were authored YEARS before they entered history — draw each
   an honest thin reach-back line along the ruler to its authored date.

MANNERS (non-negotiable behaviors of this screen)

- Altitude changes what's legible, never where things are; pinning never
  shifts neighbors.
- Closed things are marks and labels; surfaces appear only at a pin.
- Two inks: terrain for things, amber for statements about things; solid
  asserted, dashed proposed, and the key says so.
- Fog for the uncharted, ✗ for the dead, a raw address where no name
  exists, drift drawn where dates drift — the surface never lies by
  omission.
- No meaning by color alone.
- Real text only: the strings below, verbatim; ellipsis over invention.

EXPLORE (where I want your judgment pushed)

- The altitude ladder: what earns landmark status and survives to the
  top; what folds into a count; what only exists on the ground.
- Fog as a designed material for "not yet structured".
- The pinned surface: "held at ground detail while the world stays high"
  made stable and calm.
- On a pinned surface: a small affordance meaning "step into this
  commit", and an honest empty slot — "· no authored line yet ·" — where
  the builder's own one-line summary will someday live.

THE ACTUAL MATERIAL (all real; each piece carries two dates — when it
happened, and when the system received it: all arrived 07-06)

The 2024 thread, oldest first (name · title · happened):
3bb81ac · "Squashed initial commit" · 2024-01-18 (authored back in
  2022-05 — the drift board 3 draws)
2232fbb · "reduced the delta by which we zoom in or out now it feels more
  controlled" · 2024-01-18 (authored 2023-09)
dcc1c40 · "now moving back to electric" · 2024-01-26   ← the pivot
b316bae · "fixed bugs after updating to IC version of electric" · 2024-02-01
9c23442 · "finnnaalllyy after a long day electric and rama are working, I
  can do reactive and non-reactive quries both, send new events from the
  reply VERY NOICE" · 2024-02-01
7fede85 · "fix the pstate schema based on how the client passes the data,
  added a few more helper functions, more queries…" · 2024-02-03
463fc28 · "now nodes are queried from rama, each of the node specific data
  is queried as needed it is surgically reactive very very fine grained" ·
  2024-02-03
7b9077c · "Merge pull request #4 from sid597/rama" · 2024-02-03
d5ecafc · "fix electric db queries based on the rama changes, use keypath
  as the first path so pstate automatically use the right partition" ·
  2024-02-05
82ff930 · "updated electric" · 2024-04-20
72cfaec · "performant panning and zooming after using missionary, still a
  few things that I don't understand" · 2024-06-18
0c4e314 · "finally learned to get started with missionary, also maybe
  found the solution for my problem" · 2024-07-01
9d310fd · "mount items based on the diff over the visible rects, fix non
  reactive dependency, use old array for unmounting from spine…" ·
  2024-10-01

Yesterday's thread (all happened 2026-07-06), oldest first, each based-on
the previous:
4d543a2 · "feat(trail-room): R-2 bands + lanes-from-edges + move chips"
f6257a9 · "feat(git-spine): claimed-ms two-clock + alias rebase + truthful
  stats + run-level dedup"
8549a69 · "chore(fonts): slug glyph set 95 -> 591"
39b93ec · "docs: Trunk-5 wave boundary - 3 gate reviews all PASS"
fb1b30c · "docs: face-2 v1.1 COUNTERSIGNED - handed to product-side session"

The nameless conversation (all thirteen 2024 commits "produced" it):
oc:chat-conversation:chat:d7a6024bddebc679b8e3959d12d63954d45b9a59450f519d…

The pinned record for 2232fbb (board 2):
sha 2232fbb4b2cf0f6d7c49d6b331a2675f839575a3 · author sid597
<siddharthdv77@gmail.com> · authored 2023-09-25 · committed 2024-01-18 ·
attested 2026-07-06 · relations: ← based-on oc:doc:d6432f75… ·
→ based-on oc:doc:a0229146… · ← produced oc:chat-conversation:chat:affac7f3…
(each "by import:git-spine")

A few of the 161 documents in the fog:
decisions.md · BETS.md · FIRST_LIGHT_R2.md · vision/LOG.md ·
next-prompt.md · room-card-lane-2026-07-05.md · …

DELIVERABLE

One self-contained HTML page (inline CSS, system fonts, no external
resources, static). Three art-boards stacked, each 1400px wide, each
titled, one quiet caption line under each.
```

---

## Notes (Fable's, not for pasting)

- v3 correction (Sid, this sitting): the wall panel = provenance of the
  QUESTIONS (where are we / how did we get here / which crossroads), never
  an aesthetic target. "The goal of code is not to make a reality
  emulator." System 1 renamed THE WALL → THE CROSSROADS; kraft-paper
  material language removed everywhere; amber survives only as the
  functional second ink for assertions (R6's two materials).
- The pivot and dead-end relations are design fixtures: real historical
  moments, assertions not yet in the land. They exist so fork/pivot/ending
  grammar is visible in every render.
- Top layer only, per Sid. Deeper passes get fresh briefs for the
  survivor.
