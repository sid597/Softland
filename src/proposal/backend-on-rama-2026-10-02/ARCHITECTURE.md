# The intended backend on Rama, as drawn on 2 October 2026

This is the architecture one session drew with Sid, written down so that a fresh
session can recheck it. It is not a ruling and not a plan. Every part says where
it comes from, so each claim can be checked against its source and challenged.

It was drawn from Sid's proposal (`src/proposal/frame-2026-09-15/PICTURE.md`,
`PARTS-4-8.md`), his rulings (the "Now" section of that folder's `PROGRESS.md` on
main), `docs/decisions.md`, Rama's docs (`reference/rama/docs/`) and the Rama check
(`src/proposal/rama-check-2026-09-25/RESULTS.md` on main). The two builds, the
server (`src/app/server`) and Inland on the rig (`src-inland/`,
`src/proposal/rig-2026-09-25/`), were used as examples, never as the answer.

Where things come from:

- **ruled**: Sid's rulings, as recorded on main.
- **proposal**: Sid's proposal.
- **Sid, 2 Oct**: said by Sid in this session. Only what he agreed to is marked
  agreed; his positions are marked as positions.
- **Rama**: what Rama requires for the above, from its docs or the Rama check.
- **derived**: this session's reasoning. Recheck these first.
- **?n**: open. The list is at the end.

## Vocabulary used here

- **act** for what was earlier called an offer (Sid, 2 Oct: "lets call the offers
  acts so we don't have 2 names for same thing"). An act can be refused; its
  answer says whether it landed.
- **stream layer** and **microbatch layer** (Sid's words, 2 Oct): a layer whose
  acts the stream gate decides, kept whole on one task, or one the microbatch gate
  decides, spread over every task by entity.
- **act converters** (Sid's word, from his sketch): what turns something that
  happened into acts.
- **key** is a fact's key; **lock** is an encryption key (ruled vocabulary).
- Retired in this session: "offer", "whole" and "spread", "partitioning" for the
  choice of depot, and "shared" and "one-owner" as the thing that picks a gate.

## 1. The store and the runtime

```
  OUTSIDE       screens · minds (people, models) · the host (git, builds, disks) · other instances
                  gestures, replies, commits, ticks ▼          ▲ paint, prompts, enactments, sends
  ─────────────────────────────────────────────────────────────────────────────────────────────────
  THE RUNTIME   sign-in        Google says who you are · ?4 who you are inside the store
  the one tool  inside door    acts from tools: what a gesture means, a save, a rule firing
  that is not   inward return  gives what comes from outside an identity, an address, a basis
  a fact; its   every act      named here · who · layer · permission · basis · expected versions
  version is                   ① what it says: adds, or names the version it replaces
                               ② which layer it goes into                  (both: the tool's)
                               ?3 its values sealed before any depot keeps them
                               ?7 its other open parts
                the door       reads the layer's kind and appends to that kind's depot;
                               the act's name carries its layer and its kind
                crossings out  paint · enact · prompt · send
                match, tools   ?2 where they run: in the module, in the runtime, on a budget
                converters     data: adapters for the outside, tools for the inside, built on
                               base functions (data) and primitives (code); see the second picture
                the only Rama client, so no read goes around resolve
                      │ append                  │ query                     ▲ proxies push changes
  ─────────────────────────────────────────────────────────────────────────────────────────────────
  ONE MODULE    ③ each layer is a stream layer or a microbatch layer: a fact on the layer, set
  N tasks         by its members; it sticks, and it can be re-classed · ?5 what each starts as
                                   ┌──────────── promotion ─────────────┬───────────────────────┐
                                   ▼                                    ▼                       │
                DEPOTS        the stream gate's depot         the microbatch gate's depot       │
                              hashed by the layer's id        hashed by an entity's id (?7)     │
                                   │                                    │                       │
                                   ▼                                    ▼                       │
                TOPOLOGIES    stream gate                          microbatch gate              │
                              one event on the layer's             one batch across all         │
                              home task, at least once;            tasks, exactly once;         │
                              the answer on the ack, in ms         decided before it's          │
                                                                   visible, a batch later       │
                              both check shape, expected version, policy, and the act's kind    │
                              against the layer's · only they write                             │
                              promotion: the source layer's gate reads the value out, writes a  │
                              crossing fact, and sends the new act to the target's depot ───────┘
                                   │                                    │
                                   ▼                                    ▼
                PSTATES       STREAM LAYERS                        MICROBATCH LAYERS
                              each whole on its home task          each spread by entity
                              ?1 the record                        ?1 the record
                              answers by name · current versions · permissions · grammars ·
                              indexes, in both
                              locks: own rows for personal and hand values, inside the record
                              for the rest
                              facts of every kind in the same PStates: no table per kind
                              every person's lock on every task · a clock per task
                                   │                                    │
                QUERY              └────────────────► resolve ◄─────────┘
                TOPOLOGIES    the one exit: who may see what · nearest layer wins, whatever
                              its kind · a status on every read · every read leaves a basis
                              entry with each layer's moment · ?6 line those moments up?
```

Where each part comes from:

- **ruled:** two gates, stream and microbatch; their hashing, by layer and by
  entity; N fixed at launch; promotion as two acts; every act named before the
  gate and naming its permission; answers found by name; permissions kept in the
  layer they govern; a lock per value, wrapped under the people it is about; the
  clock's two promises; the act's parts (section 3).
- **proposal:** the runtime as the one tool that is not a fact; the inside door and
  the inward return; crossings out; the gates check shape, expected version and
  policy, and only they write; resolve as the one exit, with a status and a basis
  entry on every read; no table per kind.
- **Sid, 2 Oct, agreed ("3 switches only"):** ① what an act says and ② which layer
  it goes into are the tool's, per act; ③ whether a layer is a stream layer or a
  microbatch layer is a fact on the layer, set by its members; it sticks, and it
  can be re-classed.
- **Sid, earlier, agreed in chat but not ruled:** one module.
- **Rama:** whatever the stream gate checks must sit on its own task, so grammars
  and current versions live in the layer like permissions do, and every person's
  lock is copied to every task. Resolve is query topologies, and the runtime holds
  the only Rama client, because Rama lets any client read any PState. Proxies are
  Rama's only push to a watcher.
- **derived:**
  - **Two depots, one per gate.** By Rama's rule for choosing depots: the two kinds
    of layer need no order between them, and each gate would only throw the
    other's acts away. Rama does allow one depot with a class-aware partitioner,
    so two is a choice, not forced.
  - **The door picks the depot (step 1) from the layer's kind;** Rama's hash picks
    the task (step 2). Rama never routes between depots. The runtime appends
    straight to the gate's depot, because the ack only waits for topologies
    reading the depot appended to.
  - **One layer, one gate.** Acts in one layer can't be split between the gates:
    that would give the layer two orders, and Rama lets only one topology own a
    PState. An act picks its gate by picking its layer (switch ②).
  - **An act writes one layer.** A link between two canvases is one fact in one
    layer, and the other side sees it by reading.
  - **Streaming's only cost is scale:** a stream layer never uses more than one
    task's thread. Microbatch is for scale, not for sharing.
  - **Resolve reads stream and microbatch layers the same way** (nearest wins).
    Only each layer's moment differs (?6).
  - **Promotion runs between any two kinds:** the source layer's gate reads the
    value out, and a new act goes to the target layer's depot. Sid's ruling
    describes the stream case; the microbatch case is this session's
    generalisation.

## 2. Before an act: from where things happen down to the depots

The seven sources are examples, not a list. Read it top to bottom; each band is
one layer.

```
WHERE THINGS HAPPEN     THE SCREEN                     THE STORE ITSELF               THE HOST'S FILES               MINDS                          THE HOST'S WORK                GOOGLE                         OTHER INSTANCES                 ▲ CROSSINGS OUT
any source; these seven people: click · drag ·         a fact lands that some         git · code · md ·              models · Claude CLI ·          builds · checks · the          proves who you are             other copies of the             │ paint   → the screen
are examples, not a listtype · hover · scroll          rule is waiting for            logs · transcripts             agents                         clock                                                         place                           │ prompt  → minds
                                    │                              │                              │                              │                              │                              │                              │                   │ enact   → the host's work
                                    │                              │                              │                              │                              │                              │                              │                   │ send    → other instances
                                    ▼                              ▼                              ▼                              ▼                              ▼                              ▼                              ▼                   │
WHAT ARRIVES            local signals: never facts     a new fact, of some kind       files and lines, with no       a reply, with no reads         an outcome, or a moment        a signed token                 facts made elsewhere            │
                        until a rule gives them                                       ids and no basis               of its own                                                                                                                   │
                        a meaning                                                                                                                                                                                                                 │
                                    │                              │                              │                              │                              │                              │                              │                   │
                                    │                              │                              │                              │                              │                              │                              │                   │
                                    ▼                              ▼                              ▼                              ▼                              ▼                              ▼                              ▼                   │
ACT CONVERTERS          ┌─ THE INSIDE INLET: tools, for the inside ─────────────────┐ ┌─ THE OUTSIDE INLET: adapters, written by whoever brings the source; they make the identity, address and basis the outside doesn't carry ─────────────┐    │
all data                                                                                                                                                                                                                                          │
                        EVERY CONVERTER, EITHER INLET:  data  ·  found by matching what arrived against patterns, which are facts  →  runs  f( what arrived, what it reads from the store )  →  yields claims:                                    │
                        zero, one or many, each into one layer  ·  what f read becomes their basis  ·  a series is named in full before its first act is sent                                                                                     │
                                                                                                                                                                                                                                                  │
                        the pointer: the hit-test turnsmatch (?2): the rules whose    ingest: cuts files into        the reply return: its basis    outcomes: check passed at      sign-in: the first time, a     federation: their facts come    │
                        the point into an address:     patterns demand that kind      things, mints their ids,       is what the mind was shown,    revision R; the running        person (?4) and their own      in as acts, with provenance     │
                        which thing, at which version  a tool, a fact, yields what    keeps continuity by name       recorded when it was sent      revision is now X              layer; every time, a session:  made at this door               │
                        a rule, a fact, decides what   the act says                   and hash; basis: the commit                                                                  it starts, and its                                             │
                        the point means here                                                                                                        a tick: time, as a claim       permissions are granted;                                       │
                        a tool, a fact, yields what    also: a long activity          transcripts: a thread is an                                   from outside                   which adapters may start a                                     │
                        the act says                   reporting its status           entity, each turn an act                                                                     session is policy, a fact                                      │
                                                                                                                                                                                                                                                  │
                        also from the screen:                                                                                                                                                                                                     │
                        an editor's save; the reader,                                                                                                                                                                                             │
                        recording what each screen                                                                                                                                                                                                │
                        was shown                                                                                                                                                                                                                 │
                                                                                                                                                                                                                                                  │
  built on                                                                                                                                                                                                                                        │
  base functions · data base: the screen's rules       base: matching by kind         base: git's format, parsers    base: HTTP, JSON, model APIs   base: build, check protocols   base: OAuth, token checks      base: the federation protocol   │
  primitives · code     prim: input, hit-test          prim: the store itself         prim: files, bytes             prim: network bytes            prim: processes on the host    prim: network, cryptography    prim: network bytes             │
                                    │                              │                              │                              │                              │                              │                              │                   │
                                    │                              │                              │                              │                              │                              │                              │                   │
                                    └──────────────────────────────┴──────────────────────────────┴──────────────────────────────┼──────────────────────────────┴──────────────────────────────┴──────────────────────────────┘                   │
                                                                                                                                 │  claims, from every converter in both inlets                                                                   │
                                                                                                                                 │                                                                                                                │
                                                                                                                                 │                                                                                                                │
                                                                                                                                 │                                                                                                                │
                                                                                                                                 │                                                                                                                │
                                                                                                                                 │                                                                                                                │
                                                                                                                                 │                                                                                                                │
                                                                                                                                 │                                                                                                                │
                                                                                                                                 ▼                                                                                                                │
THE DOOR                every act, whatever made it, gets the same envelope:  a name (random, tagged with its layer and kind) · who · layer · permission · basis · expected versions · subjects                                                   │
(one, for every act)    ① what it says and ② which layer come from the converter;  the door reads that layer's kind ③,  ?3 seals the values,  and appends to the depot of that kind  (step 1 below)                                               │
                                                                                                                                 │                                                                                                                │
                                                                                                                                 ▼                                                                                                                │
ACT                                                         {:name        <random, tagged with its layer and kind>          made by the door before the gate (ruled)                                                                              │
its shape                                                    :layer     ★ <the layer's id>                                  what the stream gate's depot hashes (ruled: the layer is on the act)                                                  │
                                                             :who         <the actor>                                       (ruled)                                                                                                               │
                                                             :permission  <a permission kept in that layer>                 (ruled)                                                                                                               │
                                                             :based-on    [<each read, at its version>]                     = what the converter read (ruled: once, on the act)                                                                   │
                                                             :subjects    #{<the people it's about>}                        (ruled: on the act)                                                                                                   │
                                                             :session  :because-of  :claimed-when  :expected  :version      ruled parts; on the act or on every fact: ?7                                                                          │
                                                             :facts       [[entity key value replaces] ...]                 ① what it says (ruled core)                                                                                           │
                                                                           ★ the first fact's entity}                       what the microbatch gate's depot would hash (?7)                                                                      │
                                                            the gate adds its stamp, and writes its answer under the act's name                                                                                                                   │
                                                            ★ = what a depot hashes                                                                                                                                                               │
                                                                                                                                 │                                                                                                                │
                                                                     step 1 · which depot:  the door reads the layer's kind  ③   │  a fact on the layer, set by its members  ·  ?5 what each layer starts as                                      │
                                                                   ┌─────────────────────────────────────────────────────────────┴─────────────────────────────────────────────────────────────┐                                                  │
                                                                   ▼                                                                                                                           ▼                                                  │
DEPOTS                                                  the stream gate's depot                                                                                                   the microbatch gate's depot                                     │
                                                        a stream layer's acts                                                                                                     a microbatch layer's acts                                       │
                                                        step 2 · which task: Rama hashes :layer,                                                                                  step 2 · which task: Rama hashes an entity's id,                │
                                                        so a layer's acts all reach its one task                                                                                  spreading the layer over every task (?7 which                   │
                                                                                                                                                                                  entity, when one act is about several)                          │
                                                                                                                                                                                                                                                  │
                        then the gates decide, the stores keep, resolve reads, and the runners and crossings out act on what was read · a promotion comes back into a depot as a new act ─────────────────────────────────────────────────────────┘

BASE FUNCTIONS          data: the canonical libraries you write first; anyone imports or forks them, the way open source works
PRIMITIVES              code, ours, small and generic: bytes in and out · files · processes on the host · the GPU · the clock · randomness and cryptography · the screen's input and hit-test
                        the only code above the door: nothing specific to one source ever needs code · each primitive runs under a grant and records what it read
                        one is added only with a receipt, a measurement or a guarantee that needs it; how often that happens is the gauge
```

Where each part comes from:

- **Sid, 2 Oct, agreed:** the components above the act, from his own sketch:
  sources, what arrives, act converters, the act, the depots.
- **proposal:** two inlets and one gate; the inward return makes identity, address
  and basis; "every crossing is owned by the fixed part and governed by facts".
- **ruled:** a series of acts is named in full before its first act is sent; an act
  writes one layer; the act's parts.
- **derived:** the converter's form. It is found by matching, it runs
  f(what arrived, what it reads), and it yields claims whose basis is what f read.
- **Sid, 2 Oct, his position, not ruled:**
  - every converter is data, including outside adapters, written by whoever brings
    the source;
  - the base functions are data: canonical libraries Sid writes first as Softland's
    first user, which anyone imports or forks, "like open source";
  - code is only the primitives (small and generic), the evaluator, and the
    guarantees (gate, resolve, record, locks and forgetting, budgets, grants);
  - security comes first from capabilities, then from reviews and proofs (Sid
    named Lean and Bend).

  This reopens a line of `docs/decisions.md`, "Nothing in a record is a program".
  That line's own escape clause applies: "the leaves gain a designed total
  language, never runtime code, asked of Sid then". See ?8.

## 3. The act

```
{:name        <random, tagged with its layer and kind>          made before the gate (ruled)
 :layer     ★ <the layer's id>                                  what the stream gate's depot hashes (ruled: on the act)
 :who         <the actor>                                       (ruled)
 :permission  <a permission kept in that layer>                 (ruled)
 :based-on    [<each read, at its version>]                     what the converter read (ruled: once, on the act)
 :subjects    #{<the people it's about>}                        (ruled: on the act)
 :session  :because-of  :claimed-when  :expected  :version      ruled parts; on the act or on every fact: ?7
 :facts       [[entity key value replaces] ...]                 ① what it says (ruled core)
               ★ the first fact's entity}                       what the microbatch gate's depot would hash (?7)

the gate adds its stamp, and writes its answer under the act's name       ★ = what a depot hashes
```

Three tests decide any part of an act (derived):

1. **Where it goes:** a depot's partitioner sees only the act, so whatever decides
   its task must be in the act.
2. **What the gate checks:** the gate decides each act alone, in one event on one
   task, so whatever it checks must be in the act or on that task.
3. **What can't be found later:** this is Sid's card, "an event individually and
   the log collectively should have all the information that it needs to collect
   at the time it is invoked".

## Open

- **?1 where the event log lives:** in the gate's PState, in a depot of decided acts
  the gate publishes, or in the PState with a feed beside it.
  - The session's position: a depot of decided acts, because it is Rama's source
    of truth, Rama never rewrites it, and replaying it gives the same state,
    including past N tasks.
  - This holds only if one Rama run shows that a depot append made inside a stream
    event that then fails is dropped along with it. If it isn't dropped, take the
    PState with a feed.
- **?2 where match and tools run.** Hangs on ?1: reacting inside the module needs a
  depot after the gate.
- **?3 whether values are sealed before any depot keeps them.** Required if ?1 is a
  depot of decided acts.
- **?4 what identifies a person inside the store.** Opaque ids with one erasable
  link become necessary if the record can't be rewritten.
- **?5 what each layer starts as.** The session's position: every layer starts as a
  stream layer, the base included, and is re-classed when it outgrows one task.
  - This would change the 24 September line in which a promotion into the base
    crosses to the microbatch gate.
  - The rig's default differs: the base is a stream layer until the first group,
    then re-classed.
- **?6 whether one read lines up the moments of the layers it read.**
- **?7 the act's other open parts:**
  - where "because of", session, "claimed when", expected versions and the version
    marker sit;
  - the stamp's form;
  - what a fingerprint covers;
  - which entity places an act in a microbatch layer;
  - how ingest makes entity ids.

  The rig and the instance took defaults for these. None is ruled, and they freeze
  at the first kept record.
- **?8 a designed total language, so converters and base functions can be data.**
  - The session recommends opening it, with three conditions: grow it from the
    recipe records that exist; let the first real library, the repo converter,
    drive it; and start it after the act's shape and ?1.
  - The risk named: a language is where a project can disappear. It should only
    grow when a real library needs something.

## Corrections made during the session

- The base's kind was reported as Sid's decision. It wasn't; it is ?5.
- "Partitioning" had been used for two different mechanisms: which depot (step 1)
  and which task (step 2). They are now separate.
- An earlier reason for two depots, "a depot has one partitioner", was too strong.
  The real reason is in section 1.
- Earlier drawings let owner count pick the gate (one-owner meant stream, shared
  meant microbatch). It is now the layer's kind, a fact set by its members.

## Recheck these first

The claims the session was least sure of:

1. **Not run:** whether Rama drops a depot append made inside a stream event that
   then fails. ?1 depends on it.
2. **From docs, not run:** that every microbatch layer waits on the slowest task in
   each batch.
3. **The throughput numbers' scope.** They come from the rig's bench: an in-process
   cluster, one copy, small acts, Sid's machine. One layer on one task took about
   300 acts a second from one waiting writer, and 2,000 to 3,400 with 16 to 32
   writers (`src/proposal/rig-2026-09-25/BENCH_NOTES-stream.md`).
4. **Resolve reading a whole context in one query topology.** This is a design, not
   built; the instance reads one layer per query.
5. **An inference:** ruling 7 (own lock rows for personal and hand values, locks
   inside the record for the rest) reads as if the record can't be touched.
6. **The mapping of the server's parts onto this architecture.** It comes from
   `src/app/server/README.md` and the module declarations; nothing was run.

## Sources

- **The proposal:** `src/proposal/frame-2026-09-15/PICTURE.md` and `PARTS-4-8.md`.
- **Sid's rulings:** `git show main:src/proposal/frame-2026-09-15/PROGRESS.md`, the
  "Now" section.
- **Settled ground:** `docs/decisions.md`. "Tools are records over a vocabulary"
  holds the line ?8 reopens.
- **The Rama check:** `git show main:src/proposal/rama-check-2026-09-25/RESULTS.md`.
- **Rama's docs:** `reference/rama/docs/`, especially `14-depots.md` (choosing
  depots, partitioners, ack levels), `11-stream-topologies.md` and
  `19-operating-rama.md` (task scaling, deterministic replay).
- **The rig:** `src/proposal/rig-2026-09-25/` (`RIG.md`'s unruled defaults,
  `src/rig/store/module.clj`).
- **The server:** `src/app/server/README.md` and its module declarations.
- **The stretch's handover:** `/mnt/data/projects/research/softland/handover-2026-09-29/`
  (`HANDOFF.md`, `THREE-ARCHITECTURES.md`, `FACTS.md`, `WHERE-SID-IS.md`).
