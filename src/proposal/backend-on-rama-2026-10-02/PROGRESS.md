# Progress: the backend on Rama, above and below the act

Read this page first. It says where the work stands, what is written down, how
firm each part is, and what is open. It is not a ruling. Only Sid decides.

## What this folder holds

| File | What it is |
|---|---|
| `ARCHITECTURE.md` | The first drawing, made with Sid on 2 October 2026. |
| `ARCHITECTURE-STE100.md` | The same architecture after two recheck sessions, with a mark on every statement. The fuller of the two. |
| `PROGRESS.md` | This page. |
| `mapping.json` | The data behind the pictures below: every source file of the two builds, the row of the ladder it sits on, and what each build declares below the depots. |
| `make_print.py` | Draws the four print sheets from `mapping.json`. |
| `LADDER-FILLED.md` | The detail under the ladder: who writes each row, what each source would be on it, and each row with a mark for where every line comes from. |
| `CONTRACT-above-the-act.md` | Not in use. Sid declined that test on 3 October. |

The printed sheets are outside the repo, at
`/mnt/data/projects/research/softland/architecture-print-2026-10-05/`. That folder
also keeps a copy of the data and the script as they were when the sheets were made.
An earlier two-sheet print of 3 October is beside it, in
`builds-on-the-architecture-2026-10-03/`. The four sheets replace it.

## Now (5 October 2026)

On 2 October Sid and one session drew the backend from the depots down to resolve.
On 3 October Sid went up from the depot and the act, one layer at a time, and
named the layers above the act. Four sessions answered the same questions that
day. Sid compared them and found why the answers confused him. His words:

> I think we are having 2 conversations altogether .... one we are making the
> better abstractions and then the instances they appy to in the same diagram and
> same layer it is bound to fail every time ... now for this architecture we are
> doing a similarish kind of thing where I now have in my head .... Data sources
> (outside, inside inlet), adapters, converters, act, depots (streaming,
> microbatch)

So the work is now kept in separate pieces: the ladder alone, then the two
builds laid on it, then the detail. The print has one sheet for each.

The task Sid set, in his words of 3 October, relayed through another session.
One word is that session's guess: his dictation said "a D", written here as
"the door".

> the goal of what I'm trying to do is trying to map out what architecture we are
> building and what code currently we have. so that would give us the sense of
> what needs to be built in code, because currently we have adapters and as we
> were discussing that can be data, so we don't need to port that code over. what
> we might need is a converter layer that has some kind of input output attached
> to it. and then there is [the door], which goes to converting to an act and
> then going to different depots and then writing the topologies. so next task
> after figuring out what code is for what: carry on the conversation and try to
> figure out what are the different topologies we would need and why would we
> need them.

And his limit on the part below the depots, said the same day:

> dont expand on the architecture the one that we have not discussed basically
> below the depot …. But map out things as topologies and pstates based on what
> the code already declares as such in those 2

"Those 2" are the two builds: the server (`src/app/server`) and the instance,
which is Inland (`src-inland`) on the rig's store (`src/proposal/rig-2026-09-25`).

## The ladder

This is the drawing Sid pointed at on 3 October as the current architecture map.

```
 DATA SOURCES          outside                             inside  ◄────────────────────────────────────────────────┐
                   not Softland's                      Softland's own                                               │
                          │                                   │                                                     │
                          │ a thing, in the source's form     │ a thing, in Softland's form                         │
                          ▼                                   ▼                                                     │
 INLETS (g)         outside inlet                       inside inlet                                                │
                     moves it in                         notices it                                                 │
                          │                                   │                                                     │
                          │ what arrived                      │                                                     │
                          ▼                                   │                                                     │
 ADAPTERS        give it a shape (g)                          │ it has a shape already (g)                          │
                          │                                   │                                                     │
                          │ a thing with a shape              │                                                     │
                          └─────────────────┬─────────────────┘                                                     │
                                            ▼                                                                       │
 CONVERTERS                   take a shape, run a function                                                          │
                                            │                                                                       │
                                            │ an act, or a series of acts                                           │
                                            ▼                                                                       │
 ACT                           one form, whatever made it                                                           │
                                            │                                                                       │
                               ┌────────────┴────────────┐                                                          │
                               ▼                         ▼                                                          │
 DEPOTS                    streaming                microbatch                                                      │
                               │                         │                                                          │
                               └────────────┬────────────┘                                                          │
                                            └─ what is accepted lands as a fact, and a fact is an inside source ────┘
```

Who writes each row, as drawn: nobody writes a source. The inlets, the act's
form and the depots are code. The adapters and the converters are data. That
the adapters and converters are data is Sid's position of 2 October. It is not
ruled.

Two sentences of Sid's from 2 October that the middle rows rest on:

> can we just say anything above the outside inlet is an adapter that the user
> would define

> the converter is about taking a shape and running it through a function to
> output act or a series of acts

## How firm each part is

**Exact.**

- The line count of every file in `mapping.json`. Comments and blank lines are
  counted. `src/app/server/env.clj` was never opened.
- Every depot, topology, PState and query name. They are taken from the
  declarations in the code as it stood on 3 October.
- Which modules are deployed, from `bin/land` and the READMEs.

**An estimate.**

- Which row of the ladder a file sits on, and the split inside a file that
  sits on more than one row. One session read the READMEs under
  `src/app/server` and `src-inland`, the first sentence of each rig namespace,
  the line where each module starts, and one list of top-level definitions. It
  read no function bodies for this. Nothing was run.

**A guess. Each is marked (g) in the drawing.**

- The inlets are a row of their own. Sid's first item names both the sources
  and the inlets. They are split because nobody writes a source and we write
  the inlet.
- An adapter gives what arrived a shape and nothing more. Other readings: it
  also gives identity, address and basis; or an adapter is only a converter on
  the outside inlet, and the two rows are one.
- An inside thing has a shape already, so it skips the adapter.

What the comparison of the four sessions left about these rows:

- The four replies used "adapter" in three ways: the code that reaches the
  source, a data step before the converter, and a converter for the outside
  inlet. Sid lists adapters and converters as two things. His sentence below
  puts the adapter above the outside inlet and gives it to the user.
- A test from one session that judged the replies: if one author cannot write
  all of a box, it is two boxes. That is why moving a thing in, which is code,
  and giving it a shape, which is data, are two rows here.
- Another judge's line: an adapter's output comes back in as a fact, so a
  converter only ever sees inside things. That holds only if what arrived is
  recorded as a fact first, which is open question 5.

**A session's position. Sid has not ruled on any of these.**

- No written body touches the outside. To get or to do a thing outside, a body
  gives an act that asks for it. Code performs the ask after a gate accepts
  it, and what comes back arrives later. Two sessions reached this separately
  from the proposal's line "No leaf may read a clock, a random number, or
  anything mutable outside" (`frame-2026-09-15/PARTS-4-8.md:78`). Two other
  sessions read the same line the other way. If it is adopted, three things
  open: an accepted ask that a failed runtime does again, which runtime does
  an ask when two can, and what a grant covers when a base function changes
  under a converter.
- A thing is code only for one of four reasons: it reads what can change
  outside or acts there; it runs the data; it must hold when every fact is
  wrong or hostile; or a measurement or a guarantee demands compiled code. No
  reason on that list names a source.
- A grant is the permission that an asking act names. A gate examines it like
  each act.
- One body language for every tool, grown from Inland's.
- A body can live in three places, and the store offers all three. Steps over
  leaves, which is the only one that can be edited from inside. Foreign code
  as a value in a slot with a declared output, which `docs/decisions.md`
  already allows. A program on the host that offers acts through the door, as
  an actor with a permission. The third lets anyone bring any source at once,
  before the body language is grown.
- Base functions live in layers. A canonical one is a fact in the base. A fork
  is a version in a nearer layer, and the nearer layer wins. There is one
  store, so importing is not copying.
- The inward return has two parts. The fixed part says what arrived. The
  adapter says what it means.
- Security rests first on what a body cannot do. A body only gives claims and
  asks. Both are acts, both name a permission, and a gate examines both. So
  this holds for material that nobody reviewed. After that: the body's version
  is in the basis, so what stood on a bad adapter can be found; review is a
  policy on promotion into a shared layer; proofs come last. Proofs fit the
  laws of the fixed part, and canonical base functions, where a check runs on
  the host and its outcome comes back as a fact. Lean for the first, once the
  laws stop moving. Bend 2 as a foreign runner and as a checker. Neither
  inside the fixed part.
- Sign-in is the one adapter that a gate cannot contain, because it decides
  who the actor is. Its signature check stays code.
- A click is not what arrives. A button goes down and up, and a rule gives
  that pair a meaning. Today Inland listens to the browser's own click event
  (`src-inland/softland/inland/render.cljs:200`), and drag is not built.

## Both builds on the ladder

```
 one █ is 100 lines of code · ░ is parked or retired
 the server is src/app/server, 32,800 lines · the instance is Inland, 4,182 lines, on the rig, 16,257 lines

 ON THE LADDER
 INLETS            server    1,349  █████████████  file watchers and byte readers · git as a process · the HTTP listener · the CLI child
 move it in        instance    984  ██████████  the login host and socket · pointer, keys and hit-test · session cells · bells · git reads
 ADAPTERS          server    2,301  ███████████████████████  markdown and clojure cutters · transcript parse and redact · the stream-JSON parser
 give it a shape   instance    591  ██████  the revision reader: cuts a file at a revision into units
 CONVERTERS        server    5,849  ██████████████████████████████████████████████████████████  import builders · turns · facet masters · the door's handlers
 say what the      instance  1,749  █████████████████  admission kinds · records to facts · persons and sessions · repo material · citation steps
 act says          AS DATA          the instance only: 64 records in resources/inland/seed.edn and 4 citation tools as facts
 ACT               server    3,642  ████████████████████████████████████  the envelope · append and await the decision · the cluster handles
 envelope, door    instance  2,007  ████████████████████  the rig's door for each gate and for promotion · the envelope · Inland's connection

 BESIDE THE LADDER
 RUN BY            server    1,302  █████████████  route dispatch · cascade · the facet engine · binding resolution
 runs converters   instance  2,113  █████████████████████  Inland's evaluator and leaves · the rig's recipe executor and runner
 AN ASK            server    1,952  ████████████████████  the resident turn's CLI · model-run clients · prompts · the code analyzer
 what leaves       instance    137  █  the resident, handed one activity
 DATA AS CODE      server    1,802  ██████████████████  worn's eight material specs · binding rows · the verb registry
 grammars          instance    106  █  the grammars for records, cells and notes

 BELOW THE DEPOTS
 GATES AND RECORD  server    7,839  ███████████████████████████████████████████████████████████████░░░░░░░░░░░░░░░░
                   instance  6,088  ███████████████████████████████████████████████████████████░░
 READS AND SHOWN   server    6,764  ████████████████████████████████████████████████████████████████████
                   instance  6,174  ██████████████████████████████████████████████████████████████
 PROBES            instance    490  █████
```

What the count shows:

- About 8,150 lines of the server are adapters and converters. Under Sid's
  position they would be data, so they would not be ported as code. With the
  1,800 lines of data that is written as code today, that is about 30% of the
  server.
- The instance is the only build with converters as data today: the 64 seed
  records and the 4 citation tools.
- 22 of the server's 49 files sit on more than one row. `door/server_jetty.clj`
  sits on six.
- A fifth of the server and almost a third of the instance is reads. The
  ladder draws the way in and has no row for the way out.

The files behind each number are in `mapping.json`, under `builds`.

## Below the depots: what the code declares

No architecture is added here. This is only what the two builds declare. Every
name is in `mapping.json`, under `modules`.

- **The server:** 8 modules, one for each kind of data. 17 depots, 5 stream
  topologies, 2 microbatch topologies, 12 query topologies, 84 PStates.
  `bin/land` deploys five: `object-container-module`,
  `object-container-transcript-ops-module`, `relation-kernel-module`,
  `trail-view-module`, `face-arsenal-module`. `llm-module` runs only in an
  in-process cluster. `transcript-module` and `TranscriptIngestModule` are
  parked.
- **The instance:** one module in use, `rig.store.module/Store`. 5 depots, the
  stream topology `"gate"` with two sources, the microbatch topology `"micro"`
  with three sources, 6 PStates, 21 query topologies. Inland's own module,
  `softland.inland.module/material`, is retired.

## Topologies: where two sessions landed

Sid asked which topologies the architecture needs, and why. Two sessions worked
on it on 3 October and checked each other. This is their position. Sid has not
ruled on it. It also rests on D1 and D2 in `ARCHITECTURE-STE100.md`, which are
still open.

Needed:

1. **The stream gate.** One event on the layer's task, and an answer in
   milliseconds. A layer needs one order.
2. **The microbatch gate.** All tasks, exactly once, a batch later. For a layer
   that outgrows one task.
3. **Resolve.** A few query topologies, cut by kind of read and not by kind of
   data. It is the one exit for reads. If the record is private, the family
   also carries the answer by name, the door's read of a layer's class, and the
   operator's export.
4. **A reactor on a feed.** Open. It exists only if something in the module
   must react to a landed act.

What looks like it needs a topology and does not. The first five are how the
rig does it; the other session read them there. The paths are under
`src/proposal/rig-2026-09-25/src/`:

- **Index rebuilds.** A second source on each gate, because the gate owns the
  PStates (`rig/store/module.clj:159`, `rig/store/micro.clj:1861`).
- **The tick that moves the microbatch frontier.** A source on the microbatch
  gate (`rig/store/micro.clj:1513` and `:1521`).
- **The fan-out for locks and forget.** Inside the stream gate's event
  (`rig/store/module.clj:156`).
- **A promotion's landing.** The source gate appends it to the other acts
  depot.
- **Read entries.** They are acts in the reader's hand layer, so the stream
  gate decides them.
- **Asks and arrivals.** The runtime does them. A claim between two runtimes
  is an expected-version check at a gate. This is the other session's
  position.
- **One topology for each kind of data.** None.

Two notes that go with the list. An increment inside a gate's own event is
safe, because the event is atomic with the answer written under the act's
name, and a repeat finds that answer first. The unsafe increment is one made
after a hop to another task. And the operator's jobs are topologies that
someone writes once: the copy into a new module to go past N tasks, and
excision by a module update. They are not standing.

What the server shows about this:

- Its eight modules would not each become a topology. Under "no table per
  kind" they collapse into the two gates and resolve.
- It already has one reactor. The object container sends source-line
  completions through an internal depot, and the transcript-operations module
  reads them through a mirror to advance file offsets
  (`src/app/server/rama/README.md`). If that state becomes facts an adapter
  keeps, something must still react to an accepted fact.
- Its object-container stream topology writes index rows on other partitions
  with no transaction. That goes away only if every index of a stream layer
  sits on the layer's own task.
- It has no tick depot. All work driven by time is in clients.

One question was left for Sid: must the store ever answer "what stands on
this" beyond the asker's own context? Two cases sit outside any reader's
context. One is forget, if it must reach a fact that only stood on a forgotten
value. The other is the size of a change before a promotion into a shared
layer, which the server answers today from what the caller supplies
(`src/app/server/worn/README.md`). If the answer is no, the needed set is the
two gates, resolve with one query over a context, and the open reactor.

## Found against the architecture file, and not resolved

- `ARCHITECTURE-STE100.md` section 3.2.4 says Bend is not a tool for proofs.
  That is true of the first Bend only. Bend 2 has dependent types, proofs that
  its compiler checks, and a termination check. Its own documents say it is
  young. Read on 3 October at `github.com/HigherOrderCo/Bend` and
  `bend2.dev/notes/what-is-bend2/`.
- The picture keeps "other instances" as a source. Sid's ruling of 18 September
  says there is one store (`frame-2026-09-15/LEDGER.md:201`).
- `docs/decisions.md` says "one executor with several runners". Three exist,
  with three vocabularies: `src/app/client/engine/executor.cljc`,
  `src-inland/softland/inland/total.cljc` and the rig's
  `src/rig/store/recipe.clj`.
- `docs/decisions.md` promises a work budget for each read. Inland's evaluator
  examines sizes after the run (`total.cljc:6`).
- `docs/decisions.md` says the stroker, the shapes and layout are rows. In the
  client they are compiled functions (`src/app/client/path/component.cljc:143`,
  `path/source.cljc:38`).
- The picture says Google signs a person in. Inland has a password login
  (`src-inland/softland/inland/people.clj:114`). The server has no sign-in.

## Open, and Sid's to answer

About the ladder:

1. Are the inlets a row of their own?
2. Where does an adapter end: after the shape, or after identity, address and
   basis?
3. Does an inside thing ever pass through an adapter?

One level down:

4. May a body touch the outside directly, or only ask with an act?
5. Is what arrived handed on as a value, or recorded as a fact first? Does the
   store keep the bytes, or only their hash?
6. Is the one body language grown from Inland's?
7. Whose session does an ingestor run in?
8. Must the store ever answer "what stands on this" beyond the asker's own
   context?

Already waiting in `ARCHITECTURE-STE100.md` section 9: D1, D2 and the open
items ?2 to ?16.

## Next

By Sid's words above: use the map to see what must be built in code, given
that adapters can be data and are not ported; then settle which topologies are
needed and why. The first three open questions come before both, because they
fix the rows.

Sid declined one test on 3 October: writing an adapter and a converter out as
data to see if it can be done. `CONTRACT-above-the-act.md` is that test's page.

## The print

Four A4 landscape sheets, in three levels. Each level answers one question,
and all use the same rows.

- Level 1: the ladder alone.
- Level 2: how much code sits on each row.
- Level 3, two sheets: which files sit on each row, and what each build
  declares below the depots.

To draw them again after a change to `mapping.json`:

```
python3 make_print.py <output folder>
google-chrome --headless=new --no-pdf-header-footer --print-to-pdf=<output folder>/architecture-three-levels.pdf "file://<output folder>/architecture-three-levels.html"
```

## Sources

- Sid's rulings: `git show main:src/proposal/frame-2026-09-15/PROGRESS.md`, the
  "Now" section.
- Sid's proposal: `src/proposal/frame-2026-09-15/PICTURE.md` and `PARTS-4-8.md`.
- The settled ground: `docs/decisions.md`.
- The server's own maps: `src/app/server/README.md` and each folder's README.
- Inland's maps: `src-inland/README.md` and `src-inland/softland/inland/README.md`.
- The rig: `src/proposal/rig-2026-09-25/`.
- The session records of 3 to 5 October 2026. They hold the comparison of the
  four sessions and the exchange on topologies. Neither is written as a
  document; what they left is on this page. The wide drawing with a source
  mark on every line is now `LADDER-FILLED.md`.
