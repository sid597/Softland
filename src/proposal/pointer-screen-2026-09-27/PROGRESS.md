# The pointer's screen on the rig's store: the build's record

Branch `pointer-screen-2026-09-27`, cut from `rig-2026-09-25` (1e63217e), in
`~/projects/Softland-pointer-screen` on the Mac. Never pushed. The citation
session (`~/projects/Softland-citation`, branch `citation-2026-09-27`) owns
the repo material as facts (`rig.material`) and the lookup from a fact to what
stood on it (`rig.store.dependents`); this build takes both by merging that
branch and writes neither.

Sid's brief, 27 September: Inland's pointer running on the rig's store, with a
real screen, fixing every item of `src/proposal/citation-2026-09-27/HANDOFF.md`'s
list of what the pointer lacked. Read the brief's six items there; this file
says how each was built and where it stands.

## Now (27 September, evening)

- The store side is built and committed (`c3e53699`): bells, one record per
  gesture, later members, and four yielding ramafns made ramaops (the ramaop
  change not yet run).
- The adapter runs: the rig's module is deployed into Inland's local cluster
  through `bin/inland` on the Mac, and the server-side check
  (`tools/screen_smoke.clj`, `runs/smoke-6.txt`) held every check there. The
  three scenes pass on the server side on a fresh in-process cluster
  (`test-inland/softland/inland/scenes_test.clj`, `runs/scenes-1.txt`: 21
  assertions, 0 failures).
- The screen is blocked on one thing: Electric's compiler asks for a one-time
  login on this Mac (the link is in `runs/build-1.txt`); the browser build waits
  for it. Everything else `bin/inland` needs is here.
- Waiting on the citation session's commits (`rig.material`, the
  `[:dependents F]` lookup) to merge; until then the material view says no
  material is read and marks cannot be found.

## How it is built

### The screen and its store

- `bin/inland` deploys the rig's module (`rig.store.module/Store`) into
  Inland's local cluster, in place of Inland's own module.
- `softland.inland.store` is rewritten as an adapter over the rig's door
  (`rig.store.client`, `rig.store.micro-client`, `rig.store.promote-client`)
  and its one read exit (`rig.store.read-exit`, `rig.store.standing`). The
  names Inland's other namespaces call stay; what is under them changes.
- Inland's records become facts, one fact per attribute, the thing's id the
  keyword of the record's name (the start in
  `Softland-pointer-2026-09-27/.../pointer/facts.clj`, carried here).

### Reads (item 1)

- Every read goes through the exit and is recorded.
- A gesture (point, submit, keep, pin, promote, note) is run once, on the
  server, by a one-shot evaluator of Inland's recipe language: its reads are
  queried, then written as one entry act (one record per gesture), and only
  then do its effects run. The acts the gesture writes stand on the facts
  that record read.
- The screen's views read through standing reads (role `:shown`), one per
  (layer, pattern), shared by every narrow subscription Inland opens onto it.
  The opening, delivery and closing lines one cause produces go in one act.
- Push, not poll: a delivery happens only when the store pushes. A one-owner
  layer rings through a new field, `[layer :last-admitted]`, the stamp of its
  last admitted act, written in the gate's one write group; a Rama proxy on
  it pushes each change. A shared layer rings through the settled frontier
  (`$$micro-task [:frontier]`, a proxy). A forget rings through the person
  entries `$$persons [p]` on each task (a proxy per person per task), and the
  screen re-reads by id every fact it shows.
- When a delivery shows a fact replaced, or shadowed by a nearer layer, the
  adapter reads `[:dependents F]` (the citation session's pattern) in the
  person's hand layer and own layer, and marks on screen what stood on F: a
  selection, a pin, a kept variation.

## Placeholders (item 3): where a question froze into stored records

Each is the simplest answer that can change later. None is a ruling. The
ones marked **first-record** would freeze at a kept record.

Records as facts (`softland.inland.facts`):
1. A thing's id is the keyword of the record's name (`"targeting"` is
   `:targeting`). Ingest identity stays open (PROGRESS "Now", 17, 59, 60).
   **first-record**
2. One fact per attribute, the name included (a grain can be coarsened,
   never refined). **first-record**
3. A record's revision, the number a pin and an edit's check use, is the
   largest stamp among its facts.
4. A layer's id is the keyword of Inland's layer name.

What a selection, a pin and a keep are as records:
5. A selection is a fact `:cell/selection` on the browser session's thing
   in the person's hand layer; its value is the target the rule chose,
   `{:file :from :to :kind :label}`; its act stands on every fact the point
   gesture read (the rule, the tool, the material). **first-record**
6. A pin is an entry in the session's context cell (`:cell/context`,
   `{:layers [...] :pins {name {:layer :revision}}}`), same thing, same
   layer; the act writing it stands on the pinned record's facts.
   **first-record**
7. A keep writes two records in the person's own layer: a copy of the rule
   (`<name>-targeting`) and a copy of the tool naming that copy. Both stand
   on the tool and the rule the gesture read, so a kept variation stands on
   the rule it was kept with and is found when that rule changes.
8. Every other cell (drafts, tabs, pages, attention) stays in the browser.

Reads (item 1):
9. One read record per gesture: one entry act with one line per read the
   gesture's rules made, role `:stood-on`, each read on an entry entity of
   its own. **first-record**
10. A view's reads: standing reads, role `:shown`, whose opening, delivery
    and closing lines for one cause (the page settling within 15 ms, one
    push) go in one act. An entry's entity is made from a name of its own
    when its lines share an act (FR5's form, widened). **first-record**
11. "This must not exist yet" (a keep's expected revision 0) is an empty
    read recorded in the gesture's record. The gate does not enforce it:
    the envelope has no expected-versions part yet, and only a stale
    replace is refused.
12. An edit's expected revision is checked by the host, against the record
    as the person's context resolved it when they loaded it, after the
    gesture's reads are recorded.
13. A ring of a bell is not recorded: it names no fact. What is shown after
    it is read through the exit.
14. A forget does not reach a standing read's deltas (For Sid 53, left as
    built); a forget's ring makes the screen re-read by id every value it
    shows.
15. Marks: for each fact the screen sees replaced or shadowed, `[:dependents
    F]` is read in the person's hand layer and own layer; a mark shows while
    its dependent still heads its key.

People (item 4):
16. Login lives at the host: salted PBKDF2 hashes in
    `.inland-runtime/people.edn`, the words in `passphrases.txt` beside it,
    tokens in memory. Nothing of it is in the store.
17. A browser page is a session `S` in the store (`:alice-s-<8 hex>`),
    granted `[S L L p-perm]` beneath the person's own permission in their
    hand layer, own layer, the base and each group they belong to. Sessions
    are never closed yet; their unconsumed lease rows stay.
18. A group is made by the operator when a person asks, with them its first
    member; a member asks and the operator accepts another. Members are only
    added. The host refuses naming an existing member (the fold would move
    their batch).

The note and the resident (items 5, and the brief's stand-in):
19. A note is a record `note-<8 hex>` whose `:note` value is `{:on target
    :text :names #{person}}`; its grammar makes `:names` its subjects. It is
    written in the pair's layer, so a note about one person dies with them
    (in a person's own layer it would die with its owner and survive the
    person named). **first-record**
20. The resident's running and outcome are written by the operator into the
    asker's layer, each replacing the status before; the reply is a
    stand-in until Sid says yes.

Material:
21. The repo's material is read into the base by the host as the operator,
    calling the citation session's `rig.material`: no tool can run in the
    base on the rig as built (a tool's lease is sealed under the layer's
    person owner, and the base's owner is the root actor).
22. A `material` record in the base lists the files read, their paths and
    labels, and the commit; the host writes it.

## The count (item 6): every compiled step the whole build needed

In the rig's unit: one named code unit added to the fixed side or to the
vocabulary, or one existing unit whose behaviour changes; classed (a) steps
the frame already promised, (b) steps nobody anticipated, (c) capabilities.
The citation session's units (the material reader, the lookup) are its count,
not this one. Seed records are data and count zero: the material view, the
three targeting rules, the pair's controls, the note rule. So far: **35**, 9
promised, 22 unanticipated, 4 capabilities. Provisional until the build ends.

(a) Promised, 9: records as facts (`facts`); the screen's reads as standing
reads pushed by the store (`reader`); marks from what stood on a changed fact
(`reader` changed-facts and current-marks, `store` marks-view, `app` Marks);
the adapter's read addresses (`store` address, watch-path); writes as acts
standing on the gesture's reads (`store` put-record!, retract-record!,
cell-write!); a record promoted into a group (`store` promote-record!);
sessions and their permissions (`people` open-session!, grant-session!); the
first facts (`people` ensure-world!); genesis as facts (`seed`).

(b) Unanticipated, 22: the layer's bell (`module` schema and `gate-event`
write); the bell client (`rig.store.bell`); standing reads in halves with one
act for many (`standing`); the gesture's record (`read-exit` checked-query,
record!); a later member at the micro gate (`micro` micro-extras); its door
call (`micro-client` add-members!); four ramafns made ramaops (`shared-reads`,
4); the one-shot gesture evaluator (`gesture`); activities handed to the
resident, not polled (`store` start/cancel, `resident` execute!); login at the
host (`people`); the host's login routes, socket guard and boot per person
(`server`); the operator's command line (`operator`); stored cells in views
(`execution` Cell); events as server gestures (`app` Events); the page's
session and stored context (`app` Main, LiveContext); the snapshot a gesture
sends (`session` snapshot); the launcher's process custody on macOS, the module
jar with rama-helpers' classes, and the Rama distribution's discovery
(`bin/inland`, 3).

(c) Capabilities, 4: the `:range` leaf (`total`); the `:note`, `:group` and
`:member` admission kinds (`store`). `:note` could be data (a put whose rule
builds the name), which would make it 3.

Configuration, not counted: the rig's source on Inland's classpath (`deps.edn`).

## For Sid

1. Electric's compiler needs a one-time login on this Mac before the screen can
   be built (`runs/build-1.txt` has the link; the build waits for it). Its token
   is bound to the machine, so the Linux one does not carry over.
2. Found on the real cluster: a record's id taken as the keyword of its name
   collides with key ids, because keys are ids and a key's grammar is a fact
   about its id (the rule record "targeting" landed on the `:targeting` key's
   grammar thing, and its genesis act was refused). Records now live in a
   `record` namespace (placeholder 1). The same corner exists for anything that
   names things by word in one keyword space with keys.
3. Inland's earlier JVM suite (`test-inland/softland/inland/test_runner.clj`)
   targets its retired module through the old adapter; it no longer compiles and
   is not run. Port it, or remove it?
4. The live store on the Mac holds the first genesis under the old ids: inert
   now (a thing with no namespace is never a record), but there. A fresh store
   means deleting `.inland-runtime/data` in this clone; say if you want that.
5. Browser sessions are never closed in the store yet; their unconsumed lease
   rows stay (placeholder 17).

## Scenes

- Scene one: one person points at this repo's code, changes the pointer's
  rule while using it, and sees what stood on the old rule marked.
- Scene two: two people on shared material; a rule promoted into a group
  layer; the base switching to shared.
- Scene three: a note naming a person; forget that person; the note shows as
  erased on its date.
