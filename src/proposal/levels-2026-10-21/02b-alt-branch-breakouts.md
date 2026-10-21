# Round 2b (Alt Branch): Depot Correction & Initial Breakouts

## Prompt



<pasted_content id="978c">
lets fan out and explore a few threads deeply before converging—like the few-many-few shape of a neural net. since we are collaborating rather than running autonomously, dont filter out intermediate abstractions or competing metaphors just to tie neatly back to the first question.

specifically i want to unpack three directions with you:

1. virtual machine stack instead of a plain box:
what if we frame my "box" concept more like a classic computer systems stack—where hardware gates build up to an ISA, then the OS kernel, then high-level runtimes—where every level acts as a virtual machine for the level above it with its own distinct vocabulary that compiles down? how would that virtual machine vocabulary map to Softland?

2. deriving the store layers from scratch:
is the store actually one layer or multiple? lets derive the store levels Softland needs from first principles based on our vision and settled decisions rather than existing code. we can define each level by the specific invariants it guarantees (what it lets the upper levels stop worrying about)—starting at rock bottom with a durable, append-only ordered log that handles atomicity and sequence, and building up from there.

3. fractal composable layers + intentional leaky abstractions:
i use the word "layer" fractally—a layer can contain sub-layers all the way down and up. for example, an editor is a layer; inside that you have code, markdown, and plain text editors; inside those you have vim, vscode, and emacs paradigms. a user can mix pieces from different ones into their own custom editor layer, which others can then build on top of. 

at the same time, some abstractions need to leak intentionally to give users a spectrum of control. for instance, someone typing actually cares whether an operation commits immediately on each action or gets processed in a batch. instead of hiding that, how do we expose those lower-level knobs cleanly as promises in the upper layer's own vocabulary across a fractal stack?
</pasted_content id="978c">


---

## Reply (Verbatim)

A correction to my last reply comes first. I said the depot words carried Rama into the act-id. The 6 October decisions show that `:streaming` and `:microbatch` are words you decided yourself ("my streaming and microbatch"), and that day's notes already priced what changing them would cost. "Class" was the slot's name until 9 October, when it became "depot". So the question is narrower than I made it: do those two words name a promise (how soon a layer's acts are decided) or a mechanism (which Rama topology decides them)? As promise words they are fine. Thread 3 picks this up.

Now the fan-out. Each thread stays open, and where threads touch I note it and leave it. Nothing is written to disk. The outside precedents (IBM, Smalltalk, STEPS, Nix and others) come from my memory of them; I have not re-read them here.

---

# 1. The box as a stack of machines

## 1.1 The plain mapping

| Computer level | Its words | Softland candidate | Its words |
|---|---|---|---|
| Gates, devices | wire, clock, disk | machines, JVM, network | (outside the land) |
| Microarchitecture | pipeline, cache, datapath | Rama | depot, partition, topology, PState |
| Memory | address, load, store | the store's lower levels | act, fact, place (thing + key), read, live read |
| OS kernel | process, address space, user, permission, syscall, scheduler | the store's upper levels, plus the door and read path | layer, builds on, who, see / act, act and read as the "system calls"; what wakes a record (the 16 October "matcher", not built) as the scheduler |
| CPU and its ISA | instruction, register, primitive op | the executor and the record language | record, step, capability, budget, run |
| Microarchitectures of one ISA | 360/30 and 360/75 run the same code | runners | the CPU walker, the GPU tier, the in-store derivation |
| Libraries, languages | function, module, package | vocabularies of records | kinds, names, grammars |
| Applications | document, window, tool | tools, views, editors, converters | entity, component, view |
| User | | inhabitants | people, agents |

Several rows fit closely. "One executor with several runners" (decisions.md) is exactly IBM System/360's move: one instruction set, many machines that implement it. The budget on a record plays the role of a time slice, or of "gas" in a blockchain VM: every step costs something, and running out stops the run cleanly. Refusals as data play the role of error codes returned as values.

## 1.2 The order is not the classic one

In a Unix-style computer, the kernel is *software above* the instruction set. In Softland, the kernel's jobs (who may, which layers you see) live *below the waist*, inside the store. Records never see an unprotected world.

The closest real machine to this is IBM's System/38, later the AS/400:
- One persistent address space: there is no difference between memory and disk.
- Security and storage live *below* its machine interface, in licensed internal code.
- Programs are kept in a technology-independent form and translated to whatever hardware runs them. In 1995 IBM moved the line from one processor family to another without customers recompiling their programs.

The Softland parallels:
- The store is the single-level, protected memory.
- The record language is the technology-independent interface.
- The runners are the translations.

So the stack, read as an access path:

```
  inhabitants
      │ use
  tools · views · editors · converters        records, composed: layers on layers
      │ defined in
  ══ the record language + capabilities ══    the waist: Softland's "ISA"
      │ run by
  EXECUTOR ("CPU")                            runners = implementations of that ISA
      │ act / read                            the bus; the "system calls"
  LAYERS & STANDING ("kernel, below the ISA") who may; which layers; what wakes what
  FACTS · DECISIONS · LOG ("memory")          persistent, shared, transactional
      │
  Rama                                        the memory's microarchitecture
```

There is a twist: two different kinds of machine sit in this stack. One remembers (the store) and one computes (the executor). The executor doesn't *run on* the store the way a program runs on a VM; it *uses* the store, the way a CPU uses memory. So the honest picture is a CPU and a memory with an OS between them, more than a single tower of interpreters.

## 1.3 "Compiles down" is a family, not one operation

At each boundary in Softland, a higher thing becomes a lower thing in a different way:

| Boundary | How it goes down | Example |
|---|---|---|
| word → record | **defined**: a higher word is a record over lower words | "delete a word" written as steps over text operations |
| record → capabilities | **run**: interpreted (CPU walker) or compiled (GPU tier) | a brush record compiled to a shader |
| read → places | **resolved**: walk the layer list, nearest fact wins | your layer over the base |
| facts → tables | **derived**: accepted facts kept as current-state tables | Rama's PStates |
| entries → answers | **decided**: one step per arrival, the same every time it is run | the gate |
| act → log entry | **wrapped**: the door's own declaration around the maker's act | act-id, class, seal, time |

Treating all of these as "compilation" hides why each one fails differently. A definition can break when a lower word changes. A resolution gets slower as chains get deeper. A decision must be the same every time it is run. A wrap must never be visible from above.

## 1.4 Where interpretation stops: the real tension in this frame

Taken literally, the VM metaphor says every level is a machine with its own *language*, interpreting the level above. Followed all the way up, that gives a tower of languages on languages: Lisp's reflective tower (3-Lisp, where each level is interpreted by the one below and a program can reach up and change its interpreter), or Alan Kay's STEPS project at VPRI. STEPS tried to build a whole personal computing system in about 20,000 lines by giving each level its own small language.

Softland has independently arrived at three of STEPS's ideas:
- **OMeta**: parsing any format by a grammar. This matches "a format is a grammar".
- **Nile and Gezira**: a 2D renderer described as data streams in a few hundred lines. This matches "geometry generators are data".
- **Worlds**: a scope where changes stay local until committed to the parent. This matches a working layer against a base.

But the 6 September ruling went the other way on depth: "Nothing in a record is a program", one executor, and a new language only "if a record provably cannot say a needed tool twice over". So Softland is **shallow in interpretation and deep in composition.** Below the waist there are a few real machines. Above it there is one language, and "the layers are infinite because a record's output is a value any record can take as input".

So the VM frame holds below the waist. Above the waist it turns into *libraries in one language*: each layer has its own **vocabulary**, defined in terms of lower vocabularies, while the **language** stays one. If we adopt the VM metaphor naively, it quietly reopens the 6 September ruling. Whether to reopen it is your call; a metaphor shouldn't make it.

## 1.5 Competing metaphors

| Metaphor | What it shows | Where it breaks |
|---|---|---|
| Computer VM stack | each level has its own words; ISA vs runners; system calls | Softland is shared, not private per process; it appends, never overwrites; it has many writers and no global clock |
| Single-level store (AS/400) | protected persistent memory, with security below the interface | one organisation; no history or branching |
| Smalltalk image | VM + primitives (= capabilities) + everything else editable live; your "smalltalk-ui-vm" | single user; mutated in place; no versions; arbitrary programs |
| Network stack | each level wraps the one above with its own header; end-to-end argument | nothing is remembered |
| Database turned inside out (log + derived views; Datomic) | the store's bottom three levels exactly; schema as facts; reading "as of" a time | no computation, tools or inhabitants |
| Reflective tower / STEPS | malleable all the way down; a language per level | collides with the 6 September ruling |

`★ Insight ─────────────────────────────────────`
- `act.clj` keeps the act's shape as compiled code, yet "declared as data all the same, so that it can be read in one place, printed and shown". That is the "box that must open" at the very bottom: an interface can be code and still be readable.
- The System/360 lesson cuts both ways. Because runners are swappable, the record language is the thing that has to stay stable. Its version marker matters more than any runner's speed.
`─────────────────────────────────────────────────`

---

# 2. The store, derived from scratch

## 2.1 What the vision asks of a store

- **One truth:** "No side-channel state, no truth outside the log" (decisions.md).
- **Return:** leave without a save ritual and come back to the same thing (carry-on §4). Show the committed result, with no optimism ("I dont want to do any optimistic updates", 17 July).
- **Two kinds of history:** "Automatic event history and an explicit meaningful version are different needs" (§2).
- **Work against a base:** "An active layer is work against a base", comparable, with candidates that are real and adoption that has scope (§2, §5).
- **Permission:** "what am i allowed to query"; groups; "the very first power to give to sustem is authentication and authorization" (April).
- **Provenance:** who made it, from what, why. "Every result keeps the subject it was built from."
- **Grain:** "if we say its 240 fps and i start panning are these events getting stored?" (17 July). Not everything is durable.
- **Live change:** redraw only what changed, keyed by identity (§7).
- **Re-derivation:** the accepted base rests on what "can be re-derived and checked" (§8). The store comes out the same when run again (13 October).
- **Evolvable meaning:** "what if the intial algebra is wrong??" (April). Schema and vocabularies can change (§2, §6).
- **No merge deadlocks** (§6). Years of data, many writers, many readers.

## 2.2 The levels, bottom up, each defined by what it lets you stop worrying about

You asked to start from a durable, append-only, ordered log. I agree, with two refinements: the log is atomic **per entry** and ordered **per line**, nothing wider.

**S0 · The log: keeps what arrived**
- Promises:
  - once acknowledged, an entry survives crashes and the loss of a machine;
  - nothing is changed or removed;
  - an entry is whole or absent;
  - within one line, every reader sees the same order;
  - it can be read again from any point.
- You stop worrying about crashes, half-written things, lost history and reordering.
- Already decided: the log is never trimmed (12 October, per the store-spectrum note), and a layer is one line (13 October).
- Open: *how* durable. One machine plus backups (17 July) or replicas is itself a spectrum.

**S1 · Decisions: says what happened to each arrival**
- Promises:
  - every arrival gets exactly one answer, accepted or refused with a reason;
  - the same act sent twice is decided once;
  - a whole act is accepted or none of it;
  - answers follow from the arrivals in order, so running again gives the same answers;
  - the accepted acts of a layer are counted in order (the stamp).
- You stop worrying about retries, partial effects, "did it happen?" and runs that differ.
- The classic name is a replicated state machine: an ordered log plus a step function that gives the same result on the same input.
- This is also where every higher level's admission rule meets: form, "the replaced fact exists", "who may act here". The gate is a stack of checks, one per level.

**S2 · Facts: says what is true now, and what was**
- Promises:
  - every fact has an id that never changes and is never reused;
  - a fact never changes: change is a new fact that replaces it, or a retract;
  - the value at a place is the newest fact there that nothing has replaced;
  - a read never sees half an act;
  - a live reader gets every change at its places, in order, or is told it missed some;
  - past states can be read (time travel, designed later).
- You stop worrying about indexing, deriving the current state and spreading changes.
- Open: a value is capped at 64 KiB, so large raw material needs a sibling store (below).

**S3 · Layers: says which facts you see together**
- Promises:
  - an act in a layer changes only that layer;
  - a read through a list of layers sees, at each place, the nearest layer's fact, including "nothing here";
  - a layer is never changed by the layers built on it;
  - any list of layers you may see is a valid view;
  - *(candidate)* a layer can be marked at a stamp (a meaningful version) and read as of that mark.
- You stop worrying about disturbing others while you work, trying alternatives side by side, and combining others' work.
- Open: adoption, meaning how one person's work becomes the base (the store's queue, K13); marks; comparing two layers.

**S4 · Standing: says who may, and who did**
- Promises:
  - you never see a fact in a layer you may not see, and a read says when it left something out;
  - no act is accepted from someone who may not act there;
  - every permission ties back to one person (8 October);
  - a given permission never exceeds its giver's;
  - every fact says who wrote it, under which permission;
  - a locked value can be made unreadable for good by destroying its lock, while the log itself is never cut (11–12 October).
- You stop worrying about access, forged authorship and delegation you can't trace.
- It assumes one thing from the edge: sign-in tells the truth about who.
- Open: ending a permission, who may act as the system, groups.

**S5? · Vocabulary (a candidate level): says what a key means**
- Promises: every fact under a declared key fits its declared shape, and the declaration is itself a fact, versioned and overridable per layer.
- You stop worrying about malformed facts, and about meanings that are lost once the code that wrote them is gone.
- Open: whether the store *enforces* this at admission or only *describes* it. The end-to-end argument says a low level should enforce only what it can enforce completely.

**Beside the store: blobs.** Large raw material, addressed by the hash of its content. Carry-on §2 asks to keep "the raw artifact as well as the form that makes it usable".

**Before the store: the uncommitted.** Typed but not yet committed, a pan at 240 fps, cursors. Promises: fast; never shown *as* committed; may be lost. It already has a home in the engine ("the uncommitted store (what is not yet a row)"). For *truth*, the bottom is the log. For *experience*, it is this edge.

**Through every level: who, when, why.** They are written at S1 (the time label), at S4 (who, permission), and by makers (`because-of`, `based-on`). This is your "pipes and electricity cables".

## 2.3 The act is a packet

Every part of the act belongs to some level:

```
 ┌ door's declaration ──────── S0/S1: act-id · class · seal · time ───────────┐
 │ ┌ maker's act ───────────────────────────────────────────────────────────┐ │
 │ │ layer                                S3                                │ │
 │ │ who · permission · session(?)        S4 (session: unsure of its level) │ │
 │ │ based-on · because-of · claimed-when · subjects    who/when/why        │ │
 │ │ facts                                S2: the payload                   │ │
 │ └────────────────────────────────────────────────────────────────────────┘ │
 └────────────────────────────────────────────────────────────────────────────┘
```

The store already wraps like a network stack. Decision 2 of 6 October: "The door's additions (the act-id, the class, the seal) are the door's own small declaration, which contains the maker's." One thing still crosses the line: **identity.** Fact-ids are built from act-ids, and act-ids are minted in the lower wrapper. So the lower header reaches into the upper references.

## 2.4 Order: total inside a layer, causal across layers

Each layer is its own line, so there is no "before" between two acts in different layers, except a claim (time is a label, 14 October) or a link (`based-on`, `because-of`). Within a layer, the store gives a total order. Across layers, it gives causal order, carried by links. That is what `based-on` really is: the clock that works across layers. A working layer knows which base it saw by citing it, not by a timestamp.

## 2.5 Tensions the derivation turns up

| Tension | Where it stands |
|---|---|
| Append-only vs being forgotten | Resolved by locks (11–12 Oct), but only for locked values. Plain is the default (the end that keeps more), so a secret written plain stays forever. You chose this knowingly. |
| One line per layer vs atomic change across layers | Adoption can't be atomic across a working layer and the base. It is one act in the base that cites the working layer. |
| Same result on a rerun vs clocks | Settled: time is a label. |
| 64 KiB values vs raw files | Blobs, open. |
| Live reads vs a permission that ends | What does a live reader see at that moment? Open. |
| A form that lasts forever vs levels that are still learning | Keep the form to what the store *checks* (last reply's test). |

## 2.6 One level or many?

Many: five clear levels (S0 to S4), one candidate (S5), a sibling (blobs) and an edge (the uncommitted). They can all live in one Rama module. A level is an interface plus its promises, not a process or a deployment. The derivation mostly *names what each existing part promises*. The store as built holds most of S0 to S4, fused together.

`★ Insight ─────────────────────────────────────`
- Fact-ids are `[act-id index]`, and act-ids are `[layer class uuid]`. Whatever the door puts into an act-id is therefore copied into `replaces`, `based-on` and `because-of` for ever. Identity is the one channel by which a lower level's header reaches into the data's relationships.
- Decision 6 settled that "a fact has no layer of its own". So the layer lives once, on the act, the way an IP address lives on the packet and not on each byte of its payload.
`─────────────────────────────────────────────────`

---

# 3. Fractal layers and planned leaks

## 3.1 What your editor example is made of

The example uses several different relations:
- **contains**: the editor holds a code editor, a markdown editor and a text editor;
- **is a kind of**: a code editor is an editor;
- **mixes in**: the vim way of editing applied to a code editor;
- **picks from**: a few pieces taken from vim, vscode and emacs;
- **builds on and overrides**: your custom layer over those pieces, and other people's layers over yours.

April adds one more motion: "a data and view become solid kinda after some iteration it can become the base for next _________", and "from a meta pov is recursive or fractal but that dies not mean loosing the underlying thing that was invented". This is not a new relation. It is a layer **slowing down**: it moves from working to base, from fast to slow, so that others can depend on it. Adoption is that change of pace, and that is the moment a layer starts owing promises to whoever builds on it.

## 3.2 Two primitives at every scale

All of those relations can be made from two primitives:
- **Reference.** A fact points at other things. This gives "contains" and "picks from".
- **Overlay.** A layer builds on an ordered list, and the nearest wins. This gives "is a kind of", "mixes in", "overrides" and "builds on".

The store already has both. If every scale uses the same two, the structure really is self-similar, which is what "fractal" means. Precedents:
- **Self** (the prototype language): every object has parent slots, and lookup walks them in order. Delegation all the way.
- **Nix overlays**: each overlay is a function from the package set below to changes on it. Users stack their own on top of other people's.
- **Spacemacs**: very close to your example. It is an editor made of configuration layers. An editing-style setting picks **vim, emacs or hybrid**, and users write private layers and share them.
- **CSS**: two at once. *Inheritance* runs down the containment tree, and the *cascade* runs across overlays (browser, author, user). Custom properties are settings that flow through both.
- **OverlayFS and Docker image layers**: the store's "nothing here" is their *whiteout*, the one extra primitive overlays need to remove something without deleting it below.

## 3.3 Where I change my mind, and where I don't

**I change my mind on one point.** Last time I called "layer" one word with three meanings. Your fractal layer and the store's layer can be **the same concept**: reference and overlay, at every scale. Calling that a collision was wrong.

**I hold my ground on another.** The level in thread 1 is a different *relation*:
- A layer **builds on** another: it overrides and extends.
- A level **runs on** another: it is interpreted or kept by it.

An editor layer builds on a text layer, and both run on the executor. Layers are many, fractal and fast. Levels are few, stacked and slow. These are two words for two relations. The names are yours to choose.

**The bridge between them.** The fractal reaches into every level **through its knobs, never through its engine**. A layer can set the commit grain of its buffers. It cannot change the gate.

## 3.4 What a fractal costs

- **Depth.** A read through a deep chain walks many layers. The render-boundary decision names this disease: "the read side paying to rediscover what the write side already knew". Where resolution should happen is a structural question for S3. If an answer ever starts to look like a cache, the project's caching rule applies: stop, and take it to a fresh adversarial session.
- **Conflicts.** Two pieces that write the same place are settled by order. Two pieces that *mean* clashing things are visible only if they share a vocabulary. Vim's `j` and a plugin's `j` clash only if both are keymap facts. So a mix needs a shared vocabulary at the level where the mixing happens.
- **Live or pinned.** If you pick vim's `w` motion into your layer, is it a reference (it follows vim) or a copy (it is now yours)? This is the first turn's dependency question again, now one piece at a time.
- **Promises to builders.** Once others build on your layer, your changes reach them. Rich Hickey's rule is to add, never break. A layer that is built on has slowed down.
- **Lineage.** If pieces are kept as references and not copies, every custom layer carries its ancestry. Carry-on §9's "contributors identifiable" then comes for free from the fractal.

## 3.5 Planned leaks: your spectrum rule is already the core

You ruled this on 23 September: "we are providing a spectrum and if there are two outcomes on the spectrum on either end then we are providing the whole spectrum ... the choosing remains with the user and the toolmaker ... we can provide some good defaults." On 12 October you added how the default is picked: the end that "captures more". The exception is **uniformity rules**, which are one rule for every record. The test from the same note: could two tools follow different rules here without every reader carrying both forever? If not, the store fixes it.

That gives a three-way test for any lower-level choice:

| The choice changes… | Two users would want different answers? | It is… |
|---|---|---|
| only *how* (the mechanism) | no | a **secret**: hide it |
| what someone *experiences* (when, whether, who, how much is kept) | yes, and both can coexist | a **knob**: a spectrum with a default |
| what someone experiences | yes, but they can't coexist without every reader carrying both | a **uniformity rule**: fixed before the first record |

The closest academic precedent is Gregor Kiczales's *open implementation* (1990s). A module offers two interfaces: the **base interface** (what it does) and a **meta-interface** (settings for how it does it). Joel Spolsky's "leaky abstractions" is the passive version of the same observation. Open implementation is the planned version.

## 3.6 Knobs are facts in layers, so they compose fractally

If a knob is an ordinary fact in a layer, it needs no new mechanism:
- the base sets defaults;
- the editor layer overrides them;
- your custom layer overrides the editor's;
- nearest wins, and every setting has a writer, a time and a history.

That is CSS custom properties, carried into the store. Three rules then follow:
1. **Name a knob where it is felt; own it where it is decided.** The typist feels "save as I type". The store decides how soon an act is decided. Each level boundary translates from one to the other.
2. **Above the waist, translations are records; below it, code.** Someone can invent a new promise word (a profile) with no code, as long as it compiles down to existing lower knobs. A new *lower* knob needs code: a new capability or a new store class.
3. **A promise that can't be kept is refused or reported, never quietly downgraded.** This is the same rule the read path already follows: it "says that it left one out".

## 3.7 Typing, traced down

Your example hides **two** knobs, and a typist feels both:

| Level | Knob A: grain (how much history) | Knob B: speed (how soon it is committed) |
|---|---|---|
| Typist's words | "keep every keystroke" … "keep when I pause" | "appears as I type" … "appears in a moment" |
| Computation | one act per keystroke … one act per pause | (passes through) |
| Store, decisions | (passes through) | decided at once … decided in a batch |
| Mechanism (hidden) | how the uncommitted edge flushes | stream … microbatch topology |

A third setting sits beside them: **what the typist sees before the commit**. It can be nothing, or a pending mark. Under the no-optimism ruling it can never be "shown as if committed". Carry-on keeps "responsive writing without optimistic updates" as an open engineering question, and this knob is where that question lives.

So a felt promise is often a **bundle** of lower knobs. Bundles want names: "live writing" (every keystroke, at once, pending marks on), "quiet drafting", "bulk import". A named bundle is just a layer that sets knobs, so profiles are fractal too. And `:streaming` and `:microbatch`, your words, fit well as the store's own promise words for Knob B, as long as they are defined by what they promise and not by which topology runs them.

A grounding from 17 July: a session in that sitting noted Electric's *conflation* (a fast-changing value delivers only its latest state to a slow reader) as a grain affordance. That is a third grain knob, on the *read* side: whether a reader gets every change or only the newest.

## 3.8 Competing metaphors for planned leaks

- **A cell membrane.** It hides the inside, but it has specific channels that open on specific signals. The cell decides which channels exist. This is the closest image to "the level owns the knob; the world above triggers it". Given where Softland is headed, this one may matter.
- **Car drive modes.** "Eco" and "sport" are promises in the driver's words, and each bundles a dozen engine settings. Manual mode is full exposure. These are profiles.
- **SQL isolation levels.** The classic case of a concurrency mechanism exposed as a promise ("serializable", "read committed") and named by the guarantee, not by locks or versions.
- **TCP_NODELAY.** Your typing case, one level down. Nagle's algorithm batches small writes, and interactive programs turn it off. The knob is real, but its name is the mechanism ("no delay"), which shows what happens when a knob is named where it is decided instead of where it is felt.
- **Yoneda**, which you raised in April. A thing is known by all the ways it can be used. A layer *is* its interface plus its knobs.

`★ Insight ─────────────────────────────────────`
- The store-spectrum note already draws the open-implementation line. Spectra are the meta-interface. Uniformity rules (encoding, the act-id on every fact) are the parts of the base interface that must never become knobs.
- Because knobs would be ordinary facts, a knob change is itself an act. It is attributed, ordered and refusable. A layer that cannot honour "decided at once" refuses the setting with a reason, the same way it refuses any act.
`─────────────────────────────────────────────────`

---

# Where the threads touch (seeds, not a convergence)

- **Thread 1 × 2.** The VM frame maps well onto the store's levels at the bottom (memory, plus a kernel *below* the interface, as on the AS/400) and onto the executor (the CPU). Above the waist it stops being a VM stack and becomes composition in one language.
- **Thread 2 × 3.** Most planned leaks start in S0 and S1 (grain, speed, durability) and in S3 and S4 (live or pinned, who sees). The uncommitted edge is where a typist feels them.
- **Thread 1 × 3.** "Runs on" (levels: few, slow) against "builds on" (layers: fractal, fast). The fractal reaches each level through its knobs, never its engine.
- **All three.** Identity is minted at one level and used at all of them, and who, when and why run through everything. Both look like the places where the levels will have to agree.

Three things are only yours, to carry into the convergence:
1. Is "layer" one word for both the store's space and your fractal unit? I now think it can be. And what is the word for a level?
2. Should interpretation depth still stop at one executor (6 September), or does the VM frame pull you toward a language per level?
3. Which promise words should a typist see?
