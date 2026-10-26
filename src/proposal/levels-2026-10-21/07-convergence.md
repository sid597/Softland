# Round 7: The Other Two "Few" Parts (A Fresh Session)

## Prompt



<pasted_content id="9341">
load src/proposal/levels-2026-10-21/MAP.md

on 21 october i started a session from two notebook pages. page 1 is my list of problems (on the map). page 2 says: "So what I have currently is a box which can support atomic operations, do some actions and output the effect of those ops to the broader world, you can version your stuff. The starting layer access I have to now is what an ACT is. so now we have a system that gives us input output in form of ACT and that is where we have to meet the user. The question for me now is what can I do and how can I do ... So given we have the store next up is writing into and to it and having an agent interface how?. Why because it will be the resident builder."

then i asked: "i woudl first like to understand if the analogy of "box" holds and then what is the abstraction layer then we are building on like previously we had the rama and we did built it out .. we should have a seperation of abstractions because i treated everything as one abstraction in terms of softland and rama like i did not disjoin them and therefore any new idea or work just went over to cover all the abstractions ... i made the distinction of waist, that is an abstraction but now i have stronger pull towards that we should be thinking of the system buildin in abstraction layers so it gets easier to think on top???"

the way i wanted to work there: "for exploration sessions, collaborative build on top sessions we do start out in a single point but then there are soo many breakouts and then later convergence just like the neural network images few-many-few .. i do understand you need to get to the few but that only works when you work autonomously and not collaboratively this is about collaboration not autonomus"

the many part is done. the map has where we started, the stack of levels that came out of it, the agreements, the tensions, the questions still open and the branches not opened. the rounds it links are the working, verbatim, in the same folder. that session got too big: "this session is too big to have your best pov on how to do the convergence and how so i think we need a new session"

so now the other two "few" parts: "seeing where we started from now that we have the "many" parts, and then the final narrowing down / convergence stretch."
</pasted_content id="9341">


---

## Reply (Verbatim)

I've converged in this session. Nothing is written to disk.

**What I read.** I read the map and all seven rounds, including 02b and 04, which the map's own writer never held. I also read two sessions the map doesn't mention. At 05:03 you opened the same notebook pages in three sessions:
- **7846018a** answered the box question its own way. It then took your "sorry i am lost … how do i map it to what is being provided by the softland store" and "how would you describe the capabalities at each floor/level".
- **43ddd7b1** answered it too, then read about 35 earlier systems into a "frontier map".

Both answer the same question, so I brought them in. For ground I read carry-on, decisions.md, the act's form in `act.clj`, the store's map, and the before-real-data page.

**How I converged:**
1. **Where we started.** I took your starting sentences one at a time and said what each became.
2. **Narrowing.** I sorted the map by how fast each thing changes.
   - Slow things get narrowed now. Slow means it hardens at the first kept record, or the next build stands on it.
   - Fast material keeps what we learned and waits for use to find it. The reason is structural, not "not needed today": fast things drawn up front by guess get cut across by the next idea, which is the same spreading in a new form (round 2; your 16 Oct "trickle down" line).
3. **Agreement counts as modest evidence.** Where the three sessions agree, I treat it as a signal, not proof. They are one model family reading the same pages, and one of them first read another model's work that you pasted in.

## The short version

1. **The box holds for one floor, the store,** as a glass box that remembers and decides. It doesn't "do actions". It's where *builders* meet the land; users meet gestures and views.
2. **What we build on is the store's face:** an act goes in, an answer comes back, and a read comes out through your own layers. It makes six promises, in Softland words.
3. **Your pull toward levels names a missing axis.** You had one axis well built: *who may change a thing*. It runs from our code in git, to the base, to shared layers, to yours. The waist is the first cut on that axis, and your fractal layers are the rest of it. The missing axis is *what runs on what*. Without it, the architecture was a flow plus the waist, so no idea had a local home.
4. **There are two kinds of step up.**
   - A **floor** adds an engine: code that offers a small language.
   - A **layer** adds material written in a language that already exists.

   Floors are few and get drawn now, with proofs. Layers are your fractal, found by use.
5. **The floors:** Store (built), Meaning, Running, Making and Surfaces, over Rama. The waist runs down the middle of every floor.
6. **You stand on the store's face.** It's built, proven in one process, and empty. It already answers your page-1 problems about talking to it, versioning and making layers. The rest sit on floors that are empty. The resident builder needs two of them: *find* (Meaning) and *run* (Running).
7. **Four things harden at the first kept record:**
   - what an act's id holds;
   - which slots the act's form holds;
   - what a key is;
   - what a thing's id is.

   All four are yours to decide, and so is the day of that first record.

## Step 1: where we started, seen from the many

### Page 2, phrase by phrase

- **"a box which can support atomic operations"**: holds. A gate accepts the whole act or refuses the whole act, with a reason (`act.clj`'s own description, checked).
- **"do some actions"**: doesn't hold for the store.
  - The store decides and remembers. Doing belongs to Running (records run by the executor) and to the makers.
  - A runner may live inside the store's Rama module (decisions.md lists "the store's own derivation" as one runner). Being kept or run on something is still not standing on it.
  - This is where Rama got fused in. If the box does actions, every new idea has a reason to go into the box, and the box is Rama (43ddd7b1's reading).
- **"output the effect of those ops to the broader world"**: holds if it means anyone allowed can read what changed, once or live. It doesn't hold as acting on the world: nothing in the store reaches out.
- **"you can version your stuff"**: half holds.
  - Every act is kept in order, and a fact names what it replaces. That is history.
  - A version that means something ("version 3 of the converter", a candidate, an adoption) is material above the store. Carry-on §2: "Automatic event history and an explicit meaningful version are different needs."
- **"The starting layer access I have to now is what an ACT is"**: holds, with two additions: the answer, and the read. The act is the way in; the read, through your list of layers, is the way out.
- **"that is where we have to meet the user"**: I'd correct this one. The act is where we meet the **builder**: you writing directly, the agent, or a program.
  - A user meets gestures and views. A converter record turns a gesture into acts, and that's settled in decisions.md (16 Oct): "What turns what arrived into acts (a commit's files, a gesture, a new fact) is a record in a layer".
  - If users met the act, every user idea would become a store idea.
- **"yes its all one but we are also building software that is modular"**: both halves hold, on different axes.
  - *All one:* one store, one log, one place where everything has an address. Every floor's material is kept there.
  - *Modular:* floors are levels of reading, not of storage. Each floor's engine reads its own material out of the same store. The disk holds every program, but the editor is not the disk.
- **"next up is writing into it and having an agent interface how? … the resident builder"**: the agent isn't a floor and needs no interface of its own.
  - It's a maker, outside every floor, using each floor's face.
  - Writing exists already: the door. What it lacks is *find* and *run*.
  - Its "interface" is a thin harness offering those faces as tools, plus one page per floor that it reads instead of opening the box. Carry-on §9 asks for exactly this: "a composable, hierarchical description of interfaces".

### Your question, in its three parts (‹87ffd01d›)

**Does "box" hold?** For one floor, and only as a glass box. You never *have to* look inside, and you always *can*. Carry-on §5: "Opening the definition behind an instance should be deterministic and reveal its accumulated record."

**What are we building on?** The store's face, which makes six promises with no Rama in them:
1. One form in, whatever made it.
2. Each act is decided whole: accepted, or refused with a reason.
3. What's accepted is kept forever, in order inside its layer, at its version. Refusals are kept too.
4. You read through a list of layers, nearest first, once or live.
5. Who may act where is a permission that ties back to one person, and every act names the permission it uses.
6. Running the same logs again gives the same store.

The face also states its limits:
- a value has a maximum size;
- order is total inside a layer, and across layers only links (`based-on`, `because-of`) say "after";
- the time on an act is a label, not a guarantee;
- a layer is decided either at once or in a batch.

The face also lists what the store must not know:
- what a value means;
- what a thing is beyond its id;
- names;
- that anything depends on anything;
- how to run anything;
- how anyone's identity was established.

**Should we build in abstraction layers?** Yes. The many found that separating Rama from Softland was one case of a larger problem. Four things made "any new idea or work just went over to cover all the abstractions", and three of them are in your own words:
- **A flow got drawn where floors were needed.** On 3 Oct you had "Data sources (outside, inside inlet), adapters, converters, act, depots (streaming, microbatch)" in your head (‹589cf8ba›). That's the path a file travels. Whatever rides along with the data passes through every stage of a path: who, identity, time, permission. So a decision about any of those touches every stage.
- **The abstraction and its examples sat in one drawing.** Same line of yours: "one we are making the better abstractions and then the instances they appy to in the same diagram and same layer it is bound to fail every time".
- **The waist did the floors' job.** Your definition: "i use waist as to refer to something that has to exist as code vs what can and should be build on top ecs or whatever style" (‹89442b46›). That answers *what is code and what is data*. It never answers *what runs on what*. In the renderer the two answers lined up, so it worked there. After that, the waist stood in for floors everywhere.
- **Kept on is not built on.** Everything is kept in the store, and the store runs on Rama. So every idea felt like a store idea, and the store felt like Rama.

Sign-in is the worked case. Your own description on 15 Oct already had the floors right: "sign-in gives us a bunch of data and we just store that data in rama". The build still made "four changes to the store's gates and read path that no sign-in reaches", in the words of the commit that took it off main (`3a41c91d`). With floors drawn, sign-in is a maker: it hands the store acts, and the store checks only the permission an act names.

### The builder's half of page 1

- **"How and where to store computations functions" and "if code is in data … dependency"** were one word for four things:
  - a pointer from one thing to another belongs to Meaning;
  - the exact version pinned belongs to the Store (ids, `replaces`, `based-on`);
  - "what did this run against" belongs to Running;
  - "what breaks if this changes" is a placement question. It could be a table the gates keep, or a view a record keeps.
- **"can each implementation be first written down proof for"** is the form of every floor's boundary, not a floor of its own.
  - A floor's face is its promises. A proof is about those promises and assumes only the floors below, so proofs stack the same way floors do. The 24 September model was the first case of this here.
  - Proof effort follows pace. Engines get models and proofs, material gets checks when it's adopted, and sketches get none.
  - Usefulness has no proof; only you judge it.
- **The builder's authority** already has the right shape, because every act names the permission it uses and the gate checks only that one. It still needs:
  - a narrowed grant: this layer, these kinds, until tonight;
  - runs that use the asker's permission, never the system's;
  - a way to cut a grant, which isn't built.

### Page 1, sorted onto floors

| Your problem | Floor | What exists today |
|---|---|---|
| Where to store computation functions | Running (a record in a layer) | the form is decided; no executor in the store tree |
| Code as data: dependency | split across floors, as above | half: an act pins what it read; reads follow your layers |
| Store on the server side first, "because that is the interface" | Store | yes |
| Populate from a non-browser way | Making | the door exists; no maker uses it yet |
| Protocols: talk to it, modify, version | Store | yes: act, answer, read, `replaces`, version |
| Add a layer | Store | the mechanics, yes; who may make one is open |
| Protocols for running | Running | nothing |
| The LLM interface | not a floor: a maker's harness (Making) | nothing |
| Proof based | every floor's face | the store's proofs, run in one process |
| Naming, finding what exists where | Meaning | lookup by a value (`:points-at`); no names, no word search |

## Step 2: narrowing down

### The picture

```
                     CODE: ours, in git        ┃  FACTS IN LAYERS: in the store
                     the floor's engine        ┃  base ───► shared ───► yours
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SURFACES             painters, the runtime's   ┃  views, compositions, the ECS layer
                     builtins                  ┃  (editor ◄─ vim-like ◄─ yours)
MAKING               inlets, sign-in, the      ┃  data sources; what sign-in learned
                     builder's harness         ┃
RUNNING              the executor, its         ┃  records: tools, converters, checks,
                     runners, capabilities     ┃  and their results
MEANING              what makes a thing's id;  ┃  things, names, kinds, key meanings,
                     find                      ┃  grammars, "same as before" claims
STORE  ◄ you are     act form, door, gate,     ┃  acts and facts; layers, builds-on,
         here        read path                 ┃  permissions, each layer's depot
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
HOST FLOOR           Rama, JVM, disks: keeps the logs; its words stop at the store's face

┃ is the waist, once on every floor.
Carried on every act, owned by no floor: who · which layer · by what right · from what · why · about whom
```

- **Rows are floors: what runs on what.** A floor exists where there's an engine: code that offers a small language to the material above it. The four languages are the act, the record language, the grammar language and the render builtins. On 16 Oct you said you liked this framing: "we need engines as code and then other things are examples on top of them" (‹8567c8eb›).
- **Columns are who may change a thing, slowest to fastest.** Our code in git, then the base, then shared layers, then yours. The waist is the first cut on this axis. Your fractal layer is the rest of it: a layer builds on layers. The store's layer and your editor layer are the same idea; 02b changed its mind to this, and I agree.
- **A layer cuts across floors.** Your editor layer can hold its own kinds (Meaning), its own commands (Running) and its own views (Surfaces), because the store keeps material of any floor in any layer. That's how "fractal to the bottom" works: it reaches every floor through material and settings, never through an engine. A layer can set how soon its acts are decided; it can't change the gate.
- **Knobs are facts in layers, so they stack the way layers do.** Your setting overrides the editor's, and the editor's overrides the base's. That's your "some knobs do require to be available on different layers/levels".
  - The store already has one: a layer's depot (decided at once or in a batch), which its members set as a fact on the layer (7 Oct, decision 3, checked).
  - Name a knob where it's felt ("appears as I type"). Own it where it's decided (the layer's depot).
- **How to map any idea onto the store.** Find the idea's floor.
  - If it's the store floor, the store provides it: act, answer, read.
  - If it's higher, the store only keeps its material, and that floor's engine is what's missing.

**Why this order.** Meaning sits below Running: a record names things by id and uses keys, and ids and names need nothing to run. Round 5 had it the other way round; I side with both other sessions. The stack isn't a strict line, though:
- Cutting a file is a run, because the grammar reader is a capability. What a thing *is* still belongs to Meaning.
- A maker can stand straight on the store. A session writing acts by hand skips Running.
- Each floor plus everything below it is a working system, so you can stop at any floor.

`★ Insight ─────────────────────────────────────`
- `act.clj` already makes the floor argument about itself. The act's shape is compiled code, not a fact, because "it must hold when each fact is wrong". Yet it is "declared as data all the same, so that it can be read in one place, printed and shown". A floor's face can't live inside the material it guards, and it can still be glass.
- The waist is already visible inside the store floor. The gates act on the values of the store's own keys (permission, builds-on, depot, marked keys). Under any other key they keep the value, and at most sort it, without knowing what it means. That one habit is what makes Meaning a separate floor.
- The store's map says a reader learns that an act came after a fact it's based on "from the fact id … and not from the carried number". `based-on` is the store's only clock across layers. `claimed-when` is just a label, which is why the two slots fare differently in the act-form question below.
`─────────────────────────────────────────────────`

### What the picture corrects in the many

- **The 11-level stack still drew one horizontal waist** ("the waist runs through 6"). That's the one-waist picture your question was moving away from. It also put engines above that waist: Meaning's id rule and lookup, and Surfaces' painters. Its top four are mostly vocabularies, so they're layers, not floors:
  - Change and Work are material;
  - Surfaces keeps its engine;
  - Components (the ECS) is Surfaces' material.

  The questions those levels answer are real. Material answers them, not new engines.
- **"The store is five levels fused" (rounds 2–3) against "one level, permissions built into every row" (7846018a).** Both are right, on two different maps.
  - On the *what runs on what* map, the store is one floor. Nobody above thinks in log versus facts versus layers.
  - On the *which decision hides where* map, it holds several: how durable, how decided, how indexed, how layers resolve, how permissions are checked.
  - 43ddd7b1's reading found this is Parnas's "two maps, not one". Round 2's definition of a level ("words, promises, a secret, one direction") merged the two maps.
  - The five derived levels become the outline of the store's face page: keeps, decides, holds facts, layers, permissions.
- **Round 4's "a room turns into a floor when it offers words."** Under the engine test, offering words makes a vocabulary, like a library, not a floor. 02b's phrase for this: Softland is "shallow in interpretation and deep in composition". Above the waist there's one record language (6 Sept: "Nothing in a record is a program"). A separate language per floor would reopen that ruling, and that's your call, not a metaphor's.
- **"The depot is Rama's word in the act-id."** 02b corrected this one: the words are yours ("my streaming and microbatch", 6 Oct). The real question is narrower: should an id, which every reference copies, carry how its act gets decided?
- **"Forever."** The 6 Oct decision priced a later change of that word: Rama can rewrite a log's records at a module update (read in its docs, not run). So after the first record, a change is a migration, not impossible. What hardens is the cost.
- **"Your 30 July words: material, Softland code, and host floor."** A session wrote that phrase two minutes before you typed it (`who`: «652eb930», then ‹159c8e85›). It's in your typed line, but it isn't safely your coinage.

### How a new idea finds its floor

This is the "easier to think on top" you asked for, as a tool:
- **Name the one floor the idea changes.** If it changes several, it's really several ideas, or a boundary is in the wrong place.
- **A change inside a floor that keeps its face touches nothing above it.**
- **A promise that moves sends every floor above back to its proofs.** Loosening a limit is free; tightening one breaks whoever relied on it.
- **Nothing moves down except a need.** That's your trickle-down.
- **There's one write path: the door.**
- **Cost grows as you go down.** At the store floor it also grows with time once the first record is kept.

Three walk-throughs:
- **A function renamed in the repo.** Making imports the file again; Meaning records a claim that it's "the same as before"; the store keeps both at their versions. Pointers by id don't change.
- **A new file format.** A grammar record plus a converter record, with no new code. The exception: if the one grammar reader can't express the format, that comes to you.
- **A permission taken back.** Store floor, from that point on. Past acts stand.

### Where each breakout went

- **A, computer levels.**
  - Gave: a floor is a machine with a small language.
  - Gave: one record language with several runners, like System/360.
  - Gave: the "OS level" spreads across floors:
    - address spaces are layers;
    - the file system is names and lookup;
    - processes are runs with a budget;
    - the shell is the builder's harness.
- **B, levels from the log up.** Its bottom five became the outline of the store's face, and Computation and Meaning became floors. Change and Work wait as vocabularies.
- **C, one truth, many views.**
  - Gave: the store's own promise that running the logs again rebuilds it.
  - Its open question (which checks must happen at write time) folds into the act-form question.
- **D, hourglass.** Gave: one narrow language per floor, and the end-to-end test for the act's slots.
- **E, fractal.**
  - Gave: layers reach floors through material, not engines.
  - Gave: a deep stack of layers needn't cost more at run time, because it can be compiled into one.
  - Waits for the ECS layer: Emacs's known failure modes (load order, half a keymap, upstream breakage, "where did this come from?").
- **F, knobs.** Gave: knobs are facts in layers, plus the test that sorts each low-level choice into hidden, a knob, or one rule for everyone.
- **G, pace.**
  - Gave: the second axis itself.
  - Gave: a thing gets slower because others depend on it, not because someone declares it slow.
  - Gave: every pace boundary needs three things: absorb, protect, see.
  - Gave: proof effort follows pace, and the slowest things change by editions.
- **H, Plan 9.**
  - Gave: read from many layers, write into one (already the store's rule).
  - Gave: a list of layers is one shareable fact.
  - Waits for Change: a past state as just another layer.
- **I, Conway and Linux.**
  - Gave: one frozen interface with free insides. Records that use only the faces survive engine changes.
  - Waits for Change: the six moves (split, substitute, augment, exclude, invert, port), and who keeps a shared layer.
- **J, the math of vocabularies.**
  - Gave: why a key's meaning must stay low. Turning typed data into facts loses nothing only if you keep the vocabulary and each thing's kind.
  - Gave: a checkable "does this change break anything?" test for vocabularies. The frontier map says nobody has built one.
- **K, capabilities.**
  - Gave: acts already name their permission, which is the right shape.
  - Gave: records carry no authority of their own, and the builder gets least authority.
  - Waits: taking a grant back, and grants bound to a person or usable by whoever holds them.
- **L, biology.**
  - Gave: a waist freezes because things depend on it, so get it right before it freezes, and give it editions.
  - Gave: checking is strongest at the waist.
- **M, economics.** Gave: credit for contributions is computed by construction. Terms set as layer facts and commons rules wait.
- **06, the three bridges.**
  - Gave: Surfaces and its ECS material are one floor.
  - Gave: diffs are exact only if identity is given, so decision 3 decides that.
  - Gave: "late binding" just means reads go through the asker's list of layers, which the read path already does.
  - Waits for the ECS layer: the change algebra and the four coherence laws.

### The map's agreements and tensions, sorted

- **Narrowed now, because they're slow:**
  - Agreement 1, amended: the act, the read and the answer are the boundary, and it's met by builders.
  - Agreement 3: common, small, checked hardest, versioned by editions. This now applies to every engine's face.
  - Agreement 4, with the depot correction above.
  - Agreement 6: identity is decision 3.
  - Agreement 12: records carry no authority.
  - Tensions 1 and 2 merge into one question: which slots the act's form holds.
  - Tension 5: Meaning is a floor below Running. Key meaning changes slowly; the catalog of names is fast material.
  - Tension 12 becomes "what a key is".
- **Resolved by the picture:**
  - Tension 4: no separate OS level.
  - Tension 10: Surfaces is one floor, with the ECS as its material.
  - Agreement 2, amended: every floor has a fixed engine and open material. It's no longer "fixed below the act, open above it".
  - The waist question.
  - "Layer": it's one concept.
- **Waiting as understanding, because they're fast:**
  - Agreements 5, 7, 8 and 9.
  - Tensions 3, 6, 7, 8, 9, 11 and 13–16.

  Each waits for the layer that will need it. Agreements 10 and 11 aren't waiting: provenance and the absorb/protect/see rule are already in the act's slots and the layers axis.

**Words.** Four collisions are left:
- "layer" meant four things: the store's layer, your fractal unit (the same concept), an abstraction level, and the render rule's layer;
- "base" lived on three floors;
- "system" lived on three floors.

One session guessed the last two are why the sign-in work stopped at your "what i mean by base, system etc."; that's not verified.

## What this lets you build next (my position)

The builder's first walk, by floor:
- **arrive:** the harness (Making), with a grant tied to you (Store);
- **find:** Meaning, which is empty;
- **make:** the door, which exists;
- **run and check:** Running, which is empty;
- **show:** reads that carry who, when and why;
- **adopt:** one act in the base, though who may act as the system is still open;
- **return:** lookup, plus the chain of acts pointing back at your ask.

43ddd7b1's reading puts two of these steps on the field's open frontier, unsolved anywhere: identity of a part across change (decision 3), and moving a working layer into the base (adopt). Its headline: Softland's frontier is "building with minds that forget". That is the builder's *return* step.

In order:
1. **The store's face as one page**, in its own words only. It holds:
   - the six promises and the limits;
   - its one knob;
   - what it must not know;
   - a list of every place where something from another floor shows through.

   All three sessions proposed this first, separately. It's the final answer to "does the box hold", and it's what the builder reads instead of opening the box. We'd write it here in chat first.
2. **The four before-the-first-record questions, one at a time, each with its case:**
   - **What an act's id holds.**
   - **Which slots the act's form holds.**
     - The test: a slot stays if the store checks it, or if one of its promises rests on it.
     - Today the gate checks only the form and size of `based-on`, `because-of`, `claimed-when` and `subjects` (store map, checked).
     - `based-on` likely stays. The other three are the ones to ask about.
   - **What a key is.** After real data, a change "is a new table that is filled from the old one".
   - **What a thing's id is.** Decision 3's default is its own number with its name written on it. I'd ask it more sharply: the id belongs to the store, and "same as before" is a claim a named converter makes and can revise.
3. **Meaning's face and the minimum version of it:** ids, names as facts, and lookup by name and kind. Search by words comes later.
4. **The builder's harness:** a thin host offering act, read and lookup as tools.
   - The builder writes acts by hand into a throwaway store.
   - Your ask goes in as an act, and the builder's acts point at it.
   - The frontier map agrees: keep the agent's entry point thin, and put the effort into how the medium describes itself.
5. **The minimum Running floor:** the executor, with the CPU runner as the reference, and runs that use the asker's permission. Then the builder's first job: the markdown and Clojure converters, written as records. That brings Softland's own code in, which carry-on §11 names as the first subject.

The first *kept* record is the point by which item 2 must be settled, and its day is yours. The before-real-data page says no paper names that day, no cluster of this store runs, and nothing uses the store yet (checked).

## Questions for you

1. **Does the picture hold for you?** That's two axes, floors as engines, the waist down every floor, and the store as one floor. If a boundary is wrong, which one would you move?
2. **Words.** For the step that adds an engine: "floor" or "level"? "Layer" stays for your fractal unit, which is the store's layer.
   - My default is **floor**. You wrote "floor/level" this morning and "host floor" before, and it reads as something you stand on.
3. **Next.** Shall we write the store's face here in chat, then take the four questions one at a time? Default: yes.

Where the earlier open questions went:
- **"Code is going to be stored":** still open, and the floors sharpen it.
  - Softland's code as material belongs to Meaning, cut up by grammar records.
  - Computation as records belongs to Running.
  - Program code that the store runs would reopen the 6 Sept ruling.

  The default stays the first two.
- **The builder's why in the store from day one:** the default stays yes. It rides on `because-of`, so it moves with the act-form question.
- **Words:** question 2 above. **Order of work:** the section above.
- **"Making above Running, or beside it?" (7846018a):** a default, not a question. It sits above Running in what it uses, and a maker can skip it.
- **"Does floor 1 check key grammars?" (7846018a):** today the gates keep an extra order by value for marked keys; a grammar check isn't built. That's 7846018a's reading; I didn't re-check it. Whether the store should check is part of "what a key is".
- **The second axis, "who may change it":** taken into the picture.
- **43ddd7b1's three questions:** covered by question 1, by "remembers and decides", and by question 2.
- **"What stands on this":** waits as a placement question until the builder first needs it.

**What I checked, what I derived, and what I didn't read:**
- **Checked here:**
  - in `act.clj`: the act-id's shape and why the depot is in it, the act's slots, and which slots are required;
  - in the store's map: what the gate doesn't check, the 6 Oct words and price, and the 7 Oct knob;
  - the before-real-data page;
  - the sign-in revert's message;
  - decisions.md on converters, and carry-on §2, §5, §6 and §9;
  - every line of yours I quote, through `memtool`.
- **Taken from the rounds without re-checking:** all outside references, the value-size number, and "ties back to one person".
- **Derived by me and the sessions:** the picture, the engine test, the floor order, and the sorting.
- **Not read:**
  - the 117,882-character paste of another model's work in 7846018a (I read that session's assessment of it);
  - the 66,000 words of reading notes behind the frontier map;
  - your notebook photos (I worked from the transcription).
